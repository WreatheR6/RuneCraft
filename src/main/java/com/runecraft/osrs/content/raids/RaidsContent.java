package com.runecraft.osrs.content.raids;

import com.runecraft.osrs.CollectionLogData;
import com.runecraft.osrs.Station;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentModule;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.model.ModelSpec;

import java.util.ArrayList;
import java.util.List;

/** Chambers of Xeric, Theatre of Blood and Tombs of Amascut. */
public final class RaidsContent implements ContentModule {
    /** Scripted blocks used in the raid instances (besides the three entrances). */
    public static final String START_BARRIER = "raid_start_barrier";
    public static final String REWARD_CHEST = "raid_reward_chest";
    public static final String EXIT = "raid_exit";
    public static final String SUPPLIES = "raid_supply_chest";
    public static final String INVOCATION_OBELISK = "toa_invocation_obelisk";
    public static final String GRUBBY_CHEST = "cox_grubby_chest";
    public static final String TROUGH = "cox_scavenger_trough";
    public static final String SAPLING = "cox_sapling";
    public static final String BRAZIER = "cox_brazier";
    public static final String TIGHTROPE = "cox_tightrope";
    public static final String MEAT_TREE = "cox_meat_tree";
    public static final String SCABARAS_TILE = "scabaras_tile";
    public static final String HET_SEAL = "het_seal";
    public static final String APMEKEN_PILLAR = "apmeken_pillar";
    public static final String APMEKEN_VENT = "apmeken_vent";
    public static final String APMEKEN_ROOF = "apmeken_roof_support";
    public static final String CRONDIS_WATER = "crondis_water_container_block";
    public static final String CRONDIS_PALM = "crondis_palm";
    public static final String HERB_PATCH = "cox_herb_patch";
    public static final String FISHING_POOL = "cox_fishing_pool";
    public static final String BAT_ROOST = "cox_bat_roost";
    public static final String COOKING_FIRE = "cox_cooking_fire";
    public static final String GOURD_TREE = "cox_gourd_tree";
    public static final String GEYSER = "cox_geyser";
    /** Tombs of Amascut: the Nexus's path entrances and the way down to the Wardens (which one is told by position). */
    public static final String TOA_PATH_ENTRANCE = "toa_path_entrance";
    /** Tombs of Amascut: the teleport crystal a path boss leaves behind, back to the Nexus. */
    public static final String TOA_EXIT_CRYSTAL = "toa_teleport_crystal";
    public static final String TOA_HELPFUL_SPIRIT = "toa_helpful_spirit";
    /** Path of Het: the statues that lend a pickaxe, the turnable mirrors and the worn barriers a pickaxe breaks. */
    public static final String TOA_PICKAXE_STATUE = "toa_pickaxe_statue";
    public static final String TOA_MIRROR = "toa_mirror";
    public static final String TOA_WORN_BARRIER = "toa_worn_barrier";
    /** Path of Apmeken: the crate of hammers and neutralising potions. */
    public static final String TOA_APMEKEN_CRATE = "toa_apmeken_crate";
    /** Path of Scabaras: the button that replays the tile sequence and the tablet of the addition puzzle. */
    public static final String TOA_SCABARAS_PANEL = "toa_scabaras_panel";
    /** Zebak: the water jugs that wash away his acid. */
    public static final String TOA_ZEBAK_JUG = "toa_zebak_jug";

    @Override
    public String id() {
        return "raids";
    }

    @Override
    public void items(ItemSink items) {
        RaidItems.add(items);
    }

    @Override
    public List<BossDef> bosses() {
        List<BossDef> list = new ArrayList<>();
        list.addAll(CoxBosses.bosses());
        list.addAll(TobBosses.bosses());
        list.addAll(ToaBosses.bosses());
        return list;
    }

    @Override
    public List<MonsterDef> monsters() {
        List<MonsterDef> list = new ArrayList<>();
        list.addAll(CoxBosses.monsters());
        list.addAll(TobBosses.monsters());
        list.addAll(ToaBosses.monsters());
        return list;
    }

