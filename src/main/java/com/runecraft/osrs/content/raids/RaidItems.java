package com.runecraft.osrs.content.raids;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentDef.Slot;
import com.runecraft.osrs.EquipmentDef.WeaponClass;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ItemSink;

import java.util.List;
import java.util.Map;

/**
 * Raid uniques (with their OSRS equipment bonuses), pets and the temporary items used inside raids.
 * Special attacks and passive effects (twisted bow scaling, scythe multi-hit, shadow's magic
 * multiplier, dragon claws spec...) belong to the systems module; only the items live here.
 *
 * <p>There are no ring/amulet/hand slots, so Lightbearer is a plain item. Bonuses for unfortified
 * Masori armour and the Sanguinesti staff/Dinh's bulwark defensive bonuses are approximations.
 */
public final class RaidItems {
    /** Items handed out inside a raid that are removed when the player leaves. */
    public static final List<String> RAID_ONLY_ITEMS = List.of("cox_kindling", "cavern_grubs", "crondis_water_container");

    private RaidItems() {
    }

    private static EquipmentStats s(int stabA, int slashA, int crushA, int magicA, int rangedA, int stabD, int slashD, int crushD,
                                    int magicD, int rangedD, int str, int rangedStr, int magicDmg, int prayer) {
        return new EquipmentStats(stabA, slashA, crushA, magicA, rangedA, stabD, slashD, crushD, magicD, rangedD, str, rangedStr,
                magicDmg, prayer);
    }

    private static EquipmentDef armour(Slot slot, EquipmentStats stats, Map<Skill, Integer> reqs) {
        return new EquipmentDef(slot, stats, reqs, 0, AttackType.CRUSH, WeaponClass.NONE);
    }

    private static EquipmentDef weapon(EquipmentStats stats, Map<Skill, Integer> reqs, int speed, AttackType type, WeaponClass cls) {
        return new EquipmentDef(Slot.WEAPON, stats, reqs, speed, type, cls);
    }

