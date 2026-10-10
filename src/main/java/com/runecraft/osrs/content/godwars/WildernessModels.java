package com.runecraft.osrs.content.godwars;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

/** Custom models for the Wilderness bosses, their minions and the revenants. */
final class WildernessModels {
    private WildernessModels() {
    }

    static List<ModelSpec> all() {
        List<ModelSpec> list = new ArrayList<>();
        list.add(ModelKit.bear("callisto", 0x5A3A22, 0x8A6A4A, 0xE0E0D0, 1.0f));
        list.add(ModelKit.bear("artio", 0x7A4A22, 0xA07A50, 0xE0E0D0, 1.0f));
        list.add(ModelKit.spider("venenatis", 0x3A2A3A, 0x5A1A3A, 0x2A1A2A, 0xE0E0C0, 1.0f));
        list.add(ModelKit.spider("spindel", 0x2A3A2A, 0x3A5A2A, 0x1A2A1A, 0xE0E0C0, 1.0f));
        list.add(ModelKit.spider("venenatis_spiderling", 0x4A3A4A, 0x6A2A4A, 0x3A2A3A, 0xE0E0C0, 0.5f));
        list.add(skeletonChampion("vetion", 0x5A3A8A, 0x3A2A5A, 0xA060F0, 0x4A2A6A));
        list.add(skeletonChampion("calvarion", 0x8A4A2A, 0x5A3A2A, 0xF0A040, 0x6A2A1A));
        list.add(ModelKit.hound("skeleton_hellhound", 0xD8D0B8, 0xE8E0C8, 0xA060F0, 1.0f, true));
        list.add(ModelKit.hound("greater_skeleton_hellhound", 0xC8B8A0, 0xD8C8B0, 0xF0A040, 1.0f, true));
        list.add(ModelKit.scorpion("scorpia", 0x2E2A26, 0x4A3A30, 0xC02020, 1.0f));
        list.add(ModelKit.scorpion("scorpias_offspring", 0x4A3A30, 0x6A5A40, 0xC04020, 0.5f));
        list.add(ModelKit.scorpion("scorpias_guardian", 0x6A6A7A, 0x9A9AA8, 0x40C0F0, 0.7f));
        list.add(chaosElemental());
        list.add(chaosFanatic());
        list.add(crazyArchaeologist());
        list.add(kingBlackDragon());
        list.add(ModelKit.revenant("revenant_imp", 0x60D0B0, 0xD8E8E0, true, true, false));
        list.add(ModelKit.revenant("revenant_goblin", 0x60C0A0, 0xD8E8E0, false, false, false));
        list.add(ModelKit.revenant("revenant_pyrefiend", 0x70D0C0, 0xF0E0B0, true, false, false));
        list.add(ModelKit.revenant("revenant_hobgoblin", 0x60B0A0, 0xD8E8E0, false, false, false));
        list.add(ModelKit.revenant("revenant_cyclops", 0x70C0B0, 0xD8E8E0, false, false, true));
        list.add(ModelKit.revenant("revenant_hellhound", 0x50B0A0, 0xE0D0C0, true, false, false));
        list.add(ModelKit.revenant("revenant_demon", 0x60A090, 0xE0D0C0, true, true, false));
        list.add(ModelKit.revenant("revenant_ork", 0x70B0A0, 0xD8E8E0, false, false, false));
        list.add(ModelKit.revenant("revenant_dark_beast", 0x408070, 0xA0C0B0, true, false, false));
        list.add(ModelKit.revenant("revenant_knight", 0x80D0C0, 0xC8D8E0, false, false, false));
        list.add(ModelKit.revenant("revenant_dragon", 0x50C0B0, 0xD8E8E0, true, true, false));
        return list;
    }

