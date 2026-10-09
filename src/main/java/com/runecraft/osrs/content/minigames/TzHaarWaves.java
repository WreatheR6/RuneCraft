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
 * <p>The Inferno has 69 waves: three Jal-Nib nibblers on every regular wave, bats and blobs on
 * waves 1-8, Jal-ImKot from wave 9, Jal-Xil from wave 18, Jal-Zek from wave 35, JalTok-Jad on
 * wave 67, three JalTok-Jads on wave 68 and TzKal-Zuk on wave 69. Each block reuses the earlier
 * waves the way the real Inferno does; individual compositions are approximations.
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

        // Inferno: waves 1-8 of bats and blobs.
        List<List<String>> base = new ArrayList<>();
        base.add(List.of(JAL_MEJRAH));
        base.add(List.of(JAL_MEJRAH, JAL_MEJRAH));
        base.add(List.of(JAL_AK));
        base.add(List.of(JAL_AK, JAL_MEJRAH));
        base.add(List.of(JAL_AK, JAL_MEJRAH, JAL_MEJRAH));
        base.add(List.of(JAL_AK, JAL_AK));
        base.add(List.of(JAL_AK, JAL_AK, JAL_MEJRAH));
        base.add(List.of(JAL_AK, JAL_AK, JAL_MEJRAH, JAL_MEJRAH));
        List<List<String>> inferno = new ArrayList<>(base);
        // Waves 9-17: a Jal-ImKot alone, then with each of waves 1-8.
        block(inferno, JAL_IMKOT, 9);
        // Waves 18-34: a Jal-Xil alone, then with each of waves 1-16.
        block(inferno, JAL_XIL, 17);
        // Waves 35-66: a Jal-Zek alone, then with each of waves 1-31.
        block(inferno, JAL_ZEK, 32);
        List<List<String>> withNibblers = new ArrayList<>();
        for (List<String> w : inferno) {
            List<String> full = new ArrayList<>(List.of(JAL_NIB, JAL_NIB, JAL_NIB));
            full.addAll(w);
            withNibblers.add(List.copyOf(full));
        }
        withNibblers.add(List.of(JALTOK_JAD));
        withNibblers.add(List.of(JALTOK_JAD, JALTOK_JAD, JALTOK_JAD));
        withNibblers.add(List.of(ZUK));
        INFERNO = Collections.unmodifiableList(withNibblers);
    }

    private static void block(List<List<String>> waves, String top, int count) {
        List<List<String>> before = new ArrayList<>(waves);
        waves.add(List.of(top));
        for (int i = 0; i < count - 1; i++) {
            List<String> w = new ArrayList<>();
            w.add(top);
            w.addAll(before.get(i));
            waves.add(List.copyOf(w));
        }
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
}
