package com.runecraft.registry;

import com.runecraft.RuneCraft;
import com.runecraft.block.AgilityObstacleBlock;
import com.runecraft.block.AllotmentPatchBlock;
import com.runecraft.block.BossSummonBlock;
import com.runecraft.osrs.data.Boss;
import com.runecraft.block.FishingSpotBlock;
import com.runecraft.block.HerbPatchBlock;
import com.runecraft.block.OsrsFireBlock;
import com.runecraft.block.RockBlock;
import com.runecraft.block.RuneAltarBlock;
import com.runecraft.block.StallBlock;
import com.runecraft.block.StationBlock;
import com.runecraft.block.TrapBlock;
import com.runecraft.block.TreeLogBlock;
import com.runecraft.osrs.Station;
import com.runecraft.osrs.data.FishingSpot;
import com.runecraft.osrs.data.HunterCreature;
import com.runecraft.osrs.data.Rock;
import com.runecraft.osrs.data.RuneAltar;
import com.runecraft.osrs.data.Thieving;
import com.runecraft.osrs.data.Tree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/** All RuneCraft blocks. Resource nodes are unbreakable in survival, like the game world. */
public final class ModBlocks {
    private static final List<Block> ALL = new ArrayList<>();
    private static final List<BlockItem> ITEMS = new ArrayList<>();
    public static final Map<Tree, TreeLogBlock> TREES = new EnumMap<>(Tree.class);
    public static final Map<Rock, RockBlock> ROCKS = new EnumMap<>(Rock.class);
    public static final Map<FishingSpot, FishingSpotBlock> FISHING_SPOTS = new EnumMap<>(FishingSpot.class);
    public static final Map<RuneAltar, RuneAltarBlock> ALTARS = new EnumMap<>(RuneAltar.class);
    public static final Map<Thieving.Stall, StallBlock> STALLS = new EnumMap<>(Thieving.Stall.class);
    public static final Map<Station, Block> STATIONS = new LinkedHashMap<>();
    public static final Map<Boss, BossSummonBlock> BOSS_STONES = new EnumMap<>(Boss.class);

    public static final Map<String, com.runecraft.block.ModuleBlock> MODULE_BLOCKS = new LinkedHashMap<>();
    public static final Map<String, com.runecraft.block.DataBossSummonBlock> DATA_BOSS_STONES = new LinkedHashMap<>();
    public static AgilityObstacleBlock AGILITY_OBSTACLE;
    public static HerbPatchBlock HERB_PATCH;
    public static AllotmentPatchBlock ALLOTMENT_PATCH;
    public static OsrsFireBlock FIRE;
    public static TrapBlock BIRD_SNARE;
    public static TrapBlock BOX_TRAP;

    private ModBlocks() {
    }

    private static BlockBehaviour.Properties unbreakable(MapColor color, SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(color).sound(sound).strength(-1.0f, 3600000.0f).noLootTable()
                .pushReaction(PushReaction.BLOCK);
    }

    private static <T extends Block> T register(String id, T block, boolean withItem) {
        Registry.register(BuiltInRegistries.BLOCK, RuneCraft.id(id), block);
        ALL.add(block);
        if (withItem) {
            BlockItem item = Registry.register(BuiltInRegistries.ITEM, RuneCraft.id(id), new BlockItem(block, new Item.Properties()));
            ITEMS.add(item);
        }
        return block;
    }

