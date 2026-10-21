package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.dungeons.Dungeons;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.osrs.data.Untradeables;
import com.runecraft.osrs.world.StructureDef;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Slayer system walked against the OSRS Wiki (round 35): master requirements, boss tasks (who assigns which boss,
 * their requirements and sizes), the equipment shop, the slayer helmet recolours, block slots, protective gear slots, the
 * Karuulm hot floor and the task-only dungeons.
 */
class SlayerSystemTest {
    @Test
    void mastersNeedTheirQuests() {
        // OSRS Wiki "Slayer Master": Mazchna needs Priest in Peril, Chaeldar Lost City, Duradel Shilo Village.
        assertEquals("priest_in_peril", SlayerMasters.get("mazchna").quest());
        assertEquals("lost_city", SlayerMasters.get("chaeldar").quest());
        assertEquals("shilo_village", SlayerMasters.get("duradel").quest());
        assertEquals("a_porcine_of_interest", SlayerMasters.get("spria").quest());
        assertNull(SlayerMasters.get("turael").quest());
        assertNull(SlayerMasters.get("vannaka").quest());
        assertNull(SlayerMasters.get("konar").quest());
        assertNull(SlayerMasters.get("krystilia").quest());
        for (SlayerMasters.Master m : SlayerMasters.all()) {
            assertTrue(m.quest() == null || ContentRegistry.quest(m.quest()) != null, m.id());
        }
        // Without Shilo Village the station falls back to Nieve; without Priest in Peril, Turael stays.
        assertEquals("nieve", SlayerMasters.bestFor(126, 99, m -> !"shilo_village".equals(m.quest())).id());
        assertEquals("turael", SlayerMasters.bestFor(30, 1, m -> !"priest_in_peril".equals(m.quest())).id());
        assertEquals("mazchna", SlayerMasters.bestFor(30, 1, m -> true).id());
    }

    /** The masters stand where the OSRS Wiki's maps put them (within 2 tiles); Vannaka and Konar are known exceptions. */
    @Test
    void mastersStandWhereTheWikiPutsThem() {
        java.util.Map<String, int[]> wiki = java.util.Map.of("turael", new int[]{2931, 3536}, "mazchna", new int[]{3511, 3509},
                "chaeldar", new int[]{2444, 4431}, "nieve", new int[]{2432, 3423}, "steve", new int[]{2432, 3423},
                "duradel", new int[]{2869, 2982}, "krystilia", new int[]{3109, 3516});
        for (com.runecraft.osrs.content.NpcDef npc : ContentRegistry.npcs()) {
            int[] want = wiki.get(npc.id());
            if (want == null) {
                continue;
            }
            int px;
            int py;
            com.runecraft.osrs.content.PlaceDef place = ContentRegistry.places().stream().filter(x -> x.id().equals(npc.place()))
                    .findFirst().orElse(null);
            if (place != null) {
                px = place.osrsX();
                py = place.osrsY();
            } else {
                com.runecraft.osrs.Landmark l = com.runecraft.osrs.Landmark.valueOf(npc.place());
                px = l.osrsX();
                py = l.osrsY();
            }
            double x = px + npc.dx() / 2.0;
            double y = py - npc.dz() / 2.0;
            assertTrue(Math.abs(x - want[0]) <= 2 && Math.abs(y - want[1]) <= 2,
                    npc.id() + " stands at " + x + ", " + y + "; wiki " + want[0] + ", " + want[1]);
        }
    }

