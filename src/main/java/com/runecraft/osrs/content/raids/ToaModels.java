package com.runecraft.osrs.content.raids;

import com.runecraft.osrs.model.ModelSpec;

import java.util.List;

import static com.runecraft.osrs.model.ModelSpec.Role.ARM_LEFT;
import static com.runecraft.osrs.model.ModelSpec.Role.ARM_RIGHT;
import static com.runecraft.osrs.model.ModelSpec.Role.BODY;
import static com.runecraft.osrs.model.ModelSpec.Role.FLOAT;
import static com.runecraft.osrs.model.ModelSpec.Role.HEAD;
import static com.runecraft.osrs.model.ModelSpec.Role.JAW;
import static com.runecraft.osrs.model.ModelSpec.Role.LEG_EXTRA;
import static com.runecraft.osrs.model.ModelSpec.Role.LEG_FRONT_LEFT;
import static com.runecraft.osrs.model.ModelSpec.Role.LEG_FRONT_RIGHT;
import static com.runecraft.osrs.model.ModelSpec.Role.SPIN;
import static com.runecraft.osrs.model.ModelSpec.Role.STATIC;
import static com.runecraft.osrs.model.ModelSpec.Role.TAIL;
import static com.runecraft.osrs.model.ModelSpec.Role.WING_LEFT;
import static com.runecraft.osrs.model.ModelSpec.Role.WING_RIGHT;

/** Custom models for the Tombs of Amascut. */
public final class ToaModels {
    private ToaModels() {
    }

    public static List<ModelSpec> models() {
        return List.of(scarab("kephri", 0x2A5A7A, 0xD8A030, 1.0f, true), scarab("toa_scarab", 0x3A4A5A, 0xC8A040, 0.45f, false),
                akkha(), baboon("baba", 0x9A7A5A, 0xD06060, 1.0f, true), baboon("toa_baboon", 0x8A6A4A, 0xC05050, 0.6f, false),
                boulder(), CoxModels.crocodile("zebak", 0x6A7A4A, 0x3A4A2A, 0xD8C890, 1.15f),
                obelisk(), warden("elidinis_warden", 0x3A9AA0, 0x1A5A60), warden("tumekens_warden", 0xD8A030, 0x8A5A10));
    }

    /** Scarab: Kephri (with a sun disc between her horns) and her scarab minions. */
    static ModelSpec scarab(String id, int shell, int gold, float scale, boolean sunDisc) {
        ModelSpec.Builder b = ModelSpec.builder(id).scale(scale).shadow(1.2f)
                .part("body", null, BODY, 0x1A2A3A).pivot(0, 16, 0).box(-8, -5, -10, 16, 7, 20)
                .part("shell_l", "body", WING_LEFT, shell).pivot(-1, -5, -6).box(-9, -4, 0, 9, 5, 18).pattern("plates")
                .part("shell_r", "body", WING_RIGHT, shell).pivot(1, -5, -6).box(0, -4, 0, 9, 5, 18).pattern("plates")
                .part("head", "body", HEAD, shell).pivot(0, -2, -10).box(-5, -4, -6, 10, 6, 6).pattern("eyes")
                .part("horn", "head", STATIC, gold).pivot(0, -4, -4).box(-1, -6, -3, 2, 6, 3)
                .part("mandibles", "head", JAW, gold).pivot(0, 1, -6).box(-4, 0, -4, 2, 2, 4).box(2, 0, -4, 2, 2, 4);
        for (int i = 0; i < 3; i++) {
            float z = -6 + i * 6;
            b.part("leg_l" + i, "body", LEG_EXTRA, 0x1A2A3A).pivot(-8, 0, z).rotate(0, 0, -30).box(-9, -1, -1, 9, 2, 2);
            b.part("leg_r" + i, "body", LEG_EXTRA, 0x1A2A3A).pivot(8, 0, z).rotate(0, 0, 30).box(0, -1, -1, 9, 2, 2);
        }
        if (sunDisc) {
            b.part("sun", "head", SPIN, 0xF0C040).pivot(0, -12, -2).box(-5, -5, -1, 10, 10, 2).pattern("glow");
        }
        return b.build();
    }

