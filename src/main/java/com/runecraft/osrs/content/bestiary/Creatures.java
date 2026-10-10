package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/** Animals, undead and demons: the non-humanoid monsters most players meet first. */
public final class Creatures {
    private Creatures() {
    }

    static List<ModelSpec> all() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(cow("cow"));
        l.add(chicken("chicken"));
        l.add(sheep("sheep"));
        l.add(pig("pig"));
        l.add(rat("giant_rat", 0x6A5A4A, 0xD89090, 1.0f));
        l.add(spider("giant_spider", 0x3A2A1A, 0x8A2A1A, 1.0f));
        l.add(spider("spider", 0x5A4A3A, 0xC08040, 0.55f));
        l.add(spider("poison_spider", 0x3A6A2A, 0xA0D040, 1.0f));
        l.add(canine("wolf", 0x8A8A88, 0xD8D8D0, false));
        l.add(canine("wild_dog", 0x8A6A4A, 0xC8A880, false));
        l.add(canine("catacomb_hellhound", 0x7A1A16, 0xF0A020, true));
        l.add(yak("yak"));
        l.add(cat("blood_jaguar", 0x8A1A1A, 0xE0C040));
        l.add(skeleton("skeleton", 0xE0DCC8, "sword", false));
        l.add(demon("lesser_demon", 0xA8241E, 0x3A1A12, 0xE0D0B0, 1.0f, false));
        l.add(demon("greater_demon", 0x8A1A16, 0x2A0E0A, 0xE0D0B0, 1.0f, true));
        l.add(demon("black_demon", 0x2A2628, 0x141214, 0xA02020, 1.0f, true));
        l.add(demon("catacomb_greater_demon", 0x8A1A16, 0x2A0E0A, 0xE0D0B0, 1.0f, true));
        l.add(demon("catacomb_black_demon", 0x2A2628, 0x141214, 0xA02020, 1.0f, true));
        l.add(demon("reanimated_demon_spawn", 0x5A2A2A, 0x2A1010, 0xA02020, 0.8f, false));
        return l;
    }

    // ------------------------------------------------------------------------------------------
    // Farm animals
    // ------------------------------------------------------------------------------------------

    /** OSRS cow: boxy black-and-white body, big head with stubby horns, pink muzzle and udder. */
    static ModelSpec cow(String id) {
        int white = 0xE8E6DE;
        int black = 0x1E1C1C;
        int pink = 0xE0A0A0;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.7f);
        part(b, "body", null, Role.BODY, white).accent(black).at(0, 11, 1).mat("patches").box(-5, -5, -8, 10, 10, 16).add();
        part(b, "back", "body", Role.STATIC, white).accent(black).at(0, -5, 0).mat("patches").box(-4, -1, -7, 8, 1, 14).add();
        part(b, "udder", "body", Role.STATIC, pink).at(0, 5, 4).mat("skin").box(-2, 0, -2, 4, 2, 4).add();
        sym(b, "teat", "udder", false, pink).at(1, 2, 0).mat("skin").box(-0.5f, 0, -0.5f, 1, 1, 1).add();
        part(b, "neck", "body", Role.NECK, white).accent(black).at(0, -2, -8).mat("patches").box(-3, -3, -3, 6, 6, 3).add();
        part(b, "head", "neck", Role.HEAD, white).accent(black).at(0, -1, -2).mat("patches").pattern("eyes_animal")
                .box(-4, -4, -6, 8, 8, 6).add();
        part(b, "muzzle", "head", Role.STATIC, pink).at(0, 1.5f, -6).mat("skin").box(-3, -1.5f, -2, 6, 4, 2).add();
        sym(b, "nostril", "muzzle", false, 0x8A5050).at(1.5f, 0, -2).mat("skin").box(-0.5f, -0.5f, -0.5f, 1, 1, 1).add();
        sym(b, "horn", "head", false, 0xE0D8B8).at(4, -3.5f, -3).rot(0, 0, -20).mat("horn").box(0, -1, -0.5f, 2, 1, 1).add();
        sym(b, "horn_tip", "horn", true, 0xC8B890).at(2, 0, 0).rot(0, 0, -50).mat("horn").box(0, -1, -0.5f, 1, 1, 1).add();
        sym(b, "ear", "head", false, white).accent(black).at(4, -2, -1.5f).rot(0, 0, 25).mat("patches").box(0, -0.5f, -1, 2, 1, 2).add();
        part(b, "tail", "body", Role.TAIL, white).at(0, -4, 8).rot(10, 0, 0).mat("hide").box(-0.5f, 0, 0, 1, 8, 1).add();
        part(b, "tail_tuft", "tail", Role.STATIC, black).at(0, 8, 0).mat("hair").box(-1, 0, -0.5f, 2, 2, 2).add();
        legs4(b, white, black, 0x3A3030, 3.5f, -5.5f, 6, 8, 4);
        return b.build();
    }

    /** Four legs with a darker hoof/paw at the bottom, feet on the ground (Y=24). */
    static void legs4(ModelSpec.Builder b, int color, int accent, int hoof, float x, float zf, float zb, int len, int w) {
        float top = 24 - len;
        String mat = "patches";
        sym(b, "leg_f", null, false, color).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(accent).at(x, top, zf)
                .mat(mat).box(-w / 2f, 0, -w / 2f, w, len - 1, w).add();
        sym(b, "hoof_f", "leg_f", true, hoof).at(0, len - 1, 0).mat("horn").box(-w / 2f, 0, -w / 2f, w, 1, w, 0.1f).add();
        sym(b, "leg_b", null, false, color).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(accent).at(x, top, zb)
                .mat(mat).box(-w / 2f, 0, -w / 2f, w, len - 1, w).add();
        sym(b, "hoof_b", "leg_b", true, hoof).at(0, len - 1, 0).mat("horn").box(-w / 2f, 0, -w / 2f, w, 1, w, 0.1f).add();
    }

    /** OSRS chicken: plump cream body, brown-tipped wings, red comb and wattle, yellow beak and legs. */
    static ModelSpec chicken(String id) {
        int body = 0xE8DCC0;
        int brown = 0xA07848;
        int red = 0xD02020;
        int yellow = 0xE8B030;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.3f);
        part(b, "body", null, Role.BODY, body).accent(brown).at(0, 17, 0).mat("feathers").box(-3, -3, -4, 6, 6, 8).add();
        part(b, "breast", "body", Role.STATIC, body).at(0, 0, -4).mat("feathers").box(-2.5f, -2, -1, 5, 5, 1).add();
        sym(b, "wing", "body", false, 0xD0B890).roles(Role.WING_LEFT, Role.WING_RIGHT).at(3, -2, -2.5f).mat("feathers")
                .box(0, 0, 0, 1, 4, 6).add();
        part(b, "tail", "body", Role.TAIL, brown).at(0, -2, 3.5f).rot(-40, 0, 0).mat("feathers").box(-2, -4, 0, 4, 4, 2).add();
        part(b, "head", "body", Role.HEAD, body).at(0, -3, -3).mat("feathers").pattern("eyes_animal").box(-2, -5, -2, 4, 5, 3).add();
        part(b, "beak", "head", Role.STATIC, yellow).at(0, -3, -2).mat("horn").box(-1, 0, -2, 2, 1, 2).add();
        part(b, "comb", "head", Role.STATIC, red).at(0, -5, -1).mat("flesh").box(-0.5f, -2, -1, 1, 2, 3).add();
        part(b, "wattle", "head", Role.STATIC, red).at(0, -2, -2).mat("flesh").box(-0.5f, 0, -1, 1, 2, 1).add();
        sym(b, "leg", null, false, yellow).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).at(1.5f, 20, 0.5f).mat("horn")
                .box(-0.5f, 0, -0.5f, 1, 4, 1).add();
        sym(b, "foot", "leg", true, yellow).at(0, 3.5f, 0).mat("horn").box(-1, 0, -2, 2, 1, 3, -0.1f).add();
        return b.build();
    }

    static ModelSpec sheep(String id) {
        int wool = 0xEAE6DA;
        int face = 0xD8D0C0;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.6f);
        part(b, "body", null, Role.BODY, wool).at(0, 13, 1).mat("wool").box(-5, -5, -7, 10, 9, 14, 0.5f).add();
        part(b, "rump", "body", Role.STATIC, wool).at(0, -1, 6).mat("wool").box(-4, -3, 0, 8, 6, 2).add();
        part(b, "head", "body", Role.HEAD, face).at(0, -4, -7).mat("skin").pattern("eyes_animal").box(-2.5f, -3, -5, 5, 6, 5).add();
        part(b, "woolcap", "head", Role.STATIC, wool).at(0, -3, -2).mat("wool").box(-3, -1, -2, 6, 2, 4).add();
        part(b, "nose", "head", Role.STATIC, 0x3A3030).at(0, 1.5f, -5).mat("skin").box(-1.5f, 0, -0.6f, 3, 1, 1).add();
        sym(b, "ear", "head", false, face).at(2.5f, -2, -2).rot(0, 0, 30).mat("skin").box(0, -0.5f, -0.5f, 2, 1, 1).add();
        legs4(b, face, face, 0x2A2420, 3, -4, 5, 7, 3);
        return b.build();
    }

    static ModelSpec pig(String id) {
        int pink = 0xE0A098;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.6f);
        part(b, "body", null, Role.BODY, pink).at(0, 15, 1).mat("skin").box(-5, -4, -8, 10, 8, 15).add();
        part(b, "head", "body", Role.HEAD, pink).at(0, -1, -8).mat("skin").pattern("eyes_animal").box(-4, -4, -6, 8, 7, 6).add();
        part(b, "snout", "head", Role.STATIC, 0xD08880).at(0, 0, -6).mat("skin").box(-2, -1, -1, 4, 3, 1).add();
        sym(b, "ear", "head", false, pink).at(3, -4, -3).rot(-20, 0, -20).mat("skin").box(-1, -2, -1, 2, 2, 1).add();
        part(b, "tail", "body", Role.TAIL, pink).at(0, -3, 7).rot(-40, 0, 0).mat("skin").box(-0.5f, -2, 0, 1, 2, 1).add();
        legs4(b, pink, pink, 0x6A4A40, 3, -5, 5, 5, 3);
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Vermin and beasts
    // ------------------------------------------------------------------------------------------

    /** Giant rat: hunched furry body, pointed snout with whiskers and teeth, pink ears, feet and long tail. */
    static ModelSpec rat(String id, int fur, int pink, float scale) {
        ModelSpec.Builder b = ModelSpec.builder(id).scale(scale).shadow(0.5f);
        part(b, "body", null, Role.BODY, fur).at(0, 19, 1).mat("fur").box(-3.5f, -3.5f, -5, 7, 6, 10).add();
        part(b, "haunch", "body", Role.STATIC, fur).at(0, -1, 4).mat("fur").box(-4, -3, -1, 8, 6, 4).add();
        part(b, "head", "body", Role.HEAD, fur).at(0, -1, -5).mat("fur").pattern("eyes").box(-2.5f, -2.5f, -4, 5, 5, 4).add();
        part(b, "snout", "head", Role.STATIC, fur).at(0, 0.5f, -4).mat("fur").box(-1.5f, -1.5f, -3, 3, 3, 3).add();
        part(b, "nose", "snout", Role.STATIC, pink).at(0, -0.5f, -3).mat("skin").box(-0.5f, -0.5f, -0.6f, 1, 1, 1).add();
        part(b, "teeth", "snout", Role.STATIC, 0xF0E8C8).at(0, 1.5f, -2.6f).mat("bone").box(-0.5f, 0, -0.5f, 1, 1, 1).add();
        sym(b, "whisker", "snout", false, 0x2A2A2A).at(1.5f, 0, -2).rot(0, -20, 0).mat("hair").box(0, -0.25f, -0.25f, 3, 1, 1, -0.38f).add();
        sym(b, "ear", "head", false, pink).at(2, -2.5f, -1).rot(0, -20, -15).mat("skin").box(-1, -2, -0.5f, 2, 2, 1).add();
        sym(b, "leg_f", null, false, fur).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).at(2.5f, 21, -3).mat("fur")
                .box(-1, 0, -1, 2, 2, 2).add();
        sym(b, "paw_f", "leg_f", true, pink).at(0, 2, 0).mat("skin").box(-1, 0, -1.5f, 2, 1, 2).add();
        sym(b, "leg_b", null, false, fur).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).at(3, 20, 4).mat("fur")
                .box(-1, 0, -1.5f, 2, 3, 3).add();
        sym(b, "paw_b", "leg_b", true, pink).at(0, 3, 0).mat("skin").box(-1, 0, -2, 2, 1, 3).add();
        String prev = "body";
        for (int i = 0; i < 4; i++) {
            part(b, "tail" + i, prev, Role.TAIL, pink).at(0, i == 0 ? -1 : 0, i == 0 ? 7 : 4).rot(i == 0 ? 25 : 8, 0, 0)
                    .mat("skin").box(-0.5f, -0.5f, 0, 1, 1, 4, i == 0 ? 0.15f : -0.05f * i).add();
            prev = "tail" + i;
        }
        return b.build();
    }

    /** Spider: small cephalothorax with fangs and a cluster of eyes, a big patterned abdomen, 8 jointed legs. */
    static ModelSpec spider(String id, int body, int mark, float scale) {
        ModelSpec.Builder b = ModelSpec.builder(id).scale(scale).shadow(0.8f * scale);
        part(b, "thorax", null, Role.BODY, body).accent(mark).at(0, 17, -2).mat("fur").box(-3.5f, -2.5f, -4, 7, 5, 7).add();
        part(b, "abdomen", "thorax", Role.STATIC, body).accent(mark).at(0, -1, 3).rot(-12, 0, 0).mat("fur").pattern("stripes")
                .box(-5, -4, 0, 10, 8, 11).add();
        part(b, "spinnerets", "abdomen", Role.STATIC, mark).at(0, 1, 11).mat("chitin").box(-1, -1, 0, 2, 2, 1).add();
        part(b, "head", "thorax", Role.HEAD, body).accent(0xE02020).at(0, 0, -4).mat("chitin").pattern("eyes").glow("eyes")
                .box(-2.5f, -2, -3, 5, 4, 3).add();
        sym(b, "fang", "head", false, 0x1A1410).at(1.2f, 1.5f, -3).rot(-20, 0, 10).mat("horn").box(-0.5f, 0, -1, 1, 3, 1).add();
        sym(b, "palp", "head", false, body).at(2, 1, -2.5f).rot(-30, -20, 0).mat("fur").box(-0.5f, 0, -2, 1, 1, 2).add();
        for (int i = 0; i < 4; i++) {
            float z = -3 + i * 1.8f;
            float yaw = (i - 1.5f) * 24f;
            sym(b, "leg" + i, "thorax", false, body).roles(Role.LEG_EXTRA, Role.LEG_EXTRA).at(3, 0, z).rot(0, -yaw, -38)
                    .mat("fur").box(0, -1, -1, 7, 2, 2).add();
            sym(b, "shin" + i, "leg" + i, true, body).accent(mark).at(7, 0, 0).rot(0, 0, 78).mat("fur")
                    .box(0, -0.5f, -0.5f, 9, 1, 1, 0.2f).add();
        }
        return b.build();
    }

    /** Wolves, dogs and hellhounds. */
    static ModelSpec canine(String id, int fur, int light, boolean hell) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.6f);
        String m = hell ? "rock" : "fur";
        part(b, "body", null, Role.BODY, fur).accent(light).at(0, 13, 1).mat(m).glow(hell ? "cracks" : null)
                .box(-3.5f, -3.5f, -7, 7, 7, 13).add();
        part(b, "chest", "body", Role.STATIC, light).at(0, 0, -7).mat("fur").box(-3, -3, -1, 6, 6, 2).add();
        part(b, "mane", "body", Role.STATIC, fur).at(0, -3.5f, -5).mat("fur").box(-4, -1, -3, 8, 3, 6).add();
        part(b, "neck", "body", Role.NECK, fur).at(0, -2, -7).rot(-25, 0, 0).mat(m).box(-2.5f, -2.5f, -4, 5, 5, 4).add();
        part(b, "head", "neck", Role.HEAD, fur).accent(hell ? 0xFFB020 : light).at(0, -1, -3.5f).rot(25, 0, 0).mat(m)
                .pattern("eyes").glow(hell ? "eyes" : null).box(-3, -3, -4, 6, 5, 4).add();
        part(b, "snout", "head", Role.STATIC, hell ? fur : light).at(0, 0, -4).mat("fur").box(-1.5f, -1, -4, 3, 3, 4).add();
        part(b, "jaw", "head", Role.JAW, fur).at(0, 2, -4).mat("fur").glow(hell ? "maw" : null).box(-1.5f, 0, -3.5f, 3, 1, 4).add();
        part(b, "nose", "snout", Role.STATIC, 0x1A1A1A).at(0, -1, -4).mat("skin").box(-0.5f, 0, -0.6f, 1, 1, 1).add();
        sym(b, "ear", "head", false, fur).at(2, -3, -1).rot(-10, 0, -10).mat("fur").box(-1, -3, -0.5f, 2, 3, 1).add();
        if (hell) {
            sym(b, "spike", "body", false, 0x2A0A08).at(2, -3.5f, -2).rot(-20, 0, -20).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
            sym(b, "spike_b", "body", false, 0x2A0A08).at(1.5f, -3.5f, 2).rot(-30, 0, -15).mat("horn").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        }
        String prev = "body";
        for (int i = 0; i < 3; i++) {
            part(b, "tail" + i, prev, Role.TAIL, i == 2 && !hell ? light : fur).at(0, i == 0 ? -2.5f : 0, i == 0 ? 6 : 3)
                    .rot(i == 0 ? -35 : 15, 0, 0).mat("fur").box(-1, -1, 0, 2, 2, 3, i == 2 ? 0.2f : 0f).add();
            prev = "tail" + i;
        }
        sym(b, "leg_f", null, false, fur).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).at(2.2f, 15, -5).mat(m)
                .box(-1, 0, -1, 2, 8, 2, 0.2f).add();
        sym(b, "paw_f", "leg_f", true, hell ? 0x2A0A08 : light).at(0, 8, 0).mat("fur").box(-1, 0, -1.5f, 2, 1, 3, 0.1f).add();
        sym(b, "thigh_b", null, false, fur).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).at(2.2f, 13, 5).rot(15, 0, 0).mat(m)
                .box(-1.5f, -1, -1.5f, 3, 5, 3).add();
        sym(b, "hock_b", "thigh_b", true, fur).at(0, 4, 0).rot(-15, 0, 0).mat(m).box(-1, 0, -1, 2, 6, 2, 0.1f).add();
        sym(b, "paw_b", "hock_b", true, hell ? 0x2A0A08 : light).at(0, 6, 0).mat("fur").box(-1, 0, -1.5f, 2, 1, 3, 0.1f).add();
        return b.build();
    }

    static ModelSpec yak(String id) {
        int fur = 0x5A4A3A;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.8f);
        part(b, "body", null, Role.BODY, fur).at(0, 11, 1).mat("fur").box(-5, -5, -8, 10, 10, 16, 0.3f).add();
        part(b, "hump", "body", Role.STATIC, fur).at(0, -5, -5).mat("fur").box(-4, -2, -2, 8, 3, 6).add();
        part(b, "skirt", "body", Role.STATIC, 0x4A3A2A).at(0, 5, 0).mat("fur").box(-5.5f, 0, -7, 11, 3, 15).add();
        part(b, "head", "body", Role.HEAD, fur).at(0, -1, -8).mat("fur").pattern("eyes_animal").box(-3, -3, -5, 6, 7, 5).add();
        part(b, "muzzle", "head", Role.STATIC, 0x3A2E24).at(0, 2, -5).mat("skin").box(-2, -1, -1, 4, 3, 1).add();
        sym(b, "horn", "head", false, 0xD8CCA8).at(3, -2, -2).rot(0, 0, -10).mat("horn").box(0, -1, -1, 3, 2, 2).add();
        sym(b, "horn_tip", "horn", true, 0xC0B088).at(3, 0, 0).rot(0, 0, -60).mat("horn").box(0, -0.5f, -0.5f, 3, 1, 1).add();
        legs4(b, fur, fur, 0x2A2018, 3.5f, -5, 5, 8, 3);
        return b.build();
    }

    static ModelSpec cat(String id, int fur, int spot) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.7f);
        part(b, "body", null, Role.BODY, fur).accent(spot).at(0, 14, 1).mat("fur").pattern("spots").box(-3.5f, -3, -8, 7, 6, 15).add();
        part(b, "head", "body", Role.HEAD, fur).accent(0xF0D040).at(0, -2, -8).mat("fur").pattern("eyes").box(-3, -3, -4, 6, 5, 4).add();
        part(b, "muzzle", "head", Role.STATIC, fur).at(0, 1, -4).mat("fur").box(-2, -1, -2, 4, 2, 2).add();
        part(b, "jaw", "head", Role.JAW, fur).at(0, 1.5f, -3).mat("fur").box(-1.5f, 0, -2.5f, 3, 1, 3).add();
        sym(b, "ear", "head", false, fur).at(2, -3, -1).mat("fur").box(-1, -2, -0.5f, 2, 2, 1).add();
        String prev = "body";
        for (int i = 0; i < 4; i++) {
            part(b, "tail" + i, prev, Role.TAIL, fur).accent(spot).at(0, i == 0 ? -2 : 0, i == 0 ? 7 : 3).rot(i == 0 ? -20 : 12, 0, 0)
                    .mat("fur").box(-1, -1, 0, 2, 2, 3).add();
            prev = "tail" + i;
        }
        sym(b, "leg_f", null, false, fur).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(spot).at(2.2f, 16, -6).mat("fur")
                .box(-1.5f, 0, -1.5f, 3, 8, 3).add();
        sym(b, "leg_b", null, false, fur).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(spot).at(2.2f, 15, 5).mat("fur")
                .box(-1.5f, 0, -1.5f, 3, 9, 3).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Undead
    // ------------------------------------------------------------------------------------------

    /** OSRS skeleton: ivory bones with a hollow rib cage, dark-socketed skull, optional sword and shield. */
    public static ModelSpec skeleton(String id, int bone, String weapon, boolean glowEyes) {
        return skeletonBuilder(id, bone, weapon, glowEyes).build();
    }

    public static ModelSpec.Builder skeletonBuilder(String id, int bone, String weapon, boolean glowEyes) {
        int dark = Humanoid.darker(bone);
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.5f);
        part(b, "pelvis", null, Role.BODY, bone).at(0, 12, 0).mat("bone").box(-3, -1, -1.5f, 6, 2, 3).add();
        part(b, "spine", "pelvis", Role.STATIC, dark).at(0, -1, 1).rot(6, 0, 0).mat("bone").box(-0.5f, -10, -0.5f, 1, 10, 1, 0.2f).add();
        for (int i = 0; i < 4; i++) {
            part(b, "rib" + i, "spine", Role.STATIC, bone).at(0, -9 + i * 2, -1.5f).mat("bone")
                    .box(-3.5f + i * 0.25f, 0, -2, 7 - (i / 2), 1, 4).add();
        }
        part(b, "sternum", "spine", Role.STATIC, bone).at(0, -9, -3.5f).mat("bone").box(-0.5f, 0, -0.5f, 1, 6, 1).add();
        part(b, "collar", "spine", Role.STATIC, bone).at(0, -10, -0.5f).mat("bone").box(-4.5f, -0.5f, -0.5f, 9, 1, 1).add();
        part(b, "skull", "spine", Role.HEAD, bone).accent(0xFF4020).at(0, -10.5f, -0.5f).mat("bone").pattern("skull")
                .glow(glowEyes ? "eyes" : null).box(-3, -6, -3.5f, 6, 6, 6).add();
        part(b, "skull_jaw", "skull", Role.JAW, bone).at(0, -0.5f, -1).mat("bone").box(-2, 0, -2.5f, 4, 2, 4).add();
        sym(b, "arm", "spine", false, bone).roles(Role.ARM_LEFT, Role.ARM_RIGHT).at(4.5f, -9.5f, -0.5f).rot(0, 0, -6).mat("bone")
                .box(-0.5f, 0, -0.5f, 1, 5, 1, 0.25f).add();
        sym(b, "elbow", "arm", true, bone).at(0, 5, 0).mat("bone").box(-0.75f, -0.5f, -0.75f, 1, 1, 1, 0.3f).add();
        sym(b, "forearm", "arm", true, bone).at(0, 5, 0).rot(-12, 0, 0).mat("bone").box(-0.5f, 0, -0.5f, 1, 5, 1, 0.15f).add();
        sym(b, "hand", "forearm", true, bone).at(0, 5, 0).mat("bone").box(-1, 0, -1, 2, 2, 2, -0.2f).add();
        sym(b, "femur", null, false, bone).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).at(2, 12, 0).mat("bone")
                .box(-0.5f, 0, -0.5f, 1, 6, 1, 0.3f).add();
        sym(b, "knee", "femur", true, bone).at(0, 6, 0).mat("bone").box(-1, -0.5f, -1, 2, 1, 2).add();
        sym(b, "tibia", "femur", true, bone).at(0, 6, 0).mat("bone").box(-0.5f, 0, -0.5f, 1, 5, 1, 0.2f).add();
        sym(b, "foot", "tibia", true, bone).at(0, 5, 0).mat("bone").box(-1, 0, -2.5f, 2, 1, 3).add();
        if ("sword".equals(weapon)) {
            part(b, "w_grip", "hand_r", Role.STATIC, Humanoid.DARK_LEATHER).at(0, 1, 0).mat("leather").box(-0.5f, -0.5f, -1, 1, 1, 2).add();
            part(b, "w_guard", "w_grip", Role.STATIC, 0x5A5A5A).at(0, 0, -1).mat("metal").box(-1.5f, -0.5f, -1, 3, 1, 1).add();
            part(b, "w_blade", "w_guard", Role.STATIC, 0x8A8A8A).at(0, 0, -1).mat("metal").box(-0.5f, -0.5f, -8, 1, 1, 8).add();
            part(b, "shield", "forearm_l", Role.STATIC, 0x6A4A2A).at(1, 2, 0).mat("wood").box(0, -3, -3, 1, 7, 6).add();
            part(b, "shield_rim", "shield", Role.STATIC, 0x5A5A5A).at(1, 0, 0).mat("metal").box(0, -3, -0.5f, 1, 7, 1).box(0, 0, -3, 1, 1, 6).add();
        } else if ("bow".equals(weapon)) {
            part(b, "w_bow", "hand_l", Role.STATIC, 0x6A4A2A).at(0, 1, 0).mat("wood").box(-0.5f, -6, -0.5f, 1, 12, 1).add();
        }
        return b;
    }

    /** OSRS ghost: a hooded, legless white spectre trailing off into wisps, arms reaching. */
    public static ModelSpec ghost(String id, int col) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.3f);
        int dim = Humanoid.darker(col);
        part(b, "body", null, Role.FLOAT, col).at(0, 10, 0).mat("ghost").glow("full").box(-4, -10, -2.5f, 8, 10, 5).add();
        part(b, "robe", "body", Role.STATIC, col).at(0, 0, 0).rot(8, 0, 0).mat("ghost").glow("full").box(-3.5f, 0, -2, 7, 5, 4).add();
        part(b, "robe_low", "robe", Role.STATIC, dim).at(0, 5, 0.5f).rot(12, 0, 0).mat("ghost").glow("full").box(-2.5f, 0, -1.5f, 5, 4, 3).add();
        part(b, "wisp", "robe_low", Role.TAIL, dim).at(0, 4, 0.5f).rot(20, 0, 0).mat("ghost").glow("full").box(-1, 0, -1, 2, 4, 2).add();
        part(b, "hood", "body", Role.HEAD, col).accent(0x60C0FF).at(0, -10, 0).mat("ghost").glow("eyes").pattern("eyes")
                .box(-3.5f, -7, -3.5f, 7, 7, 7).add();
        part(b, "hood_tip", "hood", Role.STATIC, col).at(0, -6, 3).rot(30, 0, 0).mat("ghost").glow("full").box(-2, -1, 0, 4, 4, 2).add();
        sym(b, "arm", "body", false, col).roles(Role.ARM_LEFT, Role.ARM_RIGHT).at(5, -9, 0).rot(-70, 0, -8).mat("ghost").glow("full")
                .box(-1.5f, -1, -1.5f, 3, 6, 3).add();
        sym(b, "sleeve", "arm", true, dim).at(0, 5, 0).mat("ghost").glow("full").box(-2, 0, -2, 4, 3, 4).add();
        sym(b, "hand", "sleeve", true, col).at(0, 3, 0).mat("ghost").box(-1, 0, -1, 2, 2, 2).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Demons
    // ------------------------------------------------------------------------------------------

    /**
     * OSRS demon: hunched, muscular, red (lesser/greater) or black, with backswept horns, bat wings, a whip
     * tail ending in a spade, clawed hands and digitigrade legs on hooves.
     */
    public static ModelSpec demon(String id, int skin, int dark, int horn, float scale, boolean bigWings) {
        return demonBuilder(id, skin, dark, horn, scale, bigWings).build();
    }

    public static ModelSpec.Builder demonBuilder(String id, int skin, int dark, int horn, float scale, boolean bigWings) {
        ModelSpec.Builder b = ModelSpec.builder(id).scale(scale).shadow(0.8f);
        int mid = Humanoid.darker(skin);
        part(b, "hips", null, Role.BODY, mid).accent(dark).at(0, 12, 1).mat("hide").box(-4, -2, -3, 8, 4, 6).add();
        part(b, "loin", "hips", Role.STATIC, dark).at(0, 1, -3).mat("leather").box(-2, 0, -0.6f, 4, 4, 1).add();
        part(b, "torso", "hips", Role.STATIC, skin).accent(dark).at(0, -2, 0).rot(20, 0, 0).mat("hide").box(-5, -10, -3.5f, 10, 10, 7).add();
        part(b, "chest", "torso", Role.STATIC, skin).accent(dark).at(0, -9, -3.5f).mat("hide").box(-4.5f, 0, -1, 9, 5, 1).add();
        part(b, "abs", "torso", Role.STATIC, mid).at(0, -4, -3.5f).mat("plates").box(-3, 0, -0.75f, 6, 4, 1).add();
        sym(b, "shoulder", "torso", false, skin).accent(dark).at(5, -9, 0).rot(0, 0, -15).mat("hide").box(-2, -2, -2.5f, 5, 4, 5).add();
        part(b, "spines", "torso", Role.STATIC, horn).at(0, -10, 3).mat("horn").box(-0.5f, -1, -2, 1, 1, 1).box(-0.5f, 2, 0, 1, 1, 1)
                .box(-0.5f, 5, 0.5f, 1, 1, 1).add();
        part(b, "head", "torso", Role.HEAD, skin).accent(0xFFC020).at(0, -10, -2).rot(-18, 0, 0).mat("hide").pattern("eyes").glow("eyes")
                .box(-3, -5, -4, 6, 6, 5).add();
        part(b, "brow", "head", Role.STATIC, mid).at(0, -4, -4).rot(10, 0, 0).mat("hide").box(-3.5f, -1, -1, 7, 2, 2).add();
        part(b, "snout", "head", Role.STATIC, mid).at(0, -1, -4).mat("hide").box(-2, -1, -2, 4, 3, 2).add();
        part(b, "jaw", "head", Role.JAW, dark).at(0, 1, -3).mat("hide").box(-2.5f, 0, -3, 5, 2, 4).add();
        sym(b, "fang", "jaw", false, 0xF0E8D0).at(1.5f, 0, -3).mat("horn").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        sym(b, "ear", "head", false, skin).at(3, -3, -1).rot(0, -30, -20).mat("hide").box(0, -1, -0.5f, 2, 2, 1).add();
        sym(b, "horn", "head", false, horn).at(2.5f, -5, -1.5f).rot(-45, 0, -25).mat("horn").box(-1, -3, -1, 2, 3, 2).add();
        sym(b, "horn_mid", "horn", true, horn).at(0, -3, 0).rot(-40, 0, 5).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1, 0.15f).add();
        sym(b, "horn_tip", "horn_mid", true, Humanoid.darker(horn)).at(0, -3, 0).rot(-30, 0, 0).mat("horn").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        // arms with claws
        sym(b, "arm", "torso", false, skin).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(dark).at(6.5f, -8, 0).rot(-15, 0, -10)
                .mat("hide").box(-1.5f, -1, -1.5f, 3, 7, 3, 0.3f).add();
        sym(b, "forearm", "arm", true, mid).accent(dark).at(0, 6, 0).rot(-25, 0, 5).mat("hide").box(-1.5f, 0, -1.5f, 3, 6, 3, 0.2f).add();
        sym(b, "hand", "forearm", true, skin).at(0, 6, 0).mat("hide").box(-1.5f, 0, -1.5f, 3, 2, 3).add();
        for (int k = 0; k < 3; k++) {
            sym(b, "claw" + k, "hand", true, horn).at(-1 + k, 2, -1).rot(-25, 0, 0).mat("horn").box(-0.5f, 0, -0.5f, 1, 2, 1, -0.1f).add();
        }
        // digitigrade legs
        sym(b, "thigh", null, false, skin).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(dark).at(2.5f, 12, 1)
                .rot(-30, 0, -6).mat("hide").box(-2, -1, -2, 4, 6, 4).add();
        sym(b, "shank", "thigh", true, mid).at(0, 5, 0).rot(60, 0, 3).mat("hide").box(-1.5f, 0, -1.5f, 3, 5, 3).add();
        sym(b, "hock", "shank", true, mid).at(0, 5, 0).rot(-30, 0, 2).mat("hide").box(-1, 0, -1, 2, 4, 2, 0.1f).add();
        sym(b, "hoof", "hock", true, 0x1A1212).at(0, 3.5f, -0.5f).rot(0, 0, 0).mat("horn").box(-1.5f, 0, -2, 3, 2, 3).add();
        // tail
        String prev = "hips";
        for (int i = 0; i < 4; i++) {
            part(b, "tail" + i, prev, Role.TAIL, i == 0 ? mid : dark).at(0, i == 0 ? 0 : 0, i == 0 ? 3 : 4).rot(i == 0 ? 30 : -12, 0, 0)
                    .mat("hide").box(-1, -1, 0, 2, 2, 4, i == 0 ? 0 : -0.1f * i).add();
            prev = "tail" + i;
        }
        part(b, "tail_spade", prev, Role.STATIC, horn).at(0, 0, 4).mat("horn").box(-1.5f, -0.5f, 0, 3, 1, 2).add();
        // wings
        int span = bigWings ? 12 : 7;
        int wh = bigWings ? 10 : 6;
        sym(b, "wing", "torso", false, dark).roles(Role.WING_LEFT, Role.WING_RIGHT).accent(horn).at(2.5f, -9, 3.5f).rot(-10, -30, -20)
                .mat("hide").box(0, -1, -1, span, 2, 2).add();
        sym(b, "wing_web", "wing", true, Humanoid.darker(dark)).at(0, 1, 0).mat("membrane").box(0, 0, -0.5f, span, wh, 1).add();
        sym(b, "wing_finger", "wing", true, dark).at(span, 0, 0).rot(0, 0, 50).mat("horn").box(-0.5f, 0, -0.5f, 1, wh, 1).add();
        return b;
    }
}
