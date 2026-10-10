package com.runecraft.osrs.content.raids;

import com.runecraft.osrs.model.ModelSpec;

import java.util.List;

import static com.runecraft.osrs.model.ModelSpec.Role.ARM_LEFT;
import static com.runecraft.osrs.model.ModelSpec.Role.ARM_RIGHT;
import static com.runecraft.osrs.model.ModelSpec.Role.BODY;
import static com.runecraft.osrs.model.ModelSpec.Role.FLOAT;
import static com.runecraft.osrs.model.ModelSpec.Role.HEAD;
import static com.runecraft.osrs.model.ModelSpec.Role.JAW;
import static com.runecraft.osrs.model.ModelSpec.Role.LEG_BACK_LEFT;
import static com.runecraft.osrs.model.ModelSpec.Role.LEG_BACK_RIGHT;
import static com.runecraft.osrs.model.ModelSpec.Role.LEG_EXTRA;
import static com.runecraft.osrs.model.ModelSpec.Role.LEG_FRONT_LEFT;
import static com.runecraft.osrs.model.ModelSpec.Role.LEG_FRONT_RIGHT;
import static com.runecraft.osrs.model.ModelSpec.Role.NECK;
import static com.runecraft.osrs.model.ModelSpec.Role.STATIC;
import static com.runecraft.osrs.model.ModelSpec.Role.TAIL;
import static com.runecraft.osrs.model.ModelSpec.Role.TENTACLE;
import static com.runecraft.osrs.model.ModelSpec.Role.WING_LEFT;
import static com.runecraft.osrs.model.ModelSpec.Role.WING_RIGHT;

/** Custom models for the Theatre of Blood. */
public final class TobModels {
    private TobModels() {
    }

    public static List<ModelSpec> models() {
        return List.of(maiden(), bloat(), bloodSpawn(),
                nylocas("nylocas_matomenos", 0x8A6A6A, 0xA02A2A, 1.0f),
                nylocas("nylocas_ischyros", 0xC8C8C0, 0x6A6A60, 0.9f),
                nylocas("nylocas_toxobolos", 0x4A9A3A, 0x2A5A2A, 0.9f),
                nylocas("nylocas_hagios", 0x3A6AC8, 0x2A3A8A, 0.9f),
                nylocas("nylocas_athanatos", 0x8A3AB0, 0x4A1A6A, 1.2f),
                nylocas("nylocas_vasilias", 0x5A4A6A, 0xC03A3A, 1.0f),
                sotetseg(), xarpus(), verzikP1(), verzikP2(), verzikP3());
    }

    /** The Maiden: a vast, bloated blood-red woman chained in place, trailing tendrils. */
    static ModelSpec maiden() {
        int flesh = 0xA03A3A;
        int dark = 0x5A1A1A;
        return ModelSpec.builder("the_maiden_of_sugadinti").shadow(1.2f)
                .part("pool", null, STATIC, 0x6A0A0A).pivot(0, 24, 0).box(-14, -1, -12, 28, 1, 24)
                .part("skirt", null, STATIC, dark).pivot(0, 23, 0).box(-11, -10, -9, 22, 10, 18).pattern("spots")
                .part("body", null, BODY, flesh).pivot(0, 13, 0).box(-9, -16, -6, 18, 16, 12).pattern("spots")
                .part("head", "body", HEAD, flesh).pivot(0, -16, -1).box(-5, -9, -5, 10, 9, 9).pattern("eyes")
                .part("hair", "head", STATIC, dark).pivot(0, 0, 0).box(-5.5f, -10, -2, 11, 12, 7)
                .part("arm_l", "body", ARM_LEFT, flesh).pivot(-10, -14, 0).box(-4, -2, -3, 4, 18, 6)
                .part("arm_r", "body", ARM_RIGHT, flesh).pivot(10, -14, 0).box(0, -2, -3, 4, 18, 6)
                .part("chain_l", "arm_l", STATIC, 0x5A5A5A).pivot(-2, 12, 0).box(-6, -1, -1, 6, 2, 2)
                .part("chain_r", "arm_r", STATIC, 0x5A5A5A).pivot(2, 12, 0).box(0, -1, -1, 6, 2, 2)
                .part("tendril_1", "skirt", TENTACLE, flesh).pivot(-8, -4, -8).box(-1, -1, -14, 2, 2, 14)
                .part("tendril_2", "skirt", TENTACLE, flesh).pivot(8, -4, -8).box(-1, -1, -14, 2, 2, 14)
                .part("tendril_3", "skirt", TENTACLE, flesh).pivot(-10, -3, 4).rotate(0, 60, 0).box(-1, -1, -12, 2, 2, 12)
                .part("tendril_4", "skirt", TENTACLE, flesh).pivot(10, -3, 4).rotate(0, -60, 0).box(-1, -1, -12, 2, 2, 12)
                .build();
    }

