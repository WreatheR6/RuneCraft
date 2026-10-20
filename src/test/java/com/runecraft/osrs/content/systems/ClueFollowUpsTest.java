package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.systems.ClueScrolls.Tier;
import com.runecraft.osrs.content.zeah.ZeahHunter;
import com.runecraft.osrs.data.Drop;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Per-monster clue rates, puzzle boxes, light boxes, Falo the Bard, new clue uniques and diary perks. */
class ClueFollowUpsTest {
    @Test
    void clueRatesComeFromTheWikiTable() {
        assertTrue(ClueDrops.size() > 90);
        // Wiki rates (osrsreboxed dump).
        assertEquals(128, ClueScrolls.dropOneIn(Tier.HARD, "Abyssal demon", 124));
        assertEquals(1200, ClueScrolls.dropOneIn(Tier.ELITE, "Abyssal demon", 124));
        assertEquals(64, ClueScrolls.dropOneIn(Tier.BEGINNER, "Goblin", 2));
        assertEquals(128, ClueScrolls.dropOneIn(Tier.EASY, "Goblin", 2));
        assertEquals(50, ClueScrolls.dropOneIn(Tier.BEGINNER, "Hill Giant", 28));
        assertEquals(0, ClueScrolls.dropOneIn(Tier.EASY, "Hill Giant", 28), "known monsters drop only their wiki tiers");
        assertEquals(500, ClueScrolls.dropOneIn(Tier.ELITE, "Black dragon", 227));
        assertEquals(0, ClueScrolls.dropOneIn(Tier.MASTER, "Black dragon", 227));
        // Unknown monsters keep the combat-level fallback.
        assertEquals(ClueScrolls.dropOneIn(Tier.HARD, 150), ClueScrolls.dropOneIn(Tier.HARD, "Some unknown beast", 150));
        assertFalse(ClueDrops.known("Some unknown beast"));
        // A monster's own clue drops replace the generic roll for those tiers.
        Set<Tier> own = ClueScrolls.ownClueTiers(List.of(Drop.of("clue_scroll_elite", 1, 1, 100), Drop.always("bones")));
        assertEquals(EnumSet.of(Tier.ELITE), own);
    }

    @Test
    void sliderPuzzlesAreSolvable() {
        for (int n : new int[]{3, 5}) {
            for (long seed = 0; seed < 20; seed++) {
                CluePuzzles.Slider s = CluePuzzles.Slider.scrambled(n, new Random(seed));
                assertFalse(s.solved());
                assertTrue(s.solvable());
            }
        }
        CluePuzzles.Slider s = CluePuzzles.Slider.solvedBoard(3);
        assertTrue(s.solved());
        assertFalse(s.move(0), "tile 1 isn't next to the gap");
        assertTrue(s.move(7), "tile 8 slides right into the gap");
        assertFalse(s.solved());
        assertTrue(s.move(8));
        assertTrue(s.solved());
        assertEquals(5, CluePuzzles.puzzleSize(Tier.HARD), "every OSRS puzzle box is 5x5");
        assertEquals(5, CluePuzzles.puzzleSize(Tier.MASTER));
    }

    @Test
    void lightBoxesToggleAndSolve() {
        CluePuzzles.LightBox box = CluePuzzles.LightBox.scrambled(new Random(3));
        assertFalse(box.solved());
        int before = box.litCount();
        box.press(0);
        box.press(0);
        assertEquals(before, box.litCount(), "pressing a button twice undoes it");
        // Some combination of the 8 buttons lights everything (the box was scrambled from all-lit).
        boolean solvable = false;
        for (int mask = 0; mask < 256 && !solvable; mask++) {
            CluePuzzles.LightBox copy = CluePuzzles.LightBox.scrambled(new Random(3));
            for (int b = 0; b < 8; b++) {
                if ((mask >> b & 1) == 1) {
                    copy.press(b);
                }
            }
            solvable = copy.solved();
        }
        assertTrue(solvable);
    }

