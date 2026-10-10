package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.BestiaryHumForms.*;
import static com.runecraft.osrs.content.bestiary.BestiaryHumKit.*;
import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/** God Wars Dungeon: the four generals, their bodyguards and Nex. */
final class BestiaryHumGwd {
    private BestiaryHumGwd() {
    }

    static final int BANDOS = 0x6A5A3A;
    static final int BANDOS_DK = 0x4A3E28;
    static final int OURG = 0x6E7E46;
    static final int SARA_WHITE = 0xE8EAF0;
    static final int SARA_BLUE = 0x3A5AC8;
    static final int ZAM_RED = 0xA82020;
    static final int ARMA_GOLD = 0xD8B040;

    static List<ModelSpec> all() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(graardor());
        l.add(sergeant("sergeant_strongstack", 0x6E9A3A, "melee"));
        l.add(sergeant("sergeant_steelwill", 0x5E8A4A, "mage"));
        l.add(sergeant("sergeant_grimspike", 0x7AA048, "range"));
        l.add(kril());
        l.add(tstanon());
        l.add(zakln());
        l.add(balfrug());
        l.add(zilyana());
        l.add(starlight());
        l.add(growler());
        l.add(bree());
        l.add(kreearra());
        l.add(aviGuard("wingman_skree", 0x6A5A8A, 0xC8B8E8, "mage"));
        l.add(aviGuard("flockleader_geerin", 0x5A4A36, 0xC8B090, "range"));
        l.add(aviGuard("flight_kilisa", 0x4A6A6A, 0xB0D0C8, "melee"));
        l.add(nex());
        return l;
    }

    /** General Graardor: a colossal hunched ourg in Bandos plate, a small tusked head sunk between spiked pauldrons. */
    static ModelSpec graardor() {
        Fig f = fig("general_graardor").legs(10, 6).torso(12, 12, 18, 9, 6, 10).arms(18, 6, 7).head(7, 6, 7).pose(30, 6, 16)
                .armPitch(-22, -34).skin(OURG, "hide").shadow(1.3f);
        f.foreBend = 22;
        f.kneeBend = 14;
        f.core(BANDOS_DK, "metal", OURG, "hide", BANDOS, "metal").head("monster", 0xFFC830).beastFace(darker(OURG, 0.9f), darker(OURG, 0.75f), IVORY, false)
                .armsBuild(OURG, "hide", OURG, "hide", darker(OURG, 0.9f), "hide")
                .legsBuild(OURG, "hide", OURG, "hide", BANDOS_DK, "metal")
                .platebody(BANDOS, GOLD).pauldrons(BANDOS, GOLD, 3, 3, IVORY).tassets(BANDOS, GOLD, 6).belt(DARK_LEATHER, GOLD, false)
                .legPlates(BANDOS, GOLD);
        // huge tusks, small ears, a heavy brow and a spiked collar
        f.s("tusk", "jaw", false, IVORY).at(2.2f, 0, -2).rot(-18, 0, 14).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1, 0.15f).add();
        f.s("goblin_ear", "head", false, OURG).at(3.5f, -3.5f, 0.5f).rot(0, -20, -20).mat("hide").box(0, -1, -0.5f, 2, 2, 1).add();
        f.p("heavy_brow", "head", darker(OURG, 0.8f)).at(0, -4.5f, -3.5f).rot(15, 0, 0).mat("hide").box(-3.5f, -1, -1, 7, 2, 2).add();
        for (int k = 0; k < 4; k++) {
            f.p("collar_spike" + k, "gorget", IVORY).at(-4.5f + k * 3, -1, -3).rot(-30, 0, (k - 1.5f) * 20).mat("horn").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        }
        f.s("bracer", "forearm", true, BANDOS_DK).at(0, 2, 0).mat("metal").box(-4, 0, -4, 8, 4, 8, -0.3f).add();
        f.s("bracer_spike", "bracer", true, IVORY).at(3.5f, 2, 0).rot(0, 0, -70).mat("horn").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        f.p("bandos_emblem", "breastplate", GOLD).at(0, 2, -1.1f).mat("metal").box(-1.5f, 0, -0.3f, 3, 3, 1).add();
        f.mace("hand_r", 40, DARK_LEATHER, 0x6A6A6A, 6);
        return f.build();
    }

    /** Bandos goblin sergeants: Strongstack (melee, armoured), Steelwill (mage, robed) and Grimspike (ranger). */
    static ModelSpec sergeant(String id, int skin, String style) {
        Fig f = goblin(id, skin, 0xE0C020);
        int armour = BANDOS;
        switch (style) {
            case "mage" -> {
                f.core(0x3A4A6A, "cloth", 0x4A5A7A, "cloth", 0x4A5A7A, "cloth").head("human", 0)
                        .armsBuild(0x4A5A7A, "cloth", 0x4A5A7A, "cloth", skin, "skin").legsBuild(0x3A4A6A, "cloth", 0x3A4A6A, "cloth", DARK_LEATHER, "leather")
                        .robeSleeves(0x4A5A7A, GOLD).skirt(0x3A4A6A, "cloth", 7, GOLD, 6).belt(DARK_LEATHER, GOLD, true).helm("hood", 0x3A4A6A, GOLD);
                f.staff("hand_r", 8, WOOD, BANDOS_DK, 0x80C0FF, "full");
            }
            case "range" -> {
                f.core(DARK_LEATHER, "leather", LEATHER, "leather", LEATHER, "leather").head("human", 0)
                        .armsBuild(LEATHER, "leather", skin, "skin", skin, "skin").legsBuild(DARK_LEATHER, "leather", skin, "skin", DARK_LEATHER, "leather")
                        .skirt(armour, "leather", 5, BANDOS_DK, 5).belt(DARK_LEATHER, GOLD, true).helm("leather_coif", LEATHER, -1);
                f.p("quiver", "chest", DARK_LEATHER).at(1.5f, -5, 2.5f).rot(0, 0, 25).mat("leather").box(-1, 0, -1, 2, 6, 2).add();
                f.p("quiver_arrows", "quiver", 0xC8B898).at(0, 0, 0).mat("feathers").box(-1, -2, -1, 2, 2, 2, -0.2f).add();
                f.bow("hand_l", WOOD, 0xE0D8C0);
            }
            default -> {
                f.core(BANDOS_DK, "metal", armour, "metal", armour, "metal").head("human", 0)
                        .armsBuild(armour, "metal", skin, "skin", skin, "skin").legsBuild(BANDOS_DK, "metal", armour, "metal", BANDOS_DK, "metal")
                        .platebody(armour, GOLD).pauldrons(armour, GOLD, 2, 1, IVORY).tassets(armour, GOLD, 4).belt(DARK_LEATHER, GOLD, false)
                        .helm("med", armour, GOLD);
                f.sword("hand_r", 50, 0xB0B0B8, GOLD, 8, 2);
                f.shield("hand_l", "round", BANDOS_DK, GOLD, GOLD);
            }
        }
        goblinHead(f);
        return f.scale(1.0f).build();
    }

    /** K'ril Tsutsaroth: a towering red demon of Zamorak, great black wings, a spiked tail and his barbed spear. */
    static ModelSpec kril() {
        Fig f = demon("kril_tsutsaroth", ZAM_RED, 0x3A0C0C, 0x2A2420, 0xFFD030, 6);
        f.demonWings("chest", 0x2A1414, 0x3A1A1A, 0x8A8070, 2.5f, -f.chestH + 1.5f, f.depth / 2f + 0.5f, 20, 4, null);
        f.spear("hand_r", 30, 0x2A1A14, 0xC02010, 22);
        f.p("spear_barb", "spearhead", 0x8A1010).at(0, -1, 0).mat("metal").box(-2, -1, -0.5f, 4, 1, 1).add();
        f.p("chain_belt", "hips", 0x5A5A5A).at(0, -1.5f, 0).mat("chain").box(-f.hipW / 2f, 0, -f.depth / 2f, f.hipW, 1, f.depth, 0.4f).add();
        f.p("zamorak_mark", "pecs", 0xFF6020).at(0, 1, -1.1f).mat("energy").glow("full").box(-0.5f, 0, -0.3f, 1, 2, 1, -0.1f).add();
        return f.build();
    }

    /** Tstanon Karlak: a hulking black-and-crimson melee demon with heavy horns and an iron-spiked chest plate. */
    static ModelSpec tstanon() {
        Fig f = demon("tstanon_karlak", 0x3A2A2A, 0x1A1010, 0xA89880, 0xFF4020, 4);
        f.p("chest_plate", "chest", 0x4A4446).at(0, -f.chestH + 1, -f.depth / 2f - 0.6f).mat("metal").box(-4, 0, -0.8f, 8, 5, 1).add();
        f.mace("hand_r", 60, 0x2A1A14, 0x5A5A5E, 4);
        return f.build();
    }

    /** Zakl'n Gritch: a lanky, horned ranged demon with a bundle of javelins. */
    static ModelSpec zakln() {
        Fig f = demon("zakln_gritch", 0x8A5A2A, 0x3A2010, 0x2A2420, 0xFFE040, 1);
        f.spear("hand_r", -20, 0x4A3020, 0xA0A0A8, 12);
        f.p("javelins", "chest", 0x4A3020).at(0, -6, 3.5f).rot(0, 0, 30).mat("wood").box(-1, -6, -0.5f, 1, 12, 1).box(0.5f, -5, -0.5f, 1, 12, 1).add();
        return f.build();
    }

    /** Balfrug Kreeyath: a lesser demon mage with crackling fire in its claws. */
    static ModelSpec balfrug() {
        Fig f = demon("balfrug_kreeyath", 0x8A2A4A, 0x3A0C1E, 0xC8B8A0, 0xFF60FF, 2);
        f.s("hand_fire", "hand", true, 0xFF7020).at(0, 2, -1).mat("lava").glow("full").box(-1.5f, -1, -1.5f, 3, 3, 3, 0.2f).add();
        f.s("hand_fire_tip", "hand_fire", true, 0xFFC040).at(0, -1, 0).rot(0, 45, 0).mat("energy").glow("full").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        return f.build();
    }

    /** Commander Zilyana: Saradomin's icyene general in white-and-gold plate, with great white wings and her sword. */
    static ModelSpec zilyana() {
        Fig f = fig("commander_zilyana").legs(13, 3).torso(7, 6, 8, 6, 5, 4).arms(12, 3, 3).head(6, 7, 6).pose(4, 3, 7).armPitch(-6, -40)
                .skin(0xE8C8A8, "skin").shadow(0.7f);
        f.core(0xC8CCD8, "metal", SARA_WHITE, "metal", SARA_WHITE, "metal").head("human", 0)
                .armsBuild(SARA_WHITE, "metal", SARA_WHITE, "metal", 0xC8CCD8, "metal").legsBuild(SARA_BLUE, "cloth", SARA_WHITE, "metal", 0xC8CCD8, "metal")
                .platebody(SARA_WHITE, GOLD).pauldrons(SARA_WHITE, GOLD, 2, 0, 0).armPlates(SARA_WHITE, GOLD).legPlates(SARA_WHITE, GOLD)
                .skirt(SARA_BLUE, "cloth", 8, GOLD, 9).belt(0x2A3A7A, GOLD, false).hair(0xE8D080, "long");
        f.p("tiara", "head", GOLD).at(0, -6.6f, -3.1f).mat("metal").box(-3, -0.5f, -0.5f, 6, 1, 1).box(-0.5f, -1.5f, -0.6f, 1, 1, 1).add();
        f.p("tiara_gem", "tiara", 0x80B0FF).at(0, -1, -0.8f).mat("energy").glow("full").box(-0.5f, -0.5f, -0.3f, 1, 1, 1, -0.1f).add();
        f.p("sara_star", "breastplate", SARA_BLUE).at(0, 1.5f, -1.1f).mat("metal").box(-0.5f, 0, -0.3f, 1, 3, 1).box(-1.5f, 1, -0.3f, 3, 1, 1).add();
        f.featherWings("chest", 0xF6F6FA, 0xD8E0F0, 0xC8D0E8, 2, -5, 3, 15, 6);
        f.sword("hand_r", 64, 0xE8ECF4, GOLD, 13, 2);
        f.p("sword_gem", "sword_guard", SARA_BLUE).at(0, -0.5f, -0.6f).mat("energy").glow("full").box(-0.5f, -0.5f, -0.3f, 1, 1, 1).add();
        return f.build();
    }

    /** Starlight: Saradomin's white unicorn with a glowing horn, golden hooves and a silver mane. */
    static ModelSpec starlight() {
        ModelSpec.Builder b = ModelSpec.builder("starlight").shadow(0.9f);
        horse(b, 0xF0F0F4, 0xC8D4E8, GOLD, 0x5080E0, true);
        part(b, "uni_horn", "hhead", Role.STATIC, 0xF0E8C0).at(0, -3, -2).rot(-50, 0, 0).mat("horn").glow("full").box(-0.5f, -6, -0.5f, 1, 6, 1, 0.1f).add();
        part(b, "uni_horn_base", "hhead", Role.STATIC, GOLD).at(0, -3, -2).rot(-50, 0, 0).mat("metal").box(-1, -1, -1, 2, 1, 2).add();
        part(b, "saddle_cloth", "barrel", Role.STATIC, SARA_BLUE).at(0, -4, 0).mat("cloth").box(-5, -0.5f, -4, 10, 4, 7, 0.1f).add();
        part(b, "saddle_trim", "saddle_cloth", Role.STATIC, GOLD).at(0, 3.5f, 0).mat("cloth").box(-5, 0, -4, 10, 1, 7, 0.2f).add();
        part(b, "forelock", "hhead", Role.STATIC, 0xC8D4E8).at(0, -3, -1).rot(30, 0, 0).mat("hair").box(-1, -1, -1, 2, 2, 2).add();
        for (int k = 0; k < 3; k++) {
            part(b, "horn_ring" + k, "uni_horn", Role.STATIC, GOLD).at(0, -1.5f - k * 1.6f, 0).mat("metal").box(-0.5f, -0.5f, -0.5f, 1, 1, 1, 0.15f).add();
        }
        part(b, "bridle", "hmuzzle", Role.STATIC, GOLD).at(0, -1, -2).mat("leather").box(-1.5f, -1, -1, 3, 1, 2, 0.2f).add();
        part(b, "breast_strap", "chest_h", Role.STATIC, SARA_BLUE).at(0, 0, -2).mat("leather").box(-4, -1, -0.5f, 8, 1, 1, 0.2f).add();
        part(b, "breast_medal", "breast_strap", Role.STATIC, GOLD).at(0, 0, -0.6f).mat("metal").box(-1, -1, -0.3f, 2, 2, 1).add();
        return b.build();
    }

    /** Growler: a great white lion-spirit of Saradomin with a heavy layered mane and blue-glowing eyes. */
    static ModelSpec growler() {
        int fur = 0xE8E0C8;
        int mane = 0xF4F0E8;
        int dk = darker(fur, 0.85f);
        ModelSpec.Builder b = ModelSpec.builder("growler").shadow(0.9f);
        part(b, "body", null, Role.BODY, fur).at(0, 13, 1).mat("fur").box(-4.5f, -4, -8, 9, 8, 16).add();
        part(b, "belly", "body", Role.STATIC, dk).at(0, 3.5f, 0).mat("fur").box(-3.5f, 0, -6, 7, 1, 12).add();
        part(b, "haunch", "body", Role.STATIC, fur).at(0, -1, 6).rot(8, 0, 0).mat("fur").box(-5, -3, -2, 10, 7, 5).add();
        part(b, "mane_back", "body", Role.STATIC, mane).at(0, -2, -6).mat("fur").box(-6, -5, -3, 12, 10, 5).add();
        part(b, "head", "body", Role.HEAD, fur).accent(0x60A0FF).at(0, -2, -9).mat("fur").pattern("eyes").glow("eyes").box(-3, -3, -4, 6, 6, 5).add();
        part(b, "mane_front", "head", Role.STATIC, mane).at(0, -0.5f, -1).mat("fur").box(-5, -5, -1, 10, 10, 2).add();
        for (int k = 0; k < 6; k++) {
            double a = Math.toRadians(k * 60);
            part(b, "mane_tuft" + k, "mane_front", Role.STATIC, k % 2 == 0 ? mane : darker(mane, 0.9f)).at((float) Math.cos(a) * 5, (float) Math.sin(a) * 5, 0)
                    .rot(0, 0, k * 60 + 90).mat("fur").box(-1.5f, -2, -1, 3, 3, 2).add();
        }
        part(b, "muzzle", "head", Role.STATIC, 0xF4F0E0).at(0, 1, -4).mat("fur").box(-2, -1.5f, -2, 4, 3, 2).add();
        part(b, "nose", "muzzle", Role.STATIC, 0x5A4A4A).at(0, -1.5f, -2).mat("skin").box(-1, 0, -0.4f, 2, 1, 1).add();
        part(b, "jaw", "head", Role.JAW, dk).at(0, 2.5f, -3.5f).mat("fur").glow("maw").box(-2, 0, -2.5f, 4, 1, 3).add();
        sym(b, "fang", "jaw", false, IVORY).at(1.2f, 0, -2.3f).mat("horn").box(-0.5f, -1, -0.5f, 1, 1, 1).add();
        sym(b, "ear", "head", false, fur).at(2.5f, -3, -1).mat("fur").box(-1, -1.5f, -0.5f, 2, 2, 1).add();
        sym(b, "leg_f", null, false, fur).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).at(3, 14, -6).mat("fur").box(-1.5f, -1, -1.5f, 3, 9, 3, 0.4f).add();
        sym(b, "paw_f", "leg_f", true, dk).at(0, 8, 0).mat("fur").box(-2, 0, -3, 4, 2, 4).add();
        sym(b, "claw_f", "paw_f", true, 0xD0D8E8).at(0, 1, -3).mat("horn").box(-1.5f, 0, -0.8f, 1, 1, 1).box(0.5f, 0, -0.8f, 1, 1, 1).add();
        sym(b, "leg_b", null, false, fur).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).at(3, 13, 6).rot(-10, 0, 0).mat("fur").box(-2, -1, -2, 4, 6, 4, 0.2f).add();
        sym(b, "shank_b", "leg_b", true, fur).at(0, 5, 0).rot(18, 0, 0).mat("fur").box(-1.5f, 0, -1.5f, 3, 5, 3).add();
        sym(b, "paw_b", "shank_b", true, dk).at(0, 4, 0).rot(-8, 0, 0).mat("fur").box(-2, 0, -3, 4, 2, 4).add();
        String prev = "haunch";
        for (int i = 0; i < 4; i++) {
            part(b, "tail" + i, prev, Role.TAIL, fur).at(0, i == 0 ? -2 : 0, i == 0 ? 3 : 3.5f).rot(i == 0 ? -30 : 15, 0, 0).mat("fur").box(-0.5f, -0.5f, 0, 1, 1, 4).add();
            prev = "tail" + i;
        }
        part(b, "tail_tuft", prev, Role.STATIC, mane).at(0, 0, 3.5f).mat("fur").box(-1, -1, 0, 2, 2, 3).add();
        for (int k = 0; k < 6; k++) {
            double a = Math.toRadians(k * 60 + 30);
            part(b, "mane_ruff" + k, "mane_back", Role.STATIC, k % 2 == 0 ? darker(mane, 0.92f) : mane).at((float) Math.cos(a) * 6, (float) Math.sin(a) * 5, 0)
                    .rot(0, 0, k * 60 + 120).mat("fur").box(-1.5f, -2, -2, 3, 3, 3).add();
        }
        sym(b, "haunch_m", "haunch", false, dk).at(5, 0, 0).mat("fur").box(-0.5f, -2, -2, 1, 5, 4).add();
        sym(b, "toe_b", "paw_b", true, 0xD0D8E8).at(0, 1, -3).mat("horn").box(-1.5f, 0, -0.8f, 1, 1, 1).box(0.5f, 0, -0.8f, 1, 1, 1).add();
        sym(b, "whisker_pad", "muzzle", false, 0xFFFFF4).at(1.5f, 0, -2).mat("fur").box(-0.5f, -0.5f, -0.5f, 1, 2, 1).add();
        part(b, "aura", "body", Role.STATIC, 0x80B0FF).at(0, -4.5f, 0).mat("energy").glow("full").box(-1, -1, -5, 2, 1, 10, -0.2f).add();
        return b.build();
    }

    /** Bree: a centaur archer of Saradomin: a chestnut horse body, a human torso in blue and gold, and a longbow. */
    static ModelSpec bree() {
        int coat = 0x8A5A34;
        int skin = 0xC8946A;
        ModelSpec.Builder b = ModelSpec.builder("bree").shadow(0.9f);
        horse(b, coat, 0x3A2A1A, 0x2A2420, 0x000000, false);
        part(b, "c_waist", "barrel", Role.STATIC, skin).at(0, -2, -8).rot(-6, 0, 0).mat("skin").box(-3, -5, -2, 6, 5, 4).add();
        part(b, "c_chest", "c_waist", Role.STATIC, SARA_BLUE).at(0, -5, 0).mat("cloth").box(-4, -6, -2.5f, 8, 6, 5).add();
        part(b, "c_vest_trim", "c_chest", Role.STATIC, GOLD).at(0, -6, -2.5f).mat("cloth").box(-0.5f, 0, -0.6f, 1, 6, 1).add();
        part(b, "c_belt", "c_waist", Role.STATIC, DARK_LEATHER).at(0, -1, 0).mat("leather").box(-3, 0, -2, 6, 1, 4, 0.3f).add();
        part(b, "c_neck", "c_chest", Role.STATIC, skin).at(0, -6, 0).mat("skin").box(-1, -2, -1, 2, 2, 2).add();
        part(b, "c_head", "c_neck", Role.HEAD, skin).at(0, -1.5f, 0).mat("face").box(-3, -7, -3, 6, 7, 6).add();
        part(b, "c_hair", "c_head", Role.STATIC, 0x5A3A1E).at(0, -7, 0).mat("hair").box(-3, -1, -3, 6, 2, 6, 0.3f).box(-3, 1, 2, 6, 4, 1, 0.3f).add();
        part(b, "c_beard", "c_head", Role.STATIC, 0x5A3A1E).at(0, -2.5f, -3).mat("hair").box(-2.5f, 0, -0.8f, 5, 3, 1).add();
        sym(b, "c_arm", "c_chest", false, SARA_BLUE).roles(Role.ARM_LEFT, Role.ARM_RIGHT).at(5, -5, 0).rot(-40, 0, -8).mat("cloth").box(-1.5f, -1, -1.5f, 3, 6, 3).add();
        sym(b, "c_fore", "c_arm", true, skin).at(0, 5, 0).rot(-30, 0, 0).mat("skin").box(-1.5f, 0, -1.5f, 3, 5, 3, -0.1f).add();
        sym(b, "c_hand", "c_fore", true, skin).at(0, 5, 0).mat("skin").box(-1.5f, 0, -1.5f, 3, 2, 2).add();
        sym(b, "c_bracer", "c_fore", true, DARK_LEATHER).at(0, 1, 0).mat("leather").box(-1.5f, 0, -1.5f, 3, 3, 3, 0.25f).add();
        part(b, "c_bow", "c_hand_l", Role.STATIC, WOOD).at(0, 1, -1).rot(70, 0, 0).mat("wood").box(-0.5f, -7, -0.5f, 1, 14, 1).add();
        part(b, "c_bow_tip", "c_bow", Role.STATIC, GOLD).at(0, -7, 0).rot(-30, 0, 0).mat("metal").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        part(b, "c_bow_string", "c_bow", Role.STATIC, 0xE0D8C0).at(0, 0, 2).mat("cloth").box(-0.5f, -7, -0.5f, 1, 14, 1, -0.4f).add();
        part(b, "c_quiver", "c_chest", Role.STATIC, DARK_LEATHER).at(1, -5, 2.5f).rot(0, 0, 25).mat("leather").box(-1, 0, 0, 2, 7, 2).add();
        part(b, "c_arrows", "c_quiver", Role.STATIC, 0xF0F0F0).at(0, 0, 1).mat("feathers").box(-1, -2, -1, 2, 2, 2, -0.2f).add();
        part(b, "c_cloth", "barrel", Role.STATIC, SARA_BLUE).at(0, -4, 2).mat("cloth").box(-5, -0.5f, -3, 10, 5, 7, 0.1f).add();
        part(b, "c_cloth_trim", "c_cloth", Role.STATIC, GOLD).at(0, 4.5f, 0).mat("cloth").box(-5, 0, -3, 10, 1, 7, 0.2f).add();
        return b.build();
    }

    /** Kree'arra: Armadyl's colossal aviansie general with golden armour plates and enormous wings. */
    static ModelSpec kreearra() {
        Fig f = aviansie("kreearra", 0x5A4232, 0xC8B090, 0xD8A030, 0xFFE060, 20, 4);
        f.p("arma_chest", "chest", ARMA_GOLD).at(0, -f.chestH + 1, -f.depth / 2f - 1.2f).mat("metal").box(-4, 0, -0.5f, 8, 5, 1).add();
        f.p("arma_chest_gem", "arma_chest", 0x40E0E0).at(0, 2, -0.7f).mat("energy").glow("full").box(-1, -1, -0.3f, 2, 2, 1).add();
        f.s("arma_shoulder", "arm", true, ARMA_GOLD).at(0, -1.5f, 0).rot(0, 0, -14).mat("metal").box(-3, -1.5f, -3, 6, 3, 6, 0.1f).add();
        f.s("arma_bracer", "forearm", true, ARMA_GOLD).at(0, 2, 0).mat("metal").box(-2.5f, 0, -2.5f, 5, 3, 5, -0.2f).add();
        f.skirt(0xE8E0D0, "cloth", 7, ARMA_GOLD, 8);
        f.belt(0x3A2A1E, ARMA_GOLD, false);
        return f.build();
    }

    /** Kree'arra's flight: Wingman Skree (mage), Flockleader Geerin (ranged) and Flight Kilisa (melee). */
    static ModelSpec aviGuard(String id, int feather, int light, String style) {
        Fig f = aviansie(id, feather, light, 0xC8A040, 0xFFE060, 14, 1);
        f.skirt(darker(light, 0.8f), "cloth", 6, ARMA_GOLD, 7);
        f.belt(0x3A2A1E, ARMA_GOLD, false);
        switch (style) {
            case "mage" -> f.staff("hand_r", 6, 0x4A3A2A, ARMA_GOLD, 0x40E0FF, "full");
            case "range" -> {
                f.spear("hand_r", -25, 0x4A3A2A, 0xB0B0B0, 10);
                f.p("javelin_pack", "chest", 0x3A2A1E).at(0, -5, 3).rot(0, 0, -30).mat("leather").box(-1, 0, 0, 2, 6, 2).add();
            }
            default -> f.sword("hand_r", 55, 0xB8C0C8, ARMA_GOLD, 9, 2);
        }
        return f.build();
    }

    /**
     * Nex: Zaros's general: a tall dark figure in black-and-violet plate with a horned skull-helm, clawed gauntlets
     * and vast skeletal wings veined with purple energy.
     */
    static ModelSpec nex() {
        int plate = 0x2E2638;
        int dk = 0x1C1624;
        int violet = 0xB050F0;
        Fig f = fig("nex").legs(13, 4).torso(8, 7, 11, 7, 5, 5).arms(14, 3, 4).head(6, 7, 6).pose(12, 3, 14).armPitch(-24, -24)
                .skin(0x6A5A7A, "hide").shadow(1.0f);
        f.foreBend = 26;
        f.core(dk, "metal", plate, "metal", plate, "metal").head("monster", violet)
                .armsBuild(plate, "metal", plate, "metal", dk, "metal").claws(0xD8D0E0, 2)
                .legsBuild(dk, "metal", plate, "metal", dk, "metal")
                .platebody(plate, violet).pauldrons(plate, violet, 3, 2, 0xD8D0E0).armPlates(plate, violet).legPlates(plate, violet)
                .tassets(plate, violet, 6).belt(dk, violet, false)
                .horns(0xD0C8D8, 2.4f, -6, 0.5f, 55, 50, 2);
        f.p("skull_helm", "head", 0x4A4058).at(0, -7, -0.2f).mat("bone").box(-3.5f, -0.5f, -3.5f, 7, 3, 7, 0.1f).add();
        f.p("skull_crest", "skull_helm", 0x6A5A78).at(0, -0.5f, 0).mat("bone").box(-0.5f, -2, -3, 1, 2, 6).add();
        f.p("rune_core", "breastplate", violet).at(0, 1.5f, -1.2f).mat("energy").glow("full").box(-1, 0, -0.3f, 2, 2, 1).add();
        f.p("tatter_skirt", "hips", 0x2A1E36).at(0, 1, 2.7f).rot(10, 0, 0).mat("cloth").box(-4, 0, -0.5f, 8, 10, 1).add();
        f.demonWings("chest", 0x3A3046, 0x2A1E3A, violet, 2.5f, -f.chestH + 1.5f, f.depth / 2f + 0.5f, 22, 3, "runes");
        return f.build();
    }
}
