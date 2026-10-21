package com.runecraft.osrs.content.minigames;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Mahogany Homes contract layouts (OSRS Wiki "Mahogany Homes/Mechanics"): each homeowner has two to four fixed layouts
 * per tier, each a set of furniture hotspots (varbit slot 0-7) to build (remove, then build with planks) or repair (planks
 * or one steel bar). {@code MahoganyHomesLayoutsTest} diffs this table and its XP against the wiki snapshot
 * {@code src/test/resources/osrs/minigames/mahogany_homes_layouts.tsv}.
 */
public final class MahoganyHomesLayouts {
    private MahoganyHomesLayouts() {
    }

    /** A furniture hotspot: its slot in the house (0-7), name, materials and whether it is repaired (else rebuilt). */
    public record Hotspot(int slot, String name, int count, boolean steelBar, boolean repair) {
    }

    /** One layout: tier (0 Beginner - 3 Expert), city, homeowner and hotspots. */
    public record Layout(int tier, String region, String owner, List<Hotspot> hotspots) {
        public int planks() {
            return hotspots.stream().filter(h -> !h.steelBar()).mapToInt(Hotspot::count).sum();
        }

        public int bars() {
            return hotspots.stream().filter(Hotspot::steelBar).mapToInt(Hotspot::count).sum();
        }

        /** Construction XP for the furniture (not the completion bonus). */
        public double furnitureXp() {
            double xp = 0;
            for (Hotspot h : hotspots) {
                xp += h.count() * MinigameRules.MahoganyHomes.hotspotXp(tier, h.steelBar(), h.repair());
            }
            return xp;
        }

        /** Everything the contract pays in XP: the furniture and the completion bonus. */
        public double totalXp() {
            return furnitureXp() + MinigameRules.MahoganyHomes.COMPLETION_XP[tier];
        }
    }

