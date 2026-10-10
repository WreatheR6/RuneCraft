package com.runecraft.osrs.content.world;

import com.runecraft.osrs.world.StructureDef;

import java.util.ArrayList;
import java.util.List;

/**
 * Asgarnia and Karamja: Falador, Port Sarim, Rimmington, the Crafting Guild, Taverley, Burthorpe,
 * the Warriors' and Heroes' Guilds, the Black Knights' Fortress, Ice Mountain, Goblin Village,
 * Entrana, Musa Point, Brimhaven, Shilo Village, Tai Bwo Wannai and the Karamja volcano.
 *
 * <p>Within each settlement the first structure carries the terrain flattening and later ones
 * (which may replace other places inside the town) build on top of it, because a flatten clears
 * everything above the new ground and blends the ring around it.
 */
final class AsgarniaStructures {
    private AsgarniaStructures() {
    }

    static List<StructureDef> all() {
        List<StructureDef> out = new ArrayList<>();
        AsgarniaFalador.build(out);
        AsgarniaTowns.build(out);
        AsgarniaNorth.build(out);
        AsgarniaKaramja.build(out);
        return out;
    }
}
