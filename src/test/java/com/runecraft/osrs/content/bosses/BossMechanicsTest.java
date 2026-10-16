package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.bosses.BossMechanics.YamaContract;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The scripted boss mechanics' numbers against the OSRS Wiki. */
class BossMechanicsTest {
    @Test
    void yamaNumbers() {
        BossDef yama = ContentRegistry.boss("yama");
        assertNotNull(yama);
        assertEquals(BossMechanics.YAMA_HITPOINTS, yama.hitpoints);
        assertEquals(BossMechanics.YAMA_SPEED, yama.attackSpeed);
        // Meteor Strike: lethal within a tile of the centre, 80 at two tiles, 40 at three, nothing at four.
        assertEquals(121, BossMechanics.meteorDamage(0));
        assertEquals(121, BossMechanics.meteorDamage(1));
        assertEquals(80, BossMechanics.meteorDamage(2));
        assertEquals(40, BossMechanics.meteorDamage(3));
        assertEquals(0, BossMechanics.meteorDamage(4));
        // Glyph of Fire: 2 / 6 / 9 / 12 as Yama gets closer.
        assertEquals(2, BossMechanics.glyphCost(6));
        assertEquals(2, BossMechanics.glyphCost(4));
        assertEquals(6, BossMechanics.glyphCost(3));
        assertEquals(9, BossMechanics.glyphCost(2));
        assertEquals(12, BossMechanics.glyphCost(1));
        // Void flares: 44 at full health, 25 in phase 3, scaled by remaining health.
        assertEquals(44, BossMechanics.flareExplosion(1.0, false));
        assertEquals(22, BossMechanics.flareExplosion(0.5, false));
        assertEquals(25, BossMechanics.flareExplosion(1.0, true));
        assertEquals(0, BossMechanics.flareExplosion(0, false));
    }

    @Test
    void yamaContractsAreRealItems() {
        Set<String> items = new HashSet<>();
        for (YamaContract c : YamaContract.values()) {
            assertNotNull(ItemCatalog.get(c.item), c.item);
            assertTrue(items.add(c.item));
            assertEquals(c, YamaContract.byItem(c.item));
            assertTrue(c.hitpoints >= BossMechanics.YAMA_HITPOINTS);
        }
        assertNull(YamaContract.byItem("dossier"));
        // Wiki contract stat table: bloodied blows 3,000 HP, glyphic attenuation 2,875, the others 2,500.
        assertEquals(3000, YamaContract.BLOODIED_BLOWS.hitpoints);
        assertEquals(2875, YamaContract.GLYPHIC_ATTENUATION.hitpoints);
        assertEquals(2500, YamaContract.SENSORY_CLOUDING.hitpoints);
        assertEquals("Contract of divine severance", YamaContract.DIVINE_SEVERANCE.displayName());
        assertEquals(53, (int) Math.round(BossMechanics.YAMA_AUTO_MAX * BossMechanics.yamaHitScale(YamaContract.FORFEIT_BREATH)));
        assertEquals(1.0, BossMechanics.yamaHitScale(null));
    }

    @Test
    void sireStunChances() {
        assertEquals(25, BossMechanics.sireStunChance("shadow_rush"));
        assertEquals(50, BossMechanics.sireStunChance("shadow_burst"));
        assertEquals(75, BossMechanics.sireStunChance("shadow_blitz"));
        assertEquals(100, BossMechanics.sireStunChance("shadow_barrage"));
        assertEquals(0, BossMechanics.sireStunChance("smoke_barrage"));
        assertEquals(0, BossMechanics.sireStunChance(null));
    }

