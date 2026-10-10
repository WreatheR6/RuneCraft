package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Builder;
import com.runecraft.osrs.model.ModelSpec.Role;

/**
 * Reusable body-plan generators for slayer creature models. Units are model pixels, Y=24 is the
 * ground and the creature faces -Z (like vanilla entity models).
 */
final class ModelKit {
    private ModelKit() {
    }

    /** Gives TENTACLE / FIN / TAIL parts distinct indices so they don't all sway in lockstep. */
    static ModelSpec finish(Builder b) {
        ModelSpec spec = b.build();
        int i = 0;
        for (ModelSpec.Part p : spec.parts()) {
            if (p.role == Role.TENTACLE || p.role == Role.FIN) {
                p.index = i++;
            }
        }
        return spec;
    }

    // ------------------------------------------------------------------ dragons

    /**
     * A winged quadruped dragon about 2 blocks long.
     *
     * @param horns 0 = short horns, 1 = long swept horns (brutal), 2 = crown of spikes (metal)
     */
    static ModelSpec dragon(String id, int body, int belly, int wing, int horn, int horns, String pattern) {
        Builder b = ModelSpec.builder(id).shadow(1.1f);
        b.part("body", null, Role.BODY, body).pivot(0, 12, 0).box(-6, -5, -10, 12, 10, 20).pattern(pattern);
        b.part("belly", "body", Role.STATIC, belly).pivot(0, 0, 0).box(-5, 4, -9, 10, 2, 18);
        b.part("spines", "body", Role.STATIC, horn).pivot(0, 0, 0)
                .box(-0.5f, -7, -8, 1, 2, 3).box(-0.5f, -7, -2, 1, 2, 3).box(-0.5f, -7, 4, 1, 2, 3);
        b.part("neck", "body", Role.NECK, body).pivot(0, -2, -9).rotate(-35, 0, 0).box(-3, -3, -9, 6, 6, 10).pattern(pattern);
        b.part("head", "neck", Role.HEAD, body).pivot(0, -1, -9).rotate(35, 0, 0).box(-4, -4, -6, 8, 6, 7).pattern("eyes");
        b.part("snout", "head", Role.STATIC, body).pivot(0, 0, -6).box(-2.5f, -2, -6, 5, 3, 6);
        b.part("jaw", "head", Role.JAW, belly).pivot(0, 2, -5).box(-2.5f, 0, -7, 5, 2, 7);
        if (horns == 1) {
            b.part("horns", "head", Role.STATIC, horn).pivot(0, -3, 0).rotate(25, 0, 0)
                    .box(-4, -1, 0, 2, 2, 8).box(2, -1, 0, 2, 2, 8).box(-0.5f, -2, -4, 1, 2, 3);
        } else if (horns == 2) {
            b.part("horns", "head", Role.STATIC, horn).pivot(0, -4, 0)
                    .box(-4, -3, -1, 1, 3, 1).box(-2, -4, 0, 1, 4, 1).box(1, -4, 0, 1, 4, 1).box(3, -3, -1, 1, 3, 1);
        } else {
            b.part("horns", "head", Role.STATIC, horn).pivot(0, -4, 0).rotate(30, 0, 0)
                    .box(-3.5f, -4, -1, 1, 4, 1).box(2.5f, -4, -1, 1, 4, 1);
        }
        b.part("leg_fl", null, Role.LEG_FRONT_LEFT, body).pivot(4, 15, -7).box(-2, 0, -2, 4, 9, 4);
        b.part("leg_fr", null, Role.LEG_FRONT_RIGHT, body).pivot(-4, 15, -7).box(-2, 0, -2, 4, 9, 4);
        b.part("leg_bl", null, Role.LEG_BACK_LEFT, body).pivot(4, 14, 7).box(-2.5f, 0, -2.5f, 5, 10, 5);
        b.part("leg_br", null, Role.LEG_BACK_RIGHT, body).pivot(-4, 14, 7).box(-2.5f, 0, -2.5f, 5, 10, 5);
        b.part("tail", "body", Role.TAIL, body).pivot(0, -1, 10).rotate(-10, 0, 0).box(-2, -2, 0, 4, 4, 10).pattern(pattern);
        b.part("tail_tip", "tail", Role.TAIL, body).pivot(0, 0, 10).rotate(10, 0, 0).box(-1, -1, 0, 2, 2, 9);
        b.part("tail_spike", "tail_tip", Role.STATIC, horn).pivot(0, 0, 9).box(-2, -0.5f, 0, 4, 1, 3);
        b.part("wing_l", "body", Role.WING_LEFT, wing).pivot(6, -4, -5)
                .box(0, -1.5f, -1, 18, 2, 2).box(0, -0.5f, 1, 17, 1, 13);
        b.part("wing_r", "body", Role.WING_RIGHT, wing).pivot(-6, -4, -5)
                .box(-18, -1.5f, -1, 18, 2, 2).box(-17, -0.5f, 1, 17, 1, 13);
        return finish(b);
    }

