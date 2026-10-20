package com.runecraft.osrs.content.raids;

import com.runecraft.osrs.model.ModelSpec;

import java.util.List;

/**
 * Tombs of Amascut models. The raid's bosses and monsters are sculpted in the bestiary module
 * ({@code com.runecraft.osrs.content.bestiary.BestiaryRad*}), which registers them under the same ids; this holds the
 * raid's own props: Kephri's eggs.
 */
public final class ToaModels {
    private ToaModels() {
    }

    public static List<ModelSpec> models() {
        return List.of(egg("toa_kephri_egg", 0xE8DCB0, 0xC8B880, 0.6f), egg("toa_kephri_dark_egg", 0x5A4A2A, 0x3A2A18, 0.9f));
    }

    /** Kephri's egg: a speckled ovoid half sunk in the sand (OSRS: small and light, or large and dark). */
    static ModelSpec egg(String id, int shell, int speckle, float scale) {
        return ModelSpec.builder(id).scale(scale).shadow(0.5f * scale)
                .part("body", null, ModelSpec.Role.BODY, shell, speckle).pivot(0, 24, 0).material("chitin").pattern("spots")
                .box(-4, -9, -4, 8, 9, 8)
                .part("crown", "body", ModelSpec.Role.STATIC, shell, speckle).pivot(0, -9, 0).material("chitin").pattern("spots")
                .box(-3, -3, -3, 6, 3, 6)
                .part("tip", "crown", ModelSpec.Role.STATIC, shell, speckle).pivot(0, -3, 0).material("chitin")
                .box(-2, -2, -2, 4, 2, 4)
                .build();
    }
}
