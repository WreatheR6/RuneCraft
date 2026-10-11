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
import static com.runecraft.osrs.content.bestiary.BestiaryWldFig.side;
import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/**
 * The stranger quest creatures and the small fry: the Dagannoth and Gelatinnoth Mothers, the Inadequacy, Agrith-Na-Na,
 * Tanglefoot, the Sea Troll Queen, the Slagilith and the Evil Creature, plus the shades of Mort'ton, the Nightmare's
 * husks and the imp.
 */
final class BestiaryQbmOdd {
    private BestiaryQbmOdd() {
    }

    static List<ModelSpec> overrides() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(dagannothMother("dagannoth_mother", 2.2f, false));
        l.add(dagannothMother("gelatinnoth_mother", 2.0f, true));
        l.add(inadequacy("the_inadequacy"));
        l.add(agrithNaNa("agrith_na_na"));
        l.add(tanglefoot("tanglefoot"));
        l.add(seaTrollQueen("sea_troll_queen"));
        l.add(evilCreature("evil_creature"));
        l.add(shade("shade"));
        return l;
    }

    static List<ModelSpec> fresh() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(slagilith("quest_slagilith"));
        l.add(husk("nightmare_husk"));
        l.add(imp("imp"));
        return l;
    }

    // ------------------------------------------------------------------------------------------------
    // Dagannoth Mother
    // ------------------------------------------------------------------------------------------------

    /**
     * The Dagannoth Mother (Horror from the Deep): a towering dagannoth - a forward-leaning, plated body with a ribbed
     * belly, a row of great back spines, a long beaked head with a fanged maw and crest fins, short clawed arms, thick
     * three-toed legs and a heavy tail. The Gelatinnoth Mother (Recipe for Disaster) is the same horror in quivering,
     * glowing green jelly.
     */
    static ModelSpec dagannothMother(String id, float scale, boolean jelly) {
        int hide = jelly ? 0x6AC06A : 0x6E7048;
        int dk = jelly ? 0x3A8A4A : 0x40422A;
        int belly = jelly ? 0xB8F0A0 : 0xB0A878;
        int spine = jelly ? 0xA8F0B0 : 0x8A7A5A;
        int eye = jelly ? 0xE0FF80 : 0xFF6A20;
        String mat = jelly ? "slime" : "scales";
        String glow = jelly ? "cracks" : "none";
        ModelSpec.Builder b = ModelSpec.builder(id).scale(scale).shadow(1.1f);
        part(b, "hips", null, Role.BODY, hide).accent(jelly ? 0xE0FF80 : dk).at(0, 14, 1).mat(mat).glow(glow).box(-5, -3, -4, 10, 6, 8).add();
        part(b, "torso", "hips", Role.STATIC, hide).accent(jelly ? 0xE0FF80 : dk).at(0, -2, 0).rot(16, 0, 0).mat(mat).glow(glow)
                .box(-5.5f, -13, -4.5f, 11, 13, 9).add();
        part(b, "chest", "torso", Role.STATIC, hide).accent(dk).at(0, -12, 0).rot(-6, 0, 0).mat(mat).box(-5, -3, -4, 10, 4, 8).add();
        for (int i = 0; i < 5; i++) {
            part(b, "belly_plate" + i, "torso", Role.STATIC, i % 2 == 0 ? belly : dark(belly, 0.08f)).at(0, -1.5f - i * 2.4f, -4.5f)
                    .rot(-4, 0, 0).mat("plates").box(-3.5f + i * 0.2f, -1, -1, 7 - (int) (i * 0.4f), 2, 2).add();
        }
        sym(b, "flank", "torso", false, dk).accent(hide).at(5.5f, -6, 0).rot(0, 0, -4).mat(mat).box(-0.5f, -5, -3.5f, 1, 10, 7).add();
        int[] sp = {6, 8, 7, 5, 4};
        for (int i = 0; i < sp.length; i++) {
            part(b, "spine" + i, "torso", Role.STATIC, i % 2 == 0 ? spine : dark(spine, 0.15f)).accent(dk).at(0, -12 + i * 2.8f, 4)
                    .rot(-55 + i * 6, 0, 0).mat(jelly ? "slime" : "horn").box(-1, -sp[i], -1, 2, sp[i], 2).box(-0.5f, -sp[i] - 2, -0.5f, 1, 2, 1).add();
            part(b, "web" + i, "spine" + i, Role.STATIC, dk).at(0, -1, 1).mat("membrane").box(-0.5f, -sp[i] + 1, 0, 1, sp[i] - 1, 2).add();
        }
        // neck and long beaked head
        part(b, "neck", "chest", Role.NECK, hide).accent(dk).at(0, -2, -1).rot(-30, 0, 0).mat(mat).box(-3.5f, -6, -3.5f, 7, 7, 7).add();
        part(b, "head", "neck", Role.HEAD, hide).accent(eye).at(0, -6, -1).rot(14, 0, 0).mat(mat).glow(glow).box(-3.5f, -4, -6, 7, 5, 8).add();
        part(b, "skull_ridge", "head", Role.STATIC, dk).at(0, -4, 0).rot(-6, 0, 0).mat(mat).box(-1.5f, -2, -5, 3, 2, 8).add();
        part(b, "beak", "head", Role.STATIC, dark(hide, 0.1f)).accent(dk).at(0, -1, -6).rot(10, 0, 0).mat(mat).box(-2.5f, -2, -6, 5, 3, 6).add();
        part(b, "beak_tip", "beak", Role.STATIC, spine).at(0, 0, -6).rot(35, 0, 0).mat("horn").box(-1, -1, -2, 2, 2, 2).add();
        b.part("eyes", "head", Role.STATIC, eye).pivot(0, -2.2f, -5).material("energy").glow("full")
                .box(3.1f, -0.5f, -1, 1, 1, 2).box(-4.1f, -0.5f, -1, 1, 1, 2);
        part(b, "jaw", "head", Role.JAW, belly).accent(0xC03020).at(0, 1, -3).rot(22, 0, 0).mat(mat).glow("maw").box(-2.5f, 0, -8, 5, 2, 8).add();
        sideFangs(b, "fangs", "beak", 0xF0E8D0, 2.4f, 1, -5, 1.6f, 3, 1, false);
        teeth(b, "lower_teeth", "jaw", 0xE8E0C8, 0, 0, -7.5f, 4, 3, 1, true);
        sym(b, "crest_fin", "head", false, spine).accent(dk).at(3.5f, -3, -1).rot(-20, -30, -25).mat("membrane").box(0, -4, -2, 1, 5, 5).add();
        sym(b, "crest_spike", "crest_fin", true, spine).at(0.5f, -4, 0).rot(-30, 0, 0).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        // short clawed arms
        sym(b, "arm", "torso", false, hide).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(dk).at(5.5f, -10, -2).rot(-40, 0, -15).mat(mat)
                .box(-1.5f, -1, -1.5f, 3, 6, 3).add();
        sym(b, "forearm", "arm", true, dark(hide, 0.08f)).at(0, 5.5f, 0).rot(-50, 0, 0).mat(mat).box(-1.5f, 0, -1.5f, 3, 5, 3).add();
        Kit.Sym cl = sym(b, "claws", "forearm", true, spine).at(0, 5, 0).rot(20, 0, 0).mat("horn");
        cl.box(-1.5f, 0, -1, 1, 3, 1).box(-0.2f, 0, -1, 1, 3, 1).box(1, 0, -1, 1, 3, 1).add();
        // thick legs with three toes
        sym(b, "leg", null, false, hide).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(dk).at(3.8f, 15, 1).rot(-20, 0, -4).mat(mat)
                .box(-2.5f, -1, -2.5f, 5, 6, 5).add();
        sym(b, "shin", "leg", true, dark(hide, 0.08f)).at(0, 5, 0).rot(30, 0, 2).mat(mat).box(-2, 0, -2, 4, 4, 4).add();
        sym(b, "foot", "shin", true, dk).at(0, 3.5f, 0).rot(-10, 0, 2).mat(mat).box(-2.5f, 0, -4, 5, 2, 6).add();
        Kit.Sym tc = sym(b, "toe_claws", "foot", true, spine).at(0, 1, -4).mat("horn");
        tc.box(-2.3f, -0.6f, -1.6f, 1, 1, 2).box(-0.5f, -0.6f, -2, 1, 1, 2).box(1.3f, -0.6f, -1.6f, 1, 1, 2).add();
        chain(b, "tail", "hips", Role.TAIL, hide, dk, mat, 0, 1, 3.5f, false, new int[]{5, 4, 3, 2}, new int[]{4, 4, 4, 3},
                new float[]{20, 10, 8, 8}, null);
        if (jelly) {
            part(b, "core", "torso", Role.STATIC, 0xE8FF90).at(0, -7, 0).mat("energy").glow("full").box(-2, -2, -2, 4, 4, 4).add();
            part(b, "drips", "hips", Role.STATIC, belly).at(0, 3, 0).mat("slime").glow("full").box(-3, 0, -3.5f, 1, 2, 1).box(2, 0, 3, 1, 3, 1)
                    .box(4, -1, -2, 1, 2, 1).add();
        }
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // The Inadequacy
    // ------------------------------------------------------------------------------------------------

    /**
     * The Inadequacy (Dream Mentor): a mountainous, oozing mass of nightmare-black flesh, its heaving body piled in folds
     * over a spreading base, studded with weeping violet eyes, a gaping fanged maw, a pulsing core and writhing arms.
     */
    static ModelSpec inadequacy(String id) {
        int ink = 0x22202E;
        int ink2 = 0x16141E;
        int fold = 0x34304A;
        int glow = 0xC070FF;
        ModelSpec.Builder b = ModelSpec.builder(id).scale(2.0f).shadow(1.5f);
        // a sprawling, oozing base
        part(b, "base", null, Role.BODY, ink2).accent(glow).at(0, 22, 1).mat("slime").box(-10, -2, -9, 20, 4, 18).add();
        sym(b, "base_lobe", "base", false, ink).at(9.5f, 0.5f, 3).rot(0, 25, 0).mat("slime").box(-2.5f, -1.5f, -4, 5, 3, 8).add();
        sym(b, "base_lobe_front", "base", false, fold).at(7, 0.5f, -8).rot(0, -30, 0).mat("slime").box(-2.5f, -1.5f, -2.5f, 5, 3, 5).add();
        part(b, "base_rear", "base", Role.STATIC, ink).at(0, 0.5f, 9).rot(-10, 0, 0).mat("slime").box(-7, -1.5f, -1, 14, 3, 3).add();
        // heaped body, layered in folds, leaning forward
        part(b, "mass", "base", Role.STATIC, ink).accent(glow).at(0, -2, 1).mat("slime").glow("cracks").box(-8.5f, -6, -7.5f, 17, 6, 15).add();
        part(b, "upper", "mass", Role.STATIC, fold).accent(glow).at(0, -6, 1.5f).rot(-8, 0, 0).mat("slime").glow("cracks").box(-7, -5, -6, 14, 5, 12).add();
        part(b, "hump", "upper", Role.STATIC, ink).accent(glow).at(0, -5, 2).rot(-12, 0, 0).mat("slime").box(-5, -4, -4, 10, 4, 8).add();
        part(b, "crown", "hump", Role.STATIC, ink2).at(0.5f, -4, 1).rot(-10, 10, 0).mat("slime").box(-3, -2, -2.5f, 6, 2, 5).add();
        float[][] folds = {{7.5f, -2, -4}, {-7.5f, -2, -3}, {8, -3, 3}, {-8, -2.5f, 4}, {5.5f, -7, -2}, {-5.5f, -7.5f, 1}, {3, -9, 4}, {-3, -9.5f, -1}};
        for (int i = 0; i < folds.length; i++) {
            float[] f = folds[i];
            part(b, "fold" + i, "mass", Role.STATIC, i % 3 == 0 ? fold : i % 3 == 1 ? ink2 : ink).at(f[0], f[1], f[2])
                    .rot(i * 11, i * 23, f[0] > 0 ? -12 : 12).mat("slime").box(-2, -2, -2.5f, 4, 4, 5).add();
        }
        part(b, "core", "mass", Role.STATIC, glow).accent(0xF0B0FF).at(0, -3, -7.5f).mat("energy").glow("full").box(-2, -2, -0.6f, 4, 4, 1).add();
        part(b, "core_ring", "core", Role.STATIC, fold).at(0, 0, -0.2f).mat("slime").box(-3, -3, 0, 6, 1, 1).box(-3, 2, 0, 6, 1, 1)
                .box(-3, -2, 0, 1, 4, 1).box(2, -2, 0, 1, 4, 1).add();
        // head lump thrust forward with a fanged maw and many eyes
        part(b, "head", "upper", Role.HEAD, fold).accent(glow).at(0, -2, -6).rot(14, 0, 0).mat("slime").box(-5, -4, -5, 10, 6, 6).add();
        part(b, "brow", "head", Role.STATIC, ink2).at(0, -4, -5).rot(15, 0, 0).mat("slime").box(-5.5f, -1, -1.5f, 11, 2, 3).add();
        sym(b, "cheek", "head", false, ink).at(5, -0.5f, -3).rot(0, -15, 0).mat("slime").box(-1, -2.5f, -2.5f, 2, 5, 5).add();
        b.part("eyes", "head", Role.STATIC, glow).pivot(0, -2.4f, -5.2f).material("energy").glow("full")
                .box(1.2f, -0.5f, -0.4f, 2, 1, 1).box(-3.2f, -0.5f, -0.4f, 2, 1, 1).box(-0.5f, -2.2f, -0.2f, 1, 1, 1, -0.1f)
                .box(3.6f, -1.6f, 0.4f, 1, 1, 1, -0.1f).box(-4.6f, -1.6f, 0.4f, 1, 1, 1, -0.1f);
        part(b, "jaw", "head", Role.JAW, ink2).accent(glow).at(0, 1.5f, -4).rot(22, 0, 0).mat("slime").glow("maw").box(-4, 0, -3, 8, 2, 4).add();
        teeth(b, "teeth", "head", 0xD8D0E0, 0, 1.6f, -4.8f, 7, 6, 1, false);
        teeth(b, "lower_teeth", "jaw", 0xC8C0D0, 0, 0, -2.6f, 6, 5, 1, true);
        Kit.P eyes2 = part(b, "body_eyes", "mass", Role.STATIC, glow).at(0, 0, 0).mat("energy").glow("full");
        eyes2.box(5, -4, -7.8f, 1, 1, 1).box(-6.5f, -2, -7.8f, 2, 1, 1).box(8.2f, -2, -2, 1, 1, 1).box(-8.8f, -4, 1, 1, 1, 1).box(8.2f, -4, 4, 1, 1, 1);
        eyes2.add();
        // writhing arms: two great clawed limbs and two lesser tentacles
        float[][] arms = {{8, -4, -3, 0}, {-8, -4, -3, 0}, {6, -4, 2, -20}, {-6, -4, 2, 20}};
        for (int i = 0; i < arms.length; i++) {
            float[] a = arms[i];
            boolean big = i < 2;
            String par = big ? "mass" : "upper";
            String n = "arm" + i;
            Role role = i == 0 ? Role.ARM_LEFT : i == 1 ? Role.ARM_RIGHT : Role.TENTACLE;
            float side = a[0] > 0 ? 1 : -1;
            part(b, n, par, role, ink).accent(glow).at(a[0], a[1], a[2]).rot(big ? -35 : -10, 0, a[3] - side * (big ? 50 : 70)).mat("slime")
                    .box(-2, 0, -2, big ? 4 : 3, big ? 7 : 6, big ? 4 : 3).add();
            part(b, n + "_b", n, Role.TENTACLE, fold).at(0, big ? 7 : 6, 0).rot(-30, 0, side * 30).mat("slime").box(-1.5f, 0, -1.5f, 3, 6, 3).add();
            part(b, n + "_c", n + "_b", Role.STATIC, ink2).at(0, 6, 0).rot(-30, 0, side * 30).mat("slime").box(-1, 0, -1, 2, 4, 2).add();
            if (big) {
                part(b, n + "_claw", n + "_c", Role.STATIC, 0xC8C0D8).at(0, 4, 0).rot(20, 0, 0).mat("bone")
                        .box(-1.2f, 0, -0.5f, 1, 3, 1).box(0.2f, 0, -0.5f, 1, 3, 1).box(-0.5f, 0, 0.6f, 1, 2, 1).add();
            } else {
                part(b, n + "_tip", n + "_c", Role.STATIC, glow).at(0, 4, 0).mat("energy").glow("full").box(-0.5f, 0, -0.5f, 1, 2, 1, -0.1f).add();
            }
        }
        part(b, "drips", "mass", Role.STATIC, glow).at(0, 0, 0).mat("slime").glow("full").box(-6, 0, -7.8f, 1, 3, 1, -0.2f)
                .box(3, 0, -7.8f, 1, 2, 1, -0.2f).box(8.2f, -1, -1, 1, 3, 1, -0.2f).box(-8.8f, 0, -4, 1, 2, 1, -0.2f).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Agrith-Na-Na
    // ------------------------------------------------------------------------------------------------

    /**
     * Agrith-Na-Na (Recipe for Disaster): a squat, gluttonous demon of sallow orange hide with a vast round belly, stumpy
     * legs, long clawed arms, a horned bulldog head with tusks, little leathery wings and a forked tail.
     */
    static ModelSpec agrithNaNa(String id) {
        int hide = 0xB0763A;
        int dk = 0x6A4220;
        int belly = 0xD8A868;
        int horn = 0xE0D4B0;
        int eye = 0xFF3020;
        ModelSpec.Builder b = ModelSpec.builder(id).scale(1.7f).shadow(1.0f);
        part(b, "hips", null, Role.BODY, hide).accent(dk).at(0, 15, 0).mat("hide").box(-5, -3, -3.5f, 10, 5, 7).add();
        part(b, "belly", "hips", Role.STATIC, belly).accent(hide).at(0, -5, -1).mat("skin").box(-6.5f, -6, -5.5f, 13, 10, 10).add();
        part(b, "belly_low", "belly", Role.STATIC, belly).at(0, 4, -1).mat("skin").box(-5.5f, -1, -4.5f, 11, 2, 8).add();
        part(b, "navel", "belly", Role.STATIC, dark(belly, 0.3f)).at(0, 0, -5.5f).mat("skin").box(-0.5f, -0.5f, -0.6f, 1, 1, 1).add();
        for (int i = 0; i < 3; i++) {
            part(b, "fold" + i, "belly", Role.STATIC, dark(belly, 0.15f)).at(0, -4 + i * 3, -5.6f).mat("skin").box(-4.5f + i, -0.5f, -0.3f, 9 - 2 * i, 1, 1, -0.1f).add();
        }
        part(b, "chest", "belly", Role.STATIC, hide).accent(dk).at(0, -6, 0.5f).rot(-6, 0, 0).mat("hide").box(-6, -5, -4, 12, 5, 8).add();
        sym(b, "pec", "chest", false, dark(hide, 0.08f)).at(3, -2.5f, -4).mat("hide").box(-2.5f, -2, -0.6f, 5, 4, 1).add();
        part(b, "back", "chest", Role.STATIC, dk).at(0, -2, 4).mat("hide").box(-5, -3, -0.5f, 10, 6, 2).add();
        BestiaryQbm.spines(b, "spine", "back", horn, dark(horn, 0.15f), "horn", -2, 0, 1.8f, new int[]{2, 3, 2}, -40, 1);
        // head
        part(b, "head", "chest", Role.HEAD, hide).accent(eye).at(0, -5, -1).mat("hide").box(-4, -6, -4.5f, 8, 6, 6).add();
        part(b, "snout", "head", Role.STATIC, dark(hide, 0.08f)).at(0, -1.5f, -4.5f).mat("hide").box(-3, -1.5f, -2, 6, 3, 2).add();
        part(b, "nostrils", "snout", Role.STATIC, 0x2A1810).at(0, -1, -2).mat("skin").box(-1.5f, -0.5f, -0.3f, 1, 1, 1, -0.1f).box(0.5f, -0.5f, -0.3f, 1, 1, 1, -0.1f).add();
        part(b, "brow", "head", Role.STATIC, dk).at(0, -4, -4.5f).rot(18, 0, 0).mat("hide").box(-4.5f, -1, -1, 9, 2, 2).add();
        b.part("eyes", "head", Role.STATIC, eye).pivot(0, -3.2f, -4.7f).material("energy").glow("full")
                .box(1.2f, -0.5f, -0.4f, 2, 1, 1).box(-3.2f, -0.5f, -0.4f, 2, 1, 1);
        part(b, "jaw", "head", Role.JAW, dk).accent(0xFF7020).at(0, 0, -3).rot(10, 0, 0).mat("hide").glow("maw").box(-3.5f, 0, -3, 7, 2, 4).add();
        sym(b, "tusk", "jaw", false, horn).at(2.6f, 0, -2.6f).rot(-15, 0, 12).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        sym(b, "jowl", "head", false, hide).at(4, -1, -3).rot(0, -15, 0).mat("hide").box(-1, -2, -1.5f, 2, 3, 3).add();
        sym(b, "horn", "head", false, horn).at(3.2f, -6, -1.5f).rot(-20, 0, -40).mat("horn").box(-1, -3, -1, 2, 3, 2).add();
        sym(b, "horn_mid", "horn", true, dark(horn, 0.1f)).at(0, -3, 0).rot(0, 0, 40).mat("horn").box(-0.75f, -3, -0.75f, 2, 3, 2, -0.25f).add();
        sym(b, "horn_tip", "horn_mid", true, dark(horn, 0.25f)).at(0, -3, 0).rot(0, 0, 30).mat("horn").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        sym(b, "ear", "head", false, hide).at(4, -4, 0).rot(0, -20, -50).mat("hide").box(0, -0.5f, -1, 3, 1, 2).add();
        // long arms
        sym(b, "arm", "chest", false, hide).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(dk).at(6.5f, -4, -0.5f).rot(-15, 0, -20).mat("hide")
                .box(-1.5f, -1, -2, 4, 7, 4).add();
        sym(b, "shoulder", "arm", true, dk).at(0.5f, -1, 0).mat("hide").box(-2, -1.5f, -2.5f, 5, 3, 5).add();
        sym(b, "forearm", "arm", true, hide).at(0.5f, 6.5f, 0).rot(-25, 0, 12).mat("hide").box(-1.5f, 0, -1.5f, 3, 7, 3).add();
        sym(b, "hand", "forearm", true, dk).at(0, 7, 0).mat("hide").box(-2, 0, -2, 4, 2, 4).add();
        Kit.Sym hc = sym(b, "talons", "hand", true, horn).at(0, 2, -1).rot(30, 0, 0).mat("horn");
        hc.box(-1.8f, 0, -1, 1, 3, 1).box(-0.5f, 0, -1, 1, 3, 1).box(0.8f, 0, -1, 1, 3, 1).add();
        // little wings
        sym(b, "wing", "back", false, dk).roles(Role.WING_LEFT, Role.WING_RIGHT).accent(0x8A3A1A).at(3, -2, 1).rot(0, -30, -20).mat("membrane")
                .box(0, -4, 0, 7, 6, 1).add();
        sym(b, "wing_bone", "wing", true, horn).at(0, -4, 0).mat("bone").box(0, -0.5f, -0.3f, 8, 1, 1, 0.1f).box(7, -0.5f, -0.3f, 1, 3, 1).add();
        // stumpy legs
        sym(b, "leg", null, false, hide).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(dk).at(3.5f, 16, 0.5f).rot(-10, 0, -6).mat("hide")
                .box(-2.5f, -1, -2.5f, 5, 5, 5).add();
        sym(b, "shin", "leg", true, dk).at(0, 4, 0).rot(10, 0, 6).mat("hide").box(-2, 0, -2, 4, 3, 4).add();
        sym(b, "hoof", "shin", true, 0x2A1A10).at(0, 2.5f, 0).mat("horn").box(-2.5f, 0, -3.5f, 5, 2, 5).add();
        Kit.Sym tc = sym(b, "toe_claws", "hoof", true, horn).at(0, 1, -3.5f).mat("horn");
        tc.box(-2.2f, -0.6f, -1, 1, 1, 1).box(-0.5f, -0.6f, -1, 1, 1, 1).box(1.2f, -0.6f, -1, 1, 1, 1).add();
        String tail = chain(b, "tail", "hips", Role.TAIL, hide, dk, "hide", 0, 0, 3.5f, false, new int[]{2, 2, 1, 1}, new int[]{4, 4, 4, 3},
                new float[]{30, -10, -15, -15}, null);
        part(b, "tail_fork", tail, Role.STATIC, dk).at(0, 0, 3).mat("hide").box(-1.5f, -0.5f, 0, 3, 1, 2).box(-2, -0.5f, 1.5f, 1, 1, 2).box(1, -0.5f, 1.5f, 1, 1, 2).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Tanglefoot
    // ------------------------------------------------------------------------------------------------

    /**
     * Tanglefoot (Fairytale I): a huge walking bramble - a gnarled, bulbous root-trunk on a spread of crawling roots, a
     * thorned maw, a crown of broad leaves and pale flowers, and lashing vines.
     */
    static ModelSpec tanglefoot(String id) {
        int bark = 0x4A3A22;
        int bark2 = 0x2E2414;
        int leaf = 0x4A8A2E;
        int leaf2 = 0x2E6A1E;
        int flower = 0xE8D0E8;
        int thorn = 0xC8B890;
        ModelSpec.Builder b = ModelSpec.builder(id).scale(1.7f).shadow(1.2f);
        part(b, "trunk", null, Role.BODY, bark).accent(bark2).at(0, 18, 0).mat("wood").box(-5, -10, -4.5f, 10, 10, 9).add();
        part(b, "bulb", "trunk", Role.STATIC, bark).accent(leaf2).at(0, -10, 0).mat("wood").box(-4, -6, -3.5f, 8, 6, 7).add();
        part(b, "moss", "trunk", Role.STATIC, leaf2).at(0, -10, 0).mat("moss").box(-5.2f, 0, -4.7f, 4, 2, 4, -0.1f).box(1, -1, 0, 4, 2, 5, -0.1f).add();
        sym(b, "burl", "trunk", false, bark2).at(5, -4, 1).rot(0, 20, 0).mat("wood").box(-1.5f, -2, -2, 3, 4, 4).add();
        // the maw
        part(b, "head", "trunk", Role.HEAD, bark).accent(0xFFE060).at(0, -6, -4.5f).mat("wood").box(-3.5f, -3, -1.5f, 7, 4, 2).add();
        b.part("eyes", "head", Role.STATIC, 0xFFE060).pivot(0, -1.8f, -1.7f).material("energy").glow("full")
                .box(1.2f, -0.5f, -0.4f, 1, 1, 1).box(-2.2f, -0.5f, -0.4f, 1, 1, 1);
        part(b, "jaw", "head", Role.JAW, bark2).accent(0x8AE040).at(0, 1, -1).rot(20, 0, 0).mat("wood").glow("maw").box(-3, 0, -2, 6, 2, 3).add();
        teeth(b, "thorns_up", "head", thorn, 0, 1, -1.8f, 5, 4, 1, false);
        // roots
        float[][] roots = {{4, -3, 40}, {-4, -3, -40}, {5, 2, 100}, {-5, 2, -100}, {2, 4, 160}, {-2, 4, -160}};
        for (int i = 0; i < roots.length; i++) {
            float[] r = roots[i];
            String n = "root" + i;
            part(b, n, "trunk", Role.LEG_EXTRA, bark2).accent(bark).at(r[0], -1, r[1]).rot(0, r[2], 0).mat("wood").box(-1.5f, -1.5f, -1.5f, 3, 3, 3).add();
            part(b, n + "_a", n, Role.STATIC, bark).at(0, 0, -1).rot(-20, 0, 0).mat("wood").box(-1.5f, -1, -5, 3, 3, 5).add();
            part(b, n + "_b", n + "_a", Role.STATIC, bark2).at(0, 0.5f, -5).rot(30, 0, 0).mat("wood").box(-1, -1, -4, 2, 2, 4).add();
            part(b, n + "_tip", n + "_b", Role.STATIC, bark2).at(0, 0, -4).rot(20, 0, 0).mat("wood").box(-0.5f, -0.5f, -3, 1, 1, 3).add();
        }
        // vines
        for (int i = 0; i < 4; i++) {
            boolean l = i % 2 == 0;
            String n = "vine" + i;
            Role role = i == 0 ? Role.ARM_LEFT : i == 1 ? Role.ARM_RIGHT : Role.TENTACLE;
            part(b, n, "bulb", role, leaf2).accent(leaf).at(l ? 3.5f : -3.5f, i < 2 ? -2 : -5, i < 2 ? -2 : 1).rot(-40, 0, l ? -55 : 55).mat("wood")
                    .box(-1, 0, -1, 2, 6, 2).add();
            part(b, n + "_b", n, Role.TENTACLE, leaf2).at(0, 6, 0).rot(-30, 0, l ? 30 : -30).mat("wood").box(-0.5f, 0, -0.5f, 1, 6, 1, 0.2f).add();
            part(b, n + "_thorns", n, Role.STATIC, thorn).at(0, 3, 0).mat("horn").box(0.5f, -0.5f, -0.5f, 1, 1, 1).box(-1.5f, 1.5f, -0.5f, 1, 1, 1)
                    .box(-0.5f, 4, 0.5f, 1, 1, 1).add();
            part(b, n + "_leaf", n + "_b", Role.STATIC, leaf).at(0, 6, 0).rot(30, 0, 0).mat("moss").box(-1.5f, 0, -0.5f, 3, 3, 1).add();
        }
        // leafy crown and flowers
        for (int i = 0; i < 7; i++) {
            float yaw = i * (360f / 7);
            part(b, "leaf" + i, "bulb", Role.STATIC, i % 2 == 0 ? leaf : leaf2).accent(leaf2).at(0, -5.5f, 0).rot(0, yaw, 0).mat("moss")
                    .box(-0.5f, -1, -0.5f, 1, 1, 1).add();
            part(b, "frond" + i, "leaf" + i, Role.STATIC, i % 2 == 0 ? leaf : leaf2).accent(leaf2).at(0, 0, -2.5f).rot(55 + (i % 3) * 8, 0, 0)
                    .mat("moss").box(-2, -7, -0.5f, 4, 7, 1).box(-1, -9, -0.5f, 2, 2, 1).add();
        }
        part(b, "leaf_heart", "bulb", Role.STATIC, leaf2).at(0, -6, 0).mat("moss").box(-2.5f, -2, -2.5f, 5, 2, 5).add();
        for (int i = 0; i < 3; i++) {
            part(b, "flower" + i, "frond" + (i * 2), Role.STATIC, flower).accent(0xF0E060).at(0, -8, -0.5f).rot(60, 0, 0).mat("cloth")
                    .box(-1.5f, -1, -1.5f, 3, 1, 3).add();
            part(b, "stamen" + i, "flower" + i, Role.STATIC, 0xF0D040).at(0, -1, 0).mat("energy").glow("full").box(-0.5f, -1, -0.5f, 1, 1, 1).add();
        }
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Sea Troll Queen
    // ------------------------------------------------------------------------------------------------

    /**
     * The Sea Troll Queen (Swan Song): a colossal sea troll - a hulking, barnacled blue-green body with a finned back,
     * webbed, clawed hands, kelp hanging from her shoulders, a tusked underbite and a crown of coral and pearls.
     */
    static ModelSpec seaTrollQueen(String id) {
        int skin = 0x3A6A6A;
        int dk = 0x1E3E42;
        int belly = 0x7AA096;
        int fin = 0x2A8A8A;
        int coral = 0xD05A4A;
        int kelp = 0x3A5A22;
        int eye = 0xFFE040;
        ModelSpec.Builder b = ModelSpec.builder(id).scale(2.2f).shadow(1.4f);
        part(b, "hips", null, Role.BODY, skin).accent(dk).at(0, 13, 2).mat("scales").box(-6, -3, -4, 12, 6, 8).add();
        part(b, "torso", "hips", Role.STATIC, skin).accent(dk).at(0, -2, 0).rot(28, 0, 0).mat("scales").box(-7.5f, -13, -5, 15, 13, 10).add();
        part(b, "belly", "torso", Role.STATIC, belly).at(0, -4, -5).rot(-4, 0, 0).mat("plates").box(-5, -7, -1, 10, 8, 2).add();
        part(b, "kelp_skirt", "hips", Role.STATIC, kelp).accent(0x5A7A2A).at(0, 1, 0).mat("moss").box(-6.5f, 0, -4.5f, 13, 4, 9, 0.1f).add();
        for (int i = 0; i < 4; i++) {
            part(b, "kelp" + i, "kelp_skirt", Role.STATIC, i % 2 == 0 ? kelp : 0x4A6A2A).at(-4.5f + i * 3, 4, -4.5f).rot(-5, 0, (i - 1.5f) * 6)
                    .mat("moss").box(-1, 0, -0.5f, 2, 3 + i % 2 * 2, 1).add();
        }
        for (int i = 0; i < 5; i++) {
            part(b, "fin" + i, "torso", Role.STATIC, i % 2 == 0 ? fin : dark(fin, 0.2f)).accent(dk).at(0, -12 + i * 2.6f, 5).rot(-50 + i * 8, 0, 0)
                    .mat("membrane").box(-0.5f, -5 + (i == 4 ? 2 : 0), -1, 1, 5 - (i == 4 ? 2 : 0), 4).add();
        }
        part(b, "barnacles", "torso", Role.STATIC, 0xC8C0A8).at(0, 0, 0).mat("bone").box(5, -10, -5.6f, 1, 1, 1).box(6.6f, -6, -2, 1, 1, 1)
                .box(-7.6f, -9, 1, 1, 1, 1).box(-5, -3, -5.6f, 1, 1, 1).box(3, -12, 4.6f, 1, 1, 1).add();
        sym(b, "shoulder", "torso", false, skin).accent(dk).at(7, -11, 0).rot(0, 0, -15).mat("scales").box(-2, -3, -4, 6, 6, 8).add();
        sym(b, "shoulder_kelp", "shoulder", true, kelp).at(2, 2, -4).mat("moss").box(-2, 0, -0.5f, 4, 6, 1).add();
        // head
        part(b, "head", "torso", Role.HEAD, skin).accent(eye).at(0, -12, -5).rot(-28, 0, 0).mat("scales").box(-5, -6, -6, 10, 8, 7).add();
        part(b, "brow", "head", Role.STATIC, dk).at(0, -4, -6).rot(16, 0, 0).mat("scales").box(-5.5f, -1, -1.5f, 11, 2, 3).add();
        part(b, "nose", "head", Role.STATIC, dark(skin, 0.08f)).at(0, -2.5f, -6).rot(-15, 0, 0).mat("scales").box(-1.5f, -1, -3, 3, 4, 3).add();
        b.part("eyes", "head", Role.STATIC, eye).pivot(0, -3, -6.2f).material("energy").glow("full")
                .box(1.6f, -0.5f, -0.4f, 2, 1, 1).box(-3.6f, -0.5f, -0.4f, 2, 1, 1);
        part(b, "jaw", "head", Role.JAW, belly).accent(0x2A1A1A).at(0, 1.5f, -4).rot(10, 0, 0).mat("scales").box(-4.5f, 0, -3, 9, 3, 4).add();
        sym(b, "tusk", "jaw", false, 0xE8E0C8).at(3.2f, 0, -2.7f).rot(-10, 0, 8).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        teeth(b, "teeth", "jaw", 0xE8E0C8, 0, 0, -2.6f, 5, 3, 1, true);
        sym(b, "gill", "head", false, fin).accent(dk).at(5, -1, -2).rot(0, -30, -10).mat("membrane").box(0, -3, -1.5f, 3, 5, 1).box(0, -2, 0.5f, 2, 4, 1).add();
        sym(b, "ear_fin", "head", false, fin).at(4.5f, -5, -1).rot(-20, -20, -35).mat("membrane").box(0, -3, -1, 1, 4, 3).add();
        part(b, "crown", "head", Role.STATIC, coral).accent(0xF08A70).at(0, -6, -2).rot(-8, 0, 0).mat("rock").box(-4, -1.5f, -2.5f, 8, 2, 5).add();
        for (int i = 0; i < 5; i++) {
            int h = i == 2 ? 4 : 2 + i % 2;
            part(b, "coral" + i, "crown", Role.STATIC, i % 2 == 0 ? coral : light(coral, 0.2f)).at(-3.5f + i * 1.75f, -1.5f, -1).rot(-10, 0, (i - 2) * 14)
                    .mat("rock").box(-0.5f, -h, -0.5f, 1, h, 1).box(-1, -h + 1, -0.5f, 2, 1, 1).add();
        }
        part(b, "pearls", "crown", Role.STATIC, 0xF0F0F8).at(0, -0.5f, -2.7f).mat("energy").glow("full").box(-3, -0.5f, -0.3f, 1, 1, 1)
                .box(-0.5f, -0.5f, -0.3f, 1, 1, 1).box(2, -0.5f, -0.3f, 1, 1, 1).add();
        // huge webbed arms
        sym(b, "arm", null, false, skin).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(dk).at(11, 1, -6).rot(-20, 0, -20).mat("scales")
                .box(-3, -2, -3, 6, 10, 6).add();
        sym(b, "forearm", "arm", true, skin).accent(dk).at(0, 8.5f, 0).rot(-10, 0, 14).mat("scales").box(-3, 0, -3, 6, 9, 6).add();
        sym(b, "arm_fin", "forearm", true, fin).at(3, 1, 0).rot(0, 0, -10).mat("membrane").box(0, 0, -0.5f, 3, 7, 1).add();
        sym(b, "hand", "forearm", true, dark(skin, 0.1f)).at(0, 9, -0.5f).rot(14, 0, 8).mat("scales").box(-3.5f, 0, -3.5f, 7, 3, 6).add();
        sym(b, "web", "hand", true, fin).at(0, 3, -3.5f).rot(30, 0, 0).mat("membrane").box(-3, 0, -0.3f, 6, 2, 1).add();
        Kit.Sym cl = sym(b, "claws", "hand", true, 0xE0D8C0).at(0, 3, -3.5f).rot(40, 0, 0).mat("horn");
        for (int k = 0; k < 4; k++) {
            cl.box(-3.2f + k * 2, 1, -0.5f, 1, 2, 1);
        }
        cl.add();
        // stubby fish-tailed legs
        sym(b, "leg", null, false, skin).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(dk).at(4.5f, 14, 2).rot(-16, 0, -6).mat("scales")
                .box(-3, -1, -3, 6, 6, 6).add();
        sym(b, "shin", "leg", true, skin).at(0, 5, 0).rot(20, 0, 6).mat("scales").box(-2.5f, 0, -2.5f, 5, 5, 5).add();
        sym(b, "foot", "shin", true, dk).at(0, 4.5f, 0).rot(-4, 0, 0).mat("scales").box(-3, 0, -5, 6, 2, 7).add();
        sym(b, "foot_web", "foot", true, fin).at(0, 1, -5).mat("membrane").box(-3, -0.5f, -2, 6, 1, 2).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Slagilith
    // ------------------------------------------------------------------------------------------------

    /**
     * The Slagilith (One Small Favour): a lumbering golem of fused slag boulders - a heaped rock body, a craggy head with
     * a smouldering seam for eyes, and great boulder fists on segmented stone arms.
     */
    static ModelSpec slagilith(String id) {
        int rock = 0x6A645A;
        int rock2 = 0x4A463E;
        int slag = 0x3A3430;
        int heat = 0xFF8A30;
        ModelSpec.Builder b = ModelSpec.builder(id).scale(1.13f).shadow(1.1f);
        part(b, "hips", null, Role.BODY, rock2).accent(heat).at(0, 13, 1).mat("rock").box(-5, -3, -4, 10, 5, 8).add();
        part(b, "hip_stones", "hips", Role.STATIC, slag).at(0, 0, 0).mat("rock").box(-5.5f, -1, -4.5f, 4, 3, 3).box(1.5f, -1.5f, -4.5f, 4, 3, 3)
                .box(-2, -1, 3, 4, 3, 2).add();
        part(b, "torso", "hips", Role.STATIC, rock).accent(heat).at(0, -2, 0).rot(14, 0, 0).mat("rock").glow("cracks").box(-7, -11, -5, 14, 11, 10).add();
        part(b, "chest_slab", "torso", Role.STATIC, rock2).accent(heat).at(0, -6, -5).rot(-6, 0, 0).mat("rock").box(-5, -4, -1, 10, 7, 2).add();
        part(b, "chest_seam", "chest_slab", Role.STATIC, heat).accent(0xFFE080).at(0, 0, -1.1f).mat("lava").glow("full")
                .box(-0.5f, -3.5f, -0.2f, 1, 6, 1, -0.2f).box(-3, 1, -0.2f, 6, 1, 1, -0.2f).add();
        part(b, "belly_stone", "torso", Role.STATIC, slag).at(0, -1, -5).rot(10, 0, 0).mat("rock").box(-4, -2, -1, 8, 3, 2).add();
        part(b, "back_boulder", "torso", Role.STATIC, slag).at(0, -8, 5).rot(10, 0, 0).mat("rock").box(-5, -4, -1, 10, 7, 4).add();
        part(b, "back_boulder2", "back_boulder", Role.STATIC, rock2).at(1, -4, 2).rot(0, 20, 10).mat("rock").box(-3, -3, -2, 6, 4, 4).add();
        float[][] rubble = {{4, -11, -2}, {-3.5f, -11.5f, 0}, {5, -11, 2.5f}, {-5.5f, -10.5f, -2.5f}, {1, -12, 3}, {-1, -11, -4}, {6.5f, -4, -4},
                {-6.5f, -5, -3}, {7, -2, 3}, {-7, -1, 2}};
        for (int i = 0; i < rubble.length; i++) {
            float[] r = rubble[i];
            int sz = i < 6 ? 3 : 2;
            part(b, "rubble" + i, "torso", Role.STATIC, i % 3 == 0 ? rock2 : i % 3 == 1 ? slag : rock).at(r[0], r[1], r[2]).rot(i * 15, i * 27, i * 11)
                    .mat("rock").box(-sz / 2f, -sz / 2f - 0.5f, -sz / 2f, sz, sz, sz).add();
        }
        sym(b, "shoulder", "torso", false, rock2).accent(heat).at(7, -9, 0).rot(0, 15, -20).mat("rock").box(-2, -3.5f, -3.5f, 6, 6, 7).add();
        sym(b, "shoulder_cap", "shoulder", true, slag).at(1, -3.5f, 0).rot(0, -20, -10).mat("rock").box(-2, -2, -2.5f, 5, 2, 5).add();
        // head: a craggy block with a smouldering seam for eyes
        part(b, "head", "torso", Role.HEAD, rock).accent(heat).at(0, -11, -2).rot(-14, 0, 0).mat("rock").box(-3.5f, -5, -4, 7, 6, 6).add();
        part(b, "brow", "head", Role.STATIC, rock2).at(0, -3, -4).rot(20, 0, 0).mat("rock").box(-4, -1.5f, -1.5f, 8, 3, 2).add();
        b.part("eyes", "head", Role.STATIC, heat).pivot(0, -1.6f, -4.2f).material("lava").glow("full")
                .box(-2.8f, -0.5f, -0.4f, 2, 1, 1).box(0.8f, -0.5f, -0.4f, 2, 1, 1);
        part(b, "jaw", "head", Role.JAW, slag).accent(heat).at(0, 0.5f, -3).rot(6, 0, 0).mat("rock").glow("maw").box(-3, 0, -1.5f, 6, 2, 3).add();
        part(b, "crag", "head", Role.STATIC, rock2).at(1, -5, 0).rot(0, 20, 15).mat("rock").box(-2, -2, -2, 4, 2, 4).add();
        part(b, "crag2", "head", Role.STATIC, slag).at(-2, -5, 1).rot(0, -25, -10).mat("rock").box(-1.5f, -1.5f, -1.5f, 3, 2, 3).add();
        sym(b, "cheek_stone", "head", false, slag).at(3.5f, -1, -2).rot(0, -15, 0).mat("rock").box(-1, -2, -2, 2, 3, 4).add();
        // segmented stone arms with boulder fists
        sym(b, "arm", "torso", false, rock).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(heat).at(9, -8, -0.5f).rot(-20, 0, -10).mat("rock").glow("cracks")
                .box(-2.5f, -1, -2.5f, 5, 7, 5).add();
        sym(b, "arm_plate", "arm", true, slag).at(2.5f, 1, 0).rot(0, 0, -8).mat("rock").box(0, 0, -2, 1, 5, 4).add();
        sym(b, "elbow", "arm", true, slag).at(0, 6.5f, 0).mat("rock").box(-2, -1.5f, -2, 4, 3, 4, 0.3f).add();
        sym(b, "forearm", "arm", true, rock2).accent(heat).at(0, 7, 0).rot(-20, 0, 8).mat("rock").glow("cracks").box(-2.5f, 0, -2.5f, 5, 6, 5).add();
        sym(b, "forearm_stone", "forearm", true, rock).at(0, 1, 2.5f).rot(20, 0, 0).mat("rock").box(-2, 0, -0.5f, 4, 4, 2).add();
        sym(b, "fist", "forearm", true, rock).accent(heat).at(0, 6, -0.5f).rot(15, 0, 0).mat("rock").box(-3.5f, 0, -3.5f, 7, 6, 7).add();
        sym(b, "fist_lump", "fist", true, rock2).at(1.5f, 1, -3.5f).rot(0, 0, 20).mat("rock").box(-1.5f, -1, -1, 3, 3, 2).add();
        Kit.Sym kn = sym(b, "knuckles", "fist", true, slag).at(0, 5, -3.5f).mat("rock");
        kn.box(-3.2f, -1, -1, 2, 2, 2).box(-1, -1, -1.2f, 2, 2, 2).box(1.2f, -1, -1, 2, 2, 2);
        kn.add();
        // stubby pillar legs
        sym(b, "leg", null, false, rock).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(heat).at(3.5f, 14, 1).rot(-8, 0, -4).mat("rock")
                .box(-3, -1, -3, 6, 6, 6).add();
        sym(b, "knee", "leg", true, slag).at(0, 4.5f, -2.5f).mat("rock").box(-2, -1.5f, -1, 4, 3, 2).add();
        sym(b, "shin", "leg", true, rock2).accent(heat).at(0, 5, 0).rot(8, 0, 4).mat("rock").glow("cracks").box(-2.5f, 0, -2.5f, 5, 4, 5).add();
        sym(b, "foot", "shin", true, slag).at(0, 3.5f, 0).mat("rock").box(-3.5f, 0, -4.5f, 7, 2, 7).add();
        Kit.Sym tz = sym(b, "toes", "foot", true, rock2).at(0, 1, -4.5f).mat("rock");
        tz.box(-3.2f, -1, -1.5f, 2, 2, 2).box(-1, -1, -1.8f, 2, 2, 2).box(1.2f, -1, -1.5f, 2, 2, 2);
        tz.add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Evil Creature
    // ------------------------------------------------------------------------------------------------

    /**
     * The Evil Creature (The Eyes of Glouphrie): a small floating purple horror - a lumpy, glistening body with a huge
     * central eye, a toothy slit mouth, eyes on stalks, a frill of spines and dangling tentacles.
     */
    static ModelSpec evilCreature(String id) {
        int skin = 0x6A2A6A;
        int dk = 0x3A1240;
        int pale = 0xB070B0;
        int eye = 0xE0FF60;
        ModelSpec.Builder b = ModelSpec.builder(id).scale(0.9f).shadow(0.6f);
        part(b, "body", null, Role.FLOAT, skin).accent(pale).at(0, 12, 0).mat("slime").pattern("spots").box(-4, -5, -3.5f, 8, 9, 7).add();
        part(b, "girth", "body", Role.STATIC, skin).accent(pale).at(0, -0.5f, 0).mat("slime").pattern("spots").box(-5, -3, -4.5f, 10, 6, 9).add();
        sym(b, "wart", "girth", false, pale).at(4, -2, -3).mat("slime").box(0, -0.5f, -0.5f, 2, 1, 1).box(0.5f, 1.5f, 2, 1, 1, 1).add();
        part(b, "dome", "body", Role.STATIC, light(skin, 0.1f)).at(0, -5, 0).mat("slime").box(-3.5f, -2, -3, 7, 2, 6).add();
        sym(b, "lobe", "body", false, dk).at(5, 0, 0).mat("slime").box(-0.5f, -3.5f, -3, 2, 6, 6).add();
        part(b, "underside", "body", Role.STATIC, dk).at(0, 4, 0).mat("slime").box(-4, -0.5f, -3.5f, 8, 2, 7).add();
        part(b, "head", "body", Role.HEAD, skin).accent(eye).at(0, -1, -4.5f).mat("slime").box(-3, -3, -0.5f, 6, 5, 1).add();
        part(b, "big_eye", "head", Role.STATIC, 0xF0F0E0).at(0, -1, -0.6f).mat("bone").box(-2, -1.5f, -0.4f, 4, 3, 1).add();
        part(b, "iris", "big_eye", Role.STATIC, 0xFF5020).at(0, 0, -0.5f).mat("energy").glow("full").box(-1, -1, -0.3f, 2, 2, 1, -0.1f).add();
        part(b, "pupil", "iris", Role.STATIC, 0x100808).at(0, 0, -0.4f).mat("skin").box(-0.5f, -1, -0.3f, 1, 2, 1, -0.2f).add();
        part(b, "lid", "head", Role.STATIC, dk).at(0, -2.8f, -0.8f).rot(20, 0, 0).mat("slime").box(-2.5f, -0.5f, -0.5f, 5, 1, 1).add();
        part(b, "jaw", "head", Role.JAW, dk).accent(0xFF4080).at(0, 2, -0.6f).mat("slime").glow("maw").box(-2.5f, 0, -0.6f, 5, 1, 1).add();
        teeth(b, "teeth", "head", 0xF0E8E0, 0, 2.1f, -1.2f, 4, 4, 1, false);
        for (boolean left : new boolean[]{true, false}) {
            side(part(b, "stalk", "body", Role.TENTACLE, skin).at(3, -4.5f, -2).rot(-20, 0, 40).mat("slime").box(-0.5f, -4, -0.5f, 1, 4, 1), left);
            side(part(b, "stalk_eye", "stalk*", Role.STATIC, eye).at(0, -4, 0).mat("energy").glow("full").box(-1, -1.5f, -1, 2, 2, 2, -0.2f), left);
        }
        for (int i = 0; i < 5; i++) {
            float a = (i - 2) * 24;
            part(b, "spine" + i, "dome", Role.STATIC, i % 2 == 0 ? pale : dk).at((i - 2) * 1.4f, -1.5f, 1.5f).rot(-30, 0, a).mat("horn")
                    .box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        }
        float[][] t = {{3, -2}, {-3, -2}, {3, 2.5f}, {-3, 2.5f}, {0, 0}};
        for (int i = 0; i < t.length; i++) {
            String n = "tentacle" + i;
            part(b, n, "underside", Role.TENTACLE, i % 2 == 0 ? skin : dk).at(t[i][0], 1, t[i][1]).rot(t[i][1] * 4, 0, -t[i][0] * 4).mat("slime")
                    .box(-1, 0, -1, 2, 4, 2).add();
            part(b, n + "_tip", n, Role.STATIC, pale).at(0, 4, 0).rot(t[i][1] * 5, 0, -t[i][0] * 5).mat("slime").box(-0.5f, 0, -0.5f, 1, 3, 1).add();
        }
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Shade
    // ------------------------------------------------------------------------------------------------

    /**
     * A shade of Mort'ton: a floating, hunched wraith of darkness in a deep tattered hood and layered, ragged robes that
     * fray into wisps instead of legs, with pale glowing eyes, a ghostly jaw and long bony fingers.
     */
    static ModelSpec shade(String id) {
        int robe = 0x24222C;
        int robe2 = 0x16141C;
        int edge = 0x3A3646;
        int bone = 0x9A968A;
        int eye = 0xC0F0FF;
        ModelSpec.Builder b = ModelSpec.builder(id).scale(1.0f).shadow(0.4f);
        part(b, "body", null, Role.FLOAT, robe).accent(edge).at(0, 10, 0).rot(14, 0, 0).mat("cloth").box(-4, -11, -2.5f, 8, 11, 5).add();
        part(b, "chest_wrap", "body", Role.STATIC, edge).at(0, -8, -2.5f).rot(-5, 0, 0).mat("cloth").box(-3.5f, -2, -0.6f, 7, 5, 1).add();
        part(b, "sash", "body", Role.STATIC, 0x3A2A3A).at(0, -3, 0).mat("cloth").box(-4.3f, -1, -2.8f, 8, 2, 5, 0.1f).add();
        part(b, "robe", "body", Role.STATIC, robe2).accent(edge).at(0, -1, 0).mat("cloth").box(-4.5f, 0, -3, 9, 5, 6).add();
        part(b, "robe_low", "robe", Role.TAIL, robe).accent(edge).at(0, 5, 0.5f).rot(10, 0, 0).mat("cloth")
                .box(-5, 0, -3, 3, 6, 6).box(-2, 0, -3.3f, 4, 8, 6).box(2, 0, -3, 3, 5, 6).add();
        part(b, "tatters", "robe_low", Role.STATIC, robe2).at(0, 6, -3).mat("ghost").box(-4.5f, 0, -0.5f, 1, 3, 1).box(-1.5f, 2, -0.5f, 1, 3, 1)
                .box(1, 2, -0.5f, 1, 2, 1).box(3, 0, -0.5f, 1, 3, 1).box(-3, 1, 5, 1, 3, 1).box(2, 0, 5, 1, 2, 1).add();
        part(b, "wisp", "robe_low", Role.TAIL, 0x3A3A50).accent(eye).at(0, 8, 1).rot(20, 0, 0).mat("ghost").glow("full").box(-1.5f, 0, -1.5f, 3, 3, 3, -0.3f).add();
        part(b, "wisp_tip", "wisp", Role.STATIC, 0x4A4A66).at(0, 3, 0.5f).rot(20, 0, 0).mat("ghost").glow("full").box(-0.5f, 0, -0.5f, 1, 3, 1, -0.1f).add();
        sym(b, "mantle", "body", false, robe).accent(edge).at(4, -10, 0).rot(0, 0, -20).mat("cloth").box(-2, -1, -3, 4, 3, 6).add();
        sym(b, "mantle_rag", "mantle", true, robe2).at(1.5f, 2, 0).rot(0, 0, 10).mat("cloth").box(-0.5f, 0, -2.5f, 1, 4, 5).add();
        part(b, "cape", "body", Role.STATIC, robe2).accent(edge).at(0, -10, 2.5f).rot(8, 0, 0).mat("cloth").box(-4, 0, 0, 8, 13, 1).add();
        part(b, "cape_rags", "cape", Role.STATIC, robe2).at(0, 13, 0.5f).mat("ghost").box(-3.5f, 0, -0.5f, 2, 3, 1).box(0.5f, 0, -0.5f, 2, 4, 1).add();
        part(b, "hood", "body", Role.STATIC, robe).accent(edge).at(0, -11, 0.5f).rot(-10, 0, 0).mat("cloth").box(-4, -7, -3.5f, 8, 7, 7).add();
        part(b, "hood_tip", "hood", Role.STATIC, robe2).at(0, -6, 3).rot(-45, 0, 0).mat("cloth").box(-1.5f, -2, -1, 3, 3, 3).add();
        part(b, "hood_point", "hood_tip", Role.STATIC, robe2).at(0, -2, 1).rot(-30, 0, 0).mat("cloth").box(-0.5f, -2, -0.5f, 1, 2, 2).add();
        part(b, "head", "hood", Role.HEAD, 0x08080C).accent(eye).at(0, -3, -3.6f).mat("ghost").box(-3, -3, -0.2f, 6, 5, 1).add();
        b.part("eyes", "head", Role.STATIC, eye).pivot(0, -1.4f, -0.4f).material("energy").glow("full")
                .box(0.8f, -0.5f, -0.3f, 2, 1, 1).box(-2.8f, -0.5f, -0.3f, 2, 1, 1);
        part(b, "jaw", "head", Role.JAW, 0x4A4A5A).accent(eye).at(0, 1, -0.4f).mat("ghost").box(-1.5f, 0, -0.3f, 3, 1, 1).add();
        part(b, "hood_rim", "hood", Role.STATIC, edge).at(0, -7, -3.6f).mat("cloth")
                .box(-4, 0, -0.8f, 8, 1, 1).box(-4, 1, -0.8f, 1, 6, 1).box(3, 1, -0.8f, 1, 6, 1).add();
        for (boolean left : new boolean[]{true, false}) {
            side(part(b, "arm", "body", left ? Role.ARM_LEFT : Role.ARM_RIGHT, robe).at(4.5f, -9.5f, 0).rot(-55, 0, -12).mat("cloth")
                    .box(-1.5f, -1, -1.5f, 3, 6, 3), left);
            side(part(b, "sleeve", "arm*", Role.STATIC, robe2).accent(edge).at(0, 5, 0).mat("cloth").box(-2, 0, -2, 4, 3, 4), left);
            side(part(b, "sleeve_rag", "sleeve*", Role.STATIC, robe2).at(0, 3, 1.5f).rot(20, 0, 0).mat("ghost").box(-1.5f, 0, -0.5f, 3, 3, 1), left);
            side(part(b, "hand", "sleeve*", Role.STATIC, bone).at(0, 3, 0).mat("bone").box(-1, 0, -1, 2, 1, 2), left);
            side(part(b, "fingers", "hand*", Role.STATIC, dark(bone, 0.1f)).at(0, 1, -0.5f).rot(35, 0, 0).mat("bone")
                    .box(-1, 0, -0.5f, 1, 3, 1, -0.15f).box(0.2f, 0, -0.5f, 1, 3, 1, -0.15f).box(-0.4f, 0, 0.5f, 1, 2, 1, -0.15f), left);
        }
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Husk
    // ------------------------------------------------------------------------------------------------

    /**
     * A husk of the Nightmare: a gaunt, hunched sleeper drained to a grey shell - sunken ribs under rags, bony limbs,
     * a lolling head with hollow glowing eyes and a slack jaw, and pale sleep-spores clinging to its back.
     */
    static ModelSpec husk(String id) {
        int skin = 0x7A7A6A;
        int dk = 0x4A4A3E;
        int rag = 0x4A463A;
        int rag2 = 0x34322A;
        int spore = 0xD8E0C0;
        int eye = 0xC8F0A0;
        ModelSpec.Builder b = ModelSpec.builder(id).scale(1.0f).shadow(0.5f);
        part(b, "hips", null, Role.BODY, rag2).at(0, 12, 0).mat("cloth").box(-3.5f, -2, -2, 7, 3, 4).add();
        part(b, "torso", "hips", Role.STATIC, skin).accent(dk).at(0, -1.5f, 0).rot(22, 0, 0).mat("skin").box(-3.5f, -10, -2, 7, 10, 4).add();
        for (int i = 0; i < 3; i++) {
            part(b, "rib" + i, "torso", Role.STATIC, light(skin, 0.1f)).at(0, -8 + i * 1.6f, -2).mat("bone").box(-3, -0.5f, -0.4f, 6, 1, 1, -0.15f).add();
        }
        part(b, "sternum_hollow", "torso", Role.STATIC, dk).at(0, -3, -2.1f).mat("skin").box(-2, -1, -0.3f, 4, 2, 1, -0.2f).add();
        part(b, "rag_top", "torso", Role.STATIC, rag).accent(rag2).at(0, -4, 0).mat("cloth").box(-3.8f, -2, -2.3f, 7, 5, 5, 0.1f).add();
        part(b, "rag_tail", "rag_top", Role.STATIC, rag2).at(1, 3, -2.3f).rot(-5, 0, 6).mat("cloth").box(-2, 0, -0.4f, 3, 3, 1).add();
        part(b, "loin_rag", "hips", Role.STATIC, rag).at(0, 1, -2).rot(-6, 0, 0).mat("cloth").box(-3.2f, 0, -0.5f, 6, 4, 1).box(-3.2f, 0, 3.5f, 6, 3, 1).add();
        part(b, "spine", "torso", Role.STATIC, dk).at(0, -9, 2).mat("bone").box(-0.5f, 0, -0.3f, 1, 9, 1).add();
        for (int i = 0; i < 4; i++) {
            part(b, "spore" + i, "torso", Role.STATIC, spore).accent(0xF0FFE0).at((i % 2 == 0 ? 1.5f : -1.5f), -9 + i * 2.2f, 2.2f).rot(i * 20, i * 30, 0)
                    .mat("slime").glow(i % 2 == 0 ? "full" : "none").box(-1, -1, -0.5f, 2, 2, 2).add();
        }
        sym(b, "shoulder", "torso", false, skin).at(3.5f, -9, 0).rot(0, 0, -12).mat("skin").box(-1, -1, -1.5f, 2, 2, 3).add();
        part(b, "neck", "torso", Role.NECK, skin).at(0, -10, -0.5f).rot(-30, 0, 0).mat("skin").box(-1, -2, -1, 2, 3, 2).add();
        part(b, "head", "neck", Role.HEAD, skin).accent(eye).at(0, -2, -0.5f).rot(20, 0, 12).mat("skin").box(-3, -6, -3, 6, 6, 6).add();
        part(b, "brow", "head", Role.STATIC, dk).at(0, -4, -3).rot(10, 0, 0).mat("skin").box(-3, -0.5f, -0.6f, 6, 1, 1).add();
        part(b, "sockets", "head", Role.STATIC, 0x1A1A16).at(0, -3.2f, -3.05f).mat("skin").box(-2.5f, -0.6f, -0.2f, 2, 2, 1, -0.1f).box(0.5f, -0.6f, -0.2f, 2, 2, 1, -0.1f).add();
        b.part("eyes", "head", Role.STATIC, eye).pivot(0, -2.8f, -3.35f).material("energy").glow("full")
                .box(-2, -0.5f, -0.2f, 1, 1, 1, -0.15f).box(1, -0.5f, -0.2f, 1, 1, 1, -0.15f);
        part(b, "jaw", "head", Role.JAW, dk).accent(0x2A2A20).at(0, -1, -2).rot(18, 0, 0).mat("skin").box(-2, 0, -1.5f, 4, 2, 3).add();
        part(b, "mouth", "jaw", Role.STATIC, 0x1A1410).at(0, 0, -1.6f).mat("skin").box(-1.5f, -0.5f, -0.2f, 3, 1, 1, -0.1f).add();
        part(b, "hair", "head", Role.STATIC, 0x5A5648).at(0, -6, 0.5f).mat("hair").box(-3.2f, -0.5f, -2.5f, 6, 1, 5).box(-3.2f, 0, 1.5f, 6, 3, 1).add();
        part(b, "ear_rot", "head", Role.STATIC, dk).at(-3, -3, 0).mat("skin").box(-0.6f, -1, -0.5f, 1, 2, 1).add();
        for (boolean left : new boolean[]{true, false}) {
            side(part(b, "arm", "torso", left ? Role.ARM_LEFT : Role.ARM_RIGHT, skin).at(4.2f, -9, 0).rot(-62, 0, -8).mat("skin")
                    .box(-1, -1, -1, 2, 6, 2), left);
            side(part(b, "elbow", "arm*", Role.STATIC, light(skin, 0.1f)).at(0, 5.5f, 0).mat("bone").box(-1, -0.8f, -1, 2, 2, 2, 0.1f), left);
            side(part(b, "forearm", "arm*", Role.STATIC, skin).at(0, 5.5f, 0).rot(-10, 0, 4).mat("skin").box(-0.75f, 0, -0.75f, 2, 5, 2, -0.25f), left);
            side(part(b, "hand", "forearm*", Role.STATIC, dk).at(0, 5, 0).rot(25, 0, 0).mat("skin").box(-1, 0, -1, 2, 2, 2), left);
            side(part(b, "nails", "hand*", Role.STATIC, 0x2A2A20).at(0, 2, -0.5f).rot(20, 0, 0).mat("horn").box(-1, 0, -0.5f, 1, 2, 1, -0.15f)
                    .box(0.2f, 0, -0.5f, 1, 2, 1, -0.15f), left);
            Role lr = left ? Role.LEG_BACK_LEFT : Role.LEG_BACK_RIGHT;
            side(part(b, "leg", "hips", lr, skin).at(1.8f, 1, 0).rot(-15, 0, -3).mat("skin").box(-1, 0, -1, 2, 6, 2), left);
            side(part(b, "knee", "leg*", Role.STATIC, light(skin, 0.1f)).at(0, 6, 0).mat("bone").box(-1, -1, -1.2f, 2, 2, 2, 0.1f), left);
            side(part(b, "shin", "leg*", Role.STATIC, skin).at(0, 6, 0).rot(22, 0, 3).mat("skin").box(-0.75f, 0, -0.75f, 2, 5, 2, -0.25f), left);
            side(part(b, "foot", "shin*", Role.STATIC, dk).at(0, 5, 0).rot(-7, 0, 0).mat("skin").box(-1, 0, -3, 2, 1, 4), left);
            side(part(b, "rag_wrap", "shin*", Role.STATIC, rag2).at(0, 3, 0).mat("cloth").box(-1.2f, 0, -1.2f, 2, 1, 2, 0.2f), left);
        }
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Imp
    // ------------------------------------------------------------------------------------------------

    /**
     * An imp (Imp Catcher): a small, mischievous red demon with a big-eared, horned head and a wide fanged grin, a little
     * pot belly, spindly clawed limbs, cloven feet and a long tail ending in a barbed spade.
     */
    static ModelSpec imp(String id) {
        int skin = 0xB02A20;
        int dk = 0x6A1410;
        int belly = 0xD86048;
        int horn = 0x2A1A14;
        int eye = 0xFFE040;
        ModelSpec.Builder b = ModelSpec.builder(id).scale(1.0f).shadow(0.35f);
        part(b, "hips", null, Role.BODY, dk).at(0, 17, 0).mat("skin").box(-2, -1.5f, -1.5f, 4, 2, 3).add();
        part(b, "torso", "hips", Role.STATIC, skin).accent(dk).at(0, -1, 0).rot(10, 0, 0).mat("skin").box(-2.5f, -5, -1.5f, 5, 5, 3).add();
        part(b, "belly", "torso", Role.STATIC, belly).at(0, -1.5f, -1.5f).mat("skin").box(-2, -2, -1, 4, 3, 1).add();
        part(b, "back_ridge", "torso", Role.STATIC, dk).at(0, -5, 1.5f).mat("skin").box(-0.5f, 0, -0.3f, 1, 4, 1).add();
        sym(b, "shoulder", "torso", false, skin).at(2.5f, -4.5f, 0).mat("skin").box(-0.5f, -0.5f, -1, 1, 1, 2).add();
        part(b, "neck", "torso", Role.NECK, dk).at(0, -5, -0.5f).rot(-10, 0, 0).mat("skin").box(-1, -1, -1, 2, 1, 2).add();
        part(b, "head", "neck", Role.HEAD, skin).accent(eye).at(0, -1, -0.5f).rot(10, 0, 0).mat("skin").box(-3, -5, -3, 6, 5, 5).add();
        part(b, "snout", "head", Role.STATIC, dark(skin, 0.08f)).at(0, -2, -3).mat("skin").box(-1, -1, -1, 2, 2, 1).add();
        part(b, "brow", "head", Role.STATIC, dk).at(0, -3.6f, -3).rot(20, 0, 0).mat("skin").box(-3, -0.5f, -0.6f, 6, 1, 1).add();
        b.part("eyes", "head", Role.STATIC, eye).pivot(0, -3, -3.2f).material("energy").glow("full")
                .box(0.7f, -0.5f, -0.3f, 2, 1, 1).box(-2.7f, -0.5f, -0.3f, 2, 1, 1);
        part(b, "jaw", "head", Role.JAW, dk).accent(0x2A0A0A).at(0, -0.5f, -2.5f).rot(8, 0, 0).mat("skin").box(-2.5f, 0, -0.8f, 5, 1, 2).add();
        part(b, "grin", "head", Role.STATIC, 0x1A0A0A).at(0, -0.8f, -3.05f).mat("skin").box(-2.5f, -0.5f, -0.2f, 5, 1, 1, -0.1f).add();
        teeth(b, "teeth", "head", 0xF0E8D0, 0, -1, -3.3f, 4, 4, 1, false);
        sym(b, "ear", "head", false, skin).accent(dk).at(3, -2.5f, -0.5f).rot(0, -25, 15).mat("skin").box(0, -1, -0.5f, 2, 2, 1).add();
        sym(b, "ear_tip", "ear", true, dk).at(2, -0.5f, 0).rot(0, 0, -20).mat("skin").box(0, -0.5f, -0.5f, 2, 1, 1).add();
        sym(b, "horn", "head", false, horn).at(1.6f, -5, -1).rot(-25, 0, -20).mat("horn").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        sym(b, "horn_tip", "horn", true, horn).at(0, -2, 0).rot(-30, 0, 15).mat("horn").box(-0.5f, -1, -0.5f, 1, 1, 1, -0.1f).add();
        part(b, "tuft", "head", Role.STATIC, 0x2A0A08).at(0, -5, 0.5f).rot(-30, 0, 0).mat("hair").box(-1, -1, -1, 2, 1, 2).add();
        for (boolean left : new boolean[]{true, false}) {
            side(part(b, "arm", "torso", left ? Role.ARM_LEFT : Role.ARM_RIGHT, skin).at(3, -4.5f, 0).rot(-20, 0, -15).mat("skin")
                    .box(-0.75f, 0, -0.75f, 2, 4, 2, -0.25f), left);
            side(part(b, "forearm", "arm*", Role.STATIC, skin).at(0, 3.5f, 0).rot(-40, 0, 0).mat("skin").box(-0.75f, 0, -0.75f, 2, 3, 2, -0.25f), left);
            side(part(b, "hand", "forearm*", Role.STATIC, dk).at(0, 3, 0).mat("skin").box(-1, 0, -1, 2, 1, 2), left);
            side(part(b, "claws", "hand*", Role.STATIC, horn).at(0, 1, -0.5f).rot(30, 0, 0).mat("horn").box(-1, 0, -0.5f, 1, 2, 1, -0.2f)
                    .box(0.2f, 0, -0.5f, 1, 2, 1, -0.2f), left);
            Role lr = left ? Role.LEG_BACK_LEFT : Role.LEG_BACK_RIGHT;
            side(part(b, "thigh", "hips", lr, skin).at(1.2f, 0, 0).rot(-30, 0, -6).mat("skin").box(-1, 0, -1, 2, 4, 2), left);
            side(part(b, "shin", "thigh*", Role.STATIC, skin).at(0, 3.5f, 0).rot(55, 0, 4).mat("skin").box(-0.75f, 0, -0.75f, 2, 3, 2, -0.25f), left);
            side(part(b, "hoof", "shin*", Role.STATIC, horn).at(0, 2.5f, 0).rot(-25, 0, 0).mat("horn").box(-1, 0, -1.5f, 2, 1, 2), left);
        }
        String tail = chain(b, "tail", "hips", Role.TAIL, skin, dk, "skin", 0, -0.5f, 1.5f, false, new int[]{1, 1, 1, 1},
                new int[]{3, 3, 3, 2}, new float[]{20, -25, -25, -20}, new float[]{0, 10, 10, 10});
        part(b, "tail_spade", tail, Role.STATIC, dk).at(0, 0, 2).rot(0, 0, 45).mat("skin").box(-1, -1, 0, 2, 2, 1).add();
        return b.build();
    }
}
