package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.BestiaryHumKit.*;

import com.runecraft.osrs.content.bestiary.BestiaryHumKit.Fig;

/**
 * The six Barrows brothers, each in his exact OSRS set: grey, withered undead with burning eyes, in tarnished,
 * rust-stained armour and their signature weapons.
 */
final class BestiaryHumBarrows {
    private BestiaryHumBarrows() {
    }

    static final int UNDEAD = 0x7E8A70;
    static final int EYE = 0xFF3A20;

    static List<ModelSpec> all() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(ahrim());
        l.add(dharok());
        l.add(guthan());
        l.add(karil());
        l.add(torag());
        l.add(verac());
        return l;
    }

    /** Glowing eye pinpoints peeking out of a helm/hood opening. */
    static void eyes(Fig f, String parent, float y, float z, float gap, int col) {
        f.s("eye_glow", parent, false, col).at(gap, y, z).mat("energy").glow("full").box(-0.5f, -0.5f, -0.3f, 1, 1, 1, -0.15f).add();
    }

    /** Ahrim the Blighted: hooded, robed mage, hunched, with his tall spiked staff. */
    static ModelSpec ahrim() {
        int robe = 0x3A3C4A;
        int robeDk = 0x2A2B36;
        int trim = 0x6A6A7E;
        Fig f = fig("ahrim").legs(12, 3).torso(7, 7, 8, 6, 5, 5).arms(12, 3, 3).head(7, 7, 7).pose(20, 1, 6).armPitch(-6, -30)
                .skin(UNDEAD, "skin").shadow(0.6f);
        f.core(robeDk, "cloth", robe, "cloth", robe, "cloth").head("skull", EYE)
                .armsBuild(robe, "cloth", robe, "cloth", UNDEAD, "skin").legsBuild(robeDk, "cloth", robeDk, "cloth", 0x2A2420, "leather")
                .robeSleeves(robe, trim).skirt(robe, "cloth", 12, trim, 6).helm("hood", 0x30323E, trim);
        // robe layers: a short over-tunic with a pointed front flap, a mantle, a rope belt
        f.p("tunic_flap", "hips", robeDk).at(0, -1, -3).rot(-8, 0, 0).mat("cloth").box(-2.5f, 0, -0.5f, 5, 6, 1).add();
        f.p("tunic_point", "tunic_flap", robe).at(0, 6, 0).rot(0, 0, 45).mat("cloth").box(-1.5f, -1.5f, -0.5f, 3, 3, 1, -0.2f).add();
        f.p("mantle", "chest", 0x30323E).at(0, -6, 0).mat("cloth").box(-5, -0.5f, -3.5f, 10, 3, 7, 0.1f).add();
        f.p("mantle_back", "mantle", robeDk).at(0, 2.5f, 3).rot(10, 0, 0).mat("cloth").box(-4.5f, 0, -0.5f, 9, 5, 1).add();
        f.p("rope_belt", "hips", 0x6A5A3A).at(0, -2, 0).mat("leather").box(-4, 0, -3, 8, 1, 6, 0.35f).add();
        f.p("rope_knot", "rope_belt", 0x6A5A3A).at(1.5f, 1, -3).rot(0, 0, 10).mat("leather").box(-0.5f, 0, -0.5f, 1, 4, 1).add();
        f.p("amulet", "chest", 0x8A8A9A).at(0, -4.5f, -3.3f).mat("metal").box(-1, 0, -0.5f, 2, 2, 1).add();
        eyes(f, "hood_shadow", 2.9f, -0.6f, 1.6f, EYE);
        f.staff("hand_r", 6, 0x3A3028, 0x5A5A68, 0x9A80C0, "full");
        return f.build();
    }

    /** Dharok the Wretched: a towering, hunched brute in his greathelm and plate, with the huge greataxe. */
    static ModelSpec dharok() {
        int plate = 0x5A5450;
        int dk = 0x403A36;
        Fig f = fig("dharok").legs(12, 4).torso(9, 9, 12, 7, 5, 6).arms(14, 4, 4).head(7, 7, 7).pose(22, 3, 10).armPitch(-10, -42)
                .skin(UNDEAD, "skin").shadow(0.8f);
        f.core(dk, "metal", plate, "metal", plate, "metal").head("skull", EYE)
                .armsBuild(plate, "metal", plate, "metal", dk, "metal").legsBuild(dk, "metal", plate, "metal", dk, "metal")
                .platebody(plate, -1).pauldrons(plate, -1, 3, 0, 0).armPlates(plate, -1).legPlates(plate, -1).tassets(plate, -1, 5)
                .belt(0x3A2A1E, 0x7A7068, false).helm("greathelm", plate, -1).gauntlets(dk, "metal");
        eyes(f, "helm_breaths", -1.2f, -0.6f, 1.8f, EYE);
        f.p("helm_crest", "helm", dk).at(0, -9f, 0).mat("metal").box(-0.5f, -2, -3, 1, 2, 6).add();
        // rusted rivet straps across the chest
        f.p("chest_strap", "breastplate", 0x3A2A1E).at(0, 1, -1.1f).rot(0, 0, 35).mat("leather").box(-6, 0, -0.3f, 12, 1, 1).add();
        f.greataxe("hand_r", 28, 0x3A2A1E, 0x6E6A66);
        return f.build();
    }

    /** Guthan the Infested: med helm with nose guard, platebody over a chainskirt, the barbed warspear. */
    static ModelSpec guthan() {
        int plate = 0x5E6050;
        int dk = 0x44463A;
        Fig f = fig("guthan").legs(12, 4).torso(8, 8, 10, 6, 5, 5).arms(13, 3, 3).head(7, 7, 7).pose(10, 2, 6).armPitch(-6, -20)
                .skin(UNDEAD, "skin").shadow(0.7f);
        f.core(dk, "chain", plate, "metal", plate, "metal").head("skull", EYE)
                .armsBuild(dk, "chain", plate, "metal", dk, "leather").legsBuild(dk, "chain", dk, "chain", 0x2E2A22, "leather")
                .platebody(plate, -1).pauldrons(plate, -1, 2, 0, 0).armPlates(plate, -1).skirt(0x6A6A62, "chain", 9, -1, 4)
                .belt(0x3A2A1E, 0x8A8A70, true).helm("med", plate, -1).boots(0x2E2A22, "leather").gauntlets(dk, "leather");
        f.p("chain_mantle", "chest", 0x6A6A62).at(0, -6, 0).mat("chain").box(-4.5f, -1, -3, 9, 3, 6, 0.2f).add();
        f.warspear("hand_r", 6, 0x4A3A28, 0x8A8C7A);
        return f.build();
    }

    /** Karil the Tainted: leather coif, studded leathertop and skirt, aiming his repeating crossbow. */
    static ModelSpec karil() {
        int leather = 0x4E4028;
        int dk = 0x3A2E1C;
        int green = 0x3A4A2A;
        Fig f = fig("karil").legs(12, 3).torso(7, 7, 8, 6, 5, 4).arms(12, 3, 3).head(7, 8, 7).pose(6, 2, 4).armPitch(-50, -75)
                .skin(UNDEAD, "skin").shadow(0.6f);
        f.core(dk, "leather", leather, "leather", leather, "leather").head("skull", EYE)
                .armsBuild(leather, "leather", UNDEAD, "skin", dk, "leather").legsBuild(dk, "leather", dk, "leather", 0x2A2016, "leather")
                .skirt(green, "leather", 7, dk, 5).belt(0x2A2016, 0x8A7A5A, true).helm("leather_coif", leather, -1).boots(0x2A2016, "leather")
                .gauntlets(dk, "leather");
        // studded leathertop: panels with stud rows and shoulder guards
        f.p("leathertop", "chest", green).at(0, -6, -2.6f).mat("leather").box(-3.5f, 0.5f, -0.5f, 7, 5, 1, 0.1f).add();
        f.p("studs", "leathertop", 0xA89A70).at(0, 1, -0.7f).mat("metal").box(-2.5f, 0, -0.2f, 1, 1, 1, -0.2f).box(1.5f, 0, -0.2f, 1, 1, 1, -0.2f)
                .box(-2.5f, 2, -0.2f, 1, 1, 1, -0.2f).box(1.5f, 2, -0.2f, 1, 1, 1, -0.2f).box(-0.5f, 1, -0.2f, 1, 1, 1, -0.2f).add();
        f.s("shoulder_guard", "arm", true, leather).at(0, -1.5f, 0).rot(0, 0, 12).mat("leather").box(-2, -1, -2, 4, 3, 4, 0.1f).add();
        f.s("bracer", "forearm", true, dk).at(0, 1, 0).mat("leather").box(-1.5f, 0, -1.5f, 3, 3, 3, 0.25f).add();
        f.p("quiver", "chest", dk).at(1.5f, -6, 3).rot(0, 0, 25).mat("leather").box(-1.5f, 0, -1, 3, 7, 2).add();
        f.p("quiver_bolts", "quiver", 0x8A8A80).at(0, 0, 0).mat("metal").box(-1, -2, -0.5f, 2, 2, 1).add();
        f.p("scarf", "chest", 0x5A2A1E).at(0, -6.5f, 0).mat("cloth").box(-3, -0.5f, -2.5f, 6, 1, 5, 0.3f).add();
        f.crossbow("hand_r", 92, 0x4A3420, 0x6A6A60);
        return f.build();
    }

    /** Torag the Corrupted: short and stout, squat helm, heavy plate, a hammer in each fist. */
    static ModelSpec torag() {
        int plate = 0x5A4E40;
        int dk = 0x403628;
        Fig f = fig("torag").legs(10, 5).torso(10, 10, 12, 7, 5, 7).arms(12, 4, 4).head(7, 7, 7).pose(10, 5, 10).armPitch(-20, -20)
                .skin(UNDEAD, "skin").shadow(0.8f);
        f.core(dk, "metal", plate, "metal", plate, "metal").head("skull", EYE)
                .armsBuild(plate, "metal", plate, "metal", dk, "metal").legsBuild(dk, "metal", plate, "metal", dk, "metal")
                .platebody(plate, -1).pauldrons(plate, -1, 2, 0, 0).armPlates(plate, -1).legPlates(plate, -1).tassets(plate, -1, 4)
                .belt(0x3A2A1E, 0x8A7A60, false).helm("med", plate, -1).gauntlets(dk, "metal");
        // squat bucket helm: wide flat top, brim, cheek plates down to the jaw
        f.p("helm_top", "helm", dk).at(0, -2, 0).mat("metal").box(-4.5f, -1, -4.5f, 9, 1, 9).add();
        f.p("helm_face", "helm", plate).at(0, 3, -4).mat("metal").pattern("visor").box(-3, 0, -0.6f, 6, 4, 1).add();
        f.p("beard_bones", "head", 0x6A6A5A).at(0, -1, -3.6f).mat("hair").box(-2.5f, 0, -0.6f, 5, 3, 1).add();
        f.warhammer("hand_r", 70, 0x3A2A1E, 0x6E6458, "hammer_r");
        f.warhammer("hand_l", 70, 0x3A2A1E, 0x6E6458, "hammer_l");
        return f.build();
    }

    /** Verac the Defiled: hunched, bell helm with a cross visor, brassard, plateskirt and his spiked flail. */
    static ModelSpec verac() {
        int plate = 0x625644;
        int dk = 0x463C2E;
        Fig f = fig("verac").legs(12, 4).torso(8, 8, 10, 6, 5, 5).arms(13, 3, 3).head(7, 7, 7).pose(24, 2, 6).armPitch(-8, -35)
                .skin(UNDEAD, "skin").shadow(0.7f);
        f.core(dk, "metal", plate, "metal", plate, "metal").head("skull", EYE)
                .armsBuild(dk, "chain", plate, "metal", dk, "metal").legsBuild(dk, "chain", dk, "chain", dk, "metal")
                .platebody(plate, -1).armPlates(plate, -1).skirt(plate, "metal", 9, dk, 5).belt(0x3A2A1E, 0x8A7A60, false)
                .helm("full", plate, -1).gauntlets(dk, "metal");
        // brassard: one huge stacked shoulder on each side, a cross on the visor and a pointed helm crown
        f.pauldrons(plate, dk, 3, 0, 0);
        f.p("helm_point", "helm", dk).at(0, -7.5f, 0).mat("metal").box(-2, -1, -2, 4, 1, 4).box(-1, -2, -1, 2, 1, 2).box(-0.5f, -3, -0.5f, 1, 1, 1).add();
        f.p("visor_cross", "helm", 0x2A2420).at(0, -5, -4.05f).mat("metal").box(-0.5f, 0, -0.5f, 1, 4, 1).add();
        f.p("chest_cross", "breastplate", 0xA8A090).at(0, 1, -1.1f).mat("metal").box(-0.5f, 0, -0.3f, 1, 4, 1).box(-1.5f, 1, -0.3f, 3, 1, 1).add();
        f.p("tabard", "hips", 0x5A4A3A).at(0, -1, -3).rot(-6, 0, 0).mat("cloth").box(-2, 0, -0.6f, 4, 9, 1).add();
        f.flail("hand_r", 55, 0x3A2A1E, 0x7A7262);
        return f.build();
    }
}
