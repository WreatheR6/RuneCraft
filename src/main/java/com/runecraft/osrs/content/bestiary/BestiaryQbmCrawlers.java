package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.BestiaryBst.chain;
import static com.runecraft.osrs.content.bestiary.BestiaryBst.dark;
import static com.runecraft.osrs.content.bestiary.BestiaryBst.light;
import static com.runecraft.osrs.content.bestiary.BestiaryBst.sideFangs;
import static com.runecraft.osrs.content.bestiary.BestiaryWldBeasts.arachLeg;
import static com.runecraft.osrs.content.bestiary.BestiaryWldFig.side;
import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/**
 * Crawling and slithering quest creatures: Kalrag, the giant scarab and the Champion of Scabaras, the Sea Slug quest's
 * slugs, the giant sea snake, the Basilisk Youngling, and the rock crab of the Rellekka coast.
 */
final class BestiaryQbmCrawlers {
    private BestiaryQbmCrawlers() {
    }

    static List<ModelSpec> overrides() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(kalrag("kalrag"));
        l.add(scarab("giant_scarab", 1.9f, false));
        l.add(scarab("champion_of_scabaras", 2.3f, true));
        l.add(slug("sea_slug", 1.0f, false));
        l.add(slug("slug_prince", 1.6f, true));
        l.add(seaSnake("giant_sea_snake"));
        return l;
    }

    static List<ModelSpec> fresh() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(basilisk("quest_basilisk_boss"));
        l.add(rockCrab("rock_crab"));
        return l;
    }

    // ------------------------------------------------------------------------------------------------
    // Kalrag
    // ------------------------------------------------------------------------------------------------

    /**
     * Kalrag (Underground Pass): Iban's giant spider - a bristling brown-grey body with a pale skull marking on its huge
     * abdomen, a cluster of burning red eyes, dripping fangs and eight long, kinked, hairy legs.
     */
    static ModelSpec kalrag(String id) {
        int body = 0x4A3A2C;
        int dk = 0x2A2018;
        int hair = 0x6A5A48;
        int mark = 0xC8BCA0;
        int eye = 0xFF3020;
        ModelSpec.Builder b = ModelSpec.builder(id).scale(1.8f).shadow(1.2f);
        part(b, "thorax", null, Role.BODY, body).accent(dk).at(0, 17.5f, -2).mat("fur").box(-4.5f, -3, -4.5f, 9, 5, 8).add();
        part(b, "carapace", "thorax", Role.STATIC, dark(body, 0.2f)).accent(hair).at(0, -3, 0).mat("chitin")
                .box(-3.5f, -1, -3.5f, 7, 1, 6).box(-2, -2, -2.5f, 4, 1, 4).add();
        part(b, "pedicel", "thorax", Role.STATIC, dk).at(0, -0.5f, 3.5f).mat("chitin").box(-1.5f, -1.5f, -0.5f, 3, 3, 2).add();
        part(b, "abdomen", "thorax", Role.STATIC, body).accent(mark).at(0, -2, 4.5f).rot(-18, 0, 0).mat("fur")
                .box(-6.5f, -6, 0, 13, 11, 13).add();
        part(b, "abdomen_top", "abdomen", Role.STATIC, dark(body, 0.1f)).at(0, -6, 1.5f).mat("fur").box(-5, -1.5f, 0, 10, 2, 10).add();
        part(b, "skull_mark", "abdomen_top", Role.STATIC, mark).at(0, -1.6f, 2).mat("bone")
                .box(-2.5f, -0.5f, 0, 5, 1, 3).box(-1.5f, -0.5f, 3, 3, 1, 2).box(-2, -0.4f, 5.5f, 1, 1, 2, -0.1f).box(1, -0.4f, 5.5f, 1, 1, 2, -0.1f).add();
        part(b, "eye_holes", "skull_mark", Role.STATIC, 0x1A1410).at(0, -0.6f, 1).mat("bone").box(-1.8f, -0.3f, 0, 1, 1, 1, -0.1f)
                .box(0.8f, -0.3f, 0, 1, 1, 1, -0.1f).add();
        sym(b, "abdomen_side", "abdomen", false, dark(body, 0.05f)).accent(hair).at(6.5f, -1, 6.5f).mat("fur").box(-1, -3.5f, -5, 2, 7, 10).add();
        for (int i = 0; i < 6; i++) {
            float x = (i % 2 == 0 ? 1 : -1) * (2 + (i % 3));
            part(b, "bristle" + i, "abdomen", Role.STATIC, i % 2 == 0 ? hair : dk).at(x, -6.5f, 2 + i * 1.8f).rot(-30, 0, x * 5).mat("hair")
                    .box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        }
        part(b, "abdomen_rear", "abdomen", Role.STATIC, dark(body, 0.08f)).accent(hair).at(0, -0.5f, 13).mat("fur").box(-5, -4.5f, -0.5f, 10, 8, 2).add();
        part(b, "abdomen_front", "abdomen", Role.STATIC, dark(body, 0.08f)).at(0, -0.5f, 0).mat("fur").box(-5, -4.5f, -1.5f, 10, 8, 2).add();
        part(b, "abdomen_belly", "abdomen", Role.STATIC, dk).at(0, 5, 6.5f).mat("fur").box(-5, -0.5f, -5.5f, 10, 2, 11).add();
        for (int i = 0; i < 3; i++) {
            sym(b, "chevron" + i, "abdomen_top", false, mark).at(2.5f, -1.6f, 6.5f + i * 1.6f).rot(0, -30, 0).mat("bone")
                    .box(0, -0.4f, -0.5f, 2, 1, 1, -0.1f).add();
        }
        part(b, "spinnerets", "abdomen", Role.STATIC, dk).at(0, 0, 14.5f).mat("chitin").box(-1.5f, -1.5f, -0.5f, 3, 3, 2)
                .box(-0.5f, -0.5f, 1.5f, 1, 1, 1).add();
        part(b, "head", "thorax", Role.HEAD, dark(body, 0.1f)).accent(eye).at(0, -0.5f, -4.5f).mat("fur").box(-3.5f, -3, -3, 7, 5, 3).add();
        part(b, "eyes", "head", Role.STATIC, eye).at(0, -2.4f, -3).mat("energy").glow("full")
                .box(-2.2f, 0, -0.6f, 1, 1, 1).box(1.2f, 0, -0.6f, 1, 1, 1).box(-1, -0.8f, -0.6f, 2, 1, 1, -0.1f)
                .box(-3.2f, 0.9f, -0.2f, 1, 1, 1, -0.2f).box(2.2f, 0.9f, -0.2f, 1, 1, 1, -0.2f).box(-0.5f, -1.8f, -0.2f, 1, 1, 1, -0.2f).add();
        for (boolean left : new boolean[]{true, false}) {
            side(part(b, "chelicera", "head", Role.JAW, dk).accent(hair).at(1.4f, 1.5f, -2.5f).rot(-15, 0, 0).mat("fur")
                    .box(-1, 0, -1.5f, 2, 3, 2), left);
            side(part(b, "fang", "chelicera*", Role.STATIC, 0x1A1410).at(0, 3, -1).rot(35, 0, 12).mat("horn")
                    .box(-0.5f, 0, -0.5f, 1, 3, 1), left);
            side(part(b, "venom", "fang*", Role.STATIC, 0x9AE040).at(0, 3, 0).mat("slime").glow("full").box(-0.5f, 0, -0.5f, 1, 1, 1, -0.2f), left);
            side(part(b, "palp", "head", Role.STATIC, body).at(3, 1, -2.5f).rot(30, -30, 0).mat("fur")
                    .box(-0.5f, 0, -0.5f, 1, 3, 1), left);
            side(part(b, "palp_tip", "palp*", Role.STATIC, dk).at(0, 3, 0).rot(-50, 0, 0).mat("fur").box(-0.5f, 0, -0.5f, 1, 2, 1, 0.1f), left);
        }
        float[] zs = {-4, -1.8f, 0.4f, 2.6f};
        float[] yaws = {52, 18, -16, -46};
        for (int i = 0; i < 4; i++) {
            for (boolean left : new boolean[]{true, false}) {
                arachLeg(b, "leg" + i, left, "thorax", 4, 0.5f, zs[i], yaws[i], 9, -48, 12, 108, body, hair, "fur");
            }
        }
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Scarabs
    // ------------------------------------------------------------------------------------------------

    /**
     * A giant scarab beetle (Contact!): a domed blue-black carapace with split wing cases, a horned pronotum, raking
     * mandibles, clubbed antennae and six spurred legs. The Champion of Scabaras (Beneath Cursed Sands) is a larger,
     * gold-armoured scarab with a sun-disc between its horns and glowing glyphs of its god along its shell.
     */
    static ModelSpec scarab(String id, float scale, boolean champion) {
        int shell = champion ? 0xB08A2A : 0x1E2A3A;
        int shell2 = champion ? 0x7A5A18 : 0x141C28;
        int trim = champion ? 0xF0D060 : 0x3A6A5A;
        int under = champion ? 0x4A3418 : 0x1A1612;
        int eye = champion ? 0x60E0FF : 0xC0FF60;
        ModelSpec.Builder b = ModelSpec.builder(id).scale(scale).shadow(1.2f);
        part(b, "body", null, Role.BODY, under).accent(shell).at(0, 17, 1).mat("chitin").box(-5, -3, -6, 10, 5, 13).add();
        // wing cases (elytra) with a seam, ridges and a rim
        sym(b, "elytron", "body", false, shell).accent(trim).at(0.2f, -3, -2).rot(-4, 0, 6).mat("chitin").glow(champion ? "runes" : "none")
                .box(0, -4, 0, 6, 5, 11).add();
        sym(b, "elytron_dome", "elytron", true, shell).accent(trim).at(0, -4, 1).mat("chitin").box(0.5f, -1.5f, 0, 4, 2, 9).add();
        sym(b, "elytron_rim", "elytron", true, shell2).accent(trim).at(5.5f, 0, 0).mat("chitin").box(0, -2, -0.5f, 1, 3, 12).add();
        sym(b, "elytron_tip", "elytron", true, shell2).at(2.5f, -1, 11).rot(20, 0, 0).mat("chitin").box(-2, -2, -0.5f, 4, 3, 2).add();
        for (int i = 0; i < 3; i++) {
            sym(b, "stria" + i, "elytron_dome", true, i == 1 ? trim : shell2).at(1 + i * 1.3f, -1.6f, 0.5f).mat("chitin")
                    .box(0, -0.3f, 0, 1, 1, 8, -0.15f).add();
        }
        // pronotum (shield behind the head) and the horn
        part(b, "pronotum", "body", Role.STATIC, shell).accent(trim).at(0, -3, -6).rot(-8, 0, 0).mat("chitin").glow(champion ? "runes" : "none")
                .box(-5.5f, -4, -4, 11, 5, 5).add();
        part(b, "pronotum_rim", "pronotum", Role.STATIC, trim).at(0, 1, -4).mat(champion ? "metal" : "chitin").box(-5.5f, -1, -0.5f, 11, 1, 1).add();
        part(b, "horn", "pronotum", Role.STATIC, shell2).accent(trim).at(0, -4, -2).rot(-40, 0, 0).mat("horn").box(-1, -5, -1, 2, 5, 2).add();
        part(b, "horn_tip", "horn", Role.STATIC, champion ? trim : shell2).at(0, -5, 0).rot(-30, 0, 0).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        sym(b, "side_horn", "pronotum", false, shell2).at(4, -3.5f, -2.5f).rot(-30, 0, -25).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        // head
        part(b, "head", "pronotum", Role.HEAD, shell2).accent(eye).at(0, 0, -4).mat("chitin").box(-3.5f, -2, -4, 7, 4, 4).add();
        part(b, "clypeus", "head", Role.STATIC, shell).accent(trim).at(0, -2, -4).rot(20, 0, 0).mat("chitin").box(-4, -1, -2, 8, 2, 2).add();
        BestiaryQbm.row(b, "clypeus_teeth", "clypeus", trim, "horn", 0, -1, -2, 7, 4, 1, 1, 1);
        b.part("eyes", "head", Role.STATIC, eye).pivot(0, -0.5f, -3.5f).material("energy").glow("full")
                .box(2.8f, -0.5f, -0.5f, 1, 1, 1).box(-3.8f, -0.5f, -0.5f, 1, 1, 1);
        for (boolean left : new boolean[]{true, false}) {
            side(part(b, "mandible", "head", Role.JAW, shell2).at(1.5f, 1, -4).rot(10, 20, 0).mat("horn").box(-0.5f, -0.5f, -3, 2, 1, 3), left);
            side(part(b, "mandible_hook", "mandible*", Role.STATIC, champion ? trim : 0x101010).at(0.5f, 0, -3).rot(0, -55, 0).mat("horn")
                    .box(-0.5f, -0.5f, -2, 1, 1, 2), left);
            side(part(b, "antenna", "head", Role.STATIC, shell2).at(2.5f, -1.5f, -3.5f).rot(-30, 30, -20).mat("chitin")
                    .box(-0.5f, -3, -0.5f, 1, 3, 1), left);
            side(part(b, "antenna_club", "antenna*", Role.STATIC, trim).at(0, -3, 0).rot(10, 0, 0).mat("chitin")
                    .box(-1, -2, -1, 2, 2, 2, -0.2f), left);
        }
        if (champion) {
            part(b, "sun_disc", "horn", Role.STATIC, 0xFFC040).accent(0xFFF0A0).at(0, -4, 1.5f).mat("energy").glow("full")
                    .box(-2.5f, -2.5f, -0.5f, 5, 5, 1).add();
            part(b, "sun_rim", "sun_disc", Role.STATIC, trim).at(0, 0, 0.3f).mat("metal").box(-3, -3, 0, 6, 1, 1).box(-3, 2, 0, 6, 1, 1)
                    .box(-3, -2, 0, 1, 4, 1).box(2, -2, 0, 1, 4, 1).add();
            part(b, "collar_gold", "pronotum", Role.STATIC, trim).at(0, -4, 1).mat("metal").box(-5, -0.5f, -0.5f, 10, 1, 2).add();
        }
        // six spurred legs
        float[] zs = {-5, 0, 4.5f};
        float[] yaws = {40, -5, -40};
        for (int i = 0; i < 3; i++) {
            for (boolean left : new boolean[]{true, false}) {
                arachLeg(b, "leg" + i, left, "body", 4.5f, 1, zs[i], yaws[i], 6, -30, 8, 96, shell2, trim, "chitin");
                side(part(b, "spur" + i, "leg" + i + "_tibia*", Role.STATIC, champion ? trim : shell).at(3, -0.5f, 0).rot(0, 0, -40).mat("horn")
                        .box(0, -1.5f, -0.5f, 1, 2, 1).box(3, -1.5f, -0.5f, 1, 2, 1), left);
            }
        }
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Slugs
    // ------------------------------------------------------------------------------------------------

    /**
     * A sea slug (The Slug Menace / Sea Slug): a fat, glistening grey-green slug with a ribbed mantle, a frilled foot,
     * papillae along its back, eyes glowing on long stalks and drooping feelers. The Slug Prince rears up with a
     * spined crest like a crown and a hypnotic violet glow.
     */
    static ModelSpec slug(String id, float scale, boolean prince) {
        int skin = prince ? 0x6A7A4A : 0x7A8A5A;
        int dk = prince ? 0x3A4A26 : 0x4A5A36;
        int pale = prince ? 0xA8B080 : 0xB8C090;
        int glow = prince ? 0xC060FF : 0xE0FF60;
        ModelSpec.Builder b = ModelSpec.builder(id).scale(scale).shadow(1.0f);
        // the foot: a long, low body tapering into the tail, with a frilled skirt
        part(b, "foot", null, Role.BODY, skin).accent(dk).at(0, 22, 0).mat("slime").box(-4.5f, -2, -8, 9, 4, 14).add();
        sym(b, "skirt", "foot", false, pale).at(4.5f, 1.2f, -1).rot(0, 0, -20).mat("slime").box(-0.5f, -0.5f, -7.5f, 2, 1, 14).add();
        part(b, "sole_front", "foot", Role.STATIC, pale).at(0, 1.2f, -8).rot(-20, 0, 0).mat("slime").box(-4, -0.5f, -1.5f, 8, 1, 2).add();
        for (int i = 0; i < 4; i++) {
            sym(b, "ripple" + i, "skirt", true, i % 2 == 0 ? light(pale, 0.1f) : dark(pale, 0.1f)).at(1, 0.2f, -6 + i * 3.6f).rot(0, 0, 10)
                    .mat("slime").box(-0.5f, -0.5f, -1, 1, 1, 2).add();
        }
        String tail = chain(b, "tail", "foot", Role.TAIL, skin, dk, "slime", 0, 0.5f, 6, false, new int[]{6, 5, 3, 2},
                new int[]{4, 4, 3, 3}, new float[]{-2, 3, 3, 4}, null);
        part(b, "tail_fin", tail, Role.STATIC, pale).at(0, 0.5f, 3).mat("membrane").box(-1.5f, -0.5f, 0, 3, 1, 2).add();
        for (int i = 0; i < 3; i++) {
            part(b, "tail_ridge" + i, "tail" + i, Role.STATIC, i % 2 == 0 ? pale : dk).at(0, -(6 - i * 1.5f) / 2f, 1).mat("slime")
                    .box(-0.5f, -1, 0, 1, 1, 3).add();
        }
        // the mantle over the back: stepped into a dome, spotted, with papillae
        part(b, "mantle", "foot", Role.STATIC, dk).accent(pale).at(0, -2, -1).rot(-3, 0, 0).mat("slime").pattern("spots")
                .box(-4, -3, -6, 8, 3, 11).add();
        part(b, "mantle_top", "mantle", Role.STATIC, skin).accent(pale).at(0, -3, 0).mat("slime").pattern("spots").box(-3, -1.5f, -4.5f, 6, 2, 9).add();
        part(b, "mantle_crown", "mantle_top", Role.STATIC, light(skin, 0.08f)).at(0, -1.5f, 0).mat("slime").box(-2, -1, -3, 4, 1, 6).add();
        sym(b, "mantle_rim", "mantle", false, pale).at(4, -0.5f, -0.5f).mat("slime").box(-0.5f, -1, -5.5f, 1, 2, 10).add();
        part(b, "mantle_lip", "mantle", Role.STATIC, pale).at(0, -0.5f, -6).rot(15, 0, 0).mat("slime").box(-3.5f, -1, -1, 7, 2, 1).add();
        for (int i = 0; i < 10; i++) {
            float x = (i % 2 == 0 ? 1 : -1) * (i < 6 ? 1.8f : 3.2f);
            part(b, "papilla" + i, i < 6 ? "mantle_top" : "mantle", Role.STATIC, i % 2 == 0 ? pale : light(skin, 0.15f))
                    .at(x, i < 6 ? -1.5f : -3, i < 6 ? -3.5f + i * 1.4f : -4 + (i - 6) * 2.6f)
                    .rot(-15, 0, x * 8).mat("slime").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        }
        part(b, "breathing_pore", "mantle", Role.STATIC, 0x2A2A1A).at(3.6f, -1.5f, 2.5f).mat("slime").box(0, -0.5f, -1, 1, 1, 2).add();
        // head end: raised neck with stalked eyes, feelers and a mouth
        float rear = prince ? -55 : -30;
        part(b, "neck", "foot", Role.NECK, skin).accent(dk).at(0, -0.5f, -6.5f).rot(rear, 0, 0).mat("slime").box(-3, -5, -3, 6, 6, 5).add();
        part(b, "neck_fold", "neck", Role.STATIC, pale).at(0, -1, -3).mat("slime").box(-3.2f, -1, -0.5f, 6, 1, 1, 0.1f).box(-3.2f, -3.5f, -0.3f, 6, 1, 1, 0.1f).add();
        part(b, "head", "neck", Role.HEAD, skin).accent(glow).at(0, -5, -0.5f).rot(-rear * 0.7f, 0, 0).mat("slime")
                .box(-2.5f, -2.5f, -3.5f, 5, 3, 4).add();
        part(b, "mouth", "head", Role.JAW, 0x2A2416).at(0, 1, -4).mat("flesh").box(-1.5f, -0.5f, -0.6f, 3, 1, 1).add();
        part(b, "oral_hood", "head", Role.STATIC, pale).at(0, 0.5f, -4).rot(-10, 0, 0).mat("slime").box(-3, -0.5f, -1.5f, 6, 1, 2).add();
        for (boolean left : new boolean[]{true, false}) {
            side(part(b, "stalk", "head", Role.TENTACLE, skin).at(1.6f, -3, -2).rot(-18, 0, 16).mat("slime").box(-0.5f, -4, -0.5f, 1, 4, 1), left);
            side(part(b, "stalk_up", "stalk*", Role.STATIC, skin).at(0, -4, 0).rot(-14, 0, 8).mat("slime").box(-0.5f, -3, -0.5f, 1, 3, 1), left);
            side(part(b, "eye", "stalk_up*", Role.STATIC, glow).at(0, -3, 0).mat("energy").glow("full").box(-1, -1.5f, -1, 2, 2, 2, -0.2f), left);
            side(part(b, "feeler", "head", Role.TENTACLE, pale).at(2.5f, 0.5f, -3.5f).rot(40, -30, 0).mat("slime").box(-0.5f, -0.5f, -3, 1, 1, 3), left);
        }
        if (prince) {
            int crown = 0x4A2A6A;
            for (int i = 0; i < 5; i++) {
                float a = (i - 2) * 18;
                part(b, "crest" + i, "head", Role.STATIC, i % 2 == 0 ? crown : light(crown, 0.2f)).accent(glow).at((i - 2) * 1.2f, -3, 0)
                        .rot(-20, 0, a).mat("horn").box(-0.5f, -3 - (i == 2 ? 2 : i % 2), -0.5f, 1, 3 + (i == 2 ? 2 : i % 2), 1).add();
            }
            part(b, "mind_gem", "head", Role.STATIC, glow).at(0, -2.5f, -4.1f).mat("energy").glow("full").box(-1, -1, -0.4f, 2, 2, 1).add();
            sym(b, "frill", "neck", false, dk).accent(glow).at(3.5f, -4, -1).rot(0, -20, -15).mat("membrane").box(0, -2, -2, 3, 5, 4).add();
            part(b, "slime_trail", "foot", Role.STATIC, glow).at(0, 3, 4).mat("slime").glow("full").box(-4, -0.2f, 0, 8, 1, 6, -0.3f).add();
        }
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Giant sea snake
    // ------------------------------------------------------------------------------------------------

    /**
     * The giant sea snake (Royal Trouble): a vast banded sea serpent coiled on the cave floor, its neck rearing up into a
     * finned, flat-browed head with a gaping fanged maw.
     */
    static ModelSpec seaSnake(String id) {
        int skin = 0x2A6A5A;
        int band = 0x163A32;
        int belly = 0xA8C890;
        int fin = 0x3A8A70;
        ModelSpec.Builder b = ModelSpec.builder(id).scale(2.0f).shadow(1.3f);
        // ground coil
        part(b, "coil", null, Role.BODY, skin).accent(band).at(0, 21, 3).mat("scales").pattern("stripes").box(-5, -3, -5, 10, 6, 10).add();
        String coil = chain(b, "loop", "coil", Role.TAIL, skin, band, "scales", 4, 0, 4, false, new int[]{5, 5, 4, 4, 3, 2},
                new int[]{5, 5, 5, 4, 4, 4}, new float[]{0, 0, 0, 0, 0, 0}, new float[]{40, 50, 50, 45, 40, 30});
        part(b, "tail_fin", coil, Role.STATIC, fin).accent(band).at(0, 0, 4).mat("membrane").box(-0.5f, -2, 0, 1, 4, 3).add();
        // rearing neck
        String neck = chain(b, "neck", "coil", Role.NECK, skin, band, "scales", 0, -2, -3, true, new int[]{6, 6, 6, 5, 5},
                new int[]{4, 4, 4, 4, 3}, new float[]{-60, -10, 0, 12, 24}, null);
        for (int i = 0; i < 6; i++) {
            part(b, "loop_belly" + i, "loop" + i, Role.STATIC, belly).at(0, 0, 0).mat("plates")
                    .box(-(5 - i / 2) / 2f + 0.5f, (5 - i / 2) / 2f - 1, 0.5f, 4 - i / 2, 1, 4, -0.1f).add();
            part(b, "loop_ridge" + i, "loop" + i, Role.STATIC, i % 2 == 0 ? fin : band).at(0, -(5 - i / 2) / 2f, 1).mat("membrane")
                    .box(-0.5f, -2 + i / 3, 0, 1, 2 - i / 3, 3).add();
        }
        part(b, "coil_ridge", "coil", Role.STATIC, fin).accent(band).at(0, -3, 0).mat("membrane").box(-0.5f, -2, -3, 1, 2, 6).add();
        part(b, "coil_belly", "coil", Role.STATIC, belly).at(0, 2.5f, 0).mat("plates").box(-4, -0.5f, -4, 8, 1, 8, 0.1f).add();
        for (int i = 0; i < 5; i++) {
            part(b, "belly" + i, "neck" + i, Role.STATIC, belly).at(0, 0, 0).mat("plates").box(-2, -0.5f, -3.8f, 4, 3, 4, -0.1f).add();
            part(b, "dorsal" + i, "neck" + i, Role.STATIC, fin).accent(band).at(0, -3, -2).rot(-10, 0, 0).mat("membrane").box(-0.5f, -3, -1.5f, 1, 3, 3).add();
        }
        part(b, "head", neck, Role.HEAD, skin).accent(0xFFE040).at(0, 0, -4).rot(40, 0, 0).mat("scales").box(-3.5f, -2.5f, -6, 7, 4, 6).add();
        part(b, "brow", "head", Role.STATIC, band).at(0, -2.5f, -5).rot(10, 0, 0).mat("scales").box(-4, -1, -1, 8, 2, 3).add();
        part(b, "snout", "head", Role.STATIC, skin).at(0, 0, -6).mat("scales").box(-2.5f, -2, -3, 5, 3, 3).add();
        b.part("eyes", "head", Role.STATIC, 0xFFE040).pivot(0, -1.6f, -5.4f).material("energy").glow("full")
                .box(2.6f, -0.5f, -0.5f, 1, 1, 1).box(-3.6f, -0.5f, -0.5f, 1, 1, 1);
        part(b, "jaw", "head", Role.JAW, belly).accent(0xC03030).at(0, 1.5f, -2).rot(24, 0, 0).mat("scales").glow("maw")
                .box(-3, 0, -7, 6, 2, 7).add();
        sideFangs(b, "fangs", "snout", 0xF0E8D0, 2.4f, 1, -2.6f, 1.6f, 2, 2, false);
        sym(b, "head_fin", "head", false, fin).accent(band).at(3.5f, -1, -1).rot(0, -40, -15).mat("membrane").box(0, -2.5f, -0.5f, 4, 5, 1).add();
        sym(b, "head_fin_b", "head", false, dark(fin, 0.2f)).at(3.5f, -0.5f, 1).rot(0, -55, -10).mat("membrane").box(0, -1.5f, -0.5f, 3, 3, 1).add();
        sym(b, "barbel", "snout", false, belly).at(2, 1, -2).rot(30, -20, 0).mat("flesh").box(-0.5f, 0, -0.5f, 1, 4, 1).add();
        part(b, "crest_fin", "head", Role.STATIC, fin).accent(band).at(0, -2.5f, -1).rot(-30, 0, 0).mat("membrane").box(-0.5f, -4, -2, 1, 4, 5).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Basilisk Youngling
    // ------------------------------------------------------------------------------------------------

    /**
     * The Basilisk Youngling (The Fremennik Exiles): a squat, heavy lizard of mossy green scales with a ridge of bony
     * spikes, a horned, frilled head with petrifying glowing eyes, splayed clawed legs and a thick tail.
     */
    static ModelSpec basilisk(String id) {
        int scale = 0x4A6A3A;
        int dk = 0x26381E;
        int belly = 0xB0A870;
        int spike = 0xC8BC98;
        int eye = 0xC8FF40;
        ModelSpec.Builder b = ModelSpec.builder(id).scale(1.0f).shadow(1.2f);
        part(b, "body", null, Role.BODY, scale).accent(dk).at(0, 15, 0).mat("scales").box(-6, -5, -8, 12, 9, 16).add();
        part(b, "belly", "body", Role.STATIC, belly).at(0, 4, 0).mat("plates").box(-5, -0.5f, -7, 10, 1, 14).add();
        sym(b, "flank", "body", false, dark(scale, 0.12f)).accent(dk).at(6, -1, 0).mat("scales").box(-0.5f, -3, -7, 1, 6, 14).add();
        for (int i = 0; i < 7; i++) {
            int h = new int[]{3, 4, 5, 5, 4, 3, 2}[i];
            part(b, "spike" + i, "body", Role.STATIC, i % 2 == 0 ? spike : dark(spike, 0.15f)).at(0, -5, -7 + i * 2.4f).rot(-20, 0, 0).mat("horn")
                    .box(-1, -h, -1, 2, h, 2).box(-0.5f, -h - 1, -0.5f, 1, 1, 1).add();
            if (i % 2 == 1) {
                sym(b, "side_spike" + i, "body", false, spike).at(4.5f, -4, -7 + i * 2.4f).rot(-15, 0, -40).mat("horn").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
            }
        }
        part(b, "neck", "body", Role.NECK, scale).accent(dk).at(0, -1, -8).rot(-10, 0, 0).mat("scales").box(-4.5f, -4, -4, 9, 7, 5).add();
        part(b, "head", "neck", Role.HEAD, scale).accent(eye).at(0, -0.5f, -4).rot(10, 0, 0).mat("scales").box(-4, -3.5f, -6, 8, 6, 6).add();
        part(b, "snout", "head", Role.STATIC, dark(scale, 0.08f)).at(0, 0.5f, -6).mat("scales").box(-3, -2, -3, 6, 3, 3).add();
        part(b, "nostrils", "snout", Role.STATIC, 0x141A10).at(0, -1.5f, -3).mat("scales").box(-2, -0.5f, -0.3f, 1, 1, 1, -0.1f).box(1, -0.5f, -0.3f, 1, 1, 1, -0.1f).add();
        part(b, "brow", "head", Role.STATIC, dk).at(0, -3, -5.5f).rot(15, 0, 0).mat("scales").box(-4.5f, -1, -1, 9, 2, 2).add();
        b.part("eyes", "head", Role.STATIC, eye).pivot(0, -1.6f, -6.2f).material("energy").glow("full")
                .box(1.5f, -0.5f, -0.4f, 2, 2, 1).box(-3.5f, -0.5f, -0.4f, 2, 2, 1);
        part(b, "jaw", "head", Role.JAW, belly).at(0, 2.5f, -4).rot(14, 0, 0).mat("plates").box(-3.5f, 0, -5, 7, 2, 5).add();
        sideFangs(b, "teeth", "snout", 0xF0E8D0, 2.8f, 1, -2.6f, 1.4f, 2, 1, false);
        sym(b, "frill", "head", false, dk).accent(0xD0A030).at(4, -1, -1).rot(0, -30, -15).mat("membrane").box(0, -3, -1, 4, 5, 2).add();
        sym(b, "horn", "head", false, spike).at(3, -3.5f, -2).rot(-55, -15, -15).mat("horn").box(-1, -4, -1, 2, 4, 2).add();
        sym(b, "horn_tip", "horn", true, light(spike, 0.1f)).at(0, -4, 0).rot(-25, 0, 0).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        part(b, "crown_horn", "head", Role.STATIC, spike).at(0, -3.5f, -1).rot(-40, 0, 0).mat("horn").box(-1, -3, -1, 2, 3, 2).add();
        // splayed lizard legs
        for (boolean front : new boolean[]{true, false}) {
            String n = front ? "leg_f" : "leg_b";
            sym(b, n, null, false, scale).roles(front ? Role.LEG_FRONT_LEFT : Role.LEG_BACK_LEFT, front ? Role.LEG_FRONT_RIGHT : Role.LEG_BACK_RIGHT)
                    .accent(dk).at(5.5f, 16, front ? -5 : 5).rot(0, 0, -50).mat("scales").box(-2, -1.5f, -2, 4, 7, 4).add();
            sym(b, n + "_lo", n, true, dark(scale, 0.1f)).at(0, 6, 0).rot(0, 0, 55).mat("scales").box(-1.5f, 0, -1.5f, 3, 5, 3).add();
            sym(b, n + "_paw", n + "_lo", true, dk).at(0, 4.5f, 0).rot(0, 0, -5).mat("scales").box(-2, 0, -3.5f, 4, 2, 5).add();
            Kit.Sym cl = sym(b, n + "_claw", n + "_paw", true, spike).at(0, 1, -3.5f).mat("horn");
            for (int k = 0; k < 3; k++) {
                cl.box(-1.8f + k * 1.4f, -0.6f, -1.6f, 1, 1, 2);
            }
            cl.add();
        }
        chain(b, "tail", "body", Role.TAIL, scale, dk, "scales", 0, -1, 8, false, new int[]{6, 5, 4, 3, 2},
                new int[]{4, 4, 4, 4, 3}, new float[]{10, 5, 5, -5, -5}, new float[]{0, 8, 8, 8, 8});
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Rock crab
    // ------------------------------------------------------------------------------------------------

    /**
     * Rock crab (Rellekka coast): a crab hiding under a shell that is a lichen-spotted grey boulder - when it sits still
     * only the rock shows; stalked eyes peer from under its rim, and two stony pincers and eight jointed legs spread
     * from beneath it.
     */
    static ModelSpec rockCrab(String id) {
        int rock = 0x7A7468;
        int rock2 = 0x5E5A50;
        int moss = 0x6A7A3A;
        int body = 0x8A5A3A;
        int dk = 0x4A2E1E;
        ModelSpec.Builder b = ModelSpec.builder(id).scale(1.0f).shadow(0.9f);
        part(b, "body", null, Role.BODY, body).accent(dk).at(0, 19, 0).mat("chitin").box(-5.5f, -2, -5, 11, 4, 10).add();
        // boulder shell
        part(b, "boulder", "body", Role.STATIC, rock).accent(rock2).at(0, -2, 0.5f).rot(-4, 0, 0).mat("rock")
                .box(-6.5f, -5, -6, 13, 5, 12).add();
        part(b, "boulder_rim", "boulder", Role.STATIC, rock2).accent(rock).at(0, 0, 0).mat("rock").box(-7, -1, -6.5f, 14, 3, 13).add();
        part(b, "boulder_top", "boulder", Role.STATIC, rock).accent(rock2).at(0.5f, -5, 0).rot(0, 8, 0).mat("rock")
                .box(-5, -3, -4.5f, 10, 3, 9).add();
        part(b, "boulder_cap", "boulder_top", Role.STATIC, rock2).at(-0.5f, -3, 0.5f).rot(0, -12, 0).mat("rock").box(-3, -2, -2.5f, 6, 2, 5).add();
        sym(b, "boulder_lump", "boulder", false, rock2).at(5.5f, -2.5f, 1.5f).rot(0, 15, -10).mat("rock").box(-1.5f, -2.5f, -3, 3, 4, 6).add();
        part(b, "boulder_front", "boulder", Role.STATIC, rock2).at(0, -2, -6).rot(10, 0, 0).mat("rock").box(-5, -2.5f, -1, 10, 4, 2).add();
        part(b, "lichen", "boulder_top", Role.STATIC, moss).accent(0x9AAA5A).at(0, -3, 0).mat("moss")
                .box(-4, -0.5f, -2, 3, 1, 3, -0.1f).box(1.5f, -0.5f, 1, 3, 1, 2, -0.1f).box(-1, -2.5f, -4, 2, 1, 2, -0.1f).add();
        part(b, "barnacles", "boulder", Role.STATIC, 0xC8C0A8).at(0, 0, 0).mat("bone").box(5.5f, -4, -4, 1, 1, 1).box(6, -2, 3, 1, 1, 1)
                .box(-7, -3, -2, 1, 1, 1).box(-6.5f, -4.5f, 4, 1, 1, 1).add();
        // eyes on stalks peeking from under the front rim, and the mouth
        part(b, "face", "body", Role.HEAD, body).accent(0x101010).at(0, 0, -5).mat("chitin").box(-3, -1, -1.5f, 6, 2, 2).add();
        part(b, "mouth", "face", Role.JAW, dk).at(0, 1, -1.5f).mat("chitin").box(-1.5f, 0, -0.6f, 3, 1, 1).add();
        for (boolean left : new boolean[]{true, false}) {
            side(part(b, "eyestalk", "face", Role.STATIC, body).at(1.5f, -1, -1).rot(-35, 0, 14).mat("chitin").box(-0.5f, -3, -0.5f, 1, 3, 1), left);
            side(part(b, "eye", "eyestalk*", Role.STATIC, 0x141010).at(0, -3, 0).mat("energy").box(-0.75f, -1.5f, -0.75f, 2, 2, 2, -0.25f), left);
            Role ar = left ? Role.ARM_LEFT : Role.ARM_RIGHT;
            side(part(b, "arm", "body", ar, body).at(4.5f, 0.5f, -4).rot(0, -30, -10).mat("chitin").box(-1, -1, -4, 2, 2, 4), left);
            side(part(b, "forearm", "arm*", Role.STATIC, body).at(0, 0, -4).rot(0, 55, 0).mat("chitin").box(-1.5f, -1.5f, -3, 3, 3, 3), left);
            side(part(b, "claw", "forearm*", Role.STATIC, rock).at(0, 0, -3).rot(0, -15, 0).mat("rock").box(-2.5f, -2.5f, -3, 5, 4, 3), left);
            side(part(b, "claw_fixed", "claw*", Role.STATIC, rock2).at(-1.2f, 0, -3).rot(0, 10, 0).mat("rock").box(-1, -1.5f, -4, 2, 3, 4), left);
            side(part(b, "claw_moving", "claw*", Role.JAW, rock2).at(1.3f, -0.5f, -3).rot(0, -20, 0).mat("rock").box(-0.75f, -1, -4, 2, 2, 4), left);
        }
        for (int i = 0; i < 4; i++) {
            for (boolean left : new boolean[]{true, false}) {
                arachLeg(b, "leg" + i, left, "body", 5, 0.5f, -2.5f + i * 2.2f, 20 - i * 18, 4, -30, 6, 100, body, dk, "chitin");
            }
        }
        return b.build();
    }
}
