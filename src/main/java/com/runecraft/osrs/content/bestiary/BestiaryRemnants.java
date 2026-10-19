package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

import static com.runecraft.osrs.content.bestiary.BestiaryHumGwd.*;
import static com.runecraft.osrs.content.bestiary.BestiaryHumKit.*;
import static com.runecraft.osrs.content.bestiary.BestiaryLookalikes.mixRgb;
import static com.runecraft.osrs.content.bestiary.Kit.part;

import com.runecraft.osrs.content.bestiary.BestiaryHumKit.Fig;

/**
 * Round-4 models for creatures that were still on the outfit fallback or borrowing an unrelated body: the God Wars
 * followers (spiritual creatures of Bandos/Saradomin/Zamorak, orks, vampyres, Saradomin's knights and priests), the
 * Sorceress's Garden seasonal elementals, the Royal Titans' elementals, the Soul Wars avatars, Ape Atoll's monkey
 * guards, Temple Trekking's ghasts and nail beasts, plus look-alike copies (GWD bloodveld/pyrefiend, the catacomb
 * dagannoth, the giant lobster, monkey zombies, maniacal monkeys, swamp snakes).
 */
final class BestiaryRemnants {
    private BestiaryRemnants() {
    }

    static final float S_HUMAN = BestiaryHumQuest.S_HUMAN;
    static final int SKIN = BestiaryHumQuest.SKIN;
    static final int GOBLIN_GREEN = 0x6E8A3A;
    static final int ZAM_BLACK = 0x2A2226;

    static List<ModelSpec> fresh() {
        List<ModelSpec> l = new ArrayList<>();
        // Bandos: his spiritual followers are goblins in Bandosian armour, leathers and robes.
        l.add(BestiaryHumGwd.sergeant("gwd_spiritual_warrior_bandos", GOBLIN_GREEN, "melee"));
        l.add(BestiaryHumGwd.sergeant("gwd_spiritual_ranger_bandos", 0x7A9A44, "range"));
        l.add(BestiaryHumGwd.sergeant("gwd_spiritual_mage_bandos", 0x5E7A34, "mage"));
        // Saradomin: humans in white-and-blue plate, blessed hide and robes.
        l.add(BestiaryHumQuest.knight("gwd_spiritual_warrior_saradomin", S_HUMAN, SARA_WHITE, GOLD, "full", SARA_BLUE, "sword", "kite",
                SKIN, false).build());
        l.add(ranger("gwd_spiritual_ranger_saradomin", SARA_WHITE, SARA_BLUE, GOLD, 0x6A4A2A));
        l.add(BestiaryHumQuest.mage("gwd_spiritual_mage_saradomin", S_HUMAN, SARA_BLUE, SARA_WHITE, "hood", 0x6A4A2A, "none", 0xFFF0A0,
                SKIN, "human"));
        // Zamorak: humans in black-and-red armour with a horned helm, Zamorakian hide (Hazeel's mark) and robes.
        l.add(BestiaryHumQuest.knight("gwd_spiritual_warrior_zamorak", S_HUMAN, ZAM_BLACK, ZAM_RED, "horned", ZAM_RED, "sword", "kite",
                SKIN, false).build());
        l.add(ranger("gwd_spiritual_ranger_zamorak", ZAM_RED, ZAM_BLACK, 0xC8C8D0, HAIR_BLACK));
        l.add(BestiaryHumQuest.mage("gwd_spiritual_mage_zamorak", S_HUMAN, ZAM_RED, ZAM_BLACK, "hood", HAIR_BLACK, "none", 0xFF6020,
                SKIN, "human"));
        // Saradomin's encampment: knights in silver plate with the blue-and-gold tabard, priests in white and gold.
        Fig knight = BestiaryHumQuest.knight("gwd_knight_of_saradomin", S_HUMAN, 0xC4C8D0, GOLD, "med", SARA_BLUE, "sword", "kite",
                SKIN, false);
        knight.p("tabard", "chest", SARA_BLUE).at(0, -knight.chestH + 1, -knight.depth / 2f - 0.6f).mat("cloth")
                .box(-2.5f, 0, -0.4f, 5, 9, 1).add();
        knight.p("tabard_star", "tabard", GOLD).at(0, 2, -0.5f).mat("metal").box(-1, 0, -0.3f, 2, 2, 1).add();
        knight.hair(0x6A4A2A, "short");
        l.add(knight.build());
        Fig priest = BestiaryHumQuest.mageFig("gwd_saradomin_priest", S_HUMAN, SARA_WHITE, GOLD, "none", HAIR_GREY, "full", SKIN, "human");
        priest.p("stole", "chest", SARA_BLUE).at(0, -priest.chestH + 0.5f, -priest.depth / 2f - 0.6f).mat("cloth")
                .box(-3, 0, -0.3f, 1, 8, 1).box(2, 0, -0.3f, 1, 8, 1).add();
        priest.mace("hand_r", 40, 0xE8E0C8, GOLD, 3);
        priest.book("hand_l", SARA_BLUE, 0xF0E8D0);
        l.add(priest.build());
        l.add(ork());
        l.add(feralVampyre());
        // Sorceress's Garden: one lean elemental form in each season's colours. // approx: OSRS garden elementals
        l.add(seasonal("winter_elemental", 0x8AB8E0, 0xE8F8FF, "ice", "winter"));
        l.add(seasonal("spring_elemental", 0x5A9A3A, 0xE8E890, "moss", "spring"));
        l.add(seasonal("summer_elemental", 0xD86A20, 0xFFD040, "hide", "summer"));
        l.add(seasonal("autumn_elemental", 0x8A5A2A, 0xD89030, "wood", "autumn"));
        // Royal Titans: small floating spirits of ice and flame.
        l.add(spirit("titan_ice_elemental", 0x9ACCF0, 0xE8F8FF, "ice"));
        l.add(spirit("titan_fire_elemental", 0xE06A20, 0xFFD040, "ghost"));
        // Soul Wars avatars: towering horned brutes of soul-stuff, blue (Creation) and red (Destruction).
        l.add(avatar("avatar_of_creation", 0x2A4A8A, 0x60C0FF));
        l.add(avatar("avatar_of_destruction", 0x7A1E1A, 0xFF6030));
        // Ape Atoll's monkey guards are big silver-grey gorillas.
        l.add(BestiaryCrashFossil.gorilla("monkey_guard", 0x6E6C6A, 0x46423E, 0x3A3230, 0xC08020, 0xC8C0B0, false));
        l.add(ghast());
        l.add(nailBeast());
        return l;
    }

