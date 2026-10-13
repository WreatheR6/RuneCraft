package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentDef.Slot;
import com.runecraft.osrs.EquipmentDef.WeaponClass;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ItemSink;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every item the minigames module adds: reward shop gear, outfits, pets, currencies and the
 * resources used inside the minigames. Stats follow the OSRS wiki where known (approximations are
 * marked). RuneCraft has no hands/cape/ring slots, so capes sit in the shield slot (like the
 * built-in fire cape) and gloves are carried items whose effects apply while they're in the
 * inventory.
 */
public final class MinigameItems {
    private MinigameItems() {
    }

    /** Pets awarded by minigames. */
    public static final List<String> PETS = List.of("jal_nib_rek", "phoenix", "tiny_tempor", "abyssal_protector",
            "pet_penance_queen", "tzrek_zuk");

    static EquipmentStats s(int sa, int sl, int cr, int ma, int ra, int sd, int sld, int cd, int md, int rd,
                            int str, int rstr, int mdmg, int prayer) {
        return new EquipmentStats(sa, sl, cr, ma, ra, sd, sld, cd, md, rd, str, rstr, mdmg, prayer);
    }

    private static Map<Skill, Integer> reqs(Object... pairs) {
        Map<Skill, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put((Skill) pairs[i], (Integer) pairs[i + 1]);
        }
        return map;
    }

    private static ItemDef wear(ItemSink items, String id, String name, String shape, int color, Slot slot, int value,
                                EquipmentStats stats, Object... req) {
        Kind kind = slot == Slot.SHIELD ? Kind.SHIELD : Kind.ARMOUR;
        return items.add(id, name, kind, shape, color).value(value).stack(1)
                .equipment(new EquipmentDef(slot, stats, reqs(req), 0, AttackType.CRUSH, WeaponClass.NONE));
    }

    private static ItemDef weapon(ItemSink items, String id, String name, Kind kind, String shape, int color, int value,
                                  EquipmentStats stats, int speed, AttackType type, WeaponClass cls, Object... req) {
        return items.add(id, name, kind, shape, color).value(value).stack(1)
                .equipment(new EquipmentDef(Slot.WEAPON, stats, reqs(req), speed, type, cls));
    }

    private static void simple(ItemSink items, String id, String name, String shape, int color, int value, String examine) {
        items.add(id, name, Kind.SIMPLE, shape, color).value(value).examine(examine);
    }

    private static void single(ItemSink items, String id, String name, String shape, int color, int value, String examine) {
        items.add(id, name, Kind.SIMPLE, shape, color).value(value).stack(1).examine(examine);
    }

    private static void pet(ItemSink items, String id, String name, int color, String examine) {
        items.add(id, name, Kind.SIMPLE, "pet", color).value(1).stack(1).examine(examine);
    }

    public static void add(ItemSink items) {
        tzhaar(items);
        pestControl(items);
        wintertodt(items);
        tempoross(items);
        guardians(items);
        barbarianAssault(items);
        castleWars(items);
        warriorsGuild(items);
        mageArena(items);
        pyramidPlunder(items);
        sepulchre(items);
        brimhaven(items);
        gnomeRestaurant(items);
        troubleBrewing(items);
        shades(items);
        titheFarm(items);
        giantsFoundry(items);
        mahoganyHomes(items);
        trawler(items);
        roguesDen(items);
        imbues(items);
    }

    // ------------------------------------------------------------------ Nightmare Zone / Soul Wars
    /**
     * Imbued items made at the Nightmare Zone or Soul Wars (black mask (i) and slayer helmet (i)
     * belong to the slayer module). They're worn in the ring/neck slots through the systems module.
     */
    private static void imbues(ItemSink items) {
        String[][] rings = {{"berserker_ring_i", "Berserker ring (i)", "8A2A2A"}, {"archers_ring_i", "Archers ring (i)", "3A8A3A"},
                {"seers_ring_i", "Seers ring (i)", "3A3A9A"}, {"warrior_ring_i", "Warrior ring (i)", "A0A0A0"},
                {"tyrannical_ring_i", "Tyrannical ring (i)", "B04A20"}, {"treasonous_ring_i", "Treasonous ring (i)", "3A8A3A"},
                {"ring_of_the_gods_i", "Ring of the gods (i)", "E0C040"}, {"granite_ring_i", "Granite ring (i)", "7A7A7A"},
                {"ring_of_suffering_i", "Ring of suffering (i)", "F0A020"}};
        for (String[] r : rings) {
            items.add(r[0], r[1], Kind.SIMPLE, "ring", Integer.parseInt(r[2], 16)).value(1).stack(1)
                    .examine("An imbued ring: its bonuses are doubled.");
        }
        items.add("salve_amulet_i", "Salve amulet(i)", Kind.SIMPLE, "amulet", 0x7AE0B0).value(1).stack(1)
                .examine("An imbued salve amulet: also boosts Ranged and Magic against the undead.");
        items.add("salve_amulet_ei", "Salve amulet(ei)", Kind.SIMPLE, "amulet", 0x5AF0A0).value(1).stack(1)
                .examine("An imbued, enchanted salve amulet: +20% in every style against the undead.");
        items.add("soul_cape", "Soul cape", Kind.SIMPLE, "cape", 0x8A3AC0).value(1).stack(1)
                .examine("A cape from the Isle of Souls. (2,500 zeal)");
        simple(items, "soul_fragment", "Soul fragment", "shard", 0xB080FF, 0, "Lightly pulsing with energy. Offer it at the Soul Obelisk.");
        simple(items, "soul_wars_bones", "Bones", "bones", 0xE8E0D0, 0, "Bury these in your graveyard to strengthen your Avatar.");
    }

    // ------------------------------------------------------------------ TzHaar
    private static void tzhaar(ItemSink items) {
        int obsidian = 0x2A2A30;
        // Worn in the cape slot (systems module); SIMPLE so it can't be held as an off-hand shield.
        items.add("infernal_cape", "Infernal cape", Kind.SIMPLE, "cape", 0xFF6A10).value(1_000_000).stack(1)
                .equipment(new EquipmentDef(Slot.HEAD, s(4, 4, 4, 1, 1, 12, 12, 12, 12, 12, 8, 0, 0, 2), Map.of(), 0,
                        AttackType.CRUSH, WeaponClass.NONE))
                .tier("dragon").examine("A cape of solidified lava, earned in the Inferno.");
        pet(items, "jal_nib_rek", "Jal-nib-rek", 0xD05010, "A tiny nibbler from the Inferno.");
        pet(items, "tzrek_zuk", "TzRek-Zuk", 0xE06010, "A miniature TzKal-Zuk.");
        // Obsidian equipment sold for tokkul in Mor Ul Rek (TzHaar-Hur-Tel's and TzHaar-Mej-Roh's shops).
        wear(items, "obsidian_helmet", "Obsidian helmet", "full_helm", obsidian, Slot.HEAD, 84_480,
                s(0, 0, 0, -5, -1, 30, 32, 27, -1, 30, 3, 0, 0, 1), Skill.DEFENCE, 60).tier("obsidian");
        wear(items, "obsidian_platebody", "Obsidian platebody", "platebody", obsidian, Slot.CHEST, 1_331_200,
                s(0, 0, 0, -10, -10, 82, 80, 72, -6, 80, 3, 0, 0, 0), Skill.DEFENCE, 60).tier("obsidian");
        wear(items, "obsidian_platelegs", "Obsidian platelegs", "platelegs", obsidian, Slot.LEGS, 768_000,
                s(0, 0, 0, -21, -7, 51, 49, 47, -4, 49, 1, 0, 0, 0), Skill.DEFENCE, 60).tier("obsidian");
        wear(items, "toktz_ket_xil", "Toktz-ket-xil", "kiteshield", obsidian, Slot.SHIELD, 67_500,
                s(0, 0, 0, -12, -8, 40, 42, 38, 0, 65, 5, 0, 0, 0), Skill.DEFENCE, 60).tier("obsidian");
        wear(items, "obsidian_cape", "Obsidian cape", "cape", 0x3A2A30, Slot.SHIELD, 90_000,
                s(0, 0, 0, 0, 0, 9, 9, 9, 9, 9, 0, 0, 0, 0)).examine("A cape woven of obsidian plates.");
        weapon(items, "tzhaar_ket_om", "Tzhaar-ket-om", Kind.MELEE_WEAPON, "warhammer", obsidian, 75_000,
                s(0, 0, 80, -4, 0, 0, 0, 0, 0, 0, 85, 0, 0, 0), 7, AttackType.CRUSH, WeaponClass.MELEE, Skill.STRENGTH, 60)
                .tier("obsidian").examine("A large obsidian maul.");
        weapon(items, "tzhaar_ket_em", "Tzhaar-ket-em", Kind.MELEE_WEAPON, "warhammer", 0x3A3A40, 45_000,
                s(0, 0, 60, 0, 0, 0, 0, 0, 0, 0, 62, 0, 0, 0), 5, AttackType.CRUSH, WeaponClass.MELEE, Skill.ATTACK, 60)
                .tier("obsidian");
        weapon(items, "toktz_xil_ak", "Toktz-xil-ak", Kind.MELEE_WEAPON, "longsword", obsidian, 60_000,
                s(47, 38, -2, 0, 0, 0, 0, 0, 0, 0, 49, 0, 0, 0), 4, AttackType.STAB, WeaponClass.MELEE, Skill.ATTACK, 60)
                .tier("obsidian");
        weapon(items, "toktz_xil_ek", "Toktz-xil-ek", Kind.MELEE_WEAPON, "dagger", obsidian, 37_500,
                s(37, 26, -4, 0, 0, 0, 0, 0, 0, 0, 39, 0, 0, 0), 4, AttackType.STAB, WeaponClass.MELEE, Skill.ATTACK, 60)
                .tier("obsidian");
        weapon(items, "toktz_mej_tal", "Toktz-mej-tal", Kind.STAFF, "staff_orb", obsidian, 52_500,
                s(15, -1, 55, 15, 0, 10, 15, 5, 15, 0, 55, 0, 0, 0), 5, AttackType.CRUSH, WeaponClass.STAFF,
                Skill.ATTACK, 60, Skill.MAGIC, 60).tier("obsidian");
    }

    // ------------------------------------------------------------------ Pest Control
    private static void pestControl(ItemSink items) {
        int voidCol = 0x3A3A3A;
        Object[] req = {Skill.ATTACK, 42, Skill.STRENGTH, 42, Skill.DEFENCE, 42, Skill.HITPOINTS, 42, Skill.RANGED, 42,
                Skill.MAGIC, 42, Skill.PRAYER, 22};
        wear(items, "void_knight_top", "Void knight top", "platebody", voidCol, Slot.CHEST, 1,
                s(0, 0, 0, 0, 0, 45, 45, 45, 45, 45, 0, 0, 0, 0), req).tier("void")
                .examine("Torso armour from the order of the Void Knights.");
        wear(items, "void_knight_robe", "Void knight robe", "platelegs", voidCol, Slot.LEGS, 1,
                s(0, 0, 0, 0, 0, 30, 30, 30, 30, 30, 0, 0, 0, 0), req).tier("void")
                .examine("Leg armour from the order of the Void Knights.");
        single(items, "void_knight_gloves", "Void knight gloves", "boots", 0x4A4A4A, 1,
                "Gloves from the order of the Void Knights. Worn in the hands slot; part of the Void set.");
        wear(items, "void_melee_helm", "Void melee helm", "full_helm", 0x6A3A2A, Slot.HEAD, 1,
                s(0, 0, 0, 0, 0, 6, 6, 6, 6, 6, 0, 0, 0, 0), req).tier("void");
        wear(items, "void_ranger_helm", "Void ranger helm", "full_helm", 0x3A6A2A, Slot.HEAD, 1,
                s(0, 0, 0, 0, 0, 6, 6, 6, 6, 6, 0, 0, 0, 0), req).tier("void");
        wear(items, "void_mage_helm", "Void mage helm", "full_helm", 0x2A3A8A, Slot.HEAD, 1,
                s(0, 0, 0, 0, 0, 6, 6, 6, 6, 6, 0, 0, 0, 0), req).tier("void");
        wear(items, "elite_void_top", "Elite void top", "platebody", 0x4A4A5A, Slot.CHEST, 1,
                s(0, 0, 0, 0, 0, 45, 45, 45, 45, 45, 0, 0, 0, 3), req).tier("void")
                .examine("Elite Void knight torso armour.");
        wear(items, "elite_void_robe", "Elite void robe", "platelegs", 0x4A4A5A, Slot.LEGS, 1,
                s(0, 0, 0, 0, 0, 30, 30, 30, 30, 30, 0, 0, 0, 3), req).tier("void")
                .examine("Elite Void knight leg armour.");
        weapon(items, "void_knight_mace", "Void knight mace", Kind.MELEE_WEAPON, "warhammer", 0x6A6A70, 1,
                s(22, 0, 41, 8, 0, 0, 0, 0, 2, 0, 38, 0, 0, 0), 5, AttackType.CRUSH, WeaponClass.MELEE, req)
                .tier("void");
        single(items, "void_seal", "Void seal", "amulet", 0x8A6AC0, 1, "A seal of the Void Knights. Pests hate it.");
    }

    // ------------------------------------------------------------------ Wintertodt
    private static void wintertodt(ItemSink items) {
        simple(items, "bruma_root", "Bruma root", "log", 0x6A5A8A, 0, "A root that burns with the heat of the sun.");
        simple(items, "bruma_kindling", "Bruma kindling", "plank", 0x8A7AB0, 0, "Fletched bruma root; burns hotter.");
        simple(items, "bruma_herb", "Bruma herb", "herb", 0x9AC0E0, 0, "A herb that grows in the cold.");
        items.add("rejuvenation_potion", "Rejuvenation potion", Kind.FOOD, "potion", 0x60C0E0).value(0).heal(2).stack(16)
                .examine("Restores a little warmth and hitpoints.");
        single(items, "supply_crate", "Supply crate", "package", 0x8A6A3A, 1, "Wintertodt rewards. Right-click to open.");
        wear(items, "pyromancer_hood", "Pyromancer hood", "cowl", 0xB04020, Slot.HEAD, 1,
                s(0, 0, 0, 2, 0, 2, 3, 3, 2, 0, 0, 0, 0, 0)).examine("+0.4% Firemaking XP (2.5% with the full outfit).");
        wear(items, "pyromancer_garb", "Pyromancer garb", "leather_body", 0xB04020, Slot.CHEST, 1,
                s(0, 0, 0, 2, 0, 8, 9, 10, 4, 0, 0, 0, 0, 0)).examine("+0.8% Firemaking XP.");
        wear(items, "pyromancer_robe", "Pyromancer robe", "chaps", 0xB04020, Slot.LEGS, 1,
                s(0, 0, 0, 2, 0, 3, 3, 3, 2, 0, 0, 0, 0, 0)).examine("+0.6% Firemaking XP.");
        wear(items, "pyromancer_boots", "Pyromancer boots", "boots", 0xB04020, Slot.FEET, 1,
                s(0, 0, 0, 1, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0)).examine("+0.2% Firemaking XP.");
        single(items, "warm_gloves", "Warm gloves", "boots", 0xC06040, 1, "Keeps the Wintertodt's cold at bay (while carried).");
        weapon(items, "bruma_torch", "Bruma torch", Kind.MELEE_WEAPON, "staff_orb", 0xFF8030, 1,
                s(0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0), 4, AttackType.CRUSH, WeaponClass.MELEE)
                .examine("A warm torch that counts as warm clothing and a tinderbox.");
        wear(items, "tome_of_fire", "Tome of fire", "book", 0xD03010, Slot.SHIELD, 1,
                s(0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), Skill.MAGIC, 50)
                .examine("+10% magic damage (Fire spells in OSRS; all spells here).");
        simple(items, "burnt_page", "Burnt page", "note", 0x6A4A2A, 1, "Charges a tome of fire.");
        pet(items, "phoenix", "Phoenix", 0xFF7020, "A fiery pet from the Wintertodt.");
    }

    // ------------------------------------------------------------------ Tempoross
    private static void tempoross(ItemSink items) {
        items.add("raw_harpoonfish", "Raw harpoonfish", Kind.SIMPLE, "fish_raw", 0x7AA0C0).value(0);
        items.add("harpoonfish", "Harpoonfish", Kind.FOOD, "fish_cooked", 0x8AB0D0).value(0).heal(12);
        simple(items, "reward_permit", "Reward permit", "note", 0xE0D0A0, 1, "Lets you search the Tempoross reward pool.");
        single(items, "fish_barrel", "Fish barrel", "pot_item", 0x8A6A3A, 1, "Stores caught fish (holds 28).");
        single(items, "tackle_box", "Tackle box", "package", 0x6A8A6A, 1, "Holds fishing tackle.");
        single(items, "big_harpoonfish", "Big harpoonfish", "fish_raw", 0x6A90B0, 1, "A huge harpoonfish trophy.");
        simple(items, "spirit_flakes", "Spirit flakes", "secondary", 0x80E0E0, 1, "Fishing with these in your inventory can double your catch.");
        simple(items, "soaked_page", "Soaked page", "note", 0x6080A0, 1, "Charges a tome of water.");
        wear(items, "tome_of_water", "Tome of water", "book", 0x2050C0, Slot.SHIELD, 1,
                s(0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), Skill.MAGIC, 50)
                .examine("+10% magic damage (Water spells in OSRS; all spells here).");
        // dragon_harpoon is defined by the slayer module.
        pet(items, "tiny_tempor", "Tiny tempor", 0x40A0D0, "A tiny Tempoross.");
    }

    // ------------------------------------------------------------------ Guardians of the Rift
    private static void guardians(ItemSink items) {
        simple(items, "guardian_fragments", "Guardian fragments", "shard", 0x6A5AC0, 0, "Fragments of a guardian's remains.");
        simple(items, "guardian_essence", "Guardian essence", "essence", 0x9A8AE0, 0, "Essence for crafting runes in the Temple of the Eye.");
        simple(items, "elemental_guardian_stone", "Elemental guardian stone", "ore", 0xE07040, 0, "Powers the Great Guardian.");
        simple(items, "catalytic_guardian_stone", "Catalytic guardian stone", "ore", 0x8040E0, 0, "Powers the Great Guardian.");
        simple(items, "abyssal_pearls", "Abyssal pearls", "bead", 0xE0E0F8, 1, "Currency of the Temple of the Eye.");
        single(items, "intricate_pouch", "Intricate pouch", "package", 0x6A4AA0, 1, "Guardians of the Rift rewards. Right-click to open.");
        wear(items, "hat_of_the_eye", "Hat of the eye", "cowl", 0x8A2A8A, Slot.HEAD, 1,
                s(0, 0, 0, 3, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0)).examine("+10% runes crafted (+60% with the full raiments).");
        wear(items, "robe_top_of_the_eye", "Robe top of the eye", "leather_body", 0x8A2A8A, Slot.CHEST, 1,
                s(0, 0, 0, 8, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0)).examine("+10% runes crafted.");
        wear(items, "robe_bottoms_of_the_eye", "Robe bottoms of the eye", "chaps", 0x8A2A8A, Slot.LEGS, 1,
                s(0, 0, 0, 6, 0, 0, 0, 0, 6, 0, 0, 0, 0, 0)).examine("+10% runes crafted.");
        wear(items, "boots_of_the_eye", "Boots of the eye", "boots", 0x8A2A8A, Slot.FEET, 1,
                s(0, 0, 0, 2, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0)).examine("+10% runes crafted.");
        single(items, "abyssal_needle", "Abyssal needle", "needle", 0x6A2A8A, 1, "Merges essence pouches into a colossal pouch.");
        // colossal_pouch is defined by the skilling module.
        wear(items, "abyssal_lantern", "Abyssal lantern", "orb", 0x80C080, Slot.SHIELD, 1,
                s(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)).examine("Burns logs; lights the way in the Abyss.");
        single(items, "catalytic_talisman", "Catalytic talisman", "talisman", 0x8040E0, 1, "A talisman of every catalytic altar.");
        single(items, "elemental_talisman", "Elemental talisman", "talisman", 0xE07040, 1, "A talisman of every elemental altar.");
        pet(items, "abyssal_protector", "Abyssal protector", 0x6A4AE0, "A guardian of the rift, in miniature.");
        single(items, "abyssal_green_dye", "Abyssal green dye", "potion", 0x40A040, 1, "Dyes the raiments of the eye green.");
        single(items, "abyssal_blue_dye", "Abyssal blue dye", "potion", 0x4060C0, 1, "Dyes the raiments of the eye blue.");
        single(items, "abyssal_red_dye", "Abyssal red dye", "potion", 0xC03030, 1, "Dyes the raiments of the eye red.");
    }

    // ------------------------------------------------------------------ Barbarian Assault
    private static void barbarianAssault(ItemSink items) {
        wear(items, "fighter_torso", "Fighter torso", "platebody", 0x8A5A2A, Slot.CHEST, 50_000,
                s(0, 0, 0, -40, -40, 15, 15, 15, -6, 15, 4, 0, 0, 1), Skill.DEFENCE, 40).tier("barbarian")
                .examine("Penance-proof armour: +4 Strength.");
        single(items, "penance_gloves", "Penance gloves", "boots", 0x6A4A2A, 1, "Gloves awarded at Barbarian Assault.");
        wear(items, "penance_skirt", "Penance skirt", "chaps", 0x6A4A2A, Slot.LEGS, 1,
                s(0, 0, 0, 0, 0, 10, 10, 10, 10, 10, 0, 0, 0, 0), Skill.DEFENCE, 40);
        wear(items, "granite_body", "Granite body", "platebody", 0x6A6A70, Slot.CHEST, 95_000,
                s(0, 0, 0, -22, -5, 87, 84, 79, -6, 97, 0, 0, 0, 0), Skill.DEFENCE, 50, Skill.STRENGTH, 50).tier("granite");
        wear(items, "fighter_hat", "Fighter hat", "full_helm", 0xA06030, Slot.HEAD, 1,
                s(0, 0, 0, -3, 0, 15, 15, 15, 0, 15, 0, 0, 0, 0), Skill.DEFENCE, 45);
        wear(items, "ranger_hat", "Ranger hat", "cowl", 0x3A7A3A, Slot.HEAD, 1,
                s(0, 0, 0, -3, 5, 15, 15, 15, 0, 15, 0, 0, 0, 0), Skill.DEFENCE, 45);
        wear(items, "runner_hat", "Runner hat", "cowl", 0x3A3A8A, Slot.HEAD, 1,
                s(0, 0, 0, -3, 0, 15, 15, 15, 0, 15, 0, 0, 0, 0), Skill.DEFENCE, 45);
        wear(items, "healer_hat", "Healer hat", "cowl", 0xC0C0C0, Slot.HEAD, 1,
                s(0, 0, 0, 6, 0, 15, 15, 15, 6, 15, 0, 0, 0, 0), Skill.DEFENCE, 45);
        wear(items, "runner_boots", "Runner boots", "boots", 0x3A3A8A, Slot.FEET, 1,
                s(0, 0, 0, 0, 0, 1, 2, 3, 0, 0, 0, 0, 0, 0), Skill.DEFENCE, 40);
        simple(items, "penance_egg", "Penance egg", "bead", 0xD0C080, 0, "Collected from the Penance. Load it into the egg launcher.");
        pet(items, "pet_penance_queen", "Pet penance queen", 0x8A6A2A, "A tiny penance queen.");
    }

    // ------------------------------------------------------------------ Castle Wars
    private static void castleWars(ItemSink items) {
        simple(items, "castle_wars_ticket", "Castle wars ticket", "note", 0xE0C060, 1, "Exchange these with Lanthus.");
        single(items, "saradomin_standard", "Saradomin standard", "cape", 0x3060E0, 1, "Saradomin's flag. Return it to the Zamorak flag stand!");
        single(items, "zamorak_standard", "Zamorak standard", "cape", 0xC02020, 1, "Zamorak's flag. Return it to the Saradomin flag stand!");
        items.add("castle_wars_bandage", "Bandages", Kind.FOOD, "cloth", 0xF0F0F0).value(0).heal(9).stack(16)
                .examine("Heals 10% of your hitpoints.");
        String[][] deco = {{"gold", "E0B030"}, {"red", "B02020"}, {"white", "E8E8E8"}};
        for (String[] d : deco) {
            int c = Integer.parseInt(d[1], 16);
            String cap = Character.toUpperCase(d[0].charAt(0)) + d[0].substring(1);
            wear(items, d[0] + "_decorative_helm", cap + " decorative helm", "full_helm", c, Slot.HEAD, 1,
                    s(0, 0, 0, -6, -2, 30, 32, 27, -1, 30, 0, 0, 0, 0), Skill.DEFENCE, 40).tier("decorative");
            wear(items, d[0] + "_decorative_body", cap + " decorative platebody", "platebody", c, Slot.CHEST, 1,
                    s(0, 0, 0, -30, -10, 82, 80, 72, -6, 80, 0, 0, 0, 0), Skill.DEFENCE, 40).tier("decorative");
            wear(items, d[0] + "_decorative_legs", cap + " decorative platelegs", "platelegs", c, Slot.LEGS, 1,
                    s(0, 0, 0, -21, -11, 51, 49, 47, -4, 49, 0, 0, 0, 0), Skill.DEFENCE, 40).tier("decorative");
            wear(items, d[0] + "_decorative_shield", cap + " decorative shield", "kiteshield", c, Slot.SHIELD, 1,
                    s(0, 0, 0, -8, -2, 44, 48, 46, -1, 46, 0, 0, 0, 0), Skill.DEFENCE, 40).tier("decorative");
            weapon(items, d[0] + "_decorative_sword", cap + " decorative sword", Kind.MELEE_WEAPON, "longsword", c, 1,
                    s(38, 47, -2, 0, 0, 0, 3, 2, 0, 0, 49, 0, 0, 0), 5, AttackType.SLASH, WeaponClass.MELEE, Skill.ATTACK, 40)
                    .tier("decorative");
        }
        EquipmentStats halo = s(0, 0, 0, 0, 0, 11, 12, 10, 11, -1, 0, 0, 0, 3);
        wear(items, "saradomin_halo", "Saradomin halo", "full_helm", 0xE0E0FF, Slot.HEAD, 1, halo)
                .examine("A halo of Saradomin. (75 Castle Wars tickets)");
        wear(items, "zamorak_halo", "Zamorak halo", "full_helm", 0xE04040, Slot.HEAD, 1, halo)
                .examine("A halo of Zamorak. (75 Castle Wars tickets)");
        wear(items, "guthix_halo", "Guthix halo", "full_helm", 0x40C040, Slot.HEAD, 1, halo)
                .examine("A halo of Guthix. (75 Castle Wars tickets)");
        // Legacy carried copy; the real (wearable, enchanted emerald) bracelet is castle_wars_bracelet.
        single(items, "castlewars_brace", "Castle wars bracelet", "ring", 0xE0C060, 1,
                "+20% damage against flag carriers.");
        wear(items, "saradomin_hood", "Saradomin hood", "cowl", 0x3060E0, Slot.HEAD, 1, EquipmentStats.NONE);
        wear(items, "zamorak_hood", "Zamorak hood", "cowl", 0xC02020, Slot.HEAD, 1, EquipmentStats.NONE);
    }

    // ------------------------------------------------------------------ Warriors' Guild
    /** Defender tiers in order, with their defence requirement. */
    public static final String[] DEFENDER_TIERS = {"bronze", "iron", "steel", "black", "mithril", "adamant", "rune", "dragon"};
    public static final int[] DEFENDER_LEVELS = {1, 1, 5, 10, 20, 30, 40, 60};

    private static void warriorsGuild(ItemSink items) {
        simple(items, "warrior_guild_token", "Warrior guild token", "coins", 0xB08030, 1, "Lets you into the cyclops room.");
        // Wield requirement is the same Attack and Defence level. Bonuses come from EquipmentBonuses.
        int[][] stats = {
                {3, 2, 1, 1}, {5, 4, 3, 2}, {6, 5, 4, 3}, {8, 7, 6, 4},
                {10, 9, 8, 4}, {13, 12, 11, 5}, {20, 19, 18, 5}, {25, 24, 23, 6}};
        int[] colors = {0x8A5A2A, 0x6A6A6A, 0x9A9AA0, 0x2A2A2A, 0x4A5A9C, 0x4C7A4C, 0x4FB8C8, 0xC03020};
        int[] values = {200, 400, 1000, 1500, 2000, 6000, 30000, 1_000_000};
        for (int i = 0; i < DEFENDER_TIERS.length; i++) {
            String tier = DEFENDER_TIERS[i];
            int[] st = stats[i];
            wear(items, tier + "_defender", Character.toUpperCase(tier.charAt(0)) + tier.substring(1) + " defender",
                    "kiteshield", colors[i], Slot.SHIELD, values[i],
                    s(st[0], st[1], st[2], -3, -2, st[0], st[1], st[2], -3, -2, st[3], 0, 0, 0),
                    Skill.ATTACK, DEFENDER_LEVELS[i], Skill.DEFENCE, DEFENDER_LEVELS[i]).tier(tier.equals("adamant") ? "adamant" : tier)
                    .examine("A defensive weapon from the Warriors' Guild cyclopes.");
        }
    }

    // ------------------------------------------------------------------ Mage Training Arena
    private static void mageArena(ItemSink items) {
        int inf = 0x4A3A8A;
        Object[] req = {Skill.MAGIC, 50, Skill.DEFENCE, 25};
        wear(items, "infinity_hat", "Infinity hat", "cowl", inf, Slot.HEAD, 1, s(0, 0, 0, 6, 0, 0, 0, 0, 6, 0, 0, 0, 0, 0), req);
        wear(items, "infinity_top", "Infinity top", "leather_body", inf, Slot.CHEST, 1, s(0, 0, 0, 22, 0, 0, 0, 0, 22, 0, 0, 0, 0, 0), req);
        wear(items, "infinity_bottoms", "Infinity bottoms", "chaps", inf, Slot.LEGS, 1, s(0, 0, 0, 17, 0, 0, 0, 0, 17, 0, 0, 0, 0, 0), req);
        wear(items, "infinity_boots", "Infinity boots", "boots", inf, Slot.FEET, 1, s(0, 0, 0, 5, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0), req);
        single(items, "infinity_gloves", "Infinity gloves", "boots", inf, 1, "Magical gloves. Worn in the hands slot.");
        String[][] wands = {{"beginner_wand", "Beginner wand", "5", "1"}, {"apprentice_wand", "Apprentice wand", "10", "1"},
                {"teacher_wand", "Teacher wand", "15", "1"}, {"master_wand", "Master wand", "20", "50"}};
        for (String[] w : wands) {
            int bonus = Integer.parseInt(w[2]);
            weapon(items, w[0], w[1], Kind.STAFF, "staff", 0x8A6AC0, 1, s(0, 0, 0, bonus, 0, 0, 0, 0, bonus, 0, 0, 0, 0, 0),
                    4, AttackType.MAGIC, WeaponClass.STAFF, Skill.MAGIC, Integer.parseInt(w[3]) == 1 ? 1 : 60);
        }
        wear(items, "mages_book", "Mage's book", "book", 0x6A4AA0, Slot.SHIELD, 1, s(0, 0, 0, 15, 0, 0, 0, 0, 15, 0, 0, 0, 0, 0),
                Skill.MAGIC, 60);
        single(items, "bones_to_peaches_tablet", "Bones to peaches", "note", 0xF0A060, 1,
                "Right-click: turns the bones in your inventory into peaches.");
        items.add("peach", "Peach", Kind.FOOD, "produce", 0xF0A060).value(1).heal(8);
        simple(items, "enchantment_shape", "Enchantment shape", "gem", 0x40C0C0, 0, "A shape from the Enchanting Chamber.");
        simple(items, "alchemy_item", "Cupboard item", "package", 0xC0A060, 0, "Something from the Alchemists' Playground.");
        simple(items, "graveyard_bones", "Animals' bones", "bones", 0xE8E0D0, 0, "Bones from the Creature Graveyard.");
    }

    // ------------------------------------------------------------------ Pyramid Plunder
    /** Artefacts in increasing value order: id, name, value (sold to Simon Templeton). */
    public static final Object[][] ARTEFACTS = {
            {"ivory_comb", "Ivory comb", 50}, {"pottery_scarab", "Pottery scarab", 75},
            {"pottery_statuette", "Pottery statuette", 100}, {"stone_seal", "Stone seal", 150},
            {"stone_scarab", "Stone scarab", 175}, {"stone_statuette", "Stone statuette", 200},
            {"gold_seal", "Gold seal", 750}, {"golden_scarab", "Golden scarab", 1000},
            {"golden_statuette", "Golden statuette", 1250}, {"jewelled_golden_statuette", "Jewelled golden statuette", 5000}};

    private static void pyramidPlunder(ItemSink items) {
        for (Object[] a : ARTEFACTS) {
            int v = (Integer) a[2];
            int color = v >= 750 ? 0xE0B030 : v >= 150 ? 0x8A8A80 : 0xC0A080;
            items.add((String) a[0], (String) a[1], Kind.SIMPLE, "gem", color).value(v).examine("An artefact from Jalsavrah.");
        }
        weapon(items, "pharaohs_sceptre", "Pharaoh's sceptre", Kind.MELEE_WEAPON, "staff_orb", 0xE0B030, 1,
                s(-2, -2, 16, 0, 0, 0, 0, 0, 0, 0, 12, 0, 0, 0), 5, AttackType.CRUSH, WeaponClass.MELEE)
                .examine("Teleports to the pyramids. Fabulously rare.");
    }

    // ------------------------------------------------------------------ Hallowed Sepulchre
    private static void sepulchre(ItemSink items) {
        simple(items, "hallowed_mark", "Hallowed mark", "mark", 0xC0D0E0, 1, "Trade these with Valindor Lisetta in Darkmeyer.");
        single(items, "hallowed_crystal_shard", "Hallowed crystal shard", "shard", 0xC0E0F0, 1, "Teleports to the Sepulchre.");
        single(items, "hallowed_focus", "Hallowed focus", "orb", 0xC0D0E0, 1, "A cosmetic offhand from the Sepulchre.");
        single(items, "hallowed_symbol", "Hallowed symbol", "amulet", 0xC0D0E0, 1, "A cosmetic amulet from the Sepulchre.");
        single(items, "hallowed_hammer", "Hallowed hammer", "hammer", 0xC0D0E0, 1, "Works as a hammer.");
        single(items, "hallowed_grapple", "Hallowed grapple", "crossbow", 0xC0D0E0, 1, "A cosmetic grapple.");
        single(items, "hallowed_ring", "Hallowed ring", "ring", 0xC0D0E0, 1, "A cosmetic ring.");
        single(items, "dark_dye", "Dark dye", "potion", 0x202030, 1, "Dyes graceful clothing dark.");
        single(items, "dark_acorn", "Dark acorn", "seed", 0x3A3A2A, 1, "Turns a squirrel pet dark.");
        single(items, "strange_old_lockpick", "Strange old lockpick", "key", 0x8A8A70, 1, "Opens Grand Hallowed Coffins for a bonus.");
        single(items, "ring_of_endurance", "Ring of endurance", "ring", 0x60C060, 1, "Stamina potions last twice as long.");
        single(items, "mysterious_page", "Mysterious page", "note", 0xD0C0A0, 1, "Part of the Hallowed Sepulchre's history.");
    }

    // ------------------------------------------------------------------ Brimhaven Agility Arena
    private static void brimhaven(ItemSink items) {
        simple(items, "agility_arena_ticket", "Agility arena ticket", "note", 0xE0A040, 1,
                "Exchange with Pirate Jackie the Fruit for Agility experience.");
        simple(items, "brimhaven_voucher", "Brimhaven voucher", "note", 0x6AB04A, 1, "Exchange with Pirate Jackie the Fruit for rewards.");
        // Worn in the hands slot (systems module's Jewellery); SIMPLE so it can't be wielded, HEAD only feeds tooltips.
        items.add("pirates_hook", "Pirate's hook", Kind.SIMPLE, "hook", 0xA0A0A0).value(1).stack(1)
                .equipment(new EquipmentDef(Slot.HEAD, s(0, 0, 0, 0, 0, 1, 5, 3, 0, 0, 0, 0, 0, 0), reqs(), 0, AttackType.CRUSH,
                        WeaponClass.NONE))
                .examine("Yarr! 800 Brimhaven vouchers.");
    }

    // ------------------------------------------------------------------ Gnome Restaurant
    /** Gnome dishes: id, name, heal. */
    public static final Object[][] GNOME_DISHES = {
            {"fruit_batta", "Fruit batta", 11}, {"toad_batta", "Toad batta", 11}, {"worm_hole", "Worm hole", 12},
            {"chocolate_bomb", "Chocolate bomb", 15}, {"tangled_toads_legs", "Tangled toad's legs", 15},
            {"veg_ball", "Veg ball", 12}};

    private static void gnomeRestaurant(ItemSink items) {
        for (Object[] d : GNOME_DISHES) {
            items.add((String) d[0], (String) d[1], Kind.FOOD, "cake", 0xC09050).value(150).heal((Integer) d[2]).stack(1);
        }
        simple(items, "gnome_ingredients", "Gnome ingredients", "spice", 0x90B040, 0, "Gianne dough, toads, worms and equa leaves.");
        single(items, "aluft_aloft_box", "Aluft aloft box", "package", 0x6A9A4A, 1, "Gnome Restaurant rewards. Right-click to open.");
        single(items, "gnome_scarf", "Gnome scarf", "cloth", 0xE0E040, 1, "A gnomish scarf.");
        wear(items, "gnome_goggles", "Gnome goggles", "cowl", 0x8A8A50, Slot.HEAD, 1, EquipmentStats.NONE);
        items.add("mint_cake", "Mint cake", Kind.FOOD, "cake", 0xA0E0A0).value(5).heal(1).examine("Restores run energy.");
        simple(items, "gnome_restaurant_token", "Reward token", "coins", 0x6A9A4A, 1, "Gnome Restaurant delivery token.");
    }

    // ------------------------------------------------------------------ Trouble Brewing
    /** Honest Jimmy's seven naval clothing colours. */
    public static final List<String> NAVAL_COLOURS = List.of("blue", "green", "red", "brown", "black", "purple", "grey");
    private static final String[] NAVAL_RGB = {"3050C0", "30A040", "C03030", "6A4A2A", "2A2A2A", "8030A0", "8A8A8A"};

    private static void troubleBrewing(ItemSink items) {
        simple(items, "pieces_of_eight", "Pieces of eight", "coins", 0xE0C030, 1, "Pirate currency from Mos Le'Harmless.");
        simple(items, "bitternut", "Bitternut", "produce", 0x6A4A2A, 0, "A brewing ingredient.");
        simple(items, "scrapey_tree_bark", "Scrapey tree bark", "log", 0x8A6A4A, 0, "A brewing ingredient.");
        simple(items, "sweetgrubs", "Sweetgrubs", "bait", 0xE0D080, 0, "A brewing ingredient.");
        items.add("bottle_of_rum", "Karamjan rum", Kind.FOOD, "beer", 0xA04020).value(30).heal(5).stack(16);
        for (int i = 0; i < NAVAL_COLOURS.size(); i++) {
            String[] c = {NAVAL_COLOURS.get(i), NAVAL_RGB[i]};
            int col = Integer.parseInt(c[1], 16);
            String cap = Character.toUpperCase(c[0].charAt(0)) + c[0].substring(1);
            wear(items, c[0] + "_naval_shirt", cap + " naval shirt", "leather_body", col, Slot.CHEST, 1, EquipmentStats.NONE);
            wear(items, c[0] + "_tricorn_hat", cap + " tricorn hat", "cowl", col, Slot.HEAD, 1, EquipmentStats.NONE);
            wear(items, c[0] + "_navy_slacks", cap + " navy slacks", "chaps", col, Slot.LEGS, 1, EquipmentStats.NONE);
        }
        single(items, "the_stuff", "The stuff", "potion", 0x80E040, 1, "Makes brewing at Keldagrim much faster.");
    }

    // ------------------------------------------------------------------ Shades of Mort'ton
    /** Shade tiers: id prefix, name, level, prayer xp when the remains are burned. */
    public static final Object[][] SHADES = {
            {"loar", "Loar", 40, 25.0}, {"phrin", "Phrin", 60, 46.5}, {"riyl", "Riyl", 80, 59.5},
            {"asyn", "Asyn", 100, 82.5}, {"fiyr", "Fiyr", 120, 100.5}};

    private static void shades(ItemSink items) {
        int[] colors = {0x8A8A7A, 0x7A7A8A, 0x6A6A8A, 0x5A5A7A, 0x4A4A6A};
        for (int i = 0; i < SHADES.length; i++) {
            Object[] sd = SHADES[i];
            items.add(sd[0] + "_remains", sd[1] + " remains", Kind.SIMPLE, "bones", colors[i]).value(50 * (i + 1))
                    .examine("Burn these on a funeral pyre.");
        }
        for (String k : new String[]{"bronze", "steel", "black", "silver", "gold"}) {
            single(items, k + "_shade_key", Character.toUpperCase(k.charAt(0)) + k.substring(1) + " key", "key",
                    k.equals("gold") ? 0xE0B030 : k.equals("silver") ? 0xD0D0E0 : k.equals("black") ? 0x2A2A2A
                            : k.equals("steel") ? 0x9A9AA0 : 0x8A5A2A, 1, "Opens a chest in the Shade Catacombs.");
        }
        single(items, "flamtaer_hammer", "Flamtaer hammer", "hammer", 0x8A6A4A, 1, "Repairs the Mort'ton temple faster.");
    }

    // ------------------------------------------------------------------ Tithe Farm
    public static final String[] TITHE_PLANTS = {"golovanova", "bologano", "logavano"};
    public static final int[] TITHE_LEVELS = {34, 54, 74};

    private static void titheFarm(ItemSink items) {
        int[] colors = {0xE0A040, 0xC04040, 0x8040C0};
        for (int i = 0; i < 3; i++) {
            String p = TITHE_PLANTS[i];
            String cap = Character.toUpperCase(p.charAt(0)) + p.substring(1);
            simple(items, p + "_seed", cap + " seed", "seed", colors[i], 0, "A Tithe Farm seed (Farming " + TITHE_LEVELS[i] + ").");
            simple(items, p + "_fruit", cap + " fruit", "produce", colors[i], 0, "Deliver it to the sack for points.");
        }
        single(items, "gricollers_can", "Gricoller's can", "pot_item", 0x6A8AA0, 1, "A watering can that holds 1,000 uses.");
        wear(items, "farmers_strawhat", "Farmer's strawhat", "cowl", 0xE0C060, Slot.HEAD, 1, EquipmentStats.NONE)
                .examine("+0.4% Farming XP (2.5% with the full outfit).");
        wear(items, "farmers_jacket", "Farmer's jacket", "leather_body", 0x8A6A3A, Slot.CHEST, 1, EquipmentStats.NONE)
                .examine("+0.8% Farming XP.");
        wear(items, "farmers_boro_trousers", "Farmer's boro trousers", "chaps", 0x6A5A3A, Slot.LEGS, 1, EquipmentStats.NONE)
                .examine("+0.6% Farming XP.");
        wear(items, "farmers_boots", "Farmer's boots", "boots", 0x5A4A2A, Slot.FEET, 1, EquipmentStats.NONE)
                .examine("+0.2% Farming XP.");
        single(items, "seed_box", "Seed box", "package", 0x8A6A3A, 1, "Stores seeds.");
        // herb_sack is defined by the slayer module.
    }

    // ------------------------------------------------------------------ Giants' Foundry
    private static void giantsFoundry(ItemSink items) {
        wear(items, "smiths_tunic", "Smiths tunic", "leather_body", 0x8A5A3A, Slot.CHEST, 1, EquipmentStats.NONE)
                .examine("Smithing is faster with the full Smiths' uniform.");
        wear(items, "smiths_trousers", "Smiths trousers", "chaps", 0x6A4A2A, Slot.LEGS, 1, EquipmentStats.NONE);
        wear(items, "smiths_boots", "Smiths boots", "boots", 0x4A3A2A, Slot.FEET, 1, EquipmentStats.NONE);
        single(items, "smiths_gloves", "Smiths gloves", "boots", 0x6A5A4A, 1, "Part of the Smiths' uniform.");
        weapon(items, "colossal_blade", "Colossal blade", Kind.MELEE_WEAPON, "2h_sword", 0x9A9AA0, 1,
                s(0, 90, 0, 0, 0, 0, 0, 0, 0, 0, 94, 0, 0, 0), 6, AttackType.SLASH, WeaponClass.MELEE, Skill.ATTACK, 60)
                .examine("A giant's sword. Hits harder against large foes.");
        single(items, "double_ammo_mould", "Double ammo mould", "mould", 0x8A8A8A, 1, "Makes two cannonballs at once.");
        items.add("kovacs_grog", "Kovac's grog", Kind.FOOD, "beer", 0xA06030).value(1).heal(3).stack(16)
                .examine("Boosts Smithing a little.");
    }

    // ------------------------------------------------------------------ Mahogany Homes
    private static void mahoganyHomes(ItemSink items) {
        wear(items, "carpenters_helmet", "Carpenter's helmet", "cowl", 0xC0A040, Slot.HEAD, 1, EquipmentStats.NONE)
                .examine("+0.4% Construction XP (2.5% with the full outfit).");
        wear(items, "carpenters_shirt", "Carpenter's shirt", "leather_body", 0x6A8AA0, Slot.CHEST, 1, EquipmentStats.NONE)
                .examine("+0.8% Construction XP.");
        wear(items, "carpenters_trousers", "Carpenter's trousers", "chaps", 0x4A5A7A, Slot.LEGS, 1, EquipmentStats.NONE)
                .examine("+0.6% Construction XP.");
        wear(items, "carpenters_boots", "Carpenter's boots", "boots", 0x5A4A2A, Slot.FEET, 1, EquipmentStats.NONE)
                .examine("+0.2% Construction XP.");
        single(items, "plank_sack", "Plank sack", "package", 0x9A7A4A, 1, "Holds 28 planks.");
        single(items, "amys_saw", "Amy's saw", "knife", 0xB0B0B8, 1, "Works as a saw and boosts Construction by 3.");
        single(items, "hosidius_blueprints", "Hosidius blueprints", "note", 0x6080C0, 1, "Plans for Hosidius house hotspots.");
        // steel_nails is defined by the skilling module.
    }

    // ------------------------------------------------------------------ Fishing Trawler
    private static void trawler(ItemSink items) {
        wear(items, "angler_hat", "Angler hat", "cowl", 0x3A6A8A, Slot.HEAD, 1, EquipmentStats.NONE)
                .examine("+0.4% Fishing XP (2.5% with the full outfit).");
        wear(items, "angler_top", "Angler top", "leather_body", 0x3A6A8A, Slot.CHEST, 1, EquipmentStats.NONE)
                .examine("+0.8% Fishing XP.");
        wear(items, "angler_waders", "Angler waders", "chaps", 0x4A5A3A, Slot.LEGS, 1, EquipmentStats.NONE)
                .examine("+0.6% Fishing XP.");
        wear(items, "angler_boots", "Angler boots", "boots", 0x3A3A2A, Slot.FEET, 1, EquipmentStats.NONE)
                .examine("+0.2% Fishing XP.");
        simple(items, "swamp_paste", "Swamp paste", "pot_item", 0x4A5A3A, 1, "Patches leaks in the trawler.");
    }

    // ------------------------------------------------------------------ Rogues' Den
    private static void roguesDen(ItemSink items) {
        int rogue = 0x3A3A40;
        wear(items, "rogue_mask", "Rogue mask", "cowl", rogue, Slot.HEAD, 1, EquipmentStats.NONE)
                .examine("Double loot chance when pickpocketing (full set).");
        wear(items, "rogue_top", "Rogue top", "leather_body", rogue, Slot.CHEST, 1, EquipmentStats.NONE);
        wear(items, "rogue_trousers", "Rogue trousers", "chaps", rogue, Slot.LEGS, 1, EquipmentStats.NONE);
        wear(items, "rogue_boots", "Rogue boots", "boots", rogue, Slot.FEET, 1, EquipmentStats.NONE);
        single(items, "rogue_gloves", "Rogue gloves", "boots", rogue, 1, "Part of the rogue equipment.");
        single(items, "mystic_jewel", "Mystic jewel", "gem", 0xA040E0, 1, "Hand this to Brian O'Richard for a rogue reward.");
    }
}