    @Test
    void bossTasksFollowTheWikiTable() {
        SlayerBossTasks.BossTask barrows = SlayerBossTasks.get("boss_barrows");
        assertNotNull(barrows);
        assertEquals(List.of("ahrim", "dharok", "guthan", "karil", "torag", "verac"), barrows.creatures());
        assertEquals(36, barrows.maxKills(0));
        assertEquals(72, barrows.maxKills(6)); // grandmaster: 36 + 6 x 6
        assertEquals(35, SlayerBossTasks.get("boss_zulrah").maxKills(0));
        assertEquals(65, SlayerBossTasks.get("boss_zulrah").maxKills(6));
        assertEquals(3, SlayerBossTasks.clampKills(barrows, 1, 0));
        assertEquals(36, SlayerBossTasks.clampKills(barrows, 99, 0));
        assertEquals(74, SlayerBossTasks.get("boss_maggot_king").slayerLevel());
        assertEquals(51, SlayerBossTasks.get("boss_shellbane_gryphon").slayerLevel());
        assertEquals(18, SlayerBossTasks.get("boss_vorkath").slayerLevel());
        assertEquals(List.of("dragon_slayer_ii"), SlayerBossTasks.get("boss_vorkath").quests());
        assertEquals(Skill.AGILITY, SlayerBossTasks.get("boss_commander_zilyana").skill());
        assertEquals(70, SlayerBossTasks.get("boss_kreearra").skillLevel());
        for (SlayerBossTasks.BossTask t : SlayerBossTasks.all()) {
            assertTrue(SlayerBossTasks.exists(t), t.id());
            assertNotNull(SlayerCategories.get(t.id()), t.id());
            for (String q : t.quests()) {
                assertNotNull(ContentRegistry.quest(q), t.id() + " quest " + q);
            }
        }
        // Only Konar assigns the Alchemical Hydra, and her Crazy archaeologist task doesn't take the Deranged one.
        assertTrue(SlayerBossTasks.choices(99, false, List.of(), null, "duradel", t -> true).stream()
                .noneMatch(t -> t.id().equals("boss_alchemical_hydra")));
        assertTrue(SlayerBossTasks.choices(99, false, List.of(), null, "konar", t -> true).stream()
                .anyMatch(t -> t.id().equals("boss_alchemical_hydra")));
        assertTrue(SlayerBossTasks.counts("boss_crazy_archaeologist", "deranged_archaeologist", "duradel"));
        assertFalse(SlayerBossTasks.counts("boss_crazy_archaeologist", "deranged_archaeologist", "konar"));
        assertTrue(SlayerBossTasks.counts("boss_crazy_archaeologist", "crazy_archaeologist", "konar"));
        // A boss the player can't reach (quests, skills) is never picked; Krystilia never assigns the Barrows.
        Random r = new Random(5);
        for (int i = 0; i < 200; i++) {
            SlayerBossTasks.BossTask t = SlayerBossTasks.pick(r, 99, false, List.of(), null, "nieve", b -> b.quests().isEmpty());
            assertTrue(t.quests().isEmpty(), t.id());
            assertFalse(SlayerBossTasks.pick(r, 99, true, List.of(), null, "krystilia", b -> true).id().equals("boss_barrows"));
        }
        // The "bosses" line needs at least one boss whose quests are done.
        SlayerMasters.Assignment line = SlayerMasters.get("nieve").assignment(SlayerBossTasks.CATEGORY);
        assertTrue(SlayerTasks.eligible(line, 99, 126, Set.of("like_a_boss"), List.of(), null, q -> true));
        assertEquals(5000, SlayerBossTasks.COMPLETION_XP, 1e-9);
    }

    @Test
    void equipmentShopIsTheWikis() {
        for (SlayerRewards.ShopItem s : SlayerRewards.SHOP) {
            assertEquals(1, s.slayerLevel(), s.item() + ": nothing in the shop needs a Slayer level to buy");
        }
        assertEquals(200, SlayerRewards.shopItem("boots_of_stone").price());
        assertEquals(55, SlayerRewards.shopItem("broad_arrowheads").price());
        assertEquals(55, SlayerRewards.shopItem("unfinished_broad_bolts").price());
        assertNull(SlayerRewards.shopItem("leaf_bladed_sword"), "the leaf-bladed sword is a kurask/turoth drop, not sold");
        assertEquals(10, SlayerRewards.pointBuy("looting_bag").cost());
        assertEquals(200, ItemCatalog.get("boots_of_stone").value());
    }

