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
import com.runecraft.platform.ServerEntityWorldChangeEvents;
import com.runecraft.platform.ServerPlayerEvents;
import com.runecraft.platform.ServerEntityEvents;
import com.runecraft.platform.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;

/** Joins, respawns, dimension changes and the per-tick upkeep of OSRS player state. */
public final class PlayerEvents {
    private static final String SCALED_TAG = "runecraft_scaled";

    private PlayerEvents() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            XpService.updateMaxHealth(player);
            ServerNetworking.sync(player);
            com.runecraft.combat.AutoRetaliate.sendState(player, true);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            com.runecraft.combat.AutoRetaliate.forget(handler.player.getUUID());
        });
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            XpService.updateMaxHealth(player);
            if (!Gielinor.is(destination)) {
                setFood(player, 20, -1f);
            }
            ServerNetworking.sync(player);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            PlayerData data = Players.data(newPlayer);
            if (!alive) {
                com.runecraft.combat.Teleblock.forget(newPlayer.getUUID());
                com.runecraft.combat.AutoRetaliate.stop(newPlayer);
                data.activePrayers().clear();
                data.setPrayerPoints(data.skills().level(Skill.PRAYER));
                for (Skill s : Skill.values()) {
                    data.skills().setBoost(s, 0);
                }
            }
            if (!alive && Gielinor.is(oldPlayer) && newPlayer.getRespawnDimension() != Gielinor.WORLD) {
                Msg.game(newPlayer, "Oh dear, you are dead!");
                Gielinor.teleport(newPlayer, Landmark.LUMBRIDGE);
            }
            XpService.updateMaxHealth(newPlayer);
            newPlayer.setHealth(newPlayer.getMaxHealth());
            ServerNetworking.sync(newPlayer);
        });
        // Vanilla mobs in Gielinor take their OSRS hitpoints.
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!Gielinor.is(world) || !(entity instanceof Mob mob) || entity.getTags().contains(SCALED_TAG)) {
                return;
            }
            Monster m = Monster.byEntity(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
            if (m == null || m.isCustom()) {
                return;
            }
            AttributeInstance hp = mob.getAttribute(Attributes.MAX_HEALTH);
            if (hp != null) {
                hp.setBaseValue(m.hitpoints());
                mob.setHealth(m.hitpoints());
            }
            entity.addTag(SCALED_TAG);
        });
    }

    /** Every Minecraft tick. */
    public static void everyMcTick(MinecraftServer server, long tick) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!Gielinor.is(player)) {
                continue;
            }
            // No hunger in Gielinor; food at 17 also stops vanilla regeneration (OSRS regen is applied separately).
            FoodData hunger = player.getFoodData();
            if (hunger.getFoodLevel() != 17 || hunger.getSaturationLevel() != 0f) {
                setFood(player, 17, 0f);
            }
            // Coins go straight into the money pouch, tokkul into the tokkul pouch (OSRS stacks both in one slot, up to
            // 2,147,483,647; a Minecraft stack holds 64). What a full pouch can't take stays in the slot.
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack s = player.getInventory().getItem(i);
                if (!s.isEmpty() && Inv.pouched(Inv.idOf(s))) {
                    String id = Inv.idOf(s);
                    PlayerData data = Players.data(player);
                    long have = Inv.pouch(data, id);
                    int in = (int) Math.min(s.getCount(), Integer.MAX_VALUE - have);
                    if (in <= 0) {
                        continue;
                    }
                    Inv.setPouch(data, id, have + in);
                    s.shrink(in);
                    player.getInventory().setChanged();
                    ServerNetworking.sync(player);
                }
            }
            // With a full pack a pouch that holds some still takes more off the ground (OSRS: a held stack needs no free slot).
            if (Inv.pickUpPouched(player) > 0) {
                ServerNetworking.sync(player);
            }
        }
    }

    /** Every OSRS game tick (0.6s). */
    public static void gameTick(ServerPlayer player, long gameTick) {
        PlayerData data = Players.data(player);
        boolean gielinor = Gielinor.is(player);
        com.runecraft.combat.PoisonService.tick(player, gameTick);
        com.runecraft.combat.Aggression.gameTick(player, gameTick);
        com.runecraft.combat.AutoRetaliate.gameTick(player, gameTick);
        // Hitpoints regenerate 1 per minute (twice as fast with Rapid Heal).
        int regenTicks = com.runecraft.osrs.PrayerEffects.hitpointsRegenTicks(data.isActive(Prayer.RAPID_HEAL));
        if (gielinor && gameTick % regenTicks == 0 && player.getHealth() < player.getMaxHealth() && player.isAlive()) {
            player.heal(1);
        }
        // Boosted stats fall by 1 level per minute (90 s with Preserve); drained stats recover by 1 level per
        // minute (30 s with Rapid Restore).
        boolean boostDecay = gameTick % com.runecraft.osrs.PrayerEffects.boostDecayTicks(data.isActive(Prayer.PRESERVE)) == 0;
        boolean drainRestore = gameTick % com.runecraft.osrs.PrayerEffects.statRestoreTicks(data.isActive(Prayer.RAPID_RESTORE)) == 0;
        if (boostDecay || drainRestore) {
            data.skills().decay(boostDecay, drainRestore, true);
            if (boostDecay) {
                com.runecraft.combat.Overheal.decay(player);
            }
            ServerNetworking.sync(player);
        }
        if (gameTick % 5 == 0 && gielinor) {
            enforceRequirements(player, data);
            regionMessages(player, data);
        }
        if (gielinor && player.getVehicle() instanceof Boat boat) {
            net.minecraft.world.phys.Vec3 v = boat.getDeltaMovement();
            data.sailingDistance += Math.sqrt(v.x * v.x + v.z * v.z) * 12;
            if (data.sailingDistance >= 32) {
                data.sailingDistance -= 32;
                XpService.add(player, Skill.SAILING, 10);
            }
        }
    }

    /**
     * Sets food (and saturation, unless negative) through the hunger NBT, which is always present on
     * a dedicated server (HungerManager's setters are only used by client code in vanilla).
     */
    private static void setFood(ServerPlayer player, int food, float saturation) {
        FoodData hunger = player.getFoodData();
        CompoundTag nbt = new CompoundTag();
        hunger.addAdditionalSaveData(nbt);
        nbt.putInt("foodLevel", food);
        if (saturation >= 0f) {
            nbt.putFloat("foodSaturationLevel", saturation);
        }
        hunger.readAdditionalSaveData(nbt);
    }

    private static final EquipmentSlot[] ARMOUR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private static void enforceRequirements(ServerPlayer player, PlayerData data) {
        for (EquipmentSlot slot : ARMOUR) {
            ItemStack stack = player.getItemBySlot(slot);
            ItemDef def = ModItems.def(stack);
            if (def == null || def.equipment() == null) {
                continue;
            }
            String unmet = def.equipment().unmetRequirement(data.skills());
            if (unmet != null) {
                player.setItemSlot(slot, ItemStack.EMPTY);
                Inv.give(player, stack);
                Msg.game(player, "You need " + unmet + " to wear this.");
            }
        }
        Equipment.total(player, data.skills());
    }

    private static void regionMessages(ServerPlayer player, PlayerData data) {
        Region region = Gielinor.region(player);
        int wild = Gielinor.wildernessLevel(player);
        String key = region.name() + (wild > 0 ? ":" + wild : "");
        if (!key.equals(data.lastRegion)) {
            boolean enteringWild = wild > 0 && !data.lastRegion.startsWith("WILDERNESS");
            data.lastRegion = key;
            if (wild > 0) {
                player.displayClientMessage(Component.literal("Level: " + wild).withStyle(ChatFormatting.YELLOW), true);
                if (enteringWild) {
                    Msg.red(player, "You have entered the Wilderness. Other players can attack you here!");
                }
            } else {
                player.displayClientMessage(Component.literal(region.displayName()).withStyle(ChatFormatting.GOLD), true);
            }
        }
    }

}
