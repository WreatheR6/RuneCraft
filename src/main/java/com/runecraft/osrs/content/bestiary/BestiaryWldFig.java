package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/**
 * Everyday-world humanoid sculptor with OSRS proportions: a broad tapering chest over a narrower waist, chunky
 * segmented limbs, big hands (palm, curled fingers, thumb) and big boots (shaft, foot, toe cap, sole), and a
 * head built from a skull, jaw, nose, ears and brow so goblins, ogres, trolls, dwarves and men read differently.
 * Garments are layered as separate offset pieces (collars, hems, belts with buckles and pouches, tassets,
 * stacked pauldrons, skirts in tiers, capes, hoods, hats, helmets) and weapons/shields have real shapes.
 *
 * <p>Coordinates follow ModelSpec (Y=24 is the ground, smaller Y is higher, -Z is the front, "_l" at +X).
 * The right hand (-X) holds the weapon, the left forearm carries the shield.
 */
final class BestiaryWldFig {
    final String id;
    // ---- proportions ----
    int legLen = 12;
    int legW = 4;
    int hipW = 8;
    int waistW = 8;
    int waistD = 5;
    int chestW = 10;
    int chestD = 6;
    int torsoH = 10;
    int armT = 4;
    int upperLen = 5;
    int foreLen = 5;
    int handW = 5;
    int headW = 8;
    int headH = 8;
    int headD = 8;
    int footL = 7;
    int belly;
    float hunch;
    float stance;
    float armSpread = 6;
    float armPitch;
    float elbow = 14;
    float scale = 1f;
    float shadow = 0.5f;
    // ---- skin & head ----
    int skin = Humanoid.SKIN;
    int skinAccent = -1;
    String skinMat = "skin";
    String skinGlow;
    String face = "face";
    String glowEyes;
    int eyeAccent = -1;
    String nose = "small";
    String ears = "round";
    String jaw = "none";
    boolean brow;
    int hair = Humanoid.HAIR_BROWN;
    String hairStyle = "short";
    int beardCol = -1;
    String beard = "none";
    // ---- garments ----
    int top = 0x7A6A50;
    String topStyle = "shirt";
    int trim = -1;
    String sleeves = "long";
    int sleeveCol = -1;
    int bottoms = 0x4A4A5A;
    String bottomStyle = "trousers";
    int boots = Humanoid.DARK_LEATHER;
    String bootStyle = "boot";
    int gloves = -1;
    int belt = Humanoid.LEATHER;
    boolean pouch;
    int cape = -1;
    int pauldron = -1;
    String helm = "none";
    int helmCol;
    int helmTrim = -1;
    String shield = "none";
    int shieldCol;
    int shieldTrim = -1;
    String weapon = "none";
    int weaponCol;
    int weaponGlow = -1;
    float weaponTilt;
    float weaponRoll = -16;
    boolean weaponLow;

    private BestiaryWldFig(String id) {
        this.id = id;
    }

    static BestiaryWldFig of(String id) {
        return new BestiaryWldFig(id);
    }

    // ------------------------------------------------------------------------------------------
    // Fluent setters
    // ------------------------------------------------------------------------------------------

    BestiaryWldFig legs(int len, int w) {
        legLen = len;
        legW = w;
        return this;
    }

    BestiaryWldFig torso(int hipW, int waistW, int chestW, int chestD, int torsoH) {
        this.hipW = hipW;
        this.waistW = waistW;
        this.chestW = chestW;
        this.chestD = chestD;
        this.waistD = Math.max(3, chestD - 1);
        this.torsoH = torsoH;
        return this;
    }

    BestiaryWldFig arms(int thickness, int upper, int fore, int hand) {
        armT = thickness;
        upperLen = upper;
        foreLen = fore;
        handW = hand;
        return this;
    }

    BestiaryWldFig head(int w, int h, int d) {
        headW = w;
        headH = h;
        headD = d;
        return this;
    }

    BestiaryWldFig foot(int len) {
        footL = len;
        return this;
    }

    BestiaryWldFig belly(int b) {
        belly = b;
        return this;
    }

    BestiaryWldFig hunch(float deg) {
        hunch = deg;
        return this;
    }

    BestiaryWldFig stance(float deg) {
        stance = deg;
        return this;
    }

    BestiaryWldFig pose(float armPitch, float spread, float elbow) {
        this.armPitch = armPitch;
        this.armSpread = spread;
        this.elbow = elbow;
        return this;
    }

    BestiaryWldFig scale(float s) {
        scale = s;
        return this;
    }

    BestiaryWldFig shadow(float s) {
        shadow = s;
        return this;
    }

    BestiaryWldFig skin(int c) {
        skin = c;
        return this;
    }

    BestiaryWldFig skin(int c, String mat, int accent) {
        skin = c;
        skinMat = mat;
        skinAccent = accent;
        return this;
    }

    /** "face" (painted human face), "eyes" (fierce creature eyes), "skull", "none". */
    BestiaryWldFig face(String f) {
        face = f;
        return this;
    }

    BestiaryWldFig glowEyes(int col) {
        glowEyes = "eyes";
        eyeAccent = col;
        return this;
    }

    BestiaryWldFig eyeColour(int col) {
        eyeAccent = col;
        return this;
    }

    /** none, small, big, hooked (goblin), broad (ogre/troll), long. */
    BestiaryWldFig nose(String n) {
        nose = n;
        return this;
    }

    /** none, round, pointy (elf), goblin (huge), small. */
    BestiaryWldFig ears(String e) {
        ears = e;
        return this;
    }

    /** none, heavy (square chin), underbite (ogre, with tusks), tusks, fangs. */
    BestiaryWldFig jaw(String j) {
        jaw = j;
        return this;
    }

    BestiaryWldFig brow() {
        brow = true;
        return this;
    }

    /** none, short, long, ponytail, spiky, mohawk, bun, wild. */
    BestiaryWldFig hair(int c, String style) {
        hair = c;
        hairStyle = style;
        return this;
    }

    /** none, short, long, forked, moustache, goatee. */
    BestiaryWldFig beard(int c, String style) {
        beardCol = c;
        beard = style;
        return this;
    }

    /** shirt, tunic, vest, robe, plate, chain, leather, fur, bare, rags, apron. */
    BestiaryWldFig top(int c, String style) {
        top = c;
        topStyle = style;
        return this;
    }

    BestiaryWldFig trim(int c) {
        trim = c;
        return this;
    }

    /** long, short, none, wide. */
    BestiaryWldFig sleeves(String s) {
        sleeves = s;
        return this;
    }

    BestiaryWldFig sleeves(String s, int c) {
        sleeves = s;
        sleeveCol = c;
        return this;
    }

    /** trousers, shorts, kilt, skirt, robe, loincloth, plate, rags. */
    BestiaryWldFig bottoms(int c, String style) {
        bottoms = c;
        bottomStyle = style;
        return this;
    }

    /** boot, shoe, bare, sabaton, sandal. */
    BestiaryWldFig boots(int c, String style) {
        boots = c;
        bootStyle = style;
        return this;
    }

    BestiaryWldFig gloves(int c) {
        gloves = c;
        return this;
    }

    BestiaryWldFig belt(int c) {
        belt = c;
        return this;
    }

    BestiaryWldFig pouch() {
        pouch = true;
        return this;
    }

    BestiaryWldFig cape(int c) {
        cape = c;
        return this;
    }

    BestiaryWldFig pauldrons(int c) {
        pauldron = c;
        return this;
    }

    /** none, med, full, horned, hood, wizard, cap, straw, bandana, coif, crown, chef, tricorn, skullcap. */
    BestiaryWldFig helm(String h, int c) {
        helm = h;
        helmCol = c;
        return this;
    }

    BestiaryWldFig helmTrim(int c) {
        helmTrim = c;
        return this;
    }

    /** none, kite, round, square, buckler. */
    BestiaryWldFig shield(String s, int c, int trim) {
        shield = s;
        shieldCol = c;
        shieldTrim = trim;
        return this;
    }

    /** none, sword, longsword, scimitar, dagger, spear, staff, battleaxe, club, pitchfork, mace, bow, pickaxe, halberd, hammer. */
    BestiaryWldFig weapon(String w, int c) {
        weapon = w;
        weaponCol = c;
        return this;
    }

    /** Extra forward tilt and outward roll (negative = away from the body) of the held weapon, in degrees. */
    BestiaryWldFig weaponPose(float tilt, float roll) {
        weaponTilt = tilt;
        weaponRoll = roll;
        return this;
    }

    /** Carry the weapon low at the side, pointing forward-down (giants' clubs). */
    BestiaryWldFig weaponLow() {
        weaponLow = true;
        weaponTilt = 50;
        return this;
    }

    BestiaryWldFig weaponGlow(int c) {
        weaponGlow = c;
        return this;
    }

    // ------------------------------------------------------------------------------------------
    // Build
    // ------------------------------------------------------------------------------------------

    ModelSpec build() {
        return builder().build();
    }

    static int dk(int c, float f) {
        int r = (int) ((c >> 16 & 0xFF) * f);
        int g = (int) ((c >> 8 & 0xFF) * f);
        int bl = (int) ((c & 0xFF) * f);
        return Math.min(255, r) << 16 | Math.min(255, g) << 8 | Math.min(255, bl);
    }

