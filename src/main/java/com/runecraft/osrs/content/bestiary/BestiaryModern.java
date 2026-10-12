package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.BestiaryBst.chain;
import static com.runecraft.osrs.content.bestiary.BestiaryBst.dark;
import static com.runecraft.osrs.content.bestiary.BestiaryBst.eyes;
import static com.runecraft.osrs.content.bestiary.BestiaryBst.light;
import static com.runecraft.osrs.content.bestiary.BestiaryBst.sideFangs;
import static com.runecraft.osrs.content.bestiary.BestiaryBst.teeth;
import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/**
 * Bosses added in 2025-2026 (bosses module, {@code ModernBosses}), sculpted to the TzTok-Jad standard: Yama and his
 * void flares, the Doom of Mokhaiotl, the Gemstone Crab, the Shellbane gryphon, Brutus and Demonic Brutus, and the
 * Maggot King. These are the only models for these ids (no low-detail originals to override).
 */
final class BestiaryModern {
    private BestiaryModern() {
    }

    static List<ModelSpec> all() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(yama());
        l.add(voidFlare());
        l.add(doom());
        l.add(gemstoneCrab());
        l.add(shellbane());
        l.add(bull("brutus", 0x5A3A26, 0x3A2416, 0xE8DCC0, 0x2A1A10, false));
        l.add(bull("demonic_brutus", 0x5A1414, 0x2A0A0A, 0x3A3030, 0xFF6A1E, true));
        l.add(maggotKing());
        return l;
    }

    // ------------------------------------------------------------------------------------------------
    // Yama
    // ------------------------------------------------------------------------------------------------

    /**
     * Yama: a towering demon lord in glyph-scored black plate, great backswept horns crowned with fire, burning eyes,
     * clawed gauntlets, ragged membranous wings, digitigrade legs and a whipping tail.
     */
    static ModelSpec yama() {
        int skin = 0x7A1A16;
        int deep = 0x4A0E0E;
        int plate = 0x2A2226;
        int trim = 0x8A6A3A;
        int fire = 0xFF7A1E;
        int horn = 0x3A2E2A;
        int wing = 0x3A1414;
        ModelSpec.Builder b = ModelSpec.builder("yama").shadow(1.6f);
        // pelvis, torso and armour
        part(b, "hips", null, Role.BODY, plate).accent(trim).at(0, 3, 0).mat("metal").glow("runes")
                .box(-5, -2, -3, 10, 5, 6).add();
        part(b, "faulds", "hips", Role.STATIC, plate).accent(fire).at(0, 3, 0).mat("metal").glow("runes")
                .box(-5.5f, 0, -3.5f, 11, 4, 1).box(-5.5f, 0, 2.5f, 11, 4, 1).add();
        part(b, "torso", "hips", Role.STATIC, skin).accent(fire).at(0, -2, 0).rot(6, 0, 0).mat("skin").glow("cracks")
                .box(-6, -10, -3.5f, 12, 10, 7).add();
        part(b, "breastplate", "torso", Role.STATIC, plate).accent(trim).at(0, -6, -3.5f).mat("metal").glow("runes")
                .box(-6.5f, -4, -1.5f, 13, 9, 2).box(-4, 5, -1, 8, 2, 1).add();
        part(b, "backplate", "torso", Role.STATIC, plate).accent(trim).at(0, -6, 3.5f).mat("metal")
                .box(-6, -4, -0.5f, 12, 8, 2).add();
        part(b, "gorget", "torso", Role.STATIC, trim).accent(fire).at(0, -10, 0).mat("metal").box(-4.5f, -1.5f, -3.5f, 9, 2, 7).add();
        // head: long-jawed demon skull with backswept horns and a fire crown
        part(b, "head", "torso", Role.HEAD, skin).accent(fire).at(0, -11.5f, -0.5f).mat("skin").glow("eyes").pattern("eyes")
                .box(-3.5f, -6, -4, 7, 6, 7).add();
        part(b, "brow", "head", Role.STATIC, deep).at(0, -4.5f, -4).rot(14, 0, 0).mat("skin").box(-4, -1, -1, 8, 2, 2).add();
        eyes(b, "eyes", "head", 0xFFD040, 3, -3.5f, -4.3f, 2, 1);
        part(b, "snout", "head", Role.STATIC, deep).accent(fire).at(0, -1.5f, -4).mat("skin").box(-2.5f, -1, -3, 5, 3, 3).add();
        part(b, "jaw", "head", Role.JAW, deep).accent(fire).at(0, 0, -2.5f).rot(8, 0, 0).mat("skin").glow("maw")
                .box(-3, 0, -4.5f, 6, 2, 5).add();
        teeth(b, "teeth", "snout", 0xF0E0C0, 0, 2, -2.5f, 4, 4, 1, false);
        sym(b, "horn", "head", false, horn).accent(trim).at(3, -5, 0).rot(-30, 20, -30).mat("horn").box(-1.5f, -5, -1.5f, 3, 5, 3).add();
        sym(b, "horn_mid", "horn", true, horn).at(0, -5, 0).rot(-40, 0, 10).mat("horn").box(-1, -5, -1, 2, 5, 2).add();
        sym(b, "horn_tip", "horn_mid", true, dark(horn, 0.4f)).at(0, -5, 0).rot(-35, 0, 10).mat("horn").box(-0.5f, -4, -0.5f, 1, 4, 1).add();
        part(b, "crown", "head", Role.STATIC, fire).accent(0xFFE070).at(0, -6, 0.5f).mat("lava").glow("full")
                .box(-3, -2, -2.5f, 1, 2, 1).box(-0.5f, -3, -3, 1, 3, 1).box(2, -2, -2.5f, 1, 2, 1).box(-2.5f, -1, 1, 5, 1, 1).add();
        // pauldrons and arms
        sym(b, "pauldron", "torso", false, plate).accent(trim).at(6.5f, -9, 0).rot(0, 0, -20).mat("metal").glow("runes")
                .box(-2.5f, -2.5f, -3.5f, 6, 4, 7).add();
        sym(b, "pauldron_spike", "pauldron", true, horn).at(2, -2.5f, 0).rot(0, 0, -25).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        sym(b, "arm", "torso", false, skin).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(fire).at(7.5f, -8, 0).rot(-8, 0, -10)
                .mat("skin").glow("cracks").box(-2, -1, -2, 4, 9, 4).add();
        sym(b, "forearm", "arm", true, plate).accent(trim).at(0, 8, 0).rot(-18, 0, 4).mat("metal").glow("runes")
                .box(-2.5f, 0, -2.5f, 5, 7, 5).add();
        sym(b, "claw", "forearm", true, deep).accent(fire).at(0, 7, -0.5f).mat("skin").box(-2, 0, -2, 4, 3, 4).add();
        sym(b, "talons", "claw", true, 0xE8D8B0).at(0, 3, -1).rot(20, 0, 0).mat("horn")
                .box(-2, 0, -1.5f, 1, 2, 1).box(-0.5f, 0, -2, 1, 3, 1).box(1, 0, -1.5f, 1, 2, 1).add();
        // wings
        sym(b, "wing", "torso", false, wing).roles(Role.WING_LEFT, Role.WING_RIGHT).accent(fire).at(4, -8, 3.5f).rot(10, -30, -20)
                .mat("membrane").box(0, -2, 0, 10, 2, 2).box(2, 0, 0.5f, 9, 9, 1).add();
        sym(b, "wing_tip", "wing", true, dark(wing, 0.3f)).accent(fire).at(10, -1, 1).rot(0, 0, 30).mat("membrane")
                .box(0, -1, -0.5f, 7, 2, 2).box(0, 1, 0, 6, 7, 1).add();
        sym(b, "wing_claw", "wing_tip", true, 0xE8D8B0).at(7, 0, 0).mat("horn").box(0, -1, -0.5f, 2, 1, 1).add();
        // digitigrade legs
        sym(b, "thigh", "hips", false, skin).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(fire).at(3, 2, 0).rot(-20, 0, -4)
                .mat("skin").glow("cracks").box(-2.5f, 0, -2.5f, 5, 8, 5).add();
        sym(b, "greave", "thigh", true, plate).accent(trim).at(0, 7.5f, 0).rot(40, 0, 2).mat("metal").glow("runes")
                .box(-2, 0, -2, 4, 7, 4).add();
        sym(b, "hock", "greave", true, deep).at(0, 7, 0).rot(-38, 0, 0).mat("skin").box(-1.5f, 0, -1.5f, 3, 5, 3).add();
        sym(b, "hoof", "hock", true, horn).at(0, 4.5f, -0.5f).rot(18, 0, 0).mat("horn").box(-2, 0, -3, 4, 2, 4).add();
        // tail
        String tail = chain(b, "tail", "hips", Role.TAIL, skin, fire, "skin", 0, 1, 3, false,
                new int[]{3, 3, 2, 2}, new int[]{5, 5, 4, 4}, new float[]{30, 10, 10, -10}, null);
        part(b, "tail_blade", tail, Role.STATIC, horn).accent(fire).at(0, 0, 4).mat("horn").box(-2, -0.5f, 0, 4, 1, 3).add();
        return b.build();
    }

    /** Void flare: a crackling ball of fire and shadow ringed by orbiting embers. */
    static ModelSpec voidFlare() {
        ModelSpec.Builder b = ModelSpec.builder("yama_void_flare").shadow(0.6f);
        part(b, "core", null, Role.FLOAT, 0x3A1A4A).accent(0xFF8A1E).at(0, 12, 0).mat("energy").glow("full")
                .box(-3, -3, -3, 6, 6, 6).add();
        part(b, "shell", "core", Role.STATIC, 0xE05010).accent(0xFFE070).at(0, 0, 0).mat("lava").glow("cracks")
                .box(-4, -2, -2, 8, 4, 4, 0.1f).box(-2, -4, -2, 4, 8, 4, 0.1f).box(-2, -2, -4, 4, 4, 8, 0.1f).add();
        part(b, "ring", "core", Role.SPIN, 0x6A2A8A).accent(0xC060FF).at(0, 0, 0).mat("energy").glow("full")
                .box(-6, -0.5f, -0.5f, 2, 1, 1).box(4, -0.5f, -0.5f, 2, 1, 1).box(-0.5f, -0.5f, -6, 1, 1, 2)
                .box(-0.5f, -0.5f, 4, 1, 1, 2).box(-4.5f, 1, -4.5f, 1, 1, 1).box(3.5f, -2, 3.5f, 1, 1, 1).add();
        part(b, "eye", "core", Role.HEAD, 0xFFD040).at(0, 0, -3).mat("energy").glow("full").pattern("eyes")
                .box(-1.5f, -1, -1, 3, 2, 1).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Doom of Mokhaiotl
    // ------------------------------------------------------------------------------------------------

    /**
     * The Doom of Mokhaiotl: a hulking, hunched demonic burrower with a ridged carapace of volcanic plates, a cluster
     * of burning eyes, great crushing claws, mandibles around a glowing maw and six digging legs.
     */
    static ModelSpec doom() {
        int shell = 0x3A2A3A;
        int plateC = 0x5A3A4A;
        int flesh = 0x7A2A3A;
        int glowC = 0xC040FF;
        int horn = 0x2A1E22;
        ModelSpec.Builder b = ModelSpec.builder("doom_of_mokhaiotl").shadow(2.0f);
        part(b, "body", null, Role.BODY, flesh).accent(glowC).at(0, 10, 2).rot(-10, 0, 0).mat("flesh").glow("cracks")
                .box(-8, -6, -10, 16, 12, 20).add();
        // carapace plates along the back
        int[] pw = {14, 16, 16, 14, 11};
        for (int i = 0; i < pw.length; i++) {
            part(b, "plate" + i, "body", Role.STATIC, i % 2 == 0 ? shell : plateC).accent(glowC).at(0, -6, -9 + i * 4.4f)
                    .rot(-14 + i * 4, 0, 0).mat("chitin").glow(i == 2 ? "cracks" : null)
                    .box(-pw[i] / 2f, -3, -2.5f, pw[i], 3, 5).add();
            part(b, "spike" + i, "plate" + i, Role.STATIC, horn).at(0, -3, 0).rot(-20, 0, 0).mat("horn")
                    .box(-1, -3 - (i == 2 ? 1 : 0), -1, 2, 3 + (i == 2 ? 1 : 0), 2).add();
        }
        sym(b, "flank", "body", false, shell).accent(glowC).at(8, -1, 0).rot(0, 0, -8).mat("chitin").box(-1, -4, -8, 2, 8, 16).add();
        part(b, "belly", "body", Role.STATIC, dark(flesh, 0.3f)).accent(glowC).at(0, 6, 0).mat("flesh").glow("cracks")
                .box(-6, -1, -8, 12, 2, 16).add();
        // head
        part(b, "head", "body", Role.HEAD, shell).accent(glowC).at(0, -1, -11).rot(16, 0, 0).mat("chitin").pattern("eyes")
                .box(-6, -5, -6, 12, 9, 7).add();
        part(b, "crest", "head", Role.STATIC, plateC).accent(glowC).at(0, -5, -2).rot(-30, 0, 0).mat("chitin")
                .box(-5, -3, -2, 10, 3, 5).add();
        part(b, "eye_cluster", "head", Role.STATIC, glowC).at(0, -2, -6.2f).mat("energy").glow("full")
                .box(-4, -1, -0.5f, 2, 2, 1).box(-1, -2, -0.5f, 2, 2, 1).box(2, -1, -0.5f, 2, 2, 1)
                .box(-2.5f, 1, -0.5f, 1, 1, 1).box(1.5f, 1, -0.5f, 1, 1, 1).add();
        part(b, "jaw", "head", Role.JAW, flesh).accent(glowC).at(0, 3, -4).rot(12, 0, 0).mat("flesh").glow("maw")
                .box(-4, 0, -4, 8, 3, 5).add();
        sym(b, "mandible", "head", false, horn).accent(glowC).at(4.5f, 2, -6).rot(10, 25, 0).mat("horn")
                .box(-1, -1, -5, 2, 2, 5).box(-1.5f, -1, -6, 1, 2, 2).add();
        sideFangs(b, "fangs", "jaw", 0xE8D8C0, 3.5f, 0, -3.5f, 1.5f, 3, 1, true);
        // crushing fore-claws
        sym(b, "arm", "body", false, shell).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(glowC).at(8, 0, -8).rot(-20, -10, -20)
                .mat("chitin").box(-2.5f, -1, -2.5f, 5, 9, 5).add();
        sym(b, "forearm", "arm", true, plateC).accent(glowC).at(0, 8, 0).rot(-40, 0, 10).mat("chitin").glow("cracks")
                .box(-3, 0, -3, 6, 7, 6).add();
        sym(b, "pincer", "forearm", true, horn).accent(glowC).at(0, 7, 0).rot(20, 0, 0).mat("horn")
                .box(-3, 0, -3.5f, 2, 5, 3).box(1, 0, -3.5f, 2, 6, 3).add();
        // six digging legs
        for (int i = 0; i < 3; i++) {
            float z = -5 + i * 6;
            Role l = Role.LEG_EXTRA;
            sym(b, "leg" + i, "body", false, shell).roles(l, l).accent(glowC).at(8, 3, z).rot(0, i * -12 + 12, -40)
                    .mat("chitin").box(0, -1.5f, -1.5f, 7, 3, 3).add();
            sym(b, "shin" + i, "leg" + i, true, plateC).at(7, 0, 0).rot(0, 0, 70).mat("chitin").box(0, -1, -1, 8, 2, 2).add();
            sym(b, "foot" + i, "shin" + i, true, horn).at(8, 0, 0).rot(0, 0, 10).mat("horn").box(0, -0.5f, -1, 2, 1, 2).add();
        }
        // stubby tail segments
        chain(b, "tail", "body", Role.TAIL, shell, glowC, "chitin", 0, -1, 10, false,
                new int[]{10, 8, 5}, new int[]{4, 4, 3}, new float[]{20, 12, 12}, null);
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Gemstone Crab
    // ------------------------------------------------------------------------------------------------

    /** Gemstone Crab: a vast mossy rock crab whose shell is studded with sapphire, emerald, ruby and diamond clusters. */
    static ModelSpec gemstoneCrab() {
        int shell = 0x6A5A4A;
        int deep = 0x4A3E32;
        int leg = 0x8A4A2A;
        int moss = 0x4A6A2A;
        ModelSpec.Builder b = ModelSpec.builder("gemstone_crab").shadow(2.0f);
        part(b, "body", null, Role.BODY, deep).accent(leg).at(0, 15, 0).mat("chitin").box(-9, -3, -7, 18, 6, 14).add();
        part(b, "shell", "body", Role.STATIC, shell).accent(moss).at(0, -3, 0).mat("rock")
                .box(-10, -5, -8, 20, 5, 16).box(-8, -8, -6, 16, 3, 12).box(-5, -10, -4, 10, 2, 8).add();
        part(b, "moss", "shell", Role.STATIC, moss).at(0, -8, 0).mat("moss").box(-7, -0.5f, -5, 6, 1, 4).box(2, -2.5f, 1, 5, 1, 4).add();
        // Gem clusters poke out of each shell tier (tier tops at y -8, -11 and -13 from the body pivot).
        int[][] gems = {{0x3050E0, -6, -11, -3}, {0x30C060, 3, -13, -2}, {0xE03040, 6, -11, 3}, {0xF0F4FF, -2, -14, 1},
                {0x30C060, -9, -8, 5}, {0xE03040, -3, -13, 3}, {0x3050E0, 9, -8, -5}, {0xF0F4FF, 1, -11, -5}};
        for (int i = 0; i < gems.length; i++) {
            int[] g = gems[i];
            part(b, "gem" + i, "body", Role.STATIC, g[0]).accent(light(g[0], 0.5f)).at(g[1], g[2], g[3]).rot(i * 11 % 30, i * 37, i * 17 % 25)
                    .mat("energy").glow("full").box(-1, -2, -1, 2, 3, 2).box(-0.5f, -3, -0.5f, 1, 1, 1).add();
        }
        // eye stalks
        part(b, "head", "body", Role.HEAD, leg).accent(0x1A1A1A).at(0, -1, -7).mat("chitin").box(-4, -2, -2, 8, 3, 2).add();
        sym(b, "stalk", "head", false, leg).at(2.5f, -2, -1).rot(-10, 0, -10).mat("chitin").box(-0.5f, -4, -0.5f, 1, 4, 1).add();
        sym(b, "eye", "stalk", true, 0x101010).accent(0xF0F0F0).at(0, -4, 0).mat("skin").pattern("eyes").box(-1, -2, -1, 2, 2, 2).add();
        part(b, "mouth", "head", Role.JAW, dark(leg, 0.3f)).at(0, 1, -1).mat("chitin").box(-2, 0, -1.5f, 4, 2, 2).add();
        // claws
        sym(b, "arm", "body", false, leg).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(shell).at(8, 0, -7).rot(-20, -30, -10)
                .mat("chitin").box(-2, -2, -6, 4, 4, 6).add();
        sym(b, "claw", "arm", true, shell).accent(0x3050E0).at(0, 0, -6).rot(0, 20, 0).mat("rock").box(-3, -3, -7, 6, 5, 7).add();
        sym(b, "pincer", "claw", true, deep).at(0, 2, -7).rot(20, 0, 0).mat("chitin").box(-2.5f, 0, -4, 5, 2, 4).add();
        // eight legs
        for (int i = 0; i < 4; i++) {
            float z = -4 + i * 3;
            sym(b, "leg" + i, "body", false, leg).roles(Role.LEG_EXTRA, Role.LEG_EXTRA).accent(deep).at(9, 1, z)
                    .rot(0, -20 + i * 14, -35).mat("chitin").box(0, -1, -1, 6, 2, 2).add();
            sym(b, "tip" + i, "leg" + i, true, deep).at(6, 0, 0).rot(0, 0, 65).mat("chitin").box(0, -1, -1, 6, 2, 2).add();
        }
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Shellbane gryphon
    // ------------------------------------------------------------------------------------------------

    /**
     * The Shellbane gryphon: a great gryphon with a hooked eagle's beak, sea-weathered tawny body and pale feathered
     * breast, armour-like conch shell plates grown into its shoulders and back, and broad wings.
     */
    static ModelSpec shellbane() {
        int fur = 0x9A7A4A;
        int dk = 0x6A4A2A;
        int feather = 0xE8E0D0;
        int shell = 0xE0B898;
        int beak = 0xE0B030;
        ModelSpec.Builder b = ModelSpec.builder("shellbane_gryphon").shadow(1.5f);
        part(b, "body", null, Role.BODY, fur).accent(feather).at(0, 9, 2).rot(-10, 0, 0).mat("fur").box(-6, -5, -9, 12, 10, 18).add();
        part(b, "breast", "body", Role.STATIC, feather).accent(fur).at(0, 0, -9).rot(12, 0, 0).mat("feathers").box(-5, -5, -2, 10, 10, 3).add();
        for (int i = 0; i < 3; i++) {
            part(b, "conch" + i, "body", Role.STATIC, i == 1 ? light(shell, 0.2f) : shell).accent(0xC07A6A).at(0, -5, -5 + i * 5.5f)
                    .rot(-18 + i * 6, 0, 0).mat("plates").box(-5 + i * 0.5f, -3, -2.5f, 10 - i, 3, 5).add();
        }
        part(b, "conch_spire", "conch1", Role.STATIC, shell).accent(0xC07A6A).at(0, -3, 0).rot(-40, 0, 0).mat("plates")
                .box(-1.5f, -3, -1.5f, 3, 3, 3).box(-0.5f, -5, -0.5f, 1, 2, 1).add();
        // neck and eagle head
        part(b, "neck", "body", Role.NECK, feather).accent(fur).at(0, -3, -9).rot(-35, 0, 0).mat("feathers").box(-3, -3, -5, 6, 6, 6).add();
        part(b, "head", "neck", Role.HEAD, feather).accent(dk).at(0, -1, -5).rot(35, 0, 0).mat("feathers").pattern("eyes")
                .box(-3, -3.5f, -5, 6, 6, 6).add();
        eyes(b, "eyes", "head", 0xFFC020, 3.1f, -2, -2, 1, 1);
        part(b, "beak", "head", Role.STATIC, beak).accent(dark(beak, 0.3f)).at(0, -1, -5).mat("horn")
                .box(-1.5f, -1, -3, 3, 2, 3).box(-1, 0, -4, 2, 2, 1).add();
        part(b, "jaw", "head", Role.JAW, dark(beak, 0.15f)).at(0, 1, -4.5f).mat("horn").box(-1.2f, 0, -2.5f, 2, 1, 3).add();
        sym(b, "crest", "head", false, dk).at(2, -3.5f, 0).rot(-40, 15, -10).mat("feathers").box(-0.5f, -3, -1, 1, 3, 3).add();
        // wings
        sym(b, "wing", "body", false, fur).roles(Role.WING_LEFT, Role.WING_RIGHT).accent(feather).at(5.5f, -4, -4).rot(-10, -10, -25)
                .mat("feathers").box(0, -1.5f, -1, 11, 3, 8).add();
        sym(b, "wing_outer", "wing", true, dk).accent(feather).at(11, 0, 0).rot(0, 0, 20).mat("feathers").box(0, -1, -1, 9, 2, 10).add();
        sym(b, "primaries", "wing_outer", true, feather).at(9, 0, 2).rot(0, 15, 10).mat("feathers").box(0, -0.5f, -1, 5, 1, 9).add();
        // eagle forelegs, lion hind legs
        sym(b, "leg_f", "body", false, feather).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(beak).at(3.5f, 4, -7)
                .rot(8, 0, 0).mat("feathers").box(-1.5f, 0, -1.5f, 3, 7, 3).add();
        sym(b, "talon_f", "leg_f", true, beak).at(0, 7, 0).mat("horn").box(-1.5f, 0, -3, 3, 2, 4).box(-1.5f, 1, -4, 1, 1, 1)
                .box(0.5f, 1, -4, 1, 1, 1).add();
        sym(b, "thigh_b", "body", false, fur).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(feather).at(4, 1, 6)
                .rot(-20, 0, -4).mat("fur").box(-2, -2, -3, 4, 8, 6).add();
        sym(b, "shin_b", "thigh_b", true, fur).at(0, 6, 1).rot(40, 0, 2).mat("fur").box(-1.5f, 0, -1.5f, 3, 5, 3).add();
        sym(b, "paw_b", "shin_b", true, dk).at(0, 4.5f, 0).rot(-20, 0, 0).mat("fur").box(-2, 0, -3, 4, 2, 4).add();
        String tail = chain(b, "tail", "body", Role.TAIL, fur, feather, "fur", 0, -2, 9, false,
                new int[]{2, 2, 1, 1}, new int[]{4, 4, 4, 3}, new float[]{30, 15, 10, -15}, null);
        part(b, "tail_tuft", tail, Role.STATIC, dk).at(0, 0, 3).mat("fur").box(-1.5f, -1.5f, 0, 3, 3, 3).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Brutus / Demonic Brutus
    // ------------------------------------------------------------------------------------------------

    /**
     * Brutus, the Lumbridge cow field's prize bull: a deep-chested, heavy-shouldered bull with a dewlap, wide horns
     * and a brass nose ring. {@code demonic} gives Demonic Brutus' hellfire veins, burning eyes, extra horns and spines.
     */
    static ModelSpec bull(String id, int hide, int dk, int hornC, int accent, boolean demonic) {
        int hoof = 0x2A2018;
        String mat = demonic ? "rock" : "fur";
        String veins = demonic ? "cracks" : null;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.4f);
        part(b, "body", null, Role.BODY, hide).accent(accent).at(0, 10, 2).rot(-4, 0, 0).mat(mat).glow(veins)
                .box(-6, -6, -10, 12, 11, 11).box(-5.5f, -5, 1, 11, 10, 9).add();
        part(b, "hump", "body", Role.STATIC, dk).accent(accent).at(0, -6, -6).rot(-10, 0, 0).mat(mat).glow(veins)
                .box(-5, -3, -4, 10, 4, 8).add();
        part(b, "dewlap", "body", Role.STATIC, dk).at(0, 4, -9).mat(mat).box(-2.5f, 0, -2, 5, 3, 4).add();
        sym(b, "shoulder", "body", false, hide).accent(accent).at(6, -3, -6).rot(0, 0, -8).mat(mat).glow(veins)
                .box(-2, -3.5f, -4, 4, 8, 8).add();
        sym(b, "haunch", "body", false, hide).at(5.5f, -2, 6).rot(0, 0, -5).mat(mat).box(-2, -3.5f, -4, 4, 8, 8).add();
        if (!demonic) {
            part(b, "patch", "body", Role.STATIC, 0xE8E0D0).at(4, -2, 1).mat("fur").box(2, -2, -3, 1, 5, 6).add();
        } else {
            for (int i = 0; i < 4; i++) {
                part(b, "spine" + i, "body", Role.STATIC, hornC).accent(accent).at(0, -6, -2 + i * 3.5f).rot(-30, 0, 0).mat("horn")
                        .box(-1, -3 + (i == 1 ? -1 : 0), -1, 2, 3 + (i == 1 ? 1 : 0), 2).add();
            }
        }
        // head
        part(b, "head", "body", Role.HEAD, hide).accent(accent).at(0, -2, -11).rot(22, 0, 0).mat(mat).glow(demonic ? "eyes" : null)
                .pattern("eyes").box(-3.5f, -3.5f, -5, 7, 6, 6).add();
        part(b, "muzzle", "head", Role.STATIC, demonic ? dk : 0xC8A890).accent(accent).at(0, 1, -5).rot(4, 0, 0).mat(demonic ? "rock" : "skin")
                .box(-3, -1.5f, -3, 6, 4, 3).add();
        part(b, "ring", "muzzle", Role.STATIC, demonic ? 0x3A3A3A : 0xD8B040).at(0, 2.5f, -3).mat("metal")
                .box(-1.5f, 0, -0.5f, 3, 2, 1).add();
        part(b, "brow", "head", Role.STATIC, dk).at(0, -3.5f, -5).rot(10, 0, 0).mat(mat).box(-3.5f, -1, -1, 7, 2, 2).add();
        eyes(b, "eyes", "head", demonic ? accent : 0x1A1A1A, 3, -2, -5.3f, 1, 1);
        part(b, "jaw", "head", Role.JAW, dk).accent(accent).at(0, 2.5f, -4).rot(10, 0, 0).mat(mat).glow(demonic ? "maw" : null)
                .box(-2.5f, 0, -4, 5, 1, 4).add();
        sym(b, "ear", "head", false, hide).at(3.5f, -2.5f, -1).rot(0, -20, -40).mat(mat).box(0, -1, -1, 3, 2, 2).add();
        sym(b, "horn", "head", false, hornC).accent(accent).at(3.5f, -3.5f, -2).rot(0, 10, -70).mat("horn").box(0, -1.5f, -1.5f, 4, 3, 3).add();
        sym(b, "horn_tip", "horn", true, light(hornC, 0.2f)).at(4, 0, 0).rot(0, 20, 60).mat("horn").box(0, -1, -1, 4, 2, 2).add();
        if (demonic) {
            sym(b, "horn_extra", "head", false, hornC).accent(accent).at(2, -3.5f, 0.5f).rot(-50, 0, -20).mat("horn")
                    .box(-1, -4, -1, 2, 4, 2).add();
            part(b, "breath", "muzzle", Role.STATIC, accent).at(0, 0.5f, -3).mat("lava").glow("full")
                    .box(-2.5f, -0.5f, -1, 1, 1, 1).box(1.5f, -0.5f, -1, 1, 1, 1).add();
        }
        // legs
        sym(b, "leg_f", null, false, hide).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(accent).at(4.5f, 13, -7)
                .mat(mat).glow(veins).box(-2, -2, -2, 4, 7, 4).add();
        sym(b, "fore_f", "leg_f", true, dk).at(0, 5, 0).mat(mat).box(-1.5f, 0, -1.5f, 3, 5, 3).add();
        sym(b, "hoof_f", "fore_f", true, hoof).at(0, 5, 0).mat("horn").box(-2, 0, -2, 4, 1, 4).add();
        sym(b, "leg_b", null, false, hide).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(accent).at(4.5f, 12, 7)
                .rot(-10, 0, 0).mat(mat).glow(veins).box(-2, -2, -2.5f, 4, 8, 5).add();
        sym(b, "hock_b", "leg_b", true, dk).at(0, 6, 0).rot(14, 0, 0).mat(mat).box(-1.5f, 0, -1.5f, 3, 5, 3).add();
        sym(b, "hoof_b", "hock_b", true, hoof).at(0, 5, 0).rot(-4, 0, 0).mat("horn").box(-2, 0, -2, 4, 1, 4).add();
        String tail = chain(b, "tail", "body", Role.TAIL, hide, accent, mat, 0, -4, 10, false,
                new int[]{2, 1, 1}, new int[]{3, 4, 4}, new float[]{60, 15, 5}, null);
        part(b, "tail_tuft", tail, Role.STATIC, demonic ? accent : dk).at(0, 0, 3).mat(demonic ? "lava" : "fur").glow(demonic ? "full" : null)
                .box(-1, -1, 0, 2, 2, 3).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Maggot King
    // ------------------------------------------------------------------------------------------------

    /**
     * The Maggot King: an enormous bloated maggot rearing up in ringed segments, its translucent hide dotted with
     * glowing egg sacs, a tarnished vampyric crown on its eyeless, pincered head and tiny clawed forelimbs.
     */
    static ModelSpec maggotKing() {
        int flesh = 0xD8C8A0;
        int ring = 0xB8A47A;
        int sac = 0x9AD040;
        int maw = 0x8A2A2A;
        int gold = 0xA08A3A;
        ModelSpec.Builder b = ModelSpec.builder("maggot_king").shadow(1.9f);
        part(b, "base", null, Role.BODY, flesh).accent(ring).at(0, 19, 4).mat("flesh").box(-9, -5, -8, 18, 10, 16).add();
        // tail segments trailing behind
        chain(b, "tail", "base", Role.TAIL, ring, flesh, "flesh", 0, 0, 8, false,
                new int[]{14, 11, 8, 5}, new int[]{5, 5, 4, 3}, new float[]{5, 5, 5, 5}, null);
        // rearing segments
        int[] w = {16, 14, 12};
        String prev = "base";
        for (int i = 0; i < w.length; i++) {
            String n = "seg" + i;
            part(b, n, prev, i == 0 ? Role.STATIC : Role.NECK, i % 2 == 0 ? flesh : ring).accent(sac)
                    .at(0, i == 0 ? -5 : -6, i == 0 ? -3 : -1).rot(-8, 0, 0).mat("flesh")
                    .box(-w[i] / 2f, -6, -w[i] / 2f, w[i], 6, w[i]).add();
            part(b, "band" + i, n, Role.STATIC, ring).at(0, 0, 0).mat("flesh")
                    .box(-w[i] / 2f - 0.5f, -1, -w[i] / 2f - 0.5f, w[i] + 1, 1, w[i] + 1).add();
            prev = n;
        }
        // glowing egg sacs
        float[][] sacs = {{7, -2, -2}, {-7, -3, 1}, {5, -9, 4}, {-5, -10, -3}, {6, -14, 0}, {-4, -15, 3}};
        for (int i = 0; i < sacs.length; i++) {
            part(b, "sac" + i, "base", Role.STATIC, sac).accent(light(sac, 0.4f)).at(sacs[i][0], sacs[i][1], sacs[i][2])
                    .mat("slime").glow("full").box(-1.5f, -1.5f, -1.5f, 3, 3, 3).add();
        }
        // head with pincers and a crown
        part(b, "head", prev, Role.HEAD, flesh).accent(maw).at(0, -6, -1).rot(20, 0, 0).mat("flesh").pattern("eyes")
                .box(-5, -6, -6, 10, 7, 9).add();
        part(b, "jaw", "head", Role.JAW, maw).accent(0xFF6A6A).at(0, 0, -5).rot(14, 0, 0).mat("flesh").glow("maw")
                .box(-3.5f, -1, -3, 7, 3, 3).add();
        teeth(b, "teeth", "jaw", 0xF0E8D0, 0, -1, -2.8f, 6, 5, 1, true);
        sym(b, "pincer", "head", false, 0x6A5030).at(4, 0, -5).rot(10, 30, 10).mat("chitin").box(-1, -1, -4, 2, 2, 4)
                .box(-1.5f, -1, -5, 1, 2, 2).add();
        part(b, "crown", "head", Role.STATIC, gold).accent(0xC03040).at(0, -6, -1.5f).mat("metal").glow("runes")
                .box(-4, -2, -3.5f, 8, 2, 7).box(-4, -4, -3.5f, 1, 2, 1).box(-0.5f, -4.5f, -3.5f, 1, 3, 1)
                .box(3, -4, -3.5f, 1, 2, 1).box(-4, -3.5f, 2.5f, 1, 2, 1).box(3, -3.5f, 2.5f, 1, 2, 1).add();
        // stubby forelimbs
        sym(b, "arm", "seg1", false, ring).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(flesh).at(7, -3, -4).rot(-30, 0, -30)
                .mat("flesh").box(-1, 0, -1, 2, 5, 2).add();
        sym(b, "claw", "arm", true, 0x6A5030).at(0, 5, 0).mat("chitin").box(-1.5f, 0, -1.5f, 3, 2, 3).add();
        // fringe of feelers around the base
        for (int i = 0; i < 4; i++) {
            float a = (float) Math.toRadians(45 + i * 90);
            part(b, "feeler" + i, "base", Role.TENTACLE, ring).at((float) Math.cos(a) * 8, 4, (float) Math.sin(a) * 7)
                    .rot(0, -45 - i * 90, 20).mat("flesh").box(0, -0.5f, -0.5f, 4, 1, 1).add();
        }
        return b.build();
    }
}
