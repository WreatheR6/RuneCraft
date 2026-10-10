package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bosses.ModelKit.chainDown;
import static com.runecraft.osrs.content.bosses.ModelKit.chainUp;
import static com.runecraft.osrs.content.bosses.ModelKit.chainZ;
import static com.runecraft.osrs.content.bosses.ModelKit.legs2;
import static com.runecraft.osrs.content.bosses.ModelKit.legs4;
import static com.runecraft.osrs.content.bosses.ModelKit.spiderLegs;
import static com.runecraft.osrs.content.bosses.ModelKit.wings;

/**
 * Custom models for every boss in this module. Each spec has the same id as its boss so the
 * renderer picks it up automatically. Models are authored around 1-3 blocks tall; the BossDef's
 * render scale makes them boss-sized.
 */
public final class BossModels {
    private BossModels() {
    }

    public static List<ModelSpec> all() {
        List<ModelSpec> list = new ArrayList<>(List.of(
                giantMole(), kalphiteQueen(), kalphiteQueenAirborne(),
                dagannoth("dagannoth_rex", 0x6A6A40, 0xA0A070, 0x8A8A50),
                dagannoth("dagannoth_prime", 0x4A6A8A, 0x9AB0C8, 0x2A4A6A),
                dagannoth("dagannoth_supreme", 0x8A4A3A, 0xD0B0A0, 0xA03020),
                sarachnis(),
                zulrah("zulrah", 0x3A8A3A, 0x9AD06A, 0x206A20),
                zulrah("zulrah_magma", 0xA02818, 0xE07030, 0x601010),
                zulrah("zulrah_tanzanite", 0x2A4A9A, 0x40C0C0, 0x1A2A6A),
                vorkath(), kraken(), thermy(), cerberus(), abyssalSire(),
                hydra("alchemical_hydra", 0x3A7A3A, 0x8AC060, 4),
                hydra("alchemical_hydra_blue", 0x2A4A9A, 0x70A0E0, 3),
                hydra("alchemical_hydra_red", 0x9A2A2A, 0xE07050, 2),
                hydra("alchemical_hydra_enraged", 0x3A3A3A, 0xC0C0C0, 1),
                gargoyle("dusk", 0x3A3A4A, 0x6A6AA0, false),
                gargoyle("dawn", 0xA09A8A, 0xE0D8B0, true),
                corp(), skotizo(), hespori(),
                hunllef("crystalline_hunllef", 0x7AC0E0, 0xD0F0FF),
                hunllef("corrupted_hunllef", 0x9A2A2A, 0xF06040),
                nightmare("the_nightmare", 0x2A2A3A, 0x9A4AC0),
                nightmare("phosanis_nightmare", 0x3A2A2A, 0xC04A4A),
                muspah(), duke(), leviathan(), whisperer(), vardorvis(), scurrius(), araxxor(), hueycoatl(), amoxliatl(),
                titan("branda_the_fire_queen", 0xC0502A, 0xFFB040, 0xE0C040),
                titan("eldric_the_ice_king", 0x6A9AC8, 0xD0F0FF, 0xC0C0D0),
                zalcano(), mimic(), solHeredit()));
        list.addAll(MinionModels.all());
        return list;
    }

    static ModelSpec giantMole() {
        ModelSpec.Builder b = ModelSpec.builder("giant_mole").shadow(1.2f);
        b.part("body", null, Role.BODY, 0x3A2A20).pivot(0, 15, 0).box(-9, -7, -12, 18, 14, 24).box(-8, -6, 10, 16, 11, 5).pattern("spots");
        b.part("head", "body", Role.HEAD, 0x4A3426).pivot(0, 0, -12).box(-6, -5, -8, 12, 10, 8).pattern("eyes");
        b.part("snout", "head", Role.STATIC, 0xD89090).pivot(0, 1, -8).box(-3, -2, -5, 6, 5, 5).box(-1.5f, -1, -6, 3, 3, 1);
        b.part("jaw", "head", Role.JAW, 0xB07070).pivot(0, 4, -6).box(-3, 0, -6, 6, 2, 6);
        b.part("whiskers", "snout", Role.STATIC, 0xE8E0D0).pivot(0, 0, -3).box(-7, 0, 0, 14, 1, 1).box(-6, 2, 0, 12, 1, 1);
        b.part("claw_fl", null, Role.LEG_FRONT_LEFT, 0x4A3426).pivot(7, 17, -8).box(-2, 0, -3, 5, 7, 5);
        b.part("blade_fl", "claw_fl", Role.STATIC, 0xE0D0B0).pivot(0.5f, 6, -1).box(-3, -1, -5, 7, 2, 6).pattern("stripes");
        b.part("claw_fr", null, Role.LEG_FRONT_RIGHT, 0x4A3426).pivot(-7, 17, -8).box(-3, 0, -3, 5, 7, 5);
        b.part("blade_fr", "claw_fr", Role.STATIC, 0xE0D0B0).pivot(-0.5f, 6, -1).box(-4, -1, -5, 7, 2, 6).pattern("stripes");
        b.part("leg_bl", null, Role.LEG_BACK_LEFT, 0x4A3426).pivot(7, 18, 8).box(-2, 0, -2, 4, 6, 5);
        b.part("leg_br", null, Role.LEG_BACK_RIGHT, 0x4A3426).pivot(-7, 18, 8).box(-2, 0, -2, 4, 6, 5);
        b.part("tail", "body", Role.TAIL, 0xD89090).pivot(0, -2, 15).box(-1, -1, 0, 2, 2, 7);
        b.part("fur", "body", Role.STATIC, 0x2A1E16).pivot(0, -7, 0).box(-6, -1, -10, 12, 1, 8).box(-7, -1, 0, 14, 1, 9);
        return b.build();
    }

    static ModelSpec kalphiteQueen() {
        int shell = 0x6A7A3A;
        int pale = 0xB0A050;
        ModelSpec.Builder b = ModelSpec.builder("kalphite_queen").shadow(1.4f);
        b.part("thorax", null, Role.BODY, shell, pale).pivot(0, 12, -4).box(-7, -5, -8, 14, 10, 14).box(-7.5f, -6.5f, -7, 15, 2, 12)
                .pattern("plates");
        b.part("abdomen", "thorax", Role.STATIC, 0x7A8A40, pale).pivot(0, 0, 6).rotate(-10, 0, 0).box(-9, -6, 0, 18, 12, 18)
                .pattern("stripes");
        b.part("abdomen_tip", "abdomen", Role.TAIL, 0x7A8A40, pale).pivot(0, 1, 18).box(-6, -4, 0, 12, 8, 7).box(-1, -2, 7, 2, 2, 4)
                .pattern("stripes");
        b.part("spikes", "thorax", Role.STATIC, pale).pivot(0, -6, 0).box(-5, -4, -5, 2, 4, 2).box(3, -4, -5, 2, 4, 2).box(-1, -5, 1, 2, 5, 2);
        b.part("head", "thorax", Role.HEAD, 0x5A6A30, 0xE0D040).pivot(0, -1, -8).box(-5, -4, -8, 10, 8, 8).pattern("eyes");
        b.part("mandible_l", "head", Role.JAW, pale).pivot(3, 3, -8).rotate(0, 20, 0).box(-1, -1, -7, 2, 2, 7);
        b.part("mandible_r", "head", Role.JAW, pale).pivot(-3, 3, -8).rotate(0, -20, 0).box(-1, -1, -7, 2, 2, 7);
        b.part("antenna_l", "head", Role.STATIC, pale).pivot(2, -4, -6).rotate(-40, -15, 0).box(-0.5f, -11, -0.5f, 1, 11, 1);
        b.part("antenna_r", "head", Role.STATIC, pale).pivot(-2, -4, -6).rotate(-40, 15, 0).box(-0.5f, -11, -0.5f, 1, 11, 1);
        spiderLegs(b, "leg", "thorax", shell, 3, 6, 3, -6, 5, 9, 6, 2);
        return b.build();
    }

