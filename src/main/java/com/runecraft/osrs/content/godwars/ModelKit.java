package com.runecraft.osrs.content.godwars;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

/**
 * Reusable creature body plans for this module's models (units: model pixels, Y=24 is the ground,
 * smaller Y is higher, -Z is the front). Each returns the builder so callers can add details.
 */
final class ModelKit {
    private ModelKit() {
    }

    /**
     * A biped (vanilla proportions, 32 px tall) with parts body, head, arm_left, arm_right,
     * leg_left, leg_right.
     */
    static ModelSpec.Builder humanoid(String id, int skin, int top, int legs, String headPattern) {
        return ModelSpec.builder(id).shadow(0.5f)
                .part("leg_left", null, Role.LEG_FRONT_LEFT, legs).pivot(2, 12, 0).box(-2, 0, -2, 4, 12, 4)
                .part("leg_right", null, Role.LEG_FRONT_RIGHT, legs).pivot(-2, 12, 0).box(-2, 0, -2, 4, 12, 4)
                .part("body", null, Role.BODY, top).pivot(0, 12, 0).box(-4, -12, -2, 8, 12, 4)
                .part("head", "body", Role.HEAD, skin).pivot(0, -12, 0).box(-4, -8, -4, 8, 8, 8).pattern(headPattern)
                .part("arm_left", "body", Role.ARM_LEFT, top).pivot(5, -10, 0).box(-1, -2, -2, 4, 12, 4)
                .part("arm_right", "body", Role.ARM_RIGHT, top).pivot(-5, -10, 0).box(-3, -2, -2, 4, 12, 4);
    }

    /** Feathered wings for a humanoid's back (parent part with a back at z=+2). */
    static ModelSpec.Builder featherWings(ModelSpec.Builder b, String parent, float y, int feathers, int tips, int span) {
        return b.part("wing_left", parent, Role.WING_LEFT, feathers).pivot(2, y, 2.5f).rotate(0, -20, 0)
                .box(0, -2, 0, span, 3, 2).box(1, 1, 0.5f, span - 1, 12, 1).pattern("stripes")
                .part("wing_left_tip", "wing_left", Role.STATIC, tips).pivot(span - 3, 0, 0.5f)
                .box(0, -6, 0, 6, 22, 1).pattern("stripes")
                .part("wing_right", parent, Role.WING_RIGHT, feathers).pivot(-2, y, 2.5f).rotate(0, 20, 0)
                .box(-span, -2, 0, span, 3, 2).box(-span, 1, 0.5f, span - 1, 12, 1).pattern("stripes")
                .part("wing_right_tip", "wing_right", Role.STATIC, tips).pivot(-span + 3, 0, 0.5f)
                .box(-6, -6, 0, 6, 22, 1).pattern("stripes");
    }

    /** Bat/demon wings: a bone with a membrane hanging from it. */
    static ModelSpec.Builder membraneWings(ModelSpec.Builder b, String parent, float y, float z, int bone, int membrane, int span) {
        return b.part("wing_left", parent, Role.WING_LEFT, bone).pivot(3, y, z).rotate(0, -15, -10)
                .box(0, -1, -1, span, 2, 2).box(span - 1, -4, -1, 2, 4, 2)
                .part("membrane_left", "wing_left", Role.STATIC, membrane).pivot(0, 1, 0)
                .box(0, 0, 0, span, 14, 1).pattern("stripes")
                .part("wing_right", parent, Role.WING_RIGHT, bone).pivot(-3, y, z).rotate(0, 15, 10)
                .box(-span, -1, -1, span, 2, 2).box(-span - 1, -4, -1, 2, 4, 2)
                .part("membrane_right", "wing_right", Role.STATIC, membrane).pivot(0, 1, 0)
                .box(-span, 0, 0, span, 14, 1).pattern("stripes");
    }

