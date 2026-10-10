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
import static com.runecraft.osrs.model.ModelSpec.Role.SPIN;
import static com.runecraft.osrs.model.ModelSpec.Role.STATIC;
import static com.runecraft.osrs.model.ModelSpec.Role.TAIL;
import static com.runecraft.osrs.model.ModelSpec.Role.TENTACLE;
import static com.runecraft.osrs.model.ModelSpec.Role.WING_LEFT;
import static com.runecraft.osrs.model.ModelSpec.Role.WING_RIGHT;

/** Custom models for the Chambers of Xeric (model pixels, Y=24 is the ground). */
public final class CoxModels {
    private CoxModels() {
    }

    public static List<ModelSpec> models() {
        return List.of(tekton(), vasa(), vasaCrystal(), guardian(), mystic(), shaman(), spawn(),
                crocodile("small_muttadile", 0x5A7A3A, 0x3A5A2A, 0xC8C070, 0.8f),
                crocodile("muttadile", 0x4A6A3A, 0x2A4A2A, 0xD8C870, 1.0f),
                vanguard("vanguard_melee", 0xA03A2A), vanguard("vanguard_ranged", 0x3A8A3A), vanguard("vanguard_magic", 0x3A5AB0),
                wasp("vespula", 0xD8A020, 0x2A2010, 1.0f), wasp("vespine_soldier", 0xC89020, 0x2A2010, 0.55f),
                portal(), iceDemon(), olmHead(), olmClaw("olm_left_claw", true), olmClaw("olm_right_claw", false));
    }

    /** Tekton: a hulking stone-skinned golem carrying a huge forge hammer. */
    static ModelSpec tekton() {
        int skin = 0xA08A64;
        int armour = 0x5A4A3A;
        return ModelSpec.builder("tekton").shadow(1.0f)
                .part("leg_l", null, LEG_FRONT_LEFT, armour).pivot(-5, 10, 0).box(-3.5f, 0, -3.5f, 7, 14, 7)
                .part("leg_r", null, LEG_FRONT_RIGHT, armour).pivot(5, 10, 0).box(-3.5f, 0, -3.5f, 7, 14, 7)
                .part("body", null, BODY, skin).pivot(0, 10, 0).box(-10, -20, -6, 20, 20, 12).pattern("plates")
                .part("belt", "body", STATIC, 0x3A2A1A).pivot(0, 0, 0).box(-10.5f, -4, -6.5f, 21, 4, 13)
                .part("chest_plate", "body", STATIC, armour).pivot(0, 0, 0).box(-7, -18, -7, 14, 10, 1).pattern("plates")
                .part("pauldrons", "body", STATIC, armour).pivot(0, 0, 0).box(-13, -22, -5, 26, 5, 10)
                .box(-12, -24, -3, 4, 3, 6).box(8, -24, -3, 4, 3, 6)
                .part("head", "body", HEAD, skin).pivot(0, -20, -1).box(-4, -7, -5, 8, 8, 9).pattern("eyes")
                .part("brow", "head", STATIC, armour).pivot(0, 0, 0).box(-4.5f, -8, -5.5f, 9, 3, 6)
                .part("arm_l", "body", ARM_LEFT, skin).pivot(-12, -18, 0).box(-6, -2, -4, 6, 18, 8)
                .part("fist_l", "arm_l", STATIC, armour).pivot(0, 0, 0).box(-6.5f, 15, -4.5f, 7, 6, 9)
                .part("arm_r", "body", ARM_RIGHT, skin).pivot(12, -18, 0).box(0, -2, -4, 6, 18, 8)
                .part("fist_r", "arm_r", STATIC, armour).pivot(0, 0, 0).box(-0.5f, 15, -4.5f, 7, 6, 9)
                .part("hammer_handle", "arm_r", STATIC, 0x5A3A1A).pivot(3, 18, 0).box(-1.5f, -1.5f, -18, 3, 3, 22)
                .part("hammer_head", "hammer_handle", STATIC, 0x4A4A50, 0xE06010).pivot(0, 0, -18).box(-5, -6, -6, 10, 12, 8)
                .pattern("glow")
                .build();
    }

