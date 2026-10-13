package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.List;

/**
 * The Crash Site Cavern's gorillas (slayer module, {@code DemonicGorillas}) and Fossil Island's Deranged archaeologist
 * (bosses module). Model pixels; Y = 24 is the ground and -Z is the front.
 */
final class BestiaryCrashFossil {
    private BestiaryCrashFossil() {
    }

    static List<ModelSpec> all() {
        return List.of(
                // Demonic gorilla: one of Glough's demonic creations - a charcoal-furred, knuckle-walking ape with a ridge of
                // bone spikes down its back, pale horn nubs and burning red eyes.
                gorilla("demonic_gorilla", 0x2C2A2E, 0x18161A, 0x5A4E4E, 0xFF3020, 0xD8CCB8, true),
                // Tortured gorilla: the same breed before the demonic change - a scarred brown-grey ape with raw patches.
                gorilla("tortured_gorilla", 0x5A4C40, 0x3A3028, 0x8A5A50, 0xF0C040, 0xC89080, false),
                derangedArchaeologist());
    }

    /** A knuckle-walking gorilla: massive arms to the ground, short bowed legs, a heavy brow and a fanged jaw. */
    static ModelSpec gorilla(String id, int fur, int dark, int face, int eye, int accent, boolean demonic) {
        ModelSpec.Builder b = ModelSpec.builder(id).scale(1.0f).shadow(1.1f)
                .part("body", null, Role.BODY, fur, dark).pivot(0, 12, 1).rotate(28, 0, 0).material("fur")
                .box(-7, -12, -5, 14, 13, 10)
                .part("chest", "body", Role.STATIC, face).pivot(0, -7, -5).box(-5, -4, -1, 10, 7, 2).material("skin")
                .part("hump", "body", Role.STATIC, dark).pivot(0, -12, 1).box(-6, -2, -4, 12, 3, 8).material("fur")
                .part("hips", null, Role.STATIC, fur, dark).pivot(0, 13, 5).box(-5, -3, -3, 10, 6, 6).material("fur")
                .part("head", "body", Role.HEAD, face, dark).pivot(0, -11, -5).rotate(-28, 0, 0).material("skin")
                .box(-4, -5, -5, 8, 7, 6).pattern("eyes")
                .part("scalp", "head", Role.STATIC, fur).pivot(0, -4, 1).box(-4.5f, -2, -2, 9, 4, 4).material("fur")
                .part("brow", "head", Role.STATIC, dark).pivot(0, -3, -5).box(-4.5f, -1, -1.5f, 9, 2, 2)
                .part("eyes", "head", Role.STATIC, eye).pivot(0, -1.6f, -5.2f).material("energy").glow("full")
                .box(1, -0.5f, -0.4f, 2, 1, 1).box(-3, -0.5f, -0.4f, 2, 1, 1)
                .part("muzzle", "head", Role.STATIC, face).pivot(0, 0, -5).box(-3, -1, -2, 6, 3, 2).material("skin")
                .part("jaw", "head", Role.JAW, dark).pivot(0, 2, -4).rotate(12, 0, 0).box(-3, 0, -3, 6, 2, 4)
                .part("fangs", "jaw", Role.STATIC, 0xF0E8D0).pivot(0, 0, -3).box(-2.5f, -1.5f, -0.5f, 1, 2, 1)
                .box(1.5f, -1.5f, -0.5f, 1, 2, 1);
        // Huge arms reaching the ground on their knuckles.
        for (int side = -1; side <= 1; side += 2) {
            String s = side < 0 ? "right" : "left";
            b.part("arm_" + s, null, side < 0 ? Role.ARM_RIGHT : Role.ARM_LEFT, fur, dark).pivot(side * 9, 4, -5).rotate(-10, 0, side * 8)
                    .material("fur").box(-3.5f, -3, -3.5f, 7, 11, 7)
                    .part("forearm_" + s, "arm_" + s, Role.STATIC, fur, dark).pivot(0, 8, 0).rotate(10, 0, 0).material("fur")
                    .box(-3, 0, -3, 6, 10, 6)
                    .part("fist_" + s, "forearm_" + s, Role.STATIC, face).pivot(0, 10, 0).material("skin").box(-3.5f, 0, -4, 7, 2, 6)
                    .part("leg_" + s, null, side < 0 ? Role.LEG_BACK_RIGHT : Role.LEG_BACK_LEFT, fur, dark).pivot(side * 4.5f, 15, 6)
                    .material("fur").box(-2.5f, 0, -2.5f, 5, 7, 5)
                    .part("foot_" + s, "leg_" + s, Role.STATIC, face).pivot(0, 7, 0).material("skin").box(-3, 0, -4, 6, 2, 6);
        }
        if (demonic) {
            // A ridge of bone spikes down the spine and two short horns.
            b.part("spines", "body", Role.STATIC, accent).pivot(0, -12, 0)
                    .box(-1, -4, -3, 2, 4, 2).box(-1, -3, 0, 2, 3, 2).box(-1, -3, 3, 2, 3, 1)
                    .part("horns", "head", Role.STATIC, accent).pivot(0, -5, -1)
                    .box(-4, -3, -1, 1, 3, 1).box(3, -3, -1, 1, 3, 1)
                    .part("shoulder_spikes", "body", Role.STATIC, accent).pivot(0, -10, -2)
                    .box(-7.5f, -3, -1, 1, 3, 1).box(6.5f, -3, -1, 1, 3, 1);
        } else {
            // Raw, scarred patches and a crude iron collar from Glough's experiments.
            b.part("scars", "body", Role.STATIC, accent).pivot(0, -6, 0).box(-7.2f, -3, -2, 1, 4, 4).box(6.2f, -2, 0, 1, 3, 3)
                    .part("collar", "head", Role.STATIC, 0x6A6A70).pivot(0, 2, 1).material("metal").box(-4.5f, 0, -2, 9, 2, 5);
        }
        return b.build();
    }

