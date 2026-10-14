package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/**
 * Round 19 re-sculpts of the most commonly met (non-boss) creatures. Same rules as {@link Flagships}: each spec was
 * rendered before/after against the model it replaces and kept only where it reads clearly closer to the OSRS model.
 * Ids, hitboxes and everything outside the model are unchanged. Registered last in {@link Overrides}.
 */
final class Common1 {
    private Common1() {
    }

    static List<ModelSpec> overrides() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(tzhaarKet());
        l.add(ghost());
        l.add(bloodveld());
        l.add(caveKraken());
        l.add(gargoyle());
        l.add(abyssalDemon());
        // OSRS smoke devils share the Thermonuclear smoke devil's model at roughly half the size
        l.add(Flagships.thermonuclearSmokeDevil().copy("smoke_devil", 0.5f, null));
        l.add(pyrefiend());
        return l;
    }

    private static final int TOOTH = 0xF0EADA;
    private static final int CLAW = 0x2A2220;

    // ------------------------------------------------------------------------------------------
    // TzHaar-Ket
    // ------------------------------------------------------------------------------------------

    /**
     * TzHaar-Ket, the guards of Mor Ul Rek: a hunched, heavy-shouldered lava-rock body with a big forward-thrust head,
     * a fin crest running back over the skull, glowing orange eyes and molten seams in the rock, an obsidian chest
     * plate and pauldrons, and an obsidian maul. (Xil, Mej and Hur re-sculpts were tried and rejected this round.)
     */
    static ModelSpec tzhaarKet() {
        int rock = 0x7A3A26;
        int deep = 0x3E1C16;
        int light = 0x9A5032;
        int lava = 0xFF7A1A;
        int eye = 0xFFC030;
        int obs = 0x34303A;
        ModelSpec.Builder b = ModelSpec.builder("tzhaar_ket").shadow(1.0f);
        part(b, "hips", null, Role.BODY, rock).accent(lava).at(0, 9, 1).mat("rock").box(-5, -3, -3.5f, 10, 6, 7).add();
        part(b, "torso", "hips", Role.STATIC, rock).accent(lava).at(0, -2, 0).rot(28, 0, 0).mat("rock").glow("cracks")
                .box(-6, -11, -4, 12, 11, 8).box(-7.5f, -15, -4.5f, 15, 5, 9).add();
        part(b, "belly", "torso", Role.STATIC, light).accent(lava).at(0, -5, -4).mat("rock").glow("cracks")
                .box(-4, -4, -0.8f, 8, 7, 1).add();
        part(b, "head", "torso", Role.HEAD, rock).accent(eye).at(0, -13, -5).rot(-24, 0, 0).mat("rock").pattern("eyes").glow("eyes")
                .box(-4.5f, -6.5f, -7.5f, 9, 8, 8).add();
        part(b, "brow", "head", Role.STATIC, deep).at(0, -6, -7.5f).mat("rock").box(-4.5f, -1, -1, 9, 2, 2).add();
        part(b, "crest", "head", Role.STATIC, light).accent(lava).at(0, -6.5f, -3).rot(-18, 0, 0).mat("rock").glow("cracks")
                .box(-1.5f, -5, -4, 3, 5, 10).box(-1, -8, -1, 2, 3, 8).box(-0.5f, -10, 2, 1, 2, 5).add();
        part(b, "jaw", "head", Role.JAW, deep).accent(lava).at(0, 1.5f, -3).mat("rock").glow("maw").box(-3.5f, 0, -4.5f, 7, 2, 6).add();
        sym(b, "cheek_fin", "head", false, light).accent(lava).at(4.5f, -3, -2).rot(0, -20, -30).mat("rock").box(0, -2, -1.5f, 3, 4, 3).add();
        sym(b, "arm", "torso", false, rock).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(lava).at(7.5f, -13, 0).rot(-35, 0, -10)
                .mat("rock").glow("cracks").box(-2.5f, -1, -2.5f, 5, 9, 5).add();
        sym(b, "fore", "arm", true, rock).accent(lava).at(0, 8, 0).rot(-25, 0, 0).mat("rock").glow("cracks").box(-2.5f, 0, -2.5f, 5, 8, 5).add();
        sym(b, "fist", "fore", true, deep).at(0, 7.5f, 0).mat("rock").box(-3, 0, -3, 6, 4, 6).add();
        sym(b, "thigh", "hips", false, rock).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(lava).at(3.5f, 2, 0).rot(-20, 0, 0)
                .mat("rock").box(-2.5f, -1, -2.5f, 5, 8, 5).add();
        sym(b, "shin", "thigh", true, rock).accent(lava).at(0, 7, 0).rot(20, 0, 0).mat("rock").glow("cracks").box(-2.5f, 0, -2.5f, 5, 6, 5).add();
        sym(b, "foot", "shin", true, deep).at(0, 6, 0).mat("rock").box(-3, -1.3f, -4.5f, 6, 2, 7).add();
        part(b, "tail", "hips", Role.TAIL, rock).accent(lava).at(0, 0, 3.5f).rot(40, 0, 0).mat("rock").glow("cracks")
                .box(-2, -2, 0, 4, 4, 5).add();
        part(b, "tail_tip", "tail", Role.STATIC, deep).at(0, 0, 5).rot(15, 0, 0).mat("rock").box(-1, -1, 0, 2, 2, 4).add();
        part(b, "chest_plate", "torso", Role.STATIC, obs).accent(0x6A6070).at(0, -9, -4.2f).mat("obsidian")
                .box(-5, -4, -1, 10, 8, 2).add();
        sym(b, "pauldron", "torso", false, obs).at(7.5f, -15, 0).rot(0, 0, -15).mat("obsidian").box(-3, -2, -3.5f, 6, 3, 7).add();
        part(b, "maul", "fist_r", Role.STATIC, obs).accent(lava).at(0, 2, -1).rot(12, 0, 0).mat("obsidian")
                .box(-0.5f, -3, -0.5f, 1, 14, 1).add();
        part(b, "maul_head", "maul", Role.STATIC, obs).accent(lava).at(0, 11, 0).mat("obsidian").glow("cracks")
                .box(-3.5f, 0, -2.5f, 7, 5, 5).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Ghost
    // ------------------------------------------------------------------------------------------

    /**
     * Ghost: a pale, see-through hooded shroud with no legs, trailing into a wisp, its dark face hollow but for two
     * pin-prick eyes and its ragged sleeves held out in front.
     */
    static ModelSpec ghost() {
        int pale = 0xD4E2EA;
        int shade = 0xA8BCCB;
        int glow = 0xB8F0FF;
        ModelSpec.Builder b = ModelSpec.builder("ghost").shadow(0.5f);
        part(b, "body", null, Role.FLOAT, pale).accent(glow).at(0, 8, 0).rot(6, 0, 0).mat("spectral").box(-4.5f, -10, -3, 9, 10, 6).add();
        part(b, "skirt", "body", Role.STATIC, pale).accent(glow).at(0, 0, 0).rot(8, 0, 0).mat("spectral").box(-5.5f, 0, -3.5f, 11, 5, 7).add();
        part(b, "hem", "skirt", Role.STATIC, shade).accent(glow).at(0, 5, 0).mat("spectral")
                .box(-5.5f, 0, -3.5f, 2, 3, 1).box(-1.5f, 0, -3.5f, 3, 2, 1).box(3.5f, 0, -3.5f, 2, 3, 1)
                .box(-5.5f, 0, 2.5f, 3, 2, 1).box(2.5f, 0, 2.5f, 3, 3, 1).add();
        part(b, "wisp", "skirt", Role.TAIL, shade).accent(glow).at(0, 5, 0.5f).rot(20, 0, 0).mat("spectral")
                .box(-3, 0, -2, 6, 4, 4).add();
        part(b, "wisp_tip", "wisp", Role.STATIC, shade).accent(glow).at(0, 4, 0.5f).rot(25, 0, 8).mat("spectral")
                .box(-1.5f, 0, -1, 3, 4, 2).box(-0.5f, 4, -0.5f, 1, 2, 1).add();
        part(b, "hood", "body", Role.STATIC, pale).accent(glow).at(0, -10, 0.5f).mat("spectral")
                .box(-4.5f, -8, -4, 9, 8, 8).box(-2, -9.5f, -1, 4, 2, 5).add();
        part(b, "hood_rim", "hood", Role.STATIC, shade).accent(glow).at(0, -8, -4.3f).mat("spectral")
                .box(-5, 0, -0.8f, 2, 8, 1).box(3, 0, -0.8f, 2, 8, 1).box(-3, 0, -0.8f, 6, 1, 1).add();
        part(b, "head", "hood", Role.HEAD, 0x10141C).accent(glow).at(0, -3.5f, -4.2f).mat("ghost").pattern("eyes").glow("eyes")
                .box(-3, -3.5f, -0.5f, 6, 6, 1).add();
        sym(b, "arm", "body", false, pale).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(glow).at(4.5f, -9, -0.5f).rot(-75, 0, -8)
                .mat("spectral").box(-1.5f, -1, -1.5f, 3, 7, 3).add();
        sym(b, "sleeve", "arm", true, pale).accent(glow).at(0, 5.5f, 0).rot(8, 0, 0).mat("spectral").box(-2.5f, 0, -2.5f, 5, 4, 5).add();
        sym(b, "sleeve_rag", "sleeve", true, shade).accent(glow).at(0, 4, 2).rot(35, 0, 0).mat("spectral").box(-2, 0, -0.5f, 4, 3, 1).add();
        sym(b, "hand", "sleeve", true, 0xC8D6DE).accent(glow).at(0, 3.5f, -0.5f).mat("spectral").box(-1, 0, -1, 2, 3, 2).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Bloodveld
    // ------------------------------------------------------------------------------------------

    /**
     * Bloodveld: a hunched lump of raw flesh that is mostly mouth. The front of its body splits into a huge toothed
     * maw with a long lolling tongue, its brain bulges uncovered on top, and it scuttles on short clawed legs with two
     * grasping arms.
     */
    static ModelSpec bloodveld() {
        int flesh = 0xB04A44;
        int dark = 0x6E2626;
        int brain = 0xE89A9A;
        int gum = 0x7A1E26;
        ModelSpec.Builder b = ModelSpec.builder("bloodveld").shadow(0.8f);
        part(b, "hips", null, Role.BODY, flesh).accent(dark).at(0, 13, 1).mat("flesh").box(-4, -3, -3, 8, 5, 6).add();
        part(b, "torso", "hips", Role.STATIC, flesh).accent(dark).at(0, -2, 0).rot(35, 0, 0).mat("flesh").glow("veins")
                .box(-6, -12, -5, 12, 12, 10).add();
        part(b, "brain", "torso", Role.STATIC, brain).accent(dark).at(0, -12, 0.5f).mat("flesh")
                .box(-5, -3, -4.5f, 10, 3, 8).box(-0.5f, -3.6f, -4, 1, 1, 7).add();
        part(b, "brain_folds", "brain", Role.STATIC, 0xC87A80).at(0, -3, 0).mat("flesh")
                .box(-3.5f, -0.6f, -3, 2, 1, 6).box(1.5f, -0.6f, -3, 2, 1, 6).add();
        part(b, "spine", "torso", Role.STATIC, dark).at(0, -6, 5).mat("flesh")
                .box(-1, -4, -0.5f, 2, 2, 2).box(-1, 0, -0.5f, 2, 2, 2).box(-1, 4, -0.5f, 2, 2, 2).add();
        part(b, "head", "torso", Role.HEAD, flesh).accent(0xFFE040).at(0, -7, -5).rot(-25, 0, 0).mat("flesh").pattern("eyes").glow("eyes")
                .box(-5, -5, -5, 10, 5, 6).add();
        part(b, "gums_up", "head", Role.STATIC, gum).at(0, 0, -2).mat("flesh").box(-4, -0.8f, -2.8f, 8, 1, 5).add();
        BestiaryBst.teeth(b, "teeth_up", "head", TOOTH, 0, 0, -4.4f, 8, 4, 1, false);
        part(b, "jaw", "head", Role.JAW, flesh).accent(dark).at(0, 0.5f, 0).rot(28, 0, 0).mat("flesh").box(-4.5f, 0, -5, 9, 3, 6).add();
        part(b, "gums_lo", "jaw", Role.STATIC, gum).at(0, 0, -2).mat("flesh").box(-3.5f, -0.3f, -2.5f, 7, 1, 4).add();
        BestiaryBst.teeth(b, "teeth_lo", "jaw", TOOTH, 0, 0, -4.4f, 7, 3, 1, true);
        part(b, "tongue", "jaw", Role.TENTACLE, 0xC0505A).at(0, 1, -5).rot(25, 0, 0).mat("flesh").box(-1.5f, 0, -1, 3, 6, 2).add();
        part(b, "tongue_mid", "tongue", Role.TENTACLE, 0xB04450).at(0, 6, 0).rot(-15, 0, 0).mat("flesh").box(-1, 0, -1, 2, 6, 2).add();
        part(b, "tongue_tip", "tongue_mid", Role.STATIC, 0xA03A44).at(0, 6, 0).rot(-35, 0, 0).mat("flesh").box(-1.5f, 0, -1, 3, 3, 1).add();
        sym(b, "arm", "torso", false, flesh).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(dark).at(6, -9, -2).rot(-45, 0, -20)
                .mat("flesh").box(-1.5f, -1, -1.5f, 3, 6, 3).add();
        sym(b, "forearm", "arm", true, dark).at(0, 5.5f, 0).rot(-35, 0, 0).mat("flesh").box(-1.5f, 0, -1.5f, 3, 6, 3).add();
        sym(b, "claw", "forearm", true, CLAW).at(0, 6, -0.5f).rot(20, 0, 0).mat("horn")
                .box(-1.5f, 0, -1, 1, 3, 1).box(0.5f, 0, -1, 1, 3, 1).add();
        sym(b, "leg", "hips", false, flesh).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(dark).at(3, 1, 0).rot(-25, 0, -6)
                .mat("flesh").box(-2, -1, -2, 4, 6, 4).add();
        sym(b, "shin", "leg", true, dark).at(0, 5, 0).rot(45, 0, 6).mat("flesh").box(-1.5f, 0, -1.5f, 3, 5, 3).add();
        sym(b, "foot", "shin", true, dark).at(0, 4.5f, 0).rot(-20, 0, 0).mat("flesh").box(-2, 0, -3, 4, 1, 4)
                .box(-2, 0, -4, 1, 1, 1).box(1, 0, -4, 1, 1, 1).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Cave kraken
    // ------------------------------------------------------------------------------------------

    /**
     * Cave kraken: a small cousin of the Kraken boss. A tall red mantle with a spotted pale underside rises out of the
     * water, big yellow slit-pupil eyes sit either side of a horny beak, and eight arms splay out across the pool.
     */
    static ModelSpec caveKraken() {
        int hide = 0x8E2F38;
        int dark = 0x5A1A24;
        int under = 0xD88A84;
        int eye = 0xFFC83A;
        ModelSpec.Builder b = ModelSpec.builder("cave_kraken").shadow(1.2f);
        part(b, "base", null, Role.BODY, hide).accent(under).at(0, 19, 0).mat("flesh").box(-6, -3, -6, 12, 5, 12).add();
        part(b, "mantle", "base", Role.STATIC, hide).accent(under).at(0, -3, 1).rot(-14, 0, 0).mat("flesh").pattern("spots")
                .box(-5.5f, -8, -5, 11, 8, 10).add();
        part(b, "mantle_top", "mantle", Role.STATIC, hide).accent(under).at(0, -8, 0).rot(-10, 0, 0).mat("flesh").pattern("spots")
                .box(-4, -5, -3.5f, 8, 5, 7).add();
        part(b, "mantle_tip", "mantle_top", Role.STATIC, dark).at(0, -5, 0).rot(-12, 0, 0).mat("flesh").box(-2, -3, -2, 4, 3, 4).add();
        part(b, "ridge", "mantle", Role.STATIC, dark).at(0, -7, -5.3f).mat("flesh").box(-4, -0.5f, -0.5f, 8, 1, 1)
                .box(-3, 2.5f, -0.5f, 6, 1, 1).add();
        sym(b, "fin", "mantle_top", false, dark).roles(Role.FIN, Role.FIN).accent(under).at(4, -3, 0).rot(0, 0, -20)
                .mat("membrane").box(0, -3, -3, 1, 6, 6).add();
        sym(b, "eye", "base", false, eye).accent(0x1A0A08).at(5, -2, -5.5f).rot(0, -35, 0).mat("energy").glow("full")
                .box(-1.5f, -1.5f, -1, 3, 3, 2).add();
        sym(b, "pupil", "eye", true, 0x140806).at(0, 0, -1.1f).mat("skin").box(-0.5f, -1, -0.5f, 1, 2, 1).add();
        part(b, "head", "base", Role.HEAD, hide).accent(under).at(0, 0, -6).mat("flesh").pattern("blank").box(-3, -2, -2, 6, 4, 2).add();
        part(b, "beak", "head", Role.JAW, 0x2A1A14).at(0, 1.5f, -2).rot(20, 0, 0).mat("horn").box(-1.5f, 0, -2, 3, 2, 2).add();
        for (int i = 0; i < 8; i++) {
            float yaw = 22.5f + i * 45;
            double r = Math.toRadians(yaw);
            float px = (float) (-Math.sin(r) * 5);
            float pz = (float) (-Math.cos(r) * 5);
            part(b, "arm" + i, "base", Role.TENTACLE, i % 2 == 0 ? hide : dark).accent(under).at(px, 1, pz).rot(10, yaw, 0)
                    .mat("flesh").pattern("spots").box(-1.5f, -1.5f, -7, 3, 3, 7).add();
            part(b, "arm" + i + "_tip", "arm" + i, Role.TENTACLE, under).at(0, 0, -7).rot(i % 2 == 0 ? 12 : -8, 0, 0)
                    .mat("flesh").box(-1, -1, -6, 2, 2, 6).add();
        }
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Gargoyle
    // ------------------------------------------------------------------------------------------

    /**
     * Gargoyle: a crouching grey stone demon, hunched forward on long clawed arms, its blunt horned head jutting from
     * heavy shoulders and two great bat wings raised behind it.
     */
    static ModelSpec gargoyle() {
        int stone = 0x8A8A86;
        int dark = 0x5C5C5A;
        int light = 0xA8A8A2;
        ModelSpec.Builder b = ModelSpec.builder("gargoyle").shadow(1.1f);
        part(b, "hips", null, Role.BODY, stone).accent(dark).at(0, 12, 2).mat("granite").box(-5, -3, -4, 10, 6, 8).add();
        part(b, "torso", "hips", Role.STATIC, stone).accent(dark).at(0, -2, 0).rot(32, 0, 0).mat("granite")
                .box(-6, -11, -4.5f, 12, 11, 9).box(-7, -15, -5, 14, 5, 10).add();
        part(b, "head", "torso", Role.HEAD, stone).accent(0xFF4A2A).at(0, -15, -4).rot(-32, 0, 0).mat("granite").pattern("eyes")
                .box(-4, -6, -5, 8, 7, 7).add();
        part(b, "snout", "head", Role.STATIC, light).at(0, -2, -5).mat("granite").box(-3, -1, -3, 6, 3, 3).add();
        part(b, "jaw", "head", Role.JAW, dark).at(0, 1, -3).rot(10, 0, 0).mat("granite").box(-3, 0, -5, 6, 2, 6).add();
        BestiaryBst.teeth(b, "teeth", "jaw", TOOTH, 0, 0, -4.3f, 5, 3, 1, true);
        sym(b, "horn", "head", false, dark).at(3, -6, -1).rot(-50, 0, -20).mat("horn").box(-1, -5, -1, 2, 5, 2).box(-0.5f, -7, -0.5f, 1, 2, 1).add();
        sym(b, "wing", "torso", false, stone).roles(Role.WING_LEFT, Role.WING_RIGHT).accent(dark).at(4, -13, 4.5f).rot(-30, -25, -35)
                .mat("granite").box(0, -1, -1, 11, 2, 2).box(0, 1, -0.5f, 10, 9, 1).add();
        sym(b, "wing_tip", "wing", true, stone).accent(dark).at(11, 0, 0).rot(0, 0, 45).mat("granite")
                .box(0, -1, -1, 8, 2, 2).box(0, 1, -0.5f, 7, 9, 1).add();
        sym(b, "arm", "torso", false, stone).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(dark).at(7, -13, -1).rot(-40, 0, -12)
                .mat("granite").box(-2, -1, -2, 4, 8, 4).add();
        sym(b, "fore", "arm", true, stone).accent(dark).at(0, 7, 0).rot(-20, 0, 0).mat("granite").box(-2, 0, -2, 4, 8, 4).add();
        sym(b, "hand", "fore", true, dark).at(0, 8, 0).mat("granite").box(-2.5f, 0, -2.5f, 5, 2, 5)
                .box(-2.5f, 2, -2.5f, 1, 2, 1).box(1.5f, 2, -2.5f, 1, 2, 1).add();
        sym(b, "thigh", "hips", false, stone).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(dark).at(3.5f, 1, -1).rot(-50, 0, -10)
                .mat("granite").box(-2.5f, -1, -2.5f, 5, 7, 5).add();
        sym(b, "shin", "thigh", true, dark).at(0, 6, 0).rot(70, 0, 10).mat("granite").box(-2, 0, -2, 4, 6, 4).add();
        sym(b, "foot", "shin", true, dark).at(0, 6, 0).rot(-20, 0, 0).mat("granite").box(-2.5f, -0.5f, -4, 5, 1, 6).add();
        part(b, "tail", "hips", Role.TAIL, stone).accent(dark).at(0, 1, 4).rot(60, 0, 0).mat("granite").box(-1.5f, -1.5f, 0, 3, 3, 7).add();
        part(b, "tail_tip", "tail", Role.STATIC, dark).at(0, 0, 7).rot(20, 0, 0).mat("granite").box(-1, -1, 0, 2, 2, 5).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Abyssal demon
    // ------------------------------------------------------------------------------------------

    /**
     * Abyssal demon: a lanky, hunched denizen of the Abyss in dark mottled purple, with a small spiked head sunk
     * between bony shoulders, long arms ending in pale talons that nearly drag on the ground, bent digitigrade legs
     * and a long barbed tail.
     */
    static ModelSpec abyssalDemon() {
        int hide = 0x4A2448;
        int dark = 0x2C1430;
        int pale = 0x8A4A82;
        int talon = 0xD8CCB0;
        ModelSpec.Builder b = ModelSpec.builder("abyssal_demon").shadow(0.9f);
        part(b, "hips", null, Role.BODY, hide).accent(pale).at(0, 13, 3).mat("flesh").box(-4, -3, -3, 8, 6, 6).add();
        part(b, "torso", "hips", Role.STATIC, hide).accent(pale).at(0, -2, 0).rot(45, 0, 0).mat("flesh").pattern("spots")
                .box(-4.5f, -10, -3.5f, 9, 10, 7).box(-6.5f, -14, -4.5f, 13, 5, 9).add();
        part(b, "spikes", "torso", Role.STATIC, talon).at(0, -12, 4.5f).mat("horn")
                .box(-3.5f, -2, -0.5f, 1, 3, 2).box(2.5f, -2, -0.5f, 1, 3, 2).box(-0.5f, 2, -0.5f, 1, 3, 2).add();
        part(b, "head", "torso", Role.HEAD, dark).accent(0xFF3A3A).at(0, -13, -4.5f).rot(-45, 0, 0).mat("flesh").pattern("eyes").glow("eyes")
                .box(-3, -4, -5, 6, 5, 6).add();
        part(b, "jaw", "head", Role.JAW, hide).at(0, 1, -1).rot(12, 0, 0).mat("flesh").box(-2.5f, 0, -4.5f, 5, 2, 5).add();
        BestiaryBst.teeth(b, "teeth", "jaw", TOOTH, 0, 0, -4.3f, 4, 3, 1, true);
        sym(b, "horn", "head", false, talon).at(2.5f, -4, -1).rot(-55, 0, -20).mat("horn").box(-0.5f, -4, -0.5f, 1, 4, 1)
                .box(-1, -1, -1, 2, 2, 2).add();
        sym(b, "arm", "torso", false, hide).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(pale).at(7, -12, 0).rot(-40, 0, -10)
                .mat("flesh").box(-2, -1, -2, 4, 9, 4).add();
        sym(b, "fore", "arm", true, dark).accent(pale).at(0, 8.5f, 0).rot(-10, 0, 0).mat("flesh").box(-2, 0, -2, 4, 9, 4).add();
        sym(b, "talons", "fore", true, talon).at(0, 9, 0).rot(15, 0, 0).mat("horn").box(-2, 0, -2.5f, 4, 2, 4)
                .box(-2, 2, -2.5f, 1, 4, 1).box(-0.5f, 2, -2.5f, 1, 5, 1).box(1, 2, -2.5f, 1, 4, 1).add();
        sym(b, "thigh", "hips", false, hide).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(pale).at(3, 1, 0).rot(-35, 0, -5)
                .mat("flesh").box(-2, -1, -2, 4, 7, 4).add();
        sym(b, "shin", "thigh", true, dark).at(0, 6, 0).rot(70, 0, 5).mat("flesh").box(-1.5f, 0, -1.5f, 3, 6, 3).add();
        sym(b, "foot", "shin", true, dark).at(0, 6, 0).rot(-35, 0, 0).mat("flesh").box(-2, -0.5f, -3.5f, 4, 1, 5)
                .box(-2, -0.5f, -4.5f, 1, 1, 1).box(1, -0.5f, -4.5f, 1, 1, 1).add();
        part(b, "tail", "hips", Role.TAIL, hide).accent(pale).at(0, 0, 3).rot(-10, 0, 0).mat("flesh").box(-1.5f, -1.5f, 0, 3, 3, 8).add();
        part(b, "tail_mid", "tail", Role.TAIL, dark).at(0, 0, 8).rot(-20, 0, 0).mat("flesh").box(-1, -1, 0, 2, 2, 8).add();
        part(b, "tail_barb", "tail_mid", Role.STATIC, talon).at(0, 0, 8).mat("horn").box(-1.5f, -1, 0, 3, 2, 2).box(-0.5f, -0.5f, 2, 1, 1, 2).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Pyrefiend
    // ------------------------------------------------------------------------------------------

    /**
     * Pyrefiend: a small hovering fire demon, its body a crust of red-hot embers split by molten seams, a mane of
     * flame licking up from its head, clawed arms, and its lower half burning away into a tongue of fire.
     */
    static ModelSpec pyrefiend() {
        int ember = 0xB8401C;
        int dark = 0x5A1A0E;
        int flame = 0xFFB030;
        int hot = 0xFFE070;
        ModelSpec.Builder b = ModelSpec.builder("pyrefiend").shadow(0.5f);
        part(b, "body", null, Role.FLOAT, ember).accent(flame).at(0, 3, 0).rot(10, 0, 0).mat("lava").glow("cracks")
                .box(-4, -8, -3, 8, 9, 6).add();
        part(b, "head", "body", Role.HEAD, ember).accent(hot).at(0, -8, -1).rot(-10, 0, 0).mat("lava").pattern("eyes").glow("eyes")
                .box(-3.5f, -6, -3.5f, 7, 6, 7).add();
        part(b, "jaw", "head", Role.JAW, dark).accent(flame).at(0, 0, -1).rot(10, 0, 0).mat("lava").glow("maw").box(-3, 0, -3, 6, 2, 4).add();
        part(b, "mane", "head", Role.STATIC, flame).accent(hot).at(0, -6, 0.5f).rot(-15, 0, 0).mat("lava").glow("full")
                .box(-3.5f, -3, -2, 2, 3, 3).box(-1, -6, -1.5f, 2, 6, 3).box(1.5f, -4, -2, 2, 4, 3).box(-2.5f, -2, 1.5f, 5, 3, 2).add();
        part(b, "back_flames", "body", Role.STATIC, flame).accent(hot).at(0, -6, 3).rot(-25, 0, 0).mat("lava").glow("full")
                .box(-2.5f, -3, -0.5f, 2, 4, 2).box(0.5f, -2, -0.5f, 2, 3, 2).box(-1, 1, 0, 2, 3, 2).add();
        sym(b, "arm", "body", false, ember).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(flame).at(5, -7, -1).rot(-35, 0, -20)
                .mat("lava").box(-1.5f, -1, -1.5f, 3, 6, 3).add();
        sym(b, "fore", "arm", true, dark).accent(flame).at(0, 5.5f, 0).rot(-35, 0, 0).mat("lava").box(-1.5f, 0, -1.5f, 3, 5, 3).add();
        sym(b, "claw", "fore", true, 0x2A1410).at(0, 5, -0.5f).rot(20, 0, 0).mat("horn").box(-1.5f, 0, -1, 1, 2, 1).box(0.5f, 0, -1, 1, 2, 1).add();
        part(b, "fire0", "body", Role.TAIL, flame).accent(hot).at(0, 1, 0).rot(15, 0, 0).mat("lava").glow("full").box(-3.5f, 0, -2.5f, 7, 4, 5).add();
        part(b, "fire1", "fire0", Role.TAIL, flame).accent(hot).at(0, 4, 0.5f).rot(15, 0, 0).mat("lava").glow("full").box(-2.5f, 0, -1.5f, 5, 4, 3).add();
        part(b, "fire2", "fire1", Role.TAIL, hot).accent(0xFFFFC0).at(0, 4, 0).rot(15, 0, 0).mat("lava").glow("full").box(-1, 0, -1, 2, 3, 2).add();
        return b.build();
    }
}
