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
 * ({@link WaterbirthDungeon}) and Ape Atoll's undead dungeons ({@link ApeAtollDungeons}).
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
        // Holds the Waterbirth Island Dungeon's circular doors open (OSRS: from Askeladden in Rellekka; here from the
        // dungeon's rock pile, approx).
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

    @Override
    public List<StructureDef> structures() {
        return Dungeons.structures();
    }
}
