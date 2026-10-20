package com.runecraft.osrs.content.bosses;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The fight rules of the Phantom Muspah (OSRS Wiki "Phantom Muspah", "Phantom Muspah/Strategies", "Spike (Phantom Muspah)",
 * "Corruption (status)", "Walk Straight Pray True", July 2026), played out by the bosses module's Muspah script. The prayer
 * shield numbers and the spikes' growth stay in {@link BossMechanics}.
 *
 * <p>It spawns in its ranged or its melee form and swaps after taking 100 damage in the ranged form or 80 in the melee
 * form (after its first swap, also only after four separate hits). At 637 and 425 health it uses its two specials, Lightning
 * Clouds first if it spawned ranged, else Homing Spikes. Below 127 it teleports to the centre and sends out a shockwave
 * that only spikes stop, then raises a 75-point prayer shield with Soul Split; when that breaks it returns to its ranged form,
 * and spikes grow over the arena every four of its attacks.
 */
public final class MuspahRules {
    private MuspahRules() {
    }

    public enum Form { RANGED, MELEE }

    public static final int HITPOINTS = 850;
    /** Damage that ends a form: 100 in the ranged form, 80 in the melee form. */
    public static final int RANGED_FORM_DAMAGE = 100;
    public static final int MELEE_FORM_DAMAGE = 80;
    /** After its first swap it also needs this many separate hits in a form before it swaps again (22 July 2026). */
    public static final int HITS_TO_SWAP = 4;

    /** Whether the form swaps now. */
    public static boolean swaps(Form form, int damageInForm, int hitsInForm, boolean swappedBefore) {
        int needed = form == Form.RANGED ? RANGED_FORM_DAMAGE : MELEE_FORM_DAMAGE;
        return damageInForm >= needed && (!swappedBefore || hitsInForm >= HITS_TO_SWAP);
    }

    /** Health at which its specials begin (75% and 50%), and below which it raises its shield. */
    public static final int FIRST_SPECIAL_HP = 637;
    public static final int SECOND_SPECIAL_HP = 425;
    public static final int SHIELD_BELOW = 127;

    public enum Special { LIGHTNING_CLOUDS, HOMING_SPIKES }

    /** Its first (index 0) and second special: Lightning Clouds first for a ranged spawn, Homing Spikes first for a melee one. */
    public static Special special(int index, Form spawnForm) {
        boolean clouds = (spawnForm == Form.RANGED) == (index == 0);
        return clouds ? Special.LIGHTNING_CLOUDS : Special.HOMING_SPIKES;
    }

    /** How many specials are due at {@code health} (0, 1 or 2). */
    public static int specialsDue(int health) {
        return health <= SECOND_SPECIAL_HP ? 2 : health <= FIRST_SPECIAL_HP ? 1 : 0;
    }

    // ------------------------------------------------------------ attacks
    /** Max hits: ranged form 61 ranged / 72 magic; melee form 34; shielded and after the shield 72 / 79; clouds 20; shockwave 80. */
    public static final int RANGED = 61;
    public static final int MAGIC = 72;
    public static final int MELEE = 34;
    public static final int SHIELDED_RANGED = 72;
    public static final int SHIELDED_MAGIC = 79;
    public static final int LIGHTNING_CLOUD = 20;
    public static final int SHOCKWAVE = 80;
    /** Attack speed: 6 in the ranged form, 5 in the melee form, the shielded form and after the shield. */
    public static final int RANGED_SPEED = 6;
    public static final int FAST_SPEED = 5;
    /** Its charged magic attack in the ranged forms: one attack in this many (approx: "occasionally"). */
    public static final int MAGIC_ONE_IN = 5;
    /** Its magic orb flies for this many ticks: praying magic only in flight still lets it through, weakened (approx). */
    public static final int MAGIC_FLIGHT_TICKS = 2;
    /** Share of the magic hit that gets through a Protect from Magic switched on in flight (approx: "a significant amount" off). */
    public static final double MAGIC_LATE_PRAYER = 0.4;

    /**
     * DPS calculator forms: the melee form's magic defence is 34 and its ranged defence 261 (the other forms: 437 and 56);
     * the shielded form attacks with Ranged 336 and Magic 180, the post-shield form with 335 and 179 (the others 280 / 150).
     */
    public static final int MELEE_FORM_MAGIC_DEFENCE = 34;
    public static final int MELEE_FORM_RANGED_DEFENCE = 261;
    public static final int SHIELDED_RANGED_LEVEL = 336;
    public static final int SHIELDED_MAGIC_LEVEL = 180;
    public static final int POST_RANGED_LEVEL = 335;
    public static final int POST_MAGIC_LEVEL = 179;

