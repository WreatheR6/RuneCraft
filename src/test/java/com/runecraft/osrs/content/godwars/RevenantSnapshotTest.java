package com.runecraft.osrs.content.godwars;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The revenants against the wiki: their stats ({@code osrs/godwars/revenants.tsv}, the infoboxes) and the skulled revenant
 * ork's whole drop table ({@code osrs/godwars/revenant_ork_drops.tsv}, the wiki's money making guide), plus the rare rates the
 * wiki gives for the knight and the imp.
 */
class RevenantSnapshotTest {
    private static List<String[]> rows(String file) throws IOException {
        List<String[]> out = new ArrayList<>();
        for (String line : Files.readAllLines(Path.of("src/test/resources/osrs/godwars/" + file))) {
            if (!line.isBlank() && !line.startsWith("#")) {
                out.add(line.split("\t"));
            }
        }
        return out;
    }

    @Test
    void revenantStatsMatchTheWiki() throws IOException {
        List<String[]> rows = rows("revenants.tsv");
        assertEquals(RevenantRewards.REVENANTS.size(), rows.size());
        int total = 0;
        for (String[] r : rows) {
            RevenantRewards.Revenant rev = RevenantRewards.revenant(r[0]);
            assertNotNull(rev, r[0]);
            assertEquals(Integer.parseInt(r[1]), rev.level(), r[0]);
            assertEquals(Integer.parseInt(r[2]), rev.hitpoints(), r[0]);
            assertEquals(Integer.parseInt(r[3]), rev.maxHit(), r[0]);
            assertEquals(AttackType.valueOf(r[4]), rev.melee(), r[0]);
            assertEquals(Integer.parseInt(r[6]), rev.respawnTicks(), r[0]);
            assertEquals(Integer.parseInt(r[7]), rev.count(), r[0]);
            assertEquals(rev.count(), RevenantCaves.SPAWNS.stream().filter(s -> s.creature().equals(r[0])).count(), r[0] + " spawns");
            total += rev.count();
            MonsterDef m = ContentRegistry.monster(r[0]);
            assertNotNull(m, r[0]);
            assertEquals(Integer.parseInt(r[1]), m.combatLevel, r[0]);
            assertEquals(Integer.parseInt(r[2]), m.hitpoints, r[0]);
            assertEquals(Integer.parseInt(r[5]), m.attackSpeed, r[0] + " attack speed");
            for (AttackDef a : m.attacks) {
                assertEquals(Integer.parseInt(r[3]), a.maxHit(), r[0] + " " + a.name());
            }
            assertTrue(m.attacks.stream().anyMatch(a -> a.style() == AttackType.valueOf(r[4])), r[0] + " melee style");
        }
        assertEquals(30, total, "the wiki's thirty revenants");
    }

    @Test
    void skulledOrkTableMatchesTheWiki() throws IOException {
        RevenantRewards.Revenant ork = RevenantRewards.revenant("revenant_ork");
        Map<String, Double> perKill = new HashMap<>();
        Map<String, int[]> qty = new HashMap<>();
        for (String[] r : rows("revenant_ork_drops.tsv")) {
            perKill.merge(r[0], 1 / Double.parseDouble(r[3]), Double::sum);
            int[] q = qty.computeIfAbsent(r[0], k -> new int[]{Integer.MAX_VALUE, 0});
            q[0] = Math.min(q[0], Integer.parseInt(r[1]));
            q[1] = Math.max(q[1], Integer.parseInt(r[2]));
            assertNotNull(ItemCatalog.get(r[0]), r[0]);
        }
        List<String> wrong = new ArrayList<>();
        for (Map.Entry<String, Double> e : perKill.entrySet()) {
            String item = e.getKey();
            double ours = ourChance(ork, item);
            if (Math.abs(ours - e.getValue()) / e.getValue() > 0.025) {
                wrong.add(item + ": wiki 1/" + String.format("%.2f", 1 / e.getValue()) + ", ours 1/" + String.format("%.2f", 1 / ours));
            }
            int[] q = qty.get(item);
            int[] oq = ourQuantity(ork, item);
            if (oq[0] != q[0] || oq[1] != q[1]) {
                wrong.add(item + ": wiki " + q[0] + "-" + q[1] + ", ours " + oq[0] + "-" + oq[1]);
            }
        }
        assertTrue(wrong.isEmpty(), String.join("\n", wrong));
        // Every line we roll is on the wiki's table.
        for (RevenantRewards.Line l : RevenantRewards.MAIN) {
            assertTrue(perKill.containsKey(l.item()), l.item());
        }
        for (RevenantRewards.Line l : RevenantRewards.SUPPLIES) {
            assertTrue(perKill.containsKey(l.item()), l.item());
        }
        assertEquals(0, RevenantRewards.medHelmDenominator(105, true), "skulled: no dragon med helm");
    }