    @Test
    void helmetRecolours() {
        assertEquals(14, SlayerRewards.slayerHelmets(false).size());
        assertEquals(14, SlayerRewards.slayerHelmets(true).size());
        for (String id : SlayerRewards.slayerHelmets(false)) {
            assertNotNull(ItemCatalog.get(id), id);
            assertNotNull(ItemCatalog.get(id + "_i"), id + "_i");
            assertTrue(SlayerGear.SLAYER_HELMETS.contains(id) && SlayerGear.SLAYER_HELMETS.contains(id + "_i"), id);
            assertTrue(SlayerGear.MELEE_BOOST_HEADGEAR.contains(id), id);
            assertTrue(SlayerGear.IMBUED_HEADGEAR.contains(id + "_i"), id);
            assertFalse(SlayerGear.IMBUED_HEADGEAR.contains(id), id);
            assertTrue(Untradeables.untradeable(id) && Untradeables.untradeable(id + "_i"), id);
        }
        assertEquals("Black slayer helmet (i)", ItemCatalog.get("black_slayer_helmet_i").name());
        SlayerRewards.Craft black = SlayerRewards.craft("black_slayer_helmet");
        assertEquals("king_black_bonnet", black.unlock());
        assertEquals(SlayerRewards.Kind.COSMETIC, SlayerRewards.unlock("king_black_bonnet").kind());
        assertEquals(1000, SlayerRewards.unlock("king_black_bonnet").cost());
        assertTrue(black.inputs().stream().anyMatch(i -> i.item().equals("kbd_heads")));
        assertEquals("GRANDMASTER", SlayerRewards.craft("tzkal_slayer_helmet").caTier());
        assertEquals("ELITE", SlayerRewards.craft("tztok_slayer_helmet_i").caTier());
        assertEquals(List.of("slayer_helmet_i"), SlayerRewards.craft("vampyric_slayer_helmet_i").inputs().stream()
                .map(SlayerRewards.Input::item).toList());
    }

    @Test
    void recolouredHelmetsWorkLikeTheHelmetElsewhere() {
        for (String id : SlayerRewards.slayerHelmets(false)) {
            // Nightmare Zone and Soul Wars imbue every recolour for the plain helmet's 1,250,000 points / 500 zeal (OSRS Wiki).
            Object[] row = null;
            for (Object[] r : com.runecraft.osrs.content.minigames.MinigameRules.Imbues.ITEMS) {
                if (r[0].equals(id)) {
                    row = r;
                }
            }
            assertNotNull(row, id + " can't be imbued");
            assertEquals(id + "_i", row[1]);
            assertEquals(1_250_000, row[2], id);
            assertEquals(500, row[3], id);
            // Any slayer helmet filters the volcanic sulphur fumes.
            assertTrue(com.runecraft.osrs.content.skilling.MiningData.filtersFumes(id), id);
            assertTrue(com.runecraft.osrs.content.skilling.MiningData.filtersFumes(id + "_i"), id + "_i");
        }
        assertFalse(com.runecraft.osrs.content.skilling.MiningData.filtersFumes("black_mask"));
        assertFalse(com.runecraft.osrs.content.skilling.MiningData.filtersFumes(""));
    }

    @Test
    void blockSlotsComeFromQuestPoints() {
        assertEquals(0, SlayerRewards.blockSlots(49, false));
        assertEquals(1, SlayerRewards.blockSlots(50, false));
        assertEquals(6, SlayerRewards.blockSlots(300, false));
        assertEquals(6, SlayerRewards.blockSlots(330, false));
        assertEquals(7, SlayerRewards.blockSlots(330, true));
        assertEquals(SlayerRewards.MAX_BLOCKS, SlayerRewards.blockSlots(Integer.MAX_VALUE, true));
    }

    @Test
    void protectiveGearIsWornInItsOsrsSlot() {
        assertEquals(SlayerGear.Where.NECK, SlayerGear.protection("cave_horrors").where());
        assertEquals(SlayerGear.Where.HANDS, SlayerGear.protection("fever_spiders").where());
        assertEquals(Jewellery.Slot.NECK, Jewellery.worn("witchwood_icon").slot());
        assertEquals(Jewellery.Slot.HANDS, Jewellery.worn("slayer_gloves").slot());
        assertEquals(SlayerGear.Where.HEAD, SlayerGear.protection("banshees").where());
        assertEquals(SlayerGear.Where.SHIELD, SlayerGear.protection("basilisks").where());
        assertEquals(SlayerGear.Where.FEET, SlayerGear.protection("killerwatts").where());
        // Turoth and kurask: broad arrows and broad bolts both work.
        assertTrue(SlayerGear.BROAD_AMMO.containsAll(List.of("broad_arrows", "broad_bolts", "amethyst_broad_bolts")));
        for (String drake : List.of("drakes", "wyrms", "hydras")) {
            assertNull(SlayerGear.protection(drake), drake + ": the Karuulm floor burns by location, not by task");
        }
    }

