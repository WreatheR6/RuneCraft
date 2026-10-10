package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.Kit.part;

/**
 * Premium re-sculpts of the non-humanoid world and slayer bosses (dragons, serpents, beasts and abyssal horrors),
 * built to the TzTok-Jad standard: layered, segmented silhouettes of 50-120 boxes with painted materials and an
 * emissive layer. Registered as model overrides (same ids as the boss modules' originals). Shared sculpting
 * helpers live here; the creatures are split by family into the other {@code BestiaryBst*} classes.
 */
final class BestiaryBst {
    private BestiaryBst() {
    }

    static List<ModelSpec> all() {
        List<ModelSpec> l = new ArrayList<>();
        l.addAll(BestiaryBstDragons.all());
        l.addAll(BestiaryBstSerpents.all());
        l.addAll(BestiaryBstBeasts.all());
        l.addAll(BestiaryBstAbyss.all());
        return l;
    }

    // ---- colour helpers ----

    static int mix(int a, int b, float t) {
        int r = Math.round((a >> 16 & 0xFF) + ((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * t);
        int g = Math.round((a >> 8 & 0xFF) + ((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * t);
        int bl = Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return r << 16 | g << 8 | bl;
    }

    static int dark(int c, float f) {
        return mix(c, 0x000000, f);
    }

    static int light(int c, float f) {
        return mix(c, 0xFFFFFF, f);
    }

    // ---- sculpting helpers ----

    /**
     * A tapering chain of segments (tails, necks, tentacles) extending along +Z (or -Z when {@code forward}),
     * each a child of the previous one. Segment {@code i} is {@code w[i]} wide/high and {@code len[i]} long and
     * is pitched by {@code pitch[i]} / yawed by {@code yaw[i]} relative to its parent. Returns the last name.
     */
    static String chain(ModelSpec.Builder b, String prefix, String parent, Role role, int color, int accent, String mat,
                        float x, float y, float z, boolean forward, int[] w, int[] len, float[] pitch, float[] yaw) {
        String prev = parent;
        for (int i = 0; i < w.length; i++) {
            String n = prefix + i;
            float zz = i == 0 ? z : (forward ? -len[i - 1] : len[i - 1]);
            float dz = forward ? -len[i] : 0;
            part(b, n, prev, i == 0 || role == Role.TAIL || role == Role.TENTACLE ? role : Role.STATIC, color).accent(accent)
                    .at(i == 0 ? x : 0, i == 0 ? y : 0, zz).rot(pitch[i], yaw == null ? 0 : yaw[i], 0).mat(mat)
                    .box(-w[i] / 2f, -w[i] / 2f, dz, w[i], w[i], len[i]).add();
            prev = n;
        }
        return prev;
    }

    /** A row of {@code n} teeth (1x{@code h}x1) along X, hanging down (or up when {@code up}) from the pivot. */
    static ModelSpec.Builder teeth(ModelSpec.Builder b, String name, String parent, int color, float x, float y, float z,
                                   float span, int n, int h, boolean up) {
        Kit.P p = part(b, name, parent, Role.STATIC, color).at(x, y, z).mat("bone");
        for (int k = 0; k < n; k++) {
            float tx = n == 1 ? -0.5f : -span / 2f + k * (span - 1) / (n - 1);
            int th = (k == 0 || k == n - 1) ? h + 1 : h;
            p.box(tx, up ? -th : 0, -0.5f, 1, th, 1);
        }
        return p.add();
    }

    /** Fangs along both sides of a jaw: {@code n} per side at z positions stepping back from {@code z0}. */
    static ModelSpec.Builder sideFangs(ModelSpec.Builder b, String name, String parent, int color, float halfW, float y,
                                       float z0, float step, int n, int h, boolean up) {
        Kit.P p = part(b, name, parent, Role.STATIC, color).at(0, y, 0).mat("bone");
        for (int k = 0; k < n; k++) {
            int th = k == 0 ? h + 1 : h;
            float zz = z0 + k * step;
            p.box(halfW - 1, up ? -th : 0, zz, 1, th, 1);
            p.box(-halfW, up ? -th : 0, zz, 1, th, 1);
        }
        return p.add();
    }

    /** A small glowing eye (or pair) as an emissive box. */
    static ModelSpec.Builder eyes(ModelSpec.Builder b, String name, String parent, int color, float x, float y, float z,
                                  int w, int h) {
        return part(b, name, parent, Role.STATIC, color).at(0, y, z).mat("energy").glow("full")
                .box(x - w, 0, -0.5f, w, h, 1).box(-x, 0, -0.5f, w, h, 1).add();
    }
}
