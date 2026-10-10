package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

/**
 * Custom models for the non-humanoid quest bosses. Each model id equals its BossDef id, so the
 * renderer picks it up automatically. Model pixels: 16 per block, Y=24 is the ground.
 */
final class QuestModels {
    private QuestModels() {
    }

    static void register(QuestBook b) {
        // Dragons
        b.model(dragon("elvarg", 0x4A5A26, 0x8A8A4A, 0x3A4A1E, 1.9f));
        b.model(dragon("galvek", 0x2A1418, 0xC0502A, 0x5A1A1A, 2.4f));
        b.model(dragon("ds2_vorkath", 0x5A6A4A, 0x8A9A7A, 0x3A4A2A, 1.6f));
        // Dagannoth
        b.model(dagannoth("dagannoth_mother", 0x7A7A4A, 2.2f));
        b.model(dagannoth("gelatinnoth_mother", 0x8AD08A, 2.0f));
        // Demons
        b.model(demon("jungle_demon", 0x3A6A2A, 0x1A3A12, 2.0f));
        b.model(demon("agrith_na_na", 0xB07A3A, 0x6A4A1A, 1.7f));
        b.model(demon("nezikchened", 0x8A1A1A, 0x4A0A0A, 1.8f));
        b.model(demon("chronozon", 0x6A2A7A, 0x3A1048, 1.7f));
        b.model(demon("glough_black_demon", 0x262626, 0x101010, 1.8f));
        b.model(demon("balance_elemental", 0x9A8A5A, 0x3A6A8A, 1.8f));
        // Beasts
        b.model(spider("kalrag", 0x4A3A2A, 1.8f));
        b.model(hound("bouncer", 0x5A2A1A, 0xC03A1A, 1.5f));
        b.model(hound("temple_guardian", 0x6A5A4A, 0xFFD040, 1.4f));
        b.model(hound("quest_skeleton_hellhound", 0xD8D0C0, 0x8A2A2A, 1.4f));
        b.model(hound("witchs_experiment", 0x8A5A6A, 0xE04040, 1.4f));
        b.model(treeSpirit("tree_spirit", 0x5A4A2A, 0x3A7A2A, 1.6f));
        b.model(ape("kruk", 0x5A3A26, 0x8A6A4A, 2.0f));
        b.model(troll("dad", 0x6A7A5A, 0x4A3A2A, 2.0f));
        b.model(troll("draugen", 0xA8C0C0, 0x5A6A6A, 1.6f));
        b.model(troll("troll_general", 0x5A6A4A, 0x6A2A1A, 1.8f));
        b.model(slug("slug_prince", 0x8A9A5A, 1.6f));
        b.model(golem("ancient_guardian", 0x7A7A8A, 0x40C0FF, 1.8f));
        // Vampyres (humanoid but winged, cloaked and taller)
        b.model(vampyre("vanstrom_klause", 0xC8C8D0, 0x2A1A3A, 1.25f));
        b.model(vampyre("ranis_drakan", 0xD0C8C8, 0x5A0A12, 1.2f));
        b.model(vampyre("dessous", 0xA8B0B8, 0x1A1A1A, 1.2f));
        b.model(hound("plague_sheep", 0xC8D0A0, 0x8AA060, 1.1f));
        b.model(slug("sea_slug", 0x8A9A5B, 1.0f));
        b.model(slug("evil_creature", 0x6A2A6A, 0.9f));
        b.model(bird("chompy_bird", 0x8A9A5C, 0xE0A040, 1.6f));
        b.model(troll("slash_bash", 0x7A8A6A, 0x3A4A2A, 1.7f));
        b.model(golem("the_inadequacy", 0x2A2A3A, 0x8A2AA0, 2.0f));
        b.model(demon("agrith_naar", 0x9A4A2A, 0x5A2A1A, 1.7f));
        b.model(treeSpirit("tanglefoot", 0x3A5A2A, 0x6AA03A, 1.7f));
        b.model(demon("gorak", 0x6A3A6B, 0x3A1A3A, 1.5f));
        b.model(bird("warped_terrorbird", 0x6A5A8A, 0xA040C0, 2.2f));
        b.model(troll("ice_troll_king", 0x8AA0C0, 0x3A4A6A, 2.0f));
        b.model(slug("giant_sea_snake", 0x2A6A5A, 2.0f));
        b.model(spider("giant_scarab", 0x3A3A1A, 1.9f));
        b.model(spider("champion_of_scabaras", 0x6A5A1A, 2.3f));
        b.model(troll("sea_troll_queen", 0x3A6A6A, 0x1A3A4A, 2.2f));
        b.model(hound("rat_king", 0x6A5A4A, 0xE04040, 1.3f));
        b.model(demon("judge_of_yama", 0x3A1A4A, 0x1A0A2A, 1.9f));
        b.model(bird("entranan_firebird", 0xFF6A1A, 0xFFD040, 1.2f));
        // Crystal
        b.model(crystal("fragment_of_seren", 0xB8F0FF, 0x80C0FF, 1.3f));
    }

