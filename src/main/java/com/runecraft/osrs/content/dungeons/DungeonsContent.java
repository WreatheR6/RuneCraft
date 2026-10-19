package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentDef.Slot;
import com.runecraft.osrs.EquipmentDef.WeaponClass;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ContentModule;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.world.StructureDef;

import java.util.List;
import java.util.Map;

/**
 * Gielinor's dungeons, carved underground beneath (or beside) their real surface entrances and
 * stocked with their monsters: Taverley, Brimhaven, Edgeville and Asgarnian Ice dungeons, the
 * Varrock Sewers, the Stronghold of Security (with its four reward chests), the Stronghold Slayer
 * Cave, the Slayer Tower basement, the Fremennik Slayer Dungeon, the Smoke Dungeon, the Kalphite
 * Lair, the Dwarven Mine with the Mining Guild and the Motherlode Mine, the Lumbridge Swamp Caves,
 * the caves below the Lumbridge Castle cellar leading to Dorgesh-Kaan, the Waterfall Dungeon, the
 * Ancient Cavern, the Barrows crypts and tunnels, the Mos Le'Harmless caves, the Lizardman Caves, the Karuulm Slayer Dungeon, the
 * Kraken Cove, the Smoke Devil Dungeon, the Iorwerth Dungeon, the Wyvern Cave, the Lithkren Vault,
 * the Chasm of Fire, the Abyss (with rifts to the rune altars), the TzHaar city, and the smaller lairs
 * of {@link DungeonsLairs}: the Haunted Mine with Tarn's Lair, the Mort'ton catacombs, the Brine Rat
 * Cavern, the Sourhog Cave, the Legends' Guild dungeon, the Wilderness God Wars Dungeon and
 * Jormungand's Prison, Puro-Puro (the impling maze below Zanaris, {@link PuroPuro}), the Waterbirth Island Dungeon
 * ({@link WaterbirthDungeon}), Ape Atoll's undead dungeons ({@link ApeAtollDungeons}), and the cache-tile dungeons of
 * {@link CacheDungeons}: the Shadow Dungeon, the Sophanem Dungeon, the Lighthouse dungeon, the Kalphite Cave, the
 * Wilderness Slayer Cave, the Forthos Dungeon, the Giants' Den, the Isle of Souls Dungeon, the Myths' Guild dungeon, the
 * Deep Wilderness Dungeon, the Karamja Volcano dungeon, the Ice Troll Caves and the Ourania Cave.
 */
public final class DungeonsContent implements ContentModule {
    public static final String FANCY_BOOTS = "fancy_boots";
    public static final String FIGHTING_BOOTS = "fighting_boots";
    public static final String PET_ROCK = "pet_rock";

    @Override
    public String id() {
        return "dungeons";
    }

    @Override
    public void items(ItemSink items) {
        // The Cradle of Life's rewards. OSRS: no combat bonuses (purely cosmetic) - approx.
        EquipmentStats none = new EquipmentStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        items.add(FANCY_BOOTS, "Fancy boots", Kind.ARMOUR, "boots", 0x7A3AA0).value(0).stack(1)
                .equipment(new EquipmentDef(Slot.FEET, none, Map.of(), 0, AttackType.CRUSH, WeaponClass.NONE))
                .examine("Stylish!");
        items.add(FIGHTING_BOOTS, "Fighting boots", Kind.ARMOUR, "boots", 0x8A2A2A).value(0).stack(1)
                .equipment(new EquipmentDef(Slot.FEET, none, Map.of(), 0, AttackType.CRUSH, WeaponClass.NONE))
                .examine("Ready for a fight.");
        // Holds the Waterbirth Island Dungeon's circular doors open: from Askeladden in Rellekka (OSRS).
        items.add(PET_ROCK, "Pet rock", Kind.SIMPLE, "ore", 0x8A8A82).value(0).stack(1)
                .examine("A very sensible pet.");
    }

    @Override
    public List<MonsterDef> monsters() {
        return DungeonMonsters.all();
    }

    @Override
    public List<BlockDef> blocks() {
        return DungeonBlocks.all();
    }