    /** Vasa Nistirio: a lumbering golem of pale crystal with a glowing core. */
    static ModelSpec vasa() {
        int crystal = 0xC8B8E8;
        int dark = 0x6A5A8A;
        return ModelSpec.builder("vasa_nistirio").shadow(1.0f)
                .part("leg_l", null, LEG_FRONT_LEFT, dark).pivot(-6, 12, 0).box(-4, 0, -4, 8, 12, 8)
                .part("leg_r", null, LEG_FRONT_RIGHT, dark).pivot(6, 12, 0).box(-4, 0, -4, 8, 12, 8)
                .part("body", null, BODY, crystal).pivot(0, 12, 0).box(-11, -18, -7, 22, 18, 14).pattern("plates")
                .part("core", "body", SPIN, 0xF0A0FF).pivot(0, -10, -7).box(-3, -3, -3, 6, 6, 6).pattern("glow")
                .part("spikes", "body", STATIC, 0xE0D8FF).pivot(0, 0, 0)
                .box(-9, -26, -2, 4, 8, 4).box(-2, -29, 1, 4, 11, 4).box(5, -25, -1, 4, 7, 4).box(-12, -22, 3, 3, 6, 3)
                .box(9, -21, 2, 3, 5, 3)
                .part("head", "body", HEAD, crystal).pivot(0, -18, -4).box(-5, -6, -5, 10, 7, 8).pattern("eyes")
                .part("arm_l", "body", ARM_LEFT, crystal).pivot(-13, -15, 0).box(-5, -2, -4, 6, 22, 8)
                .part("hand_l", "arm_l", STATIC, 0xE0D8FF).pivot(0, 0, 0).box(-6, 19, -5, 8, 6, 10)
                .part("arm_r", "body", ARM_RIGHT, crystal).pivot(13, -15, 0).box(-1, -2, -4, 6, 22, 8)
                .part("hand_r", "arm_r", STATIC, 0xE0D8FF).pivot(0, 0, 0).box(-2, 19, -5, 8, 6, 10)
                .build();
    }

    static ModelSpec vasaCrystal() {
        return ModelSpec.builder("vasa_crystal").shadow(0.6f)
                .part("base", null, STATIC, 0x4A4050).pivot(0, 24, 0).box(-6, -3, -6, 12, 3, 12)
                .part("crystal", null, SPIN, 0xE8A0FF).pivot(0, 21, 0).box(-3, -18, -3, 6, 18, 6).box(-6, -11, -1, 3, 9, 3)
                .box(3, -13, -2, 3, 11, 3).pattern("glow")
                .build();
    }

    /** CoX Guardian: a squat stone statue animated by Xeric's magic. */
    static ModelSpec guardian() {
        int stone = 0x7A7A70;
        return ModelSpec.builder("cox_guardian").shadow(0.9f)
                .part("plinth", null, STATIC, 0x5A5A54).pivot(0, 24, 0).box(-9, -4, -8, 18, 4, 16)
                .part("legs", "plinth", STATIC, stone).pivot(0, -4, 0).box(-7, -8, -5, 14, 8, 10)
                .part("body", null, BODY, stone).pivot(0, 12, 0).box(-10, -16, -6, 20, 16, 12).pattern("plates")
                .part("head", "body", HEAD, stone).pivot(0, -16, -2).box(-5, -8, -5, 10, 8, 9).pattern("eyes")
                .part("crest", "head", STATIC, 0x9A9A8A).pivot(0, 0, 0).box(-1, -12, -5, 2, 4, 10)
                .part("arm_l", "body", ARM_LEFT, stone).pivot(-12, -14, 0).box(-6, -2, -4, 7, 16, 8).box(-7, 13, -5, 9, 7, 10)
                .part("arm_r", "body", ARM_RIGHT, stone).pivot(12, -14, 0).box(-1, -2, -4, 7, 16, 8).box(-2, 13, -5, 9, 7, 10)
                .build();
    }

