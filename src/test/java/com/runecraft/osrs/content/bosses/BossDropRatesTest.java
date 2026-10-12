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
}
