package com.runecraft.network;

import com.runecraft.combat.MagicService;
import com.runecraft.combat.PrayerService;
import com.runecraft.menu.MenuService;
import com.runecraft.osrs.CombatStyle;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.osrs.Spell;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

public final class ServerNetworking {
    private ServerNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(Net.MENU_CHOICE, (server, player, handler, buf, sender) -> {
            String recipeId = buf.readString(128);
            int quantity = buf.readVarInt();
            server.execute(() -> MenuService.choose(player, recipeId, quantity));
        });
        ServerPlayNetworking.registerGlobalReceiver(Net.SET_STYLE, (server, player, handler, buf, sender) -> {
            int ordinal = buf.readVarInt();
            server.execute(() -> {
                if (ordinal >= 0 && ordinal < CombatStyle.values().length) {
                    Players.data(player).setStyle(CombatStyle.values()[ordinal]);
                    sync(player);
                }
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(Net.TOGGLE_PRAYER, (server, player, handler, buf, sender) -> {
            int ordinal = buf.readVarInt();
            server.execute(() -> {
                if (ordinal >= 0 && ordinal < Prayer.values().length) {
                    PrayerService.toggle(player, Prayer.values()[ordinal]);
                }
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(Net.CAST_SPELL, (server, player, handler, buf, sender) -> {
            int ordinal = buf.readVarInt();
            server.execute(() -> {
                if (ordinal >= 0 && ordinal < Spell.values().length) {
                    MagicService.castFromSpellbook(player, Spell.values()[ordinal]);
                }
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(Net.SET_AUTOCAST, (server, player, handler, buf, sender) -> {
            int ordinal = buf.readVarInt();
            server.execute(() -> MagicService.setAutocast(player, ordinal < 0 || ordinal >= Spell.values().length ? null : Spell.values()[ordinal]));
        });
    }

    /** Sends the full RuneCraft state (skills, prayer, style, pouch, slayer task) to the client. */
    public static void sync(ServerPlayerEntity player) {
        PlayerData data = Players.data(player);
        SkillSet skills = data.skills();
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(Skill.count());
        for (Skill s : Skill.values()) {
            buf.writeVarInt(skills.xpTenths(s));
            buf.writeVarInt(skills.boost(s));
        }
        buf.writeVarInt(data.prayerPoints());
        int mask = 0;
        for (Prayer p : data.activePrayers()) {
            mask |= 1 << p.ordinal();
        }
        buf.writeInt(mask);
        buf.writeVarInt(data.style().ordinal());
        buf.writeVarInt(data.autocast() == null ? -1 : data.autocast().ordinal());
        buf.writeVarLong(data.coins());
        buf.writeString(data.slayerTask() == null ? "" : data.slayerTask().name());
        buf.writeVarInt(data.slayerRemaining());
        buf.writeVarInt(com.runecraft.osrs.Quest.values().length);
        for (com.runecraft.osrs.Quest q : com.runecraft.osrs.Quest.values()) {
            buf.writeVarInt(data.questStage(q));
        }
        buf.writeVarInt(data.questPoints());
        buf.writeVarInt(data.collectionLog().size());
        for (String item : data.collectionLog()) {
            buf.writeString(item);
        }
        ServerPlayNetworking.send(player, Net.SYNC, buf);
    }

    public static void xpDrop(ServerPlayerEntity player, Skill skill, int tenths) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(skill.ordinal());
        buf.writeVarInt(tenths);
        ServerPlayNetworking.send(player, Net.XP_DROP, buf);
    }

    /** Shows a hitsplat on an entity to every player tracking it (and the attacker). */
    public static void hitsplat(Entity target, int damage, byte type) {
        if (!(target.getWorld() instanceof ServerWorld world)) {
            return;
        }
        float ratio = 1f;
        if (target instanceof net.minecraft.entity.LivingEntity living) {
            ratio = Math.max(0f, living.getHealth() / Math.max(1f, living.getMaxHealth()));
        }
        for (ServerPlayerEntity p : world.getPlayers()) {
            if (p.squaredDistanceTo(target) < 64 * 64) {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeVarInt(target.getId());
                buf.writeVarInt(damage);
                buf.writeByte(type);
                buf.writeFloat(ratio);
                ServerPlayNetworking.send(p, Net.HITSPLAT, buf);
            }
        }
    }

    public record MenuOption(Recipe recipe, boolean available) {
    }

    public static void openMenu(ServerPlayerEntity player, com.runecraft.osrs.MenuType type, List<MenuOption> options) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(type.ordinal());
        buf.writeVarInt(options.size());
        for (MenuOption o : options) {
            buf.writeString(o.recipe().id());
            buf.writeString(o.recipe().name());
            buf.writeString(com.runecraft.RuneCraft.resolve(o.recipe().icon()).toString());
            buf.writeVarInt(o.recipe().skill() == null ? 0 : o.recipe().level());
            buf.writeVarInt(o.recipe().skill() == null ? -1 : o.recipe().skill().ordinal());
            buf.writeBoolean(o.available());
        }
        ServerPlayNetworking.send(player, Net.OPEN_MENU, buf);
    }
}
