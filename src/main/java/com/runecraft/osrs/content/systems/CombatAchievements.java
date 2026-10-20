package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ItemSink;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Combat Achievements: the six OSRS tiers with their point values, reward thresholds and rewards, and every task
 * from the OSRS Wiki ({@link CombatAchievementData}). Each task is classified by how this world can observe it:
 * <ul>
 *   <li>{@link Track#KC}: a boss kill count (or minigame completion count) reaching a number;</li>
 *   <li>{@link Track#KILLS}: killing a number of regular monsters;</li>
 *   <li>fight tasks ({@link Track#SPEED}, {@link Track#NO_DAMAGE}, {@link Track#PRAYED}, {@link Track#NO_PRAYER_LOSS},
 *       {@link Track#STYLE}): a single kill of a boss or monster meeting a condition measured during the fight;</li>
 *   <li>{@link Track#RULE}, {@link Track#STREAK}, {@link Track#WINDOW}, {@link Track#AVERAGE}, {@link Track#RAID} and
 *       {@link Track#RUN}: tasks with conditions from {@link CombatAchievementRules} (damage from named attacks, group
 *       size, supplies, gear, raid modes and invocations, kill streaks, kills within a time, minigame runs);</li>
 *   <li>{@link Track#UNTRACKED}: mechanics the game doesn't model yet (room puzzles, awakened bosses, delve
 *       levels...). These stay visible in the task list but can't be completed.</li>
 * </ul>
 * Points-based rewards as in OSRS: a tier's rewards unlock at its cumulative point total (the grandmaster tier
 * needs every task), regardless of which tiers the points came from.
 */
public final class CombatAchievements {
    public enum Tier {
        EASY(1, 5_000, 20), MEDIUM(2, 10_000, 30), HARD(3, 15_000, 40), ELITE(4, 25_000, 50), MASTER(5, 35_000, 60),
        GRANDMASTER(6, 50_000, 70);

        /** Points per task of this tier. */
        public final int points;
        /** XP from the tier's antique lamp and the minimum level of the skill it can go into. */
        public final int lampXp;
        public final int lampMinLevel;

        Tier(int points, int lampXp, int lampMinLevel) {
            this.points = points;
            this.lampXp = lampXp;
            this.lampMinLevel = lampMinLevel;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public String displayName() {
            return Character.toUpperCase(id().charAt(0)) + id().substring(1);
        }

        /** The Ghommal's hilt handed out for this tier. */
        public String hilt() {
            return HILT_PREFIX + (ordinal() + 1);
        }

        /** The tier's antique lamp item. */
        public String lamp() {
            return LAMP_PREFIX + id();
        }
    }

    /** The OSRS task categories. */
    public enum Type {
        KILL_COUNT("Kill Count"), MECHANICAL("Mechanical"), PERFECTION("Perfection"), RESTRICTION("Restriction"), SPEED("Speed"),
        STAMINA("Stamina");

        public final String label;

        Type(String label) {
            this.label = label;
        }

        static Type byLabel(String label) {
            for (Type t : values()) {
                if (t.label.equals(label)) {
                    return t;
                }
            }
            throw new IllegalArgumentException(label);
        }
    }

    /** How the game observes a task. */
    public enum Track {
        KC("Tracked by kill count."),
        KILLS("Tracked by kills."),
        SPEED("Tracked: time from your first hit (or the boss's) to the kill."),
        NO_DAMAGE("Tracked: take no damage at all during the fight."), // approx: stricter than "avoidable damage"
        PRAYED("Tracked: every hit you take must land while a protection prayer is up."),
        NO_PRAYER_LOSS("Tracked: your prayer points must not drop during the fight."),
        STYLE("Tracked: only damage the target with the allowed attack style."),
        WEAPON("Tracked: only hit the target with the named weapons."),
        FINISH("Tracked: land the killing blow with the named weapons."),
        UNTRACKED("Not tracked in this world yet."),
        RULE("Tracked: one kill meeting the task's conditions."),
        STREAK("Tracked: kills in a row without dying or leaving the area (64 blocks)."),
        WINDOW("Tracked: the kills must all land within the time limit."),
        AVERAGE("Tracked: average kill time over your most recent kills."),
        RAID("Tracked: raid completions meeting the task's conditions (mode, team size, time, deaths, invocations)."),
        RUN("Tracked: a whole minigame run, from entering to the final kill, meeting the conditions.");

        public final String hint;

        Track(String hint) {
            this.hint = hint;
        }

        public boolean fight() {
            return this == SPEED || this == NO_DAMAGE || this == PRAYED || this == NO_PRAYER_LOSS || this == STYLE || this == WEAPON
                    || this == FINISH;
        }
    }

    /**
     * A task. {@code target} depends on the track: a kill count expression for KC ('|' sums keys, '&' takes the
     * smallest; a key is a boss id or "mg:" + minigame id), lower-case monster names for KILLS ('|' alternatives),
     * or a fight key for fight tracks (a canonical boss id, or "kill:" + lower-case monster name). {@code ticks} is
     * the time limit of SPEED tasks, {@code style} the allowed attack style of STYLE tasks or the weapon ids of
     * WEAPON/FINISH tasks ('|' alternatives, a leading '*' matches ids containing the rest, "fists" an empty hand).
     * {@code rule} holds the {@link CombatAchievementRules} conditions of the rule-based tracks.
     */
    public record Task(String id, int wikiId, String name, String description, Tier tier, String monster, Type type, Track track,
                       String target, int count, int ticks, String style, String rule) {
        public int points() {
            return tier.points;
        }

        /** Whether a fight key (or kill key) is one of this task's targets ('|' alternatives). */
        public boolean targets(String key) {
            if (target == null || key == null) {
                return false;
            }
            for (String t : target.split("\\|")) {
                if (t.equals(key)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** What the game can observe for one OSRS boss/monster page: kill count keys, a fight key and/or monster names. */
    record Target(String kc, String fight, String kills) {
    }

    public static final String HILT_PREFIX = "ghommals_hilt_";
    public static final String LAMP_PREFIX = "ca_antique_lamp_";
    public static final String LUCKY_PENNY = "ghommals_lucky_penny";
    /** Ghommal's lucky penny: chance not to use a charge of a charged item. */
    public static final double LUCKY_PENNY_CHANCE = 0.05;
    /** Ghommal stands in front of the Warriors' Guild. */
    public static final int GHOMMAL_X = 2879;
    public static final int GHOMMAL_Y = 3547;
    /** OSRS demonbane weapons in this world. */
    public static final String DEMONBANE = "arclight|emberlight|darklight|silverlight|scorching_bow|purging_staff|burning_claws";

    // Flags and counters boss scripts and minigames report for rules ({@code flag=}, {@code noflag=}, {@code count<=N:}).
    /** The Giant Mole resurfaced within the last 10 seconds (Whack-a-Mole). */
    public static final String FLAG_MOLE_SURFACED = "mole_surfaced";
    /** Times the Giant Mole burrowed. */
    public static final String COUNT_MOLE_BURROW = "mole_burrow";
    /** Cerberus summoned her souls. */
    public static final String FLAG_CERBERUS_SOULS = "cerberus_souls";
    /** Summoned soul attacks negated with the right protection prayer. */
    public static final String COUNT_SOUL_NEGATED = "soul_negated";
    /** A scion matured at the Abyssal Sire. */
    public static final String FLAG_SIRE_SCION = "sire_scion";
    /** The Alchemical Hydra used its flame wall. */
    public static final String FLAG_HYDRA_FLAME_WALL = "hydra_flame_wall";
    /** The Alchemical Hydra absorbed the wrong chemicals. */
    public static final String FLAG_HYDRA_EMPOWERED = "hydra_empowered";
    /** Dawn healed from an energy sphere. */
    public static final String FLAG_DAWN_HEALED = "dawn_healed";
    /** Someone was hit by Nex's Smoke Dash. */
    public static final String FLAG_NEX_SMOKE_DASH = "nex_smoke_dash";
    /** Someone was hit by Nex's Shadow Smash. */
    public static final String FLAG_NEX_SHADOW_SMASH = "nex_shadow_smash";
    /** Someone took damage from an Ice special (Contain this! or the ice prison). */
    public static final String FLAG_NEX_ICE = "nex_ice";
    /** Nex healed from her Blood Siphon. */
    public static final String FLAG_NEX_SIPHON_HEAL = "nex_siphon_heal";
    /** The Chaos Elemental unequipped one of your items. */
    public static final String FLAG_CHAOS_UNEQUIPPED = "chaos_unequipped";
    /** You attacked the Hunllef with the style it protects from. */
    public static final String FLAG_HUNLLEF_WRONG_WEAPON = "hunllef_wrong_weapon";
    /** Gauntlet runs (judged by the session): no armour sung, no egniol potion made, at most one attuned weapon sung. */
    public static final String FLAG_GAUNTLET_NO_ARMOUR = "noarmour";
    public static final String FLAG_GAUNTLET_NO_EGNIOL = "noegniol";
    public static final String FLAG_GAUNTLET_ONE_WEAPON = "oneweapon";
    /** Game ticks spent in the Whisperer's Shadow Realm. */
    public static final String COUNT_SHADOW_REALM_TICKS = "shadow_realm_ticks";
    /** A Yt-HurKot healed a Jad. */
    public static final String FLAG_HURKOT_HEALED = "hurkot_healed";
    /** A Yt-MejKot healed a monster. */
    public static final String FLAG_MEJKOT_HEALED = "mejkot_healed";
    /** A Jal-ImKot dug. */
    public static final String FLAG_IMKOT_DUG = "imkot_dug";
    /** An Inferno pillar fell before wave 67. */
    public static final String FLAG_PILLAR_EARLY = "pillar_early";
    /** You healed a fallen pyromancer. */
    public static final String FLAG_HEALED_PYROMANCER = "healed_pyromancer";
    /** You repaired a broken brazier. */
    public static final String FLAG_FIXED_BRAZIER = "fixed_brazier";
    /** All four braziers were broken at once. */
    public static final String FLAG_ALL_BRAZIERS_BROKEN = "all_braziers_broken";
    /** A pyromancer fell. */
    public static final String FLAG_PYROMANCER_FELL = "pyromancer_fell";
    /** Four pieces of warm equipment worn when the Wintertodt was subdued. */
    public static final String FLAG_WARM_GEAR = "warm_gear";
    /** 3000 or more Wintertodt points. */
    public static final String FLAG_POINTS_3000 = "points_3000";
    /** You loaded both of Tempoross' cannons. */
    public static final String FLAG_BOTH_CANNONS = "both_cannons";
    /** A Tempoross wave hit you. */
    public static final String FLAG_WAVE_HIT = "wave_hit";
    /** 10 reward permits from one Tempoross fight. */
    public static final String FLAG_TEN_PERMITS = "ten_permits";
    /** Tempoross: a fire caught the player ("The Lone Angler"). */
    public static final String FLAG_FIRE_HIT = "fire_hit";
    /** Tempoross: the player doused at least 5 fires ("Master of Buckets"). */
    public static final String FLAG_FIVE_FIRES = "five_fires";
    /** Tempoross: the player repaired a mast or totem pole ("Calm Before the Storm"). */
    public static final String FLAG_TETHER_REPAIRED = "tether_repaired";
    /** Times Tekton retreated to his anvil. */
    public static final String COUNT_TEKTON_ANVIL = "tekton_anvil";
    /** Times Vasa Nistirio used his teleport attack. */
    public static final String COUNT_VASA_TELEPORT = "vasa_teleport";
    /** The Vanguards reset their health (CoX "Perfectly Balanced"). */
    public static final String FLAG_VANGUARD_RESET = "vanguard_reset";
    /** A Muttadile healed at the meat tree (CoX "Mutta-diet"). */
    public static final String FLAG_MEAT_TREE_HEAL = "meat_tree_heal";
    /** Vespula was forced to land (CoX "Redemption Enthusiast"). */
    public static final String FLAG_VESPULA_LANDED = "vespula_landed";
    /** Times the Pestilent Bloat shut down. */
    public static final String COUNT_BLOAT_SHUTDOWN = "bloat_shutdown";
    /** A blood spawn lived longer than 10 seconds (ToB "Anticoagulants"). */
    public static final String FLAG_BLOODSPAWN_OLD = "bloodspawn_old";
    /** Blood trail tiles the Maiden's blood spawn left (ToB "Stop Right There!"). */
    public static final String COUNT_MAIDEN_TRAIL = "maiden_trail";
    /** Xarpus retaliated against an attack from the quadrant he stared at (ToB "Don't Look at Me!"). */
    public static final String FLAG_XARPUS_REFLECT = "xarpus_reflect";
    /** Xarpus was healed to full health by exhumeds (ToB "Harder Mode II"). */
    public static final String FLAG_XARPUS_FULL_HEAL = "xarpus_full_heal";
    /** No awakened altar was active (Skotizo "Demonic Weakening"; taken back while one is). */
    public static final String FLAG_SKOTIZO_NO_ALTARS = "skotizo_no_altars";
    /** Someone attacked a nylocas with a style it is immune to (ToB "Perfect Nylocas"). */
    public static final String FLAG_NYLO_WRONG_STYLE = "nylo_wrong_style";
    /** A support collapsed in the Nylocas room (ToB "Perfect Nylocas"). */
    public static final String FLAG_NYLO_PILLAR_LOST = "nylo_pillar_lost";
    /** A Nylocas Prinkipas changed style (ToB HM "Royal Affairs"). */
    public static final String FLAG_PRINKIPAS_CHANGED = "prinkipas_changed";
    /** A supporting pillar collapsed during Verzik's first phase (ToB Entry "No-Pillar"). */
    public static final String FLAG_VERZIK_PILLAR_LOST = "verzik_pillar_lost";
    /** The fight broke a ToB room's "Perfect" condition ("Perfect Maiden", "Perfect Bloat", ... round 33). */
    public static final String FLAG_MAIDEN_IMPERFECT = "maiden_imperfect";
    public static final String FLAG_BLOAT_IMPERFECT = "bloat_imperfect";
    public static final String FLAG_SOTETSEG_IMPERFECT = "sotetseg_imperfect";
    public static final String FLAG_XARPUS_IMPERFECT = "xarpus_imperfect";
    public static final String FLAG_VERZIK_IMPERFECT = "verzik_imperfect";
    /** A Verzik nylocas exploded on someone (ToB "Pop It"). */
    public static final String FLAG_VERZIK_NYLO_DAMAGE = "verzik_nylo_damage";
    /** Sotetseg's ball of death taken alone and survived (ToB "A Timely Snack"). */
    public static final String COUNT_SOTETSEG_BALL_ALONE = "sotetseg_ball_alone";
    /** The Great Olm fight broke "Perfect Olm" (listed specials, a claw regrowing, one acid pool twice running). */
    public static final String FLAG_OLM_IMPERFECT = "olm_imperfect";
    /** The Olm used Crystal Burst, lightning, portals or the healing claw in his penultimate phase (CoX "A Not So Special Lizard"). */
    public static final String FLAG_OLM_PENULTIMATE_SPECIAL = "olm_penultimate_special";
    /** Rolling boulders destroyed in Ba-Ba's room (ToA "I'm in a rush"). */
    public static final String COUNT_BABA_BOULDER = "baba_boulder";
    /** Times a Warden's core was ejected in phase 2 (ToA "Down Do Specs"). */
    public static final String COUNT_WARDEN_CORE = "warden_core";
    /** Someone in a ToA party ate or drank supplies brought into the tomb (ToA "You are not prepared" fails). */
    public static final String FLAG_TOA_OUTSIDE_SUPPLY = "toa_outside_supply";
    /** Crumble Undead was cast on Vorkath's zombified spawn ("Zombie Destroyer" fails). */
    public static final String FLAG_VORKATH_CRUMBLE = "vorkath_crumble";
    /** 12 hits on Vorkath during one acid phase without rapid fire or acid damage ("The Walk"). */
    public static final String FLAG_VORKATH_WALK = "vorkath_walk";
    /** The player's last damage on the target was a Vengeance recoil (cleared by the player's next attack). */
    public static final String FLAG_VENGEANCE_LAST = "venge_last";
    /** Times Dusk used his prison attack (Grotesque Guardians "Done before Dusk"). */
    public static final String COUNT_DUSK_PRISON = "dusk_prison";
    /** Times K'ril Tsutsaroth used his "YARRRRRRR!" special ("Yarr No More"). */
    public static final String COUNT_KRIL_YARR = "kril_yarr";
    /** Times the Leviathan was stunned by a shadow spell ("Serpentine Solo"). */
    public static final String COUNT_LEVIATHAN_STUN = "leviathan_stun";
    /** Someone near Nex is coughing from her Choke virus ("I should see a doctor"; taken back when nobody is). */
    public static final String FLAG_NEX_COUGHING = "nex_coughing";
    /** A tormented demon's killing blow landed while its fire shield was down ("Through Fire and Flames"). */
    public static final String FLAG_TD_SHIELD_DOWN = "td_shield_down";
    /** Prefix of the flag a Vengeance recoil sets, followed by the attack key of the hit it reflected ("Demonic Rebound"). */
    public static final String FLAG_VENGEANCE_ON = "venge:";
    /** The player attacked the target on a game tick it didn't attack them ("Mirror Image" fails). */
    public static final String FLAG_OFF_BEAT = "off_beat";
    /** The fight's target moved more than a tile from where the fight started ("Swimming in Venom" fails). */
    public static final String FLAG_TARGET_MOVED = "target_moved";
    /** Three different tormented demons hit within 3 seconds with a crossbow or ballista ("Rapid Reload"). */
    public static final String FLAG_RAPID_RELOAD = "rapid_reload";
    /** The player's own special attack lowered the target's Defence ("Chitin Penetrator"). */
    public static final String FLAG_DEFENCE_LOWERED = "defence_lowered";
    /** The player had poison or venom immunity at some point ("Let it seep in" fails). */
    public static final String FLAG_TOXIN_IMMUNE = "toxin_immune";
    /** The player had any prayer points at some point ("Faithless Crypt Run" fails). */
    public static final String FLAG_HAD_PRAYER = "had_prayer";
    /** Araxxor took damage during its enrage phase ("Perfect Araxxor 2" fails). */
    public static final String FLAG_ARAXXOR_ENRAGE_HIT = "araxxor_enrage_hit";
    /** Branda was attacked while Eldric still stood ("One by one" fails). */
    public static final String FLAG_TITANS_BOTH = "titans_both";
    /** Inferno wave 68 was cleared within 30 seconds of the first JalTok-Jad dying ("Playing with Jads"). */
    public static final String FLAG_JADS_QUICK = "jads_quick";
    /** Barrows runs: the run tracker's key (from the first brother fought to the chest). */
    public static final String BARROWS_RUN = "run:barrows";
    /** A fight is forgotten after this many game ticks without either side attacking. */
    public static final int FIGHT_TIMEOUT_TICKS = 100;

    private static final Map<String, Target> TARGETS = new LinkedHashMap<>();
    /** Secondary forms and partner bosses whose kill/fight counts as the main boss's. */
    private static final Map<String, String> CANONICAL = new LinkedHashMap<>();
    /** Raid rooms named in task descriptions, for raid-page tasks about a single sub-boss. */
    private static final Map<String, String> SUB_BOSSES = new LinkedHashMap<>();

    private static void boss(String monster, String kc, String fight) {
        TARGETS.put(monster, new Target(kc, fight, null));
    }

    private static void boss(String monster, String id) {
        boss(monster, id, id);
    }

    private static void monster(String monster, String names) {
        String first = names.split("\\|")[0];
        TARGETS.put(monster, new Target(null, "kill:" + first, names));
    }

    static {
        boss("Abyssal Sire", "abyssal_sire");
        boss("Alchemical Hydra", "alchemical_hydra");
        boss("Amoxliatl", "amoxliatl");
        boss("Araxxor", "araxxor");
        boss("Barrows", "barrows_chests", "barrows_brother");
        boss("Brutus", "brutus");
        boss("Bryophyta", "bryophyta");
        // The reworked Wilderness bosses share their pages with their singles-plus variants (approx).
        boss("Callisto", "callisto|artio", "callisto");
        boss("Cerberus", "cerberus");
        boss("Chambers of Xeric", "great_olm", null);
        boss("Chaos Elemental", "chaos_elemental");
        boss("Chaos Fanatic", "chaos_fanatic");
        boss("Commander Zilyana", "commander_zilyana");
        boss("Corporeal Beast", "corporeal_beast");
        boss("Corrupted Hunllef", "corrupted_hunllef");
        boss("Crazy Archaeologist", "crazy_archaeologist");
        boss("Crystalline Hunllef", "crystalline_hunllef");
        boss("Dagannoth Prime", "dagannoth_prime");
        boss("Dagannoth Rex", "dagannoth_rex");
        boss("Dagannoth Supreme", "dagannoth_supreme");
        boss("Doom of Mokhaiotl", "doom_of_mokhaiotl");
        boss("Duke Sucellus", "duke_sucellus");
        boss("Fortis Colosseum", "sol_heredit");
        boss("Fragment of Seren", "fragment_of_seren");
        boss("Galvek", "galvek");
        boss("General Graardor", "general_graardor");
        boss("Giant Mole", "giant_mole");
        boss("Glough", "glough_black_demon");
        boss("Grotesque Guardians", "dusk");
        boss("Hespori", "hespori");
        boss("K'ril Tsutsaroth", "kril_tsutsaroth");
        boss("Kalphite Queen", "kalphite_queen");
        boss("King Black Dragon", "king_black_dragon");
        boss("Kraken", "kraken");
        boss("Kree'arra", "kreearra");
        boss("Leviathan", "the_leviathan");
        boss("Maggot King", "maggot_king");
        boss("Moons of Peril", "blue_moon&eclipse_moon&blood_moon", null);
        boss("Nex", "nex");
        boss("Obor", "obor");
        boss("Phantom Muspah", "phantom_muspah");
        boss("Phosani's Nightmare", "phosanis_nightmare");
        boss("Royal Titans", "branda_the_fire_queen");
        boss("Sarachnis", "sarachnis");
        boss("Scorpia", "scorpia");
        boss("Scurrius", "scurrius");
        boss("Shellbane gryphon", "shellbane_gryphon");
        boss("Skotizo", "skotizo");
        boss("Tempoross", "mg:tempoross", null);
        boss("The Hueycoatl", "the_hueycoatl");
        boss("The Mimic", "the_mimic");
        boss("The Nightmare", "the_nightmare");
        boss("Theatre of Blood", "verzik_vitur_p3", null);
        boss("Theatre of Blood: Entry Mode", "verzik_vitur_p3", null); // approx: no entry mode, a normal run counts
        boss("Thermonuclear Smoke Devil", "thermonuclear_smoke_devil");
        boss("Tombs of Amascut", "tumekens_warden", null);
        boss("Tombs of Amascut: Entry Mode", "tumekens_warden", null); // "Entry mode (or above)"
        boss("TzKal-Zuk", "tzkal_zuk");
        boss("TzTok-Jad", "mg:fight_caves", "tztok_jad");
        boss("Vardorvis", "vardorvis");
        boss("Venenatis", "venenatis|spindel", "venenatis");
        boss("Vet'ion", "vetion|calvarion", "vetion");
        boss("Vorkath", "vorkath");
        boss("Whisperer", "the_whisperer");
        boss("Wintertodt", "mg:wintertodt", null);
        boss("Yama", "yama");
        boss("Zalcano", "zalcano");
        boss("Zulrah", "zulrah");
        monster("Aberrant Spectre", "aberrant spectre");
        monster("Basilisk Knight", "basilisk knight");
        monster("Black Dragon", "black dragon");
        monster("Bloodveld", "bloodveld|mutated bloodveld");
        monster("Brutal Black Dragon", "brutal black dragon");
        monster("Fire Giant", "fire giant");
        monster("Gargoyle", "gargoyle");
        monster("Greater Demon", "greater demon");
        monster("Hellhound", "hellhound");
        monster("Kurask", "kurask");
        monster("Lizardman Shaman", "lizardman shaman");
        monster("Skeletal Wyvern", "skeletal wyvern");
        monster("Tormented Demon", "tormented demon");
        monster("Wyrm", "wyrm");
        monster("Demonic Gorilla", "demonic gorilla");
        boss("Deranged Archaeologist", "deranged_archaeologist");
        // TzHaar-Ket-Rak's challenges are minigame runs (rules in CombatAchievementRules, "stage" = the challenge).

        CANONICAL.put("zulrah_magma", "zulrah");
        CANONICAL.put("zulrah_tanzanite", "zulrah");
        CANONICAL.put("alchemical_hydra_blue", "alchemical_hydra");
        CANONICAL.put("alchemical_hydra_red", "alchemical_hydra");
        CANONICAL.put("alchemical_hydra_enraged", "alchemical_hydra");
        CANONICAL.put("kalphite_queen_airborne", "kalphite_queen");
        CANONICAL.put("dawn", "dusk");
        // Awakened DT2 kills have their own kill count; a fight with an awakened boss ends when it goes up.
        for (String dt2 : com.runecraft.osrs.content.bosses.AwakenedBosses.BOSSES) {
            CANONICAL.put(com.runecraft.osrs.content.bosses.AwakenedBosses.kcKey(dt2), dt2);
        }
        CANONICAL.put("eldric_the_ice_king", "branda_the_fire_queen");
        CANONICAL.put("verzik_vitur", "verzik_vitur_p3");
        CANONICAL.put("verzik_vitur_p2", "verzik_vitur_p3");
        CANONICAL.put("olm_left_claw", "great_olm");
        CANONICAL.put("olm_right_claw", "great_olm");
        CANONICAL.put("elidinis_warden", "tumekens_warden");
        for (String b : new String[]{"ahrim", "dharok", "guthan", "karil", "torag", "verac"}) {
            CANONICAL.put(b, "barrows_brother");
        }

        SUB_BOSSES.put("Tekton", "tekton");
        SUB_BOSSES.put("Ice Demon", "cox_ice_demon");
        SUB_BOSSES.put("Vasa", "vasa_nistirio");
        SUB_BOSSES.put("Vespula", "vespula");
        SUB_BOSSES.put("Great Olm", "great_olm");
        SUB_BOSSES.put("Maiden of Sugadinti", "the_maiden_of_sugadinti");
        SUB_BOSSES.put("Pestilent Bloat", "pestilent_bloat");
        SUB_BOSSES.put("Nylocas Vasilias", "nylocas_vasilias");
        SUB_BOSSES.put("Sotetseg", "sotetseg");
        SUB_BOSSES.put("Xarpus", "xarpus");
        SUB_BOSSES.put("Verzik", "verzik_vitur_p3");
        SUB_BOSSES.put("Kephri", "kephri");
        SUB_BOSSES.put("Akkha", "akkha");
        SUB_BOSSES.put("Ba-Ba", "baba");
        SUB_BOSSES.put("Zebak", "zebak");
    }

    private CombatAchievements() {
    }

    // ------------------------------------------------------------------ classification

    private static final Pattern COUNT = Pattern.compile("(\\d+) times?\\b");
    private static final Pattern CLOCK = Pattern.compile("(\\d+):(\\d{2})");
    private static final Pattern MIN_SEC = Pattern.compile("(\\d+) minutes?,? (?:and )?(\\d+) seconds");
    private static final Pattern SECONDS = Pattern.compile("(\\d+) seconds");
    private static final Pattern MINUTES = Pattern.compile("(\\d+) (?:minutes|mins)");

    /** "Kill X 25 times." gives 25, "Kill X once." / "Kill X." gives 1. */
    static int parseCount(String description) {
        Matcher m = COUNT.matcher(description);
        return m.find() ? Integer.parseInt(m.group(1)) : 1;
    }

    /** The time limit of a speed task in game ticks (0.6 s), or 0 if the description has none. */
    static int parseTicks(String description) {
        String d = description.toLowerCase(Locale.ROOT);
        int seconds;
        Matcher m;
        if ((m = CLOCK.matcher(d)).find()) {
            seconds = Integer.parseInt(m.group(1)) * 60 + Integer.parseInt(m.group(2));
        } else if ((m = MIN_SEC.matcher(d)).find()) {
            seconds = Integer.parseInt(m.group(1)) * 60 + Integer.parseInt(m.group(2));
        } else if ((m = Pattern.compile("(\\d+) minute (\\d+) seconds").matcher(d)).find()) {
            seconds = Integer.parseInt(m.group(1)) * 60 + Integer.parseInt(m.group(2));
        } else if ((m = SECONDS.matcher(d)).find()) {
            seconds = Integer.parseInt(m.group(1));
        } else if ((m = MINUTES.matcher(d)).find() || (m = Pattern.compile("(\\d+) minute\\b").matcher(d)).find()) {
            seconds = Integer.parseInt(m.group(1)) * 60;
        } else {
            return 0;
        }
        return seconds * 5 / 3;
    }

    /** Things a single tracked kill can't show: streaks, group sizes, raid modes, awakened/delve variants. */
    private static boolean unsupported(String d) {
        String[] words = {"awakened", "delve", "levels 1-8", "level 1 ", " times", "in a row", "consecutively", "without leaving",
                "group", "-scale", "(trio)", "(duo)", "team", "anyone", "any player", "everyone", "all three", "average",
                "contract", "challenge", "invocation", "whilst completing", "while completing", "entering", "players at the start",
                "elementals", "pillar", "free to play", "giant cave", "spawn", "minions"};
        for (String w : words) {
            if (d.contains(w)) {
                return true;
            }
        }
        return false;
    }

    private static Task classify(int wikiId, Tier tier, String monster, Type type, String name, String description) {
        String d = description.toLowerCase(Locale.ROOT);
        Target tg = TARGETS.get(monster);
        String fight = tg == null ? null : tg.fight();
        if (fight == null) {
            for (Map.Entry<String, String> e : SUB_BOSSES.entrySet()) {
                if (description.contains(e.getKey())) {
                    fight = e.getValue();
                    break;
                }
            }
        }
        Track track = Track.UNTRACKED;
        String target = null;
        int count = 1;
        int ticks = 0;
        String style = null;
        switch (type) {
            case KILL_COUNT -> {
                boolean variant = d.contains("awakened") || d.contains("delve") || d.contains("contract") || d.contains("giant cave")
                        || d.contains("jal-zek");
                if (tg != null && !variant) {
                    count = parseCount(description);
                    if (tg.kills() != null) {
                        track = Track.KILLS;
                        target = tg.kills();
                    } else if (tg.kc() != null) {
                        track = Track.KC;
                        target = tg.kc();
                    }
                }
            }
            case SPEED -> {
                boolean whole = d.startsWith("complete") || d.contains("times in") || d.contains(" times within") || d.contains("average")
                        || d.contains("all three") || d.contains("levels 1-8") || d.contains("level 1 ") || d.contains("-scale")
                        || d.contains("(solo)") || d.contains("entering");
                ticks = parseTicks(description);
                if (fight != null && !whole && ticks > 0) {
                    track = Track.SPEED;
                    target = fight;
                }
            }
            case PERFECTION -> {
                if (fight != null && !unsupported(d)) {
                    if (d.matches(".*without taking (any )?damage off prayer\\.?")) {
                        track = Track.PRAYED;
                    } else if (d.matches(".*without taking (any )?(avoidable )?damage\\.?")
                            || d.matches(".*without taking any damage from (her|it|its attacks|him)\\.?")
                            || d.matches(".*without (her|it|him) (damaging|dealing damage to|hitting) anyone\\.?")) {
                        track = Track.NO_DAMAGE;
                    }
                    if (track != Track.UNTRACKED) {
                        target = fight;
                    }
                }
            }
            case RESTRICTION -> {
                if (fight != null && !unsupported(d) && !d.contains("fight caves")) {
                    if (d.matches(".*without losing any prayer points\\.?")) {
                        track = Track.NO_PRAYER_LOSS;
                    } else if (d.contains("with a crush weapon")) {
                        style = "crush";
                    } else if (d.contains("with a stab weapon")) {
                        style = "stab";
                    } else if (d.contains("melee weapons only") || d.contains("with melee only")) {
                        style = "melee";
                    } else if (d.contains("using only magical damage")) {
                        style = "magic";
                    }
                    if (style != null) {
                        track = Track.STYLE;
                    } else if (d.contains("using only the verac's flail as a weapon")) {
                        track = Track.WEAPON;
                        style = "veracs_flail";
                    } else if (d.contains("with only your fists")) {
                        track = Track.WEAPON;
                        style = "fists";
                    } else if (d.contains("using only a spear, hasta or halberd")) {
                        track = Track.WEAPON;
                        style = "*spear|*hasta|*halberd";
                    } else if (d.contains("with a demonbane weapon equipped")) {
                        track = Track.WEAPON;
                        style = DEMONBANE;
                    } else if (d.startsWith("finish off") && d.contains("with a demonbane weapon")) {
                        track = Track.FINISH;
                        style = DEMONBANE;
                    }
                    if (track != Track.UNTRACKED) {
                        target = fight;
                    }
                }
            }
            default -> {
            }
        }
        String rule = null;
        CombatAchievementRules.Spec spec = CombatAchievementRules.spec(wikiId);
        if (track == Track.UNTRACKED && spec != null) {
            track = spec.track();
            target = spec.target();
            count = spec.count();
            rule = spec.rule();
            ticks = (int) CombatAchievementRules.timeLimit(rule);
        }
        if (track == Track.UNTRACKED) {
            count = 1;
            ticks = 0;
            style = null;
        }
        return new Task("ca_" + wikiId, wikiId, name, description, tier, monster, type, track, target, count, ticks, style, rule);
    }

    private static final List<Task> TASKS = new ArrayList<>();
    private static final Map<String, Task> BY_ID = new LinkedHashMap<>();

    static {
        String[] blocks = {CombatAchievementData.EASY, CombatAchievementData.MEDIUM, CombatAchievementData.HARD,
                CombatAchievementData.ELITE, CombatAchievementData.MASTER, CombatAchievementData.GRANDMASTER};
        for (Tier tier : Tier.values()) {
            for (String line : blocks[tier.ordinal()].split("\n")) {
                if (line.isBlank()) {
                    continue;
                }
                String[] f = line.split("\\|", -1);
                Task t = classify(Integer.parseInt(f[0]), tier, f[1], Type.byLabel(f[2]), f[3], f[4]);
                TASKS.add(t);
                BY_ID.put(t.id(), t);
            }
        }
    }

    // ------------------------------------------------------------------ lookups

    public static List<Task> tasks() {
        return Collections.unmodifiableList(TASKS);
    }

    public static List<Task> tasks(Tier tier) {
        return TASKS.stream().filter(t -> t.tier() == tier).toList();
    }

    public static Task task(String id) {
        return BY_ID.get(id);
    }

    /** Tasks grouped by boss/monster page, in alphabetical order. */
    public static Map<String, List<Task>> byMonster() {
        Map<String, List<Task>> map = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Task t : TASKS) {
            map.computeIfAbsent(t.monster(), k -> new ArrayList<>()).add(t);
        }
        return map;
    }

    /** Every boss id and monster name the tracked tasks refer to (for consistency tests). */
    public static List<String> trackedKeys() {
        List<String> keys = new ArrayList<>();
        for (Task t : TASKS) {
            if (t.track() != Track.UNTRACKED) {
                Collections.addAll(keys, t.target().split("[|&]"));
            }
        }
        return keys;
    }

    // ------------------------------------------------------------------ points and tiers

    /** Points needed for a tier's rewards: the total of that tier's and all lower tiers' tasks (as in OSRS). */
    public static int threshold(Tier tier) {
        int sum = 0;
        for (Task t : TASKS) {
            if (t.tier().ordinal() <= tier.ordinal()) {
                sum += t.points();
            }
        }
        return sum;
    }

    public static int totalPoints() {
        return threshold(Tier.GRANDMASTER);
    }

    public static int points(Collection<String> done) {
        int sum = 0;
        for (String id : done) {
            Task t = BY_ID.get(id);
            if (t != null) {
                sum += t.points();
            }
        }
        return sum;
    }

    /** The highest tier whose rewards the points unlock, or null below the easy threshold. */
    public static Tier unlocked(int points) {
        Tier best = null;
        for (Tier t : Tier.values()) {
            if (points >= threshold(t)) {
                best = t;
            }
        }
        return best;
    }

    public static boolean unlocked(int points, Tier tier) {
        return points >= threshold(tier);
    }

    // ------------------------------------------------------------------ tracking helpers

    /** The fight key of a boss id: secondary forms and partner bosses map to the main boss. */
    public static String canonical(String bossId) {
        return CANONICAL.getOrDefault(bossId, bossId);
    }

    /** Fight key of a killed/attacked entity: a boss id, or "kill:" + lower-case name for regular monsters. */
    public static String fightKey(String bossId, String monsterName) {
        if (bossId != null) {
            return canonical(bossId);
        }
        return monsterName == null ? null : "kill:" + monsterName.toLowerCase(Locale.ROOT);
    }

    /** Value of a kill count expression ('|' sums, '&' takes the smallest). {@code kc} maps a key to its count. */
    public static int kcValue(String expr, java.util.function.ToIntFunction<String> kc) {
        if (expr.contains("&")) {
            int min = Integer.MAX_VALUE;
            for (String k : expr.split("&")) {
                min = Math.min(min, kc.applyAsInt(k));
            }
            return min;
        }
        int sum = 0;
        for (String k : expr.split("\\|")) {
            sum += kc.applyAsInt(k);
        }
        return sum;
    }

    /** Whether a KILLS target names a monster (exact, case-insensitive). */
    public static boolean killMatches(String target, String monsterName) {
        String name = monsterName.toLowerCase(Locale.ROOT);
        for (String alt : target.split("\\|")) {
            if (alt.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /** Whether an attack of this type is allowed by a STYLE task. */
    public static boolean styleAllowed(String style, AttackType type) {
        return switch (style) {
            case "melee" -> type.isMelee();
            case "magic" -> type == AttackType.MAGIC;
            case "ranged" -> type == AttackType.RANGED;
            default -> type.name().equalsIgnoreCase(style);
        };
    }

    /** Whether a weapon id (empty for an empty hand) matches a WEAPON/FINISH spec. */
    public static boolean weaponMatches(String spec, String weaponId) {
        for (String alt : spec.split("\\|")) {
            if (alt.equals("fists") ? weaponId.isEmpty() : alt.startsWith("*") ? weaponId.contains(alt.substring(1)) : alt.equals(weaponId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * What a single fight showed, for evaluating fight tasks: its length, whether the player took damage (at all /
     * without a protection prayer up), lost prayer points, and the attack styles and weapons of the hits they landed.
     */
    public record FightResult(long ticks, boolean damaged, boolean damagedUnprotected, boolean prayerLost, java.util.Set<AttackType> styles,
                              java.util.Set<String> weapons, String lastWeapon) {
    }

    /** Whether a finished fight completes a fight task. */
    public static boolean fightMeets(Task t, FightResult f) {
        return switch (t.track()) {
            case SPEED -> f.ticks() < t.ticks();
            case NO_DAMAGE -> !f.damaged();
            case PRAYED -> !f.damagedUnprotected();
            case NO_PRAYER_LOSS -> !f.prayerLost();
            case STYLE -> !f.styles().isEmpty() && f.styles().stream().allMatch(s -> styleAllowed(t.style(), s));
            case WEAPON -> !f.weapons().isEmpty() && f.weapons().stream().allMatch(w -> weaponMatches(t.style(), w));
            case FINISH -> f.lastWeapon() != null && weaponMatches(t.style(), f.lastWeapon());
            default -> false;
        };
    }

    // ------------------------------------------------------------------ rewards

    /** A hilt teleport and how many times a day it can be used at a hilt tier (0 = unlimited, -1 = not available). */
    public record HiltTele(Teleports.Destination dest, int[] perDayByHilt) {
        public int perDay(int hilt) {
            return hilt < 1 || hilt > 6 ? -1 : perDayByHilt[hilt - 1];
        }

        public String key() {
            return "ca_hilt_" + dest.name().toLowerCase(Locale.ROOT).replace(' ', '_');
        }
    }

    /** God Wars Dungeon entrance by the boulder (3 / 5 / unlimited a day) and Mor Ul Rek (from the elite hilt). */
    public static final List<HiltTele> HILT_TELEPORTS = List.of(
            new HiltTele(Teleports.at("God Wars Dungeon", 2917, 3745), new int[]{3, 5, 0, 0, 0, 0}),
            new HiltTele(Teleports.at("Mor Ul Rek", 2845, 3170), new int[]{-1, -1, -1, 3, 5, 0}));

    /** Tier of a Ghommal's hilt item id (1-6), or 0. */
    public static int hiltTier(String itemId) {
        if (itemId == null || !itemId.startsWith(HILT_PREFIX)) {
            return 0;
        }
        try {
            int n = Integer.parseInt(itemId.substring(HILT_PREFIX.length()));
            return n >= 1 && n <= 6 ? n : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Teleports a hilt offers. */
    public static List<HiltTele> hiltTeleports(int hilt) {
        return HILT_TELEPORTS.stream().filter(t -> t.perDay(hilt) >= 0).toList();
    }

    /** God Wars Dungeon follower kill count needed to enter a boss room: 40, then 35/30/25/15 from hard/elite/master/GM. */
    public static int gwdKillcount(Tier unlocked) {
        if (unlocked == null || unlocked.ordinal() < Tier.HARD.ordinal()) {
            return 40;
        }
        return switch (unlocked) {
            case HARD -> 35;
            case ELITE -> 30;
            case MASTER -> 25;
            default -> 15;
        };
    }

    /** Extra Void Knight commendation points per Pest Control game: +1 easy, +2 medium, +3 hard and up. */
    public static int pestControlBonus(Tier unlocked) {
        return unlocked == null ? 0 : Math.min(3, unlocked.ordinal() + 1);
    }

    /** Warrior guild token multiplier: 100% bonus from easy, 200% from medium. */
    public static int warriorTokenMultiplier(Tier unlocked) {
        return unlocked == null ? 1 : unlocked == Tier.EASY ? 2 : 3;
    }

    /** Extra kills the "Like a boss" unlock allows on boss tasks: +5 per tier claimed. */
    public static int likeABossExtra(Tier unlocked) {
        return unlocked == null ? 0 : 5 * (unlocked.ordinal() + 1);
    }

    /** Most kills a boss slayer task can have: 35, plus the "Like a boss" extension of the unlocked tier. */
    public static int bossTaskMax(int base, Tier unlocked) {
        return base + likeABossExtra(unlocked);
    }

    /** Superior slayer monsters: 1/200 becomes 1/150 from the elite tier. */
    public static int superiorChance(int base, Tier unlocked) {
        return unlocked != null && unlocked.ordinal() >= Tier.ELITE.ordinal() ? base * 3 / 4 : base;
    }

    /**
     * Ecumenical key drop chance (1 in n) from Wilderness God Wars Dungeon monsters: 1/60, then 1/55, 1/50, 1/45 and 1/40
     * from the hard, elite, master and grandmaster tiers.
     */
    public static int ecumenicalKeyChance(Tier unlocked) {
        if (unlocked == null || unlocked.ordinal() < Tier.HARD.ordinal()) {
            return 60;
        }
        return 55 - 5 * (unlocked.ordinal() - Tier.HARD.ordinal());
    }

    /** Nightmare Zone and Soul Wars imbues cost half from the hard tier. */
    public static int imbueCost(int cost, Tier unlocked) {
        return unlocked != null && unlocked.ordinal() >= Tier.HARD.ordinal() ? cost / 2 : cost;
    }

    /**
     * From the medium tier, Barrows no longer drains Prayer while a Ghommal's hilt (or Ghommal's avernic defender) is
     * equipped.
     */
    public static boolean barrowsDrainNullified(Tier unlocked, String offHandId) {
        return unlocked != null && unlocked.ordinal() >= Tier.MEDIUM.ordinal() && offHandId != null
                && (hiltTier(offHandId) > 0 || offHandId.startsWith("ghommals_avernic_defender_"));
    }

    /** Dragon hunter crossbow ornaments: (b) with King Black Dragon heads from the hard tier, (t) with Vorkath's head from elite. */
    public static final String DHCB_KBD = "dragon_hunter_crossbow_b";
    public static final String DHCB_VORKATH = "dragon_hunter_crossbow_t";

    /** The crossbow ornament a head makes and the tier it needs, or null. */
    public static Tier dhcbOrnamentTier(String ornament) {
        return DHCB_KBD.equals(ornament) ? Tier.HARD : DHCB_VORKATH.equals(ornament) ? Tier.ELITE : null;
    }

    /** The head an ornamented crossbow uses (given back when it is taken apart). */
    public static String dhcbHead(String ornament) {
        return DHCB_KBD.equals(ornament) ? "kbd_heads" : DHCB_VORKATH.equals(ornament) ? "vorkaths_head" : null;
    }

    /** TzTok-Jad / TzKal-Zuk slayer task size: 1, 2 from master, 3 from grandmaster. */
    public static int tzhaarTaskKills(Tier unlocked) {
        return unlocked == Tier.GRANDMASTER ? 3 : unlocked == Tier.MASTER ? 2 : 1;
    }

    /** Thralls last 100% longer from the master tier. */
    public static double thrallDurationMultiplier(Tier unlocked) {
        return unlocked != null && unlocked.ordinal() >= Tier.MASTER.ordinal() ? 2.0 : 1.0;
    }

    /** 5% increased clue drop chance for easy..elite clues once the matching tier is unlocked. */
    public static double clueChanceMultiplier(Tier unlocked, ClueScrolls.Tier clue) {
        if (unlocked == null) {
            return 1.0;
        }
        int needed = switch (clue) {
            case EASY -> Tier.EASY.ordinal();
            case MEDIUM -> Tier.MEDIUM.ordinal();
            case HARD -> Tier.HARD.ordinal();
            case ELITE -> Tier.ELITE.ordinal();
            default -> Integer.MAX_VALUE;
        };
        return unlocked.ordinal() >= needed ? 1.05 : 1.0;
    }

    /** The rewards of a tier, as shown in the Combat Achievements screen (OSRS Wiki). */
    public static List<String> rewards(Tier tier) {
        return switch (tier) {
            case EASY -> List.of("Antique lamp: 5,000 XP in a skill of level 20+", "Ghommal's hilt 1: 3 daily God Wars Dungeon teleports",
                    "5% higher easy clue scroll drop chance", "Like a boss: up to 40 boss kills per task",
                    "+1 Void Knight commendation point per Pest Control game", "100% bonus Warrior guild tokens");
            case MEDIUM -> List.of("Antique lamp: 10,000 XP in a skill of level 30+", "Ghommal's hilt 2: 5 daily God Wars Dungeon teleports",
                    "5% higher medium clue scroll drop chance", "Like a boss: up to 45 boss kills per task",
                    "+2 Void Knight commendation points per Pest Control game", "200% bonus Warrior guild tokens",
                    "No Barrows Prayer drain while a Ghommal's hilt is equipped", "The dwarf multicannon holds 35 cannonballs");
            case HARD -> List.of("Antique lamp: 15,000 XP in a skill of level 40+", "Ghommal's hilt 3: unlimited God Wars Dungeon teleports",
                    "5% higher hard clue scroll drop chance", "Like a boss: up to 50 boss kills per task",
                    "God Wars Dungeon boss rooms need 35 kill count", "+3 Void Knight commendation points per Pest Control game",
                    "Nightmare Zone and Soul Wars imbues cost half",
                    "Ghommal's hilt 3: attach King Black Dragon heads to a dragon hunter crossbow (b)",
                    "Ecumenical keys drop 1/55 instead of 1/60", "The dwarf multicannon holds 45 cannonballs");
            case ELITE -> List.of("Antique lamp: 25,000 XP in a skill of level 50+", "Ghommal's hilt 4: 3 daily teleports to Mor Ul Rek",
                    "5% higher elite clue scroll drop chance", "Like a boss: up to 55 boss kills per task",
                    "God Wars Dungeon boss rooms need 30 kill count",
                    "Superior slayer monsters appear 1/150 instead of 1/200",
                    "Ghommal's hilt 4: attach Vorkath's head to a dragon hunter crossbow (t)",
                    "Ecumenical keys drop 1/50", "The dwarf multicannon holds 60 cannonballs");
            case MASTER -> List.of("Antique lamp: 35,000 XP in a skill of level 60+", "Ghommal's hilt 5: 5 daily teleports to Mor Ul Rek",
                    "Ghommal's lucky penny: 5% chance not to use a charge of a charged item",
                    "Combine the hilt with an avernic defender into Ghommal's avernic defender 5",
                    "Like a boss: up to 60 boss kills per task", "TzTok-Jad and TzKal-Zuk tasks are 2 kills",
                    "God Wars Dungeon boss rooms need 25 kill count", "Thralls last twice as long", "Ecumenical keys drop 1/45");
            case GRANDMASTER -> List.of("Antique lamp: 50,000 XP in a skill of level 70+", "Ghommal's hilt 6: unlimited teleports to Mor Ul Rek",
                    "Combine the hilt with an avernic defender into Ghommal's avernic defender 6",
                    "Like a boss: up to 65 boss kills per task", "TzTok-Jad and TzKal-Zuk tasks are 3 kills",
                    "God Wars Dungeon boss rooms need 15 kill count", "Ecumenical keys drop 1/40");
        };
    }

    /** Task counts per tier and track (for the screen and tests). */
    public static Map<Tier, int[]> trackedCounts() {
        Map<Tier, int[]> m = new EnumMap<>(Tier.class);
        for (Tier t : Tier.values()) {
            m.put(t, new int[2]);
        }
        for (Task t : TASKS) {
            m.get(t.tier())[t.track() == Track.UNTRACKED ? 1 : 0]++;
        }
        return m;
    }

    // ------------------------------------------------------------------ items

    /** Ghommal's hilts, the lucky penny, Ghommal's avernic defenders and the tier lamps. */
    static void items(ItemSink items) {
        String[] looks = {"basic", "standard", "sturdy", "refined", "superior", "perfected"};
        int[] colors = {0x8A6A4A, 0x9A9AA0, 0x5A8AC0, 0xC03030, 0xA040C0, 0xE8C030};
        for (Tier t : Tier.values()) {
            int n = t.ordinal() + 1;
            String teles = hiltTeleports(n).stream().map(h -> h.perDay(n) == 0 ? "unlimited " + h.dest().name() + " teleports"
                    : h.perDay(n) + " daily " + h.dest().name() + " teleports").reduce((a, b) -> a + " and " + b).orElse("");
            Defs.armour(items, t.hilt(), "Ghommal's hilt " + n, "hilt", colors[t.ordinal()], "steel", 0, EquipmentDef.Slot.SHIELD,
                    EquipmentStats.NONE, Map.of()).examine("A " + looks[t.ordinal()] + " artefact. Sneak + right-click for " + teles + ".");
            Defs.simple(items, t.lamp(), "Antique lamp (" + t.id() + " combat achievements)", "lamp", 0xE8C030, 1).stack(1)
                    .examine("Rub it to gain " + String.format(Locale.ROOT, "%,d", t.lampXp) + " XP in a skill of level " + t.lampMinLevel
                            + " or higher.");
        }
        Defs.worn(items, LUCKY_PENNY, "Ghommal's lucky penny", "coins", 0xC08040, 1, Jewellery.Slot.AMMO, EquipmentStats.NONE, Map.of(),
                Jewellery.Perk.NONE, List.of()).examine("I'm sure he won't mind me borrowing this... (5% chance to save a charge)");
        // Dragon hunter crossbow (b) / (t): cosmetic, same stats as the crossbow (OSRS: made at the hard / elite tier).
        for (String[] v : new String[][]{{DHCB_KBD, "Dragon hunter crossbow (b)", "kbd_heads", "the King Black Dragon"},
                {DHCB_VORKATH, "Dragon hunter crossbow (t)", "vorkaths_head", "Vorkath"}}) {
            items.add(v[0], v[1], com.runecraft.osrs.ItemDef.Kind.BOW, "crossbow", v[0].equals(DHCB_KBD) ? 0x2A2A2A : 0x6A7A6A)
                    .value(70_000_000).stack(1).tier("dragon")
                    .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON, new EquipmentStats(0, 0, 0, 0, 95, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                            Map.of(Skill.RANGED, 65), 6, AttackType.RANGED, EquipmentDef.WeaponClass.BOW))
                    .examine("A dragon hunter crossbow adorned with the head of " + v[3] + ".");
        }
        for (int n = 5; n <= 6; n++) {
            Defs.armour(items, "ghommals_avernic_defender_" + n, "Ghommal's avernic defender " + n, "defender", n == 5 ? 0xA040C0 : 0xE8C030,
                    "dragon", 0, EquipmentDef.Slot.SHIELD, new EquipmentStats(30, 29, 28, -5, -4, 30, 29, 28, -5, -4, 8, 0, 0, 0),
                    Map.of(Skill.ATTACK, 70, Skill.DEFENCE, 70)).examine("An avernic defender fitted with Ghommal's hilt " + n + ".");
        }
    }
}
