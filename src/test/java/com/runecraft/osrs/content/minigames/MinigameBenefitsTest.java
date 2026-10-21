package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.extras.Pets;
import com.runecraft.osrs.content.minigames.MinigameRules.NightmareZone;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Barbarian Assault gambles, Soul Wars spoils of war and Nightmare Zone points/benefits diffed against
 * wiki snapshots under {@code src/test/resources/osrs/minigames/}.
 */
class MinigameBenefitsTest {
    /** Wiki names whose RuneCraft id isn't the plain snake-case name. */
    private static final Map<String, String> ALIAS = Map.of("Rune dagger(p++)", "rune_dagger", "Papaya tree seed", "papaya_seed",
            "Palm tree seed", "palm_seed", "Runite bolts", "rune_bolts", "Blighted super restore(4)", "blighted_super_restore",
            "Clue scroll (elite)", "clue_scroll_elite", "Lil' creator", "lil_creator");

    private static List<String[]> tsv(String name) throws IOException {
        List<String[]> rows = new ArrayList<>();
        Path p = Path.of("src/test/resources/osrs/minigames/" + name);
        try (InputStream in = Files.newInputStream(p);
             BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            for (String line; (line = r.readLine()) != null; ) {
                if (!line.isBlank() && !line.startsWith("#")) {
                    rows.add(line.split("\t"));
                }
            }
        }
        return rows;
    }

    private static String id(String wikiName) {
        return ALIAS.getOrDefault(wikiName, wikiName.toLowerCase().replace("'", "").replace(' ', '_'));
    }

    /** "15-25 (noted)" -> {15, 25, noted}. */
    private static int[] quantity(String q) {
        boolean noted = q.contains("(noted)");
        String[] range = q.replace("(noted)", "").trim().split("-");
        int min = Integer.parseInt(range[0].trim());
        int max = range.length > 1 ? Integer.parseInt(range[1].trim()) : min;
        return new int[]{min, max, noted ? 1 : 0};
    }

