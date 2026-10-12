package com.runecraft.osrs.content;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.Skill;

import java.util.Map;

/**
 * Items referenced by more than one content module (defined once here so modules can reference
 * them freely in drop tables and recipes).
 */
public final class CommonItems {
    private CommonItems() {
    }

    public static void add(ItemSink items) {
        items.add("green_dragonhide", "Green dragonhide", Kind.SIMPLE, "hide", 0x3A8A3A).value(1800);
        items.add("blue_dragonhide", "Blue dragonhide", Kind.SIMPLE, "hide", 0x3A5AB0).value(2000);
        items.add("red_dragonhide", "Red dragonhide", Kind.SIMPLE, "hide", 0xB03A3A).value(2400);
        items.add("black_dragonhide", "Black dragonhide", Kind.SIMPLE, "hide", 0x2A2A2A).value(3200);
        items.add("anti_dragon_shield", "Anti-dragon shield", Kind.SHIELD, "kiteshield", 0x6A8AA0).value(20).stack(1).tier("steel")
                .equipment(new EquipmentDef(EquipmentDef.Slot.SHIELD, new EquipmentStats(0, 0, 0, -10, -5, 20, 24, 22, 10, 22, 0, 0, 0, 0),
                        Map.of(), 0, AttackType.CRUSH, EquipmentDef.WeaponClass.NONE))
                .examine("Protects you from dragonfire.");
        items.add("antifire_potion", "Antifire potion", Kind.POTION, "potion", 0x9A3A9A).value(180).stack(1);
        items.add("extended_antifire", "Extended antifire", Kind.POTION, "potion", 0xB04AB0).value(400).stack(1);
        items.add("abyssal_whip", "Abyssal whip", Kind.MELEE_WEAPON, "whip", 0x5A2A2A).value(120001).stack(1).tier("dragon")
                .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON, new EquipmentStats(0, 82, 0, 0, 0, 0, 0, 0, 0, 0, 82, 0, 0, 0),
                        Map.of(Skill.ATTACK, 70), 4, AttackType.SLASH, EquipmentDef.WeaponClass.MELEE));
        for (String tier : new String[]{"beginner", "easy", "medium", "hard", "elite", "master"}) {
            String name = Character.toUpperCase(tier.charAt(0)) + tier.substring(1);
            items.add("clue_scroll_" + tier, "Clue scroll (" + tier + ")", Kind.SIMPLE, "note", 0xE8D8A0).value(1).stack(1)
                    .examine("A " + name.toLowerCase() + " clue scroll.");
        }
        CommonEquipment.add(items);
    }
}