    /** An aviansie (bird-person): talons, feathered body, beaked head, wings and tail feathers. */
    static ModelSpec aviansie(String id, int feathers, int chest, int beak, int armour, float scale) {
        ModelSpec.Builder b = ModelSpec.builder(id).scale(scale).shadow(0.6f)
                .part("leg_left", null, Role.LEG_FRONT_LEFT, beak).pivot(2, 13, 0).box(-1, 0, -1, 2, 9, 2).box(-1.5f, 9, -3, 3, 2, 4)
                .part("leg_right", null, Role.LEG_FRONT_RIGHT, beak).pivot(-2, 13, 0).box(-1, 0, -1, 2, 9, 2).box(-1.5f, 9, -3, 3, 2, 4)
                .part("body", null, Role.BODY, feathers).pivot(0, 13, 0).box(-4, -11, -3, 8, 11, 6)
                .part("chest", "body", Role.STATIC, chest).pivot(0, 0, 0).box(-3, -10, -3.5f, 6, 7, 1).pattern("plates")
                .part("belt", "body", Role.STATIC, armour).pivot(0, 0, 0).box(-4.5f, -3, -3.5f, 9, 3, 7)
                .part("head", "body", Role.HEAD, feathers).pivot(0, -11, -1).box(-2.5f, -5, -3, 5, 5, 5).pattern("eyes")
                .box(-1, -3, -6, 2, 2, 3)
                .part("crest", "head", Role.STATIC, chest).pivot(0, -5, 0).box(-0.5f, -3, -1, 1, 3, 5).pattern("stripes")
                .part("arm_left", "body", Role.ARM_LEFT, feathers).pivot(4.5f, -9, 0).box(0, -1, -1, 2, 10, 2)
                .part("arm_right", "body", Role.ARM_RIGHT, feathers).pivot(-4.5f, -9, 0).box(-2, -1, -1, 2, 10, 2)
                .part("tail", "body", Role.TAIL, chest).pivot(0, -1, 3).rotate(-35, 0, 0).box(-3, 0, 0, 6, 1, 7).pattern("stripes");
        return featherWings(b, "body", -9, feathers, chest, 12).build();
    }

    /** A four-legged bear: humped body, broad head with snout and jaw, short tail. */
    static ModelSpec bear(String id, int fur, int muzzle, int claws, float scale) {
        return ModelSpec.builder(id).scale(scale).shadow(0.9f)
                .part("body", null, Role.BODY, fur).pivot(0, 11, 0).box(-7, -6, -12, 14, 12, 22).box(-6, -9, -9, 12, 3, 10)
                .part("head", "body", Role.HEAD, fur).pivot(0, -1, -12).box(-5, -5, -7, 10, 9, 7).pattern("eyes")
                .box(-5, -7, -4, 3, 3, 1).box(2, -7, -4, 3, 3, 1)
                .part("snout", "head", Role.STATIC, muzzle).pivot(0, 0, -7).box(-3, -1, -4, 6, 4, 4)
                .part("jaw", "head", Role.JAW, muzzle).pivot(0, 3, -6).box(-2.5f, 0, -4, 5, 2, 4)
                .part("leg_fl", "body", Role.LEG_FRONT_LEFT, fur).pivot(4.5f, 4, -8).box(-2.5f, 0, -2.5f, 5, 9, 5)
                .box(-2.5f, 8, -3.5f, 5, 1, 1)
                .part("leg_fr", "body", Role.LEG_FRONT_RIGHT, fur).pivot(-4.5f, 4, -8).box(-2.5f, 0, -2.5f, 5, 9, 5)
                .box(-2.5f, 8, -3.5f, 5, 1, 1)
                .part("leg_bl", "body", Role.LEG_BACK_LEFT, fur).pivot(4.5f, 4, 7).box(-2.5f, 0, -2.5f, 5, 9, 5)
                .part("leg_br", "body", Role.LEG_BACK_RIGHT, fur).pivot(-4.5f, 4, 7).box(-2.5f, 0, -2.5f, 5, 9, 5)
                .part("claws", "leg_fl", Role.STATIC, claws).pivot(0, 8, -3).box(-2, 0, -1.5f, 4, 1, 1)
                .part("tail", "body", Role.TAIL, fur).pivot(0, -4, 10).box(-1.5f, 0, 0, 3, 3, 3)
                .build();
    }

