package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.EquipmentDef.Slot;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Rune;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.data.RuneAltar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Runecraft: talismans, tiaras (bound at the matching altar), combination runes (mist, dust, mud,
 * smoke, steam, lava: talisman + runes + pure essence at an elemental altar, 50% success without a
 * worn binding necklace, which lasts 16 trips), essence pouches and the Ourania (ZMI) altar.
 * Levels/XP are OSRS values.
 */
public final class RunecraftData extends SkillingPart {

    /** A way to craft a combination rune: at {@code altar} using {@code talisman} and one {@code rune} per essence. */
    public record Combination(String id, String name, int level, RuneAltar altar, String talisman, String rune, double xp,
                              int color) {
    }

    public static final List<Combination> COMBINATIONS = List.of(
            new Combination("mist_rune", "Mist rune", 6, RuneAltar.AIR, "water_talisman", "water_rune", 8.0, 0xB0C0E0),
            new Combination("mist_rune", "Mist rune", 6, RuneAltar.WATER, "air_talisman", "air_rune", 8.5, 0xB0C0E0),
            new Combination("dust_rune", "Dust rune", 10, RuneAltar.AIR, "earth_talisman", "earth_rune", 8.3, 0xC0B090),
            new Combination("dust_rune", "Dust rune", 10, RuneAltar.EARTH, "air_talisman", "air_rune", 9.0, 0xC0B090),
            new Combination("mud_rune", "Mud rune", 13, RuneAltar.WATER, "earth_talisman", "earth_rune", 9.3, 0x6A5A3A),
            new Combination("mud_rune", "Mud rune", 13, RuneAltar.EARTH, "water_talisman", "water_rune", 9.5, 0x6A5A3A),
            new Combination("smoke_rune", "Smoke rune", 15, RuneAltar.AIR, "fire_talisman", "fire_rune", 8.5, 0x8A8A8A),
            new Combination("smoke_rune", "Smoke rune", 15, RuneAltar.FIRE, "air_talisman", "air_rune", 9.5, 0x8A8A8A),
            new Combination("steam_rune", "Steam rune", 19, RuneAltar.WATER, "fire_talisman", "fire_rune", 9.5, 0xD0D0E0),
            new Combination("steam_rune", "Steam rune", 19, RuneAltar.FIRE, "water_talisman", "water_rune", 10.0, 0xD0D0E0),
            new Combination("lava_rune", "Lava rune", 23, RuneAltar.EARTH, "fire_talisman", "fire_rune", 10.0, 0xE06020),
            new Combination("lava_rune", "Lava rune", 23, RuneAltar.FIRE, "earth_talisman", "earth_rune", 10.5, 0xE06020));

    /** A binding necklace guarantees combination runes for 16 altar clicks (per-player charges, Jewellery.chargePool; the NBT key is legacy). */
    public static final int BINDING_CHARGES = 16;
    public static final String BINDING_NBT = "BindingCharges";

    /** Tiara binding XP per altar (OSRS). */
    public static double tiaraXp(RuneAltar a) {
        return switch (a) {
            case AIR -> 25.0;
            case MIND -> 27.5;
            case WATER -> 30.0;
            case EARTH -> 32.5;
            case FIRE -> 35.0;
            case BODY -> 37.5;
            case COSMIC -> 40.0;
            case CHAOS -> 42.5;
            case ASTRAL -> 43.5;
            case NATURE -> 45.0;
            case LAW -> 47.5;
            case DEATH -> 50.0;
            case WRATH -> 52.5;
            case BLOOD -> 52.5;
            case SOUL -> 55.0;
        };
    }

    public static String talisman(RuneAltar a) {
        return a.rune().name().toLowerCase(Locale.ROOT) + "_talisman";
    }

    public static String tiara(RuneAltar a) {
        return a.rune().name().toLowerCase(Locale.ROOT) + "_tiara";
    }

    /** Essence pouches: id, Runecraft level, capacity. */
    public record Pouch(String id, String name, int level, int capacity) {
    }

    public static final List<Pouch> POUCHES = List.of(
            new Pouch("small_pouch", "Small pouch", 1, 3), new Pouch("medium_pouch", "Medium pouch", 25, 6),
            new Pouch("large_pouch", "Large pouch", 50, 9), new Pouch("giant_pouch", "Giant pouch", 75, 12),
            new Pouch("colossal_pouch", "Colossal pouch", 85, 40));

