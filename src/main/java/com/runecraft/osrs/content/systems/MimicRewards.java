package com.runecraft.osrs.content.systems;

import java.util.List;
import java.util.Random;

/**
 * The Mimic's pre-roll (OSRS Wiki "The Mimic", master casket, first attempt): a 1/228 chance of a 3rd age item (each of
 * the pieces equally likely, 1/5,244 each in OSRS) and six times that, 6/228, of a ring of 3rd age. Rolled by the systems
 * module when the Mimic dies, since it owns the clue reward items.
 */
public final class MimicRewards {
    public static final String MIMIC = "the_mimic";
    public static final String RING = "ring_of_3rd_age";
    public static final int TABLE = 228;
    public static final int RING_SLOTS = 6;
    /** The 3rd age pieces RuneCraft has (OSRS also has the 3rd age plateskirt). */
    public static final List<String> THIRD_AGE = List.of("3rd_age_full_helmet", "3rd_age_platebody", "3rd_age_platelegs",
            "3rd_age_kiteshield", "3rd_age_range_coif", "3rd_age_range_top", "3rd_age_range_legs", "3rd_age_vambraces",
            "3rd_age_mage_hat", "3rd_age_robe_top", "3rd_age_robe", "3rd_age_amulet", "3rd_age_longsword", "3rd_age_bow",
            "3rd_age_wand", "3rd_age_cloak", "3rd_age_druidic_robe_top", "3rd_age_druidic_robe_bottoms", "3rd_age_druidic_cloak",
            "3rd_age_druidic_staff", "3rd_age_pickaxe", "3rd_age_axe");

    private MimicRewards() {
    }

    /** One kill's pre-roll: the ring of 3rd age, a 3rd age piece, or null. */
    public static String roll(Random random) {
        int x = random.nextInt(TABLE);
        if (x < RING_SLOTS) {
            return RING;
        }
        return x == RING_SLOTS ? THIRD_AGE.get(random.nextInt(THIRD_AGE.size())) : null;
    }
}
