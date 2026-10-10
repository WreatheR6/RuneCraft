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

    /** Catch chance: 55% at the requirement, +2% per level above it, capped at 95% (same curve as core traps). */
    public static double catchChance(int level, Spot spot) {
        return Math.max(0.0, Math.min(0.95, 0.55 + (level - spot.level()) * 0.02));
    }
}
