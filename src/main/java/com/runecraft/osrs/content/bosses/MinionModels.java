package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.List;

import static com.runecraft.osrs.content.bosses.ModelKit.chainDown;
import static com.runecraft.osrs.content.bosses.ModelKit.chainUp;
import static com.runecraft.osrs.content.bosses.ModelKit.chainZ;
import static com.runecraft.osrs.content.bosses.ModelKit.legs2;
import static com.runecraft.osrs.content.bosses.ModelKit.legs4;
import static com.runecraft.osrs.content.bosses.ModelKit.spiderLegs;

/** Custom models for boss minions and mechanic objects (same ids as their creatures). */
final class MinionModels {
    private MinionModels() {
    }

    static List<ModelSpec> all() {
        return List.of(snakeling(), tentacle("enormous_tentacle", 0x8A3A6A, 0xC07090), zombifiedSpawn(),
                soul("summoned_soul_ranged", 0x40C040), soul("summoned_soul_magic", 0x4060E0), soul("summoned_soul_melee", 0xE04040),
                respiratorySystem(), abyssalSpawn(), darkCore(), awakenedAltar(), flower(), spider("spawn_of_sarachnis", 0x8A2A1A, 0xE0A040, 0.6f),
                rat(), spider("acidic_araxyte", 0x4A6A2A, 0xC0E040, 0.8f),
                spider("mirrorback_araxyte", 0x8A8A9A, 0xE0E0F0, 0.8f), spider("ruptura_araxyte", 0x8A2A3A, 0xF06040, 0.8f),
                tentacle("whisperer_tentacle", 0x1A2A4A, 0x50E0E0), scion());
    }

    static ModelSpec snakeling() {
        ModelSpec.Builder b = ModelSpec.builder("snakeling").shadow(0.4f);
        b.part("coil", null, Role.STATIC, 0x3A8A3A, 0x206A20).pivot(0, 22, 0).box(-4, -2, -4, 8, 4, 8).pattern("scales");
        String neck = chainUp(b, "neck", null, Role.NECK, 0x3A8A3A, 2, 0, 20, 0, 4, 4, 10, 0, "scales");
        b.part("head", neck, Role.HEAD, 0x3A8A3A, 0xF0E040).pivot(0, -4, 0).box(-2.5f, -3, -5, 5, 3, 6).pattern("eyes");
        b.part("jaw", "head", Role.JAW, 0x9AD06A).pivot(0, 0, 0).box(-2, 0, -4, 4, 1, 4);
        chainZ(b, "tail", "coil", Role.TAIL, 0x3A8A3A, 2, 2, 0, 3, 1, 2, 2, 4, 0, "scales");
        return b.build();
    }

