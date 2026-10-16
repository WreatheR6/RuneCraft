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

    /** Tokkul for leaving the Fight Cave early: roughly the tokkul value of the waves completed (approx.). */
    public static int fightCaveForfeitTokkul(int wavesCompleted) {
        if (wavesCompleted <= 0) {
            return 0;
        }
        // Completing every wave pays 8,032 tokkul; partial runs scale quadratically (approx. of the wiki table).
        return (int) Math.round(8032.0 * Math.pow(wavesCompleted / 62.0, 2));
    }

    /** Tokkul for completing the Fight Cave (8,032 plus the fire cape). */
    public static final int FIGHT_CAVE_TOKKUL = 8032;
    /** Tokkul for completing the Inferno (16,440 plus the infernal cape). */
    public static final int INFERNO_TOKKUL = 16440;
    /** Pet chances on completion (1 in N; halved when on a TzHaar slayer task in OSRS). */
    public static final int JAD_PET_CHANCE = 200;
    public static final int ZUK_PET_CHANCE = 100;

    /** Yt-HurKot that tend the Fight Cave's TzTok-Jad once it drops below half hitpoints. */
    public static final int FIGHT_CAVE_JAD_HEALERS = 4;
    /** TzKal-Zuk's hitpoints. */
    public static final int ZUK_HP = 1200;
    /** Zuk summons a JalTok-Jad (with three healers) at this many hitpoints. */
    public static final int ZUK_JAD_HP = 480;
    /** Four Jal-MejJak arrive to heal Zuk at this many hitpoints. */
    public static final int ZUK_HEALER_HP = 240;
    /** The Jal-Xil + Jal-Zek reinforcement timer pauses below this many hitpoints until the Jad dies. */
    public static final int ZUK_SET_PAUSE_HP = 600;
    /** Game ticks between Zuk's Jal-Xil + Jal-Zek reinforcements (3:30). */
    public static final int ZUK_SET_TICKS = 350;

    /** Yt-HurKot each Inferno JalTok-Jad summons below half hitpoints: five on wave 67, three otherwise. */
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
}
