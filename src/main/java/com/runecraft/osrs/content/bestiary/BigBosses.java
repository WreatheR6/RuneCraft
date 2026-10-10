package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/** Re-sculpts of the wilderness, slayer and solo bosses (same ids as their original models). */
final class BigBosses {
    private BigBosses() {
    }

    static List<ModelSpec> overrides() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(giantMole());
        l.add(kalphiteQueen());
        l.add(cerberus());
        l.add(bear("callisto", 0x5A3A22, 0x3A2616));
        l.add(bear("artio", 0x6A4A2E, 0x4A321E));
        l.add(Creatures.spider("venenatis", 0x2A1A2E, 0x8A2A6A, 1.0f));
        l.add(Creatures.spider("spindel", 0x3A2238, 0x9A3A7A, 1.0f));
        l.add(vetion("vetion", 0x5A2A8A));
        l.add(vetion("calvarion", 0x3A6A4A));
        l.add(scorpion("scorpia", 0x2A2A2E, 0x6A4A2A, 0xC04020));
        l.add(scorpion("scorpion", 0x2A2A2A, 0x4A3A2A, 0xC04020));
        l.add(kraken("kraken", 0x8A3A5A, 0xD07090));
        l.add(Slayers.dagannoth("dagannoth_rex", 0x5A6A48, 0x9A9A6A));
        l.add(Slayers.dagannoth("dagannoth_prime", 0x5A7A8A, 0x9AB0B8));
        l.add(Slayers.dagannoth("dagannoth_supreme", 0x7A5A3A, 0xB09A70));
        l.add(nex());
        l.add(Fallbacks.crab("sand_crab", 0xC8A870, 0xA08050, 0xB89058, false, false));
        return l;
    }

    /** Giant Mole: a huge dark-brown mole with a fleshy pink star of a snout and enormous pink digging claws. */
    static ModelSpec giantMole() {
        int fur = 0x4A3426;
        int pink = 0xE0A0A0;
        ModelSpec.Builder b = ModelSpec.builder("giant_mole").shadow(1.2f);
        part(b, "body", null, Role.BODY, fur).at(0, 16, 2).mat("fur").box(-7, -6, -10, 14, 12, 20, 0.3f).add();
        part(b, "rump", "body", Role.STATIC, fur).at(0, -1, 9).mat("fur").box(-6, -5, 0, 12, 10, 3).add();
        part(b, "head", "body", Role.HEAD, fur).at(0, 0, -10).mat("fur").pattern("eyes_animal").box(-5, -5, -5, 10, 9, 5).add();
        part(b, "snout", "head", Role.STATIC, pink).at(0, 0, -5).mat("flesh").box(-2.5f, -2, -3, 5, 4, 3).add();
        for (int k = 0; k < 6; k++) {
            double a = Math.toRadians(k * 60);
            part(b, "tendril" + k, "snout", Role.TENTACLE, pink).at((float) Math.cos(a) * 2, (float) Math.sin(a) * 1.6f, -3)
                    .rot((float) Math.sin(a) * 30, (float) -Math.cos(a) * 30, 0).mat("flesh").box(-0.5f, -0.5f, -2, 1, 1, 2).add();
        }
        part(b, "teeth", "head", Role.JAW, 0xF0E8D0).at(0, 2.5f, -5).mat("bone").box(-2, 0, -1, 4, 1, 1).add();
        sym(b, "paw", "body", false, pink).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).at(7, 2, -7).rot(-10, -25, -30).mat("flesh")
                .box(-1, -1, -4, 5, 2, 6).add();
        for (int k = 0; k < 4; k++) {
            sym(b, "claw" + k, "paw", true, 0xE8E0C8).at(4, 0, -3 + k * 1.5f).rot(0, -10, 0).mat("horn").box(0, -0.5f, -0.5f, 3, 1, 1).add();
        }
        sym(b, "hind", null, false, fur).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).at(5, 20, 8).mat("fur").box(-2, 0, -2, 4, 4, 4).add();
        sym(b, "hind_paw", "hind", true, pink).at(0, 3, 0).mat("flesh").box(-2, 0, -3, 4, 1, 4).add();
        part(b, "tail", "rump", Role.TAIL, pink).at(0, 1, 3).rot(20, 0, 0).mat("flesh").box(-0.5f, -0.5f, 0, 1, 1, 4).add();
        return b.build();
    }

    /** Kalphite Queen (first form): a great armoured insect with a swollen abdomen, scything forelegs and wings. */
    static ModelSpec kalphiteQueen() {
        int shell = 0xA88A3A;
        int dark = 0x6A5420;
        ModelSpec.Builder b = ModelSpec.builder("kalphite_queen").shadow(1.4f);
        part(b, "thorax", null, Role.BODY, shell).accent(dark).at(0, 12, -2).rot(-25, 0, 0).mat("chitin").box(-5, -6, -5, 10, 10, 9).add();
        part(b, "abdomen", "thorax", Role.STATIC, shell).accent(dark).at(0, 2, 4).rot(35, 0, 0).mat("chitin").box(-7, -6, 0, 14, 11, 15).add();
        for (int i = 0; i < 4; i++) {
            part(b, "band" + i, "abdomen", Role.STATIC, dark).at(0, -6, 2 + i * 3.5f).mat("chitin").box(-6, -0.5f, 0, 12, 1, 1).add();
        }
        part(b, "head", "thorax", Role.HEAD, shell).accent(0x40E060).at(0, -6, -3).rot(20, 0, 0).mat("chitin").pattern("eyes").glow("eyes")
                .box(-3.5f, -5, -5, 7, 6, 6).add();
        part(b, "crest", "head", Role.STATIC, dark).at(0, -5, 0).rot(-30, 0, 0).mat("chitin").box(-3, -4, -1, 6, 4, 3).add();
        sym(b, "mandible", "head", false, dark).at(2, 0.5f, -5).rot(0, 25, 0).mat("horn").box(-0.5f, -0.5f, -4, 1, 1, 4, 0.2f).add();
        sym(b, "antenna", "head", false, dark).at(1.5f, -5, -3).rot(-40, -15, 0).mat("horn").box(-0.5f, -7, -0.5f, 1, 7, 1, -0.1f).add();
        sym(b, "scythe", "thorax", false, shell).roles(Role.ARM_LEFT, Role.ARM_RIGHT).at(5, -3, -4).rot(-40, 0, -20).mat("chitin")
                .box(-1, -1, -1, 2, 8, 2).add();
        sym(b, "scythe_blade", "scythe", true, dark).at(0, 8, 0).rot(-70, 0, 0).mat("horn").box(-0.5f, 0, -1, 1, 9, 2, 0.1f).add();
        sym(b, "wing", "thorax", false, 0xE8E0B0).roles(Role.WING_LEFT, Role.WING_RIGHT).at(3, -6, 2).rot(-20, -15, -10).mat("membrane")
                .box(-1, -0.5f, 0, 4, 1, 16, -0.2f).add();
        for (int i = 0; i < 3; i++) {
            sym(b, "leg" + i, "thorax", false, shell).roles(Role.LEG_EXTRA, Role.LEG_EXTRA).at(4.5f, 2, -2 + i * 3).rot(25, -(i - 1) * 25, -45)
                    .mat("chitin").box(0, -1, -1, 7, 2, 2).add();
            sym(b, "shin" + i, "leg" + i, true, dark).at(7, 0, 0).rot(0, 0, 80).mat("chitin").box(0, -0.5f, -0.5f, 11, 1, 1, 0.3f).add();
        }
        return b.build();
    }

    /** Cerberus: a colossal three-headed hellhound of black, smouldering hide with lava seams and spiked collars. */
    static ModelSpec cerberus() {
        int fur = 0x2A1A1A;
        int lava = 0xF07020;
        ModelSpec.Builder b = ModelSpec.builder("cerberus").shadow(1.6f);
        part(b, "body", null, Role.BODY, fur).accent(lava).at(0, 11, 2).mat("rock").glow("cracks").box(-6, -6, -10, 12, 11, 20).add();
        part(b, "chest", "body", Role.STATIC, fur).accent(lava).at(0, -1, -10).mat("rock").glow("cracks").box(-6.5f, -5, -3, 13, 10, 4).add();
        for (int i = 0; i < 4; i++) {
            part(b, "spine" + i, "body", Role.STATIC, 0x1A0E0C).at(0, -6, -7 + i * 4.5f).rot(-30, 0, 0).mat("horn").box(-1, -4, -1, 2, 4, 2).add();
        }
        for (int h = 0; h < 3; h++) {
            float x = (h - 1) * 4.5f;
            float yaw = (h - 1) * -25f;
            String s = String.valueOf(h);
            part(b, "neck" + s, "chest", Role.STATIC, fur).accent(lava).at(x, -3, -3).rot(-30, yaw, 0).mat("rock").box(-2, -2, -5, 4, 4, 5).add();
            part(b, "collar" + s, "neck" + s, Role.STATIC, 0x3A3A3A).at(0, 0, -2).mat("metal").box(-2.5f, -2.5f, -1, 5, 5, 2).add();
            part(b, "head" + s, "neck" + s, Role.HEAD, fur).accent(0xFFB020).at(0, 0, -5).rot(30, 0, 0).mat("rock").pattern("eyes").glow("eyes")
                    .box(-2.5f, -2.5f, -4, 5, 5, 4).add();
            part(b, "snout" + s, "head" + s, Role.STATIC, fur).at(0, 0.5f, -4).mat("rock").box(-1.5f, -1, -3, 3, 2, 3).add();
            part(b, "jaw" + s, "head" + s, Role.JAW, 0x1A0E0C).at(0, 1.5f, -3).mat("rock").glow("maw").box(-1.5f, 0, -3.5f, 3, 1, 4).add();
            sym(b, "ear" + s, "head" + s, false, fur).at(1.5f, -2.5f, -1).rot(-10, 0, -15).mat("rock").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        }
        sym(b, "leg_f", null, false, fur).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(lava).at(4.5f, 13, -7).mat("rock")
                .glow("cracks").box(-2, -1, -2, 4, 11, 4).add();
        sym(b, "paw_f", "leg_f", true, 0x1A0E0C).at(0, 10, 0).mat("horn").box(-2.5f, 0, -3, 5, 1, 5).add();
        sym(b, "leg_b", null, false, fur).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(lava).at(4.5f, 12, 9).rot(10, 0, 0)
                .mat("rock").glow("cracks").box(-2.5f, -1, -2.5f, 5, 12, 5).add();
        sym(b, "paw_b", "leg_b", true, 0x1A0E0C).at(0, 11, -1).rot(-10, 0, 0).mat("horn").box(-2.5f, 0, -3, 5, 1, 5).add();
        String prev = "body";
        for (int i = 0; i < 4; i++) {
            part(b, "tail" + i, prev, Role.TAIL, fur).accent(lava).at(0, i == 0 ? -4 : 0, i == 0 ? 10 : 4).rot(i == 0 ? -30 : 10, 0, 0)
                    .mat("rock").glow(i == 3 ? "full" : null).box(-1, -1, 0, 2, 2, 4).add();
            prev = "tail" + i;
        }
        return b.build();
    }

    /** Callisto/Artio: a towering brown bear, hunched, with a pale muzzle and dark claws. */
    static ModelSpec bear(String id, int fur, int dark) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.2f);
        part(b, "body", null, Role.BODY, fur).at(0, 12, 2).mat("fur").box(-6, -6, -10, 12, 12, 19, 0.4f).add();
        part(b, "hump", "body", Role.STATIC, fur).at(0, -6, -6).mat("fur").box(-5, -2, -3, 10, 3, 7).add();
        part(b, "head", "body", Role.HEAD, fur).at(0, -2, -10).mat("fur").pattern("eyes").box(-4, -4, -5, 8, 7, 5).add();
        part(b, "muzzle", "head", Role.STATIC, 0x9A7A5A).at(0, 1, -5).mat("fur").box(-2, -1.5f, -3, 4, 3, 3).add();
        part(b, "nose", "muzzle", Role.STATIC, 0x1A1412).at(0, -1.5f, -3).mat("skin").box(-1, 0, -0.6f, 2, 1, 1).add();
        part(b, "jaw", "head", Role.JAW, dark).at(0, 3, -4).mat("fur").box(-2, 0, -3.5f, 4, 1, 4).add();
        sym(b, "ear", "head", false, fur).at(3, -4, -1).mat("fur").box(-1, -2, -0.5f, 2, 2, 1).add();
        sym(b, "leg_f", null, false, fur).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).at(4, 14, -6).mat("fur")
                .box(-2.5f, -1, -2.5f, 5, 10, 5).add();
        sym(b, "paw_f", "leg_f", true, dark).at(0, 9, 0).mat("fur").box(-3, 0, -3.5f, 6, 1, 6).add();
        sym(b, "claws_f", "paw_f", true, 0xE0D8C0).at(0, 0, -3.5f).mat("horn").box(-2.5f, 0, -1, 1, 1, 1).box(-0.5f, 0, -1, 1, 1, 1).box(1.5f, 0, -1, 1, 1, 1).add();
        sym(b, "leg_b", null, false, fur).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).at(4, 14, 6).mat("fur")
                .box(-2.5f, -1, -2.5f, 5, 10, 5).add();
        sym(b, "paw_b", "leg_b", true, dark).at(0, 9, 0).mat("fur").box(-3, 0, -3.5f, 6, 1, 6).add();
        part(b, "tail", "body", Role.TAIL, fur).at(0, -3, 9).mat("fur").box(-1, -1, 0, 2, 2, 2).add();
        return b.build();
    }

    /** Vet'ion: a skeletal champion in ornate purple-and-gold armour wielding a great halberd. */
    static ModelSpec vetion(String id, int armour) {
        ModelSpec.Builder b = Creatures.skeletonBuilder(id, 0xE0DCC8, "none", true);
        part(b, "breastplate", "spine", Role.STATIC, armour).at(0, -10, -1).mat("metal").box(-4.5f, 0, -3, 9, 7, 5).add();
        part(b, "breast_trim", "breastplate", Role.STATIC, Humanoid.GOLD).at(0, 0, -3).mat("metal").box(-1, 0, -0.6f, 2, 7, 1).add();
        sym(b, "pauldron", "arm", true, armour).at(0, 0, 0).rot(0, 0, -15).mat("metal").box(-1.5f, -1.5f, -2, 4, 3, 4).add();
        sym(b, "pauldron_spike", "pauldron", true, Humanoid.GOLD).at(1, -1.5f, 0).rot(0, 0, -30).mat("metal").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        part(b, "tassets", "pelvis", Role.STATIC, armour).at(0, 0, 0).mat("metal").box(-3.5f, 0, -2, 7, 4, 4).add();
        part(b, "helm", "skull", Role.STATIC, armour).at(0, -6, 0).mat("metal").box(-3.5f, -1, -4, 7, 3, 7, 0.2f).add();
        part(b, "crest", "helm", Role.STATIC, Humanoid.GOLD).at(0, -1, 0).mat("metal").box(-0.5f, -3, -3, 1, 3, 6).add();
        part(b, "w_shaft", "hand_r", Role.STATIC, 0x3A2A2A).at(0, 1, 0).mat("wood").box(-0.5f, -0.5f, -14, 1, 1, 20).add();
        part(b, "w_axe", "w_shaft", Role.STATIC, 0x9A9AA8).at(0, 0, -14).mat("metal").box(-0.5f, -4, -3, 1, 6, 3).box(-0.5f, -0.5f, -5, 1, 1, 2).add();
        return b.build();
    }

    /** Scorpions: low armoured body, six legs, great pincers and a segmented tail arched over the back to a stinger. */
    static ModelSpec scorpion(String id, int shell, int dark, int sting) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.8f);
        part(b, "thorax", null, Role.BODY, shell).accent(dark).at(0, 19, 0).mat("chitin").box(-4, -2.5f, -5, 8, 4, 10).add();
        for (int i = 0; i < 3; i++) {
            part(b, "plate" + i, "thorax", Role.STATIC, i % 2 == 0 ? shell : dark).at(0, -2.5f, -4 + i * 3.5f).mat("chitin")
                    .box(-4.5f, -1, 0, 9, 1, 3).add();
        }
        part(b, "head", "thorax", Role.HEAD, shell).accent(0xE04020).at(0, 0, -5).mat("chitin").pattern("eyes").box(-2.5f, -2, -2, 5, 3, 2).add();
        String prev = "thorax";
        int[] tw = {3, 3, 2, 2};
        for (int i = 0; i < tw.length; i++) {
            part(b, "tail" + i, prev, Role.TAIL, shell).accent(dark).at(0, i == 0 ? -1 : 0, i == 0 ? 5 : 3.5f)
                    .rot(i == 0 ? 50 : 38, 0, 0).mat("chitin").box(-tw[i] / 2f, -tw[i] / 2f, 0, tw[i], tw[i], 4).add();
            prev = "tail" + i;
        }
        part(b, "stinger", prev, Role.STATIC, sting).at(0, 0, 4).rot(50, 0, 0).mat("horn").box(-1, -1, 0, 2, 2, 2).box(-0.5f, -0.5f, 2, 1, 1, 2).add();
        sym(b, "arm", "thorax", false, shell).roles(Role.ARM_LEFT, Role.ARM_RIGHT).at(3.5f, 0, -4).rot(0, 25, 0).mat("chitin")
                .box(-1, -1, -5, 2, 2, 5).add();
        sym(b, "pincer", "arm", true, shell).at(0, 0, -5).rot(0, -35, 0).mat("chitin").box(-1.5f, -1.5f, -4, 3, 3, 4).add();
        sym(b, "pincer_tip", "pincer", true, dark).at(0, 0, -4).mat("horn").box(-1.5f, -1, -2, 1, 2, 2).box(0.5f, -1, -2, 1, 2, 2).add();
        for (int i = 0; i < 3; i++) {
            sym(b, "leg" + i, "thorax", false, dark).roles(Role.LEG_EXTRA, Role.LEG_EXTRA).at(4, 0.5f, -2.5f + i * 2.5f).rot(0, -(i - 1) * 20, -30)
                    .mat("chitin").box(0, -0.5f, -0.5f, 4, 1, 1, 0.2f).add();
            sym(b, "foot" + i, "leg" + i, true, dark).at(4, 0, 0).rot(0, 0, 70).mat("chitin").box(0, -0.5f, -0.5f, 4, 1, 1).add();
        }
        return b.build();
    }

    /** Kraken: a great cephalopod mantle with a huge eye and a ring of eight long curling tentacles. */
    static ModelSpec kraken(String id, int skin, int light) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.4f);
        part(b, "mantle", null, Role.FLOAT, skin).accent(light).at(0, 12, 0).mat("flesh").pattern("spots").box(-6, -14, -6, 12, 14, 12).add();
        part(b, "mantle_top", "mantle", Role.STATIC, skin).accent(light).at(0, -14, 0).mat("flesh").box(-4, -3, -4, 8, 3, 8).add();
        part(b, "head", "mantle", Role.HEAD, skin).accent(0xFFE040).at(0, 0, 0).mat("flesh").pattern("eyes").glow("eyes").box(-5, 0, -5, 10, 4, 10).add();
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45 + 22.5);
            float x = (float) Math.sin(a) * 4;
            float z = (float) Math.cos(a) * 4;
            String prev = "head";
            for (int k = 0; k < 3; k++) {
                String n = "tentacle" + i + "_" + k;
                part(b, n, prev, Role.TENTACLE, k == 2 ? light : skin).at(k == 0 ? x : 0, k == 0 ? 4 : 4, k == 0 ? z : 0)
                        .rot(k == 0 ? 35 : 25, k == 0 ? (float) Math.toDegrees(a) : 0, 0).mat("flesh")
                        .box(-1, 0, -1, 2, 4, 2, k == 0 ? 0.3f : -0.1f * k).add();
                prev = n;
            }
        }
        return b.build();
    }

    /** Nex: Zaros's general: a horned, armoured figure with great dark wings and glowing purple eyes. */
    static ModelSpec nex() {
        ModelSpec.Builder b = Humanoid.of("nex").body(12, 9, 11, 5, 12, 3).skin(0x8A7A9A).face("eyes").glowingEyes().hair(0, "none")
                .top(0x3A2A4A, "metal").trim(0xA040E0).bottoms(0x2A1E36, "plateskirt").pauldrons(0x3A2A4A).boots(0x1A1222)
                .horns(0xC8B8D8).gloves(0x2A1E36).builder();
        part(b, "crest", "head", Role.STATIC, 0x3A2A4A).at(0, -8, 0).mat("metal").box(-4, -1, -4, 8, 3, 8, 0.3f).add();
        sym(b, "wing", "torso", false, 0x2A1E36).roles(Role.WING_LEFT, Role.WING_RIGHT).accent(0xA040E0).at(3, -9, 2.5f).rot(-10, -30, -25)
                .mat("horn").box(0, -1, -1, 12, 2, 2).add();
        sym(b, "wing_web", "wing", true, 0x4A2A6A).accent(0xA040E0).at(0, 1, 0).mat("membrane").glow("runes").box(0, 0, -0.5f, 12, 11, 1).add();
        sym(b, "wing_finger", "wing", true, 0x2A1E36).at(12, 0, 0).rot(0, 0, 50).mat("horn").box(-0.5f, 0, -0.5f, 1, 11, 1).add();
        return b.build();
    }
}
