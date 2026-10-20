package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.combat.CombatHandler;
import com.runecraft.osrs.AttackType;
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
    }

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
     * The Whisperer: her three specials at 80/55/30% answered through the Shadow Realm (the fragment glows only then; the
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
        double[] at = {79, 54, 29};
        for (double pct : at) {
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
            if (w.selfTestSpecialName() != null) {
                throw fail(special + " still running after it was answered");
            }
            if (w.selfTestFailures() > failures) {
                throw fail(special + " ran out on the player although it was answered");
            }
            if (w.inRealm(p)) {
                throw fail("still in the Shadow Realm after the " + special);
            }
            d.moveTo(c.add(0, 0, -8), boss);
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

    private static String walkSeeds(DataBossEntity boss, WhispererScript w, Driver d) {
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
        for (Vec3 s : light) {
            d.moveTo(s, boss);
            d.ticks(boss, 1);
        }
        d.ticks(boss, 1);
        return "seeds: realm peek " + before + "->" + peek + "% sanity, " + n + " light seeds stepped on, sanity " + w.sanity(p) + "%";
    }

    private static String walkSiphon(DataBossEntity boss, WhispererScript w, Driver d) {
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
                d.moveTo(soul.position().add(1.2, 0, 0), boss);
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

    private static String walkScreech(DataBossEntity boss, WhispererScript w, Driver d) {
        ServerPlayer p = d.player();
        if (!WhispererScript.activateFragment(p)) {
            throw fail("screech: the glowing fragment didn't take the player into the Shadow Realm");
        }
        d.ticks(boss, 1); // a look at the columns' health
        WhispererScript.activateFragment(p);
        int ticks = until(boss, d, 40, () -> {
            Vec3 spot = w.selfTestHidingSpot(boss);
            if (spot != null) {
                d.moveTo(spot, boss);
            }
            return w.selfTestSpecialName() == null;
        });
        if (ticks < 0) {
            throw fail("screech: still screeching after 40 ticks");
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
        until(boss, d, 14, () -> {
            d.moveTo(m.glyphPos(boss), boss);
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
        d.moveTo(m.glyphPos(boss), boss);
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
        d.moveTo(block.position().add(1.2, 0, 0), boss);
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
        d.moveTo(m.selfTestBrazier(0), boss);
        d.ticks(boss, 1);
        d.moveTo(m.selfTestBrazier(1), boss);
        int ticks = until(boss, d, 30, () -> m.special == null);
        if (ticks < 0 || m.failures > failures) {
            throw fail("storm: relighting both braziers didn't stop her healing");
        }
        log.add("storm: both braziers relit");
        d.moveTo(m.glyphPos(boss), boss);
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
            d.moveTo(m.shelter(boss), boss);
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
        d.moveTo(m.glyphPos(boss), boss);
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
            d.moveTo(best, boss);
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
            d.moveTo(here, boss);
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
}