    @Test
    void gamblesMatchTheWiki() throws IOException {
        Map<BaGambles.Tier, List<String>> seen = new HashMap<>();
        for (String[] row : tsv("ba_gambles.tsv")) {
            BaGambles.Tier tier = BaGambles.Tier.valueOf(row[0].toUpperCase());
            String item = id(row[1]).equals("feather") ? BaGambles.FEATHER : id(row[1]);
            if (row[3].equals("1/32") && BaGambles.MISSING.contains(item)) {
                // Left out until the item exists; adding the item should put its line back.
                assertNull(ItemCatalog.get(item), item + " exists now: add its line to BaGambles");
                seen.computeIfAbsent(tier, t -> new ArrayList<>());
            } else if (row[3].equals("1/32")) {
                BaGambles.Line line = BaGambles.table(tier).stream().filter(l -> l.item().equals(item)).findFirst()
                        .orElseThrow(() -> new AssertionError(tier + " gamble lacks " + item));
                int[] q = quantity(row[2]);
                assertEquals(q[0], line.min(), tier + " " + item + " min");
                assertEquals(q[1], line.max(), tier + " " + item + " max");
                assertEquals(q[2] == 1, line.noted(), tier + " " + item + " noted");
                seen.computeIfAbsent(tier, t -> new ArrayList<>()).add(item);
            } else if (row[3].equals("1/16000")) {
                assertTrue(BaGambles.DRAGON.contains(item), item);
            } else if (item.equals("clue_scroll_elite")) {
                assertEquals("1/" + BaGambles.ELITE_CLUE_CHANCE, row[3]);
            } else {
                assertEquals("pet_penance_queen", item);
                assertEquals("1/" + BaGambles.PET_CHANCE, row[3]);
            }
        }
        for (BaGambles.Tier tier : BaGambles.Tier.values()) {
            assertEquals(seen.get(tier).size(), BaGambles.table(tier).size(), tier + " has lines the wiki lacks");
            for (BaGambles.Line l : BaGambles.table(tier)) {
                assertTrue(ItemCatalog.exists(l.item()), "unknown gamble item " + l.item());
            }
        }
        for (String d : BaGambles.DRAGON) {
            assertNotNull(ItemCatalog.get(d), d);
        }
        // Costs and locks (OSRS Wiki: 200 / 400 / 500 + a queen kill; wave 3 / 6 / any).
        assertEquals(200, BaGambles.Tier.LOW.cost());
        assertEquals(400, BaGambles.Tier.MEDIUM.cost());
        assertEquals(500, BaGambles.Tier.HIGH.cost());
        assertNotNull(BaGambles.lock(BaGambles.Tier.LOW, 2));
        assertNull(BaGambles.lock(BaGambles.Tier.LOW, 3));
        assertNotNull(BaGambles.lock(BaGambles.Tier.MEDIUM, 5));
        assertNull(BaGambles.lock(BaGambles.Tier.HIGH, 0));
        RewardShop ba = RewardShops.get("barbarian_assault");
        long gambles = ba.entries().stream().filter(e -> e.item().startsWith(BaGambles.ITEM_PREFIX)).count();
        assertEquals(12, gambles, "three tiers payable from each of the four roles");
        RewardShop.Entry high = ba.entries().stream().filter(e -> e.item().equals(BaGambles.Tier.HIGH.id())).findFirst().orElseThrow();
        assertTrue(high.costs().stream().anyMatch(c -> c.currency().equals(MinigameCurrencies.BA_QUEEN) && c.amount() == 1));
        assertTrue(high.costs().stream().anyMatch(c -> c.amount() == 500));
        // Rolling never hands out an item RuneCraft lacks.
        Random rng = new Random(7);
        int pets = 0;
        for (int i = 0; i < 20_000; i++) {
            for (BaGambles.Prize p : BaGambles.roll(BaGambles.Tier.HIGH, rng::nextDouble, it -> ItemCatalog.get(it) != null)) {
                assertNotNull(ItemCatalog.get(p.item()), p.item());
                assertTrue(p.amount() > 0);
                pets += p.item().equals("pet_penance_queen") ? 1 : 0;
            }
        }
        assertTrue(pets > 5 && pets < 50, "pet rate about 1/1000: " + pets);
        assertTrue(Pets.get("pet_penance_queen").note().contains("gamble"));
    }

    @Test
    void spoilsOfWarMatchTheWiki() throws IOException {
        Set<String> items = new HashSet<>();
        for (String[] row : tsv("spoils_of_war.tsv")) {
            String item = id(row[0]);
            assertNotNull(ItemCatalog.get(item), item);
            if (row[2].equals("1/9")) {
                SpoilsOfWar.Line line = SpoilsOfWar.TABLE.stream().filter(l -> l.item().equals(item)).findFirst()
                        .orElseThrow(() -> new AssertionError("spoils lack " + item));
                int[] q = quantity(row[1]);
                assertEquals(q[0], line.min(), item);
                assertEquals(q[1], line.max(), item);
                assertEquals(q[2] == 1, line.noted(), item);
                items.add(item);
            } else {
                assertEquals(SpoilsOfWar.PET, item);
                assertEquals("1/" + SpoilsOfWar.PET_CHANCE, row[2]);
            }
        }
        assertEquals(items.size(), SpoilsOfWar.TABLE.size());
        Random rng = new Random(3);
        for (int i = 0; i < 1000; i++) {
            List<SpoilsOfWar.Prize> prizes = SpoilsOfWar.open(rng::nextDouble);
            assertTrue(prizes.size() == 3 || prizes.size() == 4);
        }
        assertNotNull(Pets.get(SpoilsOfWar.PET));
        // Nomad's shop (OSRS Wiki): spoils 30, ectoplasmator 250 (40 Prayer), blighted packs 10 each.
        RewardShop sw = RewardShops.get("soul_wars");
        assertEquals(30, cost(sw, SpoilsOfWar.ITEM));
        assertEquals(250, cost(sw, "ectoplasmator"));
        assertEquals(40, entry(sw, "ectoplasmator").requiredLevel());
        assertEquals(70, entry(sw, "blighted_entangle_sack").count());
        assertEquals(4, entry(sw, "blighted_super_restore").count());
        assertEquals(10, cost(sw, "blighted_karambwan"));
    }

