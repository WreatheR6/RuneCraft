package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/** Hand-sculpted showpiece models (TzTok-Jad and friends). */
public final class Beasts {
    private Beasts() {
    }

    static List<ModelSpec> all() {
        List<ModelSpec> list = new ArrayList<>();
        list.add(jad("tztok_jad", 1.0f));
        return list;
    }

    /**
     * TzTok-Jad: a hunched, gorilla-stanced obsidian beast with massive crested shoulders, knuckle-walking
     * arms, a horned head with a glowing maw and lava veins running through its whole body.
     */
    public static ModelSpec jad(String id, float scale) {
        int red = 0xA8231C;
        int deep = 0x7A1A18;
        int dark = 0x3A1416;
        int lava = 0xFF8A1E;
        ModelSpec.Builder b = ModelSpec.builder(id).scale(scale).shadow(1.4f);
        // hips and hunched torso
        part(b, "hips", null, Role.BODY, deep).accent(lava).at(0, 12, 5).mat("rock").glow("cracks")
                .box(-6, -4, -4, 12, 8, 9).add();
        part(b, "torso", "hips", Role.STATIC, red).accent(lava).at(0, -3, -2).rot(42, 0, 0).mat("rock").glow("cracks")
                .box(-8, -13, -6, 16, 14, 12).add();
        part(b, "chest", "torso", Role.STATIC, red).accent(lava).at(0, -6, -6).rot(-4, 0, 0).mat("rock").glow("cracks")
                .box(-7, -6, -2, 14, 9, 2).box(-5, 3, -1.5f, 10, 3, 2).add();
        part(b, "belly", "torso", Role.STATIC, dark).accent(lava).at(0, 0, 0).mat("rock").glow("cracks")
                .box(-6, -2, -5, 12, 4, 10).add();
        for (int i = 0; i < 4; i++) {
            part(b, "ridge" + i, "torso", Role.STATIC, i % 2 == 0 ? red : deep).accent(lava)
                    .at(0, -13, -4 + i * 3.2f).rot(-25, 0, 0).mat("rock").box(-1.5f, -3, -1.5f, 3, 3 + (i == 1 ? 1 : 0), 3).add();
        }
        // shoulders: huge pauldron humps with crest plates
        sym(b, "shoulder", "torso", false, red).accent(lava).at(8.5f, -10, -1).rot(-10, 0, -16).mat("rock").glow("cracks")
                .box(-3, -5, -6, 9, 9, 11).add();
        sym(b, "shoulder_cap", "shoulder", true, deep).accent(lava).at(1, -4, 0).rot(0, 0, -10).mat("rock")
                .box(-3, -2, -4, 7, 2, 8).add();
        sym(b, "crest_a", "shoulder", true, red).accent(lava).at(1, -5, -2.5f).rot(-20, 0, -18).mat("rock")
                .box(-1.5f, -4, -2, 3, 4, 4).add();
        sym(b, "crest_b", "shoulder", true, red).accent(lava).at(2, -5, 2).rot(-35, 0, -22).mat("rock")
                .box(-1.5f, -3, -1.5f, 3, 3, 3).add();
        // head: low-slung, horned, glowing eyes and maw
        part(b, "head", "torso", Role.HEAD, red).accent(lava).at(0, -7, -7).rot(-30, 0, 0).mat("rock").glow("eyes")
                .pattern("eyes").box(-5, -4, -6, 10, 8, 7).add();
        part(b, "snout", "head", Role.STATIC, deep).accent(lava).at(0, 2, -6).mat("rock").box(-3.5f, -1, -3, 7, 3, 3).add();
        part(b, "brow", "head", Role.STATIC, red).accent(lava).at(0, -3.5f, -6).rot(12, 0, 0).mat("rock")
                .box(-5.5f, -1.5f, -2, 11, 3, 3).add();
        part(b, "jaw", "head", Role.JAW, dark).accent(lava).at(0, 2.5f, -4).rot(8, 0, 0).mat("rock").glow("maw")
                .box(-4.5f, 0, -5, 9, 3, 6).add();
        sym(b, "tusk", "jaw", false, 0xE8D8B0).at(3.5f, 0, -4.5f).rot(-10, 0, 8).mat("horn").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        sym(b, "cheek", "head", false, deep).accent(lava).at(5, 0, -3).rot(0, -12, 0).mat("rock").box(-1, -2.5f, -2.5f, 2, 5, 5).add();
        part(b, "crest", "head", Role.STATIC, red).accent(lava).at(0, -4, -1).rot(-35, 0, 0).mat("rock")
                .box(-1, -5, -2.5f, 2, 5, 5).add();
        sym(b, "horn", "head", false, red).accent(lava).at(3, -4, -3).rot(-30, -8, -20).mat("rock")
                .box(-1.5f, -5, -1.5f, 3, 5, 3).add();
        sym(b, "horn_mid", "horn", true, deep).accent(lava).at(0, -5, 0).rot(-28, 0, 4).mat("rock")
                .box(-1, -4, -1, 2, 4, 2).add();
        sym(b, "horn_tip", "horn_mid", true, dark).at(0, -4, 0).rot(-30, 0, 0).mat("rock").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        sym(b, "fin", "head", false, red).accent(lava).at(2, -4, 1).rot(-55, 10, -8).mat("rock")
                .box(-1, -7, -2, 2, 7, 4).add();
        sym(b, "fin_tip", "fin", true, deep).accent(lava).at(0, -7, 0).rot(-20, 0, 0).mat("rock").box(-0.5f, -3, -1.5f, 1, 3, 3).add();
        sym(b, "crest_c", "shoulder", true, deep).accent(lava).at(-1, -5, 4).rot(-50, 0, -10).mat("rock")
                .box(-1, -3, -1, 2, 3, 3).add();
        // knuckle-walking arms
        sym(b, "arm", null, false, red).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(lava).at(12, 3, -5).rot(-22, 0, -26)
                .mat("rock").glow("cracks").box(-3.5f, -2, -3.5f, 7, 11, 7).add();
        sym(b, "elbow", "arm", true, deep).accent(lava).at(0, 8, 2.5f).rot(20, 0, 0).mat("rock").box(-2.5f, -2, 0, 5, 4, 2).add();
        sym(b, "forearm", "arm", true, red).accent(lava).at(0, 9, 0).rot(4, 0, 14).mat("rock").glow("cracks")
                .box(-4, 0, -4, 8, 8, 8).add();
        sym(b, "forearm_plate", "forearm", true, deep).accent(lava).at(3.5f, 1, 0).rot(0, 0, -6).mat("rock").glow("cracks")
                .box(0, 0, -3.5f, 2, 6, 7).add();
        sym(b, "fist", "forearm", true, dark).accent(lava).at(0, 8, -0.5f).rot(18, 0, 10).mat("rock")
                .box(-4.5f, 0, -4.5f, 9, 4, 8).add();
        for (int k = 0; k < 3; k++) {
            sym(b, "knuckle" + k, "fist", true, 0x241012).at(-3 + k * 3, 3, -4.5f).rot(20, 0, 0).mat("rock")
                    .box(-1, -1, -1.5f, 2, 2, 2).add();
        }
        // short, bowed hind legs with broad feet
        sym(b, "leg", null, false, red).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(lava).at(6, 13, 6)
                .rot(-28, 0, -6).mat("rock").glow("cracks").box(-4, -2, -4, 8, 8, 8).add();
        sym(b, "thigh_plate", "leg", true, deep).accent(lava).at(3.5f, -1, 0).rot(0, 0, -8).mat("rock")
                .box(0, 0, -3, 2, 6, 6).add();
        sym(b, "shin", "leg", true, deep).accent(lava).at(0, 6, 0).rot(38, 0, 4).mat("rock").glow("cracks")
                .box(-3.5f, 0, -3.5f, 7, 6, 7).add();
        sym(b, "foot", "shin", true, dark).accent(lava).at(0, 5, 0).rot(-10, 0, 2).mat("rock")
                .box(-4.5f, 0, -6, 9, 3, 9).add();
        for (int k = 0; k < 3; k++) {
            sym(b, "toe" + k, "foot", true, 0x241012).at(-3 + k * 3, 1.5f, -6).mat("rock").box(-1, -1, -1.5f, 2, 2, 2).add();
        }
        return b.build();
    }
}
