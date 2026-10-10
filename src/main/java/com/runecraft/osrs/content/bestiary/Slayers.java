package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/** Slayer monsters re-sculpted after their OSRS models. */
final class Slayers {
    private Slayers() {
    }

    static List<ModelSpec> all() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(banshee("banshee", 0xE0E0D8, 0xF0F0F0));
        l.add(spectre("aberrant_spectre", 0x7A8A5A, 0x5A6A3A));
        l.add(abyssalDemon("abyssal_demon"));
        l.add(gargoyle("gargoyle", 0x7A7A78));
        l.add(nechryael("nechryael"));
        l.add(bloodveld("bloodveld", 0xB04848, 0xE88A8A));
        l.add(bloodveld("mutated_bloodveld", 0x7A2A6A, 0xB060A0));
        l.add(caveCrawler("cave_crawler"));
        l.add(crawlingHand("crawling_hand"));
        l.add(cockatrice("cockatrice"));
        l.add(basilisk("basilisk"));
        l.add(kurask("kurask"));
        l.add(turoth("turoth"));
        l.add(jelly("jelly", 0x7AB0C8));
        l.add(Creatures.canine("hellhound", 0x3A1414, 0xF08020, true));
        return l;
    }

    /** Banshee: pale floating woman with long white hair in a tattered white shroud, mouth open in a scream. */
    static ModelSpec banshee(String id, int skin, int robe) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.3f);
        part(b, "body", null, Role.FLOAT, robe).at(0, 10, 0).mat("ghost").box(-3.5f, -10, -2, 7, 10, 4).add();
        part(b, "shroud", "body", Role.STATIC, robe).at(0, 0, 0).rot(6, 0, 0).mat("ghost").box(-4, 0, -2.5f, 8, 6, 5).add();
        part(b, "tatters", "shroud", Role.TAIL, Humanoid.darker(robe)).at(0, 6, 0.5f).rot(10, 0, 0).mat("ghost")
                .box(-3.5f, 0, -2, 2, 4, 4).box(-1, 0, -1.5f, 2, 5, 3).box(1.5f, 0, -2, 2, 3, 4).add();
        part(b, "head", "body", Role.HEAD, skin).accent(0x101010).at(0, -10, 0).mat("ghost").pattern("eyes").box(-3, -6, -3, 6, 6, 6).add();
        part(b, "mouth", "head", Role.JAW, 0x1A1010).at(0, -1.5f, -3).mat("flesh").box(-1, 0, -0.6f, 2, 2, 1).add();
        part(b, "hair", "head", Role.STATIC, 0xF4F4F0).at(0, -6, 0).mat("hair").box(-3.5f, -1, -3.5f, 7, 2, 7).add();
        part(b, "hair_back", "head", Role.STATIC, 0xF4F4F0).at(0, -5, 3).rot(8, 0, 0).mat("hair").box(-3.5f, 0, -0.5f, 7, 13, 2).add();
        sym(b, "hair_side", "head", false, 0xF4F4F0).at(3, -5, 0).mat("hair").box(0, 0, -2.5f, 1, 9, 4).add();
        sym(b, "arm", "body", false, skin).roles(Role.ARM_LEFT, Role.ARM_RIGHT).at(4.5f, -9, 0).rot(-40, 0, -25).mat("ghost")
                .box(-1, -1, -1, 2, 7, 2).add();
        sym(b, "sleeve", "arm", true, robe).at(0, 2, 0).mat("ghost").box(-1.5f, 0, -1.5f, 3, 4, 3).add();
        sym(b, "claws", "arm", true, skin).at(0, 7, 0).mat("horn").box(-1, 0, -1, 2, 2, 2, -0.2f).add();
        return b.build();
    }

    /** Aberrant spectre: a hunched, wispy green spirit with a huge nose and trailing rags. */
    static ModelSpec spectre(String id, int col, int dark) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.4f);
        part(b, "body", null, Role.FLOAT, col).accent(dark).at(0, 10, 0).rot(15, 0, 0).mat("ghost").box(-4, -10, -3, 8, 10, 6).add();
        part(b, "rags", "body", Role.STATIC, dark).at(0, 0, 0).rot(10, 0, 0).mat("ghost").box(-4.5f, 0, -3, 9, 5, 6).add();
        part(b, "rags_low", "rags", Role.TAIL, dark).at(0, 5, 1).rot(15, 0, 0).mat("ghost")
                .box(-4, 0, -2, 3, 5, 4).box(-1, 0, -2, 2, 6, 3).box(1.5f, 0, -2, 3, 4, 4).add();
        part(b, "hood", "body", Role.STATIC, dark).at(0, -10, 1).mat("ghost").box(-4.5f, -2, -2, 9, 3, 6).add();
        part(b, "head", "body", Role.HEAD, col).accent(0xE0F060).at(0, -9, -2).rot(-10, 0, 0).mat("ghost").pattern("eyes").glow("eyes")
                .box(-3.5f, -6, -5, 7, 7, 6).add();
        part(b, "nose", "head", Role.STATIC, col).at(0, -3, -5).rot(25, 0, 0).mat("flesh").box(-1.5f, 0, -3, 3, 5, 3).add();
        part(b, "nose_tip", "nose", Role.STATIC, Humanoid.darker(col)).at(0, 5, -1.5f).mat("flesh").box(-1.5f, 0, -1.5f, 3, 1, 3).add();
        part(b, "jaw", "head", Role.JAW, dark).at(0, 1, -3).mat("ghost").box(-2.5f, 0, -2, 5, 2, 3).add();
        sym(b, "arm", "body", false, col).roles(Role.ARM_LEFT, Role.ARM_RIGHT).at(5, -9, -1).rot(-55, 0, -15).mat("ghost")
                .box(-1, -1, -1, 2, 8, 2).add();
        sym(b, "hand", "arm", true, col).at(0, 8, 0).mat("ghost").box(-1.5f, 0, -1.5f, 3, 2, 3).add();
        for (int k = 0; k < 3; k++) {
            sym(b, "finger" + k, "hand", true, Humanoid.darker(col)).at(-1 + k, 2, -1).rot(-20, 0, 0).mat("horn").box(-0.5f, 0, -0.5f, 1, 3, 1, -0.15f).add();
        }
        return b.build();
    }

    /** Abyssal demon: lanky maroon demon with tentacle-tipped head, long clawed arms and a whip tail. */
    static ModelSpec abyssalDemon(String id) {
        int skin = 0x6A1E2E;
        int dark = 0x341020;
        int purple = 0x5A2A6A;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.7f);
        part(b, "hips", null, Role.BODY, dark).at(0, 12, 1).mat("flesh").box(-3.5f, -2, -2.5f, 7, 4, 5).add();
        part(b, "torso", "hips", Role.STATIC, skin).accent(dark).at(0, -2, 0).rot(30, 0, 0).mat("flesh").box(-4, -10, -3, 8, 10, 6).add();
        part(b, "ribs", "torso", Role.STATIC, dark).at(0, -8, -3).mat("plates").box(-3.5f, 0, -0.75f, 7, 6, 1).add();
        sym(b, "shoulder_spike", "torso", false, 0x2A0A14).at(3.5f, -10, 1).rot(-30, 0, -30).mat("horn").box(-0.5f, -4, -0.5f, 1, 4, 1, 0.2f).add();
        part(b, "head", "torso", Role.HEAD, skin).accent(0xFF5030).at(0, -10, -2).rot(-30, 0, 0).mat("flesh").pattern("eyes").glow("eyes")
                .box(-3, -4, -4, 6, 5, 5).add();
        part(b, "jaw", "head", Role.JAW, dark).at(0, 1, -3).mat("flesh").box(-2.5f, 0, -2, 5, 2, 3).add();
        for (int k = 0; k < 4; k++) {
            float x = -2.25f + k * 1.5f;
            part(b, "tendril" + k, "head", Role.TENTACLE, purple).at(x, -4, -1 + Math.abs(k - 1.5f)).rot(-60 + Math.abs(k - 1.5f) * 10, 0, (k - 1.5f) * 18)
                    .mat("flesh").box(-0.5f, -5, -0.5f, 1, 5, 1, 0.15f).add();
        }
        sym(b, "arm", "torso", false, skin).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(dark).at(5, -9, 0).rot(-20, 0, -12).mat("flesh")
                .box(-1, -1, -1, 2, 9, 2, 0.3f).add();
        sym(b, "forearm", "arm", true, dark).at(0, 8, 0).rot(-30, 0, 0).mat("flesh").box(-1, 0, -1, 2, 8, 2, 0.2f).add();
        sym(b, "hand", "forearm", true, skin).at(0, 8, 0).mat("flesh").box(-1.5f, 0, -1.5f, 3, 2, 3).add();
        for (int k = 0; k < 3; k++) {
            sym(b, "claw" + k, "hand", true, 0x1A0A10).at(-1 + k, 2, -1).rot(-35, 0, 0).mat("horn").box(-0.5f, 0, -0.5f, 1, 4, 1, -0.1f).add();
        }
        sym(b, "thigh", null, false, skin).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).at(2, 12, 1).rot(-25, 0, -5).mat("flesh")
                .box(-1.5f, -1, -1.5f, 3, 6, 3).add();
        sym(b, "shank", "thigh", true, dark).at(0, 5, 0).rot(55, 0, 0).mat("flesh").box(-1, 0, -1, 2, 5, 2, 0.2f).add();
        sym(b, "hock", "shank", true, dark).at(0, 5, 0).rot(-30, 0, 0).mat("flesh").box(-1, 0, -1, 2, 4, 2).add();
        sym(b, "foot", "hock", true, 0x1A0A10).at(0, 3.5f, 0).mat("horn").box(-1.5f, 0, -3, 3, 1, 4, 0.1f).add();
        String prev = "hips";
        for (int i = 0; i < 5; i++) {
            part(b, "tail" + i, prev, Role.TAIL, i < 2 ? skin : purple).at(0, 0, i == 0 ? 2.5f : 4).rot(i == 0 ? 35 : -10, 0, 0)
                    .mat("flesh").box(-1, -1, 0, 2, 2, 4, -0.1f * i).add();
            prev = "tail" + i;
        }
        return b.build();
    }

    /** Gargoyle: hunched stone demon with bat wings, horns and talons. */
    static ModelSpec gargoyle(String id, int stone) {
        ModelSpec.Builder b = Creatures.demonBuilder(id, stone, Humanoid.darker(stone), 0x5A5A58, 1.0f, true);
        return b.build();
    }

    /** Nechryael: a bloated, fleshy flying demon with a huge fanged maw and stubby arms. */
    static ModelSpec nechryael(String id) {
        int flesh = 0xA0483A;
        int dark = 0x6A2A22;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.7f);
        part(b, "body", null, Role.FLOAT, flesh).accent(dark).at(0, 9, 0).mat("flesh").box(-5, -9, -4, 10, 11, 8).add();
        part(b, "gut", "body", Role.STATIC, 0xC07060).at(0, -3, -4).mat("flesh").box(-4, -3, -1, 8, 6, 1).add();
        part(b, "head", "body", Role.HEAD, flesh).accent(0xFFE040).at(0, -9, -1).mat("flesh").pattern("eyes").glow("eyes")
                .box(-4.5f, -7, -5, 9, 7, 8).add();
        part(b, "jaw", "head", Role.JAW, dark).at(0, -1, -4).mat("flesh").box(-4, 0, -2, 8, 3, 5).add();
        sym(b, "tusk", "jaw", false, 0xF0E8C8).at(3, 0, -2).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        sym(b, "horn", "head", false, 0x3A1A14).at(3.5f, -7, 0).rot(-20, 0, -30).mat("horn").box(-1, -3, -1, 2, 3, 2).add();
        sym(b, "arm", "body", false, flesh).roles(Role.ARM_LEFT, Role.ARM_RIGHT).at(5.5f, -7, -1).rot(-40, 0, -20).mat("flesh")
                .box(-1, -1, -1, 2, 6, 2, 0.3f).add();
        sym(b, "claw", "arm", true, 0x3A1A14).at(0, 6, 0).mat("horn").box(-1.5f, 0, -1.5f, 3, 2, 3).add();
        sym(b, "leg", "body", false, dark).at(2.5f, 2, 1).rot(15, 0, -5).mat("flesh").box(-1.5f, 0, -1.5f, 3, 6, 3).add();
        sym(b, "wing", "body", false, dark).roles(Role.WING_LEFT, Role.WING_RIGHT).at(3, -8, 3).rot(-10, -30, -20).mat("horn")
                .box(0, -1, -1, 11, 2, 2).add();
        sym(b, "wing_web", "wing", true, 0x5A2018).at(0, 1, 0).mat("membrane").box(0, 0, -0.5f, 11, 9, 1).add();
        sym(b, "wing_finger", "wing", true, dark).at(11, 0, 0).rot(0, 0, 55).mat("horn").box(-0.5f, 0, -0.5f, 1, 9, 1).add();
        return b.build();
    }

    /** Bloodveld: a huge-mouthed head on two legs with an exposed brain and a long tongue. */
    static ModelSpec bloodveld(String id, int flesh, int brain) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.7f);
        int dark = Humanoid.darker(flesh);
        part(b, "body", null, Role.BODY, flesh).accent(dark).at(0, 11, 0).rot(10, 0, 0).mat("flesh").box(-5, -10, -5, 10, 11, 9).add();
        part(b, "brain", "body", Role.STATIC, brain).at(0, -10, 1).mat("flesh").box(-4, -3, -3, 8, 3, 6).add();
        part(b, "head", "body", Role.HEAD, flesh).accent(0xFFE060).at(0, -6, -5).mat("flesh").pattern("eyes").box(-4.5f, -4, -3, 9, 5, 3).add();
        part(b, "jaw", "body", Role.JAW, dark).at(0, -1, -4).rot(20, 0, 0).mat("flesh").box(-4.5f, 0, -4, 9, 3, 6).add();
        part(b, "tongue", "jaw", Role.TENTACLE, 0xD06070).at(0, 0, -4).rot(30, 0, 0).mat("flesh").box(-1, -0.5f, -6, 2, 1, 6).add();
        part(b, "tongue_tip", "tongue", Role.TENTACLE, 0xD06070).at(0, 0, -6).rot(40, 0, 0).mat("flesh").box(-1, -0.5f, -5, 2, 1, 5, -0.1f).add();
        sym(b, "claw_arm", "body", false, flesh).at(5, -6, -2).rot(-20, 0, -40).mat("flesh").box(-1, -1, -1, 2, 5, 2).add();
        sym(b, "leg", null, false, flesh).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).at(3, 13, 1).rot(-20, 0, -8).mat("flesh")
                .box(-1.5f, -1, -1.5f, 3, 6, 3).add();
        sym(b, "shin", "leg", true, dark).at(0, 5, 0).rot(35, 0, 4).mat("flesh").box(-1, 0, -1, 2, 5, 2).add();
        sym(b, "foot", "shin", true, 0x3A1414).at(0, 4.5f, 0).rot(-15, 0, 0).mat("horn").box(-1.5f, 0, -3, 3, 1, 4).add();
        return b.build();
    }

    /** Dagannoth: upright fish-reptile with a spiked dorsal ridge, small arms, fanged snout and thick tail. */
    static ModelSpec dagannoth(String id, int skin, int belly) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.8f);
        int dark = Humanoid.darker(skin);
        part(b, "hips", null, Role.BODY, skin).accent(belly).at(0, 12, 1).mat("scales").box(-4.5f, -3, -3.5f, 9, 5, 7).add();
        part(b, "torso", "hips", Role.STATIC, skin).accent(belly).at(0, -3, 0).rot(18, 0, 0).mat("scales").box(-4.5f, -11, -3.5f, 9, 11, 7).add();
        part(b, "belly", "torso", Role.STATIC, belly).at(0, -10, -3.5f).mat("plates").box(-3, 0, -1, 6, 11, 1).add();
        for (int i = 0; i < 4; i++) {
            part(b, "fin" + i, "torso", Role.STATIC, 0x8A3A2A).at(0, -11 + i * 3, 3.5f).rot(30, 0, 0).mat("horn")
                    .box(-0.5f, -1, 0, 1, 2, 3 - i / 2).add();
        }
        part(b, "head", "torso", Role.HEAD, skin).accent(0xF0E040).at(0, -11, -1).rot(-15, 0, 0).mat("scales").pattern("eyes")
                .box(-3, -5, -5, 6, 5, 6).add();
        part(b, "snout", "head", Role.STATIC, skin).at(0, -1, -5).mat("scales").box(-2, -2, -3, 4, 3, 3).add();
        part(b, "jaw", "head", Role.JAW, belly).at(0, 0, -4).mat("plates").box(-2, 0, -4, 4, 2, 5).add();
        part(b, "crest", "head", Role.STATIC, 0x8A3A2A).at(0, -5, 0).rot(-20, 0, 0).mat("horn").box(-0.5f, -3, -2, 1, 3, 5).add();
        sym(b, "arm", "torso", false, skin).roles(Role.ARM_LEFT, Role.ARM_RIGHT).at(5, -9, -1).rot(-35, 0, -10).mat("scales")
                .box(-1, -1, -1, 2, 6, 2, 0.3f).add();
        sym(b, "claw", "arm", true, dark).at(0, 6, 0).mat("horn").box(-1, 0, -1.5f, 2, 2, 3).add();
        sym(b, "thigh", null, false, skin).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).at(3, 13, 1).rot(-15, 0, -6).mat("scales")
                .box(-2, -1, -2, 4, 6, 4).add();
        sym(b, "shin", "thigh", true, skin).at(0, 5, 0).rot(25, 0, 5).mat("scales").box(-1.5f, 0, -1.5f, 3, 5, 3).add();
        sym(b, "foot", "shin", true, dark).at(0, 4.5f, 0).rot(-10, 0, 0).mat("horn").box(-2, 0, -3, 4, 1, 4, 0.1f).add();
        String prev = "hips";
        int[] tw = {4, 3, 2, 2};
        for (int i = 0; i < tw.length; i++) {
            part(b, "tail" + i, prev, Role.TAIL, skin).accent(belly).at(0, i == 0 ? 1 : 0, i == 0 ? 3 : 4).rot(i == 0 ? 35 : 10, 0, 0)
                    .mat("scales").box(-tw[i] / 2f, -tw[i] / 2f, 0, tw[i], tw[i], 4).add();
            prev = "tail" + i;
        }
        return b.build();
    }

    /** Kalphite: sandy-yellow insect with a domed carapace, six legs, mandibles, antennae and folded wings. */
    static ModelSpec kalphite(String id, int shell, int dark) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.8f);
        part(b, "thorax", null, Role.BODY, shell).accent(dark).at(0, 15, -2).mat("chitin").box(-3.5f, -3, -4, 7, 6, 7).add();
        part(b, "abdomen", "thorax", Role.STATIC, shell).accent(dark).at(0, -1, 3).rot(-15, 0, 0).mat("chitin").box(-4.5f, -4, 0, 9, 7, 10).add();
        part(b, "shell_ridge", "abdomen", Role.STATIC, dark).at(0, -4, 0).mat("chitin").box(-1, -1, 0, 2, 1, 10).add();
        part(b, "head", "thorax", Role.HEAD, shell).accent(0x30E060).at(0, -1, -4).mat("chitin").pattern("eyes").box(-2.5f, -2.5f, -4, 5, 5, 4).add();
        sym(b, "mandible", "head", false, dark).at(1.5f, 1.5f, -4).rot(0, 25, 0).mat("horn").box(-0.5f, -0.5f, -3, 1, 1, 3, 0.1f).add();
        sym(b, "antenna", "head", false, dark).at(1, -2.5f, -3).rot(-50, -15, 0).mat("horn").box(-0.5f, -6, -0.5f, 1, 6, 1, -0.2f).add();
        sym(b, "wing", "thorax", false, 0xE8E0B0).roles(Role.WING_LEFT, Role.WING_RIGHT).at(2, -3, 1).rot(-8, -12, -6).mat("membrane")
                .box(-1, -0.5f, 0, 3, 1, 11, -0.2f).add();
        for (int i = 0; i < 3; i++) {
            sym(b, "leg" + i, "thorax", false, shell).roles(Role.LEG_EXTRA, Role.LEG_EXTRA).at(3, 1, -2.5f + i * 2.5f).rot(0, -(i - 1) * 25, -40)
                    .mat("chitin").box(0, -1, -1, 5, 2, 2).add();
            sym(b, "shin" + i, "leg" + i, true, dark).at(5, 0, 0).rot(0, 0, 80).mat("chitin").box(0, -0.5f, -0.5f, 7, 1, 1, 0.2f).add();
        }
        return b.build();
    }

    static ModelSpec caveCrawler(String id) {
        int body = 0x6A7A3A;
        int dark = 0x3A4A1A;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.6f);
        part(b, "body", null, Role.BODY, body).accent(dark).at(0, 19, 0).mat("chitin").box(-4, -3, -6, 8, 5, 12).add();
        for (int i = 0; i < 4; i++) {
            part(b, "plate" + i, "body", Role.STATIC, i % 2 == 0 ? body : dark).at(0, -3, -5 + i * 3).rot(-10, 0, 0).mat("chitin")
                    .box(-4.5f, -1, 0, 9, 2, 3).add();
            part(b, "spike" + i, "plate" + i, Role.STATIC, 0xD0C860).at(0, -1, 1.5f).rot(-25, 0, 0).mat("horn").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        }
        part(b, "head", "body", Role.HEAD, body).accent(0xF0F060).at(0, 0, -6).mat("chitin").pattern("eyes").box(-3, -2.5f, -3, 6, 4, 3).add();
        part(b, "tongue", "head", Role.TENTACLE, 0xC06060).at(0, 1, -3).rot(20, 0, 0).mat("flesh").box(-0.5f, -0.5f, -4, 1, 1, 4).add();
        for (int i = 0; i < 4; i++) {
            sym(b, "leg" + i, "body", false, dark).roles(Role.LEG_EXTRA, Role.LEG_EXTRA).at(4, 0, -4.5f + i * 3).rot(0, -(i - 1.5f) * 15, -30)
                    .mat("chitin").box(0, -0.5f, -0.5f, 4, 1, 1, 0.2f).add();
            sym(b, "foot" + i, "leg" + i, true, dark).at(4, 0, 0).rot(0, 0, 70).mat("chitin").box(0, -0.5f, -0.5f, 4, 1, 1).add();
        }
        return b.build();
    }

    static ModelSpec crawlingHand(String id) {
        int skin = 0xC8B090;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.4f);
        part(b, "palm", null, Role.BODY, skin).at(0, 19, 0).mat("skin").box(-3.5f, -1.5f, -3, 7, 3, 6).add();
        part(b, "knuckles", "palm", Role.STATIC, skin).at(0, -1.5f, -3).mat("skin").box(-3.5f, -0.5f, -1, 7, 1, 2).add();
        part(b, "wrist", "palm", Role.STATIC, skin).at(0, 0, 3).mat("skin").box(-2.5f, -1.5f, 0, 5, 3, 3).add();
        part(b, "stump", "wrist", Role.STATIC, 0x8A2020).at(0, 0, 3).mat("flesh").box(-2.5f, -1.5f, 0, 5, 3, 1).add();
        part(b, "bone_end", "stump", Role.STATIC, 0xE8E0C8).at(0, 0, 1).mat("bone").box(-1, -1, 0, 2, 2, 1).add();
        float[] fx = {-2.6f, -0.9f, 0.9f, 2.6f};
        for (int i = 0; i < 4; i++) {
            part(b, "finger" + i, "palm", Role.LEG_EXTRA, skin).at(fx[i], 0, -3).rot(-10, 0, 0).mat("skin").box(-0.5f, -0.5f, -3, 1, 1, 3, 0.15f).add();
            part(b, "fingertip" + i, "finger" + i, Role.STATIC, skin).at(0, 0, -3).rot(60, 0, 0).mat("skin").box(-0.5f, -0.5f, -3, 1, 1, 3, 0.1f).add();
            part(b, "nail" + i, "fingertip" + i, Role.STATIC, 0x8A8A50).at(0, -0.5f, -3).mat("horn").box(-0.5f, -0.25f, -0.5f, 1, 1, 1, -0.2f).add();
        }
        part(b, "thumb", "palm", Role.LEG_EXTRA, skin).at(3.5f, 0, 0).rot(0, -50, 30).mat("skin").box(0, -0.5f, -0.5f, 4, 1, 1, 0.2f).add();
        return b.build();
    }

    static ModelSpec cockatrice(String id) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.5f);
        int body = 0x8A9A3A;
        part(b, "body", null, Role.BODY, body).accent(0x5A6A1A).at(0, 13, 0).rot(-15, 0, 0).mat("scales").box(-4, -4, -5, 8, 8, 10).add();
        part(b, "breast", "body", Role.STATIC, 0xC0B060).at(0, -1, -5).mat("feathers").box(-3, -3, -1, 6, 6, 1).add();
        part(b, "neck", "body", Role.NECK, body).at(0, -3, -5).rot(-25, 0, 0).mat("feathers").box(-1.5f, -6, -2, 3, 7, 3).add();
        part(b, "head", "neck", Role.HEAD, 0xA0B040).accent(0xFF3020).at(0, -6, 0).rot(40, 0, 0).mat("feathers").pattern("eyes").glow("eyes")
                .box(-2.5f, -4, -3, 5, 4, 5).add();
        part(b, "beak", "head", Role.JAW, 0xE0C030).at(0, -2, -3).mat("horn").box(-1, -1, -3, 2, 2, 3).add();
        part(b, "comb", "head", Role.STATIC, 0xC02020).at(0, -4, 0).mat("flesh").box(-0.5f, -3, -2, 1, 3, 5).add();
        part(b, "wattle", "head", Role.STATIC, 0xC02020).at(0, 0, -3).mat("flesh").box(-0.5f, 0, -1, 1, 2, 1).add();
        sym(b, "wing", "body", false, 0x6A7A2A).roles(Role.WING_LEFT, Role.WING_RIGHT).at(4, -3, -3).mat("feathers").box(0, 0, 0, 1, 6, 8).add();
        String prev = "body";
        for (int i = 0; i < 4; i++) {
            part(b, "tail" + i, prev, Role.TAIL, body).accent(0x5A6A1A).at(0, i == 0 ? 1 : 0, i == 0 ? 5 : 4).rot(i == 0 ? 20 : 8, 0, 0)
                    .mat("scales").box(-1.5f + i * 0.25f, -1, 0, 3 - (i / 2), 2, 4).add();
            prev = "tail" + i;
        }
        sym(b, "leg", null, false, 0xE0C030).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).at(2, 16, 1).mat("horn").box(-1, 0, -1, 2, 7, 2).add();
        sym(b, "foot", "leg", true, 0xE0C030).at(0, 7, 0).mat("horn").box(-1.5f, 0, -3, 3, 1, 4).add();
        return b.build();
    }

    static ModelSpec basilisk(String id) {
        int body = 0x5A7A3A;
        int belly = 0xA0C040;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.8f);
        part(b, "body", null, Role.BODY, body).accent(belly).at(0, 17, 0).mat("scales").box(-4.5f, -3.5f, -8, 9, 7, 16).add();
        for (int i = 0; i < 5; i++) {
            part(b, "spine" + i, "body", Role.STATIC, 0xD0D060).at(0, -3.5f, -6 + i * 3).rot(-25, 0, 0).mat("horn").box(-0.5f, -3, -1, 1, 3, 2).add();
        }
        part(b, "head", "body", Role.HEAD, body).accent(0xFFF040).at(0, -1, -8).mat("scales").pattern("eyes").glow("eyes").box(-3.5f, -3, -6, 7, 5, 6).add();
        part(b, "jaw", "head", Role.JAW, belly).at(0, 2, -5).mat("plates").box(-3, 0, -5, 6, 2, 6).add();
        sym(b, "horn", "head", false, 0xD0D060).at(2.5f, -3, -2).rot(-60, 10, -20).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        String prev = "body";
        int[] tw = {5, 4, 3, 2};
        for (int i = 0; i < tw.length; i++) {
            part(b, "tail" + i, prev, Role.TAIL, body).accent(belly).at(0, 0, i == 0 ? 8 : 4).rot(i == 0 ? 5 : 3, 0, 0).mat("scales")
                    .box(-tw[i] / 2f, -tw[i] / 2f, 0, tw[i], tw[i], 4).add();
            prev = "tail" + i;
        }
        for (int i = 0; i < 2; i++) {
            float z = i == 0 ? -5 : 5;
            sym(b, "leg" + i, null, false, body).roles(i == 0 ? Role.LEG_FRONT_LEFT : Role.LEG_BACK_LEFT, i == 0 ? Role.LEG_FRONT_RIGHT : Role.LEG_BACK_RIGHT)
                    .at(4.5f, 18, z).rot(0, 0, -50).mat("scales").box(-1.5f, -1, -1.5f, 3, 4, 3).add();
            sym(b, "foreleg" + i, "leg" + i, true, body).at(0, 3, 0).rot(0, 0, 50).mat("scales").box(-1, 0, -1, 2, 4, 2).add();
            sym(b, "foot" + i, "foreleg" + i, true, 0x3A4A1A).at(0, 3.5f, 0).mat("horn").box(-1.5f, 0, -2.5f, 3, 1, 3).add();
        }
        return b.build();
    }

    /** Kurask: towering green-brown brute with a horned head, spiked shoulders and long arms. */
    static ModelSpec kurask(String id) {
        ModelSpec.Builder b = Humanoid.of(id).trollBody().skin(0x5A7A3A).skinMaterial("scales").face("eyes").hair(0, "none")
                .top(0x5A7A3A, "skin").sleeves("none").bottoms(0x3A3A2A, "loincloth").boots(0x5A7A3A).builder();
        sym(b, "horn", "head", false, 0xD0C8A0).at(3, -6, -1).rot(-10, 0, -30).mat("horn").box(-1, -4, -1, 2, 4, 2).add();
        sym(b, "horn_tip", "horn", true, 0xD0C8A0).at(0, -4, 0).rot(-30, 0, 20).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        for (int i = 0; i < 3; i++) {
            part(b, "back_spike" + i, "torso", Role.STATIC, 0xD0C8A0).at(0, -11 + i * 3.5f, 3.5f).rot(30, 0, 0).mat("horn")
                    .box(-0.5f, -1, 0, 1, 2, 3).add();
        }
        return b.build();
    }

    /** Turoth: gaunt, pale hunched creature with a long tongue-like snout and lanky clawed arms. */
    static ModelSpec turoth(String id) {
        ModelSpec.Builder b = Humanoid.of(id).body(12, 7, 11, 4, 15, 2).head(6, 6, 7).hunch(30).skin(0xB8A07A).face("eyes")
                .hair(0, "none").top(0xB8A07A, "skin").sleeves("none").bottoms(0x6A5A3A, "loincloth").boots(0xB8A07A).arms(-25).builder();
        part(b, "snout", "head", Role.STATIC, 0xB8A07A).at(0, -2, -3.5f).rot(20, 0, 0).mat("flesh").box(-1.5f, -1, -5, 3, 3, 5).add();
        part(b, "tongue", "snout", Role.TENTACLE, 0xC06A6A).at(0, 1.5f, -4).rot(50, 0, 0).mat("flesh").box(-0.5f, 0, -0.5f, 1, 5, 1).add();
        sym(b, "talon", "hand", true, 0x4A3A2A).at(0, 2, -1).rot(-30, 0, 0).mat("horn").box(-0.5f, 0, -0.5f, 1, 3, 1).add();
        return b.build();
    }

    /** Jelly: a wobbling translucent-looking cube of slime with a face and bits floating inside. */
    static ModelSpec jelly(String id, int col) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.7f);
        part(b, "blob", null, Role.BODY, col).accent(Humanoid.darker(col)).at(0, 24, 0).mat("slime").pattern("eyes").box(-6, -12, -6, 12, 12, 12).add();
        part(b, "top", "blob", Role.FLOAT, col).at(0, -12, 0).mat("slime").box(-4, -2, -4, 8, 2, 8).add();
        part(b, "drip", "blob", Role.STATIC, Humanoid.darker(col)).at(0, 0, 0).mat("slime").box(-6.5f, -1, -6.5f, 13, 1, 13).add();
        part(b, "nucleus", "blob", Role.SPIN, 0xE0F0F8).at(2, -5, 2).mat("energy").glow("full").box(-1, -1, -1, 2, 2, 2).add();
        return b.build();
    }
}