    /** A giant spider (vanilla spider layout, enlarged): head, thorax, abdomen, eight legs, fangs. */
    static ModelSpec spider(String id, int body, int abdomen, int legs, int fangs, float scale) {
        ModelSpec.Builder b = ModelSpec.builder(id).scale(scale).shadow(1.0f)
                .part("thorax", null, Role.BODY, body).pivot(0, 15, 0).box(-3.5f, -3, -3, 7, 6, 6)
                .part("abdomen", "thorax", Role.STATIC, abdomen).pivot(0, -1, 3).box(-6, -5, 0, 12, 9, 13).pattern("spots")
                .part("head", "thorax", Role.HEAD, body).pivot(0, 0, -3).box(-4, -4, -8, 8, 7, 8).pattern("eyes")
                .part("fangs", "head", Role.JAW, fangs).pivot(0, 2, -8).box(-3, 0, -1, 2, 3, 1).box(1, 0, -1, 2, 3, 1);
        float[] zs = {2, 1, 0, -1};
        float[] yaws = {45, 22.5f, -22.5f, -45};
        float[] rolls = {35, 28, 28, 35};
        for (int i = 0; i < 4; i++) {
            b.part("leg_r" + i, "thorax", Role.LEG_EXTRA, legs).pivot(-3, 0, zs[i]).rotate(0, yaws[i], -rolls[i])
                    .box(-18, -1, -1, 18, 2, 2);
            b.part("leg_l" + i, "thorax", Role.LEG_EXTRA, legs).pivot(3, 0, zs[i]).rotate(0, -yaws[i], rolls[i])
                    .box(0, -1, -1, 18, 2, 2);
        }
        return b.build();
    }

    /** A scorpion: segmented body, claws, six legs and a curled, segmented tail with a stinger. */
    static ModelSpec scorpion(String id, int shell, int claws, int stinger, float scale) {
        ModelSpec.Builder b = ModelSpec.builder(id).scale(scale).shadow(0.9f)
                .part("body", null, Role.BODY, shell).pivot(0, 17, 0).box(-5, -3, -7, 10, 5, 14).pattern("plates")
                .part("head", "body", Role.HEAD, shell).pivot(0, -0.5f, -7).box(-3.5f, -2, -4, 7, 4, 4).pattern("eyes")
                .part("mandibles", "head", Role.JAW, claws).pivot(0, 1, -4).box(-2, 0, -2, 4, 1, 2)
                .part("claw_left", "body", Role.ARM_LEFT, claws).pivot(4, 0, -6).rotate(0, -30, 0)
                .box(0, -1, -8, 2, 2, 8).box(-1, -2, -13, 4, 3, 5)
                .part("claw_right", "body", Role.ARM_RIGHT, claws).pivot(-4, 0, -6).rotate(0, 30, 0)
                .box(-2, -1, -8, 2, 2, 8).box(-3, -2, -13, 4, 3, 5)
                .part("tail1", "body", Role.TAIL, shell).pivot(0, -1, 7).rotate(55, 0, 0).box(-2, -2, 0, 4, 4, 6).pattern("plates")
                .part("tail2", "tail1", Role.TAIL, shell).pivot(0, 0, 6).rotate(35, 0, 0).box(-1.5f, -1.5f, 0, 3, 3, 6)
                .part("tail3", "tail2", Role.TAIL, shell).pivot(0, 0, 6).rotate(35, 0, 0).box(-1.5f, -1.5f, 0, 3, 3, 5)
                .part("stinger", "tail3", Role.STATIC, stinger).pivot(0, 0, 5).rotate(40, 0, 0).box(-1, -1, 0, 2, 2, 4).pattern("glow");
        float[] zs = {-3, 1, 5};
        for (int i = 0; i < 3; i++) {
            b.part("leg_r" + i, "body", Role.LEG_EXTRA, shell).pivot(-4.5f, 1, zs[i]).rotate(0, 15 - i * 15, -35)
                    .box(-10, -1, -1, 10, 2, 2);
            b.part("leg_l" + i, "body", Role.LEG_EXTRA, shell).pivot(4.5f, 1, zs[i]).rotate(0, -15 + i * 15, 35)
                    .box(0, -1, -1, 10, 2, 2);
        }
        return b.build();
    }

