package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.Region;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.data.Boss;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

/**
 * Boss tasks ("Like a Boss", 200 points; OSRS Wiki "Boss" page, section "Boss slayer", July 2026): Konar, Nieve, Steve and
 * Duradel may assign any of the bosses below (never the Corporeal Beast, Nex, the Nightmare, Yama, the Moons, the
 * Hueycoatl, Amoxliatl, the Royal Titans, Scurrius, Hespori, Obor, Bryophyta, Skotizo, the Mimic, Doom or a raid boss),
 * Krystilia only the Wilderness ones (Callisto, the Chaos Elemental and Fanatic, the Crazy archaeologist, Scorpia,
 * Venenatis and Vet'ion; the King Black Dragon's lair is not in the Wilderness). Only Konar assigns the Alchemical Hydra,
 * and her Crazy archaeologist task doesn't take the Deranged archaeologist. A boss task is drawn from the masters'
 * "bosses" line (weight 8, Duradel 12), then one boss the player can reach (Slayer level, quests, skills) is picked with
 * equal chance; each boss is its own task category ({@code boss_<id>}) that counts kills of the listed creature ids. The
 * player then chooses the amount: 3-35 (Barrows brothers 3-36), +5 per unlocked Combat Achievement tier (Barrows +6).
 * Finishing one gives 5,000 bonus Slayer experience.
 */
public final class SlayerBossTasks {
    /** The meta category on masters' tables that turns into one of the boss tasks. */
    public static final String CATEGORY = "bosses";
    public static final String UNLOCK = "like_a_boss";
    /** Bonus Slayer experience for finishing any boss task, whatever its length (OSRS). */
    public static final double COMPLETION_XP = 5000;
    /** The fewest kills a player may choose for a boss task. */
    public static final int MIN_KILLS = 3;
    /** The Konar quo Maten master id: her boss tasks follow a few rules of their own. */
    public static final String KONAR = "konar";

    /**
     * @param id          task category id ({@code boss_<id>})
     * @param creatures   module boss or built-in boss ids whose kills count
     * @param slayerLevel Slayer level the boss itself needs (Sire 85, Cerberus 91...)
     * @param wilderness  Krystilia may assign it
     * @param max         most kills the player may choose before Combat Achievement tiers (35; Barrows brothers 36)
     * @param tierStep    kills each unlocked Combat Achievement tier adds to {@code max} (5; Barrows brothers 6)
     * @param quests      quests the master wants done first (RuneCraft quest ids)
     * @param skill       another skill the boss needs (God Wars generals: 70 Strength/Agility/Ranged/Hitpoints), or null
     * @param konarOnly   only Konar assigns it (the Alchemical Hydra)
     */
    public record BossTask(String id, String name, List<String> creatures, int slayerLevel, boolean wilderness, Region region,
                           int min, int max, int tierStep, List<String> quests, Skill skill, int skillLevel, boolean konarOnly) {
        /** The most kills the player may choose with {@code tiers} Combat Achievement tiers unlocked (0-6). */
        public int maxKills(int tiers) {
            return max + tierStep * Math.max(0, tiers);
        }
    }

    private static final Map<String, BossTask> ALL = new LinkedHashMap<>();

    /** Adds a boss task; the optional requirements are set on the returned builder. */
    private static Builder b(String key, String name, int slayer, boolean wildy, Region region, String... creatures) {
        return new Builder("boss_" + key, name, slayer, wildy, region, List.of(creatures));
    }

    private static final class Builder {
        private final String id;
        private final String name;
        private final int slayer;
        private final boolean wildy;
        private final Region region;
        private final List<String> creatures;
        private int max = 35;
        private int step = 5;
        private List<String> quests = List.of();
        private Skill skill;
        private int skillLevel;
        private boolean konarOnly;

        Builder(String id, String name, int slayer, boolean wildy, Region region, List<String> creatures) {
            this.id = id;
            this.name = name;
            this.slayer = slayer;
            this.wildy = wildy;
            this.region = region;
            this.creatures = creatures;
            put();
        }

        Builder quests(String... ids) {
            quests = List.of(ids);
            return put();
        }

        Builder skill(Skill s, int level) {
            skill = s;
            skillLevel = level;
            return put();
        }

        Builder konarOnly() {
            konarOnly = true;
            return put();
        }

        Builder max(int kills, int perTier) {
            max = kills;
            step = perTier;
            return put();
        }