    @Test
    void vardorvisRamp() {
        assertEquals(0, BossMechanics.vardorvisDefenceReduction(0));
        assertEquals(10, BossMechanics.vardorvisDefenceReduction(100));
        assertEquals(71, BossMechanics.vardorvisDefenceReduction(700));
        assertEquals(32, BossMechanics.vardorvisMaxHit(1.0, false));
        assertEquals(43, BossMechanics.vardorvisMaxHit(0.0, false));
        assertEquals(46, BossMechanics.vardorvisMaxHit(1.0, true));
        assertEquals(61, BossMechanics.vardorvisMaxHit(0.0, true));
        assertEquals(1, BossMechanics.vardorvisAxes(700, false));
        assertEquals(2, BossMechanics.vardorvisAxes(500, false));
        assertEquals(3, BossMechanics.vardorvisAxes(200, false));
        assertEquals(4, BossMechanics.vardorvisAxes(200, true));
        assertEquals(1, BossMechanics.vardorvisDarts(600));
        assertEquals(2, BossMechanics.vardorvisDarts(300));
        assertEquals(3, BossMechanics.vardorvisDarts(100));
    }

    @Test
    void maggotKingAndTitans() {
        assertEquals(0, BossMechanics.maggotLarvae(1400, 0));
        assertEquals(1, BossMechanics.maggotLarvae(1000, 2));
        assertEquals(4, BossMechanics.maggotLarvae(200, 0));
        assertEquals(1, BossMechanics.maggotLarvae(200, 3));
        assertEquals(4, BossMechanics.maggotAttacksBeforeScreech(0));
        assertEquals(8, BossMechanics.maggotAttacksBeforeScreech(4));
        assertTrue(BossMechanics.dousesFire("water_blast"));
        assertTrue(BossMechanics.dousesFire("ice_barrage"));
        assertTrue(!BossMechanics.dousesFire("water_bolt"));
        assertTrue(BossMechanics.meltsIce("fire_surge"));
        assertTrue(!BossMechanics.meltsIce("fire_bolt"));
    }

    @Test
    void phosaniSleepwalkers() {
        assertEquals(2, BossMechanics.sleepwalkers(1));
        assertEquals(3, BossMechanics.sleepwalkers(2));
        assertEquals(4, BossMechanics.sleepwalkers(3));
        // Wiki: with 4 sleepwalkers each one killed takes 36 off; none killed always 149, all killed 5.
        assertEquals(149, BossMechanics.sleepwalkerBlast(4, 4));
        assertEquals(113, BossMechanics.sleepwalkerBlast(3, 4));
        assertEquals(5, BossMechanics.sleepwalkerBlast(0, 4));
        assertEquals(77, BossMechanics.sleepwalkerBlast(1, 2));
        assertEquals(53, BossMechanics.sleepwalkerBlast(1, 3));
    }

    @Test
    void minionsExist() {
        Set<String> ids = new HashSet<>();
        for (MonsterDef m : new BossesContent().monsters()) {
            ids.add(m.id);
        }
        for (String id : new String[]{BossMechanics.YAMA_JUDGE, BossMechanics.YAMA_VOID_FLARE, BossMechanics.UNSTABLE_ICE,
                BossMechanics.UR_MAGGOT_LARVA, BossMechanics.PHOSANI_SLEEPWALKER, BossMechanics.GROWTHLING,
                BossMechanics.STRANGLER_SPORE, BossMechanics.ARAXYTE_EGG, "acidic_araxyte", "mirrorback_araxyte", "ruptura_araxyte",
                "nightmare_parasite"}) {
            assertTrue(ids.contains(id), id);
        }
    }

    @Test
    void bryophytaGrowthlings() {
        // OSRS Wiki "Growthling": level 37, 10 hitpoints, slash max 5 every 4 ticks; three at a time, 1/5 per attack.
        MonsterDef g = new BossesContent().monsters().stream().filter(m -> m.id.equals(BossMechanics.GROWTHLING)).findFirst().orElseThrow();
        assertEquals(37, g.combatLevel);
        assertEquals(10, g.hitpoints);
        assertEquals(4, g.attackSpeed);
        assertEquals(5, g.attacks.get(0).maxHit());
        assertEquals(com.runecraft.osrs.AttackType.SLASH, g.attacks.get(0).style());
        assertEquals(3, BossMechanics.GROWTHLINGS);
        assertEquals(5, BossMechanics.GROWTHLING_CHANCE);
        assertEquals(20, BossMechanics.GROWTHLING_COOLDOWN);
        // Only an axe (not the blessed axe) or magic secateurs finishes one.
        assertTrue(BossMechanics.cutsGrowthling("rune_axe", true));
        assertTrue(BossMechanics.cutsGrowthling("magic_secateurs", false));
        assertFalse(BossMechanics.cutsGrowthling("blessed_axe", true));
        assertFalse(BossMechanics.cutsGrowthling("rune_battleaxe", false));
        assertFalse(BossMechanics.cutsGrowthling(null, false));
        // "Quick Cutter": all three within 3 seconds (5 ticks) of the first dying.
        assertTrue(BossMechanics.quickCut(100, 105, 3));
        assertFalse(BossMechanics.quickCut(100, 106, 3));
        assertFalse(BossMechanics.quickCut(100, 101, 2));
    }

