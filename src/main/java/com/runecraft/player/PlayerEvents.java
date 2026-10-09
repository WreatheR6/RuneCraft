package com.runecraft.player;

import com.runecraft.combat.Equipment;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Region;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.data.Monster;
import com.runecraft.registry.ModItems;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Joins, respawns, dimension changes and the per-tick upkeep of OSRS player state. */
public final class PlayerEvents {
    private static final String SCALED_TAG = "runecraft_scaled";

    private PlayerEvents() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            XpService.updateMaxHealth(player);
            ServerNetworking.sync(player);
        });
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            XpService.updateMaxHealth(player);
            if (!Gielinor.is(destination)) {
                player.getHungerManager().setFoodLevel(20);
            }
            ServerNetworking.sync(player);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            PlayerData data = Players.data(newPlayer);
            if (!alive) {
                data.activePrayers().clear();
                data.setPrayerPoints(data.skills().level(Skill.PRAYER));
                for (Skill s : Skill.values()) {
                    data.skills().setBoost(s, 0);
                }
            }
            if (!alive && Gielinor.is(oldPlayer) && newPlayer.getSpawnPointDimension() != Gielinor.WORLD) {
                Msg.game(newPlayer, "Oh dear, you are dead!");
                Gielinor.teleport(newPlayer, Landmark.LUMBRIDGE);
            }
            XpService.updateMaxHealth(newPlayer);
            newPlayer.setHealth(newPlayer.getMaxHealth());
            ServerNetworking.sync(newPlayer);
        });
        // Vanilla mobs in Gielinor take their OSRS hitpoints.
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!Gielinor.is(world) || !(entity instanceof MobEntity mob) || entity.getCommandTags().contains(SCALED_TAG)) {
                return;
            }
            Monster m = Monster.byEntity(Registries.ENTITY_TYPE.getId(entity.getType()).toString());
            if (m == null || m.isCustom()) {
                return;
            }
            EntityAttributeInstance hp = mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            if (hp != null) {
                hp.setBaseValue(m.hitpoints());
                mob.setHealth(m.hitpoints());
            }
            entity.addCommandTag(SCALED_TAG);
        });
    }

    /** Every Minecraft tick. */
    public static void everyMcTick(MinecraftServer server, long tick) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!Gielinor.is(player)) {
                continue;
            }
            // No hunger in Gielinor; food at 17 also stops vanilla regeneration (OSRS regen is applied separately).
            player.getHungerManager().setFoodLevel(17);
            player.getHungerManager().setSaturationLevel(0f);
            // Coins go straight into the money pouch.
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack s = player.getInventory().getStack(i);
                if (!s.isEmpty() && Inv.COINS.equals(Inv.idOf(s))) {
                    PlayerData data = Players.data(player);
                    data.setCoins(data.coins() + s.getCount());
                    player.getInventory().setStack(i, ItemStack.EMPTY);
                    ServerNetworking.sync(player);
                }
            }
        }
    }

    /** Every OSRS game tick (0.6s). */
    public static void gameTick(ServerPlayerEntity player, long gameTick) {
        PlayerData data = Players.data(player);
        boolean gielinor = Gielinor.is(player);
        // Hitpoints regenerate 1 per minute (twice as fast with Rapid Heal).
        int regenTicks = data.isActive(Prayer.RAPID_HEAL) ? 50 : 100;
        if (gielinor && gameTick % regenTicks == 0 && player.getHealth() < player.getMaxHealth() && player.isAlive()) {
            player.heal(1);
        }
        // Boosted and drained stats return to normal by 1 level per minute.
        int boostTicks = data.isActive(Prayer.PRESERVE) ? 150 : 100;
        if (gameTick % boostTicks == 0) {
            data.skills().decayBoosts(true);
            ServerNetworking.sync(player);
        }
        if (gameTick % 5 == 0 && gielinor) {
            enforceRequirements(player, data);
            regionMessages(player, data);
        }
        if (gielinor && player.getVehicle() instanceof BoatEntity boat) {
            data.sailingDistance += boat.getVelocity().horizontalLength() * 12;
            if (data.sailingDistance >= 32) {
                data.sailingDistance -= 32;
                XpService.add(player, Skill.SAILING, 10);
            }
        }
    }

    private static final EquipmentSlot[] ARMOUR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private static void enforceRequirements(ServerPlayerEntity player, PlayerData data) {
        for (EquipmentSlot slot : ARMOUR) {
            ItemStack stack = player.getEquippedStack(slot);
            ItemDef def = ModItems.def(stack);
            if (def == null || def.equipment() == null) {
                continue;
            }
            String unmet = def.equipment().unmetRequirement(data.skills());
            if (unmet != null) {
                player.equipStack(slot, ItemStack.EMPTY);
                Inv.give(player, stack);
                Msg.game(player, "You need " + unmet + " to wear this.");
            }
        }
        Equipment.total(player, data.skills());
    }

    private static void regionMessages(ServerPlayerEntity player, PlayerData data) {
        Region region = Gielinor.region(player);
        int wild = Gielinor.wildernessLevel(player);
        String key = region.name() + (wild > 0 ? ":" + wild : "");
        if (!key.equals(data.lastRegion)) {
            boolean enteringWild = wild > 0 && !data.lastRegion.startsWith("WILDERNESS");
            data.lastRegion = key;
            if (wild > 0) {
                player.sendMessage(Text.literal("Level: " + wild).formatted(Formatting.YELLOW), true);
                if (enteringWild) {
                    Msg.red(player, "You have entered the Wilderness. Other players can attack you here!");
                }
            } else {
                player.sendMessage(Text.literal(region.displayName()).formatted(Formatting.GOLD), true);
            }
        }
    }

}