    private static final String DATA = """
            BEGINNER|Ardougne|Jess|0:Drawers (W):2:PLANK:BUILD;1:Drawers (E):2:PLANK:BUILD;2:Cabinet (W):2:PLANK:BUILD;3:Cabinet (E):2:PLANK:BUILD;6:Grandfather clock:1:PLANK:REPAIR;7:Bathtub:1:BAR:REPAIR
            BEGINNER|Ardougne|Jess|2:Cabinet (W):2:PLANK:BUILD;3:Cabinet (E):2:PLANK:BUILD;4:Bed:3:PLANK:BUILD;5:Table:3:PLANK:BUILD;6:Grandfather clock:1:PLANK:REPAIR
            BEGINNER|Ardougne|Jess|0:Drawers (W):2:PLANK:BUILD;1:Drawers (E):2:PLANK:BUILD;4:Bed:3:PLANK:BUILD;5:Table:3:PLANK:BUILD;6:Grandfather clock:1:PLANK:REPAIR
            BEGINNER|Ardougne|Noella|0:Dresser:2:PLANK:BUILD;1:Cupboard:2:PLANK:BUILD;2:Hat stand:1:PLANK:REPAIR;4:Drawers:2:PLANK:BUILD;5:Table (W):3:PLANK:BUILD;7:Grandfather clock:1:PLANK:REPAIR
            BEGINNER|Ardougne|Noella|0:Dresser:2:PLANK:BUILD;1:Cupboard:2:PLANK:BUILD;3:Mirror:1:PLANK:REPAIR;5:Table (W):3:PLANK:BUILD;6:Table (E):3:PLANK:BUILD;7:Grandfather clock:1:PLANK:REPAIR
            BEGINNER|Ardougne|Noella|2:Hat stand:1:PLANK:REPAIR;3:Mirror:1:PLANK:REPAIR;4:Drawers:2:PLANK:BUILD;5:Table (W):3:PLANK:BUILD;6:Table (E):3:PLANK:BUILD;7:Grandfather clock:1:PLANK:REPAIR
            BEGINNER|Ardougne|Ross|0:Range:1:BAR:REPAIR;1:Drawers (N):2:PLANK:BUILD;2:Drawers (S):2:PLANK:BUILD;3:Double bed:3:PLANK:BUILD;4:Hat stand:1:PLANK:REPAIR
            BEGINNER|Ardougne|Ross|1:Drawers (N):2:PLANK:BUILD;2:Drawers (S):2:PLANK:BUILD;3:Double bed:3:PLANK:BUILD;4:Hat stand:1:PLANK:REPAIR;5:Bed:2:PLANK:BUILD;6:Mirror:1:PLANK:REPAIR
            BEGINNER|Ardougne|Ross|1:Drawers (N):2:PLANK:BUILD;2:Drawers (S):2:PLANK:BUILD;4:Hat stand:1:PLANK:REPAIR;5:Bed:2:PLANK:BUILD;6:Mirror:1:PLANK:REPAIR
            BEGINNER|Falador|Larry|1:Drawers (N):2:PLANK:BUILD;2:Drawers (S):2:PLANK:BUILD;3:Table (D):3:PLANK:BUILD;4:Hat stand:1:PLANK:REPAIR;5:Table (U):3:PLANK:BUILD
            BEGINNER|Falador|Larry|0:Range:1:BAR:REPAIR;1:Drawers (N):2:PLANK:BUILD;2:Drawers (S):2:PLANK:BUILD;3:Table (D):3:PLANK:BUILD;4:Hat stand:1:PLANK:REPAIR
            BEGINNER|Falador|Larry|1:Drawers (N):2:PLANK:BUILD;2:Drawers (S):2:PLANK:BUILD;3:Table (D):3:PLANK:BUILD;4:Hat stand:1:PLANK:REPAIR;5:Table (U):3:PLANK:BUILD;6:Grandfather clock:1:PLANK:REPAIR
            BEGINNER|Falador|Norman|1:Grandfather clock:1:PLANK:REPAIR;2:Table:3:PLANK:BUILD;3:Double bed:2:PLANK:BUILD;4:Bookshelf:2:PLANK:BUILD;5:Drawers:3:PLANK:BUILD
            BEGINNER|Falador|Norman|0:Range:1:BAR:REPAIR;1:Grandfather clock:1:PLANK:REPAIR;2:Table:3:PLANK:BUILD;3:Double bed:2:PLANK:BUILD;5:Drawers:3:PLANK:BUILD;6:Small table:2:PLANK:BUILD
            BEGINNER|Falador|Tau|0:Sink:1:BAR:REPAIR;2:Table (N):3:PLANK:BUILD;3:Cupboard:2:PLANK:BUILD;4:Shelves (W):2:PLANK:BUILD;5:Shelves (E):2:PLANK:BUILD
            BEGINNER|Falador|Tau|0:Sink:1:BAR:REPAIR;1:Table (S):3:PLANK:BUILD;3:Cupboard:2:PLANK:BUILD;4:Shelves (W):2:PLANK:BUILD;5:Shelves (E):2:PLANK:BUILD;6:Hat stand:1:PLANK:REPAIR
            BEGINNER|Falador|Tau|0:Sink:1:BAR:REPAIR;1:Table (S):3:PLANK:BUILD;3:Cupboard:2:PLANK:BUILD;4:Shelves (W):2:PLANK:BUILD;6:Hat stand:1:PLANK:REPAIR
            BEGINNER|Falador|Tau|1:Table (S):3:PLANK:BUILD;2:Table (N):3:PLANK:BUILD;3:Cupboard:2:PLANK:BUILD;4:Shelves (W):2:PLANK:BUILD;5:Shelves (E):2:PLANK:BUILD
            BEGINNER|Hosidius|Barbara|0:Grandfather clock:1:BAR:REPAIR;2:Table:2:PLANK:BUILD;3:Drawers:3:PLANK:BUILD;4:Bed:2:PLANK:BUILD
            BEGINNER|Hosidius|Barbara|0:Grandfather clock:1:BAR:REPAIR;1:Range:1:PLANK:REPAIR;5:Chair (N):1:PLANK:BUILD;6:Chair (S):1:PLANK:BUILD
            BEGINNER|Hosidius|Barbara|1:Range:1:PLANK:REPAIR;2:Table:2:PLANK:BUILD;3:Drawers:3:PLANK:BUILD;4:Bed:2:PLANK:BUILD
            BEGINNER|Hosidius|Leela|3:Double bed:3:PLANK:BUILD;4:Table (U):3:PLANK:BUILD;5:Mirror:1:PLANK:REPAIR;6:Cupboard:2:PLANK:BUILD
            BEGINNER|Hosidius|Leela|0:Small table (N):2:PLANK:BUILD;1:Small table (S):2:PLANK:BUILD;2:Sink:1:BAR:REPAIR;3:Double bed:3:PLANK:BUILD;5:Mirror:1:PLANK:REPAIR
            BEGINNER|Hosidius|Leela|2:Sink:1:BAR:REPAIR;3:Double bed:3:PLANK:BUILD;4:Table (U):3:PLANK:BUILD;5:Mirror:1:PLANK:REPAIR;6:Cupboard:2:PLANK:BUILD
            BEGINNER|Hosidius|Mariah|0:Table:3:PLANK:BUILD;1:Sink:1:BAR:REPAIR;2:Cooking shelves:2:PLANK:BUILD;3:Cupboard:2:PLANK:BUILD;4:Bed:2:PLANK:BUILD;5:Small table (W):2:PLANK:BUILD
            BEGINNER|Hosidius|Mariah|3:Cupboard:2:PLANK:BUILD;4:Bed:2:PLANK:BUILD;5:Small table (W):2:PLANK:BUILD;6:Small table (E):2:PLANK:BUILD;7:Hat stand:1:PLANK:REPAIR
            BEGINNER|Hosidius|Mariah|1:Sink:1:BAR:REPAIR;2:Cooking shelves:2:PLANK:BUILD;3:Cupboard:2:PLANK:BUILD;4:Bed:2:PLANK:BUILD;7:Hat stand:1:PLANK:REPAIR
            BEGINNER|Varrock|Bob|0:Large table:4:PLANK:BUILD;2:Cabinet (S):2:PLANK:BUILD;4:Bookcase (S):2:PLANK:BUILD;5:Bookcase (N):2:PLANK:BUILD;6:Wardrobe:2:PLANK:BUILD;7:Drawers:2:PLANK:BUILD
            BEGINNER|Varrock|Bob|0:Large table:4:PLANK:BUILD;1:Grandfather clock:1:PLANK:REPAIR;2:Cabinet (S):2:PLANK:BUILD;3:Cabinet (N):2:PLANK:BUILD;6:Wardrobe:2:PLANK:BUILD;7:Drawers:2:PLANK:BUILD
            BEGINNER|Varrock|Bob|0:Large table:4:PLANK:BUILD;1:Grandfather clock:1:PLANK:REPAIR;2:Cabinet (S):2:PLANK:BUILD;3:Cabinet (N):2:PLANK:BUILD;4:Bookcase (S):2:PLANK:BUILD;5:Bookcase (N):2:PLANK:BUILD
            BEGINNER|Varrock|Jeff|2:Shelves:2:PLANK:BUILD;3:Bed:3:PLANK:BUILD;4:Drawers:2:PLANK:BUILD;5:Dresser:2:PLANK:BUILD;6:Mirror:1:PLANK:REPAIR;7:Chair:1:PLANK:BUILD
            BEGINNER|Varrock|Jeff|0:Table:3:PLANK:BUILD;1:Bookcase:2:PLANK:BUILD;2:Shelves:2:PLANK:BUILD;5:Dresser:2:PLANK:BUILD;6:Mirror:1:PLANK:REPAIR;7:Chair:1:PLANK:BUILD
            BEGINNER|Varrock|Jeff|0:Table:3:PLANK:BUILD;1:Bookcase:2:PLANK:BUILD;2:Shelves:2:PLANK:BUILD;3:Bed:3:PLANK:BUILD;4:Drawers:2:PLANK:BUILD;6:Mirror:1:PLANK:REPAIR
            BEGINNER|Varrock|Sarah|0:Table:3:PLANK:BUILD;1:Bed:2:PLANK:BUILD;2:Dresser:2:PLANK:BUILD;3:Small table:2:PLANK:BUILD;4:Range:1:BAR:REPAIR;5:Shelves:2:PLANK:BUILD
            BEGINNER|Varrock|Sarah|0:Table:3:PLANK:BUILD;1:Bed:2:PLANK:BUILD;2:Dresser:2:PLANK:BUILD;3:Small table:2:PLANK:BUILD;5:Shelves:2:PLANK:BUILD
            NOVICE|Ardougne|Jess|0:Drawers (W):2:PLANK:BUILD;1:Drawers (E):2:PLANK:BUILD;4:Bed:3:PLANK:BUILD;5:Table:3:PLANK:BUILD;6:Grandfather clock:1:PLANK:REPAIR
            NOVICE|Ardougne|Jess|2:Cabinet (W):2:PLANK:BUILD;3:Cabinet (E):2:PLANK:BUILD;4:Bed:3:PLANK:BUILD;5:Table:3:PLANK:BUILD;6:Grandfather clock:1:PLANK:REPAIR
            NOVICE|Ardougne|Jess|0:Drawers (W):2:PLANK:BUILD;1:Drawers (E):2:PLANK:BUILD;2:Cabinet (W):2:PLANK:BUILD;3:Cabinet (E):2:PLANK:BUILD;6:Grandfather clock:1:PLANK:REPAIR;7:Bathtub:1:BAR:REPAIR
            NOVICE|Ardougne|Noella|0:Dresser:2:PLANK:BUILD;1:Cupboard:2:PLANK:BUILD;2:Hat stand:1:PLANK:REPAIR;4:Drawers:2:PLANK:BUILD;5:Table (W):3:PLANK:BUILD;7:Grandfather clock:1:PLANK:REPAIR
            NOVICE|Ardougne|Noella|2:Hat stand:1:PLANK:REPAIR;3:Mirror:1:PLANK:REPAIR;4:Drawers:2:PLANK:BUILD;5:Table (W):3:PLANK:BUILD;6:Table (E):3:PLANK:BUILD;7:Grandfather clock:1:PLANK:REPAIR
            NOVICE|Ardougne|Noella|0:Dresser:2:PLANK:BUILD;1:Cupboard:2:PLANK:BUILD;3:Mirror:1:PLANK:REPAIR;5:Table (W):3:PLANK:BUILD;6:Table (E):3:PLANK:BUILD;7:Grandfather clock:1:PLANK:REPAIR
            NOVICE|Ardougne|Ross|1:Drawers (N):2:PLANK:BUILD;2:Drawers (S):2:PLANK:BUILD;3:Double bed:3:PLANK:BUILD;4:Hat stand:1:PLANK:REPAIR;5:Bed:2:PLANK:BUILD;6:Mirror:1:PLANK:REPAIR
            NOVICE|Ardougne|Ross|0:Range:1:BAR:REPAIR;1:Drawers (N):2:PLANK:BUILD;2:Drawers (S):2:PLANK:BUILD;3:Double bed:3:PLANK:BUILD;4:Hat stand:1:PLANK:REPAIR
            NOVICE|Ardougne|Ross|1:Drawers (N):2:PLANK:BUILD;2:Drawers (S):2:PLANK:BUILD;4:Hat stand:1:PLANK:REPAIR;5:Bed:2:PLANK:BUILD;6:Mirror:1:PLANK:REPAIR
            NOVICE|Falador|Larry|1:Drawers (N):2:PLANK:BUILD;2:Drawers (S):2:PLANK:BUILD;3:Table (D):3:PLANK:BUILD;4:Hat stand:1:PLANK:REPAIR;5:Table (U):3:PLANK:BUILD;6:Grandfather clock:1:PLANK:REPAIR
            NOVICE|Falador|Larry|0:Range:1:BAR:REPAIR;1:Drawers (N):2:PLANK:BUILD;2:Drawers (S):2:PLANK:BUILD;3:Table (D):3:PLANK:BUILD;4:Hat stand:1:PLANK:REPAIR
            NOVICE|Falador|Larry|1:Drawers (N):2:PLANK:BUILD;2:Drawers (S):2:PLANK:BUILD;3:Table (D):3:PLANK:BUILD;4:Hat stand:1:PLANK:REPAIR;5:Table (U):3:PLANK:BUILD
            NOVICE|Falador|Norman|0:Range:1:BAR:REPAIR;2:Table:3:PLANK:BUILD;3:Double bed:2:PLANK:BUILD;4:Bookshelf:2:PLANK:BUILD;5:Drawers:3:PLANK:BUILD
            NOVICE|Falador|Norman|0:Range:1:BAR:REPAIR;1:Grandfather clock:1:PLANK:REPAIR;2:Table:3:PLANK:BUILD;3:Double bed:2:PLANK:BUILD;5:Drawers:3:PLANK:BUILD;6:Small table:2:PLANK:BUILD
            NOVICE|Falador|Norman|1:Grandfather clock:1:PLANK:REPAIR;2:Table:3:PLANK:BUILD;3:Double bed:2:PLANK:BUILD;4:Bookshelf:2:PLANK:BUILD;5:Drawers:3:PLANK:BUILD
            NOVICE|Falador|Tau|1:Table (S):3:PLANK:BUILD;2:Table (N):3:PLANK:BUILD;3:Cupboard:2:PLANK:BUILD;4:Shelves (W):2:PLANK:BUILD;5:Shelves (E):2:PLANK:BUILD
            NOVICE|Falador|Tau|0:Sink:1:BAR:REPAIR;1:Table (S):3:PLANK:BUILD;3:Cupboard:2:PLANK:BUILD;4:Shelves (W):2:PLANK:BUILD;6:Hat stand:1:PLANK:REPAIR
            NOVICE|Falador|Tau|0:Sink:1:BAR:REPAIR;1:Table (S):3:PLANK:BUILD;3:Cupboard:2:PLANK:BUILD;4:Shelves (W):2:PLANK:BUILD;5:Shelves (E):2:PLANK:BUILD;6:Hat stand:1:PLANK:REPAIR
            NOVICE|Hosidius|Barbara|1:Range:1:PLANK:REPAIR;2:Table:2:PLANK:BUILD;3:Drawers:3:PLANK:BUILD;4:Bed:2:PLANK:BUILD
            NOVICE|Hosidius|Barbara|0:Grandfather clock:1:BAR:REPAIR;1:Range:1:PLANK:REPAIR;5:Chair (N):1:PLANK:BUILD;6:Chair (S):1:PLANK:BUILD
            NOVICE|Hosidius|Barbara|0:Grandfather clock:1:BAR:REPAIR;2:Table:2:PLANK:BUILD;3:Drawers:3:PLANK:BUILD;4:Bed:2:PLANK:BUILD
            NOVICE|Hosidius|Leela|2:Sink:1:BAR:REPAIR;3:Double bed:3:PLANK:BUILD;4:Table (U):3:PLANK:BUILD;5:Mirror:1:PLANK:REPAIR;6:Cupboard:2:PLANK:BUILD
            NOVICE|Hosidius|Leela|3:Double bed:3:PLANK:BUILD;4:Table (U):3:PLANK:BUILD;5:Mirror:1:PLANK:REPAIR;6:Cupboard:2:PLANK:BUILD
            NOVICE|Hosidius|Leela|0:Small table (N):2:PLANK:BUILD;1:Small table (S):2:PLANK:BUILD;2:Sink:1:BAR:REPAIR;3:Double bed:3:PLANK:BUILD;5:Mirror:1:PLANK:REPAIR
            NOVICE|Hosidius|Mariah|0:Table:3:PLANK:BUILD;1:Sink:1:BAR:REPAIR;2:Cooking shelves:2:PLANK:BUILD;3:Cupboard:2:PLANK:BUILD;4:Bed:2:PLANK:BUILD;5:Small table (W):2:PLANK:BUILD
            NOVICE|Hosidius|Mariah|3:Cupboard:2:PLANK:BUILD;4:Bed:2:PLANK:BUILD;5:Small table (W):2:PLANK:BUILD;6:Small table (E):2:PLANK:BUILD;7:Hat stand:1:PLANK:REPAIR
            NOVICE|Hosidius|Mariah|1:Sink:1:BAR:REPAIR;2:Cooking shelves:2:PLANK:BUILD;3:Cupboard:2:PLANK:BUILD;4:Bed:2:PLANK:BUILD;7:Hat stand:1:PLANK:REPAIR
            NOVICE|Varrock|Bob|0:Large table:4:PLANK:BUILD;1:Grandfather clock:1:PLANK:REPAIR;2:Cabinet (S):2:PLANK:BUILD;3:Cabinet (N):2:PLANK:BUILD;4:Bookcase (S):2:PLANK:BUILD;5:Bookcase (N):2:PLANK:BUILD
            NOVICE|Varrock|Bob|0:Large table:4:PLANK:BUILD;2:Cabinet (S):2:PLANK:BUILD;4:Bookcase (S):2:PLANK:BUILD;5:Bookcase (N):2:PLANK:BUILD;6:Wardrobe:2:PLANK:BUILD;7:Drawers:2:PLANK:BUILD
            NOVICE|Varrock|Bob|0:Large table:4:PLANK:BUILD;1:Grandfather clock:1:PLANK:REPAIR;2:Cabinet (S):2:PLANK:BUILD;3:Cabinet (N):2:PLANK:BUILD;6:Wardrobe:2:PLANK:BUILD;7:Drawers:2:PLANK:BUILD
            NOVICE|Varrock|Jeff|2:Shelves:2:PLANK:BUILD;3:Bed:3:PLANK:BUILD;4:Drawers:2:PLANK:BUILD;5:Dresser:2:PLANK:BUILD;6:Mirror:1:PLANK:REPAIR;7:Chair:1:PLANK:BUILD
            NOVICE|Varrock|Jeff|0:Table:3:PLANK:BUILD;1:Bookcase:2:PLANK:BUILD;2:Shelves:2:PLANK:BUILD;3:Bed:3:PLANK:BUILD;4:Drawers:2:PLANK:BUILD;6:Mirror:1:PLANK:REPAIR
            NOVICE|Varrock|Jeff|0:Table:3:PLANK:BUILD;1:Bookcase:2:PLANK:BUILD;2:Shelves:2:PLANK:BUILD;5:Dresser:2:PLANK:BUILD;6:Mirror:1:PLANK:REPAIR;7:Chair:1:PLANK:BUILD
            NOVICE|Varrock|Sarah|0:Table:3:PLANK:BUILD;1:Bed:2:PLANK:BUILD;2:Dresser:2:PLANK:BUILD;3:Small table:2:PLANK:BUILD;4:Range:1:BAR:REPAIR;5:Shelves:2:PLANK:BUILD
            NOVICE|Varrock|Sarah|0:Table:3:PLANK:BUILD;1:Bed:2:PLANK:BUILD;2:Dresser:2:PLANK:BUILD;3:Small table:2:PLANK:BUILD;5:Shelves:2:PLANK:BUILD
            ADEPT|Ardougne|Jess|0:Drawers (W):2:PLANK:BUILD;1:Drawers (E):2:PLANK:BUILD;2:Cabinet (W):2:PLANK:BUILD;3:Cabinet (E):2:PLANK:BUILD;4:Bed:3:PLANK:BUILD;6:Grandfather clock:1:PLANK:REPAIR
            ADEPT|Ardougne|Jess|0:Drawers (W):2:PLANK:BUILD;1:Drawers (E):2:PLANK:BUILD;2:Cabinet (W):2:PLANK:BUILD;3:Cabinet (E):2:PLANK:BUILD;4:Bed:3:PLANK:BUILD;5:Table:3:PLANK:BUILD;6:Grandfather clock:1:PLANK:REPAIR;7:Bathtub:1:BAR:REPAIR
            ADEPT|Ardougne|Jess|0:Drawers (W):2:PLANK:BUILD;1:Drawers (E):2:PLANK:BUILD;2:Cabinet (W):2:PLANK:BUILD;3:Cabinet (E):2:PLANK:BUILD;4:Bed:3:PLANK:BUILD;6:Grandfather clock:1:PLANK:REPAIR;7:Bathtub:1:BAR:REPAIR
            ADEPT|Ardougne|Noella|0:Dresser:2:PLANK:BUILD;1:Cupboard:2:PLANK:BUILD;3:Mirror:1:PLANK:REPAIR;4:Drawers:2:PLANK:BUILD;5:Table (W):3:PLANK:BUILD;6:Table (E):3:PLANK:BUILD
            ADEPT|Ardougne|Noella|0:Dresser:2:PLANK:BUILD;1:Cupboard:2:PLANK:BUILD;2:Hat stand:1:PLANK:REPAIR;3:Mirror:1:PLANK:REPAIR;4:Drawers:2:PLANK:BUILD;5:Table (W):3:PLANK:BUILD;6:Table (E):3:PLANK:BUILD;7:Grandfather clock:1:PLANK:REPAIR
            ADEPT|Ardougne|Noella|0:Dresser:2:PLANK:BUILD;1:Cupboard:2:PLANK:BUILD;2:Hat stand:1:PLANK:REPAIR;3:Mirror:1:PLANK:REPAIR;4:Drawers:2:PLANK:BUILD;5:Table (W):3:PLANK:BUILD
            ADEPT|Ardougne|Ross|0:Range:1:BAR:REPAIR;1:Drawers (N):2:PLANK:BUILD;2:Drawers (S):2:PLANK:BUILD;4:Hat stand:1:PLANK:REPAIR;5:Bed:2:PLANK:BUILD;6:Mirror:1:PLANK:REPAIR
            ADEPT|Ardougne|Ross|0:Range:1:BAR:REPAIR;1:Drawers (N):2:PLANK:BUILD;2:Drawers (S):2:PLANK:BUILD;3:Double bed:3:PLANK:BUILD;4:Hat stand:1:PLANK:REPAIR;5:Bed:2:PLANK:BUILD;6:Mirror:1:PLANK:REPAIR
            ADEPT|Ardougne|Ross|0:Range:1:BAR:REPAIR;1:Drawers (N):2:PLANK:BUILD;2:Drawers (S):2:PLANK:BUILD;3:Double bed:3:PLANK:BUILD;4:Hat stand:1:PLANK:REPAIR
            ADEPT|Falador|Larry|1:Drawers (N):2:PLANK:BUILD;2:Drawers (S):2:PLANK:BUILD;3:Table (D):3:PLANK:BUILD;4:Hat stand:1:PLANK:REPAIR;5:Table (U):3:PLANK:BUILD
            ADEPT|Falador|Larry|0:Range:1:BAR:REPAIR;1:Drawers (N):2:PLANK:BUILD;2:Drawers (S):2:PLANK:BUILD;4:Hat stand:1:PLANK:REPAIR;5:Table (U):3:PLANK:BUILD;6:Grandfather clock:1:PLANK:REPAIR
            ADEPT|Falador|Larry|0:Range:1:BAR:REPAIR;1:Drawers (N):2:PLANK:BUILD;2:Drawers (S):2:PLANK:BUILD;3:Table (D):3:PLANK:BUILD;4:Hat stand:1:PLANK:REPAIR;5:Table (U):3:PLANK:BUILD;6:Grandfather clock:1:PLANK:REPAIR
            ADEPT|Falador|Norman|0:Range:1:BAR:REPAIR;1:Grandfather clock:1:PLANK:REPAIR;2:Table:3:PLANK:BUILD;3:Double bed:2:PLANK:BUILD;4:Bookshelf:2:PLANK:BUILD;5:Drawers:3:PLANK:BUILD;6:Small table:2:PLANK:BUILD
            ADEPT|Falador|Norman|1:Grandfather clock:1:PLANK:REPAIR;2:Table:3:PLANK:BUILD;3:Double bed:2:PLANK:BUILD;5:Drawers:3:PLANK:BUILD;6:Small table:2:PLANK:BUILD
            ADEPT|Falador|Norman|0:Range:1:BAR:REPAIR;2:Table:3:PLANK:BUILD;4:Bookshelf:2:PLANK:BUILD;5:Drawers:3:PLANK:BUILD;6:Small table:2:PLANK:BUILD
            ADEPT|Falador|Tau|1:Table (S):3:PLANK:BUILD;2:Table (N):3:PLANK:BUILD;3:Cupboard:2:PLANK:BUILD;4:Shelves (W):2:PLANK:BUILD;5:Shelves (E):2:PLANK:BUILD
            ADEPT|Falador|Tau|1:Table (S):3:PLANK:BUILD;2:Table (N):3:PLANK:BUILD;3:Cupboard:2:PLANK:BUILD;4:Shelves (W):2:PLANK:BUILD;5:Shelves (E):2:PLANK:BUILD;6:Hat stand:1:PLANK:REPAIR
            ADEPT|Falador|Tau|0:Sink:1:BAR:REPAIR;1:Table (S):3:PLANK:BUILD;3:Cupboard:2:PLANK:BUILD;4:Shelves (W):2:PLANK:BUILD;5:Shelves (E):2:PLANK:BUILD
            ADEPT|Hosidius|Barbara|1:Range:1:PLANK:REPAIR;2:Table:2:PLANK:BUILD;3:Drawers:3:PLANK:BUILD;4:Bed:2:PLANK:BUILD;5:Chair (N):1:PLANK:BUILD;6:Chair (S):1:PLANK:BUILD
            ADEPT|Hosidius|Barbara|0:Grandfather clock:1:BAR:REPAIR;2:Table:2:PLANK:BUILD;3:Drawers:3:PLANK:BUILD;4:Bed:2:PLANK:BUILD;5:Chair (N):1:PLANK:BUILD;6:Chair (S):1:PLANK:BUILD
            ADEPT|Hosidius|Barbara|0:Grandfather clock:1:BAR:REPAIR;1:Range:1:PLANK:REPAIR;2:Table:2:PLANK:BUILD;3:Drawers:3:PLANK:BUILD;4:Bed:2:PLANK:BUILD;5:Chair (N):1:PLANK:BUILD;6:Chair (S):1:PLANK:BUILD
            ADEPT|Hosidius|Leela|0:Small table (N):2:PLANK:BUILD;1:Small table (S):2:PLANK:BUILD;3:Double bed:3:PLANK:BUILD;5:Mirror:1:PLANK:REPAIR;6:Cupboard:2:PLANK:BUILD
            ADEPT|Hosidius|Leela|0:Small table (N):2:PLANK:BUILD;1:Small table (S):2:PLANK:BUILD;2:Sink:1:BAR:REPAIR;3:Double bed:3:PLANK:BUILD;5:Mirror:1:PLANK:REPAIR;6:Cupboard:2:PLANK:BUILD
            ADEPT|Hosidius|Leela|2:Sink:1:BAR:REPAIR;3:Double bed:3:PLANK:BUILD;4:Table (U):3:PLANK:BUILD;5:Mirror:1:PLANK:REPAIR;6:Cupboard:2:PLANK:BUILD
            ADEPT|Hosidius|Mariah|0:Table:3:PLANK:BUILD;2:Cooking shelves:2:PLANK:BUILD;3:Cupboard:2:PLANK:BUILD;5:Small table (W):2:PLANK:BUILD;6:Small table (E):2:PLANK:BUILD;7:Hat stand:1:PLANK:REPAIR
            ADEPT|Hosidius|Mariah|0:Table:3:PLANK:BUILD;1:Sink:1:BAR:REPAIR;2:Cooking shelves:2:PLANK:BUILD;3:Cupboard:2:PLANK:BUILD;4:Bed:2:PLANK:BUILD;5:Small table (W):2:PLANK:BUILD;6:Small table (E):2:PLANK:BUILD;7:Hat stand:1:PLANK:REPAIR
            ADEPT|Hosidius|Mariah|0:Table:3:PLANK:BUILD;1:Sink:1:BAR:REPAIR;2:Cooking shelves:2:PLANK:BUILD;3:Cupboard:2:PLANK:BUILD;4:Bed:2:PLANK:BUILD;5:Small table (W):2:PLANK:BUILD
            ADEPT|Varrock|Bob|0:Large table:4:PLANK:BUILD;2:Cabinet (S):2:PLANK:BUILD;4:Bookcase (S):2:PLANK:BUILD;5:Bookcase (N):2:PLANK:BUILD;6:Wardrobe:2:PLANK:BUILD;7:Drawers:2:PLANK:BUILD
            ADEPT|Varrock|Bob|0:Large table:4:PLANK:BUILD;1:Grandfather clock:1:PLANK:REPAIR;2:Cabinet (S):2:PLANK:BUILD;3:Cabinet (N):2:PLANK:BUILD;4:Bookcase (S):2:PLANK:BUILD;5:Bookcase (N):2:PLANK:BUILD;6:Wardrobe:2:PLANK:BUILD;7:Drawers:2:PLANK:BUILD
            ADEPT|Varrock|Bob|0:Large table:4:PLANK:BUILD;1:Grandfather clock:1:PLANK:REPAIR;2:Cabinet (S):2:PLANK:BUILD;3:Cabinet (N):2:PLANK:BUILD;6:Wardrobe:2:PLANK:BUILD;7:Drawers:2:PLANK:BUILD
            ADEPT|Varrock|Jeff|0:Table:3:PLANK:BUILD;1:Bookcase:2:PLANK:BUILD;2:Shelves:2:PLANK:BUILD;3:Bed:3:PLANK:BUILD;4:Drawers:2:PLANK:BUILD;5:Dresser:2:PLANK:BUILD;6:Mirror:1:PLANK:REPAIR;7:Chair:1:PLANK:BUILD
            ADEPT|Varrock|Jeff|0:Table:3:PLANK:BUILD;1:Bookcase:2:PLANK:BUILD;2:Shelves:2:PLANK:BUILD;5:Dresser:2:PLANK:BUILD;6:Mirror:1:PLANK:REPAIR;7:Chair:1:PLANK:BUILD
            ADEPT|Varrock|Jeff|0:Table:3:PLANK:BUILD;1:Bookcase:2:PLANK:BUILD;2:Shelves:2:PLANK:BUILD;3:Bed:3:PLANK:BUILD;4:Drawers:2:PLANK:BUILD;6:Mirror:1:PLANK:REPAIR
            ADEPT|Varrock|Sarah|0:Table:3:PLANK:BUILD;1:Bed:2:PLANK:BUILD;2:Dresser:2:PLANK:BUILD;3:Small table:2:PLANK:BUILD;4:Range:1:BAR:REPAIR;5:Shelves:2:PLANK:BUILD
            ADEPT|Varrock|Sarah|0:Table:3:PLANK:BUILD;1:Bed:2:PLANK:BUILD;2:Dresser:2:PLANK:BUILD;3:Small table:2:PLANK:BUILD;5:Shelves:2:PLANK:BUILD
            EXPERT|Ardougne|Jess|0:Drawers (W):2:PLANK:BUILD;1:Drawers (E):2:PLANK:BUILD;2:Cabinet (W):2:PLANK:BUILD;3:Cabinet (E):2:PLANK:BUILD;4:Bed:3:PLANK:BUILD;5:Table:3:PLANK:BUILD;7:Bathtub:1:BAR:REPAIR
            EXPERT|Ardougne|Jess|0:Drawers (W):2:PLANK:BUILD;1:Drawers (E):2:PLANK:BUILD;2:Cabinet (W):2:PLANK:BUILD;3:Cabinet (E):2:PLANK:BUILD;4:Bed:3:PLANK:BUILD;5:Table:3:PLANK:BUILD;6:Grandfather clock:1:PLANK:REPAIR;7:Bathtub:1:BAR:REPAIR
            EXPERT|Ardougne|Jess|0:Drawers (W):2:PLANK:BUILD;1:Drawers (E):2:PLANK:BUILD;2:Cabinet (W):2:PLANK:BUILD;3:Cabinet (E):2:PLANK:BUILD;4:Bed:3:PLANK:BUILD;5:Table:3:PLANK:BUILD;6:Grandfather clock:1:PLANK:REPAIR
            EXPERT|Ardougne|Noella|0:Dresser:2:PLANK:BUILD;1:Cupboard:2:PLANK:BUILD;3:Mirror:1:PLANK:REPAIR;4:Drawers:2:PLANK:BUILD;5:Table (W):3:PLANK:BUILD;6:Table (E):3:PLANK:BUILD;7:Grandfather clock:1:PLANK:REPAIR
            EXPERT|Ardougne|Noella|0:Dresser:2:PLANK:BUILD;1:Cupboard:2:PLANK:BUILD;2:Hat stand:1:PLANK:REPAIR;3:Mirror:1:PLANK:REPAIR;4:Drawers:2:PLANK:BUILD;5:Table (W):3:PLANK:BUILD;6:Table (E):3:PLANK:BUILD;7:Grandfather clock:1:PLANK:REPAIR
            EXPERT|Ardougne|Noella|0:Dresser:2:PLANK:BUILD;1:Cupboard:2:PLANK:BUILD;2:Hat stand:1:PLANK:REPAIR;3:Mirror:1:PLANK:REPAIR;4:Drawers:2:PLANK:BUILD;5:Table (W):3:PLANK:BUILD;7:Grandfather clock:1:PLANK:REPAIR
            EXPERT|Ardougne|Ross|1:Drawers (N):2:PLANK:BUILD;2:Drawers (S):2:PLANK:BUILD;3:Double bed:3:PLANK:BUILD;4:Hat stand:1:PLANK:REPAIR;5:Bed:2:PLANK:BUILD;6:Mirror:1:PLANK:REPAIR
            EXPERT|Ardougne|Ross|0:Range:1:BAR:REPAIR;1:Drawers (N):2:PLANK:BUILD;2:Drawers (S):2:PLANK:BUILD;3:Double bed:3:PLANK:BUILD;4:Hat stand:1:PLANK:REPAIR;5:Bed:2:PLANK:BUILD;6:Mirror:1:PLANK:REPAIR
            EXPERT|Ardougne|Ross|0:Range:1:BAR:REPAIR;1:Drawers (N):2:PLANK:BUILD;2:Drawers (S):2:PLANK:BUILD;3:Double bed:3:PLANK:BUILD;4:Hat stand:1:PLANK:REPAIR;5:Bed:2:PLANK:BUILD
            EXPERT|Falador|Larry|1:Drawers (N):2:PLANK:BUILD;2:Drawers (S):2:PLANK:BUILD;3:Table (D):3:PLANK:BUILD;4:Hat stand:1:PLANK:REPAIR;5:Table (U):3:PLANK:BUILD;6:Grandfather clock:1:PLANK:REPAIR
            EXPERT|Falador|Larry|0:Range:1:BAR:REPAIR;1:Drawers (N):2:PLANK:BUILD;2:Drawers (S):2:PLANK:BUILD;3:Table (D):3:PLANK:BUILD;4:Hat stand:1:PLANK:REPAIR;5:Table (U):3:PLANK:BUILD;6:Grandfather clock:1:PLANK:REPAIR
            EXPERT|Falador|Larry|0:Range:1:BAR:REPAIR;1:Drawers (N):2:PLANK:BUILD;2:Drawers (S):2:PLANK:BUILD;3:Table (D):3:PLANK:BUILD;4:Hat stand:1:PLANK:REPAIR;5:Table (U):3:PLANK:BUILD;6:Grandfather clock:1:PLANK:REPAIR
            EXPERT|Falador|Norman|0:Range:1:BAR:REPAIR;1:Grandfather clock:1:PLANK:REPAIR;2:Table:3:PLANK:BUILD;3:Double bed:2:PLANK:BUILD;4:Bookshelf:2:PLANK:BUILD;5:Drawers:3:PLANK:BUILD;6:Small table:2:PLANK:BUILD
            EXPERT|Falador|Norman|0:Range:1:BAR:REPAIR;2:Table:3:PLANK:BUILD;3:Double bed:2:PLANK:BUILD;4:Bookshelf:2:PLANK:BUILD;5:Drawers:3:PLANK:BUILD;6:Small table:2:PLANK:BUILD
            EXPERT|Falador|Norman|1:Grandfather clock:1:PLANK:REPAIR;2:Table:3:PLANK:BUILD;3:Double bed:2:PLANK:BUILD;4:Bookshelf:2:PLANK:BUILD;5:Drawers:3:PLANK:BUILD;6:Small table:2:PLANK:BUILD
            EXPERT|Falador|Tau|0:Sink:1:BAR:REPAIR;1:Table (S):3:PLANK:BUILD;2:Table (N):3:PLANK:BUILD;3:Cupboard:2:PLANK:BUILD;4:Shelves (W):2:PLANK:BUILD;5:Shelves (E):2:PLANK:BUILD
            EXPERT|Falador|Tau|1:Table (S):3:PLANK:BUILD;2:Table (N):3:PLANK:BUILD;3:Cupboard:2:PLANK:BUILD;4:Shelves (W):2:PLANK:BUILD;5:Shelves (E):2:PLANK:BUILD;6:Hat stand:1:PLANK:REPAIR
            EXPERT|Falador|Tau|0:Sink:1:BAR:REPAIR;1:Table (S):3:PLANK:BUILD;2:Table (N):3:PLANK:BUILD;3:Cupboard:2:PLANK:BUILD;4:Shelves (W):2:PLANK:BUILD;5:Shelves (E):2:PLANK:BUILD;6:Hat stand:1:PLANK:REPAIR
            EXPERT|Hosidius|Barbara|0:Grandfather clock:1:BAR:REPAIR;1:Range:1:PLANK:REPAIR;2:Table:2:PLANK:BUILD;3:Drawers:3:PLANK:BUILD;4:Bed:2:PLANK:BUILD;5:Chair (N):1:PLANK:BUILD;6:Chair (S):1:PLANK:BUILD
            EXPERT|Hosidius|Barbara|0:Grandfather clock:1:BAR:REPAIR;2:Table:2:PLANK:BUILD;3:Drawers:3:PLANK:BUILD;4:Bed:2:PLANK:BUILD;5:Chair (N):1:PLANK:BUILD;6:Chair (S):1:PLANK:BUILD
            EXPERT|Hosidius|Barbara|1:Range:1:PLANK:REPAIR;2:Table:2:PLANK:BUILD;3:Drawers:3:PLANK:BUILD;4:Bed:2:PLANK:BUILD;5:Chair (N):1:PLANK:BUILD;6:Chair (S):1:PLANK:BUILD
            EXPERT|Hosidius|Leela|0:Small table (N):2:PLANK:BUILD;1:Small table (S):2:PLANK:BUILD;3:Double bed:3:PLANK:BUILD;4:Table (U):3:PLANK:BUILD;5:Mirror:1:PLANK:REPAIR;6:Cupboard:2:PLANK:BUILD
            EXPERT|Hosidius|Leela|0:Small table (N):2:PLANK:BUILD;1:Small table (S):2:PLANK:BUILD;2:Sink:1:BAR:REPAIR;3:Double bed:3:PLANK:BUILD;4:Table (U):3:PLANK:BUILD;5:Mirror:1:PLANK:REPAIR;6:Cupboard:2:PLANK:BUILD
            EXPERT|Hosidius|Leela|0:Small table (N):2:PLANK:BUILD;1:Small table (S):2:PLANK:BUILD;2:Sink:1:BAR:REPAIR;3:Double bed:3:PLANK:BUILD;4:Table (U):3:PLANK:BUILD;6:Cupboard:2:PLANK:BUILD
            EXPERT|Hosidius|Mariah|0:Table:3:PLANK:BUILD;1:Sink:1:BAR:REPAIR;2:Cooking shelves:2:PLANK:BUILD;3:Cupboard:2:PLANK:BUILD;4:Bed:2:PLANK:BUILD;5:Small table (W):2:PLANK:BUILD;6:Small table (E):2:PLANK:BUILD;7:Hat stand:1:PLANK:REPAIR
            EXPERT|Hosidius|Mariah|0:Table:3:PLANK:BUILD;2:Cooking shelves:2:PLANK:BUILD;3:Cupboard:2:PLANK:BUILD;4:Bed:2:PLANK:BUILD;5:Small table (W):2:PLANK:BUILD;6:Small table (E):2:PLANK:BUILD;7:Hat stand:1:PLANK:REPAIR
            EXPERT|Hosidius|Mariah|0:Table:3:PLANK:BUILD;1:Sink:1:BAR:REPAIR;2:Cooking shelves:2:PLANK:BUILD;3:Cupboard:2:PLANK:BUILD;4:Bed:2:PLANK:BUILD;5:Small table (W):2:PLANK:BUILD;6:Small table (E):2:PLANK:BUILD
            EXPERT|Varrock|Bob|0:Large table:4:PLANK:BUILD;1:Grandfather clock:1:PLANK:REPAIR;2:Cabinet (S):2:PLANK:BUILD;3:Cabinet (N):2:PLANK:BUILD;4:Bookcase (S):2:PLANK:BUILD;5:Bookcase (N):2:PLANK:BUILD;6:Wardrobe:2:PLANK:BUILD;7:Drawers:2:PLANK:BUILD
            EXPERT|Varrock|Bob|0:Large table:4:PLANK:BUILD;2:Cabinet (S):2:PLANK:BUILD;3:Cabinet (N):2:PLANK:BUILD;4:Bookcase (S):2:PLANK:BUILD;5:Bookcase (N):2:PLANK:BUILD;6:Wardrobe:2:PLANK:BUILD;7:Drawers:2:PLANK:BUILD
            EXPERT|Varrock|Jeff|0:Table:3:PLANK:BUILD;1:Bookcase:2:PLANK:BUILD;2:Shelves:2:PLANK:BUILD;3:Bed:3:PLANK:BUILD;4:Drawers:2:PLANK:BUILD;5:Dresser:2:PLANK:BUILD;6:Mirror:1:PLANK:REPAIR;7:Chair:1:PLANK:BUILD
            EXPERT|Varrock|Jeff|0:Table:3:PLANK:BUILD;1:Bookcase:2:PLANK:BUILD;2:Shelves:2:PLANK:BUILD;3:Bed:3:PLANK:BUILD;4:Drawers:2:PLANK:BUILD;5:Dresser:2:PLANK:BUILD;7:Chair:1:PLANK:BUILD
            EXPERT|Varrock|Sarah|0:Table:3:PLANK:BUILD;1:Bed:2:PLANK:BUILD;2:Dresser:2:PLANK:BUILD;3:Small table:2:PLANK:BUILD;4:Range:1:BAR:REPAIR;5:Shelves:2:PLANK:BUILD
            EXPERT|Varrock|Sarah|0:Table:3:PLANK:BUILD;1:Bed:2:PLANK:BUILD;2:Dresser:2:PLANK:BUILD;3:Small table:2:PLANK:BUILD;5:Shelves:2:PLANK:BUILD
            """;

