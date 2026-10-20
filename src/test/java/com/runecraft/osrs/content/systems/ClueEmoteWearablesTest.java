package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.quests.QuestBook;
import com.runecraft.osrs.content.systems.ClueScrolls.Clue;
import com.runecraft.osrs.content.systems.ClueScrolls.StepType;
import com.runecraft.osrs.content.systems.ClueScrolls.Tier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Emote clues need their items worn ("Blow a raspberry at Aris in her tent. Equip a gold ring and a gold necklace."), so
 * every item a usable emote step asks for must be wearable somewhere in the game: armour or weapon equipment, this module's
 * jewellery / cape / gloves slots, or the quests module's gear slots. Round 38's beginner walk failed because plain gold
 * jewellery couldn't be worn at all; leather gloves, vambraces, the red cape, the plain tiara and the pink skirt couldn't
 * either, so about fifteen usable emote steps could be dealt but never finished. There are no exceptions.
 */
class ClueEmoteWearablesTest {
    /**
     * Whether a player can wear {@code id}: in this module's extra slots, the quests module's gear slots, wielded or held as a
     * shield, or as armour (head, body, legs and feet take only ARMOUR items: a SIMPLE item's HEAD equipment only feeds its
     * tooltip and can't be put on).
     */
    static boolean wearable(String id) {
        ItemDef d = ItemCatalog.get(id);
        if (d == null) {
            return false;
        }
        if (Jewellery.worn(id) != null || QuestBook.get().gear.containsKey(id)) {
            return true;
        }
        if (d.equipment() == null) {
            return false;
        }
        return switch (d.equipment().slot()) {
            case WEAPON, SHIELD -> true;
            case HEAD, CHEST, LEGS, FEET -> d.kind() == ItemDef.Kind.ARMOUR;
        };
    }

    @Test
    void plainJewelleryIsWornWithNoBonuses() {
        int n = 0;
        for (String jewel : new String[]{"gold", "sapphire", "emerald", "ruby", "diamond", "dragonstone", "onyx", "zenyte", "opal",
                "jade", "topaz"}) {
            for (String type : new String[]{"ring", "necklace", "amulet", "bracelet"}) {
                String id = jewel + "_" + type;
                assertNotNull(ItemCatalog.get(id), id);
                Jewellery.Worn w = Jewellery.worn(id);
                assertNotNull(w, id + " can't be worn");
                Jewellery.Slot want = type.equals("ring") ? Jewellery.Slot.RING : type.equals("bracelet") ? Jewellery.Slot.HANDS
                        : Jewellery.Slot.NECK;
                assertEquals(want, w.slot(), id);
                assertEquals(com.runecraft.osrs.EquipmentStats.NONE, w.stats(), id + " has bonuses (OSRS: none)");
                n++;
            }
        }
        assertEquals(44, n);
        // An unstrung amulet can't be worn (OSRS: it needs a ball of wool first).
        assertNotNull(ItemCatalog.get("sapphire_amulet_u"));
        assertTrue(Jewellery.worn("sapphire_amulet_u") == null);
    }

    @Test
    void arisRaspberryStepCanBeDressedFor() {
        List<ClueScrolls.Npc> npcs = ClueScrolls.npcs();
        Clue aris = null;
        for (Clue c : ClueScrolls.usableClues(Tier.BEGINNER, npcs)) {
            if (c.type() == StepType.EMOTE && c.text().startsWith("Blow a raspberry at Aris")) {
                aris = c;
            }
        }
        assertNotNull(aris, "the Aris step is a usable beginner step");
        assertEquals(List.of("gold_ring", "gold_necklace"), ClueReqs.items(aris.req()));
        for (String id : ClueReqs.items(aris.req())) {
            assertTrue(wearable(id), id);
        }
    }

