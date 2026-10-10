package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

/**
 * Small building blocks for boss models: leg sets, segmented chains (tails, necks, tentacles) and
 * spider legs. Units are model pixels; Y=24 is the ground, -Z is the front.
 */
final class ModelKit {
    private ModelKit() {
    }

    /** Four root-level walking legs whose feet touch the ground. */
    static void legs4(ModelSpec.Builder b, String prefix, int color, float halfX, float zFront, float zBack, int len, int w) {
        float top = 24 - len;
        float h = w / 2f;
        b.part(prefix + "_fl", null, Role.LEG_FRONT_LEFT, color).pivot(halfX, top, zFront).box(-h, 0, -h, w, len, w);
        b.part(prefix + "_fr", null, Role.LEG_FRONT_RIGHT, color).pivot(-halfX, top, zFront).box(-h, 0, -h, w, len, w);
        b.part(prefix + "_bl", null, Role.LEG_BACK_LEFT, color).pivot(halfX, top, zBack).box(-h, 0, -h, w, len, w);
        b.part(prefix + "_br", null, Role.LEG_BACK_RIGHT, color).pivot(-halfX, top, zBack).box(-h, 0, -h, w, len, w);
    }

    /** Two root-level biped legs (thigh + shin) of total length {@code len}. */
    static void legs2(ModelSpec.Builder b, String prefix, int color, float halfX, float z, int len, int w) {
        float top = 24 - len;
        int thigh = len / 2;
        int shin = len - thigh;
        float h = w / 2f;
        b.part(prefix + "_l", null, Role.LEG_FRONT_LEFT, color).pivot(halfX, top, z).box(-h, 0, -h, w, thigh, w);
        b.part(prefix + "_l_shin", prefix + "_l", Role.STATIC, color).pivot(0, thigh, 0).box(-h + 0.5f, 0, -h - 0.5f, w - 1, shin, w);
        b.part(prefix + "_r", null, Role.LEG_FRONT_RIGHT, color).pivot(-halfX, top, z).box(-h, 0, -h, w, thigh, w);
        b.part(prefix + "_r_shin", prefix + "_r", Role.STATIC, color).pivot(0, thigh, 0).box(-h + 0.5f, 0, -h - 0.5f, w - 1, shin, w);
    }

    /**
     * A horizontal chain of {@code n} segments extending along Z ({@code dir} +1 = backwards, -1 = forwards).
     * Each segment is a child of the previous one so the sway animation accumulates.
     */
    static String chainZ(ModelSpec.Builder b, String prefix, String parent, Role role, int color, int n, float px, float py, float pz,
                         int dir, int w, int h, int len, float pitchStep, String pattern) {
        String prev = parent;
        for (int i = 0; i < n; i++) {
            int wi = Math.max(1, w - (w * i) / (n + 1));
            int hi = Math.max(1, h - (h * i) / (n + 1));
            String name = prefix + i;
            b.part(name, prev, role, color);
            if (i == 0) {
                b.pivot(px, py, pz);
            } else {
                b.pivot(0, 0, dir * len);
            }
            b.rotate(pitchStep, 0, 0).box(-wi / 2f, -hi / 2f, dir > 0 ? 0 : -len, wi, hi, len).pattern(pattern);
            prev = name;
        }
        return prev;
    }

    /** A vertical chain rising upward (necks, tentacles); {@code lean} tilts each segment (degrees, +forward). */
    static String chainUp(ModelSpec.Builder b, String prefix, String parent, Role role, int color, int n, float px, float py, float pz,
                          int w, int len, float lean, float yaw, String pattern) {
        String prev = parent;
        for (int i = 0; i < n; i++) {
            int wi = Math.max(1, w - (w * i) / (n + 2));
            String name = prefix + i;
            b.part(name, prev, role, color);
            if (i == 0) {
                b.pivot(px, py, pz).rotate(-lean, yaw, 0);
            } else {
                b.pivot(0, -len, 0).rotate(-lean, 0, 0);
            }
            b.box(-wi / 2f, -len, -wi / 2f, wi, len, wi).pattern(pattern);
            prev = name;
        }
        return prev;
    }

    /** A hanging/splayed chain pointing down and outwards in direction {@code yaw} (tentacles, vines). */
    static String chainDown(ModelSpec.Builder b, String prefix, String parent, Role role, int color, int n, float px, float py, float pz,
                            int w, int len, float splay, float yaw, String pattern) {
        String prev = parent;
        for (int i = 0; i < n; i++) {
            int wi = Math.max(1, w - (w * i) / (n + 1));
            String name = prefix + i;
            b.part(name, prev, role, color);
            if (i == 0) {
                b.pivot(px, py, pz).rotate(splay, yaw, 0);
            } else {
                b.pivot(0, len, 0).rotate(splay / 3, 0, 0);
            }
            b.box(-wi / 2f, 0, -wi / 2f, wi, len, wi).pattern(pattern);
            prev = name;
        }
        return prev;
    }

    /**
     * Arachnid legs: {@code pairs} pairs attached to {@code parent} at local height {@code y}, starting at
     * {@code z0} and spaced {@code zStep}. Each leg has an upper LEG_EXTRA segment and a steep lower segment.
     */
    static void spiderLegs(ModelSpec.Builder b, String prefix, String parent, int color, int pairs, float x, float y, float z0,
                           float zStep, int upper, int lower, int thick) {
        float t = thick / 2f;
        for (int i = 0; i < pairs; i++) {
            float z = z0 + i * zStep;
            float yaw = (i - (pairs - 1) / 2f) * 22f;
            String l = prefix + "_l" + i;
            b.part(l, parent, Role.LEG_EXTRA, color).pivot(x, y, z).rotate(0, -yaw, 35).box(0, -t, -t, upper, thick, thick);
            b.part(l + "_low", l, Role.STATIC, color).pivot(upper, 0, 0).rotate(0, 0, 55)
                    .box(0, -t + 0.25f, -t + 0.25f, lower, Math.max(1, thick - 1), Math.max(1, thick - 1));
            String r = prefix + "_r" + i;
            b.part(r, parent, Role.LEG_EXTRA, color).pivot(-x, y, z).rotate(0, yaw, -35).box(-upper, -t, -t, upper, thick, thick);
            b.part(r + "_low", r, Role.STATIC, color).pivot(-upper, 0, 0).rotate(0, 0, -55)
                    .box(-lower, -t + 0.25f, -t + 0.25f, lower, Math.max(1, thick - 1), Math.max(1, thick - 1));
        }
    }

    /** A pair of bat-like wings (bone + membrane) at the given shoulder position. */
    static void wings(ModelSpec.Builder b, String parent, int bone, int membrane, float x, float y, float z, int span, int height) {
        b.part("wing_l", parent, Role.WING_LEFT, bone, membrane).pivot(x, y, z).rotate(0, -15, -15)
                .box(0, -1, -1, span, 2, 2).box(0, 1, 0, span, height, 1);
        b.part("wing_r", parent, Role.WING_RIGHT, bone, membrane).pivot(-x, y, z).rotate(0, 15, 15)
                .box(-span, -1, -1, span, 2, 2).box(-span, 1, 0, span, height, 1);
    }
}
