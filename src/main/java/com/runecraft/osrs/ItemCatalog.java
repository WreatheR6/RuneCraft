package com.runecraft.osrs;

import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.data.Allotment;
import com.runecraft.osrs.data.ArmourType;
import com.runecraft.osrs.data.ArrowTier;
import com.runecraft.osrs.data.Bar;
import com.runecraft.osrs.data.Bone;
import com.runecraft.osrs.data.BowWood;
import com.runecraft.osrs.data.Fish;
import com.runecraft.osrs.data.Gem;
import com.runecraft.osrs.data.Herb;
import com.runecraft.osrs.data.MetalTier;
import com.runecraft.osrs.data.Potion;
import com.runecraft.osrs.data.Rock;
import com.runecraft.osrs.data.ToolTier;
import com.runecraft.osrs.data.Tree;
import com.runecraft.osrs.data.WeaponType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Every item RuneCraft adds, built from the OSRS data tables. */
public final class ItemCatalog {
    private static final Map<String, ItemDef> ITEMS = new LinkedHashMap<>();

    static {
        build();
    }

    private ItemCatalog() {
    }

    public static List<ItemDef> all() {
        return Collections.unmodifiableList(new ArrayList<>(ITEMS.values()));
    }

    public static ItemDef get(String id) {
        if (id.startsWith("runecraft:")) {
            id = id.substring("runecraft:".length());
        }
        return ITEMS.get(id);
    }

    public static boolean exists(String id) {
        return id.startsWith("minecraft:") || id.startsWith("#") || get(id) != null;
    }

    private static ItemDef add(String id, String name, Kind kind, String shape, int color) {
        if (ITEMS.containsKey(id)) {
            throw new IllegalStateException("Duplicate item " + id);
        }
        ItemDef def = new ItemDef(id, name, kind, shape, color);
        ITEMS.put(id, def);
        return def;
    }

    private static int tierValue(MetalTier tier) {
        return switch (tier) {
            case BRONZE -> 32;
            case IRON -> 112;
            case STEEL -> 400;
            case BLACK -> 768;
            case MITHRIL -> 1040;
            case ADAMANT -> 2560;
            case RUNE -> 25600;
            case DRAGON -> 100000;
        };
    }

