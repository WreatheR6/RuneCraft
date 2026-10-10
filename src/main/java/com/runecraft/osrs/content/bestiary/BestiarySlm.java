package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Registry of the re-sculpted slayer and dungeon monsters (dragons, wyverns, demons, slayer creatures, the
 * Catacombs, the Abyss and the dungeon creatures). Three lists, by where the id first comes from:
 * <ul>
 *   <li>{@link #overrides()}: ids another module already models (slayer module) - replaced in place;</li>
 *   <li>{@link #fresh()}: ids the bestiary itself owns (demons, Catacombs hellhound);</li>
 *   <li>{@link #fallbacks()}: creatures with no model of their own, supplied ahead of the generic fallbacks.</li>
 * </ul>
 */
final class BestiarySlm {
    private BestiarySlm() {
    }

    static List<ModelSpec> overrides() {
        List<ModelSpec> l = new ArrayList<>();
        l.addAll(BestiarySlmDragons.all());
        l.addAll(BestiarySlmWyverns.all());
        l.addAll(BestiarySlmLow.overrides());
        l.addAll(BestiarySlmMid.overrides());
        l.add(BestiarySlmDemons.imp("gwd_imp"));
        l.add(BestiarySlmDemons.abyssalDemon("abyssal_demon"));
        l.add(BestiarySlmDemons.canine("hellhound", 0x3A1614, 0x1E0C0C, 0xFF8A20, true));
        l.add(BestiarySlmDemons.canine("terror_dog", 0x6A6458, 0x3A362E, 0xE03020, false));
        l.add(BestiarySlmDemons.gargoyle("gargoyle"));
        l.add(BestiarySlmDemons.nechryael("nechryael"));
        l.add(BestiarySlmDemons.bloodveld("bloodveld", 0xB04A44, 0xE89A9A, false));
        l.add(BestiarySlmDemons.bloodveld("mutated_bloodveld", 0x7A2A66, 0xC070B0, true));
        return l;
    }

    static List<ModelSpec> fresh() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(BestiarySlmDemons.demon("lesser_demon", 0xA8281E, 0x5A1410, 0xE0D4B4, false));
        l.add(BestiarySlmDemons.demon("greater_demon", 0x8A1C16, 0x3E0C0A, 0xD8CCA8, true));
        l.add(BestiarySlmDemons.demon("black_demon", 0x2E2A2C, 0x161214, 0x8A2420, true));
        l.add(BestiarySlmDemons.demon("catacomb_greater_demon", 0x7E1A16, 0x380C0A, 0xC8BC98, true));
        l.add(BestiarySlmDemons.demon("catacomb_black_demon", 0x26222A, 0x121014, 0x9A2A24, true));
        l.add(BestiarySlmDemons.canine("catacomb_hellhound", 0x5A1A16, 0x2A0C0A, 0xFFA020, true));
        l.addAll(BestiarySlmFolk.fresh());
        return l;
    }

    static List<ModelSpec> fallbacks() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(BestiarySlmDemons.abyssalDemon("catacomb_abyssal_demon"));
        l.addAll(BestiarySlmLow.fallbacks());
        l.addAll(BestiarySlmMid.fallbacks());
        l.addAll(BestiarySlmFolk.fallbacks());
        return l;
    }

    /** The slayer fallbacks first, then {@code rest} for every creature they do not cover. */
    static List<ModelSpec> withFallbacks(Set<String> existing, Function<Set<String>, List<ModelSpec>> rest) {
        List<ModelSpec> l = new ArrayList<>();
        Set<String> taken = new HashSet<>(existing);
        for (ModelSpec m : fallbacks()) {
            if (taken.add(m.id())) {
                l.add(m);
            }
        }
        l.addAll(rest.apply(taken));
        return l;
    }
}
