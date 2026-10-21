package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.CollectionLogData;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentModule;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.model.ModelSpec;

import java.util.List;

/**
 * Minigames: TzHaar Fight Cave and Inferno, Pest Control, Wintertodt, Tempoross, Guardians of the
 * Rift, Barbarian Assault, Castle Wars, Warriors' Guild, Mage Training Arena, Pyramid Plunder,
 * Hallowed Sepulchre, Brimhaven Agility Arena, Gnome Restaurant, Trouble Brewing, Shades of
 * Mort'ton, Tithe Farm, Giants' Foundry, Mahogany Homes, Fishing Trawler, Rogues' Den, TzHaar-Ket-Rak's Challenges,
 * Temple Trekking, Last Man Standing and the Gauntlet.
 */
public final class MinigamesContent implements ContentModule {
    @Override
    public String id() {
        return "minigames";
    }

    @Override
    public void items(ItemSink items) {
        MinigameItems.add(items);
        LastManStanding.items(items);
        EmirsArena.items(items);
        TaiBwoWannai.items(items);
        VarrockMuseum.items(items);
        Gauntlet.items(items);
    }

    @Override
    public List<MonsterDef> monsters() {
        List<MonsterDef> list = new java.util.ArrayList<>(MinigameMonsters.monsters());
        list.addAll(Gauntlet.monsters());
        list.addAll(FortisColosseum.monsters());
        return list;
    }

    @Override
    public List<BossDef> bosses() {
        List<BossDef> list = new java.util.ArrayList<>(MinigameMonsters.bosses());
        list.addAll(ChampionsChallenge.bosses());
        return list;
    }

    @Override
    public List<PlaceDef> places() {
        return MinigameWorld.places();
    }

    @Override
    public List<NpcDef> npcs() {
        return MinigameWorld.npcs();
    }

    @Override
    public List<BlockDef> blocks() {
        return MinigameWorld.blocks();
    }

    @Override
    public List<ModelSpec> models() {
        return MinigameModels.models();
    }

    @Override
    public List<ModelSpec.Variant> modelVariants() {
        List<ModelSpec.Variant> list = new java.util.ArrayList<>(Gauntlet.modelVariants());
        // Barbarian Assault's queen spawns look like small Penance Queens.
        list.add(ModelSpec.Variant.of("queen_spawn", "penance_queen", 0.35f));
        // Nightmare Zone: the dream bosses added in round 40 are copies of their quest bosses (the first twelve live in the
        // bestiary's look-alikes); the Headless Beast and the Elven traitor have no quest creature and keep their own looks.
        String[][] nmz = {{"nmz_trapped_soul", "ghost"}, {"nmz_corsair_traitor", "pirate"}, {"nmz_sand_snake", "sea_snake"},
                {"nmz_corrupt_lizardman", "corrupt_lizardman"}, {"nmz_witchs_experiment", "witchs_experiment"},
                {"nmz_witchs_experiment_spider", "giant_spider"}, {"nmz_witchs_experiment_bear", "grizzly_bear"},
                {"nmz_witchs_experiment_wolf", "wolf"}, {"nmz_moss_guardian", "moss_guardian"}, {"nmz_slagilith", "quest_slagilith"},
                {"nmz_nazastarool", "nazastarool"}, {"nmz_nazastarool_skeleton", "nazastarool"}, {"nmz_nazastarool_ghost", "nazastarool"},
                {"nmz_treus_dayth", "treus_dayth"}, {"nmz_skeleton_hellhound", "skeleton_hellhound"},
                {"nmz_dagannoth_mother", "dagannoth_mother"}, {"nmz_agrith_naar", "agrith_naar"}, {"nmz_tanglefoot", "tanglefoot"},
                {"nmz_ice_troll_king", "ice_troll_king"}, {"nmz_evil_chicken", "rfd_evil_chicken"}, {"nmz_agrith_na_na", "agrith_na_na"},
                {"nmz_flambeed", "flambeed"}, {"nmz_karamel", "karamel"}, {"nmz_dessourt", "dessourt"},
                {"nmz_gelatinnoth_mother", "gelatinnoth_mother"}, {"nmz_culinaromancer", "culinaromancer"},
                {"nmz_chronozon", "chronozon"}, {"nmz_black_demon", "glough_black_demon"}, {"nmz_giant_roc", "quest_giant_roc"},
                {"nmz_dessous", "dessous"}, {"nmz_damis", "damis"}, {"nmz_damis_second", "damis"}, {"nmz_fareed", "fareed"},
                {"nmz_kamil", "kamil"}, {"nmz_nezikchened", "nezikchened"}, {"nmz_barrelchest", "quest_barrelchest"},
                {"nmz_giant_scarab", "giant_scarab"}, {"nmz_jungle_demon", "jungle_demon"},
                {"nmz_essyllt", "sote_essyllt"}, {"nmz_the_untouchable", "the_untouchable"},
                {"nmz_the_everlasting", "the_everlasting"}, {"nmz_the_inadequacy", "the_inadequacy"}};
        for (String[] n : nmz) {
            list.add(ModelSpec.Variant.of(n[0], n[1]));
        }
        return list;
    }

    @Override
    public List<com.runecraft.osrs.world.StructureDef> structures() {
        return List.of(GauntletLobby.structure(), ColosseumLobby.structure());
    }

    @Override
    public List<CollectionLogData.Page> collectionLog() {
        return MinigameWorld.collectionLog();
    }
}