    @Test
    void oborNumbers() {
        // Melee 22, ranged 26 halved (not blocked) by Protect from Missiles; "Back to the Wall" allows a one-square push.
        assertEquals(22, BossMechanics.OBOR_MELEE_MAX);
        assertEquals(26, BossMechanics.OBOR_RANGED_MAX);
        assertEquals(13, BossMechanics.oborRangedThroughPrayer(26));
        assertFalse(BossMechanics.oborPushedTooFar(0));
        assertFalse(BossMechanics.oborPushedTooFar(1));
        assertTrue(BossMechanics.oborPushedTooFar(2));
        assertEquals(22, com.runecraft.osrs.data.Boss.OBOR.maxHit());
    }

    @Test
    void vardorvisBleed() {
        // 3 every 3 ticks for 14 ticks standing still (5 hits = 15), every tick while running.
        BossMechanics.Bleed still = new BossMechanics.Bleed();
        still.start();
        int total = 0;
        int hits = 0;
        for (int t = 0; t < 20; t++) {
            int d = still.tick(false);
            total += d;
            hits += d > 0 ? 1 : 0;
        }
        assertEquals(5, hits);
        assertEquals(15, total);
        assertFalse(still.active());
        BossMechanics.Bleed running = new BossMechanics.Bleed();
        running.start();
        int ran = 0;
        for (int t = 0; t < 20; t++) {
            ran += running.tick(true);
        }
        assertEquals(14 * 3, ran);
        assertEquals(35, BossMechanics.VARDORVIS_AXE_MAX);
        assertEquals(96, BossMechanics.VARDORVIS_AXE_MAX_AWAKENED); // OSRS Wiki: "Swinging Axes damage increased from 35 to 96"
        assertEquals(25, BossMechanics.VARDORVIS_STRANGLE_DAMAGE);
        assertEquals(5, BossMechanics.VARDORVIS_STRANGLE_TICKS);
    }

    @Test
    void burns() {
        // OSRS Wiki "Burn": 1 every 4 ticks, a full burn is 10 hits; up to 5 stack, each adding 1 per hit.
        BossMechanics.Burn b = new BossMechanics.Burn();
        b.add(BossMechanics.BURN_FULL);
        int total = 0;
        for (int t = 0; t < 40; t++) {
            int d = b.tick();
            if (d > 0) {
                assertEquals(3, t % 4);
            }
            total += d;
        }
        assertEquals(10, total);
        assertEquals(0, b.stacks());
        for (int i = 0; i < 7; i++) {
            b.add(BossMechanics.YAMA_METEOR_BURN);
        }
        assertEquals(5, b.stacks());
        for (int t = 0; t < 3; t++) {
            assertEquals(0, b.tick());
        }
        assertEquals(5, b.tick());
        assertEquals(8, BossMechanics.YAMA_METEOR_BURN);
    }

