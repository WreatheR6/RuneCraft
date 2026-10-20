package com.runecraft.osrs.content.bosses;

import java.util.List;
import java.util.Locale;

/**
 * Numbers and rules of the boss mechanics scripted on the Minecraft side (Yama and his contracts, the Abyssal Sire's
 * first phase, Vardorvis' ramp, Amoxliatl, the Maggot King, the Royal Titans and Phosani's sleepwalkers), plus the
 * Combat Achievement flags and counters those scripts report. Values are the OSRS Wiki's (boss and strategy pages);
 * anything not documented is marked approx.
 */
public final class BossMechanics {
    private BossMechanics() {
    }

    // ------------------------------------------------------------ Combat Achievement flags and counters
    /** Yama: the Judge was attacked with a style it was praying against ("Back so soon?"). */
    public static final String FLAG_YAMA_JUDGE_WRONG = "yama_judge_wrong";
    /** Yama: more than one instance of damage taken during one exile ("Back so soon?"). */
    public static final String FLAG_YAMA_EXILE_HITS = "yama_exile_hits";
    /** Yama: more than one tile from the centre of a Shadow Crash when it landed ("Shadow dancer"). */
    public static final String FLAG_YAMA_CRASH_FAR = "yama_crash_far";
    /** Yama: a void flare was killed by something other than a special attack ("Fire fighter"). */
    public static final String FLAG_YAMA_FLARE_NONSPEC = "yama_flare_nonspec";
    /** Yama: the fight is under one of the non-standard (sigil) contracts ("Contractually Unbound"). */
    public static final String FLAG_YAMA_CONTRACT = "yama_contract";
    /** Abyssal Sire: how often it was stunned (disoriented) in phase 1 ("Respiratory Runner"). */
    public static final String COUNT_SIRE_STUN = "sire_stun";
    /** Vardorvis: game ticks survived while he is enraged (max speed) ("Axe Enthusiast"). */
    public static final String COUNT_VARDORVIS_ENRAGE = "vardorvis_enrage_ticks";
    /** Vardorvis: the player left the centre 25 tiles of the arena ("Axe Enthusiast"). */
    public static final String FLAG_VARDORVIS_LEFT_CENTRE = "vardorvis_left_centre";
    /** Amoxliatl: a block of unstable ice shattered ("Totally Shattered"). */
    public static final String FLAG_AMOX_ICE_SHATTERED = "amox_ice_shattered";
    /** Maggot King: a melee slam passed without a melee punish ("King-sized clobbering"). */
    public static final String FLAG_MAGGOT_PUNISH_MISSED = "maggot_punish_missed";
    /** Maggot King: it was healed (larvae eating carrion or chipping at a player) ("Perfect Maggot King"). */
    public static final String FLAG_MAGGOT_HEALED = "maggot_healed";
    /** Royal Titans: a fire or icicle wall dissipated by itself ("I need room"). */
    public static final String FLAG_TITANS_WALL_DISSIPATED = "titans_wall_dissipated";
    /** Royal Titans: elementals killed by the Titans' own walls and blasts ("Let them fight"). */
    public static final String COUNT_TITAN_ELEMENTAL_KILLS = "titan_elemental_kills";
    /** Royal Titans: both Titans fell during the same Elemental Blast charge ("It takes too long"). */
    public static final String FLAG_TITANS_CHARGE_KILL = "titans_charge_kill";
    /** Phosani's Nightmare: a sleepwalker reached her during the desperation phase ("Dreamland Express"). */
    public static final String FLAG_PHOSANI_SLEEPWALKER = "phosani_sleepwalker_reached";
    /** Bryophyta: all three growthlings of a batch died within 3 seconds of the first ("Quick Cutter"). */
    public static final String FLAG_BRYOPHYTA_QUICK_CUT = "bryophyta_quick_cut";
    /** Obor: his knockback pushed the player back more than one square ("Back to the Wall"). */
    public static final String FLAG_OBOR_PUSHED = "obor_pushed";

    /** Names the scripts give their damage (beyond the bosses' AttackDef names), for "no damage from ..." rules. */
    public static final java.util.Set<String> SCRIPTED_ATTACKS = java.util.Set.of(
            "shadow_engulf", "shadow_stomp", "shadow_wave", "shadow_crash", "glyph_flames", "fire_streak", "fire_wave", "void_flare",
            "glyph_of_fire", "cavern_shake", "forfeit", "tentacle", "swinging_axe", "darting_spikes", "ice_spikes", "screech", "carrion",
            "slam", "larva", "fire_wall", "icicle_wall", "elemental_blast", "elemental_explosion", "sleepwalker", "power_blast",
            "bleed", "strangle", "burn", "knockback", "parasite_burst", "spike", "weakened_parasite_burst", "tail_shockwave", "spore", "explosion",
            "ruptura_explosion", "rock_explosion", "zalcano_shockwave", "pebbles", "under_zalcano",
            // Wilderness bosses (godwars module scripts): the Chaos Fanatic's and crazy archaeologist's specials, the trio's traps
            "green_explosion", "explosive_book", "bear_trap", "sticky_web");

    // ------------------------------------------------------------ minion ids
    public static final String YAMA_JUDGE = "yama_judge";
    public static final String YAMA_VOID_FLARE = "yama_void_flare";
    public static final String UNSTABLE_ICE = "unstable_ice";
    public static final String UR_MAGGOT_LARVA = "ur_maggot_larva";
    public static final String PHOSANI_SLEEPWALKER = "phosani_sleepwalker";
    public static final String GROWTHLING = "growthling";
    public static final String STRANGLER_SPORE = "strangler_spore";

    // ------------------------------------------------------------ Bryophyta (OSRS Wiki "Growthling")
    /** Three growthlings at a time; a 1/5 roll each time she attacks; 20 ticks before a new batch after the last dies. */
    public static final int GROWTHLINGS = 3;
    public static final int GROWTHLING_CHANCE = 5;
    public static final int GROWTHLING_COOLDOWN = 20;
    /** "Quick Cutter": the other two within 3 seconds (5 ticks) of the first one dying. */
    public static final int QUICK_CUTTER_TICKS = 5;

    /**
     * Whether a weapon finishes growthlings in one hit: anything with the Axe attack type (woodcutting axes; not the blessed
     * axe, battleaxes or the zombie axe) or magic secateurs. Other weapons can't bring them below 1 hitpoint.
     */
    public static boolean cutsGrowthling(String heldId, boolean woodcuttingAxe) {
        if (heldId == null) {
            return false;
        }
        return heldId.equals("magic_secateurs") || (woodcuttingAxe && !heldId.equals("blessed_axe"));
    }

    /** Whether a batch was cut down in time: its last growthling died within 3 seconds of its first. */
    public static boolean quickCut(long firstDeathTick, long lastDeathTick, int killed) {
        return killed >= GROWTHLINGS && lastDeathTick - firstDeathTick <= QUICK_CUTTER_TICKS;
    }

    // ------------------------------------------------------------ Obor (OSRS Wiki)
    /** Melee 22, ranged 26; Protect from Missiles halves the ranged attack instead of blocking it. */
    public static final int OBOR_MELEE_MAX = 22;
    public static final int OBOR_RANGED_MAX = 26;
    /** Knockback slam: one melee attack in 4 (approx) pushes the target up to 2 tiles (approx) straight away from him. */
    public static final int OBOR_KNOCKBACK_CHANCE = 4;
    public static final int OBOR_KNOCKBACK_TILES = 2;

    /** Damage of Obor's ranged attack through Protect from Missiles (50% less). */
    public static int oborRangedThroughPrayer(int damage) {
        return damage / 2;
    }

    /** "Back to the Wall" fails when a knockback moved the player more than one square. */
    public static boolean oborPushedTooFar(int tilesMoved) {
        return tilesMoved > 1;
    }

