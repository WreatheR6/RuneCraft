package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.PlaceDef;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Lost schematic lockboxes (OSRS Wiki, "Lockbox (island)" and "Small key (island)" pages, July 2026 dump): a lockbox on
 * one island opens with a small key dug up on another; it holds a lost schematic, and reading the schematic unlocks
 * building that boat part or facility. Without the key, the lockbox gives its wiki directions. Lockbox tiles are the
 * wiki map pins; key tiles are the pins of the key pages (approx: the exact dig tile is only shown in an image).
 *
 * <p>Two of the ten wiki lockboxes are left out: Anglers' Retreat (gale catcher, a facility RuneCraft does not have) and
 * Buccaneers' Haven (eternal brazier), whose key is dug up on the Tear of the Soul, an islet RuneCraft's sea has not
 * traced; the eternal brazier stays buildable without its schematic.
 */
public final class SailingLockboxes {
    private SailingLockboxes() {
    }

    /**
     * A lockbox: its island and tile, the Sailing level needed to dock there (here: to open it), the small key and the
     * tile it is dug up at, the directions, and the schematic inside with what it unlocks ({@code unlock}: a facility id,
     * or {@code PART_TYPE:tier} for a hull/sails/helm/keel tier).
     */
    public record Lockbox(String island, String islandName, int x, int y, int level, String keyIsland, String keyIslandName, int keyX,
                          int keyY, String directions, String schematic, String schematicName, String unlock) {
        public String keyId() {
            return "small_key_" + keyIsland;
        }

        public String placeId() {
            return "lockbox_" + island;
        }

        public String keyPlaceId() {
            return "small_key_" + keyIsland + "_spot";
        }

        /** The boat part type a part schematic unlocks, or null for a facility. */
        public SailingTraining.PartType partType() {
            int c = unlock.indexOf(':');
            return c < 0 ? null : SailingTraining.PartType.valueOf(unlock.substring(0, c));
        }

        public int partTier() {
            int c = unlock.indexOf(':');
            return c < 0 ? -1 : Integer.parseInt(unlock.substring(c + 1));
        }
    }

    public static final String BLOCK = "sailing_lockbox";

    /** The top tier (rosewood/dragon) of each boat part. */
    public static final int TOP_TIER = 6;

    public static final List<Lockbox> LOCKBOXES = List.of(
            new Lockbox("chinchompa_island", "Chinchompa Island", 1883, 3438, 42, "the_pandemonium", "the Pandemonium", 3050, 2995,
                    "On the Pandemonium, you'll dig on dry land, but make sure to move as far east as you can.",
                    "salvaging_station_schematic", "Salvaging station schematic", "salvaging_station"),
            new Lockbox("shimmering_atoll", "Shimmering Atoll", 1540, 2790, 49, "isle_of_bones", "the Isle of Bones", 2530, 2529,
                    "On the Isle of Bones, dig on the beach, but not on a rock, and not next to nor in line with any bones that they've"
                            + " dropped.", "rosewood_hull_schematic", "Rosewood hull schematic", "HULL:6"),
            new Lockbox("the_crown_jewel", "The Crown Jewel", 1765, 2652, 64, "minotaurs_rest", "Minotaurs' Rest", 1961, 3102,
                    "On Minotaurs' Rest, starting just outside the beastmasters door, move one pace south for each capybara and one"
                            + " pace west for each crab.", "rosewood_cargo_hold_schematic", "Rosewood cargo hold schematic", "rosewood_cargo_hold"),
            new Lockbox("lledrith_island", "Lledrith Island", 2106, 3164, 66, "ynysdail", "Ynysdail", 2223, 3467,
                    "On Ynysdail, dig where you dock, then figure out a safer place to bury the key.",
                    "dragon_salvaging_hook_schematic", "Dragon salvaging hook schematic", "dragon_salvaging_hook"),
            // The Buccaneers' Haven key lies in its laboratory (tile 2054, 10092); here on the haven's surface (approx).
            new Lockbox("deepfin_point", "Deepfin Point", 1945, 2796, 67, "buccaneers_haven", "Buccaneers' Haven", 2059, 3676,
                    "On Buccaneers' Haven, dig where you'd rest your head if you stayed.",
                    "dragon_cannon_schematic", "Dragon cannon schematic", "dragon_cannon"),
            new Lockbox("sunbleak_island", "Sunbleak Island", 2214, 2332, 72, "laguna_aurorae", "Laguna Aurorae", 1161, 2779,
                    "On Laguna Aurorae, step out your front door to face the sunrise, then take another step for each of your neighbours.",
                    "dragon_helm_schematic", "Dragon helm schematic", "HELM:6"),
            new Lockbox("brittle_isle", "Brittle Isle", 1938, 4067, 81, "charred_island", "Charred Island", 2646, 2419,
                    "On Charred Island, trace the seaweed up north, then walk six paces east and six paces north.",
                    "rosewood_cotton_sails_schematic", "Rosewood & cotton sails schematic", "SAILS:6"),
            new Lockbox("grimstone", "Grimstone", 2923, 4086, 87, "drumstick_isle", "Drumstick Isle", 2151, 3557,
                    "On Drumstick Isle, complete the square.", "dragon_keel_schematic", "Dragon keel schematic", "KEEL:6"));

