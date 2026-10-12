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

    @Override
    public String id() {
        return "slayer";
    }

    @Override
    public void items(ItemSink items) {
        SlayerItems.add(items);
        TormentedDemons.items(items);
    }

    @Override
    public void recipes(Consumer<Recipe> recipes) {
        recipes.accept(Recipe.builder("broad_arrows", MenuType.FLETCHING, "Broad arrows").skill(Skill.FLETCHING, 52, 150)
                .in("headless_arrow", 15).in("broad_arrowheads", 15).out("broad_arrows", 15).ticks(2).build());
        TormentedDemons.recipes(recipes);
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
            list.add(TormentedDemons.monster());
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
    public List<PlaceDef> places() {
        return List.of(
                // Konar quo Maten at the summit of Mount Karuulm (Zeah) with the brimstone chest.
                PlaceDef.of("mount_karuulm", "Mount Karuulm", 1311, 3807, PlaceDef.Style.CAMP, List.of("brimstone_chest")),
                // Larran's big chest in the Wilderness pirates' hideout (level 53).
                PlaceDef.of("pirates_hideout", "Pirates' Hideout", 3041, 3949, PlaceDef.Style.RUINS, List.of("larrans_big_chest")),
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
                NpcDef.of("turael", "Turael", "BURTHORPE", 4, 3, 0xC8A080, 0x3A5A8A, 0x3A3A4A, 0x6A4A2A),
                NpcDef.of("mazchna", "Mazchna", "CANIFIS", -4, 3, 0xB09070, 0x4A3A2A, 0x3A2A2A, 0x2A2A2A),
                NpcDef.of("vannaka", "Vannaka", "EDGEVILLE", 5, -4, 0xC8A080, 0x6A6A70, 0x4A4A50, 0x8A8A8A),
                NpcDef.of("chaeldar", "Chaeldar", "zanaris", 3, 2, 0xA0C0A0, 0x6A9A5A, 0x4A6A3A, 0xD0E0B0),
                NpcDef.of("nieve", "Nieve", "GNOME_STRONGHOLD", 4, -3, 0xD8B898, 0x3A6A3A, 0x2A4A2A, 0xB08040),
                // Steve takes over from Nieve after Monkey Madness II; both stand at the Stronghold Slayer Cave entrance.
                NpcDef.of("steve", "Steve", "GNOME_STRONGHOLD", 6, -3, 0xC8A080, 0x3A5A2A, 0x2A3A1A, 0x6A4A2A),
                NpcDef.of("duradel", "Duradel", "SHILO_VILLAGE", 3, 4, 0x6A4A3A, 0x8A2A2A, 0x5A1A1A, 0x202020),
                NpcDef.of("konar", "Konar quo Maten", "mount_karuulm", 2, 2, 0x8AA080, 0x6A4A6A, 0x4A2A4A, 0xC0A040),
                NpcDef.of("krystilia", "Krystilia", "EDGEVILLE", -6, 4, 0xE0C0A0, 0x2A2A2A, 0x1A1A1A, 0xC03030));
    }

    @Override
    public List<CollectionLogData.Page> collectionLog() {
        Set<String> slayer = new LinkedHashSet<>();
        for (MonsterDef m : allMonsters()) {
            if (Superiors.byId(m.id) != null || m.id.equals(TormentedDemons.ID)) { // own page
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
                new CollectionLogData.Page("Other", "Superior slayer creatures",
                        List.of("imbued_heart", "eternal_gem", "dust_battlestaff", "mist_battlestaff")),
                new CollectionLogData.Page("Other", "Dragons", List.of("draconic_visage", "dragon_limbs", "dragon_metal_slice",
                        "dragon_metal_lump", "lava_scale", "dragon_full_helm", "wyvern_visage")),
                new CollectionLogData.Page("Other", "Brimstone & Larran's chests", List.of("brimstone_key", "larran_key",
                        "dragon_harpoon", "dragon_sword")));
    }
}
