package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.BestiaryWldBeasts.arachLeg;
import static com.runecraft.osrs.content.bestiary.BestiaryWldBeasts.legPair;
import static com.runecraft.osrs.content.bestiary.BestiaryWldFig.dk;
import static com.runecraft.osrs.content.bestiary.BestiaryWldFig.side;
import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/** Island and Varlamore wildlife: suqah, jungle horrors, rock lobsters, mudskippers and blood jaguars. */
final class BestiaryWldIsles {
    private BestiaryWldIsles() {
    }

    static List<ModelSpec> all() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(suqah("suqah"));
        l.add(jungleHorror("jungle_horror"));
        l.add(rockLobster("rock_lobster"));
        l.add(mudskipper("mudskipper"));
        l.add(jaguar("blood_jaguar", 0x9A2A1E, 0x5A1210, 0xE0B040));
        return l;
    }

    /** Suqah: a hunched, shaggy Lunar Isle brute with a thick mane, ram horns, a tusked snout and long clawed arms. */
    static ModelSpec suqah(String id) {
        int fur = 0x8A5A3A;
        int mane = 0x5A3A26;
        int horn = 0xD8CCA8;
        ModelSpec.Builder b = BestiaryWldFig.of(id).legs(10, 5).torso(10, 10, 13, 8, 12).arms(5, 7, 7, 6).head(8, 7, 8).foot(6)
                .hunch(28).stance(5).skin(fur, "fur", -1).face("eyes").eyeColour(0xF0D040).hair(0, "none").nose("none").ears("small")
                .jaw("tusks").brow().top(fur, "bare").bottoms(mane, "loincloth").boots(dk(fur, 0.8f), "bare").belt(-1)
                .pose(-24, 12, 20).shadow(1.0f).builder();
        part(b, "mane", "torso", Role.STATIC, mane).at(0, -12, 0.5f).rot(-10, 0, 0).mat("hair")
                .box(-7, -2, -4.5f, 14, 5, 9).box(-6, 3, 0, 12, 5, 4).add();
        part(b, "mane_back", "mane", Role.STATIC, dk(mane, 0.9f)).at(0, 3, 4).rot(20, 0, 0).mat("hair").box(-5, 0, -1, 10, 7, 2).add();
        part(b, "snout", "head", Role.STATIC, dk(fur, 1.15f)).at(0, -2.5f, -4).rot(10, 0, 0).mat("fur").box(-2.5f, -1.5f, -3, 5, 3, 3).add();
        part(b, "nose", "snout", Role.STATIC, 0x2A1A14).at(0, -1.5f, -3).mat("skin").box(-1, 0, -0.6f, 2, 1, 1).add();
        part(b, "head_mane", "head", Role.STATIC, mane).at(0, -7, 1).rot(-15, 0, 0).mat("hair").box(-4.5f, -1.5f, -3, 9, 3, 7).add();
        for (boolean left : new boolean[]{true, false}) {
            side(part(b, "horn", "head", Role.STATIC, horn).at(3.5f, -6, 0).rot(30, 0, -60).mat("horn").box(-1, -3, -1, 2, 3, 2), left);
            side(part(b, "horn_curl", "horn*", Role.STATIC, dk(horn, 0.9f)).at(0, -3, 0).rot(70, 0, 20).mat("horn").box(-1, -3, -1, 2, 3, 2, -0.2f), left);
            side(part(b, "horn_tip", "horn_curl*", Role.STATIC, dk(horn, 0.8f)).at(0, -3, 0).rot(70, 0, 0).mat("horn").box(-0.5f, -2, -0.5f, 1, 2, 1), left);
            side(part(b, "claws", "fingers*", Role.STATIC, 0x2A2420).at(0, 1.5f, -0.5f).rot(30, 0, 0).mat("horn")
                    .box(-3, 0, -0.5f, 1, 2, 1).box(-1, 0, -0.5f, 1, 2, 1).box(1, 0, -0.5f, 1, 2, 1), left);
        }
        part(b, "tail", "hips", Role.TAIL, fur).at(0, -1, 2.5f).rot(30, 0, 0).mat("fur").box(-1, -1, 0, 2, 2, 6).add();
        part(b, "tail_tuft", "tail", Role.STATIC, mane).at(0, 0, 6).mat("hair").box(-1.5f, -1.5f, 0, 3, 3, 3).add();
        return b.build();
    }

    /** Jungle horror: a gaunt grey-green ape-thing with a huge underslung jaw of fangs, long arms and a ridge of spines. */
    static ModelSpec jungleHorror(String id) {
        int skin = 0x6A7A5A;
        ModelSpec.Builder b = BestiaryWldFig.of(id).legs(11, 4).torso(8, 7, 11, 6, 11).arms(4, 7, 7, 5).head(8, 7, 8).foot(6)
                .hunch(30).stance(5).skin(skin, "hide", -1).face("eyes").eyeColour(0xE0F040).hair(0x3A4430, "wild").nose("broad")
                .ears("pointy").jaw("underbite").brow().top(skin, "bare").bottoms(0x3A3A26, "rags").boots(skin, "bare").belt(-1)
                .pose(-30, 12, 22).shadow(0.8f).builder();
        for (int i = 0; i < 4; i++) {
            part(b, "spine" + i, "torso", Role.STATIC, 0x3A4430).at(0, -11 + i * 3, 3).rot(-40, 0, 0).mat("horn")
                    .box(-0.5f, -3, -0.5f, 1, 3, 2).add();
        }
        for (boolean left : new boolean[]{true, false}) {
            side(part(b, "claws", "fingers*", Role.STATIC, 0x1E1E18).at(0, 1.5f, -0.5f).rot(35, 0, 0).mat("horn")
                    .box(-2.5f, 0, -0.5f, 1, 3, 1, -0.1f).box(-0.5f, 0, -0.5f, 1, 3, 1, -0.1f).box(1.5f, 0, -0.5f, 1, 3, 1, -0.1f), left);
        }
        part(b, "lower_fangs", "jaw", Role.STATIC, 0xF0E8D0).at(0, 0, -2).mat("horn")
                .box(-2.5f, -1.5f, -0.6f, 1, 2, 1).box(-0.5f, -1, -0.6f, 1, 1, 1).box(1.5f, -1.5f, -0.6f, 1, 2, 1).add();
        return b.build();
    }

    /** Rock lobster: a dark, rocky-shelled lobster with huge knobbed crushers, a fanned segmented tail and long antennae. */
    static ModelSpec rockLobster(String id) {
        int shell = 0x4A4448;
        int rust = 0x8A3A2A;
        int light = 0x7A6A66;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.0f);
        part(b, "body", null, Role.BODY, shell).accent(rust).at(0, 18, -1).mat("rock").box(-4.5f, -3.5f, -6, 9, 6, 10).add();
        part(b, "carapace_ridge", "body", Role.STATIC, light).at(0, -3.5f, -1).mat("rock").box(-3, -1, -5, 6, 1, 9).add();
        part(b, "head", "body", Role.HEAD, shell).at(0, -1, -6).rot(10, 0, 0).mat("rock").box(-3, -2, -3, 6, 4, 3).add();
        part(b, "rostrum", "head", Role.STATIC, rust).at(0, -1.5f, -3).rot(-10, 0, 0).mat("horn").box(-0.5f, -0.5f, -3, 1, 1, 3).add();
        part(b, "mouth", "head", Role.JAW, dk(shell, 0.6f)).at(0, 1.5f, -3).mat("chitin").box(-1.5f, 0, -0.6f, 3, 1, 1).add();
        for (boolean left : new boolean[]{true, false}) {
            side(part(b, "eye", "head", Role.STATIC, 0x101010).at(2, -2, -2).mat("energy").box(-0.5f, -1.5f, -0.5f, 1, 2, 1, 0.1f), left);
            side(part(b, "antenna", "head", Role.STATIC, rust).at(1.5f, -1, -3).rot(-25, -25, 0).mat("horn").box(-0.5f, -0.5f, -9, 1, 1, 9, -0.2f), left);
            side(part(b, "antenna_tip", "antenna*", Role.STATIC, rust).at(0, 0, -9).rot(30, 20, 0).mat("horn").box(-0.5f, -0.5f, -6, 1, 1, 6, -0.3f), left);
            Role ar = left ? Role.ARM_LEFT : Role.ARM_RIGHT;
            side(part(b, "arm", "body", ar, shell).at(4, 0, -5).rot(0, -30, -10).mat("rock").box(-1.5f, -1.5f, -5, 3, 3, 5), left);
            side(part(b, "forearm", "arm*", Role.STATIC, shell).at(0, 0, -5).rot(0, 50, 0).mat("rock").box(-2, -2, -4, 4, 4, 4), left);
            side(part(b, "crusher", "forearm*", Role.STATIC, light).accent(rust).at(0, 0, -4).rot(0, -20, 0).mat("rock").box(-3, -3, -5, 6, 5, 5), left);
            side(part(b, "crusher_fixed", "crusher*", Role.STATIC, rust).at(-1.5f, 0, -5).mat("rock").box(-1.5f, -2, -4, 3, 3, 4), left);
            side(part(b, "crusher_moving", "crusher*", Role.JAW, dk(rust, 0.85f)).at(1.5f, -1, -5).rot(0, -15, 0).mat("rock")
                    .box(-1, -1.5f, -4, 2, 3, 4), left);
            side(part(b, "knob", "crusher*", Role.STATIC, dk(light, 1.15f)).at(0, -3, -2.5f).mat("rock").box(-1, -1, -1, 2, 1, 2).box(1.5f, 0, 0, 1, 1, 1), left);
        }
        for (int i = 0; i < 4; i++) {
            for (boolean left : new boolean[]{true, false}) {
                arachLeg(b, "leg" + i, left, "body", 4, 1, -3 + i * 2, 15 - i * 15, 4, -30, 5, 95, shell, rust, "chitin");
            }
        }
        String prev = "body";
        float[] p = {-8, 6, 6, 8, 10};
        for (int i = 0; i < 5; i++) {
            String n = "tail" + i;
            int w = 8 - i;
            part(b, n, prev, i == 0 ? Role.TAIL : Role.STATIC, i % 2 == 0 ? shell : dk(shell, 1.12f)).accent(rust)
                    .at(0, i == 0 ? -0.5f : 0, i == 0 ? 4 : 3).rot(p[i], 0, 0).mat("chitin").box(-w / 2f, -2.5f + i * 0.3f, 0, w, 4 - i / 2, 3).add();
            prev = n;
        }
        part(b, "fan", prev, Role.STATIC, rust).at(0, 0, 3).rot(10, 0, 0).mat("chitin").box(-4, -1, 0, 8, 1, 3).box(-2, -1, 3, 4, 1, 1).add();
        return b.build();
    }

    /** Mudskipper: a goggle-eyed mud fish propping itself up on stubby fins, with a dorsal sail and a flicking tail. */
    static ModelSpec mudskipper(String id) {
        int skin = 0x7A6A48;
        int belly = 0xC8B890;
        int spot = 0x3A3020;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.5f);
        part(b, "body", null, Role.BODY, skin).accent(spot).at(0, 20, 0).rot(-8, 0, 0).mat("scales").pattern("spots").box(-2.5f, -2.5f, -4, 5, 5, 8).add();
        part(b, "belly", "body", Role.STATIC, belly).at(0, 2.5f, 0).mat("plates").box(-2, -0.6f, -3.5f, 4, 1, 7).add();
        part(b, "head", "body", Role.HEAD, skin).accent(spot).at(0, 0, -4).mat("scales").box(-2.5f, -2.5f, -3, 5, 5, 3).add();
        part(b, "jaw", "head", Role.JAW, belly).at(0, 1.5f, -2).mat("plates").box(-2, 0, -1.5f, 4, 1, 2).add();
        for (boolean left : new boolean[]{true, false}) {
            side(part(b, "eye", "head", Role.STATIC, 0xE8E0C0).at(1, -2.5f, -1.5f).mat("bone").box(-1, -2, -1, 2, 2, 2), left);
            side(part(b, "pupil", "eye*", Role.STATIC, 0x101010).at(0, -1, -1).mat("energy").pattern("eyes_animal").box(-0.5f, -0.5f, -0.3f, 1, 1, 1, -0.1f), left);
            side(part(b, "fin", "body", left ? Role.LEG_FRONT_LEFT : Role.LEG_FRONT_RIGHT, dk(skin, 0.9f)).at(2.5f, 1.5f, -2.5f)
                    .rot(10, -20, -40).mat("membrane").box(0, -0.5f, -1, 3, 1, 3), left);
            side(part(b, "pelvic", "body", left ? Role.LEG_BACK_LEFT : Role.LEG_BACK_RIGHT, dk(skin, 0.85f)).at(1.5f, 2.5f, 1.5f)
                    .rot(0, 0, -20).mat("membrane").box(0, -0.5f, -1, 2, 1, 2), left);
        }
        part(b, "dorsal", "body", Role.FIN, dk(skin, 0.8f)).accent(0x3A6AA0).at(0, -2.5f, -1).mat("membrane").pattern("stripes")
                .box(-0.5f, -3, -2, 1, 3, 5).add();
        part(b, "tail", "body", Role.TAIL, skin).at(0, 0, 4).rot(-10, 0, 0).mat("scales").box(-1.5f, -1.5f, 0, 3, 3, 4).add();
        part(b, "tail_fin", "tail", Role.STATIC, dk(skin, 0.8f)).at(0, 0, 4).mat("membrane").box(-0.5f, -2.5f, 0, 1, 5, 3).add();
        return b.build();
    }

    /** Blood jaguar: a lean, low-slung big cat in blood-red fur with dark rosettes, fangs bared, tail lashing. */
    static ModelSpec jaguar(String id, int fur, int dark, int eye) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.8f);
        part(b, "body", null, Role.BODY, fur).accent(dark).at(0, 13, 0).mat("fur").pattern("spots").box(-3.5f, -3.5f, -8, 7, 7, 16).add();
        part(b, "shoulders", "body", Role.STATIC, fur).accent(dark).at(0, -3.5f, -6).mat("fur").pattern("spots").box(-3, -1.5f, -2, 6, 2, 5).add();
        part(b, "belly", "body", Role.STATIC, dk(fur, 1.35f)).at(0, 3, -1).mat("fur").box(-3, -1, -5, 6, 2, 10).add();
        part(b, "haunch", "body", Role.STATIC, fur).accent(dark).at(0, -1, 7).mat("fur").pattern("spots").box(-4, -3, -2, 8, 7, 4).add();
        part(b, "neck", "body", Role.NECK, fur).accent(dark).at(0, -1, -8).rot(-15, 0, 0).mat("fur").box(-2.5f, -3, -3, 5, 5, 4).add();
        part(b, "head", "neck", Role.HEAD, fur).accent(eye).at(0, -1, -3).rot(15, 0, 0).mat("fur").pattern("eyes").glow("eyes")
                .box(-3.5f, -3, -4, 7, 5, 4).add();
        part(b, "muzzle", "head", Role.STATIC, dk(fur, 1.3f)).at(0, 0.5f, -4).mat("fur").box(-2, -1.5f, -2, 4, 3, 2).add();
        part(b, "nose", "muzzle", Role.STATIC, 0x2A1010).at(0, -1.5f, -2).mat("skin").box(-1, 0, -0.5f, 2, 1, 1).add();
        part(b, "jaw", "head", Role.JAW, dk(fur, 1.2f)).at(0, 1.5f, -3.5f).rot(12, 0, 0).mat("fur").box(-1.5f, 0, -2.5f, 3, 1, 3).add();
        sym(b, "fang", "muzzle", false, 0xF0ECE0).at(1, 1.5f, -1.5f).mat("bone").box(-0.5f, 0, -0.5f, 1, 2, 1, -0.15f).add();
        sym(b, "ear", "head", false, fur).at(2.5f, -3, -1).rot(0, 0, -15).mat("fur").box(-1, -2, -0.5f, 2, 2, 1).add();
        sym(b, "whiskers", "muzzle", false, 0xE0D8C8).at(2, 0, -1).rot(0, -20, 0).mat("hair").box(0, -0.5f, -0.5f, 3, 1, 1, -0.4f).add();
        String prev = "haunch";
        float[] p = {-20, 15, 20, 25};
        for (int i = 0; i < 4; i++) {
            String n = "tail" + i;
            part(b, n, prev, i == 0 ? Role.TAIL : Role.STATIC, i == 3 ? dark : fur).accent(dark).at(0, i == 0 ? -1.5f : 0, i == 0 ? 2 : 4)
                    .rot(p[i], i == 0 ? 0 : 10, 0).mat("fur").pattern(i == 3 ? "none" : "spots").box(-1, -1, 0, 2, 2, 4).add();
            prev = n;
        }
        legPair(b, "leg_f", true, 2.5f, -6, fur, fur, dk(fur, 0.9f), "fur", 3, 5, 4, 4, -6, "paw", dark);
        legPair(b, "leg_b", false, 2.5f, 6, fur, fur, dk(fur, 0.9f), "fur", 3, 5, 5, -30, 40, "paw", dark);
        return b.build();
    }
}