    @Test
    void yamaDefences() {
        com.runecraft.osrs.AttackType magic = com.runecraft.osrs.AttackType.MAGIC;
        // OSRS Wiki infobox: magic -30 against a meleeing target, +60 against a mage, 81 in phase 3.
        assertEquals(-30, BossMechanics.yamaDefenceBonus(magic, false, false, null));
        assertEquals(60, BossMechanics.yamaDefenceBonus(magic, true, false, null));
        assertEquals(81, BossMechanics.yamaDefenceBonus(magic, true, true, null));
        assertEquals(135, BossMechanics.yamaDefenceBonus(com.runecraft.osrs.AttackType.STAB, false, true, null));
        assertEquals(108, BossMechanics.yamaDefenceBonus(com.runecraft.osrs.AttackType.SLASH, false, true, null));
        assertEquals(449, BossMechanics.yamaDefenceBonus(com.runecraft.osrs.AttackType.CRUSH, false, true, null));
        assertEquals(297, BossMechanics.yamaRangedDefences(true, null)[1]);
        assertEquals(333, BossMechanics.yamaRangedDefences(false, null)[0]);
        // Glyphic attenuation: 666 against everything but crush.
        assertEquals(666, BossMechanics.yamaDefenceBonus(magic, false, false, YamaContract.GLYPHIC_ATTENUATION));
        assertEquals(80, BossMechanics.yamaDefenceBonus(com.runecraft.osrs.AttackType.CRUSH, false, false, YamaContract.GLYPHIC_ATTENUATION));
        // The base phase 1-2 bonuses match the BossDef.
        BossDef yama = ContentRegistry.boss("yama");
        assertEquals(yama.defenceBonus(com.runecraft.osrs.AttackType.STAB),
                BossMechanics.yamaDefenceBonus(com.runecraft.osrs.AttackType.STAB, false, false, null));
        assertEquals(yama.defenceBonus(com.runecraft.osrs.AttackType.CRUSH),
                BossMechanics.yamaDefenceBonus(com.runecraft.osrs.AttackType.CRUSH, false, false, null));
        // Fire orbs are magic, shadow orbs ranged.
        assertEquals(magic, ModernBosses.YAMA_FIRE.style());
        assertEquals(com.runecraft.osrs.AttackType.RANGED, ModernBosses.YAMA_SHADOW.style());
    }

    @Test
    void maggotCarrion() {
        assertEquals(1, BossMechanics.MAGGOT_CARRION_HEAL);
        assertTrue(BossMechanics.SCRIPTED_ATTACKS.containsAll(Set.of("bleed", "strangle", "burn", "knockback")));
    }

    @Test
    void nightmareParasites() {
        assertEquals(18, BossMechanics.PARASITE_BURST_TICKS);
        assertEquals(40, BossMechanics.parasiteHitpoints(true));
        assertEquals(80, BossMechanics.parasiteHitpoints(false));
        java.util.Random r = new java.util.Random(7);
        for (int i = 0; i < 500; i++) {
            int heal = BossMechanics.parasiteHeal(r);
            assertTrue(heal >= 31 && heal <= 50, "heal " + heal);
            assertTrue(BossMechanics.parasiteBurst(r, true) < BossMechanics.parasiteBurst(r, false));
        }
        assertEquals(100, BossMechanics.TERRIBLE_PARENT_MAX_HEAL);
        assertTrue(BossMechanics.SCRIPTED_ATTACKS.contains("parasite_burst"));
    }

