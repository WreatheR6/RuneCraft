package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Builder;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.slayer.ModelKit.biped;
import static com.runecraft.osrs.content.slayer.ModelKit.dragon;
import static com.runecraft.osrs.content.slayer.ModelKit.finish;
import static com.runecraft.osrs.content.slayer.ModelKit.ghost;
import static com.runecraft.osrs.content.slayer.ModelKit.insect;
import static com.runecraft.osrs.content.slayer.ModelKit.lizard;
import static com.runecraft.osrs.content.slayer.ModelKit.quadruped;
import static com.runecraft.osrs.content.slayer.ModelKit.wyvern;

/** Custom models for every non-humanoid slayer creature. */
final class SlayerModels {
    private SlayerModels() {
    }

    static List<ModelSpec> all() {
        List<ModelSpec> list = new ArrayList<>();
        dragons(list);
        beasts(list);
        insects(list);
        floaters(list);
        demons(list);
        oddities(list);
        return list;
    }

    // ------------------------------------------------------------------ dragons & wyverns

    private static void dragons(List<ModelSpec> list) {
        list.add(dragon("green_dragon", 0x3A8A3A, 0x9AC060, 0x2A6A2A, 0xD8D0B0, 0, "scales"));
        list.add(dragon("blue_dragon", 0x3A5AB0, 0x8AA8E0, 0x2A3A8A, 0xD8D0B0, 0, "scales"));
        list.add(dragon("red_dragon", 0xB03A3A, 0xE0A060, 0x8A2020, 0xD8D0B0, 0, "scales"));
        list.add(dragon("black_dragon", 0x2A2A2A, 0x5A4A4A, 0x1A1A1A, 0xA09080, 0, "scales"));
        list.add(dragon("brutal_green_dragon", 0x2A6A2A, 0x7AA050, 0x1A4A1A, 0x3A3A2A, 1, "plates"));
        list.add(dragon("brutal_blue_dragon", 0x2A4A90, 0x6A88C0, 0x1A2A6A, 0x3A3A2A, 1, "plates"));
        list.add(dragon("brutal_red_dragon", 0x902A2A, 0xC08050, 0x6A1A1A, 0x3A3A2A, 1, "plates"));
        list.add(dragon("brutal_black_dragon", 0x1E1E1E, 0x4A3A3A, 0x101010, 0x6A2020, 1, "plates"));
        list.add(dragon("lava_dragon", 0x3A2A2A, 0xE06020, 0x5A2A1A, 0xF0A030, 1, "glow"));
        list.add(dragon("bronze_dragon", 0xA87A40, 0xC89A60, 0x8A6030, 0xE0C080, 2, "plates"));
        list.add(dragon("iron_dragon", 0x7A7A7A, 0x9A9A9A, 0x5A5A5A, 0xC0C0C0, 2, "plates"));
        list.add(dragon("steel_dragon", 0xA0A8B0, 0xC0C8D0, 0x8088A0, 0xE0E8F0, 2, "plates"));
        list.add(dragon("mithril_dragon", 0x4A5A8A, 0x7A8AB0, 0x3A4A7A, 0xB0C0E0, 2, "plates"));
        list.add(dragon("adamant_dragon", 0x3A6A4A, 0x6A9A7A, 0x2A5A3A, 0x9AE0B0, 2, "plates"));
        list.add(dragon("rune_dragon", 0x3A8AA0, 0x6AB0C8, 0x2A6A80, 0xA0F0FF, 2, "glow"));
        list.add(wyvern("skeletal_wyvern", 0xD8D8C8, 0xA8A898, 0xB8B8A8, "plates"));
        list.add(wyvern("spitting_wyvern", 0x6A8A9A, 0xA0C0D0, 0x4A6A7A, "scales"));
        list.add(wyvern("taloned_wyvern", 0x5A6A8A, 0x9AA8C0, 0x3A4A6A, "scales"));

        // Drake: a bipedal, small-winged volcanic dragon.
        Builder drake = biped("drake", 0x8A4A2A, 0x6A3A2A, 0x5A2A1A, 36, 10, 25, 12, true, 0x4A2A1A);
        drake.part("snout", "head", Role.STATIC, 0x8A4A2A).pivot(0, -3, -4).box(-2.5f, -2, -6, 5, 4, 6);
        drake.part("horns", "head", Role.STATIC, 0xE0C080).pivot(0, -7, 0).rotate(30, 0, 0)
                .box(-3.5f, -4, 0, 1, 4, 1).box(2.5f, -4, 0, 1, 4, 1);
        drake.part("tail_tip", "tail", Role.TAIL, 0xE06020).pivot(0, 0, 12).box(-1.5f, -1.5f, 0, 3, 3, 4).pattern("glow");
        list.add(finish(drake));

        // Wyrm: a long, low serpentine body on four short legs with a crested head.
        Builder w = ModelSpec.builder("wyrm").shadow(0.9f);
        w.part("body", null, Role.BODY, 0x4A3A5A).pivot(0, 17, -4).box(-4, -4, -6, 8, 8, 12).pattern("scales");
        w.part("neck", "body", Role.NECK, 0x4A3A5A).pivot(0, -2, -6).rotate(-35, 0, 0).box(-2.5f, -2.5f, -8, 5, 5, 8);
        w.part("head", "neck", Role.HEAD, 0x5A4A6A).pivot(0, 0, -8).rotate(35, 0, 0).box(-3, -3, -7, 6, 5, 7).pattern("eyes");
        w.part("jaw", "head", Role.JAW, 0x8A6AA0).pivot(0, 2, -2).box(-2.5f, 0, -6, 5, 2, 6);
        w.part("crest", "head", Role.STATIC, 0xC060E0).pivot(0, -3, 0).box(-0.5f, -3, -3, 1, 3, 6).pattern("glow");
        w.part("seg2", "body", Role.TAIL, 0x4A3A5A).pivot(0, 1, 6).box(-3.5f, -3.5f, 0, 7, 7, 10).pattern("scales");
        w.part("seg3", "seg2", Role.TAIL, 0x4A3A5A).pivot(0, 0.5f, 10).box(-2.5f, -2.5f, 0, 5, 5, 10);
        w.part("seg4", "seg3", Role.TAIL, 0xC060E0).pivot(0, 0, 10).box(-1.5f, -1.5f, 0, 3, 3, 8);
        w.part("leg_fl", null, Role.LEG_FRONT_LEFT, 0x3A2A4A).pivot(4, 19, -7).box(-1.5f, 0, -1.5f, 3, 5, 3);
        w.part("leg_fr", null, Role.LEG_FRONT_RIGHT, 0x3A2A4A).pivot(-4, 19, -7).box(-1.5f, 0, -1.5f, 3, 5, 3);
        w.part("leg_bl", null, Role.LEG_BACK_LEFT, 0x3A2A4A).pivot(4, 19, 0).box(-1.5f, 0, -1.5f, 3, 5, 3);
        w.part("leg_br", null, Role.LEG_BACK_RIGHT, 0x3A2A4A).pivot(-4, 19, 0).box(-1.5f, 0, -1.5f, 3, 5, 3);
        list.add(finish(w));

        // Hydra: heavy quadruped with four necks, each with a snapping head.
        Builder h = ModelSpec.builder("hydra").shadow(1.2f);
        h.part("body", null, Role.BODY, 0x4A7A3A).pivot(0, 11, 2).box(-7, -6, -9, 14, 12, 18).pattern("scales");
        h.part("belly", "body", Role.STATIC, 0x9AB060).pivot(0, 0, 0).box(-6, 5, -8, 12, 2, 16);
        h.part("leg_fl", null, Role.LEG_FRONT_LEFT, 0x3A6A2A).pivot(5, 15, -4).box(-2.5f, 0, -2.5f, 5, 9, 5);
        h.part("leg_fr", null, Role.LEG_FRONT_RIGHT, 0x3A6A2A).pivot(-5, 15, -4).box(-2.5f, 0, -2.5f, 5, 9, 5);
        h.part("leg_bl", null, Role.LEG_BACK_LEFT, 0x3A6A2A).pivot(5, 15, 8).box(-2.5f, 0, -2.5f, 5, 9, 5);
        h.part("leg_br", null, Role.LEG_BACK_RIGHT, 0x3A6A2A).pivot(-5, 15, 8).box(-2.5f, 0, -2.5f, 5, 9, 5);
        h.part("tail", "body", Role.TAIL, 0x4A7A3A).pivot(0, -1, 9).rotate(-10, 0, 0).box(-2, -2, 0, 4, 4, 12);
        float[] nx = {-5, -1.7f, 1.7f, 5};
        float[] yaw = {25, 8, -8, -25};
        for (int i = 0; i < 4; i++) {
            h.part("neck" + i, "body", Role.NECK, 0x4A7A3A).pivot(nx[i], -4, -8).rotate(-50, yaw[i], 0)
                    .box(-1.5f, -1.5f, -12, 3, 3, 12);
            h.part("head" + i, "neck" + i, Role.HEAD, 0x5A8A4A).pivot(0, 0, -12).rotate(50, 0, 0)
                    .box(-2.5f, -2.5f, -5, 5, 4, 5).pattern("eyes");
            h.part("jaw" + i, "head" + i, Role.JAW, 0x9AB060).pivot(0, 1.5f, -1).box(-2, 0, -4, 4, 1, 4);
        }
        list.add(finish(h));
    }