    static ModelSpec dragon(String id, int body, int belly, int wing, float scale) {
        return ModelSpec.builder(id).scale(scale).shadow(1.6f)
                .part("body", null, Role.BODY, body, belly).pivot(0, 10, 0).box(-8, -6, -12, 16, 12, 24).pattern("scales")
                .part("spines", "body", Role.STATIC, belly).pivot(0, -6, 0).box(-1, -3, -10, 2, 3, 20)
                .part("neck", "body", Role.NECK, body).pivot(0, -3, -12).rotate(-25, 0, 0).box(-3, -3, -10, 6, 6, 10).pattern("scales")
                .part("head", "neck", Role.HEAD, body).pivot(0, 0, -10).box(-4, -4, -10, 8, 7, 10).pattern("eyes")
                .part("jaw", "head", Role.JAW, belly).pivot(0, 3, -6).box(-3, 0, -5, 6, 2, 6)
                .part("horn_l", "head", Role.STATIC, belly).pivot(2, -4, -1).rotate(30, 0, 0).box(0, -5, 0, 2, 5, 2)
                .part("horn_r", "head", Role.STATIC, belly).pivot(-4, -4, -1).rotate(30, 0, 0).box(0, -5, 0, 2, 5, 2)
                .part("wing_l", "body", Role.WING_LEFT, wing).pivot(8, -5, -4).box(0, -1, -8, 22, 1, 16)
                .part("wing_r", "body", Role.WING_RIGHT, wing).pivot(-8, -5, -4).box(-22, -1, -8, 22, 1, 16)
                .part("tail", "body", Role.TAIL, body).pivot(0, -2, 12).box(-2, -2, 0, 4, 4, 12).pattern("scales")
                .part("tail_tip", "tail", Role.TAIL, belly).pivot(0, 0, 12).box(-1, -1, 0, 2, 2, 10)
                .part("leg_fl", null, Role.LEG_FRONT_LEFT, body).pivot(5, 14, -8).box(-2, 0, -2, 4, 10, 4)
                .part("leg_fr", null, Role.LEG_FRONT_RIGHT, body).pivot(-5, 14, -8).box(-2, 0, -2, 4, 10, 4)
                .part("leg_bl", null, Role.LEG_BACK_LEFT, body).pivot(5, 14, 8).box(-2, 0, -2, 5, 10, 5)
                .part("leg_br", null, Role.LEG_BACK_RIGHT, body).pivot(-5, 14, 8).box(-3, 0, -2, 5, 10, 5)
                .build();
    }

    static ModelSpec dagannoth(String id, int color, float scale) {
        return ModelSpec.builder(id).scale(scale).shadow(1.0f)
                .part("body", null, Role.BODY, color).pivot(0, 16, 0).rotate(10, 0, 0).box(-6, -16, -5, 12, 16, 10).pattern("plates")
                .part("head", "body", Role.HEAD, color).pivot(0, -16, -2).box(-4, -6, -9, 8, 6, 10).pattern("eyes")
                .part("jaw", "head", Role.JAW, color).pivot(0, 0, -4).box(-3, 0, -5, 6, 2, 6)
                .part("crest", "head", Role.STATIC, color).pivot(0, -6, 0).box(-1, -4, -6, 2, 4, 8)
                .part("arm_l", "body", Role.ARM_LEFT, color).pivot(6, -14, -1).box(0, -1, -2, 3, 12, 3)
                .part("arm_r", "body", Role.ARM_RIGHT, color).pivot(-6, -14, -1).box(-3, -1, -2, 3, 12, 3)
                .part("fin", "body", Role.FIN, color).pivot(0, -14, 5).box(-1, -2, 0, 2, 12, 5)
                .part("tail", "body", Role.TAIL, color).pivot(0, -2, 5).box(-2, -2, 0, 4, 4, 10)
                .part("leg_l", null, Role.LEG_BACK_LEFT, color).pivot(4, 16, 0).box(-2, 0, -2, 4, 8, 4)
                .part("leg_r", null, Role.LEG_BACK_RIGHT, color).pivot(-4, 16, 0).box(-2, 0, -2, 4, 8, 4)
                .build();
    }