    @Override
    public List<BlockDef> blocks() {
        return List.of(
                BlockDef.scripted(RaidType.COX.entranceBlock(), "Chambers of Xeric", 0x3A3A4A, "portal_glow"),
                BlockDef.scripted(RaidType.TOB.entranceBlock(), "Theatre of Blood", 0x7A1A1A, "portal_glow"),
                BlockDef.scripted(RaidType.TOA.entranceBlock(), "Tombs of Amascut", 0xD8B060, "portal_glow"),
                BlockDef.scripted(START_BARRIER, "Barrier", 0x6A2AC0, "portal_glow"),
                BlockDef.scripted(REWARD_CHEST, "Reward chest", 0x8A6A3A, "chest"),
                BlockDef.scripted(EXIT, "Exit", 0x4A8AC0, "portal_glow"),
                BlockDef.scripted(SUPPLIES, "Storage unit", 0x6A5A3A, "crate"),
                BlockDef.scripted(INVOCATION_OBELISK, "Invocation obelisk", 0xD8B060, "obelisk"),
                BlockDef.scripted(GRUBBY_CHEST, "Grubby chest", 0x5A4A2A, "chest"),
                BlockDef.scripted(TROUGH, "Scavenger's trough", 0x6A5A4A, "crate"),
                BlockDef.scripted(SAPLING, "Sapling", 0x4A7A2A, "sapling"),
                BlockDef.scripted(BRAZIER, "Brazier", 0x5A4A3A, "brazier"),
                BlockDef.scripted(TIGHTROPE, "Tightrope", 0xA08A5A, "rope"),
                BlockDef.scripted(MEAT_TREE, "Meat tree", 0x8A3A3A, "meat_tree"),
                BlockDef.scripted(SCABARAS_TILE, "Scabaras tile", 0xC8A050, "pressure_plate"),
                BlockDef.scripted(HET_SEAL, "Het's seal", 0xC0A060, "statue"),
                BlockDef.scripted(APMEKEN_PILLAR, "Pillar", 0xA08A60, "pillar"),
                BlockDef.scripted(APMEKEN_VENT, "Vent", 0x6A5A4A, "vent"),
                BlockDef.scripted(APMEKEN_ROOF, "Roof support", 0x8A6A4A, "crate"),
                BlockDef.scripted(CRONDIS_WATER, "Water container", 0x3A7AC0, "water_container"),
                BlockDef.scripted(CRONDIS_PALM, "Palm of Resourcefulness", 0x5A8A3A, "palm"),
                BlockDef.scripted(HERB_PATCH, "Herb patch", 0x5A4A2A, "plant"),
                BlockDef.scripted(FISHING_POOL, "Fishing spot", 0x2A5A8A, "pool"),
                BlockDef.scripted(BAT_ROOST, "Bat roost", 0x3A3A3A, "rock"),
                BlockDef.scripted(COOKING_FIRE, "Fire", 0x8A4A1A, "fire"),
                BlockDef.scripted(GOURD_TREE, "Gourd tree", 0x6A8A3A, "tree"),
                BlockDef.scripted(GEYSER, "Geyser", 0x4A7AA0, "water"),
                BlockDef.scripted(TOA_PATH_ENTRANCE, "Path entrance", 0xD8B060, "portal_glow"),
                BlockDef.scripted(TOA_EXIT_CRYSTAL, "Teleport crystal", 0xE8F0F8, "crystal_glow"),
                BlockDef.scripted(TOA_HELPFUL_SPIRIT, "Helpful Spirit", 0x9AD8E8, "spirit_glow"),
                BlockDef.scripted(TOA_PICKAXE_STATUE, "Statue", 0xC0A060, "statue"),
                BlockDef.scripted(TOA_MIRROR, "Mirror", 0xC8D0D8, "mirror"),
                BlockDef.scripted(TOA_WORN_BARRIER, "Barrier", 0xB09060, "rock"),
                BlockDef.scripted(TOA_APMEKEN_CRATE, "Crate", 0x8A6A4A, "crate"),
                BlockDef.scripted(TOA_SCABARAS_PANEL, "Hieroglyph tablet", 0xC8A050, "tablet"),
                BlockDef.scripted(TOA_ZEBAK_JUG, "Jug", 0xA06A3A, "pot"));
    }

    @Override
    public List<PlaceDef> places() {
        List<PlaceDef> list = new ArrayList<>();
        list.add(new PlaceDef(RaidType.COX.placeId(), RaidType.COX.placeName(), RaidType.COX.osrsX(), RaidType.COX.osrsY(),
                PlaceDef.Style.CAVE, List.of(Station.BANK_BOOTH), List.of(RaidType.COX.entranceBlock())));
        list.add(new PlaceDef(RaidType.TOB.placeId(), RaidType.TOB.placeName(), RaidType.TOB.osrsX(), RaidType.TOB.osrsY(),
                PlaceDef.Style.TOWN, List.of(Station.BANK_BOOTH), List.of(RaidType.TOB.entranceBlock())));
        list.add(new PlaceDef(RaidType.TOA.placeId(), RaidType.TOA.placeName(), RaidType.TOA.osrsX(), RaidType.TOA.osrsY(),
                PlaceDef.Style.ARENA, List.of(Station.BANK_BOOTH), List.of(RaidType.TOA.entranceBlock())));
        return list;
    }

    @Override
    public List<ModelSpec> models() {
        List<ModelSpec> list = new ArrayList<>();
        list.addAll(CoxModels.models());
        list.addAll(TobModels.models());
        list.addAll(ToaModels.models());
        return list;
    }

    @Override
    public List<CollectionLogData.Page> collectionLog() {
        List<CollectionLogData.Page> pages = new ArrayList<>();
        for (RaidType t : RaidType.values()) {
            pages.add(new CollectionLogData.Page("Raids", t.displayName(), RaidItems.collectionLog(t)));
        }
        return pages;
    }
}