    public static void add(ItemSink items) {
        // ---------------- Chambers of Xeric ----------------
        items.add("dexterous_prayer_scroll", "Dexterous prayer scroll", Kind.SIMPLE, "note", 0x3A9A4A).value(30_000_000).stack(1)
                .examine("Read this to learn the Rigour prayer.");
        items.add("arcane_prayer_scroll", "Arcane prayer scroll", Kind.SIMPLE, "note", 0x6A4AC0).value(10_000_000).stack(1)
                .examine("Read this to learn the Augury prayer.");
        items.add("twisted_buckler", "Twisted buckler", Kind.SHIELD, "buckler", 0x6A5A3A).value(4_000_000).stack(1).tier("dragon")
                .equipment(armour(Slot.SHIELD, s(0, 0, 0, -10, 18, 36, 40, 44, 10, 52, 0, 0, 0, 1),
                        Map.of(Skill.RANGED, 75, Skill.DEFENCE, 75)))
                .examine("A buckler made from the remains of Xeric's forces.");
        items.add("dragon_hunter_crossbow", "Dragon hunter crossbow", Kind.BOW, "crossbow", 0x3A6A3A).value(70_000_000).stack(1)
                .tier("dragon")
                .equipment(weapon(s(0, 0, 0, 0, 95, 0, 0, 0, 0, 0, 0, 0, 0, 0), Map.of(Skill.RANGED, 65), 6, AttackType.RANGED,
                        WeaponClass.BOW))
                .examine("A crossbow specialising in hunting dragons.");
        items.add("dinhs_bulwark", "Dinh's bulwark", Kind.MELEE_WEAPON, "bulwark", 0x7A7A8A).value(1_500_000).stack(1).tier("dragon")
                .equipment(weapon(s(0, 0, 0, -24, -20, 157, 156, 161, -12, 155, 4, 0, 0, 0),
                        Map.of(Skill.ATTACK, 75, Skill.DEFENCE, 75), 7, AttackType.CRUSH, WeaponClass.MELEE))
                .examine("A huge two-handed shield, once wielded by Dinh.");
        items.add("ancestral_hat", "Ancestral hat", Kind.ARMOUR, "cowl", 0x2A2A5A).value(15_000_000).stack(1).tier("dragon")
                .equipment(armour(Slot.HEAD, s(0, 0, 0, 8, -2, 12, 8, 11, 5, 0, 0, 0, 2, 1),
                        Map.of(Skill.MAGIC, 75, Skill.DEFENCE, 65)))
                .examine("The hat of a powerful sorceress from a bygone era.");
        items.add("ancestral_robe_top", "Ancestral robe top", Kind.ARMOUR, "robe_top", 0x2A2A5A).value(40_000_000).stack(1)
                .tier("dragon")
                .equipment(armour(Slot.CHEST, s(0, 0, 0, 35, -8, 42, 31, 51, 28, 0, 0, 0, 2, 1),
                        Map.of(Skill.MAGIC, 75, Skill.DEFENCE, 65)))
                .examine("The robe top of a powerful sorceress from a bygone era.");
        items.add("ancestral_robe_bottom", "Ancestral robe bottom", Kind.ARMOUR, "robe_bottom", 0x2A2A5A).value(35_000_000).stack(1)
                .tier("dragon")
                .equipment(armour(Slot.LEGS, s(0, 0, 0, 26, -7, 27, 24, 30, 20, 0, 0, 0, 2, 1),
                        Map.of(Skill.MAGIC, 75, Skill.DEFENCE, 65)))
                .examine("The robe bottoms of a powerful sorceress from a bygone era.");
        items.add("dragon_claws", "Dragon claws", Kind.MELEE_WEAPON, "claws", 0xB02A1A).value(40_000_000).stack(1).tier("dragon")
                .equipment(weapon(s(41, 57, -4, 0, 0, 13, 26, 7, 0, 0, 56, 0, 0, 0), Map.of(Skill.ATTACK, 60), 4, AttackType.SLASH,
                        WeaponClass.MELEE))
                .examine("A set of fighting claws.");
        items.add("elder_maul", "Elder maul", Kind.MELEE_WEAPON, "maul", 0x8A8A7A).value(10_000_000).stack(1).tier("dragon")
                .equipment(weapon(s(0, 0, 135, -4, 0, 0, 0, 0, 0, 0, 147, 0, 0, 0), Map.of(Skill.ATTACK, 75, Skill.STRENGTH, 75), 6,
                        AttackType.CRUSH, WeaponClass.MELEE))
                .examine("A maul crafted from the component parts of Tekton.");
        items.add("kodai_insignia", "Kodai insignia", Kind.SIMPLE, "gem", 0x3A8AC0).value(30_000_000).stack(1)
                .examine("The insignia of the Kodai. Combine it with a master wand.");
        items.add("kodai_wand", "Kodai wand", Kind.STAFF, "wand", 0x3A8AC0).value(31_000_000).stack(1).tier("dragon")
                .equipment(weapon(s(0, 0, 0, 28, 0, 0, 3, 3, 20, 0, 0, 0, 15, 0), Map.of(Skill.MAGIC, 75), 4, AttackType.MAGIC,
                        WeaponClass.STAFF))
                .examine("A wand of an ancient Kodai mage.");
        items.add("twisted_bow", "Twisted bow", Kind.BOW, "longbow", 0x2E4A3A).value(1_000_000_000).stack(1).tier("dragon")
                .equipment(weapon(s(0, 0, 0, 0, 70, 0, 0, 0, 0, 0, 0, 20, 0, 0), Map.of(Skill.RANGED, 75), 6, AttackType.RANGED,
                        WeaponClass.BOW))
                .examine("A mystical bow carved from the twisted remains of the Great Olm.");
        items.add("olmlet", "Olmlet", Kind.SIMPLE, "pet", 0xD8C870).value(1).stack(1).examine("Its mother may be lurking nearby.");

        // ---------------- Theatre of Blood ----------------
        items.add("avernic_defender_hilt", "Avernic defender hilt", Kind.SIMPLE, "hilt", 0xA02020).value(40_000_000).stack(1)
                .examine("Combine it with a dragon defender to make an avernic defender.");
        items.add("avernic_defender", "Avernic defender", Kind.SHIELD, "defender", 0xA02020).value(40_000_000).stack(1).tier("dragon")
                .equipment(armour(Slot.SHIELD, s(30, 29, 28, -5, -4, 30, 29, 28, -2, -8, 8, 0, 0, 0),
                        Map.of(Skill.ATTACK, 70, Skill.DEFENCE, 70)))
                .examine("A defender made with the help of the Avernic.");
        items.add("ghrazi_rapier", "Ghrazi rapier", Kind.MELEE_WEAPON, "rapier", 0xB0B0C0).value(60_000_000).stack(1).tier("dragon")
                .equipment(weapon(s(94, 55, 0, 0, 0, 0, 0, 0, 0, 0, 89, 0, 0, 0), Map.of(Skill.ATTACK, 80), 4, AttackType.STAB,
                        WeaponClass.MELEE))
                .examine("A rapier forged by the Myreque's ancestors.");
        items.add("sanguinesti_staff", "Sanguinesti staff", Kind.STAFF, "staff_orb", 0x8A1A2A).value(70_000_000).stack(1).tier("dragon")
                .equipment(weapon(s(0, 0, 0, 25, 0, 2, 3, 1, 15, 0, 0, 0, 0, 0), Map.of(Skill.MAGIC, 82), 4, AttackType.MAGIC,
                        WeaponClass.STAFF))
                .examine("A powerful staff that drinks the blood of its victims.");
        items.add("justiciar_faceguard", "Justiciar faceguard", Kind.ARMOUR, "full_helm", 0xC8B070).value(15_000_000).stack(1)
                .tier("dragon")
                .equipment(armour(Slot.HEAD, s(0, 0, 0, -6, -2, 60, 63, 59, -6, 67, 0, 0, 0, 2), Map.of(Skill.DEFENCE, 75)))
                .examine("The faceguard of a Justiciar.");
        items.add("justiciar_chestguard", "Justiciar chestguard", Kind.ARMOUR, "platebody", 0xC8B070).value(15_000_000).stack(1)
                .tier("dragon")
                .equipment(armour(Slot.CHEST, s(0, 0, 0, -40, -20, 132, 130, 117, -16, 142, 0, 0, 0, 4), Map.of(Skill.DEFENCE, 75)))
                .examine("The chestguard of a Justiciar.");
        items.add("justiciar_legguards", "Justiciar legguards", Kind.ARMOUR, "platelegs", 0xC8B070).value(15_000_000).stack(1)
                .tier("dragon")
                .equipment(armour(Slot.LEGS, s(0, 0, 0, -31, -17, 95, 92, 93, -14, 102, 0, 0, 0, 4), Map.of(Skill.DEFENCE, 75)))
                .examine("The legguards of a Justiciar.");
        items.add("scythe_of_vitur", "Scythe of vitur", Kind.MELEE_WEAPON, "scythe", 0x9A1A1A).value(500_000_000).stack(1)
                .tier("dragon")
                .equipment(weapon(s(70, 110, 30, -6, 0, -2, 8, 10, 0, 0, 75, 0, 0, 0), Map.of(Skill.ATTACK, 80, Skill.STRENGTH, 90), 5,
                        AttackType.SLASH, WeaponClass.MELEE))
                .examine("A scythe once wielded by Verzik Vitur.");
        items.add("lil_zik", "Lil' Zik", Kind.SIMPLE, "pet", 0x6A2A4A).value(1).stack(1).examine("Something tiny and nasty.");
        // vial_of_blood is defined by another module (skilling / systems).

        // ---------------- Tombs of Amascut ----------------
        items.add("osmumtens_fang", "Osmumten's fang", Kind.MELEE_WEAPON, "fang", 0xE0D8B0).value(20_000_000).stack(1).tier("dragon")
                .equipment(weapon(s(105, 75, 0, 0, 0, 0, 0, 0, 0, 0, 103, 0, 0, 0), Map.of(Skill.ATTACK, 82), 5, AttackType.STAB,
                        WeaponClass.MELEE))
                .examine("A fang from a vicious creature of the desert.");
        // lightbearer is defined by another module (skilling / systems).
        items.add("elidinis_ward", "Elidinis' ward", Kind.SHIELD, "ward", 0x3A9AA0).value(10_000_000).stack(1).tier("dragon")
                .equipment(armour(Slot.SHIELD, s(0, 0, 0, 25, 0, 53, 55, 73, 2, 52, 0, 0, 0, 4),
                        Map.of(Skill.MAGIC, 80, Skill.PRAYER, 80)))
                .examine("A ward blessed by Elidinis.");
        items.add("masori_mask", "Masori mask", Kind.ARMOUR, "mask", 0x8A6A3A).value(5_000_000).stack(1).tier("dragon")
                .equipment(armour(Slot.HEAD, s(0, 0, 0, -1, 12, 6, 7, 8, 8, 7, 0, 2, 0, 1), Map.of(Skill.RANGED, 80, Skill.DEFENCE, 30)))
                .examine("The mask of a Masori warrior.");
        items.add("masori_body", "Masori body", Kind.ARMOUR, "leather_body", 0x8A6A3A).value(20_000_000).stack(1).tier("dragon")
                .equipment(armour(Slot.CHEST, s(0, 0, 0, -4, 43, 28, 24, 32, 36, 30, 0, 4, 0, 1),
                        Map.of(Skill.RANGED, 80, Skill.DEFENCE, 30)))
                .examine("The body armour of a Masori warrior.");
        items.add("masori_chaps", "Masori chaps", Kind.ARMOUR, "chaps", 0x8A6A3A).value(20_000_000).stack(1).tier("dragon")
                .equipment(armour(Slot.LEGS, s(0, 0, 0, -2, 27, 16, 14, 18, 20, 18, 0, 2, 0, 1),
                        Map.of(Skill.RANGED, 80, Skill.DEFENCE, 30)))
                .examine("The chaps of a Masori warrior.");
        items.add("tumekens_shadow", "Tumeken's shadow", Kind.STAFF, "staff_orb", 0x5A2A8A).value(900_000_000).stack(1).tier("dragon")
                .equipment(weapon(s(0, 0, 0, 35, 0, 0, 0, 0, 20, 0, 0, 0, 1, 1), Map.of(Skill.MAGIC, 85), 5, AttackType.MAGIC,
                        WeaponClass.STAFF))
                .examine("A staff imbued with the power of Tumeken.");
        items.add("tumekens_guardian", "Tumeken's guardian", Kind.SIMPLE, "pet", 0xE0B040).value(1).stack(1)
                .examine("A tiny Warden, sworn to protect you.");

        // ---------------- Challenge Mode / Hard mode rewards ----------------
        items.add("twisted_ancestral_colour_kit", "Twisted ancestral colour kit", Kind.SIMPLE, "package", 0x5A3A2A).value(1).stack(1)
                .examine("Use it on ancestral robes to give them a twisted look.");
        items.add("metamorphic_dust", "Metamorphic dust", Kind.SIMPLE, "secondary", 0xC8B8E0).value(1).stack(1)
                .examine("Sprinkle it on an Olmlet to change its form.");
        items.add("sanguine_ornament_kit", "Sanguine ornament kit", Kind.SIMPLE, "package", 0x8A1A2A).value(2_500_000).stack(1)
                .examine("Use it on a Scythe of vitur to give it a bloody sheen.");
        items.add("holy_ornament_kit", "Holy ornament kit", Kind.SIMPLE, "package", 0xE8E0C0).value(8_000_000).stack(1)
                .examine("Use it on a Scythe of vitur, Sanguinesti staff or Ghrazi rapier to make it holy.");
        items.add("sanguine_dust", "Sanguine dust", Kind.SIMPLE, "secondary", 0xA01A2A).value(80_000_000).stack(1)
                .examine("Sprinkle it on Lil' Zik to change its form.");

        // ---------------- Common reward items not defined elsewhere ----------------
        items.add("saltpetre", "Saltpetre", Kind.SIMPLE, "secondary", 0xE8E8D8).value(10)
                .examine("Used in the making of fertiliser and dynamite.");
        items.add("torn_prayer_scroll", "Torn prayer scroll", Kind.SIMPLE, "prayer_scroll", 0xC8B890).value(1).stack(1)
                .examine("Read this to learn the Preserve prayer.");
        items.add("dark_relic", "Dark relic", Kind.SIMPLE, "orb", 0x3A2A4A).value(1).stack(1)
                .examine("A relic of Xeric's power. Rub it to gain experience in a skill.");
        items.add("dragon_dart_tip", "Dragon dart tip", Kind.SIMPLE, "dart_tip", 0xB02A1A).value(1_000)
                .examine("Fletch with feathers to make dragon darts.");
        items.add("lily_of_the_sands", "Lily of the sands", Kind.SIMPLE, "herb", 0xE8D890).value(1_000)
                .examine("A rare desert flower, used in making stamina-restoring brews.");
        items.add("blood_essence", "Blood essence", Kind.SIMPLE, "orb", 0x9A1A2A).value(25_000)
                .examine("Use it on a bracelet of ethereum or blood rune essence.");
        items.add("cache_of_runes", "Cache of runes", Kind.SIMPLE, "pouch", 0x7A5A9A).value(1).stack(1)
                .examine("A cache of blood, death and soul runes.");
        items.add("swamp_tar", "Swamp tar", Kind.SIMPLE, "jar", 0x2A2A1A).value(5)
                .examine("A foul-smelling thick tar-like substance.");

        // ---------------- Items used inside raids ----------------
        items.add("cox_kindling", "Kindling", Kind.SIMPLE, "log", 0xA07A40).value(1)
                .examine("Chopped from a sapling in the Chambers. Burn it in a brazier.");
        items.add("cavern_grubs", "Cavern grubs", Kind.SIMPLE, "bait", 0xC8C890).value(1)
                .examine("Scavengers love these.");
        items.add("crondis_water_container", "Water container", Kind.SIMPLE, "vial_water", 0x3A7AC0).value(1).stack(1)
                .examine("Full of precious water for the Palm of Resourcefulness.");
    }

    /** Collection log items per raid (uniques followed by the pet). */
    public static List<String> collectionLog(RaidType type) {
        List<String> list = new java.util.ArrayList<>();
        for (RaidRewards.Unique u : RaidRewards.uniques(type)) {
            list.add(u.item());
        }
        list.add(RaidRewards.pet(type));
        return list;
    }
}
