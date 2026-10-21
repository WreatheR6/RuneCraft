package com.runecraft.osrs;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The real-time player attack clock (round 41): buffered clicks keep the OSRS attack rate exactly, never faster and never
 * slower, whatever the click timing.
 */
class AttackClockTest {
    @Test
    void clickingEveryTickResolvesAtTheOsrsRate() {
        // Abyssal whip, speed 4 = 48 MC: clicking on every MC tick from 0 to 4800 resolves at 0, 48, ..., 4800.
        AttackClock.Sim sim = new AttackClock.Sim(48);
        for (long t = 0; t <= 4800; t++) {
            sim.click(t);
            sim.tick(t);
        }
        assertEquals(101, sim.resolved.size());
        for (int i = 0; i < sim.resolved.size(); i++) {
            assertEquals(48L * i, sim.resolved.get(i));
        }
    }

    @Test
    void everyOsrsSpeedKeepsItsAttacksPerSecond() {
        // Attacks per window = floor(window / period) + 1 for every weapon speed: DPS = maxHit x accuracy / (speed x 0.6 s).
        for (int speed = 1; speed <= 8; speed++) {
            int period = AttackClock.periodMc(speed);
            AttackClock.Sim sim = new AttackClock.Sim(period);
            int window = 6000;
            for (long t = 0; t < window; t++) {
                sim.click(t);
                sim.tick(t);
            }
            assertEquals((window - 1) / period + 1, sim.resolved.size(), "speed " + speed);
            for (int i = 1; i < sim.resolved.size(); i++) {
                assertEquals(period, sim.resolved.get(i) - sim.resolved.get(i - 1), "speed " + speed + " gap " + i);
            }
        }
    }

    @Test
    void jitteredClicksAreNeverLost() {
        // A player clicking roughly on the beat (+-3 MC) gets exactly one attack per click: early clicks wait for the ready
        // tick, late ones resolve at once.
        Random rng = new Random(41);
        AttackClock.Sim sim = new AttackClock.Sim(48);
        List<Long> clicks = new ArrayList<>();
        for (int k = 0; k < 100; k++) {
            clicks.add(48L * k + rng.nextInt(7) - 3 + 3); // +3 keeps the first click at or after tick 0
        }
        int next = 0;
        for (long t = 0; t <= 48L * 101; t++) {
            while (next < clicks.size() && clicks.get(next) == t) {
                sim.click(t);
                next++;
            }
            sim.tick(t);
        }
        assertEquals(clicks.size(), sim.resolved.size());
        for (int i = 1; i < sim.resolved.size(); i++) {
            assertTrue(sim.resolved.get(i) - sim.resolved.get(i - 1) >= 48, "two attacks closer than the weapon speed");
        }
    }

    @Test
    void anEarlyClickResolvesOnTheReadyTick() {
        AttackClock.Sim sim = new AttackClock.Sim(48);
        assertTrue(sim.click(0));
        assertFalse(sim.click(40)); // 8 MC early: buffered
        for (long t = 40; t < 48; t++) {
            assertEquals(AttackClock.Step.WAIT, sim.tick(t));
        }
        assertEquals(AttackClock.Step.FIRE, sim.tick(48));
        assertEquals(List.of(0L, 48L), sim.resolved);
        assertEquals(AttackClock.Step.IDLE, sim.tick(49));
    }

    @Test
    void switchingWeaponsMidCycleKeepsTheFirstReadyTick() {
        // OSRS: the delay after an attack is that attack's speed, whatever is wielded afterwards.
        AttackClock.Sim sim = new AttackClock.Sim(48);
        sim.click(0);
        sim.weaponPeriodMc = 72; // a speed-6 weapon wielded at tick 10
        assertFalse(sim.click(20));
        for (long t = 20; t <= 48; t++) {
            sim.tick(t);
        }
        assertEquals(List.of(0L, 48L), sim.resolved);
        // The next attack now waits the new weapon's 72.
        assertFalse(sim.click(100));
        for (long t = 100; t <= 120; t++) {
            sim.tick(t);
        }
        assertEquals(List.of(0L, 48L, 120L), sim.resolved);
    }

    @Test
    void aNewerClickReplacesTheBufferedOne() {
        AttackClock.Buffer b = new AttackClock.Buffer();
        assertTrue(b.click(0, -1000, 48, AttackClock.Kind.MELEE, 7));
        b.accept(48);
        assertFalse(b.click(10, 0, 48, AttackClock.Kind.MELEE, 7));
        assertFalse(b.click(20, 0, 48, AttackClock.Kind.SPELL, 9));
        assertEquals(AttackClock.Kind.SPELL, b.pendingKind());
        assertEquals(9, b.pendingTarget());
        assertEquals(20, b.queuedAt());
    }

    @Test
    void anInvalidTargetIsDroppedAfterTheGrace() {
        AttackClock.Sim sim = new AttackClock.Sim(48);
        sim.click(0);
        sim.click(30);
        for (long t = 30; t <= 48 + AttackClock.PENDING_GRACE_MC; t++) {
            assertEquals(AttackClock.Step.WAIT, sim.tick(t, false), "tick " + t);
        }
        assertEquals(AttackClock.Step.DROP, sim.tick(48 + AttackClock.PENDING_GRACE_MC + 1, false));
        assertEquals(List.of(0L), sim.resolved);
        // Valid again within the grace: it fires late, on the first tick it can.
        AttackClock.Sim late = new AttackClock.Sim(48);
        late.click(0);
        late.click(30);
        for (long t = 30; t < 53; t++) {
            late.tick(t, false);
        }
        assertEquals(AttackClock.Step.FIRE, late.tick(53, true));
        assertEquals(List.of(0L, 53L), late.resolved);
    }

