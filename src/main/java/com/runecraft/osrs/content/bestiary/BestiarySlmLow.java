package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.BestiarySlmKit.*;

/**
 * Low-level slayer creatures: crawling hands, cave bugs/crawlers, banshees, slimes and jellies, rockslugs,
 * lizards, cockatrices, the fiends, harpie bug swarms, wall beasts, killerwatts, molanisks, basilisks, sea
 * snakes, fever spiders and brine rats.
 */
final class BestiarySlmLow {
    private BestiarySlmLow() {
    }

    static List<ModelSpec> overrides() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(crawlingHand("crawling_hand"));
        l.add(caveBug("cave_bug"));
        l.add(caveCrawler("cave_crawler"));
        l.add(banshee("banshee", 0xE8E6DC, 0xDCDAD0, 0xF4F2EA));
        l.add(jelly("cave_slime", 0x6A9A3A, 0.7f));
        l.add(jelly("jelly", 0x5A9AC8, 1.0f));
        l.add(rockslug("rockslug"));
        l.add(lizard("lizard"));
        l.add(cockatrice("cockatrice"));
        l.add(fiend("pyrefiend", 0xE8641E, 0xFFC040, "lava"));
        l.add(fiend("waterfiend", 0x3A7AC0, 0x9AD8F8, "slime"));
        l.add(icefiend("icefiend"));
        l.add(harpieSwarm("harpie_bug_swarm"));
        l.add(wallBeast("wall_beast"));
        l.add(killerwatt("killerwatt"));
        l.add(molanisk("molanisk"));
        l.add(basilisk("basilisk"));
        l.add(seaSnake("sea_snake"));
        l.add(feverSpider("fever_spider"));
        l.add(brineRat("brine_rat"));
        return l;
    }

    static List<ModelSpec> fallbacks() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(banshee("twisted_banshee", 0xC8C0D8, 0x7A6A9A, 0xE0D8F0));
        l.add(jelly("warped_jelly", 0x8A5AB0, 1.0f));
        return l;
    }

    /** Crawling hand: a severed, rotting hand scuttling on its fingers, a bone stump and torn cuff at the wrist. */
    static ModelSpec crawlingHand(String id) {
        int skin = 0xB8B08A;
        int dark = 0x7A7458;
        int nail = 0x8A7A5A;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.4f);
        Kit.part(b, "palm", null, Role.BODY, skin).accent(dark).at(0, 17, 1).rot(-6, 0, 0).mat("skin").box(-4, -2, -4, 8, 3, 7).add();
        st(b, "back", "palm", dark).at(0, -2, 0).mat("skin").box(-3.5f, -1, -3.5f, 7, 1, 6).add();
        for (int k = 0; k < 4; k++) {
            st(b, "tendon" + k, "back", mix(skin, dark, 0.6f)).at(-2.6f + k * 1.75f, -0.6f, 0).mat("skin").box(-0.5f, -0.5f, -3, 1, 1, 5, -0.15f).add();
            st(b, "knuckle" + k, "palm", dark).at(-2.6f + k * 1.75f, -2.2f, -3.8f).mat("skin").box(-0.75f, -0.6f, -0.75f, 1, 1, 1, 0.25f).add();
        }
        st(b, "wrist", "palm", skin).accent(dark).at(0, -0.5f, 3).rot(-18, 0, 0).mat("skin").box(-3, -1.5f, 0, 6, 3, 4).add();
        st(b, "cuff", "wrist", 0x5A4A6A).at(0, 0, 3).mat("cloth").box(-3.5f, -2, 0, 7, 4, 2).add();
        st(b, "cuff_tatter", "cuff", 0x4A3A5A).at(0, 2, 1).rot(25, 0, 0).mat("cloth").box(-3, 0, -0.5f, 2, 2, 1).box(1, 0, -0.5f, 2, 3, 1).add();
        st(b, "stump", "cuff", 0x8A2A22).at(0, 0, 2).mat("flesh").box(-2.5f, -1.5f, 0, 5, 3, 1).add();
        st(b, "bone", "stump", BONE).at(0, 0, 1).mat("bone").box(-1, -1, 0, 2, 2, 2).box(-1.5f, -1.5f, 2, 3, 3, 1).add();
        // four walking fingers and a thumb
        float[] fx = {-3, -1, 1, 3};
        int[] fl = {3, 4, 4, 3};
        for (int f = 0; f < 4; f++) {
            String n = "finger" + f;
            Kit.part(b, n, "palm", Role.LEG_EXTRA, skin).accent(dark).at(fx[f] * 0.95f, -0.5f, -4).rot(18, (f - 1.5f) * -8, 0).mat("skin")
                    .box(-0.75f, -0.75f, -fl[f], 1, 1, fl[f], 0.25f).add();
            st(b, n + "_mid", n, skin).at(0, 0, -fl[f]).rot(38, 0, 0).mat("skin").box(-0.75f, -0.75f, -3, 1, 1, 3, 0.2f).add();
            st(b, n + "_tip", n + "_mid", dark).at(0, 0, -3).rot(34, 0, 0).mat("skin").box(-0.6f, -0.6f, -2, 1, 1, 2, 0.1f).add();
            st(b, n + "_nail", n + "_tip", nail).at(0, -0.6f, -1.6f).mat("horn").box(-0.5f, -0.3f, -0.5f, 1, 1, 1, -0.1f).add();
        }
        Kit.part(b, "thumb", "palm", Role.LEG_EXTRA, skin).accent(dark).at(4, 0, -1).rot(0, -50, 40).mat("skin").box(0, -0.75f, -0.75f, 3, 1, 1, 0.25f).add();
        st(b, "thumb_tip", "thumb", dark).at(3, 0, 0).rot(0, 0, 50).mat("skin").box(0, -0.6f, -0.6f, 3, 1, 1, 0.15f).add();
        st(b, "thumb_nail", "thumb_tip", nail).at(2.4f, -0.4f, 0).mat("horn").box(-0.5f, -0.4f, -0.5f, 1, 1, 1, -0.1f).add();
        st(b, "wound", "back", 0x6A2A22).at(1.5f, -0.8f, 1).mat("flesh").box(-1, -0.4f, -1, 2, 1, 2, -0.1f).add();
        return b.build();
    }

    /** Cave bug: a little domed beetle with split wing cases, a horned head, mandibles, antennae and six legs. */
    static ModelSpec caveBug(String id) {
        int shell = 0x5E4A34;
        int dark = 0x2E2418;
        int spot = 0xA88A4A;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.35f);
        Kit.part(b, "body", null, Role.BODY, dark).accent(spot).at(0, 20.5f, 1).mat("chitin").box(-3, -1.5f, -3.5f, 6, 3, 7).add();
        sy(b, "elytron", "body", shell).accent(spot).at(0.1f, -1.5f, -3).rot(-6, 0, -8).mat("chitin").pattern("spots")
                .box(0, -1.5f, 0, 3, 2, 7).add();
        st(b, "seam", "body", dark).at(0, -3.2f, 0).mat("chitin").box(-0.5f, 0, -3, 1, 1, 6, -0.1f).add();
        st(b, "abdomen_tip", "body", shell).at(0, 0, 3.5f).rot(15, 0, 0).mat("chitin").box(-2, -1, 0, 4, 2, 2).add();
        st(b, "underside", "body", 0x1E1810).at(0, 1.5f, 0).mat("chitin").box(-2.5f, -0.4f, -3, 5, 1, 6).box(-2, 0.2f, -2, 4, 1, 4).add();
        sy(b, "pronotum_horn", "thorax", shell).at(1.8f, -1.5f, -1).rot(-40, 0, -20).mat("horn").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        st(b, "thorax", "body", shell).accent(spot).at(0, -0.5f, -3.5f).mat("chitin").box(-2.5f, -1.5f, -2, 5, 3, 2).add();
        Kit.part(b, "head", "thorax", Role.HEAD, dark).accent(0xE0C040).at(0, 0.5f, -2).mat("chitin").pattern("eyes").glow("eyes")
                .box(-2, -1.5f, -2, 4, 2, 2).add();
        st(b, "horn", "head", shell).at(0, -1.5f, -1.5f).rot(-40, 0, 0).mat("horn").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        sy(b, "mandible", "head", 0x3A2A1A).at(1.2f, 0.3f, -2).rot(0, 30, 0).mat("horn").box(-0.5f, -0.5f, -2, 1, 1, 2, -0.1f).add();
        sy(b, "antenna", "head", dark).at(1.2f, -1.4f, -1.8f).rot(-30, -25, 0).mat("chitin").box(-0.5f, -0.5f, -3, 1, 1, 3, -0.3f).add();
        sy2(b, "antenna_tip", "antenna", spot).at(0, 0, -3).rot(-20, 0, 0).mat("chitin").box(-0.5f, -0.5f, -2, 1, 1, 2, -0.3f).add();
        for (int i = 0; i < 3; i++) {
            bugLeg(b, "leg" + i, "body", dark, shell, "chitin", 2.5f, 0.5f, -2.5f + i * 2.5f, 30 - i * 30, 3, 3, 2, 1);
        }
        return b.build();
    }

    /** Cave crawler: a squat, spiny green-brown arthropod with a broad plated back, mandibled maw and eight legs. */
    static ModelSpec caveCrawler(String id) {
        int body = 0x5E6A2E;
        int dark = 0x343C18;
        int spine = 0xC8B880;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.7f);
        Kit.part(b, "body", null, Role.BODY, body).accent(dark).at(0, 17.5f, 1).mat("chitin").box(-5, -3, -6, 10, 5, 12).add();
        for (int i = 0; i < 4; i++) {
            st(b, "plate" + i, "body", i % 2 == 0 ? body : mix(body, dark, 0.4f)).accent(dark).at(0, -3, -5 + i * 3.3f).rot(-8, 0, 0)
                    .mat("chitin").box(-4.5f + i * 0.2f, -1.5f, -1.5f, 9, 2, 4).add();
            sy(b, "spike" + i, "plate" + i, spine).at(3, -1.5f, 0).rot(-30, 0, -25).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1, 0.1f).add();
        }
        st(b, "tail", "body", dark).at(0, -0.5f, 6).rot(20, 0, 0).mat("chitin").box(-3, -2, 0, 6, 3, 3).add();
        st(b, "tail_spike", "tail", spine).at(0, -1, 3).rot(-20, 0, 0).mat("horn").box(-0.5f, -0.5f, 0, 1, 1, 3).add();
        Kit.part(b, "head", "body", Role.HEAD, body).accent(0xFF5020).at(0, 0, -6).rot(10, 0, 0).mat("chitin").pattern("eyes").glow("eyes")
                .box(-3.5f, -2.5f, -4, 7, 4, 4).add();
        Kit.part(b, "jaw", "head", Role.JAW, dark).at(0, 1.5f, -3).mat("chitin").box(-2.5f, 0, -1.5f, 5, 1, 2).add();
        sy(b, "mandible", "head", 0x2A2414).at(2.2f, 1, -4).rot(0, 35, 0).mat("horn").box(-0.5f, -0.5f, -3, 1, 1, 3, 0.1f).add();
        sy2(b, "mandible_tip", "mandible", spine).at(0, 0, -3).rot(0, -60, 0).mat("horn").box(-0.5f, -0.5f, -2, 1, 1, 2, -0.1f).add();
        sy(b, "feeler", "head", dark).at(1.5f, -2.5f, -3.5f).rot(-50, -20, 0).mat("chitin").box(-0.5f, -0.5f, -4, 1, 1, 4, -0.3f).add();
        for (int i = 0; i < 4; i++) {
            bugLeg(b, "leg" + i, "body", body, dark, "chitin", 4.5f, 0.5f, -4.5f + i * 3, 35 - i * 23, 4, 5, 2, 2);
        }
        return b.build();
    }

    /**
     * Banshee: a floating, screaming spirit woman - long white hair over a tiered, tattered shroud, arms raised
     * with long clawed fingers, and a dark open mouth.
     */
    static ModelSpec banshee(String id, int skin, int robe, int hair) {
        int shade = darker(robe, 0.75f);
        int voidC = 0x161418;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.3f);
        Kit.part(b, "body", null, Role.FLOAT, robe).accent(shade).at(0, 8, 0).mat("ghost").box(-3.5f, -9, -2, 7, 9, 4).add();
        st(b, "bodice", "body", shade).at(0, -8, -2).mat("ghost").box(-3, 0, -0.6f, 6, 4, 1).add();
        float[] tierW = {8, 10, 12};
        String prev = "body";
        for (int t = 0; t < 3; t++) {
            String n = "skirt" + t;
            Kit.part(b, n, prev, t == 0 ? Role.STATIC : Role.TAIL, t % 2 == 0 ? robe : shade).accent(shade).at(0, t == 0 ? 0 : 3.5f, 0.3f)
                    .rot(4, 0, 0).mat("ghost").box(-tierW[t] / 2f, 0, -2.5f - t, (int) tierW[t], 4, 5 + 2 * t).add();
            prev = n;
        }
        for (int k = 0; k < 10; k++) {
            float a = (float) Math.toRadians(k * 36 + 18);
            float x = (float) Math.sin(a) * 5.2f;
            float z = (float) Math.cos(a) * 4.2f;
            st(b, "tatter" + k, "skirt2", k % 2 == 0 ? robe : shade).at(x, 3.5f, z)
                    .rot((float) Math.cos(a) * 18, (float) Math.toDegrees(a), 0).mat("ghost").box(-1, 0, -0.5f, 2, 3 + (k % 3), 1).add();
        }
        st(b, "sash", "body", shade).at(0, -1, 0).mat("ghost").box(-3.5f, -1, -2, 7, 1, 4, 0.3f).add();
        st(b, "sash_tail", "sash", shade).at(1.5f, 0, -2.3f).rot(-5, 0, 8).mat("ghost").box(-0.5f, 0, -0.5f, 2, 6, 1).add();
        sy(b, "collar", "body", robe).at(2, -9, 0).rot(0, 0, -20).mat("ghost").box(-1, -1, -2, 3, 2, 4).add();
        // head with streaming hair
        Kit.part(b, "head", "body", Role.HEAD, skin).accent(0x7AD8FF).at(0, -9.5f, 0).rot(-8, 0, 0).mat("ghost").pattern("skull").glow("eyes")
                .box(-3, -6, -3, 6, 6, 6).add();
        Kit.part(b, "jaw", "head", Role.JAW, voidC).at(0, -1, -3).mat("flesh").box(-1.5f, 0, -0.6f, 3, 3, 1).add();
        st(b, "hair_top", "head", hair).at(0, -6, 0).mat("hair").box(-3.5f, -1, -3.5f, 7, 2, 7).add();
        st(b, "hair_back", "head", hair).at(0, -5, 3).rot(16, 0, 0).mat("hair").box(-3.5f, 0, -0.5f, 7, 11, 2).add();
        st(b, "hair_tail", "hair_back", hair).at(0, 11, 0.5f).rot(20, 0, 0).mat("hair").box(-3, 0, -1, 6, 5, 2, -0.1f).add();
        sy(b, "hair_side", "head", hair).at(3.2f, -5, -1).rot(0, 0, -8).mat("hair").box(0, 0, -2, 1, 9, 4).add();
        sy2(b, "hair_lock", "hair_side", hair).at(0.5f, 9, 0).rot(10, 0, -15).mat("hair").box(-0.5f, 0, -1.5f, 1, 4, 3, -0.1f).add();
        // raised arms in trailing sleeves
        Kit.sym(b, "arm", "body", false, robe).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(shade).at(4.5f, -8, 0).rot(-140, 0, -38)
                .mat("ghost").box(-1.5f, -1, -1.5f, 3, 6, 3).add();
        sy2(b, "sleeve", "arm", shade).at(0, 3, 1.5f).rot(30, 0, 0).mat("ghost").box(-1.5f, 0, -0.5f, 3, 7, 1).add();
        sy2(b, "forearm", "arm", skin).at(0, 5.5f, 0).rot(-30, 0, 0).mat("ghost").box(-1, 0, -1, 2, 5, 2).add();
        Kit.sym(b, "fingers", "forearm", true, skin).at(0, 5, 0).rot(20, 0, 0).mat("ghost")
                .box(-1, 0, -1, 1, 3, 1, -0.15f).box(0.1f, 0, -1, 1, 3, 1, -0.15f).box(-1, 0, 0.1f, 1, 2, 1, -0.15f).add();
        return b.build();
    }

    /**
     * Jelly / cave slime: a wobbling translucent blob - a wide base, a domed body and a lumpy crown, with drips,
     * bulges and a gaping face. {@code s} scales the slime down (cave slimes are small).
     */
    static ModelSpec jelly(String id, int c, float s) {
        int light = mix(c, 0xFFFFFF, 0.3f);
        int deep = darker(c, 0.65f);
        ModelSpec.Builder b = ModelSpec.builder(id).scale(s).shadow(0.7f * s);
        Kit.part(b, "base", null, Role.BODY, deep).accent(light).at(0, 24, 0).mat("slime").box(-7, -3, -7, 14, 3, 14).add();
        st(b, "body", "base", c).accent(light).at(0, -3, 0).mat("slime").box(-6, -6, -6, 12, 6, 12).add();
        Kit.part(b, "dome", "body", Role.FLOAT, light).accent(c).at(0, -6, 0).mat("slime").pattern("eyes").glow("eyes")
                .box(-5, -4, -5, 10, 4, 10).add();
        st(b, "crown", "dome", light).at(0, -4, 0.5f).mat("slime").box(-3, -2, -3, 6, 2, 6).add();
        st(b, "crown_tip", "crown", mix(light, 0xFFFFFF, 0.3f)).at(1, -2, 0).mat("slime").box(-1.5f, -1.5f, -1.5f, 3, 2, 3).add();
        Kit.part(b, "mouth", "body", Role.JAW, darker(c, 0.35f)).at(0, -2.5f, -6).mat("slime").box(-3, 0, -0.6f, 6, 2, 1).add();
        float[][] bulge = {{6, -3, 1}, {-6, -4, -2}, {2, -2, 6}, {-3, -3, 6}, {4, -6, -4}};
        for (int i = 0; i < bulge.length; i++) {
            st(b, "bulge" + i, "body", i % 2 == 0 ? c : light).at(bulge[i][0], bulge[i][1], bulge[i][2]).mat("slime")
                    .box(-1.5f, -1.5f, -1.5f, 3, 3, 3, 0.2f).add();
        }
        float[][] drips = {{-5, 2}, {5, -3}, {-2, -6.5f}, {3, 6.5f}, {6.5f, 4}, {-6.5f, -4}};
        for (int i = 0; i < drips.length; i++) {
            st(b, "drip" + i, "base", i % 2 == 0 ? light : c).at(drips[i][0], -3, drips[i][1]).mat("slime")
                    .box(-0.5f, -1, -0.5f, 1, 3, 1, 0.15f).add();
        }
        for (int i = 0; i < 8; i++) {
            float a = (float) Math.toRadians(i * 45 + 10);
            st(b, "rim" + i, "base", i % 2 == 0 ? deep : c).at((float) Math.cos(a) * 6.5f, -1.5f, (float) Math.sin(a) * 6.5f)
                    .rot(0, -i * 45, 0).mat("slime").box(-2, -1.5f, -1, 4, 3, 2, 0.2f).add();
            st(b, "lump" + i, "body", i % 2 == 0 ? light : c).at((float) Math.cos(a) * 5.2f, -5.5f, (float) Math.sin(a) * 5.2f)
                    .rot(0, -i * 45, 0).mat("slime").box(-1.5f, -1, -1, 3, 2, 2, 0.15f).add();
        }
        sy(b, "brow", "dome", deep).at(2, -2.6f, -5.1f).rot(0, 0, -12).mat("slime").box(-1.5f, -0.5f, -0.5f, 3, 1, 1).add();
        for (int i = 0; i < 4; i++) {
            float a = (float) Math.toRadians(45 + i * 90);
            Kit.part(b, "glob" + i, "base", Role.FLOAT, light).at((float) Math.cos(a) * 9, -1, (float) Math.sin(a) * 9).mat("slime")
                    .box(-1, -1, -1, 2, 2, 2).add();
        }
        return b.build();
    }

    /** Rockslug: a fat grey-brown slug whose back is armoured in rocky nodules, with two eye stalks and a mantle. */
    static ModelSpec rockslug(String id) {
        int skin = 0x8A7A62;
        int dark = 0x5A4C3A;
        int rock = 0x6E6A62;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.5f);
        Kit.part(b, "body", null, Role.BODY, skin).accent(dark).at(0, 22, 0).mat("slime").box(-3.5f, -3, -7, 7, 5, 14).add();
        st(b, "foot", "body", dark).at(0, 2, 0).mat("slime").box(-4.5f, -1, -8, 9, 1, 16).add();
        st(b, "mantle", "body", mix(skin, rock, 0.5f)).at(0, -3, -3).mat("rock").box(-3.5f, -1.5f, -4, 7, 2, 7, 0.2f).add();
        float[][] rocks = {{-1.5f, -1}, {1.5f, 1}, {0, 3.5f}, {-2, 5}, {2, -2.5f}, {1, 5.5f}, {-1, 2}, {0, -4}, {-2.2f, -3}, {2.2f, 3.5f},
                {0, 6.5f}, {-2.4f, 1.5f}};
        for (int i = 0; i < rocks.length; i++) {
            st(b, "rock" + i, "body", i % 3 == 0 ? rock : i % 3 == 1 ? darker(rock, 0.8f) : mix(rock, 0xA09A8A, 0.4f))
                    .at(rocks[i][0], -3.5f, rocks[i][1]).rot(i * 13 % 30 - 15, i * 37 % 90, i * 23 % 30 - 15).mat("rock")
                    .box(-1.5f, -1.5f, -1.5f, 3, 2, 3).add();
        }
        String t = chain(b, "tail", "body", Role.TAIL, skin, dark, "slime", null, 0, 0.5f, 7, 1, new int[]{6, 4, 2}, new int[]{4, 3, 2},
                new int[]{3, 3, 2}, new float[]{4, 4, 4}, null);
        // a slug's eyes sit on its stalks: no painted face on the head
        Kit.part(b, "head", "body", Role.HEAD, skin).accent(0xE0D040).at(0, 0, -7).mat("slime").pattern("blank").box(-3, -2.5f, -3, 6, 4, 3).add();
        Kit.part(b, "mouth", "head", Role.JAW, darker(skin, 0.4f)).at(0, 1, -3).mat("slime").box(-1.5f, -0.5f, -0.6f, 3, 1, 1).add();
        for (int s = -1; s <= 1; s += 2) {
            String n = s < 0 ? "stalk_r" : "stalk_l";
            st(b, n, "head", skin).at(s * 1.5f, -2.5f, -1.5f).rot(-20, 0, s * -15).mat("slime").box(-0.5f, -4, -0.5f, 1, 4, 1).add();
            Kit.part(b, n + "_eye", n, Role.STATIC, 0x1A1A14).accent(0xE0D040).at(0, -4, 0).mat("energy").glow("full")
                    .box(-1, -1.5f, -1, 2, 2, 2, -0.1f).add();
        }
        sy(b, "tentacle", "head", dark).at(2, 1, -2.5f).rot(30, -30, 0).mat("slime").box(-0.5f, -0.5f, -2, 1, 1, 2).add();
        sy(b, "feeler", "head", dark).at(1, 1.5f, -2.8f).rot(50, -10, 0).mat("slime").box(-0.5f, -0.5f, -2, 1, 1, 2, -0.1f).add();
        for (int k = 0; k < 3; k++) {
            sy(b, "frill" + k, "body", mix(skin, dark, 0.5f)).at(3.6f, 1, -5 + k * 4.5f).rot(0, 0, -25).mat("slime").box(-0.5f, -1, -2, 1, 2, 4).add();
        }
        st(b, "slime_trail", t, mix(skin, 0xB8C0A0, 0.5f)).at(0, 1, 2).mat("slime").box(-1.5f, 0, -1, 3, 1, 4, -0.1f).add();
        return b.build();
    }

    /** Desert lizard: a big sandy lizard sprawled on four splayed legs, with a dewlap, back spines and a long tail. */
    static ModelSpec lizard(String id) {
        int body = 0xB08A4E;
        int dark = 0x7A5A30;
        int belly = 0xD8C08A;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.7f);
        Kit.part(b, "body", null, Role.BODY, body).accent(dark).at(0, 19.5f, 0).mat("scales").pattern("stripes").box(-4, -2.5f, -7, 8, 5, 14).add();
        st(b, "belly", "body", belly).at(0, 2.5f, 0).mat("plates").box(-3, -0.6f, -6.5f, 6, 1, 13).add();
        for (int i = 0; i < 6; i++) {
            st(b, "spine" + i, "body", dark).at(0, -2.5f, -6 + i * 2.4f).rot(-25, 0, 0).mat("horn").box(-0.5f, -1.5f, -0.5f, 1, 2, 1).add();
        }
        sy(b, "flank", "body", dark).at(4, 0, 0).mat("scales").box(-0.5f, -1.5f, -5, 1, 3, 10).add();
        st(b, "neck", "body", body).accent(dark).at(0, -0.5f, -7).rot(-12, 0, 0).mat("scales").box(-2.5f, -2, -3, 5, 4, 3).add();
        Kit.part(b, "head", "neck", Role.HEAD, body).accent(0xE0C030).at(0, 0, -3).rot(12, 0, 0).mat("scales").pattern("eyes").glow("eyes")
                .box(-3, -2, -4, 6, 3, 4).add();
        st(b, "snout", "head", body).at(0, -0.5f, -4).mat("scales").box(-2, -1, -3, 4, 2, 3).add();
        Kit.part(b, "jaw", "head", Role.JAW, belly).at(0, 1, -3.5f).mat("plates").box(-2, 0, -3.5f, 4, 1, 4).add();
        st(b, "dewlap", "jaw", 0xC0603A).at(0, 1, -1).mat("membrane").box(-0.5f, 0, -2, 1, 3, 3).add();
        sy(b, "brow", "head", dark).at(2.4f, -2, -3).rot(0, -10, -15).mat("horn").box(-0.5f, -1, -1, 1, 1, 3).add();
        float[][] lp = {{3.5f, 20, -5}, {3.5f, 20, 5}};
        Role[][] roles = {{Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT}, {Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT}};
        for (int i = 0; i < 2; i++) {
            String ln = i == 0 ? "leg_f" : "leg_b";
            Kit.sym(b, ln, null, false, body).roles(roles[i][0], roles[i][1]).accent(dark).at(lp[i][0], lp[i][1], lp[i][2])
                    .rot(0, i == 0 ? 20 : -20, -20).mat("scales").box(0, -1, -1.5f, 4, 2, 3).add();
            sy2(b, ln + "_shin", ln, dark).at(4, 0, 0).rot(0, 0, 70).mat("scales").box(-1, 0, -1, 2, 4, 2).add();
            sy2(b, ln + "_foot", ln + "_shin", dark).at(0, 3.5f, 0).rot(0, 0, -50).mat("scales").box(-1.5f, 0, -2, 3, 1, 3).add();
            claws(b, ln + "_claws", ln + "_foot", true, CLAW, 3, 2, 0.5f, -1.8f, 2);
        }
        String t = chain(b, "tail", "body", Role.TAIL, body, dark, "scales", null, 0, 0, 7, 1, new int[]{5, 4, 3, 2, 2, 1},
                new int[]{4, 3, 3, 2, 2, 1}, new int[]{4, 4, 4, 4, 4, 3}, new float[]{4, 2, 0, -2, -2, 0},
                new float[]{0, 10, 10, 6, -10, -10});
        st(b, "tail_tip", t, dark).at(0, 0, 3).mat("horn").box(-0.5f, -0.5f, 0, 1, 1, 2).add();
        return b.build();
    }

    /** Cockatrice: a chicken-headed lizard - combed, wattled bird head on a feathered body, stubby wings, scaly legs, lizard tail. */
    static ModelSpec cockatrice(String id) {
        int feather = 0x6E8A3A;
        int dark = 0x4A5E24;
        int scale = 0x8A7A3A;
        int comb = 0xC8281E;
        int beak = 0xE0B030;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.5f);
        Kit.part(b, "body", null, Role.BODY, feather).accent(dark).at(0, 14, 1).rot(-15, 0, 0).mat("feathers").box(-4, -4, -5, 8, 8, 10).add();
        st(b, "breast", "body", mix(feather, 0xC8C060, 0.35f)).at(0, -1, -5).rot(-10, 0, 0).mat("feathers").box(-3, -3, -1.5f, 6, 7, 2).add();
        st(b, "neck", "body", feather).accent(dark).at(0, -3.5f, -4).rot(-25, 0, 0).mat("feathers").box(-2, -6, -2, 4, 6, 4).add();
        st(b, "ruff", "neck", dark).at(0, -1, 0).mat("feathers").box(-2.5f, -2, -2.5f, 5, 3, 5).add();
        Kit.part(b, "head", "neck", Role.HEAD, feather).accent(0xFF3020).at(0, -6, 0).rot(40, 0, 0).mat("feathers").pattern("eyes").glow("eyes")
                .box(-2, -3.5f, -2.5f, 4, 4, 4).add();
        st(b, "beak", "head", beak).at(0, -1.5f, -2.5f).rot(10, 0, 0).mat("horn").box(-1, -0.5f, -2, 2, 1, 2).add();
        Kit.part(b, "jaw", "head", Role.JAW, darker(beak, 0.8f)).at(0, -0.5f, -2.5f).mat("horn").box(-0.8f, 0, -1.5f, 2, 1, 2, -0.1f).add();
        st(b, "comb", "head", comb).at(0, -3.5f, -1).mat("skin").box(-0.5f, -2, -1, 1, 2, 3).box(-0.5f, -1, 2, 1, 1, 1).add();
        st(b, "wattle", "jaw", comb).at(0, 1, -0.5f).mat("skin").box(-0.5f, 0, -0.5f, 1, 2, 1).add();
        Kit.sym(b, "wing", "body", false, dark).roles(Role.WING_LEFT, Role.WING_RIGHT).accent(feather).at(4, -2.5f, -2).rot(-5, 0, -10)
                .mat("feathers").box(0, -0.5f, 0, 1, 5, 7).add();
        sy2(b, "wing_tip", "wing", darker(dark, 0.8f)).at(0.5f, 3, 6).rot(30, 0, 0).mat("feathers").box(-0.5f, 0, 0, 1, 2, 4).add();
        String t = chain(b, "tail", "body", Role.TAIL, scale, dark, "scales", null, 0, 1, 5, 1, new int[]{4, 3, 3, 2, 2, 1},
                new int[]{4, 3, 2, 2, 2, 1}, new int[]{4, 4, 4, 4, 3, 3}, new float[]{30, 4, 4, 0, -6, -10}, new float[]{0, 8, 8, -8, -10, -6});
        st(b, "tail_feathers", "tail0", dark).at(0, -2, 1).rot(-30, 0, 0).mat("feathers").box(-1.5f, -4, -0.5f, 3, 4, 1).add();
        st(b, "tail_tip", t, dark).at(0, 0, 3).mat("horn").box(-0.5f, -0.5f, 0, 1, 1, 2).add();
        Kit.sym(b, "thigh", null, false, feather).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(dark).at(2.5f, 16, 2)
                .rot(-15, 0, 0).mat("feathers").box(-1.5f, -1, -1.5f, 3, 4, 3).add();
        sy2(b, "shank", "thigh", scale).at(0, 3, 0).rot(15, 0, 0).mat("scales").box(-0.5f, 0, -0.5f, 1, 5, 1, 0.2f).add();
        sy2(b, "foot", "shank", scale).at(0, 4.5f, 0).mat("scales").box(-1.5f, 0, -2.5f, 3, 1, 3).add();
        claws(b, "talons", "foot", true, CLAW, 3, 2, 0.5f, -2.3f, 1);
        sy2(b, "spur", "shank", CLAW).at(0, 3, 0.5f).rot(-30, 0, 0).mat("horn").box(-0.5f, -0.5f, 0, 1, 1, 2, -0.1f).add();
        return b.build();
    }

    /**
     * A fiend: a floating elemental imp - a body of layered flame (pyrefiend) or water (waterfiend) tongues
     * rising to a horned head with glowing eyes, wispy arms with clawed hands, and a tapering tail of flame/water.
     */
    static ModelSpec fiend(String id, int c, int hot, String mat) {
        int dark = darker(c, 0.6f);
        boolean lava = mat.equals("lava");
        String glow = lava ? "full" : null;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.4f);
        Kit.part(b, "body", null, Role.FLOAT, c).accent(hot).at(0, 12, 0).mat(mat).box(-3.5f, -8, -2.5f, 7, 8, 5).add();
        st(b, "core", "body", hot).accent(hot).at(0, -4.5f, -2.5f).mat("energy").glow("full").box(-1.5f, -1.5f, -0.6f, 3, 3, 1).add();
        String tail = chain(b, "wisp", "body", Role.TAIL, c, hot, mat, glow, 0, 0, 0, 1, new int[]{6, 4, 3, 2}, new int[]{6, 4, 3, 2},
                new int[]{2, 2, 2, 2}, new float[]{80, 10, 10, 10}, new float[]{0, 15, 15, -15});
        st(b, "wisp_tip", tail, hot).accent(hot).at(0, 0, 2).mat(mat).glow(glow).box(-0.5f, -0.5f, 0, 1, 1, 2).add();
        for (int k = 0; k < 6; k++) {
            float a = (float) Math.toRadians(k * 60 + 30);
            st(b, "tongue" + k, "body", k % 2 == 0 ? hot : c).accent(hot).at((float) Math.sin(a) * 3.2f, -6 + (k % 3) * 2, (float) Math.cos(a) * 2.2f)
                    .rot((float) Math.cos(a) * 25, 0, (float) -Math.sin(a) * 25).mat(mat).glow(glow).box(-1, -4, -1, 2, 4, 2, -0.1f).add();
        }
        Kit.part(b, "head", "body", Role.HEAD, c).accent(lava ? 0xFFF060 : 0xE0FFFF).at(0, -8, 0).mat(mat).pattern("eyes").glow("eyes")
                .box(-3, -5, -3, 6, 5, 6).add();
        Kit.part(b, "jaw", "head", Role.JAW, dark).at(0, -1, -3).mat(mat).glow(lava ? "maw" : null).box(-2, 0, -0.6f, 4, 2, 1).add();
        horn(b, "horn", "head", true, false, lava ? 0x3A1A10 : 0x1E3A5A, 2.2f, -4.5f, 0, -20, 0, -30, new int[]{2, 1, 1},
                new int[]{2, 2, 2}, -25, 10);
        for (int k = 0; k < 3; k++) {
            st(b, "flame" + k, "head", k == 1 ? hot : c).accent(hot).at(-1.5f + k * 1.5f, -5, 1).rot(-25, 0, (k - 1) * -20).mat(mat).glow(glow)
                    .box(-0.75f, -3 - (k == 1 ? 1 : 0), -0.75f, 1, 3 + (k == 1 ? 1 : 0), 1, 0.25f).add();
        }
        Kit.sym(b, "arm", "body", false, c).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(hot).at(4.5f, -7, 0).rot(-35, 0, -25).mat(mat).glow(glow)
                .box(-1.5f, -1, -1.5f, 3, 5, 3).add();
        sy2(b, "forearm", "arm", c).accent(hot).at(0, 4.5f, 0).rot(-40, 0, 10).mat(mat).box(-1, 0, -1, 2, 4, 2, 0.2f).add();
        Kit.sym(b, "claws", "forearm", true, dark).at(0, 4, 0).mat("horn").box(-1, 0, -1, 1, 2, 1, -0.1f).box(0.2f, 0, -1, 1, 2, 1, -0.1f)
                .box(-0.4f, 0, 0.2f, 1, 2, 1, -0.1f).add();
        return b.build();
    }

    /** Icefiend: a round, spiky shard of living ice with a fanged face, stubby arms and floating ice fragments. */
    static ModelSpec icefiend(String id) {
        int ice = 0xA8D8F0;
        int deep = 0x5A9ACC;
        int white = 0xE8F8FF;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.4f);
        Kit.part(b, "body", null, Role.FLOAT, ice).accent(white).at(0, 13, 0).mat("ice").box(-5, -9, -4.5f, 10, 9, 9).add();
        st(b, "belly", "body", white).at(0, -3, -4.5f).mat("ice").box(-3, -3, -0.6f, 6, 4, 1).add();
        Kit.part(b, "head", "body", Role.HEAD, ice).accent(0x30A0FF).at(0, -9, -0.5f).mat("ice").pattern("eyes").glow("eyes")
                .box(-4, -4, -4, 8, 4, 8).add();
        Kit.part(b, "jaw", "body", Role.JAW, deep).at(0, -6, -4.5f).mat("ice").box(-3, 0, -0.6f, 6, 2, 1).add();
        teeth(b, "fangs", "jaw", white, 0, -0.9f, 2.8f, 1, false, true);
        float[][] sp = {{0, -13, 0, 0, 0}, {3, -12, 2, -15, -30}, {-3, -12, 2, -15, 30}, {4.5f, -7, 0, 0, -70}, {-4.5f, -7, 0, 0, 70},
                {0, -10, 4, -60, 0}, {3, -4, 4, -60, -30}, {-3, -4, 4, -60, 30}, {2, -12, -3, 30, -20}, {-2, -12, -3, 30, 20}};
        for (int i = 0; i < sp.length; i++) {
            st(b, "spike" + i, "body", i % 2 == 0 ? white : ice).accent(deep).at(sp[i][0], sp[i][1], sp[i][2]).rot(sp[i][3], 0, sp[i][4])
                    .mat("ice").box(-1, -4 + (i % 3), -1, 2, 4 - (i % 3), 2).add();
            st(b, "spike_tip" + i, "spike" + i, white).at(0, -4 + (i % 3), 0).mat("ice").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        }
        Kit.sym(b, "arm", "body", false, ice).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(white).at(5.5f, -6, 0).rot(-30, 0, -30).mat("ice")
                .box(-1.5f, -1, -1.5f, 3, 4, 3).add();
        Kit.sym(b, "claws", "arm", true, white).at(0, 3.5f, 0).mat("ice").box(-1.5f, 0, -1.5f, 1, 2, 1).box(0.5f, 0, -1.5f, 1, 2, 1)
                .box(-0.5f, 0, 0.5f, 1, 2, 1).add();
        for (int k = 0; k < 4; k++) {
            float a = (float) Math.toRadians(k * 90 + 20);
            Kit.part(b, "shard" + k, "body", Role.SPIN, k % 2 == 0 ? white : deep).at((float) Math.cos(a) * 8, 2 - k, (float) Math.sin(a) * 8)
                    .rot(30, k * 40, 20).mat("ice").box(-1, -1.5f, -1, 2, 3, 2).add();
        }
        return b.build();
    }

    /** Harpie bug swarm: a buzzing cloud of glowing-bodied harpie bugs orbiting a central knot of insects. */
    static ModelSpec harpieSwarm(String id) {
        int body = 0x8A6A2A;
        int wing = 0xC8D8C8;
        int glow = 0xFFD050;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.4f);
        Kit.part(b, "swarm", null, Role.SPIN, body).at(0, 12, 0).mat("chitin").box(-1, -1, -1, 2, 2, 2).add();
        Kit.part(b, "swarm2", null, Role.FLOAT, body).at(0, 12, 0).mat("chitin").box(-1, 2, -1, 2, 2, 2).add();
        float[][] pos = {{4, -4, 0}, {-3, -6, 3}, {0, -2, -5}, {-5, 0, -2}, {3, 1, 4}, {6, -1, -3}, {-1, -8, -2}, {1, 4, 1}, {-4, 3, 4},
                {5, -7, 3}, {-6, -5, -4}, {2, -5, 6}};
        for (int i = 0; i < pos.length; i++) {
            String par = i % 2 == 0 ? "swarm" : "swarm2";
            String n = "bug" + i;
            st(b, n, par, body).accent(glow).at(pos[i][0], pos[i][1] - (i % 2 == 0 ? 0 : 0), pos[i][2]).rot(0, i * 47 % 360, i % 3 * 10)
                    .mat("chitin").box(-0.5f, -0.5f, -1.5f, 1, 1, 2).add();
            st(b, n + "_tail", n, glow).accent(glow).at(0, 0, 0.5f).mat("energy").glow("full").box(-0.5f, -0.5f, 0, 1, 1, 2, -0.05f).add();
            st(b, n + "_wings", n, wing).at(0, -0.5f, -0.5f).rot(0, 0, i % 2 == 0 ? 10 : -10).mat("membrane")
                    .box(-2.5f, -0.25f, -0.5f, 2, 1, 2, -0.3f).box(0.5f, -0.25f, -0.5f, 2, 1, 2, -0.3f).add();
        }
        return b.build();
    }

    /** Wall beast: a lurking cave-wall horror - a slab of rock with a bulging fanged face and two grasping arms. */
    static ModelSpec wallBeast(String id) {
        int rock = 0x6A625A;
        int dark = 0x433C36;
        int skin = 0x8A7A5A;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.6f);
        Kit.part(b, "wall", null, Role.STATIC, rock).accent(dark).at(0, 24, 4).mat("rock").box(-8, -30, 0, 16, 30, 4).add();
        float[][] bricks = {{-6, -27, 3}, {2, -24, 4}, {-5, -18, 4}, {4, -14, 3}, {-7, -9, 3}, {3, -5, 4}, {-2, -29, 5}, {5, -21, 3}};
        for (int i = 0; i < bricks.length; i++) {
            st(b, "stone" + i, "wall", i % 2 == 0 ? darker(rock, 0.85f) : mix(rock, 0x9A9288, 0.3f)).at(bricks[i][0], bricks[i][1], -0.5f)
                    .mat("rock").box(0, 0, -0.5f, (int) bricks[i][2], 2 + (i % 2), 1).add();
        }
        Kit.part(b, "body", "wall", Role.BODY, skin).accent(dark).at(0, -15, 0).mat("hide").box(-4.5f, -6, -3, 9, 10, 3).add();
        Kit.part(b, "head", "body", Role.HEAD, skin).accent(0xF0D040).at(0, -6, -1.5f).mat("hide").pattern("eyes").glow("eyes")
                .box(-3.5f, -6, -3.5f, 7, 6, 4).add();
        st(b, "brow", "head", dark).at(0, -4.2f, -3.6f).rot(14, 0, 0).mat("hide").box(-3.6f, -1, -0.6f, 7, 2, 2).add();
        Kit.part(b, "jaw", "head", Role.JAW, dark).at(0, 0, -2.5f).rot(10, 0, 0).mat("hide").box(-3, 0, -1.5f, 6, 2, 3).add();
        teeth(b, "teeth", "jaw", TOOTH, 0, -1.4f, 2.8f, 1, false, true);
        sy(b, "horn", "head", 0xB8AC8A).at(3, -5.5f, -1).rot(-10, 0, -40).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1, 0.15f).add();
        for (int s = -1; s <= 1; s += 2) {
            String a = s > 0 ? "arm_l" : "arm_r";
            Kit.part(b, a, "body", s > 0 ? Role.ARM_LEFT : Role.ARM_RIGHT, skin).accent(dark).at(s * 5, -4, -1).rot(-70, s * -20, s * -10)
                    .mat("hide").box(-1.5f, -1, -1.5f, 3, 7, 3).add();
            st(b, a + "_fore", a, skin).at(0, 6.5f, 0).rot(-30, 0, 0).mat("hide").box(-1.5f, 0, -1.5f, 3, 6, 3, -0.1f).add();
            st(b, a + "_hand", a + "_fore", dark).at(0, 6, 0).mat("hide").box(-2, 0, -2, 4, 2, 4).add();
            st(b, a + "_claws", a + "_hand", CLAW).at(0, 2, 0).rot(30, 0, 0).mat("horn").box(-2, 0, -2, 1, 3, 1, -0.1f)
                    .box(-0.5f, 0, -2, 1, 3, 1, -0.1f).box(1, 0, -2, 1, 3, 1, -0.1f).box(-0.5f, 0, 1, 1, 2, 1, -0.1f).add();
        }
        st(b, "rubble", "wall", dark).at(0, 0, -1).mat("rock").box(-7, -2, -3, 4, 2, 3).box(3, -1, -2, 3, 1, 2).box(-2, -1, -2, 2, 1, 2).add();
        return b.build();
    }

    /** Killerwatt: a crackling ball of storm energy with a fanged face, orbiting rings and zig-zag lightning limbs. */
    static ModelSpec killerwatt(String id) {
        int core = 0xF0E060;
        int blue = 0x60A0FF;
        int dark = 0x3A4A8A;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.4f);
        Kit.part(b, "body", null, Role.FLOAT, dark).accent(blue).at(0, 10, 0).mat("metal").box(-4, -4, -4, 8, 8, 8).add();
        Kit.part(b, "head", "body", Role.HEAD, blue).accent(0xFFFFFF).at(0, 0, 0).mat("energy").pattern("eyes").glow("full")
                .box(-3.5f, -3.5f, -4.6f, 7, 7, 1).add();
        Kit.part(b, "jaw", "head", Role.JAW, 0x1A1A3A).at(0, 1, -4.8f).mat("metal").box(-2.5f, 0, -0.3f, 5, 1, 1).add();
        for (int f = 0; f < 6; f++) {
            String n = "node" + f;
            float x = f == 0 ? 4.5f : f == 1 ? -4.5f : 0;
            float y = f == 2 ? -4.5f : f == 3 ? 4.5f : 0;
            float z = f == 4 ? 4.5f : f == 5 ? -4.2f : 0;
            if (f == 5) {
                continue;
            }
            st(b, n, "body", core).accent(core).at(x, y, z).mat("energy").glow("full").box(-1, -1, -1, 2, 2, 2).add();
        }
        Kit.part(b, "ring", "body", Role.SPIN, blue).accent(blue).at(0, 0, 0).mat("energy").glow("full")
                .box(-7, -0.5f, -7, 14, 1, 1, -0.2f).box(-7, -0.5f, 6, 14, 1, 1, -0.2f).box(-7, -0.5f, -6, 1, 1, 12, -0.2f)
                .box(6, -0.5f, -6, 1, 1, 12, -0.2f).add();
        Kit.part(b, "ring2", "body", Role.SPIN, core).accent(core).at(0, 0, 0).rot(60, 0, 30).mat("energy").glow("full")
                .box(-6, -0.5f, -0.5f, 12, 1, 1, -0.25f).box(-0.5f, -0.5f, -6, 1, 1, 12, -0.25f).add();
        for (int k = 0; k < 4; k++) {
            float a = k * 90 + 45;
            String n = "bolt" + k;
            Kit.part(b, n, "body", Role.TENTACLE, core).accent(core).at((float) Math.cos(Math.toRadians(a)) * 2.5f, 4, (float) Math.sin(Math.toRadians(a)) * 2.5f)
                    .rot(0, -a, 25).mat("energy").glow("full").box(-0.5f, 0, -0.5f, 1, 3, 1).add();
            st(b, n + "_b", n, blue).accent(blue).at(0, 3, 0).rot(0, 0, -50).mat("energy").glow("full").box(-0.5f, 0, -0.5f, 1, 3, 1).add();
            st(b, n + "_c", n + "_b", core).accent(core).at(0, 3, 0).rot(0, 0, 50).mat("energy").glow("full").box(-0.5f, 0, -0.5f, 1, 3, 1, -0.1f).add();
        }
        for (int k = 0; k < 4; k++) {
            float a = (float) Math.toRadians(k * 90);
            st(b, "arc" + k, "ring", k % 2 == 0 ? core : 0xFFFFFF).accent(core).at((float) Math.cos(a) * 6.5f, 0, (float) Math.sin(a) * 6.5f)
                    .rot(0, -k * 90, 45).mat("energy").glow("full").box(-0.5f, -1.5f, -0.5f, 1, 3, 1, -0.1f).add();
        }
        sy(b, "spark", "body", core).accent(core).at(3, -4.5f, -2).rot(0, 0, -30).mat("energy").glow("full").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        sy2(b, "spark_tip", "spark", blue).accent(blue).at(0, -3, 0).rot(0, 0, 50).mat("energy").glow("full").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        return b.build();
    }

    /** Molanisk: a hunched, mole-like digger with a fleshy pointed snout, tiny eyes and enormous shovel claws. */
    static ModelSpec molanisk(String id) {
        int fur = 0x5A4636;
        int dark = 0x342820;
        int pink = 0xD89A8A;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.5f);
        Kit.part(b, "body", null, Role.BODY, fur).accent(dark).at(0, 17, 1).rot(-10, 0, 0).mat("fur").box(-5, -5, -6, 10, 9, 12).add();
        st(b, "hump", "body", dark).at(0, -5, 1).mat("fur").box(-4, -1.5f, -4, 8, 2, 8).add();
        st(b, "belly", "body", mix(fur, 0xC8A888, 0.35f)).at(0, 4, -1).mat("fur").box(-3.5f, -0.6f, -4, 7, 1, 8).add();
        Kit.part(b, "head", "body", Role.HEAD, fur).accent(0x101010).at(0, -1, -6).rot(15, 0, 0).mat("fur").pattern("eyes_animal")
                .box(-3.5f, -3, -4, 7, 6, 4).add();
        String sn = chain(b, "snout", "head", Role.STATIC, pink, darker(pink, 0.7f), "skin", null, 0, 0.5f, -4, -1,
                new int[]{4, 3, 2}, new int[]{3, 2, 2}, new int[]{2, 2, 2}, new float[]{8, 8, 8}, null);
        st(b, "nose_star", sn, 0xE8A0A0).at(0, 0, -2).mat("skin").box(-1.5f, -1.5f, -1, 3, 3, 1, -0.1f).add();
        Kit.part(b, "jaw", "head", Role.JAW, dark).at(0, 2.5f, -3).mat("fur").box(-2, 0, -2.5f, 4, 1, 3).add();
        teeth(b, "teeth", "jaw", TOOTH, 0, -2.4f, 1.8f, 1, false, true);
        for (int s = -1; s <= 1; s += 2) {
            String a = s > 0 ? "arm_l" : "arm_r";
            Kit.part(b, a, "body", s > 0 ? Role.LEG_FRONT_LEFT : Role.LEG_FRONT_RIGHT, fur).accent(dark).at(s * 5, 1, -4)
                    .rot(-30, 0, s * -30).mat("fur").box(-1.5f, -1, -1.5f, 3, 5, 3).add();
            st(b, a + "_paw", a, pink).at(0, 4.5f, 0).rot(40, 0, s * 20).mat("skin").box(-2.5f, 0, -2, 5, 2, 4).add();
            st(b, a + "_claws", a + "_paw", 0xE8E0C8).at(0, 1, -2).rot(30, 0, 0).mat("horn").box(-2.5f, -0.5f, -3, 1, 1, 3)
                    .box(-1, -0.5f, -3.5f, 1, 1, 4).box(0.5f, -0.5f, -3.5f, 1, 1, 4).box(2, -0.5f, -3, 1, 1, 3).add();
        }
        Kit.sym(b, "leg", null, false, fur).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(dark).at(3.5f, 19, 4).rot(-20, 0, -10)
                .mat("fur").box(-2, -1, -2, 4, 4, 4).add();
        sy2(b, "foot", "leg", pink).at(0, 3.5f, 0).rot(20, 0, 10).mat("skin").box(-1.5f, 0, -3, 3, 1, 4).add();
        claws(b, "toe_claws", "foot", true, 0xE8E0C8, 3, 2, 0.5f, -2.8f, 1);
        String t = chain(b, "tail", "body", Role.TAIL, pink, darker(pink, 0.7f), "skin", null, 0, 2, 6, 1, new int[]{2, 1, 1},
                new int[]{2, 1, 1}, new int[]{2, 2, 2}, new float[]{30, 10, 10}, null);
        st(b, "tail_tip", t, darker(pink, 0.8f)).at(0, 0, 2).mat("skin").box(-0.5f, -0.5f, 0, 1, 1, 1).add();
        for (int k = 0; k < 4; k++) {
            st(b, "tuft" + k, "body", dark).at(-3 + k * 2, -5.5f, -3 + (k % 2) * 4).rot(-20, 0, (k - 1.5f) * 10).mat("fur")
                    .box(-0.5f, -1.5f, -0.5f, 1, 2, 1, 0.1f).add();
        }
        return b.build();
    }

    /** Basilisk: a heavy green lizard with a frilled crest, petrifying golden eyes, a horned snout, spines and eight legs' worth of menace in four. */
    static ModelSpec basilisk(String id) {
        int body = 0x4E7A3A;
        int dark = 0x2E4E22;
        int belly = 0xB8B070;
        int crest = 0xB04A2A;
        int horn = 0xD8CCA0;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.8f);
        Kit.part(b, "body", null, Role.BODY, body).accent(belly).at(0, 17.5f, 0).mat("scales").box(-5, -4, -8, 10, 8, 16).add();
        st(b, "belly", "body", belly).accent(darker(belly, 0.7f)).at(0, 4, 0).mat("plates").box(-4, -0.6f, -7.5f, 8, 1, 15).add();
        st(b, "shoulders", "body", body).accent(belly).at(0, -1, -7).mat("scales").box(-5.5f, -3.5f, -2, 11, 7, 4).add();
        for (int i = 0; i < 6; i++) {
            st(b, "spine" + i, "body", i % 2 == 0 ? crest : dark).at(0, -4, -7 + i * 2.8f).rot(-30, 0, 0).mat("horn")
                    .box(-0.5f, -2.5f + (i % 2), -1, 1, 3 - (i % 2), 2).add();
        }
        sy(b, "scute", "body", dark).at(5, -1, 0).mat("scales").box(-0.5f, -2, -6, 1, 4, 12).add();
        st(b, "neck", "body", body).accent(belly).at(0, -1, -8.5f).rot(-15, 0, 0).mat("scales").box(-3.5f, -3, -3, 7, 6, 4).add();
        Kit.part(b, "head", "neck", Role.HEAD, body).accent(0xFFD020).at(0, 0, -3).rot(15, 0, 0).mat("scales").pattern("eyes").glow("eyes")
                .box(-4, -3, -5, 8, 5, 5).add();
        st(b, "snout", "head", body).at(0, 0, -5).mat("scales").box(-2.5f, -2, -4, 5, 3, 4).add();
        st(b, "nose_horn", "snout", horn).at(0, -2, -3).rot(-30, 0, 0).mat("horn").box(-0.5f, -2, -0.5f, 1, 2, 1, 0.1f).add();
        Kit.part(b, "jaw", "head", Role.JAW, belly).at(0, 2, -4).rot(10, 0, 0).mat("plates").box(-2.5f, 0, -5, 5, 1, 6).add();
        teeth(b, "teeth", "snout", TOOTH, 1, -3.6f, 2.2f, 2, true, false);
        sy(b, "brow", "head", dark).at(3, -3, -4.5f).rot(15, -5, -15).mat("horn").box(-1, -1, -1, 2, 1, 4).add();
        for (int k = 0; k < 3; k++) {
            st(b, "crest" + k, "head", k == 1 ? crest : mix(crest, dark, 0.3f)).accent(horn).at(-2 + k * 2, -3, -1 + Math.abs(k - 1))
                    .rot(-35, 0, (k - 1) * -25).mat("membrane").box(-0.5f, -4 - (k == 1 ? 1 : 0), -1, 1, 4 + (k == 1 ? 1 : 0), 3).add();
        }
        sy(b, "frill", "head", crest).accent(horn).at(4, -1, 0).rot(0, -30, -10).mat("membrane").box(0, -2, 0, 1, 4, 3).add();
        float[][] lp = {{4.5f, 19, -5}, {4.5f, 19, 5}};
        Role[][] roles = {{Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT}, {Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT}};
        for (int i = 0; i < 2; i++) {
            String ln = i == 0 ? "leg_f" : "leg_b";
            Kit.sym(b, ln, null, false, body).roles(roles[i][0], roles[i][1]).accent(belly).at(lp[i][0], lp[i][1], lp[i][2])
                    .rot(0, 0, -30).mat("scales").box(-1.5f, -1.5f, -2, 5, 3, 4).add();
            sy2(b, ln + "_shin", ln, dark).at(3, 0.5f, 0).rot(0, 0, 40).mat("scales").box(-1.5f, 0, -1.5f, 3, 4, 3).add();
            sy2(b, ln + "_foot", ln + "_shin", dark).at(0, 3.5f, 0).rot(0, 0, -10).mat("scales").box(-2, 0, -3, 4, 1, 4).add();
            claws(b, ln + "_claws", ln + "_foot", true, horn, 3, 2.5f, 0.5f, -2.8f, 2);
        }
        String t = chain(b, "tail", "body", Role.TAIL, body, belly, "scales", null, 0, -0.5f, 8, 1, new int[]{6, 5, 4, 3, 2, 2},
                new int[]{6, 4, 3, 3, 2, 2}, new int[]{4, 4, 4, 4, 4, 3}, new float[]{6, 2, 0, -2, 0, 0}, new float[]{0, 10, 10, 6, -10, -10});
        for (int i = 0; i < 4; i += 1) {
            st(b, "tail_spine" + i, "tail" + i, crest).at(0, -2, 2).rot(-30, 0, 0).mat("horn").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        }
        st(b, "tail_tip", t, horn).at(0, 0, 3).mat("horn").box(-0.5f, -0.5f, 0, 1, 1, 2).add();
        return b.build();
    }

    /** Sea snake: a long, banded swimming serpent with a paddle tail, dorsal fin and a fanged, hooded head. */
    static ModelSpec seaSnake(String id) {
        int body = 0x2E6A7A;
        int band = 0xD8D0A0;
        int fin = 0x5AA0A8;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.5f);
        Kit.part(b, "body", null, Role.BODY, body).accent(band).at(0, 20, -4).mat("scales").pattern("stripes").box(-2.5f, -2.5f, -4, 5, 5, 8).add();
        Kit.part(b, "neck", "body", Role.NECK, body).accent(band).at(0, -1, -4).rot(-40, 0, 0).mat("scales").pattern("stripes")
                .box(-2, -2, -5, 4, 4, 5).add();
        st(b, "hood", "neck", fin).at(0, -1, -4).rot(30, 0, 0).mat("membrane").box(-3.5f, -1, -0.5f, 7, 4, 1).add();
        Kit.part(b, "head", "neck", Role.HEAD, body).accent(0xFFE040).at(0, 0, -5).rot(45, 0, 0).mat("scales").pattern("eyes").glow("eyes")
                .box(-2.5f, -2, -4, 5, 3, 4).add();
        st(b, "snout", "head", body).at(0, 0, -4).mat("scales").box(-1.5f, -1.5f, -2, 3, 2, 2).add();
        Kit.part(b, "jaw", "head", Role.JAW, band).at(0, 1, -3).rot(20, 0, 0).mat("plates").box(-1.5f, 0, -3, 3, 1, 3).add();
        sy(b, "fang", "snout", TOOTH).at(1, 0.5f, -1.5f).mat("horn").box(-0.5f, 0, -0.5f, 1, 2, 1, -0.15f).add();
        st(b, "tongue", "jaw", 0xC03040).at(0, 0, -3).rot(10, 0, 0).mat("flesh").box(-0.5f, -0.5f, -3, 1, 1, 3, -0.2f).add();
        sy(b, "brow", "head", darker(body, 0.7f)).at(1.8f, -2, -3.5f).rot(10, 0, -12).mat("scales").box(-1, -0.5f, -0.5f, 2, 1, 3).add();
        sy(b, "nostril", "snout", 0x102028).at(0.8f, -1.5f, -1.8f).mat("scales").box(-0.5f, -0.3f, -0.5f, 1, 1, 1, -0.2f).add();
        st(b, "crest_fin", "neck", fin).at(0, -2, -2.5f).rot(-20, 0, 0).mat("membrane").box(-0.5f, -2, -2, 1, 2, 4).add();
        int[] w = {5, 5, 4, 4, 3, 3, 2, 2};
        int[] l = {4, 4, 4, 4, 4, 4, 3, 3};
        float[] yaw = {25, 25, 15, -20, -30, -25, 15, 25};
        float[] pit = {0, 0, 0, 0, 0, 0, 0, 0};
        String t = chain(b, "tail", "body", Role.TAIL, body, band, "scales", null, 0, 0, 4, 1, w, w, l, pit, yaw);
        for (int i = 0; i < 8; i++) {
            st(b, "belly" + i, "tail" + i, band).at(0, w[i] / 2f, 2).mat("plates").box(-w[i] / 2f + 0.5f, -0.6f, -2, w[i] - 1, 1, 4).add();
        }
        for (int i = 0; i < 8; i += 2) {
            st(b, "fin" + i, "tail" + i, fin).at(0, -w[i] / 2f, 2).mat("membrane").box(-0.5f, -2, -1.5f, 1, 2, 3).add();
        }
        st(b, "paddle", t, fin).at(0, 0, 3).mat("membrane").box(-0.5f, -3, 0, 1, 6, 4).add();
        return b.build();
    }

    /** Fever spider: a bristly orange-brown spider with a striped abdomen, red eye cluster and venomous fangs. */
    static ModelSpec feverSpider(String id) {
        int body = 0x8A4A20;
        int dark = 0x3A1E10;
        int stripe = 0xE09A40;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.6f);
        Kit.part(b, "body", null, Role.BODY, body).accent(stripe).at(0, 19, -1).mat("fur").box(-3.5f, -2.5f, -3.5f, 7, 5, 6).add();
        st(b, "abdomen", "body", body).accent(stripe).at(0, -1, 2.5f).rot(-15, 0, 0).mat("fur").pattern("stripes").box(-5, -4, 0, 10, 8, 10).add();
        st(b, "abdomen_tip", "abdomen", dark).at(0, 0, 10).mat("fur").box(-2.5f, -2, -1, 5, 4, 2).add();
        st(b, "mark", "abdomen", stripe).at(0, -4, 4).mat("fur").box(-2, -0.6f, -2, 4, 1, 5).add();
        for (int k = 0; k < 4; k++) {
            st(b, "bristle" + k, "abdomen", dark).at(-3 + k * 2, -4, 2 + k * 1.5f).rot(-30, 0, (k - 1.5f) * 15).mat("fur")
                    .box(-0.5f, -2, -0.5f, 1, 2, 1, -0.1f).add();
        }
        Kit.part(b, "head", "body", Role.HEAD, dark).accent(0xFF2020).at(0, 0, -3.5f).mat("fur").pattern("eyes").glow("eyes")
                .box(-2.5f, -2, -2.5f, 5, 4, 3).add();
        Kit.part(b, "eyes", "head", Role.STATIC, 0x200808).accent(0xFF3030).at(0, -2, -1.5f).mat("energy").glow("full")
                .box(-1.5f, -0.6f, -0.6f, 1, 1, 1, -0.1f).box(0.5f, -0.6f, -0.6f, 1, 1, 1, -0.1f).box(-0.5f, -0.9f, 0.3f, 1, 1, 1, -0.15f).add();
        sy(b, "fang", "head", 0x1A1010).at(1, 2, -2.2f).rot(20, 0, 10).mat("horn").box(-0.5f, 0, -0.5f, 1, 2, 1, -0.05f).add();
        sy(b, "palp", "head", body).at(2, 1, -2.5f).rot(30, -20, 0).mat("fur").box(-0.5f, -0.5f, -2, 1, 1, 2).add();
        for (int i = 0; i < 4; i++) {
            bugLeg(b, "leg" + i, "body", body, dark, "fur", 3, 0, -2.5f + i * 1.7f, 45 - i * 30, 5, 6, 3, 1);
        }
        return b.build();
    }

    /** Brine rat: a big, hairless, salt-crusted grey rat with pink ears and feet and a long scaly tail. */
    static ModelSpec brineRat(String id) {
        int skin = 0x9A9A92;
        int dark = 0x5E5E58;
        int pink = 0xD8A0A0;
        int salt = 0xE8E8E0;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.5f);
        Kit.part(b, "body", null, Role.BODY, skin).accent(dark).at(0, 18, 1).mat("skin").box(-3.5f, -3.5f, -6, 7, 7, 12).add();
        st(b, "hunch", "body", skin).at(0, -3.5f, 1).rot(-8, 0, 0).mat("skin").box(-3, -1.5f, -4, 6, 2, 8).add();
        for (int k = 0; k < 5; k++) {
            st(b, "salt" + k, "body", salt).at(-2.5f + (k * 1.3f) % 5, -4.5f + (k % 2), -4 + k * 2).mat("rock").box(-0.75f, -0.5f, -0.75f, 1, 1, 1, 0.25f).add();
        }
        st(b, "rib", "body", dark).at(0, 1, -2).mat("skin").box(-3.6f, -1, -0.5f, 7, 1, 1, 0.05f).box(-3.6f, -1, 1.5f, 7, 1, 1, 0.05f).add();
        Kit.part(b, "head", "body", Role.HEAD, skin).accent(0xFF3030).at(0, -1, -6).rot(10, 0, 0).mat("skin").pattern("eyes").glow("eyes")
                .box(-2.5f, -2.5f, -4, 5, 5, 4).add();
        st(b, "snout", "head", skin).at(0, 0.5f, -4).rot(8, 0, 0).mat("skin").box(-1.5f, -1.5f, -3, 3, 3, 3).add();
        st(b, "nose", "snout", pink).at(0, -1, -3).mat("skin").box(-1, -0.5f, -0.6f, 2, 1, 1).add();
        sy(b, "whisker", "snout", salt).at(1.5f, 0, -2).rot(0, -30, 0).mat("hair").box(0, -0.5f, -0.5f, 3, 1, 1, -0.4f).add();
        Kit.part(b, "jaw", "head", Role.JAW, dark).at(0, 2, -3).mat("skin").box(-1, 0, -3.5f, 2, 1, 3).add();
        sy(b, "incisor", "snout", TOOTH).at(0.4f, 1.5f, -2.7f).mat("horn").box(-0.5f, 0, -0.5f, 1, 2, 1, -0.15f).add();
        sy(b, "ear", "head", pink).at(2, -2.5f, -1).rot(-10, -20, -20).mat("skin").box(-1, -2.5f, -0.5f, 2, 3, 1).add();
        float[][] lp = {{2.5f, 20, -4}, {2.5f, 19.5f, 4}};
        Role[][] roles = {{Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT}, {Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT}};
        for (int i = 0; i < 2; i++) {
            String ln = i == 0 ? "leg_f" : "leg_b";
            Kit.sym(b, ln, null, false, skin).roles(roles[i][0], roles[i][1]).accent(dark).at(lp[i][0], lp[i][1], lp[i][2])
                    .rot(i == 0 ? 10 : -25, 0, 0).mat("skin").box(-1.5f, -1, -1.5f, 3, i == 0 ? 3 : 4, 3).add();
            sy2(b, ln + "_shin", ln, skin).at(0, i == 0 ? 2.5f : 3, 0).rot(i == 0 ? -10 : 40, 0, 0).mat("skin").box(-1, 0, -1, 2, 2, 2).add();
            sy2(b, ln + "_paw", ln + "_shin", pink).at(0, 1.5f, 0).rot(i == 0 ? 0 : -15, 0, 0).mat("skin").box(-1, 0, -2.5f, 2, 1, 3).add();
            claws(b, ln + "_claws", ln + "_paw", true, CLAW, 2, 1, 0.5f, -2.3f, 1);
        }
        chain(b, "tail", "body", Role.TAIL, pink, darker(pink, 0.7f), "skin", null, 0, 0, 6, 1, new int[]{2, 1, 1, 1, 1},
                new int[]{2, 1, 1, 1, 1}, new int[]{4, 4, 4, 4, 3}, new float[]{-10, 10, 10, 6, 4}, new float[]{0, 12, 12, -12, -14});
        return b.build();
    }
}