    // ------------------------------------------------------------ Yama
    /** Yama's hitpoints and attack speed (8 ticks; 7 in phase 3 and in every contract). */
    public static final int YAMA_HITPOINTS = 2500;
    public static final int YAMA_SPEED = 8;
    public static final int YAMA_SPEED_FAST = 7;
    /** Auto-attack max hit 46 (53 under a contract); melee through Protect from Melee 6 (22 in phase 3). */
    public static final int YAMA_AUTO_MAX = 46;
    public static final int YAMA_CONTRACT_AUTO_MAX = 53;
    public static final int YAMA_MELEE_THROUGH_PRAYER = 6;
    public static final int YAMA_MELEE_THROUGH_PRAYER_P3 = 22;
    /** Phase 3 ranged/magic autos pierce prayer for up to 3. */
    public static final int YAMA_PIERCE_P3 = 3;
    /** Phase 1 glyph specials without glyph protection: up to 60. Phase 2: Shadow Stomp up to 60 (2-tick root). */
    public static final int YAMA_GLYPH_SPECIAL_MAX = 60;
    public static final int YAMA_SHADOW_STOMP_MAX = 60;
    /** Shadow waves: up to 20 and protection prayers switched off. Shadow Crash: up to 20 per explosion. */
    public static final int YAMA_SHADOW_WAVE_MAX = 20;
    public static final int YAMA_SHADOW_CRASH_MAX = 20;
    /** Fire streak damage (approx: the wiki gives none, only the bind). */
    public static final int YAMA_FIRE_STREAK_MAX = 15;
    /** Void flares: 140 hitpoints (71 in phase 3), explode after ~15 seconds for up to 44 (25 in phase 3), healing Yama. */
    public static final int VOID_FLARE_TICKS = 25;
    public static final int VOID_FLARE_MAX = 44;
    public static final int VOID_FLARE_MAX_P3 = 25;
    /** The Judge of Yama: 400 hitpoints, fire spells every 5 ticks (up to 30), the cavern shakes on its 5th and 13th attack. */
    public static final int JUDGE_HITPOINTS = 400;
    public static final int JUDGE_ATTACK_TICKS = 5;
    public static final int JUDGE_FIRE_MAX = 30;

    /** Meteor Strike damage by distance (tiles) from the meteor's centre: lethal within 1, 80 at 2, 40 at 3, none beyond. */
    public static int meteorDamage(int tilesFromCentre) {
        return tilesFromCentre <= 1 ? 121 : tilesFromCentre == 2 ? 80 : tilesFromCentre == 3 ? 40 : 0;
    }

    /** Stepping on a Glyph of Fire costs 2, 6, 9 or 12 by how close Yama is (4+, 3, 2, 1 tiles); shadow drains as much prayer. */
    public static int glyphCost(int tilesFromYama) {
        return tilesFromYama >= 4 ? 2 : tilesFromYama == 3 ? 6 : tilesFromYama == 2 ? 9 : 12;
    }

    /** A void flare's explosion (and Yama's heal) scales with the flare's remaining hitpoints. */
    public static int flareExplosion(double hpFraction, boolean phase3) {
        double f = Math.max(0, Math.min(1, hpFraction));
        return (int) Math.round((phase3 ? VOID_FLARE_MAX_P3 : VOID_FLARE_MAX) * f);
    }

    /**
     * Yama's contracts (OSRS Wiki "Yama#Contracts"). Each is consumed when Yama is challenged while holding it and raises
     * his stats (max hit 53, 7-tick attacks). The five sigil contracts are returned on a kill with their purifying sigil
     * piece; the acquisition contracts (from dossiers) are not, and guarantee their reward instead. approx: the acquisition
     * contracts share the sigil contracts' attack cycle; their halved hitpoint/prayer restoration and enhanced incantations
     * aren't modelled.
     */
    public enum YamaContract {
        /** Drains run energy passively and on hits; reaching 0 loses the fight. */
        FORFEIT_BREATH("contract_of_forfeit_breath", 2500, 2, false, false, false, false),
        /** Glyphs protect for less time and cost more; Yama is weak to crush only. */
        GLYPHIC_ATTENUATION("contract_of_glyphic_attenuation", 2875, 2, false, false, false, false),
        /** More prayer penetration, lethal void flares (the halved attack range isn't modelled). */
        SENSORY_CLOUDING("contract_of_sensory_clouding", 2500, 2, true, false, false, true),
        /** Protection prayers switched off on sight, no prayer penetration, void flares continuously, lethal flares. */
        DIVINE_SEVERANCE("contract_of_divine_severance", 2500, 2, true, true, true, false),
        /** More and tougher void flares; both Yama and the challenger gain Wretched Strength. */
        BLOODIED_BLOWS("contract_of_bloodied_blows", 3000, 3, false, false, false, false),
        // Acquisition contracts (appended; dossier rewards): four void flares at a time. Hitpoints approx (base 2,500).
        /** 2,000 aether catalysts. */
        CATALYST_ACQUISITION("contract_of_catalyst_acquisition", 2500, 4, false, false, false, false),
        /** 250 diabolic worms. */
        WORM_ACQUISITION("contract_of_worm_acquisition", 2500, 4, false, false, false, false),
        /** 45 oathplate shards. */
        SHARD_ACQUISITION("contract_of_shard_acquisition", 2500, 4, false, false, false, false),
        /** One oathplate armour piece of the challenger's choice. */
        OATHPLATE_ACQUISITION("contract_of_oathplate_acquisition", 2500, 4, false, false, false, false),
        /** A soulflame horn. */
        HARMONY_ACQUISITION("contract_of_harmony_acquisition", 2500, 4, false, false, false, false);

        public final String item;
        public final int hitpoints;
        /** Void flares per flare special. */
        public final int flares;
        public final boolean lethalFlares;
        public final boolean seversPrayers;
        public final boolean continuousFlares;
        public final boolean extraPenetration;

        YamaContract(String item, int hitpoints, int flares, boolean lethalFlares, boolean seversPrayers, boolean continuousFlares,
                     boolean extraPenetration) {
            this.item = item;
            this.hitpoints = hitpoints;
            this.flares = flares;
            this.lethalFlares = lethalFlares;
            this.seversPrayers = seversPrayers;
            this.continuousFlares = continuousFlares;
            this.extraPenetration = extraPenetration;
        }

        public static YamaContract byItem(String item) {
            for (YamaContract c : values()) {
                if (c.item.equals(item)) {
                    return c;
                }
            }
            return null;
        }

        public String displayName() {
            return "Contract of " + name().toLowerCase(Locale.ROOT).replace('_', ' ');
        }

        /** The five sigil contracts come back after a kill; acquisition contracts are used up (wiki "Returned on kill"). */
        public boolean returnedOnKill() {
            return ordinal() <= BLOODIED_BLOWS.ordinal();
        }

        /** The guaranteed reward of a kill under this contract (oathplate acquisition: a piece the player picks). */
        public String reward() {
            return switch (this) {
                case FORFEIT_BREATH -> "purifying_sigil_top";
                case GLYPHIC_ATTENUATION -> "purifying_sigil_bottom";
                case SENSORY_CLOUDING -> "purifying_sigil_left";
                case DIVINE_SEVERANCE -> "purifying_sigil_right";
                case BLOODIED_BLOWS -> "purifying_sigil_middle";
                case CATALYST_ACQUISITION -> "aether_catalyst";
                case WORM_ACQUISITION -> "diabolic_worms";
                case SHARD_ACQUISITION -> "oathplate_shards";
                case OATHPLATE_ACQUISITION -> "oathplate_helm";
                case HARMONY_ACQUISITION -> "soulflame_horn";
            };
        }

        public int rewardQuantity() {
            return switch (this) {
                case CATALYST_ACQUISITION -> 2000;
                case WORM_ACQUISITION -> 250;
                case SHARD_ACQUISITION -> 45;
                default -> 1;
            };
        }
    }

