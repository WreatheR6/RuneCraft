package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/**
 * Round 21 re-sculpts of common creatures, part 3. Same rules as {@link Common1} and {@link Common2}: each spec was
 * rendered before/after against the model it replaces and kept only where it reads clearly closer to the OSRS model.
 * Ids, hitboxes and everything outside the model are unchanged. Registered after {@link Common2} in {@link Overrides}.
 */
final class Common3 {
    private Common3() {
    }

    static List<ModelSpec> overrides() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(drake());
        return l;
    }

    private static final int TOOTH = 0xF0EADA;

    /**
     * Drake: a wingless dragonkin of the Karuulm dungeon, standing semi-upright on two thick digitigrade legs, its
     * chest raised and its small clawed forelimbs held up, a long neck carrying a horned, heavy-jawed head forward,
     * a bony ridge down the back and a long tail sweeping to the ground behind for balance. Red-brown scales over a
     * pale belly.
     */
    static ModelSpec drake() {
        int skin = 0x8A3A24;
        int dark = 0x4E1E14;
        int belly = 0xC8925E;
        int horn = 0xD8C8A0;
        ModelSpec.Builder b = ModelSpec.builder("drake").shadow(1.1f);
        part(b, "hips", null, Role.BODY, skin).accent(dark).at(0, 11, 5).mat("scales").box(-4.5f, -4, -4.5f, 9, 8, 9).add();
        part(b, "torso", "hips", Role.STATIC, skin).accent(dark).at(0, -3, -2).rot(45, 0, 0).mat("scales")
                .box(-5, -12, -4.5f, 10, 13, 9).add();
        part(b, "belly", "torso", Role.STATIC, belly).accent(dark).at(0, -5.5f, -4.5f).mat("plates").box(-4, -6, -0.5f, 8, 11, 1).add();
        part(b, "ridge", "torso", Role.STATIC, horn).at(0, -6, 4.5f).mat("horn")
                .box(-0.5f, -6, -0.5f, 1, 2, 2).box(-0.5f, -2, -0.5f, 1, 3, 2).box(-0.5f, 2, -0.5f, 1, 2, 2).add();
        // long neck rising from the chest, curving forward to a level head
        part(b, "neck", "torso", Role.NECK, skin).accent(dark).at(0, -11.5f, -1).rot(-15, 0, 0).mat("scales")
                .box(-2.5f, -8, -2.5f, 5, 9, 5).add();
        part(b, "neck2", "neck", Role.NECK, skin).accent(dark).at(0, -7.5f, 0).rot(20, 0, 0).mat("scales")
                .box(-2, -7, -2, 4, 8, 4).add();
        part(b, "head", "neck2", Role.HEAD, skin).accent(0xFFC040).at(0, -7, 0).rot(-50, 0, 0).mat("scales").pattern("eyes").glow("eyes")
                .box(-3, -3.5f, -5, 6, 5, 7).add();
        part(b, "snout", "head", Role.STATIC, skin).accent(dark).at(0, -1.5f, -5).mat("scales").box(-2, -1.5f, -5, 4, 3, 5).add();
        part(b, "jaw", "head", Role.JAW, belly).accent(dark).at(0, 1.5f, -4).rot(8, 0, 0).mat("plates").box(-2, 0, -6, 4, 2, 6).add();
        part(b, "teeth", "snout", Role.STATIC, TOOTH).at(0, 1.5f, -2.5f).mat("horn").box(-2, 0, -2.5f, 4, 1, 5, -0.15f).add();
        sym(b, "horn", "head", false, horn).at(2, -3, 1).rot(28, 0, -12).mat("horn").box(-1, -1, 0, 2, 2, 5).add();
        sym(b, "horn_tip", "horn", true, horn).at(0, 0, 5).rot(18, 0, 0).mat("horn").box(-0.5f, -0.5f, 0, 1, 1, 4).add();
        sym(b, "frill", "head", false, dark).accent(skin).at(3, -1, 1).rot(0, 35, 0).mat("membrane").box(0, -2, 0, 1, 4, 3).add();
        // small clawed forelimbs held up in front of the chest
        sym(b, "arm", "torso", false, skin).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(dark).at(5, -9, -2).rot(-75, 0, -8)
                .mat("scales").box(-1.5f, -1, -1.5f, 3, 6, 3).add();
        sym(b, "forearm", "arm", true, skin).at(0, 5.5f, 0).rot(50, 0, 0).mat("scales").box(-1.5f, 0, -1.5f, 3, 5, 3).add();
        sym(b, "hand_claw", "forearm", true, horn).at(0, 5, 0).mat("horn").box(-1.5f, 0, -1, 1, 2, 1).box(0.5f, 0, -1, 1, 2, 1).add();
        // thick digitigrade hind legs
        sym(b, "thigh", "hips", false, skin).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(dark).at(5, 0, 0).rot(-35, 0, 0)
                .mat("scales").box(-3, -2, -3.5f, 6, 9, 7).add();
        sym(b, "shin", "thigh", true, skin).at(0, 7, 0).rot(75, 0, 0).mat("scales").box(-2, 0, -2, 4, 6, 4).add();
        sym(b, "foot", "shin", true, dark).at(0, 5.5f, 0).rot(-40, 0, 0).mat("scales").box(-2.5f, 0, -5, 5, 2, 7)
                .box(-2.5f, 0.5f, -6.5f, 5, 1, 2).add();
        // long tail sweeping down to the ground behind
        part(b, "tail", "hips", Role.TAIL, skin).accent(dark).at(0, -1, 4).rot(-25, 0, 0).mat("scales").box(-3, -3, 0, 6, 6, 8).add();
        part(b, "tail_mid", "tail", Role.STATIC, skin).at(0, 0, 8).rot(-10, 0, 0).mat("scales").box(-2, -2, 0, 4, 4, 8).add();
        part(b, "tail_tip", "tail_mid", Role.STATIC, dark).at(0, 0, 8).rot(-5, 0, 0).mat("scales").box(-1.5f, -1.5f, 0, 3, 3, 8)
                .box(-0.5f, -3, 2, 1, 2, 2).box(-0.5f, -3, 5, 1, 2, 2).add();
        return b.build();
    }
}