    /** Askeladden, the Fremennik youth in Rellekka who hands out pet rocks (wiki map 2658, 3660). */
    public static final String ASKELADDEN = "askeladden";
    public static final int ASKELADDEN_X = 2658;
    public static final int ASKELADDEN_Y = 3660;

    /**
     * Zooknock's cave at the end of the Ape Atoll Dungeon (the place is the surface tile above Zooknock's cache tile 2804,
     * 9145; he and his companions stand below it, {@code NpcStoreys.undergroundFeetY}), and Bardur on Waterbirth
     * sublevel 2 (cache 1850, 4392, plane 1), fighting dagannoth fledgelings.
     */
    public static final String ZOOKNOCK_CAVE = "zooknock_cave";
    public static final String BARDUR = "bardur";
    public static final String BARDUR_PLACE = "waterbirth_bardur";

    @Override
    public List<com.runecraft.osrs.content.PlaceDef> places() {
        int[] z = ApeAtollDungeons.ZOOKNOCK;
        int[] b = WaterbirthDungeon.bardurTile();
        return List.of(com.runecraft.osrs.content.PlaceDef.of(ASKELADDEN, "Askeladden", ASKELADDEN_X, ASKELADDEN_Y,
                        com.runecraft.osrs.content.PlaceDef.Style.NONE, List.of()),
                com.runecraft.osrs.content.PlaceDef.of(ZOOKNOCK_CAVE, "Zooknock's cave", z[0] + ApeAtollDungeons.DX, z[1] + ApeAtollDungeons.DY,
                        com.runecraft.osrs.content.PlaceDef.Style.NONE, List.of()),
                com.runecraft.osrs.content.PlaceDef.of(BARDUR_PLACE, "Bardur", b[0], b[1], com.runecraft.osrs.content.PlaceDef.Style.NONE,
                        List.of()));
    }

    @Override
    public List<com.runecraft.osrs.content.NpcDef> npcs() {
        int[] z = ApeAtollDungeons.ZOOKNOCK;
        int zx = z[0] + ApeAtollDungeons.DX;
        int zy = z[1] + ApeAtollDungeons.DY;
        int[] b = WaterbirthDungeon.bardurTile();
        return List.of(com.runecraft.osrs.content.NpcDef.of(ASKELADDEN, "Askeladden", ASKELADDEN, 0, 0, 0xE0B090, 0x8A5A2A, 0x5A3A2A,
                        0xE0C060),
                at("bunkwicket", "Bunkwicket", ZOOKNOCK_CAVE, zx, zy, 2803 + ApeAtollDungeons.DX, 9148 + ApeAtollDungeons.DY,
                        0x8A6A4A, 0x5A6A3A, 0x4A3A2A, 0x6A5A3A),
                at("waymottin", "Waymottin", ZOOKNOCK_CAVE, zx, zy, 2805 + ApeAtollDungeons.DX, 9147 + ApeAtollDungeons.DY,
                        0x8A6A4A, 0x6A5A3A, 0x4A3A2A, 0x5A6A3A),
                at(BARDUR, "Bardur", BARDUR_PLACE, b[0], b[1], b[0], b[1], 0xE0B090, 0x6A4A2A, 0x4A3A2A, 0xC0A060));
    }

    /** An NPC standing on tile (tx, ty) near a place centred on tile (px, py) (place centres snap to their chunk's centre). */
    static com.runecraft.osrs.content.NpcDef at(String id, String name, String place, int px, int py, int tx, int ty, int skin, int top,
                                               int bottom, int hat) {
        int cx = ((StructureDef.bx(px) >> 4) << 4) + 8;
        int cz = ((StructureDef.bz(py) >> 4) << 4) + 8;
        return com.runecraft.osrs.content.NpcDef.of(id, name, place, StructureDef.bx(tx) - cx, StructureDef.bz(ty) - cz, skin, top, bottom,
                hat);
    }

    @Override
    public List<StructureDef> structures() {
        List<StructureDef> out = new java.util.ArrayList<>(Dungeons.structures());
        out.add(EssenceMine.structure());
        return out;
    }
}
