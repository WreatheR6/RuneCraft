package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

/**
 * Small sculpting helpers shared by the slayer-monster models (BestiarySlm*): static parts, mirrored pairs,
 * tapering segment chains (necks, tails, tentacles), curved horn chains, rows of teeth and claws.
 * Coordinates follow {@link ModelSpec}: model pixels, Y=24 is the ground, smaller Y is higher, -Z is the front.
 */
final class BestiarySlmKit {
    static final int TOOTH = 0xEEE6CC;
    static final int CLAW = 0x2A2622;
    static final int BONE = 0xDCD6C0;

    private BestiarySlmKit() {
    }

    /** A static (non-animated) part. */
    static Kit.P st(ModelSpec.Builder b, String name, String parent, int color) {
        return Kit.part(b, name, parent, Role.STATIC, color);
    }

    /** A mirrored static pair on an unsided parent. */
    static Kit.Sym sy(ModelSpec.Builder b, String name, String parent, int color) {
        return Kit.sym(b, name, parent, false, color);
    }

    /** A mirrored static pair whose parent is itself a pair ("x" means "x_l"/"x_r"). */
    static Kit.Sym sy2(ModelSpec.Builder b, String name, String parent, int color) {
        return Kit.sym(b, name, parent, true, color);
    }

    static int darker(int c, float f) {
        int r = (int) ((c >> 16 & 0xFF) * f);
        int g = (int) ((c >> 8 & 0xFF) * f);
        int bl = (int) ((c & 0xFF) * f);
        return Math.min(255, r) << 16 | Math.min(255, g) << 8 | Math.min(255, bl);
    }

    static int mix(int a, int c, float t) {
        int r = (int) ((a >> 16 & 0xFF) + ((c >> 16 & 0xFF) - (a >> 16 & 0xFF)) * t);
        int g = (int) ((a >> 8 & 0xFF) + ((c >> 8 & 0xFF) - (a >> 8 & 0xFF)) * t);
        int bl = (int) ((a & 0xFF) + ((c & 0xFF) - (a & 0xFF)) * t);
        return r << 16 | g << 8 | bl;
    }

    /**
     * A tapering chain of segments, each the child of the previous one. Segments run along -Z ({@code dir} &lt; 0,
     * necks) or +Z ({@code dir} &gt; 0, tails); {@code pitch[i]}/{@code yaw[i]} bend each joint. Returns the name of
     * the last segment ({@code name + (n-1)}).
     */
    static String chain(ModelSpec.Builder b, String name, String parent, Role role, int color, int accent, String mat, String glow,
                        float x, float y, float z, int dir, int[] w, int[] h, int[] len, float[] pitch, float[] yaw) {
        String prev = parent;
        for (int i = 0; i < w.length; i++) {
            float pz = i == 0 ? z : dir * len[i - 1];
            Kit.P p = Kit.part(b, name + i, prev, role, color).accent(accent).at(i == 0 ? x : 0, i == 0 ? y : 0, pz)
                    .rot(pitch[i], yaw == null ? 0 : yaw[i], 0).mat(mat);
            if (glow != null) {
                p.glow(glow);
            }
            p.box(-w[i] / 2f, -h[i] / 2f, dir < 0 ? -len[i] : 0, w[i], h[i], len[i]).add();
            prev = name + i;
        }
        return prev;
    }

    /**
     * A curved horn of {@code n} tapering segments growing along local -Y from its root; each joint bends by
     * {@code bend} degrees of pitch (negative sweeps the tip backwards) and {@code curl} degrees of roll.
     */
    static void horn(ModelSpec.Builder b, String name, String parent, boolean mirrored, boolean sidedParent, int color,
                     float x, float y, float z, float pitch, float yaw, float roll, int[] thick, int[] len, float bend, float curl) {
        for (int i = 0; i < thick.length; i++) {
            String par = i == 0 ? parent : name + (i - 1);
            boolean sp = i == 0 ? sidedParent : true;
            float px = i == 0 ? x : 0;
            float py = i == 0 ? y : -len[i - 1] + 0.3f;
            float pz = i == 0 ? z : 0;
            float pi = i == 0 ? pitch : bend;
            float ya = i == 0 ? yaw : 0;
            float ro = i == 0 ? roll : curl;
            float t = thick[i];
            int c = i == thick.length - 1 ? darker(color, 0.8f) : color;
            if (mirrored) {
                Kit.sym(b, name + i, par, sp, c).at(px, py, pz).rot(pi, ya, ro).mat("horn")
                        .box(-t / 2f, -len[i], -t / 2f, thick[i], len[i], thick[i]).add();
            } else {
                Kit.part(b, name + i, par, Role.STATIC, c).at(px, py, pz).rot(pi, ya, ro).mat("horn")
                        .box(-t / 2f, -len[i], -t / 2f, thick[i], len[i], thick[i]).add();
            }
        }
    }

    /** One part holding a row of 1px teeth along both sides of a mouth (and across its front). */
    static void teeth(ModelSpec.Builder b, String name, String parent, int color, float y, float zFront, float halfW,
                      int perSide, boolean pointDown, boolean front) {
        Kit.P p = st(b, name, parent, color).mat("horn");
        float ty = pointDown ? y : y - 1;
        for (int i = 0; i < perSide; i++) {
            float z = zFront + 0.4f + i * 1.6f;
            p.box(halfW - 1, ty, z, 1, 1, 1, -0.15f);
            p.box(-halfW, ty, z, 1, 1, 1, -0.15f);
        }
        if (front) {
            for (float x = -halfW + 1.5f; x < halfW - 1.5f; x += 1.6f) {
                p.box(x, ty, zFront, 1, 1, 1, -0.2f);
            }
        }
        p.add();
    }

    /**
     * A mirrored pair of jointed arthropod legs (LEG_EXTRA): femur raised up and out along +X, tibia bent down,
     * tarsus to the ground. {@code yaw} swings the leg forward (+) or back (-); lengths are in model pixels.
     */
    static void bugLeg(ModelSpec.Builder b, String name, String parent, int color, int tip, String mat, float x, float y, float z,
                       float yaw, int l1, int l2, int l3, int thick) {
        Kit.sym(b, name, parent, false, color).roles(Role.LEG_EXTRA, Role.LEG_EXTRA).at(x, y, z).rot(0, yaw, -32).mat(mat)
                .box(0, -thick / 2f, -thick / 2f, l1, thick, thick).add();
        Kit.sym(b, name + "_tib", name, true, color).at(l1 - 0.3f, 0, 0).rot(0, 0, 82).mat(mat)
                .box(0, -thick / 2f, -thick / 2f, l2, thick, thick, -0.05f).add();
        Kit.sym(b, name + "_tar", name + "_tib", true, tip).at(l2 - 0.3f, 0, 0).rot(0, 0, 22).mat(mat)
                .box(0, -0.5f, -0.5f, l3, 1, 1, -0.05f).add();
    }

    /** A row of {@code n} claws (pointing -Z, curving down) across a foot front edge, as one part. */
    static void claws(ModelSpec.Builder b, String name, String parent, boolean sidedParent, int color, int n, float spread,
                      float y, float z, int len) {
        Kit.Sym s = Kit.sym(b, name, parent, sidedParent, color).at(0, y, z).rot(25, 0, 0).mat("horn");
        for (int i = 0; i < n; i++) {
            float x = n == 1 ? 0 : -spread / 2f + spread * i / (n - 1f);
            s.box(x - 0.5f, -0.5f, -len, 1, 1, len, -0.1f);
        }
        s.add();
    }
}
