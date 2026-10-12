package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.BestiaryHumKit.darker;
import static com.runecraft.osrs.content.bestiary.BestiaryHumKit.fig;
import static com.runecraft.osrs.content.bestiary.BestiaryWldBeasts.legPair;
import static com.runecraft.osrs.content.bestiary.BestiaryWldFig.dk;
import static com.runecraft.osrs.content.bestiary.BestiaryWldFig.side;
import static com.runecraft.osrs.content.bestiary.Humanoid.*;
import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/**
 * Slayer task creatures added by the slayer module's extra set: sourhogs, crocodiles, shadow warriors,
 * otherworldly beings, ents, mammoths, ice spiders and the Wilderness bandits, sculpted after their OSRS models.
 */
final class BestiaryMcd {
    private BestiaryMcd() {
    }

    static List<ModelSpec> all() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(BestiaryQbmBeasts.boar("sourhog"));
        l.add(BestiaryWldBeasts.spider("ice_spider", 0xB8D8EA, 0x4A8AC0, 0x60E0FF, 1.0f));
        l.add(crocodile("crocodile"));
        l.add(mammoth("mammoth"));
        l.add(ent("ent"));
        l.add(shadowWarrior("shadow_warrior"));
        l.add(otherworldlyBeing("otherworldly_being"));
        l.add(bandit("bandit", 0x6A5434, 0x8A2A1A, IRON, false));
        l.add(bandit("wilderness_bandit", 0x3A3026, 0x2A2A2E, 0x50C8D0, true));
        return l;
    }

    /**
     * OSRS crocodile: a long, low, armoured body on sprawling legs, a ridge of back scutes, a long flat snout with a
     * gaping toothy jaw and raised eye bumps, and a thick tapering tail.
     */
    static ModelSpec crocodile(String id) {
        int hide = 0x4A5A2A;
        int dark = 0x2E3A1A;
        int belly = 0xB8B080;
        int tooth = 0xE8E4D0;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.9f);
        part(b, "body", null, Role.BODY, hide).accent(dark).at(0, 19, 0).mat("scales").box(-4, -2.5f, -8, 8, 5, 16).add();
        part(b, "belly", "body", Role.STATIC, belly).at(0, 2.5f, 0).mat("scales").box(-3.5f, -0.5f, -7, 7, 1, 14).add();
        for (int i = 0; i < 4; i++) {
            part(b, "scute" + i, "body", Role.STATIC, dark).at(0, -2.5f, -6 + i * 4).mat("scales")
                    .box(-2.5f, -1, -1.5f, 2, 1, 3).box(0.5f, -1, -1.5f, 2, 1, 3).add();
        }
        sym(b, "flank", "body", false, dk(hide, 0.9f)).accent(dark).at(4, 0, 0).mat("scales").box(-0.5f, -2, -6, 1, 4, 12).add();
        // head
        part(b, "neck", "body", Role.NECK, hide).accent(dark).at(0, -0.5f, -8).mat("scales").box(-3.5f, -2, -3, 7, 4, 3).add();
        part(b, "head", "neck", Role.HEAD, hide).accent(dark).at(0, -0.5f, -3).mat("scales").pattern("eyes_animal")
                .box(-3, -1.5f, -4, 6, 3, 4).add();
        sym(b, "eye_bump", "head", false, dark).at(1.8f, -1.5f, -2).mat("scales").box(-1, -1, -1, 2, 1, 2).add();
        sym(b, "eye", "eye_bump", true, 0xE0C030).at(0, -0.5f, -1).mat("energy").glow("full").box(-0.5f, -0.5f, -0.3f, 1, 1, 1, -0.2f).add();
        part(b, "snout", "head", Role.STATIC, hide).accent(dark).at(0, -0.5f, -4).mat("scales").box(-2, -1, -8, 4, 2, 8).add();
        part(b, "snout_tip", "snout", Role.STATIC, dk(hide, 0.85f)).at(0, -0.2f, -8).mat("scales").box(-2.5f, -1, -1.5f, 5, 2, 2).add();
        sym(b, "nostril", "snout_tip", false, 0x1A1A10).at(1, -1, -1).mat("flesh").box(-0.5f, -0.3f, -0.5f, 1, 1, 1, -0.2f).add();
        part(b, "teeth_up", "snout", Role.STATIC, tooth).at(0, 1, -4).mat("horn").box(-2, 0, -4, 4, 1, 8, -0.3f).add();
        part(b, "jaw", "head", Role.JAW, dk(hide, 0.95f)).accent(belly).at(0, 1.5f, -1).mat("scales").box(-2.5f, 0, -11, 5, 1, 11).add();
        part(b, "teeth_low", "jaw", Role.STATIC, tooth).at(0, -0.6f, -6).mat("horn").box(-2, 0, -5, 4, 1, 9, -0.3f).add();
        part(b, "mouth", "jaw", Role.STATIC, 0xC07070).at(0, -0.2f, -5).mat("flesh").box(-1.5f, 0, -4, 3, 1, 7, -0.35f).add();
        // tail
        part(b, "tail", "body", Role.TAIL, hide).accent(dark).at(0, -0.5f, 8).mat("scales").box(-3, -2, 0, 6, 4, 6).add();
        part(b, "tail_mid", "tail", Role.STATIC, hide).accent(dark).at(0, 0.3f, 6).rot(-4, 0, 0).mat("scales")
                .box(-2.2f, -1.5f, 0, 4, 3, 6).box(-0.5f, -2.5f, 1, 1, 1, 4).add();
        part(b, "tail_end", "tail_mid", Role.STATIC, hide).accent(dark).at(0, 0.3f, 6).rot(-4, 0, 0).mat("scales")
                .box(-1.5f, -1, 0, 3, 2, 6).box(-0.5f, -2, 1, 1, 1, 4).add();
        part(b, "tail_tip", "tail_end", Role.STATIC, dark).at(0, 0.2f, 6).mat("scales").box(-0.5f, -0.5f, 0, 1, 1, 4).add();
        // four sprawling legs: upper limb out sideways, forearm down, splayed clawed foot
        for (boolean left : new boolean[]{true, false}) {
            for (boolean front : new boolean[]{true, false}) {
                String n = front ? "leg_f" : "leg_b";
                Role r = front ? (left ? Role.LEG_FRONT_LEFT : Role.LEG_FRONT_RIGHT) : (left ? Role.LEG_BACK_LEFT : Role.LEG_BACK_RIGHT);
                side(part(b, n, "body", r, hide).accent(dark).at(3.5f, 1, front ? -5 : 5).rot(0, 0, 12).mat("scales")
                        .box(0, -1, -1.2f, 3, 2, 3), left);
                side(part(b, n + "_low", n + "*", Role.STATIC, dk(hide, 0.92f)).at(3, 0, 0).rot(0, 0, -12).mat("scales")
                        .box(-1, -0.5f, -1, 2, 4, 2), left);
                side(part(b, n + "_foot", n + "_low*", Role.STATIC, dark).at(0, 3.5f, 0).mat("scales")
                        .box(-1.5f, -0.5f, -2.5f, 3, 1, 3).box(-1.5f, -0.4f, -3.5f, 1, 1, 1, -0.15f).box(0.5f, -0.4f, -3.5f, 1, 1, 1, -0.15f),
                        left);
            }
        }
        return b.build();
    }

    /**
     * OSRS mammoth: a towering woolly beast with a high domed skull and shoulder hump, a long hanging trunk, huge
     * upward-curving ivory tusks, small flapping ears, a skirt of shaggy hair and pillar legs.
     */
    static ModelSpec mammoth(String id) {
        int fur = 0x6A4428;
        int shag = 0x4A2E1A;
        int skin = 0x3A2A22;
        int ivory = 0xEDE4CC;
        ModelSpec.Builder b = ModelSpec.builder(id).scale(1.5f).shadow(1.3f);
        part(b, "body", null, Role.BODY, fur).accent(shag).at(0, 10, 1).mat("fur").box(-7, -7, -10, 14, 12, 20).add();
        part(b, "hump", "body", Role.STATIC, fur).accent(shag).at(0, -7, -6).rot(-12, 0, 0).mat("fur").box(-5.5f, -3, -4, 11, 4, 9).add();
        part(b, "rump", "body", Role.STATIC, dk(fur, 0.95f)).at(0, -6, 7).rot(14, 0, 0).mat("fur").box(-6, -1, -3, 12, 2, 6).add();
        for (boolean left : new boolean[]{true, false}) {
            side(part(b, "skirt", "body", Role.STATIC, shag).at(7, 2, 0).rot(0, 0, -5).mat("hair").box(-1, 0, -9, 2, 6, 19), left);
            side(part(b, "skirt_fringe", "skirt*", Role.STATIC, dk(shag, 0.85f)).at(0, 6, 0).mat("hair")
                    .box(-1, 0, -8, 2, 2, 5).box(-1, 0, -2, 2, 3, 5).box(-1, 0, 4, 2, 2, 5), left);
        }
        part(b, "chest_shag", "body", Role.STATIC, shag).at(0, 1, -10).mat("hair").box(-6, 0, -1.5f, 12, 7, 2).add();
        part(b, "tail", "body", Role.TAIL, shag).at(0, -4, 10.5f).rot(12, 0, 0).mat("hair").box(-1, 0, -0.5f, 2, 7, 2).add();
        part(b, "tail_tuft", "tail", Role.STATIC, dk(shag, 0.8f)).at(0, 7, 0.5f).mat("hair").box(-1.5f, 0, -1, 3, 3, 3).add();
        // head with a high dome, trunk and tusks
        part(b, "neck", "body", Role.NECK, fur).accent(shag).at(0, -2, -10).rot(10, 0, 0).mat("fur").box(-5, -5, -3, 10, 9, 4).add();
        part(b, "head", "neck", Role.HEAD, fur).accent(shag).at(0, 0, -3).rot(8, 0, 0).mat("fur").pattern("eyes_animal")
                .box(-4.5f, -5, -6, 9, 9, 6).add();
        part(b, "dome", "head", Role.STATIC, dk(fur, 1.05f)).at(0, -5, -2).rot(-15, 0, 0).mat("hair").box(-3.5f, -3, -3, 7, 3, 6).add();
        part(b, "forelock", "dome", Role.STATIC, shag).at(0, -2, -3).rot(25, 0, 0).mat("hair").box(-2.5f, -1, -1.5f, 5, 3, 2).add();
        sym(b, "ear", "head", false, skin).accent(shag).at(4.5f, -2, -1).rot(0, -15, 15).mat("hide").box(0, -2, -1.5f, 2, 4, 3).add();
        int[] seg = {4, 4, 3, 3};
        float[] bend = {-8, -10, -12, 22};
        String parent = "head";
        for (int i = 0; i < seg.length; i++) {
            String n = i == 0 ? "trunk" : "trunk" + i;
            int w = 4 - i / 2;
            part(b, n, parent, i == 0 ? Role.TENTACLE : Role.STATIC, i == seg.length - 1 ? skin : dk(fur, 0.9f - i * 0.05f))
                    .at(0, i == 0 ? 2 : seg[i - 1], i == 0 ? -6 : 0).rot(bend[i], 0, 0).mat("hide")
                    .box(-w / 2f, 0, -w / 2f, w, seg[i], w).add();
            parent = n;
        }
        for (boolean left : new boolean[]{true, false}) {
            side(part(b, "tusk", "head", Role.STATIC, ivory).at(2.5f, 2.5f, -5).rot(-35, 10, -10).mat("horn").box(-1, 0, -1, 2, 7, 2), left);
            side(part(b, "tusk_mid", "tusk*", Role.STATIC, dk(ivory, 0.97f)).at(0, 7, 0).rot(-55, 0, 8).mat("horn")
                    .box(-1, 0, -1, 2, 6, 2, -0.15f), left);
            side(part(b, "tusk_tip", "tusk_mid*", Role.STATIC, dk(ivory, 0.92f)).at(0, 6, 0).rot(-50, 0, 8).mat("horn")
                    .box(-0.5f, 0, -0.5f, 1, 4, 1), left);
        }
        legPair(b, "leg_f", true, 4.5f, -6.5f, fur, shag, skin, "fur", 5, 5, 5, 0, 0, "plain", -1);
        legPair(b, "leg_b", false, 4.5f, 6.5f, fur, shag, skin, "fur", 5, 5, 5, -4, 4, "plain", -1);
        return b.build();
    }

    /**
     * OSRS ent: a walking tree - a thick, knotted trunk on root feet, long branch arms ending in twig fingers, a
     * bark face with deep-set glowing eyes, and a heavy crown of leaves (a little greener and broader than Entrana's
     * tree spirit).
     */
    static ModelSpec ent(String id) {
        int bark = 0x6A5034;
        int leaf = 0x3E7A2C;
        BestiaryHumKit.Fig f = fig(id).legs(11, 6).torso(11, 10, 12, 9, 7, 8).arms(15, 4, 5).head(8, 8, 8).pose(8, 6, 16)
                .armPitch(-25, -25).skin(bark, "wood").scale(1.55f).shadow(1.1f);
        f.foreBend = 22;
        f.core(darker(bark, 0.85f), "wood", bark, "wood", bark, "wood").head("monster", 0xE0C040)
                .armsBuild(bark, "wood", bark, "wood", darker(bark, 0.85f), "wood").claws(0x3A2A1A, 3)
                .legsBuild(bark, "wood", bark, "wood", darker(bark, 0.8f), "wood");
        for (int k = 0; k < 8; k++) {
            f.p("canopy" + k, "head", k % 3 == 0 ? darker(leaf, 0.8f) : leaf).at(-5 + (k % 4) * 3.3f, -8 - (k / 4) * 3, (k % 2) * 2 - 1)
                    .rot(0, k * 25, (k % 4 - 1.5f) * 12).mat("moss").box(-2, -3, -2, 4, 3, 4).add();
        }
        for (int k = 0; k < 3; k++) {
            f.s("root" + k, "foot", true, darker(bark, 0.75f)).at(-1.5f + k * 1.5f, 1, -3).rot(-8, (k - 1) * -35, 0).mat("wood")
                    .box(-0.5f, -0.5f, -3, 1, 1, 3).add();
        }
        f.s("knot", "chest", false, darker(bark, 0.7f)).at(3, -4, -4.2f).mat("wood").box(-1, -1, -0.5f, 2, 2, 1).add();
        f.s("branch", "chest", false, darker(bark, 0.9f)).at(4.5f, -8, 1).rot(-15, 0, 45).mat("wood").box(-0.5f, -6, -0.5f, 1, 6, 1).add();
        f.s("branch_leaf", "branch", true, leaf).at(0, -6, 0).mat("moss").box(-1.5f, -2, -1.5f, 3, 2, 3).add();
        f.p("moss_beard", "head", darker(leaf, 0.7f)).at(0, 1, -3.8f).mat("moss").box(-2.5f, 0, -0.5f, 5, 4, 1).add();
        f.p("bark_mouth", "head", 0x1A120A).at(0, -1.5f, -4.1f).mat("wood").glow("maw").box(-2, -1, -0.3f, 4, 1, 1).add();
        return f.build();
    }

    /** OSRS shadow warrior: a translucent black knight in dark plate with a horned full helm, glowing violet eyes and a black blade. */
    static ModelSpec shadowWarrior(String id) {
        int plate = 0x1E1E24;
        ModelSpec.Builder b = BestiaryWldFig.of(id).torso(8, 8, 10, 6, 10).skin(0x2A2A34).hair(HAIR_BLACK, "short")
                .top(plate, "plate").trim(0x3A2A5A).bottoms(dk(plate, 0.9f), "plate").boots(0x141418, "sabaton")
                .helm("horned", plate).cape(0x14101E).weapon("longsword", 0x2A2A30)
                .weaponGlow(0x8A4AFF).pose(-6, 8, 24).builder();
        part(b, "visor_glow", "head", Role.STATIC, 0xA060FF).at(0, -4.5f, -4.3f).mat("energy").glow("full")
                .box(-2.5f, 0, -0.4f, 2, 1, 1, -0.15f).box(0.5f, 0, -0.4f, 2, 1, 1, -0.15f).add();
        for (int k = 0; k < 3; k++) {
            part(b, "wisp" + k, "torso", Role.FLOAT, 0x3A2A5A).at(-3 + k * 3, 4, 2.5f).rot(0, 0, (k - 1) * 12).mat("energy")
                    .glow("soft").box(-1, 0, -0.5f, 2, 4 + k % 2 * 2, 1, -0.1f).add();
        }
        return b.build();
    }

    /**
     * OSRS otherworldly being: a gaunt, pale, almost see-through spirit of Zanaris in a ragged loincloth, bald with
     * large dark eyes and long reaching arms.
     */
    static ModelSpec otherworldlyBeing(String id) {
        int skin = 0xB4CCBC;
        ModelSpec.Builder b = BestiaryWldFig.of(id).torso(6, 5, 8, 5, 11).arms(3, 6, 6, 4).legs(13, 3).head(7, 9, 7)
                .skin(skin).hair(skin, "none").ears("pointy").brow().top(skin, "bare").bottoms(0x8AA890, "loincloth")
                .boots(skin, "bare").pose(-14, 10, 30).builder();
        part(b, "ribs", "torso", Role.STATIC, dk(skin, 0.8f)).at(0, -8, -2.8f).mat("skin").box(-3, 0, -0.5f, 6, 1, 1, -0.1f)
                .box(-3, 2, -0.5f, 6, 1, 1, -0.1f).box(-2.5f, 4, -0.5f, 5, 1, 1, -0.1f).add();
        part(b, "shimmer", "head", Role.FLOAT, 0x9AF0C8).at(0, -10, 0).mat("energy").glow("full")
                .box(-2, 0, -2, 4, 1, 4, -0.3f).add();
        return b.build();
    }

    /** Wilderness bandits: rough leather, a bandana or hood, a sash and a scimitar; the level 130s wear dark gear and a mask. */
    static ModelSpec bandit(String id, int leather, int cloth, int blade, boolean veteran) {
        BestiaryWldFig f = BestiaryWldFig.of(id).skin(0xC09068).hair(HAIR_BROWN, "short").beard(HAIR_BROWN, veteran ? "goatee" : "short")
                .top(leather, "leather").sleeves("long", dk(leather, 0.85f)).bottoms(dk(leather, 0.8f), "trousers")
                .belt(0x2A1E14).pouch().boots(0x2A1E14, "boot").weapon("scimitar", blade).pose(-8, 8, 28);
        if (veteran) {
            f.helm("bandana", cloth).cape(dk(cloth, 0.9f));
        } else {
            f.helm("bandana", cloth);
        }
        ModelSpec.Builder b = f.builder();
        part(b, "sash", "torso", Role.STATIC, cloth).at(-3, -2, -2.6f).rot(0, 0, 18).mat("cloth").box(-1, -1, -0.8f, 2, 7, 1).add();
        if (veteran) {
            part(b, "mask", "head", Role.STATIC, 0x141416).at(0, -2.2f, -4).mat("cloth").box(-4.3f, -1, -0.6f, 9, 3, 1).add();
            if (blade != IRON) {
                part(b, "blade_glint", "torso", Role.STATIC, blade).at(0, -8, -3.3f).mat("energy").glow("full")
                        .box(-0.5f, 0, -0.5f, 1, 1, 1, -0.2f).add();
            }
        }
        return b.build();
    }
}