    private static void build() {
        add("coins", "Coins", Kind.COINS, "coins", 0xE8C030).value(1);
        // Bank notes: one item whose stack data names the noted item (see osrs/bank/BankNotes).
        add(com.runecraft.osrs.bank.BankNotes.NOTE_ITEM, "Bank note", Kind.SIMPLE, "note", 0xF0E8C8).value(0).stack(64)
                .examine(com.runecraft.osrs.bank.BankNotes.EXAMINE);

        // --- Woodcutting / Firemaking ---
        for (Tree t : Tree.values()) {
            add(t.logItem(), t.logName(), Kind.SIMPLE, "log", t.color()).value(t.logValue());
        }
        for (String[] p : new String[][]{{"plank", "Plank", "TREE"}, {"oak_plank", "Oak plank", "OAK"},
                {"teak_plank", "Teak plank", "TEAK"}, {"mahogany_plank", "Mahogany plank", "MAHOGANY"}}) {
            Tree t = Tree.valueOf(p[2]);
            add(p[0], p[1], Kind.SIMPLE, "plank", t.color()).value(t.logValue() * 10);
        }

        // --- Mining / Smithing ---
        for (Rock r : Rock.values()) {
            if (r.oreItem() != null) {
                String shape = r == Rock.RUNE_ESSENCE || r == Rock.PURE_ESSENCE ? "essence" : r == Rock.CLAY ? "clay" : "ore";
                add(r.oreItem(), r.oreName(), Kind.SIMPLE, shape, r.color()).value(r.oreValue());
            }
        }
        for (Bar b : Bar.values()) {
            add(b.itemId(), b.displayName(), Kind.SIMPLE, "bar", b.color()).value(b.value());
        }
        for (ArrowTier a : ArrowTier.values()) {
            add(a.tipsId(), a.metal().displayName() + " arrowtips", Kind.SIMPLE, "arrowtips", a.metal().color())
                    .value(Math.max(1, tierValue(a.metal()) / 32));
        }

        // --- Tools ---
        for (ToolTier t : ToolTier.values()) {
            add(t.axeId(), t.metal().displayName() + " axe", Kind.AXE, "axe", t.metal().color())
                    .value(t.value()).stack(1).tier(t.metal().id())
                    .equipment(EquipmentDef.melee(EquipmentStats.attack(-2, (int) Math.round(t.woodcuttingMultiplier() * 4), 0,
                            (int) Math.round(t.woodcuttingMultiplier() * 4)), t.metal().requirement(), 5, AttackType.SLASH));
            add(t.pickaxeId(), t.metal().displayName() + " pickaxe", Kind.PICKAXE, "pickaxe", t.metal().color())
                    .value(t.value() * 5 / 4).stack(1).tier(t.metal().id())
                    .equipment(EquipmentDef.melee(EquipmentStats.attack((int) Math.round(t.woodcuttingMultiplier() * 4), -2, 0,
                            (int) Math.round(t.woodcuttingMultiplier() * 4)), t.metal().requirement(), 5, AttackType.STAB));
        }
        tool("tinderbox", "Tinderbox", "tinderbox", 0x8A6A3A, 1);
        tool("knife", "Knife", "knife", 0xB0B0B8, 6);
        tool("chisel", "Chisel", "chisel", 0x9A9AA0, 1);
        tool("hammer", "Hammer", "hammer", 0x7A7A80, 1);
        tool("needle", "Needle", "needle", 0xC8C8D0, 1);
        tool("small_fishing_net", "Small fishing net", "net", 0xC8B898, 5);
        tool("fishing_rod", "Fishing rod", "rod", 0x8A6A3A, 5);
        tool("fly_fishing_rod", "Fly fishing rod", "rod", 0x6A8A4A, 5);
        tool("lobster_pot", "Lobster pot", "pot", 0x9A7A4A, 20);
        tool("harpoon", "Harpoon", "harpoon", 0xA0A0A8, 5);
        tool("seed_dibber", "Seed dibber", "dibber", 0x8A6A3A, 6);
        tool("ring_mould", "Ring mould", "mould", 0x8A8A8A, 5);
        tool("necklace_mould", "Necklace mould", "mould", 0x8A8A8A, 5);
        tool("amulet_mould", "Amulet mould", "mould", 0x8A8A8A, 5);
        add("bird_snare", "Bird snare", Kind.TRAP, "snare", 0xC8B080).value(6);
        add("box_trap", "Box trap", Kind.TRAP, "box_trap", 0x8A6A3A).value(38);
        add("fishing_bait", "Fishing bait", Kind.SIMPLE, "bait", 0xA06050).value(3);
        add("dark_fishing_bait", "Dark fishing bait", Kind.SIMPLE, "bait", 0x5A3A5A).value(15);
        add("enchanted_gem", "Enchanted gem", Kind.SLAYER_GEM, "gem", 0x8A30C0).value(1).stack(1)
                .examine("I can contact my Slayer master with this.");
        add("mark_of_grace", "Mark of grace", Kind.SIMPLE, "mark", 0xE0C040).value(1);

        // --- Fishing / Cooking ---
        for (Fish f : Fish.values()) {
            add(f.rawItem(), f.rawName(), Kind.SIMPLE, "fish_raw", f.color()).value(f.value());
        }
        for (Fish f : Fish.values()) {
            add(f.cookedItem(), f.cookedName(), Kind.FOOD, "fish_cooked", f.color()).value(f.value()).heal(f.heal());
        }
        add("burnt_fish", "Burnt fish", Kind.SIMPLE, "fish_burnt", 0x2A2A2A).value(1);
        add("burnt_meat", "Burnt meat", Kind.SIMPLE, "meat_burnt", 0x2A2020).value(1);
        add("raw_bird_meat", "Raw bird meat", Kind.SIMPLE, "meat_raw", 0xE0A0A0).value(4);
        add("roast_bird_meat", "Roast bird meat", Kind.FOOD, "meat_cooked", 0xA06030).value(8).heal(6);
        add("cake", "Cake", Kind.FOOD, "cake", 0xE8D8A0).value(50).heal(4);
        add("chocolate_slice", "Chocolate slice", Kind.FOOD, "cake", 0x6A3A1A).value(30).heal(4);

        // --- Prayer ---
        for (Bone b : Bone.values()) {
            add(b.itemId(), b.displayName(), Kind.BONES, b == Bone.BONES ? "bones" : "big_bones", b.color())
                    .value((int) Math.max(1, b.xp() * 3));
        }

        // --- Runecraft / Magic ---
        for (Rune r : Rune.values()) {
            add(r.itemId(), r.displayName(), Kind.RUNE, "rune", runeColor(r)).value(runeValue(r));
        }
        add("staff", "Staff", Kind.STAFF, "staff", 0x8A6A3A).value(15).stack(1)
                .equipment(staff(10, 0));
        add("staff_of_air", "Staff of air", Kind.STAFF, "staff_orb", runeColor(Rune.AIR)).value(1500).stack(1)
                .equipment(staff(10, 0));
        add("staff_of_water", "Staff of water", Kind.STAFF, "staff_orb", runeColor(Rune.WATER)).value(1500).stack(1)
                .equipment(staff(10, 0));
        add("staff_of_earth", "Staff of earth", Kind.STAFF, "staff_orb", runeColor(Rune.EARTH)).value(1500).stack(1)
                .equipment(staff(10, 0));
        add("staff_of_fire", "Staff of fire", Kind.STAFF, "staff_orb", runeColor(Rune.FIRE)).value(1500).stack(1)
                .equipment(staff(10, 0));
        add("ancient_staff", "Ancient staff", Kind.STAFF, "staff_orb", 0x6A3A8A).value(80000).stack(1)
                .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON,
                        new EquipmentStats(10, -1, 40, 15, 0, 2, 3, 1, 15, 0, 50, 0, 0, -1),
                        Map.of(Skill.MAGIC, 50, Skill.ATTACK, 50), 4, AttackType.MAGIC, EquipmentDef.WeaponClass.STAFF));

        // --- Melee weapons & armour ---
        for (MetalTier tier : MetalTier.values()) {
            for (WeaponType w : WeaponType.values()) {
                add(w.itemId(tier), w.displayName(tier), Kind.MELEE_WEAPON, w.shape(), tier.color())
                        .value((int) (tierValue(tier) * w.valueFactor())).stack(1).tier(tier.id())
                        .equipment(EquipmentDef.melee(w.stats(tier), tier.requirement(), w.speed(), w.attackType()));
            }
            for (ArmourType a : ArmourType.values()) {
                Kind kind = a == ArmourType.KITESHIELD ? Kind.SHIELD : Kind.ARMOUR;
                add(a.itemId(tier), a.displayName(tier), kind, a.shape(), tier.color())
                        .value((int) (tierValue(tier) * a.valueFactor())).stack(1).tier(tier.id())
                        .equipment(EquipmentDef.armour(slotOf(a.slot()), a.stats(tier), tier.requirement()));
            }
        }

        // --- Fletching / Ranged ---
        add("arrow_shaft", "Arrow shaft", Kind.SIMPLE, "shaft", 0xA08050).value(1);
        add("headless_arrow", "Headless arrow", Kind.SIMPLE, "headless", 0xA08050).value(1);
        add("bow_string", "Bow string", Kind.SIMPLE, "string", 0xE0E0D0).value(5);
        add("flax", "Flax", Kind.SIMPLE, "flax", 0x7090C0).value(5);
        for (BowWood w : BowWood.values()) {
            for (boolean longbow : new boolean[]{false, true}) {
                add(w.bowId(longbow, false), w.bowName(longbow, false), Kind.SIMPLE, longbow ? "longbow_u" : "shortbow_u",
                        w.tree().color()).value(w.value(longbow) / 2).stack(1);
            }
        }
        for (BowWood w : BowWood.values()) {
            for (boolean longbow : new boolean[]{false, true}) {
                add(w.bowId(longbow, true), w.bowName(longbow, true), Kind.BOW, longbow ? "longbow" : "shortbow",
                        w.tree().color()).value(w.value(longbow)).stack(1).tier(w.id())
                        .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON,
                                new EquipmentStats(0, 0, 0, 0, w.rangedAttack(), 0, 0, 0, 0, 0, 0, 0, 0, 0),
                                w.rangedLevel() > 1 ? Map.of(Skill.RANGED, w.rangedLevel()) : Map.of(),
                                longbow ? 6 : 4, AttackType.RANGED, EquipmentDef.WeaponClass.BOW));
            }
        }
        for (ArrowTier a : ArrowTier.values()) {
            add(a.arrowId(), a.metal().displayName() + " arrow", Kind.ARROW, "arrow", a.metal().color())
                    .value(Math.max(1, tierValue(a.metal()) / 16)).rangedStrength(a.rangedStrength()).tier(a.metal().id());
        }

        // --- Crafting ---
        add("cowhide", "Cowhide", Kind.SIMPLE, "hide", 0xE8E0D0).value(1);
        add("leather", "Leather", Kind.SIMPLE, "leather", 0x9A6A3A).value(2);
        add("ball_of_wool", "Ball of wool", Kind.SIMPLE, "wool", 0xF0F0F0).value(2);
        add("silk", "Silk", Kind.SIMPLE, "cloth", 0xE8E0F0).value(30);
        add("grey_wolf_fur", "Grey wolf fur", Kind.SIMPLE, "fur", 0x8A8A8A).value(50);
        add("spice", "Spice", Kind.SIMPLE, "spice", 0xC04020).value(230);
        add("leather_cowl", "Leather cowl", Kind.ARMOUR, "cowl", 0x9A6A3A).value(24).stack(1).tier("leather")
                .equipment(EquipmentDef.armour(EquipmentDef.Slot.HEAD, EquipmentStats.defence(2, 3, 4, 3, 2, 1, 2), 1));
        add("leather_body", "Leather body", Kind.ARMOUR, "leather_body", 0x9A6A3A).value(21).stack(1).tier("leather")
                .equipment(EquipmentDef.armour(EquipmentDef.Slot.CHEST, EquipmentStats.defence(8, 9, 10, 4, 9, 2, 2), 1));
        add("leather_chaps", "Leather chaps", Kind.ARMOUR, "chaps", 0x9A6A3A).value(20).stack(1).tier("leather")
                .equipment(EquipmentDef.armour(EquipmentDef.Slot.LEGS, EquipmentStats.defence(2, 2, 2, 2, 2, 0, 4), 1));
        add("leather_boots", "Leather boots", Kind.ARMOUR, "boots", 0x9A6A3A).value(6).stack(1).tier("leather")
                .equipment(EquipmentDef.armour(EquipmentDef.Slot.FEET, EquipmentStats.defence(1, 1, 1, 0, 0, 0, 0), 1));
        for (Gem g : Gem.values()) {
            add(g.uncutItem(), "Uncut " + g.displayName().toLowerCase(java.util.Locale.ROOT), Kind.SIMPLE, "gem_uncut", g.color())
                    .value(g.value() / 2);
        }
        for (Gem g : Gem.values()) {
            add(g.cutItem(), g.displayName(), Kind.SIMPLE, "gem", g.color()).value(g.value());
        }
        add("gold_ring", "Gold ring", Kind.SIMPLE, "ring", 0xE8C030).value(350);
        add("gold_necklace", "Gold necklace", Kind.SIMPLE, "necklace", 0xE8C030).value(450);
        add("gold_amulet", "Gold amulet", Kind.SIMPLE, "amulet", 0xE8C030).value(350);
        add("sapphire_ring", "Sapphire ring", Kind.SIMPLE, "ring", Gem.SAPPHIRE.color()).value(900);
        add("emerald_ring", "Emerald ring", Kind.SIMPLE, "ring", Gem.EMERALD.color()).value(1275);
        add("ruby_ring", "Ruby ring", Kind.SIMPLE, "ring", Gem.RUBY.color()).value(2025);
        add("diamond_ring", "Diamond ring", Kind.SIMPLE, "ring", Gem.DIAMOND.color()).value(3525);

        // --- Herblore ---
        add("vial", "Vial", Kind.VIAL, "vial", 0xD0E0F0).value(2);
        add("vial_of_water", "Vial of water", Kind.SIMPLE, "vial_water", 0x6090E0).value(2);
        for (Herb h : Herb.values()) {
            add(h.grimyItem(), "Grimy " + h.displayName().toLowerCase(java.util.Locale.ROOT), Kind.GRIMY_HERB, "herb_grimy",
                    h.color()).value(h.value());
        }
        for (Herb h : Herb.values()) {
            add(h.cleanItem(), h.displayName(), Kind.SIMPLE, "herb", h.color()).value(h.value());
        }
        for (Herb h : Herb.values()) {
            add(h.unfinishedPotion(), h.shortName() + " potion (unf)", Kind.SIMPLE, "potion_unf", h.color()).value(h.value());
        }
        String[][] secondaries = {
                {"eye_of_newt", "Eye of newt", "C8C850", "3"},
                {"unicorn_horn_dust", "Unicorn horn dust", "F0F0F0", "20"},
                {"limpwurt_root", "Limpwurt root", "C09050", "8"},
                {"red_spiders_eggs", "Red spiders' eggs", "C02020", "10"},
                {"chocolate_dust", "Chocolate dust", "6A3A1A", "10"},
                {"white_berries", "White berries", "F0F0E0", "10"},
                {"wine_of_zamorak", "Wine of zamorak", "8A1A2A", "50"},
                {"potato_cactus", "Potato cactus", "8AA040", "40"},
                {"crushed_nest", "Crushed nest", "A08050", "50"}};
        for (String[] s : secondaries) {
            add(s[0], s[1], Kind.SIMPLE, "secondary", Integer.parseInt(s[2], 16)).value(Integer.parseInt(s[3]));
        }
        for (Potion p : Potion.values()) {
            add(p.itemId(), p.displayName(), Kind.POTION, "potion", p.color()).value(p.level() * 4).stack(1);
        }

        // --- Farming ---
        for (Herb h : Herb.values()) {
            add(h.seedItem(), h.shortName() + " seed", Kind.SEED, "seed", h.color()).value(h.value());
        }
        for (Allotment a : Allotment.values()) {
            add(a.seedItem(), a.displayName() + " seed", Kind.SEED, "seed", a.color()).value(Math.max(1, a.value() / 2));
        }
        for (Allotment a : Allotment.values()) {
            if (a == Allotment.SNAPE_GRASS) {
                add(a.produceItem(), a.displayName(), Kind.SIMPLE, "secondary", a.color()).value(a.value());
            } else {
                add(a.produceItem(), a.displayName(), Kind.FOOD, "produce", a.color()).value(a.value())
                        .heal(a == Allotment.WATERMELON ? 5 : a == Allotment.STRAWBERRY ? 6 : a == Allotment.SWEETCORN ? 3 : 1);
            }
        }

        // --- Bosses, keys & quest items ---
        bossAndQuestItems();

        // --- Hunter ---
        add("ferret", "Ferret", Kind.SIMPLE, "creature", 0xC8B898).value(10);
        add("grey_chinchompa", "Chinchompa", Kind.SIMPLE, "creature", 0xA0A0A0).value(50);
        add("red_chinchompa", "Red chinchompa", Kind.SIMPLE, "creature", 0xC04030).value(100);
        add("black_chinchompa", "Black chinchompa", Kind.SIMPLE, "creature", 0x303030).value(200);

        // --- Items shared by several content modules ---
        com.runecraft.osrs.content.CommonItems.add(ItemCatalog::add);

        // --- Content modules ---
        for (com.runecraft.osrs.content.ContentModule module : com.runecraft.osrs.content.Modules.ALL) {
            module.items(ItemCatalog::add);
        }
        // --- Reference OSRS bonuses / attack speeds override whatever the defining module wrote ---
        for (ItemDef def : ITEMS.values()) {
            com.runecraft.osrs.data.EquipmentBonuses.apply(def);
            // Reference OSRS store value, weight, stackability and examine text (osrs/data/ItemValues).
            com.runecraft.osrs.data.ItemValues.apply(def);
        }
    }

    private static EquipmentDef wearable(EquipmentDef.Slot slot, EquipmentStats stats, Object... reqs) {
        Map<Skill, Integer> map = new java.util.EnumMap<>(Skill.class);
        for (int i = 0; i < reqs.length; i += 2) {
            map.put((Skill) reqs[i], (Integer) reqs[i + 1]);
        }
        return new EquipmentDef(slot, stats, map, 0, AttackType.CRUSH, EquipmentDef.WeaponClass.NONE);
    }

    private static EquipmentDef weapon(EquipmentStats stats, int speed, AttackType type, EquipmentDef.WeaponClass cls, Object... reqs) {
        Map<Skill, Integer> map = new java.util.EnumMap<>(Skill.class);
        for (int i = 0; i < reqs.length; i += 2) {
            map.put((Skill) reqs[i], (Integer) reqs[i + 1]);
        }
        return new EquipmentDef(EquipmentDef.Slot.WEAPON, stats, map, speed, type, cls);
    }

    private static EquipmentStats stats(int sa, int sl, int cr, int ma, int ra, int sd, int sld, int cd, int md, int rd,
                                        int str, int rstr, int mdmg, int prayer) {
        return new EquipmentStats(sa, sl, cr, ma, ra, sd, sld, cd, md, rd, str, rstr, mdmg, prayer);
    }

    private static void armour(String id, String name, String shape, int color, String tier, int value, EquipmentDef eq) {
        add(id, name, Kind.ARMOUR, shape, color).value(value).stack(1).tier(tier).equipment(eq);
    }

    private static void bossAndQuestItems() {
        final EquipmentDef.Slot H = EquipmentDef.Slot.HEAD;
        final EquipmentDef.Slot C = EquipmentDef.Slot.CHEST;
        final EquipmentDef.Slot L = EquipmentDef.Slot.LEGS;
        final EquipmentDef.Slot F = EquipmentDef.Slot.FEET;
        int barrows = 0x6A6A60;
        // Obor & Bryophyta
        add("giant_key", "Giant key", Kind.SIMPLE, "key", 0xB09060).value(1).examine("Opens Obor's lair.");
        add("mossy_key", "Mossy key", Kind.SIMPLE, "key", 0x4A7A2A).value(1).examine("Opens Bryophyta's lair.");
        add("hill_giant_club", "Hill giant club", Kind.MELEE_WEAPON, "club", 0x8A6A3A).value(200000).stack(1).tier("rune")
                .equipment(weapon(stats(-4, -4, 65, 0, 0, 0, 0, 0, 0, 0, 70, 0, 0, 0), 7, AttackType.CRUSH,
                        EquipmentDef.WeaponClass.MELEE, Skill.ATTACK, 40));
        add("bryophytas_essence", "Bryophyta's essence", Kind.SIMPLE, "essence", 0x3AAA3A).value(150000);
        // Barrows - Ahrim
        armour("ahrims_hood", "Ahrim's hood", "cowl", 0x3A3A5A, "barrows", 50000,
                wearable(H, stats(0, 0, 0, 6, -2, 15, 13, 16, 6, 0, 0, 0, 0, 0), Skill.MAGIC, 70, Skill.DEFENCE, 70));
        armour("ahrims_robetop", "Ahrim's robetop", "leather_body", 0x3A3A5A, "barrows", 120000,
                wearable(C, stats(0, 0, 0, 30, -10, 52, 37, 63, 30, 0, 0, 0, 0, 0), Skill.MAGIC, 70, Skill.DEFENCE, 70));
        armour("ahrims_robeskirt", "Ahrim's robeskirt", "chaps", 0x3A3A5A, "barrows", 110000,
                wearable(L, stats(0, 0, 0, 22, -7, 22, 20, 24, 22, 0, 0, 0, 0, 0), Skill.MAGIC, 70, Skill.DEFENCE, 70));
        add("ahrims_staff", "Ahrim's staff", Kind.STAFF, "staff_orb", 0x3A3A5A).value(100000).stack(1).tier("barrows")
                .equipment(weapon(stats(12, -1, 65, 15, 0, 3, 5, 2, 15, 0, 68, 0, 5, 0), 5, AttackType.CRUSH,
                        EquipmentDef.WeaponClass.STAFF, Skill.MAGIC, 70, Skill.ATTACK, 70));
        // Dharok
        armour("dharoks_helm", "Dharok's helm", "full_helm", barrows, "barrows", 100000,
                wearable(H, stats(0, 0, 0, -3, -1, 45, 48, 44, -1, 51, 0, 0, 0, 0), Skill.DEFENCE, 70));
        armour("dharoks_platebody", "Dharok's platebody", "platebody", barrows, "barrows", 250000,
                wearable(C, stats(0, 0, 0, -30, -10, 122, 120, 110, -6, 120, 0, 0, 0, 0), Skill.DEFENCE, 70));
        armour("dharoks_platelegs", "Dharok's platelegs", "platelegs", barrows, "barrows", 250000,
                wearable(L, stats(0, 0, 0, -21, -7, 85, 82, 83, -4, 92, 0, 0, 0, 0), Skill.DEFENCE, 70));
        add("dharoks_greataxe", "Dharok's greataxe", Kind.MELEE_WEAPON, "battleaxe", barrows).value(208000).stack(1).tier("rune")
                .equipment(weapon(stats(-4, 103, 95, 0, 0, 0, 0, 0, 0, 0, 105, 0, 0, 0), 7, AttackType.SLASH,
                        EquipmentDef.WeaponClass.MELEE, Skill.ATTACK, 70, Skill.STRENGTH, 70));
        // Guthan
        armour("guthans_helm", "Guthan's helm", "full_helm", 0x6A6A5A, "barrows", 100000,
                wearable(H, stats(0, 0, 0, -6, -2, 55, 58, 54, -1, 62, 0, 0, 0, 0), Skill.DEFENCE, 70));
        armour("guthans_platebody", "Guthan's platebody", "platebody", 0x6A6A5A, "barrows", 250000,
                wearable(C, stats(0, 0, 0, -30, -10, 132, 130, 117, -6, 138, 0, 0, 0, 0), Skill.DEFENCE, 70));
        armour("guthans_chainskirt", "Guthan's chainskirt", "platelegs", 0x6A6A5A, "barrows", 250000,
                wearable(L, stats(0, 0, 0, -21, -7, 85, 82, 83, -4, 92, 0, 0, 0, 0), Skill.DEFENCE, 70));
        add("guthans_warspear", "Guthan's warspear", Kind.MELEE_WEAPON, "spear", 0x6A6A5A).value(196000).stack(1).tier("rune")
                .equipment(weapon(stats(75, 75, 75, 0, 0, 0, 0, 0, 0, 0, 75, 0, 0, 0), 5, AttackType.STAB,
                        EquipmentDef.WeaponClass.MELEE, Skill.ATTACK, 70));
        // Karil
        armour("karils_coif", "Karil's coif", "cowl", 0x4A3A2A, "barrows", 50000,
                wearable(H, stats(0, 0, 0, -1, 7, 6, 9, 12, 6, 10, 0, 0, 0, 0), Skill.RANGED, 70, Skill.DEFENCE, 70));
        armour("karils_leathertop", "Karil's leathertop", "leather_body", 0x4A3A2A, "barrows", 150000,
                wearable(C, stats(0, 0, 0, -15, 30, 47, 42, 50, 65, 57, 0, 0, 0, 0), Skill.RANGED, 70, Skill.DEFENCE, 70));
        armour("karils_leatherskirt", "Karil's leatherskirt", "chaps", 0x4A3A2A, "barrows", 100000,
                wearable(L, stats(0, 0, 0, -8, 17, 26, 20, 28, 34, 35, 0, 0, 0, 0), Skill.RANGED, 70, Skill.DEFENCE, 70));
        add("karils_crossbow", "Karil's crossbow", Kind.BOW, "crossbow", 0x4A3A2A).value(140000).stack(1).tier("barrows")
                .equipment(weapon(stats(0, 0, 0, 0, 84, 0, 0, 0, 0, 0, 0, 0, 0, 0), 4, AttackType.RANGED,
                        EquipmentDef.WeaponClass.BOW, Skill.RANGED, 70));
        // Torag
        armour("torags_helm", "Torag's helm", "full_helm", 0x5A4A3A, "barrows", 100000,
                wearable(H, stats(0, 0, 0, -6, -2, 55, 58, 54, -1, 62, 0, 0, 0, 0), Skill.DEFENCE, 70));
        armour("torags_platebody", "Torag's platebody", "platebody", 0x5A4A3A, "barrows", 250000,
                wearable(C, stats(0, 0, 0, -30, -10, 122, 120, 110, -6, 120, 0, 0, 0, 0), Skill.DEFENCE, 70));
        armour("torags_platelegs", "Torag's platelegs", "platelegs", 0x5A4A3A, "barrows", 250000,
                wearable(L, stats(0, 0, 0, -21, -7, 85, 82, 83, -4, 92, 0, 0, 0, 0), Skill.DEFENCE, 70));
        add("torags_hammers", "Torag's hammers", Kind.MELEE_WEAPON, "warhammer", 0x5A4A3A).value(196000).stack(1).tier("rune")
                .equipment(weapon(stats(-4, -4, 85, 0, 0, 0, 0, 0, 0, 0, 72, 0, 0, 0), 5, AttackType.CRUSH,
                        EquipmentDef.WeaponClass.MELEE, Skill.ATTACK, 70, Skill.STRENGTH, 70));
        // Verac
        armour("veracs_helm", "Verac's helm", "full_helm", 0x6A5A4A, "barrows", 100000,
                wearable(H, stats(0, 0, 0, -6, -2, 55, 58, 54, 0, 56, 0, 0, 0, 3), Skill.DEFENCE, 70));
        armour("veracs_brassard", "Verac's brassard", "platebody", 0x6A5A4A, "barrows", 250000,
                wearable(C, stats(0, 0, 0, -6, -5, 81, 95, 85, 0, 81, 0, 0, 0, 5), Skill.DEFENCE, 70));
        armour("veracs_plateskirt", "Verac's plateskirt", "platelegs", 0x6A5A4A, "barrows", 250000,
                wearable(L, stats(0, 0, 0, -21, -7, 85, 82, 83, 0, 84, 0, 0, 0, 4), Skill.DEFENCE, 70));
        add("veracs_flail", "Verac's flail", Kind.MELEE_WEAPON, "flail", 0x6A5A4A).value(196000).stack(1).tier("rune")
                .equipment(weapon(stats(68, -2, 82, 0, 0, 0, 0, 0, 0, 0, 72, 0, 0, 6), 5, AttackType.CRUSH,
                        EquipmentDef.WeaponClass.MELEE, Skill.ATTACK, 70));
        // God Wars Dungeon
        armour("bandos_chestplate", "Bandos chestplate", "platebody", 0x6A7A3A, "bandos", 2000000,
                wearable(C, stats(0, 0, 0, -15, -10, 98, 93, 105, -6, 133, 4, 0, 0, 1), Skill.DEFENCE, 65));
        armour("bandos_tassets", "Bandos tassets", "platelegs", 0x6A7A3A, "bandos", 2000000,
                wearable(L, stats(0, 0, 0, -21, -7, 71, 63, 66, -4, 93, 2, 0, 0, 1), Skill.DEFENCE, 65));
        armour("bandos_boots", "Bandos boots", "boots", 0x6A7A3A, "bandos", 300000,
                wearable(F, stats(0, 0, 0, -5, -3, 17, 18, 19, 0, 15, 1, 0, 0, 1), Skill.DEFENCE, 65));
        add("zamorakian_spear", "Zamorakian spear", Kind.MELEE_WEAPON, "spear", 0x801818).value(800000).stack(1).tier("dragon")
                .equipment(weapon(stats(85, 65, 65, 0, 0, 13, 13, 12, 0, 13, 75, 0, 0, 2), 4, AttackType.STAB,
                        EquipmentDef.WeaponClass.MELEE, Skill.ATTACK, 70));
        add("steam_battlestaff", "Steam battlestaff", Kind.STAFF, "staff_orb", 0xA0A0C0).value(45000).stack(1).tier("rune")
                .equipment(weapon(stats(10, -1, 40, 12, 0, 2, 3, 1, 12, 0, 45, 0, 0, 0), 4, AttackType.CRUSH,
                        EquipmentDef.WeaponClass.STAFF, Skill.MAGIC, 30, Skill.ATTACK, 30));
        add("staff_of_the_dead", "Staff of the dead", Kind.STAFF, "staff_orb", 0x205020).value(1500000).stack(1).tier("dragon")
                .equipment(weapon(stats(55, 70, 0, 17, 0, 0, 3, 3, 17, 0, 72, 0, 15, 0), 4, AttackType.SLASH,
                        EquipmentDef.WeaponClass.STAFF, Skill.MAGIC, 75, Skill.ATTACK, 75));
        // Fight Caves
        // Worn in the systems module's cape slot (Jewellery); the HEAD-slot equipment only feeds tooltips, the item
        // is SIMPLE so it can't be equipped in a vanilla slot (it used to count as an off-hand shield).
        add("fire_cape", "Fire cape", Kind.SIMPLE, "cape", 0xE05010).value(64000).stack(1).tier("dragon")
                .equipment(wearable(EquipmentDef.Slot.HEAD, stats(1, 1, 1, 1, 1, 11, 11, 11, 11, 11, 4, 0, 0, 2)));
        add("tokkul", "Tokkul", Kind.SIMPLE, "tokkul", 0x5A3A2A).value(1);
        add("tzrek_jad", "TzRek-Jad", Kind.SIMPLE, "pet", 0xE05010).value(1).stack(1).examine("A pet TzTok-Jad.");
        // Quest items
        add("pot_of_flour", "Pot of flour", Kind.SIMPLE, "pot_item", 0xF0F0E0).value(10);
        add("ghostspeak_amulet", "Ghostspeak amulet", Kind.SIMPLE, "amulet", 0x60E0A0).value(35).stack(1);
        add("ghosts_skull", "Ghost's skull", Kind.SIMPLE, "skull", 0xE8E0D0).value(1).stack(1);
        add("black_bead", "Black bead", Kind.SIMPLE, "bead", 0x202020).value(1);
        add("red_bead", "Red bead", Kind.SIMPLE, "bead", 0xC02020).value(1);
        add("yellow_bead", "Yellow bead", Kind.SIMPLE, "bead", 0xE0D020).value(1);
        add("white_bead", "White bead", Kind.SIMPLE, "bead", 0xF0F0F0).value(1);
        add("rats_tail", "Rat's tail", Kind.SIMPLE, "tail", 0xC09080).value(1);
        add("cadava_berries", "Cadava berries", Kind.SIMPLE, "secondary", 0x8A2A8A).value(3);
        add("cadava_potion", "Cadava potion", Kind.SIMPLE, "potion_unf", 0x8A2A8A).value(3).stack(1);
        add("message_from_juliet", "Message", Kind.SIMPLE, "note", 0xE8E0C0).value(1).stack(1);
        add("research_package", "Research package", Kind.SIMPLE, "package", 0xA08050).value(1).stack(1);
        add("research_notes", "Research notes", Kind.SIMPLE, "note", 0xE8E0C0).value(1).stack(1);
        add("air_talisman", "Air talisman", Kind.SIMPLE, "talisman", 0xE0E0E8).value(4).stack(1);
        add("silverlights_key", "Silverlight key", Kind.SIMPLE, "key", 0xC0C0C8).value(1).stack(1);
        add("stake", "Stake", Kind.SIMPLE, "stake", 0x8A6A3A).value(1).stack(1);
        add("garlic", "Garlic", Kind.FOOD, "produce", 0xF0F0E0).value(1).heal(1);
        add("beer", "Beer", Kind.FOOD, "beer", 0xE0A030).value(2).heal(1);
        add("silverlight", "Silverlight", Kind.MELEE_WEAPON, "longsword", 0xD8D8F0).value(50).stack(1).tier("rune")
                .equipment(weapon(stats(9, 14, -2, 0, 0, 0, 3, 2, 1, 0, 12, 0, 0, 1), 4, AttackType.SLASH,
                        EquipmentDef.WeaponClass.MELEE));
        // Champion scrolls (OSRS: 1/5000 from the champions' kin) open a challenge in the Champions' Guild.
        for (String[] c : new String[][]{{"goblin", "Goblin"}, {"imp", "Imp"}, {"skeleton", "Skeleton"}, {"zombie", "Zombie"},
                {"ghoul", "Ghoul"}, {"giant", "Giant"}, {"hobgoblin", "Hobgoblin"}, {"jogre", "Jogre"},
                {"lesser_demon", "Lesser demon"}, {"earth_warrior", "Earth warrior"}}) {
            add(c[0] + "_champion_scroll", c[1] + " champion scroll", Kind.SIMPLE, "scroll", 0xE8D8A8).value(1).stack(1)
                    .examine("A champion scroll. It's a challenge from the " + c[1].toLowerCase() + " champion.");
        }
        add("amulet_of_accuracy", "Amulet of accuracy", Kind.SIMPLE, "amulet", 0x3060E0).value(100).stack(1);
        // Dragon equipment that is bought or dropped, not smithed (bonuses: data/EquipmentBonuses).
        // Quest gates (Heroes' Quest, Lost City, Regicide, Legends' Quest) live in the quests module.
        int dragon = MetalTier.DRAGON.color();
        armour("dragon_med_helm", "Dragon med helm", "med_helm", dragon, "dragon", 100000,
                wearable(H, stats(0, 0, 0, -3, 0, 33, 35, 32, -1, 34, 0, 0, 0, 0), Skill.DEFENCE, 60));
        armour("dragon_plateskirt", "Dragon plateskirt", "plateskirt", dragon, "dragon", 270000,
                wearable(L, stats(0, 0, 0, -21, -11, 68, 66, 63, -4, 65, 0, 0, 0, 0), Skill.DEFENCE, 60));
        add("dragon_sq_shield", "Dragon sq shield", Kind.SHIELD, "sq_shield", dragon).value(500000).stack(1).tier("dragon")
                .equipment(wearable(EquipmentDef.Slot.SHIELD, stats(0, 0, 0, -6, 0, 50, 52, 48, 0, 50, 0, 0, 0, 0), Skill.DEFENCE, 60));
        add("dragon_mace", "Dragon mace", Kind.MELEE_WEAPON, "mace", dragon).value(50000).stack(1).tier("dragon")
                .equipment(weapon(stats(40, -2, 60, 0, 0, 0, 0, 0, 0, 0, 55, 0, 0, 5), 4, AttackType.CRUSH,
                        EquipmentDef.WeaponClass.MELEE, Skill.ATTACK, 60));
        add("dragon_spear", "Dragon spear", Kind.MELEE_WEAPON, "spear", dragon).value(62400).stack(1).tier("dragon")
                .equipment(weapon(stats(55, 55, 55, 0, 0, 5, 5, 5, 5, 5, 60, 0, 0, 0), 4, AttackType.STAB,
                        EquipmentDef.WeaponClass.MELEE, Skill.ATTACK, 60));
        // Rune spear: only from the rare drop table's mega-rare slot (and a few monsters) in OSRS.
        add("rune_spear", "Rune spear", Kind.MELEE_WEAPON, "spear", MetalTier.RUNE.color()).value(20800).stack(1).tier("rune")
                .equipment(weapon(stats(36, 36, 36, 0, 0, 1, 1, 1, 0, 0, 42, 0, 0, 0), 5, AttackType.STAB,
                        EquipmentDef.WeaponClass.MELEE, Skill.ATTACK, 40));
        add("dragon_halberd", "Dragon halberd", Kind.MELEE_WEAPON, "spear", dragon).value(325000).stack(1).tier("dragon")
                .equipment(weapon(stats(70, 95, 0, -4, 0, -1, 4, 5, 0, 0, 89, 0, 0, 0), 7, AttackType.SLASH,
                        EquipmentDef.WeaponClass.MELEE, Skill.ATTACK, 60, Skill.STRENGTH, 30));
    }

    private static EquipmentDef.Slot slotOf(ArmourType.Slot slot) {
        return switch (slot) {
            case HEAD -> EquipmentDef.Slot.HEAD;
            case CHEST -> EquipmentDef.Slot.CHEST;
            case LEGS -> EquipmentDef.Slot.LEGS;
            case FEET -> EquipmentDef.Slot.FEET;
            case SHIELD -> EquipmentDef.Slot.SHIELD;
        };
    }

    private static EquipmentDef staff(int magicAttack, int magicLevel) {
        return new EquipmentDef(EquipmentDef.Slot.WEAPON,
                new EquipmentStats(2, -1, 10, magicAttack, 0, 2, 3, 1, magicAttack, 0, 7, 0, 0, 0),
                magicLevel > 1 ? Map.of(Skill.MAGIC, magicLevel) : Map.of(), 5, AttackType.CRUSH, EquipmentDef.WeaponClass.STAFF);
    }

    private static void tool(String id, String name, String shape, int color, int value) {
        add(id, name, Kind.TOOL, shape, color).value(value).stack(1);
    }

    private static int runeColor(Rune r) {
        return switch (r) {
            case AIR -> 0xE0E0E8;
            case WATER -> 0x3060E0;
            case EARTH -> 0x8A5A2A;
            case FIRE -> 0xE04010;
            case MIND -> 0xE08020;
            case BODY -> 0x4040C0;
            case COSMIC -> 0xE0E040;
            case CHAOS -> 0xE06010;
            case ASTRAL -> 0xC0A0E0;
            case NATURE -> 0x30A030;
            case LAW -> 0x4060E0;
            case DEATH -> 0xE0E0E0;
            case BLOOD -> 0xC01010;
            case SOUL -> 0xA040C0;
            case WRATH -> 0x801010;
        };
    }

    private static int runeValue(Rune r) {
        return switch (r) {
            case AIR, WATER, EARTH, FIRE -> 4;
            case MIND, BODY -> 3;
            case COSMIC -> 50;
            case CHAOS -> 90;
            case ASTRAL -> 50;
            case NATURE -> 180;
            case LAW -> 240;
            case DEATH -> 180;
            case BLOOD -> 400;
            case SOUL -> 300;
            case WRATH -> 400;
        };
    }
}
