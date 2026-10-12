package com.runecraft.osrs.content.zeah;

import java.util.List;

/**
 * Varlamore Hunter spots (OSRS levels and XP). Each is a scripted block placed at the creature's
 * habitat; the Minecraft side lays the trap, waits {@code catchTicks} game ticks and rolls the
 * catch like the core Hunter traps do.
 */
public final class ZeahHunter {
    private ZeahHunter() {
    }

    public enum Method { PITFALL, NET, BOX_TRAP }

    /**
     * @param tools items that must be in the inventory
     * @param consumed item used up when the trap is laid (logs for pitfalls, a jar for moths), or null
     */
    public record Spot(String blockId, String creature, Method method, int level, double xp, List<String> tools,
                       String consumed, List<String> loot, int catchTicks) {
    }

    public static final List<Spot> SPOTS = List.of(
            new Spot(ZeahBlocks.JERBOA_BURROW, "Embertailed jerboa", Method.BOX_TRAP, 39, 137.0, List.of("box_trap"), null,
                    List.of("bones", "raw_embertailed_jerboa"), 15),
            new Spot(ZeahBlocks.SUNLIGHT_MOTH_BUSH, "Sunlight moth", Method.NET, 65, 74.0, List.of("butterfly_net"), "butterfly_jar",
                    List.of("sunlight_moth"), 5),
            new Spot(ZeahBlocks.SUNLIGHT_ANTELOPE_PIT, "Sunlight antelope", Method.PITFALL, 72, 380.0, List.of("knife"), "logs",
                    List.of("big_bones", "raw_sunlight_antelope", "sunlight_antler", "sunlight_antelope_fur"), 20),
            new Spot(ZeahBlocks.MOONLIGHT_MOTH_BUSH, "Moonlight moth", Method.NET, 75, 84.0, List.of("butterfly_net"), "butterfly_jar",
                    List.of("moonlight_moth"), 5),
            new Spot(ZeahBlocks.MOONLIGHT_ANTELOPE_PIT, "Moonlight antelope", Method.PITFALL, 91, 450.0, List.of("knife"), "logs",
                    List.of("big_bones", "raw_moonlight_antelope", "moonlight_antler", "moonlight_antelope_fur"), 20));

    public static Spot spot(String blockId) {
        for (Spot s : SPOTS) {
            if (s.blockId().equals(blockId)) {
                return s;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ Hunters' Rumours

    /** Catches of the assigned creature needed to find the rumour's proof (approx: OSRS rolls a rare part per catch). */
    public static final int RUMOUR_CATCHES = 5;
    /** Kill-count keys that hold a player's rumour progress (persisted with the player's kill counts). */
    public static final String RUMOURS_DONE = "hunter_rumours";
    public static final String RUMOUR_TARGET = "hunter_rumour_target";
    public static final String RUMOUR_CAUGHT = "hunter_rumour_caught";
    /** Chance (1 in n) that a completed rumour's loot sack holds a guild hunter outfit piece (approx). */
    public static final int OUTFIT_ONE_IN = 4;

    /** Hunter Guild minimum level (OSRS: 46 Hunter to join and take rumours). */
    public static final int GUILD_LEVEL = 46;

    /** A rumour target the player can catch: a random Varlamore spot at or below their level, or null. */
    public static Spot rumourTarget(int hunterLevel, java.util.Random random) {
        List<Spot> ok = new java.util.ArrayList<>();
        for (Spot s : SPOTS) {
            if (s.level() <= hunterLevel) {
                ok.add(s);
            }
        }
        return ok.isEmpty() ? null : ok.get(random.nextInt(ok.size()));
    }

    /**
     * A Hunters' Rumours reward: {@code rumours} > 0 = handed over on reaching that many completed rumours
     * (quetzal whistles: basic at 10 per the wiki, enhanced/perfected approx); 0 = a loot sack roll
     * (guild hunter outfit, 1 in {@link #OUTFIT_ONE_IN} per rumour).
     */
    public record RumourReward(String item, int rumours) {
    }

    public static final List<RumourReward> RUMOUR_REWARDS = List.of(
            new RumourReward("guild_hunter_headwear", 0), new RumourReward("guild_hunter_top", 0),
            new RumourReward("guild_hunter_legs", 0), new RumourReward("guild_hunter_boots", 0),
            new RumourReward("quetzal_whistle_basic", 10), new RumourReward("quetzal_whistle_enhanced", 50),
            new RumourReward("quetzal_whistle_perfected", 150));

    /** The milestone reward for reaching this many completed rumours, or null. */
    public static String whistleAt(int rumoursDone) {
        for (RumourReward r : RUMOUR_REWARDS) {
            if (r.rumours() > 0 && r.rumours() == rumoursDone) {
                return r.item();
            }
        }
        return null;
    }

    /** Outfit pieces a loot sack can hold. */
    public static List<String> outfitPieces() {
        List<String> out = new java.util.ArrayList<>();
        for (RumourReward r : RUMOUR_REWARDS) {
            if (r.rumours() == 0) {
                out.add(r.item());
            }
        }
        return out;
    }

    /** Catch chance: 55% at the requirement, +2% per level above it, capped at 95% (same curve as core traps). */
    public static double catchChance(int level, Spot spot) {
        return Math.max(0.0, Math.min(0.95, 0.55 + (level - spot.level()) * 0.02));
    }
}