    /** Akkha: a towering warrior of Het in black and gold, wielding a great halberd. */
    static ModelSpec akkha() {
        int black = 0x2A2A30;
        int gold = 0xD8B040;
        return ModelSpec.builder("akkha").shadow(0.8f)
                .part("leg_l", null, LEG_FRONT_LEFT, black).pivot(-3, 12, 0).box(-2, 0, -2, 4, 12, 4)
                .part("leg_r", null, LEG_FRONT_RIGHT, black).pivot(3, 12, 0).box(-2, 0, -2, 4, 12, 4)
                .part("kilt", null, STATIC, gold).pivot(0, 12, 0).box(-5, -1, -3, 10, 6, 6).pattern("stripes")
                .part("body", null, BODY, black).pivot(0, 12, 0).box(-5, -14, -3, 10, 14, 6)
                .part("collar", "body", STATIC, gold).pivot(0, 0, 0).box(-6, -14, -3.5f, 12, 4, 7).pattern("stripes")
                .part("head", "body", HEAD, black).pivot(0, -14, 0).box(-4, -8, -4, 8, 8, 8).pattern("eyes")
                .part("headdress", "head", STATIC, gold).pivot(0, 0, 0).box(-5, -9, -1, 10, 10, 6).pattern("stripes")
                .part("ears", "head", STATIC, black).pivot(0, 0, 0).box(-4, -13, -1, 2, 5, 2).box(2, -13, -1, 2, 5, 2)
                .part("arm_l", "body", ARM_LEFT, black).pivot(-6, -13, 0).box(-3, -1, -2, 3, 13, 4)
                .part("arm_r", "body", ARM_RIGHT, black).pivot(6, -13, 0).box(0, -1, -2, 3, 13, 4)
                .part("halberd", "arm_r", STATIC, 0x5A4A30).pivot(1.5f, 11, 0).box(-0.5f, -24, -0.5f, 1, 34, 1)
                .part("blade", "halberd", STATIC, gold).pivot(0, -24, 0).box(-0.5f, -6, -5, 1, 8, 5).pattern("glow")
                .build();
    }

    /** Ba-Ba (and the lesser baboons): a hulking baboon with a bright face and long arms. */
    static ModelSpec baboon(String id, int fur, int face, float scale, boolean crown) {
        ModelSpec.Builder b = ModelSpec.builder(id).scale(scale).shadow(1.0f)
                .part("leg_l", null, LEG_FRONT_LEFT, fur).pivot(-4, 16, 3).box(-2.5f, 0, -2.5f, 5, 8, 5)
                .part("leg_r", null, LEG_FRONT_RIGHT, fur).pivot(4, 16, 3).box(-2.5f, 0, -2.5f, 5, 8, 5)
                .part("body", null, BODY, fur).pivot(0, 16, 2).rotate(25, 0, 0).box(-7, -16, -5, 14, 16, 10).pattern("spots")
                .part("chest", "body", STATIC, 0xC8A880).pivot(0, 0, 0).box(-4, -13, -5.5f, 8, 9, 1)
                .part("head", "body", HEAD, fur).pivot(0, -16, -2).rotate(-25, 0, 0).box(-4.5f, -7, -5, 9, 8, 8).pattern("eyes")
                .part("muzzle", "head", STATIC, face).pivot(0, 0, 0).box(-2.5f, -4, -9, 5, 4, 4)
                .part("jaw", "head", JAW, face).pivot(0, 0, -6).box(-2, 0, -3, 4, 2, 3)
                .part("mane", "head", STATIC, 0x6A5A4A).pivot(0, 0, 0).box(-6, -8, -1, 12, 9, 5)
                .part("arm_l", "body", ARM_LEFT, fur).pivot(-8, -14, 0).rotate(-25, 0, 0).box(-3, -1, -2.5f, 4, 20, 5)
                .part("arm_r", "body", ARM_RIGHT, fur).pivot(8, -14, 0).rotate(-25, 0, 0).box(-1, -1, -2.5f, 4, 20, 5)
                .part("tail", "body", TAIL, fur).pivot(0, -2, 5).box(-1, -1, 0, 2, 2, 10);
        if (crown) {
            b.part("crown", "head", STATIC, 0xE0B040).pivot(0, 0, 0).box(-4, -10, -4, 8, 3, 6).pattern("glow");
        }
        return b.build();
    }

