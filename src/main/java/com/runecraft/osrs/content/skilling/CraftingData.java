package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentDef.Slot;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.data.Gem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Crafting: dragonhide, snakeskin and hard/studded leather armour, battlestaves and elemental orbs,
 * pottery, glassblowing, the full gem jewellery table (gold and silver, unstrung and strung),
 * silver items, gold tiaras, spinning (crossbow string from sinew or roots, magic string), weaving at a
 * loom (strip of cloth, empty sack, basket) and bird houses. Levels/XP are OSRS values; armour bonuses for
 * dragonhide/snakeskin are close approximations of the in-game ones.
 *
 * <p>Menus: tanning at the tanner (TANNING), sewing with a needle or at a crafting table
 * (LEATHERWORKING), shaping on a potter's wheel (SPINNING), firing and molten glass at a pottery
 * oven or furnace (SMELTING), gold/silver jewellery at a furnace (SMELTING), weaving at a loom (SPINNING: the
 * Minecraft looms in Gielinor's buildings open it, see SkillingHooks).
 */
public final class CraftingData extends SkillingPart {

    /** Dragonhide tiers: crafting levels (vambraces, chaps, shield, body) and the Ranged requirement. */
    private record Hide(String id, String name, int color, int vamb, int chaps, int shield, int body, double vambXp,
                        int ranged, int rangedAtk, int bodyDef, int chapsDef, int shieldDef) {
    }

    private static final Hide[] HIDES = {
            new Hide("green", "Green", 0x3A8A3A, 57, 60, 62, 63, 62.0, 40, 15, 40, 12, 21),
            new Hide("blue", "Blue", 0x3A5AB0, 66, 68, 69, 71, 70.0, 50, 20, 45, 17, 25),
            new Hide("red", "Red", 0xB03A3A, 73, 75, 76, 77, 78.0, 60, 25, 50, 24, 29),
            new Hide("black", "Black", 0x2A2A2A, 79, 82, 83, 84, 86.0, 70, 30, 55, 31, 33)};

    /** Jewellery: gem (null for gold/silver), type, level, XP. */
    public record Jewel(String gem, String type, int level, double xp, boolean silver) {
        public String id() {
            return (gem == null ? (silver ? "silver" : "gold") : gem) + "_" + type;
        }
    }

    public static final List<Jewel> JEWELLERY = new ArrayList<>();

    static {
        jewel(null, 0, 0, 7, 0, 0, 0, 25, 0);  // gold bracelet only (ring/necklace/amulet are built in)
        jewel("sapphire", 0, 22, 23, 24, 0, 55, 60, 65);
        jewel("emerald", 0, 29, 30, 31, 0, 60, 65, 70);
        jewel("ruby", 0, 40, 42, 50, 0, 75, 80, 85);
        jewel("diamond", 0, 56, 58, 70, 0, 90, 95, 100);
        jewel("dragonstone", 55, 72, 74, 80, 100, 105, 110, 150);
        jewel("onyx", 67, 82, 84, 90, 115, 120, 125, 165);
        jewel("zenyte", 89, 92, 95, 98, 150, 165, 180, 200);
        silverJewel("opal", 1, 16, 22, 27, 10, 35, 45, 55);
        silverJewel("jade", 13, 25, 29, 34, 32, 54, 60, 70);
        silverJewel("topaz", 16, 32, 38, 45, 35, 70, 75, 80);
    }

    private static void jewel(String gem, int ring, int neck, int brace, int amulet, double ringXp, double neckXp,
                              double braceXp, double amuletXp) {
        if (ring > 0) {
            JEWELLERY.add(new Jewel(gem, "ring", ring, ringXp, false));
        }
        if (neck > 0) {
            JEWELLERY.add(new Jewel(gem, "necklace", neck, neckXp, false));
        }
        if (brace > 0) {
            JEWELLERY.add(new Jewel(gem, "bracelet", brace, braceXp, false));
        }
        if (amulet > 0) {
            JEWELLERY.add(new Jewel(gem, "amulet", amulet, amuletXp, false));
        }
    }

    private static void silverJewel(String gem, int ring, int neck, int brace, int amulet, double ringXp, double neckXp,
                                    double braceXp, double amuletXp) {
        JEWELLERY.add(new Jewel(gem, "ring", ring, ringXp, true));
        JEWELLERY.add(new Jewel(gem, "necklace", neck, neckXp, true));
        JEWELLERY.add(new Jewel(gem, "bracelet", brace, braceXp, true));
        JEWELLERY.add(new Jewel(gem, "amulet", amulet, amuletXp, true));
    }

    private static String gemItem(String gem) {
        return gem.equals("topaz") ? "red_topaz" : gem;
    }

    private static int gemColor(String gem) {
        for (Gem g : Gem.values()) {
            if (g.cutItem().equals(gemItem(gem))) {
                return g.color();
            }
        }
        return 0xE8C030;
    }

    private static int gemValue(String gem) {
        for (Gem g : Gem.values()) {
            if (g.cutItem().equals(gemItem(gem))) {
                return g.value();
            }
        }
        return 0;
    }

    /** Elemental orbs: charged at obelisks (Magic level/XP), attached to battlestaves (Crafting level/XP). */
    public record Orb(String element, int magicLevel, double magicXp, int craftLevel, double craftXp, String rune) {
        public String orb() {
            return element + "_orb";
        }

        public String obelisk() {
            return "obelisk_of_" + element;
        }

        public String staff() {
            return element + "_battlestaff";
        }
    }

    public static final List<Orb> ORBS = List.of(
            new Orb("water", 56, 66.0, 54, 100.0, "water_rune"),
            new Orb("earth", 60, 70.0, 58, 112.5, "earth_rune"),
            new Orb("fire", 63, 73.0, 62, 125.0, "fire_rune"),
            new Orb("air", 66, 76.0, 66, 137.5, "air_rune"));

    /** Bird houses: log, crafting level/XP, hunter level and the Hunter XP for a full house. */
    public record BirdHouse(String id, String name, String log, int craftLevel, double craftXp, int hunterLevel,
                            double hunterXp, int color) {
    }

    public static final List<BirdHouse> BIRD_HOUSES = List.of(
            new BirdHouse("bird_house", "Bird house", "logs", 5, 15.0, 5, 280.0, 0x6B4A2A),
            new BirdHouse("oak_bird_house", "Oak bird house", "oak_logs", 15, 20.0, 14, 420.0, 0x8A6A3A),
            new BirdHouse("willow_bird_house", "Willow bird house", "willow_logs", 25, 25.0, 24, 560.0, 0x6E7A3A),
            new BirdHouse("teak_bird_house", "Teak bird house", "teak_logs", 35, 30.0, 34, 700.0, 0x9C7A4A),
            new BirdHouse("maple_bird_house", "Maple bird house", "maple_logs", 45, 35.0, 44, 820.0, 0xA0502A),
            new BirdHouse("mahogany_bird_house", "Mahogany bird house", "mahogany_logs", 50, 40.0, 49, 960.0, 0x6A2A1A),
            new BirdHouse("yew_bird_house", "Yew bird house", "yew_logs", 60, 45.0, 59, 1020.0, 0x5A4A2A),
            new BirdHouse("magic_bird_house", "Magic bird house", "magic_logs", 75, 50.0, 74, 1140.0, 0x3A8AA8),
            new BirdHouse("redwood_bird_house", "Redwood bird house", "redwood_logs", 90, 55.0, 89, 1200.0, 0xA03A2A));

    /** Snelm id, name, shell (a common item dropped by the snails) and colour. */
    public static final String[][] SNELMS = {
            {"myre_snelm", "Myre snelm", "blamish_myre_shell_round", "7A7A3A"},
            {"pointed_myre_snelm", "Pointed myre snelm", "blamish_myre_shell_pointed", "7A7A3A"},
            {"ochre_snelm", "Ochre snelm", "blamish_ochre_shell_round", "C0902A"},
            {"pointed_ochre_snelm", "Pointed ochre snelm", "blamish_ochre_shell_pointed", "C0902A"},
            {"bloodntar_snelm", "Blood'n'tar snelm", "blamish_red_shell_round", "8A1A1A"},
            {"pointed_bloodntar_snelm", "Pointed blood'n'tar snelm", "blamish_red_shell_pointed", "8A1A1A"},
            {"bruise_blue_snelm", "Bruise blue snelm", "blamish_blue_shell_round", "3A4A8A"},
            {"pointed_bruise_blue_snelm", "Pointed bruise blue snelm", "blamish_blue_shell_pointed", "3A4A8A"},
            {"broken_bark_snelm", "Broken bark snelm", "blamish_bark_shell", "5A4A3A"}};

    @Override
    public void items(ItemSink s) {
        // --- Tools & moulds ---
        tool(s, "bracelet_mould", "Bracelet mould", "mould", 0x8A8A8A, 5);
        tool(s, "holy_mould", "Holy mould", "mould", 0x9A9AA0, 5);
        tool(s, "unholy_mould", "Unholy mould", "mould", 0x6A5A5A, 5);
        tool(s, "tiara_mould", "Tiara mould", "mould", 0xA0A0B0, 5);
        tool(s, "sickle_mould", "Sickle mould", "mould", 0x8A9A8A, 5);
        tool(s, "bolt_mould", "Bolt mould", "mould", 0x7A7A8A, 5);
        tool(s, "glassblowing_pipe", "Glassblowing pipe", "pipe", 0x8A8A9A, 2);

        // --- Leathers ---
        simple(s, "hard_leather", "Hard leather", "leather", 0x7A5028, 12);
        simple(s, "snake_hide", "Snake hide", "hide", 0x6A7A3A, 30);
        simple(s, "snakeskin", "Snakeskin", "leather", 0x7A8A4A, 45);
        for (Hide h : HIDES) {
            simple(s, h.id + "_dragon_leather", h.name + " dragon leather", "leather", h.color, 1900 + 400 * List.of(HIDES).indexOf(h));
        }
        // Snelms: a chisel on a Mort Myre snail shell (all share the steel-med-helm-like wiki bonuses, no requirements).
        for (String[] sn : SNELMS) {
            s.add(sn[0], sn[1], Kind.ARMOUR, "med_helm", Integer.parseInt(sn[3], 16)).value(300).stack(1)
                    .equipment(wear(Slot.HEAD, stats(0, 0, 0, -3, -1, 7, 8, 6, -1, 7, 0, 0, 0, 0)))
                    .examine("A snail shell helmet.");
        }
        // Leather extras (no hands slot yet: gloves and vambraces are carried, not worn)
        simple(s, "leather_gloves", "Leather gloves", "gloves", 0x9A6A3A, 6).stack(1).examine("These will keep my hands warm!");
        simple(s, "leather_vambraces", "Leather vambraces", "vambraces", 0x9A6A3A, 18).stack(1);
        s.add("coif", "Coif", Kind.ARMOUR, "cowl", 0x8A5A2A).value(200).stack(1).tier("leather")
                .equipment(wear(Slot.HEAD, stats(0, 0, 0, -1, 2, 4, 6, 8, 4, 4, 0, 0, 0, 0), Skill.RANGED, 20));
        s.add("hard_leather_body", "Hardleather body", Kind.ARMOUR, "leather_body", 0x7A5028).value(170).stack(1).tier("leather")
                .equipment(wear(Slot.CHEST, stats(0, 0, 0, -6, 8, 12, 15, 18, 6, 12, 0, 0, 0, 0), Skill.DEFENCE, 10));
        s.add("studded_body", "Studded body", Kind.ARMOUR, "leather_body", 0x8A6A4A).value(850).stack(1).tier("leather")
                .equipment(wear(Slot.CHEST, stats(0, 0, 0, -4, 8, 18, 25, 22, 8, 25, 0, 0, 0, 0), Skill.RANGED, 20, Skill.DEFENCE, 20));
        s.add("studded_chaps", "Studded chaps", Kind.ARMOUR, "chaps", 0x8A6A4A).value(750).stack(1).tier("leather")
                .equipment(wear(Slot.LEGS, stats(0, 0, 0, -5, 6, 15, 16, 17, 6, 16, 0, 0, 0, 0), Skill.RANGED, 20));
        // Snakeskin
        int snake = 0x7A8A4A;
        simple(s, "snakeskin_vambraces", "Snakeskin vambraces", "vambraces", snake, 1400).stack(1);
        s.add("snakeskin_boots", "Snakeskin boots", Kind.ARMOUR, "boots", snake).value(1200).stack(1).tier("snakeskin")
                .equipment(wear(Slot.FEET, stats(0, 0, 0, -10, 3, 1, 1, 2, 1, 1, 0, 0, 0, 0), Skill.RANGED, 30, Skill.DEFENCE, 30));
        s.add("snakeskin_bandana", "Snakeskin bandana", Kind.ARMOUR, "cowl", snake).value(1200).stack(1).tier("snakeskin")
                .equipment(wear(Slot.HEAD, stats(0, 0, 0, -5, 4, 2, 4, 4, 2, 4, 0, 0, 0, 0), Skill.RANGED, 30, Skill.DEFENCE, 30));
        s.add("snakeskin_chaps", "Snakeskin chaps", Kind.ARMOUR, "chaps", snake).value(2000).stack(1).tier("snakeskin")
                .equipment(wear(Slot.LEGS, stats(0, 0, 0, -5, 6, 3, 4, 5, 4, 5, 0, 0, 0, 0), Skill.RANGED, 30, Skill.DEFENCE, 30));
        s.add("snakeskin_body", "Snakeskin body", Kind.ARMOUR, "leather_body", snake).value(2400).stack(1).tier("snakeskin")
                .equipment(wear(Slot.CHEST, stats(0, 0, 0, -5, 12, 25, 25, 30, 25, 28, 0, 0, 0, 0), Skill.RANGED, 30, Skill.DEFENCE, 30));
        // Dragonhide
        for (Hide h : HIDES) {
            String t = h.id + "_dhide";
            simple(s, t + "_vambraces", h.name + " d'hide vambraces", "vambraces", h.color, 1200 + h.ranged * 30).stack(1)
                    .examine("Vambraces made from " + lower(h.name) + " dragonhide.");
            int b = h.bodyDef;
            s.add(t + "_body", h.name + " d'hide body", Kind.ARMOUR, "leather_body", h.color).value(7800 + h.ranged * 100)
                    .stack(1).tier(t).equipment(wear(Slot.CHEST,
                            stats(0, 0, 0, -15, h.rangedAtk, b, b - 8, b + 5, (b - 40) * 2 + 20, b + (h.id.equals("black") ? 2 : 0),
                                    0, 0, 0, 0), Skill.RANGED, h.ranged, Skill.DEFENCE, 40));
            int c = h.chapsDef;
            s.add(t + "_chaps", h.name + " d'hide chaps", Kind.ARMOUR, "chaps", h.color).value(3900 + h.ranged * 50)
                    .stack(1).tier(t).equipment(wear(Slot.LEGS,
                            stats(0, 0, 0, -10, 8 + (h.ranged - 40) / 10 * 3, c, c - 6, c + 2, c - 3, c, 0, 0, 0, 0),
                            Skill.RANGED, h.ranged));
            int d = h.shieldDef;
            s.add(t + "_shield", h.name + " d'hide shield", Kind.SHIELD, "round_shield", h.color).value(3900 + h.ranged * 50)
                    .stack(1).tier(t).equipment(wear(Slot.SHIELD,
                            stats(0, 0, 0, -10, 0, d, d + 2, d + 1, d - 6, d, 0, 0, 0, 0), Skill.RANGED, h.ranged, Skill.DEFENCE, 40));
        }

        // --- Battlestaves & orbs ---
        s.add("battlestaff", "Battlestaff", Kind.STAFF, "staff", 0x6A5A3A).value(7000).stack(1).tier("battlestaff")
                .equipment(weapon(stats(9, -1, 40, 10, 0, 2, 3, 1, 10, 0, 50, 0, 0, 0), 4, AttackType.CRUSH,
                        EquipmentDef.WeaponClass.STAFF, Skill.MAGIC, 30, Skill.ATTACK, 30));
        simple(s, "unpowered_orb", "Unpowered orb", "orb", 0xC8D0D8, 100);
        for (Orb o : ORBS) {
            int color = switch (o.element()) {
                case "water" -> 0x3060E0;
                case "earth" -> 0x8A5A2A;
                case "fire" -> 0xE04010;
                default -> 0xE0E0E8;
            };
            simple(s, o.orb(), title(o.element()) + " orb", "orb", color, 300);
            s.add(o.staff(), title(o.element()) + " battlestaff", Kind.STAFF, "staff_orb", color).value(15500).stack(1)
                    .tier("battlestaff").equipment(weapon(stats(10, -1, 40, 12, 0, 2, 3, 1, 12, 0, 50, 0, 0, 0), 4,
                            AttackType.CRUSH, EquipmentDef.WeaponClass.STAFF, Skill.MAGIC, 30, Skill.ATTACK, 30));
        }

        // Purging staff: a battlestaff empowered by a tormented synapse (osrs/content/CommonItems); wiki / DPS calculator
        // bonuses, 77 Magic and 50 Attack. Boosts demonbane spells.
        s.add("purging_staff", "Purging staff", Kind.STAFF, "staff_orb", 0xE05020).value(800_000).stack(1).tier("battlestaff")
                .equipment(weapon(stats(10, -5, 83, 37, 0, 1, 1, 0, 25, 0, 62, 0, 10, 0), 5, AttackType.CRUSH,
                        EquipmentDef.WeaponClass.STAFF, Skill.MAGIC, 77, Skill.ATTACK, 50))
                .examine("A battlestaff burning with a tormented synapse. It empowers demonbane spells.");

        // --- Pottery ---
        simple(s, "soft_clay", "Soft clay", "clay", 0xB09070, 2);
        String[][] pottery = {{"pot", "Pot"}, {"pie_dish", "Pie dish"}, {"bowl", "Bowl"}, {"plant_pot", "Plant pot"},
                {"pot_lid", "Pot lid"}};
        for (String[] p : pottery) {
            simple(s, "unfired_" + p[0], "Unfired " + lower(p[1]), p[0].equals("pot") ? "pot_item" : p[0], 0xB09070, 1);
            if (!p[0].equals("pot")) {   // fired pots are CookingData's "empty_pot"
                simple(s, p[0], p[1], p[0].equals("plant_pot") ? "pot_item" : p[0], 0xA06040, 3);
            }
        }
        simple(s, "filled_plant_pot", "Filled plant pot", "pot_item", 0x6A4A2A, 3);

        // --- Glass ---
        simple(s, "molten_glass", "Molten glass", "glass", 0xE08040, 5);
        simple(s, "beer_glass", "Beer glass", "glass", 0xD0E0E0, 1);
        simple(s, "empty_candle_lantern", "Empty candle lantern", "lantern", 0xD0D0C0, 10);
        simple(s, "empty_oil_lamp", "Empty oil lamp", "lamp", 0xD0D0C0, 20);
        simple(s, "empty_fishbowl", "Empty fishbowl", "glass", 0xC0E0F0, 10);
        simple(s, "lantern_lens", "Lantern lens", "glass", 0xE0F0F0, 10);
        simple(s, "empty_light_orb", "Light orb", "orb", 0xF0F0C0, 100);

        // --- Jewellery ---
        for (Jewel j : JEWELLERY) {
            String metalName = j.gem() == null ? (j.silver() ? "Silver" : "Gold") : (j.gem().equals("topaz") ? "Topaz" : title(j.gem()));
            int color = j.gem() == null ? 0xE8C030 : gemColor(j.gem());
            int value = (j.silver() ? 75 : 300) + gemValue(j.gem() == null ? "x" : j.gem()) * 3 / 2 + j.level() * 10;
            if (j.type().equals("amulet")) {
                simple(s, j.id() + "_u", metalName + " amulet (u)", "amulet", color, value).stack(1);
                simple(s, j.id(), metalName + " amulet", "amulet", color, value + 50).stack(1);
            } else {
                simple(s, j.id(), metalName + " " + j.type(), j.type(), color, value).stack(1);
            }
        }

        // --- Silver ---
        simple(s, "unstrung_symbol", "Unstrung symbol", "amulet", 0xD8D8E8, 120).stack(1);
        simple(s, "unblessed_symbol", "Unblessed symbol", "amulet", 0xD8D8E8, 200).stack(1);
        simple(s, "unstrung_emblem", "Unstrung emblem", "amulet", 0x8A8A9A, 120).stack(1);
        simple(s, "unpowered_symbol", "Unpowered symbol", "amulet", 0x8A8A9A, 200).stack(1);
        simple(s, "tiara", "Tiara", "tiara", 0xD8D8E8, 100).stack(1);
        // Gold tiara (42 Crafting): a cosmetic head piece with no bonuses (wiki / DPS calculator).
        s.add("gold_tiara", "Gold tiara", Kind.ARMOUR, "tiara", 0xE8C030).value(100).stack(1)
                .equipment(wear(Slot.HEAD, stats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)))
                .examine("Makes me feel like a wealthy Princess.");
        s.add("silver_sickle", "Silver sickle", Kind.MELEE_WEAPON, "sickle", 0xD8D8E8).value(175).stack(1).tier("iron")
                .equipment(weapon(stats(4, 6, -2, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0), 4, AttackType.SLASH, EquipmentDef.WeaponClass.MELEE));

        // --- Spinning ---
        simple(s, "crossbow_string", "Crossbow string", "string", 0xD0C0A0, 15);
        simple(s, "rope", "Rope", "rope", 0xB09060, 18);
        simple(s, "bolt_of_cloth", "Bolt of cloth", "cloth", 0xE0D8C8, 650);
        simple(s, "magic_string", "Magic string", "string", 0x7AB0D8, 1).examine("I could use this to make jewellery.");
        // --- Weaving (loom) ---
        simple(s, "strip_of_cloth", "Strip of cloth", "cloth", 0xE8E0D0, 10).examine("A strip of cloth.");
        simple(s, "basket", "Basket", "bucket", 0xB08A50, 1).examine("An empty fruit basket.");
        // Cut from a farmed willow tree with secateurs in OSRS; bought on the Grand Exchange here.
        simple(s, "willow_branch", "Willow branch", "log", 0x7A8A4A, 2).examine("A branch from a willow tree.");

        // --- Bird houses ---
        simple(s, "clockwork", "Clockwork", "clockwork", 0xB0B0B8, 100);
        for (BirdHouse b : BIRD_HOUSES) {
            simple(s, b.id(), b.name(), "birdhouse", b.color(), 20 + b.craftLevel() * 5).stack(16);
        }
    }

    @Override
    public void recipes(Consumer<Recipe> out) {
        MenuType L = MenuType.LEATHERWORKING;
        // --- Tanning ---
        out.accept(plain("tan_hard_leather", MenuType.TANNING, "Hard leather (3 coins)").in("cowhide", 1).in("coins", 3)
                .out("hard_leather", 1).ticks(0).build());
        out.accept(plain("tan_snakeskin", MenuType.TANNING, "Snakeskin (15 coins)").in("snake_hide", 1).in("coins", 15)
                .out("snakeskin", 1).ticks(0).build());
        for (Hide h : HIDES) {
            out.accept(plain("tan_" + h.id + "_dragon_leather", MenuType.TANNING, h.name + " dragon leather (20 coins)")
                    .in(h.id + "_dragonhide", 1).in("coins", 20).out(h.id + "_dragon_leather", 1).ticks(0).build());
        }
        // --- Sewing (needle + thread) ---
        sew(out, "leather_gloves", "Leather gloves", 1, 13.8, "leather", 1);
        sew(out, "leather_vambraces", "Leather vambraces", 11, 22.0, "leather", 1);
        sew(out, "hard_leather_body", "Hardleather body", 28, 35.0, "hard_leather", 1);
        sew(out, "coif", "Coif", 38, 37.0, "leather", 1);
        out.accept(r("craft_studded_body", L, "Studded body", Skill.CRAFTING, 41, 40.0).in("leather_body", 1).in("steel_studs", 1)
                .out("studded_body", 1).ticks(3).build());
        out.accept(r("craft_studded_chaps", L, "Studded chaps", Skill.CRAFTING, 44, 42.0).in("leather_chaps", 1).in("steel_studs", 1)
                .out("studded_chaps", 1).ticks(3).build());
        sew(out, "snakeskin_boots", "Snakeskin boots", 45, 30.0, "snakeskin", 6);
        sew(out, "snakeskin_vambraces", "Snakeskin vambraces", 47, 35.0, "snakeskin", 8);
        sew(out, "snakeskin_bandana", "Snakeskin bandana", 48, 45.0, "snakeskin", 5);
        sew(out, "snakeskin_chaps", "Snakeskin chaps", 51, 50.0, "snakeskin", 12);
        sew(out, "snakeskin_body", "Snakeskin body", 53, 55.0, "snakeskin", 15);
        for (Hide h : HIDES) {
            String t = h.id + "_dhide";
            String leather = h.id + "_dragon_leather";
            sew(out, t + "_vambraces", h.name + " d'hide vambraces", h.vamb, h.vambXp, leather, 1);
            sew(out, t + "_chaps", h.name + " d'hide chaps", h.chaps, h.vambXp * 2, leather, 2);
            sew(out, t + "_shield", h.name + " d'hide shield", h.shield, h.vambXp * 2, leather, 2);
            sew(out, t + "_body", h.name + " d'hide body", h.body, h.vambXp * 3, leather, 3);
        }
        // --- Battlestaves ---
        for (Orb o : ORBS) {
            out.accept(r("craft_" + o.staff(), L, title(o.element()) + " battlestaff", Skill.CRAFTING, o.craftLevel(), o.craftXp())
                    .in("battlestaff", 1).in(o.orb(), 1).out(o.staff(), 1).ticks(2).build());
        }
        // Purging staff (74 Crafting and 55 Smithing in OSRS, made at an anvil with a hammer).
        out.accept(plain("craft_purging_staff", MenuType.SMITHING, "Purging staff").skill(Skill.CRAFTING, 74, 0)
                .in("battlestaff", 1).in("tormented_synapse", 1).in("iron_bar", 1).out("purging_staff", 1).tool("hammer")
                .ticks(3).build()); // XP approx (none known)
        // --- Pottery ---
        out.accept(r("make_soft_clay", L, "Soft clay", Skill.CRAFTING, 1, 0).in("clay", 1).in("jug_of_water", 1)
                .out("soft_clay", 1).out("jug", 1).ticks(1).build());
        Object[][] pottery = {{"pot", "Pot", 1, 6.3, 6.3, "empty_pot"}, {"pie_dish", "Pie dish", 7, 15.0, 10.0, "pie_dish"},
                {"bowl", "Bowl", 8, 18.0, 15.0, "bowl"}, {"plant_pot", "Plant pot", 19, 20.0, 17.5, "plant_pot"},
                {"pot_lid", "Pot lid", 25, 20.0, 20.0, "pot_lid"}};
        for (Object[] p : pottery) {
            String id = (String) p[0];
            out.accept(r("shape_" + id, MenuType.SPINNING, "Unfired " + lower((String) p[1]), Skill.CRAFTING, (Integer) p[2], (Double) p[3])
                    .in("soft_clay", 1).out("unfired_" + id, 1).ticks(3).build());
            out.accept(r("fire_" + id, MenuType.SMELTING, (String) p[1], Skill.CRAFTING, (Integer) p[2], (Double) p[4])
                    .in("unfired_" + id, 1).out((String) p[5], 1).ticks(4).build());
        }
        out.accept(plain("fill_plant_pot", L, "Filled plant pot").in("plant_pot", 1).in("minecraft:dirt", 1)
                .out("filled_plant_pot", 1).ticks(1).build());
        // --- Glass ---
        out.accept(r("smelt_molten_glass", MenuType.SMELTING, "Molten glass", Skill.CRAFTING, 1, 20.0).in("minecraft:sand", 1)
                .in("soda_ash", 1).out("molten_glass", 1).ticks(3).build());
        Object[][] glass = {{"beer_glass", "Beer glass", 1, 17.5}, {"empty_candle_lantern", "Candle lantern", 4, 19.0},
                {"empty_oil_lamp", "Oil lamp", 12, 25.0}, {"vial", "Vial", 33, 35.0}, {"empty_fishbowl", "Fishbowl", 42, 42.5},
                {"unpowered_orb", "Unpowered orb", 46, 52.5}, {"lantern_lens", "Lantern lens", 49, 55.0},
                {"empty_light_orb", "Light orb", 87, 70.0}};
        for (Object[] g : glass) {
            out.accept(r("blow_" + g[0], L, (String) g[1], Skill.CRAFTING, (Integer) g[2], (Double) g[3]).in("molten_glass", 1)
                    .out((String) g[0], 1).tool("glassblowing_pipe").ticks(3).build());
        }
        // --- Jewellery (furnace) ---
        for (Jewel j : JEWELLERY) {
            String mould = j.type() + "_mould";
            String product = j.type().equals("amulet") ? j.id() + "_u" : j.id();
            String name = j.type().equals("amulet") ? title(j.gem() == null ? "gold" : j.gem()) + " amulet (u)" : title(j.id());
            Recipe.Builder b = r("craft_" + product, MenuType.SMELTING, name, Skill.CRAFTING, j.level(), j.xp())
                    .in(j.silver() ? "silver_bar" : "gold_bar", 1).out(product, 1).tool(mould).ticks(3);
            if (j.gem() != null) {
                b.in(gemItem(j.gem()), 1);
            }
            out.accept(b.build());
            if (j.type().equals("amulet")) {
                out.accept(r("string_" + j.id(), L, title(j.gem() == null ? "gold" : j.gem()) + " amulet", Skill.CRAFTING, 1, 4.0)
                        .in(product, 1).in("ball_of_wool", 1).out(j.id(), 1).ticks(2).build());
            }
        }
        // --- Silver ---
        silver(out, "unstrung_symbol", "Unstrung symbol", 16, 50.0, "holy_mould", 1);
        silver(out, "unstrung_emblem", "Unstrung emblem", 17, 50.0, "unholy_mould", 1);
        silver(out, "silver_sickle", "Silver sickle", 18, 50.0, "sickle_mould", 1);
        silver(out, "silver_bolts_unf", "Silver bolts (unf) x10", 21, 50.0, "bolt_mould", 10);
        silver(out, "tiara", "Tiara", 23, 52.5, "tiara_mould", 1);
        out.accept(r("string_unblessed_symbol", L, "Unblessed symbol", Skill.CRAFTING, 16, 4.0).in("unstrung_symbol", 1)
                .in("ball_of_wool", 1).out("unblessed_symbol", 1).ticks(2).build());
        out.accept(r("string_unpowered_symbol", L, "Unpowered symbol", Skill.CRAFTING, 17, 4.0).in("unstrung_emblem", 1)
                .in("ball_of_wool", 1).out("unpowered_symbol", 1).ticks(2).build());
        // --- Spinning ---
        out.accept(r("spin_crossbow_string", MenuType.SPINNING, "Crossbow string", Skill.CRAFTING, 10, 15.0).in("sinew", 1)
                .out("crossbow_string", 1).ticks(3).build());
        for (String root : List.of("oak", "willow", "maple", "yew", "magic")) {
            out.accept(r("spin_crossbow_string_" + root, MenuType.SPINNING, "Crossbow string (" + root + " roots)", Skill.CRAFTING,
                    10, 15.0).in(root + "_roots", 1).out("crossbow_string", 1).ticks(3).build());
        }
        out.accept(r("spin_magic_string", MenuType.SPINNING, "Magic string", Skill.CRAFTING, 19, 30.0).in("magic_roots", 1)
                .out("magic_string", 1).ticks(3).build());
        // --- Weaving (loom, OSRS Wiki) ---
        out.accept(r("weave_strip_of_cloth", MenuType.SPINNING, "Strip of cloth", Skill.CRAFTING, 10, 12.0).in("ball_of_wool", 4)
                .out("strip_of_cloth", 1).ticks(3).build());
        out.accept(r("weave_empty_sack", MenuType.SPINNING, "Empty sack", Skill.CRAFTING, 21, 38.0).in("jute_fibre", 4)
                .out("empty_sack", 1).ticks(3).build());
        out.accept(r("weave_basket", MenuType.SPINNING, "Basket", Skill.CRAFTING, 36, 56.0).in("willow_branch", 6)
                .out("basket", 1).ticks(4).build());
        out.accept(r("craft_gold_tiara", MenuType.SMELTING, "Gold tiara", Skill.CRAFTING, 42, 35.0).in("gold_bar", 1)
                .out("gold_tiara", 1).tool("tiara_mould").ticks(3).build());
        // --- Snelms (chisel on a snail shell: 15 Crafting, 32.5 XP) ---
        for (String[] sn : SNELMS) {
            out.accept(r("craft_" + sn[0], MenuType.GEM_CUTTING, sn[1], Skill.CRAFTING, 15, 32.5).in(sn[2], 1).out(sn[0], 1)
                    .tool("chisel").ticks(2).build());
        }
        // --- Bird houses (hammer + chisel) ---
        out.accept(r("craft_clockwork", L, "Clockwork", Skill.CRAFTING, 8, 15.0).in("steel_bar", 1).out("clockwork", 1)
                .tool("hammer").ticks(3).build());
        for (BirdHouse b : BIRD_HOUSES) {
            out.accept(r("craft_" + b.id(), L, b.name(), Skill.CRAFTING, b.craftLevel(), b.craftXp()).in(b.log(), 1)
                    .in("clockwork", 1).out(b.id(), 1).tool("chisel").ticks(3).build());
        }
    }

    private static void sew(Consumer<Recipe> out, String item, String name, int level, double xp, String leather, int amount) {
        out.accept(r("craft_" + item, MenuType.LEATHERWORKING, name, Skill.CRAFTING, level, xp).in(leather, amount)
                .in("minecraft:string", 1).out(item, 1).tool("needle").ticks(3).build());
    }

    private static void silver(Consumer<Recipe> out, String item, String name, int level, double xp, String mould, int amount) {
        out.accept(r("craft_" + item, MenuType.SMELTING, name, Skill.CRAFTING, level, xp).in("silver_bar", 1).out(item, amount)
                .tool(mould).ticks(3).build());
    }

    @Override
    public List<BlockDef> blocks() {
        List<BlockDef> list = new ArrayList<>();
        list.add(BlockDef.menu("potters_wheel", "Potter's wheel", MenuType.SPINNING, 0x8A6A4A, "wheel"));
        list.add(BlockDef.menu("pottery_oven", "Pottery oven", MenuType.SMELTING, 0x9A5A3A, "furnace"));
        list.add(BlockDef.menu("crafting_table", "Crafting table", MenuType.LEATHERWORKING, 0x8A6A3A, "table"));
        for (Orb o : ORBS) {
            list.add(BlockDef.scripted(o.obelisk(), "Obelisk of " + o.element(), 0x5A5A6A, "obelisk"));
        }
        return list;
    }

    @Override
    public List<PlaceDef> places() {
        return List.of(
                PlaceDef.of("crafting_guild", "Crafting Guild", 2933, 3286, PlaceDef.Style.TOWN,
                        List.of("potters_wheel", "pottery_oven", "crafting_table", "dairy_churn")),
                PlaceDef.of("barbarian_village_pottery", "Barbarian Village pottery", 3086, 3409, PlaceDef.Style.CAMP,
                        List.of("potters_wheel", "pottery_oven")),
                PlaceDef.of("obelisk_of_water_site", "Obelisk of Water", 2844, 3424, PlaceDef.Style.RUINS, List.of("obelisk_of_water")),
                PlaceDef.of("obelisk_of_earth_site", "Obelisk of Earth", 3088, 3477, PlaceDef.Style.RUINS, List.of("obelisk_of_earth")),
                PlaceDef.of("obelisk_of_fire_site", "Obelisk of Fire", 2817, 3171, PlaceDef.Style.RUINS, List.of("obelisk_of_fire")),
                PlaceDef.of("obelisk_of_air_site", "Obelisk of Air", 3088, 3569, PlaceDef.Style.RUINS, List.of("obelisk_of_air")));
    }
}
