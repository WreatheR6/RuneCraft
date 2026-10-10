package com.runecraft.osrs.content.world;

import com.runecraft.osrs.world.StructureDef;

import java.util.ArrayList;
import java.util.List;

/** Kandarin, the Fremennik Province, Tirannwn and the troll lands: Camelot, Seers' Village, Catherby, Ardougne, Yanille, the Gnome Stronghold, Rellekka, Lletya, Prifddinas, Trollheim. */
final class KandarinStructures {
    private KandarinStructures() {
    }

    static List<StructureDef> all() {
        List<StructureDef> out = new ArrayList<>();
        KandarinSeers.addAll(out);
        KandarinArdougne.addAll(out);
        KandarinSouth.addAll(out);
        KandarinGnomes.addAll(out);
        KandarinNorth.addAll(out);
        KandarinElves.addAll(out);
        return out;
    }
}
