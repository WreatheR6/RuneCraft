package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import static com.runecraft.osrs.content.bestiary.BestiarySlmKit.*;

/**
 * Demons and demon-kin: lesser/greater/black demons (and their Catacombs cousins), imps, abyssal demons,
 * hellhounds and terror dogs, gargoyles, nechryael and bloodvelds.
 */
final class BestiarySlmDemons {
    private BestiarySlmDemons() {
    }

    /**
     * OSRS demon: a hunched, heavily muscled humanoid on digitigrade legs with long clawed arms, a horned,
     * fanged head, a whip tail and (greater/black) bat wings.
     */
    static ModelSpec demon(String id, int skin, int dark, int horn, boolean wings) {
        int belly = mix(skin, 0xE8B080, 0.25f);
        int eye = 0xFFD030;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.9f);
        Kit.part(b, "hips", null, Role.BODY, skin).accent(dark).at(0, 12, 2).mat("skin").box(-4.5f, -2, -3, 9, 4, 6).add();
        st(b, "torso", "hips", skin).accent(dark).at(0, -1.5f, 0).rot(24, 0, 0).mat("skin").box(-6, -11, -4, 12, 11, 8).add();
        st(b, "pecs", "torso", skin).accent(dark).at(0, -10, -4).mat("skin").box(-5.5f, 0, -1, 5, 4, 1).box(0.5f, 0, -1, 5, 4, 1).add();
        Kit.P abs = st(b, "abs", "torso", belly).accent(dark).at(0, -5.5f, -4).mat("skin");
        for (int r = 0; r < 3; r++) {
            abs.box(-3, r * 1.8f, -0.8f, 2, 1, 1, 0.2f).box(1, r * 1.8f, -0.8f, 2, 1, 1, 0.2f);
        }
        abs.add();
        st(b, "back_hump", "torso", dark).at(0, -10, 3).rot(-10, 0, 0).mat("skin").box(-4.5f, 0, -1, 9, 6, 2).add();
        for (int i = 0; i < 3; i++) {
            st(b, "spine" + i, "torso", horn).at(0, -9.5f + i * 3.2f, 4).rot(55, 0, 0).mat("horn").box(-0.5f, -0.5f, -0.5f, 1, 1, 2).add();
        }
        sy(b, "trap", "torso", skin).accent(dark).at(3.5f, -11, 0.5f).rot(0, 0, -18).mat("skin").box(-2.5f, -1, -2.5f, 5, 2, 5).add();
        // head
        st(b, "neck", "torso", dark).at(0, -11, -1.5f).mat("skin").box(-2, -2, -2, 4, 3, 4).add();
        Kit.part(b, "head", "torso", Role.HEAD, skin).accent(eye).at(0, -12, -2.5f).rot(-26, 0, 0).mat("skin").pattern("eyes").glow("eyes")
                .box(-3.5f, -5, -4, 7, 6, 6).add();
        st(b, "brow", "head", dark).at(0, -3.6f, -4.2f).rot(14, 0, 0).mat("skin").box(-3.8f, -1, -0.6f, 8, 1, 2, -0.1f).add();
        st(b, "muzzle", "head", skin).accent(dark).at(0, -1.5f, -4).mat("skin").box(-2.5f, 0, -2, 5, 2, 2).add();
        sy(b, "nostril", "muzzle", darker(skin, 0.4f)).at(1, 0, -2.1f).mat("skin").box(-0.5f, 0, -0.3f, 1, 1, 1, -0.2f).add();
        Kit.part(b, "jaw", "head", Role.JAW, dark).at(0, 0.5f, -2.5f).rot(8, 0, 0).mat("skin").box(-3, 0, -3.5f, 6, 2, 4).add();
        sy(b, "fang", "muzzle", TOOTH).at(1.6f, 2, -1.5f).mat("horn").box(-0.5f, 0, -0.5f, 1, 2, 1, -0.1f).add();
        sy(b, "tusk", "jaw", TOOTH).at(2.4f, 0, -3).rot(-10, 0, 10).mat("horn").box(-0.5f, -2, -0.5f, 1, 2, 1, -0.1f).add();
        sy(b, "ear", "head", skin).at(3.5f, -3, -1).rot(-10, -30, -30).mat("skin").box(0, -1, -0.5f, 3, 2, 1).add();
        horn(b, "horn", "head", true, false, horn, 2.2f, -4.5f, -1.5f, -20, 10, -38, new int[]{2, 2, 1}, new int[]{3, 3, 3}, -28, 18);
        // arms
        Kit.sym(b, "arm", "torso", false, skin).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(dark).at(7, -9, 0).rot(-18, 0, -12)
                .mat("skin").box(-2.5f, -2, -2.5f, 5, 8, 5).add();
        sy2(b, "shoulder_spike", "arm", horn).at(1, -2, 0).rot(0, 0, -30).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1, 0.1f).add();
        sy2(b, "forearm", "arm", skin).accent(dark).at(0, 6, 0).rot(-28, 0, 4).mat("skin").box(-2.5f, 0, -2.5f, 5, 7, 5, 0.2f).add();
        sy2(b, "bracer", "forearm", dark).at(0, 2, 0).mat("skin").box(-2.5f, 0, -2.5f, 5, 1, 5, 0.45f).add();
        sy2(b, "hand", "forearm", dark).at(0, 7, 0).rot(14, 0, 0).mat("skin").box(-2.5f, 0, -2, 5, 3, 4).add();
        Kit.sym(b, "talons", "hand", true, CLAW).at(0, 3, -1).rot(18, 0, 0).mat("horn")
                .box(-2.5f, 0, -1, 1, 3, 1, -0.1f).box(-0.9f, 0, -1.2f, 1, 3, 1, -0.1f).box(0.7f, 0, -1.2f, 1, 3, 1, -0.1f)
                .box(1.6f, -1, 0.5f, 1, 2, 1, -0.1f).add();
        // digitigrade legs
        Kit.sym(b, "thigh", null, false, skin).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(dark).at(3, 12, 2)
                .rot(-22, 0, -4).mat("skin").box(-2.5f, -1, -2.5f, 5, 7, 5).add();
        sy2(b, "shin", "thigh", dark).at(0, 5, 0).rot(48, 0, 2).mat("skin").box(-2, 0, -2, 4, 5, 4).add();
        sy2(b, "ankle", "shin", dark).at(0, 4, 0).rot(-26, 0, 0).mat("skin").box(-1.5f, 0, -1.5f, 3, 3, 3).add();
        sy2(b, "foot", "ankle", dark).at(0, 2.5f, 0).mat("skin").box(-2, 0, -3.5f, 4, 1, 5).add();
        claws(b, "toe_claws", "foot", true, CLAW, 3, 2.5f, 0.5f, -3.2f, 2);
        // tail
        String t = chain(b, "tail", "hips", Role.TAIL, skin, dark, "skin", null, 0, 0, 2.5f, 1,
                new int[]{3, 2, 2, 1, 1}, new int[]{3, 2, 2, 1, 1}, new int[]{4, 4, 4, 4, 3}, new float[]{40, 14, 10, -10, -20},
                new float[]{0, 10, 10, 10, 10});
        st(b, "tail_tip", t, horn).at(0, 0, 3).mat("horn").box(-0.5f, -0.5f, 0, 1, 1, 3).box(-1.5f, -0.5f, 0.5f, 3, 1, 1).add();
        if (wings) {
            int web = darker(skin, 0.55f);
            Kit.sym(b, "wing", "torso", false, dark).roles(Role.WING_LEFT, Role.WING_RIGHT).accent(horn).at(3, -9, 3.5f).rot(-25, -40, -28)
                    .mat("skin").box(0, -1, -1, 8, 2, 2).add();
            sy2(b, "wing_web0", "wing", web).accent(dark).at(0, 0, 0.8f).mat("membrane").box(0, -0.25f, 0, 8, 1, 6, -0.25f).add();
            sy2(b, "wing_fore", "wing", dark).at(8, 0, 0).rot(0, 30, -10).mat("skin").box(0, -0.75f, -0.75f, 8, 1, 1, 0.3f).add();
            sy2(b, "wing_claw", "wing_fore", horn).at(8, 0, 0).rot(0, 40, -20).mat("horn").box(0, -0.5f, -0.5f, 2, 1, 1).add();
            for (int f = 0; f < 2; f++) {
                String fn = "wing_finger" + f;
                sy2(b, fn, "wing_fore", dark).at(7.5f, 0, 0).rot(0, f == 0 ? -45 : -95, 15 + f * 10).mat("horn")
                        .box(0, -0.5f, -0.5f, f == 0 ? 11 : 9, 1, 1).add();
                sy2(b, fn + "_web", fn, web).accent(dark).at(0, 0, 0.3f).mat("membrane").box(0, -0.25f, 0, f == 0 ? 10 : 8, 1, f == 0 ? 7 : 6, -0.25f).add();
            }
        }
        return b.build();
    }

    /** Imp: a small, impish red demon with a big head, long pointed ears, little horns, bat wings and a tail. */
    static ModelSpec imp(String id) {
        int skin = 0xB0301E;
        int dark = 0x5A1410;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.35f);
        Kit.part(b, "hips", null, Role.BODY, skin).accent(dark).at(0, 17, 0).mat("skin").box(-2, -1, -1.5f, 4, 2, 3).add();
        st(b, "torso", "hips", skin).accent(dark).at(0, -1, 0).rot(14, 0, 0).mat("skin").box(-2.5f, -5, -2, 5, 5, 4).add();
        st(b, "belly", "torso", mix(skin, 0xE0A070, 0.3f)).at(0, -2.5f, -2).mat("skin").box(-1.5f, -2, -0.5f, 3, 4, 1).add();
        Kit.part(b, "head", "torso", Role.HEAD, skin).accent(0xFFE040).at(0, -5, -0.5f).rot(-14, 0, 0).mat("skin").pattern("eyes").glow("eyes")
                .box(-3, -5, -3, 6, 5, 5).add();
        st(b, "snout", "head", skin).at(0, -1.5f, -3).mat("skin").box(-1, 0, -1.5f, 2, 1, 2).add();
        Kit.part(b, "jaw", "head", Role.JAW, dark).at(0, 0, -1.5f).mat("skin").box(-2.5f, 0, -1.5f, 5, 1, 3).add();
        teeth(b, "fangs", "jaw", TOOTH, 0, -1.4f, 2.2f, 1, false, true);
        sy(b, "ear", "head", skin).at(3, -3, 0).rot(10, -25, -20).mat("skin").box(0, -1, -0.5f, 4, 2, 1).add();
        sy2(b, "ear_tip", "ear", skin).at(4, 0, 0).rot(0, 0, -25).mat("skin").box(0, -0.5f, -0.5f, 2, 1, 1, -0.1f).add();
        horn(b, "horn", "head", true, false, 0xE0D8B8, 1.5f, -4.5f, -1, -30, 0, -25, new int[]{1, 1}, new int[]{2, 2}, -20, 10);
        Kit.sym(b, "arm", "torso", false, skin).roles(Role.ARM_LEFT, Role.ARM_RIGHT).at(3, -4.5f, 0).rot(-20, 0, -10).mat("skin")
                .box(-1, -0.5f, -1, 2, 4, 2).add();
        sy2(b, "forearm", "arm", skin).at(0, 3.5f, 0).rot(-30, 0, 0).mat("skin").box(-1, 0, -1, 2, 3, 2, -0.05f).add();
        Kit.sym(b, "claw", "forearm", true, CLAW).at(0, 3, 0).mat("horn").box(-1, 0, -1, 1, 1, 1, -0.1f).box(0.2f, 0, -1, 1, 1, 1, -0.1f).add();
        Kit.sym(b, "thigh", null, false, skin).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).at(1.2f, 17, 0).rot(-20, 0, -4)
                .mat("skin").box(-1, 0, -1, 2, 3, 2).add();
        sy2(b, "shin", "thigh", dark).at(0, 3, 0).rot(40, 0, 0).mat("skin").box(-1, 0, -1, 2, 3, 2).add();
        sy2(b, "foot", "shin", dark).at(0, 2.5f, 0).rot(-20, 0, 0).mat("skin").box(-1, 0, -2.5f, 2, 1, 3).add();
        Kit.sym(b, "wing", "torso", false, dark).roles(Role.WING_LEFT, Role.WING_RIGHT).at(1.5f, -4.5f, 2).rot(-30, -50, -55)
                .mat("skin").box(0, -0.5f, -0.5f, 5, 1, 1).add();
        sy2(b, "wing_web", "wing", darker(skin, 0.6f)).at(0, 0, 0.3f).mat("membrane").box(0, -0.25f, 0, 6, 1, 4, -0.25f).add();
        sy2(b, "wing_tip", "wing", dark).at(5, 0, 0).rot(0, -60, 10).mat("horn").box(0, -0.5f, -0.5f, 4, 1, 1, -0.1f).add();
        String t = chain(b, "tail", "hips", Role.TAIL, skin, dark, "skin", null, 0, 0, 1.5f, 1, new int[]{1, 1, 1}, new int[]{1, 1, 1},
                new int[]{3, 3, 3}, new float[]{40, -10, -30}, null);
        st(b, "tail_tip", t, dark).at(0, 0, 3).mat("horn").box(-1, -0.5f, 0, 2, 1, 2, -0.1f).add();
        return b.build();
    }

    /**
     * Abyssal demon: a tall, gaunt, hunched black-purple horror with very long clawed arms, a beaked skull-like
     * head, knobbly spine growths and a long barbed tail.
     */
    static ModelSpec abyssalDemon(String id) {
        int skin = 0x3E2440;
        int dark = 0x1E1022;
        int knob = 0x6A3A5A;
        int bone = 0xB8A888;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.8f);
        Kit.part(b, "hips", null, Role.BODY, skin).accent(knob).at(0, 11, 3).mat("flesh").box(-3.5f, -2, -2.5f, 7, 4, 5).add();
        st(b, "torso", "hips", skin).accent(knob).at(0, -1.5f, 0).rot(40, 0, 0).mat("flesh").box(-4.5f, -10, -3, 9, 10, 6).add();
        st(b, "ribs", "torso", dark).at(0, -7, -3).mat("flesh").box(-3.5f, 0, -0.6f, 7, 1, 1).box(-3.5f, 2, -0.6f, 7, 1, 1)
                .box(-3, 4, -0.6f, 6, 1, 1).add();
        for (int i = 0; i < 5; i++) {
            st(b, "knob" + i, "torso", knob).at(i % 2 == 0 ? 0 : (i % 4 == 1 ? 2.2f : -2.2f), -9 + i * 2, 3).rot(30, 0, 0).mat("flesh")
                    .box(-1, -1, -0.5f, 2, 2, 2, 0.1f).add();
        }
        sy(b, "shoulder", "torso", skin).accent(knob).at(5, -9, 0).mat("flesh").box(-1.5f, -2, -2.5f, 3, 4, 5).add();
        sy2(b, "shoulder_knob", "shoulder", knob).at(0.5f, -2, 0).mat("flesh").box(-1, -1.5f, -1, 2, 2, 2).add();
        Kit.part(b, "head", "torso", Role.HEAD, skin).accent(0xE02020).at(0, -10.5f, -1.5f).rot(-36, 0, 0).mat("flesh").pattern("eyes")
                .glow("eyes").box(-3, -5, -3.5f, 6, 5, 6).add();
        st(b, "crown", "head", knob).at(0, -5, 0).mat("flesh").box(-2, -1.5f, -2.5f, 4, 2, 5).add();
        st(b, "beak", "head", dark).at(0, -1.5f, -3.5f).rot(18, 0, 0).mat("horn").box(-1.5f, -1, -3.5f, 3, 2, 4).add();
        Kit.part(b, "jaw", "head", Role.JAW, dark).at(0, 0, -3).rot(14, 0, 0).mat("flesh").box(-2, 0, -3, 4, 1, 4).add();
        teeth(b, "teeth", "jaw", bone, 0, -2.6f, 1.8f, 2, false, false);
        sy(b, "tendril", "head", knob).at(2, 0, -2).rot(30, 0, -10).mat("flesh").box(-0.5f, 0, -0.5f, 1, 4, 1).add();
        sy(b, "horn", "head", bone).at(2.5f, -4.5f, 1).rot(-50, 0, -20).mat("horn").box(-0.5f, -4, -0.5f, 1, 4, 1, 0.15f).add();
        // long dangling arms
        Kit.sym(b, "arm", "torso", false, skin).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(knob).at(5.5f, -8.5f, 0).rot(-40, 0, -8)
                .mat("flesh").box(-1.5f, -1, -1.5f, 3, 9, 3).add();
        sy2(b, "elbow_knob", "arm", knob).at(0, 8, 1.5f).mat("flesh").box(-1, -1, -0.5f, 2, 2, 2).add();
        sy2(b, "forearm", "arm", skin).at(0, 8.5f, 0).rot(-30, 0, 4).mat("flesh").box(-1.5f, 0, -1.5f, 3, 9, 3, -0.1f).add();
        sy2(b, "hand", "forearm", dark).at(0, 9, 0).mat("flesh").box(-2, 0, -2, 4, 2, 4).add();
        Kit.sym(b, "talons", "hand", true, bone).at(0, 2, -1).rot(30, 0, 0).mat("horn")
                .box(-2, 0, -1, 1, 4, 1, -0.1f).box(-0.5f, 0, -1.2f, 1, 4, 1, -0.1f).box(1, 0, -1, 1, 4, 1, -0.1f).add();
        // legs
        Kit.sym(b, "thigh", null, false, skin).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(knob).at(2.5f, 11, 3)
                .rot(-25, 0, -5).mat("flesh").box(-1.5f, -1, -1.5f, 3, 7, 3).add();
        sy2(b, "shin", "thigh", skin).at(0, 5.5f, 0).rot(52, 0, 3).mat("flesh").box(-1.5f, 0, -1.5f, 3, 6, 3, -0.1f).add();
        sy2(b, "ankle", "shin", dark).at(0, 5, 0).rot(-27, 0, 0).mat("flesh").box(-1, 0, -1, 2, 3, 2).add();
        sy2(b, "foot", "ankle", dark).at(0, 2.5f, 0).mat("flesh").box(-1.5f, 0, -3, 3, 1, 4).add();
        claws(b, "toe_claws", "foot", true, bone, 3, 2, 0.5f, -2.8f, 2);
        String t = chain(b, "tail", "hips", Role.TAIL, skin, knob, "flesh", null, 0, 0, 2, 1, new int[]{3, 2, 2, 2, 1, 1},
                new int[]{3, 2, 2, 2, 1, 1}, new int[]{4, 4, 4, 4, 4, 3}, new float[]{30, 10, 6, 0, -8, -10}, new float[]{0, 8, 8, -6, -10, -10});
        for (int i = 1; i < 5; i += 2) {
            st(b, "tail_knob" + i, "tail" + i, knob).at(0, -1, 2).mat("flesh").box(-0.5f, -1, -0.5f, 1, 1, 1, 0.2f).add();
        }
        st(b, "tail_barb", t, bone).at(0, 0, 3).mat("horn").box(-0.5f, -0.5f, 0, 1, 1, 3).box(-1.5f, -0.5f, 0.5f, 3, 1, 1).add();
        return b.build();
    }

    /** A big hound: barrel chest, wolfish skull with a toothed jaw, ridge of bristles, sturdy legs and a tail. */
    static ModelSpec canine(String id, int fur, int dark, int eye, boolean hell) {
        int belly = mix(fur, 0xD8C8A8, hell ? 0.1f : 0.35f);
        String mat = hell ? "fur" : "fur";
        String veins = null;
        int acc = hell ? 0xFF7A1E : belly;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.7f);
        Kit.part(b, "body", null, Role.BODY, fur).accent(acc).at(0, 12, 1).mat(mat).glow(veins).box(-4, -4, -8, 8, 8, 16).add();
        st(b, "chest", "body", fur).accent(acc).at(0, 0, -8).rot(-8, 0, 0).mat(mat).glow(veins).box(-4.5f, -4.5f, -3, 9, 10, 5).add();
        st(b, "ruff", "chest", dark).at(0, -4.5f, -1).mat("fur").box(-5, -1, -2, 10, 3, 5).add();
        st(b, "belly", "body", belly).at(0, 4, -1).mat("fur").box(-3, -0.6f, -6, 6, 1, 12).add();
        for (int i = 0; i < 5; i++) {
            st(b, "bristle" + i, "body", dark).at(0, -4, -7 + i * 3.4f).rot(-40, 0, 0).mat(hell ? "horn" : "fur")
                    .box(-1, -1, -1, 2, hell ? 3 : 2, 2, -0.1f).add();
        }
        sy(b, "haunch", "body", fur).accent(acc).at(3.8f, 0, 5).mat(mat).box(-1, -3.5f, -3.5f, 2, 7, 7).add();
        // head
        st(b, "neck", "body", fur).accent(acc).at(0, -2, -10).rot(-35, 0, 0).mat(mat).box(-3, -3, -4, 6, 6, 5).add();
        Kit.part(b, "head", "neck", Role.HEAD, fur).accent(eye).at(0, -1, -4).rot(35, 0, 0).mat(mat).pattern("eyes").glow("eyes")
                .box(-3.5f, -3.5f, -4.5f, 7, 6, 6).add();
        st(b, "snout", "head", fur).at(0, 0, -4.5f).mat(mat).box(-2, -1.5f, -4.5f, 4, 3, 5).add();
        st(b, "nose", "snout", 0x181414).at(0, -1.5f, -4.5f).mat("skin").box(-1, 0, -0.6f, 2, 1, 1).add();
        teeth(b, "teeth_up", "snout", TOOTH, 1.5f, -4.2f, 2, 2, true, false);
        Kit.part(b, "jaw", "head", Role.JAW, dark).at(0, 1.5f, -4).rot(14, 0, 0).mat(mat).glow(hell ? "maw" : null)
                .box(-1.8f, 0, -5, 4, 1, 5, -0.1f).add();
        sy(b, "fang", "jaw", TOOTH).at(1.2f, 0, -4.4f).mat("horn").box(-0.5f, -1.5f, -0.5f, 1, 2, 1, -0.15f).add();
        sy(b, "ear", "head", dark).at(2.4f, -3.5f, -1).rot(-12, 0, 14).mat("fur").box(-1, -3, -1, 2, 3, 2).add();
        sy(b, "brow", "head", dark).at(2, -3.4f, -4.5f).rot(12, 0, -12).mat("fur").box(-1.2f, -0.6f, -0.6f, 2, 1, 2).add();
        if (hell) {
            sy(b, "horn", "head", 0x2A1A16).at(2.2f, -3.5f, 0).rot(-45, 0, -15).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1, 0.15f).add();
        }
        // legs
        float[][] lp = {{3, 13, -7}, {3, 12.5f, 6}};
        Role[][] roles = {{Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT}, {Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT}};
        for (int i = 0; i < 2; i++) {
            String ln = i == 0 ? "leg_f" : "leg_b";
            Kit.sym(b, ln, null, false, fur).roles(roles[i][0], roles[i][1]).accent(acc).at(lp[i][0], lp[i][1], lp[i][2])
                    .rot(i == 0 ? 6 : -20, 0, -3).mat(mat).glow(veins).box(-1.5f, -1, -2, 3, 6, 4).add();
            sy2(b, ln + "_shin", ln, dark).at(0, 5, i == 0 ? 0 : -0.5f).rot(i == 0 ? -8 : 34, 0, 3).mat(mat).box(-1, 0, -1, 2, 5, 2).add();
            sy2(b, ln + "_paw", ln + "_shin", dark).at(0, 4.6f, 0).rot(i == 0 ? 2 : -14, 0, 0).mat(mat).box(-1.5f, 0, -2.5f, 3, 1, 3).add();
            claws(b, ln + "_claws", ln + "_paw", true, hell ? 0x201814 : CLAW, 3, 2, 0.5f, -2.3f, 1);
        }
        String t = chain(b, "tail", "body", Role.TAIL, fur, acc, mat, null, 0, -3, 8, 1, new int[]{3, 2, 2, 1}, new int[]{3, 2, 2, 1},
                new int[]{3, 3, 3, 3}, new float[]{-40, 15, 15, 10}, null);
        st(b, "tail_tuft", t, dark).at(0, 0, 3).mat("fur").box(-1, -1, 0, 2, 2, 2).add();
        return b.build();
    }

    /** Gargoyle: hunched grey stone fiend with bat wings, ram horns, a fanged maw and huge clawed hands. */
    static ModelSpec gargoyle(String id) {
        int stone = 0x7E7C78;
        int dark = 0x54524E;
        int moss = 0x6A7A4A;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.9f);
        Kit.part(b, "hips", null, Role.BODY, stone).accent(moss).at(0, 13, 2).mat("hide").box(-4.5f, -2, -3, 9, 4, 6).add();
        st(b, "torso", "hips", stone).accent(moss).at(0, -1.5f, 0).rot(36, 0, 0).mat("hide").box(-5.5f, -10, -4, 11, 10, 8).add();
        st(b, "chest", "torso", dark).at(0, -9, -4).mat("hide").box(-4.5f, 0, -1, 9, 4, 1).box(-3, 4.5f, -0.8f, 6, 3, 1).add();
        for (int i = 0; i < 4; i++) {
            st(b, "spine" + i, "torso", dark).at(0, -9 + i * 2.6f, 4).rot(50, 0, 0).mat("hide").box(-1, -1, -0.5f, 2, 2, 2).add();
        }
        Kit.part(b, "head", "torso", Role.HEAD, stone).accent(0xFF5020).at(0, -10.5f, -2).rot(-36, 0, 0).mat("hide").pattern("eyes").glow("eyes")
                .box(-3.5f, -5, -4, 7, 6, 6).add();
        st(b, "brow", "head", dark).at(0, -3.8f, -4.2f).rot(16, 0, 0).mat("hide").box(-4, -1, -0.6f, 8, 2, 2).add();
        st(b, "snout", "head", stone).at(0, -1.5f, -4).mat("hide").box(-2.5f, 0, -2.5f, 5, 2, 3).add();
        Kit.part(b, "jaw", "head", Role.JAW, dark).at(0, 0.5f, -3).rot(14, 0, 0).mat("hide").box(-3, 0, -3.5f, 6, 2, 4).add();
        teeth(b, "teeth", "jaw", 0xC8C4B8, 0, -3.2f, 2.6f, 2, false, true);
        sy(b, "ear", "head", stone).at(3.5f, -3.5f, -0.5f).rot(-20, -20, -40).mat("hide").box(0, -1, -0.5f, 3, 2, 1).add();
        horn(b, "horn", "head", true, false, dark, 2.5f, -4.5f, 0, -50, 0, -30, new int[]{2, 2, 1}, new int[]{3, 3, 3}, -45, -10);
        Kit.sym(b, "arm", "torso", false, stone).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(moss).at(6.5f, -8.5f, 0).rot(-30, 0, -10)
                .mat("hide").box(-2, -2, -2, 4, 7, 4).add();
        sy2(b, "elbow", "arm", dark).at(0, 6, 2).mat("hide").box(-1, -1, -0.5f, 2, 2, 2).add();
        sy2(b, "forearm", "arm", stone).at(0, 6.5f, 0).rot(-35, 0, 4).mat("hide").box(-2, 0, -2, 4, 6, 4, 0.2f).add();
        sy2(b, "hand", "forearm", dark).at(0, 6, 0).rot(10, 0, 0).mat("hide").box(-2.5f, 0, -2.5f, 5, 2, 5).add();
        Kit.sym(b, "talons", "hand", true, 0x3A3836).at(0, 2, -1.5f).rot(25, 0, 0).mat("horn")
                .box(-2.5f, 0, -1, 1, 3, 1, -0.05f).box(-0.9f, 0, -1.3f, 1, 3, 1, -0.05f).box(0.7f, 0, -1.3f, 1, 3, 1, -0.05f)
                .box(1.7f, -1, 1, 1, 2, 1, -0.05f).add();
        Kit.sym(b, "thigh", null, false, stone).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(moss).at(3, 13, 2)
                .rot(-30, 0, -8).mat("hide").box(-2.5f, -1, -2.5f, 5, 6, 5).add();
        sy2(b, "shin", "thigh", dark).at(0, 4.5f, 0).rot(62, 0, 6).mat("hide").box(-2, 0, -2, 4, 5, 4).add();
        sy2(b, "ankle", "shin", dark).at(0, 4.5f, 0).rot(-32, 0, 0).mat("hide").box(-1.5f, 0, -1.5f, 3, 3, 3).add();
        sy2(b, "foot", "ankle", stone).at(0, 2.5f, 0).mat("hide").box(-2.5f, 0, -3.5f, 5, 1, 5).add();
        claws(b, "toe_claws", "foot", true, 0x3A3836, 3, 3, 0.5f, -3.2f, 2);
        Kit.sym(b, "wing", "torso", false, dark).roles(Role.WING_LEFT, Role.WING_RIGHT).accent(stone).at(3, -9, 3.5f).rot(-30, -35, -40)
                .mat("hide").box(0, -1, -1, 8, 2, 2).add();
        sy2(b, "wing_web0", "wing", stone).at(0, 0, 0.8f).mat("hide").box(0, -0.25f, 0, 8, 1, 6, -0.2f).add();
        sy2(b, "wing_fore", "wing", dark).at(8, 0, 0).rot(0, 30, -15).mat("hide").box(0, -0.75f, -0.75f, 8, 1, 1, 0.3f).add();
        for (int f = 0; f < 3; f++) {
            String fn = "wing_finger" + f;
            sy2(b, fn, "wing_fore", dark).at(7.5f, 0, 0).rot(0, -30 - f * 40, 15 + f * 8).mat("hide").box(0, -0.5f, -0.5f, 10 - f * 2, 1, 1).add();
            sy2(b, fn + "_web", fn, stone).at(0, 0, 0.3f).mat("hide").box(0, -0.25f, 0, 9 - f * 2, 1, 6, -0.2f).add();
        }
        String t = chain(b, "tail", "hips", Role.TAIL, stone, moss, "rock", null, 0, 0, 2.5f, 1, new int[]{3, 2, 2, 1}, new int[]{3, 2, 2, 1},
                new int[]{4, 4, 4, 3}, new float[]{50, 10, 0, -20}, new float[]{0, 10, 10, 10});
        st(b, "tail_tip", t, dark).at(0, 0, 3).mat("hide").box(-1.5f, -0.5f, 0, 3, 1, 2).add();
        return b.build();
    }

    /**
     * Nechryael: a hulking red-fleshed demon whose lower body tapers into a tendrilled stalk; huge arms, a wide
     * tusked maw, swept horns and small bat wings.
     */
    static ModelSpec nechryael(String id) {
        int skin = 0xA8403A;
        int dark = 0x6A201E;
        int pale = 0xD89080;
        int horn = 0xD8CCA8;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.8f);
        Kit.part(b, "body", null, Role.FLOAT, skin).accent(dark).at(0, 8, 0).mat("flesh").box(-6, -10, -4, 12, 11, 8).add();
        st(b, "belly", "body", pale).at(0, -4, -4).mat("flesh").box(-4, -3, -0.8f, 8, 7, 1).add();
        st(b, "chest", "body", skin).at(0, -10, -4).mat("flesh").box(-5.5f, 0, -1, 5, 4, 1).box(0.5f, 0, -1, 5, 4, 1).add();
        st(b, "hump", "body", dark).at(0, -10, 3).rot(-15, 0, 0).mat("flesh").box(-5, -1, -1, 10, 6, 3).add();
        // tapering stalk with dangling tendrils
        String s = chain(b, "stalk", "body", Role.TAIL, skin, dark, "flesh", null, 0, 1, 0, 1, new int[]{9, 7, 5, 3},
                new int[]{9, 7, 5, 3}, new int[]{1, 1, 1, 1}, new float[]{90, 0, 0, 0}, null);
        for (int k = 0; k < 6; k++) {
            float a = k * 60;
            float x = (float) Math.sin(Math.toRadians(a)) * 3.2f;
            float z = (float) Math.cos(Math.toRadians(a)) * 3.2f;
            Kit.part(b, "tendril" + k, "body", Role.TENTACLE, dark).at(x, 1, z).rot((float) Math.cos(Math.toRadians(a)) * 18, 0,
                    (float) -Math.sin(Math.toRadians(a)) * 18).mat("flesh").box(-0.5f, 0, -0.5f, 1, 6 + (k % 2) * 3, 1).add();
        }
        st(b, "stalk_tip", s, dark).at(0, 0, 1).mat("flesh").box(-1, -1, 0, 2, 2, 3).add();
        // head
        Kit.part(b, "head", "body", Role.HEAD, skin).accent(0xFFE040).at(0, -10, -1.5f).rot(-6, 0, 0).mat("flesh").pattern("eyes").glow("eyes")
                .box(-4, -6, -4.5f, 8, 6, 7).add();
        st(b, "brow", "head", dark).at(0, -4.2f, -4.7f).rot(16, 0, 0).mat("flesh").box(-4.2f, -1, -0.6f, 8, 2, 2, -0.1f).add();
        Kit.part(b, "jaw", "head", Role.JAW, dark).at(0, -0.5f, -3.5f).rot(10, 0, 0).mat("flesh").glow("maw").box(-3.5f, 0, -2, 7, 3, 4).add();
        teeth(b, "teeth", "jaw", TOOTH, 0, -1.8f, 3.2f, 2, false, true);
        sy(b, "tusk", "jaw", TOOTH).at(3, 0, -1.5f).rot(-20, 0, 15).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1, 0.1f).add();
        horn(b, "horn", "head", true, false, horn, 3, -5.5f, -0.5f, -40, 10, -35, new int[]{2, 1, 1}, new int[]{3, 3, 3}, -30, 10);
        sy(b, "ear_fin", "head", dark).at(4, -3, 0).rot(-10, -30, -10).mat("membrane").box(0, -2, 0, 1, 4, 4).add();
        // massive arms
        Kit.sym(b, "arm", "body", false, skin).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(dark).at(7.5f, -8.5f, 0).rot(-15, 0, -16)
                .mat("flesh").box(-2.5f, -2, -2.5f, 5, 8, 5).add();
        sy2(b, "shoulder", "arm", dark).at(0, -2, 0).mat("flesh").box(-3, -1.5f, -3, 6, 3, 6).add();
        sy2(b, "forearm", "arm", skin).at(0, 6, 0).rot(-25, 0, 6).mat("flesh").box(-2.5f, 0, -2.5f, 5, 7, 5, 0.3f).add();
        sy2(b, "hand", "forearm", dark).at(0, 7, 0).mat("flesh").box(-2.5f, 0, -2.5f, 5, 3, 5).add();
        Kit.sym(b, "talons", "hand", true, 0x2A1A18).at(0, 3, -1.5f).rot(25, 0, 0).mat("horn")
                .box(-2.5f, 0, -1, 1, 3, 1, -0.05f).box(-0.9f, 0, -1.3f, 1, 3, 1, -0.05f).box(0.7f, 0, -1.3f, 1, 3, 1, -0.05f).add();
        Kit.sym(b, "wing", "body", false, dark).roles(Role.WING_LEFT, Role.WING_RIGHT).at(3, -9, 3).rot(-25, -45, -35)
                .mat("flesh").box(0, -0.75f, -0.75f, 6, 1, 1, 0.3f).add();
        sy2(b, "wing_web", "wing", darker(skin, 0.6f)).at(0, 0, 0.5f).mat("membrane").box(0, -0.25f, 0, 7, 1, 6, -0.25f).add();
        sy2(b, "wing_finger", "wing", dark).at(6, 0, 0).rot(0, -55, 15).mat("horn").box(0, -0.5f, -0.5f, 6, 1, 1).add();
        return b.build();
    }

    /**
     * Bloodveld: a squat, fleshy two-legged horror that is mostly mouth - huge toothed maw, a long whipping
     * tongue, an exposed brain on top and stubby clawed arms.
     */
    static ModelSpec bloodveld(String id, int flesh, int brain, boolean mutated) {
        int dark = darker(flesh, 0.62f);
        int gum = mix(flesh, 0x6A1018, 0.5f);
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.7f);
        Kit.part(b, "body", null, Role.BODY, flesh).accent(dark).at(0, 12, 0).mat("flesh").box(-5, -9, -4, 10, 10, 8).add();
        st(b, "belly", "body", mix(flesh, 0xF0C0A0, 0.3f)).at(0, -3, -4).mat("flesh").box(-3.5f, -3, -0.8f, 7, 6, 1).add();
        st(b, "hump", "body", dark).at(0, -9, 2).rot(-20, 0, 0).mat("flesh").box(-4, -1, -1, 8, 5, 4).add();
        // the head is a great hinged maw
        Kit.part(b, "head", "body", Role.HEAD, flesh).accent(mutated ? 0x60FF60 : 0xFFE040).at(0, -9, -1).rot(-10, 0, 0).mat("flesh")
                .pattern("eyes").glow("eyes").box(-5, -6, -6, 10, 6, 8).add();
        st(b, "brain", "head", brain).accent(darker(brain, 0.7f)).at(0, -6, 0).mat("flesh").box(-3.5f, -2.5f, -3.5f, 7, 3, 6).add();
        st(b, "brain_fold", "brain", darker(brain, 0.8f)).at(0, -2.5f, 0).mat("flesh").box(-0.5f, -0.6f, -3, 1, 1, 5)
                .box(-2.5f, -0.4f, -2, 1, 1, 3).box(1.5f, -0.4f, -2, 1, 1, 3).add();
        st(b, "upper_lip", "head", dark).at(0, -0.5f, -6).mat("flesh").box(-4.5f, -1, -0.5f, 9, 2, 1).add();
        teeth(b, "teeth_up", "head", TOOTH, 0.5f, -6.2f, 4.2f, 3, true, true);
        Kit.part(b, "jaw", "head", Role.JAW, flesh).at(0, 0, -1).rot(22, 0, 0).mat("flesh").box(-4.5f, 0, -5.5f, 9, 3, 7).add();
        st(b, "gums", "jaw", gum).at(0, 0, -2).mat("flesh").box(-3.5f, -0.3f, -3, 7, 1, 5).add();
        teeth(b, "teeth_lo", "jaw", TOOTH, 0, -5.2f, 4, 3, false, true);
        String tg = chain(b, "tongue", "jaw", Role.TENTACLE, 0xC0505A, gum, "flesh", null, 0, 0.5f, -4, -1, new int[]{3, 2, 2, 1},
                new int[]{1, 1, 1, 1}, new int[]{3, 3, 3, 3}, new float[]{40, 25, 20, -10}, new float[]{0, 15, -10, -15});
        st(b, "tongue_tip", tg, 0xA03A44).at(0, 0, -3).mat("flesh").box(-1, -0.5f, -2, 2, 1, 2, -0.1f).add();
        if (mutated) {
            for (int k = 0; k < 4; k++) {
                st(b, "growth" + k, "body", brain).at(k % 2 == 0 ? 3.5f : -3.5f, -7 + k * 1.8f, 3.5f).rot(30, 0, k % 2 == 0 ? -20 : 20)
                        .mat("flesh").box(-1, -2, -1, 2, 2, 2, 0.2f).add();
            }
        }
        sy(b, "arm", "body", flesh).accent(dark).at(5, -6, -1).rot(-30, 0, -20).mat("flesh").box(-1.5f, -1, -1.5f, 3, 5, 3).add();
        sy2(b, "forearm", "arm", dark).at(0, 4.5f, 0).rot(-40, 0, 0).mat("flesh").box(-1.5f, 0, -1.5f, 3, 4, 3, -0.1f).add();
        Kit.sym(b, "claw", "forearm", true, CLAW).at(0, 4, -0.5f).rot(20, 0, 0).mat("horn")
                .box(-1.5f, 0, -1, 1, 2, 1, -0.1f).box(0.5f, 0, -1, 1, 2, 1, -0.1f).add();
        Kit.sym(b, "leg", null, false, flesh).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(dark).at(3, 12, 1).rot(-20, 0, -8)
                .mat("flesh").box(-2, -1, -2, 4, 6, 4).add();
        sy2(b, "shin", "leg", dark).at(0, 5, 0).rot(40, 0, 6).mat("flesh").box(-1.5f, 0, -1.5f, 3, 5, 3).add();
        sy2(b, "foot", "shin", dark).at(0, 4.5f, 0).rot(-20, 0, 0).mat("flesh").box(-2, 0, -3, 4, 1, 4).add();
        claws(b, "toe_claws", "foot", true, CLAW, 3, 2.5f, 0.5f, -2.8f, 2);
        return b.build();
    }
}
