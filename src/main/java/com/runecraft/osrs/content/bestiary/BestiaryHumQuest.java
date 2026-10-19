package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.BestiaryHumForms.*;
import static com.runecraft.osrs.content.bestiary.BestiaryHumKit.*;
import static com.runecraft.osrs.content.bestiary.Kit.part;

import com.runecraft.osrs.content.bestiary.BestiaryHumKit.Fig;

/**
 * Humanoid quest bosses and the Mage Arena: archetype sculpts (knight, mage, rogue, brute, ghost, demon, vampyre,
 * elemental) dressed per boss with their signature colours, headgear and weapons.
 */
final class BestiaryHumQuest {
    private BestiaryHumQuest() {
    }

    static final float S_HUMAN = 1.3333333f;
    static final float S_TALL = 1.2307693f;
    static final int SKIN = 0xC8946A;
    static final int SKIN_DARK = 0x8A5A3A;
    static final int SKIN_DESERT = 0xA87048;
    static final int UNDEAD = 0x7E8A70;

    /** Ids no other module defines (they would otherwise get a generated fallback). */
    static List<ModelSpec> fresh() {
        List<ModelSpec> l = new ArrayList<>();
        // ---- Mage Arena ----
        l.add(kolodion());
        l.add(justiciar());
        l.add(derwen());
        l.add(porazdir());
        // ---- knights and warriors ----
        l.add(knight("sir_mordred", S_HUMAN, 0x2A2A30, 0x8A1A1A, "full", 0x6A1010, "sword", "kite", SKIN, false).build());
        l.add(blackKnightTitan());
        l.add(generalKhazard());
        l.add(knight("khazard_warlord", S_HUMAN, 0x5A2A2A, 0xB08A40, "horned", 0x3A1A1A, "greataxe", "none", SKIN, false).build());
        l.add(kingTyras());
        l.add(knight("arrav", S_HUMAN, 0x5A5A50, 0xA89060, "med", 0x3A4A2A, "sword", "round", UNDEAD, true).build());
        l.add(knight("gadderanks", S_HUMAN, 0x4A4A52, 0x9A9AA8, "med", -1, "sword", "kite", SKIN, false).build());
        l.add(fireWarrior());
        l.add(knight("quest_sigmund", S_HUMAN, 0x5A4A3A, 0xC8C0B0, "none", 0x6A5A4A, "sword", "kite", SKIN, false).build());
        // ---- mages ----
        l.add(mage("melzar_the_mad", S_HUMAN, 0x4A3A8A, 0xD8C040, "wizard", HAIR_WHITE, "long", 0xFF60FF, SKIN, "human"));
        l.add(mage("solus_dellagar", S_HUMAN, 0x2A1E2A, 0x7A1A1A, "hood", 0x1A1A1A, "goatee", 0xC040FF, SKIN, "human"));
        l.add(mage("alomone", S_HUMAN, 0x3A0E14, 0x9A7A3A, "hood", 0x1A1A1A, "none", 0xFF4030, SKIN, "human"));
        l.add(zemouregal());
        l.add(iceQueen());
        l.add(headMourner());
        l.add(culinaromancer());
        l.add(possessedRoald());
        l.add(mage("quest_twilight_emissary", S_HUMAN, 0x1E2A4A, 0x8A6AD0, "hood", 0x1A1A1A, "none", 0xA080FF, SKIN_DESERT, "human"));
        l.add(ogreShaman());
        // ---- rogues, sailors, mercenaries ----
        l.add(rogue("jonny_the_beard", S_HUMAN, 0x5A3A22, 0x8A2A1A, "bandana", HAIR_BLACK_C, "full", "scimitar", SKIN));
        l.add(rogue("phoenix_weapon_guard", S_HUMAN, 0xB04A1A, 0x6A6A6A, "none", 0x6A3A1A, "goatee", "sword", SKIN));
        l.add(rogue("misthalin_murderer", S_HUMAN, 0x2A2A2E, 0x1A1A1E, "hood", 0x1A1A1A, "none", "dagger", SKIN));
        l.add(rogue("ithoi_the_navigator", S_HUMAN, 0x2A3A6A, 0xD8C080, "tricorn", 0x4A3A2A, "full", "scimitar", SKIN));
        l.add(rogue("mercenary_captain", S_HUMAN, 0xC8B08A, 0x6A4A2A, "headwrap", 0x1A1A1A, "full", "scimitar", SKIN_DESERT));
        l.add(kamil());
        l.add(fareed());
        // ---- undead, ghosts, shadows ----
        l.add(ghost("treus_dayth", S_HUMAN, 0xB8D0C8, 0xE0FFF0, "full", true));
        l.add(ghost("ulfric", S_HUMAN, 0xA8C0D0, 0xE0F4FF, "horned", true));
        l.add(ghost("dessourt", S_HUMAN, 0xB0A890, 0xFFE080, "hood", false));
        l.add(ghost("damis", S_HUMAN, 0x3A3448, 0xB060FF, "hood", false));
        l.add(ghost("shadow_of_light", S_HUMAN, 0x1E1C24, 0xFFFFFF, "none", false));
        l.add(ghost("me", S_HUMAN, 0x26222E, 0xFF3030, "none", false));
        l.add(fearReaper());
        l.add(nazastarool());
        l.add(koschei());
        l.add(boneguard());
        // ---- brutes: giants, ogres, trolls ----
        l.add(brute("glod", S_HUMAN, 0xA87A50, "skin", "giant", 0x5A3A1E, "club"));
        l.add(brute("moss_guardian", S_HUMAN, 0x5A7A3B, "moss", "giant", 0x3A5A1A, "club"));
        l.add(brute("quest_arrg", 1.4358974f, 0x7A7468, "hide", "troll", 0x4A3A2A, "club"));
        l.add(brute("quest_mother", 1.5384614f, 0x6A7068, "hide", "troll", 0x3A3A2A, "none"));
        l.add(barrelchest());
        l.add(tarn());
        l.add(lizardman());
        l.add(rfdElemental("flambeed", S_TALL, true));
        l.add(rfdElemental("karamel", S_HUMAN, false));
        l.add(golem("quest_arzinian_avatar", S_TALL, 0x8A7A68, 0xFFD040, "rock"));
        return l;
    }

    static final int HAIR_BLACK_C = 0x221A16;

