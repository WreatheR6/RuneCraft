package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/**
 * Flagship boss re-sculpts (round 10): the most-fought bosses rebuilt at TzTok-Jad's level of detail with
 * OSRS-faithful silhouettes, palettes and signature details. Registered last in {@link Overrides}, so they
 * replace every earlier version of the same model id.
 */
final class Flagships {
    private Flagships() {
    }

    static List<ModelSpec> overrides() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(corporealBeast());
        l.add(kraken());
        l.add(thermonuclearSmokeDevil());
        l.add(nightmare());
        l.add(verzikP3());
        l.add(zuk());
        l.add(kreearra());
        l.add(graardor());
        l.add(vorkath());
        l.add(cerberus());
        return l;
    }

    private static final int BONE = 0xE6DCC4;
    private static final int TOOTH = 0xF0EADA;

    /**
     * Corporeal Beast: a hulking, pale spectral quadruped with towering shoulders, a low brutish head with a
     * gaping fanged maw, a bony dorsal ridge and its spirit sigil glowing on the chest and brow.
     */
    static ModelSpec corporealBeast() {
        int body = 0x6FAE96;
        int pale = 0x96CCB6;
        int deep = 0x467E6C;
        int rim = 0xE4FFF2;
        int sigil = 0xF0FFF8;
        ModelSpec.Builder b = ModelSpec.builder("corporeal_beast").shadow(1.8f);
        part(b, "body", null, Role.BODY, body).accent(rim).at(0, 8, 2).mat("spectral").pattern("swirls")
                .box(-11, -8, -12, 22, 16, 14).box(-9, -5, 2, 18, 12, 13).add();
        part(b, "hump", "body", Role.STATIC, pale).accent(rim).at(0, -8, -4).rot(-8, 0, 0).mat("spectral").pattern("swirls")
                .box(-9, -5, -8, 18, 6, 13).add();
        part(b, "belly", "body", Role.STATIC, deep).accent(rim).at(0, 8, 0).mat("spectral").box(-8, -1, -10, 16, 2, 20).add();
        part(b, "sigil", "body", Role.STATIC, deep).accent(sigil).at(0, -2, -12.2f).mat("spectral").glow("sigil")
                .box(-5, -5, -0.5f, 10, 10, 1).add();
        for (int i = 0; i < 6; i++) {
            part(b, "spine" + i, "body", Role.STATIC, BONE).at(0, -12 + i * 1.2f, -8 + i * 4.2f).rot(-30 - i * 6, 0, 0).mat("bone")
                    .box(-1, -6 + (i > 3 ? 2 : 0), -1, 2, 6 - (i > 3 ? 2 : 0), 2).add();
            if (i < 4) {
                sym(b, "rib" + i, "body", false, BONE).at(10.6f, -3, -9 + i * 4).rot(0, 0, 8).mat("bone").box(0, -4, -0.5f, 1, 9, 1).add();
            }
        }
        // head: low and forward, heavy brow, sigil on the forehead, long fanged jaw
        part(b, "head", "body", Role.HEAD, pale).accent(0xFF6A50).at(0, 6, -12).mat("spectral").glow("eyes").pattern("eyes")
                .box(-6.5f, -5, -9, 13, 9, 9).add();
        part(b, "brow", "head", Role.STATIC, body).accent(rim).at(0, -5, -9).rot(14, 0, 0).mat("spectral")
                .box(-7, -1.5f, -1, 14, 2, 4).add();
        part(b, "crest", "head", Role.STATIC, BONE).at(0, -6, -4).rot(-40, 0, 0).mat("bone").box(-1, -6, -1, 2, 6, 3).add();
        sym(b, "horn", "head", false, BONE).at(5.5f, -4, -5).rot(-30, 0, 35).mat("bone").box(-1, -6, -1, 2, 6, 2).add();
        part(b, "snout", "head", Role.STATIC, pale).accent(rim).at(0, 1.5f, -9).mat("spectral").box(-5, -2, -4, 10, 4, 4).add();
        part(b, "jaw", "head", Role.JAW, deep).accent(rim).at(0, 4, -3).rot(10, 0, 0).mat("spectral").box(-6, 0, -10, 12, 3, 10).add();
        sym(b, "fang", "jaw", false, TOOTH).at(4.5f, 0, -9.5f).rot(-8, 0, 0).mat("bone").box(-0.5f, -4, -0.5f, 1, 4, 1).add();
        sym(b, "tusk", "head", false, TOOTH).at(4, 3.5f, -12.5f).mat("bone").box(-0.5f, 0, -0.5f, 1, 3, 1).add();
        // towering front legs, shorter hind legs
        sym(b, "arm", null, false, body).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(rim).at(11, 6, -6)
                .rot(0, 0, -6).mat("spectral").pattern("swirls").box(-4, -3, -4.5f, 8, 13, 9).add();
        sym(b, "forearm", "arm", true, pale).accent(rim).at(0, 10, 0).mat("spectral").box(-3.5f, 0, -3.5f, 7, 6, 7).add();
        sym(b, "paw", "forearm", true, deep).accent(rim).at(0, 6, -0.5f).rot(0, 0, 6).mat("spectral").box(-4, 0, -5, 8, 2, 9).add();
        for (int k = 0; k < 3; k++) {
            sym(b, "claw" + k, "paw", true, BONE).at(-2.5f + k * 2.5f, 1, -5).mat("bone").box(-0.5f, -1, -2, 1, 2, 2).add();
        }
        sym(b, "leg", null, false, body).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(rim).at(8, 12, 11)
                .mat("spectral").pattern("swirls").box(-3.5f, -2, -4, 7, 8, 8).add();
        sym(b, "shin", "leg", true, pale).accent(rim).at(0, 6, 0.5f).rot(10, 0, 0).mat("spectral").box(-3, 0, -3, 6, 5, 6).add();
        sym(b, "foot", "shin", true, deep).accent(rim).at(0, 5, -0.5f).rot(-10, 0, 0).mat("spectral").box(-3.5f, 0, -5, 7, 2, 7).add();
        part(b, "tail", "body", Role.TAIL, deep).accent(rim).at(0, -2, 15).rot(-20, 0, 0).mat("spectral").box(-2, -2, 0, 4, 4, 7).add();
        part(b, "tail_tip", "tail", Role.TAIL, BONE).at(0, 0, 7).rot(25, 0, 0).mat("bone").box(-1, -1, 0, 2, 2, 4).add();
        return b.build();
    }

    /**
     * Kraken: a vast crimson cephalopod rising from its whirlpool, a ridged bulbous mantle with side fins, huge
     * glowing eyes above a hooked beak, eight sucker-lined arms and two long feeding tentacles.
     */
    static ModelSpec kraken() {
        int hide = 0x8E2F38;
        int dark = 0x5A1A24;
        int under = 0xD88A84;
        int eye = 0xFFC83A;
        ModelSpec.Builder b = ModelSpec.builder("kraken").shadow(1.8f);
        part(b, "base", null, Role.BODY, hide).accent(under).at(0, 12, 0).mat("flesh").box(-10, -4, -9, 20, 8, 18).add();
        part(b, "mantle", "base", Role.STATIC, hide).accent(under).at(0, -4, 1).rot(-14, 0, 0).mat("flesh").pattern("spots")
                .box(-9, -12, -8, 18, 12, 16).add();
        part(b, "mantle_top", "mantle", Role.STATIC, hide).accent(under).at(0, -12, 0).rot(-8, 0, 0).mat("flesh").pattern("spots")
                .box(-7, -8, -6, 14, 8, 12).add();
        part(b, "mantle_tip", "mantle_top", Role.STATIC, dark).at(0, -8, 0).rot(-10, 0, 0).mat("flesh").box(-4, -5, -4, 8, 5, 8)
                .box(-2, -7, -2, 4, 2, 4).add();
        for (int i = 0; i < 3; i++) {
            part(b, "ridge" + i, "mantle", Role.STATIC, dark).at(0, -11 + i * 4, -8.3f).mat("flesh").box(-6 + i, -0.5f, -0.5f, 12 - 2 * i, 1, 1).add();
        }
        sym(b, "fin", "mantle_top", false, dark).roles(Role.FIN, Role.FIN).accent(under).at(6.5f, -6, 0).rot(0, 0, -20)
                .mat("membrane").box(0, -4, -5, 2, 9, 10).add();
        sym(b, "fin_edge", "fin", true, hide).at(1.5f, -3, 0).rot(0, 0, -20).mat("membrane").box(0, -3, -4, 2, 6, 8).add();
        sym(b, "eye", "base", false, eye).accent(0x1A0A08).at(8.5f, -2, -8.5f).rot(0, -35, 0).mat("energy").glow("full")
                .box(-3, -3, -1.5f, 6, 6, 3).add();
        sym(b, "pupil", "eye", true, 0x140806).at(0, 0, -1.6f).mat("skin").box(-0.5f, -2, -0.5f, 1, 4, 1).add();
        sym(b, "lid", "eye", true, dark).at(0, -3, -1.6f).rot(15, 0, 0).mat("flesh").box(-3.5f, -1.5f, -1, 7, 2, 4).add();
        part(b, "head", "base", Role.HEAD, hide).accent(under).at(0, 0, -8).mat("flesh").pattern("blank")
                .box(-5, -3, -3, 10, 6, 3).add();
        part(b, "beak", "head", Role.JAW, 0x2A1A14).at(0, 2, -3).rot(20, 0, 0).mat("horn").box(-2, 0, -3, 4, 3, 3).add();
        // eight arms radiating from the base, drooping into the water
        for (int i = 0; i < 8; i++) {
            float yaw = 22.5f + i * 45;
            double r = Math.toRadians(yaw);
            float px = (float) (-Math.sin(r) * 7);
            float pz = (float) (-Math.cos(r) * 7);
            part(b, "arm" + i, "base", Role.TENTACLE, i % 2 == 0 ? hide : dark).accent(under).at(px, 3, pz).rot(28, yaw, 0)
                    .mat("flesh").pattern("spots").box(-2, -2, -10, 4, 4, 10).add();
            part(b, "arm" + i + "_tip", "arm" + i, Role.TENTACLE, hide).accent(under).at(0, 0, -10).rot(i % 2 == 0 ? 22 : -10, 0, 0)
                    .mat("flesh").pattern("spots").box(-1.5f, -1.5f, -9, 3, 3, 9).add();
            part(b, "arm" + i + "_end", "arm" + i + "_tip", Role.TENTACLE, under).at(0, 0, -9).rot(25, 0, 0).mat("flesh")
                    .box(-1, -1, -5, 2, 2, 5).add();
        }
        // two long feeding tentacles reaching up out of the water
        sym(b, "feeder", "base", false, hide).roles(Role.TENTACLE, Role.TENTACLE).accent(under).at(6, 0, -6).rot(-60, -25, -20)
                .mat("flesh").box(-1.5f, -1.5f, -12, 3, 3, 12).add();
        sym(b, "feeder_mid", "feeder", true, hide).roles(Role.TENTACLE, Role.TENTACLE).accent(under).at(0, 0, -12).rot(40, 0, 0)
                .mat("flesh").box(-1, -1, -10, 2, 2, 10).add();
        sym(b, "feeder_club", "feeder_mid", true, under).roles(Role.TENTACLE, Role.TENTACLE).accent(dark).at(0, 0, -10).rot(45, 0, 0)
                .mat("flesh").pattern("spots").box(-2, -1.5f, -5, 4, 3, 5).add();
        return b.build();
    }

    /**
     * Thermonuclear smoke devil: a floating, ash-grey devil that is mostly head, with great swept horns and bat
     * ears, burning yellow eyes, an underslung toothy jaw, stubby clawed arms and a body trailing off into smoke.
     */
    static ModelSpec thermonuclearSmokeDevil() {
        int hide = 0x5C5650;
        int dark = 0x3A3532;
        int smoke = 0x8A847C;
        int eye = 0xFFD848;
        ModelSpec.Builder b = ModelSpec.builder("thermonuclear_smoke_devil").shadow(1.1f);
        part(b, "body", null, Role.FLOAT, hide).accent(eye).at(0, 4, 0).mat("hide").box(-8, -8, -6, 16, 13, 12).add();
        part(b, "chest", "body", Role.STATIC, dark).at(0, -2, -6).mat("hide").box(-6, -5, -1, 12, 9, 2).add();
        part(b, "head", "body", Role.HEAD, hide).accent(eye).at(0, -7, -2).mat("hide").pattern("eyes").glow("eyes")
                .box(-8, -8, -9, 16, 10, 12).add();
        part(b, "brow", "head", Role.STATIC, dark).at(0, -6.5f, -9).rot(18, 0, 0).mat("hide").box(-8.5f, -1, -1.5f, 17, 2, 3).add();
        part(b, "nose", "head", Role.STATIC, dark).at(0, -3, -9.5f).mat("hide").box(-2, -1, -1.5f, 4, 3, 2).add();
        part(b, "jaw", "head", Role.JAW, dark).accent(0xFF9A30).at(0, 1, -3).rot(8, 0, 0).mat("hide").glow("maw")
                .box(-7.5f, 0, -8, 15, 5, 10).add();
        for (int k = 0; k < 4; k++) {
            part(b, "tooth" + k, "jaw", Role.STATIC, TOOTH).at(-5.25f + k * 3.5f, 0, -7.5f).mat("bone").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        }
        sym(b, "horn", "head", false, 0x2A2420).at(6, -7, -2).rot(-10, 0, 55).mat("horn").box(-1.5f, -6, -1.5f, 3, 6, 3).add();
        sym(b, "horn_mid", "horn", true, 0x3C342C).at(0, -6, 0).rot(-25, 0, -40).mat("horn").box(-1, -5, -1, 2, 5, 2).add();
        sym(b, "horn_tip", "horn_mid", true, 0xBFB6A0).at(0, -5, 0).rot(-20, 0, -30).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        sym(b, "ear", "head", false, hide).accent(0x9A5A50).at(8, -3, 0).rot(0, -25, -20).mat("membrane").box(0, -3, -1, 6, 6, 2).add();
        sym(b, "arm", "body", false, hide).roles(Role.ARM_LEFT, Role.ARM_RIGHT).at(8, -2, -2).rot(-40, 0, -20).mat("hide")
                .box(-1.5f, -1, -1.5f, 3, 6, 3).add();
        sym(b, "hand", "arm", true, dark).at(0, 6, 0).rot(-30, 0, 0).mat("hide").box(-2, 0, -2, 4, 2, 4).add();
        for (int k = 0; k < 3; k++) {
            sym(b, "talon" + k, "hand", true, 0xBFB6A0).at(-1.3f + k * 1.3f, 2, -1.5f).rot(30, 0, 0).mat("horn")
                    .box(-0.5f, 0, -0.5f, 1, 3, 1).add();
        }
        // the body billows away into a tapering column of smoke
        part(b, "smoke0", "body", Role.TAIL, hide).accent(smoke).at(0, 5, 1).rot(10, 0, 0).mat("smoke").box(-7, 0, -5, 14, 5, 11).add();
        part(b, "smoke1", "smoke0", Role.TAIL, smoke).at(0, 5, 1).rot(10, 0, 0).mat("smoke").box(-5, 0, -4, 10, 5, 9).add();
        part(b, "smoke2", "smoke1", Role.TAIL, smoke).at(0, 5, 0.5f).rot(12, 0, 0).mat("smoke").box(-3.5f, 0, -3, 7, 4, 6).add();
        part(b, "smoke3", "smoke2", Role.TAIL, 0xA8A298).at(0, 4, 0).rot(15, 0, 0).mat("smoke").box(-2, 0, -2, 4, 3, 4).add();
        sym(b, "puff", "body", false, smoke).at(7, 4, 3).mat("smoke").box(-2, -3, -3, 5, 5, 6).add();
        part(b, "puff_back", "body", Role.STATIC, smoke).at(0, -4, 6).mat("smoke").box(-5, -3, -1, 10, 6, 4).add();
        return b.build();
    }

    /**
     * The Nightmare: a towering, gaunt shade floating over a skirt of writhing tendrils, its hunched ribbed torso
     * dotted with glowing eyes, an elongated skull-like head with a fanged maw and a mane of tendrils swept back,
     * and long, thin arms ending in hooked claws.
     */
    static ModelSpec nightmare() {
        int body = 0x2E2838;
        int skin = 0x7A7286;
        int dark = 0x1C1824;
        int glow = 0xC07CFF;
        ModelSpec.Builder b = ModelSpec.builder("the_nightmare").shadow(1.3f);
        part(b, "pelvis", null, Role.FLOAT, body).accent(glow).at(0, 4, 2).mat("hide").box(-6, -3, -4, 12, 6, 8).add();
        for (int i = 0; i < 7; i++) {
            double r = Math.toRadians(i * 51.4f);
            float px = (float) (Math.sin(r) * 4.5f);
            float pz = (float) (Math.cos(r) * 3.5f);
            int len = 9 + (i * 3) % 4;
            part(b, "tatter" + i, "pelvis", Role.TENTACLE, i % 2 == 0 ? body : dark).accent(glow).at(px, 2, pz)
                    .rot((float) (Math.cos(r) * 12), 0, (float) (-Math.sin(r) * 14)).mat("cloth").box(-2, 0, -2, 4, len, 4).add();
            part(b, "tatter" + i + "_end", "tatter" + i, Role.TENTACLE, dark).at(0, len, 0)
                    .rot((float) (Math.cos(r) * 10), 0, (float) (-Math.sin(r) * 12)).mat("cloth").box(-1, 0, -1, 2, 18 - len, 2).add();
        }
        part(b, "torso", "pelvis", Role.BODY, body).accent(glow).at(0, -3, 0).rot(16, 0, 0).mat("hide").glow("spots")
                .box(-7, -16, -4, 14, 16, 8).add();
        part(b, "ribs", "torso", Role.STATIC, skin).at(0, -13, -4.3f).mat("bone").box(-5, 0, -0.5f, 10, 1, 1).box(-5.5f, 2.5f, -0.5f, 11, 1, 1)
                .box(-5, 5, -0.5f, 10, 1, 1).box(-4, 7.5f, -0.5f, 8, 1, 1).box(-0.5f, -1, -0.8f, 1, 10, 1).add();
        sym(b, "shoulder", "torso", false, dark).accent(glow).at(7, -14, 0).rot(0, 0, -20).mat("bone").box(-2, -3, -3, 5, 4, 6).add();
        sym(b, "shoulder_spike", "shoulder", true, skin).at(2, -3, 0).rot(-20, 0, 30).mat("bone").box(-0.5f, -6, -0.5f, 1, 6, 1).add();
        for (int i = 0; i < 4; i++) {
            part(b, "spine" + i, "torso", Role.STATIC, skin).at(0, -15 + i * 4, 4).rot(-55, 0, 0).mat("bone").box(-0.5f, -4, -0.5f, 1, 4, 1).add();
        }
        part(b, "neck", "torso", Role.NECK, skin).at(0, -16, -1).rot(-14, 0, 0).mat("skin").box(-2, -4, -2, 4, 5, 4).add();
        part(b, "head", "neck", Role.HEAD, skin).accent(glow).at(0, -4, -1).mat("bone").pattern("eyes").glow("eyes")
                .box(-4, -7, -7, 8, 8, 8).add();
        part(b, "cranium", "head", Role.STATIC, dark).accent(glow).at(0, -6, 0).rot(-38, 0, 0).mat("bone").box(-3.5f, -3, -1, 7, 4, 11).add();
        part(b, "cheeks", "head", Role.STATIC, dark).at(0, -1, -7).mat("bone").box(-4.8f, -1, -0.5f, 2, 3, 1).box(2.8f, -1, -0.5f, 2, 3, 1).add();
        part(b, "jaw", "head", Role.JAW, skin).accent(glow).at(0, 1, -2).rot(14, 0, 0).mat("bone").box(-4, 0, -6, 8, 3, 7).add();
        sym(b, "fang", "jaw", false, TOOTH).at(3, 0, -5.5f).mat("bone").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        for (int i = 0; i < 4; i++) {
            float x = -3 + i * 2f;
            part(b, "mane" + i, "cranium", Role.TENTACLE, i % 2 == 0 ? body : dark).accent(glow).at(x, -1, 7)
                    .rot(-20 + i % 2 * 12, (x) * 4, 0).mat("hair").box(-1, -1, 0, 2, 2, 10).add();
            part(b, "mane" + i + "_tip", "mane" + i, Role.TENTACLE, dark).at(0, 0, 10).rot(30, 0, 0).mat("hair").box(-0.5f, -0.5f, 0, 1, 1, 8).add();
        }
        // long gaunt arms with hooked claws
        sym(b, "arm", "torso", false, skin).roles(Role.ARM_LEFT, Role.ARM_RIGHT).at(8, -13, 0).rot(-30, 0, -14).mat("skin")
                .box(-1.5f, -1, -1.5f, 3, 13, 3).add();
        sym(b, "bracer", "arm", true, dark).accent(glow).at(0, 11, 0).mat("cloth").glow("rim").box(-2, 0, -2, 4, 2, 4).add();
        sym(b, "forearm", "arm", true, skin).at(0, 12, 0).rot(-55, 0, 6).mat("skin").box(-1.25f, 0, -1.25f, 2, 12, 2, 0.25f).add();
        sym(b, "hand", "forearm", true, dark).at(0, 12, 0).rot(-10, 0, 0).mat("skin").box(-2, 0, -1.5f, 4, 3, 3).add();
        for (int k = 0; k < 3; k++) {
            sym(b, "claw" + k, "hand", true, BONE).at(-1.3f + k * 1.3f, 3, -0.5f).rot(30, 0, 0).mat("bone")
                    .box(-0.5f, 0, -0.5f, 1, 7, 1).add();
        }
        return b.build();
    }

    /**
     * Verzik Vitur's final form: her pale, black-haired upper body grown out of the front of a bloated
     * blood-red spider, with eight long jointed legs, a spined abdomen glowing with red sores and grasping claws.
     */
    static ModelSpec verzikP3() {
        int carapace = 0x4A1622;
        int abdomen = 0x521A2E;
        int skin = 0xA296AA;
        int hair = 0x16121A;
        int leg = 0x2E1218;
        int sore = 0xFF3A3A;
        ModelSpec.Builder b = ModelSpec.builder("verzik_vitur_p3").shadow(1.7f);
        part(b, "body", null, Role.BODY, carapace).accent(sore).at(0, 5, 0).mat("chitin").box(-7, -4, -9, 14, 8, 15).add();
        part(b, "abdomen", "body", Role.STATIC, abdomen).accent(sore).at(0, -1, 5).rot(-28, 0, 0).mat("chitin").glow("spots")
                .box(-8, -6, 1, 16, 12, 14).box(-6, -8, 2, 12, 2, 12).box(-6, 6, 2, 12, 2, 11).box(-6, -4, 0, 12, 8, 1).add();
        part(b, "abdomen_tip", "abdomen", Role.STATIC, carapace).at(0, 0, 15).mat("chitin").box(-5, -4, 0, 10, 8, 2).box(-2, -2, 2, 4, 4, 2).add();
        for (int i = 0; i < 4; i++) {
            part(b, "spike" + i, "abdomen", Role.STATIC, BONE).at(0, -8, 3 + i * 3.5f).rot(-25 - i * 8, 0, 0).mat("bone")
                    .box(-1, -4, -1, 2, 4, 2).add();
            if (i < 3) {
                sym(b, "side_spike" + i, "abdomen", false, BONE).at(7.5f, -5, 4 + i * 4.5f).rot(0, 0, 50).mat("bone")
                        .box(-0.5f, -3, -0.5f, 1, 3, 1).add();
            }
        }
        // the humanoid torso rising from the front of the spider body
        part(b, "waist", "body", Role.STATIC, skin).at(0, -5, -6).rot(-6, 0, 0).mat("skin").box(-3.5f, -6, -2.5f, 7, 7, 5).add();
        part(b, "chest", "waist", Role.STATIC, skin).at(0, -6, 0).rot(-8, 0, 0).mat("skin").box(-5, -7, -3, 10, 7, 6).add();
        part(b, "collar", "chest", Role.STATIC, carapace).accent(sore).at(0, -7, 0).mat("chitin").box(-6, -1, -3.5f, 12, 2, 7).add();
        part(b, "head", "chest", Role.HEAD, skin).accent(sore).at(0, -8, -0.5f).mat("skin").pattern("eyes").glow("eyes")
                .box(-3.5f, -7, -3.5f, 7, 7, 7).add();
        part(b, "jaw", "head", Role.JAW, skin).accent(sore).at(0, -0.5f, -1.5f).mat("skin").box(-3, 0, -2, 6, 2, 3).add();
        part(b, "hair", "head", Role.STATIC, hair).at(0, -7.5f, 0).mat("hair").box(-4, 0, -4, 8, 2, 8).box(-4, 1, 1.5f, 8, 9, 3)
                .box(-4.2f, 1, -3, 1, 6, 5).box(3.2f, 1, -3, 1, 6, 5).add();
        part(b, "hair_tail", "hair", Role.TAIL, hair).at(0, 10, 3).rot(30, 0, 0).mat("hair").box(-3, 0, -1, 6, 8, 2).add();
        sym(b, "arm", "chest", false, skin).roles(Role.ARM_LEFT, Role.ARM_RIGHT).at(5.5f, -6, 0).rot(-50, 0, -18).mat("skin")
                .box(-1.25f, -1, -1.25f, 2, 9, 2, 0.25f).add();
        sym(b, "forearm", "arm", true, skin).at(0, 8.5f, 0).rot(-30, 0, 10).mat("skin").box(-1, 0, -1, 2, 8, 2).add();
        sym(b, "claw", "forearm", true, hair).at(0, 8, 0).rot(10, 0, 0).mat("chitin").box(-1.5f, 0, -1.5f, 3, 2, 3)
                .box(-1.5f, 2, -1, 1, 4, 1).box(0.5f, 2, -1, 1, 4, 1).add();
        // eight legs: femurs arch up and out, tibiae plunge to the ground
        float[] zs = {-8, -3, 2, 6};
        float[] yaws = {38, 14, -14, -38};
        for (int i = 0; i < 4; i++) {
            sym(b, "leg" + i, "body", false, leg).roles(Role.LEG_EXTRA, Role.LEG_EXTRA).accent(sore).at(6, -1, zs[i])
                    .rot(0, yaws[i], -55).mat("chitin").box(0, -1.5f, -1.5f, 14, 3, 3).add();
            sym(b, "leg" + i + "_knee", "leg" + i, true, BONE).at(14, 0, 0).mat("bone").box(-1.5f, -2.5f, -1, 3, 2, 2).add();
            sym(b, "leg" + i + "_shin", "leg" + i, true, leg).at(14, 0, 0).rot(0, 0, 40).mat("chitin")
                    .box(-1, 0, -1, 2, 29, 2).add();
            sym(b, "leg" + i + "_tip", "leg" + i + "_shin", true, BONE).at(0, 29, 0).mat("bone").box(-0.5f, 0, -0.5f, 1, 3, 1).add();
        }
        return b.build();
    }

    /**
     * TzKal-Zuk: a colossal obsidian TzHaar rising waist-up out of a rock mound, broad lava-veined chest,
     * spiked pauldrons, a low head with a burning maw and a tall fanned crown of horns, and massive arms with
     * fists planted on the rock.
     */
    static ModelSpec zuk() {
        int red = 0x8E2A1C;
        int deep = 0x5E1A16;
        int dark = 0x2A1214;
        int lava = 0xFF7A1A;
        ModelSpec.Builder b = ModelSpec.builder("tzkal_zuk").scale(2.6f).shadow(1.6f);
        part(b, "hips", null, Role.BODY, deep).accent(lava).at(0, 13, 0).mat("rock").box(-8, -4, -6, 16, 8, 11).add();
        part(b, "mound", "hips", Role.STATIC, dark).accent(lava).at(0, 11, 0).mat("rock").glow("cracks")
                .box(-14, -5, -9, 28, 5, 17).box(-11, -8, -7, 22, 3, 13).box(-15, -2, 4, 6, 2, 5).box(10, -3, -10, 5, 3, 5).add();
        part(b, "torso", "hips", Role.STATIC, red).accent(lava).at(0, -3, 0).rot(10, 0, 0).mat("rock").glow("cracks")
                .box(-9, -16, -6, 18, 16, 11).add();
        part(b, "pecs", "torso", Role.STATIC, deep).accent(lava).at(0, -14, -6).rot(-6, 0, 0).mat("rock")
                .box(-8, 0, -1.5f, 7, 6, 2).box(1, 0, -1.5f, 7, 6, 2).add();
        part(b, "abs", "torso", Role.STATIC, dark).accent(lava).at(0, -7, -6.2f).mat("rock")
                .box(-4, 0, -0.5f, 8, 2, 1).box(-4, 2.5f, -0.5f, 8, 2, 1).box(-3.5f, 5, -0.5f, 7, 2, 1).add();
        for (int i = 0; i < 4; i++) {
            part(b, "dorsal" + i, "torso", Role.STATIC, i % 2 == 0 ? deep : dark).accent(lava).at(0, -15 + i * 4, 5)
                    .rot(-50, 0, 0).mat("rock").box(-1.5f, -4, -1.5f, 3, 4, 3).add();
        }
        sym(b, "shoulder", "torso", false, red).accent(lava).at(10, -13, 0).rot(0, 0, 14).mat("rock")
                .box(-3, -4, -5.5f, 9, 8, 11).add();
        for (int k = 0; k < 3; k++) {
            sym(b, "pauldron_spike" + k, "shoulder", true, k == 1 ? dark : deep).accent(lava).at(2, -4, -3.5f + k * 3.5f)
                    .rot(-10 + k * 10, 0, 25 + k * 6).mat("rock").box(-1, -5 + (k == 1 ? -2 : 0), -1, 2, 5 + (k == 1 ? 2 : 0), 2).add();
        }
        part(b, "head", "torso", Role.HEAD, red).accent(lava).at(0, -15, -3).rot(-6, 0, 0).mat("rock").glow("eyes").pattern("eyes")
                .box(-5, -7, -7, 10, 8, 8).add();
        part(b, "brow", "head", Role.STATIC, deep).accent(lava).at(0, -6.5f, -7).rot(18, 0, 0).mat("rock").box(-5.5f, -1, -1.5f, 11, 2, 3).add();
        part(b, "jaw", "head", Role.JAW, dark).accent(lava).at(0, 1, -3).rot(8, 0, 0).mat("rock").glow("maw").box(-4.5f, 0, -5, 9, 3, 6).add();
        sym(b, "cheek_spike", "head", false, deep).accent(lava).at(5, -1, -3).rot(0, 0, 60).mat("rock").box(-1, -4, -1, 2, 4, 2).add();
        for (int i = -3; i <= 3; i++) {
            int h = 9 - Math.abs(i) * 2 + (i == 0 ? 2 : 0);
            String n = "crown" + (i + 3);
            part(b, n, "head", Role.STATIC, Math.abs(i) % 2 == 0 ? red : deep).accent(lava).at(i * 1.6f, -6.5f, 0)
                    .rot(-18, 0, i * 15).mat("rock").box(-1.5f, -h, -1.5f, 3, h, 3).add();
            part(b, n + "_tip", n, Role.STATIC, lava).at(0, -h, 0).rot(-10, 0, 0).mat("lava").glow("full").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        }
        sym(b, "arm", "torso", false, red).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(lava).at(13, -11, -1).rot(-12, 0, -10)
                .mat("rock").box(-3.5f, -1, -3.5f, 7, 10, 7).add();
        sym(b, "forearm", "arm", true, deep).accent(lava).at(0, 9, 0).rot(-45, 0, 6).mat("rock").glow("cracks").box(-4, 0, -4, 8, 9, 8).add();
        sym(b, "bracer", "forearm", true, dark).accent(lava).at(0, 2, 0).mat("rock").box(-4.5f, 0, -4.5f, 9, 3, 9).add();
        sym(b, "fist", "forearm", true, dark).accent(lava).at(0, 9, -0.5f).rot(40, 0, 0).mat("rock").box(-4.5f, 0, -4.5f, 9, 5, 9).add();
        return b.build();
    }

    /**
     * Kree'arra: Armadyl's towering aviansie general, dark-brown plumage with great spread wings that fade to
     * pale primaries, a white Armadyl kilt and gilded chest plate, a crested raptor head with a hooked golden beak,
     * arms held forward as if levelling a crossbow, and golden talons.
     */
    static ModelSpec kreearra() {
        int brown = 0x5C3C24;
        int dark = 0x3A2616;
        int tan = 0xB89468;
        int pale = 0xE4DCC8;
        int white = 0xECE8DC;
        int gold = 0xC0922C;
        int beak = 0xE8B83A;
        ModelSpec.Builder b = ModelSpec.builder("kreearra").shadow(1.4f);
        part(b, "hips", null, Role.BODY, brown).at(0, 11, 0).mat("feathers").box(-6, -3, -4, 12, 6, 8).add();
        part(b, "kilt", "hips", Role.STATIC, white).accent(gold).at(0, 1, 0).mat("cloth").box(-7, 0, -5, 14, 7, 10).add();
        part(b, "kilt_trim", "kilt", Role.STATIC, gold).at(0, 6.5f, 0).mat("metal").box(-7.5f, 0, -5.5f, 15, 1, 11).add();
        part(b, "belt", "hips", Role.STATIC, gold).at(0, 0, 0).mat("metal").box(-7, -1, -4.5f, 14, 2, 9).add();
        part(b, "torso", "hips", Role.STATIC, brown).accent(tan).at(0, -3, 0).rot(12, 0, 0).mat("feathers")
                .box(-7, -12, -5, 14, 12, 10).add();
        part(b, "chestplate", "torso", Role.STATIC, gold).accent(0x7FE8FF).at(0, -11, -5).mat("metal").glow("runes")
                .box(-6, 0, -1.5f, 12, 8, 2).add();
        part(b, "breast", "torso", Role.STATIC, tan).at(0, -3, -5).mat("feathers").box(-5, 0, -1, 10, 3, 1).add();
        sym(b, "pauldron", "torso", false, gold).at(7, -11, 0).rot(0, 0, 18).mat("metal").box(-2, -2, -4, 6, 4, 8).add();
        part(b, "neck", "torso", Role.NECK, brown).at(0, -12, -1).rot(-10, 0, 0).mat("feathers").box(-3, -4, -3, 6, 5, 6).add();
        part(b, "head", "neck", Role.HEAD, dark).accent(beak).at(0, -4, -1).rot(10, 0, 0).mat("feathers").pattern("eyes").glow("eyes")
                .box(-4, -8, -5, 8, 8, 8).add();
        part(b, "beak", "head", Role.STATIC, beak).at(0, -2, -5).rot(10, 0, 0).mat("horn").box(-1.5f, -1, -4, 3, 3, 4).add();
        part(b, "beak_hook", "beak", Role.STATIC, 0xC08A20).at(0, 2, -4).rot(30, 0, 0).mat("horn").box(-1, -1, -1.5f, 2, 3, 2).add();
        part(b, "jaw", "head", Role.JAW, 0xC08A20).at(0, 0.5f, -4).rot(5, 0, 0).mat("horn").box(-1, 0, -3, 2, 1, 3).add();
        for (int i = 0; i < 3; i++) {
            part(b, "crest" + i, "head", Role.STATIC, i == 1 ? tan : dark).at(-1.5f + i * 1.5f, -8, 1).rot(-60 + i * 8, 0, (i - 1) * 12)
                    .mat("feathers").box(-0.5f, -6 + (i == 1 ? -2 : 0), -1, 1, 6 + (i == 1 ? 2 : 0), 3).add();
        }
        // great wings: inner arm + coverts, outer arm + pale panel, fanned primaries
        sym(b, "wing", "torso", false, dark).roles(Role.WING_LEFT, Role.WING_RIGHT).at(5, -10, 4).rot(0, -18, -28)
                .mat("feathers").box(0, -1.5f, -1, 12, 3, 3).add();
        sym(b, "wing_coverts", "wing", true, brown).at(0, 1.5f, 0.5f).mat("feathers").box(0, 0, -0.5f, 12, 8, 1).add();
        sym(b, "wing_outer", "wing", true, dark).at(12, 0, 0).rot(0, 0, 18).mat("feathers").box(0, -1, -1, 10, 2, 2).add();
        sym(b, "wing_panel", "wing_outer", true, tan).at(0, 1, 0.5f).mat("feathers").box(0, 0, -0.5f, 10, 9, 1).add();
        for (int k = 0; k < 4; k++) {
            sym(b, "primary" + k, "wing_outer", true, k % 2 == 0 ? pale : tan).at(9, 1 + k * 2.2f, 0.5f).rot(0, 0, 8 + k * 16)
                    .mat("feathers").box(0, -1, -0.5f, 9 - k, 2, 1).add();
        }
        for (int k = 0; k < 4; k++) {
            sym(b, "secondary" + k, "wing", true, k % 2 == 0 ? tan : pale).at(2 + k * 3, 9, 1).rot(0, 0, 80 - k * 6)
                    .mat("feathers").box(0, -1, -0.5f, 5, 2, 1).add();
        }
        // arms held forward, as if aiming a crossbow
        sym(b, "arm", "torso", false, brown).roles(Role.ARM_LEFT, Role.ARM_RIGHT).at(7.5f, -9, -1).rot(-55, 0, -8).mat("feathers")
                .box(-1.5f, -1, -1.5f, 3, 8, 3).add();
        sym(b, "bracer", "arm", true, gold).at(0, 7, 0).rot(-30, 0, 4).mat("metal").box(-2, 0, -2, 4, 5, 4).add();
        sym(b, "hand", "bracer", true, beak).at(0, 5, 0).mat("horn").box(-1.5f, 0, -1.5f, 3, 2, 3).box(-1.5f, 2, -1.5f, 1, 2, 1)
                .box(0.5f, 2, -1.5f, 1, 2, 1).add();
        // feathered thighs, scaly golden shanks and talons
        sym(b, "leg", "hips", false, brown).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).at(3.5f, 2, 0).mat("feathers")
                .box(-2.5f, -1, -2.5f, 5, 6, 5).add();
        sym(b, "shank", "leg", true, beak).at(0, 5, 0.5f).mat("scales").box(-1, 0, -1, 2, 5, 2).add();
        sym(b, "foot", "shank", true, gold).at(0, 5, 0).mat("horn").box(-2, 0, -4, 4, 1, 5).add();
        for (int k = 0; k < 3; k++) {
            sym(b, "talon" + k, "foot", true, 0x2A2018).at(-1.3f + k * 1.3f, 0.5f, -4).rot(40, 0, 0).mat("horn")
                    .box(-0.5f, -0.5f, -2, 1, 1, 2).add();
        }
        return b.build();
    }

    /**
     * General Graardor: Bandos's hulking green ogre general in riveted bronze plate, spiked pauldrons, a heavy
     * brow and tusked underbite, a fur-trimmed war-kilt, and a spiked ball-and-chain flail in his right hand.
     */
    static ModelSpec graardor() {
        int skin = 0x5E7A32;
        int skinDark = 0x435A22;
        int bronze = 0x7A5C34;
        int bronzeDark = 0x4E3A22;
        int leather = 0x5A3A22;
        int fur = 0x7A6A54;
        int spike = 0xD8D0B8;
        int steel = 0x7A7C82;
        ModelSpec.Builder b = ModelSpec.builder("general_graardor").shadow(1.0f);
        part(b, "hips", null, Role.BODY, leather).at(0, 10, 0).mat("leather").box(-8, -3, -5, 16, 6, 10).add();
        part(b, "belt", "hips", Role.STATIC, bronzeDark).at(0, -2, 0).mat("metal").box(-8.5f, -1, -5.5f, 17, 3, 11).add();
        part(b, "buckle", "belt", Role.STATIC, bronze).at(0, 0.5f, -5.6f).mat("metal").box(-2, -1.5f, -0.5f, 4, 3, 1).add();
        part(b, "kilt_front", "hips", Role.STATIC, leather).accent(fur).at(0, 2, -5).rot(-4, 0, 0).mat("leather").box(-4, 0, -0.5f, 8, 7, 1).add();
        part(b, "kilt_fur", "hips", Role.STATIC, fur).at(0, 2, 0).mat("fur").box(-8.5f, 0, -4.5f, 17, 3, 10).add();
        part(b, "torso", "hips", Role.STATIC, skin).at(0, -3, 0).rot(10, 0, 0).mat("skin").box(-10, -13, -6, 20, 13, 12).add();
        part(b, "chestplate", "torso", Role.STATIC, bronze).at(0, -12, -6).mat("metal").box(-9, 0, -1.5f, 18, 7, 2)
                .box(-8, 7.5f, -1.2f, 16, 2, 1).box(-7, 10, -1.2f, 14, 2, 1).add();
        part(b, "backplate", "torso", Role.STATIC, bronzeDark).at(0, -12, 6).mat("metal").box(-9, 0, -0.5f, 18, 10, 2).add();
        part(b, "gorget", "torso", Role.STATIC, bronzeDark).at(0, -13, 0).mat("metal").box(-6, -1, -5, 12, 2, 10).add();
        sym(b, "pauldron", "torso", false, bronze).at(11, -12, 0).rot(0, 0, 14).mat("metal").box(-4, -3, -5.5f, 9, 5, 11)
                .box(-3, 2, -5, 8, 2, 10).add();
        for (int k = 0; k < 3; k++) {
            sym(b, "spike" + k, "pauldron", true, spike).at(1 + k * 1.2f, -3, -3.5f + k * 3.5f).rot(0, 0, 18 + k * 8).mat("bone")
                    .box(-1, -5 + (k == 1 ? -1 : 0), -1, 2, 5 + (k == 1 ? 1 : 0), 2).add();
        }
        part(b, "head", "torso", Role.HEAD, skin).accent(0xFFD040).at(0, -13, -4).rot(-8, 0, 0).mat("skin").pattern("eyes")
                .box(-5, -8, -6, 10, 8, 8).add();
        part(b, "brow", "head", Role.STATIC, skinDark).at(0, -6.8f, -6).rot(14, 0, 0).mat("skin").box(-5.5f, -1, -1.5f, 11, 2, 3).add();
        part(b, "nose", "head", Role.STATIC, skinDark).at(0, -2.5f, -6.5f).mat("skin").box(-1.5f, -1, -1, 3, 3, 2).add();
        part(b, "helm", "head", Role.STATIC, bronzeDark).at(0, -8, 0).mat("metal").box(-5.5f, -2, -6.5f, 11, 3, 9).add();
        part(b, "helm_crest", "helm", Role.STATIC, spike).at(0, -2, -1).rot(-20, 0, 0).mat("bone").box(-1, -4, -1, 2, 4, 3).add();
        sym(b, "ear", "head", false, skin).at(5, -4, -2).rot(0, -20, 30).mat("skin").box(0, -1.5f, -1, 4, 3, 2).add();
        part(b, "jaw", "head", Role.JAW, skin).at(0, 0, -2).rot(4, 0, 0).mat("skin").box(-5.5f, 0, -5.5f, 11, 4, 8).add();
        sym(b, "tusk", "jaw", false, spike).at(3.5f, 0, -5).rot(-10, 0, 10).mat("horn").box(-0.75f, -4, -0.75f, 1, 4, 1, 0.25f).add();
        // arms: bare green upper arms, bronze vambraces, big fists
        sym(b, "arm", "torso", false, skin).roles(Role.ARM_LEFT, Role.ARM_RIGHT).at(13, -10, -1).rot(-15, 0, -14).mat("skin")
                .box(-3, -1, -3, 6, 10, 6).add();
        sym(b, "vambrace", "arm", true, bronze).at(0, 9, 0).rot(-30, 0, 6).mat("metal").box(-3.5f, 0, -3.5f, 7, 7, 7).add();
        sym(b, "vambrace_spike", "vambrace", true, spike).at(3.5f, 3.5f, 0).rot(0, 0, 90).mat("bone").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        sym(b, "fist", "vambrace", true, skinDark).at(0, 7, 0).mat("skin").box(-3, 0, -3, 6, 5, 6).add();
        // the flail, held in his right hand: haft, chain links, spiked ball
        part(b, "flail_haft", "fist_r", Role.STATIC, leather).at(0, 2.5f, -1).rot(90, 0, 0).mat("wood").box(-1, -10, -1, 2, 14, 2).add();
        part(b, "flail_cap", "flail_haft", Role.STATIC, steel).at(0, -10, 0).mat("metal").box(-1.5f, -2, -1.5f, 3, 2, 3).add();
        part(b, "flail_chain", "flail_cap", Role.TAIL, steel).at(0, -1, 0).rot(-45, 0, 0).mat("chain").box(-0.5f, 0, -0.5f, 1, 5, 1, 0.2f).add();
        part(b, "flail_ball", "flail_chain", Role.STATIC, steel).at(0, 5, 0).mat("metal").box(-3, 0, -3, 6, 6, 6).add();
        part(b, "flail_spikes", "flail_ball", Role.STATIC, spike).at(0, 3, 0).mat("bone").box(-0.5f, -5, -0.5f, 1, 2, 1)
                .box(-0.5f, 3, -0.5f, 1, 2, 1).box(-5, -0.5f, -0.5f, 2, 1, 1).box(3, -0.5f, -0.5f, 2, 1, 1)
                .box(-0.5f, -0.5f, -5, 1, 1, 2).box(-0.5f, -0.5f, 3, 1, 1, 2).add();
        // legs: green thighs, bronze greaves, broad feet
        sym(b, "leg", "hips", false, skin).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).at(5, 2, 0).mat("skin")
                .box(-3.5f, -1, -3.5f, 7, 7, 7).add();
        sym(b, "greave", "leg", true, bronze).at(0, 6, 0).mat("metal").box(-3, 0, -3, 6, 4, 6).add();
        sym(b, "foot", "greave", true, leather).at(0, 4, 0).mat("leather").box(-3.5f, 0, -5, 7, 2, 8).add();
        return b.build();
    }

    /**
     * Vorkath: the undead frost dragon, slate-grey rotting hide torn open over white exposed ribs with a frozen
     * blue glow inside, a bare horned skull with burning blue eyes, tattered wings and a spined bony tail.
     */
    static ModelSpec vorkath() {
        int hide = 0x46483E;
        int dark = 0x2A2B26;
        int frost = 0x7FE0FF;
        ModelSpec.Builder b = ModelSpec.builder("vorkath").shadow(1.6f);
        part(b, "body", null, Role.BODY, hide).accent(frost).at(0, 9, 4).mat("hide").glow("cracks").box(-8, -6, -12, 16, 11, 24).add();
        part(b, "cavity", "body", Role.STATIC, 0x1E2C36).accent(frost).at(0, 0, -2).mat("flesh").glow("spots")
                .box(-8.6f, -4, -7, 1, 8, 12).box(7.6f, -4, -7, 1, 8, 12).add();
        for (int i = 0; i < 5; i++) {
            sym(b, "rib" + i, "body", false, BONE).at(8.4f, -5, -7.5f + i * 3).rot(0, 0, 8).mat("bone")
                    .box(-0.5f, 0, -0.5f, 1, 10, 1).add();
        }
        part(b, "keel", "body", Role.STATIC, BONE).at(0, 5, -4).mat("bone").box(-1, 0, -8, 2, 1, 14).add();
        for (int i = 0; i < 6; i++) {
            part(b, "spine" + i, "body", Role.STATIC, BONE).at(0, -6, -10 + i * 4.2f).rot(-30, 0, 0).mat("bone")
                    .box(-1, -3 - (i % 2), -1, 2, 3 + (i % 2), 2).add();
        }
        part(b, "neck", "body", Role.NECK, hide).accent(frost).at(0, -3, -11).rot(-38, 0, 0).mat("hide").glow("cracks").box(-3, -3, -11, 6, 6, 11).add();
        part(b, "neck_bones", "neck", Role.STATIC, BONE).at(0, -3.2f, 0).mat("bone").box(-1, -1, -10, 2, 1, 9).box(-0.5f, -3, -9, 1, 2, 1)
                .box(-0.5f, -3, -6, 1, 2, 1).box(-0.5f, -3, -3, 1, 2, 1).add();
        sym(b, "neck_rib", "neck", false, BONE).at(3.1f, -1, -6).mat("bone").box(-0.5f, -1.5f, -3, 1, 4, 1).box(-0.5f, -1.5f, 0, 1, 4, 1).add();
        part(b, "head", "neck", Role.HEAD, BONE).accent(frost).at(0, 0, -11).rot(38, 0, 0).mat("bone").pattern("skull").glow("eyes")
                .box(-4, -5, -8, 8, 8, 8).add();
        part(b, "snout", "head", Role.STATIC, BONE).at(0, 2, -8).mat("bone").box(-2.5f, -1.5f, -6, 5, 3, 6).add();
        sym(b, "nostril", "snout", false, 0x2A2A30).at(1.2f, -1, -6.1f).mat("bone").box(-0.5f, -0.5f, -0.5f, 1, 1, 1).add();
        part(b, "jaw", "head", Role.JAW, 0xC8C0AC).accent(frost).at(0, 3, -2).rot(6, 0, 0).mat("bone").box(-3, 0, -12, 6, 2, 12).add();
        sym(b, "horn", "head", false, BONE).at(3, -4, 0).rot(-55, 0, 20).mat("bone").box(-1, -1, 0, 2, 2, 8).add();
        sym(b, "horn_tip", "horn", true, 0xBDB4A0).at(0, 0, 8).rot(-25, 0, 0).mat("bone").box(-0.5f, -0.5f, 0, 1, 1, 5).add();
        // tattered wings (one flapping pair)
        sym(b, "wing", "body", false, BONE).roles(Role.WING_LEFT, Role.WING_RIGHT).at(7, -5, -6).rot(0, -10, -30).mat("bone")
                .box(0, -1, -1, 16, 2, 2).add();
        sym(b, "wing_web", "wing", true, dark).accent(hide).at(0, 1, 0.5f).mat("membrane").box(0, 0, -0.5f, 16, 9, 1).add();
        sym(b, "wing_outer", "wing", true, BONE).at(16, 0, 0).rot(0, 0, 35).mat("bone").box(0, -1, -1, 14, 2, 2).add();
        sym(b, "wing_web_outer", "wing_outer", true, dark).accent(hide).at(0, 1, 0.5f).mat("membrane").box(0, 0, -0.5f, 12, 7, 1).add();
        for (int k = 0; k < 3; k++) {
            sym(b, "finger" + k, "wing_outer", true, BONE).at(4 + k * 4, 1, 0).rot(0, 0, 70 - k * 12).mat("bone")
                    .box(0, -0.5f, -0.5f, 9 - k * 2, 1, 1).add();
        }
        // legs: hide thighs, bony lower legs, clawed feet
        sym(b, "fleg", null, false, hide).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(frost).at(6.5f, 12, -4).mat("hide").glow("cracks")
                .box(-2.5f, -2, -3, 5, 7, 6).add();
        sym(b, "fshin", "fleg", true, BONE).at(0, 5, 0).mat("bone").box(-1.5f, 0, -1.5f, 3, 6, 3).add();
        sym(b, "ffoot", "fshin", true, dark).at(0, 6, 0).mat("hide").box(-2.5f, 0, -4, 5, 1, 6).box(-2.5f, 0, -5, 1, 1, 1)
                .box(1.5f, 0, -5, 1, 1, 1).add();
        sym(b, "bleg", null, false, hide).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(frost).at(6.5f, 11, 13).mat("hide").glow("cracks")
                .box(-3, -2, -3.5f, 6, 8, 7).add();
        sym(b, "bshin", "bleg", true, BONE).at(0, 6, 1).rot(15, 0, 0).mat("bone").box(-1.5f, 0, -1.5f, 3, 6, 3).add();
        sym(b, "bfoot", "bshin", true, dark).at(0, 6, 0).rot(-15, 0, 0).mat("hide").box(-2.5f, 0, -4, 5, 1, 6).add();
        // tail: tapering hide over vertebrae, ending in a bony spike
        String parent = "body";
        int[] w = {5, 4, 3, 2};
        for (int i = 0; i < 4; i++) {
            String n = "tail" + i;
            part(b, n, parent, Role.TAIL, i % 2 == 0 ? hide : dark).accent(frost).glow(i < 2 ? "cracks" : "none").at(0, i == 0 ? -2 : 0, i == 0 ? 12 : 8).rot(i == 0 ? 12 : 6, 0, 0)
                    .mat("hide").box(-w[i] / 2f, -w[i] / 2f, 0, w[i], w[i], 8).add();
            part(b, n + "_spike", n, Role.STATIC, BONE).at(0, -w[i] / 2f, 4).rot(-35, 0, 0).mat("bone").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
            parent = n;
        }
        part(b, "tail_blade", parent, Role.STATIC, BONE).at(0, 0, 8).mat("bone").box(-1.5f, -0.5f, 0, 3, 1, 4).add();
        return b.build();
    }

    /**
     * Cerberus: the three-headed guardian hound of the River of Souls, black-brown hide split by molten seams,
     * a broad spined back, three snarling heads with burning eyes and maws, spiked iron collars trailing broken
     * chains, and a whip of a lava-tipped tail.
     */
    static ModelSpec cerberus() {
        int hide = 0x5C3C2C;
        int dark = 0x382218;
        int lava = 0xE8561A;
        int iron = 0x8A8A92;
        ModelSpec.Builder b = ModelSpec.builder("cerberus").shadow(1.7f);
        part(b, "body", null, Role.BODY, hide).accent(lava).at(0, 9, 3).mat("fur").glow("cracks")
                .box(-8, -7, -13, 16, 13, 13).box(-7, -5, 0, 14, 10, 13).add();
        for (int i = 0; i < 6; i++) {
            part(b, "spine" + i, "body", Role.STATIC, dark).accent(lava).at(0, -7 + (i > 2 ? 2 : 0), -11 + i * 4.5f).rot(-30, 0, 0)
                    .mat("horn").box(-1, -3, -1, 2, 3, 2).add();
        }
        // three heads on short thick necks, each in a spiked iron collar with a hanging chain
        float[][] heads = {{0, -2, 0}, {8, 1, -40}, {-8, 1, 40}};
        String[] names = {"mid", "left", "right"};
        for (int i = 0; i < 3; i++) {
            float[] h = heads[i];
            String n = names[i];
            part(b, "neck_" + n, "body", Role.NECK, hide).accent(lava).at(h[0], -3 + h[1], -12).rot(-25, h[2], 0).mat("fur")
                    .box(-3, -3, -9, 6, 6, 9).add();
            part(b, "collar_" + n, "neck_" + n, Role.STATIC, iron).at(0, 0, -4).mat("metal").box(-3.5f, -3.5f, -1, 7, 7, 2).add();
            part(b, "collar_spikes_" + n, "collar_" + n, Role.STATIC, 0xBFBFC4).at(0, 0, 0).mat("metal")
                    .box(-0.5f, -5.5f, -0.5f, 1, 2, 1).box(3.5f, -0.5f, -0.5f, 2, 1, 1).box(-5.5f, -0.5f, -0.5f, 2, 1, 1).add();
            part(b, "chain_" + n, "collar_" + n, Role.TAIL, iron).at(0, 3.5f, 0).rot(10, 0, 0).mat("chain")
                    .box(-0.5f, 0, -0.5f, 1, 7 + i, 1, 0.2f).add();
            part(b, "head_" + n, "neck_" + n, Role.HEAD, hide).accent(lava).at(0, -1, -9).rot(25, 0, 0).mat("fur").pattern("eyes")
                    .glow("eyes").box(-3.5f, -4, -5, 7, 6, 6).add();
            part(b, "snout_" + n, "head_" + n, Role.STATIC, dark).at(0, -0.5f, -5).mat("hide").box(-2.5f, -1.5f, -4, 5, 3, 4).add();
            part(b, "jaw_" + n, "head_" + n, Role.JAW, dark).accent(lava).at(0, 1.5f, -2).rot(12, 0, 0).mat("hide")
                    .box(-2.5f, 0, -7, 5, 2, 7).add();
            sym(b, "ear_" + n, "head_" + n, false, dark).at(2.5f, -4, -1).rot(-25, 0, 20).mat("hide").box(-1, -3, -1, 2, 3, 2).add();
        }
        // legs
        sym(b, "fleg", null, false, hide).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(lava).at(6, 11, -6).mat("fur")
                .box(-2.5f, -2, -3, 5, 8, 6).add();
        sym(b, "fshin", "fleg", true, hide).at(0, 6, 0).mat("hide").box(-2, 0, -2, 4, 6, 4).add();
        sym(b, "fpaw", "fshin", true, dark).at(0, 6, 0).mat("hide").box(-2.5f, 0, -4, 5, 1, 6).box(-2.5f, 0, -5, 1, 1, 1)
                .box(-0.5f, 0, -5, 1, 1, 1).box(1.5f, 0, -5, 1, 1, 1).add();
        sym(b, "bleg", null, false, hide).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(lava).at(5.5f, 11, 12).mat("fur")
                .box(-3, -2, -3.5f, 6, 8, 7).add();
        sym(b, "bshin", "bleg", true, hide).at(0, 6, 1).rot(18, 0, 0).mat("hide").box(-2, 0, -2, 4, 6, 4).add();
        sym(b, "bpaw", "bshin", true, dark).at(0, 6, 0).rot(-18, 0, 0).mat("hide").box(-2.5f, 0, -4, 5, 1, 6).add();
        part(b, "tail", "body", Role.TAIL, hide).accent(lava).at(0, -3, 13).rot(-35, 0, 0).mat("hide").box(-1.5f, -1.5f, 0, 3, 3, 8).add();
        part(b, "tail_mid", "tail", Role.TAIL, hide).accent(lava).at(0, 0, 8).rot(25, 0, 0).mat("hide").box(-1, -1, 0, 2, 2, 7).add();
        part(b, "tail_tip", "tail_mid", Role.TAIL, lava).at(0, 0, 7).mat("lava").glow("full").box(-1.5f, -1.5f, 0, 3, 3, 3).add();
        return b.build();
    }
}
