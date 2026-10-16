package com.runecraft.osrs.content.npcs;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.content.ContentModule;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gielinor's ordinary population: shopkeepers with their OSRS stock ({@link Shops}), bankers at every
 * bank, travel NPCs ({@link Travel}), flavour characters ({@link Townsfolk}) and attackable townsfolk
 * with drop tables and pickpocketing ({@link TownMonsters}).
 *
 * <p>Every NPC stands at its real OSRS tile: NPCs are grouped by the 16x16 block chunk they stand in
 * and each chunk gets an invisible ({@link PlaceDef.Style#NONE}) place, so the NPC spawns when its
 * chunk is first generated, with dx/dz offsets from the chunk centre.
 */
public final class NpcsContent implements ContentModule {
    /** Place id of the chunk an OSRS tile falls in. */
    public static String placeId(int osrsX, int osrsY) {
        int cx = GielinorMap.toBlockX(osrsX) >> 4;
        int cz = GielinorMap.toBlockZ(osrsY) >> 4;
        return "npcs_spot_" + (cx < 0 ? "m" + -cx : String.valueOf(cx)) + "_" + (cz < 0 ? "m" + -cz : String.valueOf(cz));
    }

    @Override
    public String id() {
        return "npcs";
    }

    @Override
    public List<PlaceDef> places() {
        Map<String, PlaceDef> places = new LinkedHashMap<>();
        for (TownNpc n : Townsfolk.all()) {
            String id = placeId(n.x(), n.y());
            places.putIfAbsent(id, new PlaceDef(id, n.area(), n.x(), n.y(), PlaceDef.Style.NONE, List.of(), List.of()));
        }
        List<PlaceDef> out = new ArrayList<>(places.values());
        out.addAll(WildernessTransport.places());
        return out;
    }

    /** The Mage Bank cavern under the sea at its OSRS tile (the Mage Arena lever's destination). */
    @Override
    public List<com.runecraft.osrs.world.StructureDef> structures() {
        return List.of(WildernessTransport.mageBank());
    }

    @Override
    public List<NpcDef> npcs() {
        List<NpcDef> list = new ArrayList<>();
        Map<String, PlaceDef> byId = new LinkedHashMap<>();
        for (PlaceDef p : places()) {
            byId.put(p.id(), p);
        }
        for (TownNpc n : Townsfolk.all()) {
            PlaceDef p = byId.get(placeId(n.x(), n.y()));
            int dx = GielinorMap.toBlockX(n.x()) - p.blockX();
            int dz = GielinorMap.toBlockZ(n.y()) - p.blockZ();
            int[] c = n.colors();
            list.add(NpcDef.of(n.id(), n.name(), p.id(), dx, dz, c[0], c[1], c[2], c[3]));
        }
        return list;
    }

    @Override
    public List<MonsterDef> monsters() {
        return TownMonsters.monsters();
    }

    private static EquipmentDef wear(EquipmentDef.Slot slot, EquipmentStats stats) {
        return new EquipmentDef(slot, stats, Map.of(), 0, AttackType.CRUSH, EquipmentDef.WeaponClass.NONE);
    }

    private static EquipmentStats none() {
        return new EquipmentStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    @Override
    public void items(ItemSink s) {
        // Shop goods that no other module defines (OSRS values).
        s.add("magic_staff", "Magic staff", Kind.STAFF, "staff", 0x6A4A8A).value(200).stack(1)
                .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON, new EquipmentStats(2, -1, 10, 10, 0, 2, 3, 1, 10, 0, 7, 0, 0, 0),
                        Map.of(), 5, AttackType.CRUSH, EquipmentDef.WeaponClass.STAFF))
                .examine("A magical staff.");
        s.add("wooden_shield", "Wooden shield", Kind.SHIELD, "kiteshield", 0x8A6A3A).value(20).stack(1)
                .equipment(wear(EquipmentDef.Slot.SHIELD, new EquipmentStats(0, 0, 0, 0, 0, 4, 5, 3, 1, 4, 0, 0, 0, 0)))
                .examine("A solid wooden shield.");
        s.add("thread", "Thread", Kind.SIMPLE, "string", 0xE0E0D0).value(1).examine("Use with a needle to make clothes.");
        s.add("candle", "Candle", Kind.SIMPLE, "stake", 0xF0E8C0).value(3).stack(1).examine("A candle.");
        s.add("kebab", "Kebab", Kind.FOOD, "meat_cooked", 0xC08A4A).value(1).heal(3).examine("A meaty kebab.");
        s.add("waterskin_4", "Waterskin(4)", Kind.SIMPLE, "pot_item", 0x8A6A4A).value(30).stack(1).examine("A full waterskin with four portions of water.");
        s.add("shantay_pass_ticket", "Shantay pass", Kind.SIMPLE, "note", 0xE8D8A0).value(5).examine("Allows you to pass through the Shantay Pass into the Kharidian Desert.");
        s.add("asgarnian_ale", "Asgarnian ale", Kind.FOOD, "beer", 0xA06A2A).value(2).heal(2).stack(1).examine("Probably the finest readily available ale in Asgarnia.");
        s.add("dwarven_stout", "Dwarven stout", Kind.FOOD, "beer", 0x3A2A1A).value(3).heal(1).stack(1).examine("A pint of thick dark beer.");
        s.add("cider", "Cider", Kind.FOOD, "beer", 0xD0A040).value(2).heal(2).stack(1).examine("A glass of cider.");
        s.add("lemon", "Lemon", Kind.FOOD, "produce", 0xE8E040).value(2).heal(2).examine("A sour lemon.");
        s.add("lime", "Lime", Kind.FOOD, "produce", 0x60C040).value(2).heal(2).examine("A sour lime.");
        s.add("equa_leaves", "Equa leaves", Kind.SIMPLE, "herb", 0x4A9A3A).value(1).examine("A common gnome ingredient.");
        s.add("gnome_spice", "Gnome spice", Kind.SIMPLE, "spice", 0xC0602A).value(1).examine("Aluft Gianne's secret spice.");
        s.add("gianne_dough", "Gianne dough", Kind.SIMPLE, "cake", 0xE0D0A0).value(2).examine("A ball of gnome dough.");
        s.add("cocktail_shaker", "Cocktail shaker", Kind.TOOL, "pot_item", 0xB0B0C0).value(2).stack(1).examine("Used for mixing cocktails.");
        s.add("choc_ice", "Choc-ice", Kind.FOOD, "cake", 0x5A3A2A).value(30).heal(7).examine("A delicious chocolate ice cream. Best eaten in the desert.");
        s.add("bear_fur", "Bear fur", Kind.SIMPLE, "fur", 0x5A3A2A).value(20).examine("This would make warm clothing.");
        s.add("noose_wand", "Noose wand", Kind.TOOL, "staff", 0x8A6A3A).value(4).stack(1).examine("A wand with a noose for catching kebbits.");
        s.add("rabbit_snare", "Rabbit snare", Kind.TOOL, "snare", 0xA08A5A).value(18).stack(1).examine("A simple snare for catching rabbits.");
        s.add("teasing_stick", "Teasing stick", Kind.TOOL, "staff", 0x6A4A2A).value(1).stack(1).examine("For luring creatures out of their lairs.");
        s.add("watering_can", "Watering can(8)", Kind.TOOL, "pot_item", 0x6A8A6A).value(25).stack(1).examine("This watering can is full.");
        s.add("white_apron", "White apron", Kind.ARMOUR, "leather_body", 0xE8E8E8).value(2).stack(1)
                .equipment(wear(EquipmentDef.Slot.CHEST, none())).examine("A mostly clean apron.");
        s.add("brown_apron", "Brown apron", Kind.ARMOUR, "leather_body", 0x7A5A3A).value(2).stack(1)
                .equipment(wear(EquipmentDef.Slot.CHEST, none())).examine("A mostly clean apron.");
        s.add("chefs_hat", "Chef's hat", Kind.ARMOUR, "cowl", 0xF0F0F0).value(2).stack(1)
                .equipment(wear(EquipmentDef.Slot.HEAD, none())).examine("What a silly hat. Needed to enter the Cooks' Guild.");
        s.add("black_skirt", "Black skirt", Kind.ARMOUR, "chaps", 0x2A2A2A).value(2).stack(1)
                .equipment(wear(EquipmentDef.Slot.LEGS, none())).examine("Clothing favoured by women and dark wizards.");
        s.add("blue_skirt", "Blue skirt", Kind.ARMOUR, "chaps", 0x2A3A9A).value(2).stack(1)
                .equipment(wear(EquipmentDef.Slot.LEGS, none())).examine("A ladies' skirt.");
        s.add("red_cape", "Red cape", Kind.SIMPLE, "cape", 0xB02A2A).value(2).stack(1).examine("A thick red cape.");
        s.add("priest_gown_top", "Priest gown (top)", Kind.ARMOUR, "leather_body", 0xE0E0E0).value(5).stack(1)
                .equipment(wear(EquipmentDef.Slot.CHEST, new EquipmentStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3)))
                .examine("A priest's gown.");
        s.add("priest_gown_bottom", "Priest gown (bottom)", Kind.ARMOUR, "chaps", 0xE0E0E0).value(5).stack(1)
                .equipment(wear(EquipmentDef.Slot.LEGS, new EquipmentStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3)))
                .examine("A priest's gown.");
        s.add("wizard_hat", "Wizard hat", Kind.ARMOUR, "cowl", 0x2A3A9A).value(2).stack(1)
                .equipment(wear(EquipmentDef.Slot.HEAD, new EquipmentStats(0, 0, 0, 2, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0)))
                .examine("A silly pointed hat.");
        s.add("desert_shirt", "Desert shirt", Kind.ARMOUR, "leather_body", 0xE8E0C8).value(40).stack(1)
                .equipment(wear(EquipmentDef.Slot.CHEST, new EquipmentStats(0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0)))
                .examine("A cool, light desert shirt.");
        s.add("desert_robe", "Desert robe", Kind.ARMOUR, "chaps", 0xE8E0C8).value(40).stack(1)
                .equipment(wear(EquipmentDef.Slot.LEGS, new EquipmentStats(0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0)))
                .examine("A cool, light desert robe.");
        s.add("desert_boots", "Desert boots", Kind.ARMOUR, "boots", 0xC8B898).value(20).stack(1)
                .equipment(wear(EquipmentDef.Slot.FEET, none())).examine("Comfortable desert shoes.");
    }
}
