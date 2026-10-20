package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.combat.CombatHandler;
import com.runecraft.osrs.AttackType;
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
}
