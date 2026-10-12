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

    public static double defence(PlayerData d) {
        return best(d, Prayer::defenceMultiplier);
    }

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
        ItemStack held = player.getMainHandStack();
        Bone bone = held.isEmpty() ? null : Bone.byItem(Inv.idOf(held));
        if (bone != null) {
            held.decrement(1);
            XpService.add(player, Skill.PRAYER, bone.xp() * Bone.ALTAR_MULTIPLIER);
            Msg.action(player, "The gods are very pleased with your offering.");
            player.swingHand(net.minecraft.util.Hand.MAIN_HAND, true);
            return;
        }
        int max = data.skills().currentLevel(Skill.PRAYER);
        if (data.prayerPoints() >= max) {
            Msg.game(player, "You already have full Prayer points.");
            return;
        }
        data.setPrayerPoints(max);
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.6f, 1.0f);
        Msg.game(player, "You recharge your Prayer points.");
        ServerNetworking.sync(player);
    }

    public static boolean inGielinor(ServerPlayerEntity player) {
        return Gielinor.is(player);
    }
}
