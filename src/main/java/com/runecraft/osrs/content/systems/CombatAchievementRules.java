package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.systems.CombatAchievements.Track;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The conditions Combat Achievement tasks put on a fight, a raid or a minigame run, and which tasks use them.
 *
 * <p>The game measures a {@link Metrics} over a fight (from the first hit either way to the kill), over a whole raid or
 * over a whole minigame run (Fight Caves, Inferno, Tempoross...). A task's {@code rule} is a list of space-separated
 * conditions that must all hold ({@code ||} separates alternatives). Names in rules use {@code _} for spaces in attack
 * names and {@code +} for spaces in monster names; lists use {@code ,} (all of) or {@code /} (any of).
 *
 * <ul>
 *   <li>{@code t<M:SS}, {@code t<=M:SS} (or {@code Nt} in game ticks): fight/raid/run time.</li>
 *   <li>{@code onlydmg=a,b}: every damaging hit was one of these attacks (or attackers, by name), with the same extra names
 *       as {@code nodmg}.</li>
 *   <li>{@code nodmg}: no damage at all; {@code nodmg=a,b}: no damage from these attacks. Besides boss attack names:
 *       {@code boss} (anything the target itself did), {@code other} (minions, bodyguards, other monsters),
 *       {@code melee} (any melee-style hit), {@code bossmelee}, {@code offprayer} (hits without the matching protection
 *       prayer), {@code mechanic} (scripted damage with no attack name), {@code hazard} (ground pools), {@code poison}.</li>
 *   <li>{@code prayed}, {@code noprayloss}, {@code style=melee,ranged}, {@code weapon=a/b}, {@code noweapon=a/b},
 *       {@code finish=a/b}, {@code spec} (every attack on the target a special attack), {@code finishspec},
 *       {@code spell=*earth}, {@code finishspell=*fire}, {@code nohit} (never attacked the target directly),
 *       {@code hits<=N} (damaging hits on the target).</li>
 *   <li>{@code solo}, {@code players<=N}, {@code players>=N}, {@code startplayers<=N}: players near the fight.</li>
 *   <li>{@code nofood}, {@code nopotion}, {@code nosupplies}, {@code foodonly=a/b}, {@code noequip}, {@code norun},
 *       {@code maxhp<=N}, {@code minhp>=N}, {@code prayer=NAME} (active at the kill), {@code noprayer=NAME} (never
 *       active), {@code wear=a,b} (worn at the kill), {@code antifire}, {@code book=ARCEUUS}.</li>
 *   <li>{@code notattacked} (the target never attacked you), {@code noattacker=a/b}, {@code attackedby=a,b},
 *       {@code nohitby=a/b}, {@code offprayerhitby=a} (took and survived an unprayed hit), {@code nokill=key/key},
 *       {@code killed>=N:key}, {@code frozen} (target immobilised when it died), {@code alone} (no other monster alive
 *       next to it when it died), {@code id=a/b} (which form or brother died).</li>
 *   <li>Raids: {@code mode=challenge}, {@code size=N}, {@code size>=N}, {@code level>=N} (ToA raid level),
 *       {@code deathless} (nobody died), {@code nodeath} (you didn't die), {@code inv=A,B} (ToA invocations),
 *       {@code path>=N}, {@code target} (within the CoX CM / ToB HM target time), {@code points>N},
 *       {@code nospirit} (no Helpful Spirit supplies taken).</li>
 *   <li>Boss variants: {@code awakened} (an awakened Desert Treasure II boss), {@code delve=N}/{@code delve>=N} (the Doom of
 *       Mokhaiotl's delve level; on a delve run, the level just completed).</li>
 *   <li>{@code noprayrestore} (prayer points never went up), {@code onehand} (no two-handed weapon used on the target).</li>
 *   <li>{@code count<=N:a/b}: how often boss scripts counted something (summed over the alternatives); every monster attack
 *       is counted as {@code used:<attack>}, and the same attack twice in a row flags {@code repeat:<attack>}.</li>
 *   <li>{@code moved<=N} (tiles walked), {@code weight<N} (kg of worn gear, at its heaviest), {@code gearvalue<=N} (GE value
 *       of worn gear), {@code geartier<N} (highest level requirement of worn gear), {@code noattack=a/b} (never attacked
 *       these other creatures during the fight).</li>
 *   <li>Minigame runs: {@code stage=N} (which challenge or difficulty the run was, e.g. TzHaar-Ket-Rak's challenge number),
 *       {@code flag=NAME} (a condition the minigame itself judged, e.g. an empty inventory throughout).</li>
 *   <li>{@code flag=NAME} / {@code noflag=NAME} on fights too: something a boss script reported during the fight
 *       (e.g. the Doom of Mokhaiotl's {@code grub_absorbed}).</li>
 *   <li>{@link Track#WINDOW}: {@code distinct} (different targets) and {@code fromstart} (time from the first fight's
 *       start rather than its kill).</li>
 * </ul>
 */
public final class CombatAchievementRules {
    private CombatAchievementRules() {
    }

    /** One hit a player took: from the fight's target or something else, the attacker and attack names, its style. */
    public record Hit(boolean fromTarget, String attacker, String attack, AttackType style, int damage, boolean prayed) {
    }

    /** A raid at the moment a fight or the raid ended. */
    public record Raid(String type, String mode, int size, int level, Set<String> invocations, int pathLevel, int deaths, int myDeaths,
                       boolean withinTarget, int points, boolean spiritSupplies) {
    }

    /** What the game observed over a fight, raid or minigame run. */
    public static final class Metrics {
        public long ticks;
        public boolean damaged;
        public boolean damagedUnprotected;
        public boolean prayerLost;
        public final Set<AttackType> styles = EnumSet.noneOf(AttackType.class);
        public final Set<String> weapons = new HashSet<>();
        public final Set<String> spells = new HashSet<>();
        public String lastWeapon;
        public String lastSpell;
        public boolean lastSpec;
        /** Attacks on the target, those that were special attacks, and those that did damage. */
        public int targetAttacks;
        public int specAttacks;
        public int targetHits;
        public final List<Hit> taken = new ArrayList<>();
        public final Set<String> attackers = new HashSet<>();
        public boolean targetAttacked;
        public final Map<String, Integer> killed = new HashMap<>();
        public int startPlayers = 1;
        public int maxPlayers = 1;
        public final Set<String> eaten = new HashSet<>();
        public boolean drank;
        public boolean equipChanged;
        public boolean ran;
        public int maxHp;
        public int minHp = Integer.MAX_VALUE;
        public final Set<String> prayersEver = new HashSet<>();
        public final Set<String> prayersAtEnd = new HashSet<>();
        public final Set<String> wornAtEnd = new HashSet<>();
        public boolean antifire;
        public boolean frozen;
        public boolean othersAlive;
        public String book;
        public boolean bookChanged;
        public String killedId;
        public Raid raid;
        public boolean died;
        /** Minigame runs: the challenge or difficulty played (0 when the minigame has none). */
        public int stage;
        /** Minigame runs: conditions the minigame judged itself (see {@code MinigameSession.caFlags}). */
        public final Set<String> flags = new HashSet<>();
        /** Boss variants: an awakened Desert Treasure II boss, and the Doom of Mokhaiotl's delve level (0: none). */
        public boolean awakened;
        public int delve;
        /** Prayer points went up at some point (potions, altars, restores). */
        public boolean prayerRestored;
        /** Things boss scripts and minigames counted during the fight (attacks used as "used:" + attack, burrows...). */
        public final Map<String, Integer> counts = new HashMap<>();
        /** Tiles walked during the fight (one OSRS tile is two blocks). */
        public double moved;
        /** The heaviest (kg), most valuable (GE gp) and highest-tier (level requirement) worn gear seen during the fight. */
        public double maxWeight;
        public long maxGearValue;
        public int maxGearTier;
        /** Fight keys of other creatures the player attacked during the fight (minions, elementals...). */
        public final Set<String> struck = new HashSet<>();

        public void took(Hit h) {
            taken.add(h);
            if (h.damage() > 0) {
                damaged = true;
                if (!h.prayed()) {
                    damagedUnprotected = true;
                }
            }
        }

        public void killed(String key) {
            killed.merge(key, 1, Integer::sum);
        }

        public void count(String key, int n) {
            counts.merge(key.toLowerCase(Locale.ROOT), n, Integer::sum);
        }

        /** Notes the gear worn this tick (item ids; empty slots are ""). */
        public void gear(List<String> worn) {
            maxWeight = Math.max(maxWeight, gearWeight(worn));
            maxGearValue = Math.max(maxGearValue, gearValue(worn));
            maxGearTier = Math.max(maxGearTier, gearTier(worn));
        }

        public CombatAchievements.FightResult result() {
            return new CombatAchievements.FightResult(ticks, damaged, damagedUnprotected, prayerLost, styles, weapons, lastWeapon);
        }
    }

    /** A kill for streak and time-window tasks: the fight key, when the fight started and when the kill landed. */
    public record Kill(String key, long start, long end) {
    }

    // ------------------------------------------------------------------ rule evaluation

    /** Whether a rule holds for these metrics ({@code null} or blank always holds). */
    public static boolean meets(String rule, Metrics m) {
        if (rule == null || rule.isBlank()) {
            return true;
        }
        for (String alt : rule.split("\\|\\|")) {
            boolean all = true;
            for (String token : alt.trim().split("\\s+")) {
                if (!token.isEmpty() && !holds(token, m)) {
                    all = false;
                    break;
                }
            }
            if (all) {
                return true;
            }
        }
        return false;
    }

    private static final Set<String> NOT_PREDICATES = Set.of("distinct", "fromstart");

    /** The parts of a rule that each kill of a streak or window must meet (time limits and window flags removed). */
    public static String killFilter(String rule) {
        if (rule == null) {
            return "";
        }
        List<String> keep = new ArrayList<>();
        for (String token : rule.trim().split("\\s+")) {
            if (!token.isEmpty() && !NOT_PREDICATES.contains(token) && !token.startsWith("t<")) {
                keep.add(token);
            }
        }
        return String.join(" ", keep);
    }

    /** The {@code t<} / {@code t<=} limit of a rule in game ticks, or 0. A strict limit is returned one tick lower. */
    public static long timeLimit(String rule) {
        if (rule == null) {
            return 0;
        }
        for (String token : rule.trim().split("\\s+")) {
            if (token.startsWith("t<=")) {
                return parseTime(token.substring(3));
            }
            if (token.startsWith("t<")) {
                return parseTime(token.substring(2)) - 1;
            }
        }
        return 0;
    }

    public static boolean hasFlag(String rule, String flag) {
        return rule != null && List.of(rule.trim().split("\\s+")).contains(flag);
    }

    /** "M:SS" (or "H:MM:SS") in game ticks of 0.6 s, or "Nt" in ticks. */
    public static long parseTime(String s) {
        if (s.endsWith("t")) {
            return Long.parseLong(s.substring(0, s.length() - 1));
        }
        String[] parts = s.split(":");
        long seconds = 0;
        for (String p : parts) {
            seconds = seconds * 60 + Long.parseLong(p);
        }
        return seconds * 5 / 3;
    }

    private static String name(String s) {
        return s.replace('+', ' ').toLowerCase(Locale.ROOT);
    }

    private static List<String> list(String value, String sep) {
        List<String> out = new ArrayList<>();
        for (String s : value.split(sep)) {
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
        return out;
    }

    /** Whether an id matches a spec of '/'-separated alternatives ('*' prefix: contains; "fists": empty hand). */
    public static boolean matches(String spec, String id) {
        if (id == null) {
            return false;
        }
        for (String alt : spec.split("/")) {
            String a = name(alt);
            if (a.equals("fists") ? id.isEmpty() : a.startsWith("*") ? id.contains(a.substring(1)) : a.equals(id)) {
                return true;
            }
        }
        return false;
    }

    /** Attack names boss scripts give damage that no {@code AttackDef} carries (Hunllef's tornadoes and floor, Nex's dash...). */
    public static final Set<String> SCRIPTED_ATTACKS = Set.of("tornado", "damaging_floor", "summoned_soul", "smoke_dash",
            "blinding", "prison", "dark_core", "rapid_fire", "acid_pool", "spawn_explosion");

    /** Normalised attack name of a hit (lower case, '_' for spaces), or "mechanic" when the damage had no name. */
    public static String attackKey(String attack) {
        return attack == null || attack.isEmpty() ? "mechanic" : attack.toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private static boolean hitMatches(Hit h, String item) {
        return switch (item) {
            case "boss" -> h.fromTarget();
            case "other" -> !h.fromTarget();
            case "melee" -> h.style() != null && h.style().isMelee();
            case "bossmelee" -> h.fromTarget() && h.style() != null && h.style().isMelee();
            case "offprayer" -> !h.prayed();
            default -> attackKey(h.attack()).equals(item);
        };
    }

    private static int number(String v) {
        return Integer.parseInt(v.trim());
    }

    private static boolean compare(long actual, String op, long expected) {
        return switch (op) {
            case "<" -> actual < expected;
            case "<=" -> actual <= expected;
            case ">" -> actual > expected;
            case ">=" -> actual >= expected;
            default -> actual == expected;
        };
    }

    static boolean holds(String token, Metrics m) {
        int i = 0;
        while (i < token.length() && Character.isLetter(token.charAt(i))) {
            i++;
        }
        String key = token.substring(0, i);
        String rest = token.substring(i);
        String op = rest.startsWith("<=") || rest.startsWith(">=") ? rest.substring(0, 2) : rest.isEmpty() ? "" : rest.substring(0, 1);
        String v = rest.substring(op.length());
        Raid r = m.raid;
        return switch (key) {
            case "t" -> compare(m.ticks, op, parseTime(v));
            case "nodmg" -> op.isEmpty() ? !m.damaged
                    : m.taken.stream().filter(h -> h.damage() > 0).noneMatch(h -> list(v, ",").stream().anyMatch(it -> hitMatches(h, it)));
            // Every damaging hit was one of these attacks (or came from one of these attackers, by name).
            case "onlydmg" -> m.taken.stream().filter(h -> h.damage() > 0)
                    .allMatch(h -> list(v, ",").stream().anyMatch(it -> hitMatches(h, it) || matches(it, h.attacker())));
            case "prayed" -> !m.damagedUnprotected;
            case "noprayloss" -> !m.prayerLost;
            case "style" -> !m.styles.isEmpty() && m.styles.stream()
                    .allMatch(s -> list(v, ",").stream().anyMatch(st -> CombatAchievements.styleAllowed(st, s)));
            case "weapon" -> !m.weapons.isEmpty() && m.weapons.stream().allMatch(w -> matches(v, w));
            case "noweapon" -> m.weapons.stream().noneMatch(w -> matches(v, w));
            case "finish" -> matches(v, m.lastWeapon);
            case "spec" -> m.targetAttacks > 0 && m.specAttacks == m.targetAttacks;
            case "finishspec" -> m.lastSpec;
            case "spell" -> !m.spells.isEmpty() && m.spells.stream().allMatch(s -> matches(v, s));
            case "finishspell" -> matches(v, m.lastSpell);
            case "nohit" -> m.targetAttacks == 0;
            case "hits" -> compare(m.targetHits, op, number(v));
            case "solo" -> m.maxPlayers <= 1;
            case "players" -> compare(m.maxPlayers, op, number(v));
            case "startplayers" -> compare(m.startPlayers, op, number(v));
            case "nofood" -> m.eaten.isEmpty();
            case "nopotion" -> !m.drank;
            case "nosupplies" -> m.eaten.isEmpty() && !m.drank;
            case "foodonly" -> m.eaten.stream().allMatch(f -> matches(v, f));
            case "noequip" -> !m.equipChanged;
            case "norun" -> !m.ran;
            case "maxhp" -> compare(m.maxHp, op, number(v));
            case "minhp" -> m.minHp == Integer.MAX_VALUE || compare(m.minHp, op, number(v));
            case "prayer" -> m.prayersAtEnd.contains(v);
            case "noprayer" -> !m.prayersEver.contains(v);
            case "wear" -> list(v, ",").stream().allMatch(spec -> m.wornAtEnd.stream().anyMatch(w -> matches(spec, w)));
            case "antifire" -> m.antifire;
            case "book" -> v.equals(m.book) && !m.bookChanged;
            case "notattacked" -> !m.targetAttacked;
            case "noattacker" -> m.attackers.stream().noneMatch(a -> matches(v, a));
            case "attackedby" -> list(v, ",").stream().allMatch(a -> m.attackers.contains(name(a)));
            case "nohitby" -> m.taken.stream().noneMatch(h -> h.damage() > 0 && matches(v, h.attacker()));
            case "offprayerhitby" -> m.taken.stream().anyMatch(h -> h.damage() > 0 && !h.prayed() && matches(v, h.attacker()));
            case "nokill" -> m.killed.keySet().stream().noneMatch(k -> matches(v, k));
            case "count" -> {
                int colon = v.indexOf(':');
                int sum = 0;
                for (String what : list(v.substring(colon + 1), "/")) {
                    sum += m.counts.getOrDefault(name(what), 0);
                }
                yield compare(sum, op, number(v.substring(0, colon)));
            }
            case "moved" -> compare((long) Math.floor(m.moved), op, number(v));
            case "weight" -> compare((long) Math.ceil(m.maxWeight * 1000), op, number(v) * 1000L);
            case "gearvalue" -> compare(m.maxGearValue, op, Long.parseLong(v.trim()));
            case "geartier" -> compare(m.maxGearTier, op, number(v));
            case "noattack" -> m.struck.stream().noneMatch(k -> matches(v, k));
            case "killed" -> {
                int colon = v.indexOf(':');
                String what = name(v.substring(colon + 1));
                yield compare(m.killed.getOrDefault(what, 0), op, number(v.substring(0, colon)));
            }
            case "frozen" -> m.frozen;
            case "alone" -> !m.othersAlive;
            case "id" -> matches(v, m.killedId);
            case "nodeath" -> !m.died && (r == null || r.myDeaths() == 0);
            case "mode" -> r != null && r.mode().equalsIgnoreCase(v);
            case "size" -> r != null ? compare(r.size(), op, number(v)) : compare(m.maxPlayers, op, number(v));
            case "level" -> r != null && compare(r.level(), op, number(v));
            case "deathless" -> r != null && r.deaths() == 0;
            case "inv" -> r != null && r.invocations().containsAll(list(v, ","));
            case "path" -> r != null && compare(r.pathLevel(), op, number(v));
            case "target" -> r != null && r.withinTarget();
            case "points" -> r != null && compare(r.points(), op, number(v));
            case "nospirit" -> r != null && !r.spiritSupplies();
            case "stage" -> compare(m.stage, op, number(v));
            case "flag" -> m.flags.contains(v);
            case "noflag" -> !m.flags.contains(v);
            case "awakened" -> m.awakened;
            case "delve" -> compare(m.delve, op, number(v));
            case "noprayrestore" -> !m.prayerRestored;
            case "onehand" -> !m.weapons.isEmpty() && m.weapons.stream().noneMatch(com.runecraft.osrs.data.EquipmentBonuses::twoHanded);
            case "distinct", "fromstart" -> true;
            default -> throw new IllegalArgumentException("Unknown combat achievement condition: " + token);
        };
    }

    /** Every condition name a rule may use (for tests). */
    static boolean knownToken(String token) {
        try {
            holds(token, new Metrics());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        } catch (RuntimeException e) {
            return true;
        }
    }

    // ------------------------------------------------------------------ gear

    /** Total weight in kg of worn items (weight-reducing gear counts negative, as in OSRS). */
    public static double gearWeight(List<String> worn) {
        double kg = 0;
        for (String id : worn) {
            com.runecraft.osrs.ItemDef d = id == null || id.isEmpty() ? null : com.runecraft.osrs.ItemCatalog.get(id);
            if (d != null) {
                kg += d.weight() != 0 ? d.weight() : com.runecraft.osrs.data.EquipmentWeights.of(id);
            }
        }
        return Math.max(0, kg);
    }

    /** Total Grand Exchange guide price of worn items (untradeables count as nothing). */
    public static long gearValue(List<String> worn) {
        long gp = 0;
        for (String id : worn) {
            if (id != null && !id.isEmpty()) {
                gp += com.runecraft.osrs.data.GePrices.guide(id);
            }
        }
        return gp;
    }

    /** The gear tier of worn items: the highest skill level any of them needs to wear (0 for none). */
    public static int gearTier(List<String> worn) {
        int tier = 0;
        for (String id : worn) {
            com.runecraft.osrs.ItemDef d = id == null || id.isEmpty() ? null : com.runecraft.osrs.ItemCatalog.get(id);
            if (d != null && d.equipment() != null) {
                for (int lvl : d.equipment().requirements().values()) {
                    tier = Math.max(tier, lvl);
                }
            }
        }
        return tier;
    }

    // ------------------------------------------------------------------ streaks, windows, averages

    /**
     * Whether the most recent kills complete a time-window task: {@code count} kills (of different targets when
     * {@code distinct}) whose span is at most {@code ticks}, measured from the first one's fight start or kill.
     */
    public static boolean windowMet(List<Kill> kills, int count, long ticks, boolean distinct, boolean fromStart) {
        if (kills.isEmpty()) {
            return false;
        }
        Kill last = kills.get(kills.size() - 1);
        Set<String> keys = new HashSet<>();
        int n = 0;
        for (int i = kills.size() - 1; i >= 0; i--) {
            Kill k = kills.get(i);
            if (distinct && !keys.add(k.key())) {
                continue;
            }
            n++;
            long from = fromStart ? k.start() : k.end();
            if (last.end() - from > ticks) {
                return false;
            }
            if (n >= count) {
                return true;
            }
        }
        return false;
    }

    /** Whether the average of the last {@code count} kill times is at most {@code ticks}. */
    public static boolean averageMet(List<Long> durations, int count, long ticks) {
        if (durations.size() < count) {
            return false;
        }
        long sum = 0;
        for (int i = durations.size() - count; i < durations.size(); i++) {
            sum += durations.get(i);
        }
        return sum <= ticks * count;
    }

    // ------------------------------------------------------------------ the tracked tasks

    /** How the game tracks one wiki task that the description parser leaves untracked. */
    record Spec(Track track, String target, int count, String rule) {
    }

    /** ToA boss invocations (OSRS: the invocations listed under each boss). */
    public static final String AKKHA = "DOUBLE_TROUBLE,KEEP_BACK,STAY_VIGILANT,FEELING_SPECIAL";
    public static final String BABA = "MIND_THE_GAP,GOTTA_HAVE_FAITH,JUNGLE_JAPES,SHAKING_THINGS_UP,BOULDERDASH";
    public static final String KEPHRI = "LIVELY_LARVAE,MORE_OVERLORDS,BLOWING_MUD,MEDIC,AERIAL_ASSAULT";
    public static final String ZEBAK = "NOT_JUST_A_HEAD,ARTERIAL_SPRAY,BLOOD_THINNERS,UPSET_STOMACH";
    public static final String WARDENS = "ANCIENT_HASTE,ACCELERATION,PENETRATION,OVERCLOCKED,OVERCLOCKED_2,INSANITY";
    /** Barrows weapons (OSRS also allows the Dawnbringer, which only exists inside the Theatre and isn't built here). */
    private static final String BARROWS_WEAPONS = "ahrims_staff/dharoks_greataxe/guthans_warspear/karils_crossbow/torags_hammers/veracs_flail";
    private static final String MOONS = "blue_moon|eclipse_moon|blood_moon";

    /**
     * wikiId;track;target;count;rule. Targets: fight keys (boss ids or "kill:" + monster name, '|' alternatives) for
     * RULE/STREAK/WINDOW/AVERAGE, a {@link com.runecraft.osrs.content.raids.RaidType} name for RAID, "mg:" + minigame id
     * for RUN (or "delve:" + boss id for a delve run, judged at each level completed), monster names for KILLS.
     * Conditions marked approx in comments are stricter or looser than OSRS where
     * the exact mechanic isn't modelled (e.g. "perfect" kills read as "no scripted, ground or off-prayer damage";
     * "anyone in the team" reads as yourself; "without leaving" means staying within 64 blocks). Approx: 271 (Zombie
     * Destroyer) is judged at the Vorkath kill; 511 (Unconventional) only knows mithril weapons, so mithril darts, knives
     * and thrownaxes count but mithril arrows and bolts fired from a bow don't; 331 (The Spurned Hero) needs a solo kill
     * (damage per player isn't tracked); 200 (From One King to Another) can't see the bounce off Rex, only the
     * finishing rune thrownaxe special.
     */
    private static final String TABLE = """
            7;RULE;abyssal_sire;1;nodmg=miasma,hazard
            8;RULE;abyssal_sire;1;nodmg=miasma,hazard,tentacle,explosion,mechanic,offprayer
            137;RULE;alchemical_hydra;1;nodmg=poison_pool,hazard
            138;RULE;alchemical_hydra;1;nodmg=lightning
            139;RULE;alchemical_hydra;1;nodmg=fire_wall
            143;RULE;alchemical_hydra;1;weapon=dharoks_greataxe maxhp<=10
            146;STREAK;alchemical_hydra;15;
            584;RULE;amoxliatl;1;weapon=glacial_temotli
            586;STREAK;amoxliatl;10;
            558;WINDOW;araxxor;4;t<=10:00 fromstart
            559;WINDOW;araxxor;5;t<=10:00 fromstart
            560;WINDOW;araxxor;6;t<=10:00 fromstart
            561;RULE;araxxor;1;nodmg=acid_spit,web_magic,acid_ball,hazard,other,offprayer
            567;STREAK;araxxor;10;
            29;RULE;barrows_brother;1;id=karil spec
            635;RULE;brutus;1;noequip nosupplies
            636;RULE;brutus;1;notattacked
            37;RULE;bryophyta;1;prayer=PROTECT_FROM_MAGIC
            39;RULE;bryophyta;1;nodmg=poison
            55;RULE;cerberus;1;nodmg=lava_pool,hazard
            56;RULE;cerberus;1;nodmg=melee
            303;WINDOW;vanguard_melee|vanguard_ranged|vanguard_magic;3;t<=0:10 distinct
            305;RAID;COX;1;size=1 points>40000
            311;RULE;cox_guardian;1;nodmg=mechanic,hazard
            312;RAID;COX;1;deathless
            313;RAID;COX;1;size=1 nodeath
            314;RULE;cox_lizardman_shaman;1;nodmg
            317;RULE;great_olm;1;size=1 noflag=olm_imperfect
            318;RULE;great_olm;1;size=3 noflag=olm_imperfect
            306;RULE;great_olm;1;size=1 noflag=olm_penultimate_special
            320;RULE;cox_ice_demon;1;noprayer=PROTECT_FROM_MISSILES
            321;RULE;cox_ice_demon;1;finishspell=*fire
            322;RAID;COX;1;mode=normal size=1 t<21:00
            323;RAID;COX;1;mode=normal size=1 t<17:00
            324;RAID;COX;1;mode=normal size=5 t<15:00
            325;RAID;COX;1;mode=normal size=5 t<12:30
            326;RAID;COX;1;mode=normal size=3 t<16:30
            327;RAID;COX;1;mode=normal size=3 t<14:30
            288;RAID;COX;10;mode=challenge
            289;RAID;COX;25;mode=challenge
            290;RAID;COX;1;mode=challenge deathless
            291;RAID;COX;1;mode=challenge size=1 nodeath
            292;RAID;COX;1;mode=challenge target
            293;RAID;COX;1;mode=challenge size=1 t<45:00
            294;RAID;COX;1;mode=challenge size=1 t<38:30
            295;RAID;COX;1;mode=challenge size=5 t<30:00
            296;RAID;COX;1;mode=challenge size=5 t<25:00
            297;RAID;COX;1;mode=challenge size=3 t<35:00
            298;RAID;COX;1;mode=challenge size=3 t<27:00
            63;RULE;chaos_fanatic;1;nodmg=green_explosion
            64;STREAK;chaos_fanatic;10;nopotion
            213;RULE;commander_zilyana;1;alone
            214;RULE;commander_zilyana;1;nodmg
            215;RULE;commander_zilyana;1;nohit
            217;STREAK;commander_zilyana;50;
            68;RULE;corporeal_beast;1;finishspec finish=*halberd
            69;RULE;corporeal_beast;1;solo
            72;RULE;crazy_archaeologist;1;style=magic
            73;RULE;crazy_archaeologist;1;nodmg=explosive_book
            204;RULE;dagannoth_rex;1;frozen
            199;RULE;dagannoth_prime;1;attackedby=dagannoth+supreme,dagannoth+rex
            203;RULE;dagannoth_rex;1;attackedby=dagannoth+supreme,dagannoth+prime
            235;RULE;dagannoth_supreme;1;attackedby=dagannoth+prime,dagannoth+rex
            205;WINDOW;dagannoth_rex|dagannoth_prime|dagannoth_supreme;2;t<=1t distinct
            236;WINDOW;dagannoth_rex|dagannoth_prime|dagannoth_supreme;3;t<=0:09 distinct
            78;RULE;deranged_archaeologist;1;style=magic
            79;RULE;deranged_archaeologist;1;nodmg=mechanic
            518;STREAK;duke_sucellus;5;nodmg=mechanic,hazard,gas_vent,freezing_gaze,fist_slam,magic_blast,offprayer
            519;RULE;duke_sucellus;1;nodmg=mechanic,hazard,gas_vent,freezing_gaze norun
            541;RULE;sol_heredit;1;norun
            543;RULE;sol_heredit;1;nodmg=spear,shield_bash,grapple,triple_attack
            18;RULE;general_graardor;1;frozen
            19;RULE;general_graardor;1;notattacked
            20;RULE;general_graardor;1;alone
            21;STREAK;general_graardor;2;nodmg=other
            22;RULE;general_graardor;1;nodmg
            23;STREAK;general_graardor;15;
            180;RULE;giant_mole;1;hits<=4
            182;RULE;giant_mole;1;nodmg=boss
            610;RULE;doom_of_mokhaiotl;1;delve>=3
            611;RULE;doom_of_mokhaiotl;1;delve>=8
            612;RULE;doom_of_mokhaiotl;1;delve>=16
            621;RULE;doom_of_mokhaiotl;1;delve=1 t<0:30
            622;RUN;delve:doom_of_mokhaiotl;1;delve=8 t<10:00
            623;RUN;delve:doom_of_mokhaiotl;1;delve=8 t<7:15
            616;RUN;delve:doom_of_mokhaiotl;1;delve=8 nodmg=boss,hazard noflag=grub_absorbed
            613;RULE;doom_of_mokhaiotl;1;flag=exposed
            614;RULE;doom_of_mokhaiotl;1;delve>=8 noflag=burrowed
            615;RUN;delve:doom_of_mokhaiotl;1;delve=8 noflag=grub_absorbed
            620;RUN;delve:doom_of_mokhaiotl;1;delve=8 noflag=noshield
            624;RULE;doom_of_mokhaiotl;1;flag=mopping_up
            617;RUN;delve:doom_of_mokhaiotl;1;delve=8 noprayrestore
            618;RUN;delve:doom_of_mokhaiotl;1;delve=8 style=melee onehand
            619;RUN;delve:doom_of_mokhaiotl;1;delve=8 noweapon=DEMONBANE
            517;RULE;duke_sucellus;1;awakened
            508;RULE;the_leviathan;1;awakened
            499;RULE;the_whisperer;1;awakened
            490;RULE;vardorvis;1;awakened
            86;RULE;dusk;1;nodmg=falling_rocks
            95;STREAK;dusk;10;
            96;STREAK;dusk;20;
            124;RULE;hespori;1;finishspec
            125;WINDOW;kill:flower;6;t<=0:05
            335;RULE;kril_tsutsaroth;1;alone
            336;RULE;kril_tsutsaroth;1;style=magic spell=*demonbane
            337;RULE;kril_tsutsaroth;1;nodmg=bossmelee
            338;RULE;kril_tsutsaroth;1;nodmg=other
            340;STREAK;kril_tsutsaroth;20;
            164;RULE;king_black_dragon;1;prayer=PROTECT_FROM_MELEE
            166;RULE;king_black_dragon;1;antifire wear=anti_dragon_shield/dragonfire_shield/dragonfire_ward
            167;STREAK;king_black_dragon;10;
            169;RULE;kraken;1;killed>=4:kill:enormous+tentacle
            170;STREAK;kraken;25;
            171;STREAK;kraken;50;
            172;STREAK;kraken;100;
            11;RULE;kreearra;1;nohit
            12;RULE;kreearra;1;alone
            13;RULE;kreearra;1;nodmg=melee
            14;RULE;kreearra;1;weapon=*salamander
            15;STREAK;kreearra;30;
            509;STREAK;the_leviathan;5;nodmg=mechanic,hazard,falling_debris,lightning,offprayer
            175;RULE;kill:lizardman shaman;1;nodmg=boss
            639;STREAK;maggot_king;12;
            640;WINDOW;maggot_king;5;t<=9:00 fromstart
            532;RULE;MOONS;1;nosupplies
            533;RULE;MOONS;1;id=blue_moon weapon=blue_moon_spear || id=eclipse_moon weapon=eclipse_atlatl || id=blood_moon weapon=dual_macuahuitl
            534;WINDOW;MOONS;3;t<=8:00 fromstart distinct weapon=dragon_scimitar
            535;WINDOW;MOONS;3;t<8:00 fromstart distinct
            536;WINDOW;MOONS;3;t<6:00 fromstart distinct
            537;STREAK;MOONS;30;
            412;RULE;nex;1;nodeath
            416;RULE;nex;1;startplayers<=3
            417;RULE;nex;1;startplayers<=2
            131;RULE;obor;1;frozen
            472;RULE;phantom_muspah;1;weapon=*salamander
            473;RULE;phantom_muspah;1;norun
            474;STREAK;phantom_muspah;10;
            409;STREAK;phosanis_nightmare;5;
            588;RULE;branda_the_fire_queen;1;nodmg=molten_fist,frozen_fist,mechanic,hazard
            593;STREAK;branda_the_fire_queen;15;
            209;RULE;sarachnis;1;nodmg=boss
            221;RULE;scorpia;1;nokill=kill:scorpia's+guardian
            524;RULE;scurrius;1;nodmg=falling_ceiling,stab,mechanic,hazard prayed
            525;WINDOW;kill:giant rat;6;t<=0:03
            628;WINDOW;shellbane_gryphon;10;t<=12:30 fromstart
            629;STREAK;shellbane_gryphon;25;
            49;RULE;skotizo;1;noweapon=DEMONBANE
            50;RULE;skotizo;1;finish=*chinchompa
            355;RUN;mg:tempoross;1;wear=angler_hat,angler_top,angler_waders,angler_boots
            571;STREAK;the_hueycoatl;5;nodmg=thunderstorm,tail_sweep,tail_shockwave,mechanic,hazard,offprayer
            573;RULE;the_hueycoatl;1;weapon=*dragon_hunter
            574;RULE;the_hueycoatl;1;style=magic spell=*earth
            189;RULE;the_nightmare;1;solo
            190;RULE;the_nightmare;1;noprayloss
            191;RULE;the_nightmare;1;solo t<23:00
            192;RULE;the_nightmare;1;solo t<19:00
            193;RULE;the_nightmare;1;solo t<16:00
            194;RULE;the_nightmare;1;players>=5 t<5:00
            195;RULE;the_nightmare;1;players>=5 t<4:00
            196;RULE;the_nightmare;1;players>=5 t<3:30
            244;RULE;the_maiden_of_sugadinti;1;mode=normal noflag=maiden_imperfect
            245;RULE;pestilent_bloat;1;mode=normal noflag=bloat_imperfect
            247;RULE;sotetseg;1;mode=normal noflag=sotetseg_imperfect
            248;RULE;xarpus;1;mode=normal noflag=xarpus_imperfect
            249;RULE;verzik_vitur_p3;1;mode=normal noflag=verzik_imperfect
            241;RULE;verzik_vitur_p3;1;mode=normal noflag=verzik_nylo_damage
            242;RULE;sotetseg;1;mode=normal count>=3:sotetseg_ball_alone
            250;RULE;the_maiden_of_sugadinti;1;noprayloss
            251;RULE;xarpus;1;style=melee
            386;RULE;the_maiden_of_sugadinti;1;mode=entry noflag=bloodspawn_old
            372;RULE;the_maiden_of_sugadinti;1;mode=hard count<=15:maiden_trail
            392;RULE;xarpus;1;mode=entry noflag=xarpus_reflect
            376;RULE;xarpus;1;mode=hard flag=xarpus_full_heal nodmg
            243;RAID;TOB;1;mode=normal deathless noflag=maiden_imperfect noflag=bloat_imperfect noflag=nylo_wrong_style noflag=nylo_pillar_lost noflag=nylo_off_prayer noflag=sotetseg_imperfect noflag=xarpus_imperfect noflag=verzik_imperfect
            373;RULE;pestilent_bloat;1;mode=hard players>=3 noflag=bloat_stacked
            375;RULE;sotetseg;1;mode=hard noflag=sotetseg_ball_shared noflag=sotetseg_hit
            377;RULE;verzik_vitur_p3;1;mode=hard noflag=verzik_nylo_popped
            378;RULE;verzik_vitur_p3;1;mode=hard flag=verzik_teamwork
            379;RULE;verzik_vitur_p3;1;mode=hard noflag=verzik_p3_melee
            391;RULE;sotetseg;1;mode=entry size>=2 flag=sotetseg_team_split
            394;RULE;verzik_vitur_p2;1;mode=entry noflag=verzik_bounced
            395;RULE;verzik_vitur_p3;1;mode=entry flag=verzik_green_passed
            252;RAID;TOB;1;weapon=BARROWS_WEAPONS
            253;RAID;TOB;1;noweapon=scythe_of_vitur
            254;RAID;TOB;1;mode=normal size=2 t<26:00
            255;RAID;TOB;1;mode=normal size=3 t<20:00
            256;RAID;TOB;1;mode=normal size=3 t<17:30
            257;RAID;TOB;1;mode=normal size=4 t<17:00
            258;RAID;TOB;1;mode=normal size=4 t<15:00
            259;RAID;TOB;1;mode=normal size=5 t<16:00
            260;RAID;TOB;1;mode=normal size=5 t<14:15
            387;RULE;pestilent_bloat;1;mode=entry wear=*salve_amulet
            389;RULE;pestilent_bloat;1;mode=entry finishspec finish=*halberd
            396;RAID;TOB;1;mode=entry t<17:00
            381;RAID;TOB;1;mode=hard target
            382;RAID;TOB;1;mode=hard size=3 t<23:00
            383;RAID;TOB;1;mode=hard size=4 t<21:00
            384;RAID;TOB;1;mode=hard size=5 t<19:00
            385;RAID;TOB;50;mode=hard
            262;RULE;thermonuclear_smoke_devil;1;nodmg=boss
            263;RULE;thermonuclear_smoke_devil;1;spec
            421;RAID;TOA;1;level>=150 t<18:00
            422;RAID;TOA;1;level>=300 t<20:00
            423;RAID;TOA;1;level>=300 size=8 t<18:00
            424;RAID;TOA;1;level>=150 deathless noflag=toa_outside_supply
            425;RAID;TOA;1;nospirit deathless
            426;RAID;TOA;1;inv=ON_A_DIET,DEHYDRATION deathless
            428;RULE;zebak;1;style=melee nodeath
            429;RULE;tumekens_warden;1;size>=2 style=melee inv=INSANITY nodeath
            430;RAID;TOA;1;size>=2 deathless
            431;RAID;TOA;1;size=1 nodeath
            433;RULE;akkha;1;size>=2 inv=AKKHA nodmg=mechanic,hazard,offprayer
            436;RULE;baba;1;size>=2 inv=BABA nodmg=slam,falling_rocks,mechanic,hazard,offprayer
            439;RULE;zebak;1;inv=ZEBAK nodmg=poison,mechanic,hazard,other,offprayer
            442;RULE;kephri;1;size>=2 nodmg=boss,other,mechanic,hazard
            444;RULE;tumekens_warden;1;size>=2 inv=WARDENS nodmg=mechanic,hazard,other,offprayer
            445;RULE;tumekens_warden;1;size>=2 level>=300 inv=WARDENS nodmg=mechanic,hazard,other,offprayer
            446;RAID;TOA;1;level>=500 deathless
            447;RAID;TOA;1;level>=350 deathless
            448;RAID;TOA;1;inv=AKKHA,BABA,KEPHRI,ZEBAK,WARDENS deathless
            450;RULE;akkha;1;inv=AKKHA path>=4 nodeath
            453;RULE;baba;1;inv=BABA path>=4 nodeath
            455;RULE;zebak;1;inv=ZEBAK path>=4 nodeath
            456;RULE;kephri;1;inv=KEPHRI path>=4 nodeath
            452;RULE;baba;1;count<=4:baba_boulder nodeath
            454;RULE;zebak;1;inv=ZEBAK path>=4 nodeath noprayloss
            458;RULE;tumekens_warden;1;count<=2:warden_core nodeath
            459;RULE;tumekens_warden;1;inv=AERIAL_ASSAULT,STAY_VIGILANT,INSANITY nodeath
            460;RULE;tumekens_warden;1;inv=WARDENS level>=300 nodeath
            464;RAID;TOA;1;level>=50
            465;RAID;TOA;1;level>=100
            469;RAID;TOA;1;level>=300
            470;RAID;TOA;25;level>=300
            471;RAID;TOA;50;level>=300
            553;WINDOW;kill:tormented demon;2;t<=0:02
            554;WINDOW;kill:tormented demon;3;t<=0:03
            341;KILLS;jal-zek;1;
            345;RUN;mg:inferno;1;noattacker=tzkal-zuk nohitby=jaltok-jad
            347;RUN;mg:inferno;1;minhp>=50
            348;RUN;mg:inferno;1;noweapon=twisted_bow
            349;RUN;mg:inferno;1;style=melee,ranged
            351;RUN;mg:inferno;1;nokill=kill:jaltok-jad
            352;RUN;mg:inferno;1;t<65:00
            361;RUN;mg:tzhaar_ket_rak;1;stage=2
            362;RUN;mg:tzhaar_ket_rak;1;stage=4
            363;RUN;mg:tzhaar_ket_rak;1;stage=6
            364;RUN;mg:tzhaar_ket_rak;1;stage=1 t<0:45
            365;RUN;mg:tzhaar_ket_rak;1;stage=3 t<3:00
            366;RUN;mg:tzhaar_ket_rak;1;stage=5 t<5:00
            367;RUN;mg:tzhaar_ket_rak;1;stage=2 style=melee
            368;RUN;mg:tzhaar_ket_rak;1;stage=4 style=melee
            369;RUN;mg:tzhaar_ket_rak;1;stage=3 flag=noinv
            370;RUN;mg:tzhaar_ket_rak;1;stage=6 flag=fluke
            371;RUN;mg:tzhaar_ket_rak;1;stage=3 flag=multistyle
            548;RULE;kill:jaguar warrior;1;finishspec finish=*claws
            542;RUN;mg:fortis_colosseum;1;stage=4 flag=nomanticoredmg
            545;RUN;mg:fortis_colosseum;1;stage=7 flag=nominotaurheal
            546;RUN;mg:fortis_colosseum;1;stage=11 flag=oneoff
            547;RUN;mg:fortis_colosseum;1;stage=12 flag=reinforcements
            549;RUN;mg:fortis_colosseum;1;stage=12 flag=total28
            550;RUN;mg:fortis_colosseum;1;stage=12 flag=total24
            544;RUN;mg:fortis_colosseum;1;stage=12 flag=showboating
            149;RUN;mg:fight_caves;1;offprayerhitby=tztok-jad
            152;RUN;mg:fight_caves;1;noattacker=tz-kih
            153;RUN;mg:fight_caves;1;style=melee
            154;RUN;mg:fight_caves;1;noprayloss
            155;RUN;mg:fight_caves;1;t<30:00
            156;RUN;mg:fight_caves;1;t<26:30
            491;STREAK;vardorvis;5;nodmg=mechanic,hazard,swinging_axe,darting_spikes,strangle,offprayer
            272;STREAK;vorkath;5;nodmg=dragonfire_bomb,venomous_dragonfire,pink_dragonfire,mechanic,hazard
            278;STREAK;vorkath;10;
            500;STREAK;the_whisperer;5;nodmg=mechanic,hazard,soul_bolt,tentacle_lash,tentacle_whack,screech,soul_siphon,offprayer
            501;RULE;the_whisperer;1;book=ARCEUUS
            601;AVERAGE;yama;4;t<3:38
            602;AVERAGE;yama;4;t<3:00
            603;AVERAGE;yama;4;t<2:34
            604;RULE;yama;1;nopotion foodonly=plain_pizza/half_plain_pizza
            329;STREAK;zalcano;5;nodmg=rockfall,red_symbol,rock_explosion,zalcano_shockwave,mechanic,hazard
            229;RULE;zulrah;1;nodmg=other,toxic_cloud,venom_spit,tail_slam,mechanic,hazard
            228;WINDOW;kill:snakeling;3;t<=1t
            187;WINDOW;kill:husk;2;t<=1t
            179;RULE;giant_mole;1;count<=2:mole_burrow
            181;RULE;giant_mole;1;flag=mole_surfaced
            53;RULE;cerberus;1;noflag=cerberus_souls
            54;RULE;cerberus;1;count>=6:soul_negated
            3;RULE;abyssal_sire;1;noflag=sire_scion
            140;RULE;alchemical_hydra;1;noflag=hydra_empowered
            141;RULE;alchemical_hydra;1;noflag=hydra_flame_wall
            87;RULE;dusk;1;noflag=dawn_healed
            88;RULE;dusk;1;nodmg=lightning
            413;RULE;nex;1;noflag=nex_siphon_heal
            414;RULE;nex;1;noflag=nex_ice
            415;RULE;nex;1;noflag=nex_smoke_dash
            420;RULE;nex;1;noflag=nex_shadow_smash
            419;RULE;nex;1;noflag=nex_siphon_heal noflag=nex_ice noflag=nex_smoke_dash noflag=nex_shadow_smash
            59;RULE;chaos_elemental;1;noflag=chaos_unequipped
            112;RULE;crystalline_hunllef;1;nodmg=tornado,damaging_floor,stomp,offprayer noflag=hunllef_wrong_weapon
            102;RULE;corrupted_hunllef;1;nodmg=tornado,damaging_floor,stomp,offprayer noflag=hunllef_wrong_weapon
            113;RUN;mg:gauntlet;1;stage=1 flag=noarmour
            103;RUN;mg:gauntlet;1;stage=2 flag=noarmour
            114;RUN;mg:gauntlet;1;stage=1 flag=noegniol
            104;RUN;mg:gauntlet;1;stage=2 flag=noegniol
            116;RUN;mg:gauntlet;1;stage=1 flag=oneweapon
            106;RUN;mg:gauntlet;1;stage=2 flag=oneweapon
            115;RUN;mg:gauntlet;1;stage=1 wear=crystal_helm_perfected,crystal_body_perfected,crystal_legs_perfected
            105;RUN;mg:gauntlet;1;stage=2 wear=corrupted_helm_perfected,corrupted_body_perfected,corrupted_legs_perfected
            117;RUN;mg:gauntlet;1;stage=1 t<4:45
            118;RUN;mg:gauntlet;1;stage=1 t<3:45
            107;RUN;mg:gauntlet;1;stage=2 t<7:05
            108;RUN;mg:gauntlet;1;stage=2 t<6:05
            502;RULE;the_whisperer;1;count<10:shadow_realm_ticks
            208;RULE;sarachnis;1;noflag=repeat:web_spit
            527;RULE;MOONS;1;id=blue_moon count<=1:used:frozen_weapons/used:winter_storm || id=eclipse_moon count<=1:used:eclipse/used:moonlight_clones || id=blood_moon count<=1:used:blood_rain/used:call_jaguar
            594;RULE;branda_the_fire_queen;1;noattack=kill:ice+elemental/kill:fire+elemental
            643;RULE;maggot_king;1;moved<=40
            631;STREAK;shellbane_gryphon;5;weight<40
            493;RULE;vardorvis;1;gearvalue<=2000000
            302;RULE;vanguard_melee|vanguard_ranged|vanguard_magic;1;noflag=vanguard_reset
            307;RULE;muttadile;1;noflag=meat_tree_heal
            308;RULE;vespula_portal;1;noflag=vespula_landed
            310;RULE;tekton;1;count<=1:tekton_anvil
            309;RULE;vasa_nistirio;1;count<=1:vasa_teleport
            240;RULE;pestilent_bloat;1;mode=normal count<=2:bloat_shutdown
            46;RULE;skotizo;1;flag=skotizo_no_altars
            246;RULE;nylocas_vasilias;1;mode=normal noflag=nylo_wrong_style noflag=nylo_pillar_lost nodmg=offprayer
            374;RULE;nylocas_vasilias;1;mode=hard noflag=prinkipas_changed
            393;RULE;verzik_vitur;1;mode=entry noflag=verzik_pillar_lost
            380;RAID;TOB;1;mode=hard target deathless nospirit
            427;RAID;TOA;1;geartier<75
            150;RUN;mg:fight_caves;1;noflag=mejkot_healed
            151;RUN;mg:fight_caves;1;noflag=hurkot_healed
            343;RUN;mg:inferno;1;noflag=imkot_dug
            346;RUN;mg:inferno;1;noflag=pillar_early
            281;RUN;mg:wintertodt;1;flag=healed_pyromancer
            282;RUN;mg:wintertodt;1;flag=fixed_brazier
            283;RUN;mg:wintertodt;1;noflag=all_braziers_broken
            284;RUN;mg:wintertodt;1;noflag=pyromancer_fell
            285;RUN;mg:wintertodt;1;flag=warm_gear
            286;RUN;mg:wintertodt;1;flag=points_3000
            358;RUN;mg:tempoross;1;flag=ten_permits
            359;RUN;mg:tempoross;1;solo noflag=wave_hit noflag=fire_hit
            356;RUN;mg:tempoross;1;flag=five_fires
            357;RUN;mg:tempoross;1;flag=tether_repaired
            360;RUN;mg:tempoross;1;flag=both_cannons
            227;RULE;zulrah;1;flag=venge_last
            160;RULE;kalphite_queen;1;flag=venge_last
            270;RULE;vorkath;1;flag=vorkath_walk
            271;RULE;vorkath;1;killed>=1:kill:zombified+spawn noflag=vorkath_crumble
            84;RULE;dusk;1;nodmg=blinding
            85;RULE;dusk;1;nodmg=prison
            89;RULE;dusk;1;count<=1:dusk_prison
            90;RULE;dusk;1;nodmg=blinding,prison,falling_rocks,lightning noflag=dawn_healed count<=1:dusk_prison
            91;STREAK;dusk;5;nodmg=blinding,prison,falling_rocks,lightning noflag=dawn_healed count<=1:dusk_prison
            67;RULE;corporeal_beast;1;nodmg=dark_core nokill=kill:dark+energy+core
            334;RULE;kril_tsutsaroth;1;count<=0:kril_yarr
            510;RULE;the_leviathan;1;count<=1:leviathan_stun
            511;RULE;the_leviathan;1;weapon=*mithril maxhp<=25
            418;RULE;nex;1;flag=nex_coughing
            552;RULE;kill:tormented demon;1;flag=td_shield_down
            331;RULE;zalcano;1;solo
            330;RULE;zalcano;1;flag=zalcano_golem_tephra
            200;RULE;dagannoth_prime;1;finishspec finish=rune_thrownaxe
            6;RULE;abyssal_sire;1;flag=venge:explosion
            520;RULE;duke_sucellus;1;noflag=off_beat
            565;RULE;araxxor;1;noflag=target_moved
            563;RULE;araxxor;1;noflag=toxin_immune
            562;RULE;araxxor;1;nodmg=acid_spit,web_magic,acid_ball,hazard,other,offprayer noflag=araxxor_enrage_hit
            555;RULE;kill:tormented demon;1;flag=rapid_reload
            159;RULE;kalphite_queen;1;flag=defence_lowered
            591;RULE;branda_the_fire_queen;1;noflag=titans_both
            344;RUN;mg:inferno;1;flag=jads_quick
            27;RUN;run:barrows;1;killed>=6:barrows_brother nodmg=boss
            30;RUN;run:barrows;1;killed>=6:barrows_brother noflag=had_prayer
            26;RUN;run:barrows;1;killed>=6:barrows_brother noattacker=*dharok/*verac/*torag/*guthan
            605;RULE;yama;1;nodmg=shadow_orb,shadow_engulf,shadow_stomp,shadow_wave,shadow_crash noflag=yama_crash_far
            606;RULE;yama;1;nodmg=fire_orb,glyph_flames,meteor_strike,fire_streak,fire_wave noflag=yama_flare_nonspec
            607;RULE;yama;1;noflag=yama_judge_wrong noflag=yama_exile_hits
            608;RULE;yama;1;nodmg=shadow_orb,shadow_engulf,shadow_stomp,shadow_wave,shadow_crash,fire_orb,glyph_flames,meteor_strike,fire_streak,fire_wave,void_flare,claw_swipe,offprayer noflag=yama_crash_far noflag=yama_flare_nonspec noflag=yama_judge_wrong noflag=yama_exile_hits
            609;RULE;yama;1;flag=yama_contract
            4;RULE;abyssal_sire;1;count<=1:sire_stun
            5;RULE;abyssal_sire;1;nodmg=tentacle
            492;RULE;vardorvis;1;count>=300:vardorvis_enrage_ticks noflag=vardorvis_left_centre
            587;RULE;amoxliatl;1;noflag=amox_ice_shattered
            642;RULE;maggot_king;1;noflag=maggot_punish_missed
            645;RULE;maggot_king;1;nodmg=screech,carrion,slam,larva,hazard,offprayer noflag=maggot_healed
            595;RULE;branda_the_fire_queen;1;noflag=titans_wall_dissipated
            597;RULE;branda_the_fire_queen;1;count>=10:titan_elemental_kills
            598;RULE;branda_the_fire_queen;1;flag=titans_charge_kill
            408;RULE;phosanis_nightmare;1;noflag=phosani_sleepwalker_reached
            38;RULE;bryophyta;1;flag=bryophyta_quick_cut
            132;RULE;obor;1;noflag=obor_pushed
            186;RULE;the_nightmare;1;solo count<=100:nightmare_parasite_heal
            406;RULE;phosanis_nightmare;1;noflag=phosani_minion_survived
            481;RULE;phantom_muspah;1;flag=muspah_surrounded
            482;RULE;phantom_muspah;1;flag=muspah_three_drains
            564;RULE;araxxor;1;count>=6:araxxor_eggs_destroyed
            566;RULE;araxxor;1;flag=araxyte_betrayal
            526;RULE;eclipse_moon;1;noflag=eclipse_direct_hit
            531;WINDOW;MOONS;3;t<=30:00 fromstart distinct onlydmg=spear_thrust,shield_bash,atlatl_dart,macuahuitl_flurry
            572;RULE;the_hueycoatl;1;flag=huey_vulnerable_kill
            575;RULE;the_hueycoatl;1;wear=hueycoatl_hide_coif,hueycoatl_hide_body || wear=hueycoatl_hide_coif,hueycoatl_hide_chaps || wear=hueycoatl_hide_coif,hueycoatl_hide_vambraces || wear=hueycoatl_hide_body,hueycoatl_hide_chaps || wear=hueycoatl_hide_body,hueycoatl_hide_vambraces || wear=hueycoatl_hide_chaps,hueycoatl_hide_vambraces
            188;RULE;the_nightmare;1;nodmg=grasping_claws,surge,parasite_burst,offprayer noflag=nightmare_spored
            403;RULE;phosanis_nightmare;1;onlydmg=husk,power_blast,weakened_parasite_burst noflag=nightmare_spored noflag=phosani_sleepwalker_reached
            407;RULE;phosanis_nightmare;1;noflag=phosani_prayer_disabled
            """;

    private static final Map<Integer, Spec> SPECS = new LinkedHashMap<>();

    static {
        Map<String, String> macros = Map.of("AKKHA", AKKHA, "BABA", BABA, "KEPHRI", KEPHRI, "ZEBAK", ZEBAK, "WARDENS", WARDENS,
                "BARROWS_WEAPONS", BARROWS_WEAPONS, "DEMONBANE", CombatAchievements.DEMONBANE.replace('|', '/'));
        for (String line : TABLE.split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            String[] f = line.split(";", -1);
            String target = f[2].equals("MOONS") ? MOONS : f[2];
            String rule = f[4];
            for (Map.Entry<String, String> e : macros.entrySet()) {
                rule = rule.replace("=" + e.getKey(), "=" + e.getValue()).replace("," + e.getKey(), "," + e.getValue());
            }
            Spec prev = SPECS.put(Integer.parseInt(f[0]), new Spec(Track.valueOf(f[1]), target, Integer.parseInt(f[3]), rule));
            if (prev != null) {
                throw new IllegalStateException("Duplicate combat achievement rule " + f[0]);
            }
        }
    }

    static Spec spec(int wikiId) {
        return SPECS.get(wikiId);
    }

    static Map<Integer, Spec> specs() {
        return Collections.unmodifiableMap(SPECS);
    }
}
