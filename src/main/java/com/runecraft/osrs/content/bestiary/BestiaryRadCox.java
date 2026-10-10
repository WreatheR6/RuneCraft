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

/** Chambers of Xeric: the Great Olm and its claws, the room bosses and the raid's notable monsters. */
final class BestiaryRadCox {
    private BestiaryRadCox() {
    }

    static final int BONE = 0xE2DCC6;
    static final int TOOTH = 0xEDE4CC;
    static final int CRYSTAL = 0x7AE8E0;

    static List<ModelSpec> all() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(tekton());
        l.add(vasa());
        l.add(vasaCrystal());
        l.add(guardian());
        l.add(robedSkeleton("skeletal_mystic", 0x4A2A6A, 0xC8A040, "staff", 0xB060F0, 1.0f));
        l.add(robedSkeleton("deathly_mage", 0x22264A, 0x8A8AC8, "staff", 0x60A0FF, 1.0f));
        l.add(robedSkeleton("deathly_ranger", 0x2E3E24, 0x8A7A4A, "bow", 0x9AE060, 1.0f));
        l.add(shaman());
        l.add(spawn());
        l.add(muttadile("small_muttadile", 0x5A7A3A, 0x34502A, 0xC8C070, 0.8f));
        l.add(muttadile("muttadile", 0x4A6A3A, 0x2A4426, 0xD8C870, 1.0f));
        l.add(vanguard("vanguard_melee", 0xB0402C, "blade"));
        l.add(vanguard("vanguard_ranged", 0x3E9A3E, "launcher"));
        l.add(vanguard("vanguard_magic", 0x3E62C0, "staff"));
        l.add(wasp("vespula", 1.0f, true));
        l.add(wasp("vespine_soldier", 0.55f, false));
        l.add(portal());
        l.add(iceDemon());
        l.add(olmHead());
        l.add(olmClaw("olm_left_claw", true));
        l.add(olmClaw("olm_right_claw", false));
        return l;
    }

    // ------------------------------------------------------------------------------------------
    // Tekton
    // ------------------------------------------------------------------------------------------

    /** Tekton: a hulking, scarred brute in riveted bronze plate, dragging a forge hammer that glows white-hot. */
    static ModelSpec tekton() {
        int skin = 0xB08A60;
        int dark = 0x7A5A3A;
        int armour = 0x4A4A52;
        int brass = 0xB08A3A;
        BestiaryRad.Frame f = new BestiaryRad.Frame("tekton", 1.0f, 1.1f).colors(skin, dark, armour, 0xE06010)
                .legs(8, 7, 7, 5.5f).torso(20, 15, 12, 16).arms(9, 9, 7, 8).head(8, 7, 8).hips(16, 6, 11);
        f.mat = "skin";
        f.handW = 8;
        f.handH = 5;
        f.claws = 0;
        f.toes = 3;
        f.clawColor = 0x4A3A2A;
        f.clawMat = "metal";
        f.armSplay = 14;
        f.neckLen = 1;
        f.footLen = 9;
        ModelSpec.Builder b = f.build();
        // breastplate with rivets, layered pauldrons, belt, loin plates
        p(b, "breastplate", "torso", armour).at(0, -14, -6.2f).rot(-3, 0, 0).mat("metal").pattern("plates")
                .box(-8, 0, -1, 16, 8, 2).box(-6, 8, -0.8f, 12, 3, 2).add();
        p(b, "plate_ridge", "breastplate", brass).at(0, 0, -1.2f).mat("metal").box(-0.5f, 0, -0.5f, 1, 10, 1).add();
        s(b, "pauldron", "shoulder", true, armour).at(1.5f, -3.5f, 0).rot(0, 0, -18).mat("metal").pattern("plates")
                .box(-3, -1.5f, -6.5f, 10, 3, 13).add();
        s(b, "pauldron2", "pauldron", true, shade(armour, 0.85f)).at(2.5f, 1.5f, 0).rot(0, 0, 22).mat("metal")
                .box(-2, 0, -6, 7, 3, 12).add();
        s(b, "pauldron_spike", "pauldron", true, brass).at(2, -1.5f, 0).rot(0, 0, -20).mat("metal")
                .box(-1, -3, -1, 2, 3, 2).add();
        p(b, "belt", "hips", 0x3A2614).at(0, -4, 0).mat("leather").box(-8.5f, -1, -6, 17, 3, 12).add();
        p(b, "buckle", "belt", brass).at(0, 0, -6).mat("metal").box(-2, -1.5f, -0.8f, 4, 4, 1).add();
        p(b, "loin_f", "hips", 0x5A3A22).at(0, -1, -5.6f).rot(-6, 0, 0).mat("leather").box(-3.5f, 0, -0.5f, 7, 8, 1).add();
        p(b, "loin_b", "hips", 0x5A3A22).at(0, -1, 5.6f).rot(6, 0, 0).mat("leather").box(-4, 0, -0.5f, 8, 7, 1).add();
        s(b, "tasset", "hips", false, armour).at(6.5f, -2, -1).rot(0, 0, -12).mat("metal").box(-2, 0, -4, 4, 6, 8).add();
        s(b, "greave", "shin", true, armour).at(0, 1, -3.5f).mat("metal").box(-3, 0, -0.8f, 6, 5, 1).add();
        s(b, "bracer", "forearm", true, armour).at(0, 2, 0).mat("metal").box(-4.5f, 0, -4.5f, 9, 4, 9).add();
        s(b, "bracer_rim", "bracer", true, brass).at(0, 4, 0).mat("metal").box(-4.5f, 0, -4.5f, 9, 1, 9).add();
        s(b, "knuckles", "hand", true, shade(skin, 0.8f)).at(0, 5, -3.5f).mat("skin")
                .box(-3.5f, -1.5f, -1, 2, 2, 2).box(-1.5f, -1.5f, -1, 2, 2, 2).box(0.5f, -1.5f, -1, 2, 2, 2).box(2.5f, -1.5f, -0.5f, 1, 2, 2).add();
        // head: bald, scarred, with tusks and a heavy brow
        s(b, "tusk", "jaw", false, TOOTH).at(2.5f, 0, -4).rot(-15, 0, 10).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        s(b, "ear", "head", false, skin).at(4, -2, -2).rot(0, -20, -10).mat("skin").box(0, -1.5f, -1, 1, 3, 2).add();
        p(b, "scar", "head", 0x8A4A3A).at(1, -4, -6.1f).mat("flesh").box(-0.5f, 0, -0.5f, 1, 4, 1).add();
        p(b, "topknot", "head", 0x2A1A10).at(0, -5, 0).mat("hair").box(-1.5f, -3, -1.5f, 3, 3, 3).box(-1, -2, 1.5f, 2, 2, 3).add();
        // the hammer: long haft, banded grip and a huge head with a glowing striking face
        p(b, "haft", "hand_r", 0x5A3A1E).at(0, 3, -1).rot(-72, 0, 0).mat("wood").box(-1.5f, -4, -1.5f, 3, 21, 3).add();
        p(b, "haft_band", "haft", 0x3A3A3A).at(0, 0, 0).mat("metal").box(-2, 2, -2, 4, 2, 4).box(-2, 10, -2, 4, 2, 4).add();
        p(b, "hammer_head", "haft", 0x4A4A50).accent(0xFF7A20).at(0, 16, 0).mat("metal").glow("cracks")
                .box(-6, 0, -5, 12, 7, 10).add();
        s(b, "hammer_face", "hammer_head", false, 0xFFA040).at(6, 3.5f, 0).mat("lava").glow("full")
                .box(0, -3, -4, 1, 6, 8).add();
        p(b, "hammer_cap", "hammer_head", 0x6A5034).at(0, 0, 0).mat("metal").box(-4, -1, -4, 8, 1, 8).box(-3, 7, -3, 6, 1, 6).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Vasa Nistirio and its crystals
    // ------------------------------------------------------------------------------------------

    /** Vasa Nistirio: a lumbering stone golem studded with purple crystal, its chest cracked open on a glowing core. */
    static ModelSpec vasa() {
        int stone = 0xA49CAE;
        int dark = 0x6E6678;
        int deep = 0x847C90;
        int cry = 0xB46CF0;
        BestiaryRad.Frame f = new BestiaryRad.Frame("vasa_nistirio", 1.0f, 1.1f).colors(stone, dark, deep, cry)
                .legs(7, 6, 8, 6).torso(22, 16, 14, 14).arms(10, 10, 7, 8).head(9, 7, 8).hips(16, 6, 12);
        f.mat = "hide";
        f.handW = 9;
        f.handH = 5;
        f.claws = 3;
        f.clawLen = 2;
        f.clawColor = cry;
        f.clawMat = "ice";
        f.armSplay = 12;
        f.neckLen = 1;
        f.footLen = 9;
        f.faceEyes = true;
        f.headGlow = "eyes";
        ModelSpec.Builder b = f.build();
        p(b, "core", "torso", Role.SPIN, 0xF0B0FF).at(0, -9, -7.5f).mat("energy").glow("full").box(-3, -3, -1.5f, 6, 6, 3).add();
        p(b, "core_rim", "torso", dark).at(0, -9, -7.2f).mat("rock")
                .box(-5, -5, -1, 10, 2, 2).box(-5, 3, -1, 10, 2, 2).box(-5, -3, -1, 2, 6, 2).box(3, -3, -1, 2, 6, 2).add();
        // crystal growths: clusters on the back, shoulders and forearms
        float[][] back = {{-6, -15, 3, 8, -20, 15}, {-1, -16, 5, 11, -10, -5}, {5, -15, 3, 7, -15, -20}, {-8, -9, 6, 6, 30, 30},
            {7, -10, 6, 7, 35, -25}, {1, -8, 7, 5, 40, 0}};
        for (int i = 0; i < back.length; i++) {
            float[] q = back[i];
            int h = (int) q[3];
            p(b, "crystal" + i, "torso", i % 2 == 0 ? cry : 0xC890F8).at(q[0], q[1], q[2]).rot(q[4], 0, q[5]).mat("ice")
                    .glow(i % 3 == 0 ? "full" : null).box(-1.5f, -h, -1.5f, 3, h, 3).box(-0.5f, -h - 2, -0.5f, 1, 2, 1).add();
        }
        s(b, "shoulder_crystal", "shoulder", true, cry).at(2, -3, 1).rot(-10, 0, -25).mat("ice").glow("full")
                .box(-1.5f, -6, -1.5f, 3, 6, 3).box(1, -4, 1, 2, 4, 2).add();
        s(b, "fore_crystal", "forearm", true, 0xC890F8).at(2.5f, 3, 1).rot(20, 0, -40).mat("ice")
                .box(-1, -5, -1, 2, 5, 2).add();
        s(b, "fist_rock", "hand", true, deep).at(0, 0, 0).mat("rock").box(-5, -1, -5, 10, 2, 10).add();
        p(b, "head_crystal", "head", cry).at(0, -5, -1).rot(-15, 0, 0).mat("ice").glow("full")
                .box(-1.5f, -5, -1.5f, 3, 5, 3).box(2, -3, 0, 2, 3, 2).box(-4, -3, 0, 2, 3, 2).add();
        p(b, "rubble", "hips", dark).at(0, 0, 0).mat("rock").box(-7, -1, -6.5f, 5, 3, 3).box(3, -2, 5, 4, 3, 3).add();
        return b.build();
    }

    /** One of Vasa's energy crystals: a cluster of glowing shards on a cracked stone base. */
    static ModelSpec vasaCrystal() {
        int cry = 0xB866F0;
        ModelSpec.Builder b = ModelSpec.builder("vasa_crystal").shadow(0.6f);
        p(b, "base", null, 0x4A4452).at(0, 24, 0).mat("rock").box(-6, -3, -6, 12, 3, 12).box(-5, -4, -5, 10, 1, 10).add();
        p(b, "rubble", "base", 0x5A5262).at(0, 0, 0).mat("rock").box(-8, -2, -3, 2, 2, 3).box(6, -2, 1, 3, 2, 2)
                .box(-2, -2, 6, 3, 2, 2).box(1, -2, -8, 2, 2, 2).add();
        p(b, "crystal", null, Role.SPIN, cry).at(0, 20, 0).mat("ice").glow("full").box(-2.5f, -16, -2.5f, 5, 16, 5)
                .box(-1.5f, -19, -1.5f, 3, 3, 3).box(-0.5f, -21, -0.5f, 1, 2, 1).add();
        float[][] q = {{4, 0, 0, 10, 0, -28}, {-4, 0, 1, 9, 10, 30}, {0, 0, 4, 8, 30, 0}, {1, 0, -4, 7, -30, -10}, {-3, 0, -3, 6, -25, 25}};
        for (int i = 0; i < q.length; i++) {
            int h = (int) q[i][3];
            p(b, "shard" + i, "crystal", i % 2 == 0 ? 0xD8A0FF : cry).at(q[i][0], q[i][1], q[i][2]).rot(q[i][4], 0, q[i][5])
                    .mat("ice").glow(i % 2 == 0 ? "full" : null).box(-1.5f, -h, -1.5f, 3, h, 3).box(-0.5f, -h - 2, -0.5f, 1, 2, 1).add();
        }
        p(b, "glow_ring", "base", Role.SPIN, 0xC080FF).at(0, -4.2f, 0).mat("energy").glow("full")
                .box(-4, -0.5f, -4, 8, 1, 1).box(-4, -0.5f, 3, 8, 1, 1).box(-4, -0.5f, -3, 1, 1, 6).box(3, -0.5f, -3, 1, 1, 6).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Guardians
    // ------------------------------------------------------------------------------------------

    /** A CoX Guardian: a squat, angular stone statue on its plinth, carved with glowing Xerician runes. */
    static ModelSpec guardian() {
        int stone = 0x8A8A7E;
        int dark = 0x5E5E56;
        int deep = 0x6E6E64;
        int rune = 0x60D0C0;
        BestiaryRad.Frame f = new BestiaryRad.Frame("cox_guardian", 1.0f, 1.0f).colors(stone, dark, deep, rune)
                .legs(5, 5, 7, 5).torso(20, 14, 12, 8).arms(8, 8, 7, 8).head(10, 8, 9).hips(14, 5, 10);
        f.mat = "hide";
        f.bodyGlow = "runes";
        f.handW = 9;
        f.handH = 6;
        f.claws = 0;
        f.toes = 0;
        f.footH = 6;
        f.footLen = 9;
        f.armSplay = 12;
        f.neckLen = 1;
        f.jaw = false;
        ModelSpec.Builder b = f.build();
        p(b, "plinth", null, 0x5A5A54).at(0, 24, 0).mat("rock").box(-11, -2, -9, 22, 2, 18).box(-10, -3, -8, 20, 1, 16).add();
        p(b, "head_crest", "head", deep).at(0, -6, -2).mat("rock").box(-1.5f, -4, -6, 3, 4, 10).add();
        s(b, "head_horn", "head", false, dark).at(5, -4, -2).rot(-20, 0, -30).mat("rock").box(-1, -4, -1, 2, 4, 3).add();
        p(b, "face_plate", "head", dark).at(0, -1, -7.2f).mat("rock").box(-4, 0, -0.5f, 8, 3, 1).add();
        p(b, "chest_rune", "torso", rune).at(0, -10, -6.2f).mat("energy").glow("full")
                .box(-3, 0, -0.5f, 6, 1, 1).box(-0.5f, 0, -0.5f, 1, 5, 1).box(-2, 4, -0.5f, 4, 1, 1).add();
        s(b, "pauldron", "shoulder", true, deep).at(2, -3.5f, 0).rot(0, 0, -15).mat("rock").box(-3, -2, -6.5f, 9, 3, 13).add();
        s(b, "fist_block", "hand", true, dark).at(0, 1, 0).mat("rock").box(-5, 0, -5, 10, 4, 10).add();
        s(b, "fist_rune", "fist_block", true, rune).at(0, 2, -5.2f).mat("energy").glow("full").box(-2, -0.5f, -0.5f, 4, 1, 1).add();
        p(b, "cracks", "torso", dark).at(0, -4, 0).mat("rock").box(-10.5f, -3, -3, 1, 6, 6).box(9.5f, -6, -2, 1, 5, 5).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Robed skeletons: Skeletal Mystic, Deathly mage and Deathly ranger
    // ------------------------------------------------------------------------------------------

    /**
     * A gaunt skeleton in a tattered hooded robe: skull with sunken glowing eyes and a hinged jaw, open-fronted
     * robe over the ribcage, sleeves, hanging skirt panels, bony hands and feet, and a staff or bow.
     */
    static ModelSpec robedSkeleton(String id, int robe, int trim, String weapon, int magic, float scale) {
        int bone = BONE;
        int boneDark = 0xB8B098;
        int robeDark = shade(robe, 0.7f);
        ModelSpec.Builder b = ModelSpec.builder(id).scale(scale).shadow(0.5f);
        // pelvis and spine
        p(b, "pelvis", null, Role.BODY, bone).at(0, 12, 0).mat("bone").box(-3, -1, -2, 6, 3, 4).add();
        p(b, "spine", "pelvis", boneDark).at(0, -1, 1).rot(6, 0, 0).mat("bone")
                .box(-1, -4, -1, 2, 4, 2).box(-1, -8, -1, 2, 3, 2).add();
        p(b, "ribs", "spine", bone).at(0, -8, -1).mat("bone")
                .box(-3.5f, -5, -2, 7, 1, 4).box(-3.5f, -3, -2.2f, 7, 1, 4).box(-3, -1, -2, 6, 1, 4).box(-0.5f, -5, -2.4f, 1, 5, 1).add();
        // robe: open-fronted coat, mantle and hood
        p(b, "robe", "spine", robe).accent(trim).at(0, -8, 0).mat("cloth")
                .box(-4.5f, -6, -1, 9, 7, 4).box(-4.5f, -6, -2.6f, 2, 7, 2).box(2.5f, -6, -2.6f, 2, 7, 2).add();
        p(b, "trim", "robe", trim).at(0, 0, -2.7f).mat("cloth").box(-2.6f, -6, -0.2f, 1, 7, 1).box(1.6f, -6, -0.2f, 1, 7, 1).add();
        p(b, "mantle", "robe", robeDark).at(0, -6, 0.5f).mat("cloth").box(-5.5f, -1, -3, 11, 2, 5).box(-4, -1.5f, 2, 8, 3, 1).add();
        p(b, "sash", "pelvis", trim).at(0, -1, 0).mat("cloth").box(-3.5f, -1, -2.5f, 7, 2, 5).box(1, 1, -2.7f, 2, 4, 1).add();
        // skirt: hanging panels, slightly flared
        float[][] panels = {{-3, -2.4f, -10}, {0, -2.6f, 0}, {3, -2.4f, 10}, {-3.6f, 0.5f, -14}, {3.6f, 0.5f, 14}, {-2, 2.4f, -6}, {2, 2.4f, 6}};
        for (int i = 0; i < panels.length; i++) {
            float[] q = panels[i];
            boolean side = Math.abs(q[0]) > 3.3f;
            p(b, "skirt" + i, "pelvis", i % 2 == 0 ? robe : robeDark).at(q[0], 0.5f, q[1])
                    .rot(q[1] < 0 ? -8 : q[1] > 1 ? 8 : 0, 0, q[2]).mat("cloth")
                    .box(side ? -0.5f : -1.5f, 0, side ? -1.5f : -0.5f, side ? 1 : 3, 9 - (i % 3), side ? 3 : 1).add();
        }
        // skull: painted skull face with glowing pinpoints, cheekbones, hinged jaw, hood around it
        p(b, "neck", "spine", boneDark).at(0, -8, -0.5f).mat("bone").box(-1, -2, -1, 2, 2, 2).add();
        p(b, "head", "neck", Role.HEAD, bone).accent(magic).at(0, -2, 0).mat("bone").pattern("skull").glow("eyes")
                .box(-3.5f, -7, -4, 7, 7, 7).add();
        p(b, "cheek", "head", boneDark).at(0, -2, -4).mat("bone").box(-3.5f, -1, -0.5f, 2, 1, 1).box(1.5f, -1, -0.5f, 2, 1, 1).add();
        p(b, "jaw", "head", Role.JAW, boneDark).at(0, 0, -2).rot(8, 0, 0).mat("bone").box(-2.5f, 0, -2.2f, 5, 2, 4).add();
        p(b, "hood", "head", robeDark).at(0, -7, 0).mat("cloth")
                .box(-4.5f, -1, -4.5f, 9, 1, 8).box(-4.5f, 0, -4.5f, 1, 7, 8).box(3.5f, 0, -4.5f, 1, 7, 8).box(-4, 0, 3, 8, 8, 1).add();
        p(b, "hood_peak", "hood", robe).at(0, -1, 2).rot(-25, 0, 0).mat("cloth").box(-2, -2, -1, 4, 2, 3).add();
        // arms: sleeved upper arm, bony forearm, hand with finger bones
        s(b, "arm", "robe", false, robe).roles(Role.ARM_LEFT, Role.ARM_RIGHT).at(5, -5, 0.5f).rot(-8, 0, -8).mat("cloth")
                .box(-1.5f, -1, -1.5f, 3, 5, 3).add();
        s(b, "cuff", "arm", true, trim).at(0, 4, 0).mat("cloth").box(-2, 0, -2, 4, 2, 4).add();
        s(b, "forearm", "arm", true, bone).at(0, 5, 0).rot(-30, 0, 0).mat("bone").box(-0.5f, 0, -0.5f, 1, 5, 1).box(0.5f, 0.5f, -0.5f, 1, 4, 1).add();
        s(b, "hand", "forearm", true, bone).at(0, 5, 0).mat("bone").box(-1, 0, -1, 2, 1, 2)
                .box(-1, 1, -1.2f, 1, 2, 1).box(0.2f, 1, -1.2f, 1, 2, 1).box(-1.4f, 0.5f, 0, 1, 2, 1).add();
        // legs
        s(b, "leg", "pelvis", false, bone).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).at(1.8f, 1, 0).rot(-4, 0, 0)
                .mat("bone").box(-0.5f, 0, -0.5f, 1, 5, 1).add();
        s(b, "knee", "leg", true, boneDark).at(0, 5, 0).mat("bone").box(-1, -0.5f, -1, 2, 1, 2).add();
        s(b, "shin", "leg", true, bone).at(0, 5.5f, 0).rot(6, 0, 0).mat("bone").box(-0.5f, 0, -0.5f, 1, 5, 1).box(0.4f, 0.5f, 0, 1, 4, 1).add();
        s(b, "foot", "shin", true, boneDark).at(0, 5, 0).rot(-2, 0, 0).mat("bone").box(-1, 0, -2.5f, 2, 1, 3)
                .box(-1, 0.2f, -3.5f, 1, 1, 1).box(0.2f, 0.2f, -3.5f, 1, 1, 1).add();
        if (weapon.equals("staff")) {
            p(b, "staff", "hand_r", 0x4A3220).at(0, 1, -0.5f).rot(20, 0, 0).mat("wood").box(-0.5f, -16, -0.5f, 1, 24, 1).add();
            p(b, "staff_head", "staff", boneDark).at(0, -16, 0).mat("bone").box(-1.5f, -2, -1, 3, 2, 2).box(-2, -5, -0.5f, 1, 4, 1)
                    .box(1, -5, -0.5f, 1, 4, 1).add();
            p(b, "staff_orb", "staff", Role.SPIN, magic).at(0, -19, 0).mat("energy").glow("full").box(-1.5f, -1.5f, -1.5f, 3, 3, 3).add();
            p(b, "spell", "hand_l", magic).at(0, 3, -0.5f).mat("energy").glow("full").box(-1, -1, -1, 2, 2, 2).add();
        } else {
            p(b, "bow", "hand_l", 0x5A4024).at(0, 1.5f, -0.5f).rot(0, 90, 0).mat("wood")
                    .box(-0.5f, -2, -0.5f, 1, 4, 1).box(-0.5f, -6, 0.5f, 1, 4, 1).box(-0.5f, 2, 0.5f, 1, 4, 1)
                    .box(-0.5f, -8, 1.5f, 1, 2, 1).box(-0.5f, 6, 1.5f, 1, 2, 1).add();
            p(b, "bowstring", "bow", 0xD8D0B8).at(0, 0, 2.5f).mat("cloth").box(-0.3f, -8, -0.2f, 1, 16, 1).add();
            p(b, "arrow", "hand_r", magic).at(0, 1, -1).rot(-80, 0, 0).mat("energy").glow("full").box(-0.5f, -1, -0.5f, 1, 7, 1).add();
            p(b, "quiver", "robe", 0x4A3018).at(2, -5, 2.5f).rot(0, 0, 25).mat("leather").box(-1.5f, -1, -1, 3, 8, 2).add();
            p(b, "quiver_arrows", "quiver", magic).at(0, -1, 0).mat("energy").glow("full").box(-1, -2, -0.5f, 2, 2, 1).add();
        }
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Lizardmen
    // ------------------------------------------------------------------------------------------

    /** Lizardman shaman: a hunched green reptile with a bone-and-feather headdress, frill, fetishes and a long tail. */
    static ModelSpec shaman() {
        int scale = 0x4E8A3A;
        int dark = 0x2E5A26;
        int belly = 0xB8C080;
        BestiaryRad.Frame f = new BestiaryRad.Frame("cox_lizardman_shaman", 1.0f, 0.7f).colors(scale, dark, shade(scale, 0.8f), belly)
                .legs(6, 6, 4, 3).torso(11, 11, 7, 22).arms(6, 7, 3, 3).head(6, 5, 7).hips(9, 4, 7);
        f.mat = "scales";
        f.claws = 3;
        f.clawLen = 2;
        f.clawColor = 0xD8CCA8;
        f.toes = 3;
        f.neckLen = 3;
        f.jaw = false;
        f.faceEyes = false;
        ModelSpec.Builder b = f.build();
        p(b, "belly_scales", "torso", belly).at(0, -10, -3.7f).mat("plates").box(-3, 0, -0.5f, 6, 9, 1).add();
        // head: long snout, jaw with teeth, eyes, frill, headdress
        p(b, "snout", "head", scale).at(0, -1, -5).mat("scales").box(-2, -1.5f, -5, 4, 3, 5).add();
        p(b, "lizard_jaw", "head", Role.JAW, belly).at(0, 1.5f, -4).rot(10, 0, 0).mat("scales").box(-1.8f, 0, -5.5f, 4, 1, 6).add();
        teeth(b, "teeth", "snout", TOOTH, -1.5f, 1.5f, 1.5f, -4.5f, 4, 1);
        eyes(b, "eye", "head", 0xF0D040, 2.4f, -2, -3.5f, 1, 1);
        s(b, "frill", "head", false, 0xC0482E).at(3, -1, -1).rot(0, -35, -25).mat("membrane").box(0, -3, -0.5f, 4, 5, 1).add();
        p(b, "crest", "head", 0xC0482E).at(0, -3, -1).rot(-20, 0, 0).mat("membrane").box(-0.5f, -3, -2, 1, 3, 6).add();
        p(b, "headdress", "head", 0xE0D8C0).at(0, -3.5f, 0).mat("bone").box(-3.5f, -1, -3.5f, 7, 1, 5).add();
        s(b, "feather", "headdress", false, 0x3AA0B0).at(2.5f, -1, 0).rot(-15, 0, -20).mat("feathers").box(-0.5f, -6, -0.5f, 1, 6, 2).add();
        p(b, "feather_c", "headdress", 0xD04A2A).at(0, -1, 1).rot(-25, 0, 0).mat("feathers").box(-0.5f, -7, -0.5f, 1, 7, 2).add();
        // tail
        p(b, "tail", "hips", Role.TAIL, scale).at(0, -1, 3).rot(20, 0, 0).mat("scales").box(-2, -2, 0, 4, 4, 6).add();
        p(b, "tail2", "tail", Role.TAIL, dark).at(0, 0, 6).rot(10, 0, 0).mat("scales").box(-1.5f, -1.5f, 0, 3, 3, 6).add();
        p(b, "tail3", "tail2", Role.TAIL, scale).at(0, 0, 6).rot(5, 0, 0).mat("scales").box(-1, -1, 0, 2, 2, 5).add();
        // regalia: bone necklace, loincloth, fetish satchel
        p(b, "necklace", "torso", 0xE8E0C8).at(0, -10, -3.5f).mat("bone")
                .box(-4, -0.5f, -0.5f, 8, 1, 1).box(-1, 0.5f, -0.8f, 2, 2, 1).box(-3, 0.5f, -0.6f, 1, 2, 1).box(2, 0.5f, -0.6f, 1, 2, 1).add();
        p(b, "loin", "hips", 0x6A3A22).at(0, 0, -3.6f).rot(-6, 0, 0).mat("leather").box(-2.5f, 0, -0.5f, 5, 6, 1).add();
        p(b, "loin_b", "hips", 0x6A3A22).at(0, 0, 3.6f).rot(6, 0, 0).mat("leather").box(-3, 0, -0.5f, 6, 5, 1).add();
        p(b, "satchel", "hips", 0x8A5A30).at(-4.5f, 0, 0).rot(0, 0, 8).mat("leather").box(-1, 0, -1.5f, 2, 3, 3).add();
        s(b, "spine_scale", "torso", false, dark).at(1.5f, -10, 3.4f).rot(-30, 0, 0).mat("scales").box(-0.5f, -2, -1, 1, 2, 2).add();
        p(b, "spit", "hand_r", 0x9AE040).at(0, 3, -1).mat("slime").glow("full").box(-1, -1, -1, 2, 2, 2).add();
        return b.build();
    }

    /** A lizardman shaman's purple spawn: a squat, bulging little reptile about to burst. */
    static ModelSpec spawn() {
        int purple = 0x9A3AC0;
        int dark = 0x5A1E7A;
        ModelSpec.Builder b = ModelSpec.builder("cox_lizardman_spawn").shadow(0.3f);
        p(b, "body", null, Role.BODY, purple).accent(0xE070FF).at(0, 20, 0).mat("slime").pattern("spots")
                .box(-3.5f, -4, -4, 7, 5, 8).add();
        p(b, "belly", "body", 0xC080E0).at(0, 1, 0).mat("slime").box(-3, -0.5f, -3.5f, 6, 1, 7).add();
        p(b, "bulge", "body", 0xC060F0).at(0, -4, 0).mat("slime").glow("full").box(-2.5f, -2, -3, 5, 2, 6).add();
        for (int i = 0; i < 3; i++) {
            p(b, "pustule" + i, "body", 0xF0A0FF).at(-2 + i * 2, -5, -1 + i).mat("energy").glow("full").box(-0.5f, -1, -0.5f, 1, 1, 1).add();
        }
        p(b, "head", "body", Role.HEAD, purple).at(0, -2, -4).mat("slime").box(-2.5f, -2, -3, 5, 3, 3).add();
        p(b, "snout", "head", dark).at(0, 0, -3).mat("slime").box(-1.5f, -1, -2, 3, 2, 2).add();
        p(b, "jaw", "head", Role.JAW, dark).at(0, 1, -2).rot(15, 0, 0).mat("slime").box(-1.5f, 0, -2, 3, 1, 2).add();
        eyes(b, "eye", "head", 0xFFE060, 1.4f, -1.5f, -3, 1, 1);
        s(b, "leg_f", null, false, dark).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).at(3, 20, -2.5f).rot(0, 0, -25)
                .mat("slime").box(-1, 0, -1, 2, 4, 2).add();
        s(b, "leg_b", null, false, dark).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).at(3, 20, 2.5f).rot(0, 0, -25)
                .mat("slime").box(-1, 0, -1, 2, 4, 2).add();
        s(b, "toes_f", "leg_f", true, 0xE0C0F0).at(0, 4, -1).mat("horn").box(-1, -0.5f, -1, 1, 1, 1).box(0.2f, -0.5f, -1, 1, 1, 1).add();
        s(b, "toes_b", "leg_b", true, 0xE0C0F0).at(0, 4, -1).mat("horn").box(-1, -0.5f, -1, 1, 1, 1).box(0.2f, -0.5f, -1, 1, 1, 1).add();
        p(b, "tail", "body", Role.TAIL, purple).at(0, -2, 4).rot(10, 0, 0).mat("slime").box(-1, -1, 0, 2, 2, 4).add();
        p(b, "tail2", "tail", Role.TAIL, dark).at(0, 0, 4).mat("slime").box(-0.5f, -0.5f, 0, 1, 1, 3).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Muttadiles
    // ------------------------------------------------------------------------------------------

    /** Muttadile: a heavy, armour-backed crocodile with rows of osteoderms, a long toothy snout and a ridged tail. */
    static ModelSpec muttadile(String id, int back, int dark, int belly, float scale) {
        ModelSpec.Builder b = ModelSpec.builder(id).scale(scale).shadow(1.1f);
        p(b, "body", null, Role.BODY, back).accent(belly).at(0, 15, 0).mat("scales").box(-7, -5, -11, 14, 9, 22).add();
        p(b, "flank", "body", shade(back, 0.9f)).accent(belly).at(0, 0, 0).mat("scales").box(-8, -3, -8, 16, 6, 16).add();
        p(b, "belly", "body", belly).at(0, 4, 0).mat("plates").box(-6, -0.5f, -10, 12, 2, 20).add();
        // osteoderm rows along the back
        for (int r = 0; r < 3; r++) {
            float x = (r - 1) * 3.5f;
            Kit.P row = p(b, "scutes" + r, "body", r == 1 ? dark : shade(dark, 1.2f)).at(x, -5, 0).mat("plates");
            for (int i = 0; i < 6; i++) {
                row.box(-1, -1.5f + (r == 1 ? -0.5f : 0), -10 + i * 3.6f, 2, 2 + (r == 1 ? 1 : 0), 2);
            }
            row.add();
        }
        // head: wide skull, long snout, interlocking teeth, eyes on top
        p(b, "neck", "body", back).accent(belly).at(0, -1, -11).mat("scales").box(-5.5f, -4, -4, 11, 8, 5).add();
        p(b, "head", "neck", Role.HEAD, back).accent(belly).at(0, -1, -4).mat("scales").box(-5, -3, -7, 10, 5, 7).add();
        s(b, "eye_bump", "head", false, dark).at(3, -3, -4).mat("scales").box(-1.5f, -1.5f, -1.5f, 3, 2, 3).add();
        s(b, "eye", "eye_bump", true, 0xF0C030).at(0, -0.5f, -1.5f).mat("energy").glow("full").box(-1, -0.5f, -0.6f, 2, 1, 1).add();
        p(b, "snout", "head", back).accent(belly).at(0, -1, -7).mat("scales").box(-3.5f, -1.5f, -11, 7, 3, 11).add();
        p(b, "snout_tip", "snout", dark).at(0, -1.5f, -11).mat("scales").box(-2.5f, 0, -1.5f, 5, 2, 2).box(-1.5f, -0.6f, -1, 1, 1, 1)
                .box(0.5f, -0.6f, -1, 1, 1, 1).add();
        teeth(b, "teeth_l", "snout", TOOTH, -10, -1, 1.5f, -3.4f, 5, 1);
        teeth(b, "teeth_side", "snout", TOOTH, -10.5f, -1.5f, 1.5f, 3.4f, 5, 1);
        p(b, "jaw", "head", Role.JAW, belly).at(0, 2, -5).rot(12, 0, 0).mat("scales").box(-3.5f, 0, -13, 7, 2, 13).add();
        teeth(b, "teeth_low", "jaw", TOOTH, -3, 3, 0, -12, 5, -1);
        // splayed legs with clawed toes
        float[][] legs = {{7, -1, -7}, {7, -1, 7}};
        String[] names = {"leg_f", "leg_b"};
        Role[][] roles = {{Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT}, {Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT}};
        for (int i = 0; i < 2; i++) {
            String n = names[i];
            s(b, n, "body", false, back).roles(roles[i][0], roles[i][1]).accent(belly).at(legs[i][0], legs[i][1], legs[i][2])
                    .rot(0, 0, 35).mat("scales").box(-2, -1.5f, -2.5f, 6, 4, 5).add();
            s(b, n + "_shin", n, true, dark).at(4, 0.5f, 0).rot(0, 0, -40).mat("scales").box(-1.5f, 0, -2, 3, 6, 4).add();
            s(b, n + "_foot", n + "_shin", true, dark).at(0, 6, 0).rot(0, 0, 5).mat("scales").box(-2, 0, -4, 4, 1, 5).add();
            s(b, n + "_claws", n + "_foot", true, TOOTH).at(0, 0.5f, -4).mat("horn").box(-2, -0.5f, -1.5f, 1, 1, 2)
                    .box(-0.5f, -0.5f, -2, 1, 1, 2).box(1, -0.5f, -1.5f, 1, 1, 2).add();
        }
        // tail in three tapering ridged segments
        p(b, "tail", "body", Role.TAIL, back).accent(belly).at(0, -1, 11).mat("scales").box(-5, -3.5f, 0, 10, 7, 9)
                .add();
        p(b, "tail_ridge", "tail", dark).at(0, -3.5f, 0).mat("plates").box(-3, -2, 1, 2, 2, 2).box(1, -2, 1, 2, 2, 2)
                .box(-3, -2, 5, 2, 2, 2).box(1, -2, 5, 2, 2, 2).add();
        p(b, "tail2", "tail", Role.TAIL, back).accent(belly).at(0, 0.5f, 9).mat("scales").box(-3.5f, -2.5f, 0, 7, 5, 9).add();
        p(b, "tail2_ridge", "tail2", dark).at(0, -2.5f, 0).mat("plates").box(-0.5f, -2, 1, 2, 2, 2).box(-1.5f, -2, 5, 2, 2, 2).add();
        p(b, "tail3", "tail2", Role.TAIL, dark).accent(belly).at(0, 0.5f, 9).mat("scales").box(-2, -1.5f, 0, 4, 3, 8)
                .box(-0.5f, -3, 1, 1, 2, 6).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Vanguards
    // ------------------------------------------------------------------------------------------

    /**
     * A Vanguard: a stooped, crab-armoured warrior hiding under a great domed shell; its visor, shell trim and
     * weapon arm carry its combat style's colour (blade, rock-launcher or staff).
     */
    static ModelSpec vanguard(String id, int style, String weapon) {
        int shell = 0x6E6A5E;
        int dark = 0x46443C;
        int deep = 0x585448;
        BestiaryRad.Frame f = new BestiaryRad.Frame(id, 1.0f, 0.9f).colors(shell, dark, deep, style)
                .legs(6, 6, 5, 4).torso(15, 12, 10, 30).arms(7, 7, 4, 5).head(8, 6, 7).hips(12, 5, 9);
        f.mat = "chitin";
        f.claws = 3;
        f.clawColor = 0xD0C8A8;
        f.toes = 2;
        f.neckLen = 1;
        f.jaw = false;
        f.faceEyes = false;
        ModelSpec.Builder b = f.build();
        // the great shell: dome segments over the back, trimmed with the style colour
        p(b, "shell", "torso", deep).accent(style).at(0, -6, 4).rot(-10, 0, 0).mat("chitin")
                .box(-9, -8, -1, 18, 14, 6).add();
        p(b, "shell_top", "shell", shell).at(0, -8, 1).rot(-20, 0, 0).mat("chitin").box(-7, -3, -2, 14, 3, 6).add();
        p(b, "shell_rim", "shell", style).at(0, 6, 2).mat("metal").box(-9.5f, 0, -1.5f, 19, 1, 4).add();
        s(b, "shell_side", "shell", false, shade(deep, 0.9f)).at(9, -1, 1).rot(0, -20, 0).mat("chitin").box(-1, -6, -2, 3, 12, 5).add();
        for (int i = 0; i < 3; i++) {
            p(b, "spike" + i, "shell", 0xD8D0B0).at(-5 + i * 5, -8 + (i == 1 ? -1 : 0), 3).rot(-35, 0, (i - 1) * 20).mat("horn")
                    .box(-1, -4, -1, 2, 4, 2).box(-0.5f, -6, -0.5f, 1, 2, 1).add();
        }
        p(b, "shell_band", "shell", style).at(0, -2, 5.1f).mat("metal").box(-7, 0, -0.5f, 14, 1, 1).box(-0.5f, -7, -0.6f, 1, 13, 1).add();
        // helm with a glowing visor slit and a crest
        p(b, "helm", "head", deep).accent(style).at(0, -3, -1).mat("metal").pattern("visor")
                .box(-4.5f, -2.5f, -5.5f, 9, 7, 8).add();
        p(b, "visor", "helm", style).at(0, 0.5f, -5.6f).mat("energy").glow("full").box(-3, -0.5f, -0.5f, 6, 1, 1).add();
        p(b, "helm_crest", "helm", shell).at(0, -2.5f, -2).rot(-15, 0, 0).mat("chitin").box(-1, -3, -3, 2, 3, 8).add();
        s(b, "mandible", "head", false, 0xD0C8A8).at(2, 3, -5).rot(10, 15, 0).mat("horn").box(-0.5f, 0, -2, 1, 3, 2).add();
        s(b, "pauldron", "shoulder", true, shell).accent(style).at(2, -3.5f, 0).rot(0, 0, -20).mat("chitin")
                .box(-3, -2, -5, 8, 3, 10).add();
        s(b, "pauldron_trim", "pauldron", true, style).at(5, 1, 0).mat("metal").box(-0.5f, -1, -5, 1, 2, 10).add();
        s(b, "greave", "shin", true, shell).at(0, 0.5f, -2.5f).mat("chitin").box(-2.5f, 0, -0.8f, 5, 5, 1).add();
        s(b, "vambrace", "forearm", true, shell).at(0, 1, 0).mat("chitin").box(-3, 0, -3, 6, 4, 6).add();
        p(b, "belt", "hips", 0x3A2E20).at(0, -3, 0).mat("leather").box(-6.5f, -1, -5, 13, 2, 10).add();
        p(b, "tabard", "hips", style).at(0, -2, -4.8f).rot(-6, 0, 0).mat("cloth").box(-2.5f, 0, -0.5f, 5, 8, 1).add();
        switch (weapon) {
            case "blade" -> {
                p(b, "blade", "hand_r", 0xC8C8D0).accent(style).at(0, 2, -1).rot(-85, 0, 0).mat("metal").glow("runes")
                        .box(-0.5f, -2, -1.5f, 1, 16, 3).box(-0.5f, 14, -1, 1, 2, 2).add();
                p(b, "guard", "blade", style).at(0, -2, 0).mat("metal").box(-1, -1, -3, 2, 1, 6).add();
                p(b, "hilt", "blade", 0x3A2A1A).at(0, -2, 0).mat("leather").box(-0.5f, -4, -0.5f, 1, 3, 1).add();
            }
            case "launcher" -> {
                p(b, "launcher", "hand_r", deep).accent(style).at(0, 2, -1).rot(-80, 0, 0).mat("metal")
                        .box(-2, -1, -2, 4, 9, 4).box(-1.5f, 8, -1.5f, 3, 2, 3).add();
                p(b, "launcher_rock", "launcher", 0x8A7A60).at(0, 10, 0).mat("rock").box(-1.5f, 0, -1.5f, 3, 3, 3).add();
                p(b, "launcher_glow", "launcher", style).at(0, 3, -2.1f).mat("energy").glow("full").box(-1, 0, -0.5f, 2, 4, 1).add();
                p(b, "rock_pack", "shell", 0x8A7A60).at(0, 2, 5).mat("rock").box(-3, -3, 0, 3, 3, 3).box(0.5f, -1, 0, 3, 3, 3).add();
            }
            default -> {
                p(b, "staff", "hand_r", 0x4A3220).at(0, 2, -1).rot(10, 0, 0).mat("wood").box(-0.5f, -14, -0.5f, 1, 20, 1).add();
                p(b, "staff_claw", "staff", shell).at(0, -14, 0).mat("chitin").box(-1.5f, -2, -1.5f, 3, 2, 3)
                        .box(-2, -5, -0.5f, 1, 3, 1).box(1, -5, -0.5f, 1, 3, 1).add();
                p(b, "staff_orb", "staff", Role.SPIN, style).at(0, -17, 0).mat("energy").glow("full").box(-1.5f, -1.5f, -1.5f, 3, 3, 3).add();
            }
        }
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Vespula and her soldiers
    // ------------------------------------------------------------------------------------------

    /**
     * A giant abyssal wasp: segmented thorax, striped abdomen ending in a barbed stinger, compound eyes, mandibles,
     * antennae, two pairs of veined wings and six jointed legs. The queen (Vespula) adds a spiked crest and dangling
     * hooked forelegs.
     */
    static ModelSpec wasp(String id, float scale, boolean queen) {
        int yellow = queen ? 0xE0A824 : 0xC89022;
        int black = 0x2A2014;
        int dark = 0x18120A;
        int wing = 0xD8E8F0;
        ModelSpec.Builder b = ModelSpec.builder(id).scale(scale).shadow(0.9f * scale);
        p(b, "thorax", null, Role.FLOAT, black).accent(yellow).at(0, 7, 0).mat("chitin").box(-5, -5, -5, 10, 9, 10).add();
        p(b, "thorax_band", "thorax", yellow).at(0, -5, 0).mat("chitin").box(-4, -1, -4, 8, 2, 8).add();
        p(b, "thorax_plate", "thorax", shade(black, 1.4f)).at(0, -6, -2).rot(-10, 0, 0).mat("chitin").box(-3.5f, -1, -2, 7, 1, 6).add();
        p(b, "waist", "thorax", dark).at(0, 0, 5).mat("chitin").box(-2, -2, -0.5f, 4, 4, 3).add();
        // abdomen: three striped segments tapering to the stinger
        p(b, "abdomen", "waist", Role.TAIL, yellow).at(0, 0, 2.5f).rot(-25, 0, 0).mat("chitin").box(-6, -5, 0, 12, 10, 6).add();
        p(b, "abd_band1", "abdomen", black).at(0, 0, 5).mat("chitin").box(-6.5f, -5.5f, 0, 13, 11, 2).add();
        p(b, "abdomen2", "abdomen", yellow).at(0, 0, 7).rot(8, 0, 0).mat("chitin").box(-5, -4.5f, 0, 10, 9, 5).add();
        p(b, "abd_band2", "abdomen2", black).at(0, 0, 4).mat("chitin").box(-5.5f, -5, 0, 11, 10, 2).add();
        p(b, "abdomen3", "abdomen2", yellow).at(0, 0, 6).rot(10, 0, 0).mat("chitin").box(-3.5f, -3, 0, 7, 6, 4).add();
        p(b, "abd_tip", "abdomen3", black).at(0, 0, 4).rot(10, 0, 0).mat("chitin").box(-2, -2, 0, 4, 4, 3).add();
        p(b, "stinger", "abd_tip", 0xD8C8A0).at(0, 0, 3).rot(10, 0, 0).mat("horn").box(-0.5f, -0.5f, 0, 1, 1, 5).box(-1, -1, 0, 2, 2, 2).add();
        // head
        p(b, "head", "thorax", Role.HEAD, black).accent(yellow).at(0, -1, -5).mat("chitin").box(-4, -4, -5, 8, 7, 5).add();
        s(b, "compound_eye", "head", false, 0xC03018).at(4, -1.5f, -2.5f).rot(0, -10, 0).mat("energy").glow("full")
                .box(-1.5f, -2.5f, -2.5f, 2, 5, 4).add();
        p(b, "face", "head", yellow).at(0, 1, -5).mat("chitin").box(-2, -2, -0.6f, 4, 3, 1).add();
        s(b, "mandible", "head", false, 0x8A6A20).roles(Role.JAW, Role.JAW).at(1.5f, 2.5f, -5).rot(10, 20, 0).mat("horn")
                .box(-0.5f, 0, -3, 2, 1, 3).box(-1, 0, -3.5f, 1, 2, 1).add();
        s(b, "antenna", "head", false, dark).at(1.5f, -4, -4).rot(-30, 0, -20).mat("chitin").box(-0.5f, -6, -0.5f, 1, 6, 1).add();
        s(b, "antenna_tip", "antenna", true, dark).at(0, -6, 0).rot(-50, 0, 0).mat("chitin").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        // wings: front and rear pairs, with a vein strut each
        s(b, "wing", "thorax", false, wing).roles(Role.WING_LEFT, Role.WING_RIGHT).at(3.5f, -5, -2).rot(0, -15, -10)
                .mat("membrane").box(0, -0.5f, -3, 18, 1, 7).box(14, -0.5f, -2, 4, 1, 4).add();
        s(b, "wing_vein", "wing", true, 0x3A3A3A).at(0, -0.6f, 0).mat("chitin").box(0, -0.5f, -1, 16, 1, 1).box(4, -0.5f, 1, 10, 1, 1).add();
        s(b, "wing2", "thorax", false, wing).roles(Role.WING_LEFT, Role.WING_RIGHT).at(3.5f, -5, 2).rot(0, 20, -5)
                .mat("membrane").box(0, -0.5f, -2, 13, 1, 6).add();
        // six legs: thigh, shin and hooked foot
        float[] zs = {-3, 0, 3};
        float[] yaws = {-25, 0, 25};
        for (int i = 0; i < 3; i++) {
            String n = "leg" + i;
            s(b, n, "thorax", false, black).roles(Role.LEG_EXTRA, Role.LEG_EXTRA).at(4, 3, zs[i]).rot(0, yaws[i], 30).mat("chitin")
                    .box(0, -1, -1, 6, 2, 2).add();
            s(b, n + "_shin", n, true, yellow).at(6, 0, 0).rot(0, 0, -60).mat("chitin").box(-0.5f, -0.5f, -0.5f, 1, 7, 1).add();
            s(b, n + "_hook", n + "_shin", true, dark).at(0, 7, 0).rot(30, 0, 0).mat("horn").box(-0.5f, -0.5f, -2, 1, 1, 2).add();
        }
        if (queen) {
            spikes(b, "crest", "thorax", 0xD8C8A0, "horn", -6, new float[][]{{-2.5f, -1, 3}, {0, 0, 4}, {2.5f, -1, 3}}, -15);
            s(b, "foreleg", "thorax", false, black).at(3, 2, -5).rot(-40, 0, -20).mat("chitin").box(-1, 0, -1, 2, 6, 2).add();
            s(b, "foreleg_hook", "foreleg", true, 0xD8C8A0).at(0, 6, 0).rot(-60, 0, 0).mat("horn").box(-0.5f, 0, -0.5f, 1, 4, 1)
                    .box(-0.5f, 3, -2, 1, 1, 2).add();
            p(b, "venom_sac", "abdomen2", 0x9AE040).at(0, 4.6f, 2).mat("slime").glow("full").box(-2, -0.5f, -1, 4, 1, 3).add();
        }
        return b.build();
    }

    /** Vespula's abyssal portal: a twisted bone-and-stone arch around a swirling purple vortex with a staring eye. */
    static ModelSpec portal() {
        int stone = 0x3E3048;
        int dark = 0x261C2E;
        ModelSpec.Builder b = ModelSpec.builder("vespula_portal").shadow(0.9f);
        p(b, "base", null, dark).at(0, 24, 0).mat("rock").box(-14, -2, -5, 28, 2, 10).box(-12, -3, -4, 24, 1, 8).add();
        s(b, "pillar", "base", false, stone).at(11, -3, 0).rot(0, 0, 4).mat("rock").box(-2, -34, -2.5f, 4, 34, 5).add();
        s(b, "pillar_band", "pillar", true, dark).at(0, -10, 0).mat("rock").box(-2.5f, -1, -3, 5, 2, 6).box(-2.5f, -22, -3, 5, 2, 6).add();
        s(b, "pillar_spike", "pillar", true, 0xD8C8B0).at(1.5f, -18, 0).rot(0, 0, -50).mat("horn").box(-0.5f, -4, -1, 1, 4, 2)
                .box(-0.5f, 6, -1, 1, 3, 2).add();
        p(b, "arch", "base", stone).at(0, -37, 0).mat("rock").box(-12, -3, -2.5f, 24, 3, 5).box(-8, -5, -2, 16, 2, 4).add();
        p(b, "arch_horns", "arch", 0xD8C8B0).at(0, -5, 0).mat("horn").box(-7, -4, -1, 2, 4, 2).box(5, -4, -1, 2, 4, 2)
                .box(-1, -6, -1, 2, 6, 2).add();
        p(b, "vortex", null, Role.SPIN, 0x8A3AE0).at(0, 5, 0).mat("energy").glow("full").box(-9, -15, -1, 18, 30, 2).add();
        p(b, "vortex_ring", "vortex", 0xC070FF).at(0, 0, 0).mat("energy").glow("full")
                .box(-6, -11, -1.5f, 12, 2, 3).box(-6, 9, -1.5f, 12, 2, 3).box(-7, -9, -1.5f, 2, 18, 3).box(5, -9, -1.5f, 2, 18, 3).add();
        p(b, "vortex_inner", "vortex", 0x3A0A6A).at(0, 0, 0).mat("energy").glow("full").box(-4, -7, -1.2f, 8, 14, 3).add();
        p(b, "eye", "vortex", Role.FLOAT, 0xF06060).at(0, 0, -1.5f).mat("energy").glow("full").box(-2, -1.5f, -1, 4, 3, 1).add();
        p(b, "pupil", "eye", 0x200808).at(0, 0, -1).mat("energy").box(-0.5f, -1, -0.4f, 1, 2, 1).add();
        p(b, "tendrils", "base", 0x5A2A6A).at(0, -2, 0).mat("flesh").box(-10, -3, -4, 2, 3, 2).box(8, -4, 2, 2, 4, 2)
                .box(-4, -2, 3, 3, 2, 2).box(3, -2, -5, 2, 2, 2).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Ice demon
    // ------------------------------------------------------------------------------------------

    /** The CoX Ice demon: a towering frost-crusted demon with curling horns, icicle spines and ragged wings. */
    static ModelSpec iceDemon() {
        int ice = 0x8AC8E8;
        int deep = 0x3A7AB0;
        int dark = 0x2A5A8A;
        int frost = 0xE8F8FF;
        BestiaryRad.Frame f = new BestiaryRad.Frame("cox_ice_demon", 1.0f, 1.0f).colors(ice, deep, dark, 0xC0F0FF)
                .legs(7, 7, 6, 4.5f).torso(17, 14, 10, 18).arms(9, 9, 5, 6).head(8, 7, 8).hips(12, 5, 9);
        f.mat = "ice";
        f.claws = 3;
        f.clawLen = 3;
        f.clawColor = frost;
        f.clawMat = "ice";
        f.toes = 3;
        f.neckLen = 2;
        f.headGlow = "eyes";
        ModelSpec.Builder b = f.build();
        s(b, "horn", "head", false, frost).at(3.5f, -4, -1).rot(-10, 0, -40).mat("ice").box(-1.5f, -4, -1.5f, 3, 4, 3).add();
        s(b, "horn2", "horn", true, frost).at(0, -4, 0).rot(-40, 0, 20).mat("ice").box(-1, -4, -1, 2, 4, 2).add();
        s(b, "horn3", "horn2", true, 0xFFFFFF).at(0, -4, 0).rot(-40, 0, 10).mat("ice").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        teeth(b, "fangs", "head", frost, -2.5f, 2.5f, 1.6f, -6.6f, 4, 2);
        spikes(b, "back_icicles", "torso", frost, "ice", -13, new float[][]{{-5, 1, 4}, {-1.5f, 2, 6}, {1.5f, 2, 6}, {5, 1, 4}}, -40);
        spikes(b, "back_icicles2", "torso", frost, "ice", -7, new float[][]{{-3, 4, 4}, {3, 4, 4}}, -60);
        s(b, "shoulder_ice", "shoulder", true, frost).at(2, -3, 0).rot(0, 0, -30).mat("ice").glow("full")
                .box(-1, -5, -2, 2, 5, 2).box(1, -3, 1, 2, 3, 2).add();
        p(b, "chest_frost", "torso", frost).at(0, -13, -5.2f).mat("ice").box(-6, 0, -0.5f, 12, 2, 1).add();
        // ragged wings
        s(b, "wing", "torso", false, dark).roles(Role.WING_LEFT, Role.WING_RIGHT).at(3, -11, 5).rot(10, -30, -20).mat("membrane")
                .box(0, -1, -0.5f, 12, 2, 2).add();
        s(b, "wing_mem", "wing", true, shade(deep, 0.9f)).at(0, 1, 0.5f).mat("membrane").box(0, 0, 0, 12, 12, 1)
                .box(2, 12, 0, 6, 3, 1).box(9, 12, 0, 3, 2, 1).add();
        s(b, "wing_finger", "wing", true, frost).at(12, 0, 0.5f).mat("ice").box(-1, -2, -0.5f, 1, 15, 1).add();
        p(b, "tail", "hips", Role.TAIL, deep).at(0, -2, 4).rot(25, 0, 0).mat("ice").box(-1.5f, -1.5f, 0, 3, 3, 8).add();
        p(b, "tail2", "tail", Role.TAIL, dark).at(0, 0, 8).rot(10, 0, 0).mat("ice").box(-1, -1, 0, 2, 2, 6).add();
        p(b, "tail_tip", "tail2", frost).at(0, 0, 6).mat("ice").glow("full").box(-1.5f, -1.5f, 0, 3, 3, 3).add();
        s(b, "icicle_arm", "forearm", true, frost).at(2.5f, 4, 2).rot(30, 0, -40).mat("ice").box(-0.5f, -4, -0.5f, 1, 4, 1).add();
        p(b, "frost_aura", "hips", Role.SPIN, 0xD0F0FF).at(0, 9, 0).mat("energy").glow("full")
                .box(-8, 0, -8, 3, 1, 3).box(5, 0, 5, 3, 1, 3).box(5, 0, -8, 2, 1, 2).box(-7, 0, 5, 2, 1, 2).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // The Great Olm
    // ------------------------------------------------------------------------------------------

    /**
     * The Great Olm's head, thrust out of the cavern wall: a vast pale salamander skull with a crystal-crowned brow,
     * glowing eyes, gill frills, barbels and a gaping jaw ringed with teeth.
     */
    static ModelSpec olmHead() {
        int hide = 0xD6CCB0;
        int dark = 0x8A7E62;
        int spot = 0x9A9078;
        int rock = 0x4A3A30;
        ModelSpec.Builder b = ModelSpec.builder("great_olm").shadow(1.5f);
        // wall and ledge
        p(b, "wall", null, rock).at(0, 24, 12).mat("rock").box(-22, -40, 0, 44, 40, 6).add();
        p(b, "wall_rocks", "wall", shade(rock, 0.8f)).at(0, 0, 0).mat("rock")
                .box(-20, -38, -3, 8, 10, 3).box(12, -36, -3, 9, 12, 3).box(-21, -14, -4, 7, 14, 4).box(14, -16, -4, 8, 16, 4)
                .box(-6, -40, -2, 12, 4, 2).add();
        p(b, "ledge", "wall", 0x5A4838).at(0, 0, -6).mat("rock").box(-18, -4, -14, 36, 4, 20).box(-14, -5, -10, 28, 1, 14).add();
        // neck and skull
        p(b, "neck", "wall", hide).accent(spot).at(0, -16, 0).mat("scales").pattern("spots").box(-11, -10, -6, 22, 18, 7).add();
        p(b, "head", "neck", Role.HEAD, hide).accent(spot).at(0, -1, -6).mat("scales").pattern("spots")
                .box(-13, -10, -16, 26, 13, 17).add();
        p(b, "cranium", "head", shade(hide, 1.05f)).accent(spot).at(0, -10, -6).rot(-6, 0, 0).mat("scales")
                .box(-10, -3, -8, 20, 3, 15).add();
        p(b, "brow", "head", dark).at(0, -8, -16).rot(12, 0, 0).mat("scales").box(-13.5f, -2, -2, 27, 3, 4).add();
        s(b, "eye_socket", "head", false, 0x2A2420).at(8, -5, -16).mat("flesh").box(-3, -2, -0.6f, 6, 4, 1).add();
        eyes(b, "eye", "head", 0x9AF0E0, 8, -5, -16.4f, 4, 2);
        p(b, "snout", "head", hide).accent(spot).at(0, -3, -16).mat("scales").box(-10, -4, -8, 20, 6, 8).add();
        p(b, "snout_top", "snout", shade(hide, 1.08f)).at(0, -4, -4).mat("scales").box(-8, -1, -4, 16, 1, 7).add();
        s(b, "nostril", "snout", false, 0x3A2A22).at(4, -3, -8).mat("flesh").box(-1.5f, -1, -0.6f, 3, 2, 1).add();
        teeth(b, "teeth_top", "snout", TOOTH, -9, 9, 2, -7.3f, 10, 2);
        s(b, "teeth_side", "snout", false, TOOTH).at(9.6f, 2, 0).mat("horn").box(-0.5f, 0, -7, 1, 2, 1).box(-0.5f, 0, -4.5f, 1, 2, 1)
                .box(-0.5f, 0, -2, 1, 2, 1).add();
        p(b, "gullet", "head", 0x6A1A1A).at(0, 3, -14).mat("flesh").box(-9, -1, -2, 18, 2, 12).add();
        p(b, "jaw", "head", Role.JAW, dark).accent(spot).at(0, 3, -6).rot(18, 0, 0).mat("scales").box(-11, 0, -17, 22, 4, 17).add();
        p(b, "chin", "jaw", shade(dark, 0.9f)).at(0, 4, -8).mat("scales").box(-9, 0, -8, 18, 1, 12).add();
        p(b, "tongue", "jaw", 0xA04A4A).at(0, 0, -6).mat("flesh").box(-4, -1, -8, 8, 1, 10).add();
        teeth(b, "teeth_low", "jaw", TOOTH, -9, 9, 0, -16.3f, 9, -2);
        s(b, "teeth_low_side", "jaw", false, TOOTH).at(10.4f, 0, 0).mat("horn").box(-0.5f, -2, -15, 1, 2, 1).box(-0.5f, -2, -11, 1, 2, 1)
                .box(-0.5f, -2, -7, 1, 2, 1).add();
        // crystal crown
        float[][] cr = {{-7, 0, -2, 9, -15, 20}, {-2, 0, 0, 12, -10, 4}, {4, 0, -1, 10, -12, -16}, {9, 0, 2, 7, -5, -30},
            {-10, 0, 3, 6, 5, 32}, {1, 0, 4, 8, 20, -6}, {-4, 0, 5, 6, 25, 15}};
        for (int i = 0; i < cr.length; i++) {
            float[] q = cr[i];
            int h = (int) q[3];
            p(b, "crystal" + i, "cranium", i % 2 == 0 ? CRYSTAL : 0xB0F8F0).at(q[0], -3, q[2]).rot(q[4], 0, q[5]).mat("ice")
                    .glow(i % 3 == 0 ? "full" : null).box(-1.5f, -h, -1.5f, 3, h, 3).box(-0.5f, -h - 2, -0.5f, 1, 2, 1).add();
        }
        // gill frills and barbels
        for (int k = 0; k < 3; k++) {
            s(b, "gill" + k, "head", false, 0xC08A8A).at(13, -7 + k * 3.5f, -2 - k).rot(0, -30 + k * 10, -30 + k * 25).mat("membrane")
                    .box(0, -1, -1, 6 - k, 2, 4).add();
        }
        s(b, "barbel", "snout", false, dark).roles(Role.TENTACLE, Role.TENTACLE).at(10, 0, -5).rot(0, 20, 30).mat("flesh")
                .box(0, -0.5f, -0.5f, 6, 1, 1).add();
        s(b, "barbel_tip", "barbel", true, dark).at(6, 0, 0).rot(0, 0, 40).mat("flesh").box(0, -0.5f, -0.5f, 5, 1, 1).add();
        return b.build();
    }

    /** One of the Olm's claws, reaching out of the wall: knuckled palm, three jointed taloned fingers, a thumb and crystals. */
    static ModelSpec olmClaw(String id, boolean left) {
        int hide = 0xD6CCB0;
        int dark = 0x8A7E62;
        int spot = 0x9A9078;
        int nail = 0x3E3428;
        int rock = 0x4A3A30;
        int crystal = left ? CRYSTAL : 0x9AD0A8;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.1f);
        p(b, "rock", null, rock).at(0, 24, 6).mat("rock").box(-14, -4, -10, 28, 4, 18).box(-12, -12, 4, 24, 8, 4)
                .box(-10, -5, -8, 6, 1, 4).add();
        p(b, "wrist", "rock", hide).accent(spot).at(0, -7, 4).mat("scales").pattern("spots").box(-6, -5, -6, 12, 9, 7).add();
        p(b, "palm", null, Role.BODY, hide).accent(spot).at(0, 18, 0).mat("scales").pattern("spots")
                .box(-9, -8, -7, 18, 9, 12).add();
        p(b, "palm_back", "palm", shade(hide, 1.06f)).at(0, -8, -1).rot(-6, 0, 0).mat("scales").box(-8, -1.5f, -5, 16, 2, 10).add();
        p(b, "knuckles", "palm", dark).at(0, -6, -7).mat("scales")
                .box(-8.5f, -1, -1.5f, 5, 4, 3).box(-2.5f, -1.5f, -1.5f, 5, 4, 3).box(3.5f, -1, -1.5f, 5, 4, 3).add();
        float[] fx = {-6, 0, 6};
        int[] len = {6, 7, 6};
        Role[] roles = {Role.ARM_LEFT, Role.ARM_RIGHT, Role.ARM_LEFT};
        for (int i = 0; i < 3; i++) {
            String n = "finger" + i;
            p(b, n, "palm", roles[i], hide).accent(spot).at(fx[i], -4, -7.5f).rot(-10, (i - 1) * -10, 0).mat("scales")
                    .box(-2, -2, -len[i], 4, 4, len[i]).add();
            p(b, n + "_mid", n, dark).at(0, 0, -len[i]).rot(35, 0, 0).mat("scales").box(-1.5f, -1.5f, -5, 3, 3, 5).add();
            p(b, n + "_tip", n + "_mid", hide).at(0, 0, -5).rot(30, 0, 0).mat("scales").box(-1.5f, -1.5f, -3, 3, 3, 3).add();
            p(b, n + "_nail", n + "_tip", nail).at(0, 0, -3).rot(35, 0, 0).mat("horn").box(-1, -1, -4, 2, 2, 4).box(-0.5f, -0.5f, -6, 1, 1, 2).add();
        }
        float tx = left ? -9 : 9;
        p(b, "thumb", "palm", Role.ARM_RIGHT, hide).accent(spot).at(tx, -3, -3).rot(0, left ? 40 : -40, 0).mat("scales")
                .box(-2, -2, -6, 4, 4, 6).add();
        p(b, "thumb_tip", "thumb", dark).at(0, 0, -6).rot(30, 0, 0).mat("scales").box(-1.5f, -1.5f, -4, 3, 3, 4).add();
        p(b, "thumb_nail", "thumb_tip", nail).at(0, 0, -4).rot(30, 0, 0).mat("horn").box(-1, -1, -3, 2, 2, 3).add();
        float[][] cr = {{-4, -2, 8, -15, 20}, {1, 0, 10, -5, -5}, {5, -1, 7, -20, -25}, {-1, 3, 6, 25, 10}, {6, 3, 5, 20, -35}};
        for (int i = 0; i < cr.length; i++) {
            float[] q = cr[i];
            int h = (int) q[2];
            p(b, "crystal" + i, "palm_back", i % 2 == 0 ? crystal : shade(crystal, 1.15f)).at(q[0], -1.5f, q[1]).rot(q[3], 0, q[4])
                    .mat("ice").glow(left && i % 2 == 0 ? "full" : null).box(-1.5f, -h, -1.5f, 3, h, 3).box(-0.5f, -h - 2, -0.5f, 1, 2, 1).add();
        }
        p(b, "scars", "palm", dark).at(0, -4, 5).mat("scales").box(-7, -3, -0.5f, 3, 1, 1).box(3, -5, -0.5f, 4, 1, 1).add();
        return b.build();
    }
}
