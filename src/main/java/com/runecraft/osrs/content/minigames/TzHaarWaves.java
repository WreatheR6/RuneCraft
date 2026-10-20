package com.runecraft.osrs.content.minigames;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wave lists for the TzHaar Fight Cave and the Inferno.
 *
 * <p>The Fight Cave's 63 waves follow its exact counting pattern: monster levels 22, 45, 90, 180,
 * 360. Each new tier T contributes the waves [T], [T + every earlier wave], [T, T], giving 2 + 4 +
 * 8 + 16 + 32 = 62 waves, then TzTok-Jad on wave 63.
 *
 * <p>The Inferno has 69 waves built the same way over Jal-MejRah, Jal-Ak, Jal-ImKot, Jal-Xil and
 * Jal-Zek (with a nibbler-only wave after each of the first four tiers), three Jal-Nib on every
 * wave, then JalTok-Jad on wave 67, three JalTok-Jads on wave 68 and TzKal-Zuk on wave 69.
 */
public final class TzHaarWaves {
    public static final String TZ_KIH = "tz_kih";
    public static final String TZ_KEK = "tz_kek";
    public static final String TOK_XIL = "tok_xil";
    public static final String YT_MEJKOT = "yt_mejkot";
    public static final String KET_ZEK = "ket_zek";
    public static final String JAD = "tztok_jad";

    public static final String JAL_NIB = "jal_nib";
    public static final String JAL_MEJRAH = "jal_mejrah";
    public static final String JAL_AK = "jal_ak";
    public static final String JAL_IMKOT = "jal_imkot";
    public static final String JAL_XIL = "jal_xil";
    public static final String JAL_ZEK = "jal_zek";
    public static final String JALTOK_JAD = "jaltok_jad";
    public static final String ZUK = "tzkal_zuk";

    public static final int FIGHT_CAVE_WAVES = 63;
    public static final int INFERNO_WAVES = 69;

    private static final List<List<String>> FIGHT_CAVES;
    private static final List<List<String>> INFERNO;

    static {
        String[] caveTiers = {TZ_KIH, TZ_KEK, TOK_XIL, YT_MEJKOT, KET_ZEK};
        List<List<String>> waves = new ArrayList<>();
        for (String tier : caveTiers) {
            List<List<String>> before = new ArrayList<>(waves);
            waves.add(List.of(tier));
            for (List<String> w : before) {
                List<String> combined = new ArrayList<>();
                combined.add(tier);
                combined.addAll(w);
                waves.add(List.copyOf(combined));
            }
            waves.add(List.of(tier, tier));
        }
        waves.add(List.of(JAD));
        FIGHT_CAVES = Collections.unmodifiableList(waves);

        // Inferno: the same counting pattern over bats, blobs, meleers, rangers and magers. Each
        // tier T contributes [T], [T + every earlier non-empty wave], [T, T]; the first four
        // tiers are each followed by a nibbler-only wave. That gives waves 1-3 (bat), 4-8 (blob),
        // 9-17 (Jal-ImKot), 18-34 (Jal-Xil) and 35-66 (Jal-Zek), with the double-blob waves on
        // 7, 15, 24, 31, 41, 48, 56 and 63 exactly as in OSRS.
        String[] infernoTiers = {JAL_MEJRAH, JAL_AK, JAL_IMKOT, JAL_XIL, JAL_ZEK};
        List<List<String>> combos = new ArrayList<>();
        List<List<String>> inferno = new ArrayList<>();
        for (int t = 0; t < infernoTiers.length; t++) {
            String tier = infernoTiers[t];
            List<List<String>> before = new ArrayList<>(combos);
            List<List<String>> block = new ArrayList<>();
            block.add(List.of(tier));
            for (List<String> w : before) {
                List<String> combined = new ArrayList<>();
                combined.add(tier);
                combined.addAll(w);
                block.add(List.copyOf(combined));
            }
            block.add(List.of(tier, tier));
            combos.addAll(block);
            inferno.addAll(block);
            if (t < infernoTiers.length - 1) {
                inferno.add(List.of());
            }
        }
        List<List<String>> withNibblers = new ArrayList<>();
        for (List<String> w : inferno) {
            List<String> full = new ArrayList<>(List.of(JAL_NIB, JAL_NIB, JAL_NIB));
            if (w.isEmpty()) {
                // Nibbler-only waves bring a second group of three.
                full.addAll(List.of(JAL_NIB, JAL_NIB, JAL_NIB));
            }
            full.addAll(w);
            withNibblers.add(List.copyOf(full));
        }
        withNibblers.add(List.of(JALTOK_JAD));
        withNibblers.add(List.of(JALTOK_JAD, JALTOK_JAD, JALTOK_JAD));
        withNibblers.add(List.of(ZUK));
        INFERNO = Collections.unmodifiableList(withNibblers);
    }

