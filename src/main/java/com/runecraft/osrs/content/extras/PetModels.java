package com.runecraft.osrs.content.extras;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.List;

/** Small models for pets whose source has no model of its own (skilling pets, Herbi, Chompy chick...). */
final class PetModels {
    private PetModels() {
    }

    static List<ModelSpec> all() {
        return List.of(
                bird("ext_pet_heron", 0xB0B8C0, 0xE0C040, 1.0f, true),
                bird("ext_pet_chompy", 0x8A9A5C, 0xE0A040, 0.8f, false),
                bird("ext_pet_phoenix", 0xE06020, 0xF0D040, 0.9f, false),
                golem(),
                quad("ext_pet_beaver", 0x7A5A3A, 0x3A2A1A, 6, 5, 9, 2, 5, true),
                quad("ext_pet_chinchompa", 0x9A9A9A, 0xE0E0E0, 5, 5, 6, 1, 5, true),
                quad("ext_pet_squirrel", 0xA0603A, 0xE0C0A0, 4, 5, 6, 2, 4, true),
                quad("ext_pet_rocky", 0x6A6A6A, 0x2A2A2A, 5, 5, 8, 3, 5, true),
                quad("ext_pet_herbi", 0x6A5A3A, 0x4A8A3A, 6, 6, 9, 3, 5, true),
                quad("ext_pet_bloodhound", 0x8A5A3A, 0x3A2A1A, 5, 6, 10, 5, 5, true),
                quad("ext_pet_nibbler", 0x8A3A1A, 0xE07020, 6, 5, 7, 2, 6, false),
                tangleroot(),
                riftGuardian(),
                tempor());
    }

