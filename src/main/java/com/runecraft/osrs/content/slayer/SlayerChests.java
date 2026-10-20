package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.data.Drop;

import java.util.List;
import java.util.Random;

/**
 * Konar's brimstone keys and Krystilia's Larran's keys: drop chances and chest loot (OSRS Wiki "Brimstone chest" and
 * "Larran's big chest", July 2026; snapshot in {@code src/test/resources/osrs/slayer/slayer_chests.txt}). Opening a chest
 * gives one roll on the unique pre-roll ({@code *_RARE}: mutually exclusive lines, each {@code rarity} = 1 in N per
 * opening, usually nothing) and one weighted roll on the main table ({@code *_MAIN}: {@code rarity} is the weight out of
 * 60). The main table's {@link #FISH} line (3/60) is the fish table: raw fish chosen by the opener's Fishing level
 * ({@link #fish}), the Larran's chest's quantities 50% higher. Main-table loot comes noted.
 */
public final class SlayerChests {
    private SlayerChests() {
    }

    /** The main-table line that rolls the fish table instead of an item. */
    public static final String FISH = "#fish";
    /** Total weight of each chest's main table. */
    public static final int MAIN_TOTAL = 60;

    /** Brimstone key chance (1 in N) for a Konar task kill of the given combat level. */
    public static int brimstoneKeyChance(int combatLevel) {
        if (combatLevel < 100) {
            return (int) Math.round(0.2 * (combatLevel - 100) * (combatLevel - 100) + 100);
        }
        return (int) Math.max(50, Math.round(-0.2 * combatLevel + 120));
    }

    /** Larran's key chance (1 in N) for a Krystilia task kill of the given combat level. */
    public static int larranKeyChance(int combatLevel) {
        if (combatLevel < 81) {
            return (int) Math.round((3.0 / 10.0) * (80 - combatLevel) * (80 - combatLevel) + 100);
        }
        return (int) Math.max(50, Math.round(-0.18 * combatLevel + 114.58));
    }

    public static final List<Drop> BRIMSTONE_MAIN = List.of(
            Drop.of("uncut_diamond", 25, 35, 5),
            Drop.of("uncut_ruby", 25, 35, 5),
            Drop.of("coal", 300, 500, 5),
            Drop.of("coins", 50000, 150000, 5),
            Drop.of("gold_ore", 100, 200, 4),
            Drop.of("dragon_arrowtips", 50, 200, 4),
            Drop.of("iron_ore", 350, 500, 3),
            Drop.of("rune_full_helm", 2, 4, 3),
            Drop.of("rune_platebody", 1, 2, 3),
            Drop.of("rune_platelegs", 1, 2, 3),
            Drop.of(FISH, 1, 1, 3),
            Drop.of("runite_ore", 10, 15, 2),
            Drop.of("steel_bar", 300, 500, 2),
            Drop.of("magic_logs", 120, 160, 2),
            Drop.of("dragon_dart_tip", 40, 160, 2),
            Drop.of("palm_seed", 2, 4, 1),
            Drop.of("magic_seed", 2, 3, 1),
            Drop.of("celastrus_seed", 2, 4, 1),
            Drop.of("dragonfruit_seed", 2, 4, 1),
            Drop.of("redwood_tree_seed", 1, 1, 1),
            Drop.of("torstol_seed", 3, 5, 1),
            Drop.of("snapdragon_seed", 3, 5, 1),
            Drop.of("ranarr_seed", 3, 5, 1),
            Drop.of("pure_essence", 3000, 6000, 1));
    /** The brimstone chest's pre-roll: 1/100 in all (broken dragon hasta 1/200, each dusk mystic piece 1/1,000). */
    public static final List<Drop> BRIMSTONE_RARE = List.of(
            Drop.of("broken_dragon_hasta", 1, 1, 200),
            Drop.of("mystic_hat_dusk", 1, 1, 1000),
            Drop.of("mystic_robe_top_dusk", 1, 1, 1000),
            Drop.of("mystic_robe_bottom_dusk", 1, 1, 1000),
            Drop.of("mystic_gloves_dusk", 1, 1, 1000),
            Drop.of("mystic_boots_dusk", 1, 1, 1000));