        private Builder put() {
            ALL.put(id, new BossTask(id, name, creatures, slayer, wildy, region, MIN_KILLS, max, step, quests, skill, skillLevel,
                    konarOnly));
            return this;
        }
    }

    static {
        b("abyssal_sire", "Abyssal Sire", 85, false, Region.MISTHALIN, "abyssal_sire").quests("enter_the_abyss");
        b("alchemical_hydra", "Alchemical Hydra", 95, false, Region.KOUREND, "alchemical_hydra").konarOnly();
        b("araxxor", "Araxxor", 92, false, Region.MORYTANIA, "araxxor").quests("priest_in_peril");
        b("callisto", "Callisto and Artio", 1, true, Region.WILDERNESS, "callisto", "artio");
        b("cerberus", "Cerberus", 91, false, Region.ASGARNIA, "cerberus");
        b("chaos_elemental", "Chaos Elemental", 1, true, Region.WILDERNESS, "chaos_elemental");
        b("chaos_fanatic", "Chaos Fanatic", 1, true, Region.WILDERNESS, "chaos_fanatic");
        b("commander_zilyana", "Commander Zilyana", 1, false, Region.TROLL_COUNTRY, "commander_zilyana")
                .quests("death_plateau").skill(Skill.AGILITY, 70);
        // The Deranged archaeologist (Fossil Island) counts too, except on Konar's task (see counts).
        b("crazy_archaeologist", "Crazy Archaeologist", 1, true, Region.WILDERNESS, "crazy_archaeologist",
                "deranged_archaeologist");
        b("dagannoth_kings", "Dagannoth Kings", 1, false, Region.FREMENNIK_PROVINCE, "dagannoth_rex", "dagannoth_prime",
                "dagannoth_supreme");
        b("duke_sucellus", "Duke Sucellus", 1, false, Region.FREMENNIK_PROVINCE, "duke_sucellus").quests("desert_treasure_ii");
        b("general_graardor", "General Graardor", 1, false, Region.TROLL_COUNTRY, "general_graardor")
                .quests("death_plateau").skill(Skill.STRENGTH, 70);
        b("giant_mole", "Giant Mole", 1, false, Region.ASGARNIA, "giant_mole");
        b("grotesque_guardians", "Grotesque Guardians", 75, false, Region.MORYTANIA, "dusk").quests("priest_in_peril");
        b("kalphite_queen", "Kalphite Queen", 1, false, Region.KHARIDIAN_DESERT, "kalphite_queen");
        b("king_black_dragon", "King Black Dragon", 1, false, Region.WILDERNESS, "king_black_dragon").quests("dragon_slayer_i");
        b("kraken", "Kraken", 87, false, Region.KANDARIN, "kraken");
        b("kreearra", "Kree'arra", 1, false, Region.TROLL_COUNTRY, "kreearra").quests("death_plateau").skill(Skill.RANGED, 70);
        b("kril_tsutsaroth", "K'ril Tsutsaroth", 1, false, Region.TROLL_COUNTRY, "kril_tsutsaroth")
                .quests("death_plateau").skill(Skill.HITPOINTS, 70);
        b("phantom_muspah", "Phantom Muspah", 1, false, Region.FREMENNIK_PROVINCE, "phantom_muspah").quests("secrets_of_the_north");
        b("sarachnis", "Sarachnis", 1, false, Region.KANDARIN, "sarachnis");
        b("scorpia", "Scorpia", 1, true, Region.WILDERNESS, "scorpia");
        b("the_leviathan", "The Leviathan", 1, false, Region.KHARIDIAN_DESERT, "the_leviathan").quests("desert_treasure_ii");
        b("the_whisperer", "The Whisperer", 1, false, Region.MISTHALIN, "the_whisperer").quests("desert_treasure_ii");
        b("thermonuclear_smoke_devil", "Thermonuclear smoke devil", 93, false, Region.KANDARIN, "thermonuclear_smoke_devil");
        b("vardorvis", "Vardorvis", 1, false, Region.MORYTANIA, "vardorvis").quests("desert_treasure_ii");
        b("venenatis", "Venenatis and Spindel", 1, true, Region.WILDERNESS, "venenatis", "spindel");
        b("vetion", "Vet'ion and Calvar'ion", 1, true, Region.WILDERNESS, "vetion", "calvarion");
        b("vorkath", "Vorkath", 18, false, Region.FREMENNIK_PROVINCE, "vorkath").quests("dragon_slayer_ii");
        b("zulrah", "Zulrah", 1, false, Region.TIRANNWN, "zulrah").quests("regicide");
        // Added to the wiki table after the first boss Slayer rows (appended so the ids above keep their order).
        b("barrows", "Barrows brothers", 1, false, Region.MORYTANIA, "ahrim", "dharok", "guthan", "karil", "torag", "verac")
                .quests("priest_in_peril").max(36, 6);
        b("maggot_king", "Maggot King", 74, false, Region.MORYTANIA, "maggot_king").quests("the_blood_moon_rises");
        b("shellbane_gryphon", "Shellbane gryphon", 51, false, Region.UNCHARTED_ISLES, "shellbane_gryphon")
                .quests("troubled_tortugans");
    }

