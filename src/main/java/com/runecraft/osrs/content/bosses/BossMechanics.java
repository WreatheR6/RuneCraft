package com.runecraft.osrs.content.bosses;

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
            "bleed", "strangle", "burn", "knockback");

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
     * Yama's sigil contracts (the five that exist as items here; the acquisition contracts and their rewards don't). Each is
     * consumed when Yama is challenged while holding it, raises his stats (max hit 53, 7-tick attacks) and is returned on a
     * kill. OSRS Wiki "Yama#Contracts".
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
        BLOODIED_BLOWS("contract_of_bloodied_blows", 3000, 3, false, false, false, false);

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
    public static final int VARDORVIS_BLEED_TICKS = 14;
    public static final int VARDORVIS_BLEED_INTERVAL = 3;
    /** Strangle (below 570): destroy the spores within 5 ticks (awakened 4, approx) or take ~25 (awakened ~40). */
    public static final int VARDORVIS_STRANGLE_TICKS = 5;
    public static final int VARDORVIS_STRANGLE_TICKS_AWAKENED = 4;
    public static final int VARDORVIS_STRANGLE_DAMAGE = 25;
    public static final int VARDORVIS_STRANGLE_DAMAGE_AWAKENED = 40;
    /** Spores to destroy per Strangle (the wiki says "several"; approx). */
    public static final int VARDORVIS_STRANGLE_SPORES = 3;

    /** A Vardorvis bleed: damage per game tick, more often while the player is running. */
    public static final class Bleed {
        private int ticksLeft;
        private int sinceHit;

        public void start() {
            ticksLeft = VARDORVIS_BLEED_TICKS;
            sinceHit = 1; // the first hit lands 2 ticks in: 5 hits over 14 ticks standing still
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
                return VARDORVIS_BLEED_DAMAGE;
            }
            return 0;
        }
    }

    /** Darting Spikes darts: 1 above ~500 hitpoints, 2 down to ~130, 3 below. */
    public static int vardorvisDarts(int hp) {
        return hp > 500 ? 1 : hp >= 130 ? 2 : 3;
    }

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
}
