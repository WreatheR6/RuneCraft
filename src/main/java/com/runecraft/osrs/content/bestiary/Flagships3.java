package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/**
 * Flagship boss re-sculpts, part 3 (round 12). Same rules as {@link Flagships} and {@link Flagships2}: each spec here
 * was rendered before/after against the version it replaces and kept only where it reads clearly closer to OSRS.
 * Registered last in {@link Overrides}.
 */
final class Flagships3 {
    private Flagships3() {
    }

    static List<ModelSpec> overrides() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(phantomMuspah());
        l.add(whisperer());
        return l;
    }

    private static final int TOOTH = 0xF0EADA;

    /**
     * Phantom Muspah: the spectral brute of the Ghorrock dungeon, hunched forward on enormous knuckle-walking arms,
     * a dark slate-blue body cracked with icy light, a white skull-like head thrust out low between massive
     * shoulders, ice tusks, and a ridge of glowing ice shards bursting from its back.
     */
    static ModelSpec phantomMuspah() {
        int body = 0x3E4C6E;
        int deep = 0x262F4A;
        int ice = 0xA8D8F8;
        int spirit = 0xC8ECFF;
        int bone = 0xDCE2EA;
        ModelSpec.Builder b = ModelSpec.builder("phantom_muspah").shadow(1.8f);
        part(b, "hips", null, Role.BODY, deep).accent(spirit).at(0, 11, 6).mat("ghost").box(-6, -3, -4, 12, 6, 8).add();
        part(b, "torso", "hips", Role.STATIC, body).accent(spirit).at(0, -2, 0).rot(58, 0, 0).mat("ghost").glow("cracks")
                .box(-8, -12, -6, 16, 12, 12).box(-10, -18, -7, 20, 7, 14).add();
        part(b, "chest", "torso", Role.STATIC, bone).accent(deep).at(0, -10, -6).mat("bone").box(-6, -6, -0.8f, 12, 7, 1)
                .box(-4, 1.5f, -0.8f, 8, 1, 1).box(-3, 3.5f, -0.8f, 6, 1, 1).add();
        // ridge of ice shards bursting from the back
        int[][] sh = {{0, -16, 9}, {-5, -14, 7}, {5, -14, 7}, {0, -10, 7}, {-4, -6, 5}, {4, -6, 5}, {0, -2, 4}};
        for (int i = 0; i < sh.length; i++) {
            part(b, "shard" + i, "torso", Role.STATIC, ice).accent(spirit).at(sh[i][0], sh[i][1], 6).rot(-20 + (i % 3) * 8, 0, sh[i][0] * -3)
                    .mat("ice").glow("full").box(-1.5f, -1.5f, 0, 3, 3, sh[i][2]).box(-0.5f, -0.5f, sh[i][2], 1, 1, 2).add();
        }
        // skull head thrust forward between the shoulders
        part(b, "head", "torso", Role.HEAD, bone).accent(spirit).at(0, -16, -6).rot(-62, 0, 0).mat("bone").box(-5, -6, -8, 10, 8, 9).add();
        part(b, "brow", "head", Role.STATIC, bone).at(0, -4.5f, -8).rot(15, 0, 0).mat("bone").box(-5.5f, -1.5f, -1, 11, 3, 2).add();
        part(b, "crest", "head", Role.STATIC, ice).accent(spirit).at(0, -6, -3).rot(-40, 0, 0).mat("ice").glow("full")
                .box(-1, -6, -1, 2, 6, 3).box(-0.5f, -9, 0, 1, 3, 1).add();
        part(b, "sockets", "head", Role.STATIC, 0x141A2A).at(0, -2.5f, -8.1f).mat("rock").box(1, -1, -0.3f, 3, 2, 1).box(-4, -1, -0.3f, 3, 2, 1).add();
        b.part("eyes", "head", Role.STATIC, spirit).pivot(0, -2.5f, -8.5f).material("energy").glow("full")
                .box(1.5f, -0.5f, -0.4f, 2, 1, 1).box(-3.5f, -0.5f, -0.4f, 2, 1, 1);
        part(b, "jaw", "head", Role.JAW, bone).accent(deep).at(0, 1.5f, -5).rot(12, 0, 0).mat("bone").box(-5, 0, -4, 10, 3, 6).add();
        BestiaryBst.teeth(b, "teeth", "jaw", TOOTH, 0, 0, -3.6f, 8, 5, 1, true);
        sym(b, "tusk", "jaw", false, ice).at(4.5f, 0.5f, -3.5f).rot(-35, 0, 20).mat("ice").glow("full").box(-1, -5, -1, 2, 5, 2)
                .box(-0.5f, -7, -0.5f, 1, 2, 1).add();
        sym(b, "horn", "head", false, ice).at(4.5f, -5, -1).rot(-60, 0, -35).mat("ice").glow("full").box(-1, -7, -1, 2, 7, 2)
                .box(-0.5f, -9, -0.5f, 1, 2, 1).add();
        // enormous shoulders and knuckle-walking arms planted ahead of the body
        sym(b, "shoulder", "torso", false, deep).accent(spirit).at(10, -15, 0).rot(0, 0, -12).mat("ghost").box(-3, -4, -6, 8, 8, 12).add();
        sym(b, "shoulder_shard", "shoulder", true, ice).accent(spirit).at(2, -4, 1).rot(-10, 0, 30).mat("ice").glow("full")
                .box(-1.5f, -6, -1.5f, 3, 6, 3).box(-0.5f, -8, -0.5f, 1, 2, 1).add();
        sym(b, "arm", "torso", false, body).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(spirit).at(11, -13, -1).rot(-62, 0, -12)
                .mat("ghost").glow("cracks").box(-3, -1, -3, 7, 10, 7).add();
        sym(b, "fore", "arm", true, deep).accent(spirit).at(0.5f, 9, 0).rot(4, 0, 10).mat("ghost").box(-4, 0, -4, 8, 9, 8).add();
        sym(b, "fist", "fore", true, bone).at(0, 8.5f, -0.5f).mat("bone").box(-4.5f, 0, -4.5f, 9, 5, 9).add();
        sym(b, "knuckles", "fist", true, ice).at(0, 3, -4.5f).mat("ice").glow("full").box(-4, -1, -0.6f, 2, 2, 1)
                .box(-1, -1, -0.6f, 2, 2, 1).box(2, -1, -0.6f, 2, 2, 1).add();
        // short bent hind legs
        sym(b, "thigh", "hips", false, body).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(spirit).at(5, 1, 0).rot(-35, 0, -6)
                .mat("ghost").box(-3, -1, -3, 6, 8, 6).add();
        sym(b, "shin", "thigh", true, deep).at(0, 7, 0).rot(50, 0, 6).mat("ghost").box(-2.5f, 0, -2.5f, 5, 6, 5).add();
        sym(b, "foot", "shin", true, bone).at(0, 5.5f, 0).rot(-15, 0, 0).mat("bone").box(-3, 0, -5, 6, 2, 7).add();
        part(b, "aura", "torso", Role.SPIN, spirit).at(0, -8, 0).mat("ice").glow("full")
                .box(-15, 0, -1, 2, 2, 2).box(13, -3, 0, 2, 2, 2).box(0, 2, 13, 2, 2, 2).box(-1, -6, -15, 2, 2, 2).add();
        return b.build();
    }

    /**
     * The Whisperer: the drowned eldritch horror of the Lassar Undercity, floating upright like a monstrous
     * cephalopod: a tall, tapered mantle-head swept back over a face of hanging tendrils, a narrow ribbed body,
     * long thin clawed arms, and a skirt of long tentacles, all deep navy flesh lit by cyan veins and glowing tips.
     */
    static ModelSpec whisperer() {
        int body = 0x22324C;
        int deep = 0x141E34;
        int glow = 0x50E8E0;
        int pale = 0x5A7A9A;
        ModelSpec.Builder b = ModelSpec.builder("the_whisperer").shadow(1.6f);
        part(b, "body", null, Role.FLOAT, body).accent(glow).at(0, 4, 0).mat("flesh").glow("veins").box(-6, -10, -4, 12, 12, 8)
                .box(-5, 2, -3, 10, 2, 6).add();
        for (int i = 0; i < 4; i++) {
            part(b, "rib" + i, "body", Role.STATIC, pale).accent(glow).at(0, -8 + i * 3, -4).mat("bone")
                    .box(-5 + i * 0.5f, -0.5f, -0.8f, (int) (10 - i), 1, 1).add();
        }
        sym(b, "shoulder", "body", false, deep).accent(glow).at(6, -9, 0).rot(0, 0, -20).mat("flesh").box(-2, -2, -3, 5, 4, 6).add();
        // head: a face of hanging tendrils under a tall mantle swept up and back
        part(b, "head", "body", Role.HEAD, body).accent(glow).at(0, -10, -1).rot(-5, 0, 0).mat("flesh").glow("veins").pattern("blank")
                .box(-6, -7, -7, 12, 8, 10).add();
        part(b, "mantle", "head", Role.STATIC, body).accent(glow).at(0, -6, 0).rot(-28, 0, 0).mat("flesh").glow("veins")
                .box(-7, -7, -6, 14, 7, 12).box(-6, -13, -5, 12, 6, 10).box(-4.5f, -18, -4, 9, 5, 8).box(-3, -22, -2.5f, 6, 4, 5)
                .box(-1.5f, -25, -1.5f, 3, 3, 3).add();
        part(b, "mantle_tip", "mantle", Role.FIN, deep).accent(glow).at(0, -25, 0).rot(-25, 0, 0).mat("flesh").box(-1, -4, -1, 2, 4, 2).add();
        sym(b, "mantle_fin", "mantle", false, deep).roles(Role.FIN, Role.FIN).accent(glow).at(6.5f, -9, 1).rot(0, 0, -30).mat("membrane")
                .box(0, -8, -0.5f, 4, 12, 1).add();
        b.part("eyes", "head", Role.STATIC, glow).pivot(0, -4.5f, -7.1f).material("energy").glow("full")
                .box(1.5f, -0.5f, -0.4f, 2, 1, 1).box(-3.5f, -0.5f, -0.4f, 2, 1, 1).box(3.5f, -2, -0.2f, 1, 1, 1).box(-4.5f, -2, -0.2f, 1, 1, 1);
        part(b, "maw", "head", Role.JAW, 0x0A1020).accent(glow).at(0, -0.5f, -7).mat("flesh").box(-4, -1, -0.6f, 8, 2, 1).add();
        for (int i = 0; i < 6; i++) {
            part(b, "face_tendril" + i, "head", Role.TENTACLE, i % 2 == 0 ? pale : body).accent(glow).at(-4.5f + i * 1.8f, 1, -6.5f)
                    .rot(8, 0, (i - 2.5f) * 6).mat("flesh").box(-0.5f, 0, -0.5f, 1, 7 + (i % 3) * 3, 1).add();
        }
        // long thin clawed arms
        sym(b, "arm", "body", false, body).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(glow).at(7, -8, 0).rot(-20, 0, -25).mat("flesh")
                .box(-1, -1, -1, 2, 10, 2).add();
        sym(b, "fore", "arm", true, pale).at(0, 10, 0).rot(-50, 0, 10).mat("flesh").box(-1, 0, -1, 2, 9, 2).add();
        sym(b, "claw", "fore", true, glow).at(0, 9, 0).mat("energy").glow("full").box(-1.5f, 0, -1, 1, 3, 1).box(0.5f, 0, -1, 1, 3, 1)
                .box(-0.5f, 0, 0.5f, 1, 2, 1).add();
        // skirt of long tentacles
        for (int i = 0; i < 6; i++) {
            float yaw = i * 60 + 30;
            float x = (float) Math.sin(Math.toRadians(yaw)) * 4;
            float z = (float) Math.cos(Math.toRadians(yaw)) * 3;
            String prev = "body";
            int[] w = {3, 2, 2, 1};
            for (int k = 0; k < w.length; k++) {
                String n = "tentacle" + i + "_" + k;
                part(b, n, prev, Role.TENTACLE, k == 3 ? glow : (k % 2 == 0 ? body : deep)).accent(glow).at(k == 0 ? x : 0, k == 0 ? 3 : 5, k == 0 ? z : 0)
                        .rot(k == 0 ? 22 : 12, k == 0 ? yaw : 0, 0).mat(k == 3 ? "energy" : "flesh").glow(k == 3 ? "full" : null)
                        .box(-w[k] / 2f, 0, -w[k] / 2f, w[k], 5, w[k]).add();
                prev = n;
            }
        }
        return b.build();
    }
}