    // ------------------------------------------------------------------ quadrupeds

    private static void beasts(List<ModelSpec> list) {
        list.add(quadruped("hellhound", 0x3A1A1A, 0xE04010, 9, 9, 18, 10, 7, 9, 2, "glow"));
        list.add(quadruped("terror_dog", 0x6A6A5A, 0xC0B090, 8, 9, 16, 10, 7, 8, 1, "stripes"));
        list.add(quadruped("grizzly_bear", 0x5A3A20, 0x3A2A1A, 12, 11, 20, 8, 8, 2, 1, "none"));
        list.add(quadruped("brine_rat", 0x5A6A6A, 0xC09090, 8, 6, 14, 5, 6, 12, 1, "none"));
        list.add(quadruped("dark_beast", 0x1A1A2A, 0x6A3A8A, 14, 13, 22, 12, 9, 10, 2, "glow"));
        list.add(quadruped("molanisk", 0x4A3A2A, 0xC08080, 12, 9, 14, 4, 7, 3, 0, "spots"));
        list.add(lizard("lizard", 0xC0A060, 0x8A7040, 9, false, true));
        list.add(lizard("basilisk", 0x5A7A3A, 0xA0C040, 12, true, true));

        // Cockatrice: a bird-lizard on two legs with a crest and small wings.
        Builder c = ModelSpec.builder("cockatrice").shadow(0.5f);
        c.part("body", null, Role.BODY, 0x8A9A3A).pivot(0, 13, 0).rotate(-15, 0, 0).box(-4, -4, -5, 8, 8, 10).pattern("scales");
        c.part("neck", "body", Role.NECK, 0x8A9A3A).pivot(0, -3, -5).rotate(-25, 0, 0).box(-1.5f, -6, -2, 3, 7, 3);
        c.part("head", "neck", Role.HEAD, 0xA0B040).pivot(0, -6, 0).rotate(40, 0, 0).box(-2.5f, -4, -3, 5, 4, 5).pattern("eyes");
        c.part("beak", "head", Role.JAW, 0xE0C030).pivot(0, -2, -3).box(-1, -1, -3, 2, 2, 3);
        c.part("crest", "head", Role.STATIC, 0xC02020).pivot(0, -4, 0).box(-0.5f, -3, -2, 1, 3, 5);
        c.part("wing_l", "body", Role.WING_LEFT, 0x6A7A2A).pivot(4, -3, -2).box(0, 0, 0, 1, 6, 8);
        c.part("wing_r", "body", Role.WING_RIGHT, 0x6A7A2A).pivot(-4, -3, -2).box(-1, 0, 0, 1, 6, 8);
        c.part("tail", "body", Role.TAIL, 0x8A9A3A).pivot(0, 0, 5).rotate(20, 0, 0).box(-1, -1, 0, 2, 2, 10);
        c.part("leg_l", null, Role.LEG_FRONT_LEFT, 0xE0C030).pivot(2, 16, 1).box(-1, 0, -1, 2, 8, 2);
        c.part("leg_r", null, Role.LEG_FRONT_RIGHT, 0xE0C030).pivot(-2, 16, 1).box(-1, 0, -1, 2, 8, 2);
        list.add(finish(c));

        // Giant bat: furry body, big leathery wings and dangling feet.
        Builder bat = ModelSpec.builder("giant_bat").shadow(0.4f);
        bat.part("body", null, Role.FLOAT, 0x3A2A2A).pivot(0, 10, 0).box(-3, -3, -3, 6, 7, 5);
        bat.part("head", "body", Role.HEAD, 0x4A3A3A).pivot(0, -3, -1).box(-2.5f, -4, -2.5f, 5, 4, 5).pattern("eyes");
        bat.part("ears", "head", Role.STATIC, 0x6A4A4A).pivot(0, -4, 0).box(-2.5f, -3, 0, 1, 3, 1).box(1.5f, -3, 0, 1, 3, 1);
        bat.part("jaw", "head", Role.JAW, 0x6A4A4A).pivot(0, -1, -2.5f).box(-1.5f, 0, -1, 3, 1, 1);
        bat.part("wing_l", "body", Role.WING_LEFT, 0x5A3A3A).pivot(3, -2, 0).box(0, -1, -1, 14, 1, 2).box(0, 0, -1, 13, 7, 1);
        bat.part("wing_r", "body", Role.WING_RIGHT, 0x5A3A3A).pivot(-3, -2, 0).box(-14, -1, -1, 14, 1, 2).box(-13, 0, -1, 13, 7, 1);
        bat.part("feet", "body", Role.STATIC, 0x2A1A1A).pivot(0, 4, 0).box(-2, 0, -0.5f, 1, 2, 1).box(1, 0, -0.5f, 1, 2, 1);
        list.add(finish(bat));

        // Monkey: small hunched biped with a long curled tail.
        Builder monkey = biped("monkey", 0xB08A60, 0x6A4A2A, 0x6A4A2A, 14, 6, 15, 7, true, -1);
        list.add(finish(monkey));
    }