    static ModelSpec demon(String id, int skin, int dark, float scale) {
        return ModelSpec.builder(id).scale(scale).shadow(0.9f)
                .part("body", null, Role.BODY, skin, dark).pivot(0, 12, 0).box(-5, -12, -3, 10, 12, 6)
                .part("head", "body", Role.HEAD, skin).pivot(0, -12, 0).box(-4, -8, -4, 8, 8, 8).pattern("eyes")
                .part("jaw", "head", Role.JAW, dark).pivot(0, -1, -3).box(-3, 0, -2, 6, 2, 3)
                .part("horn_l", "head", Role.STATIC, 0xE0D8B0).pivot(3, -8, 0).rotate(0, 0, -25).box(0, -5, -1, 2, 5, 2)
                .part("horn_r", "head", Role.STATIC, 0xE0D8B0).pivot(-5, -8, 0).rotate(0, 0, 25).box(0, -5, -1, 2, 5, 2)
                .part("arm_l", "body", Role.ARM_LEFT, skin).pivot(6, -11, 0).box(-1, -1, -2, 4, 13, 4)
                .part("arm_r", "body", Role.ARM_RIGHT, skin).pivot(-6, -11, 0).box(-3, -1, -2, 4, 13, 4)
                .part("wing_l", "body", Role.WING_LEFT, dark).pivot(3, -9, 3).box(0, -6, 0, 14, 12, 1)
                .part("wing_r", "body", Role.WING_RIGHT, dark).pivot(-3, -9, 3).box(-14, -6, 0, 14, 12, 1)
                .part("tail", "body", Role.TAIL, dark).pivot(0, -2, 3).box(-1, -1, 0, 2, 2, 12)
                .part("leg_l", null, Role.LEG_BACK_LEFT, dark).pivot(3, 12, 0).box(-2, 0, -2, 4, 12, 4)
                .part("leg_r", null, Role.LEG_BACK_RIGHT, dark).pivot(-3, 12, 0).box(-2, 0, -2, 4, 12, 4)
                .build();
    }

    static ModelSpec spider(String id, int color, float scale) {
        ModelSpec.Builder m = ModelSpec.builder(id).scale(scale).shadow(1.0f)
                .part("abdomen", null, Role.BODY, color).pivot(0, 15, 2).box(-6, -5, 0, 12, 9, 12).pattern("spots")
                .part("thorax", null, Role.STATIC, color).pivot(0, 16, 0).box(-4, -3, -6, 8, 6, 6)
                .part("head", "thorax", Role.HEAD, color).pivot(0, 0, -6).box(-3, -3, -5, 6, 5, 5).pattern("eyes")
                .part("jaw", "head", Role.JAW, 0x2A1A10).pivot(0, 2, -5).box(-2, 0, -2, 4, 2, 2);
        for (int i = 0; i < 4; i++) {
            float z = -5 + i * 2.5f;
            m.part("leg_l" + i, null, Role.LEG_EXTRA, color).pivot(4, 17, z).rotate(0, -20 + i * 13, 35).box(0, -1, -1, 13, 2, 2);
            m.part("leg_r" + i, null, Role.LEG_EXTRA, color).pivot(-4, 17, z).rotate(0, 20 - i * 13, -35).box(-13, -1, -1, 13, 2, 2);
        }
        return m.build();
    }

