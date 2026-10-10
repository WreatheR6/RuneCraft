package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom models for minigame creatures and bosses. Units are model pixels (16 per block), Y grows
 * downwards and Y=24 is the ground; the front of a model faces -Z.
 */
public final class MinigameModels {
    private MinigameModels() {
    }

    /** Two-legged creature: legs, torso, head and arms. Returns the builder for extras. */
    private static ModelSpec.Builder biped(String id, float scale, int legLen, int bodyW, int bodyH, int head, int skin, int dark) {
        int bodyTop = 24 - legLen;
        return ModelSpec.builder(id).scale(scale).shadow(0.6f * scale)
                .part("leg_l", null, Role.LEG_FRONT_LEFT, dark).pivot(bodyW / 4f, bodyTop, 0).box(-2, 0, -2, 4, legLen, 4)
                .part("leg_r", null, Role.LEG_FRONT_RIGHT, dark).pivot(-bodyW / 4f, bodyTop, 0).box(-2, 0, -2, 4, legLen, 4)
                .part("body", null, Role.BODY, skin).pivot(0, bodyTop, 0).box(-bodyW / 2f, -bodyH, -3, bodyW, bodyH, 6)
                .part("head", "body", Role.HEAD, skin).pivot(0, -bodyH, 0).box(-head / 2f, -head, -head / 2f, head, head, head)
                .pattern("eyes")
                .part("arm_l", "body", Role.ARM_LEFT, skin).pivot(bodyW / 2f + 2, -bodyH + 2, 0).box(-2, -2, -2, 4, bodyH, 4)
                .part("arm_r", "body", Role.ARM_RIGHT, skin).pivot(-bodyW / 2f - 2, -bodyH + 2, 0).box(-2, -2, -2, 4, bodyH, 4);
    }

    /** Round blob on stubby legs with a gaping mouth. */
    private static ModelSpec.Builder blob(String id, float scale, int size, int skin, int dark) {
        return ModelSpec.builder(id).scale(scale).shadow(0.5f * scale)
                .part("body", null, Role.BODY, skin).pivot(0, 24 - size / 2f - 2, 0).box(-size / 2f, -size / 2f, -size / 2f, size, size, size)
                .pattern("spots")
                .part("face", "body", Role.HEAD, skin).pivot(0, -size / 4f, -size / 2f).box(-size / 3f, -size / 6f, -2, size * 2 / 3, size / 3, 2)
                .pattern("eyes")
                .part("jaw", "body", Role.JAW, dark).pivot(0, size / 6f, -size / 2f).box(-size / 3f, 0, -3, size * 2 / 3, 3, 3)
                .part("leg_fl", null, Role.LEG_FRONT_LEFT, dark).pivot(size / 3f, 22, -size / 3f).box(-1.5f, 0, -1.5f, 3, 2, 3)
                .part("leg_fr", null, Role.LEG_FRONT_RIGHT, dark).pivot(-size / 3f, 22, -size / 3f).box(-1.5f, 0, -1.5f, 3, 2, 3)
                .part("leg_bl", null, Role.LEG_BACK_LEFT, dark).pivot(size / 3f, 22, size / 3f).box(-1.5f, 0, -1.5f, 3, 2, 3)
                .part("leg_br", null, Role.LEG_BACK_RIGHT, dark).pivot(-size / 3f, 22, size / 3f).box(-1.5f, 0, -1.5f, 3, 2, 3);
    }