    /**
     * The Deranged archaeologist: a wild-eyed old scholar gone native in the Tar Swamp - tattered khaki coat, battered pith
     * helmet over a shock of white hair and beard, a satchel of books and one book raised to throw.
     */
    static ModelSpec derangedArchaeologist() {
        int skin = 0xD0A07A;
        int coat = 0x7A6A42;
        int trousers = 0x4A3E2A;
        int hair = 0xE6E2DA;
        return ModelSpec.builder("deranged_archaeologist").shadow(0.6f)
                .part("body", null, Role.BODY, coat).pivot(0, 12, 0).rotate(8, 0, 0).box(-4, -12, -2, 8, 12, 4).material("cloth")
                .part("coat_tails", "body", Role.STATIC, coat).pivot(0, 0, 0).box(-4.3f, -1, -2.3f, 8, 5, 4, 0.3f)
                .part("belt", "body", Role.STATIC, 0x3A2A1A).pivot(0, -1, 0).box(-4.2f, -1, -2.2f, 8, 1, 4, 0.2f).material("leather")
                .part("satchel", "body", Role.STATIC, 0x6A4A2A).pivot(3, -4, 0).box(0, 0, -2, 2, 5, 4).material("leather")
                .part("strap", "body", Role.STATIC, 0x5A3A1A).pivot(0, -12, -2.3f).rotate(0, 0, 35).box(-0.5f, 0, -0.2f, 1, 14, 1)
                .part("head", "body", Role.HEAD, skin).pivot(0, -12, 0).box(-4, -8, -4, 8, 8, 8).pattern("eyes").material("skin")
                .part("hair", "head", Role.STATIC, hair).pivot(0, -6, 1).box(-4.6f, -2, -2, 9, 5, 6).material("fur")
                .part("beard", "head", Role.STATIC, hair).pivot(0, -1, -4).box(-3, -1, -1, 6, 5, 2).material("fur")
                .part("helmet", "head", Role.STATIC, 0xC8B488).pivot(0, -8, 0).box(-5, -1, -5, 10, 1, 10).box(-4, -4, -4, 8, 3, 8)
                .part("helmet_band", "helmet", Role.STATIC, 0x5A4A2A).pivot(0, -1.5f, 0).box(-4.2f, -1, -4.2f, 8, 1, 8, 0.2f)
                .part("arm_right", null, Role.ARM_RIGHT, coat).pivot(-5, 2, 0).box(-2, -2, -2, 3, 12, 4).material("cloth")
                .part("hand_right", "arm_right", Role.STATIC, skin).pivot(-0.5f, 10, 0).box(-1.5f, 0, -1.5f, 3, 2, 3)
                .part("arm_left", null, Role.ARM_LEFT, coat).pivot(5, 2, 0).rotate(-60, 0, 0).box(-1, -2, -2, 3, 12, 4).material("cloth")
                .part("hand_left", "arm_left", Role.STATIC, skin).pivot(0.5f, 10, 0).box(-1.5f, 0, -1.5f, 3, 2, 3)
                .part("book", "hand_left", Role.STATIC, 0xC05A1A).pivot(0, 2, 0).box(-2, 0, -3, 4, 1, 5).material("leather")
                .part("leg_right", null, Role.LEG_BACK_RIGHT, trousers).pivot(-2, 12, 0).box(-2, 0, -2, 4, 12, 4).material("cloth")
                .part("leg_left", null, Role.LEG_BACK_LEFT, trousers).pivot(2, 12, 0).box(-2, 0, -2, 4, 12, 4).material("cloth")
                .part("boots", "leg_right", Role.STATIC, 0x2A1E14).pivot(0, 9, 0).box(-2.2f, 0, -2.6f, 4, 3, 5, 0.2f)
                .part("boots_l", "leg_left", Role.STATIC, 0x2A1E14).pivot(0, 9, 0).box(-2.2f, 0, -2.6f, 4, 3, 5, 0.2f)
                .build();
    }
}
