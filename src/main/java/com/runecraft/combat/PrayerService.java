package com.runecraft.combat;

import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.data.Bone;
import com.runecraft.osrs.data.PrayerAltars;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

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
    public static java.util.function.BiPredicate<ServerPlayer, Prayer> scrollRead = (p, prayer) -> true;

    /**
     * A minigame's prayer rules (Last Man Standing builds, No Prayer duels): null leaves the normal checks, "" allows the prayer
     * without its quest/scroll unlocks, any other text forbids it with that message. Set by the minigames module.
     */
    public static java.util.function.BiFunction<ServerPlayer, Prayer, String> minigameRule = (p, prayer) -> null;

    public static double rangedAttack(PlayerData d) {
        return best(d, Prayer::rangedAttackMultiplier);
    }

    public static double rangedStrength(PlayerData d) {
        return best(d, Prayer::rangedStrengthMultiplier);
    }

    public static double magic(PlayerData d) {
        return best(d, Prayer::magicMultiplier);
    }

    /**
     * Turn the prayer a player clicks into another (The Nightmare's curse shuffles the protection prayers); each returns the
     * prayer to toggle, or the one clicked.
     */
    public static final java.util.List<java.util.function.BiFunction<ServerPlayer, Prayer, Prayer>> REDIRECTS =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    /** The prayer a click on {@code prayer} switches after every {@link #REDIRECTS} entry (the Nightmare's curse). */
    private static Prayer redirect(ServerPlayer player, Prayer prayer) {
        for (java.util.function.BiFunction<ServerPlayer, Prayer, Prayer> r : REDIRECTS) {
            Prayer to = r.apply(player, prayer);
            prayer = to == null ? prayer : to;
        }
        return prayer;
    }

    /** The player clicks a prayer: it switches on (with the usual checks) or off. Redirects apply. */
    public static void toggle(ServerPlayer player, Prayer prayer) {
        toggleRedirected(player, redirect(player, prayer));
    }

    /**
     * Sets a prayer on or off; nothing happens when it already is (idempotent, so a key press that crosses a sync under
     * latency can't flip it back). Redirects apply first, then the normal toggle rules (level, quests, scrolls, points).
     * // playability: the hotkeys and quick-prayer keys send this instead of a toggle.
     */
    public static void set(ServerPlayer player, Prayer prayer, boolean on) {
        Prayer target = redirect(player, prayer);
        PlayerData data = Players.data(player);
        if (data.isActive(target) != on) {
            toggleRedirected(player, target);
        }
        if (data.isActive(target) != on || target != prayer) {
            // Refused (level, quest, points) or redirected: the client's optimistic HUD needs the real state.
            ServerNetworking.sync(player);
        }
    }

    /**
     * OSRS Quick-prayers: if any quick prayer is on, all of them switch off; otherwise each switches on through the normal
     * toggle (so locked or conflicting ones follow the usual rules). An empty set tells the player how to choose them.
     */
    public static void toggleQuick(ServerPlayer player) {
        PlayerData data = Players.data(player);
        java.util.Set<Prayer> quick = com.runecraft.osrs.PrayerGrace.fromMask(data.quickPrayers());
        if (quick.isEmpty()) {
            Msg.game(player, "You haven't selected any quick-prayers. Shift-click prayers in the Prayer tab to choose them.");
            return;
        }
        boolean anyOn = false;
        for (Prayer p : quick) {
            anyOn |= data.isActive(p);
        }
        for (Prayer p : quick) {
            if (anyOn) {
                if (data.isActive(p)) {
                    toggleRedirected(player, p);
                }
            } else if (!data.isActive(p)) {
                toggleRedirected(player, p);
            }
        }
        ServerNetworking.sync(player);
    }

    /** Sets the quick-prayer selection (a mask of Prayer ordinals); conflicting prayers keep the last one picked. */
    public static void setQuickPrayers(ServerPlayer player, long mask) {
        PlayerData data = Players.data(player);
        data.setQuickPrayers(mask);
        ServerNetworking.sync(player);
    }

    /** Stamps a voluntary switch of a protection overhead for the switch grace (others are ignored). */
    private static void stamp(PlayerData data, Prayer prayer, boolean on) {
        int i = com.runecraft.osrs.PrayerGrace.index(prayer);
        if (i < 0) {
            return;
        }
        long now = com.runecraft.action.ActionManager.mcTick();
        if (on) {
            data.overheadOnAt[i] = now;
        } else {
            data.overheadOffAt[i] = now;
        }
    }

    private static void toggleRedirected(ServerPlayer player, Prayer prayer) {
        PlayerData data = Players.data(player);
        if (data.isActive(prayer)) {
            data.activePrayers().remove(prayer);
            stamp(data, prayer, false);
            player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.3f, 1.6f);
        } else {
            if (data.skills().level(Skill.PRAYER) < prayer.level()) {
                Msg.game(player, "You need a Prayer level of " + prayer.level() + " to use " + prayer.displayName() + ".");
                return;
            }
            String rule = minigameRule.apply(player, prayer);
            if (rule != null && !rule.isEmpty()) {
                Msg.game(player, rule);
                return;
            }
            String locked = rule != null ? null : com.runecraft.osrs.content.quests.QuestUnlocks.prayer(prayer, data.skills(), id -> {
                com.runecraft.osrs.QuestInfo q = com.runecraft.osrs.content.ContentRegistry.quest(id);
                return q == null || data.questComplete(q);
            });
            if (locked != null) {
                Msg.game(player, locked);
                return;
            }
            if (rule == null && prayer.unlockScroll() != null && !scrollRead.test(player, prayer)) {
                com.runecraft.osrs.ItemDef scroll = com.runecraft.osrs.ItemCatalog.get(prayer.unlockScroll());
                Msg.game(player, "You need to learn " + prayer.displayName() + " from "
                        + (scroll == null ? "a prayer scroll" : Msg.article(scroll.name().toLowerCase(java.util.Locale.ROOT))) + " first.");
                return;
            }
            if (data.prayerPoints() <= 0) {
                Msg.game(player, "You have run out of Prayer points, you can recharge at an altar.");
                return;
            }
            // Switching overheads is voluntary too: the one replaced gets the switch grace.
            data.activePrayers().removeIf(other -> {
                if (prayer.conflictsWith(other)) {
                    stamp(data, other, false);
                    return true;
                }
                return false;
            });
            data.activePrayers().add(prayer);
            stamp(data, prayer, true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.3f, 1.6f);
        }
        ServerNetworking.sync(player);
    }

    /** Called every game tick. */
    public static void gameTick(ServerPlayer player, long gameTick) {
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

    /** Praying at an altar recharges Prayer points (no bone offering; see {@link #useAltar(ServerPlayer, BlockPos)}). */
    public static void useAltar(ServerPlayer player) {
        useAltar(player, null);
    }

    /**
     * A world altar at {@code pos}: the Chaos Altar (350%, 50% chance to keep the bone) and the Ectofuntus (400%) take the
     * held bone as an offering; any other altar only recharges Prayer points, as in OSRS (bones there are buried instead).
     */
    public static void useAltar(ServerPlayer player, BlockPos pos) {
        PlayerData data = Players.data(player);
        PrayerAltars.Kind kind = pos == null || !Gielinor.is(player) ? PrayerAltars.Kind.REGULAR
                : PrayerAltars.at(GielinorMap.toOsrsX(pos.getX()), GielinorMap.toOsrsY(pos.getZ()));
        if (kind != PrayerAltars.Kind.REGULAR) {
            if (offerHeldBone(player, kind.multiplier, kind == PrayerAltars.Kind.CHAOS ? PrayerAltars.CHAOS_SAVE_CHANCE : 0.0)) {
                return;
            }
        } else if (!player.getMainHandItem().isEmpty() && Bone.byItem(Inv.idOf(player.getMainHandItem())) != null) {
            Msg.game(player, "The gods won't accept offerings at this altar. Use a gilded altar, the Chaos Altar or the Ectofuntus,"
                    + " or bury your bones.");
        }
        // Altars restore Prayer points to the base level (a boosted or drained level doesn't change the cap).
        int max = data.skills().level(Skill.PRAYER);
        if (data.prayerPoints() >= max) {
            Msg.game(player, "You already have full Prayer points.");
            return;
        }
        data.setPrayerPoints(max);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6f, 1.0f);
        Msg.game(player, "You recharge your Prayer points.");
        ServerNetworking.sync(player);
    }

    /**
     * Offers the bone held in the main hand at an altar for {@code multiplier} times its burying XP (house altars pass their
     * own multiplier, e.g. 3.5 for a gilded altar with two lit burners). Returns false if no bone is held.
     */
    public static boolean offerHeldBone(ServerPlayer player, double multiplier) {
        return offerHeldBone(player, multiplier, 0.0);
    }

    private static boolean offerHeldBone(ServerPlayer player, double multiplier, double saveChance) {
        ItemStack held = player.getMainHandItem();
        Bone bone = held.isEmpty() ? null : Bone.byItem(Inv.idOf(held));
        if (bone == null) {
            return false;
        }
        boolean saved = saveChance > 0 && player.getRandom().nextDouble() < saveChance;
        if (!saved) {
            held.shrink(1);
        }
        XpService.add(player, Skill.PRAYER, bone.xp() * multiplier);
        Msg.action(player, multiplier >= 3 ? "The gods are very pleased with your offering." : "The gods are pleased with your offering.");
        if (saved) {
            Msg.game(player, "The Dark Lord spares your bones.");
        }
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        return true;
    }

    public static boolean inGielinor(ServerPlayer player) {
        return Gielinor.is(player);
    }
}