    /** Bat-like flyer: small body, head and two flapping wings, floating above the ground. */
    private static ModelSpec.Builder flyer(String id, float scale, int skin, int wing) {
        return ModelSpec.builder(id).scale(scale).shadow(0.4f * scale)
                .part("body", null, Role.FLOAT, skin).pivot(0, 12, 0).box(-3, -3, -4, 6, 6, 8).pattern("scales")
                .part("head", "body", Role.HEAD, skin).pivot(0, -1, -4).box(-2.5f, -2.5f, -5, 5, 5, 5).pattern("eyes")
                .part("wing_l", "body", Role.WING_LEFT, wing).pivot(3, -2, 0).box(0, -1, -4, 12, 1, 8)
                .part("wing_r", "body", Role.WING_RIGHT, wing).pivot(-3, -2, 0).box(-12, -1, -4, 12, 1, 8)
                .part("tail", "body", Role.TAIL, wing).pivot(0, 0, 4).box(-1, -1, 0, 2, 2, 6);
    }

    public static List<ModelSpec> models() {
        List<ModelSpec> list = new ArrayList<>();
        int lava = 0xFF8020;
        // ---------------- Fight Cave ----------------
        list.add(flyer("tz_kih", 0.8f, 0xB03010, 0x601808)
                .part("fangs", "head", Role.JAW, 0xF0E0C0).pivot(0, 2, -4).box(-2, 0, -1, 4, 2, 1).build());
        list.add(blob("tz_kek", 1.0f, 12, 0xA03010, 0x501008)
                .part("magma", "body", Role.STATIC, lava).pivot(0, -6, 0).box(-3, -2, -3, 6, 2, 6).pattern("glow").build());
        list.add(biped("tok_xil", 1.0f, 10, 8, 12, 7, 0x903818, 0x501808)
                .part("spikes", "body", Role.STATIC, 0x3A1A0A).pivot(0, -10, 3).box(-4, -2, 0, 8, 10, 3).pattern("plates")
                .part("spine1", "spikes", Role.STATIC, 0xE0D0B0).pivot(0, -2, 2).box(-1, -6, 0, 2, 6, 2)
                .part("spine2", "spikes", Role.STATIC, 0xE0D0B0).pivot(3, 0, 2).box(-1, -5, 0, 2, 5, 2)
                .part("spine3", "spikes", Role.STATIC, 0xE0D0B0).pivot(-3, 0, 2).box(-1, -5, 0, 2, 5, 2).build());
        list.add(biped("yt_mejkot", 1.2f, 10, 12, 12, 8, 0x8A3010, 0x4A1808)
                .part("belly", "body", Role.STATIC, lava).pivot(0, -6, -3).box(-4, -4, -1, 8, 8, 1).pattern("glow")
                .part("club_l", "arm_l", Role.STATIC, 0x3A1A0A).pivot(0, 10, 0).box(-3, 0, -3, 6, 5, 6).build());
        list.add(biped("ket_zek", 1.3f, 12, 10, 14, 8, 0x7A2810, 0x3A1408)
                .part("crest", "head", Role.STATIC, 0x2A0A04).pivot(0, -8, 0).box(-1, -6, -4, 2, 6, 8)
                .part("orb", "body", Role.SPIN, 0xFFE060).pivot(0, -22, -6).box(-2, -2, -2, 4, 4, 4).pattern("glow")
                .part("robe", "body", Role.STATIC, 0x4A1808).pivot(0, 0, 0).box(-6, -4, -4, 12, 6, 8).build());
        list.add(biped("yt_hurkot", 0.9f, 9, 8, 10, 7, 0xA04818, 0x5A2808)
                .part("glow", "body", Role.STATIC, 0xFFE070).pivot(0, -5, -3).box(-2, -2, -1, 4, 4, 1).pattern("glow").build());
        // ---------------- Inferno ----------------
        list.add(blob("jal_nib", 0.7f, 8, 0x902810, 0x501008)
                .part("teeth", "jaw", Role.STATIC, 0xF0E0C0).pivot(0, 0, -3).box(-2, -1, -1, 4, 1, 1).build());
        list.add(flyer("jal_mejrah", 1.1f, 0x6A2010, 0x3A1008)
                .part("ears", "head", Role.STATIC, 0x3A1008).pivot(0, -2, -2).box(-3, -3, 0, 6, 2, 1).build());
        list.add(blob("jal_ak", 1.3f, 14, 0x8A3818, 0x4A1808)
                .part("spot_r", "body", Role.STATIC, 0x30C040).pivot(-7, 0, 0).box(-1, -2, -2, 1, 4, 4).pattern("glow")
                .part("spot_m", "body", Role.STATIC, 0x4060E0).pivot(7, 0, 0).box(0, -2, -2, 1, 4, 4).pattern("glow")
                .part("spot_k", "body", Role.STATIC, 0xE03020).pivot(0, -7, 0).box(-2, -1, -2, 4, 1, 4).pattern("glow").build());
        list.add(blob("jal_akrek", 0.8f, 8, 0x8A3818, 0x4A1808).build());
        list.add(ModelSpec.builder("jal_imkot").scale(1.4f).shadow(0.8f)
                .part("body", null, Role.BODY, 0x7A3010).pivot(0, 14, 0).box(-6, -5, -8, 12, 10, 16).pattern("plates")
                .part("head", "body", Role.HEAD, 0x7A3010).pivot(0, -2, -8).box(-4, -4, -6, 8, 7, 6).pattern("eyes")
                .part("jaw", "head", Role.JAW, 0x4A2008).pivot(0, 3, -4).box(-3, 0, -3, 6, 2, 4)
                .part("claw_l", "body", Role.ARM_LEFT, 0x2A1008).pivot(6, 0, -6).box(0, -1, -6, 3, 3, 6)
                .part("claw_r", "body", Role.ARM_RIGHT, 0x2A1008).pivot(-6, 0, -6).box(-3, -1, -6, 3, 3, 6)
                .part("leg_fl", null, Role.LEG_FRONT_LEFT, 0x4A2008).pivot(5, 18, -5).box(-2, 0, -2, 4, 6, 4)
                .part("leg_fr", null, Role.LEG_FRONT_RIGHT, 0x4A2008).pivot(-5, 18, -5).box(-2, 0, -2, 4, 6, 4)
                .part("leg_bl", null, Role.LEG_BACK_LEFT, 0x4A2008).pivot(5, 18, 5).box(-2, 0, -2, 4, 6, 4)
                .part("leg_br", null, Role.LEG_BACK_RIGHT, 0x4A2008).pivot(-5, 18, 5).box(-2, 0, -2, 4, 6, 4)
                .part("tail", "body", Role.TAIL, 0x5A2008).pivot(0, 0, 8).box(-2, -2, 0, 4, 4, 8).build());
        list.add(biped("jal_xil", 1.3f, 14, 8, 14, 7, 0x6A2810, 0x3A1408)
                .part("bow", "arm_l", Role.STATIC, 0xFFB050).pivot(1, 10, -2).box(-1, -8, -1, 1, 16, 2).pattern("glow")
                .part("spines", "body", Role.STATIC, 0x2A0A04).pivot(0, -12, 3).box(-5, -4, 0, 10, 4, 2).build());
        list.add(biped("jal_zek", 1.5f, 14, 12, 16, 9, 0x5A2010, 0x2A1008)
                .part("horn_l", "head", Role.STATIC, 0xE0D0B0).pivot(3, -9, 0).box(0, -5, -1, 2, 5, 2)
                .part("horn_r", "head", Role.STATIC, 0xE0D0B0).pivot(-3, -9, 0).box(-2, -5, -1, 2, 5, 2)
                .part("staff", "arm_r", Role.STATIC, 0x3A1A0A).pivot(0, 12, -2).box(-1, -18, -1, 2, 22, 2)
                .part("staff_orb", "staff", Role.SPIN, 0xFFC060).pivot(0, -19, 0).box(-2, -2, -2, 4, 4, 4).pattern("glow")
                .part("robe", "body", Role.STATIC, 0x3A1008).box(-7, -4, -4, 14, 6, 8).build());
        list.add(ModelSpec.builder("jaltok_jad").scale(1.8f).shadow(1.2f)
                .part("body", null, Role.BODY, 0xC04010).pivot(0, 10, 0).box(-7, -8, -9, 14, 12, 18).pattern("plates")
                .part("neck", "body", Role.NECK, 0xC04010).pivot(0, -6, -9).box(-3, -10, -3, 6, 10, 6)
                .part("head", "neck", Role.HEAD, 0xC04010).pivot(0, -10, 0).box(-4, -5, -9, 8, 7, 10).pattern("eyes")
                .part("jaw", "head", Role.JAW, 0x802008).pivot(0, 2, -4).box(-3, 0, -5, 6, 2, 6)
                .part("mane", "neck", Role.STATIC, lava).pivot(0, -8, 3).box(-1, -2, 0, 2, 10, 4).pattern("glow")
                .part("leg_fl", null, Role.LEG_FRONT_LEFT, 0x802008).pivot(5, 14, -6).box(-2.5f, 0, -2.5f, 5, 10, 5)
                .part("leg_fr", null, Role.LEG_FRONT_RIGHT, 0x802008).pivot(-5, 14, -6).box(-2.5f, 0, -2.5f, 5, 10, 5)
                .part("leg_bl", null, Role.LEG_BACK_LEFT, 0x802008).pivot(5, 14, 6).box(-2.5f, 0, -2.5f, 5, 10, 5)
                .part("leg_br", null, Role.LEG_BACK_RIGHT, 0x802008).pivot(-5, 14, 6).box(-2.5f, 0, -2.5f, 5, 10, 5)
                .part("tail", "body", Role.TAIL, 0x802008).pivot(0, -4, 9).box(-1.5f, -1.5f, 0, 3, 3, 10).build());
        list.add(biped("jal_mejjak", 0.9f, 10, 8, 10, 7, 0x8A3010, 0x4A1808)
                .part("cowl", "head", Role.STATIC, 0x3A1008).pivot(0, -7, 0).box(-4, -2, -4, 8, 3, 8)
                .part("orb", "arm_r", Role.SPIN, lava).pivot(0, 10, -2).box(-1.5f, -1.5f, -1.5f, 3, 3, 3).pattern("glow").build());
        list.add(ModelSpec.builder("ancestral_glyph").scale(1.0f).shadow(0.8f)
                .part("slab", null, Role.FLOAT, 0x3A2A1A).pivot(0, 8, 0).box(-10, -14, -2, 20, 28, 4).pattern("plates")
                .part("rune", "slab", Role.STATIC, lava).pivot(0, 0, -2).box(-4, -6, -1, 8, 12, 1).pattern("glow").build());
        list.add(ModelSpec.builder("tzkal_zuk").scale(2.6f).shadow(1.6f)
                .part("base", null, Role.STATIC, 0x2A1008).pivot(0, 20, 0).box(-10, -4, -6, 20, 8, 12).pattern("plates")
                .part("body", "base", Role.BODY, 0xC04010).pivot(0, -4, 0).box(-8, -16, -5, 16, 16, 10).pattern("plates")
                .part("chest", "body", Role.STATIC, lava).pivot(0, -10, -5).box(-4, -4, -1, 8, 8, 1).pattern("glow")
                .part("head", "body", Role.HEAD, 0xC04010).pivot(0, -16, 0).box(-5, -9, -5, 10, 9, 10).pattern("eyes")
                .part("crown", "head", Role.STATIC, 0x3A1008).pivot(0, -9, 0).box(-6, -4, -6, 12, 4, 12)
                .part("crown_spike", "crown", Role.STATIC, lava).pivot(0, -4, 0).box(-1, -5, -1, 2, 5, 2).pattern("glow")
                .part("jaw", "head", Role.JAW, 0x802008).pivot(0, -1, -4).box(-4, 0, -2, 8, 3, 3)
                .part("arm_l", "body", Role.ARM_LEFT, 0xA03010).pivot(10, -14, 0).box(-2, -2, -3, 5, 16, 6)
                .part("arm_r", "body", Role.ARM_RIGHT, 0xA03010).pivot(-10, -14, 0).box(-3, -2, -3, 5, 16, 6)
                .part("fist_l", "arm_l", Role.STATIC, 0x3A1008).pivot(0, 14, 0).box(-3, 0, -4, 7, 6, 8)
                .part("fist_r", "arm_r", Role.STATIC, 0x3A1008).pivot(0, 14, 0).box(-4, 0, -4, 7, 6, 8).build());
        // ---------------- Pest Control ----------------
        list.add(ModelSpec.builder("pest_portal").scale(1.2f).shadow(0.9f)
                .part("frame", null, Role.STATIC, 0x4A4A4A).pivot(0, 24, 0).box(-8, -32, -2, 2, 32, 4).box(6, -32, -2, 2, 32, 4)
                .box(-8, -34, -2, 16, 2, 4)
                .part("vortex", null, Role.SPIN, 0xC060E0).pivot(0, 8, 0).box(-6, -14, -1, 12, 28, 2).pattern("glow")
                .part("core", "vortex", Role.FLOAT, 0xFFFFFF).pivot(0, 0, -1).box(-2, -2, -1, 4, 4, 1).pattern("glow").build());
        list.add(biped("brawler", 1.3f, 8, 12, 12, 7, 0x6A7A4A, 0x3A4A1A)
                .part("hump", "body", Role.STATIC, 0x4A5A2A).pivot(0, -12, 3).box(-5, -2, -1, 10, 6, 4).pattern("spots")
                .part("fist_l", "arm_l", Role.STATIC, 0x4A5A2A).pivot(0, 10, 0).box(-3, 0, -3, 6, 4, 6)
                .part("fist_r", "arm_r", Role.STATIC, 0x4A5A2A).pivot(0, 10, 0).box(-3, 0, -3, 6, 4, 6).build());
        list.add(biped("defiler", 1.0f, 8, 6, 8, 6, 0x7A6A3A, 0x3A2A1A)
                .part("tail", "body", Role.TAIL, 0x5A4A2A).pivot(0, -1, 3).box(-1, -1, 0, 2, 2, 8).build());
        list.add(ModelSpec.builder("ravager").scale(1.1f).shadow(0.6f)
                .part("body", null, Role.BODY, 0x5A4A6A).pivot(0, 14, 0).box(-5, -5, -6, 10, 9, 12).pattern("plates")
                .part("head", "body", Role.HEAD, 0x5A4A6A).pivot(0, -2, -6).box(-3, -3, -5, 6, 6, 5).pattern("eyes")
                .part("horn", "head", Role.STATIC, 0xE0D0C0).pivot(0, -3, -5).box(-1, -4, -1, 2, 4, 2)
                .part("arm_l", "body", Role.ARM_LEFT, 0x3A2A4A).pivot(5, -2, -3).box(0, -1, -1, 2, 10, 2)
                .part("arm_r", "body", Role.ARM_RIGHT, 0x3A2A4A).pivot(-5, -2, -3).box(-2, -1, -1, 2, 10, 2)
                .part("leg_l", null, Role.LEG_FRONT_LEFT, 0x3A2A4A).pivot(3, 18, 3).box(-1.5f, 0, -1.5f, 3, 6, 3)
                .part("leg_r", null, Role.LEG_FRONT_RIGHT, 0x3A2A4A).pivot(-3, 18, 3).box(-1.5f, 0, -1.5f, 3, 6, 3).build());
        list.add(ModelSpec.builder("shifter").scale(1.0f).shadow(0.5f)
                .part("body", null, Role.BODY, 0x4A6A6A).pivot(0, 14, 0).box(-3, -3, -5, 6, 6, 10).pattern("stripes")
                .part("head", "body", Role.HEAD, 0x4A6A6A).pivot(0, -2, -5).box(-2.5f, -2.5f, -5, 5, 5, 5).pattern("eyes")
                .part("leg_fl", null, Role.LEG_FRONT_LEFT, 0x2A4A4A).pivot(2, 17, -3).box(-1, 0, -1, 2, 7, 2)
                .part("leg_fr", null, Role.LEG_FRONT_RIGHT, 0x2A4A4A).pivot(-2, 17, -3).box(-1, 0, -1, 2, 7, 2)
                .part("leg_bl", null, Role.LEG_BACK_LEFT, 0x2A4A4A).pivot(2, 17, 3).box(-1, 0, -1, 2, 7, 2)
                .part("leg_br", null, Role.LEG_BACK_RIGHT, 0x2A4A4A).pivot(-2, 17, 3).box(-1, 0, -1, 2, 7, 2)
                .part("tail", "body", Role.TAIL, 0x2A4A4A).pivot(0, -1, 5).box(-1, -1, 0, 2, 2, 7).build());
        ModelSpec.Builder spinner = ModelSpec.builder("spinner").scale(1.0f).shadow(0.5f)
                .part("body", null, Role.BODY, 0x8A6A3A).pivot(0, 16, 0).box(-4, -4, -4, 8, 6, 8).pattern("spots")
                .part("head", "body", Role.HEAD, 0x8A6A3A).pivot(0, -1, -4).box(-2.5f, -2, -4, 5, 4, 4).pattern("eyes");
        for (int i = 0; i < 6; i++) {
            float side = i % 2 == 0 ? 1 : -1;
            float z = -3 + (i / 2) * 3;
            spinner.part("leg" + i, null, Role.LEG_EXTRA, 0x4A3A1A).pivot(side * 4, 17, z)
                    .rotate(0, 0, side * 40).box(side > 0 ? 0 : -8, -1, -1, 8, 2, 2);
        }
        list.add(spinner.build());
        list.add(blob("splatter", 0.9f, 10, 0x9A4A6A, 0x5A1A3A)
                .part("boils", "body", Role.STATIC, 0xE080A0).pivot(0, -5, 0).box(-3, -2, -3, 6, 2, 6).pattern("spots").build());
        list.add(flyer("torcher", 0.9f, 0xC06030, 0x602010)
                .part("flame", "body", Role.STATIC, 0xFFC060).pivot(0, -3, 0).box(-2, -4, -2, 4, 4, 4).pattern("glow").build());
        // ---------------- Barbarian Assault ----------------
        list.add(biped("penance_fighter", 1.0f, 8, 8, 10, 7, 0x6A5A3A, 0x2A2A1A)
                .part("claws", "arm_r", Role.STATIC, 0xE0D0B0).pivot(0, 10, -1).box(-2, 0, -2, 4, 3, 1).build());
        list.add(biped("penance_ranger", 1.0f, 8, 8, 10, 7, 0x5A6A3A, 0x2A3A1A)
                .part("sac", "body", Role.STATIC, 0x80A040).pivot(0, -6, 3).box(-3, -3, 0, 6, 6, 3).pattern("spots").build());
        list.add(biped("penance_runner", 0.85f, 10, 6, 8, 6, 0x5A5A6A, 0x2A2A3A)
                .part("tail", "body", Role.TAIL, 0x3A3A4A).pivot(0, -1, 3).box(-1, -1, 0, 2, 2, 7).build());
        list.add(biped("penance_healer", 0.9f, 8, 7, 9, 7, 0x6A5A5A, 0x2A2A2A)
                .part("cross", "body", Role.STATIC, 0xE0A0A0).pivot(0, -5, -3).box(-2, -2, -1, 4, 4, 1).pattern("glow").build());
        ModelSpec.Builder queen = ModelSpec.builder("penance_queen").scale(2.0f).shadow(1.4f)
                .part("body", null, Role.BODY, 0x7A6A3A).pivot(0, 14, 0).box(-7, -6, -8, 14, 10, 16).pattern("plates")
                .part("abdomen", "body", Role.TAIL, 0x5A4A2A).pivot(0, -2, 8).box(-6, -5, 0, 12, 9, 10).pattern("stripes")
                .part("head", "body", Role.HEAD, 0x7A6A3A).pivot(0, -3, -8).box(-4, -4, -6, 8, 7, 6).pattern("eyes")
                .part("jaw", "head", Role.JAW, 0x3A2A1A).pivot(0, 2, -5).box(-3, 0, -2, 6, 2, 3)
                .part("crown", "head", Role.STATIC, 0xE0C060).pivot(0, -4, -2).box(-3, -3, -1, 6, 3, 2).pattern("glow");
        for (int i = 0; i < 6; i++) {
            float side = i % 2 == 0 ? 1 : -1;
            float z = -5 + (i / 2) * 5;
            queen.part("leg" + i, null, Role.LEG_EXTRA, 0x3A2A1A).pivot(side * 7, 16, z)
                    .rotate(0, 0, side * 35).box(side > 0 ? 0 : -10, -1, -1, 10, 2, 2);
        }
        list.add(queen.build());
        // ---------------- Wintertodt: a towering spirit of cold ----------------
        list.add(ModelSpec.builder("wintertodt").scale(2.5f).shadow(1.5f)
                .part("mist", null, Role.SPIN, 0xD0E8FF).pivot(0, 22, 0).box(-12, -2, -12, 24, 2, 24).pattern("glow")
                .part("body", null, Role.FLOAT, 0xA0C8F0).pivot(0, 8, 0).box(-7, -12, -5, 14, 22, 10).pattern("scales")
                .part("core", "body", Role.STATIC, 0xFFFFFF).pivot(0, -4, -5).box(-3, -3, -1, 6, 6, 1).pattern("glow")
                .part("head", "body", Role.HEAD, 0xC0E0FF).pivot(0, -12, 0).box(-5, -9, -5, 10, 9, 10).pattern("eyes")
                .part("spike_l", "head", Role.STATIC, 0xE0F0FF).pivot(4, -8, 0).box(0, -8, -1, 2, 8, 2)
                .part("spike_m", "head", Role.STATIC, 0xE0F0FF).pivot(0, -9, 0).box(-1, -10, -1, 2, 10, 2)
                .part("spike_r", "head", Role.STATIC, 0xE0F0FF).pivot(-4, -8, 0).box(-2, -8, -1, 2, 8, 2)
                .part("arm_l", "body", Role.TENTACLE, 0x80A8E0).pivot(8, -10, 0).box(0, -2, -2, 4, 18, 4)
                .part("arm_r", "body", Role.TENTACLE, 0x80A8E0).pivot(-8, -10, 0).box(-4, -2, -2, 4, 18, 4)
                .part("claw_l", "arm_l", Role.STATIC, 0xE0F0FF).pivot(2, 16, 0).box(-2, 0, -3, 4, 5, 6)
                .part("claw_r", "arm_r", Role.STATIC, 0xE0F0FF).pivot(-2, 16, 0).box(-2, 0, -3, 4, 5, 6).build());
        // ---------------- Tempoross: a storm spirit of sea and lightning ----------------
        ModelSpec.Builder temp = ModelSpec.builder("tempoross").scale(2.5f).shadow(1.6f)
                .part("wave", null, Role.SPIN, 0x2A5A8A).pivot(0, 22, 0).box(-14, -2, -14, 28, 2, 28).pattern("stripes")
                .part("body", null, Role.FLOAT, 0x3A6A9A).pivot(0, 6, 0).box(-8, -12, -6, 16, 20, 12).pattern("scales")
                .part("head", "body", Role.HEAD, 0x4A7AAA).pivot(0, -12, 0).box(-6, -8, -7, 12, 8, 12).pattern("eyes")
                .part("jaw", "head", Role.JAW, 0x1A3A5A).pivot(0, 0, -5).box(-5, 0, -3, 10, 4, 5)
                .part("storm", "head", Role.SPIN, 0xE0F0FF).pivot(0, -10, 0).box(-7, -2, -7, 14, 2, 14).pattern("glow");
        for (int i = 0; i < 4; i++) {
            float side = i % 2 == 0 ? 1 : -1;
            temp.part("tentacle" + i, "body", Role.TENTACLE, 0x2A5A8A).pivot(side * 8, -6 + (i / 2) * 8, 0)
                    .rotate(0, 0, side * 20).box(side > 0 ? 0 : -14, -2, -2, 14, 4, 4);
        }
        list.add(temp.build());
        list.add(ModelSpec.builder("great_guardian").scale(1.8f).shadow(1.0f)
                .part("pedestal", null, Role.STATIC, 0x3A3A4A).pivot(0, 24, 0).box(-8, -4, -8, 16, 4, 16).pattern("plates")
                .part("body", null, Role.FLOAT, 0x5A4AA0).pivot(0, 8, 0).box(-6, -12, -4, 12, 16, 8).pattern("plates")
                .part("head", "body", Role.HEAD, 0x6A5AB0).pivot(0, -12, 0).box(-4, -8, -4, 8, 8, 8).pattern("eyes")
                .part("eye", "head", Role.STATIC, 0xFFE0FF).pivot(0, -5, -4).box(-1.5f, -1.5f, -1, 3, 3, 1).pattern("glow")
                .part("arm_l", "body", Role.ARM_LEFT, 0x3A2A80).pivot(8, -10, 0).box(-2, -2, -2, 4, 14, 4)
                .part("arm_r", "body", Role.ARM_RIGHT, 0x3A2A80).pivot(-8, -10, 0).box(-2, -2, -2, 4, 14, 4)
                .part("ring", "body", Role.SPIN, 0xC0A0FF).pivot(0, -4, 0).box(-9, -1, -9, 18, 1, 18).pattern("glow").build());
        // ---------------- Warriors' Guild cyclops ----------------
        list.add(biped("cyclops", 1.3f, 10, 10, 12, 9, 0xA08060, 0x5A3A2A)
                .part("eye", "head", Role.STATIC, 0xF0F0F0).pivot(0, -5, -4.5f).box(-2, -2, -1, 4, 4, 1).pattern("glow")
                .part("loincloth", "body", Role.STATIC, 0x6A4A2A).box(-6, -2, -4, 12, 4, 8)
                .part("club", "arm_r", Role.STATIC, 0x5A3A1A).pivot(0, 10, -2).box(-2, -2, -10, 4, 4, 12).build());
        // ---------------- Mort'ton shades ----------------
        list.add(ModelSpec.builder("shade").scale(1.0f).shadow(0.4f)
                .part("robe", null, Role.FLOAT, 0x1A1A2A).pivot(0, 12, 0).box(-5, -10, -3, 10, 20, 6).pattern("stripes")
                .part("hood", "robe", Role.HEAD, 0x10101A).pivot(0, -10, 0).box(-4, -8, -4, 8, 8, 8).pattern("eyes")
                .part("arm_l", "robe", Role.TENTACLE, 0x1A1A2A).pivot(5, -8, 0).box(0, -1, -1, 2, 10, 2)
                .part("arm_r", "robe", Role.TENTACLE, 0x1A1A2A).pivot(-5, -8, 0).box(-2, -1, -1, 2, 10, 2)
                .part("wisp", "robe", Role.SPIN, 0x8080FF).pivot(0, 10, 0).box(-3, 0, -3, 6, 2, 6).pattern("glow").build());
        return list;
    }
}
