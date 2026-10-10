package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.BestiaryRad.eyes;
import static com.runecraft.osrs.content.bestiary.BestiaryRad.p;
import static com.runecraft.osrs.content.bestiary.BestiaryRad.s;
import static com.runecraft.osrs.content.bestiary.BestiaryRad.shade;
import static com.runecraft.osrs.content.bestiary.BestiaryRad.spikes;
import static com.runecraft.osrs.content.bestiary.BestiaryRad.teeth;

/**
 * The Fight Caves and the Inferno: obsidian-and-magma TzHaar creatures in the same lava-veined rock as
 * TzTok-Jad (which lives in {@link Beasts} and is not redefined here), up to TzKal-Zuk on his throne.
 */
final class BestiaryRadTzhaar {
    private BestiaryRadTzhaar() {
    }

    // Fight Caves palette (Jad's)
    static final int RED = 0xA8231C;
    static final int DEEP = 0x7A1A18;
    static final int DARK = 0x3A1416;
    static final int LAVA = 0xFF8A1E;
    static final int BONE = 0xE8D8B0;
    // Inferno palette: browner, scorched rock
    static final int IRED = 0x8E3A22;
    static final int IDEEP = 0x5E2616;
    static final int IDARK = 0x2C1410;
    static final int ILAVA = 0xFFA030;