    /** Max hit of its ranged and magic attacks in its current stage. */
    public static int rangedMax(boolean shieldedOrAfter) {
        return shieldedOrAfter ? SHIELDED_RANGED : RANGED;
    }

    public static int magicMax(boolean shieldedOrAfter) {
        return shieldedOrAfter ? SHIELDED_MAGIC : MAGIC;
    }

    /**
     * Share of a melee hit that gets through Protect from Melee: none when it had to chase the player into reach before
     * attacking, more the longer it stands still (a quarter more per attack in place, approx: "deal increasingly more damage
     * the longer it stays still, even through prayer").
     */
    public static double meleeThroughPrayer(int attacksInPlace) {
        return Math.max(0, Math.min(1.0, 0.25 * attacksInPlace));
    }

    // ------------------------------------------------------------ Corruption
    /** Corruption from its magic attack drains 3, 6 and 9 prayer, one every 6 seconds (10 ticks); the shockwave's twice that (approx). */
    public static final int CORRUPTION_INTERVAL = 10;
    public static final int CORRUPTION_PULSES = 3;

    public static int corruptionDrain(int pulse, boolean shockwave) {
        int d = 3 * Math.max(1, Math.min(CORRUPTION_PULSES, pulse));
        return shockwave ? d * 2 : d;
    }

    // ------------------------------------------------------------ Lightning Clouds
    /** Teleports during Lightning Clouds, a cloud per teleport (approx), ticks between them, and the clouds' life. */
    public static final int CLOUD_TELEPORTS = 6;
    public static final int CLOUD_INTERVAL = 2;
    public static final int CLOUD_LIFE = 12;

    // ------------------------------------------------------------ Homing Spikes
    /** 2-4 spikes home in on the player, one tile every two ticks, never diagonally, for 34 ticks; then they harden. */
    public static final int HOMING_MIN = 2;
    public static final int HOMING_MAX = 4;
    public static final int HOMING_STEP_TICKS = 2;
    public static final int HOMING_TICKS = 34;

    /**
     * One step of a homing spike at ({@code x}, {@code z}) towards ({@code tx}, {@code tz}): along the longer axis first, the
     * other if that tile holds a spike; stays put when both are blocked (it can't pass through idle spikes). Returns the new
     * tile as {x, z}.
     */
    public static int[] homingStep(int x, int z, int tx, int tz, Set<Long> blocked) {
        int dx = Integer.signum(tx - x);
        int dz = Integer.signum(tz - z);
        boolean xFirst = Math.abs(tx - x) >= Math.abs(tz - z);
        int[][] tries = xFirst ? new int[][]{{dx, 0}, {0, dz}} : new int[][]{{0, dz}, {dx, 0}};
        for (int[] t : tries) {
            if (t[0] == 0 && t[1] == 0) {
                continue;
            }
            long next = BossMechanics.tile(x + t[0], z + t[1]);
            if (!blocked.contains(next)) {
                return new int[]{x + t[0], z + t[1]};
            }
        }
        return new int[]{x, z};
    }

    // ------------------------------------------------------------ Shockwave
    /** Ticks between its teleport to the centre and the shockwave (it can still be hurt; a kill here skips the shield). */
    public static final int SHOCKWAVE_WIND_UP = 5;

    /**
     * Whether a spike shelters a player at ({@code px}, {@code pz}) from the shockwave of a Muspah centred on ({@code cx},
     * {@code cz}): a spike on a tile of the straight line between them, outside the Muspah's own footprint (half-size
     * {@code half}).
     */
    public static boolean sheltered(Set<Long> spikes, int cx, int cz, int px, int pz, int half) {
        for (long t : line(cx, cz, px, pz)) {
            int x = (int) (t >> 32);
            int z = (int) t;
            if (Math.max(Math.abs(x - cx), Math.abs(z - cz)) <= half || (x == px && z == pz)) {
                continue;
            }
            if (spikes.contains(t)) {
                return true;
            }
        }
        return false;
    }

