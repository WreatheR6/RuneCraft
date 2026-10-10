package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.Humanoid.*;
import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/** Bosses: the Barrows brothers and other built-in bosses (new models), plus re-sculpts of famous ones. */
final class Bosses {
    private Bosses() {
    }

    static final int UNDEAD = 0x8A9878;

    /** New models for bosses that had none (Boss enum creatures). */
    static List<ModelSpec> fresh() {
        List<ModelSpec> l = new ArrayList<>();
        // ---- Barrows brothers: undead in their signature sets, eyes burning ----
        l.add(of("ahrim").skin(UNDEAD).face("skull").glowingEyes().hair(0, "none").top(0x34343E, "robe")
                .bottoms(0x2A2A34, "robe").trim(0x5A5A6A).helm("hood", 0x2C2C36).weapon("staff", 0x6A6A9A).build());
        l.add(of("dharok").body(12, 9, 11, 5, 12, 3).skin(UNDEAD).face("skull").glowingEyes().hair(0, "none")
                .top(0x5A5652, "metal").bottoms(0x4E4A46, "plate").helm("full", 0x4A4642).weapon("greataxe", 0x6A6662)
                .weaponPitch(30).build());
        l.add(of("guthan").skin(UNDEAD).face("skull").glowingEyes().hair(0, "none").top(0x5E604E, "metal")
                .bottoms(0x4E5040, "plateskirt").helm("med", 0x5A5C4C).weapon("warspear", 0x6A6C5A).build());
        l.add(of("karil").skin(UNDEAD).face("skull").glowingEyes().hair(0, "none").top(0x4A3A26, "leather")
                .bottoms(0x3E3020, "skirt").helm("coif", 0x4A3A26).weapon("crossbow", 0x5A5A50).cape(0x2A3A20).build());
        l.add(of("torag").body(11, 9, 10, 5, 11, 3).skin(UNDEAD).face("skull").glowingEyes().hair(0, "none")
                .top(0x564A3C, "metal").bottoms(0x4A4034, "plate").helm("full", 0x524636).weapon("hammers", 0x605446).build());
        l.add(of("verac").skin(UNDEAD).face("skull").glowingEyes().hair(0, "none").top(0x625644, "metal")
                .bottoms(0x564C3C, "plateskirt").helm("med", 0x5E5240).weapon("flail", 0x6A5E4A).build());
        // ---- other built-in bosses ----
        l.add(obor());
        l.add(bryophyta());
        l.add(of("count_draynor").skin(0xD8D0C8).face("eyes").glowingEyes().hair(HAIR_BLACK, "short").top(0x1A1A24, "cloth")
                .bottoms(0x1A1A24, "trousers").cape(0x8A1A1A).trim(0xA02020).build());
        l.add(Creatures.demon("delrith", 0x7A2A5A, 0x3A1028, 0xE0D0B0, 1.0f, true));
        return l;
    }

    /** Re-sculpts of existing boss models (same ids). */
    static List<ModelSpec> overrides() {
        List<ModelSpec> l = new ArrayList<>();
        ModelSpec.Builder kbd = Wyrms.builder("king_black_dragon", 0x222024, 0x4A3A3A, 0x161416, 0x8A7A6A, Wyrms.BRUTAL, "scales", 3);
        l.add(kbd.shadow(1.6f).build());
        l.add(Wyrms.builder("vorkath", 0xC8C4B0, 0x6A5E50, 0x4A4A40, 0x3A3A34, Wyrms.CHROMATIC, "bone", 1).shadow(1.6f).build());
        l.add(zulrah("zulrah", 0x3A8A3A, 0x9AC060));
        l.add(zulrah("zulrah_magma", 0xC03A1A, 0xF0A040));
        l.add(zulrah("zulrah_tanzanite", 0x3A4ABA, 0x40C0D0));
        l.add(corp());
        l.add(graardor());
        l.add(kril());
        l.add(zilyana());
        l.add(kreearra());
        return l;
    }

