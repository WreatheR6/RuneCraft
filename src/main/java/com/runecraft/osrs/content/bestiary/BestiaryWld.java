package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Everyday world monsters and animals (townsfolk in arms, goblins, giants, ogres, trolls, farm animals, vermin,
 * beasts, undead, TzHaar, implings, crabs...): premium hand-sculpted looks keyed by creature id.
 */
final class BestiaryWld {
    private BestiaryWld() {
    }

    /** Models whose ids no other module defines (bestiary-owned ids and creatures that would otherwise use a fallback). */
    static List<ModelSpec> models() {
        List<ModelSpec> l = new ArrayList<>();
        l.addAll(BestiaryWldFolk.all());
        l.addAll(BestiaryWldBeasts.all());
        l.addAll(BestiaryWldMisc.all());
        l.addAll(BestiaryWldIsles.all());
        return l;
    }

    /** Replacements for models another module registers under the same id. */
    static List<ModelSpec> overrides() {
        List<ModelSpec> l = new ArrayList<>();
        l.addAll(BestiaryWldBeasts.overrides());
        l.addAll(BestiaryWldMisc.overrides());
        return l;
    }
}
