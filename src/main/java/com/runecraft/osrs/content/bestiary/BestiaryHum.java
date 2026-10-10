package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Premium re-sculpts of the humanoid bosses (Barrows brothers, God Wars generals and bodyguards, Nex, the wilderness
 * and quest humanoids, Moons of Peril, Sol Heredit...). {@link #fresh()} are ids no other module defines (they would
 * otherwise get a generated fallback); {@link #overrides()} replace models other modules define, keeping their ids.
 */
final class BestiaryHum {
    private BestiaryHum() {
    }

    static List<ModelSpec> fresh() {
        List<ModelSpec> l = new ArrayList<>();
        l.addAll(BestiaryHumBarrows.all());
        l.addAll(BestiaryHumBosses.fresh());
        l.addAll(BestiaryHumQuest.fresh());
        return l;
    }

    static List<ModelSpec> overrides() {
        List<ModelSpec> l = new ArrayList<>();
        l.addAll(BestiaryHumGwd.all());
        l.addAll(BestiaryHumBosses.overrides());
        l.addAll(BestiaryHumQuest.overrides());
        return l;
    }
}
