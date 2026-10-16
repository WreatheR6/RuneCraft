package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.MonsterDef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The Fortis Colosseum (OSRS Wiki "Fortis Colosseum", "Fortis Colosseum/Modifiers", "Fortis Colosseum/Strategies#Wave
 * breakdown", "Glory"): eleven waves of Varlamore's gladiators and beasts, then Sol Heredit on wave 12. Before every wave
 * Minimus offers three modifiers (the first wave always Relentless, Blasphemy and Frailty); the chosen ones stay for the
 * rest of the run. Each cleared wave adds a roll on that wave's loot table (see the bosses module's
 * {@code ColosseumRewards}) and Glory (a personal best, not a currency). Between waves the player may leave with the loot
 * earned so far; dying forfeits it. If a wave isn't cleared within 40 seconds its reinforcements arrive from the gate
 * closest to the player.
 *
 * <p>Monster stats come from the wiki's DPS calculator data ({@code monsters.json}), waves and modifiers from the wiki
 * (snapshots in {@code src/test/resources/osrs/minigames/colosseum_*.tsv}).
 */
public final class FortisColosseum {
    private FortisColosseum() {
    }

    /** The entrance (a style-NONE place) just outside the Colosseum's west gate. */
    public static final String PLACE = "mg_fortis_colosseum";
    public static final int ENTRANCE_X = 1786;
    public static final int ENTRANCE_Y = 3099;
    /** The arena itself (the wiki's map pin). The lobby below is at 1805-1825, 9506. */
    public static final int ARENA_X = 1824;
    public static final int ARENA_Y = 3107;
    /** The Colosseum scoreboard in the lobby: Glory, waves cleared, best times. */
    public static final String SCOREBOARD = "colosseum_scoreboard";
    /** Minimus, the Colosseum master (the zeah module's NPC at the Colosseum): runs, quiver exchange. */
    public static final String MINIMUS = "zeah_minimus";
    /** Sol Heredit (the bosses module's boss), who leaps into the arena on wave 12. */
    public static final String SOL_HEREDIT = "sol_heredit";

    public static final int WAVES = 12;
    /** Reinforcements arrive when a wave is not cleared within 40 seconds (67 game ticks). */
    public static final int REINFORCEMENT_TICKS = 67;
    /** Ticks between a wave being cleared (or a modifier chosen) and the next wave's monsters appearing. */
    public static final int WAVE_START_DELAY = 5; // approx

    // ------------------------------------------------------------------ monsters

    public static final String BERSERKER = "fremennik_warband_berserker";
    public static final String ARCHER = "fremennik_warband_archer";
    public static final String SEER = "fremennik_warband_seer";
    public static final String SHAMAN = "serpent_shaman";
    public static final String JAGUAR = "jaguar_warrior";
    public static final String JAVELIN = "javelin_colossus";
    public static final String MANTICORE = "manticore";
    public static final String SHOCKWAVE = "shockwave_colossus";
    /** The Colosseum's level-318 minotaur (the plain id "minotaur" is the level-27 Stronghold of Security one). */
    public static final String MINOTAUR = "colosseum_minotaur";
    /** Modifier creatures: Bees! swarms and Totemic healing totems (1 hitpoint each; they don't hold up a wave). */
    public static final String BEE_SWARM = "colosseum_bee_swarm";
    public static final String HEALING_TOTEM = "colosseum_healing_totem";
    public static final List<String> WARBAND = List.of(BERSERKER, ARCHER, SEER);

    /**
     * The style that always lands a max hit on each Fremennik warband member (wiki: magic on the berserker, melee on the
     * archer, ranged on the seer).
     */
    public static String warbandWeakness(String creature) {
        return switch (creature) {
            case BERSERKER -> "magic";
            case ARCHER -> "melee";
            case SEER -> "ranged";
            default -> null;
        };
    }

    private static AttackDef melee(AttackType type, int max) {
        return AttackDef.of("melee", type, max, 2.5, 10);
    }

    private static MonsterDef m(String id, String name, int level) {
        return MonsterDef.of(id, name).level(level);
    }

