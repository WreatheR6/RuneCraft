package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.BestiaryWldBeasts.arachLeg;
import static com.runecraft.osrs.content.bestiary.BestiaryWldFig.dk;
import static com.runecraft.osrs.content.bestiary.BestiaryWldFig.side;
import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/** Undead, spirits, insects, sea creatures, TzHaar, implings and crabs of the everyday world. */
final class BestiaryWldMisc {
    private BestiaryWldMisc() {
    }

    static List<ModelSpec> all() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(skeleton("skeleton"));
        l.add(tzhaar("tzhaar_hur_citizen", "hur"));
        l.add(tzhaar("tzhaar_ket", "ket"));
        l.add(tzhaar("tzhaar_xil", "xil"));
        l.add(tzhaar("tzhaar_mej", "mej"));
        String[][] imps = {
                {"baby_impling", "C8A880", "F0E0C0"}, {"young_impling", "8AB060", "E0F0B0"}, {"gourmet_impling", "A07040", "F0C080"},
                {"earth_impling", "6A8A3A", "C0A060"}, {"essence_impling", "5A7AC8", "B0D0FF"}, {"eclectic_impling", "9A5AB0", "F0B0FF"},
                {"nature_impling", "4A9A3A", "B0FF80"}, {"magpie_impling", "2A2A30", "F0F0F0"}, {"ninja_impling", "1E1E24", "C02020"},
                {"dragon_impling", "B0302A", "FFB040"}, {"lucky_impling", "E8E0C0", "FFE040"}};
        for (String[] i : imps) {
            l.add(impling(i[0], Integer.parseInt(i[1], 16), Integer.parseInt(i[2], 16)));
        }
        l.add(crab("ammonite_crab", 0xB07A5A, 0xE0C8A0, true));
        l.add(dagannoth("wallasalki", 0x5A6A8A, 0x3A4664, 0xA8B4C8));
        return l;
    }

    static List<ModelSpec> overrides() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(ghost("ghost"));
        l.add(icefiend("icefiend"));
        l.add(kalphite("kalphite"));
        l.add(dagannoth("dagannoth", 0x4A6A50, 0x2E4A36, 0x9AA878));
        l.add(cyclops("cyclops"));
        l.add(crab("sand_crab", 0xD8B880, 0xB89058, false));
        return l;
    }

    // ------------------------------------------------------------------------------------------
    // Skeleton
    // ------------------------------------------------------------------------------------------

    /**
     * OSRS skeleton: a gaunt frame of separate bones (pelvis, vertebrae, ribcage with sternum, clavicles, a skull with
     * a hinged jaw) on knobbly limbs, wielding a notched rusty scimitar and a battered round shield.
     */
    static ModelSpec skeleton(String id) {
        int bone = 0xE4DFC8;
        int dark = 0xB8B098;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.5f);
        part(b, "pelvis", null, Role.BODY, bone).at(0, 12, 0).mat("bone").box(-3.5f, -1.5f, -2, 7, 3, 4).add();
        sym(b, "hip_wing", "pelvis", false, dark).at(3, -1.5f, 0).rot(0, 0, -20).mat("bone").box(-1, -1.5f, -1.5f, 2, 2, 3).add();
        part(b, "spine", "pelvis", Role.STATIC, dark).at(0, -1.5f, 1).rot(6, 0, 0).mat("bone")
                .box(-1, -5, -1, 2, 5, 2).box(-1.5f, -2, -1.5f, 3, 1, 3).box(-1.5f, -4.5f, -1.5f, 3, 1, 3).add();
        part(b, "ribcage", "spine", Role.STATIC, bone).at(0, -5, -0.5f).rot(4, 0, 0).mat("bone")
                .box(-4, -6, -2.5f, 8, 1, 5).box(-4, -4, -2.5f, 8, 1, 5).box(-3.5f, -2, -2, 7, 1, 4).add();
        part(b, "sternum", "ribcage", Role.STATIC, dk(bone, 0.95f)).at(0, -6, -2.6f).mat("bone").box(-0.5f, 0, -0.5f, 1, 5, 1).add();
        sym(b, "rib_side", "ribcage", false, dk(bone, 0.9f)).at(3.5f, -6, 0).mat("bone").box(0, 0, -2, 1, 5, 4, -0.1f).add();
        part(b, "spine_back", "ribcage", Role.STATIC, dark).at(0, -7, 2).mat("bone").box(-1, 0, -0.5f, 2, 7, 1).add();
        part(b, "clavicle", "ribcage", Role.STATIC, bone).at(0, -7, -0.5f).mat("bone").box(-5, -0.5f, -1, 10, 1, 2).add();
        part(b, "neck", "ribcage", Role.STATIC, dark).at(0, -7.5f, 0).mat("bone").box(-0.5f, -2, -0.5f, 1, 2, 1, 0.2f).add();
        part(b, "head", "ribcage", Role.HEAD, bone).at(0, -9, 0).mat("bone").pattern("skull").box(-3.5f, -6, -3.5f, 7, 6, 7).add();
        part(b, "cheekbones", "head", Role.STATIC, dk(bone, 0.95f)).at(0, -2, -3.5f).mat("bone").box(-3, 0, -0.6f, 6, 1, 1).add();
        part(b, "jaw", "head", Role.JAW, dk(bone, 0.92f)).at(0, -0.5f, -1).rot(8, 0, 0).mat("bone").box(-2.5f, 0, -2.7f, 5, 2, 4).add();
        part(b, "brow_ridge", "head", Role.STATIC, bone).at(0, -4, -3.5f).mat("bone").box(-3.5f, -0.5f, -0.6f, 7, 1, 1).add();
        for (boolean left : new boolean[]{true, false}) {
            Role ar = left ? Role.ARM_LEFT : Role.ARM_RIGHT;
            float pitch = left ? -20 : -30;
            side(part(b, "arm", "ribcage", ar, bone).at(5, -6, 0).rot(pitch, 0, -8).mat("bone")
                    .box(-1.5f, -1, -1.5f, 3, 2, 3).box(-0.5f, 0, -0.5f, 1, 5, 1, 0.2f), left);
            side(part(b, "elbow", "arm*", Role.STATIC, dark).at(0, 5, 0).mat("bone").box(-1, -0.5f, -1, 2, 2, 2), left);
            side(part(b, "forearm", "arm*", Role.STATIC, bone).at(0, 5.5f, 0).rot(left ? -30 : -55, 0, 0).mat("bone")
                    .box(-1, 0, -0.5f, 1, 5, 1).box(0.2f, 0, -0.5f, 1, 5, 1, -0.15f), left);
            side(part(b, "hand", "forearm*", Role.STATIC, dark).at(0, 5, 0).rot(left ? 0 : -10, 0, 0).mat("bone")
                    .box(-1, 0, -1, 2, 1, 2).box(-1, 1, -1, 1, 2, 1, -0.1f).box(0.2f, 1, -1, 1, 2, 1, -0.1f), left);
            Role lr = left ? Role.LEG_FRONT_LEFT : Role.LEG_FRONT_RIGHT;
            side(part(b, "leg", "pelvis", lr, bone).at(2, 1, 0).rot(0, 0, -3).mat("bone")
                    .box(-1.5f, -1, -1.5f, 3, 2, 3).box(-0.5f, 1, -0.5f, 1, 4, 1, 0.25f), left);
            side(part(b, "knee", "leg*", Role.STATIC, dark).at(0, 5, 0).mat("bone").box(-1, 0, -1.2f, 2, 2, 2), left);
            side(part(b, "shin", "leg*", Role.STATIC, bone).at(0, 7, 0).rot(0, 0, 3).mat("bone")
                    .box(-0.5f, 0, -0.5f, 1, 4, 1, 0.2f).box(0.4f, 0, 0, 1, 4, 1, -0.2f), left);
            side(part(b, "foot", "shin*", Role.STATIC, dark).at(0, 4, 0).mat("bone")
                    .box(-1, 0, -3, 2, 1, 4).box(-1.2f, 0, -4, 1, 1, 1, -0.1f).box(0.2f, 0, -4, 1, 1, 1, -0.1f), left);
        }
        // rusty scimitar in the right hand, battered shield on the left forearm
        int rust = 0x8A6A4A;
        part(b, "weapon", "hand_r", Role.STATIC, 0x3A2A1A).at(0, 1.5f, -0.3f).rot(96, 0, -14).mat("leather").box(-0.5f, -1, -0.5f, 1, 3, 1).add();
        part(b, "guard", "weapon", Role.STATIC, dk(rust, 0.8f)).at(0, -1, 0).mat("metal").box(-1.5f, -1, -0.5f, 3, 1, 1).add();
        part(b, "blade", "weapon", Role.STATIC, rust).at(0, -2, 0).mat("metal").box(-0.5f, -4, -1, 1, 4, 2).add();
        part(b, "blade_mid", "blade", Role.STATIC, rust).at(0, -4, 0).rot(-15, 0, 0).mat("metal").box(-0.5f, -3, -1.2f, 1, 3, 2).add();
        part(b, "blade_tip", "blade_mid", Role.STATIC, dk(rust, 1.15f)).at(0, -3, 0).rot(-22, 0, 0).mat("metal").box(-0.5f, -3, -1, 1, 3, 2, -0.1f).add();
        part(b, "shield_l", "forearm_l", Role.STATIC, 0x6A4A2A).at(1.4f, 2, -0.5f).mat("wood")
                .box(0, -4, -3, 1, 8, 6).box(0, -3, -4, 1, 6, 8).add();
        part(b, "shield_rim", "shield_l", Role.STATIC, 0x5A5A5A).at(0.4f, 0, 0).mat("metal")
                .box(0, -4.5f, -3, 1, 1, 6).box(0, 3.5f, -3, 1, 1, 6).box(0, -3, -4.5f, 1, 6, 1).box(0, -3, 3.5f, 1, 6, 1).add();
        part(b, "shield_boss", "shield_l", Role.STATIC, 0x7A7A7A).at(1, 0, 0).mat("metal").box(0, -1, -1, 1, 2, 2, 0.2f).add();
        part(b, "shield_crack", "shield_l", Role.STATIC, 0x2A1A10).at(1.05f, -2, 1).rot(25, 0, 0).mat("wood").box(0, 0, -0.25f, 1, 4, 1, -0.4f).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Spirits
    // ------------------------------------------------------------------------------------------

    /** OSRS ghost: a pale floating shroud tapering to a wispy tail, a hood with a dark hollow face, arms reaching out. */
    static ModelSpec ghost(String id) {
        int c = 0xDDE6EC;
        int d = 0xAEBCC8;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.35f);
        part(b, "body", null, Role.FLOAT, c).at(0, 9, 0).rot(8, 0, 0).mat("ghost").box(-4.5f, -10, -3, 9, 10, 6).add();
        part(b, "shroud", "body", Role.STATIC, dk(c, 0.96f)).at(0, -1, 0).mat("ghost").box(-5, 0, -3.5f, 10, 4, 7).add();
        part(b, "tail_a", "shroud", Role.TAIL, d).at(0, 4, 0.5f).rot(14, 0, 0).mat("ghost").box(-4, 0, -2.5f, 8, 4, 5).add();
        part(b, "tail_b", "tail_a", Role.STATIC, dk(d, 0.95f)).at(0, 4, 0.5f).rot(16, 0, 6).mat("ghost").box(-2.5f, 0, -1.5f, 5, 4, 3).add();
        part(b, "tail_c", "tail_b", Role.STATIC, dk(d, 0.9f)).at(0, 4, 0).rot(20, 0, 10).mat("ghost").box(-1, 0, -1, 2, 3, 2).add();
        part(b, "tatters", "shroud", Role.STATIC, d).at(0, 4, -3.5f).mat("ghost")
                .box(-5, 0, -0.5f, 2, 3, 1).box(-1, 0, -0.5f, 2, 2, 1).box(3, 0, -0.5f, 2, 3, 1).add();
        part(b, "hood", "body", Role.STATIC, c).at(0, -10, 0.5f).mat("ghost").box(-4, -7, -3.5f, 8, 7, 7).add();
        part(b, "hood_peak", "hood", Role.STATIC, dk(c, 0.95f)).at(0, -7, 1).rot(-25, 0, 0).mat("ghost").box(-2, -2, -1.5f, 4, 3, 4).add();
        part(b, "head", "hood", Role.HEAD, 0x1A1E26).accent(0xA0E0FF).at(0, -3, -3.6f).mat("ghost").pattern("eyes").glow("eyes")
                .box(-3, -3, -0.2f, 6, 5, 1).add();
        part(b, "hood_rim", "hood", Role.STATIC, dk(c, 0.9f)).at(0, -7, -3.6f).mat("ghost")
                .box(-4, 0, -0.8f, 8, 1, 1).box(-4, 1, -0.8f, 1, 6, 1).box(3, 1, -0.8f, 1, 6, 1).add();
        for (boolean left : new boolean[]{true, false}) {
            side(part(b, "arm", "body", left ? Role.ARM_LEFT : Role.ARM_RIGHT, c).at(4.5f, -9, -0.5f).rot(-70, 0, -10).mat("ghost")
                    .box(-1.5f, -1, -1.5f, 3, 6, 3), left);
            side(part(b, "sleeve", "arm*", Role.STATIC, d).at(0, 5, 0).rot(10, 0, 0).mat("ghost").box(-2, 0, -2, 4, 3, 4), left);
            side(part(b, "sleeve_rag", "sleeve*", Role.STATIC, dk(d, 0.9f)).at(0, 2, 2).rot(30, 0, 0).mat("ghost").box(-1.5f, 0, -0.5f, 3, 3, 1), left);
            side(part(b, "hand", "sleeve*", Role.STATIC, 0xC8D8E0).at(0, 3, 0).mat("ghost").box(-1, 0, -1, 2, 2, 2), left);
            side(part(b, "fingers", "hand*", Role.STATIC, 0xB8C8D4).at(0, 2, -0.5f).rot(25, 0, 0).mat("ghost")
                    .box(-1, 0, -0.5f, 1, 2, 1, -0.1f).box(0.2f, 0, -0.5f, 1, 2, 1, -0.1f), left);
        }
        return b.build();
    }

    /** Icefiend: a floating knot of jagged ice shards around a glowing frosty core, with horned crown and icy claws. */
    static ModelSpec icefiend(String id) {
        int ice = 0xA8D8F0;
        int deep = 0x6AA8D8;
        int glow = 0xC0F8FF;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.4f);
        part(b, "body", null, Role.FLOAT, ice).at(0, 14, 0).mat("ice").box(-3.5f, -6, -3, 7, 7, 6).add();
        part(b, "core", "body", Role.STATIC, glow).at(0, -3, -3).mat("energy").glow("full").box(-1.5f, -1.5f, -0.6f, 3, 3, 1).add();
        part(b, "belly_shard", "body", Role.TAIL, deep).at(0, 1, 0).rot(10, 0, 0).mat("ice").box(-2.5f, 0, -2, 5, 3, 4).add();
        part(b, "tail_shard", "belly_shard", Role.STATIC, ice).at(0, 3, 0).rot(15, 0, 0).mat("ice").box(-1, 0, -1, 2, 4, 2).add();
        part(b, "head", "body", Role.HEAD, ice).accent(0x40E0FF).at(0, -6, -0.5f).mat("ice").pattern("eyes").glow("eyes")
                .box(-3, -5, -3, 6, 5, 5).add();
        part(b, "jaw", "head", Role.JAW, deep).at(0, -0.5f, -2).mat("ice").box(-2.5f, 0, -1.5f, 5, 2, 3).add();
        for (int i = 0; i < 3; i++) {
            part(b, "crown" + i, "head", Role.STATIC, i == 1 ? 0xE0F8FF : ice).at(-2 + i * 2, -5, -0.5f + (i == 1 ? 0.5f : 0))
                    .rot(-15, 0, -25 + i * 25).mat("ice").box(-0.5f, -4 + (i == 1 ? -1 : 0), -0.5f, 1, 4 + (i == 1 ? 1 : 0), 1, 0.2f).add();
        }
        for (boolean left : new boolean[]{true, false}) {
            side(part(b, "horn", "head", Role.STATIC, deep).at(3, -4, 0).rot(0, 0, -50).mat("ice").box(-0.5f, -3, -0.5f, 1, 3, 1, 0.2f), left);
            side(part(b, "shoulder", "body", Role.STATIC, deep).at(3.5f, -5, 0).rot(0, 0, -30).mat("ice").box(-1, -3, -1.5f, 2, 3, 3), left);
            side(part(b, "arm", "body", left ? Role.ARM_LEFT : Role.ARM_RIGHT, ice).at(4, -4, 0).rot(-30, 0, -20).mat("ice")
                    .box(-1, 0, -1, 2, 5, 2), left);
            side(part(b, "claw", "arm*", Role.STATIC, 0xE0F8FF).at(0, 5, 0).rot(-20, 0, 0).mat("ice")
                    .box(-1, 0, -1, 2, 2, 2).box(-1, 2, -1.5f, 1, 2, 1, -0.1f).box(0.3f, 2, -1.5f, 1, 2, 1, -0.1f), left);
            side(part(b, "back_spike", "body", Role.STATIC, 0xD8F4FF).at(2, -5, 3).rot(-40, 20, -20).mat("ice").box(-0.5f, -4, -0.5f, 1, 4, 1, 0.3f), left);
        }
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Kalphite & dagannoth
    // ------------------------------------------------------------------------------------------

    /**
     * Kalphite (workers, soldiers and guardians share it at different sizes): a sandy-gold armoured insect with a
     * wedge head, sickle mandibles and antennae, a plated thorax, a heavy banded abdomen and six jointed legs.
     */
    static ModelSpec kalphite(String id) {
        int shell = 0xC89A50;
        int dark = 0x8A6A30;
        int light = 0xE8C478;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.0f);
        part(b, "thorax", null, Role.BODY, shell).accent(dark).at(0, 15, -2).rot(-10, 0, 0).mat("chitin").box(-4, -4, -5, 8, 7, 9).add();
        part(b, "thorax_plate", "thorax", Role.STATIC, light).at(0, -4, -0.5f).mat("chitin").box(-3.5f, -1, -4, 7, 1, 8).add();
        part(b, "thorax_ridge", "thorax_plate", Role.STATIC, dark).at(0, -1, 0).mat("chitin").box(-0.5f, -1, -3.5f, 1, 1, 7).add();
        part(b, "waist", "thorax", Role.STATIC, dark).at(0, -0.5f, 4).mat("chitin").box(-2, -2, -0.5f, 4, 4, 3).add();
        part(b, "abdomen", "waist", Role.TAIL, shell).accent(dark).at(0, 0, 2).rot(12, 0, 0).mat("chitin").box(-5, -4.5f, 0, 10, 9, 11).add();
        for (int i = 0; i < 3; i++) {
            part(b, "band" + i, "abdomen", Role.STATIC, i % 2 == 0 ? light : dk(shell, 0.85f)).at(0, -4.5f, 1.5f + i * 3.2f).mat("chitin")
                    .box(-4.5f, -0.8f, 0, 9, 1, 2, 0.1f).add();
        }
        part(b, "abdomen_tip", "abdomen", Role.STATIC, dark).at(0, 0, 11).rot(20, 0, 0).mat("chitin").box(-3, -3, -1, 6, 6, 3).add();
        part(b, "stinger", "abdomen_tip", Role.STATIC, 0x3A2A10).at(0, 1, 2).rot(30, 0, 0).mat("horn").box(-0.5f, -0.5f, 0, 1, 1, 3).add();
        part(b, "neck", "thorax", Role.NECK, dark).at(0, -1, -5).mat("chitin").box(-2, -2, -2, 4, 4, 2).add();
        part(b, "head", "neck", Role.HEAD, shell).accent(0x30FF30).at(0, -1, -2).rot(15, 0, 0).mat("chitin").pattern("eyes")
                .box(-3.5f, -3, -5, 7, 5, 5).add();
        part(b, "head_crest", "head", Role.STATIC, light).at(0, -3, -1).rot(-20, 0, 0).mat("chitin").box(-2.5f, -2, -2.5f, 5, 2, 5).add();
        for (boolean left : new boolean[]{true, false}) {
            side(part(b, "mandible", "head", Role.JAW, dark).at(2, 1, -5).rot(0, 25, 0).mat("chitin").box(-1, -0.5f, -3, 2, 1, 3), left);
            side(part(b, "mandible_tip", "mandible*", Role.STATIC, 0x3A2A10).at(0, 0, -3).rot(0, 50, 0).mat("horn").box(-0.5f, -0.5f, -2, 1, 1, 2), left);
            side(part(b, "antenna", "head", Role.STATIC, dark).at(1.5f, -3, -4).rot(-50, -20, 0).mat("chitin").box(-0.5f, -5, -0.5f, 1, 5, 1), left);
            side(part(b, "antenna_tip", "antenna*", Role.STATIC, dark).at(0, -5, 0).rot(40, 0, 0).mat("chitin").box(-0.5f, -3, -0.5f, 1, 3, 1, -0.1f), left);
            side(part(b, "eye", "head", Role.STATIC, 0x20E020).at(3.2f, -1.5f, -3.5f).mat("energy").glow("full").box(-0.5f, -1, -1, 1, 2, 2), left);
        }
        float[] zs = {-4, -1, 2};
        float[] yaws = {45, 5, -35};
        for (int i = 0; i < 3; i++) {
            for (boolean left : new boolean[]{true, false}) {
                arachLeg(b, "leg" + i, left, "thorax", 3.5f, 1.5f, zs[i], yaws[i], 5, -30, 7, 100, shell, dark, "chitin");
            }
        }
        return b.build();
    }

    /**
     * Dagannoth: a hunched green sea-beast on thick hind legs with a long beaked head, rows of spines down its back,
     * pale belly plates, small clawed arms and a heavy tail.
     */
    static ModelSpec dagannoth(String id, int skin, int dark, int belly) {
        int spine = 0xD8D0B0;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.9f);
        part(b, "hips", null, Role.BODY, skin).at(0, 13, 2).mat("scales").box(-4.5f, -3, -3, 9, 6, 7).add();
        part(b, "torso", "hips", Role.STATIC, skin).at(0, -2, -1).rot(35, 0, 0).mat("scales").box(-5, -11, -4, 10, 11, 8).add();
        part(b, "belly", "torso", Role.STATIC, belly).at(0, -1, -4).mat("plates").box(-3.5f, -9, -1, 7, 9, 1).add();
        for (int i = 0; i < 4; i++) {
            part(b, "spine" + i, "torso", Role.STATIC, spine).at(0, -10 + i * 3, 4).rot(-45, 0, 0).mat("horn")
                    .box(-1, -4 + (i == 0 ? -1 : 0), -1, 2, 4 + (i == 0 ? 1 : 0), 2).add();
        }
        part(b, "neck", "torso", Role.NECK, skin).at(0, -11, -1).rot(-25, 0, 0).mat("scales").box(-3, -3, -3, 6, 4, 6).add();
        part(b, "head", "neck", Role.HEAD, skin).accent(0xE0E040).at(0, -2, -2).rot(-15, 0, 0).mat("scales").pattern("eyes")
                .box(-3.5f, -4, -6, 7, 5, 7).add();
        part(b, "beak", "head", Role.STATIC, dk(skin, 0.9f)).at(0, -1, -6).rot(8, 0, 0).mat("scales").box(-2, -2, -5, 4, 3, 5).add();
        part(b, "beak_tip", "beak", Role.STATIC, dark).at(0, -0.5f, -5).rot(25, 0, 0).mat("horn").box(-1, -1, -2, 2, 2, 2).add();
        part(b, "jaw", "head", Role.JAW, belly).at(0, 1, -5).rot(10, 0, 0).mat("plates").box(-2, 0, -5, 4, 2, 6).add();
        part(b, "crest", "head", Role.STATIC, spine).at(0, -4, 0).rot(-40, 0, 0).mat("horn").box(-0.5f, -4, -1, 1, 4, 3).add();
        for (boolean left : new boolean[]{true, false}) {
            side(part(b, "horn", "head", Role.STATIC, spine).at(3, -4, -1).rot(-30, 0, -30).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1, 0.2f), left);
            side(part(b, "fin", "neck", Role.STATIC, dark).at(3, -1, 0).rot(0, -20, -30).mat("membrane").box(0, -2, -1.5f, 3, 4, 3), left);
            side(part(b, "arm", "torso", left ? Role.ARM_LEFT : Role.ARM_RIGHT, skin).at(5, -9, -1).rot(-40, 0, -15).mat("scales")
                    .box(-1.5f, -1, -1.5f, 3, 6, 3), left);
            side(part(b, "forearm", "arm*", Role.STATIC, dk(skin, 0.95f)).at(0, 5, 0).rot(-40, 0, 0).mat("scales").box(-1, 0, -1, 2, 5, 2), left);
            side(part(b, "claw", "forearm*", Role.STATIC, spine).at(0, 5, 0).rot(20, 0, 0).mat("horn")
                    .box(-1, 0, -1, 1, 2, 1).box(0.2f, 0, -1, 1, 2, 1).box(-0.4f, 0, 0.2f, 1, 2, 1), left);
            Role lr = left ? Role.LEG_FRONT_LEFT : Role.LEG_FRONT_RIGHT;
            side(part(b, "leg", "hips", lr, skin).at(3.5f, 1, 0).rot(-30, 0, -4).mat("scales").box(-2, -1, -2, 4, 7, 4), left);
            side(part(b, "shin", "leg*", Role.STATIC, dk(skin, 0.95f)).at(0, 6, 0).rot(45, 0, 0).mat("scales").box(-1.5f, 0, -1.5f, 3, 5, 3), left);
            side(part(b, "foot", "shin*", Role.STATIC, dark).at(0, 4.5f, 0).rot(-15, 0, 0).mat("scales").box(-2, 0, -4, 4, 2, 5), left);
            side(part(b, "toe", "foot*", Role.STATIC, spine).at(0, 1, -4).mat("horn")
                    .box(-2, -0.5f, -1.5f, 1, 1, 2).box(-0.5f, -0.5f, -1.5f, 1, 1, 2).box(1, -0.5f, -1.5f, 1, 1, 2), left);
        }
        part(b, "tail", "hips", Role.TAIL, skin).at(0, 0, 4).rot(30, 0, 0).mat("scales").box(-3, -2.5f, 0, 6, 5, 6).add();
        part(b, "tail_mid", "tail", Role.STATIC, dk(skin, 0.95f)).at(0, 0, 6).rot(15, 0, 0).mat("scales").box(-2, -2, 0, 4, 4, 5).add();
        part(b, "tail_tip", "tail_mid", Role.STATIC, dark).at(0, 0, 5).rot(10, 0, 0).mat("scales").box(-1, -1, 0, 2, 2, 4).add();
        part(b, "tail_spine", "tail", Role.STATIC, spine).at(0, -2.5f, 3).rot(-40, 0, 0).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 2).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // TzHaar
    // ------------------------------------------------------------------------------------------

    /**
     * TzHaar: hulking obsidian folk with lava veins, a broad fish-like head with a fin crest and glowing eyes,
     * massive arms and a stubby tail. Hur are citizens with tool belts, Ket wear obsidian armour and swing mauls,
     * Xil carry throwing spikes and a spiked collar, Mej are mages in robes with a lava staff.
     */
    static ModelSpec tzhaar(String id, String kind) {
        int rock = 0x5A2E26;
        int lava = 0xFF7A1A;
        BestiaryWldFig f = BestiaryWldFig.of(id).legs(11, 5).torso(10, 10, 14, 8, 12).arms(5, 6, 6, 6).head(9, 8, 8).foot(7)
                .hunch(10).stance(4).skin(rock, "rock", lava).skinGlow("cracks").face("eyes").glowEyes(0xFFB030).hair(0, "none")
                .nose("none").ears("none").jaw("heavy").brow().top(rock, "bare").boots(rock, "bare").belt(-1).pose(-6, 10, 22)
                .shadow(0.9f);
        switch (kind) {
            case "ket" -> f.bottoms(0x2A2226, "plate").top(0x2E2A2E, "plate").trim(0x8A3A20).boots(0x2A2226, "sabaton")
                    .weapon("hammer", 0x2A2428).shield("round", 0x2E2A2E, 0xC04A20).scale(1.0f);
            case "xil" -> f.bottoms(0x3A2A2A, "kilt").trim(0x8A3A20).pauldrons(0x2E2A2E).weapon("spear", 0x2A2428);
            case "mej" -> f.top(0x6A1E1A, "robe").sleeves("wide").trim(0xD08030).bottoms(0x5A1A16, "robe").belt(0x2A1A10)
                    .weapon("staff", 0x2A2428).weaponGlow(0xFF9030);
            default -> f.bottoms(0x4A3426, "loincloth").belt(0x3A2A1A).pouch();
        }
        ModelSpec.Builder b = f.builder();
        // fin crest down the head, cheek fins, a stubby tail
        part(b, "crest", "head", Role.STATIC, dk(rock, 1.15f)).accent(lava).at(0, -8, 0).rot(-20, 0, 0).mat("rock")
                .box(-1, -3, -3, 2, 3, 6).box(-0.5f, -5, -1, 1, 2, 4).add();
        part(b, "crest_back", "head", Role.STATIC, rock).accent(lava).at(0, -6, 4).rot(-60, 0, 0).mat("rock").box(-1, -4, -1, 2, 4, 3).add();
        sym(b, "cheek_fin", "head", false, dk(rock, 1.1f)).accent(lava).at(4.5f, -3, -1).rot(0, -25, -30).mat("rock").box(0, -2, -1.5f, 3, 3, 3).add();
        sym(b, "brow_horn", "head", false, 0x2A1A18).at(3, -7, -3).rot(-20, 0, -30).mat("rock").box(-0.5f, -3, -0.5f, 1, 3, 1, 0.2f).add();
        part(b, "tail", "hips", Role.TAIL, rock).accent(lava).at(0, -1, 2.5f).rot(40, 0, 0).mat("rock").glow("cracks").box(-2, -2, 0, 4, 4, 5).add();
        part(b, "tail_tip", "tail", Role.STATIC, dk(rock, 0.9f)).accent(lava).at(0, 0, 5).rot(15, 0, 0).mat("rock").box(-1, -1, 0, 2, 2, 4).add();
        sym(b, "shoulder_rock", "torso", false, dk(rock, 1.1f)).accent(lava).at(6, -12, 0).rot(0, 0, -25).mat("rock")
                .box(-2, -2, -3, 4, 3, 6).add();
        if (kind.equals("xil")) {
            for (int i = 0; i < 4; i++) {
                part(b, "collar_spike" + i, "torso", Role.STATIC, 0x1E1A1C).at(-4.5f + i * 3, -12, -3).rot(-30, 0, -30 + i * 20).mat("rock")
                        .box(-0.5f, -3, -0.5f, 1, 3, 1, 0.2f).add();
            }
            part(b, "spike_bundle", "torso", Role.STATIC, 0x1E1A1C).at(-4, -10, 4.5f).rot(0, 0, 30).mat("rock")
                    .box(-1, -4, -0.5f, 1, 8, 1).box(0.5f, -4, -0.5f, 1, 8, 1).box(-0.25f, -5, 0, 1, 9, 1).add();
        }
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Implings, crabs, cyclops
    // ------------------------------------------------------------------------------------------

    /** Impling: a tiny big-headed imp with long pointed ears, membrane wings, a curling tail and a sparkle of magic. */
    static ModelSpec impling(String id, int skin, int spark) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.25f);
        part(b, "body", null, Role.FLOAT, skin).at(0, 15, 0).rot(10, 0, 0).mat("skin").box(-2, -4, -1.5f, 4, 4, 3).add();
        part(b, "belly", "body", Role.STATIC, dk(skin, 1.15f)).at(0, -2, -1.5f).mat("skin").box(-1.5f, 0, -0.6f, 3, 2, 1).add();
        part(b, "head", "body", Role.HEAD, skin).accent(0xFFE040).at(0, -4, -0.5f).rot(-10, 0, 0).mat("skin").pattern("eyes").glow("eyes")
                .box(-3, -5, -3, 6, 5, 5).add();
        part(b, "nose", "head", Role.STATIC, dk(skin, 0.9f)).at(0, -2.5f, -3).rot(-20, 0, 0).mat("skin").box(-0.5f, 0, -1.5f, 1, 2, 2).add();
        part(b, "grin", "head", Role.JAW, dk(skin, 0.8f)).at(0, -0.5f, -2.5f).mat("skin").box(-2, 0, -0.6f, 4, 1, 1).add();
        part(b, "tuft", "head", Role.STATIC, dk(skin, 0.7f)).at(0, -5, 0).rot(-30, 0, 0).mat("hair").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        for (boolean left : new boolean[]{true, false}) {
            side(part(b, "ear", "head", Role.STATIC, skin).at(3, -3, 0).rot(0, -20, -25).mat("skin").box(0, -1, -0.5f, 3, 2, 1), left);
            side(part(b, "ear_tip", "ear*", Role.STATIC, skin).at(3, 0, 0).rot(0, 0, -25).mat("skin").box(0, -0.5f, -0.5f, 2, 1, 1), left);
            side(part(b, "wing", "body", left ? Role.WING_LEFT : Role.WING_RIGHT, dk(skin, 0.7f)).at(0.5f, -3.5f, 1.6f).rot(0, -35, -35)
                    .mat("membrane").box(0, -1, 0, 4, 3, 1), left);
            side(part(b, "wing_tip", "wing*", Role.STATIC, dk(skin, 0.6f)).at(4, -1, 0).rot(0, 0, -25).mat("membrane").box(0, 0, 0, 3, 2, 1), left);
            side(part(b, "arm", "body", left ? Role.ARM_LEFT : Role.ARM_RIGHT, skin).at(2.5f, -3.5f, 0).rot(-40, 0, -20).mat("skin")
                    .box(-0.5f, 0, -0.5f, 1, 4, 1), left);
            side(part(b, "hand", "arm*", Role.STATIC, dk(skin, 0.9f)).at(0, 4, 0).mat("skin").box(-1, 0, -1, 2, 1, 2), left);
            side(part(b, "leg", "body", left ? Role.LEG_FRONT_LEFT : Role.LEG_FRONT_RIGHT, skin).at(1, 0, 0).rot(-20, 0, -8).mat("skin")
                    .box(-0.5f, 0, -0.5f, 1, 4, 1), left);
            side(part(b, "foot", "leg*", Role.STATIC, dk(skin, 0.85f)).at(0, 4, 0).rot(20, 0, 0).mat("skin").box(-1, 0, -2, 2, 1, 3), left);
        }
        part(b, "tail", "body", Role.TAIL, skin).at(0, -0.5f, 1.5f).rot(30, 0, 0).mat("skin").box(-0.5f, -0.5f, 0, 1, 1, 4).add();
        part(b, "tail_tip", "tail", Role.STATIC, dk(skin, 0.8f)).at(0, 0, 4).rot(-60, 0, 0).mat("horn").box(-1, -1, 0, 2, 2, 1).add();
        part(b, "sparkle", "body", Role.SPIN, spark).at(0, -5, 0).mat("energy").glow("full")
                .box(-4, -3, 1, 1, 1, 1, -0.2f).box(3.5f, -2, -2, 1, 1, 1, -0.2f).box(-3.5f, 3, 2, 1, 1, 1, -0.2f).box(3, 4, 1.5f, 1, 1, 1, -0.2f).add();
        return b.build();
    }

    /**
     * Crabs: a broad knobbly carapace on eight jointed legs with two heavy pincers and eyes on stalks. The sand crab's
     * shell is a lumpy sandy mound; the ammonite crab carries a coiled fossil shell on its back.
     */
    static ModelSpec crab(String id, int shell, int light, boolean ammonite) {
        int dark = dk(shell, 0.7f);
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.0f);
        part(b, "body", null, Role.BODY, shell).at(0, 19, 0).mat("chitin").box(-6, -3, -5, 12, 4, 10).add();
        part(b, "carapace", "body", Role.STATIC, light).at(0, -3, 0).mat(ammonite ? "chitin" : "rock")
                .box(-5, -2, -4, 10, 2, 8).box(-3.5f, -3, -3, 7, 1, 6).add();
        part(b, "belly", "body", Role.STATIC, dk(light, 0.9f)).at(0, 1, 0).mat("plates").box(-5, -0.5f, -4, 10, 1, 8).add();
        part(b, "head", "body", Role.HEAD, shell).at(0, -1, -5).mat("chitin").box(-3, -1.5f, -1.5f, 6, 3, 2).add();
        part(b, "mouth", "head", Role.JAW, dark).at(0, 1, -1.5f).mat("chitin").box(-1.5f, 0, -0.6f, 3, 1, 1).add();
        for (boolean left : new boolean[]{true, false}) {
            side(part(b, "eyestalk", "head", Role.STATIC, shell).at(1.5f, -1.5f, -1).rot(-15, 0, -10).mat("chitin").box(-0.5f, -3, -0.5f, 1, 3, 1), left);
            side(part(b, "eye", "eyestalk*", Role.STATIC, 0x101010).at(0, -3, 0).mat("energy").box(-0.75f, -1.5f, -0.75f, 2, 2, 2, -0.25f), left);
            Role ar = left ? Role.ARM_LEFT : Role.ARM_RIGHT;
            side(part(b, "arm", "body", ar, shell).at(5, -0.5f, -4).rot(0, -35, -10).mat("chitin").box(-1, -1, -4, 2, 2, 4), left);
            side(part(b, "forearm", "arm*", Role.STATIC, shell).at(0, 0, -4).rot(0, 60, 0).mat("chitin").box(-1.5f, -1.5f, -3, 3, 3, 3), left);
            side(part(b, "claw", "forearm*", Role.STATIC, light).at(0, 0, -3).rot(0, -15, 0).mat("chitin").box(-2.5f, -2.5f, -3, 5, 4, 3), left);
            side(part(b, "claw_fixed", "claw*", Role.STATIC, dk(light, 0.95f)).at(-1.2f, 0, -3).rot(0, 10, 0).mat("chitin")
                    .box(-1, -1.5f, -4, 2, 3, 4), left);
            side(part(b, "claw_moving", "claw*", Role.JAW, dark).at(1.3f, -0.5f, -3).rot(0, -20, 0).mat("chitin").box(-0.75f, -1, -4, 2, 2, 4), left);
        }
        for (int i = 0; i < 4; i++) {
            for (boolean left : new boolean[]{true, false}) {
                arachLeg(b, "leg" + i, left, "body", 5.5f, 0, -2.5f + i * 2.2f, 20 - i * 18, 4, -35, 6, 100, shell, dark, "chitin");
            }
        }
        if (ammonite) {
            int fos = 0xE8D8B8;
            part(b, "shell", "body", Role.STATIC, fos).at(0, -4, 2).rot(-10, 0, 0).mat("bone").box(-3.5f, -9, -4.5f, 7, 9, 9).add();
            part(b, "shell_coil", "shell", Role.STATIC, dk(fos, 0.85f)).at(0, -4.5f, 0).mat("bone")
                    .box(-4, -2.5f, -2.5f, 8, 5, 5).box(-4.2f, -1, -1, 8, 2, 2, 0.2f).add();
            part(b, "shell_lip", "shell", Role.STATIC, dk(fos, 0.95f)).at(0, -1, -4.5f).mat("bone").box(-4, -3, -1, 8, 4, 2).add();
            for (int i = 0; i < 4; i++) {
                part(b, "rib" + i, "shell", Role.STATIC, dk(fos, 0.75f)).at(0, -9 + i * 2.5f, 4.5f).mat("bone").box(-3.5f, 0, -0.4f, 7, 1, 1).add();
            }
        } else {
            for (int i = 0; i < 5; i++) {
                part(b, "knob" + i, "carapace", Role.STATIC, i % 2 == 0 ? dk(light, 1.05f) : dk(light, 0.9f))
                        .at(-3.5f + (i % 3) * 3.5f, -3, -2.5f + (i / 3) * 4).rot(0, i * 25, 0).mat("rock").box(-1, -1.5f, -1, 2, 2, 2).add();
            }
            part(b, "barnacles", "carapace", Role.STATIC, 0xC8C0A8).at(4, -2, 2).mat("bone").box(-1, -1, -1, 2, 1, 2).box(-2, -1.5f, 1, 1, 1, 1).add();
        }
        return b.build();
    }

    /** Cyclops: a hulking one-eyed giant with a single great eye under a heavy brow, a single horn, loincloth and spiked club. */
    static ModelSpec cyclops(String id) {
        ModelSpec.Builder b = BestiaryWldFolk.giant(id, 0xB89070).face("none").hair(0x4A3020, "short").nose("none")
                .bottoms(0x5A3A22, "loincloth").weapon("greatclub", Humanoid.WOOD).belt(0x3A2414).scale(1.3f).builder();
        part(b, "eye_socket", "head", Role.STATIC, 0x5A3A2A).at(0, -5, -4.5f).mat("flesh").box(-3, -2, -0.4f, 6, 4, 1).add();
        part(b, "eye", "eye_socket", Role.STATIC, 0xF4F0E4).at(0, 0, -0.4f).mat("bone").box(-2.5f, -1.5f, -0.4f, 5, 3, 1).add();
        part(b, "pupil", "eye", Role.STATIC, 0x9A2A10).at(0, 0, -0.6f).mat("energy").glow("full").box(-1, -1, -0.2f, 2, 2, 1, -0.1f).add();
        part(b, "nose", "head", Role.STATIC, 0xA88060).at(0, -3, -4.5f).rot(-10, 0, 0).mat("skin").box(-1, 0, -1.5f, 2, 2, 2).add();
        part(b, "mouth", "head", Role.STATIC, 0x3A1A12).at(0, -1.5f, -4.6f).mat("flesh").box(-2.5f, 0, -0.4f, 5, 1, 1).add();
        part(b, "teeth", "mouth", Role.STATIC, 0xE8E0C8).at(0, 0, -0.5f).mat("bone").box(-2, 0, -0.1f, 1, 1, 1, -0.2f).box(1, 0, -0.1f, 1, 1, 1, -0.2f).add();
        part(b, "horn", "head", Role.STATIC, 0xE0D4B0).at(0, -9, -1).rot(-25, 0, 0).mat("horn").box(-1, -3, -1, 2, 3, 2).add();
        part(b, "horn_tip", "horn", Role.STATIC, 0xC8B890).at(0, -3, 0).rot(-20, 0, 0).mat("horn").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        return b.build();
    }
}
