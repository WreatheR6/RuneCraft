package com.runecraft.osrs.content.zeah;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.List;

/** Custom models: the sand crab (shared by the King sand crab, rendered larger). */
public final class ZeahModels {
    private ZeahModels() {
    }

    public static List<ModelSpec> all() {
        return List.of(sandCrab());
    }

    /** A squat crab with a rock-like shell (sand crabs pose as rocks until disturbed), pincers and six legs. */
    static ModelSpec sandCrab() {
        ModelSpec.Builder b = ModelSpec.builder("sand_crab").shadow(0.6f);
        b.part("body", null, Role.BODY, 0xC0905A).pivot(0, 19, 0).box(-5, -2, -4, 10, 4, 8);
        b.part("shell", "body", Role.STATIC, 0x9A8A6A).pivot(0, -2, 0).box(-5.5f, -3, -4.5f, 11, 3, 9).pattern("spots");
        b.part("eyes", "body", Role.HEAD, 0xC0905A).pivot(0, -1, -4).box(-2, -2, -1, 4, 2, 1).pattern("eyes");
        b.part("claw_l", "body", Role.ARM_LEFT, 0xD07040).pivot(4, 0, -3).rotate(0, 25, 0).box(-1, -1, -5, 2, 2, 5)
                .box(-1.5f, -1.5f, -8, 3, 3, 3);
        b.part("claw_r", "body", Role.ARM_RIGHT, 0xD07040).pivot(-4, 0, -3).rotate(0, -25, 0).box(-1, -1, -5, 2, 2, 5)
                .box(-1.5f, -1.5f, -8, 3, 3, 3);
        float[] zs = {-2, 0.5f, 3};
        for (int i = 0; i < zs.length; i++) {
            b.part("leg_l" + i, "body", Role.LEG_EXTRA, 0xB07A4A).pivot(5, 1, zs[i]).rotate(0, 0, -30).box(0, -0.5f, -0.5f, 5, 1, 1);
            b.part("leg_r" + i, "body", Role.LEG_EXTRA, 0xB07A4A).pivot(-5, 1, zs[i]).rotate(0, 0, 30).box(-5, -0.5f, -0.5f, 5, 1, 1);
        }
        return b.build();
    }
}