    /** The Colosseum's creatures, stats from monsters.json (stats(hp, att, str, def, magic, ranged)). */
    public static List<MonsterDef> monsters() {
        List<MonsterDef> list = new ArrayList<>();
        list.add(m(BERSERKER, "Fremennik warband berserker", 103).stats(48, 110, 110, 80, 110, 110).speed(6)
                .defenceBonus(50, 50, 50, 0, 75).attackBonus(150, 150, 0).attack(melee(AttackType.STAB, 29))
                .size(0.7f, 1.9f, 1.0f).skin(0xD8B090, 0x6A4A2A, 0x4A3A2A, 0xB0B0B8));
        list.add(m(ARCHER, "Fremennik warband archer", 104).stats(50, 110, 110, 80, 110, 110).speed(6)
                .defenceBonus(0, 0, 0, 75, 50).attackBonus(150, 150, 150)
                .attack(AttackDef.of("ranged", AttackType.RANGED, 14, 8, 10).particle("minecraft:crit"))
                .size(0.7f, 1.9f, 1.0f).skin(0xD8B090, 0x3A5A2A, 0x4A3A2A, 0x8A6A3A));
        list.add(m(SEER, "Fremennik warband seer", 104).stats(50, 110, 110, 80, 110, 110).speed(6)
                .defenceBonus(50, 50, 50, 30, 0).attackBonus(0, 150, 0)
                .attack(AttackDef.of("magic", AttackType.MAGIC, 12, 8, 10).particle("minecraft:witch"))
                .size(0.7f, 1.9f, 1.0f).skin(0xD8B090, 0x3A3A6A, 0x4A3A2A, 0x6060C0));
        list.add(m(SHAMAN, "Serpent shaman", 161).stats(125, 100, 90, 90, 220, 160).speed(5)
                .defenceBonus(30, 30, 30, 15, 50).attackBonus(0, 50, 0)
                .attack(AttackDef.of("magic", AttackType.MAGIC, 28, 9, 10).particle("minecraft:witch"))
                .size(0.8f, 2.0f, 1.0f).skin(0x5A8A3A, 0x3A6A2A, 0x2A4A1A, 0xE0C040));
        // Jaguar warriors claw three times on a five-tick cycle (47 each).
        list.add(m(JAGUAR, "Jaguar warrior", 234).stats(125, 200, 330, 125, 100, 160).speed(5)
                .defenceBonus(30, 30, 30, 15, 45).attackBonus(0, 0, 0).attack(melee(AttackType.SLASH, 47))
                .size(1.0f, 2.2f, 1.2f).skin(0xC09040, 0x3A2A1A, 0x6A4A2A, 0x2A2A2A));
        list.add(m(JAVELIN, "Javelin Colossus", 278).stats(220, 200, 300, 190, 225, 360).speed(5)
                .defenceBonus(15, 15, 15, 20, 30).attackBonus(0, 0, 25)
                .attack(AttackDef.of("javelin", AttackType.RANGED, 48, 12, 10).particle("minecraft:crit"))
                .size(1.6f, 3.4f, 1.6f).skin(0xA08060, 0xC0A060, 0x8A6A3A, 0xE0C040));
        // The manticore fires three orbs per 10-tick cycle (manticorePattern): its own attack is the first orb (magic or
        // ranged; the melee orb is never first, weight 0) and the Colosseum session fires the other two a tick apart. Range 15.
        list.add(m(MANTICORE, "Manticore", 320).stats(250, 300, 300, 250, 300, 350).speed(MANTICORE_CYCLE_TICKS)
                .defenceBonus(0, 0, 0, 10, 25).attackBonus(0, 0, 0)
                .attack(AttackDef.of("melee_orb", AttackType.CRUSH, 31, MANTICORE_RANGE, 0).particle("minecraft:crit"))
                .attack(AttackDef.of("ranged_orb", AttackType.RANGED, 36, MANTICORE_RANGE, 10).particle("minecraft:crit"))
                .attack(AttackDef.of("magic_orb", AttackType.MAGIC, 31, MANTICORE_RANGE, 10).particle("minecraft:witch"))
                .size(1.6f, 2.0f, 1.6f).skin(0xB06A2A, 0x8A4A1A, 0x6A3A1A, 0x3A2A1A));
        list.add(m(SHOCKWAVE, "Shockwave Colossus", 239).stats(125, 120, 190, 150, 350, 220).speed(5)
                .defenceBonus(15, 15, 15, 5, 35).attackBonus(0, 55, 0)
                .attack(AttackDef.of("shockwave", AttackType.MAGIC, 56, 12, 10).particle("minecraft:sonic_boom"))
                .size(1.6f, 3.4f, 1.6f).skin(0x8A8AA0, 0x5A5A8A, 0x3A3A5A, 0x80C0FF));
        list.add(m(MINOTAUR, "Minotaur", 318).stats(225, 300, 360, 190, 250, 120).speed(5)
                .defenceBonus(0, 0, 0, 0, 12).attackBonus(15, 0, 0).attack(melee(AttackType.CRUSH, 74))
                .size(1.6f, 3.0f, 1.5f).skin(0x6A4A2A, 0x4A3A2A, 0x3A2A1A, 0xE0D0B0));
        list.add(m(BEE_SWARM, "Bee Swarm", 0).stats(1, 1, 1, 1, 1, 1).speed(4).passive().flying()
                .attack(AttackDef.of("none", AttackType.CRUSH, 0, 0.5, 1)).size(0.8f, 0.8f, 0.8f)
                .skin(0xE0C020, 0x2A2A2A, 0xE0C020, 0x2A2A2A));
        list.add(m(HEALING_TOTEM, "Healing totem", 0).stats(1, 1, 1, 1, 1, 1).speed(4).passive()
                .attack(AttackDef.of("none", AttackType.CRUSH, 0, 0.5, 1)).size(0.8f, 1.6f, 1.0f)
                .skin(0x8A6A3A, 0x6A4A2A, 0x4A3A1A, 0x40E060).moveSpeed(0.0));
        return list;
    }

