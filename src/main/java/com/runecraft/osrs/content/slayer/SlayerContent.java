package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.CollectionLogData;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ContentModule;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.data.Drop;
import com.runecraft.osrs.model.ModelSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** Slayer monsters, dragons, slayer masters' new assignments, slayer gear and rewards. */
public final class SlayerContent implements ContentModule {
    private static List<MonsterDef> monsters;

    /** Konar quo Maten's tile on Mount Karuulm (OSRS Wiki map; the brimstone chest stands beside her). */
    public static final int[] KONAR_TILE = {1309, 3785};
    /** Larran's big chest, on the deck of the ship west of the Pirates' Hideout (OSRS Wiki map, plane 1). */
    public static final int[] LARRAN_CHEST_TILE = {3018, 3955};
    public static final String LARRAN_SHIP = "larrans_ship";
    /** Vannaka's tile in the Edgeville Dungeon (OSRS Wiki map, cache coordinates). */
    public static final int[] VANNAKA_TILE = {3147, 9914};
    public static final String VANNAKA_POST = "vannaka_post";

    @Override
    public String id() {
        return "slayer";
    }

    @Override
    public void items(ItemSink items) {
        SlayerItems.add(items);
        TormentedDemons.items(items);
        DemonicGorillas.items(items);
    }

    @Override
    public void recipes(Consumer<Recipe> recipes) {
        recipes.accept(Recipe.builder("broad_arrows", MenuType.FLETCHING, "Broad arrows").skill(Skill.FLETCHING, 52, 150)
                .in("headless_arrow", 15).in("broad_arrowheads", 15).out("broad_arrows", 15).ticks(2).build());
        TormentedDemons.recipes(recipes);
        DemonicGorillas.recipes(recipes);
    }

    /** Every creature this module adds (natural spawns plus superiors). Built once. */
    public static synchronized List<MonsterDef> allMonsters() {
        if (monsters == null) {
            List<MonsterDef> list = new ArrayList<>();
            list.addAll(LowSlayerMonsters.all());
            list.addAll(MidSlayerMonsters.all());
            list.addAll(HighSlayerMonsters.all());
            list.addAll(Dragons.all());
            list.addAll(SlayerExtraMonsters.all());
            list.addAll(SpawnVariants.all());
            list.addAll(SlayerTaskMonsters.all());
            list.addAll(SlayerTaskMonsters2.all());
            list.addAll(SlayerTaskMonsters3.all());
            list.add(TormentedDemons.monster());
            list.addAll(DemonicGorillas.monsters());
            list.addAll(Superiors.build(list));
            monsters = Collections.unmodifiableList(list);
        }
        return monsters;
    }

    @Override
    public List<MonsterDef> monsters() {
        return allMonsters();
    }

    @Override
    public List<ModelSpec> models() {
        return SlayerModels.all();
    }

    @Override
    public List<ModelSpec.Variant> modelVariants() {
        List<ModelSpec.Variant> out = new ArrayList<>(SlayerTaskMonsters.variants());
        out.addAll(SlayerTaskMonsters2.variants());
        return out;
    }

    @Override
    public List<PlaceDef> places() {
        return List.of(
                // Konar quo Maten on the summit of Mount Karuulm (Kebos Lowlands) with the brimstone chest next to her
                // (OSRS Wiki: both at 1309, 3785; the Karuulm Slayer Dungeon's entrance is further north, at 1311, 3807).
                PlaceDef.of("mount_karuulm", "Mount Karuulm", KONAR_TILE[0], KONAR_TILE[1], PlaceDef.Style.CAMP,
                        List.of("brimstone_chest")),
                // The Wilderness pirates' hideout (level 53); Larran's big chest is on the ship west of it (OSRS Wiki:
                // 3018, 3955, on deck), built by the Minecraft side at LARRAN_CHEST_TILE.
                PlaceDef.of("pirates_hideout", "Pirates' Hideout", 3041, 3949, PlaceDef.Style.RUINS, List.of()),
                PlaceDef.of(LARRAN_SHIP, "Larran's ship", LARRAN_CHEST_TILE[0], LARRAN_CHEST_TILE[1], PlaceDef.Style.NONE,
                        List.of()),
                // Vannaka stands in the Edgeville Dungeon (OSRS 3147, 9914), east of the Wilderness gate: his spot is
                // the surface tile above it (cache y - 6400), the dungeons module carves his room.
                PlaceDef.of(VANNAKA_POST, "Vannaka's post", VANNAKA_TILE[0], VANNAKA_TILE[1] - 6400, PlaceDef.Style.NONE,
                        List.of()),
                // Chaeldar lives in Zanaris; the shed in Lumbridge Swamp leads there.
                PlaceDef.of("zanaris", "Zanaris", 3202, 3169, PlaceDef.Style.CAMP, List.of("fairy_ring_shed")));
    }