    @Test
    void muspahShieldAndSpikes() {
        assertEquals(75, BossMechanics.MUSPAH_SHIELD);
        assertEquals(2, BossMechanics.MUSPAH_SHIELD_PER_ATTACK);
        assertEquals(33, BossMechanics.MUSPAH_SAPPHIRE_DRAIN);
        assertEquals(0.25, BossMechanics.MUSPAH_SAPPHIRE_CHANCE);
        assertEquals(17, BossMechanics.muspahSpikeHeal(23)); // ~75%
        assertEquals(6, BossMechanics.muspahCorruptionDrain(false)); // 1 + 2 + 3
        assertEquals(12, BossMechanics.muspahCorruptionDrain(true)); // 2 + 4 + 6
        assertFalse(BossMechanics.versatileDrainer(Set.of(BossMechanics.DRAIN_SMITE, BossMechanics.DRAIN_SAPPHIRE)));
        assertTrue(BossMechanics.versatileDrainer(Set.of(BossMechanics.DRAIN_SMITE, BossMechanics.DRAIN_SAPPHIRE,
                BossMechanics.DRAIN_CORRUPTION)));
        // A 2x2 footprint at (10, 10): the ring is the 12 tiles around it.
        Set<Long> spikes = new HashSet<>();
        for (int x = 9; x <= 12; x++) {
            for (int z = 9; z <= 12; z++) {
                if (x == 9 || z == 9 || x == 12 || z == 12) {
                    spikes.add(BossMechanics.tile(x, z));
                }
            }
        }
        assertEquals(12, spikes.size());
        assertTrue(BossMechanics.muspahSurrounded(spikes, 10, 10, 2));
        spikes.remove(BossMechanics.tile(12, 11));
        assertFalse(BossMechanics.muspahSurrounded(spikes, 10, 10, 2));
        assertEquals(BossMechanics.tile(-3, 5), BossMechanics.tile(-3, 5));
        assertEquals(-3, (int) (BossMechanics.tile(-3, -5) >> 32));
        assertEquals(-5, (int) BossMechanics.tile(-3, -5));
        // Growth only spreads next to existing spikes, inside the radius, and eventually fills the area.
        Set<Long> grow = new HashSet<>(Set.of(BossMechanics.tile(0, 0)));
        java.util.Random r = new java.util.Random(1);
        for (int wave = 0; wave < 40; wave++) {
            for (long t : BossMechanics.muspahSpikeGrowth(grow, 0, 0, 3, 0.35, r)) {
                assertTrue(Math.abs((int) (t >> 32)) <= 3 && Math.abs((int) t) <= 3);
                grow.add(t);
            }
        }
        assertEquals(49, grow.size());
        assertTrue(BossMechanics.muspahSurrounded(grow, 0, 0, 1));
    }

    @Test
    void araxxorEggs() {
        java.util.List<String> eggs = BossMechanics.araxxorEggs(0);
        assertEquals(9, eggs.size());
        assertEquals(java.util.List.of("acidic_araxyte", "mirrorback_araxyte", "ruptura_araxyte"), eggs.subList(0, 3));
        assertEquals(eggs.subList(0, 3), eggs.subList(6, 9)); // the last two sets copy the first
        assertEquals("mirrorback_araxyte", BossMechanics.araxxorEggs(1).get(0));
        assertEquals("ruptura_araxyte", BossMechanics.araxxorEggs(2).get(0));
        assertFalse(BossMechanics.araxxorHatches(2));
        assertTrue(BossMechanics.araxxorHatches(3));
        assertFalse(BossMechanics.araxxorHatches(8));
        assertTrue(BossMechanics.araxxorHatches(9));
        assertTrue(BossMechanics.araxxorHatches(15));
        assertEquals(58, BossMechanics.araxyteHitpoints(0));
        assertEquals(18, BossMechanics.araxyteHitpoints(40));
        assertEquals(1, BossMechanics.araxyteHitpoints(58));
        assertEquals(0, BossMechanics.araxyteHitpoints(59)); // more than the araxyte has: the egg crumbles
        MonsterDef egg = new BossesContent().monsters().stream().filter(m -> m.id.equals(BossMechanics.ARAXYTE_EGG)).findFirst()
                .orElseThrow();
        assertEquals(65, egg.hitpoints);
    }

    /** Awakened Vardorvis: the bleed deals 5 per hit (3 normally), on the same 14-tick schedule. */
    @Test
    void awakenedVardorvisBleed() {
        BossMechanics.Bleed b = new BossMechanics.Bleed();
        b.start(BossMechanics.VARDORVIS_BLEED_DAMAGE_AWAKENED);
        int total = 0;
        while (b.active()) {
            total += b.tick(false);
        }
        assertEquals(25, total);
        b.start();
        total = 0;
        while (b.active()) {
            total += b.tick(false);
        }
        assertEquals(15, total);
    }

