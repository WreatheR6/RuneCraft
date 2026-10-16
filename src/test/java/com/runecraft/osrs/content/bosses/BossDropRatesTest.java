package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.CollectionLogData;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.extras.Pets;
import com.runecraft.osrs.data.Boss;
import com.runecraft.osrs.data.Drop;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** OSRS drop rates, tertiary clue rates and mechanics data for the major bosses (per-kill 1/x). */
class BossDropRatesTest {
    private static int rate(List<Drop> drops, String item) {
        return drops.stream().filter(d -> d.item().equals(item)).mapToInt(Drop::rarity).findFirst()
                .orElseThrow(() -> new AssertionError("no drop " + item));
    }

    private static int rate(String boss, String item) {
        BossDef b = ContentRegistry.boss(boss);
        return rate(b.drops, item);
    }

    @Test
    void tertiaryEliteClueRates() {
        assertEquals(500, rate("giant_mole", "clue_scroll_elite"));
        assertEquals(100, rate("kalphite_queen", "clue_scroll_elite"));
        assertEquals(750, rate("dagannoth_rex", "clue_scroll_elite"));
        assertEquals(60, rate("sarachnis", "clue_scroll_elite"));
        assertEquals(200, rate("corporeal_beast", "clue_scroll_elite"));
        assertEquals(5, rate("skotizo", "clue_scroll_elite"));
        assertEquals(75, rate("zulrah", "clue_scroll_elite"));
        assertEquals(65, rate("vorkath", "clue_scroll_elite"));
        assertEquals(500, rate("kraken", "clue_scroll_elite"));
        assertEquals(500, rate("thermonuclear_smoke_devil", "clue_scroll_elite"));
        assertEquals(100, rate("cerberus", "clue_scroll_elite"));
        assertEquals(256, rate("alchemical_hydra", "clue_scroll_elite"));
        assertEquals(450, rate("king_black_dragon", "clue_scroll_elite"));
    }

    @Test
    void clueScrollsNeverFillACollectionLogPage() {
        for (CollectionLogData.Page p : CollectionLogData.pages()) {
            if (p.category().equals("Bosses") || p.category().equals("Wilderness") || p.category().equals("God Wars Dungeon")) {
                assertFalse(p.items().stream().anyMatch(i -> i.startsWith("clue_scroll")), p.name());
            }
        }
        // ... while the real uniques still do.
        assertTrue(CollectionLogData.pages().stream().anyMatch(p -> p.name().equals("Vorkath")
                && p.items().containsAll(List.of("vorkaths_head", "skeletal_visage", "draconic_visage", "jar_of_decay", "pet_vorki"))));
    }

    @Test
    void uniqueRates() {
        assertEquals(400, rate("kalphite_queen", "dragon_pickaxe"));
        assertEquals(128, rate("kalphite_queen", "kq_head"));
        assertEquals(5000, rate("vorkath", "draconic_visage"));
        assertEquals(5000, rate("king_black_dragon", "draconic_visage"));
        assertEquals(64, rate("corporeal_beast", "spirit_shield"));
        assertEquals(4095, rate("corporeal_beast", "elysian_sigil"));
        // Grotesque Guardians roll their unique table twice per kill (wiki: 2 x 1/250 maul, 2 x 1/500 ring...).
        assertEquals(125, rate("dusk", "granite_maul"));
        assertEquals(250, rate("dusk", "granite_gloves"));
        assertEquals(250, rate("dusk", "granite_ring"));
        assertEquals(375, rate("dusk", "granite_hammer"));
        assertEquals(500, rate("dusk", "black_tourmaline_core"));
        assertEquals(181, rate("alchemical_hydra", "hydras_eye"));
        assertEquals(25, rate("skotizo", "dark_claw"));
    }

