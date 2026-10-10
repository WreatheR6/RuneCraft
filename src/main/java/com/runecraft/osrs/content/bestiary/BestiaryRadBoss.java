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

/** Minigame bosses outside the raids: the Gauntlet's Hunllef, the Nightmare of Ashihama and Zalcano, with their minions. */
final class BestiaryRadBoss {
    private BestiaryRadBoss() {
    }

    static List<ModelSpec> all() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(hunllef("crystalline_hunllef", 0x7AC4E4, 0x3E7AA8, 0xD8F6FF));
        l.add(hunllef("corrupted_hunllef", 0xB0322A, 0x5E1414, 0xFF7A50));
        l.add(tornado());
        l.add(nightmare("the_nightmare", 0x2E2A3E, 0xA058D8, 0xD8D0E4));
        l.add(nightmare("phosanis_nightmare", 0x3E2A2E, 0xE05A50, 0xE4D0D0));
        l.add(totem());
        l.add(parasite());
        l.add(zalcano());
        l.add(golem());
        return l;
    }

    // ------------------------------------------------------------------------------------------
    // The Hunllef
    // ------------------------------------------------------------------------------------------

    /**
     * The Hunllef: a great crystal-grown beast, half wolf and half lion - long legs with crystal-clawed paws, a
     * deep chest, a ridge of crystal blades down the back, a crested, fanged head and a long tail ending in a shard.
     */
    static ModelSpec hunllef(String id, int crystal, int deep, int light) {
        int dark = shade(deep, 0.75f);
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.6f);
        p(b, "torso", null, Role.BODY, crystal).accent(light).at(0, 9, 1).rot(-8, 0, 0).mat("ice").box(-7, -6, -10, 14, 11, 20).add();
        p(b, "chest", "torso", deep).accent(light).at(0, -2, -10).rot(-10, 0, 0).mat("ice").box(-6.5f, -4, -3, 13, 10, 4).add();
        p(b, "belly", "torso", dark).at(0, 5, 0).mat("ice").box(-5, -1, -8, 10, 2, 16).add();
        p(b, "haunch", "torso", deep).accent(light).at(0, -4, 7).mat("ice").box(-7.5f, -3, -3, 15, 7, 6).add();
        // crystal blades along the spine
        float[][] blades = {{0, -8, 7}, {0, -4, 9}, {0, 0, 9}, {0, 4, 7}, {0, 8, 5}};
        for (int i = 0; i < blades.length; i++) {
            p(b, "blade" + i, "torso", i % 2 == 0 ? light : crystal).accent(light).at(0, -6, blades[i][1]).rot(-25, 0, (i % 2 == 0 ? 8 : -8))
                    .mat("ice").glow(i % 2 == 0 ? "full" : null).box(-1, -blades[i][2], -2, 2, (int) blades[i][2], 4)
                    .box(-0.5f, -blades[i][2] - 2, -1, 1, 2, 2).add();
        }
        s(b, "flank_shard", "torso", false, light).at(7, -4, -2).rot(-20, 0, -40).mat("ice").glow("full").box(-1, -5, -1, 2, 5, 2)
                .box(1, -3, 3, 2, 3, 2).add();
        // neck and crested head
        p(b, "neck", "torso", Role.NECK, crystal).accent(light).at(0, -3, -10).rot(35, 0, 0).mat("ice").box(-4, -8, -4, 8, 9, 8).add();
        p(b, "mane", "neck", deep).accent(light).at(0, -7, 2).mat("ice").box(-5, 0, -1, 10, 8, 4).add();
        p(b, "head", "neck", Role.HEAD, crystal).accent(light).at(0, -8, -1).rot(-40, 0, 0).mat("ice").pattern("eyes").glow("eyes")
                .box(-4.5f, -4, -7, 9, 7, 8).add();
        p(b, "snout", "head", deep).accent(light).at(0, 0, -7).mat("ice").box(-3, -2.5f, -5, 6, 4, 5).add();
        p(b, "nose", "snout", dark).at(0, -2.5f, -5).mat("ice").box(-1.5f, 0, -0.6f, 3, 1, 1).add();
        teeth(b, "fangs", "snout", light, -2.5f, 2.5f, 1.5f, -4.6f, 4, 1);
        p(b, "jaw", "head", Role.JAW, dark).accent(light).at(0, 3, -5).rot(14, 0, 0).mat("ice").glow("maw").box(-3, 0, -7, 6, 2, 7).add();
        teeth(b, "fangs_low", "jaw", light, -2.5f, 2.5f, 0, -6.5f, 3, -1);
        p(b, "crest", "head", light).accent(light).at(0, -4, -2).rot(-35, 0, 0).mat("ice").glow("full").box(-1, -6, -2, 2, 6, 4)
                .box(-0.5f, -8, -1, 1, 2, 2).add();
        s(b, "horn", "head", false, light).at(3.5f, -4, -1).rot(-45, 0, -25).mat("ice").box(-1, -6, -1, 2, 6, 2).box(-0.5f, -8, -0.5f, 1, 2, 1).add();
        s(b, "ear_crystal", "head", false, crystal).at(4.5f, -2, 0).rot(-10, 0, -60).mat("ice").box(-1, -4, -1, 2, 4, 2).add();
        // legs
        String[] names = {"leg_f", "leg_b"};
        float[] zs = {-7, 7};
        Role[][] roles = {{Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT}, {Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT}};
        for (int i = 0; i < 2; i++) {
            String n = names[i];
            boolean back = i == 1;
            s(b, n, "torso", false, crystal).roles(roles[i][0], roles[i][1]).accent(light).at(6, 1, zs[i]).rot(back ? -25 : 0, 0, -5)
                    .mat("ice").box(-2.5f, -2, -3, 5, 8, 6).add();
            s(b, n + "_shin", n, true, deep).accent(light).at(0, 7, 0).rot(back ? 45 : 8, 0, 3).mat("ice").box(-2, 0, -2, 4, 7, 4).add();
            s(b, n + "_paw", n + "_shin", true, dark).at(0, 6.5f, 0).rot(back ? -20 : -6, 0, 0).mat("ice").box(-2.5f, 0, -4, 5, 2, 5).add();
            s(b, n + "_claw", n + "_paw", true, light).at(0, 1, -4).rot(20, 0, 0).mat("ice").glow("full")
                    .box(-2.5f, -0.5f, -2, 1, 1, 2).box(-0.5f, -0.5f, -2.5f, 1, 1, 3).box(1.5f, -0.5f, -2, 1, 1, 2).add();
            s(b, n + "_spur", n, true, light).at(2.5f, 3, 0).rot(0, 0, -50).mat("ice").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        }
        // tail
        p(b, "tail", "torso", Role.TAIL, crystal).accent(light).at(0, -3, 10).rot(-25, 0, 0).mat("ice").box(-2, -2, 0, 4, 4, 7).add();
        p(b, "tail2", "tail", Role.TAIL, deep).accent(light).at(0, 0, 7).rot(15, 0, 0).mat("ice").box(-1.5f, -1.5f, 0, 3, 3, 7).add();
        p(b, "tail3", "tail2", Role.TAIL, crystal).accent(light).at(0, 0, 7).rot(15, 0, 0).mat("ice").box(-1, -1, 0, 2, 2, 5).add();
        p(b, "tail_shard", "tail3", light).at(0, 0, 5).mat("ice").glow("full").box(-1.5f, -3, 0, 3, 6, 3).box(-0.5f, -4, 1, 1, 8, 2).add();
        return b.build();
    }

    /** A Hunllef tornado: a whirling funnel of crystal wind, bands widening upward, studded with spinning shards. */
    static ModelSpec tornado() {
        int c = 0x8AC4EC;
        ModelSpec.Builder b = ModelSpec.builder("hunllef_tornado").shadow(0.5f);
        p(b, "base", null, Role.SPIN, c).accent(0xFFFFFF).at(0, 24, 0).mat("ice").box(-2, -6, -2, 4, 6, 4).add();
        String parent = "base";
        int[] sizes = {6, 8, 10, 12};
        for (int i = 0; i < sizes.length; i++) {
            int w = sizes[i];
            String n = "band" + i;
            p(b, n, parent, i % 2 == 0 ? 0xD8F0FF : c).accent(0xFFFFFF).at(0, -6, 0).rot(0, 25, 0).mat("ice").glow(i % 2 == 0 ? "full" : null)
                    .box(-w / 2f, -6, -w / 2f, w, 6, w).add();
            parent = n;
        }
        p(b, "shards", "band1", Role.SPIN, 0xF0FAFF).at(0, -3, 0).mat("ice").glow("full")
                .box(-8, -1, -1, 2, 2, 2).box(6, -2, 1, 2, 2, 2).box(-1, 0, 6, 2, 2, 2).box(1, -1, -8, 2, 2, 2).add();
        p(b, "shards2", "band3", Role.SPIN, 0xF0FAFF).at(0, -3, 0).mat("ice").glow("full")
                .box(-10, -1, 2, 2, 2, 2).box(8, -2, -2, 2, 2, 2).box(2, 0, 8, 2, 2, 2).box(-3, -1, -10, 2, 2, 2).add();
        p(b, "wisp", "band3", 0xFFFFFF).at(0, -6, 0).mat("energy").glow("full").box(-4, -2, -4, 3, 2, 3).box(2, -3, 1, 2, 3, 2).add();
        p(b, "debris", "base", 0x6A8AA0).at(0, 0, 0).mat("rock").box(-5, -1, -3, 2, 1, 2).box(3, -1, 2, 2, 1, 2).box(1, -1, -5, 1, 1, 1).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // The Nightmare
    // ------------------------------------------------------------------------------------------

    /**
     * The Nightmare of Ashihama: a towering, hovering wraith in layered tattered robes - a pale porcelain mask under a
     * vast crown of curving horns, spiked shoulders, long two-jointed arms ending in sickle claws, and writhing
     * shadow tendrils where legs should be.
     */
    static ModelSpec nightmare(String id, int robe, int glow, int mask) {
        int robeDark = shade(robe, 0.65f);
        int robeLight = shade(robe, 1.3f);
        int horn = shade(robe, 0.5f);
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.5f);
        p(b, "body", null, Role.FLOAT, robe).accent(glow).at(0, 6, 0).mat("cloth").box(-6, -12, -4, 12, 13, 8).add();
        p(b, "chest", "body", robeDark).accent(glow).at(0, -12, -4).rot(-6, 0, 0).mat("cloth").box(-5, 0, -1, 10, 7, 2).add();
        p(b, "sternum", "chest", mask).at(0, 1, -1.2f).mat("bone").box(-0.5f, 0, -0.5f, 1, 5, 1).box(-3, 1, -0.4f, 6, 1, 1)
                .box(-2.5f, 3, -0.4f, 5, 1, 1).add();
        p(b, "heart", "chest", glow).at(0, 3.5f, -1.4f).mat("energy").glow("full").box(-1, -1, -0.5f, 2, 2, 1).add();
        // robe layers flaring downward
        p(b, "robe_mid", "body", robe).accent(glow).at(0, 1, 0).mat("cloth").box(-8, 0, -6, 16, 6, 12).add();
        p(b, "robe_low", "robe_mid", robeDark).accent(glow).at(0, 6, 0).mat("cloth").box(-10, 0, -8, 20, 5, 16).add();
        float[][] tatters = {{-7, -6, -8}, {-2.5f, -7, 0}, {2.5f, -7, 0}, {7, -6, 8}, {-8.5f, 0, -12}, {8.5f, 0, 12}, {-5, 6.5f, -6},
            {0, 7, 0}, {5, 6.5f, 6}};
        for (int i = 0; i < tatters.length; i++) {
            float[] q = tatters[i];
            boolean side = Math.abs(q[0]) > 8;
            int len = 5 + (i * 3) % 4;
            p(b, "tatter" + i, "robe_low", i % 2 == 0 ? robe : robeDark).at(q[0], 5, q[1]).rot(q[1] < -1 ? -10 : q[1] > 1 ? 10 : 0, 0, q[2])
                    .mat("cloth").box(side ? -0.5f : -2, 0, side ? -2 : -0.5f, side ? 1 : 4, len, side ? 4 : 1).add();
        }
        p(b, "sash", "body", glow).at(0, 0, 0).mat("cloth").box(-6.5f, -1, -4.5f, 13, 2, 9).add();
        p(b, "sash_tail", "sash", glow).at(-3, 1, -4.6f).rot(-5, 0, 5).mat("cloth").box(-1, 0, -0.5f, 2, 9, 1).add();
        // shadow tendrils beneath
        for (int i = 0; i < 5; i++) {
            float a = (float) (i * Math.PI * 2 / 5 + 0.3);
            float x = (float) Math.cos(a) * 5;
            float z = (float) Math.sin(a) * 5;
            String n = "tendril" + i;
            p(b, n, "robe_low", Role.TENTACLE, robeDark).accent(glow).at(x, 4, z).rot(-50, (float) Math.toDegrees(-a) - 90, 0).mat("ghost")
                    .box(-1.5f, 0, -1.5f, 3, 6, 3).add();
            p(b, n + "b", n, Role.TENTACLE, horn).accent(glow).at(0, 6, 0).rot(-25, 0, 0).mat("ghost").box(-1, 0, -1, 2, 5, 2).add();
        }
        // head: porcelain mask, hood, crown of horns
        p(b, "neck", "body", robeDark).at(0, -12, 0).mat("cloth").box(-2, -2, -2, 4, 2, 4).add();
        p(b, "head", "neck", Role.HEAD, mask).accent(glow).at(0, -2, 0).mat("bone").pattern("skull").glow("eyes")
                .box(-3.5f, -7, -4, 7, 7, 7).add();
        p(b, "hood", "head", robeDark).at(0, -7, 0).mat("cloth").box(-4.5f, -1, -4.5f, 9, 2, 9).box(-4.5f, 0, -4.5f, 1, 7, 8)
                .box(3.5f, 0, -4.5f, 1, 7, 8).box(-4, 0, 3, 8, 8, 1).add();
        p(b, "crown", "hood", horn).accent(glow).at(0, -1, 0).mat("horn").box(-3, -2, -3, 6, 2, 6).add();
        float[][] horns = {{2.5f, -10, -22, 12}, {4, 5, -48, 10}, {3, 20, -72, 8}};
        for (int i = 0; i < horns.length; i++) {
            float[] q = horns[i];
            int len = (int) q[3];
            s(b, "horn" + i, "crown", false, horn).accent(glow).at(q[0], -1, 0).rot(q[1], 0, q[2]).mat("horn")
                    .box(-1, -len, -1, 2, len, 2).add();
            s(b, "horn" + i + "_tip", "horn" + i, true, i == 0 ? glow : horn).at(0, -len, 0).rot(-20, 0, 25).mat(i == 0 ? "energy" : "horn")
                    .glow(i == 0 ? "full" : null).box(-0.5f, -4, -0.5f, 1, 4, 1).add();
        }
        p(b, "horn_c", "crown", horn).accent(glow).at(0, -2, 1).rot(-20, 0, 0).mat("horn").box(-1, -13, -1, 2, 13, 2)
                .box(-0.5f, -17, -0.5f, 1, 4, 1).add();
        // spiked shoulders and long clawed arms
        s(b, "shoulder", "body", false, robeLight).accent(glow).at(6, -11, 0).rot(0, 0, -15).mat("cloth").box(-1.5f, -2, -3, 5, 4, 6).add();
        s(b, "shoulder_spike", "shoulder", true, horn).at(2, -2, 0).rot(-10, 0, -35).mat("horn").box(-1, -5, -1, 2, 5, 2)
                .box(-0.5f, -7, -0.5f, 1, 2, 1).add();
        s(b, "arm", "body", false, robe).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(glow).at(8, -9, 0).rot(-20, 0, -18).mat("cloth")
                .box(-1.5f, -1, -1.5f, 3, 10, 3).add();
        s(b, "sleeve", "arm", true, robeDark).at(0, 7, 0).mat("cloth").box(-2.5f, 0, -2.5f, 5, 4, 5).box(-2, 4, 1, 4, 3, 1).add();
        s(b, "forearm", "arm", true, mask).at(0, 10, 0).rot(-35, 0, 10).mat("bone").box(-1, 0, -1, 2, 9, 2).add();
        s(b, "hand", "forearm", true, mask).at(0, 9, 0).mat("bone").box(-1.5f, 0, -1.5f, 3, 2, 3).add();
        s(b, "claw", "hand", true, horn).at(0, 2, -1).rot(-25, 0, 0).mat("horn")
                .box(-1.5f, 0, -0.5f, 1, 6, 1).box(-0.3f, 0, -0.5f, 1, 7, 1).box(0.9f, 0, -0.5f, 1, 6, 1).box(-1.5f, 5, -1.5f, 1, 1, 1)
                .box(-0.3f, 6, -1.5f, 1, 1, 1).box(0.9f, 5, -1.5f, 1, 1, 1).add();
        p(b, "aura", "body", Role.SPIN, glow).at(0, -6, 0).mat("energy").glow("full")
                .box(-10, -1, -1, 2, 2, 2).box(8, 1, 0, 2, 2, 2).box(0, -3, 8, 2, 2, 2).box(-1, 2, -10, 2, 2, 2).add();
        return b.build();
    }

    /** A Nightmare totem: a carved, spirit-haunted pillar of dark wood with screaming faces and a glowing charge orb. */
    static ModelSpec totem() {
        int wood = 0x4A3A4A;
        int dark = 0x2E222E;
        int glow = 0xA05AD8;
        ModelSpec.Builder b = ModelSpec.builder("nightmare_totem").shadow(0.7f);
        p(b, "base", null, dark).at(0, 24, 0).mat("wood").box(-6, -3, -6, 12, 3, 12).box(-5, -4, -5, 10, 1, 10).add();
        p(b, "roots", "base", dark).at(0, 0, 0).mat("wood").box(-8, -2, -1, 2, 2, 3).box(6, -2, -2, 2, 2, 3).box(-1, -2, 6, 3, 2, 2)
                .box(-2, -2, -8, 3, 2, 2).add();
        p(b, "pole", "base", wood).accent(glow).at(0, -4, 0).mat("wood").glow("runes").box(-4, -26, -4, 8, 26, 8).add();
        for (int i = 0; i < 3; i++) {
            float y = -6 - i * 8;
            p(b, "face" + i, "pole", i % 2 == 0 ? 0x5A4A5A : 0x3A2E3A).accent(glow).at(0, y, -4).mat("wood").pattern("eyes").glow("eyes")
                    .box(-3.5f, -3, -1.5f, 7, 6, 2).add();
            p(b, "brow" + i, "pole", dark).at(0, y - 3, -4.5f).rot(15, 0, 0).mat("wood").box(-4, -1, -1.5f, 8, 2, 2).add();
            s(b, "ear" + i, "pole", false, dark).at(4, y, 0).rot(0, 0, -20).mat("wood").box(0, -2, -1, 2, 4, 2).add();
        }
        p(b, "cap", "pole", dark).at(0, -26, 0).mat("wood").box(-5, -2, -5, 10, 2, 10).add();
        s(b, "cap_horn", "cap", false, 0xD8D0C0).at(4, -2, 0).rot(0, 0, -30).mat("horn").box(-0.5f, -5, -0.5f, 1, 5, 1).add();
        p(b, "orb", "cap", Role.FLOAT, glow).at(0, -7, 0).mat("energy").glow("full").box(-2.5f, -2.5f, -2.5f, 5, 5, 5).add();
        p(b, "orb_ring", "orb", Role.SPIN, 0xE0B0FF).at(0, 0, 0).mat("energy").glow("full").box(-4, -0.5f, -4, 8, 1, 1).box(-4, -0.5f, 3, 8, 1, 1)
                .box(-4, -0.5f, -3, 1, 1, 6).box(3, -0.5f, -3, 1, 1, 6).add();
        return b.build();
    }

    /** A Nightmare parasite: a bloated, segmented grub with a lamprey maw ringed in teeth. */
    static ModelSpec parasite() {
        int skin = 0x6E8A4E;
        int dark = 0x3E5A2E;
        int pale = 0xB0C080;
        ModelSpec.Builder b = ModelSpec.builder("nightmare_parasite").shadow(0.4f);
        p(b, "body", null, Role.BODY, skin).accent(pale).at(0, 20, 0).mat("flesh").box(-3, -4, -4, 6, 5, 7).add();
        p(b, "body_band", "body", dark).at(0, 0, 0).mat("flesh").box(-3.5f, -4.5f, -1, 7, 6, 2).add();
        p(b, "belly", "body", pale).at(0, 1, 0).mat("flesh").box(-2.5f, -0.5f, -3.5f, 5, 1, 6).add();
        p(b, "head", "body", Role.HEAD, skin).at(0, -1.5f, -4).mat("flesh").box(-3, -3, -3, 6, 5, 3).add();
        p(b, "maw", "head", 0x5A1A1A).at(0, -0.5f, -3).mat("flesh").box(-2, -2, -0.6f, 4, 4, 1).add();
        Kit.P ring = p(b, "teeth", "head", 0xEDE4CC).at(0, -0.5f, -3.4f).mat("horn");
        ring.box(-2, -2.5f, -0.5f, 1, 1, 1).box(1, -2.5f, -0.5f, 1, 1, 1).box(-2, 1.5f, -0.5f, 1, 1, 1).box(1, 1.5f, -0.5f, 1, 1, 1)
                .box(-2.5f, -0.5f, -0.5f, 1, 1, 1).box(1.5f, -0.5f, -0.5f, 1, 1, 1);
        ring.add();
        p(b, "jaw", "head", Role.JAW, dark).at(0, 1.5f, -2).mat("flesh").box(-2, 0, -1.5f, 4, 1, 2).add();
        eyes(b, "eye", "head", 0xF04040, 2, -2.5f, -2, 1, 1);
        String parent = "body";
        for (int i = 0; i < 3; i++) {
            String n = "seg" + i;
            int w = 5 - i;
            p(b, n, parent, Role.TAIL, i % 2 == 0 ? dark : skin).accent(pale).at(0, 0, i == 0 ? 3 : 3).rot(5, 0, 0).mat("flesh")
                    .box(-w / 2f, -w + 1, 0, w, w, 3).add();
            parent = n;
        }
        p(b, "tail_spike", parent, 0xD8D0B0).at(0, -1, 3).mat("horn").box(-0.5f, -0.5f, 0, 1, 1, 2).add();
        s(b, "leg", "body", false, dark).roles(Role.LEG_EXTRA, Role.LEG_EXTRA).at(3, 0, -1).rot(0, 0, 30).mat("flesh").box(0, -0.5f, -0.5f, 3, 1, 1).add();
        s(b, "leg2", "body", false, dark).roles(Role.LEG_EXTRA, Role.LEG_EXTRA).at(3, 0, 2).rot(0, 20, 30).mat("flesh").box(0, -0.5f, -0.5f, 3, 1, 1).add();
        p(b, "spores", "body", 0xC0F070).at(0, -4.5f, 0).mat("energy").glow("full").box(-2, -1, -2, 1, 1, 1).box(1, -1, 1, 1, 1, 1).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Zalcano
    // ------------------------------------------------------------------------------------------

    /** Zalcano: a demon of living tephra - cracked molten rock, a blazing core, curling horns and a mane of jagged stone. */
    static ModelSpec zalcano() {
        int rock = 0x5E4C4A;
        int dark = 0x342828;
        int deep = 0x463838;
        int lava = 0xFF8A24;
        BestiaryRad.Frame f = new BestiaryRad.Frame("zalcano", 1.0f, 1.5f).colors(rock, dark, deep, lava)
                .legs(7, 7, 6, 4.5f).torso(16, 14, 9, 8).arms(9, 9, 5, 6).head(8, 8, 8).hips(12, 5, 8);
        f.mat = "rock";
        f.bodyGlow = "cracks";
        f.limbGlow = "cracks";
        f.claws = 3;
        f.clawLen = 3;
        f.clawColor = 0x241818;
        f.clawMat = "rock";
        f.handW = 6;
        f.handH = 4;
        f.toes = 3;
        f.neckLen = 1;
        f.headGlow = "eyes";
        f.armSplay = 14;
        f.elbowBend = 25;
        ModelSpec.Builder b = f.build();
        p(b, "core", "torso", Role.SPIN, lava).at(0, -8, -5).mat("energy").glow("full").box(-2.5f, -2.5f, -1.5f, 5, 5, 3).add();
        p(b, "core_rim", "torso", dark).accent(lava).at(0, -8, -4.8f).mat("rock")
                .box(-4, -4, -1, 8, 1, 2).box(-4, 3, -1, 8, 1, 2).box(-4, -3, -1, 1, 6, 2).box(3, -3, -1, 1, 6, 2).add();
        s(b, "horn", "head", false, dark).accent(lava).at(3.5f, -5, -1).rot(-20, 0, -45).mat("rock").box(-1.5f, -4, -1.5f, 3, 4, 3).add();
        s(b, "horn2", "horn", true, deep).accent(lava).at(0, -4, 0).rot(-30, 0, 40).mat("rock").box(-1, -4, -1, 2, 4, 2).add();
        s(b, "horn3", "horn2", true, lava).at(0, -4, 0).rot(-30, 0, 20).mat("lava").glow("full").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        float[][] mane = {{-3, -15, 4}, {0, -5, 5}, {3, 10, 4}, {-4.5f, -30, 3}, {4.5f, 25, 3}};
        for (int i = 0; i < mane.length; i++) {
            float[] q = mane[i];
            int h = (int) q[2];
            p(b, "mane" + i, "head", i % 2 == 0 ? deep : rock).accent(lava).at(q[0], -5, 2).rot(-50, 0, q[1] * 0.6f).mat("rock").glow("cracks")
                    .box(-1.5f, -h, -1.5f, 3, h, 3).add();
        }
        spikes(b, "back_rocks", "torso", deep, "rock", -12, new float[][]{{-5, 3, 4}, {0, 4, 5}, {5, 3, 4}, {-3, 7, 3}, {3, 7, 3}}, -55);
        s(b, "pauldron", "shoulder", true, deep).accent(lava).at(2, -3.5f, 0).rot(0, 0, -20).mat("rock").glow("cracks")
                .box(-3, -2, -5, 9, 3, 10).add();
        s(b, "pauldron_spike", "pauldron", true, dark).at(3, -2, 0).rot(0, 0, -30).mat("rock").box(-1, -4, -1, 2, 4, 2).add();
        s(b, "fist_glow", "hand", true, lava).at(0, 2, -3.2f).mat("lava").glow("full").box(-2, -1, -0.5f, 4, 2, 1).add();
        teeth(b, "teeth", "head", 0xFFC060, -2.5f, 2.5f, 1.6f, -6.6f, 4, 1);
        p(b, "skirt", "hips", deep).accent(lava).at(0, 0, 0).mat("rock").glow("cracks").box(-6.5f, -1, -4.5f, 13, 5, 9).add();
        p(b, "embers", "hips", Role.SPIN, lava).at(0, 14, 0).mat("lava").glow("full").box(-9, 0, -4, 2, 1, 2).box(7, 0, 3, 2, 1, 2)
                .box(-2, 0, 8, 2, 1, 2).box(3, 0, -9, 2, 1, 2).add();
        return b.build();
    }

    /** A Zalcano golem: a squat, lumbering lump of cooling tephra that marches to heal its mistress. */
    static ModelSpec golem() {
        int rock = 0x6A5A56;
        int dark = 0x3A2E2C;
        int deep = 0x504240;
        int lava = 0xFF9A30;
        BestiaryRad.Frame f = new BestiaryRad.Frame("zalcano_golem", 1.0f, 0.8f).colors(rock, dark, deep, lava)
                .legs(4, 4, 5, 4).torso(14, 10, 10, 15).arms(6, 7, 5, 6).head(7, 5, 6).hips(11, 4, 8);
        f.mat = "rock";
        f.bodyGlow = "cracks";
        f.claws = 0;
        f.toes = 2;
        f.clawColor = dark;
        f.clawMat = "rock";
        f.handW = 7;
        f.handH = 5;
        f.neckLen = 0;
        f.jaw = false;
        f.headGlow = "eyes";
        f.armSplay = 16;
        ModelSpec.Builder b = f.build();
        p(b, "core", "torso", lava).at(0, -6, -5).mat("energy").glow("full").box(-2, -2, -0.8f, 4, 4, 1).add();
        for (int i = 0; i < 4; i++) {
            float x = -4.5f + i * 3;
            p(b, "boulder" + i, "torso", i % 2 == 0 ? deep : rock).accent(lava).at(x, -10, 1 + (i % 2)).rot(-20, i * 25, (i - 1.5f) * 12)
                    .mat("rock").box(-2, -3, -2, 4, 3, 4).add();
        }
        s(b, "fist_rock", "hand", true, dark).accent(lava).at(0, 0, 0).mat("rock").glow("cracks").box(-4, -1, -4, 8, 3, 8).add();
        s(b, "shoulder_rock", "shoulder", true, deep).at(2, -3, 0).rot(10, 0, -15).mat("rock").box(-2, -2, -3, 5, 3, 6).add();
        p(b, "brow_rock", "head", dark).at(0, -3, -4).mat("rock").box(-4, -2, -1, 8, 2, 3).add();
        return b.build();
    }
}
