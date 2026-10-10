package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.BestiarySlmKit.*;

/**
 * OSRS dragons for the slayer module: a true dragon silhouette - four clawed legs, a deep chest, huge membrane
 * wings on finger bones, a long S-curved neck, a horned and frilled head with a toothed jaw, back spines and a
 * long tapering spaded tail - with hide that matches the colour (scaled chromatic, heavier brutal, plated
 * metal, molten lava, rune-etched rune). Baby dragons reuse these models at a smaller monster scale.
 */
final class BestiarySlmDragons {
    static final int CHROMATIC = 0;
    static final int BRUTAL = 1;
    static final int METAL = 2;
    static final int LAVA = 3;
    static final int RUNE = 4;

    private BestiarySlmDragons() {
    }

    static List<ModelSpec> all() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(dragon("green_dragon", 0x3C8A34, 0xB8C870, 0x2E6E2A, 0xE0D8B8, CHROMATIC));
        l.add(dragon("blue_dragon", 0x3456B0, 0x9AB0E0, 0x26408A, 0xE0D8B8, CHROMATIC));
        l.add(dragon("red_dragon", 0xB0302A, 0xE8B070, 0x8A2020, 0xE0D8B8, CHROMATIC));
        l.add(dragon("black_dragon", 0x2A2828, 0x5E4E4A, 0x1C1A1A, 0xB0A090, CHROMATIC));
        l.add(dragon("brutal_green_dragon", 0x2C6626, 0x8AA050, 0x1E4A1C, 0x4A4636, BRUTAL));
        l.add(dragon("brutal_blue_dragon", 0x26448E, 0x7088C0, 0x1A2E6A, 0x4A4636, BRUTAL));
        l.add(dragon("brutal_red_dragon", 0x8E2824, 0xC88A50, 0x6A1A18, 0x4A4636, BRUTAL));
        l.add(dragon("brutal_black_dragon", 0x1E1C1C, 0x4A3A3A, 0x121010, 0x7A2420, BRUTAL));
        l.add(dragon("lava_dragon", 0x3A2622, 0xFF7A1E, 0x4A2218, 0x1E1614, LAVA));
        l.add(dragon("bronze_dragon", 0x9A6A34, 0xD0A468, 0x7A5028, 0xE8C888, METAL));
        l.add(dragon("iron_dragon", 0x6E6A68, 0xA8A6A2, 0x52504E, 0xC8C8C8, METAL));
        l.add(dragon("steel_dragon", 0x9C9EA4, 0xD0D6DE, 0x74787E, 0xE8EEF4, METAL));
        l.add(dragon("mithril_dragon", 0x4A5888, 0x8494C0, 0x36446E, 0xB8C8E8, METAL));
        l.add(dragon("adamant_dragon", 0x44684A, 0x74A884, 0x30503A, 0xA8E8BC, METAL));
        l.add(dragon("rune_dragon", 0x3A8298, 0x78BCD0, 0x2A6478, 0x90F0FF, RUNE));
        return l;
    }

    /**
     * A full dragon. {@code body}/{@code belly}/{@code wing}/{@code horn} colour the hide, the ventral plates, the
     * wing membranes and the horns/spines/claws.
     */
    static ModelSpec dragon(String id, int body, int belly, int wing, int horn, int kind) {
        boolean brutal = kind == BRUTAL;
        boolean metal = kind == METAL || kind == RUNE;
        boolean lava = kind == LAVA;
        String hide = lava ? "rock" : metal ? "plates" : "scales";
        String armour = lava ? "rock" : metal ? "metal" : "scales";
        String veins = lava ? "cracks" : kind == RUNE ? "runes" : null;
        int glowAcc = lava ? 0xFF8A1E : 0x8AF4FF;
        int acc = veins != null ? glowAcc : belly;
        int dark = darker(body, 0.7f);
        int spine = brutal ? darker(horn, 1.1f) : lava ? 0x2A1A16 : metal ? darker(horn, 0.85f) : darker(body, 0.55f);
        int eye = metal ? 0x70E8FF : lava ? 0xFFB040 : brutal ? 0xFF4A20 : 0xFFD040;
        int bw = brutal ? 14 : 12;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.3f);

        // ---------------- torso ----------------
        Kit.part(b, "body", null, Role.BODY, body).accent(acc).at(0, 11, 1).mat(hide).glow(veins)
                .box(-bw / 2f, -5, -10, bw, 10, 20).add();
        st(b, "chest", "body", body).accent(acc).at(0, -1, -9).rot(-12, 0, 0).mat(armour).glow(veins)
                .box(-bw / 2f + 0.5f, -4, -5, bw - 1, 10, 6).add();
        st(b, "back", "body", darker(body, 0.9f)).accent(acc).at(0, -5, 0).mat(hide)
                .box(-bw / 2f + 1.5f, -1.5f, -9, bw - 3, 2, 17).add();
        Kit.P plates = st(b, "belly", "body", belly).accent(darker(belly, 0.7f)).at(0, 5, 0).mat("plates");
        for (int i = 0; i < 3; i++) {
            plates.box(-bw / 2f + 1.5f, -0.5f + (i == 0 ? -0.5f : 0), -9.5f + i * 6.4f, bw - 3, 1, 6, 0.05f);
        }
        plates.add();
        st(b, "chest_plate", "chest", belly).accent(darker(belly, 0.7f)).at(0, 4, -5).rot(-8, 0, 0).mat("plates")
                .box(-3.5f, -4, -0.6f, 7, 7, 1).add();
        sy(b, "shoulder", "body", body).accent(acc).at(bw / 2f - 0.5f, -1, -6.5f).rot(0, 0, -6).mat(armour).glow(veins)
                .box(-2, -4, -4, 4, 8, 8).add();
        sy(b, "haunch", "body", body).accent(acc).at(bw / 2f - 0.5f, -0.5f, 5.5f).rot(0, 0, -5).mat(armour).glow(veins)
                .box(-2, -4.5f, -4.5f, 4, 9, 9).add();
        int spines = 5;
        for (int i = 0; i < spines; i++) {
            int hgt = brutal ? 4 : 3;
            st(b, "spine" + i, "body", spine).at(0, -6, -7.5f + i * 3.8f).rot(-28, 0, 0).mat("horn")
                    .box(-0.5f, -hgt, -1.5f, 1, hgt + 1, 3, i % 2 == 0 ? 0.1f : 0).add();
        }

        // ---------------- neck (S-curve) and head ----------------
        int[] nw = brutal ? new int[]{8, 7, 6, 6} : new int[]{7, 6, 6, 5};
        int[] nl = {5, 5, 5, 4};
        float[] np = {-62, 0, 14, 20};
        String prev = "body";
        for (int i = 0; i < 4; i++) {
            String n = "neck" + i;
            Kit.part(b, n, prev, i == 0 ? Role.NECK : Role.STATIC, body).accent(acc)
                    .at(0, i == 0 ? -3.5f : 0, i == 0 ? -10 : -nl[i - 1] + 0.5f).rot(np[i], 0, 0).mat(hide).glow(i < 2 ? veins : null)
                    .box(-nw[i] / 2f, -nw[i] / 2f, -nl[i] - 0.5f, nw[i], nw[i], nl[i] + 1).add();
            st(b, n + "_plate", n, belly).accent(darker(belly, 0.7f)).at(0, nw[i] / 2f, 0).mat("plates")
                    .box(-nw[i] / 2f + 1, -0.6f, -nl[i] - 0.5f, nw[i] - 2, 1, nl[i] + 1).add();
            if (i % 2 == 1) {
                st(b, n + "_spine", n, spine).at(0, -nw[i] / 2f, -nl[i] / 2f).rot(-35, 0, 0).mat("horn")
                        .box(-0.5f, -(brutal ? 3 : 2), -1, 1, brutal ? 3 : 2, 2).add();
            }
            prev = n;
        }
        Kit.part(b, "head", "neck3", Role.HEAD, body).accent(eye).at(0, 0, -4).rot(48, 0, 0).mat(armour).pattern("eyes").glow("eyes")
                .box(-4, -3.5f, -6, 8, 6, 7).add();
        st(b, "snout", "head", body).accent(acc).at(0, 0, -6).rot(4, 0, 0).mat(hide).box(-3, -2, -6, 6, 4, 6).add();
        st(b, "snout_ridge", "snout", dark).at(0, -2, -0.5f).rot(6, 0, 0).mat(armour).box(-2, -1, -5, 4, 1, 5).add();
        sy(b, "nostril", "snout", darker(body, 0.4f)).at(1.6f, -1.6f, -5.4f).mat("horn").box(-0.5f, -0.5f, -0.5f, 1, 1, 1).add();
        teeth(b, "teeth_up", "snout", TOOTH, 2, -5.6f, 2.6f, 3, true, false);
        Kit.part(b, "jaw", "head", Role.JAW, belly).accent(darker(belly, 0.7f)).at(0, 2.5f, -5).rot(10, 0, 0).mat("plates")
                .glow(veins != null ? "maw" : null).box(-2.5f, 0, -6.5f, 5, 2, 7).add();
        teeth(b, "teeth_lo", "jaw", TOOTH, 0, -6.2f, 2.3f, 2, false, false);
        sy(b, "brow", "head", dark).at(2.7f, -3.5f, -5.5f).rot(14, -8, -10).mat(armour).box(-1.5f, -1, -1, 3, 2, 5).add();
        sy(b, "cheek", "head", body).accent(acc).at(4, 0.5f, -3).rot(0, -10, 0).mat(armour).box(-1, -2, -2.5f, 2, 4, 5).add();
        // ear frills (chromatic/brutal/lava) or a crown of blades (metal)
        if (metal) {
            for (int k = 0; k < 4; k++) {
                float off = k - 1.5f;
                st(b, "crown" + k, "head", horn).at(off * 1.8f, -3.5f, -0.5f + Math.abs(off)).rot(-40, 0, off * 14).mat("horn")
                        .box(-0.5f, -(k == 1 || k == 2 ? 5 : 4), -0.5f, 1, k == 1 || k == 2 ? 5 : 4, 1, 0.1f).add();
            }
            sy(b, "fin", "head", wing).accent(horn).at(4, -1.5f, -0.5f).rot(-15, -40, -10).mat("membrane")
                    .box(0, -2, 0, 1, 4, 5).add();
            sy2(b, "fin_rib", "fin", horn).at(0.5f, -2, 0).rot(-30, 0, 0).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        } else {
            sy(b, "frill", "head", wing).accent(horn).at(4, -1, -0.5f).rot(-10, -35, -15).mat("membrane")
                    .box(0, -3, 0, 1, 5, 5).add();
            sy2(b, "frill_rib", "frill", horn).at(0.5f, -3, 0.5f).rot(-50, 0, 0).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
            int[] th = brutal ? new int[]{3, 2, 1} : new int[]{2, 2, 1};
            int[] ln = brutal ? new int[]{4, 4, 4} : new int[]{4, 4, 3};
            horn(b, "horn", "head", true, false, horn, 2.6f, -3.2f, -1.5f, -62, 16, -8, th, ln, -14, 4);
            if (brutal) {
                st(b, "nose_horn", "snout", horn).at(0, -2, -4).rot(-25, 0, 0).mat("horn")
                        .box(-1, -3, -1, 2, 3, 2).box(-0.5f, -5, -0.5f, 1, 2, 1).add();
                sy(b, "jaw_spike", "jaw", horn).at(2.5f, 1, -1).rot(20, -30, -40).mat("horn").box(-0.5f, -0.5f, 0, 1, 1, 3).add();
            }
        }

        // ---------------- wings: arm, forearm, three finger bones, membrane panels ----------------
        int bone = lava ? 0x2A1A16 : metal ? horn : darker(wing, 0.75f);
        Kit.sym(b, "wing", "body", false, bone).roles(Role.WING_LEFT, Role.WING_RIGHT).accent(horn).at(bw / 2f - 1, -4.5f, -6)
                .rot(0, -8, -46).mat(armour).box(0, -1.5f, -1.5f, 10, 3, 3).add();
        sy2(b, "wing_web0", "wing", wing).accent(bone).at(0, 0, 1).mat("membrane").glow(lava ? "cracks" : null)
                .box(0, -0.25f, 0, 10, 1, 9, -0.25f).add();
        sy2(b, "wing_fore", "wing", bone).accent(horn).at(10, 0, 0).rot(0, 30, 4).mat(armour).box(0, -1, -1, 11, 2, 2).add();
        sy2(b, "wing_web1", "wing_fore", wing).accent(bone).at(0, 0, 0.5f).mat("membrane").box(0, -0.25f, 0, 11, 1, 8, -0.25f).add();
        sy2(b, "wing_thumb", "wing_fore", horn).at(11, -1, -0.5f).rot(0, 40, -30).mat("horn").box(0, -0.5f, -0.5f, 3, 1, 1).add();
        float[] fy = {-40, -78, -116};
        int[] fl = {16, 15, 12};
        for (int f = 0; f < 3; f++) {
            String fn = "wing_finger" + f;
            sy2(b, fn, "wing_fore", bone).accent(horn).at(10.5f, 0, 0).rot(0, fy[f], 14 + f * 6).mat("horn")
                    .box(0, -0.5f, -0.5f, fl[f], 1, 1, 0.15f).add();
            int web = f == 2 ? 7 : (int) Math.round(fl[f] * 0.62);
            sy2(b, fn + "_web", fn, wing).accent(bone).at(0, 0, 0.3f).rot(0, f == 2 ? 0 : -6, 0).mat("membrane")
                    .glow(lava ? "cracks" : null).box(0, -0.25f, 0, fl[f] - 1, 1, web, -0.25f).add();
        }

        // ---------------- legs ----------------
        sym(b, "leg_f", body, acc, armour, veins, true, bw);
        sym(b, "leg_b", body, acc, armour, veins, false, bw);
        for (String leg : new String[]{"leg_f", "leg_b"}) {
            claws(b, leg + "_claws", leg + "_foot", true, horn, 3, 3, 0.6f, -3.2f, 2);
        }

        // ---------------- tail ----------------
        int[] tw = brutal ? new int[]{7, 6, 5, 4, 3, 3, 2} : new int[]{6, 5, 5, 4, 3, 3, 2};
        int[] tl = {5, 5, 5, 4, 4, 4, 4};
        float[] tp = {14, -4, -4, -2, 2, 4, 5};
        float[] ty = {0, 8, 7, 4, -5, -8, -6};
        String last = chain(b, "tail", "body", Role.TAIL, body, acc, hide, null, 0, -1.5f, 9.5f, 1, tw, tw, tl, tp, ty);
        for (int i = 0; i < 5; i += 1) {
            if (i % 2 == 1 && !brutal) {
                continue;
            }
            st(b, "tail_spine" + i, "tail" + i, spine).at(0, -tw[i] / 2f, 2).rot(-35, 0, 0).mat("horn")
                    .box(-0.5f, -(tw[i] > 4 ? 3 : 2), -1, 1, tw[i] > 4 ? 3 : 2, 2).add();
        }
        st(b, "tail_spade", last, metal ? horn : spine).at(0, 0, 4).mat("horn")
                .box(-0.5f, -0.5f, 0, 1, 1, 5).box(-2, -0.5f, 1, 4, 1, 2, -0.05f).add();
        if (veins != null) {
            // molten / rune-lit eyes of the hide read better with a glowing throat
            st(b, "throat_glow", "neck1", glowAcc).accent(glowAcc).at(0, nw[1] / 2f - 0.2f, -nl[1] / 2f).mat("energy").glow("full")
                    .box(-1.5f, -0.5f, -2, 3, 1, 4, -0.1f).add();
        }
        return b.build();
    }

    /** Front ({@code front}) or hind pair of dragon legs ending in a three-toed foot named "{name}_foot". */
    private static void sym(ModelSpec.Builder b, String name, int body, int acc, String hide, String veins, boolean front, int bw) {
        int dark = darker(body, 0.72f);
        float x = bw / 2f - 0.5f;
        if (front) {
            Kit.sym(b, name, null, false, body).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(acc).at(x, 13, -7).rot(10, 0, -4)
                    .mat(hide).glow(veins).box(-3, -2, -3, 6, 7, 6).add();
            sy2(b, name + "_shin", name, body).accent(acc).at(0, 5, 0).rot(-18, 0, 4).mat(hide).box(-2.5f, 0, -2.5f, 5, 5, 5).add();
            sy2(b, name + "_foot", name + "_shin", dark).at(0, 5, 0).rot(8, 0, 0).mat(hide)
                    .box(-2.5f, 0, -3.5f, 5, 1, 5).add();
        } else {
            Kit.sym(b, name, null, false, body).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(acc).at(x, 11.5f, 6.5f).rot(-26, 0, -4)
                    .mat(hide).glow(veins).box(-3.5f, -3, -4, 7, 9, 8).add();
            sy2(b, name + "_shin", name, body).accent(acc).at(0, 5.5f, -0.5f).rot(58, 0, 4).mat(hide).box(-2.5f, 0, -2.5f, 5, 6, 5).add();
            sy2(b, name + "_ankle", name + "_shin", dark).at(0, 5.5f, 0).rot(-32, 0, 0).mat(hide).box(-1.5f, 0, -1.5f, 3, 4, 3).add();
            sy2(b, name + "_foot", name + "_ankle", dark).at(0, 3.5f, 0).rot(0, 0, 0).mat(hide)
                    .box(-2.5f, 0, -3.5f, 5, 1, 5).add();
            sy2(b, name + "_spur", name + "_ankle", darker(body, 0.5f)).at(0, 1, 1.5f).rot(-40, 0, 0).mat("horn").box(-0.5f, -0.5f, 0, 1, 1, 2).add();
        }
    }
}
