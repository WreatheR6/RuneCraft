package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentDef.Slot;
import com.runecraft.osrs.EquipmentDef.WeaponClass;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.model.ModelSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The Gauntlet and the Corrupted Gauntlet (Prifddinas, Song of the Elves): a solo run through a 7x7 maze of rooms generated
 * per run. The player enters with nothing, gathers crystal ore, phren bark, linum tirinum, grym leaves and paddlefish, kills
 * crystalline creatures and demi-bosses for shards, weapon frames and the perfecting components, sings armour and weapons at
 * the singing bowl, then fights the Hunllef in the centre room (entered through the barrier, or forced when the preparation
 * timer runs out).
 *
 * <p>Wiki sources (dump of July 2026, after the 22 July 2026 "Summer Sweep-Up" changes): The Gauntlet, Reward Chest (The
 * Gauntlet), each attuned item page, the creature pages. Equipment costs and bonuses are snapshotted in
 * {@code src/test/resources/osrs/minigames/gauntlet.tsv} and diffed by {@code GauntletTest}.
 *
 * <p>The player enters with a crystal (corrupted) axe, pickaxe and harpoon and a pestle and mortar: phren roots need the axe,
 * crystal deposits the pickaxe and fishing spots the harpoon (linum tirinum and grym roots need no tool). Egniol potions are
 * made as in OSRS: a grym leaf in a water-filled vial gives a grym potion (unf), the pestle grinds shards into dust ten at a
 * time, and ten dust finish the potion (3 doses, 10 Herblore XP). Simplification: Minecraft bows need ammunition, so the run keeps a
 * quiver of weightless crystal arrows topped up while a Gauntlet bow is carried (OSRS: the bow needs none).
 */
public final class Gauntlet {
    private Gauntlet() {
    }

    // ------------------------------------------------------------------ modes and timers

    /** The two versions. The corrupted one is unlocked by completing the normal Gauntlet once. */
    public enum Mode {
        NORMAL("crystal", "Crystal", "crystalline", "crystalline_hunllef", 1000),
        CORRUPTED("corrupted", "Corrupted", "corrupted", "corrupted_hunllef", 750);

        /** Item id prefix and display word ("crystal"/"corrupted"). */
        public final String prefix;
        public final String word;
        /** Creature id prefix ("crystalline"/"corrupted"). */
        public final String creaturePrefix;
        public final String boss;
        /** Preparation time in game ticks: 10:00 (normal) and 7:30 (corrupted). */
        public final int prepTicks;

        Mode(String prefix, String word, String creaturePrefix, String boss, int prepTicks) {
            this.prefix = prefix;
            this.word = word;
            this.creaturePrefix = creaturePrefix;
            this.boss = boss;
            this.prepTicks = prepTicks;
        }

        /** The Combat Achievement run stage: 1 normal, 2 corrupted. */
        public int stage() {
            return ordinal() + 1;
        }
    }

    /** Extra entrance block for the Corrupted Gauntlet (the normal one is the minigame's own entrance). */
    public static final String CORRUPTED_ENTRANCE = "corrupted_gauntlet_entrance";
    /**
     * The Gauntlet Portal's place in north-west Prifddinas (wiki map 3228, 6116; until round 34 at 3234 with the entrances and
     * chest beside it). The portal leads into the lobby ({@link GauntletLobby}), where the entrances and the reward chest are.
     */
    public static final String PLACE = "mg_gauntlet";
    public static final int ENTRANCE_X = GauntletLobby.PORTAL_X;
    public static final int ENTRANCE_Y = GauntletLobby.PORTAL_Y;
    /** The instanced maze (starting room around 1908, 5666). */
    public static final int MAZE_X = 1908;
    public static final int MAZE_Y = 5666;

    // ------------------------------------------------------------------ arena blocks

    public static final String SINGING_BOWL = "gauntlet_singing_bowl";
    public static final String RANGE = "gauntlet_range";
    public static final String WATER_PUMP = "gauntlet_water_pump";
    public static final String BARRIER = "gauntlet_barrier";
    public static final String RECIPES_BOOK = "gauntlet_recipes";

    /**
     * Resource nodes: block id, display name, yield per node, colour, look, and the skill and experience of each resource
     * gathered (wiki node pages, Skill info: Mining, Woodcutting and Fishing 10, Farming 1 for the plants; no level needed).
     */
    public enum Node {
        ORE("gauntlet_crystal_deposit", "Crystal deposit", 3, 0x9AD8F0, "rock", Skill.MINING, 10),
        BARK("gauntlet_phren_roots", "Phren roots", 3, 0x8A6A9A, "roots", Skill.WOODCUTTING, 10),
        LINUM("gauntlet_linum_tirinum", "Linum tirinum", 3, 0xC0E0F0, "sapling", Skill.FARMING, 1),
        GRYM("gauntlet_grym_root", "Grym root", 1, 0x5AA05A, "roots", Skill.FARMING, 1),
        FISH("gauntlet_fishing_spot", "Fishing spot", 4, 0x4080C0, "water_spot", Skill.FISHING, 10);

        public final String block;
        public final String displayName;
        public final int yield;
        public final int colour;
        public final String look;
        public final Skill skill;
        public final double xp;

        Node(String block, String displayName, int yield, int colour, String look, Skill skill, double xp) {
            this.block = block;
            this.displayName = displayName;
            this.yield = yield;
            this.colour = colour;
            this.look = look;
            this.skill = skill;
            this.xp = xp;
        }

        /** The tool gathering needs (wiki: crystal pickaxe, axe, harpoon; none for linum tirinum and grym roots), or null. */
        public String tool(Mode m) {
            return switch (this) {
                case ORE -> m.prefix + "_gauntlet_pickaxe";
                case BARK -> m.prefix + "_gauntlet_axe";
                case FISH -> m.prefix + "_gauntlet_harpoon";
                default -> null;
            };
        }

        /** The item a node gives in this mode. */
        public String item(Mode m) {
            return switch (this) {
                case ORE -> ore(m);
                case BARK -> bark(m);
                case LINUM -> linum(m);
                case GRYM -> grym(m);
                case FISH -> RAW_PADDLEFISH;
            };
        }

        public static Node byBlock(String block) {
            for (Node n : values()) {
                if (n.block.equals(block)) {
                    return n;
                }
            }
            return null;
        }
    }

    /** Gathering from a node gives shards 1 time in 3 (wiki: crystal deposit and grym root; assumed for every node). */
    public static final int NODE_SHARD_CHANCE = 3;
    public static final int NODE_SHARD_MIN = 10;
    public static final int NODE_SHARD_MAX = 30;

    /** Starting room: gives back a lost sceptre, tool or pestle and mortar (wiki Tool Storage). */
    public static final String TOOL_STORAGE = "gauntlet_tool_storage";
    /**
     * Teleport Platform (wiki): in the starting room it abandons the run with no loot; in the lobby it leads back to the
     * Gauntlet Portal in Prifddinas.
     */
    public static final String TELEPORT_PLATFORM = "gauntlet_teleport_platform";
    /** The Gauntlet Portal in the north-west of Prifddinas: leads into the lobby. */
    public static final String PORTAL = "gauntlet_portal";
    /** The lobby's scoreboard: completions, deaths and best times of both versions. */
    public static final String SCOREBOARD = "gauntlet_scoreboard";
    /** The lobby's and starting room's "Egniol Potions" bookstand. */
    public static final String EGNIOL_BOOK = "gauntlet_egniol_potions";