    /** The five purifying sigil pieces, combined into the purifying sigil (wiki). */
    public static final List<String> SIGIL_PIECES = List.of("purifying_sigil_top", "purifying_sigil_bottom", "purifying_sigil_left",
            "purifying_sigil_right", "purifying_sigil_middle");
    /** Aether runes the sigil draws to turn one oathplate piece radiant (wiki). */
    public static final int RADIANT_AETHER_RUNES = 2500;

    /**
     * A dossier's contents (OSRS Wiki "Dossier", weights out of 1,740): {item, min, max, weight}. approx: the rite of vile
     * transference (116) and the contract of familiar acquisition (65) don't exist here and give chasm teleport scrolls.
     */
    public static final List<Object[]> DOSSIER_TABLE = List.of(
            new Object[]{"contract_of_bloodied_blows", 3, 3, 260}, new Object[]{"contract_of_divine_severance", 3, 3, 260},
            new Object[]{"contract_of_forfeit_breath", 3, 3, 260}, new Object[]{"contract_of_glyphic_attenuation", 3, 3, 260},
            new Object[]{"contract_of_sensory_clouding", 3, 3, 260}, new Object[]{"chasm_teleport_scroll", 15, 18, 116 + 116 + 65},
            new Object[]{"contract_of_shard_acquisition", 1, 1, 39}, new Object[]{"contract_of_worm_acquisition", 1, 1, 39},
            new Object[]{"contract_of_catalyst_acquisition", 1, 1, 39}, new Object[]{"contract_of_harmony_acquisition", 1, 1, 13},
            new Object[]{"contract_of_oathplate_acquisition", 1, 1, 13});
    public static final int DOSSIER_TOTAL = 1740;

    /** The dossier entry for a roll in [0, 1740). */
    public static Object[] dossierRoll(int roll) {
        int r = Math.floorMod(roll, DOSSIER_TOTAL);
        for (Object[] e : DOSSIER_TABLE) {
            r -= (Integer) e[3];
            if (r < 0) {
                return e;
            }
        }
        return DOSSIER_TABLE.get(DOSSIER_TABLE.size() - 1);
    }

    /** Max-hit multiplier of Yama's attacks under a contract (53 instead of 46). */
    public static double yamaHitScale(YamaContract c) {
        return c == null ? 1.0 : YAMA_CONTRACT_AUTO_MAX / (double) YAMA_AUTO_MAX;
    }

    /**
     * Yama's defence bonus against a style (OSRS Wiki infobox): magic -30 while his primary target melees him and +60 while
     * it uses magic; phase 3 raises everything (stab 135, slash 108, crush and light/heavy ranged 449, standard ranged 297,
     * magic 81). Glyphic attenuation: 666 against everything but crush (80, phase 3 108). Ranged is the standard bonus.
     */
    public static int yamaDefenceBonus(com.runecraft.osrs.AttackType type, boolean targetMaging, boolean phase3, YamaContract c) {
        if (c == YamaContract.GLYPHIC_ATTENUATION) {
            return type == com.runecraft.osrs.AttackType.CRUSH ? (phase3 ? 108 : 80) : 666;
        }
        return switch (type) {
            case STAB -> phase3 ? 135 : 100;
            case SLASH -> phase3 ? 108 : 80;
            case CRUSH -> phase3 ? 449 : 333;
            case MAGIC -> phase3 ? 81 : targetMaging ? 60 : -30;
            case RANGED -> phase3 ? 297 : 220;
        };
    }

    /** {light, standard, heavy} ranged defence: 333/220/333, phase 3 449/297/449 (glyphic attenuation 666). */
    public static int[] yamaRangedDefences(boolean phase3, YamaContract c) {
        if (c == YamaContract.GLYPHIC_ATTENUATION) {
            return new int[]{666, 666, 666};
        }
        return phase3 ? new int[]{449, 297, 449} : new int[]{333, 220, 333};
    }

    /** Burns: an unprotected Meteor Strike burns for 8; an unprotected fire glyph special for 10 (approx: a full burn). */
    public static final int YAMA_METEOR_BURN = 8;
    public static final int YAMA_GLYPH_BURN = 10;

    /** Wretched Strength under the Contract of bloodied blows (approx: +10% damage both ways). */
    public static final double WRETCHED_STRENGTH = 1.10;

    // ------------------------------------------------------------ Abyssal Sire
    /** 75 ranged/magic damage disorients the Sire; so may a shadow spell. Disoriented for 30 s, tentacles wake after 27 s. */
    public static final int SIRE_DISORIENT_DAMAGE = 75;
    public static final int SIRE_DISORIENT_TICKS = 50;
    public static final int SIRE_TENTACLE_WAKE_TICKS = 45;
    /** The six tentacles hit nearby players for up to 30 (Protect from Melee reduces it slightly; approx 25%). */
    public static final int SIRE_TENTACLE_MAX = 30;
    /** Attacks on protected respiratory systems deal 0-3. */
    public static final int SIRE_PROTECTED_MAX = 3;

    /** Chance (percent) that a shadow spell disorients the Sire: Rush 25, Burst 50, Blitz 75, Barrage 100. */
    public static int sireStunChance(String spell) {
        if (spell == null) {
            return 0;
        }
        return switch (spell.toLowerCase(Locale.ROOT)) {
            case "shadow_rush" -> 25;
            case "shadow_burst" -> 50;
            case "shadow_blitz" -> 75;
            case "shadow_barrage" -> 100;
            default -> 0;
        };
    }

    // ------------------------------------------------------------ Vardorvis
    public static final int VARDORVIS_HITPOINTS = 700;
    /** Below 570 hitpoints his head gazes and he strangles; below 33% (231) he enrages. */
    public static final int VARDORVIS_HEAD_HP = 570;
    public static final int VARDORVIS_ENRAGE_HP = 231;

    /** Defence lost as he takes damage: round(0.1021 x damage dealt - 0.2075) (OSRS Wiki). */
    public static int vardorvisDefenceReduction(int damageDealt) {
        return Math.max(0, (int) Math.round(0.1021 * damageDealt - 0.2075));
    }

    /** His melee max hit grows linearly with damage taken: 32 to 43 (awakened 46 to 61). */
    public static int vardorvisMaxHit(double hpFraction, boolean awakened) {
        double lost = 1 - Math.max(0, Math.min(1, hpFraction));
        int lo = awakened ? 46 : 32;
        int hi = awakened ? 61 : 43;
        return (int) Math.round(lo + (hi - lo) * lost);
    }

    /** Axes per swing: 1 above 690 hitpoints, 2 down to 231, 3 below (awakened one more; OSRS Wiki). */
    public static int vardorvisAxes(int hp, boolean awakened) {
        int n = hp > 690 ? 1 : hp >= VARDORVIS_ENRAGE_HP ? 2 : 3;
        return awakened ? n + 1 : n;
    }

    /** Swinging axes hit for up to 35 (awakened 96; Protect from Melee halves it) and start a bleed. */
    public static final int VARDORVIS_AXE_MAX = 35;
    public static final int VARDORVIS_AXE_MAX_AWAKENED = 96;
    /** Bleed: 3 damage every 3 ticks for 14 ticks (5 hits), or every tick while the player runs. */
    public static final int VARDORVIS_BLEED_DAMAGE = 3;
    /**
     * Awakened Vardorvis (OSRS Wiki "Vardorvis/Strategies", Awakened mode): bleed damage 3 -> 5; each axe also hits on
     * appearing (that hit doesn't bleed). The appearing hit's max isn't documented: the swing's max is used (approx).
     */
    public static final int VARDORVIS_BLEED_DAMAGE_AWAKENED = 5;
    public static final int VARDORVIS_BLEED_TICKS = 14;
    public static final int VARDORVIS_BLEED_INTERVAL = 3;
    /** Strangle (below 570): destroy the spores within 5 ticks (awakened 4, approx) or take ~25 (awakened ~40). */
    public static final int VARDORVIS_STRANGLE_TICKS = 5;
    public static final int VARDORVIS_STRANGLE_TICKS_AWAKENED = 4;
    public static final int VARDORVIS_STRANGLE_DAMAGE = 25;
    public static final int VARDORVIS_STRANGLE_DAMAGE_AWAKENED = 40;
    /**
     * Spores to destroy per Strangle (OSRS Wiki "Vardorvis/Strategies"): at least four, more the more often he has used
     * it, up to six. approx: one more per earlier Strangle of the fight.
     */
    public static int vardorvisStrangleSpores(int earlierStrangles) {
        return Math.min(6, 4 + Math.max(0, earlierStrangles));
    }