    @Override
    public List<BlockDef> blocks() {
        return List.of(
                BlockDef.scripted("brimstone_chest", "Brimstone chest", 0x8A3A1A, "chest"),
                BlockDef.scripted("larrans_big_chest", "Larran's big chest", 0x6A4A2A, "chest"),
                BlockDef.decor("fairy_ring_shed", "Zanaris shed door", 0x6A5A3A, "crate"));
    }

    @Override
    public List<NpcDef> npcs() {
        return List.of(
                // Offsets are blocks from the place (2 per OSRS tile; +dz is south). Wiki spots: Turael 2931, 3536.
                NpcDef.of("turael", "Turael", "BURTHORPE", 64, 16, 0xC8A080, 0x3A5A8A, 0x3A3A4A, 0x6A4A2A),
                NpcDef.of("mazchna", "Mazchna", "CANIFIS", 30, -42, // north-east of the square (OSRS 3511, 3509)
                        0xB09070, 0x4A3A2A, 0x3A2A2A, 0x2A2A2A),
                // In the Edgeville Dungeon (OSRS 3147, 9914): NpcStoreys puts his feet on the dungeon's floor.
                NpcDef.of("vannaka", "Vannaka", VANNAKA_POST, 2, -8, 0xC8A080, 0x6A6A70, 0x4A4A50, 0x8A8A8A),
                // By the Fairy Queen's throne in Zanaris (OSRS 2444, 4431; the islands module's Zanaris at 2412, 4434).
                NpcDef.of("chaeldar", "Chaeldar", "isl_zanaris", 64, 6, 0xA0C0A0, 0x6A9A5A, 0x4A6A3A, 0xD0E0B0),
                // By the yews south of the Stronghold's bank (OSRS 2432, 3423).
                NpcDef.of("nieve", "Nieve", "GNOME_STRONGHOLD", -58, 42, 0xD8B898, 0x3A6A3A, 0x2A4A2A, 0xB08040),
                // Steve takes over from Nieve after Monkey Madness II and stands in the same spot.
                NpcDef.of("steve", "Steve", "GNOME_STRONGHOLD", -56, 42, 0xC8A080, 0x3A5A2A, 0x2A3A1A, 0x6A4A2A),
                NpcDef.of("duradel", "Duradel", "SHILO_VILLAGE", 34, -54, // in his hut (OSRS 2869, 2982)
                        0x6A4A3A, 0x8A2A2A, 0x5A1A1A, 0x202020),
                NpcDef.of("konar", "Konar quo Maten", "mount_karuulm", 6, -6, 0x8AA080, 0x6A4A6A, 0x4A2A4A, 0xC0A040),
                // By the Edgeville jailhouse, north-east of the bank (OSRS 3109, 3514).
                NpcDef.of("krystilia", "Krystilia", "EDGEVILLE", 32, -42, 0xE0C0A0, 0x2A2A2A, 0x1A1A1A, 0xC03030));
    }

    @Override
    public List<CollectionLogData.Page> collectionLog() {
        Set<String> slayer = new LinkedHashSet<>();
        for (MonsterDef m : allMonsters()) {
            if (Superiors.byId(m.id) != null || m.id.equals(TormentedDemons.ID)
                    || m.id.equals(DemonicGorillas.DEMONIC) || m.id.equals(DemonicGorillas.TORTURED)) { // own page
                continue;
            }
            for (Drop d : m.drops) {
                if (d.rarity() >= 300 && !d.item().startsWith("#") && !d.item().startsWith("clue_scroll")) {
                    slayer.add(d.item());
                }
            }
        }
        return List.of(
                new CollectionLogData.Page("Other", "Slayer", new ArrayList<>(slayer)),
                TormentedDemons.collectionLog(),
                DemonicGorillas.collectionLog(),
                new CollectionLogData.Page("Other", "Superior slayer creatures",
                        List.of("imbued_heart", "eternal_gem", "dust_battlestaff", "mist_battlestaff")),
                new CollectionLogData.Page("Other", "Dragons", List.of("draconic_visage", "dragon_limbs", "dragon_metal_slice",
                        "dragon_metal_lump", "lava_scale", "dragon_full_helm", "wyvern_visage")),
                new CollectionLogData.Page("Other", "Brimstone & Larran's chests", List.of("brimstone_key", "larran_key",
                        "dragon_harpoon", "dragon_sword")));
    }
}
