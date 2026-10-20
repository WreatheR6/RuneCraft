package com.runecraft.osrs.content.minigames;

import java.util.List;

/**
 * Blocks added for the skilling bosses in round 38 (kept apart from {@link MinigameWorld#ARENA_BLOCKS}): the Wintertodt's
 * reward cart (OSRS Wiki "Reward Cart", since 9 October 2024; in the Wintertodt Camp and the prison's lobby) and the
 * Guardians of the Rift remains beyond the arena's ordinary guardian remains: the large guardian remains on the east shelf
 * (56 Agility, 2-4 fragments) and the huge guardian remains on the west shelf, reached through a portal that opens 160
 * seconds into a game and every 140 seconds after for about 25 (4-6 guardian essence).
 */
public final class SkillingBosses {
    private SkillingBosses() {
    }

    public static final String WT_REWARD_CART = "wintertodt_reward_cart";
    public static final String GOTR_LARGE_REMAINS = "gotr_large_remains";
    public static final String GOTR_HUGE_REMAINS = "gotr_huge_remains";
    public static final String GOTR_PORTAL = "gotr_portal";

    /** id, name, colour, look (as in {@link MinigameWorld#ARENA_BLOCKS}). */
    public static final String[][] BLOCKS = {
            {WT_REWARD_CART, "Reward Cart", "8A6A3A", "crate"},
            {GOTR_LARGE_REMAINS, "Large guardian remains", "7A6AD0", "rock"},
            {GOTR_HUGE_REMAINS, "Huge guardian remains", "9A8AE0", "rock"},
            {GOTR_PORTAL, "Portal", "E0D040", "portal_glow"}};

    /** The blocks that only exist inside a minigame arena (dispatched to the session). */
    public static final List<String> ARENA_ONLY = List.of(GOTR_LARGE_REMAINS, GOTR_HUGE_REMAINS, GOTR_PORTAL);
}