    /** The tiles of a straight line from one tile to another (Bresenham), both ends included. */
    public static List<Long> line(int x0, int z0, int x1, int z1) {
        List<Long> out = new ArrayList<>();
        int dx = Math.abs(x1 - x0);
        int dz = -Math.abs(z1 - z0);
        int sx = x0 < x1 ? 1 : -1;
        int sz = z0 < z1 ? 1 : -1;
        int err = dx + dz;
        int x = x0;
        int z = z0;
        while (true) {
            out.add(BossMechanics.tile(x, z));
            if (x == x1 && z == z1) {
                break;
            }
            int e2 = 2 * err;
            if (e2 >= dz) {
                err += dz;
                x += sx;
            }
            if (e2 <= dx) {
                err += dx;
                z += sz;
            }
        }
        return out;
    }

    // ------------------------------------------------------------ the shielded phase
    /** In its shielded form it raises a spike under the player every this many attacks (approx). */
    public static final int SHIELD_SPIKE_EVERY = 3;
    /** Spikes erupt this many ticks after their tiles are marked, so a player can step off (approx). */
    public static final int SPIKE_ERUPT_TICKS = 2;
    /** Walking onto a standing spike hurts again at most once every this many ticks (approx). */
    public static final int SPIKE_WALK_COOLDOWN = 3;

    // ------------------------------------------------------------ drops
    /** A kill in under 3:00 (300 ticks) gives charged ice (not in RuneCraft yet); a Venator shard comes with no other loot. */
    public static final int CHARGED_ICE_TICKS = 300;
    public static final String VENATOR_SHARD = "venator_shard";
    public static final String FROZEN_CACHE = "frozen_cache";

    /** One reward line of the frozen cache: item, quantity range and weight (out of {@link #FROZEN_CACHE_TOTAL}). */
    public record CacheLine(String item, int min, int max, int weight) {
    }

    /** The frozen cache's pre-roll (1/50): another cache, an ancient icon or a Venator shard (7 : 2 : 1). */
    public static final int CACHE_PREROLL = 50;
    public static final List<CacheLine> CACHE_PREROLL_LINES = List.of(new CacheLine(FROZEN_CACHE, 1, 1, 7),
            new CacheLine("ancient_icon", 1, 1, 2), new CacheLine(VENATOR_SHARD, 1, 1, 1));
    /**
     * Its main table (OSRS Wiki "Frozen cache", weights out of 80): ancient essence 15/80 (split 60 : 23 : 10 as the
     * Muspah's own unique table), weapons and armour, runes, herbs and seeds, resources.
     */
    public static final int FROZEN_CACHE_TOTAL = 80 * 93;
    public static final List<CacheLine> CACHE_LINES = List.of(
            new CacheLine("ancient_essence", 540, 599, 15 * 60), new CacheLine("ancient_essence", 885, 995, 15 * 23),
            new CacheLine("ancient_essence", 1970, 2060, 15 * 10),
            new CacheLine("dragon_plateskirt", 1, 1, 5 * 93), new CacheLine("rune_platelegs", 3, 3, 5 * 93),
            new CacheLine("black_dhide_body", 1, 1, 5 * 93), new CacheLine("dragon_platelegs", 2, 2, 4 * 93),
            new CacheLine("rune_sword", 1, 1, 93),
            new CacheLine("chaos_rune", 480, 480, 5 * 93), new CacheLine("fire_rune", 1964, 1964, 5 * 93),
            new CacheLine("cannonball", 670, 670, 5 * 93),
            new CacheLine("torstol_seed", 2, 2, 5 * 93), new CacheLine("grimy_toadflax", 40, 40, 3 * 93),
            new CacheLine("ranarr_seed", 5, 5, 3 * 93), new CacheLine("snapdragon_seed", 5, 5, 4 * 93),
            new CacheLine("spirit_seed", 1, 1, 2 * 93),
            new CacheLine("coal", 163, 163, 5 * 93), new CacheLine("runite_ore", 18, 18, 3 * 93),
            new CacheLine("limpwurt_root", 21, 21, 3 * 93), new CacheLine("silver_ore", 101, 101, 2 * 93));

    /** One opening of a frozen cache: the line it gives (quantities are rolled by the caller). */
    public static CacheLine openCache(java.util.Random r) {
        if (r.nextInt(CACHE_PREROLL) == 0) {
            return weighted(CACHE_PREROLL_LINES, 10, r);
        }
        return weighted(CACHE_LINES, FROZEN_CACHE_TOTAL, r);
    }

    private static CacheLine weighted(List<CacheLine> lines, int total, java.util.Random r) {
        int roll = r.nextInt(total);
        for (CacheLine l : lines) {
            roll -= l.weight();
            if (roll < 0) {
                return l;
            }
        }
        return lines.get(lines.size() - 1);
    }
}