    static List<ModelSpec.Variant> variants() {
        List<ModelSpec.Variant> l = new ArrayList<>();
        // Same creatures as outside the dungeons in OSRS.
        l.add(ModelSpec.Variant.of("gwd_bloodveld", "bloodveld"));
        l.add(ModelSpec.Variant.of("gwd_pyrefiend", "pyrefiend"));
        l.add(ModelSpec.Variant.of("catacomb_dagannoth", "dagannoth"));
        // The giant lobster: a rock lobster grown huge, in boiled-lobster red.
        l.add(new ModelSpec.Variant("quest_giant_lobster", "rock_lobster", 1.5f, recolour(0xC8402A, 0.6f)));
        // Ape Atoll and Kruk's Dungeon monkeys: rotting green-grey zombies, and the dark-furred maniacal monkeys.
        l.add(new ModelSpec.Variant("monkey_zombie", "monkey", 1.0f, recolour(0x7A8A5A, 0.55f)));
        l.add(new ModelSpec.Variant("maniacal_monkey", "monkey", 1.0f, recolour(0x3A2C24, 0.5f)));
        // Mort Myre's swamp snakes: a sea snake in murky brown-green.
        l.add(new ModelSpec.Variant("swamp_snake", "sea_snake", 1.0f, recolour(0x6A6030, 0.85f)));
        return l;
    }

    static final int HAIR_BLACK = 0x1E1A1A;

    /** Recolours towards {@code target} while keeping each texel's light and shade. */
    static IntUnaryOperator recolour(int target, float amount) {
        return c -> {
            int r = c >> 16 & 0xFF;
            int g = c >> 8 & 0xFF;
            int bl = c & 0xFF;
            float lum = (r * 3 + g * 6 + bl) / 10f / 128f;
            int tr = Math.min(255, (int) ((target >> 16 & 0xFF) * lum));
            int tg = Math.min(255, (int) ((target >> 8 & 0xFF) * lum));
            int tb = Math.min(255, (int) ((target & 0xFF) * lum));
            return mixRgb(c, tr << 16 | tg << 8 | tb, amount);
        };
    }