    // ------------------------------------------------------------------ waves

    /** One wave: the monsters present at the start and the reinforcements after 40 seconds. */
    public record Wave(int number, List<String> monsters, List<String> reinforcements) {
    }

    private static final List<Wave> WAVE_LIST = new ArrayList<>();

    private static void wave(List<String> extra, List<String> reinforcements) {
        List<String> all = new ArrayList<>(WARBAND);
        all.addAll(extra);
        WAVE_LIST.add(new Wave(WAVE_LIST.size() + 1, List.copyOf(all), List.copyOf(reinforcements)));
    }

    static {
        wave(List.of(SHAMAN), List.of(JAGUAR));
        wave(List.of(SHAMAN, JAVELIN), List.of(JAGUAR));
        wave(List.of(SHAMAN, JAVELIN, JAVELIN), List.of(JAGUAR));
        wave(List.of(SHAMAN, MANTICORE), List.of(JAGUAR, SHAMAN));
        wave(List.of(SHAMAN, JAVELIN, MANTICORE), List.of(JAGUAR, SHAMAN));
        wave(List.of(SHAMAN, JAVELIN, JAVELIN, MANTICORE), List.of(JAGUAR, SHAMAN));
        wave(List.of(JAVELIN, MANTICORE, SHOCKWAVE), List.of(MINOTAUR));
        wave(List.of(JAVELIN, JAVELIN, MANTICORE, SHOCKWAVE), List.of(MINOTAUR));
        wave(List.of(JAVELIN, MANTICORE, MANTICORE), List.of(MINOTAUR));
        wave(List.of(JAVELIN, JAVELIN, MANTICORE, MANTICORE), List.of(MINOTAUR, SHAMAN));
        wave(List.of(JAVELIN, MANTICORE, MANTICORE, SHOCKWAVE), List.of(MINOTAUR, SHAMAN));
        WAVE_LIST.add(new Wave(12, List.of(SOL_HEREDIT), List.of()));
    }

    /** Wave 1-12. */
    public static Wave wave(int n) {
        return WAVE_LIST.get(n - 1);
    }

    public static List<Wave> waves() {
        return Collections.unmodifiableList(WAVE_LIST);
    }

