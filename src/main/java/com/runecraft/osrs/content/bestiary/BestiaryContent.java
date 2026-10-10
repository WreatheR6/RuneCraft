package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.content.ContentModule;
import com.runecraft.osrs.model.ModelSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * OSRS-accurate looks for creatures that would otherwise render as a plain biped or a vanilla mob:
 * the core monsters (men, goblins, giants, demons...), the vanilla mobs Gielinor uses (cows, chickens,
 * rats, spiders...), TzTok-Jad, the Barrows brothers and the other built-in bosses, and every module
 * monster/boss without a model of its own. Models are keyed by creature id, so the client picks them up
 * automatically. Pure data: no items, monsters or blocks of its own.
 */
public final class BestiaryContent implements ContentModule {
    @Override
    public String id() {
        return "bestiary";
    }

    @Override
    public List<ModelSpec> models() {
        List<ModelSpec> list = new ArrayList<>();
        list.addAll(Beasts.all());
        list.addAll(Folk.all());
        list.addAll(Creatures.all());
        list.addAll(Bosses.fresh());
        list.addAll(BestiaryHum.fresh());
        return list;
    }

    @Override
    public List<ModelSpec> fallbackModels(java.util.Set<String> existingModelIds) {
        return Fallbacks.all(existingModelIds);
    }

    @Override
    public List<ModelSpec> modelOverrides() {
        return Overrides.all();
    }
}