    static ModelSpec kalphiteQueenAirborne() {
        int shell = 0x6A7A3A;
        int pale = 0xB0A050;
        ModelSpec.Builder b = ModelSpec.builder("kalphite_queen_airborne").shadow(1.4f);
        b.part("thorax", null, Role.BODY, shell, pale).pivot(0, 11, 0).box(-6, -16, -5, 12, 17, 10).pattern("plates");
        b.part("abdomen", "thorax", Role.STATIC, 0x7A8A40, pale).pivot(0, 0, 4).rotate(35, 0, 0).box(-8, -6, 0, 16, 12, 18).pattern("stripes");
        b.part("head", "thorax", Role.HEAD, 0x5A6A30, 0xE0D040).pivot(0, -16, -1).box(-5, -8, -7, 10, 8, 9).pattern("eyes");
        b.part("crest", "head", Role.STATIC, pale).pivot(0, -8, 0).box(-4, -4, -2, 2, 4, 2).box(2, -4, -2, 2, 4, 2);
        b.part("mandible_l", "head", Role.JAW, pale).pivot(3, -1, -7).rotate(0, 20, 0).box(-1, -1, -6, 2, 2, 6);
        b.part("mandible_r", "head", Role.JAW, pale).pivot(-3, -1, -7).rotate(0, -20, 0).box(-1, -1, -6, 2, 2, 6);
        b.part("antenna_l", "head", Role.STATIC, pale).pivot(2, -8, -5).rotate(-30, -15, 0).box(-0.5f, -10, -0.5f, 1, 10, 1);
        b.part("antenna_r", "head", Role.STATIC, pale).pivot(-2, -8, -5).rotate(-30, 15, 0).box(-0.5f, -10, -0.5f, 1, 10, 1);
        b.part("wing_l", "thorax", Role.WING_LEFT, 0xC0D0B0, 0x8A9A70).pivot(4, -14, 5).rotate(0, -25, -20).box(0, -0.5f, 0, 24, 1, 12)
                .pattern("stripes");
        b.part("wing_r", "thorax", Role.WING_RIGHT, 0xC0D0B0, 0x8A9A70).pivot(-4, -14, 5).rotate(0, 25, 20).box(-24, -0.5f, 0, 24, 1, 12)
                .pattern("stripes");
        b.part("arm_l", "thorax", Role.ARM_LEFT, shell).pivot(6, -13, -3).box(-1.5f, 0, -1.5f, 3, 11, 3);
        b.part("scythe_l", "arm_l", Role.STATIC, pale).pivot(0, 11, 0).rotate(-70, 0, 0).box(-1, 0, -1.5f, 2, 13, 3);
        b.part("arm_r", "thorax", Role.ARM_RIGHT, shell).pivot(-6, -13, -3).box(-1.5f, 0, -1.5f, 3, 11, 3);
        b.part("scythe_r", "arm_r", Role.STATIC, pale).pivot(0, 11, 0).rotate(-70, 0, 0).box(-1, 0, -1.5f, 2, 13, 3);
        spiderLegs(b, "leg", "thorax", shell, 2, 5, 0, -2, 5, 9, 8, 2);
        return b.build();
    }

