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
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private static AbstractBlock.Settings unbreakable(MapColor color, BlockSoundGroup sound) {
        return AbstractBlock.Settings.create().mapColor(color).sounds(sound).strength(-1.0f, 3600000.0f).dropsNothing()
                .pistonBehavior(PistonBehavior.BLOCK);
    }

    private static <T extends Block> T register(String id, T block, boolean withItem) {
        Registry.register(Registries.BLOCK, RuneCraft.id(id), block);
        ALL.add(block);
        if (withItem) {
            BlockItem item = Registry.register(Registries.ITEM, RuneCraft.id(id), new BlockItem(block, new Item.Settings()));
            ITEMS.add(item);
        }
        return block;
    }

    public static void register() {
        for (Tree t : Tree.values()) {
            TREES.put(t, register(t.blockId(), new TreeLogBlock(t, unbreakable(MapColor.BROWN, BlockSoundGroup.WOOD)), true));
        }
        for (Rock r : Rock.values()) {
            ROCKS.put(r, register(r.blockId(), new RockBlock(r, unbreakable(MapColor.STONE_GRAY, BlockSoundGroup.STONE)), true));
        }
        for (FishingSpot s : FishingSpot.values()) {
            FISHING_SPOTS.put(s, register(s.blockId(), new FishingSpotBlock(s,
                    unbreakable(MapColor.WATER_BLUE, BlockSoundGroup.WET_GRASS).noCollision().nonOpaque()), true));
        }
        for (RuneAltar a : RuneAltar.values()) {
            ALTARS.put(a, register(a.blockId(), new RuneAltarBlock(a,
                    unbreakable(MapColor.STONE_GRAY, BlockSoundGroup.STONE).luminance(s -> 7)), true));
        }
        for (Thieving.Stall s : Thieving.Stall.values()) {
            STALLS.put(s, register(s.blockId(), new StallBlock(s, unbreakable(MapColor.OAK_TAN, BlockSoundGroup.WOOD)), true));
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
            AbstractBlock.Settings settings = AbstractBlock.Settings.create().mapColor(MapColor.STONE_GRAY)
                    .sounds(s == Station.ANVIL ? BlockSoundGroup.ANVIL : BlockSoundGroup.STONE).strength(3.0f, 1200.0f)
                    .requiresTool();
            if (s == Station.FURNACE || s == Station.RANGE || s == Station.PORTAL || s == Station.PRAYER_ALTAR) {
                settings = settings.luminance(st -> 10);
            }
            STATIONS.put(s, register(s.blockId(), new StationBlock(s, settings), true));
        }
        for (Boss b : Boss.values()) {
            BOSS_STONES.put(b, register(b.summonBlockId(), new BossSummonBlock(b,
                    unbreakable(MapColor.BLACK, BlockSoundGroup.DEEPSLATE).luminance(st -> 6)), true));
        }
        for (com.runecraft.osrs.content.BlockDef def : com.runecraft.osrs.content.ContentRegistry.blocks()) {
            AbstractBlock.Settings settings = def.breakable()
                    ? AbstractBlock.Settings.create().mapColor(MapColor.STONE_GRAY).strength(2.5f, 1200.0f).sounds(BlockSoundGroup.STONE)
                    : unbreakable(MapColor.STONE_GRAY, BlockSoundGroup.STONE);
            if (def.look() != null && (def.look().contains("glow") || def.look().contains("fire") || def.look().contains("portal"))) {
                settings = settings.luminance(st -> 10);
            }
            MODULE_BLOCKS.put(def.id(), register(def.id(), new com.runecraft.block.ModuleBlock(def, settings), true));
        }
        for (com.runecraft.osrs.content.BossDef b : com.runecraft.osrs.content.ContentRegistry.bosses()) {
            if (!b.raidOnly) {
                DATA_BOSS_STONES.put(b.id, register(b.summonBlockId(), new com.runecraft.block.DataBossSummonBlock(b,
                        unbreakable(MapColor.BLACK, BlockSoundGroup.DEEPSLATE).luminance(st -> 6)), true));
            }
        }
        AGILITY_OBSTACLE = register("agility_obstacle", new AgilityObstacleBlock(
                unbreakable(MapColor.OAK_TAN, BlockSoundGroup.WOOD)), true);
        HERB_PATCH = register("herb_patch", new HerbPatchBlock(
                unbreakable(MapColor.DIRT_BROWN, BlockSoundGroup.ROOTED_DIRT).ticksRandomly().nonOpaque()), true);
        ALLOTMENT_PATCH = register("allotment_patch", new AllotmentPatchBlock(
                unbreakable(MapColor.DIRT_BROWN, BlockSoundGroup.ROOTED_DIRT).ticksRandomly().nonOpaque()), true);
        STATIONS.put(Station.HERB_PATCH, HERB_PATCH);
        STATIONS.put(Station.ALLOTMENT_PATCH, ALLOTMENT_PATCH);
        FIRE = register("osrs_fire", new OsrsFireBlock(AbstractBlock.Settings.create().mapColor(MapColor.BRIGHT_RED)
                .noCollision().breakInstantly().luminance(s -> 15).sounds(BlockSoundGroup.WOOL).dropsNothing()
                .pistonBehavior(PistonBehavior.DESTROY)), false);
        BIRD_SNARE = register("bird_snare_trap", new TrapBlock(HunterCreature.Trap.BIRD_SNARE, AbstractBlock.Settings.create()
                .noCollision().nonOpaque().breakInstantly().dropsNothing().sounds(BlockSoundGroup.GRASS)), false);
        BOX_TRAP = register("box_trap_trap", new TrapBlock(HunterCreature.Trap.BOX_TRAP, AbstractBlock.Settings.create()
                .noCollision().nonOpaque().breakInstantly().dropsNothing().sounds(BlockSoundGroup.WOOD)), false);
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
