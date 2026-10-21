package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Chance;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.data.Drop;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sailing, round 40, diffed against snapshots of the OSRS Wiki under src/test/resources/osrs/sailing: shipwreck salvaging
 * success rates (the wiki's salvaging calculator), sea creature accuracy against the hull's defence, the cannon max hit
 * and accuracy formulas, lost-crate potion doses and the lit lanterns of lantern harpooning.
 */
class SailingRound40Test {

    static List<String[]> tsv(String name) throws Exception {
        List<String[]> out = new ArrayList<>();
        Path p = Path.of("src/test/resources/osrs/sailing", name);
        assertTrue(Files.exists(p), p.toString());
        for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
            if (!line.isBlank() && !line.startsWith("#")) {
                out.add(line.split("\t", -1));
            }
        }
        return out;
    }

    @Test
    void salvageSuccessFollowsTheWikiCalculator() throws Exception {
        List<String[]> rows = tsv("salvage_success.tsv");
        int wrecks = 0;
        int hooks = 0;
        for (String[] r : rows) {
            switch (r[0]) {
                case "wreck" -> {
                    SailingData.Wreck w = SailingData.WRECKS.stream().filter(x -> x.name().startsWith(r[1])).findFirst().orElse(null);
                    assertNotNull(w, r[1]);
                    assertEquals(Integer.parseInt(r[2]), w.low(), w.name());
                    assertEquals(Integer.parseInt(r[3]), w.high(), w.name());
                    wrecks++;
                }
                case "hook" -> {
                    int tier = SailingSea.METALS.indexOf(r[1].toLowerCase(Locale.ROOT));
                    assertTrue(tier >= 0, r[1]);
                    assertEquals(Integer.parseInt(r[2]), SailingSea.HOOK_BONUS_PERCENT[tier], r[1]);
                    hooks++;
                }
                case "ticks" -> assertEquals(Integer.parseInt(r[2]), r[1].equals("player") ? SailingSea.SALVAGE_TICKS
                        : SailingCrew.CREW_SALVAGE_TICKS, r[1]);
                default -> throw new AssertionError(r[0]);
            }
        }
        assertEquals(8, wrecks);
        assertEquals(7, hooks);
        // The calculator's integer steps: low/high rounded down after the hook bonus, then the skilling curve.
        SailingData.Wreck small = SailingData.wreck("small_shipwreck");
        assertEquals(Chance.lerp(15, 50, 100), SailingSea.salvageChance(small, 15, 0), 1e-12);
        assertEquals(Chance.lerp(60, 67, 135), SailingSea.salvageChance(small, 60, 6), 1e-12);
        // Better hooks and levels never lower the chance.
        for (SailingData.Wreck w : SailingData.WRECKS) {
            double prev = 0;
            for (int hook = 0; hook < 7; hook++) {
                double c = SailingSea.salvageChance(w, 90, hook);
                assertTrue(c >= prev, w.name() + " hook " + hook);
                prev = c;
            }
            assertTrue(SailingSea.salvageChance(w, 99, 0) > SailingSea.salvageChance(w, w.level(), 0), w.name());
        }
    }

    @Test
    void seaCreaturesHitThroughTheHullsDefence() throws Exception {
        int creatures = 0;
        int hulls = 0;
        for (String[] r : tsv("sea_creature_accuracy.tsv")) {
            if (r[0].equals("creature")) {
                SailingSea.SeaCreature c = SailingSea.creature(SailingSea.creatureId(r[1]));
                assertNotNull(c, r[1]);
                assertEquals(AttackType.valueOf(r[2].toUpperCase(Locale.ROOT)), c.style(), r[1]);
                assertEquals(Integer.parseInt(r[3]), c.att(), r[1]);
                boolean magic = c.style() == AttackType.MAGIC;
                assertEquals(magic ? Integer.parseInt(r[4]) : c.att(), c.attackLevel(), r[1]);
                assertEquals(magic ? Integer.parseInt(r[6]) : Integer.parseInt(r[5]), c.attackBonus(), r[1]);
                creatures++;
            } else {
                for (int tier = 0; tier < 7; tier++) {
                    assertEquals(Integer.parseInt(r[2 + tier]), SailingSea.hullDefence(r[1], tier), r[1] + " " + tier);
                }
                hulls++;
            }
        }
        assertEquals(SailingSea.CREATURES.size(), creatures);
        assertEquals(3, hulls);
        // OSRS accuracy: a bull shark ((80 + 9) x 64) against a wooden raft ((1 + 9) x 64) and a rosewood sloop ((75 + 9) x 64).
        SailingSea.SeaCreature shark = SailingSea.creature("bull_shark");
        assertEquals(Chance.hitChance(89 * 64, 10 * 64), SailingSea.creatureHitChance(shark, "raft", 0), 1e-12);
        assertEquals(Chance.hitChance(89 * 64, 84 * 64), SailingSea.creatureHitChance(shark, "sloop", 6), 1e-12);
        // The Mogre's +22 crush bonus; the vampyre kraken casts with its Magic level of 215.
        assertEquals(Chance.hitChance(49 * 86, 19 * 64), SailingSea.creatureHitChance(SailingSea.creature("sea_mogre"), "skiff", 0), 1e-12);
        assertEquals(Chance.hitChance(224 * 64, 19 * 64), SailingSea.creatureHitChance(SailingSea.creature("vampyre_kraken"), "skiff", 0),
                1e-12);
        // A better hull is hit less often.
        for (SailingSea.SeaCreature c : SailingSea.CREATURES) {
            assertTrue(SailingSea.creatureHitChance(c, "sloop", 6) < SailingSea.creatureHitChance(c, "raft", 0), c.id());
        }
    }

    @Test
    void cannonsFollowTheWikiFormulas() {
        SailingSea.Cannon bronze = SailingSea.CANNONS.get(0);
        SailingSea.Cannon dragon = SailingSea.CANNONS.get(6);
        SailingSea.Ball bronzeBall = SailingSea.ball("bronze_cannonball");
        SailingSea.Ball dragonBall = SailingSea.ball("dragon_cannonball");
        // Crew: floor((Sailing x (cannon + ball + 64) + 320) / 640); 99 Sailing, dragon/dragon: (99 x 366 + 320) / 640 = 57.
        assertEquals(57, SailingSea.cannonMaxHit(99, dragon, dragonBall, false));
        // Player: floor(0.5 + (Ranged + 8) x (cannon + ball + heavy + worn + 64) / 640); 99 Ranged, bronze: 107 x 130 / 640.
        assertEquals(107, SailingSea.effectiveRanged(99, 1.0, 1.0));
        assertEquals(22, SailingSea.playerCannonMaxHit(107, bronze, bronzeBall, 0));
        assertEquals(SailingSea.playerCannonMaxHit(107, bronze, bronzeBall, 0), SailingSea.cannonMaxHit(99, bronze, bronzeBall, true));
        // Rigour (+23% strength) on 99 Ranged: floor(99 x 1.23) + 8 = 129; dragon/dragon with +100 heavy and +20 worn.
        assertEquals(129, SailingSea.effectiveRanged(99, 1.23, 1.0));
        assertEquals((int) Math.floor(0.5 + 129 * (32 + 270 + 100 + 20 + 64) / 640.0), SailingSea.playerCannonMaxHit(129, dragon, dragonBall, 20));
        // Elite Void (x1.125) after the +8.
        assertEquals(120, SailingSea.effectiveRanged(99, 1.0, 1.125));
        // Accuracy: the attack roll uses the effective level and the manned accuracy against heavy ranged defence.
        SailingSea.SeaCreature tern = SailingSea.creature("tern");
        long def = (long) (tern.def() + 9) * (tern.dheavy() + 64);
        assertEquals(Chance.hitChance(107L * (60 + 12 + 64), def), SailingSea.cannonAccuracy(107, bronze, bronzeBall, true, 0, tern), 1e-12);
        assertEquals(Chance.hitChance(107L * (30 + 12 + 64), def), SailingSea.cannonAccuracy(SailingSea.crewEffectiveLevel(99), bronze,
                bronzeBall, false, 0, tern), 1e-12);
        assertTrue(SailingSea.cannonAccuracy(107, bronze, bronzeBall, true, 50, tern) > SailingSea.cannonAccuracy(107, bronze, bronzeBall, true, 0, tern));
    }

    @Test
    void lostCratePotionsComeWithTheWikiDoses() {
        // Wiki Lost crate: Strength potion(4), Superantipoison(2), Prayer potion(4), Stamina potion(2), Super combat potion(2),
        // Prayer regeneration potion(1); construction supplies of the wooden and oak crates are sawmill coupons.
        Set<String> potions = new HashSet<>();
        Random r = new Random(5);
        for (int tier = 0; tier < 6; tier++) {
            for (int i = 0; i < 400; i++) {
                SailingSea.Got g = SailingSea.lostCrateLoot(tier, r);
                if (ItemCatalog.get(Drop.baseOf(g.item())).kind() == com.runecraft.osrs.ItemDef.Kind.POTION) {
                    potions.add(g.item());
                }
            }
        }
        assertEquals(Set.of("strength_potion", "superantipoison(2)", "prayer_potion", "stamina_potion(2)", "super_combat_potion(2)",
                "prayer_regeneration_potion(1)"), potions);
        assertTrue(SailingSea.lostCrateItems().contains("sawmill_coupon_wood_plank"));
        assertTrue(SailingSea.lostCrateItems().contains("sawmill_coupon_oak_plank"));
    }

    @Test
    void anyLitLanternLightsLanternHarpooning() {
        // Wiki Lantern harpooning: any lit lantern; RuneCraft has these four of the wiki's seven.
        assertEquals(List.of("bug_lantern", "abyssal_lantern", "sapphire_lantern", "ld_emerald_lantern"), GatheringData.LIT_LANTERNS);
        for (String l : GatheringData.LIT_LANTERNS) {
            assertTrue(ItemCatalog.exists(l), l);
        }
    }
}
