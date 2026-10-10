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
