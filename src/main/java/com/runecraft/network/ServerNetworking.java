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
import com.runecraft.platform.PacketByteBufs;
import com.runecraft.platform.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import java.util.List;

public final class ServerNetworking {
    private ServerNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(Net.MENU_CHOICE, (server, player, handler, buf, sender) -> {
            String recipeId = buf.readUtf(128);
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
        ServerPlayNetworking.registerGlobalReceiver(Net.TOGGLE_RETALIATE, (server, player, handler, buf, sender) ->
                server.execute(() -> com.runecraft.combat.AutoRetaliate.toggle(player)));
        ServerPlayNetworking.registerGlobalReceiver(Net.SET_AUTOCAST, (server, player, handler, buf, sender) -> {
            int ordinal = buf.readVarInt();
            server.execute(() -> MagicService.setAutocast(player, ordinal < 0 || ordinal >= Spell.values().length ? null : Spell.values()[ordinal]));
        });
        // Prayer hotkeys, quick-prayers and the side panel (round 41 playability).
        ServerPlayNetworking.registerGlobalReceiver(Net.PRAYER_SET, (server, player, handler, buf, sender) -> {
            int ordinal = buf.readVarInt();
            boolean on = buf.readBoolean();
            server.execute(() -> {
                if (ordinal >= 0 && ordinal < Prayer.values().length) {
                    PrayerService.set(player, Prayer.values()[ordinal], on);
                }
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(Net.PRAYER_QUICK, (server, player, handler, buf, sender) ->
                server.execute(() -> PrayerService.toggleQuick(player)));
        ServerPlayNetworking.registerGlobalReceiver(Net.PRAYER_QUICK_SET, (server, player, handler, buf, sender) -> {
            long mask = buf.readVarLong();
            server.execute(() -> PrayerService.setQuickPrayers(player, mask));
        });
        com.runecraft.combat.FreezeRules.register();
        com.runecraft.skill.ConsumeCooldowns.register();
    }

    /** Sends the full RuneCraft state (skills, prayer, style, pouch, slayer task) to the client. */
    public static void sync(ServerPlayer player) {
        PlayerData data = Players.data(player);
        SkillSet skills = data.skills();
        FriendlyByteBuf buf = PacketByteBufs.create();
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
        buf.writeUtf(data.slayerTask() == null ? "" : data.slayerTask().name());
        buf.writeVarInt(data.slayerRemaining());
        java.util.List<com.runecraft.osrs.QuestInfo> quests = com.runecraft.osrs.content.ContentRegistry.quests();
        buf.writeVarInt(quests.size());
        for (com.runecraft.osrs.QuestInfo q : quests) {
            buf.writeVarInt(data.questStage(q));
        }
        buf.writeVarInt(data.questPoints());
        buf.writeVarInt(data.collectionLog().size());
        for (String item : data.collectionLog()) {
            buf.writeUtf(item);
        }
        buf.writeVarInt(data.killCounts().size());
        data.killCounts().forEach((key, kc) -> {
            buf.writeUtf(key);
            buf.writeVarInt(kc);
        });
        buf.writeVarLong(data.tokkul());
        buf.writeVarLong(data.quickPrayers());
        ServerPlayNetworking.send(player, Net.SYNC, buf);
    }

    public static void xpDrop(ServerPlayer player, Skill skill, int tenths) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(skill.ordinal());
        buf.writeVarInt(tenths);
        ServerPlayNetworking.send(player, Net.XP_DROP, buf);
    }

    /** One hitsplat as sent: target and attacker entity ids (-1 for none), the server MC tick. */
    public record Splat(int target, int damage, byte type, int attacker, long mcTick) {
    }

    /** Self-test hook: the last hitsplat sent (or that would have been sent), whoever was watching. */
    public static volatile Splat LAST_SPLAT;

    /** Shows a hitsplat on an entity to every player tracking it (and the attacker). */
    public static void hitsplat(Entity target, int damage, byte type) {
        hitsplat(target, damage, type, null);
    }

    /**
     * Shows a hitsplat on an entity to every player within 64 blocks, naming the attacker (null for none) so the client can
     * outline the local player's own hits and point at whoever hit them.
     */
    public static void hitsplat(Entity target, int damage, byte type, Entity attacker) {
        int attackerId = attacker == null ? -1 : attacker.getId();
        LAST_SPLAT = new Splat(target.getId(), damage, type, attackerId, com.runecraft.action.ActionManager.mcTick());
        if (!(target.level() instanceof ServerLevel world)) {
            return;
        }
        float ratio = 1f;
        if (target instanceof net.minecraft.world.entity.LivingEntity living) {
            ratio = Math.max(0f, living.getHealth() / Math.max(1f, living.getMaxHealth()));
        }
        for (ServerPlayer p : world.players()) {
            if (p.distanceToSqr(target) < 64 * 64) {
                FriendlyByteBuf buf = PacketByteBufs.create();
                buf.writeVarInt(target.getId());
                buf.writeVarInt(damage);
                buf.writeByte(type);
                buf.writeFloat(ratio);
                buf.writeVarInt(attackerId);
                ServerPlayNetworking.send(p, Net.HITSPLAT, buf);
            }
        }
    }

    public record MenuOption(Recipe recipe, boolean available) {
    }

    public static void openMenu(ServerPlayer player, com.runecraft.osrs.MenuType type, List<MenuOption> options) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(type.ordinal());
        buf.writeVarInt(options.size());
        for (MenuOption o : options) {
            buf.writeUtf(o.recipe().id());
            buf.writeUtf(o.recipe().name());
            buf.writeUtf(com.runecraft.RuneCraft.resolve(o.recipe().icon()).toString());
            buf.writeVarInt(o.recipe().skill() == null ? 0 : o.recipe().level());
            buf.writeVarInt(o.recipe().skill() == null ? -1 : o.recipe().skill().ordinal());
            buf.writeBoolean(o.available());
        }
        ServerPlayNetworking.send(player, Net.OPEN_MENU, buf);
    }
}
