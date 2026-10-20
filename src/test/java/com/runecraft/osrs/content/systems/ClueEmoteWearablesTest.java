package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
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
 * jewellery couldn't be worn at all.
 */
class ClueEmoteWearablesTest {
    /**
     * Items emote clues ask for that the game can't wear yet, with why (the steps that need them can't be completed until
     * then; nothing may be added here to hide a new gap).
     */
    private static final Map<String, String> NOT_WEARABLE_YET = Map.of(
            "leather_gloves", "gloves outside the jewellery slots are carried, not worn (CraftingData)",
            "blue_dhide_vambraces", "vambraces are carried, not worn (CraftingData)",
            "black_dhide_vambraces", "vambraces are carried, not worn (CraftingData)",
            "hueycoatl_hide_vambraces", "boss accessory not wired to a slot (BossItems.ACCESSORIES)",
            "red_cape", "plain coloured capes have no worn slot",
            "pink_skirt", "the quest item has no legs equipment",
            "tiara", "the unenchanted tiara has no head equipment",
            "lockpick", "the magic axe hut step carries the lockpick; the requirement lists it with the worn items");

    static boolean wearable(String id) {
        ItemDef d = ItemCatalog.get(id);
        return d != null && (Jewellery.worn(id) != null || d.equipment() != null || QuestBook.get().gear.containsKey(id));
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
        unwearable.removeAll(NOT_WEARABLE_YET.keySet());
        assertTrue(unwearable.isEmpty(), "emote clue items the game can't wear: " + unwearable);
    }
}