    private TzHaarWaves() {
    }

    /** Monsters of a Fight Cave wave (1-based). */
    public static List<String> fightCave(int wave) {
        return FIGHT_CAVES.get(wave - 1);
    }

    /** Monsters of an Inferno wave (1-based). */
    public static List<String> inferno(int wave) {
        return INFERNO.get(wave - 1);
    }

    public static List<List<String>> fightCaveWaves() {
        return FIGHT_CAVES;
    }

    public static List<List<String>> infernoWaves() {
        return INFERNO;
    }

    // ------------------------------------------------------------------ Fight Cave spawn rotations

    /** The Fight Cave's five spawn points (OSRS Wiki "TzHaar Fight Cave/Rotations"). */
    public enum CaveSpawn {
        SE, SW, C, NW, S
    }

    /**
     * The spawn cycle every rotation follows (OSRS Wiki "TzHaar Fight Cave", Map breakdown): each wave the highest-level
     * monster spawns at the next point of the cycle, lower-levelled ones at the points after it.
     */
    private static final CaveSpawn[] CYCLE = {CaveSpawn.SE, CaveSpawn.SW, CaveSpawn.C, CaveSpawn.NW, CaveSpawn.SW, CaveSpawn.SE,
            CaveSpawn.S, CaveSpawn.NW, CaveSpawn.C, CaveSpawn.SE, CaveSpawn.SW, CaveSpawn.S, CaveSpawn.NW, CaveSpawn.C, CaveSpawn.S};
    /** Where in {@link #CYCLE} each of the 15 rotations starts (rotation 1 first), from the wiki's rotation table. */
    private static final int[] ROTATION_START = {2, 13, 8, 12, 7, 3, 11, 6, 14, 5, 0, 9, 1, 10, 4};
    public static final int FIGHT_CAVE_ROTATIONS = ROTATION_START.length;
    /** TzTok-Jad's four Yt-HurKot always appear at these spawn points, whatever the rotation. */
    public static final List<CaveSpawn> HEALER_SPAWNS = List.of(CaveSpawn.SE, CaveSpawn.S, CaveSpawn.NW, CaveSpawn.SW);

    /**
     * Spawn points of a Fight Cave wave's monsters in rotation {@code rotation} (1-15), one per monster of
     * {@link #fightCave(int)} in the same order (highest level first). TzTok-Jad (wave 63) lands where wave 3's Tz-Kek did.
     */
    public static List<CaveSpawn> fightCaveSpawns(int rotation, int wave) {
        int start = ROTATION_START[Math.floorMod(rotation - 1, FIGHT_CAVE_ROTATIONS)];
        List<CaveSpawn> out = new ArrayList<>();
        int n = fightCave(wave).size();
        for (int i = 0; i < n; i++) {
            out.add(CYCLE[(start + wave - 1 + i) % CYCLE.length]);
        }
        return out;
    }

    /** Monster level shown on the wiki's rotation pages for a Fight Cave creature id (0 if not one). */
    public static int caveLevel(String id) {
        return switch (id) {
            case TZ_KIH -> 22;
            case TZ_KEK -> 45;
            case TOK_XIL -> 90;
            case YT_MEJKOT -> 180;
            case KET_ZEK -> 360;
            case JAD -> 702;
            case "yt_hurkot" -> 108;
            default -> 0;
        };
    }

    // ------------------------------------------------------------------ rewards

    /** Tokkul for completing the Fight Cave (8,032 plus the fire cape): 63 waves of {@link #fightCaveTokkul} plus 4,000 for Jad. */
    public static final int FIGHT_CAVE_TOKKUL = 8032;
    /** Extra tokkul for killing TzTok-Jad (Mod Kieren, quoted on the wiki's Fight Cave page). */
    public static final int JAD_BONUS_TOKKUL = 4000;