    /**
     * The monsters a wave starts with under the active modifiers: Quartet adds a random warband member, Dynamic Duo pairs
     * every Shockwave Colossus.
     */
    public static List<String> startingMonsters(int n, Map<Modifier, Integer> active, Random random) {
        List<String> list = new ArrayList<>(wave(n).monsters());
        if (n >= WAVES) {
            return list;
        }
        if (active.containsKey(Modifier.QUARTET)) {
            list.add(WARBAND.get(random.nextInt(WARBAND.size())));
        }
        if (active.containsKey(Modifier.DYNAMIC_DUO)) {
            long shockwaves = list.stream().filter(SHOCKWAVE::equals).count();
            for (int i = 0; i < shockwaves; i++) {
                list.add(SHOCKWAVE);
            }
        }
        return list;
    }

    // ------------------------------------------------------------------ modifiers

    /** The wiki's modifiers with their Glory per wave for each tier (enum ordinals are not saved). */
    public enum Modifier {
        BEES("Bees!", 150, 300, 450),
        BLASPHEMY("Blasphemy", 100, 200, 300),
        DOOM("Doom", 200, 400, 600),
        DYNAMIC_DUO("Dynamic Duo", 150),
        FRAILTY("Frailty", 200, 400, 600),
        MANTIMAYHEM("Mantimayhem", 150, 300, 450),
        MYOPIA("Myopia", 200, 400, 600),
        REENTRY("Reentry", 150, 300, 450),
        RED_FLAG("Red Flag", 250),
        RELENTLESS("Relentless", 200, 400, 600),
        SOLARFLARE("Solarflare", 250, 500, 750),
        QUARTET("Quartet", 100),
        TOTEMIC("Totemic", 200),
        VOLATILITY("Volatility", 100, 200, 300);

        public final String displayName;
        private final int[] glory;

        Modifier(String displayName, int... glory) {
            this.displayName = displayName;
            this.glory = glory;
        }

        public int maxTier() {
            return glory.length;
        }

        /** Glory per wave at a tier (1-based). */
        public int glory(int tier) {
            return glory[Math.max(1, Math.min(tier, glory.length)) - 1];
        }

        /** "Bees! (II)" style name. */
        public String label(int tier) {
            return tier <= 1 ? displayName : displayName + " (" + "I".repeat(tier) + ")";
        }

        /** Modifiers that are not offered for wave 12 (they only affect the regular waves). */
        public boolean notForSol() {
            return this == DYNAMIC_DUO || this == MANTIMAYHEM || this == REENTRY || this == RED_FLAG;
        }

        /** The first wave a modifier can be offered for: Red Flag and Dynamic Duo become available before wave 7. */
        public int firstWave() {
            return this == RED_FLAG || this == DYNAMIC_DUO ? 7 : 2;
        }
    }

    /** The modifiers always offered before the first wave. */
    public static final List<Modifier> FIRST_OFFER = List.of(Modifier.RELENTLESS, Modifier.BLASPHEMY, Modifier.FRAILTY);

    /** One choice: a modifier at the tier it would reach. */
    public record Offer(Modifier modifier, int tier) {
        public String label() {
            return modifier.label(tier);
        }
    }

    /** How strongly an upgrade of a chosen modifier is preferred over a new one. */
    static final int UPGRADE_WEIGHT = 2; // approx: the wiki only says upgrades are preferred

    /** The three modifiers Minimus offers before a wave (1-12) given the active ones (modifier to tier). */
    public static List<Offer> offers(int wave, Map<Modifier, Integer> active, Random random) {
        List<Offer> out = new ArrayList<>();
        if (wave <= 1) {
            for (Modifier m : FIRST_OFFER) {
                out.add(new Offer(m, 1));
            }
            return out;
        }
        List<Offer> pool = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        for (Modifier m : Modifier.values()) {
            int tier = active.getOrDefault(m, 0);
            if (tier >= m.maxTier() || wave < m.firstWave() || (wave >= WAVES && m.notForSol())) {
                continue;
            }
            pool.add(new Offer(m, tier + 1));
            weights.add(tier > 0 ? UPGRADE_WEIGHT : 1);
        }
        while (out.size() < 3 && !pool.isEmpty()) {
            int total = weights.stream().mapToInt(Integer::intValue).sum();
            int roll = random.nextInt(total);
            int i = 0;
            while (roll >= weights.get(i)) {
                roll -= weights.get(i);
                i++;
            }
            out.add(pool.remove(i));
            weights.remove(i);
        }
        return out;
    }

