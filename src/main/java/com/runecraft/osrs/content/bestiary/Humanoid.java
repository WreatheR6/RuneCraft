package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/**
 * Parametric OSRS-style humanoid: proportions ({@link Humanoid#body}) plus an outfit (hair, beard, top,
 * sleeves, bottoms, boots, belt, cape, helmet, shield, weapon) produce a 30-50 box model with separate
 * materials per garment. Used for townsfolk, guards, warriors, wizards, goblins, giants, dwarves, gnomes,
 * knights, the Barrows brothers and many quest bosses.
 *
 * <p>Fields are public and chainable through the setters so a look reads like a description:
 * {@code Humanoid.of("guard").skin(SKIN).hair(BROWN, "short").top(STEEL, "metal").helm("med", STEEL)...}.
 */
public final class Humanoid {
    // ---- common OSRS colours ----
    public static final int SKIN = 0xC8946A;
    public static final int SKIN_PALE = 0xE0B490;
    public static final int SKIN_DARK = 0x8A5A3A;
    public static final int SKIN_DESERT = 0xA87048;
    public static final int GOBLIN_GREEN = 0x6E9A3A;
    public static final int HAIR_BROWN = 0x5A3A1E;
    public static final int HAIR_BLACK = 0x221A16;
    public static final int HAIR_BLOND = 0xC8A050;
    public static final int HAIR_GREY = 0x9A9890;
    public static final int HAIR_WHITE = 0xE0DED6;
    public static final int HAIR_GINGER = 0xA0481E;
    public static final int LEATHER = 0x6A4424;
    public static final int DARK_LEATHER = 0x3E2814;
    public static final int WOOD = 0x7A5230;
    // OSRS metal tiers
    public static final int BRONZE = 0x9A6A34;
    public static final int IRON = 0x6E6A68;
    public static final int STEEL = 0x9A9A9E;
    public static final int BLACK = 0x2C2C30;
    public static final int MITHRIL = 0x4E5A88;
    public static final int ADAMANT = 0x4A6A48;
    public static final int RUNE = 0x3E8A9E;
    public static final int DRAGON = 0x9A1A16;
    public static final int GOLD = 0xD8A830;

    final String id;
    // body proportions (model pixels)
    int legLen = 12;
    int legW = 4;
    int torsoW = 8;
    int torsoH = 10;
    int torsoD = 5;
    int armLen = 11;
    int armW = 3;
    int headW = 8;
    int headH = 8;
    int headD = 8;
    int belly;
    float hunch;
    float stance;
    float modelScale = 1f;
    float shadow = 0.5f;
    // colours and garments
    int skin = SKIN;
    String face = "face";
    String skinMaterial = "skin";
    int hair = HAIR_BROWN;
    String hairStyle = "short";
    int beardColor = -1;
    String beard = "none";
    int top = 0x7A6A50;
    String topMaterial = "cloth";
    String sleeves = "long";
    int sleeveColor = -1;
    int bottoms = 0x4A4A5A;
    String bottomsMaterial = "cloth";
    String bottomsStyle = "trousers";
    int boots = DARK_LEATHER;
    int gloves = -1;
    int belt = -1;
    int cape = -1;
    String helm = "none";
    int helmColor;
    String shield = "none";
    int shieldColor;
    int shieldTrim = -1;
    String weapon = "none";
    int weaponColor;
    String ears = "round";
    boolean bigNose;
    boolean tusks;
    int horns = -1;
    int trim = -1;
    int pauldrons = -1;
    String eyesGlow;
    float armPitch;
    float weaponPitch;

    private Humanoid(String id) {
        this.id = id;
    }

    public static Humanoid of(String id) {
        return new Humanoid(id);
    }

    // ---- proportion presets ----
    public Humanoid body(int legLen, int torsoW, int torsoH, int torsoD, int armLen, int armW) {
        this.legLen = legLen;
        this.torsoW = torsoW;
        this.torsoH = torsoH;
        this.torsoD = torsoD;
        this.armLen = armLen;
        this.armW = armW;
        return this;
    }

    public Humanoid head(int w, int h, int d) {
        headW = w;
        headH = h;
        headD = d;
        return this;
    }

    public Humanoid legW(int w) {
        legW = w;
        return this;
    }

    public Humanoid belly(int belly) {
        this.belly = belly;
        return this;
    }

    public Humanoid hunch(float degrees) {
        hunch = degrees;
        return this;
    }

    public Humanoid stance(float degrees) {
        stance = degrees;
        return this;
    }

    public Humanoid scale(float s) {
        modelScale = s;
        return this;
    }

    public Humanoid shadow(float s) {
        shadow = s;
        return this;
    }

    /** Short, big-headed goblin proportions. */
    public Humanoid goblinBody() {
        return body(7, 7, 7, 4, 8, 2).head(8, 7, 7).legW(3).ears("pointy").bigNose(true).hunch(8);
    }

    /** Broad, heavy giant proportions (entity scale makes it big). */
    public Humanoid giantBody() {
        return body(11, 11, 11, 6, 13, 5).head(8, 8, 8).legW(5).belly(2).hunch(12).stance(5).weaponPitch(55);
    }

    /** Short, stocky dwarf proportions. */
    public Humanoid dwarfBody() {
        return body(6, 9, 9, 6, 9, 3).head(8, 8, 8).legW(4);
    }

    public Humanoid gnomeBody() {
        return body(6, 6, 7, 4, 7, 2).head(7, 7, 7).legW(3).ears("pointy").bigNose(true);
    }