    /** Small keys are dug up within this many tiles of their spot (approx: OSRS digs on one tile). */
    public static final int DIG_RADIUS = 2;

    public static Lockbox byIsland(String island) {
        for (Lockbox l : LOCKBOXES) {
            if (l.island().equals(island)) {
                return l;
            }
        }
        return null;
    }

    public static Lockbox bySchematic(String item) {
        for (Lockbox l : LOCKBOXES) {
            if (l.schematic().equals(item)) {
                return l;
            }
        }
        return null;
    }

    public static Lockbox byKey(String item) {
        for (Lockbox l : LOCKBOXES) {
            if (l.keyId().equals(item)) {
                return l;
            }
        }
        return null;
    }

    /** The lockbox nearest an OSRS tile (within 8 tiles), for the lockbox block. */
    public static Lockbox nearestLockbox(int x, int y) {
        Lockbox best = null;
        int bestD = 64;
        for (Lockbox l : LOCKBOXES) {
            int d = (l.x() - x) * (l.x() - x) + (l.y() - y) * (l.y() - y);
            if (d <= bestD) {
                bestD = d;
                best = l;
            }
        }
        return best;
    }

    /** The small key dug up at an OSRS tile (within {@link #DIG_RADIUS}), or null. */
    public static Lockbox keyAt(int x, int y) {
        for (Lockbox l : LOCKBOXES) {
            if (Math.abs(l.keyX() - x) <= DIG_RADIUS && Math.abs(l.keyY() - y) <= DIG_RADIUS) {
                return l;
            }
        }
        return null;
    }

    /** The schematic needed to build a facility, or null when it needs none. */
    public static Lockbox forFacility(String facility) {
        for (Lockbox l : LOCKBOXES) {
            if (l.unlock().equals(facility)) {
                return l;
            }
        }
        return null;
    }

    /** The schematic needed to build a boat part tier (on any boat), or null when it needs none. */
    public static Lockbox forPart(SailingTraining.PartType type, int tier) {
        for (Lockbox l : LOCKBOXES) {
            if (l.partType() == type && l.partTier() == tier) {
                return l;
            }
        }
        return null;
    }

    /**
     * Why a facility or part can't be built for want of its schematic, or null: {@code read} holds the schematic ids
     * the player has read.
     */
    public static String missingSchematic(Lockbox needed, Set<String> read) {
        if (needed == null || read.contains(needed.schematic())) {
            return null;
        }
        return "You need to read the " + needed.schematicName().toLowerCase(java.util.Locale.ROOT) + " before you can build that. It"
                + " is locked in a lockbox on " + needed.islandName() + ".";
    }

    public static void items(ItemSink s) {
        for (Lockbox l : LOCKBOXES) {
            s.add(l.keyId(), "Small key (" + l.keyIslandName().replace("the ", "") + ")", ItemDef.Kind.SIMPLE, "key", 0xB09A5A).value(1)
                    .stack(1).examine("A small key dug up on " + l.keyIslandName() + ".");
            s.add(l.schematic(), l.schematicName(), ItemDef.Kind.SIMPLE, "note", 0x9AB0D0).value(1).stack(1)
                    .examine("A lost schematic. Reading it would teach me how to build something for my boat.");
        }
    }

    /** Lockboxes and key dig spots as style-NONE places (the Sailing hooks put the lockbox block on its tile). */
    public static List<PlaceDef> places() {
        List<PlaceDef> out = new ArrayList<>();
        for (Lockbox l : LOCKBOXES) {
            out.add(PlaceDef.of(l.placeId(), "Lockbox (" + l.islandName() + ")", l.x(), l.y(), PlaceDef.Style.NONE, List.of()));
        }
        return out;
    }
}
