package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.BestiaryRad.eyes;
import static com.runecraft.osrs.content.bestiary.BestiaryRad.p;
import static com.runecraft.osrs.content.bestiary.BestiaryRad.s;
import static com.runecraft.osrs.content.bestiary.BestiaryRad.shade;
import static com.runecraft.osrs.content.bestiary.BestiaryRad.spikes;
import static com.runecraft.osrs.content.bestiary.BestiaryRad.teeth;

/** Theatre of Blood: the Maiden, the Bloat, the Nylocas swarm, Sotetseg, Xarpus and all three forms of Verzik Vitur. */
final class BestiaryRadTob {
    private BestiaryRadTob() {
    }

    static final int TOOTH = 0xEDE4CC;
    static final int BLOOD = 0x9A1414;

    static List<ModelSpec> all() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(maiden());
        l.add(bloat());
        l.add(bloodSpawn());
        l.add(nylocas("nylocas_matomenos", 0x8A6A6A, 0xA02A2A, 1.0f, false));
        l.add(nylocas("nylocas_ischyros", 0xCFCBC0, 0x6E6A60, 0.9f, false));
        l.add(nylocas("nylocas_toxobolos", 0x4E9A3A, 0x2A5A2A, 0.9f, false));
        l.add(nylocas("nylocas_hagios", 0x3E6AC8, 0x22348A, 0.9f, false));
        l.add(nylocas("nylocas_athanatos", 0x8A3AB0, 0x4A1A6A, 1.2f, false));
        l.add(nylocas("nylocas_vasilias", 0x5A4A6A, 0xC03A3A, 1.0f, true));
        l.add(sotetseg());
        l.add(xarpus());
        l.add(verzikP1());
        l.add(verzikP2());
        l.add(verzikP3());
        return l;
    }

    // ------------------------------------------------------------------------------------------
    // The Maiden of Sugadinti and her blood spawn
    // ------------------------------------------------------------------------------------------

    /**
     * The Maiden: a vast woman of swollen, blood-soaked flesh rising from a mound in her own pool, arms flung wide in
     * shackles and chains, long matted hair, and veined tendrils trailing across the floor.
     */
    static ModelSpec maiden() {
        int flesh = 0xB04A44;
        int dark = 0x6A1E1E;
        int pale = 0xC8706A;
        int hair = 0x3A0E10;
        ModelSpec.Builder b = ModelSpec.builder("the_maiden_of_sugadinti").shadow(1.4f);
        p(b, "pool", null, BLOOD).at(0, 24, 0).mat("slime").glow("cracks").accent(0xE02020).box(-16, -1, -14, 32, 1, 28).add();
        p(b, "pool_rim", "pool", 0x6A0808).at(0, 0, 0).mat("slime").box(-14, -1.5f, -16, 12, 1, 3).box(4, -1.5f, 13, 10, 1, 3)
                .box(-17, -1.5f, -4, 2, 1, 10).add();
        // mound of flesh in tiers
        p(b, "mound", null, dark).at(0, 23, 1).mat("flesh").box(-12, -5, -10, 24, 5, 20).add();
        p(b, "mound2", "mound", shade(flesh, 0.85f)).at(0, -5, 0).mat("flesh").box(-10, -4, -8, 20, 4, 16).add();
        p(b, "mound3", "mound2", flesh).at(0, -4, 0).mat("flesh").box(-8, -3, -6, 16, 3, 12).add();
        for (int i = 0; i < 4; i++) {
            float x = i < 2 ? -9 : 9;
            float z = i % 2 == 0 ? -6 : 6;
            p(b, "lump" + i, "mound", i % 2 == 0 ? flesh : dark).at(x, -4, z).rot(i * 15, i * 40, -i * 10).mat("flesh")
                    .box(-2.5f, -2.5f, -2.5f, 5, 4, 5).add();
        }
        // torso: swollen belly, chest and shoulders
        p(b, "body", "mound3", Role.BODY, flesh).at(0, -3, 0).mat("flesh").box(-7, -9, -5, 14, 9, 10).add();
        p(b, "belly", "body", pale).at(0, -3, -5).mat("flesh").box(-5.5f, -4, -1.5f, 11, 7, 2).add();
        p(b, "chest", "body", flesh).at(0, -9, 0).mat("flesh").box(-6, -6, -4, 12, 6, 8).add();
        s(b, "breast", "chest", false, pale).at(2.5f, -3, -4).rot(-8, 0, 0).mat("flesh").box(-2, -2, -1.5f, 4, 4, 2).add();
        p(b, "veins", "body", 0x4A0A0A).at(0, -6, -5.2f).mat("flesh").box(-4, 0, -0.5f, 1, 6, 1).box(3, -2, -0.5f, 1, 7, 1)
                .box(-1, 2, -0.5f, 3, 1, 1).add();
        // head and long hair
        p(b, "neck", "chest", pale).at(0, -6, 0).mat("flesh").box(-2, -2, -2, 4, 3, 4).add();
        p(b, "head", "neck", Role.HEAD, pale).accent(0xFF3020).at(0, -2, 0).mat("flesh").pattern("eyes").glow("eyes")
                .box(-3.5f, -7, -4, 7, 7, 7).add();
        p(b, "hair", "head", hair).at(0, -7, 0).mat("hair").box(-4, -1, -4.5f, 8, 2, 8).box(-4, 0, 2, 8, 10, 2)
                .box(-4.5f, 0, -3.5f, 1, 9, 6).box(3.5f, 0, -3.5f, 1, 9, 6).add();
        p(b, "hair_tail", "hair", hair).at(0, 10, 3).rot(15, 0, 0).mat("hair").box(-3, 0, -1, 6, 7, 2).add();
        p(b, "brow_blood", "head", 0x5A0A0A).at(0, -4.5f, -4.1f).mat("flesh").box(-3, 0, -0.5f, 6, 1, 1).add();
        // arms flung wide, shackled and chained to the floor
        s(b, "arm", "chest", false, flesh).roles(Role.ARM_LEFT, Role.ARM_RIGHT).at(6, -4, 0).rot(0, 0, -70).mat("flesh")
                .box(-2, -1, -2, 4, 8, 4).add();
        s(b, "forearm", "arm", true, pale).at(0, 8, 0).rot(-20, 0, 30).mat("flesh").box(-1.5f, 0, -1.5f, 3, 8, 3).add();
        s(b, "hand", "forearm", true, flesh).at(0, 8, 0).mat("flesh").box(-1.5f, 0, -2, 3, 3, 4)
                .box(-1.5f, 3, -2, 1, 2, 1).box(0.5f, 3, -2, 1, 2, 1).add();
        s(b, "shackle", "forearm", true, 0x4A4A50).at(0, 6, 0).mat("metal").box(-2, 0, -2, 4, 2, 4).add();
        s(b, "chain", "shackle", true, 0x5A5A60).at(0, 1, 0).rot(0, 0, -30).mat("chain").box(-0.5f, 0, -0.5f, 1, 10, 1)
                .box(-1, 2, -1, 2, 1, 2).box(-1, 6, -1, 2, 1, 2).add();
        // tendrils
        float[][] t = {{-8, -8, 0}, {8, -8, 0}, {-11, 2, 60}, {11, 2, -60}, {-6, 9, 150}, {6, 9, -150}};
        for (int i = 0; i < t.length; i++) {
            String n = "tendril" + i;
            p(b, n, "mound", Role.TENTACLE, i % 2 == 0 ? flesh : dark).at(t[i][0], -2, t[i][1]).rot(10, t[i][2], 0).mat("flesh")
                    .box(-1.5f, -1.5f, -7, 3, 3, 7).add();
            p(b, n + "b", n, Role.TENTACLE, pale).at(0, 0, -7).rot(-15, (i % 2 == 0 ? 20 : -20), 0).mat("flesh")
                    .box(-1, -1, -6, 2, 2, 6).add();
        }
        p(b, "drips", "body", BLOOD).at(0, 0, 0).mat("slime").glow("full").accent(0xFF2020)
                .box(-6, -2, -5.4f, 1, 3, 1).box(4, -5, -5.4f, 1, 4, 1).box(-2, -1, 5, 1, 3, 1).add();
        return b.build();
    }

    /** One of the Maiden's blood spawn: a quivering, many-dripped orb of blood with a single staring eye. */
    static ModelSpec bloodSpawn() {
        int red = 0xB01A1A;
        int dark = 0x6A0A0A;
        ModelSpec.Builder b = ModelSpec.builder("blood_spawn").shadow(0.4f);
        p(b, "blob", null, Role.FLOAT, red).accent(0xFF4040).at(0, 13, 0).mat("slime").box(-4, -4, -4, 8, 8, 8).add();
        p(b, "blob_x", "blob", red).at(0, 0, 0).mat("slime").box(-5, -2.5f, -2.5f, 10, 5, 5).add();
        p(b, "blob_z", "blob", red).at(0, 0, 0).mat("slime").box(-2.5f, -2.5f, -5, 5, 5, 10).add();
        p(b, "blob_y", "blob", shade(red, 1.1f)).at(0, 0, 0).mat("slime").box(-2.5f, -5, -2.5f, 5, 10, 5).add();
        p(b, "eye", "blob", Role.HEAD, 0xF0E0D0).at(0, -1, -5).mat("flesh").box(-2, -1.5f, -0.8f, 4, 3, 1).add();
        p(b, "pupil", "eye", 0x200404).at(0, 0, -0.8f).mat("energy").glow("full").accent(0xFF2020).box(-0.5f, -1, -0.4f, 1, 2, 1).add();
        p(b, "maw", "blob", Role.JAW, dark).at(0, 2.5f, -4.5f).mat("flesh").box(-2, 0, -1, 4, 1, 2).add();
        for (int i = 0; i < 5; i++) {
            float a = (float) (i * Math.PI * 2 / 5);
            float x = (float) Math.cos(a) * 3;
            float z = (float) Math.sin(a) * 3;
            p(b, "drip" + i, "blob", Role.TENTACLE, i % 2 == 0 ? dark : red).at(x, 4, z).mat("slime")
                    .box(-1, 0, -1, 2, 3 + i % 3, 2).box(-0.5f, 3 + i % 3, -0.5f, 1, 2, 1).add();
        }
        p(b, "bubbles", "blob", 0xFF6060).at(0, -5, 0).mat("energy").glow("full").box(-2, -1, 1, 1, 1, 1).box(1, -1.5f, -1, 1, 1, 1)
                .box(2, -0.5f, 2, 1, 1, 1).add();
        float[][] lobes = {{-3.5f, -3.5f, -3.5f}, {3.5f, -3.5f, -3.5f}, {-3.5f, -3.5f, 3.5f}, {3.5f, -3.5f, 3.5f}, {-3.5f, 3.5f, -3.5f},
            {3.5f, 3.5f, -3.5f}, {-3.5f, 3.5f, 3.5f}, {3.5f, 3.5f, 3.5f}};
        for (int i = 0; i < lobes.length; i++) {
            float[] q = lobes[i];
            p(b, "lobe" + i, "blob", i % 2 == 0 ? shade(red, 0.9f) : red).at(q[0] * 0.8f, q[1] * 0.8f, q[2] * 0.8f).mat("slime")
                    .box(-1.5f, -1.5f, -1.5f, 3, 3, 3).add();
        }
        p(b, "veins", "blob", dark).at(0, 0, 0).mat("flesh").box(-5.2f, -1, -1, 1, 3, 1).box(4.2f, -2, 1, 1, 4, 1)
                .box(-1, -5.2f, 2, 3, 1, 1).box(1, -1, 4.2f, 1, 3, 1).add();
        teeth(b, "teeth", "maw", TOOTH, -1.5f, 1.5f, 0, -1.1f, 3, 1);
        p(b, "orbiting", "blob", Role.SPIN, 0xD02020).at(0, 0, 0).mat("slime").box(-8, -1, -1, 2, 2, 2).box(6, 0, 1, 2, 2, 2)
                .box(0, -2, 6, 2, 2, 2).box(-1, 1, -8, 2, 2, 2).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // The Pestilent Bloat
    // ------------------------------------------------------------------------------------------

    /** The Pestilent Bloat: a stitched, rotting sack of corpses lurching on stubby limbs, oozing and fly-blown. */
    static ModelSpec bloat() {
        int rot = 0x7E8A5E;
        int dark = 0x4E5A3A;
        int pale = 0xA8AA80;
        int stitch = 0x2E2014;
        ModelSpec.Builder b = ModelSpec.builder("pestilent_bloat").shadow(1.3f);
        p(b, "sack", null, Role.BODY, rot).accent(0x9AA060).at(0, 17, 0).mat("flesh").pattern("spots")
                .box(-10, -17, -9, 20, 17, 18).add();
        p(b, "sack_bulge", "sack", rot).at(0, -8, 0).mat("flesh").box(-11.5f, -6, -7, 23, 10, 14).box(-8, -7, -10.5f, 16, 11, 21).add();
        p(b, "sack_top", "sack", shade(rot, 1.05f)).at(0, -17, 0).mat("flesh").box(-8, -2, -7, 16, 2, 14).add();
        p(b, "sack_bottom", "sack", dark).at(0, 0, 0).mat("flesh").box(-8, -1, -7, 16, 2, 14).add();
        // stitches and patches
        p(b, "stitch_seam", "sack", stitch).at(0, 0, 0).mat("leather").box(-0.5f, -19, -10.8f, 1, 18, 1).box(-0.5f, -19.5f, -6, 1, 1, 12)
                .box(-0.5f, -18, 10, 1, 17, 1).add();
        Kit.P cross = p(b, "stitch_cross", "sack", stitch).at(0, -9, -10.8f).mat("leather");
        for (int i = 0; i < 6; i++) {
            cross.box(-1.5f, -7 + i * 3, -0.6f, 3, 1, 1);
        }
        cross.add();
        p(b, "patch_a", "sack", 0x8A7A5A).at(6, -12, -10.6f).rot(0, 0, 10).mat("leather").box(-2.5f, -2.5f, -0.5f, 5, 5, 1).add();
        p(b, "patch_b", "sack", 0x6A6048).at(-11.8f, -10, 2).mat("leather").box(-0.5f, -3, -3, 1, 6, 6).add();
        for (int i = 0; i < 6; i++) {
            float[] q = {-6 + (i % 3) * 6, -15 + (i / 3) * 9, (i % 2 == 0 ? -1 : 1) * (10.8f)};
            p(b, "pustule" + i, "sack", 0xB8D060).at(q[0], q[1], q[2]).mat("slime").glow("full").accent(0xD0F070)
                    .box(-1, -1, -1, 2, 2, 2).add();
        }
        // a gaping, toothy maw low on the front with a lolling tongue
        p(b, "head", "sack", Role.HEAD, pale).at(0, -6, -10).mat("flesh").box(-6, -4, -2, 12, 5, 3).add();
        p(b, "maw", "head", 0x3A0A0A).at(0, 1, -1.5f).mat("flesh").box(-5, 0, -0.5f, 10, 3, 1).add();
        teeth(b, "teeth_top", "head", TOOTH, -4.5f, 4.5f, 1, -2.3f, 7, 1);
        p(b, "jaw", "head", Role.JAW, dark).at(0, 4, -1).rot(15, 0, 0).mat("flesh").box(-5.5f, 0, -2, 11, 2, 3).add();
        teeth(b, "teeth_bot", "jaw", TOOTH, -4.5f, 4.5f, 0, -1.6f, 6, -1);
        p(b, "tongue", "jaw", 0x9A3A4A).at(1, 0.5f, -2).rot(30, 0, 0).mat("flesh").box(-1.5f, 0, -3, 3, 1, 4).add();
        eyes(b, "eye", "sack", 0xD8E060, 4, -14, -10.6f, 2, 2);
        // stubby legs, little grasping arms
        s(b, "leg_f", null, false, dark).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).at(7, 17, -6).mat("flesh")
                .box(-2.5f, 0, -2.5f, 5, 5, 5).add();
        s(b, "leg_b", null, false, dark).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).at(7, 17, 6).mat("flesh")
                .box(-2.5f, 0, -2.5f, 5, 5, 5).add();
        s(b, "foot_f", "leg_f", true, shade(dark, 0.8f)).at(0, 5, 0).mat("flesh").box(-3, 0, -3.5f, 6, 2, 6).add();
        s(b, "foot_b", "leg_b", true, shade(dark, 0.8f)).at(0, 5, 0).mat("flesh").box(-3, 0, -3.5f, 6, 2, 6).add();
        s(b, "toes_f", "foot_f", true, 0xC8C0A0).at(0, 1.5f, -3.5f).mat("horn").box(-2.5f, -0.5f, -1, 1, 1, 1).box(-0.5f, -0.5f, -1, 1, 1, 1)
                .box(1.5f, -0.5f, -1, 1, 1, 1).add();
        s(b, "arm", "sack", false, pale).roles(Role.ARM_LEFT, Role.ARM_RIGHT).at(10, -11, -5).rot(-30, 0, -30).mat("flesh")
                .box(-1, -1, -1, 2, 7, 2).add();
        s(b, "claw", "arm", true, dark).at(0, 7, 0).rot(-20, 0, 0).mat("flesh").box(-1, 0, -1, 2, 2, 2).box(-1, 2, -1.5f, 1, 2, 1)
                .box(0.3f, 2, -1.5f, 1, 2, 1).add();
        p(b, "flies", "sack", Role.SPIN, 0x141414).at(0, -22, 0).mat("chitin")
                .box(-6, -1, -3, 1, 1, 1).box(5, -2, 2, 1, 1, 1).box(-2, -3, 5, 1, 1, 1).box(3, 0, -6, 1, 1, 1).box(-7, -2, 4, 1, 1, 1)
                .box(7, -1, -2, 1, 1, 1).add();
        p(b, "ooze", "sack_bottom", 0x9AB050).at(0, 1, 0).mat("slime").glow("full").box(-6, 0, -5, 3, 1, 3).box(3, 0, 3, 4, 1, 3).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Nylocas
    // ------------------------------------------------------------------------------------------

    /**
     * A Nylocas: a crawling bloodsucker with a domed, plated abdomen, a cephalothorax bristling with eyes, hooked
     * chelicerae and eight three-jointed legs. The colour shows the style that kills it. The Vasilias (the room's
     * queen) gets a crown of spines and a larger, glowing abdomen.
     */
    static ModelSpec nylocas(String id, int body, int accent, float scale, boolean queen) {
        int dark = shade(body, 0.6f);
        int leg = shade(body, 0.8f);
        ModelSpec.Builder b = ModelSpec.builder(id).scale(scale).shadow(0.9f * scale);
        p(b, "thorax", null, Role.BODY, body).accent(accent).at(0, 15, 0).mat("chitin").box(-5, -4, -5, 10, 6, 10).add();
        p(b, "thorax_plate", "thorax", shade(body, 1.12f)).at(0, -4, 0).mat("chitin").box(-4, -1, -4, 8, 1, 8).add();
        p(b, "abdomen", "thorax", Role.TAIL, body).accent(accent).at(0, -2, 5).rot(-12, 0, 0).mat("chitin")
                .box(-6, -6, 0, 12, 10, 12).add();
        p(b, "abd_top", "abdomen", shade(body, 1.1f)).at(0, -6, 6).mat("chitin").box(-4.5f, -1.5f, -5, 9, 2, 10).add();
        p(b, "abd_bands", "abdomen", accent).at(0, 0, 0).mat("chitin").box(-6.5f, -5, 3, 13, 9, 1).box(-6.5f, -5, 7, 13, 9, 1)
                .box(-5.5f, -4, 11, 11, 7, 1).add();
        p(b, "abd_tip", "abdomen", dark).at(0, -1, 12).rot(15, 0, 0).mat("chitin").box(-3, -3, 0, 6, 5, 3).add();
        p(b, "spinner", "abd_tip", accent).at(0, 0, 3).mat("chitin").box(-1, -1, 0, 2, 2, 2).add();
        // head with clustered eyes and chelicerae
        p(b, "head", "thorax", Role.HEAD, body).accent(accent).at(0, -1, -5).mat("chitin").box(-3.5f, -3, -4, 7, 5, 4).add();
        p(b, "eyes_main", "head", 0xF03030).at(0, -1.5f, -4).mat("energy").glow("full")
                .box(-2.5f, -0.5f, -0.6f, 2, 2, 1).box(0.5f, -0.5f, -0.6f, 2, 2, 1).box(-1, -2, -0.4f, 1, 1, 1).box(0, -2, -0.4f, 1, 1, 1)
                .box(-3.2f, 0.8f, -0.2f, 1, 1, 1).box(2.2f, 0.8f, -0.2f, 1, 1, 1).add();
        s(b, "chelicera", "head", false, dark).roles(Role.JAW, Role.JAW).at(1.5f, 1.5f, -3.5f).rot(15, 0, 0).mat("chitin")
                .box(-1, 0, -2, 2, 3, 2).add();
        s(b, "fang", "chelicera", true, TOOTH).at(0, 3, -1).rot(-30, 0, 0).mat("horn").box(-0.5f, 0, -0.5f, 1, 3, 1).add();
        s(b, "pedipalp", "head", false, leg).at(3, 1, -3).rot(20, 30, -20).mat("chitin").box(-0.5f, 0, -3, 1, 1, 3).box(-0.5f, 1, -4, 1, 2, 1).add();
        // eight legs: coxa, long femur arching up, tibia down to the floor
        float[] zs = {-3.5f, -1.2f, 1.2f, 3.5f};
        float[] yaws = {-40, -12, 12, 40};
        for (int i = 0; i < 4; i++) {
            String n = "leg" + i;
            s(b, n, "thorax", false, leg).roles(Role.LEG_EXTRA, Role.LEG_EXTRA).accent(accent).at(5, -1, zs[i])
                    .rot(0, yaws[i], -30).mat("chitin").box(0, -1, -1, 7, 2, 2).add();
            s(b, n + "_knee", n, true, accent).at(7, 0, 0).mat("chitin").box(-1, -1.5f, -1.5f, 2, 3, 3).add();
            s(b, n + "_shin", n, true, dark).at(7, 0, 0).rot(0, 0, 12).mat("chitin").box(-1, 0, -1, 2, 11, 2).add();
            s(b, n + "_foot", n + "_shin", true, TOOTH).at(0, 11, 0).mat("horn").box(-0.5f, 0, -0.5f, 1, 2, 1).add();
        }
        if (queen) {
            spikes(b, "crown", "thorax", accent, "horn", -5, new float[][]{{-3, -2, 4}, {0, -3, 6}, {3, -2, 4}, {-4, 2, 3}, {4, 2, 3}}, -10);
            p(b, "abd_glow", "abdomen", accent).at(0, -7.6f, 6).mat("energy").glow("full").box(-2, -0.5f, -3, 4, 1, 6).add();
            s(b, "abd_spike", "abdomen", false, TOOTH).at(5, -4, 6).rot(0, 0, -50).mat("horn").box(-0.5f, -4, -0.5f, 1, 4, 1)
                    .box(-0.5f, -3, 3, 1, 3, 1).add();
        }
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Sotetseg
    // ------------------------------------------------------------------------------------------

    /**
     * Sotetseg: a hulking crimson beast of the maze - armoured shoulders, long clawed forelimbs, a skull-like
     * horned head with a burning maw, a spined back split by a glowing seam, and a heavy tail.
     */
    static ModelSpec sotetseg() {
        int red = 0xB02A26;
        int dark = 0x5A1010;
        int deep = 0x7E1A18;
        int bone = 0xE0D2C0;
        int glow = 0xFF4A3A;
        ModelSpec.Builder b = ModelSpec.builder("sotetseg").shadow(1.4f);
        p(b, "body", null, Role.BODY, red).accent(glow).at(0, 9, 3).rot(-12, 0, 0).mat("flesh").box(-8, -7, -11, 16, 12, 22).add();
        p(b, "chest", "body", deep).at(0, -2, -11).rot(-6, 0, 0).mat("flesh").box(-7, -5, -2, 14, 10, 3).add();
        p(b, "belly", "body", dark).at(0, 5, 0).mat("plates").box(-6, -1, -9, 12, 2, 18).add();
        p(b, "back_plate", "body", dark).at(0, -7, 0).mat("chitin").box(-6, -2, -10, 5, 2, 20).box(1, -2, -10, 5, 2, 20).add();
        p(b, "seam", "body", glow).at(0, -7.6f, 0).mat("energy").glow("full").box(-1, -1, -9, 2, 1, 18).add();
        spikes(b, "spines_l", "body", bone, "horn", -8, new float[][]{{-3.5f, -8, 3}, {-3.5f, -3, 4}, {-3.5f, 2, 4}, {-3.5f, 7, 3}}, -25);
        spikes(b, "spines_r", "body", bone, "horn", -8, new float[][]{{3.5f, -8, 3}, {3.5f, -3, 4}, {3.5f, 2, 4}, {3.5f, 7, 3}}, -25);
        s(b, "shoulder", "body", false, deep).accent(glow).at(7.5f, -4, -7).rot(0, 0, -15).mat("chitin").box(-2, -4, -4, 6, 7, 8).add();
        s(b, "shoulder_spike", "shoulder", true, bone).at(2, -4, 0).rot(-20, 0, -30).mat("horn").box(-1, -4, -1, 2, 4, 2)
                .box(-0.5f, -6, -0.5f, 1, 2, 1).add();
        // neck and skull head
        p(b, "neck", "body", Role.NECK, red).at(0, -3, -11).rot(20, 0, 0).mat("flesh").box(-4, -4, -5, 8, 8, 5).add();
        p(b, "head", "neck", Role.HEAD, bone).accent(glow).at(0, 0, -5).rot(-10, 0, 0).mat("bone").pattern("skull").glow("eyes")
                .box(-4.5f, -4, -7, 9, 7, 7).add();
        p(b, "snout", "head", bone).at(0, 0, -7).mat("bone").box(-3, -2, -3, 6, 4, 3).add();
        teeth(b, "teeth", "snout", TOOTH, -2.5f, 2.5f, 2, -2.6f, 5, 1);
        p(b, "jaw", "head", Role.JAW, dark).accent(glow).at(0, 3, -4).rot(15, 0, 0).mat("flesh").glow("maw").box(-3.5f, 0, -6, 7, 2, 6).add();
        teeth(b, "teeth_low", "jaw", TOOTH, -2.5f, 2.5f, 0, -5.5f, 4, -1);
        s(b, "horn", "head", false, dark).at(4, -3, -2).rot(-20, 0, -45).mat("horn").box(-1, -5, -1, 2, 5, 2).add();
        s(b, "horn_tip", "horn", true, bone).at(0, -5, 0).rot(-40, 0, 10).mat("horn").box(-0.5f, -4, -0.5f, 1, 4, 1).add();
        // long forelimbs with sickle claws, crouched hind legs
        s(b, "leg_f", "body", false, red).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).at(8, 0, -8).rot(-10, 0, -10).mat("flesh")
                .box(-2.5f, -2, -3, 5, 8, 6).add();
        s(b, "shin_f", "leg_f", true, deep).at(0, 7, 0).rot(15, 0, 8).mat("flesh").box(-2, 0, -2, 4, 8, 4).add();
        s(b, "paw_f", "shin_f", true, dark).at(0, 7.5f, 0).mat("flesh").box(-2.5f, 0, -3.5f, 5, 2, 5).add();
        s(b, "claw_f", "paw_f", true, bone).at(0, 1, -3.5f).rot(25, 0, 0).mat("horn").box(-2.5f, -0.5f, -3, 1, 1, 3).box(-0.5f, -0.5f, -3.5f, 1, 1, 4)
                .box(1.5f, -0.5f, -3, 1, 1, 3).add();
        s(b, "leg_b", "body", false, red).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).at(7, 1, 8).rot(-30, 0, -8).mat("flesh")
                .box(-3, -2, -3, 6, 8, 6).add();
        s(b, "shin_b", "leg_b", true, deep).at(0, 7, 0).rot(55, 0, 4).mat("flesh").box(-2, 0, -2, 4, 6, 4).add();
        s(b, "paw_b", "shin_b", true, dark).at(0, 6, 0).rot(-20, 0, 0).mat("flesh").box(-2.5f, 0, -3.5f, 5, 2, 5).add();
        s(b, "claw_b", "paw_b", true, bone).at(0, 1, -3.5f).mat("horn").box(-2, -0.5f, -2, 1, 1, 2).box(1, -0.5f, -2, 1, 1, 2).add();
        p(b, "tail", "body", Role.TAIL, red).at(0, -3, 11).rot(-15, 0, 0).mat("flesh").box(-3, -3, 0, 6, 6, 8).add();
        p(b, "tail2", "tail", Role.TAIL, deep).at(0, 0, 8).rot(10, 0, 0).mat("flesh").box(-2, -2, 0, 4, 4, 7).add();
        p(b, "tail3", "tail2", Role.TAIL, dark).at(0, 0, 7).rot(10, 0, 0).mat("flesh").box(-1.5f, -1.5f, 0, 3, 3, 6)
                .box(-0.5f, -3.5f, 2, 1, 2, 3).add();
        p(b, "orb", "body", Role.FLOAT, glow).at(0, -16, -4).mat("energy").glow("full").box(-1.5f, -1.5f, -1.5f, 3, 3, 3).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Xarpus
    // ------------------------------------------------------------------------------------------

    /**
     * Xarpus: a towering, emaciated plague-beast hunched over its throne room - skeletal arms, leathery wings, a
     * long neck carrying a beaked skull, and poison welling from its pustules and maw.
     */
    static ModelSpec xarpus() {
        int skin = 0x6E7E62;
        int dark = 0x3A4632;
        int deep = 0x52604A;
        int poison = 0x9AE040;
        BestiaryRad.Frame f = new BestiaryRad.Frame("xarpus", 1.0f, 1.3f).colors(skin, dark, deep, poison)
                .legs(8, 7, 5, 5).torso(16, 14, 11, 35).arms(9, 10, 3, 3).head(7, 6, 8).hips(12, 5, 9);
        f.mat = "flesh";
        f.claws = 3;
        f.clawLen = 4;
        f.clawColor = 0xD8D0B0;
        f.toes = 3;
        f.neckLen = 6;
        f.jaw = false;
        f.headGlow = "eyes";
        f.elbowBend = 40;
        ModelSpec.Builder b = f.build();
        p(b, "ribs", "torso", 0xC8C0A0).at(0, -10, -5.6f).mat("bone").box(-5, 0, -0.5f, 10, 1, 1).box(-5, 2, -0.5f, 10, 1, 1)
                .box(-4, 4, -0.5f, 8, 1, 1).box(-0.5f, -1, -0.8f, 1, 7, 1).add();
        p(b, "hump", "torso", dark).at(0, -13, 3).rot(-15, 0, 0).mat("flesh").box(-6, -4, -3, 12, 5, 7).add();
        spikes(b, "vertebrae", "hump", 0xC8C0A0, "bone", -4, new float[][]{{0, -2, 3}, {0, 1, 3}, {0, 4, 2}}, -10);
        for (int i = 0; i < 4; i++) {
            float x = (i % 2 == 0 ? -1 : 1) * (4 + i);
            p(b, "pustule" + i, "torso", poison).at(x, -12 + i * 2.5f, (i < 2 ? -5.4f : 5.4f)).mat("slime").glow("full")
                    .box(-1, -1, -1, 2, 2, 2).add();
        }
        // beaked skull
        p(b, "beak", "head", 0xC8B880).at(0, 0, -6).rot(10, 0, 0).mat("horn").box(-2, -1.5f, -5, 4, 3, 5).box(-1, -0.5f, -7, 2, 2, 2).add();
        p(b, "beak_low", "head", Role.JAW, 0xA89860).at(0, 1.5f, -6).rot(18, 0, 0).mat("horn").box(-1.5f, 0, -4, 3, 1, 4).add();
        p(b, "spit", "beak_low", poison).at(0, 0, -3).mat("slime").glow("full").box(-1, -0.5f, -1, 2, 2, 2).add();
        p(b, "skull_crest", "head", dark).at(0, -4, 0).rot(-40, 0, 0).mat("flesh").box(-1, -4, -2, 2, 4, 6).add();
        s(b, "skull_horn", "head", false, 0xC8C0A0).at(3, -3, 1).rot(-60, 0, -20).mat("bone").box(-0.5f, -5, -0.5f, 1, 5, 1).add();
        // leathery wings, folded half open
        s(b, "wing", "torso", false, dark).roles(Role.WING_LEFT, Role.WING_RIGHT).at(6, -12, 4).rot(-20, -25, -25).mat("bone")
                .box(0, -1, -1, 14, 2, 2).add();
        s(b, "wing_mem", "wing", true, shade(skin, 0.75f)).at(0, 1, 0).mat("membrane").box(0, 0, -0.5f, 14, 12, 1).box(3, 12, -0.5f, 8, 3, 1).add();
        s(b, "wing_finger", "wing", true, 0xC8C0A0).at(14, 0, 0).rot(0, 0, 15).mat("bone").box(-1, -1, -0.5f, 1, 15, 1).add();
        s(b, "wing_finger2", "wing", true, 0xC8C0A0).at(7, 1, 0).rot(0, 0, 8).mat("bone").box(-0.5f, 0, -0.5f, 1, 13, 1).add();
        s(b, "wing_claw", "wing", true, 0xD8D0B0).at(14, -1, 0).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        p(b, "loin", "hips", 0x4A3A2A).at(0, 0, -4.6f).rot(-6, 0, 0).mat("cloth").box(-3, 0, -0.5f, 6, 7, 1).add();
        p(b, "poison_pool", null, poison).at(0, 24, 0).mat("slime").glow("cracks").accent(0xC0FF60).box(-9, -0.5f, -9, 18, 1, 18).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Verzik Vitur
    // ------------------------------------------------------------------------------------------

    /** Verzik Vitur, phase one: the vampyre queen at ease upon her bone throne, in a high-collared gown and crown. */
    static ModelSpec verzikP1() {
        int gown = 0x2A1A2E;
        int gownDark = 0x180E1C;
        int skin = 0xD8D0DA;
        int gold = 0xC8A040;
        int boneC = 0xD8CCB4;
        ModelSpec.Builder b = ModelSpec.builder("verzik_vitur").shadow(1.1f);
        // throne of bone and skulls
        p(b, "throne", null, boneC).at(0, 24, 1).mat("bone").box(-10, -9, -8, 20, 9, 15).add();
        p(b, "throne_step", "throne", shade(boneC, 0.8f)).at(0, 0, -8).mat("bone").box(-11, -2, -3, 22, 2, 3).add();
        p(b, "throne_back", "throne", boneC).at(0, -9, 6).rot(-6, 0, 0).mat("bone").box(-9, -24, -1, 18, 24, 3).add();
        p(b, "throne_ribs", "throne_back", shade(boneC, 0.85f)).at(0, -20, -1.2f).mat("bone").box(-7, 0, -0.5f, 14, 1, 1)
                .box(-7, 4, -0.5f, 14, 1, 1).box(-7, 8, -0.5f, 14, 1, 1).box(-7, 12, -0.5f, 14, 1, 1).box(-0.5f, 0, -0.8f, 1, 16, 1).add();
        Kit.P skulls = p(b, "skulls", "throne_back", 0xEDE4D0).at(0, -24, 0).mat("bone").pattern("skull");
        for (int i = 0; i < 5; i++) {
            skulls.box(-10 + i * 4, -3 - (i == 2 ? 2 : 0), -1, 3, 3, 3);
        }
        skulls.add();
        s(b, "throne_arm", "throne", false, boneC).at(10, -9, -1).mat("bone").box(-1.5f, -6, -7, 3, 6, 13).add();
        s(b, "arm_skull", "throne_arm", true, 0xEDE4D0).at(0, -6, -7).mat("bone").pattern("skull").box(-2, -3, -2, 4, 3, 4).add();
        // seated figure
        p(b, "body", "throne", Role.BODY, gown).accent(gold).at(0, -9, 1).mat("cloth").box(-4.5f, -11, -3, 9, 11, 6).add();
        p(b, "bodice", "body", gownDark).at(0, -11, -3).mat("cloth").box(-3.5f, 0, -0.6f, 7, 7, 1).add();
        p(b, "lap", "body", gown).at(0, -0.5f, -2).mat("cloth").box(-5, -1, -8, 10, 3, 9).add();
        p(b, "skirt", "lap", gownDark).at(0, 1, -8).rot(8, 0, 0).mat("cloth").box(-5.5f, 0, -1, 11, 8, 2).add();
        s(b, "skirt_fold", "skirt", false, gown).at(3.5f, 0, -0.5f).rot(0, 0, -6).mat("cloth").box(-1.5f, 0, -1, 3, 8, 1).add();
        p(b, "belt", "body", gold).at(0, -1, 0).mat("metal").box(-5, -1, -3.5f, 10, 1, 7).add();
        p(b, "collar", "body", gownDark).accent(gold).at(0, -11, 1).rot(-15, 0, 0).mat("cloth")
                .box(-6, -6, 0, 12, 7, 1).box(-6, -6, -3, 1, 6, 3).box(5, -6, -3, 1, 6, 3).add();
        p(b, "collar_trim", "collar", gold).at(0, -6, 0).mat("metal").box(-6, -1, -3, 12, 1, 4).add();
        s(b, "shoulder", "body", false, gownDark).at(4.5f, -10, 0).mat("cloth").box(-1, -1, -2.5f, 3, 3, 5).add();
        s(b, "arm", "body", false, gown).roles(Role.ARM_LEFT, Role.ARM_RIGHT).at(5.5f, -9, 0).rot(-30, 0, -6).mat("cloth")
                .box(-1, 0, -1, 2, 6, 2).add();
        s(b, "forearm", "arm", true, skin).at(0, 6, 0).rot(-50, 0, 0).mat("skin").box(-1, 0, -1, 2, 5, 2).add();
        s(b, "nails", "forearm", true, 0x3A1A2A).at(0, 5, 0).mat("horn").box(-1, 0, -1, 2, 2, 1).add();
        p(b, "neck", "body", skin).at(0, -11, 0).mat("skin").box(-1.5f, -2, -1.5f, 3, 2, 3).add();
        p(b, "head", "neck", Role.HEAD, skin).accent(0xE02020).at(0, -2, 0).mat("skin").pattern("eyes").glow("eyes")
                .box(-3.5f, -7, -3.5f, 7, 7, 7).add();
        p(b, "hair", "head", 0x16101A).at(0, -7, 0).mat("hair").box(-4, -1, -4, 8, 2, 8).box(-4, 0, 2, 8, 10, 2)
                .box(-4, 0, -3, 1, 7, 5).box(3, 0, -3, 1, 7, 5).add();
        p(b, "crown", "head", gold).at(0, -8, 0).mat("metal").box(-3.5f, -1, -3.5f, 7, 1, 7).box(-3, -3, -3.6f, 1, 2, 1)
                .box(-0.5f, -4, -3.6f, 1, 3, 1).box(2, -3, -3.6f, 1, 2, 1).add();
        p(b, "crown_gem", "crown", 0xE02030).at(0, -2, -3.8f).mat("energy").glow("full").box(-0.5f, -0.5f, -0.4f, 1, 1, 1).add();
        teeth(b, "fangs", "head", 0xFFFFFF, -1, 1, -1.4f, -3.6f, 2, 1);
        s(b, "boot", "skirt", false, gownDark).at(2.5f, 8, 0).mat("leather").box(-1.5f, 0, -3, 3, 1, 4).add();
        return b.build();
    }

    /** Verzik Vitur, phase two: risen as a gaunt, bloated vampyre horror with exposed ribs, long talons and wild hair. */
    static ModelSpec verzikP2() {
        int pale = 0xBCAEBC;
        int dark = 0x4A2A4A;
        int deep = 0x7A5A7A;
        BestiaryRad.Frame f = new BestiaryRad.Frame("verzik_vitur_p2", 1.0f, 1.2f).colors(pale, dark, deep, 0xE02020)
                .legs(8, 8, 4, 3.5f).torso(15, 15, 12, 20).arms(9, 11, 3, 3).head(8, 8, 8).hips(10, 5, 8);
        f.mat = "skin";
        f.claws = 4;
        f.clawLen = 5;
        f.clawColor = 0x2A1220;
        f.toes = 3;
        f.clawMat = "horn";
        f.handW = 4;
        f.elbowBend = 30;
        f.headGlow = "eyes";
        ModelSpec.Builder b = f.build();
        p(b, "ribcage", "torso", 0xE8DCE0).at(0, -13, -6.4f).mat("bone").box(-6, 0, -0.5f, 12, 1, 1).box(-6, 2, -0.5f, 12, 1, 1)
                .box(-5, 4, -0.5f, 10, 1, 1).box(-4, 6, -0.5f, 8, 1, 1).box(-0.5f, -1, -0.8f, 1, 9, 1).add();
        p(b, "gut", "torso", shade(pale, 0.85f)).at(0, -4, 0).mat("flesh").pattern("spots").box(-7.5f, -4, -6.5f, 15, 6, 13).add();
        spikes(b, "spines", "torso", dark, "horn", -14, new float[][]{{-4, 4, 4}, {0, 4.5f, 6}, {4, 4, 4}, {-2, 2, 3}, {2, 2, 3}}, -40);
        p(b, "hair", "head", 0x14101A).at(0, -6, 0).mat("hair").box(-4.5f, -1, -4.5f, 9, 2, 9).box(-4.5f, 0, 3, 9, 13, 2)
                .box(-5, 0, -3, 1, 10, 6).box(4, 0, -3, 1, 10, 6).add();
        p(b, "hair_wild", "hair", 0x14101A).at(0, 13, 4).rot(25, 0, 0).mat("hair").box(-4, 0, -1, 8, 6, 2).add();
        teeth(b, "fangs", "jaw", 0xFFFFFF, -2, 2, 0, -2.3f, 4, -2);
        s(b, "shoulder_bone", "shoulder", true, 0xE8DCE0).at(2, -3, 0).rot(0, 0, -30).mat("bone").box(-1, -3, -1, 2, 3, 2).add();
        p(b, "rags", "hips", dark).at(0, 0, 0).mat("cloth").box(-5.5f, 0, -4.5f, 4, 7, 1).box(1.5f, 0, -4.5f, 4, 6, 1)
                .box(-5, 0, 3.5f, 10, 8, 1).add();
        s(b, "rag_side", "hips", false, shade(dark, 0.8f)).at(5, 0, 0).rot(0, 0, -10).mat("cloth").box(-0.5f, 0, -3, 1, 6, 6).add();
        p(b, "blood_orb", "hand_r", 0xE02020).at(0, 7, -1).mat("energy").glow("full").box(-1.5f, -1.5f, -1.5f, 3, 3, 3).add();
        return b.build();
    }

    /**
     * Verzik Vitur, phase three: her true form - a monstrous blood spider, its bulbous marked abdomen and eight
     * spined legs carrying her pale, clawed upper body and screaming face.
     */
    static ModelSpec verzikP3() {
        int body = 0x5A1A2A;
        int dark = 0x2A0A14;
        int legC = 0x3A1020;
        int pale = 0xC8B8C8;
        int red = 0xE02838;
        ModelSpec.Builder b = ModelSpec.builder("verzik_vitur_p3").shadow(1.7f);
        p(b, "thorax", null, Role.BODY, body).accent(red).at(0, 11, 0).mat("chitin").box(-8, -6, -8, 16, 10, 16).add();
        p(b, "thorax_plate", "thorax", shade(body, 1.2f)).at(0, -6, 0).mat("chitin").box(-6, -1, -6, 12, 1, 12).add();
        p(b, "abdomen", "thorax", Role.TAIL, body).accent(red).at(0, -3, 8).rot(-15, 0, 0).mat("chitin").box(-10, -9, 0, 20, 16, 17).add();
        p(b, "abd_top", "abdomen", shade(body, 1.15f)).at(0, -9, 8).mat("chitin").box(-8, -2, -7, 16, 2, 14).add();
        p(b, "abd_mark", "abd_top", red).at(0, -2.2f, 0).mat("energy").glow("full").box(-1.5f, -0.5f, -5, 3, 1, 10)
                .box(-4, -0.5f, -2, 8, 1, 2).box(-3, -0.5f, 3, 6, 1, 1).add();
        p(b, "abd_bands", "abdomen", dark).at(0, 0, 0).mat("chitin").box(-10.5f, -8, 5, 21, 15, 1).box(-10.5f, -8, 11, 21, 15, 1).add();
        p(b, "spinneret", "abdomen", dark).at(0, -1, 17).mat("chitin").box(-3, -3, 0, 6, 6, 3).add();
        spikes(b, "abd_spikes", "abdomen", 0xD8C8C0, "horn", -9, new float[][]{{-6, 3, 4}, {6, 3, 4}, {-6, 10, 3}, {6, 10, 3}}, 0);
        // her upper body rising from the front of the thorax
        p(b, "torso", "thorax", pale).accent(red).at(0, -6, -5).rot(-10, 0, 0).mat("skin").box(-4.5f, -11, -3, 9, 11, 6).add();
        p(b, "ribs", "torso", 0xE8DCE0).at(0, -9, -3.4f).mat("bone").box(-3.5f, 0, -0.5f, 7, 1, 1).box(-3.5f, 2, -0.5f, 7, 1, 1)
                .box(-3, 4, -0.5f, 6, 1, 1).add();
        p(b, "head", "torso", Role.HEAD, pale).accent(red).at(0, -11, 0).mat("skin").pattern("eyes").glow("eyes")
                .box(-4, -8, -4, 8, 8, 7).add();
        p(b, "jaw", "head", Role.JAW, shade(pale, 0.7f)).accent(red).at(0, -1, -2).rot(20, 0, 0).mat("skin").glow("maw")
                .box(-3, 0, -2.5f, 6, 3, 4).add();
        p(b, "hair", "head", 0x14101A).at(0, -8, 0).mat("hair").box(-4.5f, -1, -4.5f, 9, 2, 9).box(-4.5f, 0, 2.5f, 9, 13, 2)
                .box(-5, 0, -3, 1, 9, 6).box(4, 0, -3, 1, 9, 6).add();
        s(b, "arm", "torso", false, pale).roles(Role.ARM_LEFT, Role.ARM_RIGHT).at(5, -10, 0).rot(-40, 0, -35).mat("skin")
                .box(-1, -1, -1, 2, 8, 2).add();
        s(b, "forearm", "arm", true, pale).at(0, 8, 0).rot(-50, 0, 20).mat("skin").box(-1, 0, -1, 2, 7, 2).add();
        s(b, "talon", "forearm", true, dark).at(0, 7, 0).mat("horn").box(-1, 0, -1, 1, 4, 1).box(0.2f, 0, -1, 1, 5, 1).box(-1, 0, 0.2f, 1, 4, 1).add();
        s(b, "fang", "thorax", false, 0xE8DCD0).roles(Role.JAW, Role.JAW).at(3, 3, -8).rot(-20, 0, 0).mat("horn")
                .box(-1, 0, -1, 2, 5, 2).box(-0.5f, 5, -1.5f, 1, 2, 1).add();
        s(b, "eye_cluster", "thorax", false, red).at(5, -3, -8.2f).mat("energy").glow("full").box(-1, -1, -0.5f, 2, 2, 1)
                .box(-2.5f, 1.5f, -0.4f, 1, 1, 1).add();
        // eight legs: thick upper segment arching high, spined knee, long lower leg
        float[] zs = {-6, -2, 2, 6};
        float[] yaws = {-35, -10, 10, 35};
        for (int i = 0; i < 4; i++) {
            String n = "leg" + i;
            s(b, n, "thorax", false, legC).roles(Role.LEG_EXTRA, Role.LEG_EXTRA).accent(red).at(8, -2, zs[i]).rot(0, yaws[i], -40)
                    .mat("chitin").box(0, -1.5f, -1.5f, 12, 3, 3).add();
            s(b, n + "_knee", n, true, dark).at(12, 0, 0).mat("chitin").box(-1.5f, -2, -2, 3, 4, 4).box(-0.5f, -4, -0.5f, 1, 2, 1).add();
            s(b, n + "_shin", n, true, body).at(12, 0, 0).rot(0, 0, 25).mat("chitin").box(-1, 0, -1, 2, 19, 2).add();
            s(b, n + "_tip", n + "_shin", true, 0xD8C8C0).at(0, 19, 0).mat("horn").box(-0.5f, 0, -0.5f, 1, 3, 1).add();
        }
        return b.build();
    }
}