    // ------------------------------------------------------------------ insects & spiders

    private static void insects(List<ModelSpec> list) {
        list.add(finish(insect("cave_bug", 0x5A4A3A, 0x3A2A1A, 6, 3, 5, "spots")));
        Builder crawler = insect("cave_crawler", 0x6A7A4A, 0x4A5A2A, 9, 4, 7, "spots");
        crawler.part("spikes", "abdomen", Role.STATIC, 0xC0C060).pivot(0, -3, 0)
                .box(-2, -2, 1, 1, 2, 1).box(1, -2, 3, 1, 2, 1).box(-1, -2, 6, 1, 2, 1);
        list.add(finish(crawler));
        list.add(finish(insect("fever_spider", 0x8A3A2A, 0x5A2A1A, 10, 4, 10, "stripes")));
        Builder kalphite = insect("kalphite", 0xC09A50, 0x8A6A30, 12, 3, 10, "plates");
        kalphite.part("wing_l", "thorax", Role.WING_LEFT, 0xE0D8A0).pivot(2, -3, 1).rotate(0, -20, 0).box(0, 0, 0, 10, 1, 4);
        kalphite.part("wing_r", "thorax", Role.WING_RIGHT, 0xE0D8A0).pivot(-2, -3, 1).rotate(0, 20, 0).box(-10, 0, 0, 10, 1, 4);
        kalphite.part("antennae", "head", Role.STATIC, 0x5A4A20).pivot(0, -3, -3).rotate(-30, 0, 0)
                .box(-2, -5, 0, 1, 5, 1).box(1, -5, 0, 1, 5, 1);
        list.add(finish(kalphite));

        // Scorpion: segmented tail arching over the back, big pincers.
        Builder s = insect("scorpion", 0x2A2A2A, 0x4A3A2A, 8, 4, 6, "plates");
        s.part("tail1", "abdomen", Role.TAIL, 0x2A2A2A).pivot(0, -1, 8).rotate(65, 0, 0).box(-1.5f, -1.5f, 0, 3, 3, 5);
        s.part("tail2", "tail1", Role.TAIL, 0x2A2A2A).pivot(0, 0, 5).rotate(50, 0, 0).box(-1, -1, 0, 2, 2, 5);
        s.part("stinger", "tail2", Role.STATIC, 0xC04020).pivot(0, 0, 5).rotate(45, 0, 0).box(-1, -1, 0, 2, 2, 3);
        s.part("claw_l", "thorax", Role.ARM_LEFT, 0x3A3A3A).pivot(3, 0, -3).rotate(0, 20, 0).box(-1, -1, -6, 2, 2, 6)
                .box(-1.5f, -1.5f, -9, 3, 3, 3);
        s.part("claw_r", "thorax", Role.ARM_RIGHT, 0x3A3A3A).pivot(-3, 0, -3).rotate(0, -20, 0).box(-1, -1, -6, 2, 2, 6)
                .box(-1.5f, -1.5f, -9, 3, 3, 3);
        list.add(finish(s));

        // Harpie bug swarm: a cloud of buzzing insects.
        Builder swarm = ModelSpec.builder("harpie_bug_swarm").shadow(0.4f);
        swarm.part("core", null, Role.SPIN, 0x6A5A2A).pivot(0, 12, 0).box(-1, -1, -1, 2, 2, 2);
        float[][] bugs = {{-5, -2, -3}, {4, -4, 2}, {-2, 3, 5}, {5, 2, -4}, {0, -6, 0}, {-6, 1, 3}};
        for (int i = 0; i < bugs.length; i++) {
            swarm.part("bug" + i, "core", Role.FLOAT, 0x8A6A2A).pivot(bugs[i][0], bugs[i][1], bugs[i][2])
                    .box(-1.5f, -1, -2, 3, 2, 4).pattern("stripes");
            swarm.part("wings" + i, "bug" + i, i % 2 == 0 ? Role.WING_LEFT : Role.WING_RIGHT, 0xE0E0C0)
                    .pivot(0, -1, 0).box(-3, -0.5f, -1, 6, 1, 2);
        }
        list.add(finish(swarm));

        // Crawling hand: a severed hand scuttling on its fingers.
        Builder hand = ModelSpec.builder("crawling_hand").shadow(0.4f);
        hand.part("palm", null, Role.BODY, 0xB0A080).pivot(0, 19, 0).box(-3.5f, -1.5f, -3, 7, 3, 6);
        hand.part("wrist", "palm", Role.STATIC, 0x8A5A4A).pivot(0, 0, 3).box(-2.5f, -1.5f, 0, 5, 3, 3);
        float[] fx = {-3, -1, 1, 3};
        for (int i = 0; i < 4; i++) {
            hand.part("finger" + i, "palm", Role.LEG_EXTRA, 0xB0A080).pivot(fx[i], 0, -3).rotate(35, 0, 0)
                    .box(-0.75f, -0.5f, -5, 1, 1, 5);
        }
        hand.part("thumb", "palm", Role.LEG_EXTRA, 0xB0A080).pivot(3.5f, 0, 0).rotate(0, -60, 30).box(0, -0.5f, -0.5f, 4, 1, 1);
        hand.part("nails", "palm", Role.STATIC, 0x6A6A40).pivot(0, -1.5f, -2).box(-3, -0.5f, -1, 6, 1, 1);
        list.add(finish(hand));
    }

