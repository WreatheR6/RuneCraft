package com.runecraft.content.bosses;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.combat.CombatHandler;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.bosses.BossMechanics;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * A whole Zalcano kill as the real player ({@code boss:walk:zalcano}, round 38; OSRS Wiki "Zalcano", "Zalcano/Strategies"):
 * her armour ignores ordinary attacks; tephra is mined at the glowing rock formation (following it when she moves the
 * glow), refined at the furnace and imbued at the altar, then thrown at her armour through the real attack path; a golem
 * killed with imbued tephra leaves the player 16-24 of it, a golem that reaches her restores her armour; once her armour
 * breaks she falls and is mined with a pickaxe; she rises armoured, destroying every tephra; until she dies, with the player
 * as her most damaging attacker ("The Spurned Hero"). Any broken step throws.
 */
public final class ZalcanoWalk {
    private ZalcanoWalk() {
    }

    private static IllegalStateException fail(String message) {
        return new IllegalStateException(message);
    }

    /** One attack on a creature through the real player attack path (as the fight checks do). */
    private static void attack(ServerPlayer p, LivingEntity target) {
        Players.data(p).lastAttackTick = ActionManager.mcTick() - 10_000;
        target.hurt(CombatHandler.source(p), 1f);
    }

    private static double xp(ServerPlayer p, Skill s) {
        return Players.data(p).skills().xpTenths(s) / 10.0;
    }

    private static int until(DataBossEntity boss, BossWalks.Driver d, int max, BooleanSupplier done) {
        return BossWalks.until(boss, d, max, done);
    }

