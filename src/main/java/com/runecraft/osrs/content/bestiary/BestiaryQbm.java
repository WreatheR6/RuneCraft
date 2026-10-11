package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/**
 * Premium re-sculpts of the non-humanoid quest bosses (Dagannoth Mother, Kalrag, the giant scarabs, the Sea Slug
 * quest slugs, Kruk, the Kendal, the temple guardian, the Inadequacy...), the minigame shade and the Nightmare's
 * husks, plus two everyday creatures that had no model at all: the rock crab and the imp. Built to the TzTok-Jad
 * standard (layered, segmented silhouettes with painted materials and an emissive layer).
 * <ul>
 *   <li>{@link #overrides()}: ids another module already models (the quests module's simple quest-boss models and
 *   the minigames module's shade) - replaced in place, keeping their ids;</li>
 *   <li>{@link #fresh()}: ids nothing else models (quest bosses that fell back to generated forms, the husk, the
 *   rock crab and the imp).</li>
 * </ul>
 * The creatures are split by body plan into the other {@code BestiaryQbm*} classes; shared limb helpers live here.
 */
final class BestiaryQbm {
    private BestiaryQbm() {
    }

    static List<ModelSpec> overrides() {
        List<ModelSpec> l = new ArrayList<>();
        l.addAll(BestiaryQbmBeasts.overrides());
        l.addAll(BestiaryQbmCrawlers.overrides());
        l.addAll(BestiaryQbmFliers.overrides());
        l.addAll(BestiaryQbmOdd.overrides());
        return l;
    }

    static List<ModelSpec> fresh() {
        List<ModelSpec> l = new ArrayList<>();
        l.addAll(BestiaryQbmBeasts.fresh());
        l.addAll(BestiaryQbmCrawlers.fresh());
        l.addAll(BestiaryQbmFliers.fresh());
        l.addAll(BestiaryQbmOdd.fresh());
        return l;
    }

    // ---- shared limb helpers ----

    /**
     * A mirrored, jointed animal leg: an upper limb with the walk role, a lower limb, a flat paw (or hoof) and a row of
     * claws. The pivot height is solved so the paw rests on the ground (Y=24). {@code up}/{@code lo} are the pitches of
     * the two segments (negative swings the foot forward); the paw is levelled again.
     */
    static void leg(ModelSpec.Builder b, String name, boolean front, float x, float z, int upW, int upH, int loW, int loH,
                    int pawW, int pawL, float up, float lo, int col, int lower, int pawCol, int claw, String mat,
                    String glow, int claws) {
        double a = Math.toRadians(up);
        double c = Math.toRadians(up + lo);
        float y = (float) (24 - ((upH - 1.5) * Math.cos(a) + (loH - 0.5) * Math.cos(c) + 2));
        Role l = front ? Role.LEG_FRONT_LEFT : Role.LEG_BACK_LEFT;
        Role r = front ? Role.LEG_FRONT_RIGHT : Role.LEG_BACK_RIGHT;
        Kit.Sym s = sym(b, name, null, false, col).roles(l, r).at(x, y, z).rot(up, 0, 0).mat(mat)
                .box(-upW / 2f, -1.5f, -upW / 2f, upW, upH, upW);
        if (glow != null) {
            s.glow(glow);
        }
        s.add();
        Kit.Sym s2 = sym(b, name + "_lo", name, true, lower).at(0, upH - 1.5f, 0).rot(lo, 0, 0).mat(mat)
                .box(-loW / 2f, -0.5f, -loW / 2f, loW, loH, loW);
        if (glow != null) {
            s2.glow(glow);
        }
        s2.add();
        sym(b, name + "_paw", name + "_lo", true, pawCol).at(0, loH - 0.5f, 0).rot(-(up + lo), 0, 0).mat(mat)
                .box(-pawW / 2f, 0, -pawL + loW / 2f, pawW, 2, pawL).add();
        if (claws > 0) {
            Kit.Sym t = sym(b, name + "_claw", name + "_paw", true, claw).at(0, 1, -pawL + loW / 2f).mat("horn");
            for (int k = 0; k < claws; k++) {
                float tx = claws == 1 ? -0.5f : -pawW / 2f + 0.2f + k * (pawW - 1.4f) / (claws - 1);
                t.box(tx, -0.6f, -1.6f, 1, 1, 2);
            }
            t.add();
        }
    }

    /** A row of {@code n} small boxes (studs, spines, warts) along X centred on the pivot. */
    static ModelSpec.Builder row(ModelSpec.Builder b, String name, String parent, int col, String mat, float x, float y,
                                 float z, float span, int n, int w, int h, int d) {
        Kit.P p = part(b, name, parent, Role.STATIC, col).at(x, y, z).mat(mat);
        for (int k = 0; k < n; k++) {
            float tx = n == 1 ? -w / 2f : -span / 2f + k * (span - w) / (n - 1);
            p.box(tx, -h / 2f, -d / 2f, w, h, d);
        }
        return p.add();
    }

    /** Spines (or plates) along the back: {@code n} tilted pieces stepping back along Z from {@code z0}. */
    static void spines(ModelSpec.Builder b, String name, String parent, int col, int alt, String mat, float y, float z0,
                       float step, int[] heights, float tilt, int w) {
        for (int i = 0; i < heights.length; i++) {
            int h = heights[i];
            part(b, name + i, parent, Role.STATIC, i % 2 == 0 ? col : alt).at(0, y, z0 + i * step).rot(tilt, 0, 0).mat(mat)
                    .box(-w / 2f, -h, -1, w, h, 2).add();
        }
    }
}
