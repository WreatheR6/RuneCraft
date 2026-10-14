package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/**
 * Round 20 re-sculpts of common creatures, part 2. Same rules as {@link Common1}: each spec was rendered before/after
 * against the model it replaces and kept only where it reads clearly closer to the OSRS model. Ids, hitboxes and
 * everything outside the model are unchanged. Registered after {@link Common1} in {@link Overrides}.
 */
final class Common2 {
    private Common2() {
    }

    static List<ModelSpec> overrides() {
        List<ModelSpec> l = new ArrayList<>();
        // The TzHaar castes share one lava-rock body (the round-19 TzHaar-Ket body) and differ only by their gear.
        l.add(tzhaar("tzhaar_ket", Caste.KET));
        l.add(tzhaar("tzhaar_xil", Caste.XIL));
        l.add(tzhaar("tzhaar_mej", Caste.MEJ));
        l.add(tzhaar("tzhaar_hur_citizen", Caste.HUR));
        // OSRS greater and black demons have no wings
        l.add(bigDemon("greater_demon", 0xA4261A, 0x5C1610, 0xD8CCA8, 0xFFD030));
        l.add(bigDemon("black_demon", 0x3A3436, 0x1E191B, 0x6A5C56, 0xFF5020));
        l.add(bigDemon("catacomb_greater_demon", 0x96221A, 0x52140E, 0xC8BC98, 0xFFD030));
        l.add(bigDemon("catacomb_black_demon", 0x36323A, 0x1C181E, 0x6A5C56, 0xFF5020));
        l.add(dagannoth());
        l.add(turoth());
        l.add(drake());
        return l;
    }

    private static final int TOOTH = 0xF0EADA;
    private static final int CLAW = 0x2A2220;

    // ------------------------------------------------------------------------------------------
    // TzHaar
    // ------------------------------------------------------------------------------------------

    enum Caste { KET, XIL, MEJ, HUR }

    private static final int TZ_ROCK = 0x7A3A26;
    private static final int TZ_DEEP = 0x3E1C16;
    private static final int TZ_LIGHT = 0x9A5032;
    private static final int TZ_LAVA = 0xFF7A1A;
    private static final int TZ_EYE = 0xFFC030;
    private static final int OBSIDIAN = 0x34303A;

