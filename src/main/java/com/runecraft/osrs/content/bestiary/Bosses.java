package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.Humanoid.*;
import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/** Bosses: re-sculpts of famous non-humanoid bosses (the humanoid bosses live in {@link BestiaryHum}). */
final class Bosses {
    private Bosses() {
    }

    /** New models for bosses that had none (the humanoid ones now live in {@link BestiaryHum}). */
    static List<ModelSpec> fresh() {
        return new ArrayList<>();
    }

    /** Re-sculpts of existing boss models (same ids). */
    static List<ModelSpec> overrides() {
        // KBD, Vorkath, Zulrah and Corp are re-sculpted in BestiaryBst.
        List<ModelSpec> l = new ArrayList<>();
        return l;
    }

    /** Zulrah: a great serpent rearing from the water: coils, a tall segmented neck, a hooded, horned head. */
    static ModelSpec zulrah(String id, int skin, int belly) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.4f);
        int dark = darker(skin);
        part(b, "coil", null, Role.BODY, skin).accent(belly).at(0, 22, 2).mat("scales").box(-8, -2, -8, 16, 4, 16).add();
        part(b, "coil_top", "coil", Role.STATIC, skin).accent(belly).at(0, -2, 0).mat("scales").box(-6, -2, -6, 12, 2, 12).add();
        String prev = "coil";
        int[] w = {7, 6, 6, 5, 5};
        for (int i = 0; i < w.length; i++) {
            part(b, "neck" + i, prev, i < 3 ? Role.NECK : Role.STATIC, skin).accent(belly).at(0, i == 0 ? -3 : -5, i == 0 ? -2 : 0)
                    .rot(i == 0 ? -10 : i < 3 ? 4 : -4, 0, 0).mat("scales").box(-w[i] / 2f, -5, -w[i] / 2f, w[i], 5, w[i]).add();
            part(b, "neck_belly" + i, "neck" + i, Role.STATIC, belly).at(0, -2.5f, -w[i] / 2f).mat("plates")
                    .box(-w[i] / 2f + 1, -2.5f, -0.75f, w[i] - 2, 5, 1).add();
            prev = "neck" + i;
        }
        part(b, "head", prev, Role.HEAD, skin).accent(0xFFE040).at(0, -5, 0).rot(30, 0, 0).mat("scales").pattern("eyes").glow("eyes")
                .box(-3.5f, -4, -6, 7, 5, 7).add();
        part(b, "snout", "head", Role.STATIC, skin).at(0, -1, -6).mat("scales").box(-2.5f, -2, -3, 5, 3, 3).add();
        part(b, "jaw", "head", Role.JAW, belly).at(0, 1, -5).mat("plates").box(-3, 0, -4, 6, 2, 5).add();
        sym(b, "hood", "head", false, dark).at(3.5f, -2, -1).rot(0, -20, -15).mat("membrane").box(0, -4, -2, 4, 8, 1).add();
        sym(b, "fin", "head", false, belly).at(2, -4, 0).rot(-50, 0, -15).mat("horn").box(-0.5f, -5, -1, 1, 5, 3).add();
        part(b, "crest", "head", Role.STATIC, belly).at(0, -4, 0).rot(-40, 0, 0).mat("horn").box(-0.5f, -6, -1, 1, 6, 4).add();
        String tprev = "coil";
        for (int i = 0; i < 4; i++) {
            part(b, "tail" + i, tprev, Role.TAIL, skin).accent(belly).at(i == 0 ? 6 : 0, i == 0 ? 0 : 0, i == 0 ? 7 : 4).rot(0, i == 0 ? 40 : 25, 0)
                    .mat("scales").box(-1.5f + i * 0.25f, -1.5f + i * 0.25f, 0, 3 - i / 2, 3 - i / 2, 4).add();
            tprev = "tail" + i;
        }
        return b.build();
    }

    /** Corporeal Beast: a hulking, spectral green beast on four columns with a spiked crest and burning eyes. */
    static ModelSpec corp() {
        int green = 0x8AC8A0;
        int dark = 0x3A6A4A;
        ModelSpec.Builder b = ModelSpec.builder("corporeal_beast").shadow(1.8f);
        part(b, "body", null, Role.BODY, green).accent(0xE0FFF0).at(0, 10, 2).rot(-10, 0, 0).mat("ghost").glow("cracks")
                .box(-8, -8, -9, 16, 14, 18).add();
        part(b, "belly", "body", Role.STATIC, dark).at(0, 6, 0).mat("ghost").box(-6, -1, -7, 12, 2, 14).add();
        for (int i = 0; i < 5; i++) {
            part(b, "spike" + i, "body", Role.STATIC, 0xE0F0E8).at(0, -8, -7 + i * 3.5f).rot(-30, 0, 0).mat("horn")
                    .box(-1, -4 + (i == 2 ? -1 : 0), -1, 2, 4 + (i == 2 ? 1 : 0), 2).add();
        }
        sym(b, "shoulder", "body", false, green).accent(0xE0FFF0).at(8, -5, -6).mat("ghost").box(-2, -3, -3, 4, 7, 6).add();
        part(b, "head", "body", Role.HEAD, green).accent(0xFF3020).at(0, -3, -9).rot(10, 0, 0).mat("ghost").pattern("eyes").glow("eyes")
                .box(-5, -5, -7, 10, 9, 7).add();
        part(b, "jaw", "head", Role.JAW, dark).at(0, 3, -5).mat("ghost").box(-4.5f, 0, -3, 9, 3, 5).add();
        sym(b, "horn", "head", false, 0xE0F0E8).at(4, -5, -3).rot(-20, 0, -35).mat("horn").box(-1, -5, -1, 2, 5, 2).add();
        sym(b, "horn_tip", "horn", true, 0xC0E0D0).at(0, -5, 0).rot(-20, 0, 30).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        sym(b, "leg_f", null, false, green).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).at(7, 12, -6).mat("ghost")
                .box(-3, -1, -3, 6, 12, 6).add();
        sym(b, "paw_f", "leg_f", true, dark).at(0, 10, 0).mat("ghost").box(-3.5f, 0, -4.5f, 7, 2, 8).add();
        sym(b, "leg_b", null, false, green).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).at(6, 12, 8).mat("ghost")
                .box(-3, -1, -3, 6, 12, 6).add();
        sym(b, "paw_b", "leg_b", true, dark).at(0, 10, 0).mat("ghost").box(-3.5f, 0, -4.5f, 7, 2, 8).add();
        part(b, "tail", "body", Role.TAIL, green).at(0, -3, 9).rot(-20, 0, 0).mat("ghost").box(-2, -2, 0, 4, 4, 8).add();
        return b.build();
    }

    /** Big feathered wings (one WING_LEFT/WING_RIGHT pair) with layered primaries. */
    static void featherWings(ModelSpec.Builder b, String parent, int col, int tip, float x, float y, float z, int span) {
        sym(b, "wing", parent, false, col).roles(Role.WING_LEFT, Role.WING_RIGHT).at(x, y, z).rot(-10, -25, -25).mat("feathers")
                .box(0, -1.5f, -1, span, 3, 2).add();
        sym(b, "wing_cover", "wing", true, col).at(0, 1.5f, 0).mat("feathers").box(0, 0, -0.5f, span, 7, 1).add();
        sym(b, "wing_primary", "wing", true, tip).at(0, 8, 0).mat("feathers").box(1, 0, -0.5f, span - 1, 6, 1).add();
        sym(b, "wing_tip", "wing", true, tip).at(span, 0, 0).rot(0, 0, 30).mat("feathers").box(0, -1, -0.5f, 5, 10, 1).add();
    }
}