    /** The Pestilent Bloat: a stitched-together sack of rotting flesh on stubby legs. */
    static ModelSpec bloat() {
        int rot = 0x7A8A5A;
        int stitch = 0x3A2A1A;
        return ModelSpec.builder("pestilent_bloat").shadow(1.2f)
                .part("leg_fl", null, LEG_FRONT_LEFT, rot).pivot(-7, 18, -6).box(-3, 0, -3, 5, 6, 5)
                .part("leg_fr", null, LEG_FRONT_RIGHT, rot).pivot(7, 18, -6).box(-2, 0, -3, 5, 6, 5)
                .part("leg_bl", null, LEG_BACK_LEFT, rot).pivot(-7, 18, 6).box(-3, 0, -2, 5, 6, 5)
                .part("leg_br", null, LEG_BACK_RIGHT, rot).pivot(7, 18, 6).box(-2, 0, -2, 5, 6, 5)
                .part("sack", null, BODY, rot).pivot(0, 18, 0).box(-11, -20, -10, 22, 20, 20).pattern("spots")
                .part("stitches", "sack", STATIC, stitch).pivot(0, 0, 0).box(-0.5f, -20.5f, -10.5f, 1, 21, 21)
                .part("growths", "sack", STATIC, 0x9AAA6A).pivot(0, 0, 0).box(-13, -14, -4, 3, 6, 6).box(10, -16, 0, 3, 7, 6)
                .box(-4, -23, 2, 7, 3, 6)
                .part("head", "sack", HEAD, rot).pivot(0, -12, -10).box(-4, -4, -5, 8, 7, 5).pattern("eyes")
                .part("jaw", "head", JAW, stitch).pivot(0, 2, -4).box(-3, 0, -2, 6, 2, 3)
                .part("arm_l", "sack", ARM_LEFT, rot).pivot(-11, -12, -4).box(-3, -1, -2, 3, 12, 4)
                .part("arm_r", "sack", ARM_RIGHT, rot).pivot(11, -12, -4).box(0, -1, -2, 3, 12, 4)
                .build();
    }

    static ModelSpec bloodSpawn() {
        return ModelSpec.builder("blood_spawn").shadow(0.4f)
                .part("blob", null, FLOAT, 0xB01A1A).pivot(0, 16, 0).box(-4, -4, -4, 8, 8, 8).pattern("spots")
                .part("eye", "blob", HEAD, 0xB01A1A).pivot(0, -2, -4).box(-2, -2, -1, 4, 3, 1).pattern("eyes")
                .part("drip_1", "blob", TENTACLE, 0x7A0A0A).pivot(-2, 4, 0).box(-1, 0, -1, 2, 5, 2)
                .part("drip_2", "blob", TENTACLE, 0x7A0A0A).pivot(2, 4, 1).box(-1, 0, -1, 2, 4, 2)
                .build();
    }

