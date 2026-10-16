package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.PlaceDef;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Drift net fishing (OSRS Wiki "Drift net fishing", "Fish shoal", July 2026 dump): 47 Fishing and 44 Hunter. A drift net
 * is set on one of two drift net anchors; fish shoals swim into it slowly by themselves, or faster when chased. Each shoal
 * caught gives Fishing and Hunter XP that scale with level up to 70; a net holds 10 shoals and is destroyed when
 * harvested, giving one loot roll per shoal: 1/25 the fossil table, else 1/600 a medium clue, else one fish from the
 * level-based table (all equally likely, a new fish every 10 levels from 50).
 *
 * <p>Approx: the underwater area (Ceto's daily numulite fee, oxygen, Annette's net storage) isn't built; the anchors and
 * shoals stand on Fossil Island's north coast next to the underwater seaweed stand-in, and the shoals are fishing-spot
 * blocks that are chased in place. The passive catch rate and the chase success chance are guesses (the wiki gives none).
 */
public final class DriftNetFishing extends SkillingPart {
    public static final String NET = "drift_net";
    public static final String ANCHOR = "drift_net_anchor";
    public static final String SHOAL = "fish_shoal";
    public static final String PUFFERFISH = "pufferfish";
    public static final String PLACE = "fossil_drift_net_fishing";
    public static final int PLACE_X = 3716;
    public static final int PLACE_Y = 3893;

    public static final int FISHING_LEVEL = 47;
    public static final int HUNTER_LEVEL = 44;
    /** Shoals a drift net holds before it is full. */
    public static final int CAPACITY = 10;
    /** XP per shoal stops growing at level 70 in both skills. */
    public static final int XP_CAP_LEVEL = 70;
    /** Game ticks per chase attempt. // approx */
    public static final int CHASE_TICKS = 3;
    /** Chance a chase scares the shoal into a net; a merfolk trident, trident of the seas/swamp or dragon harpoon helps. // approx */
    public static final double CHASE_CHANCE = 0.45;
    public static final double CHASE_CHANCE_TOOL = 0.65;
    /** A set net catches a shoal by itself about once a minute ("a slow rate"). // approx */
    public static final int PASSIVE_TICKS = 100;
    /** Pre-rolls per shoal: 1/25 the fossil table, then 1/600 a medium clue (a pufferfish if the clue can't be had). */
    public static final int FOSSIL_CHANCE = 25;
    public static final int CLUE_CHANCE = 600;
    public static final String CLUE = "clue_scroll_medium"; // approx: the clue bottle (medium) is a plain medium clue here
    /** Held tools that improve chasing (wiki: merfolk trident, not in RuneCraft; the harpoon upgrades count here, approx). */
    public static final List<String> CHASE_TOOLS = List.of("trident_of_the_seas", "trident_of_the_swamp", "dragon_harpoon",
            "infernal_harpoon", "crystal_harpoon");

    /** Fish table: item and the Fishing level from which it joins the table. */
    public record Fish(String item, int level) {
    }

    public static final List<Fish> FISH = List.of(new Fish("oyster", 47), new Fish(PUFFERFISH, 47), new Fish("raw_anchovies", 47),
            new Fish("raw_sardine", 47), new Fish("raw_tuna", 47), new Fish("raw_lobster", 50), new Fish("raw_swordfish", 60),
            new Fish("raw_shark", 70), new Fish("raw_sea_turtle", 80), new Fish("raw_manta_ray", 90));

    /** Fossil table on the 1/25 pre-roll (Fossil Island fossils and numulites; the weights are approx). */
    public static final Object[][] FOSSIL_TABLE = {{"numulite", 10, 40}, {"unidentified_small_fossil", 1, 30},
            {"unidentified_medium_fossil", 1, 18}, {"unidentified_large_fossil", 1, 10}, {"unidentified_rare_fossil", 1, 2}};

    private static double tenths(double xp) {
        return Math.floor(xp * 10 + 1e-9) / 10.0;
    }

    /** Fishing XP per shoal caught: 0.005 L^2 + 0.75 L with L capped at 70 (wiki table: 46.2 at 47, 50 at 50, 77 at 70). */
    public static double fishingXp(int fishingLevel) {
        int l = Math.min(XP_CAP_LEVEL, fishingLevel);
        return tenths(0.005 * l * l + 0.75 * l);
    }

    /** Hunter XP per shoal caught: 0.01 L^2 + 0.75 L with L capped at 70 (wiki table: 52.3 at 44, 62.5 at 50, 101.5 at 70). */
    public static double hunterXp(int hunterLevel) {
        int l = Math.min(XP_CAP_LEVEL, hunterLevel);
        return tenths(0.01 * l * l + 0.75 * l);
    }

    /** The fish a harvest can give at a Fishing level (boosts don't count). */
    public static List<String> fishTable(int fishingLevel) {
        List<String> out = new ArrayList<>();
        for (Fish f : FISH) {
            if (fishingLevel >= f.level()) {
                out.add(f.item());
            }
        }
        return out;
    }

    public static double chaseChance(boolean tool) {
        return tool ? CHASE_CHANCE_TOOL : CHASE_CHANCE;
    }

    /** One loot roll for a caught shoal: {item, count}. */
    public static Object[] roll(int fishingLevel, boolean clueAllowed, Random random) {
        if (random.nextInt(FOSSIL_CHANCE) == 0) {
            int total = 0;
            for (Object[] l : FOSSIL_TABLE) {
                total += (Integer) l[2];
            }
            int r = random.nextInt(total);
            for (Object[] l : FOSSIL_TABLE) {
                r -= (Integer) l[2];
                if (r < 0) {
                    return new Object[]{l[0], l[1]};
                }
            }
        }
        if (random.nextInt(CLUE_CHANCE) == 0) {
            return new Object[]{clueAllowed ? CLUE : PUFFERFISH, 1};
        }
        List<String> table = fishTable(fishingLevel);
        return new Object[]{table.get(random.nextInt(table.size())), 1};
    }

    @Override
    public void items(ItemSink s) {
        simple(s, NET, "Drift net", "net", 0x9A8A5A, 20).examine("A net used to catch fish shoals.");
        simple(s, PUFFERFISH, "Pufferfish", "fish_raw", 0xC0A060, 230).examine("Full of hot air.");
    }

    @Override
    public void recipes(Consumer<Recipe> out) {
        // OSRS Wiki: two jute fibre woven into a drift net at a loom (26 Crafting, 55 XP); looms are spinning stations here.
        out.accept(r("weave_drift_net", MenuType.SPINNING, "Drift net", Skill.CRAFTING, 26, 55).in("jute_fibre", 2)
                .out(NET, 1).ticks(3).build());
    }

    @Override
    public List<BlockDef> blocks() {
        return List.of(BlockDef.scripted(ANCHOR, "Drift net anchors", 0x8A7A5A, "rope"),
                BlockDef.scripted(SHOAL, "Fish shoal", 0x4A7AC0, "fishing_spot"));
    }

    @Override
    public List<PlaceDef> places() {
        // Approx: the drift net area lies underwater north-east of Fossil Island; it stands on the north coast here.
        return List.of(PlaceDef.of(PLACE, "Drift net fishing", PLACE_X, PLACE_Y, PlaceDef.Style.CAMP,
                List.of(ANCHOR, ANCHOR, SHOAL, SHOAL, SHOAL, SHOAL)));
    }
}