    public static String walk(DataBossEntity boss, BossWalks.Driver d) {
        if (!(boss.script() instanceof ClassicScripts.Zalcano z)) {
            throw fail("Zalcano has no Zalcano script");
        }
        ServerPlayer p = d.player();
        List<String> notes = new ArrayList<>();
        p.setItemInHand(InteractionHand.MAIN_HAND, Inv.stack("rune_pickaxe", 1));
        for (String id : BossMechanics.TEPHRA_ITEMS) {
            Inv.remove(p, id, Inv.count(p, id));
        }

        // Her stone armour ignores an ordinary attack.
        int armour = z.selfTestArmour();
        attack(p, boss);
        if (z.selfTestArmour() != armour || z.selfTestDown()) {
            throw fail("her armour took an attack without imbued tephra (" + armour + " -> " + z.selfTestArmour() + ")");
        }

        // Mine tephra at the glowing rock formation (it moves when she explodes it).
        double mining = xp(p, Skill.MINING);
        int got = until(boss, d, 300, () -> {
            d.moveTo(z.selfTestGlowingRock(boss), boss);
            return Inv.count(p, BossMechanics.TEPHRA) >= 5;
        });
        if (got < 0) {
            throw fail("300 ticks at the glowing rock formation mined " + Inv.count(p, BossMechanics.TEPHRA) + " tephra");
        }
        if (xp(p, Skill.MINING) <= mining) {
            throw fail("mining tephra gave no Mining experience");
        }
        notes.add("mined 5 tephra in " + got + " ticks");
        // Refine at the furnace, imbue at the altar.
        double smithing = xp(p, Skill.SMITHING);
        d.moveTo(z.selfTestFurnace(boss), boss);
        if (until(boss, d, 40, () -> Inv.count(p, BossMechanics.TEPHRA) == 0) < 0
                || Inv.count(p, BossMechanics.REFINED_TEPHRA) < 5 || xp(p, Skill.SMITHING) <= smithing) {
            throw fail("the furnace didn't refine the tephra (" + Inv.count(p, BossMechanics.REFINED_TEPHRA) + " refined)");
        }
        double rc = xp(p, Skill.RUNECRAFT);
        d.moveTo(z.selfTestAltar(boss), boss);
        if (until(boss, d, 40, () -> Inv.count(p, BossMechanics.REFINED_TEPHRA) == 0) < 0
                || Inv.count(p, BossMechanics.IMBUED_TEPHRA) < 5 || xp(p, Skill.RUNECRAFT) <= rc) {
            throw fail("the altar didn't imbue the tephra (" + Inv.count(p, BossMechanics.IMBUED_TEPHRA) + " imbued)");
        }
        notes.add("refined and imbued them");
        // Throw the imbued tephra at her armour.
        armour = z.selfTestArmour();
        int thrown = 0;
        while (Inv.has(p, BossMechanics.IMBUED_TEPHRA, 1) && thrown < 10) {
            attack(p, boss);
            thrown++;
        }
        if (Inv.has(p, BossMechanics.IMBUED_TEPHRA, 1) || z.selfTestArmour() >= armour) {
            throw fail("throwing 5 imbued tephra: " + Inv.count(p, BossMechanics.IMBUED_TEPHRA) + " left, armour " + armour + " -> "
                    + z.selfTestArmour());
        }
        notes.add("5 tephra took her armour " + armour + " -> " + z.selfTestArmour());

        // A golem killed with imbued tephra leaves its attacker 16-24 of it.
        Mob golem = z.selfTestSpawnGolem(boss);
        if (golem == null) {
            throw fail("no golem formed");
        }
        Inv.give(p, BossMechanics.IMBUED_TEPHRA, 30);
        for (int i = 0; i < 30 && golem.isAlive() && Inv.has(p, BossMechanics.IMBUED_TEPHRA, 1); i++) {
            attack(p, golem);
        }
        if (golem.isAlive()) {
            throw fail("30 imbued tephra didn't kill a golem (" + golem.getHealth() + " hp left)");
        }
        for (String id : BossMechanics.TEPHRA_ITEMS) {
            Inv.remove(p, id, Inv.count(p, id));
        }
        d.ticks(boss, 1);
        int left = Inv.count(p, BossMechanics.IMBUED_TEPHRA);
        if (left < BossMechanics.ZALCANO_GOLEM_TEPHRA_MIN || left > BossMechanics.ZALCANO_GOLEM_TEPHRA_MAX) {
            throw fail("the killed golem left " + left + " imbued tephra");
        }
        notes.add("a golem killed with tephra left " + left);
        // A golem that reaches her restores her armour (first break it to at most half, short of making her fall).
        for (int i = 0; i < 40 && z.selfTestArmour() > BossMechanics.ZALCANO_ARMOUR / 2 && !z.selfTestDown(); i++) {
            if (!Inv.has(p, BossMechanics.IMBUED_TEPHRA, 1)) {
                Inv.give(p, BossMechanics.IMBUED_TEPHRA, 10);
            }
            attack(p, boss);
        }
        for (String id : BossMechanics.TEPHRA_ITEMS) {
            Inv.remove(p, id, Inv.count(p, id));
        }
        armour = z.selfTestArmour();
        if (armour >= BossMechanics.ZALCANO_ARMOUR || z.selfTestDown()) {
            throw fail("her armour before the golem test: " + armour + (z.selfTestDown() ? " (fallen)" : ""));
        }
        Mob mender = z.selfTestSpawnGolem(boss);
        if (mender == null) {
            throw fail("no second golem formed");
        }
        mender.teleportTo(boss.getX() + 1, boss.getY(), boss.getZ());
        if (until(boss, d, 20, () -> !mender.isAlive() || mender.isRemoved()) < 0 || z.selfTestArmour() <= armour) {
            throw fail("a golem beside her didn't restore her armour (" + armour + " -> " + z.selfTestArmour() + ")");
        }
        notes.add("a golem restored her armour " + armour + " -> " + z.selfTestArmour());

        // Break her armour and mine her until she dies, phase after phase.
        int falls = 0;
        int mined = 0;
        for (int phase = 0; phase < 12 && boss.isAlive(); phase++) {
            Inv.give(p, BossMechanics.IMBUED_TEPHRA, 64);
            for (int i = 0; i < 200 && !z.selfTestDown() && boss.isAlive(); i++) {
                if (!Inv.has(p, BossMechanics.IMBUED_TEPHRA, 1)) {
                    Inv.give(p, BossMechanics.IMBUED_TEPHRA, 64);
                }
                attack(p, boss);
            }
            if (!z.selfTestDown()) {
                throw fail("200 imbued tephra didn't break her armour (" + z.selfTestArmour() + " left)");
            }
            falls++;
            for (String id : BossMechanics.TEPHRA_ITEMS) {
                Inv.remove(p, id, Inv.count(p, id));
            }
            float before = boss.getHealth();
            double miningXp = xp(p, Skill.MINING);
            d.moveTo(boss.position().add(3, 0, 0), boss);
            for (int t = 0; t < 40 && z.selfTestDown() && boss.isAlive(); t++) {
                attack(p, boss);
                mined++;
                d.ticks(boss, 1);
            }
            if (boss.isAlive() && boss.getHealth() >= before) {
                throw fail("mining the fallen Zalcano didn't hurt her (" + before + " -> " + boss.getHealth() + ")");
            }
            if (xp(p, Skill.MINING) <= miningXp) {
                throw fail("mining the fallen Zalcano gave no Mining experience");
            }
            if (boss.isAlive()) {
                // She rises armoured again and destroys every tephra in the prison.
                Inv.give(p, BossMechanics.TEPHRA, 3);
                if (until(boss, d, 40, () -> !z.selfTestDown()) < 0) {
                    throw fail("she never rose again");
                }
                if (Inv.has(p, BossMechanics.TEPHRA, 1) || z.selfTestArmour() != BossMechanics.ZALCANO_ARMOUR) {
                    throw fail("rising: tephra left " + Inv.count(p, BossMechanics.TEPHRA) + ", armour " + z.selfTestArmour());
                }
            }
        }
        if (boss.isAlive()) {
            throw fail("Zalcano survived " + falls + " falls (" + boss.getHealth() + " hp left)");
        }
        if (!p.getUUID().equals(z.selfTestMvp())) {
            throw fail("the only attacker isn't her most damaging player (The Spurned Hero)");
        }
        for (String id : BossMechanics.TEPHRA_ITEMS) {
            Inv.remove(p, id, Inv.count(p, id));
        }
        notes.add("killed after " + falls + " falls and " + mined + " pickaxe hits, as her MVP");
        return String.join("; ", notes);
    }
}