    @Test
    void karuulmFloorBurnsBeyondTheEntrance() {
        Dungeons.Camp wyrms = camp("dng_karuulm_slayer_dungeon", "wyrm");
        Dungeons.Camp hydras = camp("dng_karuulm_slayer_dungeon", "hydra");
        assertTrue(SlayerGear.hotFloorAt(wyrms.centreX(), wyrms.feetY(), wyrms.centreZ()));
        assertTrue(SlayerGear.hotFloorAt(hydras.centreX(), hydras.feetY(), hydras.centreZ()));
        // The entrance chamber is safe, and so is the rest of the world.
        int ex = StructureDef.bx(SlayerGear.KARUULM_ENTRANCE_X);
        int ez = StructureDef.bz(SlayerGear.KARUULM_ENTRANCE_Y);
        assertFalse(SlayerGear.hotFloorAt(ex, wyrms.feetY(), ez));
        Dungeons.Camp kalphite = camp("dng_kalphite_cave", null);
        assertFalse(SlayerGear.hotFloorAt(kalphite.centreX(), kalphite.feetY(), kalphite.centreZ()));
        assertFalse(SlayerGear.hotFloorAt(wyrms.centreX(), 70, wyrms.centreZ()));
        assertEquals(4, SlayerGear.hotFloorDamage("dragon_boots", false));
        for (String boots : List.of("boots_of_stone", "boots_of_brimstone", "granite_boots")) {
            assertEquals(0, SlayerGear.hotFloorDamage(boots, false), boots);
            assertTrue(ItemCatalog.exists(boots), boots);
        }
        assertEquals(0, SlayerGear.hotFloorDamage("dragon_boots", true), "the elite Kourend & Kebos Diary protects");
    }

    @Test
    void taskOnlyDungeons() {
        for (String[] d : new String[][]{{"dng_kalphite_cave", null}, {"dng_kraken_cove", "cave_kraken"},
                {"dng_smoke_devil_dungeon", "smoke_devil"}, {"dng_lizardman_caves", "lizardman_shaman"},
                {"dng_slayer_tower_basement", null}}) {
            Dungeons.Camp c = camp(d[0], d[1]);
            assertNotNull(Dungeons.taskOnly(c.centreX(), c.feetY(), c.centreZ()), d[0] + " camp is not task-only");
        }
        Dungeons.Camp wyrms = camp("dng_karuulm_slayer_dungeon", "wyrm");
        assertNull(Dungeons.taskOnly(wyrms.centreX(), wyrms.feetY(), wyrms.centreZ()));
    }

