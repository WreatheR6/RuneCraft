package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.BestiaryWldFig.dk;
import static com.runecraft.osrs.content.bestiary.BestiaryWldFig.side;
import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/** Farm animals, vermin and wild beasts of Gielinor, sculpted after their OSRS models. */
final class BestiaryWldBeasts {
    private BestiaryWldBeasts() {
    }

    static List<ModelSpec> all() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(cow("cow"));
        l.add(chicken("chicken"));
        l.add(sheep("sheep"));
        l.add(pig("pig"));
        l.add(rat("giant_rat", 0x6A5848, 0xE0A0A0, 1.0f));
        l.add(spider("giant_spider", 0x2E2420, 0x8A2A1A, 0xFF3020, 1.0f));
        l.add(spider("spider", 0x5A4A3A, 0xC08040, 0xFF6020, 0.55f));
        l.add(spider("poison_spider", 0x3A5A26, 0xA0D040, 0xC0FF40, 1.0f));
        l.add(wolf("wolf", 0xC8C8C4, 0x8A8A88, 0xE8E8E0));
        l.add(wolf("wild_dog", 0x8A6A4A, 0x5A4430, 0xC8A880));
        l.add(yak("yak"));
        return l;
    }

    /** Ones that replace models the slayer module defines. */
    static List<ModelSpec> overrides() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(bear("grizzly_bear"));
        l.add(bat("giant_bat"));
        l.add(scorpion("scorpion"));
        l.add(monkey("monkey"));
        return l;
    }

    // ------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------

    /**
     * A jointed leg standing on the ground: upper segment (walk role) tilted by {@code p1}, lower segment bent by
     * {@code p2}, then a levelled foot. The hip pivot height is solved so the foot sole lands on Y=24.
     * Foot styles: hoof (split hoof), paw (pad + three claws), bear (wide pad + four claws), plain.
     */
    static void leg(ModelSpec.Builder b, String name, boolean left, Role role, float x, float z, int upCol, int lowCol, int footCol,
                    String mat, int t, int upper, int lower, float p1, float p2, String foot, int accent) {
        int footH = foot.equals("hoof") ? 2 : 1;
        double a1 = Math.toRadians(p1);
        double a12 = Math.toRadians(p1 + p2);
        float y = (float) (24 - footH - upper * Math.cos(a1) - lower * Math.cos(a12));
        Kit.P up = part(b, name, null, role, upCol).at(x, y, z).rot(p1, 0, 0).mat(mat)
                .box(-t / 2f, -2, -t / 2f, t, upper + 2, t);
        if (accent >= 0) {
            up.accent(accent);
        }
        side(up, left);
        int lt = Math.max(2, t - 1);
        Kit.P low = part(b, name + "_low", name + "*", Role.STATIC, lowCol).at(0, upper, 0).rot(p2, 0, 0).mat(mat)
                .box(-lt / 2f, -0.5f, -lt / 2f, lt, lower + 1, lt);
        if (accent >= 0) {
            low.accent(accent);
        }
        side(low, left);
        String fp = name + "_low*";
        float lev = -(p1 + p2);
        switch (foot) {
            case "hoof" -> {
                side(part(b, name + "_hoof", fp, Role.STATIC, footCol).at(0, lower, 0).rot(lev, 0, 0).mat("horn")
                        .box(-t / 2f, 0, -t / 2f - 0.5f, t, 2, t + 1, 0.05f), left);
                side(part(b, name + "_cleft", name + "_hoof*", Role.STATIC, dk(footCol, 0.5f)).at(0, 0.5f, -t / 2f - 0.6f).mat("horn")
                        .box(-0.5f, 0, -0.1f, 1, 2, 1, -0.2f), left);
                side(part(b, name + "_fetlock", fp, Role.STATIC, dk(lowCol, 0.9f)).at(0, lower - 1.5f, 0.5f).mat(mat)
                        .box(-lt / 2f, 0, -lt / 2f, lt, 2, lt, 0.3f), left);
            }
            case "paw", "bear" -> {
                boolean bear = foot.equals("bear");
                int pw = bear ? t + 1 : t;
                side(part(b, name + "_paw", fp, Role.STATIC, footCol).at(0, lower, 0).rot(lev, 0, 0).mat(mat)
                        .box(-pw / 2f, -0.5f, -pw / 2f - 1, pw, 2, pw + 1), left);
                int n = bear ? 4 : 3;
                for (int k = 0; k < n; k++) {
                    float cx = -pw / 2f + 0.5f + k * (pw - 1f) / (n - 1);
                    side(part(b, name + "_claw" + k, name + "_paw*", Role.STATIC, bear ? 0x2A2420 : 0x3A3430).at(cx, 1, -pw / 2f - 1)
                            .rot(25, 0, 0).mat("horn").box(-0.5f, -0.5f, -1.2f, 1, 1, 2, -0.15f), left);
                }
            }
            default -> side(part(b, name + "_foot", fp, Role.STATIC, footCol).at(0, lower, 0).rot(lev, 0, 0).mat(mat)
                    .box(-t / 2f, -0.5f, -t / 2f - 0.5f, t, 2, t + 1), left);
        }
    }

    static void legPair(ModelSpec.Builder b, String name, boolean front, float x, float z, int upCol, int lowCol, int footCol,
                        String mat, int t, int upper, int lower, float p1, float p2, String foot, int accent) {
        leg(b, name, true, front ? Role.LEG_FRONT_LEFT : Role.LEG_BACK_LEFT, x, z, upCol, lowCol, footCol, mat, t, upper, lower, p1, p2, foot, accent);
        leg(b, name, false, front ? Role.LEG_FRONT_RIGHT : Role.LEG_BACK_RIGHT, x, z, upCol, lowCol, footCol, mat, t, upper, lower, p1, p2, foot, accent);
    }

    // ------------------------------------------------------------------------------------------
    // Farm animals
    // ------------------------------------------------------------------------------------------

    /** OSRS cow: boxy black-and-white body, huge head with a broad pink muzzle, stubby horns, floppy ears and an udder. */
    static ModelSpec cow(String id) {
        int white = 0xECEAE2;
        int black = 0x1E1C1C;
        int pink = 0xE6A6A0;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.8f);
        part(b, "body", null, Role.BODY, white).accent(black).at(0, 12, 1).mat("patches")
                .box(-6, -6, -10, 12, 11, 20).add();
        part(b, "withers", "body", Role.STATIC, white).accent(black).at(0, -6, -6).rot(-6, 0, 0).mat("patches")
                .box(-5, -1.5f, -4, 10, 2, 8).add();
        part(b, "rump", "body", Role.STATIC, white).accent(black).at(0, -6, 6).mat("patches")
                .box(-5.5f, -1, -3, 11, 2, 7).add();
        sym(b, "hipbone", "rump", false, dk(white, 0.95f)).accent(black).at(4, -1, 1).mat("patches").box(-1, -1, -1.5f, 2, 1, 3).add();
        part(b, "belly", "body", Role.STATIC, white).accent(black).at(0, 5, 0).mat("patches").box(-5, -1, -7, 10, 2, 13).add();
        part(b, "brisket", "body", Role.STATIC, white).accent(black).at(0, 1, -10).rot(10, 0, 0).mat("patches")
                .box(-3.5f, -2, -2, 7, 6, 3).add();
        part(b, "udder", "body", Role.STATIC, pink).at(0, 6, 5).mat("skin").box(-2.5f, -1, -2.5f, 5, 3, 5).add();
        for (int k = 0; k < 4; k++) {
            part(b, "teat" + k, "udder", Role.STATIC, dk(pink, 0.9f)).at(k % 2 == 0 ? -1.2f : 1.2f, 2, k < 2 ? -1.2f : 1.2f).mat("skin")
                    .box(-0.5f, 0, -0.5f, 1, 1, 1).add();
        }
        // neck and the big OSRS head
        part(b, "neck", "body", Role.NECK, white).accent(black).at(0, -2, -9).rot(-10, 0, 0).mat("patches")
                .box(-4, -4, -5, 8, 8, 6).add();
        part(b, "head", "neck", Role.HEAD, white).accent(black).at(0, -1, -4).rot(14, 0, 0).mat("patches").pattern("eyes_animal")
                .box(-5, -5, -8, 10, 9, 8).add();
        part(b, "poll", "head", Role.STATIC, black).at(0, -5, -4).mat("hair").box(-3, -1.5f, -2.5f, 6, 2, 5).add();
        part(b, "muzzle", "head", Role.STATIC, pink).at(0, 1.5f, -8).mat("skin").box(-4.5f, -2.5f, -3, 9, 5, 4).add();
        part(b, "lip", "muzzle", Role.STATIC, dk(pink, 0.85f)).at(0, 2.5f, -0.5f).mat("skin").box(-4, 0, -2.5f, 8, 1, 3).add();
        sym(b, "nostril", "muzzle", false, 0x7A4040).at(2, -0.8f, -3).mat("flesh").box(-1, -0.5f, -0.4f, 2, 1, 1, -0.1f).add();
        sym(b, "horn", "head", false, 0xE8DEC0).at(4, -4.5f, -4).rot(0, 0, -70).mat("horn").box(-1, -2.5f, -1, 2, 3, 2).add();
        sym(b, "horn_tip", "horn", true, 0xC8B890).at(0, -2.5f, 0).rot(-20, 0, 50).mat("horn").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        sym(b, "ear", "head", false, white).accent(black).at(5, -2, -2.5f).rot(0, -25, 20).mat("patches").box(0, -1, -1.5f, 4, 2, 3).add();
        sym(b, "ear_in", "ear", true, pink).at(1, -0.6f, -1.6f).mat("skin").box(0, 0, -0.1f, 2, 1, 1, -0.1f).add();
        part(b, "tail", "body", Role.TAIL, white).at(0, -4.5f, 10).rot(14, 0, 0).mat("hair").box(-0.5f, 0, -0.5f, 1, 9, 1).add();
        part(b, "tail_tuft", "tail", Role.STATIC, black).at(0, 9, 0).mat("hair").box(-1, 0, -1, 2, 3, 2).add();
        legPair(b, "leg_f", true, 4, -7, white, white, 0x3A3030, "patches", 4, 5, 5, 0, 0, "hoof", black);
        legPair(b, "leg_b", false, 4, 8, white, white, 0x3A3030, "patches", 4, 5, 5, -8, 8, "hoof", black);
        return b.build();
    }

    /** OSRS chicken: plump white body, layered brown-tipped wings, a fanned tail, red comb and wattle, yellow beak and feet. */
    static ModelSpec chicken(String id) {
        int body = 0xF0E8D6;
        int brown = 0xB08050;
        int red = 0xD82020;
        int yellow = 0xE8B030;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.35f);
        part(b, "body", null, Role.BODY, body).accent(brown).at(0, 17, 0).mat("feathers").box(-3.5f, -3.5f, -4.5f, 7, 7, 9).add();
        part(b, "breast", "body", Role.STATIC, body).at(0, -1, -4.5f).rot(-10, 0, 0).mat("feathers").box(-3, -2, -1.5f, 6, 6, 2).add();
        part(b, "back", "body", Role.STATIC, dk(body, 0.96f)).at(0, -3.5f, 1).rot(-8, 0, 0).mat("feathers").box(-3, -1, -3, 6, 2, 6).add();
        part(b, "fluff", "body", Role.STATIC, dk(body, 0.92f)).at(0, 3, 2).mat("feathers").box(-3, -1, -2, 6, 2, 4).add();
        for (boolean left : new boolean[]{true, false}) {
            side(part(b, "wing", "body", left ? Role.WING_LEFT : Role.WING_RIGHT, dk(body, 0.95f)).accent(brown).at(3.5f, -2.5f, -3)
                    .rot(-6, 0, 0).mat("feathers").box(0, 0, 0, 1, 4, 6), left);
            side(part(b, "primaries", "wing*", Role.STATIC, brown).at(0.4f, 2.5f, 3).rot(-14, 0, 0).mat("feathers")
                    .box(0, 0, 0, 1, 2, 5), left);
        }
        for (int i = 0; i < 3; i++) {
            part(b, "tailfeather" + i, "body", Role.TAIL, i == 1 ? dk(brown, 0.85f) : brown).at(-1.5f + i * 1.5f, -2, 4)
                    .rot(-35 - (i == 1 ? 10 : 0), 0, -20 + i * 20).mat("feathers").box(-1, -5, -0.5f, 2, 5, 2).add();
        }
        part(b, "neck", "body", Role.NECK, body).at(0, -3, -3).rot(-10, 0, 0).mat("feathers").box(-2, -3, -2, 4, 3, 4).add();
        part(b, "head", "neck", Role.HEAD, body).at(0, -3, 0).mat("feathers").pattern("eyes_animal").box(-2, -4, -2.5f, 4, 4, 4).add();
        part(b, "beak", "head", Role.STATIC, yellow).at(0, -2.2f, -2.5f).mat("horn").box(-1, 0, -2, 2, 1, 2).add();
        part(b, "beak_low", "head", Role.JAW, dk(yellow, 0.85f)).at(0, -1.2f, -2.5f).mat("horn").box(-0.5f, 0, -1.5f, 1, 1, 2, -0.1f).add();
        part(b, "comb", "head", Role.STATIC, red).at(0, -4, -0.5f).mat("flesh")
                .box(-0.5f, -1, -2, 1, 1, 4).box(-0.5f, -2, -1.5f, 1, 1, 1).box(-0.5f, -2, 0.5f, 1, 1, 1).add();
        part(b, "wattle", "head", Role.STATIC, red).at(0, -1, -2.5f).mat("flesh").box(-0.5f, 0, -1, 1, 2, 1).add();
        for (boolean left : new boolean[]{true, false}) {
            side(part(b, "thigh", "body", left ? Role.LEG_FRONT_LEFT : Role.LEG_FRONT_RIGHT, dk(body, 0.95f)).at(1.5f, 3, 0.5f)
                    .mat("feathers").box(-1.5f, -1, -1.5f, 3, 2, 3), left);
            side(part(b, "shank", "thigh*", Role.STATIC, yellow).at(0, 1, 0).mat("horn").box(-0.5f, 0, -0.5f, 1, 3, 1), left);
            side(part(b, "foot", "shank*", Role.STATIC, yellow).at(0, 3, 0).mat("horn")
                    .box(-1.5f, 0, -2, 1, 1, 2, -0.1f).box(-0.5f, 0, -2.5f, 1, 1, 3, -0.1f).box(0.5f, 0, -2, 1, 1, 2, -0.1f)
                    .box(-0.5f, 0, 0, 1, 1, 1, -0.1f), left);
        }
        return b.build();
    }

    /** OSRS sheep: a lumpy cloud of wool clumps on thin dark legs, with a bare tan face and sideways ears. */
    static ModelSpec sheep(String id) {
        int wool = 0xEEEAE0;
        int face = 0xD0C4B0;
        int leg = 0x4A403A;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.7f);
        part(b, "body", null, Role.BODY, wool).at(0, 13, 1).mat("wool").box(-5.5f, -5.5f, -8, 11, 10, 16).add();
        part(b, "fleece_top", "body", Role.STATIC, dk(wool, 1.02f)).at(0, -5.5f, 0).mat("wool")
                .box(-4.5f, -1.5f, -6.5f, 9, 2, 13).add();
        part(b, "clump_a", "fleece_top", Role.STATIC, wool).at(-2, -1.5f, -3).rot(0, 20, 8).mat("wool").box(-2, -1.5f, -2, 4, 2, 4).add();
        part(b, "clump_b", "fleece_top", Role.STATIC, dk(wool, 0.97f)).at(2.5f, -1.2f, 2).rot(0, -15, -8).mat("wool").box(-2, -1.5f, -2, 4, 2, 4).add();
        part(b, "clump_c", "fleece_top", Role.STATIC, wool).at(-1, -1, 5).rot(0, 35, 0).mat("wool").box(-2, -1, -2, 4, 2, 3).add();
        sym(b, "flank", "body", false, dk(wool, 0.97f)).at(5.5f, 0, 0).mat("wool")
                .box(-1, -4, -6.5f, 2, 7, 13).add();
        sym(b, "flank_clump", "flank", true, wool).at(1, -1, -3).rot(0, 0, 10).mat("wool").box(-0.5f, -2, -2, 2, 4, 4).add();
        part(b, "rump", "body", Role.STATIC, wool).at(0, -1, 8).mat("wool").box(-4.5f, -4, -1, 9, 8, 3).add();
        part(b, "chest", "body", Role.STATIC, wool).at(0, 0, -8).mat("wool").box(-4.5f, -4, -2, 9, 8, 3).add();
        part(b, "belly", "body", Role.STATIC, dk(wool, 0.92f)).at(0, 4.5f, 0).mat("wool").box(-4.5f, -1, -6, 9, 2, 12).add();
        part(b, "tail", "rump", Role.TAIL, wool).at(0, -2, 2).rot(20, 0, 0).mat("wool").box(-1.5f, 0, -1, 3, 3, 2).add();
        part(b, "neck", "body", Role.NECK, wool).at(0, -3, -9).rot(-15, 0, 0).mat("wool").box(-3, -3, -3, 6, 6, 4).add();
        part(b, "head", "neck", Role.HEAD, face).at(0, -1, -3).rot(18, 0, 0).mat("skin").pattern("eyes_animal")
                .box(-2.5f, -3, -5, 5, 6, 5).add();
        part(b, "snout", "head", Role.STATIC, dk(face, 0.95f)).at(0, 1, -5).mat("skin").box(-2, -1.5f, -1.5f, 4, 3, 2).add();
        part(b, "nose", "snout", Role.STATIC, 0x3A3030).at(0, -0.8f, -1.6f).mat("skin").box(-1, 0, -0.3f, 2, 1, 1, -0.1f).add();
        part(b, "mouth", "snout", Role.JAW, dk(face, 0.8f)).at(0, 1, -1).mat("skin").box(-1.5f, 0, -0.6f, 3, 1, 1).add();
        part(b, "woolcap", "head", Role.STATIC, wool).at(0, -3, -1.5f).mat("wool").box(-3, -1.5f, -2.5f, 6, 2, 5).add();
        sym(b, "ear", "head", false, face).at(2.5f, -2, -2).rot(0, -20, 25).mat("skin").box(0, -0.5f, -1, 3, 1, 2).add();
        legPair(b, "leg_f", true, 3, -5, wool, leg, 0x2A2420, "skin", 3, 4, 5, 0, 0, "hoof", -1);
        legPair(b, "leg_b", false, 3, 6, wool, leg, 0x2A2420, "skin", 3, 4, 5, -6, 6, "hoof", -1);
        return b.build();
    }

    static ModelSpec pig(String id) {
        int pink = 0xE8A8A0;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.7f);
        part(b, "body", null, Role.BODY, pink).at(0, 15, 1).mat("skin").box(-5, -4.5f, -8, 10, 9, 16).add();
        part(b, "back", "body", Role.STATIC, dk(pink, 0.97f)).at(0, -4.5f, 0).mat("skin").box(-4, -1, -6.5f, 8, 2, 12).add();
        part(b, "jowl", "body", Role.STATIC, pink).at(0, 2, -8).mat("skin").box(-4, -3, -1.5f, 8, 5, 2).add();
        part(b, "belly", "body", Role.STATIC, dk(pink, 0.93f)).at(0, 4.5f, 0).mat("skin").box(-4, -1, -6, 8, 2, 11).add();
        part(b, "head", "body", Role.HEAD, pink).at(0, -1, -9).mat("skin").pattern("eyes_animal").box(-4, -4, -5, 8, 7, 5).add();
        part(b, "snout", "head", Role.STATIC, dk(pink, 0.92f)).at(0, 0.5f, -5).mat("skin").box(-2.5f, -1.5f, -2, 5, 4, 2).add();
        sym(b, "nostril", "snout", false, 0x8A5050).at(1, 0, -2).mat("flesh").box(-0.5f, -0.5f, -0.3f, 1, 1, 1, -0.1f).add();
        part(b, "jaw", "head", Role.JAW, dk(pink, 0.88f)).at(0, 3, -3).mat("skin").box(-2.5f, 0, -3.5f, 5, 1, 4).add();
        sym(b, "ear", "head", false, pink).at(3, -4, -2).rot(-50, 0, -20).mat("skin").box(-1.5f, -3, -0.5f, 3, 3, 1).add();
        part(b, "tail", "body", Role.TAIL, pink).at(0, -3, 8).rot(-30, 0, 0).mat("skin").box(-0.5f, -2, 0, 1, 2, 1).add();
        part(b, "tail_curl", "tail", Role.STATIC, pink).at(0, -2, 0.5f).rot(-80, 0, 0).mat("skin").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        part(b, "tail_tip", "tail_curl", Role.STATIC, pink).at(0, -2, 0).rot(-80, 0, 0).mat("skin").box(-0.5f, -1, -0.5f, 1, 1, 1).add();
        legPair(b, "leg_f", true, 3, -5, pink, pink, 0x6A4A40, "skin", 3, 2, 2, 0, 0, "hoof", -1);
        legPair(b, "leg_b", false, 3, 5, pink, pink, 0x6A4A40, "skin", 3, 2, 2, 0, 0, "hoof", -1);
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Vermin
    // ------------------------------------------------------------------------------------------

    /** OSRS giant rat: hunched, bristly body in stacked segments, wedge snout with incisors and whiskers, round pink ears, long ringed tail. */
    static ModelSpec rat(String id, int fur, int pink, float scale) {
        ModelSpec.Builder b = ModelSpec.builder(id).scale(scale).shadow(0.6f);
        part(b, "body", null, Role.BODY, fur).at(0, 17, 1).mat("fur").box(-4, -4, -5, 8, 7, 10).add();
        part(b, "hunch", "body", Role.STATIC, dk(fur, 0.92f)).at(0, -4, 1).rot(-10, 0, 0).mat("fur").box(-3.5f, -2, -4, 7, 3, 8).add();
        part(b, "haunch", "body", Role.STATIC, fur).at(0, -1, 5).mat("fur").box(-4.5f, -3, -2, 9, 6, 4).add();
        part(b, "belly", "body", Role.STATIC, dk(fur, 1.15f)).at(0, 3, 0).mat("fur").box(-3, -1, -4, 6, 2, 8).add();
        part(b, "shoulders", "body", Role.STATIC, fur).at(0, -1, -5).rot(10, 0, 0).mat("fur").box(-3.5f, -3, -2, 7, 6, 3).add();
        part(b, "head", "shoulders", Role.HEAD, fur).at(0, -1, -2).rot(8, 0, 0).mat("fur").pattern("eyes").accent(0xE02020)
                .box(-3, -3, -4, 6, 5, 4).add();
        part(b, "snout", "head", Role.STATIC, fur).at(0, 0, -4).rot(8, 0, 0).mat("fur").box(-2, -2, -3, 4, 3, 3).add();
        part(b, "snout_tip", "snout", Role.STATIC, dk(fur, 0.95f)).at(0, -0.5f, -3).mat("fur").box(-1.5f, -1, -2, 3, 2, 2).add();
        part(b, "nose", "snout_tip", Role.STATIC, pink).at(0, -1, -2).mat("skin").box(-1, -0.2f, -0.5f, 2, 1, 1).add();
        part(b, "jaw", "snout", Role.JAW, dk(fur, 0.85f)).at(0, 1, -0.5f).mat("fur").box(-1.5f, 0, -3.5f, 3, 1, 3).add();
        part(b, "incisors", "snout_tip", Role.STATIC, 0xF4E8B0).at(0, 1, -1.6f).mat("bone").box(-1, 0, -0.5f, 2, 2, 1, -0.1f).add();
        sym(b, "whisker", "snout_tip", false, 0x1A1A1A).at(1.5f, 0, -1).rot(0, -25, 8).mat("hair").box(0, -0.5f, -0.5f, 4, 1, 1, -0.4f).add();
        sym(b, "whisker_b", "snout_tip", false, 0x1A1A1A).at(1.5f, 0.6f, -1).rot(0, -10, -10).mat("hair").box(0, -0.5f, -0.5f, 4, 1, 1, -0.4f).add();
        sym(b, "ear", "head", false, pink).at(2.5f, -3, -1).rot(-10, -25, -20).mat("skin").box(-1.5f, -3, -0.5f, 3, 3, 1).add();
        sym(b, "ear_rim", "ear", true, fur).at(0, -3, 0.2f).mat("fur").box(-1.5f, -0.5f, -0.5f, 3, 1, 1, 0.05f).add();
        // ringed tail in four curving segments
        String prev = "haunch";
        float[] pitches = {-30, 25, 25, 20};
        for (int i = 0; i < 4; i++) {
            String n = "tail" + i;
            part(b, n, prev, i == 0 ? Role.TAIL : Role.STATIC, dk(pink, 1 - i * 0.04f)).at(0, i == 0 ? -1 : 0, i == 0 ? 2 : 4)
                    .rot(pitches[i], i == 0 ? 0 : 8, 0).mat("skin").box(-0.5f, -0.5f, 0, i < 2 ? 2 : 1, 1, 4, i < 2 ? -0.2f : 0f).add();
            prev = n;
        }
        legPair(b, "leg_f", true, 3, -4, fur, fur, pink, "fur", 2, 3, 3, 10, -20, "paw", -1);
        legPair(b, "leg_b", false, 3.5f, 4, fur, fur, pink, "fur", 3, 3, 2, -30, 40, "paw", -1);
        return b.build();
    }

    /**
     * An arachnid leg: a coxa yawed out from the body (gait role), a femur rolled up into a high knee, a tibia rolled back
     * down to the ground and a short tarsus. Built along +X for the left side and mirrored for the right.
     */
    static void arachLeg(ModelSpec.Builder b, String name, boolean left, String parent, float x, float y, float z, float yaw,
                         int femur, float femurRoll, int tibia, float tibiaRoll, int col, int col2, String mat) {
        side(part(b, name, parent, Role.LEG_EXTRA, col).at(x, y, z).rot(0, yaw, 0).mat(mat).box(-0.5f, -1, -1, 2, 2, 2), left);
        side(part(b, name + "_femur", name + "*", Role.STATIC, col).at(1.2f, 0, 0).rot(0, 0, femurRoll).mat(mat)
                .box(0, -1, -1, femur, 2, 2).box(1, -1.5f, -0.5f, femur - 2, 1, 1), left);
        side(part(b, name + "_knee", name + "_femur*", Role.STATIC, col2).at(femur, 0, 0).mat(mat).box(-1, -1, -1, 2, 2, 2, 0.15f), left);
        side(part(b, name + "_tibia", name + "_femur*", Role.STATIC, col).at(femur, 0, 0).rot(0, 0, tibiaRoll).mat(mat)
                .box(0, -0.5f, -0.5f, tibia, 1, 1, 0.1f), left);
        side(part(b, name + "_tarsus", name + "_tibia*", Role.STATIC, col2).at(tibia, 0, 0).rot(0, 0, 30).mat("horn")
                .box(0, -0.5f, -0.5f, 2, 1, 1, -0.1f), left);
    }

    /**
     * OSRS spider: a fat, hairy abdomen with a bright marking, a smaller cephalothorax with fangs and a cluster of
     * glowing eyes, and eight legs that arch high before dropping to the ground.
     */
    static ModelSpec spider(String id, int body, int mark, int eye, float scale) {
        ModelSpec.Builder b = ModelSpec.builder(id).scale(scale).shadow(0.9f);
        part(b, "thorax", null, Role.BODY, body).at(0, 18.5f, -2).mat("fur").box(-4, -2.5f, -4, 8, 5, 7).add();
        part(b, "carapace", "thorax", Role.STATIC, dk(body, 1.2f)).at(0, -2.5f, 0).mat("chitin").box(-3, -1, -3, 6, 1, 5).add();
        part(b, "abdomen", "thorax", Role.STATIC, body).accent(mark).at(0, -1.5f, 3).rot(-16, 0, 0).mat("fur")
                .box(-5.5f, -5, 0, 11, 9, 11).add();
        part(b, "abdomen_top", "abdomen", Role.STATIC, dk(body, 0.9f)).at(0, -5, 1.5f).mat("fur").box(-4, -1.5f, 0, 8, 2, 8).add();
        part(b, "marking", "abdomen_top", Role.STATIC, mark).at(0, -1.6f, 1).mat("hide")
                .box(-1, -0.5f, 0, 2, 1, 6).box(-3, -0.5f, 2, 6, 1, 2).add();
        sym(b, "abdomen_side", "abdomen", false, dk(body, 0.95f)).at(5.5f, -1, 5.5f).mat("fur").box(-1, -3, -4, 2, 6, 8).add();
        part(b, "spinnerets", "abdomen", Role.STATIC, dk(body, 0.7f)).at(0, 0, 11).mat("chitin").box(-1.5f, -1.5f, -0.5f, 3, 3, 2).add();
        part(b, "head", "thorax", Role.HEAD, dk(body, 1.1f)).at(0, -0.5f, -4).mat("fur").box(-3, -2.5f, -3, 6, 4, 3).add();
        part(b, "eyes", "head", Role.STATIC, eye).at(0, -2, -3).mat("energy").glow("full")
                .box(-2, 0, -0.6f, 1, 1, 1).box(1, 0, -0.6f, 1, 1, 1).box(-1, -1, -0.6f, 2, 1, 1, -0.1f)
                .box(-2.8f, 0.8f, -0.2f, 1, 1, 1, -0.2f).box(1.8f, 0.8f, -0.2f, 1, 1, 1, -0.2f).add();
        for (boolean left : new boolean[]{true, false}) {
            side(part(b, "chelicera", "head", Role.JAW, dk(body, 0.8f)).at(1.2f, 1.5f, -2.5f).rot(-15, 0, 0).mat("fur")
                    .box(-1, 0, -1, 2, 2, 2), left);
            side(part(b, "fang", "chelicera*", Role.STATIC, 0x1A1410).at(0, 2, -0.5f).rot(30, 0, 10).mat("horn")
                    .box(-0.5f, 0, -0.5f, 1, 2, 1), left);
            side(part(b, "palp", "head", Role.STATIC, body).at(2.5f, 1, -2.5f).rot(30, -30, 0).mat("fur")
                    .box(-0.5f, 0, -0.5f, 1, 3, 1), left);
        }
        float[] zs = {-3.5f, -1.5f, 0.5f, 2.5f};
        float[] yaws = {50, 18, -15, -45};
        for (int i = 0; i < 4; i++) {
            for (boolean left : new boolean[]{true, false}) {
                arachLeg(b, "leg" + i, left, "thorax", 3.5f, 0.5f, zs[i], yaws[i], 7, -45, 10, 106, body, dk(body, 1.25f), "fur");
            }
        }
        return b.build();
    }


    // ------------------------------------------------------------------------------------------
    // Wild beasts
    // ------------------------------------------------------------------------------------------

    /** OSRS wolf: deep-chested and lean, with a shaggy ruff, a long snout with a snarling jaw, pointed ears and a bushy tail. */
    static ModelSpec wolf(String id, int fur, int dark, int light) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.7f);
        part(b, "body", null, Role.BODY, fur).at(0, 12, 0).rot(-4, 0, 0).mat("fur").box(-3.5f, -3.5f, -7, 7, 7, 14).add();
        part(b, "chest", "body", Role.STATIC, fur).at(0, 0, -7).rot(8, 0, 0).mat("fur").box(-4, -4.5f, -3, 8, 9, 5).add();
        part(b, "ruff", "chest", Role.STATIC, light).at(0, -4.5f, -1).rot(-12, 0, 0).mat("fur").box(-4.5f, -1.5f, -2, 9, 5, 5).add();
        part(b, "ruff_low", "chest", Role.STATIC, light).at(0, 3, -2.5f).rot(15, 0, 0).mat("fur").box(-3, 0, -1, 6, 3, 2).add();
        part(b, "back", "body", Role.STATIC, dark).at(0, -3.5f, 0).mat("fur").box(-3, -1, -6, 6, 2, 12).add();
        part(b, "belly", "body", Role.STATIC, light).at(0, 3, -1).rot(-6, 0, 0).mat("fur").box(-2.5f, -1, -4, 5, 2, 8).add();
        part(b, "haunch", "body", Role.STATIC, fur).at(0, -1, 6).mat("fur").box(-3.5f, -2.5f, -1.5f, 7, 6, 4).add();
        part(b, "neck", "chest", Role.NECK, fur).at(0, -3, -2).rot(-30, 0, 0).mat("fur").box(-2.5f, -3, -4, 5, 5, 5).add();
        part(b, "head", "neck", Role.HEAD, fur).at(0, -1.5f, -3.5f).rot(30, 0, 0).mat("fur").pattern("eyes").accent(0xF0C040)
                .box(-3, -3, -4, 6, 5, 4).add();
        part(b, "brow", "head", Role.STATIC, dark).at(0, -3, -3).mat("fur").box(-2.5f, -1, -1, 5, 1, 3).add();
        part(b, "snout", "head", Role.STATIC, fur).at(0, 0, -4).mat("fur").box(-1.5f, -1.5f, -4, 3, 2, 4).add();
        part(b, "nose", "snout", Role.STATIC, 0x141414).at(0, -1.6f, -4).mat("skin").box(-1, 0, -0.6f, 2, 1, 1).add();
        part(b, "jaw", "head", Role.JAW, light).at(0, 1, -3.5f).rot(10, 0, 0).mat("fur").box(-1.5f, 0, -4, 3, 1, 4).add();
        part(b, "fangs", "snout", Role.STATIC, 0xF0ECE0).at(0, 0.5f, -3.5f).mat("bone")
                .box(-1.5f, 0, -0.5f, 1, 1, 1, -0.1f).box(0.5f, 0, -0.5f, 1, 1, 1, -0.1f).add();
        sym(b, "cheek", "head", false, light).at(3, 0, -1.5f).rot(0, -10, 0).mat("fur").box(-1, -1.5f, -1.5f, 2, 3, 3).add();
        sym(b, "ear", "head", false, fur).at(2, -3, -0.5f).rot(-10, 0, -12).mat("fur").box(-1, -3, -0.5f, 2, 3, 1).add();
        sym(b, "ear_tip", "ear", true, dark).at(0, -3, 0).mat("fur").box(-0.5f, -1, -0.5f, 1, 1, 1).add();
        part(b, "tail", "haunch", Role.TAIL, fur).at(0, -1, 2.5f).rot(-35, 0, 0).mat("fur").box(-1, -1, 0, 2, 2, 4).add();
        part(b, "tail_mid", "tail", Role.STATIC, fur).at(0, 0, 4).rot(30, 0, 0).mat("fur").box(-1.5f, -1.5f, 0, 3, 3, 4).add();
        part(b, "tail_tip", "tail_mid", Role.STATIC, light).at(0, 0, 4).rot(20, 0, 0).mat("fur").box(-1, -1, 0, 2, 2, 3).add();
        legPair(b, "leg_f", true, 2.5f, -7, fur, fur, light, "fur", 3, 5, 5, 4, -4, "paw", -1);
        legPair(b, "leg_b", false, 2.5f, 6.5f, fur, fur, light, "fur", 3, 5, 5, -28, 34, "paw", -1);
        return b.build();
    }

    /** Yak: a shaggy black-brown hulk with a skirt of long hair to the ground, a shoulder hump and wide curved horns. */
    static ModelSpec yak(String id) {
        int fur = 0x3A2E26;
        int shag = 0x2A221C;
        int horn = 0xE0D8C0;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.9f);
        part(b, "body", null, Role.BODY, fur).at(0, 12, 1).mat("fur").box(-6, -6, -9, 12, 11, 18).add();
        part(b, "hump", "body", Role.STATIC, fur).at(0, -6, -5).rot(-10, 0, 0).mat("fur").box(-5, -3, -3, 10, 4, 8).add();
        for (boolean left : new boolean[]{true, false}) {
            side(part(b, "skirt", "body", Role.STATIC, shag).at(6, 2, 0).rot(0, 0, -6).mat("hair").box(-1, 0, -8, 2, 7, 17), left);
            side(part(b, "skirt_fringe", "skirt*", Role.STATIC, dk(shag, 0.9f)).at(0, 7, 0).mat("hair")
                    .box(-1, 0, -7, 2, 2, 4).box(-1, 0, -1.5f, 2, 3, 4).box(-1, 0, 4, 2, 2, 4), left);
        }
        part(b, "skirt_front", "body", Role.STATIC, shag).at(0, 2, -9).mat("hair").box(-5, 0, -1.5f, 10, 7, 2).add();
        part(b, "skirt_back", "body", Role.STATIC, shag).at(0, 2, 9).mat("hair").box(-5, 0, -0.5f, 10, 6, 2).add();
        part(b, "tail", "body", Role.TAIL, shag).at(0, -4, 9.5f).rot(15, 0, 0).mat("hair").box(-1, 0, -0.5f, 2, 8, 2).add();
        part(b, "neck", "body", Role.NECK, fur).at(0, 0, -9).rot(15, 0, 0).mat("fur").box(-4, -4, -4, 8, 8, 5).add();
        part(b, "head", "neck", Role.HEAD, dk(fur, 1.1f)).at(0, 1, -4).rot(10, 0, 0).mat("fur").pattern("eyes_animal")
                .box(-4, -4, -6, 8, 7, 6).add();
        part(b, "forelock", "head", Role.STATIC, shag).at(0, -4, -4).rot(20, 0, 0).mat("hair").box(-3, -1, -2.5f, 6, 3, 4).add();
        part(b, "muzzle", "head", Role.STATIC, 0x5A4A40).at(0, 1.5f, -6).mat("skin").box(-3, -1.5f, -2, 6, 4, 2).add();
        part(b, "beard", "head", Role.STATIC, shag).at(0, 3, -4).mat("hair").box(-2.5f, 0, -2, 5, 4, 3).add();
        for (boolean left : new boolean[]{true, false}) {
            side(part(b, "horn", "head", Role.STATIC, horn).at(4, -3, -3).rot(0, 10, -80).mat("horn").box(-1, -3, -1, 2, 3, 2), left);
            side(part(b, "horn_mid", "horn*", Role.STATIC, dk(horn, 0.95f)).at(0, -3, 0).rot(-20, 0, 60).mat("horn")
                    .box(-1, -3, -1, 2, 3, 2, -0.2f), left);
            side(part(b, "horn_tip", "horn_mid*", Role.STATIC, dk(horn, 0.85f)).at(0, -3, 0).rot(-20, 0, 40).mat("horn")
                    .box(-0.5f, -2, -0.5f, 1, 2, 1), left);
            side(part(b, "ear", "head", Role.STATIC, fur).at(4, -1.5f, -1.5f).rot(0, -20, 30).mat("fur").box(0, -0.5f, -1, 2, 1, 2), left);
        }
        legPair(b, "leg_f", true, 4, -6, fur, shag, 0x2A2420, "fur", 4, 4, 4, 0, 0, "hoof", -1);
        legPair(b, "leg_b", false, 4, 6, fur, shag, 0x2A2420, "fur", 4, 4, 4, -6, 6, "hoof", -1);
        return b.build();
    }

    /** Grizzly bear: a towering shoulder hump, heavy round head with a pale muzzle and round ears, thick clawed limbs. */
    static ModelSpec bear(String id) {
        int fur = 0x6A4428;
        int dark = 0x4A2E1A;
        int muzzle = 0xA07A50;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.0f);
        part(b, "body", null, Role.BODY, fur).at(0, 11, 2).rot(-5, 0, 0).mat("fur").box(-6, -6, -10, 12, 11, 20).add();
        part(b, "hump", "body", Role.STATIC, fur).at(0, -6, -6).rot(-12, 0, 0).mat("fur").box(-5, -3, -4, 10, 4, 8).add();
        part(b, "hump_top", "hump", Role.STATIC, dk(fur, 1.1f)).at(0, -3, 0).mat("fur").box(-3.5f, -1, -3, 7, 2, 6).add();
        part(b, "rump", "body", Role.STATIC, fur).at(0, -4, 8).mat("fur").box(-5.5f, -2, -3, 11, 7, 5).add();
        part(b, "belly", "body", Role.STATIC, dark).at(0, 5, 0).mat("fur").box(-5, -1, -8, 10, 2, 15).add();
        part(b, "chest", "body", Role.STATIC, fur).at(0, 0, -10).rot(10, 0, 0).mat("fur").box(-5, -5, -2, 10, 10, 3).add();
        part(b, "tail", "rump", Role.TAIL, dark).at(0, 0, 2).rot(30, 0, 0).mat("fur").box(-1, -1, 0, 2, 2, 2).add();
        part(b, "neck", "body", Role.NECK, fur).at(0, -2, -11).rot(10, 0, 0).mat("fur").box(-4.5f, -4, -3, 9, 8, 4).add();
        part(b, "head", "neck", Role.HEAD, fur).at(0, 0, -3).rot(5, 0, 0).mat("fur").pattern("eyes").accent(0x201010)
                .box(-4.5f, -4, -5, 9, 8, 5).add();
        part(b, "brow", "head", Role.STATIC, dk(fur, 0.9f)).at(0, -4, -3).mat("fur").box(-4, -1, -2, 8, 2, 4).add();
        part(b, "muzzle", "head", Role.STATIC, muzzle).at(0, 1, -5).mat("fur").box(-2.5f, -2, -4, 5, 4, 4).add();
        part(b, "nose", "muzzle", Role.STATIC, 0x141010).at(0, -2, -4).mat("skin").box(-1.5f, 0, -0.6f, 3, 2, 1).add();
        part(b, "jaw", "head", Role.JAW, dk(muzzle, 0.85f)).at(0, 3, -4).rot(8, 0, 0).mat("fur").box(-2, 0, -4.5f, 4, 2, 5).add();
        part(b, "teeth", "muzzle", Role.STATIC, 0xF0E8D8).at(0, 2, -3.5f).mat("bone")
                .box(-2, 0, -0.5f, 1, 1, 1, -0.1f).box(1, 0, -0.5f, 1, 1, 1, -0.1f).add();
        sym(b, "ear", "head", false, fur).at(3.5f, -4, -1).mat("fur").box(-1, -2, -1, 2, 2, 2).add();
        sym(b, "ear_in", "ear", true, dark).at(0, -1.5f, -1.1f).mat("fur").box(-0.5f, 0, -0.1f, 1, 1, 1).add();
        sym(b, "cheek_ruff", "head", false, dk(fur, 1.05f)).at(4.5f, 0, -1).rot(0, 0, -8).mat("fur").box(-1, -2.5f, -2, 2, 5, 4).add();
        legPair(b, "leg_f", true, 4, -7, fur, dark, dark, "fur", 5, 6, 5, 0, 0, "bear", -1);
        legPair(b, "leg_b", false, 4, 8, fur, dark, dark, "fur", 5, 5, 5, -10, 10, "bear", -1);
        return b.build();
    }

    /** Giant bat: a furry body hanging between huge finger-boned membrane wings, big ears, a pug snout with fangs. */
    static ModelSpec bat(String id) {
        int fur = 0x3A2E2A;
        int mem = 0x5A3A3A;
        int bone = 0x2A201C;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.5f);
        part(b, "body", null, Role.FLOAT, fur).at(0, 8, 0).rot(10, 0, 0).mat("fur").box(-3, -4, -2.5f, 6, 8, 5).add();
        part(b, "chest", "body", Role.STATIC, dk(fur, 1.2f)).at(0, -2, -2.5f).mat("fur").box(-2, 0, -0.8f, 4, 4, 1).add();
        part(b, "head", "body", Role.HEAD, fur).at(0, -4, -0.5f).mat("fur").pattern("eyes").accent(0xFF3020).glow("eyes")
                .box(-3, -4, -3, 6, 4, 5).add();
        part(b, "snout", "head", Role.STATIC, 0x6A4A44).at(0, -1.5f, -3).mat("skin").box(-1.5f, -1, -1.5f, 3, 2, 2).add();
        part(b, "jaw", "head", Role.JAW, dk(fur, 0.8f)).at(0, 0, -2).mat("fur").box(-1.5f, 0, -1.5f, 3, 1, 2).add();
        sym(b, "fang", "snout", false, 0xF0E8E0).at(1, 1, -1.2f).mat("bone").box(-0.5f, 0, -0.5f, 1, 2, 1, -0.15f).add();
        sym(b, "ear", "head", false, fur).at(2, -4, 0).rot(-10, 0, -20).mat("fur").box(-1, -4, -0.5f, 2, 4, 1).add();
        sym(b, "ear_in", "ear", true, 0x8A5A54).at(0, -3.5f, -0.6f).mat("skin").box(-0.5f, 0, -0.1f, 1, 3, 1, -0.1f).add();
        for (boolean left : new boolean[]{true, false}) {
            Role r = left ? Role.WING_LEFT : Role.WING_RIGHT;
            side(part(b, "arm", "body", r, bone).at(3, -3, 0).rot(0, 0, -10).mat("hide").box(0, -1, -1, 6, 2, 2), left);
            side(part(b, "wing", "arm*", Role.STATIC, mem).at(0, 0, 0).mat("membrane").box(0, 0, -0.5f, 6, 9, 1), left);
            side(part(b, "hand", "arm*", Role.STATIC, bone).at(6, 0, 0).rot(0, 0, -15).mat("hide").box(0, -0.5f, -0.5f, 7, 1, 1), left);
            side(part(b, "wing_outer", "hand*", Role.STATIC, dk(mem, 0.92f)).at(0, 0, 0).mat("membrane")
                    .box(0, 0, -0.4f, 7, 7, 1).box(0, 7, -0.4f, 4, 3, 1), left);
            side(part(b, "finger_a", "hand*", Role.STATIC, bone).at(7, 0, 0).rot(0, 0, 50).mat("horn").box(0, -0.5f, -0.5f, 6, 1, 1), left);
            side(part(b, "finger_b", "hand*", Role.STATIC, bone).at(4, 0, 0).rot(0, 0, 80).mat("horn").box(0, -0.5f, -0.5f, 8, 1, 1), left);
            side(part(b, "thumb", "hand*", Role.STATIC, 0x1A1410).at(0, 0, 0).rot(0, 0, -40).mat("horn").box(0, -0.5f, -0.5f, 2, 1, 1), left);
            side(part(b, "foot", "body", left ? Role.LEG_BACK_LEFT : Role.LEG_BACK_RIGHT, fur).at(1.5f, 4, 1).mat("fur")
                    .box(-0.5f, 0, -0.5f, 1, 3, 1), left);
            side(part(b, "talon", "foot*", Role.STATIC, 0x1A1410).at(0, 3, 0).rot(30, 0, 0).mat("horn").box(-1, 0, -1, 2, 1, 2, -0.2f), left);
        }
        return b.build();
    }

    /**
     * Scorpion: plated, segmented body low to the ground, huge two-part pincers carried forward, eight bent legs and a
     * six-segment tail curling high over the back to a barbed stinger.
     */
    static ModelSpec scorpion(String id) {
        int shell = 0x2A2622;
        int plate = 0x3E3630;
        int belly = 0x5A4A3A;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.9f);
        part(b, "body", null, Role.BODY, shell).accent(belly).at(0, 19, 0).mat("chitin").box(-4.5f, -2.5f, -6, 9, 4, 12).add();
        for (int i = 0; i < 4; i++) {
            part(b, "tergite" + i, "body", Role.STATIC, i % 2 == 0 ? plate : shell).at(0, -2.5f, -5 + i * 3)
                    .rot(-6, 0, 0).mat("chitin").box(-4, -1, 0, 8, 1, 3, 0.1f).add();
        }
        part(b, "head", "body", Role.HEAD, plate).at(0, -1, -6).mat("chitin").box(-3.5f, -1.5f, -3, 7, 3, 3).add();
        part(b, "eyes", "head", Role.STATIC, 0xFF3010).at(0, -1.5f, -2).mat("energy").glow("full")
                .box(-1.5f, -0.6f, -0.5f, 1, 1, 1).box(0.5f, -0.6f, -0.5f, 1, 1, 1).add();
        part(b, "mouth", "head", Role.JAW, dk(shell, 0.7f)).at(0, 1, -3).mat("chitin").box(-1.5f, 0, -1, 3, 1, 1).add();
        // pincers
        for (boolean left : new boolean[]{true, false}) {
            Role r = left ? Role.ARM_LEFT : Role.ARM_RIGHT;
            side(part(b, "arm", "body", r, shell).at(3.5f, -0.5f, -6).rot(0, -30, 0).mat("chitin").box(-1, -1, -5, 2, 2, 5), left);
            side(part(b, "forearm", "arm*", Role.STATIC, plate).at(0, 0, -5).rot(0, 55, 0).mat("chitin").box(-1.5f, -1.5f, -4, 3, 3, 4), left);
            side(part(b, "claw", "forearm*", Role.STATIC, shell).at(0, 0, -4).rot(0, -20, 0).mat("chitin")
                    .box(-2, -2, -4, 4, 4, 4), left);
            side(part(b, "claw_fixed", "claw*", Role.STATIC, plate).at(-1, -0.5f, -4).rot(0, 10, 0).mat("chitin")
                    .box(-1, -1, -4, 2, 2, 4).box(-0.5f, -0.5f, -5, 1, 1, 1), left);
            side(part(b, "claw_moving", "claw*", Role.JAW, dk(plate, 1.1f)).at(1, 0.5f, -4).rot(0, -18, 0).mat("chitin")
                    .box(-0.75f, -1, -4, 2, 2, 4, -0.2f), left);
        }
        // legs
        for (int i = 0; i < 4; i++) {
            for (boolean left : new boolean[]{true, false}) {
                arachLeg(b, "leg" + i, left, "body", 4, 0.5f, -3.5f + i * 2.5f, 25 - i * 17, 4, -40, 6, 105, shell, plate, "chitin");
            }
        }
        // tail arcs up and forward
        String prev = "body";
        float z = 6;
        float[] pitch = {42, 26, 22, 20, 18, 16};
        for (int i = 0; i < 6; i++) {
            String n = "tail" + i;
            int w = i < 3 ? 4 : 3;
            part(b, n, prev, i == 0 ? Role.TAIL : Role.STATIC, i % 2 == 0 ? shell : plate).at(0, i == 0 ? -1 : 0, i == 0 ? z - 0.5f : 3.2f)
                    .rot(pitch[i], 0, 0).mat("chitin").box(-w / 2f, -w / 2f, 0, w, w, 4).add();
            prev = n;
        }
        part(b, "bulb", prev, Role.STATIC, 0x8A2A1A).at(0, 0, 3.5f).rot(20, 0, 0).mat("chitin").box(-1.5f, -1.5f, 0, 3, 3, 3).add();
        part(b, "stinger", "bulb", Role.STATIC, 0x1A1410).at(0, 0.5f, 3).rot(50, 0, 0).mat("horn").box(-0.5f, -0.5f, 0, 1, 1, 3).add();
        return b.build();
    }

    /** OSRS monkey: a small, long-armed brown monkey with a pale face, big round ears and a curling tail. */
    static ModelSpec monkey(String id) {
        int fur = 0x6A4A2A;
        int face = 0xC8A078;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.4f);
        part(b, "hips", null, Role.BODY, fur).at(0, 16, 1).mat("fur").box(-2.5f, -2, -2, 5, 3, 4).add();
        part(b, "torso", "hips", Role.STATIC, fur).at(0, -2, 0).rot(20, 0, 0).mat("fur").box(-3, -6, -2.5f, 6, 6, 5).add();
        part(b, "belly", "torso", Role.STATIC, dk(face, 0.85f)).at(0, -4, -2.5f).mat("fur").box(-2, 0, -0.6f, 4, 4, 1).add();
        part(b, "head", "torso", Role.HEAD, fur).at(0, -6, -0.5f).rot(-20, 0, 0).mat("fur").box(-3, -5, -3, 6, 5, 5).add();
        part(b, "face", "head", Role.STATIC, face).at(0, -2.5f, -3).mat("face").pattern("eyes_animal").box(-2.5f, -2, -0.6f, 5, 4, 1).add();
        part(b, "muzzle", "face", Role.STATIC, dk(face, 0.95f)).at(0, 1, -0.6f).mat("skin").box(-1.5f, -0.5f, -1.2f, 3, 2, 2).add();
        part(b, "jaw", "muzzle", Role.JAW, dk(face, 0.8f)).at(0, 1.5f, -0.5f).mat("skin").box(-1, 0, -0.6f, 2, 1, 1).add();
        part(b, "brow", "head", Role.STATIC, dk(fur, 0.8f)).at(0, -4.5f, -3).mat("fur").box(-3, -0.5f, -0.6f, 6, 1, 1).add();
        sym(b, "ear", "head", false, face).at(3, -3, -0.5f).mat("skin").box(-0.3f, -1.5f, -1, 1, 3, 2).add();
        for (boolean left : new boolean[]{true, false}) {
            side(part(b, "arm", "torso", left ? Role.ARM_LEFT : Role.ARM_RIGHT, fur).at(3.5f, -5, 0).rot(-30, 0, -8).mat("fur")
                    .box(-1, -1, -1, 2, 6, 2), left);
            side(part(b, "forearm", "arm*", Role.STATIC, fur).at(0, 5, 0).rot(-20, 0, 0).mat("fur").box(-1, 0, -1, 2, 5, 2, -0.1f), left);
            side(part(b, "hand", "forearm*", Role.STATIC, face).at(0, 5, 0).mat("skin").box(-1, 0, -1.5f, 2, 2, 3), left);
            side(part(b, "leg", "hips", left ? Role.LEG_FRONT_LEFT : Role.LEG_FRONT_RIGHT, fur).at(1.8f, 0, 0).rot(-40, 0, -10).mat("fur")
                    .box(-1, 0, -1, 2, 4, 2), left);
            side(part(b, "shin", "leg*", Role.STATIC, fur).at(0, 4, 0).rot(60, 0, 0).mat("fur").box(-1, 0, -1, 2, 4, 2, -0.1f), left);
            side(part(b, "foot", "shin*", Role.STATIC, face).at(0, 4, 0).rot(-20, 0, 0).mat("skin").box(-1, -0.5f, -2.5f, 2, 1, 3), left);
        }
        String prev = "hips";
        float[] tp = {40, 30, 40, 50};
        for (int i = 0; i < 4; i++) {
            String n = "tail" + i;
            part(b, n, prev, i == 0 ? Role.TAIL : Role.STATIC, fur).at(0, i == 0 ? -1 : 0, i == 0 ? 2 : 3.5f).rot(tp[i], 0, 0).mat("fur")
                    .box(-0.5f, -0.5f, 0, 1, 1, 4).add();
            prev = n;
        }
        return b.build();
    }
}
