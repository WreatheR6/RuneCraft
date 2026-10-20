package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.systems.CombatAchievementRules;
import com.runecraft.osrs.content.systems.CombatAchievementRules.Metrics;
import com.runecraft.osrs.data.MonsterStatsTable;
import com.runecraft.osrs.data.Untradeables;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Doom of Mokhaiotl fight mechanics, DT2 tablets and quartzes, and Demonic Brutus' revive against the OSRS Wiki. */
class DoomMechanicsTest {
    @Test
    void larvaeFollowTheWiki() {
        // 10 healed and 1 damage at no stacks; a giant larva counts three times.
        assertEquals(10, DoomDelves.larvaHeal(0, false));
        assertEquals(1, DoomDelves.larvaDamage(0, false, 1));
        assertEquals(30, DoomDelves.larvaHeal(0, true));
        assertEquals(3, DoomDelves.larvaDamage(0, true, 8));
        // Damage caps: 12 before delve 8, then 20 (giant 30).
        assertEquals(12, DoomDelves.larvaDamage(50, false, 7));
        assertEquals(20, DoomDelves.larvaDamage(50, false, 8));
        assertEquals(30, DoomDelves.larvaDamage(50, true, 9));
        // 2 hitpoints (giant 4); non-demonbane hits are capped at 1, a demonbane hit kills a small larva.
        assertEquals(2, DoomDelves.larvaHitpoints(false));
        assertEquals(4, DoomDelves.larvaHitpoints(true));
        assertEquals(1, DoomDelves.larvaHit(40, false));
        assertEquals(0, DoomDelves.larvaHit(0, false));
        assertEquals(2, DoomDelves.larvaHit(0, true));
        // Two larvae at delves 5-7, one giant from delve 8.
        assertEquals(1, DoomDelves.larvaeOutsideShield(4));
        assertEquals(2, DoomDelves.larvaeOutsideShield(6));
        assertEquals(1, DoomDelves.larvaeOutsideShield(8));
        assertTrue(DoomDelves.giantLarvae(8));
        assertFalse(DoomDelves.giantLarvae(7));
        assertEquals(500, DoomDelves.SHIELD_POINTS);
        assertEquals(100, DoomDelves.LARVA_SHIELD_DAMAGE);
        assertEquals(3, DoomDelves.SHIELD_FROM);
        assertEquals(5, DoomDelves.BURROW_FROM);
    }

    @Test
    void meleePunish() {
        assertEquals(30, DoomDelves.punishBonus(150));
        // OSRS Wiki strategies: "8 tick delay at delves 1 and 2, 7 ticks at delves 3-6, and 6 ticks at delves 7+".
        assertEquals(8, DoomDelves.punishDelay(1));
        assertEquals(8, DoomDelves.punishDelay(2));
        assertEquals(7, DoomDelves.punishDelay(3));
        assertEquals(7, DoomDelves.punishDelay(6));
        assertEquals(6, DoomDelves.punishDelay(7));
        assertEquals(6, DoomDelves.punishDelay(9));
    }