    /** A wyvern: two legs, wings as forelimbs, long neck and tail. */
    static ModelSpec wyvern(String id, int body, int accent, int wing, String pattern) {
        Builder b = ModelSpec.builder(id).shadow(1.0f);
        b.part("body", null, Role.BODY, body).pivot(0, 9, 0).rotate(-20, 0, 0).box(-5, -5, -8, 10, 10, 16).pattern(pattern);
        b.part("ribs", "body", Role.STATIC, accent).pivot(0, 0, 0).box(-5.5f, -2, -6, 11, 6, 1).box(-5.5f, -2, -2, 11, 6, 1)
                .box(-5.5f, -2, 2, 11, 6, 1);
        b.part("neck", "body", Role.NECK, body).pivot(0, -3, -7).rotate(-30, 0, 0).box(-2, -2, -10, 4, 4, 11).pattern(pattern);
        b.part("head", "neck", Role.HEAD, body).pivot(0, 0, -10).rotate(50, 0, 0).box(-3, -3, -7, 6, 5, 7).pattern("eyes");
        b.part("jaw", "head", Role.JAW, accent).pivot(0, 2, -2).box(-2.5f, 0, -6, 5, 2, 6);
        b.part("crest", "head", Role.STATIC, accent).pivot(0, -3, 0).box(-0.5f, -4, -2, 1, 4, 5);
        b.part("leg_l", null, Role.LEG_FRONT_LEFT, body).pivot(4, 12, 3).box(-2, 0, -2, 4, 12, 4);
        b.part("leg_r", null, Role.LEG_FRONT_RIGHT, body).pivot(-4, 12, 3).box(-2, 0, -2, 4, 12, 4);
        b.part("tail", "body", Role.TAIL, body).pivot(0, 0, 8).rotate(25, 0, 0).box(-2, -2, 0, 4, 4, 12).pattern(pattern);
        b.part("tail_tip", "tail", Role.TAIL, accent).pivot(0, 0, 12).box(-1, -1, 0, 2, 2, 10);
        b.part("wing_l", "body", Role.WING_LEFT, wing).pivot(5, -4, -4).box(0, -1, -1, 20, 2, 2).box(0, 0, 1, 19, 1, 14);
        b.part("wing_r", "body", Role.WING_RIGHT, wing).pivot(-5, -4, -4).box(-20, -1, -1, 20, 2, 2).box(-19, 0, 1, 19, 1, 14);
        return finish(b);
    }

    // ------------------------------------------------------------------ quadrupeds

