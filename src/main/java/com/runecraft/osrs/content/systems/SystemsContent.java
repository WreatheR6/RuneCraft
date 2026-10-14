package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.CollectionLogData;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ContentModule;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Special attacks, enchanted jewellery and teleport items, the Ancient/Lunar/Arceuus spellbooks, clue
 * scrolls, the Grand Exchange, achievement diaries, skillcapes, run energy and hiscores.
 */
public final class SystemsContent implements ContentModule {
    public static final String GE_BOOTH = "grand_exchange_booth";
    public static final String CAPE_STAND = "skillcape_stand";
    public static final String FOUNTAIN = "fountain_of_heroes";
    public static final String ANCIENT_ALTAR = "ancient_altar";
    public static final String LUNAR_ALTAR = "lunar_altar";
    public static final String DARK_ALTAR = "dark_altar";
    public static final String DIARY_BOARD = "achievement_diary_board";
    /** Falo the Bard stands on the hill north of Seers' Village (OSRS ~2689, 3550). */
    public static final int FALO_X = 2689;
    public static final int FALO_Y = 3550;
    /** Ghommal, the Combat Achievements taskmaster, outside the Warriors' Guild. */
    public static final String GHOMMAL = "ghommal";

    @Override
    public String id() {
        return "systems";
    }

    @Override
    public void items(ItemSink items) {
        Jewellery.items(items);
        ClueRewards.items(items);
        Capes.items(items);
        CombatAchievements.items(items);
        Multicannon.items(items);
        Defs.simple(items, CluePuzzles.PUZZLE_BOX, "Puzzle box", "package", 0x8A6A3A, 1).stack(1)
                .examine("Slide the pieces into order to solve the puzzle.");
        Defs.simple(items, CluePuzzles.LIGHT_BOX, "Light box", "package", 0xE0D060, 1).stack(1)
                .examine("Turn all of the lights on to solve the puzzle.");
    }

    /**
     * Zenyte upgrades with boss materials from osrs/content/CommonItems: necklace of rupture (84 Crafting, chisel) and
     * confliction gauntlets (83 Crafting and 70 Smithing in OSRS, plus 10,000 demon tears; made here at an anvil).
     */
    @Override
    public void recipes(Consumer<Recipe> recipes) {
        recipes.accept(Recipe.builder("make_necklace_of_rupture", MenuType.GEM_CUTTING, "Necklace of rupture")
                .skill(Skill.CRAFTING, 84, 0).in("necklace_of_anguish", 1).in("elder_venator_fang", 1)
                .out("necklace_of_rupture", 1).tool("chisel").ticks(3).build()); // XP approx (none known)
        recipes.accept(Recipe.builder("make_confliction_gauntlets", MenuType.SMITHING, "Confliction gauntlets")
                .skill(Skill.CRAFTING, 83, 0).in("tormented_bracelet", 1).in("mokhaiotl_cloth", 1).in("demon_tear", 10000)
                .out("confliction_gauntlets", 1).ticks(3).build()); // XP approx (none known)
        // Gold helmet (OSRS Wiki): 3 gold bars and a hammer at an anvil, 50 Smithing, 30 XP. OSRS only lets players who have
        // started Between a Rock... make it, and only one at a time; neither is enforced here. // approx
        recipes.accept(Recipe.builder("smith_gold_helmet", MenuType.SMITHING, "Gold helmet")
                .skill(Skill.SMITHING, 50, 30).in("gold_bar", 3).out("gold_helmet", 1).tool("hammer").ticks(5).build());
        // God books: the god's four clue scroll pages complete a damaged book (CommonEquipment). OSRS adds the pages one at a
        // time by using them on the book; here all four are bound at once in the needle menu. // approx
        String[] gods = {"saradomin", "zamorak", "guthix", "bandos", "armadyl", "ancient"};
        for (int i = 0; i < gods.length; i++) {
            String[] book = com.runecraft.osrs.content.CommonEquipment.GOD_BOOKS[i];
            Recipe.Builder b = Recipe.builder("complete_" + book[1], MenuType.LEATHERWORKING, book[2] + " (4 " + book[3] + " pages)")
                    .in(book[0], 1).out(book[1], 1).ticks(2);
            for (int page = 1; page <= 4; page++) {
                b.in(gods[i] + "_page_" + page, 1);
            }
            recipes.accept(b.build());
        }
    }


