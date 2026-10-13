package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.data.Drop;

import java.util.List;

/**
 * Konar's brimstone keys and Krystilia's Larran's keys: drop chances and chest loot. Loot is one
 * weighted roll on {@code main} (Drop.rarity is the weight) plus each rare rolled independently.
 * Values approximate the OSRS wiki tables.
 */
public final class SlayerChests {
    private SlayerChests() {
    }

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
            Drop.of("coins", 50000, 150000, 5),
            Drop.of("uncut_diamond", 25, 35, 5),
            Drop.of("uncut_ruby", 25, 35, 5),
            Drop.of("coal", 300, 500, 5),
            Drop.of("gold_ore", 100, 200, 5),
            Drop.of("rune_arrowtips", 50, 200, 4),
            Drop.of("iron_ore", 350, 500, 5),
            Drop.of("runite_ore", 10, 15, 4),
            Drop.of("adamantite_ore", 80, 140, 4),
            Drop.of("raw_shark", 100, 200, 4),
            Drop.of("raw_swordfish", 100, 300, 4),
            Drop.of("raw_monkfish", 100, 300, 4),
            Drop.of("silver_ore", 200, 400, 4),
            Drop.of("pure_essence", 500, 1000, 5),
            Drop.of("dragon_dagger", 1, 1, 2));
    public static final List<Drop> BRIMSTONE_RARE = List.of(
            Drop.of("dragon_harpoon", 1, 1, 200),
            Drop.of("dragon_sword", 1, 1, 200),
            Drop.of("abyssal_dagger", 1, 1, 1000),
            // Dusk mystic robes (CommonEquipment): 1/1,000 each (OSRS Wiki).
            Drop.of("mystic_hat_dusk", 1, 1, 1000),
            Drop.of("mystic_robe_top_dusk", 1, 1, 1000),
            Drop.of("mystic_robe_bottom_dusk", 1, 1, 1000),
            Drop.of("mystic_gloves_dusk", 1, 1, 1000),
            Drop.of("mystic_boots_dusk", 1, 1, 1000));

    public static final List<Drop> LARRAN_MAIN = List.of(
            Drop.of("coins", 75000, 175000, 5),
            Drop.of("uncut_diamond", 35, 45, 5),
            Drop.of("uncut_ruby", 35, 45, 5),
            Drop.of("coal", 450, 650, 5),
            Drop.of("gold_ore", 150, 250, 5),
            Drop.of("iron_ore", 500, 650, 5),
            Drop.of("runite_ore", 15, 20, 4),
            Drop.of("adamantite_ore", 100, 160, 4),
            Drop.of("raw_dark_crab", 100, 150, 4),
            Drop.of("raw_shark", 150, 250, 4),
            Drop.of("raw_monkfish", 150, 350, 4),
            Drop.of("pure_essence", 700, 1200, 5),
            Drop.of("dragon_dagger", 1, 1, 2));
    public static final List<Drop> LARRAN_RARE = List.of(
            Drop.of("dragon_boots", 1, 1, 400),
            Drop.of("dark_bow", 1, 1, 400),
            Drop.of("occult_necklace", 1, 1, 1000));
}
