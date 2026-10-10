package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/**
 * Premium humanoid sculpting kit for the humanoid bosses: a layered anatomy (pelvis, waist, deep chest, neck, head
 * with brow, nose, ears and jaw; segmented arms with elbow, wrist, fingers and thumb; legs with knee and shaped feet)
 * and a wardrobe of separate overlapping pieces (plate with rims and rivets, layered pauldrons, tassets, robe panels,
 * capes, hoods, helms) plus a weapon rack. Coordinates follow ModelSpec: model pixels, Y=24 is the ground, smaller Y
 * is higher, -Z is the front, "_l" parts sit at +X.
 *
 * <p>Weapons are attached to a hand with the grip at the palm: the weapon's business end points along local -Y, so a
 * pitch of 0 holds it straight up, +90 points it forward and -30 rests it back over the shoulder.
 */
final class BestiaryHumKit {
    private BestiaryHumKit() {
    }

    static final int GOLD = 0xD8A830;
    static final int BONE = 0xE4DCC4;
    static final int IVORY = 0xF0E8D0;
    static final int DARK = 0x1C1618;
    static final int LEATHER = 0x6A4424;
    static final int DARK_LEATHER = 0x3E2814;
    static final int WOOD = 0x6E4A2A;
    static final int STEEL = 0x9A9AA2;
    static final int IRON = 0x5E5A58;
    static final int HAIR_WHITE = 0xE0DED6;
    static final int HAIR_GREY = 0x9A9890;

    static int darker(int c, float f) {
        int r = (int) ((c >> 16 & 0xFF) * f);
        int g = (int) ((c >> 8 & 0xFF) * f);
        int b = (int) ((c & 0xFF) * f);
        return r << 16 | g << 8 | b;
    }

    static int lighter(int c, float t) {
        int r = c >> 16 & 0xFF;
        int g = c >> 8 & 0xFF;
        int b = c & 0xFF;
        r += (int) ((255 - r) * t);
        g += (int) ((255 - g) * t);
        b += (int) ((255 - b) * t);
        return r << 16 | g << 8 | b;
    }

    static Fig fig(String id) {
        return new Fig(id);
    }

    /** A humanoid figure being sculpted. Configure proportions and colours, then call the stage methods. */
    static final class Fig {
        final String id;
        final ModelSpec.Builder b;
        // proportions
        int legLen = 12;
        int legW = 4;
        int hipW = 8;
        int waistW = 7;
        int chestW = 9;
        int chestH = 6;
        int absH = 5;
        int depth = 5;
        int armLen = 12;
        int armW = 3;
        int foreW = 3;
        int headW = 7;
        int headH = 8;
        int headD = 7;
        float hunch;
        float stance = 2;
        float spread = 5;
        float armPitchL = -4;
        float armPitchR = -4;
        float foreBend = 12;
        float kneeBend = 10;
        // colours
        int skin = 0xC8946A;
        String skinMat = "skin";
        int counter;

        Fig(String id) {
            this.id = id;
            this.b = ModelSpec.builder(id);
        }

        Fig scale(float s) {
            b.scale(s);
            return this;
        }

        Fig shadow(float s) {
            b.shadow(s);
            return this;
        }

        Fig legs(int len, int w) {
            legLen = len;
            legW = w;
            return this;
        }

        Fig torso(int hip, int waist, int chest, int chestH, int absH, int depth) {
            this.hipW = hip;
            this.waistW = waist;
            this.chestW = chest;
            this.chestH = chestH;
            this.absH = absH;
            this.depth = depth;
            return this;
        }

        Fig arms(int len, int w, int fore) {
            armLen = len;
            armW = w;
            foreW = fore;
            return this;
        }

        Fig head(int w, int h, int d) {
            headW = w;
            headH = h;
            headD = d;
            return this;
        }

        Fig pose(float hunch, float stance, float spread) {
            this.hunch = hunch;
            this.stance = stance;
            this.spread = spread;
            return this;
        }

        Fig armPitch(float left, float right) {
            armPitchL = left;
            armPitchR = right;
            return this;
        }

        Fig skin(int c, String mat) {
            skin = c;
            skinMat = mat;
            return this;
        }

        String n(String base) {
            return base + "_" + (counter++);
        }

        Kit.P p(String name, String parent, int color) {
            return part(b, name, parent, Role.STATIC, color);
        }

        Kit.Sym s(String name, String parent, boolean sided, int color) {
            return sym(b, name, parent, sided, color);
        }

        float hipY() {
            return 24 - legLen;
        }

        int upperLen() {
            return Math.round(armLen * 0.45f);
        }

        int foreLen() {
            return armLen - upperLen() - 3;
        }

        int thighLen() {
            return (legLen - 2) / 2;
        }

        int shinLen() {
            return legLen - 2 - thighLen();
        }

        ModelSpec build() {
            return b.build();
        }

        // ------------------------------------------------------------------------------------------
        // anatomy
        // ------------------------------------------------------------------------------------------

        /** Pelvis ("hips", the BODY root), waist and chest. */
        Fig core(int pelvisCol, String pelvisMat, int waistCol, String waistMat, int chestCol, String chestMat) {
            part(b, "hips", null, Role.BODY, pelvisCol).at(0, hipY(), 0).mat(pelvisMat)
                    .box(-hipW / 2f, -2, -depth / 2f, hipW, 4, depth).add();
            p("waist", "hips", waistCol).at(0, -2, 0.3f).rot(hunch * 0.4f, 0, 0).mat(waistMat)
                    .box(-waistW / 2f, -absH, -(depth - 1) / 2f, waistW, absH, depth - 1).add();
            p("chest", "waist", chestCol).at(0, -absH + 0.5f, 0).rot(hunch * 0.6f, 0, 0).mat(chestMat)
                    .box(-chestW / 2f, -chestH, -depth / 2f - 0.5f, chestW, chestH, depth + 1).add();
            p("neck", "chest", skin).at(0, -chestH, 0).mat(skinMat).box(-1.5f, -2, -1.5f, 3, 2, 3).add();
            // trapezius slopes from neck to shoulders and a pelvis band: breaks the box silhouette
            s("trap", "chest", false, darker(chestCol, 0.94f)).at(1.2f, -chestH + 0.2f, 0.3f).rot(0, 0, -24).mat(chestMat)
                    .box(0, -1, -depth / 2f + 0.5f, Math.max(2, chestW / 2 - 1), 2, depth - 1).add();
            p("pelvis_band", "hips", darker(pelvisCol, 0.9f)).at(0, 1.5f, 0).mat(pelvisMat)
                    .box(-hipW / 2f + 0.5f, 0, -depth / 2f - 0.25f, hipW - 1, 1, depth, 0.15f).add();
            return this;
        }

        /** An upper-back hump of muscle/armour behind the neck (brutes, ourgs, trolls). */
        Fig hump(int col, String mat, int w, int h, int d) {
            p("hump", "chest", col).at(0, -chestH + 1, depth / 2f - 1).rot(-20, 0, 0).mat(mat).box(-w / 2f, -h, -d / 2f, w, h, d).add();
            return this;
        }

        /** A pot belly bulging out of the waist. */
        Fig paunch(int col, String mat, int out) {
            p("paunch", "waist", col).at(0, -absH / 2f, -(depth - 1) / 2f).mat(mat).box(-waistW / 2f + 0.5f, -absH / 2f - 1, -out, waistW - 1, absH + 1, out + 1).add();
            p("paunch_low", "paunch", darker(col, 0.92f)).at(0, absH / 2f, -out + 0.5f).rot(-15, 0, 0).mat(mat).box(-waistW / 2f + 1.5f, -1, -0.5f, waistW - 3, 2, 2).add();
            return this;
        }

        /** Bare-chested musculature: pecs, abs and a spine ridge. */
        Fig muscles(int col) {
            int dk = darker(col, 0.85f);
            p("pecs", "chest", col).at(0, -chestH + 1, -depth / 2f - 0.5f).mat(skinMat)
                    .box(-chestW / 2f + 0.5f, 0, -1, chestW / 2, Math.max(2, chestH / 2), 1)
                    .box(0, 0, -1, chestW / 2, Math.max(2, chestH / 2), 1).add();
            p("abs", "waist", dk).at(0, -absH + 0.5f, -(depth - 1) / 2f).mat(skinMat)
                    .box(-Math.max(2, waistW / 2 - 1) / 2f - 1, 0, -0.6f, Math.max(2, waistW / 2 - 1), absH - 1, 1)
                    .box(1 - Math.max(2, waistW / 2 - 1) / 2f, 0, -0.6f, Math.max(2, waistW / 2 - 1), absH - 1, 1).add();
            p("spine_ridge", "chest", dk).at(0, -chestH + 0.5f, depth / 2f + 0.5f).mat(skinMat).box(-1, 0, -0.5f, 2, chestH - 1, 1).add();
            return this;
        }