    /**
     * Pouch decay (OSRS Wiki): pouches degrade by the essence stored in them since their last repair. The small
     * pouch never degrades; the medium holds 3 after 270 essence; the large 7 after 261, then 5 and 3; the giant
     * loses 3 after 130 and one more every couple of fills. Each stage is {essence stored, capacity}. Approx: the
     * later large/giant stages and the colossal pouch's thresholds are estimates, and degraded pouches stop at
     * their last stage instead of disintegrating. The Dark Mage (NPC Contact) repairs them for free.
     */
    public static int[][] decayStages(String pouchId) {
        return switch (pouchId) {
            case "medium_pouch" -> new int[][]{{270, 3}};
            case "large_pouch" -> new int[][]{{261, 7}, {324, 5}, {369, 3}};
            case "giant_pouch" -> new int[][]{{130, 9}, {155, 8}, {180, 7}, {205, 6}, {230, 5}, {255, 4}, {280, 3}};
            case "colossal_pouch" -> new int[][]{{1020, 35}, {1120, 30}, {1220, 25}, {1320, 20}};
            default -> new int[0][];
        };
    }

    /** Current capacity of a pouch that has stored {@code used} essence since its last repair. */
    public static int capacity(Pouch p, int used) {
        int cap = p.capacity();
        for (int[] stage : decayStages(p.id())) {
            if (used >= stage[0]) {
                cap = stage[1];
            }
        }
        return cap;
    }

    /** True once the pouch has started to decay. */
    public static boolean degraded(Pouch p, int used) {
        return capacity(p, used) < p.capacity();
    }

