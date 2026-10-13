package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.Skill;

import java.util.ArrayList;
import java.util.List;

/**
 * Effects of the achievement diary reward items (tiers 1-4 = easy..elite), after the OSRS wiki: teleports
 * with their daily charges, the Falador shield prayer restore, the Explorer's ring run-energy restores and
 * alchemy, bonus resources (Varrock armour ores/bars, Rada's blessing fish), and perks hooked into other
 * systems (TzHaar discount, Seers' marks of grace, Hosidius range, Falador farm XP, noted boss/dragon bones,
 * staffless fairy rings, elite void). Perks the game has no mechanic for (coal trucks, the flax keeper,
 * chompy birds, the Resource Area fee, desert heat) are not simulated.
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
            new Tele("fremennik", Teleports.at("Rellekka", 2660, 3657), new int[]{1, 3, 5, 0}),
            new Tele("kandarin", Teleports.at("Sherlock", 2733, 3415), new int[]{-1, -1, 1, 0}),
            new Tele("karamja", Teleports.at("Shilo gem mine", 2825, 2997), new int[]{-1, -1, 0, 0}),
            new Tele("karamja", Teleports.at("Duradel", 2869, 2982), new int[]{-1, -1, -1, 0}),
            new Tele("kourend", Teleports.at("Kourend Woodland", 1552, 3456), new int[]{3, 5, 0, 0}),
            new Tele("kourend", Teleports.at("Mount Karuulm", 1311, 3795), new int[]{-1, -1, 3, 0}),
            new Tele("lumbridge", Teleports.at("Cabbage patch", 3052, 3289), new int[]{-1, 3, 0, 0}),
            new Tele("morytania", Teleports.at("Ectofuntus", 3660, 3522), new int[]{-1, 5, 5, 0}),
            new Tele("morytania", Teleports.at("Burgh de Rott", 3482, 3231), new int[]{-1, -1, 0, 0}),
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
        return switch (tier) {
            case 1 -> 25;
            case 2 -> 50;
            default -> 100;
        };
    }

    public static int faladorRestoresPerDay(int tier) {
        return tier >= 4 ? 2 : 1;
    }

    /**
     * Explorer's ring (OSRS wiki): run energy restores per day (2 / 3 / 4 of 50%, then 3 of 100% on ring 4) and
     * whether its 30 daily alchemy casts may be High Alchemy (ring 4 only; rings 1-3 cast Low Alchemy).
     */
    public static int explorerEnergyRestores(int tier) {
        return switch (tier) {
            case 1 -> 2;
            case 2 -> 3;
            case 3 -> 4;
            default -> 3;
        };
    }

    /** Percent of run energy each Explorer's ring restore gives. */
    public static int explorerEnergyPercent(int tier) {
        return tier >= 4 ? 100 : 50;
    }

    public static final int EXPLORER_ALCHS_PER_DAY = 30;

    public static boolean explorerHighAlch(int tier) {
        return tier >= 4;
    }

    /** Varrock armour: highest ore it can double when mining (10% chance), by tier. */
    private static final String[][] VARROCK_ORES = {
            {"clay", "copper_ore", "tin_ore", "iron_ore", "silver_ore", "coal", "gold_ore", "rune_essence", "pure_essence"},
            {"mithril_ore", "lovakite_ore", "granite_500g", "granite_2kg", "granite_5kg", "sandstone_1kg"},
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

    /** Rada's blessing (worn): percent chance of an extra fish anywhere (OSRS wiki: 2 / 4 / 6 / 8%). */
    public static int radasExtraFishPercent(int tier) {
        return tier <= 0 ? 0 : tier * 2;
    }

    /**
     * Ardougne diary pickpocketing bonus (OSRS): the medium diary gives 10% more pickpocket success in Ardougne,
     * the hard diary 10% more everywhere. Returns the success-chance multiplier.
     */
    public static double pickpocketMultiplier(boolean mediumDone, boolean hardDone, boolean inArdougne) {
        return hardDone || (mediumDone && inArdougne) ? 1.1 : 1.0;
    }

    /** Morytania elite diary: 50% more runes from the Barrows chest. */
    public static double barrowsRuneMultiplier(boolean eliteDone) {
        return eliteDone ? 1.5 : 1.0;
    }

    // ------------------------------------------------------------------ perks hooked into other systems

    /**
     * Karamja gloves (any tier, worn): TzHaar shop prices drop by 13.33% (OSRS wiki: obsidian platebody
     * 126,000 -> 109,200 tokkul; obsidian cape 90,000 -> 78,000).
     */
    public static int tzhaarPrice(int price, boolean karamjaGloves) {
        return karamjaGloves ? price - price * 2 / 15 : price;
    }

    /** Fraction of a TzHaar shop price paid (for shops that scale a whole cost list). */
    public static final double TZHAAR_GLOVES_PRICE = 13 / 15.0;

    /**
     * Seers' Village Rooftop Course marks of grace (OSRS wiki): a failed mark spawn is rerolled with a 5% /
     * 10% / 15% chance after the easy / medium / hard Kandarin diary (the elite keeps the hard bonus).
     */
    public static double seersMarkChance(double base, int kandarinTiersDone) {
        double reroll = switch (Math.min(3, Math.max(0, kandarinTiersDone))) {
            case 1 -> 0.05;
            case 2 -> 0.10;
            case 3 -> 0.15;
            default -> 0;
        };
        return base + (1 - base) * reroll;
    }

    /**
     * Hosidius kitchen range (OSRS wiki): 5% less burning once the easy Kourend &amp; Kebos diary is done,
     * 10% with the elite diary; no bonus before the easy diary.
     */
    public static double hosidiusBurnReduction(boolean easyDone, boolean eliteDone) {
        return eliteDone ? 0.10 : easyDone ? 0.05 : 0;
    }

    /** The Falador farm patches outside the south wall (OSRS tiles, inclusive). */
    public static final int[] FALADOR_FARM = {3042, 3302, 3060, 3310};

    /**
     * Falador medium diary (shield 2+, OSRS wiki): 10% more Farming XP from the Falador farm's allotment,
     * flower and herb patches. Returns the XP multiplier. // approx: the shield need not be worn here
     */
    public static double faladorFarmXp(boolean mediumDone, int osrsX, int osrsY) {
        int[] f = FALADOR_FARM;
        boolean atFarm = osrsX >= f[0] && osrsX <= f[2] && osrsY >= f[1] && osrsY <= f[3];
        return mediumDone && atFarm ? 1.1 : 1.0;
    }

    /** Tier checks for {@link #notedDrop}: (area id, tier) -> completed. */
    @FunctionalInterface
    public interface TierDone {
        boolean done(String area, Diaries.Tier tier);
    }

    /**
     * Whether a monster drop arrives as bank notes (OSRS wiki): the Giant Mole's mole skins and claws after the
     * hard Falador diary, the Dagannoth Kings' dagannoth bones after the elite Fremennik diary, and dragon bones
     * (and lava dragon bones) from dragons killed in the Wilderness after the elite Wilderness diary.
     */
    public static boolean notedDrop(String monsterId, String item, boolean inWilderness, TierDone done) {
        if (monsterId == null || item == null) {
            return false;
        }
        if (monsterId.equals("giant_mole") && (item.equals("mole_skin") || item.equals("mole_claw"))) {
            return done.done("falador", Diaries.Tier.HARD);
        }
        if ((monsterId.equals("dagannoth_rex") || monsterId.equals("dagannoth_prime") || monsterId.equals("dagannoth_supreme"))
                && item.equals("dagannoth_bones")) {
            return done.done("fremennik", Diaries.Tier.ELITE);
        }
        if (inWilderness && (item.equals("dragon_bones") || item.equals("lava_dragon_bones"))) {
            return done.done("wilderness", Diaries.Tier.ELITE);
        }
        return false;
    }

    /** Lumbridge &amp; Draynor elite diary: fairy rings work without a Dramen or Lunar staff (OSRS). */
    public static boolean fairyRingsWithoutStaff(boolean lumbridgeEliteDone) {
        return lumbridgeEliteDone;
    }

    /** Pest Control rewards that need a diary: the elite void top and robe need the hard Western Provinces diary. */
    public static boolean pestControlUnlocked(String item, boolean westernHardDone) {
        return !(item.equals("elite_void_top") || item.equals("elite_void_robe")) || westernHardDone;
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
                if (tier == 2) {
                    out.add("10% more pickpocket success in Ardougne");
                } else if (tier >= 3) {
                    out.add("10% more pickpocket success everywhere");
                }
            }
            case "falador" -> {
                out.add("Restore " + faladorRestorePercent(tier) + "% prayer, " + faladorRestoresPerDay(tier) + "/day");
                if (tier >= 2) {
                    out.add("10% more Farming XP at the Falador farm patches");
                }
                if (tier >= 3) {
                    out.add("The Giant Mole drops noted mole skins and claws");
                }
            }
            case "lumbridge" -> {
                out.add(explorerEnergyRestores(tier) + " run energy restores (" + explorerEnergyPercent(tier) + "%) per day");
                out.add(EXPLORER_ALCHS_PER_DAY + " free " + (explorerHighAlch(tier) ? "High" : "Low") + " Alchemy casts per day");
                if (tier >= 4) {
                    out.add("Use fairy rings without a Dramen staff");
                }
            }
            case "varrock" -> out.add(VARROCK_BONUS_PERCENT + "% chance of an extra ore/bar up to "
                    + new String[]{"gold/steel", "lovakite/mithril", "adamantite", "runite"}[Math.max(0, Math.min(3, tier - 1))]);
            case "kourend" -> {
                out.add(radasExtraFishPercent(tier) + "% chance of an extra fish (worn, anywhere)");
                out.add((tier >= 4 ? 10 : 5) + "% less burning on the Hosidius kitchen range");
            }
            case "morytania" -> {
                if (tier >= 4) {
                    out.add("50% more runes from the Barrows chest");
                }
            }
            case "karamja" -> out.add("13% off TzHaar shop prices while wearing the gloves");
            case "kandarin" -> out.add((Math.min(3, tier) * 5) + "% reroll on failed Seers' Village rooftop marks of grace");
            case "fremennik" -> {
                if (tier >= 4) {
                    out.add("The Dagannoth Kings drop noted dagannoth bones");
                }
            }
            case "wilderness" -> {
                if (tier >= 4) {
                    out.add("Dragons killed in the Wilderness drop noted bones");
                }
            }
            case "western" -> {
                if (tier >= 3) {
                    out.add("Elite void knight equipment available from Pest Control");
                }
            }
            default -> {
            }
        }
        return out;
    }
}