        /**
         * Head. {@code face}: "human" (painted face), "monster" (fierce glowing eyes), "skull" (sockets with glowing
         * pinpoints), "eyes" (fierce, not glowing) or "none". Adds brow, nose and (for humans) ears.
         */
        Fig head(String face, int eyeAccent) {
            Kit.P h = part(b, "head", "neck", Role.HEAD, skin).accent(eyeAccent).at(0, -1.5f, -0.3f).rot(-hunch, 0, 0)
                    .box(-headW / 2f, -headH, -headD / 2f, headW, headH, headD);
            switch (face) {
                case "human" -> h.mat("face");
                case "monster" -> h.mat(skinMat).pattern("eyes").glow("eyes");
                case "eyes" -> h.mat(skinMat).pattern("eyes");
                case "skull" -> h.mat(skinMat).pattern("skull").glow("eyes");
                case "skull_dark" -> h.mat(skinMat).pattern("skull");
                default -> h.mat(skinMat);
            }
            h.add();
            int dk = darker(skin, 0.82f);
            float browY = -headH + headH * 0.31f;
            p("brow", "head", dk).at(0, browY, -headD / 2f).rot(8, 0, 0).mat(skinMat)
                    .box(-headW / 2f + 0.5f, -0.5f, -0.7f, headW - 1, 1, 1).add();
            if (!face.equals("human")) {
                p("chin", "head", dk).at(0, 0, -headD / 2f + 0.5f).rot(-6, 0, 0).mat(skinMat).box(-headW / 2f + 1, -1.5f, -0.8f, headW - 2, 2, 2).add();
            }
            if (face.equals("human")) {
                p("nose", "head", skin).at(0, -headH * 0.5f, -headD / 2f).rot(-8, 0, 0).mat(skinMat).box(-0.5f, 0, -1, 1, 2, 1).add();
                s("ear", "head", false, skin).at(headW / 2f, -headH * 0.5f, 0.5f).mat(skinMat).box(-0.2f, -1, -0.5f, 1, 2, 1).add();
                p("chin", "head", skin).at(0, 0, -headD / 2f).mat(skinMat).box(-1.5f, -1.2f, -0.4f, 3, 1, 1).add();
            }
            return this;
        }

        /** Arms: shoulder, upper arm (ARM role), elbow, forearm, wrist, palm, fingers and thumb. */
        Fig armsBuild(int upperCol, String upperMat, int foreCol, String foreMat, int handCol, String handMat) {
            float ax = chestW / 2f + armW / 2f - 0.5f;
            int up = upperLen();
            int fo = foreLen();
            part(b, "arm_l", "chest", Role.ARM_LEFT, upperCol).at(ax, -chestH + 1.5f, 0).rot(armPitchL - hunch, 0, -spread).mat(upperMat)
                    .box(-armW / 2f, -1.5f, -armW / 2f, armW, up + 1, armW).add();
            part(b, "arm_r", "chest", Role.ARM_RIGHT, upperCol).at(-ax, -chestH + 1.5f, 0).rot(armPitchR - hunch, 0, spread).mat(upperMat)
                    .box(-armW / 2f, -1.5f, -armW / 2f, armW, up + 1, armW).add();
            s("forearm", "arm", true, foreCol).at(0, up - 0.5f, 0).rot(-foreBend, 0, 0).mat(foreMat)
                    .box(-foreW / 2f, 0, -foreW / 2f, foreW, fo, foreW).add();
            s("hand", "forearm", true, handCol).at(0, fo, 0).rot(foreBend * 0.4f, 0, 0).mat(handMat)
                    .box(-foreW / 2f + 0.25f, 0, -foreW / 2f + 0.25f, Math.max(2, foreW), 2, Math.max(2, foreW - 1)).add();
            s("fingers", "hand", true, darker(handCol, 0.92f)).at(0, 2, -foreW / 2f + 0.5f).rot(25, 0, 0).mat(handMat)
                    .box(-foreW / 2f + 0.25f, 0, -0.5f, Math.max(2, foreW), 2, Math.max(1, foreW - 2)).add();
            s("thumb", "hand", true, handCol).at(-foreW / 2f + 0.5f, 0.5f, -foreW / 2f + 0.5f).rot(-10, 0, 20).mat(handMat)
                    .box(-1, 0, -1, 1, 2, 1).add();
            s("knuckles", "fingers", true, darker(handCol, 0.8f)).at(0, 0, -0.75f).mat(handMat)
                    .box(-foreW / 2f + 0.25f, -0.25f, -0.25f, Math.max(2, foreW), 1, 1, 0.1f).add();
            s("bicep", "arm", true, upperCol).at(0, 0, 0).mat(upperMat).box(-armW / 2f + 0.5f, 1, -armW / 2f - 0.5f, armW - 1, Math.max(2, up - 2), 1).add();
            return this;
        }

        /** Legs: thigh (LEG role), knee, shin and a shaped foot with a toe cap. */
        Fig legsBuild(int thighCol, String thighMat, int shinCol, String shinMat, int footCol, String footMat) {
            float lx = hipW / 4f + 0.25f;
            int th = thighLen();
            int sh = shinLen();
            s("leg", null, false, thighCol).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).at(lx, hipY(), 0.2f).rot(-kneeBend * 0.5f, 0, -stance)
                    .mat(thighMat).box(-legW / 2f, -1, -legW / 2f, legW, th + 1, legW).add();
            s("shin", "leg", true, shinCol).at(0, th, 0).rot(kneeBend, 0, stance).mat(shinMat)
                    .box(-legW / 2f + 0.25f, 0, -legW / 2f + 0.25f, legW, sh, legW, -0.25f).add();
            s("foot", "shin", true, footCol).at(0, sh, 0).rot(-kneeBend * 0.5f, 0, 0).mat(footMat)
                    .box(-legW / 2f, 0, -legW / 2f - 1, legW, 2, legW + 1, 0.1f).add();
            s("toe", "foot", true, darker(footCol, 0.85f)).at(0, 0.5f, -legW / 2f - 1).mat(footMat)
                    .box(-legW / 2f + 0.5f, 0, -1, legW - 1, 1, 1, 0.15f).add();
            s("calf", "shin", true, darker(shinCol, 0.92f)).at(0, 1, legW / 2f - 0.25f).mat(shinMat)
                    .box(-legW / 2f + 0.75f, 0, -0.5f, Math.max(1, legW - 1), Math.max(2, sh - 3), 1).add();
            s("heel", "foot", true, darker(footCol, 0.85f)).at(0, 0.5f, legW / 2f - 0.5f).mat(footMat)
                    .box(-legW / 2f + 0.5f, 0, -0.5f, legW - 1, 1, 1, 0.15f).add();
            return this;
        }