    public static void register() {
        for (Tree t : Tree.values()) {
            TREES.put(t, register(t.blockId(), new TreeLogBlock(t, unbreakable(MapColor.COLOR_BROWN, SoundType.WOOD)), true));
        }
        for (Rock r : Rock.values()) {
            ROCKS.put(r, register(r.blockId(), new RockBlock(r, unbreakable(MapColor.STONE, SoundType.STONE)), true));
        }
        for (FishingSpot s : FishingSpot.values()) {
            FISHING_SPOTS.put(s, register(s.blockId(), new FishingSpotBlock(s,
                    unbreakable(MapColor.WATER, SoundType.WET_GRASS).noCollission().noOcclusion()), true));
        }
        for (RuneAltar a : RuneAltar.values()) {
            ALTARS.put(a, register(a.blockId(), new RuneAltarBlock(a,
                    unbreakable(MapColor.STONE, SoundType.STONE).lightLevel(s -> 7).noOcclusion()), true));
        }
        for (Thieving.Stall s : Thieving.Stall.values()) {
            STALLS.put(s, register(s.blockId(), new StallBlock(s, unbreakable(MapColor.WOOD, SoundType.WOOD).noOcclusion()), true));
        }
        for (Station s : Station.values()) {
            if (s == Station.HERB_PATCH || s == Station.ALLOTMENT_PATCH) {
                continue;
            }
            Thieving.Stall stall = Thieving.Stall.byBlock(s.blockId());
            if (stall != null) {
                STATIONS.put(s, STALLS.get(stall));
                continue;
            }
            BlockBehaviour.Properties settings = BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .sound(s == Station.ANVIL ? SoundType.ANVIL : SoundType.STONE).strength(3.0f, 1200.0f)
                    .requiresCorrectToolForDrops();
            if (s == Station.BANK_BOOTH || s == Station.FURNACE || s == Station.ANVIL || s == Station.RANGE
                    || s == Station.PRAYER_ALTAR) {
                settings = settings.noOcclusion(); // shaped OSRS models (see tools/gen_assets.py station_shape)
            }
            if (s == Station.FURNACE || s == Station.RANGE || s == Station.PORTAL || s == Station.PRAYER_ALTAR) {
                settings = settings.lightLevel(st -> 10);
            }
            STATIONS.put(s, register(s.blockId(), new StationBlock(s, settings), true));
        }
        for (Boss b : Boss.values()) {
            BOSS_STONES.put(b, register(b.summonBlockId(), new BossSummonBlock(b,
                    unbreakable(MapColor.COLOR_BLACK, SoundType.DEEPSLATE).lightLevel(st -> 6)), true));
        }
        for (com.runecraft.osrs.content.BlockDef def : com.runecraft.osrs.content.ContentRegistry.blocks()) {
            BlockBehaviour.Properties settings = def.breakable()
                    ? BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.5f, 1200.0f).sound(SoundType.STONE)
                    : unbreakable(MapColor.STONE, SoundType.STONE);
            if (def.look() != null && (def.look().contains("glow") || def.look().contains("fire") || def.look().contains("portal"))) {
                settings = settings.lightLevel(st -> 10);
            }
            MODULE_BLOCKS.put(def.id(), register(def.id(), new com.runecraft.block.ModuleBlock(def, settings), true));
        }
        for (com.runecraft.osrs.content.BossDef b : com.runecraft.osrs.content.ContentRegistry.bosses()) {
            if (!b.raidOnly) {
                DATA_BOSS_STONES.put(b.id, register(b.summonBlockId(), new com.runecraft.block.DataBossSummonBlock(b,
                        unbreakable(MapColor.COLOR_BLACK, SoundType.DEEPSLATE).lightLevel(st -> 6)), true));
            }
        }
        AGILITY_OBSTACLE = register("agility_obstacle", new AgilityObstacleBlock(
                unbreakable(MapColor.WOOD, SoundType.WOOD)), true);
        HERB_PATCH = register("herb_patch", new HerbPatchBlock(
                unbreakable(MapColor.DIRT, SoundType.ROOTED_DIRT).randomTicks().noOcclusion()), true);
        ALLOTMENT_PATCH = register("allotment_patch", new AllotmentPatchBlock(
                unbreakable(MapColor.DIRT, SoundType.ROOTED_DIRT).randomTicks().noOcclusion()), true);
        STATIONS.put(Station.HERB_PATCH, HERB_PATCH);
        STATIONS.put(Station.ALLOTMENT_PATCH, ALLOTMENT_PATCH);
        FIRE = register("osrs_fire", new OsrsFireBlock(BlockBehaviour.Properties.of().mapColor(MapColor.FIRE)
                .noCollission().instabreak().lightLevel(s -> 15).sound(SoundType.WOOL).noLootTable()
                .pushReaction(PushReaction.DESTROY)), false);
        BIRD_SNARE = register("bird_snare_trap", new TrapBlock(HunterCreature.Trap.BIRD_SNARE, BlockBehaviour.Properties.of()
                .noCollission().noOcclusion().instabreak().noLootTable().sound(SoundType.GRASS)), false);
        BOX_TRAP = register("box_trap_trap", new TrapBlock(HunterCreature.Trap.BOX_TRAP, BlockBehaviour.Properties.of()
                .noCollission().noOcclusion().instabreak().noLootTable().sound(SoundType.WOOD)), false);
    }

    public static List<Block> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static List<BlockItem> items() {
        return Collections.unmodifiableList(ITEMS);
    }

    public static int count() {
        return ALL.size();
    }
}
