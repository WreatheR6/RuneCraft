package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.BestiaryBst.chain;
import static com.runecraft.osrs.content.bestiary.BestiaryBst.dark;
import static com.runecraft.osrs.content.bestiary.BestiaryBst.light;
import static com.runecraft.osrs.content.bestiary.BestiaryBst.sideFangs;
import static com.runecraft.osrs.content.bestiary.BestiaryBst.teeth;
import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/**
 * Abyssal, eldritch and elemental bosses: the Kraken, the Abyssal Sire, the Thermonuclear smoke devil, the
 * Dagannoth Kings, the Chaos Elemental, the Grotesque Guardians (Dusk and Dawn), Duke Sucellus, the Whisperer,
 * the Phantom Muspah, Hespori, the Royal Titans and the Mimic.
 */
final class BestiaryBstAbyss {
    private BestiaryBstAbyss() {
    }

    static List<ModelSpec> all() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(kraken());
        l.add(sire());
        l.add(thermy());
        l.add(dagannoth("dagannoth_rex", 0x5E6A44, 0xA8A870, 0xE0D8A0, 0xFFD040, 0));
        l.add(dagannoth("dagannoth_prime", 0x4A6A86, 0x9AB4C8, 0xD8E8F0, 0x60E0FF, 1));
        l.add(dagannoth("dagannoth_supreme", 0x7A4A36, 0xC8A088, 0xE8D8C0, 0xFF7040, 2));
        l.add(chaosElemental());
        l.add(gargoyle("dusk", 0x3E3E4A, 0x6A6A86, 0xFF3A2A, false));
        l.add(gargoyle("dawn", 0xA8A090, 0xD8D0B8, 0x60C0FF, true));
        l.add(duke());
        l.add(whisperer());
        l.add(muspah());
        l.add(hespori());
        l.add(titan("branda_the_fire_queen", 0x4A2A22, 0xFF7A20, 0xE0B040, true));
        l.add(titan("eldric_the_ice_king", 0x5A7A9A, 0xB0E8FF, 0xC8D0E0, false));
        l.add(mimic());
        return l;
    }

    // ------------------------------------------------------------------------------------------------
    // Kraken
    // ------------------------------------------------------------------------------------------------

    /** The Kraken: a towering cephalopod with a ridged mantle, great glowing eyes, a beak and eight sucker-lined arms. */
    static ModelSpec kraken() {
        int skin = 0x8A3A5A;
        int light = 0xD07090;
        int dk = dark(skin, 0.4f);
        int eye = 0xFFD040;
        ModelSpec.Builder b = ModelSpec.builder("kraken").shadow(1.8f);
        part(b, "head", null, Role.FLOAT, skin).accent(light).at(0, 10, 0).mat("flesh").pattern("spots")
                .box(-6, -4, -6, 12, 6, 12).add();
        part(b, "mantle", "head", Role.STATIC, skin).accent(light).at(0, -4, 1).rot(-12, 0, 0).mat("flesh").pattern("spots")
                .box(-5.5f, -10, -5, 11, 10, 10).box(-4, -14, -3.5f, 8, 4, 7).box(-2.5f, -16, -2, 5, 2, 4).add();
        sym(b, "mantle_fin", "mantle", false, light).accent(dk).at(5, -11, 0).rot(0, 0, -25).mat("membrane").box(0, -3, -3, 3, 6, 6).add();
        for (int i = 0; i < 3; i++) {
            part(b, "ridge" + i, "mantle", Role.STATIC, dk).at(0, -3 - i * 4, 5).rot(10, 0, 0).mat("flesh").box(-3 + i, -1, -0.5f, 6 - 2 * i, 2, 1).add();
        }
        sym(b, "eye", "head", false, eye).at(5.5f, -1.5f, -3).rot(0, -20, 0).mat("energy").glow("full").box(-1, -2, -2, 2, 3, 4).add();
        sym(b, "lid", "eye", true, dk).at(0, -2, 0).rot(0, 0, -10).mat("flesh").box(-1.5f, -1, -2.5f, 3, 1, 5).add();
        part(b, "beak", "head", Role.JAW, 0x2A2020).at(0, 2, -1).mat("horn").box(-1.5f, 0, -1.5f, 3, 2, 3).box(-0.5f, 2, -0.5f, 1, 1, 1).add();
        // eight arms: two long feeding tentacles and six curling arms
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45 + 22.5);
            float x = (float) Math.sin(a) * 4.5f;
            float z = (float) Math.cos(a) * 4.5f;
            boolean longArm = i == 3 || i == 4;
            int segs = longArm ? 5 : 4;
            String prev = "head";
            for (int k = 0; k < segs; k++) {
                String n = "arm" + i + "_" + k;
                int w = Math.max(1, 3 - k * 3 / segs);
                part(b, n, prev, Role.TENTACLE, k == segs - 1 ? light : skin).accent(light)
                        .at(k == 0 ? x : 0, k == 0 ? 2 : 4, k == 0 ? z : 0)
                        .rot(k == 0 ? 68 : (longArm ? -14 : 26), k == 0 ? (float) Math.toDegrees(a) : 0, 0).mat("flesh")
                        .box(-w / 2f, 0, -w / 2f, w, 4, w).add();
                if (k == 1) {
                    part(b, "suckers" + i, n, Role.STATIC, light).at(0, 2, -w / 2f).mat("flesh").box(-0.5f, -1.5f, -0.6f, 1, 1, 1)
                            .box(-0.5f, 0.5f, -0.6f, 1, 1, 1).add();
                }
                prev = n;
            }
        }
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Abyssal Sire
    // ------------------------------------------------------------------------------------------------

    /** The Abyssal Sire: a vast, hunched abyssal demon-lord of veined flesh, bristling with tentacles and burning eyes. */
    static ModelSpec sire() {
        int flesh = 0x6A1A3A;
        int pale = 0xA84A6A;
        int dk = 0x3A0A1E;
        int bone = 0x2A1A26;
        int eye = 0xFF4A3A;
        ModelSpec.Builder b = ModelSpec.builder("abyssal_sire").shadow(2.0f);
        part(b, "body", null, Role.BODY, flesh).accent(eye).at(0, 9, 2).rot(14, 0, 0).mat("flesh").glow("cracks")
                .box(-10, -16, -7, 20, 18, 14).box(-8, -20, -5, 16, 4, 11).add();
        part(b, "belly", "body", Role.STATIC, pale).accent(dk).at(0, -5, -7).mat("flesh").box(-7, -9, -1, 14, 13, 2).box(-5, -10, -1.5f, 10, 1, 1).add();
        for (int i = 0; i < 4; i++) {
            part(b, "belly_fold" + i, "belly", Role.STATIC, dark(pale, 0.2f)).at(0, -7 + i * 3.2f, -1.2f).mat("flesh")
                    .box(-6 + Math.abs(i - 1.5f), -0.5f, -0.5f, (int) (12 - 2 * Math.abs(i - 1.5f)), 1, 1).add();
        }
        sym(b, "shoulder", "body", false, flesh).accent(eye).at(9, -16, 0).rot(0, 0, -15).mat("flesh").glow("cracks").box(-3, -4, -5, 7, 8, 10).add();
        sym(b, "shoulder_spike", "shoulder", true, bone).at(2, -4, 0).rot(-10, 0, -25).mat("horn").box(-1, -5, -1, 2, 5, 2).box(-0.5f, -7, -0.5f, 1, 2, 1).add();
        for (int i = 0; i < 4; i++) {
            part(b, "spine" + i, "body", Role.STATIC, bone).at(0, -18 + i * 4.5f, 5.5f).rot(50, 0, 0).mat("horn")
                    .box(-1, -4, -1, 2, 4, 2).add();
        }
        // head
        part(b, "head", "body", Role.HEAD, flesh).accent(eye).at(0, -20, -3).rot(-14, 0, 0).mat("flesh").box(-6, -7, -7, 12, 8, 9).add();
        part(b, "brow", "head", Role.STATIC, dk).at(0, -5, -7).rot(12, 0, 0).mat("flesh").box(-6.5f, -1, -1, 13, 2, 3).add();
        b.part("eyes", "head", Role.STATIC, eye).pivot(0, -3.6f, -7.1f).material("energy").glow("full")
                .box(1.5f, -0.5f, -0.4f, 3, 1, 1).box(-4.5f, -0.5f, -0.4f, 3, 1, 1).box(-0.5f, -2, -0.4f, 1, 1, 1);
        part(b, "jaw", "head", Role.JAW, pale).accent(dk).at(0, 0, -5).rot(10, 0, 0).mat("flesh").glow("maw").box(-5, 0, -3, 10, 3, 4).add();
        teeth(b, "teeth", "head", 0xE8D8C8, 0, 0, -7.6f, 9, 6, 1, false);
        sym(b, "horn", "head", false, bone).at(5, -7, -2).rot(-35, 0, -30).mat("horn").box(-1, -6, -1, 2, 6, 2).add();
        sym(b, "horn_tip", "horn", true, light(bone, 0.15f)).at(0, -6, 0).rot(-30, 0, 20).mat("horn").box(-0.5f, -4, -0.5f, 1, 4, 1).add();
        for (int i = 0; i < 4; i++) {
            part(b, "face_tendril" + i, "jaw", Role.TENTACLE, pale).at(-3 + i * 2, 3, -2).rot(10, 0, (i - 1.5f) * 10).mat("flesh")
                    .box(-0.5f, 0, -0.5f, 1, 5 + (i % 2) * 2, 1, 0.1f).add();
        }
        // long arms with claws
        sym(b, "arm", "body", false, flesh).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(eye).at(11, -15, -1).rot(-20, 0, -14).mat("flesh")
                .glow("cracks").box(-1, -2, -3, 6, 12, 6).add();
        sym(b, "fore", "arm", true, flesh).at(2, 10, 0).rot(-30, 0, 8).mat("flesh").box(-2.5f, 0, -2.5f, 5, 10, 5).add();
        sym(b, "hand", "fore", true, dk).at(0, 10, 0).mat("flesh").box(-3, 0, -3, 6, 3, 6).add();
        sym(b, "claws", "hand", true, 0xD8C8B8).at(0, 3, 0).rot(20, 0, 0).mat("horn").box(-2.5f, 0, -2.5f, 1, 4, 1).box(-0.5f, 0, -3, 1, 5, 1)
                .box(1.5f, 0, -2.5f, 1, 4, 1).box(-0.5f, 0, 1.5f, 1, 3, 1).add();
        // stubby legs
        sym(b, "leg", null, false, flesh).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(eye).at(5.5f, 12, 3).rot(-10, 0, -6)
                .mat("flesh").box(-3.5f, -2, -3.5f, 7, 8, 7).add();
        sym(b, "foot", "leg", true, dk).at(0, 6, 0).rot(10, 0, 6).mat("flesh").box(-3.5f, 0, -5, 7, 6, 7).add();
        sym(b, "toes", "foot", true, 0xD8C8B8).at(0, 5, -5).mat("horn").box(-3, -0.5f, -1.5f, 1, 1, 2).box(-0.5f, -0.5f, -1.5f, 1, 1, 2).box(2, -0.5f, -1.5f, 1, 1, 2).add();
        // back tentacles
        for (int i = 0; i < 6; i++) {
            float x = (i - 2.5f) * 3.2f;
            String t = chain(b, "tentacle" + i + "_", "body", Role.TENTACLE, i % 2 == 0 ? flesh : pale, pale, "flesh", x, -10 + Math.abs(i - 2.5f) * 2, 6,
                    false, new int[]{3, 2, 2, 1}, new int[]{5, 5, 4, 4}, new float[]{-20 + Math.abs(i - 2.5f) * 10, 18, 22, 22},
                    new float[]{(i - 2.5f) * 14, 0, 0, 0});
        }
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Thermonuclear smoke devil
    // ------------------------------------------------------------------------------------------------

    /** The Thermonuclear smoke devil: a floating, bat-eared maw of a creature trailing a whirling tail of smoke and embers. */
    static ModelSpec thermy() {
        int skin = 0x4A4646;
        int ash = 0x6A6260;
        int dk = 0x2A2626;
        int fire = 0xFF8A20;
        ModelSpec.Builder b = ModelSpec.builder("thermonuclear_smoke_devil").shadow(1.1f);
        part(b, "body", null, Role.FLOAT, skin).accent(fire).at(0, 6, 0).mat("rock").glow("cracks").box(-6, -8, -5, 12, 11, 10).add();
        part(b, "chest", "body", Role.STATIC, ash).accent(fire).at(0, -3, -5).mat("rock").box(-4.5f, -4, -1, 9, 7, 1).add();
        // the head is all mouth: a huge maw with layered teeth
        part(b, "head", "body", Role.HEAD, skin).accent(fire).at(0, -8, -1).rot(-8, 0, 0).mat("rock").box(-6, -7, -6, 12, 7, 9).add();
        b.part("eyes", "head", Role.STATIC, fire).pivot(0, -5.2f, -6.1f).material("energy").glow("full")
                .box(1.5f, -0.5f, -0.4f, 3, 1, 1).box(-4.5f, -0.5f, -0.4f, 3, 1, 1);
        part(b, "brow", "head", Role.STATIC, dk).at(0, -6, -6).rot(15, 0, 0).mat("rock").box(-6, -1, -1, 12, 2, 2).add();
        part(b, "maw", "head", Role.STATIC, 0x5A1A10).at(0, -2.5f, -6).mat("lava").glow("full").box(-4.5f, -1, -0.3f, 9, 3, 1).add();
        teeth(b, "teeth_up", "head", 0xE8E0D0, 0, -3.5f, -6.4f, 9, 6, 1, false);
        part(b, "jaw", "head", Role.JAW, ash).accent(fire).at(0, 0, -4).rot(14, 0, 0).mat("rock").glow("maw").box(-5, -1, -3, 10, 3, 5).add();
        teeth(b, "teeth_lo", "jaw", 0xE8E0D0, 0, -1, -2.6f, 8, 5, 1, true);
        sym(b, "ear", "head", false, skin).accent(fire).at(6, -5, 0).rot(-10, -20, -40).mat("membrane").box(0, -2, -0.5f, 7, 5, 1).box(5, -4, -0.5f, 3, 2, 1).add();
        sym(b, "horn", "head", false, dk).at(3, -7, 0).rot(-30, 0, -20).mat("horn").box(-0.5f, -4, -0.5f, 1, 4, 1, 0.3f).add();
        // small clawed arms
        sym(b, "arm", "body", false, skin).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(fire).at(6, -6, -1).rot(-30, 0, -20).mat("rock")
                .box(0, -1, -1.5f, 3, 7, 3).add();
        sym(b, "fore", "arm", true, ash).at(1.5f, 6.5f, 0).rot(-40, 0, 10).mat("rock").box(-1.5f, 0, -1.5f, 3, 5, 3).add();
        sym(b, "claw", "fore", true, 0xE0D8C8).at(0, 5, 0).mat("horn").box(-1.5f, 0, -1.5f, 1, 2, 1).box(0.5f, 0, -1.5f, 1, 2, 1).box(-0.5f, 0, 0.5f, 1, 2, 1).add();
        // whirling smoke tail with embers
        int[] w = {8, 6, 5, 4, 3};
        String prev = "body";
        for (int i = 0; i < w.length; i++) {
            String n = "smoke" + i;
            part(b, n, prev, i == 0 ? Role.TAIL : Role.SPIN, i % 2 == 0 ? ash : 0x7A7270).accent(fire).at(0, i == 0 ? 3 : 3, 0).rot(0, 30, 6)
                    .mat("ghost").box(-w[i] / 2f, 0, -w[i] / 2f, w[i], 3, w[i]).add();
            prev = n;
        }
        part(b, "embers", "body", Role.SPIN, fire).at(0, 0, 0).mat("lava").glow("full")
                .box(-10, -4, -1, 2, 2, 2).box(8, -2, 0, 2, 2, 2).box(-1, 2, 8, 2, 2, 2).box(0, -7, -10, 2, 2, 2).box(7, 4, -6, 1, 1, 1)
                .box(-8, 5, 5, 1, 1, 1).add();
        part(b, "puffs", "body", Role.SPIN, 0x8A8280).at(0, 2, 0).rot(0, 45, 0).mat("ghost")
                .box(-12, -2, -2, 4, 4, 4).box(8, -1, -2, 4, 3, 4).box(-2, -3, 8, 4, 4, 4).box(-2, 1, -12, 4, 3, 4).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Dagannoth Kings
    // ------------------------------------------------------------------------------------------------

    /**
     * A Dagannoth King: a towering, hunched sea-beast on thick legs, plated belly, finned arms, a long crested head
     * with tusks. {@code kind}: 0 = Rex (horned brute), 1 = Prime (finned, magical), 2 = Supreme (spiked, ranged).
     */
    static ModelSpec dagannoth(String id, int hide, int belly, int horn, int eye, int kind) {
        int dk = dark(hide, 0.35f);
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.2f);
        part(b, "hips", null, Role.BODY, hide).accent(belly).at(0, 12, 3).mat("scales").box(-5, -3, -4, 10, 6, 8).add();
        part(b, "torso", "hips", Role.STATIC, hide).accent(belly).at(0, -2, -1).rot(24, 0, 0).mat("scales")
                .box(-6, -13, -4.5f, 12, 13, 9).box(-5, -16, -3.5f, 10, 3, 7).add();
        Kit.P pl = part(b, "belly", "torso", Role.STATIC, belly).accent(dark(belly, 0.3f)).at(0, -1, -4.5f).mat("plates");
        for (int i = 0; i < 4; i++) {
            pl.box(-4 + i * 0.5f, -12 + i * 3, -0.8f, 8 - i, 3, 1);
        }
        pl.add();
        // dorsal spikes / fin
        for (int i = 0; i < 4; i++) {
            part(b, "dorsal" + i, "torso", Role.STATIC, kind == 1 ? light(hide, 0.25f) : horn).accent(dk).at(0, -14 + i * 3.5f, 4)
                    .rot(-55 + i * 8, 0, 0).mat(kind == 1 ? "membrane" : "horn").box(-0.5f, -4 + i / 2, -1, 1, 4 - i / 2, kind == 1 ? 4 : 2).add();
        }
        sym(b, "shoulder", "torso", false, hide).accent(belly).at(6, -13, 0).rot(0, 0, -15).mat("scales").box(-2, -2.5f, -3, 4, 5, 6).add();
        if (kind == 2) {
            sym(b, "shoulder_spike", "shoulder", true, horn).at(1, -2.5f, 0).rot(-10, 0, -30).mat("horn").box(-0.5f, -4, -0.5f, 1, 4, 1, 0.2f).add();
        }
        // head: long, crested, tusked
        part(b, "neck", "torso", Role.NECK, hide).accent(belly).at(0, -15, -2).rot(-30, 0, 0).mat("scales").box(-3, -4, -3, 6, 5, 6).add();
        part(b, "head", "neck", Role.HEAD, hide).accent(eye).at(0, -3, -2).rot(-4, 0, 0).mat("scales").box(-4, -4, -7, 8, 6, 9).add();
        part(b, "snout", "head", Role.STATIC, hide).accent(belly).at(0, -0.5f, -7).rot(6, 0, 0).mat("scales").box(-3, -2.5f, -4, 6, 4, 4).add();
        b.part("eyes", "head", Role.STATIC, eye).pivot(0, -2.2f, -7.1f).material("energy").glow("full")
                .box(1.5f, -0.5f, -0.4f, 2, 1, 1).box(-3.5f, -0.5f, -0.4f, 2, 1, 1);
        part(b, "jaw", "head", Role.JAW, belly).accent(dk).at(0, 2, -5).rot(12, 0, 0).mat("plates").box(-3, 0, -6, 6, 2, 7).add();
        sideFangs(b, "fangs", "snout", 0xF0ECD8, 2.6f, 1.4f, -3.6f, 2f, 2, 1, false);
        sym(b, "tusk", "jaw", false, horn).at(2.6f, 0, -5.5f).rot(-30, 0, 15).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1, 0.2f).add();
        part(b, "crest", "head", Role.STATIC, kind == 1 ? light(hide, 0.25f) : dk).accent(horn).at(0, -4, -2).rot(-20, 0, 0)
                .mat(kind == 1 ? "membrane" : "horn").box(-0.5f, -4, -4, 1, 4, 9).box(-0.5f, -6, 1, 1, 2, 4).add();
        if (kind == 0) {
            sym(b, "horn", "head", false, horn).at(3.5f, -4, -1).rot(-50, 20, -20).mat("horn").box(-1, -5, -1, 2, 5, 2)
                    .box(-0.5f, -7, -0.5f, 1, 2, 1).add();
        } else {
            sym(b, "frill", "head", false, kind == 1 ? light(hide, 0.25f) : horn).accent(dk).at(4, -2, 0).rot(0, -30, -20)
                    .mat(kind == 1 ? "membrane" : "horn").box(0, -2, -0.5f, 4, 4, 1).box(3, -3, -0.5f, 2, 2, 1).add();
        }
        // arms with webbed fins and claws
        sym(b, "arm", "torso", false, hide).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(belly).at(7, -12, -1).rot(-20, 0, -10).mat("scales")
                .box(-1.5f, -1, -1.5f, 3, 8, 3).add();
        sym(b, "fin", "arm", true, kind == 2 ? horn : light(hide, 0.2f)).accent(dk).at(1.2f, 1, 1).rot(10, 0, -10).mat("membrane")
                .box(0, 0, -0.5f, 3, 7, 1).add();
        sym(b, "fore", "arm", true, hide).at(0, 7.5f, 0).rot(-40, 0, 5).mat("scales").box(-1.5f, 0, -1.5f, 3, 7, 3).add();
        sym(b, "hand", "fore", true, dk).at(0, 7, 0).mat("scales").box(-2, 0, -2, 4, 2, 4).add();
        sym(b, "claws", "hand", true, horn).at(0, 2, 0).rot(15, 0, 0).mat("horn").box(-2, 0, -2, 1, 3, 1).box(-0.5f, 0, -2, 1, 3, 1)
                .box(1, 0, -2, 1, 3, 1).add();
        // legs
        sym(b, "thigh", null, false, hide).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(belly).at(4, 13, 3).rot(-25, 0, -5)
                .mat("scales").box(-2.5f, -2, -3, 5, 7, 6).add();
        sym(b, "shin", "thigh", true, hide).at(0, 6, 0).rot(45, 0, 5).mat("scales").box(-2, 0, -2, 4, 5, 4).add();
        sym(b, "foot", "shin", true, dk).at(0, 4.5f, 0).rot(-20, 0, 0).mat("scales").box(-2.5f, 0, -4.5f, 5, 2, 6).add();
        sym(b, "toes", "foot", true, horn).at(0, 1, -4.5f).mat("horn").box(-2.5f, -0.5f, -1.5f, 1, 1, 2).box(-0.5f, -0.5f, -2, 1, 1, 2)
                .box(1.5f, -0.5f, -1.5f, 1, 1, 2).add();
        String tail = chain(b, "tail", "hips", Role.TAIL, hide, belly, "scales", 0, 0, 4, false,
                new int[]{5, 4, 3, 2}, new int[]{5, 5, 5, 4}, new float[]{25, 8, 4, 4}, null);
        part(b, "tail_fin", tail, Role.STATIC, kind == 1 ? light(hide, 0.25f) : horn).at(0, 0, 4).mat("membrane")
                .box(-0.5f, -2.5f, 0, 1, 5, 3).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Chaos Elemental
    // ------------------------------------------------------------------------------------------------

    /** The Chaos Elemental: a hovering, ever-shifting being of clashing red, blue and green chaos-matter. */
    static ModelSpec chaosElemental() {
        int red = 0xC83030;
        int blue = 0x3050D0;
        int green = 0x30C040;
        int gold = 0xF0D040;
        int purple = 0x9030C0;
        ModelSpec.Builder b = ModelSpec.builder("chaos_elemental").shadow(0.9f);
        part(b, "core", null, Role.FLOAT, red).accent(gold).at(0, 6, 0).mat("energy").glow("full").box(-5, -6, -4, 10, 10, 8).add();
        part(b, "inner", "core", Role.SPIN, gold).at(0, -1, 0).rot(45, 0, 45).mat("energy").glow("full").box(-3, -3, -3, 6, 6, 6).add();
        part(b, "plates", "core", Role.STATIC, 0x2A1A2A).accent(red).at(0, 0, 0).mat("rock")
                .box(-5.5f, -6.5f, -4.5f, 4, 4, 2).box(1.5f, -2, -4.5f, 4, 5, 2).box(-5.5f, 1, 2.5f, 5, 4, 2).box(2, -6.5f, 2.5f, 4, 3, 2).add();
        // head-like mass with chaotic eyes
        part(b, "head", "core", Role.HEAD, blue).accent(gold).at(0, -6, 0).rot(0, 20, 0).mat("energy").glow("full").box(-4, -6, -4, 8, 6, 8).add();
        part(b, "head_plates", "head", Role.STATIC, 0x1A1A2A).accent(blue).at(0, -3, 0).mat("rock")
                .box(-4.5f, -3.5f, -4.5f, 3, 3, 2).box(1.5f, -1, -4.5f, 3, 2, 2).box(-4.5f, -2, 2.5f, 4, 3, 2).add();
        b.part("eyes", "head", Role.STATIC, 0xFFFFFF).pivot(0, -3.5f, -4.4f).material("energy").glow("full")
                .box(1, -0.5f, -0.4f, 2, 1, 1).box(-3, -0.5f, -0.4f, 2, 1, 1);
        part(b, "crown", "head", Role.STATIC, 0x202020).accent(purple).at(0, -6, 0).mat("horn")
                .box(-4, -4, -1, 1, 4, 1).box(3, -4, -1, 1, 4, 1).box(-0.5f, -5, -3, 1, 5, 1).box(-2, -3, 2, 1, 3, 1).box(1.5f, -3, 2, 1, 3, 1).add();
        // chaotic arms of floating chunks
        sym(b, "arm", "core", false, green).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(gold).at(6, -4, 0).rot(0, 0, -25).mat("energy").glow("full")
                .box(0, -2, -2, 4, 4, 4).add();
        sym(b, "arm_mid", "arm", true, purple).at(5, 1, 0).rot(0, 20, 30).mat("energy").glow("full").box(0, -1.5f, -1.5f, 3, 3, 3).add();
        sym(b, "arm_end", "arm_mid", true, red).at(4, 1, 0).rot(0, 0, 30).mat("energy").glow("full").box(0, -2, -2, 4, 4, 4).add();
        sym(b, "arm_claw", "arm_end", true, 0x2A1A2A).at(4, 0, 0).mat("horn").box(0, -1.5f, -1.5f, 2, 1, 1).box(0, 0.5f, -1.5f, 2, 1, 1)
                .box(0, -0.5f, 0.5f, 2, 1, 1).add();
        // orbiting rings of chaos-matter
        part(b, "ring_green", "core", Role.SPIN, green).at(0, 1, 0).mat("energy").glow("full")
                .box(-12, -2, -1, 3, 4, 2).box(9, -2, -1, 3, 4, 2).box(-1, -2, -12, 2, 4, 3).box(-1, -2, 9, 2, 4, 3).add();
        part(b, "ring_blue", "core", Role.SPIN, blue).at(0, -9, 0).rot(0, 45, 0).mat("energy").glow("full")
                .box(-9, -1, -1, 2, 2, 2).box(7, -1, -1, 2, 2, 2).box(-1, -1, -9, 2, 2, 2).box(-1, -1, 7, 2, 2, 2).add();
        part(b, "ring_purple", "core", Role.SPIN, purple).at(0, 8, 0).rot(10, 22, 0).mat("energy").glow("full")
                .box(-8, -1, -1, 2, 2, 2).box(6, -1, -1, 2, 2, 2).box(-1, -1, -8, 2, 2, 2).box(-1, -1, 6, 2, 2, 2).add();
        // dissolving lower body
        int[] w = {8, 6, 5, 3};
        String prev = "core";
        for (int i = 0; i < w.length; i++) {
            String n = "wisp" + i;
            int c = i % 3 == 0 ? purple : i % 3 == 1 ? red : blue;
            part(b, n, prev, i == 0 ? Role.TAIL : Role.SPIN, c).accent(gold).at(i == 0 ? 0 : (i % 2 == 0 ? 0.5f : -0.5f), i == 0 ? 4 : 3, 0)
                    .rot(0, 25, i * 4).mat("energy").glow("full").box(-w[i] / 2f, 0, -w[i] / 2f, w[i], 3, w[i]).add();
            prev = n;
        }
        part(b, "shards", "core", Role.SPIN, 0x1A1A1A).accent(gold).at(0, 2, 0).rot(0, 30, 0).mat("rock")
                .box(-7, 4, -6, 2, 2, 2).box(6, 6, 4, 2, 2, 2).box(-3, 9, 7, 1, 1, 1).box(5, -10, -5, 1, 2, 1).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Grotesque Guardians
    // ------------------------------------------------------------------------------------------------

    /**
     * Dusk and Dawn: stone gargoyles of the Slayer Tower roof. Dusk is the dark, horned brute; Dawn is the pale,
     * slender flier. Bat-winged, clawed, with stone plates and glowing eyes.
     */
    static ModelSpec gargoyle(String id, int stone, int accentStone, int eye, boolean dawn) {
        int dk = dark(stone, 0.35f);
        int claw = dawn ? 0xE8E0D0 : 0x24242A;
        float bulk = dawn ? 0 : 1;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.2f);
        part(b, "hips", null, dawn ? Role.FLOAT : Role.BODY, stone).accent(accentStone).at(0, 12, 1).mat("rock")
                .box(-4.5f - bulk, -3, -3, (int) (9 + bulk * 2), 5, 6).add();
        part(b, "loin", "hips", Role.STATIC, dk).at(0, 1.5f, -3).mat("rock").box(-3, 0, -0.6f, 6, 4, 1).add();
        part(b, "torso", "hips", Role.STATIC, stone).accent(accentStone).at(0, -2, 0).rot(dawn ? 8 : 22, 0, 0).mat("rock")
                .box(-5 - bulk, -11, -3.5f - bulk / 2, (int) (10 + bulk * 2), 11, (int) (7 + bulk)).add();
        part(b, "chest_plate", "torso", Role.STATIC, accentStone).accent(dk).at(0, -9, -3.5f - bulk / 2).mat("rock")
                .box(-4.5f - bulk / 2, 0, -0.8f, (int) (9 + bulk), 4, 1).box(-3, 4.5f, -0.8f, 6, 3, 1).add();
        part(b, "abs", "torso", Role.STATIC, dk).at(0, -1.5f, -3.5f - bulk / 2).mat("rock").box(-3, -1, -0.6f, 6, 2, 1).add();
        sym(b, "shoulder", "torso", false, accentStone).accent(dk).at(5 + bulk, -10, 0).rot(0, 0, -15).mat("rock")
                .box(-1.5f, -2.5f, -3, (int) (4 + bulk), 5, 6).add();
        sym(b, "shoulder_spike", "shoulder", true, claw).at(1.5f, -2.5f, 0).rot(-10, 0, -25).mat("horn").box(-0.5f, -3 - bulk, -0.5f, 1, (int) (3 + bulk), 1, 0.2f).add();
        // head
        part(b, "head", "torso", Role.HEAD, stone).accent(eye).at(0, -11, -1).rot(dawn ? -6 : -18, 0, 0).mat("rock").box(-3.5f, -6, -4, 7, 6, 7).add();
        part(b, "brow", "head", Role.STATIC, dk).at(0, -4.5f, -4).rot(18, 0, 0).mat("rock").box(-3.5f, -1, -1, 7, 2, 2).add();
        b.part("eyes", "head", Role.STATIC, eye).pivot(0, -3.6f, -4.1f).material("energy").glow("full")
                .box(1, -0.5f, -0.4f, 2, 1, 1).box(-3, -0.5f, -0.4f, 2, 1, 1);
        part(b, "snout", "head", Role.STATIC, stone).at(0, -1.5f, -4).mat("rock").box(-2.5f, -1.5f, -2, 5, 3, 2).add();
        part(b, "jaw", "head", Role.JAW, dk).at(0, 0, -3).rot(10, 0, 0).mat("rock").box(-3, 0, -3, 6, 2, 4).add();
        sideFangs(b, "fangs", "jaw", 0xE8E0D0, 2.5f, 0, -2.6f, 1.5f, 1, 2, true);
        sym(b, "horn", "head", false, claw).at(2.5f, -6, -1).rot(dawn ? -55 : -30, 15, dawn ? -10 : -30).mat("horn")
                .box(-1, -4, -1, 2, 4, 2).add();
        sym(b, "horn_tip", "horn", true, claw).at(0, -4, 0).rot(dawn ? -25 : -35, 0, dawn ? 0 : 30).mat("horn").box(-0.5f, -4, -0.5f, 1, 4, 1).add();
        sym(b, "ear", "head", false, stone).at(3.5f, -3.5f, 0).rot(0, -30, -20).mat("rock").box(0, -2, -0.5f, 3, 3, 1).add();
        // arms
        sym(b, "arm", "torso", false, stone).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(accentStone).at(6 + bulk, -9, 0).rot(-10, 0, -12)
                .mat("rock").box(-1.5f - bulk / 2, -1, -1.5f - bulk / 2, (int) (3 + bulk), 8, (int) (3 + bulk)).add();
        sym(b, "fore", "arm", true, accentStone).accent(dk).at(0, 7.5f, 0).rot(-20, 0, 6).mat("rock")
                .box(-2 - bulk / 2, 0, -2 - bulk / 2, (int) (4 + bulk), 7, (int) (4 + bulk)).add();
        sym(b, "hand", "fore", true, dk).at(0, 7, 0).mat("rock").box(-2, 0, -2, 4, 2, 4).add();
        sym(b, "claws", "hand", true, claw).at(0, 2, 0).rot(15, 0, 0).mat("horn").box(-2, 0, -2, 1, 3, 1).box(-0.5f, 0, -2, 1, 3, 1)
                .box(1, 0, -2, 1, 3, 1).add();
        // digitigrade legs
        sym(b, "thigh", null, false, stone).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(accentStone).at(3 + bulk / 2, 13, 1)
                .rot(-25, 0, -4).mat("rock").box(-2 - bulk / 2, -2, -2.5f, (int) (4 + bulk), 7, 5).add();
        sym(b, "shin", "thigh", true, accentStone).at(0, 6, 0.5f).rot(50, 0, 4).mat("rock").box(-1.5f, 0, -1.5f, 3, 5, 3).add();
        sym(b, "foot", "shin", true, dk).at(0, 4.5f, 0).rot(-25, 0, 0).mat("rock").box(-2, 0, -4, 4, 2, 5).add();
        sym(b, "talons", "foot", true, claw).at(0, 1, -4).mat("horn").box(-2, -0.5f, -1.5f, 1, 1, 2).box(1, -0.5f, -1.5f, 1, 1, 2).add();
        // bat wings
        int membrane = dawn ? light(stone, 0.1f) : dark(stone, 0.15f);
        sym(b, "wing", "torso", false, accentStone).roles(Role.WING_LEFT, Role.WING_RIGHT).accent(dk).at(3, -9, 3.5f)
                .rot(dawn ? -20 : -5, -30, dawn ? -45 : -25).mat("rock").box(0, -1, -1, 10, 2, 2).add();
        sym(b, "wing_fore", "wing", true, accentStone).at(10, 0, 0).rot(0, 30, 30).mat("rock").box(0, -0.5f, -0.5f, 9, 1, 1, 0.3f).add();
        sym(b, "wing_web", "wing", true, membrane).accent(dk).at(0, 0.2f, 0).mat("membrane").box(0, -0.5f, 0, 10, 1, 9, -0.3f).add();
        sym(b, "wing_web2", "wing_fore", true, membrane).accent(dk).at(0, 0.3f, 0).rot(0, -15, 0).mat("membrane").box(0, -0.5f, 0, 9, 1, 10, -0.3f).add();
        sym(b, "wing_finger", "wing_fore", true, accentStone).at(9, 0, 0).rot(0, -70, 10).mat("rock").box(0, -0.5f, -0.5f, 8, 1, 1, 0.2f).add();
        sym(b, "wing_claw", "wing_fore", true, claw).at(9, 0, 0).rot(0, 20, -20).mat("horn").box(0, -0.5f, -0.5f, 2, 1, 1).add();
        String tail = chain(b, "tail", "hips", Role.TAIL, stone, accentStone, "rock", 0, 0, 3, false,
                new int[]{3, 2, 2, 1}, new int[]{4, 4, 4, 4}, new float[]{30, 10, -10, -15}, null);
        part(b, "tail_tip", tail, Role.STATIC, claw).at(0, 0, 4).mat("horn").box(-1.5f, -0.5f, 0, 3, 1, 2).box(-0.5f, -0.5f, 2, 1, 1, 1).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Duke Sucellus
    // ------------------------------------------------------------------------------------------------

    /** Duke Sucellus: a colossal, bloated horned demon-lord, slumped and chained, with a tusked maw and gas vents. */
    static ModelSpec duke() {
        int skin = 0x5E4E6A;
        int pale = 0x9A88A8;
        int dk = 0x2E2236;
        int horn = 0x2A2026;
        int eye = 0xB0F040;
        int chain = 0x7A7A80;
        ModelSpec.Builder b = ModelSpec.builder("duke_sucellus").shadow(1.9f);
        part(b, "belly", null, Role.BODY, skin).accent(pale).at(0, 13, 0).mat("flesh")
                .box(-12, -11, -10, 24, 20, 18).box(-10, -13, -8, 20, 2, 14).add();
        part(b, "gut", "belly", Role.STATIC, pale).accent(dk).at(0, -2, -10).mat("flesh").box(-9, -7, -2, 18, 14, 2).box(-7, -5, -3, 14, 10, 1).add();
        for (int i = 0; i < 3; i++) {
            part(b, "fold" + i, "gut", Role.STATIC, dark(pale, 0.25f)).at(0, -4 + i * 4, -3.2f).mat("flesh")
                    .box(-6 + i, -0.5f, -0.5f, 12 - 2 * i, 1, 1).add();
        }
        part(b, "chest", "belly", Role.STATIC, skin).accent(pale).at(0, -12, -1).rot(-6, 0, 0).mat("flesh").box(-10, -8, -6, 20, 8, 12).add();
        sym(b, "shoulder", "chest", false, dk).accent(eye).at(10, -6, 0).rot(0, 0, -20).mat("flesh").box(-3, -4, -5, 7, 7, 10).add();
        // gas vents along the back
        for (int i = 0; i < 3; i++) {
            part(b, "vent" + i, "chest", Role.STATIC, horn).accent(eye).at((i - 1) * 6, -8, 4).rot(-25, 0, (i - 1) * -15).mat("horn")
                    .box(-1.5f, -4, -1.5f, 3, 4, 3).add();
            part(b, "vent_gas" + i, "vent" + i, Role.STATIC, eye).at(0, -4, 0).mat("energy").glow("full").box(-1, -1, -1, 2, 1, 2).add();
        }
        // head with huge horns, four eyes and a tusked maw
        part(b, "head", "chest", Role.HEAD, skin).accent(eye).at(0, -8, -3).rot(8, 0, 0).mat("flesh").box(-6, -8, -8, 12, 9, 10).add();
        part(b, "brow", "head", Role.STATIC, dk).at(0, -6.5f, -8).rot(15, 0, 0).mat("flesh").box(-6.5f, -1, -1, 13, 3, 3).add();
        b.part("eyes", "head", Role.STATIC, eye).pivot(0, -4.4f, -8.1f).material("energy").glow("full")
                .box(1, -0.5f, -0.4f, 2, 1, 1).box(-3, -0.5f, -0.4f, 2, 1, 1).box(3.8f, -1.5f, -0.2f, 2, 1, 1).box(-5.8f, -1.5f, -0.2f, 2, 1, 1);
        part(b, "maw", "head", Role.STATIC, 0x2A0E14).at(0, -1, -8).mat("flesh").glow("maw").box(-4.5f, -1, -0.3f, 9, 2, 1).add();
        part(b, "jaw", "head", Role.JAW, pale).accent(dk).at(0, 1, -6).rot(10, 0, 0).mat("flesh").box(-5.5f, 0, -3, 11, 4, 5).add();
        teeth(b, "teeth", "jaw", 0xE0D8C0, 0, 0, -2.6f, 9, 6, 1, true);
        sym(b, "tusk", "jaw", false, 0xE0D8C0).at(4.5f, 0, -2.5f).rot(-20, 0, 15).mat("bone").box(-1, -5, -1, 2, 5, 2).box(-0.5f, -7, -0.5f, 1, 2, 1).add();
        sym(b, "horn", "head", false, horn).at(5, -7, -2).rot(-20, 0, -60).mat("horn").box(-1.5f, -6, -1.5f, 3, 6, 3).add();
        sym(b, "horn_mid", "horn", true, horn).at(0, -6, 0).rot(-30, 0, 40).mat("horn").box(-1, -6, -1, 2, 6, 2).add();
        sym(b, "horn_tip", "horn_mid", true, light(horn, 0.2f)).at(0, -6, 0).rot(-20, 0, 30).mat("horn").box(-0.5f, -4, -0.5f, 1, 4, 1).add();
        for (int i = 0; i < 3; i++) {
            part(b, "tendril" + i, "jaw", Role.TENTACLE, pale).at(-3 + i * 3, 4, -1).rot(5, 0, (i - 1) * 12).mat("flesh")
                    .box(-0.5f, 0, -0.5f, 1, 5 + (i == 1 ? 2 : 0), 1, 0.2f).add();
        }
        // massive arms resting on the ground
        sym(b, "arm", "chest", false, skin).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(pale).at(12, -5, -1).rot(-12, 0, -18).mat("flesh")
                .box(-1, -2, -3.5f, 7, 13, 7).add();
        sym(b, "fore", "arm", true, skin).at(2.5f, 12, 0).rot(-20, 0, 18).mat("flesh").box(-3.5f, 0, -3.5f, 7, 10, 7).add();
        sym(b, "hand", "fore", true, dk).at(0, 10, -1).mat("flesh").box(-4, 0, -4.5f, 8, 3, 8).add();
        sym(b, "claws", "hand", true, horn).at(0, 2, -4.5f).mat("horn").box(-3.5f, -0.5f, -2, 1, 1, 2).box(-1.2f, -0.5f, -2.5f, 1, 1, 3)
                .box(1.2f, -0.5f, -2.5f, 1, 1, 3).box(2.5f, -0.5f, -2, 1, 1, 2).add();
        sym(b, "shackle", "fore", true, chain).at(0, 7, 0).mat("metal").box(-4, -1, -4, 8, 2, 8).add();
        // stubby folded legs
        sym(b, "leg", "belly", false, skin).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(pale).at(8, 6, -4).rot(-80, -20, 0).mat("flesh")
                .box(-3.5f, -2, -3.5f, 7, 9, 7).add();
        sym(b, "foot", "leg", true, dk).at(0, 8, 0).rot(80, 0, 0).mat("flesh").box(-3, -2, -6, 6, 4, 7).add();
        // chains to the floor
        part(b, "chains", "belly", Role.STATIC, chain).at(0, 11, 0).mat("chain")
                .box(-17, -12, -1, 1, 12, 1).box(16, -12, -1, 1, 12, 1).box(-17, -1, -1, 6, 1, 1).box(11, -1, -1, 6, 1, 1).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // The Whisperer
    // ------------------------------------------------------------------------------------------------

    /** The Whisperer: a hovering eldritch horror, a hooded, crested head over a ribbed body that dissolves into tentacles. */
    static ModelSpec whisperer() {
        int body = 0x24344E;
        int deep = 0x16223A;
        int glow = 0x50E8E0;
        int pale = 0x5A7A9A;
        ModelSpec.Builder b = ModelSpec.builder("the_whisperer").shadow(1.6f);
        part(b, "body", null, Role.FLOAT, body).accent(glow).at(0, 6, 0).mat("flesh").glow("runes").box(-7, -10, -5, 14, 13, 10).add();
        for (int i = 0; i < 4; i++) {
            part(b, "rib" + i, "body", Role.STATIC, pale).accent(glow).at(0, -8 + i * 3, -5).mat("bone")
                    .box(-6 + i * 0.5f, -0.5f, -0.8f, (int) (12 - i), 1, 1).add();
        }
        part(b, "spine_glow", "body", Role.STATIC, glow).at(0, -5, 5).mat("energy").glow("full").box(-0.5f, -5, -0.3f, 1, 10, 1).add();
        sym(b, "shoulder", "body", false, deep).accent(glow).at(7, -9, 0).rot(0, 0, -20).mat("flesh").box(-2, -2, -4, 5, 4, 8).add();
        // head: crested, hooded, eyeless with a glowing maw of tendrils
        part(b, "head", "body", Role.HEAD, body).accent(glow).at(0, -10, -1).rot(-10, 0, 0).mat("flesh").box(-5, -8, -6, 10, 8, 9).add();
        part(b, "hood", "head", Role.STATIC, deep).accent(glow).at(0, -8, 1).rot(-20, 0, 0).mat("flesh").box(-6, -2, -6, 12, 4, 10).box(-5, -4, -3, 10, 2, 7).add();
        b.part("eyes", "head", Role.STATIC, glow).pivot(0, -5, -6.1f).material("energy").glow("full")
                .box(1.5f, -0.5f, -0.4f, 2, 1, 1).box(-3.5f, -0.5f, -0.4f, 2, 1, 1).box(-0.5f, -2, -0.4f, 1, 1, 1)
                .box(3.5f, -2, -0.2f, 1, 1, 1).box(-4.5f, -2, -0.2f, 1, 1, 1);
        part(b, "maw", "head", Role.JAW, 0x0A1020).accent(glow).at(0, -1.5f, -6).mat("flesh").glow("maw").box(-3.5f, -1, -1, 7, 3, 2).add();
        for (int i = 0; i < 5; i++) {
            part(b, "mouth_tendril" + i, "maw", Role.TENTACLE, pale).at(-2.8f + i * 1.4f, 2, -0.5f).rot(15, 0, (i - 2) * 8).mat("flesh")
                    .box(-0.5f, 0, -0.5f, 1, 4 + (i % 2) * 2, 1, -0.1f).add();
        }
        for (int i = 0; i < 5; i++) {
            float a = (i - 2) * 25f;
            part(b, "crest" + i, "head", Role.FIN, i % 2 == 0 ? deep : pale).accent(glow).at((i - 2) * 2, -8, 1).rot(-40 + Math.abs(i - 2) * 8, 0, -a)
                    .mat("membrane").glow(i == 2 ? "runes" : null).box(-1, -8 + Math.abs(i - 2), -0.5f, 2, 8 - Math.abs(i - 2), 1).add();
        }
        // thin clawed arms
        sym(b, "arm", "body", false, body).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(glow).at(8, -8, 0).rot(-20, 0, -25).mat("flesh")
                .box(-1, -1, -1, 2, 9, 2).add();
        sym(b, "fore", "arm", true, pale).at(0, 9, 0).rot(-50, 0, 10).mat("flesh").box(-1, 0, -1, 2, 8, 2).add();
        sym(b, "claw", "fore", true, glow).at(0, 8, 0).mat("energy").glow("full").box(-1.5f, 0, -1, 1, 3, 1).box(0.5f, 0, -1, 1, 3, 1).box(-0.5f, 0, 0.5f, 1, 2, 1).add();
        // tentacled lower body
        for (int i = 0; i < 6; i++) {
            float yaw = i * 60 + 30;
            float x = (float) Math.sin(Math.toRadians(yaw)) * 5;
            float z = (float) Math.cos(Math.toRadians(yaw)) * 3.5f;
            String prev = "body";
            int[] w = {3, 2, 2, 1};
            for (int k = 0; k < w.length; k++) {
                String n = "tentacle" + i + "_" + k;
                part(b, n, prev, Role.TENTACLE, k == 3 ? glow : (k % 2 == 0 ? body : deep)).accent(glow).at(k == 0 ? x : 0, k == 0 ? 2 : 5, k == 0 ? z : 0)
                        .rot(k == 0 ? 25 : 15, k == 0 ? yaw : 0, 0).mat(k == 3 ? "energy" : "flesh").glow(k == 3 ? "full" : null)
                        .box(-w[k] / 2f, 0, -w[k] / 2f, w[k], 5, w[k]).add();
                prev = n;
            }
        }
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Phantom Muspah
    // ------------------------------------------------------------------------------------------------

    /** The Phantom Muspah: a spectral, ice-shard-crowned brute with a skull-like head, tusks and heavy fists. */
    static ModelSpec muspah() {
        int body = 0x4A5A7E;
        int deep = 0x2E3A5A;
        int ice = 0xA8D8F8;
        int spirit = 0xC0E8FF;
        int bone = 0xD8E0E8;
        ModelSpec.Builder b = ModelSpec.builder("phantom_muspah").shadow(1.5f);
        part(b, "hips", null, Role.BODY, deep).accent(spirit).at(0, 11, 1).mat("ghost").box(-6, -3, -4, 12, 6, 8).add();
        part(b, "torso", "hips", Role.STATIC, body).accent(spirit).at(0, -2, 0).rot(18, 0, 0).mat("ghost").glow("cracks")
                .box(-8, -13, -5, 16, 13, 10).box(-6, -15, -4, 12, 2, 8).add();
        part(b, "ribcage", "torso", Role.STATIC, bone).at(0, -10, -5).mat("bone").box(-6, 0, -0.8f, 12, 1, 1).box(-5, 2.5f, -0.8f, 10, 1, 1)
                .box(-4, 5, -0.8f, 8, 1, 1).box(-0.5f, -1, -1, 1, 8, 1).add();
        // ice shards erupting from the back and shoulders
        int[][] sh = {{-5, -13, 7}, {4, -14, 8}, {0, -12, 6}, {-7, -9, 5}, {7, -10, 6}};
        for (int i = 0; i < sh.length; i++) {
            part(b, "shard" + i, "torso", Role.STATIC, ice).accent(spirit).at(sh[i][0], sh[i][1], 3).rot(-30 + i * 6, i * 25, sh[i][0] * -4)
                    .mat("ice").glow("full").box(-1.5f, -sh[i][2], -1.5f, 3, sh[i][2], 3).box(-0.5f, -sh[i][2] - 2, -0.5f, 1, 2, 1).add();
        }
        sym(b, "shoulder", "torso", false, deep).accent(spirit).at(8, -12, 0).rot(0, 0, -15).mat("ghost").box(-2, -3, -4, 6, 6, 8).add();
        // skull-like head
        part(b, "head", "torso", Role.HEAD, bone).accent(spirit).at(0, -15, -2).rot(-14, 0, 0).mat("bone").box(-5, -7, -7, 10, 8, 9).add();
        part(b, "brow", "head", Role.STATIC, light(bone, 0.1f)).at(0, -5, -7).rot(15, 0, 0).mat("bone").box(-5.5f, -1, -1, 11, 2, 2).add();
        part(b, "sockets", "head", Role.STATIC, 0x141A2A).at(0, -3.5f, -7.1f).mat("rock").box(1, -1, -0.3f, 3, 2, 1).box(-4, -1, -0.3f, 3, 2, 1).add();
        b.part("eyes", "head", Role.STATIC, spirit).pivot(0, -3.5f, -7.5f).material("energy").glow("full")
                .box(1.5f, -0.5f, -0.4f, 2, 1, 1).box(-3.5f, -0.5f, -0.4f, 2, 1, 1);
        part(b, "jaw", "head", Role.JAW, bone).accent(deep).at(0, 0.5f, -5).rot(10, 0, 0).mat("bone").box(-4.5f, 0, -3, 9, 3, 5).add();
        teeth(b, "teeth", "jaw", 0xF0F4F8, 0, 0, -2.6f, 7, 5, 1, true);
        sym(b, "tusk", "jaw", false, ice).at(4, 0, -2.5f).rot(-40, 0, 20).mat("ice").glow("full").box(-0.5f, -5, -0.5f, 1, 5, 1, 0.3f).add();
        sym(b, "horn", "head", false, ice).at(4, -7, -2).rot(-30, 0, -40).mat("ice").glow("full").box(-1, -6, -1, 2, 6, 2).box(-0.5f, -8, -0.5f, 1, 2, 1).add();
        // heavy arms with stone-like fists
        sym(b, "arm", "torso", false, body).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(spirit).at(9, -11, 0).rot(-15, 0, -12).mat("ghost")
                .glow("cracks").box(-2, -1, -2.5f, 5, 11, 5).add();
        sym(b, "fore", "arm", true, deep).accent(spirit).at(0.5f, 10.5f, 0).rot(-25, 0, 8).mat("ghost").box(-2.5f, 0, -2.5f, 5, 9, 5).add();
        sym(b, "fist", "fore", true, bone).at(0, 9, 0).mat("bone").box(-3.5f, 0, -3.5f, 7, 5, 7).add();
        sym(b, "knuckles", "fist", true, ice).at(0, 4.5f, -3.5f).mat("ice").glow("full").box(-3, -1, -0.5f, 2, 2, 1).box(-0.5f, -1, -0.5f, 2, 2, 1)
                .box(2, -1, -0.5f, 1, 2, 1).add();
        // legs
        sym(b, "thigh", null, false, body).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(spirit).at(4, 13, 1).rot(-10, 0, -5)
                .mat("ghost").box(-2.5f, -2, -3, 5, 7, 6).add();
        sym(b, "shin", "thigh", true, deep).at(0, 6, 0).rot(15, 0, 5).mat("ghost").box(-2, 0, -2, 4, 5, 4).add();
        sym(b, "foot", "shin", true, bone).at(0, 4.5f, 0).rot(-5, 0, 0).mat("bone").box(-2.5f, 0, -4, 5, 2, 6).add();
        part(b, "aura", "torso", Role.SPIN, spirit).at(0, -4, 0).mat("ice").glow("full")
                .box(-13, 0, -1, 2, 2, 2).box(11, -3, 0, 2, 2, 2).box(0, 2, 11, 2, 2, 2).box(-1, -6, -13, 2, 2, 2).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Hespori
    // ------------------------------------------------------------------------------------------------

    /** Hespori: a monstrous carnivorous plant: a ridged bulb on a nest of roots, crowned by a toothed bloom and thorned vines. */
    static ModelSpec hespori() {
        int plant = 0x3A6A2A;
        int dk = 0x1E3A16;
        int bloom = 0xC04A8A;
        int pollen = 0xF0E040;
        int thorn = 0xD8C890;
        ModelSpec.Builder b = ModelSpec.builder("hespori").shadow(1.7f);
        part(b, "bulb", null, Role.BODY, plant).accent(0x6A9A3A).at(0, 18, 0).mat("moss")
                .box(-9, -10, -9, 18, 12, 18).box(-7, -14, -7, 14, 4, 14).box(-10, -6, -7, 20, 6, 14).add();
        for (int i = 0; i < 4; i++) {
            part(b, "rib" + i, "bulb", Role.STATIC, dk).at(0, -6, 0).rot(0, i * 90 + 45, 0).mat("wood")
                    .box(-1, -8, -10.5f, 2, 12, 2).add();
        }
        // roots splayed on the ground
        for (int i = 0; i < 6; i++) {
            float yaw = i * 60 + 15;
            part(b, "root" + i, "bulb", Role.STATIC, 0x5A4A2A).at(0, 2, 0).rot(0, yaw, 0).mat("wood")
                    .box(-1.5f, 0, -14, 3, 3, 6).box(-1, 1, -18, 2, 2, 4).add();
        }
        // the bloom head
        part(b, "stem", "bulb", Role.NECK, plant).accent(dk).at(0, -14, 0).mat("moss").box(-3.5f, -4, -3.5f, 7, 4, 7).add();
        part(b, "flower", "stem", Role.HEAD, bloom).accent(pollen).at(0, -4, 0).mat("flesh").box(-5, -5, -5, 10, 5, 10).add();
        part(b, "maw", "flower", Role.STATIC, 0x5A1030).at(0, -5.2f, 0).mat("flesh").glow("full").box(-3, -0.5f, -3, 6, 1, 6).add();
        part(b, "pollen", "maw", Role.STATIC, pollen).at(0, -0.5f, 0).mat("energy").glow("full").box(-1, -2, -1, 2, 2, 2).add();
        teeth(b, "teeth_n", "flower", 0xF0E8D0, 0, -5, -3, 5, 4, 1, true);
        teeth(b, "teeth_s", "flower", 0xF0E8D0, 0, -5, 3, 5, 4, 1, true);
        for (int i = 0; i < 6; i++) {
            float yaw = i * 60;
            part(b, "petal" + i, "flower", Role.FIN, i % 2 == 0 ? bloom : light(bloom, 0.15f)).accent(0xF0A0C0).at(0, -4, 0).rot(-62, yaw, 0)
                    .mat("membrane").box(-3, -11, -0.5f, 6, 11, 1).box(-2, -13, -0.5f, 4, 2, 1).add();
        }
        // thorned vines
        for (int i = 0; i < 4; i++) {
            float yaw = i * 90 + 45;
            float x = (float) Math.sin(Math.toRadians(yaw)) * 8;
            float z = (float) Math.cos(Math.toRadians(yaw)) * 8;
            String prev = "bulb";
            for (int k = 0; k < 3; k++) {
                String n = "vine" + i + "_" + k;
                part(b, n, prev, Role.TENTACLE, plant).accent(dk).at(k == 0 ? x : 0, k == 0 ? -8 : -6, k == 0 ? z : 0)
                        .rot(k == 0 ? -35 : -20, k == 0 ? yaw + 180 : 0, 0).mat("moss").box(-1, -6, -1, 2, 6, 2).add();
                prev = n;
            }
            part(b, "thorns" + i, "vine" + i + "_1", Role.STATIC, thorn).at(0, -3, 0).mat("horn")
                    .box(1, -1, -0.5f, 2, 1, 1).box(-3, -4, -0.5f, 2, 1, 1).box(-0.5f, -2, 1, 1, 1, 2).add();
            part(b, "leaf" + i, "vine" + i + "_2", Role.STATIC, 0x5A9A3A).at(0, -6, 0).rot(30, 0, 0).mat("moss").box(-2, -0.5f, -3, 4, 1, 4).add();
        }
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Royal Titans
    // ------------------------------------------------------------------------------------------------

    /**
     * The Royal Titans: Branda the Fire Queen and Eldric the Ice King, towering elemental giants of rock wreathed in
     * flame or ice, crowned, with a craggy beard/mane and huge fists.
     */
    static ModelSpec titan(String id, int rock, int element, int crown, boolean fire) {
        int dk = dark(rock, 0.4f);
        String emat = fire ? "lava" : "ice";
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.6f);
        part(b, "hips", null, Role.BODY, rock).accent(element).at(0, 9, 1).mat("rock").glow("cracks").box(-6, -3, -4, 12, 6, 8).add();
        part(b, "loin", "hips", Role.STATIC, dk).accent(element).at(0, 2, -4).mat("rock").box(-4, 0, -0.6f, 8, 5, 1).add();
        part(b, "torso", "hips", Role.STATIC, rock).accent(element).at(0, -2, 0).rot(10, 0, 0).mat("rock").glow("cracks")
                .box(-8, -14, -5, 16, 14, 10).box(-7, -16, -4, 14, 2, 8).add();
        part(b, "chest_core", "torso", Role.STATIC, element).at(0, -10, -5).mat("energy").glow("full").box(-2, -2, -0.6f, 4, 4, 1).add();
        sym(b, "pec", "torso", false, dk).accent(element).at(4, -10, -5).mat("rock").box(-3, -3, -0.8f, 5, 5, 1).add();
        // elemental growths: crystals of ice or tongues of flame across the shoulders and back
        int[][] g = {{-6, -15, 5}, {5, -16, 7}, {0, -14, 6}, {-4, -10, 4}, {6, -9, 5}};
        for (int i = 0; i < g.length; i++) {
            part(b, "growth" + i, "torso", Role.STATIC, element).accent(light(element, 0.3f)).at(g[i][0], g[i][1], 4)
                    .rot(-30 + i * 5, i * 30, g[i][0] * -5).mat(emat).glow("full").box(-1.5f, -g[i][2], -1.5f, 3, g[i][2], 3)
                    .box(-0.5f, -g[i][2] - 2, -0.5f, 1, 2, 1).add();
        }
        sym(b, "shoulder", "torso", false, dk).accent(element).at(8, -13, 0).rot(0, 0, -15).mat("rock").glow("cracks")
                .box(-2, -3, -4.5f, 6, 7, 9).add();
        // head with crown and beard/mane
        part(b, "head", "torso", Role.HEAD, rock).accent(element).at(0, -16, -1).rot(-6, 0, 0).mat("rock").box(-4, -8, -4.5f, 8, 8, 8).add();
        part(b, "brow", "head", Role.STATIC, dk).at(0, -6, -4.5f).rot(15, 0, 0).mat("rock").box(-4.5f, -1, -1, 9, 2, 2).add();
        b.part("eyes", "head", Role.STATIC, element).pivot(0, -4.6f, -4.6f).material("energy").glow("full")
                .box(1, -0.5f, -0.4f, 2, 1, 1).box(-3, -0.5f, -0.4f, 2, 1, 1);
        part(b, "beard", "head", Role.JAW, element).accent(light(element, 0.3f)).at(0, -2.5f, -4.5f).rot(8, 0, 0).mat(emat).glow("full")
                .box(-3.5f, 0, -1, 7, 4, 2).box(-2.5f, 4, -1, 5, 3, 2).box(-1, 7, -1, 2, 2, 2).add();
        part(b, "mane", "head", Role.STATIC, element).accent(light(element, 0.3f)).at(0, -5, 3).rot(20, 0, 0).mat(emat).glow("full")
                .box(-4.5f, -3, 0, 9, 8, 2).box(-3, 5, 0, 6, 3, 2).add();
        part(b, "crown", "head", Role.STATIC, crown).accent(element).at(0, -8, 0).mat("metal")
                .box(-4.5f, -2, -5, 9, 2, 9).box(-4.5f, -4, -5, 1, 2, 1).box(3.5f, -4, -5, 1, 2, 1).box(-0.5f, -5, -5.2f, 1, 3, 1)
                .box(-4.5f, -4, 3, 1, 2, 1).box(3.5f, -4, 3, 1, 2, 1).add();
        part(b, "crown_gem", "crown", Role.STATIC, element).at(0, -3, -5.3f).mat("energy").glow("full").box(-1, -1, -0.5f, 2, 2, 1).add();
        // arms and fists
        sym(b, "arm", "torso", false, rock).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(element).at(10, -12, 0).rot(-8, 0, -10).mat("rock")
                .glow("cracks").box(-2, -1, -3, 6, 11, 6).add();
        sym(b, "fore", "arm", true, dk).accent(element).at(1, 10.5f, 0).rot(-15, 0, 6).mat("rock").glow("cracks").box(-3, 0, -3, 6, 9, 6).add();
        sym(b, "bracer", "fore", true, crown).at(0, 6, 0).mat("metal").box(-3.5f, 0, -3.5f, 7, 2, 7).add();
        sym(b, "fist", "fore", true, rock).accent(element).at(0, 9, 0).mat("rock").box(-3.5f, 0, -3.5f, 7, 5, 7).add();
        sym(b, "knuckles", "fist", true, dk).at(0, 4, -3.5f).mat("rock").box(-3, -1, -0.8f, 2, 2, 1).box(-0.5f, -1, -0.8f, 2, 2, 1)
                .box(2, -1, -0.8f, 1, 2, 1).add();
        // legs
        sym(b, "thigh", null, false, rock).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(element).at(3.5f, 11, 1).mat("rock")
                .glow("cracks").box(-3, -2, -3, 6, 7, 6).add();
        sym(b, "shin", "thigh", true, dk).accent(element).at(0, 6, 0).mat("rock").box(-2.5f, 0, -2.5f, 5, 6, 5).add();
        sym(b, "greave", "shin", true, crown).at(0, 1, -2.5f).mat("metal").box(-2, 0, -0.8f, 4, 4, 1).add();
        sym(b, "foot", "shin", true, dk).at(0, 5, 0).mat("rock").box(-3, 0, -4.5f, 6, 2, 7).add();
        // cape of flame / frost
        part(b, "cape", "torso", Role.STATIC, fire ? 0x8A1A10 : 0x2A4A8A).accent(crown).at(0, -15, 5).rot(8, 0, 0).mat("cloth")
                .box(-7, 0, 0, 14, 20, 1).add();
        part(b, "cape_hem", "cape", Role.STATIC, element).at(0, 20, 0).mat(emat).glow("full").box(-7, -1, -0.2f, 14, 2, 1)
                .box(-5, 1, -0.2f, 3, 1, 1).box(2, 1, -0.2f, 3, 1, 1).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // The Mimic
    // ------------------------------------------------------------------------------------------------

    /** The Mimic: a gold-banded treasure chest come alive: a gaping toothed lid, a lolling tongue, eyes and spindly legs. */
    static ModelSpec mimic() {
        int wood = 0x7A4A22;
        int dk = 0x4A2A12;
        int gold = 0xE0C040;
        int tooth = 0xF0EADA;
        ModelSpec.Builder b = ModelSpec.builder("the_mimic").shadow(1.0f);
        part(b, "chest", null, Role.BODY, wood).accent(gold).at(0, 16, 0).mat("wood").box(-7, -6, -5, 14, 8, 10).add();
        part(b, "bands", "chest", Role.STATIC, gold).accent(dk).at(0, 0, 0).mat("metal")
                .box(-7.5f, -6, -5.5f, 1, 8, 11).box(6.5f, -6, -5.5f, 1, 8, 11).box(-7.5f, 1, -5.5f, 15, 1, 11).add();
        part(b, "rivets", "bands", Role.STATIC, light(gold, 0.2f)).at(0, 0, -5.6f).mat("metal")
                .box(-7.5f, -5, -0.3f, 1, 1, 1).box(6.5f, -5, -0.3f, 1, 1, 1).box(-7.5f, -1, -0.3f, 1, 1, 1).box(6.5f, -1, -0.3f, 1, 1, 1).add();
        part(b, "inside", "chest", Role.STATIC, 0x3A0E14).at(0, -6, 0).mat("flesh").glow("maw").box(-6, -0.5f, -4, 12, 1, 8).add();
        teeth(b, "teeth_lo", "chest", tooth, 0, -6, -4.6f, 12, 8, 1, true);
        sym(b, "fangs_lo", "chest", false, tooth).at(6, -6, -1).mat("bone").box(-0.5f, -2, -2.5f, 1, 2, 1).box(-0.5f, -1, 1, 1, 1, 1).add();
        b.part("eyes", "chest", Role.STATIC, 0xFFE040).pivot(0, -3, -5.2f).material("energy").glow("full")
                .box(1.5f, -0.5f, -0.4f, 2, 1, 1).box(-3.5f, -0.5f, -0.4f, 2, 1, 1);
        part(b, "lid", "chest", Role.JAW, wood).accent(gold).at(0, -6, 5).rot(-38, 0, 0).mat("wood")
                .box(-7.5f, -3, -10, 15, 3, 10).box(-6.5f, -4.5f, -9, 13, 2, 8).add();
        part(b, "lid_bands", "lid", Role.STATIC, gold).at(0, 0, 0).mat("metal").box(-8, -3.5f, -10.5f, 1, 4, 11).box(7, -3.5f, -10.5f, 1, 4, 11)
                .box(-1, -5, -10.5f, 2, 2, 10).add();
        part(b, "lock", "lid", Role.STATIC, gold).at(0, -1, -10.5f).mat("metal").box(-1.5f, -1, -0.5f, 3, 4, 1).add();
        teeth(b, "teeth_up", "lid", tooth, 0, 0, -9.5f, 13, 8, 2, false);
        part(b, "tongue", "chest", Role.TAIL, 0xC04060).at(0, -6, -3).rot(18, 0, 0).mat("flesh").box(-2, -1, -7, 4, 1, 7).box(-1.5f, -0.5f, -9, 3, 1, 2).add();
        part(b, "coins", "chest", Role.STATIC, gold).at(0, -6.6f, 2).mat("metal").glow("full")
                .box(-4, -0.5f, -1, 2, 1, 2).box(1, -0.5f, 0, 3, 1, 2).box(-1, -0.8f, 1.5f, 2, 1, 1).add();
        // spindly legs
        for (int i = 0; i < 2; i++) {
            sym(b, "leg" + i, "chest", false, dk).roles(Role.LEG_EXTRA, Role.LEG_EXTRA).accent(gold).at(6, 1, -3 + i * 6).rot(0, (0.5f - i) * 50, -20)
                    .mat("wood").box(0, -1, -1, 6, 2, 2).add();
            sym(b, "shin" + i, "leg" + i, true, dk).at(6, 0, 0).rot(0, 0, 80).mat("wood").box(0, -0.75f, -0.75f, 7, 1, 1, 0.3f).add();
            sym(b, "claw" + i, "shin" + i, true, tooth).at(7, 0, 0).rot(0, 0, 10).mat("horn").box(0, -0.5f, -1, 2, 1, 2).add();
        }
        return b.build();
    }
}
