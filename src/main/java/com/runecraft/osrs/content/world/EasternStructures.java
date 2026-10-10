package com.runecraft.osrs.content.world;

import com.runecraft.osrs.world.StructureDef;

import java.util.ArrayList;
import java.util.List;

/**
 * Morytania, the Kharidian Desert and the Wilderness: Al Kharid, the PvP Arena, Shantay Pass, Pollnivneach,
 * Nardah, Sophanem, Menaphos and the desert pyramids; Paterdomus, Canifis, the Slayer Tower, Port Phasmatys,
 * the Barrows, Mort'ton, Burgh de Rott, Ver Sinhaza, Darkmeyer, Fenkenstrain's Castle and Slepe; the
 * Wilderness ditch, Ferox Enclave, the Lava Maze, the Mage Arena, Rogues' Castle and the Wilderness ruins.
 * Each settlement levels its own footprint first; its separately listed buildings follow it.
 */
final class EasternStructures {
    private EasternStructures() {
    }

    static List<StructureDef> all() {
        List<StructureDef> out = new ArrayList<>();
        EasternDesert.addAll(out);
        EasternMorytania.addAll(out);
        EasternWilderness.addAll(out);
        return out;
    }
}