    /** A god's ranger: dragonhide body and chaps in the god's colours, a coif, a cape, a quiver and a bow. */
    static ModelSpec ranger(String id, int hide, int trim, int metal, int hair) {
        int dk = darker(hide, 0.78f);
        Fig f = fig(id).legs(12, 4).torso(8, 7, 9, 6, 5, 5).arms(12, 3, 3).head(7, 8, 7).pose(8, 4, 10).armPitch(-14, -46)
                .skin(SKIN, "skin").scale(S_HUMAN).shadow(0.6f);
        f.foreBend = 24;
        f.core(dk, "leather", hide, "leather", hide, "leather").head("human", 0)
                .armsBuild(hide, "leather", hide, "leather", SKIN, "skin").legsBuild(hide, "leather", dk, "leather", 0x2A1E14, "leather")
                .belt(trim, metal, true).boots(trim, "leather").gauntlets(trim, "leather").helm("coif", hide, trim)
                .cape(trim, hide, 18, false);
        f.p("body_trim", "chest", trim).at(0, -f.chestH + 0.5f, -f.depth / 2f - 0.5f).mat("leather").box(-1, 0, -0.4f, 2, 6, 1).add();
        f.s("shoulder_pad", "arm", true, trim).at(0, -1.5f, 0).rot(0, 0, 15).mat("leather").box(-2, -1, -2, 4, 3, 4, 0.1f).add();
        f.p("amulet", "chest", metal).at(0, -f.chestH + 1.5f, -f.depth / 2f - 0.8f).mat("metal").box(-1, 0, -0.3f, 2, 2, 1).add();
        f.p("quiver", "chest", DARK_LEATHER).at(1.5f, -5, 2.5f).rot(0, 0, 25).mat("leather").box(-1, 0, -1, 2, 6, 2).add();
        f.p("quiver_arrows", "quiver", 0xD8D0C0).mat("feathers").box(-1, -2, -1, 2, 2, 2, -0.2f).add();
        f.bow("hand_l", WOOD, 0xE0D8C0);
        return f.build();
    }

    /** Ork: a towering, heavy-jawed green brute with tusks, in Bandos-brown plate with spiked pauldrons and a big mace. */
    static ModelSpec ork() {
        int skin = OURG;
        int dk = darker(skin, 0.8f);
        Fig f = fig("gwd_ork").legs(12, 5).torso(9, 9, 12, 8, 5, 7).arms(15, 4, 4).head(7, 7, 7).pose(14, 5, 12).armPitch(-16, -30)
                .skin(skin, "skin").scale(1.45f).shadow(0.9f);
        f.foreBend = 20;
        f.core(BANDOS_DK, "metal", BANDOS, "metal", BANDOS, "metal").muscles(skin).head("monster", 0xF0C020)
                .armsBuild(skin, "skin", skin, "skin", dk, "skin").legsBuild(BANDOS_DK, "metal", skin, "skin", 0x2A2018, "leather")
                .platebody(BANDOS, 0x8A7A5A).pauldrons(BANDOS, 0x8A7A5A, 2, 2, IVORY).tassets(BANDOS, 0x8A7A5A, 5)
                .belt(DARK_LEATHER, 0xB09A60, false).boots(0x2A2018, "leather").hump(dk, "skin", 10, 3, 5);
        f.p("ork_jaw", "head", dk).at(0, -0.5f, -f.headD / 2f + 0.5f).rot(-8, 0, 0).mat("skin").box(-3, -2, -1.5f, 6, 3, 2).add();
        f.s("ork_tusk", "ork_jaw", false, IVORY).at(2.2f, -2, -1.3f).rot(-10, 0, 10).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        f.p("ork_nose", "head", dk).at(0, -3.5f, -f.headD / 2f).rot(-20, 0, 0).mat("skin").box(-1, 0, -1.5f, 2, 2, 2).add();
        f.s("ork_ear", "head", false, skin).at(f.headW / 2f, -4, 0.5f).rot(0, -25, -20).mat("skin").box(0, -1, -0.5f, 3, 2, 1).add();
        f.helm("med", BANDOS_DK, 0x8A7A5A);
        f.mace("hand_r", 45, 0x3A2A1E, 0x6A6A6E, 4);
        return f.build();
    }