    private static ModelSpec obor() {
        ModelSpec.Builder b = of("obor").giantBody().skin(0xC0946A).hair(HAIR_BROWN, "none").top(0xC0946A, "skin").sleeves("none")
                .bottoms(0x5A3A1E, "loincloth").boots(0xC0946A).weapon("club", WOOD).builder();
        part(b, "headdress", "head", Role.STATIC, 0xE0D8C0).at(0, -8, 0).mat("bone").box(-4.5f, -1, -4.5f, 9, 2, 9).add();
        for (int i = -1; i <= 1; i++) {
            part(b, "plume" + (i + 1), "headdress", Role.STATIC, i == 0 ? 0xA02020 : 0xE0D8C0).at(i * 2.5f, -1, -3).rot(-10, 0, i * 15)
                    .mat("feathers").box(-0.5f, -4, -0.5f, 1, 4, 1).add();
        }
        part(b, "necklace", "torso", Role.STATIC, 0xE0D8C0).at(0, -11, -3).mat("bone").box(-3, 0, -1, 6, 1, 1).box(-0.5f, 1, -1, 1, 2, 1).add();
        return b.build();
    }

    private static ModelSpec bryophyta() {
        ModelSpec.Builder b = of("bryophyta").giantBody().femaleBody().body(12, 10, 11, 6, 13, 4).skin(0x5A7A3B).skinMaterial("moss")
                .hair(0x3A6A1E, "long").top(0x4A6A2A, "robe").bottoms(0x3A5A1A, "robe").weapon("staff", 0x80E040).weaponPitch(0).builder();
        part(b, "moss_crown", "head", Role.STATIC, 0x2A5A16).at(0, -8, 0).mat("moss").box(-4.5f, -2, -4.5f, 9, 2, 9).add();
        return b.build();
    }