    /**
     * Tokkul for a Fight Cave run that ended (death or leaving) after {@code wavesCompleted} waves: each wave pays two more
     * than the last, N(N+1) (OSRS Wiki "TzHaar Fight Cave", Rewards); killing Jad completes wave 63 and adds 4,000.
     */
    public static int fightCaveTokkul(int wavesCompleted, boolean jadKilled) {
        int n = Math.max(0, Math.min(FIGHT_CAVE_WAVES, wavesCompleted));
        return n * (n + 1) + (jadKilled ? JAD_BONUS_TOKKUL : 0);
    }

    /** Tokkul for completing the Inferno (16,440 plus the infernal cape). */
    public static final int INFERNO_TOKKUL = 16440;

    /**
     * Tokkul for an Inferno run that ended after {@code wavesCompleted} waves. The wiki only says it grows with the wave
     * reached, up to 16,440; this scales it like the Fight Cave's N(N+1). // approx
     */
    public static int infernoTokkul(int wavesCompleted) {
        int n = Math.max(0, Math.min(INFERNO_WAVES, wavesCompleted));
        return n >= INFERNO_WAVES ? INFERNO_TOKKUL
                : (int) Math.round(INFERNO_TOKKUL * n * (n + 1) / (double) (INFERNO_WAVES * (INFERNO_WAVES + 1)));
    }

    /** The elite Karamja Diary doubles the Fight Cave's and the Inferno's tokkul. */
    public static int diaryTokkul(int tokkul, boolean eliteKaramja) {
        return eliteKaramja ? tokkul * 2 : tokkul;
    }

    /** Pet chances on completion (1 in N), and on a TzHaar Slayer task (OSRS Wiki TzTok-Jad / TzKal-Zuk drops). */
    public static final int JAD_PET_CHANCE = 200;
    public static final int JAD_PET_CHANCE_TASK = 100;
    public static final int ZUK_PET_CHANCE = 100;
    public static final int ZUK_PET_CHANCE_TASK = 75;
    /** TzHaar-Mej-Jal takes a spare fire cape for 8,000 tokkul or a 1/200 roll at TzRek-Jad (OSRS Wiki "TzTok-Jad"). */
    public static final int FIRE_CAPE_TOKKUL = 8000;
    public static final int FIRE_CAPE_PET_CHANCE = 200;
    /** TzHaar-Ket-Keh takes a spare infernal cape for a 1/100 roll at Jal-nib-rek (OSRS Wiki "Jal-nib-rek"). */
    public static final int INFERNAL_CAPE_PET_CHANCE = 100;

    /**
     * Whether a pet may change form: TzRek-Jad and JalRek-Jad once all six of TzHaar-Ket-Rak's challenges are done (OSRS Wiki
     * "TzRek-Jad"), Jal-nib-rek and TzRek-Zuk always.
     */
    public static boolean canMetamorphose(String petId, boolean ketRakComplete) {
        return switch (petId) {
            case "tzrek_jad", "jalrek_jad" -> ketRakComplete;
            case "jal_nib_rek", "tzrek_zuk" -> true;
            default -> false;
        };
    }

    /** Slayer experience for TzTok-Jad and TzKal-Zuk on a TzHaar task (wiki infoboxes). */
    public static final int JAD_SLAYER_XP = 25250;
    public static final int ZUK_SLAYER_XP = 101890;

    // ------------------------------------------------------------------ Jad healers

    /** TzTok-Jad's magic attack hits up to 95 (his melee and ranged 97; OSRS Wiki "TzTok-Jad"). Melee is a stab. */
    public static final int JAD_MAGIC_MAX = 95;

    /** TzTok-Jad's level-108 healers and JalTok-Jad's level-141 ones (OSRS Wiki "Yt-HurKot"). */
    public static final String HURKOT = "yt_hurkot";
    public static final String HURKOT_INFERNO = "yt_hurkot_inferno";

    /** The healer a Jad summons. */
    public static String healerOf(String jadId) {
        return JAD.equals(jadId) ? HURKOT : HURKOT_INFERNO;
    }

    /** Yt-HurKot that tend the Fight Cave's TzTok-Jad once it drops to half hitpoints. */
    public static final int FIGHT_CAVE_JAD_HEALERS = 4;

    /** Healers spawn when their Jad falls to or below half of its hitpoints (OSRS Wiki "Yt-HurKot"). */
    public static boolean healersDue(float health, float maxHealth) {
        return health > 0 && health <= maxHealth / 2f;
    }