    static ModelSpec hound(String id, int color, int eyes, float scale) {
        return ModelSpec.builder(id).scale(scale).shadow(0.8f)
                .part("body", null, Role.BODY, color).pivot(0, 11, 0).box(-4, -4, -8, 8, 8, 16)
                .part("head", "body", Role.HEAD, color, eyes).pivot(0, -3, -8).box(-3, -4, -6, 6, 6, 6).pattern("eyes")
                .part("jaw", "head", Role.JAW, color).pivot(0, 1, -5).box(-2, 0, -4, 4, 2, 4)
                .part("ear_l", "head", Role.STATIC, color).pivot(1, -4, -2).box(0, -3, 0, 2, 3, 1)
                .part("ear_r", "head", Role.STATIC, color).pivot(-3, -4, -2).box(0, -3, 0, 2, 3, 1)
                .part("tail", "body", Role.TAIL, color).pivot(0, -3, 8).rotate(-30, 0, 0).box(-1, -1, 0, 2, 2, 9)
                .part("leg_fl", null, Role.LEG_FRONT_LEFT, color).pivot(3, 15, -6).box(-1, 0, -1, 3, 9, 3)
                .part("leg_fr", null, Role.LEG_FRONT_RIGHT, color).pivot(-3, 15, -6).box(-2, 0, -1, 3, 9, 3)
                .part("leg_bl", null, Role.LEG_BACK_LEFT, color).pivot(3, 15, 6).box(-1, 0, -1, 3, 9, 3)
                .part("leg_br", null, Role.LEG_BACK_RIGHT, color).pivot(-3, 15, 6).box(-2, 0, -1, 3, 9, 3)
                .build();
    }

    static ModelSpec bird(String id, int body, int accent, float scale) {
        return ModelSpec.builder(id).scale(scale).shadow(0.4f)
                .part("body", null, Role.BODY, body).pivot(0, 16, 0).box(-3, -3, -4, 6, 6, 8)
                .part("head", "body", Role.HEAD, body).pivot(0, -3, -4).box(-2, -4, -3, 4, 4, 4).pattern("eyes")
                .part("beak", "head", Role.JAW, accent).pivot(0, -2, -3).box(-1, 0, -2, 2, 1, 2)
                .part("crest", "head", Role.STATIC, accent).pivot(0, -4, -1).box(0, -3, -1, 1, 3, 3).pattern("glow")
                .part("wing_l", "body", Role.WING_LEFT, accent).pivot(3, -2, 0).box(0, 0, -3, 7, 1, 6).pattern("glow")
                .part("wing_r", "body", Role.WING_RIGHT, accent).pivot(-3, -2, 0).box(-7, 0, -3, 7, 1, 6).pattern("glow")
                .part("tail", "body", Role.TAIL, accent).pivot(0, -1, 4).box(-2, -1, 0, 4, 1, 6).pattern("glow")
                .part("leg_l", null, Role.LEG_BACK_LEFT, accent).pivot(1, 19, 0).box(-1, 0, -1, 1, 5, 1)
                .part("leg_r", null, Role.LEG_BACK_RIGHT, accent).pivot(-1, 19, 0).box(0, 0, -1, 1, 5, 1)
                .build();
    }

    static ModelSpec treeSpirit(String id, int bark, int leaves, float scale) {
        return ModelSpec.builder(id).scale(scale).shadow(0.9f)
                .part("trunk", null, Role.BODY, bark).pivot(0, 16, 0).box(-4, -18, -3, 8, 18, 6)
                .part("head", "trunk", Role.HEAD, bark).pivot(0, -18, 0).box(-4, -7, -4, 8, 7, 8).pattern("eyes")
                .part("canopy", "head", Role.STATIC, leaves).pivot(0, -7, 0).box(-8, -8, -8, 16, 8, 16).pattern("spots")
                .part("branch_l", "trunk", Role.ARM_LEFT, bark).pivot(4, -15, 0).rotate(0, 0, -20).box(0, -1, -1, 3, 14, 3)
                .part("branch_r", "trunk", Role.ARM_RIGHT, bark).pivot(-4, -15, 0).rotate(0, 0, 20).box(-3, -1, -1, 3, 14, 3)
                .part("root_l", null, Role.LEG_BACK_LEFT, bark).pivot(3, 16, 0).box(-2, 0, -2, 4, 8, 4)
                .part("root_r", null, Role.LEG_BACK_RIGHT, bark).pivot(-3, 16, 0).box(-2, 0, -2, 4, 8, 4)
                .build();
    }