    /** A gaunt dog (hellhounds and skeleton hellhounds): ribbed body, long snout, thin legs. */
    static ModelSpec hound(String id, int body, int head, int eyes, float scale, boolean skeletal) {
        int leg = skeletal ? 2 : 3;
        return ModelSpec.builder(id).scale(scale).shadow(0.6f)
                .part("body", null, Role.BODY, body).pivot(0, 13, 0).box(-3, -3, -8, 6, 6, 15).pattern(skeletal ? "stripes" : "none")
                .part("head", "body", Role.HEAD, head).pivot(0, -2, -8).box(-3, -3, -5, 6, 6, 5).pattern("eyes")
                .box(-3, -5, -2, 2, 2, 1).box(1, -5, -2, 2, 2, 1)
                .part("snout", "head", Role.STATIC, head).pivot(0, 0, -5).box(-1.5f, 0, -4, 3, 3, 4)
                .part("jaw", "head", Role.JAW, eyes).pivot(0, 3, -5).box(-1.5f, 0, -4, 3, 1, 4)
                .part("leg_fl", "body", Role.LEG_FRONT_LEFT, body).pivot(2, 2, -6).box(-leg / 2f, 0, -leg / 2f, leg, 9, leg)
                .part("leg_fr", "body", Role.LEG_FRONT_RIGHT, body).pivot(-2, 2, -6).box(-leg / 2f, 0, -leg / 2f, leg, 9, leg)
                .part("leg_bl", "body", Role.LEG_BACK_LEFT, body).pivot(2, 2, 6).box(-leg / 2f, 0, -leg / 2f, leg, 9, leg)
                .part("leg_br", "body", Role.LEG_BACK_RIGHT, body).pivot(-2, 2, 6).box(-leg / 2f, 0, -leg / 2f, leg, 9, leg)
                .part("tail", "body", Role.TAIL, body).pivot(0, -2, 7).rotate(30, 0, 0).box(-0.5f, -0.5f, 0, 1, 1, 9)
                .build();
    }

    /** A floating hooded spirit (Nex's generals): bobbing robe, hood, arms and a spinning orb. */
    static ModelSpec hoodedSpirit(String id, int robe, int trim, int glow, float scale) {
        return ModelSpec.builder(id).scale(scale).shadow(0.4f)
                .part("robe", null, Role.FLOAT, robe).pivot(0, 9, 0).box(-4, -10, -3, 8, 15, 6).box(-3, 5, -2, 6, 5, 4)
                .part("trim", "robe", Role.STATIC, trim).pivot(0, 0, 0).box(-4.5f, -10, -3.5f, 9, 2, 7).box(-1, -8, -3.6f, 2, 13, 1)
                .part("hood", "robe", Role.HEAD, robe).pivot(0, -10, 0).box(-4, -8, -4, 8, 8, 8).pattern("eyes")
                .box(-3, -10, -2, 6, 2, 6)
                .part("arm_left", "robe", Role.ARM_LEFT, robe).pivot(5, -8, 0).box(-1, -2, -2, 3, 11, 3)
                .part("arm_right", "robe", Role.ARM_RIGHT, robe).pivot(-5, -8, 0).box(-2, -2, -2, 3, 11, 3)
                .part("orb", "robe", Role.SPIN, glow).pivot(0, -2, -8).box(-2, -2, -2, 4, 4, 4).pattern("glow")
                .part("shards", "orb", Role.SPIN, trim).pivot(0, 0, 0).box(-5, -0.5f, -0.5f, 1, 1, 1).box(4, -0.5f, -0.5f, 1, 1, 1)
                .box(-0.5f, -0.5f, -5, 1, 1, 1).box(-0.5f, -0.5f, 4, 1, 1, 1)
                .build();
    }