    /**
     * A four-legged beast.
     *
     * @param len   body length, {@code w}/{@code h} body width and height, {@code legH} leg height
     * @param extra 0 none, 1 ears, 2 horns, 3 spikes along the back, 4 frill (basilisk)
     */
    static ModelSpec quadruped(String id, int body, int accent, int w, int h, int len, int legH, int head, int tailLen,
                               int extra, String pattern) {
        Builder b = ModelSpec.builder(id).shadow(Math.max(0.4f, w / 14f));
        float bodyY = 24 - legH - h / 2f;
        b.part("body", null, Role.BODY, body).pivot(0, bodyY, 0).box(-w / 2f, -h / 2f, -len / 2f, w, h, len).pattern(pattern);
        b.part("head", "body", Role.HEAD, body).pivot(0, -h / 4f, -len / 2f)
                .box(-head / 2f, -head / 2f, -head, head, head, head).pattern("eyes");
        b.part("snout", "head", Role.STATIC, accent).pivot(0, head / 6f, -head)
                .box(-head / 4f, -head / 6f, -head / 2f, Math.max(2, head / 2), Math.max(2, head / 3), Math.max(2, head / 2));
        b.part("jaw", "head", Role.JAW, accent).pivot(0, head / 2f - 1, -head / 2f)
                .box(-head / 2f + 1, 0, -head / 2f - 1, Math.max(2, head - 2), 1, Math.max(2, head / 2 + 1));
        switch (extra) {
            case 1 -> b.part("ears", "head", Role.STATIC, accent).pivot(0, -head / 2f, -head / 2f)
                    .box(-head / 2f, -2, 0, 2, 2, 1).box(head / 2f - 2, -2, 0, 2, 2, 1);
            case 2 -> b.part("horns", "head", Role.STATIC, accent).pivot(0, -head / 2f, -head / 2f).rotate(-20, 0, 0)
                    .box(-head / 2f, -4, 0, 1, 4, 1).box(head / 2f - 1, -4, 0, 1, 4, 1);
            case 3 -> b.part("spikes", "body", Role.STATIC, accent).pivot(0, -h / 2f, 0)
                    .box(-0.5f, -3, -len / 3f, 1, 3, 2).box(-0.5f, -3, 0, 1, 3, 2).box(-0.5f, -3, len / 3f - 2, 1, 3, 2);
            case 4 -> b.part("frill", "head", Role.STATIC, accent).pivot(0, -head / 2f, -1)
                    .box(-head / 2f - 2, -3, 0, head + 4, 4, 1);
            default -> {
            }
        }
        float lx = w / 2f - 1.5f;
        float lz = len / 2f - 2;
        b.part("leg_fl", null, Role.LEG_FRONT_LEFT, body).pivot(lx, 24 - legH, -lz).box(-1.5f, 0, -1.5f, 3, legH, 3);
        b.part("leg_fr", null, Role.LEG_FRONT_RIGHT, body).pivot(-lx, 24 - legH, -lz).box(-1.5f, 0, -1.5f, 3, legH, 3);
        b.part("leg_bl", null, Role.LEG_BACK_LEFT, body).pivot(lx, 24 - legH, lz).box(-1.5f, 0, -1.5f, 3, legH, 3);
        b.part("leg_br", null, Role.LEG_BACK_RIGHT, body).pivot(-lx, 24 - legH, lz).box(-1.5f, 0, -1.5f, 3, legH, 3);
        if (tailLen > 0) {
            b.part("tail", "body", Role.TAIL, body).pivot(0, -h / 4f, len / 2f).rotate(-20, 0, 0)
                    .box(-1, -1, 0, 2, 2, tailLen);
        }
        return finish(b);
    }