    /** The OSRS Wiki's brimstone and Larran's big chest tables (snapshot slayer_chests.txt) against SlayerChests. */
    @Test
    void slayerChestsMatchTheWiki() throws java.io.IOException {
        java.util.Map<String, String> alias = java.util.Map.of("palm tree seed", "palm_seed", "dragonfruit tree seed",
                "dragonfruit_seed");
        List<String> bad = new java.util.ArrayList<>();
        int mainRows = 0;
        for (String row : java.nio.file.Files.readAllLines(java.nio.file.Path.of("src/test/resources/osrs/slayer/slayer_chests.txt"))) {
            if (row.isBlank() || row.startsWith("#")) {
                continue;
            }
            String[] f = row.split("\\|");
            boolean larran = f[0].equals("larran");
            String name = f[2].toLowerCase(java.util.Locale.ROOT);
            String id = alias.containsKey(name) ? alias.get(name) : idOf(name);
            String[] q = f[3].split("-");
            int lo = Integer.parseInt(q[0]);
            int hi = Integer.parseInt(q[q.length - 1]);
            switch (f[1]) {
                case "pre", "main" -> {
                    List<com.runecraft.osrs.data.Drop> table = f[1].equals("pre")
                            ? (larran ? SlayerChests.LARRAN_RARE : SlayerChests.BRIMSTONE_RARE)
                            : (larran ? SlayerChests.LARRAN_MAIN : SlayerChests.BRIMSTONE_MAIN);
                    int want = Integer.parseInt(f[4].split("/")[f[1].equals("pre") ? 1 : 0]);
                    com.runecraft.osrs.data.Drop d = table.stream().filter(x -> x.item().equals(id)).findFirst().orElse(null);
                    if (d == null) {
                        bad.add(row + ": missing (" + id + ")");
                    } else if (d.min() != lo || d.max() != hi || d.rarity() != want) {
                        bad.add(row + ": RuneCraft " + d);
                    }
                    if (f[1].equals("main")) {
                        mainRows++;
                    }
                }
                case "fish" -> {
                    // Brimstone quantities; Larran's chest 1.5x, rounded down (shark lures don't exist: raw sharks instead).
                    if (!name.equals("shark lure") && !larran) {
                        assertNotNull(ItemCatalog.get(id), id);
                    }
                }
                default -> bad.add("unknown table " + row);
            }
        }
        assertTrue(bad.isEmpty(), String.join("\n", bad));
        // Every main line is the wiki's, plus the 3/60 fish slot; 60 in all.
        assertEquals(mainRows + 2, SlayerChests.BRIMSTONE_MAIN.size() + SlayerChests.LARRAN_MAIN.size());
        for (List<com.runecraft.osrs.data.Drop> main : List.of(SlayerChests.BRIMSTONE_MAIN, SlayerChests.LARRAN_MAIN)) {
            assertEquals(SlayerChests.MAIN_TOTAL, main.stream().mapToInt(com.runecraft.osrs.data.Drop::rarity).sum());
            assertTrue(main.stream().anyMatch(d -> d.item().equals(SlayerChests.FISH) && d.rarity() == 3));
        }
        // The brimstone pre-roll is 1/100 in all.
        assertEquals(0.01, SlayerChests.BRIMSTONE_RARE.stream().mapToDouble(d -> 1.0 / d.rarity()).sum(), 1e-12);
        // Fish: the skilling curve (Module:Skilling success chart) and the chests' quantities.
        assertEquals(21 / 256.0, SlayerChests.interp(-10, 20, 99), 1e-12);
        assertEquals(161 / 256.0, SlayerChests.interp(160, 0, 1), 1e-12);
        Random r = new Random(11);
        for (int i = 0; i < 2000; i++) {
            SlayerChests.Fish b = SlayerChests.fish(1 + r.nextInt(99), false, r);
            SlayerChests.Fish l = SlayerChests.fish(1 + r.nextInt(99), true, r);
            assertTrue(ItemCatalog.exists(b.item()) && b.count() >= 80 && b.count() <= 350, b.toString());
            assertTrue(ItemCatalog.exists(l.item()) && l.count() >= 120 && l.count() <= 525, l.toString());
        }
        // Level 1 never lands a manta ray roll often; level 99 rarely ends on lobsters.
        int lobsters = 0;
        for (int i = 0; i < 4000; i++) {
            if (SlayerChests.fish(99, false, r).item().equals("raw_lobster")) {
                lobsters++;
            }
        }
        assertTrue(lobsters < 400, "lobsters at 99 Fishing: " + lobsters);
    }

    private static String idOf(String lowerName) {
        for (com.runecraft.osrs.ItemDef d : ItemCatalog.all()) {
            if (d.name().toLowerCase(java.util.Locale.ROOT).equals(lowerName)) {
                return d.id();
            }
        }
        return lowerName.replaceAll("[^a-z0-9]+", "_");
    }

    private static Dungeons.Camp camp(String dungeon, String creature) {
        for (Dungeons.Camp c : Dungeons.camps()) {
            if (c.dungeon().equals(dungeon) && (creature == null || c.creatures().contains(creature))) {
                return c;
            }
        }
        throw new AssertionError("no " + creature + " camp in " + dungeon);
    }
}