    private static RewardShop.Entry entry(RewardShop shop, String item) {
        return shop.entries().stream().filter(e -> e.item().equals(item)).findFirst()
                .orElseThrow(() -> new AssertionError(shop.id() + " lacks " + item));
    }

    private static int cost(RewardShop shop, String item) {
        return entry(shop, item).costs().get(0).amount();
    }

    @Test
    void nightmareZoneHardBossesHitHarder() throws IOException {
        List<String[]> rows = tsv("nmz_hard_stats.tsv");
        assertEquals(12, rows.size());
        for (String[] row : rows) {
            NightmareZone.HardStats h = NightmareZone.hardStats(row[0]);
            assertNotNull(h, row[0]);
            assertEquals(Integer.parseInt(row[1]), h.attack(), row[0] + " attack");
            assertEquals(Integer.parseInt(row[2]), h.hardAttack(), row[0] + " hard attack");
            assertEquals(Integer.parseInt(row[5]), h.maxHit(), row[0] + " max hit");
            assertEquals(Integer.parseInt(row[6]), h.hardMaxHit(), row[0] + " hard max hit");
            assertEquals(Integer.parseInt(row[1]), ContentRegistry.monster(row[0]).attack, row[0] + " normal attack level");
            assertEquals(Integer.parseInt(row[5]), ContentRegistry.monster(row[0]).attacks.get(0).maxHit(), row[0] + " normal max hit");
            assertTrue(NightmareZone.isBoss(row[0]));
            assertNotNull(ContentRegistry.quest(NightmareZone.BOSS_QUEST.get(row[0])), row[0] + " quest");
        }
        // Count Draynor: attack 30 -> 198, so his attack roll grows by (198 + 9) / (30 + 9); max hit 3 -> 17.
        assertEquals(207 / 39.0, NightmareZone.hardAccuracy("nmz_count_draynor", true, false), 1e-9);
        assertEquals(14, NightmareZone.hardMaxHitBonus("nmz_count_draynor"));
        assertEquals(1.0, NightmareZone.hardAccuracy("chicken", true, false), 1e-9);
        assertEquals(149 / 79.0, NightmareZone.hardAccuracy("nmz_arrg", false, true), 1e-9);
        // Zapper: only Count Draynor is protected from its killing blow, until the player has hit him.
        assertTrue(NightmareZone.zapperMayKill("nmz_glod", false));
        assertTrue(!NightmareZone.zapperMayKill("nmz_count_draynor", false));
        assertTrue(NightmareZone.zapperMayKill("nmz_count_draynor", true));
        // Modes and their fees (wiki "Cost per dream").
        assertEquals(0, NightmareZone.Mode.PRACTICE.cost(true));
        assertEquals(1000, NightmareZone.Mode.ENDURANCE.cost(false));
        assertEquals(5000, NightmareZone.Mode.ENDURANCE.cost(true));
        assertEquals(2000, NightmareZone.Mode.RUMBLE.cost(false));
        assertEquals(26000, NightmareZone.Mode.CUSTOM_RUMBLE.cost(true));
        NightmareZone.Dream d = new NightmareZone.Dream(NightmareZone.Mode.PRACTICE, true, "nmz_elvarg");
        assertEquals(d, NightmareZone.Dream.parse(d.variant()));
        assertEquals(new NightmareZone.Dream(NightmareZone.Mode.RUMBLE, true, null), NightmareZone.Dream.parse("nightmare_zone_hard_entrance"));
        assertEquals(new NightmareZone.Dream(NightmareZone.Mode.RUMBLE, false, null), NightmareZone.Dream.parse("nightmare_zone_entrance"));
        // Unlocks and point scaling: all fifty bosses pay the wiki maximum, twenty-five pay half.
        assertEquals(List.of("nmz_count_draynor", "nmz_elvarg"),
                NightmareZone.unlocked(q -> q.equals("VAMPYRE_SLAYER") || q.equals("dragon_slayer_i")));
        // No boss quest done: no bosses (not every boss at full points), so the dream is refused.
        assertTrue(NightmareZone.unlocked(q -> false).isEmpty());
        assertEquals(NightmareZone.BOSSES.length, NightmareZone.allBosses().size());
        assertEquals(NightmareZone.points("nmz_glod", true), NightmareZone.points("nmz_glod", true, NightmareZone.BOSSES.length));
        assertEquals(50, NightmareZone.BOSSES.length);
        assertEquals(17493 / 2, NightmareZone.points("nmz_glod", true, 25));
    }

