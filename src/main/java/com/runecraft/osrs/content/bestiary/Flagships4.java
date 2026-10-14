package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/**
 * Flagship boss re-sculpts, part 4 (round 21): TzTok-Jad rebuilt after the Jad concept sheet
 * (docs/reference/jad_concept.webp) and OSRS, JalTok-Jad on the same body, and the Giant Mole. Same rules as
 * {@link Flagships}: each spec was rendered before/after against the model it replaces and kept only where it reads
 * clearly closer to OSRS (a round-21 Hespori attempt was rejected: no clearer than the existing bulb and bloom).
 * Registered last in {@link Overrides}.
 */
final class Flagships4 {
    private Flagships4() {
    }

    static List<ModelSpec> overrides() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(jad("tztok_jad", 1.0f, 0xA81E1A, 0x6A1216, 0x2A1216, 0xFF6A14));
        // OSRS JalTok-Jad is the same beast in the Inferno's hotter, orange-brown rock; scaled so it stands about as
        // tall as TzTok-Jad (its creature render scale is 1.8 against Jad's 2.8)
        l.add(jad("jaltok_jad", 1.55f, 0xA43A1C, 0x6A2414, 0x26100E, 0xFFA030));
        l.add(giantMole());
        return l;
    }

    /**
     * TzTok-Jad: a colossal lava-rock beast on four limbs, its weight on two pillar-like forelimbs with huge dark
     * rock feet, towering boulder shoulders, the heavy head slung low and forward between them with a glowing brow
     * and maw and great rock crests swept back over the skull, a mane of molten spikes along the back, and short,
     * bowed hind legs. Crimson rock mottled black, with a few thick molten seams.
     */
    static ModelSpec jad(String id, float scale, int red, int deep, int dark, int lava) {
        int foot = 0x24121A;
        int hot = 0xFF9A3A;
        int light = mix(red, 0xFF6040, 0.25f);
        ModelSpec.Builder b = ModelSpec.builder(id).scale(scale).shadow(1.5f);
        // hips and a hunched torso rising to the shoulders
        part(b, "hips", null, Role.BODY, deep).accent(lava).at(0, 11, 7).mat("rock")
                .box(-7, -5, -5, 14, 10, 11).add();
        part(b, "torso", "hips", Role.STATIC, red).accent(lava).at(0, -3, -3).rot(30, 0, 0).mat("rock")
                .box(-8, -10, -7, 16, 11, 12).add();
        part(b, "chest", "torso", Role.STATIC, deep).accent(lava).at(0, -5, -7).mat("rock")
                .box(-6, -5, -1.5f, 12, 9, 2).add();
        // boulder shoulders standing higher than the head
        sym(b, "shoulder", "torso", false, red).accent(lava).at(9.5f, -8, -3).rot(-15, 0, -8).mat("rock").glow("cracks")
                .box(-5.5f, -6, -6.5f, 11, 11, 13).add();
        sym(b, "shoulder_cap", "shoulder", true, dark).accent(lava).at(0, -6, 0).rot(0, 0, -6).mat("rock")
                .box(-4.5f, -2, -5, 9, 2, 10).add();
        // the molten mane: spikes sweeping back along the spine and over the shoulders
        float[][] mane = {{0, -10, -2, -62, 0, 0, 8}, {0, -9, 2, -72, 0, 0, 9}, {0, -7, 5, -82, 0, 0, 7}};
        for (int i = 0; i < mane.length; i++) {
            float[] m = mane[i];
            int h = (int) m[6];
            part(b, "mane" + i, "torso", Role.STATIC, deep).accent(lava).at(m[0], m[1], m[2]).rot(m[3], 0, 0).mat("rock")
                    .box(-2, -h, -2, 4, h, 4).add();
            part(b, "mane_tip" + i, "mane" + i, Role.STATIC, lava).accent(hot).at(0, -h, 0).rot(-10, 0, 0).mat("lava").glow("full")
                    .box(-1, -4, -1, 2, 4, 2).add();
        }
        sym(b, "spike", "shoulder", true, dark).accent(lava).at(1, -7, 2).rot(-55, 0, -25).mat("rock")
                .box(-1.5f, -6, -1.5f, 3, 6, 3).add();
        sym(b, "spike_tip", "spike", true, lava).accent(hot).at(0, -6, 0).rot(-10, 0, 0).mat("lava").glow("full")
                .box(-1, -3, -1, 2, 3, 2).add();
        // head slung low and forward between the shoulders
        part(b, "head", "torso", Role.HEAD, red).accent(hot).at(0, -5, -9).rot(-24, 0, 0).mat("rock").pattern("eyes").glow("eyes")
                .box(-5, -4, -8, 10, 8, 9).add();
        part(b, "brow", "head", Role.STATIC, dark).accent(lava).at(0, -4.5f, -8).rot(10, 0, 0).mat("rock")
                .box(-6, -1.5f, -2, 12, 2, 3).add();
        part(b, "jaw", "head", Role.JAW, deep).accent(lava).at(0, 3, -4).rot(8, 0, 0).mat("rock").glow("maw")
                .box(-5, 0, -6, 10, 4, 7).add();
        sym(b, "tusk", "jaw", false, 0x3A1A1C).accent(lava).at(4, 0, -5.5f).rot(-15, 0, 10).mat("rock").box(-1, -3, -1, 2, 3, 2).add();
        // great rock crests swept back over the skull
        sym(b, "crest", "head", false, light).accent(lava).at(3, -3, -6).rot(-20, 10, 8).mat("rock")
                .box(-2, -9, -2.5f, 4, 9, 5).add();
        sym(b, "crest_tip", "crest", true, light).accent(lava).at(0, -9, 0).rot(-40, 0, 0).mat("rock")
                .box(-1.5f, -4, -2, 3, 4, 4).add();
        part(b, "crown", "head", Role.STATIC, deep).accent(lava).at(0, -4, -2).rot(-50, 0, 0).mat("rock")
                .box(-1.5f, -6, -2, 3, 6, 4).add();
        // pillar forelimbs on huge dark rock feet
        sym(b, "arm", null, false, red).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(lava).at(11, 3, -5).rot(-4, 0, -10)
                .mat("rock").glow("cracks").box(-4, -3, -4, 8, 12, 8).add();
        sym(b, "forearm", "arm", true, red).accent(lava).at(0, 9, 0).rot(4, 0, 9).mat("rock").glow("cracks")
                .box(-4.5f, 0, -4.5f, 9, 9, 9).add();
        sym(b, "fist", "forearm", true, foot).accent(lava).at(0, 8.5f, -0.5f).mat("rock")
                .box(-5.5f, 0, -5.5f, 11, 4, 11).box(-5.5f, 1, -7.5f, 3, 3, 2).box(-1.5f, 1, -7.5f, 3, 3, 2).box(2.5f, 1, -7.5f, 3, 3, 2).add();
        // short, bowed hind legs
        sym(b, "leg", null, false, red).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(lava).at(7, 12, 8)
                .rot(-25, 0, -6).mat("rock").glow("cracks").box(-4, -2, -4, 8, 8, 8).add();
        sym(b, "shin", "leg", true, deep).accent(lava).at(0, 5.5f, 0).rot(30, 0, 4).mat("rock")
                .box(-3.5f, 0, -3.5f, 7, 5, 7).add();
        sym(b, "foot", "shin", true, foot).accent(lava).at(0, 4.5f, 0).rot(-5, 0, 2).mat("rock")
                .box(-4.5f, 0, -6, 9, 3, 9).box(-4.5f, 0.5f, -8, 2, 2, 2).box(-1, 0.5f, -8, 2, 2, 2).box(2.5f, 0.5f, -8, 2, 2, 2).add();
        return b.build();
    }

    /**
     * Giant Mole: an enormous, rounded mound of dark velvet fur with a small head ending in a long pink snout, tiny
     * eyes, huge pink spade-like digging hands turned out to the sides with pale claws, stubby pink hind feet and a
     * short naked tail.
     */
    static ModelSpec giantMole() {
        int fur = 0x3C2E28;
        int deep = 0x281E1A;
        int pink = 0xD48C84;
        int nose = 0xB0605C;
        int claw = 0xE6DCC8;
        ModelSpec.Builder b = ModelSpec.builder("giant_mole").shadow(1.6f);
        // a loaf of fur built up in layers so it reads round
        part(b, "body", null, Role.BODY, fur).accent(deep).at(0, 15, 4).mat("fur")
                .box(-11, -8, -14, 22, 15, 28).box(-12, -5, -12, 24, 9, 24).box(-9, -10, -12, 18, 2, 24)
                .box(-6, -11, -9, 12, 1, 18).box(-9, -6, 14, 18, 11, 2).add();
        part(b, "head", "body", Role.HEAD, fur).accent(deep).at(0, 0, -14).rot(0, 0, 0).mat("fur").pattern("eyes_animal")
                .box(-6, -5, -6, 12, 9, 7).add();
        part(b, "muzzle", "head", Role.STATIC, fur).accent(deep).at(0, 1, -6).mat("fur").box(-4, -3, -4, 8, 6, 4).add();
        part(b, "snout", "muzzle", Role.STATIC, pink).accent(nose).at(0, 0, -4).rot(-6, 0, 0).mat("flesh")
                .box(-2, -1.5f, -6, 4, 3, 6).add();
        part(b, "nose", "snout", Role.STATIC, nose).at(0, 0, -6).mat("flesh").box(-1.5f, -1, -1, 3, 2, 1).add();
        // spade hands turned out to the sides
        sym(b, "arm", "body", false, fur).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(deep).at(10, 3, -10)
                .rot(0, -20, -70).mat("fur").box(-2.5f, -2, -3, 5, 5, 6).add();
        sym(b, "paw", "arm", true, pink).accent(nose).at(0, 4, 0).rot(0, 0, 70).mat("flesh")
                .box(-1.5f, -3, -5, 3, 7, 10).add();
        sym(b, "claws", "paw", true, claw).at(0, 0, -5).mat("horn")
                .box(-0.5f, -2.5f, -3, 2, 2, 3).box(-0.5f, 0, -3, 2, 2, 3).box(-0.5f, 2, -3, 2, 2, 3).add();
        sym(b, "hind", "body", false, pink).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(nose).at(8, 5, 11).mat("flesh")
                .box(-2.5f, 0, -4, 5, 4, 7).add();
        part(b, "tail", "body", Role.TAIL, pink).accent(nose).at(0, 2, 16).rot(-25, 0, 0).mat("flesh").box(-1, -1, 0, 2, 2, 6).add();
        return b.build();
    }

    private static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
