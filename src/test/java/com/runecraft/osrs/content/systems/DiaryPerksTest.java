package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.content.systems.ClueScrolls.Tier;
import com.runecraft.osrs.data.Untradeables;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Diary perks hooked into other systems, saved clue puzzles and treasure trail tradeability. */
class DiaryPerksTest {
    private static DiaryRewards.TierDone only(String area, Diaries.Tier tier) {
        return (a, t) -> a.equals(area) && t == tier;
    }

    private static final DiaryRewards.TierDone NONE = (a, t) -> false;

    @Test
    void karamjaGlovesTzhaarDiscount() {
        // OSRS wiki: obsidian platebody 126,000 -> 109,200 tokkul, obsidian cape 90,000 -> 78,000.
        assertEquals(109_200, DiaryRewards.tzhaarPrice(126_000, true));
        assertEquals(78_000, DiaryRewards.tzhaarPrice(90_000, true));
        assertEquals(90_000, DiaryRewards.tzhaarPrice(90_000, false));
        assertTrue(DiaryRewards.perks("karamja", 1).stream().anyMatch(s -> s.contains("TzHaar")));
    }

    @Test
    void seersMarksRerollByKandarinTier() {
        double base = 1 / 3.0;
        assertEquals(base, DiaryRewards.seersMarkChance(base, 0), 1e-9);
        // A failed spawn is rerolled at 5/10/15%: +10%/+20%/+30% of a 1/3 base rate (OSRS wiki).
        assertEquals(base * 1.1, DiaryRewards.seersMarkChance(base, 1), 1e-9);
        assertEquals(base * 1.2, DiaryRewards.seersMarkChance(base, 2), 1e-9);
        assertEquals(base * 1.3, DiaryRewards.seersMarkChance(base, 3), 1e-9);
        assertEquals(base * 1.3, DiaryRewards.seersMarkChance(base, 4), 1e-9, "elite keeps the hard bonus");
    }

    @Test
    void hosidiusRangeNeedsKourendDiary() {
        assertEquals(0, DiaryRewards.hosidiusBurnReduction(false, false));
        assertEquals(0.05, DiaryRewards.hosidiusBurnReduction(true, false));
        assertEquals(0.10, DiaryRewards.hosidiusBurnReduction(true, true));
    }

    @Test
    void faladorFarmXp() {
        assertEquals(1.1, DiaryRewards.faladorFarmXp(true, 3051, 3305));
        assertEquals(1.0, DiaryRewards.faladorFarmXp(false, 3051, 3305));
        assertEquals(1.0, DiaryRewards.faladorFarmXp(true, 3222, 3218), "only at the Falador farm");
        assertTrue(DiaryRewards.perks("falador", 2).stream().anyMatch(s -> s.contains("Farming")));
        assertFalse(DiaryRewards.perks("falador", 1).stream().anyMatch(s -> s.contains("Farming")));
    }

    @Test
    void notedDrops() {
        assertTrue(DiaryRewards.notedDrop("giant_mole", "mole_skin", false, only("falador", Diaries.Tier.HARD)));
        assertTrue(DiaryRewards.notedDrop("giant_mole", "mole_claw", false, only("falador", Diaries.Tier.HARD)));
        assertFalse(DiaryRewards.notedDrop("giant_mole", "big_bones", false, only("falador", Diaries.Tier.HARD)));
        assertFalse(DiaryRewards.notedDrop("giant_mole", "mole_skin", false, NONE));
        for (String dk : new String[]{"dagannoth_rex", "dagannoth_prime", "dagannoth_supreme"}) {
            assertTrue(DiaryRewards.notedDrop(dk, "dagannoth_bones", false, only("fremennik", Diaries.Tier.ELITE)));
            assertFalse(DiaryRewards.notedDrop(dk, "dagannoth_bones", false, only("fremennik", Diaries.Tier.HARD)));
        }
        assertFalse(DiaryRewards.notedDrop("dagannoth", "dagannoth_bones", false, only("fremennik", Diaries.Tier.ELITE)),
                "only the kings");
        assertTrue(DiaryRewards.notedDrop("lava_dragon", "lava_dragon_bones", true, only("wilderness", Diaries.Tier.ELITE)));
        assertTrue(DiaryRewards.notedDrop("green_dragon", "dragon_bones", true, only("wilderness", Diaries.Tier.ELITE)));
        assertFalse(DiaryRewards.notedDrop("green_dragon", "dragon_bones", false, only("wilderness", Diaries.Tier.ELITE)),
                "outside the Wilderness");
        assertFalse(DiaryRewards.notedDrop("lava_dragon", "lava_dragon_bones", true, NONE));
        // Every noted item must be a real catalog item that can be noted.
        for (String id : new String[]{"mole_skin", "mole_claw", "dagannoth_bones", "dragon_bones", "lava_dragon_bones"}) {
            assertNotNull(com.runecraft.osrs.ItemCatalog.get(id), id);
        }
    }

