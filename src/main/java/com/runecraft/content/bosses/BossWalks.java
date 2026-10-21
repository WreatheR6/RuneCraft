package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.combat.CombatHandler;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.bosses.BossMechanics;
import com.runecraft.osrs.content.bosses.LeviathanRules;
import com.runecraft.osrs.content.bosses.MoonsRules;
import com.runecraft.osrs.content.bosses.WhispererRules;
import com.runecraft.util.Inv;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Phase walks of the modern bosses for the in-game self-test ({@code boss:walk:<id>}, selftest/BossWalkChecks): a player
 * (the real one in the client walkthrough) fights the boss through every phase, and each mechanic must actually fire and be
 * answerable the way OSRS answers it (step into the Shadow Realm, step on the light seeds, hide, kill the right souls...).
 * Each walk returns a summary of what happened, or throws {@link IllegalStateException} naming the step that broke.
 */
public final class BossWalks {
    private BossWalks() {
    }

    /** What the self-test provides: game ticks of the fight (the boss and its mechanics; the player kept alive). */
    public interface Driver {
        /** Runs {@code n} game ticks of the boss and the pending mechanics, keeping the player alive. */
        void ticks(DataBossEntity boss, int n);

        ServerPlayer player();

        /** Moves the player to a spot (facing the boss). */
        void moveTo(Vec3 at, DataBossEntity boss);

        /** Damage the player has taken since the last {@link #clearDamage} from attacks of this name (rolled, before healing). */
        int damage(String attack);

        /** The biggest single hit of this attack name since the last {@link #clearDamage}. */
        int biggest(String attack);

        void clearDamage();

        /**
         * Walks the player to a spot in real time (round 44): at a human's pace through the arena's collision, the fight ticking
         * meanwhile (selftest/RealPlayer), as a player answers a mechanic. {@link #moveTo} stays for setting the scene.
         */
        default void walkTo(Vec3 at, DataBossEntity boss) {
            moveTo(at, boss);
        }

        /**
         * The player switches a protection prayer on through the prayer key a human's reaction time after now (round 44:
         * the walk read the cue now; selftest/RealPlayer.react). Without a real-player helper it switches at once.
         */
        default void react(com.runecraft.osrs.Prayer prayer) {
            pray(player(), prayer);
        }

        /** Real walks keep {@code radius} blocks from these spots (a dark seed, a pool) until {@link #clearAvoid}. */
        default void avoid(List<Vec3> spots, double radius) {
        }

        default void clearAvoid() {
        }

        /**
         * Runs {@code n} Minecraft ticks of the fight (a player's reaction to what it just saw: round 44); without a real-player
         * driver, whole game ticks covering them.
         */
        default void mcTicks(DataBossEntity boss, int n) {
            ticks(boss, (n + com.runecraft.action.ActionManager.MC_TICKS_PER_GAME_TICK - 1)
                    / com.runecraft.action.ActionManager.MC_TICKS_PER_GAME_TICK);
        }

        /**
         * Keeps a protection prayer up (null: stop): switched on now, and on again a reaction time after anything switches it
         * off (a prayer disable). Without a real-player driver it is only switched on now.
         */
        default void keep(com.runecraft.osrs.Prayer prayer) {
            if (prayer != null) {
                pray(player(), prayer);
            }
        }

        /** Times a kept prayer was switched off and on again. */
        default int prayerRestores() {
            return 0;
        }

        /**
         * Answers an area attack on a cue the walk read itself (round 44): the cue came {@code sinceCueMc} Minecraft ticks ago
         * and the attack lands {@code landsInMc} from now; a reaction time after the cue the player walks to {@code safe}. The
         * walk out is judged against the cue's whole lead, failing the walk with the attack's name when a player reacting at
         * once couldn't make it.
         */
        default void walkOut(String name, Vec3 safe, int sinceCueMc, int landsInMc, DataBossEntity boss) {
            walkTo(safe, boss);
        }
    }

    /** Minecraft ticks since the boss's last game tick (what the walk saw at the end of a game tick happened then). */
    static int sinceGameTick(DataBossEntity boss) {
        return Math.floorMod(boss.tickCount, com.runecraft.action.ActionManager.MC_TICKS_PER_GAME_TICK);
    }

    /**
     * Runs the fight on to just after the boss's next game tick (round 44), so a special the walk starts itself starts where the
     * boss's own game tick would start it and keeps its whole lead.
     */
    static void alignToGameTick(DataBossEntity boss, Driver d) {
        int k = sinceGameTick(boss);
        if (k != 0) {
            d.mcTicks(boss, com.runecraft.action.ActionManager.MC_TICKS_PER_GAME_TICK - k);
        }
    }

    /** A human's reaction time in Minecraft ticks (round 44): what a walk waits between seeing a cue and moving. */
    static final int REACTION_MC = com.runecraft.osrs.RealReaction.REACTION_MC;

    private static IllegalStateException fail(String message) {
        return new IllegalStateException(message);
    }

    /** Runs game ticks until {@code done} holds (at most {@code max}); returns the ticks it took, or -1. */
    static int until(DataBossEntity boss, Driver d, int max, BooleanSupplier done) {
        for (int t = 0; t < max; t++) {
            if (done.getAsBoolean()) {
                return t;
            }
            d.ticks(boss, 1);
        }
        return done.getAsBoolean() ? max : -1;
    }

    /** A hit through the boss's own damage rules (immunities, special damage reduction); returns what it took. */
    static int hit(DataBossEntity boss, ServerPlayer p, int damage) {
        int dmg = boss.modifyIncoming(p, damage, AttackType.SLASH);
        return dmg > 0 ? CombatHandler.applyDamage(p, boss, dmg) : 0;
    }

    /** A player melee attack on a creature through the real combat path (attack delay waived). */
    static void attack(ServerPlayer p, net.minecraft.world.entity.LivingEntity target) {
        com.runecraft.player.Players.data(p).lastAttackTick = com.runecraft.action.ActionManager.mcTick() - 10_000;
        target.hurt(CombatHandler.source(p), 1f);
    }

    /** Sets the boss to a share of its hitpoints (OSRS terms: the Minecraft health scaled). */
    static void setPercent(DataBossEntity boss, double percent) {
        boss.setHealth(Math.max(1f, (float) (boss.getMaxHealth() * percent / 100.0)));
    }

    // =====================================================================================================
    // The Whisperer
    // =====================================================================================================

