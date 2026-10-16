package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.data.MetalTier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Fletching: crossbow stocks, unstrung/strung crossbows (bronze to dragon), bolts (metal, gem-tipped,
 * broad, amethyst broad, kebbit), darts, javelins (bronze to dragon), throwing knives (smithed),
 * broad/amethyst/dragon arrows, ogre arrows, wooden shields and celastrus battlestaves. Levels/XP/ranged
 * strength are OSRS values.
 *
 * <p>Darts, knives and javelins are {@code Kind.ARROW} items: they can be thrown by right-clicking
 * (see SkillingHooks) and also count as ammunition.
 */
public final class FletchingData extends SkillingPart {

    /** Crossbows: id, display, stock, limbs, fletching level, attach XP, string XP, ranged attack, ranged req. */
    public record Crossbow(String id, String name, String stock, String limbs, int level, double attachXp, double stringXp,
                           int rangedAttack, int rangedLevel, int color) {
    }

    public static final List<Crossbow> CROSSBOWS = List.of(
            new Crossbow("bronze_crossbow", "Bronze crossbow", "wooden_stock", "bronze_limbs", 9, 12.0, 6.0, 18, 1, 0xB08D57),
            new Crossbow("blurite_crossbow", "Blurite crossbow", "oak_stock", "blurite_limbs", 24, 32.0, 16.0, 30, 16, 0x5A7AD0),
            new Crossbow("iron_crossbow", "Iron crossbow", "willow_stock", "iron_limbs", 39, 44.0, 22.0, 42, 26, 0x8A8A8E),
            new Crossbow("steel_crossbow", "Steel crossbow", "teak_stock", "steel_limbs", 46, 54.0, 27.0, 54, 31, 0xB8B8C0),
            new Crossbow("mithril_crossbow", "Mith crossbow", "maple_stock", "mithril_limbs", 54, 64.0, 32.0, 66, 36, 0x4A5A9C),
            new Crossbow("adamant_crossbow", "Adamant crossbow", "mahogany_stock", "adamant_limbs", 61, 82.0, 41.0, 78, 46, 0x4C7A4C),
            new Crossbow("rune_crossbow", "Rune crossbow", "yew_stock", "rune_limbs", 69, 100.0, 50.0, 90, 61, 0x4FB8C8),
            new Crossbow("dragon_crossbow", "Dragon crossbow", "magic_stock", "dragon_limbs", 78, 135.0, 70.0, 94, 64, 0xB02020));

    private record Stock(String id, String log, int level, double xp, int color) {
    }

    private static final Stock[] STOCKS = {
            new Stock("wooden_stock", "logs", 9, 6.0, 0x6B4A2A), new Stock("oak_stock", "oak_logs", 24, 16.0, 0x8A6A3A),
            new Stock("willow_stock", "willow_logs", 39, 22.0, 0x6E7A3A), new Stock("teak_stock", "teak_logs", 46, 27.0, 0x9C7A4A),
            new Stock("maple_stock", "maple_logs", 54, 32.0, 0xA0502A), new Stock("mahogany_stock", "mahogany_logs", 61, 41.0, 0x6A2A1A),
            new Stock("yew_stock", "yew_logs", 69, 50.0, 0x5A4A2A), new Stock("magic_stock", "magic_logs", 78, 70.0, 0x3A8AA8)};

    /** Metal bolts: tier prefix, fletching level, XP per bolt, ranged strength. */
    private record Bolt(String prefix, String name, int level, double xpEach, int strength, int color) {
    }

    private static final Bolt[] BOLTS = {
            new Bolt("bronze", "Bronze", 9, 0.5, 10, 0xB08D57), new Bolt("blurite", "Blurite", 24, 1.0, 28, 0x5A7AD0),
            new Bolt("iron", "Iron", 39, 1.5, 46, 0x8A8A8E), new Bolt("silver", "Silver", 43, 2.5, 36, 0xD8D8E8),
            new Bolt("steel", "Steel", 46, 3.5, 64, 0xB8B8C0), new Bolt("mithril", "Mithril", 54, 5.0, 82, 0x4A5A9C),
            new Bolt("adamant", "Adamant", 61, 7.0, 100, 0x4C7A4C), new Bolt("rune", "Runite", 69, 10.0, 115, 0x4FB8C8)};