    @Test
    void nightmareZoneMatchesTheWiki() throws IOException {
        Map<String, String[]> wiki = new HashMap<>();
        for (String[] row : tsv("nmz_bosses.tsv")) {
            wiki.put(row[0].toLowerCase(), row);
        }
        for (Object[] b : NightmareZone.BOSSES) {
            String id = (String) b[0];
            String name = ContentRegistry.monster(id).name.toLowerCase();
            String[] row = wiki.get(name);
            assertNotNull(row, "no wiki row for " + name);
            assertEquals(Integer.parseInt(row[1]), NightmareZone.points(id, false), id + " normal points");
            assertEquals(Integer.parseInt(row[2]), NightmareZone.points(id, true), id + " hard points");
            assertEquals(Integer.parseInt(row[4]), NightmareZone.hardHitpoints(id), id + " hard hitpoints");
        }
        // Benefits: sold by the dose into the potion barrels.
        RewardShop nmz = RewardShops.get("nightmare_zone");
        assertEquals(1500, cost(nmz, NightmareZone.OVERLOAD));
        assertEquals(1000, cost(nmz, NightmareZone.ABSORPTION));
        assertEquals(250, cost(nmz, NightmareZone.SUPER_RANGING));
        assertEquals(250, cost(nmz, NightmareZone.SUPER_MAGIC));
        assertEquals(255, NightmareZone.barrelRoom(0, 1000));
        assertEquals(5, NightmareZone.barrelRoom(250, 10));
        assertEquals(0, NightmareZone.barrelRoom(255, 1));
        assertEquals(9500, cost(nmz, "herb_box"));
        assertEquals(15, RewardShops.dailyLimit("nightmare_zone", "herb_box")); // wiki: 15 a day from the NMZ
        assertEquals(0, RewardShops.dailyLimit("tithe_farm", "herb_box")); // no limit at Tithe Farm
        // Scroll of redirection: nine towns, each tablet a real teleport tablet.
        assertEquals(9, MinigameRules.NightmareZone.REDIRECTS.size());
        for (MinigameRules.NightmareZone.Redirect r : MinigameRules.NightmareZone.REDIRECTS) {
            assertNotNull(com.runecraft.osrs.content.systems.Spellbooks.tablet(r.tablet()), r.tablet());
        }
        assertEquals(50, MinigameRules.NightmareZone.redirect("yanille").construction());
        assertEquals("song_of_the_elves", MinigameRules.NightmareZone.redirect("Prifddinas").quest());
        assertEquals(775, cost(nmz, "scroll_of_redirection"));
        for (String potion : NightmareZone.POTIONS) {
            assertNotNull(ItemCatalog.get(potion), potion);
        }
        // Boosts: floor(level * 15%) + 5; 99 -> 19 (118), 1 -> 5.
        assertEquals(19, NightmareZone.boost(99));
        assertEquals(5, NightmareZone.boost(1));
        assertEquals(16, NightmareZone.boost(75));
        assertEquals(50, NightmareZone.absorb(0));
        assertEquals(1000, NightmareZone.absorb(990));
        assertEquals(26, NightmareZone.recurrent(35));
        assertEquals(75, NightmareZone.PowerUp.RECURRENT_DAMAGE.ticks());
        assertEquals(100, NightmareZone.PowerUp.ZAPPER.ticks());
        assertEquals(75, NightmareZone.PowerUp.POWER_SURGE.ticks());
    }
}