    /**
     * The Whisperer: her three specials at 80/55/30% (awakened: her first three of eight, at ~2,400/2,120/1,820 hitpoints,
     * {@link #whispererSpecialSteps}) answered through the Shadow Realm (the fragment glows only then; the
     * realm drains sanity; souls die only to realm hits; stepping on every light seed withers them; hiding behind her
     * columns), none of them running out; then the enrage pulls the player into the realm, and she dies.
     */
    public static String whisperer(DataBossEntity boss, Driver d) {
        if (!(boss.script() instanceof WhispererScript w)) {
            throw fail("the Whisperer has no WhispererScript");
        }
        ServerPlayer p = d.player();
        Inv.give(p, WhispererRules.FRAGMENT, 1);
        Vec3 c = w.homeVec();
        d.moveTo(c.add(0, 0, -8), boss);
        d.ticks(boss, 4);
        if (WhispererScript.activateFragment(p)) {
            throw fail("the fragment let the player into the Shadow Realm with no special running");
        }
        List<String> log = new ArrayList<>();
        for (double pct : whispererSpecialSteps(boss)) {
            setPercent(boss, pct);
            if (until(boss, d, 6, () -> w.selfTestSpecialName() != null) < 0) {
                throw fail("no special at " + pct + "% (specials so far: " + log + ")");
            }
            String special = w.selfTestSpecialName();
            int failures = w.selfTestFailures();
            switch (special) {
                case "seeds" -> log.add(walkSeeds(boss, w, d));
                case "siphon" -> log.add(walkSiphon(boss, w, d));
                default -> log.add(walkScreech(boss, w, d));
            }
            String next = w.selfTestSpecialName();
            if (next != null && !next.equals(special)) {
                throw fail(special + " answered, but the " + next + " started at once: her hitpoints (" + Math.round(pct)
                        + "%) were already past the next special's threshold");
            }
            if (next != null) {
                throw fail(special + " still running after it was answered");
            }
            if (w.selfTestFailures() > failures) {
                throw fail(special + " ran out on the player although it was answered");
            }
            if (w.inRealm(p)) {
                throw fail("still in the Shadow Realm after the " + special);
            }
            d.walkTo(c.add(0, 0, -8), boss); // back to the fight spot, on foot
            d.ticks(boss, 10); // her binding orb and a volley or two
        }
        // the enrage: a lethal hit leaves her at 1, she heals and pulls the player into the realm
        setPercent(boss, 2);
        hit(boss, p, 5000);
        d.ticks(boss, 2);
        if (!w.selfTestEnraged()) {
            throw fail("no enrage at 0 hitpoints (" + Math.round(boss.getHealth()) + " health left, alive " + boss.isAlive() + ")");
        }
        if (!w.inRealm(p)) {
            throw fail("the enrage didn't pull the player into the Shadow Realm");
        }
        int realmTicks = w.realmTicks(p);
        d.ticks(boss, 6);
        if (w.realmTicks(p) != realmTicks) {
            throw fail("the enrage's Shadow Realm drained sanity like a visit (Dark Memories counted it)");
        }
        for (int i = 0; i < 20 && boss.isAlive(); i++) {
            hit(boss, p, 200);
            d.ticks(boss, 1);
        }
        if (boss.isAlive()) {
            throw fail("the enraged Whisperer survived 20 heavy hits");
        }
        return String.join("; ", log) + "; enraged into the realm, killed";
    }

    /**
     * Where the walk sets her hitpoints for her first three specials: just under each threshold of the fight it walks. The
     * normal fight's are 80/55/30%; the awakened one (OSRS Wiki "The Whisperer/Strategies", Awakened mode) has eight, at
     * about 2,400, 2,120, 1,820... of her 2,700 hitpoints. When a special ends with her already past the next threshold she
     * starts that one at once ("she will immediately initiate it rather than launch the projectile"), so the normal fight's
     * percentages would chain two awakened specials (54% is 1,458, past both 2,120 and 1,820).
     */
    private static double[] whispererSpecialSteps(DataBossEntity boss) {
        double[] at = new double[BossMechanics.WHISPERER_SPECIAL_PERCENT.length];
        for (int i = 0; i < at.length; i++) {
            at[i] = boss.awakened()
                    ? (BossMechanics.WHISPERER_AWAKENED_SPECIAL_HP[i] - 20) * 100.0 / BossMechanics.WHISPERER_AWAKENED_HP
                    : BossMechanics.WHISPERER_SPECIAL_PERCENT[i] - 1;
        }
        return at;
    }

    static String walkSeeds(DataBossEntity boss, WhispererScript w, Driver d) {
        ServerPlayer p = d.player();
        if (!WhispererScript.activateFragment(p) || !w.inRealm(p)) {
            throw fail("seeds: the glowing fragment didn't take the player into the Shadow Realm");
        }
        int before = w.sanity(p);
        d.ticks(boss, 2);
        int peek = w.sanity(p);
        if (peek >= before) {
            throw fail("seeds: the Shadow Realm didn't drain sanity (" + before + " -> " + peek + ")");
        }
        WhispererScript.activateFragment(p); // out again: the light seeds are memorised and stepped on from the real world
        if (w.inRealm(p)) {
            throw fail("seeds: activating the fragment again didn't leave the realm");
        }
        List<Vec3> light = w.selfTestLightSeeds();
        if (light.isEmpty()) {
            throw fail("seeds: no light seeds to step on");
        }
        int n = light.size();
        // Round 44: walked in real time, nearest seed first, routed round the dark seeds the realm showed (a seed bursts the
        // moment a player is on it).
        d.avoid(w.selfTestDarkSeeds(), 1.25);
        List<Vec3> left = new ArrayList<>(light);
        try {
            while (!left.isEmpty() && w.selfTestSpecialName() != null) {
                Vec3 next = left.get(0);
                for (Vec3 s : left) {
                    if (s.distanceToSqr(p.position()) < next.distanceToSqr(p.position())) {
                        next = s;
                    }
                }
                d.walkTo(next, boss);
                left.remove(next);
                left.retainAll(w.selfTestLightSeeds()); // the ones crossed on the way burst too
            }
        } finally {
            d.clearAvoid();
        }
        d.ticks(boss, 1);
        return "seeds: realm peek " + before + "->" + peek + "% sanity, " + n + " light seeds stepped on, sanity " + w.sanity(p) + "%";
    }

    static String walkSiphon(DataBossEntity boss, WhispererScript w, Driver d) {
        ServerPlayer p = d.player();
        List<Mob> souls = new ArrayList<>(w.selfTestSouls());
        if (souls.isEmpty()) {
            throw fail("siphon: no lost souls");
        }
        Mob first = souls.get(0);
        attack(p, first);
        if (!first.isAlive()) {
            throw fail("siphon: a lost soul died to a hit from outside the Shadow Realm");
        }
        if (!WhispererScript.activateFragment(p)) {
            throw fail("siphon: the glowing fragment didn't take the player into the Shadow Realm");
        }
        int killed = 0;
        for (Mob soul : souls) {
            if (soul.isAlive()) {
                d.walkTo(soul.position().add(1.2, 0, 0), boss); // round 44: from soul to soul on foot
                attack(p, soul);
                if (soul.isAlive()) {
                    throw fail("siphon: a lost soul survived a hit from inside the Shadow Realm");
                }
                killed++;
            }
        }
        d.ticks(boss, 2);
        return "siphon: souls immune from outside, " + killed + " killed from the realm";
    }