        /** Digitigrade legs (demons, aviansies): thigh angled forward, shank back, hock, then a hoof or talons. */
        Fig beastLegs(int col, String mat, int footCol, String footMat, boolean talons) {
            float lx = hipW / 4f + 0.5f;
            int th = Math.max(3, Math.round(legLen * 0.42f));
            int sk = Math.max(3, Math.round(legLen * 0.38f));
            int hk = Math.max(2, legLen - th - sk);
            double drop = th * Math.cos(Math.toRadians(28)) + (sk + 0.5) * Math.cos(Math.toRadians(34)) + hk + 0.5 + 2;
            s("leg", null, false, col).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).at(lx, 24 - (float) drop, 0.5f).rot(-28, 0, -stance)
                    .mat(mat).box(-legW / 2f, -1, -legW / 2f, legW, th + 1, legW, 0.2f).add();
            s("shank", "leg", true, darker(col, 0.9f)).at(0, th, 0).rot(62, 0, stance * 0.5f).mat(mat)
                    .box(-legW / 2f + 0.5f, 0, -legW / 2f + 0.5f, legW - 1, sk + 1, legW - 1).add();
            s("hock", "shank", true, darker(col, 0.85f)).at(0, sk + 0.5f, 0).rot(-36, 0, 0).mat(mat)
                    .box(-legW / 2f + 0.75f, 0, -legW / 2f + 0.75f, Math.max(2, legW - 2), hk + 1, Math.max(2, legW - 2)).add();
            if (talons) {
                s("foot", "hock", true, footCol).at(0, hk + 0.5f, -0.5f).rot(2, 0, 0).mat(footMat).box(-1.5f, 0, -1.5f, 3, 1, 3).add();
                for (int k = 0; k < 3; k++) {
                    s("talon" + k, "foot", true, darker(footCol, 0.6f)).at(-1 + k, 0.5f, -1.5f).rot(-20, (k - 1) * -20, 0).mat("horn")
                            .box(-0.5f, -0.5f, -3, 1, 1, 3, -0.05f).add();
                }
                s("talon_back", "foot", true, darker(footCol, 0.6f)).at(0, 0.5f, 1.5f).rot(20, 0, 0).mat("horn").box(-0.5f, -0.5f, 0, 1, 1, 2).add();
            } else {
                s("foot", "hock", true, footCol).at(0, hk + 0.5f, -0.5f).rot(2, 0, 0).mat(footMat)
                        .box(-legW / 2f + 0.25f, 0, -legW / 2f - 0.5f, legW, 2, legW + 1).add();
                s("hoof_split", "foot", true, darker(footCol, 0.5f)).at(0, 0, -legW / 2f - 0.5f).mat(footMat)
                        .box(-0.5f, 0, -0.2f, 1, 2, 1).add();
            }
            return this;
        }

        /** Muzzle, hinged jaw (JAW role) with fangs and an optional glowing maw, for demons and beasts. */
        Fig beastFace(int snoutCol, int jawCol, int fangCol, boolean glowMaw) {
            float hd = headD / 2f;
            p("snout", "head", snoutCol).at(0, -headH * 0.38f, -hd).rot(6, 0, 0).mat(skinMat)
                    .box(-headW / 2f + 1, 0, -2, headW - 2, Math.max(2, headH / 3), 2).add();
            p("nostrils", "snout", darker(snoutCol, 0.6f)).at(0, 0, -2).mat(skinMat).box(-1.5f, 0.2f, -0.3f, 3, 1, 1, -0.2f).add();
            part(b, "jaw", "head", Role.JAW, jawCol).at(0, -1, -hd + 1).rot(8, 0, 0).mat(skinMat).glow(glowMaw ? "maw" : null)
                    .box(-headW / 2f + 0.5f, 0, -2.5f, headW - 1, 2, headD - 2).add();
            s("fang", "jaw", false, fangCol).at(headW / 2f - 1.5f, 0, -2.2f).rot(-8, 0, 6).mat("horn").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
            s("cheek", "head", false, darker(skin, 0.85f)).at(headW / 2f, -headH * 0.35f, -hd + 1.5f).rot(0, -15, 0).mat(skinMat)
                    .box(-0.6f, -1.5f, -1.5f, 1, 3, 3).add();
            return this;
        }

        /** Three claws on each hand's fingers. */
        Fig claws(int col, int len) {
            for (int k = 0; k < 3; k++) {
                s("claw" + k, "fingers", true, col).at(-foreW / 2f + 0.75f + k * (foreW - 1) / 2f, 1.5f, -0.25f).rot(30, 0, 0).mat("horn")
                        .box(-0.5f, 0, -0.5f, 1, len, 1, -0.1f).add();
            }
            return this;
        }

        /** A row of spikes down the back of the chest. */
        Fig backSpikes(int col, int n, int size) {
            for (int i = 0; i < n; i++) {
                p("back_spike" + i, "chest", i % 2 == 0 ? col : darker(col, 0.85f)).at(0, -chestH + 0.5f + i * (chestH - 1f) / n, depth / 2f + 0.5f)
                        .rot(-55, 0, 0).mat("horn").box(-size / 2f, -size - 1, -size / 2f, size, size + 1, size).add();
            }
            return this;
        }

        // ------------------------------------------------------------------------------------------
        // armour & clothing
        // ------------------------------------------------------------------------------------------

        /** Platebody: breastplate with a raised centre ridge, a rim, abdomen lames and a backplate. */
        Fig platebody(int col, int trim) {
            int dk = darker(col, 0.8f);
            float fz = -depth / 2f - 0.5f;
            p("breastplate", "chest", col).accent(trim).at(0, -chestH, fz).mat("metal")
                    .box(-chestW / 2f + 0.5f, 0.5f, -1, chestW - 1, chestH - 1, 1, 0.1f).add();
            p("breast_ridge", "breastplate", lighter(col, 0.15f)).at(0, 0.5f, -1).mat("metal").box(-0.5f, 0, -0.6f, 1, chestH - 1, 1).add();
            p("gorget", "chest", dk).at(0, -chestH, 0).mat("metal").box(-chestW / 2f + 1.5f, -1, -depth / 2f - 0.5f, chestW - 3, 2, depth + 1, 0.05f).add();
            for (int i = 0; i < Math.max(1, absH / 2); i++) {
                p("lame" + i, "waist", i % 2 == 0 ? col : dk).accent(trim).at(0, -absH + 0.5f + i * 2, -(depth - 1) / 2f).mat("metal")
                        .box(-waistW / 2f - 0.5f, 0, -0.75f, waistW + 1, 2, depth, 0.05f).add();
            }
            p("backplate", "chest", dk).at(0, -chestH + 0.5f, depth / 2f + 0.5f).mat("metal").box(-chestW / 2f + 1, 0, -0.4f, chestW - 2, chestH - 1, 1).add();
            if (trim >= 0) {
                p("plate_trim", "breastplate", trim).at(0, chestH - 1.5f, -1).mat("metal").box(-chestW / 2f + 0.5f, 0, -0.5f, chestW - 1, 1, 1, 0.05f).add();
                s("plate_trim_side", "breastplate", false, trim).at(chestW / 2f - 1, 0.5f, -1).mat("metal").box(-0.5f, 0, -0.5f, 1, chestH - 2, 1).add();
            }
            return this;
        }

        /** Layered pauldrons: 2-3 overlapping rotated plates with an optional rim colour and spikes. */
        Fig pauldrons(int col, int rim, int layers, int spikes, int spikeCol) {
            int dk = darker(col, 0.8f);
            s("pauldron", "arm", true, col).accent(rim).at(0, -1.5f, 0).rot(0, 0, -14).mat("metal")
                    .box(-armW / 2f - 1, -1.5f, -armW / 2f - 1, armW + 3, 3, armW + 2, 0.1f).add();
            for (int i = 1; i < layers; i++) {
                s("pauldron" + i, "pauldron", true, i % 2 == 1 ? dk : col).at(0.5f, 1.4f * i, 0).rot(0, 0, 6 * i).mat("metal")
                        .box(-armW / 2f - 0.5f, 0, -armW / 2f - 0.75f, armW + 3, 2, armW + 2).add();
            }
            if (rim >= 0) {
                s("pauldron_rim", "pauldron", true, rim).at(0, 1.4f, 0).mat("metal")
                        .box(-armW / 2f - 1, 0, -armW / 2f - 1.25f, armW + 3, 1, 1, 0.05f).add();
            }
            for (int k = 0; k < spikes; k++) {
                s("pauldron_spike" + k, "pauldron", true, spikeCol).at(-0.5f + k * 1.6f - (spikes - 1) * 0.8f + 1, -1.5f, 0)
                        .rot(0, 0, -20 - k * 8).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
            }
            return this;
        }

        /** Plate arms: rerebrace band, couter (elbow cop) and vambrace with a flared cuff. */
        Fig armPlates(int col, int rim) {
            int dk = darker(col, 0.8f);
            s("couter", "forearm", true, dk).accent(rim).at(0, 0, 0).mat("metal").box(-foreW / 2f - 0.5f, -1, -foreW / 2f - 0.5f, foreW + 1, 2, foreW + 1).add();
            s("couter_wing", "couter", true, col).at(0, 0, foreW / 2f + 0.5f).rot(20, 0, 0).mat("metal").box(-1, -1, 0, 2, 2, 1).add();
            s("vambrace", "forearm", true, col).at(0, foreLen() - 2.5f, 0).mat("metal")
                    .box(-foreW / 2f - 0.5f, 0, -foreW / 2f - 0.5f, foreW + 1, 2, foreW + 1, 0.1f).add();
            return this;
        }

        /** Greaves: knee cop with a wing, shin guard and a sabaton plate over the foot. */
        Fig legPlates(int col, int rim) {
            int dk = darker(col, 0.8f);
            s("knee", "shin", true, col).accent(rim).at(0, 0, -legW / 2f).mat("metal").box(-legW / 2f + 0.25f, -1.5f, -1, legW, 3, 2).add();
            s("knee_wing", "knee", true, dk).at(legW / 2f - 0.5f, 0, 0).rot(0, -20, 0).mat("metal").box(0, -1, -0.5f, 1, 2, 2).add();
            s("greave", "shin", true, dk).at(0, 2, -legW / 2f).mat("metal").box(-legW / 2f + 0.5f, 0, -0.75f, legW - 1, shinLen() - 2, 1).add();
            s("sabaton", "foot", true, col).at(0, -0.5f, -legW / 2f - 1).rot(10, 0, 0).mat("metal").box(-legW / 2f + 0.25f, 0, -0.5f, legW, 1, 3, 0.1f).add();
            return this;
        }

        /** Tassets: plate flaps hanging from the belt over the thighs (front, sides), slightly flared. */
        Fig tassets(int col, int rim, int len) {
            int dk = darker(col, 0.82f);
            float fz = -depth / 2f - 0.5f;
            s("tasset", "hips", false, col).accent(rim).at(hipW / 4f + 0.25f, 1, fz).rot(-6, 0, -8).mat("metal")
                    .box(-legW / 2f - 0.5f, 0, -0.5f, legW + 1, len, 1, 0.05f).add();
            s("tasset_low", "tasset", true, dk).at(0, len - 0.5f, -0.2f).rot(-4, 0, 0).mat("metal")
                    .box(-legW / 2f - 0.25f, 0, -0.5f, legW + 1, 2, 1).add();
            s("tasset_side", "hips", false, dk).at(hipW / 2f + 0.25f, 1, 0).rot(0, 0, -12).mat("metal")
                    .box(-0.5f, 0, -depth / 2f + 0.5f, 1, len - 1, depth - 1).add();
            if (rim >= 0) {
                s("tasset_rim", "tasset_low", true, rim).at(0, 1.5f, 0).mat("metal").box(-legW / 2f - 0.25f, 0, -0.7f, legW + 1, 1, 1).add();
            }
            p("tasset_back", "hips", dk).at(0, 1, depth / 2f).rot(8, 0, 0).mat("metal").box(-hipW / 2f, 0, -0.5f, hipW, len, 1).add();
            return this;
        }

        /** A belt with buckle and (optionally) a pouch. */
        Fig belt(int col, int buckle, boolean pouch) {
            p("belt", "hips", col).at(0, -2, 0).mat("leather").box(-hipW / 2f, 0, -depth / 2f, hipW, 1, depth, 0.3f).add();
            p("belt_buckle", "belt", buckle).at(0, 0, -depth / 2f).mat("metal").box(-1, -0.25f, -0.9f, 2, 1, 1, 0.1f).add();
            if (pouch) {
                p("pouch", "belt", darker(col, 0.85f)).at(hipW / 2f - 1, 1, -depth / 2f + 1).rot(0, 20, 0).mat("leather").box(-1, 0, -1, 2, 2, 2).add();
            }
            return this;
        }

        /** Skirt of separate panels (front, back, two sides) flaring out: robes, tunics, plateskirts, chainskirts. */
        Fig skirt(int col, String mat, int len, int hem, float flare) {
            int dk = darker(col, 0.85f);
            float fz = -depth / 2f - 0.5f;
            p("skirt_front", "hips", col).at(0, 0, fz).rot(-flare, 0, 0).mat(mat).box(-hipW / 2f + 1, -1, -0.5f, hipW - 2, len, 1, 0.05f).add();
            p("skirt_back", "hips", dk).at(0, 0, depth / 2f + 0.5f).rot(flare, 0, 0).mat(mat).box(-hipW / 2f, -1, -0.5f, hipW, len, 1, 0.05f).add();
            s("skirt_side", "hips", false, col).at(hipW / 2f + 0.5f, 0, 0).rot(0, 0, -flare).mat(mat)
                    .box(-0.5f, -1, -depth / 2f - 0.5f, 1, len, depth + 1, 0.05f).add();
            s("skirt_corner", "hips", false, dk).at(hipW / 2f - 0.5f, 0, fz).rot(-flare * 0.7f, 0, -flare * 0.7f).mat(mat)
                    .box(-1.5f, -1, -0.5f, 2, len - 1, 1).add();
            if (hem >= 0) {
                p("hem_front", "skirt_front", hem).at(0, len - 1.5f, 0).mat(mat).box(-hipW / 2f + 1, 0, -0.75f, hipW - 2, 1, 1, 0.1f).add();
                p("hem_back", "skirt_back", hem).at(0, len - 1.5f, 0).mat(mat).box(-hipW / 2f, 0, -0.25f, hipW, 1, 1, 0.1f).add();
                s("hem_side", "skirt_side", true, hem).at(0, len - 1.5f, 0).mat(mat).box(-0.25f, 0, -depth / 2f - 0.5f, 1, 1, depth + 1, 0.1f).add();
            }
            return this;
        }

        /** Loincloth flaps front and back. */
        Fig loincloth(int col, int len) {
            p("loin_front", "hips", col).at(0, 0, -depth / 2f - 0.3f).rot(-4, 0, 0).mat("leather").box(-1.5f, -1, -0.5f, 3, len, 1).add();
            p("loin_back", "hips", darker(col, 0.85f)).at(0, 0, depth / 2f + 0.3f).rot(4, 0, 0).mat("leather").box(-2, -1, -0.5f, 4, len, 1).add();
            return this;
        }

        /** A cape hanging from the shoulders in three segments (collar, body, tattered hem). */
        Fig cape(int col, int lining, int len, boolean tattered) {
            int dk = darker(col, 0.85f);
            float z = depth / 2f + 0.8f;
            int w = chestW + 1;
            p("cape_collar", "chest", lining >= 0 ? lining : dk).at(0, -chestH - 0.5f, 0).mat("cloth")
                    .box(-w / 2f, -1, -depth / 2f - 0.5f, w, 2, depth + 2, 0.1f).add();
            p("cape", "chest", col).accent(lining >= 0 ? lining : dk).at(0, -chestH + 0.5f, z).rot(8 + hunch * -0.3f, 0, 0).mat("cloth")
                    .box(-w / 2f, 0, 0, w, len / 2, 1).add();
            p("cape_low", "cape", dk).at(0, len / 2f - 0.2f, 0).rot(6, 0, 0).mat("cloth").box(-w / 2f - 0.5f, 0, 0, w + 1, len - len / 2 - (tattered ? 2 : 0), 1).add();
            if (tattered) {
                for (int i = 0; i < 3; i++) {
                    p("cape_tatter" + i, "cape_low", darker(col, 0.7f)).at(-w / 2f + 1 + i * (w - 2) / 2f, len - len / 2f - 2.2f, 0)
                            .rot(4, 0, (i - 1) * 6).mat("cloth").box(-1, 0, 0, 2, 2 + (i % 2), 1).add();
                }
            }
            if (lining >= 0) {
                p("cape_lining", "cape", lining).at(0, 0, -0.1f).mat("cloth").box(-w / 2f + 0.5f, 0.5f, -0.3f, w - 1, len / 2 - 1, 1, -0.2f).add();
            }
            return this;
        }

        /** Wide robe sleeves: a flared cuff on the forearm and a sleeve drape. */
        Fig robeSleeves(int col, int trim) {
            int fo = foreLen();
            s("cuff", "forearm", true, col).at(0, fo - 3, 0).rot(10, 0, 0).mat("cloth").box(-foreW / 2f - 1, 0, -foreW / 2f - 1, foreW + 2, 3, foreW + 2).add();
            if (trim >= 0) {
                s("cuff_trim", "cuff", true, trim).at(0, 2.5f, 0).mat("cloth").box(-foreW / 2f - 1, 0, -foreW / 2f - 1, foreW + 2, 1, foreW + 2, 0.1f).add();
            }
            s("sleeve_drape", "cuff", true, darker(col, 0.85f)).at(0, 3, foreW / 2f + 0.5f).rot(25, 0, 0).mat("cloth").box(-foreW / 2f - 0.5f, 0, -0.5f, foreW + 1, 2, 1).add();
            return this;
        }

        /** Boots with a folded top. */
        Fig boots(int col, String mat) {
            s("boot_top", "shin", true, darker(col, 0.9f)).at(0, shinLen() - 3, 0).mat(mat)
                    .box(-legW / 2f, 0, -legW / 2f, legW, 1, legW, 0.35f).add();
            return this;
        }

        /** Gloves/gauntlet cuffs. */
        Fig gauntlets(int col, String mat) {
            s("gauntlet", "hand", true, col).at(0, -0.5f, 0).mat(mat).box(-foreW / 2f - 0.25f, 0, -foreW / 2f - 0.25f, foreW + 1, 1, foreW + 1, 0.1f).add();
            return this;
        }

        // ------------------------------------------------------------------------------------------
        // heads: hair, beards, helms
        // ------------------------------------------------------------------------------------------

        Fig hair(int col, String style) {
            float t = -headH;
            float hw = headW / 2f;
            float hd = headD / 2f;
            switch (style) {
                case "short" -> {
                    p("hair", "head", col).at(0, t, 0).mat("hair").box(-hw, -1, -hd, headW, 2, headD, 0.3f).box(-hw, 1, hd - 2, headW, 3, 2, 0.3f).add();
                    p("fringe", "hair", darker(col, 0.9f)).at(0, 1, -hd).rot(10, 0, 0).mat("hair").box(-hw + 0.5f, 0, -0.5f, headW - 1, 1, 1).add();
                }
                case "slick" -> {
                    p("hair", "head", col).at(0, t, 0).mat("hair").box(-hw, -1, -hd, headW, 2, headD, 0.3f).box(-hw, 1, hd - 3, headW, 4, 3, 0.3f).add();
                    p("widows_peak", "hair", col).at(0, 1, -hd).rot(0, 0, 45).mat("hair").box(-1, -1, -0.6f, 2, 2, 1).add();
                    s("hair_temple", "hair", false, col).at(hw, 1, -1).mat("hair").box(-0.2f, 0, -1, 1, 3, 3, 0.1f).add();
                }
                case "long" -> {
                    p("hair", "head", col).at(0, t, 0).mat("hair").box(-hw, -1, -hd, headW, 2, headD, 0.3f).add();
                    p("hair_back", "head", col).at(0, t + 1, hd - 0.5f).rot(6, 0, 0).mat("hair").box(-hw - 0.5f, 0, -0.5f, headW + 1, headH + 4, 2).add();
                    s("hair_side", "head", false, darker(col, 0.92f)).at(hw, t + 1, -0.5f).mat("hair").box(-0.2f, 0, -hd + 1, 1, headH - 1, headD - 1, 0.1f).add();
                    p("hair_tip", "hair_back", darker(col, 0.85f)).at(0, headH + 3.5f, 0).rot(10, 0, 0).mat("hair").box(-hw + 0.5f, 0, -0.5f, headW - 1, 2, 1).add();
                }
                case "wild" -> {
                    p("hair", "head", col).at(0, t, 0).mat("hair").box(-hw, -1.5f, -hd, headW, 2, headD, 0.5f).add();
                    for (int k = 0; k < 5; k++) {
                        float a = (k - 2) * 22f;
                        p("hair_tuft" + k, "hair", k % 2 == 0 ? col : darker(col, 0.85f)).at((k - 2) * 1.6f, -1, (k % 2) * 2 - 1).rot(-15 + (k % 2) * 30, 0, a)
                                .mat("hair").box(-1, -3, -1, 2, 3, 2).add();
                    }
                    s("hair_side", "head", false, col).at(hw, t + 1, 0.5f).rot(0, 0, -15).mat("hair").box(-0.2f, 0, -2, 2, 4, 4).add();
                }
                case "bald_fringe" -> s("hair_side", "head", false, col).at(hw, -headH * 0.55f, 1).mat("hair")
                        .box(-0.3f, -1, -2, 1, 3, 4, 0.1f).add();
                case "mohawk" -> {
                    for (int k = 0; k < 4; k++) {
                        p("mohawk" + k, "head", col).at(0, t, -hd + 1.5f + k * 1.8f).rot(-20 + k * 14, 0, 0).mat("hair").box(-0.5f, -3 - (k == 1 ? 1 : 0), -1, 1, 3 + (k == 1 ? 1 : 0), 2).add();
                    }
                }
                default -> {
                }
            }
            return this;
        }

        Fig beard(int col, String style) {
            float fz = -headD / 2f;
            switch (style) {
                case "long" -> {
                    p("beard", "head", col).at(0, -headH * 0.32f, fz).rot(6, 0, 0).mat("hair").box(-headW / 2f + 0.5f, 0, -1, headW - 1, 5, 1).add();
                    p("beard_tip", "beard", darker(col, 0.9f)).at(0, 5, 0).rot(8, 0, 0).mat("hair").box(-2, 0, -1, 4, 3, 1).add();
                    p("moustache", "head", darker(col, 0.9f)).at(0, -headH * 0.36f, fz).mat("hair").box(-2.5f, 0, -1.4f, 5, 1, 1).add();
                }
                case "full" -> {
                    p("beard", "head", col).at(0, -headH * 0.35f, fz).rot(4, 0, 0).mat("hair").box(-headW / 2f, 0, -1, headW, 4, 2).add();
                    p("moustache", "head", darker(col, 0.9f)).at(0, -headH * 0.38f, fz).mat("hair").box(-2.5f, 0, -1.4f, 5, 1, 1).add();
                }
                case "goatee" -> p("beard", "head", col).at(0, -1.5f, fz).rot(10, 0, 0).mat("hair").box(-1, 0, -0.8f, 2, 3, 1).add();
                case "moustache" -> p("moustache", "head", col).at(0, -headH * 0.36f, fz).mat("hair").box(-2.5f, 0, -1.2f, 5, 1, 1).add();
                default -> {
                }
            }
            return this;
        }

        /**
         * Helmets. "full" (great helm with visor slit and crest ridge), "med" (open helm with nose guard), "horned",
         * "coif" (mail/leather coif), "hood" (deep hood with peak and shadowed opening), "crown", "wizard", "greathelm".
         */
        Fig helm(String type, int col, int trim) {
            float hw = headW / 2f;
            float hd = headD / 2f;
            float t = -headH;
            int dk = darker(col, 0.8f);
            switch (type) {
                case "full" -> {
                    p("helm", "head", col).accent(trim).at(0, 0, 0).mat("metal").pattern("visor")
                            .box(-hw - 0.5f, t - 0.5f, -hd - 0.5f, headW + 1, headH + 1, headD + 1, 0.05f).add();
                    p("helm_ridge", "helm", trim >= 0 ? trim : dk).at(0, t - 0.5f, 0).mat("metal").box(-0.5f, -1, -hd - 0.5f, 1, 1, headD + 1).add();
                    p("helm_brim", "helm", dk).at(0, t + headH * 0.42f, -hd - 0.5f).mat("metal").box(-hw - 0.5f, -0.5f, -0.6f, headW + 1, 1, 1).add();
                    s("helm_cheek", "helm", false, dk).at(hw + 0.5f, -2, -hd + 1).mat("metal").box(-0.4f, -2, -1.5f, 1, 3, 3).add();
                }
                case "greathelm" -> {
                    p("helm", "head", col).accent(trim).at(0, 0, 0).mat("metal").pattern("visor")
                            .box(-hw - 0.75f, t - 0.5f, -hd - 0.75f, headW + 2, headH + 1, headD + 2, -0.25f).add();
                    p("helm_crown", "helm", dk).at(0, t - 0.5f, 0).mat("metal").box(-hw, -1, -hd, headW, 1, headD).add();
                    p("helm_face", "helm", lighter(col, 0.1f)).at(0, t + 1, -hd - 0.75f).mat("metal").box(-hw + 0.5f, 0, -0.7f, headW - 1, headH - 1, 1).add();
                    p("helm_breaths", "helm", DARK).at(0, t + headH * 0.62f, -hd - 1.3f).mat("metal").box(-hw + 1.5f, 0, -0.3f, headW - 3, 1, 1).add();
                }
                case "med" -> {
                    p("helm", "head", col).accent(trim).at(0, t, 0).mat("metal").box(-hw - 0.5f, -1, -hd - 0.5f, headW + 1, 4, headD + 1, 0.05f).add();
                    p("helm_dome", "helm", lighter(col, 0.08f)).at(0, -1, 0).mat("metal").box(-hw + 0.5f, -1, -hd + 0.5f, headW - 1, 1, headD - 1).add();
                    p("helm_nasal", "helm", dk).at(0, 3, -hd - 0.5f).mat("metal").box(-0.5f, 0, -0.6f, 1, 3, 1).add();
                    s("helm_guard", "helm", false, dk).at(hw + 0.5f, 3, 0).mat("metal").box(-0.4f, 0, -hd + 0.5f, 1, 3, headD - 1).add();
                    p("helm_back", "helm", dk).at(0, 3, hd + 0.5f).mat("metal").box(-hw, 0, -0.4f, headW, 3, 1).add();
                }
                case "horned" -> {
                    helm("med", col, trim);
                    s("helm_horn", "helm", false, IVORY).at(hw, 1, 0).rot(0, 0, -50).mat("horn").box(0, -1, -1, 3, 2, 2).add();
                    s("helm_horn_tip", "helm_horn", true, darker(IVORY, 0.85f)).at(3, 0, 0).rot(0, 0, 60).mat("horn").box(0, -0.5f, -0.5f, 3, 1, 1).add();
                }
                case "coif", "leather_coif" -> {
                    String m = type.equals("coif") ? "chain" : "leather";
                    p("coif", "head", col).accent(trim).at(0, t, 0).mat(m).box(-hw - 0.5f, -1, -hd - 0.5f, headW + 1, 3, headD + 1, 0.1f).add();
                    p("coif_back", "head", dk).at(0, t + 2, hd - 1.5f).mat(m).box(-hw - 0.5f, 0, -0.5f, headW + 1, headH - 1, 3, 0.1f).add();
                    s("coif_side", "head", false, col).at(hw + 0.1f, t + 2, -hd).mat(m).box(-0.5f, 0, -0.5f, 1, headH - 2, headD - 1, 0.1f).add();
                    p("coif_chin", "head", dk).at(0, -0.5f, -hd - 0.3f).mat(m).box(-hw + 0.5f, -1.5f, -0.4f, headW - 1, 2, 1).add();
                    p("coif_mantle", "chest", col).at(0, -chestH, 0).mat(m).box(-chestW / 2f + 0.5f, -1.5f, -depth / 2f - 0.75f, chestW - 1, 3, depth + 2).add();
                }
                case "hood" -> {
                    p("hood", "head", col).accent(trim).at(0, 0, 0.5f).mat("cloth").box(-hw - 0.5f, t - 0.5f, -hd - 0.5f, headW + 1, headH, headD + 1, 0.25f).add();
                    p("hood_peak", "hood", dk).at(0, t - 0.5f, -hd - 0.5f).rot(28, 0, 0).mat("cloth").box(-hw + 0.5f, -1, -1.5f, headW - 1, 2, 3).add();
                    p("hood_crown", "hood", col).at(0, t - 0.5f, 1).rot(-20, 0, 0).mat("cloth").box(-2, -1.5f, -2, 4, 2, 4).add();
                    p("hood_drape", "hood", dk).at(0, -1, hd + 0.5f).rot(12, 0, 0).mat("cloth").box(-hw - 0.5f, 0, -0.5f, headW + 1, 4, 1).add();
                    s("hood_side", "hood", false, col).at(hw + 0.5f, t + 1, -hd - 0.5f).mat("cloth").box(-1, 0, -0.75f, 1, headH, 2).add();
                    p("hood_shadow", "head", DARK).at(0, t + 1.2f, -hd - 0.15f).mat("cloth").box(-hw + 0.5f, 0, -0.6f, headW - 1, headH - 2, 1).add();
                    if (trim >= 0) {
                        p("hood_trim", "hood", trim).at(0, t - 0.5f, -hd - 0.75f).mat("cloth").box(-hw - 0.5f, 0, -0.3f, headW + 1, 1, 1, 0.1f).add();
                    }
                }
                case "crown" -> {
                    p("crown", "head", col).at(0, t, 0).mat("metal").box(-hw - 0.25f, -1.5f, -hd - 0.25f, headW, 2, headD, 0.25f).add();
                    for (int k = 0; k < 3; k++) {
                        p("crown_point" + k, "crown", col).at((k - 1) * (hw - 1), -1.5f, -hd).mat("metal").box(-0.5f, -2 + (k == 1 ? -1 : 0), -0.5f, 1, 2 + (k == 1 ? 1 : 0), 1).add();
                    }
                    if (trim >= 0) {
                        p("crown_gem", "crown", trim).at(0, -1, -hd - 0.4f).mat("energy").glow("full").box(-0.5f, -0.5f, -0.5f, 1, 1, 1).add();
                    }
                }
                case "wizard" -> {
                    p("hat_brim", "head", col).at(0, t + 0.5f, 0).mat("cloth").box(-hw - 1.5f, -1, -hd - 1.5f, headW + 3, 1, headD + 3).add();
                    p("hat", "hat_brim", col).accent(trim).at(0, -1, 0).rot(-6, 0, 0).mat("cloth").box(-hw + 0.5f, -3, -hd + 0.5f, headW - 1, 3, headD - 1).add();
                    p("hat_mid", "hat", dk).at(0, -3, 0.5f).rot(-12, 0, 0).mat("cloth").box(-2, -3, -2, 4, 3, 4).add();
                    p("hat_tip", "hat_mid", col).at(0, -3, 0.5f).rot(-20, 0, 0).mat("cloth").box(-1, -2, -1, 2, 2, 2).add();
                }
                default -> {
                }
            }
            return this;
        }

        // ------------------------------------------------------------------------------------------
        // horns, wings, tails
        // ------------------------------------------------------------------------------------------

        /** Curving three-segment horns on the head. */
        Fig horns(int col, float x, float y, float z, float out, float back, int size) {
            int tip = darker(col, 0.7f);
            s("horn", "head", false, col).at(x, y, z).rot(-back, 0, -out).mat("horn").box(-size / 2f, -size - 1, -size / 2f, size, size + 1, size).add();
            s("horn_mid", "horn", true, darker(col, 0.88f)).at(0, -size - 1, 0).rot(-back * 0.8f, 0, out * 0.6f).mat("horn")
                    .box(-(size - 1) / 2f, -size - 1, -(size - 1) / 2f, Math.max(1, size - 1), size + 1, Math.max(1, size - 1)).add();
            s("horn_tip", "horn_mid", true, tip).at(0, -size - 1, 0).rot(-back * 0.6f, 0, out * 0.5f).mat("horn")
                    .box(-0.5f, -size, -0.5f, 1, size, 1).add();
            return this;
        }

        /**
         * Layered feathered wings: an arm bone, a covert layer and a fan of separate primary feathers, each rotated
         * a little further so the trailing edge reads as feathers.
         */
        Fig featherWings(String parent, int col, int tip, int under, float x, float y, float z, int span, int feathers) {
            int dk = darker(col, 0.85f);
            s("wing", parent, false, col).roles(Role.WING_LEFT, Role.WING_RIGHT).at(x, y, z).rot(-8, -28, -28).mat("feathers")
                    .box(0, -1.5f, -1, span, 3, 2).add();
            s("wing_covert", "wing", true, dk).at(0, 1.5f, 0).mat("feathers").box(0, 0, -0.5f, span - 1, 4, 1).add();
            s("wing_under", "wing", true, under).at(1, 1, 0.5f).mat("feathers").box(0, 0, 0, span - 3, 3, 1, -0.1f).add();
            for (int k = 0; k < feathers; k++) {
                float fx = span * (k + 0.5f) / feathers;
                int len = 6 + k * 6 / Math.max(1, feathers - 1);
                s("feather" + k, "wing", true, k % 2 == 0 ? tip : darker(tip, 0.88f)).at(fx, 4, k % 2 == 0 ? -0.2f : 0.2f)
                        .rot(0, 0, -8 - k * (40f / feathers)).mat("feathers").box(-1, 0, -0.5f, 2, len, 1).add();
            }
            s("wing_tip", "wing", true, tip).at(span, 0, 0).rot(0, 0, 35).mat("feathers").box(-0.5f, -1, -0.5f, 3, 9, 1).add();
            return this;
        }

        /** Bat/demon wings: a jointed bone arm with clawed fingers and membrane panels between them. */
        Fig demonWings(String parent, int bone, int membrane, int claw, float x, float y, float z, int span, int fingers, String glow) {
            s("wing", parent, false, bone).roles(Role.WING_LEFT, Role.WING_RIGHT).at(x, y, z).rot(-10, -30, -25).mat("hide")
                    .box(0, -1, -1, span / 2, 2, 2).add();
            s("wing_fore", "wing", true, bone).at(span / 2f, 0, 0).rot(0, 0, -25).mat("hide").box(0, -0.75f, -0.75f, span / 2, 2, 2, -0.25f).add();
            s("wing_claw", "wing_fore", true, claw).at(span / 2f, 0, 0).rot(0, 0, -30).mat("horn").box(0, -0.5f, -0.5f, 2, 1, 1).add();
            s("wing_web_in", "wing", true, membrane).accent(claw).at(0, 0.5f, 0).mat("membrane").glow(glow)
                    .box(0, 0, -0.5f, span / 2, span / 2 + 2, 1, -0.1f).add();
            for (int k = 0; k < fingers; k++) {
                float a = 35 + k * (55f / Math.max(1, fingers - 1));
                int len = span / 2 + 4 - k;
                s("wing_finger" + k, "wing_fore", true, bone).at(span / 2f - 0.5f, 0, 0).rot(0, 0, a).mat("hide")
                        .box(-0.5f, -0.5f, -0.5f, len, 1, 1, -0.1f).add();
                s("wing_web" + k, "wing_finger" + k, true, darker(membrane, k % 2 == 0 ? 1f : 0.88f)).accent(claw).at(0, 0, 0).rot(0, 0, 12).mat("membrane").glow(glow)
                        .box(0, 0, -0.5f, len - 1, 4, 1, -0.15f).add();
            }
            return this;
        }

        /** A segmented tail from the hips ending in a spade or spike. */
        Fig tail(int col, int tipCol, int segs, String tip) {
            String prev = "hips";
            for (int i = 0; i < segs; i++) {
                int w = Math.max(1, 3 - i * 2 / Math.max(1, segs - 1));
                part(b, "tail" + i, prev, Role.TAIL, i % 2 == 0 ? col : darker(col, 0.9f)).at(0, i == 0 ? 0 : 0, i == 0 ? depth / 2f : 3.6f)
                        .rot(i == 0 ? 40 : -10, 0, 0).mat(skinMat).box(-w / 2f, -w / 2f, 0, w, w, 4).add();
                prev = "tail" + i;
            }
            if (tip.equals("spade")) {
                p("tail_spade", prev, tipCol).at(0, 0, 3.5f).mat("horn").box(-1.5f, -0.5f, 0, 3, 1, 2).box(-0.5f, -0.5f, 2, 1, 1, 1).add();
            } else if (tip.equals("spike")) {
                p("tail_spike", prev, tipCol).at(0, 0, 3.5f).rot(-20, 0, 0).mat("horn").box(-0.5f, -0.5f, 0, 1, 1, 3).add();
            }
            return this;
        }

        // ------------------------------------------------------------------------------------------
        // weapons (attached to a hand; business end along local -Y)
        // ------------------------------------------------------------------------------------------

        /**
         * Grip part on a hand; returns its name. {@code pitch} is the weapon's world pitch (0 = straight up, 90 =
         * pointing forward, 180 = pointing down, negative = leaning back over the shoulder): the arm's own pose is
         * compensated so weapons read the same whatever the arm is doing.
         */
        String grip(String hand, float pitch, float yaw, float roll) {
            String g = "grip_" + hand;
            float arm = hand.endsWith("_l") ? armPitchL : armPitchR;
            float rel = pitch - arm + foreBend * 0.6f;
            p(g, hand, darker(WOOD, 0.8f)).at(0, 1.5f, -0.5f).rot(rel, yaw, roll).mat("leather").box(-0.5f, -1, -0.5f, 1, 2, 1, 0.3f).add();
            return g;
        }

        Fig staff(String hand, float pitch, int shaft, int head, int orb, String orbGlow) {
            String g = grip(hand, pitch, 0, 0);
            p("staff", g, shaft).at(0, 0, 0).mat("wood").box(-0.5f, -16, -0.5f, 1, 22, 1, 0.1f).add();
            p("staff_band", "staff", head).at(0, -15, 0).mat("metal").box(-1, -1, -1, 2, 2, 2).add();
            p("staff_prong", "staff_band", head).at(0, -1, 0).mat("metal").box(-1.5f, -3, -0.5f, 1, 3, 1).box(0.5f, -3, -0.5f, 1, 3, 1)
                    .box(-0.5f, -3, -1.5f, 1, 3, 1).box(-0.5f, -3, 0.5f, 1, 3, 1).add();
            p("staff_orb", "staff_band", orb).accent(orb).at(0, -2.5f, 0).mat("energy").glow(orbGlow).box(-1, -1, -1, 2, 2, 2, 0.2f).add();
            p("staff_foot", "staff", head).at(0, 6, 0).mat("metal").box(-0.5f, -1, -0.5f, 1, 1, 1, 0.2f).add();
            return this;
        }

        Fig greataxe(String hand, float pitch, int haft, int blade) {
            String g = grip(hand, pitch, 0, 0);
            p("axe_haft", g, haft).at(0, 0, 0).mat("wood").box(-0.5f, -15, -0.5f, 1, 20, 1, 0.15f).add();
            p("axe_socket", "axe_haft", darker(blade, 0.7f)).at(0, -13, 0).mat("metal").box(-1, -2, -1, 2, 4, 2).add();
            for (int sd = -1; sd <= 1; sd += 2) {
                String nm = sd < 0 ? "axe_blade_r" : "axe_blade_l";
                p(nm, "axe_socket", blade).at(sd * 1, 0, 0).mat("metal").box(sd < 0 ? -4 : 0, -3, -0.5f, 4, 6, 1).add();
                p(nm + "_edge", nm, lighter(blade, 0.25f)).at(sd * 4, 0, 0).mat("metal").box(sd < 0 ? -1 : 0, -4.5f, -0.5f, 1, 9, 1, -0.1f).add();
                p(nm + "_horn", nm, blade).at(sd * 3.5f, -3, 0).rot(0, 0, sd * -30).mat("metal").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
            }
            p("axe_spike", "axe_socket", blade).at(0, -2, 0).mat("metal").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
            p("axe_pommel", "axe_haft", darker(blade, 0.7f)).at(0, 5, 0).mat("metal").box(-1, 0, -1, 2, 1, 2).add();
            return this;
        }

        Fig warspear(String hand, float pitch, int shaft, int head) {
            String g = grip(hand, pitch, 0, 0);
            p("spear_shaft", g, shaft).at(0, 0, 0).mat("wood").box(-0.5f, -18, -0.5f, 1, 26, 1, 0.1f).add();
            p("spear_collar", "spear_shaft", darker(head, 0.75f)).at(0, -18, 0).mat("metal").box(-1, -1, -1, 2, 2, 2).add();
            p("spearhead", "spear_collar", head).at(0, -1, 0).mat("metal").box(-1.5f, -4, -0.5f, 3, 4, 1).box(-1, -6, -0.5f, 2, 2, 1).box(-0.5f, -7, -0.5f, 1, 1, 1).add();
            s("spear_hook", "spear_collar", false, head).at(1, -0.5f, 0).rot(0, 0, -40).mat("metal").box(0, -0.5f, -0.5f, 3, 1, 1).add();
            p("spear_tassel", "spear_collar", 0x8A1A1A).at(0, 1, 0).rot(10, 0, 0).mat("cloth").box(-0.5f, 0, -0.5f, 1, 3, 1, 0.2f).add();
            p("spear_butt", "spear_shaft", darker(head, 0.75f)).at(0, 8, 0).mat("metal").box(-0.5f, -1, -0.5f, 1, 2, 1, 0.2f).add();
            return this;
        }

        Fig spear(String hand, float pitch, int shaft, int head, int len) {
            String g = grip(hand, pitch, 0, 0);
            p("spear_shaft", g, shaft).at(0, 0, 0).mat("wood").box(-0.5f, -len, -0.5f, 1, len + 6, 1, 0.1f).add();
            p("spearhead", "spear_shaft", head).at(0, -len, 0).mat("metal").box(-1, -3, -0.5f, 2, 3, 1).box(-0.5f, -5, -0.5f, 1, 2, 1).add();
            p("spear_collar", "spear_shaft", darker(head, 0.7f)).at(0, -len, 0).mat("metal").box(-1, 0, -1, 2, 1, 2).add();
            return this;
        }

        Fig crossbow(String hand, float pitch, int stock, int metal) {
            String g = grip(hand, pitch, 0, 0);
            p("xbow_stock", g, stock).at(0, -1, 0).mat("wood").box(-1, -9, -1, 2, 12, 2).add();
            p("xbow_limbs", "xbow_stock", metal).at(0, -8, 0).mat("metal").box(-6, -1, -1, 12, 1, 2).add();
            s("xbow_limb_tip", "xbow_limbs", false, metal).at(6, 0, 0).rot(0, 0, -25).mat("metal").box(-0.5f, -0.5f, -0.5f, 2, 1, 1).add();
            p("xbow_string", "xbow_limbs", 0xD8D0B8).at(0, 1, 0).mat("cloth").box(-5, 0, -0.5f, 10, 1, 1, -0.4f).add();
            p("xbow_bolt", "xbow_stock", metal).at(0, -10, -1.2f).mat("metal").box(-0.5f, -2, -0.5f, 1, 7, 1, -0.2f).add();
            p("xbow_mag", "xbow_stock", darker(stock, 0.8f)).at(0, -6, 1).mat("wood").box(-1, 0, 0, 2, 3, 2).add();
            return this;
        }

        Fig warhammer(String hand, float pitch, int haft, int headCol, String nm) {
            String g = grip(hand, pitch, 0, 0);
            p(nm + "_haft", g, haft).at(0, 0, 0).mat("wood").box(-0.5f, -9, -0.5f, 1, 12, 1, 0.1f).add();
            p(nm + "_head", nm + "_haft", headCol).at(0, -9, 0).mat("metal").box(-3, -3, -1.5f, 6, 3, 3).add();
            p(nm + "_face", nm + "_head", lighter(headCol, 0.15f)).at(0, -1.5f, 0).mat("metal").box(-3.5f, -1, -1, 1, 2, 2).box(2.5f, -1, -1, 1, 2, 2).add();
            p(nm + "_cap", nm + "_head", darker(headCol, 0.7f)).at(0, -3, 0).mat("metal").box(-1, -1, -1, 2, 1, 2).add();
            return this;
        }

        Fig flail(String hand, float pitch, int haft, int ball) {
            String g = grip(hand, pitch, 0, 0);
            p("flail_haft", g, haft).at(0, 0, 0).mat("wood").box(-0.5f, -7, -0.5f, 1, 10, 1, 0.1f).add();
            p("flail_cap", "flail_haft", darker(ball, 0.75f)).at(0, -7, 0).mat("metal").box(-1, -1, -1, 2, 1, 2).add();
            String prev = "flail_cap";
            for (int k = 0; k < 3; k++) {
                p("flail_link" + k, prev, darker(ball, 0.85f)).at(0, k == 0 ? -1 : -1.5f, 0).rot(k == 0 ? -35 : -15, 0, 0).mat("chain")
                        .box(-0.5f, -1.5f, -0.5f, 1, 2, 1, -0.1f).add();
                prev = "flail_link" + k;
            }
            p("flail_ball", prev, ball).at(0, -2, 0).mat("metal").box(-1.5f, -3, -1.5f, 3, 3, 3).add();
            p("flail_spikes", "flail_ball", lighter(ball, 0.2f)).at(0, -1.5f, 0).mat("metal").box(-2.5f, -0.5f, -0.5f, 5, 1, 1).box(-0.5f, -0.5f, -2.5f, 1, 1, 5)
                    .box(-0.5f, -2.5f, -0.5f, 1, 1, 1).add();
            return this;
        }

        /** A long sword: grip, crossguard, ricasso and a two-step tapered blade with a fuller. */
        Fig sword(String hand, float pitch, int blade, int guard, int len, int width) {
            String g = grip(hand, pitch, 0, 0);
            p("sword_hilt", g, DARK_LEATHER).at(0, 0, 0).mat("leather").box(-0.5f, -1, -0.5f, 1, 4, 1, 0.1f).add();
            p("sword_pommel", "sword_hilt", guard).at(0, 3, 0).mat("metal").box(-1, 0, -1, 2, 1, 2).add();
            p("sword_guard", "sword_hilt", guard).at(0, -1, 0).mat("metal").box(-2.5f, -1, -0.5f, 5, 1, 1, 0.1f).add();
            p("sword_blade", "sword_guard", blade).at(0, -1, 0).mat("metal").box(-width / 2f, -len, -0.5f, width, len, 1, -0.05f).add();
            p("sword_fuller", "sword_blade", darker(blade, 0.75f)).at(0, -1, -0.45f).mat("metal").box(-0.5f, -len + 3, -0.2f, 1, len - 3, 1, -0.3f).add();
            p("sword_tip", "sword_blade", lighter(blade, 0.1f)).at(0, -len, 0).mat("metal").box(-Math.max(1, width - 1) / 2f, -2, -0.5f, Math.max(1, width - 1), 2, 1, -0.1f)
                    .box(-0.5f, -3, -0.5f, 1, 1, 1, -0.15f).add();
            return this;
        }

        /** A big flanged/spiked mace. */
        Fig mace(String hand, float pitch, int haft, int head, int size) {
            String g = grip(hand, pitch, 0, 0);
            p("mace_haft", g, haft).at(0, 0, 0).mat("wood").box(-0.5f, -10, -0.5f, 1, 14, 1, 0.2f).add();
            p("mace_head", "mace_haft", head).at(0, -10, 0).mat("metal").box(-size / 2f, -size, -size / 2f, size, size, size).add();
            p("mace_flange", "mace_head", lighter(head, 0.15f)).at(0, -size / 2f, 0).mat("metal")
                    .box(-size / 2f - 1, -size / 2f + 0.5f, -0.5f, size + 2, size - 1, 1).box(-0.5f, -size / 2f + 0.5f, -size / 2f - 1, 1, size - 1, size + 2).add();
            p("mace_spike", "mace_head", lighter(head, 0.25f)).at(0, -size, 0).mat("horn").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
            p("mace_collar", "mace_haft", darker(head, 0.7f)).at(0, -10, 0).mat("metal").box(-1, 0, -1, 2, 2, 2).add();
            return this;
        }

        Fig shield(String hand, String style, int col, int trim, int emblem) {
            String sh = "shield";
            switch (style) {
                case "kite" -> {
                    p(sh, hand, col).accent(trim).at(1.8f, 1, 0).rot(0, 90, 0).mat("metal").box(-3.5f, -5, -0.5f, 7, 8, 1).add();
                    p("shield_point", sh, col).at(0, 3, 0).mat("metal").box(-2.5f, 0, -0.5f, 5, 2, 1).box(-1.5f, 2, -0.5f, 3, 1, 1).box(-0.5f, 3, -0.5f, 1, 1, 1).add();
                }
                case "round" -> {
                    p(sh, hand, col).accent(trim).at(1.8f, 1, 0).rot(0, 90, 0).mat("wood").box(-4, -4, -0.5f, 8, 8, 1).add();
                    p("shield_edge_a", sh, col).at(0, 0, 0).mat("wood").box(-3, -5, -0.5f, 6, 1, 1).box(-3, 4, -0.5f, 6, 1, 1).box(-5, -3, -0.5f, 1, 6, 1).box(4, -3, -0.5f, 1, 6, 1).add();
                }
                case "tower" -> p(sh, hand, col).accent(trim).at(1.8f, 1, 0).rot(0, 90, 0).mat("metal").box(-4, -7, -0.5f, 8, 13, 1).add();
                default -> p(sh, hand, col).accent(trim).at(1.8f, 1, 0).rot(0, 90, 0).mat("metal").box(-3.5f, -4, -0.5f, 7, 7, 1).add();
            }
            if (trim >= 0) {
                p("shield_rim", sh, trim).at(0, 0, -0.6f).mat("metal").box(-0.5f, -4, -0.3f, 1, 7, 1).box(-3, -1, -0.3f, 6, 1, 1).add();
            }
            if (emblem >= 0) {
                p("shield_boss", sh, emblem).at(0, -0.5f, -1).mat("metal").box(-1, -1, -0.5f, 2, 2, 1, 0.1f).add();
            }
            return this;
        }

        /** Gnarled wooden club. */
        Fig club(String hand, float pitch, int col) {
            String g = grip(hand, pitch, 0, 0);
            p("club", g, col).at(0, 0, 0).mat("wood").box(-1, -10, -1, 2, 13, 2).add();
            p("club_head", "club", darker(col, 0.9f)).at(0, -10, 0).rot(0, 20, 6).mat("wood").box(-2, -5, -2, 4, 6, 4).add();
            p("club_knot", "club_head", darker(col, 0.75f)).at(1.5f, -2, 0).rot(0, 0, -30).mat("wood").box(0, -1, -1, 2, 2, 2).add();
            p("club_spike", "club_head", 0xC8C0A8).at(-2, -3, 0).rot(0, 0, 50).mat("horn").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
            return this;
        }

        /** Halberd: long shaft with an axe blade, a back spike and a spear point. */
        Fig halberd(String hand, float pitch, int shaft, int head, int len) {
            String g = grip(hand, pitch, 0, 0);
            p("hal_shaft", g, shaft).at(0, 0, 0).mat("wood").box(-0.5f, -len, -0.5f, 1, len + 6, 1, 0.1f).add();
            p("hal_socket", "hal_shaft", darker(head, 0.75f)).at(0, -len + 2, 0).mat("metal").box(-1, -2, -1, 2, 4, 2).add();
            p("hal_blade", "hal_socket", head).at(1, 0, 0).mat("metal").box(0, -3, -0.5f, 4, 5, 1).box(3, -4, -0.5f, 1, 7, 1).add();
            p("hal_spike", "hal_socket", head).at(-1, -1, 0).rot(0, 0, 20).mat("metal").box(-3, -0.5f, -0.5f, 3, 1, 1).add();
            p("hal_point", "hal_socket", lighter(head, 0.15f)).at(0, -2, 0).mat("metal").box(-0.5f, -5, -0.5f, 1, 5, 1).box(-1, -3, -0.5f, 2, 2, 1).add();
            return this;
        }

        /** A short recurve bow held vertically. */
        Fig bow(String hand, int col, int string) {
            String g = grip(hand, 0, 0, 0);
            p("bow_grip", g, col).at(0, 0, -0.5f).mat("wood").box(-0.5f, -2, -0.5f, 1, 4, 1, 0.1f).add();
            s("bow_limb", "bow_grip", false, col).at(0, -2, 0).rot(-20, 0, 0).mat("wood").box(-0.5f, -6, -0.5f, 1, 6, 1).add();
            // lower limb (mirror in Y by hand)
            p("bow_limb_low", "bow_grip", col).at(0, 2, 0).rot(20, 0, 0).mat("wood").box(-0.5f, 0, -0.5f, 1, 6, 1).add();
            p("bow_string", "bow_grip", string).at(0, 0, 2.5f).mat("cloth").box(-0.5f, -7, -0.5f, 1, 14, 1, -0.4f).add();
            return this;
        }

        /** A scythe: long snath with a curved blade. */
        Fig scythe(String hand, float pitch, int shaft, int blade) {
            String g = grip(hand, pitch, 0, 0);
            p("scy_shaft", g, shaft).at(0, 0, 0).mat("wood").box(-0.5f, -16, -0.5f, 1, 22, 1, 0.1f).add();
            p("scy_blade", "scy_shaft", blade).at(0, -16, 0).rot(0, 0, -10).mat("metal").box(0, -1, -0.5f, 7, 2, 1).add();
            p("scy_blade_tip", "scy_blade", lighter(blade, 0.15f)).at(7, 0, 0).rot(0, 0, 35).mat("metal").box(0, -0.5f, -0.5f, 4, 1, 1).add();
            return this;
        }

        /** A book held open (for archaeologists, mages). */
        Fig book(String hand, int cover, int pages) {
            p("book", hand, cover).at(0, 2, -1.5f).rot(60, 0, 0).mat("leather").box(-2.5f, -0.5f, -2, 5, 1, 4).add();
            p("book_pages", "book", pages).at(0, -0.5f, 0).mat("cloth").box(-2, -0.5f, -1.5f, 4, 1, 3).add();
            return this;
        }
    }
}
