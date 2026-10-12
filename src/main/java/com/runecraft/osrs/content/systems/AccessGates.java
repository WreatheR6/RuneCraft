package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Skill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * OSRS area and guild access requirements, as one data table of gates. Each gate is a set of areas in
 * OSRS tile coordinates (rectangles taken from the hand-built structures, circles for round towers and
 * small places, a polygon for Morytania) plus the requirements checked when a player <em>enters</em> one
 * of them. Like OSRS, a player who got in is not thrown out when a boost wears off; someone who logs in
 * inside without meeting the requirements is moved out.
 *
 * <p>The Minecraft side ({@code AccessGateService}) checks each player's tile every couple of ticks and
 * pushes a refused player back to their last allowed position (or the gate's exit point) with the
 * gate's message. Creative and spectator players are never blocked.
 */
public final class AccessGates {
    private AccessGates() {
    }

    // ------------------------------------------------------------------ player view

    /** What a requirement may ask about a player (implemented on the Minecraft side and by tests). */
    public interface PlayerView {
        /** Current (boostable) level, like OSRS guild doors. */
        int level(Skill skill);

        int questStage(String questId);

        boolean questComplete(String questId);

        int questPoints();

        /** Worn in an equipment slot (head, body, legs, feet, weapon, shield). */
        boolean wearing(String itemId);

        /** Worn or anywhere in the inventory. */
        boolean carrying(String itemId);

        /** Carrying or wearing weapons or armour (the Entrana rule, see {@link #entranaBanned}). */
        boolean hasCombatGear();

        /** Whether every task of an achievement diary tier is done. */
        default boolean diaryComplete(String area, Diaries.Tier tier) {
            return false;
        }
    }

    // ------------------------------------------------------------------ areas

    /** An area in OSRS tile coordinates (fractional tiles allowed: block x / 2 + origin). */
    public interface Area {
        boolean contains(double x, double y);

        /** A tile just outside the area, near (x, y). */
        double[] exitNear(double x, double y);
    }

    /** Inclusive tile rectangle. */
    public record Rect(int x1, int y1, int x2, int y2) implements Area {
        public Rect {
            if (x1 > x2 || y1 > y2) {
                throw new IllegalArgumentException("bad rect");
            }
        }

        @Override
        public boolean contains(double x, double y) {
            return x >= x1 && x < x2 + 1 && y >= y1 && y < y2 + 1;
        }

        @Override
        public double[] exitNear(double x, double y) {
            double west = x - x1;
            double east = x2 + 1 - x;
            double south = y - y1;
            double north = y2 + 1 - y;
            double min = Math.min(Math.min(west, east), Math.min(south, north));
            if (min == west) {
                return new double[]{x1 - 2.5, y};
            } else if (min == east) {
                return new double[]{x2 + 3.5, y};
            } else if (min == south) {
                return new double[]{x, y1 - 2.5};
            }
            return new double[]{x, y2 + 3.5};
        }
    }

    /** Circle of {@code r} tiles around tile centre (cx, cy). */
    public record Circle(double cx, double cy, double r) implements Area {
        @Override
        public boolean contains(double x, double y) {
            double dx = x - cx;
            double dy = y - cy;
            return dx * dx + dy * dy <= r * r;
        }

        @Override
        public double[] exitNear(double x, double y) {
            double dx = x - cx;
            double dy = y - cy;
            double d = Math.sqrt(dx * dx + dy * dy);
            if (d < 1e-6) {
                dx = 0;
                dy = -1;
                d = 1;
            }
            double k = (r + 2.5) / d;
            return new double[]{cx + dx * k, cy + dy * k};
        }
    }

    /** Simple polygon (x0, y0, x1, y1, ...) with an explicit exit (no meaningful "nearest edge"). */
    public record Polygon(double[] pts, double exitX, double exitY) implements Area {
        @Override
        public boolean contains(double x, double y) {
            boolean in = false;
            int n = pts.length / 2;
            for (int i = 0, j = n - 1; i < n; j = i++) {
                double xi = pts[2 * i];
                double yi = pts[2 * i + 1];
                double xj = pts[2 * j];
                double yj = pts[2 * j + 1];
                if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
                    in = !in;
                }
            }
            return in;
        }

        @Override
        public double[] exitNear(double x, double y) {
            return new double[]{exitX, exitY};
        }
    }

    // ------------------------------------------------------------------ requirements

    /** A requirement: returns the refusal message, or null when met. */
    @FunctionalInterface
    public interface Req {
        String unmet(PlayerView p);
    }

    public static Req level(Skill skill, int lvl, String message) {
        return p -> p.level(skill) >= lvl ? null : message;
    }

    public static Req questDone(String questId, String message) {
        return p -> p.questComplete(questId) ? null : message;
    }

    public static Req questStarted(String questId, String message) {
        return p -> p.questStage(questId) > 0 || p.questComplete(questId) ? null : message;
    }

    public static Req questPoints(int qp, String message) {
        return p -> p.questPoints() >= qp ? null : message;
    }

    /** Wearing one of {@code worn}, or carrying one of {@code carried} (skill capes, diary armour). */
    public static Req wearAny(List<String> worn, List<String> carried, String message) {
        return p -> {
            for (String id : worn) {
                if (p.wearing(id)) {
                    return null;
                }
            }
            for (String id : carried) {
                if (p.carrying(id)) {
                    return null;
                }
            }
            return message;
        };
    }

    public static Req carryAny(List<String> ids, String message) {
        return p -> {
            for (String id : ids) {
                if (p.carrying(id)) {
                    return null;
                }
            }
            return message;
        };
    }

    /** Met when either requirement is met (the first one's message is shown). */
    public static Req either(Req a, Req b) {
        return p -> a.unmet(p) == null || b.unmet(p) == null ? null : a.unmet(p);
    }

    public static Req diary(String area, Diaries.Tier tier, String message) {
        return p -> p.diaryComplete(area, tier) ? null : message;
    }

    public static Req noCombatGear(String message) {
        return p -> p.hasCombatGear() ? message : null;
    }

    /** Warriors' Guild: combined Attack and Strength of 130, or 99 in either. */
    public static Req warriors(String message) {
        return p -> {
            int a = p.level(Skill.ATTACK);
            int s = p.level(Skill.STRENGTH);
            return a + s >= 130 || a >= 99 || s >= 99 ? null : message;
        };
    }

    // ------------------------------------------------------------------ gates

    /**
     * One gated area. {@code exitX/exitY} (OSRS tile, or NaN) is where a refused player with no last
     * allowed position is put (islands, realms); otherwise the nearest point outside the area is used.
     */
    public record Gate(String id, String name, List<Area> areas, List<Req> reqs, double exitX, double exitY) {
        public boolean contains(double x, double y) {
            for (Area a : areas) {
                if (a.contains(x, y)) {
                    return true;
                }
            }
            return false;
        }

        /** First unmet requirement's message, or null when the player may enter. */
        public String check(PlayerView p) {
            for (Req r : reqs) {
                String m = r.unmet(p);
                if (m != null) {
                    return m;
                }
            }
            return null;
        }

        /** Where to put a player found inside without access. */
        public double[] exit(double x, double y) {
            if (!Double.isNaN(exitX)) {
                return new double[]{exitX, exitY};
            }
            for (Area a : areas) {
                if (a.contains(x, y)) {
                    double[] out = a.exitNear(x, y);
                    // adjacent areas of the same gate: keep walking the same way until outside all of them
                    double dx = out[0] - x;
                    double dy = out[1] - y;
                    double len = Math.max(1e-6, Math.sqrt(dx * dx + dy * dy));
                    for (int i = 0; i < 400 && contains(out[0], out[1]); i++) {
                        out[0] += dx / len;
                        out[1] += dy / len;
                    }
                    return out;
                }
            }
            return new double[]{x, y};
        }
    }

    public static final String CHEFS_HAT = "chefs_hat";

    private static final List<Gate> GATES = new ArrayList<>();

    private static void gate(String id, String name, List<Area> areas, double exitX, double exitY, Req... reqs) {
        GATES.add(new Gate(id, name, List.copyOf(areas), List.of(reqs), exitX, exitY));
    }

    private static void gate(String id, String name, List<Area> areas, Req... reqs) {
        gate(id, name, areas, Double.NaN, Double.NaN, reqs);
    }

    /**
     * Morytania east of the River Salve: from the north coast down the river (GielinorGeography's
     * Salve plus its half-width) past Paterdomus and Mort Myre, south to the Morytanian ridge and east
     * to the coast. Paterdomus itself (west bank) stays open; the Slayer Tower is inside.
     */
    static final double[] MORYTANIA = {
            3401, 3640, 3401, 3560, 3407, 3529, 3435, 3512, 3446, 3486, 3446, 3440, 3434, 3390, 3428, 3345,
            3428, 3150, 3800, 3150, 3800, 3640};

    static {
        // ---- regions
        gate("morytania", "Morytania", List.of(new Polygon(MORYTANIA, 3398, 3487)), 3398, 3487,
                questDone("priest_in_peril", "A holy barrier stops you crossing the Salve. You need to complete Priest in Peril to enter Morytania."));
        gate("entrana", "Entrana", List.of(new Rect(2800, 3300, 2870, 3400)), 3029, 3217,
                noCombatGear("The monks of Entrana won't let you bring weapons or armour onto their holy island."));
        gate("zanaris", "Zanaris", List.of(new Rect(2368, 4380, 2464, 4480), new Circle(3168.5, 3158.5, 8)), 3202, 3166,
                questDone("lost_city", "Nothing interesting happens. (You need to complete Lost City to enter Zanaris.)"),
                // The elite Lumbridge & Draynor diary lets fairy rings work without a staff (so arriving by ring is allowed).
                either(carryAny(List.of("dramen_staff", "lunar_staff"), "Nothing interesting happens. You need to wield a Dramen staff to enter Zanaris."),
                        diary("lumbridge", Diaries.Tier.ELITE, "")));
        gate("fossil_island", "Fossil Island", List.of(new Rect(3640, 3700, 3860, 3910)), 3362, 3445,
                questDone("bone_voyage", "The barge won't take you to Fossil Island until you have completed Bone Voyage."));
        gate("prifddinas", "Prifddinas", List.of(new Rect(3196, 6036, 3332, 6172)), 2240, 3285,
                questDone("song_of_the_elves", "The crystal gates of Prifddinas stay sealed. You need to complete Song of the Elves."));
        gate("ape_atoll", "Ape Atoll", List.of(new Rect(2690, 2690, 2820, 2820)), 2465, 3495,
                questStarted("monkey_madness_i", "You can't reach Ape Atoll yet. Start Monkey Madness I first."));
        gate("lunar_isle", "Lunar Isle", List.of(new Rect(2060, 3850, 2180, 3970)), 2213, 3800,
                questStarted("lunar_diplomacy", "The Lunar Isle is hidden from you. Start Lunar Diplomacy first."));

        // ---- guilds (rectangles are the hand-built buildings / grounds in osrs/content/world)
        gate("heroes_guild", "Heroes' Guild", List.of(new Rect(2890, 3504, 2901, 3516)),
                questDone("heroes_quest", "You need to complete Heroes' Quest to enter the Heroes' Guild."));
        gate("legends_guild", "Legends' Guild", List.of(new Rect(2716, 3346, 2744, 3390)),
                questDone("legends_quest", "You need to complete Legends' Quest to enter the Legends' Guild."));
        gate("champions_guild", "Champions' Guild", List.of(new Rect(3187, 3352, 3199, 3364)),
                questPoints(32, "You need 32 quest points to enter the Champions' Guild."));
        gate("fishing_guild", "Fishing Guild", List.of(new Rect(2586, 3390, 2620, 3440)),
                level(Skill.FISHING, 68, "You need a Fishing level of 68 to enter the Fishing Guild."));
        gate("crafting_guild", "Crafting Guild", List.of(new Rect(2926, 3283, 2939, 3292), new Rect(2926, 3272, 2944, 3281)),
                level(Skill.CRAFTING, 40, "You need a Crafting level of 40 to enter the Crafting Guild."),
                wearAny(List.of("brown_apron", "golden_apron"), List.of("crafting_cape", "crafting_cape_t"),
                        "You need to wear a brown apron to enter the Crafting Guild."));
        gate("mining_guild", "Mining Guild", List.of(new Rect(3014, 3333, 3027, 3344)),
                level(Skill.MINING, 60, "Sorry, but you need level 60 Mining to go in there."));
        // the fenced grounds around the tower: centre tile (2665, 3433), 28-block (14-tile) fence ring
        gate("ranging_guild", "Ranging Guild", List.of(new Circle(2665.5, 3433.5, 14.5)),
                level(Skill.RANGED, 40, "You need a Ranged level of 40 to enter the Ranging Guild."));
        // the tower: centre (2591, 3086), 8-block (4-tile) radius
        gate("wizards_guild", "Wizards' Guild", List.of(new Circle(2591.5, 3086.5, 4.5)),
                level(Skill.MAGIC, 66, "You need a Magic level of 66 to enter the Wizards' Guild."));
        gate("cooks_guild", "Cooks' Guild", List.of(new Rect(3138, 3439, 3148, 3448)),
                level(Skill.COOKING, 32, "You need a Cooking level of 32 to enter the Cooks' Guild."),
                wearAny(List.of(CHEFS_HAT, "golden_chef_hat"), List.of("cooking_cape", "cooking_cape_t", "varrock_armour_3", "varrock_armour_4"),
                        "You need to wear a chef's hat to enter the Cooks' Guild."));
        // Kourend favour was removed from OSRS in 2023: 60 Woodcutting is the only requirement now.
        gate("woodcutting_guild", "Woodcutting Guild", List.of(new Rect(1586, 3478, 1658, 3514)),
                level(Skill.WOODCUTTING, 60, "You need a Woodcutting level of 60 to enter the Woodcutting Guild."));
        // The guild is one plaza here; the 65/85 tiers are enforced per patch (farmingGuildTier).
        gate("farming_guild", "Farming Guild", List.of(new Circle(1249.5, 3726.5, 7)),
                level(Skill.FARMING, 45, "You need a Farming level of 45 to enter the Farming Guild."));
        gate("warriors_guild", "Warriors' Guild", List.of(new Rect(2852, 3532, 2871, 3554)),
                warriors("You need a combined Attack and Strength level of 130, or 99 in either, to enter the Warriors' Guild."));
        gate("myths_guild", "Myths' Guild", List.of(new Circle(2458.5, 2847.5, 6)), // approx footprint around the place
                questStarted("dragon_slayer_ii", "You need to have started Dragon Slayer II to enter the Myths' Guild."));
    }

    public static List<Gate> all() {
        return Collections.unmodifiableList(GATES);
    }

    public static Gate byId(String id) {
        for (Gate g : GATES) {
            if (g.id().equals(id)) {
                return g;
            }
        }
        return null;
    }

    /** The first gate containing tile (x, y), or null. */
    public static Gate at(double x, double y) {
        for (Gate g : GATES) {
            if (g.contains(x, y)) {
                return g;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ Farming Guild tiers

    /** Farming Guild tiers by patch (OSRS: beginner 45, intermediate west wing 65, advanced north wing 85). */
    private static final Map<String, Integer> FARMING_TIERS = Map.ofEntries(
            Map.entry("allotment_patch", 45), Map.entry("flower_patch", 45), Map.entry("bush_patch", 45),
            Map.entry("cactus_patch", 45), Map.entry("compost_bin", 45),
            Map.entry("herb_patch", 65), Map.entry("tree_patch", 65), Map.entry("anima_patch", 65), Map.entry("hespori_patch", 65),
            Map.entry("fruit_tree_patch", 85), Map.entry("spirit_tree_patch", 85), Map.entry("celastrus_patch", 85),
            Map.entry("redwood_patch", 85));

    /** Farming level needed to use this patch block inside the Farming Guild (0 = no extra requirement). */
    public static int farmingGuildTier(String blockId) {
        return FARMING_TIERS.getOrDefault(blockId, 0);
    }

    // ------------------------------------------------------------------ Entrana

    private static final List<EquipmentDef.Slot> ENTRANA_SLOTS = List.of(EquipmentDef.Slot.HEAD, EquipmentDef.Slot.CHEST,
            EquipmentDef.Slot.LEGS, EquipmentDef.Slot.FEET, EquipmentDef.Slot.WEAPON, EquipmentDef.Slot.SHIELD);

    /**
     * Whether the monks of Entrana refuse this item: any weapon or armour piece with combat bonuses.
     * Woodcutting axes, pickaxes and other tools, robes without bonuses, and jewellery are allowed
     * (approx: OSRS keeps a hand-made exception list).
     */
    public static boolean entranaBanned(ItemDef def) {
        if (def == null || def.equipment() == null) {
            return false;
        }
        switch (def.kind()) {
            case AXE, PICKAXE, TOOL, ARROW -> {
                return false;
            }
            default -> {
            }
        }
        if (!ENTRANA_SLOTS.contains(def.equipment().slot())) {
            return false;
        }
        EquipmentStats s = def.equipment().stats();
        return s != null && !s.equals(EquipmentStats.NONE);
    }
}