    /** Every interactive block placed in the maze or the lobby: id, name, colour, look (registered with the module's arena blocks). */
    public static List<String[]> blocks() {
        List<String[]> list = new ArrayList<>();
        list.add(new String[]{SINGING_BOWL, "Singing bowl", "9AD8F0", "altar"});
        list.add(new String[]{RANGE, "Range", "6A6A7A", "range"});
        list.add(new String[]{WATER_PUMP, "Water pump", "4A6A9A", "machine"});
        list.add(new String[]{BARRIER, "Barrier", "B0E8FF", "portal_glow"});
        list.add(new String[]{RECIPES_BOOK, "Crystal Singing Recipes", "C0E0F0", "notice_board"});
        list.add(new String[]{REWARD_CHEST, "Reward Chest (The Gauntlet)", "7A9AB0", "chest"});
        for (Node n : Node.values()) {
            list.add(new String[]{n.block, n.displayName, String.format("%06X", n.colour), n.look});
        }
        // Round 34 (appended): the starting room's tool storage and teleport platform, the lobby and its portal.
        list.add(new String[]{TOOL_STORAGE, "Tool Storage", "8A9AB0", "crate"});
        list.add(new String[]{TELEPORT_PLATFORM, "Teleport Platform", "A0D8F0", "portal_glow"});
        list.add(new String[]{PORTAL, "Gauntlet Portal", "8AD0F8", "portal_glow"});
        list.add(new String[]{SCOREBOARD, "Scoreboard (The Gauntlet)", "6A8AA0", "notice_board"});
        list.add(new String[]{EGNIOL_BOOK, "Egniol Potions", "60C0A0", "notice_board"});
        return list;
    }

    // ------------------------------------------------------------------ items

    public static final String RAW_PADDLEFISH = "raw_paddlefish";
    public static final String PADDLEFISH = "paddlefish";
    public static final String BURNT_PADDLEFISH = "burnt_paddlefish";
    public static final String VIAL = "gauntlet_vial";
    public static final String WATER_VIAL = "gauntlet_water_vial";
    public static final String EGNIOL = "egniol_potion";
    /** Given on entry; grinds shards into crystal dust ten at a time. */
    public static final String PESTLE = "gauntlet_pestle_and_mortar";
    /** Grym leaf (either mode) + water-filled vial. */
    public static final String GRYM_UNF = "gauntlet_grym_potion_unf";
    public static final int DUST_PER_GRIND = 10;
    public static final int EGNIOL_DUST = 10;
    public static final double EGNIOL_XP = 10;
    public static final String ARROWS = "gauntlet_crystal_arrow";
    /** Escape crystal (wiki): 200 shards at the singing bowl (100 Crafting and Smithing XP); leaves the Gauntlet at once. */
    public static final String ESCAPE_CRYSTAL = "gauntlet_escape_crystal";
    public static final int ESCAPE_CRYSTAL_SHARDS = 200;
    /** The reward chest in the lobby, where the loot of a run waits after leaving. */
    public static final String REWARD_CHEST = "gauntlet_reward_chest";
    /** The junk table's items (only found in the Gauntlet's reward chest). */
    public static final String IWANS_FLYER = "iwans_flyer";
    public static final String APOTHECARY_POTION = "apothecary_potion";
    public static final String ROTTEN_TOMATO = "rotten_tomato";

    /** Paddlefish heal 20; crystal/corrupted paddlefish heal 16 and can be combo-eaten. Paddlefish stop burning at 47. */
    public static final int PADDLEFISH_HEAL = 20;
    public static final int CRYSTAL_PADDLEFISH_HEAL = 16;
    public static final int PADDLEFISH_STOP_BURN = 47;
    public static final double PADDLEFISH_COOK_XP = 15;
    /** Paddlefish cooking chance on the range (wiki Skilling success chart: low 155, high 368; 100% from level 47). */
    public static final int PADDLEFISH_LOW = 155;
    public static final int PADDLEFISH_HIGH = 368;

    public static double cookChance(int cookingLevel) {
        return com.runecraft.osrs.Chance.lerp(cookingLevel, PADDLEFISH_LOW, PADDLEFISH_HIGH);
    }
    /** Egniol potions are made with 3 doses; each restores prayer (floor(level / 4) + 7) and 40% run energy (a stamina). */
    public static final int EGNIOL_DOSES = 3;

    public static int egniolPrayer(int prayerLevel) {
        return prayerLevel / 4 + 7;
    }

    public static String shards(Mode m) {
        return m.prefix + "_gauntlet_shards";
    }

    /** Crystal dust (corrupted dust): one per shard ground with the pestle and mortar. */
    public static String dust(Mode m) {
        return m.prefix + "_gauntlet_dust";
    }

    /** What the player is handed on entering: the sceptre, a teleport crystal, the three tools and the pestle and mortar. */
    public static List<String> startingItems(Mode m) {
        return List.of(sceptre(m), teleport(m), Node.BARK.tool(m), Node.ORE.tool(m), Node.FISH.tool(m), PESTLE);
    }

    public static String ore(Mode m) {
        return m.prefix + "_gauntlet_ore";
    }

    public static String bark(Mode m) {
        return m.prefix + "_phren_bark";
    }

    public static String linum(Mode m) {
        return m.prefix + "_linum_tirinum";
    }

    public static String grym(Mode m) {
        return m.prefix + "_grym_leaf";
    }

    public static String frame(Mode m) {
        return m.prefix + "_weapon_frame";
    }

    public static String teleport(Mode m) {
        return m.prefix + "_gauntlet_teleport";
    }

    public static String sceptre(Mode m) {
        return m.prefix + "_gauntlet_sceptre";
    }

    public static String upgradedFish(Mode m) {
        return m.prefix + "_paddlefish";
    }

    /** Armour and weapon pieces, in the snapshot's order. */
    public enum Piece {
        HELM(Slot.HEAD, "full_helm"), BODY(Slot.CHEST, "platebody"), LEGS(Slot.LEGS, "platelegs"),
        HALBERD(Slot.WEAPON, "halberd"), BOW(Slot.WEAPON, "crystal_bow"), STAFF(Slot.WEAPON, "staff_orb");

        public final Slot slot;
        public final String shape;

        Piece(Slot slot, String shape) {
            this.slot = slot;
            this.shape = shape;
        }

        public boolean armour() {
            return slot != Slot.WEAPON;
        }

        public String key() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** Tiers 1-3. */
    public enum Tier {
        BASIC, ATTUNED, PERFECTED;

        public int level() {
            return ordinal() + 1;
        }

        public String key() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static String equipment(Mode m, Piece p, Tier t) {
        return m.prefix + "_" + p.key() + "_" + t.key();
    }

    /** The perfecting component of a weapon: crystal spike (halberd), crystal orb (staff), crystalline bowstring (bow). */
    public static String component(Mode m, Piece p) {
        return switch (p) {
            case HALBERD -> m.prefix + "_gauntlet_spike";
            case STAFF -> m.prefix + "_gauntlet_orb";
            case BOW -> m.prefix + "_gauntlet_bowstring";
            default -> null;
        };
    }

    /** The component a demi-boss always drops the first time: bear spike, dragon orb, dark beast bowstring. */
    public static Piece componentOf(String demi) {
        return switch (demi) {
            case "bear" -> Piece.HALBERD;
            case "dragon" -> Piece.STAFF;
            default -> Piece.BOW;
        };
    }