    /** The strategy page's table of attacks by delve level. */
    @Test
    void attacksByDelveLevel() {
        int[] projectiles = {1, 2, 2, 3, 3, 3, 3, 6, 6};
        int[] shockwaves = {1, 1, 2, 2, 3, 3, 4, 5, 5};
        int[] beam = {60, 60, 60, 80, 80, 99, 99, 99, 99};
        int[] trample = {0, 0, 0, 0, 10, 20, 30, 40, 40};
        int[] zooms = {0, 0, 0, 0, 2, 3, 3, 3, 3};
        for (int lvl = 1; lvl <= 9; lvl++) {
            assertEquals(projectiles[lvl - 1], DoomDelves.rockThrowProjectiles(lvl), "rock throw, delve " + lvl);
            assertEquals(shockwaves[lvl - 1], DoomDelves.shockwaves(lvl), "shockwaves, delve " + lvl);
            assertEquals(beam[lvl - 1], DoomDelves.chargeMaxHit(lvl), "beam, delve " + lvl);
            assertEquals(trample[lvl - 1], DoomDelves.trampleDamage(lvl), "trample, delve " + lvl);
            assertEquals(zooms[lvl - 1], DoomDelves.zooms(lvl), "zooms, delve " + lvl);
            assertEquals(lvl >= 6, DoomDelves.carSlams(lvl), "car slams, delve " + lvl);
        }
        assertEquals(20, DoomDelves.SHOCKWAVE_WINDUP);
        assertEquals(30, DoomDelves.SHOCKWAVE_MIN);
        assertEquals(41, DoomDelves.SHOCKWAVE_MAX);
        assertEquals(7, DoomDelves.ROCK_THROW_TICKS);
        assertEquals(21, DoomDelves.LARVA_EXPLOSION_MAX);
        assertEquals(40, DoomDelves.MELEE_MAX);
        assertEquals(0.5, DoomDelves.earthenShieldSpeed(1));
        assertEquals(1.0, DoomDelves.earthenShieldSpeed(2));
        // The Doom's own BossDef carries no invented phase (the delve script runs its phases).
        assertTrue(ContentRegistry.boss(DoomDelves.BOSS).phases.isEmpty());
    }

    @Test
    void heldLootRoundTrips() {
        List<DoomDelves.Loot> loot = List.of(new DoomDelves.Loot("demon_tear", 150), new DoomDelves.Loot("eye_of_ayak", 1));
        assertEquals(loot, DoomDelves.decode(DoomDelves.encode(loot)));
        assertTrue(DoomDelves.decode("").isEmpty());
        assertEquals(1, DoomDelves.decode("broken;coal:3").size());
    }

    @Test
    void doomTasksReadTheFightFlags() {
        Metrics m = new Metrics();
        m.delve = 8;
        assertTrue(CombatAchievementRules.meets("delve=8 noflag=grub_absorbed", m));
        m.flags.add(DoomDelves.FLAG_GRUB_ABSORBED);
        assertFalse(CombatAchievementRules.meets("delve=8 noflag=grub_absorbed", m));
        m.flags.add(DoomDelves.FLAG_EXPOSED);
        assertTrue(CombatAchievementRules.meets("flag=exposed", m));
    }

    private static int rate(BossDef b, String item) {
        return b.drops.stream().filter(d -> d.item().equals(item)).findFirst().orElseThrow().rarity();
    }

    @Test
    void dt2TabletsAndQuartzes() {
        String[][] bosses = {{"duke_sucellus", "frozen_tablet", "ice_quartz"}, {"the_leviathan", "scarred_tablet", "smoke_quartz"},
                {"the_whisperer", "sirenic_tablet", "shadow_quartz"}, {"vardorvis", "strangled_tablet", "blood_quartz"}};
        for (String[] b : bosses) {
            BossDef def = ContentRegistry.boss(b[0]);
            assertEquals(25, rate(def, b[1]), b[0]);
            assertEquals(200, rate(def, b[2]), b[0]);
            assertTrue(ItemCatalog.exists(b[1]) && ItemCatalog.exists(b[2]));
            assertTrue(Untradeables.untradeable(b[1]) && Untradeables.untradeable(b[2]));
        }
    }

    @Test
    void vardorvisStatsAtFullHealth() {
        MonsterStatsTable.Row r = MonsterStatsTable.row("vardorvis");
        assertEquals(280, r.attack());
        assertEquals(270, r.strength());
        assertEquals(215, r.defence());
        assertEquals(215, r.magic());
    }

    @Test
    void demonicBrutusRevives() {
        assertEquals(487, ModernBosses.DEMONIC_REVIVE_HP);
        assertEquals(750, ContentRegistry.boss("demonic_brutus").hitpoints);
    }
}