    /**
     * Vardorvis' head (OSRS Wiki "Vardorvis' Head"): below 570 hitpoints it gazes at the player with a projectile that
     * deals no damage; unless Protect from Missiles (the green gaze) or Protect from Magic (the awakened blue gaze) blocks
     * it, it drains 10 Prayer points, switches overhead prayers off and keeps them off for 3 ticks. He doesn't melee while
     * it flies and attacks 2 ticks after it lands. Awakened below 50% the head gazes in pairs, one of each, 1 tick apart.
     */
    public static final int VARDORVIS_HEAD_PRAYER_DRAIN = 10;
    public static final int VARDORVIS_HEAD_LOCK_TICKS = 3;
    /** Game ticks the gaze is in the air (approx: not documented). */
    public static final int VARDORVIS_HEAD_FLIGHT_TICKS = 3;
    public static final int VARDORVIS_HEAD_AFTER_TICKS = 2;

    /**
     * The gazes of one head appearance, as "magic" flags in firing order: post-quest a single green (ranged) gaze;
     * awakened a random one, or below half his hitpoints a pair of both styles in random order.
     */
    public static boolean[] vardorvisGazes(boolean awakened, double hpFraction, boolean coin) {
        if (!awakened) {
            return new boolean[]{false};
        }
        return hpFraction < 0.5 ? new boolean[]{coin, !coin} : new boolean[]{coin};
    }

    /** A Vardorvis bleed: damage per game tick, more often while the player is running. */
    public static final class Bleed {
        private int ticksLeft;
        private int sinceHit;
        private int damage = VARDORVIS_BLEED_DAMAGE;

        public void start() {
            start(VARDORVIS_BLEED_DAMAGE);
        }

        /** Starts (or restarts) the bleed with {@code perHit} damage per bleed hit (5 for awakened Vardorvis). */
        public void start(int perHit) {
            ticksLeft = VARDORVIS_BLEED_TICKS;
            sinceHit = 1; // the first hit lands 2 ticks in: 5 hits over 14 ticks standing still
            damage = perHit;
        }

        public boolean active() {
            return ticksLeft > 0;
        }

        public int tick(boolean moving) {
            if (ticksLeft <= 0) {
                return 0;
            }
            ticksLeft--;
            sinceHit++;
            if (moving || sinceHit >= VARDORVIS_BLEED_INTERVAL) {
                sinceHit = 0;
                return damage;
            }
            return 0;
        }
    }

    /** Darting Spikes darts: 1 above ~500 hitpoints, 2 down to ~130, 3 below. */
    public static int vardorvisDarts(int hp) {
        return hp > 500 ? 1 : hp >= 130 ? 2 : 3;
    }

    // ------------------------------------------------------------ Hespori (OSRS Wiki "Hespori")
    /** Four flower buds (10 hitpoints, one hit kills) keep it invulnerable; they open at the start, 66% and 33%. */
    public static final int HESPORI_BUDS = 4;
    /** Entangle: rooted for 10 ticks, then 40+ damage, unless the player struggles free (the sixth click frees them). */
    public static final int HESPORI_ENTANGLE_TICKS = 10;
    public static final int HESPORI_ENTANGLE_MIN = 40;
    public static final int HESPORI_STRUGGLES = 6;
    /** Game ticks between entangles (approx: the wiki says a fight lasting over 30-40 seconds gets one). */
    public static final int HESPORI_ENTANGLE_INTERVAL = 58;

    // ------------------------------------------------------------ Duke Sucellus (OSRS Wiki "Duke Sucellus/Strategies")
    /** Freezing Gaze: every fifth standard attack; it lands 5 ticks after it starts (6 ticks in all) unless a pillar hides you. */
    public static final int DUKE_GAZE_EVERY = 5;
    public static final int DUKE_GAZE_TICKS = 5;
    /** Gas flares leave gas that deals up to 12 a tick (awakened ~27) for a few seconds (approx: 8 ticks). */
    public static final int DUKE_GAS_MAX = 12;
    public static final int DUKE_GAS_MAX_AWAKENED = 27;
    public static final int DUKE_GAS_TICKS = 8;

    /** Standard attacks between gas flares: 5 above 75% health, 4 above 25%, 3 below. */
    public static int dukeFlareInterval(double percent) {
        return percent > 75 ? 5 : percent > 25 ? 4 : 3;
    }

    /** Gas flares launched at once: 1 above 50% and 2 below; awakened 4 below 76%, else 1. */
    public static int dukeFlares(double percent, boolean awakened) {
        if (awakened) {
            return percent < 76 ? 4 : 1;
        }
        return percent > 50 ? 1 : 2;
    }

    /** Enraged (attacks every 4 ticks instead of 5) at 25% health; awakened at 440 of 1,697 (25.9%). */
    public static boolean dukeEnraged(double percent, boolean awakened) {
        return percent <= (awakened ? 100.0 * 440 / 1697 : 25);
    }

    /**
     * Awakened Duke from ~32% health: an Orb of Darkness rolls along the row in front of him every ~10 seconds, up to 27
     * damage and every combat stat drained by 8 if it hits.
     */
    public static final int DUKE_ORB_PERCENT = 32;
    public static final int DUKE_ORB_INTERVAL = 17;
    public static final int DUKE_ORB_MAX = 27;
    public static final int DUKE_ORB_DRAIN = 8;

    // ------------------------------------------------------------ The Leviathan (OSRS Wiki "The Leviathan/Strategies")
    /**
     * Its volleys speed up through the fight: 3-tick magic/ranged, 2-tick magic/ranged, 2-tick all three styles, 1-tick
     * magic/ranged, then 1-tick all three; enraged (below 20%) 2 ticks between attacks (awakened 1), magic and ranged only.
     */
    public static int leviathanVolleyTicks(int volley, boolean enraged, boolean awakened) {
        if (enraged) {
            return awakened ? 1 : 2;
        }
        int[] ticks = {3, 2, 2, 1, 1};
        return ticks[Math.max(0, Math.min(ticks.length - 1, volley))];
    }

    /** Whether a volley mixes in the melee (shadow) projectile: from the third volley, but not the fourth; never enraged. */
    public static boolean leviathanVolleyMelee(int volley, boolean enraged) {
        return !enraged && volley >= 2 && volley != 3;
    }

    /** Awakened from 50% health: a tornado chases the player for 40+ on contact (approx: up to 45). */
    public static final int LEVIATHAN_TORNADO_MIN = 40;
    public static final int LEVIATHAN_TORNADO_MAX = 45;

    // ------------------------------------------------------------ Amoxliatl
    /** Unstable ice: 1-4 blocks, shatter after 15 ticks into 3x3 icy pools and heal her 16-25 each. */
    public static final int UNSTABLE_ICE_TICKS = 15;
    public static final int UNSTABLE_ICE_HEAL_MIN = 16;
    public static final int UNSTABLE_ICE_HEAL_MAX = 25;
    /** Icy pools deal 6-10 a tick. */
    public static final int ICY_POOL_MIN = 6;
    public static final int ICY_POOL_MAX = 10;

