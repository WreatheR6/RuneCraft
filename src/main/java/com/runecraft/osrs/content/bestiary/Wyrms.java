package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/**
 * OSRS dragons and their kin: four-legged, bat-winged, long-necked, horned, with plated bellies, back spines
 * and a spaded tail. Used by the slayer module (chromatic, brutal and metal dragons, wyverns) and the bosses
 * (King Black Dragon, Vorkath).
 */
public final class Wyrms {
    /** Chromatic (green/blue/red/black) dragons: two long swept horns. */
    public static final int CHROMATIC = 0;
    /** Brutal dragons: heavier horns and a nose horn. */
    public static final int BRUTAL = 1;
    /** Metal dragons (bronze..rune): a crown of short spikes and plated hide. */
    public static final int METAL = 2;

    private Wyrms() {
    }

    /**
     * A full dragon. {@code pattern} chooses the hide: "scales", "plates" (metal), "glow" (lava/rune: emissive
     * veins/runes). {@code heads} &gt; 1 builds a multi-headed dragon (the King Black Dragon) whose first head
     * keeps the NECK/HEAD/JAW roles on every neck.
     */
    public static ModelSpec dragon(String id, int body, int belly, int wing, int horn, int kind, String pattern) {
        return builder(id, body, belly, wing, horn, kind, pattern, 1).build();
    }