    /** Yt-HurKot each Inferno JalTok-Jad summons at half hitpoints: five on wave 67, three otherwise (wave 68 and Zuk's Jad). */
    public static int infernoJadHealers(int wave) {
        return wave == 67 ? 5 : 3;
    }

    /** Game ticks between two heals of a Yt-HurKot (OSRS Wiki "Yt-HurKot", quoting Mod Ash). */
    public static final int HURKOT_HEAL_TICKS = 4;

    /**
     * Hitpoints one Yt-HurKot restores to its Jad every {@link #HURKOT_HEAL_TICKS} ticks (OSRS Wiki "Yt-HurKot", quoting Mod Ash):
     * a flat 5 for the Fight Caves' TzTok-Jad, 15-24 for a JalTok-Jad (the Inferno and TzHaar-Ket-Rak's challenges).
     */
    public static int hurKotHeal(String jadId, java.util.Random random) {
        return JAD.equals(jadId) ? 5 : 15 + random.nextInt(10);
    }

    /**
     * Whether a Yt-HurKot heals its Jad (OSRS Wiki "Yt-HurKot" / "TzHaar Fight Cave"): Fight Cave healers heal within melee
     * range of Jad, or while in combat with the player within 4 tiles of him; JalTok-Jad healers (the Inferno, TzHaar-Ket-Rak)
     * only when untagged and right next to their Jad. {@code tilesFromJad} counts from the edge of Jad's body (1 = adjacent).
     */
    public static boolean hurKotHeals(String jadId, double tilesFromJad, boolean tagged) {
        if (JAD.equals(jadId)) {
            return tilesFromJad <= 1 || (tagged && tilesFromJad <= 4);
        }
        return !tagged && tilesFromJad <= 1;
    }

    /** Fight Cave healers that restored Jad to full health come back the next time he drops to half; JalTok-Jad's never do. */
    public static boolean healersRespawn(String jadId) {
        return JAD.equals(jadId);
    }

    // ------------------------------------------------------------------ the Inferno's waves

    /** Hitpoints of each of the Inferno's three rocky supports (OSRS Wiki "Rocky support"). */
    public static final int PILLAR_HP = 255;
    /** Jal-Nib hit a pillar for 2-4 every 4 ticks (2.4 seconds) and always hit (OSRS Wiki "Inferno/Strategies"). */
    public static final int NIBBLER_ATTACK_TICKS = 4;
    public static final int NIBBLER_PILLAR_MIN = 2;
    public static final int NIBBLER_PILLAR_MAX = 4;
    /** The last regular wave: clearing it collapses the remaining pillars. */
    public static final int PILLARS_COLLAPSE_AFTER = 66;

    /** A pillar collapsing deals half the current hitpoints of anyone within one tile (OSRS Wiki "Inferno", wave 66). */
    public static int pillarCollapseDamage(int currentHitpoints) {
        return currentHitpoints / 2;
    }

    /** Jal-Zek: 1 in 10 of its attacks revives a monster slain this wave instead; it attacks again 8 ticks later (Mod Ash). */
    public static final int ZEK_REVIVE_CHANCE = 10;
    public static final int ZEK_REVIVE_PAUSE = 8;

    /** Jal-Nib and the Jal-AkRek splits can't be revived (nor Jads, healers or Zuk); anything else slain this wave can, once. */
    public static boolean revivable(String id) {
        return !id.equals(JAL_NIB) && !id.startsWith("jal_akrek") && !id.startsWith("yt_hurkot") && !id.equals(JALTOK_JAD)
                && !id.equals(ZUK) && !id.equals("jal_mejjak");
    }

    /**
     * Jal-ImKot dig 50 ticks after the wave starts and every 40-60 ticks after that, unless they attacked in the last 15
     * ticks; after resurfacing beside the player they wait 6 ticks before attacking (OSRS Wiki "Jal-ImKot").
     */
    public static final int IMKOT_FIRST_DIG = 50;
    public static final int IMKOT_DIG_MIN = 40;
    public static final int IMKOT_DIG_MAX = 60;
    public static final int IMKOT_ATTACK_LOCK = 15;
    public static final int IMKOT_SURFACE_DELAY = 6;