    /** Feral vampyre (GWD vampires, Temple Trekking juvinates): hunched, bald, grey-skinned, bat-eared, fanged and clawed, in rags. */
    static ModelSpec feralVampyre() {
        int skin = 0x8C8E9C;
        int rag = 0x5A4A3E;
        Fig f = fig("gwd_vampire").legs(12, 3).torso(7, 6, 9, 6, 5, 5).arms(14, 3, 3).head(6, 7, 6).pose(26, 4, 12).armPitch(-38, -38)
                .skin(skin, "skin").scale(S_HUMAN).shadow(0.6f);
        f.foreBend = 34;
        f.kneeBend = 18;
        f.core(rag, "cloth", skin, "skin", skin, "skin").muscles(lighter(skin, 0.15f)).head("monster", 0xFF2020)
                .armsBuild(skin, "skin", skin, "skin", darker(skin, 0.9f), "skin").claws(0xE0D8D0, 2)
                .legsBuild(rag, "cloth", rag, "cloth", darker(skin, 0.85f), "skin").hump(skin, "skin", 8, 3, 4);
        f.s("bat_ear", "head", false, skin).at(f.headW / 2f, -5, 0.5f).rot(0, -15, -25).mat("skin").box(0, -3, -0.5f, 2, 4, 1).add();
        f.s("fang", "head", false, IVORY).at(1, -1.2f, -f.headD / 2f - 0.1f).mat("horn").box(-0.5f, 0, -0.4f, 1, 1, 1, -0.1f).add();
        f.p("vamp_nose", "head", darker(skin, 0.9f)).at(0, -3.5f, -f.headD / 2f).mat("skin").box(-1, 0, -1, 2, 1, 1).add();
        f.p("rag_shirt", "chest", darker(rag, 1.1f)).at(0, -f.chestH + 2, -f.depth / 2f - 0.3f).rot(4, 0, 0).mat("cloth")
                .box(-3.5f, 0, -0.5f, 7, 5, 1).add();
        for (int k = 0; k < 3; k++) {
            f.p("rag_tatter" + k, "hips", darker(rag, 0.85f)).at(-3 + k * 3, 2, -2.6f).rot(-5, 0, (k - 1) * 12).mat("cloth")
                    .box(-1, 0, -0.5f, 2, 3 + k % 2, 1).add();
        }
        return f.build();
    }

    /** A Sorceress's Garden elemental: a lean, long-limbed figure of its season with a seasonal crown and mantle. */
    static ModelSpec seasonal(String id, int body, int accent, String mat, String season) {
        int dk = darker(body, 0.75f);
        Fig f = fig(id).legs(13, 3).torso(7, 6, 8, 6, 5, 5).arms(13, 3, 3).head(6, 8, 6).pose(10, 2, 10).armPitch(-20, -20)
                .skin(body, mat).scale(S_HUMAN).shadow(0.6f);
        f.foreBend = 22;
        f.core(dk, mat, body, mat, body, mat).head("monster", accent)
                .armsBuild(body, mat, body, mat, dk, mat).legsBuild(body, mat, dk, mat, dk, mat)
                .skirt(dk, mat, 6, accent, 6);
        f.p("mantle", "chest", dk).at(0, -f.chestH, 0).mat(mat).box(-4.5f, -0.5f, -3.5f, 9, 3, 7, 0.1f).add();
        f.p("heart", "chest", accent).at(0, -4, -f.depth / 2f - 0.3f).mat("energy").glow("full").box(-1, -1, -0.3f, 2, 2, 1).add();
        for (int k = 0; k < 5; k++) {
            float a = (k - 2) * 22f;
            String cm = switch (season) {
                case "winter" -> "ice";
                case "summer" -> "lava";
                default -> "membrane";
            };
            int c = k % 2 == 0 ? accent : body;
            if (season.equals("autumn")) {
                c = k % 2 == 0 ? 0xC8642A : 0xD8A030;
            }
            var p = f.p("crown" + k, "head", c).at((k - 2) * 1.4f, -f.headH, (k % 2) - 0.5f).rot(-8, 0, a).mat(cm)
                    .box(-0.5f, -3 - (k == 2 ? 2 : 0), -1, 1, 3 + (k == 2 ? 2 : 0), 2);
            if (season.equals("summer") || season.equals("winter")) {
                p.glow("full");
            }
            p.add();
        }
        if (season.equals("spring") || season.equals("autumn")) {
            int leaf = season.equals("spring") ? 0x7AC048 : 0xB85A20;
            f.s("shoulder_leaf", "chest", false, leaf).at(4.5f, -f.chestH + 0.5f, 0).rot(0, 0, -30).mat("membrane").box(0, -0.5f, -2, 3, 1, 4).add();
        }
        return f.build();
    }

