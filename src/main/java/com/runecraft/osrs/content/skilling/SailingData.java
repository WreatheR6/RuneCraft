package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.PlaceDef;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Sailing (released November 2025), faithful in spirit: boats built at a workbench and launched on
 * water (raft, skiff, sloop; better boats need more Sailing and give bonus XP while sailing),
 * salvaging shipwrecks with a salvaging hook, sea charting with a spyglass, port courier tasks
 * between port task boards, and trawling for fish from a boat. Sailing needs the Pandemonium quest
 * (it gives the raft). Boat levels (raft 1, skiff 15, sloop 50), shipwreck levels with their salvage
 * and sorting XP, port docking levels and trawled fish levels follow the OSRS Wiki; charting XP,
 * courier pay and salvage loot are approximations.
 */
public final class SailingData extends SkillingPart {

    /** Boats: kit item, Sailing level to sail, bonus Sailing XP per 32 blocks, vanilla boat variant. */
    public record Boat(String kit, String name, int level, double bonusXp, String variant) {
    }

    public static final List<Boat> BOATS = List.of(
            new Boat("raft_kit", "Raft", 1, 0.0, "BAMBOO"),
            new Boat("skiff_kit", "Skiff", 15, 5.0, "OAK"),
            new Boat("sloop_kit", "Sloop", 50, 10.0, "DARK_OAK"));

    /** The quest that teaches Sailing and gives the first boat (OSRS: Pandemonium). */
    public static final String SAILING_QUEST = "pandemonium";

    public static Boat boat(String kit) {
        for (Boat b : BOATS) {
            if (b.kit().equals(kit)) {
                return b;
            }
        }
        return null;
    }

    /**
     * Shipwrecks: block id, Sailing level, XP per salvage hauled up, salvage item (and its OSRS name), success chance out of
     * 256 at level 1 and 99, the loot table and the XP for sorting one salvage.
     */
    public record Wreck(String blockId, String name, int level, double xp, String salvage, int low, int high, List<Loot> loot,
                        String salvageName, double sortXp) {
    }

    private static List<Loot> salvageTable(int tier) {
        List<Loot> l = new ArrayList<>();
        String[] planks = {"plank", "plank", "oak_plank", "oak_plank", "teak_plank", "teak_plank", "mahogany_plank", "mahogany_plank"};
        String[] nails = {"bronze_nails", "iron_nails", "iron_nails", "steel_nails", "steel_nails", "mithril_nails", "adamant_nails",
                "rune_nails"};
        String[] gems = {"uncut_opal", "uncut_jade", "uncut_red_topaz", "uncut_sapphire", "uncut_emerald", "uncut_ruby", "uncut_diamond",
                "uncut_dragonstone"};
        l.add(Loot.of(planks[tier], 2, 5 + tier, 30));
        l.add(Loot.of(nails[tier], 5, 15 + tier * 3, 25));
        l.add(Loot.of("rope", 1, 2, 15));
        l.add(Loot.of("coins", 20 + tier * 40, 100 + tier * 150, 25));
        l.add(Loot.of("bolt_of_cloth", 1, 1 + tier / 3, 8));
        l.add(Loot.of(gems[tier], 1, 1, 6));
        l.add(Loot.of("casket", 4));
        l.add(Loot.of("seaweed", 1, 4, 10));
        l.add(Loot.of(tier < 4 ? "clue_scroll_easy" : tier < 6 ? "clue_scroll_medium" : "clue_scroll_hard", 1));
        return l;
    }

    /** OSRS Wiki (shipwreck salvaging): salvage XP and sorting XP per salvage. */
    public static final List<Wreck> WRECKS = List.of(
            new Wreck("small_shipwreck", "Small shipwreck", 15, 10.0, "small_salvage", 80, 220, salvageTable(0), "Small salvage", 5.5),
            new Wreck("fishermans_shipwreck", "Fisherman's shipwreck", 26, 17.0, "fishy_salvage", 60, 200, salvageTable(1),
                    "Fishy salvage", 9.0),
            new Wreck("barracuda_shipwreck", "Barracuda shipwreck", 35, 31.0, "barracuda_salvage", 50, 190, salvageTable(2),
                    "Barracuda salvage", 15.5),
            new Wreck("large_shipwreck", "Large shipwreck", 53, 48.0, "large_salvage", 40, 170, salvageTable(3), "Large salvage", 24.0),
            new Wreck("pirate_shipwreck", "Pirate shipwreck", 64, 76.0, "pirate_salvage", 32, 150, salvageTable(4), "Plundered salvage",
                    31.5),
            new Wreck("mercenary_shipwreck", "Mercenary shipwreck", 73, 138.0, "mercenary_salvage", 26, 130, salvageTable(5),
                    "Martial salvage", 63.5),
            new Wreck("fremennik_shipwreck", "Fremennik shipwreck", 80, 162.0, "fremennik_salvage", 20, 120, salvageTable(6),
                    "Fremennik salvage", 75.0),
            new Wreck("merchant_shipwreck", "Merchant shipwreck", 87, 200.0, "merchant_salvage", 16, 110, salvageTable(7),
                    "Opulent salvage", 95.0));