    /**
     * A revenant: a translucent-looking spectral form trailing a wisp instead of legs. Optional horns,
     * wings or a big single eye make the different revenants recognisable.
     */
    static ModelSpec revenant(String id, int spirit, int bones, boolean horns, boolean wings, boolean cyclops) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.3f)
                .part("body", null, Role.FLOAT, spirit).pivot(0, 10, 0).box(-4, -10, -2.5f, 8, 11, 5).pattern("glow")
                .part("ribs", "body", Role.STATIC, bones).pivot(0, 0, 0).box(-3, -9, -3, 6, 6, 1).pattern("stripes")
                .part("wisp", "body", Role.TAIL, spirit).pivot(0, 1, 0).box(-3, 0, -2, 6, 5, 4).box(-2, 5, -1, 4, 4, 3)
                .box(-1, 9, 0, 2, 3, 2)
                .part("head", "body", Role.HEAD, bones).pivot(0, -10, 0).box(-3.5f, -7, -3.5f, 7, 7, 7).pattern("eyes")
                .part("jaw", "head", Role.JAW, bones).pivot(0, -1, -2).box(-2.5f, 0, -1.5f, 5, 2, 4)
                .part("arm_left", "body", Role.ARM_LEFT, spirit).pivot(5, -8, 0).box(-1, -1, -1, 2, 11, 2)
                .part("arm_right", "body", Role.ARM_RIGHT, spirit).pivot(-5, -8, 0).box(-1, -1, -1, 2, 11, 2);
        if (horns) {
            b.part("horns", "head", Role.STATIC, bones).pivot(0, -7, 0).box(-4.5f, -4, -1, 2, 4, 2).box(2.5f, -4, -1, 2, 4, 2);
        }
        if (cyclops) {
            b.part("eye", "head", Role.STATIC, 0xF0F0A0).pivot(0, -4, -3.6f).box(-1.5f, -1.5f, -0.5f, 3, 3, 1).pattern("glow");
        }
        if (wings) {
            membraneWings(b, "body", -8, 2, bones, spirit, 12);
        }
        return b.build();
    }

    /** A goblin (Graardor's sergeants and GWD goblins): small, big-eared, with a weapon slot on the right arm. */
    static ModelSpec.Builder goblin(String id, int skin, int armour, int legs, float scale) {
        return ModelSpec.builder(id).scale(scale).shadow(0.4f)
                .part("leg_left", null, Role.LEG_FRONT_LEFT, legs).pivot(1.5f, 16, 0).box(-1.5f, 0, -1.5f, 3, 8, 3)
                .part("leg_right", null, Role.LEG_FRONT_RIGHT, legs).pivot(-1.5f, 16, 0).box(-1.5f, 0, -1.5f, 3, 8, 3)
                .part("body", null, Role.BODY, armour).pivot(0, 16, 0).box(-3.5f, -9, -2, 7, 9, 4).pattern("plates")
                .part("head", "body", Role.HEAD, skin).pivot(0, -9, 0).box(-3, -6, -3, 6, 6, 6).pattern("eyes")
                .box(-6, -5, 0, 3, 2, 1).box(3, -5, 0, 3, 2, 1).box(-1, -3, -4, 2, 2, 1)
                .part("arm_left", "body", Role.ARM_LEFT, skin).pivot(4.5f, -8, 0).box(-1, -1, -1.5f, 2, 8, 3)
                .part("arm_right", "body", Role.ARM_RIGHT, skin).pivot(-4.5f, -8, 0).box(-1, -1, -1.5f, 2, 8, 3);
    }
}