    public static final List<Drop> LARRAN_MAIN = List.of(
            Drop.of("uncut_diamond", 35, 45, 5),
            Drop.of("uncut_ruby", 35, 45, 5),
            Drop.of("coal", 450, 650, 5),
            Drop.of("gold_ore", 150, 250, 4),
            Drop.of("dragon_arrowtips", 100, 250, 4),
            Drop.of("coins", 75000, 175000, 3),
            Drop.of("iron_ore", 500, 650, 3),
            Drop.of("rune_full_helm", 3, 5, 3),
            Drop.of("rune_platebody", 2, 3, 3),
            Drop.of("rune_platelegs", 2, 3, 3),
            Drop.of("pure_essence", 4500, 7500, 3),
            Drop.of(FISH, 1, 1, 3),
            Drop.of("runite_ore", 15, 20, 2),
            Drop.of("steel_bar", 350, 550, 2),
            Drop.of("magic_logs", 180, 220, 2),
            Drop.of("dragon_dart_tip", 80, 200, 2),
            Drop.of("palm_seed", 3, 5, 1),
            Drop.of("magic_seed", 3, 4, 1),
            Drop.of("celastrus_seed", 3, 5, 1),
            Drop.of("dragonfruit_seed", 3, 5, 1),
            Drop.of("redwood_tree_seed", 1, 1, 1),
            Drop.of("torstol_seed", 4, 6, 1),
            Drop.of("snapdragon_seed", 4, 6, 1),
            Drop.of("ranarr_seed", 4, 6, 1));
    /** Larran's big chest pre-roll: each Dagon'hai robe piece 1/256. */
    public static final List<Drop> LARRAN_RARE = List.of(
            Drop.of("dagonhai_hat", 1, 1, 256),
            Drop.of("dagonhai_robe_top", 1, 1, 256),
            Drop.of("dagonhai_robe_bottom", 1, 1, 256));

    /** The pre-roll's prize (mutually exclusive lines, each 1 in {@code rarity}), or null for none. */
    public static Drop preRoll(List<Drop> rares, Random random) {
        double roll = random.nextDouble();
        for (Drop d : rares) {
            roll -= 1.0 / d.rarity();
            if (roll < 0) {
                return d;
            }
        }
        return null;
    }

    /** One weighted roll on a main table. */
    public static Drop mainRoll(List<Drop> main, Random random) {
        int total = 0;
        for (Drop d : main) {
            total += d.rarity();
        }
        int roll = random.nextInt(Math.max(1, total));
        for (Drop d : main) {
            roll -= d.rarity();
            if (roll < 0) {
                return d;
            }
        }
        return main.get(main.size() - 1);
    }

    /**
     * The OSRS skilling-curve chance used by the fish table: {@code (1 + floor(low*(99-L)/98 + high*(L-1)/98 + 0.5)) / 256},
     * clamped to 0..1 (Module:Skilling success chart).
     */
    public static double interp(int low, int high, int level) {
        double value = Math.floor(low * (99.0 - level) / 98.0 + high * (level - 1.0) / 98.0 + 0.5) + 1;
        return Math.min(Math.max(value / 256.0, 0), 1);
    }

    /** One fish-table prize: item id and quantity. */
    public record Fish(String item, int count) {
    }

    /**
     * The fish table (Module:Slayer chest fish chart): sequential rolls from raw manta ray, sea turtle and shark down to
     * monkfish, swordfish and tuna by the opener's Fishing level, raw lobster when all fail; a manta, turtle or shark roll
     * gives that fish 2/8, raw sharks 3/8 or shark lures 3/8 (RuneCraft has no shark lure: raw sharks instead, approx).
     * Quantities are the brimstone chest's, x1.5 (rounded down) for Larran's chest.
     */
    public static Fish fish(int fishingLevel, boolean larran, Random random) {
        String item;
        int lo;
        int hi;
        if (random.nextDouble() < interp(-10, 20, fishingLevel)) {
            item = "raw_manta_ray";
        } else if (random.nextDouble() < interp(-10, 50, fishingLevel)) {
            item = "raw_sea_turtle";
        } else if (random.nextDouble() < interp(-60, 40, fishingLevel)) {
            item = "raw_shark";
        } else if (random.nextDouble() < interp(0, 170, fishingLevel)) {
            item = "raw_monkfish";
        } else if (random.nextDouble() < interp(25, 200, fishingLevel)) {
            item = "raw_swordfish";
        } else if (random.nextDouble() < interp(160, 0, fishingLevel)) {
            item = "raw_tuna";
        } else {
            item = "raw_lobster";
        }
        if (item.equals("raw_manta_ray") || item.equals("raw_sea_turtle") || item.equals("raw_shark")) {
            // The subtable: the fish itself 2/8, else raw sharks (3/8, and the 3/8 shark lure, approx).
            if (random.nextInt(8) >= 2) {
                item = "raw_shark";
            }
        }
        switch (item) {
            case "raw_tuna", "raw_lobster" -> {
                lo = 100;
                hi = 350;
            }
            case "raw_swordfish", "raw_monkfish" -> {
                lo = 100;
                hi = 300;
            }
            case "raw_shark" -> {
                lo = 80;
                hi = 250;
            }
            case "raw_sea_turtle" -> {
                lo = 80;
                hi = 200;
            }
            default -> {
                lo = 80;
                hi = 160;
            }
        }
        int qty = lo + random.nextInt(hi - lo + 1);
        return new Fish(item, larran ? qty * 3 / 2 : qty);
    }
}
