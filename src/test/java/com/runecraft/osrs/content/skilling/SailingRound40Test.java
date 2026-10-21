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

    private static int depth(String d) {
        return switch (d) {
            case "Shallow" -> SailingShoals.SHALLOW;
            case "Moderate" -> SailingShoals.MODERATE;
            case "Deep" -> SailingShoals.DEEP;
            default -> throw new AssertionError(d);
        };
    }

    private static int deepest(String depths) {
        int max = -1;
        for (String d : depths.split(",")) {
            max = Math.max(max, depth(d));
        }
        return max;
    }

    private static String fishId(String raw) {
        return raw.toLowerCase(Locale.ROOT).replace("raw ", "").replace(' ', '_');
    }

    @Test
    void trawlingShoalsMatchTheWiki() throws Exception {
        int shoals = 0;
        int mixed = 0;
        int nets = 0;
        for (String[] r : tsv("trawling_shoals.tsv")) {
            switch (r[0]) {
                case "shoal" -> {
                    SailingShoals.Shoal s = SailingShoals.SHOALS.stream().filter(x -> x.name().equals(r[1])).findFirst().orElse(null);
                    assertNotNull(s, r[1]);
                    assertEquals(Integer.parseInt(r[2]), s.level(), r[1]);
                    assertEquals(deepest(r[3]), s.maxDepth(), r[1]);
                    assertEquals(Integer.parseInt(r[4]), s.stopTicks(), r[1]);
                    assertEquals(!r[5].contains("Fish offcuts"), s.fineBait(), r[1]);
                    List<String> bait = new ArrayList<>();
                    for (String b : r[5].split(",")) {
                        bait.add(b.equals("Fish offcuts") ? SailingShoals.OFFCUTS : SailingShoals.FINE_OFFCUTS);
                    }
                    assertEquals(bait, SailingShoals.bait(s), r[1]);
                    assertEquals(fishId(r[6]), s.fish(), r[1]);
                    assertEquals(Double.parseDouble(r[7]), s.xp(), 1e-9, r[1]);
                    assertEquals(Integer.parseInt(r[8]), s.low(), r[1]);
                    assertEquals(Integer.parseInt(r[9]), s.high(), r[1]);
                    String[] pins = r[12].split(";");
                    assertEquals(pins.length, s.pins().size(), r[1]);
                    for (int i = 0; i < pins.length; i++) {
                        String[] xy = pins[i].split(",");
                        assertEquals(Integer.parseInt(xy[0]), s.pins().get(i)[0], r[1]);
                        assertEquals(Integer.parseInt(xy[1]), s.pins().get(i)[1], r[1]);
                        assertEquals(s, SailingShoals.nearest(s.pins().get(i)[0] + 3, s.pins().get(i)[1] - 3, SailingShoals.RANGE), r[1]);
                    }
                    // The shoal's Fishing XP per catch is the raw fish's.
                    CookingData.NewFish f = CookingData.FISH.stream().filter(x -> x.id().equals(s.fish())).findFirst().orElseThrow();
                    assertEquals(s.xp(), f.fishXp(), 1e-9, r[1]);
                    assertEquals(s.level(), f.fishLevel(), r[1]);
                    shoals++;
                }
                case "mixed" -> {
                    SailingShoals.Mixed m = SailingShoals.MIXED.stream().filter(x -> x.name().equals(r[1])).findFirst().orElse(null);
                    assertNotNull(m, r[1]);
                    assertEquals(Integer.parseInt(r[2]), m.level(), r[1]);
                    assertEquals(deepest(r[3]), m.maxDepth(), r[1]);
                    assertEquals(Integer.parseInt(r[4]), m.low(), r[1]);
                    assertEquals(Integer.parseInt(r[5]), m.high(), r[1]);
                    for (String from : r[6].split(",")) {
                        SailingShoals.Shoal s = SailingShoals.SHOALS.stream().filter(x -> x.name().equals(from)).findFirst().orElseThrow();
                        assertEquals(m, SailingShoals.mixedFor(s), from);
                    }
                    java.util.Map<String, Integer> w = new java.util.LinkedHashMap<>();
                    for (String kv : r[7].split(";")) {
                        String[] p = kv.split(":");
                        w.put(fishId(p[0]), Integer.parseInt(p[1]));
                    }
                    assertEquals(w, m.fish(), r[1]);
                    mixed++;
                }
                case "rule" -> {
                    double v = Double.parseDouble(r[2]);
                    switch (r[1]) {
                        case "crew_chance_percent" -> assertEquals(v / 100, SailingShoals.CREW_CHANCE, 1e-12);
                        case "crew_roll_ticks" -> assertEquals((int) v, SailingShoals.CREW_ROLL_TICKS);
                        case "player_roll_ticks" -> {
                            assertEquals((int) v, SailingShoals.PLAYER_ROLL_TICKS);
                            assertEquals((int) v, SailingTraining.TRAWL_ROLL_TICKS);
                        }
                        case "mixed_chance_percent" -> assertEquals(v / 100, SailingShoals.MIXED_CHANCE, 1e-12);
                        case "mixed_stops" -> assertEquals((int) v, SailingShoals.MIXED_STOPS);
                        case "net_capacity" -> assertEquals((int) v, SailingShoals.NET_CAPACITY);
                        case "bait_extra_fish_percent" -> assertEquals(v / 100, SailingShoals.BAIT_EXTRA, 1e-12);
                        default -> throw new AssertionError(r[1]);
                    }
                }
                case "net" -> {
                    SailingTraining.Net n = SailingTraining.NETS.get(nets);
                    assertEquals(r[1], SailingTraining.facility(n.facility()).name());
                    assertEquals(deepest(r[2]), n.depth(), r[1]);
                    assertEquals(Integer.parseInt(r[3]), n.maxFish(), r[1]);
                    assertEquals(Double.parseDouble(r[4]), n.xpPerRoll(), 1e-9, r[1]);
                    nets++;
                }
                default -> throw new AssertionError(r[0]);
            }
        }
        assertEquals(6, shoals);
        assertEquals(3, mixed);
        assertEquals(4, nets);
        assertEquals(SailingShoals.SHOALS.size(), shoals);
    }

    @Test
    void shoalsSwimAtSeaAndChangeDepth() {
        for (SailingShoals.Shoal s : SailingShoals.SHOALS) {
            for (int[] p : s.pins()) {
                assertTrue(com.runecraft.osrs.world.Elevation.naturalHeight(com.runecraft.osrs.world.StructureDef.bx(p[0]),
                        com.runecraft.osrs.world.StructureDef.bz(p[1])) <= com.runecraft.osrs.world.Elevation.SEA_LEVEL,
                        s.name() + " pin on land: " + p[0] + "," + p[1]);
            }
        }
        // Shallow-only shoals stay shallow; others stop shallow (or deep for three-depth shoals) and rise to moderate halfway.
        assertEquals(SailingShoals.SHALLOW, SailingShoals.depthAt(SailingShoals.SHALLOW, 150, 140, true));
        assertEquals(SailingShoals.SHALLOW, SailingShoals.depthAt(SailingShoals.MODERATE, 100, 10, true));
        assertEquals(SailingShoals.MODERATE, SailingShoals.depthAt(SailingShoals.MODERATE, 100, 50, false));
        assertEquals(SailingShoals.DEEP, SailingShoals.depthAt(SailingShoals.DEEP, 70, 10, true));
        assertEquals(SailingShoals.SHALLOW, SailingShoals.depthAt(SailingShoals.DEEP, 70, 10, false));
        assertEquals(SailingShoals.MODERATE, SailingShoals.depthAt(SailingShoals.DEEP, 70, 40, true));
        // Catches: 1..max fish, halved at the wrong depth, +20% per fish when baited.
        Random r = new Random(9);
        long right = 0;
        long wrong = 0;
        long baited = 0;
        int n = 20_000;
        for (int i = 0; i < n; i++) {
            int a = SailingShoals.fishCaught(5, true, false, r);
            assertTrue(a >= 1 && a <= 5);
            right += a;
            wrong += SailingShoals.fishCaught(5, false, false, r);
            baited += SailingShoals.fishCaught(5, true, true, r);
        }
        assertEquals(3.0, right / (double) n, 0.05);
        assertEquals(1.5, wrong / (double) n, 0.05);
        assertEquals(3.6, baited / (double) n, 0.06);
        // A vibrant shoal gives bluefin and marlin half and half.
        SailingShoals.Mixed vibrant = SailingShoals.mixedFor(SailingShoals.shoal("marlin_shoal"));
        int marlin = 0;
        for (int i = 0; i < 4000; i++) {
            marlin += SailingShoals.mixedFish(vibrant, r).equals("marlin") ? 1 : 0;
        }
        assertEquals(0.5, marlin / 4000.0, 0.03);
        // The catch chance is the wiki's skilling curve: marlin at 99 Fishing, (1 + 20) / 256.
        assertEquals(21 / 256.0, SailingShoals.catchChance(99, 4, 20), 1e-12);
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