    /** One piece's materials and bonuses (wiki; see the snapshot). */
    public record Gear(int shards, int ore, int bark, int linum, EquipmentStats stats, int speed, int staffMaxHit) {
    }

    private static EquipmentStats st(int sa, int la, int ca, int ma, int ra, int sd, int ld, int cd, int md, int rd, int str, int rstr,
                                     int prayer) {
        return new EquipmentStats(sa, la, ca, ma, ra, sd, ld, cd, md, rd, str, rstr, 0, prayer);
    }

    private static EquipmentStats armour(int a, int c, int d, int prayer) {
        return st(a, a, c, a, a, d, d, d, d, d, 0, 0, prayer);
    }

    /** Costs and bonuses; the corrupted pieces are identical (wiki: "All items are Corrupted instead of Crystal, with the same stats"). */
    public static Gear gear(Piece p, Tier t) {
        int i = t.ordinal();
        return switch (p) {
            case HELM -> new Gear(new int[]{50, 50, 100}[i], i == 2 ? 2 : 1, i == 2 ? 2 : 1, i == 2 ? 2 : 1,
                    new EquipmentStats[]{armour(2, 1, 28, 1), armour(6, 2, 48, 2), armour(10, 3, 68, 3)}[i], 0, 0);
            case BODY -> new Gear(new int[]{50, 100, 100}[i], i == 0 ? 1 : 2, i == 0 ? 1 : 2, i == 0 ? 1 : 2,
                    new EquipmentStats[]{armour(8, 3, 86, 3), armour(12, 4, 102, 4), st(15, 16, 5, 16, 16, 124, 124, 124, 124, 124, 0, 0, 5)}[i],
                    0, 0);
            case LEGS -> new Gear(new int[]{50, 50, 100}[i], i == 2 ? 2 : 1, i == 2 ? 2 : 1, i == 2 ? 2 : 1,
                    new EquipmentStats[]{armour(6, 2, 52, 2), armour(10, 3, 74, 3), armour(14, 4, 92, 4)}[i], 0, 0);
            case HALBERD -> new Gear(i == 1 ? 50 : 0, 0, 0, 0, new EquipmentStats[]{st(68, 68, 4, 0, 0, 0, 0, 0, 0, 0, 42, 0, 1),
                    st(114, 114, 12, 0, 0, 0, 0, 0, 0, 0, 88, 0, 2), st(166, 166, 28, 0, 0, 0, 0, 0, 0, 0, 138, 0, 3)}[i], 4, 0);
            case BOW -> new Gear(i == 1 ? 50 : 0, 0, 0, 0, new EquipmentStats[]{st(0, 0, 0, 0, 72, 0, 0, 0, 0, 0, 0, 42, 1),
                    st(0, 0, 0, 0, 118, 0, 0, 0, 0, 0, 0, 88, 2), st(0, 0, 0, 0, 172, 0, 0, 0, 0, 0, 0, 138, 3)}[i], 5, 0);
            case STAFF -> new Gear(i == 1 ? 50 : 0, 0, 0, 0, new EquipmentStats[]{st(0, 0, 0, 84, 0, 0, 0, 0, 0, 0, 0, 0, 1),
                    st(0, 0, 0, 128, 0, 0, 0, 0, 0, 0, 0, 0, 2), st(0, 0, 0, 184, 0, 0, 0, 0, 0, 0, 0, 0, 3)}[i], 4,
                    new int[]{23, 31, 39}[i]);
        };
    }

    /** The staves' fixed max hit (23 / 31 / 39 regardless of Magic level), or -1 for any other item. */
    public static int staffMaxHit(String itemId) {
        for (Mode m : Mode.values()) {
            for (Tier t : Tier.values()) {
                if (equipment(m, Piece.STAFF, t).equals(itemId)) {
                    return gear(Piece.STAFF, t).staffMaxHit();
                }
            }
        }
        return -1;
    }

    /** The armour tier (1-3) of a worn Gauntlet armour piece, or 0. */
    public static int armourTier(String itemId) {
        if (itemId == null) {
            return 0;
        }
        for (Mode m : Mode.values()) {
            for (Piece p : new Piece[]{Piece.HELM, Piece.BODY, Piece.LEGS}) {
                for (Tier t : Tier.values()) {
                    if (equipment(m, p, t).equals(itemId)) {
                        return t.level();
                    }
                }
            }
        }
        return 0;
    }

    /**
     * The Hunllef's max hit through the right protection prayer by the sum of worn armour tiers (wiki tables: 0/3/6/9 →
     * 12/10/8/6 normal, 16/13/10/8 corrupted; sums in between interpolated, approx). Tornadoes scale the same way.
     */
    public static int hunllefMaxHit(Mode m, int tierSum) {
        int[] at = m == Mode.NORMAL ? new int[]{12, 10, 8, 6} : new int[]{16, 13, 10, 8};
        int s = Math.max(0, Math.min(9, tierSum));
        int lo = s / 3;
        if (lo == 3) {
            return at[3];
        }
        double f = (s - lo * 3) / 3.0;
        return (int) Math.round(at[lo] + (at[lo + 1] - at[lo]) * f);
    }

    /** Multiplier on the Hunllef's (and its tornadoes') damage for the worn armour. */
    public static double hunllefDamageScale(Mode m, int tierSum) {
        return hunllefMaxHit(m, tierSum) / (double) hunllefMaxHit(m, 0);
    }