    /** Gem-tipped bolts: gem item, tip cutting level/XP (per gem, 12 tips), base bolts, attach level/XP each, strength. */
    public record GemBolt(String gem, String name, int cutLevel, double cutXp, String baseBolts, int level, double xpEach,
                          int strength, int color) {
        /** Bolt family prefix: "topaz" for red topaz, "pearl" for oyster pearls, else the gem id. */
        public String prefix() {
            return gem.equals("red_topaz") ? "topaz" : gem.equals("oyster_pearl") ? "pearl" : gem;
        }

        public String tips() {
            return prefix() + "_bolt_tips";
        }

        public String bolts() {
            return prefix() + "_bolts";
        }

        /** Tips cut from one gem: 12, or 6 from an oyster pearl. */
        public int tipsPerGem() {
            return gem.equals("oyster_pearl") ? 6 : 12;
        }

        /** Enchanted bolts (Enchant Crossbow Bolt). */
        public String enchanted() {
            return bolts() + "_e";
        }

        /** The same tip on dragon bolts (84 Fletching, 122 ranged strength). */
        public String dragonBolts() {
            return prefix() + "_dragon_bolts";
        }

        public String dragonEnchanted() {
            return dragonBolts() + "_e";
        }
    }

    /** Dragon bolts: fletching level, XP per bolt and ranged strength (shared by every gem-tipped dragon bolt). */
    public static final int DRAGON_BOLT_LEVEL = 84;
    public static final double DRAGON_BOLT_XP = 12.0;
    public static final int DRAGON_BOLT_STRENGTH = 122;

    public static final List<GemBolt> GEM_BOLTS = List.of(
            new GemBolt("opal", "Opal", 11, 1.5, "bronze_bolts", 11, 1.6, 14, 0xE0E0F0),
            new GemBolt("jade", "Jade", 26, 2.4, "blurite_bolts", 26, 2.4, 30, 0x50C060),
            new GemBolt("oyster_pearl", "Pearl", 41, 2.0, "iron_bolts", 41, 3.2, 48, 0xF0F0F0), // tip cutting XP approx
            new GemBolt("red_topaz", "Topaz", 48, 3.9, "steel_bolts", 48, 3.9, 66, 0xE06050),
            new GemBolt("sapphire", "Sapphire", 56, 4.0, "mithril_bolts", 56, 4.7, 83, 0x3050E0),
            new GemBolt("emerald", "Emerald", 58, 5.5, "mithril_bolts", 58, 5.5, 85, 0x30C050),
            new GemBolt("ruby", "Ruby", 63, 6.3, "adamant_bolts", 63, 6.3, 103, 0xE02030),
            new GemBolt("diamond", "Diamond", 65, 7.0, "adamant_bolts", 65, 7.0, 105, 0xF0F0FF),
            new GemBolt("dragonstone", "Dragonstone", 71, 8.2, "rune_bolts", 71, 8.2, 117, 0xB040E0),
            new GemBolt("onyx", "Onyx", 73, 9.4, "rune_bolts", 73, 9.4, 120, 0x303030));

    /** Darts: tier, fletching level, XP each, ranged strength. */
    private record Dart(String tier, String name, int level, double xpEach, int strength, int color) {
    }

    private static final Dart[] DARTS = {
            new Dart("bronze", "Bronze", 10, 1.8, 1, 0xB08D57), new Dart("iron", "Iron", 22, 3.8, 3, 0x8A8A8E),
            new Dart("steel", "Steel", 37, 7.5, 4, 0xB8B8C0), new Dart("mithril", "Mithril", 52, 11.2, 7, 0x4A5A9C),
            new Dart("adamant", "Adamant", 67, 15.0, 10, 0x4C7A4C), new Dart("rune", "Rune", 81, 18.8, 14, 0x4FB8C8),
            new Dart("amethyst", "Amethyst", 90, 21.0, 28, 0xA070D0),
            new Dart("dragon", "Dragon", 95, 25.0, 35, 0xB02020)}; // dragon_dart_tip: Chambers of Xeric rewards