    /** Vet'ion / Calvar'ion: an armoured skeletal champion with a glowing halberd, shield and cape. */
    private static ModelSpec skeletonChampion(String id, int armour, int armour2, int glow, int cape) {
        int bone = 0xE0D8C0;
        return ModelSpec.builder(id).shadow(0.6f)
                .part("leg_left", null, Role.LEG_FRONT_LEFT, bone).pivot(2, 12, 0).box(-1, 0, -1, 2, 12, 2)
                .part("greave_left", "leg_left", Role.STATIC, armour).pivot(0, 0, 0).box(-1.5f, 5, -1.5f, 3, 7, 3).pattern("plates")
                .part("leg_right", null, Role.LEG_FRONT_RIGHT, bone).pivot(-2, 12, 0).box(-1, 0, -1, 2, 12, 2)
                .part("greave_right", "leg_right", Role.STATIC, armour).pivot(0, 0, 0).box(-1.5f, 5, -1.5f, 3, 7, 3).pattern("plates")
                .part("body", null, Role.BODY, armour).pivot(0, 12, 0).box(-5, -12, -3, 10, 10, 6).pattern("plates")
                .box(-3, -2, -1, 6, 2, 2)
                .part("tassets", "body", Role.STATIC, armour2).pivot(0, 0, 0).box(-4.5f, -3, -2.5f, 9, 5, 5).pattern("plates")
                .part("head", "body", Role.HEAD, bone).pivot(0, -12, 0).box(-3.5f, -7, -3.5f, 7, 7, 7).pattern("eyes")
                .part("helm", "head", Role.STATIC, armour).pivot(0, 0, 0).box(-4, -8.5f, -4, 8, 4, 8)
                .part("crest", "helm", Role.STATIC, glow).pivot(0, -8.5f, 0).box(-0.5f, -4, -3, 1, 4, 7).pattern("glow")
                .part("arm_left", "body", Role.ARM_LEFT, bone).pivot(6, -10, 0).box(-1, -1, -1, 2, 12, 2)
                .part("pauldron_left", "arm_left", Role.STATIC, armour).pivot(0, 0, 0).box(-2, -3, -3, 5, 4, 6)
                .part("shield", "arm_left", Role.STATIC, armour2).pivot(1.5f, 6, 0).box(0, -5, -4, 1, 10, 8).pattern("plates")
                .part("arm_right", "body", Role.ARM_RIGHT, bone).pivot(-6, -10, 0).box(-1, -1, -1, 2, 12, 2)
                .part("pauldron_right", "arm_right", Role.STATIC, armour).pivot(0, 0, 0).box(-3, -3, -3, 5, 4, 6)
                .part("halberd", "arm_right", Role.STATIC, 0x3A3A3A).pivot(0, 10, -1).box(-0.5f, -18, -0.5f, 1, 26, 1)
                .part("halberd_blade", "halberd", Role.STATIC, glow).pivot(0, -18, 0).box(-0.5f, -1, -5, 1, 7, 4).box(-0.5f, -4, -1, 1, 3, 2)
                .pattern("glow")
                .part("cape", "body", Role.TAIL, cape).pivot(0, -11, 3).box(-4.5f, 0, 0, 9, 17, 1)
                .build();
    }