    @Test
    void everyEmoteStepItemCanBeWorn() {
        List<ClueScrolls.Npc> npcs = ClueScrolls.npcs();
        Set<String> unwearable = new TreeSet<>();
        for (Tier tier : Tier.values()) {
            for (Clue c : ClueScrolls.usableClues(tier, npcs)) {
                if (c.type() != StepType.EMOTE) {
                    continue;
                }
                for (List<List<ClueReqs.Token>> group : ClueReqs.groups(c.req())) {
                    // A group is met by any one of its alternatives: it needs at least one the game can wear.
                    boolean any = false;
                    Set<String> missing = new TreeSet<>();
                    for (List<ClueReqs.Token> alt : group) {
                        boolean all = true;
                        for (ClueReqs.Token t : alt) {
                            if (!t.isSlotRule() && !wearable(t.item())) {
                                all = false;
                                missing.add(t.item());
                            }
                        }
                        any |= all;
                    }
                    if (!any) {
                        unwearable.addAll(missing);
                    }
                }
            }
        }
        assertTrue(unwearable.isEmpty(), "emote clue items the game can't wear: " + unwearable);
    }

    /** Gloves, vambraces and capes from other modules are worn in this module's slots, with the OSRS wear levels. */
    @Test
    void glovesVambracesAndCapesAreWornWithTheirLevels() {
        Map<String, Jewellery.Slot> slots = Map.of("leather_gloves", Jewellery.Slot.HANDS, "leather_vambraces", Jewellery.Slot.HANDS,
                "snakeskin_vambraces", Jewellery.Slot.HANDS, "green_dhide_vambraces", Jewellery.Slot.HANDS,
                "blue_dhide_vambraces", Jewellery.Slot.HANDS, "red_dhide_vambraces", Jewellery.Slot.HANDS,
                "black_dhide_vambraces", Jewellery.Slot.HANDS, "hueycoatl_hide_vambraces", Jewellery.Slot.HANDS,
                "red_cape", Jewellery.Slot.CAPE);
        slots.forEach((id, slot) -> {
            assertNotNull(ItemCatalog.get(id), id);
            assertNotNull(Jewellery.worn(id), id + " can't be worn");
            assertEquals(slot, Jewellery.worn(id).slot(), id);
        });
        assertEquals(Map.of(), Jewellery.wearLevels("leather_gloves"));
        assertEquals(Map.of(), Jewellery.wearLevels("leather_vambraces"));
        assertEquals(Map.of(Skill.RANGED, 30, Skill.DEFENCE, 30), Jewellery.wearLevels("snakeskin_vambraces"));
        String[] hides = {"green", "blue", "red", "black"};
        for (int i = 0; i < hides.length; i++) {
            assertEquals(Map.of(Skill.RANGED, 40 + 10 * i), Jewellery.wearLevels(hides[i] + "_dhide_vambraces"), hides[i]);
        }
        assertEquals(Map.of(Skill.RANGED, 70), Jewellery.wearLevels("hueycoatl_hide_vambraces"));
        // The plain tiara and the pink skirt are head and legs armour with no bonuses.
        assertEquals(EquipmentDef.Slot.HEAD, ItemCatalog.get("tiara").equipment().slot());
        assertEquals(EquipmentDef.Slot.LEGS, ItemCatalog.get("pink_skirt").equipment().slot());
        assertEquals(EquipmentStats.NONE, ItemCatalog.get("tiara").equipment().stats());
        assertEquals(EquipmentStats.NONE, ItemCatalog.get("pink_skirt").equipment().stats());
    }

    /**
     * Uri checks what is worn. The magic axe hut step ("Equip only some flared trousers") needs a lockpick in OSRS only to
     * open the hut's door (23 Thieving; the STASH unit can't hold it), so it is not part of the worn requirement.
     */
    @Test
    void magicAxeHutAsksOnlyForFlaredTrousers() {
        Clue hut = null;
        for (Clue c : ClueScrolls.usableClues(Tier.MASTER, ClueScrolls.npcs())) {
            if (c.type() == StepType.EMOTE && c.text().startsWith("Clap in the magic axe hut")) {
                hut = c;
            }
        }
        assertNotNull(hut, "the magic axe hut step is a usable master step");
        assertEquals(List.of("flared_trousers"), ClueReqs.items(hut.req()));
        assertTrue(ClueReqs.groups(hut.req()).stream().anyMatch(g -> g.get(0).get(0).isSlotRule()), "every other slot empty");
    }
}