    /** Our per-kill chance of an item for the skulled ork off task. */
    private static double ourChance(RevenantRewards.Revenant ork, String item) {
        switch (item) {
            case "revenant_ether":
                return 1;
            case "coins":
                return ork.coinChance();
            case "craws_bow", "viggoras_chainmace", "thammarons_sceptre":
                return 1 / RevenantRewards.uniqueDenominator(ork.level(), true, false) / 5;
            case "amulet_of_avarice":
                return 1 / RevenantRewards.uniqueDenominator(ork.level(), true, false) * 2 / 5;
            case "yew_seed", "magic_seed", "ancient_emblem", "ancient_totem", "ancient_crystal", "ancient_statuette",
                    "ancient_medallion", "ancient_effigy", "ancient_relic":
                return 1 / RevenantRewards.rareDenominator(item, ork.level(), true);
            default:
                return RevenantRewards.lineChance(ork, RevenantRewards.MAIN, item) + RevenantRewards.lineChance(ork, RevenantRewards.SUPPLIES, item);
        }
    }

    private static int[] ourQuantity(RevenantRewards.Revenant ork, String item) {
        if (item.equals("revenant_ether")) {
            return RevenantRewards.ether(ork.level());
        }
        if (item.equals("coins")) {
            return new int[]{1, RevenantRewards.coinsMax(ork.level())};
        }
        if (item.endsWith("_seed")) {
            return new int[]{2, 6};
        }
        int min = Integer.MAX_VALUE;
        int max = 0;
        for (List<RevenantRewards.Line> t : List.of(RevenantRewards.MAIN, RevenantRewards.SUPPLIES)) {
            for (RevenantRewards.Line l : t) {
                if (l.item().equals(item)) {
                    min = Math.min(min, l.min());
                    max = Math.max(max, l.max());
                }
            }
        }
        return min == Integer.MAX_VALUE ? new int[]{1, 1} : new int[]{min, max};
    }

    @Test
    void knightAndImpRareRates() {
        // Wiki (Revenant knight): emblem 1/1,333.33 unskulled, 1/4,400 skulled; totem 1/2,000 and 1/1,100; statuette
        // 1/4,000 and 1/2,200; dragon med helm 1/615.38 unskulled.
        assertEquals(1333.33, RevenantRewards.rareDenominator("ancient_emblem", 126, false), 1);
        assertEquals(4400, RevenantRewards.rareDenominator("ancient_emblem", 126, true), 2);
        assertEquals(2000, RevenantRewards.rareDenominator("ancient_totem", 126, false), 1);
        assertEquals(1100, RevenantRewards.rareDenominator("ancient_totem", 126, true), 1);
        assertEquals(4000, RevenantRewards.rareDenominator("ancient_statuette", 126, false), 2);
        assertEquals(2200, RevenantRewards.rareDenominator("ancient_statuette", 126, true), 1);
        assertEquals(615.38, RevenantRewards.medHelmDenominator(126, false), 0.5);
        // Imp: yew and magic seeds 1/11,000 unskulled; ether 2-6. Dragon: ether 2-24, never coins.
        assertEquals(11000, RevenantRewards.rareDenominator("yew_seed", 7, false), 2);
        assertEquals(2, RevenantRewards.ether(7)[0]);
        assertEquals(6, RevenantRewards.ether(7)[1]);
        assertEquals(24, RevenantRewards.ether(135)[1]);
        assertEquals(0, RevenantRewards.revenant("revenant_dragon").coinChance());
        // A Slayer task boosts only the unique table.
        assertEquals(RevenantRewards.uniqueDenominator(126, false, false) / 5, RevenantRewards.uniqueDenominator(126, false, true), 1e-9);
        assertEquals(RevenantRewards.artefactDenominator("ancient_relic", 126, false, false),
                RevenantRewards.artefactDenominator("ancient_relic", 126, false, true), 1e-9);
    }