    public static ModelSpec.Builder builder(String id, int body, int belly, int wing, int horn, int kind, String pattern, int heads) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.1f);
        boolean metal = kind == METAL;
        boolean bone = "bone".equals(pattern);
        String hide = bone ? "bone" : "glow".equals(pattern) ? (metal ? "metal" : "rock") : metal ? "metal" : "scales";
        String veins = "glow".equals(pattern) ? (metal ? "runes" : "cracks") : null;
        int glowAccent = metal ? 0x80F0FF : 0xFF8A20;
        int dark = Humanoid.darker(body);
        // ---- torso ----
        part(b, "body", null, Role.BODY, body).accent(veins != null ? glowAccent : belly).at(0, 12, 0).mat(hide).glow(veins)
                .box(-6, -5, -10, 12, 10, 20).add();
        part(b, "chest", "body", Role.STATIC, body).accent(veins != null ? glowAccent : belly).at(0, -1, -10).mat(hide).glow(veins)
                .box(-5, -3, -3, 10, 8, 3).add();
        part(b, "belly", "body", Role.STATIC, belly).at(0, 5, 0).mat("plates").box(-5, -0.5f, -11, 10, 1, 19).add();
        if (bone) {
            for (int i = 0; i < 4; i++) {
                sym(b, "rib" + i, "body", false, 0xE8E4D4).at(6, -2, -7 + i * 4).rot(0, 0, -8).mat("bone").box(-0.5f, -3, -1, 1, 8, 2).add();
            }
        }
        sym(b, "shoulder", "body", false, body).accent(belly).at(5.5f, -1, -7).mat(hide).box(-1, -3, -3, 3, 7, 6).add();
        sym(b, "haunch", "body", false, body).accent(belly).at(5.5f, -1, 6).mat(hide).box(-1, -4, -4, 3, 8, 8).add();
        for (int i = 0; i < 5; i++) {
            part(b, "spine" + i, "body", Role.STATIC, horn).at(0, -5, -8 + i * 4).rot(-30, 0, 0).mat("horn")
                    .box(-0.5f, -3 + (i == 0 || i == 4 ? 1 : 0), -1, 1, 3 - (i == 0 || i == 4 ? 1 : 0), 2).add();
        }
        // ---- neck(s) and head(s) ----
        for (int h = 0; h < heads; h++) {
            String sfx = heads == 1 ? "" : String.valueOf(h);
            float yaw = heads == 1 ? 0 : (h - (heads - 1) / 2f) * 28f;
            float nx = heads == 1 ? 0 : (h - (heads - 1) / 2f) * 4f;
            part(b, "neck" + sfx, "body", Role.NECK, body).accent(belly).at(nx, -3, -11).rot(-42, yaw, 0).mat(hide).glow(veins)
                    .box(-3, -3, -6, 6, 6, 6).add();
            part(b, "neck_mid" + sfx, "neck" + sfx, Role.STATIC, body).accent(belly).at(0, 0, -6).rot(-8, 0, 0).mat(hide)
                    .box(-2.5f, -2.5f, -6, 5, 5, 6).add();
            part(b, "neck_frill" + sfx, "neck_mid" + sfx, Role.STATIC, horn).at(0, -2.5f, -3).rot(-20, 0, 0).mat("horn")
                    .box(-0.5f, -2, -2, 1, 2, 4).add();
            part(b, "neck_top" + sfx, "neck_mid" + sfx, Role.STATIC, body).accent(belly).at(0, 0, -6).rot(12, 0, 0).mat(hide)
                    .box(-2, -2, -4, 4, 4, 4).add();
            part(b, "head" + sfx, "neck_top" + sfx, Role.HEAD, body).accent(metal || bone ? 0x60D8FF : 0xFFC030).at(0, 0, -4).rot(40, 0, 0)
                    .mat(hide).pattern("eyes").glow("eyes").box(-3.5f, -3, -6, 7, 5, 7).add();
            part(b, "snout" + sfx, "head" + sfx, Role.STATIC, body).at(0, 0, -6).mat(hide).box(-2.5f, -1.5f, -6, 5, 3, 6).add();
            sym(b, "nostril" + sfx, "snout" + sfx, false, dark).at(1.2f, -1.5f, -5.5f).mat("horn").box(-0.5f, -0.5f, -0.5f, 1, 1, 1).add();
            sym(b, "brow" + sfx, "head" + sfx, false, dark).at(2.5f, -3, -5).rot(15, 0, -10).mat("horn").box(-1, -1, -1, 2, 1, 4).add();
            part(b, "jaw" + sfx, "head" + sfx, Role.JAW, belly).at(0, 2, -5).rot(6, 0, 0).mat("plates").glow(veins != null ? "maw" : null)
                    .box(-2.5f, 0, -7, 5, 2, 8).add();
            sym(b, "frill" + sfx, "head" + sfx, false, wing).at(3.5f, -1, -1).rot(0, -35, -10).mat("membrane").box(0, -2, -0.5f, 3, 4, 1).add();
            if (kind == METAL) {
                for (int k = 0; k < 4; k++) {
                    part(b, "crown" + k + sfx, "head" + sfx, Role.STATIC, horn).at(-2.25f + k * 1.5f, -3, -1 + Math.abs(k - 1.5f))
                            .rot(-25, 0, (k - 1.5f) * 12).mat("horn").box(-0.5f, -3 + (k == 1 || k == 2 ? -1 : 0), -0.5f, 1, 3 + (k == 1 || k == 2 ? 1 : 0), 1).add();
                }
            } else {
                int hw = kind == BRUTAL ? 2 : 1;
                sym(b, "horn" + sfx, "head" + sfx, false, horn).at(2, -3, 0).rot(-60, 18, 0).mat("horn")
                        .box(-hw / 2f - 0.5f, -4, -hw / 2f - 0.5f, hw + 1, 4, hw + 1).add();
                sym(b, "horn_tip" + sfx, "horn" + sfx, true, Humanoid.darker(horn)).at(0, -4, 0).rot(-20, 0, 0).mat("horn")
                        .box(-0.5f, -4, -0.5f, 1, 4, 1, kind == BRUTAL ? 0.2f : 0f).add();
                if (kind == BRUTAL) {
                    part(b, "nose_horn" + sfx, "snout" + sfx, Role.STATIC, horn).at(0, -1.5f, -4).rot(-20, 0, 0).mat("horn")
                            .box(-0.5f, -3, -0.5f, 1, 3, 1, 0.2f).add();
                }
            }
        }
        // ---- wings: arm bone, finger bones and membrane panels ----
        sym(b, "wing", "body", false, wing).roles(Role.WING_LEFT, Role.WING_RIGHT).accent(horn).at(5.5f, -4, -6).rot(0, -10, -22)
                .mat("horn").box(0, -1, -1, 10, 2, 2).add();
        sym(b, "wing_web", "wing", true, wing).accent(horn).at(0, 0, 1).mat("membrane").box(0, -0.25f, 0, 10, 1, 12, -0.25f).add();
        sym(b, "wing_fore", "wing", true, wing).accent(horn).at(10, 0, 0).rot(0, -25, 18).mat("horn").box(0, -0.5f, -0.5f, 10, 1, 1, 0.2f).add();
        sym(b, "wing_web2", "wing_fore", true, wing).accent(horn).at(0, 0, 0.5f).rot(0, 12, 0).mat("membrane")
                .box(0, -0.25f, 0, 10, 1, 13, -0.25f).add();
        sym(b, "wing_claw", "wing_fore", true, horn).at(10, 0, 0).rot(0, 0, 20).mat("horn").box(0, -0.5f, -0.5f, 2, 1, 1).add();
        sym(b, "wing_finger", "wing_fore", true, horn).at(9, 0, 0.5f).rot(0, 60, 0).mat("horn").box(0, -0.5f, -0.5f, 11, 1, 1).add();
        // ---- legs ----
        sym(b, "leg_f", null, false, body).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(belly).at(5, 14, -7).rot(8, 0, -4)
                .mat(hide).box(-2, -1, -2, 4, 6, 4).add();
        sym(b, "shin_f", "leg_f", true, body).accent(belly).at(0, 5, 0).rot(-14, 0, 4).mat(hide).box(-1.5f, 0, -1.5f, 3, 4, 3).add();
        sym(b, "foot_f", "shin_f", true, dark).at(0, 4, 0).rot(6, 0, 0).mat(hide).box(-2, 0, -3, 4, 1, 4, 0.1f).add();
        sym(b, "claws_f", "foot_f", true, horn).at(0, 0.5f, -3).mat("horn").box(-2, -0.5f, -1, 1, 1, 1).box(-0.5f, -0.5f, -1, 1, 1, 1)
                .box(1, -0.5f, -1, 1, 1, 1).add();
        sym(b, "thigh_b", null, false, body).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(belly).at(5, 13, 7).rot(-18, 0, -4)
                .mat(hide).box(-2.5f, -2, -3, 5, 7, 6).add();
        sym(b, "shin_b", "thigh_b", true, body).accent(belly).at(0, 5, -0.5f).rot(36, 0, 4).mat(hide).box(-1.5f, 0, -1.5f, 3, 5, 3).add();
        sym(b, "foot_b", "shin_b", true, dark).at(0, 4.5f, 0).rot(-18, 0, 0).mat(hide).box(-2, 0, -3.5f, 4, 1, 5, 0.1f).add();
        sym(b, "claws_b", "foot_b", true, horn).at(0, 0.5f, -3.5f).mat("horn").box(-2, -0.5f, -1, 1, 1, 1).box(-0.5f, -0.5f, -1, 1, 1, 1)
                .box(1, -0.5f, -1, 1, 1, 1).add();
        // ---- tail ----
        String prev = "body";
        int[] tw = {5, 4, 3, 2, 2};
        for (int i = 0; i < tw.length; i++) {
            part(b, "tail" + i, prev, Role.TAIL, body).accent(belly).at(0, i == 0 ? -1 : 0, i == 0 ? 10 : 5).rot(i == 0 ? -8 : 7, 0, 0)
                    .mat(hide).glow(i < 2 ? veins : null).box(-tw[i] / 2f, -tw[i] / 2f, 0, tw[i], tw[i], 5).add();
            if (i < 4) {
                part(b, "tail_spine" + i, "tail" + i, Role.STATIC, horn).at(0, -tw[i] / 2f, 2).rot(-30, 0, 0).mat("horn")
                        .box(-0.5f, -2, -0.5f, 1, 2, 2).add();
            }
            prev = "tail" + i;
        }
        part(b, "tail_spade", prev, Role.STATIC, horn).at(0, 0, 5).mat("horn").box(-2, -0.5f, 0, 4, 1, 2).box(-1, -0.5f, 2, 2, 1, 1).add();
        return b;
    }

    /** A wyvern: two legs, wings as forelimbs, long neck, crested head and long tail. */
    public static ModelSpec wyvern(String id, int body, int accent, int wing, String pattern) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.0f);
        boolean bony = "plates".equals(pattern);
        String hide = bony ? "bone" : "scales";
        int dark = Humanoid.darker(body);
        part(b, "body", null, Role.BODY, body).accent(accent).at(0, 10, 1).rot(-20, 0, 0).mat(hide).box(-5, -5, -7, 10, 10, 14).add();
        if (bony) {
            for (int i = 0; i < 4; i++) {
                part(b, "rib" + i, "body", Role.STATIC, accent).at(0, -1, -5 + i * 3).mat("bone").box(-5.5f, -3, -0.5f, 11, 7, 1).add();
            }
        } else {
            part(b, "belly", "body", Role.STATIC, accent).at(0, 5, 0).mat("plates").box(-4, -0.5f, -6, 8, 1, 12).add();
        }
        part(b, "neck", "body", Role.NECK, body).accent(accent).at(0, -3, -7).rot(-35, 0, 0).mat(hide).box(-2, -2, -6, 4, 4, 6).add();
        part(b, "neck_top", "neck", Role.STATIC, body).accent(accent).at(0, 0, -6).rot(-10, 0, 0).mat(hide).box(-1.5f, -1.5f, -5, 3, 4, 5).add();
        part(b, "head", "neck_top", Role.HEAD, body).accent(bony ? 0x60E0FF : 0xFFD040).at(0, 0, -5).rot(60, 0, 0).mat(hide)
                .pattern("eyes").glow("eyes").box(-2.5f, -3, -6, 5, 5, 6).add();
        part(b, "snout", "head", Role.STATIC, body).at(0, 0, -6).mat(hide).box(-1.5f, -1.5f, -4, 3, 3, 4).add();
        part(b, "jaw", "head", Role.JAW, accent).at(0, 2, -4).mat("plates").box(-2, 0, -6, 4, 1, 6).add();
        part(b, "crest", "head", Role.STATIC, accent).at(0, -3, -1).rot(-35, 0, 0).mat("horn").box(-0.5f, -5, -1, 1, 5, 4).add();
        sym(b, "horn", "head", false, accent).at(2, -2.5f, 0).rot(-70, 15, 0).mat("horn").box(-0.5f, -4, -0.5f, 1, 4, 1).add();
        sym(b, "wing", "body", false, wing).roles(Role.WING_LEFT, Role.WING_RIGHT).accent(accent).at(5, -3, -4).rot(0, -10, -25)
                .mat("horn").box(0, -1, -1, 9, 2, 2).add();
        sym(b, "wing_web", "wing", true, wing).accent(accent).at(0, 0, 1).mat("membrane").box(0, -0.25f, 0, 9, 1, 10, -0.25f).add();
        sym(b, "wing_fore", "wing", true, wing).at(9, 0, 0).rot(0, -30, 25).mat("horn").box(0, -0.5f, -0.5f, 9, 1, 1, 0.2f).add();
        sym(b, "wing_web2", "wing_fore", true, wing).accent(accent).at(0, 0, 0.5f).rot(0, 15, 0).mat("membrane").box(0, -0.25f, 0, 9, 1, 11, -0.25f).add();
        sym(b, "wing_talon", "wing_fore", true, accent).at(9, 0, 0).rot(0, 0, 60).mat("horn").box(0, -0.5f, -0.5f, 3, 1, 1).add();
        sym(b, "thigh", null, false, body).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(accent).at(4, 12, 3).rot(-20, 0, -4)
                .mat(hide).box(-2, -1, -2.5f, 4, 6, 5).add();
        sym(b, "shin", "thigh", true, body).at(0, 5, 0).rot(40, 0, 4).mat(hide).box(-1.5f, 0, -1.5f, 3, 5, 3).add();
        sym(b, "foot", "shin", true, dark).at(0, 4.5f, 0).rot(-20, 0, 0).mat(hide).box(-2, 0, -3.5f, 4, 2, 5).add();
        sym(b, "talons", "foot", true, accent).at(0, 1, -3.5f).mat("horn").box(-2, -0.5f, -1, 1, 1, 1).box(1, -0.5f, -1, 1, 1, 1).add();
        String prev = "body";
        int[] tw = {4, 3, 3, 2, 2, 1};
        for (int i = 0; i < tw.length; i++) {
            part(b, "tail" + i, prev, Role.TAIL, body).accent(accent).at(0, i == 0 ? 2 : 0, i == 0 ? 7 : 4).rot(i == 0 ? 25 : 4, 0, 0)
                    .mat(hide).box(-tw[i] / 2f, -tw[i] / 2f, 0, tw[i], tw[i], 4).add();
            prev = "tail" + i;
        }
        part(b, "tail_barb", prev, Role.STATIC, accent).at(0, 0, 4).mat("horn").box(-1.5f, -0.5f, 0, 3, 1, 2).add();
        return b.build();
    }
}