    /** Re-sculpts of the quest module's humanoid boss models (same ids and scales). */
    static List<ModelSpec> overrides() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(demon("chronozon", 0x8A2020, 0x3A0A0A, 0x2A2420, 0xFF8030, 3).scale(1.7f).build());
        l.add(demonWinged("glough_black_demon", 0x2A2A2E, 0x121214, 0x8A8070, 0xFF3020, 4, 1.8f));
        l.add(demonWinged("jungle_demon", 0x2A5A2A, 0x102A10, 0x2A2420, 0xFFE040, 4, 2.0f));
        l.add(demonWinged("nezikchened", 0x5A3A6A, 0x2A1430, 0xC8B8A0, 0xFF60FF, 3, 1.8f));
        l.add(demon("agrith_naar", 0x7A2A1A, 0x2A0A0A, 0xD0C0A0, 0xFFB030, 2).scale(1.7f).build());
        l.add(judgeOfYama());
        l.add(brute("draugen", 1.6f, 0x8AA0A8, "ghost", "giant", 0x3A4A50, "none"));
        l.add(vampyre("dessous", 1.2f, 0x6A6A78, 0x2A2A34, 0x8A1A1A));
        l.add(vampyre("ranis_drakan", 1.2f, 0xC8C0C8, 0x2A1A2A, 0xA01A2A));
        l.add(vampyre("vanstrom_klause", 1.25f, 0xB8B4C0, 0x1A1A24, 0x5A1A6A));
        l.add(brute("dad", 2.0f, 0x7A7468, "hide", "troll", 0x5A4A2A, "club"));
        l.add(brute("troll_general", 1.8f, 0x6A7068, "hide", "troll", 0x4A3A2A, "general"));
        l.add(brute("ice_troll_king", 2.0f, 0x8AB0C8, "ice", "troll", 0x3A5A7A, "king"));
        l.add(brute("slash_bash", 1.7f, 0x7A8A6A, "flesh", "ogre", 0x3A3A2A, "club"));
        l.add(demon("gorak", 0x6A5A4A, 0x2A2018, 0x3A3028, 0xFFD040, 2).scale(1.5f).build());
        l.add(seren());
        l.add(treeSpirit());
        l.add(golem("balance_elemental", 1.8f, 0x6A8A6A, 0xFFFFFF, "rock"));
        l.add(golem("ancient_guardian", 1.8f, 0x8A8070, 0x40E0FF, "rock"));
        return l;
    }

    // ============================================================================================================
    // archetypes
    // ============================================================================================================

    /** A knight in full plate: platebody, layered pauldrons, tassets, greaves, helm, cape, sword or axe and shield. */
    static Fig knight(String id, float scale, int armour, int trim, String helm, int cape, String weapon, String shield, int skin, boolean undead) {
        int dk = darker(armour, 0.78f);
        Fig f = fig(id).legs(12, 4).torso(8, 7, 10, 6, 5, 5).arms(13, 3, 3).head(7, 7, 7).pose(undead ? 16 : 6, 3, 9).armPitch(-14, -38)
                .skin(skin, "skin").scale(scale).shadow(0.7f);
        f.foreBend = 18;
        f.core(dk, "metal", armour, "metal", armour, "metal").head(undead ? "skull" : "human", 0xFF3020)
                .armsBuild(armour, "metal", armour, "metal", dk, "metal").legsBuild(dk, "metal", armour, "metal", dk, "metal")
                .platebody(armour, trim).pauldrons(armour, trim, 3, 0, 0).armPlates(armour, trim).legPlates(armour, trim)
                .tassets(armour, trim, 5).belt(0x3A2A1E, trim >= 0 ? trim : GOLD, false).gauntlets(dk, "metal");
        if (helm.equals("none")) {
            f.hair(0x6A4A2A, "short").beard(0x6A4A2A, "full");
        } else {
            f.helm(helm, armour, trim);
        }
        if (cape >= 0) {
            f.cape(cape, trim, 21, undead);
        }
        switch (weapon) {
            case "greataxe" -> f.greataxe("hand_r", 35, 0x3A2A1E, 0x8A8A90);
            case "mace" -> f.mace("hand_r", 50, 0x3A2A1E, 0x8A8A90, 3);
            case "halberd" -> f.halberd("hand_r", 15, 0x3A2A1E, 0xA0A0A8, 20);
            case "spear" -> f.spear("hand_r", 20, 0x3A2A1E, 0xA0A0A8, 20);
            default -> f.sword("hand_r", 60, 0xB8B8C0, trim >= 0 ? trim : GOLD, 10, 2);
        }
        if (!shield.equals("none")) {
            f.shield("hand_l", shield, armour, trim, trim >= 0 ? trim : GOLD);
        }
        return f;
    }

    /** A robed caster: layered robe, wide sleeves, hat or hood, hair and beard, a staff with a glowing orb. */
    static ModelSpec mage(String id, float scale, int robe, int trim, String hat, int hair, String beard, int orb, int skin, String face) {
        Fig f = mageFig(id, scale, robe, trim, hat, hair, beard, skin, face);
        f.staff("hand_r", 8, 0x4A3420, darker(trim, 0.8f), orb, "full");
        f.p("hand_glow", "hand_l", orb).at(0, 3, -0.5f).mat("energy").glow("full").box(-1, -1, -1, 2, 2, 2).add();
        return f.build();
    }

    static Fig mageFig(String id, float scale, int robe, int trim, String hat, int hair, String beard, int skin, String face) {
        int dk = darker(robe, 0.8f);
        Fig f = fig(id).legs(12, 3).torso(7, 7, 8, 6, 5, 5).arms(12, 3, 3).head(7, 8, 7).pose(8, 2, 10).armPitch(-36, -24)
                .skin(skin, "skin").scale(scale).shadow(0.6f);
        f.foreBend = 34;
        f.core(dk, "cloth", robe, "cloth", robe, "cloth").head(face, 0xFF4040)
                .armsBuild(robe, "cloth", robe, "cloth", skin, "skin").legsBuild(dk, "cloth", dk, "cloth", 0x2A2018, "leather")
                .robeSleeves(robe, trim).skirt(robe, "cloth", 11, trim, 7).belt(trim, GOLD, true);
        f.p("robe_panel", "chest", trim).at(0, -6, -3.1f).mat("cloth").box(-1, 0.5f, -0.5f, 2, 5, 1).add();
        f.p("robe_panel_low", "skirt_front", trim).at(0, 0, -0.6f).mat("cloth").box(-1, -1, -0.3f, 2, 10, 1).add();
        f.p("mantle", "chest", dk).at(0, -6, 0).mat("cloth").box(-4.5f, -0.5f, -3.5f, 9, 3, 7, 0.1f).add();
        f.p("mantle_trim", "mantle", trim).at(0, 2.5f, 0).mat("cloth").box(-4.5f, -0.5f, -3.5f, 9, 1, 7, 0.2f).add();
        if (!hat.equals("none")) {
            f.helm(hat, dk, trim);
        }
        if (hat.equals("none") || hat.equals("wizard")) {
            f.hair(hair, hat.equals("none") ? "long" : "wild");
        }
        if (!beard.equals("none")) {
            f.beard(hair, beard);
        }
        return f;
    }

    /** Light-armoured fighter: tunic, leather jerkin, belt and pouch, boots, headgear and a blade. */
    static ModelSpec rogue(String id, float scale, int tunic, int leather, String hat, int hair, String beard, String weapon, int skin) {
        int dk = darker(tunic, 0.8f);
        Fig f = fig(id).legs(12, 4).torso(8, 7, 9, 6, 5, 5).arms(12, 3, 3).head(7, 8, 7).pose(8, 4, 10).armPitch(-14, -46)
                .skin(skin, "skin").scale(scale).shadow(0.6f);
        f.foreBend = 24;
        f.core(dk, "cloth", tunic, "cloth", tunic, "cloth").head("human", 0)
                .armsBuild(tunic, "cloth", skin, "skin", skin, "skin").legsBuild(0x4A3A2A, "cloth", 0x4A3A2A, "cloth", 0x2A1E14, "leather")
                .skirt(tunic, "cloth", 5, dk, 6).belt(0x2A1E14, 0xB0A070, true).boots(0x2A1E14, "leather").gauntlets(0x3A2A1A, "leather");
        f.p("jerkin", "chest", leather).at(0, -6, -2.7f).mat("leather").box(-3.5f, 0.5f, -0.5f, 7, 5, 1, 0.1f).add();
        f.p("jerkin_back", "chest", darker(leather, 0.85f)).at(0, -6, 2.7f).mat("leather").box(-3.5f, 0.5f, -0.5f, 7, 5, 1, 0.1f).add();
        f.s("shoulder_pad", "arm", true, leather).at(0, -1.5f, 0).rot(0, 0, 15).mat("leather").box(-2, -1, -2, 4, 3, 4, 0.1f).add();
        f.s("bracer", "forearm", true, darker(leather, 0.8f)).at(0, 1, 0).mat("leather").box(-1.5f, 0, -1.5f, 3, 3, 3, 0.25f).add();
        f.p("bandolier", "chest", 0x2A1E14).at(0, -6, 0).rot(0, 0, 32).mat("leather").box(-5.5f, 0, -3, 11, 1, 6, 0.2f).add();
        switch (hat) {
            case "hood" -> f.helm("hood", tunic, -1);
            case "bandana" -> {
                f.hair(hair, "short");
                f.p("bandana", "head", leather).at(0, -8, 0).mat("cloth").box(-3.5f, -0.5f, -3.5f, 7, 2, 7, 0.35f).add();
                f.p("bandana_knot", "bandana", leather).at(0, 0.5f, 3.6f).rot(20, 0, 0).mat("cloth").box(-1, 0, 0, 2, 3, 1).add();
                f.p("eyepatch", "head", 0x1A1A1A).at(1.6f, -4.5f, -3.7f).mat("leather").box(-1, -0.5f, -0.3f, 2, 2, 1).add();
            }
            case "tricorn" -> {
                f.hair(hair, "short");
                f.p("tricorn", "head", tunic).at(0, -8, 0).mat("cloth").box(-4.5f, -0.5f, -4.5f, 9, 1, 9).add();
                f.p("tricorn_crown", "tricorn", darker(tunic, 0.8f)).at(0, -0.5f, 0).mat("cloth").box(-3, -2, -3, 6, 2, 6).add();
                f.p("tricorn_trim", "tricorn", leather).at(0, -0.5f, 0).mat("cloth").box(-4.5f, -1, -4.5f, 9, 1, 1).add();
                f.p("coat_tails", "hips", tunic).at(0, 0, 2.8f).rot(10, 0, 0).mat("cloth").box(-4, -1, -0.5f, 8, 9, 1).add();
            }
            case "headwrap" -> {
                f.p("headwrap", "head", tunic).at(0, -8, 0).mat("cloth").box(-3.5f, -1, -3.5f, 7, 3, 7, 0.35f).add();
                f.p("headwrap_tail", "headwrap", darker(tunic, 0.85f)).at(0, 2, 3.6f).rot(15, 0, 0).mat("cloth").box(-2, 0, 0, 4, 5, 1).add();
                f.p("face_veil", "head", tunic).at(0, -2.5f, -3.6f).mat("cloth").box(-3, 0, -0.4f, 6, 2, 1).add();
            }
            default -> f.hair(hair, "short");
        }
        if (!beard.equals("none")) {
            f.beard(hair, beard);
        }
        switch (weapon) {
            case "dagger" -> f.sword("hand_r", 100, 0xB8B8C0, 0x3A3A3A, 5, 1);
            case "scimitar" -> {
                f.sword("hand_r", 60, 0xB8B8C0, GOLD, 9, 2);
                f.p("scim_curve", "sword_blade", 0xC8C8D0).at(1, -9, 0).rot(0, 0, -25).mat("metal").box(-1, -3, -0.5f, 2, 3, 1, -0.05f).add();
            }
            default -> f.sword("hand_r", 60, 0xB0B0B8, 0x6A6A6A, 9, 2);
        }
        return f.build();
    }

    /**
     * A spirit: robed upper body and arms, a long tattered robe that tapers into a floating wisp tail (no legs),
     * a glowing face and translucent ghost material.
     */
    static ModelSpec ghost(String id, float scale, int col, int glow, String headgear, boolean warrior) {
        int dk = darker(col, 0.75f);
        Fig f = fig(id).legs(12, 3).torso(8, 7, 9, 6, 5, 5).arms(13, 3, 3).head(7, 8, 7).pose(14, 0, 12).armPitch(-40, -40)
                .skin(col, "ghost").scale(scale).shadow(0.5f);
        f.foreBend = 30;
        f.core(dk, "ghost", col, "ghost", col, "ghost").head("monster", glow)
                .armsBuild(col, "ghost", col, "ghost", dk, "ghost").claws(lighter(col, 0.3f), 2)
                .robeSleeves(col, dk).skirt(col, "ghost", 9, dk, 8);
        String prev = "hips";
        for (int i = 0; i < 4; i++) {
            int w = 6 - i * 3 / 2;
            part(f.b, "wisp" + i, prev, Role.TAIL, i % 2 == 0 ? col : dk).at(0, i == 0 ? 5 : 3, i == 0 ? 1 : 0.5f).rot(i == 0 ? 15 : 30, 0, 0).mat("ghost")
                    .box(-w / 2f, 0, -w / 2f, w, 4, w).add();
            prev = "wisp" + i;
        }
        for (int k = 0; k < 3; k++) {
            f.p("tatter" + k, "skirt_back", dk).at(-3 + k * 3, 8, 0).rot(10, 0, (k - 1) * 10).mat("ghost").box(-1, 0, -0.5f, 2, 3, 1).add();
        }
        f.p("soul_glow", "chest", glow).at(0, -3.5f, -3.1f).mat("energy").glow("full").box(-1, -1, -0.3f, 2, 2, 1, -0.1f).add();
        if (warrior) {
            f.platebody(lighter(col, 0.15f), glow).pauldrons(lighter(col, 0.15f), glow, 2, 0, 0);
            f.sword("hand_r", 60, lighter(col, 0.4f), glow, 11, 2);
            f.p("sword_aura", "sword_blade", glow).at(0, -5, 0).mat("energy").glow("full").box(-0.5f, -4, -0.6f, 1, 8, 1, -0.3f).add();
        } else {
            f.s("hand_wisp", "hand", true, glow).at(0, 3, -0.5f).mat("energy").glow("full").box(-1, -1, -1, 2, 2, 2, -0.2f).add();
        }
        if (!headgear.equals("none")) {
            f.helm(headgear, headgear.equals("hood") ? dk : lighter(col, 0.15f), headgear.equals("hood") ? -1 : glow);
        } else {
            f.p("shadow_crown", "head", dk).at(0, -8, 0).mat("ghost").box(-3, -2, -3, 6, 2, 6).add();
            for (int k = 0; k < 3; k++) {
                f.p("shadow_flame" + k, "shadow_crown", k == 1 ? col : dk).at((k - 1) * 2, -2, 0).rot(0, 0, (k - 1) * 20).mat("ghost").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
            }
        }
        if (id.equals("damis")) {
            f.sword("hand_r", 60, 0x6A5A8A, 0x2A2238, 9, 2);
        }
        return f.build();
    }

    /**
     * A brute: giant/troll/ogre proportions (short legs, huge shoulders, hump, small head, long arms), loincloth
     * and a club. {@code kind}: "giant", "troll" (rocky, big nose, hunched), "ogre" (tusks).
     */
    static ModelSpec brute(String id, float scale, int skin, String mat, String kind, int cloth, String gear) {
        boolean troll = kind.equals("troll");
        Fig f = fig(id).legs(troll ? 10 : 11, 5).torso(10, 10, 14, 8, 6, 8).arms(troll ? 18 : 16, 5, 6).head(7, 7, 7)
                .pose(troll ? 30 : 18, 6, 14).armPitch(-18, -30).skin(skin, mat).scale(scale).shadow(1.0f);
        f.foreBend = 22;
        f.kneeBend = 16;
        f.core(cloth, "leather", skin, mat, skin, mat).muscles(skin).head("monster", troll ? 0xFFB020 : 0xE0C040)
                .armsBuild(skin, mat, skin, mat, darker(skin, 0.9f), mat).legsBuild(skin, mat, skin, mat, darker(skin, 0.85f), mat)
                .loincloth(cloth, 7).belt(darker(cloth, 0.8f), 0xC8C0A8, true).hump(skin, mat, 12, 5, 6);
        f.p("big_nose", "head", darker(skin, 0.92f)).at(0, -3.5f, -3.5f).rot(-15, 0, 0).mat(mat).box(-1, 0, -(troll ? 3 : 2), 2, troll ? 3 : 2, troll ? 3 : 2).add();
        f.p("heavy_brow", "head", darker(skin, 0.8f)).at(0, -4.5f, -3.5f).rot(15, 0, 0).mat(mat).box(-3.5f, -1, -1, 7, 2, 2).add();
        f.s("big_ear", "head", false, skin).at(3.5f, -4, 0.5f).rot(0, -20, troll ? -30 : 0).mat(mat).box(-0.5f, -1.5f, -1, troll ? 3 : 2, 3, 2).add();
        if (kind.equals("ogre") || troll) {
            f.s("tusk", "head", false, IVORY).at(2, -1, -3.5f).rot(-10, 0, 12).mat("horn").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        }
        if (troll) {
            f.backSpikes(darker(skin, 0.7f), 3, 2);
            f.s("rock_shoulder", "chest", false, darker(skin, 0.85f)).at(6, -8, 0).rot(0, 0, -25).mat(mat).box(-2.5f, -3, -3, 5, 4, 6).add();
        }
        f.s("wrist_wrap", "forearm", true, cloth).at(0, f.foreLen() - 2.5f, 0).mat("leather").box(-2.5f, 0, -2.5f, 5, 2, 5, 0.2f).add();
        switch (gear) {
            case "club" -> f.club("hand_r", 50, 0x6A4A2A);
            case "general" -> {
                f.platebody(0x5A5A5E, 0x8A6A3A).pauldrons(0x5A5A5E, 0x8A6A3A, 2, 2, IVORY).helm("horned", 0x5A5A5E, -1);
                f.mace("hand_r", 45, 0x3A2A1E, 0x6A6A6A, 4);
            }
            case "king" -> {
                f.helm("crown", 0xD8F0FF, 0x60E0FF).cape(0x3A5A7A, 0xD8F0FF, 18, true);
                f.club("hand_r", 50, 0x9AC0D8);
            }
            default -> f.claws(IVORY, 2);
        }
        return f.build();
    }

    /** A demon with membranous wings (quest variant of the GWD demon form). */
    static ModelSpec demonWinged(String id, int skin, int dark, int horn, int eye, int bulk, float scale) {
        Fig f = demon(id, skin, dark, horn, eye, bulk).scale(scale);
        f.demonWings("chest", darker(dark, 1.2f), dark, horn, 2.5f, -f.chestH + 1.5f, f.depth / 2f + 0.5f, 16, 3, null);
        return f.build();
    }

    /** A vampyre: tall, gaunt, high-collared cloak, slicked hair, pale skin, fangs and claws. */
    static ModelSpec vampyre(String id, float scale, int skin, int coat, int lining) {
        Fig f = fig(id).legs(13, 3).torso(8, 6, 9, 6, 5, 4).arms(14, 3, 3).head(6, 8, 6).pose(14, 3, 12).armPitch(-40, -40)
                .skin(skin, "skin").scale(scale).shadow(0.7f);
        f.foreBend = 40;
        f.core(coat, "cloth", coat, "cloth", coat, "cloth").head("monster", 0xFF2020)
                .armsBuild(coat, "cloth", coat, "cloth", skin, "skin").claws(lighter(skin, 0.3f), 2)
                .legsBuild(coat, "cloth", coat, "cloth", 0x0E0E12, "leather").hair(0x1A1418, "slick").cape(coat, lining, 22, true)
                .skirt(coat, "cloth", 9, lining, 6);
        f.s("collar_wing", "cape_collar", false, lining).at(4, -1, 0).rot(0, 0, 25).mat("cloth").box(0, -5, -2, 1, 5, 4).add();
        f.p("collar_back", "cape_collar", coat).at(0, -1, 2.5f).rot(-12, 0, 0).mat("cloth").box(-4, -5, -0.5f, 8, 5, 1).add();
        f.s("fang", "head", false, IVORY).at(1, -1.5f, -3.1f).mat("horn").box(-0.5f, 0, -0.4f, 1, 1, 1).add();
        f.s("pointed_ear", "head", false, skin).at(3, -4.5f, 0).rot(0, -20, -30).mat("skin").box(0, -1, -0.5f, 2, 2, 1).add();
        f.p("brooch", "cape_collar", lining).at(0, 0, -3.1f).mat("energy").glow("full").box(-1, -1, -0.3f, 2, 2, 1).add();
        return f.build();
    }

    /** A rock/crystal golem: chunky segmented body, glowing core and eyes. */
    static ModelSpec golem(String id, float scale, int col, int glow, String mat) {
        Fig f = fig(id).legs(10, 5).torso(10, 9, 13, 8, 6, 7).arms(16, 5, 6).head(6, 6, 6).pose(18, 6, 16).armPitch(-20, -20)
                .skin(col, mat).scale(scale).shadow(1.0f);
        f.foreBend = 24;
        f.kneeBend = 14;
        f.core(darker(col, 0.8f), mat, col, mat, col, mat).head("monster", glow)
                .armsBuild(col, mat, darker(col, 0.9f), mat, darker(col, 0.8f), mat).legsBuild(col, mat, darker(col, 0.9f), mat, darker(col, 0.8f), mat)
                .hump(darker(col, 0.85f), mat, 11, 4, 6);
        f.p("core_glow", "chest", glow).at(0, -5, -4.2f).mat("energy").glow("full").box(-1.5f, -1.5f, -0.3f, 3, 3, 1).add();
        for (int k = 0; k < 3; k++) {
            f.p("boulder" + k, "chest", k % 2 == 0 ? darker(col, 0.9f) : lighter(col, 0.1f)).at(-4 + k * 4, -8, k % 2 == 0 ? 1 : -1).rot(15 * k, 25 * k, 10 - k * 10)
                    .mat(mat).box(-1.5f, -2, -1.5f, 3, 2, 3).add();
        }
        f.s("shoulder_rock", "chest", false, darker(col, 0.9f)).at(6, -8, 0).rot(0, 0, -25).mat(mat).box(-2.5f, -3, -3, 5, 4, 6).add();
        f.s("knuckle_rock", "fingers", true, darker(col, 0.8f)).at(0, 1, -1).mat(mat).box(-2, -0.5f, -1, 4, 2, 2).add();
        f.s("glow_seam", "forearm", true, glow).at(0, 2, -3.1f).mat("energy").glow("full").box(-0.5f, 0, -0.3f, 1, 5, 1, -0.2f).add();
        f.s("knee_rock", "shin", true, darker(col, 0.85f)).at(0, 0, -2.5f).mat(mat).box(-2, -1.5f, -1, 4, 3, 2).add();
        f.p("chest_slab", "chest", lighter(col, 0.08f)).at(0, -7, -4.3f).rot(-5, 0, 0).mat(mat).box(-4, 0, -0.5f, 3, 4, 1).box(1, 0, -0.5f, 3, 4, 1).add();
        f.s("back_crystal", "hump", false, glow).at(3, -3, 0).rot(-20, 0, -25).mat("energy").glow("full").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        return f.build();
    }

    // ============================================================================================================
    // individual bosses
    // ============================================================================================================

    /** Kolodion: the Mage Arena's battle mage: elderly, white-bearded, in crimson-and-gold robes with a battle staff. */
    static ModelSpec kolodion() {
        Fig f = mageFig("quest_kolodion", S_HUMAN, 0x6A1A2A, 0xD8B040, "hood", HAIR_WHITE, "long", SKIN, "human");
        f.staff("hand_r", 8, 0x2A1A14, 0xD8B040, 0xFF40FF, "full");
        f.p("orb_left", "hand_l", 0x80C0FF).at(0, 3, -0.5f).mat("energy").glow("full").box(-1, -1, -1, 2, 2, 2).add();
        f.p("gold_sash", "chest", 0xD8B040).at(0, -6, -2.6f).rot(0, 0, -30).mat("cloth").box(-1, 0, -0.5f, 2, 9, 1).add();
        return f.build();
    }

    /** Justiciar Zachariah: Saradomin's avatar: a winged justiciar in white-gold plate with a great sword. */
    static ModelSpec justiciar() {
        Fig f = knight("quest_justiciar_zachariah", S_TALL, 0xE8E8EE, 0xD8B040, "full", 0x3A5AC8, "sword", "none", SKIN, false);
        f.featherWings("chest", 0xF4F4F8, 0xD8E0F0, 0xC8D0E8, 2, -5, 3, 13, 4);
        return f.build();
    }

    /** Derwen: Guthix's avatar: a bark-skinned druid-giant with mossy shoulders, a leafy crown and green-glowing eyes. */
    static ModelSpec derwen() {
        int bark = 0x5A4A30;
        Fig f = fig("quest_derwen").legs(12, 4).torso(9, 8, 11, 7, 6, 6).arms(15, 4, 4).head(7, 8, 7).pose(14, 4, 12).armPitch(-20, -36)
                .skin(bark, "wood").scale(S_TALL).shadow(0.9f);
        f.foreBend = 22;
        f.core(darker(bark, 0.85f), "wood", bark, "wood", bark, "wood").head("monster", 0x60FF40)
                .armsBuild(bark, "wood", bark, "wood", darker(bark, 0.85f), "wood").claws(0x3A2A1A, 2)
                .legsBuild(bark, "wood", bark, "wood", darker(bark, 0.8f), "wood").skirt(0x3A6A2A, "moss", 10, 0x2A4A1A, 8)
                .beard(0x3A6A2A, "long").hump(0x3A6A2A, "moss", 10, 3, 5);
        for (int k = 0; k < 5; k++) {
            f.p("leaf" + k, "head", k % 2 == 0 ? 0x4A8A2A : 0x2A6A1A).at(-3 + k * 1.5f, -8, (k % 2) * 1.5f - 1).rot(-20 + (k % 2) * 30, 0, (k - 2) * 18)
                    .mat("moss").box(-1, -3, -0.5f, 2, 3, 1).add();
        }
        f.s("moss_shoulder", "chest", false, 0x3A6A2A).at(4.5f, -7, 0).rot(0, 0, -25).mat("moss").box(-2, -2, -3, 4, 3, 6).add();
        f.s("branch", "chest", false, darker(bark, 0.9f)).at(3, -7, 2).rot(-30, 0, 30).mat("wood").box(-0.5f, -6, -0.5f, 1, 6, 1).add();
        f.staff("hand_r", 8, 0x4A3420, 0x2A5A1A, 0x60FF40, "full");
        return f.build();
    }

    /** Porazdir: Zamorak's avatar: a winged red demon in black war-plate with a flaming sword. */
    static ModelSpec porazdir() {
        Fig f = demon("quest_porazdir", 0xA02020, 0x2A0A0A, 0x2A2420, 0xFFD030, 3).scale(S_TALL);
        f.platebody(0x2A2226, 0xC03010).pauldrons(0x2A2226, 0xC03010, 2, 0, 0);
        f.demonWings("chest", 0x2A1414, 0x3A1A1A, 0x8A8070, 2.5f, -f.chestH + 1.5f, f.depth / 2f + 0.5f, 16, 2, null);
        f.sword("hand_r", 60, 0xE04010, 0x2A2226, 12, 2);
        f.p("sword_fire", "sword_blade", 0xFF9030).at(0, -6, 0).mat("lava").glow("full").box(-1, -6, -0.6f, 2, 12, 1, -0.2f).add();
        return f.build();
    }

    static ModelSpec blackKnightTitan() {
        Fig f = knight("black_knight_titan", S_TALL, 0x1E1E22, 0x5A1A1A, "greathelm", 0x2A0A0A, "sword", "none", 0x6A6A6A, false);
        f.p("titan_horn_l", "helm", 0x3A3A3A).at(4.5f, -6, 0).rot(0, 0, -40).mat("horn").box(0, -1, -1, 4, 2, 2).add();
        f.p("titan_horn_r", "helm", 0x3A3A3A).at(-4.5f, -6, 0).rot(0, 0, 40).mat("horn").box(-4, -1, -1, 4, 2, 2).add();
        f.p("titan_eyes", "helm_breaths", 0xFF3020).at(0, -1.5f, -0.4f).mat("energy").glow("full").box(-2, -0.5f, -0.3f, 4, 1, 1, -0.15f).add();
        return f.build();
    }

    /** General Khazard: the Mahjarrat general: grey-skinned, horned helm, dark plate and a great sword. */
    static ModelSpec generalKhazard() {
        Fig f = knight("general_khazard", S_TALL, 0x4A3A3A, 0xC09040, "horned", 0x5A1A1A, "sword", "none", 0x8A8A80, false);
        f.p("khazard_eyes", "head", 0xFF3020).at(0, -4.5f, -3.6f).mat("energy").glow("full").box(-2, 0, -0.3f, 1, 1, 1, -0.1f).box(1, 0, -0.3f, 1, 1, 1, -0.1f).add();
        f.p("chest_skull", "breastplate", BONE).at(0, 1, -1.2f).mat("bone").pattern("skull").box(-1.5f, 0, -0.5f, 3, 3, 1).add();
        return f.build();
    }

    static ModelSpec kingTyras() {
        Fig f = knight("king_tyras", S_HUMAN, 0x3A3A44, 0xD8B040, "none", 0x8A1A1A, "sword", "kite", SKIN, false);
        f.helm("crown", GOLD, 0xC02020);
        return f.build();
    }

    static ModelSpec fireWarrior() {
        Fig f = knight("fire_warrior_of_lesarkus", S_HUMAN, 0x5A2A1A, 0xFF8030, "full", -1, "sword", "none", 0xC86040, false);
        f.p("fire_aura", "helm", 0xFF8030).at(0, -8, 0).mat("lava").glow("full").box(-2, -3, -2, 4, 3, 4, -0.3f).add();
        f.p("sword_fire", "sword_blade", 0xFF9030).at(0, -5, 0).mat("lava").glow("full").box(-1, -5, -0.6f, 2, 10, 1, -0.2f).add();
        f.s("shoulder_flame", "pauldron", true, 0xFFA030).at(0, -1.5f, 0).mat("lava").glow("full").box(-1, -2, -1, 2, 2, 2).add();
        return f.build();
    }

    /** Zemouregal: a tall, gaunt Mahjarrat necromancer in black and violet robes with a skull staff. */
    static ModelSpec zemouregal() {
        Fig f = mageFig("quest_zemouregal", S_HUMAN, 0x2A1E36, 0x8A40C0, "none", 0x1A1418, "none", 0x8A8A9A, "monster");
        f.horns(0x5A5060, 2.2f, -7, -0.5f, 25, 30, 1);
        f.staff("hand_r", 8, 0x2A1E14, 0x8A40C0, 0xA060FF, "full");
        f.p("staff_skull", "staff_band", BONE).at(0, -4.5f, 0).mat("bone").pattern("skull").box(-1.5f, -1.5f, -1.5f, 3, 3, 3).add();
        f.s("bone_pauldron", "arm", true, BONE).at(0, -1.5f, 0).rot(0, 0, 20).mat("bone").box(-2, -1, -2, 4, 2, 4).add();
        return f.build();
    }

    /** The Ice Queen: blue-white skin, an icicle crown, flowing frost robes and an ice staff. */
    static ModelSpec iceQueen() {
        Fig f = mageFig("ice_queen", S_HUMAN, 0x8AB8E0, 0xE0F4FF, "none", 0xE8F8FF, "none", 0xB8D8F0, "human");
        f.p("ice_crown", "head", 0xE0F4FF).at(0, -8, 0).mat("ice").box(-3.5f, -1, -3.5f, 7, 1, 7, 0.2f).add();
        for (int k = 0; k < 5; k++) {
            f.p("icicle" + k, "ice_crown", 0xD0F0FF).at(-3 + k * 1.5f, -1, -3.2f).mat("ice").glow("full").box(-0.5f, -2 - (k == 2 ? 2 : k % 2), -0.5f, 1, 2 + (k == 2 ? 2 : k % 2), 1).add();
        }
        f.p("frost_train", "skirt_back", 0xC8E4F8).at(0, 8.5f, 0.5f).rot(60, 0, 0).mat("ice").box(-4, 0, -0.5f, 8, 4, 1).add();
        f.staff("hand_r", 8, 0xA8D0F0, 0xE0F4FF, 0x80E0FF, "full");
        return f.build();
    }

    /** The Head Mourner: plague-doctor beaked mask, dark green-grey mourner robes and gloves. */
    static ModelSpec headMourner() {
        Fig f = mageFig("head_mourner", S_HUMAN, 0x2E3A2E, 0x1A1A1A, "hood", 0x1A1A1A, "none", SKIN, "human");
        f.p("gas_mask", "head", 0x5A5A50).at(0, -4.5f, -3.6f).mat("leather").box(-3, -1.5f, -0.6f, 6, 4, 1).add();
        f.p("mask_beak", "gas_mask", 0x4A4A40).at(0, 1, -0.6f).rot(25, 0, 0).mat("leather").box(-1, -1, -3, 2, 2, 3).add();
        f.p("mask_lenses", "gas_mask", 0x8AA0A0).at(0, -1, -0.7f).mat("metal").box(-2.5f, 0, -0.3f, 2, 1, 1).box(0.5f, 0, -0.3f, 2, 1, 1).add();
        f.sword("hand_r", 80, 0xA0A0A8, 0x3A3A3A, 7, 1);
        return f.build();
    }

    /** The Culinaromancer: an evil chef-wizard: towering chef's hat, white jacket over dark robes, a giant ladle. */
    static ModelSpec culinaromancer() {
        Fig f = mageFig("culinaromancer", S_HUMAN, 0xE8E8E8, 0x2A2A2A, "none", 0x3A2A1A, "moustache", SKIN, "human");
        f.p("chef_hat", "head", 0xF4F4F4).at(0, -8, 0).mat("cloth").box(-3.5f, -2, -3.5f, 7, 2, 7, 0.2f).add();
        f.p("chef_hat_top", "chef_hat", 0xF8F8F8).at(0, -2, 0).mat("cloth").box(-4.5f, -4, -4.5f, 9, 4, 9).add();
        f.p("apron", "hips", 0xF0F0F0).at(0, -1, -3.2f).mat("cloth").box(-3, 0, -0.5f, 6, 9, 1).add();
        f.p("ladle", "grip_hand_r", 0x9A9AA0).at(0, 0, 0).mat("metal").box(-0.5f, -14, -0.5f, 1, 18, 1).add();
        f.p("ladle_bowl", "ladle", 0xB0B0B8).at(0, -14, 0).mat("metal").box(-2, -3, -2, 4, 3, 4).add();
        f.grip("hand_r", 10, 0, 0);
        return f.build();
    }

    /** King Roald possessed: royal robes, ermine collar, crown, and violet-glowing possessed eyes. */
    static ModelSpec possessedRoald() {
        Fig f = mageFig("possessed_king_roald", S_HUMAN, 0x6A1A3A, 0xD8B040, "none", 0x6A4A2A, "full", SKIN, "monster");
        f.helm("crown", GOLD, 0xA040FF);
        f.cape(0x8A1A2A, 0xF0F0F0, 21, false);
        f.p("ermine", "cape_collar", 0xF4F4F4).at(0, 0, 0).mat("fur").box(-5, -1.5f, -3, 10, 2, 7, 0.3f).add();
        f.sword("hand_r", 60, 0xB0B0B8, GOLD, 10, 2);
        f.p("possession", "head", 0xA040FF).at(0, -9, 0).mat("energy").glow("full").box(-2, -2, -2, 4, 1, 4, -0.2f).add();
        return f.build();
    }

    /** An ogre shaman: ogre bulk in hide and bone fetishes, a feathered skull headdress, a totem staff. */
    static ModelSpec ogreShaman() {
        int skin = 0x8A7A4A;
        Fig f = fig("ogre_shaman").legs(11, 5).torso(10, 10, 13, 8, 6, 7).arms(15, 5, 5).head(7, 7, 7).pose(16, 5, 12).armPitch(-20, -30)
                .skin(skin, "skin").scale(S_HUMAN).shadow(0.9f);
        f.foreBend = 22;
        f.core(0x5A3A1E, "leather", skin, "skin", 0x6A4A2A, "leather").head("monster", 0xC0FF60)
                .armsBuild(skin, "skin", skin, "skin", darker(skin, 0.9f), "skin").legsBuild(skin, "skin", skin, "skin", darker(skin, 0.85f), "skin")
                .skirt(0x5A3A1E, "leather", 8, 0x3A2614, 8).belt(0x3A2614, BONE, true).hump(skin, "skin", 10, 4, 5);
        f.s("tusk", "head", false, IVORY).at(2, -1, -3.5f).rot(-10, 0, 12).mat("horn").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        f.p("skull_headdress", "head", BONE).at(0, -7, -0.5f).mat("bone").pattern("skull").box(-3, -3, -3.5f, 6, 3, 6).add();
        for (int k = 0; k < 3; k++) {
            f.p("hd_feather" + k, "skull_headdress", k == 1 ? 0xC03020 : 0x3A6A2A).at((k - 1) * 2, -3, 1).rot(-25, 0, (k - 1) * 20).mat("feathers").box(-0.5f, -4, -0.5f, 1, 4, 1).add();
        }
        f.p("fetish_necklace", "chest", BONE).at(0, -7.5f, -4.1f).mat("bone").box(-3, 0, -0.5f, 6, 1, 1).box(-1, 1, -0.5f, 2, 2, 1).add();
        f.staff("hand_r", 8, 0x5A3A1E, BONE, 0xC0FF60, "full");
        return f.build();
    }

    /** Kamil: a frost-wielding desert warrior: bare-chested, icy bracers and pauldron, curved ice blade. */
    static ModelSpec kamil() {
        int skin = SKIN_DARK;
        Fig f = fig("kamil").legs(12, 4).torso(8, 7, 10, 6, 5, 5).arms(13, 3, 4).head(7, 8, 7).pose(8, 4, 12).armPitch(-20, -50)
                .skin(skin, "skin").scale(S_HUMAN).shadow(0.7f);
        f.foreBend = 22;
        f.core(0x3A5A8A, "cloth", skin, "skin", skin, "skin").muscles(skin).head("human", 0)
                .armsBuild(skin, "skin", skin, "skin", skin, "skin").legsBuild(0x2A4A7A, "cloth", 0x2A4A7A, "cloth", 0x5A7A9A, "ice")
                .skirt(0x3A5A8A, "cloth", 6, 0xC8E8FF, 6).belt(0x2A3A5A, 0xC8E8FF, false).pauldrons(0xA8D8F0, 0xE0F8FF, 2, 2, 0xE0F8FF).hair(0x1A1418, "mohawk");
        f.s("ice_bracer", "forearm", true, 0xA8D8F0).at(0, 1, 0).mat("ice").box(-2, 0, -2, 4, 3, 4, 0.1f).add();
        f.sword("hand_r", 60, 0xC8F0FF, 0x5A7A9A, 11, 2);
        f.p("blade_frost", "sword_blade", 0xE0FFFF).at(0, -6, 0).mat("ice").glow("full").box(-1, -5, -0.6f, 2, 10, 1, -0.25f).add();
        return f.build();
    }

    /** Fareed: a fire-wielding desert warrior in scorched robes with a burning scimitar and flame-wreathed hands. */
    static ModelSpec fareed() {
        Fig f = fig("fareed").legs(12, 4).torso(8, 7, 10, 6, 5, 5).arms(13, 3, 4).head(7, 8, 7).pose(8, 4, 12).armPitch(-20, -50)
                .skin(0x9A4A2A, "skin").scale(S_HUMAN).shadow(0.7f);
        f.foreBend = 22;
        f.core(0x5A1A0A, "cloth", 0x8A2A10, "cloth", 0x8A2A10, "cloth").head("monster", 0xFFC030)
                .armsBuild(0x8A2A10, "cloth", 0x9A4A2A, "skin", 0x9A4A2A, "skin").legsBuild(0x5A1A0A, "cloth", 0x5A1A0A, "cloth", 0x2A1A10, "leather")
                .skirt(0x8A2A10, "cloth", 8, 0xFF8030, 7).belt(0x2A1A10, GOLD, false);
        f.p("headwrap", "head", 0x5A1A0A).at(0, -8, 0).mat("cloth").box(-3.5f, -1, -3.5f, 7, 3, 7, 0.35f).add();
        f.p("flame_crest", "headwrap", 0xFFA030).at(0, -1, 0).mat("lava").glow("full").box(-1, -3, -1, 2, 3, 2).add();
        f.s("hand_flame", "hand", true, 0xFF8030).at(0, 2, -0.5f).mat("lava").glow("full").box(-1.5f, -1, -1.5f, 3, 3, 3, -0.2f).add();
        f.sword("hand_r", 60, 0xE06020, GOLD, 10, 2);
        f.p("blade_fire", "sword_blade", 0xFFB040).at(0, -5, 0).mat("lava").glow("full").box(-1, -5, -0.6f, 2, 10, 1, -0.25f).add();
        return f.build();
    }

    /** The Fear Reaper: a hooded, skeletal reaper in black tatters floating on a wisp, with a great scythe. */
    static ModelSpec fearReaper() {
        Fig f = fig("quest_fear_reaper").legs(12, 3).torso(8, 7, 9, 6, 5, 5).arms(13, 3, 3).head(7, 8, 7).pose(16, 0, 12).armPitch(-30, -40)
                .skin(0xD8D0C0, "bone").scale(S_HUMAN).shadow(0.5f);
        f.foreBend = 26;
        f.core(0x141218, "ghost", 0x1E1A22, "ghost", 0x1E1A22, "ghost").head("skull", 0x60FF80)
                .armsBuild(0x1E1A22, "ghost", 0x1E1A22, "ghost", 0xD8D0C0, "bone").robeSleeves(0x1E1A22, 0x2A2430)
                .skirt(0x1E1A22, "ghost", 10, 0x2A2430, 8).helm("hood", 0x1E1A22, -1);
        String prev = "hips";
        for (int i = 0; i < 3; i++) {
            int w = 6 - i * 2;
            part(f.b, "wisp" + i, prev, Role.TAIL, 0x1E1A22).at(0, i == 0 ? 5 : 3, i == 0 ? 1 : 0.5f).rot(i == 0 ? 15 : 30, 0, 0).mat("ghost").box(-w / 2f, 0, -w / 2f, w, 4, w).add();
            prev = "wisp" + i;
        }
        f.p("reaper_eyes", "hood_shadow", 0x60FF80).at(0, 2.9f, -0.6f).mat("energy").glow("full").box(-2.5f, -0.5f, -0.3f, 1, 1, 1, -0.15f).box(1.5f, -0.5f, -0.3f, 1, 1, 1, -0.15f).add();
        f.scythe("hand_r", 0, 0x2A1E14, 0xB8B8C0);
        return f.build();
    }

    /** Nazastarool: the undead giant of Rashiliyia's tomb: rotting zombie flesh, exposed ribs, rusted chains. */
    static ModelSpec nazastarool() {
        int flesh = 0x7A8A5A;
        Fig f = fig("nazastarool").legs(12, 4).torso(9, 7, 11, 7, 6, 6).arms(15, 4, 4).head(7, 7, 7).pose(24, 5, 14).armPitch(-60, -70)
                .skin(flesh, "flesh").scale(S_HUMAN).shadow(0.8f);
        f.foreBend = 14;
        f.core(0x3A3A2A, "cloth", flesh, "flesh", flesh, "flesh").head("skull", 0x80FF60)
                .armsBuild(flesh, "flesh", flesh, "flesh", darker(flesh, 0.85f), "flesh").claws(0xC8C0A0, 2)
                .legsBuild(flesh, "flesh", flesh, "flesh", darker(flesh, 0.8f), "flesh").loincloth(0x3A3A2A, 7).hump(flesh, "flesh", 9, 3, 5);
        for (int i = 0; i < 4; i++) {
            f.p("rib" + i, "chest", BONE).at(0, -6 + i * 1.5f, -3.6f).mat("bone").box(-3, 0, -0.5f, 6, 1, 1).add();
        }
        f.p("sternum", "chest", BONE).at(0, -6.5f, -3.7f).mat("bone").box(-0.5f, 0, -0.5f, 1, 6, 1).add();
        f.s("chain", "forearm", true, 0x5A5A5A).at(0, 2, 0).mat("chain").box(-2.5f, 0, -2.5f, 5, 2, 5, -0.2f).add();
        f.s("chain_end", "chain", true, 0x5A5A5A).at(1.5f, 2, 0).rot(0, 0, 10).mat("chain").box(-0.5f, 0, -0.5f, 1, 4, 1).add();
        f.p("torn_flesh", "waist", 0x5A2A2A).at(2, -3, -2.6f).mat("flesh").box(-1, 0, -0.5f, 2, 2, 1).add();
        return f.build();
    }

    /** Koschei the Deathless: a hulking bald Fremennik warrior-spirit, bare-chested with fur and a great axe. */
    static ModelSpec koschei() {
        int skin = 0xB0A8A0;
        Fig f = fig("koschei_the_deathless").legs(12, 4).torso(10, 9, 13, 8, 6, 7).arms(15, 4, 5).head(7, 7, 7).pose(10, 5, 14).armPitch(-20, -40)
                .skin(skin, "skin").scale(S_HUMAN).shadow(0.9f);
        f.foreBend = 22;
        f.core(0x4A3A2A, "fur", skin, "skin", skin, "skin").muscles(skin).head("monster", 0x80E0FF)
                .armsBuild(skin, "skin", skin, "skin", skin, "skin").legsBuild(0x4A3A2A, "cloth", 0x4A3A2A, "cloth", 0x5A4A3A, "fur")
                .skirt(0x5A4A3A, "fur", 7, 0x3A2A1A, 6).belt(0x3A2A1A, 0xA0A0A8, false).beard(0x8A8070, "long").boots(0x5A4A3A, "fur");
        f.p("fur_mantle", "chest", 0x6A5A48).at(0, -8, 0).mat("fur").box(-6, -1, -4, 12, 3, 8, 0.3f).add();
        f.s("arm_ring", "arm", true, 0xA0A0A8).at(0, 2.5f, 0).mat("metal").box(-2.5f, 0, -2.5f, 5, 1, 5, -0.2f).add();
        f.p("rune_tattoo", "pecs", 0x80E0FF).at(0, 1, -1.1f).mat("energy").glow("full").box(-3, 0, -0.3f, 1, 2, 1).box(2, 0, -0.3f, 1, 2, 1).add();
        f.greataxe("hand_r", 35, 0x4A3420, 0x9A9AA0);
        return f.build();
    }

    /** The Boneguard: a giant guardian built of bones: skull, ribcage, bone plates, a bone shield and staff. */
    static ModelSpec boneguard() {
        Fig f = fig("quest_boneguard").legs(12, 4).torso(9, 6, 12, 7, 6, 6).arms(15, 4, 4).head(7, 7, 7).pose(10, 5, 14).armPitch(-20, -40)
                .skin(BONE, "bone").scale(1.0f).shadow(0.9f);
        f.foreBend = 20;
        f.core(0xC8C0A8, "bone", BONE, "bone", BONE, "bone").head("skull", 0xFFD040)
                .armsBuild(BONE, "bone", BONE, "bone", 0xC8C0A8, "bone").legsBuild(BONE, "bone", BONE, "bone", 0xC8C0A8, "bone")
                .hump(0xC8C0A8, "bone", 10, 4, 5).pauldrons(0xD8D0B8, -1, 3, 2, IVORY).tassets(0xD0C8B0, -1, 6);
        for (int i = 0; i < 4; i++) {
            f.p("rib" + i, "chest", i % 2 == 0 ? BONE : 0xD0C8B0).at(0, -6.5f + i * 1.6f, -3.7f).mat("bone").box(-4, 0, -0.5f, 8, 1, 1).add();
        }
        f.p("sternum", "chest", 0xD0C8B0).at(0, -7, -3.8f).mat("bone").box(-0.5f, 0, -0.5f, 1, 7, 1).add();
        f.horns(0xD0C8B0, 2.5f, -6, 0, 40, 30, 1);
        f.shield("hand_l", "tower", 0xD8D0B8, 0xB0A890, 0xFFD040);
        f.staff("hand_r", 8, 0xC8C0A8, 0xB0A890, 0xFFD040, "full");
        return f.build();
    }

    /** Barrelchest: a hulking undead pirate-brute with a barrel-shaped plated chest, wielding a ship's anchor. */
    static ModelSpec barrelchest() {
        int skin = 0x6A7A6A;
        Fig f = fig("quest_barrelchest").legs(10, 5).torso(11, 12, 16, 10, 6, 10).arms(16, 5, 6).head(6, 6, 6).pose(20, 6, 16).armPitch(-30, -40)
                .skin(skin, "flesh").scale(S_TALL).shadow(1.1f);
        f.foreBend = 24;
        f.kneeBend = 14;
        f.core(0x3A2A1E, "leather", skin, "flesh", 0x6A4A2A, "wood").head("monster", 0x80FF60)
                .armsBuild(skin, "flesh", skin, "flesh", darker(skin, 0.9f), "flesh").legsBuild(0x3A3A4A, "cloth", skin, "flesh", 0x2A2018, "leather")
                .loincloth(0x3A3A4A, 6).hump(skin, "flesh", 12, 4, 6);
        for (int i = 0; i < 3; i++) {
            f.p("barrel_hoop" + i, "chest", 0x5A5A5E).at(0, -9 + i * 3.5f, 0).mat("metal").box(-8.5f, 0, -5.5f, 17, 1, 11, 0.1f).add();
        }
        f.s("tusk", "head", false, IVORY).at(1.8f, -1, -3).rot(-10, 0, 12).mat("horn").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        f.p("anchor_shaft", "grip_hand_r", 0x4A4A4E).at(0, 0, 0).mat("metal").box(-1, -14, -1, 2, 18, 2).add();
        f.p("anchor_ring", "anchor_shaft", 0x5A5A5E).at(0, -14, 0).mat("metal").box(-1.5f, -2, -0.5f, 3, 2, 1).add();
        f.p("anchor_arms", "anchor_shaft", 0x4A4A4E).at(0, 4, 0).mat("metal").box(-6, 0, -1, 12, 2, 2).add();
        f.p("anchor_fluke_l", "anchor_arms", 0x5A5A5E).at(6, 0, 0).rot(0, 0, 30).mat("metal").box(-1, -4, -1, 2, 4, 2).add();
        f.p("anchor_fluke_r", "anchor_arms", 0x5A5A5E).at(-6, 0, 0).rot(0, 0, -30).mat("metal").box(-1, -4, -1, 2, 4, 2).add();
        f.grip("hand_r", 30, 0, 0);
        return f.build();
    }

    /** Mutant Tarn: Tarn Razorlor mutated: a lumpy, swollen brute with glowing growths and a cracked crown of bone. */
    static ModelSpec tarn() {
        int flesh = 0x8A6A7A;
        Fig f = fig("quest_tarn_razorlor").legs(11, 5).torso(11, 10, 15, 9, 6, 8).arms(17, 5, 6).head(7, 7, 7).pose(24, 6, 16).armPitch(-24, -24)
                .skin(flesh, "flesh").scale(S_HUMAN).shadow(1.0f);
        f.foreBend = 26;
        f.kneeBend = 14;
        f.core(0x3A2A3A, "leather", flesh, "flesh", flesh, "flesh").muscles(flesh).head("monster", 0x80FF40)
                .armsBuild(flesh, "flesh", flesh, "flesh", darker(flesh, 0.85f), "flesh").claws(0xC8B8A0, 3)
                .legsBuild(flesh, "flesh", flesh, "flesh", darker(flesh, 0.8f), "flesh").loincloth(0x3A2A3A, 7).hump(flesh, "flesh", 12, 5, 6)
                .beastFace(darker(flesh, 0.9f), darker(flesh, 0.7f), IVORY, true);
        for (int k = 0; k < 5; k++) {
            f.p("growth" + k, k < 2 ? "hump" : "chest", 0x80FF40).at(-4 + k * 2, k < 2 ? -4 : -8 + k, k < 2 ? 0 : -4.2f).mat("energy").glow("full")
                    .box(-1, -1, -1, 2, 2, 2, -0.1f * (k % 2)).add();
        }
        f.horns(0xC8B8A0, 2.5f, -6, 0, 20, 15, 1);
        return f.build();
    }

    /** The corrupt lizardman: scaled, long-snouted, tailed, hunched, with a bone spear. */
    static ModelSpec lizardman() {
        int scale = 0x4A6A3A;
        Fig f = fig("corrupt_lizardman").legs(12, 4).torso(8, 7, 10, 6, 5, 5).arms(13, 3, 3).head(5, 5, 6).pose(22, 4, 12).armPitch(-30, -40)
                .skin(scale, "scales").scale(S_TALL).shadow(0.7f);
        f.foreBend = 22;
        f.core(0x3A2A1E, "leather", scale, "scales", scale, "scales").head("monster", 0xFF4030)
                .armsBuild(scale, "scales", scale, "scales", darker(scale, 0.85f), "scales").claws(IVORY, 2)
                .beastLegs(scale, "scales", darker(scale, 0.7f), "scales", true).loincloth(0x3A2A1E, 6).tail(scale, IVORY, 5, "spike")
                .backSpikes(0x8A3A2A, 3, 1);
        f.p("lizard_snout", "head", scale).at(0, -2, -3).mat("scales").box(-2, -1.5f, -4, 4, 2, 4).add();
        part(f.b, "lizard_jaw", "head", Role.JAW, darker(scale, 0.8f)).at(0, -0.5f, -3).mat("scales").glow("maw").box(-1.5f, 0, -4, 3, 1, 4).add();
        f.s("lizard_frill", "head", false, 0x8A3A2A).at(2.5f, -3, 0).rot(0, -20, -30).mat("membrane").box(0, -2, -1, 2, 3, 3).add();
        f.p("corrupt_mark", "chest", 0xA040FF).at(0, -4, -3.1f).mat("energy").glow("full").box(-1, -1, -0.3f, 2, 2, 1).add();
        f.spear("hand_r", 30, 0xC8C0A8, 0xD8D0B8, 18);
        return f.build();
    }

    /** Recipe for Disaster elementals: Flambeed (a fire elemental chef) and Karamel (a frosty toffee sorceress). */
    static ModelSpec rfdElemental(String id, float scale, boolean fire) {
        int body = fire ? 0xE06020 : 0x9A6A3A;
        Fig f = fig(id).legs(12, 4).torso(9, 8, 11, 7, 6, 6).arms(14, 4, 4).head(7, 8, 7).pose(12, 4, 14).armPitch(-30, -30)
                .skin(body, fire ? "lava" : "skin").scale(scale).shadow(0.8f);
        f.foreBend = 26;
        if (fire) {
            f.core(0x3A1A12, "rock", body, "lava", body, "lava").head("monster", 0xFFE060)
                    .armsBuild(body, "lava", body, "lava", 0x3A1A12, "rock").legsBuild(0x3A1A12, "rock", body, "lava", 0x3A1A12, "rock")
                    .hump(0x3A1A12, "rock", 10, 3, 5);
            f.p("chef_hat", "head", 0xF4F4F4).at(0, -8, 0).mat("cloth").box(-3.5f, -2, -3.5f, 7, 2, 7, 0.2f).add();
            f.p("chef_hat_top", "chef_hat", 0xF8F8F8).at(0, -2, 0).mat("cloth").box(-4, -3, -4, 8, 3, 8).add();
            for (int k = 0; k < 4; k++) {
                f.p("flame" + k, "chest", k % 2 == 0 ? 0xFFB030 : 0xFF6020).at(-3 + k * 2, -9, 2).rot(-20, 0, (k - 1.5f) * 15).mat("lava").glow("full")
                        .box(-1, -3, -1, 2, 3, 2).add();
            }
            f.s("hand_fire", "hand", true, 0xFFC040).at(0, 2, -0.5f).mat("lava").glow("full").box(-1.5f, -1, -1.5f, 3, 3, 3, -0.2f).add();
            f.p("apron", "hips", 0xF0F0F0).at(0, -1, -3.3f).mat("cloth").box(-3, 0, -0.5f, 6, 8, 1).add();
            f.p("apron_strap", "chest", 0xF0F0F0).at(0, -7, -3.6f).mat("cloth").box(-2.5f, 0, -0.5f, 5, 6, 1).add();
            f.s("shoulder_flame", "chest", false, 0xFF9030).at(4.5f, -7, 0).rot(0, 0, -20).mat("lava").glow("full").box(-1, -3, -1, 2, 3, 2).add();
            f.sword("hand_r", 80, 0xB0B0B8, 0x3A2A1A, 5, 3);
        } else {
            f.core(0x6A4A2A, "cloth", 0x8AB8E0, "cloth", 0x8AB8E0, "cloth").head("human", 0)
                    .armsBuild(0x8AB8E0, "cloth", 0x8AB8E0, "cloth", body, "skin").legsBuild(0x6A4A2A, "cloth", 0x6A4A2A, "cloth", 0x3A2A1A, "leather")
                    .robeSleeves(0x8AB8E0, 0xE0F4FF).skirt(0x6A9AC8, "cloth", 11, 0xE0F4FF, 7).hair(0x6A3A1A, "long");
            f.p("toffee_drip", "head", 0xB07A3A).at(0, -8, 0).mat("slime").box(-3.5f, -1, -3.5f, 7, 2, 7, 0.4f).add();
            f.staff("hand_r", 8, 0xB07A3A, 0xE0F4FF, 0x80E0FF, "full");
            f.p("frost_orb", "hand_l", 0xC8F0FF).at(0, 3, -0.5f).mat("ice").glow("full").box(-1, -1, -1, 2, 2, 2).add();
        }
        return f.build();
    }

    /** The Judge of Yama: a towering robed demon-judge with a horned mask and a great scythe-staff. */
    static ModelSpec judgeOfYama() {
        Fig f = demon("judge_of_yama", 0x5A1A2A, 0x1A0A10, 0xD8C8A0, 0xFF6020, 3).scale(1.9f);
        f.skirt(0x2A1A2A, "cloth", 10, 0xB08A40, 8);
        f.cape(0x2A1A2A, 0xB08A40, 20, true);
        f.scythe("hand_r", 0, 0x2A1A14, 0xC8C0B0);
        return f.build();
    }

    /** The Fragment of Seren: a crystalline elf-woman: faceted crystal body, flowing crystal hair and glowing core. */
    static ModelSpec seren() {
        int crystal = 0xA8D8F0;
        Fig f = fig("fragment_of_seren").legs(13, 3).torso(7, 6, 8, 6, 5, 4).arms(12, 3, 3).head(6, 7, 6).pose(4, 2, 16).armPitch(-50, -50)
                .skin(crystal, "ice").scale(1.3f).shadow(0.7f);
        f.foreBend = 30;
        f.core(0x7AA8D0, "ice", crystal, "ice", crystal, "ice").head("monster", 0xFFFFFF)
                .armsBuild(crystal, "ice", crystal, "ice", lighter(crystal, 0.2f), "ice").legsBuild(0x7AA8D0, "ice", crystal, "ice", 0x7AA8D0, "ice")
                .skirt(0x8AC0E8, "ice", 11, 0xE0F8FF, 9).hair(0xD0F0FF, "long");
        f.p("seren_core", "chest", 0xFFFFFF).at(0, -4, -2.8f).mat("energy").glow("full").box(-1, -1, -0.3f, 2, 2, 1).add();
        for (int k = 0; k < 4; k++) {
            f.p("crystal_shard" + k, "chest", k % 2 == 0 ? 0xD0F0FF : 0x90C8F0).at(-3 + k * 2, -6, 2.5f).rot(-30, 0, (k - 1.5f) * 25).mat("ice").glow(k % 2 == 0 ? "full" : null)
                    .box(-0.5f, -5, -0.5f, 1, 5, 1).add();
        }
        f.s("hand_light", "hand", true, 0xFFFFFF).at(0, 3, -0.5f).mat("energy").glow("full").box(-1, -1, -1, 2, 2, 2, -0.2f).add();
        return f.build();
    }

    /** The tree spirit: a towering gnarled treant of Entrana: bark body, root feet, branch arms and leafy crown. */
    static ModelSpec treeSpirit() {
        int bark = 0x5A4228;
        Fig f = fig("tree_spirit").legs(12, 5).torso(10, 9, 12, 8, 6, 7).arms(16, 4, 5).head(7, 8, 7).pose(10, 6, 18).armPitch(-30, -30)
                .skin(bark, "wood").scale(1.6f).shadow(1.0f);
        f.foreBend = 26;
        f.core(darker(bark, 0.85f), "wood", bark, "wood", bark, "wood").head("monster", 0xC0FF60)
                .armsBuild(bark, "wood", bark, "wood", darker(bark, 0.85f), "wood").claws(0x3A2A1A, 3)
                .legsBuild(bark, "wood", bark, "wood", darker(bark, 0.8f), "wood").hump(bark, "wood", 11, 4, 6);
        for (int k = 0; k < 6; k++) {
            f.p("canopy" + k, "head", k % 2 == 0 ? 0x3A7A2A : 0x2A5A1A).at(-4 + (k % 3) * 4, -8 - (k / 3) * 2.5f, (k / 3) * 2 - 1).rot(0, k * 20, (k % 3 - 1) * 15)
                    .mat("moss").box(-2, -3, -2, 4, 3, 4).add();
        }
        for (int k = 0; k < 3; k++) {
            f.s("root" + k, "foot", true, darker(bark, 0.8f)).at(-1 + k, 1, -2.5f).rot(-10, (k - 1) * -30, 0).mat("wood").box(-0.5f, -0.5f, -3, 1, 1, 3).add();
        }
        f.s("branch", "chest", false, darker(bark, 0.9f)).at(4, -8, 1).rot(-20, 0, 40).mat("wood").box(-0.5f, -6, -0.5f, 1, 6, 1).add();
        f.s("branch_leaf", "branch", true, 0x3A7A2A).at(0, -6, 0).mat("moss").box(-1.5f, -2, -1.5f, 3, 2, 3).add();
        f.p("bark_mouth", "head", 0x1A120A).at(0, -2, -3.6f).mat("wood").glow("maw").box(-2, -1, -0.3f, 4, 2, 1).add();
        return f.build();
    }
}