    @Override
    public List<BlockDef> blocks() {
        return List.of(
                BlockDef.scripted(GE_BOOTH, "Grand Exchange booth", 0x3A6A3A, "machine"),
                BlockDef.scripted(CAPE_STAND, "Skillcape stand", 0x8A2A2A, "statue"),
                BlockDef.scripted(FOUNTAIN, "Fountain of Heroes", 0x3A7AC0, "altar"),
                BlockDef.scripted(ANCIENT_ALTAR, "Ancient altar", 0x6A3A8A, "altar"),
                BlockDef.scripted(LUNAR_ALTAR, "Lunar altar", 0xC0C0E0, "altar"),
                BlockDef.scripted(DARK_ALTAR, "Dark altar", 0x2A2A3A, "obelisk"),
                BlockDef.scripted(DIARY_BOARD, "Achievement diary board", 0x8A6A3A, "crate"),
                BlockDef.scripted(com.runecraft.osrs.bank.DepositBoxes.BLOCK, "Bank deposit box", 0x6A5A3A, "chest"),
                BlockDef.scripted(DiaryExtras.MUDDY_CHEST, "Muddy chest", 0x6A4A2A, "chest"),
                BlockDef.scripted(DiaryExtras.CRYSTAL_CHEST, "Crystal chest", 0x8AB0C0, "chest"),
                BlockDef.scripted(Multicannon.BLOCK, "Dwarf multicannon", 0x3A3A40, "machine"),
                BlockDef.scripted(DiaryExtras.SALTPETRE_DEPOSIT, "Saltpetre deposit", 0x9A9A90, "rock"),
                BlockDef.scripted(DiaryExtras.LEGENDS_TOTEM, "Totem pole", 0x8A5A2A, "totem"),
                BlockDef.scripted(DiaryExtras.ENT_TRUNK, "Ent trunk", 0x5A4A2A, "stump"));
    }

    @Override
    public List<PlaceDef> places() {
        List<PlaceDef> places = new ArrayList<>(List.of(
                PlaceDef.of("grand_exchange", "Grand Exchange", 3165, 3487, PlaceDef.Style.TOWN,
                        List.of(GE_BOOTH, GE_BOOTH, CAPE_STAND, DIARY_BOARD)),
                PlaceDef.of("heroes_guild", "Heroes' Guild", 2902, 3510, PlaceDef.Style.TOWN, List.of(FOUNTAIN)),
                PlaceDef.of("jaldraocht_pyramid", "Jaldraocht Pyramid", 3233, 2898, PlaceDef.Style.RUINS, List.of(ANCIENT_ALTAR)),
                PlaceDef.of("lunar_isle", "Lunar Isle", 2100, 3914, PlaceDef.Style.TOWN, List.of(LUNAR_ALTAR)),
                PlaceDef.of("arceuus_dark_altar", "Dark Altar", 1715, 3883, PlaceDef.Style.RUINS, List.of(DARK_ALTAR)),
                PlaceDef.of(CluePuzzles.FALO, "Falo the Bard", FALO_X, FALO_Y, PlaceDef.Style.NONE, List.of()),
                PlaceDef.of(GHOMMAL, "Ghommal", CombatAchievements.GHOMMAL_X, CombatAchievements.GHOMMAL_Y, PlaceDef.Style.NONE, List.of()),
                PlaceDef.of(DiaryExtras.TINDEL, "Tindel Marchant", DiaryExtras.TINDEL_X, DiaryExtras.TINDEL_Y, PlaceDef.Style.NONE, List.of()),
                PlaceDef.of(DiaryExtras.GENIE, "Genie's crevice", DiaryExtras.GENIE_X, DiaryExtras.GENIE_Y, PlaceDef.Style.NONE, List.of()),
                PlaceDef.of(DiaryExtras.MUDDY_CHEST, "Muddy chest", DiaryExtras.MUDDY_CHEST_X, DiaryExtras.MUDDY_CHEST_Y, PlaceDef.Style.NONE,
                        List.of(DiaryExtras.MUDDY_CHEST)),
                PlaceDef.of(Multicannon.NULODION, "Nulodion", Multicannon.NULODION_X, Multicannon.NULODION_Y, PlaceDef.Style.NONE, List.of()),
                PlaceDef.of(DiaryExtras.CRYSTAL_CHEST, "Crystal chest", DiaryExtras.CRYSTAL_CHEST_X, DiaryExtras.CRYSTAL_CHEST_Y,
                        PlaceDef.Style.NONE, List.of(DiaryExtras.CRYSTAL_CHEST))));
        // Diary objects (round 23): the hooks put each block on the ground (or hall floor) at its tile.
        for (int i = 0; i < DiaryExtras.SALTPETRE_TILES.length; i++) {
            int[] t = DiaryExtras.SALTPETRE_TILES[i];
            places.add(PlaceDef.of(DiaryExtras.SALTPETRE_DEPOSIT + "_" + (i + 1), "Saltpetre deposit", t[0], t[1], PlaceDef.Style.NONE,
                    List.of()));
        }
        places.add(PlaceDef.of(DiaryExtras.LEGENDS_TOTEM, "Totem pole", DiaryExtras.TOTEM_X, DiaryExtras.TOTEM_Y, PlaceDef.Style.NONE,
                List.of()));
        places.add(PlaceDef.of(DiaryExtras.SAWMILL_OPERATOR, "Sawmill", DiaryExtras.SAWMILL_X, DiaryExtras.SAWMILL_Y, PlaceDef.Style.NONE,
                List.of()));
        // Bank deposit boxes: nothing is levelled; the hooks put the box on the ground at the spot.
        for (com.runecraft.osrs.bank.DepositBoxes.Spot spot : com.runecraft.osrs.bank.DepositBoxes.SPOTS) {
            places.add(PlaceDef.of(spot.id(), spot.name(), spot.osrsX(), spot.osrsY(), PlaceDef.Style.NONE, List.of()));
        }
        return places;
    }

