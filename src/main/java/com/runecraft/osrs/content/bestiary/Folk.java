package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.Humanoid.*;

/** Humanoid monsters met all over Gielinor, dressed as in OSRS. */
final class Folk {
    private Folk() {
    }

    static List<ModelSpec> all() {
        List<ModelSpec> l = new ArrayList<>();
        // townsfolk, guards, goblins, giants, ogres, trolls and undead live in BestiaryWldFolk
        l.add(of("void_knight").skin(SKIN).hair(HAIR_BROWN, "short").top(0x3A3A3A, "cloth").trim(0xE0E0E0)
                .bottoms(0x3A3A3A, "robe").helm("full", 0xE0E0E0).weapon("mace", 0xE0E0E0).build());
        return l;
    }
}