    public static void items(ItemSink items) {
        items.add(ESCAPE_CRYSTAL, "Escape crystal", Kind.SIMPLE, "gem", 0xD0F0FF).value(1).stack(1)
                .examine("A crystal enchanted to allow the user to escape the Gauntlet.");
        for (Mode m : Mode.values()) {
            boolean c = m == Mode.CORRUPTED;
            int col = c ? 0xC04030 : 0x9AD8F0;
            simple(items, shards(m), m.word + " shards", "shard", col, 60000, "Shards of " + m.word.toLowerCase() + " crystal, used at the singing bowl.");
            simple(items, ore(m), m.word + " ore", "ore", col, 60000, "Ore for singing armour.");
            simple(items, bark(m), c ? "Phren bark (corrupted)" : "Phren bark", "log", c ? 0x8A3A3A : 0x8A6A9A, 60000, "Bark for singing armour.");
            simple(items, linum(m), c ? "Linum tirinum (corrupted)" : "Linum tirinum", "flax", c ? 0xC06060 : 0xC0E0F0, 60000,
                    "A fibrous plant for singing armour.");
            simple(items, grym(m), c ? "Grym leaf (corrupted)" : "Grym leaf", "herb", c ? 0xA04040 : 0x5AA05A, 1, "A herb for egniol potions.");
            simple(items, dust(m), c ? "Corrupted dust" : "Crystal dust", "secondary", col, 60000,
                    "Crystal crushed down to the finest form imaginable, useful for making potions.");
            items.add(Node.BARK.tool(m), m.word + " axe", Kind.TOOL, "axe", col).value(1).stack(1).examine("A very powerful axe.");
            items.add(Node.ORE.tool(m), m.word + " pickaxe", Kind.TOOL, "pickaxe", col).value(1).stack(1)
                    .examine("A very powerful pickaxe.");
            items.add(Node.FISH.tool(m), m.word + " harpoon", Kind.TOOL, "harpoon", col).value(1).stack(1)
                    .examine("A very powerful harpoon.");
            simple(items, frame(m), c ? "Weapon frame (corrupted)" : "Weapon frame", "shaft", col, 1, "The frame of a crystal weapon.");
            simple(items, component(m, Piece.HALBERD), m.word + " spike", "arrowtips", col, 1, "Perfects a halberd.");
            simple(items, component(m, Piece.STAFF), m.word + " orb", "orb", col, 1, "Perfects a staff.");
            simple(items, component(m, Piece.BOW), c ? "Corrupted bowstring" : "Crystalline bowstring", "string", col, 1, "Perfects a bow.");
            simple(items, teleport(m), c ? "Corrupted teleport crystal" : "Teleport crystal", "gem", col, 1,
                    "Teleports you back to the starting room.");
            items.add(upgradedFish(m), m.word + " paddlefish", Kind.FOOD, "fish_cooked", col).value(1).stack(1).heal(CRYSTAL_PADDLEFISH_HEAL)
                    .examine("A paddlefish infused with crystal. Can be eaten straight after another fish.");
            items.add(sceptre(m), m.word + " sceptre", Kind.MELEE_WEAPON, "sceptre", col).value(1).stack(1)
                    .equipment(new EquipmentDef(Slot.WEAPON, st(8, 10, 16, 0, 0, 0, 0, 0, 0, 0, 20, 0, 0), Map.of(), 5, AttackType.CRUSH,
                            WeaponClass.MELEE))
                    .examine("Lights the nodes of the Gauntlet. A weak weapon.");
            for (Piece p : Piece.values()) {
                for (Tier t : Tier.values()) {
                    Gear g = gear(p, t);
                    String name = m.word + " " + p.key() + " (" + t.key() + ")";
                    int tint = t == Tier.BASIC ? shade(col, 0.75) : t == Tier.ATTUNED ? col : shade(col, 1.2);
                    Kind kind = p == Piece.BOW ? Kind.BOW : p == Piece.STAFF ? Kind.STAFF : p == Piece.HALBERD ? Kind.MELEE_WEAPON : Kind.ARMOUR;
                    EquipmentDef def = switch (p) {
                        case HALBERD -> new EquipmentDef(Slot.WEAPON, g.stats(), Map.of(), g.speed(), AttackType.SLASH, WeaponClass.MELEE);
                        case BOW -> new EquipmentDef(Slot.WEAPON, g.stats(), Map.of(), g.speed(), AttackType.RANGED, WeaponClass.BOW);
                        case STAFF -> new EquipmentDef(Slot.WEAPON, g.stats(), Map.of(), g.speed(), AttackType.MAGIC, WeaponClass.STAFF);
                        default -> new EquipmentDef(p.slot, g.stats(), Map.of(), 0, AttackType.CRUSH, WeaponClass.NONE);
                    };
                    items.add(equipment(m, p, t), name, kind, p.shape, tint).value(1).stack(1).equipment(def)
                            .examine("Sung from " + m.word.toLowerCase() + " crystal in the Gauntlet.");
                }
            }
        }
        items.add(RAW_PADDLEFISH, "Raw paddlefish", Kind.SIMPLE, "fish_raw", 0x6A8AA0).value(1).stack(1).examine("I should cook this.");
        items.add(PADDLEFISH, "Paddlefish", Kind.FOOD, "fish_cooked", 0xC09060).value(1).stack(1).heal(PADDLEFISH_HEAL)
                .examine("A cooked paddlefish.");
        items.add(BURNT_PADDLEFISH, "Burnt fish", Kind.SIMPLE, "fish_burnt", 0x3A3028).value(1).stack(1).examine("Oops!");
        items.add(VIAL, "Vial", Kind.SIMPLE, "vial", 0xC0E0F0).value(1).stack(1).examine("A crystal vial.");
        items.add(PESTLE, "Pestle and mortar", Kind.TOOL, "pestle", 0x9A9AA0).value(1).stack(1)
                .examine("I can grind things for potions in this.");
        items.add(GRYM_UNF, "Grym potion (unf)", Kind.SIMPLE, "potion_unf", 0x70B070).value(1).stack(1)
                .examine("I need another ingredient to finish this Grym potion.");
        items.add(WATER_VIAL, "Water-filled vial", Kind.SIMPLE, "vial_water", 0x80B0E0).value(1).stack(1).examine("A crystal vial of water.");
        items.add(EGNIOL, "Egniol potion", Kind.POTION, "potion", 0x60C0A0).value(1).stack(1)
                .examine("Restores prayer and run energy.");
        items.add(ARROWS, "Crystal arrows (Gauntlet)", Kind.ARROW, "arrow", 0x9AD8F0).value(0).stack(64).rangedStrength(0)
                .examine("Sung by your Gauntlet bow; they vanish when you leave.");
        items.add(IWANS_FLYER, "Iwan's flyer", Kind.SIMPLE, "note", 0xE0D8B0).value(1).stack(1)
                .examine("A flyer for Iwan's shop.");
        items.add(APOTHECARY_POTION, "Potion", Kind.SIMPLE, "potion", 0xA0A0A0).value(1).stack(1)
                .examine("A potion of dubious origin.");
        items.add(ROTTEN_TOMATO, "Rotten tomato", Kind.SIMPLE, "produce", 0x6A4A2A).value(1).stack(1)
                .examine("Ewww.");
    }

    private static void simple(ItemSink items, String id, String name, String shape, int colour, int stack, String examine) {
        items.add(id, name, Kind.SIMPLE, shape, colour).value(1).stack(stack).examine(examine);
    }

    private static int shade(int rgb, double f) {
        int r = Math.min(255, (int) (((rgb >> 16) & 255) * f));
        int g = Math.min(255, (int) (((rgb >> 8) & 255) * f));
        int b = Math.min(255, (int) ((rgb & 255) * f));
        return (r << 16) | (g << 8) | b;
    }

    /** Every item that exists only inside the Gauntlet (taken away when the player leaves). */
    public static List<String> runItems() {
        List<String> ids = new ArrayList<>();
        for (Mode m : Mode.values()) {
            ids.addAll(List.of(shards(m), ore(m), bark(m), linum(m), grym(m), frame(m), component(m, Piece.HALBERD),
                    component(m, Piece.STAFF), component(m, Piece.BOW), teleport(m), upgradedFish(m), sceptre(m), dust(m),
                    Node.BARK.tool(m), Node.ORE.tool(m), Node.FISH.tool(m)));
            for (Piece p : Piece.values()) {
                for (Tier t : Tier.values()) {
                    ids.add(equipment(m, p, t));
                }
            }
        }
        ids.addAll(List.of(RAW_PADDLEFISH, PADDLEFISH, BURNT_PADDLEFISH, VIAL, WATER_VIAL, EGNIOL, ARROWS, ESCAPE_CRYSTAL, PESTLE,
                GRYM_UNF));
        return ids;
    }

    // ------------------------------------------------------------------ the singing bowl