    /**
     * The TzHaar body every caste shares (OSRS uses one model and changes only the gear): a hunched, heavy-shouldered
     * lava-rock body with a big forward-thrust head, a fin crest running back over the skull, glowing orange eyes and
     * molten seams in the rock, digitigrade legs and a stubby tail. 27 boxes.
     */
    static ModelSpec.Builder tzhaarBody(String id) {
        int rock = TZ_ROCK;
        int deep = TZ_DEEP;
        int light = TZ_LIGHT;
        int lava = TZ_LAVA;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.0f);
        part(b, "hips", null, Role.BODY, rock).accent(lava).at(0, 9, 1).mat("rock").box(-5, -3, -3.5f, 10, 6, 7).add();
        part(b, "torso", "hips", Role.STATIC, rock).accent(lava).at(0, -2, 0).rot(28, 0, 0).mat("rock").glow("cracks")
                .box(-6, -11, -4, 12, 11, 8).box(-7.5f, -15, -4.5f, 15, 5, 9).add();
        part(b, "belly", "torso", Role.STATIC, light).accent(lava).at(0, -5, -4).mat("rock").glow("cracks")
                .box(-4, -4, -0.8f, 8, 7, 1).add();
        part(b, "head", "torso", Role.HEAD, rock).accent(TZ_EYE).at(0, -13, -5).rot(-24, 0, 0).mat("rock").pattern("eyes").glow("eyes")
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
        return b;
    }

    /**
     * A TzHaar of the given caste on the shared body. Ket (guards): obsidian chest plate, pauldrons and an obsidian
     * maul. Xil (fighters): a jagged obsidian sword, a throwing ring and a rock-plate kilt. Mej (mystics): a dark red
     * robe over the body with a gold-trimmed collar and an obsidian staff with a glowing orb. Hur (craftsmen): a
     * leather apron and tool belt with a pouch and a crafting hammer.
     */
    static ModelSpec tzhaar(String id, Caste caste) {
        ModelSpec.Builder b = tzhaarBody(id);
        int lava = TZ_LAVA;
        switch (caste) {
            case KET -> {
                part(b, "chest_plate", "torso", Role.STATIC, OBSIDIAN).accent(0x6A6070).at(0, -9, -4.2f).mat("obsidian")
                        .box(-5, -4, -1, 10, 8, 2).add();
                sym(b, "pauldron", "torso", false, OBSIDIAN).accent(0x6A6070).at(7.5f, -15.5f, 0).rot(0, 0, -15).mat("obsidian")
                        .box(-3.5f, -2, -4, 7, 4, 8).add();
                part(b, "maul", "fist_r", Role.STATIC, OBSIDIAN).accent(lava).at(0, 2, -1).rot(12, 0, 0).mat("obsidian")
                        .box(-1, -4, -1, 2, 15, 2).add();
                part(b, "maul_head", "maul", Role.STATIC, OBSIDIAN).accent(lava).at(0, 11, 0).mat("obsidian").glow("cracks")
                        .box(-4.5f, 0, -3, 9, 6, 6).add();
            }
            case XIL -> {
                // Toktz-xil-ak: a short grip and a long jagged obsidian blade with lava along its edge
                part(b, "sword", "fist_r", Role.STATIC, OBSIDIAN).accent(lava).at(0, 2, -2.5f).mat("obsidian")
                        .box(-1.5f, -2, -1, 3, 5, 1).add();
                part(b, "blade", "sword", Role.STATIC, 0x5A5264).accent(lava).at(0, 0.5f, -1).rot(-10, 0, 0).mat("crystal").glow("rim")
                        .box(-1, -1.5f, -14, 2, 3, 14).box(-1, -2.5f, -11, 2, 1, 2).box(-1, 1.5f, -7, 2, 1, 2).box(-1, -2.5f, -4, 2, 1, 2).add();
                // Toktz-xil-ul: an obsidian throwing ring held in the off hand
                part(b, "ring", "fist_l", Role.STATIC, 0x5A5264).accent(lava).at(0, 2, -6).rot(-30, 0, 0).mat("crystal").glow("rim")
                        .box(-3.5f, -3.5f, -1, 7, 2, 2).box(-3.5f, 1.5f, -1, 7, 2, 2).box(-3.5f, -1.5f, -1, 2, 3, 2).box(1.5f, -1.5f, -1, 2, 3, 2).add();
                part(b, "kilt", "hips", Role.STATIC, OBSIDIAN).accent(0x6A6070).at(0, 1, 0).mat("obsidian")
                        .box(-5.5f, 0, -4, 11, 4, 8).add();
            }
            case MEJ -> {
                int robe = 0x8A2A1E;
                int trim = 0xD8902A;
                part(b, "robe", "torso", Role.STATIC, robe).accent(trim).at(0, 0, 0).mat("cloth")
                        .box(-6.5f, -12, -4.5f, 13, 12, 9).add();
                part(b, "trim", "torso", Role.STATIC, trim).at(0, 0, -4.6f).mat("cloth")
                        .box(-1, -12, -0.6f, 2, 12, 1).box(-6.5f, -12.5f, -0.6f, 13, 1, 1).add();
                part(b, "skirt", "hips", Role.STATIC, robe).accent(trim).at(0, 1, 0).rot(-10, 0, 0).mat("cloth")
                        .box(-6, 0, -4.5f, 12, 10, 9).add();
                part(b, "hem", "skirt", Role.STATIC, trim).at(0, 10, 0).mat("cloth").box(-6.5f, -1, -5, 13, 1, 10).add();
                part(b, "staff", "fist_r", Role.STATIC, 0x2A2428).accent(lava).at(0, 2, -1).rot(35, 0, 0).mat("obsidian")
                        .box(-1, -18, -1, 2, 26, 2).add();
                part(b, "orb", "staff", Role.STATIC, 0xFF9030).accent(0xFFD070).at(0, -19.5f, 0).mat("energy").glow("full")
                        .box(-2, -2, -2, 4, 4, 4).add();
            }
            case HUR -> {
                int leather = 0x5A3A22;
                part(b, "apron", "torso", Role.STATIC, leather).accent(0x3A2414).at(0, -3, -4.4f).mat("leather")
                        .box(-4.5f, -8, -0.6f, 9, 9, 1).add();
                part(b, "apron_skirt", "hips", Role.STATIC, leather).accent(0x3A2414).at(0, 1, -3.8f).rot(-12, 0, 0).mat("leather")
                        .box(-4.5f, 0, -0.6f, 9, 8, 1).add();
                part(b, "belt", "hips", Role.STATIC, 0x3A2414).accent(0xC8A040).at(0, -1.5f, 0).mat("leather")
                        .box(-5.5f, -1, -4, 11, 2, 8).box(4.5f, 0, -1.5f, 2, 3, 3).add();
                part(b, "hammer", "fist_r", Role.STATIC, 0x6A4A2A).accent(lava).at(0, 2, -2).mat("wood")
                        .box(-1, -1, -9, 2, 2, 9).add();
                part(b, "hammer_head", "hammer", Role.STATIC, 0x5A5660).accent(lava).at(0, 0, -9).mat("metal")
                        .box(-1.5f, -3.5f, -2, 3, 7, 3).add();
            }
        }
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Greater and black demons
    // ------------------------------------------------------------------------------------------

    /**
     * Greater/black demon: a big hunched, wingless demon (OSRS gives neither wings) with a bull-like head, thick
     * horns curving out and up, heavy shoulders, long clawed arms, digitigrade legs on hooves and a long thin tail.
     */
    static ModelSpec bigDemon(String id, int skin, int dark, int horn, int eye) {
        int belly = mix(skin, (skin >> 16 & 255) > 0x60 ? 0xE8A070 : 0x6A6266, 0.22f);
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.0f);
        part(b, "hips", null, Role.BODY, skin).accent(dark).at(0, 10, 1.5f).mat("skin").box(-5, -2.5f, -3.5f, 10, 5, 7).add();
        part(b, "torso", "hips", Role.STATIC, skin).accent(dark).at(0, -1.5f, 0).rot(30, 0, 0).mat("skin")
                .box(-6, -11, -4.5f, 12, 11, 9).box(-8, -16, -5, 16, 6, 10).add();
        part(b, "belly", "torso", Role.STATIC, belly).accent(dark).at(0, -6, -4.5f).mat("skin")
                .box(-4, -5, -0.6f, 8, 9, 1).add();
        part(b, "spines", "torso", Role.STATIC, horn).at(0, -14, 5).rot(-35, 0, 0).mat("horn")
                .box(-0.5f, -3, -1, 1, 3, 2).box(-0.5f, 1, -1, 1, 2, 2).box(-0.5f, 4.5f, -1, 1, 2, 2).add();
        part(b, "head", "torso", Role.HEAD, skin).accent(eye).at(0, -15, -5).rot(-30, 0, 0).mat("skin").pattern("eyes").glow("eyes")
                .box(-4, -6, -6.5f, 8, 7, 7).add();
        part(b, "brow", "head", Role.STATIC, dark).at(0, -4.5f, -6.8f).rot(12, 0, 0).mat("skin").box(-4.2f, -1, -0.6f, 8, 2, 2).add();
        part(b, "snout", "head", Role.STATIC, skin).accent(dark).at(0, -1.5f, -6.5f).mat("skin").box(-2.5f, -1.5f, -2.5f, 5, 3, 3).add();
        part(b, "jaw", "head", Role.JAW, dark).at(0, 1, -3).rot(10, 0, 0).mat("skin").box(-3.5f, 0, -5.5f, 7, 2, 6).add();
        part(b, "fangs", "jaw", Role.STATIC, TOOTH).at(0, 0, -5).mat("horn")
                .box(-2.5f, -2, 0, 1, 2, 1).box(1.5f, -2, 0, 1, 2, 1).add();
        sym(b, "horn", "head", false, horn).at(3, -5, -3).rot(-10, 0, 70).mat("horn").box(-1.5f, -4, -1.5f, 3, 4, 3).add();
        sym(b, "horn_tip", "horn", true, horn).at(0, -3.5f, 0).rot(-25, 0, -60).mat("horn").box(-1, -5, -1, 2, 5, 2).add();
        sym(b, "arm", "torso", false, skin).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(dark).at(8.5f, -14, 0).rot(-40, 0, -12)
                .mat("skin").box(-3, -1.5f, -3, 6, 10, 6).add();
        sym(b, "fore", "arm", true, skin).accent(dark).at(0, 8, 0).rot(-30, 0, 0).mat("skin").box(-2.5f, 0, -2.5f, 5, 9, 5).add();
        sym(b, "hand", "fore", true, dark).at(0, 9, 0).mat("skin").box(-2.5f, 0, -2.5f, 5, 3, 5).add();
        sym(b, "claws", "hand", true, CLAW).at(0, 3, -1).mat("horn")
                .box(-2.5f, 0, -1.5f, 1, 3, 1).box(-0.5f, 0, -2, 1, 3, 1).box(1.5f, 0, -1.5f, 1, 3, 1).add();
        sym(b, "thigh", "hips", false, skin).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(dark).at(3.5f, 1.5f, 0).rot(-30, 0, 0)
                .mat("skin").box(-3, -1, -3, 6, 7, 6).add();
        sym(b, "shin", "thigh", true, skin).accent(dark).at(0, 6, 0).rot(45, 0, 0).mat("skin").box(-2, 0, -2, 4, 6, 4).add();
        sym(b, "hoof", "shin", true, dark).at(0, 6, 0).rot(-15, 0, 0).mat("horn").box(-2.5f, 0, -3.5f, 5, 4, 5).add();
        part(b, "tail", "hips", Role.TAIL, skin).accent(dark).at(0, 0, 3.5f).rot(55, 0, 0).mat("skin").box(-1.5f, -1.5f, 0, 3, 3, 7).add();
        part(b, "tail_mid", "tail", Role.STATIC, skin).at(0, 0, 7).rot(25, 0, 0).mat("skin").box(-1, -1, 0, 2, 2, 7).add();
        part(b, "tail_tip", "tail_mid", Role.STATIC, dark).at(0, 0, 7).rot(-30, 0, 0).mat("skin").box(-1, -1, 0, 2, 2, 4).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Dagannoth
    // ------------------------------------------------------------------------------------------

    /**
     * Dagannoth: a hunched green-grey sea horror leaning far forward on two thick legs, a long beak-like snout lined
     * with teeth, a row of bony spikes from the crown of its head down its back to the tail, a pale belly and two
     * short clawed arms.
     */
    static ModelSpec dagannoth() {
        int skin = 0x5E7466;
        int dark = 0x3A4A40;
        int belly = 0xAEB496;
        int spike = 0xD8D2B8;
        ModelSpec.Builder b = ModelSpec.builder("dagannoth").shadow(1.0f);
        part(b, "hips", null, Role.BODY, skin).accent(dark).at(0, 11, 3).mat("scales").box(-4.5f, -3, -3.5f, 9, 6, 7).add();
        part(b, "torso", "hips", Role.STATIC, skin).accent(dark).at(0, -2, -1).rot(62, 0, 0).mat("scales")
                .box(-5, -12, -4, 10, 12, 8).box(-4, -16, -3.5f, 8, 4, 7).add();
        part(b, "belly", "torso", Role.STATIC, belly).accent(dark).at(0, -6, -4).mat("plates").box(-4, -6, -0.8f, 8, 11, 1).add();
        part(b, "back_spikes", "torso", Role.STATIC, spike).at(0, 0, 3.5f).mat("horn")
                .box(-0.5f, -3, 0, 1, 2, 3).box(-1, -7, 0, 2, 2, 4).box(-1, -11, 0, 2, 2, 5).box(-1, -15, 0, 2, 2, 4).add();
        part(b, "neck", "torso", Role.NECK, skin).accent(dark).at(0, -16, -0.5f).rot(-20, 0, 0).mat("scales").box(-2.5f, -6, -3, 5, 6, 5).add();
        part(b, "head", "neck", Role.HEAD, skin).accent(0xFFE060).at(0, -5, -0.5f).rot(-30, 0, 0).mat("scales").pattern("eyes_animal")
                .box(-3, -4, -4, 6, 5, 6).add();
        part(b, "snout", "head", Role.STATIC, skin).accent(dark).at(0, -2.5f, -4).rot(8, 0, 0).mat("scales").box(-2, -1, -8, 4, 3, 8).add();
        part(b, "jaw", "head", Role.JAW, belly).accent(dark).at(0, 1, -3).rot(6, 0, 0).mat("plates").box(-1.8f, 0, -8.5f, 4, 1, 9).add();
        part(b, "teeth", "snout", Role.STATIC, TOOTH).at(0, 2, -4).mat("horn").box(-2, 0, -4, 4, 1, 7, -0.15f).add();
        part(b, "crown", "head", Role.STATIC, spike).at(0, -4, 0).rot(-60, 0, 0).mat("horn")
                .box(-0.5f, -3, -2, 1, 3, 2).box(-0.5f, -4, 1, 1, 4, 2).add();
        sym(b, "frill", "head", false, dark).at(3, -2, 1).rot(0, -50, -10).mat("membrane").box(0, -1.5f, -0.5f, 2, 3, 1).add();
        sym(b, "arm", "torso", false, skin).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(dark).at(5, -13, -2).rot(-80, 0, -10)
                .mat("scales").box(-1.5f, -1, -1.5f, 3, 6, 3).add();
        sym(b, "fore", "arm", true, skin).at(0, 5.5f, 0).rot(-50, 0, 0).mat("scales").box(-1.5f, 0, -1.5f, 3, 5, 3).add();
        sym(b, "claw", "fore", true, spike).at(0, 5, 0).mat("horn").box(-1.5f, 0, -1.5f, 1, 3, 1).box(0.5f, 0, -1.5f, 1, 3, 1).add();
        sym(b, "thigh", "hips", false, skin).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(dark).at(4, 1, 0).rot(-25, 0, 0)
                .mat("scales").box(-3, -1.5f, -3, 6, 8, 6).add();
        sym(b, "shin", "thigh", true, skin).accent(dark).at(0, 6.5f, 0).rot(35, 0, 0).mat("scales").box(-2, 0, -2, 4, 6, 4).add();
        sym(b, "foot", "shin", true, dark).at(0, 6, 0).rot(-10, 0, 0).mat("scales").box(-2.5f, -1, -5, 5, 2, 7).add();
        part(b, "tail", "hips", Role.TAIL, skin).accent(dark).at(0, -1, 3).rot(30, 0, 0).mat("scales").box(-2.5f, -2.5f, 0, 5, 5, 7).add();
        part(b, "tail_tip", "tail", Role.STATIC, skin).at(0, 0, 7).rot(15, 0, 0).mat("scales").box(-1.5f, -1.5f, 0, 3, 3, 6)
                .box(-0.5f, -3.5f, 1, 1, 2, 2).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Turoth
    // ------------------------------------------------------------------------------------------

    /**
     * Turoth: "one leg short" - a pink, fleshy, hunched brute that hops on a single thick leg, balancing on long
     * knuckle-dragging arms, with a heavy underslung jaw, small deep-set eyes and a lumpy back.
     */
    static ModelSpec turoth() {
        int skin = 0xC48A80;
        int dark = 0x8A5450;
        int light = 0xDCA89C;
        ModelSpec.Builder b = ModelSpec.builder("turoth").shadow(0.9f);
        part(b, "hips", null, Role.BODY, skin).accent(dark).at(0, 9, 1).mat("skin").box(-4.5f, -3, -3.5f, 9, 6, 7).add();
        part(b, "torso", "hips", Role.STATIC, skin).accent(dark).at(0, -2, 0).rot(35, 0, 0).mat("skin")
                .box(-6, -11, -4.5f, 12, 11, 9).box(-7, -15, -4, 14, 5, 9).add();
        part(b, "belly", "torso", Role.STATIC, light).accent(dark).at(0, -5, -4.5f).mat("skin").box(-4, -5, -0.6f, 8, 8, 1).add();
        part(b, "lumps", "torso", Role.STATIC, dark).at(0, -12, 4.5f).mat("skin")
                .box(-3.5f, 0, -1, 3, 3, 2).box(1, 3, -1, 3, 3, 2).box(-2, 6, -1, 3, 2, 2).add();
        part(b, "head", "torso", Role.HEAD, skin).accent(0xFFE070).at(0, -14, -5).rot(-30, 0, 0).mat("skin").pattern("eyes_animal")
                .box(-3.5f, -6, -5, 7, 6, 6).add();
        part(b, "brow", "head", Role.STATIC, dark).at(0, -4.5f, -5.2f).mat("skin").box(-3.8f, -1, -0.6f, 8, 2, 2).add();
        part(b, "jaw", "head", Role.JAW, skin).accent(dark).at(0, -0.5f, -2).rot(8, 0, 0).mat("skin").box(-4.5f, 0, -5, 9, 4, 6).add();
        part(b, "teeth", "jaw", Role.STATIC, TOOTH).at(0, 0, -4.8f).mat("horn")
                .box(-3.5f, -1.5f, -0.3f, 1, 2, 1).box(2.5f, -1.5f, -0.3f, 1, 2, 1).box(-1, -1, -0.3f, 2, 1, 1).add();
        sym(b, "arm", "torso", false, skin).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(dark).at(8, -13, 0).rot(-30, 0, -22)
                .mat("skin").box(-2.5f, -1.5f, -2.5f, 5, 10, 5).add();
        sym(b, "fore", "arm", true, skin).accent(dark).at(0, 8.5f, 0).rot(-20, 0, 0).mat("skin").box(-2, 0, -2, 4, 10, 4).add();
        sym(b, "hand", "fore", true, dark).at(0, 10, 0).mat("skin").box(-2.5f, 0, -3, 5, 3, 5).add();
        sym(b, "talons", "hand", true, CLAW).at(0, 1.5f, -3).mat("horn").box(-2.5f, 0, -1.5f, 1, 2, 2).box(1.5f, 0, -1.5f, 1, 2, 2).add();
        // the single leg, under the middle of the body
        part(b, "thigh", "hips", Role.LEG_FRONT_LEFT, skin).accent(dark).at(0, 2, 0.5f).rot(-15, 0, 0).mat("skin")
                .box(-3.5f, -1, -3.5f, 7, 7, 7).add();
        part(b, "shin", "thigh", Role.STATIC, skin).accent(dark).at(0, 6, 0).rot(25, 0, 0).mat("skin").box(-2.5f, 0, -2.5f, 5, 7, 5).add();
        part(b, "foot", "shin", Role.STATIC, dark).at(0, 6.5f, 0).rot(-10, 0, 0).mat("skin").box(-3.5f, -1, -5.5f, 7, 2, 8).add();
        part(b, "toes", "foot", Role.STATIC, CLAW).at(0, 0, -5.5f).mat("horn")
                .box(-3.5f, -1, -1, 1, 2, 1).box(-0.5f, -1, -1, 1, 2, 1).box(2.5f, -1, -1, 1, 2, 1).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Drake
    // ------------------------------------------------------------------------------------------

    /**
     * Drake: a wingless, four-legged red-brown dragon ("a dragon-like creature with no wings") with open, glowing
     * sores on its shoulders where wings would have grown, a spined back, a long neck, a horned head and a heavy tail.
     */
    static ModelSpec drake() {
        int skin = 0x8A3A24;
        int dark = 0x4E1E14;
        int belly = 0xC8925E;
        int horn = 0xD8C8A0;
        int sore = 0xFF6A2A;
        ModelSpec.Builder b = ModelSpec.builder("drake").shadow(1.2f);
        part(b, "body", null, Role.BODY, skin).accent(dark).at(0, 11, 1).rot(-8, 0, 0).mat("scales").box(-5, -4.5f, -9, 10, 9, 17).add();
        part(b, "belly", "body", Role.STATIC, belly).accent(dark).at(0, 5, 0).mat("plates").box(-4.5f, -0.5f, -8, 9, 1, 15).add();
        part(b, "chest", "body", Role.STATIC, skin).accent(dark).at(0, -1, -9).rot(-15, 0, 0).mat("scales").box(-5, -5, -3, 10, 10, 4).add();
        part(b, "sores", "body", Role.STATIC, 0xA82A1C).accent(sore).at(0, -4.5f, -5).mat("flesh").glow("spots")
                .box(-5.5f, -1.5f, -2.5f, 4, 2, 5).box(1.5f, -1.5f, -2.5f, 4, 2, 5).add();
        part(b, "ridge", "body", Role.STATIC, horn).at(0, -5, 0).mat("horn")
                .box(-0.5f, -2, -2, 1, 2, 2).box(-0.5f, -3, 2, 1, 3, 2).box(-0.5f, -2, 6, 1, 2, 2).add();
        part(b, "neck", "body", Role.NECK, skin).accent(dark).at(0, -2, -10).rot(58, 0, 0).mat("scales").box(-2.5f, -10, -3, 5, 11, 6).add();
        part(b, "head", "neck", Role.HEAD, skin).accent(0xFFC040).at(0, -10, -0.5f).rot(-62, 0, 0).mat("scales").pattern("eyes").glow("eyes")
                .box(-3.5f, -4, -5, 7, 5, 6).add();
        part(b, "snout", "head", Role.STATIC, skin).accent(dark).at(0, -2.5f, -5).mat("scales").box(-2.5f, 0, -5, 5, 3, 5).add();
        part(b, "jaw", "head", Role.JAW, belly).accent(dark).at(0, 1, -4).rot(6, 0, 0).mat("plates").box(-2.5f, 0, -6, 5, 2, 6).add();
        part(b, "teeth", "snout", Role.STATIC, TOOTH).at(0, 3, -2.5f).mat("horn").box(-2.5f, 0, -2.5f, 5, 1, 5, -0.15f).add();
        sym(b, "horn", "head", false, horn).at(2.5f, -4, 0).rot(30, 0, -15).mat("horn").box(-1, -1, 0, 2, 2, 5).add();
        sym(b, "horn_tip", "horn", true, horn).at(0, 0, 5).rot(15, 0, 0).mat("horn").box(-0.5f, -0.5f, 0, 1, 1, 4).add();
        sym(b, "front_leg", "body", false, skin).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(dark).at(5.5f, 1, -6).rot(-10, 0, -12)
                .mat("scales").box(-2.5f, -2, -2.5f, 5, 8, 5).add();
        sym(b, "front_shin", "front_leg", true, skin).at(0, 6, 0).rot(15, 0, 0).mat("scales").box(-2, 0, -2, 4, 6, 4).add();
        sym(b, "front_foot", "front_shin", true, dark).at(0, 5.5f, 0).rot(-5, 0, 0).mat("scales").box(-2.5f, 0, -4, 5, 2, 6)
                .box(-2.5f, 0.5f, -5, 1, 1, 1).box(1.5f, 0.5f, -5, 1, 1, 1).add();
        sym(b, "hind_leg", "body", false, skin).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(dark).at(5.5f, 0, 5).rot(-25, 0, -10)
                .mat("scales").box(-3, -3, -3.5f, 6, 9, 7).add();
        sym(b, "hind_shin", "hind_leg", true, skin).at(0, 5.5f, 1).rot(40, 0, 0).mat("scales").box(-2, 0, -2, 4, 6, 4).add();
        sym(b, "hind_foot", "hind_shin", true, dark).at(0, 5.5f, 0).rot(-15, 0, 0).mat("scales").box(-2.5f, 0, -4, 5, 2, 6).add();
        part(b, "tail", "body", Role.TAIL, skin).accent(dark).at(0, -1, 8).rot(-12, 0, 0).mat("scales").box(-3.5f, -3, 0, 7, 6, 8).add();
        part(b, "tail_mid", "tail", Role.STATIC, skin).at(0, 0, 8).rot(-8, 0, 0).mat("scales").box(-2.5f, -2, 0, 5, 4, 8).add();
        part(b, "tail_tip", "tail_mid", Role.STATIC, dark).at(0, 0, 8).rot(-5, 0, 0).mat("scales").box(-1.5f, -1.5f, 0, 3, 3, 7)
                .box(-0.5f, -3, 1, 1, 2, 2).add();
        return b.build();
    }

    private static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
