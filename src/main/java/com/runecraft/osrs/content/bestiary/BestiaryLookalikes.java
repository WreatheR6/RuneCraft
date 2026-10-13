package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

import static com.runecraft.osrs.content.bestiary.BestiaryHumKit.*;
import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/**
 * Creatures that used to fall back to a generic outfit biped: dedicated OSRS-shaped sculpts (Tormented demons,
 * goraks, werewolves, minotaurs, nagua, animated armour, kalphites, aviansies) and look-alike copies of existing
 * models for creatures that look the same in OSRS (Nightmare Zone bosses, Champions' Challenge champions, the
 * God Wars ogres/cyclopes/hobgoblins, quest gryphons and the bull).
 */
final class BestiaryLookalikes {
    private BestiaryLookalikes() {
    }

    /** Models for creature ids that had none of their own (they used the outfit fallback). */
    static List<ModelSpec> fresh() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(tormentedDemon());
        l.add(gorak());
        l.add(werewolf());
        l.add(minotaur());
        l.add(nagua("sulphur_nagua", 0xC8902C, 0x7A4A18, 0xFFB030)); // approx: orange-yellow sulphur hide
        l.add(nagua("frost_nagua", 0x6E9CC4, 0x2E4A6A, 0xB8F0FF)); // approx: ice-blue hide
        String[][] tiers = {{"bronze", "8C5A2C"}, {"iron", "5E5A58"}, {"steel", "9A9AA2"}, {"black", "2C2C30"},
                {"mithril", "4C4C82"}, {"adamant", "4A6E4A"}, {"rune", "4EA0B4"}};
        for (String[] t : tiers) {
            l.add(animatedArmour("animated_" + t[0] + "_armour", Integer.parseInt(t[1], 16)));
        }
        // Armadyl's spiritual followers are aviansies.
        l.add(aviansie("gwd_spiritual_warrior_armadyl", 0x5A6680, 0xC8D0E0, "melee", 0.95f));
        l.add(aviansie("gwd_spiritual_ranger_armadyl", 0x6A5A40, 0xD8C8A0, "range", 0.95f));
        l.add(aviansie("gwd_spiritual_mage_armadyl", 0x4E4A6E, 0xC0B8E0, "mage", 0.95f));
        return l;
    }

    /** Re-sculpts of models other modules define with a cruder kit. */
    static List<ModelSpec> overrides() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(kalphite());
        l.add(aviansie("gwd_aviansie", 0x6E5E4C, 0xCDBB98, "range", 0.9f));
        l.add(aviansie("gwd_aviansie_elite", 0x8A8C9C, 0xE4E4EC, "melee", 1.0f));
        return l;
    }

    /** Creatures that look exactly like another one in OSRS. */
    static List<ModelSpec.Variant> variants() {
        List<ModelSpec.Variant> l = new ArrayList<>();
        // Champions' Challenge: the champion is a slightly larger copy of its kind.
        String[][] champs = {{"imp_champion", "imp"}, {"goblin_champion", "goblin"}, {"skeleton_champion", "skeleton"},
                {"zombies_champion", "zombie"}, {"giant_champion", "hill_giant"}, {"hobgoblin_champion", "hobgoblin"},
                {"ghoul_champion", "ghoul"}, {"earth_warrior_champion", "earth_warrior"}, {"jogre_champion", "jogre"},
                {"lesser_demon_champion", "lesser_demon"}};
        for (String[] c : champs) {
            l.add(ModelSpec.Variant.of(c[0], c[1], 1.12f));
        }
        // Nightmare Zone: dream copies of the quest bosses.
        String[][] nmz = {{"nmz_count_draynor", "count_draynor"}, {"nmz_king_roald", "possessed_king_roald"},
                {"nmz_the_kendal", "the_kendal"}, {"nmz_me", "me"}, {"nmz_elvarg", "elvarg"}, {"nmz_tree_spirit", "tree_spirit"},
                {"nmz_dad", "dad"}, {"nmz_khazard_warlord", "khazard_warlord"}, {"nmz_arrg", "quest_arrg"},
                {"nmz_black_knight_titan", "black_knight_titan"}, {"nmz_bouncer", "bouncer"}, {"nmz_glod", "glod"}};
        for (String[] n : nmz) {
            l.add(ModelSpec.Variant.of(n[0], n[1]));
        }
        // God Wars Dungeon: Bandos's ogres, jogres, cyclopes and hobgoblins are the ordinary kinds.
        l.add(ModelSpec.Variant.of("gwd_ogre", "ogre"));
        l.add(ModelSpec.Variant.of("gwd_jogre", "jogre"));
        l.add(ModelSpec.Variant.of("gwd_cyclops", "cyclops"));
        l.add(ModelSpec.Variant.of("gwd_hobgoblin", "hobgoblin"));
        // Quest gryphons share the Shellbane gryphon's body.
        l.add(ModelSpec.Variant.of("quest_shellbane_gryphon", "shellbane_gryphon"));
        l.add(ModelSpec.Variant.of("quest_gryphon", "shellbane_gryphon", 0.9f));
        // The bull: the cow's body in a dark brown coat.
        l.add(new ModelSpec.Variant("quest_bull", "cow", 1.1f, bullCoat()));
        // Crystal impling: an impling in pale crystal blue.
        l.add(new ModelSpec.Variant("crystal_impling", "essence_impling", 1.0f, c -> mixRgb(c, 0x9CE8F4, 0.65f)));
        return l;
    }

    // ----------------------------------------------------------------------------------------------
    // dedicated sculpts
    // ----------------------------------------------------------------------------------------------

    /**
     * Tormented demon: a huge hunched crimson demon with black horns, ragged bat wings, a burning maw and broken
     * shackles with chain ends hanging from its wrists.
     */
    static ModelSpec tormentedDemon() {
        int skin = 0x8E2C1A;
        int dark = 0x3A0E08;
        int horn = 0x241E1C;
        Fig f = BestiaryHumForms.demon("tormented_demon", skin, dark, horn, 0xFF9020, 5);
        f.demonWings("chest", 0x3A1610, 0x5A2016, horn, 2.5f, -f.chestH + 1.5f, f.depth / 2f + 0.5f, 18, 3, null);
        f.s("shackle", "forearm", true, 0x4A4A50).at(0, f.foreLen() - 2.5f, 0).mat("metal")
                .box(-f.foreW / 2f - 0.5f, 0, -f.foreW / 2f - 0.5f, f.foreW + 1, 2, f.foreW + 1, 0.15f).add();
        f.s("shackle_chain", "shackle", true, 0x6A6A70).at(f.foreW / 2f, 2, 0).rot(15, 0, -10).mat("chain")
                .box(-0.5f, 0, -0.5f, 1, 5, 1).add();
        return f.scale(1.15f).build();
    }

    /** Gorak: a hulking, hunched lavender-skinned brute with a small fanged head, long arms and clawed hands. */
    static ModelSpec gorak() {
        int skin = 0x7468B0;
        int dk = 0x3E3670;
        Fig f = fig("gwd_gorak").legs(9, 5).torso(10, 10, 15, 8, 5, 8).arms(18, 5, 5).head(7, 8, 7).pose(22, 5, 14)
                .armPitch(-22, -22).skin(skin, "hide").shadow(1.0f);
        f.foreBend = 24;
        f.core(dk, "hide", skin, "hide", skin, "hide").muscles(BestiaryLookalikes.mixRgb(skin, 0xE0D8F0, 0.25f)).head("monster", 0xFF3020)
                .beastFace(darker(skin, 0.9f), dk, IVORY, false)
                .armsBuild(skin, "hide", darker(skin, 0.88f), "hide", dk, "hide").claws(0x2A2230, 2)
                .legsBuild(skin, "hide", darker(skin, 0.9f), "hide", dk, "hide").hump(skin, "hide", 12, 4, 6);
        f.p("gorak_crest", "head", dk).at(0, -f.headH, 0.5f).rot(-20, 0, 0).mat("hide").box(-1, -2, -2.5f, 2, 2, 5).add();
        f.s("gorak_ear", "head", false, skin).at(f.headW / 2f, -3.5f, 0.5f).rot(0, -20, -35).mat("hide").box(0, -1, -0.5f, 3, 2, 1).add();
        f.loincloth(0x4A3424, 4);
        return f.build();
    }

    /** Werewolf (wolf form): grey-brown fur, a long wolf muzzle and pricked ears, clawed hands, digitigrade legs, torn breeches. */
    static ModelSpec werewolf() {
        int fur = 0x6E6050;
        int dk = 0x4A3E32;
        int light = 0xA89A86;
        Fig f = fig("gwd_werewolf").legs(12, 4).torso(8, 7, 11, 7, 5, 6).arms(15, 3, 3).head(6, 6, 6).pose(26, 3, 12)
                .armPitch(-22, -22).skin(fur, "fur").shadow(0.8f);
        f.foreBend = 22;
        f.core(0x4A3A2A, "cloth", fur, "fur", fur, "fur").head("monster", 0xFFD040)
                .beastFace(dk, dk, IVORY, false)
                .armsBuild(fur, "fur", fur, "fur", dk, "fur").claws(0x2A2420, 2)
                .beastLegs(fur, "fur", dk, "fur", true)
                .tail(fur, fur, 3, "none");
        f.p("muzzle", "snout", fur).at(0, 0, -2).rot(4, 0, 0).mat("fur").box(-1.5f, -0.5f, -3, 3, 2, 3).add();
        f.p("wolf_nose", "muzzle", 0x1A1614).at(0, -0.5f, -3).mat("skin").box(-1, 0, -0.5f, 2, 1, 1).add();
        f.s("wolf_ear", "head", false, fur).at(2, -f.headH, 0.5f).rot(-10, 0, -12).mat("fur").box(-1, -3, -0.5f, 2, 3, 1).add();
        f.p("ruff", "chest", light).at(0, -f.chestH + 1, -f.depth / 2f - 0.5f).mat("fur").box(-3, 0, -1, 6, 4, 1).add();
        f.p("mane", "chest", dk).at(0, -f.chestH, f.depth / 2f).rot(-25, 0, 0).mat("fur").box(-3, -2, -1, 6, 3, 3).add();
        f.skirt(0x4A3A2A, "cloth", 4, -1, 6);
        return f.build();
    }

    /** Minotaur: a brown-furred bull-headed brute with wide pale horns, a nose ring, hooves, a loincloth and a tufted tail. */
    static ModelSpec minotaur() {
        int fur = 0x6A4228;
        int dk = 0x3E2616;
        int muzzle = 0x9A6E4E;
        Fig f = fig("minotaur").legs(11, 4).torso(9, 8, 13, 7, 5, 7).arms(14, 4, 4).head(7, 7, 7).pose(16, 3, 12)
                .armPitch(-12, -16).skin(fur, "fur").shadow(0.8f);
        f.core(0x5A3A1E, "leather", fur, "fur", fur, "fur").muscles(fur).head("eyes", 0xE02020)
                .beastFace(muzzle, dk, IVORY, false)
                .horns(0xE8DCC0, 3.2f, -f.headH + 1.5f, 0, 70, 12, 2)
                .armsBuild(fur, "fur", fur, "fur", dk, "fur")
                .beastLegs(fur, "fur", 0x2A2018, "horn", false)
                .loincloth(0x5A3A1E, 5).tail(fur, dk, 3, "none");
        f.p("nose_ring", "snout", GOLD).at(0, 1.5f, -2.3f).mat("metal").box(-1, 0, -0.5f, 2, 2, 1, -0.2f).add();
        f.s("bull_ear", "head", false, fur).at(f.headW / 2f, -f.headH * 0.6f, 0.5f).rot(0, -20, 20).mat("fur").box(0, -0.5f, -1, 3, 1, 2).add();
        f.hump(fur, "fur", 10, 3, 5);
        return f.build();
    }

    /** Nagua: a tall, heavy-shouldered reptilian humanoid with a frilled crest, clawed hands and a thick tail. // approx */
    static ModelSpec nagua(String id, int hide, int dk, int eye) {
        Fig f = fig(id).legs(12, 5).torso(10, 9, 14, 8, 5, 7).arms(16, 4, 4).head(7, 6, 7).pose(18, 4, 12).armPitch(-14, -18)
                .skin(hide, "scales").shadow(1.0f);
        f.core(dk, "scales", hide, "scales", hide, "scales").muscles(mixRgb(hide, 0xF0E0B0, 0.25f)).head("monster", eye)
                .beastFace(darker(hide, 0.9f), dk, IVORY, false)
                .armsBuild(hide, "scales", hide, "scales", dk, "scales").claws(0x2A2420, 2)
                .legsBuild(hide, "scales", darker(hide, 0.9f), "scales", dk, "scales").tail(hide, dk, 5, "spike")
                .backSpikes(dk, 3, 1).loincloth(0x4A3020, 5);
        for (int k = 0; k < 3; k++) {
            f.p("frill" + k, "head", k == 1 ? hide : dk).at((k - 1) * 2.2f, -f.headH, 1).rot(-35, 0, (k - 1) * 30).mat("membrane")
                    .box(-1, -4, -0.5f, 2, 4, 2).add();
        }
        f.s("frill_side", "head", false, dk).at(f.headW / 2f, -3.5f, 1).rot(0, -30, -30).mat("membrane").box(0, -2, -0.5f, 3, 4, 1).add();
        return f.build();
    }

    /** Animated armour: an empty full helm, platebody and platelegs of one metal, moving on their own. */
    static ModelSpec animatedArmour(String id, int metal) {
        int dk = darker(metal, 0.75f);
        int rim = lighter(metal, 0.18f);
        Fig f = fig(id).legs(12, 4).torso(8, 7, 10, 6, 5, 5).arms(12, 3, 3).head(7, 7, 7).pose(4, 2, 6).armPitch(-6, -10)
                .skin(dk, "metal").shadow(0.6f);
        f.core(metal, "metal", dk, "metal", metal, "metal").head("none", 0)
                .armsBuild(metal, "metal", metal, "metal", dk, "metal").legsBuild(metal, "metal", metal, "metal", dk, "metal")
                .platebody(metal, rim).pauldrons(metal, rim, 2, 0, 0).armPlates(metal, rim).legPlates(metal, rim).tassets(metal, rim, 4)
                .helm("full", metal, rim);
        return f.build();
    }

    /** An aviansie of Armadyl: a feathered bird-man with layered wings, a hooked beak and a weapon for its style. */
    static ModelSpec aviansie(String id, int feather, int light, String style, float scale) {
        Fig f = BestiaryHumForms.aviansie(id, feather, light, 0xC8A040, 0xFFE060, 13, 1);
        f.skirt(darker(light, 0.8f), "cloth", 6, BestiaryHumGwd.ARMA_GOLD, 6);
        f.belt(DARK_LEATHER, BestiaryHumGwd.ARMA_GOLD, false);
        switch (style) {
            case "mage" -> f.staff("hand_r", 6, 0x4A3A2A, BestiaryHumGwd.ARMA_GOLD, 0x40E0FF, "full");
            case "range" -> {
                f.spear("hand_r", -25, 0x4A3A2A, 0xB0B0B0, 10);
                f.p("javelin_pack", "chest", 0x3A2A1E).at(0, -5, 3).rot(0, 0, -30).mat("leather").box(-1, 0, 0, 2, 6, 2).add();
            }
            default -> f.sword("hand_r", 55, 0xB8C0C8, BestiaryHumGwd.ARMA_GOLD, 8, 2);
        }
        return f.scale(scale).build();
    }

    /**
     * Kalphite (worker, soldier, guardian): a sandy-tan giant insect with a raised, plated thorax, a segmented
     * abdomen ending in a stinger, a crested head with green eyes, mandibles and antennae, small scything forelimbs
     * and six jointed legs.
     */
    static ModelSpec kalphite() {
        int shell = 0xC29A4A;
        int pale = 0xE6CC84;
        int dk = 0x6E5226;
        int eye = 0x60E040;
        ModelSpec.Builder b = ModelSpec.builder("kalphite").shadow(0.9f);
        part(b, "thorax", null, Role.BODY, shell).accent(pale).at(0, 13, -1).rot(-16, 0, 0).mat("chitin")
                .box(-3.5f, -4, -4, 7, 7, 8).add();
        part(b, "collar", "thorax", Role.STATIC, dk).accent(pale).at(0, -1.5f, -4).mat("chitin").box(-4, -3, -1, 8, 5, 2).add();
        part(b, "back_plate", "thorax", Role.STATIC, pale).accent(dk).at(0, -4, 0).mat("chitin").box(-3, -1, -3, 6, 1, 6).add();
        int[] aw = {8, 9, 8, 6};
        String prev = "thorax";
        for (int i = 0; i < aw.length; i++) {
            String n = "abdomen" + i;
            part(b, n, prev, Role.STATIC, i % 2 == 0 ? shell : lighter(shell, 0.08f)).accent(dk).at(0, 0, i == 0 ? 4 : 2.6f)
                    .rot(i == 0 ? 30 : 6, 0, 0).mat("chitin").box(-aw[i] / 2f, -aw[i] / 2f + 1, 0, aw[i], aw[i] - 2, 3).add();
            prev = n;
        }
        part(b, "stinger", prev, Role.TAIL, dk).at(0, 1, 3).rot(-20, 0, 0).mat("horn").box(-1, -1, 0, 2, 2, 2).box(-0.5f, -0.5f, 2, 1, 1, 2).add();
        part(b, "head", "thorax", Role.HEAD, shell).accent(eye).at(0, -3, -4.5f).rot(24, 0, 0).mat("chitin").pattern("blank")
                .box(-3, -3, -4, 6, 5, 4).add();
        b.part("eyes", "head", Role.STATIC, eye).pivot(0, -1.5f, -4.1f).material("energy").glow("full")
                .box(1, -0.5f, -0.4f, 2, 1, 1).box(-3, -0.5f, -0.4f, 2, 1, 1);
        part(b, "crest", "head", Role.STATIC, pale).accent(dk).at(0, -3, -1).rot(-35, 0, 0).mat("chitin").box(-2.5f, -3, -1, 5, 3, 2).add();
        sym(b, "mandible", "head", false, dk).at(1.6f, 1.5f, -4).rot(10, 25, 0).mat("horn")
                .box(-0.5f, -0.5f, -3, 1, 1, 3, 0.1f).add();
        sym(b, "antenna", "head", false, dk).at(1.2f, -3, -3).rot(-45, -18, 0).mat("horn").box(-0.5f, -6, -0.5f, 1, 6, 1, -0.1f).add();
        sym(b, "scythe", "thorax", false, shell).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(dk).at(3.5f, -2, -3.5f).rot(-55, 0, -14)
                .mat("chitin").box(-1, -1, -1, 2, 6, 2).add();
        sym(b, "scythe_blade", "scythe", true, pale).at(0, 5.5f, 0).rot(-85, 0, 0).mat("horn").box(-0.5f, 0, -1, 1, 6, 2).add();
        for (int i = 0; i < 3; i++) {
            sym(b, "leg" + i, "thorax", false, shell).roles(Role.LEG_EXTRA, Role.LEG_EXTRA).accent(dk).at(3.5f, 1.5f, -2 + i * 2.5f)
                    .rot(0, -(i - 1) * 32f, -40).mat("chitin").box(0, -1, -1, 6, 2, 2).add();
            sym(b, "shin" + i, "leg" + i, true, dk).at(5.7f, 0, 0).rot(0, 0, 95).mat("chitin").box(0, -0.75f, -0.75f, 9, 2, 2, -0.25f).add();
        }
        return b.build();
    }

    // ----------------------------------------------------------------------------------------------
    // colour helpers
    // ----------------------------------------------------------------------------------------------

    static int mixRgb(int a, int c, float t) {
        int r = (int) ((a >> 16 & 0xFF) + ((c >> 16 & 0xFF) - (a >> 16 & 0xFF)) * t);
        int g = (int) ((a >> 8 & 0xFF) + ((c >> 8 & 0xFF) - (a >> 8 & 0xFF)) * t);
        int bl = (int) ((a & 0xFF) + ((c & 0xFF) - (a & 0xFF)) * t);
        return r << 16 | g << 8 | bl;
    }

    /** Recolours the cow's white coat dark brown, its black patches near-black and its pink muzzle a dusky pink. */
    static IntUnaryOperator bullCoat() {
        return c -> {
            int r = c >> 16 & 0xFF;
            int g = c >> 8 & 0xFF;
            int bl = c & 0xFF;
            int lum = (r * 3 + g * 6 + bl) / 10;
            if (r > 200 && g > 200 && bl > 190) {
                return mixRgb(0x5A3420, 0x7A4A2A, (lum - 200) / 55f);
            }
            if (r > g + 40) {
                return mixRgb(c, 0x6A4038, 0.5f);
            }
            if (lum < 60) {
                return 0x3E2416;
            }
            return c;
        };
    }
}