    /**
     * One singing bowl recipe. XP: the bowl gives Smithing and Crafting each half the shards used (wiki, Singing Bowl);
     * egniol potions give 10 Herblore XP instead. {@code points} count towards the reward chest of an unfinished run.
     */
    public record Sing(String id, String name, String out, int outCount, int shards, int ore, int bark, int linum, String needs,
                       String component, int points, double herbloreXp, Piece piece, Tier tier) {
        public double xpEach() {
            return shards / 2.0;
        }

        /** Every input, item id to count. */
        public Map<String, Integer> inputs(Mode m) {
            Map<String, Integer> in = new LinkedHashMap<>();
            if (shards > 0) {
                in.put(Gauntlet.shards(m), shards);
            }
            if (ore > 0) {
                in.put(Gauntlet.ore(m), ore);
            }
            if (bark > 0) {
                in.put(Gauntlet.bark(m), bark);
            }
            if (linum > 0) {
                in.put(Gauntlet.linum(m), linum);
            }
            if (needs != null) {
                in.merge(needs, 1, Integer::sum);
            }
            if (component != null) {
                in.merge(component, 1, Integer::sum);
            }
            return in;
        }
    }

    /** Points for crafting a tier (wiki: basic 2, attuned 5, perfected 10). */
    public static int craftPoints(Tier t) {
        return switch (t) {
            case BASIC -> 2;
            case ATTUNED -> 5;
            case PERFECTED -> 10;
        };
    }

    /** The singing bowl's recipes in one mode. */
    public static List<Sing> recipes(Mode m) {
        List<Sing> list = new ArrayList<>();
        list.add(new Sing("vial", "Vial", VIAL, 1, 10, 0, 0, 0, null, null, 0, 0, null, null));
        list.add(new Sing("teleport", m == Mode.NORMAL ? "Teleport crystal" : "Corrupted teleport crystal", teleport(m), 1, 50, 0, 0, 0,
                null, null, 0, 0, null, null));
        list.add(new Sing("escape", "Escape crystal", ESCAPE_CRYSTAL, 1, ESCAPE_CRYSTAL_SHARDS, 0, 0, 0, null, null, 0, 0, null, null));
        list.add(new Sing("paddlefish", m.word + " paddlefish", upgradedFish(m), 1, 10, 0, 0, 0, PADDLEFISH, null, 1, 0, null, null));
        // Egniol potions aren't sung: see GRYM_UNF, PESTLE and dust().
        for (Piece p : Piece.values()) {
            for (Tier t : Tier.values()) {
                Gear g = gear(p, t);
                String needs = t == Tier.BASIC ? (p.armour() ? null : frame(m)) : equipment(m, p, Tier.values()[t.ordinal() - 1]);
                String comp = !p.armour() && t == Tier.PERFECTED ? component(m, p) : null;
                list.add(new Sing(p.key() + "_" + t.key(), m.word + " " + p.key() + " (" + t.key() + ")", equipment(m, p, t), 1,
                        g.shards(), g.ore(), g.bark(), g.linum(), needs, comp, craftPoints(t), 0, p, t));
            }
        }
        return list;
    }

