package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/**
 * Flagship boss re-sculpts, part 2 (round 11): Zulrah's three forms, the Alchemical Hydra's four phases, the King
 * Black Dragon, the Kalphite Queen's two forms, the Abyssal Sire, the Grotesque Guardians (Dusk and Dawn) and
 * Sarachnis. Same style as {@link Flagships}: OSRS
 * silhouettes and palettes, signature details, painter materials and glows. Registered after it in
 * {@link Overrides}, so these replace every earlier version of the same model id.
 */
final class Flagships2 {
    private Flagships2() {
    }

    static List<ModelSpec> overrides() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(zulrah("zulrah", 0x3E7A2C, 0xB4C46A, 0x24521E, 0xFFE040, null));
        l.add(zulrah("zulrah_magma", 0x9A2A18, 0xE8823A, 0x5E140C, 0xFFC040, "cracks"));
        l.add(zulrah("zulrah_tanzanite", 0x2E3486, 0x3EC2C8, 0x1C1E58, 0x8AF6FF, "spots"));
        l.add(hydra("alchemical_hydra", 4, 0x56664A, 0xA0A87A, 0x8AF040));
        l.add(hydra("alchemical_hydra_blue", 3, 0x4C5A66, 0x98A8B0, 0x5AB8FF));
        l.add(hydra("alchemical_hydra_red", 2, 0x66504A, 0xB0948A, 0xFF5A30));
        l.add(hydra("alchemical_hydra_enraged", 1, 0x3C3C42, 0x7A7A80, 0xEDEDFF));
        l.add(kingBlackDragon());
        l.add(kalphiteQueen());
        l.add(kalphiteQueenAirborne());
        l.add(gargoyle("dusk", 0x4E4A50, 0x2E2B30, 0xFF8A3A, false));
        l.add(gargoyle("dawn", 0xB8B2A8, 0x8A847A, 0x7AD8FF, true));
        l.add(sarachnis());
        l.add(abyssalSire());
        return l;
    }

    private static final int BONE = 0xE6DCC4;
    private static final int TOOTH = 0xF0EADA;

    /**
     * Zulrah: the great swamp serpent risen from Zul-Andra's water in tight coils, an S-curved neck with pale
     * banded belly scutes, a back-swept crown of finned spines around the head, a fanged maw, and the arched loop
     * of its tail breaking the surface behind. The three forms (serpentine, magma, tanzanite) share the shape.
     */
    static ModelSpec zulrah(String id, int skin, int belly, int fin, int eye, String glow) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.6f);
        Kit.P coil = part(b, "coil", null, Role.BODY, skin).accent(belly).at(0, 21, 2).mat("scales")
                .box(-6, -3, -10, 12, 5, 5).box(-6, -3, 5, 12, 5, 5).box(5, -3, -6, 5, 5, 12).box(-10, -3, -6, 5, 5, 12);
        if (glow != null) {
            coil.glow(glow);
        }
        coil.add();
        Kit.P coil2 = part(b, "coil2", "coil", Role.STATIC, skin).accent(belly).at(0, -3.5f, 0).rot(0, 45, 0).mat("scales")
                .box(-5, -2, -8, 10, 4, 4).box(-5, -2, 4, 10, 4, 4).box(4, -2, -5, 4, 4, 10).box(-8, -2, -5, 4, 4, 10);
        if (glow != null) {
            coil2.glow(glow);
        }
        coil2.add();
        // S-curved neck: leans back, then forward over the water
        int[][] seg = {{8, 12, 7}, {7, 11, 6}, {6, 9, 6}};
        float[] pitch = {-12, 20, 16};
        String parent = "coil2";
        float py = -2;
        for (int i = 0; i < 3; i++) {
            int w = seg[i][0];
            int h = seg[i][1];
            int d = seg[i][2];
            String n = "neck" + i;
            part(b, n, parent, Role.NECK, skin).accent(belly).at(0, py, i == 0 ? -4 : 0).rot(pitch[i], 0, 0).mat("scales")
                    .box(-w / 2f, -h, -d / 2f, w, h, d).add();
            part(b, n + "_belly", n, Role.STATIC, belly).at(0, -h / 2f, -d / 2f - 0.1f).mat("plates")
                    .box(-(w - 2) / 2f, -h / 2f, -0.6f, w - 2, h, 1).add();
            part(b, n + "_dorsal", n, Role.FIN, fin).accent(skin).at(0, -h * 0.6f, d / 2f).rot(-35, 0, 0).mat("membrane")
                    .box(-0.5f, -5, 0, 1, 5, 3).add();
            parent = n;
            py = -h;
        }
        // head, levelled out at the top of the neck
        part(b, "head", parent, Role.HEAD, skin).accent(eye).at(0, -9, 0).rot(-34, 0, 0).mat("scales").pattern("eyes").glow("eyes")
                .box(-4.5f, -5, -7, 9, 6, 9).add();
        sym(b, "brow", "head", false, fin).at(3, -5, -6.5f).rot(0, 0, -12).mat("scales").box(-2, -1, -1, 3, 2, 3).add();
        part(b, "snout", "head", Role.STATIC, skin).accent(belly).at(0, -1.5f, -7).mat("scales").box(-3, -2.5f, -5, 6, 4, 5).add();
        part(b, "jaw", "head", Role.JAW, belly).at(0, 1, -3).rot(10, 0, 0).mat("plates").box(-3.5f, 0, -9, 7, 2, 9).add();
        sym(b, "fang", "snout", false, TOOTH).at(2, 1.5f, -4.4f).mat("bone").box(-0.5f, 0, -0.5f, 1, 2, 1).add();
        // the crown of finned spines fanning around the back of the head
        part(b, "crest", "head", Role.FIN, fin).accent(belly).at(0, -5, 1).rot(-25, 0, 0).mat("membrane").box(-1.5f, -11, -0.5f, 3, 11, 1).add();
        part(b, "tongue", "jaw", Role.STATIC, 0xC0283A).at(0, 0, -9).rot(15, 0, 0).mat("flesh").box(-0.5f, -0.5f, -3, 1, 1, 3)
                .box(-1.5f, -0.5f, -4, 1, 1, 1).box(0.5f, -0.5f, -4, 1, 1, 1).add();
        for (int k = 0; k < 3; k++) {
            sym(b, "frill" + k, "head", false, k % 2 == 0 ? fin : skin).roles(Role.FIN, Role.FIN).accent(belly)
                    .at(3, -4 + k * 1.5f, 1).rot(-22, 0, 26 + k * 28).mat("membrane").box(-1.5f, -10 + k * 2, -0.5f, 3, 10 - k * 2, 1).add();
        }
        // the tail loop arching out of the water behind the coils
        part(b, "tail0", "coil", Role.TAIL, skin).accent(belly).at(5, -2, 7).rot(70, 30, 0).mat("scales").box(-2.5f, -2.5f, 0, 5, 5, 8).add();
        part(b, "tail1", "tail0", Role.TAIL, skin).accent(belly).at(0, 0, 8).rot(-110, 0, 0).mat("scales").box(-2, -2, 0, 4, 4, 9).add();
        part(b, "tail2", "tail1", Role.TAIL, skin).accent(belly).at(0, 0, 8).rot(-30, 0, 0).mat("scales").box(-1.5f, -1.5f, 0, 3, 3, 6).add();
        part(b, "tail_fin", "tail2", Role.FIN, fin).at(0, -1.5f, 3).mat("membrane").box(-0.5f, -3, 0, 1, 3, 5).add();
        return b.build();
    }

    /**
     * Alchemical Hydra: the hulking, low-slung hydra of the Karuulm laboratory, a heavy scaled body over four
     * clawed legs, a bony back ridge and a thick tail, its chest sprouting one long neck per remaining head
     * (four, three, two, then the enraged last head). Each phase's chemical colour lights its eyes and stains its
     * hide.
     */
    static ModelSpec hydra(String id, int heads, int hide, int belly, int chem) {
        int horn = 0x3E3632;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.7f);
        part(b, "body", null, Role.BODY, hide).accent(belly).at(0, 9, 3).mat("scales")
                .box(-8, -6, -11, 16, 12, 21).box(-7, -5, -15, 14, 10, 4).add();
        part(b, "belly", "body", Role.STATIC, belly).at(0, 6, -2).mat("plates").box(-6, -0.5f, -12, 12, 1, 20).add();
        part(b, "stain", "body", Role.STATIC, hide).accent(chem).at(0, -2, -1).mat("scales").glow("spots")
                .box(-8.4f, -3, -8, 1, 6, 14).box(7.4f, -3, -8, 1, 6, 14).add();
        Kit.P ridge = part(b, "ridge", "body", Role.STATIC, horn).at(0, -6, 0).mat("horn");
        for (int i = 0; i < 6; i++) {
            ridge.box(-1, -3 - (i % 2 == 0 ? 1 : 0), -11 + i * 4, 2, 3 + (i % 2 == 0 ? 1 : 0), 2);
        }
        ridge.add();
        // necks fan out from the chest; outer heads lower, inner heads higher
        float spread = heads == 1 ? 0 : 4.5f;
        for (int i = 0; i < heads; i++) {
            float x = heads == 1 ? 0 : (i - (heads - 1) / 2f) * spread;
            float yaw = -x * 5;
            float lift = heads == 1 ? -32 : -36 + Math.abs(x) * 2.2f;
            int big = heads == 1 ? 1 : 0;
            String n = "n" + i;
            part(b, "neck_" + n, "body", Role.NECK, hide).accent(belly).at(x, -2, -14).rot(lift, yaw, 0).mat("scales")
                    .box(-2.5f - big, -2.5f - big, -12, 5 + 2 * big, 5 + 2 * big, 12).add();
            part(b, "neck2_" + n, "neck_" + n, Role.NECK, hide).accent(belly).at(0, 0, -12).rot(6, 0, 0).mat("scales")
                    .box(-2 - big, -2 - big, -9, 4 + 2 * big, 4 + 2 * big, 9).add();
            part(b, "head_" + n, "neck2_" + n, Role.HEAD, hide).accent(chem).at(0, 0, -9).rot(34, 0, 0).mat("scales")
                    .pattern("eyes").glow("eyes").box(-3.5f - big, -3 - big, -6, 7 + 2 * big, 6 + big, 6 + big).add();
            sym(b, "frill_" + n, "head_" + n, false, horn).accent(chem).at(3.5f + big, -1, -1).rot(0, -30, -20).mat("membrane")
                    .box(0, -3, -0.5f, 3, 5, 1).add();
            part(b, "snout_" + n, "head_" + n, Role.STATIC, hide).accent(belly).at(0, -0.5f, -5).mat("scales")
                    .box(-2, -1.5f, -5 - big, 4, 3, 5 + big).add();
            part(b, "jaw_" + n, "head_" + n, Role.JAW, belly).at(0, 1.5f, -2).rot(12, 0, 0).mat("plates")
                    .box(-2, 0, -8 - big, 4, 2, 8 + big).add();
            part(b, "crest_" + n, "head_" + n, Role.STATIC, horn).at(0, -3 - big, -1).rot(-35, 0, 0).mat("horn")
                    .box(1.5f, -1, 0, 1, 1, 7 + big).box(-2.5f, -1, 0, 1, 1, 7 + big).box(-0.5f, -2, -3, 1, 2, 5).add();
        }
        // legs: thick scaled limbs, horn-clawed feet
        sym(b, "fleg", null, false, hide).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(belly).at(8, 12, -8).mat("scales")
                .box(-3, -2, -3.5f, 6, 8, 7).add();
        sym(b, "fshin", "fleg", true, hide).at(0, 6, 0).mat("scales").box(-2.5f, 0, -2.5f, 5, 6, 5).add();
        sym(b, "ffoot", "fshin", true, horn).at(0, 6, 0).mat("horn").box(-3, 0, -4, 6, 1, 6).box(-3, 0, -6, 1, 1, 2)
                .box(-0.5f, 0, -6, 1, 1, 2).box(2, 0, -6, 1, 1, 2).add();
        sym(b, "bleg", null, false, hide).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(belly).at(7.5f, 11, 9).mat("scales")
                .box(-3.5f, -2, -4, 7, 9, 8).add();
        sym(b, "bshin", "bleg", true, hide).at(0, 7, 1).rot(15, 0, 0).mat("scales").box(-2.5f, 0, -2.5f, 5, 5, 5).add();
        sym(b, "bfoot", "bshin", true, horn).at(0, 5, 0).rot(-15, 0, 0).mat("horn").box(-3, 0, -4, 6, 1, 6).box(-3, 0, -6, 1, 1, 2)
                .box(2, 0, -6, 1, 1, 2).add();
        // tail
        String parent = "body";
        int[] w = {7, 5, 4, 2};
        for (int i = 0; i < 4; i++) {
            String n = "tail" + i;
            part(b, n, parent, Role.TAIL, hide).accent(belly).at(0, i == 0 ? 0 : 0, i == 0 ? 10 : 8).rot(i == 0 ? -12 : -6, 0, 0)
                    .mat("scales").box(-w[i] / 2f, -w[i] / 2f, 0, w[i], w[i], 8).add();
            parent = n;
        }
        part(b, "tail_spike", parent, Role.STATIC, horn).at(0, -1, 4).rot(-30, 0, 0).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 2).add();
        return b.build();
    }

    /**
     * King Black Dragon: the three-headed black dragon of the Wilderness lair, a massive dark scaled body with a
     * pale belly and a row of bony spines, three necks splayed forward with red-eyed horned heads, great leathery
     * wings and a spade-tipped tail.
     */
    static ModelSpec kingBlackDragon() {
        int hide = 0x28252C;
        int dark = 0x17151A;
        int belly = 0x4A4246;
        int web = 0x3A2A30;
        int horn = 0x9A9086;
        int eye = 0xFF3A20;
        ModelSpec.Builder b = ModelSpec.builder("king_black_dragon").shadow(2.0f);
        part(b, "body", null, Role.BODY, hide).accent(belly).at(0, 7, 4).mat("scales").box(-9, -6, -13, 18, 12, 25).add();
        part(b, "belly", "body", Role.STATIC, belly).at(0, 6, -1).mat("plates").box(-7, -0.5f, -11, 14, 1, 21).add();
        Kit.P spines = part(b, "spines", "body", Role.STATIC, horn).at(0, -6, 0).mat("horn");
        for (int i = 0; i < 6; i++) {
            spines.box(-1, -3 - (i < 3 ? 1 : 0), -11 + i * 4, 2, 3 + (i < 3 ? 1 : 0), 2);
        }
        spines.add();
        // three necks splayed forward from the shoulders
        float[][] hs = {{0, -3, 0, -42, 12}, {6, -1, -28, -30, 10}, {-6, -1, 28, -30, 10}};
        String[] names = {"mid", "left", "right"};
        for (int i = 0; i < 3; i++) {
            float[] h = hs[i];
            String n = names[i];
            int len = (int) h[4];
            part(b, "neck_" + n, "body", Role.NECK, hide).accent(belly).at(h[0], h[1], -12).rot(h[3], h[2], 0).mat("scales")
                    .box(-2.5f, -2.5f, -len, 5, 5, len).add();
            part(b, "head_" + n, "neck_" + n, Role.HEAD, hide).accent(eye).at(0, 0, -len).rot(-h[3] - 6, 0, 0).mat("scales")
                    .pattern("eyes").glow("eyes").box(-3.5f, -4, -7, 7, 7, 7).add();
            part(b, "snout_" + n, "head_" + n, Role.STATIC, dark).at(0, 0, -7).mat("scales").box(-3, -2, -6, 6, 4, 6)
                    .box(-1.5f, -2.5f, -4.5f, 1, 1, 1).box(0.5f, -2.5f, -4.5f, 1, 1, 1).add();
            part(b, "jaw_" + n, "head_" + n, Role.JAW, belly).at(0, 2, -2).rot(14, 0, 0).mat("plates")
                    .box(-3, 0, -11, 6, 2, 11).add();
            part(b, "horns_" + n, "head_" + n, Role.STATIC, horn).at(0, -4, -1).rot(-28, 0, 0).mat("horn")
                    .box(2, -1, 0, 1, 1, 8).box(-3, -1, 0, 1, 1, 8).add();
        }
        // great wings: arm, inner web, outer arm, outer web, finger bones
        sym(b, "wing", "body", false, dark).roles(Role.WING_LEFT, Role.WING_RIGHT).accent(web).at(8, -5, -6).rot(0, -12, -26)
                .mat("hide").box(0, -1, -1, 18, 2, 2).add();
        sym(b, "wing_web", "wing", true, web).accent(hide).at(0, 1, 0.5f).mat("membrane").box(0, 0, -0.5f, 18, 12, 1).add();
        sym(b, "wing_outer", "wing", true, dark).at(18, 0, 0).rot(0, 0, 32).mat("hide").box(0, -1, -1, 16, 2, 2).add();
        sym(b, "wing_web_outer", "wing_outer", true, web).accent(hide).at(0, 1, 0.5f).mat("membrane").box(0, 0, -0.5f, 14, 10, 1).add();
        for (int k = 0; k < 2; k++) {
            sym(b, "finger" + k, "wing_outer", true, horn).at(5 + k * 6, 1, 0).rot(0, 0, 72 - k * 16).mat("horn")
                    .box(0, -0.5f, -0.5f, 10 - k * 2, 1, 1).add();
        }
        // legs
        sym(b, "fleg", null, false, hide).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(belly).at(7.5f, 11, -6).mat("scales")
                .box(-3, -2, -3.5f, 6, 8, 7).add();
        sym(b, "fshin", "fleg", true, hide).at(0, 6, 0).mat("scales").box(-2, 0, -2, 4, 6, 4).add();
        sym(b, "ffoot", "fshin", true, dark).at(0, 6, 0).mat("scales").box(-3, 0, -4, 6, 1, 6).box(-3, 0, -6, 1, 1, 2)
                .box(-0.5f, 0, -6, 1, 1, 2).box(2, 0, -6, 1, 1, 2).add();
        sym(b, "bleg", null, false, hide).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(belly).at(7.5f, 10, 12).mat("scales")
                .box(-3.5f, -2, -4, 7, 9, 8).add();
        sym(b, "bshin", "bleg", true, hide).at(0, 7, 1).rot(15, 0, 0).mat("scales").box(-2, 0, -2, 4, 6, 4).add();
        sym(b, "bfoot", "bshin", true, dark).at(0, 6, 0).rot(-15, 0, 0).mat("scales").box(-3, 0, -4, 6, 1, 6).add();
        // tail with a bony spade
        String parent = "body";
        int[] w = {6, 5, 4, 3};
        for (int i = 0; i < 4; i++) {
            String n = "tail" + i;
            part(b, n, parent, Role.TAIL, i % 2 == 0 ? hide : dark).accent(belly).at(0, i == 0 ? -1 : 0, i == 0 ? 12 : 8)
                    .rot(i == 0 ? -8 : -4, 0, 0).mat("scales").box(-w[i] / 2f, -w[i] / 2f, 0, w[i], w[i], 8).add();
            parent = n;
        }
        part(b, "tail_spade", parent, Role.STATIC, horn).at(0, 0, 8).mat("horn").box(-2.5f, -0.5f, 0, 5, 1, 4).box(-1.5f, -0.5f, 4, 3, 1, 2).add();
        return b.build();
    }

    private static final int KQ_SHELL = 0xA8A04A;
    private static final int KQ_DARK = 0x5E6224;
    private static final int KQ_BELLY = 0xD8CC86;

    /**
     * Kalphite Queen, first form: the giant crawling queen of the Kalphite Lair, a sandy-gold segmented carapace
     * with dark seams, a swollen ridged abdomen, a broad armoured head with green compound eyes, hooked
     * mandibles and antennae, a pair of raised scythe forelimbs and six jointed legs.
     */
    static ModelSpec kalphiteQueen() {
        ModelSpec.Builder b = ModelSpec.builder("kalphite_queen").shadow(1.6f);
        part(b, "thorax", null, Role.BODY, KQ_SHELL).accent(KQ_BELLY).at(0, 12, -3).mat("chitin").box(-7, -5, -8, 14, 9, 14).add();
        part(b, "carapace", "thorax", Role.STATIC, KQ_DARK).at(0, -5, -1).mat("chitin").box(-6, -2, -7, 12, 2, 12)
                .box(-1, -4, -6, 2, 2, 10).add();
        part(b, "abdomen", "thorax", Role.STATIC, KQ_SHELL).accent(KQ_BELLY).at(0, -2, 6).rot(-14, 0, 0).mat("chitin")
                .box(-8, -6, 0, 16, 12, 18).add();
        part(b, "abdomen_bands", "abdomen", Role.STATIC, KQ_DARK).at(0, 0, 0).mat("chitin").box(-8.5f, -6.5f, 5, 17, 13, 1)
                .box(-8.5f, -6.5f, 11, 17, 13, 1).add();
        part(b, "abdomen_tip", "abdomen", Role.STATIC, KQ_DARK).at(0, 1, 18).rot(-20, 0, 0).mat("chitin").box(-5, -4, 0, 10, 8, 5)
                .box(-1, -1, 5, 2, 2, 3).add();
        part(b, "head", "thorax", Role.HEAD, KQ_SHELL).accent(0x9AE040).at(0, -1, -8).rot(10, 0, 0).mat("chitin").pattern("blank")
                .box(-5.5f, -4, -7, 11, 8, 7).add();
        part(b, "head_crest", "head", Role.STATIC, KQ_DARK).at(0, -4, -2).rot(-30, 0, 0).mat("chitin").box(-4, -2, -4, 8, 2, 7).add();
        sym(b, "eye", "head", false, 0x3A6A18).accent(0x9AE040).at(4.5f, -2, -6).rot(0, -30, 0).mat("chitin").glow("spots")
                .box(-1.5f, -1.5f, -1.5f, 3, 3, 2).add();
        sym(b, "mandible", "head", false, KQ_DARK).roles(Role.JAW, Role.STATIC).at(3, 3, -7).rot(0, 25, 0).mat("chitin")
                .box(-1, -1, -6, 2, 2, 6).add();
        sym(b, "mandible_hook", "mandible", true, 0x2E2A14).at(0, 0, -6).rot(0, 50, 0).mat("horn").box(-0.5f, -1, -3, 1, 2, 3).add();
        sym(b, "antenna", "head", false, KQ_DARK).at(2, -4, -6).rot(-50, -20, 0).mat("chitin").box(-0.5f, -0.5f, -10, 1, 1, 10).add();
        // raised scythe forelimbs
        sym(b, "scythe", "thorax", false, KQ_SHELL).roles(Role.ARM_LEFT, Role.ARM_RIGHT).at(6, -2, -7).rot(-60, -15, -20).mat("chitin")
                .box(-1.5f, -1, -1.5f, 3, 9, 3).add();
        sym(b, "scythe_blade", "scythe", true, KQ_DARK).at(0, 9, 0).rot(-80, 0, 0).mat("chitin").box(-1, 0, -1, 2, 9, 2)
                .box(-0.5f, 2, 1, 1, 6, 1).add();
        // six jointed legs
        for (int k = 0; k < 3; k++) {
            float yaw = 25 - k * 25;
            sym(b, "leg" + k, "thorax", false, k % 2 == 0 ? KQ_SHELL : KQ_DARK).roles(Role.LEG_EXTRA, Role.LEG_EXTRA)
                    .at(6.5f, 2, -5 + k * 5).rot(0, yaw, -30).mat("chitin").box(0, -1, -1, 9, 2, 2).add();
            sym(b, "leg" + k + "_low", "leg" + k, true, KQ_DARK).at(9, 0, 0).rot(0, 0, 75).mat("chitin").box(0, -1, -1, 11, 2, 2)
                    .box(10, -0.5f, -0.5f, 2, 1, 1).add();
        }
        return b.build();
    }

    /**
     * Kalphite Queen, second form: risen upright on her hind legs, the carapace split open to spread two pairs of
     * veined translucent wings, a taller crested head with glowing green eyes and mandibles, four raised
     * scythed forelimbs, and the ridged abdomen hanging between the legs.
     */
    static ModelSpec kalphiteQueenAirborne() {
        int wing = 0xC8D0A8;
        ModelSpec.Builder b = ModelSpec.builder("kalphite_queen_airborne").shadow(1.4f);
        part(b, "thorax", null, Role.BODY, KQ_SHELL).accent(KQ_BELLY).at(0, 4, 2).rot(-14, 0, 0).mat("chitin").box(-6, -12, -5, 12, 14, 10).add();
        part(b, "plastron", "thorax", Role.STATIC, KQ_BELLY).at(0, -5, -5).mat("plates").box(-4, -6, -0.6f, 8, 12, 1).add();
        part(b, "abdomen", "thorax", Role.STATIC, KQ_SHELL).accent(KQ_BELLY).at(0, 2, 2).rot(50, 0, 0).mat("chitin")
                .box(-6.5f, -5, 0, 13, 10, 14).box(-7, -5.5f, 4, 14, 11, 1).box(-7, -5.5f, 9, 14, 11, 1).add();
        part(b, "head", "thorax", Role.HEAD, KQ_SHELL).accent(0x9AE040).at(0, -12, -2).rot(14, 0, 0).mat("chitin").pattern("blank")
                .box(-4.5f, -6, -6, 9, 7, 7).add();
        part(b, "crest", "head", Role.STATIC, KQ_DARK).at(0, -6, 0).rot(-35, 0, 0).mat("chitin").box(-3, -6, -1, 6, 6, 2)
                .box(-1, -9, 0, 2, 3, 1).add();
        sym(b, "eye", "head", false, 0x3A6A18).accent(0xA8FF50).at(3.8f, -3, -5).rot(0, -30, 0).mat("chitin").glow("spots")
                .box(-1.5f, -1.5f, -1.5f, 3, 3, 2).add();
        sym(b, "mandible", "head", false, KQ_DARK).roles(Role.JAW, Role.STATIC).at(2.5f, 0, -6).rot(20, 25, 0).mat("chitin")
                .box(-1, -1, -5, 2, 2, 5).box(-1, -1, -6, 1, 2, 1).add();
        // two pairs of veined wings
        sym(b, "wing", "thorax", false, wing).roles(Role.WING_LEFT, Role.WING_RIGHT).accent(KQ_DARK).at(3, -10, 5).rot(-20, -30, -40)
                .mat("membrane").box(0, -2, -0.5f, 18, 6, 1).add();
        sym(b, "wing_hind", "wing", true, wing).accent(KQ_DARK).at(1, 4, 0.5f).rot(0, 0, 25).mat("membrane").box(0, -2, -0.5f, 15, 5, 1).add();
        // four raised scythed forelimbs
        for (int k = 0; k < 2; k++) {
            String n = "scythe" + k;
            sym(b, n, "thorax", false, KQ_SHELL).roles(k == 0 ? Role.ARM_LEFT : Role.STATIC, k == 0 ? Role.ARM_RIGHT : Role.STATIC)
                    .at(6, -10 + k * 5, -2).rot(-100 + k * 30, 0, -30 - k * 10).mat("chitin").box(-1.5f, -1, -1.5f, 3, 9 - k, 3).add();
            sym(b, n + "_blade", n, true, KQ_DARK).at(0, 9 - k, 0).rot(-70, 0, 0).mat("chitin").box(-1, 0, -1, 2, 9 - k, 2)
                    .box(-0.5f, 2, 1, 1, 5, 1).add();
        }
        // the legs she stands on
        for (int k = 0; k < 3; k++) {
            sym(b, "leg" + k, "thorax", false, k % 2 == 0 ? KQ_SHELL : KQ_DARK).roles(Role.LEG_EXTRA, Role.LEG_EXTRA)
                    .at(5, 1, -2 + k * 3).rot(0, -20 + k * 25, 20).mat("chitin").box(0, -1, -1, 8, 2, 2).add();
            sym(b, "leg" + k + "_low", "leg" + k, true, KQ_DARK).at(8, 0, 0).rot(0, 0, 65).mat("chitin").box(0, -1, -1, 14, 2, 2)
                    .box(13, -0.5f, -0.5f, 2, 1, 1).add();
        }
        return b.build();
    }

    /**
     * Grotesque Guardians: Dusk and Dawn, the living stone gargoyles atop the Slayer Tower. Carved granite bodies
     * hunched forward, horned heads with heavy brows and glowing eyes, long arms ending in stone claws, raised
     * bat-like stone wings and a pointed tail. Dusk is the bulkier dark-grey brute; Dawn is pale, leaner and
     * crested.
     */
    static ModelSpec gargoyle(String id, int stone, int dark, int eye, boolean dawn) {
        int f = dawn ? 1 : 0;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.5f);
        part(b, "hips", null, Role.BODY, stone).at(0, 9, 2).mat("granite").box(-6 + f, -3, -4, 12 - 2 * f, 6, 8).add();
        part(b, "torso", "hips", Role.STATIC, stone).accent(dark).at(0, -3, 0).rot(dawn ? 14 : 30, 0, 0).mat("granite")
                .box(-9 + 2 * f, -13, -6 + f, 18 - 4 * f, 13, 12 - 2 * f).add();
        part(b, "chest", "torso", Role.STATIC, dark).at(0, -8, -6 + f).mat("granite").box(-6 + f, -4, -0.6f, 12 - 2 * f, 9, 1).add();
        part(b, "head", "torso", Role.HEAD, stone).accent(eye).at(0, -13, -4).rot(dawn ? -10 : -26, 0, 0).mat("granite")
                .pattern("eyes").glow("eyes").box(-4, -7, -6, 8, 7, 7).add();
        part(b, "brow", "head", Role.STATIC, dark).at(0, -5, -6).rot(16, 0, 0).mat("granite").box(-4.5f, -1, -1.5f, 9, 2, 3).add();
        part(b, "jaw", "head", Role.JAW, dark).at(0, 0, -2).rot(8, 0, 0).mat("granite").box(-3.5f, 0, -5, 7, 3, 6).add();
        sym(b, "tusk", "jaw", false, BONE).at(2.5f, 0, -4.6f).mat("bone").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        sym(b, "horn", "head", false, dark).at(3, -7, -2).rot(-35, 0, dawn ? -5 : -30).mat("granite").box(-1, -5, -1, 2, 5, 2).add();
        sym(b, "horn_tip", "horn", true, dark).at(0, -5, 0).rot(-40, 0, 0).mat("granite").box(-0.5f, -3 - 2 * f, -0.5f, 1, 3 + 2 * f, 1).add();
        sym(b, "ear", "head", false, stone).at(4, -4, -1).rot(0, -25, -35).mat("granite").box(0, -1, -1, 4, 2, 2).add();
        if (dawn) {
            part(b, "crest", "head", Role.STATIC, dark).at(0, -7, 0).rot(-20, 0, 0).mat("granite").box(-1, -3, -4, 2, 3, 8).add();
        }
        // stone wings, raised high behind
        sym(b, "wing", "torso", false, dark).roles(Role.WING_LEFT, Role.WING_RIGHT).accent(stone).at(5 - f, -11, 5).rot(-10, -20, -55)
                .mat("granite").box(0, -1.5f, -1.5f, 14, 3, 3).add();
        sym(b, "wing_web", "wing", true, stone).accent(dark).at(0, 1, 0).mat("granite").box(0, 0, -0.5f, 14, 9, 1).add();
        sym(b, "wing_outer", "wing", true, dark).at(14, 0, 0).rot(0, 0, 50).mat("granite").box(0, -1, -1, 11, 2, 2).add();
        sym(b, "wing_web_outer", "wing_outer", true, stone).accent(dark).at(0, 1, 0).mat("granite").box(0, 0, -0.5f, 10, 8, 1).add();
        sym(b, "wing_spike", "wing_outer", true, dark).at(11, 0, 0).rot(0, 0, -25).mat("granite").box(0, -0.5f, -0.5f, 4, 1, 1).add();
        // long arms, knuckles low
        int g = 1 - f;
        sym(b, "arm", "torso", false, stone).roles(Role.ARM_LEFT, Role.ARM_RIGHT).at(10 - 2 * f, -11, -1).rot(-20, 0, -10).mat("granite")
                .box(-2.5f - g, -1, -2.5f - g, 5 + 2 * g, 10, 5 + 2 * g).add();
        sym(b, "forearm", "arm", true, stone).at(0, 9, 0).rot(-20, 0, 6).mat("granite").box(-2.5f, 0, -2.5f, 5, 8, 5).add();
        sym(b, "claw", "forearm", true, dark).at(0, 8, 0).mat("granite").box(-2.5f, 0, -2.5f, 5, 2, 5).box(-2.5f, 2, -2.5f, 1, 2, 1)
                .box(-0.5f, 2, -2.5f, 1, 2, 1).box(1.5f, 2, -2.5f, 1, 2, 1).add();
        // crouched digitigrade legs
        sym(b, "leg", "hips", false, stone).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).at(4, 2, 0).rot(-30, 0, 0).mat("granite")
                .box(-3, -1, -3, 6, 8, 6).add();
        sym(b, "shin", "leg", true, stone).at(0, 7, 0).rot(55, 0, 0).mat("granite").box(-2, 0, -2, 4, 6, 4).add();
        sym(b, "foot", "shin", true, dark).at(0, 6, 0).rot(-25, 0, 0).mat("granite").box(-2.5f, 0, -5, 5, 2, 6).box(-2.5f, 0.5f, -6, 1, 1, 1)
                .box(1.5f, 0.5f, -6, 1, 1, 1).add();
        part(b, "tail", "hips", Role.TAIL, stone).at(0, 0, 4).rot(-40, 0, 0).mat("granite").box(-1.5f, -1.5f, 0, 3, 3, 9).add();
        part(b, "tail_tip", "tail", Role.TAIL, dark).at(0, 0, 9).rot(30, 0, 0).mat("granite").box(-1, -1, 0, 2, 2, 5)
                .box(-2, -0.5f, 5, 4, 1, 2).add();
        return b.build();
    }

    /**
     * Sarachnis: the giant spider matriarch of the Forthos Dungeon, a deep crimson body with a lighter blotched
     * cephalothorax crowned by a cluster of tall cream bone spikes, a bulbous red abdomen, glowing pale eyes,
     * curved fangs and eight long jointed legs.
     */
    static ModelSpec sarachnis() {
        int red = 0x8E2A22;
        int dark = 0x4E1612;
        int light = 0xC05A40;
        int spike = 0xE8D8B0;
        ModelSpec.Builder b = ModelSpec.builder("sarachnis").shadow(1.7f);
        part(b, "body", null, Role.BODY, red).accent(light).at(0, 13, -3).mat("chitin").pattern("spots").box(-7, -5, -8, 14, 8, 13).add();
        part(b, "abdomen", "body", Role.STATIC, red).accent(light).at(0, -2, 5).rot(-14, 0, 0).mat("chitin").pattern("stripes")
                .box(-8, -7, 0, 16, 12, 15).add();
        part(b, "abdomen_tip", "abdomen", Role.STATIC, dark).at(0, 0, 15).mat("chitin").box(-4, -4, 0, 8, 8, 3).add();
        Kit.P crown = part(b, "crown", "body", Role.STATIC, spike).at(0, -5, -2).mat("bone");
        int[][] sp = {{-1, -10, -1, 2, 10, 2}, {3, -8, -3, 2, 8, 2}, {-5, -8, -3, 2, 8, 2}, {3, -7, 2, 2, 7, 2}, {-5, -7, 2, 2, 7, 2},
                {-1, -6, 3, 2, 6, 2}, {5, -5, -1, 1, 5, 1}, {-6, -5, -1, 1, 5, 1}};
        for (int[] s : sp) {
            crown.box(s[0], s[1], s[2], s[3], s[4], s[5]);
        }
        crown.add();
        part(b, "head", "body", Role.HEAD, dark).accent(0xF0E8FF).at(0, 0, -8).mat("chitin").pattern("blank")
                .box(-5, -4, -5, 10, 7, 5).add();
        part(b, "eyes", "head", Role.STATIC, 0xF4ECFF).accent(0xF4ECFF).at(0, -2, -5.2f).mat("energy").glow("full")
                .box(-3, -1, -0.5f, 2, 2, 1).box(1, -1, -0.5f, 2, 2, 1).box(-4.5f, -2.5f, -0.3f, 1, 1, 1).box(3.5f, -2.5f, -0.3f, 1, 1, 1)
                .box(-1.5f, -3, -0.3f, 1, 1, 1).box(0.5f, -3, -0.3f, 1, 1, 1).add();
        sym(b, "fang", "head", false, spike).roles(Role.JAW, Role.STATIC).at(2, 2, -4.5f).rot(20, 0, 10).mat("bone")
                .box(-1, 0, -1, 2, 4, 2).box(-0.5f, 4, -1, 1, 2, 1).add();
        sym(b, "palp", "head", false, red).at(4, 1, -4).rot(30, -20, 0).mat("chitin").box(-1, 0, -1, 2, 5, 2).add();
        for (int k = 0; k < 4; k++) {
            float yaw = 40 - k * 27;
            sym(b, "leg" + k, "body", false, k % 2 == 0 ? red : light).roles(Role.LEG_EXTRA, Role.LEG_EXTRA).accent(dark)
                    .at(6.5f, -2, -6 + k * 3.5f).rot(0, yaw, -40).mat("chitin").box(0, -1.5f, -1.5f, 11, 3, 3).add();
            sym(b, "leg" + k + "_low", "leg" + k, true, dark).at(11, 0, 0).rot(0, 0, 85).mat("chitin").box(0, -1, -1, 14, 2, 2)
                    .box(13, -0.5f, -0.5f, 2, 1, 1).add();
        }
        return b.build();
    }

    /**
     * Abyssal Sire: the colossal abyssal demon lord in the Abyssal Nexus, a swollen hunched mass of crimson flesh
     * lit by glowing vein-work, a small horned head sunk between the shoulders over a gaping fanged maw, clusters
     * of glowing eyes, two massive clawed arms and a ring of writhing tentacles anchoring it to the floor.
     */
    static ModelSpec abyssalSire() {
        int flesh = 0x8A1E2E;
        int deep = 0x4E0E1C;
        int pale = 0xC85A62;
        int vein = 0xD8405A;
        int horn = 0x2A1A1E;
        ModelSpec.Builder b = ModelSpec.builder("abyssal_sire").shadow(2.1f);
        part(b, "base", null, Role.BODY, deep).accent(vein).at(0, 18, 2).mat("flesh").box(-12, -6, -11, 24, 8, 22).add();
        part(b, "mass", "base", Role.STATIC, flesh).accent(vein).at(0, -6, 0).rot(10, 0, 0).mat("flesh").glow("veins")
                .box(-11, -14, -9, 22, 14, 18).add();
        part(b, "hump", "mass", Role.STATIC, flesh).accent(0xFFE07A).at(0, -14, 2).rot(-10, 0, 0).mat("flesh").glow("spots")
                .box(-9, -7, -7, 18, 7, 14).add();
        Kit.P spikes = part(b, "spikes", "hump", Role.STATIC, horn).at(0, -7, 0).mat("horn");
        for (int i = 0; i < 4; i++) {
            spikes.box(-6 + i * 4, -4 - (i % 2) * 2, -1 + (i % 2) * 2, 2, 4 + (i % 2) * 2, 2);
        }
        spikes.add();
        part(b, "belly", "mass", Role.STATIC, pale).at(0, -6, -9).mat("flesh").box(-7, -6, -0.6f, 14, 11, 1).add();
        part(b, "head", "mass", Role.HEAD, flesh).accent(0xFFE07A).at(0, -13, -8).rot(-6, 0, 0).mat("flesh").pattern("eyes").glow("eyes")
                .box(-5, -6, -6, 10, 7, 7).add();
        sym(b, "horn", "head", false, horn).at(4, -6, -2).rot(-30, 0, 40).mat("horn").box(-1, -6, -1, 2, 6, 2).add();
        sym(b, "horn_tip", "horn", true, 0x5A4448).at(0, -6, 0).rot(-40, 0, -25).mat("horn").box(-0.5f, -4, -0.5f, 1, 4, 1).add();
        part(b, "jaw", "head", Role.JAW, deep).at(0, 1, -3).rot(14, 0, 0).mat("flesh").box(-5, 0, -5, 10, 4, 7).add();
        Kit.P teeth = part(b, "teeth", "jaw", Role.STATIC, TOOTH).at(0, 0, -4.5f).mat("bone");
        for (int i = 0; i < 4; i++) {
            teeth.box(-4 + i * 2.5f, -2, -0.5f, 1, 2, 1);
        }
        teeth.add();
        // massive arms
        sym(b, "arm", "mass", false, flesh).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(vein).at(12, -11, -2).rot(-25, 0, -14).mat("flesh")
                .glow("veins").box(-4, -2, -4, 8, 12, 8).add();
        sym(b, "forearm", "arm", true, deep).at(0, 10, 0).rot(-30, 0, 10).mat("flesh").box(-3.5f, 0, -3.5f, 7, 9, 7).add();
        sym(b, "claws", "forearm", true, horn).at(0, 9, 0).mat("horn").box(-3, 0, -3, 2, 4, 2).box(-1, 0, -3.5f, 2, 5, 2)
                .box(1, 0, -3, 2, 4, 2).add();
        // ring of tentacles anchoring it
        for (int i = 0; i < 6; i++) {
            float yaw = 30 + i * 60;
            double r = Math.toRadians(yaw);
            float px = (float) (-Math.sin(r) * 10);
            float pz = (float) (-Math.cos(r) * 9);
            part(b, "tentacle" + i, "base", Role.TENTACLE, i % 2 == 0 ? flesh : deep).accent(vein).at(px, 0, pz).rot(15, yaw, 0)
                    .mat("flesh").box(-2, -2, -9, 4, 4, 9).add();
            part(b, "tentacle" + i + "_tip", "tentacle" + i, Role.TENTACLE, pale).at(0, 0, -9).rot(-20, 0, 0).mat("flesh")
                    .box(-1, -1, -7, 2, 2, 7).add();
        }
        return b.build();
    }
}
