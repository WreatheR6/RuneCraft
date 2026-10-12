package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentDef.Slot;
import com.runecraft.osrs.EquipmentDef.WeaponClass;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ItemSink;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Boss uniques, pets and boss-specific resources with their OSRS equipment bonuses.
 *
 * <p>The equipment system currently only has head/body/legs/feet/weapon/shield slots, so rings,
 * amulets, gloves and capes are added as simple items and their real bonuses are recorded in
 * {@link #ACCESSORIES} so the integrator can wire them up once those slots exist.
 */
public final class BossItems {
    /** Bonuses of items that need an equipment slot RuneCraft doesn't have yet. */
    public record Accessory(String slot, EquipmentStats stats) {
    }

    public static final Map<String, Accessory> ACCESSORIES = new LinkedHashMap<>();

    /** Every pet item added by this module (one per boss). */
    public static final Map<String, String> PETS = new LinkedHashMap<>();

    private BossItems() {
    }

    static EquipmentStats st(int stabA, int slashA, int crushA, int magicA, int rangedA, int stabD, int slashD, int crushD,
                             int magicD, int rangedD, int str, int rangedStr, int magicDmg, int prayer) {
        return new EquipmentStats(stabA, slashA, crushA, magicA, rangedA, stabD, slashD, crushD, magicD, rangedD, str, rangedStr,
                magicDmg, prayer);
    }

    private static Map<Skill, Integer> reqs(Object... kv) {
        Map<Skill, Integer> map = new EnumMap<>(Skill.class);
        for (int i = 0; i < kv.length; i += 2) {
            map.put((Skill) kv[i], (Integer) kv[i + 1]);
        }
        return map;
    }

    private static ItemDef weapon(ItemSink items, String id, String name, Kind kind, String shape, int color, int value,
                                  EquipmentStats s, int speed, AttackType type, WeaponClass wc, Object... req) {
        return items.add(id, name, kind, shape, color).value(value).stack(1).tier("dragon")
                .equipment(new EquipmentDef(Slot.WEAPON, s, reqs(req), speed, type, wc));
    }

    private static ItemDef wear(ItemSink items, String id, String name, Kind kind, String shape, int color, int value, Slot slot,
                                EquipmentStats s, Object... req) {
        return items.add(id, name, kind, shape, color).value(value).stack(1).tier("dragon")
                .equipment(new EquipmentDef(slot, s, reqs(req), 0, AttackType.CRUSH, WeaponClass.NONE));
    }

    private static ItemDef simple(ItemSink items, String id, String name, String shape, int color, int value, String examine) {
        return items.add(id, name, Kind.SIMPLE, shape, color).value(value).stack(1).examine(examine);
    }

    private static ItemDef accessory(ItemSink items, String id, String name, String shape, int color, int value, String slot,
                                     EquipmentStats s, String examine) {
        ACCESSORIES.put(id, new Accessory(slot, s));
        return simple(items, id, name, shape, color, value, examine);
    }

    private static void pet(ItemSink items, String id, String name, String boss, int color) {
        PETS.put(id, boss);
        items.add(id, name, Kind.SIMPLE, "pet", color).value(1).stack(1).examine("A loyal companion. (Pet from " + boss + ")");
    }

    private static void jar(ItemSink items, String id, String name, int color) {
        items.add(id, name, Kind.SIMPLE, "jar", color).value(1).stack(1).examine("A jar of something from a fearsome boss.");
    }

    public static void add(ItemSink items) {
        ACCESSORIES.clear();
        PETS.clear();
        // ---------------- Giant Mole ----------------
        items.add("mole_claw", "Mole claw", Kind.SIMPLE, "claw", 0xE0D0B0).value(1).examine("The claw of a giant mole.");
        items.add("mole_skin", "Mole skin", Kind.SIMPLE, "hide", 0x3A2A20).value(1).examine("The skin of a giant mole.");
        pet(items, "pet_baby_mole", "Baby mole", "Giant Mole", 0x3A2A20);

        // ---------------- Kalphite Queen ----------------
        simple(items, "kq_head", "Kq head", "head", 0x6A7A3A, 1, "The severed head of the Kalphite Queen.");
        wear(items, "dragon_chainbody", "Dragon chainbody", Kind.ARMOUR, "chainbody", 0xA02020, 250000, Slot.CHEST,
                st(0, 0, 0, -15, 0, 81, 93, 98, -3, 82, 0, 0, 0, 0), Skill.DEFENCE, 60)
                .examine("A series of connected metal rings.");
        jar(items, "jar_of_sand", "Jar of sand", 0xE0C080);
        pet(items, "pet_kalphite_princess", "Kalphite princess", "Kalphite Queen", 0x6A7A3A);

        // ---------------- Dagannoth Kings ----------------
        accessory(items, "berserker_ring", "Berserker ring", "ring", 0x8A2A2A, 650000, "ring",
                st(0, 0, 0, 0, 0, 4, 4, 4, 0, 0, 4, 0, 0, 0), "A ring reputed to bring out a berserk fury in its wearer.");
        accessory(items, "warrior_ring", "Warrior ring", "ring", 0xA0A0A0, 140000, "ring",
                st(0, 4, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0, 0, 0), "A ring reputed to bring out a warrior's spirit.");
        accessory(items, "seers_ring", "Seers ring", "ring", 0x3A6AC0, 160000, "ring",
                st(0, 0, 0, 4, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0), "A mysterious ring that can fill the wearer with magical power.");
        accessory(items, "archers_ring", "Archers ring", "ring", 0x3A8A3A, 160000, "ring",
                st(0, 0, 0, 0, 4, 0, 0, 0, 0, 4, 0, 0, 0, 0), "A fabled ring that improves the wearer's skill with a bow.");
        weapon(items, "mud_battlestaff", "Mud battlestaff", Kind.STAFF, "staff_orb", 0x6A5A3A, 40000,
                st(10, -1, 40, 12, 0, 2, 3, 1, 12, 0, 45, 0, 0, 0), 4, AttackType.CRUSH, WeaponClass.STAFF,
                Skill.MAGIC, 30, Skill.ATTACK, 30).examine("It's a slightly magical stick.");
        weapon(items, "seercull", "Seercull", Kind.BOW, "longbow", 0xC0C0D0, 100000,
                st(0, 0, 0, 0, 69, 0, 0, 0, 0, 0, 0, 0, 0, 0), 5, AttackType.RANGED, WeaponClass.BOW, Skill.RANGED, 50)
                .examine("An ancient Fremennik bow that was once used to battle the Moon Clan.");
        pet(items, "pet_dagannoth_rex", "Pet dagannoth rex", "Dagannoth Rex", 0x6A6A40);
        pet(items, "pet_dagannoth_prime", "Pet dagannoth prime", "Dagannoth Prime", 0x5A7A9A);
        pet(items, "pet_dagannoth_supreme", "Pet dagannoth supreme", "Dagannoth Supreme", 0x9A5A4A);

        // ---------------- Sarachnis ----------------
        weapon(items, "sarachnis_cudgel", "Sarachnis cudgel", Kind.MELEE_WEAPON, "club", 0x8A2A1A, 60000,
                st(-4, -4, 69, 0, 0, 0, 0, 0, 0, 0, 59, 0, 0, 0), 4, AttackType.CRUSH, WeaponClass.MELEE, Skill.ATTACK, 65)
                .examine("A heavy cudgel crafted from the spider queen's leg.");
        simple(items, "giant_egg_sac_full", "Giant egg sac (full)", "egg", 0xE0E0C0, 1, "Contains many red spiders' eggs.");
        jar(items, "jar_of_eyes", "Jar of eyes", 0xC0A0A0);
        pet(items, "pet_sraracha", "Sraracha", "Sarachnis", 0x8A2A1A);

        // ---------------- Zulrah ----------------
        items.add("zulrahs_scales", "Zulrah's scales", Kind.SIMPLE, "scales", 0x4A9A3A).value(1).stack(64000)
                .examine("Flakes of toxic snakeskin.");
        simple(items, "tanzanite_fang", "Tanzanite fang", "fang", 0x3AA0C0, 1, "A fang from the mouth of Zulrah.");
        simple(items, "magic_fang", "Magic fang", "fang", 0x6A3AA0, 1, "A fang from the mouth of Zulrah.");
        simple(items, "serpentine_visage", "Serpentine visage", "visage", 0x4A8A3A, 1, "A scaly face.");
        simple(items, "tanzanite_mutagen", "Tanzanite mutagen", "vial", 0x3AA0C0, 1, "Mutates a serpentine helm.");
        simple(items, "magma_mutagen", "Magma mutagen", "vial", 0xC03A20, 1, "Mutates a serpentine helm.");
        weapon(items, "toxic_blowpipe", "Toxic blowpipe", Kind.BOW, "blowpipe", 0x3AA0C0, 1500000,
                st(0, 0, 0, 0, 30, 0, 0, 0, 0, 0, 0, 20, 0, 0), 3, AttackType.RANGED, WeaponClass.BOW, Skill.RANGED, 75)
                .examine("It's very venomous. Special attack: Toxic Siphon heals 50% of the damage dealt.");
        weapon(items, "trident_of_the_seas", "Trident of the seas", Kind.STAFF, "trident", 0x3AA0B0, 400000,
                st(0, 0, 0, 25, 0, 2, 3, 1, 15, 0, 0, 0, 0, 0), 4, AttackType.MAGIC, WeaponClass.STAFF, Skill.MAGIC, 75)
                .examine("A powerful staff with a built-in spell: max hit is a third of your Magic level minus 5.");
        weapon(items, "trident_of_the_swamp", "Trident of the swamp", Kind.STAFF, "trident", 0x3A8A3A, 800000,
                st(0, 0, 0, 25, 0, 2, 3, 1, 15, 0, 0, 0, 0, 0), 4, AttackType.MAGIC, WeaponClass.STAFF, Skill.MAGIC, 75)
                .examine("A venomous trident: max hit is a third of your Magic level minus 2, and it can envenom.");
        for (String[] h : new String[][]{{"serpentine_helm", "Serpentine helm", "4A8A3A"}, {"tanzanite_helm", "Tanzanite helm", "3AA0C0"},
                {"magma_helm", "Magma helm", "C03A20"}}) {
            wear(items, h[0], h[1], Kind.ARMOUR, "full_helm", Integer.parseInt(h[2], 16), 2500000, Slot.HEAD,
                    st(0, 0, 0, -5, -5, 52, 55, 58, 0, 50, 0, 0, 0, 0), Skill.DEFENCE, 75)
                    .examine("A venomous helm. Protects the wearer from poison and venom.");
        }
        jar(items, "jar_of_swamp", "Jar of swamp", 0x3A6A3A);
        pet(items, "pet_snakeling", "Pet snakeling", "Zulrah", 0x4A9A3A);

        // ---------------- Vorkath ----------------
        simple(items, "vorkaths_head", "Vorkath's head", "head", 0xD8D0B8, 1, "The severed head of Vorkath. Ava would love this.");
        simple(items, "skeletal_visage", "Skeletal visage", "visage", 0xD8D0B8, 1, "The skeletal face of an undead dragon.");
        accessory(items, "dragonbone_necklace", "Dragonbone necklace", "necklace", 0xE0D8C0, 1, "neck",
                st(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 12), "Burying bones while wearing this restores prayer points.");
        wear(items, "dragonfire_ward", "Dragonfire ward", Kind.SHIELD, "ward", 0xC0C0A0, 1000000, Slot.SHIELD,
                st(0, 0, 0, -10, 7, 20, 24, 22, 10, 22, 0, 0, 0, 0), Skill.DEFENCE, 75)
                .examine("A heavy shield with a snarling, draconic visage. Protects against dragonfire.");
        jar(items, "jar_of_decay", "Jar of decay", 0x6A8A6A);
        pet(items, "pet_vorki", "Vorki", "Vorkath", 0xD8D0B8);

        // ---------------- Kraken & Thermonuclear smoke devil ----------------
        simple(items, "kraken_tentacle", "Kraken tentacle", "tentacle", 0x8A3A6A, 1, "A slimy tentacle.");
        weapon(items, "abyssal_tentacle", "Abyssal tentacle", Kind.MELEE_WEAPON, "whip", 0x6A2A5A, 2500000,
                st(0, 90, 0, 0, 0, 0, 0, 0, 0, 0, 86, 0, 0, 0), 4, AttackType.SLASH, WeaponClass.MELEE, Skill.ATTACK, 75)
                .examine("A weapon from the Abyss, embedded in a slimy tentacle.");
        jar(items, "jar_of_dirt", "Jar of dirt", 0x6A5A3A);
        pet(items, "pet_kraken", "Pet kraken", "Kraken", 0x8A3A6A);
        accessory(items, "occult_necklace", "Occult necklace", "necklace", 0x8A2A8A, 1000000, "neck",
                st(0, 0, 0, 12, 0, 0, 0, 0, 0, 0, 0, 0, 10, 0), "A brooding necklace: +10% magic damage.");
        weapon(items, "smoke_battlestaff", "Smoke battlestaff", Kind.STAFF, "staff_orb", 0x7A7A7A, 300000,
                st(10, -1, 40, 14, 0, 2, 3, 1, 14, 0, 45, 0, 10, 0), 4, AttackType.CRUSH, WeaponClass.STAFF,
                Skill.MAGIC, 40, Skill.ATTACK, 40).examine("A staff that gives 10% bonus accuracy and damage to standard spells.");
        jar(items, "jar_of_smoke", "Jar of smoke", 0x7A7A7A);
        pet(items, "pet_smoke_devil", "Pet smoke devil", "Thermonuclear smoke devil", 0x7A7A7A);

        // ---------------- Cerberus ----------------
        simple(items, "primordial_crystal", "Primordial crystal", "crystal", 0xC02A2A, 1, "Used to make primordial boots.");
        simple(items, "pegasian_crystal", "Pegasian crystal", "crystal", 0x2AC04A, 1, "Used to make pegasian boots.");
        simple(items, "eternal_crystal", "Eternal crystal", "crystal", 0x2A6AC0, 1, "Used to make eternal boots.");
        simple(items, "smouldering_stone", "Smouldering stone", "crystal", 0xE06010, 1, "Used to make infernal tools.");
        wear(items, "primordial_boots", "Primordial boots", Kind.ARMOUR, "boots", 0xA02A2A, 3000000, Slot.FEET,
                st(2, 2, 2, -4, -1, 22, 22, 22, 0, 0, 5, 0, 0, 0), Skill.DEFENCE, 75, Skill.STRENGTH, 75);
        wear(items, "pegasian_boots", "Pegasian boots", Kind.ARMOUR, "boots", 0x3A8A3A, 3000000, Slot.FEET,
                st(-12, -12, -12, -12, 12, 5, 5, 5, 5, 5, 0, 0, 0, 0), Skill.DEFENCE, 75, Skill.RANGED, 75);
        wear(items, "eternal_boots", "Eternal boots", Kind.ARMOUR, "boots", 0x3A5AB0, 3000000, Slot.FEET,
                st(0, 0, 0, 8, 0, 5, 5, 5, 8, 5, 0, 0, 0, 0), Skill.DEFENCE, 75, Skill.MAGIC, 75);
        jar(items, "jar_of_souls", "Jar of souls", 0x6A2A2A);
        pet(items, "pet_hellpuppy", "Hellpuppy", "Cerberus", 0x3A2A2A);

        // ---------------- Abyssal Sire ----------------
        simple(items, "unsired", "Unsired", "creature", 0x6A1A3A, 1, "Offer it to the Font of Consumption.");
        weapon(items, "abyssal_dagger", "Abyssal dagger", Kind.MELEE_WEAPON, "dagger", 0x5A2A5A, 2000000,
                st(75, 40, -4, 1, 0, 0, 0, 0, 1, 0, 75, 0, 0, 0), 4, AttackType.STAB, WeaponClass.MELEE, Skill.ATTACK, 70)
                .examine("Something sharp from the body of a defeated Abyssal Sire. Special: Abyssal Puncture hits twice.");
        weapon(items, "abyssal_bludgeon", "Abyssal bludgeon", Kind.MELEE_WEAPON, "bludgeon", 0x5A2A5A, 2000000,
                st(0, 0, 102, 0, 0, 0, 0, 0, 0, 0, 85, 0, 0, 0), 4, AttackType.CRUSH, WeaponClass.MELEE,
                Skill.ATTACK, 70, Skill.STRENGTH, 70).examine("Something sharp from the body of a defeated Abyssal Sire.");
        simple(items, "bludgeon_claw", "Bludgeon claw", "claw", 0x6A2A5A, 1, "One of the pieces of an abyssal bludgeon.");
        simple(items, "bludgeon_spine", "Bludgeon spine", "bone", 0x6A2A5A, 1, "One of the pieces of an abyssal bludgeon.");
        simple(items, "bludgeon_axon", "Bludgeon axon", "tentacle", 0x6A2A5A, 1, "One of the pieces of an abyssal bludgeon.");
        simple(items, "abyssal_head", "Abyssal head", "head", 0x6A1A3A, 1, "The head of an abyssal demon.");
        jar(items, "jar_of_miasma", "Jar of miasma", 0x6A1A3A);
        pet(items, "pet_abyssal_orphan", "Abyssal orphan", "Abyssal Sire", 0x6A1A3A);

        // ---------------- Alchemical Hydra ----------------
        simple(items, "hydras_eye", "Hydra's eye", "orb", 0xC02A2A, 1, "Part of the brimstone ring.");
        simple(items, "hydras_fang", "Hydra's fang", "fang", 0x2AA02A, 1, "Part of the brimstone ring.");
        simple(items, "hydras_heart", "Hydra's heart", "orb", 0x2A6AC0, 1, "Part of the brimstone ring.");
        simple(items, "hydra_claw", "Hydra's claw", "claw", 0x3A6A3A, 1, "Combine with a Zamorakian spear to make a dragon hunter lance.");
        simple(items, "hydra_leather", "Hydra leather", "leather", 0x3A6A3A, 1, "Shaped into ferocious gloves.");
        simple(items, "hydra_tail", "Hydra tail", "tail", 0x3A6A3A, 1, "Used to upgrade a bonecrusher.");
        simple(items, "alchemical_hydra_heads", "Alchemical hydra heads", "head", 0x3A8A3A, 1, "A trophy of the Alchemical Hydra.");
        accessory(items, "brimstone_ring", "Brimstone ring", "ring", 0xE0A030, 2000000, "ring",
                st(4, 4, 4, 6, 4, 0, 0, 0, 0, 0, 4, 0, 1, 0),
                "Attacks have a 25% chance to ignore 10% of the target's magic defence.");
        accessory(items, "ferocious_gloves", "Ferocious gloves", "gloves", 0x3A6A3A, 2000000, "hands",
                st(16, 16, 16, -16, -16, 0, 0, 0, 0, 0, 14, 0, 0, 0), "Gloves made from hydra leather.");
        weapon(items, "dragon_hunter_lance", "Dragon hunter lance", Kind.MELEE_WEAPON, "spear", 0x3A6A3A, 2000000,
                st(85, 65, 65, 0, 0, 13, 13, 12, 0, 13, 70, 0, 0, 0), 4, AttackType.STAB, WeaponClass.MELEE, Skill.ATTACK, 78)
                .examine("Grants 20% accuracy and damage against dragons.");
        jar(items, "jar_of_chemicals", "Jar of chemicals", 0x3A8A3A);
        pet(items, "pet_ikkle_hydra", "Ikkle hydra", "Alchemical Hydra", 0x3A8A3A);

        // ---------------- Grotesque Guardians ----------------
        weapon(items, "granite_maul", "Granite maul", Kind.MELEE_WEAPON, "warhammer", 0x7A7A7A, 300000,
                st(0, 0, 81, 0, 0, 0, 0, 0, 0, 0, 79, 0, 0, 0), 7, AttackType.CRUSH, WeaponClass.MELEE,
                Skill.ATTACK, 50, Skill.STRENGTH, 50).examine("Simplicity is the best weapon. Special: Quick Smash.");
        weapon(items, "granite_hammer", "Granite hammer", Kind.MELEE_WEAPON, "warhammer", 0x8A8A8A, 300000,
                st(0, 0, 57, 0, 0, 0, 0, 0, 0, 0, 56, 0, 0, 0), 4, AttackType.CRUSH, WeaponClass.MELEE,
                Skill.ATTACK, 50, Skill.STRENGTH, 50).examine("A hammer that can finish off gargoyles.");
        accessory(items, "granite_ring", "Granite ring", "ring", 0x7A7A7A, 300000, "ring",
                st(4, 4, 4, 0, 0, 4, 4, 4, 0, 4, 4, 0, 0, 0), "A ring carved from granite.");
        accessory(items, "granite_gloves", "Granite gloves", "gloves", 0x7A7A7A, 300000, "hands",
                st(11, 11, 11, 0, 0, 6, 6, 6, 0, 6, 8, 0, 0, 0), "Gloves carved from granite.");
        simple(items, "black_tourmaline_core", "Black tourmaline core", "core", 0x2A2A2A, 1, "Combine with bandos boots.");
        wear(items, "guardian_boots", "Guardian boots", Kind.ARMOUR, "boots", 0x4A4A4A, 3000000, Slot.FEET,
                st(0, 0, 0, -3, -1, 32, 32, 32, -3, 24, 1, 0, 0, 1), Skill.DEFENCE, 75);
        items.add("granite_dust", "Granite dust", Kind.SIMPLE, "essence", 0x9A9A9A).value(1).stack(64000);
        jar(items, "jar_of_stone", "Jar of stone", 0x7A7A7A);
        pet(items, "pet_noon", "Noon", "Grotesque Guardians", 0x7A7A8A);

        // ---------------- Corporeal Beast ----------------
        EquipmentStats spirit = st(0, 0, 0, 0, 0, 39, 41, 50, 1, 45, 0, 0, 0, 1);
        wear(items, "spirit_shield", "Spirit shield", Kind.SHIELD, "spirit_shield", 0xC0C0D0, 70000, Slot.SHIELD, spirit,
                Skill.DEFENCE, 45, Skill.PRAYER, 55).examine("An ethereal shield.");
        simple(items, "holy_elixir", "Holy elixir", "vial", 0xE0E0F0, 1, "A bottle of holy elixir.");
        wear(items, "blessed_spirit_shield", "Blessed spirit shield", Kind.SHIELD, "spirit_shield", 0xE0D8A0, 1400000, Slot.SHIELD,
                st(0, 0, 0, 0, 0, 53, 55, 73, 2, 52, 0, 0, 0, 3), Skill.DEFENCE, 70, Skill.PRAYER, 60)
                .examine("An ethereal shield that has been blessed with holy powers.");
        wear(items, "spectral_spirit_shield", "Spectral spirit shield", Kind.SHIELD, "spirit_shield", 0xA0E0E0, 1500000, Slot.SHIELD,
                st(0, 0, 0, 0, 0, 53, 55, 73, 30, 52, 0, 0, 0, 3), Skill.DEFENCE, 75, Skill.PRAYER, 70, Skill.MAGIC, 65)
                .examine("Reduces prayer drains from attacks by 50%.");
        wear(items, "arcane_spirit_shield", "Arcane spirit shield", Kind.SHIELD, "spirit_shield", 0xE0B040, 1500000, Slot.SHIELD,
                st(0, 0, 0, 20, 0, 70, 72, 75, 20, 52, 0, 0, 5, 3), Skill.DEFENCE, 75, Skill.PRAYER, 70, Skill.MAGIC, 65)
                .examine("An ethereal shield with an arcane sigil attached to it.");
        wear(items, "elysian_spirit_shield", "Elysian spirit shield", Kind.SHIELD, "spirit_shield", 0xF0E0A0, 1500000, Slot.SHIELD,
                st(0, 0, 0, 0, 0, 63, 65, 75, 2, 57, 0, 0, 0, 3), Skill.DEFENCE, 75, Skill.PRAYER, 75)
                .examine("Has a 70% chance to reduce incoming damage by 25%.");
        simple(items, "spectral_sigil", "Spectral sigil", "sigil", 0xA0E0E0, 1, "A sigil in the shape of a spectral symbol.");
        simple(items, "arcane_sigil", "Arcane sigil", "sigil", 0xE0B040, 1, "A sigil in the shape of an arcane rune.");
        simple(items, "elysian_sigil", "Elysian sigil", "sigil", 0xF0E0A0, 1, "A sigil in the shape of an elysian sign.");
        jar(items, "jar_of_spirits", "Jar of spirits", 0xA0C0A0);
        pet(items, "pet_dark_core", "Pet dark core", "Corporeal Beast", 0x2A2A3A);

        // ---------------- Skotizo ----------------
        simple(items, "dark_totem_base", "Dark totem base", "totem", 0x3A2A3A, 1, "Part of a dark totem.");
        simple(items, "dark_totem_middle", "Dark totem middle", "totem", 0x3A2A3A, 1, "Part of a dark totem.");
        simple(items, "dark_totem_top", "Dark totem top", "totem", 0x3A2A3A, 1, "Part of a dark totem.");
        simple(items, "dark_totem", "Dark totem", "totem", 0x4A2A4A, 1, "Use it on the altar in the Catacombs of Kourend.");
        simple(items, "dark_claw", "Dark claw", "claw", 0x2A1A2A, 1, "A claw from a powerful demon.");
        items.add("ancient_shard", "Ancient shard", Kind.SIMPLE, "shard", 0x6A8A9A).value(1).stack(64000)
                .examine("A shard of an ancient power.");
        jar(items, "jar_of_darkness", "Jar of darkness", 0x1A1A2A);
        pet(items, "pet_skotos", "Skotos", "Skotizo", 0x2A1A2A);

        // ---------------- Hespori ----------------
        items.add("bottomless_compost_bucket", "Bottomless compost bucket", Kind.TOOL, "bucket", 0x6A5A3A).value(1).stack(1)
                .examine("A compost bucket that never runs out.");
        for (String[] s : new String[][]{{"iasor_seed", "Iasor seed", "C04A8A"}, {"kronos_seed", "Kronos seed", "8A4A2A"},
                {"attas_seed", "Attas seed", "4A8AC0"}}) {
            items.add(s[0], s[1], Kind.SIMPLE, "seed", Integer.parseInt(s[2], 16)).value(1).examine("An anima seed.");
        }

        // ---------------- The Gauntlet (Hunllef) & Zalcano ----------------
        items.add("crystal_shard", "Crystal shard", Kind.SIMPLE, "shard", 0x9AD8F0).value(1).stack(64000)
                .examine("A shard of crystal used in Prifddinas crafting.");
        simple(items, "crystal_weapon_seed", "Crystal weapon seed", "seed", 0x9AD8F0, 1, "Sing it into a crystal weapon.");
        simple(items, "crystal_armour_seed", "Crystal armour seed", "seed", 0x9AD8F0, 1, "Sing it into crystal armour.");
        simple(items, "enhanced_crystal_weapon_seed", "Enhanced crystal weapon seed", "seed", 0xB0F0FF, 1,
                "Sing it into a bow of faerdhinen or a blade of saeldor.");
        simple(items, "crystal_tool_seed", "Crystal tool seed", "seed", 0x9AD8F0, 1, "Sing it into a crystal tool.");
        weapon(items, "bow_of_faerdhinen", "Bow of faerdhinen", Kind.BOW, "crystal_bow", 0x9AD8F0, 3000000,
                st(0, 0, 0, 0, 128, 0, 0, 0, 0, 0, 0, 106, 0, 0), 4, AttackType.RANGED, WeaponClass.BOW,
                Skill.RANGED, 80, Skill.AGILITY, 70).examine("A bow of elven crystal. Needs no ammunition.");
        weapon(items, "blade_of_saeldor", "Blade of saeldor", Kind.MELEE_WEAPON, "crystal_sword", 0x9AD8F0, 3000000,
                st(55, 94, 0, 0, 0, 0, 0, 0, 0, 0, 89, 0, 0, 0), 4, AttackType.SLASH, WeaponClass.MELEE,
                Skill.ATTACK, 75, Skill.AGILITY, 50).examine("A blade of elven crystal.");
        wear(items, "crystal_helm", "Crystal helm", Kind.ARMOUR, "full_helm", 0x9AD8F0, 500000, Slot.HEAD,
                st(0, 0, 0, -10, 9, 12, 8, 14, 10, 18, 0, 0, 0, 2), Skill.DEFENCE, 70, Skill.AGILITY, 50);
        wear(items, "crystal_body", "Crystal body", Kind.ARMOUR, "platebody", 0x9AD8F0, 1500000, Slot.CHEST,
                st(0, 0, 0, -18, 31, 46, 38, 48, 44, 68, 0, 0, 0, 3), Skill.DEFENCE, 70, Skill.AGILITY, 50);
        wear(items, "crystal_legs", "Crystal legs", Kind.ARMOUR, "platelegs", 0x9AD8F0, 1000000, Slot.LEGS,
                st(0, 0, 0, -12, 18, 26, 21, 30, 34, 38, 0, 0, 0, 2), Skill.DEFENCE, 70, Skill.AGILITY, 50);
        simple(items, "gauntlet_cape", "Gauntlet cape", "cape", 0x9AD8F0, 1, "Earned by completing the Gauntlet.");
        pet(items, "pet_youngllef", "Youngllef", "Crystalline Hunllef", 0x9AD8F0);
        simple(items, "zalcano_shard", "Zalcano shard", "shard", 0xE07020, 1, "Combine with a dragon pickaxe for a crystal pickaxe.");
        pet(items, "pet_smolcano", "Smolcano", "Zalcano", 0x5A4A4A);

        // ---------------- The Nightmare ----------------
        weapon(items, "nightmare_staff", "Nightmare staff", Kind.STAFF, "staff_orb", 0x4A2A6A, 1000000,
                st(0, 0, 0, 16, 0, 0, 0, 0, 16, 0, 0, 0, 15, 0), 4, AttackType.CRUSH, WeaponClass.STAFF, Skill.MAGIC, 65)
                .examine("A staff that was once wielded by the Nightmare.");
        String[][] orbs = {{"harmonised", "C0A040", "Harmonised nightmare staff"}, {"volatile", "C03A3A", "Volatile nightmare staff"},
                {"eldritch", "4A6AC0", "Eldritch nightmare staff"}};
        for (String[] o : orbs) {
            int c = Integer.parseInt(o[1], 16);
            simple(items, o[0] + "_orb", Character.toUpperCase(o[0].charAt(0)) + o[0].substring(1) + " orb", "orb", c, 1,
                    "Attach it to a Nightmare staff.");
            weapon(items, o[0] + "_nightmare_staff", o[2], Kind.STAFF, "staff_orb", c, 30000000,
                    st(0, 0, 0, 16, 0, 0, 0, 0, 16, 0, 0, 0, 15, 0), 4, AttackType.MAGIC, WeaponClass.STAFF, Skill.MAGIC, 75)
                    .examine("A Nightmare staff empowered with a " + o[0] + " orb.");
        }
        wear(items, "inquisitors_great_helm", "Inquisitor's great helm", Kind.ARMOUR, "full_helm", 0x9A8A6A, 5000000, Slot.HEAD,
                st(-4, -4, 8, -5, -5, 12, 14, 8, -3, 11, 4, 0, 0, 1), Skill.DEFENCE, 30, Skill.STRENGTH, 70);
        wear(items, "inquisitors_hauberk", "Inquisitor's hauberk", Kind.ARMOUR, "platebody", 0x9A8A6A, 15000000, Slot.CHEST,
                st(-12, -12, 22, -18, -14, 58, 66, 40, -4, 52, 6, 0, 0, 2), Skill.DEFENCE, 30, Skill.STRENGTH, 70);
        wear(items, "inquisitors_plateskirt", "Inquisitor's plateskirt", Kind.ARMOUR, "platelegs", 0x9A8A6A, 15000000, Slot.LEGS,
                st(-10, -10, 12, -14, -10, 32, 40, 26, -4, 34, 4, 0, 0, 2), Skill.DEFENCE, 30, Skill.STRENGTH, 70);
        weapon(items, "inquisitors_mace", "Inquisitor's mace", Kind.MELEE_WEAPON, "mace", 0x9A8A6A, 40000000,
                st(52, -4, 95, 0, 0, 0, 0, 0, 0, 0, 89, 0, 0, 2), 4, AttackType.CRUSH, WeaponClass.MELEE, Skill.ATTACK, 80)
                .examine("Crush bonus is boosted by each piece of inquisitor's armour worn.");
        jar(items, "jar_of_dreams", "Jar of dreams", 0x4A2A6A);
        simple(items, "parasitic_egg", "Parasitic egg", "egg", 0x6A8A4A, 1, "A fleshy egg from Phosani's Nightmare.");
        pet(items, "pet_little_nightmare", "Little nightmare", "The Nightmare", 0x2A2A3A);

        // ---------------- Phantom Muspah ----------------
        simple(items, "venator_shard", "Venator shard", "shard", 0x6A9AC0, 1, "Five of these make a venator bow.");
        weapon(items, "venator_bow", "Venator bow", Kind.BOW, "longbow", 0x6A9AC0, 30000000,
                st(0, 0, 0, 0, 90, 0, 0, 0, 0, 0, 0, 2, 0, 0), 5, AttackType.RANGED, WeaponClass.BOW, Skill.RANGED, 80)
                .examine("Arrows bounce to two nearby enemies.");
        simple(items, "ancient_icon", "Ancient icon", "icon", 0x9AC0E0, 1, "A relic of the Ancient Zarosian empire.");
        items.add("ancient_essence", "Ancient essence", Kind.SIMPLE, "essence", 0x8A6AC0).value(1).stack(64000)
                .examine("Pure essence of Zarosian magic.");
        pet(items, "pet_muphin", "Muphin", "Phantom Muspah", 0x6A7A9A);

        // ---------------- Desert Treasure II ----------------
        simple(items, "chromium_ingot", "Chromium ingot", "bar", 0x9AB0C0, 1, "Three are needed to forge a DT2 ring.");
        simple(items, "awakeners_orb", "Awakener's orb", "orb", 0xC03A3A, 1, "Awakens a Desert Treasure II boss.");
        for (String[] v : new String[][]{{"ultor", "8A2A2A"}, {"magus", "3A3AA0"}, {"venator", "3A8A3A"}, {"bellator", "A08A3A"}}) {
            String cap = Character.toUpperCase(v[0].charAt(0)) + v[0].substring(1);
            simple(items, v[0] + "_vestige", cap + " vestige", "gem", Integer.parseInt(v[1], 16), 1,
                    "An ancient vestige. Forge it with chromium ingots and a Fremennik ring.");
        }
        accessory(items, "ultor_ring", "Ultor ring", "ring", 0x8A2A2A, 40000000, "ring",
                st(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 12, 0, 0, 0), "The improved berserker ring.");
        accessory(items, "bellator_ring", "Bellator ring", "ring", 0xA08A3A, 10000000, "ring",
                st(0, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), "The improved warrior ring.");
        accessory(items, "magus_ring", "Magus ring", "ring", 0x3A3AA0, 10000000, "ring",
                st(0, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0), "The improved seers ring.");
        accessory(items, "venator_ring", "Venator ring", "ring", 0x3A8A3A, 10000000, "ring",
                st(0, 0, 0, 0, 10, 0, 0, 0, 0, 0, 0, 2, 0, 0), "The improved archers ring.");
        wear(items, "virtus_mask", "Virtus mask", Kind.ARMOUR, "cowl", 0x3A2A5A, 4000000, Slot.HEAD,
                st(0, 0, 0, 8, -5, 0, 0, 0, 3, 0, 0, 0, 1, 0), Skill.MAGIC, 78, Skill.DEFENCE, 75);
        wear(items, "virtus_robe_top", "Virtus robe top", Kind.ARMOUR, "leather_body", 0x3A2A5A, 15000000, Slot.CHEST,
                st(0, 0, 0, 35, -11, 0, 0, 0, 30, 0, 0, 0, 2, 0), Skill.MAGIC, 78, Skill.DEFENCE, 75);
        wear(items, "virtus_robe_bottom", "Virtus robe bottom", Kind.ARMOUR, "chaps", 0x3A2A5A, 15000000, Slot.LEGS,
                st(0, 0, 0, 26, -9, 0, 0, 0, 22, 0, 0, 0, 2, 0), Skill.MAGIC, 78, Skill.DEFENCE, 75);
        simple(items, "executioners_axe_head", "Executioner's axe head", "axe", 0x8A2A2A, 1, "Part of the soulreaper axe (Vardorvis).");
        simple(items, "leviathans_lure", "Leviathan's lure", "orb", 0x2A6A8A, 1, "Part of the soulreaper axe (The Leviathan).");
        simple(items, "sirens_staff", "Siren's staff", "staff", 0x4A2A6A, 1, "Part of the soulreaper axe (The Whisperer).");
        simple(items, "eye_of_the_duke", "Eye of the duke", "orb", 0x6A8A3A, 1, "Part of the soulreaper axe (Duke Sucellus).");
        weapon(items, "soulreaper_axe", "Soulreaper axe", Kind.MELEE_WEAPON, "battleaxe", 0x4A2A3A, 60000000,
                st(28, 134, 66, 0, 0, 0, 0, 0, 0, 0, 121, 0, 0, 0), 5, AttackType.SLASH, WeaponClass.MELEE,
                Skill.ATTACK, 80, Skill.STRENGTH, 80).examine("Each hit stacks up to 5 souls, boosting Strength by 6% each.");
        pet(items, "pet_baron", "Baron", "Duke Sucellus", 0x5A4A6A);
        pet(items, "pet_lilviathan", "Lil'viathan", "The Leviathan", 0x2A5A6A);
        pet(items, "pet_wisp", "Wisp", "The Whisperer", 0x2A3A5A);
        pet(items, "pet_butch", "Butch", "Vardorvis", 0x6A2A2A);

        // ---------------- Scurrius ----------------
        simple(items, "scurrius_spine", "Scurrius' spine", "bone", 0xD8C8A8, 1, "The spine of the rat king.");
        pet(items, "pet_scurry", "Scurry", "Scurrius", 0x6A5A4A);

        // ---------------- Araxxor ----------------
        simple(items, "noxious_point", "Noxious point", "blade", 0x6A8A3A, 1, "Part of the noxious halberd.");
        simple(items, "noxious_blade", "Noxious blade", "blade", 0x6A8A3A, 1, "Part of the noxious halberd.");
        simple(items, "noxious_pommel", "Noxious pommel", "orb", 0x6A8A3A, 1, "Part of the noxious halberd.");
        weapon(items, "noxious_halberd", "Noxious halberd", Kind.MELEE_WEAPON, "halberd", 0x6A8A3A, 30000000,
                st(85, 118, 0, -4, 0, -1, 4, 5, 0, 0, 125, 0, 0, 0), 5, AttackType.SLASH, WeaponClass.MELEE,
                Skill.ATTACK, 80, Skill.STRENGTH, 80).examine("A two-handed halberd. Special: Noxious Swipe.");
        simple(items, "araxyte_fang", "Araxyte fang", "fang", 0xE0D040, 1, "Used to upgrade an amulet of torture.");
        simple(items, "coagulated_venom", "Coagulated venom", "vial", 0x6A8A3A, 1, "Can be used to make weapons venomous.");
        simple(items, "araxyte_head", "Araxyte head", "head", 0x4A5A3A, 1, "The head of Araxxor.");
        simple(items, "echo_crystal", "Echo crystal", "crystal", 0xA0E0C0, 1, "A crystal echoing with the screams of spiders.");
        jar(items, "jar_of_venom", "Jar of venom", 0x6A8A3A);
        pet(items, "pet_nid", "Nid", "Araxxor", 0x4A5A3A);

        // ---------------- Varlamore ----------------
        simple(items, "hueycoatl_hide", "Hueycoatl hide", "hide", 0x3A9A8A, 1, "Used to craft Hueycoatl armour.");
        simple(items, "tome_of_earth", "Tome of earth", "book", 0x6A5A3A, 1, "Boosts earth spells.");
        weapon(items, "dragon_hunter_wand", "Dragon hunter wand", Kind.STAFF, "wand", 0x3A9A8A, 20000000,
                st(0, 0, 0, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), 4, AttackType.MAGIC, WeaponClass.STAFF, Skill.MAGIC, 82)
                .examine("Grants 50% accuracy and 20% damage with magic against dragons.");
        pet(items, "pet_huberte", "Huberte", "The Hueycoatl", 0x3A9A8A);
        weapon(items, "glacial_temotli", "Glacial temotli", Kind.MELEE_WEAPON, "club", 0xA0D0F0, 1000000,
                st(0, 0, 76, 0, 0, 0, 0, 0, 0, 0, 72, 0, 0, 0), 4, AttackType.CRUSH, WeaponClass.MELEE, Skill.ATTACK, 70)
                .examine("A frozen club from Amoxliatl.");
        accessory(items, "pendant_of_ates", "Pendant of ates", "amulet", 0xA0D0F0, 1000000, "neck",
                st(2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 0, 0, 2), "Teleports to Varlamore's ancient sites.");
        simple(items, "frozen_tear", "Frozen tear", "gem", 0xA0D0F0, 1, "Charges the pendant of ates.");
        pet(items, "pet_moxi", "Moxi", "Amoxliatl", 0xA0D0F0);
        simple(items, "deadeye_prayer_scroll", "Deadeye prayer scroll", "note", 0x8AB04A, 1, "Teaches the Deadeye prayer.");
        simple(items, "mystic_vigour_prayer_scroll", "Mystic vigour prayer scroll", "note", 0x4A6AC0, 1,
                "Teaches the Mystic Vigour prayer.");
        accessory(items, "giantsoul_amulet", "Giantsoul amulet", "amulet", 0xC0A040, 1000000, "neck",
                st(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), "Absorbs the soul of giants.");
        simple(items, "fire_element_staff_crown", "Fire element staff crown", "crown", 0xE05010, 1, "Upgrades a fire staff.");
        simple(items, "ice_element_staff_crown", "Ice element staff crown", "crown", 0x80C0F0, 1, "Upgrades a water staff.");
        pet(items, "pet_bran", "Bran", "Royal Titans", 0xE07030);

        // ---------------- Fortis Colosseum ----------------
        wear(items, "sunfire_fanatic_helm", "Sunfire fanatic helm", Kind.ARMOUR, "full_helm", 0xE0A030, 5000000, Slot.HEAD,
                st(0, 0, 0, -5, -2, 43, 45, 38, -1, 45, 0, 0, 0, 1), Skill.DEFENCE, 80);
        wear(items, "sunfire_fanatic_cuirass", "Sunfire fanatic cuirass", Kind.ARMOUR, "platebody", 0xE0A030, 10000000, Slot.CHEST,
                st(0, 0, 0, -18, -6, 105, 98, 93, -2, 118, 0, 0, 0, 1), Skill.DEFENCE, 80);
        wear(items, "sunfire_fanatic_chausses", "Sunfire fanatic chausses", Kind.ARMOUR, "platelegs", 0xE0A030, 10000000, Slot.LEGS,
                st(0, 0, 0, -12, -4, 78, 74, 72, -2, 84, 0, 0, 0, 1), Skill.DEFENCE, 80);
        weapon(items, "tonalztics_of_ralos", "Tonalztics of ralos", Kind.BOW, "chakram", 0xE0A030, 30000000,
                st(0, 0, 0, 0, 69, 0, 0, 0, 0, 0, 0, 75, 0, 0), 6, AttackType.RANGED, WeaponClass.BOW, Skill.RANGED, 75)
                .examine("Thrown twice per attack.");
        accessory(items, "dizanas_quiver", "Dizana's quiver", "cape", 0x8A3A2A, 1, "cape",
                st(0, 0, 0, 0, 18, 3, 3, 3, 3, 3, 0, 3, 0, 0), "A quiver earned in the Fortis Colosseum.");
        items.add("sunfire_splinters", "Sunfire splinters", Kind.SIMPLE, "shard", 0xE0A030).value(1).stack(64000)
                .examine("Smouldering splinters of sunfire.");
        pet(items, "pet_smol_heredit", "Smol heredit", "Sol Heredit", 0xC0A040);

        ModernBosses.items(items);
    }

    // Package-private access for ModernBosses' item block.
    static ItemDef weaponItem(ItemSink items, String id, String name, Kind kind, String shape, int color, int value,
                              EquipmentStats s, int speed, AttackType type, WeaponClass wc, Object... req) {
        return weapon(items, id, name, kind, shape, color, value, s, speed, type, wc, req);
    }

    static ItemDef wearItem(ItemSink items, String id, String name, Kind kind, String shape, int color, int value, Slot slot,
                            EquipmentStats s, Object... req) {
        return wear(items, id, name, kind, shape, color, value, slot, s, req);
    }

    static ItemDef simpleItem(ItemSink items, String id, String name, String shape, int color, int value, String examine) {
        return simple(items, id, name, shape, color, value, examine);
    }

    static void petItem(ItemSink items, String id, String name, String boss, int color) {
        pet(items, id, name, boss, color);
    }

    static void jarItem(ItemSink items, String id, String name, int color) {
        jar(items, id, name, color);
    }

    /** Combinations and crafting of boss uniques. */
    public static void recipes(Consumer<Recipe> recipes) {
        // Zulrah
        recipes.accept(Recipe.builder("make_toxic_blowpipe", MenuType.FLETCHING, "Toxic blowpipe").skill(Skill.FLETCHING, 78, 120)
                .in("tanzanite_fang", 1).out("toxic_blowpipe", 1).tool("chisel").ticks(3).build());
        recipes.accept(Recipe.builder("make_serpentine_helm", MenuType.GEM_CUTTING, "Serpentine helm").skill(Skill.CRAFTING, 52, 120)
                .in("serpentine_visage", 1).out("serpentine_helm", 1).tool("chisel").ticks(3).build());
        recipes.accept(Recipe.builder("make_trident_of_the_swamp", MenuType.GEM_CUTTING, "Trident of the swamp")
                .skill(Skill.CRAFTING, 59, 350).in("magic_fang", 1).in("trident_of_the_seas", 1).out("trident_of_the_swamp", 1)
                .tool("chisel").ticks(3).build());
        recipes.accept(Recipe.builder("make_tanzanite_helm", MenuType.GEM_CUTTING, "Tanzanite helm")
                .in("serpentine_helm", 1).in("tanzanite_mutagen", 1).out("tanzanite_helm", 1).build());
        recipes.accept(Recipe.builder("make_magma_helm", MenuType.GEM_CUTTING, "Magma helm")
                .in("serpentine_helm", 1).in("magma_mutagen", 1).out("magma_helm", 1).build());
        // Vorkath
        recipes.accept(Recipe.builder("make_dragonfire_ward", MenuType.SMITHING, "Dragonfire ward").skill(Skill.SMITHING, 90, 2000)
                .in("skeletal_visage", 1).in("anti_dragon_shield", 1).out("dragonfire_ward", 1).tool("hammer").ticks(5).build());
        // King Black Dragon / Vorkath / wyrms: draconic visage forged onto an anti-dragon shield (OSRS: 90 Smithing, 2,000 XP).
        recipes.accept(Recipe.builder("make_dragonfire_shield", MenuType.SMITHING, "Dragonfire shield").skill(Skill.SMITHING, 90, 2000)
                .in("draconic_visage", 1).in("anti_dragon_shield", 1).out("dragonfire_shield", 1).tool("hammer").ticks(5).build());
        // Haunted Mine's salve shard: chiselled and strung into a salve amulet; Tarn's diary enchants it to (e).
        recipes.accept(Recipe.builder("make_salve_amulet", MenuType.GEM_CUTTING, "Salve amulet")
                .in("salve_shard", 1).in("ball_of_wool", 1).out("salve_amulet", 1).tool("chisel").ticks(3).build());
        recipes.accept(Recipe.builder("make_salve_amulet_e", MenuType.GEM_CUTTING, "Salve amulet (e)")
                .in("salve_amulet", 1).out("salve_amulet_e", 1).tool("tarns_diary").ticks(3).build());
        // Kraken
        recipes.accept(Recipe.builder("make_abyssal_tentacle", MenuType.LEATHERWORKING, "Abyssal tentacle")
                .in("abyssal_whip", 1).in("kraken_tentacle", 1).out("abyssal_tentacle", 1).build());
        // Abyssal Sire
        recipes.accept(Recipe.builder("make_abyssal_bludgeon", MenuType.SMITHING, "Abyssal bludgeon")
                .in("bludgeon_claw", 1).in("bludgeon_spine", 1).in("bludgeon_axon", 1).out("abyssal_bludgeon", 1).build());
        // Alchemical Hydra
        recipes.accept(Recipe.builder("make_brimstone_ring", MenuType.GEM_CUTTING, "Brimstone ring")
                .in("hydras_eye", 1).in("hydras_fang", 1).in("hydras_heart", 1).out("brimstone_ring", 1).build());
        recipes.accept(Recipe.builder("make_ferocious_gloves", MenuType.LEATHERWORKING, "Ferocious gloves").skill(Skill.CRAFTING, 72, 0)
                .in("hydra_leather", 1).out("ferocious_gloves", 1).tool("needle").ticks(4).build());
        // Approximation: the real lance uses a Zamorakian hasta (an upgraded Zamorakian spear).
        recipes.accept(Recipe.builder("make_dragon_hunter_lance", MenuType.SMITHING, "Dragon hunter lance")
                .in("hydra_claw", 1).in("zamorakian_spear", 1).out("dragon_hunter_lance", 1).build());
        // Grotesque Guardians
        recipes.accept(Recipe.builder("make_guardian_boots", MenuType.SMITHING, "Guardian boots")
                .in("black_tourmaline_core", 1).in("bandos_boots", 1).out("guardian_boots", 1).build());
        // Corporeal Beast (the real ones also need 85/90 Prayer at an altar).
        recipes.accept(Recipe.builder("make_blessed_spirit_shield", MenuType.SMITHING, "Blessed spirit shield")
                .skill(Skill.PRAYER, 85, 0).in("spirit_shield", 1).in("holy_elixir", 1).out("blessed_spirit_shield", 1).build());
        for (String sigil : new String[]{"spectral", "arcane", "elysian"}) {
            recipes.accept(Recipe.builder("make_" + sigil + "_spirit_shield", MenuType.SMITHING,
                            Character.toUpperCase(sigil.charAt(0)) + sigil.substring(1) + " spirit shield")
                    .skill(Skill.SMITHING, 85, 0).in("blessed_spirit_shield", 1).in(sigil + "_sigil", 1)
                    .out(sigil + "_spirit_shield", 1).tool("hammer").ticks(5).build());
        }
        // Skotizo
        recipes.accept(Recipe.builder("make_dark_totem", MenuType.SMITHING, "Dark totem")
                .in("dark_totem_base", 1).in("dark_totem_middle", 1).in("dark_totem_top", 1).out("dark_totem", 1).build());
        // Crystal singing (Prifddinas): 100 shards per enhanced weapon; armour 50/100/150 shards and 1/2/3 seeds.
        recipes.accept(Recipe.builder("sing_bow_of_faerdhinen", MenuType.SMITHING, "Bow of faerdhinen").skill(Skill.CRAFTING, 82, 5000)
                .in("enhanced_crystal_weapon_seed", 1).in("crystal_shard", 100).out("bow_of_faerdhinen", 1).build());
        recipes.accept(Recipe.builder("sing_blade_of_saeldor", MenuType.SMITHING, "Blade of saeldor").skill(Skill.SMITHING, 82, 5000)
                .in("enhanced_crystal_weapon_seed", 1).in("crystal_shard", 100).out("blade_of_saeldor", 1).build());
        recipes.accept(Recipe.builder("sing_crystal_helm", MenuType.SMITHING, "Crystal helm").skill(Skill.CRAFTING, 70, 2500)
                .in("crystal_armour_seed", 1).in("crystal_shard", 50).out("crystal_helm", 1).build());
        recipes.accept(Recipe.builder("sing_crystal_legs", MenuType.SMITHING, "Crystal legs").skill(Skill.CRAFTING, 72, 5000)
                .in("crystal_armour_seed", 2).in("crystal_shard", 100).out("crystal_legs", 1).build());
        recipes.accept(Recipe.builder("sing_crystal_body", MenuType.SMITHING, "Crystal body").skill(Skill.CRAFTING, 74, 7500)
                .in("crystal_armour_seed", 3).in("crystal_shard", 150).out("crystal_body", 1).build());
        // Nightmare
        for (String o : new String[]{"harmonised", "volatile", "eldritch"}) {
            recipes.accept(Recipe.builder("make_" + o + "_nightmare_staff", MenuType.SMITHING,
                            Character.toUpperCase(o.charAt(0)) + o.substring(1) + " nightmare staff")
                    .in("nightmare_staff", 1).in(o + "_orb", 1).out(o + "_nightmare_staff", 1).build());
        }
        // Phantom Muspah
        recipes.accept(Recipe.builder("make_venator_bow", MenuType.FLETCHING, "Venator bow").skill(Skill.FLETCHING, 80, 500)
                .in("venator_shard", 5).out("venator_bow", 1).tool("knife").ticks(4).build());
        // Desert Treasure II rings (forged at the Ancient forge) and the soulreaper axe.
        String[][] rings = {{"ultor", "berserker_ring"}, {"bellator", "warrior_ring"}, {"magus", "seers_ring"},
                {"venator", "archers_ring"}};
        for (String[] r : rings) {
            recipes.accept(Recipe.builder("forge_" + r[0] + "_ring", MenuType.SMITHING,
                            Character.toUpperCase(r[0].charAt(0)) + r[0].substring(1) + " ring")
                    .skill(Skill.CRAFTING, 80, 0).in(r[0] + "_vestige", 1).in("chromium_ingot", 3).in(r[1], 1)
                    .out(r[0] + "_ring", 1).tool("hammer").ticks(5).build());
        }
        recipes.accept(Recipe.builder("make_soulreaper_axe", MenuType.SMITHING, "Soulreaper axe")
                .in("executioners_axe_head", 1).in("leviathans_lure", 1).in("sirens_staff", 1).in("eye_of_the_duke", 1)
                .out("soulreaper_axe", 1).build());
        // Araxxor
        recipes.accept(Recipe.builder("make_noxious_halberd", MenuType.SMITHING, "Noxious halberd")
                .in("noxious_point", 1).in("noxious_blade", 1).in("noxious_pommel", 1).out("noxious_halberd", 1).build());
    }

    /** Unsired offering table at the Font of Consumption (weights out of 128). */
    public static final List<Map.Entry<String, Integer>> UNSIRED_TABLE = List.of(
            Map.entry("bludgeon_claw", 22),
            Map.entry("bludgeon_spine", 20),
            Map.entry("bludgeon_axon", 20),
            Map.entry("abyssal_dagger", 26),
            Map.entry("abyssal_whip", 12),
            Map.entry("jar_of_miasma", 13),
            Map.entry("abyssal_head", 10),
            Map.entry("pet_abyssal_orphan", 5));
}