    static ModelSpec mystic() {
        int bone = 0xE0DCC8;
        int robe = 0x4A2A6A;
        return ModelSpec.builder("skeletal_mystic").shadow(0.5f)
                .part("leg_l", null, LEG_FRONT_LEFT, bone).pivot(-2, 12, 0).box(-1, 0, -1, 2, 12, 2)
                .part("leg_r", null, LEG_FRONT_RIGHT, bone).pivot(2, 12, 0).box(-1, 0, -1, 2, 12, 2)
                .part("robe", null, BODY, robe).pivot(0, 12, 0).box(-5, -12, -3, 10, 12, 6).box(-6, -2, -4, 12, 8, 8).pattern("stripes")
                .part("ribs", "robe", STATIC, bone).pivot(0, 0, 0).box(-3, -11, -3.5f, 6, 5, 1)
                .part("head", "robe", HEAD, bone).pivot(0, -12, 0).box(-4, -8, -4, 8, 8, 8).pattern("eyes")
                .part("hood", "head", STATIC, robe).pivot(0, 0, 0).box(-4.5f, -9, -2, 9, 4, 7)
                .part("arm_l", "robe", ARM_LEFT, bone).pivot(-6, -11, 0).box(-1.5f, -1, -1.5f, 3, 12, 3)
                .part("arm_r", "robe", ARM_RIGHT, bone).pivot(6, -11, 0).box(-1.5f, -1, -1.5f, 3, 12, 3)
                .part("staff", "arm_r", STATIC, 0x3A2A1A).pivot(0, 10, 0).box(-0.5f, -16, -0.5f, 1, 22, 1)
                .part("staff_orb", "staff", STATIC, 0x9A4AE0).pivot(0, -16, 0).box(-1.5f, -3, -1.5f, 3, 3, 3).pattern("glow")
                .build();
    }

    static ModelSpec shaman() {
        int scales = 0x4A7A3A;
        int belly = 0xA0B070;
        return ModelSpec.builder("cox_lizardman_shaman").shadow(0.6f)
                .part("leg_l", null, LEG_FRONT_LEFT, scales).pivot(-3, 13, 1).box(-2, 0, -2, 4, 11, 5)
                .part("leg_r", null, LEG_FRONT_RIGHT, scales).pivot(3, 13, 1).box(-2, 0, -2, 4, 11, 5)
                .part("body", null, BODY, scales).pivot(0, 13, 0).box(-5, -13, -3, 10, 13, 7).pattern("scales")
                .part("belly", "body", STATIC, belly).pivot(0, 0, 0).box(-3.5f, -11, -3.5f, 7, 10, 1)
                .part("tail", "body", TAIL, scales).pivot(0, -1, 4).box(-2, -2, 0, 4, 4, 12).pattern("scales")
                .part("head", "body", HEAD, scales).pivot(0, -13, -1).box(-3.5f, -6, -5, 7, 6, 7).pattern("eyes")
                .part("snout", "head", STATIC, scales).pivot(0, 0, 0).box(-2.5f, -4, -10, 5, 3, 5)
                .part("jaw", "head", JAW, belly).pivot(0, -1, -5).box(-2.5f, 0, -5, 5, 2, 5)
                .part("frill", "head", STATIC, 0xC04A3A).pivot(0, 0, 0).box(-5, -8, 0, 10, 4, 1)
                .part("arm_l", "body", ARM_LEFT, scales).pivot(-6, -12, 0).box(-2, -1, -2, 3, 12, 4)
                .part("arm_r", "body", ARM_RIGHT, scales).pivot(6, -12, 0).box(-1, -1, -2, 3, 12, 4)
                .part("totem", "arm_r", STATIC, 0x6A4A2A).pivot(0, 10, 0).box(-0.5f, -14, -0.5f, 1, 20, 1).box(-2, -17, -2, 4, 4, 4)
                .build();
    }

    static ModelSpec spawn() {
        return ModelSpec.builder("cox_lizardman_spawn").shadow(0.3f)
                .part("blob", null, BODY, 0x9A3AC0).pivot(0, 24, 0).box(-4, -7, -4, 8, 7, 8).pattern("spots")
                .part("eyes", "blob", HEAD, 0x9A3AC0).pivot(0, -7, 0).box(-3, -3, -3, 6, 3, 6).pattern("eyes")
                .part("leg_a", "blob", LEG_EXTRA, 0x6A2A90).pivot(-3, -1, -3).box(-1, 0, -1, 2, 2, 2)
                .part("leg_b", "blob", LEG_EXTRA, 0x6A2A90).pivot(3, -1, -3).box(-1, 0, -1, 2, 2, 2)
                .part("leg_c", "blob", LEG_EXTRA, 0x6A2A90).pivot(-3, -1, 3).box(-1, 0, -1, 2, 2, 2)
                .part("leg_d", "blob", LEG_EXTRA, 0x6A2A90).pivot(3, -1, 3).box(-1, 0, -1, 2, 2, 2)
                .build();
    }