    /** Nylocas: a spider-like bloodsucker. The colour shows which style kills it. */
    static ModelSpec nylocas(String id, int body, int accent, float scale) {
        return ModelSpec.builder(id).scale(scale).shadow(0.8f)
                .part("thorax", null, BODY, body).pivot(0, 17, 0).box(-5, -4, -5, 10, 6, 10).pattern("plates")
                .part("abdomen", "thorax", TAIL, body).pivot(0, -1, 5).box(-6, -6, 0, 12, 9, 12).pattern("spots")
                .part("markings", "abdomen", STATIC, accent).pivot(0, 0, 0).box(-3, -6.5f, 2, 6, 1, 8)
                .part("head", "thorax", HEAD, body).pivot(0, -1, -5).box(-3.5f, -3, -5, 7, 5, 5).pattern("eyes")
                .part("fangs", "head", JAW, accent).pivot(0, 2, -5).box(-2.5f, 0, -2, 1, 3, 1).box(1.5f, 0, -2, 1, 3, 1)
                .part("leg_1", "thorax", LEG_EXTRA, accent).pivot(-5, 0, -3).rotate(0, 20, -40).box(-12, -1, -1, 12, 2, 2)
                .part("leg_2", "thorax", LEG_EXTRA, accent).pivot(5, 0, -3).rotate(0, -20, 40).box(0, -1, -1, 12, 2, 2)
                .part("leg_3", "thorax", LEG_EXTRA, accent).pivot(-5, 0, -1).rotate(0, 5, -40).box(-12, -1, -1, 12, 2, 2)
                .part("leg_4", "thorax", LEG_EXTRA, accent).pivot(5, 0, -1).rotate(0, -5, 40).box(0, -1, -1, 12, 2, 2)
                .part("leg_5", "thorax", LEG_EXTRA, accent).pivot(-5, 0, 1).rotate(0, -10, -40).box(-12, -1, -1, 12, 2, 2)
                .part("leg_6", "thorax", LEG_EXTRA, accent).pivot(5, 0, 1).rotate(0, 10, 40).box(0, -1, -1, 12, 2, 2)
                .part("leg_7", "thorax", LEG_EXTRA, accent).pivot(-5, 0, 3).rotate(0, -25, -40).box(-12, -1, -1, 12, 2, 2)
                .part("leg_8", "thorax", LEG_EXTRA, accent).pivot(5, 0, 3).rotate(0, 25, 40).box(0, -1, -1, 12, 2, 2)
                .build();
    }

    /** Sotetseg: a crimson, maze-weaving hound of the Theatre with huge clawed forelimbs. */
    static ModelSpec sotetseg() {
        int red = 0xB02A2A;
        int dark = 0x5A1010;
        return ModelSpec.builder("sotetseg").shadow(1.2f)
                .part("body", null, BODY, red).pivot(0, 10, 2).box(-8, -8, -12, 16, 12, 24).pattern("plates")
                .part("spine", "body", STATIC, dark).pivot(0, 0, 0).box(-2, -12, -10, 4, 4, 20).pattern("plates")
                .part("neck", "body", NECK, red).pivot(0, -4, -12).box(-5, -5, -6, 10, 9, 6)
                .part("head", "neck", HEAD, red).pivot(0, -1, -6).box(-5, -5, -9, 10, 8, 9).pattern("eyes")
                .part("jaw", "head", JAW, dark).pivot(0, 2, -4).box(-4, 0, -7, 8, 3, 8)
                .part("horns", "head", STATIC, 0xE0D0C0).pivot(0, 0, 0).box(-6, -9, -2, 2, 5, 2).box(4, -9, -2, 2, 5, 2)
                .part("leg_fl", "body", LEG_FRONT_LEFT, red).pivot(-8, 2, -9).box(-4, 0, -3, 5, 12, 6)
                .part("claw_fl", "leg_fl", STATIC, 0xE0D0C0).pivot(0, 0, 0).box(-4, 11, -6, 5, 1, 4)
                .part("leg_fr", "body", LEG_FRONT_RIGHT, red).pivot(8, 2, -9).box(-1, 0, -3, 5, 12, 6)
                .part("claw_fr", "leg_fr", STATIC, 0xE0D0C0).pivot(0, 0, 0).box(-1, 11, -6, 5, 1, 4)
                .part("leg_bl", "body", LEG_BACK_LEFT, red).pivot(-7, 2, 9).box(-3, 0, -3, 4, 12, 5)
                .part("leg_br", "body", LEG_BACK_RIGHT, red).pivot(7, 2, 9).box(-1, 0, -3, 4, 12, 5)
                .part("tail", "body", TAIL, dark).pivot(0, -4, 12).box(-2, -2, 0, 4, 4, 14)
                .build();
    }