    @Test
    void healingAndStyleChoice() {
        // Below half health, off cooldown, with heals left: heal; never again within 15 ticks.
        assertTrue(RevenantRewards.shouldHeal(40, 105, 100, -1, 8));
        assertFalse(RevenantRewards.shouldHeal(60, 105, 100, -1, 8));
        assertFalse(RevenantRewards.shouldHeal(40, 105, 110, 100, 8));
        assertTrue(RevenantRewards.shouldHeal(40, 105, 115, 100, 8));
        assertFalse(RevenantRewards.shouldHeal(40, 105, 115, 100, 0));
        int lo = Integer.MAX_VALUE;
        int hi = 0;
        for (int i = 0; i < 18; i++) {
            final int r = i;
            int n = RevenantRewards.heals(bound -> Math.min(r, bound - 1));
            lo = Math.min(lo, n);
            hi = Math.max(hi, n);
        }
        assertEquals(8, lo);
        assertEquals(25, hi);
        // Magic by default; the lowest defence otherwise; never the prayed style; melee only within reach.
        assertEquals(AttackType.MAGIC, RevenantRewards.chooseStyle(AttackType.SLASH, true, 50, 50, 50, null));
        assertEquals(AttackType.RANGED, RevenantRewards.chooseStyle(AttackType.SLASH, true, 80, 20, 60, null));
        assertEquals(AttackType.SLASH, RevenantRewards.chooseStyle(AttackType.SLASH, true, 10, 20, 60, null));
        assertEquals(AttackType.RANGED, RevenantRewards.chooseStyle(AttackType.SLASH, false, 10, 20, 60, null));
        assertEquals(AttackType.RANGED, RevenantRewards.chooseStyle(AttackType.CRUSH, true, 90, 40, 10, AttackType.MAGIC));
        assertEquals(AttackType.RANGED, RevenantRewards.chooseStyle(AttackType.CRUSH, true, 10, 40, 90, AttackType.STAB));
        assertEquals(AttackType.MAGIC, RevenantRewards.chooseStyle(AttackType.CRUSH, true, 10, 40, 30, AttackType.CRUSH));
    }

    @Test
    void wholeKillRollsTheWikiStructure() {
        RevenantRewards.Revenant dragon = RevenantRewards.revenant("revenant_dragon");
        // A dragon never drops coins: ether, one main line and one supplies line.
        List<RevenantRewards.Rolled> kill = RevenantRewards.rollCommon(dragon, bound -> 0);
        assertEquals(3, kill.size());
        assertEquals("revenant_ether", kill.get(0).item());
        assertEquals(2, kill.get(0).count());
        assertEquals("bracelet_of_ethereum", kill.get(1).item());
        assertEquals("blighted_entangle_sack", kill.get(2).item());
        // An imp nearly always drops coins instead.
        List<RevenantRewards.Rolled> imp = RevenantRewards.rollCommon(RevenantRewards.revenant("revenant_imp"), bound -> 0);
        assertEquals(2, imp.size());
        assertEquals("coins", imp.get(1).item());
        // The unique table hit ends the rare chain.
        List<String> rares = RevenantRewards.roll(135, false, false, bound -> 0);
        assertEquals("amulet_of_avarice", rares.get(0));
        assertFalse(rares.stream().anyMatch(i -> i.startsWith("ancient_")));
    }
}