    /** Applies a chosen offer to the active modifiers. */
    public static void choose(Map<Modifier, Integer> active, Offer offer) {
        active.put(offer.modifier(), offer.tier());
    }

    public static Map<Modifier, Integer> newModifiers() {
        return new EnumMap<>(Modifier.class);
    }

    // Modifier effects (tier is 1-based; 0 = not active).

    /** Bees!: swarms in the arena (one per tier), moving a tile every 12 ticks, respawning 30 seconds after dying. */
    public static final int BEE_MOVE_TICKS = 12;
    public static final int BEE_RESPAWN_TICKS = 50;
    /** Bees!: up to 10 unblockable damage a tick under the swarm, and poison starting at 1. */
    public static final int BEE_DAMAGE = 10;

    /** Blasphemy: prayer drained as a percentage of damage taken. */
    public static int blasphemyPercent(int tier) {
        return 20 * tier;
    }

    /** Doom: the stacks that kill (15, 10, 5); one stack per damaging hit, cleared after each wave. */
    public static int doomLimit(int tier) {
        return tier <= 0 ? Integer.MAX_VALUE : new int[]{15, 10, 5}[Math.min(tier, 3) - 1];
    }

    /** Frailty: base Hitpoints reduced by 10%, 20% or 40%; no overhealing. */
    public static int frailtyPercent(int tier) {
        return tier <= 0 ? 0 : new int[]{10, 20, 40}[Math.min(tier, 3) - 1];
    }

    /** The highest health the player may have under Frailty. */
    public static int frailtyCap(int hitpointsLevel, int tier) {
        return hitpointsLevel - hitpointsLevel * frailtyPercent(tier) / 100;
    }

    /** Myopia: attack range reduced by 2, 4 or 6 tiles (manual casts unaffected). */
    public static int myopiaReduction(int tier) {
        return 2 * tier;
    }

    /** Relentless: enemy max hits +1, +3, +6. */
    public static int relentlessMaxHitBonus(int tier) {
        return tier <= 0 ? 0 : new int[]{1, 3, 6}[Math.min(tier, 3) - 1];
    }

    /** Relentless: percentage of the player's Defence level the enemies' accuracy bypasses (III ignores accuracy). */
    public static int relentlessDefenceBypass(int tier) {
        return tier <= 0 ? 0 : new int[]{33, 66, 100}[Math.min(tier, 3) - 1];
    }

    /** Relentless (III): enemies skip the accuracy roll; returned as an attack-roll multiplier large enough to always hit. */
    public static final double IGNORE_ACCURACY = 1_000_000.0;

    /**
     * Relentless: the factor on an enemy's attack roll that has the same effect as the player's Defence level being cut
     * by {@link #relentlessDefenceBypass} percent. A player's defence roll is (effective level) x (bonus + 64) with an
     * effective level of the boosted level + 8 + stance (taken as 1 here; prayers ignored, approx); against magic the
     * level is 70% Magic + 30% Defence, so only the Defence part is bypassed. Tier III ignores accuracy entirely.
     */
    public static double relentlessAccuracy(int tier, int defenceLevel, int magicLevel, boolean magicAttack) {
        if (tier <= 0) {
            return 1.0;
        }
        if (tier >= 3) {
            return IGNORE_ACCURACY;
        }
        int def = Math.max(0, defenceLevel);
        int bypassed = def - def * relentlessDefenceBypass(tier) / 100;
        double full = (magicAttack ? 0.7 * magicLevel + 0.3 * def : def) + 9;
        double reduced = (magicAttack ? 0.7 * magicLevel + 0.3 * bypassed : bypassed) + 9;
        return full / Math.max(1.0, reduced);
    }

    /** The longest attack range (tiles): magic and longbows. */
    public static final int MAX_RANGE = 10;

