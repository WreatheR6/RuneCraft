package com.runecraft.osrs.content.islands;

import java.util.ArrayList;
import java.util.List;

/**
 * The Ape Atoll Agility Course (level 48, 580 experience a lap): stepping stone, tropical tree,
 * monkeybars, skull slope, rope swing and the climb down the last tropical tree. Each obstacle is a
 * scripted block; using them in order moves the player to the obstacle's landing tile and awards its
 * experience. OSRS requires the player to be a ninja monkey; here anyone may try (approx.).
 */
public final class IslandsAgility {
    private IslandsAgility() {
    }

    public static final int APE_ATOLL_LEVEL = 48;

    public record Obstacle(String name, int tileX, int tileY, int landX, int landY, double xp) {
    }

    private static final String[] NAMES = {"Stepping stone", "Tropical tree", "Monkeybars", "Skull slope", "Rope swing",
            "Tropical tree"};
    private static final double[] XP = {40, 40, 40, 60, 100, 300};

    public static final List<Obstacle> APE_ATOLL;

    static {
        List<Obstacle> list = new ArrayList<>();
        for (int i = 0; i < IslandsApeAtoll.OBSTACLES.length; i++) {
            int[] o = IslandsApeAtoll.OBSTACLES[i];
            list.add(new Obstacle(NAMES[i], o[0], o[1], o[2], o[3], XP[i]));
        }
        APE_ATOLL = List.copyOf(list);
    }

    /** The obstacle at a tile, or -1. */
    public static int indexAt(int tileX, int tileY) {
        for (int i = 0; i < APE_ATOLL.size(); i++) {
            Obstacle o = APE_ATOLL.get(i);
            if (o.tileX() == tileX && o.tileY() == tileY) {
                return i;
            }
        }
        return -1;
    }
}