    static List<ModelSpec> all() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(bat("tz_kih", 0.8f, RED, DEEP, DARK, LAVA, false));
        l.add(blob("tz_kek", 1.0f, 12, RED, DEEP, DARK, LAVA, false));
        l.add(tokXil());
        l.add(ytMejKot());
        l.add(ketZek());
        l.add(ytHurKot());
        l.add(blob("jal_nib", 0.7f, 9, IRED, IDEEP, IDARK, ILAVA, true));
        l.add(bat("jal_mejrah", 1.1f, IRED, IDEEP, IDARK, ILAVA, true));
        l.add(blob("jal_ak", 1.3f, 14, IRED, IDEEP, IDARK, ILAVA, false));
        l.add(blob("jal_akrek", 0.8f, 10, IRED, IDEEP, IDARK, ILAVA, false));
        l.add(jalImKot());
        l.add(jalXil());
        l.add(jalZek());
        l.add(jalTokJad());
        l.add(jalMejJak());
        l.add(glyph());
        l.add(zuk());
        return l;
    }

    // ------------------------------------------------------------------------------------------
    // Flyers: Tz-Kih and Jal-MejRah
    // ------------------------------------------------------------------------------------------

    /** A lava bat: stout body, eared head with fangs, segmented membrane wings, dangling talons and a tail. */
    static ModelSpec bat(String id, float scale, int red, int deep, int dark, int lava, boolean big) {
        ModelSpec.Builder b = ModelSpec.builder(id).scale(scale).shadow(0.5f * scale);
        p(b, "body", null, Role.FLOAT, red).accent(lava).at(0, 9, 0).mat("rock").glow("cracks")
                .box(-3.5f, -3.5f, -4, 7, 7, 9).add();
        p(b, "chest", "body", deep).accent(lava).at(0, -2.5f, -4).rot(-6, 0, 0).mat("rock")
                .box(-2.5f, 0, -1.2f, 5, 5, 2).add();
        p(b, "belly", "body", dark).accent(lava).at(0, 3.5f, 0).mat("rock").glow("cracks").box(-2.5f, -0.5f, -3, 5, 2, 7).add();
        spikes(b, "back_spikes", "body", deep, "rock", -3.5f, new float[][]{{0, -2, 2}, {0, 1, 3}, {0, 4, 2}}, -20);
        // head
        p(b, "neck", "body", deep).accent(lava).at(0, -2, -4).mat("rock").box(-2, -2, -2, 4, 4, 3).add();
        p(b, "head", "neck", Role.HEAD, red).accent(lava).at(0, -0.5f, -2).mat("rock")
                .box(-3, -3, -4, 6, 5, 5).add();
        p(b, "snout", "head", deep).accent(lava).at(0, 0, -4).mat("rock").box(-2, -1.5f, -2.5f, 4, 3, 3).add();
        p(b, "nose", "snout", dark).at(0, -1.5f, -2.5f).mat("rock").box(-1, -0.5f, -0.6f, 2, 1, 1).add();
        p(b, "jaw", "head", Role.JAW, dark).accent(lava).at(0, 1.5f, -3).rot(10, 0, 0).mat("rock").glow("maw")
                .box(-2, 0, -3.5f, 4, 1, 4).add();
        s(b, "fang", "head", false, BONE).at(1.3f, 1.4f, -6.2f).mat("horn").box(-0.5f, 0, -0.5f, 1, 2, 1).add();
        eyes(b, "eye", "head", lava, 1.6f, -1.5f, -4, 2, 1);
        int ear = big ? 6 : 4;
        s(b, "ear", "head", false, red).accent(lava).at(2, -3, -1).rot(-15, 0, -20).mat("rock")
                .box(-1.5f, -ear, -1, 3, ear, 2).add();
        s(b, "ear_tip", "ear", true, dark).at(0, -ear, 0).rot(-15, 0, 0).mat("rock").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        s(b, "ear_inner", "ear", true, lava).at(0, -1, -1).mat("energy").glow("full").box(-0.5f, -ear + 2, -0.3f, 1, ear - 2, 1).add();
        // wings: arm bone, hand bone, membranes and three finger struts
        int arm = big ? 10 : 8;
        int hand = big ? 9 : 7;
        s(b, "wing", "body", false, deep).roles(Role.WING_LEFT, Role.WING_RIGHT).accent(lava).at(3.5f, -2.5f, -1.5f)
                .rot(0, 0, -10).mat("rock").box(0, -1, -1, arm, 2, 2).add();
        s(b, "wing_claw", "wing", true, BONE).at(arm, -1, -1).mat("horn").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        s(b, "wing_mem", "wing", true, shade(red, 0.75f)).accent(lava).at(0, 0, 0.5f).mat("membrane")
                .box(0, -0.5f, 0, arm, 1, 7).add();
        s(b, "wing_hand", "wing", true, deep).accent(lava).at(arm, 0, 0).rot(0, -18, 8).mat("rock")
                .box(0, -0.5f, -0.5f, hand, 1, 1).add();
        s(b, "wing_mem2", "wing_hand", true, shade(red, 0.7f)).accent(lava).at(0, 0, 0.5f).mat("membrane")
                .box(0, -0.4f, 0, hand, 1, 9).box(0, -0.4f, 9, hand - 3, 1, 2).add();
        s(b, "finger", "wing_hand", true, dark).at(hand, 0, 0).mat("rock")
                .box(-3, -0.6f, 2, 1, 1, 7).box(-6, -0.6f, 4, 1, 1, 6).box(-1, -0.6f, 0, 1, 1, 5).add();
        // dangling legs with talons
        s(b, "leg", "body", false, deep).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(lava).at(2, 3, 2.5f)
                .rot(25, 0, -5).mat("rock").box(-1, 0, -1, 2, 4, 2).add();
        s(b, "talon", "leg", true, BONE).at(0, 4, 0).rot(-30, 0, 0).mat("horn")
                .box(-1, 0, -1.5f, 1, 2, 1).box(0.2f, 0, -1.5f, 1, 2, 1).add();
        p(b, "tail", "body", Role.TAIL, deep).accent(lava).at(0, 0, 5).rot(-10, 0, 0).mat("rock").box(-1.5f, -1.5f, 0, 3, 3, 5).add();
        p(b, "tail_tip", "tail", Role.TAIL, dark).accent(lava).at(0, 0, 5).rot(-15, 0, 0).mat("rock")
                .box(-1, -1, 0, 2, 2, 4).box(-0.5f, -2, 3, 1, 1, 2).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Blobs: Tz-Kek, Jal-Ak, Jal-AkRek and the nibbler
    // ------------------------------------------------------------------------------------------

    /**
     * A domed magma blob: stacked rock tiers rounding the silhouette, a gaping glowing maw full of teeth, brow
     * ridges over small fiery eyes, rocky lumps and stubby clawed feet. {@code nibbler} gives the Jal-Nib its
     * squat, big-jawed look.
     */
    static ModelSpec blob(String id, float scale, int size, int red, int deep, int dark, int lava, boolean nibbler) {
        ModelSpec.Builder b = ModelSpec.builder(id).scale(scale).shadow(0.55f * scale * size / 10f);
        int h = nibbler ? size - 2 : size;
        float cy = 24 - 2.5f;
        p(b, "body", null, Role.BODY, red).accent(lava).at(0, cy, 0).mat("rock").glow("cracks")
                .box(-size / 2f + 1, -h + 1, -size / 2f + 1.5f, size - 2, h - 1, size - 2).add();
        // rounding tiers: a bulging girth, a swollen back and a stepped dome
        int gh = Math.round(h * 0.55f);
        p(b, "girth", "body", deep).accent(lava).at(0, -h * 0.2f, 0.5f).mat("rock").glow("cracks")
                .box(-size / 2f - 0.5f, -gh, -size / 2f + 2.5f, size + 1, gh, size - 4).add();
        p(b, "rump", "body", deep).accent(lava).at(0, -h * 0.15f, size / 2f - 1).rot(-8, 0, 0).mat("rock").glow("cracks")
                .box(-size / 2f + 2, -gh - 1, -0.5f, size - 4, gh + 1, 2).add();
        p(b, "cap", "body", red).accent(lava).at(0, -h + 1, 0.5f).rot(-4, 0, 0).mat("rock").glow("cracks")
                .box(-size / 2f + 2, -2, -size / 2f + 2.5f, size - 4, 2, size - 5).add();
        p(b, "cap2", "cap", deep).accent(lava).at(0, -2, 0.5f).rot(-6, 0, 4).mat("rock")
                .box(-size / 4f, -1.5f, -size / 4f, size / 2, 2, size / 2).add();
        p(b, "skirt", "body", dark).accent(lava).at(0, 0, 0.5f).mat("rock")
                .box(-size / 2f + 1.5f, -1.5f, -size / 2f + 1, size - 3, 2, size - 2).add();
        // maw: heavy upper lip, glowing gullet, a dropping jaw with teeth
        int mw = size - 3;
        float lipY = -h * (nibbler ? 0.62f : 0.55f);
        p(b, "lip", "body", Role.HEAD, red).accent(lava).at(0, lipY, -size / 2f + 1).rot(8, 0, 0).mat("rock")
                .box(-mw / 2f - 0.5f, -2.5f, -2, mw + 1, 3, 3).add();
        p(b, "gullet", "body", lava).at(0, lipY + 0.5f, -size / 2f + 1.2f).mat("lava").glow("full")
                .box(-mw / 2f + 1, 0, -0.4f, mw - 2, (int) Math.max(2, -lipY - 3), 1).add();
        teeth(b, "teeth_top", "lip", BONE, -mw / 2f + 0.5f, mw / 2f - 0.5f, 0.5f, -1.4f, Math.max(3, mw / 2 + 1), 1);
        int jawD = nibbler ? 6 : 4;
        p(b, "jaw", "body", Role.JAW, dark).accent(lava).at(0, -2.5f, -size / 2f + 2).rot(12, 0, 0).mat("rock").glow("maw")
                .box(-mw / 2f, -1, -jawD, mw, 3, jawD + 1).add();
        teeth(b, "teeth_bot", "jaw", BONE, -mw / 2f + 1, mw / 2f - 1, -1, -jawD + 0.6f, Math.max(3, mw / 2), -1);
        if (nibbler) {
            s(b, "tusk", "jaw", false, BONE).at(mw / 2f - 0.8f, -1, -jawD + 0.6f).rot(-10, 0, 10).mat("horn")
                    .box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        }
        // brows and eyes
        s(b, "brow", "body", false, deep).accent(lava).at(size / 4f, lipY - 3.5f, -size / 2f + 1).rot(10, 0, -14).mat("rock")
                .box(-2, -1, -1.5f, 4, 2, 2).add();
        eyes(b, "eye", "body", lava, size / 4f, lipY - 2.2f, -size / 2f + 0.8f, 2, 1);
        // rocky lumps breaking up the dome
        float[][] lumps = {{size / 2f, -h * 0.6f, -1, 3, 20}, {-size / 2f, -h * 0.5f, 2, 3, -25}, {size / 3f, -h, 2, 2, 30},
            {-size / 3f, -h + 0.5f, -1, 2, -20}, {0, -h * 0.7f, size / 2f, 3, 0}};
        for (int i = 0; i < lumps.length; i++) {
            float[] q = lumps[i];
            int sz = (int) q[3];
            p(b, "lump" + i, "body", i % 2 == 0 ? deep : dark).accent(lava).at(q[0], q[1], q[2]).rot(q[4] * 0.6f, q[4], q[4] * 0.8f)
                    .mat("rock").box(-sz / 2f, -sz / 2f, -sz / 2f, sz, sz, sz).add();
        }
        // stubby feet with claws
        float fx = size / 2f - 1.5f;
        float fz = size / 2f - 2.5f;
        s(b, "foot_f", null, false, dark).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(lava).at(fx, 21, -fz)
                .mat("rock").box(-1.5f, 0, -2, 3, 3, 3).add();
        s(b, "foot_b", null, false, dark).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(lava).at(fx, 21, fz)
                .mat("rock").box(-1.5f, 0, -1, 3, 3, 3).add();
        s(b, "claw_f", "foot_f", true, BONE).at(0, 2, -2).mat("horn").box(-1.5f, 0, -1, 1, 1, 1).box(0.5f, 0, -1, 1, 1, 1).add();
        s(b, "claw_b", "foot_b", true, BONE).at(0, 2, -1).mat("horn").box(-1.5f, 0, -1, 1, 1, 1).box(0.5f, 0, -1, 1, 1, 1).add();
        if (id.equals("jal_ak")) {
            // the three spots it splits into (ranged green, magic blue, melee red)
            p(b, "spot_ranged", "body", 0x40D050).at(size / 2f + 0.5f, -h * 0.45f, 1).mat("energy").glow("full")
                    .box(-0.6f, -2, -2, 1, 4, 4).add();
            p(b, "spot_magic", "body", 0x5070F0).at(-size / 2f - 0.5f, -h * 0.45f, 1).mat("energy").glow("full")
                    .box(-0.4f, -2, -2, 1, 4, 4).add();
            p(b, "spot_melee", "body", 0xF03A28).at(0, -h - 0.6f, 2.5f).mat("energy").glow("full")
                    .box(-2, -0.6f, -2, 4, 1, 4).add();
        }
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // TzHaar bipeds
    // ------------------------------------------------------------------------------------------

    static BestiaryRad.Frame tz(String id, float scale, int red, int deep, int dark, int lava) {
        BestiaryRad.Frame f = new BestiaryRad.Frame(id, scale, 0.6f * scale).colors(red, dark, deep, lava).mat("rock");
        f.limbGlow = "cracks";
        f.bodyGlow = "cracks";
        f.clawColor = 0x241012;
        f.clawMat = "rock";
        return f;
    }

    /** Tok-Xil: a hunched ranger bristling with the spines it hurls. */
    static ModelSpec tokXil() {
        BestiaryRad.Frame f = tz("tok_xil", 1.0f, RED, DEEP, DARK, LAVA).legs(6, 5, 4, 3.5f).torso(12, 11, 8, 28)
                .arms(7, 7, 4, 4).head(7, 6, 7);
        f.claws = 3;
        f.clawLen = 2;
        ModelSpec.Builder b = f.build();
        spikes(b, "spines_a", "torso", BONE, "horn", -9, new float[][]{{-4, 1, 5}, {0, 2, 7}, {4, 1, 5}}, -55);
        spikes(b, "spines_b", "torso", BONE, "horn", -5, new float[][]{{-5, 2, 4}, {-1.5f, 3, 6}, {1.5f, 3, 6}, {5, 2, 4}}, -65);
        spikes(b, "spines_c", "torso", BONE, "horn", -1, new float[][]{{-3, 3, 4}, {3, 3, 4}}, -75);
        s(b, "shoulder_spine", "shoulder", true, BONE).at(1, -3, 0).rot(-25, 0, -25).mat("horn")
                .box(-1, -4, -1, 2, 4, 2).box(-0.5f, -6, -0.5f, 1, 2, 1).add();
        s(b, "fore_spine", "forearm", true, BONE).at(2, 2, 1).rot(30, 0, -40).mat("horn").box(-0.5f, -4, -0.5f, 1, 4, 1).add();
        s(b, "fore_plate", "forearm", true, DEEP).accent(LAVA).at(2, 1, 0).mat("rock").glow("cracks").box(0, 0, -2, 1, 5, 4).add();
        p(b, "crest", "head", RED).accent(LAVA).at(0, -4, 0).rot(-30, 0, 0).mat("rock").box(-1, -3, -2, 2, 3, 5).add();
        s(b, "horn", "head", false, BONE).at(2.5f, -4, -2).rot(-35, 0, -25).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        teeth(b, "teeth", "head", BONE, -2.5f, 2.5f, 1.6f, -4.6f, 4, 1);
        s(b, "thigh_plate", "leg", true, DEEP).accent(LAVA).at(2, 0, 0).rot(0, 0, -6).mat("rock").box(0, 0, -2, 1, 5, 4).add();
        return b.build();
    }

    /** Yt-MejKot: a broad, heavy-fisted brute with a glowing magma belly. */
    static ModelSpec ytMejKot() {
        BestiaryRad.Frame f = tz("yt_mejkot", 1.2f, RED, DEEP, DARK, LAVA).legs(6, 5, 5, 4).torso(16, 12, 10, 22)
                .arms(7, 7, 5, 6).head(7, 6, 7);
        f.handW = 7;
        f.handH = 5;
        f.claws = 3;
        f.armSplay = 14;
        f.neckLen = 1;
        ModelSpec.Builder b = f.build();
        p(b, "magma_belly", "torso", LAVA).at(0, -7, -5.2f).mat("lava").glow("full").box(-4, 0, -0.5f, 8, 6, 1).add();
        p(b, "belly_rim", "torso", DARK).accent(LAVA).at(0, -7.5f, -5.4f).mat("rock")
                .box(-5, -1, -0.6f, 10, 1, 1).box(-5, 6, -0.6f, 10, 1, 1).box(-5, 0, -0.6f, 1, 6, 1).box(4, 0, -0.6f, 1, 6, 1).add();
        for (int i = 0; i < 3; i++) {
            p(b, "hump" + i, "torso", i == 1 ? RED : DEEP).accent(LAVA).at(-4 + i * 4, -12, 2).rot(-25, 0, (i - 1) * 15)
                    .mat("rock").glow("cracks").box(-2, -3, -2, 4, 3 + (i == 1 ? 1 : 0), 5).add();
        }
        s(b, "pauldron", "shoulder", true, DEEP).accent(LAVA).at(1, -3, 0).rot(0, 0, -15).mat("rock")
                .box(-2, -2, -4, 7, 2, 8).box(-1, -3.5f, -2, 4, 2, 4).add();
        s(b, "fist_plate", "hand", true, DARK).at(0, 0, 0).mat("rock").box(-4, -1, -4, 8, 2, 8).add();
        s(b, "knuckles", "hand", true, 0x241012).at(0, 5, -3.5f).mat("rock")
                .box(-3.5f, -1, -1, 2, 2, 2).box(-1, -1, -1, 2, 2, 2).box(1.5f, -1, -1, 2, 2, 2).add();
        s(b, "tusk", "jaw", false, BONE).at(2.5f, 0, -3.5f).rot(-10, 0, 8).mat("horn").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        p(b, "crest", "head", RED).accent(LAVA).at(0, -4, -1).rot(-25, 0, 0).mat("rock").box(-1.5f, -2, -2, 3, 2, 6).add();
        s(b, "cheek", "head", false, DEEP).accent(LAVA).at(3.6f, 0, -2).rot(0, -10, 0).mat("rock").box(-1, -2, -2, 2, 4, 4).add();
        return b.build();
    }

    /** Ket-Zek: a tall fire mage with a great swept-back head crest and robe-like hanging plates. */
    static ModelSpec ketZek() {
        BestiaryRad.Frame f = tz("ket_zek", 1.3f, RED, DEEP, DARK, LAVA).legs(7, 6, 4, 3.5f).torso(11, 12, 7, 10)
                .arms(8, 8, 3, 4).head(7, 7, 8);
        f.claws = 3;
        f.clawLen = 3;
        f.elbowBend = 35;
        ModelSpec.Builder b = f.build();
        // crest: a broad fan sweeping back from the brow
        p(b, "crest", "head", RED).accent(LAVA).at(0, -5, -2).rot(-40, 0, 0).mat("rock").glow("cracks")
                .box(-3, -6, -1, 6, 6, 3).add();
        p(b, "crest2", "crest", DEEP).accent(LAVA).at(0, -6, 0.5f).rot(-20, 0, 0).mat("rock").box(-2, -5, -1, 4, 5, 2).add();
        p(b, "crest3", "crest2", DARK).at(0, -5, 0).rot(-15, 0, 0).mat("rock").box(-1, -3, -0.5f, 2, 3, 1).add();
        s(b, "crest_fin", "head", false, RED).accent(LAVA).at(3, -4, 0).rot(-45, 10, -25).mat("rock")
                .box(-0.5f, -6, -1.5f, 1, 6, 3).add();
        teeth(b, "teeth", "head", BONE, -2.5f, 2.5f, 1.6f, -6.6f, 4, 1);
        // hanging plate skirt
        for (int i = 0; i < 4; i++) {
            float x = -4.5f + i * 3;
            p(b, "tasset" + i, "hips", i % 2 == 0 ? DEEP : DARK).accent(LAVA).at(x, 0, -3.2f).rot(-8, 0, (x) * 2)
                    .mat("rock").glow("cracks").box(-1.5f, 0, -0.6f, 3, 7, 1).add();
        }
        p(b, "tail_plate", "hips", DEEP).accent(LAVA).at(0, 0, 3.3f).rot(10, 0, 0).mat("rock").box(-4, 0, -0.5f, 8, 7, 1).add();
        // the fire it conjures between its claws
        p(b, "fireball", "hand_r", Role.SPIN, LAVA).at(-1, 6, -2).mat("lava").glow("full").box(-2, -2, -2, 4, 4, 4).add();
        p(b, "fire_core", "fireball", 0xFFE070).at(0, 0, 0).mat("energy").glow("full").box(-1, -3, -1, 2, 6, 2).add();
        s(b, "collar", "torso", false, DEEP).accent(LAVA).at(3, -12, -1).rot(-10, 0, -30).mat("rock").box(-1, -3, -2, 2, 3, 5).add();
        return b.build();
    }

    /** Yt-HurKot: Jad's small hunched healer, its claws aglow with restorative magic. */
    static ModelSpec ytHurKot() {
        BestiaryRad.Frame f = tz("yt_hurkot", 0.9f, 0xB23A1E, DEEP, DARK, 0xFFD060).legs(5, 5, 4, 3).torso(10, 10, 7, 30)
                .arms(6, 7, 3, 4).head(7, 6, 7);
        f.claws = 3;
        ModelSpec.Builder b = f.build();
        s(b, "heal_glow", "hand", true, 0xFFC040).at(0, 3, -1).mat("energy").glow("full").box(-1, 0, -1, 2, 2, 2).add();
        p(b, "heart", "torso", 0xFFE070).at(0, -7, -3.8f).mat("energy").glow("full").box(-1.5f, 0, -0.5f, 3, 3, 1).add();
        spikes(b, "ridge", "torso", DEEP, "rock", -10, new float[][]{{0, 0, 3}, {0, 2.5f, 3}}, -50);
        s(b, "ear", "head", false, RED).accent(LAVA).at(3, -3, 0).rot(-20, 0, -40).mat("rock").box(-1, -3, -1, 2, 3, 2).add();
        teeth(b, "teeth", "head", BONE, -2, 2, 1.6f, -4.6f, 3, 1);
        return b.build();
    }

    /** Jal-Xil: a lanky Inferno ranger whose back, shoulders and arms bristle with long spines. */
    static ModelSpec jalXil() {
        BestiaryRad.Frame f = tz("jal_xil", 1.3f, IRED, IDEEP, IDARK, ILAVA).legs(8, 7, 4, 3.5f).torso(11, 12, 7, 18)
                .arms(8, 8, 3, 4).head(7, 6, 8);
        f.claws = 3;
        f.clawLen = 3;
        ModelSpec.Builder b = f.build();
        spikes(b, "spines_a", "torso", BONE, "horn", -10, new float[][]{{-4, 1, 6}, {0, 2, 8}, {4, 1, 6}}, -50);
        spikes(b, "spines_b", "torso", BONE, "horn", -6, new float[][]{{-4.5f, 2, 5}, {-1.5f, 3, 7}, {1.5f, 3, 7}, {4.5f, 2, 5}}, -62);
        spikes(b, "spines_c", "torso", BONE, "horn", -2, new float[][]{{-2.5f, 3, 5}, {2.5f, 3, 5}}, -72);
        s(b, "shoulder_spine", "shoulder", true, BONE).at(1, -3, 0).rot(-20, 0, -30).mat("horn")
                .box(-1, -5, -1, 2, 5, 2).box(-0.5f, -8, -0.5f, 1, 3, 1).add();
        s(b, "arm_spine", "forearm", true, BONE).at(2, 2, 1).rot(40, 0, -35).mat("horn")
                .box(-0.5f, -5, -0.5f, 1, 5, 1).box(-0.5f, -4, 2, 1, 4, 1).add();
        p(b, "crest", "head", IRED).accent(ILAVA).at(0, -4, 0).rot(-35, 0, 0).mat("rock").box(-1, -4, -2, 2, 4, 6).add();
        s(b, "head_spine", "head", false, BONE).at(2.5f, -4, 0).rot(-50, 0, -20).mat("horn").box(-0.5f, -5, -0.5f, 1, 5, 1).add();
        teeth(b, "teeth", "head", BONE, -2.5f, 2.5f, 1.6f, -5.6f, 4, 1);
        p(b, "tail", "hips", Role.TAIL, IDEEP).accent(ILAVA).at(0, -1, 3).rot(30, 0, 0).mat("rock").box(-1.5f, -1.5f, 0, 3, 3, 6).add();
        p(b, "tail_tip", "tail", Role.TAIL, BONE).at(0, 0, 6).rot(15, 0, 0).mat("horn").box(-0.5f, -0.5f, 0, 1, 1, 4).add();
        return b.build();
    }

    /** Jal-Zek: the Inferno's towering mage, with a long crested skull and fire cupped in its claws. */
    static ModelSpec jalZek() {
        BestiaryRad.Frame f = tz("jal_zek", 1.5f, IRED, IDEEP, IDARK, ILAVA).legs(8, 7, 5, 4).torso(13, 13, 8, 12)
                .arms(9, 8, 4, 4).head(7, 8, 9);
        f.claws = 3;
        f.clawLen = 3;
        f.elbowBend = 40;
        ModelSpec.Builder b = f.build();
        p(b, "skull_back", "head", IDEEP).accent(ILAVA).at(0, -5, 2).rot(-30, 0, 0).mat("rock").glow("cracks")
                .box(-3, -3, -1, 6, 4, 7).add();
        p(b, "skull_crest", "skull_back", IRED).accent(ILAVA).at(0, -3, 3).rot(-15, 0, 0).mat("rock").box(-1, -4, -3, 2, 4, 8).add();
        s(b, "horn", "head", false, BONE).at(3, -5, -3).rot(-30, 0, -30).mat("horn")
                .box(-1, -4, -1, 2, 4, 2).box(-0.5f, -7, -0.5f, 1, 3, 1).add();
        teeth(b, "teeth", "head", BONE, -2.5f, 2.5f, 1.6f, -7.6f, 4, 1);
        for (int i = 0; i < 5; i++) {
            float x = -5 + i * 2.5f;
            p(b, "robe" + i, "hips", i % 2 == 0 ? IDEEP : IDARK).accent(ILAVA).at(x, 0, -3.6f).rot(-6, 0, x * 1.5f)
                    .mat("rock").glow("cracks").box(-1.25f, 0, -0.6f, 3, 8, 1).add();
        }
        p(b, "robe_back", "hips", IDEEP).accent(ILAVA).at(0, 0, 3.6f).rot(8, 0, 0).mat("rock").box(-5, 0, -0.5f, 10, 9, 1).add();
        p(b, "flame", "hand_r", Role.SPIN, ILAVA).at(0, 7, -2).mat("lava").glow("full").box(-2, -2, -2, 4, 4, 4).add();
        p(b, "flame_l", "hand_l", Role.SPIN, ILAVA).at(0, 7, -2).mat("lava").glow("full").box(-1.5f, -1.5f, -1.5f, 3, 3, 3).add();
        s(b, "mantle", "torso", false, IDEEP).accent(ILAVA).at(4, -13, 0).rot(0, 0, -25).mat("rock").glow("cracks")
                .box(-2, -2, -4, 6, 3, 8).add();
        return b.build();
    }

    /** Jal-MejJak: the small floating healers that tend TzKal-Zuk. */
    static ModelSpec jalMejJak() {
        BestiaryRad.Frame f = tz("jal_mejjak", 0.9f, IRED, IDEEP, IDARK, 0xFFD070).legs(5, 5, 4, 3).torso(10, 10, 7, 25)
                .arms(6, 6, 3, 4).head(7, 6, 7);
        ModelSpec.Builder b = f.build();
        p(b, "cowl", "head", IDEEP).accent(ILAVA).at(0, -4, 0.5f).mat("rock").box(-4, -1.5f, -4.5f, 8, 2, 8).box(-3.5f, 0, 2, 7, 4, 2).add();
        p(b, "orb", "hand_r", Role.SPIN, 0xFFE070).at(0, 5, -1).mat("energy").glow("full").box(-1.5f, -1.5f, -1.5f, 3, 3, 3).add();
        p(b, "heart", "torso", 0xFFE070).at(0, -7, -3.8f).mat("energy").glow("full").box(-1.5f, 0, -0.5f, 3, 3, 1).add();
        spikes(b, "ridge", "torso", IDEEP, "rock", -10, new float[][]{{-2, 0, 3}, {2, 0, 3}, {0, 3, 3}}, -50);
        teeth(b, "teeth", "head", BONE, -2, 2, 1.6f, -4.6f, 3, 1);
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Quadrupeds
    // ------------------------------------------------------------------------------------------

    /** Jal-ImKot: the burrowing melee beast - a low, broad back of plates and huge digging claws. */
    static ModelSpec jalImKot() {
        ModelSpec.Builder b = ModelSpec.builder("jal_imkot").scale(1.4f).shadow(1.0f);
        p(b, "body", null, Role.BODY, IRED).accent(ILAVA).at(0, 13, 1).rot(-8, 0, 0).mat("rock").glow("cracks")
                .box(-6, -5, -8, 12, 10, 16).add();
        for (int i = 0; i < 4; i++) {
            p(b, "plate" + i, "body", i % 2 == 0 ? IDEEP : IRED).accent(ILAVA).at(0, -5, -6 + i * 4).rot(-12, 0, 0).mat("rock")
                    .glow("cracks").box(-6.5f + i * 0.5f, -2, -1, 13 - i, 2, 5).add();
        }
        spikes(b, "spines", "body", BONE, "horn", -7, new float[][]{{0, -6, 3}, {0, -2, 4}, {0, 2, 4}, {0, 6, 3}}, -30);
        p(b, "belly", "body", IDARK).accent(ILAVA).at(0, 5, 0).mat("rock").box(-5, -1, -7, 10, 2, 14).add();
        p(b, "neck", "body", IDEEP).accent(ILAVA).at(0, -1, -8).mat("rock").box(-4, -3, -3, 8, 6, 4).add();
        p(b, "head", "neck", Role.HEAD, IRED).accent(ILAVA).at(0, 0, -3).rot(8, 0, 0).mat("rock").pattern("eyes").glow("eyes")
                .box(-4, -3.5f, -6, 8, 6, 6).add();
        p(b, "brow", "head", IDEEP).accent(ILAVA).at(0, -3, -6).rot(15, 0, 0).mat("rock").box(-4.5f, -1, -1, 9, 2, 2).add();
        p(b, "snout", "head", IDEEP).accent(ILAVA).at(0, 0.5f, -6).mat("rock").box(-3, -1.5f, -2.5f, 6, 3, 3).add();
        p(b, "jaw", "head", Role.JAW, IDARK).accent(ILAVA).at(0, 2.5f, -4).rot(10, 0, 0).mat("rock").glow("maw")
                .box(-3.5f, 0, -4.5f, 7, 2, 5).add();
        teeth(b, "teeth", "snout", BONE, -2.5f, 2.5f, 1.5f, -2.2f, 4, 1);
        s(b, "horn", "head", false, BONE).at(3, -3, -3).rot(-40, 0, -30).mat("horn").box(-1, -3, -1, 2, 3, 2)
                .box(-0.5f, -5, -0.5f, 1, 2, 1).add();
        // front: massive digging arms
        s(b, "arm", "body", false, IRED).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(ILAVA).at(6, 0, -5)
                .rot(-15, 0, -15).mat("rock").glow("cracks").box(-2.5f, -2, -3, 5, 7, 6).add();
        s(b, "forearm", "arm", true, IDEEP).accent(ILAVA).at(0, 5, 0).rot(25, 0, 10).mat("rock").glow("cracks")
                .box(-2.5f, 0, -3, 5, 5, 6).add();
        s(b, "paw", "forearm", true, IDARK).at(0, 5, 0).rot(-10, 0, 0).mat("rock").box(-3, 0, -4, 6, 2, 6).add();
        s(b, "digclaw", "paw", true, BONE).at(0, 1, -4).rot(30, 0, 0).mat("horn")
                .box(-3, -0.5f, -3, 1, 1, 3).box(-1, -0.5f, -3.5f, 1, 1, 4).box(1, -0.5f, -3.5f, 1, 1, 4).box(2.5f, -0.5f, -2.5f, 1, 1, 3).add();
        s(b, "leg", "body", false, IDEEP).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(ILAVA).at(5, 2, 5)
                .rot(20, 0, -8).mat("rock").glow("cracks").box(-2, -1, -2.5f, 4, 6, 5).add();
        s(b, "hind", "leg", true, IDARK).at(0, 5, 0).rot(-30, 0, 0).mat("rock").box(-1.5f, 0, -1.5f, 3, 5, 3).add();
        s(b, "hind_foot", "hind", true, IDARK).at(0, 4.5f, 0).rot(15, 0, 0).mat("rock").box(-2, 0, -3, 4, 2, 4).add();
        s(b, "hind_claw", "hind_foot", true, BONE).at(0, 1, -3).mat("horn").box(-1.5f, -0.5f, -1, 1, 1, 1).box(0.5f, -0.5f, -1, 1, 1, 1).add();
        p(b, "tail", "body", Role.TAIL, IDEEP).accent(ILAVA).at(0, -2, 8).rot(-10, 0, 0).mat("rock").box(-2, -2, 0, 4, 4, 6).add();
        p(b, "tail2", "tail", Role.TAIL, IDARK).accent(ILAVA).at(0, 0, 6).rot(10, 0, 0).mat("rock").box(-1.5f, -1.5f, 0, 3, 3, 5)
                .box(-0.5f, -3, 2, 1, 2, 2).add();
        return b.build();
    }

    /** JalTok-Jad: the Inferno's Jad, a molten four-legged beast with a towering crest and blazing maw. */
    static ModelSpec jalTokJad() {
        int red = 0xB8401A;
        int deep = 0x7E2412;
        int dark = 0x341210;
        int lava = 0xFFB040;
        ModelSpec.Builder b = ModelSpec.builder("jaltok_jad").scale(1.8f).shadow(1.2f);
        p(b, "body", null, Role.BODY, red).accent(lava).at(0, 11, 1).rot(-10, 0, 0).mat("rock").glow("cracks")
                .box(-7, -7, -9, 14, 12, 18).add();
        p(b, "chest", "body", deep).accent(lava).at(0, -3, -9).rot(-8, 0, 0).mat("rock").glow("cracks").box(-6, -3, -2, 12, 9, 3).add();
        p(b, "haunch", "body", deep).accent(lava).at(0, -6, 6).rot(12, 0, 0).mat("rock").box(-6.5f, -2, -3, 13, 4, 7).add();
        for (int i = 0; i < 4; i++) {
            p(b, "ridge" + i, "body", i % 2 == 0 ? red : deep).accent(lava).at(0, -7, -7 + i * 4.5f).rot(-30, 0, 0).mat("rock")
                    .box(-1.5f, -4 - (i == 0 ? 1 : 0), -1.5f, 3, 4 + (i == 0 ? 1 : 0), 3).add();
        }
        s(b, "shoulder", "body", false, red).accent(lava).at(6.5f, -5, -6).rot(-10, 0, -15).mat("rock").glow("cracks")
                .box(-2, -3, -4, 6, 7, 8).add();
        s(b, "shoulder_crest", "shoulder", true, deep).accent(lava).at(1.5f, -3, 0).rot(-30, 0, -20).mat("rock")
                .box(-1.5f, -4, -2, 3, 4, 4).add();
        // neck and the great crested head
        p(b, "neck", "body", Role.NECK, red).accent(lava).at(0, -4, -8).rot(58, 0, 0).mat("rock").glow("cracks")
                .box(-3.5f, -8, -3.5f, 7, 9, 7).add();
        p(b, "mane", "neck", lava).at(0, -8, 3).mat("lava").glow("full").box(-1, 0, -0.5f, 2, 8, 2).add();
        p(b, "head", "neck", Role.HEAD, red).accent(lava).at(0, -8, 0).rot(-62, 0, 0).mat("rock").pattern("eyes").glow("eyes")
                .box(-4.5f, -4, -8, 9, 7, 9).add();
        p(b, "snout", "head", deep).accent(lava).at(0, 0, -8).mat("rock").box(-3, -2.5f, -3, 6, 4, 3).add();
        p(b, "brow", "head", red).accent(lava).at(0, -3.5f, -8).rot(15, 0, 0).mat("rock").box(-5, -1.5f, -1.5f, 10, 3, 3).add();
        p(b, "jaw", "head", Role.JAW, dark).accent(lava).at(0, 2.5f, -5).rot(14, 0, 0).mat("rock").glow("maw")
                .box(-4, 0, -6, 8, 2, 7).add();
        teeth(b, "teeth", "snout", BONE, -2.5f, 2.5f, 1.5f, -2.7f, 5, 1);
        s(b, "tusk", "jaw", false, BONE).at(3.3f, 0, -5.5f).rot(-10, 0, 10).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        p(b, "crest", "head", red).accent(lava).at(0, -4, -2).rot(-30, 0, 0).mat("rock").glow("cracks").box(-1.5f, -8, -2, 3, 8, 6).add();
        p(b, "crest_tip", "crest", deep).accent(lava).at(0, -8, 1).rot(-25, 0, 0).mat("rock").box(-1, -5, -2, 2, 5, 4).add();
        s(b, "horn", "head", false, red).accent(lava).at(3.5f, -4, -3).rot(-35, -10, -25).mat("rock").box(-1.5f, -5, -1.5f, 3, 5, 3).add();
        s(b, "horn_tip", "horn", true, dark).at(0, -5, 0).rot(-30, 0, 0).mat("rock").box(-1, -4, -1, 2, 4, 2).add();
        s(b, "fin", "head", false, deep).accent(lava).at(4, -1, 1).rot(-20, 20, -40).mat("rock").box(-0.5f, -6, -2, 1, 6, 4).add();
        // legs
        s(b, "leg_f", "body", false, red).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(lava).at(5, 2, -6)
                .rot(-5, 0, -6).mat("rock").glow("cracks").box(-2.5f, -1, -2.5f, 5, 7, 5).add();
        s(b, "shin_f", "leg_f", true, deep).accent(lava).at(0, 6, 0).rot(8, 0, 3).mat("rock").glow("cracks")
                .box(-2, 0, -2, 4, 6, 4).add();
        s(b, "foot_f", "shin_f", true, dark).at(0, 5.5f, 0).rot(-2, 0, 0).mat("rock").box(-3, 0, -4, 6, 2, 6).add();
        s(b, "toe_f", "foot_f", true, 0x241012).at(0, 1, -4).mat("rock").box(-3, -0.5f, -1, 2, 2, 2).box(-1, -0.5f, -1.5f, 2, 2, 2)
                .box(1, -0.5f, -1, 2, 2, 2).add();
        s(b, "leg_b", "body", false, red).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(lava).at(5, 2, 6)
                .rot(-25, 0, -6).mat("rock").glow("cracks").box(-3, -2, -3, 6, 7, 6).add();
        s(b, "shin_b", "leg_b", true, deep).accent(lava).at(0, 5, 0).rot(40, 0, 3).mat("rock").glow("cracks")
                .box(-2, 0, -2, 4, 6, 4).add();
        s(b, "foot_b", "shin_b", true, dark).at(0, 5.5f, 0).rot(-8, 0, 0).mat("rock").box(-3, 0, -4, 6, 2, 6).add();
        s(b, "toe_b", "foot_b", true, 0x241012).at(0, 1, -4).mat("rock").box(-3, -0.5f, -1, 2, 2, 2).box(1, -0.5f, -1, 2, 2, 2).add();
        p(b, "tail", "body", Role.TAIL, deep).accent(lava).at(0, -4, 9).rot(-20, 0, 0).mat("rock").glow("cracks").box(-2, -2, 0, 4, 4, 7).add();
        p(b, "tail2", "tail", Role.TAIL, dark).accent(lava).at(0, 0, 7).rot(15, 0, 0).mat("rock").box(-1.5f, -1.5f, 0, 3, 3, 6).add();
        p(b, "tail_spike", "tail2", BONE).at(0, -1.5f, 5).rot(-30, 0, 0).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Inferno set pieces
    // ------------------------------------------------------------------------------------------

    /** The Ancestral Glyph: a great floating obsidian shield carved with a blazing rune. */
    static ModelSpec glyph() {
        ModelSpec.Builder b = ModelSpec.builder("ancestral_glyph").scale(1.0f).shadow(0.8f);
        p(b, "slab", null, Role.FLOAT, IDEEP).accent(ILAVA).at(0, 7, 0).mat("rock").glow("cracks").box(-9, -13, -1.5f, 18, 26, 3).add();
        // stepped rim
        p(b, "rim", "slab", IDARK).accent(ILAVA).at(0, 0, 0).mat("rock")
                .box(-11, -12, -2, 2, 24, 4).box(9, -12, -2, 2, 24, 4).box(-8, -15, -2, 16, 2, 4).box(-8, 13, -2, 16, 2, 4).add();
        p(b, "corners", "slab", IRED).accent(ILAVA).at(0, 0, 0).mat("rock")
                .box(-11, -15, -2.5f, 3, 3, 5).box(8, -15, -2.5f, 3, 3, 5).box(-11, 12, -2.5f, 3, 3, 5).box(8, 12, -2.5f, 3, 3, 5).add();
        p(b, "spike_top", "slab", IRED).accent(ILAVA).at(0, -15, 0).mat("rock").box(-1.5f, -4, -1.5f, 3, 4, 3).box(-0.5f, -6, -0.5f, 1, 2, 1).add();
        p(b, "spike_bot", "slab", IRED).accent(ILAVA).at(0, 15, 0).mat("rock").box(-1.5f, 0, -1.5f, 3, 4, 3).box(-0.5f, 4, -0.5f, 1, 2, 1).add();
        s(b, "side_fin", "slab", false, IRED).accent(ILAVA).at(11, 0, 0).mat("rock").box(0, -6, -1, 2, 12, 2).box(2, -3, -0.5f, 2, 6, 1).add();
        // the rune: a ringed eye with rays, front and back
        for (int side = 0; side < 2; side++) {
            float z = side == 0 ? -1.9f : 1.9f;
            String n = side == 0 ? "rune_f" : "rune_b";
            p(b, n, "slab", ILAVA).at(0, 0, z).mat("energy").glow("full")
                    .box(-4, -6, -0.5f, 8, 1, 1).box(-4, 5, -0.5f, 8, 1, 1).box(-5, -5, -0.5f, 1, 10, 1).box(4, -5, -0.5f, 1, 10, 1)
                    .box(-1.5f, -2, -0.6f, 3, 4, 1).box(-0.5f, -11, -0.5f, 1, 4, 1).box(-0.5f, 7, -0.5f, 1, 4, 1)
                    .box(-8, -0.5f, -0.5f, 3, 1, 1).box(5, -0.5f, -0.5f, 3, 1, 1).add();
        }
        p(b, "embers", "slab", Role.SPIN, ILAVA).at(0, 16, 0).mat("lava").glow("full")
                .box(-6, 4, -6, 2, 2, 2).box(4, 5, 4, 2, 2, 2).box(5, 3, -5, 1, 1, 1).box(-5, 6, 5, 1, 1, 1).add();
        return b.build();
    }

    /**
     * TzKal-Zuk: the colossal king of the Inferno, risen waist-deep from his throne of obsidian - vast plated
     * shoulders, arms braced on the throne, a blazing heart and a towering fan of a crown.
     */
    static ModelSpec zuk() {
        int red = 0xB0381C;
        int deep = 0x6E2014;
        int dark = 0x2E1210;
        int lava = 0xFF9A28;
        int gold = 0x3A1810;
        ModelSpec.Builder b = ModelSpec.builder("tzkal_zuk").scale(2.6f).shadow(1.6f);
        // throne
        p(b, "throne", null, dark).accent(lava).at(0, 24, 2).mat("rock").glow("cracks").box(-12, -6, -7, 24, 6, 14).add();
        p(b, "throne_step", "throne", deep).accent(lava).at(0, 0, -7).mat("rock").box(-13, -3, -2, 26, 3, 3).box(-11, -5, -1, 22, 2, 2).add();
        s(b, "throne_arm", "throne", false, deep).accent(lava).at(11, -6, 0).mat("rock").glow("cracks")
                .box(-2, -4, -6, 5, 4, 12).box(-1, -6, -6, 4, 2, 3).add();
        p(b, "throne_back", "throne", dark).accent(lava).at(0, -6, 6).rot(8, 0, 0).mat("rock").box(-11, -18, -1, 22, 18, 3).add();
        s(b, "throne_spire", "throne_back", false, deep).accent(lava).at(9, -18, 0.5f).rot(0, 0, -10).mat("rock")
                .box(-1.5f, -8, -1.5f, 3, 8, 3).box(-0.5f, -11, -0.5f, 1, 3, 1).add();
        p(b, "throne_glow", "throne", lava).at(0, -3, -7.3f).mat("lava").glow("full").box(-9, -1, -0.5f, 18, 1, 1).add();
        // body
        p(b, "body", "throne", Role.BODY, red).accent(lava).at(0, -6, 0).rot(6, 0, 0).mat("rock").glow("cracks")
                .box(-8, -15, -5, 16, 15, 10).add();
        p(b, "abs", "body", deep).accent(lava).at(0, -6, -5).mat("rock")
                .box(-4, 0, -1, 3, 2, 2).box(1, 0, -1, 3, 2, 2).box(-4, 2.5f, -1, 3, 2, 2).box(1, 2.5f, -1, 3, 2, 2).add();
        p(b, "heart", "body", lava).at(0, -11, -5.2f).mat("energy").glow("full").box(-3, -2, -0.6f, 6, 5, 1).add();
        p(b, "heart_rim", "body", dark).accent(lava).at(0, -11, -5.5f).mat("rock")
                .box(-4, -3, -0.6f, 8, 1, 1).box(-4, 3, -0.6f, 8, 1, 1).box(-4, -2, -0.6f, 1, 5, 1).box(3, -2, -0.6f, 1, 5, 1).add();
        p(b, "pecs", "body", red).accent(lava).at(0, -14, -5).rot(-6, 0, 0).mat("rock").box(-7, 0, -1.5f, 5, 3, 2).box(2, 0, -1.5f, 5, 3, 2).add();
        // shoulders: layered pauldron plates with spikes
        s(b, "shoulder", "body", false, red).accent(lava).at(8, -13, 0).rot(0, 0, -15).mat("rock").glow("cracks")
                .box(-2, -4, -5, 8, 7, 10).add();
        s(b, "pauldron", "shoulder", true, deep).accent(lava).at(2, -4, 0).rot(0, 0, -10).mat("rock")
                .box(-3, -2, -5.5f, 8, 2, 11).box(-1, -3.5f, -4, 6, 2, 8).add();
        s(b, "pauldron_spike", "pauldron", true, dark).at(2, -3.5f, 0).rot(0, 0, -25).mat("rock")
                .box(-1, -4, -3, 2, 4, 2).box(-1, -5, 1, 2, 5, 2).add();
        // arms braced on the throne arms
        s(b, "arm", "body", false, red).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(lava).at(11, -10, 0).rot(-25, 0, -12)
                .mat("rock").glow("cracks").box(-2.5f, -2, -2.5f, 5, 9, 5).add();
        s(b, "forearm", "arm", true, deep).accent(lava).at(0, 8, 0).rot(-40, 0, 10).mat("rock").glow("cracks")
                .box(-3, 0, -3, 6, 7, 6).add();
        s(b, "bracer", "forearm", true, dark).accent(lava).at(0, 3, 0).mat("rock").box(-3.5f, 0, -3.5f, 7, 2, 7).add();
        s(b, "fist", "forearm", true, dark).accent(lava).at(0, 7, 0).rot(10, 0, 0).mat("rock").box(-3, 0, -3.5f, 6, 4, 6).add();
        s(b, "knuckles", "fist", true, 0x241012).at(0, 3.5f, -3.5f).mat("rock").box(-3, -1, -1, 2, 2, 2).box(-1, -1, -1, 2, 2, 2)
                .box(1, -1, -1, 2, 2, 2).add();
        // head and crown
        p(b, "neck", "body", deep).accent(lava).at(0, -15, -1).mat("rock").box(-3, -3, -3, 6, 4, 6).add();
        p(b, "head", "neck", Role.HEAD, red).accent(lava).at(0, -3, 0).mat("rock").pattern("eyes").glow("eyes")
                .box(-5, -9, -5, 10, 9, 10).add();
        p(b, "brow", "head", deep).accent(lava).at(0, -6, -5).rot(12, 0, 0).mat("rock").box(-5, -1.5f, -1.5f, 10, 2, 2).add();
        p(b, "jaw", "head", Role.JAW, dark).accent(lava).at(0, -1, -3).rot(5, 0, 0).mat("rock").glow("maw").box(-3.5f, 0, -2.5f, 7, 3, 4).add();
        teeth(b, "teeth", "head", BONE, -3, 3, -0.6f, -4.9f, 5, 1);
        s(b, "cheek", "head", false, deep).accent(lava).at(4.5f, -3, -2).rot(0, -10, 0).mat("rock").box(-1, -3, -2.5f, 2, 5, 5).add();
        p(b, "crown_band", "head", gold).accent(lava).at(0, -9, 0).mat("rock").glow("cracks").box(-5.5f, -2, -5.5f, 11, 2, 11).add();
        // a radiating hood of seven obsidian fins behind the head, like a sunburst
        float[] rolls = {0, 28, 56, 82};
        for (int k = 0; k < rolls.length; k++) {
            int len = k == 0 ? 13 : k == 1 ? 12 : k == 2 ? 10 : 7;
            int col = k % 2 == 0 ? red : deep;
            if (k == 0) {
                p(b, "fin0", "crown_band", col).accent(lava).at(0, -1, 3).rot(-12, 0, 0).mat("rock").glow("cracks")
                        .box(-2, -len, -1, 4, len, 2).box(-1, -len - 3, -0.5f, 2, 3, 1).add();
            } else {
                s(b, "fin" + k, "crown_band", false, col).accent(lava).at(1, -1, 3).rot(-12, 0, -rolls[k]).mat("rock")
                        .glow("cracks").box(-1.5f, -len, -1, 3, len, 2).box(-0.5f, -len - 2, -0.5f, 1, 2, 1).add();
            }
        }
        p(b, "hood_web", "crown_band", dark).accent(lava).at(0, -1, 3.8f).rot(-12, 0, 0).mat("rock").glow("cracks")
                .box(-8, -9, -0.5f, 16, 9, 1).box(-5, -12, -0.5f, 10, 3, 1).add();
        p(b, "crown_gem", "crown_band", lava).at(0, -1, -5.2f).mat("energy").glow("full").box(-1, -1.5f, -0.5f, 2, 2, 1).add();
        return b.build();
    }
}