    static String walkScreech(DataBossEntity boss, WhispererScript w, Driver d) {
        ServerPlayer p = d.player();
        if (!WhispererScript.activateFragment(p)) {
            throw fail("screech: the glowing fragment didn't take the player into the Shadow Realm");
        }
        d.ticks(boss, 1); // a look at the columns' health
        WhispererScript.activateFragment(p);
        d.clearDamage();
        // Round 44: the player walks to the next column a reaction time after the last one broke, choosing the nearest of the
        // weakest (each screech breaks the one hidden behind and takes 20 from the rest).
        Vec3[] hidingAt = {null};
        int ticks = until(boss, d, 40, () -> {
            Vec3 spot = w.selfTestHidingSpot(boss, p.position());
            if (spot != null && (hidingAt[0] == null || spot.distanceToSqr(hidingAt[0]) > 0.04)) {
                if (hidingAt[0] == null) {
                    d.walkTo(spot, boss); // the first column: the special's own wind-up gives the time
                } else {
                    // the column broke with the last screech: the next one comes 4 game ticks after it
                    int since = sinceGameTick(boss);
                    d.walkOut("screech", spot, since, 4 * com.runecraft.action.ActionManager.MC_TICKS_PER_GAME_TICK - since, boss);
                }
                hidingAt[0] = spot;
            }
            return w.selfTestSpecialName() == null;
        });
        if (ticks < 0) {
            throw fail("screech: still screeching after 40 ticks");
        }
        if (d.damage("screech") > 0) {
            throw fail("screech: hiding behind the weakest standing column still took " + d.damage("screech") + " screech damage");
        }
        return "screech: hid behind the columns, over in " + ticks + " ticks";
    }

    // =====================================================================================================
    // The Moons of Peril
    // =====================================================================================================

    /** Faces the player toward a point (the Eclipse Moon's clones are parried by facing them). */
    static void face(ServerPlayer p, Vec3 at) {
        float yaw = (float) (Math.toDegrees(Math.atan2(at.z - p.getZ(), at.x - p.getX())) - 90.0);
        p.teleportTo(p.serverLevel(), p.getX(), p.getY(), p.getZ(), yaw, p.getXRot());
    }

    /** Stands on Eyatlalli's glyph and lets her make standard attacks; the glyph moving on, and standing off it, counted. */
    private static String moonStandard(DataBossEntity boss, MoonScripts.Moon m, Driver d) {
        ServerPlayer p = d.player();
        d.moveTo(m.glyphPos(boss), boss);
        int attacks = m.attackCount;
        // round 44: the glyph moves on with her attacks; the player walks after it a reaction time after it lights up
        Vec3[] lit = {m.glyphPos(boss)};
        until(boss, d, 14, () -> {
            Vec3 g = m.glyphPos(boss);
            if (g.distanceToSqr(lit[0]) > 0.04) {
                // standing off the new glyph counts from two game ticks after it moved (BossRealtime.offGlyphCounts)
                lit[0] = g;
                int since = sinceGameTick(boss);
                d.walkOut("glyph", g, since, (com.runecraft.osrs.content.bosses.BossRealtime.GLYPH_MOVE_GRACE_TICKS + 1)
                        * com.runecraft.action.ActionManager.MC_TICKS_PER_GAME_TICK - since, boss);
            } else {
                d.walkTo(g, boss);
            }
            return m.attackCount > attacks;
        });
        if (m.attackCount <= attacks) {
            throw fail("no standard attack in 14 ticks");
        }
        d.ticks(boss, 3); // its three hits land a tick apart
        int glyph = m.glyph;
        if (m.offGlyphTicks(p) != 0) {
            throw fail("standing on the glyph counted as off it");
        }
        Vec3 off = m.glyphPos(boss, (glyph + 4) % 8);
        d.moveTo(off, boss);
        d.ticks(boss, 2);
        int offTicks = m.offGlyphTicks(p);
        if (offTicks < 2) {
            throw fail("2 ticks off the glyph were counted as " + offTicks + " (no glyph damage)");
        }
        d.walkTo(m.glyphPos(boss), boss);
        d.ticks(boss, 1);
        return "standard attack, glyph " + glyph + ", " + offTicks + " ticks off it hurt (curse " + m.curse(p).stacks() + ")";
    }

    /** Forces the full (true) or new moon special and waits for it to start. */
    private static MoonsRules.Special moonSpecial(DataBossEntity boss, MoonScripts.Moon m, Driver d, boolean full) {
        if (full) {
            m.selfTestSpecialNow();
        } else {
            m.selfTestNewMoonNow();
        }
        if (until(boss, d, 8, () -> m.special != null) < 0) {
            throw fail("no special started after the " + (full ? "full" : "new") + " moon");
        }
        return m.special;
    }

    /** Heavy hits through her rules until she dies (specials may come between); a Moon's loot waits in the Lunar Chest. */
    private static void moonKill(DataBossEntity boss, Driver d, int before) {
        ServerPlayer p = d.player();
        for (int i = 0; i < 80 && boss.isAlive(); i++) {
            if (boss.script().isVulnerable(boss)) {
                hit(boss, p, 120);
            }
            d.ticks(boss, 1);
        }
        if (boss.isAlive()) {
            throw fail("still alive after 80 ticks of heavy hits (" + Math.round(boss.getHealth()) + " health)");
        }
        if (LunarChest.pending(p) <= before) {
            throw fail("killed, but the kill wasn't recorded for the Lunar Chest");
        }
    }