    @Test
    void godWarsGeneralRates() {
        for (String piece : new String[]{"bandos_chestplate", "bandos_tassets", "bandos_boots"}) {
            assertEquals(381, rate(Boss.GENERAL_GRAARDOR.drops(), piece));
        }
        assertEquals(127, rate(Boss.KRIL_TSUTSAROTH.drops(), "zamorakian_spear"));
        assertEquals(127, rate(Boss.KRIL_TSUTSAROTH.drops(), "steam_battlestaff"));
        assertEquals(508, rate(Boss.KRIL_TSUTSAROTH.drops(), "staff_of_the_dead"));
        assertEquals(381, rate("kreearra", "armadyl_helmet"));
        assertEquals(127, rate("commander_zilyana", "saradomin_sword"));
        assertEquals(258, rate("nex", "torva_full_helm"));
        // Each general fights alongside three bodyguards that exist as monsters.
        for (Boss general : new Boss[]{Boss.GENERAL_GRAARDOR, Boss.KRIL_TSUTSAROTH}) {
            assertEquals(3, general.bodyguards().size());
            for (String guard : general.bodyguards()) {
                assertTrue(ContentRegistry.monster(guard) != null, guard);
            }
        }
    }

    @Test
    void legacyBossStats() {
        assertEquals(255, Boss.GENERAL_GRAARDOR.hitpoints());
        assertEquals(60, Boss.GENERAL_GRAARDOR.maxHit());
        assertEquals(250, Boss.TZTOK_JAD.hitpoints());
        assertEquals(97, Boss.TZTOK_JAD.maxHit());
        assertEquals(8, Boss.TZTOK_JAD.attackSpeed());
        assertTrue(Boss.TZTOK_JAD.meleeWhenAdjacent());
        assertEquals(29, Boss.DHAROK.maxHit());
    }

    @Test
    void hydraVentsBeatTheCarapace() {
        assertEquals("red", SlayerBosses.HydraPhase.GREEN.vent);
        assertEquals("green", SlayerBosses.HydraPhase.BLUE.vent);
        assertEquals("blue", SlayerBosses.HydraPhase.RED.vent);
        for (SlayerBosses.HydraPhase p : SlayerBosses.HydraPhase.values()) {
            assertFalse(p.colour.equals(p.vent), p.name());
        }
    }

    @Test
    void bossPetRates() {
        assertEquals(3000, Pets.engineRate("vorkath"));
        assertEquals(4000, Pets.engineRate("zulrah"));
        assertEquals(5000, Pets.engineRate("corporeal_beast"));
        assertEquals(2560, Pets.engineRate("abyssal_sire"));
    }

    @Test
    void modernBossStats() {
        BossDef yama = ContentRegistry.boss("yama");
        assertEquals(1238, yama.combatLevel);
        assertEquals(2500, yama.hitpoints);
        assertEquals(333, yama.defenceBonus(com.runecraft.osrs.AttackType.CRUSH));
        assertTrue(yama.hasAttribute("demon"));
        assertEquals(2, yama.phases.size());
        BossDef doom = ContentRegistry.boss("doom_of_mokhaiotl");
        assertEquals(908, doom.combatLevel);
        assertEquals(625, doom.hitpoints);
        assertEquals(99, doom.attacks.stream().filter(a -> a.name().equals("charge")).findFirst().orElseThrow().maxHit());
        assertTrue(doom.attacks.stream().filter(a -> a.name().equals("charge")).findFirst().orElseThrow().telegraphTicks() >= 3);
        assertEquals(50000, ContentRegistry.boss("gemstone_crab").hitpoints);
        assertFalse(ContentRegistry.boss("gemstone_crab").aggressive);
        assertEquals(235, ContentRegistry.boss("shellbane_gryphon").combatLevel);
        assertEquals(51, ContentRegistry.boss("shellbane_gryphon").slayerLevel);
        assertEquals(30, ContentRegistry.boss("brutus").combatLevel);
        assertEquals(58, ContentRegistry.boss("brutus").hitpoints);
        assertEquals(1224, ContentRegistry.boss("demonic_brutus").combatLevel);
        assertEquals(750, ContentRegistry.boss("demonic_brutus").hitpoints);
        assertEquals(1500, ContentRegistry.boss("maggot_king").hitpoints);
        // Amoxliatl is the post-quest boss (520 HP), magic only.
        assertEquals(520, ContentRegistry.boss("amoxliatl").hitpoints);
        assertTrue(ContentRegistry.boss("amoxliatl").attacks.stream().allMatch(a -> a.style() == com.runecraft.osrs.AttackType.MAGIC));
    }