    @Test
    void noRecordedPeriodFallsBackToTheWeaponSpeed() {
        AttackClock.Buffer b = new AttackClock.Buffer();
        assertEquals(60, b.periodMc(60));
        assertEquals(100 + 60, b.readyAt(100, 60));
        b.accept(36);
        assertEquals(36, b.periodMc(60));
    }

    @Test
    void scriptsThatMoveLastAttackKeepTheirMeaning() {
        // The Nightmare's slowing spores push lastAttack = now + slow x 12 on the attack tick: the next attack waits slow
        // ticks longer. Amoxliatl's unstable ice sets lastAttack = now - (speed - 1) x 12: ready a tick later.
        AttackClock.Sim sim = new AttackClock.Sim(48);
        sim.click(0);
        sim.lastAttack = 0 + 2 * 12;
        for (long t = 1; t <= 200; t++) {
            sim.click(t);
            sim.tick(t);
        }
        assertEquals(72L, sim.resolved.get(1));
        AttackClock.Sim ice = new AttackClock.Sim(48);
        ice.click(0);
        ice.lastAttack = 0 - 3 * 12;
        for (long t = 1; t <= 20; t++) {
            ice.click(t);
        }
        assertEquals(12L, ice.resolved.get(1));
        // A self-test "make ready" (lastAttack = now - 10000) is ready at once whatever the recorded period.
        AttackClock.Buffer b = new AttackClock.Buffer();
        b.accept(72);
        assertTrue(b.click(500, 500 - 10_000, 48, AttackClock.Kind.MELEE, 1));
    }

    @Test
    void rangedSpeed() {
        assertEquals(3, AttackClock.rangedSpeed(4, true));
        assertEquals(4, AttackClock.rangedSpeed(4, false));
        assertEquals(1, AttackClock.rangedSpeed(1, true));
        assertEquals(5, AttackClock.rangedSpeed(6, true)); // crossbows on Rapid
    }

    @Test
    void vanillaIndicatorMatchesTheOsrsPeriod() {
        for (int ticks = 1; ticks <= 10; ticks++) {
            double speed = AttackClock.VANILLA_BASE_ATTACK_SPEED + AttackClock.vanillaAttackSpeedModifier(ticks);
            assertEquals(ticks * 12.0, AttackClock.vanillaPeriodMc(speed), 1e-3, "ticks " + ticks);
        }
        assertEquals(-4.0f + 1.0f / 2.4f, AttackClock.vanillaAttackSpeedModifier(4), 1e-6);
    }

    @Test
    void theWeaponsOfTheSelfTestsHaveTheirOsrsSpeeds() {
        // osrs-dps-calc equipment.json: dragon axe 5, abyssal whip 4, magic shortbow 4 (Rapid 3), staff of fire 5.
        assertEquals(5, ItemCatalog.get("dragon_axe").equipment().attackSpeed());
        assertEquals(4, ItemCatalog.get("abyssal_whip").equipment().attackSpeed());
        assertEquals(4, ItemCatalog.get("magic_shortbow").equipment().attackSpeed());
        assertEquals(5, ItemCatalog.get("staff_of_fire").equipment().attackSpeed());
        // ...and register as the item classes that carry the speed as their vanilla ATTACK_SPEED (ModItems).
        assertEquals(ItemDef.Kind.AXE, ItemCatalog.get("dragon_axe").kind());
        assertEquals(ItemDef.Kind.MELEE_WEAPON, ItemCatalog.get("abyssal_whip").kind());
        assertEquals(ItemDef.Kind.MELEE_WEAPON, ItemCatalog.get("dragon_halberd").kind());
        assertEquals(ItemDef.Kind.STAFF, ItemCatalog.get("staff_of_fire").kind());
        assertEquals(ItemDef.Kind.BOW, ItemCatalog.get("magic_shortbow").kind());
        // Dragon axe: vanilla's indicator fills in 60 MC = 5 ticks.
        assertEquals(60.0, AttackClock.vanillaPeriodMc(AttackClock.VANILLA_BASE_ATTACK_SPEED
                + AttackClock.vanillaAttackSpeedModifier(ItemCatalog.get("dragon_axe").equipment().attackSpeed())), 1e-3);
        // A held magic shortbow for 480 MC (first shot once drawn 4 MC): 10 arrows on Accurate, 14 on Rapid.
        for (boolean rapid : new boolean[]{false, true}) {
            int period = AttackClock.periodMc(AttackClock.rangedSpeed(4, rapid));
            AttackClock.Sim sim = new AttackClock.Sim(period);
            for (long t = AttackClock.HOLD_DRAW_MC; t < 480; t++) {
                sim.click(t);
            }
            assertEquals(rapid ? 14 : 10, sim.resolved.size());
        }
    }

    @Test
    void fillAndFacing() {
        assertEquals(1f, AttackClock.fill(0, 48), 1e-6);
        assertEquals(0.5f, AttackClock.fill(24, 48), 1e-6);
        assertEquals(0f, AttackClock.fill(48, 48), 1e-6);
        assertTrue(AttackClock.facing(0, 1, 0, 5, 75));
        assertTrue(AttackClock.facing(0, 1, 1, 1, 75)); // 45 degrees off
        assertFalse(AttackClock.facing(0, 1, 1, 0.1, 75)); // ~84 degrees off
        assertFalse(AttackClock.facing(0, 1, 0, -3, 75)); // behind
        assertTrue(AttackClock.facing(0, 1, 0, 0, 75)); // straight above
    }
}