    /** Crocodile used for both Muttadiles (and scaled for Zebak by the ToA models). */
    static ModelSpec crocodile(String id, int back, int dark, int belly, float scale) {
        return ModelSpec.builder(id).scale(scale).shadow(1.0f)
                .part("body", null, BODY, back).pivot(0, 16, 0).box(-7, -6, -12, 14, 8, 24).pattern("scales")
                .part("ridge", "body", STATIC, dark).pivot(0, 0, 0).box(-3, -8, -11, 6, 2, 22).pattern("plates")
                .part("belly", "body", STATIC, belly).pivot(0, 0, 0).box(-6, 1, -11, 12, 2, 22)
                .part("neck", "body", STATIC, back).pivot(0, -2, -12).box(-5, -4, -5, 10, 7, 5)
                .part("head", "neck", HEAD, back).pivot(0, -1, -5).box(-5, -4, -8, 10, 5, 8).pattern("eyes")
                .part("snout", "head", STATIC, back).pivot(0, 0, 0).box(-3.5f, -3, -18, 7, 3, 10).pattern("scales")
                .part("teeth", "snout", STATIC, 0xF0F0E0).pivot(0, 0, 0).box(-3, 0, -17, 6, 1, 9)
                .part("jaw", "head", JAW, belly).pivot(0, 1, -6).box(-3.5f, 0, -12, 7, 2, 12)
                .part("leg_fl", "body", LEG_FRONT_LEFT, dark).pivot(-7, 0, -8).box(-3, 0, -2, 4, 8, 4)
                .part("leg_fr", "body", LEG_FRONT_RIGHT, dark).pivot(7, 0, -8).box(-1, 0, -2, 4, 8, 4)
                .part("leg_bl", "body", LEG_BACK_LEFT, dark).pivot(-7, 0, 8).box(-3, 0, -2, 4, 8, 5)
                .part("leg_br", "body", LEG_BACK_RIGHT, dark).pivot(7, 0, 8).box(-1, 0, -2, 4, 8, 5)
                .part("tail", "body", TAIL, back).pivot(0, -2, 12).box(-4, -3, 0, 8, 6, 12).pattern("scales")
                .part("tail_tip", "tail", TAIL, dark).pivot(0, 0, 12).box(-2, -2, 0, 4, 4, 12)
                .build();
    }

    /** Vanguard: a crouching armoured shell-creature; its weapon arm shows its combat style. */
    static ModelSpec vanguard(String id, int accent) {
        int shell = 0x6A6A60;
        return ModelSpec.builder(id).shadow(0.8f)
                .part("leg_l", null, LEG_FRONT_LEFT, shell).pivot(-4, 14, 0).box(-2.5f, 0, -3, 5, 10, 6)
                .part("leg_r", null, LEG_FRONT_RIGHT, shell).pivot(4, 14, 0).box(-2.5f, 0, -3, 5, 10, 6)
                .part("body", null, BODY, shell).pivot(0, 14, 0).box(-8, -14, -6, 16, 14, 12).pattern("plates")
                .part("shell", "body", STATIC, accent).pivot(0, 0, 0).box(-9, -16, 0, 18, 15, 8).pattern("plates")
                .part("spines", "shell", STATIC, 0xC8C0A0).pivot(0, 0, 0).box(-6, -19, 4, 2, 4, 2).box(-1, -20, 5, 2, 5, 2)
                .box(4, -19, 4, 2, 4, 2)
                .part("head", "body", HEAD, shell).pivot(0, -12, -6).box(-4, -4, -6, 8, 7, 7).pattern("eyes")
                .part("visor", "head", STATIC, accent).pivot(0, 0, 0).box(-4.5f, -5, -6.5f, 9, 2, 5).pattern("glow")
                .part("arm_l", "body", ARM_LEFT, shell).pivot(-9, -12, -1).box(-4, -1, -2.5f, 4, 14, 5)
                .part("arm_r", "body", ARM_RIGHT, shell).pivot(9, -12, -1).box(0, -1, -2.5f, 4, 14, 5)
                .part("weapon", "arm_r", STATIC, accent).pivot(2, 12, 0).box(-1, -1, -12, 2, 3, 14).pattern("glow")
                .build();
    }

