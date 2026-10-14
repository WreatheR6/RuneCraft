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
        // ---- round 10 flagship boss re-sculpts (last, so they win) ----
        l.addAll(Flagships.overrides());
        // ---- round 11 flagship boss re-sculpts, part 2 ----
        l.addAll(Flagships2.overrides());
        // ---- round 12 flagship boss re-sculpts, part 3 ----
        l.addAll(Flagships3.overrides());
        // ---- round 19 re-sculpts of the most commonly met creatures ----
        l.addAll(Common1.overrides());
        // ---- round 20 re-sculpts, part 2 (shared TzHaar body, wingless demons, slayer creatures) ----
        l.addAll(Common2.overrides());
        return l;
    }
}