    static ModelSpec tentacle(String id, int color, int pale) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.6f);
        b.part("base", null, Role.STATIC, 0x2A4A6A, color).pivot(0, 24, 0).box(-5, -2, -5, 10, 2, 10).pattern("spots");
        String tip = chainUp(b, "seg", null, Role.TENTACLE, color, 4, 0, 22, 0, 5, 8, 12, 0, "spots");
        b.part("tip", tip, Role.TENTACLE, pale).pivot(0, -8, 0).rotate(-20, 0, 0).box(-1, -5, -1, 2, 5, 2);
        return b.build();
    }

    static ModelSpec zombifiedSpawn() {
        ModelSpec.Builder b = ModelSpec.builder("zombified_spawn").shadow(0.4f);
        b.part("torso", null, Role.BODY, 0xC8C0A8, 0x6AB0E0).pivot(0, 12, 0).rotate(25, 0, 0).box(-3, -6, -2, 6, 7, 4).pattern("plates");
        b.part("head", "torso", Role.HEAD, 0xD8D0B8, 0x6AB0E0).pivot(0, -6, -1).box(-2.5f, -5, -4, 5, 5, 5).pattern("eyes");
        b.part("arm_l", "torso", Role.ARM_LEFT, 0xC8C0A8).pivot(3, -5, 0).rotate(-60, 0, 0).box(0, 0, -1, 2, 7, 2);
        b.part("arm_r", "torso", Role.ARM_RIGHT, 0xC8C0A8).pivot(-3, -5, 0).rotate(-60, 0, 0).box(-2, 0, -1, 2, 7, 2);
        legs2(b, "leg", 0xC8C0A8, 1.5f, 1, 11, 2);
        return b.build();
    }

    static ModelSpec soul(String id, int glow) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.4f);
        b.part("robe", null, Role.FLOAT, 0x2A2A3A, glow).pivot(0, 10, 0).box(-4, -8, -3, 8, 10, 6).box(-5, 2, -4, 10, 8, 8)
                .pattern("stripes");
        b.part("head", "robe", Role.HEAD, 0xD0D0E0, glow).pivot(0, -8, 0).box(-3, -6, -3, 6, 6, 6).pattern("eyes");
        b.part("hood", "head", Role.STATIC, 0x2A2A3A).pivot(0, -6, 1).box(-3.5f, -1, -3, 7, 7, 6, 0.25f);
        b.part("arm_l", "robe", Role.ARM_LEFT, 0x2A2A3A).pivot(4, -7, 0).box(0, 0, -1, 2, 9, 2);
        b.part("arm_r", "robe", Role.ARM_RIGHT, 0x2A2A3A).pivot(-4, -7, 0).box(-2, 0, -1, 2, 9, 2);
        b.part("wisp", "robe", Role.SPIN, glow).pivot(0, -4, 0).box(-7, 0, -1, 2, 2, 2).box(5, -2, 0, 2, 2, 2);
        return b.build();
    }

    static ModelSpec respiratorySystem() {
        ModelSpec.Builder b = ModelSpec.builder("respiratory_system").shadow(0.8f);
        b.part("sack", null, Role.BODY, 0x8A2A4A, 0xC06A8A).pivot(0, 16, 0).box(-6, -8, -6, 12, 12, 12).box(-4, -12, -4, 8, 4, 8)
                .pattern("spots");
        for (int i = 0; i < 4; i++) {
            float yaw = i * 90 + 45;
            float x = (float) Math.sin(Math.toRadians(yaw)) * 5;
            float z = (float) Math.cos(Math.toRadians(yaw)) * 5;
            chainDown(b, "tube" + i + "_", "sack", Role.TENTACLE, 0x6A1A3A, 2, x, 2, z, 2, 4, 70, yaw, "none");
        }
        return b.build();
    }

    static ModelSpec abyssalSpawn() {
        ModelSpec.Builder b = ModelSpec.builder("abyssal_spawn").shadow(0.4f);
        b.part("blob", null, Role.BODY, 0x6A1A3A, 0xA04A6A).pivot(0, 19, 0).box(-4, -4, -4, 8, 7, 8).pattern("spots");
        b.part("eye", "blob", Role.HEAD, 0xE0D040, 0x202020).pivot(0, -2, -4).box(-2, -1, -1, 4, 2, 1).pattern("eyes");
        for (int i = 0; i < 4; i++) {
            float yaw = i * 90;
            chainDown(b, "leg" + i + "_", "blob", Role.TENTACLE, 0x5A1A2A, 1, (float) Math.sin(Math.toRadians(yaw)) * 3, 2,
                    (float) Math.cos(Math.toRadians(yaw)) * 3, 2, 4, 50, yaw, "none");
        }
        return b.build();
    }

    static ModelSpec scion() {
        ModelSpec.Builder b = ModelSpec.builder("abyssal_scion").shadow(0.6f);
        b.part("torso", null, Role.BODY, 0x6A1A3A, 0xA04A6A).pivot(0, 10, 0).box(-4, -8, -3, 8, 10, 6).pattern("spots");
        b.part("head", "torso", Role.HEAD, 0x6A1A3A, 0xF04040).pivot(0, -8, 0).box(-3, -6, -3, 6, 6, 6).pattern("eyes");
        b.part("arm_l", "torso", Role.ARM_LEFT, 0x5A1A2A).pivot(4, -7, 0).box(0, 0, -1, 2, 10, 2);
        b.part("arm_r", "torso", Role.ARM_RIGHT, 0x5A1A2A).pivot(-4, -7, 0).box(-2, 0, -1, 2, 10, 2);
        chainDown(b, "tendril_a", "torso", Role.TENTACLE, 0x5A1A2A, 2, 2, -4, 3, 2, 5, 40, 160, "none");
        chainDown(b, "tendril_b", "torso", Role.TENTACLE, 0x5A1A2A, 2, -2, -4, 3, 2, 5, 40, 200, "none");
        legs2(b, "leg", 0x5A1A2A, 2, 0, 12, 3);
        return b.build();
    }

    static ModelSpec darkCore() {
        ModelSpec.Builder b = ModelSpec.builder("dark_energy_core").shadow(0.3f);
        b.part("core", null, Role.FLOAT, 0x1A1A2A, 0x6A4AC0).pivot(0, 16, 0).box(-3, -3, -3, 6, 6, 6).pattern("glow");
        b.part("shell", "core", Role.SPIN, 0x3A2A5A, 0x9A6AE0).pivot(0, 0, 0).box(-5, -1, -1, 2, 2, 2).box(3, -1, -1, 2, 2, 2)
                .box(-1, -1, -5, 2, 2, 2).box(-1, -1, 3, 2, 2, 2).box(-1, -5, -1, 2, 2, 2);
        return b.build();
    }

    static ModelSpec awakenedAltar() {
        ModelSpec.Builder b = ModelSpec.builder("awakened_altar").shadow(0.8f);
        b.part("altar", null, Role.STATIC, 0x3A2A3A, 0x8A2A4A).pivot(0, 24, 0).box(-7, -8, -5, 14, 8, 10).box(-8, -10, -6, 16, 2, 12)
                .pattern("plates");
        b.part("eye", null, Role.FLOAT, 0xE04040, 0xFFE040).pivot(0, 9, 0).box(-2.5f, -2.5f, -2.5f, 5, 5, 5).pattern("eyes");
        b.part("flames", "eye", Role.SPIN, 0xA020C0).pivot(0, 0, 0).box(-6, -1, -1, 2, 2, 2).box(4, 1, -1, 2, 2, 2);
        return b.build();
    }

    static ModelSpec flower() {
        ModelSpec.Builder b = ModelSpec.builder("hesporis_flower").shadow(0.4f);
        b.part("stem", null, Role.NECK, 0x2A5A1A).pivot(0, 24, 0).box(-1, -12, -1, 2, 12, 2);
        b.part("bloom", "stem", Role.HEAD, 0xE070B0, 0xF0E040).pivot(0, -12, 0).box(-3, -3, -3, 6, 3, 6).pattern("eyes");
        b.part("petals", "bloom", Role.SPIN, 0xF0A0D0).pivot(0, -1, 0).box(-6, -1, -1, 3, 1, 2).box(3, -1, -1, 3, 1, 2)
                .box(-1, -1, -6, 2, 1, 3).box(-1, -1, 3, 2, 1, 3);
        b.part("leaves", "stem", Role.FIN, 0x3A7A2A).pivot(0, -4, 0).box(-5, 0, -0.5f, 10, 1, 1);
        return b.build();
    }

    static ModelSpec spider(String id, int color, int accent, float size) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.6f).scale(size);
        b.part("body", null, Role.BODY, color, accent).pivot(0, 17, -2).box(-4, -3, -4, 8, 5, 7).pattern("plates");
        b.part("abdomen", "body", Role.STATIC, color, accent).pivot(0, -1, 3).rotate(-15, 0, 0).box(-5, -4, 0, 10, 8, 9).pattern("spots");
        b.part("head", "body", Role.HEAD, color, 0xE04020).pivot(0, 0, -4).box(-2.5f, -2, -3, 5, 4, 3).pattern("eyes");
        b.part("fangs", "head", Role.JAW, accent).pivot(0, 2, -3).box(-2, 0, -1, 1, 2, 1).box(1, 0, -1, 1, 2, 1);
        spiderLegs(b, "leg", "body", color, 4, 3.5f, 1, -3, 2, 6, 5, 1);
        return b.build();
    }

    static ModelSpec rat() {
        ModelSpec.Builder b = ModelSpec.builder("scurrius_giant_rat").shadow(0.4f);
        b.part("body", null, Role.BODY, 0x6A5A4A).pivot(0, 19, 1).box(-3, -3, -5, 6, 5, 10).pattern("spots");
        b.part("head", "body", Role.HEAD, 0x6A5A4A, 0xE02020).pivot(0, -1, -5).box(-2, -2, -4, 4, 4, 4).box(-1, 0, -6, 2, 2, 2)
                .pattern("eyes");
        b.part("ears", "head", Role.STATIC, 0xD89090).pivot(0, -2, -1).box(-3, -2, 0, 2, 2, 1).box(1, -2, 0, 2, 2, 1);
        legs4(b, "leg", 0x5A4A3A, 2.5f, -3, 4, 3, 2);
        chainZ(b, "tail", "body", Role.TAIL, 0xD89090, 3, 0, 0, 5, 1, 1, 1, 4, -5, "none");
        return b.build();
    }
}