    /** The Blue Moon: Weapon Freeze (punch the glowing block free) and Frost Storm (relight both braziers). */
    public static String blueMoon(DataBossEntity boss, Driver d) {
        if (!(boss.script() instanceof MoonScripts.BlueMoon m)) {
            throw fail("the Blue Moon has no BlueMoon script");
        }
        ServerPlayer p = d.player();
        int pendingBefore = LunarChest.pending(p);
        List<String> log = new ArrayList<>();
        log.add(moonStandard(boss, m, d));
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, Inv.stack("abyssal_whip", 1));
        if (moonSpecial(boss, m, d, true) != MoonsRules.Special.WEAPON_FREEZE) {
            throw fail("the full moon didn't bring Weapon Freeze but " + m.special);
        }
        if (!p.getMainHandItem().isEmpty() || !Inv.has(p, "abyssal_whip", 1)) {
            throw fail("freeze: the weapon wasn't frozen out of the hand into the pack");
        }
        Mob block = m.selfTestRealBlock();
        if (block == null) {
            throw fail("freeze: no glowing block of ice");
        }
        int failures = m.failures;
        d.walkTo(block.position().add(1.2, 0, 0), boss);
        int punches = 0;
        for (int i = 0; i < 10 && block.isAlive(); i++) {
            attack(p, block);
            punches++;
            d.ticks(boss, 1);
        }
        d.ticks(boss, 1);
        if (m.special != null || m.failures > failures) {
            throw fail("freeze: the glowing block wasn't broken by " + punches + " punches");
        }
        if (!"abyssal_whip".equals(Inv.idOf(p.getMainHandItem()))) {
            throw fail("freeze: the freed weapon didn't come back to the hand");
        }
        log.add("freeze: weapon to the pack, freed in " + punches + " punches and wielded again");
        if (moonSpecial(boss, m, d, false) != MoonsRules.Special.FROST_STORM) {
            throw fail("the new moon didn't bring Frost Storm but " + m.special);
        }
        d.walkTo(m.selfTestBrazier(0), boss);
        d.ticks(boss, 1);
        d.walkTo(m.selfTestBrazier(1), boss);
        int ticks = until(boss, d, 30, () -> m.special == null);
        if (ticks < 0 || m.failures > failures) {
            throw fail("storm: relighting both braziers didn't stop her healing");
        }
        log.add("storm: both braziers relit");
        d.walkTo(m.glyphPos(boss), boss);
        moonKill(boss, d, pendingBefore);
        return String.join("; ", log) + "; pacified, reward waits in the Lunar Chest";
    }

    /** The Eclipse Moon: Searing Rays (keep behind the turning shield) and Clones (face each to parry it). */
    public static String eclipseMoon(DataBossEntity boss, Driver d) {
        if (!(boss.script() instanceof MoonScripts.EclipseMoon m)) {
            throw fail("the Eclipse Moon has no EclipseMoon script");
        }
        ServerPlayer p = d.player();
        int pendingBefore = LunarChest.pending(p);
        List<String> log = new ArrayList<>();
        log.add(moonStandard(boss, m, d));
        if (moonSpecial(boss, m, d, false) != MoonsRules.Special.SEARING_RAYS) {
            throw fail("the new moon didn't bring Searing Rays but " + m.special);
        }
        m.curse(p).remove(200);
        int ticks = until(boss, d, 24, () -> {
            d.walkTo(m.shelter(boss), boss);
            return m.special == null;
        });
        if (ticks < 0) {
            throw fail("rays: the shield never finished its turn");
        }
        if (m.curse(p).stacks() >= MoonsRules.RAY_CURSE) {
            throw fail("rays: a ray found the player behind the shield (" + m.curse(p).stacks() + " curse stacks)");
        }
        log.add("rays: kept behind the shield for its full turn (" + ticks + " ticks)");
        if (moonSpecial(boss, m, d, true) != MoonsRules.Special.CLONES) {
            throw fail("the full moon didn't bring the Clones but " + m.special);
        }
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, Inv.stack("abyssal_whip", 1));
        float hp = boss.getHealth();
        int clones = until(boss, d, 80, () -> {
            Vec3 clone = m.selfTestClone();
            if (clone != null) {
                face(p, clone);
            }
            return m.special == null;
        });
        if (clones < 0) {
            throw fail("clones: still running after 80 ticks");
        }
        if (boss.getHealth() >= hp && boss.isAlive()) {
            throw fail("clones: no parried clone hurt her");
        }
        log.add(String.format(java.util.Locale.ROOT, "clones: faced every clone, parries took %.0f health", hp - boss.getHealth()));
        d.walkTo(m.glyphPos(boss), boss);
        moonKill(boss, d, pendingBefore);
        return String.join("; ", log) + "; pacified, reward waits in the Lunar Chest";
    }

    /** The Blood Moon: Blood Rain (stay off the pools; she can't be hit) and Blood Jaguars (glyph, strike its jaguar); then
     *  the Lunar Chest pays out for the Moons. */
    public static String bloodMoon(DataBossEntity boss, Driver d) {
        if (!(boss.script() instanceof MoonScripts.BloodMoon m)) {
            throw fail("the Blood Moon has no BloodMoon script");
        }
        ServerPlayer p = d.player();
        int pendingBefore = LunarChest.pending(p);
        List<String> log = new ArrayList<>();
        log.add(moonStandard(boss, m, d));
        if (moonSpecial(boss, m, d, true) != MoonsRules.Special.BLOOD_RAIN) {
            throw fail("the full moon didn't bring Blood Rain but " + m.special);
        }
        if (m.isVulnerable(boss)) {
            throw fail("rain: she can still be hit while she has vanished");
        }
        Vec3 c = m.homeVec();
        Vec3[] spots = {c.add(9, 0, 0), c.add(-9, 0, 0), c.add(0, 0, 9), c.add(0, 0, -9), c.add(6, 0, 6), c.add(-6, 0, -6)};
        int ticks = until(boss, d, 30, () -> {
            Vec3 best = spots[0];
            double bestGap = -1;
            for (Vec3 s : spots) {
                double gap = Double.MAX_VALUE;
                for (Vec3 pool : m.selfTestPools()) {
                    gap = Math.min(gap, pool.distanceToSqr(s.x, pool.y, s.z));
                }
                if (gap > bestGap) {
                    bestGap = gap;
                    best = s;
                }
            }
            d.walkTo(best, boss);
            return m.special == null;
        });
        if (ticks < 0 || !m.isVulnerable(boss)) {
            throw fail("rain: it never stopped, or she never came back");
        }
        log.add("rain: kept off the pools for " + ticks + " ticks");
        if (moonSpecial(boss, m, d, false) != MoonsRules.Special.BLOOD_JAGUARS) {
            throw fail("the new moon didn't bring the Blood Jaguars but " + m.special);
        }
        List<Mob> jaguars = new ArrayList<>(m.selfTestJaguars());
        if (jaguars.size() != MoonsRules.JAGUARS) {
            throw fail("jaguars: " + jaguars.size() + " jaguars instead of " + MoonsRules.JAGUARS);
        }
        Vec3 glyph = m.glyphPos(boss);
        Mob near = jaguars.get(0);
        for (Mob j : jaguars) {
            if (j.distanceToSqr(glyph) < near.distanceToSqr(glyph)) {
                near = j;
            }
        }
        Mob target = near;
        Vec3 side = glyph.subtract(c).normalize().cross(new Vec3(0, 1, 0)).scale(0.6);
        boolean[] flip = {false};
        ticks = until(boss, d, 40, () -> {
            // on the glyph, stepping between two of its tiles away from the pools forming under the player
            Vec3 here = glyph.add(flip[0] ? side : side.reverse());
            for (Vec3 pool : m.selfTestPools()) {
                if (pool.distanceToSqr(here.x, pool.y, here.z) < 1.0) {
                    flip[0] = !flip[0];
                    here = glyph.add(flip[0] ? side : side.reverse());
                    break;
                }
            }
            d.walkTo(here, boss);
            if (target.isAlive()) {
                attack(p, target);
            }
            return m.special == null;
        });
        if (ticks < 0) {
            throw fail("jaguars: the special never ended");
        }
        if (m.selfTestRestored() <= 0) {
            throw fail("jaguars: hitting the glyph's jaguar gave no hitpoints back");
        }
        log.add("jaguars: on the glyph, " + m.selfTestRestored() + " hitpoints back off its jaguar");
        moonKill(boss, d, pendingBefore);
        // the chest: after Perilous Moons, once all three Moons are down, it pays out for the Moons pacified since the last claim
        com.runecraft.player.PlayerData data = com.runecraft.player.Players.data(p);
        var quest = com.runecraft.osrs.content.ContentRegistry.quest("perilous_moons");
        if (quest != null) {
            data.setQuestStage(quest, quest.finalStage());
        }
        for (String moon : MoonsRules.MOONS) {
            if (data.killCount(moon) <= 0) {
                data.incrementKillCount(moon);
            }
        }
        LunarChest.record(p, MoonsRules.BLUE);
        int moons = LunarChest.pending(p);
        List<String> got = LunarChest.claim(p);
        if (got.isEmpty() || LunarChest.pending(p) != 0) {
            throw fail("the Lunar Chest gave nothing for " + moons + " Moons, or kept them");
        }
        return String.join("; ", log) + "; pacified, Lunar Chest for " + moons + " Moons gave " + got.size() + " drops";
    }

    // =====================================================================================================
    // Desert Treasure II, Varlamore and the Royal Titans
    // =====================================================================================================

    /** A hit of a given style through the boss's own rules. */
    static int hit(DataBossEntity boss, ServerPlayer p, int damage, AttackType type) {
        int dmg = boss.modifyIncoming(p, damage, type);
        return dmg > 0 ? CombatHandler.applyDamage(p, boss, dmg) : 0;
    }

    /** The protection prayer against an attack's style (null for no attack). */
    static com.runecraft.osrs.Prayer prayerAgainst(com.runecraft.osrs.content.AttackDef a) {
        if (a == null) {
            return null;
        }
        return switch (a.style()) {
            case MAGIC -> com.runecraft.osrs.Prayer.PROTECT_FROM_MAGIC;
            case RANGED -> com.runecraft.osrs.Prayer.PROTECT_FROM_MISSILES;
            default -> com.runecraft.osrs.Prayer.PROTECT_FROM_MELEE;
        };
    }

    /**
     * Switches the player's overhead to the protection against an attack's style (none for null) through the real prayer
     * toggle, as a player clicking it does: the walk calls it on the game tick after an orb is announced and launched, some
     * Minecraft ticks into its flight (round 41: orbs fly at least 3 ticks).
     */
    static void prayAgainst(ServerPlayer p, com.runecraft.osrs.content.AttackDef a) {
        com.runecraft.osrs.Prayer pr = prayerAgainst(a);
        if (pr == null) {
            return;
        }
        com.runecraft.player.PlayerData data = com.runecraft.player.Players.data(p);
        if (data.prayerPoints() <= 0) {
            data.setPrayerPoints(99); // the walk tests mechanics, not prayer upkeep
        }
        if (!data.isActive(pr)) {
            com.runecraft.combat.PrayerService.toggle(p, pr);
            if (!data.isActive(pr)) {
                throw fail("the prayer toggle refused " + pr + " (active " + data.activePrayers() + ")");
            }
        }
    }

    /**
     * Switches a prayer on as a player does, through the prayer key's own path ({@code PrayerService.set}: the level, points and
     * conflict rules, an overhead replacing the other overheads, the switch grace stamped). Round 42: the walks no longer set
     * active prayers directly.
     */
    static void pray(ServerPlayer p, com.runecraft.osrs.Prayer pr) {
        com.runecraft.player.PlayerData data = com.runecraft.player.Players.data(p);
        if (data.prayerPoints() <= 0) {
            data.setPrayerPoints(99); // the walk tests mechanics, not prayer upkeep
        }
        if (!data.isActive(pr)) {
            com.runecraft.combat.PrayerService.set(p, pr, true);
        }
    }

    /** {@link #pray} that throws when the prayer didn't come on (refused, or switched straight off by a lock). */
    static void prayOrFail(ServerPlayer p, com.runecraft.osrs.Prayer pr) {
        pray(p, pr);
        if (!com.runecraft.player.Players.data(p).isActive(pr)) {
            throw fail("the prayer key refused " + pr + " (" + com.runecraft.combat.PrayerService.lastRefusal + ")");
        }
    }

    /** Kills the boss with heavy hits of a style (between its mechanics); throws if it survives. */
    static void finish(DataBossEntity boss, Driver d, AttackType type, int damage, int ticks) {
        ServerPlayer p = d.player();
        for (int i = 0; i < ticks && boss.isAlive(); i++) {
            if (boss.script().isVulnerable(boss)) {
                hit(boss, p, damage, type);
            }
            d.ticks(boss, 1);
        }
        if (boss.isAlive()) {
            throw fail("still alive after " + ticks + " ticks of heavy hits (" + Math.round(boss.getHealth()) + " health)");
        }
    }

    /**
     * The Leviathan: melee can't reach it; its orbs are prayed against as they fly; after the roar its special is answered
     * (keep moving through the barrage, hide behind the debris from the smoke blast); a stunned Leviathan takes 10 at most
     * except on its wound, which breaks the stun into a special and resets the volleys; then the pathfinder's enrage.
     */
    public static String leviathan(DataBossEntity boss, Driver d) {
        if (!(boss.script() instanceof LeviathanScript l)) {
            throw fail("the Leviathan has no LeviathanScript");
        }
        ServerPlayer p = d.player();
        Vec3 c = l.homeVec();
        d.moveTo(c.add(0, 0, 8), boss);
        List<String> log = new ArrayList<>();
        if (boss.modifyIncoming(p, 30, AttackType.SLASH) != 0) {
            throw fail("a melee hit reached the lure");
        }
        d.clearDamage();
        int ticks = until(boss, d, 80, () -> {
            d.react(prayerAgainst(l.selfTestNextOrb())); // round 44: a reaction time after the orb flies
            return l.selfTestSpecial() != null;
        });
        if (ticks < 0) {
            throw fail("no roar and special after the first volley (stage " + l.selfTestStage() + ")");
        }
        int orbs = d.damage("boulder") + d.damage("abyssal_orb") + d.damage("shadow_strike");
        if (orbs > 0) {
            throw fail("orbs prayed against while they flew still hit for " + orbs);
        }
        log.add("volley prayed through (" + ticks + " ticks), roar, " + l.selfTestSpecial());
        log.add(answerLeviathanSpecial(boss, l, d));
        if (l.selfTestStage() < 1) {
            throw fail("the volleys didn't speed up after the first");
        }
        // a stun: hits capped off the wound; the wound breaks it
        if (until(boss, d, 30, () -> {
            d.react(prayerAgainst(l.selfTestNextOrb())); // round 44: a reaction time after the orb flies
            return l.selfTestFlying() > 0;
        }) < 0) {
            throw fail("no second volley");
        }
        l.selfTestStun(boss, p);
        if (!l.selfTestStunned()) {
            throw fail("a shadow spell during a volley didn't stun it");
        }
        int front = boss.modifyIncoming(p, 40, AttackType.RANGED);
        if (front > LeviathanRules.STUNNED_CAP) {
            throw fail("a hit off its wound took " + front + " (capped at " + LeviathanRules.STUNNED_CAP + ")");
        }
        Vec3 wound = l.selfTestWound(boss);
        d.walkTo(wound, boss);
        int back = boss.modifyIncoming(p, 40, AttackType.RANGED);
        if (back <= LeviathanRules.STUNNED_CAP || l.selfTestStunned() || l.selfTestSpecial() == null) {
            throw fail("the wound: " + back + " damage, stunned " + l.selfTestStunned() + ", special " + l.selfTestSpecial());
        }
        if (l.selfTestStage() != LeviathanRules.afterStun(1)) {
            throw fail("the wound didn't reset the volleys to the second speed (stage " + l.selfTestStage() + ")");
        }
        log.add("stunned: " + front + " off the wound, " + back + " on it, retaliation " + l.selfTestSpecial());
        log.add(answerLeviathanSpecial(boss, l, d));
        // the enrage: stay in the pathfinder's light
        setPercent(boss, 19);
        if (until(boss, d, 6, l::selfTestEnraged) < 0 || l.selfTestPathfinder() == null) {
            throw fail("no enrage with an abyssal pathfinder below 20%");
        }
        d.clearDamage();
        for (int i = 0; i < 20; i++) {
            d.walkTo(l.selfTestPathfinder(), boss);
            d.react(prayerAgainst(l.selfTestNextOrb())); // round 44: a reaction time after the orb flies
            d.ticks(boss, 1);
        }
        d.walkTo(l.selfTestPathfinder(), boss);
        if (!l.selfTestInPathfinder(boss, p)) {
            throw fail("standing on the pathfinder isn't inside its area");
        }
        int pierced = d.damage("boulder") + d.damage("abyssal_orb");
        if (pierced > 0) {
            throw fail("inside the pathfinder, prayed orbs still hit for " + pierced);
        }
        for (int i = 0; i < 40 && boss.isAlive(); i++) {
            d.walkTo(l.selfTestPathfinder(), boss);
            d.react(prayerAgainst(l.selfTestNextOrb())); // round 44: a reaction time after the orb flies
            hit(boss, p, 150, AttackType.RANGED);
            d.ticks(boss, 1);
        }
        if (boss.isAlive()) {
            throw fail("the enraged Leviathan survived 40 ranged hits from the pathfinder");
        }
        if (l.selfTestDebris() != 0) {
            throw fail("its debris stayed after it died");
        }
        return String.join("; ", log) + "; enraged, killed from the pathfinder's light";
    }

    private static String answerLeviathanSpecial(DataBossEntity boss, LeviathanScript l, Driver d) {
        ServerPlayer p = d.player();
        String special = l.selfTestSpecial();
        Vec3 c = l.homeVec();
        double[] a = {Math.atan2(p.getZ() - c.z, p.getX() - c.x)};
        d.clearDamage();
        int ticks = until(boss, d, 30, () -> {
            boolean smokeBlastNext = "smoke".equals(l.selfTestSpecial()) && !l.selfTestSmokeDebris().isEmpty()
                    && l.selfTestSpecialTick() >= LeviathanRules.SMOKE_DEBRIS;
            if (smokeBlastNext) {
                // the ten debris are down: hide right behind the nearest pile, away from it (round 44: on foot, the blast lands
                // three game ticks after the last debris)
                net.minecraft.core.BlockPos pile = l.selfTestSmokeDebris().get(0);
                for (net.minecraft.core.BlockPos b : l.selfTestSmokeDebris()) {
                    if (b.distToCenterSqr(p.position()) < pile.distToCenterSqr(p.position())) {
                        pile = b;
                    }
                }
                Vec3 at = Vec3.atBottomCenterOf(pile);
                Vec3 away = at.subtract(boss.position()).multiply(1, 0, 1).normalize();
                Vec3 behind = at.add(away.scale(1.0));
                if (behind.distanceToSqr(p.position()) > 0.04) {
                    int since = sinceGameTick(boss);
                    int left = (LeviathanRules.SMOKE_DEBRIS + 3 - l.selfTestSpecialTick())
                            * com.runecraft.action.ActionManager.MC_TICKS_PER_GAME_TICK - since;
                    d.walkOut("smoke_blast", behind, since, left, boss);
                }
            } else {
                a[0] += 0.25; // keep moving round it (the current and the debris land where the player was)
                d.walkTo(BossFx.ground(BossFx.world(boss), c.x + Math.cos(a[0]) * 8, c.z + Math.sin(a[0]) * 8, c.y), boss);
            }
            return l.selfTestSpecial() == null;
        });
        if (ticks < 0) {
            throw fail(special + ": never ended");
        }
        if ("smoke".equals(special) && d.damage("smoke_blast") > 0) {
            throw fail("smoke: the blast found the player behind the debris (" + d.damage("smoke_blast") + ")");
        }
        return special + " answered in " + ticks + " ticks";
    }

    /** Amoxliatl: Unstable Ice broken with melee before it shatters; Icicle Crash blocked by praying again in time. */
    public static String amoxliatl(DataBossEntity boss, Driver d) {
        if (!(boss.script() instanceof ModernScripts.Amoxliatl a)) {
            throw fail("Amoxliatl has no Amoxliatl script");
        }
        ServerPlayer p = d.player();
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, Inv.stack("abyssal_whip", 1));
        d.moveTo(a.homeVec().add(0, 0, 6), boss);
        a.walkSpecialNext(true);
        if (until(boss, d, 14, () -> !a.walkIce().isEmpty()) < 0) {
            throw fail("no Unstable Ice");
        }
        List<Mob> ice = new ArrayList<>(a.walkIce());
        for (Mob m : ice) {
            d.walkTo(m.position().add(1.2, 0, 0), boss);
            attack(p, m);
            if (m.isAlive()) {
                throw fail("a melee hit didn't break the unstable ice");
            }
        }
        d.ticks(boss, 2);
        if (a.walkShattered() > 0) {
            throw fail("the ice shattered although every block was broken");
        }
        // Icicle Crash: she switches the prayers off; Protect from Magic again before it lands blocks it (round 44: the player
        // switches it back on through the prayer key a reaction time after seeing it go, at Minecraft-tick precision)
        d.keep(com.runecraft.osrs.Prayer.PROTECT_FROM_MAGIC);
        int restoresBefore = d.prayerRestores();
        a.walkSpecialNext(false);
        d.clearDamage();
        d.ticks(boss, 16);
        d.keep(null);
        int disabled = d.prayerRestores() - restoresBefore;
        if (disabled == 0) {
            throw fail("Icicle Crash never switched the prayers off");
        }
        if (d.damage("icicle_crash") > 0) {
            throw fail("Icicle Crash hit for " + d.damage("icicle_crash") + " through Protect from Magic turned on again");
        }
        finish(boss, d, AttackType.CRUSH, 150, 60);
        return ice.size() + " unstable ice broken, Icicle Crash blocked by praying again, killed";
    }

    /** The Hueycoatl: body parts first, rain fire through prayer for 0-2, the tail's capped hits, the final phase. */
    public static String hueycoatl(DataBossEntity boss, Driver d) {
        if (!(boss.script() instanceof EncounterScripts.Hueycoatl h)) {
            throw fail("the Hueycoatl has no Hueycoatl script");
        }
        ServerPlayer p = d.player();
        d.moveTo(h.homeVec().add(0, 0, 7), boss);
        d.ticks(boss, 1);
        if (h.isVulnerable(boss) || h.walkSegments().isEmpty()) {
            throw fail("the head is open with no body parts in the way");
        }
        int parts = h.walkSegments().size();
        for (Mob seg : new ArrayList<>(h.walkSegments())) {
            CombatHandler.applyDamage(p, seg, 400);
        }
        if (until(boss, d, 4, () -> h.isVulnerable(boss)) < 0) {
            throw fail("the head stayed shut with every body part down");
        }
        Vec3 c = h.homeVec();
        d.keep(com.runecraft.osrs.Prayer.PROTECT_FROM_MELEE); // round 44: kept up through the prayer key, as a player does
        d.clearDamage();
        double[] a = {0};
        for (int i = 0; i < 50; i++) {
            a[0] += 0.3; // keep off her glowing symbols
            d.walkTo(BossFx.ground(BossFx.world(boss), c.x + Math.cos(a[0]) * 7, c.z + Math.sin(a[0]) * 7, c.y), boss);
            d.ticks(boss, 1);
        }
        d.keep(null);
        if (d.biggest("tail_strike") > 2) {
            throw fail("a red feather hit for " + d.biggest("tail_strike") + " through Protect from Melee (0-2)");
        }
        setPercent(boss, 49);
        if (until(boss, d, 4, () -> h.walkTail() != null) < 0 || h.isVulnerable(boss)) {
            throw fail("no shielded tail phase at 50%");
        }
        Mob tail = h.walkTail();
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, Inv.stack("abyssal_whip", 1));
        float before = tail.getHealth();
        attack(p, tail);
        float took = before - tail.getHealth();
        if (took > BossMechanics.HUEY_TAIL_CAP_CRUSH) {
            throw fail("a hit on the tail took " + took + " (capped at 4, 9 with crush)");
        }
        CombatHandler.applyDamage(p, tail, 400);
        if (until(boss, d, 4, h::walkFinal) < 0 || !h.isVulnerable(boss)) {
            throw fail("the broken tail didn't open the final phase");
        }
        finish(boss, d, AttackType.SLASH, 400, 60);
        return parts + " body parts, rain fire through prayer " + d.biggest("tail_strike") + " at most, tail hit capped (" + took
                + "), final phase, killed";
    }

    /** The Royal Titans: walls doused, the ranged boost while they keep away, the Elemental Blast's safe spot, both together. */
    public static String royalTitans(DataBossEntity boss, Driver d) {
        if (!(boss.script() instanceof EncounterScripts.RoyalTitans t)) {
            throw fail("Branda has no RoyalTitans script");
        }
        ServerPlayer p = d.player();
        d.moveTo(t.homeVec().add(0, 0, 7), boss);
        d.ticks(boss, 1);
        DataBossEntity eldric = t.walkEldric();
        if (eldric == null || !eldric.isAlive()) {
            throw fail("Eldric didn't rise beside Branda");
        }
        if (!t.rangedBoost()) {
            throw fail("no ranged accuracy boost while they keep their distance");
        }
        Vec3[] cell = {null};
        if (until(boss, d, 30, () -> (cell[0] = t.walkWallCell(true)) != null) < 0) {
            throw fail("no fire wall formed in 30 ticks");
        }
        t.walkClearWall(boss, cell[0], true);
        Vec3 next = t.walkWallCell(true);
        if (next != null && next.distanceToSqr(cell[0]) < 0.01) {
            throw fail("a Water Blast didn't douse the fire");
        }
        // phase 2 below 210: the Elemental Blast's safe spot
        boss.setHealth(boss.getMaxHealth() * 0.3f);
        if (until(boss, d, 4, t::walkPhaseTwo) < 0) {
            throw fail("no phase 2 below 210 hitpoints");
        }
        if (until(boss, d, 40, t::walkBlasting) < 0) {
            throw fail("no Elemental Blast in phase 2");
        }
        d.clearDamage();
        if (until(boss, d, 30, () -> {
            if (t.walkSafe() != null) {
                d.walkTo(t.walkSafe(), boss);
            }
            return !t.walkBlasting();
        }) < 0) {
            throw fail("the Elemental Blast never ended");
        }
        if (d.damage("elemental_blast") > 0) {
            throw fail("the blast hit the player on the safe spot for " + d.damage("elemental_blast"));
        }
        // Branda brought down while Eldric stands is subdued; both fall together
        hit(boss, p, 5000, AttackType.SLASH);
        if (!t.walkSubdued() || !boss.isAlive()) {
            throw fail("Branda wasn't subdued while Eldric still stood");
        }
        CombatHandler.applyDamage(p, eldric, 5000);
        if (until(boss, d, 6, () -> !boss.isAlive()) < 0) {
            throw fail("Branda didn't fall with Eldric");
        }
        return "fire wall doused, ranged boost, blast dodged on the safe spot, subdued and fell together";
    }

    /**
     * Duke Sucellus: asleep until poisoned (mushrooms ground into powder, salt mined, both fermented into arder-musca poison
     * and used on him), then his Freezing Gaze dodged behind a pillar, and the kill.
     */
    public static String duke(DataBossEntity boss, Driver d) {
        if (!(boss.script() instanceof EncounterScripts.Duke duke)) {
            throw fail("Duke Sucellus has no Duke script");
        }
        ServerPlayer p = d.player();
        d.moveTo(duke.homeVec().add(0, 0, 6), boss);
        d.clearDamage();
        d.ticks(boss, 10); // his pillars rise; he sleeps
        if (!duke.asleep() || duke.isVulnerable(boss) || d.damage("fist_slam") + d.damage("magic_blast") > 0) {
            throw fail("the Duke didn't sleep until poisoned");
        }
        int poisons = com.runecraft.osrs.content.bosses.DukeRules.poisonsToWake(boss.awakened());
        int per = com.runecraft.osrs.content.bosses.DukeRules.PER_POISON * poisons;
        int powderEach = com.runecraft.osrs.content.bosses.DukeRules.powderPerMushroom(
                com.runecraft.player.Players.data(p).skills().level(com.runecraft.osrs.Skill.HERBLORE));
        int mushrooms = (per + powderEach - 1) / powderEach;
        Inv.give(p, "pestle_and_mortar", 1);
        Inv.give(p, "rune_pickaxe", 1);
        Inv.give(p, com.runecraft.osrs.content.bosses.DukeRules.ARDER_MUSHROOM, mushrooms);
        Inv.give(p, com.runecraft.osrs.content.bosses.DukeRules.MUSCA_MUSHROOM, mushrooms);
        DukePrep.grind(p, com.runecraft.osrs.content.bosses.DukeRules.ARDER_MUSHROOM);
        DukePrep.grind(p, com.runecraft.osrs.content.bosses.DukeRules.MUSCA_MUSHROOM);
        // the salt deposit: one harvest a pickaxe swing (rune: 3 ticks), 5 Mining XP each, and a second click changes nothing
        int miningXp = com.runecraft.player.Players.data(p).skills().xpTenths(com.runecraft.osrs.Skill.MINING);
        int salt0 = Inv.count(p, com.runecraft.osrs.content.bosses.DukeRules.SALAX_SALT);
        DukePrep.mineSalt(p, p.blockPosition());
        DukePrep.mineSalt(p, p.blockPosition());
        if (Inv.count(p, com.runecraft.osrs.content.bosses.DukeRules.SALAX_SALT) != salt0) {
            throw fail("clicking the salt deposit gave salt before the pickaxe swung");
        }
        int saltTicks = 0;
        while (saltTicks < 80 && Inv.count(p, com.runecraft.osrs.content.bosses.DukeRules.SALAX_SALT) < per
                && com.runecraft.action.ActionManager.tickNow(p)) {
            saltTicks++;
        }
        com.runecraft.action.ActionManager.cancel(p);
        int salt = Inv.count(p, com.runecraft.osrs.content.bosses.DukeRules.SALAX_SALT);
        int harvests = (salt - salt0) / com.runecraft.osrs.content.bosses.DukeRules.saltPerHarvest(
                com.runecraft.player.Players.data(p).skills().level(com.runecraft.osrs.Skill.MINING));
        int gained = com.runecraft.player.Players.data(p).skills().xpTenths(com.runecraft.osrs.Skill.MINING) - miningXp;
        if (salt < per || saltTicks < 2 * harvests || gained > harvests * 50) {
            throw fail("the salt deposit: " + salt + " salt (" + per + " needed) in " + saltTicks + " ticks for " + gained / 10.0
                    + " Mining XP");
        }
        int herbloreXp = com.runecraft.player.Players.data(p).skills().xpTenths(com.runecraft.osrs.Skill.HERBLORE);
        int made = DukePrep.ferment(p);
        if (made < poisons) {
            throw fail("the vat made " + made + " poisons, " + poisons + " needed");
        }
        if (com.runecraft.player.Players.data(p).skills().xpTenths(com.runecraft.osrs.Skill.HERBLORE) - herbloreXp > made * 100) {
            throw fail("the vat gave more than 10 Herblore XP a poison");
        }
        for (int i = 0; i < poisons; i++) {
            Inv.remove(p, com.runecraft.osrs.content.bosses.DukeRules.POISON, 1);
            duke.feed(boss, p, com.runecraft.osrs.content.bosses.DukeRules.POISON_VALUE);
        }
        if (duke.asleep()) {
            throw fail(poisons + " arder-musca poisons didn't wake him");
        }
        d.ticks(boss, 2);
        duke.walkGazeNext();
        if (until(boss, d, 12, duke::walkGazing) < 0) {
            throw fail("no Freezing Gaze");
        }
        Vec3 best = null;
        for (Vec3 spot : duke.walkHidingSpots(boss)) {
            if (best == null || spot.distanceToSqr(p.position()) < best.distanceToSqr(p.position())) {
                best = spot;
            }
        }
        // round 44: on foot, a reaction time after his gaze begins; it lands DUKE_GAZE_TICKS game ticks after it began
        int since = sinceGameTick(boss);
        d.walkOut("freezing_gaze", best, since,
                BossMechanics.DUKE_GAZE_TICKS * com.runecraft.action.ActionManager.MC_TICKS_PER_GAME_TICK - since, boss);
        d.clearDamage();
        if (until(boss, d, 10, () -> !duke.walkGazing()) < 0) {
            throw fail("the gaze never landed");
        }
        if (d.damage("freezing_gaze") > 0) {
            throw fail("the gaze found the player behind a pillar (" + d.damage("freezing_gaze") + ")");
        }
        finish(boss, d, AttackType.SLASH, 120, 80);
        return "asleep until " + poisons + " poisons (mushrooms ground, " + salt + " salt mined in " + saltTicks
                + " ticks, fermented), gaze dodged behind a pillar, killed";
    }

    /** Vardorvis: Strangle answered by destroying every spore in time; his head's gaze blocked by the matching protection. */
    public static String vardorvis(DataBossEntity boss, Driver d) {
        if (!(boss.script() instanceof EncounterScripts.Vardorvis v)) {
            throw fail("Vardorvis has no Vardorvis script");
        }
        ServerPlayer p = d.player();
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, Inv.stack("abyssal_whip", 1));
        d.moveTo(v.homeVec().add(0, 0, 5), boss);
        d.ticks(boss, 2);
        v.walkStrangle(boss, p);
        List<Mob> spores = new ArrayList<>(v.walkSpores());
        if (spores.isEmpty()) {
            throw fail("Strangle brought no spores");
        }
        for (Mob spore : spores) {
            attack(p, spore);
        }
        d.clearDamage();
        if (until(boss, d, 12, () -> !v.walkStrangling()) < 0) {
            throw fail("the Strangle never resolved");
        }
        if (d.damage("strangle") > 0) {
            throw fail("the tendrils tightened although every spore was destroyed");
        }
        pray(p, com.runecraft.osrs.Prayer.PROTECT_FROM_MISSILES);
        int points = com.runecraft.player.Players.data(p).prayerPoints();
        v.walkGaze(boss, p);
        if (until(boss, d, 10, () -> {
            // round 44: through the prayer key a reaction time after the head shows its style
            d.react(v.walkGazeMagic() ? com.runecraft.osrs.Prayer.PROTECT_FROM_MAGIC : com.runecraft.osrs.Prayer.PROTECT_FROM_MISSILES);
            return !v.walkGazing();
        }) < 0) {
            throw fail("the head's gaze never landed");
        }
        if (com.runecraft.player.Players.data(p).prayerPoints() < points) {
            throw fail("a gaze blocked by the matching protection prayer still drained prayer");
        }
        finish(boss, d, AttackType.SLASH, 150, 80);
        return spores.size() + " spores destroyed before the Strangle, the head's gaze blocked, killed";
    }
}