    public Humanoid ogreBody() {
        return body(11, 11, 12, 7, 13, 4).head(7, 7, 7).legW(5).belly(3).hunch(14).stance(5).tusks(true);
    }

    public Humanoid trollBody() {
        return body(9, 11, 12, 7, 15, 4).head(8, 7, 8).legW(5).hunch(22).stance(6).bigNose(true);
    }

    public Humanoid femaleBody() {
        return body(12, 7, 10, 4, 11, 3).head(8, 8, 8).legW(3);
    }

    // ---- looks ----
    public Humanoid skin(int c) {
        skin = c;
        return this;
    }

    /** "face" (human), "eyes" (fierce creature eyes), "skull" (eyes as dark sockets) or "none". */
    public Humanoid face(String f) {
        face = f;
        return this;
    }

    public Humanoid skinMaterial(String m) {
        skinMaterial = m;
        return this;
    }

    /** Styles: none/bald, short, long, ponytail, bun, spiky, mohawk. */
    public Humanoid hair(int c, String style) {
        hair = c;
        hairStyle = style;
        return this;
    }

    /** Styles: none, short, long, goatee, moustache. */
    public Humanoid beard(int c, String style) {
        beardColor = c;
        beard = style;
        return this;
    }

    /** Material: cloth, leather, metal, chain, fur, robe (a robe top also adds a long skirt). */
    public Humanoid top(int c, String material) {
        top = c;
        topMaterial = material;
        return this;
    }

    /** "long", "short", "none" (bare arms), or "skin". */
    public Humanoid sleeves(String s) {
        sleeves = s;
        return this;
    }

    public Humanoid sleeves(String s, int c) {
        sleeves = s;
        sleeveColor = c;
        return this;
    }

    /** Styles: trousers, shorts, skirt, robe (floor-length), kilt, loincloth, plate (platelegs), plateskirt. */
    public Humanoid bottoms(int c, String style) {
        bottoms = c;
        bottomsStyle = style;
        bottomsMaterial = style.startsWith("plate") ? "metal" : style.equals("loincloth") ? "leather" : "cloth";
        return this;
    }

    public Humanoid bottomsMaterial(String m) {
        bottomsMaterial = m;
        return this;
    }

    public Humanoid boots(int c) {
        boots = c;
        return this;
    }

    public Humanoid gloves(int c) {
        gloves = c;
        return this;
    }

    public Humanoid belt(int c) {
        belt = c;
        return this;
    }

    public Humanoid cape(int c) {
        cape = c;
        return this;
    }

    /** Coloured trim on robes / platebodies (e.g. gold on a white knight). */
    public Humanoid trim(int c) {
        trim = c;
        return this;
    }

    public Humanoid pauldrons(int c) {
        pauldrons = c;
        return this;
    }

    /** Helmets: none, med, full, horned, hood, wizard, cowl, crown, bandana, chef, coif, mask, tiara, sallet. */
    public Humanoid helm(String type, int c) {
        helm = type;
        helmColor = c;
        return this;
    }

    /** Shields: none, kite, sq, round, buckler, book. */
    public Humanoid shield(String type, int c) {
        shield = type;
        shieldColor = c;
        return this;
    }

    public Humanoid shield(String type, int c, int trim) {
        shield = type;
        shieldColor = c;
        shieldTrim = trim;
        return this;
    }

    /**
     * Weapons: none, sword, longsword, scimitar, dagger, mace, axe, battleaxe, greataxe, warhammer, hammers, club,
     * spear, warspear, halberd, staff, battlestaff, wand, shortbow, longbow, crossbow, flail, scythe, pickaxe,
     * pitchfork, whip.
     */
    public Humanoid weapon(String type, int c) {
        weapon = type;
        weaponColor = c;
        return this;
    }

    /** Idle weapon angle: positive points the weapon down towards the ground (clubs resting, swords lowered). */
    public Humanoid weaponPitch(float p) {
        weaponPitch = p;
        return this;
    }

    public Humanoid ears(String e) {
        ears = e;
        return this;
    }

    public Humanoid bigNose(boolean b) {
        bigNose = b;
        return this;
    }

    public Humanoid tusks(boolean b) {
        tusks = b;
        return this;
    }

    public Humanoid horns(int c) {
        horns = c;
        return this;
    }

    /** Idle arm pose: negative pitch raises the arms forward (zombies reach out at -80). */
    public Humanoid arms(float pitch) {
        armPitch = pitch;
        return this;
    }

    /** Make the eyes emissive (undead, demons, spirits). */
    public Humanoid glowingEyes() {
        eyesGlow = "eyes";
        return this;
    }

    // ---- build ----
    public ModelSpec build() {
        return builder().build();
    }