    // ------------------------------------------------------------------ floating spirits & elementals

    private static void floaters(List<ModelSpec> list) {
        list.add(finish(ghost("ghost", 0xD8E0E8, 0xE8F0F8, 0xC0C8D0, false)));
        list.add(finish(ghost("banshee", 0xB8C0B0, 0xD8E0D0, 0x5A5A50, true)));

        // Aberrant spectre: a huge floating head with gaping nostrils and trailing tendrils.
        Builder sp = ModelSpec.builder("aberrant_spectre").shadow(0.5f);
        sp.part("head", null, Role.FLOAT, 0x8A9A6A).pivot(0, 6, 0).box(-6, -6, -5, 12, 11, 10).pattern("eyes");
        sp.part("nose", "head", Role.STATIC, 0x6A7A4A).pivot(0, 0, -5).box(-2, -2, -3, 4, 4, 3);
        sp.part("mouth", "head", Role.JAW, 0x3A4A2A).pivot(0, 3, -4).box(-4, 0, -1.5f, 8, 2, 2);
        float[][] t = {{-4, 3}, {0, 4}, {4, 3}, {-2, -1}, {2, -1}};
        for (int i = 0; i < t.length; i++) {
            sp.part("tendril" + i, "head", Role.TENTACLE, 0x6A7A5A).pivot(t[i][0], 5, t[i][1]).box(-1, 0, -1, 2, 9 + i % 2 * 3, 2);
        }
        list.add(finish(sp));

        // Icefiend: a spinning cluster of ice crystals with a face.
        Builder ice = ModelSpec.builder("icefiend").shadow(0.4f);
        ice.part("core", null, Role.FLOAT, 0xA0D0F0).pivot(0, 12, 0).box(-3, -4, -3, 6, 8, 6).pattern("eyes");
        ice.part("crystals", "core", Role.SPIN, 0xD0F0FF).pivot(0, 0, 0)
                .box(-6, -2, -1, 3, 5, 2).box(3, -3, -1, 3, 6, 2).box(-1, -9, -1, 2, 5, 2).box(-1, 4, -1, 2, 5, 2);
        list.add(finish(ice));

        // Killerwatt: crackling ball of energy with spikes.
        Builder kw = ModelSpec.builder("killerwatt").shadow(0.4f);
        kw.part("ball", null, Role.FLOAT, 0xF0E060).pivot(0, 11, 0).box(-4, -4, -4, 8, 8, 8).pattern("glow");
        kw.part("face", "ball", Role.HEAD, 0xF0E060).pivot(0, 0, -4).box(-3, -2, -1, 6, 4, 1).pattern("eyes");
        kw.part("spikes", "ball", Role.SPIN, 0xFFFFA0).pivot(0, 0, 0)
                .box(-7, -0.5f, -0.5f, 3, 1, 1).box(4, -0.5f, -0.5f, 3, 1, 1).box(-0.5f, -7, -0.5f, 1, 3, 1)
                .box(-0.5f, 4, -0.5f, 1, 3, 1).box(-0.5f, -0.5f, 4, 1, 1, 3);
        kw.part("tail", "ball", Role.TAIL, 0xC0B030).pivot(0, 3, 3).box(-1, 0, 0, 2, 2, 6);
        list.add(finish(kw));

        // Pyrefiend: a living flame with horns and arms.
        Builder pf = ModelSpec.builder("pyrefiend").shadow(0.4f);
        pf.part("flame", null, Role.FLOAT, 0xE06010).pivot(0, 12, 0).box(-4, -6, -3, 8, 10, 6).pattern("glow");
        pf.part("tip", "flame", Role.TAIL, 0xF0A020).pivot(0, 4, 0).box(-3, 0, -2, 6, 4, 4).box(-1.5f, 4, -1, 3, 4, 2);
        pf.part("head", "flame", Role.HEAD, 0xF08020).pivot(0, -6, 0).box(-3, -5, -3, 6, 5, 6).pattern("eyes");
        pf.part("horns", "head", Role.STATIC, 0x3A1A0A).pivot(0, -5, 0).box(-3, -3, -0.5f, 1, 3, 1).box(2, -3, -0.5f, 1, 3, 1);
        pf.part("jaw", "head", Role.JAW, 0xC04010).pivot(0, -1, -3).box(-2, 0, -0.5f, 4, 1, 1);
        pf.part("arm_l", "flame", Role.ARM_LEFT, 0xE06010).pivot(5, -5, 0).box(-1, -1, -1, 2, 7, 2);
        pf.part("arm_r", "flame", Role.ARM_RIGHT, 0xE06010).pivot(-5, -5, 0).box(-1, -1, -1, 2, 7, 2);
        list.add(finish(pf));

        // Waterfiend: a watery torso hovering on a vortex, with spikes and arms.
        Builder wf = ModelSpec.builder("waterfiend").shadow(0.5f);
        wf.part("torso", null, Role.FLOAT, 0x3A7AC0).pivot(0, 8, 0).box(-4, -6, -3, 8, 8, 6).pattern("glow");
        wf.part("vortex", "torso", Role.SPIN, 0x5A9AD8).pivot(0, 2, 0).box(-3, 0, -3, 6, 4, 6).box(-2, 4, -2, 4, 4, 4)
                .box(-1, 8, -1, 2, 4, 2);
        wf.part("head", "torso", Role.HEAD, 0x6AB0E8).pivot(0, -6, 0).box(-3, -6, -3, 6, 6, 6).pattern("eyes");
        wf.part("fins", "head", Role.STATIC, 0x2A5A9A).pivot(0, -6, 0).box(-0.5f, -3, -2, 1, 3, 6).box(-4, -2, 0, 1, 3, 3)
                .box(3, -2, 0, 1, 3, 3);
        wf.part("arm_l", "torso", Role.ARM_LEFT, 0x3A7AC0).pivot(5, -5, 0).box(-1, -1, -1, 2, 9, 2);
        wf.part("arm_r", "torso", Role.ARM_RIGHT, 0x3A7AC0).pivot(-5, -5, 0).box(-1, -1, -1, 2, 9, 2);
        list.add(finish(wf));

        // Dust devil: a whirling dust vortex with a leering face.
        Builder dd = ModelSpec.builder("dust_devil").shadow(0.5f);
        dd.part("vortex", null, Role.SPIN, 0xB0A080).pivot(0, 24, 0).box(-1, -4, -1, 2, 4, 2).box(-2.5f, -9, -2.5f, 5, 5, 5)
                .box(-4, -15, -4, 8, 6, 8).box(-5.5f, -21, -5.5f, 11, 6, 11);
        dd.part("head", null, Role.HEAD, 0xC8B890).pivot(0, 6, 0).box(-4, -5, -4, 8, 6, 8).pattern("eyes");
        dd.part("jaw", "head", Role.JAW, 0x8A7A5A).pivot(0, 1, -3).box(-3, 0, -1.5f, 6, 2, 2);
        dd.part("horns", "head", Role.STATIC, 0x6A5A3A).pivot(0, -5, 0).box(-4, -3, -1, 1, 3, 1).box(3, -3, -1, 1, 3, 1);
        dd.part("arm_l", null, Role.ARM_LEFT, 0xB0A080).pivot(7, 6, 0).box(-1, -1, -1, 2, 9, 2);
        dd.part("arm_r", null, Role.ARM_RIGHT, 0xB0A080).pivot(-7, 6, 0).box(-1, -1, -1, 2, 9, 2);
        list.add(finish(dd));

        // Smoke devil: a bloated floating body with horns, small wings and trailing smoke.
        Builder sd = ModelSpec.builder("smoke_devil").shadow(0.6f);
        sd.part("body", null, Role.FLOAT, 0x5A5A5A).pivot(0, 8, 0).box(-6, -6, -5, 12, 11, 10).pattern("eyes");
        sd.part("horns", "body", Role.STATIC, 0x2A2A2A).pivot(0, -6, -2).rotate(-20, 0, 0)
                .box(-5, -4, 0, 2, 4, 2).box(3, -4, 0, 2, 4, 2);
        sd.part("mouth", "body", Role.JAW, 0x2A1A1A).pivot(0, 2, -5).box(-4, 0, -1, 8, 2, 1);
        sd.part("wing_l", "body", Role.WING_LEFT, 0x3A3A3A).pivot(6, -3, 1).box(0, -1, -1, 8, 1, 2).box(0, 0, -1, 7, 5, 1);
        sd.part("wing_r", "body", Role.WING_RIGHT, 0x3A3A3A).pivot(-6, -3, 1).box(-8, -1, -1, 8, 1, 2).box(-7, 0, -1, 7, 5, 1);
        float[][] smoke = {{-4, -2}, {0, 2}, {4, -2}, {-2, 3}, {2, -3}};
        for (int i = 0; i < smoke.length; i++) {
            sd.part("smoke" + i, "body", Role.TENTACLE, 0x8A8A8A).pivot(smoke[i][0], 5, smoke[i][1])
                    .box(-1.5f, 0, -1.5f, 3, 6 + (i % 3) * 2, 3);
        }
        list.add(finish(sd));
    }