    public static final List<Layout> LAYOUTS = parse();

    private static List<Layout> parse() {
        List<Layout> list = new ArrayList<>();
        for (String line : DATA.strip().split("\n")) {
            String[] c = line.strip().split("\\|");
            List<Hotspot> spots = new ArrayList<>();
            for (String h : c[3].split(";")) {
                String[] f = h.split(":");
                spots.add(new Hotspot(Integer.parseInt(f[0]), f[1], Integer.parseInt(f[2]), f[3].equals("BAR"), f[4].equals("REPAIR")));
            }
            list.add(new Layout(List.of(MinigameRules.MahoganyHomes.TIER_IDS).indexOf(c[0]), c[1], c[2], List.copyOf(spots)));
        }
        return List.copyOf(list);
    }

    /** Every layout of a tier. */
    public static List<Layout> layouts(int tier) {
        return LAYOUTS.stream().filter(l -> l.tier() == tier).toList();
    }

    /** The homeowners in table order (Ardougne, Falador, Hosidius, Varrock). */
    public static List<String> owners() {
        Set<String> owners = new LinkedHashSet<>();
        for (Layout l : LAYOUTS) {
            owners.add(l.owner());
        }
        return List.copyOf(owners);
    }

    /** The layouts a homeowner has at a tier. */
    public static List<Layout> layouts(int tier, String owner) {
        return LAYOUTS.stream().filter(l -> l.tier() == tier && l.owner().equals(owner)).toList();
    }
}
