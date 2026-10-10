package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.BestiarySlmKit.*;

/**
 * Dragon kin of the slayer module: wyverns (skeletal, spitting, taloned - the long-tailed and ancient wyverns
 * share the latter two), Karuulm drakes, wyrms and hydras.
 */
final class BestiarySlmWyverns {
    private static final int SKELETAL = 0;
    private static final int SPITTING = 1;
    private static final int TALONED = 2;
    private static final int DRAKE = 3;

    private BestiarySlmWyverns() {
    }

    static List<ModelSpec> all() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(wyvern("skeletal_wyvern", 0xDCD8C4, 0xA8A492, 0x5A5A62, 0xC8C4B0, SKELETAL));
        l.add(wyvern("spitting_wyvern", 0x667E88, 0xA8BCC0, 0x3E5462, 0xD8D0B8, SPITTING));
        l.add(wyvern("taloned_wyvern", 0x58607E, 0x9CA4BC, 0x3A3E5E, 0xE0D8C0, TALONED));
        l.add(wyvern("drake", 0x7A3E26, 0xC08050, 0x4A2216, 0xE0C890, DRAKE));
        l.add(wyrm("wyrm"));
        l.add(hydra("hydra"));
        return l;
    }

    /** Bipedal wyvern (wings are its forelimbs), long neck, crested head, long tail; drakes add arms and horns. */
    static ModelSpec wyvern(String id, int body, int belly, int wing, int horn, int style) {
        boolean bony = style == SKELETAL;
        boolean drake = style == DRAKE;
        String hide = bony ? "bone" : "scales";
        int dark = darker(body, 0.7f);
        int eye = bony ? 0x6AE8FF : drake ? 0xFF7A20 : 0xFFD040;
        int voidC = 0x1A1C22;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.0f);
        Kit.part(b, "hips", null, Role.BODY, body).accent(belly).at(0, 11, 4).mat(hide).box(-4.5f, -3, -3.5f, 9, 6, 8).add();
        st(b, "chest", "hips", bony ? voidC : body).accent(belly).at(0, -2, -1).rot(38, 0, 0).mat(bony ? "hide" : hide)
                .box(-5, -11, -4, 10, 11, 8).add();
        if (bony) {
            for (int i = 0; i < 4; i++) {
                sy(b, "rib" + i, "chest", body).at(4.6f, -2.5f - i * 2.4f, -1).rot(0, 0, -6).mat("bone")
                        .box(-0.5f, -0.5f, -3.5f, 1, 1, 8).add();
                st(b, "rib_front" + i, "chest", body).at(0, -2.5f - i * 2.4f, -4.4f).mat("bone").box(-4.5f, -0.5f, -0.5f, 9, 1, 1).add();
            }
            st(b, "sternum", "chest", body).at(0, -6, -4.6f).mat("bone").box(-1, -5, -0.5f, 2, 9, 1).add();
            st(b, "vertebrae", "chest", body).at(0, -11, 3.6f).mat("bone").box(-1, 0, -0.5f, 2, 11, 2).add();
            st(b, "pelvis", "hips", body).at(0, -3.5f, 0).mat("bone").box(-5, -1, -2.5f, 10, 2, 6).add();
        } else {
            st(b, "belly", "chest", belly).accent(darker(belly, 0.7f)).at(0, -5.5f, -4).mat("plates").box(-3.5f, -5, -0.6f, 7, 10, 1).add();
            st(b, "back", "chest", dark).at(0, -11, 4).mat(hide).box(-3.5f, 0, -0.5f, 7, 10, 1).add();
            for (int i = 0; i < 4; i++) {
                st(b, "spine" + i, "chest", horn).at(0, -2 - i * 2.8f, 4).rot(40, 0, 0).mat("horn")
                        .box(-0.5f, -0.5f, -0.5f, 1, 1, i == 1 || i == 2 ? 3 : 2).add();
            }
        }
        sy(b, "shoulder", "chest", body).accent(belly).at(5, -9, 0).mat(hide).box(-1.5f, -2.5f, -2.5f, 3, 5, 5).add();
        // neck & head
        int[] nw = drake ? new int[]{6, 5, 5} : new int[]{5, 4, 4};
        String prev = "chest";
        float[] np = {-76, 12, 22};
        for (int i = 0; i < 3; i++) {
            String n = "neck" + i;
            Kit.part(b, n, prev, i == 0 ? Role.NECK : Role.STATIC, body).accent(belly)
                    .at(0, i == 0 ? -10.5f : 0, i == 0 ? -1.5f : -4.5f).rot(np[i], 0, 0).mat(hide)
                    .box(-nw[i] / 2f, -nw[i] / 2f, -5, nw[i], nw[i], 5).add();
            if (bony) {
                st(b, n + "_knob", n, body).at(0, -nw[i] / 2f, -2.5f).mat("bone").box(-0.5f, -1, -1, 1, 1, 2).add();
            } else {
                st(b, n + "_plate", n, belly).at(0, nw[i] / 2f, 0).mat("plates").box(-nw[i] / 2f + 1, -0.6f, -5, nw[i] - 2, 1, 5).add();
            }
            prev = n;
        }
        int hw = drake ? 7 : 6;
        Kit.part(b, "head", "neck2", Role.HEAD, body).accent(eye).at(0, 0, -4).rot(30, 0, 0).mat(bony ? "bone" : hide)
                .pattern(bony ? "skull" : "eyes").glow("eyes").box(-hw / 2f, -3, -5, hw, 5, 6).add();
        int sl = style == SPITTING ? 8 : 6;
        st(b, "snout", "head", body).at(0, 0, -5).mat(bony ? "bone" : hide).box(-2, -2, -sl, 4, 3, sl).add();
        sy(b, "nostril", "snout", darker(body, 0.35f)).at(1, -2, -sl + 0.5f).mat("horn").box(-0.5f, -0.3f, -0.5f, 1, 1, 1).add();
        teeth(b, "teeth_up", "snout", TOOTH, 1, -sl + 0.4f, 2, style == SPITTING ? 3 : 2, true, false);
        Kit.part(b, "jaw", "head", Role.JAW, bony ? body : belly).at(0, 2, -4).rot(12, 0, 0).mat(bony ? "bone" : "plates")
                .glow(bony ? "maw" : null).box(-2, 0, -sl - 1, 4, 2, sl + 1).add();
        teeth(b, "teeth_lo", "jaw", TOOTH, 0, -sl - 0.6f, 1.8f, 2, false, false);
        sy(b, "brow", "head", dark).at(2.3f, -3, -4.5f).rot(12, -6, -10).mat(bony ? "bone" : hide).box(-1, -1, -1, 2, 2, 4).add();
        if (drake) {
            horn(b, "horn", "head", true, false, horn, 2.5f, -2.5f, 0, -70, 20, -10, new int[]{2, 2, 1}, new int[]{4, 3, 3}, -18, 6);
            sy(b, "cheek_spike", "head", horn).at(3.5f, 1, -1).rot(0, -50, -20).mat("horn").box(0, -0.5f, -0.5f, 3, 1, 1).add();
        } else {
            st(b, "crest", "head", horn).at(0, -3, -1).rot(-40, 0, 0).mat("horn").box(-0.5f, -6, -1.5f, 1, 6, 3).add();
            st(b, "crest_tip", "crest", darker(horn, 0.8f)).at(0, -6, 0).rot(-30, 0, 0).mat("horn").box(-0.5f, -3, -1, 1, 3, 2).add();
            horn(b, "horn", "head", true, false, horn, 2.2f, -2.5f, 0.5f, -75, 18, -6, new int[]{1, 1}, new int[]{4, 3}, -10, 0);
        }
        // wings (the forelimbs) half-spread
        int bone = bony ? body : darker(wing, 0.8f);
        Kit.sym(b, "wing", "chest", false, bone).roles(Role.WING_LEFT, Role.WING_RIGHT).accent(horn).at(5.5f, -9, 0.5f).rot(10, -20, -40)
                .mat(bony ? "bone" : hide).box(0, -1, -1, 9, 2, 2).add();
        if (!bony) {
            sy2(b, "wing_web0", "wing", wing).accent(bone).at(0, 0, 0.8f).mat("membrane").box(0, -0.25f, 0, 9, 1, 7, -0.25f).add();
        }
        sy2(b, "wing_fore", "wing", bone).accent(horn).at(9, 0, 0).rot(0, 35, 10).mat(bony ? "bone" : hide).box(0, -0.75f, -0.75f, 9, 1, 1, 0.3f).add();
        sy2(b, "wing_talon", "wing_fore", horn).at(9, 0, 0).rot(0, 60, 30).mat("horn").box(0, -0.5f, -0.5f, 3, 1, 1, 0.1f).add();
        float[] fy = {-45, -90};
        int[] fl = {13, 10};
        for (int f = 0; f < 2; f++) {
            String fn = "wing_finger" + f;
            sy2(b, fn, "wing_fore", bone).accent(horn).at(8.5f, 0, 0).rot(0, fy[f], 18 + f * 8).mat(bony ? "bone" : "horn")
                    .box(0, -0.5f, -0.5f, fl[f], 1, 1).add();
            sy2(b, fn + "_web", fn, wing).accent(bone).at(0, 0, 0.3f).mat("membrane")
                    .box(0, -0.25f, 0, fl[f] - 1, 1, f == 0 ? 8 : 6, bony ? -0.4f : -0.25f).add();
        }
        if (drake) {
            sy(b, "arm", "chest", body).accent(belly).at(4.5f, -6, -3).rot(-30, 0, -10).mat(hide).box(-1.5f, -1, -1.5f, 3, 6, 3).add();
            sy2(b, "forearm", "arm", body).at(0, 5, 0).rot(-50, 0, 0).mat(hide).box(-1.5f, 0, -1.5f, 3, 5, 3).add();
            claws(b, "hand_claws", "forearm", true, horn, 3, 2, 5, -0.5f, 2);
        }
        // legs: thick thighs, reverse shins, three big talons
        int tc = style == TALONED ? 3 : 2;
        Kit.sym(b, "thigh", null, false, body).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(belly).at(4.5f, 11, 4)
                .rot(-28, 0, -6).mat(hide).box(-2.5f, -2, -3, 5, 8, 6).add();
        sy2(b, "shin", "thigh", bony ? body : dark).at(0, 6, -0.5f).rot(62, 0, 6).mat(hide).box(-1.5f, 0, -1.5f, 3, 6, 3).add();
        sy2(b, "ankle", "shin", dark).at(0, 5.5f, 0).rot(-34, 0, 0).mat(hide).box(-1.5f, 0, -1.5f, 3, 3, 3).add();
        sy2(b, "foot", "ankle", dark).at(0, 3, 0).mat(hide).box(-2.5f, 0, -3.5f, 5, 1, 5).add();
        claws(b, "talons", "foot", true, horn, 3, 3.5f, 0.5f, -3.2f, tc);
        sy2(b, "hallux", "ankle", horn).at(0, 2.5f, 1.5f).rot(-30, 0, 0).mat("horn").box(-0.5f, -0.5f, 0, 1, 1, tc).add();
        // tail
        int[] tw = bony ? new int[]{3, 3, 2, 2, 2, 2, 1} : new int[]{5, 4, 4, 3, 3, 2, 2};
        int[] tl = {5, 5, 5, 5, 4, 4, 4};
        float[] tp = {32, -6, -6, -4, 0, 4, 6};
        float[] ty = {0, 8, 8, 6, -6, -8, -8};
        String last = chain(b, "tail", "hips", Role.TAIL, body, belly, hide, null, 0, 0, 4, 1, tw, tw, tl, tp, ty);
        if (bony) {
            for (int i = 0; i < 5; i += 2) {
                st(b, "tail_knob" + i, "tail" + i, body).at(0, 0, 2.5f).mat("bone").box(-1.5f, -1.5f, -0.5f, 3, 3, 1).add();
            }
        } else {
            for (int i = 0; i < 4; i += 1) {
                st(b, "tail_spine" + i, "tail" + i, horn).at(0, -tw[i] / 2f, 2.5f).rot(-35, 0, 0).mat("horn")
                        .box(-0.5f, -2, -0.5f, 1, 2, 2).add();
            }
        }
        st(b, "tail_barb", last, horn).at(0, 0, 4).mat("horn").box(-0.5f, -0.5f, 0, 1, 1, 4).box(-2, -0.5f, 1, 4, 1, 2, -0.05f).add();
        return b.build();
    }

    /** Karuulm wyrm: a long, low, four-legged serpent with a huge frilled head and a glowing magical maw. */
    static ModelSpec wyrm(String id) {
        int body = 0x4E3E58;
        int belly = 0x8A7A8E;
        int dark = 0x2E2236;
        int frill = 0x7A3A5A;
        int horn = 0xCFC2A8;
        int glow = 0xFF8A3A;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.0f);
        Kit.part(b, "body", null, Role.BODY, body).accent(belly).at(0, 17, -2).mat("scales").box(-5, -4.5f, -9, 10, 9, 18).add();
        st(b, "belly", "body", belly).accent(darker(belly, 0.7f)).at(0, 4.5f, 0).mat("plates").box(-4, -0.6f, -8.5f, 8, 1, 17).add();
        for (int i = 0; i < 5; i++) {
            st(b, "ridge" + i, "body", dark).at(0, -4.5f, -7 + i * 3.6f).rot(-20, 0, 0).mat("horn")
                    .box(-1.5f, -1.5f, -1, 3, 2, 2).box(-0.5f, -3, -0.5f, 1, 2, 2).add();
        }
        sy(b, "flank", "body", dark).at(5, 0, 0).mat("scales").box(-0.5f, -3, -7, 1, 6, 14).add();
        // neck rising to a big head
        String n = chain(b, "neck", "body", Role.NECK, body, belly, "scales", null, 0, -1.5f, -9, -1,
                new int[]{8, 7, 7}, new int[]{8, 7, 7}, new int[]{5, 4, 4}, new float[]{-34, -8, 10}, null);
        st(b, "neck_plate", "neck1", belly).at(0, 3.5f, 0).mat("plates").box(-2.5f, -0.6f, -4, 5, 1, 4).add();
        Kit.part(b, "head", n, Role.HEAD, body).accent(glow).at(0, 0, -4).rot(30, 0, 0).mat("scales").pattern("eyes").glow("eyes")
                .box(-4, -3.5f, -6, 8, 6, 7).add();
        st(b, "snout", "head", body).at(0, 0.5f, -6).mat("scales").box(-3, -2.5f, -5, 6, 4, 5).add();
        Kit.part(b, "jaw", "head", Role.JAW, belly).at(0, 2.5f, -5).rot(14, 0, 0).mat("plates").glow("maw").box(-3, 0, -6, 6, 2, 7).add();
        teeth(b, "teeth", "snout", TOOTH, 1.5f, -4.6f, 2.8f, 3, true, true);
        st(b, "tongue_glow", "jaw", glow).accent(glow).at(0, -0.3f, -3).mat("energy").glow("full").box(-2, -0.5f, -2.5f, 4, 1, 4, -0.1f).add();
        sy(b, "brow", "head", dark).at(3.5f, -4, -6).rot(18, -10, -14).mat("horn").box(-1.5f, -1, -1, 3, 2, 6).add();
        // the big neck frill: fanned spines with membrane
        for (int k = 0; k < 3; k++) {
            sy(b, "frill" + k, "head", frill).accent(horn).at(4.5f, -2 + k * 2.5f, 0).rot(-30 + k * 25, -30, -20 + k * 10).mat("membrane")
                    .box(0, -1, 0, 1, 3, 7).add();
            sy2(b, "frill_spine" + k, "frill" + k, horn).at(0.5f, -1, 7).mat("horn").box(-0.5f, -0.5f, -0.5f, 1, 1, 3).add();
        }
        st(b, "crest", "head", frill).accent(horn).at(0, -4, -2).rot(-30, 0, 0).mat("membrane").box(-0.5f, -4, -1, 1, 4, 7).add();
        // four short splayed legs
        float[][] legs = {{5.5f, 18, -6}, {5.5f, 18, 6}};
        Role[][] roles = {{Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT}, {Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT}};
        for (int i = 0; i < 2; i++) {
            String ln = i == 0 ? "leg_f" : "leg_b";
            Kit.sym(b, ln, null, false, body).roles(roles[i][0], roles[i][1]).accent(belly).at(legs[i][0], legs[i][1], legs[i][2])
                    .rot(0, 0, -45).mat("scales").box(-2, -1.5f, -2, 6, 4, 4).add();
            sy2(b, ln + "_shin", ln, dark).at(4.5f, 0.5f, 0).rot(0, 0, 50).mat("scales").box(-1.5f, 0, -1.5f, 3, 5, 3).add();
            sy2(b, ln + "_foot", ln + "_shin", dark).at(0, 4.5f, 0).rot(0, 0, -5).mat("scales").box(-2, 0, -3, 4, 1, 4).add();
            claws(b, ln + "_claws", ln + "_foot", true, horn, 3, 2.5f, 0.5f, -2.8f, 2);
        }
        // long tail
        String t = chain(b, "tail", "body", Role.TAIL, body, belly, "scales", null, 0, -0.5f, 9, 1,
                new int[]{8, 7, 6, 5, 4, 3, 2}, new int[]{7, 6, 5, 4, 4, 3, 2}, new int[]{5, 5, 5, 5, 4, 4, 4},
                new float[]{4, 2, 0, 0, -2, -2, 0}, new float[]{0, 10, 10, 8, -6, -10, -10});
        for (int i = 0; i < 5; i += 2) {
            st(b, "tail_ridge" + i, "tail" + i, dark).at(0, -3, 2.5f).rot(-25, 0, 0).mat("horn").box(-0.5f, -2, -1, 1, 2, 2).add();
        }
        st(b, "tail_tip", t, horn).at(0, 0, 4).mat("horn").box(-0.5f, -0.5f, 0, 1, 1, 3).add();
        return b.build();
    }

    /** Hydra: a bulky four-legged reptile with four long necks ending in snapping heads, and a stubby tail. */
    static ModelSpec hydra(String id) {
        int body = 0x3E6E44;
        int belly = 0xB0B878;
        int dark = 0x28482C;
        int spine = 0x8A9A5A;
        int horn = 0xE0D8B0;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.3f);
        Kit.part(b, "body", null, Role.BODY, body).accent(belly).at(0, 11, 2).mat("scales").box(-7, -6, -10, 14, 12, 20).add();
        st(b, "belly", "body", belly).accent(darker(belly, 0.7f)).at(0, 6, 0).mat("plates").box(-5.5f, -0.6f, -9, 11, 1, 17).add();
        st(b, "chest", "body", body).accent(belly).at(0, 0, -10).rot(-10, 0, 0).mat("scales").box(-6, -5, -3, 12, 10, 4).add();
        st(b, "chest_plate", "chest", belly).at(0, 1, -3).mat("plates").box(-4, -3, -0.6f, 8, 7, 1).add();
        sy(b, "haunch", "body", body).accent(belly).at(6.5f, 0, 5).mat("scales").box(-1.5f, -4.5f, -5, 3, 9, 10).add();
        sy(b, "shoulder", "body", body).accent(belly).at(6.5f, -0.5f, -6).mat("scales").box(-1.5f, -4, -4, 3, 8, 8).add();
        for (int i = 0; i < 5; i++) {
            st(b, "spine" + i, "body", spine).at(0, -6, -7 + i * 3.8f).rot(-25, 0, 0).mat("horn")
                    .box(-1.5f, -1.5f, -1.5f, 3, 2, 3).box(-0.5f, -3, -1, 1, 2, 2).add();
        }
        // four necks fanning from the chest
        float[] yaws = {-58, -20, 20, 58};
        float[] lift = {-40, -58, -58, -40};
        for (int h = 0; h < 4; h++) {
            String s = String.valueOf(h);
            float nx = -6 + h * 4f;
            String n = chain(b, "neck" + s + "_", "body", h == 1 ? Role.NECK : Role.STATIC, body, belly, "scales", null, nx, -3.5f, -10, -1,
                    new int[]{5, 4, 4, 4}, new int[]{5, 4, 4, 4}, new int[]{5, 5, 5, 4}, new float[]{lift[h], -6, 14, 22},
                    new float[]{yaws[h], 0, 0, 0});
            Kit.part(b, "head" + s, n, Role.HEAD, body).accent(0xFFD040).at(0, 0, -3.5f).rot(40, 0, 0).mat("scales")
                    .pattern("eyes").glow("eyes").box(-3, -3, -4, 6, 5, 5).add();
            st(b, "snout" + s, "head" + s, body).at(0, 0, -4).mat("scales").box(-2, -2, -4, 4, 3, 4).add();
            Kit.part(b, "jaw" + s, "head" + s, Role.JAW, belly).at(0, 2, -3).rot(16, 0, 0).mat("plates").box(-2, 0, -5, 4, 1, 5).add();
            sy(b, "horn" + s, "head" + s, horn).at(1.5f, -2.5f, -0.5f).rot(-60, 15, 0).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
            st(b, "fang" + s, "snout" + s, TOOTH).mat("horn").at(0, 0.5f, -2.6f).box(-1.5f, 0, 0, 1, 1, 1, -0.1f).box(0.5f, 0, 0, 1, 1, 1, -0.1f).add();
        }
        // legs: columnar with clawed feet
        float[][] lp = {{6, 14, -6}, {6, 14, 6}};
        Role[][] roles = {{Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT}, {Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT}};
        for (int i = 0; i < 2; i++) {
            String ln = i == 0 ? "leg_f" : "leg_b";
            Kit.sym(b, ln, null, false, body).roles(roles[i][0], roles[i][1]).accent(belly).at(lp[i][0], lp[i][1], lp[i][2])
                    .rot(i == 0 ? 8 : -8, 0, -4).mat("scales").box(-3, -2, -3, 6, 7, 6).add();
            sy2(b, ln + "_shin", ln, dark).at(0, 5, 0).rot(i == 0 ? -8 : 8, 0, 4).mat("scales").box(-2.5f, 0, -2.5f, 5, 4, 5).add();
            sy2(b, ln + "_foot", ln + "_shin", dark).at(0, 4, 0).mat("scales").box(-3, 0, -4, 6, 1, 6).add();
            claws(b, ln + "_claws", ln + "_foot", true, horn, 3, 4, 0.5f, -3.8f, 2);
        }
        String t = chain(b, "tail", "body", Role.TAIL, body, belly, "scales", null, 0, -1, 10, 1,
                new int[]{7, 6, 4, 3, 2}, new int[]{7, 5, 4, 3, 2}, new int[]{5, 5, 4, 4, 3},
                new float[]{18, 4, 2, 0, 0}, new float[]{0, 8, 8, 6, 6});
        st(b, "tail_tip", t, spine).at(0, 0, 3).mat("horn").box(-0.5f, -0.5f, 0, 1, 1, 2).add();
        return b.build();
    }
}
