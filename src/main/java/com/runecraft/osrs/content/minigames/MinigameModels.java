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

    public static List<ModelSpec> models() {
        List<ModelSpec> list = new ArrayList<>();
        // Fight Caves, Inferno, Pest Control, Barbarian Assault, Wintertodt, Tempoross and Guardians of the Rift
        // creatures are sculpted in the bestiary module (BestiaryRad*).
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