    public static Pouch pouch(String id) {
        for (Pouch p : POUCHES) {
            if (p.id().equals(id)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Aether runes (OSRS Wiki): 90 Runecraft, at the Cosmic Altar with Magic Imbue, one soul rune and one aether catalyst per
     * rune; 20 XP each (30 with daeyalt essence). 50% per essence without a binding necklace; a failed rune loses its essence
     * and soul rune but not the catalyst.
     */
    public static final int AETHER_LEVEL = 90;
    public static final double AETHER_XP = 20.0;
    public static final String AETHER_RUNE = "aether_rune";
    public static final String AETHER_CATALYST = "aether_catalyst";

    /**
     * Daeyalt essence (OSRS Wiki, Daeyalt essence mine): 60 Mining, 5 XP and 2-3 daeyalt shards per successful roll on the
     * active essence rock (one of three, switching about every 60 seconds); Noranna Tytanin trades each shard for one
     * daeyalt essence, which crafts like pure essence for 50% more Runecraft XP and is used before pure essence.
     */
    public static final int DAEYALT_MINING_LEVEL = 60;
    public static final double DAEYALT_MINING_XP = 5.0;
    public static final int DAEYALT_SHARDS_MIN = 2;
    public static final int DAEYALT_SHARDS_MAX = 3;
    public static final double DAEYALT_XP_MULTIPLIER = 1.5;
    public static final int DAEYALT_ROCKS = 3;
    /** Game ticks between switches of the active daeyalt rock ("roughly every 60 seconds"). */
    public static final int DAEYALT_SWITCH_TICKS = 100;

    /** Runecraft XP for binding {@code essence} at an altar giving {@code xpPerEssence}, daeyalt or not. */
    public static double essenceXp(double xpPerEssence, int essence, boolean daeyalt) {
        return xpPerEssence * essence * (daeyalt ? DAEYALT_XP_MULTIPLIER : 1.0);
    }

    /** Ourania altar XP multiplier over the normal altar (approximation of the ZMI XP table). */
    public static final double OURANIA_MULTIPLIER = 1.7;

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

    @Override
    public void items(ItemSink s) {
        simple(s, "daeyalt_shard", "Daeyalt shard", "shard", 0x9A90A8, 1).stack(Integer.MAX_VALUE)
                .examine("A shard of daeyalt essence. Noranna Tytanin will trade them for daeyalt essence.");
        simple(s, "daeyalt_essence", "Daeyalt essence", "essence", 0x8A8098, 4)
                .examine("Imbued with the power of the Stone of Jas: worth half again as much experience as pure essence.");
        for (RuneAltar a : RuneAltar.values()) {
            String name = a.rune().displayName().replace(" rune", "");
            if (a != RuneAltar.AIR) {   // air talisman is built in
                simple(s, talisman(a), name + " talisman", "talisman", runeColor(a.rune()), 4 + a.level()).stack(1);
            }
            s.add(tiara(a), name + " tiara", Kind.ARMOUR, "tiara", runeColor(a.rune())).value(100).stack(1).tier("tiara")
                    .equipment(wear(Slot.HEAD, stats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
        }
        List<String> done = new ArrayList<>();
        for (Combination c : COMBINATIONS) {
            if (!done.contains(c.id())) {
                done.add(c.id());
                s.add(c.id(), c.name(), Kind.RUNE, "rune", c.color()).value(10 + c.level());
            }
        }
        for (Pouch p : POUCHES) {
            simple(s, p.id(), p.name(), "pouch", 0x8A6A4A, 25 + p.level() * 40).stack(1)
                    .examine("Can hold " + p.capacity() + " pure essence.");
        }
    }

    @Override
    public void recipes(Consumer<Recipe> out) {
        // Pouches are dropped by Abyss creatures in-game; the rune shop sells them here.
        int[] prices = {25, 5000, 25000, 100000, 500000};
        for (int i = 0; i < POUCHES.size(); i++) {
            Pouch p = POUCHES.get(i);
            out.accept(plain("shop_runes_" + p.id(), MenuType.SHOP_RUNES, p.name() + " - " + String.format(Locale.ROOT, "%,d", prices[i]) + " gp")
                    .in("coins", prices[i]).out(p.id(), 1).ticks(0).build());
        }
        out.accept(plain("shop_runes_battlestaff", MenuType.SHOP_RUNES, "Battlestaff - 7,000 gp").in("coins", 7000)
                .out("battlestaff", 1).ticks(0).build());
        for (RuneAltar a : new RuneAltar[]{RuneAltar.WATER, RuneAltar.EARTH, RuneAltar.FIRE, RuneAltar.MIND, RuneAltar.BODY}) {
            out.accept(plain("shop_runes_" + talisman(a), MenuType.SHOP_RUNES, a.rune().displayName().replace(" rune", "")
                    + " talisman - 200 gp").in("coins", 200).out(talisman(a), 1).ticks(0).build());
        }
    }

    @Override
    public List<BlockDef> blocks() {
        return List.of(BlockDef.scripted("ourania_altar", "Ourania altar", 0x6A4A8A, "altar"),
                BlockDef.scripted(DAEYALT_ROCK, "Daeyalt essence", 0x9A90A8, "ore"));
    }

    /** The daeyalt essence rock block and Noranna Tytanin, who trades daeyalt shards for essence. */
    public static final String DAEYALT_ROCK = "daeyalt_essence_rock";
    public static final String NORANNA = "noranna_tytanin";

    /**
     * Whether a daeyalt rock is the active one: each of the three is active for one {@link #DAEYALT_SWITCH_TICKS} period in
     * three, its turn set by {@code phase} (approx: OSRS picks the next rock at random).
     */
    public static boolean daeyaltActive(long gameTick, int phase) {
        return Math.floorMod(gameTick / DAEYALT_SWITCH_TICKS + phase, DAEYALT_ROCKS) == 0;
    }

    /**
     * A daeyalt rock's turn ({@link #daeyaltActive} phase) from its block position. Darkmeyer's three rocks stand 4 blocks
     * apart in a row along x, so they get the three different phases.
     */
    public static int daeyaltPhase(int blockX, int blockZ) {
        return Math.floorMod(blockX + blockZ, DAEYALT_ROCKS);
    }

    /** The Dark Mage, who repairs essence pouches (talk to him, or reach him with NPC Contact). */
    public static final String DARK_MAGE = "dark_mage";

    @Override
    public List<PlaceDef> places() {
        // The Ourania altar stands inside the dungeons module's Ourania Cave (its ZMI chamber); this place only marks the
        // cave's entrance on the surface.
        return List.of(PlaceDef.of("ourania_cave", "Ourania Cave", 2453, 3231, PlaceDef.Style.NONE, List.of()),
                // Approx: in OSRS he stands in the centre of the Abyss (NPCs aren't placed underground here), so he waits a
                // little west of the Abyss shaft by the Mage of Zamorak's spot.
                PlaceDef.of("abyss_dark_mage", "Dark Mage", 3094, 3557, PlaceDef.Style.NONE, List.of()),
                // The daeyalt essence mine lies beneath Darkmeyer (entrance stairs at 3632,3340, OSRS Wiki); approx: its three
                // essence rocks are part of the Darkmeyer structure, in a yard between the south-east houses, and this
                // place only puts Noranna there.
                PlaceDef.of("daeyalt_essence_mine", "Daeyalt essence mine", 3636, 3347, PlaceDef.Style.NONE, List.of()));
    }

    @Override
    public List<NpcDef> npcs() {
        return List.of(NpcDef.of(DARK_MAGE, "Dark Mage", "abyss_dark_mage", 0, 0, 0xC8A080, 0x2A1A3A, 0x1A1A2A, 0x2A1A3A),
                NpcDef.of(NORANNA, "Noranna Tytanin", "daeyalt_essence_mine", 6, 8, 0xD8B090, 0x6A3A3A, 0x3A2A2A, 0x6A3A3A));
    }
}
