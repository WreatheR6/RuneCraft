package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.BestiaryBst.dark;
import static com.runecraft.osrs.content.bestiary.BestiaryBst.light;
import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/** Quest birds: the chompy bird, the warped terrorbird, the Entranan firebird and the Giant Roc. */
final class BestiaryQbmFliers {
    private BestiaryQbmFliers() {
    }

    static final int CHOMPY = 0;
    static final int TERRORBIRD = 1;
    static final int FIREBIRD = 2;
    static final int ROC = 3;

    static List<ModelSpec> overrides() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(bird("chompy_bird", 1.6f, CHOMPY));
        l.add(bird("warped_terrorbird", 2.2f, TERRORBIRD));
        l.add(bird("entranan_firebird", 1.2f, FIREBIRD));
        return l;
    }

    static List<ModelSpec> fresh() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(bird("quest_giant_roc", 1.25f, ROC));
        return l;
    }

    /**
     * A bird built from layered plumage: a stacked, egg-shaped body with a breast and a mantle, a jointed neck, a head
     * with a two-part beak, wings of overlapping coverts and primaries, a fanned tail and scaled legs with taloned feet.
     * <ul>
     *   <li>chompy bird (Big Chompy Bird Hunting): plump, olive, a huge orange bill, stubby wings and short legs;</li>
     *   <li>warped terrorbird (The Path of Glouphrie): a tall gnome war-bird twisted by the warping - violet plumage,
     *   bony spikes, crackling magic, a long neck and long legs;</li>
     *   <li>Entranan firebird (Heroes' Quest): a phoenix of flame-red plumage with burning wing tips and tail streamers;</li>
     *   <li>Giant Roc (My Arm's Big Adventure): a vast brown eagle with half-spread wings and great hooked talons.</li>
     * </ul>
     */
    static ModelSpec bird(String id, float scale, int kind) {
        boolean chompy = kind == CHOMPY;
        boolean terror = kind == TERRORBIRD;
        boolean fire = kind == FIREBIRD;
        boolean roc = kind == ROC;
        int plume = chompy ? 0x7A8A4C : terror ? 0x5A4A7A : fire ? 0xD8401A : 0x6A4A2A;
        int plume2 = chompy ? 0x5A6A34 : terror ? 0x3A2E54 : fire ? 0xA82410 : 0x4A3018;
        int breast = chompy ? 0xC8B878 : terror ? 0x8A78A8 : fire ? 0xFFA030 : 0xB89A6A;
        int beak = chompy ? 0xF09030 : terror ? 0xC8B890 : fire ? 0xFFD040 : 0xE0C040;
        int legCol = chompy ? 0xE08A30 : terror ? 0x7A6A5A : fire ? 0xC88A30 : 0xD8B040;
        int eye = chompy ? 0x101010 : terror ? 0xD060FF : fire ? 0xFFF0A0 : 0xFFC020;
        int talon = 0x2A2420;
        String feather = "feathers";
        // proportions
        int bw = chompy ? 10 : terror ? 7 : fire ? 6 : 10;
        int bh = chompy ? 9 : terror ? 7 : fire ? 6 : 8;
        int bl = chompy ? 11 : terror ? 10 : fire ? 9 : 13;
        int thigh = chompy ? 3 : terror ? 6 : fire ? 3 : 4;
        int shank = chompy ? 3 : terror ? 8 : fire ? 4 : 5;
        float bodyY = 24 - (thigh + shank + 1) - bh / 2f + (chompy ? 1.5f : 1);
        ModelSpec.Builder b = ModelSpec.builder(id).scale(scale).shadow(roc ? 1.4f : chompy ? 0.8f : 0.7f);
        part(b, "body", null, Role.BODY, plume).accent(plume2).at(0, bodyY, 0).rot(chompy ? 0 : terror ? -14 : -8, 0, 0).mat(feather)
                .box(-bw / 2f, -bh / 2f, -bl / 2f, bw, bh, bl).add();
        part(b, "breast", "body", Role.STATIC, breast).accent(plume).at(0, 0.5f, -bl / 2f).rot(10, 0, 0).mat(feather)
                .box(-bw / 2f + 1, -bh / 2f + 1, -1.5f, bw - 2, bh - 1, 2).add();
        part(b, "belly", "body", Role.STATIC, breast).at(0, bh / 2f, 0).mat(feather).box(-bw / 2f + 1, -0.5f, -bl / 2f + 1, bw - 2, 1, bl - 3).add();
        part(b, "mantle", "body", Role.STATIC, plume2).accent(plume).at(0, -bh / 2f, 0).mat(feather)
                .box(-bw / 2f + 1, -1, -bl / 2f + 1, bw - 2, 1, bl - 2).add();
        part(b, "rump", "body", Role.STATIC, plume).at(0, -0.5f, bl / 2f).rot(-15, 0, 0).mat(feather).box(-bw / 2f + 1, -bh / 2f + 1, -0.5f, bw - 2, bh - 2, 2).add();
        // neck and head
        int nw = chompy ? 6 : terror ? 4 : fire ? 3 : 5;
        int nl = chompy ? 2 : terror ? 6 : fire ? 4 : 4;
        float npitch = chompy ? -30 : terror ? -70 : fire ? -55 : -45;
        part(b, "neck", "body", Role.NECK, plume).accent(plume2).at(0, -bh / 2f + 2, -bl / 2f + 1).rot(npitch, 0, 0).mat(feather)
                .box(-nw / 2f, -nw / 2f, -nl, nw, nw, nl + 1).add();
        String neckEnd = "neck";
        if (terror || fire) {
            part(b, "neck2", "neck", Role.STATIC, plume).accent(plume2).at(0, 0, -nl).rot(15, 0, 0).mat(feather)
                    .box(-nw / 2f + 0.5f, -nw / 2f + 0.5f, -nl, nw - 1, nw - 1, nl + 1).add();
            neckEnd = "neck2";
        }
        if (roc) {
            part(b, "ruff", "neck", Role.STATIC, plume2).accent(plume).at(0, 0, 0).mat(feather).box(-nw / 2f - 1, -nw / 2f - 1, -2, nw + 2, nw + 2, 3).add();
        }
        int hw = chompy ? 6 : terror ? 5 : fire ? 4 : 6;
        int hl = chompy ? 6 : terror ? 6 : fire ? 4 : 6;
        float hp = -npitch - (terror || fire ? 15 : 0);
        part(b, "head", neckEnd, Role.HEAD, chompy ? plume : roc ? light(plume, 0.25f) : plume).accent(eye).at(0, 0, -nl)
                .rot(hp, 0, 0).mat(feather).box(-hw / 2f, -hw + 1.5f, -hl, hw, hw - 1, hl).add();
        part(b, "cheeks", "head", Role.STATIC, roc ? 0xE8DCC0 : breast).at(0, -hw / 2f + 2, -hl + 1).mat(feather)
                .box(-hw / 2f - 0.3f, -1, 0, hw, 2, 3).add();
        float eyeY = -hw / 2f + 0.8f;
        b.part("eyes", "head", Role.STATIC, eye).pivot(0, eyeY, -hl + 1.5f).material(chompy ? "bone" : "energy").glow(chompy ? "none" : "full")
                .box(hw / 2f - 0.6f, -0.5f, -0.5f, 1, 1, 1).box(-hw / 2f - 0.4f, -0.5f, -0.5f, 1, 1, 1);
        part(b, "brow", "head", Role.STATIC, plume2).at(0, eyeY - 1, -hl + 0.5f).rot(roc || terror ? 20 : 0, 0, 0).mat(feather)
                .box(-hw / 2f - 0.2f, -0.5f, -1, hw, 1, 3).add();
        // beak: upper (static, hooked for roc/terror) and lower (jaw)
        int bkw = chompy ? 4 : terror ? 3 : 2;
        int bkl = chompy ? 6 : terror ? 6 : fire ? 3 : 5;
        part(b, "beak", "head", Role.STATIC, beak).accent(dark(beak, 0.3f)).at(0, -hw / 2f + 2, -hl).rot(chompy ? 5 : 0, 0, 0).mat("horn")
                .box(-bkw / 2f, -1.5f, -bkl, bkw, chompy ? 3 : 2, bkl).add();
        part(b, "cere", "beak", Role.STATIC, roc ? 0xF0D060 : dark(beak, 0.15f)).at(0, -1.5f, 0).mat("horn").box(-bkw / 2f - 0.2f, 0, -1.5f, bkw, 2, 2).add();
        if (roc || terror) {
            part(b, "beak_hook", "beak", Role.STATIC, dark(beak, 0.2f)).at(0, 0, -bkl).rot(40, 0, 0).mat("horn").box(-0.5f, -1, -2, 1, 2, 2).add();
        }
        part(b, "jaw", "head", Role.JAW, dark(beak, 0.12f)).at(0, -hw / 2f + 3.5f, -hl + 0.5f).rot(8, 0, 0).mat("horn")
                .box(-bkw / 2f + 0.5f, 0, -bkl + 1, bkw - 1, 1, bkl - 1).add();
        // crest
        int crestN = chompy ? 2 : terror ? 4 : fire ? 5 : 3;
        for (int i = 0; i < crestN; i++) {
            int h = (fire ? 4 : 2) + (i == crestN / 2 ? 1 : 0) + (fire ? i % 2 : 0);
            Kit.P c = part(b, "crest" + i, "head", Role.STATIC, fire ? (i % 2 == 0 ? 0xFFB030 : 0xFF6018) : terror ? 0xC8B8A0 : plume2)
                    .accent(0xFFE070).at(0, -hw + 1.5f, -hl + 1.5f + i * 1.2f).rot(-40 - i * 10, 0, 0).mat(terror ? "horn" : fire ? "energy" : feather)
                    .box(-0.5f, -h, -0.5f, 1, h, 1);
            if (fire) {
                c.glow("full");
            }
            c.add();
        }
        // wings: a shoulder covert block (flap role), a hand segment and a row of primaries
        float spread = roc ? -55 : fire ? -35 : 0;
        int ww = 2;
        int wl = chompy ? 7 : terror ? 7 : fire ? 8 : 12;
        int wh = chompy ? 5 : terror ? 4 : fire ? 4 : 7;
        Kit.Sym wing = sym(b, "wing", "body", false, plume2).roles(Role.WING_LEFT, Role.WING_RIGHT).accent(plume).at(bw / 2f, -bh / 2f + 1.5f, -bl / 2f + 2)
                .rot(roc ? 10 : -6, roc ? -10 : 4, spread).mat(feather).box(-0.5f, -1, -1, ww, wh, wl);
        wing.add();
        sym(b, "covert", "wing", true, plume).accent(plume2).at(1, 0, 0).mat(feather).box(-0.3f, -1.3f, -0.5f, 1, wh - 1, wl - 2).add();
        int pn = chompy ? 3 : terror ? 3 : fire ? 4 : 5;
        for (int i = 0; i < pn; i++) {
            int len = (chompy ? 3 : terror ? 4 : fire ? 6 : 8) - i;
            Kit.Sym f = sym(b, "primary" + i, "wing", true, i % 2 == 0 ? plume2 : dark(plume2, 0.15f)).accent(fire ? 0xFFE070 : plume)
                    .at(0.5f, wh - 1.5f - i * (wh / (float) pn), wl - 1).rot(-8 - i * 6, 0, 0).mat(fire && i < 2 ? "lava" : feather)
                    .box(-0.5f, -0.5f, -1, 1, Math.max(1, wh / pn + 1), len);
            if (fire) {
                f.glow(i < 2 ? "full" : "cracks");
            }
            f.add();
        }
        // tail fan
        int tn = chompy ? 3 : terror ? 4 : fire ? 3 : 5;
        for (int i = 0; i < tn; i++) {
            float off = i - (tn - 1) / 2f;
            int len = (chompy ? 3 : terror ? 5 : fire ? 10 : 7) - (int) Math.abs(off);
            Kit.P t = part(b, "tail" + i, "rump", i == tn / 2 ? Role.TAIL : Role.STATIC, i % 2 == 0 ? plume2 : plume).accent(fire ? 0xFFE070 : plume)
                    .at(off * 1.6f, -1, 1).rot(chompy ? -30 : fire ? 5 : -10, off * 12, 0).mat(fire ? "lava" : feather).box(-1, -0.5f, 0, 2, 1, len);
            if (fire) {
                t.glow("full");
            }
            t.add();
        }
        if (fire) {
            for (boolean left : new boolean[]{true, false}) {
                int s = left ? 1 : -1;
                part(b, "streamer" + (left ? "_l" : "_r"), "rump", Role.TAIL, 0xFFD040).accent(0xFFF0A0).at(s * 1.5f, 0, 1).rot(10, s * 6, 0)
                        .mat("energy").glow("full").box(-0.5f, -0.5f, 0, 1, 1, 14).add();
            }
            part(b, "ember_core", "body", Role.STATIC, 0xFFC040).at(0, 0, -bl / 2f - 0.5f).mat("energy").glow("full").box(-1, -1, -0.6f, 2, 2, 1).add();
        }
        if (terror) {
            for (int i = 0; i < 4; i++) {
                part(b, "warp_spike" + i, "body", Role.STATIC, 0xC8B8A0).accent(0xD060FF).at((i % 2 == 0 ? 1 : -1) * 2, -bh / 2f, -3 + i * 2.2f)
                        .rot(-25, 0, (i % 2 == 0 ? -15 : 15)).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
            }
            part(b, "warp_rift", "body", Role.STATIC, 0xB040FF).accent(0xF0B0FF).at(bw / 2f, 0, 1).mat("energy").glow("full")
                    .box(-0.2f, -2, -2, 1, 3, 3, -0.2f).add();
        }
        if (chompy) {
            part(b, "wattle", "jaw", Role.STATIC, 0xD04030).at(0, 1, -2).mat("flesh").box(-1, 0, -1, 2, 2, 2).add();
        }
        // legs: feathered thigh (walk role), scaled shank, three toes forward and one back
        float lx = bw / 2f - 1.5f;
        sym(b, "thigh", null, false, roc || chompy ? plume2 : plume).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(plume)
                .at(lx, 24 - shank - thigh - 1, 1).rot(-10, 0, 0).mat(feather).box(-1.5f, -1, -1.5f, 3, thigh + 1, 3).add();
        sym(b, "shank", "thigh", true, legCol).accent(dark(legCol, 0.3f)).at(0, thigh, 0).rot(10, 0, 0).mat("scales")
                .box(-0.5f, 0, -0.5f, roc ? 2 : 1, shank, roc ? 2 : 1).add();
        Kit.Sym toes = sym(b, "toes", "shank", true, legCol).at(0, shank, 0).mat("scales");
        int tl = roc ? 4 : terror ? 4 : 3;
        toes.box(-0.5f, 0, -tl, 1, 1, tl).box(-1.6f, 0, -tl + 1, 1, 1, tl - 1).box(0.6f, 0, -tl + 1, 1, 1, tl - 1).box(-0.5f, 0, 0, 1, 1, 2);
        toes.add();
        sym(b, "talons", "toes", true, talon).at(0, 0.5f, -tl).mat("horn").box(-0.5f, -0.5f, -1, 1, 1, 1).box(-1.6f, -0.5f, 0, 1, 1, 1)
                .box(0.6f, -0.5f, 0, 1, 1, 1).add();
        return b.build();
    }
}