    /**
     * A ranged weapon's attack range in tiles (OSRS Wiki "Attack range"): darts 3, knives and thrownaxes 4, the toxic
     * blowpipe 5, shortbows and crossbows 7, the Armadyl and zaryte crossbows 8, ballistae and chinchompas 9, longbows,
     * composite bows and the crystal, twisted and dark bows 10; longrange adds 2 (at most 10). Spells cast from the
     * autocast menu and powered staves reach 10. Unknown ranged weapons count as 7 (approx).
     */
    public static int attackRange(String weapon, boolean magic, boolean longrange) {
        if (magic) {
            return MAX_RANGE;
        }
        String w = weapon == null ? "" : weapon;
        int base;
        if (w.endsWith("_dart") || w.contains("_dart_") || w.endsWith("_dart(p)")) {
            base = 3;
        } else if (w.contains("knife") || w.contains("thrownaxe")) {
            base = 4;
        } else if (w.contains("blowpipe")) {
            base = 5;
        } else if (w.contains("longbow") || w.contains("comp_bow") || w.equals("crystal_bow") || w.startsWith("crystal_bow")
                || w.contains("twisted_bow") || w.contains("dark_bow") || w.contains("bow_of_faerdhinen") || w.equals("third_age_bow")) {
            base = 10;
        } else if (w.contains("ballista") || w.contains("chinchompa")) {
            base = 9;
        } else if (w.equals("armadyl_crossbow") || w.equals("zaryte_crossbow")) {
            base = 8;
        } else {
            base = 7; // shortbows, crossbows and anything else
        }
        return Math.min(MAX_RANGE, base + (longrange ? 2 : 0));
    }

    /** Myopia: the attack range left (at least 1 tile) after the tier's reduction. */
    public static int myopiaRange(int range, int tier) {
        return Math.max(1, range - myopiaReduction(tier));
    }

    /** Solarflare: ticks between the orb's moves and the pause at a corner (I: 2 / 7, II: 2 / 0, III: 1 / 2). */
    public static int solarflareMoveTicks(int tier) {
        return tier >= 3 ? 1 : 2;
    }

    public static int solarflareCornerPause(int tier) {
        return tier == 1 ? 7 : tier == 2 ? 0 : 2;
    }

    /** Solarflare: contact damage ("deals more damage" each tier; exact values undocumented). */
    public static int solarflareDamage(int tier) {
        return 10 * tier; // approx
    }

    /**
     * Totemic: a healing totem appears when an enemy drops to 50%; it heals 30% of the enemy's health "every few ticks". The
     * strategy guide times the heals on Sol Heredit (75 hitpoints) at every 4.2 seconds, i.e. 7 ticks, used for every enemy.
     */
    public static final int TOTEM_HEAL_PERCENT = 30;
    public static final int TOTEM_HEAL_TICKS = 7;
    public static final int TOTEM_RESPAWN_TICKS = 200;

    /** Volatility: an enemy explodes on death, one (I) or two (II, III) tiles beyond its size; III leaves molten sand. */
    public static int volatilityRadius(int size, int tier) {
        return (size + 2 * Math.min(tier, 2)) / 2;
    }

    public static final int VOLATILITY_DAMAGE = 15; // approx: "the explosions don't do heavy damage"

    /** Molten sand (Reentry, Volatility III): up to 15 damage every two ticks while standing on it. */
    public static final int MOLTEN_SAND_DAMAGE = 15;

    /** The Javelin Colossus replaces every fifth attack with a javelin thrown at the player's tile (ignores prayer). */
    public static final int JAVELIN_EVERY = 5;
    public static final int JAVELIN_FLIGHT_TICKS = 3; // approx

    // ------------------------------------------------------------------ manticores

    /** Manticores charge for 10 ticks and attack from up to 15 tiles (= blocks in the arena). */
    public static final int MANTICORE_CYCLE_TICKS = 10;
    public static final int MANTICORE_RANGE = 15;

    /** An orb's max hit: 31 melee, 36 ranged, 31 magic. */
    public static int manticoreOrbMaxHit(AttackType style) {
        return style == AttackType.RANGED ? 36 : 31;
    }