    static ModelSpec dagannoth(String id, int color, int belly, int accent) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.0f);
        b.part("body", null, Role.BODY, color, accent).pivot(0, 11, 0).rotate(20, 0, 0).box(-6, -10, -4, 12, 15, 9).pattern("scales");
        b.part("belly", "body", Role.STATIC, belly).pivot(0, 0, -4).box(-4, -8, -1.5f, 8, 12, 2);
        b.part("neck", "body", Role.NECK, color, accent).pivot(0, -10, -1).rotate(-25, 0, 0).box(-3, -5, -3, 6, 6, 6).pattern("scales");
        b.part("head", "neck", Role.HEAD, color, 0xE0E040).pivot(0, -5, -1).box(-4, -4, -10, 8, 6, 10).pattern("eyes");
        b.part("crest", "head", Role.STATIC, accent).pivot(0, -4, -2).box(-0.5f, -4, -6, 1, 4, 9).box(-3, -3, -1, 1, 3, 3)
                .box(2, -3, -1, 1, 3, 3);
        b.part("jaw", "head", Role.JAW, belly).pivot(0, 2, -1).box(-3.5f, 0, -9, 7, 2, 9).pattern("stripes");
        b.part("fin", "body", Role.FIN, accent).pivot(0, -8, 4).box(-0.5f, -3, 0, 1, 10, 5);
        b.part("arm_l", "body", Role.ARM_LEFT, color).pivot(6, -7, -1).rotate(-20, 0, 0).box(0, -1, -1, 2, 8, 2).box(0, 7, -2, 2, 2, 3);
        b.part("arm_r", "body", Role.ARM_RIGHT, color).pivot(-6, -7, -1).rotate(-20, 0, 0).box(-2, -1, -1, 2, 8, 2).box(-2, 7, -2, 2, 2, 3);
        chainZ(b, "tail", "body", Role.TAIL, color, 4, 0, 3, 4, 1, 6, 6, 6, 10, "scales");
        legs2(b, "leg", color, 3.5f, 2, 11, 4);
        return b.build();
    }

    static ModelSpec sarachnis() {
        int shell = 0x8A2A1A;
        int dark = 0x2A1A1A;
        ModelSpec.Builder b = ModelSpec.builder("sarachnis").shadow(1.3f);
        b.part("body", null, Role.BODY, shell, dark).pivot(0, 13, -3).box(-7, -4, -7, 14, 8, 12).pattern("plates");
        b.part("abdomen", "body", Role.STATIC, shell, 0xE0A040).pivot(0, -1, 5).rotate(-15, 0, 0).box(-9, -7, 0, 18, 13, 16)
                .pattern("spots");
        b.part("spinnerets", "abdomen", Role.TAIL, dark).pivot(0, 0, 16).box(-2, -2, 0, 4, 4, 3);
        b.part("head", "body", Role.HEAD, dark, 0xE04020).pivot(0, 0, -7).box(-4, -3, -5, 8, 6, 5).pattern("eyes");
        b.part("fang_l", "head", Role.JAW, 0xE0D0B0).pivot(2, 3, -5).box(-1, 0, -1, 2, 4, 2);
        b.part("fang_r", "head", Role.JAW, 0xE0D0B0).pivot(-2, 3, -5).box(-1, 0, -1, 2, 4, 2);
        b.part("palps", "head", Role.STATIC, shell).pivot(0, 1, -5).box(3, 0, -4, 1, 1, 4).box(-4, 0, -4, 1, 1, 4);
        spiderLegs(b, "leg", "body", shell, 4, 6, 2, -5, 3, 9, 7, 2);
        return b.build();
    }

    static ModelSpec zulrah(String id, int color, int belly, int dark) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.3f);
        b.part("coils", null, Role.STATIC, color, dark).pivot(0, 22, 0).box(-10, -2, -10, 20, 4, 20).box(-8, -5, -8, 16, 3, 16)
                .pattern("scales");
        b.part("body", null, Role.BODY, color, dark).pivot(0, 17, 0).box(-5, -6, -5, 10, 8, 10).pattern("scales");
        b.part("belly", "body", Role.STATIC, belly).pivot(0, -2, -5).box(-3, -4, -1, 6, 6, 1);
        String neck = chainUp(b, "neck", "body", Role.NECK, color, 4, 0, -6, 0, 9, 8, 6, 0, "scales");
        b.part("head", neck, Role.HEAD, color, 0xF0E040).pivot(0, -8, 0).rotate(18, 0, 0).box(-5, -5, -10, 10, 6, 12).pattern("eyes");
        b.part("hood", "head", Role.FIN, belly, dark).pivot(0, -2, 1).box(-9, -7, 0, 18, 11, 1).pattern("stripes");
        b.part("horns", "head", Role.STATIC, dark).pivot(0, -5, 0).box(-4, -3, -2, 2, 3, 2).box(2, -3, -2, 2, 3, 2);
        b.part("jaw", "head", Role.JAW, belly).pivot(0, 1, 0).box(-4, 0, -9, 8, 2, 9);
        b.part("fangs", "jaw", Role.STATIC, 0xF0F0E0).pivot(0, 0, -8).box(-3, -2, -1, 1, 2, 1).box(2, -2, -1, 1, 2, 1);
        b.part("fins", "neck1", Role.FIN, belly).pivot(0, -4, 3).box(-0.5f, -6, 0, 1, 8, 4);
        chainZ(b, "tail", "coils", Role.TAIL, color, 5, 7, -1, 7, 1, 6, 4, 5, -6, "scales");
        return b.build();
    }

    static ModelSpec vorkath() {
        int bone = 0xC8C0A8;
        int dark = 0x3A3A4A;
        int glow = 0x6AB0E0;
        ModelSpec.Builder b = ModelSpec.builder("vorkath").shadow(1.6f);
        b.part("torso", null, Role.BODY, bone, dark).pivot(0, 12, 0).box(-7, -6, -10, 14, 11, 20).pattern("plates");
        b.part("ribs", "torso", Role.STATIC, 0xE0D8C0).pivot(0, 0, 0).box(-8, -5, -7, 16, 9, 2).box(-8, -5, -2, 16, 9, 2)
                .box(-8, -5, 3, 16, 9, 2);
        b.part("spine", "torso", Role.STATIC, dark, glow).pivot(0, -6, 0).box(-1, -3, -10, 2, 3, 20).pattern("glow");
        String neck = chainZ(b, "neck", "torso", Role.NECK, bone, 3, 0, -3, -10, -1, 6, 6, 6, -12, "plates");
        b.part("skull", neck, Role.HEAD, bone, glow).pivot(0, 0, -6).rotate(30, 0, 0).box(-5, -4, -10, 10, 7, 10).pattern("eyes");
        b.part("horns", "skull", Role.STATIC, dark).pivot(0, -3, 0).rotate(-30, 0, 0).box(-5, -2, 0, 2, 2, 9).box(3, -2, 0, 2, 2, 9);
        b.part("jaw", "skull", Role.JAW, bone).pivot(0, 3, -1).box(-4, 0, -9, 8, 2, 9).pattern("stripes");
        wings(b, "torso", bone, 0x5A6A7A, 6, -5, -4, 18, 12);
        legs4(b, "leg", bone, 6, -7, 7, 13, 4);
        chainZ(b, "tail", "torso", Role.TAIL, bone, 4, 0, -1, 10, 1, 5, 5, 6, -4, "plates");
        b.part("frost", "torso", Role.FLOAT, glow).pivot(0, -10, 0).box(-1, -1, -6, 2, 2, 2).box(-1, -1, 4, 2, 2, 2);
        return b.build();
    }

    static ModelSpec kraken() {
        int skin = 0x8A3A6A;
        int pale = 0xC07090;
        ModelSpec.Builder b = ModelSpec.builder("kraken").shadow(1.8f);
        b.part("mantle", null, Role.BODY, skin, pale).pivot(0, 14, 0).box(-9, -12, -8, 18, 16, 16).box(-7, -20, -6, 14, 8, 12)
                .pattern("spots");
        b.part("eyes", "mantle", Role.HEAD, 0xE0D040, 0x202020).pivot(0, -2, -8).box(-8, -3, -1, 16, 6, 2).pattern("eyes");
        b.part("beak", "mantle", Role.JAW, 0x3A2A1A).pivot(0, 3, -8).box(-2, 0, -3, 4, 3, 3);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            float x = (float) Math.sin(a) * 7;
            float z = (float) Math.cos(a) * 7;
            String t = "tentacle" + i;
            b.part(t, "mantle", Role.TENTACLE, skin, pale).pivot(x, 3, z).rotate(55, (float) Math.toDegrees(a), 0)
                    .box(-1.5f, 0, -1.5f, 3, 9, 3).pattern("spots");
            b.part(t + "_tip", t, Role.TENTACLE, skin, pale).pivot(0, 9, 0).rotate(30, 0, 0).box(-1, 0, -1, 2, 8, 2);
        }
        return b.build();
    }

    static ModelSpec thermy() {
        int smoke = 0x4A4A4A;
        ModelSpec.Builder b = ModelSpec.builder("thermonuclear_smoke_devil").shadow(1.0f);
        b.part("body", null, Role.FLOAT, smoke, 0xE07020).pivot(0, 10, 0).box(-6, -6, -5, 12, 12, 10).pattern("spots");
        b.part("head", "body", Role.HEAD, 0x5A5A5A, 0xFF8020).pivot(0, -6, 0).box(-5, -8, -5, 10, 8, 9).pattern("eyes");
        b.part("horns", "head", Role.STATIC, 0x2A2A2A).pivot(0, -8, 0).rotate(-20, 0, 0).box(-5, -5, -1, 2, 5, 2).box(3, -5, -1, 2, 5, 2);
        b.part("jaw", "head", Role.JAW, 0x3A3A3A).pivot(0, -1, -5).box(-3, 0, -1, 6, 2, 2);
        b.part("arm_l", "body", Role.ARM_LEFT, smoke).pivot(6, -4, 0).box(0, -1, -1.5f, 3, 10, 3);
        b.part("arm_r", "body", Role.ARM_RIGHT, smoke).pivot(-6, -4, 0).box(-3, -1, -1.5f, 3, 10, 3);
        chainDown(b, "smoke_tail", "body", Role.TAIL, 0x6A6A6A, 3, 0, 6, 0, 8, 3, 0, 0, "none");
        b.part("puffs", "body", Role.SPIN, 0x8A8A8A).pivot(0, -2, 0).box(-11, -2, -2, 4, 4, 4).box(7, 0, -2, 4, 4, 4)
                .box(-2, -4, 7, 4, 4, 4).box(-2, 2, -11, 4, 4, 4);
        return b.build();
    }

    static ModelSpec cerberus() {
        int fur = 0x3A2A2A;
        int lava = 0xE05010;
        ModelSpec.Builder b = ModelSpec.builder("cerberus").shadow(1.6f);
        b.part("torso", null, Role.BODY, fur, lava).pivot(0, 10, 0).box(-7, -6, -11, 14, 12, 22).pattern("stripes");
        b.part("spikes", "torso", Role.STATIC, lava).pivot(0, -6, 0).box(-1, -3, -10, 2, 3, 18).pattern("glow");
        for (int i = -1; i <= 1; i++) {
            String n = "neck" + (i + 1);
            b.part(n, "torso", Role.NECK, fur, lava).pivot(i * 5, -3, -10).rotate(-20, -i * 25, 0).box(-2.5f, -3, -6, 5, 5, 6)
                    .box(-3, -3.5f, -3, 6, 1, 2).pattern("stripes");
            String h = "head" + (i + 1);
            b.part(h, n, Role.HEAD, fur, 0xFF6020).pivot(0, -1, -6).box(-3.5f, -3.5f, -7, 7, 6, 7).box(-2, -1, -11, 4, 3, 4)
                    .box(-3, -6, -3, 2, 3, 2).box(1, -6, -3, 2, 3, 2).pattern("eyes");
            b.part("jaw" + (i + 1), h, Role.JAW, 0x5A2A2A).pivot(0, 2, -5).box(-2, 0, -6, 4, 2, 6);
        }
        legs4(b, "leg", fur, 5, -8, 8, 10, 4);
        chainZ(b, "tail", "torso", Role.TAIL, fur, 3, 0, -4, 11, 1, 3, 3, 6, -15, "none");
        return b.build();
    }

    static ModelSpec abyssalSire() {
        int flesh = 0x6A1A3A;
        int pale = 0xA04A6A;
        ModelSpec.Builder b = ModelSpec.builder("abyssal_sire").shadow(2.0f);
        b.part("body", null, Role.BODY, flesh, pale).pivot(0, 12, 0).box(-11, -12, -8, 22, 22, 16).pattern("spots");
        b.part("belly", "body", Role.STATIC, pale).pivot(0, 0, -8).box(-8, -8, -1, 16, 14, 1).pattern("stripes");
        b.part("head", "body", Role.HEAD, flesh, 0xF04040).pivot(0, -12, -2).box(-6, -9, -6, 12, 9, 10).pattern("eyes");
        b.part("horns", "head", Role.STATIC, 0x2A1A2A).pivot(0, -9, 0).rotate(-25, 0, 0).box(-7, -6, -1, 2, 7, 2).box(5, -6, -1, 2, 7, 2);
        for (int i = 0; i < 4; i++) {
            b.part("face_tentacle" + i, "head", Role.TENTACLE, pale).pivot(-3 + i * 2, 0, -6).box(-0.5f, 0, -0.5f, 1, 7, 1);
        }
        b.part("arm_l", "body", Role.ARM_LEFT, flesh).pivot(11, -9, 0).box(0, -2, -3, 6, 16, 6).pattern("spots");
        b.part("claw_l", "arm_l", Role.TENTACLE, pale).pivot(3, 14, 0).box(-1, 0, -3, 2, 6, 2).box(-1, 0, 1, 2, 6, 2);
        b.part("arm_r", "body", Role.ARM_RIGHT, flesh).pivot(-11, -9, 0).box(-6, -2, -3, 6, 16, 6).pattern("spots");
        b.part("claw_r", "arm_r", Role.TENTACLE, pale).pivot(-3, 14, 0).box(-1, 0, -3, 2, 6, 2).box(-1, 0, 1, 2, 6, 2);
        for (int i = 0; i < 4; i++) {
            float yaw = 45 + i * 90;
            float x = (float) Math.sin(Math.toRadians(yaw)) * 9;
            float z = (float) Math.cos(Math.toRadians(yaw)) * 7;
            chainDown(b, "base_tentacle" + i + "_", "body", Role.TENTACLE, flesh, 2, x, 8, z, 4, 6, 60, yaw, "spots");
        }
        return b.build();
    }

    static ModelSpec hydra(String id, int color, int belly, int heads) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.6f);
        b.part("body", null, Role.BODY, color, belly).pivot(0, 12, 2).box(-9, -7, -10, 18, 13, 22).pattern("scales");
        b.part("belly", "body", Role.STATIC, belly).pivot(0, 6, 0).box(-7, -1, -8, 14, 1, 18);
        b.part("spines", "body", Role.STATIC, belly).pivot(0, -7, 0).box(-1, -3, -8, 2, 3, 6).box(-1, -3, 0, 2, 3, 6).box(-1, -2, 7, 2, 2, 4);
        legs4(b, "leg", color, 7, -5, 9, 8, 5);
        chainZ(b, "tail", "body", Role.TAIL, color, 4, 0, -1, 12, 1, 6, 5, 6, -6, "scales");
        for (int i = 0; i < heads; i++) {
            float x = heads == 1 ? 0 : -6 + 12f * i / (heads - 1);
            String n = chainUp(b, "neck" + i + "_", "body", Role.NECK, color, 2, x, -5, -9, 4, 7, 30, -x * 4, "scales");
            String h = "head" + i;
            b.part(h, n, Role.HEAD, color, 0xF0E040).pivot(0, -7, 0).rotate(50, 0, 0).box(-3, -3, -7, 6, 5, 7).box(-2, -5, -2, 1, 2, 2)
                    .box(1, -5, -2, 1, 2, 2).pattern("eyes");
            b.part("jaw" + i, h, Role.JAW, belly).pivot(0, 2, -1).box(-2.5f, 0, -6, 5, 1, 6);
        }
        return b.build();
    }

    static ModelSpec gargoyle(String id, int stone, int accent, boolean flier) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.0f);
        b.part("torso", null, flier ? Role.FLOAT : Role.BODY, stone, accent).pivot(0, 8, 0).box(-5, -8, -3, 10, 12, 6).pattern("plates");
        b.part("head", "torso", Role.HEAD, stone, accent).pivot(0, -8, 0).box(-4, -7, -4, 8, 7, 7).pattern("eyes");
        b.part("horns", "head", Role.STATIC, accent).pivot(0, -7, 0).rotate(-30, 0, 0).box(-4, -6, 0, 2, 6, 2).box(2, -6, 0, 2, 6, 2);
        b.part("jaw", "head", Role.JAW, stone).pivot(0, -1, -3).box(-3, 0, -2, 6, 2, 3);
        b.part("arm_l", "torso", Role.ARM_LEFT, stone).pivot(5, -6, 0).box(0, -1, -1.5f, 3, 11, 3).box(0, 10, -2.5f, 3, 2, 4);
        b.part("arm_r", "torso", Role.ARM_RIGHT, stone).pivot(-5, -6, 0).box(-3, -1, -1.5f, 3, 11, 3).box(-3, 10, -2.5f, 3, 2, 4);
        wings(b, "torso", stone, accent, 2, -6, 3, 16, 10);
        chainZ(b, "tail", "torso", Role.TAIL, stone, 3, 0, 3, 3, 1, 2, 2, 5, 20, "none");
        legs2(b, "leg", stone, 2.5f, 0, 12, 3);
        return b.build();
    }

    static ModelSpec corp() {
        int spirit = 0x9AB08A;
        int dark = 0x4A6A4A;
        ModelSpec.Builder b = ModelSpec.builder("corporeal_beast").shadow(1.8f);
        b.part("torso", null, Role.BODY, spirit, dark).pivot(0, 9, 0).box(-9, -8, -12, 18, 15, 22).box(-7, -12, -10, 14, 5, 12)
                .pattern("spots");
        b.part("head", "torso", Role.HEAD, spirit, 0xE0F0A0).pivot(0, -3, -12).box(-6, -6, -9, 12, 11, 9).pattern("eyes");
        b.part("horns", "head", Role.STATIC, 0xE0E0C0).pivot(0, -6, -3).rotate(-35, 0, 0).box(-7, -7, -1, 2, 7, 2).box(5, -7, -1, 2, 7, 2)
                .box(-8, -9, 0, 2, 3, 2).box(6, -9, 0, 2, 3, 2);
        b.part("jaw", "head", Role.JAW, dark).pivot(0, 4, -2).box(-5, 0, -8, 10, 3, 8).pattern("stripes");
        b.part("tusks", "jaw", Role.STATIC, 0xF0F0E0).pivot(0, 0, -7).box(-5, -4, -1, 1, 4, 1).box(4, -4, -1, 1, 4, 1);
        legs4(b, "leg", spirit, 7, -8, 7, 9, 5);
        chainZ(b, "tail", "torso", Role.TAIL, spirit, 3, 0, -4, 10, 1, 4, 4, 6, 15, "none");
        b.part("wisps", "torso", Role.SPIN, 0xD0F0C0).pivot(0, -14, 0).box(-10, 0, -1, 2, 2, 2).box(8, -2, 0, 2, 2, 2).box(0, -4, 8, 2, 2, 2);
        return b.build();
    }

    static ModelSpec skotizo() {
        int skin = 0x3A1A2A;
        int glow = 0xC02040;
        ModelSpec.Builder b = ModelSpec.builder("skotizo").shadow(1.4f);
        b.part("torso", null, Role.BODY, skin, glow).pivot(0, 6, 0).box(-7, -8, -4, 14, 14, 8).pattern("plates");
        b.part("head", "torso", Role.HEAD, skin, glow).pivot(0, -8, -1).box(-4, -8, -5, 8, 8, 8).pattern("eyes");
        b.part("horns", "head", Role.STATIC, 0x1A1A1A).pivot(0, -7, 0).box(-9, -3, -1, 5, 2, 2).box(4, -3, -1, 5, 2, 2)
                .box(-9, -8, -1, 2, 5, 2).box(7, -8, -1, 2, 5, 2);
        b.part("jaw", "head", Role.JAW, 0x2A1018).pivot(0, -1, -4).box(-3, 0, -1, 6, 2, 3);
        b.part("arm_l", "torso", Role.ARM_LEFT, skin).pivot(7, -7, 0).box(0, -1, -2, 4, 12, 4).box(0, 11, -3, 4, 2, 5);
        b.part("arm_r", "torso", Role.ARM_RIGHT, skin).pivot(-7, -7, 0).box(-4, -1, -2, 4, 12, 4).box(-4, 11, -3, 4, 2, 5);
        wings(b, "torso", 0x2A1018, glow, 3, -6, 3, 18, 12);
        chainZ(b, "tail", "torso", Role.TAIL, skin, 4, 0, 4, 4, 1, 3, 3, 5, 15, "none");
        legs2(b, "leg", skin, 3.5f, 0, 12, 4);
        return b.build();
    }

    static ModelSpec hespori() {
        int plant = 0x3A6A2A;
        int bloom = 0xC04A8A;
        ModelSpec.Builder b = ModelSpec.builder("hespori").shadow(1.6f);
        b.part("bulb", null, Role.BODY, plant, 0x6A9A3A).pivot(0, 16, 0).box(-9, -10, -9, 18, 14, 18).box(-6, -14, -6, 12, 4, 12)
                .pattern("spots");
        b.part("flower", "bulb", Role.HEAD, bloom, 0xF0E040).pivot(0, -14, 0).box(-5, -6, -5, 10, 6, 10).pattern("eyes");
        b.part("maw", "flower", Role.JAW, 0x6A1A3A).pivot(0, -1, -5).box(-4, 0, -1, 8, 3, 2);
        for (int i = 0; i < 4; i++) {
            float yaw = i * 90;
            b.part("petal" + i, "flower", Role.FIN, bloom, 0xF0A0C0).pivot(0, -5, 0).rotate(-60, yaw, 0).box(-3, -12, -0.5f, 6, 12, 1)
                    .pattern("stripes");
        }
        for (int i = 0; i < 6; i++) {
            float yaw = i * 60 + 30;
            float x = (float) Math.sin(Math.toRadians(yaw)) * 9;
            float z = (float) Math.cos(Math.toRadians(yaw)) * 9;
            chainDown(b, "vine" + i + "_", "bulb", Role.TENTACLE, 0x2A5A1A, 2, x, 0, z, 3, 7, 70, yaw, "none");
        }
        return b.build();
    }

    static ModelSpec hunllef(String id, int crystal, int light) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.5f);
        b.part("torso", null, Role.BODY, crystal, light).pivot(0, 9, 0).box(-8, -7, -10, 16, 13, 20).pattern("plates");
        b.part("crystals", "torso", Role.STATIC, light, crystal).pivot(0, -7, 0).box(-5, -6, -8, 3, 6, 3).box(2, -8, -4, 3, 8, 3)
                .box(-3, -7, 1, 3, 7, 3).box(3, -5, 5, 2, 5, 2).pattern("glow");
        b.part("head", "torso", Role.HEAD, crystal, 0xFFFFFF).pivot(0, -2, -10).box(-5, -5, -8, 10, 9, 8).pattern("eyes");
        b.part("head_crystals", "head", Role.STATIC, light).pivot(0, -5, -2).rotate(-20, 0, 0).box(-5, -5, 0, 2, 5, 2).box(3, -5, 0, 2, 5, 2);
        b.part("jaw", "head", Role.JAW, crystal).pivot(0, 3, -2).box(-4, 0, -7, 8, 2, 7).pattern("stripes");
        b.part("leg_fl", null, Role.LEG_FRONT_LEFT, crystal).pivot(7, 11, -7).box(-2.5f, 0, -2.5f, 5, 13, 5);
        b.part("leg_fr", null, Role.LEG_FRONT_RIGHT, crystal).pivot(-7, 11, -7).box(-2.5f, 0, -2.5f, 5, 13, 5);
        b.part("leg_bl", null, Role.LEG_BACK_LEFT, crystal).pivot(6, 14, 7).box(-2.5f, 0, -2.5f, 5, 10, 6);
        b.part("leg_br", null, Role.LEG_BACK_RIGHT, crystal).pivot(-6, 14, 7).box(-2.5f, 0, -2.5f, 5, 10, 6);
        String tail = chainZ(b, "tail", "torso", Role.TAIL, crystal, 4, 0, -2, 10, 1, 5, 5, 6, 8, "plates");
        b.part("tail_crystal", tail, Role.STATIC, light).pivot(0, 0, 6).box(-1.5f, -4, 0, 3, 6, 4).pattern("glow");
        return b.build();
    }

    static ModelSpec nightmare(String id, int robe, int glow) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.4f);
        b.part("robe", null, Role.FLOAT, robe, glow).pivot(0, 8, 0).box(-6, -12, -4, 12, 14, 8).box(-8, 2, -6, 16, 10, 12)
                .pattern("stripes");
        b.part("head", "robe", Role.HEAD, 0xD8D0E0, glow).pivot(0, -12, 0).box(-4, -8, -4, 8, 8, 8).pattern("eyes");
        b.part("hood", "head", Role.STATIC, robe).pivot(0, -8, 1).box(-5, -1, -4, 10, 9, 8, 0.25f);
        b.part("crown", "head", Role.STATIC, glow).pivot(0, -9, 0).box(-4, -3, -1, 1, 3, 1).box(-0.5f, -4, -1, 1, 4, 1)
                .box(3, -3, -1, 1, 3, 1).pattern("glow");
        b.part("arm_l", "robe", Role.ARM_LEFT, robe).pivot(6, -10, 0).box(0, -1, -1.5f, 3, 17, 3);
        b.part("claw_l", "arm_l", Role.STATIC, 0xD8D0E0).pivot(1.5f, 16, 0).box(-1.5f, 0, -2, 1, 6, 1).box(0, 0, -2, 1, 7, 1)
                .box(1, 0, -2, 1, 6, 1);
        b.part("arm_r", "robe", Role.ARM_RIGHT, robe).pivot(-6, -10, 0).box(-3, -1, -1.5f, 3, 17, 3);
        b.part("claw_r", "arm_r", Role.STATIC, 0xD8D0E0).pivot(-1.5f, 16, 0).box(-1.5f, 0, -2, 1, 6, 1).box(-0.5f, 0, -2, 1, 7, 1)
                .box(0.5f, 0, -2, 1, 6, 1);
        for (int i = 0; i < 4; i++) {
            chainDown(b, "tendril" + i + "_", "robe", Role.TENTACLE, robe, 2, -4.5f + i * 3, -8, 4, 2, 8, 30, 180, "none");
        }
        return b.build();
    }

    static ModelSpec muspah() {
        int body = 0x5A6A8A;
        int ice = 0xA0D0F0;
        ModelSpec.Builder b = ModelSpec.builder("phantom_muspah").shadow(1.4f);
        b.part("torso", null, Role.BODY, body, ice).pivot(0, 7, 0).box(-8, -10, -5, 16, 16, 10).pattern("plates");
        b.part("ice_spikes", "torso", Role.STATIC, ice, body).pivot(0, -10, 2).box(-8, -6, -1, 3, 6, 3).box(5, -7, -1, 3, 7, 3)
                .box(-1.5f, -5, 2, 3, 5, 3).box(-4, -3, 4, 2, 4, 2).pattern("glow");
        b.part("head", "torso", Role.HEAD, body, 0xE0F0FF).pivot(0, -10, -2).box(-5, -7, -6, 10, 8, 9).pattern("eyes");
        b.part("tusks", "head", Role.STATIC, 0xE0F0FF).pivot(0, 0, -6).rotate(-30, 0, 0).box(-5, -1, -4, 2, 2, 5).box(3, -1, -4, 2, 2, 5);
        b.part("jaw", "head", Role.JAW, 0x3A4A6A).pivot(0, 0, -4).box(-4, 0, -2, 8, 2, 3);
        b.part("arm_l", "torso", Role.ARM_LEFT, body).pivot(8, -8, 0).box(0, -2, -2.5f, 5, 15, 5).box(-0.5f, 13, -3, 6, 4, 6);
        b.part("arm_r", "torso", Role.ARM_RIGHT, body).pivot(-8, -8, 0).box(-5, -2, -2.5f, 5, 15, 5).box(-5.5f, 13, -3, 6, 4, 6);
        legs2(b, "leg", body, 4, 0, 11, 5);
        b.part("aura", "torso", Role.SPIN, ice).pivot(0, -4, 0).box(-12, 0, -1, 2, 2, 2).box(10, -3, 0, 2, 2, 2).box(0, 2, 10, 2, 2, 2);
        return b.build();
    }

    static ModelSpec duke() {
        int skin = 0x6A5A7A;
        int pale = 0x9A8AA8;
        ModelSpec.Builder b = ModelSpec.builder("duke_sucellus").shadow(1.8f);
        b.part("belly", null, Role.BODY, skin, pale).pivot(0, 12, 0).box(-12, -10, -10, 24, 20, 18).box(-10, -18, -7, 20, 8, 14)
                .pattern("spots");
        b.part("head", "belly", Role.HEAD, skin, 0xA0E040).pivot(0, -18, -3).box(-6, -9, -7, 12, 9, 10).box(-7, -10, -6, 14, 2, 4)
                .pattern("eyes");
        b.part("jaw", "head", Role.JAW, pale).pivot(0, 0, -5).box(-5, 0, -3, 10, 3, 5);
        b.part("arm_l", "belly", Role.ARM_LEFT, skin).pivot(12, -15, 0).box(0, -2, -3, 6, 22, 6).box(-1, 19, -4, 8, 4, 8);
        b.part("arm_r", "belly", Role.ARM_RIGHT, skin).pivot(-12, -15, 0).box(-6, -2, -3, 6, 22, 6).box(-7, 19, -4, 8, 4, 8);
        b.part("chains", null, Role.STATIC, 0x8A8A8A).pivot(0, 24, 0).box(-16, -10, -2, 1, 10, 1).box(15, -10, -2, 1, 10, 1)
                .box(-16, -1, -2, 32, 1, 1);
        b.part("hood", "head", Role.STATIC, 0x3A2A4A).pivot(0, -9, 1).box(-7, -2, -6, 14, 11, 10, 0.25f);
        for (int i = 0; i < 3; i++) {
            b.part("mouth_tendril" + i, "jaw", Role.TENTACLE, pale).pivot(-3 + i * 3, 3, -1).box(-0.5f, 0, -0.5f, 1, 6, 1);
        }
        b.part("shackles", "belly", Role.STATIC, 0x6A6A6A).pivot(0, 0, 0).box(11.5f, 4, -4, 8, 2, 8).box(-19.5f, 4, -4, 8, 2, 8);
        b.part("leg_l", null, Role.STATIC, skin).pivot(6, 20, -6).box(-4, -2, -8, 8, 6, 10);
        b.part("leg_r", null, Role.STATIC, skin).pivot(-6, 20, -6).box(-4, -2, -8, 8, 6, 10);
        return b.build();
    }

    static ModelSpec leviathan() {
        int shell = 0x2A5A6A;
        int flesh = 0x6A8A7A;
        ModelSpec.Builder b = ModelSpec.builder("the_leviathan").shadow(2.0f);
        b.part("shell", null, Role.BODY, shell, 0x60C0D0).pivot(0, 12, 2).box(-12, -10, -12, 24, 14, 26).pattern("plates");
        b.part("ridge", "shell", Role.STATIC, 0x1A3A4A, 0x60C0D0).pivot(0, -10, 0).box(-2, -5, -10, 4, 5, 22).box(-7, -3, -6, 2, 3, 2)
                .box(5, -3, -6, 2, 3, 2).box(-7, -3, 4, 2, 3, 2).box(5, -3, 4, 2, 3, 2).pattern("glow");
        b.part("head", "shell", Role.HEAD, flesh, 0x60F0F0).pivot(0, -1, -12).box(-6, -5, -10, 12, 9, 10).pattern("eyes");
        b.part("jaw", "head", Role.JAW, flesh).pivot(0, 3, -2).box(-5, 0, -8, 10, 3, 8).pattern("stripes");
        b.part("mandibles", "head", Role.STATIC, 0x1A3A4A).pivot(0, 2, -10).box(-6, -1, -4, 2, 2, 5).box(4, -1, -4, 2, 2, 5);
        b.part("claw_l", "shell", Role.ARM_LEFT, shell).pivot(11, 0, -9).rotate(0, -30, 0).box(0, -2, -2, 10, 4, 4);
        b.part("pincer_l", "claw_l", Role.JAW, shell).pivot(10, 0, 0).box(0, -3, -3, 6, 2, 6).box(0, 1, -3, 6, 2, 6);
        b.part("claw_r", "shell", Role.ARM_RIGHT, shell).pivot(-11, 0, -9).rotate(0, 30, 0).box(-10, -2, -2, 10, 4, 4);
        b.part("pincer_r", "claw_r", Role.JAW, shell).pivot(-10, 0, 0).box(-6, -3, -3, 6, 2, 6).box(-6, 1, -3, 6, 2, 6);
        spiderLegs(b, "leg", "shell", flesh, 3, 11, 3, -4, 8, 6, 7, 3);
        chainZ(b, "tail", "shell", Role.TAIL, shell, 3, 0, 0, 14, 1, 8, 5, 6, 5, "plates");
        return b.build();
    }

    static ModelSpec whisperer() {
        int body = 0x2A3A5A;
        int glow = 0x50E0E0;
        ModelSpec.Builder b = ModelSpec.builder("the_whisperer").shadow(1.6f);
        b.part("body", null, Role.FLOAT, body, glow).pivot(0, 8, 0).box(-8, -10, -6, 16, 14, 12).pattern("spots");
        b.part("head", "body", Role.HEAD, body, glow).pivot(0, -10, -2).box(-6, -8, -6, 12, 8, 10).pattern("eyes");
        b.part("frill", "head", Role.FIN, 0x1A2A4A, glow).pivot(0, -8, 3).box(-8, -6, 0, 16, 7, 1).pattern("glow");
        b.part("maw", "head", Role.JAW, 0x101828).pivot(0, -1, -6).box(-4, 0, -1, 8, 3, 2);
        for (int i = 0; i < 8; i++) {
            float yaw = i * 45;
            float x = (float) Math.sin(Math.toRadians(yaw)) * 6;
            float z = (float) Math.cos(Math.toRadians(yaw)) * 5;
            chainDown(b, "tentacle" + i + "_", "body", Role.TENTACLE, body, 2, x, 4, z, 3, 7, 25, yaw, "spots");
        }
        return b.build();
    }

    static ModelSpec vardorvis() {
        int skin = 0x5A2A2A;
        int steel = 0xA0A0A8;
        ModelSpec.Builder b = ModelSpec.builder("vardorvis").shadow(1.0f);
        b.part("torso", null, Role.BODY, skin, 0x8A1A1A).pivot(0, 8, 0).box(-5, -10, -3, 10, 12, 6).pattern("plates");
        b.part("cloak", "torso", Role.STATIC, 0x2A1A1A).pivot(0, -10, 3).box(-5.5f, 0, 0, 11, 20, 1);
        b.part("head", "torso", Role.HEAD, 0x3A3A3A, 0xE04020).pivot(0, -10, 0).box(-4, -8, -4, 8, 8, 8).pattern("eyes");
        b.part("helm_horns", "head", Role.STATIC, 0xD0C8B0).pivot(0, -7, 0).box(-7, -2, -1, 3, 2, 2).box(4, -2, -1, 3, 2, 2)
                .box(-7, -6, -1, 2, 4, 2).box(5, -6, -1, 2, 4, 2);
        b.part("parasite_head", "torso", Role.FLOAT, 0x7A3A3A, 0x40E040).pivot(4, -11, 3).box(-2, -3, -2, 4, 3, 4).pattern("eyes");
        b.part("arm_l", "torso", Role.ARM_LEFT, skin).pivot(5, -9, 0).box(0, -1, -1.5f, 3, 11, 3);
        b.part("axe_l", "arm_l", Role.STATIC, steel).pivot(1.5f, 10, 0).rotate(-80, 0, 0).box(-0.5f, -1, -1, 1, 12, 1)
                .box(-0.5f, 8, -5, 1, 5, 4).pattern("stripes");
        b.part("arm_r", "torso", Role.ARM_RIGHT, skin).pivot(-5, -9, 0).box(-3, -1, -1.5f, 3, 11, 3);
        b.part("axe_r", "arm_r", Role.STATIC, steel).pivot(-1.5f, 10, 0).rotate(-80, 0, 0).box(-0.5f, -1, -1, 1, 12, 1)
                .box(-0.5f, 8, -5, 1, 5, 4).pattern("stripes");
        legs2(b, "leg", 0x3A2A2A, 2.5f, 0, 12, 3);
        return b.build();
    }

    static ModelSpec scurrius() {
        int fur = 0x6A5A4A;
        int pink = 0xD89090;
        ModelSpec.Builder b = ModelSpec.builder("scurrius").shadow(1.2f);
        b.part("body", null, Role.BODY, fur, 0x4A3A2A).pivot(0, 15, 2).box(-7, -6, -10, 14, 11, 20).pattern("spots");
        b.part("head", "body", Role.HEAD, fur, 0xE02020).pivot(0, -1, -10).box(-4, -4, -7, 8, 7, 7).pattern("eyes");
        b.part("snout", "head", Role.STATIC, pink).pivot(0, 1, -7).box(-2, -1, -3, 4, 3, 3).box(-1, -1, -4, 2, 1, 1);
        b.part("ears", "head", Role.STATIC, pink).pivot(0, -4, -3).box(-5, -3, 0, 3, 3, 1).box(2, -3, 0, 3, 3, 1);
        b.part("whiskers", "snout", Role.STATIC, 0xE8E0D0).pivot(0, 0, -2).box(-6, 0, 0, 12, 1, 1);
        b.part("jaw", "head", Role.JAW, pink).pivot(0, 3, -5).box(-2, 0, -4, 4, 1, 4);
        b.part("teeth", "head", Role.STATIC, 0xF0E8C0).pivot(0, 2, -9).box(-1, 0, -1, 2, 2, 1);
        legs4(b, "leg", fur, 5, -7, 8, 5, 3);
        chainZ(b, "tail", "body", Role.TAIL, pink, 5, 0, 0, 10, 1, 2, 2, 6, -4, "stripes");
        return b.build();
    }

    static ModelSpec araxxor() {
        int shell = 0x2A2A24;
        int glow = 0xC0D040;
        ModelSpec.Builder b = ModelSpec.builder("araxxor").shadow(1.8f);
        b.part("body", null, Role.BODY, shell, glow).pivot(0, 12, -3).box(-8, -5, -8, 16, 9, 13).pattern("plates");
        b.part("abdomen", "body", Role.STATIC, 0x3A3A2A, glow).pivot(0, -2, 5).rotate(-20, 0, 0).box(-10, -8, 0, 20, 15, 18)
                .pattern("glow");
        b.part("armour", "abdomen", Role.STATIC, glow, shell).pivot(0, -8, 2).box(-6, -2, 0, 12, 2, 12).box(-1, -4, 2, 2, 2, 10)
                .pattern("stripes");
        b.part("head", "body", Role.HEAD, shell, 0x60F040).pivot(0, -1, -8).box(-5, -4, -6, 10, 7, 6).pattern("eyes");
        b.part("fang_l", "head", Role.JAW, glow).pivot(2.5f, 3, -6).box(-1, 0, -1, 2, 5, 2);
        b.part("fang_r", "head", Role.JAW, glow).pivot(-2.5f, 3, -6).box(-1, 0, -1, 2, 5, 2);
        spiderLegs(b, "leg", "body", shell, 4, 7, 2, -6, 4, 10, 8, 3);
        return b.build();
    }

    static ModelSpec hueycoatl() {
        int scales = 0x2A8A7A;
        int gold = 0xE0B040;
        ModelSpec.Builder b = ModelSpec.builder("the_hueycoatl").shadow(1.8f);
        b.part("coils", null, Role.STATIC, scales, gold).pivot(0, 21, 4).box(-11, -3, -11, 22, 6, 22).box(-9, -7, -9, 18, 4, 18)
                .pattern("scales");
        b.part("body", null, Role.BODY, scales, gold).pivot(0, 15, 0).box(-5, -6, -5, 10, 8, 10).pattern("scales");
        String neck = chainUp(b, "neck", "body", Role.NECK, scales, 4, 0, -6, 0, 9, 7, 8, 0, "scales");
        b.part("head", neck, Role.HEAD, scales, 0xF0F040).pivot(0, -7, 0).rotate(25, 0, 0).box(-5, -5, -11, 10, 7, 12).pattern("eyes");
        b.part("feathers", "head", Role.FIN, 0xE04040, gold).pivot(0, -5, 0).box(-7, -8, 0, 14, 8, 1).box(-1, -10, -6, 2, 5, 6)
                .pattern("stripes");
        b.part("jaw", "head", Role.JAW, gold).pivot(0, 1, -1).box(-4, 0, -9, 8, 2, 9);
        b.part("wing_l", "neck1", Role.WING_LEFT, 0x30B0A0, gold).pivot(4, -4, 2).rotate(0, -20, -30).box(0, -0.5f, 0, 14, 1, 8)
                .pattern("stripes");
        b.part("wing_r", "neck1", Role.WING_RIGHT, 0x30B0A0, gold).pivot(-4, -4, 2).rotate(0, 20, 30).box(-14, -0.5f, 0, 14, 1, 8)
                .pattern("stripes");
        chainZ(b, "tail", "coils", Role.TAIL, scales, 5, -8, -1, 8, 1, 7, 5, 6, -4, "scales");
        return b.build();
    }

    static ModelSpec amoxliatl() {
        int hide = 0x7AA0C0;
        int ice = 0xD0F0FF;
        ModelSpec.Builder b = ModelSpec.builder("amoxliatl").shadow(1.2f);
        b.part("body", null, Role.BODY, hide, 0x3A5A7A).pivot(0, 13, 1).box(-6, -5, -10, 12, 10, 20).pattern("spots");
        b.part("crystals", "body", Role.STATIC, ice, hide).pivot(0, -5, 0).box(-4, -5, -7, 2, 5, 2).box(1, -7, -3, 3, 7, 3)
                .box(-2, -4, 3, 2, 4, 2).box(2, -3, 6, 2, 3, 2).pattern("glow");
        b.part("head", "body", Role.HEAD, hide, 0x40C0FF).pivot(0, -2, -10).box(-4, -4, -7, 8, 7, 7).box(-2.5f, -1, -10, 5, 3, 3)
                .pattern("eyes");
        b.part("ears", "head", Role.STATIC, ice).pivot(0, -4, -3).box(-4, -3, 0, 2, 3, 2).box(2, -3, 0, 2, 3, 2);
        b.part("jaw", "head", Role.JAW, 0x3A5A7A).pivot(0, 3, -6).box(-2.5f, 0, -4, 5, 1, 4);
        legs4(b, "leg", hide, 4.5f, -7, 8, 9, 3);
        chainZ(b, "tail", "body", Role.TAIL, hide, 4, 0, -3, 10, 1, 3, 3, 5, -10, "stripes");
        return b.build();
    }

    static ModelSpec titan(String id, int skin, int element, int crown) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.4f);
        b.part("torso", null, Role.BODY, skin, element).pivot(0, 8, 0).box(-7, -12, -4, 14, 14, 8).pattern("plates");
        b.part("belt", "torso", Role.STATIC, 0x4A3A2A).pivot(0, 1, 0).box(-7.5f, -1, -4.5f, 15, 2, 9);
        b.part("head", "torso", Role.HEAD, skin, element).pivot(0, -12, 0).box(-4, -8, -4, 8, 8, 8).pattern("eyes");
        b.part("beard", "head", Role.JAW, element).pivot(0, -2, -4).box(-3.5f, 0, -1, 7, 6, 2).pattern("stripes");
        b.part("crown", "head", Role.STATIC, crown).pivot(0, -8, 0).box(-4.5f, -2, -4.5f, 9, 2, 9).box(-4, -5, -4, 1, 3, 1)
                .box(3, -5, -4, 1, 3, 1).box(-0.5f, -6, -4, 1, 4, 1).pattern("glow");
        b.part("arm_l", "torso", Role.ARM_LEFT, skin).pivot(7, -11, 0).box(0, -1, -2.5f, 5, 14, 5).box(-0.5f, 12, -3, 6, 5, 6);
        b.part("arm_r", "torso", Role.ARM_RIGHT, skin).pivot(-7, -11, 0).box(-5, -1, -2.5f, 5, 14, 5).box(-5.5f, 12, -3, 6, 5, 6);
        b.part("aura", "torso", Role.SPIN, element).pivot(0, -14, 0).box(-11, 0, -1, 2, 2, 2).box(9, -2, 0, 2, 2, 2).box(0, 2, 9, 2, 2, 2);
        legs2(b, "leg", 0x5A4A3A, 3.5f, 0, 14, 5);
        return b.build();
    }

    static ModelSpec zalcano() {
        int rock = 0x5A4A4A;
        int core = 0xE07020;
        ModelSpec.Builder b = ModelSpec.builder("zalcano").shadow(1.5f);
        b.part("torso", null, Role.BODY, rock, core).pivot(0, 8, 0).box(-7, -10, -4, 14, 14, 8).pattern("spots");
        b.part("core", "torso", Role.SPIN, core, 0xFFE040).pivot(0, -4, -4.5f).box(-2, -2, -1, 4, 4, 2).pattern("glow");
        b.part("head", "torso", Role.HEAD, rock, 0xFFA040).pivot(0, -10, 0).box(-4, -8, -4, 8, 8, 8).pattern("eyes");
        b.part("rock_hair", "head", Role.STATIC, 0x3A2A2A, core).pivot(0, -8, 1).box(-5, -4, -3, 3, 4, 3).box(-1, -5, -2, 3, 5, 3)
                .box(2, -3, -3, 3, 3, 3).box(-2, -2, 2, 4, 4, 3).pattern("glow");
        b.part("shoulders", "torso", Role.STATIC, 0x4A3A3A).pivot(0, -10, 0).box(-11, -2, -3, 5, 5, 6).box(6, -2, -3, 5, 5, 6);
        b.part("arm_l", "torso", Role.ARM_LEFT, rock).pivot(9, -7, 0).box(-1, 0, -2.5f, 5, 14, 5).box(-1.5f, 12, -3, 6, 5, 6);
        b.part("arm_r", "torso", Role.ARM_RIGHT, rock).pivot(-9, -7, 0).box(-4, 0, -2.5f, 5, 14, 5).box(-4.5f, 12, -3, 6, 5, 6);
        legs2(b, "leg", rock, 3.5f, 0, 12, 5);
        return b.build();
    }

    static ModelSpec mimic() {
        int wood = 0x8A5A2A;
        int gold = 0xE0C040;
        ModelSpec.Builder b = ModelSpec.builder("the_mimic").shadow(1.0f);
        b.part("chest", null, Role.BODY, wood, gold).pivot(0, 17, 0).box(-7, -6, -5, 14, 8, 10).pattern("plates");
        b.part("teeth_low", "chest", Role.STATIC, 0xF0F0E0).pivot(0, -6, -4).box(-6, -2, 0, 12, 2, 1);
        b.part("eyes", "chest", Role.HEAD, 0x2A1A1A, 0xF0E040).pivot(0, -4, -5).box(-4, -1, -0.5f, 8, 2, 1).pattern("eyes");
        b.part("lid", "chest", Role.JAW, wood, gold).pivot(0, -6, 5).rotate(20, 0, 0).box(-7.5f, -4, -10, 15, 4, 10).pattern("plates");
        b.part("teeth_up", "lid", Role.STATIC, 0xF0F0E0).pivot(0, 0, -9).box(-6, 0, 0, 12, 2, 1);
        b.part("lock", "lid", Role.STATIC, gold).pivot(0, -1, -10).box(-1.5f, -1, -1, 3, 4, 1);
        b.part("bands", "chest", Role.STATIC, gold).pivot(0, 0, 0).box(-7.5f, -6, -5.5f, 1, 8, 11).box(6.5f, -6, -5.5f, 1, 8, 11);
        b.part("tongue", "chest", Role.TAIL, 0xC04060).pivot(0, -6, -3).rotate(-30, 0, 0).box(-1.5f, -1, -7, 3, 1, 7);
        spiderLegs(b, "leg", "chest", 0x5A3A1A, 2, 6, 1, -3, 6, 4, 4, 2);
        return b.build();
    }

    static ModelSpec solHeredit() {
        int gold = 0xC0A040;
        int skin = 0x8A6A4A;
        ModelSpec.Builder b = ModelSpec.builder("sol_heredit").shadow(1.0f);
        b.part("torso", null, Role.BODY, gold, 0xE07020).pivot(0, 8, 0).box(-5, -10, -3, 10, 12, 6).pattern("plates");
        b.part("cape", "torso", Role.STATIC, 0xA02020).pivot(0, -10, 3).box(-5, 0, 0, 10, 18, 1);
        b.part("head", "torso", Role.HEAD, gold, 0xFFE080).pivot(0, -10, 0).box(-4, -8, -4, 8, 8, 8).pattern("eyes");
        b.part("crest", "head", Role.FIN, 0xE03020).pivot(0, -8, 0).box(-0.5f, -5, -5, 1, 5, 11).pattern("stripes");
        b.part("arm_l", "torso", Role.ARM_LEFT, skin).pivot(5, -9, 0).box(0, -1, -1.5f, 3, 11, 3);
        b.part("shield", "arm_l", Role.STATIC, gold, 0xE07020).pivot(3, 6, 0).box(0, -6, -5, 1, 12, 10).pattern("glow");
        b.part("arm_r", "torso", Role.ARM_RIGHT, skin).pivot(-5, -9, 0).box(-3, -1, -1.5f, 3, 11, 3);
        b.part("spear", "arm_r", Role.STATIC, 0xD0C8B0).pivot(-1.5f, 10, 0).rotate(-90, 0, 0).box(-0.5f, -10, -0.5f, 1, 26, 1)
                .box(-1, 14, -1, 2, 4, 2);
        b.part("pauldrons", "torso", Role.STATIC, 0xE0C060, 0xE07020).pivot(0, -10, 0).box(-8, -1, -3.5f, 4, 3, 7).box(4, -1, -3.5f, 4, 3, 7)
                .pattern("plates");
        b.part("kilt", "torso", Role.STATIC, 0xA02020, gold).pivot(0, 2, 0).box(-5.5f, 0, -3.5f, 11, 5, 7).pattern("stripes");
        b.part("visor", "head", Role.STATIC, 0x8A6A20).pivot(0, -3, -4).box(-3, -1, -1, 6, 4, 1);
        legs2(b, "leg", gold, 2.5f, 0, 12, 3);
        return b.build();
    }
}
