package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

/**
 * Raid and minigame bosses and their notable monsters, re-sculpted to the TzTok-Jad standard: Chambers of
 * Xeric, Theatre of Blood, Tombs of Amascut, the Fight Caves and the Inferno, the Gauntlet, the Nightmare,
 * Zalcano, Wintertodt, Tempoross, Guardians of the Rift, Barbarian Assault and Pest Control.
 *
 * <p>Also the shared sculpting vocabulary those files use: a configurable creature {@link Frame} (hips, hunched
 * torso, segmented arms and legs with claws and toes), and small detail helpers (teeth rows, spike rows, glowing
 * eyes). Coordinates follow {@link ModelSpec}: model pixels, Y=24 is the ground, smaller Y is higher, -Z is the
 * front and the "left" side of a creature sits at +X.
 */
public final class BestiaryRad {
    private BestiaryRad() {
    }

    /** Every model of the raid/minigame scope (ids are the creature ids the modules already use). */
    static List<ModelSpec> all() {
        List<ModelSpec> l = new ArrayList<>();
        l.addAll(BestiaryRadTzhaar.all());
        l.addAll(BestiaryRadCox.all());
        l.addAll(BestiaryRadTob.all());
        l.addAll(BestiaryRadToa.all());
        l.addAll(BestiaryRadBoss.all());
        l.addAll(BestiaryRadGames.all());
        return l;
    }

    // ------------------------------------------------------------------------------------------
    // Fluent shorthands
    // ------------------------------------------------------------------------------------------

    static Kit.P p(ModelSpec.Builder b, String name, String parent, int color) {
        return Kit.part(b, name, parent, Role.STATIC, color);
    }

    static Kit.P p(ModelSpec.Builder b, String name, String parent, Role role, int color) {
        return Kit.part(b, name, parent, role, color);
    }

    static Kit.Sym s(ModelSpec.Builder b, String name, String parent, boolean sidedParent, int color) {
        return Kit.sym(b, name, parent, sidedParent, color);
    }

