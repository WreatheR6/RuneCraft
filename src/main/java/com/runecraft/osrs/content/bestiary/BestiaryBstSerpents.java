package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.BestiaryBst.chain;
import static com.runecraft.osrs.content.bestiary.BestiaryBst.dark;
import static com.runecraft.osrs.content.bestiary.BestiaryBst.light;
import static com.runecraft.osrs.content.bestiary.BestiaryBst.sideFangs;
import static com.runecraft.osrs.content.bestiary.BestiaryBst.teeth;
import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/**
 * Serpents and reptiles: Zulrah (serpentine, magma and tanzanite forms), the Alchemical Hydra (all four phases),
 * the Hueycoatl, Amoxliatl and the Leviathan.
 */
final class BestiaryBstSerpents {
    private BestiaryBstSerpents() {
    }

    static List<ModelSpec> all() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(zulrah("zulrah", 0x3A7A32, 0x9AC060, 0x5AA040, 0xFFE040, null));
        l.add(zulrah("zulrah_magma", 0x9A2A1A, 0xE07030, 0xD04A20, 0xFFB040, "cracks"));
        l.add(zulrah("zulrah_tanzanite", 0x2A3A8A, 0x40B0C0, 0x30A8C0, 0x70F0FF, null));
        l.add(hydra("alchemical_hydra", 0x4A5A44, 0x8A9A70, 0x7AE040, 4));
        l.add(hydra("alchemical_hydra_blue", 0x44505A, 0x80909A, 0x50B0FF, 3));
        l.add(hydra("alchemical_hydra_red", 0x5A4440, 0x9A807A, 0xFF5A30, 2));
        l.add(hydra("alchemical_hydra_enraged", 0x3A3A3E, 0x6A6A70, 0xE0E0F0, 1));
        l.add(hueycoatl());
        l.add(amoxliatl());
        l.add(leviathan());
        return l;
    }

    // ------------------------------------------------------------------------------------------------
    // Zulrah
    // ------------------------------------------------------------------------------------------------

    /** Zulrah: coils in the swamp, a tall S-curved trunk, a broad horned head ringed by a fan of fins. */
    static ModelSpec zulrah(String id, int skin, int belly, int fin, int eye, String veins) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.4f);
        int dk = dark(skin, 0.35f);
        int acc = veins != null ? eye : belly;
        // two stacked coils (square rings, the upper one turned 40 degrees)
        part(b, "coil", null, Role.BODY, skin).accent(acc).at(0, 24, 2).mat("scales").glow(veins)
                .box(-10, -4, -10, 20, 4, 5).box(-10, -4, 5, 20, 4, 5).box(5, -4, -5, 5, 4, 10).box(-10, -4, -5, 5, 4, 10).add();
        part(b, "coil2", "coil", Role.STATIC, skin).accent(acc).at(0, -4, 0).rot(0, 40, 0).mat("scales").glow(veins)
                .box(-7.5f, -3, -7.5f, 15, 3, 4).box(-7.5f, -3, 3.5f, 15, 3, 4).box(3.5f, -3, -3.5f, 4, 3, 7).box(-7.5f, -3, -3.5f, 4, 3, 7).add();
        // rising trunk
        int[] w = {9, 8, 7, 7, 6};
        int[] len = {6, 6, 6, 6, 5};
        float[] pitch = {-18, -14, 14, 22, 18};
        String prev = "coil2";
        for (int i = 0; i < w.length; i++) {
            String n = "trunk" + i;
            part(b, n, prev, i < 3 ? Role.NECK : Role.STATIC, skin).accent(acc).at(0, i == 0 ? -2 : -len[i - 1], i == 0 ? 0 : 0)
                    .rot(pitch[i], i == 0 ? -40 : 0, 0).mat("scales").glow(i < 3 ? veins : null)
                    .box(-w[i] / 2f, -len[i], -w[i] / 2f, w[i], len[i], w[i]).add();
            part(b, "belly" + i, n, Role.STATIC, belly).accent(dark(belly, 0.3f)).at(0, -len[i] / 2f, -w[i] / 2f).mat("plates")
                    .box(-w[i] / 2f + 1, -len[i] / 2f, -0.6f, w[i] - 2, len[i], 1).add();
            part(b, "dorsal" + i, n, Role.STATIC, fin).accent(dk).at(0, -len[i] / 2f, w[i] / 2f).rot(-15, 0, 0).mat("membrane")
                    .box(-0.5f, -len[i] / 2f, 0, 1, len[i], 2 + (i % 2)).add();
            if (i % 2 == 1) {
                sym(b, "side_spike" + i, n, false, light(fin, 0.2f)).at(w[i] / 2f, -len[i] / 2f, 1).rot(0, -30, -20).mat("horn")
                        .box(0, -0.5f, -0.5f, 2, 1, 1).add();
            }
            prev = n;
        }
        // head
        part(b, "head", prev, Role.HEAD, skin).accent(eye).at(0, -5, 0).rot(-26, 0, 0).mat("scales").glow(veins)
                .box(-4, -4, -5, 8, 5, 8).box(-4.5f, -3, -2, 9, 3, 5).add();
        part(b, "snout", "head", Role.STATIC, skin).accent(belly).at(0, -1, -5).rot(6, 0, 0).mat("scales")
                .box(-3, -2.5f, -4, 6, 3, 4).box(-2, -3, -5, 4, 2, 2).add();
        part(b, "brow", "head", Role.STATIC, dk).accent(fin).at(0, -4, -4.5f).rot(14, 0, 0).mat("horn")
                .box(-4, -1, -1, 8, 2, 2).add();
        b.part("eyes", "head", Role.STATIC, eye).pivot(0, -2.6f, -5.2f).material("energy").glow("full")
                .box(1.6f, -0.5f, -0.4f, 2, 1, 1).box(-3.6f, -0.5f, -0.4f, 2, 1, 1);
        sideFangs(b, "fangs", "snout", 0xF0ECD8, 2.6f, 0.4f, -3.6f, 1.6f, 2, 2, false);
        part(b, "jaw", "head", Role.JAW, belly).accent(dark(belly, 0.4f)).at(0, 1, -4).rot(14, 0, 0).mat("plates")
                .glow(veins != null && veins.equals("cracks") ? "maw" : null).box(-3.5f, 0, -5, 7, 2, 6).add();
        part(b, "tongue", "jaw", Role.STATIC, 0xB03040).at(0, -0.3f, -5).rot(10, 0, 0).mat("flesh").box(-0.5f, 0, -3, 1, 1, 3).add();
        // fan of fins around the head, a central crest and swept horns
        sym(b, "frill_a", "head", false, fin).accent(dk).at(4, -1.5f, 0).rot(0, -25, -10).mat("membrane")
                .box(0, -3, -0.5f, 6, 6, 1).box(5, -2, -0.5f, 2, 4, 1).add();
        sym(b, "frill_b", "head", false, fin).accent(dk).at(3.5f, -3.5f, 1).rot(-15, -20, -55).mat("membrane")
                .box(0, -2.5f, -0.5f, 7, 5, 1).add();
        sym(b, "frill_c", "head", false, light(fin, 0.1f)).accent(dk).at(3.5f, 0.5f, 1).rot(10, -20, 35).mat("membrane")
                .box(0, -2, -0.5f, 5, 4, 1).add();
        sym(b, "frill_spine", "frill_a", true, light(fin, 0.35f)).at(0, 0, -0.6f).mat("horn").box(0, -0.5f, -0.5f, 8, 1, 1, -0.1f).add();
        part(b, "crest", "head", Role.STATIC, fin).accent(dk).at(0, -4, 0).rot(-30, 0, 0).mat("membrane")
                .box(-0.5f, -6, -2, 1, 6, 6).box(-0.5f, -8, 0, 1, 2, 3).add();
        sym(b, "horn", "head", false, light(belly, 0.25f)).at(2.8f, -4, -1).rot(-55, 15, -10).mat("horn").box(-0.5f, -4, -0.5f, 1, 4, 1, 0.2f).add();
        // tail tip escaping the coils
        String tail = chain(b, "tail", "coil", Role.TAIL, skin, acc, "scales", 7, -2, 6, false,
                new int[]{4, 3, 3, 2}, new int[]{5, 5, 4, 4}, new float[]{0, 6, 8, 10}, new float[]{50, 30, 30, 30});
        part(b, "tail_tip", tail, Role.STATIC, fin).at(0, 0, 4).mat("horn").box(-0.5f, -1.5f, 0, 1, 3, 3).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Alchemical Hydra
    // ------------------------------------------------------------------------------------------------

    /**
     * The Alchemical Hydra: a heavy, low-slung reptile with alchemical vents on its back, a thick tail and
     * {@code heads} serpent heads on short muscular necks (it loses a head each phase).
     */
    static ModelSpec hydra(String id, int body, int belly, int vent, int heads) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.6f);
        int dk = dark(body, 0.35f);
        int horn = 0xD8CCA8;
        part(b, "body", null, Role.BODY, body).accent(belly).at(0, 11, 3).rot(-4, 0, 0).mat("scales")
                .box(-8, -6, -11, 16, 12, 12).box(-7.5f, -5.5f, 1, 15, 11, 10).add();
        part(b, "chest", "body", Role.STATIC, body).accent(belly).at(0, -1, -11).rot(10, 0, 0).mat("scales")
                .box(-7, -5, -2, 14, 10, 3).add();
        Kit.P pl = part(b, "belly", "body", Role.STATIC, belly).accent(dark(belly, 0.3f)).at(0, 6, 0).mat("plates");
        for (int i = 0; i < 4; i++) {
            pl.box(-6, -0.6f + (i % 2) * 0.3f, -11 + i * 5.2f, 12, 1, 5);
        }
        pl.add();
        sym(b, "shoulder", "body", false, body).accent(belly).at(7.5f, -3, -7).rot(0, 0, -10).mat("scales").box(-2, -4, -4, 4, 8, 8).add();
        // alchemical vents: metal collars with glowing chemical mouths
        for (int i = 0; i < 3; i++) {
            float z = -6 + i * 6;
            part(b, "vent" + i, "body", Role.STATIC, 0x5A5650).accent(vent).at(i == 1 ? 0 : (i == 0 ? 2.5f : -2.5f), -6, z)
                    .rot(-15 + i * 5, 0, i == 0 ? 10 : i == 2 ? -10 : 0).mat("metal").box(-1.5f, -3, -1.5f, 3, 3, 3).box(-2, -4, -2, 4, 1, 4).add();
            part(b, "vent_glow" + i, "vent" + i, Role.STATIC, vent).at(0, -4, 0).mat("energy").glow("full").box(-1, -0.6f, -1, 2, 1, 2).add();
        }
        for (int i = 0; i < 4; i++) {
            part(b, "spine" + i, "body", Role.STATIC, horn).at(i % 2 == 0 ? 0.5f : -0.5f, -6, -9 + i * 6 + 3).rot(-35, 0, 0).mat("horn")
                    .box(-0.5f, -3, -1, 1, 3, 2).add();
        }
        // necks and heads
        for (int h = 0; h < heads; h++) {
            String s = String.valueOf(h);
            float off = heads == 1 ? 0 : h - (heads - 1) / 2f;
            float nx = heads == 1 ? 0 : off * 13f / (heads - 1) * (heads > 2 ? 1 : 0.7f);
            float yaw = -off * (heads > 2 ? 18 : 22);
            float lift = -38 + Math.abs(off) * 8;
            part(b, "neck" + s, "body", Role.NECK, body).accent(belly).at(nx, -3, -12).rot(lift, yaw, -off * 6).mat("scales")
                    .box(-2.5f, -2.5f, -7, 5, 5, 8).add();
            part(b, "neck_b" + s, "neck" + s, Role.STATIC, body).accent(belly).at(0, 0, -7).rot(-6, 0, 0).mat("scales")
                    .box(-2, -2, -6, 4, 4, 7).box(-1.5f, 1.6f, -6, 3, 1, 7).add();
            String hd = "head" + s;
            part(b, hd, "neck_b" + s, Role.HEAD, body).accent(vent).at(0, 0, -6).rot(40 - Math.abs(off) * 6, 0, 0).mat("scales")
                    .box(-3, -2.5f, -5, 6, 4, 6).add();
            part(b, "snout" + s, hd, Role.STATIC, body).accent(belly).at(0, -0.5f, -5).rot(5, 0, 0).mat("scales")
                    .box(-2, -1.5f, -4, 4, 2, 4).add();
            b.part("eyes" + s, hd, Role.STATIC, vent).pivot(0, -1.4f, -5.1f).material("energy").glow("full")
                    .box(1.1f, -0.5f, -0.4f, 2, 1, 1).box(-3.1f, -0.5f, -0.4f, 2, 1, 1);
            part(b, "jaw" + s, hd, Role.JAW, belly).accent(dk).at(0, 1.5f, -4).rot(14, 0, 0).mat("plates").box(-2, 0, -4.5f, 4, 1, 5).add();
            sideFangs(b, "fangs" + s, "snout" + s, 0xF0ECD8, 2, 0.5f, -3.6f, 2f, heads > 2 ? 1 : 2, 1, false);
            sym(b, "horn" + s, hd, false, horn).accent(dk).at(2, -2.5f, -0.5f).rot(-60, 20, 0).mat("horn")
                    .box(-0.5f, -4, -0.5f, 1, 4, 1, 0.25f).box(-0.5f, -5, 0, 1, 1, 1).add();
            if (heads <= 2) {
                sym(b, "frill" + s, hd, false, vent).at(3, -0.5f, -1).rot(0, -35, 0).mat("membrane").box(0, -1.5f, -0.5f, 3, 3, 1).add();
            }
        }
        // legs: thick, bowed, with clawed feet
        sym(b, "leg_f", null, false, body).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(belly).at(8, 12, -7).rot(10, 0, -18)
                .mat("scales").box(-2.5f, -2, -3, 5, 7, 6).add();
        sym(b, "shin_f", "leg_f", true, body).accent(belly).at(0, 5, 0).rot(-12, 0, 18).mat("scales").box(-2, 0, -2, 4, 6, 4).add();
        sym(b, "foot_f", "shin_f", true, dk).at(0, 5.5f, 0).rot(2, 0, 0).mat("scales").box(-2.5f, 0, -4, 5, 2, 6).add();
        sym(b, "claws_f", "foot_f", true, horn).at(0, 1, -4).mat("horn").box(-2.5f, -0.5f, -2, 1, 1, 2).box(-0.5f, -0.5f, -2.5f, 1, 1, 3)
                .box(1.5f, -0.5f, -2, 1, 1, 2).add();
        sym(b, "thigh_b", null, false, body).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(belly).at(7.5f, 10, 9).rot(-20, 0, -12)
                .mat("scales").box(-3, -3, -4, 6, 9, 8).add();
        sym(b, "shin_b", "thigh_b", true, body).accent(belly).at(0, 6, 1).rot(40, 0, 12).mat("scales").box(-2, 0, -2, 4, 6, 4).add();
        sym(b, "foot_b", "shin_b", true, dk).at(0, 5.5f, 0).rot(-20, 0, 0).mat("scales").box(-2.5f, 0, -4.5f, 5, 2, 6).add();
        sym(b, "claws_b", "foot_b", true, horn).at(0, 1, -4.5f).mat("horn").box(-2.5f, -0.5f, -2, 1, 1, 2).box(-0.5f, -0.5f, -2.5f, 1, 1, 3)
                .box(1.5f, -0.5f, -2, 1, 1, 2).add();
        // tail
        String tail = chain(b, "tail", "body", Role.TAIL, body, belly, "scales", 0, -1, 11, false,
                new int[]{7, 6, 5, 4, 3}, new int[]{6, 6, 6, 5, 5}, new float[]{-18, -6, 4, 6, 8}, null);
        for (int i = 0; i < 4; i += 2) {
            part(b, "tail_spine" + i, "tail" + i, Role.STATIC, horn).at(0, -3 + i * 0.5f, 3).rot(-40, 0, 0).mat("horn")
                    .box(-0.5f, -2, -0.5f, 1, 2, 2).add();
        }
        part(b, "tail_tip", tail, Role.STATIC, horn).at(0, 0, 5).mat("horn").box(-1, -1, 0, 2, 2, 3).box(-0.5f, -0.5f, 3, 1, 1, 2).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // The Hueycoatl
    // ------------------------------------------------------------------------------------------------

    /** The Hueycoatl: a colossal feathered serpent-dragon rearing from its coils, crested and winged with plumes. */
    static ModelSpec hueycoatl() {
        int scale = 0x2A7A6A;
        int belly = 0xD8B860;
        int gold = 0xE0B040;
        int red = 0xC83A2A;
        int blue = 0x3A6AC8;
        int eye = 0xFFF060;
        ModelSpec.Builder b = ModelSpec.builder("the_hueycoatl").shadow(1.8f);
        // ground coils
        part(b, "coil", null, Role.STATIC, scale).accent(belly).at(0, 24, 4).mat("scales")
                .box(-12, -5, -12, 24, 5, 6).box(-12, -5, 6, 24, 5, 6).box(6, -5, -6, 6, 5, 12).box(-12, -5, -6, 6, 5, 12).add();
        part(b, "coil2", "coil", Role.STATIC, scale).accent(belly).at(0, -5, 0).rot(0, 35, 0).mat("scales")
                .box(-9, -4, -9, 18, 4, 5).box(-9, -4, 4, 18, 4, 5).box(4, -4, -4, 5, 4, 8).box(-9, -4, -4, 5, 4, 8).add();
        part(b, "body", "coil2", Role.BODY, scale).accent(belly).at(0, -3, 0).rot(-8, -35, 0).mat("scales")
                .box(-5, -8, -5, 10, 8, 10).add();
        int[] w = {10, 9, 8, 8, 7, 6};
        int[] len = {8, 7, 7, 6, 6, 6};
        float[] pitch = {-16, -12, -4, 18, 26, 22};
        String prev = "body";
        for (int i = 0; i < w.length; i++) {
            String n = "neck" + i;
            part(b, n, prev, i < 2 ? Role.NECK : Role.STATIC, scale).accent(belly).at(0, i == 0 ? -8 : -len[i - 1], 0).rot(pitch[i], 0, 0)
                    .mat("scales").box(-w[i] / 2f, -len[i], -w[i] / 2f, w[i], len[i], w[i]).add();
            part(b, "nbelly" + i, n, Role.STATIC, belly).accent(dark(belly, 0.35f)).at(0, -len[i] / 2f, -w[i] / 2f).mat("plates")
                    .box(-w[i] / 2f + 1, -len[i] / 2f, -0.6f, w[i] - 2, len[i], 1).add();
            int fc = i % 3 == 0 ? red : i % 3 == 1 ? gold : blue;
            part(b, "plume" + i, n, Role.STATIC, fc).accent(dark(fc, 0.4f)).at(0, -len[i] / 2f, w[i] / 2f).rot(-30, 0, 0).mat("feathers")
                    .box(-1.5f, -len[i] / 2f, 0, 3, len[i], 3 + i % 2).add();
            prev = n;
        }
        // feathered wings sprouting from the upper body
        for (int k = 0; k < 3; k++) {
            int fc = k == 0 ? scale : k == 1 ? blue : red;
            sym(b, "wing" + k, k == 0 ? "neck1" : "wing0", k > 0, fc).roles(k == 0 ? Role.WING_LEFT : Role.STATIC, k == 0 ? Role.WING_RIGHT : Role.STATIC)
                    .accent(gold).at(k == 0 ? 3.5f : 0, k == 0 ? -5 : k * 4, k == 0 ? 2 : k * 0.4f).rot(k == 0 ? -25 : 0, k == 0 ? -50 : 0, k == 0 ? -55 : k * 12)
                    .mat("feathers").box(0, -1, -0.5f, 18 - k * 3, 6, 1).box(16 - k * 3, 0, -0.5f, 5, 4, 1).add();
        }
        // head
        part(b, "head", prev, Role.HEAD, scale).accent(eye).at(0, -6, 0).rot(-34, 0, 0).mat("scales")
                .box(-4.5f, -4, -6, 9, 6, 9).add();
        part(b, "snout", "head", Role.STATIC, scale).accent(belly).at(0, -1, -6).rot(5, 0, 0).mat("scales")
                .box(-3.5f, -2.5f, -6, 7, 3, 6).box(-2.5f, -3.2f, -6, 5, 1, 3).add();
        part(b, "beak", "snout", Role.STATIC, gold).at(0, -1, -6).rot(20, 0, 0).mat("horn").box(-1.5f, -1, -2, 3, 2, 2).add();
        b.part("eyes", "head", Role.STATIC, eye).pivot(0, -2.2f, -6.1f).material("energy").glow("full")
                .box(1.8f, -0.5f, -0.4f, 2, 1, 1).box(-3.8f, -0.5f, -0.4f, 2, 1, 1);
        part(b, "jaw", "head", Role.JAW, belly).accent(dark(belly, 0.4f)).at(0, 2, -5).rot(14, 0, 0).mat("plates")
                .box(-3.5f, 0, -7, 7, 2, 8).add();
        sideFangs(b, "fangs", "snout", 0xF0ECD8, 3.2f, 0.4f, -5.5f, 2.5f, 2, 1, false);
        // feather crest: a crown of long plumes in red, gold and blue
        for (int k = 0; k < 5; k++) {
            int fc = k % 3 == 0 ? red : k % 3 == 1 ? gold : blue;
            float a = (k - 2) * 22f;
            part(b, "crest" + k, "head", Role.STATIC, fc).accent(dark(fc, 0.4f)).at((k - 2) * 1.6f, -4, 1).rot(-55 + Math.abs(k - 2) * 10, 0, -a)
                    .mat("feathers").box(-1, -9 + Math.abs(k - 2), -0.5f, 2, 9 - Math.abs(k - 2), 1).box(-0.5f, -11 + Math.abs(k - 2), -0.5f, 1, 2, 1).add();
        }
        sym(b, "cheek_plume", "head", false, gold).accent(red).at(4.5f, 0, -1).rot(0, -30, -20).mat("feathers").box(0, -1.5f, -0.5f, 5, 3, 1).add();
        // tail leaving the coils
        String tail = chain(b, "tail", "coil", Role.TAIL, scale, belly, "scales", -9, -2, 8, false,
                new int[]{5, 4, 4, 3, 2}, new int[]{6, 6, 5, 5, 4}, new float[]{0, 0, 4, 6, 8}, new float[]{-40, -25, -25, -25, -20});
        part(b, "tail_fan", tail, Role.STATIC, red).accent(gold).at(0, 0, 4).mat("feathers").box(-3, -0.5f, 0, 6, 1, 4).box(-1.5f, -0.5f, 4, 3, 1, 2).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Amoxliatl
    // ------------------------------------------------------------------------------------------------

    /** Amoxliatl: a long, frost-blue axolotl-like beast with feathery gills and a back of glowing ice crystals. */
    static ModelSpec amoxliatl() {
        int hide = 0x6A90B4;
        int belly = 0xB8D4E4;
        int ice = 0xC8F0FF;
        int gill = 0x9AD8F0;
        int dk = dark(hide, 0.4f);
        ModelSpec.Builder b = ModelSpec.builder("amoxliatl").shadow(1.2f);
        part(b, "body", null, Role.BODY, hide).accent(belly).at(0, 15, 1).mat("skin").pattern("spots")
                .box(-6, -5, -10, 12, 9, 12).box(-5.5f, -4.5f, 2, 11, 8, 9).add();
        part(b, "belly", "body", Role.STATIC, belly).accent(dark(belly, 0.3f)).at(0, 4, 0).mat("plates")
                .box(-5, -0.6f, -9, 10, 1, 9).box(-4.5f, -0.3f, 0, 9, 1, 10).add();
        // ice crystals along the back
        int[][] cr = {{-2, -8, 5}, {2, -4, 7}, {-1, 0, 6}, {2, 4, 5}, {-2, 7, 4}, {0, -1, 3}};
        for (int i = 0; i < cr.length; i++) {
            part(b, "crystal" + i, "body", Role.STATIC, ice).accent(0x60D8FF).at(cr[i][0], -5, cr[i][1]).rot(-15 + i * 6, i * 30, cr[i][0] * -8)
                    .mat("ice").glow("full").box(-1, -cr[i][2], -1, 2, cr[i][2], 2).box(-0.5f, -cr[i][2] - 1, -0.5f, 1, 1, 1).add();
        }
        sym(b, "shard", "body", false, ice).at(5.5f, -3, -3).rot(-20, 0, -40).mat("ice").glow("full").box(-1, -4, -1, 2, 4, 2).add();
        // broad flat head with feathery gill stalks
        part(b, "head", "body", Role.HEAD, hide).accent(0x60E0FF).at(0, -1, -10).rot(6, 0, 0).mat("skin").pattern("eyes").glow("eyes")
                .box(-5, -4, -7, 10, 6, 7).add();
        part(b, "snout", "head", Role.STATIC, hide).accent(belly).at(0, 0, -7).mat("skin").box(-4, -2.5f, -2, 8, 4, 2).add();
        part(b, "jaw", "head", Role.JAW, belly).accent(dk).at(0, 2, -6).rot(8, 0, 0).mat("skin").box(-4, 0, -3, 8, 1, 4).add();
        teeth(b, "teeth", "head", 0xE8F4FF, 0, 1.6f, -8.5f, 7, 5, 1, false);
        for (int k = 0; k < 3; k++) {
            sym(b, "gill" + k, "head", false, gill).accent(0xE070A0).at(4.5f, -3 + k * 2, -2).rot(0, -30 + k * 10, -40 + k * 35).mat("feathers")
                    .box(0, -0.5f, -0.5f, 5 - k, 1, 1, 0.2f).box(2, -1.5f, -0.5f, 3 - k, 3, 1).add();
        }
        // splayed legs with webbed hands
        sym(b, "leg_f", null, false, hide).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(belly).at(6, 16, -6).rot(0, 0, -50)
                .mat("skin").box(-1.5f, -1.5f, -1.5f, 3, 6, 3).add();
        sym(b, "fore_f", "leg_f", true, hide).at(0, 5, 0).rot(0, 0, 50).mat("skin").box(-1.5f, 0, -1.5f, 3, 4, 3).add();
        sym(b, "hand_f", "fore_f", true, belly).at(0, 3.5f, 0).mat("skin").box(-2.5f, 0, -3.5f, 5, 1, 5).box(-2.5f, 0, -4.5f, 1, 1, 1)
                .box(-0.5f, 0, -4.5f, 1, 1, 1).box(1.5f, 0, -4.5f, 1, 1, 1).add();
        sym(b, "leg_b", null, false, hide).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(belly).at(5.5f, 16, 8).rot(0, 0, -50)
                .mat("skin").box(-2, -1.5f, -2, 4, 6, 4).add();
        sym(b, "fore_b", "leg_b", true, hide).at(0, 5, 0).rot(0, 0, 50).mat("skin").box(-1.5f, 0, -1.5f, 3, 4, 3).add();
        sym(b, "hand_b", "fore_b", true, belly).at(0, 3.5f, 0).mat("skin").box(-2.5f, 0, -3.5f, 5, 1, 5).box(-2.5f, 0, -4.5f, 1, 1, 1)
                .box(-0.5f, 0, -4.5f, 1, 1, 1).box(1.5f, 0, -4.5f, 1, 1, 1).add();
        // long finned tail
        String tail = chain(b, "tail", "body", Role.TAIL, hide, belly, "skin", 0, -1, 11, false,
                new int[]{7, 6, 5, 4, 3}, new int[]{6, 6, 5, 5, 5}, new float[]{-4, 2, 4, 4, 2}, null);
        for (int i = 0; i < 5; i++) {
            part(b, "tail_fin" + i, "tail" + i, Role.STATIC, gill).accent(dk).at(0, -2, 0).mat("membrane")
                    .box(-0.5f, -2 - (i < 3 ? 1 : 0), 0, 1, 2 + (i < 3 ? 1 : 0), 6 - (i == 4 ? 1 : 0)).add();
        }
        part(b, "tail_tip", tail, Role.STATIC, ice).at(0, 0, 5).mat("ice").glow("full").box(-0.5f, -2, 0, 1, 4, 3).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // The Leviathan
    // ------------------------------------------------------------------------------------------------

    /** The Leviathan: an abyssal crustacean behemoth: a vast barnacled shell, crab legs, great claws and a fanged maw. */
    static ModelSpec leviathan() {
        int shell = 0x2E4E5A;
        int rim = 0x1E323C;
        int flesh = 0x6A8A7A;
        int glow = 0x60F0E0;
        int barn = 0xA8A090;
        ModelSpec.Builder b = ModelSpec.builder("the_leviathan").shadow(2.0f);
        part(b, "shell", null, Role.BODY, shell).accent(glow).at(0, 11, 3).rot(-6, 0, 0).mat("chitin").glow("cracks")
                .box(-12, -8, -12, 24, 12, 25).box(-10, -12, -10, 20, 4, 21).box(-7, -14, -7, 14, 2, 15).add();
        part(b, "rim", "shell", Role.STATIC, rim).accent(glow).at(0, 3, 0).mat("chitin")
                .box(-13, -1, -13, 26, 3, 27).add();
        // ridge of spines and rocky barnacle clusters on the dome
        for (int i = 0; i < 5; i++) {
            part(b, "ridge" + i, "shell", Role.STATIC, rim).accent(glow).at(0, -14, -6 + i * 3.5f).rot(-30, 0, 0).mat("horn")
                    .box(-1, -4 + Math.abs(i - 2), -1, 2, 4 - Math.abs(i - 2), 3).add();
        }
        int[][] bar = {{-8, -9, -6}, {7, -10, -2}, {-6, -11, 6}, {8, -9, 8}, {0, -12, 10}, {-10, -6, 1}, {10, -6, -7}};
        for (int i = 0; i < bar.length; i++) {
            part(b, "barnacle" + i, "shell", Role.STATIC, barn).accent(0x5A5048).at(bar[i][0], bar[i][1], bar[i][2]).rot(i * 13, i * 40, i * 7)
                    .mat("rock").box(-1.5f, -2, -1.5f, 3, 2, 3).box(-1, -3, -1, 2, 1, 2).add();
        }
        sym(b, "vent", "shell", false, glow).at(6, -11, 0).mat("energy").glow("full").box(-1, -0.5f, -3, 2, 1, 6).add();
        // head and maw beneath the front of the shell
        part(b, "head", "shell", Role.HEAD, flesh).accent(glow).at(0, 0, -12).mat("flesh").box(-7, -5, -8, 14, 8, 9).add();
        part(b, "brow", "head", Role.STATIC, rim).accent(glow).at(0, -5, -7).rot(15, 0, 0).mat("chitin").box(-7.5f, -1, -2, 15, 3, 4).add();
        b.part("eyes", "head", Role.STATIC, glow).pivot(0, -3.2f, -8.1f).material("energy").glow("full")
                .box(2, -0.5f, -0.4f, 3, 2, 1).box(-5, -0.5f, -0.4f, 3, 2, 1).box(5.5f, 0.5f, -0.2f, 1, 1, 1).box(-6.5f, 0.5f, -0.2f, 1, 1, 1);
        part(b, "maw", "head", Role.STATIC, 0x3A1418).at(0, 0, -8).mat("flesh").glow("full").box(-5, -1, -0.3f, 10, 3, 1).add();
        teeth(b, "teeth_up", "head", 0xE8E0C8, 0, -1, -8.6f, 10, 6, 2, false);
        part(b, "jaw", "head", Role.JAW, flesh).accent(rim).at(0, 3, -2).rot(12, 0, 0).mat("flesh").box(-6.5f, 0, -8, 13, 3, 8).add();
        teeth(b, "teeth_lo", "jaw", 0xE8E0C8, 0, 0, -7.5f, 11, 6, 2, true);
        sym(b, "mandible", "head", false, rim).at(6.5f, 2, -6).rot(0, 25, 0).mat("horn").box(-1, -1, -6, 2, 2, 6).box(-1.5f, -1.5f, -7, 1, 1, 2).add();
        sym(b, "feeler", "head", false, flesh).roles(Role.TENTACLE, Role.TENTACLE).at(4, 1, -8).rot(30, -20, 0).mat("flesh")
                .box(-0.5f, 0, -0.5f, 1, 7, 1, 0.2f).add();
        // great claws
        sym(b, "arm", "shell", false, shell).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(glow).at(11, 1, -9).rot(0, -35, 15).mat("chitin")
                .box(0, -2.5f, -2.5f, 9, 5, 5).add();
        sym(b, "forearm", "arm", true, shell).accent(glow).at(9, 0, 0).rot(0, 60, 0).mat("chitin").box(0, -2, -2, 7, 4, 4).add();
        sym(b, "claw", "forearm", true, rim).accent(glow).at(7, 0, 0).rot(0, 10, 0).mat("chitin").box(0, -3.5f, -3, 6, 4, 6).box(5, -3, -2.5f, 3, 2, 5).add();
        sym(b, "pincer", "claw", true, rim).at(0, 0.5f, 0).rot(0, 0, 12).mat("horn").box(0, 0, -2.5f, 8, 2, 5).add();
        // six crab legs
        for (int i = 0; i < 3; i++) {
            sym(b, "leg" + i, "shell", false, shell).roles(Role.LEG_EXTRA, Role.LEG_EXTRA).accent(glow).at(12, 2, -3 + i * 6)
                    .rot(0, (i - 1) * -25f, -25).mat("chitin").box(0, -1.5f, -1.5f, 8, 3, 3).add();
            sym(b, "shin" + i, "leg" + i, true, rim).at(8, 0, 0).rot(0, 0, 70).mat("chitin").box(0, -1, -1, 9, 2, 2).box(8, -0.5f, -0.5f, 2, 1, 1).add();
        }
        // armoured tail with a fluke
        String tail = chain(b, "tail", "shell", Role.TAIL, shell, glow, "chitin", 0, 0, 12, false,
                new int[]{8, 7, 6, 5}, new int[]{6, 6, 5, 5}, new float[]{-10, -5, 0, 4}, null);
        part(b, "fluke", tail, Role.STATIC, rim).at(0, 0, 5).mat("chitin").box(-6, -0.5f, 0, 12, 1, 4).box(-4, -0.5f, 4, 8, 1, 2).add();
        return b.build();
    }
}