    /** The populated builder (for adding extra parts before building). */
    public ModelSpec.Builder builder() {
        ModelSpec.Builder b = ModelSpec.builder(id).scale(modelScale).shadow(shadow);
        boolean robe = "robe".equals(bottomsStyle);
        int sleeveCol = sleeveColor >= 0 ? sleeveColor : top;
        String sleeveMat = topMaterial.equals("robe") ? "cloth" : topMaterial;
        String topMat = topMaterial.equals("robe") ? "cloth" : topMaterial;
        float hipY = 24 - legLen;
        int hw = torsoW;
        // ---- hips & torso ----
        part(b, "hips", null, Role.BODY, bottoms).at(0, hipY, 0).mat(bottomsMaterial)
                .box(-hw / 2f, -2, -torsoD / 2f, hw, 3, torsoD).add();
        part(b, "torso", "hips", Role.STATIC, top).at(0, -2, 0).rot(hunch, 0, 0).mat(topMat)
                .box(-hw / 2f, -torsoH, -torsoD / 2f, hw, torsoH, torsoD).add();
        if (belly > 0) {
            part(b, "belly", "torso", Role.STATIC, sleeves.equals("none") && topMaterial.equals("skin") ? skin : top).at(0, -torsoH / 2f + 1, -torsoD / 2f)
                    .mat(topMaterial.equals("skin") ? skinMaterial : topMat)
                    .box(-hw / 2f + 1, -torsoH / 2f + 1, -belly, hw - 2, torsoH / 2 + 1, belly).add();
        }
        if ("metal".equals(topMaterial) || "chain".equals(topMaterial)) {
            part(b, "chestplate", "torso", Role.STATIC, top).at(0, -torsoH + 1, -torsoD / 2f).mat("metal")
                    .box(-hw / 2f + 0.5f, 0, -1, hw - 1, Math.max(3, torsoH / 2), 1).add();
        }
        if (trim >= 0) {
            part(b, "trim", "torso", Role.STATIC, trim).at(0, -torsoH, -torsoD / 2f).mat("metal")
                    .box(-1, 0, -0.6f, 2, torsoH, 1).add();
        }
        int beltCol = belt >= 0 ? belt : (robe ? -1 : LEATHER);
        if (beltCol >= 0) {
            part(b, "belt", "hips", Role.STATIC, beltCol).at(0, -2, 0).mat("leather")
                    .box(-hw / 2f, -1, -torsoD / 2f, hw, 1, torsoD, 0.25f).add();
            part(b, "belt_buckle", "belt", Role.STATIC, GOLD).at(0, -1, -torsoD / 2f).mat("metal").box(-1, 0, -0.75f, 2, 1, 1).add();
        }
        part(b, "neck", "torso", Role.STATIC, skin).at(0, -torsoH, 0).mat(skinMaterial).box(-1.5f, -1, -1.5f, 3, 1, 3).add();
        // ---- legs ----
        int thigh = (legLen + 1) / 2;
        int bootH = Math.max(1, Math.min(3, legLen / 4));
        int shin = legLen - thigh - bootH;
        boolean bareLegs = "loincloth".equals(bottomsStyle) || "shorts".equals(bottomsStyle) || "kilt".equals(bottomsStyle);
        int shinCol = bareLegs ? skin : bottoms;
        String shinMat = bareLegs ? skinMaterial : bottomsMaterial;
        float lx = hw / 4f;
        sym(b, "leg", null, false, bareLegs && "loincloth".equals(bottomsStyle) ? skin : bottoms)
                .roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).at(lx, hipY, 0).rot(0, 0, -stance)
                .mat(bareLegs && "loincloth".equals(bottomsStyle) ? skinMaterial : bottomsMaterial)
                .box(-legW / 2f, 0, -legW / 2f, legW, thigh, legW).add();
        sym(b, "shin", "leg", true, shinCol).at(0, thigh, 0).rot(0, 0, stance * 0.6f).mat(shinMat)
                .box(-legW / 2f + 0.01f, 0, -legW / 2f, legW, shin, legW, -0.1f).add();
        if (bottomsStyle.startsWith("plate")) {
            sym(b, "knee", "shin", true, bottoms).at(0, 0, -legW / 2f).mat("metal").box(-legW / 2f + 0.5f, -1, -0.75f, legW - 1, 2, 1).add();
        }
        sym(b, "boot", "shin", true, boots).at(0, shin, 0).mat(boots == skin ? skinMaterial : "leather")
                .box(-legW / 2f, 0, -legW / 2f - 1, legW, bootH, legW + 1, 0.05f).add();
        // ---- lower garments ----
        switch (bottomsStyle) {
            case "skirt", "plateskirt", "kilt" -> part(b, "skirt", "hips", Role.STATIC, bottoms).at(0, 0, 0)
                    .mat(bottomsStyle.equals("plateskirt") ? "metal" : "cloth")
                    .box(-hw / 2f - 0.5f, -1, -torsoD / 2f - 0.5f, hw + 1, Math.max(3, thigh + (bottomsStyle.equals("kilt") ? 0 : 3)), torsoD + 1).add();
            case "robe" -> {
                part(b, "skirt", "hips", Role.STATIC, bottoms).at(0, 0, 0).mat("cloth")
                        .box(-hw / 2f - 0.5f, -1, -torsoD / 2f - 0.5f, hw + 1, legLen - 1, torsoD + 1).add();
                part(b, "hem", "skirt", Role.STATIC, trim >= 0 ? trim : bottoms).at(0, legLen - 2, 0).mat("cloth")
                        .box(-hw / 2f - 1, -0.5f, -torsoD / 2f - 1, hw + 2, 1, torsoD + 2).add();
            }
            case "loincloth" -> part(b, "loincloth", "hips", Role.STATIC, bottoms).at(0, 1, -torsoD / 2f).mat("leather")
                    .box(-2, -1, -0.6f, 4, Math.max(3, thigh), 1).box(-2, -1, torsoD - 0.4f, 4, Math.max(3, thigh), 1).add();
            default -> {
            }
        }
        // ---- arms ----
        float shoulderY = -torsoH + 1;
        float ax = hw / 2f + armW / 2f;
        int upper = (armLen + 1) / 2;
        int handH = Math.max(1, Math.min(2, armLen / 5));
        int fore = armLen - upper - handH;
        boolean bareArms = sleeves.equals("none") || sleeves.equals("skin");
        boolean shortSleeves = sleeves.equals("short");
        int upperCol = bareArms ? skin : sleeveCol;
        int foreCol = bareArms || shortSleeves ? skin : sleeveCol;
        sym(b, "arm", "torso", false, upperCol).roles(Role.ARM_LEFT, Role.ARM_RIGHT).at(ax, shoulderY, 0)
                .rot(armPitch, 0, -4 - stance).mat(bareArms ? skinMaterial : sleeveMat)
                .box(-armW / 2f, -1, -armW / 2f, armW, upper + 1, armW).add();
        sym(b, "forearm", "arm", true, foreCol).at(0, upper, 0).rot(-8, 0, 0)
                .mat(bareArms || shortSleeves ? skinMaterial : sleeveMat)
                .box(-armW / 2f, 0, -armW / 2f, armW, fore, armW, -0.05f).add();
        if (topMaterial.equals("robe") && !bareArms) {
            sym(b, "cuff", "forearm", true, trim >= 0 ? trim : sleeveCol).at(0, fore - 1, 0).mat("cloth")
                    .box(-armW / 2f - 0.5f, 0, -armW / 2f - 0.5f, armW + 1, 1, armW + 1).add();
        }
        int handCol = gloves >= 0 ? gloves : skin;
        sym(b, "hand", "forearm", true, handCol).at(0, fore, 0).mat(gloves >= 0 ? "leather" : skinMaterial)
                .box(-armW / 2f, 0, -armW / 2f, armW, handH, armW).add();
        int pd = pauldrons >= 0 ? pauldrons : ("metal".equals(topMaterial) ? top : -1);
        if (pd >= 0) {
            sym(b, "pauldron", "arm", true, pd).at(0, -1, 0).rot(0, 0, -10).mat("metal")
                    .box(-armW / 2f - 0.5f, -1, -armW / 2f - 0.5f, armW + 2, 3, armW + 1).add();
        }
        // ---- cape ----
        if (cape >= 0) {
            part(b, "cape", "torso", Role.STATIC, cape).at(0, -torsoH, torsoD / 2f).rot(6, 0, 0).mat("cloth")
                    .box(-hw / 2f, 0, 0, hw, torsoH + legLen - 3, 1).add();
        }
        // ---- head ----
        part(b, "head", "torso", Role.HEAD, skin).at(0, -torsoH - 1, 0).rot(-hunch * 0.7f, 0, 0)
                .mat(face.equals("face") ? "face" : skinMaterial).pattern(face.equals("eyes") ? "eyes" : face.equals("skull") ? "skull" : "none")
                .glow(eyesGlow).box(-headW / 2f, -headH, -headD / 2f, headW, headH, headD).add();
        if (bigNose) {
            part(b, "nose", "head", Role.STATIC, skin).at(0, -headH * 0.42f, -headD / 2f).rot(-10, 0, 0).mat(skinMaterial)
                    .box(-1, 0, -2, 2, 2, 2).add();
        } else if (face.equals("face")) {
            part(b, "nose", "head", Role.STATIC, skin).at(0, -headH * 0.42f, -headD / 2f).mat(skinMaterial)
                    .box(-0.5f, 0, -1, 1, 2, 1).add();
        }
        if (ears.equals("pointy")) {
            sym(b, "ear", "head", false, skin).at(headW / 2f, -headH * 0.55f, 0).rot(0, -25, -35).mat(skinMaterial)
                    .box(0, -1, -0.5f, 3, 2, 1).add();
        } else if (ears.equals("round") && helm.equals("none") && (hairStyle.equals("none") || hairStyle.equals("bald") || hairStyle.equals("short"))) {
            sym(b, "ear", "head", false, skin).at(headW / 2f, -headH * 0.5f, 0.5f).mat(skinMaterial)
                    .box(0, -1, -0.5f, 1, 2, 1).add();
        }
        if (tusks) {
            sym(b, "tusk", "head", false, 0xE8E0C0).at(headW / 4f + 0.5f, -1, -headD / 2f).rot(-15, 0, 10).mat("horn")
                    .box(-0.5f, -2, -1, 1, 2, 1).add();
        }
        if (horns >= 0) {
            sym(b, "horn", "head", false, horns).at(headW / 2f - 1, -headH + 1, -1).rot(-15, 0, -40).mat("horn")
                    .box(-1, -4, -1, 2, 4, 2).add();
            sym(b, "horn_tip", "horn", true, horns).at(0, -4, 0).rot(-25, 0, 30).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        }
        hair(b);
        beard(b);
        helm(b);
        shield(b);
        weapon(b);
        return b;
    }

    private void hair(ModelSpec.Builder b) {
        if (!helm.equals("none") && !helm.equals("crown") && !helm.equals("tiara") && !helm.equals("bandana")) {
            if (hairStyle.equals("long")) {
                part(b, "hair_back", "head", Role.STATIC, hair).at(0, -headH + 2, headD / 2f).mat("hair")
                        .box(-headW / 2f, 0, -0.5f, headW, headH + 2, 1).add();
            }
            return;
        }
        float t = -headH;
        switch (hairStyle) {
            case "short", "spiky" -> {
                part(b, "hair", "head", Role.STATIC, hair).at(0, t, 0).mat("hair")
                        .box(-headW / 2f - 0.25f, -1, -headD / 2f - 0.25f, headW, 2, headD, 0.25f)
                        .box(-headW / 2f - 0.25f, 1, headD / 2f - 2, headW, headH / 2, 2, 0.25f).add();
                if (hairStyle.equals("spiky")) {
                    for (int i = -1; i <= 1; i++) {
                        part(b, "spike" + (i + 1), "hair", Role.STATIC, hair).at(i * 2.5f, -1, 0).rot(-20, 0, i * 20).mat("hair")
                                .box(-0.5f, -2, -0.5f, 1, 2, 1).add();
                    }
                }
            }
            case "long", "ponytail", "bun" -> {
                part(b, "hair", "head", Role.STATIC, hair).at(0, t, 0).mat("hair")
                        .box(-headW / 2f - 0.25f, -1, -headD / 2f - 0.25f, headW, 2, headD, 0.25f).add();
                part(b, "hair_back", "head", Role.STATIC, hair).at(0, t + 1, headD / 2f - 1).mat("hair")
                        .box(-headW / 2f - 0.5f, 0, -0.5f, headW + 1, hairStyle.equals("long") ? headH + 3 : headH - 2, 2).add();
                if (hairStyle.equals("long")) {
                    sym(b, "hair_side", "head", false, hair).at(headW / 2f, t + 1, -1).mat("hair")
                            .box(0, 0, -headD / 2f + 2, 1, headH - 2, headD - 2).add();
                } else if (hairStyle.equals("ponytail")) {
                    part(b, "ponytail", "hair_back", Role.STATIC, hair).at(0, 2, 1).rot(25, 0, 0).mat("hair")
                            .box(-1, 0, 0, 2, 6, 2).add();
                } else {
                    part(b, "bun", "hair_back", Role.STATIC, hair).at(0, 0, 1).mat("hair").box(-1.5f, -2, 0, 3, 3, 3).add();
                }
            }
            case "mohawk" -> part(b, "hair", "head", Role.STATIC, hair).at(0, t, 0).mat("hair")
                    .box(-1, -3, -headD / 2f, 2, 3, headD).add();
            default -> {
            }
        }
    }

    private void beard(ModelSpec.Builder b) {
        if (beard.equals("none")) {
            return;
        }
        int c = beardColor >= 0 ? beardColor : hair;
        float fy = -headH * 0.32f;
        float fz = -headD / 2f;
        switch (beard) {
            case "long" -> part(b, "beard", "head", Role.STATIC, c).at(0, fy, fz).rot(8, 0, 0).mat("hair")
                    .box(-headW / 2f + 0.5f, 0, -1, headW - 1, headH / 2 + 5, 2).add();
            case "goatee" -> part(b, "beard", "head", Role.STATIC, c).at(0, -2, fz).mat("hair").box(-1.5f, 0, -1, 3, 3, 1).add();
            case "moustache" -> part(b, "beard", "head", Role.STATIC, c).at(0, fy + 0.5f, fz).mat("hair").box(-2.5f, 0, -0.75f, 5, 1, 1).add();
            default -> part(b, "beard", "head", Role.STATIC, c).at(0, fy, fz).mat("hair")
                    .box(-headW / 2f + 0.5f, 0, -1, headW - 1, headH / 3 + 1, 2).add();
        }
    }

    private void helm(ModelSpec.Builder b) {
        int c = helmColor;
        float t = -headH;
        float hw = headW / 2f;
        float hd = headD / 2f;
        switch (helm) {
            case "med" -> {
                part(b, "helm", "head", Role.STATIC, c).at(0, t, 0).mat("metal").box(-hw, -1, -hd, headW, 4, headD, 0.35f).add();
                part(b, "helm_nose", "helm", Role.STATIC, c).at(0, 2, -hd).mat("metal").box(-0.5f, 0, -0.9f, 1, 3, 1).add();
                part(b, "helm_ridge", "helm", Role.STATIC, c).at(0, -1.5f, 0).mat("metal").box(-0.5f, -0.5f, -hd, 1, 1, headD).add();
            }
            case "full", "sallet" -> {
                part(b, "helm", "head", Role.STATIC, c).at(0, t, 0).mat("metal").pattern("visor")
                        .box(-hw, -1, -hd, headW, headH + 1, headD, 0.4f).add();
                part(b, "helm_crest", "helm", Role.STATIC, c).at(0, -1.5f, 0).mat("metal").box(-0.5f, -1, -hd, 1, 1, headD).add();
            }
            case "horned" -> {
                part(b, "helm", "head", Role.STATIC, c).at(0, t, 0).mat("metal").box(-hw, -1, -hd, headW, 5, headD, 0.35f).add();
                sym(b, "helm_horn", "helm", false, 0xE0D8C0).at(hw, 1, 0).rot(0, 0, -50).mat("horn").box(0, -1, -0.5f, 3, 1, 1).add();
                sym(b, "helm_horn_tip", "helm_horn", true, 0xE0D8C0).at(3, 0, 0).rot(0, 0, -45).mat("horn").box(0, -1, -0.5f, 2, 1, 1).add();
            }
            case "hood", "cowl" -> {
                part(b, "hood", "head", Role.STATIC, c).at(0, t, 0).mat(helm.equals("cowl") ? "leather" : "cloth")
                        .box(-hw, -1, -hd + 1, headW, headH + 1, headD - 1, 0.45f).box(-hw, -1, -hd, headW, 2, 1, 0.45f).add();
                if (helm.equals("hood")) {
                    part(b, "hood_tip", "hood", Role.STATIC, c).at(0, 0, hd).rot(30, 0, 0).mat("cloth").box(-2, -1, 0, 4, 4, 2).add();
                }
            }
            case "wizard" -> {
                part(b, "hat_brim", "head", Role.STATIC, c).at(0, t + 1, 0).mat("cloth").box(-hw - 1.5f, -1, -hd - 1.5f, headW + 3, 1, headD + 3).add();
                part(b, "hat", "hat_brim", Role.STATIC, c).at(0, -1, 0).mat("cloth").box(-hw + 1, -3, -hd + 1, headW - 2, 3, headD - 2).add();
                part(b, "hat_mid", "hat", Role.STATIC, c).at(0, -3, 0.5f).rot(-8, 0, 0).mat("cloth").box(-2, -3, -2, 4, 3, 4).add();
                part(b, "hat_tip", "hat_mid", Role.STATIC, c).at(0, -3, 0.5f).rot(-15, 0, 0).mat("cloth").box(-1, -3, -1, 2, 3, 2).add();
                part(b, "hat_band", "hat", Role.STATIC, trim >= 0 ? trim : GOLD).at(0, 0, 0).mat("cloth")
                        .box(-hw + 1, -1, -hd + 1, headW - 2, 1, headD - 2, 0.2f).add();
            }
            case "crown", "tiara" -> {
                part(b, "crown", "head", Role.STATIC, c).at(0, t, 0).mat("metal").box(-hw, -1, -hd, headW, 2, headD, 0.4f).add();
                if (helm.equals("crown")) {
                    for (int i = 0; i < 3; i++) {
                        part(b, "crown_point" + i, "crown", Role.STATIC, c).at(-hw + 1.5f + i * (headW - 3) / 2f, -1, -hd).mat("metal")
                                .box(-0.5f, -2, -0.9f, 1, 2, 1).add();
                    }
                    part(b, "crown_gem", "crown", Role.STATIC, 0xC02020).at(0, -0.5f, -hd).mat("energy").box(-0.5f, -0.5f, -1, 1, 1, 1).add();
                }
            }
            case "bandana" -> part(b, "bandana", "head", Role.STATIC, c).at(0, t, 0).mat("cloth")
                    .box(-hw, 0, -hd, headW, 3, headD, 0.3f).box(-1, 1, hd, 2, 3, 1).add();
            case "chef" -> part(b, "chef_hat", "head", Role.STATIC, c).at(0, t, 0).mat("cloth")
                    .box(-hw + 0.5f, -5, -hd + 0.5f, headW - 1, 6, headD - 1).box(-hw - 0.5f, -7, -hd - 0.5f, headW + 1, 2, headD + 1).add();
            case "coif" -> part(b, "coif", "head", Role.STATIC, c).at(0, t, 0).mat("chain")
                    .box(-hw, -1, -hd + 1, headW, headH + 2, headD - 1, 0.45f).box(-hw, -1, -hd, headW, 2, 1, 0.45f).add();
            case "mask" -> part(b, "mask", "head", Role.STATIC, c).at(0, -headH * 0.45f, -hd).mat("cloth")
                    .box(-hw, 0, -0.6f, headW, headH / 2, 1, 0.3f).add();
            case "cap" -> part(b, "cap", "head", Role.STATIC, c).at(0, t, 0).mat("cloth")
                    .box(-hw, -1, -hd, headW, 3, headD, 0.4f).box(-hw + 1, 1, -hd - 2, headW - 2, 1, 2).add();
            default -> {
            }
        }
    }

    private void shield(ModelSpec.Builder b) {
        if (shield.equals("none")) {
            return;
        }
        int t = shieldTrim >= 0 ? shieldTrim : darker(shieldColor);
        float x = armW / 2f;
        switch (shield) {
            case "kite" -> {
                part(b, "shield", "forearm_l", Role.STATIC, shieldColor).at(x, 1, 0).rot(0, 0, 0).mat("metal")
                        .box(0, -3, -3.5f, 1, 9, 7).add();
                part(b, "shield_point", "shield", Role.STATIC, shieldColor).at(0.5f, 6, 0).rot(45, 0, 0).mat("metal")
                        .box(-0.5f, -2, -2, 1, 4, 4).add();
                part(b, "shield_boss", "shield", Role.STATIC, t).at(1, 0, 0).mat("metal").box(0, -2, -1, 1, 5, 2).box(0, -1, -2.5f, 1, 2, 5).add();
            }
            case "sq" -> {
                part(b, "shield", "forearm_l", Role.STATIC, shieldColor).at(x, 0, 0).mat("metal").box(0, -3, -4, 1, 8, 8).add();
                part(b, "shield_rim", "shield", Role.STATIC, t).at(1, 0, 0).mat("metal")
                        .box(0, -3, -4, 1, 1, 8).box(0, 4, -4, 1, 1, 8).box(0, -2, -0.5f, 1, 6, 1).add();
            }
            case "round", "buckler" -> {
                int s = shield.equals("round") ? 8 : 5;
                part(b, "shield", "forearm_l", Role.STATIC, shieldColor).at(x, 1, 0).mat(shield.equals("round") ? "wood" : "metal")
                        .box(0, -s / 2f, -s / 2f + 1, 1, s, s - 2).box(0, -s / 2f + 1, -s / 2f, 1, s - 2, s).add();
                part(b, "shield_boss", "shield", Role.STATIC, t).at(1, 0, 0).mat("metal").box(0, -1, -1, 1, 2, 2).add();
            }
            case "book" -> part(b, "shield", "hand_l", Role.STATIC, shieldColor).at(0, 1, -1).mat("leather")
                    .box(-1.5f, 0, -2, 3, 4, 3).add();
            default -> {
            }
        }
    }

    private void weapon(ModelSpec.Builder b) {
        if (weapon.equals("none")) {
            return;
        }
        int c = weaponColor;
        float hy = 1;
        part(b, "w_mount", "hand_r", Role.STATIC, WOOD).at(0, hy, 0).rot(weaponPitch, 0, 0).mat("wood")
                .box(-0.5f, -0.5f, -0.5f, 1, 1, 1, -0.2f).add();
        String hand = "w_mount";
        hy = 0;
        switch (weapon) {
            case "sword", "longsword", "dagger" -> {
                int len = weapon.equals("dagger") ? 4 : weapon.equals("sword") ? 7 : 10;
                part(b, "w_grip", hand, Role.STATIC, DARK_LEATHER).at(0, hy, 0).mat("leather").box(-0.5f, -0.5f, -1, 1, 1, 2).add();
                part(b, "w_guard", "w_grip", Role.STATIC, darker(c)).at(0, 0, -1).mat("metal").box(-1.5f, -0.5f, -1, 3, 1, 1).add();
                part(b, "w_blade", "w_guard", Role.STATIC, c).at(0, 0, -1).mat("metal").box(-0.5f, -0.5f, -len, 1, 1, len, 0.0f)
                        .box(-0.5f, -0.2f, -len - 1, 1, 1, 1, -0.2f).add();
            }
            case "scimitar" -> {
                part(b, "w_grip", hand, Role.STATIC, DARK_LEATHER).at(0, hy, 0).mat("leather").box(-0.5f, -0.5f, -1, 1, 1, 2).add();
                part(b, "w_guard", "w_grip", Role.STATIC, darker(c)).at(0, 0, -1).mat("metal").box(-0.5f, -1, -1, 1, 2, 1).add();
                part(b, "w_blade", "w_guard", Role.STATIC, c).at(0, 0, -1).rot(-8, 0, 0).mat("metal").box(-0.5f, -1, -4, 1, 2, 4).add();
                part(b, "w_blade2", "w_blade", Role.STATIC, c).at(0, 0, -4).rot(-18, 0, 0).mat("metal").box(-0.5f, -1, -3, 1, 2, 3).add();
                part(b, "w_tip", "w_blade2", Role.STATIC, c).at(0, 0, -3).rot(-25, 0, 0).mat("metal").box(-0.5f, -0.5f, -2, 1, 1, 2).add();
            }
            case "mace", "warhammer", "hammers" -> {
                part(b, "w_handle", hand, Role.STATIC, WOOD).at(0, hy, 0).mat("wood").box(-0.5f, -0.5f, -7, 1, 1, 9).add();
                if (weapon.equals("mace")) {
                    part(b, "w_head", "w_handle", Role.STATIC, c).at(0, 0, -7).mat("metal").box(-1.5f, -1.5f, -3, 3, 3, 3)
                            .box(-0.5f, -2.5f, -2, 1, 5, 1).box(-2.5f, -0.5f, -2, 5, 1, 1).add();
                } else {
                    part(b, "w_head", "w_handle", Role.STATIC, c).at(0, 0, -7).mat("metal").box(-1.5f, -2.5f, -2, 3, 5, 2).add();
                }
                if (weapon.equals("hammers")) {
                    part(b, "w2_handle", "hand_l", Role.STATIC, WOOD).at(0, hy, 0).mat("wood").box(-0.5f, -0.5f, -7, 1, 1, 9).add();
                    part(b, "w2_head", "w2_handle", Role.STATIC, c).at(0, 0, -7).mat("metal").box(-1.5f, -2.5f, -2, 3, 5, 2).add();
                }
            }
            case "axe", "battleaxe", "greataxe", "pickaxe" -> {
                boolean big = !weapon.equals("axe") && !weapon.equals("pickaxe");
                int len = weapon.equals("greataxe") ? 14 : big ? 10 : 8;
                part(b, "w_handle", hand, Role.STATIC, WOOD).at(0, hy, 0).mat("wood").box(-0.5f, -0.5f, -len + 2, 1, 1, len).add();
                if (weapon.equals("pickaxe")) {
                    part(b, "w_head", "w_handle", Role.STATIC, c).at(0, 0, -len + 2).rot(0, 0, 0).mat("metal")
                            .box(-0.5f, -4, -1.5f, 1, 8, 2).add();
                } else {
                    part(b, "w_head", "w_handle", Role.STATIC, c).at(0, 0, -len + 3).mat("metal")
                            .box(-0.5f, -1, -3, 1, big ? 5 : 3, 3).box(-0.5f, -2, -3.5f, 1, big ? 7 : 5, 1).add();
                    if (big) {
                        part(b, "w_head2", "w_handle", Role.STATIC, c).at(0, 0, -len + 3).mat("metal")
                                .box(-0.5f, -4, -3, 1, 3, 3).add();
                    }
                }
            }
            case "club" -> {
                part(b, "w_handle", hand, Role.STATIC, WOOD).at(0, hy, 0).rot(-10, 0, 0).mat("wood").box(-1, -1, -6, 2, 2, 8).add();
                part(b, "w_head", "w_handle", Role.STATIC, darker(WOOD)).at(0, 0, -6).rot(5, 0, 0).mat("wood").box(-2, -2, -7, 4, 4, 7).add();
                part(b, "w_knot", "w_head", Role.STATIC, darker(WOOD)).at(0, -2, -4).mat("wood").box(-1, -1, -1, 2, 1, 2).add();
            }
            case "spear", "warspear", "halberd", "pitchfork" -> {
                part(b, "w_shaft", hand, Role.STATIC, weapon.equals("warspear") ? darker(c) : WOOD).at(0, hy, 0)
                        .mat(weapon.equals("warspear") ? "metal" : "wood").box(-0.5f, -0.5f, -14, 1, 1, 20).add();
                if (weapon.equals("halberd")) {
                    part(b, "w_head", "w_shaft", Role.STATIC, c).at(0, 0, -14).mat("metal").box(-0.5f, -3, -3, 1, 4, 3)
                            .box(-0.5f, -0.5f, -5, 1, 1, 2).add();
                } else if (weapon.equals("pitchfork")) {
                    part(b, "w_head", "w_shaft", Role.STATIC, IRON).at(0, 0, -14).mat("metal").box(-0.5f, -1.5f, -1, 1, 3, 1)
                            .box(-0.5f, -1.5f, -4, 1, 1, 3).box(-0.5f, 0.5f, -4, 1, 1, 3).add();
                } else {
                    part(b, "w_head", "w_shaft", Role.STATIC, c).at(0, 0, -14).mat("metal").box(-0.5f, -1, -3, 1, 2, 3)
                            .box(-0.5f, -0.5f, -4, 1, 1, 1).add();
                    if (weapon.equals("warspear")) {
                        part(b, "w_wings", "w_head", Role.STATIC, c).at(0, 0, 0).mat("metal").box(-0.5f, -2.5f, -1, 1, 5, 1).add();
                    }
                }
            }
            case "staff", "battlestaff", "wand" -> {
                int len = weapon.equals("wand") ? 6 : 18;
                part(b, "w_staff", hand, Role.STATIC, weapon.equals("battlestaff") ? 0x3A3A3A : WOOD).at(0, hy, -0.5f)
                        .mat("wood").box(-0.5f, -len + 4, -0.5f, 1, len, 1).add();
                part(b, "w_orb", "w_staff", Role.STATIC, c).at(0, -len + 4, 0).mat("energy").glow("full")
                        .box(-1, -2, -1, 2, 2, 2).add();
                if (!weapon.equals("wand")) {
                    part(b, "w_claw", "w_staff", Role.STATIC, weapon.equals("battlestaff") ? 0x3A3A3A : darker(WOOD)).at(0, -len + 4, 0).mat("wood")
                            .box(-1.5f, -1, -0.5f, 3, 1, 1).box(-0.5f, -1, -1.5f, 1, 1, 3).add();
                }
            }
            case "shortbow", "longbow" -> {
                int len = weapon.equals("longbow") ? 8 : 6;
                part(b, "w_bow", "hand_l", Role.STATIC, c).at(0, hy, 0).mat("wood").box(-0.5f, -0.5f, -1, 1, 1, 2).add();
                part(b, "w_limb_u", "w_bow", Role.STATIC, c).at(0, 0, -0.5f).rot(30, 0, 0).mat("wood").box(-0.5f, -len, -0.5f, 1, len, 1).add();
                part(b, "w_limb_d", "w_bow", Role.STATIC, c).at(0, 0, -0.5f).rot(-30, 0, 0).mat("wood").box(-0.5f, 0, -0.5f, 1, len, 1).add();
                part(b, "w_string", "w_bow", Role.STATIC, 0xE0E0D0).at(0, 0, 3.5f).mat("cloth").box(-0.25f, -len + 1, 0, 1, len * 2 - 2, 1, -0.35f).add();
            }
            case "crossbow" -> {
                part(b, "w_stock", hand, Role.STATIC, WOOD).at(0, hy, 0).mat("wood").box(-0.5f, -0.5f, -6, 1, 1, 7).add();
                part(b, "w_limbs", "w_stock", Role.STATIC, c).at(0, 0, -5).mat("metal").box(-3.5f, -0.5f, -0.5f, 7, 1, 1).add();
            }
            case "flail" -> {
                part(b, "w_handle", hand, Role.STATIC, WOOD).at(0, hy, 0).mat("wood").box(-0.5f, -0.5f, -5, 1, 1, 6).add();
                part(b, "w_chain", "w_handle", Role.STATIC, c).at(0, 0, -5).rot(60, 0, 0).mat("chain").box(-0.5f, 0, -0.5f, 1, 3, 1).add();
                part(b, "w_ball", "w_chain", Role.STATIC, c).at(0, 3, 0).mat("metal").box(-1.5f, 0, -1.5f, 3, 3, 3)
                        .box(-0.5f, -0.5f, -2.5f, 1, 1, 5).add();
            }
            case "scythe" -> {
                part(b, "w_shaft", hand, Role.STATIC, WOOD).at(0, hy, -0.5f).mat("wood").box(-0.5f, -14, -0.5f, 1, 18, 1).add();
                part(b, "w_blade", "w_shaft", Role.STATIC, c).at(0, -14, 0).rot(0, 0, 0).mat("metal").box(-0.5f, -1, -8, 1, 2, 8).add();
            }
            case "whip" -> part(b, "w_whip", hand, Role.STATIC, c).at(0, hy, 0).rot(40, 0, 0).mat("leather")
                    .box(-0.5f, 0, -0.5f, 1, 8, 1).add();
            default -> {
            }
        }
    }

    static int darker(int c) {
        int r = (c >> 16 & 0xFF) * 3 / 4;
        int g = (c >> 8 & 0xFF) * 3 / 4;
        int bl = (c & 0xFF) * 3 / 4;
        return r << 16 | g << 8 | bl;
    }
}