    // ------------------------------------------------------------ Maggot King
    public static final int MAGGOT_HITPOINTS = 1500;
    /** Larvae from ~1,250 hitpoints, melee slams from ~980, only melee and up to four larvae from ~299. */
    public static final int MAGGOT_LARVA_HP = 1250;
    public static final int MAGGOT_MELEE_HP = 980;
    public static final int MAGGOT_FINAL_HP = 299;
    /** Screech: 15-25 a tick (up to three times) while a protection prayer is on; defence halved to 100 meanwhile. */
    public static final int MAGGOT_SCREECH_MIN = 15;
    public static final int MAGGOT_SCREECH_MAX = 25;
    public static final int MAGGOT_SCREECH_DEFENCE = 100;
    /** Slam 55 (35 through Protect from Melee); carrion 25, smaller bits 10. */
    public static final int MAGGOT_SLAM_MAX = 55;
    public static final int MAGGOT_SLAM_PRAYED = 35;
    public static final int MAGGOT_CARRION_MAX = 25;
    public static final int MAGGOT_CARRION_BIT_MAX = 10;
    /** A landed larva eats each carrion pile on its row, healing the King 1 hitpoint per pile (OSRS Wiki). */
    public static final int MAGGOT_CARRION_HEAL = 1;
    /** How long a carrion pile lies on the floor if no larva eats it (approx: until the next screech's carrion). */
    public static final int MAGGOT_CARRION_TICKS = 60;
    /** A melee punish: +50% damage on the attack landed as the slam comes down. */
    public static final double MAGGOT_PUNISH_DAMAGE = 1.5;

    /** Ranged/magic attacks between screeches: 4-8. */
    public static int maggotAttacksBeforeScreech(int roll0to4) {
        return 4 + Math.max(0, Math.min(4, roll0to4));
    }

    /** Larvae launched per screech: none above 1,250, one down to 299, then up to four (one per row without a larva). */
    public static int maggotLarvae(int hp, int alive) {
        if (hp > MAGGOT_LARVA_HP) {
            return 0;
        }
        return hp > MAGGOT_FINAL_HP ? 1 : Math.max(0, 4 - alive);
    }

    // ------------------------------------------------------------ Royal Titans
    /** Phase 2 starts when either Titan drops below 210 (35%); elementals come once a Titan has taken 100 damage. */
    public static final int TITAN_PHASE_TWO_HP = 210;
    public static final int TITAN_ELEMENTAL_DAMAGE = 100;
    /** Walls form after 4 ticks (12 to anyone standing in them, which stops that part forming). */
    public static final int TITAN_WALL_FORM_TICKS = 4;
    public static final int TITAN_WALL_MAX = 12;
    /** Elemental Blast: three blasts of up to 30, one safe tile; elementals self-destruct for 8-10 in 3x3. */
    public static final int TITAN_BLAST_MAX = 30;
    public static final int TITAN_ELEMENTAL_BLAST_MIN = 8;
    public static final int TITAN_ELEMENTAL_BLAST_MAX = 10;

    /** Whether a spell (id as in the spellbook, e.g. "water_blast") douses Royal Titans fire: Water Blast or greater, or ice. */
    public static boolean dousesFire(String spell) {
        if (spell == null) {
            return false;
        }
        String s = spell.toLowerCase(Locale.ROOT);
        return s.startsWith("ice_") || s.equals("water_blast") || s.equals("water_wave") || s.equals("water_surge");
    }

    /** Whether a spell melts Royal Titans icicles: Fire Blast or greater. */
    public static boolean meltsIce(String spell) {
        if (spell == null) {
            return false;
        }
        String s = spell.toLowerCase(Locale.ROOT);
        return s.equals("fire_blast") || s.equals("fire_wave") || s.equals("fire_surge");
    }

    // ------------------------------------------------------------ Phosani's Nightmare
    /** Sleepwalkers at the end of phases 1-3: 2, 3 and 4; in the desperation phase each one that arrives deals 15. */
    public static int sleepwalkers(int phaseEnded) {
        return Math.max(2, Math.min(4, phaseEnded + 1));
    }

    /** The power blast after the sleepwalkers: 5 + 144 x unkilled / total (OSRS Wiki). */
    public static int sleepwalkerBlast(int unkilled, int total) {
        return total <= 0 ? 5 : 5 + 144 * Math.max(0, Math.min(total, unkilled)) / total;
    }

    public static final int DESPERATION_SLEEPWALKER_DAMAGE = 15;

    /**
     * Phosani's Nightmare (OSRS Wiki): three phases of 400 shield durability, then a desperation phase of 150 health, 1,350
     * physical damage in all. Her health bar here is that damage, so a phase ends at these percentages of it left: 70.4%,
     * 40.7% and 11.1% (the third phase's sleepwalkers, then the desperation phase).
     */
    public static final int PHOSANI_PHASE_SHIELD = 400;
    public static final int PHOSANI_DESPERATION_HEALTH = 150;

    public static double phosaniPhaseEnd(int phase) {
        int total = 3 * PHOSANI_PHASE_SHIELD + PHOSANI_DESPERATION_HEALTH;
        return 100.0 * (total - PHOSANI_PHASE_SHIELD * Math.max(1, Math.min(3, phase))) / total;
    }

    /**
     * Desperation phase: one sleepwalker at a time walks in from the edge of the arena; the first reaches her about 20 ticks
     * after it appears (OSRS Wiki "Phosani's Nightmare/Strategies").
     */
    public static final int DESPERATION_WALKER_TICKS = 20;

    // ------------------------------------------------------------ Burn (OSRS Wiki "Burn")
    /** A burn hits every 4 ticks; a full burn is 10 hits (40 ticks); up to 5 burns stack, each adding 1 to every hit. */
    public static final int BURN_INTERVAL = 4;
    public static final int BURN_FULL = 10;
    public static final int BURN_MAX_STACKS = 5;

    /** The burns on one player or monster. */
    public static final class Burn {
        private final int[] hitsLeft = new int[BURN_MAX_STACKS];
        private int stacks;
        private int phase;

        /** Adds a burn of {@code hits} hits (a burn of value N); a sixth burn is discarded. */
        public void add(int hits) {
            if (hits > 0 && stacks < BURN_MAX_STACKS) {
                hitsLeft[stacks++] = hits;
            }
        }

        public int stacks() {
            return stacks;
        }

        /** One game tick: every 4th tick each burn deals 1 and burns out after its hits. */
        public int tick() {
            if (stacks == 0) {
                phase = 0;
                return 0;
            }
            if (++phase < BURN_INTERVAL) {
                return 0;
            }
            phase = 0;
            int damage = stacks;
            int kept = 0;
            for (int i = 0; i < stacks; i++) {
                if (--hitsLeft[i] > 0) {
                    hitsLeft[kept++] = hitsLeft[i];
                }
            }
            stacks = kept;
            return damage;
        }
    }

    // ------------------------------------------------------------ The Nightmare's parasites (OSRS Wiki "Parasite")
    /** The parasite bursts out ~18 ticks after it lands. */
    public static final int PARASITE_BURST_TICKS = 18;
    /** Hitpoints of a weakened parasite (a dose of sanfew serum or relicym's balm before it bursts) and of a full one. */
    public static final int PARASITE_HP_WEAKENED = 40;
    public static final int PARASITE_HP = 80;
    /** Each support blast restores 31-50 of the Nightmare's shield (or health here, where the shield is the totems). */
    public static final int PARASITE_HEAL_MIN = 31;
    public static final int PARASITE_HEAL_MAX = 50;
    /** Game ticks between a parasite's support blasts (approx: the wiki only says "periodically"). */
    public static final int PARASITE_HEAL_INTERVAL = 10;
    /** "Terrible Parent": the parasites may heal her for at most 100 in a solo kill. */
    public static final String COUNT_NIGHTMARE_PARASITE_HEAL = "nightmare_parasite_heal";
    public static final int TERRIBLE_PARENT_MAX_HEAL = 100;
    /** Phosani's: a parasite or husk survived the hit it took ("Crush Hour"). */
    public static final String FLAG_PHOSANI_MINION_SURVIVED = "phosani_minion_survived";

    /** One support blast: 31-50 (the range since the parasite rework; 1-80 before). */
    public static int parasiteHeal(java.util.Random r) {
        return PARASITE_HEAL_MIN + r.nextInt(PARASITE_HEAL_MAX - PARASITE_HEAL_MIN + 1);
    }

