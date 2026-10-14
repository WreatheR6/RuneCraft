package com.runecraft.osrs;

import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.NpcDef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcRaceTest {
    private static NpcRace race(String id) {
        NpcDef n = ContentRegistry.npc(id);
        assertTrue(n != null, id);
        return NpcRace.of(n.id(), n.name(), n.place());
    }

    @Test
    void racesFromIdsNamesAndHomes() {
        assertEquals(NpcRace.DWARF, NpcRace.of(QuestNpc.DORIC.name(), QuestNpc.DORIC.displayName(), "FALADOR"));
        assertEquals(NpcRace.HUMAN, NpcRace.of(QuestNpc.DUKE_HORACIO.name(), "Duke Horacio", "LUMBRIDGE"));
        assertEquals(NpcRace.DWARF, race("thurgo"));
        assertEquals(NpcRace.DWARF, race("rowdy_dwarf"));
        assertEquals(NpcRace.DWARF, race("vermundi")); // Keldagrim resident
        assertEquals(NpcRace.GNOME, race("gnome_pickpocket"));
        assertEquals(NpcRace.GNOME, race("king_narnode"));
        assertEquals(NpcRace.GNOME, race("npc_banker_gnome_stronghold_tree_1"));
        assertEquals(NpcRace.ELF, race("arianwyn"));
        assertEquals(NpcRace.ELF, race("npc_banker_lletya_1"));
        assertEquals(NpcRace.TZHAAR, race("tzhaar_hur"));
        assertEquals(NpcRace.HUMAN, race("nieve")); // a human slayer master in the Gnome Stronghold
        assertEquals(NpcRace.HUMAN, race("hans"));
        assertEquals(NpcRace.HUMAN, race("tool_leprechaun_prifddinas"));
        assertTrue(NpcRace.GNOME.height() < NpcRace.DWARF.height() && NpcRace.DWARF.height() < 1f);
        assertTrue(NpcRace.ELF.height() > 1f);
    }
}
