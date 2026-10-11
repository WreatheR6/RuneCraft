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
import static com.runecraft.osrs.content.bestiary.BestiaryQbm.leg;
import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/**
 * Four-legged and great-ape quest beasts: the hounds (temple guardian, Bouncer, the skeleton hellhound, the witch's
 * experiment), King rat, the plague sheep, the Sourhog, the Shaikahan, the Kendal and Kruk.
 */
final class BestiaryQbmBeasts {
    private BestiaryQbmBeasts() {
    }

    static final int DOG = 0;
    static final int HELL = 1;
    static final int BONE = 2;
    static final int MUTANT = 3;

    static List<ModelSpec> overrides() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(hound("temple_guardian", 1.4f, DOG, 0x7A6248, 0x5A4632, 0xB89C78, 0xF0C040));
        l.add(hound("bouncer", 1.5f, HELL, 0x4A1E18, 0x2A100E, 0x6A2A20, 0xFF7A20));
        l.add(hound("quest_skeleton_hellhound", 1.4f, BONE, 0xD8D0BC, 0xA89E88, 0x8A8070, 0xFF4030));
        l.add(hound("witchs_experiment", 1.4f, MUTANT, 0x8A5A62, 0x5A3A40, 0xC08A86, 0xE04040));
        l.add(rat("rat_king"));
        l.add(sheep("plague_sheep"));
        l.add(kruk("kruk"));
        return l;
    }

    static List<ModelSpec> fresh() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(boar("quest_sourhog"));
        l.add(shaikahan("quest_shaikahan"));
        l.add(kendal("the_kendal"));
        return l;
    }

    // ------------------------------------------------------------------------------------------------
    // Hounds
    // ------------------------------------------------------------------------------------------------

    /**
     * A lean, deep-chested hound: the Priest in Peril temple guardian (a brown mastiff with a studded collar), Bouncer
     * (a hellhound of fissured black-red hide with spikes and a burning maw), the skeleton hellhound (a frame of bones
     * with a ribcage and a skull) and the witch's experiment (a stitched-together mutant with mismatched limbs).
     */
    static ModelSpec hound(String id, float scale, int style, int fur, int dk, int pale, int eye) {
        boolean bone = style == BONE;
        boolean hell = style == HELL;
        boolean mut = style == MUTANT;
        String mat = bone ? "bone" : hell ? "rock" : mut ? "flesh" : "fur";
        String crack = hell ? "cracks" : null;
        int claw = bone ? 0xC8BCA0 : 0xE0D8C0;
        int stitch = 0x2A1A1A;
        ModelSpec.Builder b = ModelSpec.builder(id).scale(scale).shadow(0.9f);
        // torso: deep chest, tucked loin, haunches
        Kit.P chest = part(b, "chest", null, Role.BODY, fur).accent(hell ? eye : pale).at(0, 10, -3).rot(-6, 0, 0).mat(mat);
        if (bone) {
            chest.box(-1, -4.5f, -6, 2, 2, 11); // spine
        } else {
            chest.box(-4.5f, -4.5f, -6, 9, 9, 9);
        }
        if (crack != null) {
            chest.glow(crack);
        }
        chest.add();
        if (bone) {
            for (int i = 0; i < 5; i++) {
                part(b, "rib" + i, "chest", Role.STATIC, i % 2 == 0 ? fur : pale).at(0, -3.5f, -5 + i * 2.2f).rot(0, 0, 0).mat("bone")
                        .box(-4, 0, -0.5f, 1, 6 - i / 2, 1).box(3, 0, -0.5f, 1, 6 - i / 2, 1).box(-4, 6 - i / 2f - 1, -0.5f, 8, 1, 1).add();
            }
            part(b, "sternum", "chest", Role.STATIC, pale).at(0, 3, -4).rot(-10, 0, 0).mat("bone").box(-1, 0, -2, 2, 1, 6).add();
            part(b, "heart", "chest", Role.STATIC, eye).at(0, 0, -2).mat("energy").glow("full").box(-1, -1, -1, 2, 2, 2).add();
        } else {
            part(b, "brisket", "chest", Role.STATIC, pale).accent(dk).at(0, 3.5f, -5.5f).rot(14, 0, 0).mat(mat)
                    .box(-3.5f, -3, -1, 7, 4, 2).add();
            sym(b, "shoulder", "chest", false, fur).accent(dk).at(4, -1.5f, -3.5f).rot(0, 0, -8).mat(mat)
                    .box(-1, -3, -2.5f, 2, 6, 5).add();
        }
        Kit.P loin = part(b, "loin", "chest", Role.STATIC, fur).accent(pale).at(0, -1, 3).rot(8, 0, 0).mat(mat);
        if (bone) {
            loin.box(-1, -3.5f, 0, 2, 2, 8);
        } else {
            loin.box(-3.5f, -3.5f, 0, 7, 6, 8);
        }
        if (crack != null) {
            loin.glow(crack);
        }
        loin.add();
        part(b, "pelvis", "loin", Role.STATIC, bone ? pale : fur).accent(dk).at(0, -1, 7).rot(-4, 0, 0).mat(mat)
                .box(-4, -3, -1, 8, bone ? 3 : 6, 4).add();
        if (!bone) {
            part(b, "belly", "loin", Role.STATIC, mut ? pale : dk).at(0, 2.5f, 1).mat(mat).box(-2.5f, 0, 0, 5, 1, 6).add();
        }
        // back: hackles / spikes / vertebrae
        int n = 5;
        for (int i = 0; i < n; i++) {
            int h = hell ? 3 + (i % 2) : bone ? 1 : mut ? 1 + (i % 3) : 1;
            String m = hell ? "horn" : bone ? "bone" : mut ? "bone" : "fur";
            int c = hell ? 0x3A2A26 : bone ? pale : mut ? 0xD8C8B0 : dk;
            part(b, "hackle" + i, i < 2 ? "chest" : "loin", Role.STATIC, c).at(0, i < 2 ? -4.5f : -3.5f, i < 2 ? -4 + i * 3.5f : (i - 2) * 2.8f)
                    .rot(hell ? -30 : -15, 0, 0).mat(m).box(-1, -h, -1, 2, h, 2).add();
        }
        if (mut) {
            // stitched seams and a grafted growth
            part(b, "seam", "chest", Role.STATIC, stitch).at(0, -4.6f, -2).mat("leather").box(-4.6f, 0, -0.5f, 1, 9, 1, 0.1f)
                    .box(3.6f, 0, 1, 1, 9, 1, 0.1f).add();
            part(b, "growth", "loin", Role.STATIC, 0x6A8A4A).accent(0xA0D060).at(3, -2, 3).rot(0, 0, -20).mat("slime")
                    .box(-1, -2, -2, 3, 3, 4).box(0, -3, -1, 2, 1, 2).add();
        }
        // neck and head
        Kit.P neck = part(b, "neck", "chest", Role.NECK, fur).accent(pale).at(0, -2, -5.5f).rot(-35, 0, 0).mat(mat);
        if (bone) {
            neck.box(-1, -1, -5, 2, 2, 6).box(-1.5f, -1.5f, -3, 3, 3, 1);
        } else {
            neck.box(-3, -3, -5, 6, 6, 6);
        }
        if (crack != null) {
            neck.glow(crack);
        }
        neck.add();
        if (style == DOG || hell) {
            part(b, "collar", "neck", Role.STATIC, hell ? 0x3A3638 : 0x5A3A22).accent(0xB0A890).at(0, 0, -2).mat(hell ? "metal" : "leather")
                    .box(-3.5f, -3.5f, -1, 7, 7, 2).add();
            Kit.P studs = part(b, "studs", "collar", Role.STATIC, hell ? 0x9A9AA0 : 0xC8B070).at(0, 0, 0).mat("metal");
            if (hell) {
                studs.box(-0.5f, -6, -0.5f, 1, 3, 1).box(3, -0.5f, -0.5f, 3, 1, 1).box(-6, -0.5f, -0.5f, 3, 1, 1).box(2.5f, -5, -0.5f, 1, 2, 1)
                        .box(-3.5f, -5, -0.5f, 1, 2, 1);
            } else {
                studs.box(-0.5f, -4, -0.5f, 1, 1, 1).box(3.2f, -0.5f, -0.5f, 1, 1, 1).box(-4.2f, -0.5f, -0.5f, 1, 1, 1).box(-0.5f, 3.2f, -1, 1, 2, 1);
            }
            studs.add();
        }
        String hd = "head";
        Kit.P head = part(b, hd, "neck", Role.HEAD, bone ? pale : fur).accent(eye).at(0, 0, -5).rot(35, 0, 0).mat(mat);
        head.box(-3.5f, -3.5f, -5, 7, 6, 6);
        if (crack != null) {
            head.glow(crack);
        }
        head.add();
        part(b, "muzzle", hd, Role.STATIC, bone ? fur : style == DOG ? dark(fur, 0.15f) : dk).accent(dk).at(0, 0, -5).rot(4, 0, 0).mat(mat)
                .box(-2.5f, -1.5f, -5, 5, 3, 5).add();
        part(b, "nose", "muzzle", Role.STATIC, bone ? 0x1A1410 : 0x1A1212).at(0, -1.5f, -5).mat(bone ? "bone" : "skin")
                .box(-1.5f, -0.5f, -0.6f, 3, 1, 1).add();
        part(b, "brow", hd, Role.STATIC, bone ? fur : dk).accent(dk).at(0, -3, -5).rot(16, 0, 0).mat(mat).box(-3.5f, -1, -1, 7, 2, 2).add();
        if (bone) {
            part(b, "sockets", hd, Role.STATIC, 0x141010).at(0, -1.6f, -5.1f).mat("bone").box(1, -0.8f, -0.4f, 2, 2, 1).box(-3, -0.8f, -0.4f, 2, 2, 1).add();
        }
        b.part("eyes", hd, Role.STATIC, eye).pivot(0, -1.4f, -5.3f).material("energy").glow("full")
                .box(1.2f, -0.5f, -0.4f, 2, 1, 1).box(-3.2f, -0.5f, -0.4f, 2, 1, 1);
        part(b, "jaw", hd, Role.JAW, bone ? fur : dk).accent(eye).at(0, 2, -4).rot(18, 0, 0).mat(mat).glow(hell ? "maw" : "none")
                .box(-2.5f, 0, -6, 5, 2, 6).add();
        sideFangs(b, "fangs", "muzzle", 0xF0E8D0, 2.4f, 1.4f, -4.6f, 2.2f, 2, 1, false);
        teeth(b, "lower_teeth", "jaw", 0xE8E0C8, 0, 0, -5.6f, 4, 3, 1, true);
        if (style == DOG) {
            // floppy mastiff ears and jowls
            sym(b, "ear", hd, false, dk).at(3.4f, -3, -1.5f).rot(0, 0, -30).mat("fur").box(-0.5f, 0, -1.5f, 1, 4, 3).add();
            sym(b, "jowl", "muzzle", false, dark(fur, 0.1f)).at(2.3f, 1, -2.5f).rot(0, 0, -8).mat("fur").box(-0.5f, 0, -2.5f, 1, 2, 5).add();
        } else {
            sym(b, "ear", hd, false, bone ? pale : fur).accent(dk).at(2.6f, -3.5f, -1).rot(-35, -10, -20).mat(mat)
                    .box(-1, -4, -1, 2, 4, 2).box(-0.5f, -5, -0.5f, 1, 1, 1).add();
        }
        if (hell) {
            sym(b, "horn", hd, false, 0x3A2A26).at(2.2f, -3.5f, -2.5f).rot(-50, 0, -15).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        }
        // legs (the experiment's are mismatched: one bony, one bulging)
        int lw = bone ? 2 : 3;
        leg(b, "leg_f", true, 3.4f, -6.5f, lw + (mut ? 1 : 0), 8, lw, 6, 4, 5, 8, -10, fur, bone ? fur : dark(fur, 0.12f), dk, claw,
                mat, crack, 3);
        leg(b, "leg_b", false, 3.4f, 7.5f, lw + 1, 8, lw, 7, 4, 5, -22, 40, fur, bone ? fur : dark(fur, 0.12f), dk, claw, mat, crack, 3);
        if (!bone) {
            sym(b, "haunch", "leg_b", true, fur).accent(dk).at(0.5f, 0, 0).mat(mat).box(-2, -2, -2.5f, 4, 5, 5).add();
        }
        if (mut) {
            part(b, "claw_graft", "leg_f_lo_l", Role.STATIC, 0x4A3A30).at(0, 3, -1).mat("chitin").box(-1.5f, 0, -2.5f, 3, 4, 2).add();
        }
        // tail
        String tail = chain(b, "tail", "pelvis", Role.TAIL, bone ? pale : fur, dk, mat, 0, -1.5f, 3, false,
                bone ? new int[]{2, 1, 1, 1} : new int[]{2, 2, 2, 1}, new int[]{4, 4, 3, 3},
                style == DOG ? new float[]{-40, 15, 15, 10} : new float[]{-20, 12, 14, 16}, null);
        if (hell) {
            part(b, "tail_fire", tail, Role.STATIC, eye).accent(0xFFE070).at(0, 0, 3).mat("lava").glow("full")
                    .box(-1, -1.5f, 0, 2, 2, 2).box(-0.5f, -3, 0.5f, 1, 2, 1).add();
        }
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // King rat
    // ------------------------------------------------------------------------------------------------

    /** King rat (Ratcatchers): a bloated giant rat with mangy fur, a pink naked tail, yellow incisors and a tin crown. */
    static ModelSpec rat(String id) {
        int fur = 0x6A5A4A;
        int dk = 0x3E3428;
        int pink = 0xC88A84;
        int gold = 0xC8A040;
        ModelSpec.Builder b = ModelSpec.builder(id).scale(1.3f).shadow(0.9f);
        part(b, "body", null, Role.BODY, fur).accent(dk).at(0, 15, 1).rot(-4, 0, 0).mat("fur")
                .box(-5, -5, -6, 10, 9, 8).box(-5.5f, -5.5f, 1, 11, 10, 7).add();
        part(b, "rump", "body", Role.STATIC, fur).accent(dk).at(0, -1, 8).rot(10, 0, 0).mat("fur").box(-4.5f, -4, -1, 9, 8, 3).add();
        part(b, "belly", "body", Role.STATIC, 0x9A8670).at(0, 4, 0).mat("fur").box(-4, -0.5f, -5, 8, 1, 11).add();
        for (int i = 0; i < 5; i++) {
            part(b, "mange" + i, "body", Role.STATIC, i % 2 == 0 ? dk : light(fur, 0.1f)).at(i % 2 == 0 ? 1.5f : -2, -5.5f, -4 + i * 2.8f)
                    .rot(-20, (i - 2) * 12, (i % 2 == 0 ? -10 : 10)).mat("fur").box(-1.5f, -1.5f, -1, 3, 2, 2).add();
        }
        sym(b, "scar", "body", false, pink).at(5.3f, -2, 2).rot(0, 0, 0).mat("flesh").box(0, -0.5f, -2, 1, 1, 4, -0.1f).add();
        part(b, "neck", "body", Role.NECK, fur).accent(dk).at(0, -1.5f, -6).rot(-12, 0, 0).mat("fur").box(-3.5f, -3.5f, -3, 7, 7, 4).add();
        part(b, "head", "neck", Role.HEAD, fur).accent(0xE04040).at(0, 0, -3).rot(12, 0, 0).mat("fur").box(-3, -3, -4, 6, 6, 4).add();
        part(b, "snout", "head", Role.STATIC, dark(fur, 0.1f)).at(0, 0.5f, -4).rot(8, 0, 0).mat("fur").box(-2, -1.5f, -4, 4, 3, 4).add();
        part(b, "nose", "snout", Role.STATIC, pink).at(0, -0.5f, -4).mat("flesh").box(-1, -1, -0.8f, 2, 2, 1).add();
        b.part("eyes", "head", Role.STATIC, 0xFF3020).pivot(0, -1.2f, -4.2f).material("energy").glow("full")
                .box(1.4f, -0.5f, -0.4f, 1, 1, 1).box(-2.4f, -0.5f, -0.4f, 1, 1, 1);
        part(b, "jaw", "head", Role.JAW, dk).at(0, 2, -3).rot(10, 0, 0).mat("fur").box(-1.5f, 0, -4, 3, 1, 4).add();
        part(b, "incisors", "snout", Role.STATIC, 0xE8C860).at(0, 1.5f, -3.6f).mat("bone").box(-1, 0, -0.5f, 2, 2, 1).add();
        sym(b, "whiskers", "snout", false, 0xD8D0C0).at(2, 0, -3).rot(0, -20, 10).mat("hair").box(0, 0, -0.5f, 3, 1, 1, -0.3f).add();
        sym(b, "ear", "head", false, fur).accent(pink).at(2.4f, -3, -1).rot(-10, -20, -25).mat("skin").box(-1.5f, -3, -0.5f, 3, 3, 1).add();
        sym(b, "ear_in", "ear", true, pink).at(0, -1.5f, -0.6f).mat("flesh").box(-1, -1, -0.2f, 2, 2, 1, -0.2f).add();
        part(b, "crown", "head", Role.STATIC, gold).accent(0xF0D070).at(0, -3, -1.5f).rot(-8, 0, 0).mat("metal")
                .box(-2.5f, -1.5f, -1.5f, 5, 2, 3).add();
        BestiaryQbm.row(b, "crown_pts", "crown", light(gold, 0.2f), "metal", 0, -2.2f, -1.5f, 4.5f, 3, 1, 2, 1);
        part(b, "crown_gem", "crown", Role.STATIC, 0xC02030).at(0, -0.5f, -1.8f).mat("energy").glow("full").box(-0.5f, -0.5f, -0.4f, 1, 1, 1).add();
        leg(b, "leg_f", true, 3.6f, -4, 3, 5, 2, 4, 3, 4, 8, -14, fur, dk, pink, 0xE0D0C0, "fur", null, 3);
        leg(b, "leg_b", false, 4.2f, 5.5f, 4, 6, 2, 4, 3, 5, -30, 52, fur, dk, pink, 0xE0D0C0, "fur", null, 3);
        chain(b, "tail", "rump", Role.TAIL, pink, dark(pink, 0.2f), "flesh", 0, 1, 1.5f, false,
                new int[]{2, 2, 1, 1, 1}, new int[]{5, 5, 5, 4, 4}, new float[]{-15, 12, 12, 10, 8}, new float[]{0, 15, 15, 10, 10});
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Plague sheep
    // ------------------------------------------------------------------------------------------------

    /** Plague sheep (Sheep Herder): a sickly sheep with matted greenish-grey fleece, sores, a sallow face and thin legs. */
    static ModelSpec sheep(String id) {
        int wool = 0xB8C098;
        int dirty = 0x8A9270;
        int face = 0x6A6A58;
        int sore = 0x9AB040;
        ModelSpec.Builder b = ModelSpec.builder(id).scale(1.1f).shadow(0.8f);
        part(b, "body", null, Role.BODY, wool).accent(dirty).at(0, 12, 0).mat("wool").box(-5, -5, -7, 10, 9, 14).add();
        part(b, "fleece_top", "body", Role.STATIC, wool).accent(dirty).at(0, -5, 0).mat("wool").box(-4, -2, -6, 8, 2, 12).add();
        sym(b, "fleece_side", "body", false, dirty).at(5, 0, 0).mat("wool").box(-0.5f, -3.5f, -5.5f, 1, 6, 11).add();
        part(b, "fleece_rump", "body", Role.STATIC, dirty).at(0, -1, 7).mat("wool").box(-4, -3.5f, -0.5f, 8, 6, 2).add();
        part(b, "fleece_chest", "body", Role.STATIC, wool).at(0, 0, -7).mat("wool").box(-4, -4, -1.5f, 8, 7, 2).add();
        for (int i = 0; i < 12; i++) {
            float x = (i % 3 - 1) * 3.2f;
            part(b, "clump" + i, "fleece_top", Role.STATIC, i % 2 == 0 ? dirty : light(wool, 0.08f)).at(x, -2, -5 + (i / 3) * 3.3f + (i % 2))
                    .rot(0, i * 30, 0).mat("wool").box(-1.5f, -1, -1.5f, 3, 1, 3).add();
        }
        part(b, "sores", "body", Role.STATIC, sore).accent(0xD0F060).at(0, 0, 0).mat("slime").glow("full")
                .box(5.2f, -2, -3, 1, 2, 2, -0.2f).box(-6.2f, 0, 2, 1, 2, 2, -0.2f).box(1, -7.6f, 3, 2, 1, 2, -0.2f).add();
        part(b, "neck", "body", Role.NECK, wool).at(0, -2, -7).rot(-30, 0, 0).mat("wool").box(-2.5f, -2.5f, -3, 5, 5, 3).add();
        part(b, "head", "neck", Role.HEAD, face).accent(sore).at(0, -1, -3).rot(40, 0, 0).mat("skin").pattern("eyes_animal")
                .box(-2.5f, -3, -4, 5, 5, 4).add();
        part(b, "muzzle", "head", Role.STATIC, dark(face, 0.15f)).at(0, 0.5f, -4).mat("skin").box(-1.5f, -1, -2, 3, 3, 2).add();
        part(b, "jaw", "head", Role.JAW, dark(face, 0.25f)).at(0, 2, -4).mat("skin").box(-1.5f, 0, -2, 3, 1, 2).add();
        part(b, "drool", "jaw", Role.STATIC, sore).at(0.5f, 1, -1.5f).mat("slime").glow("full").box(-0.5f, 0, -0.5f, 1, 2, 1, -0.2f).add();
        part(b, "poll", "head", Role.STATIC, wool).at(0, -3, -1).mat("wool").box(-2.5f, -1.5f, -2, 5, 2, 4).add();
        sym(b, "ear", "head", false, face).at(2.5f, -2, -1).rot(0, -10, -60).mat("skin").box(0, -0.5f, -1, 3, 1, 2).add();
        b.part("eyes", "head", Role.STATIC, 0xC8E040).pivot(0, -1, -4.1f).material("energy").glow("full")
                .box(1, -0.5f, -0.4f, 1, 1, 1).box(-2, -0.5f, -0.4f, 1, 1, 1);
        leg(b, "leg_f", true, 3, -5, 3, 5, 2, 6, 2, 2, 4, -6, wool, face, 0x2A2420, 0x2A2420, "skin", null, 0);
        leg(b, "leg_b", false, 3, 5, 3, 5, 2, 6, 2, 2, -10, 16, wool, face, 0x2A2420, 0x2A2420, "skin", null, 0);
        sym(b, "leg_f_fleece", "leg_f", true, dirty).at(0, 0.5f, 0).mat("wool").box(-2, -1.5f, -2, 4, 3, 4).add();
        sym(b, "leg_b_fleece", "leg_b", true, dirty).at(0, 0.5f, 0).mat("wool").box(-2, -1.5f, -2, 4, 3, 4).add();
        part(b, "flies", "body", Role.FLOAT, 0x1A1A14).at(0, -9, -2).mat("hide").box(-3, 0, 0, 1, 1, 1, -0.3f).box(2, -1, 2, 1, 1, 1, -0.3f)
                .box(0, -2, -1, 1, 1, 1, -0.3f).add();
        part(b, "tail", "fleece_rump", Role.TAIL, wool).at(0, -2, 1).rot(30, 0, 0).mat("wool").box(-1, 0, 0, 2, 4, 2).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Sourhog
    // ------------------------------------------------------------------------------------------------

    /** The Sourhog (A Porcine of Interest): a huge, bristle-backed wild boar with curling tusks and a sour green drool. */
    static ModelSpec boar(String id) {
        int hide = 0x5A4434;
        int dk = 0x2E2219;
        int bristle = 0x2A2018;
        int pink = 0x9A6A5A;
        ModelSpec.Builder b = ModelSpec.builder(id).scale(1.0f).shadow(1.1f);
        part(b, "body", null, Role.BODY, hide).accent(dk).at(0, 11, 0).rot(-6, 0, 0).mat("hide")
                .box(-6, -6, -9, 12, 11, 10).box(-5, -5, 1, 10, 9, 8).add();
        part(b, "hump", "body", Role.STATIC, hide).accent(dk).at(0, -6, -5).rot(-10, 0, 0).mat("hide").box(-4.5f, -2.5f, -4, 9, 3, 8).add();
        part(b, "belly", "body", Role.STATIC, pink).at(0, 5, -1).mat("skin").box(-4, -0.5f, -7, 8, 1, 13).add();
        int[] br = {3, 4, 4, 3, 3, 2, 2};
        for (int i = 0; i < br.length; i++) {
            part(b, "bristle" + i, i < 3 ? "hump" : "body", Role.STATIC, i % 2 == 0 ? bristle : dk).at(0, i < 3 ? -2.5f : -5, i < 3 ? -3 + i * 3 : -1 + (i - 3) * 2.6f)
                    .rot(-25, 0, 0).mat("hair").box(-1.5f, -br[i], -0.5f, 3, br[i], 2).add();
        }
        sym(b, "flank", "body", false, dark(hide, 0.1f)).accent(dk).at(6, -1, -2).rot(0, 0, -5).mat("hide").box(-0.5f, -4, -5, 1, 8, 10).add();
        part(b, "neck", "body", Role.NECK, hide).accent(dk).at(0, -1, -9).rot(10, 0, 0).mat("hide").box(-4.5f, -4.5f, -3, 9, 9, 4).add();
        part(b, "head", "neck", Role.HEAD, hide).accent(0xE0B040).at(0, 0.5f, -3).rot(14, 0, 0).mat("hide").box(-3.5f, -4, -5, 7, 7, 5).add();
        part(b, "snout", "head", Role.STATIC, dark(hide, 0.05f)).at(0, 1, -5).rot(4, 0, 0).mat("hide").box(-2.5f, -2, -4, 5, 4, 4).add();
        part(b, "disc", "snout", Role.STATIC, pink).at(0, 0, -4).mat("skin").box(-2, -1.5f, -0.8f, 4, 3, 1).add();
        part(b, "nostrils", "disc", Role.STATIC, 0x2A1A18).at(0, 0, -0.8f).mat("skin").box(-1.2f, -0.5f, -0.3f, 1, 1, 1, -0.2f).box(0.2f, -0.5f, -0.3f, 1, 1, 1, -0.2f).add();
        part(b, "jaw", "head", Role.JAW, dk).at(0, 2.5f, -4).rot(8, 0, 0).mat("hide").box(-2.5f, 0, -4, 5, 2, 4).add();
        part(b, "drool", "jaw", Role.STATIC, 0x9AD040).at(1.2f, 1.5f, -3).mat("slime").glow("full").box(-0.5f, 0, -0.5f, 1, 3, 1, -0.2f).add();
        sym(b, "tusk", "jaw", false, 0xE8DCC0).at(2.3f, 0, -3.5f).rot(-20, 0, 10).mat("horn").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        sym(b, "tusk_curl", "tusk", true, 0xD8CCA8).at(0, -2, 0).rot(-50, 0, 10).mat("horn").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        b.part("eyes", "head", Role.STATIC, 0xE0B040).pivot(0, -2, -5.2f).material("energy").glow("full")
                .box(1.4f, -0.5f, -0.4f, 1, 1, 1).box(-2.4f, -0.5f, -0.4f, 1, 1, 1);
        part(b, "brow", "head", Role.STATIC, dk).at(0, -3, -5).rot(20, 0, 0).mat("hair").box(-3.5f, -1, -1, 7, 2, 2).add();
        sym(b, "ear", "head", false, hide).accent(pink).at(3, -4, -1).rot(-20, -10, -35).mat("hide").box(-1, -3, -0.5f, 2, 3, 1).box(-0.5f, -4, -0.5f, 1, 1, 1).add();
        leg(b, "leg_f", true, 4, -6, 4, 5, 3, 6, 3, 3, 4, -6, hide, dk, 0x1A1410, 0x1A1410, "hide", null, 2);
        leg(b, "leg_b", false, 3.8f, 6, 4, 6, 3, 5, 3, 3, -16, 26, hide, dk, 0x1A1410, 0x1A1410, "hide", null, 2);
        sym(b, "ham", "leg_b", true, hide).accent(dk).at(0, 0, 0).mat("hide").box(-2.5f, -2.5f, -3, 5, 5, 6).add();
        chain(b, "tail", "body", Role.TAIL, hide, dk, "hide", 0, -3, 9, false, new int[]{1, 1, 1}, new int[]{2, 2, 2},
                new float[]{40, 30, -40}, null);
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Shaikahan
    // ------------------------------------------------------------------------------------------------

    /**
     * The Shaikahan (Tai Bwo Wannai Trio): a sinewy jungle predator, part big cat and part lizard - a striped tawny hide,
     * a frilled crest, sweeping horns, heavy clawed forelimbs and a long whipping tail.
     */
    static ModelSpec shaikahan(String id) {
        int hide = 0xA0783A;
        int dk = 0x5A3E1E;
        int stripe = 0x3A2412;
        int pale = 0xD8C090;
        int eye = 0xE8E040;
        ModelSpec.Builder b = ModelSpec.builder(id).scale(1.0f).shadow(1.0f);
        part(b, "chest", null, Role.BODY, hide).accent(stripe).at(0, 11, -3).rot(-8, 0, 0).mat("hide").pattern("stripes")
                .box(-5, -5, -6, 10, 10, 9).add();
        part(b, "loin", "chest", Role.STATIC, hide).accent(stripe).at(0, -0.5f, 3).rot(8, 0, 0).mat("hide").pattern("stripes")
                .box(-4, -4, 0, 8, 7, 9).add();
        part(b, "belly", "chest", Role.STATIC, pale).at(0, 5, -1).mat("hide").box(-3.5f, -0.5f, -5, 7, 1, 13).add();
        sym(b, "shoulder", "chest", false, hide).accent(stripe).at(4.5f, -1, -3.5f).rot(0, 0, -8).mat("hide").pattern("stripes")
                .box(-1, -4, -3, 2, 8, 6).add();
        BestiaryQbm.spines(b, "crest", "chest", 0x8A3A1A, 0xB0582A, "membrane", -5, -5, 2.6f, new int[]{3, 4, 4, 3}, -20, 1);
        BestiaryQbm.spines(b, "dorsal", "loin", stripe, dk, "horn", -4, 1, 2.6f, new int[]{2, 2, 1}, -30, 2);
        part(b, "neck", "chest", Role.NECK, hide).accent(stripe).at(0, -2, -6).rot(-20, 0, 0).mat("hide").box(-3, -3, -4, 6, 6, 5).add();
        part(b, "head", "neck", Role.HEAD, hide).accent(eye).at(0, 0, -4).rot(20, 0, 0).mat("hide").box(-3.5f, -3.5f, -4, 7, 6, 5).add();
        part(b, "muzzle", "head", Role.STATIC, pale).at(0, 0.5f, -4).rot(5, 0, 0).mat("hide").box(-2.5f, -1.5f, -4, 5, 3, 4).add();
        part(b, "nose", "muzzle", Role.STATIC, 0x2A1A14).at(0, -1.5f, -4).mat("skin").box(-1, -0.5f, -0.6f, 2, 1, 1).add();
        b.part("eyes", "head", Role.STATIC, eye).pivot(0, -1.6f, -4.2f).material("energy").glow("full")
                .box(1.2f, -0.5f, -0.4f, 2, 1, 1).box(-3.2f, -0.5f, -0.4f, 2, 1, 1);
        part(b, "jaw", "head", Role.JAW, dk).at(0, 2, -3).rot(14, 0, 0).mat("hide").box(-2.5f, 0, -5, 5, 2, 5).add();
        sideFangs(b, "fangs", "muzzle", 0xF0E8D0, 2.4f, 1.4f, -3.6f, 2, 1, 2, false);
        sym(b, "frill", "head", false, 0x8A3A1A).accent(0xD0702A).at(3.5f, -1, -0.5f).rot(0, -35, -10).mat("membrane")
                .box(0, -3.5f, -0.5f, 3, 6, 1).add();
        sym(b, "horn", "head", false, 0x3A2A1E).at(2.5f, -3.5f, -1).rot(-60, -20, -20).mat("horn").box(-1, -4, -1, 2, 4, 2).add();
        sym(b, "horn_tip", "horn", true, 0xC8B890).at(0, -4, 0).rot(-30, 0, 0).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        leg(b, "leg_f", true, 4, -5.5f, 4, 7, 3, 6, 5, 5, 6, -12, hide, dk, dk, 0xE8E0C8, "hide", null, 4);
        leg(b, "leg_b", false, 3.6f, 9, 4, 7, 3, 6, 4, 5, -26, 46, hide, dk, dk, 0xE8E0C8, "hide", null, 3);
        chain(b, "tail", "loin", Role.TAIL, hide, stripe, "hide", 0, -2, 8.5f, false, new int[]{3, 3, 2, 2, 1},
                new int[]{4, 4, 4, 4, 4}, new float[]{-25, 10, 10, 12, 14}, new float[]{0, 10, 10, -10, -10});
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // The Kendal
    // ------------------------------------------------------------------------------------------------

    /**
     * The Kendal (Mountain Daughter): a towering, upright bear-like brute of shaggy brown fur, hunched with a heavy ruff,
     * long clawed arms, a bear's snout with bared fangs and small furious eyes.
     */
    static ModelSpec kendal(String id) {
        int fur = 0x6A4A30;
        int dk = 0x3E2A1A;
        int pale = 0xA8865E;
        int claw = 0xE8E0C8;
        ModelSpec.Builder b = ModelSpec.builder(id).scale(1.2f).shadow(1.0f);
        part(b, "hips", null, Role.BODY, fur).accent(dk).at(0, 12, 1).mat("fur").box(-5, -3, -3.5f, 10, 5, 7).add();
        part(b, "torso", "hips", Role.STATIC, fur).accent(dk).at(0, -2, 0).rot(18, 0, 0).mat("fur").box(-6.5f, -12, -4.5f, 13, 12, 9).add();
        part(b, "belly", "torso", Role.STATIC, pale).at(0, -4, -4.5f).rot(-6, 0, 0).mat("fur").box(-4, -6, -1, 8, 7, 2).add();
        part(b, "ruff", "torso", Role.STATIC, dk).accent(fur).at(0, -11, -1).rot(-10, 0, 0).mat("fur").box(-7.5f, -2.5f, -5, 15, 5, 10).add();
        for (int i = 0; i < 5; i++) {
            float x = (i - 2) * 3f;
            part(b, "shag" + i, "ruff", Role.STATIC, i % 2 == 0 ? fur : dk).at(x, 2, -5).rot(10, 0, (i - 2) * 6).mat("fur")
                    .box(-1.5f, 0, -0.5f, 3, 3 + (i % 2), 2).add();
        }
        part(b, "hump", "torso", Role.STATIC, fur).at(0, -12, 2).rot(-20, 0, 0).mat("fur").box(-5, -3, -3, 10, 4, 7).add();
        part(b, "head", "torso", Role.HEAD, fur).accent(0xFF5020).at(0, -12, -3.5f).rot(-14, 0, 0).mat("fur").box(-4, -6, -5, 8, 7, 6).add();
        part(b, "snout", "head", Role.STATIC, pale).at(0, -1.5f, -5).rot(6, 0, 0).mat("fur").box(-2.5f, -2, -4, 5, 3, 4).add();
        part(b, "nose", "snout", Role.STATIC, 0x1A1210).at(0, -2, -4).mat("skin").box(-1.5f, -0.5f, -0.6f, 3, 2, 1).add();
        part(b, "brow", "head", Role.STATIC, dk).at(0, -4.5f, -5).rot(14, 0, 0).mat("fur").box(-4, -1, -1, 8, 2, 2).add();
        b.part("eyes", "head", Role.STATIC, 0xFF5020).pivot(0, -3.4f, -5.3f).material("energy").glow("full")
                .box(1.3f, -0.5f, -0.4f, 1, 1, 1).box(-2.3f, -0.5f, -0.4f, 1, 1, 1);
        part(b, "jaw", "head", Role.JAW, dk).at(0, 0.5f, -4).rot(16, 0, 0).mat("fur").box(-2.5f, 0, -4, 5, 2, 4).add();
        sideFangs(b, "fangs", "snout", 0xF0E8D0, 2.4f, 1, -3.6f, 2, 2, 2, false);
        teeth(b, "lower_teeth", "jaw", 0xE8E0C8, 0, 0, -3.6f, 4, 3, 1, true);
        sym(b, "ear", "head", false, fur).accent(dk).at(3.2f, -6, -1.5f).rot(0, -15, -20).mat("fur").box(-1.5f, -2, -1, 3, 2, 2).add();
        sym(b, "cheek", "head", false, fur).at(4, -1, -3).rot(0, -15, 0).mat("fur").box(-1, -2, -1.5f, 2, 4, 3).add();
        // long, heavy arms
        sym(b, "arm", "torso", false, fur).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(dk).at(7.5f, -10, -0.5f).rot(-28, 0, -12).mat("fur")
                .box(-2.5f, -2, -3, 5, 10, 6).add();
        sym(b, "shoulder_fur", "arm", true, dk).at(0.5f, -2, 0).mat("fur").box(-3, -2, -3.5f, 6, 4, 7).add();
        sym(b, "forearm", "arm", true, fur).accent(dk).at(0, 7.5f, 0).rot(-30, 0, 6).mat("fur").box(-2.5f, 0, -2.5f, 5, 9, 5).add();
        sym(b, "fur_tuft", "forearm", true, dk).at(0, 2, 2.5f).rot(30, 0, 0).mat("fur").box(-2, 0, -0.5f, 4, 4, 2).add();
        sym(b, "paw", "forearm", true, dk).at(0, 9, -0.5f).rot(20, 0, 0).mat("fur").box(-3, 0, -3, 6, 3, 5).add();
        Kit.Sym cl = sym(b, "claws", "paw", true, claw).at(0, 2, -3).rot(30, 0, 0).mat("horn");
        for (int k = 0; k < 4; k++) {
            cl.box(-2.7f + k * 1.5f, 0, -0.5f, 1, 3, 1);
        }
        cl.add();
        // short bowed legs
        sym(b, "leg", null, false, fur).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(dk).at(3.5f, 12.5f, 1).rot(-16, 0, -4).mat("fur")
                .box(-3, -1, -3, 6, 7, 6).add();
        sym(b, "shin", "leg", true, fur).at(0, 5.5f, 0).rot(24, 0, 4).mat("fur").box(-2.5f, 0, -2.5f, 5, 6, 5).add();
        sym(b, "foot", "shin", true, dk).at(0, 5, 0).rot(-8, 0, 0).mat("fur").box(-3, 0, -5, 6, 2, 7).add();
        Kit.Sym tc = sym(b, "toe_claws", "foot", true, claw).at(0, 1, -5).mat("horn");
        for (int k = 0; k < 3; k++) {
            tc.box(-2.5f + k * 2, -0.5f, -1.5f, 1, 1, 2);
        }
        tc.add();
        part(b, "stub_tail", "hips", Role.TAIL, dk).at(0, -1, 3.5f).rot(30, 0, 0).mat("fur").box(-1.5f, -1, 0, 3, 3, 2).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Kruk
    // ------------------------------------------------------------------------------------------------

    /**
     * Kruk (Monkey Madness II): a massive, knuckle-walking ape of the Gorilla guard breed - a silver-backed brown giant
     * with a crested skull, heavy brow, bared fangs, and battered leather bracers and belt.
     */
    static ModelSpec kruk(String id) {
        int fur = 0x5A3A26;
        int dk = 0x2E1E14;
        int silver = 0x9A9284;
        int face = 0x3E3A36;
        int leather = 0x6A4A2A;
        ModelSpec.Builder b = ModelSpec.builder(id).scale(2.0f).shadow(1.1f);
        part(b, "hips", null, Role.BODY, fur).accent(dk).at(0, 12, 4).mat("fur").box(-5, -3, -3.5f, 10, 6, 7).add();
        part(b, "belt", "hips", Role.STATIC, leather).accent(0xC8B070).at(0, -1, 0).mat("leather").box(-5.5f, -1.5f, -4, 11, 3, 8, 0.1f).add();
        part(b, "buckle", "belt", Role.STATIC, 0xC8B070).at(0, 0, -4).mat("metal").box(-1, -1, -0.6f, 2, 2, 1).add();
        part(b, "loincloth", "belt", Role.STATIC, 0x7A2A20).at(0, 1.5f, -4).rot(-6, 0, 0).mat("cloth").box(-2, 0, -0.5f, 4, 5, 1).add();
        part(b, "torso", "hips", Role.STATIC, fur).accent(dk).at(0, -2, -1).rot(40, 0, 0).mat("fur").box(-7, -13, -5, 14, 13, 10).add();
        part(b, "chest", "torso", Role.STATIC, face).at(0, -7, -5).rot(-4, 0, 0).mat("skin").box(-5.5f, -5, -1, 11, 7, 2).add();
        part(b, "saddle", "torso", Role.STATIC, silver).accent(fur).at(0, -6, 5).rot(0, 0, 0).mat("fur").box(-6, -6, -0.5f, 12, 9, 1).add();
        part(b, "back", "torso", Role.STATIC, silver).at(0, -13, 0).mat("fur").box(-6, -1.5f, -3, 12, 2, 8).add();
        sym(b, "trap", "torso", false, fur).accent(dk).at(4.5f, -12, -1).rot(0, 0, -20).mat("fur").box(-3, -3, -3.5f, 6, 4, 7).add();
        part(b, "head", "torso", Role.HEAD, face).accent(0xE0A040).at(0, -10, -6).rot(-40, 0, 0).mat("skin").box(-4, -5, -4, 8, 7, 5).add();
        part(b, "crest", "head", Role.STATIC, fur).at(0, -5, 0).rot(-10, 0, 0).mat("fur").box(-2.5f, -3, -3, 5, 3, 6).add();
        part(b, "scalp", "head", Role.STATIC, fur).at(0, -4, 1).mat("fur").box(-4.5f, -2, -2, 9, 5, 4).add();
        part(b, "brow", "head", Role.STATIC, dark(face, 0.25f)).at(0, -3, -4).rot(10, 0, 0).mat("skin").box(-4.5f, -1, -1.5f, 9, 2, 2).add();
        b.part("eyes", "head", Role.STATIC, 0xE8A030).pivot(0, -1.6f, -4.2f).material("energy").glow("full")
                .box(1, -0.5f, -0.4f, 2, 1, 1).box(-3, -0.5f, -0.4f, 2, 1, 1);
        part(b, "muzzle", "head", Role.STATIC, dark(face, 0.1f)).at(0, 0, -4).rot(0, 0, 0).mat("skin").box(-3, -1, -2, 6, 3, 2).add();
        part(b, "nostrils", "muzzle", Role.STATIC, 0x141010).at(0, -0.5f, -2).mat("skin").box(-1.5f, -0.5f, -0.3f, 1, 1, 1, -0.1f).box(0.5f, -0.5f, -0.3f, 1, 1, 1, -0.1f).add();
        part(b, "jaw", "head", Role.JAW, dark(face, 0.2f)).at(0, 2, -3).rot(14, 0, 0).mat("skin").box(-3, 0, -3, 6, 2, 4).add();
        sideFangs(b, "fangs", "jaw", 0xF0E8D0, 2.8f, 0, -2.6f, 1, 1, 2, true);
        sym(b, "ear", "head", false, face).at(4, -2, -1).mat("skin").box(-0.5f, -1.5f, -1, 1, 3, 2).add();
        // huge knuckle-walking arms with bracers
        sym(b, "arm", null, false, fur).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(dk).at(10, 3, -6).rot(-16, 0, -18).mat("fur")
                .box(-3.5f, -2, -3.5f, 7, 11, 7).add();
        sym(b, "shoulder", "arm", true, fur).accent(dk).at(0, -1, 0).mat("fur").box(-4, -3, -4, 8, 5, 8).add();
        sym(b, "forearm", "arm", true, fur).accent(dk).at(0, 9, 0).rot(4, 0, 12).mat("fur").box(-3.5f, 0, -3.5f, 7, 9, 7).add();
        sym(b, "bracer", "forearm", true, leather).accent(0xC8B070).at(0, 5, 0).mat("leather").box(-3.8f, 0, -3.8f, 7, 3, 7, 0.3f).add();
        sym(b, "bracer_studs", "bracer", true, 0xC8B070).at(0, 1.5f, -4.2f).mat("metal").box(-2.5f, -0.5f, -0.3f, 1, 1, 1).box(1.5f, -0.5f, -0.3f, 1, 1, 1).add();
        sym(b, "hand", "forearm", true, face).at(0, 9, -0.5f).rot(16, 0, 8).mat("skin").box(-4, 0, -4, 8, 3, 7).add();
        Kit.Sym kn = sym(b, "knuckles", "hand", true, dark(face, 0.25f)).at(0, 2.5f, -4).rot(20, 0, 0).mat("skin");
        for (int k = 0; k < 4; k++) {
            kn.box(-3.8f + k * 2, -1, -1.5f, 2, 2, 2);
        }
        kn.add();
        // short bowed legs
        sym(b, "leg", null, false, fur).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(dk).at(4.5f, 13, 5).rot(-26, 0, -8).mat("fur")
                .box(-3, -2, -3, 6, 7, 6).add();
        sym(b, "shin", "leg", true, fur).at(0, 4.5f, 0).rot(36, 0, 4).mat("fur").box(-2.5f, 0, -2.5f, 5, 6, 5).add();
        sym(b, "foot", "shin", true, face).at(0, 5, 0).rot(-10, 0, 4).mat("skin").box(-3, 0, -5, 6, 2, 7).add();
        Kit.Sym tz = sym(b, "toes", "foot", true, dark(face, 0.25f)).at(0, 1, -5).mat("skin");
        for (int k = 0; k < 3; k++) {
            tz.box(-2.8f + k * 2, -0.8f, -1.5f, 2, 2, 2);
        }
        tz.add();
        return b.build();
    }
}