    static ModelSpec ape(String id, int fur, int face, float scale) {
        return ModelSpec.builder(id).scale(scale).shadow(0.9f)
                .part("body", null, Role.BODY, fur).pivot(0, 13, 0).rotate(20, 0, 0).box(-6, -12, -4, 12, 12, 8)
                .part("head", "body", Role.HEAD, face).pivot(0, -12, -2).box(-4, -7, -5, 8, 7, 7).pattern("eyes")
                .part("jaw", "head", Role.JAW, face).pivot(0, -1, -4).box(-3, 0, -2, 6, 2, 3)
                .part("arm_l", "body", Role.ARM_LEFT, fur).pivot(7, -11, 0).box(-1, -1, -2, 4, 17, 4)
                .part("arm_r", "body", Role.ARM_RIGHT, fur).pivot(-7, -11, 0).box(-3, -1, -2, 4, 17, 4)
                .part("leg_l", null, Role.LEG_BACK_LEFT, fur).pivot(3, 15, 1).box(-2, 0, -2, 4, 9, 4)
                .part("leg_r", null, Role.LEG_BACK_RIGHT, fur).pivot(-3, 15, 1).box(-2, 0, -2, 4, 9, 4)
                .build();
    }

    static ModelSpec troll(String id, int skin, int cloth, float scale) {
        return ModelSpec.builder(id).scale(scale).shadow(1.0f)
                .part("body", null, Role.BODY, skin).pivot(0, 12, 0).box(-6, -13, -4, 12, 13, 8)
                .part("loincloth", "body", Role.STATIC, cloth).pivot(0, 0, 0).box(-6, -2, -4, 12, 4, 8, 0.2f)
                .part("head", "body", Role.HEAD, skin).pivot(0, -13, -1).box(-4, -7, -5, 8, 7, 8).pattern("eyes")
                .part("nose", "head", Role.STATIC, skin).pivot(0, -3, -5).box(-1, -1, -3, 2, 3, 3)
                .part("jaw", "head", Role.JAW, skin).pivot(0, -1, -4).box(-3, 0, -2, 6, 2, 3)
                .part("arm_l", "body", Role.ARM_LEFT, skin).pivot(7, -12, 0).box(-1, -1, -2, 5, 15, 5)
                .part("arm_r", "body", Role.ARM_RIGHT, skin).pivot(-7, -12, 0).box(-4, -1, -2, 5, 15, 5)
                .part("leg_l", null, Role.LEG_BACK_LEFT, skin).pivot(3, 12, 0).box(-2, 0, -2, 5, 12, 5)
                .part("leg_r", null, Role.LEG_BACK_RIGHT, skin).pivot(-3, 12, 0).box(-3, 0, -2, 5, 12, 5)
                .build();
    }

    static ModelSpec slug(String id, int color, float scale) {
        return ModelSpec.builder(id).scale(scale).shadow(1.0f)
                .part("body", null, Role.BODY, color).pivot(0, 19, 0).box(-5, -5, -10, 10, 10, 22).pattern("spots")
                .part("head", "body", Role.HEAD, color).pivot(0, -3, -10).box(-4, -10, -6, 8, 12, 6).pattern("eyes")
                .part("stalk_l", "head", Role.TENTACLE, color).pivot(2, -10, -3).box(-1, -6, -1, 2, 6, 2)
                .part("stalk_r", "head", Role.TENTACLE, color).pivot(-2, -10, -3).box(-1, -6, -1, 2, 6, 2)
                .part("feeler_l", "head", Role.TENTACLE, color).pivot(3, 0, -6).box(-1, -1, -4, 1, 1, 4)
                .part("feeler_r", "head", Role.TENTACLE, color).pivot(-3, 0, -6).box(0, -1, -4, 1, 1, 4)
                .part("tail", "body", Role.TAIL, color).pivot(0, 2, 12).box(-3, -3, 0, 6, 4, 8)
                .build();
    }