    // ------------------------------------------------------------------ demons & brutes

    private static void demons(List<ModelSpec> list) {
        // Gargoyle: hunched stone biped with horns, wings and a tail.
        Builder g = biped("gargoyle", 0x7A7A70, 0x6A6A60, 0x5A5A50, 34, 10, 20, 13, true, 0x5A5A50);
        g.part("horns", "head", Role.STATIC, 0x4A4A40).pivot(0, -7, 0).rotate(-25, 0, 0)
                .box(-3.5f, -4, 0, 1, 4, 1).box(2.5f, -4, 0, 1, 4, 1);
        g.part("claws_l", "arm_l", Role.STATIC, 0x3A3A30).pivot(0, 11.5f, 0).box(-1.5f, 0, -2, 3, 2, 1);
        g.part("claws_r", "arm_r", Role.STATIC, 0x3A3A30).pivot(0, 11.5f, 0).box(-1.5f, 0, -2, 3, 2, 1);
        list.add(finish(g));

        // Abyssal demon: tall, thin and hunched, with long clawed arms, horns and a barbed tail.
        Builder a = biped("abyssal_demon", 0x6A2A4A, 0x4A1A3A, 0x3A102A, 38, 8, 35, 18, true, -1);
        a.part("horns", "head", Role.STATIC, 0xC0B090).pivot(0, -7, 1).rotate(-40, 0, 0)
                .box(-3.5f, -6, 0, 1, 6, 1).box(2.5f, -6, 0, 1, 6, 1);
        a.part("claws_l", "arm_l", Role.STATIC, 0xD0C0A0).pivot(0, 16.5f, 0).box(-1.5f, 0, -1.5f, 1, 3, 1).box(0.5f, 0, -1.5f, 1, 3, 1);
        a.part("claws_r", "arm_r", Role.STATIC, 0xD0C0A0).pivot(0, 16.5f, 0).box(-1.5f, 0, -1.5f, 1, 3, 1).box(0.5f, 0, -1.5f, 1, 3, 1);
        a.part("tail_barb", "tail", Role.STATIC, 0xC02020).pivot(0, 0, 12).box(-1.5f, -1.5f, 0, 3, 3, 3);
        list.add(finish(a));

        // Nechryael: bulky fleshy torso, tusked head, little bat wings.
        Builder n = biped("nechryael", 0xC08070, 0xA06050, 0x7A4A3A, 36, 14, 10, 13, false, 0x8A4A3A);
        n.part("belly", "torso", Role.STATIC, 0xD8A090).pivot(0, -4, -3).box(-6, -8, -2, 12, 9, 2);
        n.part("tusks", "head", Role.STATIC, 0xF0E8D0).pivot(0, -1, -4).box(-3, -3, -1, 1, 3, 1).box(2, -3, -1, 1, 3, 1);
        n.part("horn", "head", Role.STATIC, 0x5A3A2A).pivot(0, -7, 0).box(-0.5f, -3, -1, 1, 3, 1);
        list.add(finish(n));

        // Kurask: a hulking brute with a bony crest and huge forearms.
        Builder k = biped("kurask", 0x7A8A5A, 0x5A6A3A, 0x4A5A2A, 40, 12, 15, 17, false, -1);
        k.part("crest", "head", Role.STATIC, 0xC0C090).pivot(0, -8, 1).box(-0.5f, -4, -3, 1, 4, 7).box(-3, -2, 0, 6, 2, 3);
        k.part("fist_l", "arm_l", Role.STATIC, 0x6A7A4A).pivot(0, 14, 0).box(-2.5f, 0, -2.5f, 5, 4, 5);
        k.part("fist_r", "arm_r", Role.STATIC, 0x6A7A4A).pivot(0, 14, 0).box(-2.5f, 0, -2.5f, 5, 4, 5);
        list.add(finish(k));

        // Turoth: lanky creature with a long skull and drooping arms.
        Builder tu = biped("turoth", 0x9A8A6A, 0x7A6A4A, 0x5A4A3A, 38, 8, 25, 18, false, -1);
        tu.part("skull", "head", Role.STATIC, 0xB0A080).pivot(0, -7, 1).rotate(-30, 0, 0).box(-2.5f, -2, 0, 5, 3, 6);
        list.add(finish(tu));

        // Dagannoth: upright sea creature with a beaked head and a thick tail.
        Builder dg = biped("dagannoth", 0x4A6A5A, 0x3A5A4A, 0x2A4A3A, 36, 11, 15, 12, true, -1);
        dg.part("beak", "head", Role.STATIC, 0x8A8A50).pivot(0, -3, -4).box(-2, -2, -4, 4, 3, 4);
        dg.part("spines", "torso", Role.STATIC, 0x8AA090).pivot(0, -12, 3).box(-0.5f, -2, 0, 1, 2, 2).box(-0.5f, 2, 0, 1, 2, 2)
                .box(-0.5f, 6, 0, 1, 2, 2);
        list.add(finish(dg));

        // Lizardman: reptilian warrior with a snout and a tail.
        Builder lz = biped("lizardman", 0x4A7A3A, 0x3A5A2A, 0x3A5A2A, 34, 9, 10, 13, true, -1);
        lz.part("snout", "head", Role.STATIC, 0x5A8A4A).pivot(0, -3, -4).box(-2, -2, -4, 4, 3, 4);
        lz.part("frill", "head", Role.STATIC, 0xC0A030).pivot(0, -7, 1).box(-0.5f, -2, -2, 1, 3, 6);
        list.add(finish(lz));

        // Basilisk knight: an armoured biped with a basilisk's head and a shield.
        Builder bk = biped("basilisk_knight", 0x5A7A3A, 0x6A6A70, 0x4A4A50, 38, 11, 5, 14, true, -1);
        bk.part("snout", "head", Role.STATIC, 0x6A8A4A).pivot(0, -3, -4).box(-2.5f, -2.5f, -4, 5, 4, 4);
        bk.part("frill", "head", Role.STATIC, 0xA0C040).pivot(0, -6, 1).box(-5, -3, 0, 10, 4, 1);
        bk.part("shield", "arm_l", Role.STATIC, 0x8A8A90).pivot(2, 6, 0).box(0, -4, -4, 1, 9, 8).pattern("plates");
        bk.part("sword", "arm_r", Role.STATIC, 0xC0C0C8).pivot(0, 11, -1).rotate(-90, 0, 0).box(-0.5f, -12, -0.5f, 1, 12, 1);
        list.add(finish(bk));

        // Bloodveld: a fleshy body perched on short legs with a gaping maw and long tongue.
        list.add(bloodveld("bloodveld", 0xB04040, 0xE07070));
        list.add(bloodveld("mutated_bloodveld", 0x7A2A6A, 0xB060A0));
    }

