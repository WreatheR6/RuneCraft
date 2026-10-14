package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ItemSink;

import java.util.List;
import java.util.function.Consumer;

/**
 * Aerial fishing at Lake Molch (OSRS Wiki): a cormorant (cormorant's glove worn, both hands free) catches a fish
 * every time it is sent, for a king worm or, as a reward every 3-4 catches, a fish offcut. The fish depends on
 * X = (2 x Fishing + Hunter) / 3: a random number 0..X-1 of 82+ gives a greater siren, 67+ a mottled eel, 52+ a
 * common tench, otherwise a bluegill, each only with its Fishing and Hunter levels. Each catch also rolls molch
 * pearls at 1 / (100 - (X - 40) x 25 / 59) and a golden tench at 1/20,000. Fish are cut with a knife into offcuts.
 */
public final class AerialFishing extends SkillingPart {
    public static final String SPOT = "aerial_fishing_spot";
    public static final String GLOVE = "cormorants_glove";
    public static final String OFFCUTS = "fish_offcuts";
    public static final String PEARL = "molch_pearl";
    public static final String GOLDEN_TENCH = "golden_tench";
    public static final String KING_WORM = "king_worm";
    public static final int GOLDEN_TENCH_CHANCE = 20_000;
    /** The cormorant takes one offcut every 3-4 catches (2 in 7 per catch). // approx */
    public static final double OFFCUT_USE_CHANCE = 2.0 / 7.0;
    /** Fishing and Hunter needed to be lent a glove and to catch anything (bluegill). */
    public static final int FISHING_LEVEL = 43;
    public static final int HUNTER_LEVEL = 35;

    /** A catch: Fishing level/XP, Hunter level/XP, Cooking XP for cutting it, and the roll it needs (0..X-1). */
    public record Fish(String item, String name, int fishing, double fishingXp, int hunter, double hunterXp, double cookingXp,
                       int threshold) {
    }

    /** Highest first, as the catch is decided. */
    public static final List<Fish> FISH = List.of(
            new Fish("greater_siren", "Greater siren", 91, 100.0, 87, 130.0, 25.0, 82),
            new Fish("mottled_eel", "Mottled eel", 73, 65.0, 68, 90.0, 20.0, 67),
            new Fish("common_tench", "Common tench", 56, 40.0, 51, 45.0, 10.0, 52),
            new Fish("bluegill", "Bluegill", 43, 11.5, 35, 16.5, 3.5, 0));

    /** X = (Fishing x 2 + Hunter) / 3 (visible levels, rounded down). */
    public static int x(int fishing, int hunter) {
        return (fishing * 2 + hunter) / 3;
    }

    /** The fish caught for a random number {@code roll} in 0..X-1. */
    public static Fish catchFor(int fishing, int hunter, int roll) {
        for (Fish f : FISH) {
            if (roll >= f.threshold() && fishing >= f.fishing() && hunter >= f.hunter()) {
                return f;
            }
        }
        return FISH.get(FISH.size() - 1);
    }

    /** Chance of molch pearls with a catch: 1 / (100 - (X - 40) x 25 / 59), divided in one step. */
    public static double pearlChance(int fishing, int hunter) {
        return 1.0 / (100.0 - (x(fishing, hunter) - 40) * 25.0 / 59.0);
    }

    public static Fish fish(String item) {
        for (Fish f : FISH) {
            if (f.item().equals(item)) {
                return f;
            }
        }
        return null;
    }

    @Override
    public void items(ItemSink s) {
        int[] colors = {0x4A8A9A, 0x8A8A4A, 0x6A5A3A, 0x3A6A8A};
        for (int i = 0; i < FISH.size(); i++) {
            Fish f = FISH.get(i);
            simple(s, f.item(), f.name(), "fish_raw", colors[i], 1).examine("A fish from Lake Molch. Cut it up for offcuts.");
        }
        simple(s, OFFCUTS, "Fish offcuts", "bait", 0xB07060, 1).stack(Integer.MAX_VALUE).examine("Cormorants love these.");
        simple(s, PEARL, "Molch pearl", "gem", 0xE8E0F0, 1).stack(Integer.MAX_VALUE)
                .examine("A pearl from Lake Molch. Alry the Angler collects these.");
        simple(s, GOLDEN_TENCH, "Golden tench", "fish_raw", 0xE8C030, 1).stack(1).examine("A rare and beautiful fish.");
        simple(s, GLOVE, "Cormorant's glove", "gloves", 0x6A5A4A, 1).stack(1)
                .examine("A heavy glove for a cormorant to perch on.");
    }

    @Override
    public void recipes(Consumer<Recipe> out) {
        // Cutting a Molch fish with a knife (the knife opens the fletching menu here).
        for (Fish f : FISH) {
            out.accept(r("cut_" + f.item(), MenuType.FLETCHING, "Fish offcuts (" + lower(f.name()) + ")", Skill.COOKING, 1,
                    f.cookingXp()).in(f.item(), 1).out(OFFCUTS, 1).tool("knife").ticks(2).build());
        }
    }

    @Override
    public List<BlockDef> blocks() {
        return List.of(BlockDef.scripted(SPOT, "Fishing spot (aerial)", 0x3A6AB0, "fishing_spot"));
    }
}