    private SlayerBossTasks() {
    }

    public static List<BossTask> all() {
        return Collections.unmodifiableList(new ArrayList<>(ALL.values()));
    }

    /** The boss task with this category id, or null. */
    public static BossTask get(String category) {
        return category == null ? null : ALL.get(category);
    }

    /** True when killing {@code creatureId} counts towards the boss task {@code category}. */
    public static boolean counts(String category, String creatureId) {
        BossTask t = get(category);
        return t != null && creatureId != null && t.creatures().contains(creatureId);
    }

    /** As {@link #counts(String, String)} for a task set by {@code masterId}: Konar's Crazy archaeologist task wants him. */
    public static boolean counts(String category, String creatureId, String masterId) {
        if (KONAR.equals(masterId) && "boss_crazy_archaeologist".equals(category) && "deranged_archaeologist".equals(creatureId)) {
            return false;
        }
        return counts(category, creatureId);
    }

    /** True if at least one of the task's bosses exists in the game. */
    public static boolean exists(BossTask t) {
        for (String c : t.creatures()) {
            BossDef def = ContentRegistry.boss(c);
            if (def != null || Boss.byEntity(c) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Picks one boss task with equal chance among those the player can do: present in the game, Slayer
     * level met, not blocked, not the previous task, Wilderness-only for Krystilia. Null when none qualify.
     */
    public static BossTask pick(Random random, int slayerLevel, boolean wildernessOnly, java.util.Collection<String> blocked,
                                String previous) {
        return pick(random, slayerLevel, wildernessOnly, blocked, previous, null, t -> true);
    }

    /** As above for the master {@code masterId}, also requiring the boss's quests and skills ({@code reachable}). */
    public static BossTask pick(Random random, int slayerLevel, boolean wildernessOnly, java.util.Collection<String> blocked,
                                String previous, String masterId, Predicate<BossTask> reachable) {
        List<BossTask> ok = choices(slayerLevel, wildernessOnly, blocked, previous, masterId, reachable);
        return ok.isEmpty() ? null : ok.get(random.nextInt(ok.size()));
    }

    public static List<BossTask> choices(int slayerLevel, boolean wildernessOnly, java.util.Collection<String> blocked,
                                         String previous) {
        return choices(slayerLevel, wildernessOnly, blocked, previous, null, t -> true);
    }

    /**
     * The boss tasks a master may assign: Slayer level met, Wilderness-only for Krystilia, the Alchemical Hydra only for
     * Konar, not blocked, not the previous task, present in the game and {@code reachable} (quests and skills).
     */
    public static List<BossTask> choices(int slayerLevel, boolean wildernessOnly, java.util.Collection<String> blocked,
                                         String previous, String masterId, Predicate<BossTask> reachable) {
        List<BossTask> ok = new ArrayList<>();
        for (BossTask t : ALL.values()) {
            if (slayerLevel >= t.slayerLevel() && (!wildernessOnly || t.wilderness()) && !blocked.contains(t.id())
                    && !t.id().equals(previous) && (!t.konarOnly() || KONAR.equals(masterId)) && exists(t)
                    && reachable.test(t)) {
                ok.add(t);
            }
        }
        return ok;
    }

    /** The amount a chosen boss-task size is clamped to: {@link #MIN_KILLS} to the task's maximum with {@code tiers}. */
    public static int clampKills(BossTask t, int wanted, int tiers) {
        return Math.max(t.min(), Math.min(t.maxKills(tiers), wanted));
    }
}