    /**
     * A manticore's three orbs, launched a tick apart (OSRS Wiki, "Manticore"): ranged-magic or magic-ranged, the last always
     * melee; with Mantimayhem III the melee orb can come anywhere. {@code first} is the orb its own attack fired.
     * // approx: with Mantimayhem III the melee orb is never first here, and paired manticores don't copy each other's pattern
     */
    public static List<AttackType> manticorePattern(int mayhemTier, AttackType first, Random random) {
        List<AttackType> rest = new ArrayList<>(List.of(AttackType.MAGIC, AttackType.RANGED, AttackType.CRUSH));
        rest.remove(first.isMelee() ? AttackType.CRUSH : first);
        AttackType lead = first.isMelee() ? AttackType.CRUSH : first;
        if (mayhemTier >= 3 || lead == AttackType.CRUSH) {
            java.util.Collections.shuffle(rest, random);
        }
        List<AttackType> out = new ArrayList<>();
        out.add(lead);
        out.addAll(rest);
        return out;
    }

    /** Mantimayhem I: each orb hits twice. */
    public static int manticoreHitsPerOrb(int mayhemTier) {
        return mayhemTier >= 1 ? 2 : 1;
    }

    /** The minotaur heals another monster below 75% to full when the player is out of its melee reach (within 7 tiles). */
    public static final int MINOTAUR_HEAL_BELOW_PERCENT = 75;
    public static final int MINOTAUR_HEAL_RANGE = 7;

    // ------------------------------------------------------------------ glory

    /** Glory for a cleared wave: completion, no-damage, modifier and time bonuses (wiki "Glory#Earning Glory"). */
    public static int glory(int wave, long ticks, boolean noDamage, Map<Modifier, Integer> active) {
        int g = 100 * wave;
        if (noDamage) {
            g += 100 * wave;
        }
        g += modifierGlory(active);
        g += timeBonus(wave, ticks);
        return g;
    }

    public static int modifierGlory(Map<Modifier, Integer> active) {
        int sum = 0;
        for (Map.Entry<Modifier, Integer> e : active.entrySet()) {
            sum += e.getKey().glory(e.getValue());
        }
        return sum;
    }

    /** Time bonus: 500 per wave number, minus the wave number per tick taken, rounded down to an even number. */
    public static int timeBonus(int wave, long ticks) {
        long bonus = 500L * wave - (long) wave * ticks;
        if (bonus <= 0) {
            return 0;
        }
        return (int) (bonus - bonus % 2);
    }

    /** Glory ranks and what they unlock. */
    public static final Map<String, Integer> RANKS = new LinkedHashMap<>();

    static {
        RANKS.put("Brawler", 2000);
        RANKS.put("Challenger", 5000);
        RANKS.put("Gladiator", 8000);
        RANKS.put("Hero", 12000);
        RANKS.put("Champion", 16000);
        RANKS.put("Grand Champion", 20000);
    }