    /** A low sprawling lizard: long tail, legs splayed out sideways. */
    static ModelSpec lizard(String id, int body, int accent, int scaleLen, boolean frill, boolean spikes) {
        Builder b = ModelSpec.builder(id).shadow(0.7f);
        b.part("body", null, Role.BODY, body).pivot(0, 19, 0).box(-4, -3, -8, 8, 6, 16).pattern("scales");
        b.part("head", "body", Role.HEAD, body).pivot(0, -1, -8).box(-3, -2.5f, -6, 6, 5, 6).pattern("eyes");
        b.part("jaw", "head", Role.JAW, accent).pivot(0, 2, -1).box(-2.5f, 0, -5, 5, 1, 5);
        if (frill) {
            b.part("frill", "head", Role.STATIC, accent).pivot(0, -1, -1).rotate(-20, 0, 0).box(-6, -4, 0, 12, 5, 1);
        }
        if (spikes) {
            b.part("spikes", "body", Role.STATIC, accent).pivot(0, -3, 0)
                    .box(-0.5f, -2, -6, 1, 2, 2).box(-0.5f, -2, -1, 1, 2, 2).box(-0.5f, -2, 4, 1, 2, 2);
        }
        b.part("leg_fl", null, Role.LEG_FRONT_LEFT, body).pivot(4, 19, -5).rotate(0, 0, -35).box(-1, 0, -1.5f, 3, 6, 3);
        b.part("leg_fr", null, Role.LEG_FRONT_RIGHT, body).pivot(-4, 19, -5).rotate(0, 0, 35).box(-2, 0, -1.5f, 3, 6, 3);
        b.part("leg_bl", null, Role.LEG_BACK_LEFT, body).pivot(4, 19, 5).rotate(0, 0, -35).box(-1, 0, -1.5f, 3, 6, 3);
        b.part("leg_br", null, Role.LEG_BACK_RIGHT, body).pivot(-4, 19, 5).rotate(0, 0, 35).box(-2, 0, -1.5f, 3, 6, 3);
        b.part("tail", "body", Role.TAIL, body).pivot(0, 0, 8).box(-2, -1.5f, 0, 4, 3, scaleLen).pattern("scales");
        b.part("tail_tip", "tail", Role.TAIL, accent).pivot(0, 0, scaleLen).box(-1, -1, 0, 2, 2, scaleLen);
        return finish(b);
    }

    // ------------------------------------------------------------------ insects & spiders

    /**
     * Arthropod body plan: head with mandibles, thorax, abdomen and {@code legsPerSide} legs a side.
     */
    static Builder insect(String id, int body, int accent, int size, int legsPerSide, int legLen, String pattern) {
        Builder b = ModelSpec.builder(id).shadow(size / 10f);
        float y = 24 - legLen * 0.64f;
        b.part("thorax", null, Role.BODY, body).pivot(0, y, 0).box(-size / 3f, -size / 4f, -size / 3f,
                Math.max(2, size * 2 / 3), Math.max(2, size / 2), Math.max(2, size * 2 / 3)).pattern(pattern);
        b.part("abdomen", "thorax", Role.STATIC, body).pivot(0, -1, size / 3f).rotate(-15, 0, 0)
                .box(-size / 2f, -size / 3f, 0, size, Math.max(2, size * 2 / 3), size).pattern(pattern);
        int hs = Math.max(3, size / 2);
        b.part("head", "thorax", Role.HEAD, accent).pivot(0, 0, -size / 3f).box(-hs / 2f, -hs / 2f, -hs, hs, hs, hs).pattern("eyes");
        b.part("mandibles", "head", Role.JAW, accent).pivot(0, hs / 2f - 1, -hs)
                .box(-hs / 2f, 0, -2, 1, 1, 3).box(hs / 2f - 1, 0, -2, 1, 1, 3);
        float spacing = size * 0.6f / Math.max(1, legsPerSide - 1);
        for (int i = 0; i < legsPerSide; i++) {
            float z = -size * 0.3f + i * spacing;
            float yaw = -30 + i * 60f / Math.max(1, legsPerSide - 1);
            b.part("leg_l" + i, "thorax", Role.LEG_EXTRA, accent).pivot(size / 3f, 0, z).rotate(0, yaw, 40)
                    .box(0, -0.5f, -0.5f, legLen, 1, 1);
            b.part("leg_r" + i, "thorax", Role.LEG_EXTRA, accent).pivot(-size / 3f, 0, z).rotate(0, -yaw, -40)
                    .box(-legLen, -0.5f, -0.5f, legLen, 1, 1);
        }
        return b;
    }

    // ------------------------------------------------------------------ bipeds

