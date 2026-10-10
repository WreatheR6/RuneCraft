package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.BestiarySlmKit.*;

/**
 * Mid/high slayer creatures: turoths and kurasks, zygomites, spectres, dust and smoke devils, cave krakens,
 * dark beasts, basilisk knights, cave horrors and lizardmen.
 */
final class BestiarySlmMid {
    private BestiarySlmMid() {
    }

    static List<ModelSpec> overrides() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(fremennikBeast("turoth", false));
        l.add(fremennikBeast("kurask", true));
        l.add(zygomite("zygomite"));
        l.add(spectre("aberrant_spectre", 0x7A8A52, 0x4E5A30, 0xB0C070));
        l.add(dustDevil("dust_devil", 0xC0A87A, 0x8A7450));
        l.add(smokeDevil("smoke_devil"));
        l.add(caveKraken("cave_kraken"));
        l.add(darkBeast("dark_beast"));
        l.add(basiliskKnight("basilisk_knight"));
        l.add(lizardman("lizardman"));
        return l;
    }

    static List<ModelSpec> fallbacks() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(spectre("deviant_spectre", 0x8A5A6A, 0x5A3040, 0xC890A0));
        l.add(dustDevil("catacomb_dust_devil", 0xA89878, 0x6A5A40));
        l.add(caveHorror("cave_horror", 0x2E2A2A, 0x5A4A44));
        l.add(caveHorror("cave_abomination", 0x3A2238, 0x6A4060));
        return l;
    }

    /**
     * Turoth / kurask: tall, stooped Fremennik-province brutes on long legs with dangling clawed arms and a big
     * drooping nose; kurasks are greener and heavier, with a horned head and spikes down the back.
     */
    static ModelSpec fremennikBeast(String id, boolean kurask) {
        int skin = kurask ? 0x6E8A4A : 0xB8A47A;
        int dark = kurask ? 0x46602C : 0x847050;
        int horn = kurask ? 0xD8CCA0 : 0x6A5A40;
        int cloth = kurask ? 0x5A3E26 : 0x6A4A30;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.8f);
        Kit.part(b, "hips", null, Role.BODY, skin).accent(dark).at(0, 11, 1).mat("skin").box(-4, -2, -2.5f, 8, 4, 5).add();
        st(b, "loin", "hips", cloth).at(0, 1, 0).mat("leather").box(-4.5f, -1, -3, 9, 2, 6, 0.1f).add();
        st(b, "loin_flap", "loin", cloth).at(0, 1, -3).rot(-6, 0, 0).mat("leather").box(-2, 0, -0.5f, 4, 5, 1).box(-2, 0, 5.2f, 4, 4, 1).add();
        st(b, "torso", "hips", skin).accent(dark).at(0, -1.5f, 0).rot(32, 0, 0).mat("skin").box(-5, -11, -3, 10, 11, 6).add();
        st(b, "chest", "torso", dark).at(0, -10, -3).mat("skin").box(-4.5f, 0, -0.8f, 4, 4, 1).box(0.5f, 0, -0.8f, 4, 4, 1).add();
        st(b, "ribs", "torso", dark).at(0, -5, -3).mat("skin").box(-3.5f, 0, -0.6f, 7, 1, 1).box(-3, 2, -0.6f, 6, 1, 1).add();
        st(b, "back", "torso", skin).at(0, -11, 3).rot(-12, 0, 0).mat("skin").box(-4.5f, 0, -1.5f, 9, 7, 2).add();
        if (kurask) {
            for (int i = 0; i < 5; i++) {
                st(b, "spike" + i, "torso", horn).at(i % 2 == 0 ? 0 : (i == 1 ? 2.5f : -2.5f), -10 + i * 2.2f, 3.5f).rot(50, 0, 0).mat("horn")
                        .box(-0.5f, -0.5f, -0.5f, 1, 1, 3 - (i % 2), 0.15f).add();
            }
        }
        st(b, "neck", "torso", skin).at(0, -11, -1.5f).mat("skin").box(-2, -2, -2, 4, 3, 4).add();
        Kit.part(b, "head", "torso", Role.HEAD, skin).accent(kurask ? 0xF0D040 : 0x302018).at(0, -12, -2.5f).rot(-34, 0, 0).mat("skin")
                .pattern("eyes").glow(kurask ? "eyes" : null).box(-3, -5, -3.5f, 6, 6, 5).add();
        st(b, "brow", "head", dark).at(0, -3.5f, -3.7f).rot(16, 0, 0).mat("skin").box(-3.2f, -1, -0.6f, 6, 1, 2).add();
        st(b, "nose", "head", dark).at(0, -2.5f, -3.5f).rot(20, 0, 0).mat("skin").box(-1, 0, -2.5f, 2, 3, 2).add();
        st(b, "nose_tip", "nose", darker(dark, 0.85f)).at(0, 3, -1.5f).rot(30, 0, 0).mat("skin").box(-1.5f, -0.5f, -1, 3, 2, 2, -0.1f).add();
        Kit.part(b, "jaw", "head", Role.JAW, dark).at(0, 0.5f, -2.5f).rot(8, 0, 0).mat("skin").box(-2.5f, 0, -1.5f, 5, 2, 3).add();
        sy(b, "tusk", "jaw", TOOTH).at(1.8f, 0, -1.3f).rot(-10, 0, 10).mat("horn").box(-0.5f, -2, -0.5f, 1, 2, 1, -0.1f).add();
        sy(b, "ear", "head", skin).at(3, -3, 0).rot(0, -30, -40).mat("skin").box(0, -1, -0.5f, 3, 3, 1).add();
        if (kurask) {
            horn(b, "horn", "head", true, false, horn, 2, -4.5f, 0, -35, 0, -30, new int[]{2, 1, 1}, new int[]{3, 3, 2}, -30, 5);
            st(b, "crown_spike", "head", horn).at(0, -5, 0).rot(-30, 0, 0).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1, 0.15f).add();
        } else {
            st(b, "hair", "head", 0x4A3A28).at(0, -5, 0.5f).mat("hair").box(-2.5f, -1, -2.5f, 5, 2, 4).add();
        }
        Kit.sym(b, "arm", "torso", false, skin).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(dark).at(6, -9.5f, 0).rot(-40, 0, -8)
                .mat("skin").box(-2, -1.5f, -2, 4, 9, 4).add();
        sy2(b, "shoulder", "arm", dark).at(0, -1, 0).mat("skin").box(-2.5f, -1.5f, -2.5f, 5, 3, 5).add();
        sy2(b, "forearm", "arm", skin).at(0, 8.5f, 0).rot(-35, 0, 6).mat("skin").box(-2, 0, -2, 4, 8, 4, 0.15f).add();
        sy2(b, "wrist", "forearm", cloth).at(0, 5, 0).mat("leather").box(-2, 0, -2, 4, 2, 4, 0.35f).add();
        sy2(b, "hand", "forearm", dark).at(0, 8, 0).rot(10, 0, 0).mat("skin").box(-2, 0, -2, 4, 2, 4).add();
        Kit.sym(b, "talons", "hand", true, CLAW).at(0, 2, -1).rot(25, 0, 0).mat("horn")
                .box(-2, 0, -1, 1, 3, 1, -0.05f).box(-0.5f, 0, -1.2f, 1, 3, 1, -0.05f).box(1, 0, -1, 1, 3, 1, -0.05f).add();
        Kit.sym(b, "thigh", null, false, skin).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(dark).at(2.5f, 11, 1)
                .rot(-14, 0, -5).mat("skin").box(-2, -1, -2, 4, 7, 4).add();
        sy2(b, "shin", "thigh", skin).at(0, 6, 0).rot(20, 0, 4).mat("skin").box(-1.5f, 0, -1.5f, 3, 6, 3).add();
        sy2(b, "knee", "shin", dark).at(0, 0, -1.5f).mat("skin").box(-1.5f, -1, -0.8f, 3, 2, 1).add();
        sy2(b, "foot", "shin", dark).at(0, 5.5f, 0).rot(-6, 0, 0).mat("skin").box(-2, 0, -3.5f, 4, 2, 5).add();
        claws(b, "toe_claws", "foot", true, CLAW, 3, 2.5f, 1, -3.3f, 1);
        return b.build();
    }

    /** Mutated zygomite: a walking fungus - a broad spotted cap over a stalk body with root legs and gill fringe. */
    static ModelSpec zygomite(String id) {
        int cap = 0x8A3A2A;
        int spot = 0xE8D8B0;
        int stalk = 0xD0C098;
        int gill = 0xA89070;
        int dark = 0x5A4A30;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.6f);
        Kit.part(b, "stalk", null, Role.BODY, stalk).accent(dark).at(0, 18, 0).mat("skin").box(-3, -9, -3, 6, 9, 6).add();
        st(b, "ring", "stalk", gill).at(0, -6, 0).mat("skin").box(-3.5f, 0, -3.5f, 7, 1, 7, 0.2f).add();
        st(b, "base", "stalk", dark).at(0, 0, 0).mat("moss").box(-4, -1, -4, 8, 2, 8).add();
        Kit.part(b, "head", "stalk", Role.HEAD, cap).accent(spot).at(0, -9, 0).mat("skin").pattern("spots").box(-7, -4, -7, 14, 4, 14).add();
        st(b, "cap_top", "head", cap).accent(spot).at(0, -4, 0).mat("skin").pattern("spots").box(-5, -2, -5, 10, 2, 10).add();
        st(b, "cap_peak", "cap_top", darker(cap, 0.85f)).accent(spot).at(0, -2, 0).mat("skin").box(-2.5f, -1.5f, -2.5f, 5, 2, 5).add();
        st(b, "gills", "head", gill).at(0, 0, 0).mat("skin").box(-6.5f, -0.5f, -6.5f, 13, 1, 13, -0.2f).add();
        for (int k = 0; k < 8; k++) {
            float a = (float) Math.toRadians(k * 45);
            st(b, "fringe" + k, "head", k % 2 == 0 ? cap : darker(cap, 0.8f)).at((float) Math.sin(a) * 6.5f, 0, (float) Math.cos(a) * 6.5f)
                    .rot(0, k * 45, 0).mat("skin").box(-1.5f, -0.5f, -0.5f, 3, 2, 1).add();
        }
        Kit.part(b, "face", "stalk", Role.STATIC, stalk).accent(0xA0FF60).at(0, -7, -3).mat("skin").pattern("eyes").glow("eyes")
                .box(-2.5f, 0, -0.6f, 5, 4, 1).add();
        Kit.part(b, "jaw", "face", Role.JAW, dark).at(0, 3, -0.4f).mat("skin").box(-1.5f, 0, -0.5f, 3, 1, 1).add();
        Kit.sym(b, "arm", "stalk", false, stalk).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(dark).at(3.5f, -7, 0).rot(-30, 0, -30)
                .mat("skin").box(-1, -1, -1, 2, 5, 2).add();
        sy2(b, "forearm", "arm", stalk).at(0, 4.5f, 0).rot(-30, 0, 10).mat("skin").box(-1, 0, -1, 2, 4, 2, -0.05f).add();
        Kit.sym(b, "fingers", "forearm", true, dark).at(0, 4, 0).rot(15, 0, 0).mat("skin")
                .box(-1, 0, -1, 1, 2, 1, -0.1f).box(0.1f, 0, -1, 1, 2, 1, -0.1f).box(-0.5f, 0, 0.1f, 1, 2, 1, -0.1f).add();
        for (int k = 0; k < 4; k++) {
            float a = (float) Math.toRadians(k * 90 + 45);
            Kit.part(b, "root" + k, "stalk", k % 2 == 0 ? Role.LEG_FRONT_LEFT : Role.LEG_FRONT_RIGHT, stalk).accent(dark)
                    .at((float) Math.sin(a) * 2.5f, 0, (float) Math.cos(a) * 2.5f).rot((float) Math.cos(a) * 35, 0, (float) -Math.sin(a) * 35)
                    .mat("skin").box(-1, 0, -1, 2, 4, 2).add();
            st(b, "root_tip" + k, "root" + k, dark).at(0, 4, 0).rot((float) -Math.cos(a) * 25, 0, (float) Math.sin(a) * 25).mat("moss")
                    .box(-0.5f, 0, -0.5f, 1, 3, 1).add();
        }
        for (int k = 0; k < 3; k++) {
            Kit.part(b, "spore" + k, "head", Role.FLOAT, 0xC8F070).accent(0xC8F070).at(-4 + k * 4, -8 - (k % 2) * 2, k - 1).mat("energy").glow("full")
                    .box(-0.5f, -0.5f, -0.5f, 1, 1, 1).add();
        }
        return b.build();
    }

    /**
     * Aberrant / deviant spectre: a hovering, hunched spirit - huge drooping face with a great hooked nose, a
     * torso trailing into wispy tatters, long thin dangling arms, and a cloud of stench around it.
     */
    static ModelSpec spectre(String id, int skin, int dark, int light) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.5f);
        Kit.part(b, "body", null, Role.FLOAT, skin).accent(dark).at(0, 8, 0).rot(20, 0, 0).mat("ghost").box(-5, -9, -3.5f, 10, 9, 7).add();
        st(b, "hump", "body", dark).at(0, -9, 2).rot(-25, 0, 0).mat("ghost").box(-4.5f, -1, -2, 9, 5, 4).add();
        String t = chain(b, "wisp", "body", Role.TAIL, skin, dark, "ghost", null, 0, 0, 1, 1, new int[]{8, 6, 4, 3, 2},
                new int[]{6, 5, 4, 3, 2}, new int[]{3, 3, 3, 3, 3}, new float[]{60, 15, 10, 10, 5}, new float[]{0, 12, -12, 12, -12});
        st(b, "wisp_tip", t, light).at(0, 0, 3).mat("ghost").box(-0.5f, -0.5f, 0, 1, 1, 2).add();
        for (int k = 0; k < 6; k++) {
            float x = -4 + k * 1.6f;
            st(b, "tatter" + k, "body", k % 2 == 0 ? dark : skin).at(x, 0, k % 2 == 0 ? -3 : 3).rot(k % 2 == 0 ? -15 : 20, 0, (k - 2.5f) * 6)
                    .mat("ghost").box(-0.75f, 0, -0.5f, 1, 4 + (k % 3), 1, 0.25f).add();
        }
        Kit.part(b, "head", "body", Role.HEAD, skin).accent(0xF0F070).at(0, -8.5f, -2.5f).rot(-30, 0, 0).mat("ghost").pattern("eyes").glow("eyes")
                .box(-4, -7, -4, 8, 8, 6).add();
        st(b, "brow", "head", dark).at(0, -4.5f, -4.2f).rot(10, 0, 0).mat("ghost").box(-4.2f, -1, -0.6f, 8, 2, 2).add();
        st(b, "nose", "head", light).at(0, -4, -4).rot(30, 0, 0).mat("ghost").box(-1.5f, 0, -3, 3, 4, 3).add();
        st(b, "nose_tip", "nose", skin).at(0, 4, -2).rot(40, 0, 0).mat("ghost").box(-1.5f, -1, -1.5f, 3, 3, 2, -0.1f).add();
        sy(b, "nostril", "nose_tip", darker(dark, 0.6f)).at(0.8f, 1.6f, -1).mat("ghost").box(-0.5f, -0.3f, -0.5f, 1, 1, 1, -0.2f).add();
        Kit.part(b, "jaw", "head", Role.JAW, dark).at(0, 0.5f, -3).rot(14, 0, 0).mat("ghost").box(-3, 0, -1.5f, 6, 3, 3).add();
        st(b, "mouth", "jaw", 0x1E1A16).at(0, 0, -1.6f).mat("flesh").box(-2, 0.2f, -0.2f, 4, 1, 1, -0.1f).add();
        sy(b, "cheek", "head", light).at(3.5f, -2, -3).mat("ghost").box(-1, -1.5f, -1.5f, 2, 3, 3).add();
        sy(b, "ear", "head", skin).at(4, -4, -0.5f).rot(0, -30, -20).mat("ghost").box(0, -2, -0.5f, 3, 4, 1).add();
        st(b, "hair", "head", dark).at(0, -7, 0.5f).mat("hair").box(-3.5f, -1, -3, 7, 2, 5).add();
        Kit.sym(b, "arm", "body", false, skin).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(dark).at(5.5f, -7.5f, -0.5f).rot(-50, 0, -12)
                .mat("ghost").box(-1, -1, -1, 2, 8, 2).add();
        sy2(b, "forearm", "arm", skin).at(0, 7.5f, 0).rot(-20, 0, 6).mat("ghost").box(-1, 0, -1, 2, 7, 2, -0.1f).add();
        Kit.sym(b, "fingers", "forearm", true, dark).at(0, 7, 0).rot(15, 0, 0).mat("ghost")
                .box(-1, 0, -1, 1, 3, 1, -0.15f).box(0.1f, 0, -1, 1, 3, 1, -0.15f).box(-0.5f, 0, 0.1f, 1, 2, 1, -0.15f).add();
        for (int k = 0; k < 5; k++) {
            float a = (float) Math.toRadians(k * 72);
            Kit.part(b, "stench" + k, "body", Role.SPIN, k % 2 == 0 ? light : mix(light, 0xFFFFFF, 0.3f)).at((float) Math.cos(a) * 8, -4 - k, (float) Math.sin(a) * 8)
                    .mat("ghost").box(-1, -1, -1, 2, 2, 2, -0.2f).add();
        }
        return b.build();
    }

    /** Dust devil: a whirling column of dust (stacked spinning rings, wider at the top) with a scowling face and debris. */
    static ModelSpec dustDevil(String id, int dust, int dark) {
        int light = mix(dust, 0xFFFFFF, 0.25f);
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.5f);
        Kit.part(b, "base", null, Role.SPIN, dark).accent(light).at(0, 24, 0).mat("hide").box(-2, -3, -2, 4, 3, 4).add();
        int[] w = {4, 6, 7, 9, 10, 12, 13, 14};
        String prev = "base";
        for (int i = 0; i < w.length; i++) {
            String n = "swirl" + i;
            Kit.part(b, n, prev, i % 2 == 0 ? Role.SPIN : Role.STATIC, i % 2 == 0 ? dust : light).accent(dark)
                    .at(i == 0 ? 0 : (i % 2 == 0 ? 0.5f : -0.5f), i == 0 ? -3 : -3, 0).rot(0, 15, i % 2 == 0 ? 4 : -4).mat("hide")
                    .box(-w[i] / 2f, -3, -w[i] / 2f, w[i], 3, w[i]).add();
            st(b, n + "_band", n, dark).at(0, -1.5f, 0).rot(0, 45, 0).mat("hide").box(-w[i] / 2f + 0.5f, -0.5f, -w[i] / 2f + 0.5f, w[i] - 1, 1, w[i] - 1, 0.3f).add();
            prev = n;
        }
        Kit.part(b, "head", "swirl4", Role.HEAD, dust).accent(0xFF8030).at(4.8f, -2, -1.3f).rot(0, -75, 0).mat("hide").pattern("eyes").glow("eyes")
                .box(-3.5f, -3, -1.5f, 7, 5, 2).add();
        st(b, "brow", "head", dark).at(0, -1.8f, -1.6f).rot(0, 0, 0).mat("hide").box(-3.5f, -0.5f, -0.6f, 7, 1, 1).add();
        Kit.part(b, "jaw", "head", Role.JAW, 0x2A2016).at(0, 1, -1.6f).mat("hide").box(-2, 0, -0.4f, 4, 1, 1).add();
        st(b, "spray", "swirl7", light).at(0, -3, 0).rot(0, 30, 0).mat("hide").box(-5, -2, -5, 10, 2, 10, -0.2f).add();
        for (int k = 0; k < 4; k++) {
            st(b, "plume" + k, "spray", k % 2 == 0 ? dust : dark).at(k < 2 ? (k == 0 ? 4 : -4) : 0, -2, k >= 2 ? (k == 2 ? 4 : -4) : 0)
                    .rot(k >= 2 ? (k == 2 ? 30 : -30) : 0, 0, k < 2 ? (k == 0 ? -30 : 30) : 0).mat("hide").box(-1.5f, -3, -1.5f, 3, 3, 3, -0.2f).add();
        }
        sy(b, "arm", "swirl4", dust).accent(dark).at(5, -2, 0).rot(0, 0, -50).mat("hide").box(-1, -1, -1, 2, 5, 2).add();
        sy2(b, "claw", "arm", dark).at(0, 5, 0).rot(0, 0, 40).mat("horn").box(-0.5f, 0, -0.5f, 1, 3, 1).add();
        for (int k = 0; k < 6; k++) {
            float a = (float) Math.toRadians(k * 60);
            Kit.part(b, "debris" + k, "swirl2", Role.SPIN, k % 2 == 0 ? 0x6A5A44 : light).at((float) Math.cos(a) * 7.5f, -k % 3 * 2, (float) Math.sin(a) * 7.5f)
                    .rot(k * 20, k * 30, 0).mat("rock").box(-0.5f, -0.5f, -0.5f, 1 + k % 2, 1, 1).add();
        }
        return b.build();
    }

    /** Smoke devil: a floating smoky imp - a sooty bulbous body with a toothy grin, horn nubs, and curling smoke tentacles. */
    static ModelSpec smokeDevil(String id) {
        int smoke = 0x4A4A50;
        int dark = 0x26262A;
        int light = 0x7A7A82;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.6f);
        Kit.part(b, "body", null, Role.FLOAT, smoke).accent(light).at(0, 11, 0).mat("ghost").box(-5, -8, -5, 10, 8, 10).add();
        st(b, "puff_top", "body", light).at(0, -8, 0).mat("ghost").box(-4, -2, -4, 8, 2, 8).add();
        float[][] puffs = {{5, -5, 1}, {-5, -4, -1}, {2, -6, 5}, {-3, -3, 5}, {4, -1, -4}, {-4, -7, 3}};
        for (int i = 0; i < puffs.length; i++) {
            st(b, "puff" + i, "body", i % 2 == 0 ? light : smoke).at(puffs[i][0], puffs[i][1], puffs[i][2]).mat("ghost")
                    .box(-1.5f, -1.5f, -1.5f, 3, 3, 3, 0.2f).add();
        }
        Kit.part(b, "head", "body", Role.HEAD, smoke).accent(0xFF4A20).at(0, -4, -5).mat("ghost").pattern("eyes").glow("eyes")
                .box(-4, -3, -1, 8, 5, 2).add();
        Kit.part(b, "jaw", "head", Role.JAW, dark).at(0, 1.5f, -1).mat("ghost").box(-3, 0, -0.6f, 6, 2, 1).add();
        teeth(b, "grin", "jaw", 0xE0D8C8, 0, -0.9f, 2.8f, 1, false, true);
        sy(b, "horn", "body", dark).at(3, -9, -1).rot(-20, 0, -30).mat("horn").box(-1, -3, -1, 2, 3, 2).add();
        sy2(b, "horn_tip", "horn", 0x161618).at(0, -3, 0).rot(-20, 0, 20).mat("horn").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        for (int k = 0; k < 6; k++) {
            float a = (float) Math.toRadians(k * 60 + 30);
            String n = "tentacle" + k;
            Kit.part(b, n, "body", Role.TENTACLE, smoke).accent(light).at((float) Math.sin(a) * 3.5f, 0, (float) Math.cos(a) * 3.5f)
                    .rot((float) Math.cos(a) * 30, 0, (float) -Math.sin(a) * 30).mat("ghost").box(-1, 0, -1, 2, 4, 2).add();
            st(b, n + "_b", n, dark).at(0, 4, 0).rot((float) -Math.cos(a) * 20, 0, (float) Math.sin(a) * 20).mat("ghost").box(-0.5f, 0, -0.5f, 1, 4, 1, 0.1f).add();
            st(b, n + "_c", n + "_b", light).at(0, 4, 0).rot((float) Math.cos(a) * 30, 0, (float) -Math.sin(a) * 30).mat("ghost")
                    .box(-0.5f, 0, -0.5f, 1, 2, 1, -0.1f).add();
        }
        for (int k = 0; k < 3; k++) {
            Kit.part(b, "wisp" + k, "body", Role.SPIN, light).at(k * 4 - 4, -10 - k, k * 2 - 2).mat("ghost").box(-1, -1, -1, 2, 2, 2, -0.2f).add();
        }
        return b.build();
    }

    /** Cave kraken: a submerged mound of a head with a parrot beak, baleful eyes, and eight thick suckered tentacles. */
    static ModelSpec caveKraken(String id) {
        int skin = 0x6A3A5A;
        int dark = 0x3E1E36;
        int sucker = 0xD8A0B0;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.1f);
        Kit.part(b, "body", null, Role.BODY, skin).accent(dark).at(0, 22, 2).mat("flesh").pattern("spots").box(-7, -9, -6, 14, 9, 12).add();
        st(b, "mantle", "body", skin).accent(dark).at(0, -9, 2).rot(-20, 0, 0).mat("flesh").pattern("spots").box(-5.5f, -6, -4, 11, 6, 9).add();
        st(b, "mantle_tip", "mantle", dark).at(0, -6, 2).rot(-20, 0, 0).mat("flesh").box(-3.5f, -3, -3, 7, 3, 6).add();
        sy(b, "fin", "mantle", dark).at(5.5f, -3, 2).rot(0, 0, -30).mat("membrane").box(0, -2, -3, 3, 4, 6).add();
        Kit.part(b, "head", "body", Role.HEAD, skin).accent(0xFFC020).at(0, -4, -6).mat("flesh").pattern("eyes").glow("eyes")
                .box(-6, -4, -1.5f, 12, 6, 2).add();
        st(b, "beak", "head", 0x2A2420).at(0, 1.5f, -1.5f).rot(20, 0, 0).mat("horn").box(-1.5f, -1, -3, 3, 3, 3).add();
        Kit.part(b, "jaw", "beak", Role.JAW, 0x1A1614).at(0, 2, -1).rot(20, 0, 0).mat("horn").box(-1, 0, -1.5f, 2, 1, 2).add();
        for (int k = 0; k < 8; k++) {
            float a = (float) Math.toRadians(k * 45 + 22.5f);
            String n = "tentacle" + k;
            float cx = (float) Math.sin(a);
            float cz = (float) Math.cos(a);
            Kit.part(b, n, "body", Role.TENTACLE, skin).accent(sucker).at(cx * 6, -1, cz * 5).rot(-10, k * 45 + 22.5f, 0).mat("flesh")
                    .box(-1.5f, -1.5f, 0, 3, 3, 5).add();
            st(b, n + "_b", n, skin).at(0, 0, 5).rot(25 + (k % 3) * 12, 0, 0).mat("flesh").box(-1, -1, 0, 2, 2, 5).add();
            st(b, n + "_c", n + "_b", dark).at(0, 0, 5).rot(40, 0, 0).mat("flesh").box(-0.5f, -0.5f, 0, 1, 1, 4).add();
            st(b, n + "_sucker", n, sucker).at(0, 1.5f, 1).mat("flesh").box(-1, -0.5f, 0, 2, 1, 4, -0.1f).add();
        }
        return b.build();
    }

    /** Dark beast: a massive, hunched black quadruped with heavy forelimbs, a horned armoured skull and a wide fanged maw. */
    static ModelSpec darkBeast(String id) {
        int hide = 0x2E2E36;
        int dark = 0x18181E;
        int plate = 0x4A4A56;
        int horn = 0xB0A890;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.1f);
        Kit.part(b, "body", null, Role.BODY, hide).accent(plate).at(0, 12, 3).rot(-12, 0, 0).mat("hide").box(-6, -6, -8, 12, 11, 16).add();
        st(b, "hump", "body", hide).accent(plate).at(0, -6, -4).rot(-10, 0, 0).mat("hide").box(-6.5f, -4, -5, 13, 5, 10).add();
        for (int i = 0; i < 5; i++) {
            st(b, "plate" + i, "body", i % 2 == 0 ? plate : darker(plate, 0.8f)).at(0, -6.5f - (i < 2 ? 3 : 0), -6 + i * 3.4f).rot(-10, 0, 0)
                    .mat("plates").box(-4.5f + (i % 2), -1, -1.5f, 9 - 2 * (i % 2), 2, 3).add();
            st(b, "spike" + i, "plate" + i, horn).at(0, -1, 0).rot(-35, 0, 0).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 2, 0.1f).add();
        }
        st(b, "belly", "body", dark).at(0, 5, 0).mat("hide").box(-4.5f, -0.6f, -7, 9, 1, 14).add();
        // head
        st(b, "neck", "body", hide).accent(plate).at(0, -2, -8).rot(10, 0, 0).mat("hide").box(-4, -4, -4, 8, 8, 4).add();
        Kit.part(b, "head", "neck", Role.HEAD, hide).accent(0xFF2A20).at(0, 0, -4).rot(4, 0, 0).mat("hide").pattern("eyes").glow("eyes")
                .box(-4.5f, -4, -6, 9, 7, 6).add();
        st(b, "skull_plate", "head", plate).at(0, -4, -3).rot(-8, 0, 0).mat("plates").box(-4, -1, -3.5f, 8, 2, 7).add();
        st(b, "snout", "head", hide).at(0, 0.5f, -6).mat("hide").box(-3.5f, -2, -3, 7, 3, 3).add();
        teeth(b, "teeth_up", "snout", TOOTH, 1, -2.7f, 3.2f, 2, true, true);
        Kit.part(b, "jaw", "head", Role.JAW, dark).at(0, 2.5f, -5).rot(14, 0, 0).mat("hide").glow("maw").box(-3.5f, 0, -4.5f, 7, 2, 5).add();
        teeth(b, "teeth_lo", "jaw", TOOTH, 0, -4.2f, 3.2f, 2, false, true);
        horn(b, "horn", "head", true, false, horn, 3.5f, -3.5f, -2, -55, 20, -35, new int[]{2, 2, 1}, new int[]{3, 3, 3}, -30, 10);
        sy(b, "brow", "head", plate).at(3, -4, -5.5f).rot(15, -10, -15).mat("plates").box(-1.5f, -1, -1, 3, 2, 4).add();
        // legs: forelegs thick and long, hind legs shorter
        Kit.sym(b, "leg_f", null, false, hide).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(plate).at(6.5f, 9, -4)
                .rot(4, 0, -6).mat("hide").box(-2.5f, -3, -3, 5, 9, 6).add();
        sy2(b, "shoulder_plate", "leg_f", plate).at(1.5f, -3, 0).rot(0, 0, -15).mat("plates").box(-2, -1, -3.5f, 4, 3, 7).add();
        sy2(b, "fore_shin", "leg_f", hide).at(0, 6, 0).rot(-6, 0, 6).mat("hide").box(-2, 0, -2, 4, 8, 4, 0.2f).add();
        sy2(b, "fore_foot", "fore_shin", dark).at(0, 8, 0).mat("hide").box(-2.5f, 0, -3.5f, 5, 1, 5).add();
        claws(b, "fore_claws", "fore_foot", true, horn, 3, 3, 0.5f, -3.3f, 2);
        Kit.sym(b, "leg_b", null, false, hide).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(plate).at(5, 13, 9)
                .rot(-25, 0, -5).mat("hide").box(-2.5f, -3, -3, 5, 8, 6).add();
        sy2(b, "hind_shin", "leg_b", hide).at(0, 4.5f, -0.5f).rot(50, 0, 4).mat("hide").box(-1.5f, 0, -1.5f, 3, 5, 3).add();
        sy2(b, "hind_foot", "hind_shin", dark).at(0, 4.5f, 0).rot(-25, 0, 0).mat("hide").box(-2, 0, -3, 4, 1, 4).add();
        claws(b, "hind_claws", "hind_foot", true, horn, 3, 2.5f, 0.5f, -2.8f, 2);
        String t = chain(b, "tail", "body", Role.TAIL, hide, plate, "hide", null, 0, -2, 8, 1, new int[]{5, 4, 3, 2}, new int[]{5, 4, 3, 2},
                new int[]{4, 4, 4, 3}, new float[]{20, 6, 4, 4}, new float[]{0, 10, 10, -10});
        st(b, "tail_spike", t, horn).at(0, 0, 3).mat("horn").box(-0.5f, -0.5f, 0, 1, 1, 3).box(-1.5f, -0.5f, 0.5f, 3, 1, 1).add();
        return b.build();
    }

    /** Basilisk knight: an armoured basilisk warrior - scaled lizard head under a crested helm, plate armour, a sword and a mirror shield. */
    static ModelSpec basiliskKnight(String id) {
        int scale = 0x4A6E3A;
        int dark = 0x2C4422;
        int metal = 0x6E7480;
        int trim = 0xB09040;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.8f);
        Kit.part(b, "hips", null, Role.BODY, metal).accent(trim).at(0, 13, 1).mat("metal").box(-4.5f, -2, -2.5f, 9, 4, 5).add();
        st(b, "tasset", "hips", metal).at(0, 1, 0).mat("metal").box(-5, 0, -3, 4, 4, 1).box(1, 0, -3, 4, 4, 1).box(-5, 0, 2.5f, 10, 4, 1).add();
        st(b, "torso", "hips", metal).accent(trim).at(0, -1.5f, 0).rot(14, 0, 0).mat("metal").box(-5.5f, -11, -3.5f, 11, 11, 7).add();
        st(b, "breastplate", "torso", metal).accent(trim).at(0, -10, -3.5f).mat("metal").box(-4.5f, 0, -1, 9, 7, 1).add();
        st(b, "trim", "breastplate", trim).at(0, 0, -1).mat("metal").box(-0.5f, 0, -0.4f, 1, 7, 1).box(-4.5f, 6, -0.4f, 9, 1, 1).add();
        st(b, "belt", "hips", 0x4A3424).at(0, -2, 0).mat("leather").box(-5, -1, -3, 10, 1, 6, 0.2f).add();
        st(b, "cape", "torso", 0x5A2A2A).at(0, -10.5f, 3.6f).rot(8, 0, 0).mat("cloth").box(-4.5f, 0, 0, 9, 18, 1).add();
        sy(b, "pauldron", "torso", metal).accent(trim).at(5.5f, -10, 0).rot(0, 0, -20).mat("metal").box(-2, -1.5f, -3, 5, 3, 6).add();
        sy2(b, "pauldron_rim", "pauldron", trim).at(3, 1.2f, 0).mat("metal").box(-0.5f, -0.5f, -3, 1, 1, 6, 0.1f).add();
        Kit.part(b, "head", "torso", Role.HEAD, scale).accent(0xFFD020).at(0, -11.5f, -1.5f).rot(-14, 0, 0).mat("scales").pattern("eyes").glow("eyes")
                .box(-3, -4.5f, -4, 6, 5, 6).add();
        st(b, "snout", "head", scale).at(0, -1, -4).mat("scales").box(-2, -1.5f, -3, 4, 2, 3).add();
        Kit.part(b, "jaw", "head", Role.JAW, dark).at(0, 0.5f, -3.5f).rot(10, 0, 0).mat("scales").box(-2, 0, -3.5f, 4, 1, 4).add();
        teeth(b, "teeth", "snout", TOOTH, 0.5f, -2.7f, 1.8f, 1, true, false);
        st(b, "helm", "head", metal).accent(trim).at(0, -4.5f, 0).mat("metal").box(-3.5f, -1.5f, -3.5f, 7, 2, 7).add();
        st(b, "helm_crest", "helm", 0xA02A20).at(0, -1.5f, 0).mat("hair").box(-0.5f, -3, -3, 1, 3, 7).add();
        sy(b, "cheek_guard", "helm", metal).at(3.3f, 0.5f, -1).mat("metal").box(-0.5f, 0, -2, 1, 3, 4).add();
        Kit.sym(b, "arm", "torso", false, metal).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(trim).at(6.5f, -9, 0).rot(-20, 0, -8)
                .mat("chain").box(-1.5f, -1, -1.5f, 3, 6, 3).add();
        sy2(b, "forearm", "arm", metal).at(0, 5.5f, 0).rot(-30, 0, 0).mat("metal").box(-2, 0, -2, 4, 5, 4).add();
        sy2(b, "hand", "forearm", scale).at(0, 5, 0).mat("scales").box(-1.5f, 0, -1.5f, 3, 2, 3).add();
        // sword in the right hand, shield on the left
        st(b, "sword_grip", "hand_r", 0x3A2A1A).at(0, 1, 0).rot(80, 0, 0).mat("leather").box(-0.5f, -2, -0.5f, 1, 3, 1).add();
        st(b, "sword_guard", "sword_grip", trim).at(0, -2, 0).mat("metal").box(-2, -1, -0.5f, 4, 1, 1).add();
        st(b, "sword_blade", "sword_guard", 0xC8CCD4).at(0, -1, 0).mat("metal").box(-1, -11, -0.5f, 2, 11, 1).add();
        st(b, "sword_tip", "sword_blade", 0xC8CCD4).at(0, -11, 0).mat("metal").box(-0.5f, -1.5f, -0.5f, 1, 2, 1, -0.05f).add();
        st(b, "shield", "forearm_l", 0x9AA8B8).accent(trim).at(2.5f, 2, 0).rot(0, 0, 0).mat("metal").box(0, -4, -4, 1, 9, 8).add();
        st(b, "shield_rim", "shield", trim).at(1, 0, 0).mat("metal").box(-0.2f, -4, -4, 1, 1, 8).box(-0.2f, 4, -4, 1, 1, 8)
                .box(-0.2f, -3, -4, 1, 7, 1).box(-0.2f, -3, 3, 1, 7, 1).add();
        st(b, "shield_mirror", "shield", 0xD8F0F8).accent(0xFFFFFF).at(1, 0.5f, 0).mat("ice").box(-0.1f, -2.5f, -2.5f, 1, 5, 5).add();
        Kit.sym(b, "thigh", null, false, metal).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(trim).at(2.5f, 13, 1)
                .rot(-8, 0, -3).mat("chain").box(-2, -1, -2, 4, 6, 4).add();
        sy2(b, "greave", "thigh", metal).at(0, 5.5f, 0).rot(10, 0, 2).mat("metal").box(-2, 0, -2, 4, 5, 4, 0.1f).add();
        sy2(b, "knee", "greave", trim).at(0, 0, -2).mat("metal").box(-1.5f, -1, -0.8f, 3, 2, 1).add();
        sy2(b, "foot", "greave", scale).at(0, 5, 0).rot(-2, 0, 0).mat("scales").box(-2, 0, -3.5f, 4, 1, 5).add();
        claws(b, "toe_claws", "foot", true, CLAW, 3, 2.5f, 0.5f, -3.3f, 1);
        String t = chain(b, "tail", "hips", Role.TAIL, scale, dark, "scales", null, 0, 0, 2.5f, 1, new int[]{3, 3, 2, 2, 1},
                new int[]{3, 3, 2, 2, 1}, new int[]{4, 4, 4, 3, 3}, new float[]{45, 12, 8, 4, 0}, new float[]{0, 10, 10, -8, -10});
        st(b, "tail_tip", t, dark).at(0, 0, 3).mat("horn").box(-0.5f, -0.5f, 0, 1, 1, 2).add();
        return b.build();
    }

    /** Cave horror / cave abomination: a lanky, stooped jungle-cave ape-thing with a huge gaping mouth, bat ears and long arms. */
    static ModelSpec caveHorror(String id, int skin, int light) {
        int dark = darker(skin, 0.6f);
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.8f);
        Kit.part(b, "hips", null, Role.BODY, skin).accent(light).at(0, 12, 1).mat("hide").box(-4, -2, -2.5f, 8, 4, 5).add();
        st(b, "torso", "hips", skin).accent(light).at(0, -1.5f, 0).rot(38, 0, 0).mat("hide").box(-5, -11, -3.5f, 10, 11, 7).add();
        st(b, "chest", "torso", light).at(0, -9, -3.5f).mat("hide").box(-4, 0, -0.8f, 8, 6, 1).add();
        st(b, "ribs", "torso", dark).at(0, -4, -3.5f).mat("hide").box(-3.5f, 0, -0.6f, 7, 1, 1).box(-3, 2, -0.6f, 6, 1, 1).add();
        for (int i = 0; i < 4; i++) {
            st(b, "vertebra" + i, "torso", light).at(0, -10 + i * 2.6f, 3.5f).mat("bone").box(-0.75f, -0.5f, -0.5f, 1, 1, 1, 0.25f).add();
        }
        Kit.part(b, "head", "torso", Role.HEAD, skin).accent(0xF0E040).at(0, -11, -2).rot(-38, 0, 0).mat("hide").pattern("eyes").glow("eyes")
                .box(-3.5f, -6, -4, 7, 6, 6).add();
        st(b, "brow", "head", dark).at(0, -4.5f, -4.2f).rot(14, 0, 0).mat("hide").box(-3.8f, -1, -0.6f, 8, 2, 2).add();
        st(b, "upper_lip", "head", light).at(0, -2, -4).mat("hide").box(-3, 0, -1.5f, 6, 2, 2).add();
        teeth(b, "teeth_up", "upper_lip", TOOTH, 2, -1.4f, 2.8f, 1, true, true);
        Kit.part(b, "jaw", "head", Role.JAW, dark).at(0, 0, -2.5f).rot(28, 0, 0).mat("hide").box(-3, 0, -3.5f, 6, 3, 4).add();
        st(b, "maw", "jaw", 0x4A1018).at(0, -0.2f, -1.5f).mat("flesh").box(-2.5f, 0, -1.5f, 5, 1, 3).add();
        teeth(b, "teeth_lo", "jaw", TOOTH, 0, -3.2f, 2.8f, 1, false, true);
        sy(b, "ear", "head", skin).at(3.5f, -4, 0).rot(-10, -20, -30).mat("membrane").box(0, -3, -0.5f, 3, 5, 1).add();
        st(b, "crest", "head", dark).at(0, -6, 0).mat("hair").box(-1, -1.5f, -3, 2, 2, 6).add();
        Kit.sym(b, "arm", "torso", false, skin).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(light).at(6, -9.5f, 0).rot(-42, 0, -8)
                .mat("hide").box(-1.5f, -1.5f, -1.5f, 3, 10, 3).add();
        sy2(b, "shoulder", "arm", dark).at(0, -1, 0).mat("hide").box(-2, -1.5f, -2, 4, 3, 4).add();
        sy2(b, "forearm", "arm", skin).at(0, 9.5f, 0).rot(-28, 0, 6).mat("hide").box(-1.5f, 0, -1.5f, 3, 9, 3, 0.1f).add();
        sy2(b, "hand", "forearm", dark).at(0, 9, 0).mat("hide").box(-2, 0, -2, 4, 2, 4).add();
        Kit.sym(b, "talons", "hand", true, CLAW).at(0, 2, -1).rot(25, 0, 0).mat("horn")
                .box(-2, 0, -1, 1, 3, 1, -0.05f).box(-0.5f, 0, -1.2f, 1, 3, 1, -0.05f).box(1, 0, -1, 1, 3, 1, -0.05f).add();
        Kit.sym(b, "thigh", null, false, skin).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(light).at(2.5f, 12, 1)
                .rot(-25, 0, -6).mat("hide").box(-2, -1, -2, 4, 6, 4).add();
        sy2(b, "shin", "thigh", skin).at(0, 5, 0).rot(45, 0, 5).mat("hide").box(-1.5f, 0, -1.5f, 3, 5, 3).add();
        sy2(b, "ankle", "shin", dark).at(0, 4.5f, 0).rot(-20, 0, 0).mat("hide").box(-1, 0, -1, 2, 3, 2).add();
        sy2(b, "foot", "ankle", dark).at(0, 2.5f, 0).mat("hide").box(-2, 0, -3.5f, 4, 1, 5).add();
        claws(b, "toe_claws", "foot", true, CLAW, 3, 2.5f, 0.5f, -3.3f, 1);
        st(b, "loincloth", "hips", 0x4A3A28).at(0, 1, -2.6f).mat("leather").box(-2, 0, -0.5f, 4, 5, 1).add();
        return b.build();
    }

    /** Lizardman: a stooped bipedal lizard warrior - crested reptile head, scaly hide with a pale belly, clawed hands and a thick tail. */
    static ModelSpec lizardman(String id) {
        int scale = 0x5A7A3A;
        int dark = 0x34502A;
        int belly = 0xC8C088;
        int crest = 0xB05A2A;
        int cloth = 0x6A4A2A;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.8f);
        Kit.part(b, "hips", null, Role.BODY, scale).accent(belly).at(0, 11, 1).mat("scales").box(-4, -2, -2.5f, 8, 4, 5).add();
        st(b, "loin", "hips", cloth).at(0, -0.5f, 0).mat("leather").box(-4.5f, -1, -3, 9, 2, 6, 0.1f).add();
        st(b, "loin_flap", "loin", cloth).at(0, 1, -3).rot(-8, 0, 0).mat("leather").box(-2, 0, -0.5f, 4, 6, 1).add();
        st(b, "torso", "hips", scale).accent(belly).at(0, -1.5f, 0).rot(26, 0, 0).mat("scales").box(-5, -11, -3, 10, 11, 6).add();
        Kit.P bp = st(b, "belly", "torso", belly).accent(darker(belly, 0.7f)).at(0, -10, -3).mat("plates");
        for (int r = 0; r < 4; r++) {
            bp.box(-3.5f + (r == 3 ? 0.5f : 0), r * 2.5f, -0.7f, 7 - (r == 3 ? 1 : 0), 2, 1, 0.05f);
        }
        bp.add();
        for (int i = 0; i < 5; i++) {
            st(b, "dorsal" + i, "torso", crest).at(0, -10.5f + i * 2.4f, 3).rot(45, 0, 0).mat("horn").box(-0.5f, -0.5f, -0.5f, 1, 1, 2 + (i % 2)).add();
        }
        st(b, "bandolier", "torso", 0x4A3420).at(0, -10, 0).rot(0, 0, 32).mat("leather").box(-0.75f, 0, -3.5f, 1, 13, 7, 0.1f).add();
        st(b, "neck", "torso", scale).at(0, -11, -1).rot(-20, 0, 0).mat("scales").box(-2, -3, -2.5f, 4, 4, 4).add();
        Kit.part(b, "head", "neck", Role.HEAD, scale).accent(0xF0C020).at(0, -3, -1).rot(-6, 0, 0).mat("scales").pattern("eyes").glow("eyes")
                .box(-3, -4, -4, 6, 5, 5).add();
        st(b, "snout", "head", scale).at(0, -1, -4).mat("scales").box(-2, -1.5f, -4, 4, 2, 4).add();
        sy(b, "nostril", "snout", darker(scale, 0.4f)).at(1, -1.5f, -3.6f).mat("scales").box(-0.5f, -0.3f, -0.5f, 1, 1, 1, -0.2f).add();
        Kit.part(b, "jaw", "head", Role.JAW, belly).at(0, 0.5f, -3.5f).rot(10, 0, 0).mat("plates").box(-2, 0, -4.5f, 4, 1, 5).add();
        teeth(b, "teeth", "snout", TOOTH, 0.5f, -3.6f, 1.8f, 2, true, false);
        for (int k = 0; k < 3; k++) {
            st(b, "crest" + k, "head", k == 1 ? crest : mix(crest, dark, 0.3f)).at(0, -4 + k * 0.5f, -2 + k * 2.2f).rot(-40, 0, 0).mat("membrane")
                    .box(-0.5f, -3 + (k == 2 ? 1 : 0), -1, 1, 3 - (k == 2 ? 1 : 0), 3).add();
        }
        sy(b, "frill", "head", crest).at(3, -1.5f, 0).rot(0, -40, -15).mat("membrane").box(0, -1.5f, 0, 1, 3, 3).add();
        Kit.sym(b, "arm", "torso", false, scale).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(belly).at(6, -9.5f, 0).rot(-30, 0, -10)
                .mat("scales").box(-1.5f, -1.5f, -1.5f, 3, 7, 3).add();
        sy2(b, "shoulder", "arm", dark).at(0, -1, 0).mat("scales").box(-2, -1.5f, -2, 4, 3, 4).add();
        sy2(b, "forearm", "arm", scale).at(0, 6.5f, 0).rot(-35, 0, 4).mat("scales").box(-1.5f, 0, -1.5f, 3, 6, 3, 0.1f).add();
        sy2(b, "bracer", "forearm", 0x4A3420).at(0, 2, 0).mat("leather").box(-1.5f, 0, -1.5f, 3, 2, 3, 0.35f).add();
        sy2(b, "hand", "forearm", dark).at(0, 6, 0).mat("scales").box(-1.5f, 0, -1.5f, 3, 2, 3).add();
        Kit.sym(b, "talons", "hand", true, CLAW).at(0, 2, -0.5f).rot(25, 0, 0).mat("horn")
                .box(-1.5f, 0, -1, 1, 2, 1, -0.05f).box(-0.5f, 0, -1.2f, 1, 2, 1, -0.05f).box(0.5f, 0, -1, 1, 2, 1, -0.05f).add();
        Kit.sym(b, "thigh", null, false, scale).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(belly).at(2.5f, 11, 1)
                .rot(-25, 0, -5).mat("scales").box(-2, -1, -2.5f, 4, 6, 5).add();
        sy2(b, "shin", "thigh", scale).at(0, 5, 0).rot(50, 0, 4).mat("scales").box(-1.5f, 0, -1.5f, 3, 5, 3).add();
        sy2(b, "ankle", "shin", dark).at(0, 4.5f, 0).rot(-25, 0, 0).mat("scales").box(-1, 0, -1, 2, 3, 2).add();
        sy2(b, "foot", "ankle", dark).at(0, 2.5f, 0).mat("scales").box(-2, 0, -3.5f, 4, 1, 5).add();
        claws(b, "toe_claws", "foot", true, CLAW, 3, 2.5f, 0.5f, -3.3f, 2);
        String t = chain(b, "tail", "hips", Role.TAIL, scale, belly, "scales", null, 0, 0, 2.5f, 1, new int[]{4, 4, 3, 2, 2, 1},
                new int[]{4, 3, 3, 2, 2, 1}, new int[]{4, 4, 4, 4, 3, 3}, new float[]{50, 10, 6, 4, 0, -6}, new float[]{0, 10, 10, -8, -10, -10});
        st(b, "tail_tip", t, dark).at(0, 0, 3).mat("horn").box(-0.5f, -0.5f, 0, 1, 1, 2).add();
        return b.build();
    }
}