    /** Moons of Peril: flat armour and the Eclipse Moon's parry check. */
    @Test
    void moonsOfPeril() {
        assertEquals(14, BossMechanics.flatArmour(20, BossMechanics.ECLIPSE_FLAT_ARMOUR));
        assertEquals(0, BossMechanics.flatArmour(3, BossMechanics.ECLIPSE_FLAT_ARMOUR));
        assertEquals(1, BossMechanics.flatArmour(5, BossMechanics.ECLIPSE_FLAT_ARMOUR_CLONES));
        assertEquals(15, BossMechanics.flatArmour(10, BossMechanics.BLUE_MOON_FLAT_ARMOUR));
        assertEquals(0, BossMechanics.flatArmour(0, BossMechanics.BLUE_MOON_FLAT_ARMOUR), "a miss stays a miss");
        assertEquals(15, BossMechanics.ECLIPSE_CLONE_CYCLES * BossMechanics.ECLIPSE_CLONES_PER_CYCLE);
        // Minecraft yaw 0 faces +z (south), 90 faces -x (west), 180 faces -z.
        assertTrue(BossMechanics.faces(0, 0, 3));
        assertTrue(BossMechanics.faces(90, -3, 0));
        assertTrue(BossMechanics.faces(0, 1, 3), "a tile off still parries");
        assertFalse(BossMechanics.faces(0, 0, -3));
        assertFalse(BossMechanics.faces(0, 3, 0));
        assertFalse(BossMechanics.faces(180, 0, 3));
    }

    /** The Hueycoatl: pillar prayers per team size, the tail's damage cap. */
    @Test
    void hueycoatl() {
        int[][] table = {{1, 1}, {5, 1}, {6, 2}, {8, 2}, {9, 3}, {11, 3}, {12, 4}, {14, 4}, {15, 5}, {17, 5}, {18, 6}, {20, 6}};
        for (int[] row : table) {
            assertEquals(row[1], BossMechanics.hueyPrayersPerPillar(row[0]), "team of " + row[0]);
        }
        assertFalse(BossMechanics.hueyPillarsCharged(1, 1, 0, 2), "a duo can't charge all three");
        assertTrue(BossMechanics.hueyPillarsCharged(1, 1, 1, 3));
        assertFalse(BossMechanics.hueyPillarsCharged(2, 2, 1, 6));
        assertTrue(BossMechanics.hueyPillarsCharged(2, 2, 2, 6));
        assertEquals(4, BossMechanics.hueyTailDamage(30, false));
        assertEquals(0, BossMechanics.hueyTailDamage(0, false));
        assertEquals(9, BossMechanics.hueyTailDamage(30, true));
        assertEquals(1, BossMechanics.hueyTailDamage(0, true), "crush rounds misses up to 1");
        assertEquals(4050, BossMechanics.HUEY_BODY_SEGMENTS * BossMechanics.HUEY_BODY_HP + 2500 + BossMechanics.HUEY_TAIL_HP,
                "4,050 damage for the whole encounter");
        assertNotNull(ItemCatalog.get("hueycoatl_hide_body"));
        assertNotNull(ItemCatalog.get("hueycoatl_hide_vambraces"));
        Set<String> ids = new HashSet<>();
        ContentRegistry.monsters().forEach(m -> ids.add(m.id));
        assertTrue(ids.contains(BossMechanics.HUEYCOATL_BODY));
        assertTrue(ids.contains(BossMechanics.HUEYCOATL_TAIL));
    }

    /** Ruptura araxytes: 70 under a player at full health, 21 less per tile; 80/64/... at least 33 on other creatures. */
    @Test
    void rupturaExplosion() {
        assertEquals(70, BossMechanics.rupturaPlayerDamage(0, 1.0));
        assertEquals(49, BossMechanics.rupturaPlayerDamage(1, 1.0));
        assertEquals(28, BossMechanics.rupturaPlayerDamage(2, 1.0));
        assertEquals(7, BossMechanics.rupturaPlayerDamage(3, 1.0));
        assertEquals(0, BossMechanics.rupturaPlayerDamage(4, 1.0));
        assertEquals(35, BossMechanics.rupturaPlayerDamage(0, 0.5));
        assertEquals(80, BossMechanics.rupturaEnemyDamage(0));
        assertEquals(64, BossMechanics.rupturaEnemyDamage(1));
        assertEquals(33, BossMechanics.rupturaEnemyDamage(3));
        assertEquals(0, BossMechanics.rupturaEnemyDamage(5));
    }
}