    /** Xarpus: a hunched, diseased gargoyle with a long neck and a beaked head. */
    static ModelSpec xarpus() {
        int skin = 0x6A7A6A;
        int dark = 0x3A4A3A;
        return ModelSpec.builder("xarpus").shadow(1.2f)
                .part("leg_l", null, LEG_FRONT_LEFT, dark).pivot(-6, 14, 2).box(-3, 0, -3, 6, 10, 6)
                .part("leg_r", null, LEG_FRONT_RIGHT, dark).pivot(6, 14, 2).box(-3, 0, -3, 6, 10, 6)
                .part("body", null, BODY, skin).pivot(0, 14, 2).box(-10, -16, -8, 20, 16, 16).pattern("spots")
                .part("hump", "body", STATIC, dark).pivot(0, 0, 0).box(-7, -20, -2, 14, 5, 10).pattern("plates")
                .part("neck", "body", NECK, skin).pivot(0, -12, -8).rotate(-30, 0, 0).box(-3, -3, -10, 6, 6, 10)
                .part("head", "neck", HEAD, skin).pivot(0, 0, -10).box(-4, -4, -7, 8, 7, 7).pattern("eyes")
                .part("beak", "head", STATIC, 0xC8B880).pivot(0, 0, 0).box(-2, -1, -12, 4, 3, 5)
                .part("jaw", "head", JAW, 0xA89860).pivot(0, 2, -7).box(-2, 0, -4, 4, 1, 4)
                .part("wing_l", "body", WING_LEFT, dark).pivot(-9, -14, 0).box(-14, -1, -6, 14, 1, 14)
                .part("wing_r", "body", WING_RIGHT, dark).pivot(9, -14, 0).box(0, -1, -6, 14, 1, 14)
                .part("arm_l", "body", ARM_LEFT, skin).pivot(-10, -10, -4).box(-3, -1, -2, 3, 14, 4)
                .part("arm_r", "body", ARM_RIGHT, skin).pivot(10, -10, -4).box(0, -1, -2, 3, 14, 4)
                .part("pustules", "body", STATIC, 0x9AC060).pivot(0, 0, 0).box(-11, -10, -4, 2, 3, 3).box(9, -6, 2, 2, 3, 3)
                .box(-3, -8, -9, 3, 3, 1).pattern("glow")
                .build();
    }

    /** Verzik Vitur, phase 1: seated upon her throne of bone. */
    static ModelSpec verzikP1() {
        int robe = 0x2A1A2A;
        int skin = 0xD8D0D8;
        return ModelSpec.builder("verzik_vitur").shadow(1.0f)
                .part("throne", null, STATIC, 0xC8B8A0).pivot(0, 24, 0).box(-9, -10, -7, 18, 10, 14).box(-9, -34, 4, 18, 24, 3)
                .box(-11, -16, -7, 2, 6, 14).box(9, -16, -7, 2, 6, 14).pattern("plates")
                .part("skulls", "throne", STATIC, 0xE8E0D0).pivot(0, 0, 0).box(-8, -37, 4, 4, 3, 3).box(-2, -38, 4, 4, 4, 3)
                .box(4, -37, 4, 4, 3, 3)
                .part("body", null, BODY, robe).pivot(0, 14, 1).box(-5, -12, -3, 10, 12, 6).pattern("stripes")
                .part("lap", "body", STATIC, robe).pivot(0, 0, 0).box(-5, -1, -8, 10, 3, 9)
                .part("shins", "body", STATIC, robe).pivot(0, 0, 0).box(-5, 1, -9, 10, 9, 3)
                .part("head", "body", HEAD, skin).pivot(0, -12, 0).box(-4, -8, -4, 8, 8, 8).pattern("eyes")
                .part("hair", "head", STATIC, 0x1A1A1A).pivot(0, 0, 0).box(-4.5f, -9, -1, 9, 11, 6)
                .part("crown", "head", STATIC, 0xC0A040).pivot(0, 0, 0).box(-4, -10, -4, 8, 2, 8)
                .part("arm_l", "body", ARM_LEFT, skin).pivot(-6, -11, 0).box(-2, -1, -2, 3, 11, 3)
                .part("arm_r", "body", ARM_RIGHT, skin).pivot(6, -11, 0).box(-1, -1, -2, 3, 11, 3)
                .build();
    }

