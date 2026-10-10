package com.runecraft.osrs.content.godwars;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom models for the God Wars Dungeon: the four generals (incl. the built-in General Graardor
 * and K'ril Tsutsaroth, picked up by creature id), Nex and her generals, the bodyguards and the
 * followers that have a non-human shape.
 */
final class GwdModels {
    private GwdModels() {
    }

    static List<ModelSpec> all() {
        List<ModelSpec> list = new ArrayList<>();
        list.add(kreearra());
        list.add(zilyana());
        list.add(nex());
        list.add(graardor());
        list.add(kril());
        // Kree'arra's flock and the Armadyl aviansies.
        list.add(ModelKit.aviansie("flight_kilisa", 0x6A5A3A, 0xC8B080, 0xD8A030, 0x8A8A9A, 1.0f));
        list.add(ModelKit.aviansie("wingman_skree", 0x3A4A6A, 0x8AA0C0, 0xD8B040, 0xC0C0D0, 1.0f));
        list.add(ModelKit.aviansie("flockleader_geerin", 0x4A5A3A, 0xA0B080, 0xD8A030, 0xB09040, 1.0f));
        list.add(ModelKit.aviansie("gwd_aviansie", 0x7A6A5A, 0xB0A080, 0xC8A040, 0x6A6A70, 1.0f));
        list.add(ModelKit.aviansie("gwd_aviansie_elite", 0xA0A0B0, 0xE0E0E8, 0xD8B040, 0xC8B060, 1.0f));
        // Zilyana's guard.
        list.add(starlight());
        list.add(growler());
        list.add(bree());
        // Graardor's sergeants and the Bandos goblins.
        list.add(sergeant("sergeant_strongstack", 0x6A8A3A, 0x5A4A3A, 0x8A8A8A, "mace"));
        list.add(sergeant("sergeant_steelwill", 0x5A8A4A, 0x3A3A6A, 0x6A4AC0, "staff"));
        list.add(sergeant("sergeant_grimspike", 0x7A8A3A, 0x4A3A2A, 0x6A4A2A, "bow"));
        list.add(ModelKit.goblin("gwd_goblin", 0x6A9A3A, 0x6A5A3A, 0x4A3A2A, 1.0f)
                .part("spear", "arm_right", Role.STATIC, 0x8A6A3A).pivot(0, 6, 0).box(-0.5f, -10, -1, 1, 18, 1)
                .part("spear_tip", "spear", Role.STATIC, 0xA0A0A0).pivot(0, -10, 0).box(-1, -3, -1.5f, 2, 3, 2)
                .build());
        // K'ril's demons and the Zamorak imps / hellhounds.
        list.add(demon("tstanon_karlak", 0x8A2A1A, 0x2A1A1A, 1.0f, true).build());
        list.add(demon("zakln_gritch", 0x6A1A2A, 0x1A1A1A, 1.0f, true).build());
        list.add(demon("balfrug_kreeyath", 0x3A1A4A, 0x1A1A1A, 1.0f, true).build());
        list.add(demon("gwd_imp", 0xA03020, 0x2A1A1A, 1.0f, false)
                .part("imp_wing_left", "body", Role.WING_LEFT, 0x6A1A1A).pivot(3, -11, 4).box(0, 0, 0, 7, 6, 1)
                .part("imp_wing_right", "body", Role.WING_RIGHT, 0x6A1A1A).pivot(-3, -11, 4).box(-7, 0, 0, 7, 6, 1)
                .scale(0.45f).build());
        list.add(ModelKit.hound("gwd_hellhound", 0x5A1A10, 0x6A2010, 0xF06020, 1.0f, false));
        // Nex's generals and blood reavers.
        list.add(ModelKit.hoodedSpirit("fumus", 0x4A4A4A, 0x8A8A8A, 0xC0C0C0, 1.0f));
        list.add(ModelKit.hoodedSpirit("umbra", 0x1A1A2A, 0x4A3A6A, 0x7A3AC0, 1.0f));
        list.add(ModelKit.hoodedSpirit("cruor", 0x5A0A0A, 0xA02020, 0xE03030, 1.0f));
        list.add(ModelKit.hoodedSpirit("glacies", 0x5A90C0, 0xD8F0FF, 0xA0E0FF, 1.0f));
        list.add(bloodReaver());
        return list;
    }

    /** Kree'arra: a towering aviansie with a vast wingspan, golden talons and Armadyl's gold. */
    private static ModelSpec kreearra() {
        int feathers = 0x4A4A5A;
        int dark = 0x2A2A38;
        int gold = 0xC8A040;
        return ModelSpec.builder("kreearra").shadow(1.4f)
                .part("leg_left", null, Role.LEG_FRONT_LEFT, gold).pivot(3.5f, 13, 0).box(-1.5f, 0, -1.5f, 3, 9, 3)
                .box(-2.5f, 9, -5, 5, 2, 7)
                .part("thigh_left", "leg_left", Role.STATIC, feathers).pivot(0, 0, 0).box(-2.5f, -2, -2.5f, 5, 5, 5)
                .part("leg_right", null, Role.LEG_FRONT_RIGHT, gold).pivot(-3.5f, 13, 0).box(-1.5f, 0, -1.5f, 3, 9, 3)
                .box(-2.5f, 9, -5, 5, 2, 7)
                .part("thigh_right", "leg_right", Role.STATIC, feathers).pivot(0, 0, 0).box(-2.5f, -2, -2.5f, 5, 5, 5)
                .part("body", null, Role.BODY, feathers).pivot(0, 13, 0).box(-6, -15, -4, 12, 15, 8)
                .part("breast", "body", Role.STATIC, 0x8A8070).pivot(0, 0, 0).box(-5, -14, -4.5f, 10, 9, 1).pattern("stripes")
                .part("belt", "body", Role.STATIC, gold).pivot(0, 0, 0).box(-6.5f, -4, -4.5f, 13, 4, 9).pattern("plates")
                .part("neck", "body", Role.NECK, feathers).pivot(0, -15, -1).box(-2.5f, -5, -2.5f, 5, 6, 5)
                .part("head", "neck", Role.HEAD, dark).pivot(0, -5, 0).box(-3.5f, -6, -4.5f, 7, 6, 8).pattern("eyes")
                .part("beak", "head", Role.STATIC, 0xD8B040).pivot(0, -3, -4.5f).box(-1.5f, -1.5f, -5, 3, 3, 5)
                .box(-1, 0.5f, -6.5f, 2, 2, 2)
                .part("jaw", "head", Role.JAW, 0xB89030).pivot(0, 0, -4.5f).box(-1, 0, -4, 2, 1, 4)
                .part("crest", "head", Role.STATIC, 0xB04A20).pivot(0, -6, 0).box(-1, -4, -1, 2, 4, 7).pattern("stripes")
                .part("wing_left", "body", Role.WING_LEFT, feathers).pivot(4, -13, 4).rotate(0, -25, 0)
                .box(0, -2, -1, 18, 4, 3).box(1, 2, 0, 17, 14, 1).pattern("stripes")
                .part("wing_left_tip", "wing_left", Role.STATIC, dark).pivot(16, 0, 0).box(0, -6, 0, 8, 26, 1).pattern("stripes")
                .part("wing_right", "body", Role.WING_RIGHT, feathers).pivot(-4, -13, 4).rotate(0, 25, 0)
                .box(-18, -2, -1, 18, 4, 3).box(-18, 2, 0, 17, 14, 1).pattern("stripes")
                .part("wing_right_tip", "wing_right", Role.STATIC, dark).pivot(-16, 0, 0).box(-8, -6, 0, 8, 26, 1).pattern("stripes")
                .part("tail", "body", Role.TAIL, dark).pivot(0, -2, 4).rotate(-30, 0, 0).box(-5, 0, 0, 10, 2, 10).pattern("stripes")
                .build();
    }

    /** Commander Zilyana: an icyene in white and gold with great feathered wings and a Saradomin sword. */
    private static ModelSpec zilyana() {
        int gold = 0xE0C040;
        ModelSpec.Builder b = ModelKit.humanoid("commander_zilyana", 0xE8C8A8, 0xE8E8F0, 0xC8B070, "eyes")
                .part("hair", "head", Role.STATIC, 0xF0D070).pivot(0, 0, 0).box(-4.5f, -8.5f, -4.5f, 9, 3, 9).box(-4, -6, 3, 8, 12, 2)
                .part("skirt", "body", Role.STATIC, 0xD8C890).pivot(0, 0, 0).box(-4.5f, -2, -2.5f, 9, 6, 5).pattern("plates")
                .part("breastplate", "body", Role.STATIC, gold).pivot(0, 0, 0).box(-4.5f, -11, -2.5f, 9, 5, 1).pattern("plates")
                .part("pauldron_left", "arm_left", Role.STATIC, gold).pivot(0, 0, 0).box(-1.5f, -3, -2.5f, 5, 3, 5)
                .part("pauldron_right", "arm_right", Role.STATIC, gold).pivot(0, 0, 0).box(-3.5f, -3, -2.5f, 5, 3, 5)
                .part("sword", "arm_right", Role.STATIC, 0xD8E0F0).pivot(-1, 9, 0).box(-0.5f, 0, -15, 1, 2, 13).pattern("glow")
                .part("sword_hilt", "arm_right", Role.STATIC, gold).pivot(-1, 9, 0).box(-2.5f, -0.5f, -2.5f, 5, 3, 1)
                .box(-1, 0, -1.5f, 2, 2, 3);
        return ModelKit.featherWings(b, "body", -10, 0xF0F0F8, 0xD0D8E8, 13).build();
    }

    /** Nex: a horned, winged Zarosian general in black armour with violet glow. */
    private static ModelSpec nex() {
        int armour = 0x2A2438;
        int glow = 0x9A4AD0;
        ModelSpec.Builder b = ModelKit.humanoid("nex", 0x6A6A88, armour, armour, "eyes")
                .part("horns", "head", Role.STATIC, 0x1A1A22).pivot(0, 0, 0).box(-5, -10, -1, 2, 5, 2).box(3, -10, -1, 2, 5, 2)
                .box(-6, -13, 0, 1, 3, 1).box(5, -13, 0, 1, 3, 1)
                .part("visor", "head", Role.STATIC, glow).pivot(0, 0, 0).box(-3, -5, -4.4f, 6, 1, 1).pattern("glow")
                .part("gem", "body", Role.STATIC, glow).pivot(0, 0, 0).box(-1.5f, -10, -2.6f, 3, 3, 1).pattern("glow")
                .part("tassets", "body", Role.STATIC, 0x3A2A4A).pivot(0, 0, 0).box(-4.5f, -1, -2.5f, 9, 5, 5).pattern("plates")
                .part("pauldron_left", "arm_left", Role.STATIC, armour).pivot(0, 0, 0).box(-2, -4, -3, 6, 4, 6).box(1, -7, -1, 1, 3, 1)
                .part("pauldron_right", "arm_right", Role.STATIC, armour).pivot(0, 0, 0).box(-4, -4, -3, 6, 4, 6)
                .box(-2, -7, -1, 1, 3, 1)
                .part("claws", "arm_right", Role.STATIC, glow).pivot(0, 10, 0).box(-3, 0, -2.5f, 4, 2, 1);
        return ModelKit.membraneWings(b, "body", -10, 2.5f, armour, 0x4A2A6A, 18).build();
    }

    /** General Graardor: a hulking ourg in Bandos armour wielding a huge mace. */
    private static ModelSpec graardor() {
        int skin = 0x6A7A4A;
        int armour = 0x5A4030;
        int metal = 0x7A7A7A;
        return ModelSpec.builder("general_graardor").shadow(0.9f)
                .part("leg_left", null, Role.LEG_FRONT_LEFT, skin).pivot(4, 16, 0).box(-3, 0, -3, 6, 8, 6)
                .part("leg_right", null, Role.LEG_FRONT_RIGHT, skin).pivot(-4, 16, 0).box(-3, 0, -3, 6, 8, 6)
                .part("body", null, Role.BODY, skin).pivot(0, 16, 0).box(-8, -14, -5, 16, 14, 10)
                .part("chestplate", "body", Role.STATIC, armour).pivot(0, 0, 0).box(-7, -13, -6, 14, 7, 1).pattern("plates")
                .part("belt", "body", Role.STATIC, armour).pivot(0, 0, 0).box(-8.5f, -4, -5.5f, 17, 4, 11).pattern("plates")
                .part("tassets", "body", Role.STATIC, 0x4A3828).pivot(0, 0, 0).box(-7, 0, -5.5f, 14, 3, 1)
                .part("head", "body", Role.HEAD, skin).pivot(0, -14, -2).box(-4, -6, -5, 8, 7, 7).pattern("eyes")
                .part("helm", "head", Role.STATIC, metal).pivot(0, 0, 0).box(-4.5f, -7, -5.5f, 9, 2, 8).box(-0.5f, -10, -3, 1, 3, 5)
                .part("jaw", "head", Role.JAW, skin).pivot(0, 0, -3).box(-3.5f, 0, -3, 7, 3, 4)
                .part("tusks", "jaw", Role.STATIC, 0xE8E0C0).pivot(0, 0, 0).box(-3, -2, -3.5f, 1, 2, 1).box(2, -2, -3.5f, 1, 2, 1)
                .part("arm_left", "body", Role.ARM_LEFT, skin).pivot(10, -11, 0).box(-2, -2, -3, 6, 16, 6)
                .part("pad_left", "arm_left", Role.STATIC, armour).pivot(0, 0, 0).box(-3, -4, -4, 8, 4, 8).pattern("plates")
                .part("arm_right", "body", Role.ARM_RIGHT, skin).pivot(-10, -11, 0).box(-4, -2, -3, 6, 16, 6)
                .part("pad_right", "arm_right", Role.STATIC, armour).pivot(0, 0, 0).box(-5, -4, -4, 8, 4, 8).pattern("plates")
                .part("mace", "arm_right", Role.STATIC, 0x5A4A3A).pivot(-1, 13, 0).box(-1, -1, -12, 2, 2, 13)
                .part("mace_head", "mace", Role.STATIC, metal).pivot(0, 0, -12).box(-3, -3, -6, 6, 6, 6).pattern("plates")
                .build();
    }

    /** A demon body plan: digitigrade legs, scaled torso, horned head, clawed arms and a spaded tail. */
    private static ModelSpec.Builder demon(String id, int skin, int horn, float scale, boolean tail) {
        ModelSpec.Builder b = ModelSpec.builder(id).scale(scale).shadow(0.8f)
                .part("leg_left", null, Role.LEG_FRONT_LEFT, skin).pivot(3.5f, 11, 0).box(-2, 0, -3, 4, 6, 5)
                .box(-1.5f, 6, 0, 3, 5, 3).box(-2, 11, -4, 4, 2, 6)
                .part("leg_right", null, Role.LEG_FRONT_RIGHT, skin).pivot(-3.5f, 11, 0).box(-2, 0, -3, 4, 6, 5)
                .box(-1.5f, 6, 0, 3, 5, 3).box(-2, 11, -4, 4, 2, 6)
                .part("body", null, Role.BODY, skin).pivot(0, 11, 0).box(-6, -14, -4, 12, 14, 8).pattern("scales")
                .part("belly", "body", Role.STATIC, horn).pivot(0, 0, 0).box(-4, -9, -4.5f, 8, 8, 1).pattern("plates")
                .part("head", "body", Role.HEAD, skin).pivot(0, -14, -1).box(-3.5f, -7, -4, 7, 7, 7).pattern("eyes")
                .part("horns", "head", Role.STATIC, horn).pivot(0, 0, 0).box(-5, -11, -1, 2, 6, 2).box(3, -11, -1, 2, 6, 2)
                .part("jaw", "head", Role.JAW, skin).pivot(0, -1, -3).box(-3, 0, -3, 6, 2, 4)
                .part("arm_left", "body", Role.ARM_LEFT, skin).pivot(8, -12, 0).box(-2, -2, -2.5f, 5, 14, 5)
                .part("claw_left", "arm_left", Role.STATIC, horn).pivot(0, 12, 0).box(-2, 0, -3, 5, 3, 2)
                .part("arm_right", "body", Role.ARM_RIGHT, skin).pivot(-8, -12, 0).box(-3, -2, -2.5f, 5, 14, 5)
                .part("claw_right", "arm_right", Role.STATIC, horn).pivot(0, 12, 0).box(-3, 0, -3, 5, 3, 2);
        if (tail) {
            b.part("tail", "body", Role.TAIL, skin).pivot(0, -1, 4).rotate(-25, 0, 0).box(-1.5f, -1.5f, 0, 3, 3, 10)
                    .part("tail_tip", "tail", Role.TAIL, skin).pivot(0, 0, 10).rotate(20, 0, 0).box(-1, -1, 0, 2, 2, 8)
                    .part("tail_spade", "tail_tip", Role.STATIC, horn).pivot(0, 0, 8).box(-2.5f, -0.5f, 0, 5, 1, 4);
        }
        return b;
    }

    /** K'ril Tsutsaroth: a towering red demon general with great wings, a whip-like tail and a chain mace. */
    private static ModelSpec kril() {
        int skin = 0x8A1A1A;
        int horn = 0x2A1A14;
        ModelSpec.Builder b = demon("kril_tsutsaroth", skin, horn, 1.0f, true)
                .part("spikes", "body", Role.STATIC, horn).pivot(0, 0, 0).box(-1, -15, 3.5f, 2, 12, 2)
                .part("shoulder_spikes", "body", Role.STATIC, horn).pivot(0, 0, 0).box(-8, -16, -1, 2, 3, 2).box(6, -16, -1, 2, 3, 2)
                .part("chain", "arm_right", Role.STATIC, 0x7A7A7A).pivot(0, 12, 0).box(-1, 0, -1, 1, 8, 1)
                .part("mace", "chain", Role.STATIC, 0x5A5A5A).pivot(0, 8, 0).box(-2.5f, 0, -2.5f, 4, 4, 4).pattern("plates");
        return ModelKit.membraneWings(b, "body", -11, 3, 0x5A1010, 0x3A0A0A, 20).build();
    }

    /** Starlight: a radiant armoured knight with a glowing blade. */
    private static ModelSpec starlight() {
        return ModelKit.humanoid("starlight", 0xF0E8D0, 0xE8E8F0, 0xD8D8E0, "eyes")
                .part("helm", "head", Role.STATIC, 0xF0F0F8).pivot(0, 0, 0).box(-4.5f, -8.5f, -4.5f, 9, 4, 9).box(-0.5f, -12, -2, 1, 4, 6)
                .pattern("plates")
                .part("halo", "head", Role.SPIN, 0xF8E880).pivot(0, -11, 0).box(-4, 0, -4, 8, 1, 1).box(-4, 0, 3, 8, 1, 1)
                .box(-4, 0, -3, 1, 1, 6).box(3, 0, -3, 1, 1, 6).pattern("glow")
                .part("blade", "arm_right", Role.STATIC, 0xF8F8B0).pivot(-1, 9, 0).box(-0.5f, 0, -13, 1, 2, 12).pattern("glow")
                .part("shield", "arm_left", Role.STATIC, 0x3A5AD0).pivot(3, 4, 0).box(0, -2, -4, 1, 9, 8).pattern("plates")
                .build();
    }

    /** Growler: a great maned lion. */
    private static ModelSpec growler() {
        int fur = 0xC8A050;
        return ModelSpec.builder("growler").shadow(0.8f)
                .part("body", null, Role.BODY, fur).pivot(0, 13, 0).box(-5, -4, -9, 10, 8, 17)
                .part("mane", "body", Role.STATIC, 0x8A5A20).pivot(0, 0, 0).box(-6, -6, -11, 12, 11, 6).pattern("stripes")
                .part("head", "body", Role.HEAD, fur).pivot(0, -2, -11).box(-4, -4, -5, 8, 7, 5).pattern("eyes")
                .part("snout", "head", Role.STATIC, 0xE0C080).pivot(0, 0, -5).box(-2, -1, -3, 4, 3, 3)
                .part("jaw", "head", Role.JAW, 0xE0C080).pivot(0, 3, -5).box(-2, 0, -3, 4, 1, 3)
                .part("leg_fl", "body", Role.LEG_FRONT_LEFT, fur).pivot(3, 3, -6).box(-1.5f, 0, -1.5f, 3, 8, 3)
                .part("leg_fr", "body", Role.LEG_FRONT_RIGHT, fur).pivot(-3, 3, -6).box(-1.5f, 0, -1.5f, 3, 8, 3)
                .part("leg_bl", "body", Role.LEG_BACK_LEFT, fur).pivot(3, 3, 6).box(-1.5f, 0, -1.5f, 3, 8, 3)
                .part("leg_br", "body", Role.LEG_BACK_RIGHT, fur).pivot(-3, 3, 6).box(-1.5f, 0, -1.5f, 3, 8, 3)
                .part("tail", "body", Role.TAIL, fur).pivot(0, -3, 8).rotate(20, 0, 0).box(-0.5f, -0.5f, 0, 1, 1, 10)
                .part("tuft", "tail", Role.STATIC, 0x8A5A20).pivot(0, 0, 10).box(-1, -1, 0, 2, 2, 2)
                .build();
    }

    /** Bree: a centaur archer. */
    private static ModelSpec bree() {
        int coat = 0x8A6A4A;
        return ModelSpec.builder("bree").shadow(0.8f)
                .part("body", null, Role.BODY, coat).pivot(0, 12, 0).box(-4, -4, -10, 8, 8, 16)
                .part("leg_fl", "body", Role.LEG_FRONT_LEFT, coat).pivot(2.5f, 4, -8).box(-1.5f, 0, -1.5f, 3, 8, 3)
                .part("leg_fr", "body", Role.LEG_FRONT_RIGHT, coat).pivot(-2.5f, 4, -8).box(-1.5f, 0, -1.5f, 3, 8, 3)
                .part("leg_bl", "body", Role.LEG_BACK_LEFT, coat).pivot(2.5f, 4, 4).box(-1.5f, 0, -1.5f, 3, 8, 3)
                .part("leg_br", "body", Role.LEG_BACK_RIGHT, coat).pivot(-2.5f, 4, 4).box(-1.5f, 0, -1.5f, 3, 8, 3)
                .part("tail", "body", Role.TAIL, 0x3A2A1A).pivot(0, -3, 6).rotate(-30, 0, 0).box(-1, 0, 0, 2, 9, 2)
                .part("torso", "body", Role.STATIC, 0xE8E8F0).pivot(0, -4, -8).box(-3.5f, -11, -2, 7, 11, 4).pattern("plates")
                .part("head", "torso", Role.HEAD, 0xD8B890).pivot(0, -11, 0).box(-3, -6, -3, 6, 6, 6).pattern("eyes")
                .part("hair", "head", Role.STATIC, 0x5A3A1A).pivot(0, 0, 0).box(-3.5f, -6.5f, -3.5f, 7, 2, 7).box(-3, -5, 2.5f, 6, 8, 1)
                .part("arm_left", "torso", Role.ARM_LEFT, 0xD8B890).pivot(4.5f, -10, 0).box(-1, -1, -1, 2, 10, 2)
                .part("arm_right", "torso", Role.ARM_RIGHT, 0xD8B890).pivot(-4.5f, -10, 0).box(-1, -1, -1, 2, 10, 2)
                .part("bow", "arm_left", Role.STATIC, 0x6A4A2A).pivot(0, 8, -1).box(-0.5f, -7, -1, 1, 14, 1)
                .build();
    }

    /** One of Graardor's goblin sergeants with a weapon. */
    private static ModelSpec sergeant(String id, int skin, int armour, int weapon, String kind) {
        ModelSpec.Builder b = ModelKit.goblin(id, skin, armour, 0x3A2A1A, 1.0f)
                .part("helmet", "head", Role.STATIC, 0x7A7A7A).pivot(0, 0, 0).box(-3.5f, -7, -3.5f, 7, 2, 7);
        switch (kind) {
            case "mace" -> b.part("weapon", "arm_right", Role.STATIC, 0x5A4A3A).pivot(0, 6, 0).box(-0.5f, 0, -7, 1, 1, 8)
                    .part("weapon_head", "weapon", Role.STATIC, weapon).pivot(0, 0, -7).box(-2, -2, -4, 4, 4, 4);
            case "staff" -> b.part("weapon", "arm_right", Role.STATIC, 0x5A4A3A).pivot(0, 6, 0).box(-0.5f, -10, -0.5f, 1, 16, 1)
                    .part("weapon_head", "weapon", Role.SPIN, weapon).pivot(0, -11, 0).box(-1.5f, -1.5f, -1.5f, 3, 3, 3).pattern("glow");
            default -> b.part("weapon", "arm_left", Role.STATIC, weapon).pivot(0, 6, -1).box(-0.5f, -6, -1, 1, 12, 1);
        }
        return b.build();
    }

    /** Blood reaver: a bloated crimson beast. */
    private static ModelSpec bloodReaver() {
        return ModelSpec.builder("blood_reaver").shadow(0.7f)
                .part("body", null, Role.BODY, 0x7A1010).pivot(0, 14, 0).box(-5, -6, -6, 10, 9, 12).pattern("spots")
                .part("head", "body", Role.HEAD, 0x9A2020).pivot(0, -3, -6).box(-3.5f, -3, -5, 7, 6, 5).pattern("eyes")
                .part("jaw", "head", Role.JAW, 0x5A0808).pivot(0, 2, -4).box(-3, 0, -2, 6, 2, 3)
                .part("leg_fl", "body", Role.LEG_FRONT_LEFT, 0x5A0808).pivot(4, 2, -4).box(-1.5f, 0, -1.5f, 3, 8, 3)
                .part("leg_fr", "body", Role.LEG_FRONT_RIGHT, 0x5A0808).pivot(-4, 2, -4).box(-1.5f, 0, -1.5f, 3, 8, 3)
                .part("leg_bl", "body", Role.LEG_BACK_LEFT, 0x5A0808).pivot(4, 2, 4).box(-1.5f, 0, -1.5f, 3, 8, 3)
                .part("leg_br", "body", Role.LEG_BACK_RIGHT, 0x5A0808).pivot(-4, 2, 4).box(-1.5f, 0, -1.5f, 3, 8, 3)
                .part("tendril", "body", Role.TAIL, 0x9A2020).pivot(0, -4, 6).rotate(30, 0, 0).box(-1, -1, 0, 2, 2, 7)
                .build();
    }
}