    private static ModelSpec bloodveld(String id, int flesh, int brain) {
        Builder b = ModelSpec.builder(id).shadow(0.6f);
        b.part("body", null, Role.BODY, flesh).pivot(0, 14, 0).rotate(-15, 0, 0).box(-5, -14, -4, 10, 14, 8).pattern("spots");
        b.part("brain", "body", Role.STATIC, brain).pivot(0, -14, 1).box(-4, -3, -3, 8, 3, 6).pattern("spots");
        b.part("maw", "body", Role.JAW, 0x3A0A0A).pivot(0, -6, -4).box(-4, 0, -3, 8, 3, 3);
        b.part("teeth", "body", Role.STATIC, 0xF0E8D0).pivot(0, -6, -4).box(-3.5f, -1, -2.5f, 7, 1, 2);
        b.part("tongue", "maw", Role.TENTACLE, 0xE06080).pivot(0, 1, -3).rotate(60, 0, 0).box(-1, 0, -1, 2, 12, 2);
        b.part("eyes", "body", Role.HEAD, flesh).pivot(0, -10, -4).box(-3, -2, -1, 6, 3, 1).pattern("eyes");
        b.part("leg_l", null, Role.LEG_FRONT_LEFT, flesh).pivot(3, 14, 0).box(-1.5f, 0, -1.5f, 3, 10, 3);
        b.part("leg_r", null, Role.LEG_FRONT_RIGHT, flesh).pivot(-3, 14, 0).box(-1.5f, 0, -1.5f, 3, 10, 3);
        b.part("arm_l", "body", Role.ARM_LEFT, flesh).pivot(5.5f, -10, 0).box(-1, -1, -1, 2, 8, 2);
        b.part("arm_r", "body", Role.ARM_RIGHT, flesh).pivot(-5.5f, -10, 0).box(-1, -1, -1, 2, 8, 2);
        return finish(b);
    }