    public static Sing recipe(Mode m, String id) {
        for (Sing s : recipes(m)) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ points and the reward chest

    /** Points for a kill (wiki: weak 2, strong 5, demi-boss 10). */
    public static int killPoints(Foe f) {
        return switch (f) {
            case WEAK -> 2;
            case STRONG -> 5;
            case DEMI -> 10;
        };
    }

    /** Points for cooking a raw paddlefish. */
    public static final int COOK_POINTS = 1;
    /** An unfinished run: 50+ points rolls the incomplete table, 1-49 the junk table, 0 nothing. */
    public static final int INCOMPLETE_POINTS = 50;

    /** One weighted row of a reward table (weight out of the table's total). */
    public record Loot(String item, int min, int max, int weight) {
    }

    /** A reward table: rows, the weight total and how many rolls a completion gets. */
    public record Table(List<Loot> rows, int total, int rolls) {
        public Loot roll(Random r) {
            int x = r.nextInt(total);
            for (Loot l : rows) {
                x -= l.weight();
                if (x < 0) {
                    return l;
                }
            }
            return rows.get(rows.size() - 1);
        }
    }

    private static Loot l(String item, int min, int max, int weight) {
        return new Loot(item, min, max, weight);
    }

    /** Regular loot table (Crystalline Hunllef): two rolls, every row 1/24. Shards and uniques drop from the Hunllef itself. */
    public static final Table REGULAR = new Table(List.of(
            l("battlestaff", 4, 8, 1), l("rune_full_helm", 2, 4, 1), l("rune_chainbody", 1, 2, 1), l("rune_platebody", 1, 2, 1),
            l("rune_platelegs", 1, 2, 1), l("rune_plateskirt", 1, 2, 1), l("rune_halberd", 1, 2, 1), l("rune_pickaxe", 1, 2, 1),
            l("dragon_halberd", 1, 1, 1),
            l("cosmic_rune", 160, 240, 1), l("nature_rune", 100, 140, 1), l("law_rune", 80, 140, 1), l("chaos_rune", 180, 300, 1),
            l("death_rune", 100, 160, 1), l("blood_rune", 80, 140, 1), l("mithril_arrow", 800, 1200, 1), l("adamant_arrow", 400, 600, 1),
            l("rune_arrow", 200, 300, 1), l("dragon_arrow", 30, 85, 1),
            l("uncut_sapphire", 20, 60, 1), l("uncut_emerald", 10, 50, 1), l("uncut_ruby", 5, 30, 1), l("uncut_diamond", 3, 7, 1),
            l("coins", 20000, 80000, 1)), 24, 2);

    /** Corrupted loot table: three rolls out of 48 (rows 2/48; dragon halberd 1/48 and coins 3/48). */
    public static final Table CORRUPTED = new Table(List.of(
            l("battlestaff", 8, 12, 2), l("rune_full_helm", 3, 5, 2), l("rune_chainbody", 2, 3, 2), l("rune_platebody", 2, 2, 2),
            l("rune_platelegs", 2, 3, 2), l("rune_plateskirt", 2, 3, 2), l("rune_halberd", 2, 3, 2), l("rune_pickaxe", 2, 3, 2),
            l("dragon_halberd", 1, 2, 1),
            l("cosmic_rune", 175, 250, 2), l("nature_rune", 125, 150, 2), l("law_rune", 100, 150, 2), l("chaos_rune", 200, 350, 2),
            l("death_rune", 125, 175, 2), l("blood_rune", 100, 150, 2), l("mithril_arrow", 1000, 1500, 2), l("adamant_arrow", 500, 750, 2),
            l("rune_arrow", 250, 450, 2), l("dragon_arrow", 50, 100, 2),
            l("uncut_sapphire", 25, 65, 2), l("uncut_emerald", 15, 60, 2), l("uncut_ruby", 10, 40, 2), l("uncut_diamond", 5, 15, 2),
            l("coins", 75000, 150000, 3)), 48, 3);

    /** Incomplete loot table (50+ points, Hunllef not killed): one roll, every row 1/27. */
    public static final Table INCOMPLETE = new Table(List.of(
            l("adamant_dagger", 1, 1, 1), l("adamant_full_helm", 1, 1, 1), l("adamant_mace", 2, 3, 1), l("adamant_pickaxe", 1, 1, 1),
            l("adamant_platebody", 1, 1, 1), l("adamant_platelegs", 1, 1, 1), l("adamant_plateskirt", 1, 1, 1),
            l("adamant_scimitar", 1, 1, 1), l("maple_longbow", 7, 13, 1), l("maple_shortbow", 8, 11, 1), l("mithril_full_helm", 1, 1, 1),
            l("mithril_mace", 2, 5, 1), l("mithril_platebody", 1, 1, 1), l("mithril_platelegs", 1, 1, 1), l("mithril_plateskirt", 1, 1, 1),
            l("air_rune", 200, 300, 1), l("body_rune", 250, 350, 1), l("earth_rune", 200, 300, 1), l("fire_rune", 200, 300, 1),
            l("mind_rune", 300, 400, 1), l("water_rune", 200, 300, 1),
            l("cake", 10, 20, 1), l("cod", 75, 125, 1), l("trout", 50, 100, 1), l("eye_of_newt", 300, 500, 1), l("silver_bar", 15, 30, 1),
            l("uncut_sapphire", 1, 3, 1)), 27, 1);

    /** Junk table (1-49 points): one roll, each 1/3. */
    public static final Table JUNK = new Table(List.of(l(IWANS_FLYER, 1, 1, 1), l(APOTHECARY_POTION, 1, 1, 1), l(ROTTEN_TOMATO, 1, 1, 1)),
            3, 1);

    /** The main table a Hunllef kill rolls. */
    public static Table completion(Mode m) {
        return m == Mode.NORMAL ? REGULAR : CORRUPTED;
    }

    /** The table an unfinished run rolls by its points, or null (0 points: the chest stays empty). */
    public static Table unfinished(int points) {
        return points >= INCOMPLETE_POINTS ? INCOMPLETE : points > 0 ? JUNK : null;
    }

    public static Mode modeOfBoss(String bossId) {
        for (Mode m : Mode.values()) {
            if (m.boss.equals(bossId)) {
                return m;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ creatures

    /** Monster groups. */
    public enum Foe {
        WEAK("spider", "rat", "bat"), STRONG("unicorn", "scorpion", "wolf"), DEMI("bear", "dark_beast", "dragon");

        public final List<String> kinds;

        Foe(String... kinds) {
            this.kinds = List.of(kinds);
        }

        public static Foe of(String kind) {
            for (Foe f : values()) {
                if (f.kinds.contains(kind)) {
                    return f;
                }
            }
            return null;
        }
    }

    public static String creature(Mode m, String kind) {
        return m.creaturePrefix + "_" + kind;
    }

    /** The kind ("bear", "rat"...) and mode of a Gauntlet creature id, or null. */
    public static String kindOf(String creatureId) {
        for (Mode m : Mode.values()) {
            String pre = m.creaturePrefix + "_";
            if (creatureId != null && creatureId.startsWith(pre) && Foe.of(creatureId.substring(pre.length())) != null) {
                return creatureId.substring(pre.length());
            }
        }
        return null;
    }

    /** Shards a creature drops (wiki: weak 20-30, strong 80-100, demi-boss 50-60). */
    public static int[] creatureShards(Foe f) {
        return switch (f) {
            case WEAK -> new int[]{20, 30};
            case STRONG -> new int[]{80, 100};
            case DEMI -> new int[]{50, 60};
        };
    }

    /** The creature resource rows' items, as their crystal (normal) ids; {@link #resourceItem} gives each mode's own. */
    public static final String RES_SHARDS = "crystal_gauntlet_shards";
    public static final String RES_FISH = "raw_paddlefish";
    public static final String RES_GRYM = "crystal_grym_leaf";
    public static final String RES_TELEPORT = "crystal_gauntlet_teleport";

    /**
     * A creature's resource roll on top of its 100% shards (wiki Drop Log Project rates of the corrupted creatures; one roll,
     * item null = nothing): weak 15/24 (shards 3-7 9/24, raw paddlefish 1-3 3/24, grym leaf 2/24, teleport crystal 1/24),
     * strong 15/21 (shards 7-14 9/21, paddlefish 2-4 3/21, grym 2/21, teleport 1/21), demi-bosses 15/18 (shards 10-21 9/18,
     * paddlefish 3-5 3/18, grym 2/18, teleport 1/18). The crystalline pages list the same items without rates (approx: the
     * same table).
     */
    public static Table resources(Foe f) {
        return switch (f) {
            case WEAK -> WEAK_RESOURCES;
            case STRONG -> STRONG_RESOURCES;
            case DEMI -> DEMI_RESOURCES;
        };
    }

    private static final Table WEAK_RESOURCES = new Table(List.of(l(null, 0, 0, 9), l(RES_SHARDS, 3, 7, 9), l(RES_FISH, 1, 3, 3),
            l(RES_GRYM, 1, 1, 2), l(RES_TELEPORT, 1, 1, 1)), 24, 1);
    private static final Table STRONG_RESOURCES = new Table(List.of(l(null, 0, 0, 6), l(RES_SHARDS, 7, 14, 9), l(RES_FISH, 2, 4, 3),
            l(RES_GRYM, 1, 1, 2), l(RES_TELEPORT, 1, 1, 1)), 21, 1);
    private static final Table DEMI_RESOURCES = new Table(List.of(l(null, 0, 0, 3), l(RES_SHARDS, 10, 21, 9), l(RES_FISH, 3, 5, 3),
            l(RES_GRYM, 1, 1, 2), l(RES_TELEPORT, 1, 1, 1)), 18, 1);

    /** The item of a resource row in this mode (null stays null: nothing). */
    public static String resourceItem(Mode m, String key) {
        if (key == null) {
            return null;
        }
        return switch (key) {
            case RES_SHARDS -> shards(m);
            case RES_GRYM -> grym(m);
            case RES_TELEPORT -> teleport(m);
            default -> key;
        };
    }

    /** Weak creatures drop a weapon frame 1 time in 4, always from the first one killed in a run (wiki, 22 July 2026). */
    public static final int WEAK_FRAME_CHANCE = 4;
    /** Strong creatures drop a weapon frame 2 times in 7, guaranteed by the second one killed without one (wiki). */
    public static final int STRONG_FRAME_NUM = 2;
    public static final int STRONG_FRAME_DEN = 7;

    /**
     * Whether a kill drops a weapon frame. {@code kill} counts this run's kills of the creature's tier, this one included;
     * {@code frameFromTier} says whether that tier has already dropped one. Demi-bosses always drop one.
     */
    public static boolean dropsFrame(Foe f, int kill, boolean frameFromTier, Random r) {
        return switch (f) {
            case WEAK -> kill <= 1 || r.nextInt(WEAK_FRAME_CHANCE) == 0;
            case STRONG -> (!frameFromTier && kill >= 2) || r.nextInt(STRONG_FRAME_DEN) < STRONG_FRAME_NUM;
            case DEMI -> true;
        };
    }

    /** The pestle and mortar crushes a spike, orb or bowstring into 80 shards (wiki Crystal shards, Creation). */
    public static final int COMPONENT_SHARDS = 80;

    /** The weapon piece whose perfecting component this item is, or null. */
    public static Piece pieceOfComponent(Mode m, String itemId) {
        for (Piece p : new Piece[]{Piece.HALBERD, Piece.STAFF, Piece.BOW}) {
            if (component(m, p).equals(itemId)) {
                return p;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ the Hunllef

    /**
     * The Hunllef's standard attacks' max hit without armour or the right protection prayer: the NPC formula with 240 Ranged
     * and Magic and +64 (crystalline) or +112 (corrupted) strength bonus, 50 and 68 (wiki infoboxes: "50+" and 68).
     */
    public static int hunllefUnprayedMax(Mode m) {
        return m == Mode.NORMAL ? 50 : 68;
    }

    /**
     * Damage of a standard attack that lands through the right protection prayer: the roll is scaled from the unprayed max to
     * the prayed one (12 / 16 without armour). Worn armour then lowers both by {@link #hunllefDamageScale} (unprayed with full
     * T1/T2/T3 corrupted: 55/42/34; wiki 55/45/35, approx).
     */
    public static int prayedDamage(Mode m, int rolled) {
        return (int) Math.round(rolled * hunllefMaxHit(m, 0) / (double) hunllefUnprayedMax(m));
    }

    /** Tornado damage per tick without armour (wiki): 10-20 crystalline, 15-30 corrupted; armour scales it like the attacks. */
    public static int[] tornadoDamage(Mode m) {
        return m == Mode.NORMAL ? new int[]{10, 20} : new int[]{15, 30};
    }

    /** Tornadoes last 20 ticks (wiki). */
    public static final int TORNADO_TICKS = 20;

    /**
     * Tornadoes summoned at once: one (crystalline) or two (corrupted) above 66% health, one more between 33% and 66%, and
     * another below 33% (wiki Tornado pages).
     */
    public static int tornadoes(Mode m, double healthPercent) {
        int n = m == Mode.NORMAL ? 1 : 2;
        if (healthPercent <= 66) {
            n++;
        }
        if (healthPercent <= 33) {
            n++;
        }
        return n;
    }

    /**
     * The damaging floor's phase by the Hunllef's health (wiki Corrupted Hunllef: 1000-667 slow, 666-333 medium, 332 and
     * below fast; both Hunllefs use the same mechanics): 0, 1 or 2.
     */
    public static int floorPhase(double healthPercent) {
        return healthPercent > 66.6 ? 0 : healthPercent > 33.3 ? 1 : 2;
    }

    /** Damage per tick on an orange floor tile (wiki: 10-20). */
    public static final int FLOOR_MIN = 10;
    public static final int FLOOR_MAX = 20;
    /** Game ticks a pattern of tiles stays blue (crimson) before turning orange, by phase (approx: slow, medium, fast). */
    public static final int[] FLOOR_WARN_TICKS = {5, 4, 2};
    /** Game ticks between floor patterns, by phase (approx). */
    public static final int[] FLOOR_INTERVAL_TICKS = {16, 12, 8};
    /** Game ticks a pattern stays orange (approx). */
    public static final int FLOOR_ACTIVE_TICKS = 4;

    /**
     * Creature stats (osrs-dps-calc monsters.json): level, hitpoints, attack, strength, defence, magic, ranged, max hit,
     * attack bonus, strength bonus. Defensive bonuses are all 0 since the July 2026 standardisation.
     */
    private record Stats(int level, int hp, int att, int str, int def, int mag, int rng, int max, int attBonus, int strBonus) {
    }

    private static Stats stats(Mode m, String kind) {
        boolean c = m == Mode.CORRUPTED;
        return switch (kind) {
            case "spider" -> c ? new Stats(32, 12, 48, 39, 4, 4, 0, 5, 0, 0) : new Stats(22, 12, 32, 26, 4, 4, 0, 4, 0, 0);
            case "rat" -> c ? new Stats(33, 12, 42, 48, 4, 6, 0, 14, 0, 0) : new Stats(23, 12, 28, 32, 4, 6, 0, 4, 0, 0);
            case "bat" -> c ? new Stats(48, 12, 36, 102, 4, 2, 0, 11, 0, 0) : new Stats(33, 12, 24, 68, 4, 2, 0, 8, 0, 0);
            case "unicorn" -> c ? new Stats(61, 38, 75, 72, 18, 24, 0, 17, 0, 0) : new Stats(45, 38, 50, 48, 18, 24, 0, 6, 0, 0);
            case "scorpion" -> c ? new Stats(89, 38, 72, 159, 18, 18, 0, 17, 0, 0) : new Stats(64, 38, 48, 106, 18, 18, 0, 11, 0, 0);
            case "wolf" -> c ? new Stats(97, 38, 159, 99, 18, 22, 0, 19, 0, 0) : new Stats(69, 38, 106, 66, 18, 22, 0, 8, 0, 0);
            // The corrupted dragon's magic bonuses are +146 attack / +226 damage, not the bear's and dark beast's +81 / +132.
            case "dragon" -> c ? new Stats(258, 100, 147, 147, 98, 98, 147, 48, 146, 226) : new Stats(172, 100, 98, 98, 98, 98, 98, 28, 54, 106);
            default -> c ? new Stats(258, 100, 147, 147, 98, 98, 147, 48, 81, 132) : new Stats(172, 100, 98, 98, 98, 98, 98, 28, 54, 106);
        };
    }

    private static String displayKind(String kind) {
        return kind.equals("dark_beast") ? "Dark Beast" : Character.toUpperCase(kind.charAt(0)) + kind.substring(1);
    }

    public static List<MonsterDef> monsters() {
        List<MonsterDef> list = new ArrayList<>();
        for (Mode m : Mode.values()) {
            int skin = m == Mode.NORMAL ? 0x9AD8F0 : 0xB03A2A;
            int dark = m == Mode.NORMAL ? 0x3E7AA8 : 0x5E1414;
            for (Foe f : Foe.values()) {
                for (String kind : f.kinds) {
                    Stats s = stats(m, kind);
                    AttackDef attack = switch (kind) {
                        case "spider", "unicorn" -> AttackDef.of("melee", AttackType.STAB, s.max(), 2.5, 10);
                        case "rat", "wolf" -> AttackDef.of("melee", AttackType.CRUSH, s.max(), 2.5, 10);
                        // Demi-bosses attack from up to 10 tiles away since July 2026.
                        case "dark_beast" -> AttackDef.of("ranged", AttackType.RANGED, s.max(), 10, 10).particle("minecraft:end_rod");
                        case "dragon" -> AttackDef.of("magic", AttackType.MAGIC, s.max(), 10, 10).particle("minecraft:glow");
                        default -> AttackDef.of("melee", AttackType.SLASH, s.max(), 2.5, 10);
                    };
                    float scale = f == Foe.DEMI ? 1.4f : f == Foe.STRONG ? 1.0f : 0.8f;
                    MonsterDef d = MonsterDef.of(creature(m, kind), (m == Mode.NORMAL ? "Crystalline " : "Corrupted ") + displayKind(kind))
                            .level(s.level()).stats(s.hp(), s.att(), s.str(), s.def(), s.mag(), Math.max(1, s.rng())).speed(4)
                            .defenceBonus(0, 0, 0, 0, 0).attack(attack).skin(skin, dark, dark, 0xFFFFFF)
                            .size(f == Foe.DEMI ? 1.4f : 0.9f, f == Foe.DEMI ? 1.8f : 1.0f, scale);
                    if (f == Foe.DEMI) {
                        AttackType style = kind.equals("bear") ? AttackType.SLASH : kind.equals("dragon") ? AttackType.MAGIC : AttackType.RANGED;
                        d.attackBonus(style == AttackType.SLASH ? s.attBonus() : 0, style == AttackType.MAGIC ? s.attBonus() : 0,
                                style == AttackType.RANGED ? s.attBonus() : 0);
                    }
                    list.add(d);
                }
            }
        }
        return list;
    }

    /** Existing creature models the Gauntlet's creatures copy, tinted crystal blue or corrupted red. */
    public static List<ModelSpec.Variant> modelVariants() {
        Map<String, String> base = Map.of("spider", "giant_spider", "rat", "giant_rat", "bat", "giant_bat", "unicorn", "big_wolf",
                "scorpion", "scorpion", "wolf", "wolf", "bear", "grizzly_bear", "dark_beast", "dark_beast", "dragon", "green_dragon");
        List<ModelSpec.Variant> list = new ArrayList<>();
        for (Mode m : Mode.values()) {
            int target = m == Mode.NORMAL ? 0x9AD8F0 : 0xB03A2A;
            for (Foe f : Foe.values()) {
                for (String kind : f.kinds) {
                    float scale = kind.equals("dragon") ? 0.8f : f == Foe.DEMI ? 1.3f : 1.0f;
                    list.add(new ModelSpec.Variant(creature(m, kind), base.get(kind), scale, rgb -> blend(rgb, target)));
                }
            }
        }
        return list;
    }

    private static int blend(int rgb, int target) {
        int lum = (((rgb >> 16) & 255) * 3 + ((rgb >> 8) & 255) * 6 + (rgb & 255)) / 10;
        double f = 0.45 + lum / 255.0 * 0.75;
        int r = Math.min(255, (int) (((target >> 16) & 255) * f));
        int g = Math.min(255, (int) (((target >> 8) & 255) * f));
        int b = Math.min(255, (int) ((target & 255) * f));
        return (r << 16) | (g << 8) | b;
    }

    // ------------------------------------------------------------------ the maze

    /** Grid size (7x7 rooms), the boss room at the centre and the starting room just south of it. */
    public static final int GRID = 7;
    public static final int BOSS = 3;
    public static final int START_X = 3;
    public static final int START_Y = 4;

    public enum RoomKind { BOSS, START, RESOURCE }

    /** One room: its nodes and the creatures (kinds) that spawn when it is first lit. */
    public record Room(int x, int y, RoomKind kind, List<Node> nodes, List<String> creatures) {
        public boolean edge() {
            return x == 0 || y == 0 || x == GRID - 1 || y == GRID - 1;
        }

        /** In the inner 3x3 around the boss room. */
        public boolean inner() {
            return Math.abs(x - BOSS) <= 1 && Math.abs(y - BOSS) <= 1;
        }
    }

    /**
     * Demi-bosses per run: six (wiki change notes, poll 71: "6 of the 12 specific rooms of the outer areas"); since 22 July 2026
     * one is guaranteed in each of the rooms furthest north/east/south/west of the centre and the others may be in any edge or
     * corner room.
     */
    public static final int DEMI_BOSSES = 6;

    /**
     * Generates a run's maze (wiki rules as of July 2026): the N/E/S/W-most rooms each hold a random demi-boss and the other
     * demi-bosses go to other edge or corner rooms (alone); in the normal Gauntlet four rooms of the inner 3x3 are guaranteed
     * at least 3 crystal deposits, 3 linum tirinum, 3 phren roots and 3 weak monsters respectively, and in the corrupted one a
     * single inner room is guaranteed 3 fishing spots. Other rooms get 0-5 random nodes and a few weak or strong monsters
     * (distribution approx).
     */
    public static Room[][] generate(Mode mode, Random r) {
        Room[][] grid = new Room[GRID][GRID];
        List<int[]> edges = new ArrayList<>();
        for (int x = 0; x < GRID; x++) {
            for (int y = 0; y < GRID; y++) {
                if ((x == 0 || y == 0 || x == GRID - 1 || y == GRID - 1) && !(x == BOSS || y == BOSS)) {
                    edges.add(new int[]{x, y});
                }
            }
        }
        List<String> demis = new ArrayList<>();
        for (int i = 0; i < DEMI_BOSSES; i++) {
            demis.add(Foe.DEMI.kinds.get(i % 3));
        }
        Collections.shuffle(demis, r);
        Collections.shuffle(edges, r);
        int[][] demiRooms = {{BOSS, 0}, {GRID - 1, BOSS}, {BOSS, GRID - 1}, {0, BOSS}, edges.get(0), edges.get(1)};
        for (int i = 0; i < demiRooms.length; i++) {
            int[] p = demiRooms[i];
            grid[p[0]][p[1]] = new Room(p[0], p[1], RoomKind.RESOURCE, randomNodes(r, r.nextInt(3)), List.of(demis.get(i)));
        }
        grid[BOSS][BOSS] = new Room(BOSS, BOSS, RoomKind.BOSS, List.of(), List.of());
        grid[START_X][START_Y] = new Room(START_X, START_Y, RoomKind.START, List.of(), List.of());
        List<int[]> inner = new ArrayList<>();
        for (int x = BOSS - 1; x <= BOSS + 1; x++) {
            for (int y = BOSS - 1; y <= BOSS + 1; y++) {
                if (grid[x][y] == null) {
                    inner.add(new int[]{x, y});
                }
            }
        }
        Collections.shuffle(inner, r);
        if (mode == Mode.NORMAL) {
            Node[] guaranteed = {Node.ORE, Node.LINUM, Node.BARK};
            for (int i = 0; i < 3; i++) {
                int[] p = inner.get(i);
                List<Node> nodes = new ArrayList<>(Collections.nCopies(3, guaranteed[i]));
                nodes.addAll(randomNodes(r, r.nextInt(3)));
                grid[p[0]][p[1]] = new Room(p[0], p[1], RoomKind.RESOURCE, nodes, randomCreatures(r));
            }
            int[] p = inner.get(3);
            List<String> weak = new ArrayList<>();
            for (int i = 0; i < 3 + r.nextInt(2); i++) {
                weak.add(Foe.WEAK.kinds.get(r.nextInt(3)));
            }
            grid[p[0]][p[1]] = new Room(p[0], p[1], RoomKind.RESOURCE, randomNodes(r, r.nextInt(4)), weak);
        } else {
            int[] p = inner.get(0);
            List<Node> nodes = new ArrayList<>(Collections.nCopies(3, Node.FISH));
            nodes.addAll(randomNodes(r, r.nextInt(3)));
            grid[p[0]][p[1]] = new Room(p[0], p[1], RoomKind.RESOURCE, nodes, randomCreatures(r));
        }
        for (int x = 0; x < GRID; x++) {
            for (int y = 0; y < GRID; y++) {
                if (grid[x][y] == null) {
                    grid[x][y] = new Room(x, y, RoomKind.RESOURCE, randomNodes(r, r.nextInt(6)), randomCreatures(r));
                }
            }
        }
        return grid;
    }

    private static List<Node> randomNodes(Random r, int n) {
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            nodes.add(Node.values()[r.nextInt(Node.values().length)]);
        }
        return nodes;
    }

    /** approx: a third of rooms are empty, a third hold 1-3 weak monsters and a third 1-2 strong ones. */
    private static List<String> randomCreatures(Random r) {
        List<String> list = new ArrayList<>();
        int roll = r.nextInt(3);
        if (roll == 1) {
            for (int i = 0, n = 1 + r.nextInt(3); i < n; i++) {
                list.add(Foe.WEAK.kinds.get(r.nextInt(3)));
            }
        } else if (roll == 2) {
            for (int i = 0, n = 1 + r.nextInt(2); i < n; i++) {
                list.add(Foe.STRONG.kinds.get(r.nextInt(3)));
            }
        }
        return list;
    }
}
