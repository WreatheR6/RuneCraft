package com.runecraft.combat;

import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.data.Bone;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.function.ToDoubleFunction;

/** Prayer points, toggling, drain (OSRS drain-resistance model) and altars. */
public final class PrayerService {
    private PrayerService() {
    }

    private static double best(PlayerData data, ToDoubleFunction<Prayer> f) {
        double m = 1.0;
        for (Prayer p : data.activePrayers()) {
            m = Math.max(m, f.applyAsDouble(p));
        }
        return m;
    }

    public static double attack(PlayerData d) {
        return best(d, Prayer::attackMultiplier);
    }

    public static double strength(PlayerData d) {
        return best(d, Prayer::strengthMultiplier);
    }

    /** Best defence prayer plus Deadeye's / Mystic Vigour's stacking +5%. */
    public static double defence(PlayerData d) {
        return Prayer.defenceMultiplier(d.activePrayers());
    }

    /**
     * Whether the player has read the scroll a prayer needs (Rigour, Augury, Preserve, Deadeye, Mystic Vigour).
     * The systems module stores the scrolls read; without it every prayer counts as learnt.
     */
    public static java.util.function.BiPredicate<ServerPlayerEntity, Prayer> scrollRead = (p, prayer) -> true;

    public static double rangedAttack(PlayerData d) {
        return best(d, Prayer::rangedAttackMultiplier);
    }

    public static double rangedStrength(PlayerData d) {
        return best(d, Prayer::rangedStrengthMultiplier);
    }

    public static double magic(PlayerData d) {
        return best(d, Prayer::magicMultiplier);
    }

    public static void toggle(ServerPlayerEntity player, Prayer prayer) {
        PlayerData data = Players.data(player);
        if (data.isActive(prayer)) {
            data.activePrayers().remove(prayer);
        } else {
            if (data.skills().level(Skill.PRAYER) < prayer.level()) {
                Msg.game(player, "You need a Prayer level of " + prayer.level() + " to use " + prayer.displayName() + ".");
                return;
            }
            String locked = com.runecraft.osrs.content.quests.QuestUnlocks.prayer(prayer, data.skills(), id -> {
                com.runecraft.osrs.QuestInfo q = com.runecraft.osrs.content.ContentRegistry.quest(id);
                return q == null || data.questComplete(q);
            });
            if (locked != null) {
                Msg.game(player, locked);
                return;
            }
            if (prayer.unlockScroll() != null && !scrollRead.test(player, prayer)) {
                com.runecraft.osrs.ItemDef scroll = com.runecraft.osrs.ItemCatalog.get(prayer.unlockScroll());
                Msg.game(player, "You need to learn " + prayer.displayName() + " from "
                        + (scroll == null ? "a prayer scroll" : Msg.article(scroll.name().toLowerCase(java.util.Locale.ROOT))) + " first.");
                return;
            }
            if (data.prayerPoints() <= 0) {
                Msg.game(player, "You have run out of Prayer points, you can recharge at an altar.");
                return;
            }
            data.activePrayers().removeIf(prayer::conflictsWith);
            data.activePrayers().add(prayer);
            player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 0.3f, 1.6f);
        }
        ServerNetworking.sync(player);
    }

    /** Called every game tick. */
    public static void gameTick(ServerPlayerEntity player, long gameTick) {
        PlayerData data = Players.data(player);
        if (data.activePrayers().isEmpty()) {
            return;
        }
        PrayerEffectsService.redemption(player, data);
        if (data.activePrayers().isEmpty()) {
            return;
        }
        int drain = 0;
        for (Prayer p : data.activePrayers()) {
            drain += p.drainEffect();
        }
        int resistance = 60 + 2 * Equipment.total(player, data.skills()).prayer();
        int counter = data.prayerDrainCounter() + drain;
        boolean changed = false;
        while (counter > resistance) {
            counter -= resistance;
            data.setPrayerPoints(data.prayerPoints() - 1);
            changed = true;
        }
        data.setPrayerDrainCounter(counter);
        if (data.prayerPoints() <= 0) {
            data.activePrayers().clear();
            Msg.game(player, "You have run out of Prayer points, you can recharge at an altar.");
            changed = true;
        }
        if (changed) {
            ServerNetworking.sync(player);
        }
    }

    /** Altar: offer bones held in hand (350% XP) or recharge prayer points. */
    public static void useAltar(ServerPlayerEntity player) {
        PlayerData data = Players.data(player);
        if (offerHeldBone(player, Bone.ALTAR_MULTIPLIER)) {
            return;
        }
        // Altars restore Prayer points to the base level (a boosted or drained level doesn't change the cap).
        int max = data.skills().level(Skill.PRAYER);
        if (data.prayerPoints() >= max) {
            Msg.game(player, "You already have full Prayer points.");
            return;
        }
        data.setPrayerPoints(max);
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.6f, 1.0f);
        Msg.game(player, "You recharge your Prayer points.");
        ServerNetworking.sync(player);
    }

    /**
     * Offers the bone held in the main hand at an altar for {@code multiplier} times its burying XP (house altars pass their
     * own multiplier, e.g. 3.5 for a gilded altar with two lit burners). Returns false if no bone is held.
     */
    public static boolean offerHeldBone(ServerPlayerEntity player, double multiplier) {
        ItemStack held = player.getMainHandStack();
        Bone bone = held.isEmpty() ? null : Bone.byItem(Inv.idOf(held));
        if (bone == null) {
            return false;
        }
        held.decrement(1);
        XpService.add(player, Skill.PRAYER, bone.xp() * multiplier);
        Msg.action(player, multiplier >= 3 ? "The gods are very pleased with your offering." : "The gods are pleased with your offering.");
        player.swingHand(net.minecraft.util.Hand.MAIN_HAND, true);
        return true;
    }

    public static boolean inGielinor(ServerPlayerEntity player) {
        return Gielinor.is(player);
    }
}