    /** A floating elemental spirit: crystal or flame torso and arms over a tapering wisp, no legs. */
    static ModelSpec spirit(String id, int body, int glow, String mat) {
        int dk = darker(body, 0.75f);
        Fig f = fig(id).legs(10, 3).torso(7, 6, 9, 6, 5, 5).arms(12, 3, 3).head(6, 7, 6).pose(10, 0, 12).armPitch(-30, -30)
                .skin(body, mat).scale(1.1f).shadow(0.4f);
        f.foreBend = 26;
        f.core(dk, mat, body, mat, body, mat).head("monster", glow)
                .armsBuild(body, mat, body, mat, dk, mat);
        String prev = "hips";
        for (int i = 0; i < 4; i++) {
            int w = 6 - i * 3 / 2;
            part(f.b, "wisp" + i, prev, Role.TAIL, i % 2 == 0 ? body : glow).at(0, i == 0 ? 2 : 3, 0.5f).rot(i == 0 ? 10 : 18, 0, 0).mat(mat.equals("ghost") ? "lava" : mat)
                    .glow(i > 0 ? "full" : null).box(-w / 2f, 0, -w / 2f, w, 4, w).add();
            prev = "wisp" + i;
        }
        for (int k = 0; k < 3; k++) {
            f.p("shard" + k, "head", k == 1 ? glow : body).at((k - 1) * 2, -f.headH, 0).rot(-10, 0, (k - 1) * 25).mat(mat).glow("full")
                    .box(-0.5f, -3 - (k == 1 ? 2 : 0), -1, 1 + (k == 1 ? 1 : 0), 3 + (k == 1 ? 2 : 0), 2).add();
        }
        f.s("shoulder_shard", "chest", false, glow).at(4, -f.chestH, 0).rot(0, 0, -30).mat(mat).glow("full").box(-1, -3, -1, 2, 3, 2).add();
        f.p("core", "chest", glow).at(0, -4, -f.depth / 2f - 0.3f).mat("energy").glow("full").box(-1.5f, -1.5f, -0.3f, 3, 3, 1).add();
        f.s("hand_orb", "hand", true, glow).at(0, 3, -0.5f).mat("energy").glow("full").box(-1, -1, -1, 2, 2, 2, -0.2f).add();
        return f.build();
    }

    /** A Soul Wars avatar: a huge hunched brute of soul-stone with curling horns, a glowing core and seams of light. */
    static ModelSpec avatar(String id, int col, int glow) {
        String mat = "hide";
        int dk = darker(col, 0.7f);
        Fig f = fig(id).legs(11, 5).torso(10, 10, 14, 8, 6, 8).arms(17, 5, 6).head(7, 7, 7).pose(22, 6, 16).armPitch(-20, -24)
                .skin(col, mat).scale(1.6f).shadow(1.2f);
        f.foreBend = 24;
        f.kneeBend = 14;
        f.core(dk, mat, col, mat, col, mat).muscles(lighter(col, 0.1f)).head("monster", glow)
                .armsBuild(col, mat, darker(col, 0.9f), mat, dk, mat).legsBuild(col, mat, darker(col, 0.9f), mat, dk, mat)
                .hump(dk, mat, 12, 5, 6).horns(0x2A2224, 3.2f, -f.headH + 1.5f, 0, 70, 20, 3).backSpikes(glow, 3, 2)
                .beastFace(darker(col, 0.85f), dk, lighter(glow, 0.5f), true);
        f.p("core_glow", "chest", glow).at(0, -5, -f.depth / 2f - 0.2f).mat("energy").glow("full").box(-2, -2, -0.3f, 4, 4, 1).add();
        f.s("glow_seam", "forearm", true, glow).at(0, 2, -3.1f).mat("energy").glow("full").box(-0.5f, 0, -0.3f, 1, 6, 1, -0.2f).add();
        f.s("knuckle_rock", "fingers", true, dk).at(0, 1, -1).mat(mat).box(-2.5f, -0.5f, -1, 5, 2, 2).add();
        f.s("shoulder_rock", "chest", false, dk).at(6.5f, -8, 0).rot(0, 0, -25).mat(mat).box(-2.5f, -3, -3, 5, 4, 6).add();
        return f.build();
    }