    /** Jal-Ak read the player's protection prayer 3 ticks before each attack (every 6 ticks) (OSRS Wiki "Jal-Ak"). */
    public static final int BLOB_READ_TICKS = 3;

    /**
     * The style a Jal-Ak attacks with (OSRS Wiki "Jal-Ak"): the one the read prayer doesn't block (magic against Protect from
     * Missiles, ranged against Protect from Magic), random between the two otherwise; next to a player not praying melee it
     * sometimes bites instead. {@code roll} is uniform in [0, 1). // approx: the wiki gives no melee chance (1 in 3 used)
     */
    public static String blobStyle(boolean protectMagic, boolean protectMissiles, boolean protectMelee, boolean adjacent, double roll) {
        if (adjacent && !protectMelee && roll < 1 / 3.0) {
            return "melee";
        }
        if (protectMagic && !protectMissiles) {
            return "ranged";
        }
        if (protectMissiles && !protectMagic) {
            return "magic";
        }
        return roll < 0.5 ? "magic" : "ranged";
    }

    /** Jal-MejRah drain 3 run energy per hit (OSRS Wiki "Jal-MejRah"), in hundredths of a percent. */
    public static final int BAT_RUN_DRAIN = 300;

    /** Wave 68's three JalTok-Jads attack every 9 ticks, each 3 ticks after the previous one (OSRS Wiki "Inferno/Strategies"). */
    public static final int TRIPLE_JAD_SPEED = 9;
    public static final int TRIPLE_JAD_STAGGER = 3;

    // ------------------------------------------------------------------ TzKal-Zuk

    /** TzKal-Zuk's hitpoints. */
    public static final int ZUK_HP = 1200;
    /** Zuk summons a JalTok-Jad (with three healers) at this many hitpoints. */
    public static final int ZUK_JAD_HP = 480;
    /** Four Jal-MejJak arrive to heal Zuk at this many hitpoints, and Zuk enrages. */
    public static final int ZUK_HEALER_HP = 240;
    /** The Jal-Xil + Jal-Zek set timer pauses at this many hitpoints (gaining {@link #ZUK_SET_DELAY}) until the Jad spawns. */
    public static final int ZUK_SET_PAUSE_HP = 600;
    /** Game ticks between Zuk's Jal-Xil + Jal-Zek sets (3:30). */
    public static final int ZUK_SET_TICKS = 350;
    /** One-time addition to the set timer when Zuk reaches 600 hitpoints (1:45). */
    public static final int ZUK_SET_DELAY = 175;
    /** Zuk attacks every 10 ticks, every 7 once enraged at 240 hitpoints (OSRS Wiki "Inferno/Strategies"). */
    public static final int ZUK_SPEED = 10;
    public static final int ZUK_ENRAGED_SPEED = 7;
    /** The Ancestral Glyph holds 600 damage from the monsters that attack it (Zuk's own blasts never break it). */
    public static final int GLYPH_HP = 600;
    /** Each Jal-MejJak heals Zuk for 15-24 every 3 ticks until attacked (OSRS Wiki "Jal-MejJak"). */
    public static final int MEJJAK_HEAL_TICKS = 3;

    public static int mejJakHeal(java.util.Random random) {
        return 15 + random.nextInt(10);
    }

    /** A Jal-MejJak lava ball, once the healer has been attacked: 5-10 damage in a 3x3 area (OSRS Wiki "Jal-MejJak"). */
    public static int mejJakBlast(java.util.Random random) {
        return 5 + random.nextInt(6);
    }

    /**
     * One tick of Zuk's set timer (OSRS Wiki "Inferno/Strategies"): it counts down unless Zuk is at or below 600 hitpoints
     * with his Jad not yet summoned. Returns the new countdown; 0 or less means a Jal-Xil + Jal-Zek set spawns now.
     */
    public static int zukSetTick(int untilSet, int zukHp, boolean jadSpawned) {
        boolean paused = zukHp <= ZUK_SET_PAUSE_HP && !jadSpawned;
        return paused ? untilSet : untilSet - 1;
    }

    /** TzKal-Zuk's real hitpoints from his Minecraft health (capped at {@code maxHealth}, e.g. 1,024 for his 1,200). */
    public static int zukHitpoints(float health, float maxHealth) {
        return maxHealth <= 0 ? 0 : (int) Math.ceil(health * ZUK_HP / maxHealth - 1e-6);
    }
}
