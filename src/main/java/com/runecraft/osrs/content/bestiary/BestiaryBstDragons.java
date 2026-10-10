package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.BestiaryBst.dark;
import static com.runecraft.osrs.content.bestiary.BestiaryBst.light;
import static com.runecraft.osrs.content.bestiary.BestiaryBst.sideFangs;
import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/**
 * The great dragons: the King Black Dragon (three heads), Vorkath (undead, in both its boss and Dragon Slayer II
 * forms), Elvarg and Galvek. One sculpt, styled per dragon: a deep-chested, plated body on four clawed legs, long
 * segmented necks with fanged jaws and swept horns, huge bat wings with finger bones and layered membranes and a
 * spined tail.
 */
final class BestiaryBstDragons {
    private BestiaryBstDragons() {
    }

    static final int KBD = 0;
    static final int UNDEAD = 1;
    static final int ELVARG = 2;
    static final int GALVEK = 3;

    static List<ModelSpec> all() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(dragon("king_black_dragon", KBD, 3, 1.0f));
        l.add(dragon("vorkath", UNDEAD, 1, 1.0f));
        l.add(dragon("ds2_vorkath", UNDEAD, 1, 1.6f));
        l.add(dragon("elvarg", ELVARG, 1, 1.9f));
        l.add(dragon("galvek", GALVEK, 1, 2.4f));
        return l;
    }

    static ModelSpec dragon(String id, int style, int heads, float scale) {
        int body;
        int belly;
        int membrane;
        int horn;
        int eye;
        String hide = "scales";
        String veins = null;
        int veinCol = 0xFF8A20;
        switch (style) {
            case UNDEAD -> {
                body = 0x5A5848;
                belly = 0x7A7460;
                membrane = 0x3A3830;
                horn = 0xD8D2BC;
                eye = 0x60E0FF;
                hide = "flesh";
                veins = "cracks";
                veinCol = 0x70E8FF;
            }
            case ELVARG -> {
                body = 0x4E5E2A;
                belly = 0x9A9A5A;
                membrane = 0x3A4A22;
                horn = 0x2A2620;
                eye = 0xFFC030;
            }
            case GALVEK -> {
                body = 0x3A2224;
                belly = 0x7A3A26;
                membrane = 0x8A2A1A;
                horn = 0x2A1E1E;
                eye = 0xFFA030;
                veins = "cracks";
                veinCol = 0xFF7A1E;
            }
            default -> {
                body = 0x262428;
                belly = 0x4E4040;
                membrane = 0x1E1C22;
                horn = 0x9A8A78;
                eye = 0xFF5A1E;
            }
        }
        boolean undead = style == UNDEAD;
        boolean multi = heads > 1;
        int bone = 0xD8D2BC;
        int dk = dark(body, 0.35f);
        int vacc = veins != null ? veinCol : belly;
        int claw = undead ? bone : dark(horn, 0.2f);
        ModelSpec.Builder b = ModelSpec.builder(id).scale(scale).shadow(1.6f);

        // ---------------- torso ----------------
        part(b, "body", null, Role.BODY, body).accent(veins != null ? veinCol : belly).at(0, 9, 1).rot(-5, 0, 0).mat(hide).glow(veins)
                .box(-7, -6, -12, 14, 12, 12).box(-6.5f, -5.5f, 0, 13, 11, 11).add();
        Kit.P chest = part(b, "chest", "body", Role.STATIC, body).accent(belly).at(0, 0, -12).rot(8, 0, 0).mat(hide)
                .box(-6, -5, -2, 12, 10, 3);
        (multi ? chest : chest.box(-4.5f, -3, -3, 9, 7, 1)).add();
        Kit.P plates = part(b, "belly", "body", Role.STATIC, belly).accent(dark(belly, 0.3f)).at(0, 6, 0).mat("plates");
        int np = multi ? 2 : 5;
        for (int i = 0; i < np; i++) {
            int w = i == 0 || i == np - 1 ? 9 : 11;
            plates.box(-w / 2f, -0.6f + (i % 2) * 0.3f, -12 + i * 22f / np, w, 1, 22 / np);
        }
        plates.add();
        sym(b, "shoulder", "body", false, body).accent(vacc).at(6.5f, -3, -8).rot(0, 0, -10).mat(hide).glow(veins)
                .box(-2, -4, -4, 4, 8, 8).add();
        if (!multi) {
            sym(b, "haunch", "body", false, body).accent(belly).at(6, -2, 6).rot(0, 0, -6).mat(hide)
                    .box(-2, -5, -5, 4, 10, 10).add();
        }
        // back spines, tallest over the shoulders
        int[] sp = {3, 5, 6, 5, 4, 4, 3};
        for (int i = 0; i < sp.length; i += multi ? 2 : 1) {
            part(b, "spine" + i, "body", Role.STATIC, undead ? bone : horn).accent(dk).at(0, -6, -10 + i * 3.4f).rot(-38, 0, 0)
                    .mat(undead ? "bone" : "horn").box(-1, -sp[i], -1, 2, sp[i], 2).add();
        }
        if (undead) {
            // exposed ribs and spine plates on a rotting hide
            for (int i = 0; i < 3; i++) {
                sym(b, "rib" + i, "body", false, bone).at(6.6f, -4, -8 + i * 5f).rot(0, 0, -6 - i).mat("bone")
                        .box(0, 0, -1, 1, 9, 2).add();
            }
            part(b, "spine_bones", "body", Role.STATIC, bone).at(0, -6.2f, 0).mat("bone")
                    .box(-2, -1, -11, 4, 2, 5).box(-1.5f, -1, -2, 3, 2, 6).add();
        }

        // ---------------- necks and heads ----------------
        for (int h = 0; h < heads; h++) {
            String s = multi ? String.valueOf(h) : "";
            float off = multi ? h - (heads - 1) / 2f : 0;
            boolean full = off == 0;
            float yaw = -off * 32f;
            float nx = off * 5f;
            float lift = full ? -34 : -24;
            part(b, "neck" + s, "body", Role.NECK, body).accent(vacc).at(nx, -3, -13).rot(lift, yaw, -off * 10).mat(hide).glow(veins)
                    .box(-3, -3, -6, 6, 6, 7).add();
            part(b, "neck_b" + s, "neck" + s, Role.STATIC, body).accent(belly).at(0, 0, -6).rot(-12, 0, 0).mat(hide)
                    .box(-2.5f, -2.5f, -6, 5, 5, 7).add();
            part(b, "neck_c" + s, "neck_b" + s, Role.STATIC, body).accent(belly).at(0, 0, -6).rot(12, 0, 0).mat(hide)
                    .box(-2.5f, -2.5f, -5, 5, 5, 6).add();
            if (!multi) {
                part(b, "neck_plates" + s, "neck" + s, Role.STATIC, belly).accent(dark(belly, 0.3f)).at(0, 3, 0).mat("plates")
                        .box(-2, -0.2f, -6, 4, 1, 6).add();
                part(b, "neck_spines" + s, "neck_b" + s, Role.STATIC, undead ? bone : horn).at(0, -2.5f, -1).rot(-40, 0, 0).mat("horn")
                        .box(-0.5f, -3, -0.5f, 1, 3, 2).box(-0.5f, -2, 3.5f, 1, 2, 2).add();
            }
            String head = "head" + s;
            int skull = undead ? bone : body;
            part(b, head, "neck_c" + s, Role.HEAD, skull).accent(eye).at(0, 0, -5).rot(full ? 34 : 24, 0, 0).mat(undead ? "bone" : hide)
                    .box(-3.5f, -3, -6, 7, 5, 7).add();
            part(b, "snout" + s, head, Role.STATIC, skull).accent(belly).at(0, -0.5f, -6).rot(4, 0, 0).mat(undead ? "bone" : hide)
                    .box(-2.5f, -1.5f, -6, 5, 3, 6).box(-2, -2.3f, -6, 4, 1, 3).add();
            if (full) {
                sym(b, "brow" + s, head, false, undead ? light(bone, 0.1f) : dk).accent(horn).at(2.6f, -3, -5.5f).rot(12, -6, -14)
                        .mat("horn").box(-1, -1, 0, 2, 2, 5).add();
            }
            b.part("eyes" + s, head, Role.STATIC, eye).pivot(0, -1.6f, -6.1f).material("energy").glow("full")
                    .box(1.4f, -0.5f, -0.4f, 2, 1, 1).box(-3.4f, -0.5f, -0.4f, 2, 1, 1);
            if (full || !multi) {
                sideFangs(b, "fangs_up" + s, "snout" + s, 0xEEE6CC, 2.4f, 1.4f, -5.6f, 2.4f, 2, 1, false);
            }
            part(b, "jaw" + s, head, Role.JAW, undead ? bone : belly).accent(dark(belly, 0.4f)).at(0, 2, -5).rot(10, 0, 0)
                    .mat(undead ? "bone" : "plates").glow(veins != null && !undead ? "maw" : null).box(-2.5f, 0, -7, 5, 2, 8).add();
            sideFangs(b, "fangs_lo" + s, "jaw" + s, 0xEEE6CC, 2.3f, 0, -6.6f, 2.4f, full ? 2 : 1, 1, true);
            if (undead) {
                part(b, "breath" + s, "jaw" + s, Role.STATIC, 0x70E8FF).at(0, -0.2f, -3).mat("energy").glow("full")
                        .box(-1.5f, -1, -3, 3, 1, 4).add();
            }
            float hw = style == KBD ? 1.5f : 1;
            sym(b, "horn" + s, head, false, horn).accent(dk).at(2.4f, -2.8f, -0.5f).rot(-62, 22, 0).mat("horn")
                    .box(-hw, -5, -hw, (int) (hw * 2), 5, (int) (hw * 2)).add();
            Kit.Sym hb = sym(b, "horn_b" + s, "horn" + s, true, light(horn, 0.12f)).at(0, -5, 0).rot(-24, 0, 4).mat("horn")
                    .box(-1, -4, -1, 2, 4, 2, -0.25f);
            (full ? hb.box(-0.5f, -6, -0.5f, 1, 2, 1) : hb).add();
            if (full && !undead) {
                sym(b, "cheek" + s, head, false, horn).accent(horn).at(3.4f, 0.5f, -2).rot(-10, -40, -10)
                        .mat("horn").box(0, -1, -0.5f, 4, 2, 1).add();
            }
            if (!multi && !undead) {
                part(b, "crest" + s, head, Role.STATIC, horn).at(0, -3, -2).rot(-30, 0, 0).mat("horn").box(-0.5f, -2, -2, 1, 2, 4).add();
            }
            if (style == ELVARG || style == GALVEK) {
                sym(b, "horn_lo" + s, head, false, horn).at(3.2f, -0.5f, -1).rot(-80, 40, 0).mat("horn")
                        .box(-0.5f, -4, -0.5f, 1, 4, 1, 0.15f).add();
            }
            if (style == GALVEK) {
                part(b, "nose_horn" + s, "snout" + s, Role.STATIC, horn).at(0, -2.2f, -4.5f).rot(-25, 0, 0).mat("horn")
                        .box(-0.5f, -3, -0.5f, 1, 3, 1, 0.2f).add();
            }
        }

        // ---------------- wings ----------------
        int wbone = undead ? bone : dark(body, 0.1f);
        String wmat = undead ? "bone" : "horn";
        sym(b, "wing", "body", false, wbone).roles(Role.WING_LEFT, Role.WING_RIGHT).accent(horn).at(6.5f, -5, -6).rot(-4, -10, -40)
                .mat(wmat).box(-1, -1.5f, -1.5f, 12, 3, 3).add();
        sym(b, "wing_fore", "wing", true, wbone).accent(horn).at(11, 0, 0).rot(0, 34, 18).mat(wmat)
                .box(0, -1, -1, 12, 2, 2).add();
        if (!multi) {
            sym(b, "wing_thumb", "wing_fore", true, claw).at(12, -0.5f, -0.5f).rot(0, 30, -20).mat("horn")
                    .box(0, -0.5f, -0.5f, 3, 1, 1, 0.2f).add();
        }
        float[] fy = {-26, -58, -94};
        int[] fl = {15, 14, 12};
        for (int k = 0; k < 3; k++) {
            sym(b, "finger" + k, "wing_fore", true, wbone).at(12, 0, 0).rot(0, fy[k], 6 + k * 4).mat(wmat)
                    .box(0, -0.5f, -0.5f, fl[k], 1, 1, 0.25f).add();
        }
        // membranes: an inner panel along the arm and panels trailing each finger (offset so they never z-fight)
        sym(b, "web_arm", "wing", true, membrane).accent(horn).at(0, 0.2f, 0).mat("membrane")
                .box(0, -0.5f, 0, 11, 1, 12, -0.3f).add();
        sym(b, "web_fore", "wing_fore", true, membrane).accent(horn).at(0, 0.35f, 0).mat("membrane")
                .box(0, -0.5f, 0, 12, 1, 11, -0.3f).add();
        int[] fw = multi ? new int[]{9, 13, 9} : new int[]{8, 9, 9};
        for (int k = 0; k < (multi ? 2 : 3); k++) {
            Kit.Sym web = sym(b, "web" + k, "finger" + k, true, membrane).accent(horn).at(0, 0.1f + k * 0.12f, 0).mat("membrane");
            if (undead && k < 2) {
                web.box(0, -0.5f, 0, fl[k] - 6, 1, fw[k], -0.3f).box(fl[k] - 5, -0.5f, 0, 4, 1, fw[k] - 4, -0.3f);
            } else {
                web.box(0, -0.5f, 0, fl[k] - 1, 1, fw[k], -0.3f);
            }
            web.add();
        }

        // ---------------- legs ----------------
        sym(b, "leg_f", null, false, body).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(vacc).at(6, 11, -8).rot(12, 0, -6)
                .mat(hide).glow(veins).box(-2.5f, -2, -3, 5, 8, 6).add();
        sym(b, "shin_f", "leg_f", true, body).accent(belly).at(0, 6, 0.5f).rot(-22, 0, 6).mat(hide).box(-2, 0, -2, 4, 6, 4).add();
        sym(b, "foot_f", "shin_f", true, dk).accent(belly).at(0, 5.5f, 0).rot(10, 0, 0).mat(hide).box(-2.5f, 0, -4, 5, 2, 6).add();
        claws(b, "claws_f", "foot_f", claw, -4, multi);
        sym(b, "thigh_b", null, false, body).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(vacc).at(6.5f, 8, 7).rot(-28, 0, -6)
                .mat(hide).glow(veins).box(-3, -3, -4, 6, 10, 8).add();
        sym(b, "shin_b", "thigh_b", true, body).accent(belly).at(0, 6.5f, 1).rot(62, 0, 4).mat(hide).box(-2, 0, -2, 4, 6, 4).add();
        sym(b, "meta_b", "shin_b", true, dk).accent(belly).at(0, 5.5f, 0).rot(-48, 0, 0).mat(hide).box(-1.5f, 0, -1.5f, 3, 4, 3).add();
        sym(b, "foot_b", "meta_b", true, dk).accent(belly).at(0, 3.5f, 0).rot(12, 0, 0).mat(hide).box(-2.5f, 0, -4.5f, 5, 2, 6).add();
        claws(b, "claws_b", "foot_b", claw, -4.5f, multi);

        // ---------------- tail ----------------
        int[] tw = {6, 5, 4, 4, 3, 2};
        int[] tl = {7, 7, 6, 6, 6, 5};
        float[] tp = {-16, -8, -2, 4, 8, 10};
        String prev = "body";
        for (int i = 0; i < tw.length; i++) {
            String n = "tail" + i;
            part(b, n, prev, Role.TAIL, body).accent(vacc).at(0, i == 0 ? -2 : 0, i == 0 ? 10.5f : tl[i - 1]).rot(tp[i], 0, 0)
                    .mat(hide).glow(i < 2 ? veins : null).box(-tw[i] / 2f, -tw[i] / 2f, 0, tw[i], tw[i], tl[i]).add();
            if (i < tw.length - 1 && (!(multi || undead) || i % 2 == 0)) {
                part(b, "tail_spine" + i, n, Role.STATIC, undead ? bone : horn).at(0, -tw[i] / 2f, tl[i] / 2f).rot(-40, 0, 0)
                        .mat("horn").box(-0.5f, -3 + (i > 2 ? 1 : 0), -0.5f, 1, 3 - (i > 2 ? 1 : 0), 2).add();
            }
            prev = n;
        }
        if (undead) {
            part(b, "tail_club", prev, Role.STATIC, bone).at(0, 0, 5).mat("bone").box(-1.5f, -1.5f, 0, 3, 3, 3)
                    .box(-3.5f, -0.5f, 0.5f, 7, 1, 2).add();
        } else {
            part(b, "tail_spade", prev, Role.STATIC, horn).at(0, 0, 5).mat("horn").box(-2.5f, -0.5f, 0, 5, 1, 2)
                    .box(-1.5f, -0.5f, 2, 3, 1, 1).add();
        }
        return b.build();
    }

    /** Two or three hooked claws at the front of a foot. */
    static void claws(ModelSpec.Builder b, String name, String foot, int claw, float z, boolean two) {
        Kit.Sym c = sym(b, name, foot, true, claw).at(0, 1, z).mat("horn").box(-2.5f, -0.5f, -2, 1, 1, 2).box(1.5f, -0.5f, -2, 1, 1, 2);
        if (!two) {
            c.box(-0.5f, -0.5f, -2.5f, 1, 1, 3);
        }
        c.add();
    }
}