    public static Wreck wreck(String blockId) {
        for (Wreck w : WRECKS) {
            if (w.blockId().equals(blockId)) {
                return w;
            }
        }
        return null;
    }

    public static Wreck wreckBySalvage(String item) {
        for (Wreck w : WRECKS) {
            if (w.salvage().equals(item)) {
                return w;
            }
        }
        return null;
    }

    /** Sea charting points: place id, name, OSRS coords, Sailing level, XP (once per player). */
    public record ChartPoint(String placeId, String name, int x, int y, int level, double xp) {
    }

    public static final List<ChartPoint> CHART_POINTS = List.of(
            new ChartPoint("chart_mudskipper_point", "Mudskipper Point", 2995, 3115, 1, 60),
            new ChartPoint("chart_musa_point", "Musa Point waters", 2925, 3135, 5, 90),
            new ChartPoint("chart_crandor", "Crandor", 2850, 3240, 12, 150),
            new ChartPoint("chart_entrana", "Entrana", 2830, 3360, 18, 220),
            new ChartPoint("chart_catherby_bay", "Catherby bay", 2840, 3420, 24, 300),
            new ChartPoint("chart_fishing_platform", "Fishing Platform", 2780, 3285, 30, 380),
            new ChartPoint("chart_port_khazard", "Port Khazard", 2670, 3150, 35, 450),
            new ChartPoint("chart_rellekka", "Rellekka waters", 2640, 3700, 45, 600),
            new ChartPoint("chart_miscellania", "Miscellania", 2560, 3870, 52, 720),
            new ChartPoint("chart_waterbirth", "Waterbirth Island", 2540, 3760, 58, 850),
            new ChartPoint("chart_lunar_isle", "Lunar Isle", 2110, 3910, 65, 1000),
            new ChartPoint("chart_iceberg", "The Iceberg", 2660, 4010, 72, 1200),
            new ChartPoint("chart_ape_atoll", "Ape Atoll", 2700, 2780, 80, 1450),
            new ChartPoint("chart_fossil_island", "Fossil Island", 3730, 3880, 86, 1700),
            new ChartPoint("chart_void_outpost", "Void Knights' Outpost", 2660, 2650, 90, 2000));

    public static ChartPoint chart(String placeId) {
        for (ChartPoint c : CHART_POINTS) {
            if (c.placeId().equals(placeId)) {
                return c;
            }
        }
        return null;
    }

    /** Ports with a task board: block id (one block per port), name, OSRS coords, Sailing level to take tasks there. */
    public record Port(String id, String name, int x, int y, int level) {
        public String board() {
            return id + "_task_board";
        }
    }

    /** Docking levels from the OSRS Wiki; Port Phasmatys approx. */
    public static final List<Port> PORTS = List.of(
            new Port("port_sarim", "Port Sarim", 3043, 3200, 1),
            new Port("musa_point", "Musa Point", 2955, 3150, 10),
            new Port("catherby", "Catherby", 2805, 3425, 20),
            new Port("brimhaven", "Brimhaven", 2775, 3230, 25),
            new Port("port_khazard", "Port Khazard", 2675, 3160, 30),
            new Port("rellekka", "Rellekka", 2630, 3690, 62),
            new Port("port_phasmatys", "Port Phasmatys", 3702, 3503, 45), // approx
            new Port("corsair_cove", "Corsair Cove", 2570, 2860, 40),
            new Port("port_piscarilius", "Port Piscarilius", 1824, 3690, 15));

    public static Port port(String id) {
        for (Port p : PORTS) {
            if (p.id().equals(id)) {
                return p;
            }
        }
        return null;
    }

    public static Port portByBoard(String block) {
        for (Port p : PORTS) {
            if (p.board().equals(block)) {
                return p;
            }
        }
        return null;
    }

    /** Courier reward: Sailing XP and coins for a delivery over {@code tiles} OSRS tiles. */
    public static double courierXp(int tiles, int destLevel) {
        return 40 + tiles * 0.5 + destLevel * 3;
    }

    public static int courierCoins(int tiles) {
        return 50 + tiles / 2;
    }

    /** Trawling: Sailing XP per catch on top of the fish's Fishing XP. approx (the wiki lists roughly 9-13 combined) */
    public static final double TRAWL_SAILING_XP = 8.0;