    /** Ghast: a crouched, emaciated grey-green corpse-spirit with a gaping maw and long clawed fingers. */
    static ModelSpec ghast() {
        int skin = 0x8A9A80;
        int dk = darker(skin, 0.7f);
        Fig f = fig("trek_ghast").legs(11, 2).torso(6, 5, 8, 6, 5, 4).arms(14, 2, 2).head(5, 6, 5).pose(34, 4, 14).armPitch(-50, -50)
                .skin(skin, "ghost").scale(S_HUMAN).shadow(0.5f);
        f.foreBend = 30;
        f.kneeBend = 30;
        f.core(dk, "ghost", skin, "ghost", skin, "ghost").muscles(lighter(skin, 0.2f)).head("monster", 0xC0FF60)
                .armsBuild(skin, "ghost", skin, "ghost", dk, "ghost").claws(0xE0E8D0, 3)
                .legsBuild(skin, "ghost", skin, "ghost", dk, "ghost").hump(skin, "ghost", 6, 3, 4);
        f.p("maw", "head", 0x1A1A14).at(0, -1.5f, -f.headD / 2f - 0.1f).mat("flesh").box(-1.5f, 0, -0.3f, 3, 2, 1).add();
        f.p("ribs", "chest", lighter(skin, 0.25f)).at(0, -4, -f.depth / 2f - 0.2f).mat("bone").box(-2.5f, 0, -0.3f, 5, 1, 1)
                .box(-2.5f, 1.5f, -0.3f, 5, 1, 1).add();
        f.p("loin_rag", "hips", 0x4A4A3A).at(0, 0, -2.2f).mat("cloth").box(-2, 0, -0.5f, 4, 4, 1).add();
        return f.build();
    }

    /** Nail beast: a squat, pig-snouted, grey-pink quadruped bristling with iron nails along its back. */
    static ModelSpec nailBeast() {
        int hide = 0x8A6A62;
        int dk = darker(hide, 0.75f);
        int nail = 0x7A7A82;
        ModelSpec.Builder b = ModelSpec.builder("nail_beast").scale(1.1f).shadow(0.9f);
        part(b, "body", null, Role.BODY, hide).accent(dk).at(0, 13, 1).mat("hide").box(-5, -5, -8, 10, 9, 15, 0.2f).add();
        part(b, "head", "body", Role.HEAD, hide).accent(0xFFC020).at(0, -1, -8).rot(10, 0, 0).mat("hide").pattern("eyes")
                .box(-3.5f, -3, -5, 7, 6, 5).add();
        part(b, "snout", "head", Role.STATIC, dk).at(0, 1, -5).mat("hide").box(-2, -1.5f, -2, 4, 3, 2).add();
        part(b, "jaw", "head", Role.JAW, dk).at(0, 3, -4).mat("hide").box(-3, 0, -2, 6, 2, 3).add();
        Kit.sym(b, "tusk", "jaw", false, IVORY).at(2, 0, -2).rot(-25, 0, 0).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        part(b, "hump", "body", Role.STATIC, hide).at(0, -5, -1).mat("hide").box(-4, -2, -5, 8, 2, 10).add();
        for (int i = 0; i < 8; i++) {
            float z = -5 + (i / 2) * 3.2f;
            float x = (i % 2 == 0 ? -2 : 2);
            part(b, "nail" + i, "hump", Role.STATIC, nail).at(x, -2, z).rot(-12 + (i / 2) * 8, 0, (i % 2 == 0 ? 12 : -12)).mat("metal")
                    .box(-0.5f, -4, -0.5f, 1, 4, 1).add();
        }
        part(b, "tail", "body", Role.TAIL, dk).at(0, -3, 7).rot(-30, 0, 0).mat("hide").box(-1, -1, 0, 2, 2, 4).add();
        Creatures.legs4(b, hide, dk, 0x2A2020, 3.5f, -5, 5, 8, 4);
        return b.build();
    }
}