    /** The highest rank reached with a Glory personal best, or null. */
    public static String rank(int glory) {
        String best = null;
        for (Map.Entry<String, Integer> e : RANKS.entrySet()) {
            if (glory >= e.getValue()) {
                best = e.getKey();
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ records and Combat Achievements

    /** MinigameData stats: Glory personal best, full completions, best total time (ticks). */
    public static final String STAT_GLORY = "colosseum_glory";
    public static final String STAT_COMPLETIONS = "colosseum_completions";
    public static final String STAT_BEST_TIME = "colosseum_best_ticks";
    public static final String STAT_BEST_WAVE = "colosseum_best_wave";

    /** Combat Achievement flags the session reports (rules in CombatAchievementRules, stage = the wave just cleared). */
    public static final String FLAG_NO_MANTICORE_DAMAGE = "nomanticoredmg";
    public static final String FLAG_MINOTAUR_NEVER_HEALED = "nominotaurheal";
    public static final String FLAG_ONE_OFF = "oneoff";
    public static final String FLAG_REINFORCEMENTS = "reinforcements";
    public static final String FLAG_SPEED_CHASER = "total28";
    public static final String FLAG_SPEED_RUNNER = "total24";
    /** Speed tasks: the whole Colosseum (every wave from wave 1's start) in 28:00 / 24:00. */
    public static final int SPEED_CHASER_TICKS = 2800;
    public static final int SPEED_RUNNER_TICKS = 2400;

    /** "One-off": wave 11 completed with Red Flag, Dynamic Duo or Doom (II) active. */
    public static boolean oneOff(Map<Modifier, Integer> active) {
        return active.containsKey(Modifier.RED_FLAG) || active.containsKey(Modifier.DYNAMIC_DUO)
                || active.getOrDefault(Modifier.DOOM, 0) >= 2;
    }

    /** "Reinforcements": Sol Heredit defeated with Bees! (II), Quartet and Solarflare (II) active. */
    public static boolean reinforcements(Map<Modifier, Integer> active) {
        return active.getOrDefault(Modifier.BEES, 0) >= 2 && active.containsKey(Modifier.QUARTET)
                && active.getOrDefault(Modifier.SOLARFLARE, 0) >= 2;
    }

    /** "Showboating": Sol Heredit defeated after Fortis Salutes to all four sides while he was below 10% hitpoints. */
    public static final String FLAG_SHOWBOATING = "showboating";
    public static final int SHOWBOATING_BELOW_PERCENT = 10;

    /**
     * The side of the arena a player faces (Minecraft yaw: 0 south, 90 west, 180 north, 270 east), or null when facing
     * diagonally (wiki: diagonal salutes don't count). Within 22.5 degrees of a side counts as facing it.
     */
    public static String saluteDirection(float yaw) {
        double y = ((yaw % 360) + 360) % 360;
        String[] sides = {"south", "west", "north", "east"};
        for (int i = 0; i < 4; i++) {
            double diff = Math.abs(y - i * 90.0);
            if (Math.min(diff, 360 - diff) <= 22.5) {
                return sides[i];
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ Glory unlocks (wiki "Glory")

    /** Brawler: the bank chest in the lobby. */
    public static final int GLORY_BANK = 2000;
    /** Challenger: Civitas illa Fortis as a respawn point. */
    public static final int GLORY_RESPAWN = 5000;
    /** Hero: the ring of dueling's Fortis Colosseum teleport. */
    public static final int GLORY_DUELING = 12000;
    /** Grand Champion: the Fortis Salute emote. */
    public static final int GLORY_SALUTE = 20000;
    /** The lobby bank chest (beside the scoreboard). */
    public static final String BANK_CHEST = "colosseum_bank_chest";
    /** The respawn point in Civitas illa Fortis (OSRS tile, the city's centre square). approx */
    public static final int RESPAWN_X = 1681;
    public static final int RESPAWN_Y = 3133;

    // ------------------------------------------------------------------ Varlamore tokens (wiki "Token (Varlamore)")

    /** The capybaras' token: one per day from the activity a character name's length is assigned that day. */
    public static final String TOKEN = "varlamore_token";
    /** The Colosseum's token comes from completing wave 3 (wearing a partyhat in OSRS; no partyhats here, approx). */
    public static final int TOKEN_WAVE = 3;
    /** The seven token activities in the wiki table's order (Monday's activity for 1- or 8-letter names first). */
    public static final List<String> TOKEN_ACTIVITIES = List.of("colosseum", "buffalo", "digging", "mining", "pickpocketing",
            "fishing", "bones");

    /**
     * The token activity open to a character name of {@code nameLength} characters on a day (1 = Monday ... 7 = Sunday):
     * the wiki's table shifts each name length one activity along per day, lengths 8-12 repeating 1-5.
     */
    public static String tokenActivity(int nameLength, int dayOfWeek) {
        int row = Math.floorMod(nameLength - 1, 7);
        int day = Math.floorMod(dayOfWeek - 1, 7);
        return TOKEN_ACTIVITIES.get(Math.floorMod(day - row, 7));
    }

    /** Dizana's quiver traded to Minimus: 4,000 sunfire splinters or another 1/200 roll at Smol heredit. */
    public static final int QUIVER_SPLINTERS = 4000;
    public static final int QUIVER_PET_RATE = 200;
}
