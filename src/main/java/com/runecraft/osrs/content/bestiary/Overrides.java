package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;

import java.util.ArrayList;
import java.util.List;

/** Re-sculpted, OSRS-faithful replacements for models first defined by other modules (same ids). */
final class Overrides {
    private Overrides() {
    }

    static List<ModelSpec> all() {
        List<ModelSpec> l = new ArrayList<>();
        l.addAll(BestiarySlm.overrides());
        // ---- slayer creatures ----
        l.addAll(Slayers.all());
        l.addAll(Bosses.overrides());
        l.addAll(BigBosses.overrides());
        l.addAll(BestiaryBst.all());
        l.addAll(BestiaryWld.overrides());
        l.addAll(BestiaryHum.overrides());
        l.addAll(BestiaryQbm.overrides());
        l.addAll(BestiaryLookalikes.overrides());
        return l;
    }
}
