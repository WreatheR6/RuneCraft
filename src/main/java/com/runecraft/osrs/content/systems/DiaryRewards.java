package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.Skill;

import java.util.ArrayList;
import java.util.List;

/**
 * Effects of the achievement diary reward items (tiers 1-4 = easy..elite), after the OSRS wiki: teleports
 * with their daily charges, the Falador shield prayer restore, the Explorer's ring run-energy restores and
 * alchemy, and bonus resources (Varrock armour ores/bars, Rada's blessing fish). Perks the game has no
 * mechanic for are listed in {@link #perks} only.
 */
public final class DiaryRewards {
    /** A reward teleport. {@code daily[tier-1]}: -1 = not available at that tier, 0 = unlimited, n = uses per day. */
    public record Tele(String area, Teleports.Destination dest, int[] daily) {
        public boolean available(int tier) {
            return tier >= 1 && tier <= 4 && daily[tier - 1] >= 0;
        }

        /** Uses per day at the tier (0 = unlimited). */
        public int perDay(int tier) {
            return daily[tier - 1];
        }

        /** Daily-limit key shared by every tier of the item. */
        public String key() {
            return "diary_tele:" + area + ":" + dest.name();
        }
    }

    public static final List<Tele> TELES = List.of(
            new Tele("ardougne", Teleports.at("Ardougne Monastery", 2606, 3222), new int[]{0, 0, 0, 0}),
            new Tele("ardougne", Teleports.at("Ardougne Farm", 2673, 3375), new int[]{-1, 3, 5, 0}),
            new Tele("desert", Teleports.at("Nardah", 3428, 2892), new int[]{-1, 1, 0, 0}),
            new Tele("desert", Teleports.at("Kalphite Cave", 3321, 3122), new int[]{-1, -1, -1, 0}),
            new Tele("fremennik", Teleports.at("Rellekka", 2660, 3657), new int[]{-1, 1, 3, 0}), // approx
            new Tele("kandarin", Teleports.at("Sherlock", 2733, 3415), new int[]{-1, -1, 1, 0}),
            new Tele("karamja", Teleports.at("Shilo gem mine", 2825, 2997), new int[]{-1, -1, 0, 0}),
            new Tele("karamja", Teleports.at("Duradel", 2869, 2982), new int[]{-1, -1, -1, 0}),
            new Tele("kourend", Teleports.at("Kourend Woodland", 1552, 3456), new int[]{3, 5, 0, 0}),
            new Tele("kourend", Teleports.at("Mount Karuulm", 1311, 3795), new int[]{-1, -1, 3, 0}),
            new Tele("lumbridge", Teleports.at("Cabbage patch", 3052, 3289), new int[]{-1, 3, 0, 0}),
            new Tele("morytania", Teleports.at("Ectofuntus", 3660, 3522), new int[]{-1, 5, 0, 0}),
            new Tele("morytania", Teleports.at("Burgh de Rott", 3482, 3231), new int[]{-1, 5, 0, 0}),
            new Tele("western", Teleports.at("Piscatoris Fishing Colony", 2340, 3650), new int[]{-1, -1, 1, 0}), // approx
            new Tele("wilderness", Teleports.at("Fountain of Rune", 3375, 3893), new int[]{-1, -1, 1, 0}));

    private DiaryRewards() {
    }

    /** The diary area of a reward item id ({@code <base>_<tier>}), or null. */
    public static Diaries.Area areaOf(String itemId) {
        int tier = tierOf(itemId);
        if (tier == 0) {
            return null;
        }
        String base = itemId.substring(0, itemId.length() - 2);
        for (Diaries.Area a : Diaries.AREAS) {
            if (a.rewardBase().equals(base)) {
                return a;
            }
        }
        return null;
    }

    /** Reward tier 1-4 from the id suffix, or 0. */
    public static int tierOf(String itemId) {
        if (itemId == null || itemId.length() < 3 || itemId.charAt(itemId.length() - 2) != '_') {
            return 0;
        }
        char c = itemId.charAt(itemId.length() - 1);
        return c >= '1' && c <= '4' ? c - '0' : 0;
    }

    /** Teleports a reward item offers at its tier. */
    public static List<Tele> teleports(String areaId, int tier) {
        List<Tele> out = new ArrayList<>();
        for (Tele t : TELES) {
            if (t.area().equals(areaId) && t.available(tier)) {
                out.add(t);
            }
        }
        return out;
    }