    // ------------------------------------------------------------------ everything else

    private static void oddities(List<ModelSpec> list) {
        // Cave kraken: a bulbous head with glowing eyes surrounded by eight tentacles.
        Builder kr = ModelSpec.builder("cave_kraken").shadow(1.1f);
        kr.part("mantle", null, Role.BODY, 0x8A3A4A).pivot(0, 14, 0).box(-6, -12, -6, 12, 12, 12).pattern("spots");
        kr.part("face", "mantle", Role.HEAD, 0x9A4A5A).pivot(0, -3, -6).box(-5, -3, -1, 10, 5, 1).pattern("eyes");
        kr.part("beak", "mantle", Role.JAW, 0x3A2A2A).pivot(0, 0, -6).box(-1.5f, 0, -2, 3, 2, 2);
        for (int i = 0; i < 8; i++) {
            double ang = Math.toRadians(i * 45);
            float x = (float) (Math.sin(ang) * 5);
            float z = (float) (Math.cos(ang) * 5);
            kr.part("tentacle" + i, "mantle", Role.TENTACLE, 0x7A2A3A).pivot(x, -1, z).rotate((float) (Math.cos(ang) * 50), 0,
                    (float) (-Math.sin(ang) * 50)).box(-1.5f, 0, -1.5f, 3, 12, 3);
        }
        list.add(finish(kr));

        // Rockslug: a slimy slug carrying rocks, with eye stalks.
        Builder rs = ModelSpec.builder("rockslug").shadow(0.5f);
        rs.part("body", null, Role.BODY, 0x8A8A70).pivot(0, 21, 0).box(-3.5f, -3, -7, 7, 6, 14).pattern("spots");
        rs.part("rocks", "body", Role.STATIC, 0x6A6A6A).pivot(0, -3, 1).box(-3, -3, -3, 4, 3, 4).box(0, -4, 0, 3, 4, 3)
                .box(-2, -2, 3, 3, 2, 3);
        rs.part("head", "body", Role.HEAD, 0x9A9A80).pivot(0, 0, -7).box(-2.5f, -2, -3, 5, 4, 3);
        rs.part("stalk_l", "head", Role.TENTACLE, 0x9A9A80).pivot(1.5f, -2, -2).rotate(-20, 0, 15).box(-0.5f, -5, -0.5f, 1, 5, 1);
        rs.part("stalk_r", "head", Role.TENTACLE, 0x9A9A80).pivot(-1.5f, -2, -2).rotate(-20, 0, -15).box(-0.5f, -5, -0.5f, 1, 5, 1);
        rs.part("tail", "body", Role.TAIL, 0x7A7A60).pivot(0, 1, 7).box(-2.5f, -2, 0, 5, 4, 4);
        list.add(finish(rs));

        // Cave slime: a quivering blob with eyes.
        Builder sl = ModelSpec.builder("cave_slime").shadow(0.5f);
        sl.part("blob", null, Role.BODY, 0x6A9A3A).pivot(0, 24, 0).box(-5, -8, -5, 10, 8, 10).pattern("glow");
        sl.part("top", "blob", Role.FLOAT, 0x7AAA4A).pivot(0, -8, 0).box(-3, -3, -3, 6, 3, 6);
        sl.part("face", "blob", Role.HEAD, 0x6A9A3A).pivot(0, -5, -5).box(-3, -1.5f, -1, 6, 3, 1).pattern("eyes");
        sl.part("drip", "blob", Role.TAIL, 0x5A8A2A).pivot(0, -1, 5).box(-1.5f, -1, 0, 3, 2, 3);
        list.add(finish(sl));

        // Jelly: a tall translucent cube with a darker core.
        Builder je = ModelSpec.builder("jelly").shadow(0.6f);
        je.part("cube", null, Role.BODY, 0x3A8AA0).pivot(0, 24, 0).box(-6, -16, -6, 12, 16, 12).pattern("glow");
        je.part("core", "cube", Role.SPIN, 0x1A4A5A).pivot(0, -8, 0).box(-2.5f, -2.5f, -2.5f, 5, 5, 5);
        je.part("face", "cube", Role.HEAD, 0x3A8AA0).pivot(0, -11, -6).box(-4, -2, -1, 8, 4, 1).pattern("eyes");
        je.part("blob", "cube", Role.FLOAT, 0x4A9AB0).pivot(0, -16, 0).box(-4, -3, -4, 8, 3, 8);
        list.add(finish(je));

        // Zygomite: a mutant mushroom with a spotted cap and fungal arms.
        Builder zy = ModelSpec.builder("zygomite").shadow(0.5f);
        zy.part("stem", null, Role.BODY, 0xD8D0A0).pivot(0, 18, 0).box(-3, -12, -3, 6, 12, 6).pattern("eyes");
        zy.part("cap", "stem", Role.STATIC, 0xA03030).pivot(0, -12, 0).box(-8, -4, -8, 16, 4, 16).box(-5, -6, -5, 10, 2, 10)
                .pattern("spots");
        zy.part("mouth", "stem", Role.JAW, 0x5A4A2A).pivot(0, -5, -3).box(-2, 0, -1, 4, 1, 1);
        zy.part("arm_l", "stem", Role.ARM_LEFT, 0xC8C090).pivot(3.5f, -9, 0).box(-1, -1, -1, 2, 8, 2);
        zy.part("arm_r", "stem", Role.ARM_RIGHT, 0xC8C090).pivot(-3.5f, -9, 0).box(-1, -1, -1, 2, 8, 2);
        zy.part("leg_l", null, Role.LEG_FRONT_LEFT, 0xB8B080).pivot(1.5f, 18, 0).box(-1.25f, 0, -1.25f, 2, 6, 2);
        zy.part("leg_r", null, Role.LEG_FRONT_RIGHT, 0xB8B080).pivot(-1.5f, 18, 0).box(-0.75f, 0, -1.25f, 2, 6, 2);
        list.add(finish(zy));

        // Sea snake: a sinuous serpent.
        Builder ss = ModelSpec.builder("sea_snake").shadow(0.5f);
        ss.part("head", null, Role.HEAD, 0x3A6A5A).pivot(0, 18, -8).box(-2.5f, -2, -5, 5, 4, 5).pattern("eyes");
        ss.part("jaw", "head", Role.JAW, 0xC0D0A0).pivot(0, 1.5f, -1).box(-2, 0, -4, 4, 1, 4);
        ss.part("fin", "head", Role.FIN, 0x2A5A4A).pivot(0, -2, 0).box(-0.5f, -2, -2, 1, 2, 4);
        ss.part("seg1", "head", Role.TAIL, 0x3A6A5A).pivot(0, 2, 0).rotate(20, 0, 0).box(-2, -2, 0, 4, 4, 7).pattern("stripes");
        ss.part("seg2", "seg1", Role.TAIL, 0x3A6A5A).pivot(0, 0, 7).rotate(-20, 0, 0).box(-1.75f, -1.75f, 0, 3, 3, 7).pattern("stripes");
        ss.part("seg3", "seg2", Role.TAIL, 0x3A6A5A).pivot(0, 0, 7).box(-1.25f, -1.25f, 0, 2, 2, 7);
        ss.part("tail_fin", "seg3", Role.FIN, 0x2A5A4A).pivot(0, 0, 7).box(-0.5f, -2, 0, 1, 4, 3);
        list.add(finish(ss));

        // Wall beast: a stone wall slab with a face and two huge grasping arms.
        Builder wb = ModelSpec.builder("wall_beast").shadow(0.6f);
        wb.part("wall", null, Role.STATIC, 0x6A6A60).pivot(0, 24, 4).box(-8, -30, 0, 16, 30, 4).pattern("plates");
        wb.part("face", "wall", Role.HEAD, 0x7A6A5A).pivot(0, -22, 0).box(-4, -4, -2, 8, 7, 2).pattern("eyes");
        wb.part("jaw", "face", Role.JAW, 0x4A3A2A).pivot(0, 2, -2).box(-3, 0, -0.5f, 6, 2, 1);
        wb.part("arm_l", "wall", Role.ARM_LEFT, 0x8A7A6A).pivot(5, -16, 0).rotate(-80, 0, 0).box(-2, -2, -2, 4, 14, 4);
        wb.part("arm_r", "wall", Role.ARM_RIGHT, 0x8A7A6A).pivot(-5, -16, 0).rotate(-80, 0, 0).box(-2, -2, -2, 4, 14, 4);
        wb.part("hand_l", "arm_l", Role.STATIC, 0x9A8A7A).pivot(0, 12, 0).box(-3, 0, -3, 6, 4, 6);
        wb.part("hand_r", "arm_r", Role.STATIC, 0x9A8A7A).pivot(0, 12, 0).box(-3, 0, -3, 6, 4, 6);
        list.add(finish(wb));
    }
}
