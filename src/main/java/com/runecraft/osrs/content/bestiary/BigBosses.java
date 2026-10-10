package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/** Re-sculpts of the wilderness, slayer and solo bosses (same ids as their original models). */
final class BigBosses {
    private BigBosses() {
    }

    static List<ModelSpec> overrides() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(vetion("vetion", 0x5A2A8A));
        l.add(vetion("calvarion", 0x3A6A4A));
        l.add(nex());
        return l;
    }

    /** Vet'ion: a skeletal champion in ornate purple-and-gold armour wielding a great halberd. */
    static ModelSpec vetion(String id, int armour) {
        ModelSpec.Builder b = Creatures.skeletonBuilder(id, 0xE0DCC8, "none", true);
        part(b, "breastplate", "spine", Role.STATIC, armour).at(0, -10, -1).mat("metal").box(-4.5f, 0, -3, 9, 7, 5).add();
        part(b, "breast_trim", "breastplate", Role.STATIC, Humanoid.GOLD).at(0, 0, -3).mat("metal").box(-1, 0, -0.6f, 2, 7, 1).add();
        sym(b, "pauldron", "arm", true, armour).at(0, 0, 0).rot(0, 0, -15).mat("metal").box(-1.5f, -1.5f, -2, 4, 3, 4).add();
        sym(b, "pauldron_spike", "pauldron", true, Humanoid.GOLD).at(1, -1.5f, 0).rot(0, 0, -30).mat("metal").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        part(b, "tassets", "pelvis", Role.STATIC, armour).at(0, 0, 0).mat("metal").box(-3.5f, 0, -2, 7, 4, 4).add();
        part(b, "helm", "skull", Role.STATIC, armour).at(0, -6, 0).mat("metal").box(-3.5f, -1, -4, 7, 3, 7, 0.2f).add();
        part(b, "crest", "helm", Role.STATIC, Humanoid.GOLD).at(0, -1, 0).mat("metal").box(-0.5f, -3, -3, 1, 3, 6).add();
        part(b, "w_shaft", "hand_r", Role.STATIC, 0x3A2A2A).at(0, 1, 0).mat("wood").box(-0.5f, -0.5f, -14, 1, 1, 20).add();
        part(b, "w_axe", "w_shaft", Role.STATIC, 0x9A9AA8).at(0, 0, -14).mat("metal").box(-0.5f, -4, -3, 1, 6, 3).box(-0.5f, -0.5f, -5, 1, 1, 2).add();
        return b.build();
    }

    /** Scorpions: low armoured body, six legs, great pincers and a segmented tail arched over the back to a stinger. */
    static ModelSpec scorpion(String id, int shell, int dark, int sting) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.8f);
        part(b, "thorax", null, Role.BODY, shell).accent(dark).at(0, 19, 0).mat("chitin").box(-4, -2.5f, -5, 8, 4, 10).add();
        for (int i = 0; i < 3; i++) {
            part(b, "plate" + i, "thorax", Role.STATIC, i % 2 == 0 ? shell : dark).at(0, -2.5f, -4 + i * 3.5f).mat("chitin")
                    .box(-4.5f, -1, 0, 9, 1, 3).add();
        }
        part(b, "head", "thorax", Role.HEAD, shell).accent(0xE04020).at(0, 0, -5).mat("chitin").pattern("eyes").box(-2.5f, -2, -2, 5, 3, 2).add();
        String prev = "thorax";
        int[] tw = {3, 3, 2, 2};
        for (int i = 0; i < tw.length; i++) {
            part(b, "tail" + i, prev, Role.TAIL, shell).accent(dark).at(0, i == 0 ? -1 : 0, i == 0 ? 5 : 3.5f)
                    .rot(i == 0 ? 50 : 38, 0, 0).mat("chitin").box(-tw[i] / 2f, -tw[i] / 2f, 0, tw[i], tw[i], 4).add();
            prev = "tail" + i;
        }
        part(b, "stinger", prev, Role.STATIC, sting).at(0, 0, 4).rot(50, 0, 0).mat("horn").box(-1, -1, 0, 2, 2, 2).box(-0.5f, -0.5f, 2, 1, 1, 2).add();
        sym(b, "arm", "thorax", false, shell).roles(Role.ARM_LEFT, Role.ARM_RIGHT).at(3.5f, 0, -4).rot(0, 25, 0).mat("chitin")
                .box(-1, -1, -5, 2, 2, 5).add();
        sym(b, "pincer", "arm", true, shell).at(0, 0, -5).rot(0, -35, 0).mat("chitin").box(-1.5f, -1.5f, -4, 3, 3, 4).add();
        sym(b, "pincer_tip", "pincer", true, dark).at(0, 0, -4).mat("horn").box(-1.5f, -1, -2, 1, 2, 2).box(0.5f, -1, -2, 1, 2, 2).add();
        for (int i = 0; i < 3; i++) {
            sym(b, "leg" + i, "thorax", false, dark).roles(Role.LEG_EXTRA, Role.LEG_EXTRA).at(4, 0.5f, -2.5f + i * 2.5f).rot(0, -(i - 1) * 20, -30)
                    .mat("chitin").box(0, -0.5f, -0.5f, 4, 1, 1, 0.2f).add();
            sym(b, "foot" + i, "leg" + i, true, dark).at(4, 0, 0).rot(0, 0, 70).mat("chitin").box(0, -0.5f, -0.5f, 4, 1, 1).add();
        }
        return b.build();
    }

    /** Nex: Zaros's general: a horned, armoured figure with great dark wings and glowing purple eyes. */
    static ModelSpec nex() {
        ModelSpec.Builder b = Humanoid.of("nex").body(12, 9, 11, 5, 12, 3).skin(0x8A7A9A).face("eyes").glowingEyes().hair(0, "none")
                .top(0x3A2A4A, "metal").trim(0xA040E0).bottoms(0x2A1E36, "plateskirt").pauldrons(0x3A2A4A).boots(0x1A1222)
                .horns(0xC8B8D8).gloves(0x2A1E36).builder();
        part(b, "crest", "head", Role.STATIC, 0x3A2A4A).at(0, -8, 0).mat("metal").box(-4, -1, -4, 8, 3, 8, 0.3f).add();
        sym(b, "wing", "torso", false, 0x2A1E36).roles(Role.WING_LEFT, Role.WING_RIGHT).accent(0xA040E0).at(3, -9, 2.5f).rot(-10, -30, -25)
                .mat("horn").box(0, -1, -1, 12, 2, 2).add();
        sym(b, "wing_web", "wing", true, 0x4A2A6A).accent(0xA040E0).at(0, 1, 0).mat("membrane").glow("runes").box(0, 0, -0.5f, 12, 11, 1).add();
        sym(b, "wing_finger", "wing", true, 0x2A1E36).at(12, 0, 0).rot(0, 0, 50).mat("horn").box(-0.5f, 0, -0.5f, 1, 11, 1).add();
        return b.build();
    }
}