    static int mix(int a, int b, float t) {
        int r = (int) ((a >> 16 & 0xFF) * (1 - t) + (b >> 16 & 0xFF) * t);
        int g = (int) ((a >> 8 & 0xFF) * (1 - t) + (b >> 8 & 0xFF) * t);
        int bl = (int) ((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return r << 16 | g << 8 | bl;
    }

    private Kit.P skinPart(ModelSpec.Builder b, String name, String parent, Role role) {
        Kit.P p = part(b, name, parent, role, skin).mat(skinMat);
        if (skinAccent >= 0) {
            p.accent(skinAccent);
        }
        if (skinGlow != null) {
            p.glow(skinGlow);
        }
        return p;
    }

    private Kit.Sym skinSym(ModelSpec.Builder b, String name, String parent, boolean sided) {
        Kit.Sym p = sym(b, name, parent, sided, skin).mat(skinMat);
        if (skinAccent >= 0) {
            p.accent(skinAccent);
        }
        if (skinGlow != null) {
            p.glow(skinGlow);
        }
        return p;
    }

    /** Emissive detail on every bare-skin part ("cracks" for TzHaar lava veins). */
    BestiaryWldFig skinGlow(String g) {
        skinGlow = g;
        return this;
    }

    private boolean isArmour() {
        return topStyle.equals("plate") || topStyle.equals("chain");
    }

    private String topMat() {
        return switch (topStyle) {
            case "plate" -> "metal";
            case "chain" -> "chain";
            case "leather" -> "leather";
            case "vest" -> skinMat;
            case "fur" -> "fur";
            case "bare" -> skinMat;
            case "rags" -> "cloth";
            default -> "cloth";
        };
    }

    ModelSpec.Builder builder() {
        ModelSpec.Builder b = ModelSpec.builder(id).scale(scale).shadow(shadow);
        float hipY = 24 - legLen;
        int footH = legLen >= 9 ? 2 : 1;
        int thighH = (legLen - footH + 1) / 2;
        int shinH = legLen - footH - thighH;
        boolean bareTop = topStyle.equals("bare") || topStyle.equals("vest");
        int topC = bareTop ? skin : top;
        String topM = topMat();
        int chestH = torsoH - 4;
        int trimC = trim >= 0 ? trim : dk(top, 0.7f);

        // ---------------- pelvis ----------------
        boolean bareHips = bottomStyle.equals("loincloth") || bottomStyle.equals("rags");
        int hipC = bareHips ? skin : bottoms;
        String hipM = bareHips ? skinMat : bottomStyle.equals("plate") ? "metal" : "cloth";
        part(b, "hips", null, Role.BODY, hipC).at(0, hipY, 0).mat(hipM)
                .box(-hipW / 2f, -3, -waistD / 2f, hipW, 4, waistD).add();

        // ---------------- torso ----------------
        Kit.P torso = part(b, "torso", "hips", Role.STATIC, topC).at(0, -2, 0).rot(hunch, 0, 0).mat(topM)
                .box(-waistW / 2f, -4, -waistD / 2f, waistW, 5, waistD)
                .box(-chestW / 2f, -torsoH, -chestD / 2f, chestW, chestH, chestD);
        if (skinAccent >= 0 && bareTop) {
            torso.accent(skinAccent);
        }
        if (skinGlow != null && bareTop) {
            torso.glow(skinGlow);
        }
        torso.add();
        // trapezius / upper back hump
        part(b, "traps", "torso", Role.STATIC, topC).at(0, -torsoH, 0.5f).rot(-8, 0, 0).mat(topM)
                .box(-chestW / 2f + 1.5f, -1.5f, -chestD / 2f + 1, chestW - 3, 2, chestD - 1).add();
        if (belly > 0) {
            part(b, "belly", "torso", Role.STATIC, topC).at(0, -4, -waistD / 2f).mat(topM)
                    .box(-waistW / 2f + 0.5f, -3, -belly, waistW - 1, 6, belly + 1).add();
        }
        if (topStyle.equals("bare")) {
            // pecs and abdomen crease
            sym(b, "pec", "torso", false, skin).at(chestW / 4f, -torsoH + 1.5f, -chestD / 2f).rot(4, 0, 0).mat(skinMat)
                    .box(-chestW / 4f + 0.25f, 0, -0.75f, Math.max(2, chestW / 2 - 1), 3, 1).add();
            part(b, "abs", "torso", Role.STATIC, dk(skin, 0.86f)).at(0, -4, -waistD / 2f).mat(skinMat)
                    .box(-waistW / 4f, -1, -0.5f, Math.max(2, waistW / 2), 4, 1).add();
        }
        switch (topStyle) {
            case "shirt", "tunic", "rags", "apron" -> {
                part(b, "collar", "torso", Role.STATIC, trimC).at(0, -torsoH, -0.5f).mat("cloth")
                        .box(-2.5f, -0.5f, -chestD / 2f - 0.4f, 5, 1, chestD - 1).add();
                part(b, "hem", "torso", Role.STATIC, topStyle.equals("tunic") ? top : dk(top, 0.9f)).at(0, 0, 0)
                        .mat("cloth").box(-hipW / 2f - 0.5f, -1, -waistD / 2f - 0.5f, hipW + 1, topStyle.equals("tunic") ? 4 : 2, waistD + 1).add();
                if (topStyle.equals("rags")) {
                    part(b, "tatter", "hem", Role.STATIC, dk(top, 0.8f)).at(0, 1, 0).mat("cloth")
                            .box(-hipW / 2f, 0, -waistD / 2f - 0.6f, 2, 3, 1).box(hipW / 2f - 3, 0, -waistD / 2f - 0.6f, 2, 2, 1)
                            .box(-1, 0, waistD / 2f - 0.4f, 3, 3, 1).add();
                }
                if (topStyle.equals("apron")) {
                    part(b, "apron", "torso", Role.STATIC, trimC).at(0, -chestH + 1, -chestD / 2f).rot(-3, 0, 0).mat("cloth")
                            .box(-3, -4, -0.6f, 6, 4, 1).box(-3.5f, 0, -0.1f, 7, 7, 1).add();
                }
                if (topStyle.equals("tunic")) {
                    part(b, "placket", "torso", Role.STATIC, trimC).at(0, -torsoH, -chestD / 2f).mat("cloth")
                            .box(-0.5f, 0, -0.5f, 1, chestH, 1).add();
                }
            }
            case "vest", "leather" -> {
                part(b, "vest", "torso", Role.STATIC, top).at(0, -torsoH, 0).mat("leather")
                        .box(-chestW / 2f - 0.3f, 0, -chestD / 2f - 0.3f, 3, chestH + 4, chestD, 0.3f)
                        .box(chestW / 2f - 2.7f, 0, -chestD / 2f - 0.3f, 3, chestH + 4, chestD, 0.3f)
                        .box(-chestW / 2f + 0.3f, 0, chestD / 2f - 0.7f, chestW - 1, chestH + 4, 1, 0.3f).add();
                part(b, "vest_lace", "vest", Role.STATIC, dk(top, 0.6f)).at(0, 1, -chestD / 2f).mat("leather")
                        .box(-1.5f, 0, -0.6f, 3, 1, 1).box(-1.5f, 2, -0.6f, 3, 1, 1).add();
                part(b, "hem", "torso", Role.STATIC, dk(top, 0.85f)).at(0, 0, 0).mat("leather")
                        .box(-hipW / 2f - 0.5f, -1, -waistD / 2f - 0.5f, hipW + 1, 2, waistD + 1).add();
            }
            case "fur" -> {
                part(b, "pelt", "torso", Role.STATIC, top).at(0, -torsoH, 0).mat("fur")
                        .box(-chestW / 2f - 0.5f, -1, -chestD / 2f - 0.5f, chestW + 1, 3, chestD + 1)
                        .box(-chestW / 2f - 0.4f, 1, chestD / 2f - 1, chestW + 1, chestH, 2).add();
                part(b, "pelt_strap", "torso", Role.STATIC, Humanoid.DARK_LEATHER).at(0, -torsoH + 1, -chestD / 2f).rot(0, 0, 35)
                        .mat("leather").box(-0.75f, -1, -0.6f, 2, chestH + 3, 1).add();
                sym(b, "pelt_tail", "torso", false, dk(top, 0.85f)).at(chestW / 2f - 1, -torsoH + 1, -chestD / 2f).rot(-5, 0, 6)
                        .mat("fur").box(-1, 0, -0.8f, 2, 4, 1).add();
            }
            case "robe" -> {
                part(b, "collar", "torso", Role.STATIC, trimC).at(0, -torsoH, 0).mat("cloth")
                        .box(-3.5f, -1, -chestD / 2f - 0.3f, 7, 2, chestD, 0.3f).add();
                part(b, "robe_front", "torso", Role.STATIC, trimC).at(0, -torsoH + 1, -chestD / 2f).mat("cloth")
                        .box(-1, 0, -0.5f, 2, torsoH - 1, 1).add();
            }
            case "plate" -> plateTop(b, trimC);
            case "chain" -> {
                part(b, "chain_hem", "torso", Role.STATIC, dk(top, 0.9f)).at(0, 0, 0).mat("chain")
                        .box(-hipW / 2f - 0.5f, -1, -waistD / 2f - 0.5f, hipW + 1, 4, waistD + 1).add();
                if (trim >= 0) {
                    part(b, "tabard", "torso", Role.STATIC, trim).at(0, -torsoH + 0.5f, -chestD / 2f).mat("cloth")
                            .box(-2.5f, 0, -0.6f, 5, torsoH + 3, 1).box(-2.5f, 0, chestD - 0.4f, 5, torsoH + 3, 1).add();
                }
            }
            default -> {
            }
        }
        // belt
        if (belt >= 0) {
            part(b, "belt", "torso", Role.STATIC, belt).at(0, 0, 0).mat("leather")
                    .box(-waistW / 2f - 0.5f, -1.5f, -waistD / 2f - 0.5f, waistW + 1, 2, waistD + 1).add();
            part(b, "buckle", "belt", Role.STATIC, Humanoid.GOLD).at(0, -1.5f, -waistD / 2f - 0.5f).mat("metal")
                    .box(-1, 0, -0.6f, 2, 2, 1).add();
            if (pouch) {
                part(b, "pouch", "belt", Role.STATIC, dk(belt, 1.15f)).at(-waistW / 2f + 0.5f, 0, -waistD / 2f + 1).rot(0, 20, 0)
                        .mat("leather").box(-1.5f, 0, -1.5f, 3, 3, 2).add();
            }
        }
        // neck
        skinPart(b, "neck", "torso", Role.STATIC).at(0, -torsoH, -0.5f).box(-1.5f, -2, -1.5f, 3, 2, 3).add();

        legs(b, hipY, thighH, shinH, footH);
        arms(b);
        if (cape >= 0) {
            cape(b);
        }
        head(b);
        hairAndBeard(b);
        helm(b);
        shield(b);
        weapon(b);
        return b;
    }

    // ------------------------------------------------------------------------------------------
    // Armour
    // ------------------------------------------------------------------------------------------

    private void plateTop(ModelSpec.Builder b, int trimC) {
        int c = top;
        int edge = trim >= 0 ? trim : dk(c, 1.2f);
        int chestH = torsoH - 4;
        // breastplate with a raised keel and rim, gorget, two stacked fauld bands
        part(b, "breastplate", "torso", Role.STATIC, c).at(0, -torsoH + 0.5f, -chestD / 2f).rot(-4, 0, 0).mat("metal")
                .box(-chestW / 2f + 0.5f, 0, -1, chestW - 1, chestH, 1).add();
        part(b, "keel", "breastplate", Role.STATIC, edge).at(0, 0.5f, -1).mat("metal").box(-0.5f, 0, -0.6f, 1, chestH - 1, 1).add();
        part(b, "gorget", "torso", Role.STATIC, dk(c, 0.85f)).at(0, -torsoH, 0).mat("metal")
                .box(-3, -1.5f, -chestD / 2f + 0.5f, 6, 2, chestD - 1, 0.25f).add();
        part(b, "fauld", "torso", Role.STATIC, c).at(0, -3.5f, 0).mat("metal")
                .box(-waistW / 2f - 0.5f, 0, -waistD / 2f - 0.5f, waistW + 1, 2, waistD + 1)
                .box(-waistW / 2f - 1, 2, -waistD / 2f - 0.8f, waistW + 2, 2, waistD + 2).add();
        part(b, "fauld_rim", "fauld", Role.STATIC, edge).at(0, 3.5f, 0).mat("metal")
                .box(-waistW / 2f - 1, 0, -waistD / 2f - 0.8f, waistW + 2, 1, 1, 0.1f).add();
        // tassets hang over the thighs
        sym(b, "tasset", "hips", false, c).at(hipW / 4f + 0.3f, 1, -waistD / 2f).rot(-6, 0, -8).mat("metal")
                .box(-2, 0, -1, 4, 4, 1).box(-1.5f, 3, -0.8f, 3, 2, 1).add();
        if (pauldron < 0) {
            pauldron = c;
        }
    }

    // ------------------------------------------------------------------------------------------
    // Legs
    // ------------------------------------------------------------------------------------------

    private void legs(ModelSpec.Builder b, float hipY, int thighH, int shinH, int footH) {
        boolean bareThigh = bottomStyle.equals("loincloth") || bottomStyle.equals("rags") || bottomStyle.equals("kilt")
                || bottomStyle.equals("skirt");
        boolean bareShin = bareThigh || bottomStyle.equals("shorts");
        boolean plate = bottomStyle.equals("plate");
        String legMat = plate ? "metal" : "cloth";
        int thighC = bareThigh ? skin : bottoms;
        int shinC = bareShin ? skin : bottoms;
        float lx = Math.max(legW / 2f, hipW / 4f + 0.2f);
        Kit.Sym leg = sym(b, "leg", null, false, thighC).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT)
                .at(lx, hipY, 0).rot(0, 0, -stance).mat(bareThigh ? skinMat : legMat)
                .box(-legW / 2f, -1, -legW / 2f, legW, thighH + 1, legW);
        if (bareThigh && skinAccent >= 0) {
            leg.accent(skinAccent);
        }
        if (bareThigh && skinGlow != null) {
            leg.glow(skinGlow);
        }
        leg.add();
        Kit.Sym shin = sym(b, "shin", "leg", true, shinC).at(0, thighH, 0).rot(0, 0, stance * 0.7f)
                .mat(bareShin ? skinMat : legMat).box(-legW / 2f + 0.25f, 0, -legW / 2f + 0.25f, legW, shinH, legW, -0.25f);
        if (bareShin && skinAccent >= 0) {
            shin.accent(skinAccent);
        }
        if (bareShin && skinGlow != null) {
            shin.glow(skinGlow);
        }
        shin.add();
        if (bareShin && legW >= 5) {
            Kit.Sym kneecap = sym(b, "kneecap", "shin", true, dk(skin, 1.05f)).at(0, -0.5f, -legW / 2f).mat(skinMat)
                    .box(-legW / 2f + 1, 0, -0.8f, legW - 2, 2, 1);
            if (skinAccent >= 0) {
                kneecap.accent(skinAccent);
            }
            kneecap.add();
            Kit.Sym calf = sym(b, "calf", "shin", true, skin).at(0, 1, legW / 2f).mat(skinMat)
                    .box(-legW / 2f + 0.75f, 0, -0.5f, legW - 1, shinH - 2, 1);
            if (skinAccent >= 0) {
                calf.accent(skinAccent);
            }
            calf.add();
        }
        if (plate) {
            sym(b, "knee", "shin", true, dk(bottoms, 1.15f)).at(0, 0, -legW / 2f).mat("metal")
                    .box(-legW / 2f + 0.5f, -1.5f, -0.8f, legW - 1, 3, 1).add();
            sym(b, "greave", "shin", true, bottoms).at(0, 1.5f, -legW / 2f).mat("metal")
                    .box(-legW / 2f + 0.5f, 0, -0.6f, legW - 1, shinH - 2, 1).add();
        } else if (bottomStyle.equals("trousers")) {
            sym(b, "knee", "shin", true, dk(bottoms, 0.85f)).at(0, -0.5f, -legW / 2f).mat("cloth")
                    .box(-legW / 2f + 0.5f, 0, -0.4f, legW - 1, 1, 1).add();
        }
        // feet
        boolean bare = bootStyle.equals("bare");
        boolean sab = bootStyle.equals("sabaton");
        int fc = bare ? skin : boots;
        String fm = bare ? skinMat : sab ? "metal" : "leather";
        int fl = Math.max(4, footL);
        int fw = legW + 1;
        Kit.Sym foot = sym(b, "foot", "shin", true, fc).at(0, shinH, 0).rot(0, 0, stance * 0.3f).mat(fm)
                .box(-fw / 2f, 0, -legW / 2f - (fl - legW) + 0.5f, fw, footH, fl);
        if (bare && skinAccent >= 0) {
            foot.accent(skinAccent);
        }
        foot.add();
        float toeZ = -legW / 2f - (fl - legW) + 0.5f;
        if (bare) {
            int toes = fw >= 5 ? 3 : 2;
            for (int k = 0; k < toes; k++) {
                float tx = -fw / 2f + 0.5f + k * ((fw - 1.5f) / Math.max(1, toes - 1)) - 0.5f;
                skinSym(b, "toe" + k, "foot", true).at(tx + 0.5f, footH - 0.5f, toeZ).rot(-10, 0, 0)
                        .box(-0.5f, -0.5f, -1, 1, 1, 1, 0.1f).add();
            }
        } else if (bootStyle.equals("boot") || sab) {
            sym(b, "boot_cuff", "shin", true, dk(boots, sab ? 1.15f : 1.12f)).at(0, shinH - 2, 0).mat(fm)
                    .box(-legW / 2f, 0, -legW / 2f, legW, 2, legW, 0.35f).add();
            sym(b, "toecap", "foot", true, dk(boots, sab ? 1.2f : 0.8f)).at(0, 0, toeZ).mat(fm)
                    .box(-fw / 2f + 0.5f, footH - 1.5f, -0.4f, fw - 1, 1, 2, 0.1f).add();
            sym(b, "sole", "foot", true, dk(boots, 0.45f)).at(0, footH - 1, 0).mat("leather")
                    .box(-fw / 2f, 0, toeZ, fw, 1, fl, 0.12f).add();
        } else if (bootStyle.equals("sandal")) {
            sym(b, "sandal_strap", "foot", true, Humanoid.LEATHER).at(0, 0, toeZ + 2).mat("leather")
                    .box(-fw / 2f, -0.2f, -0.5f, fw, 1, 1, 0.15f).add();
        }
        // lower garments
        float wd = waistD / 2f;
        switch (bottomStyle) {
            case "kilt", "skirt" -> {
                boolean kilt = bottomStyle.equals("kilt");
                int len = kilt ? thighH + 1 : thighH + shinH - 1;
                part(b, "skirt", "hips", Role.STATIC, bottoms).at(0, 0, 0).mat("cloth")
                        .box(-hipW / 2f - 0.5f, -1, -wd - 0.5f, hipW + 1, len - 1, waistD + 1).add();
                part(b, "skirt_flare", "skirt", Role.STATIC, dk(bottoms, 0.92f)).at(0, len - 2, 0).mat("cloth")
                        .box(-hipW / 2f - 1, 0, -wd - 1, hipW + 2, 2, waistD + 2).add();
                if (kilt) {
                    part(b, "kilt_flap", "skirt", Role.STATIC, trim >= 0 ? trim : dk(bottoms, 0.75f)).at(0, -1, -wd - 0.5f)
                            .mat("cloth").box(-1.5f, 0, -0.6f, 3, len + 1, 1).add();
                }
            }
            case "robe" -> {
                int len = thighH + shinH;
                part(b, "skirt", "hips", Role.STATIC, bottoms).at(0, 0, 0).mat("cloth")
                        .box(-hipW / 2f - 0.5f, -1, -wd - 0.5f, hipW + 1, len / 2 + 1, waistD + 1).add();
                part(b, "skirt_low", "skirt", Role.STATIC, dk(bottoms, 0.95f)).at(0, len / 2f, 0).rot(0, 0, 0).mat("cloth")
                        .box(-hipW / 2f - 1, 0, -wd - 1, hipW + 2, len - len / 2, waistD + 2).add();
                part(b, "hem", "skirt_low", Role.STATIC, trim >= 0 ? trim : dk(bottoms, 0.7f)).at(0, len - len / 2f - 1, 0)
                        .mat("cloth").box(-hipW / 2f - 1.5f, 0, -wd - 1.5f, hipW + 3, 1, waistD + 3).add();
                part(b, "robe_fold", "skirt_low", Role.STATIC, dk(bottoms, 0.8f)).at(0, -len / 2f, -wd - 1).mat("cloth")
                        .box(-hipW / 4f - 0.5f, 0, -0.5f, 1, len, 1).box(hipW / 4f - 0.5f, 0, -0.5f, 1, len, 1).add();
            }
            case "loincloth", "rags" -> {
                int lc = bottoms;
                part(b, "loin_front", "hips", Role.STATIC, lc).at(0, 0, -wd).rot(-4, 0, 0).mat("cloth")
                        .box(-hipW / 4f - 0.5f, -1, -0.8f, hipW / 2 + 1, thighH + 1, 1).add();
                part(b, "loin_back", "hips", Role.STATIC, dk(lc, 0.9f)).at(0, 0, wd).rot(4, 0, 0).mat("cloth")
                        .box(-hipW / 4f - 1, -1, -0.2f, hipW / 2 + 2, thighH + 2, 1).add();
                part(b, "loin_band", "hips", Role.STATIC, dk(lc, 0.7f)).at(0, -2, 0).mat("leather")
                        .box(-hipW / 2f - 0.3f, 0, -wd - 0.3f, hipW, 1, waistD, 0.3f).add();
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Arms
    // ------------------------------------------------------------------------------------------

    /** Emits a one-sided part: "name_l" (as given) or "name_r" (mirrored); a parent ending in '*' is sided too. */
    static ModelSpec.Builder side(Kit.P p, boolean left) {
        String suf = left ? "_l" : "_r";
        String par = p.parent == null ? null : p.parent.endsWith("*") ? p.parent.substring(0, p.parent.length() - 1) + suf : p.parent;
        p.emit(p.name + suf, par, p.role, left ? p.px : -p.px, p.py, p.pz, p.pitch, left ? p.yaw : -p.yaw, left ? p.roll : -p.roll, !left);
        return p.b;
    }

    private void arms(ModelSpec.Builder b) {
        for (boolean left : new boolean[]{true, false}) {
            arm(b, left);
        }
    }

    private void arm(ModelSpec.Builder b, boolean left) {
        boolean bare = sleeves.equals("none") || topStyle.equals("bare");
        boolean shortS = sleeves.equals("short");
        boolean wide = sleeves.equals("wide");
        boolean plate = topStyle.equals("plate");
        int sc = sleeveCol >= 0 ? sleeveCol : top;
        String sm = plate ? "metal" : topStyle.equals("chain") ? "chain" : "cloth";
        if (topStyle.equals("fur") || topStyle.equals("vest") || topStyle.equals("leather")) {
            bare = bare || sleeveCol < 0;
        }
        int upperC = bare ? skin : sc;
        int foreC = bare || shortS ? skin : sc;
        String upperM = bare ? skinMat : sm;
        String foreM = bare || shortS ? skinMat : sm;
        int skinAcc = skinAccent;
        boolean armed = !left && !weapon.equals("none") && !weaponLow;
        boolean shielded = left && !shield.equals("none");
        float pitch = armPitch - hunch * 0.6f + (armed ? -12 : shielded ? -8 : 0);
        float elb = elbow + (armed ? 34 : shielded ? 20 : 0);
        float spread = armSpread + (armed ? 6 : 0);
        float ax = chestW / 2f + armT / 2f - 0.5f;
        float ay = -torsoH + 2;
        Kit.P arm = part(b, "arm", "torso", left ? Role.ARM_LEFT : Role.ARM_RIGHT, upperC).at(ax, ay, 0)
                .rot(pitch, 0, -spread).mat(upperM)
                .box(-armT / 2f - 0.5f, -2.5f, -armT / 2f - 0.5f, armT + 1, 4, armT + 1)
                .box(-armT / 2f, 0, -armT / 2f, armT, upperLen + 1, armT);
        if (bare && skinAcc >= 0) {
            arm.accent(skinAcc);
        }
        if (bare && skinGlow != null) {
            arm.glow(skinGlow);
        }
        side(arm, left);
        if (bare && armT >= 4) {
            Kit.P bicep = part(b, "bicep", "arm*", Role.STATIC, dk(skin, 1.04f)).at(0, 1, -armT / 2f).rot(-4, 0, 0).mat(skinMat)
                    .box(-armT / 2f + 0.5f, 0, -0.8f, armT - 1, upperLen - 1, 2);
            if (skinAcc >= 0) {
                bicep.accent(skinAcc);
            }
            side(bicep, left);
        }
        Kit.P fore = part(b, "forearm", "arm*", Role.STATIC, foreC).at(0, upperLen + 0.5f, 0).rot(-elb, 0, 2).mat(foreM)
                .box(-armT / 2f, 0, -armT / 2f, armT, foreLen, armT, -0.1f);
        if ((bare || shortS) && skinAcc >= 0) {
            fore.accent(skinAcc);
        }
        if ((bare || shortS) && skinGlow != null) {
            fore.glow(skinGlow);
        }
        side(fore, left);
        if ((bare || shortS) && armT >= 5) {
            Kit.P bulge = part(b, "forearm_bulge", "forearm*", Role.STATIC, skin).at(armT / 2f, 1, 0).rot(0, 0, -4).mat(skinMat)
                    .box(-0.6f, 0, -armT / 2f + 0.5f, 1, foreLen - 2, armT - 1);
            if (skinAcc >= 0) {
                bulge.accent(skinAcc);
            }
            side(bulge, left);
        }
        if (shortS && !bare) {
            side(part(b, "sleeve_hem", "arm*", Role.STATIC, dk(sc, 0.85f)).at(0, upperLen - 1, 0).mat("cloth")
                    .box(-armT / 2f, 0, -armT / 2f, armT, 1, armT, 0.35f), left);
        }
        if (wide) {
            side(part(b, "cuff", "forearm*", Role.STATIC, trim >= 0 ? trim : dk(sc, 0.9f)).at(0, foreLen - 3, 0).rot(10, 0, 0).mat("cloth")
                    .box(-armT / 2f - 1, 0, -armT / 2f - 1, armT + 2, 3, armT + 2), left);
        } else if (!bare && !shortS && !plate) {
            side(part(b, "cuff", "forearm*", Role.STATIC, trim >= 0 ? trim : dk(sc, 0.85f)).at(0, foreLen - 1, 0).mat("cloth")
                    .box(-armT / 2f, 0, -armT / 2f, armT, 1, armT, 0.3f), left);
        }
        if (plate) {
            side(part(b, "vambrace", "forearm*", Role.STATIC, dk(sc, 1.1f)).at(0, 1, 0).mat("metal")
                    .box(-armT / 2f, 0, -armT / 2f, armT, foreLen - 2, armT, 0.35f), left);
            side(part(b, "elbow_cop", "forearm*", Role.STATIC, dk(sc, 1.2f)).at(0, -0.5f, armT / 2f).mat("metal")
                    .box(-1, -1, -0.5f, 2, 2, 1, 0.1f), left);
        }
        if (pauldron >= 0) {
            int pc = pauldron;
            side(part(b, "pauldron", "arm*", Role.STATIC, pc).at(0.5f, -2.5f, 0).rot(0, 0, -14).mat("metal")
                    .box(-armT / 2f - 1, -0.5f, -armT / 2f - 1, armT + 2, 2, armT + 2), left);
            side(part(b, "pauldron_b", "pauldron*", Role.STATIC, dk(pc, 0.9f)).at(0.5f, 1.5f, 0).rot(0, 0, -8).mat("metal")
                    .box(-armT / 2f - 0.8f, 0, -armT / 2f - 0.8f, armT + 2, 2, armT + 2, -0.15f), left);
            side(part(b, "pauldron_rim", "pauldron_b*", Role.STATIC, trim >= 0 ? trim : dk(pc, 1.25f)).at(0.5f, 1.5f, 0).mat("metal")
                    .box(-armT / 2f - 0.8f, 0, -armT / 2f - 0.8f, armT + 2, 1, armT + 2, -0.3f), left);
        }
        // hands: palm + thumb, curled fingers
        int hc = gloves >= 0 ? gloves : plate ? dk(sc, 0.95f) : skin;
        String hm = plate ? "metal" : gloves >= 0 ? "leather" : skinMat;
        Kit.P hand = part(b, "hand", "forearm*", Role.STATIC, hc).at(0, foreLen - 0.5f, 0).rot(armed ? -10 : 0, 0, 0).mat(hm)
                .box(-handW / 2f, 0, -armT / 2f, handW, 3, armT)
                .box(-handW / 2f - 1, 0.5f, -armT / 2f, 1, 2, 2);
        if (hc == skin && skinAcc >= 0) {
            hand.accent(skinAcc);
        }
        side(hand, left);
        Kit.P fing = part(b, "fingers", "hand*", Role.STATIC, dk(hc, 0.93f)).at(0, 2.5f, -armT / 2f + 0.5f).rot(armed ? 60 : 28, 0, 0)
                .mat(hm).box(-handW / 2f, 0, -0.5f, handW, 2, armT - 1);
        if (hc == skin && skinAcc >= 0) {
            fing.accent(skinAcc);
        }
        side(fing, left);
        if (gloves >= 0 || plate) {
            side(part(b, "glove_cuff", "forearm*", Role.STATIC, dk(hc, 1.1f)).at(0, foreLen - 2, 0).mat(hm)
                    .box(-armT / 2f, 0, -armT / 2f, armT, 2, armT, 0.45f), left);
        }
    }

    private void cape(ModelSpec.Builder b) {
        int len = torsoH + legLen - 4;
        part(b, "cape", "torso", Role.STATIC, cape).at(0, -torsoH + 0.5f, chestD / 2f + 0.3f).rot(8 - hunch * 0.5f, 0, 0).mat("cloth")
                .box(-chestW / 2f + 0.5f, 0, 0, chestW - 1, len / 2, 1).add();
        part(b, "cape_low", "cape", Role.TAIL, dk(cape, 0.92f)).at(0, len / 2f, 0).rot(6, 0, 0).mat("cloth")
                .box(-chestW / 2f, 0, 0, chestW, len - len / 2, 1).add();
        part(b, "cape_clasp", "torso", Role.STATIC, Humanoid.GOLD).at(0, -torsoH + 0.5f, -chestD / 2f).mat("metal")
                .box(-chestW / 2f + 1, -0.5f, -0.6f, 1, 1, 1).box(chestW / 2f - 2, -0.5f, -0.6f, 1, 1, 1).add();
        part(b, "cape_collar", "torso", Role.STATIC, dk(cape, 0.8f)).at(0, -torsoH, chestD / 2f - 1).mat("cloth")
                .box(-chestW / 2f + 0.5f, -1.5f, 0, chestW - 1, 2, 2).add();
    }

    // ------------------------------------------------------------------------------------------
    // Head
    // ------------------------------------------------------------------------------------------

    private void head(ModelSpec.Builder b) {
        float hw = headW / 2f;
        float hd = headD / 2f;
        String pat = face.equals("eyes") ? "eyes" : face.equals("skull") ? "skull" : "none";
        Kit.P head = part(b, "head", "torso", Role.HEAD, skin).at(0, -torsoH - 1.5f, -0.5f).rot(-hunch * 0.85f, 0, 0)
                .mat(face.equals("face") ? "face" : skinMat).pattern(pat)
                .box(-hw, -headH, -hd, headW, headH, headD);
        if (glowEyes != null) {
            head.glow(glowEyes);
        }
        if (eyeAccent >= 0) {
            head.accent(eyeAccent);
        } else if (skinAccent >= 0) {
            head.accent(skinAccent);
        }
        head.add();
        // jaw / chin
        switch (jaw) {
            case "heavy" -> skinPart(b, "chin", "head", Role.STATIC).at(0, 0, -hd + 1).box(-hw + 1, -1.5f, -1.5f, headW - 2, 2, 3).add();
            case "underbite", "tusks" -> {
                skinPart(b, "jaw", "head", Role.JAW).at(0, -1.5f, -hd + 1.5f).rot(4, 0, 0)
                        .box(-hw + 0.5f, 0, -2, headW - 1, 3, headD - 2).add();
                sym(b, "tusk", "jaw", false, 0xE8DEC0).at(hw - 2, 0.5f, -2).rot(-12, 0, -8).mat("horn")
                        .box(-0.5f, -2.5f, -0.5f, 1, 3, 1).add();
            }
            case "fangs" -> sym(b, "fang", "head", false, 0xF0E8D0).at(1.5f, -1, -hd).mat("horn")
                    .box(-0.5f, 0, -0.6f, 1, 2, 1, -0.1f).add();
            case "grin" -> {
                skinPart(b, "jaw", "head", Role.JAW).at(0, -1.5f, -hd + 1).rot(6, 0, 0)
                        .box(-hw + 1, 0, -1.5f, headW - 2, 2, 3).add();
                sym(b, "fang", "jaw", false, 0xF0E8D0).at(hw - 2, 0, -1.5f).mat("horn").box(-0.5f, -1.5f, -0.6f, 1, 2, 1, -0.1f).add();
            }
            default -> {
            }
        }
        if (brow) {
            float eyeTop = face.equals("face") ? 0.36f : 0.30f;
            skinPart(b, "brow", "head", Role.STATIC).at(0, -headH * (1 - eyeTop) - 1, -hd).rot(10, 0, 0)
                    .box(-hw, -1, -1, headW, 2, 2, -0.1f).add();
        }
        switch (nose) {
            case "small" -> skinPart(b, "nose", "head", Role.STATIC).at(0, -headH * 0.45f, -hd).box(-0.5f, 0, -1, 1, 2, 1).add();
            case "big" -> skinPart(b, "nose", "head", Role.STATIC).at(0, -headH * 0.48f, -hd).rot(-8, 0, 0)
                    .box(-1, 0, -1.5f, 2, 3, 2).add();
            case "hooked" -> {
                skinPart(b, "nose", "head", Role.STATIC).at(0, -headH * 0.55f, -hd).rot(-20, 0, 0)
                        .box(-1, 0, -2, 2, 3, 2).add();
                skinPart(b, "nose_tip", "nose", Role.STATIC).at(0, 3, -1.5f).rot(30, 0, 0).box(-1, -0.5f, -0.5f, 2, 1, 1, -0.05f).add();
            }
            case "broad" -> skinPart(b, "nose", "head", Role.STATIC).at(0, -headH * 0.48f, -hd).rot(-6, 0, 0)
                    .box(-1.5f, 0, -1.5f, 3, 3, 2).add();
            case "long" -> {
                skinPart(b, "nose", "head", Role.STATIC).at(0, -headH * 0.55f, -hd).rot(-30, 0, 0)
                        .box(-1, 0, -3, 2, 2, 3).add();
                skinPart(b, "nose_tip", "nose", Role.STATIC).at(0, 1, -3).rot(20, 0, 0).box(-1, 0, -1.5f, 2, 2, 2, -0.1f).add();
            }
            default -> {
            }
        }
        boolean hidden = helm.equals("full") || helm.equals("hood") || helm.equals("coif");
        switch (ears) {
            case "round" -> {
                if (!hidden) {
                    skinSym(b, "ear", "head", false).at(hw, -headH * 0.5f, 0.5f).box(-0.2f, -1, -0.5f, 1, 2, 1).add();
                }
            }
            case "small" -> skinSym(b, "ear", "head", false).at(hw, -headH * 0.5f, 0.5f).box(-0.2f, -0.5f, -0.5f, 1, 1, 1).add();
            case "pointy" -> {
                skinSym(b, "ear", "head", false).at(hw - 0.2f, -headH * 0.5f, 0.5f).rot(0, -15, -40).box(0, -1, -0.5f, 2, 2, 1).add();
                skinSym(b, "ear_tip", "ear", true).at(2, -0.5f, 0).rot(0, 0, -20).box(0, -0.5f, -0.5f, 2, 1, 1, -0.05f).add();
            }
            case "goblin" -> {
                skinSym(b, "ear", "head", false).at(hw - 0.3f, -headH * 0.55f, 0.5f).rot(-8, -20, -14).box(0, -2, -0.5f, 3, 3, 1).add();
                skinSym(b, "ear_tip", "ear", true).at(3, -0.5f, 0).rot(0, 0, -18).box(0, -1, -0.5f, 3, 2, 1, -0.05f).add();
                sym(b, "ear_inner", "ear", true, dk(skin, 0.7f)).at(0.5f, -1.5f, -0.55f).mat("flesh").box(0, 0, -0.1f, 2, 2, 1, -0.3f).add();
            }
            default -> {
            }
        }
    }

    private void hairAndBeard(ModelSpec.Builder b) {
        float hw = headW / 2f;
        float hd = headD / 2f;
        float t = -headH;
        boolean covered = !helm.equals("none") && !helm.equals("crown") && !helm.equals("straw");
        if (!hairStyle.equals("none")) {
            if (!covered) {
                part(b, "hair", "head", Role.STATIC, hair).at(0, t, 0).mat("hair")
                        .box(-hw - 0.5f, -1, -hd - 0.5f, headW + 1, 2, headD + 1)
                        .box(-hw - 0.5f, 1, hd - 2.5f, headW + 1, Math.max(2, headH / 2 - 1), 3).add();
                sym(b, "hair_side", "hair", false, dk(hair, 0.92f)).at(hw, 1, 0).mat("hair")
                        .box(-0.5f, 0, -hd + 1.5f, 1, 2, headD - 2).add();
                part(b, "fringe", "hair", Role.STATIC, dk(hair, 1.08f)).at(0, 0.5f, -hd).rot(-12, 0, 0).mat("hair")
                        .box(-hw - 0.25f, 0, -0.75f, headW / 2 + 1, 2, 1).box(0.5f, 0, -0.5f, headW / 2 - 1, 1, 1).add();
            }
            switch (hairStyle) {
                case "long" -> part(b, "hair_long", "head", Role.STATIC, hair).at(0, t + 2, hd).rot(6, 0, 0).mat("hair")
                        .box(-hw - 0.5f, 0, -1, headW + 1, headH + 3, 2).add();
                case "ponytail" -> part(b, "ponytail", "head", Role.STATIC, hair).at(0, t + 3, hd + 0.5f).rot(28, 0, 0).mat("hair")
                        .box(-1, 0, -1, 2, 7, 2).box(-1.5f, -1, -1.5f, 3, 2, 3).add();
                case "bun" -> part(b, "bun", "head", Role.STATIC, hair).at(0, t + 1, hd).mat("hair").box(-1.5f, -2, -1, 3, 3, 3).add();
                case "spiky", "wild" -> {
                    if (!covered) {
                        for (int i = 0; i < 4; i++) {
                            part(b, "spike" + i, "hair", Role.STATIC, dk(hair, i % 2 == 0 ? 1f : 0.85f))
                                    .at(-hw + 1.2f + i * (headW - 2.4f) / 3f, -0.5f, -0.5f + (i % 2) * 2)
                                    .rot(-20 + (i % 2) * 30, 0, -30 + i * 20).mat("hair").box(-1, -3, -1, 2, 3, 2).add();
                        }
                    }
                }
                case "mohawk" -> part(b, "mohawk", "head", Role.STATIC, hair).at(0, t, 0).mat("hair")
                        .box(-1, -3, -hd + 1, 2, 3, headD - 1).add();
                default -> {
                }
            }
        }
        if (!beard.equals("none")) {
            int c = beardCol >= 0 ? beardCol : hair;
            float fy = -headH * 0.36f;
            switch (beard) {
                case "long", "forked" -> {
                    part(b, "beard", "head", Role.STATIC, c).at(0, fy, -hd).rot(6, 0, 0).mat("hair")
                            .box(-hw + 0.5f, 0, -1.5f, headW - 1, headH / 2 + 2, 2).add();
                    sym(b, "sideburn", "head", false, dk(c, 0.92f)).at(hw - 0.5f, -headH * 0.6f, -hd + 1).mat("hair")
                            .box(-0.6f, 0, -1.5f, 1, headH * 6 / 10, 3).add();
                    part(b, "moustache", "head", Role.STATIC, dk(c, 1.1f)).at(0, fy + 0.2f, -hd - 1.2f).mat("hair")
                            .box(-2.5f, 0, -0.6f, 5, 1, 1).add();
                    if (beard.equals("forked")) {
                        sym(b, "beard_fork", "beard", false, dk(c, 0.95f)).at(1.5f, headH / 2f + 2, -0.5f).rot(8, 0, -8).mat("hair")
                                .box(-1, 0, -1, 2, 4, 2).add();
                    } else {
                        part(b, "beard_tip", "beard", Role.STATIC, dk(c, 0.95f)).at(0, headH / 2f + 2, -0.5f).rot(8, 0, 0).mat("hair")
                                .box(-hw + 1.5f, 0, -1, headW - 3, 3, 2).add();
                    }
                }
                case "moustache" -> part(b, "moustache", "head", Role.STATIC, c).at(0, fy + 0.5f, -hd).mat("hair")
                        .box(-2.5f, 0, -0.8f, 5, 1, 1).add();
                case "goatee" -> part(b, "beard", "head", Role.STATIC, c).at(0, -2, -hd).mat("hair").box(-1.5f, 0, -1, 3, 3, 1).add();
                default -> {
                    part(b, "beard", "head", Role.STATIC, c).at(0, fy, -hd).mat("hair")
                            .box(-hw + 0.5f, 0, -1, headW - 1, headH / 3 + 2, 2).add();
                    sym(b, "sideburn", "head", false, dk(c, 0.92f)).at(hw - 0.5f, -headH * 0.6f, -hd + 1).mat("hair")
                            .box(-0.6f, 0, -1.5f, 1, headH / 2, 3).add();
                }
            }
        }
    }

    private void helm(ModelSpec.Builder b) {
        float hw = headW / 2f;
        float hd = headD / 2f;
        float t = -headH;
        int c = helmCol;
        int tc = helmTrim >= 0 ? helmTrim : dk(c, 1.2f);
        switch (helm) {
            case "med", "horned" -> {
                part(b, "helm", "head", Role.STATIC, c).at(0, t, 0).mat("metal")
                        .box(-hw - 0.5f, -1.5f, -hd - 0.5f, headW + 1, 5, headD + 1).add();
                part(b, "helm_band", "helm", Role.STATIC, tc).at(0, 2.5f, 0).mat("metal")
                        .box(-hw - 0.5f, 0, -hd - 0.5f, headW + 1, 1, headD + 1, 0.2f).add();
                part(b, "helm_ridge", "helm", Role.STATIC, tc).at(0, -1.5f, 0).mat("metal")
                        .box(-0.5f, -1, -hd - 0.5f, 1, 1, headD + 1).add();
                part(b, "helm_nose", "helm", Role.STATIC, c).at(0, 3, -hd - 0.5f).mat("metal").box(-0.5f, 0, -0.7f, 1, 3, 1).add();
                sym(b, "helm_cheek", "helm", false, dk(c, 0.9f)).at(hw + 0.2f, 3, -hd + 1).mat("metal")
                        .box(-0.8f, 0, -1.5f, 1, 3, 3).add();
                if (helm.equals("horned")) {
                    int hc = 0xE8DCC0;
                    sym(b, "horn", "helm", false, hc).at(hw + 0.3f, 1, 0).rot(0, 0, -70).mat("horn").box(-1, -3, -1, 2, 3, 2).add();
                    sym(b, "horn_mid", "horn", true, dk(hc, 0.95f)).at(0, -3, 0).rot(0, 0, 40).mat("horn").box(-0.75f, -3, -0.75f, 2, 3, 2, -0.25f).add();
                    sym(b, "horn_tip", "horn_mid", true, dk(hc, 0.85f)).at(0, -3, 0).rot(0, 0, 30).mat("horn").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
                }
            }
            case "full" -> {
                part(b, "helm", "head", Role.STATIC, c).at(0, t, 0).mat("metal").pattern("visor")
                        .box(-hw - 0.6f, -1.2f, -hd - 0.6f, headW + 1, headH + 1, headD + 1, 0.1f).add();
                part(b, "helm_crest", "helm", Role.STATIC, tc).at(0, -1.2f, 0).mat("metal")
                        .box(-0.5f, -1.5f, -hd - 0.4f, 1, 2, headD + 1).add();
                part(b, "helm_visor", "helm", Role.STATIC, dk(c, 1.1f)).at(0, headH * 0.42f, -hd - 0.6f).rot(-6, 0, 0).mat("metal")
                        .box(-hw + 0.5f, 0, -0.8f, headW - 1, 2, 1).add();
                part(b, "helm_chin", "helm", Role.STATIC, dk(c, 0.92f)).at(0, headH - 1.2f, -hd).mat("metal")
                        .box(-hw + 1, -1, -1.2f, headW - 2, 2, 2).add();
                if (helmTrim >= 0) {
                    part(b, "plume", "helm", Role.STATIC, helmTrim).at(0, -2, 1).rot(-20, 0, 0).mat("feathers")
                            .box(-0.5f, -4, -1, 1, 4, 3).box(-0.5f, -3, 2, 1, 5, 2).add();
                }
            }
            case "hood" -> {
                part(b, "hood", "head", Role.STATIC, c).at(0, t, 0).mat("cloth")
                        .box(-hw - 0.7f, -1.2f, -hd - 0.4f, headW + 1, headH - 1, headD + 1, 0.2f).add();
                part(b, "hood_rim", "hood", Role.STATIC, dk(c, 0.75f)).at(0, -1, -hd - 0.5f).mat("cloth")
                        .box(-hw - 0.7f, 0, -0.6f, headW + 1, 2, 1, 0.1f)
                        .box(-hw - 0.7f, 1, -0.6f, 1, headH - 2, 1, 0.1f).box(hw - 0.3f, 1, -0.6f, 1, headH - 2, 1, 0.1f).add();
                part(b, "hood_drape", "hood", Role.STATIC, dk(c, 0.9f)).at(0, headH - 3, hd).rot(14, 0, 0).mat("cloth")
                        .box(-hw - 0.5f, 0, -1, headW + 1, 5, 2).add();
                part(b, "hood_tip", "hood", Role.STATIC, dk(c, 0.95f)).at(0, -1, hd - 1).rot(-35, 0, 0).mat("cloth")
                        .box(-1.5f, -2, -1, 3, 3, 3).add();
            }
            case "coif" -> {
                part(b, "coif", "head", Role.STATIC, c).at(0, t, 0).mat("chain")
                        .box(-hw - 0.6f, -1, -hd - 0.4f, headW + 1, headH, headD + 1, 0.15f).add();
                part(b, "coif_mantle", "coif", Role.STATIC, dk(c, 0.9f)).at(0, headH - 0.5f, 0).mat("chain")
                        .box(-hw - 1, 0, -hd - 0.5f, headW + 2, 2, headD + 1).add();
            }
            case "wizard" -> {
                part(b, "hat_brim", "head", Role.STATIC, c).at(0, t + 1, 0).rot(-4, 0, 0).mat("cloth")
                        .box(-hw - 2.5f, -1, -hd - 2.5f, headW + 5, 1, headD + 5).add();
                part(b, "hat", "hat_brim", Role.STATIC, c).at(0, -1, 0.5f).rot(-6, 0, 0).mat("cloth")
                        .box(-hw + 0.5f, -3, -hd + 0.5f, headW - 1, 3, headD - 1).add();
                part(b, "hat_mid", "hat", Role.STATIC, dk(c, 0.95f)).at(0, -3, 0.5f).rot(-10, 0, 0).mat("cloth")
                        .box(-hw + 1.5f, -3, -hd + 1.5f, headW - 3, 3, headD - 3).add();
                part(b, "hat_tip", "hat_mid", Role.STATIC, dk(c, 0.9f)).at(0, -3, 0.5f).rot(-22, 0, 0).mat("cloth")
                        .box(-1, -3, -1, 2, 3, 2).add();
                part(b, "hat_band", "hat", Role.STATIC, tc).at(0, -0.5f, 0).mat("cloth")
                        .box(-hw + 0.5f, -0.5f, -hd + 0.5f, headW - 1, 1, headD - 1, 0.25f).add();
            }
            case "straw", "cap" -> {
                boolean straw = helm.equals("straw");
                part(b, "hat_brim", "head", Role.STATIC, c).at(0, t + 1.5f, 0).mat(straw ? "wood" : "cloth")
                        .box(-hw - (straw ? 2.5f : 0.5f), -1, -hd - (straw ? 2.5f : 2.5f), headW + (straw ? 5 : 1), 1, headD + (straw ? 5 : 3)).add();
                part(b, "hat", "hat_brim", Role.STATIC, dk(c, 0.95f)).at(0, -1, 0).mat(straw ? "wood" : "cloth")
                        .box(-hw + 0.5f, -2, -hd + 0.5f, headW - 1, 2, headD - 1, 0.3f).add();
                part(b, "hat_band", "hat", Role.STATIC, tc).at(0, 0, 0).mat("cloth")
                        .box(-hw + 0.5f, -0.8f, -hd + 0.5f, headW - 1, 1, headD - 1, 0.45f).add();
            }
            case "bandana" -> {
                part(b, "bandana", "head", Role.STATIC, c).at(0, t + 1, 0).mat("cloth")
                        .box(-hw - 0.4f, -1.2f, -hd - 0.4f, headW, 3, headD, 0.4f).add();
                part(b, "bandana_knot", "bandana", Role.STATIC, dk(c, 0.85f)).at(0, 1, hd + 0.3f).mat("cloth")
                        .box(-1, -1, -0.5f, 2, 2, 1).add();
                part(b, "bandana_tail", "bandana_knot", Role.STATIC, dk(c, 0.9f)).at(0, 0.5f, 0.3f).rot(25, 0, 10).mat("cloth")
                        .box(-1.5f, 0, -0.5f, 1, 4, 1).box(0.5f, 0, -0.5f, 1, 3, 1).add();
            }
            case "tricorn" -> {
                part(b, "hat", "head", Role.STATIC, c).at(0, t + 1, 0).mat("leather")
                        .box(-hw - 0.3f, -3, -hd - 0.3f, headW, 3, headD, 0.3f).add();
                part(b, "hat_brim", "hat", Role.STATIC, dk(c, 0.9f)).at(0, -0.5f, 0).mat("leather")
                        .box(-hw - 2, -1, -hd - 1.5f, headW + 4, 2, headD + 3).add();
                part(b, "hat_front", "hat_brim", Role.STATIC, c).at(0, -1, -hd - 1.5f).rot(-30, 0, 0).mat("leather")
                        .box(-hw - 1, -2, -0.5f, headW + 2, 2, 1).add();
                part(b, "hat_skull", "hat", Role.STATIC, 0xE8E4D8).at(0, -1.5f, -hd - 0.4f).mat("bone").box(-1, -1, -0.5f, 2, 2, 1).add();
            }
            case "crown" -> {
                part(b, "crown", "head", Role.STATIC, c).at(0, t, 0).mat("metal")
                        .box(-hw - 0.4f, -1.5f, -hd - 0.4f, headW, 2, headD, 0.4f).add();
                for (int i = 0; i < 4; i++) {
                    part(b, "crown_pt" + i, "crown", Role.STATIC, c).at(-hw + 1 + i * (headW - 2) / 3f, -1.5f, -hd - 0.4f).mat("metal")
                            .box(-0.5f, -1.5f, -0.4f, 1, 2, 1).add();
                }
            }
            case "chef" -> {
                part(b, "hat", "head", Role.STATIC, c).at(0, t, 0).mat("cloth")
                        .box(-hw + 0.5f, -2, -hd + 0.5f, headW - 1, 3, headD - 1, 0.4f).add();
                part(b, "hat_puff", "hat", Role.STATIC, c).at(0, -2, 0).mat("cloth")
                        .box(-hw - 0.5f, -4, -hd - 0.5f, headW + 1, 4, headD + 1).add();
            }
            case "skullcap" -> part(b, "cap", "head", Role.STATIC, c).at(0, t, 0).mat("cloth")
                    .box(-hw - 0.3f, -1, -hd - 0.3f, headW, 3, headD, 0.3f).add();
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Shield & weapon
    // ------------------------------------------------------------------------------------------

    private void shield(ModelSpec.Builder b) {
        if (shield.equals("none")) {
            return;
        }
        int c = shieldCol;
        int tc = shieldTrim >= 0 ? shieldTrim : dk(c, 1.25f);
        String sh = "shield_l";
        float x = armT / 2f + 0.6f;
        switch (shield) {
            case "kite" -> {
                part(b, sh, "forearm_l", Role.STATIC, c).at(x, 1.5f, -0.5f).rot(0, 0, -4).mat("metal")
                        .box(0, -4, -4, 1, 6, 8).add();
                part(b, "shield_mid", sh, Role.STATIC, c).at(0, 2, 0).mat("metal").box(0, 0, -3, 1, 3, 6).add();
                part(b, "shield_low", sh, Role.STATIC, c).at(0, 5, 0).mat("metal").box(0, 0, -2, 1, 2, 4).box(0, 2, -1, 1, 2, 2).add();
                part(b, "shield_rim", sh, Role.STATIC, tc).at(0.6f, 0, 0).mat("metal")
                        .box(0, -4, -4.5f, 1, 1, 9).box(0, -4, -4.5f, 1, 7, 1).box(0, -4, 3.5f, 1, 7, 1).add();
                part(b, "shield_emblem", sh, Role.STATIC, tc).at(0.7f, 0, 0).mat("metal")
                        .box(0, -2.5f, -0.5f, 1, 7, 1).box(0, -1, -2, 1, 1, 4).add();
            }
            case "round", "buckler" -> {
                int r = shield.equals("buckler") ? 2 : 3;
                part(b, sh, "forearm_l", Role.STATIC, c).at(x, 2, -0.5f).mat("wood")
                        .box(0, -r - 1, -r, 1, 2 * r + 2, 2 * r).box(0, -r, -r - 1, 1, 2 * r, 2 * r + 2).add();
                part(b, "shield_rim", sh, Role.STATIC, tc).at(0.4f, 0, 0).mat("metal")
                        .box(0, -r - 1.5f, -r, 1, 1, 2 * r).box(0, r + 0.5f, -r, 1, 1, 2 * r)
                        .box(0, -r, -r - 1.5f, 1, 2 * r, 1).box(0, -r, r + 0.5f, 1, 2 * r, 1).add();
                part(b, "shield_boss", sh, Role.STATIC, tc).at(1, 0, 0).mat("metal").box(0, -1, -1, 1, 2, 2, 0.2f).add();
            }
            default -> {
                part(b, sh, "forearm_l", Role.STATIC, c).at(x, 1.5f, -0.5f).mat("metal").box(0, -4, -3.5f, 1, 8, 7).add();
                part(b, "shield_rim", sh, Role.STATIC, tc).at(0.5f, 0, 0).mat("metal")
                        .box(0, -4.5f, -4, 1, 1, 8).box(0, 3.5f, -4, 1, 1, 8).add();
                part(b, "shield_boss", sh, Role.STATIC, tc).at(1, -0.5f, 0).mat("metal").box(0, -1, -1, 1, 2, 2, 0.2f).add();
            }
        }
    }

    private void weapon(ModelSpec.Builder b) {
        if (weapon.equals("none")) {
            return;
        }
        int c = weaponCol;
        int wood = Humanoid.WOOD;
        int grip = 0x3A2616;
        String w = "weapon";
        // grip axis runs along -Y (up) from the fist; pitch tilts it forward
        Kit.P base = part(b, w, "hand_r", Role.STATIC, grip).at(0, 1.8f, -0.3f);
        float wTilt = weaponTilt;
        float wRoll = weaponRoll;
        switch (weapon) {
            case "sword", "longsword", "scimitar", "dagger" -> {
                boolean lng = weapon.equals("longsword");
                boolean dag = weapon.equals("dagger");
                int bl = dag ? 4 : lng ? 10 : 8;
                base.rot(96 + wTilt, 0, wRoll).mat("leather").box(-0.5f, -1.5f, -0.5f, 1, 4, 1).add();
                part(b, "pommel", w, Role.STATIC, dk(c, 0.8f)).at(0, 2.5f, 0).mat("metal").box(-0.5f, 0, -0.5f, 1, 1, 1, 0.2f).add();
                part(b, "guard", w, Role.STATIC, dk(c, 0.85f)).at(0, -1.5f, 0).mat("metal")
                        .box(-2, -1, -0.5f, 4, 1, 1).box(-0.5f, -1, -1, 1, 1, 2).add();
                if (weapon.equals("scimitar")) {
                    part(b, "blade", w, Role.STATIC, c).at(0, -2.5f, 0).mat("metal").box(-0.5f, -4, -1, 1, 4, 2).add();
                    part(b, "blade_mid", "blade", Role.STATIC, c).at(0, -4, 0).rot(-14, 0, 0).mat("metal").box(-0.5f, -3, -1.2f, 1, 3, 2).add();
                    part(b, "blade_tip", "blade_mid", Role.STATIC, dk(c, 1.1f)).at(0, -3, 0).rot(-22, 0, 0).mat("metal")
                            .box(-0.5f, -3, -1, 1, 3, 2, -0.1f).add();
                } else {
                    part(b, "blade", w, Role.STATIC, c).at(0, -2.5f, 0).mat("metal").box(-1, -bl, -0.5f, 2, bl, 1).add();
                    part(b, "fuller", "blade", Role.STATIC, dk(c, 0.75f)).at(0, -1, -0.55f).mat("metal")
                            .box(-0.5f, -bl + 2, -0.1f, 1, bl - 2, 1, -0.3f).add();
                    part(b, "blade_tip", "blade", Role.STATIC, dk(c, 1.1f)).at(0, -bl, 0).mat("metal")
                            .box(-0.5f, -1.5f, -0.5f, 1, 2, 1).add();
                }
            }
            case "spear", "halberd" -> {
                base.rot(86 + wTilt, 0, wRoll).mat("wood").box(-0.5f, -12, -0.5f, 1, 20, 1).add();
                part(b, "spear_head", w, Role.STATIC, c).at(0, -12, 0).mat("metal")
                        .box(-1, -3, -0.5f, 2, 3, 1).box(-0.5f, -5, -0.5f, 1, 2, 1).add();
                part(b, "spear_collar", w, Role.STATIC, dk(c, 0.8f)).at(0, -12, 0).mat("metal").box(-0.5f, 0, -0.5f, 1, 1, 1, 0.25f).add();
                if (weapon.equals("halberd")) {
                    part(b, "axe_blade", w, Role.STATIC, c).at(0, -11, 0).mat("metal").box(0.5f, -2, -0.5f, 3, 4, 1).box(3.5f, -3, -0.5f, 1, 6, 1).add();
                } else {
                    part(b, "tassel", w, Role.STATIC, 0xA02020).at(0, -11, 0).rot(0, 0, 10).mat("cloth").box(-0.5f, 0, -0.5f, 1, 3, 1, 0.1f).add();
                }
            }
            case "staff" -> {
                base.rot(84 + wTilt, 0, wRoll).mat("wood").box(-0.5f, -13, -0.5f, 1, 22, 1).add();
                part(b, "staff_wrap", w, Role.STATIC, dk(wood, 0.6f)).at(0, -1, 0).mat("leather").box(-0.5f, -1, -0.5f, 1, 3, 1, 0.2f).add();
                part(b, "staff_head", w, Role.STATIC, dk(wood, 0.8f)).at(0, -13, 0).mat("wood")
                        .box(-1.5f, -1, -0.5f, 3, 1, 1).box(-1.5f, -3, -0.5f, 1, 2, 1).box(0.5f, -3, -0.5f, 1, 2, 1).add();
                int orb = weaponGlow >= 0 ? weaponGlow : c;
                part(b, "staff_orb", w, Role.STATIC, orb).at(0, -15, 0).mat("energy").glow("full").box(-1, -1, -1, 2, 2, 2).add();
            }
            case "battleaxe", "pickaxe", "hammer" -> {
                base.rot(104 + wTilt, 0, wRoll).mat("wood").box(-0.5f, -9, -0.5f, 1, 13, 1).add();
                if (weapon.equals("battleaxe")) {
                    part(b, "axehead", w, Role.STATIC, c).at(0, -8, 0).mat("metal")
                            .box(-0.5f, -1.5f, -0.75f, 1, 3, 2, -0.2f).box(0.5f, -2, -0.5f, 2, 4, 1).box(2.5f, -3, -0.5f, 1, 6, 1)
                            .box(-2.5f, -2, -0.5f, 2, 4, 1).box(-3.5f, -3, -0.5f, 1, 6, 1).add();
                } else if (weapon.equals("pickaxe")) {
                    part(b, "pickhead", w, Role.STATIC, c).at(0, -8.5f, 0).mat("metal")
                            .box(-3, -1, -0.5f, 6, 1, 1).box(-4.5f, -0.5f, -0.5f, 2, 1, 1).box(2.5f, -0.5f, -0.5f, 2, 1, 1).add();
                } else {
                    part(b, "hammerhead", w, Role.STATIC, c).at(0, -8.5f, 0).mat("metal").box(-2.5f, -1.5f, -1.5f, 5, 3, 3).add();
                }
            }
            case "greatclub" -> {
                base.rot(112 + wTilt, 0, wRoll).mat("wood").box(-1.5f, -4, -1.5f, 3, 8, 3).add();
                part(b, "club_wrap", w, Role.STATIC, dk(wood, 0.55f)).at(0, 1, 0).mat("leather").box(-1.5f, 0, -1.5f, 3, 2, 3, 0.3f).add();
                part(b, "club_mid", w, Role.STATIC, wood).at(0, -4, 0).rot(-5, 0, 0).mat("wood").box(-2.5f, -6, -2.5f, 5, 6, 5).add();
                part(b, "club_head", "club_mid", Role.STATIC, dk(wood, 0.92f)).at(0, -6, 0).rot(-6, 0, 3).mat("wood")
                        .box(-3.5f, -7, -3.5f, 7, 7, 7).add();
                part(b, "club_top", "club_head", Role.STATIC, dk(wood, 0.85f)).at(0, -7, 0).mat("wood").box(-2.5f, -1.5f, -2.5f, 5, 2, 5).add();
                part(b, "club_knot", "club_head", Role.STATIC, dk(wood, 0.7f)).at(0, -3.5f, 0).mat("wood")
                        .box(3, -1, -1, 1, 2, 2).box(-4, 0.5f, 0, 1, 2, 2).box(-1, -2, -4, 2, 2, 1).box(0, 1, 3, 2, 1, 1).add();
                part(b, "club_spikes", "club_head", Role.STATIC, 0xC8C0B0).at(0, -4, 0).mat("bone")
                        .box(3.2f, -3, 1.5f, 2, 1, 1).box(-5.2f, -2, -2, 2, 1, 1).box(1, -1, -5.2f, 1, 1, 2).box(-1.5f, -5, 3.2f, 1, 1, 2).add();
            }
            case "club" -> {
                base.rot(108 + wTilt, 0, wRoll).mat("wood").box(-1, -3, -1, 2, 6, 2).add();
                part(b, "club_mid", w, Role.STATIC, wood).at(0, -3, 0).rot(-6, 0, 0).mat("wood").box(-1.5f, -5, -1.5f, 3, 5, 3).add();
                part(b, "club_head", "club_mid", Role.STATIC, dk(wood, 0.95f)).at(0, -5, 0).rot(-6, 0, 0).mat("wood")
                        .box(-2, -5, -2, 4, 5, 4).add();
                part(b, "club_knot", "club_head", Role.STATIC, dk(wood, 0.75f)).at(0, -3, 0).mat("wood")
                        .box(1.5f, -1, -0.5f, 1, 1, 1).box(-2.5f, 0, 0, 1, 1, 1).box(0, -2, -2.5f, 1, 1, 1).add();
            }
            case "pitchfork" -> {
                base.rot(86 + wTilt, 0, wRoll).mat("wood").box(-0.5f, -11, -0.5f, 1, 18, 1).add();
                part(b, "fork_bar", w, Role.STATIC, c).at(0, -11, 0).mat("metal").box(-2, -1, -0.5f, 4, 1, 1).add();
                part(b, "fork_tines", w, Role.STATIC, c).at(0, -12, 0).mat("metal")
                        .box(-2, -3, -0.5f, 1, 3, 1).box(-0.5f, -3, -0.5f, 1, 3, 1).box(1, -3, -0.5f, 1, 3, 1).add();
            }
            case "mace" -> {
                base.rot(100 + wTilt, 0, wRoll).mat("wood").box(-0.5f, -7, -0.5f, 1, 10, 1).add();
                part(b, "macehead", w, Role.STATIC, c).at(0, -8, 0).mat("metal")
                        .box(-1.5f, -1.5f, -1.5f, 3, 3, 3).box(-0.5f, -2.5f, -2.5f, 1, 5, 5, -0.1f).box(-2.5f, -2.5f, -0.5f, 5, 5, 1, -0.1f).add();
            }
            case "bow" -> {
                base.rot(84 + wTilt, 0, wRoll).mat("wood").box(-0.5f, -2, -0.5f, 1, 4, 1).add();
                part(b, "bow_up", w, Role.STATIC, c).at(0, -2, 0).rot(-18, 0, 0).mat("wood").box(-0.5f, -5, -0.5f, 1, 5, 1).add();
                part(b, "bow_tip_up", "bow_up", Role.STATIC, c).at(0, -5, 0).rot(-22, 0, 0).mat("wood").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
                part(b, "bow_dn", w, Role.STATIC, c).at(0, 2, 0).rot(18, 0, 0).mat("wood").box(-0.5f, 0, -0.5f, 1, 5, 1).add();
                part(b, "bow_tip_dn", "bow_dn", Role.STATIC, c).at(0, 5, 0).rot(22, 0, 0).mat("wood").box(-0.5f, 0, -0.5f, 1, 3, 1).add();
                part(b, "bow_string", w, Role.STATIC, 0xE8E0C8).at(0, 0, 3.2f).mat("cloth").box(-0.5f, -9, -0.5f, 1, 18, 1, -0.4f).add();
            }
            default -> base.mat("wood").box(-0.5f, -6, -0.5f, 1, 9, 1).add();
        }
    }
}