    /** Wasp: Vespula and her vespine soldiers. */
    static ModelSpec wasp(String id, int yellow, int black, float scale) {
        return ModelSpec.builder(id).scale(scale).shadow(0.8f)
                .part("thorax", null, FLOAT, black).pivot(0, 8, 0).box(-5, -5, -5, 10, 9, 10)
                .part("abdomen", "thorax", TAIL, yellow).pivot(0, 0, 4).rotate(-20, 0, 0).box(-6, -5, 0, 12, 10, 16).pattern("stripes")
                .part("stinger", "abdomen", STATIC, black).pivot(0, 0, 16).box(-1, -1, 0, 2, 2, 6)
                .part("head", "thorax", HEAD, black).pivot(0, -1, -5).box(-4, -4, -6, 8, 7, 6).pattern("eyes")
                .part("mandibles", "head", JAW, yellow).pivot(0, 2, -6).box(-3, 0, -2, 6, 2, 2)
                .part("antennae", "head", STATIC, black).pivot(0, -4, -4).box(-3, -5, -1, 1, 5, 1).box(2, -5, -1, 1, 5, 1)
                .part("wing_l", "thorax", WING_LEFT, 0xD8E8F0).pivot(-4, -5, -1).box(-18, -1, -4, 18, 1, 9)
                .part("wing_r", "thorax", WING_RIGHT, 0xD8E8F0).pivot(4, -5, -1).box(0, -1, -4, 18, 1, 9)
                .part("leg_1", "thorax", LEG_EXTRA, black).pivot(-4, 3, -3).box(-1, 0, -1, 1, 8, 1)
                .part("leg_2", "thorax", LEG_EXTRA, black).pivot(4, 3, -3).box(0, 0, -1, 1, 8, 1)
                .part("leg_3", "thorax", LEG_EXTRA, black).pivot(-4, 3, 0).box(-1, 0, -1, 1, 9, 1)
                .part("leg_4", "thorax", LEG_EXTRA, black).pivot(4, 3, 0).box(0, 0, -1, 1, 9, 1)
                .part("leg_5", "thorax", LEG_EXTRA, black).pivot(-4, 3, 3).box(-1, 0, -1, 1, 8, 1)
                .part("leg_6", "thorax", LEG_EXTRA, black).pivot(4, 3, 3).box(0, 0, -1, 1, 8, 1)
                .build();
    }

    static ModelSpec portal() {
        return ModelSpec.builder("vespula_portal").shadow(0.8f)
                .part("frame", null, STATIC, 0x3A2A4A).pivot(0, 24, 0).box(-12, -40, -2, 3, 40, 4).box(9, -40, -2, 3, 40, 4)
                .box(-12, -43, -2, 24, 3, 4).box(-12, -2, -3, 24, 2, 6).pattern("plates")
                .part("vortex", null, SPIN, 0x8A3AE0).pivot(0, 4, 0).box(-8, -14, -1, 16, 28, 2).pattern("glow")
                .part("eye", "vortex", FLOAT, 0xF06060).pivot(0, 0, -1).box(-2, -2, -1, 4, 4, 1).pattern("glow")
                .build();
    }

    static ModelSpec iceDemon() {
        int ice = 0x8AC8E8;
        int deep = 0x3A7AB0;
        return ModelSpec.builder("cox_ice_demon").shadow(0.9f)
                .part("leg_l", null, LEG_FRONT_LEFT, deep).pivot(-4, 12, 1).box(-3, 0, -3, 6, 12, 6)
                .part("leg_r", null, LEG_FRONT_RIGHT, deep).pivot(4, 12, 1).box(-3, 0, -3, 6, 12, 6)
                .part("body", null, BODY, ice).pivot(0, 12, 0).box(-8, -18, -5, 16, 18, 10).pattern("plates")
                .part("head", "body", HEAD, ice).pivot(0, -18, -1).box(-4, -7, -5, 8, 7, 8).pattern("eyes")
                .part("horns", "head", STATIC, 0xF0F8FF).pivot(0, 0, 0).box(-7, -12, -1, 3, 6, 3).box(4, -12, -1, 3, 6, 3)
                .part("jaw", "head", JAW, deep).pivot(0, -1, -4).box(-3, 0, -2, 6, 2, 3)
                .part("arm_l", "body", ARM_LEFT, ice).pivot(-10, -16, 0).box(-4, -2, -3, 5, 18, 6)
                .part("arm_r", "body", ARM_RIGHT, ice).pivot(10, -16, 0).box(-1, -2, -3, 5, 18, 6)
                .part("claws_l", "arm_l", STATIC, 0xF0F8FF).pivot(0, 0, 0).box(-4, 16, -3, 5, 3, 6)
                .part("claws_r", "arm_r", STATIC, 0xF0F8FF).pivot(0, 0, 0).box(-1, 16, -3, 5, 3, 6)
                .part("wing_l", "body", WING_LEFT, deep).pivot(-5, -15, 5).box(-16, -2, 0, 16, 14, 1)
                .part("wing_r", "body", WING_RIGHT, deep).pivot(5, -15, 5).box(0, -2, 0, 16, 14, 1)
                .part("tail", "body", TAIL, deep).pivot(0, -2, 5).box(-1.5f, -1.5f, 0, 3, 3, 12)
                .part("icicles", "body", STATIC, 0xF0F8FF).pivot(0, 0, 0).box(-6, -21, -3, 2, 3, 2).box(4, -21, -3, 2, 3, 2)
                .build();
    }