    /** A four-legged animal: body w/h/length, leg height, head size, optional tail. */
    static ModelSpec quad(String id, int color, int accent, int w, int h, int len, int legH, int head, boolean tail) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.3f);
        float bodyY = 24 - legH - h / 2f;
        b.part("body", null, Role.BODY, color, accent).pivot(0, bodyY, 0).box(-w / 2f, -h / 2f, -len / 2f, w, h, len).pattern("spots");
        b.part("head", "body", Role.HEAD, color, accent).pivot(0, -h / 2f + 1, -len / 2f)
                .box(-head / 2f, -head + 1, -head + 1, head, head, head).pattern("eyes");
        if (legH > 0) {
            float lx = w / 2f - 1;
            float lz = len / 2f - 1.5f;
            float top = h / 2f;
            b.part("leg_fl", "body", Role.LEG_FRONT_LEFT, accent).pivot(lx, top, -lz).box(-1, 0, -1, 2, legH, 2);
            b.part("leg_fr", "body", Role.LEG_FRONT_RIGHT, accent).pivot(-lx, top, -lz).box(-1, 0, -1, 2, legH, 2);
            b.part("leg_bl", "body", Role.LEG_BACK_LEFT, accent).pivot(lx, top, lz).box(-1, 0, -1, 2, legH, 2);
            b.part("leg_br", "body", Role.LEG_BACK_RIGHT, accent).pivot(-lx, top, lz).box(-1, 0, -1, 2, legH, 2);
        }
        if (tail) {
            b.part("tail", "body", Role.TAIL, accent).pivot(0, -h / 2f + 1, len / 2f).rotate(-30, 0, 0).box(-1.5f, -1, 0, 3, 2, 5);
        }
        return b.build();
    }

    static ModelSpec bird(String id, int color, int beak, float size, boolean longLegs) {
        ModelSpec.Builder b = ModelSpec.builder(id).scale(size).shadow(0.3f);
        int legH = longLegs ? 9 : 3;
        float bodyY = 24 - legH - 3;
        b.part("body", null, Role.BODY, color, beak).pivot(0, bodyY, 0).box(-3, -3, -4, 6, 6, 8).pattern("plates");
        String neck = "body";
        if (longLegs) {
            b.part("neck", "body", Role.NECK, color).pivot(0, -3, -3).rotate(15, 0, 0).box(-1, -6, -1, 2, 6, 2);
            neck = "neck";
        }
        b.part("head", neck, Role.HEAD, color, beak).pivot(0, longLegs ? -6 : -3, longLegs ? 0 : -3).box(-2, -4, -3, 4, 4, 4)
                .pattern("eyes");
        b.part("beak", "head", Role.STATIC, beak).pivot(0, -2, -3).box(-0.5f, -0.5f, longLegs ? -5 : -2, 1, 1, longLegs ? 5 : 2);
        b.part("wing_l", "body", Role.WING_LEFT, color).pivot(3, -2, 0).box(0, 0, -3, 1, 4, 7);
        b.part("wing_r", "body", Role.WING_RIGHT, color).pivot(-3, -2, 0).box(-1, 0, -3, 1, 4, 7);
        b.part("leg_l", "body", Role.LEG_BACK_LEFT, beak).pivot(1.5f, 3, 0).box(-0.5f, 0, -0.5f, 1, legH, 1);
        b.part("leg_r", "body", Role.LEG_BACK_RIGHT, beak).pivot(-1.5f, 3, 0).box(-0.5f, 0, -0.5f, 1, legH, 1);
        b.part("tail", "body", Role.TAIL, color).pivot(0, -2, 4).rotate(-20, 0, 0).box(-2, 0, 0, 4, 1, 4);
        return b.build();
    }

    static ModelSpec golem() {
        ModelSpec.Builder b = ModelSpec.builder("ext_pet_rock_golem").shadow(0.35f);
        b.part("body", null, Role.BODY, 0x7A7A7A, 0x4A4A4A).pivot(0, 14, 0).box(-5, -6, -3, 10, 9, 6).pattern("spots");
        b.part("head", "body", Role.HEAD, 0x8A8A8A, 0x4A4A4A).pivot(0, -6, 0).box(-3, -5, -3, 6, 5, 6).pattern("eyes");
        b.part("arm_l", "body", Role.ARM_LEFT, 0x6A6A6A).pivot(5, -5, 0).box(0, 0, -1.5f, 3, 9, 3);
        b.part("arm_r", "body", Role.ARM_RIGHT, 0x6A6A6A).pivot(-5, -5, 0).box(-3, 0, -1.5f, 3, 9, 3);
        b.part("leg_l", "body", Role.LEG_BACK_LEFT, 0x6A6A6A).pivot(2, 3, 0).box(-1.5f, 0, -1.5f, 3, 7, 3);
        b.part("leg_r", "body", Role.LEG_BACK_RIGHT, 0x6A6A6A).pivot(-2, 3, 0).box(-1.5f, 0, -1.5f, 3, 7, 3);
        return b.build();
    }

    static ModelSpec tangleroot() {
        ModelSpec.Builder b = ModelSpec.builder("ext_pet_tangleroot").shadow(0.3f);
        b.part("stem", null, Role.BODY, 0x5A4A2A, 0x4A7A2A).pivot(0, 18, 0).box(-2, -8, -2, 4, 10, 4).pattern("stripes");
        b.part("head", "stem", Role.HEAD, 0x4A7A2A, 0xE0E040).pivot(0, -8, 0).box(-3.5f, -6, -3.5f, 7, 6, 7).pattern("eyes");
        b.part("root_l", "stem", Role.LEG_BACK_LEFT, 0x5A4A2A).pivot(1, 2, 0).rotate(0, 0, -30).box(0, 0, -1, 2, 5, 2);
        b.part("root_r", "stem", Role.LEG_BACK_RIGHT, 0x5A4A2A).pivot(-1, 2, 0).rotate(0, 0, 30).box(-2, 0, -1, 2, 5, 2);
        b.part("vine_l", "stem", Role.TENTACLE, 0x3A8A2A).pivot(2, -6, 0).rotate(0, 0, -50).box(0, -1, -1, 6, 2, 2);
        b.part("vine_r", "stem", Role.TENTACLE, 0x3A8A2A).pivot(-2, -6, 0).rotate(0, 0, 50).box(-6, -1, -1, 6, 2, 2);
        return b.build();
    }

    static ModelSpec riftGuardian() {
        ModelSpec.Builder b = ModelSpec.builder("ext_pet_rift_guardian").shadow(0.3f);
        b.part("core", null, Role.FLOAT, 0xC03030, 0xF0A040).pivot(0, 14, 0).box(-4, -4, -4, 8, 8, 8).pattern("glow");
        b.part("eye", "core", Role.HEAD, 0x2A2A3A, 0xF0F0F0).pivot(0, 0, -4).box(-2, -2, -1, 4, 4, 1).pattern("eyes");
        b.part("ring", "core", Role.SPIN, 0x8A6AC0).pivot(0, 0, 0).box(-7, -0.5f, -0.5f, 2, 1, 1).box(5, -0.5f, -0.5f, 2, 1, 1)
                .box(-0.5f, -0.5f, -7, 1, 1, 2).box(-0.5f, -0.5f, 5, 1, 1, 2);
        return b.build();
    }

    static ModelSpec tempor() {
        ModelSpec.Builder b = ModelSpec.builder("ext_pet_tempor").shadow(0.3f);
        b.part("body", null, Role.FLOAT, 0x3A6A9A, 0xC0E0F0).pivot(0, 16, 0).box(-3, -4, -6, 6, 7, 12).pattern("scales");
        b.part("head", "body", Role.HEAD, 0x3A6A9A, 0xC0E0F0).pivot(0, -1, -6).box(-2.5f, -3, -4, 5, 5, 4).pattern("eyes");
        b.part("fin_l", "body", Role.FIN, 0x2A4A7A).pivot(3, 0, -1).box(0, -1, -2, 3, 1, 4);
        b.part("fin_r", "body", Role.FIN, 0x2A4A7A).pivot(-3, 0, -1).box(-3, -1, -2, 3, 1, 4);
        b.part("tail", "body", Role.TAIL, 0x2A4A7A).pivot(0, -1, 6).box(-0.5f, -3, 0, 1, 6, 4);
        return b.build();
    }
}