    private static final Dart[] JAVELINS = {
            new Dart("bronze", "Bronze", 3, 1.0, 6, 0xB08D57), new Dart("iron", "Iron", 17, 2.0, 10, 0x8A8A8E),
            new Dart("steel", "Steel", 32, 5.0, 12, 0xB8B8C0), new Dart("mithril", "Mithril", 47, 8.0, 18, 0x4A5A9C),
            new Dart("adamant", "Adamant", 62, 10.0, 28, 0x4C7A4C), new Dart("rune", "Rune", 77, 12.4, 42, 0x4FB8C8),
            new Dart("amethyst", "Amethyst", 84, 13.5, 55, 0xA070D0),
            new Dart("dragon", "Dragon", 92, 15.0, 150, 0xB02020)}; // dragon_javelin_heads: Vorkath / Grand Exchange

    /** Wooden shields: log, level, XP (2 logs each). */
    private record Shield(String id, String name, String log, int level, double xp, int def, int color) {
    }

    private static final Shield[] SHIELDS = {
            new Shield("oak_shield", "Oak shield", "oak_logs", 27, 50.0, 8, 0x8A6A3A),
            new Shield("willow_shield", "Willow shield", "willow_logs", 42, 83.0, 12, 0x6E7A3A),
            new Shield("maple_shield", "Maple shield", "maple_logs", 57, 116.5, 16, 0xA0502A),
            new Shield("yew_shield", "Yew shield", "yew_logs", 72, 150.0, 21, 0x5A4A2A),
            new Shield("magic_shield", "Magic shield", "magic_logs", 87, 183.0, 26, 0x3A8AA8),
            new Shield("redwood_shield", "Redwood shield", "redwood_logs", 92, 216.0, 31, 0xA03A2A)};

    /** Ids of throwable items (thrown on right-click) and their attack interval in game ticks. */
    public static final List<String> THROWN = new ArrayList<>();

