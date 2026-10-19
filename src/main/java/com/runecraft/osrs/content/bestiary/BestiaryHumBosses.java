package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.BestiaryHumForms.*;
import static com.runecraft.osrs.content.bestiary.BestiaryHumKit.*;

import com.runecraft.osrs.content.bestiary.BestiaryHumKit.Fig;

/**
 * Humanoid world bosses: Obor, Bryophyta, Count Draynor, Delrith, Vet'ion/Calvar'ion, the Chaos Fanatic, the Crazy
 * Archaeologist, Skotizo, Vardorvis, Sol Heredit, the Royal Titans and the Moons of Peril.
 */
final class BestiaryHumBosses {
    private BestiaryHumBosses() {
    }

    /** Ids no other module defines (bestiary-owned). */
    static List<ModelSpec> fresh() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(obor());
        l.add(bryophyta());
        l.add(draynor());
        l.add(delrith());
        float moon = 1.2307693f;
        l.add(blueMoon("blue_moon", moon));
        l.add(eclipseMoon("eclipse_moon", moon));
        l.add(bloodMoon("blood_moon", moon));
        l.add(blueMoon("quest_blue_moon", moon));
        l.add(eclipseMoon("quest_eclipse_moon", moon));
        l.add(bloodMoon("quest_blood_moon", moon));
        return l;
    }

    /** Re-sculpts of ids defined by other modules. */
    static List<ModelSpec> overrides() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(vetion("vetion", 0x4A2A7A, 0xB060FF));
        l.add(vetion("calvarion", 0x2E5A3E, 0x60FF90));
        l.add(chaosFanatic());
        l.add(crazyArchaeologist());
        l.add(skotizo());
        l.add(vardorvis());
        l.add(solHeredit());
        l.add(titan("branda_the_fire_queen", true));
        l.add(titan("eldric_the_ice_king", false));
        return l;
    }

    /** Obor: the hill giant chieftain: obese, bald, tusked, a huge spiked club, fur pelt and bone trophies. */
    static ModelSpec obor() {
        int skin = 0xB08458;
        Fig f = fig("obor").legs(10, 5).torso(11, 11, 14, 8, 7, 8).arms(15, 5, 5).head(8, 7, 7).pose(14, 6, 14).armPitch(-16, -28)
                .skin(skin, "skin").shadow(1.1f);
        f.foreBend = 18;
        f.kneeBend = 16;
        f.core(0x5A3A1E, "leather", skin, "skin", skin, "skin").muscles(skin).paunch(skin, "skin", 3).head("monster", 0xE0C040)
                .armsBuild(skin, "skin", skin, "skin", darker(skin, 0.9f), "skin").legsBuild(skin, "skin", skin, "skin", darker(skin, 0.85f), "skin")
                .loincloth(0x5A3A1E, 7).belt(0x3A2614, 0xC8C0A8, true).hump(skin, "skin", 10, 4, 5)
                .hair(0x4A2E18, "bald_fringe");
        f.p("big_nose", "head", darker(skin, 0.92f)).at(0, -3.5f, -3.5f).rot(-10, 0, 0).mat("skin").box(-1, 0, -2, 2, 3, 2).add();
        f.s("tusk", "head", false, IVORY).at(2, -1, -3.5f).rot(-10, 0, 12).mat("horn").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        f.s("big_ear", "head", false, skin).at(4, -4, 0.5f).rot(0, -20, 0).mat("skin").box(-0.5f, -1.5f, -1, 2, 3, 2).add();
        f.p("pelt", "chest", 0x6A5038).at(0, -8, 0).rot(0, 0, -18).mat("fur").box(-7, -1, -4.5f, 9, 4, 9, 0.2f).add();
        f.p("pelt_tail", "pelt", 0x5A4028).at(-6, 3, 3).rot(20, 0, 10).mat("fur").box(-1, 0, -1, 2, 5, 2).add();
        f.p("bone_necklace", "chest", BONE).at(0, -7.5f, -4.6f).mat("bone").box(-3, 0, -0.5f, 6, 1, 1).box(-2.5f, 1, -0.5f, 1, 2, 1).box(-0.5f, 1, -0.5f, 1, 3, 1)
                .box(1.5f, 1, -0.5f, 1, 2, 1).add();
        f.p("belt_skull", "belt", BONE).at(-3, 0.5f, -4.4f).mat("bone").pattern("skull").box(-1, 0, -1, 2, 2, 2).add();
        f.s("wrist_wrap", "forearm", true, 0x5A3A1E).at(0, f.foreLen() - 2.5f, 0).mat("leather").box(-2.5f, 0, -2.5f, 5, 2, 5, 0.2f).add();
        f.club("hand_r", 55, 0x6A4A2A);
        f.p("club_band", "club", 0x5A5A5A).at(0, -6, 0).mat("metal").box(-1.5f, -1, -1.5f, 3, 1, 3, 0.1f).add();
        return f.scale(1.0f).build();
    }

    /** Bryophyta: the moss giantess: moss-grown skin, long trailing moss hair, a rough robe and a gnarled staff. */
    static ModelSpec bryophyta() {
        int skin = 0x5A7A3B;
        int moss = 0x3A6A1E;
        Fig f = fig("bryophyta").legs(12, 4).torso(9, 8, 11, 7, 6, 6).arms(15, 4, 4).head(8, 8, 7).pose(14, 4, 12).armPitch(-10, -30)
                .skin(skin, "moss").shadow(1.0f);
        f.foreBend = 16;
        f.core(0x3A5A1A, "moss", skin, "moss", 0x4A6A2A, "cloth").head("monster", 0xC0FF60)
                .armsBuild(skin, "moss", skin, "moss", darker(skin, 0.9f), "moss").legsBuild(skin, "moss", skin, "moss", darker(skin, 0.85f), "moss")
                .skirt(0x3A5A1A, "cloth", 11, 0x2A4A10, 7).hair(moss, "long").hump(skin, "moss", 9, 3, 5);
        f.p("moss_crown", "head", 0x2A5A16).at(0, -8, 0).mat("moss").box(-4.5f, -2, -4, 9, 2, 8).add();
        for (int k = 0; k < 4; k++) {
            f.p("moss_tuft" + k, "moss_crown", k % 2 == 0 ? 0x4A8A2A : 0x2A6A16).at(-3 + k * 2, -2, k % 2 == 0 ? -2 : 1).rot(-10, 0, (k - 1.5f) * 15)
                    .mat("moss").box(-1, -2, -1, 2, 2, 2).add();
        }
        f.s("moss_shoulder", "chest", false, moss).at(4.5f, -7, 0).rot(0, 0, -25).mat("moss").box(-2, -2, -3, 4, 3, 6).add();
        f.p("vine_belt", "hips", 0x2A4A10).at(0, -2, 0).mat("moss").box(-4.5f, 0, -3, 9, 1, 6, 0.3f).add();
        f.p("nose_big", "head", darker(skin, 0.9f)).at(0, -4, -3.5f).rot(-10, 0, 0).mat("moss").box(-1, 0, -1.5f, 2, 2, 2).add();
        f.staff("hand_r", 8, 0x5A4026, 0x3A6A1E, 0x80FF40, "full");
        return f.build();
    }

    /** Count Draynor: a tall, pale vampire in black evening dress, slicked hair with a widow's peak, high-collared cape. */
    static ModelSpec draynor() {
        int pale = 0xD8D0CC;
        int black = 0x1C1A22;
        int red = 0x9A1A1A;
        Fig f = fig("count_draynor").legs(13, 3).torso(7, 6, 8, 6, 5, 4).arms(13, 3, 3).head(6, 8, 6).pose(4, 2, 7).armPitch(-30, -30)
                .skin(pale, "skin").shadow(0.6f);
        f.foreBend = 40;
        f.core(black, "cloth", black, "cloth", black, "cloth").head("monster", 0xFF2020)
                .armsBuild(black, "cloth", black, "cloth", pale, "skin").legsBuild(black, "cloth", black, "cloth", 0x0E0E12, "leather")
                .claws(pale, 2).hair(0x0E0C10, "slick").cape(black, red, 22, false);
        f.p("shirt_front", "chest", 0xE8E4E0).at(0, -6, -2.6f).mat("cloth").box(-1.5f, 0.5f, -0.5f, 3, 4, 1).add();
        f.p("cravat", "shirt_front", red).at(0, 0.5f, -0.6f).mat("cloth").box(-1, 0, -0.3f, 2, 2, 1).add();
        f.s("lapel", "chest", false, 0x2A2832).at(2.2f, -5.5f, -2.6f).rot(0, 0, 15).mat("cloth").box(-0.5f, 0, -0.6f, 1, 5, 1).add();
        f.s("collar_wing", "cape_collar", false, red).at(3.5f, -1, 0).rot(0, 0, 25).mat("cloth").box(0, -5, -2, 1, 5, 4).add();
        f.p("collar_back", "cape_collar", black).at(0, -1, 2.5f).rot(-12, 0, 0).mat("cloth").box(-4, -5, -0.5f, 8, 5, 1).add();
        f.s("fang", "head", false, IVORY).at(1, -1.5f, -3.1f).mat("horn").box(-0.5f, 0, -0.4f, 1, 1, 1).add();
        f.p("medallion", "chest", GOLD).at(0, -2, -2.8f).mat("metal").box(-1, 0, -0.5f, 2, 2, 1).add();
        f.p("waistcoat_hem", "waist", 0x2A2832).at(0, -0.5f, -2).rot(-6, 0, 0).mat("cloth").box(-3.5f, 0, -0.5f, 7, 4, 1).add();
        return f.build();
    }

    /** Delrith: the demon summoned in Demon Slayer: purple-grey, great swept horns, burning maw, wingless. */
    static ModelSpec delrith() {
        Fig f = demon("delrith", 0x7A3A6A, 0x3A1030, 0xD8D0B8, 0xFFD040, 3);
        f.p("brand", "pecs", 0xFF7020).at(0, 1, -1.1f).mat("energy").glow("full").box(-1.5f, 0, -0.3f, 3, 1, 1).box(-0.5f, -1, -0.3f, 1, 3, 1).add();
        return f.build();
    }

    /** Vet'ion: a skeletal champion in ornate armour with a glowing crested skull-helm and a great halberd. */
    static ModelSpec vetion(String id, int armour, int glow) {
        int bone = 0xE0DCC8;
        Fig f = fig(id).legs(13, 3).torso(8, 5, 10, 6, 5, 5).arms(14, 3, 3).head(6, 7, 6).pose(10, 4, 10).armPitch(-20, -50)
                .skin(bone, "bone").shadow(0.9f);
        f.foreBend = 20;
        f.core(armour, "metal", bone, "bone", armour, "metal").head("skull", glow)
                .armsBuild(armour, "metal", bone, "bone", bone, "bone").legsBuild(bone, "bone", bone, "bone", armour, "metal")
                .platebody(armour, GOLD).pauldrons(armour, GOLD, 3, 2, GOLD).armPlates(armour, GOLD).legPlates(armour, GOLD)
                .tassets(armour, GOLD, 6).belt(0x2A1A1A, GOLD, false).cape(darker(armour, 0.7f), GOLD, 18, true);
        // ribs showing between the plates, a crested skull-helm with a glowing crown
        for (int i = 0; i < 3; i++) {
            f.p("rib" + i, "waist", bone).at(0, -4.5f + i * 1.5f, -2).mat("bone").box(-2.5f, 0, -0.6f, 5, 1, 1).add();
        }
        f.p("spine", "waist", bone).at(0, -5, 0).mat("bone").box(-0.5f, 0, -0.5f, 1, 5, 1).add();
        f.p("helm", "head", armour).at(0, -7, 0).mat("metal").box(-3.5f, -0.5f, -3.5f, 7, 3, 7, 0.1f).add();
        f.p("helm_crest", "helm", GOLD).at(0, -0.5f, 0).mat("metal").box(-0.5f, -3, -3.5f, 1, 3, 7).add();
        f.s("helm_wing", "helm", false, GOLD).at(3.5f, 0, 0).rot(0, 0, 40).mat("metal").box(0, -3, -1, 1, 3, 2).add();
        f.p("soul_flame", "helm_crest", glow).at(0, -3, 0).mat("energy").glow("full").box(-1, -2, -1, 2, 2, 2).add();
        f.halberd("hand_r", 20, 0x2A1A1A, 0xA8A8B8, 22);
        f.p("halberd_glow", "hal_blade", glow).at(3.5f, 0, -0.6f).mat("energy").glow("full").box(-0.5f, -3, -0.2f, 1, 6, 1, -0.2f).add();
        return f.build();
    }

    /** The Chaos Fanatic: a raving bald old mage in tattered chaos robes, long white beard, a gnarled staff. */
    static ModelSpec chaosFanatic() {
        int skin = 0xC8946A;
        int robe = 0x2A2026;
        int red = 0x8A1A1A;
        Fig f = fig("chaos_fanatic").legs(12, 3).torso(7, 7, 8, 6, 5, 4).arms(12, 3, 3).head(7, 8, 7).pose(14, 3, 14).armPitch(-60, -20)
                .skin(skin, "skin").shadow(0.6f);
        f.foreBend = 30;
        f.core(robe, "cloth", robe, "cloth", robe, "cloth").head("human", 0)
                .armsBuild(robe, "cloth", robe, "cloth", skin, "skin").legsBuild(robe, "cloth", robe, "cloth", 0x2A2016, "leather")
                .robeSleeves(robe, red).skirt(robe, "cloth", 11, red, 7).hair(HAIR_WHITE, "bald_fringe").beard(HAIR_WHITE, "long")
                .belt(red, GOLD, true);
        f.p("chaos_sash", "chest", red).at(0, -6, -2.6f).rot(0, 0, 30).mat("cloth").box(-0.75f, 0, -0.5f, 2, 9, 1).add();
        f.p("chaos_mark", "chest", 0xFF4020).at(0, -4.5f, -2.8f).mat("energy").glow("full").box(-1, 0, -0.3f, 2, 2, 1).add();
        f.p("wild_brows", "head", HAIR_WHITE).at(0, -5.5f, -3.6f).mat("hair").box(-3, 0, -0.5f, 6, 1, 1).add();
        f.s("tatter", "skirt_front", false, darker(robe, 0.8f)).at(1.5f, 10, 0).rot(0, 0, 8).mat("cloth").box(-1, 0, -0.5f, 2, 2, 1).add();
        f.staff("hand_r", 10, 0x3A2A1A, 0x5A3A3A, 0xFF3020, "full");
        f.p("hand_spark", "hand_l", 0xFF5020).at(0, 3, -1).mat("energy").glow("full").box(-0.5f, -0.5f, -0.5f, 1, 1, 1).add();
        return f.build();
    }

    /** The Crazy Archaeologist: wide-brimmed hat, khaki field gear, satchel and an explosive book held aloft. */
    static ModelSpec crazyArchaeologist() {
        int skin = 0xC8946A;
        int khaki = 0xA08A5A;
        int brown = 0x5A4028;
        Fig f = fig("crazy_archaeologist").legs(12, 3).torso(7, 7, 8, 6, 5, 4).arms(12, 3, 3).head(7, 8, 7).pose(10, 3, 12).armPitch(-110, -30)
                .skin(skin, "skin").shadow(0.6f);
        f.foreBend = 20;
        f.core(brown, "cloth", khaki, "cloth", khaki, "cloth").head("human", 0)
                .armsBuild(khaki, "cloth", skin, "skin", skin, "skin").legsBuild(brown, "cloth", brown, "cloth", 0x3A2A1A, "leather")
                .boots(0x3A2A1A, "leather").belt(0x3A2A1A, 0xA89060, true).beard(0x8A6A4A, "full").hair(0x6A4A30, "short");
        f.p("hat_brim", "head", brown).at(0, -7.5f, 0).mat("leather").box(-5.5f, -0.5f, -5.5f, 11, 1, 11).add();
        f.p("hat_crown", "hat_brim", brown).at(0, -0.5f, 0).mat("leather").box(-3.5f, -3, -3.5f, 7, 3, 7).add();
        f.p("hat_band", "hat_crown", 0x3A2A1A).at(0, -1, 0).mat("leather").box(-3.5f, 0, -3.5f, 7, 1, 7, 0.15f).add();
        f.p("hat_dent", "hat_crown", darker(brown, 0.85f)).at(0, -3, 0).mat("leather").box(-1, -0.5f, -2.5f, 2, 1, 5).add();
        f.p("satchel", "hips", brown).at(-4.5f, -1, 0).rot(0, 0, 10).mat("leather").box(-1.5f, 0, -2, 2, 4, 4).add();
        f.p("satchel_strap", "chest", 0x3A2A1A).at(0, -6, 0).rot(0, 0, -35).mat("leather").box(-5.5f, 0, -2.5f, 11, 1, 5, 0.25f).add();
        f.p("shirt_pocket", "chest", darker(khaki, 0.9f)).at(-2, -4.5f, -2.6f).mat("cloth").box(-1, 0, -0.5f, 2, 2, 1).add();
        f.p("rolled_sleeve", "forearm_l", khaki).at(0, 0, 0).mat("cloth").box(-1.5f, 0, -1.5f, 3, 1, 3, 0.3f).add();
        f.p("rolled_sleeve_r", "forearm_r", khaki).at(0, 0, 0).mat("cloth").box(-1.5f, 0, -1.5f, 3, 1, 3, 0.3f).add();
        f.p("spectacles", "head", 0x3A3A3A).at(0, -4.5f, -3.6f).mat("metal").box(-2.5f, 0, -0.4f, 5, 1, 1, -0.1f).add();
        f.book("hand_l", 0x6A2A1A, 0xE8E0C8);
        f.p("book_spark", "book", 0xFFB030).at(0, -1.5f, 0).mat("energy").glow("full").box(-1, -1, -1, 2, 1, 2).add();
        f.p("trowel", "hand_r", 0x9A9A9A).at(0, 2.5f, -0.5f).rot(80, 0, 0).mat("metal").box(-1, -3, -0.25f, 2, 3, 1, -0.2f).add();
        return f.build();
    }

    /** Skotizo: a colossal dark demon of the catacombs: black-grey hide, rows of spikes, long claws, violet fire eyes. */
    static ModelSpec skotizo() {
        Fig f = demon("skotizo", 0x3A3438, 0x18141A, 0x8A8090, 0xC060FF, 6);
        f.backSpikes(0x6A6070, 3, 2);
        f.horns(0x6A6070, 1.2f, -5.5f, -2, 10, 50, 1);
        f.demonWings("chest", 0x2A2428, 0x221C24, 0xC060FF, 2.5f, -f.chestH + 1.5f, f.depth / 2f + 0.5f, 14, 3, null);
        f.s("long_claw", "fingers", true, 0x8A8090).at(0, 1.5f, -0.5f).rot(40, 0, 0).mat("horn").box(-0.5f, 0, -0.5f, 1, 4, 1).add();
        return f.build();
    }

    /**
     * Vardorvis: the strangled, gaunt warlord of the Stranglewood: grey corrupted flesh, a broken crested helm,
     * tattered crimson cloth, writhing tendrils from his back and a great two-handed axe.
     */
    static ModelSpec vardorvis() {
        int flesh = 0x7A7472;
        int armour = 0x4A3E3A;
        int crimson = 0x7A1A1A;
        Fig f = fig("vardorvis").legs(15, 3).torso(8, 6, 10, 7, 6, 5).arms(16, 3, 3).head(6, 7, 6).pose(18, 4, 12).armPitch(-30, -40)
                .skin(flesh, "flesh").shadow(0.9f);
        f.foreBend = 24;
        f.core(armour, "metal", flesh, "flesh", armour, "metal").head("monster", 0xFF3030)
                .armsBuild(flesh, "flesh", flesh, "flesh", darker(flesh, 0.85f), "flesh").claws(0x3A3030, 2)
                .legsBuild(armour, "metal", flesh, "flesh", armour, "metal")
                .platebody(armour, crimson).pauldrons(armour, crimson, 2, 3, 0x3A3030).legPlates(armour, -1)
                .skirt(crimson, "cloth", 10, darker(crimson, 0.7f), 6).cape(crimson, -1, 22, true).helm("med", armour, -1);
        f.p("helm_crest", "helm", crimson).at(0, -2, 0).rot(-10, 0, 0).mat("hair").box(-0.5f, -3, -3, 1, 3, 7).add();
        f.p("helm_crack", "helm", 0x2A1A1A).at(1.5f, 0, -4).mat("metal").box(-0.5f, -1, -0.6f, 1, 3, 1).add();
        for (int k = 0; k < 4; k++) {
            String n = "tendril" + k;
            f.p(n, "chest", k % 2 == 0 ? 0x5A2A2A : 0x4A2020).at(-3 + k * 2, -6, 3).rot(-50 + k * 8, (k - 1.5f) * 18, 0).mat("flesh")
                    .box(-0.5f, -6, -0.5f, 1, 6, 1).add();
            f.p(n + "_tip", n, 0xFF4040).at(0, -6, 0).rot(-30, 0, 0).mat("energy").glow("full").box(-0.5f, -2, -0.5f, 1, 2, 1, -0.1f).add();
        }
        f.s("vein", "forearm", true, 0xFF4040).at(1.2f, 1, -1.6f).mat("energy").glow("full").box(-0.3f, 0, -0.3f, 1, 4, 1, -0.3f).add();
        f.greataxe("hand_r", 30, 0x2A1A14, 0x6A6060);
        return f.build();
    }

    /** Sol Heredit: the Colosseum's champion: a towering gladiator in sun-gold armour with a crested helm, spear and shield. */
    static ModelSpec solHeredit() {
        int skin = 0xA06A40;
        int gold = 0xE0B040;
        int red = 0xB03A1A;
        Fig f = fig("sol_heredit").legs(14, 4).torso(9, 9, 13, 8, 6, 6).arms(15, 4, 4).head(7, 7, 7).pose(10, 5, 12).armPitch(-30, -50)
                .skin(skin, "skin").shadow(1.0f);
        f.foreBend = 22;
        f.core(red, "cloth", skin, "skin", gold, "metal").muscles(skin).head("monster", 0xFFD060)
                .armsBuild(skin, "skin", skin, "skin", darker(skin, 0.9f), "skin").legsBuild(skin, "skin", skin, "skin", 0x5A3A1E, "leather")
                .platebody(gold, red).pauldrons(gold, red, 3, 0, 0).legPlates(gold, red).skirt(red, "leather", 8, gold, 8).belt(0x5A3A1E, gold, false)
                .helm("full", gold, red).boots(0x5A3A1E, "leather");
        for (int k = 0; k < 5; k++) {
            f.p("plume" + k, "helm", k % 2 == 0 ? red : darker(red, 0.85f)).at(0, -8, -3 + k * 1.6f).rot(-30 + k * 22, 0, 0).mat("hair")
                    .box(-0.5f, -3, -0.75f, 1, 3, 2).add();
        }
        f.p("sun_disc", "breastplate", 0xFFE070).at(0, 2, -1.2f).mat("energy").glow("full").box(-1.5f, -1.5f, -0.3f, 3, 3, 1).add();
        for (int k = 0; k < 4; k++) {
            f.p("sun_ray" + k, "sun_disc", gold).at(0, 0, 0).rot(0, 0, k * 45).mat("metal").box(-0.5f, -3, -0.4f, 1, 6, 1, -0.1f).add();
        }
        f.s("arm_band", "arm", true, gold).at(0, 2, 0).mat("metal").box(-2.5f, 0, -2.5f, 5, 2, 5, -0.2f).add();
        f.s("bracer", "forearm", true, gold).at(0, 2, 0).mat("metal").box(-2.5f, 0, -2.5f, 5, 3, 5, -0.2f).add();
        f.spear("hand_r", 20, 0x5A3A1E, gold, 22);
        f.shield("hand_l", "round", gold, red, 0xFFE070);
        return f.build();
    }

    /** The Royal Titans: Branda the Fire Queen (molten) and Eldric the Ice King (frozen): crowned elemental giants. */
    static ModelSpec titan(String id, boolean fire) {
        int body = fire ? 0xE06020 : 0xA8D8F0;
        int crust = fire ? 0x3A1A12 : 0x5A8AB0;
        String mat = fire ? "lava" : "ice";
        int eye = fire ? 0xFFE060 : 0x60E0FF;
        Fig f = fig(id).legs(13, 5).torso(10, 9, 14, 9, 6, 7).arms(17, 5, 6).head(7, 8, 7).pose(14, 5, 14).armPitch(-20, -26)
                .skin(body, mat).shadow(1.2f);
        f.foreBend = 22;
        f.core(crust, fire ? "rock" : "ice", body, mat, crust, fire ? "rock" : "ice").head("monster", eye)
                .armsBuild(crust, fire ? "rock" : "ice", body, mat, crust, fire ? "rock" : "ice")
                .legsBuild(crust, fire ? "rock" : "ice", body, mat, crust, fire ? "rock" : "ice").hump(crust, fire ? "rock" : "ice", 12, 4, 5);
        f.p("crown", "head", fire ? GOLD : 0xD8F0FF).at(0, -8, 0).mat(fire ? "metal" : "ice").box(-4, -1.5f, -4, 8, 2, 8, 0.2f).add();
        for (int k = 0; k < 5; k++) {
            f.p("crown_spike" + k, "crown", fire ? 0xFFB030 : 0xE8FFFF).at(-3 + k * 1.5f, -1.5f, -3.5f).rot(-10, 0, (k - 2) * 10)
                    .mat(fire ? "lava" : "ice").glow(fire ? "full" : null).box(-0.5f, -3 - (k == 2 ? 2 : k % 2), -0.5f, 1, 3 + (k == 2 ? 2 : k % 2), 1).add();
        }
        f.s("shoulder_shard", "chest", false, fire ? 0xFF9030 : 0xD8F4FF).at(5.5f, -9, 0).rot(0, 0, 30).mat(fire ? "lava" : "ice")
                .box(-1.5f, -5, -1.5f, 3, 5, 3).add();
        f.s("shoulder_shard2", "chest", false, fire ? 0xC04010 : 0xB8E0F8).at(6.5f, -8, 2).rot(20, 0, 50).mat(fire ? "lava" : "ice")
                .box(-1, -4, -1, 2, 4, 2).add();
        if (fire) {
            f.hair(0xFF7020, "long");
            f.p("flame_hair", "hair_back", 0xFFB030).at(0, 4, 1).mat("lava").glow("full").box(-3, 0, -0.5f, 6, 6, 1).add();
            f.skirt(0x3A1A12, "rock", 10, 0xFF6020, 8);
            f.p("fire_core", "chest", 0xFFD060).at(0, -5, -4.2f).mat("energy").glow("full").box(-1.5f, -1.5f, -0.3f, 3, 3, 1).add();
        } else {
            f.beard(0xE8F8FF, "long");
            f.p("ice_core", "chest", 0x80F0FF).at(0, -5, -4.2f).mat("energy").glow("full").box(-1.5f, -1.5f, -0.3f, 3, 3, 1).add();
            f.cape(0x4A6A9A, 0xD8F0FF, 22, true);
        }
        f.s("knuckle_spike", "fingers", true, fire ? 0xFFA030 : 0xE0FFFF).at(0, 1, -2).rot(-60, 0, 0).mat(fire ? "lava" : "ice")
                .box(-1.5f, -0.5f, -0.5f, 3, 1, 2).add();
        return f.build();
    }

    // ---- Moons of Peril -------------------------------------------------------------------------------------

    /** Common Moon warrior: tall, armoured in Ralos-style plate, a crescent-crested moon mask with a glowing face. */
    static Fig moon(String id, float scale, int armour, int trim, int glow) {
        Fig f = fig(id).legs(13, 4).torso(8, 7, 10, 7, 5, 5).arms(13, 3, 4).head(6, 7, 6).pose(8, 4, 10).armPitch(-24, -40)
                .skin(0x6A5A4A, "skin").scale(scale).shadow(0.8f);
        f.foreBend = 22;
        f.core(darker(armour, 0.75f), "metal", armour, "metal", armour, "metal").head("eyes", glow)
                .armsBuild(armour, "metal", darker(armour, 0.85f), "metal", darker(armour, 0.75f), "metal")
                .legsBuild(darker(armour, 0.75f), "metal", armour, "metal", darker(armour, 0.75f), "metal")
                .platebody(armour, trim).pauldrons(armour, trim, 3, 0, 0).armPlates(armour, trim).legPlates(armour, trim)
                .tassets(armour, trim, 6).belt(0x2A2018, trim, false).helm("full", armour, trim);
        f.p("moon_mask", "helm", trim).at(0, -4, -4.1f).mat("metal").box(-2.5f, -2.5f, -0.4f, 5, 5, 1).add();
        f.p("moon_face", "moon_mask", glow).at(0, 0, -0.5f).mat("energy").glow("full").box(-1.5f, -1.5f, -0.3f, 3, 3, 1, -0.1f).add();
        f.s("crescent", "helm", false, trim).at(3.5f, -7, 0).rot(0, 0, 30).mat("metal").box(-0.5f, -4, -0.5f, 1, 4, 1).add();
        f.s("crescent_tip", "crescent", true, trim).at(0, -4, 0).rot(0, 0, -50).mat("metal").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        f.p("moon_gem", "breastplate", glow).at(0, 2, -1.2f).mat("energy").glow("full").box(-1, -1, -0.3f, 2, 2, 1).add();
        return f;
    }

    static ModelSpec blueMoon(String id, float scale) {
        Fig f = moon(id, scale, 0x3A5A8A, 0xC8D8F0, 0x80D0FF);
        f.cape(0x2A3A6A, 0xC8D8F0, 20, false);
        f.spear("hand_r", 25, 0x2A2A3A, 0xA8E0FF, 20);
        f.p("frost_tip", "spearhead", 0xC0F0FF).at(0, -3, 0).mat("ice").glow("full").box(-1, -2, -1, 2, 2, 2, -0.2f).add();
        f.shield("hand_l", "round", 0x3A5A8A, 0xC8D8F0, 0x80D0FF);
        return f.build();
    }

    static ModelSpec eclipseMoon(String id, float scale) {
        Fig f = moon(id, scale, 0x2A2630, 0xD8B040, 0xFFE080);
        f.cape(0x1A1620, 0xD8B040, 20, false);
        f.sword("hand_r", 70, 0x8A8A94, 0xD8B040, 11, 2);
        f.shield("hand_l", "tower", 0x2A2630, 0xD8B040, 0xFFE080);
        f.p("eclipse_ring", "shield", 0xFFE080).at(0, -1, -1.1f).mat("energy").glow("full").box(-2.5f, -2.5f, -0.3f, 5, 1, 1).box(-2.5f, 1.5f, -0.3f, 5, 1, 1)
                .box(-2.5f, -1.5f, -0.3f, 1, 3, 1).box(1.5f, -1.5f, -0.3f, 1, 3, 1).add();
        return f.build();
    }

    static ModelSpec bloodMoon(String id, float scale) {
        Fig f = moon(id, scale, 0x6A1A1E, 0xC8A070, 0xFF4040);
        f.cape(0x4A0E12, 0xC8A070, 20, true);
        f.greataxe("hand_r", 60, 0x2A1A14, 0xB0A0A0);
        f.sword("hand_l", 60, 0xA02020, 0xC8A070, 8, 2);
        return f.build();
    }
}