    /**
     * A humanoid body plan (head, torso, arms, legs) with optional tail and wings; returns the
     * builder so callers can add horns, weapons and so on.
     *
     * @param hunch forward lean of the torso in degrees
     */
    static Builder biped(String id, int skin, int body, int legs, int height, int width, float hunch, int armLen,
                         boolean tail, int wingColor) {
        Builder b = ModelSpec.builder(id).shadow(width / 16f);
        int legH = height * 4 / 10;
        int torsoH = height * 4 / 10;
        int headS = Math.max(5, height / 5);
        float hipY = 24 - legH;
        b.part("torso", null, Role.BODY, body).pivot(0, hipY, 0).rotate(hunch, 0, 0)
                .box(-width / 2f, -torsoH, -width / 4f, width, torsoH, Math.max(3, width / 2));
        b.part("head", "torso", Role.HEAD, skin).pivot(0, -torsoH, -1).rotate(-hunch, 0, 0)
                .box(-headS / 2f, -headS, -headS / 2f - 1, headS, headS, headS).pattern("eyes");
        b.part("jaw", "head", Role.JAW, skin).pivot(0, -1, -headS / 2f).box(-headS / 2f + 1, 0, -1.5f, headS - 2, 1, 2);
        b.part("arm_l", "torso", Role.ARM_LEFT, skin).pivot(width / 2f + 1.5f, -torsoH + 1.5f, 0).rotate(-hunch, 0, 0)
                .box(-1.5f, -1.5f, -1.5f, 3, armLen, 3);
        b.part("arm_r", "torso", Role.ARM_RIGHT, skin).pivot(-width / 2f - 1.5f, -torsoH + 1.5f, 0).rotate(-hunch, 0, 0)
                .box(-1.5f, -1.5f, -1.5f, 3, armLen, 3);
        b.part("leg_l", null, Role.LEG_FRONT_LEFT, legs).pivot(width / 4f, hipY, 0).box(-1.75f, 0, -1.75f, 3, legH, 3);
        b.part("leg_r", null, Role.LEG_FRONT_RIGHT, legs).pivot(-width / 4f, hipY, 0).box(-1.25f, 0, -1.75f, 3, legH, 3);
        if (tail) {
            b.part("tail", "torso", Role.TAIL, body).pivot(0, -1, width / 4f).rotate(30, 0, 0).box(-1, -1, 0, 2, 2, height / 3);
        }
        if (wingColor >= 0) {
            b.part("wing_l", "torso", Role.WING_LEFT, wingColor).pivot(2, -torsoH + 2, width / 4f).rotate(0, -30, 0)
                    .box(0, -1, 0, height / 2, 1, 1).box(0, 0, 0, height / 2 - 1, height / 3, 1);
            b.part("wing_r", "torso", Role.WING_RIGHT, wingColor).pivot(-2, -torsoH + 2, width / 4f).rotate(0, 30, 0)
                    .box(-height / 2f, -1, 0, height / 2, 1, 1).box(-height / 2f + 1, 0, 0, height / 2 - 1, height / 3, 1);
        }
        return b;
    }

    // ------------------------------------------------------------------ floaters

    /** A floating ghostly figure: tapering robe instead of legs, arms and a head. */
    static Builder ghost(String id, int robe, int skin, int hair, boolean longHair) {
        Builder b = ModelSpec.builder(id).shadow(0.3f);
        b.part("float", null, Role.FLOAT, robe).pivot(0, 10, 0).box(-4, -6, -2.5f, 8, 8, 5);
        b.part("robe", "float", Role.STATIC, robe).pivot(0, 2, 0).box(-3.5f, 0, -2, 7, 5, 4).box(-2.5f, 5, -1.5f, 5, 4, 3);
        b.part("wisp", "robe", Role.TAIL, robe).pivot(0, 9, 0).box(-1, 0, -1, 2, 3, 2);
        b.part("head", "float", Role.HEAD, skin).pivot(0, -6, 0).box(-3, -6, -3, 6, 6, 6).pattern("eyes");
        b.part("jaw", "head", Role.JAW, skin).pivot(0, -1, -3).box(-2, 0, -0.5f, 4, 1, 1);
        if (longHair) {
            b.part("hair", "head", Role.STATIC, hair).pivot(0, -6, 0).box(-3.5f, -0.5f, -1, 7, 9, 5);
        }
        b.part("arm_l", "float", Role.ARM_LEFT, robe).pivot(5, -5, 0).rotate(-40, 0, 0).box(-1, -1, -1, 2, 8, 2);
        b.part("arm_r", "float", Role.ARM_RIGHT, robe).pivot(-5, -5, 0).rotate(-40, 0, 0).box(-1, -1, -1, 2, 8, 2);
        return b;
    }
}