    @Override
    public void items(ItemSink s) {
        for (Stock st : STOCKS) {
            simple(s, st.id, title(st.id), "stock", st.color, st.level * 2);
        }
        // dragon_limbs is defined by the slayer module (dropped by metal dragons).
        for (Crossbow c : CROSSBOWS) {
            simple(s, c.id() + "_u", c.name() + " (u)", "crossbow_u", c.color(), c.rangedAttack() * 20).stack(1);
            s.add(c.id(), c.name(), Kind.BOW, "crossbow", c.color()).value(c.rangedAttack() * 40).stack(1)
                    .tier(c.id().replace("_crossbow", ""))
                    .equipment(weapon(stats(0, 0, 0, 0, c.rangedAttack(), 0, 0, 0, 0, 0, 0, 0, 0, 0), 6, AttackType.RANGED,
                            EquipmentDef.WeaponClass.BOW, Skill.RANGED, c.rangedLevel()));
        }
        for (Bolt b : BOLTS) {
            if (!b.prefix.equals("blurite")) {  // blurite (unf) bolts live in SmithingData
                if (b.prefix.equals("silver")) {
                    simple(s, "silver_bolts_unf", "Silver bolts (unf)", "bolts", b.color, 3);
                }
            }
            s.add(b.prefix + "_bolts", b.name + " bolts", Kind.ARROW, "bolts", b.color).value(Math.max(1, b.strength / 4))
                    .rangedStrength(b.strength).tier(b.prefix);
        }
        for (GemBolt g : GEM_BOLTS) {
            simple(s, g.tips(), g.name() + " bolt tips", "bolt_tips", g.color(), g.strength() / 3 + 1);
            s.add(g.bolts(), g.name() + " bolts", Kind.ARROW, "bolts", g.color()).value(g.strength() / 2 + 1)
                    .rangedStrength(g.strength()).tier(g.gem());
            s.add(g.enchanted(), g.name() + " bolts (e)", Kind.ARROW, "bolts", g.color()).value(g.strength() + 2)
                    .rangedStrength(g.strength()).tier(g.gem()).examine("Enchanted " + lower(g.name()) + " tipped crossbow bolts.");
            s.add(g.dragonBolts(), g.name() + " dragon bolts", Kind.ARROW, "bolts", g.color()).value(DRAGON_BOLT_STRENGTH)
                    .rangedStrength(DRAGON_BOLT_STRENGTH).tier("dragon").examine(g.name() + " tipped dragon crossbow bolts.");
            s.add(g.dragonEnchanted(), g.name() + " dragon bolts (e)", Kind.ARROW, "bolts", g.color()).value(DRAGON_BOLT_STRENGTH + 10)
                    .rangedStrength(DRAGON_BOLT_STRENGTH).tier("dragon")
                    .examine("Enchanted " + lower(g.name()) + " tipped dragon crossbow bolts.");
        }
        simple(s, "dragon_bolts_unf", "Dragon bolts (unf)", "bolts", 0xB02020, 60);
        s.add("dragon_bolts", "Dragon bolts", Kind.ARROW, "bolts", 0xB02020).value(DRAGON_BOLT_STRENGTH)
                .rangedStrength(DRAGON_BOLT_STRENGTH).tier("dragon");
        simple(s, "unfinished_broad_bolts", "Unfinished broad bolts", "bolts", 0x8A8A70, 5);
        s.add("broad_bolts", "Broad bolts", Kind.ARROW, "bolts", 0x8A8A70).value(10).rangedStrength(100).tier("broad");
        // broad_arrowheads / broad_arrows are defined by the slayer module.
        simple(s, "amethyst_arrowtips", "Amethyst arrowtips", "arrowtips", 0xA070D0, 40);
        s.add("amethyst_arrow", "Amethyst arrow", Kind.ARROW, "arrow", 0xA070D0).value(80).rangedStrength(55).tier("amethyst");
        simple(s, "amethyst_bolt_tips", "Amethyst bolt tips", "bolt_tips", 0xA070D0, 30);
        simple(s, "amethyst_javelin_heads", "Amethyst javelin heads", "javelin_heads", 0xA070D0, 80);
        simple(s, "amethyst_dart_tip", "Amethyst dart tip", "dart_tip", 0xA070D0, 50);
        simple(s, "dragon_arrowtips", "Dragon arrowtips", "arrowtips", 0xB02020, 400);
        simple(s, "dragon_javelin_heads", "Dragon javelin heads", "javelin_heads", 0xB02020, 1000)
                .examine("Needs a shaft.");
        s.add("amethyst_broad_bolts", "Amethyst broad bolts", Kind.ARROW, "bolts", 0xA070D0).value(240).rangedStrength(115)
                .tier("broad").examine("Crossbow bolts with amethyst broad tips.");
        // Kebbit bolts: chiselled from the spikes of spined and razor-backed kebbits (Hunter).
        s.add("kebbit_bolts", "Kebbit bolts", Kind.ARROW, "bolts", 0xE0D8C0).value(18).rangedStrength(28).tier("kebbit")
                .examine("Bolts made from the spikes of a prickly kebbit.");
        s.add("long_kebbit_bolts", "Long kebbit bolts", Kind.ARROW, "bolts", 0xE8E0C8).value(31).rangedStrength(38).tier("kebbit")
                .examine("Bolts made from the spikes of a razor-backed kebbit.");
        // Ogre arrows (Big Chompy Bird Hunting): achey shafts, four feathers each, wolfbone tips. The finished
        // ogre_arrow is the quests module's item.
        simple(s, "ogre_arrow_shaft", "Ogre arrow shaft", "shaft", 0x8A5A3A, 1).examine("A wooden arrow shaft.");
        simple(s, "flighted_ogre_arrow", "Flighted ogre arrow", "headless", 0x8A5A3A, 1)
                .examine("A wooden arrow shaft with four flights attached.");
        simple(s, "wolfbone_arrowtips", "Wolfbone arrowtips", "arrowtips", 0xE0D8C0, 3).examine("I can make an ogre arrow with these.");
        // wolf_bones is a built-in Bone (osrs/data/Bone) since round 27.
        s.add("dragon_arrow", "Dragon arrow", Kind.ARROW, "arrow", 0xB02020).value(800).rangedStrength(60).tier("dragon");
        for (Dart d : DARTS) {
            s.add(d.tier + "_dart", d.name + " dart", Kind.ARROW, "dart", d.color).value(Math.max(1, d.strength * 3))
                    .rangedStrength(d.strength).tier(d.tier);
        }
        simple(s, "javelin_shaft", "Javelin shaft", "shaft", 0xA08050, 1);
        for (Dart j : JAVELINS) {
            s.add(j.tier + "_javelin", j.name + " javelin", Kind.ARROW, "javelin", j.color).value(Math.max(2, j.strength * 4))
                    .rangedStrength(j.strength).tier(j.tier);
        }
        for (Shield sh : SHIELDS) {
            s.add(sh.id, sh.name, Kind.SHIELD, "round_shield", sh.color).value(sh.level * 10).stack(1).tier("wood")
                    .equipment(wear(EquipmentDef.Slot.SHIELD, stats(0, 0, 0, 0, 0, sh.def, sh.def + 1, sh.def - 1, sh.def / 2,
                            sh.def + 2, 0, 0, 0, 0), Skill.DEFENCE, Math.max(1, sh.level - 27)));
        }
        simple(s, "celastrus_bark", "Celastrus bark", "log", 0xA07A5A, 100);
    }