    /** Damage of the burst: much less when it was weakened in time (approx: the wiki gives no numbers). */
    public static int parasiteBurst(java.util.Random r, boolean weakened) {
        return weakened ? 5 + r.nextInt(6) : 30 + r.nextInt(21); // approx: 5-10 weakened, 30-50 otherwise
    }

    /** The parasite's hitpoints for how it burst out. */
    public static int parasiteHitpoints(boolean weakened) {
        return weakened ? PARASITE_HP_WEAKENED : PARASITE_HP;
    }

    // ------------------------------------------------------------ Phantom Muspah (OSRS Wiki "Phantom Muspah/Strategies")
    /** The shielded form's prayer shield: 75 points, 2 lost to every attack. */
    public static final int MUSPAH_SHIELD = 75;
    public static final int MUSPAH_SHIELD_PER_ATTACK = 2;
    /** An enchanted sapphire bolt activation drains "around 33" of it. */
    public static final int MUSPAH_SAPPHIRE_DRAIN = 33;
    /** Enchanted sapphire bolts activate 25% of the time against it (27.5% with the hard Kandarin Diary), after an accurate hit. */
    public static final double MUSPAH_SAPPHIRE_CHANCE = 0.25;
    /** Standing on a spike as it emerges knocks the player aside and heals the Muspah ~75% of the damage. */
    public static final int MUSPAH_SPIKE_MAX = 20; // approx
    /** After the shield breaks, spikes grow from the existing clusters every four of its attacks. */
    public static final int MUSPAH_SPIKE_EVERY = 4;
    /** Prayer-drain sources that can work the shield ("Versatile Drainer" needs three in one kill). */
    public static final String DRAIN_SMITE = "smite";
    public static final String DRAIN_SAPPHIRE = "sapphire_bolts";
    public static final String DRAIN_CORRUPTION = "corruption";
    public static final String FLAG_MUSPAH_THREE_DRAINS = "muspah_three_drains";
    /** "Space is Tight": it died with spikes on every tile around it. */
    public static final String FLAG_MUSPAH_SURROUNDED = "muspah_surrounded";

    /** Healing from a spike hit (75%, rounded down). */
    public static int muspahSpikeHeal(int damage) {
        return Math.max(0, damage) * 3 / 4;
    }

    /** Shield points a Smite hit drains: a quarter of the damage, as Smite takes from players. */
    public static int muspahSmiteDrain(int damage) {
        return Math.max(0, damage) / 4;
    }

    /** Shield points a corruption spell drains: everything its three pulses would take (lesser 1+2+3, greater 2+4+6). */
    public static int muspahCorruptionDrain(boolean greater) {
        return greater ? 12 : 6;
    }

    /** Whether the prayer-drain sources used in one kill complete "Versatile Drainer". */
    public static boolean versatileDrainer(java.util.Set<String> sources) {
        return sources.size() >= 3;
    }