    /** Chaos Elemental: a floating, ever-shifting core orbited by shards of three colours. */
    private static ModelSpec chaosElemental() {
        return ModelSpec.builder("chaos_elemental").shadow(0.8f)
                .part("core", null, Role.FLOAT, 0xC03030).pivot(0, 6, 0).box(-5, -5, -5, 10, 10, 10).pattern("glow")
                .part("inner", "core", Role.SPIN, 0xF0D040).pivot(0, 0, 0).rotate(45, 0, 45).box(-3, -3, -3, 6, 6, 6).pattern("glow")
                .part("ring_green", "core", Role.SPIN, 0x30C040).pivot(0, 1, 0)
                .box(-11, -2, -1, 3, 4, 2).box(8, -2, -1, 3, 4, 2).box(-1, -2, -11, 2, 4, 3).box(-1, -2, 8, 2, 4, 3)
                .pattern("glow")
                .part("ring_blue", "core", Role.SPIN, 0x3050D0).pivot(0, -8, 0).rotate(0, 45, 0)
                .box(-8, -1, -1, 2, 2, 2).box(6, -1, -1, 2, 2, 2).box(-1, -1, -8, 2, 2, 2).box(-1, -1, 6, 2, 2, 2)
                .pattern("glow")
                .part("ring_purple", "core", Role.SPIN, 0x9030C0).pivot(0, 8, 0).rotate(0, 22, 0)
                .box(-7, -1, -1, 2, 2, 2).box(5, -1, -1, 2, 2, 2).box(-1, -1, -7, 2, 2, 2).box(-1, -1, 5, 2, 2, 2)
                .pattern("glow")
                .part("crown", "core", Role.STATIC, 0x202020).pivot(0, -5, 0).box(-4, -4, -1, 1, 4, 1).box(3, -4, -1, 1, 4, 1)
                .box(-1, -6, -1, 2, 6, 2)
                .part("eye", "core", Role.HEAD, 0xF0F0F0).pivot(0, -1, -5).box(-2, -2, -1, 4, 4, 1).pattern("eyes")
                .part("tendril_a", "core", Role.TENTACLE, 0x30C040).pivot(-3, 5, 0).box(-1, 0, -1, 2, 9, 2)
                .part("tendril_b", "core", Role.TENTACLE, 0x3050D0).pivot(3, 5, 1).box(-1, 0, -1, 2, 8, 2)
                .part("tendril_c", "core", Role.TENTACLE, 0xC03030).pivot(0, 5, -3).box(-1, 0, -1, 2, 10, 2)
                .build();
    }

    /** Chaos Fanatic: a raving robed zealot with a staff. */
    private static ModelSpec chaosFanatic() {
        return ModelKit.humanoid("chaos_fanatic", 0xC8A080, 0x2A3A2A, 0x2A3A2A, "eyes")
                .part("hood", "head", Role.STATIC, 0x1A2A1A).pivot(0, 0, 0).box(-4.5f, -8.5f, -2, 9, 9, 7).box(-4.5f, -8.5f, -4.5f, 9, 2, 3)
                .part("robe", "body", Role.STATIC, 0x2A3A2A).pivot(0, 0, 0).box(-4.5f, -1, -2.5f, 9, 12, 5).pattern("stripes")
                .part("sash", "body", Role.STATIC, 0x8A1A1A).pivot(0, 0, 0).box(-4.2f, -12, -2.2f, 2, 12, 1)
                .part("staff", "arm_right", Role.STATIC, 0x4A3A2A).pivot(-1, 9, -1).box(-0.5f, -14, -0.5f, 1, 22, 1)
                .part("orb", "staff", Role.SPIN, 0x40E040).pivot(0, -15, 0).box(-1.5f, -1.5f, -1.5f, 3, 3, 3).pattern("glow")
                .build();
    }

    /** Crazy archaeologist: khaki explorer with a fedora, backpack and crossbow. */
    private static ModelSpec crazyArchaeologist() {
        return ModelKit.humanoid("crazy_archaeologist", 0xD8B090, 0x8A6A3A, 0x6A5A3A, "eyes")
                .part("fedora", "head", Role.STATIC, 0x5A3A22).pivot(0, 0, 0).box(-5.5f, -9, -5.5f, 11, 1, 11).box(-4, -11, -4, 8, 2, 8)
                .part("beard", "head", Role.STATIC, 0xE0E0E0).pivot(0, 0, 0).box(-3, -2, -4.5f, 6, 3, 1)
                .part("backpack", "body", Role.STATIC, 0x6A4A2A).pivot(0, 0, 0).box(-3, -11, 2, 6, 8, 3)
                .part("crossbow", "arm_right", Role.STATIC, 0x5A4A2A).pivot(-1, 9, 0).box(-1, 0, -8, 2, 2, 8)
                .part("limbs", "crossbow", Role.STATIC, 0x8A8A8A).pivot(0, 0, -7).box(-4, 0, -1, 8, 1, 1)
                .build();
    }