    static ModelSpec boulder() {
        return ModelSpec.builder("baba_boulder").shadow(0.8f)
                .part("rock", null, SPIN, 0x8A7A60).pivot(0, 16, 0).box(-7, -7, -7, 14, 14, 14).box(-5, -9, -5, 10, 2, 10)
                .box(-5, 7, -5, 10, 1, 10).pattern("spots")
                .build();
    }

    static ModelSpec obelisk() {
        return ModelSpec.builder("wardens_obelisk").shadow(0.9f)
                .part("base", null, STATIC, 0x8A7A5A).pivot(0, 24, 0).box(-8, -4, -8, 16, 4, 16).pattern("plates")
                .part("pillar", "base", STATIC, 0xC8B080).pivot(0, -4, 0).box(-5, -38, -5, 10, 38, 10).pattern("stripes")
                .part("tip", "pillar", STATIC, 0xE0C060).pivot(0, -38, 0).box(-3, -6, -3, 6, 6, 6)
                .part("energy", null, SPIN, 0x80E0F0).pivot(0, -26, 0).box(-3, -3, -3, 6, 6, 6).pattern("glow")
                .part("orb", "energy", FLOAT, 0xF0F0FF).pivot(0, -8, 0).box(-1.5f, -1.5f, -1.5f, 3, 3, 3).pattern("glow")
                .build();
    }

    /** A Warden: a colossal guardian statue with a glowing core in its chest. */
    static ModelSpec warden(String id, int stone, int dark) {
        return ModelSpec.builder(id).shadow(1.2f)
                .part("plinth", null, STATIC, 0x7A6A50).pivot(0, 24, 0).box(-10, -4, -8, 20, 4, 16).pattern("plates")
                .part("legs", "plinth", STATIC, stone).pivot(0, -4, 0).box(-7, -12, -4, 14, 12, 8).pattern("stripes")
                .part("body", null, BODY, stone).pivot(0, 8, 0).box(-9, -20, -5, 18, 20, 10).pattern("plates")
                .part("core", "body", SPIN, 0xF0F0C0).pivot(0, -12, -5).box(-2.5f, -2.5f, -2.5f, 5, 5, 5).pattern("glow")
                .part("head", "body", HEAD, stone).pivot(0, -20, 0).box(-4, -8, -4, 8, 8, 8).pattern("eyes")
                .part("nemes", "head", STATIC, dark).pivot(0, 0, 0).box(-6, -9, -2, 12, 13, 6).pattern("stripes")
                .part("beard", "head", STATIC, dark).pivot(0, 0, 0).box(-1, 0, -4, 2, 4, 2)
                .part("arm_l", "body", ARM_LEFT, stone).pivot(-10, -18, 0).box(-4, -1, -3, 4, 20, 6)
                .part("arm_r", "body", ARM_RIGHT, stone).pivot(10, -18, 0).box(0, -1, -3, 4, 20, 6)
                .part("staff", "arm_r", STATIC, dark).pivot(2, 18, 0).box(-0.5f, -30, -0.5f, 1, 36, 1)
                .part("ankh", "staff", STATIC, 0xF0D060).pivot(0, -30, 0).box(-2, -5, -0.5f, 4, 5, 1).pattern("glow")
                .build();
    }
}