    @Override
    public List<NpcDef> npcs() {
        return List.of(
                NpcDef.of("grand_exchange_clerk", "Grand Exchange Clerk", "grand_exchange", 3, 0, 0xE0B090, 0x3A6A3A, 0x2A2A2A, 0x3A6A3A),
                NpcDef.of("wise_old_man", "Wise Old Man", "grand_exchange", -3, 2, 0xE0B090, 0x3050C0, 0x3050C0, 0xE0E0E0),
                NpcDef.of("watson", "Watson", "grand_exchange", 0, 4, 0xE0B090, 0x6A4A2A, 0x2A2A2A, 0x6A4A2A),
                NpcDef.of("achievement_diary_master", "Diary master", "grand_exchange", -4, -2, 0xE0B090, 0x8A6A3A, 0x5A3A2A, 0x8A6A3A),
                NpcDef.of(CluePuzzles.FALO, "Falo the Bard", CluePuzzles.FALO, 0, 0, 0xE0B090, 0x3A6A9A, 0x5A3A2A, 0xC03030),
                NpcDef.of(GHOMMAL, "Ghommal", GHOMMAL, 0, 0, 0xC8A080, 0x8A8A90, 0x5A5A60, 0x8A8A90),
                NpcDef.of(DiaryExtras.TINDEL, "Tindel Marchant", DiaryExtras.TINDEL, 0, 0, 0xE0B898, 0x6A4A2A, 0x3A3A4A, 0x5A3A2A),
                NpcDef.of(DiaryExtras.GENIE, "Genie", DiaryExtras.GENIE, 0, 0, 0x5A8AE0, 0xC03030, 0x3A6AC0, 0xE0C040),
                NpcDef.of(Multicannon.NULODION, "Nulodion", Multicannon.NULODION, 0, 0, 0xD8A888, 0x3A3A3A, 0x2A2A2A, 0x6A6A70),
                NpcDef.of(DiaryExtras.SAWMILL_OPERATOR, "Sawmill operator", DiaryExtras.SAWMILL_OPERATOR, 0, 0, 0xE0B090, 0x6A4A2A,
                        0x3A3A4A, 0x8A6A3A));
    }

    @Override
    public List<CollectionLogData.Page> collectionLog() {
        List<CollectionLogData.Page> pages = new ArrayList<>();
        for (ClueScrolls.Tier t : ClueScrolls.Tier.values()) {
            String name = Character.toUpperCase(t.id().charAt(0)) + t.id().substring(1) + " Treasure Trails";
            pages.add(new CollectionLogData.Page("Clues", name, ClueRewards.logItems(t)));
        }
        return pages;
    }
}