    /** Zulrah: a great serpent rearing from the water: coils, a tall segmented neck, a hooded, horned head. */
    static ModelSpec zulrah(String id, int skin, int belly) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.4f);
        int dark = darker(skin);
        part(b, "coil", null, Role.BODY, skin).accent(belly).at(0, 22, 2).mat("scales").box(-8, -2, -8, 16, 4, 16).add();
        part(b, "coil_top", "coil", Role.STATIC, skin).accent(belly).at(0, -2, 0).mat("scales").box(-6, -2, -6, 12, 2, 12).add();
        String prev = "coil";
        int[] w = {7, 6, 6, 5, 5};
        for (int i = 0; i < w.length; i++) {
            part(b, "neck" + i, prev, i < 3 ? Role.NECK : Role.STATIC, skin).accent(belly).at(0, i == 0 ? -3 : -5, i == 0 ? -2 : 0)
                    .rot(i == 0 ? -10 : i < 3 ? 4 : -4, 0, 0).mat("scales").box(-w[i] / 2f, -5, -w[i] / 2f, w[i], 5, w[i]).add();
            part(b, "neck_belly" + i, "neck" + i, Role.STATIC, belly).at(0, -2.5f, -w[i] / 2f).mat("plates")
                    .box(-w[i] / 2f + 1, -2.5f, -0.75f, w[i] - 2, 5, 1).add();
            prev = "neck" + i;
        }
        part(b, "head", prev, Role.HEAD, skin).accent(0xFFE040).at(0, -5, 0).rot(30, 0, 0).mat("scales").pattern("eyes").glow("eyes")
                .box(-3.5f, -4, -6, 7, 5, 7).add();
        part(b, "snout", "head", Role.STATIC, skin).at(0, -1, -6).mat("scales").box(-2.5f, -2, -3, 5, 3, 3).add();
        part(b, "jaw", "head", Role.JAW, belly).at(0, 1, -5).mat("plates").box(-3, 0, -4, 6, 2, 5).add();
        sym(b, "hood", "head", false, dark).at(3.5f, -2, -1).rot(0, -20, -15).mat("membrane").box(0, -4, -2, 4, 8, 1).add();
        sym(b, "fin", "head", false, belly).at(2, -4, 0).rot(-50, 0, -15).mat("horn").box(-0.5f, -5, -1, 1, 5, 3).add();
        part(b, "crest", "head", Role.STATIC, belly).at(0, -4, 0).rot(-40, 0, 0).mat("horn").box(-0.5f, -6, -1, 1, 6, 4).add();
        String tprev = "coil";
        for (int i = 0; i < 4; i++) {
            part(b, "tail" + i, tprev, Role.TAIL, skin).accent(belly).at(i == 0 ? 6 : 0, i == 0 ? 0 : 0, i == 0 ? 7 : 4).rot(0, i == 0 ? 40 : 25, 0)
                    .mat("scales").box(-1.5f + i * 0.25f, -1.5f + i * 0.25f, 0, 3 - i / 2, 3 - i / 2, 4).add();
            tprev = "tail" + i;
        }
        return b.build();
    }

    /** Corporeal Beast: a hulking, spectral green beast on four columns with a spiked crest and burning eyes. */
    static ModelSpec corp() {
        int green = 0x8AC8A0;
        int dark = 0x3A6A4A;
        ModelSpec.Builder b = ModelSpec.builder("corporeal_beast").shadow(1.8f);
        part(b, "body", null, Role.BODY, green).accent(0xE0FFF0).at(0, 10, 2).rot(-10, 0, 0).mat("ghost").glow("cracks")
                .box(-8, -8, -9, 16, 14, 18).add();
        part(b, "belly", "body", Role.STATIC, dark).at(0, 6, 0).mat("ghost").box(-6, -1, -7, 12, 2, 14).add();
        for (int i = 0; i < 5; i++) {
            part(b, "spike" + i, "body", Role.STATIC, 0xE0F0E8).at(0, -8, -7 + i * 3.5f).rot(-30, 0, 0).mat("horn")
                    .box(-1, -4 + (i == 2 ? -1 : 0), -1, 2, 4 + (i == 2 ? 1 : 0), 2).add();
        }
        sym(b, "shoulder", "body", false, green).accent(0xE0FFF0).at(8, -5, -6).mat("ghost").box(-2, -3, -3, 4, 7, 6).add();
        part(b, "head", "body", Role.HEAD, green).accent(0xFF3020).at(0, -3, -9).rot(10, 0, 0).mat("ghost").pattern("eyes").glow("eyes")
                .box(-5, -5, -7, 10, 9, 7).add();
        part(b, "jaw", "head", Role.JAW, dark).at(0, 3, -5).mat("ghost").box(-4.5f, 0, -3, 9, 3, 5).add();
        sym(b, "horn", "head", false, 0xE0F0E8).at(4, -5, -3).rot(-20, 0, -35).mat("horn").box(-1, -5, -1, 2, 5, 2).add();
        sym(b, "horn_tip", "horn", true, 0xC0E0D0).at(0, -5, 0).rot(-20, 0, 30).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        sym(b, "leg_f", null, false, green).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).at(7, 12, -6).mat("ghost")
                .box(-3, -1, -3, 6, 12, 6).add();
        sym(b, "paw_f", "leg_f", true, dark).at(0, 10, 0).mat("ghost").box(-3.5f, 0, -4.5f, 7, 2, 8).add();
        sym(b, "leg_b", null, false, green).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).at(6, 12, 8).mat("ghost")
                .box(-3, -1, -3, 6, 12, 6).add();
        sym(b, "paw_b", "leg_b", true, dark).at(0, 10, 0).mat("ghost").box(-3.5f, 0, -4.5f, 7, 2, 8).add();
        part(b, "tail", "body", Role.TAIL, green).at(0, -3, 9).rot(-20, 0, 0).mat("ghost").box(-2, -2, 0, 4, 4, 8).add();
        return b.build();
    }

    /** General Graardor: a towering ork-like brute in Bandos plate with a spiked mace. */
    static ModelSpec graardor() {
        ModelSpec.Builder b = of("general_graardor").ogreBody().skin(0x6A7A3A).face("eyes").glowingEyes().hair(0, "none")
                .top(0x5E5232, "metal").trim(GOLD).bottoms(0x4A4028, "plateskirt").pauldrons(0x5E5232).boots(0x3A3020)
                .weapon("mace", 0x6A6A6A).weaponPitch(20).builder();
        part(b, "helm_spike", "head", Role.STATIC, 0x5E5232).at(0, -7, 0).mat("metal").box(-4, -1, -4, 8, 2, 8, 0.3f).add();
        sym(b, "pauldron_spike", "pauldron", true, 0xD0C8A0).at(1, -2, 0).rot(0, 0, -30).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        return b.build();
    }

    /** K'ril Tsutsaroth: a towering red demon of Zamorak with great black wings and a fiery spear. */
    static ModelSpec kril() {
        ModelSpec.Builder b = Creatures.demonBuilder("kril_tsutsaroth", 0xA82020, 0x2A0A0A, 0x2A2A2A, 1.0f, true);
        part(b, "spear", "hand_r", Role.STATIC, 0x2A2A2A).at(0, 1, 0).mat("metal").box(-0.5f, -0.5f, -14, 1, 1, 20).add();
        part(b, "spear_head", "spear", Role.STATIC, 0xE04010).at(0, 0, -14).mat("lava").glow("full").box(-1, -1.5f, -4, 2, 3, 4).add();
        return b.build();
    }

    /** Commander Zilyana: Saradomin's icyene general: white and blue robes, great white wings, Saradomin sword. */
    static ModelSpec zilyana() {
        ModelSpec.Builder b = of("commander_zilyana").femaleBody().skin(SKIN_PALE).hair(HAIR_BLOND, "long").top(0xE8E8F0, "metal")
                .trim(0x3A5AD0).bottoms(0xE0E0E8, "robe").weapon("longsword", 0xE8E8F0).builder();
        featherWings(b, "torso", 0xF4F4F8, 0xC8D0E0, 3, -9, 2.5f, 12);
        return b.build();
    }

    /** Kree'arra: Armadyl's avian general: brown-feathered bird-man with a hooked beak, gold armour and huge wings. */
    static ModelSpec kreearra() {
        ModelSpec.Builder b = of("kreearra").body(12, 10, 12, 6, 12, 3).skin(0x8A6A3A).skinMaterial("feathers").face("eyes")
                .glowingEyes().hair(0, "none").top(0x8A6A3A, "skin").sleeves("none").bottoms(0x6A4A2A, "kilt")
                .boots(0xE0B030).trim(GOLD).builder();
        part(b, "beak", "head", Role.STATIC, 0xE0B030).at(0, -3.5f, -4).rot(20, 0, 0).mat("horn").box(-1.5f, -1, -3, 3, 2, 3).add();
        part(b, "beak_hook", "beak", Role.STATIC, 0xC09020).at(0, 0, -3).rot(50, 0, 0).mat("horn").box(-1, -1, -1, 2, 2, 2).add();
        part(b, "crest", "head", Role.STATIC, 0xE0D0B0).at(0, -8, 1).rot(-40, 0, 0).mat("feathers").box(-1.5f, -4, -1, 3, 4, 4).add();
        part(b, "chest_armour", "torso", Role.STATIC, GOLD).at(0, -11, -3).mat("metal").box(-4, 0, -0.75f, 8, 5, 1).add();
        featherWings(b, "torso", 0x7A5A2A, 0xE0D0B0, 4, -10, 3, 14);
        return b.build();
    }

    /** Big feathered wings (one WING_LEFT/WING_RIGHT pair) with layered primaries. */
    static void featherWings(ModelSpec.Builder b, String parent, int col, int tip, float x, float y, float z, int span) {
        sym(b, "wing", parent, false, col).roles(Role.WING_LEFT, Role.WING_RIGHT).at(x, y, z).rot(-10, -25, -25).mat("feathers")
                .box(0, -1.5f, -1, span, 3, 2).add();
        sym(b, "wing_cover", "wing", true, col).at(0, 1.5f, 0).mat("feathers").box(0, 0, -0.5f, span, 7, 1).add();
        sym(b, "wing_primary", "wing", true, tip).at(0, 8, 0).mat("feathers").box(1, 0, -0.5f, span - 1, 6, 1).add();
        sym(b, "wing_tip", "wing", true, tip).at(span, 0, 0).rot(0, 0, 30).mat("feathers").box(0, -1, -0.5f, 5, 10, 1).add();
    }
}
