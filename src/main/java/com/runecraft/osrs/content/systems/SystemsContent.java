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
    /** Watson (master clue exchange) in his house in Hosidius (OSRS Wiki map 1646, 3574). */
    public static final String WATSON = "watson";
    public static final int WATSON_X = 1646;
    public static final int WATSON_Y = 3574;
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
        // Treasure trail tools (OSRS Wiki): the sextant (Murphy, Port Khazard), watch (Brother Kojo, Clock Tower) and chart
        // (Observatory professor) locate the player for coordinate clues; Reldo's and Jorral's strange devices find hot/cold
        // treasure; medium and elite key steps drop a key from the monster the clue names.
        Defs.simple(items, "sextant", "Sextant", "tool_generic", 0xB09040, 50).stack(1).weight(0.2)
                .examine("Used by navigators to find their position in Gielinor.");
        Defs.simple(items, "watch", "Watch", "bracelet", 0xC0C0C8, 50).stack(1).weight(0.1).examine("A handy way to tell the time.");
        Defs.simple(items, "chart", "Chart", "map", 0xE0D0A0, 50).stack(1).weight(0.1).examine("A chart of the stars.");
        Defs.simple(items, ClueScrolls.strangeDevice(ClueScrolls.Tier.BEGINNER), "Strange device", "orb", 0x8AA0C0, 20).stack(1)
                .weight(0.02).examine("You can sense strange powers emanating from it.");
        Defs.simple(items, ClueScrolls.strangeDevice(ClueScrolls.Tier.MASTER), "Strange device", "orb", 0x6A3A8A, 2).stack(1)
                .weight(0.01).examine("You can sense strange powers emanating from it.");
        Defs.simple(items, "key_medium", "Key (medium)", "key", 0xB09040, 1).stack(1).examine("A key to unlock a treasure chest.");
        Defs.simple(items, "key_elite", "Key (elite)", "key", 0xE0C030, 1).stack(1).examine("A key to unlock a treasure chest.");
        // Weiss Salt Mine (ItemSources): basalt and the three salts that make the basalt teleports (OSRS Wiki values; the salts
        // are in CommonItems, since the skilling module's eternal brazier takes them too).
        Defs.simple(items, "basalt", "Basalt", "ore", 0x3A3A40, 10).stack(1).weight(1).examine("A rock that holds salts well.");
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
                BlockDef.scripted(DiaryExtras.ENT_TRUNK, "Ent trunk", 0x5A4A2A, "stump"),
                BlockDef.scripted(DiaryExtras.WATER_PUMP, "Water pump", 0x6A6A70, "pump"),
                BlockDef.scripted(DiaryExtras.WELL, "Well", 0x7A7A80, "water_container"),
                BlockDef.scripted(DiaryTransport.EYRIE, "Eagle's eyrie", 0x8A6A3A, "stump"),
                BlockDef.scripted(DiaryTransport.TRAIN, "Train cart", 0x5A4A3A, "machine"),
                BlockDef.scripted(DiaryExtras.SEAWEED.block(), DiaryExtras.SEAWEED.name(), 0x3A6A2A, "plant"),
                BlockDef.scripted(DiaryExtras.POTATO_CACTUS.block(), DiaryExtras.POTATO_CACTUS.name(), 0x6A8A3A, "plant"),
                BlockDef.scripted(ItemSources.SALT_ROCKS.get(0).block(), ItemSources.SALT_ROCKS.get(0).name(), 0x3A3A40, "rock"),
                BlockDef.scripted(ItemSources.SALT_ROCKS.get(1).block(), ItemSources.SALT_ROCKS.get(1).name(), 0xE0E0D0, "rock"),
                BlockDef.scripted(ItemSources.SALT_ROCKS.get(2).block(), ItemSources.SALT_ROCKS.get(2).name(), 0xB0D8F0, "rock"),
                BlockDef.scripted(ItemSources.SALT_ROCKS.get(3).block(), ItemSources.SALT_ROCKS.get(3).name(), 0x9A6A4A, "rock"));
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
                PlaceDef.of(WATSON, "Watson", WATSON_X, WATSON_Y, PlaceDef.Style.NONE, List.of()),
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
        // Round 40: the diary taskmasters, the Falador water pump and the Rellekka well (the hooks put each block at its tile).
        for (DiaryExtras.Taskmaster t : DiaryExtras.TASKMASTERS) {
            if (!t.npcId().startsWith("mg_")) {
                places.add(PlaceDef.of(t.npcId(), t.name(), t.x(), t.y(), PlaceDef.Style.NONE, List.of()));
            }
        }
        places.add(PlaceDef.of(DiaryExtras.WATER_PUMP, "Water pump", DiaryExtras.FALADOR_PUMP_X, DiaryExtras.FALADOR_PUMP_Y,
                PlaceDef.Style.NONE, List.of()));
        places.add(PlaceDef.of(DiaryExtras.WELL, "Well", DiaryExtras.RELLEKKA_WELL_X, DiaryExtras.RELLEKKA_WELL_Y, PlaceDef.Style.NONE,
                List.of()));
        for (DiaryTransport.Stop s : DiaryTransport.STOPS) {
            places.add(PlaceDef.of("diary_stop_" + s.id(), s.name(), s.x(), s.y(), PlaceDef.Style.NONE, List.of()));
        }
        // Weiss Salt Mine rocks and the Fremennik armour makers (round 28): the hooks put each rock on the ground at its tile.
        for (ItemSources.SaltRock r : ItemSources.SALT_ROCKS) {
            places.add(PlaceDef.of(r.block(), r.name(), r.x(), r.y(), PlaceDef.Style.NONE, List.of()));
        }
        places.add(PlaceDef.of(ItemSources.SKULGRIMEN, "Skulgrimen's Battle Gear", 2662, 3693, PlaceDef.Style.NONE, List.of())); // approx tile
        places.add(PlaceDef.of(ItemSources.PEER_THE_SEER, "Peer the Seer", 2632, 3667, PlaceDef.Style.NONE, List.of())); // approx tile
        places.addAll(ClueNpcs.places());
        // Bank deposit boxes: nothing is levelled; the hooks put the box on the ground at the spot.
        for (com.runecraft.osrs.bank.DepositBoxes.Spot spot : com.runecraft.osrs.bank.DepositBoxes.SPOTS) {
            places.add(PlaceDef.of(spot.id(), spot.name(), spot.osrsX(), spot.osrsY(), PlaceDef.Style.NONE, List.of()));
        }
        return places;
    }

    @Override
    public List<com.runecraft.osrs.content.MonsterDef> monsters() {
        return ClueGuardians.monsters();
    }

    @Override
    public List<NpcDef> npcs() {
        List<NpcDef> npcs = new ArrayList<>(ClueNpcs.npcs());
        npcs.addAll(List.of(
                NpcDef.of("grand_exchange_clerk", "Grand Exchange Clerk", "grand_exchange", 3, 0, 0xE0B090, 0x3A6A3A, 0x2A2A2A, 0x3A6A3A),
                NpcDef.of("wise_old_man", "Wise Old Man", "grand_exchange", -3, 2, 0xE0B090, 0x3050C0, 0x3050C0, 0xE0E0E0),
                NpcDef.of(WATSON, "Watson", WATSON, 0, 0, 0xE0B090, 0x6A4A2A, 0x2A2A2A, 0x6A4A2A),
                NpcDef.of("achievement_diary_master", "Diary master", "grand_exchange", -4, -2, 0xE0B090, 0x8A6A3A, 0x5A3A2A, 0x8A6A3A),
                NpcDef.of(CluePuzzles.FALO, "Falo the Bard", CluePuzzles.FALO, 0, 0, 0xE0B090, 0x3A6A9A, 0x5A3A2A, 0xC03030),
                NpcDef.of(GHOMMAL, "Ghommal", GHOMMAL, 0, 0, 0xC8A080, 0x8A8A90, 0x5A5A60, 0x8A8A90),
                NpcDef.of(DiaryExtras.TINDEL, "Tindel Marchant", DiaryExtras.TINDEL, 0, 0, 0xE0B898, 0x6A4A2A, 0x3A3A4A, 0x5A3A2A),
                NpcDef.of(DiaryExtras.GENIE, "Genie", DiaryExtras.GENIE, 0, 0, 0x5A8AE0, 0xC03030, 0x3A6AC0, 0xE0C040),
                NpcDef.of(Multicannon.NULODION, "Nulodion", Multicannon.NULODION, 0, 0, 0xD8A888, 0x3A3A3A, 0x2A2A2A, 0x6A6A70),
                NpcDef.of(DiaryExtras.SAWMILL_OPERATOR, "Sawmill operator", DiaryExtras.SAWMILL_OPERATOR, 0, 0, 0xE0B090, 0x6A4A2A,
                        0x3A3A4A, 0x8A6A3A),
                NpcDef.of(ItemSources.SKULGRIMEN, "Skulgrimen", ItemSources.SKULGRIMEN, 0, 0, 0xE8C0A0, 0x6A5A4A, 0x3A2A1A, 0x8A6A3A),
                NpcDef.of(ItemSources.PEER_THE_SEER, "Peer the Seer", ItemSources.PEER_THE_SEER, 0, 0, 0xE8C0A0, 0x3A3A6A, 0x2A2A3A, 0xE0C060)));
        // The diary taskmasters (round 40), each at its OSRS tile; Karamja's Pirate Jackie belongs to the minigames module.
        int[][] looks = {{0xE0B090, 0x6A3A2A, 0x3A2A1A, 0x6A3A2A}, {0xC8A080, 0xD8C8A0, 0x8A6A3A, 0xE0D8C0},
                {0xE0B090, 0xC0C0C8, 0x8A8A90, 0xC0C0C8}, {0xE0B090, 0x6A4A2A, 0x5A3A2A, 0x8A6A3A}, {0xE0B090, 0x3A5A8A, 0x2A2A3A, 0x3A5A8A},
                null, {0xE0B090, 0x7A3A5A, 0x3A2A3A, 0x7A3A5A}, {0xE0B090, 0x3A6A3A, 0x2A3A2A, 0x6A4A2A},
                {0xB0A090, 0x4A3A5A, 0x2A2A2A, 0x4A3A5A}, {0xE0B090, 0x8A2A2A, 0x3A3A4A, 0x8A6A3A}, {0xE8C0A0, 0x6A8A3A, 0x4A5A2A, 0xC03030},
                {0xE0B090, 0x8A1A1A, 0x2A0A0A, 0x8A1A1A}};
        for (int i = 0; i < DiaryExtras.TASKMASTERS.size(); i++) {
            DiaryExtras.Taskmaster t = DiaryExtras.TASKMASTERS.get(i);
            if (looks[i] != null) {
                npcs.add(NpcDef.of(t.npcId(), t.name(), t.npcId(), 0, 0, looks[i][0], looks[i][1], looks[i][2], looks[i][3]));
            }
        }
        return npcs;
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