    /** Linear mix of two RGB colours. */
    static int mix(int a, int b, float t) {
        int r = Math.round((a >> 16 & 0xFF) + ((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * t);
        int g = Math.round((a >> 8 & 0xFF) + ((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * t);
        int bl = Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return r << 16 | g << 8 | bl;
    }

    static int shade(int c, float f) {
        int r = Math.min(255, Math.round((c >> 16 & 0xFF) * f));
        int g = Math.min(255, Math.round((c >> 8 & 0xFF) * f));
        int bl = Math.min(255, Math.round((c & 0xFF) * f));
        return r << 16 | g << 8 | bl;
    }

    // ------------------------------------------------------------------------------------------
    // Detail helpers
    // ------------------------------------------------------------------------------------------

    /** A row of {@code n} teeth (one part, one box each) hanging down (len > 0) or pointing up (len < 0). */
    static void teeth(ModelSpec.Builder b, String name, String parent, int color, float x0, float x1, float y, float z,
                      int n, int len) {
        Kit.P t = p(b, name, parent, color).at(0, y, z).mat("horn");
        for (int i = 0; i < n; i++) {
            float x = n == 1 ? (x0 + x1) / 2f : x0 + (x1 - x0) * i / (n - 1f);
            boolean big = i == 0 || i == n - 1;
            int l = Math.abs(len) + (big ? 1 : 0);
            t.box(x - 0.5f, len > 0 ? 0 : -l, -0.5f, 1, l, 1);
        }
        t.add();
    }

    /** Glowing eyes: a mirrored pair of small full-bright boxes on the front of {@code parent}. */
    static void eyes(ModelSpec.Builder b, String name, String parent, int color, float x, float y, float z, int w, int h) {
        s(b, name, parent, false, color).at(x, y, z).mat("energy").glow("full").box(-w / 2f, -h / 2f, -0.6f, w, h, 1).add();
    }

    /** A row of rising spikes (one part); each spike is a 2-step taper. Positions along X, Z given per spike. */
    static void spikes(ModelSpec.Builder b, String name, String parent, int color, String mat, float y, float[][] xzh,
                       float pitch) {
        Kit.P sp = p(b, name, parent, color).at(0, y, 0).rot(pitch, 0, 0).mat(mat);
        for (float[] q : xzh) {
            int h = (int) q[2];
            sp.box(q[0] - 1, -h, q[1] - 1, 2, h, 2).box(q[0] - 0.5f, -h - Math.max(1, h / 2), q[1] - 0.5f, 1, Math.max(1, h / 2), 1);
        }
        sp.add();
    }

    // ------------------------------------------------------------------------------------------
    // Creature frame
    // ------------------------------------------------------------------------------------------

    /**
     * A configurable two-legged creature body: hips, a (hunched) torso with chest and belly plates, shoulders,
     * upper arms, elbows, forearms, hands with claws, thighs with knee caps, shins, feet with toes, a neck and a
     * head with brow and jaw. Callers set the fields, call {@link #build()}, then hang their creature's own
     * features on the named parts: "hips", "torso", "chest", "neck", "head", "jaw", "shoulder_l/_r",
     * "arm_l/_r", "forearm_l/_r", "hand_l/_r", "leg_l/_r", "shin_l/_r", "foot_l/_r".
     */
    static final class Frame {
        final ModelSpec.Builder b;
        int skin;
        int dark;
        int deep;
        int accent;
        String mat = "hide";
        String limbGlow;
        String bodyGlow;
        int clawColor = 0xE8DCC0;
        String clawMat = "horn";
        // proportions
        float thigh = 7;
        float shinLen = 6;
        int legW = 5;
        float legX = 4;
        float footH = 2;
        int footLen = 7;
        int toes = 3;
        int hipW = 10;
        int hipH = 5;
        int hipD = 7;
        int torsoW = 14;
        int torsoH = 12;
        int torsoD = 9;
        float hunch = 15;
        float upperArm = 8;
        float foreArm = 8;
        int armW = 4;
        int foreW = 5;
        float armSplay = 10;
        float elbowBend = 18;
        int handW = 5;
        int handH = 3;
        int claws = 3;
        int clawLen = 2;
        boolean shoulders = true;
        boolean kneeCaps = true;
        int neckLen = 3;
        int headW = 8;
        int headH = 7;
        int headD = 7;
        float headPitch;
        boolean jaw = true;
        boolean faceEyes = true;
        String headGlow;
        float hipY;

        Frame(String id, float scale, float shadow) {
            this.b = ModelSpec.builder(id).scale(scale).shadow(shadow);
        }

        Frame colors(int skin, int dark, int deep, int accent) {
            this.skin = skin;
            this.dark = dark;
            this.deep = deep;
            this.accent = accent;
            return this;
        }

        Frame legs(float thigh, float shin, int w, float x) {
            this.thigh = thigh;
            this.shinLen = shin;
            this.legW = w;
            this.legX = x;
            return this;
        }

        Frame torso(int w, int h, int d, float hunch) {
            this.torsoW = w;
            this.torsoH = h;
            this.torsoD = d;
            this.hunch = hunch;
            return this;
        }

        Frame hips(int w, int h, int d) {
            this.hipW = w;
            this.hipH = h;
            this.hipD = d;
            return this;
        }

        Frame arms(float upper, float fore, int w, int foreW) {
            this.upperArm = upper;
            this.foreArm = fore;
            this.armW = w;
            this.foreW = foreW;
            return this;
        }

        Frame head(int w, int h, int d) {
            this.headW = w;
            this.headH = h;
            this.headD = d;
            return this;
        }

        Frame mat(String m) {
            this.mat = m;
            return this;
        }

        Kit.P part(String name, String parent, int color) {
            return Kit.part(b, name, parent, Role.STATIC, color).accent(accent).mat(mat);
        }

        Kit.Sym sym(String name, String parent, boolean sided, int color) {
            return Kit.sym(b, name, parent, sided, color).accent(accent).mat(mat);
        }

        ModelSpec.Builder build() {
            double k1 = Math.toRadians(14);
            double k2 = Math.toRadians(10);
            hipY = 24 - footH - (float) (thigh * Math.cos(k1) + shinLen * Math.cos(k2)) + 0.4f;
            Kit.P hips = Kit.part(b, "hips", null, Role.BODY, deep).accent(accent).mat(mat).at(0, hipY, 0)
                    .box(-hipW / 2f, -hipH + 1, -hipD / 2f, hipW, hipH, hipD);
            if (bodyGlow != null) {
                hips.glow(bodyGlow);
            }
            hips.add();
            // torso, hunched forward around the waist
            Kit.P torso = part("torso", "hips", skin).at(0, -hipH + 2, 0.5f).rot(hunch, 0, 0)
                    .box(-torsoW / 2f, -torsoH, -torsoD / 2f, torsoW, torsoH, torsoD);
            if (bodyGlow != null) {
                torso.glow(bodyGlow);
            }
            torso.add();
            part("chest", "torso", shade(skin, 1.08f)).at(0, -torsoH + 1, -torsoD / 2f).rot(-4, 0, 0)
                    .box(-torsoW / 2f + 1.5f, 0, -1.5f, torsoW / 2 - 1, (int) (torsoH * 0.45f), 2)
                    .box(0.5f, 0, -1.5f, torsoW / 2 - 1, (int) (torsoH * 0.45f), 2).add();
            part("belly", "torso", dark).at(0, -torsoH * 0.42f, -torsoD / 2f)
                    .box(-torsoW / 2f + 2.5f, 0, -1, torsoW - 5, 2, 2)
                    .box(-torsoW / 2f + 3, 2.2f, -1, torsoW - 6, 2, 2)
                    .box(-torsoW / 2f + 3.5f, 4.4f, -0.8f, torsoW - 7, 2, 2).add();
            part("back", "torso", deep).at(0, -torsoH + 1, torsoD / 2f)
                    .box(-torsoW / 2f + 1, 0, -1, torsoW - 2, (int) (torsoH * 0.6f), 2).add();
            // legs
            Kit.Sym leg = sym("leg", null, false, skin).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).at(legX, hipY, 0.5f)
                    .rot(-14, 0, -3).box(-legW / 2f, -1.5f, -legW / 2f, legW, (int) Math.ceil(thigh + 1.5f), legW);
            if (limbGlow != null) {
                leg.glow(limbGlow);
            }
            leg.add();
            if (kneeCaps) {
                sym("knee", "leg", true, deep).at(0, thigh - 1, -legW / 2f).rot(-10, 0, 0)
                        .box(-legW / 2f + 0.5f, -1.5f, -1.2f, legW - 1, 3, 2).add();
            }
            Kit.Sym shin = sym("shin", "leg", true, dark).at(0, thigh, 0).rot(24, 0, 3)
                    .box(-legW / 2f + 0.5f, 0, -legW / 2f + 0.5f, legW - 1, (int) Math.ceil(shinLen), legW - 1);
            if (limbGlow != null) {
                shin.glow(limbGlow);
            }
            shin.add();
            sym("foot", "shin", true, deep).at(0, shinLen, 0).rot(-10, 0, 0)
                    .box(-legW / 2f, 0, -footLen + legW / 2f + 0.5f, legW, (int) footH, footLen).add();
            if (toes > 0) {
                Kit.Sym t = Kit.sym(b, "toe", "foot", true, clawColor).mat(clawMat).at(0, footH - 1, -footLen + legW / 2f + 0.5f);
                for (int i = 0; i < toes; i++) {
                    float x = toes == 1 ? 0 : -legW / 2f + 0.5f + (legW - 2f) * i / (toes - 1f);
                    t.box(x, -0.2f, -1.5f, 1, 1, 2);
                }
                t.add();
            }
            // arms hang from the shoulders, counter-rotated against the hunch
            float sy = -torsoH + 2.5f;
            if (shoulders) {
                sym("shoulder", "torso", false, shade(skin, 1.05f)).at(torsoW / 2f, sy - 0.5f, 0).rot(0, 0, -12)
                        .box(-2, -3, -torsoD / 2f + 0.5f, armW + 3, 5, torsoD - 1).add();
            }
            Kit.Sym arm = sym("arm", "torso", false, skin).roles(Role.ARM_LEFT, Role.ARM_RIGHT)
                    .at(torsoW / 2f + armW / 2f, sy + 1, 0).rot(-hunch - 4, 0, -armSplay)
                    .box(-armW / 2f, -1.5f, -armW / 2f, armW, (int) Math.ceil(upperArm + 1.5f), armW);
            if (limbGlow != null) {
                arm.glow(limbGlow);
            }
            arm.add();
            sym("elbow", "arm", true, deep).at(0, upperArm, armW / 2f).rot(15, 0, 0)
                    .box(-armW / 2f + 0.5f, -1.5f, -0.5f, armW - 1, 3, 2).add();
            Kit.Sym fore = sym("forearm", "arm", true, dark).at(0, upperArm, 0).rot(-elbowBend, 0, armSplay * 0.4f)
                    .box(-foreW / 2f, 0, -foreW / 2f, foreW, (int) Math.ceil(foreArm), foreW);
            if (limbGlow != null) {
                fore.glow(limbGlow);
            }
            fore.add();
            sym("hand", "forearm", true, deep).at(0, foreArm, 0).rot(8, 0, 0)
                    .box(-handW / 2f, 0, -handW / 2f, handW, handH, handW).add();
            if (claws > 0) {
                Kit.Sym c = Kit.sym(b, "claw", "hand", true, clawColor).mat(clawMat).at(0, handH, -handW / 2f + 0.5f).rot(-20, 0, 0);
                for (int i = 0; i < claws; i++) {
                    float x = claws == 1 ? -0.5f : -handW / 2f + (handW - 1f) * i / (claws - 1f);
                    c.box(x, -0.5f, -0.5f, 1, clawLen, 1);
                }
                c.add();
            }
            // neck and head
            part("neck", "torso", dark).at(0, -torsoH + 1, -torsoD / 2f + 2.5f).rot(-hunch * 0.6f, 0, 0)
                    .box(-headW / 2f + 1.5f, -neckLen - 1, -2.5f, headW - 3, neckLen + 2, 5).add();
            Kit.P head = Kit.part(b, "head", "neck", Role.HEAD, skin).accent(accent).mat(mat).at(0, -neckLen, -1)
                    .rot(-hunch * 0.4f + headPitch, 0, 0).box(-headW / 2f, -headH + 2, -headD + 2, headW, headH, headD);
            if (faceEyes) {
                head.pattern("eyes").glow(headGlow == null ? "eyes" : headGlow);
            } else if (headGlow != null) {
                head.glow(headGlow);
            }
            head.add();
            part("brow", "head", deep).at(0, -headH + 3.5f, -headD + 2).rot(10, 0, 0)
                    .box(-headW / 2f - 0.5f, -1.5f, -1.2f, headW + 1, 2, 2).add();
            if (jaw) {
                Kit.part(b, "jaw", "head", Role.JAW, dark).accent(accent).mat(mat).at(0, 2, -headD + 3).rot(6, 0, 0)
                        .glow("maw").box(-headW / 2f + 1, 0, -2, headW - 2, 2, headD - 3).add();
            }
            return b;
        }
    }
}
