package com.runecraft.osrs.content.systems;

import java.util.List;

/**
 * Where the round 27 teleport items come from in OSRS (OSRS Wiki item pages), for the items whose source is a dialogue
 * trade, a mining spot or an item-on-item combination rather than a shop, reward shop or recipe. Shops: Diango's Toy
 * Store (Chronicle, teleport cards), Jack's Mythical Cape Store and Olaf the Bard (npcs module {@code Shops}); the grand
 * seed pod is a Gnome Restaurant tip (minigames), the eternal teleport crystal a crystal-singing recipe (bosses).
 */
public final class ItemSources {
    /**
     * An NPC who turns one item into another in dialogue: {@code takes} (may be null) plus {@code coins}, needing
     * {@code quest} (may be null).
     */
    public record Trade(String npc, String item, String takes, int coins, String quest) {
    }

    public static final String ELUNED = "eluned";
    public static final String LUMBRIDGE_GUIDE = "lumbridge_guide";
    /** Quest a teleport crystal needs (OSRS: Mourning's End Part I started; RuneCraft checks completion when it exists). */
    public static final String TELEPORT_CRYSTAL_QUEST = "mournings_end_part_i";

    public static final List<Trade> TRADES = List.of(
            // Eluned sings a crystal teleport seed (Lletya/Prifddinas elf drop) into a teleport crystal: 750 coins for the
            // first, 150 less for each later one, never under 150.
            new Trade(ELUNED, "teleport_crystal", "crystal_teleport_seed", 750, TELEPORT_CRYSTAL_QUEST),
            // Phileas the Lumbridge Guide swaps the tarnished locket (intricate pouch) for the amulet of the eye.
            new Trade(LUMBRIDGE_GUIDE, "amulet_of_the_eye", "tarnished_locket", 0, null));

    /** Eluned's price for singing or recharging a teleport crystal after {@code earlier} earlier ones (OSRS Wiki). */
    public static int elunedCost(int earlier) {
        return Math.max(150, 750 - 150 * Math.max(0, earlier));
    }

    /** Charges Eluned sings into a crystal: 3, or 5 with the hard Western Provinces diary (OSRS Wiki). */
    public static int elunedCharges(boolean westernHard) {
        return westernHard ? 5 : 3;
    }

    // ------------------------------------------------------------------ Weiss Salt Mine

    /** Making Friends with My Arm unlocks the Salt Mine and the basalt teleports. */
    public static final String SALT_MINE_QUEST = "making_friends_with_my_arm";

    /**
     * A Salt Mine rock: block id, the item it gives, Mining level and XP (OSRS Wiki: 72 Mining, 5 XP for basalt and each
     * salt) and the OSRS tile it stands on. The mine is under Weiss in OSRS; RuneCraft has no Salt Mine cave, so the rocks
     * stand on the surface beside Weiss. // approx
     */
    public record SaltRock(String block, String item, String name, int level, double xp, int x, int y) {
    }

    public static final List<SaltRock> SALT_ROCKS = List.of(
            new SaltRock("weiss_basalt_rocks", "basalt", "Basalt rocks", 72, 5, 2852, 3940),
            new SaltRock("weiss_te_salt_rocks", "te_salt", "Te salt rocks", 72, 5, 2854, 3940),
            new SaltRock("weiss_efh_salt_rocks", "efh_salt", "Efh salt rocks", 72, 5, 2852, 3942),
            new SaltRock("weiss_urt_salt_rocks", "urt_salt", "Urt salt rocks", 72, 5, 2854, 3942));

    /** A basalt teleport: 1 basalt, 3 of {@code salt} and 1 te salt (OSRS Wiki: icy uses efh salt, stony urt salt). */
    public record Basalt(String item, String salt) {
    }

    public static final List<Basalt> BASALTS = List.of(new Basalt("icy_basalt", "efh_salt"), new Basalt("stony_basalt", "urt_salt"));
    public static final int BASALT_SALTS = 3;

    public static SaltRock saltRock(String block) {
        for (SaltRock r : SALT_ROCKS) {
            if (r.block().equals(block)) {
                return r;
            }
        }
        return null;
    }

    /** The basalt teleport made from what the player carries (efh first), or null without basalt, te salt and 3 salts. */
    public static Basalt basaltFor(java.util.function.ToIntFunction<String> count) {
        if (count.applyAsInt("basalt") < 1 || count.applyAsInt("te_salt") < 1) {
            return null;
        }
        for (Basalt b : BASALTS) {
            if (count.applyAsInt(b.salt()) >= BASALT_SALTS) {
                return b;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ Fremennik armour makers

    /**
     * Rellekka craftsmen who make Fremennik armour from dagannoth hides after The Fremennik Trials (OSRS Wiki): Sigli the
     * Huntsman (spined), Skulgrimen (rock-shell), Peer the Seer (skeletal). Recipe id prefix in the tanning menu:
     * {@code fremennik_<item>}.
     */
    public static final String FREMENNIK_QUEST = "fremennik_trials";
    public static final String FREMENNIK_RECIPE_PREFIX = "fremennik_";
    public static final String SKULGRIMEN = "skulgrimen";
    public static final String PEER_THE_SEER = "peer_the_seer";
    public static final String SIGLI = "sigli";

    /** The armour line each craftsman makes. */
    public static String fremennikLine(String npc) {
        return switch (npc) {
            case SIGLI -> "spined";
            case SKULGRIMEN -> "rock_shell";
            case PEER_THE_SEER -> "skeletal";
            default -> null;
        };
    }

    private ItemSources() {
    }
}
