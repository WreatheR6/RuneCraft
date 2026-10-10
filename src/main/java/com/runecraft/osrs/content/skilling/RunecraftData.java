package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.EquipmentDef.Slot;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Rune;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.data.RuneAltar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Runecraft: talismans, tiaras (bound at the matching altar), combination runes (mist, dust, mud,
 * smoke, steam, lava: talisman + runes + pure essence at an elemental altar, 50% success without a
 * binding necklace), essence pouches and the Ourania (ZMI) altar. Levels/XP are OSRS values.
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

    public static Pouch pouch(String id) {
        for (Pouch p : POUCHES) {
            if (p.id().equals(id)) {
                return p;
            }
        }
        return null;
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
        return List.of(BlockDef.scripted("ourania_altar", "Ourania altar", 0x6A4A8A, "altar"));
    }

    @Override
    public List<PlaceDef> places() {
        return List.of(PlaceDef.of("ourania_cave", "Ourania Cave", 2453, 3231, PlaceDef.Style.CAVE, List.of("ourania_altar")));
    }
}
