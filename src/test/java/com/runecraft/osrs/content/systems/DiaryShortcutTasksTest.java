package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.content.skilling.AgilityShortcuts;
import com.runecraft.osrs.data.AgilityCourse;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round 30: diary tasks that need an agility shortcut used, dungeon visits under their surface tiles, and the jewellery
 * teleport keys the game reports.
 */
class DiaryShortcutTasksTest {
    private static Diaries.Task byDesc(String prefix) {
        return Diaries.tasks().stream().filter(t -> t.description().startsWith(prefix)).findFirst().orElseThrow();
    }

    @Test
    void shortcutTasksNameRealShortcuts() {
        Set<String> ids = new HashSet<>();
        AgilityShortcuts.SHORTCUTS.forEach(s -> ids.add(s.id()));
        AgilityShortcuts.INDOOR.forEach(s -> ids.add(s.id()));
        int n = 0;
        for (Diaries.Task t : Diaries.tasks()) {
            if (t.type() == Diaries.Type.EVENT && t.target().startsWith("shortcut:")) {
                n++;
                String id = t.target().substring("shortcut:".length());
                assertTrue(ids.contains(id), t.id() + " names no shortcut: " + id);
                AgilityShortcuts.Shortcut s = AgilityShortcuts.indoor(id) != null ? AgilityShortcuts.indoor(id)
                        : AgilityShortcuts.SHORTCUTS.stream().filter(x -> x.id().equals(id)).findFirst().orElseThrow();
                // The task's Agility requirement is the shortcut's.
                assertEquals(s.level(), (int) t.reqs().getOrDefault(com.runecraft.osrs.Skill.AGILITY, 0), t.id());
            }
        }
        assertTrue(n >= 15, "shortcut tasks: " + n);
        assertTrue(Diaries.ACTIVITIES.contains("shortcut"));
        // The Salve shortcut exists now, so its task is no longer a stand-in.
        Diaries.Task salve = byDesc("Use the shortcut to get to the bridge over the Salve");
        assertEquals("shortcut:salve_ornate_railing", salve.target());
        assertFalse(Diaries.isApprox(salve));
        assertEquals("shortcut:dense_essence_boulder", byDesc("Use the boulder leap").target());
        assertEquals("shortcut:trollheim_wilderness", byDesc("Take the agility shortcut from Trollheim").target());
    }

    @Test
    void dungeonVisitsAreUnderground() {
        Diaries.Task wall = byDesc("Discover hidden wall");
        Diaries.Task crandor = byDesc("Visit the Isle of Crandor");
        Diaries.Task sos = byDesc("Enter the second level of the Stronghold");
        for (Diaries.Task t : List.of(wall, crandor, sos)) {
            assertEquals(Diaries.Type.VISIT, t.type(), t.id());
        }
        assertTrue(wall.target().endsWith(",u") && crandor.target().endsWith(",u"));
        assertTrue(Diaries.questReqs(crandor).contains("dragon_slayer_i"));
        // Underground: the surface (y 64) doesn't count, the volcano dungeon floor (y -7) does.
        assertFalse(Diaries.visitDepthOk(crandor.target(), 64));
        assertTrue(Diaries.visitDepthOk(crandor.target(), -7));
        // The Stronghold's first floor stands at block y 37, its second at 21.
        assertFalse(Diaries.visitDepthOk(sos.target(), 37));
        assertTrue(Diaries.visitDepthOk(sos.target(), 21));
        assertTrue(Diaries.visitDepthOk("3081,3421,10", 200));
        int[] v = Diaries.visitTarget(sos.target());
        assertEquals(3066, v[0]);
        assertEquals(25, v[2]);
    }

    @Test
    void chargedJewelleryTeleportKeys() {
        // A digsite pendant with any number of charges reports its base id.
        assertEquals("teleport:digsite_pendant:digsite", Diaries.teleportKey("digsite_pendant_3", "digsite_pendant", "Digsite"));
        assertEquals("teleport:camulet:enakhras_temple", Diaries.teleportKey("camulet", null, "Enakhra's Temple"));
        Diaries.Task digsite = byDesc("Teleport to the digsite using a Digsite pendant");
        assertEquals(Diaries.Type.EVENT, digsite.type());
        assertTrue(Diaries.eventMatches(digsite.target(), Diaries.teleportKey("digsite_pendant_5", "digsite_pendant", "Digsite")));
        assertTrue(Diaries.questReqs(digsite).contains("the_dig_site"));
        // Every jewellery teleport task names a real piece (its base id) and one of its destinations.
        Set<String> keys = new HashSet<>();
        List<Jewellery.Worn> pieces = new java.util.ArrayList<>(Jewellery.all());
        // Held teleport items (round 40: Kharedst's memoirs and the book of the dead) report keys the same way.
        for (String held : List.of("kharedsts_memoirs", "book_of_the_dead")) {
            pieces.add(Jewellery.held(held));
        }
        for (Jewellery.Worn w : pieces) {
            for (Teleports.Destination d : w.teleports()) {
                keys.add(Diaries.teleportKey(w.id(), w.chargeBase(), d.name()));
            }
        }
        for (Diaries.Task t : Diaries.tasks()) {
            if (t.type() == Diaries.Type.EVENT && t.target().startsWith("teleport:") && !t.target().startsWith("teleport:skull_sceptre")) {
                // '&' parts (Kharedst's memoirs: all five cities) and '|' alternatives (either book) are each a fired key.
                for (String key : t.target().split("[&|]")) {
                    assertTrue(keys.contains(key), t.id() + " teleport key never fired: " + key);
                }
            }
        }
    }

    @Test
    void dorgeshKaanCanBeFailedAt99() {
        AgilityCourse dk = AgilityCourse.DORGESH_KAAN;
        int failable = -1;
        for (int i = 0; i < dk.obstacles(); i++) {
            if (dk.canFail(i)) {
                failable = i;
                break;
            }
        }
        assertTrue(failable >= 0);
        assertEquals(AgilityCourse.DORGESH_KAAN_MIN_FAIL, dk.failChance(failable, 99), 1e-9);
        assertEquals(0, AgilityCourse.CANIFIS.failChance(0, 99), 1e-9);
        assertTrue(dk.needsLight() && !AgilityCourse.GNOME_STRONGHOLD.needsLight());
        assertTrue(AgilityCourse.LIGHT_SOURCES.contains("bug_lantern"));
    }
}