    /** The Great Olm's head, emerging from the rock wall, crowned with crystals. */
    static ModelSpec olmHead() {
        int hide = 0xD8C890;
        int dark = 0x8A7A50;
        return ModelSpec.builder("great_olm").shadow(1.4f)
                .part("ledge", null, STATIC, 0x4A3A30).pivot(0, 24, 0).box(-18, -6, -10, 36, 6, 22).pattern("plates")
                .part("head", "ledge", HEAD, hide).pivot(0, -6, 2).box(-14, -18, -16, 28, 18, 28).pattern("eyes")
                .part("brow", "head", STATIC, dark).pivot(0, 0, 0).box(-15, -20, -17, 30, 4, 10)
                .part("snout", "head", STATIC, hide).pivot(0, 0, 0).box(-9, -11, -24, 18, 9, 8).pattern("scales")
                .part("nostrils", "snout", STATIC, dark).pivot(0, 0, 0).box(-6, -10, -25, 3, 2, 1).box(3, -10, -25, 3, 2, 1)
                .part("jaw", "head", JAW, dark).pivot(0, -2, -12).box(-10, 0, -12, 20, 4, 14)
                .part("crystals", "head", STATIC, 0x6AE0E0).pivot(0, 0, 0).box(-10, -25, -4, 4, 6, 4).box(6, -24, 0, 4, 5, 4)
                .box(-2, -27, 4, 4, 8, 4).box(-13, -22, 6, 3, 4, 3).box(10, -22, 8, 3, 4, 3).pattern("glow")
                .part("whisker_l", "snout", TENTACLE, dark).pivot(-9, -4, -20).box(-8, -1, -1, 8, 1, 1)
                .part("whisker_r", "snout", TENTACLE, dark).pivot(9, -4, -20).box(0, -1, -1, 8, 1, 1)
                .build();
    }

    /** One of the Great Olm's claws: palm, three long fingers and (left claw) a crystal growth. */
    static ModelSpec olmClaw(String id, boolean left) {
        int hide = 0xD8C890;
        int nail = 0x4A4030;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.0f)
                .part("rock", null, STATIC, 0x4A3A30).pivot(0, 24, 0).box(-12, -4, -8, 24, 4, 16).pattern("plates")
                .part("palm", null, BODY, hide).pivot(0, 20, 4).box(-9, -10, -6, 18, 10, 12).pattern("scales")
                .part("finger_1", "palm", ARM_LEFT, hide).pivot(-6, -4, -6).box(-2, -2, -12, 4, 4, 12)
                .part("nail_1", "finger_1", STATIC, nail).pivot(0, 0, -12).box(-1.5f, -1, -4, 3, 3, 4)
                .part("finger_2", "palm", ARM_RIGHT, hide).pivot(0, -4, -6).box(-2, -2, -14, 4, 4, 14)
                .part("nail_2", "finger_2", STATIC, nail).pivot(0, 0, -14).box(-1.5f, -1, -4, 3, 3, 4)
                .part("finger_3", "palm", ARM_LEFT, hide).pivot(6, -4, -6).box(-2, -2, -12, 4, 4, 12)
                .part("nail_3", "finger_3", STATIC, nail).pivot(0, 0, -12).box(-1.5f, -1, -4, 3, 3, 4)
                .part("thumb", "palm", ARM_RIGHT, hide).pivot(left ? 9 : -9, -4, -2).box(-2, -2, -8, 4, 4, 8);
        if (left) {
            b.part("crystal", "palm", SPIN, 0x6AE0E0).pivot(0, -10, 0).box(-3, -10, -3, 6, 10, 6).box(-6, -6, -1, 3, 6, 3)
                    .pattern("glow");
        } else {
            b.part("spikes", "palm", STATIC, 0x8A7A50).pivot(0, -10, 0).box(-6, -4, -2, 3, 4, 3).box(3, -5, 0, 3, 5, 3);
        }
        return b.build();
    }
}
