package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Sailing (released November 2025): boats launched on water (raft 1, skiff 15, sloop 50), shipwreck
 * salvaging and sorting (wiki levels and XP, one wiki spot per wreck type), sea charting (the wiki's
 * 358 tasks, see {@link SailingTraining}), courier tasks between port notice boards (the wiki's 439
 * tasks and their XP), trawling from a boat, boat parts and facilities built at boat schematics, and
 * the Barracuda Trials. Sailing needs the Pandemonium quest (it gives the raft). Salvage loot, boat combat, bounty tasks,
 * ocean encounters and hazards live in {@link SailingSea} (wiki tables). The
 * shop sources of Sailing-island materials are approximations.
 */
public final class SailingData extends SkillingPart {

    /** Boats: kit item, Sailing level to sail, (unused since sail trimming) bonus XP, vanilla boat variant. */
    public record Boat(String kit, String name, int level, double bonusXp, String variant) {
        /** The boat type id used for parts and trials ("raft", "skiff", "sloop"). */
        public String type() {
            return kit.replace("_kit", "");
        }
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
     * 256 at level 1 and 99 (approx), the salvage's OSRS name and the XP for sorting one salvage. Sorting loot is the
     * wiki table in {@link SailingSea#SALVAGE}.
     */
    public record Wreck(String blockId, String name, int level, double xp, String salvage, int low, int high,
                        String salvageName, double sortXp) {
        /** Every item sorting this salvage can give (weights out of 100,000 of the wiki chances). */
        public List<Loot> loot() {
            List<Loot> l = new ArrayList<>();
            for (SailingSea.SalvageLine s : SailingSea.SALVAGE.getOrDefault(salvage, List.of())) {
                l.add(Loot.of(s.item(), s.min(), s.max(), (int) Math.max(1, Math.round(s.chance() * 100_000))));
            }
            return l;
        }
    }

    /** OSRS Wiki (shipwreck salvaging): salvage XP and sorting XP per salvage. */
    public static final List<Wreck> WRECKS = List.of(
            new Wreck("small_shipwreck", "Small shipwreck", 15, 10.0, "small_salvage", 80, 220, "Small salvage", 5.5),
            new Wreck("fishermans_shipwreck", "Fisherman's shipwreck", 26, 17.0, "fishy_salvage", 60, 200,
                    "Fishy salvage", 9.0),
            new Wreck("barracuda_shipwreck", "Barracuda shipwreck", 35, 31.0, "barracuda_salvage", 50, 190,
                    "Barracuda salvage", 15.5),
            new Wreck("large_shipwreck", "Large shipwreck", 53, 48.0, "large_salvage", 40, 170, "Large salvage", 24.0),
            new Wreck("pirate_shipwreck", "Pirate shipwreck", 64, 76.0, "pirate_salvage", 32, 150, "Plundered salvage",
                    31.5),
            new Wreck("mercenary_shipwreck", "Mercenary shipwreck", 73, 138.0, "mercenary_salvage", 26, 130,
                    "Martial salvage", 63.5),
            new Wreck("fremennik_shipwreck", "Fremennik shipwreck", 80, 162.0, "fremennik_salvage", 20, 120,
                    "Fremennik salvage", 75.0),
            new Wreck("merchant_shipwreck", "Merchant shipwreck", 87, 200.0, "merchant_salvage", 16, 110,
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

    /** Ports with a task board: block id (one block per port), name, OSRS coords, Sailing level to take tasks there. */
    public record Port(String id, String name, int x, int y, int level) {
        public String board() {
            return id + "_task_board";
        }
    }

    /**
     * Port task levels from the OSRS Wiki (Sailing level up table); notice board tiles from the wiki's notice board map
     * where the port has one. Port Phasmatys has no OSRS port tasks (kept for its charter dock).
     */
    public static final List<Port> PORTS = List.of(
            new Port("port_sarim", "Port Sarim", 3043, 3200, 1),
            new Port("musa_point", "Musa Point", 2955, 3150, 10),
            new Port("catherby", "Catherby", 2805, 3425, 20),
            new Port("brimhaven", "Brimhaven", 2775, 3230, 25),
            new Port("port_khazard", "Port Khazard", 2675, 3160, 30),
            new Port("rellekka", "Rellekka", 2630, 3690, 62),
            new Port("port_phasmatys", "Port Phasmatys", 3702, 3503, 45), // approx
            new Port("corsair_cove", "Corsair Cove", 2570, 2860, 40),
            new Port("port_piscarilius", "Port Piscarilius", 1824, 3690, 15),
            // Courier ports added with the wiki task list (round 8).
            new Port("pandemonium", "The Pandemonium", 3058, 2985, 1),
            new Port("lands_end", "Land's End", 1502, 3407, 5),
            new Port("hosidius", "Hosidius", 1762, 3460, 5), // approx dock (no notice board)
            new Port("ardougne", "Ardougne", 2676, 3276, 28),
            new Port("entrana", "Entrana", 2834, 3335, 36), // approx dock (no notice board)
            new Port("civitas_illa_fortis", "Civitas illa Fortis", 1782, 3142, 38),
            new Port("cairn_isle", "Cairn Isle", 2766, 2975, 42), // approx dock (no notice board)
            new Port("sunset_coast", "Sunset Coast", 1514, 2972, 44), // approx dock (no notice board)
            new Port("the_summer_shore", "The Summer Shore", 3183, 2368, 45),
            new Port("aldarin", "Aldarin", 1438, 2969, 46),
            new Port("ruins_of_unkah", "Ruins of Unkah", 3145, 2828, 48),
            new Port("void_knights_outpost", "Void Knights' Outpost", 2659, 2672, 50),
            new Port("port_roberts", "Port Roberts", 1872, 3303, 50),
            new Port("red_rock", "Red Rock", 2806, 2511, 52), // approx level (lowest courier task there)
            new Port("etceteria", "Etceteria", 2617, 3849, 65),
            new Port("port_tyras", "Port Tyras", 2146, 3123, 66),
            new Port("deepfin_point", "Deepfin Point", 1931, 2761, 67),
            new Port("jatizso", "Jatizso", 2420, 3781, 68), // approx dock (no notice board)
            new Port("neitiznot", "Neitiznot", 2311, 3781, 68), // approx dock (no notice board)
            new Port("prifddinas", "Prifddinas", 2163, 3326, 70),
            new Port("piscatoris", "Piscatoris", 2341, 3690, 75), // approx dock (no notice board)
            new Port("lunar_isle", "Lunar Isle", 2139, 3884, 76));

    /** Ports whose task-board places predate round 8 and are built as camps with a plaza. */
    static final int CAMP_PORTS = 9;

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
        SailingLockboxes.items(s);
        tool(s, "spyglass", "Spyglass", "spyglass", 0xB09040, 50);
        tool(s, "salvaging_hook", "Salvaging hook", "hook", 0x8A8A9A, 40);
        tool(s, "trawling_net", "Trawling net", "net", 0x8A7A5A, 60);
        simple(s, "cargo_crate", "Cargo crate", "crate", 0x8A6A3A, 1).examine("A courier delivery for another port.");
        // Sea charting tools (OSRS: crowbar from Prying Times, current duck from Current Affairs; sold here, approx).
        tool(s, "crowbar", "Crowbar", "crowbar", 0x6A6A7A, 30).examine("Good for prying open sealed crates.");
        tool(s, "current_duck", "Current duck", "duck", 0xE0C030, 40).examine("Follows the currents of the sea.");
        tool(s, "diving_apparatus", "Diving apparatus", "helmet", 0xB0C0D0, 200).examine("A fishbowl helmet and air pump for diving.");
        tool(s, "portable_weather_station", "Portable weather station", "machine", 0x9AA0A8, 100)
                .examine("Takes readings of the local weather.");
        // Shipbuilding materials of the Sailing islands (OSRS: Sailing woods, lead and nickel ores; sold by shipwrights here, approx).
        // Log values: OSRS Wiki infoboxes (camphor 220, ironwood 360, rosewood 370); plank values approx.
        String[][] woods = {{"camphor", "Camphor", "8A6A4A", "220"}, {"ironwood", "Ironwood", "5A4A40", "360"},
                {"rosewood", "Rosewood", "9A4A4A", "370"}};
        for (String[] w : woods) {
            int c = Integer.parseInt(w[2], 16);
            simple(s, w[0] + "_logs", w[1] + " logs", "log", c, Integer.parseInt(w[3])).examine("Logs cut from " + w[0] + " trees.");
            simple(s, w[0] + "_plank", w[1] + " plank", "plank", c, 600).examine("A plank of fine " + w[0] + ".");
        }
        simple(s, "lead_bar", "Lead bar", "bar", 0x6A6A78, 120).examine("A bar of lead.");
        simple(s, "cupronickel_bar", "Cupronickel bar", "bar", 0xB08A6A, 400).examine("An alloy of copper and nickel.");
        simple(s, "bolt_of_linen", "Bolt of linen", "cloth", 0xE0D8C0, 20).examine("A bolt of linen for sails.");
        simple(s, "bolt_of_canvas", "Bolt of canvas", "cloth", 0xC8B890, 100).examine("A bolt of canvas for sails.");
        simple(s, "bolt_of_cotton", "Bolt of cotton", "cloth", 0xF4F0E8, 400).examine("A bolt of cotton for sails.");
        simple(s, "dragon_nails", "Dragon nails", "nails", 0xA02020, 1500).examine("Keeps things in place very permanently.");
        simple(s, "dragon_metal_sheet", "Dragon metal sheet", "bar", 0xB02A2A, 30000).examine("A sheet of dragon metal.");
        simple(s, "heart_of_ithell", "Heart of Ithell", "orb", 0x80E0D0, 1).examine("A crystal heart from the Gwenith Glide.");
        for (Boat b : BOATS) {
            simple(s, b.kit(), b.name() + " kit", "boat", b.variant().equals("BAMBOO") ? 0xC0B060
                    : b.variant().equals("OAK") ? 0x9A7A4A : 0x4A3A2A, 100 + b.level() * 50).stack(1)
                    .examine("Use it on water to launch your " + b.name().toLowerCase() + ".");
        }
        for (Wreck w : WRECKS) {
            simple(s, w.salvage(), w.salvageName(), "salvage", 0x7A6A5A, 10 + w.level())
                    .examine("Sort through it for anything useful.");
        }
        // Boat cannon ammunition (OSRS Wiki Cannonball table; steel is the multicannon's "cannonball").
        String[][] balls = {{"bronze", "B08850"}, {"iron", "6A6A6A"}, {"mithril", "4A5A8A"}, {"adamant", "4A7A5A"}, {"rune", "4A8AAA"},
                {"dragon", "A03030"}};
        int[] ballValue = {5, 9, 30, 60, 160, 600}; // approx
        for (int i = 0; i < balls.length; i++) {
            simple(s, balls[i][0] + "_cannonball", SailingSea.title(balls[i][0]) + " cannonball", "cannonball",
                    Integer.parseInt(balls[i][1], 16), ballValue[i]).stack(Integer.MAX_VALUE).examine("Ammo for a boat cannon.");
        }
        // Chainshot and incendiary cannonballs, repair kits, chain, rubium splinters, nickel ore and shark paint (wiki; values approx).
        for (SailingCrew.Special sp : SailingCrew.SPECIALS) {
            SailingSea.Ball base = SailingSea.ball(sp.base());
            simple(s, sp.item(), sp.name(), "cannonball", sp.chainshot() ? 0x5A5A60 : 0xC04A20, 10 + base.tier() * 40)
                    .stack(Integer.MAX_VALUE).examine(sp.chainshot() ? "Two cannonballs chained together; slows what it hits."
                            : "A cannonball packed with rubium; sets what it hits alight.");
        }
        for (SailingCrew.Kit k : SailingCrew.KITS) {
            simple(s, k.id(), k.name(), "package", 0x8A6A3A, k.heals() * 100).examine("Apply it on your boat to repair "
                    + k.heals() + " hitpoints.");
        }
        simple(s, "chain", "Chain", "rope", 0x7A7A80, 60).examine("Links two cannonballs together.");
        simple(s, "rubium_splinters", "Rubium splinters", "shard", 0xC03A2A, 20).stack(Integer.MAX_VALUE)
                .examine("Splinters of a volatile red mineral.");
        simple(s, "nickel_ore", "Nickel ore", "ore", 0xA8A890, 200).examine("This needs refining.");
        simple(s, "shark_paint", "Shark paint", "pot_item", 0x5A6A7A, 1000).examine("Paint for the trim of a skiff or sloop.");
        // Salvage, sea creature and bounty items (wiki names; values approx).
        for (String[] it : SEA_ITEMS) {
            if (it[0].endsWith(" seed") || it[0].endsWith(" frag")) {
                // Sailing seeds and coral frags are planted (FarmingData: hops, hardwood and coral nursery crops).
                s.add(SailingSea.itemId(it[0]), it[0], com.runecraft.osrs.ItemDef.Kind.SEED, it[1], Integer.parseInt(it[2], 16))
                        .value(Integer.parseInt(it[3]));
                continue;
            }
            simple(s, SailingSea.itemId(it[0]), it[0], it[1], Integer.parseInt(it[2], 16), Integer.parseInt(it[3]));
        }
        for (String part : SailingSea.bountyParts()) {
            String name = SailingSea.BOUNTIES.stream().filter(b -> b.item().equals(part)).findFirst().orElseThrow().itemName();
            String shape = part.endsWith("beak") || part.endsWith("barbs") ? "tooth" : part.endsWith("feather") || part.endsWith("fin")
                    ? "tail" : part.endsWith("jaw") || part.endsWith("teeth") ? "teeth" : part.endsWith("skin") ? "hide"
                    : part.endsWith("tentacle") ? "tentacle" : part.endsWith("ink_sac") ? "pot_item" : part.endsWith("tusk")
                    || part.endsWith("horn") ? "horn" : part.endsWith("mace") ? "club" : part.endsWith("head") ? "skull" : "meat_raw";
            simple(s, part, name, shape, 0x8A7A6A, 0).stack(Integer.MAX_VALUE).examine("A bounty for a port master.");
        }
    }

    /** One shipwreck in each of the type's other wiki salvaging areas (x, y pairs; Shipwreck salvaging map pins). */
    public static final int[][] EXTRA_WRECK_SITES = {{1592, 3303}, {1863, 3832, 2696, 3050}, {2133, 3291, 2489, 2694, 2977, 2890, 3097, 2759},
            {1546, 2636, 1861, 2546, 2496, 2289, 3372, 2112}, {1792, 2869, 2088, 3656, 2177, 3804}, {1673, 2384, 1739, 2232},
            {1767, 4090, 2246, 4115}, {1794, 3995}};

    /** Items from salvage, sea creatures and ocean encounters: wiki name, shape, colour, value (approx; the seeds and frags are wiki values). */
    static final String[][] SEA_ITEMS = {
            {"Rusty locket", "amulet", "8A5A3A", "100"}, {"Boat bottle (empty)", "vial", "A0C0C0", "100"},
            {"Facility bottle (empty)", "vial", "A0B0C0", "100"}, {"Sailors' amulet (inert)", "amulet", "4A7AAA", "5000"},
            {"Mouldy block", "package", "6A7A4A", "10"}, {"Dull knife", "knife", "7A7A7A", "10"}, {"Broken compass", "orb", "B09040", "50"},
            {"Rusty coin", "coins", "8A5A3A", "50"}, {"Broken sextant", "orb", "B0A060", "100"}, {"Smashed mirror", "shard", "C0D0E0", "100"},
            {"Mouldy doll", "pet", "7A6A4A", "200"}, {"Hemp seed", "seed", "7A8A3A", "5"}, {"Flax seed", "seed", "8A9AC0", "5"},
            {"Cotton seed", "seed", "E0E0D0", "5"}, {"Camphor seed", "seed", "8A6A4A", "60"}, {"Ironwood seed", "seed", "5A4A40", "72"},
            {"Rosewood seed", "seed", "9A4A4A", "80"}, {"Elkhorn frag", "shard", "D0C0A0", "26"}, {"Pillar frag", "shard", "E0A0B0", "52"},
            {"Umbral frag", "shard", "5A3A7A", "78"}, {"Sawmill coupon (wood plank)", "note", "C0A070", "100"},
            {"Sawmill coupon (oak plank)", "note", "A08050", "250"}, {"Salvor's paint", "pot_item", "3A6A8A", "1000"},
            {"Inky paint", "pot_item", "202030", "1000"}, {"Broken dragon hook", "hook", "A02020", "50000"},
            {"Dragon cannon barrel", "bar", "B02A2A", "200000"}, {"Echo pearl", "orb", "C0E0F0", "5000"}, {"Ray barbs", "tooth", "8A7A6A", "500"},
            {"Swift albatross feather", "tail", "F0F0F0", "5000"}, {"Bottled storm", "vial", "6A7AC0", "5000"},
            {"Narwhal horn", "horn", "E0E0D0", "5000"}, {"Castaway", "creature", "C08060", "0"}};

    @Override
    public List<com.runecraft.osrs.content.MonsterDef> monsters() {
        return SailingSea.monsters();
    }

    @Override
    public void recipes(Consumer<Recipe> out) {
        out.accept(r("build_raft_kit", MenuType.CONSTRUCTION, "Raft kit", Skill.CONSTRUCTION, 1, 20.0).in("logs", 5).in("rope", 2)
                .out("raft_kit", 1).tool("hammer").ticks(4).build());
        out.accept(r("build_skiff_kit", MenuType.CONSTRUCTION, "Skiff kit", Skill.CONSTRUCTION, 30, 300.0).in("oak_plank", 20)
                .in("iron_nails", 30).in("bolt_of_cloth", 2).in("rope", 4).out("skiff_kit", 1).tool("hammer").ticks(4).build());
        out.accept(r("build_sloop_kit", MenuType.CONSTRUCTION, "Sloop kit", Skill.CONSTRUCTION, 50, 900.0).in("teak_plank", 40)
                .in("steel_nails", 60).in("bolt_of_cloth", 6).in("rope", 8).out("sloop_kit", 1).tool("hammer").ticks(4).build());
        // Repair kits at a workbench (wiki: plank, nails and swamp paste; 2 or 3 kits an action, 4 ticks).
        for (SailingCrew.Kit k : SailingCrew.KITS) {
            out.accept(r("build_" + k.id(), MenuType.CONSTRUCTION, k.made() + " x " + k.name(), Skill.CONSTRUCTION, k.construction(), k.xp())
                    .in(k.plank(), k.planks()).in(k.nails(), k.nailQty()).in("swamp_paste", k.paste()).out(k.id(), k.made())
                    .tool("hammer").ticks(4).build());
        }
        // Boat cannonballs at a furnace with an ammo mould (wiki: 4 balls a bar, 8 ticks; steel is SmithingData's).
        String[][] metals = {{"bronze", "bronze_bar", "5", "8.8"}, {"iron", "iron_bar", "20", "17.2"}, {"mithril", "mithril_bar", "55", "34"},
                {"adamant", "adamantite_bar", "75", "42.4"}, {"rune", "runite_bar", "90", "50.8"}};
        for (String[] m : metals) {
            int lvl = Integer.parseInt(m[2]);
            double xp = Double.parseDouble(m[3]);
            out.accept(r("smith_" + m[0] + "_cannonball", MenuType.SMELTING, "4 " + m[0] + " cannonballs", Skill.SMITHING, lvl, xp)
                    .in(m[1], 1).out(m[0] + "_cannonball", 4).tool("ammo_mould").ticks(8).build());
            out.accept(r("smith_" + m[0] + "_cannonball_double", MenuType.SMELTING, "8 " + m[0] + " cannonballs (double mould)",
                    Skill.SMITHING, lvl, xp * 2).in(m[1], 2).out(m[0] + "_cannonball", 8).tool("double_ammo_mould").ticks(8).build());
        }
        // Chain (34 Smithing, 37.5 XP, a steel bar) and the chainshot / incendiary cannonballs, 2 a time at an anvil.
        out.accept(r("smith_chain", MenuType.SMITHING, "Chain", Skill.SMITHING, 34, 37.5).in("steel_bar", 1).out("chain", 1)
                .tool("hammer").ticks(5).build());
        for (SailingCrew.Special sp : SailingCrew.SPECIALS) {
            out.accept(r("smith_" + sp.item(), MenuType.SMITHING, "2 x " + sp.name(), Skill.SMITHING, sp.smithing(), sp.xp())
                    .in(sp.base(), 2).in(sp.chainshot() ? "chain" : "rubium_splinters", sp.chainshot() ? 1 : 10).out(sp.item(), 2)
                    .tool("hammer").ticks(sp.chainshot() ? 2 : 3).build());
        }
        // Cupronickel bar: 2 copper ore and 1 nickel ore at a furnace (74 Smithing, 42 XP).
        out.accept(r("smelt_cupronickel_bar", MenuType.SMELTING, "Cupronickel bar", Skill.SMITHING, 74, SailingCrew.CUPRONICKEL_XP)
                .in("copper_ore", 2).in("nickel_ore", 1).out("cupronickel_bar", 1).ticks(4).build());
        // Sailing woods at the sawmill (wiki fees).
        for (Object[] w : SailingCrew.SAWMILL) {
            out.accept(plain("saw_" + w[0], MenuType.CONSTRUCTION, title((String) w[0]) + " (" + w[2] + " coins)").in((String) w[1], 1)
                    .in("coins", (Integer) w[2]).out((String) w[0], 1).ticks(0).build());
        }
        Object[][] shop = {{"spyglass", 50}, {"salvaging_hook", 40}, {"trawling_net", 60}, {"rope", 18}, {"crowbar", 30},
                {"current_duck", 40}, {"diving_apparatus", 200}, {"portable_weather_station", 100}};
        for (Object[] o : shop) {
            out.accept(plain("shop_tools_" + o[0], MenuType.SHOP_TOOLS, title((String) o[0]) + " - " + o[1] + " gp")
                    .in("coins", (Integer) o[1]).out((String) o[0], 1).ticks(0).build());
        }
        for (Object[] o : SHIPWRIGHT_SUPPLIES) {
            if (!o[0].equals("swamp_paste")) {
                out.accept(plain("shop_tools_" + o[0], MenuType.SHOP_TOOLS, title((String) o[0]) + " - " + o[1] + " gp")
                        .in("coins", (Integer) o[1]).out((String) o[0], 1).ticks(0).build());
            }
        }
    }

    /**
     * Shipwright supplies: Sailing materials that have no source in RuneCraft's world yet (approx prices). The Sailing woods
     * (camphor, ironwood and rosewood trees), lead (lead rocks) and cupronickel (nickel rocks and copper) are gathered on
     * the islands and sawn or smelted instead.
     */
    public static final Object[][] SHIPWRIGHT_SUPPLIES = {{"bolt_of_linen", 150}, {"bolt_of_canvas", 400}, {"bolt_of_cotton", 900}, {"dragon_nails", 1500},
            {"dragon_metal_sheet", 30000}, {"swamp_paste", 30}};

    @Override
    public List<NpcDef> npcs() {
        List<NpcDef> list = new ArrayList<>();
        for (SailingCrew.Crewmate c : SailingCrew.CREW) {
            list.add(NpcDef.of(c.id(), c.name(), c.placeId(), 0, 0, 0xC8A080, 0x2A4A7A, 0x3A3A4A, 0x6A4A2A));
        }
        return list;
    }

    @Override
    public List<BlockDef> blocks() {
        List<BlockDef> list = new ArrayList<>();
        for (Wreck w : WRECKS) {
            list.add(BlockDef.scripted(w.blockId(), w.name(), 0x5A4A3A, "shipwreck"));
        }
        list.add(BlockDef.scripted("sea_chart_marker", "Charting buoy", 0xD04030, "buoy"));
        list.add(BlockDef.scripted("port_salvaging_station", "Salvaging station", 0x7A6A4A, "crate"));
        list.add(BlockDef.scripted("boat_schematics", "Boat schematics", 0x6A5A3A, "notice_board"));
        list.add(BlockDef.scripted("barracuda_trial_buoy", "Barracuda trial master's buoy", 0x3080C0, "buoy"));
        list.add(BlockDef.scripted(SailingLockboxes.BLOCK, "Lockbox", 0x6A4A2A, "chest"));
        for (Port p : PORTS) {
            list.add(BlockDef.scripted(p.board(), p.name() + " port task board", 0x8A6A3A, "notice_board"));
        }
        return list;
    }

    @Override
    public List<PlaceDef> places() {
        List<PlaceDef> list = new ArrayList<>();
        // Shipwrecks sit out at sea; SkillingHooks places the wreck block on the water surface.
        // One of each type's wiki map pins (OSRS has 6 spots per area and several areas per type).
        int[][] wreckCoords = {{3095, 2928}, {2752, 3324}, {2477, 2964}, {2809, 3996}, {2659, 2556}, {2114, 2217}, {2571, 4152},
                {1861, 2163}};
        for (int i = 0; i < WRECKS.size(); i++) {
            Wreck w = WRECKS.get(i);
            list.add(PlaceDef.of(w.blockId() + "_site", w.name(), wreckCoords[i][0], wreckCoords[i][1], PlaceDef.Style.NONE, List.of()));
            for (int j = 0; j < EXTRA_WRECK_SITES[i].length; j += 2) {
                list.add(PlaceDef.of(w.blockId() + "_site_" + (j / 2 + 2), w.name(), EXTRA_WRECK_SITES[i][j], EXTRA_WRECK_SITES[i][j + 1],
                        PlaceDef.Style.NONE, List.of()));
            }
        }
        for (SailingTraining.ChartTask c : SailingTraining.CHARTS) {
            if (!c.underground()) {
                // Task 45's buoy would sit on the Taverley Dungeon entrance; it moves a few tiles west, onto the water.
                int dx = c.id() == 45 ? -6 : 0;
                list.add(PlaceDef.of(c.placeId(), "Charting: " + c.sea(), c.x() + dx, c.y(), PlaceDef.Style.NONE, List.of()));
            }
        }
        // The Current Affairs quest charts the duck's course near Entrana (old charting point).
        list.add(PlaceDef.of("chart_entrana", "Charting point: Entrana", 2830, 3360, PlaceDef.Style.NONE, List.of()));
        for (SailingTraining.Trial t : SailingTraining.TRIALS) {
            list.add(PlaceDef.of(t.placeId(), t.name(), t.objectiveB()[0], t.objectiveB()[1], PlaceDef.Style.NONE, List.of()));
        }
        list.addAll(SailingLockboxes.places());
        // Crewmates wait at their wiki towns (tiles approx); talking to one recruits them.
        for (SailingCrew.Crewmate c : SailingCrew.CREW) {
            list.add(PlaceDef.of(c.placeId(), c.name(), c.x(), c.y(), PlaceDef.Style.NONE, List.of()));
        }
        for (int i = 0; i < PORTS.size(); i++) {
            Port p = PORTS.get(i);
            // Older ports keep their camp plaza; the rest get a board (and schematics) placed by SailingHooks.
            list.add(PlaceDef.of(p.id() + "_docks", p.name() + " docks", p.x(), p.y(), i < CAMP_PORTS ? PlaceDef.Style.CAMP
                    : PlaceDef.Style.NONE, i < CAMP_PORTS ? List.of(p.board()) : List.of()));
        }
        return list;
    }
}