    static {
        for (Dart d : DARTS) {
            THROWN.add(d.tier + "_dart");
        }
        for (Dart j : JAVELINS) {
            THROWN.add(j.tier + "_javelin");
        }
        for (MetalTier t : SmithingData.SMITHABLE) {
            THROWN.add(t.id() + "_knife");
        }
        // Dropped by drakes, wyrms and hydras (slayer module).
        THROWN.add("dragon_knife");
        THROWN.add("dragon_thrownaxe");
        // Thrown weapons from slayer monsters, the Dagannoth Kings and the TzHaar (osrs/content/CommonEquipment).
        THROWN.add("steel_thrownaxe");
        THROWN.add("mithril_thrownaxe");
        THROWN.add("rune_thrownaxe");
        THROWN.add("black_knife");
        THROWN.add("toktz_xil_ul");
    }

    @Override
    public void recipes(Consumer<Recipe> out) {
        MenuType F = MenuType.FLETCHING;
        for (Stock st : STOCKS) {
            out.accept(r("fletch_" + st.id, F, title(st.id), Skill.FLETCHING, st.level, st.xp).in(st.log, 1).out(st.id, 1)
                    .tool("knife").ticks(3).build());
        }
        for (Crossbow c : CROSSBOWS) {
            out.accept(r("fletch_" + c.id() + "_u", F, c.name() + " (u)", Skill.FLETCHING, c.level(), c.attachXp())
                    .in(c.stock(), 1).in(c.limbs(), 1).out(c.id() + "_u", 1).tool("hammer").ticks(3).build());
            out.accept(r("string_" + c.id(), F, c.name(), Skill.FLETCHING, c.level(), c.stringXp())
                    .in(c.id() + "_u", 1).in("crossbow_string", 1).out(c.id(), 1).ticks(2).build());
        }
        for (Bolt b : BOLTS) {
            out.accept(r("fletch_" + b.prefix + "_bolts", F, "10 " + lower(b.name) + " bolts", Skill.FLETCHING, b.level, b.xpEach * 10)
                    .in(b.prefix + "_bolts_unf", 10).in("minecraft:feather", 10).out(b.prefix + "_bolts", 10).ticks(2).build());
        }
        for (GemBolt g : GEM_BOLTS) {
            out.accept(r("cut_" + g.tips(), MenuType.GEM_CUTTING, g.tipsPerGem() + " " + lower(g.name()) + " bolt tips", Skill.FLETCHING,
                    g.cutLevel(), g.cutXp()).in(g.gem(), 1).out(g.tips(), g.tipsPerGem()).tool("chisel").ticks(2).build());
            out.accept(r("fletch_" + g.bolts(), F, "10 " + lower(g.name()) + " bolts", Skill.FLETCHING, g.level(), g.xpEach() * 10)
                    .in(g.baseBolts(), 10).in(g.tips(), 10).out(g.bolts(), 10).ticks(2).build());
            out.accept(r("fletch_" + g.dragonBolts(), F, "10 " + lower(g.name()) + " dragon bolts", Skill.FLETCHING, DRAGON_BOLT_LEVEL,
                    g.xpEach() * 10).in("dragon_bolts", 10).in(g.tips(), 10).out(g.dragonBolts(), 10).ticks(2).build());
        }
        out.accept(r("fletch_dragon_bolts", F, "10 dragon bolts", Skill.FLETCHING, DRAGON_BOLT_LEVEL, DRAGON_BOLT_XP * 10)
                .in("dragon_bolts_unf", 10).in("minecraft:feather", 10).out("dragon_bolts", 10).ticks(2).build());
        out.accept(r("fletch_broad_bolts", F, "10 broad bolts", Skill.FLETCHING, 55, 30.0).in("unfinished_broad_bolts", 10)
                .in("minecraft:feather", 10).out("broad_bolts", 10).ticks(2).build());
        // Slayer reward "Broader fletching" unlocks these in OSRS (not checked here).
        out.accept(r("fletch_amethyst_broad_bolts", F, "10 amethyst broad bolts", Skill.FLETCHING, 76, 106.0).in("broad_bolts", 10)
                .in("amethyst_bolt_tips", 10).out("amethyst_broad_bolts", 10).ticks(2).build());
        // OSRS Wiki: 12 bolts per spike for 5.8 / 7.9 XP (RuneLite's calculator lists 1 / 1.3 per bolt).
        out.accept(r("fletch_kebbit_bolts", MenuType.GEM_CUTTING, "12 kebbit bolts", Skill.FLETCHING, 32, 5.8).in("kebbit_spike", 1)
                .out("kebbit_bolts", 12).tool("chisel").ticks(3).build());
        out.accept(r("fletch_long_kebbit_bolts", MenuType.GEM_CUTTING, "12 long kebbit bolts", Skill.FLETCHING, 42, 7.9).in("long_kebbit_spike", 1)
                .out("long_kebbit_bolts", 12).tool("chisel").ticks(2).build());
        // Ogre arrows (OSRS Wiki; Big Chompy Bird Hunting must be started in-game, not checked here).
        out.accept(r("fletch_ogre_arrow_shaft", F, "4 ogre arrow shafts", Skill.FLETCHING, 5, 6.4).in("achey_tree_logs", 1)
                .out("ogre_arrow_shaft", 4).tool("knife").ticks(3).build());
        out.accept(r("fletch_flighted_ogre_arrow", F, "6 flighted ogre arrows", Skill.FLETCHING, 5, 5.4).in("ogre_arrow_shaft", 6)
                .in("minecraft:feather", 24).out("flighted_ogre_arrow", 6).ticks(2).build());
        out.accept(r("fletch_wolfbone_arrowtips", MenuType.GEM_CUTTING, "4 wolfbone arrowtips", Skill.FLETCHING, 5, 10.0)
                .in("wolf_bones", 1).out("wolfbone_arrowtips", 4).tool("chisel").ticks(2).build());
        out.accept(r("fletch_ogre_arrow", F, "6 ogre arrows", Skill.FLETCHING, 5, 6.0).in("flighted_ogre_arrow", 6)
                .in("wolfbone_arrowtips", 6).out("ogre_arrow", 6).ticks(2).build());
        out.accept(r("fletch_broad_arrows", F, "15 broad arrows", Skill.FLETCHING, 52, 150.0).in("headless_arrow", 15)
                .in("broad_arrowheads", 15).out("broad_arrows", 15).ticks(2).build());
        // Amethyst (chiselled with Crafting; fletched with Fletching)
        out.accept(r("cut_amethyst_bolt_tips", MenuType.GEM_CUTTING, "15 amethyst bolt tips", Skill.CRAFTING, 83, 60.0)
                .in("amethyst", 1).out("amethyst_bolt_tips", 15).tool("chisel").ticks(3).build());
        out.accept(r("cut_amethyst_arrowtips", MenuType.GEM_CUTTING, "15 amethyst arrowtips", Skill.CRAFTING, 85, 60.0)
                .in("amethyst", 1).out("amethyst_arrowtips", 15).tool("chisel").ticks(3).build());
        out.accept(r("cut_amethyst_javelin_heads", MenuType.GEM_CUTTING, "5 amethyst javelin heads", Skill.CRAFTING, 87, 60.0)
                .in("amethyst", 1).out("amethyst_javelin_heads", 5).tool("chisel").ticks(3).build());
        out.accept(r("cut_amethyst_dart_tips", MenuType.GEM_CUTTING, "8 amethyst dart tips", Skill.CRAFTING, 89, 60.0)
                .in("amethyst", 1).out("amethyst_dart_tip", 8).tool("chisel").ticks(3).build());
        out.accept(r("fletch_amethyst_arrow", F, "15 amethyst arrows", Skill.FLETCHING, 82, 202.5).in("headless_arrow", 15)
                .in("amethyst_arrowtips", 15).out("amethyst_arrow", 15).ticks(2).build());
        out.accept(r("fletch_dragon_arrow", F, "15 dragon arrows", Skill.FLETCHING, 90, 225.0).in("headless_arrow", 15)
                .in("dragon_arrowtips", 15).out("dragon_arrow", 15).ticks(2).build());
        for (Dart d : DARTS) {
            out.accept(r("fletch_" + d.tier + "_dart", F, "10 " + lower(d.name) + " darts", Skill.FLETCHING, d.level, d.xpEach * 10)
                    .in(d.tier + "_dart_tip", 10).in("minecraft:feather", 10).out(d.tier + "_dart", 10).ticks(2).build());
        }
        // Arrow shafts from better logs (normal logs are built in): 15 more shafts per tier, 5 more XP.
        Object[][] shafts = {{"oak_logs", 15, 30, 10.0}, {"willow_logs", 30, 45, 15.0}, {"maple_logs", 45, 60, 20.0},
                {"yew_logs", 60, 75, 25.0}, {"magic_logs", 75, 90, 30.0}, {"redwood_logs", 90, 105, 35.0}};
        for (Object[] sh : shafts) {
            String log = (String) sh[0];
            out.accept(r("fletch_arrow_shaft_" + log.replace("_logs", ""), F, sh[2] + " arrow shafts (" + lower(title(log)) + ")",
                    Skill.FLETCHING, (Integer) sh[1], (Double) sh[3]).in(log, 1).out("arrow_shaft", (Integer) sh[2]).tool("knife")
                    .ticks(3).build());
        }
        out.accept(r("fletch_javelin_shaft", F, "15 javelin shafts", Skill.FLETCHING, 3, 5.0).in("logs", 1)
                .out("javelin_shaft", 15).tool("knife").ticks(3).build());
        for (Dart j : JAVELINS) {
            out.accept(r("fletch_" + j.tier + "_javelin", F, "5 " + lower(j.name) + " javelins", Skill.FLETCHING, j.level, j.xpEach * 5)
                    .in(j.tier + "_javelin_heads", 5).in("javelin_shaft", 5).out(j.tier + "_javelin", 5).ticks(2).build());
        }
        for (Shield sh : SHIELDS) {
            out.accept(r("fletch_" + sh.id, F, sh.name, Skill.FLETCHING, sh.level, sh.xp).in(sh.log, 2).out(sh.id, 1)
                    .tool("knife").ticks(3).build());
        }
        out.accept(r("fletch_celastrus_battlestaff", F, "Battlestaff (celastrus bark)", Skill.FLETCHING, 40, 80.0)
                .in("celastrus_bark", 1).out("battlestaff", 1).tool("knife").ticks(3).build());
        // Ballistae (OSRS Wiki): limbs on a frame, then the spring, then a monkey tail. Light 47 Fletching (15 + 15 + 300 XP),
        // heavy 72 Fletching (30 + 30 + 600 XP). Parts and ballistae are CommonItems/CommonEquipment.
        for (String size : new String[]{"light", "heavy"}) {
            boolean heavy = size.equals("heavy");
            int level = heavy ? 72 : 47;
            double step = heavy ? 30.0 : 15.0;
            String name = heavy ? "heavy ballista" : "light ballista";
            out.accept(r("fletch_incomplete_" + size + "_ballista", F, "Incomplete " + name, Skill.FLETCHING, level, step)
                    .in(size + "_frame", 1).in("ballista_limbs", 1).out("incomplete_" + size + "_ballista", 1).ticks(3).build());
            out.accept(r("fletch_unstrung_" + size + "_ballista", F, "Unstrung " + name, Skill.FLETCHING, level, step)
                    .in("incomplete_" + size + "_ballista", 1).in("ballista_spring", 1).out("unstrung_" + size + "_ballista", 1)
                    .ticks(3).build());
            out.accept(r("fletch_" + size + "_ballista", F, Character.toUpperCase(name.charAt(0)) + name.substring(1), Skill.FLETCHING,
                    level, heavy ? 600.0 : 300.0).in("unstrung_" + size + "_ballista", 1).in("monkey_tail", 1)
                    .out(size + "_ballista", 1).ticks(3).build());
        }
    }
}