    /** Why a player can't take a boat out, or null: Pandemonium must be done and the boat's Sailing level met. */
    public static String canSail(boolean pandemoniumDone, int sailingLevel, Boat boat) {
        if (!pandemoniumDone) {
            return "You need to complete Pandemonium before you can sail your own boat.";
        }
        if (boat != null && sailingLevel < boat.level()) {
            return "You need a Sailing level of " + boat.level() + " to sail " + (boat.name().startsWith("A") ? "an " : "a ")
                    + boat.name().toLowerCase(java.util.Locale.ROOT) + ".";
        }
        return null;
    }

    @Override
    public void items(ItemSink s) {
        tool(s, "spyglass", "Spyglass", "spyglass", 0xB09040, 50);
        tool(s, "salvaging_hook", "Salvaging hook", "hook", 0x8A8A9A, 40);
        tool(s, "trawling_net", "Trawling net", "net", 0x8A7A5A, 60);
        simple(s, "cargo_crate", "Cargo crate", "crate", 0x8A6A3A, 1).stack(1).examine("A courier delivery for another port.");
        for (Boat b : BOATS) {
            simple(s, b.kit(), b.name() + " kit", "boat", b.variant().equals("BAMBOO") ? 0xC0B060
                    : b.variant().equals("OAK") ? 0x9A7A4A : 0x4A3A2A, 100 + b.level() * 50).stack(1)
                    .examine("Use it on water to launch your " + b.name().toLowerCase() + ".");
        }
        for (Wreck w : WRECKS) {
            simple(s, w.salvage(), w.salvageName(), "salvage", 0x7A6A5A, 10 + w.level())
                    .examine("Sort through it for anything useful.");
        }
    }

    @Override
    public void recipes(Consumer<Recipe> out) {
        out.accept(r("build_raft_kit", MenuType.CONSTRUCTION, "Raft kit", Skill.CONSTRUCTION, 1, 20.0).in("logs", 5).in("rope", 2)
                .out("raft_kit", 1).tool("hammer").ticks(4).build());
        out.accept(r("build_skiff_kit", MenuType.CONSTRUCTION, "Skiff kit", Skill.CONSTRUCTION, 30, 300.0).in("oak_plank", 20)
                .in("iron_nails", 30).in("bolt_of_cloth", 2).in("rope", 4).out("skiff_kit", 1).tool("hammer").ticks(4).build());
        out.accept(r("build_sloop_kit", MenuType.CONSTRUCTION, "Sloop kit", Skill.CONSTRUCTION, 50, 900.0).in("teak_plank", 40)
                .in("steel_nails", 60).in("bolt_of_cloth", 6).in("rope", 8).out("sloop_kit", 1).tool("hammer").ticks(4).build());
        Object[][] shop = {{"spyglass", 50}, {"salvaging_hook", 40}, {"trawling_net", 60}, {"rope", 18}};
        for (Object[] o : shop) {
            out.accept(plain("shop_tools_" + o[0], MenuType.SHOP_TOOLS, title((String) o[0]) + " - " + o[1] + " gp")
                    .in("coins", (Integer) o[1]).out((String) o[0], 1).ticks(0).build());
        }
    }

    @Override
    public List<BlockDef> blocks() {
        List<BlockDef> list = new ArrayList<>();
        for (Wreck w : WRECKS) {
            list.add(BlockDef.scripted(w.blockId(), w.name(), 0x5A4A3A, "shipwreck"));
        }
        list.add(BlockDef.scripted("sea_chart_marker", "Charting buoy", 0xD04030, "buoy"));
        for (Port p : PORTS) {
            list.add(BlockDef.scripted(p.board(), p.name() + " port task board", 0x8A6A3A, "notice_board"));
        }
        return list;
    }

    @Override
    public List<PlaceDef> places() {
        List<PlaceDef> list = new ArrayList<>();
        // Shipwrecks sit out at sea; SkillingHooks places the wreck block on the water surface.
        int[][] wreckCoords = {{3060, 3150}, {2960, 3190}, {2860, 3170}, {2760, 3300}, {2700, 3200}, {2600, 3500}, {2560, 3800},
                {2440, 3100}};
        for (int i = 0; i < WRECKS.size(); i++) {
            Wreck w = WRECKS.get(i);
            list.add(PlaceDef.of(w.blockId() + "_site", w.name(), wreckCoords[i][0], wreckCoords[i][1], PlaceDef.Style.NONE, List.of()));
        }
        for (ChartPoint c : CHART_POINTS) {
            list.add(PlaceDef.of(c.placeId(), "Charting point: " + c.name(), c.x(), c.y(), PlaceDef.Style.NONE, List.of()));
        }
        for (Port p : PORTS) {
            list.add(PlaceDef.of(p.id() + "_docks", p.name() + " docks", p.x(), p.y(), PlaceDef.Style.CAMP, List.of(p.board())));
        }
        return list;
    }
}
