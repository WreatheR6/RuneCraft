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
        // Boss/slayer materials upgraded by other modules: Doom of Mokhaiotl and Maggot King drops (bosses) become
        // confliction gauntlets and the necklace of rupture (systems); the tormented synapse (slayer, Tormented Demons)
        // becomes the purging staff (skilling) and the scorching bow (slayer).
        items.add("mokhaiotl_cloth", "Mokhaiotl cloth", Kind.SIMPLE, "cloth", 0x6A3A8A).value(30_000_000).stack(1)
                .examine("Woven from the Doom's flesh. Upgrades a tormented bracelet into confliction gauntlets.");
        items.add("demon_tear", "Demon tear", Kind.SIMPLE, "gem", 0xC03050).value(1).stack(100000)
                .examine("Charges the eye of ayak.");
        items.add("elder_venator_fang", "Elder venator fang", Kind.SIMPLE, "fang", 0xE0D0B0).value(40_000_000).stack(1)
                .examine("Combine with a necklace of anguish to make a necklace of rupture (84 Crafting).");
        items.add("tormented_synapse", "Tormented synapse", Kind.SIMPLE, "orb", 0xE05020).value(800_000).stack(1)
                .examine("A twitching synapse from a tormented demon. Its fiery energy could empower a demonbane weapon.");
        // Construction material also used to repair the Mort'ton temple (minigames); sacred oil, made at the temple's
        // fire altar and turned into pyre logs (minigames), is also a Shades of Mort'ton quest item (quests).
        items.add("limestone_brick", "Limestone brick", Kind.SIMPLE, "brick", 0xD8D0B0).value(21);
        items.add("sacred_oil", "Sacred oil", Kind.POTION, "potion", 0xE0E0A0).value(100).stack(1)
                .examine("4 doses of sacred oil.");
        CommonEquipment.add(items);
    }
}