    @Test
    void puzzleStepsAndFaloAppearOnTheirTiers() {
        List<ClueScrolls.Npc> npcs = ClueScrolls.npcs();
        assertTrue(npcs.stream().anyMatch(n -> n.id().equals(CluePuzzles.FALO)), "Falo stands in the world");
        boolean puzzle = false;
        boolean light = false;
        boolean falo = false;
        for (Tier t : Tier.values()) {
            for (long seed = 0; seed < 300; seed++) {
                for (ClueScrolls.Step s : ClueScrolls.generate(t, seed, npcs)) {
                    if (s.type() == ClueScrolls.StepType.FALO) {
                        falo = true;
                        assertEquals(Tier.MASTER, t);
                        assertEquals(CluePuzzles.FALO, s.npcId());
                        assertFalse(ClueReqs.items(s.req()).isEmpty(), s.text());
                    }
                    if (CluePuzzles.PUZZLE_BOX.equals(s.box())) {
                        puzzle = true;
                        assertTrue(t.ordinal() >= Tier.HARD.ordinal(), "puzzle boxes: hard, elite and master");
                    }
                    if (CluePuzzles.LIGHT_BOX.equals(s.box())) {
                        light = true;
                        assertEquals(Tier.MASTER, t, "light boxes: master only");
                    }
                }
            }
        }
        assertTrue(puzzle && light && falo);
        // Falo's twenty songs, each naming an item this world has (RuneLite FaloTheBardClue).
        long songs = ClueScrolls.clues().stream().filter(c -> c.type() == ClueScrolls.StepType.FALO).count();
        assertEquals(20, songs);
        assertNotNull(ItemCatalog.get(CluePuzzles.PUZZLE_BOX));
        assertNotNull(ItemCatalog.get(CluePuzzles.LIGHT_BOX));
    }

    @Test
    void newClueUniquesArePerTier() {
        assertTrue(ClueRewards.uniques(Tier.EASY).containsAll(List.of("bronze_platebody_t", "iron_full_helm_g", "steel_kiteshield_t",
                "black_wizard_hat_g", "bobs_red_shirt", "ham_joint", "black_shield_h3", "team_cape_x")));
        assertTrue(ClueRewards.uniques(Tier.MEDIUM).containsAll(List.of("wolf_cloak", "crier_bell", "cabbage_round_shield",
                "adamant_helm_h5", "pink_boater")));
        assertTrue(ClueRewards.uniques(Tier.HARD).containsAll(List.of("armadyl_platebody", "saradomin_dhide_body", "pith_helmet",
                "rune_shield_h1", "nunchaku")));
        assertTrue(ClueRewards.uniques(Tier.ELITE).containsAll(List.of("dragon_cane", "dragon_full_helm_ornament_kit", "top_hat",
                "dark_tuxedo_jacket")));
        assertTrue(ClueRewards.uniques(Tier.MASTER).containsAll(List.of("samurai_greaves", "lesser_demon_mask", "hood_of_darkness",
                "bowl_wig")));
        for (Tier t : Tier.values()) {
            for (String id : ClueRewards.uniques(t)) {
                assertNotNull(ItemCatalog.get(id), id);
            }
        }
    }

    @Test
    void diaryPerks() {
        assertEquals(1.0, DiaryRewards.pickpocketMultiplier(false, false, true));
        assertEquals(1.1, DiaryRewards.pickpocketMultiplier(true, false, true));
        assertEquals(1.0, DiaryRewards.pickpocketMultiplier(true, false, false), "medium only counts in Ardougne");
        assertEquals(1.1, DiaryRewards.pickpocketMultiplier(true, true, false), "hard counts everywhere");
        assertEquals(1.5, DiaryRewards.barrowsRuneMultiplier(true));
        assertEquals(1.0, DiaryRewards.barrowsRuneMultiplier(false));
        assertTrue(DiaryRewards.perks("morytania", 4).stream().anyMatch(s -> s.contains("Barrows")));
    }

    @Test
    void huntersRumours() {
        assertEquals("quetzal_whistle_basic", ZeahHunter.whistleAt(10));
        assertNull(ZeahHunter.whistleAt(11));
        assertNull(ZeahHunter.rumourTarget(30, new Random(1)));
        for (int i = 0; i < 20; i++) {
            ZeahHunter.Spot s = ZeahHunter.rumourTarget(70, new Random(i));
            assertNotNull(s);
            assertTrue(s.level() <= 70);
        }
    }
}
