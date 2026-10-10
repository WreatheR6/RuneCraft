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

/**
 * Minigame creatures: the Wintertodt, Tempoross, the Great Guardian of the Rift, Barbarian Assault's Penance (with
 * their Queen) and Pest Control's portals and pests.
 */
final class BestiaryRadGames {
    private BestiaryRadGames() {
    }

    static final int TOOTH = 0xEDE4CC;

    static List<ModelSpec> all() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(wintertodt());
        l.add(tempoross());
        l.add(greatGuardian());
        l.add(penanceQueen());
        l.add(penance("penance_fighter", 1.0f, 0x6E5E3A, "claws"));
        l.add(penance("penance_ranger", 1.0f, 0x5E6E3A, "sac"));
        l.add(penance("penance_runner", 0.85f, 0x5E5E6E, "tail"));
        l.add(penance("penance_healer", 0.9f, 0x6E5A5A, "healer"));
        l.add(pestPortal());
        l.add(brawler());
        l.add(defiler());
        l.add(ravager());
        l.add(shifter());
        l.add(spinner());
        l.add(splatter());
        l.add(torcher());
        return l;
    }

    // ------------------------------------------------------------------------------------------
    // Wintertodt
    // ------------------------------------------------------------------------------------------

    /**
     * The Wintertodt: an ancient storm of living ice rising from its frozen prison - a body of jagged shards around
     * a blinding cold core, a crown of icicles, long grasping arms of frost and a churning ring of blizzard.
     */
    static ModelSpec wintertodt() {
        int ice = 0xA8D0F4;
        int deep = 0x5A90D0;
        int dark = 0x3A6AA8;
        int white = 0xE8F6FF;
        int core = 0xF0FAFF;
        ModelSpec.Builder b = ModelSpec.builder("wintertodt").scale(2.5f).shadow(1.6f);
        p(b, "prison", null, deep).at(0, 24, 0).mat("ice").box(-11, -3, -11, 22, 3, 22).box(-9, -5, -9, 18, 2, 18).add();
        Kit.P shards = p(b, "prison_shards", "prison", ice).at(0, -3, 0).mat("ice");
        float[][] ps = {{-10, -9, 6}, {9, -8, 5}, {-9, 9, 4}, {10, 8, 6}, {0, -11, 3}, {-11, 1, 4}, {11, -1, 3}};
        for (float[] q : ps) {
            shards.box(q[0] - 1.5f, -q[2], q[1] - 1.5f, 3, (int) q[2], 3);
        }
        shards.add();
        p(b, "blizzard", "prison", Role.SPIN, white).at(0, -6, 0).mat("energy").glow("full")
                .box(-13, -1, -2, 3, 1, 4).box(10, -1, -2, 3, 1, 4).box(-2, -1, -13, 4, 1, 3).box(-2, -1, 10, 4, 1, 3)
                .box(-9, -2, -9, 2, 1, 2).box(7, -2, 7, 2, 1, 2).add();
        // body of shards
        p(b, "body", null, Role.FLOAT, ice).accent(white).at(0, 7, 0).mat("ice").box(-6, -12, -4, 12, 16, 8).add();
        p(b, "body_lower", "body", deep).at(0, 4, 0).mat("ice").box(-4, 0, -3, 8, 6, 6).box(-2, 6, -2, 4, 4, 4).add();
        p(b, "chest_l", "body", white).at(3, -10, -4).rot(-10, 15, 0).mat("ice").box(-3, 0, -1.5f, 5, 7, 2).add();
        p(b, "chest_r", "body", white).at(-3, -10, -4).rot(-10, -15, 0).mat("ice").box(-2, 0, -1.5f, 5, 7, 2).add();
        p(b, "core", "body", Role.SPIN, core).accent(0x80D0FF).at(0, -5, -4.5f).mat("energy").glow("full").box(-2.5f, -2.5f, -1.5f, 5, 5, 3).add();
        p(b, "core_glow", "core", 0x80D0FF).at(0, 0, 0).mat("energy").glow("full").box(-1, -4, -1, 2, 8, 2).box(-4, -1, -1, 8, 2, 2).add();
        float[][] bodyShards = {{-6, -8, 2, 6, -30, 30}, {6, -9, 1, 6, -25, -30}, {-4, -1, 3, 5, 40, 40}, {5, 0, 3, 5, 40, -35},
            {0, -11, 4, 6, -40, 0}, {-6, 2, -1, 4, 20, 60}, {6, 2, -1, 4, 20, -60}};
        for (int i = 0; i < bodyShards.length; i++) {
            float[] q = bodyShards[i];
            int h = (int) q[3];
            p(b, "shard" + i, "body", i % 2 == 0 ? white : ice).at(q[0], q[1], q[2]).rot(q[4], 0, q[5]).mat("ice")
                    .glow(i % 3 == 0 ? "full" : null).box(-1.5f, -h, -1.5f, 3, h, 3).box(-0.5f, -h - 2, -0.5f, 1, 2, 1).add();
        }
        // head: a frozen skull-mask crowned with icicles
        p(b, "head", "body", Role.HEAD, white).accent(0x60C0FF).at(0, -12, 0).mat("ice").pattern("eyes").glow("eyes")
                .box(-5, -8, -5, 10, 8, 9).add();
        p(b, "brow", "head", deep).at(0, -5.5f, -5).rot(12, 0, 0).mat("ice").box(-5.5f, -1.5f, -1.5f, 11, 2, 2).add();
        p(b, "jaw", "head", Role.JAW, deep).accent(0x80D0FF).at(0, -1, -3).rot(8, 0, 0).mat("ice").glow("maw").box(-4, 0, -2.5f, 8, 3, 6).add();
        teeth(b, "fangs", "head", white, -3.5f, 3.5f, -0.6f, -5.4f, 5, 2);
        float[][] crown = {{0, 0, 12}, {3, -20, 10}, {6, -45, 8}, {8, -70, 6}};
        for (int i = 0; i < crown.length; i++) {
            float[] q = crown[i];
            int h = (int) q[2];
            if (i == 0) {
                p(b, "icicle0", "head", white).at(0, -8, 0).rot(-10, 0, 0).mat("ice").glow("full").box(-1.5f, -h, -1.5f, 3, h, 3)
                        .box(-0.5f, -h - 3, -0.5f, 1, 3, 1).add();
            } else {
                s(b, "icicle" + i, "head", false, i % 2 == 0 ? white : ice).at(q[0] * 0.55f, -8, 0).rot(-10, 0, q[1]).mat("ice")
                        .box(-1, -h, -1, 2, h, 2).box(-0.5f, -h - 2, -0.5f, 1, 2, 1).add();
            }
        }
        // long grasping arms of frost
        s(b, "shoulder", "body", false, deep).at(6, -11, 0).rot(0, 0, -20).mat("ice").box(-1, -3, -3, 6, 5, 6).add();
        s(b, "arm", "body", false, ice).roles(Role.TENTACLE, Role.TENTACLE).at(8, -9, 0).rot(-10, 0, -25).mat("ice")
                .box(-2, -1, -2, 4, 11, 4).add();
        s(b, "forearm", "arm", true, deep).at(0, 10, 0).rot(-35, 0, 15).mat("ice").box(-1.5f, 0, -1.5f, 3, 10, 3).add();
        s(b, "hand", "forearm", true, white).at(0, 10, 0).mat("ice").box(-2, 0, -2, 4, 2, 4).add();
        s(b, "claw", "hand", true, white).at(0, 2, -1).rot(-20, 0, 0).mat("ice").glow("full")
                .box(-2, 0, -1, 1, 5, 1).box(-0.5f, 0, -1, 1, 6, 1).box(1, 0, -1, 1, 5, 1).add();
        s(b, "arm_spike", "forearm", true, white).at(1.5f, 3, 1).rot(30, 0, -40).mat("ice").box(-0.5f, -5, -0.5f, 1, 5, 1).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Tempoross
    // ------------------------------------------------------------------------------------------

    /**
     * Tempoross: the storm-spirit of the sea, surging from a whirlpool - a vast, barnacled fish-beast head with a
     * jagged maw, glowing eyes, finned crest and coral horns, flailing tentacles, and a crackling stormcloud above.
     */
    static ModelSpec tempoross() {
        int sea = 0x3A6E9E;
        int deep = 0x24486E;
        int dark = 0x16304A;
        int belly = 0x8AB0C8;
        int foam = 0xE0F0FF;
        int storm = 0x5A6478;
        ModelSpec.Builder b = ModelSpec.builder("tempoross").scale(2.5f).shadow(1.7f);
        p(b, "whirlpool", null, Role.SPIN, deep).accent(foam).at(0, 24, 0).mat("slime").box(-14, -1, -14, 28, 1, 28).add();
        p(b, "foam", "whirlpool", foam).at(0, -1, 0).mat("energy").glow("full")
                .box(-13, -1, -3, 4, 1, 6).box(9, -1, -3, 4, 1, 6).box(-3, -1, -13, 6, 1, 4).box(-3, -1, 9, 6, 1, 4)
                .box(-6, -1, -6, 3, 1, 3).box(4, -1, 4, 3, 1, 3).add();
        p(b, "wave", "whirlpool", sea).at(0, -1, 0).mat("slime").box(-10, -3, -10, 20, 3, 20).add();
        // a vast fish-beast surging up out of the water, leaning back with its maw agape
        p(b, "body", null, Role.FLOAT, sea).accent(belly).at(0, 15, 3).rot(-18, 0, 0).mat("scales").box(-9, -14, -7, 18, 17, 14).add();
        p(b, "belly", "body", belly).at(0, -6, -7).mat("plates").box(-7, -7, -1.5f, 14, 15, 2).add();
        p(b, "flank", "body", deep).at(0, -4, 0).mat("scales").box(-10, -6, -5, 20, 8, 10).add();
        spikes(b, "back_spines", "body", foam, "horn", -13, new float[][]{{0, 3, 4}, {0, 6, 3}, {-4, 4, 3}, {4, 4, 3}}, 25);
        p(b, "dorsal", "body", deep).at(0, -10, 6).rot(30, 0, 0).mat("membrane").box(-0.5f, -9, -4, 1, 9, 8).add();
        p(b, "dorsal_rays", "dorsal", foam).at(0, 0, 0).mat("horn").box(-1, -10, -3, 2, 10, 1).box(-1, -8, 0, 2, 8, 1).box(-1, -6, 3, 2, 6, 1).add();
        // the great head: skull with brows and coral horns, gaping jaw ringed with teeth
        p(b, "head", "body", Role.HEAD, sea).accent(0xFFE070).at(0, -14, -1).rot(28, 0, 0).mat("scales").box(-8, -7, -12, 16, 7, 13).add();
        p(b, "brow", "head", deep).at(0, -6, -12).rot(15, 0, 0).mat("scales").box(-8.5f, -2, -2, 17, 3, 4).add();
        s(b, "eye", "head", false, 0xFFE070).at(8, -4, -8).mat("energy").glow("full").box(-0.6f, -1.5f, -1.5f, 1, 3, 3).add();
        s(b, "eye_ridge", "head", false, dark).at(8, -6, -8).rot(0, 0, -20).mat("scales").box(-1, -1, -2.5f, 2, 2, 5).add();
        teeth(b, "teeth_top", "head", TOOTH, -7, 7, 0, -11.3f, 9, 2);
        s(b, "teeth_side", "head", false, TOOTH).at(7.3f, 0, 0).mat("horn").box(-0.5f, 0, -10, 1, 2, 1).box(-0.5f, 0, -7, 1, 2, 1)
                .box(-0.5f, 0, -4, 1, 2, 1).add();
        p(b, "gullet", "head", 0x5A1A2A).at(0, 0, -6).mat("flesh").glow("cracks").accent(0xFF6040).box(-6, -1, -5, 12, 2, 10).add();
        p(b, "jaw", "head", Role.JAW, belly).accent(0xFFE070).at(0, 0, 0).rot(32, 0, 0).mat("plates").box(-7.5f, 0, -12, 15, 4, 12).add();
        teeth(b, "teeth_low", "jaw", TOOTH, -6.5f, 6.5f, 0, -11.3f, 8, -2);
        p(b, "chin_barbels", "jaw", deep).at(0, 4, -10).mat("flesh").box(-4, 0, -1, 1, 4, 1).box(3, 0, -1, 1, 4, 1).box(-0.5f, 0, -2, 1, 5, 1).add();
        s(b, "coral_horn", "head", false, 0xE06A50).at(5, -7, -4).rot(-30, 0, -25).mat("bone").box(-1, -7, -1, 2, 7, 2)
                .box(-3, -5, -0.5f, 2, 1, 1).box(1, -8, -0.5f, 1, 2, 1).add();
        Kit.P barn = p(b, "barnacles", "head", 0xC8C0B0).at(0, 0, 0).mat("rock");
        barn.box(5, -8, -9, 2, 1, 2).box(-7, -8, -5, 2, 1, 2).box(2, -8, -1, 2, 1, 2).box(-4, -8, -10, 2, 1, 2).box(7.5f, -4, -2, 1, 2, 2);
        barn.add();
        // pectoral fins sweeping out like arms
        s(b, "fin", "body", false, deep).roles(Role.TENTACLE, Role.TENTACLE).accent(belly).at(9, -7, -2).rot(10, -20, -35).mat("membrane")
                .box(0, -1, -4, 12, 2, 9).add();
        s(b, "fin_rays", "fin", true, foam).at(0, -1.2f, 0).mat("horn").box(0, -0.5f, -3, 12, 1, 1).box(0, -0.5f, 0, 11, 1, 1)
                .box(0, -0.5f, 3, 9, 1, 1).add();
        s(b, "fin_tip", "fin", true, dark).at(12, 0, 0).rot(0, 0, 20).mat("membrane").box(0, -0.5f, -3, 5, 1, 6).add();
        // tail fluke rising from the waves behind
        p(b, "tail", "whirlpool", Role.TAIL, sea).accent(belly).at(0, -3, 13).rot(-35, 0, 0).mat("scales").box(-3, -8, -2, 6, 8, 4).add();
        s(b, "fluke", "tail", false, deep).at(1, -8, 0).rot(0, 0, -40).mat("membrane").box(0, -2, -1, 8, 4, 2).box(6, -3, -1, 3, 2, 2).add();
        // stormcloud and lightning
        p(b, "cloud", "whirlpool", Role.SPIN, storm).at(0, -42, 0).mat("ghost").box(-11, -3, -7, 22, 4, 14).box(-7, -5, -5, 14, 2, 10).add();
        p(b, "bolts", "cloud", 0xFFF4A0).at(0, 1, 0).mat("energy").glow("full").box(-8, 0, -2, 1, 4, 1).box(-7, 3, -2, 1, 3, 1)
                .box(7, 0, 2, 1, 3, 1).box(6, 2, 2, 1, 4, 1).box(1, 0, 5, 1, 5, 1).add();
        p(b, "spray", "whirlpool", foam).at(0, -2, 0).mat("energy").glow("full").box(-9, -2, -7, 3, 2, 3).box(6, -3, 5, 3, 3, 3)
                .box(-8, -2, 6, 2, 2, 2).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // The Great Guardian
    // ------------------------------------------------------------------------------------------

    /** The Great Guardian of the Rift: an ancient carved-stone colossus pulsing with rune energy, orbited by rune stones. */
    static ModelSpec greatGuardian() {
        int stone = 0x6A6A86;
        int dark = 0x3E3E56;
        int deep = 0x52526C;
        int rune = 0xC090FF;
        BestiaryRad.Frame f = new BestiaryRad.Frame("great_guardian", 1.8f, 1.1f).colors(stone, dark, deep, rune)
                .legs(5, 5, 5, 3.5f).torso(13, 11, 8, 6).arms(6, 7, 4, 5).head(7, 7, 7).hips(10, 4, 7);
        f.mat = "plates";
        f.bodyGlow = "runes";
        f.claws = 0;
        f.toes = 0;
        f.handW = 5;
        f.handH = 4;
        f.neckLen = 1;
        f.jaw = false;
        f.headGlow = "eyes";
        f.armSplay = 12;
        f.elbowBend = 30;
        ModelSpec.Builder b = f.build();
        p(b, "pedestal", null, 0x3A3A4A).at(0, 24, 0).mat("rock").box(-8, -2, -8, 16, 2, 16).box(-7, -3, -7, 14, 1, 14).add();
        p(b, "pedestal_runes", "pedestal", rune).at(0, -3.1f, 0).mat("energy").glow("full").box(-6, -0.5f, -6, 3, 1, 1).box(3, -0.5f, 5, 3, 1, 1)
                .box(5, -0.5f, -5, 1, 1, 3).box(-6, -0.5f, 2, 1, 1, 3).add();
        p(b, "eye", "head", 0xFFE0FF).at(0, -3, -5.2f).mat("energy").glow("full").box(-1.5f, -1.5f, -0.5f, 3, 3, 1).add();
        p(b, "hood", "head", dark).at(0, -5, 0).mat("plates").box(-4, -1, -5.5f, 8, 2, 8).box(-4.5f, 0, -1, 1, 6, 5).box(3.5f, 0, -1, 1, 6, 5).add();
        p(b, "crest", "head", deep).at(0, -6, 0).rot(-15, 0, 0).mat("plates").box(-1, -4, -3, 2, 4, 7).add();
        s(b, "pauldron", "shoulder", true, deep).accent(rune).at(1.5f, -3, 0).rot(0, 0, -15).mat("plates").glow("runes").box(-2, -2, -4, 7, 3, 8).add();
        p(b, "chest_rune", "torso", rune).at(0, -9, -4.2f).mat("energy").glow("full").box(-2, 0, -0.5f, 4, 1, 1).box(-0.5f, 0, -0.5f, 1, 5, 1)
                .box(-2, 4, -0.5f, 4, 1, 1).add();
        p(b, "tabard", "hips", 0x4A3A80).accent(rune).at(0, 0, -3.6f).mat("cloth").box(-2.5f, -1, -0.5f, 5, 8, 1).add();
        p(b, "ring", "torso", Role.SPIN, rune).at(0, -4, 0).mat("energy").glow("full").box(-11, -0.5f, -11, 22, 1, 1).box(-11, -0.5f, 10, 22, 1, 1)
                .box(-11, -0.5f, -10, 1, 1, 20).box(10, -0.5f, -10, 1, 1, 20).add();
        int[] runeColors = {0xFF6040, 0x60A0FF, 0x80E060, 0xE0E0E0, 0xFFE060, 0xD060FF};
        for (int i = 0; i < runeColors.length; i++) {
            float a = (float) (i * Math.PI * 2 / runeColors.length);
            p(b, "rune" + i, "ring", runeColors[i]).at((float) Math.cos(a) * 10.5f, -2, (float) Math.sin(a) * 10.5f).rot(0, (float) Math.toDegrees(a), 0)
                    .mat("energy").glow("full").box(-1, -1.5f, -1, 2, 3, 2).add();
        }
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Barbarian Assault: the Penance
    // ------------------------------------------------------------------------------------------

    /**
     * A Penance creature: a hunched, chitin-plated insectoid biped with a big mandibled head, antennae and a ridged
     * back carapace. Variants: fighters with sickle claws, rangers with a spit sac, runners with a long tail and
     * lean legs, healers with a glowing sac.
     */
    static ModelSpec penance(String id, float scale, int shell, String variant) {
        int dark = shade(shell, 0.62f);
        int deep = shade(shell, 0.8f);
        boolean runner = variant.equals("tail");
        BestiaryRad.Frame f = new BestiaryRad.Frame(id, scale, 0.7f * scale).colors(shell, dark, deep, 0xE0C070)
                .legs(runner ? 7 : 5, runner ? 7 : 5, 4, 3).torso(11, 10, 8, 35).arms(6, 7, 3, 4).head(8, 6, 7).hips(9, 4, 7);
        f.mat = "chitin";
        f.claws = 3;
        f.clawLen = variant.equals("claws") ? 4 : 2;
        f.clawColor = 0xD8CCA8;
        f.toes = 3;
        f.neckLen = 1;
        f.jaw = false;
        f.headGlow = "eyes";
        f.armSplay = 14;
        f.elbowBend = 30;
        ModelSpec.Builder b = f.build();
        p(b, "carapace", "torso", deep).at(0, -10, 3).rot(-10, 0, 0).mat("chitin").box(-6, -1, -1, 12, 8, 3).add();
        p(b, "carapace2", "carapace", shell).at(0, 6, 1).rot(12, 0, 0).mat("chitin").box(-5, 0, -1, 10, 5, 3).add();
        spikes(b, "ridge", "carapace", dark, "chitin", -1, new float[][]{{-3, 1, 2}, {0, 1.5f, 3}, {3, 1, 2}}, -20);
        s(b, "mandible", "head", false, 0xD8CCA8).roles(Role.JAW, Role.JAW).at(2, 2, -5).rot(10, 25, 0).mat("horn")
                .box(-0.5f, 0, -3, 1, 2, 3).box(-1.5f, 1, -3.5f, 2, 1, 1).add();
        s(b, "antenna", "head", false, dark).at(2, -3.5f, -4).rot(-40, 0, -25).mat("chitin").box(-0.5f, -5, -0.5f, 1, 5, 1).add();
        s(b, "antenna_tip", "antenna", true, dark).at(0, -5, 0).rot(-40, 0, 0).mat("chitin").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        p(b, "head_plate", "head", deep).at(0, -3.5f, -1).rot(-10, 0, 0).mat("chitin").box(-4.5f, -1, -4, 9, 2, 7).add();
        switch (variant) {
            case "claws" -> s(b, "sickle", "forearm", true, 0xD8CCA8).at(0, 4, -2).rot(-60, 0, 0).mat("horn")
                    .box(-0.5f, -6, -0.5f, 1, 6, 1).box(-0.5f, -7, 0.5f, 1, 2, 2).add();
            case "sac" -> {
                p(b, "sac", "carapace2", 0x9ACA50).at(0, 3, 2).mat("slime").glow("full").box(-3, -2, 0, 6, 5, 3).add();
                p(b, "spit", "head", 0x9ACA50).at(0, 2, -6).mat("slime").glow("full").box(-1, -1, -1, 2, 2, 2).add();
            }
            case "tail" -> {
                p(b, "tail", "hips", Role.TAIL, shell).at(0, -1, 3).rot(25, 0, 0).mat("chitin").box(-1.5f, -1.5f, 0, 3, 3, 6).add();
                p(b, "tail2", "tail", Role.TAIL, deep).at(0, 0, 6).rot(10, 0, 0).mat("chitin").box(-1, -1, 0, 2, 2, 6).add();
                p(b, "tail3", "tail2", Role.TAIL, dark).at(0, 0, 6).mat("chitin").box(-0.5f, -0.5f, 0, 1, 1, 4).box(-1, -1, 3, 2, 2, 2).add();
            }
            default -> {
                p(b, "heal_sac", "carapace2", 0xF08A8A).at(0, 3, 2).mat("slime").glow("full").box(-3, -2, 0, 6, 5, 3).add();
                p(b, "cross", "heal_sac", 0xFFE0E0).at(0, 0, 3).mat("energy").glow("full").box(-0.5f, -1.5f, 0, 1, 3, 1).box(-1.5f, -0.5f, 0, 3, 1, 1).add();
            }
        }
        return b.build();
    }

    /**
     * The Penance Queen: a colossal insect matriarch - plated thorax, a bloated, banded egg-laden abdomen, a
     * crowned and mandibled head, spiked forelimbs and six jointed legs.
     */
    static ModelSpec penanceQueen() {
        int shell = 0x7A6A3E;
        int dark = 0x3E3420;
        int deep = 0x5A4C2C;
        int pale = 0xC8B880;
        int gold = 0xE0C060;
        ModelSpec.Builder b = ModelSpec.builder("penance_queen").scale(2.0f).shadow(1.5f);
        p(b, "thorax", null, Role.BODY, shell).accent(pale).at(0, 13, -2).rot(-10, 0, 0).mat("chitin").box(-6, -6, -7, 12, 10, 12).add();
        p(b, "thorax_plate", "thorax", deep).at(0, -6, -1).mat("chitin").box(-5, -1.5f, -6, 10, 2, 11).box(-4, -2.5f, -4, 8, 1, 7).add();
        p(b, "abdomen", "thorax", Role.TAIL, shell).accent(pale).at(0, -1, 5).rot(5, 0, 0).mat("chitin").box(-7, -6, 0, 14, 11, 13).add();
        p(b, "abd_bands", "abdomen", dark).at(0, 0, 0).mat("chitin").box(-7.5f, -6.5f, 3, 15, 12, 1).box(-7.5f, -6.5f, 7, 15, 12, 1)
                .box(-6.5f, -5.5f, 11, 13, 10, 1).add();
        p(b, "abd_tip", "abdomen", deep).at(0, 0, 13).rot(15, 0, 0).mat("chitin").box(-4, -4, 0, 8, 7, 4).add();
        p(b, "eggs", "abd_tip", 0xE8E0B0).at(0, 2, 3).mat("slime").glow("full").box(-2, -1, 0, 2, 2, 2).box(0.5f, 0, 0.5f, 2, 2, 2)
                .box(-1, 1.5f, 1, 2, 2, 2).add();
        p(b, "head", "thorax", Role.HEAD, shell).accent(0xFF5030).at(0, -2, -7).mat("chitin").pattern("eyes").glow("eyes")
                .box(-4.5f, -4, -6, 9, 7, 6).add();
        p(b, "crown", "head", gold).at(0, -4, -3).mat("metal").box(-4, -1, -3, 8, 1, 6).box(-3.5f, -3, -3.2f, 1, 2, 1)
                .box(-0.5f, -4, -3.2f, 1, 3, 1).box(2.5f, -3, -3.2f, 1, 2, 1).add();
        p(b, "crown_gem", "crown", 0xFF5030).at(0, -2, -3.4f).mat("energy").glow("full").box(-0.5f, -0.5f, -0.4f, 1, 1, 1).add();
        s(b, "mandible", "head", false, pale).roles(Role.JAW, Role.JAW).at(2.5f, 2, -6).rot(10, 25, 0).mat("horn")
                .box(-1, 0, -4, 2, 2, 4).box(-2, 1, -4.5f, 2, 1, 1).add();
        s(b, "antenna", "head", false, dark).at(2.5f, -4, -5).rot(-30, 0, -30).mat("chitin").box(-0.5f, -7, -0.5f, 1, 7, 1).add();
        s(b, "foreleg", "thorax", false, deep).at(6, 0, -6).rot(-50, 0, -20).mat("chitin").box(-1.5f, 0, -1.5f, 3, 8, 3).add();
        s(b, "foreleg_blade", "foreleg", true, pale).at(0, 8, 0).rot(-70, 0, 0).mat("horn").box(-0.5f, 0, -1, 1, 7, 2)
                .box(-0.5f, 5, -2.5f, 1, 2, 2).add();
        float[] zs = {-3, 1, 5};
        float[] yaws = {-30, 0, 30};
        for (int i = 0; i < 3; i++) {
            String n = "leg" + i;
            s(b, n, "thorax", false, deep).roles(Role.LEG_EXTRA, Role.LEG_EXTRA).at(6, 2, zs[i]).rot(0, yaws[i], -25).mat("chitin")
                    .box(0, -1, -1, 8, 2, 2).add();
            s(b, n + "_knee", n, true, dark).at(8, 0, 0).mat("chitin").box(-1, -1.5f, -1.5f, 2, 3, 3).add();
            s(b, n + "_shin", n, true, shell).at(8, 0, 0).rot(0, 0, 15).mat("chitin").box(-1, 0, -1, 2, 10, 2).add();
            s(b, n + "_tip", n + "_shin", true, pale).at(0, 10, 0).mat("horn").box(-0.5f, 0, -0.5f, 1, 2, 1).add();
        }
        spikes(b, "back_spikes", "thorax_plate", pale, "horn", -2, new float[][]{{-3, -3, 3}, {3, -3, 3}, {0, 1, 4}}, -15);
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Pest Control
    // ------------------------------------------------------------------------------------------

    /** A Pest Control portal: a jagged void-stone frame around a churning purple vortex, shielded by void runes. */
    static ModelSpec pestPortal() {
        int stone = 0x4A4A56;
        int dark = 0x2E2E38;
        int vortex = 0xC060E0;
        ModelSpec.Builder b = ModelSpec.builder("pest_portal").scale(1.2f).shadow(1.0f);
        p(b, "base", null, dark).at(0, 24, 0).mat("rock").box(-11, -2, -4, 22, 2, 8).box(-9, -3, -3, 18, 1, 6).add();
        s(b, "pillar", "base", false, stone).at(8, -3, 0).rot(0, 0, 5).mat("rock").box(-1.5f, -28, -2, 3, 28, 4).add();
        s(b, "pillar_spike", "pillar", true, dark).at(1.5f, -14, 0).rot(0, 0, -40).mat("rock").box(-0.5f, -4, -1, 1, 4, 2)
                .box(-0.5f, 5, -1, 1, 3, 2).add();
        s(b, "pillar_cap", "pillar", true, dark).at(0, -28, 0).rot(0, 0, -15).mat("rock").box(-2, -4, -2.5f, 4, 4, 5).box(-1, -7, -1, 2, 3, 2).add();
        p(b, "lintel", "base", stone).at(0, -31, 0).mat("rock").box(-7, -2, -2, 14, 2, 4).box(-1.5f, -5, -1.5f, 3, 3, 3).add();
        p(b, "vortex", null, Role.SPIN, vortex).at(0, 9, 0).mat("energy").glow("full").box(-6, -14, -1, 12, 28, 2).add();
        p(b, "vortex_ring", "vortex", 0xE0A0FF).at(0, 0, 0).mat("energy").glow("full").box(-4, -10, -1.5f, 8, 2, 3).box(-4, 8, -1.5f, 8, 2, 3)
                .box(-5, -8, -1.5f, 2, 16, 3).box(3, -8, -1.5f, 2, 16, 3).add();
        p(b, "vortex_inner", "vortex", 0x3A0A5A).at(0, 0, 0).mat("energy").glow("full").box(-2, -6, -1.2f, 4, 12, 3).add();
        p(b, "core", "vortex", Role.FLOAT, 0xFFFFFF).at(0, 0, -1.6f).mat("energy").glow("full").box(-1.5f, -1.5f, -1, 3, 3, 1).add();
        Kit.P shield = p(b, "shield_runes", "base", 0x60E0F0).at(0, -16, 0).mat("energy").glow("full");
        shield.box(-10, -8, -0.5f, 1, 3, 1).box(9, -6, -0.5f, 1, 3, 1).box(-10, 4, -0.5f, 1, 3, 1).box(9, 6, -0.5f, 1, 3, 1);
        shield.add();
        p(b, "rubble", "base", stone).at(0, 0, 0).mat("rock").box(-13, -2, 1, 2, 2, 2).box(11, -2, -3, 2, 2, 2).box(-4, -1, 4, 2, 1, 2).add();
        s(b, "pillar_crystal", "pillar", true, 0x9A60D0).at(-1.5f, -20, 0).rot(0, 0, 40).mat("ice").glow("full")
                .box(-1, -4, -1, 2, 4, 2).box(-0.5f, 2, -1, 1, 3, 1).add();
        s(b, "pillar_band", "pillar", true, dark).at(0, -8, 0).mat("rock").box(-2, 0, -2.5f, 4, 2, 5).box(-2, -12, -2.5f, 4, 2, 5).add();
        p(b, "ground_cracks", "base", vortex).at(0, -3, 0).mat("energy").glow("full").box(-8, -0.5f, -1, 4, 1, 1)
                .box(4, -0.5f, 1, 4, 1, 1).box(-1, -0.5f, 2, 1, 1, 2).box(1, -0.5f, -3, 1, 1, 2).add();
        p(b, "lintel_spikes", "lintel", dark).at(0, -2, 0).mat("rock").box(-6, -3, -1, 2, 3, 2).box(4, -3, -1, 2, 3, 2)
                .box(-0.5f, -6, -0.5f, 1, 1, 1).add();
        p(b, "motes", "vortex", Role.SPIN, 0xF0C0FF).at(0, 0, 0).mat("energy").glow("full").box(-9, -6, -1, 1, 1, 1)
                .box(8, 2, 1, 1, 1, 1).box(-1, 12, 2, 1, 1, 1).box(2, -13, -2, 1, 1, 1).add();
        return b.build();
    }

    /** Brawler: a hulking, hunched pest with a swollen back and huge knuckled fists. */
    static ModelSpec brawler() {
        int skin = 0x6E7E4A;
        int dark = 0x3E4A26;
        int deep = 0x56663A;
        BestiaryRad.Frame f = new BestiaryRad.Frame("brawler", 1.3f, 0.9f).colors(skin, dark, deep, 0xD0E060)
                .legs(4, 4, 5, 4).torso(15, 11, 10, 30).arms(6, 6, 5, 6).head(7, 5, 6).hips(11, 4, 8);
        f.mat = "hide";
        f.claws = 0;
        f.handW = 7;
        f.handH = 5;
        f.toes = 3;
        f.clawColor = 0xD8CCA8;
        f.neckLen = 0;
        f.headGlow = "eyes";
        f.armSplay = 16;
        f.elbowBend = 10;
        ModelSpec.Builder b = f.build();
        p(b, "hump", "torso", deep).at(0, -11, 2).rot(-20, 0, 0).mat("hide").pattern("spots").accent(0x8A9A5A).box(-6, -4, -3, 12, 5, 7).add();
        spikes(b, "hump_spikes", "hump", 0xD8CCA8, "horn", -4, new float[][]{{-3, 0, 3}, {0, 1, 4}, {3, 0, 3}}, -20);
        s(b, "knuckles", "hand", true, dark).at(0, 5, -3.5f).mat("hide").box(-3, -1.5f, -1, 2, 2, 2).box(-1, -1.5f, -1, 2, 2, 2)
                .box(1, -1.5f, -1, 2, 2, 2).add();
        s(b, "fist_plate", "hand", true, 0x8A8A70).at(0, 0, 0).mat("chitin").box(-4, -1, -4, 8, 2, 8).add();
        teeth(b, "tusks", "jaw", TOOTH, -2, 2, 0, -3.5f, 2, -2);
        p(b, "belly", "torso", 0xA0A878).at(0, -7, -5.2f).mat("flesh").box(-4.5f, 0, -0.5f, 9, 6, 1).add();
        return b.build();
    }

    /** Defiler: a scrawny, long-armed hurler hunched over its missiles. */
    static ModelSpec defiler() {
        int skin = 0x7A6A3E;
        BestiaryRad.Frame f = new BestiaryRad.Frame("defiler", 1.0f, 0.5f).colors(skin, shade(skin, 0.6f), shade(skin, 0.8f), 0xE0C070)
                .legs(4, 4, 3, 2.5f).torso(8, 8, 6, 35).arms(6, 7, 2, 3).head(6, 5, 6).hips(7, 3, 5);
        f.mat = "hide";
        f.claws = 3;
        f.clawLen = 2;
        f.toes = 3;
        f.neckLen = 2;
        f.headGlow = "eyes";
        f.elbowBend = 40;
        f.shoulders = false;
        ModelSpec.Builder b = f.build();
        s(b, "ear", "head", false, skin).at(3, -2, -1).rot(0, -30, -30).mat("hide").box(0, -1, -1, 3, 2, 2).add();
        p(b, "spine", "torso", shade(skin, 0.6f)).at(0, -8, 3).mat("horn").box(-0.5f, 0, 0, 1, 7, 1).box(-1.5f, 1, 0.5f, 3, 1, 1)
                .box(-1.5f, 4, 0.5f, 3, 1, 1).add();
        p(b, "rock", "hand_r", 0x8A8A80).at(0, 3, -1).mat("rock").box(-1.5f, -1.5f, -1.5f, 3, 3, 3).add();
        p(b, "tail", "hips", Role.TAIL, skin).at(0, -1, 2.5f).rot(30, 0, 0).mat("hide").box(-1, -1, 0, 2, 2, 6).add();
        p(b, "tail2", "tail", Role.TAIL, shade(skin, 0.7f)).at(0, 0, 6).rot(10, 0, 0).mat("hide").box(-0.5f, -0.5f, 0, 1, 1, 4).add();
        p(b, "loin", "hips", 0x5A3A22).at(0, 0, -2.8f).mat("leather").box(-2, 0, -0.5f, 4, 4, 1).add();
        return b.build();
    }

    /** Ravager: a horned, crab-clawed wrecker that tears barricades apart. */
    static ModelSpec ravager() {
        int shell = 0x5E4E70;
        int dark = 0x382C48;
        ModelSpec.Builder b = ModelSpec.builder("ravager").scale(1.1f).shadow(0.7f);
        p(b, "body", null, Role.BODY, shell).accent(0xC0A0E0).at(0, 14, 0).rot(-15, 0, 0).mat("chitin").box(-5, -5, -6, 10, 9, 12).add();
        p(b, "carapace", "body", shade(shell, 1.15f)).at(0, -5, 1).mat("chitin").box(-5.5f, -2, -5, 11, 2, 10).add();
        spikes(b, "spikes", "carapace", 0xE0D0C0, "horn", -2, new float[][]{{-3, -3, 2}, {3, -3, 2}, {-3, 2, 3}, {3, 2, 3}}, -20);
        p(b, "head", "body", Role.HEAD, shell).accent(0xFF5030).at(0, -2, -6).rot(15, 0, 0).mat("chitin").pattern("eyes").glow("eyes")
                .box(-3.5f, -3, -5, 7, 6, 5).add();
        p(b, "horn", "head", 0xE0D0C0).at(0, -3, -4).rot(-30, 0, 0).mat("horn").box(-1, -5, -1, 2, 5, 2).box(-0.5f, -7, -0.5f, 1, 2, 1).add();
        s(b, "side_horn", "head", false, 0xE0D0C0).at(3, -2, -3).rot(-20, 0, -40).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        p(b, "jaw", "head", Role.JAW, dark).at(0, 2.5f, -3).rot(10, 0, 0).mat("chitin").box(-2.5f, 0, -2.5f, 5, 2, 3).add();
        teeth(b, "teeth", "jaw", TOOTH, -2, 2, 0, -2, 3, -1);
        s(b, "arm", "body", false, dark).roles(Role.ARM_LEFT, Role.ARM_RIGHT).at(5, -2, -3).rot(-30, 0, -20).mat("chitin")
                .box(-1, -1, -1, 2, 7, 2).add();
        s(b, "claw", "arm", true, shell).at(0, 7, 0).rot(-30, 0, 0).mat("chitin").box(-1.5f, 0, -2, 3, 4, 4).add();
        s(b, "pincer", "claw", true, 0xE0D0C0).at(0, 4, -1).rot(-10, 0, 0).mat("horn").box(-1.5f, 0, -1, 1, 3, 1).box(0.5f, 0, -1, 1, 3, 1).add();
        s(b, "leg_f", null, false, dark).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).at(3, 17, -2).mat("chitin").box(-1.5f, 0, -1.5f, 3, 7, 3).add();
        s(b, "leg_b", null, false, dark).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).at(3, 17, 3).mat("chitin").box(-1.5f, 0, -1.5f, 3, 7, 3).add();
        s(b, "foot_f", "leg_f", true, 0xE0D0C0).at(0, 6, -1.5f).mat("horn").box(-1.5f, 0, -1, 1, 1, 1).box(0.5f, 0, -1, 1, 1, 1).add();
        s(b, "foot_b", "leg_b", true, 0xE0D0C0).at(0, 6, -1.5f).mat("horn").box(-1.5f, 0, -1, 1, 1, 1).box(0.5f, 0, -1, 1, 1, 1).add();
        p(b, "tail", "body", Role.TAIL, shell).at(0, 0, 6).rot(-20, 0, 0).mat("chitin").box(-1.5f, -1.5f, 0, 3, 3, 5).add();
        p(b, "tail_spike", "tail", 0xE0D0C0).at(0, -1, 5).rot(-30, 0, 0).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        p(b, "belly", "body", dark).at(0, 4, 0).mat("chitin").box(-4, -0.5f, -5, 8, 1, 10).add();
        p(b, "carapace2", "carapace", shade(shell, 0.9f)).at(0, -2, 4).rot(15, 0, 0).mat("chitin").box(-4.5f, -1, -2, 9, 2, 4).add();
        s(b, "shoulder_spike", "body", false, 0xE0D0C0).at(5, -4, -4).rot(-20, 0, -40).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        eyes(b, "eye", "head", 0xFF5030, 2, -1.5f, -5, 1, 1);
        s(b, "knee", "leg_b", true, shell).at(0, 3, -1.5f).mat("chitin").box(-1.5f, -1, -0.5f, 3, 2, 1).add();
        s(b, "mandible", "jaw", false, 0xE0D0C0).at(2, 0, -2.5f).rot(0, 20, 0).mat("horn").box(-0.5f, -1, -2, 1, 1, 2).add();
        return b.build();
    }

    /** Shifter: a lean, four-legged teleporting pest with a long skull and a whip of a tail. */
    static ModelSpec shifter() {
        int skin = 0x4E6E6E;
        int dark = 0x2A4A4A;
        int glow = 0x80FFE0;
        ModelSpec.Builder b = ModelSpec.builder("shifter").shadow(0.6f);
        p(b, "body", null, Role.BODY, skin).accent(glow).at(0, 13, 0).mat("hide").pattern("stripes").box(-3, -3, -6, 6, 6, 12).add();
        p(b, "spine", "body", dark).at(0, -3, 0).mat("chitin").box(-1, -1, -5, 2, 1, 10).add();
        spikes(b, "spikes", "body", glow, "energy", -3.5f, new float[][]{{0, -3, 2}, {0, 0, 2}, {0, 3, 2}}, -30);
        p(b, "neck", "body", skin).at(0, -1, -6).rot(-20, 0, 0).mat("hide").box(-2, -2, -3, 4, 4, 3).add();
        p(b, "head", "neck", Role.HEAD, skin).accent(glow).at(0, 0, -3).rot(20, 0, 0).mat("hide").box(-2.5f, -2.5f, -4, 5, 5, 4).add();
        p(b, "snout", "head", dark).at(0, 0.5f, -4).mat("hide").box(-1.5f, -1.5f, -3, 3, 3, 3).add();
        p(b, "jaw", "head", Role.JAW, dark).at(0, 2, -3).rot(15, 0, 0).mat("hide").box(-1.5f, 0, -4, 3, 1, 4).add();
        teeth(b, "teeth", "snout", TOOTH, -1, 1, 1.5f, -2.6f, 2, 1);
        eyes(b, "eye", "head", glow, 1.6f, -1, -4, 1, 1);
        s(b, "ear", "head", false, dark).at(2, -2.5f, -1).rot(-30, 0, -30).mat("hide").box(-0.5f, -3, -0.5f, 1, 3, 2).add();
        String[] names = {"leg_f", "leg_b"};
        float[] zs = {-4, 4};
        Role[][] roles = {{Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT}, {Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT}};
        for (int i = 0; i < 2; i++) {
            String n = names[i];
            s(b, n, null, false, skin).roles(roles[i][0], roles[i][1]).at(2.5f, 13, zs[i]).rot(i == 1 ? -15 : 5, 0, -5).mat("hide")
                    .box(-1, -1, -1.5f, 2, 6, 3).add();
            s(b, n + "_shin", n, true, dark).at(0, 5, 0).rot(i == 1 ? 25 : -5, 0, 5).mat("hide").box(-0.5f, 0, -0.5f, 1, 6, 1).add();
            s(b, n + "_paw", n + "_shin", true, 0xD8D0C0).at(0, 5.5f, 0).rot(i == 1 ? -10 : 0, 0, 0).mat("horn").box(-1, 0, -2, 2, 1, 3).add();
        }
        p(b, "tail", "body", Role.TAIL, skin).at(0, -1, 6).rot(-20, 0, 0).mat("hide").box(-1, -1, 0, 2, 2, 5).add();
        p(b, "tail2", "tail", Role.TAIL, dark).at(0, 0, 5).rot(-15, 0, 0).mat("hide").box(-0.5f, -0.5f, 0, 1, 1, 6).add();
        p(b, "tail_wisp", "tail2", glow).at(0, 0, 6).mat("energy").glow("full").box(-1, -1, 0, 2, 2, 2).add();
        p(b, "ribs", "body", dark).at(0, 0, 0).mat("hide").box(-3.2f, -1, -3, 1, 3, 1).box(-3.2f, -1, 0, 1, 3, 1).box(2.2f, -1, -3, 1, 3, 1)
                .box(2.2f, -1, 0, 1, 3, 1).add();
        spikes(b, "neck_spines", "neck", glow, "energy", -2, new float[][]{{0, -1, 2}, {0, 1, 2}}, -40);
        s(b, "shoulder", "body", false, dark).at(3, -1, -4).mat("hide").box(-0.5f, -1.5f, -1.5f, 1, 3, 3).add();
        s(b, "hip", "body", false, dark).at(3, -1, 4).mat("hide").box(-0.5f, -1.5f, -1.5f, 1, 3, 3).add();
        p(b, "collar", "neck", glow).at(0, 0, 0).mat("energy").glow("full").box(-2.5f, -2.5f, -0.5f, 5, 1, 1).add();
        s(b, "claws_f", "leg_f_paw", true, 0xE8E0D0).at(0, 0.5f, -2).mat("horn").box(-1, -0.5f, -1, 1, 1, 1).box(0.2f, -0.5f, -1, 1, 1, 1).add();
        s(b, "claws_b", "leg_b_paw", true, 0xE8E0D0).at(0, 0.5f, -2).mat("horn").box(-1, -0.5f, -1, 1, 1, 1).box(0.2f, -0.5f, -1, 1, 1, 1).add();
        return b.build();
    }

    /** Spinner: a fat, spider-like portal healer with a woven sac on its back. */
    static ModelSpec spinner() {
        int body = 0x8A6A3E;
        int dark = 0x4A3A22;
        ModelSpec.Builder b = ModelSpec.builder("spinner").shadow(0.6f);
        p(b, "body", null, Role.BODY, body).accent(0xE0C080).at(0, 16, 0).mat("chitin").box(-4, -4, -4, 8, 6, 8).add();
        p(b, "abdomen", "body", Role.TAIL, body).accent(0xE0C080).at(0, -1, 4).rot(-15, 0, 0).mat("chitin").pattern("spots").box(-5, -5, 0, 10, 8, 9).add();
        p(b, "web_sac", "abdomen", 0xE8E0D0).at(0, -5, 4).mat("cloth").box(-3, -2, -3, 6, 2, 6).add();
        p(b, "head", "body", Role.HEAD, body).accent(0xFF5030).at(0, -1, -4).mat("chitin").box(-3, -2.5f, -3, 6, 4, 3).add();
        p(b, "eyes", "head", 0xFF5030).at(0, -1, -3).mat("energy").glow("full").box(-2, -0.5f, -0.6f, 1, 1, 1).box(1, -0.5f, -0.6f, 1, 1, 1)
                .box(-1, -1.5f, -0.5f, 2, 1, 1).add();
        s(b, "fang", "head", false, TOOTH).roles(Role.JAW, Role.JAW).at(1, 1.5f, -3).mat("horn").box(-0.5f, 0, -0.5f, 1, 2, 1).add();
        float[] zs = {-3, -1, 1, 3};
        float[] yaws = {-35, -10, 10, 35};
        for (int i = 0; i < 4; i++) {
            String n = "leg" + i;
            s(b, n, "body", false, dark).roles(Role.LEG_EXTRA, Role.LEG_EXTRA).at(4, -1, zs[i]).rot(0, yaws[i], -35).mat("chitin")
                    .box(0, -0.5f, -0.5f, 5, 1, 1).add();
            s(b, n + "_shin", n, true, body).at(5, 0, 0).rot(0, 0, 15).mat("chitin").box(-0.5f, 0, -0.5f, 1, 10, 1).add();
        }
        p(b, "thread", "abdomen", 0xF0F0F0).at(0, 0, 9).mat("cloth").box(-0.5f, 0, 0, 1, 1, 3).add();
        for (int i = 0; i < 4; i++) {
            String n = "leg" + i;
            s(b, n + "_knee", n, true, body).at(5, 0, 0).mat("chitin").box(-1, -1, -1, 2, 2, 2).add();
            s(b, n + "_tip", n + "_shin", true, 0xE8D8B0).at(0, 10, 0).mat("horn").box(-0.5f, 0, -0.5f, 1, 1, 1).add();
        }
        p(b, "abd_bands", "abdomen", dark).at(0, 0, 0).mat("chitin").box(-5.5f, -5.5f, 2, 11, 9, 1).box(-5.5f, -5.5f, 5, 11, 9, 1)
                .box(-4.5f, -4.5f, 8, 9, 7, 1).add();
        s(b, "palp", "head", false, dark).at(2.5f, 1, -2.5f).rot(20, 20, 0).mat("chitin").box(-0.5f, 0, -2, 1, 1, 2).box(-0.5f, 1, -2.5f, 1, 2, 1).add();
        p(b, "web_strands", "web_sac", 0xF0F0F0).at(0, 0, 0).mat("cloth").box(-3.5f, -1, -0.5f, 7, 1, 1).box(-0.5f, -1, -3.5f, 1, 1, 7).add();
        return b.build();
    }

    /** Splatter: a pulsing, boil-covered sac of volatile filth on stubby legs. */
    static ModelSpec splatter() {
        int skin = 0xA0506E;
        int dark = 0x5A1E3A;
        ModelSpec.Builder b = ModelSpec.builder("splatter").scale(0.9f).shadow(0.5f);
        p(b, "body", null, Role.BODY, skin).accent(0xF090B0).at(0, 20, 0).mat("slime").box(-5, -9, -5, 10, 9, 10).add();
        p(b, "bulge", "body", skin).at(0, -4, 0).mat("slime").box(-6, -3, -4, 12, 5, 8).box(-4, -3, -6, 8, 5, 12).add();
        p(b, "cap", "body", shade(skin, 1.1f)).at(0, -9, 0).mat("slime").box(-3.5f, -1.5f, -3.5f, 7, 2, 7).add();
        for (int i = 0; i < 7; i++) {
            float a = (float) (i * Math.PI * 2 / 7);
            float y = -3 - (i % 3) * 2.5f;
            p(b, "boil" + i, "body", 0xF090B0).at((float) Math.cos(a) * 5.5f, y, (float) Math.sin(a) * 5.5f).mat("slime").glow("full")
                    .box(-1, -1, -1, 2, 2, 2).add();
        }
        p(b, "head", "body", Role.HEAD, skin).at(0, -5, -5).mat("slime").box(-3, -1.5f, -1.5f, 6, 3, 2).add();
        eyes(b, "eye", "head", 0xFFE060, 1.5f, -0.5f, -1.5f, 1, 1);
        p(b, "mouth", "body", Role.JAW, dark).at(0, -2, -5.5f).mat("flesh").box(-3, 0, -1, 6, 2, 1).add();
        teeth(b, "teeth", "mouth", TOOTH, -2.5f, 2.5f, 0, -1.2f, 4, 1);
        s(b, "leg_f", null, false, dark).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).at(3, 20, -3).mat("slime").box(-1.5f, 0, -1.5f, 3, 4, 3).add();
        s(b, "leg_b", null, false, dark).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).at(3, 20, 3).mat("slime").box(-1.5f, 0, -1.5f, 3, 4, 3).add();
        p(b, "drip", "body", 0xF090B0).at(0, 0, 0).mat("slime").glow("full").box(-4, -1, -5.5f, 1, 2, 1).box(3, -2, 5, 1, 3, 1).add();
        p(b, "veins", "body", dark).at(0, 0, 0).mat("flesh").box(-5.2f, -7, -1, 1, 5, 1).box(4.2f, -6, 1, 1, 5, 1)
                .box(-2, -7, 4.2f, 1, 5, 1).box(2, -8, -5.2f, 1, 4, 1).add();
        p(b, "lip", "mouth", shade(skin, 0.8f)).at(0, 2, -0.5f).mat("slime").box(-3.5f, 0, -1, 7, 1, 1).add();
        s(b, "toes_f", "leg_f", true, 0xE8D0D8).at(0, 3.5f, -1.5f).mat("horn").box(-1.5f, 0, -1, 1, 1, 1).box(0.5f, 0, -1, 1, 1, 1).add();
        s(b, "toes_b", "leg_b", true, 0xE8D0D8).at(0, 3.5f, -1.5f).mat("horn").box(-1.5f, 0, -1, 1, 1, 1).box(0.5f, 0, -1, 1, 1, 1).add();
        spikes(b, "nubs", "cap", shade(skin, 0.75f), "slime", -1.5f, new float[][]{{-2, -1, 1}, {1.5f, 0, 2}, {-0.5f, 2, 1}}, 0);
        p(b, "tongue", "mouth", 0xE07090).at(0.5f, 1, -1).rot(30, 0, 0).mat("flesh").box(-1, 0, -2, 2, 1, 2).add();
        p(b, "glow_cracks", "body", 0xF0A0C0).at(0, -1, 0).mat("energy").glow("full").box(-5.3f, -3, 2, 1, 1, 2).box(4.3f, -5, -3, 1, 1, 2)
                .box(-1, -2, 5.3f, 2, 1, 1).add();
        return b.build();
    }

    /** Torcher: a flitting, flame-winged pest that hurls fire. */
    static ModelSpec torcher() {
        int body = 0xC0602E;
        int dark = 0x60200E;
        int flame = 0xFFB040;
        ModelSpec.Builder b = ModelSpec.builder("torcher").scale(0.9f).shadow(0.4f);
        p(b, "body", null, Role.FLOAT, body).accent(flame).at(0, 11, 0).mat("hide").box(-3, -3, -4, 6, 6, 8).add();
        p(b, "belly", "body", dark).at(0, 3, 0).mat("hide").box(-2, -0.5f, -3, 4, 1, 6).add();
        p(b, "head", "body", Role.HEAD, body).accent(flame).at(0, -1, -4).mat("hide").pattern("eyes").glow("eyes").box(-2.5f, -2.5f, -4, 5, 5, 4).add();
        p(b, "beak", "head", 0xE0C080).at(0, 0.5f, -4).mat("horn").box(-1, -1, -2, 2, 2, 2).add();
        p(b, "flame_crest", "head", flame).at(0, -2.5f, -1).mat("lava").glow("full").box(-1.5f, -3, -1.5f, 3, 3, 4).box(-0.5f, -5, 0, 1, 2, 2).add();
        s(b, "wing", "body", false, dark).roles(Role.WING_LEFT, Role.WING_RIGHT).accent(flame).at(3, -2, 0).rot(0, 0, -10).mat("membrane")
                .box(0, -0.5f, -3, 9, 1, 7).add();
        s(b, "wing_flame", "wing", true, flame).at(9, 0, 0).mat("lava").glow("full").box(-3, -0.6f, -2, 4, 1, 5).box(1, -0.6f, -1, 2, 1, 3).add();
        s(b, "wing_bone", "wing", true, 0xE0C080).at(0, -0.6f, -3).mat("bone").box(0, -0.5f, -0.5f, 9, 1, 1).add();
        s(b, "talon", "body", false, dark).at(1.5f, 3, 1).rot(20, 0, 0).mat("hide").box(-0.5f, 0, -0.5f, 1, 3, 1).box(-1, 3, -1.5f, 2, 1, 2).add();
        p(b, "tail", "body", Role.TAIL, body).at(0, 0, 4).rot(-10, 0, 0).mat("hide").box(-1, -1, 0, 2, 2, 4).add();
        p(b, "tail_flame", "tail", flame).at(0, 0, 4).mat("lava").glow("full").box(-1.5f, -1.5f, 0, 3, 3, 3).add();
        p(b, "fireball", "body", Role.SPIN, flame).at(0, 5, -5).mat("energy").glow("full").box(-1, -1, -1, 2, 2, 2).add();
        s(b, "wing2", "wing", true, dark).accent(flame).at(9, 0, 0).rot(0, -20, 0).mat("membrane").box(0, -0.5f, -2, 6, 1, 6).add();
        s(b, "wing_finger", "wing2", true, 0xE0C080).at(0, -0.6f, -2).mat("bone").box(0, -0.5f, -0.5f, 6, 1, 1).box(2, -0.5f, 1, 4, 1, 1).add();
        p(b, "jaw", "head", Role.JAW, 0xC0A060).at(0, 1.5f, -4).rot(10, 0, 0).mat("horn").box(-1, 0, -2, 2, 1, 2).add();
        s(b, "horn", "head", false, 0xE0C080).at(2, -2.5f, -2).rot(-30, 0, -30).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        p(b, "chest_plate", "body", shade(body, 1.15f)).at(0, -1, -4).mat("hide").box(-2, -1.5f, -0.6f, 4, 4, 1).add();
        p(b, "flame_trail", "tail", Role.SPIN, flame).at(0, 0, 7).mat("lava").glow("full").box(-2, -1, 0, 1, 1, 1).box(1, 0, 1, 1, 1, 1)
                .box(0, -2, 2, 1, 1, 1).box(-1, 1, 3, 1, 1, 1).add();
        spikes(b, "back_flames", "body", flame, "lava", -3, new float[][]{{0, -2, 2}, {0, 0.5f, 3}, {0, 3, 2}}, -30);
        return b.build();
    }
}