    /** King Black Dragon: a massive three-headed black dragon. */
    private static ModelSpec kingBlackDragon() {
        int scales = 0x26262C;
        int dark = 0x141418;
        int belly = 0x4A3A3A;
        ModelSpec.Builder b = ModelSpec.builder("king_black_dragon").shadow(1.6f)
                .part("body", null, Role.BODY, scales).pivot(0, 10, 0).box(-7, -6, -12, 14, 12, 24).pattern("scales")
                .part("belly", "body", Role.STATIC, belly).pivot(0, 0, 0).box(-6, 5, -11, 12, 2, 20).pattern("plates")
                .part("spines", "body", Role.STATIC, dark).pivot(0, 0, 0).box(-1, -9, -10, 2, 3, 20)
                .part("leg_fl", "body", Role.LEG_FRONT_LEFT, scales).pivot(6, 4, -8).box(-2.5f, 0, -2.5f, 5, 10, 5).box(-3, 9, -4.5f, 6, 1, 2)
                .part("leg_fr", "body", Role.LEG_FRONT_RIGHT, scales).pivot(-6, 4, -8).box(-2.5f, 0, -2.5f, 5, 10, 5)
                .box(-3, 9, -4.5f, 6, 1, 2)
                .part("leg_bl", "body", Role.LEG_BACK_LEFT, scales).pivot(6, 4, 8).box(-3, 0, -3, 6, 10, 6)
                .part("leg_br", "body", Role.LEG_BACK_RIGHT, scales).pivot(-6, 4, 8).box(-3, 0, -3, 6, 10, 6);
        String[] names = {"right", "middle", "left"};
        for (int i = -1; i <= 1; i++) {
            String n = names[i + 1];
            b.part("neck_" + n, "body", Role.NECK, scales).pivot(i * 4, -3, -11).rotate(-35, i * -28, 0).box(-2, -2, -10, 4, 4, 10)
                    .pattern("scales")
                    .part("head_" + n, "neck_" + n, Role.HEAD, scales).pivot(0, 0, -10).rotate(35, 0, 0)
                    .box(-3, -3, -5, 6, 5, 5).box(-2.5f, -2, -10, 5, 3, 5).pattern("eyes")
                    .part("horns_" + n, "head_" + n, Role.STATIC, dark).pivot(0, -3, 0).box(-3, -3, -1, 1, 3, 2).box(2, -3, -1, 1, 3, 2)
                    .part("jaw_" + n, "head_" + n, Role.JAW, 0x5A2A2A).pivot(0, 2, -4).box(-2.5f, 0, -6, 5, 1, 6);
        }
        return b.part("wing_left", "body", Role.WING_LEFT, dark).pivot(6, -5, -6).rotate(0, 0, -15).box(0, -1, -1, 18, 2, 2)
                .part("membrane_left", "wing_left", Role.STATIC, 0x3A2A36).pivot(0, 0, 0).box(0, 0, 0, 18, 1, 16).pattern("stripes")
                .part("wing_right", "body", Role.WING_RIGHT, dark).pivot(-6, -5, -6).rotate(0, 0, 15).box(-18, -1, -1, 18, 2, 2)
                .part("membrane_right", "wing_right", Role.STATIC, 0x3A2A36).pivot(0, 0, 0).box(-18, 0, 0, 18, 1, 16).pattern("stripes")
                .part("tail1", "body", Role.TAIL, scales).pivot(0, -2, 12).rotate(-10, 0, 0).box(-2.5f, -2, 0, 5, 4, 10)
                .part("tail2", "tail1", Role.TAIL, scales).pivot(0, 0, 10).rotate(-5, 0, 0).box(-1.5f, -1.5f, 0, 3, 3, 10)
                .part("tail_spike", "tail2", Role.STATIC, dark).pivot(0, 0, 10).box(-2.5f, -0.5f, 0, 5, 1, 4)
                .build();
    }
}
