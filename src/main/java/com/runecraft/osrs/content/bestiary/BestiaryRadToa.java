package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.BestiaryRad.eyes;
import static com.runecraft.osrs.content.bestiary.BestiaryRad.p;
import static com.runecraft.osrs.content.bestiary.BestiaryRad.s;
import static com.runecraft.osrs.content.bestiary.BestiaryRad.shade;
import static com.runecraft.osrs.content.bestiary.BestiaryRad.teeth;

/** Tombs of Amascut: Kephri and her scarabs, Akkha, Ba-Ba and the baboons, Zebak, the Wardens and their obelisk. */
final class BestiaryRadToa {
    private BestiaryRadToa() {
    }

    static final int GOLD = 0xD8A838;
    static final int LAPIS = 0x2A4A9A;
    static final int TOOTH = 0xEDE4CC;

    static List<ModelSpec> all() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(scarab("kephri", 0x2A5A7A, 1.0f, true));
        l.add(scarab("toa_scarab", 0x3A4A5A, 0.45f, false));
        l.add(akkha());
        l.add(baboon("baba", 0x9A7A5A, 1.0f, true));
        l.add(baboon("toa_baboon", 0x8A6A4A, 0.6f, false));
        l.add(boulder());
        l.add(zebak());
        l.add(obelisk());
        l.add(warden("elidinis_warden", 0x3AA0A8, 0x1A5A60, 0x70F0F0));
        l.add(warden("tumekens_warden", 0xD8A030, 0x8A5A10, 0xFFE070));
        return l;
    }

    // ------------------------------------------------------------------------------------------
    // Kephri and her scarabs
    // ------------------------------------------------------------------------------------------

    /**
     * A sacred scarab: domed, gold-rimmed wing cases over a segmented body, a horned shovel head, mandibles and six
     * toothed legs. Kephri herself carries a blazing sun disc between her horns and lapis-inlaid wing cases.
     */
    static ModelSpec scarab(String id, int shell, float scale, boolean queen) {
        int body = 0x1A2632;
        int dark = 0x101820;
        ModelSpec.Builder b = ModelSpec.builder(id).scale(scale).shadow(1.3f * Math.max(scale, 0.5f));
        p(b, "body", null, Role.BODY, body).accent(GOLD).at(0, 15, 0).mat("chitin").box(-7, -4, -10, 14, 7, 20).add();
        p(b, "belly", "body", dark).at(0, 3, 0).mat("chitin").box(-6, -0.5f, -9, 12, 2, 18).add();
        p(b, "pronotum", "body", shell).accent(GOLD).at(0, -4, -9).rot(8, 0, 0).mat("chitin").box(-7.5f, -3, -1, 15, 5, 7).add();
        p(b, "pronotum_rim", "pronotum", GOLD).at(0, -3, -1).mat("metal").box(-7.5f, -0.5f, -0.5f, 15, 1, 1).add();
        // wing cases (open slightly) with gold rims and a lapis stripe
        s(b, "elytron", "body", false, shell).roles(Role.WING_LEFT, Role.WING_RIGHT).accent(GOLD).at(0.3f, -4, -3).rot(-4, 0, -6)
                .mat("chitin").box(0, -4, 0, 8, 5, 14).box(0.5f, -4.5f, 1, 6, 1, 12).add();
        s(b, "elytron_rim", "elytron", true, GOLD).at(0, 0, 0).mat("metal").box(7.6f, -3, 0, 1, 4, 14).box(0, -3, 13.6f, 8, 4, 1).add();
        s(b, "elytron_stripe", "elytron", true, queen ? LAPIS : shade(shell, 1.3f)).at(0, -4.6f, 0).mat("metal")
                .box(3, -0.5f, 2, 2, 1, 10).add();
        p(b, "seam", "body", GOLD).at(0, -8.6f, 4).mat("metal").box(-0.5f, 0, -7, 1, 1, 14).add();
        // head: shovel clypeus, horn, eyes, mandibles
        p(b, "head", "body", Role.HEAD, shell).accent(GOLD).at(0, -1, -10).mat("chitin").box(-5, -3, -5, 10, 5, 5).add();
        p(b, "clypeus", "head", shell).at(0, 0, -5).rot(15, 0, 0).mat("chitin").box(-6, -1, -3, 12, 2, 3).add();
        p(b, "clypeus_teeth", "clypeus", GOLD).at(0, 0, -3).mat("metal").box(-5, -0.5f, -1, 2, 1, 1).box(-1, -0.5f, -1, 2, 1, 1)
                .box(3, -0.5f, -1, 2, 1, 1).add();
        eyes(b, "eye", "head", queen ? 0xFFD040 : 0xE05030, 4.2f, -1.5f, -3, 2, 2);
        p(b, "horn", "head", shell).accent(GOLD).at(0, -3, -3).rot(-30, 0, 0).mat("chitin").box(-1, -5, -1, 2, 5, 2).add();
        p(b, "horn_tip", "horn", GOLD).at(0, -5, 0).rot(-30, 0, 0).mat("metal").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        s(b, "mandible", "head", false, 0x8A6A20).roles(Role.JAW, Role.JAW).at(2.5f, 1.5f, -5).rot(0, 25, 0).mat("horn")
                .box(-0.5f, 0, -4, 2, 1, 4).box(-1, 0, -4.5f, 1, 1, 2).add();
        // six toothed legs
        float[] zs = {-7, -1, 5};
        float[] yaws = {-35, 0, 30};
        for (int i = 0; i < 3; i++) {
            String n = "leg" + i;
            s(b, n, "body", false, body).roles(Role.LEG_EXTRA, Role.LEG_EXTRA).at(7, 1, zs[i]).rot(0, yaws[i], -15).mat("chitin")
                    .box(0, -1, -1.5f, 6, 2, 3).add();
            s(b, n + "_shin", n, true, shell).at(6, 0, 0).rot(0, 0, -15).mat("chitin").box(-1, 0, -1, 2, 7, 2)
                    .box(-1.5f, 2, -0.5f, 1, 1, 1).box(-1.5f, 4, -0.5f, 1, 1, 1).add();
            s(b, n + "_tarsus", n + "_shin", true, GOLD).at(0, 7, 0).rot(0, 0, 30).mat("metal").box(-0.5f, 0, -0.5f, 1, 3, 1).add();
        }
        if (queen) {
            p(b, "sun", "horn", Role.SPIN, 0xF8C040).at(0, -10, 0).mat("energy").glow("full").box(-5, -5, -1, 10, 10, 2).add();
            p(b, "sun_rays", "sun", 0xFFE070).at(0, 0, 0).mat("energy").glow("full").box(-7, -1, -0.5f, 2, 2, 1).box(5, -1, -0.5f, 2, 2, 1)
                    .box(-1, -7, -0.5f, 2, 2, 1).box(-1, 5, -0.5f, 2, 2, 1).add();
            s(b, "sun_horn", "head", false, GOLD).at(3, -3, -2).rot(-10, 0, -20).mat("metal").box(-0.5f, -9, -0.5f, 1, 9, 1).add();
            p(b, "scarab_ankh", "pronotum", LAPIS).at(0, -3.2f, 2).mat("energy").glow("full").box(-1, -0.5f, -1, 2, 1, 3)
                    .box(-2, -0.5f, 0, 4, 1, 1).add();
        }
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Akkha
    // ------------------------------------------------------------------------------------------

    /** Akkha: a towering jackal-headed warrior of Het in black and gold, with a striped nemes, wesekh collar and halberd. */
    static ModelSpec akkha() {
        int black = 0x2A2A32;
        int dark = 0x1A1A20;
        int deep = 0x222228;
        BestiaryRad.Frame f = new BestiaryRad.Frame("akkha", 1.0f, 0.9f).colors(black, dark, deep, GOLD)
                .legs(7, 7, 4, 3).torso(11, 11, 6, 2).arms(7, 7, 3, 3).head(6, 6, 7).hips(9, 4, 6);
        f.mat = "skin";
        f.claws = 0;
        f.toes = 0;
        f.neckLen = 2;
        f.jaw = false;
        f.armSplay = 8;
        f.elbowBend = 25;
        f.headGlow = "eyes";
        f.shoulders = false;
        ModelSpec.Builder b = f.build();
        // jackal muzzle and ears
        p(b, "muzzle", "head", black).at(0, 0.5f, -5).mat("skin").box(-1.5f, -1.5f, -4, 3, 3, 4).add();
        p(b, "nose", "muzzle", 0x0A0A0A).at(0, -1.5f, -4).mat("skin").box(-1, 0, -0.6f, 2, 1, 1).add();
        s(b, "ear", "head", false, black).at(2, -4, 0).rot(-10, 0, -10).mat("skin").box(-1, -6, -1, 2, 6, 2).box(-0.5f, -8, -0.5f, 1, 2, 1).add();
        s(b, "ear_gold", "ear", true, GOLD).at(0, -1, -1.1f).mat("metal").box(-0.5f, -4, -0.2f, 1, 4, 1).add();
        // nemes headdress striped gold and lapis
        p(b, "nemes", "head", GOLD).at(0, -4, 1).mat("cloth").pattern("stripes").accent(LAPIS).box(-4, -1, -3, 8, 2, 6).add();
        s(b, "nemes_lappet", "nemes", false, GOLD).accent(LAPIS).at(3.5f, 1, -1).rot(-5, 0, -5).mat("cloth").pattern("stripes")
                .box(-1, 0, -1.5f, 2, 9, 3).add();
        p(b, "nemes_back", "nemes", LAPIS).accent(GOLD).at(0, 1, 3).rot(10, 0, 0).mat("cloth").pattern("stripes").box(-3, 0, -0.5f, 6, 8, 2).add();
        // wesekh collar, belt, shendyt kilt and greaves
        p(b, "collar", "torso", GOLD).accent(LAPIS).at(0, -11, 0).mat("metal").pattern("stripes").box(-7, 0, -3.5f, 14, 4, 7).add();
        p(b, "collar_gems", "collar", LAPIS).at(0, 3.5f, -3.6f).mat("energy").box(-5, 0, -0.5f, 2, 1, 1).box(-1, 0, -0.5f, 2, 1, 1)
                .box(3, 0, -0.5f, 2, 1, 1).add();
        p(b, "belt", "hips", GOLD).at(0, -2, 0).mat("metal").box(-5, -1, -3.5f, 10, 2, 7).add();
        p(b, "kilt", "hips", 0xE8E0C8).accent(GOLD).at(0, 0, 0).mat("cloth").pattern("stripes").box(-5, -1, -3.6f, 10, 6, 7).add();
        p(b, "kilt_front", "kilt", GOLD).at(0, 0, -3.8f).rot(-5, 0, 0).mat("metal").box(-1.5f, 0, -0.5f, 3, 7, 1).add();
        s(b, "greave", "shin", true, GOLD).at(0, 1, -1.6f).mat("metal").box(-2, 0, -0.6f, 4, 5, 1).add();
        s(b, "bracer", "forearm", true, GOLD).at(0, 3, 0).mat("metal").box(-2, 0, -2, 4, 3, 4).add();
        s(b, "pauldron", "torso", false, GOLD).accent(LAPIS).at(6, -10, 0).rot(0, 0, -25).mat("metal").box(-1, -1.5f, -3, 4, 2, 6).add();
        // halberd: long shaft, crescent blade glowing with power, counter-spike
        p(b, "halberd", "hand_r", 0x3A2A1A).at(0, 1, -1).rot(5, 0, 0).mat("wood").box(-0.5f, -26, -0.5f, 1, 34, 1).add();
        p(b, "halberd_band", "halberd", GOLD).at(0, 0, 0).mat("metal").box(-1, -22, -1, 2, 2, 2).box(-1, 5, -1, 2, 2, 2).add();
        p(b, "halberd_blade", "halberd", GOLD).accent(0xFFF0A0).at(0, -26, 0).mat("metal").glow("runes")
                .box(-0.5f, -3, -6, 1, 6, 6).box(-0.5f, -5, -5, 1, 2, 3).box(-0.5f, 3, -5, 1, 2, 3).add();
        p(b, "halberd_spike", "halberd", GOLD).at(0, -26, 0).mat("metal").box(-0.5f, -6, -0.5f, 1, 6, 1).box(-0.5f, -1, 0.5f, 1, 2, 3).add();
        p(b, "shadow_orb", "hand_l", 0x9A60E0).at(0, 4, -1).mat("energy").glow("full").box(-1.5f, -1.5f, -1.5f, 3, 3, 3).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Ba-Ba and her baboons
    // ------------------------------------------------------------------------------------------

    /**
     * A sacred baboon: hunched, long-armed and knuckle-walking, with a thick mane, a bare pink muzzle and fangs.
     * Ba-Ba herself is huge, crowned with a gold uraeus and banded with gold.
     */
    static ModelSpec baboon(String id, int fur, float scale, boolean queen) {
        int mane = shade(fur, 0.75f);
        int dark = shade(fur, 0.6f);
        int face = 0xD07068;
        BestiaryRad.Frame f = new BestiaryRad.Frame(id, scale, 1.0f).colors(fur, dark, mane, GOLD)
                .legs(5, 5, 5, 4).torso(14, 13, 10, 40).arms(10, 10, 4, 5).head(8, 7, 7).hips(11, 5, 8);
        f.mat = "fur";
        f.claws = 3;
        f.clawLen = 1;
        f.clawColor = 0x2A1A14;
        f.handW = 5;
        f.handH = 2;
        f.toes = 3;
        f.clawMat = "horn";
        f.neckLen = 1;
        f.jaw = false;
        f.faceEyes = false;
        f.armSplay = 14;
        f.elbowBend = 10;
        f.headPitch = -6;
        ModelSpec.Builder b = f.build();
        p(b, "mane", "torso", mane).at(0, -13, -1).rot(-10, 0, 0).mat("fur").box(-8, -1, -6, 16, 7, 10).add();
        p(b, "mane_back", "torso", mane).at(0, -11, 4).mat("fur").box(-6.5f, -1, -1, 13, 7, 3).add();
        p(b, "chest_fur", "torso", shade(fur, 1.2f)).at(0, -9, -5.4f).mat("fur").box(-4, 0, -0.5f, 8, 7, 1).add();
        // head: bare muzzle, brow, fangs, eyes, ears
        p(b, "face", "head", face).at(0, -1, -5).mat("skin").box(-3, -3, -0.6f, 6, 5, 1).add();
        p(b, "muzzle", "head", face).at(0, 1, -5).mat("skin").box(-2.5f, -1.5f, -4, 5, 4, 4).add();
        p(b, "nostrils", "muzzle", 0x5A2020).at(0, -1.5f, -4).mat("skin").box(-1.5f, 0, -0.6f, 1, 1, 1).box(0.5f, 0, -0.6f, 1, 1, 1).add();
        p(b, "baboon_jaw", "muzzle", Role.JAW, shade(face, 0.8f)).at(0, 2.5f, -1).rot(20, 0, 0).mat("skin").box(-2, 0, -3, 4, 1, 3).add();
        s(b, "fang", "muzzle", false, TOOTH).at(1.6f, 2.5f, -3.5f).mat("horn").box(-0.5f, 0, -0.5f, 1, 2, 1).add();
        eyes(b, "eye", "head", queen ? 0xFFD040 : 0xF0A030, 1.6f, -2.5f, -5.6f, 2, 1);
        s(b, "ear", "head", false, face).at(4, -2, -2).mat("skin").box(0, -1, -1, 1, 2, 2).add();
        p(b, "tail", "hips", Role.TAIL, fur).at(0, -2, 3.5f).rot(-30, 0, 0).mat("fur").box(-1, -1, 0, 2, 2, 6).add();
        p(b, "tail2", "tail", Role.TAIL, dark).at(0, 0, 6).rot(30, 0, 0).mat("fur").box(-1, -1, 0, 2, 2, 5).box(-1.5f, -1.5f, 4, 3, 3, 2).add();
        p(b, "rump", "hips", 0xC85A5A).at(0, -1, 3.6f).mat("skin").box(-3, -1, -0.5f, 6, 3, 1).add();
        if (queen) {
            p(b, "crown", "head", GOLD).at(0, -5, -1).mat("metal").box(-4, -1, -4, 8, 2, 6).box(-1, -4, -4.5f, 2, 3, 1).add();
            p(b, "uraeus", "crown", GOLD).at(0, -4, -4.5f).rot(-20, 0, 0).mat("metal").box(-1, -2, -1, 2, 2, 2).add();
            p(b, "crown_gem", "crown", 0x3AA0E0).at(0, -0.5f, -4.2f).mat("energy").glow("full").box(-1, -0.5f, -0.5f, 2, 1, 1).add();
            s(b, "armband", "arm", true, GOLD).at(0, 3, 0).mat("metal").box(-2.5f, 0, -2.5f, 5, 2, 5).add();
            s(b, "wristband", "forearm", true, GOLD).at(0, 7, 0).mat("metal").box(-3, 0, -3, 6, 2, 6).add();
            p(b, "collar", "torso", GOLD).accent(LAPIS).at(0, -12, -2).rot(-20, 0, 0).mat("metal").pattern("stripes")
                    .box(-5, 0, -4, 10, 3, 3).add();
            p(b, "loin", "hips", GOLD).accent(LAPIS).at(0, 0, -4.2f).mat("cloth").pattern("stripes").box(-2.5f, 0, -0.5f, 5, 5, 1).add();
        } else {
            p(b, "collar", "torso", 0x8A6A3A).at(0, -12, -2).rot(-20, 0, 0).mat("leather").box(-4, 0, -4, 8, 2, 3).add();
        }
        return b.build();
    }

    /** One of Ba-Ba's rolling boulders: a chipped, rune-scored sphere of sandstone. */
    static ModelSpec boulder() {
        int sand = 0xA8906A;
        int dark = 0x7A6448;
        ModelSpec.Builder b = ModelSpec.builder("baba_boulder").shadow(0.8f);
        p(b, "rock", null, Role.SPIN, sand).accent(0xE0C060).at(0, 16, 0).mat("hide").box(-5, -5, -5, 10, 10, 10).add();
        p(b, "rock_x", "rock", sand).at(0, 0, 0).mat("hide").box(-7, -3.5f, -3.5f, 14, 7, 7).add();
        p(b, "rock_y", "rock", shade(sand, 1.05f)).at(0, 0, 0).mat("hide").box(-3.5f, -7, -3.5f, 7, 14, 7).add();
        p(b, "rock_z", "rock", shade(sand, 0.95f)).at(0, 0, 0).mat("hide").box(-3.5f, -3.5f, -7, 7, 7, 14).add();
        for (int i = 0; i < 6; i++) {
            float a = (float) (i * Math.PI / 3);
            float x = (float) Math.cos(a) * 5.5f;
            float z = (float) Math.sin(a) * 5.5f;
            float y = (i % 2 == 0 ? -1 : 1) * 3.5f;
            p(b, "chip" + i, "rock", i % 2 == 0 ? dark : shade(sand, 1.1f)).at(x, y, z).rot(i * 20, i * 35, i * 15).mat("hide")
                    .box(-1.5f, -1.5f, -1.5f, 3, 3, 3).add();
        }
        p(b, "glyph", "rock", 0xE0B040).at(0, 0, -7.1f).mat("energy").glow("full").box(-1.5f, -2, -0.5f, 3, 1, 1)
                .box(-1.5f, 1, -0.5f, 3, 1, 1).box(-2.5f, -1, -0.5f, 1, 2, 1).box(1.5f, -1, -0.5f, 1, 2, 1).box(-0.5f, -0.5f, -0.6f, 1, 1, 1).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Zebak
    // ------------------------------------------------------------------------------------------

    /** Zebak: the great crocodile of the Path of Crondis, gold-collared and scarred, with a jaw full of spikes. */
    static ModelSpec zebak() {
        int back = 0x6A7A4A;
        int dark = 0x3A4A2A;
        int belly = 0xD8C890;
        ModelSpec.Builder b = ModelSpec.builder("zebak").scale(1.15f).shadow(1.3f);
        p(b, "body", null, Role.BODY, back).accent(belly).at(0, 13, 0).mat("scales").box(-8, -6, -11, 16, 11, 22).add();
        p(b, "flank", "body", shade(back, 0.9f)).accent(belly).at(0, 0, 0).mat("scales").box(-9, -4, -8, 18, 7, 16).add();
        p(b, "belly", "body", belly).at(0, 5, 0).mat("plates").box(-7, -0.5f, -10, 14, 2, 20).add();
        for (int r = 0; r < 3; r++) {
            Kit.P row = p(b, "scutes" + r, "body", r == 1 ? dark : shade(dark, 1.25f)).at((r - 1) * 4, -6, 0).mat("plates");
            for (int i = 0; i < 6; i++) {
                row.box(-1.2f, -2 - (r == 1 ? 1 : 0), -10 + i * 3.6f, 2, 2 + (r == 1 ? 1 : 0), 2);
            }
            row.add();
        }
        // a hunched, upright front - Zebak rears on thick forelegs
        p(b, "neck", "body", back).accent(belly).at(0, -2, -11).rot(-15, 0, 0).mat("scales").box(-6.5f, -5, -5, 13, 10, 6).add();
        p(b, "collar", "neck", GOLD).accent(LAPIS).at(0, 0, -2).mat("metal").pattern("stripes").box(-7, -5.5f, -1.5f, 14, 11, 3).add();
        p(b, "collar_gem", "collar", LAPIS).at(0, 3, -1.6f).mat("energy").glow("full").box(-1.5f, -1.5f, -0.5f, 3, 3, 1).add();
        p(b, "head", "neck", Role.HEAD, back).accent(belly).at(0, -1, -5).rot(15, 0, 0).mat("scales").box(-5.5f, -3, -7, 11, 6, 7).add();
        s(b, "eye_bump", "head", false, dark).at(3.5f, -3, -4).mat("scales").box(-1.5f, -1.5f, -1.5f, 3, 2, 3).add();
        s(b, "eye", "eye_bump", true, 0xF0C030).at(0, -0.5f, -1.5f).mat("energy").glow("full").box(-1, -0.5f, -0.6f, 2, 1, 1).add();
        p(b, "snout", "head", back).accent(belly).at(0, -1, -7).mat("scales").box(-4, -1.5f, -12, 8, 3, 12).add();
        p(b, "snout_tip", "snout", dark).at(0, -1.5f, -12).mat("scales").box(-3, 0, -1.5f, 6, 2, 2).add();
        teeth(b, "teeth_l", "snout", TOOTH, -11, -1, 1.5f, -3.9f, 6, 2);
        teeth(b, "teeth_r", "snout", TOOTH, -11, -1, 1.5f, 3.9f, 6, 2);
        p(b, "jaw", "head", Role.JAW, belly).at(0, 2, -5).rot(14, 0, 0).mat("scales").box(-4, 0, -14, 8, 2, 14).add();
        teeth(b, "teeth_low", "jaw", TOOTH, -3.5f, 3.5f, 0, -13, 5, -2);
        p(b, "head_gold", "head", GOLD).at(0, -3, -2).mat("metal").box(-3, -1, -3, 6, 1, 5).box(-0.5f, -3, -3, 1, 2, 2).add();
        float[][] legs = {{8, 0, -7}, {8, 0, 7}};
        String[] names = {"leg_f", "leg_b"};
        Role[][] roles = {{Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT}, {Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT}};
        for (int i = 0; i < 2; i++) {
            String n = names[i];
            s(b, n, "body", false, back).roles(roles[i][0], roles[i][1]).accent(belly).at(legs[i][0], legs[i][1], legs[i][2])
                    .rot(0, 0, 30).mat("scales").box(-2, -2, -3, 7, 5, 6).add();
            s(b, n + "_shin", n, true, dark).at(5, 1, 0).rot(0, 0, -35).mat("scales").box(-2, 0, -2.5f, 4, 6, 5).add();
            s(b, n + "_foot", n + "_shin", true, dark).at(0, 5.5f, 0).rot(0, 0, 5).mat("scales").box(-2.5f, 0, -5, 5, 2, 6).add();
            s(b, n + "_claws", n + "_foot", true, TOOTH).at(0, 1, -5).mat("horn").box(-2.5f, -0.5f, -1.5f, 1, 1, 2)
                    .box(-0.5f, -0.5f, -2, 1, 1, 2).box(1.5f, -0.5f, -1.5f, 1, 1, 2).add();
        }
        p(b, "tail", "body", Role.TAIL, back).accent(belly).at(0, -1, 11).mat("scales").box(-5.5f, -4, 0, 11, 8, 9).add();
        p(b, "tail_ridge", "tail", dark).at(0, -4, 0).mat("plates").box(-3, -2, 1, 2, 2, 2).box(1, -2, 1, 2, 2, 2).box(-3, -2, 5, 2, 2, 2)
                .box(1, -2, 5, 2, 2, 2).add();
        p(b, "tail2", "tail", Role.TAIL, back).accent(belly).at(0, 0.5f, 9).mat("scales").box(-4, -3, 0, 8, 6, 9).add();
        p(b, "tail3", "tail2", Role.TAIL, dark).accent(belly).at(0, 0.5f, 9).mat("scales").box(-2.5f, -2, 0, 5, 4, 8)
                .box(-0.5f, -3.5f, 1, 1, 2, 6).add();
        p(b, "tail_band", "tail2", GOLD).at(0, 0, 6).mat("metal").box(-4.5f, -3.5f, 0, 9, 7, 1).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // The Wardens and their obelisk
    // ------------------------------------------------------------------------------------------

    /** The Wardens' obelisk: a tapering, hieroglyph-carved sandstone needle on a stepped base, crowned with a gold pyramidion. */
    static ModelSpec obelisk() {
        int sand = 0xC8B080;
        int dark = 0x8A7A5A;
        int energy = 0x80E0F0;
        ModelSpec.Builder b = ModelSpec.builder("wardens_obelisk").shadow(1.0f);
        p(b, "base", null, dark).at(0, 24, 0).mat("plates").box(-9, -3, -9, 18, 3, 18).add();
        p(b, "base2", "base", sand).at(0, -3, 0).mat("plates").box(-7, -3, -7, 14, 3, 14).add();
        p(b, "pillar", "base2", sand).accent(energy).at(0, -3, 0).mat("plates").glow("runes").box(-5, -16, -5, 10, 16, 10).add();
        p(b, "pillar2", "pillar", shade(sand, 1.05f)).accent(energy).at(0, -16, 0).mat("plates").glow("runes").box(-4, -12, -4, 8, 12, 8).add();
        p(b, "pillar3", "pillar2", sand).accent(energy).at(0, -12, 0).mat("plates").box(-3, -8, -3, 6, 8, 6).add();
        p(b, "pyramidion", "pillar3", GOLD).at(0, -8, 0).mat("metal").box(-3.5f, -2, -3.5f, 7, 2, 7).box(-2, -4, -2, 4, 2, 4)
                .box(-1, -6, -1, 2, 2, 2).add();
        Kit.P glyphs = p(b, "glyphs", "pillar", energy).at(0, 0, 0).mat("energy").glow("full");
        for (int i = 0; i < 4; i++) {
            float y = -14 + i * 3.5f;
            glyphs.box(-2, y, -5.6f, 1, 2, 1).box(0, y, -5.6f, 2, 1, 1).box(-2, y, 4.6f, 2, 1, 1).box(1, y + 1, 4.6f, 1, 1, 1);
        }
        glyphs.add();
        p(b, "eye_glyph", "pillar2", energy).at(0, -7, -4.2f).mat("energy").glow("full").box(-2, -1, -0.5f, 4, 2, 1)
                .box(-0.5f, 1, -0.5f, 1, 2, 1).add();
        p(b, "energy", "base", Role.SPIN, energy).at(0, -40, 0).mat("energy").glow("full").box(-2, -2, -2, 4, 4, 4).add();
        p(b, "energy_ring", "energy", 0xC0F8FF).at(0, 0, 0).mat("energy").glow("full").box(-5, -0.5f, -5, 10, 1, 1).box(-5, -0.5f, 4, 10, 1, 1)
                .box(-5, -0.5f, -4, 1, 1, 8).box(4, -0.5f, -4, 1, 1, 8).add();
        s(b, "corner", "base", false, GOLD).at(8, -3, 8).mat("metal").box(-1.5f, -2, -1.5f, 3, 2, 3).add();
        s(b, "corner_f", "base", false, GOLD).at(8, -3, -8).mat("metal").box(-1.5f, -2, -1.5f, 3, 2, 3).add();
        return b.build();
    }

    /**
     * A Warden: a colossal pharaoh-statue of carved stone - nemes headdress, false beard, broad pectoral collar, a
     * glowing core in its chest, pleated kilt, and a was-sceptre crowned with an ankh.
     */
    static ModelSpec warden(String id, int stone, int dark, int glow) {
        BestiaryRad.Frame f = new BestiaryRad.Frame(id, 1.0f, 1.3f).colors(stone, shade(stone, 0.75f), dark, glow)
                .legs(8, 7, 5, 4).torso(18, 16, 10, 4).arms(10, 10, 5, 5).head(8, 8, 8).hips(14, 5, 9);
        f.mat = "plates";
        f.bodyGlow = "runes";
        f.claws = 0;
        f.toes = 0;
        f.handW = 6;
        f.handH = 4;
        f.neckLen = 2;
        f.jaw = false;
        f.armSplay = 8;
        f.elbowBend = 30;
        f.headGlow = "eyes";
        ModelSpec.Builder b = f.build();
        p(b, "plinth", null, 0x7A6A50).at(0, 24, 0).mat("rock").box(-11, -2, -9, 22, 2, 18).box(-10, -3, -8, 20, 1, 16).add();
        p(b, "nemes", "head", dark).accent(GOLD).at(0, -5, 1).mat("cloth").pattern("stripes").box(-5, -1.5f, -4, 10, 3, 8).add();
        s(b, "nemes_lappet", "nemes", false, dark).accent(GOLD).at(4.5f, 1.5f, -2).mat("cloth").pattern("stripes")
                .box(-1, 0, -1.5f, 2, 11, 3).add();
        p(b, "nemes_back", "nemes", dark).accent(GOLD).at(0, 1.5f, 4).rot(8, 0, 0).mat("cloth").pattern("stripes").box(-4, 0, -0.5f, 8, 10, 2).add();
        p(b, "uraeus", "nemes", GOLD).at(0, -1.5f, -4).rot(-15, 0, 0).mat("metal").box(-1, -3, -1, 2, 3, 2).add();
        p(b, "beard", "head", dark).accent(GOLD).at(0, 2, -5).mat("plates").box(-1, 0, -1, 2, 5, 2).add();
        p(b, "collar", "torso", GOLD).accent(glow).at(0, -16, 0).mat("metal").pattern("stripes").box(-9.5f, 0, -5.5f, 19, 5, 11).add();
        p(b, "collar_beads", "collar", dark).at(0, 4.6f, -5.6f).mat("metal").box(-7, 0, -0.5f, 2, 1, 1).box(-3, 0, -0.5f, 2, 1, 1)
                .box(1, 0, -0.5f, 2, 1, 1).box(5, 0, -0.5f, 2, 1, 1).add();
        p(b, "core", "torso", Role.SPIN, glow).at(0, -8, -5.5f).mat("energy").glow("full").box(-2.5f, -2.5f, -1.5f, 5, 5, 3).add();
        p(b, "core_frame", "torso", GOLD).at(0, -8, -5.3f).mat("metal").box(-4, -4, -1, 8, 1, 2).box(-4, 3, -1, 8, 1, 2)
                .box(-4, -3, -1, 1, 6, 2).box(3, -3, -1, 1, 6, 2).add();
        p(b, "kilt", "hips", 0xE8E0C8).accent(GOLD).at(0, 0, 0).mat("cloth").pattern("stripes").box(-7.5f, -1, -5, 15, 7, 10).add();
        p(b, "kilt_apron", "kilt", GOLD).accent(glow).at(0, 0, -5.2f).mat("metal").glow("runes").box(-2, 0, -0.5f, 4, 8, 1).add();
        s(b, "armlet", "arm", true, GOLD).at(0, 3, 0).mat("metal").box(-3, 0, -3, 6, 2, 6).add();
        s(b, "bracer", "forearm", true, GOLD).at(0, 6, 0).mat("metal").box(-3, 0, -3, 6, 3, 6).add();
        // was-sceptre and ankh
        p(b, "sceptre", "hand_r", dark).at(0, 2, -1).rot(5, 0, 0).mat("metal").box(-0.5f, -28, -0.5f, 1, 36, 1).add();
        p(b, "sceptre_head", "sceptre", GOLD).at(0, -28, 0).rot(0, 0, 0).mat("metal").box(-0.5f, -2, -3, 1, 2, 4).box(-0.5f, -3, -3, 1, 1, 1).add();
        p(b, "ankh", "hand_l", glow).at(0, 4, -1).mat("energy").glow("full").box(-0.5f, -1, -0.5f, 1, 6, 1).box(-2, 0, -0.5f, 4, 1, 1)
                .box(-1.5f, -4, -0.5f, 1, 3, 1).box(0.5f, -4, -0.5f, 1, 3, 1).box(-1, -5, -0.5f, 2, 1, 1).add();
        return b.build();
    }
}
