package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/**
 * Round 24 re-sculpts of common slayer creatures, part 4. Same rules as {@link Common1}..{@link Common3}: each spec was
 * rendered before/after against the model it replaces and kept only where it reads clearly closer to the OSRS model.
 * Ids, hitboxes and everything outside the model are unchanged. Registered last in {@link Overrides}.
 */
final class Common4 {
    private Common4() {
    }

    static List<ModelSpec> overrides() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(aberrantSpectre());
        l.add(dustDevil());
        l.add(kurask());
        l.add(rockslug());
        l.add(hydra());
        l.add(wallBeast());
        l.add(cockatrice());
        return l;
    }

    private static final int TOOTH = 0xF0EADA;

    /**
     * Aberrant spectre ("A very smelly ghost."): no body at all, just a bloated, lumpy olive-green mass shaped like a
     * huge head, with a drooping hooked nose and a slack mouth, floating above a fringe of long dangling tendrils and
     * two thin, trailing arms.
     */
    static ModelSpec aberrantSpectre() {
        int body = 0x7E8E56;
        int dark = 0x4E5A30;
        int light = 0xB4C27A;
        ModelSpec.Builder b = ModelSpec.builder("aberrant_spectre").shadow(0.6f);
        part(b, "mass", null, Role.FLOAT, body).accent(dark).at(0, 4, 0).mat("smoke")
                .box(-6.5f, -3, -5, 13, 10, 11)
                .box(-4.5f, -6, -3.5f, 9, 3, 8)
                .box(-8.5f, -1, -3, 2, 6, 7).box(6.5f, -1, -3, 2, 6, 7)
                .add();
        part(b, "face", "mass", Role.HEAD, light).accent(0xF0C020).at(0, 1, -5).mat("flesh").pattern("eyes")
                .box(-5, -3.5f, -1.5f, 10, 7, 2).add();
        part(b, "nose", "face", Role.STATIC, light).accent(dark).at(0, -0.5f, -1.5f).rot(28, 0, 0).mat("flesh")
                .box(-1.5f, 0, -3, 3, 5, 3).add();
        part(b, "jaw", "face", Role.JAW, dark).accent(body).at(0, 3.5f, -0.8f).rot(6, 0, 0).mat("flesh")
                .box(-3.5f, 0, -1.5f, 7, 3, 2).add();
        // fringe of long dangling tendrils hanging from the underside
        float[][] t = {{-5, -3}, {0, -3.5f}, {5, -3}, {-4.5f, 2.5f}, {4.5f, 2.5f}, {-1.5f, 4}, {2, 0}, {-2, 0.5f}};
        for (int i = 0; i < t.length; i++) {
            String n = "tendril" + i;
            float sway = (i % 3 - 1) * 8;
            part(b, n, "mass", Role.TENTACLE, i % 2 == 0 ? body : dark).accent(dark).at(t[i][0], 6.5f, t[i][1])
                    .rot(t[i][1] * 2, 0, -t[i][0] * 1.5f + sway).mat("smoke").box(-1, 0, -1, 2, 6 + i % 3, 2).add();
            part(b, n + "_tip", n, Role.STATIC, dark).at(0, 6 + i % 3, 0).rot(sway, 0, -sway).mat("smoke")
                    .box(-0.5f, 0, -0.5f, 1, 5 + (i + 1) % 3, 1).add();
        }
        // two thin, trailing arms
        sym(b, "arm", "mass", false, body).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(dark).at(7, 3, -2).rot(-20, 0, -18)
                .mat("smoke").box(-1, 0, -1, 2, 8, 2).add();
        sym(b, "forearm", "arm", true, body).at(0, 8, 0).rot(-30, 0, 10).mat("smoke").box(-1, 0, -1, 2, 7, 2).add();
        sym(b, "fingers", "forearm", true, dark).at(0, 7, 0).mat("smoke")
                .box(-1.2f, 0, -1, 1, 3, 1).box(0.2f, 0, -1, 1, 3, 1).add();
        return b.build();
    }

    /**
     * Dust devil ("The vacuumed face of evil."): a whirling column of sand, a point at the ground widening as it rises
     * in twisted rings, crowned by a dusty cloud with a hollow-eyed, gaping face; grit and stones orbit the column.
     */
    static ModelSpec dustDevil() {
        int sand = 0xC4AC7E;
        int dark = 0x8A7450;
        int stone = 0x6E6050;
        ModelSpec.Builder b = ModelSpec.builder("dust_devil").shadow(0.5f);
        part(b, "vortex", null, Role.SPIN, sand).accent(dark).at(0, 24, 0).mat("smoke").box(-1, -3, -1, 2, 3, 2).add();
        int[] w = {4, 6, 8, 9, 11, 12};
        for (int i = 0; i < w.length; i++) {
            part(b, "ring" + i, "vortex", Role.STATIC, i % 2 == 0 ? sand : dark).accent(dark)
                    .at((float) Math.sin(i * 1.3) * 0.8f, -3 - i * 2.4f, 0).rot(0, i * 23, i % 2 == 0 ? 7 : -7).mat("smoke")
                    .box(-w[i] / 2f, -3, -w[i] / 2f, w[i], 3, w[i]).add();
        }
        // dust streamers and stones swept round the column
        part(b, "streamers", "vortex", Role.STATIC, dark).at(0, -12, 0).mat("smoke")
                .box(4, -1, -1, 5, 2, 2).box(-8, -4, 0, 4, 2, 2).box(-1, -7, 5, 2, 2, 5).box(0, 2, -7, 2, 1, 4).add();
        part(b, "stones", "vortex", Role.STATIC, stone).at(0, -10, 0).mat("rock")
                .box(7.5f, -2, 2, 1, 1, 1).box(-8, 1, -3, 1, 1, 1).box(3, -6, -7.5f, 1, 1, 1).box(-4, 3, 6.5f, 1, 1, 1).add();
        // the dusty crown with its face
        part(b, "head", null, Role.HEAD, sand).accent(0x2A2018).at(0, 8, 0).mat("smoke").pattern("eyes")
                .box(-6, -6, -6, 12, 7, 12).add();
        part(b, "crown", "head", Role.STATIC, dark).at(0, -6, 0).rot(0, 30, 0).mat("smoke")
                .box(-7, -2, -7, 14, 2, 14).box(-4.5f, -4, -4.5f, 9, 2, 9).add();
        part(b, "maw", "head", Role.JAW, 0x3A2C1E).at(0, -2, -6.1f).mat("smoke").box(-2.5f, 0, -0.6f, 5, 3, 1).add();
        return b.build();
    }

    /**
     * Kurask ("Large, heavy, with sharp things attached to its head."): a hulking, deeply hunched green brute, its
     * massive shoulders and humped back carried far forward over short thick legs, long heavy arms ending in fists that
     * reach the ground, and a small low-slung tusked head crowned with big blade-like horns. Brown loincloth.
     */
    static ModelSpec kurask() {
        int skin = 0x6E8A4A;
        int dark = 0x44582C;
        int horn = 0xD8CCA8;
        int cloth = 0x5A3E24;
        ModelSpec.Builder b = ModelSpec.builder("kurask").shadow(1.0f);
        part(b, "hips", null, Role.BODY, skin).accent(dark).at(0, 10, 4).mat("skin").box(-5, -4, -4, 10, 8, 8).add();
        part(b, "loin", "hips", Role.STATIC, cloth).at(0, 2, 0).mat("leather").box(-5.5f, 0, -4.5f, 11, 3, 9, 0.1f)
                .box(-3, 2, -4.8f, 6, 5, 1).add();
        part(b, "torso", "hips", Role.STATIC, skin).accent(dark).at(0, -3, -1).rot(62, 0, 0).mat("skin")
                .box(-7, -12, -5, 14, 13, 10).add();
        part(b, "hump", "torso", Role.STATIC, skin).accent(dark).at(0, -8, 4).mat("skin").box(-6, -4, -2, 12, 7, 5).add();
        part(b, "chest", "torso", Role.STATIC, dark).at(0, -11, -5).mat("skin").box(-6, 0, -0.8f, 5, 5, 1).box(1, 0, -0.8f, 5, 5, 1).add();
        part(b, "head", "torso", Role.HEAD, skin).accent(0xE0C050).at(0, -12, -5).rot(-62, 0, 0).mat("skin").pattern("eyes")
                .box(-3.5f, -3, -6, 7, 6, 6).add();
        part(b, "jaw", "head", Role.JAW, dark).at(0, 2.5f, -3).rot(6, 0, 0).mat("skin").box(-3, 0, -3.5f, 6, 2, 4).add();
        part(b, "tusks", "jaw", Role.STATIC, TOOTH).at(0, 0, -3.3f).mat("horn").box(-2.5f, -2, 0, 1, 2, 1).box(1.5f, -2, 0, 1, 2, 1).add();
        // the "sharp things": a tall central blade and two side blades sweeping back
        part(b, "horn_mid", "head", Role.STATIC, horn).at(0, -3, -3).rot(-35, 0, 0).mat("horn").box(-1, -9, -2.5f, 2, 9, 5).add();
        part(b, "horn_mid_tip", "horn_mid", Role.STATIC, horn).at(0, -9, 0).rot(-25, 0, 0).mat("horn").box(-0.5f, -5, -1.5f, 1, 5, 3).add();
        sym(b, "horn_side", "head", false, horn).at(3, -2, -2).rot(-45, 0, -30).mat("horn").box(-1, -7, -2, 2, 7, 4).add();
        sym(b, "horn_side_tip", "horn_side", true, horn).at(0, -7, 0).rot(-20, 0, 0).mat("horn").box(-0.5f, -3, -1, 1, 3, 2).add();
        // long heavy arms, knuckles to the ground
        sym(b, "arm", "torso", false, skin).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(dark).at(7.5f, -10, 0).rot(-78, 0, -10)
                .mat("skin").box(-2.5f, -1, -2.5f, 5, 10, 5).add();
        sym(b, "forearm", "arm", true, skin).at(0, 9, 0).rot(-6, 0, 4).mat("skin").box(-2.5f, 0, -2.5f, 5, 7, 5, 0.2f).add();
        sym(b, "fist", "forearm", true, dark).at(0, 7, 0).mat("skin").box(-3, 0, -3, 6, 4, 6).add();
        // short thick legs
        sym(b, "thigh", "hips", false, skin).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(dark).at(4, 1, 0).rot(-20, 0, 0)
                .mat("skin").box(-2.5f, -1, -2.5f, 5, 7, 5).add();
        sym(b, "shin", "thigh", true, skin).at(0, 6, 0).rot(25, 0, 0).mat("skin").box(-2, 0, -2, 4, 5, 4).add();
        sym(b, "foot", "shin", true, dark).at(0, 5, 0).rot(-5, 0, 0).mat("skin").box(-2.5f, 0, -4, 5, 2, 6).add();
        return b.build();
    }

    /**
     * Rockslug ("The slime of evil."): a low grey-brown slug whose back is crusted with stony lumps, a slimy foot
     * spreading round it, two eye stalks and a pair of short feelers at the front.
     */
    static ModelSpec rockslug() {
        int body = 0x8A7C6A;
        int dark = 0x5E5446;
        int rock = 0x6E6862;
        int eye = 0x2E2620;
        ModelSpec.Builder b = ModelSpec.builder("rockslug").shadow(0.7f);
        part(b, "body", null, Role.BODY, body).accent(dark).at(0, 21, 0).mat("flesh").box(-3.5f, -2, -7, 7, 4, 14).add();
        part(b, "foot", "body", Role.STATIC, dark).accent(body).at(0, 2, 0).mat("flesh").box(-4.5f, 0, -8, 9, 1, 17).add();
        part(b, "head", "body", Role.HEAD, body).accent(dark).at(0, 0.5f, -7).mat("flesh").pattern("blank")
                .box(-2.5f, -2, -3, 5, 3, 3).add();
        sym(b, "stalk", "head", false, body).at(1.5f, -2.5f, -2.5f).rot(-25, 0, 22).mat("flesh").box(-0.5f, -4, -0.5f, 1, 4, 1).add();
        sym(b, "eye", "stalk", true, eye).at(0, -4, 0).mat("flesh").box(-1, -2, -1, 2, 2, 2).add();
        sym(b, "feeler", "head", false, dark).at(1.5f, 0.5f, -2.5f).rot(70, 0, 25).mat("flesh").box(-0.5f, 0, -0.5f, 1, 3, 1).add();
        part(b, "tail", "body", Role.TAIL, body).accent(dark).at(0, 0.5f, 7).mat("flesh").box(-2.5f, -1.5f, 0, 5, 3, 4).box(-1.5f, -0.5f, 4, 3, 2, 3).add();
        // stony crust over the back
        part(b, "crust", "body", Role.STATIC, rock).accent(dark).at(0, -2, 0).mat("rock")
                .box(-3, -2, -5, 6, 3, 5).box(-3.5f, -4, -1, 7, 5, 5).box(-2.5f, -2, 4, 5, 3, 4).add();
        part(b, "crust_lump", "crust", Role.STATIC, rock).accent(dark).at(1.5f, -4.5f, 1).rot(15, 30, 20).mat("rock")
                .box(-1.5f, -1.5f, -1.5f, 3, 2, 3).add();
        part(b, "crust_lump2", "crust", Role.STATIC, dark).at(-2.5f, -2.5f, -3.5f).rot(-20, 20, -25).mat("rock")
                .box(-1.5f, -2, -1.5f, 3, 2, 3).add();
        part(b, "crust_lump3", "crust", Role.STATIC, rock).at(1, -2.5f, 6).rot(25, -15, 10).mat("rock")
                .box(-1.5f, -2, -1.5f, 3, 2, 3).add();
        return b.build();
    }

    /**
     * Hydra (non-boss, Karuulm Slayer Dungeon): a bulky, green four-legged reptile with three long necks fanning from
     * its chest, each ending in a horned snapping head (the wiki's attack notes name its left, middle and right head),
     * a ridge of spines down the back, a pale belly and a heavy tail.
     */
    static ModelSpec hydra() {
        int body = 0x3E6E44;
        int belly = 0xB0B878;
        int dark = 0x28482C;
        int spine = 0x8A9A5A;
        int horn = 0xE0D8B0;
        ModelSpec.Builder b = ModelSpec.builder("hydra").shadow(1.3f);
        part(b, "body", null, Role.BODY, body).accent(belly).at(0, 11, 2).mat("scales").box(-7, -6, -10, 14, 12, 20).add();
        part(b, "belly", "body", Role.STATIC, belly).accent(dark).at(0, 6, 0).mat("plates").box(-5.5f, -0.6f, -9, 11, 1, 17).add();
        part(b, "spines", "body", Role.STATIC, spine).at(0, -6, 0).mat("horn")
                .box(-0.5f, -3, -6, 1, 3, 3).box(-0.5f, -4, -1, 1, 4, 3).box(-0.5f, -3, 4, 1, 3, 3).add();
        float[] yaw = {32, 0, -32};
        float[] lift = {-42, -55, -42};
        for (int h = 0; h < 3; h++) {
            String s = String.valueOf(h);
            float nx = (h - 1) * 4.5f;
            part(b, "neck" + s, "body", h == 1 ? Role.NECK : Role.STATIC, body).accent(belly).at(nx, -2, -9).rot(lift[h], yaw[h], 0)
                    .mat("scales").box(-2.5f, -2.5f, -6, 5, 5, 6).add();
            part(b, "neck" + s + "b", "neck" + s, Role.STATIC, body).accent(belly).at(0, 0, -5.5f).rot(18, 0, 0).mat("scales")
                    .box(-2, -2, -6, 4, 4, 6).add();
            part(b, "neck" + s + "c", "neck" + s + "b", Role.STATIC, body).accent(belly).at(0, 0, -5.5f).rot(22, 0, 0).mat("scales")
                    .box(-2, -2, -5, 4, 4, 5).add();
            part(b, "head" + s, "neck" + s + "c", Role.HEAD, body).accent(0xFFD040).at(0, 0, -4.5f).rot(12, 0, 0).mat("scales")
                    .pattern("eyes").glow("eyes").box(-3, -3, -4, 6, 5, 5).add();
            part(b, "snout" + s, "head" + s, Role.STATIC, body).at(0, 0, -4).mat("scales").box(-2, -2, -4, 4, 3, 4).add();
            part(b, "jaw" + s, "head" + s, Role.JAW, belly).at(0, 2, -3).rot(16, 0, 0).mat("plates").box(-2, 0, -5, 4, 1, 5).add();
            part(b, "horns" + s, "head" + s, Role.STATIC, horn).at(0, -2.5f, 0).rot(-55, 0, 0).mat("horn")
                    .box(-2.5f, -4, -0.5f, 1, 4, 1).box(1.5f, -4, -0.5f, 1, 4, 1).add();
        }
        // columnar legs with clawed feet
        sym(b, "leg_f", "body", false, body).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(belly).at(6, 3, -6).rot(8, 0, -4)
                .mat("scales").box(-3, -2, -3, 6, 8, 6).add();
        sym(b, "foot_f", "leg_f", true, dark).at(0, 6, 0).rot(-8, 0, 4).mat("scales").box(-2.5f, 0, -3.5f, 5, 4, 6).add();
        sym(b, "leg_b", "body", false, body).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(belly).at(6, 3, 6).rot(-8, 0, -4)
                .mat("scales").box(-3, -2, -3.5f, 6, 8, 7).add();
        sym(b, "foot_b", "leg_b", true, dark).at(0, 6, 0).rot(8, 0, 4).mat("scales").box(-2.5f, 0, -3.5f, 5, 4, 6).add();
        part(b, "tail", "body", Role.TAIL, body).accent(belly).at(0, -1, 10).rot(18, 0, 0).mat("scales").box(-3.5f, -3.5f, 0, 7, 7, 6).add();
        part(b, "tail_mid", "tail", Role.STATIC, body).at(0, 0, 6).rot(8, 0, 0).mat("scales").box(-2.5f, -2.5f, 0, 5, 5, 6).add();
        part(b, "tail_tip", "tail_mid", Role.STATIC, dark).at(0, 0, 6).rot(6, 0, 0).mat("scales").box(-1.5f, -1.5f, 0, 3, 3, 6)
                .box(-0.5f, -3, 1, 1, 2, 2).add();
        return b.build();
    }

    /**
     * Wall beast ("A big, scary hand!"): all that shows of it is a huge, hairy grey-brown arm lunging out of a dark
     * crack in the cave wall, ending in a great clawed hand, with a pair of eyes glinting in the hole behind it.
     */
    static ModelSpec wallBeast() {
        int rock = 0x6A625A;
        int dark = 0x433C36;
        int hide = 0x7A6A52;
        int claw = 0xD8CCB0;
        ModelSpec.Builder b = ModelSpec.builder("wall_beast").shadow(0.6f);
        part(b, "wall", null, Role.STATIC, rock).accent(dark).at(0, 24, 4).mat("rock").box(-8, -30, 0, 16, 30, 4)
                .box(-9, -12, -1, 4, 12, 2).box(5, -20, -1, 4, 9, 2).box(-6, -30, -1, 7, 4, 2).add();
        part(b, "crack", "wall", Role.STATIC, 0x141012).at(0, -17, -0.2f).mat("rock").box(-4.5f, -6, -0.5f, 9, 11, 1).add();
        part(b, "eyes", "crack", Role.HEAD, 0x1E1816).accent(0xFFC040).at(0, -3, -0.6f).mat("flesh").pattern("eyes").glow("eyes")
                .box(-3, -2, -0.5f, 6, 3, 1).add();
        part(b, "arm", "crack", Role.ARM_RIGHT, hide).accent(dark).at(0, 2, -1).rot(-62, 0, 0).mat("fur")
                .box(-3, -1, -3, 6, 10, 6).add();
        part(b, "forearm", "arm", Role.STATIC, hide).accent(dark).at(0, 9, 0).rot(35, 0, 0).mat("fur").box(-2.5f, 0, -2.5f, 5, 8, 5).add();
        part(b, "hand", "forearm", Role.STATIC, hide).accent(dark).at(0, 8, 0).rot(25, 0, 0).mat("skin").box(-4, 0, -2, 8, 4, 4).add();
        part(b, "fingers", "hand", Role.STATIC, hide).accent(dark).at(0, 4, 0).rot(30, 0, 0).mat("skin")
                .box(-4, 0, -1, 1, 5, 2).box(-2, 0, -1, 1, 6, 2).box(0, 0, -1, 1, 6, 2).box(2, 0, -1, 1, 5, 2).add();
        part(b, "claws", "fingers", Role.STATIC, claw).at(0, 5, 0).rot(30, 0, 0).mat("horn")
                .box(-4, 0, -1, 1, 2, 1).box(-2, 1, -1, 1, 2, 1).box(0, 1, -1, 1, 2, 1).box(2, 0, -1, 1, 2, 1).add();
        part(b, "thumb", "hand", Role.STATIC, hide).at(4, 1, -1).rot(0, 0, -50).mat("skin").box(0, 0, -1, 1, 4, 2).add();
        return b.build();
    }

    /**
     * Cockatrice ("The winged reptile."): a rooster's head with a red comb and wattle and a yellow beak on a green,
     * scaly lizard body, small leathery wings raised from the shoulders, scrawny bird legs and a long reptile tail.
     */
    static ModelSpec cockatrice() {
        int body = 0x6E8A3A;
        int dark = 0x4A5E24;
        int belly = 0xA8AE6A;
        int comb = 0xC8281E;
        int beak = 0xE0B030;
        int leg = 0x9A8A3E;
        ModelSpec.Builder b = ModelSpec.builder("cockatrice").shadow(0.6f);
        part(b, "body", null, Role.BODY, body).accent(dark).at(0, 12, 0).rot(-12, 0, 0).mat("scales").box(-4, -4, -5, 8, 8, 10).add();
        part(b, "breast", "body", Role.STATIC, belly).at(0, 0, -5).mat("feathers").box(-3, -3, -1, 6, 6, 2).add();
        part(b, "neck", "body", Role.NECK, body).accent(dark).at(0, -3, -4).rot(18, 0, 0).mat("feathers").box(-2, -6, -2, 4, 7, 4).add();
        part(b, "head", "neck", Role.HEAD, body).accent(0xE8D040).at(0, -6, 0).rot(-6, 0, 0).mat("feathers").pattern("eyes_animal")
                .box(-2.5f, -4, -3, 5, 5, 5).add();
        part(b, "beak", "head", Role.JAW, beak).at(0, -1.5f, -3).mat("horn").box(-1, -0.5f, -3, 2, 2, 3).add();
        part(b, "comb", "head", Role.STATIC, comb).at(0, -4, 0).mat("flesh")
                .box(-0.5f, -2, -2.5f, 1, 2, 1).box(-0.5f, -3, -1, 1, 3, 1).box(-0.5f, -2, 0.5f, 1, 2, 1).add();
        part(b, "wattle", "head", Role.STATIC, comb).at(0, 0.5f, -3).mat("flesh").box(-0.5f, 0, -0.5f, 1, 2, 1).add();
        sym(b, "wing", "body", false, dark).roles(Role.WING_LEFT, Role.WING_RIGHT).accent(body).at(4, -3, -2).rot(-10, 0, -55)
                .mat("membrane").box(0, -1, 0, 1, 6, 8).add();
        sym(b, "wing_tip", "wing", true, dark).accent(body).at(0.5f, -1, 8).rot(25, 0, 0).mat("membrane").box(-0.5f, 0, 0, 1, 5, 5).add();
        sym(b, "thigh", "body", false, body).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(dark).at(2.2f, 3, 1).rot(12, 0, 0)
                .mat("feathers").box(-1.5f, -1, -1.5f, 3, 4, 3).add();
        sym(b, "shin", "thigh", true, leg).at(0, 3, 0).mat("scales").box(-0.5f, 0, -0.5f, 1, 5, 1).add();
        sym(b, "foot", "shin", true, leg).at(0, 5, 0).mat("scales").box(-1.5f, 0, -2.5f, 3, 1, 4).add();
        part(b, "tail", "body", Role.TAIL, body).accent(dark).at(0, 0, 5).rot(25, 0, 0).mat("scales").box(-2, -2, 0, 4, 4, 6).add();
        part(b, "tail_mid", "tail", Role.STATIC, body).at(0, 0, 6).rot(15, 0, 0).mat("scales").box(-1.5f, -1.5f, 0, 3, 3, 6).add();
        part(b, "tail_tip", "tail_mid", Role.STATIC, dark).at(0, 0, 6).rot(-10, 0, 0).mat("scales").box(-1, -1, 0, 2, 2, 6).add();
        return b.build();
    }
}