    static ModelSpec golem(String id, int stone, int glow, float scale) {
        return ModelSpec.builder(id).scale(scale).shadow(1.0f)
                .part("body", null, Role.BODY, stone).pivot(0, 12, 0).box(-7, -14, -5, 14, 14, 10).pattern("plates")
                .part("core", "body", Role.STATIC, glow).pivot(0, -9, -5).box(-2, -2, -1, 4, 4, 1).pattern("glow")
                .part("head", "body", Role.HEAD, stone, glow).pivot(0, -14, -1).box(-4, -6, -4, 8, 6, 8).pattern("eyes")
                .part("arm_l", "body", Role.ARM_LEFT, stone).pivot(8, -13, 0).box(-1, -1, -3, 6, 16, 6)
                .part("arm_r", "body", Role.ARM_RIGHT, stone).pivot(-8, -13, 0).box(-5, -1, -3, 6, 16, 6)
                .part("leg_l", null, Role.LEG_BACK_LEFT, stone).pivot(4, 12, 0).box(-3, 0, -3, 6, 12, 6)
                .part("leg_r", null, Role.LEG_BACK_RIGHT, stone).pivot(-4, 12, 0).box(-3, 0, -3, 6, 12, 6)
                .build();
    }

    static ModelSpec vampyre(String id, int skin, int cloak, float scale) {
        return ModelSpec.builder(id).scale(scale).shadow(0.6f)
                .part("body", null, Role.BODY, cloak).pivot(0, 12, 0).box(-4, -12, -2, 8, 12, 4)
                .part("cape", "body", Role.STATIC, cloak).pivot(0, -12, 2).rotate(8, 0, 0).box(-5, 0, 0, 10, 22, 1)
                .part("collar", "body", Role.STATIC, cloak).pivot(0, -12, 0).box(-5, -4, 0, 10, 4, 3)
                .part("head", "body", Role.HEAD, skin).pivot(0, -12, 0).box(-4, -8, -4, 8, 8, 8).pattern("eyes")
                .part("arm_l", "body", Role.ARM_LEFT, skin).pivot(5, -11, 0).box(-1, -1, -2, 3, 13, 3)
                .part("arm_r", "body", Role.ARM_RIGHT, skin).pivot(-5, -11, 0).box(-2, -1, -2, 3, 13, 3)
                .part("wing_l", "body", Role.WING_LEFT, cloak).pivot(2, -10, 3).box(0, -4, 0, 14, 10, 1)
                .part("wing_r", "body", Role.WING_RIGHT, cloak).pivot(-2, -10, 3).box(-14, -4, 0, 14, 10, 1)
                .part("leg_l", null, Role.LEG_BACK_LEFT, 0x1A1A1A).pivot(2, 12, 0).box(-2, 0, -2, 4, 12, 4)
                .part("leg_r", null, Role.LEG_BACK_RIGHT, 0x1A1A1A).pivot(-2, 12, 0).box(-2, 0, -2, 4, 12, 4)
                .build();
    }

    static ModelSpec crystal(String id, int body, int glow, float scale) {
        return ModelSpec.builder(id).scale(scale).shadow(0.6f)
                .part("body", null, Role.BODY, body).pivot(0, 12, 0).box(-4, -12, -2, 8, 12, 4).pattern("glow")
                .part("dress", "body", Role.STATIC, body).pivot(0, 0, 0).box(-5, 0, -3, 10, 10, 6)
                .part("head", "body", Role.HEAD, body).pivot(0, -12, 0).box(-4, -8, -4, 8, 8, 8).pattern("eyes")
                .part("crown", "head", Role.STATIC, glow).pivot(0, -8, 0).box(-3, -3, -3, 6, 3, 6).pattern("glow")
                .part("arm_l", "body", Role.ARM_LEFT, body).pivot(5, -11, 0).box(-1, -1, -2, 3, 12, 3)
                .part("arm_r", "body", Role.ARM_RIGHT, body).pivot(-5, -11, 0).box(-2, -1, -2, 3, 12, 3)
                .part("shard_a", null, Role.SPIN, glow).pivot(0, 4, 0).box(9, -2, -1, 2, 5, 2).pattern("glow")
                .part("shard_b", null, Role.SPIN, glow).pivot(0, 2, 0).box(-11, -2, -1, 2, 5, 2).pattern("glow")
                .part("shard_c", null, Role.FLOAT, glow).pivot(0, -6, 0).box(-1, -2, 8, 2, 4, 2).pattern("glow")
                .part("leg_l", null, Role.LEG_BACK_LEFT, body).pivot(2, 12, 0).box(-2, 0, -2, 4, 12, 4)
                .part("leg_r", null, Role.LEG_BACK_RIGHT, body).pivot(-2, 12, 0).box(-2, 0, -2, 4, 12, 4)
                .build();
    }
}