    public static List<Teleports.Destination> destinations(String areaId, int tier) {
        List<Teleports.Destination> out = new ArrayList<>();
        for (Tele t : teleports(areaId, tier)) {
            out.add(t.dest());
        }
        return out;
    }

    /** Falador shield: percent of the Prayer level restored, and uses per day. */
    public static int faladorRestorePercent(int tier) {
        return tier <= 1 ? 50 : 100;
    }

    public static int faladorRestoresPerDay(int tier) {
        return tier >= 4 ? 2 : 1;
    }

    /** Explorer's ring: 50% run energy restores per day and whether its 30 daily alchemy casts are High Alchemy. */
    public static int explorerEnergyRestores(int tier) {
        return switch (tier) {
            case 1 -> 2;
            case 2, 3 -> 3;
            default -> 4;
        };
    }

    public static final int EXPLORER_ALCHS_PER_DAY = 30;

    public static boolean explorerHighAlch(int tier) {
        return tier >= 2;
    }

    /** Varrock armour: highest ore it can double when mining (10% chance), by tier. */
    private static final String[][] VARROCK_ORES = {
            {"clay", "copper_ore", "tin_ore", "iron_ore", "silver_ore", "coal", "rune_essence", "pure_essence"},
            {"gold_ore", "mithril_ore", "granite_500g", "granite_2kg", "granite_5kg", "sandstone_1kg"},
            {"adamantite_ore"},
            {"runite_ore", "amethyst"}};
    private static final String[][] VARROCK_BARS = {
            {"bronze_bar", "iron_bar", "silver_bar", "steel_bar"}, {"gold_bar", "mithril_bar"}, {"adamantite_bar"}, {"runite_bar"}};

    public static final int VARROCK_BONUS_PERCENT = 10;

    public static boolean varrockOre(int tier, String item) {
        return inTiers(VARROCK_ORES, tier, item);
    }

    public static boolean varrockBar(int tier, String item) {
        return inTiers(VARROCK_BARS, tier, item);
    }

    private static boolean inTiers(String[][] table, int tier, String item) {
        for (int i = 0; i < Math.min(tier, table.length); i++) {
            for (String s : table[i]) {
                if (s.equals(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Rada's blessing: percent chance of an extra fish when fishing in Kourend & Kebos. */
    public static int radasExtraFishPercent(int tier) { // approx
        return tier <= 0 ? 0 : tier * 2;
    }

    /** The skill whose gathering the bonus applies to, for the Minecraft side. */
    public static Skill bonusSkill(String areaId) {
        return switch (areaId) {
            case "varrock" -> Skill.MINING;
            case "kourend" -> Skill.FISHING;
            default -> null;
        };
    }

    /** Short perk descriptions (OSRS effects), shown in the diary screen and the item examine text. */
    public static List<String> perks(String areaId, int tier) {
        List<String> out = new ArrayList<>();
        for (Tele t : teleports(areaId, tier)) {
            out.add("Teleport to " + t.dest().name() + (t.perDay(tier) == 0 ? " (unlimited)" : " (" + t.perDay(tier) + "/day)"));
        }
        switch (areaId) {
            case "ardougne" -> {
                if (tier >= 2) {
                    out.add("10% more Thieving success in Ardougne"); // approx: not simulated
                }
            }
            case "falador" -> out.add("Restore " + faladorRestorePercent(tier) + "% prayer, " + faladorRestoresPerDay(tier) + "/day");
            case "lumbridge" -> {
                out.add(explorerEnergyRestores(tier) + " run energy restores (50%) per day");
                out.add(EXPLORER_ALCHS_PER_DAY + " free " + (explorerHighAlch(tier) ? "High" : "Low") + " Alchemy casts per day");
            }
            case "varrock" -> out.add(VARROCK_BONUS_PERCENT + "% chance of an extra ore/bar up to "
                    + new String[]{"coal/steel", "mithril", "adamantite", "runite"}[Math.max(0, Math.min(3, tier - 1))]);
            case "kourend" -> out.add(radasExtraFishPercent(tier) + "% chance of an extra fish in Kourend & Kebos");
            case "karamja" -> out.add("Discount at TzHaar shops"); // approx: not simulated
            default -> {
            }
        }
        return out;
    }
}