    /** Packs a tile (block x, z) for a spike set. */
    public static long tile(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    /**
     * Whether every tile of the ring around a {@code size} x {@code size} footprint whose north-west corner is ({@code x},
     * {@code z}) holds a spike.
     */
    public static boolean muspahSurrounded(java.util.Set<Long> spikes, int x, int z, int size) {
        for (int dx = -1; dx <= size; dx++) {
            for (int dz = -1; dz <= size; dz++) {
                boolean ring = dx == -1 || dz == -1 || dx == size || dz == size;
                if (ring && !spikes.contains(tile(x + dx, z + dz))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * One growth wave of the post-shield spikes: every free tile next to an existing spike (4-neighbour) within
     * {@code radius} of the centre becomes a spike with probability {@code chance}; returns the new tiles.
     */
    public static java.util.List<Long> muspahSpikeGrowth(java.util.Set<Long> spikes, int cx, int cz, int radius, double chance,
                                                         java.util.Random r) {
        java.util.List<Long> grown = new java.util.ArrayList<>();
        java.util.List<Long> sorted = new java.util.ArrayList<>(spikes);
        java.util.Collections.sort(sorted);
        int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (long t : sorted) {
            int x = (int) (t >> 32);
            int z = (int) t;
            for (int[] s : steps) {
                int nx = x + s[0];
                int nz = z + s[1];
                long n = tile(nx, nz);
                if (Math.abs(nx - cx) > radius || Math.abs(nz - cz) > radius || spikes.contains(n) || grown.contains(n)) {
                    continue;
                }
                if (r.nextDouble() < chance) {
                    grown.add(n);
                }
            }
        }
        return grown;
    }

    // ------------------------------------------------------------ Araxxor's eggs (OSRS Wiki "Araxxor/Strategies")
    public static final String ARAXYTE_EGG = "araxyte_egg";
    /** Nine eggs, three of each colour; the first hatches after 3 standard attacks, then one every 6. */
    public static final int ARAXXOR_EGGS = 9;
    public static final int ARAXXOR_FIRST_HATCH = 3;
    public static final int ARAXXOR_HATCH_EVERY = 6;
    /** An egg has 65 hitpoints, the araxyte inside 58: damage carries over; more than 58 and the egg just despawns. */
    public static final int ARAXYTE_EGG_HP = 65;
    public static final int ARAXYTE_HP = 58;
    /** "Relaxxor": eggs destroyed in the kill. */
    public static final String COUNT_ARAXXOR_EGGS = "araxxor_eggs_destroyed";
    public static final int RELAXXOR_EGGS = 6;

    private static final String[][] EGG_PATTERNS = {
            {"acidic_araxyte", "mirrorback_araxyte", "ruptura_araxyte"},  // green > white > red
            {"mirrorback_araxyte", "ruptura_araxyte", "acidic_araxyte"},  // white > red > green
            {"ruptura_araxyte", "acidic_araxyte", "mirrorback_araxyte"}}; // red > green > white

    /**
     * The araxyte in each of the nine eggs, clockwise from the south-easternmost one: one of the three wiki patterns
     * (green acidic, white mirrorback, red ruptura), the last two sets copying the first.
     */
    public static java.util.List<String> araxxorEggs(int pattern) {
        String[] p = EGG_PATTERNS[Math.floorMod(pattern, EGG_PATTERNS.length)];
        java.util.List<String> eggs = new java.util.ArrayList<>();
        for (int i = 0; i < ARAXXOR_EGGS; i++) {
            eggs.add(p[i % 3]);
        }
        return eggs;
    }

    /** Whether Araxxor's {@code standardAttacks}-th standard attack hatches the next egg (3rd, 9th, 15th...). */
    public static boolean araxxorHatches(int standardAttacks) {
        return standardAttacks >= ARAXXOR_FIRST_HATCH && (standardAttacks - ARAXXOR_FIRST_HATCH) % ARAXXOR_HATCH_EVERY == 0;
    }

    /** The hatched araxyte's hitpoints after the egg took {@code eggDamage}; 0 when the egg despawns instead (over 58). */
    public static int araxyteHitpoints(int eggDamage) {
        return eggDamage > ARAXYTE_HP ? 0 : Math.max(1, ARAXYTE_HP - Math.max(0, eggDamage));
    }

    // ---------------- Moons of Peril (OSRS Wiki "Moons of Peril/Strategies") ----------------

    /** Combat Achievement flag: a player damaged the Eclipse Moon other than through a parried clone ("The Clone Zone"). */
    public static final String FLAG_ECLIPSE_DIRECT_HIT = "eclipse_direct_hit";
    /** Eclipse Moon flat armour: every melee or ranged hitsplat on her is reduced by 6, by 4 during her Clone special. */
    public static final int ECLIPSE_FLAT_ARMOUR = 6;
    public static final int ECLIPSE_FLAT_ARMOUR_CLONES = 4;
    /** Blue Moon flat armour of -5: every melee or ranged hitsplat on her is increased by 5. */
    public static final int BLUE_MOON_FLAT_ARMOUR = -5;
    /** The Clone special: five cycles of three clones, 15 attacks; face each clone as it appears to parry it. */
    public static final int ECLIPSE_CLONE_CYCLES = 5;
    public static final int ECLIPSE_CLONES_PER_CYCLE = 3;
    /** Game ticks between clones (approx: the wiki gives no timing; about one clone per weapon swing). */
    public static final int ECLIPSE_CLONE_INTERVAL = 2;
    /** How far (degrees) a player's facing may be off a clone and still parry it ("you can click one tile away"). approx */
    public static final double ECLIPSE_PARRY_ARC = 50;

    /** A hitsplat after flat armour: hits that land are reduced (or, for negative armour, increased); misses stay 0. */
    public static int flatArmour(int damage, int armour) {
        return damage <= 0 ? damage : Math.max(0, damage - armour);
    }

    /**
     * Whether a player facing {@code yawDegrees} (Minecraft yaw: 0 = +z, 90 = -x) faces a clone at offset (dx, dz) closely
     * enough to parry it.
     */
    public static boolean faces(double yawDegrees, double dx, double dz) {
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1e-6) {
            return true;
        }
        double yaw = Math.toRadians(yawDegrees);
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        return (fx * dx + fz * dz) / len >= Math.cos(Math.toRadians(ECLIPSE_PARRY_ARC));
    }

    // ---------------- Zalcano (OSRS Wiki "Zalcano", "Zalcano/Strategies", tephra pages) ----------------

    /** Her stone armour (300) must be broken with imbued tephra before she can be mined (1,000 hitpoints). */
    public static final int ZALCANO_ARMOUR = 300;
    public static final String TEPHRA = "tephra";
    public static final String REFINED_TEPHRA = "refined_tephra";
    public static final String IMBUED_TEPHRA = "imbued_tephra";
    public static final List<String> TEPHRA_ITEMS = List.of(TEPHRA, REFINED_TEPHRA, IMBUED_TEPHRA);
    /** XP: 35.2 Mining per tephra (and per hit on the fallen Zalcano), 10 Smithing per refine, 5 Runecraft per imbue. */
    public static final double TEPHRA_MINING_XP = 35.2;
    public static final double TEPHRA_REFINE_XP = 10;
    public static final double TEPHRA_IMBUE_XP = 5;
    public static final double ZALCANO_MINING_XP = 35.2;
    /** 1/65 chance of 1-3 crystal shards with each tephra mined. */
    public static final int TEPHRA_SHARD_CHANCE = 65;
    /** Golems (14 hitpoints) restore 50 armour at full health (a share of it when damaged); killed, 16-24 imbued tephra. */
    public static final int ZALCANO_GOLEM_HP = 14;
    public static final int ZALCANO_GOLEM_HEAL = 50;
    public static final int ZALCANO_GOLEM_TEPHRA_MIN = 16;
    public static final int ZALCANO_GOLEM_TEPHRA_MAX = 24;
    /** She stays down 12-16 seconds (20-27 ticks); standing under her deals 3-10 a tick. */
    public static final int ZALCANO_DOWN_MIN_TICKS = 20;
    public static final int ZALCANO_DOWN_MAX_TICKS = 27;
    public static final int ZALCANO_UNDER_MIN = 3;
    public static final int ZALCANO_UNDER_MAX = 10;
    /** Rising: a shockwave of ~10 to anyone beside her (stunned 1-2 ticks); every tephra in the prison is destroyed. */
    public static final int ZALCANO_RISE_DAMAGE = 10;
    /** The glowing rock explodes for 21-28 when she targets it; orange symbols ramp up to 26 after 3 ticks. */
    public static final int ZALCANO_ROCK_MIN = 21;
    public static final int ZALCANO_ROCK_MAX = 28;
    public static final int ZALCANO_SYMBOL_MAX = 26;
    /** Combat Achievement flag: imbued tephra received from a golem ("Team Player"). */
    public static final String FLAG_ZALCANO_GOLEM_TEPHRA = "zalcano_golem_tephra";

    /**
     * Imbued tephra's max hit on her armour (Mod Ash): 5 + floor((Smithing + Runecraft) / 14), 50% more standing on a blue
     * demonic symbol. approx: the chance of +1 as the average nears the next multiple of 7 isn't modelled.
     */
    public static int tephraMaxHit(int smithing, int runecraft, boolean blueSymbol) {
        int base = 5 + (Math.max(1, smithing) + Math.max(1, runecraft)) / 14;
        return blueSymbol ? base * 3 / 2 : base;
    }

    /** Mining the fallen Zalcano: 30 (1-74 Mining), 31, 32, 33 (95+), plus max((pickaxe level - 40) / 15, 0). */
    public static int zalcanoMiningMaxHit(int mining, int pickaxeLevel) {
        int base = mining >= 95 ? 33 : mining >= 85 ? 32 : mining >= 75 ? 31 : 30;
        return base + Math.max((pickaxeLevel - 40) / 15, 0);
    }

    /** Armour a golem restores when it reaches her: 50 at full health, in proportion when damaged. */
    public static int golemArmourHeal(double hpFraction) {
        return (int) Math.round(ZALCANO_GOLEM_HEAL * Math.max(0, Math.min(1, hpFraction)));
    }

    // ---------------- The Hueycoatl (OSRS Wiki "The Hueycoatl/Strategies") ----------------

    /** Phase one: five body parts of 250 hitpoints (they don't attack) block the way to the head. */
    public static final String HUEYCOATL_BODY = "hueycoatl_body";
    public static final String HUEYCOATL_TAIL = "hueycoatl_tail";
    public static final int HUEY_BODY_SEGMENTS = 5;
    public static final int HUEY_BODY_HP = 250;
    /** Phase three: the tail (300 hitpoints) shields her from 50%; hits on it cap at 4, or 9 with crush as the best bonus. */
    public static final int HUEY_TAIL_HP = 300;
    public static final int HUEY_TAIL_CAP = 4;
    public static final int HUEY_TAIL_CAP_CRUSH = 9;
    /** The tail changes sides about every 20 seconds and slams out a 20-25 shockwave; in the final phase every ~15 s. */
    public static final int HUEY_TAIL_SWAP_TICKS = 33;
    public static final int HUEY_FINAL_SHOCKWAVE_TICKS = 25;
    public static final int HUEY_SHOCKWAVE_MIN = 20;
    public static final int HUEY_SHOCKWAVE_MAX = 25;
    /** Lit braziers (all three pillars charged when the seer's bar fills) give every player 30% more damage. */
    public static final double HUEY_BRAZIER_DAMAGE = 1.30;
    /** Ticks for the seer's charge bar to fill (approx: not documented). */
    public static final int HUEY_SEER_CHARGE_TICKS = 16;
    /** Combat Achievement flag: the Hueycoatl died while the braziers were lit ("Pillar Lover"). */
    public static final String FLAG_HUEY_VULNERABLE_KILL = "huey_vulnerable_kill";

    /** Active protection prayers needed per pillar for a team: 1+ for 1-5 players, 2+ for 6-8, 3+ for 9-11... 6+ for 18-20. */
    public static int hueyPrayersPerPillar(int teamSize) {
        return teamSize <= 5 ? 1 : (teamSize - 3) / 3 + 1;
    }

    /** Whether the blue (Magic), red (Melee) and green (Missiles) pillars are all charging for this team. */
    public static boolean hueyPillarsCharged(int prayingMelee, int prayingMissiles, int prayingMagic, int teamSize) {
        int need = hueyPrayersPerPillar(teamSize);
        return prayingMelee >= need && prayingMissiles >= need && prayingMagic >= need;
    }

    /** A hit on the tail: capped at 4 (9 with crush as the attacker's highest attack bonus, which also turns misses into 1). */
    public static int hueyTailDamage(int damage, boolean crushBest) {
        if (crushBest) {
            return Math.max(1, Math.min(damage, HUEY_TAIL_CAP_CRUSH));
        }
        return Math.max(0, Math.min(damage, HUEY_TAIL_CAP));
    }

    // ---------------- The Nightmare / Phosani's Nightmare ----------------

    /** Combat Achievement flags: a player was made weary by a spore; Phosani switched off a player's protection prayer. */
    public static final String FLAG_NIGHTMARE_SPORED = "nightmare_spored";
    public static final String FLAG_PHOSANI_PRAYER_DISABLED = "phosani_prayer_disabled";
    /** Spores: standing within one tile bursts one; weary players can't run and attack 1 tick slower (Phosani: 2). */
    public static final int SPORE_SLOW_TICKS = 1;
    public static final int SPORE_SLOW_TICKS_PHOSANI = 2;
    /** Phosani's spores: every yawn drains 6 prayer points. */
    public static final int SPORE_YAWN_PRAYER = 6;
    /** How long a player stays weary (approx: "a short duration"). */
    public static final int SPORE_WEARY_TICKS = 20;
    /** Spores placed per Spores special (approx: "set patterns"). */
    public static final int SPORES = 8;
    /** Attack names of the weakened (cured) and full parasite bursts ("Perfect Phosani's Nightmare" allows the weakened one). */
    public static final String PARASITE_BURST = "parasite_burst";
    public static final String WEAKENED_PARASITE_BURST = "weakened_parasite_burst";

    // ---------------- Araxxor: ruptura araxytes (OSRS Wiki "Araxxor/Strategies") ----------------

    /** Combat Achievement flag: one ruptura araxyte's explosion killed three other araxytes ("Araxyte Betrayal"). */
    public static final String FLAG_ARAXYTE_BETRAYAL = "araxyte_betrayal";
    public static final int ARAXYTE_BETRAYAL_KILLS = 3;
    /** The explosion reaches 4 tiles from the ruptura (an 8x8 area). */
    public static final int RUPTURA_RANGE = 4;

    /** Explosion damage to a player {@code tiles} away: 70 underneath, 21 less per tile, scaled by the ruptura's health. */
    public static int rupturaPlayerDamage(int tiles, double hpFraction) {
        int full = Math.max(0, 70 - 21 * Math.max(0, tiles));
        return (int) Math.floor(full * Math.max(0, Math.min(1, hpFraction)));
    }

    /** Explosion damage to Araxxor, araxytes and eggs: 80 under it, 64 next to it, at least 33 up to 3 tiles (approx between). */
    public static int rupturaEnemyDamage(int tiles) {
        if (tiles > RUPTURA_RANGE) {
            return 0;
        }
        return tiles <= 0 ? 80 : Math.max(33, 80 - 16 * tiles);
    }

    // ---------------- The Whisperer (OSRS Wiki "The Whisperer/Strategies", "Lost Soul (The Whisperer)") ----------------
    /** The lost souls of her Soul Siphon. */
    public static final String WHISPERER_LOST_SOUL = "whisperer_lost_soul";
    /** Her three special attacks come at these shares of her 900 hitpoints (720, 495 and 270). */
    public static final int[] WHISPERER_SPECIAL_PERCENT = {80, 55, 30};
    /** Awakened, she uses one at roughly every tenth of her 2,700 hitpoints instead. */
    public static final int[] WHISPERER_AWAKENED_SPECIAL_HP = {2400, 2120, 1820, 1600, 1310, 1050, 800, 500};
    public static final int WHISPERER_AWAKENED_HP = 2700;
    /** The specials in order: Corrupted Seeds, Screech, Soul Siphon (or the same cycle started at the Screech). */
    public static final String[] WHISPERER_SPECIALS = {"seeds", "screech", "siphon"};
    /** The tentacles after each volley (20, awakened 30, whatever how many hit; dodged by moving off the tile). */
    public static final int WHISPERER_TENTACLE = 20;
    public static final int WHISPERER_TENTACLE_AWAKENED = 30;
    /** Screech: three blasts (awakened five) of 45 (70) for anyone she can see; hide behind the floating columns. */
    public static final int WHISPERER_SCREECHES = 3;
    public static final int WHISPERER_SCREECHES_AWAKENED = 5;
    /** Soul Siphon: 20 ticks (awakened 35) to kill a full set of lost souls, else 50 to the player and she heals 100. */
    public static final int WHISPERER_SIPHON_TICKS = 20;
    public static final int WHISPERER_SIPHON_TICKS_AWAKENED = 35;
    public static final int WHISPERER_SIPHON_HEAL = 100;
    /** Lost soul sets: Vita (heal 20% hitpoints), Oratio (20% prayer), Sanitas (sanity), Mors (50 to her, 75 if all die). */
    public static final String[] WHISPERER_SOUL_SETS = {"vita", "oratio", "sanitas", "mors"};
    public static final int[] WHISPERER_SOUL_SET_SIZES = {2, 3, 3, 4};
    public static final int WHISPERER_MORS_DAMAGE = 50;
    public static final int WHISPERER_MORS_DAMAGE_ALL = 75;
    /** Corrupted Seeds: step on every light seed in time (awakened 26 ticks; approx 33 normally) or take 75. */
    public static final int WHISPERER_SEED_TICKS = 33; // approx
    public static final int WHISPERER_SEED_TICKS_AWAKENED = 26;
    public static final int WHISPERER_SEED_FAIL = 75;
    /** A dark seed stepped on: 15-25 (awakened 60-75). */
    public static final int[] WHISPERER_DARK_SEED = {15, 25};
    public static final int[] WHISPERER_DARK_SEED_AWAKENED = {60, 75};
    /** During a special's animation she takes 70% less damage. */
    public static final double WHISPERER_SPECIAL_DAMAGE = 0.3;
    /** The binding projectile after a special: ~8 seconds. */
    public static final int WHISPERER_BIND_TICKS = 13;
    /** At 0 hitpoints she heals 140 (awakened 250), pulls the player into the Shadow Realm and attacks every 2 ticks. */
    public static final int WHISPERER_ENRAGE_HEAL = 140;
    public static final int WHISPERER_ENRAGE_HEAL_AWAKENED = 250;
    /** Her melee (next to her): two hits, the second the same as the first; Protect from Melee only trims it (approx 3/4). */
    public static final double WHISPERER_MELEE_THROUGH_PRAYER = 0.75; // approx: "slightly reduces damage"

    /** The special attack used as her {@code n}th (from 0) when the rotation starts at {@code start} (0 seeds, 1 screech). */
    public static String whispererSpecial(int start, int n) {
        return WHISPERER_SPECIALS[(start + n) % WHISPERER_SPECIALS.length];
    }

    /**
     * Styles of one of her volleys (true = magic), {@code first} being its first projectile's: three projectiles, all one style
     * before her first special, then two and one (M-M-R), after the third special alternating (M-R-M); awakened five: three and
     * two (R-R-R-M-M), from the second special one, two, two (R-M-M-R-R), from the seventh alternating.
     */
    public static boolean[] whispererVolley(int specialsDone, boolean awakened, boolean first) {
        boolean[] v = new boolean[awakened ? 5 : 3];
        for (int i = 0; i < v.length; i++) {
            boolean same;
            if (!awakened) {
                same = specialsDone == 0 || (specialsDone < 3 ? i < 2 : i % 2 == 0);
            } else if (specialsDone < 2) {
                same = i < 3;
            } else if (specialsDone < 7) {
                same = i == 0 || i >= 3;
            } else {
                same = i % 2 == 0;
            }
            v[i] = same == first;
        }
        return v;
    }

    /** Whether her special number {@code done} (from 0) is due at {@code hp} of {@code maxHp} (her variant's hitpoints). */
    public static boolean whispererSpecialDue(int done, double hp, int maxHp, boolean awakened) {
        if (awakened) {
            return done < WHISPERER_AWAKENED_SPECIAL_HP.length
                    && hp * WHISPERER_AWAKENED_HP / maxHp <= WHISPERER_AWAKENED_SPECIAL_HP[done];
        }
        return done < WHISPERER_SPECIAL_PERCENT.length && hp * 100.0 / maxHp <= WHISPERER_SPECIAL_PERCENT[done];
    }
}