    @Test
    void lumbridgeEliteFairyRingsAndZanaris() {
        assertTrue(DiaryRewards.fairyRingsWithoutStaff(true));
        assertFalse(DiaryRewards.fairyRingsWithoutStaff(false));
        AccessGates.Gate z = AccessGates.byId("zanaris");
        assertNotNull(z);
        AccessGates.PlayerView elite = view(true);
        AccessGates.PlayerView none = view(false);
        assertTrue(z.reqs().stream().allMatch(r -> r.unmet(elite) == null));
        assertTrue(z.reqs().stream().anyMatch(r -> r.unmet(none) != null));
    }

    private static AccessGates.PlayerView view(boolean lumbridgeElite) {
        return new AccessGates.PlayerView() {
            public int level(com.runecraft.osrs.Skill skill) {
                return 99;
            }

            public int questStage(String questId) {
                return 100;
            }

            public boolean questComplete(String questId) {
                return true;
            }

            public int questPoints() {
                return 300;
            }

            public boolean wearing(String itemId) {
                return false;
            }

            public boolean carrying(String itemId) {
                return false;
            }

            public boolean hasCombatGear() {
                return false;
            }

            public boolean diaryComplete(String area, Diaries.Tier tier) {
                return lumbridgeElite && area.equals("lumbridge") && tier == Diaries.Tier.ELITE;
            }
        };
    }

    @Test
    void eliteVoidNeedsWesternHard() {
        assertFalse(DiaryRewards.pestControlUnlocked("elite_void_top", false));
        assertFalse(DiaryRewards.pestControlUnlocked("elite_void_robe", false));
        assertTrue(DiaryRewards.pestControlUnlocked("elite_void_top", true));
        assertTrue(DiaryRewards.pestControlUnlocked("void_knight_top", false));
    }

    @Test
    void puzzlesSurviveSaving() {
        Random r = new Random(7);
        for (int n : new int[]{3, 5}) {
            CluePuzzles.Slider s = CluePuzzles.Slider.scrambled(n, r);
            s.move(s.gap() > 0 ? s.gap() - 1 : s.gap() + 1);
            CluePuzzles.Slider back = CluePuzzles.Slider.decode(s.encode());
            assertNotNull(back);
            assertEquals(s.encode(), back.encode());
            assertEquals(s.solved(), back.solved());
        }
        assertNull(CluePuzzles.Slider.decode("3:1,2,3,4,5,6,7,8,8"), "duplicate tile");
        assertNull(CluePuzzles.Slider.decode("3:2,1,3,4,5,6,7,8,0"), "unsolvable board");
        assertNull(CluePuzzles.Slider.decode("garbage"));

        CluePuzzles.LightBox box = CluePuzzles.LightBox.scrambled(r);
        box.press(2);
        CluePuzzles.LightBox copy = CluePuzzles.LightBox.decode(box.encode());
        assertNotNull(copy);
        assertEquals(box.encode(), copy.encode());
        // The copy keeps the button wiring: pressing the same button changes both the same way.
        box.press(5);
        copy.press(5);
        assertEquals(box.encode(), copy.encode());
        assertEquals(box.litCount(), copy.litCount());
        assertNull(CluePuzzles.LightBox.decode("111:1,2"));
    }

    @Test
    void treasureTrailTradeability() {
        // OSRS: clue scrolls, reward caskets, puzzle/light boxes and the Bloodhound are untradeable...
        for (Tier t : Tier.values()) {
            assertTrue(Untradeables.untradeable(t.scrollItem()), t.scrollItem());
            assertTrue(Untradeables.untradeable(t.casketItem()), t.casketItem());
        }
        for (String id : Set.of("puzzle_box", "light_box", "bloodhound")) {
            assertTrue(Untradeables.untradeable(id), id);
        }
        // ...but the reward items themselves trade freely.
        for (String id : Set.of("pirate_hat", "ranger_gloves", "holy_wraps", "ankou_gloves", "mummys_hands")) {
            assertFalse(Untradeables.untradeable(id), id);
        }
        assertEquals("Pirate's hat", com.runecraft.osrs.ItemCatalog.get("pirate_hat").name());
    }
}