    @Test
    void modernBossDropRates() {
        assertEquals(300, rate("yama", "soulflame_horn"));
        for (String piece : new String[]{"oathplate_helm", "oathplate_chest", "oathplate_legs"}) {
            assertEquals(600, rate("yama", piece));
        }
        assertEquals(12, rate("yama", "dossier")); // wiki 1/12.1 (round 29)
        for (String u : new String[]{"mokhaiotl_cloth", "eye_of_ayak", "avernic_treads"}) {
            assertEquals(540, rate("doom_of_mokhaiotl", u));
        }
        assertEquals(30, rate("brutus", "mooleta"));
        assertEquals(150, rate("brutus", "cow_slippers"));
        assertEquals(1, rate("demonic_brutus", "brutus_slippers"));
        assertEquals(340, rate("maggot_king", "elder_venator_fang"));
        assertEquals(520, rate("maggot_king", "crimson_kisten"));
        // Varlamore bosses (solo, full contribution)
        assertEquals(105, rate("the_hueycoatl", "dragon_hunter_wand"));
        assertEquals(90, rate("the_hueycoatl", "tome_of_earth"));
        assertEquals(25, rate("amoxliatl", "pendant_of_ates"));
        assertEquals(100, rate("amoxliatl", "glacial_temotli"));
        assertEquals(16, rate("branda_the_fire_queen", "giantsoul_amulet"));
        assertEquals(75, rate("branda_the_fire_queen", "fire_element_staff_crown"));
        // Pets
        assertEquals(2500, Pets.engineRate("yama"));
        assertEquals(250, Pets.engineRate("doom_of_mokhaiotl"));
        assertEquals(1000, Pets.engineRate("brutus"));
        assertEquals(400, Pets.engineRate("demonic_brutus"));
        assertEquals(3500, Pets.engineRate("maggot_king"));
        // Their uniques fill collection log pages.
        assertTrue(CollectionLogData.pages().stream().anyMatch(p -> p.name().equals("Yama")
                && p.items().containsAll(List.of("soulflame_horn", "oathplate_helm", "oathplate_chest", "oathplate_legs", "pet_yami"))));
        assertTrue(CollectionLogData.pages().stream().anyMatch(p -> p.name().equals("Doom of Mokhaiotl")
                && p.items().containsAll(List.of("mokhaiotl_cloth", "eye_of_ayak", "avernic_treads", "pet_dom"))));
    }

    @Test
    void modernUniqueStats() {
        com.runecraft.osrs.EquipmentStats chest = com.runecraft.osrs.ItemCatalog.get("oathplate_chest").equipment().stats();
        assertEquals(128, chest.slashDefence());
        assertEquals(4, chest.meleeStrength());
        com.runecraft.osrs.EquipmentStats eye = com.runecraft.osrs.ItemCatalog.get("eye_of_ayak").equipment().stats();
        assertEquals(30, eye.magicAttack());
        assertEquals(3, com.runecraft.osrs.ItemCatalog.get("eye_of_ayak").equipment().attackSpeed());
        assertEquals(15, com.runecraft.osrs.ItemCatalog.get("avernic_treads").equipment().stats().rangedAttack());
        assertEquals(56, com.runecraft.osrs.ItemCatalog.get("crimson_kisten").equipment().stats().meleeStrength());
        assertTrue(com.runecraft.osrs.data.EquipmentBonuses.twoHanded("crimson_kisten"));
        assertEquals(23, com.runecraft.osrs.ItemCatalog.get("soulflame_horn").equipment().stats().meleeStrength());
    }
}
