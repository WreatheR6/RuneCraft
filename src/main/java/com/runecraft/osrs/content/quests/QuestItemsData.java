package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.Skill;

import java.util.Map;

/**
 * Quest reward equipment shared by several quests, the virtual-slot accessories (gloves, capes,
 * amulets) and the quest-locked weapons.
 */
final class QuestItemsData {
    private QuestItemsData() {
    }

    /** Recipe for Disaster gloves: id, name, colour, attack/def bonus, magic bonus, price (Culinaromancer's chest). */
    static final Object[][] RFD_GLOVES = {
            {"rfd_bronze_gloves", "Bronze gloves", 0x8A5A2A, 1, 0, 104},
            {"rfd_iron_gloves", "Iron gloves", 0x6A6A6A, 2, 1, 208},
            {"rfd_steel_gloves", "Steel gloves", 0x9A9AA0, 3, 2, 520},
            {"rfd_black_gloves", "Black gloves", 0x2A2A2A, 4, 2, 728},
            {"rfd_mithril_gloves", "Mithril gloves", 0x3A3A7A, 5, 3, 1040},
            {"rfd_adamant_gloves", "Adamant gloves", 0x3A6A4A, 6, 4, 1560},
            {"rfd_rune_gloves", "Rune gloves", 0x3A7A9A, 8, 5, 2080},
            {"rfd_dragon_gloves", "Dragon gloves", 0xA02020, 9, 6, 104000},
            {"barrows_gloves", "Barrows gloves", 0x7A4A2A, 12, 6, 130000},
    };

    static EquipmentStats allRound(int bonus, int magic, int str) {
        return new EquipmentStats(bonus, bonus, bonus, magic, bonus, bonus, bonus, bonus, magic, bonus, str, 0, 0, 0);
    }