    /** Verzik Vitur, phase 2: risen as a gaunt, bloated horror. */
    static ModelSpec verzikP2() {
        int pale = 0xB8A8B8;
        int dark = 0x4A2A4A;
        return ModelSpec.builder("verzik_vitur_p2").shadow(1.1f)
                .part("leg_l", null, LEG_FRONT_LEFT, dark).pivot(-4, 12, 0).box(-2, 0, -2, 4, 12, 4)
                .part("leg_r", null, LEG_FRONT_RIGHT, dark).pivot(4, 12, 0).box(-2, 0, -2, 4, 12, 4)
                .part("body", null, BODY, pale).pivot(0, 12, 0).box(-9, -16, -8, 18, 16, 14).pattern("spots")
                .part("ribs", "body", STATIC, 0xE0D8E0).pivot(0, 0, 0).box(-7, -15, -8.5f, 14, 6, 1)
                .part("spines", "body", STATIC, dark).pivot(0, 0, 0).box(-6, -20, 2, 2, 5, 2).box(-1, -21, 3, 2, 6, 2)
                .box(4, -20, 2, 2, 5, 2)
                .part("head", "body", HEAD, pale).pivot(0, -16, -3).box(-4, -8, -5, 8, 8, 8).pattern("eyes")
                .part("jaw", "head", JAW, dark).pivot(0, -1, -4).box(-3, 0, -2, 6, 3, 3)
                .part("hair", "head", STATIC, 0x1A1A1A).pivot(0, 0, 0).box(-4.5f, -9, 0, 9, 14, 4)
                .part("arm_l", "body", ARM_LEFT, pale).pivot(-10, -14, -2).box(-3, -1, -2, 3, 20, 4)
                .part("arm_r", "body", ARM_RIGHT, pale).pivot(10, -14, -2).box(0, -1, -2, 3, 20, 4)
                .part("talons_l", "arm_l", STATIC, dark).pivot(0, 0, 0).box(-3.5f, 19, -3, 4, 4, 5)
                .part("talons_r", "arm_r", STATIC, dark).pivot(0, 0, 0).box(-0.5f, 19, -3, 4, 4, 5)
                .build();
    }

    /** Verzik Vitur, phase 3: her true form, a giant blood spider with a ghastly face. */
    static ModelSpec verzikP3() {
        int body = 0x5A1A2A;
        int leg = 0x2A0A10;
        ModelSpec.Builder b = ModelSpec.builder("verzik_vitur_p3").shadow(1.5f)
                .part("thorax", null, BODY, body).pivot(0, 12, 0).box(-8, -6, -8, 16, 10, 16).pattern("plates")
                .part("abdomen", "thorax", TAIL, body).pivot(0, -2, 8).box(-10, -10, 0, 20, 16, 18).pattern("spots")
                .part("mark", "abdomen", STATIC, 0xD03030).pivot(0, 0, 0).box(-4, -10.5f, 4, 8, 1, 10).pattern("glow")
                .part("torso", "thorax", STATIC, 0xB8A8B8).pivot(0, -6, -6).box(-4, -10, -3, 8, 10, 5)
                .part("head", "torso", HEAD, 0xC8B8C8).pivot(0, -10, 0).box(-4, -8, -4, 8, 8, 7).pattern("eyes")
                .part("hair", "head", STATIC, 0x1A1A1A).pivot(0, 0, 0).box(-4.5f, -9, 0, 9, 12, 4)
                .part("fangs", "thorax", JAW, 0xE0D8D0).pivot(0, 2, -8).box(-4, 0, -2, 2, 5, 2).box(2, 0, -2, 2, 5, 2);
        float[] zs = {-6, -2, 2, 6};
        float[] yaws = {25, 8, -8, -25};
        for (int i = 0; i < 4; i++) {
            b.part("leg_l" + i, "thorax", LEG_EXTRA, leg).pivot(-8, -2, zs[i]).rotate(0, yaws[i], -35).box(-20, -1.5f, -1.5f, 20, 3, 3);
            b.part("leg_r" + i, "thorax", LEG_EXTRA, leg).pivot(8, -2, zs[i]).rotate(0, -yaws[i], 35).box(0, -1.5f, -1.5f, 20, 3, 3);
        }
        return b.build();
    }
}