    static void register(QuestBook b) {
        // Recipe for Disaster gloves (worn in the virtual HANDS slot). Stats are the real OSRS values.
        for (Object[] g : RFD_GLOVES) {
            String id = (String) g[0];
            int bonus = (Integer) g[3];
            int magic = (Integer) g[4];
            String name = (String) g[1];
            b.items(s -> s.add(id, name, Kind.SIMPLE, "gloves", (Integer) g[2]).value((Integer) g[5]).stack(1)
                    .examine(name + " from the Culinaromancer's chest. +" + bonus + " attack/defence/strength. Right-click to wear."));
            b.gear(id, QuestBook.GearSlot.HANDS, allRound(bonus, magic, bonus), null);
        }
        // Ava's devices (Animal Magnetism). Accumulator: +4 ranged attack (ammo saving is not modelled).
        b.items(s -> s.add("avas_attractor", "Ava's attractor", Kind.SIMPLE, "cape", 0x6A5A3A).value(999).stack(1)
                .examine("A superbly animated chicken that attracts iron arrows. Right-click to wear."));
        b.items(s -> s.add("avas_accumulator", "Ava's accumulator", Kind.SIMPLE, "cape", 0x8A6A3A).value(999).stack(1)
                .examine("A superbly animated chicken that collects ammunition. Right-click to wear."));
        b.gear("avas_attractor", QuestBook.GearSlot.CAPE, new EquipmentStats(0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0), null);
        b.gear("avas_accumulator", QuestBook.GearSlot.CAPE, new EquipmentStats(0, 0, 0, 0, 4, 0, 1, 0, 4, 0, 0, 0, 0, 0), null);
        // Cape of Legends (Legends' Quest): +7 all defences.
        b.items(s -> s.add("cape_of_legends", "Cape of legends", Kind.SIMPLE, "cape", 0xE8E8E8).value(450).stack(1)
                .examine("The cape worn by members of the Legends' Guild. Right-click to wear."));
        b.gear("cape_of_legends", QuestBook.GearSlot.CAPE, new EquipmentStats(0, 0, 0, 0, 0, 7, 7, 7, 7, 7, 0, 0, 0, 0), "legends_quest");
        // The quest point cape item is defined (and worn) by the systems module; the Wise Old Man sells it.
        // Steel gauntlets (Family Crest).
        b.items(s -> s.add("steel_gauntlets", "Steel gauntlets", Kind.SIMPLE, "gloves", 0xA0A0A8).value(1).stack(1)
                .examine("Very handy gauntlets bearing the crest of the Fitzharmon family. Right-click to wear."));
        b.items(s -> s.add("chaos_gauntlets", "Chaos gauntlets", Kind.SIMPLE, "gloves", 0x5A5A8A).value(1).stack(1)
                .examine("Gauntlets that boost bolt spells. Right-click to wear."));
        b.gear("steel_gauntlets", QuestBook.GearSlot.HANDS, new EquipmentStats(0, 0, 0, 0, 0, 5, 5, 5, 0, 5, 0, 0, 0, 0), null);
        b.gear("chaos_gauntlets", QuestBook.GearSlot.HANDS, new EquipmentStats(0, 0, 0, 0, 0, 5, 5, 5, 0, 5, 0, 0, 0, 0), null);
        // Gnome amulet of protection (Tree Gnome Village).
        b.items(s -> s.add("gnome_amulet", "Gnome amulet of protection", Kind.SIMPLE, "amulet", 0x7AB04A).value(1).stack(1)
                .examine("A gift from King Bolren. Right-click to wear."));
        b.gear("gnome_amulet", QuestBook.GearSlot.NECK, new EquipmentStats(0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0), null);

        // Quest reward weapons
        b.items(s -> s.add("excalibur", "Excalibur", Kind.MELEE_WEAPON, "longsword", 0xD8D8F0).value(200).stack(1).tier("steel")
                .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON, new EquipmentStats(20, 29, -2, 0, 0, 0, 3, 2, 1, 0, 25, 0, 0, 0),
                        Map.of(Skill.ATTACK, 20), 5, AttackType.SLASH, EquipmentDef.WeaponClass.MELEE))
                .examine("This used to belong to King Arthur."));
        b.items(s -> s.add("dramen_staff", "Dramen staff", Kind.STAFF, "staff", 0x6A8A3A).value(15).stack(1)
                .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON, new EquipmentStats(-1, -1, 10, 10, 0, 0, 0, 0, 10, 0, 10, 0, 0, 0),
                        Map.of(), 4, AttackType.CRUSH, EquipmentDef.WeaponClass.STAFF))
                .examine("A magical staff cut from the Dramen tree. Lets you enter Zanaris."));
        b.items(s -> s.add("wolfbane", "Wolfbane", Kind.MELEE_WEAPON, "dagger", 0xC0C0D0).value(1).stack(1).tier("silver")
                .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON, new EquipmentStats(10, 5, -4, 0, 0, 0, 0, 0, 1, 0, 6, 0, 0, 0),
                        Map.of(), 4, AttackType.STAB, EquipmentDef.WeaponClass.MELEE))
                .examine("A dagger that prevents werewolves from changing form."));
        b.items(s -> s.add("silver_sickle_b", "Silver sickle (b)", Kind.MELEE_WEAPON, "scimitar", 0xD0D0E0).value(1).stack(1).tier("silver")
                .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON, new EquipmentStats(4, 6, 2, 0, 0, 0, 0, 0, 0, 0, 6, 0, 0, 0),
                        Map.of(), 4, AttackType.SLASH, EquipmentDef.WeaponClass.MELEE))
                .examine("A blessed silver sickle. Casts Bloom in the swamps of Mort Myre."));
        b.items(s -> s.add("ibans_staff", "Iban's staff", Kind.STAFF, "staff_orb", 0x3A2A2A).value(1).stack(1)
                .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON, new EquipmentStats(5, -1, 7, 20, 0, 2, 3, 1, 20, 0, 7, 0, 0, 0),
                        Map.of(Skill.MAGIC, 50, Skill.ATTACK, 50), 5, AttackType.CRUSH, EquipmentDef.WeaponClass.STAFF))
                .examine("An ancient staff, formerly the property of Iban. Casts Iban Blast."));
        b.items(s -> s.add("lunar_staff", "Lunar staff", Kind.STAFF, "staff", 0x8AA0C0).value(1).stack(1)
                .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON, new EquipmentStats(4, -1, 12, 12, 0, 2, 3, 1, 12, 0, 10, 0, 0, 0),
                        Map.of(Skill.MAGIC, 65), 5, AttackType.CRUSH, EquipmentDef.WeaponClass.STAFF))
                .examine("A staff carved from Moonclan wood."));
        // Ivandis/Blisterwood flails (Taste of Hope / Sins of the Father). Approximate stats.
        b.items(s -> s.add("ivandis_flail", "Ivandis flail", Kind.MELEE_WEAPON, "flail", 0xB0B0C0).value(1).stack(1).tier("silver")
                .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON, new EquipmentStats(0, 0, 40, 0, 0, 0, 0, 0, 0, 0, 45, 0, 0, 0),
                        Map.of(Skill.ATTACK, 40, Skill.SLAYER, 38), 4, AttackType.CRUSH, EquipmentDef.WeaponClass.MELEE))
                .examine("A blessed silver flail that can harm even the most powerful vampyres."));
        b.items(s -> s.add("blisterwood_flail", "Blisterwood flail", Kind.MELEE_WEAPON, "flail", 0x6A4A6A).value(1).stack(1).tier("dragon")
                .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON, new EquipmentStats(0, 0, 67, 0, 0, 0, 0, 0, 0, 0, 81, 0, 0, 0),
                        Map.of(Skill.ATTACK, 70, Skill.SLAYER, 50), 4, AttackType.CRUSH, EquipmentDef.WeaponClass.MELEE))
                .examine("A flail of blisterwood, deadly to vampyres."));

        // Quest-locked base-game weapons
        b.gate("dragon_longsword", "lost_city");
        b.gate("dragon_dagger", "lost_city");
        b.gate("dragon_scimitar", "monkey_madness_i");
        b.gate("dragon_battleaxe", "heroes_quest");
        b.gate("ibans_staff", "underground_pass");
        b.gate("lunar_staff", "lunar_diplomacy");
        b.gate("dragon_mace", "heroes_quest");
        b.gate("dragon_spear", "lost_city");
        b.gate("dragon_halberd", "regicide");
        b.gate("heavy_ballista", "monkey_madness_ii"); // CommonEquipment item; OSRS also needs Duke's training
        // Quest-locked armour: the rune platebody, its trimmed/god variants and the green d'hide body need
        // Dragon Slayer I; the dragon sq shield needs Legends' Quest.
        for (String body : new String[]{"rune_platebody", "rune_platebody_t", "rune_platebody_g", "saradomin_platebody",
                "zamorak_platebody", "guthix_platebody", "gilded_platebody", "green_dhide_body", "green_dhide_body_t",
                "green_dhide_body_g"}) {
            b.armourGate(body, "dragon_slayer_i");
        }
        b.armourGate("dragon_sq_shield", "legends_quest");
    }
}
