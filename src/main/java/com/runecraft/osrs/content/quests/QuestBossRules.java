package com.runecraft.osrs.content.quests;

import java.util.Map;

/**
 * The rules of the master quests' boss fights that their scripts (the quests module's QuestBossScripts)
 * apply, kept here so they are unit-tested (OSRS Wiki and the DPS calculator's monster data).
 */
public final class QuestBossRules {
    private QuestBossRules() {
    }

    /**
     * Bosses only magic of one element hurts (other spells do nothing): Fareed (water), Kamil (fire). Other styles follow
     * each boss's own rules.
     */
    public static final Map<String, String> SPELL_ELEMENT = Map.of("fareed", "water", "kamil", "fire");

    /** Whether a spell of {@code element} (null: not an elemental spell) can hurt the boss. */
    public static boolean spellHurts(String bossId, String element) {
        String only = SPELL_ELEMENT.get(bossId);
        return only == null || only.equals(element);
    }

    /** Fareed's melee max hit, and against a player wearing ice gloves. */
    public static final int FAREED_MAX = 40;
    public static final int FAREED_MAX_ICE_GLOVES = 37;

    /** Damis's two forms (DPS calculator): level, Hitpoints, Attack, Strength, Defence, attack speed, max hit. */
    public static final int[] DAMIS_FIRST = {103, 90, 90, 90, 90, 4, 22};
    public static final int[] DAMIS_SECOND = {174, 200, 160, 100, 160, 3, 28};

    /** Times Dessous teleports to a player hitting him from afar before he grows bored and returns to his grave. */
    public static final int DESSOUS_TELEPORTS = 3;
    /** Dessous's bat swarm: half Magic, half Ranged, each up to 5. */
    public static final int DESSOUS_BATS_MAX = 10;
    /** Dessous's 3-tick melee. */
    public static final int DESSOUS_MELEE_MAX = 19;

    /**
     * Monkey Madness I: the 10th squad's gnomes hit the Jungle demon for up to {@link #GNOME_MAX_HIT} every
     * {@link #GNOME_HIT_TICKS} game ticks (approx), and it regenerates to 20% of its hitpoints (wiki) when anyone but the
     * player would deal the final blow.
     */
    public static final int GNOME_HIT_TICKS = 4;
    public static final int GNOME_MAX_HIT = 4;
    public static final int JUNGLE_DEMON_REGEN_PERCENT = 20;

    /** Making Friends with My Arm: the quest flag set when the Fire of Domination is doused; Mother can't be hurt before. */
    public static final String MOTHER_FIRE_OUT_FLAG = "mfa_fire_out";

    /** Dream Mentor (wiki): the Illusive burrows away after this many hits land on it. */
    public static final int ILLUSIVE_HITS_BEFORE_BURROW = 2;
    /** Dream Mentor: the Inadequacy summons Doubts on one attack in this many, while fewer than the cap are about (approx). */
    public static final int INADEQUACY_SUMMON_ONE_IN = 6;
    public static final int INADEQUACY_MAX_DOUBTS = 4;
    /** Dream Mentor (wiki): Cyrisus's fears, fought in this order. */
    public static final java.util.List<String> DREAM_FEARS = java.util.List.of("the_inadequacy", "the_everlasting", "the_untouchable",
            "the_illusive");

    /** A Kingdom Divided (wiki): only melee hurts the Judge of Yama on the pier. */
    public static boolean judgeOfYamaHurtBy(com.runecraft.osrs.AttackType type) {
        return type != com.runecraft.osrs.AttackType.MAGIC && type != com.runecraft.osrs.AttackType.RANGED;
    }

    /** A Kingdom Divided: the Judge banishes the player this far down the docks on one attack in this many (approx). */
    public static final int JUDGE_BANISH_EVERY = 5;
    public static final double JUDGE_BANISH_BLOCKS = 8;

    /** A Kingdom Divided (wiki): Xamphur's special mechanics, every tenth attack, falling hands and Phantom hands in turn. */
    public static final int XAMPHUR_SPECIAL_EVERY = 10;

    /** Xamphur's attack on his {@code n}th (1-based): a special, or null for his basic magic attack. */
    public static String xamphurSpecial(int n) {
        if (n % XAMPHUR_SPECIAL_EVERY != 0) {
            return null;
        }
        return (n / XAMPHUR_SPECIAL_EVERY) % 2 == 1 ? "falling_hands" : "phantom_hands";
    }

    /** The Path of Glouphrie (wiki): warped terrorbirds are only hurt by a player carrying this. */
    public static final String TERRORBIRD_CHIME = "crystal_chime";
    /** Their ranged screech with earmuffs or a slayer helmet on (wiki: it lowers the max hit; the number is approx). */
    public static final int TERRORBIRD_SCREECH_MAX_WITH_EARS = 8;
    public static final java.util.List<String> TERRORBIRD_EAR_PROTECTION = java.util.List.of("earmuffs", "slayer_helmet", "slayer_helmet_i");

    /** Beneath Cursed Sands (wiki: "after several normal attacks"): one attack in this many is a special (approx). */
    public static final int SCABARAS_SPECIAL_EVERY = 5;
    /**
     * The Shadow Rift: 20 hitpoints, and unless destroyed it explodes after a few seconds for "35+" (strategy) and up to 48
     * (its infobox) on the players fighting the Champion (wiki; the fuse is approx).
     */
    public static final int SCABARAS_RIFT_FUSE_TICKS = 8;
    public static final int SCABARAS_RIFT_MIN = 35;
    public static final int SCABARAS_RIFT_MAX = 48;
    /** The Champion's specials in order, then again (wiki: Shadow Burst -> Shadow Rift -> Shadow Burst -> Shadow Flames -> Repeat). */
    public static final java.util.List<String> SCABARAS_SPECIALS = java.util.List.of("shadow_burst", "shadow_rift", "shadow_burst",
            "shadow_flames");

    /** The Champion's special on its {@code n}th attack (1-based), or null for a normal attack. */
    public static String scabarasSpecial(int n) {
        if (n <= 0 || n % SCABARAS_SPECIAL_EVERY != 0) {
            return null;
        }
        return SCABARAS_SPECIALS.get((n / SCABARAS_SPECIAL_EVERY - 1) % SCABARAS_SPECIALS.size());
    }

    /** One Shadow Rift explosion: 35 to 48. // approx: the spread between the wiki's "35+" and its 48 max */
    public static int scabarasRiftDamage(java.util.Random random) {
        return SCABARAS_RIFT_MIN + random.nextInt(SCABARAS_RIFT_MAX - SCABARAS_RIFT_MIN + 1);
    }

    /**
     * A Night at the Theatre (wiki): the quest flag a completed Theatre of Blood run (any mode) sets, which the Mysterious
     * Stranger waits for.
     */
    public static final String TOB_COMPLETED_FLAG = "tob_completed";

    /** Quest flags that game systems set rather than quest triggers (the Theatre of Blood's completion). */
    public static final java.util.Set<String> EXTERNAL_FLAGS = java.util.Set.of(TOB_COMPLETED_FLAG);

    /** Haunted Mine: Treus Dayth's carts and cranes hit up to this every few game ticks (approx; the wiki gives no numbers). */
    public static final int TREUS_MACHINERY_TICKS = 6;
    public static final int TREUS_MACHINERY_MAX = 4;

    /** Haunted Mine (wiki: more often the closer he is to dying): one hit in this many makes Treus Dayth shift (approx). */
    public static int treusShiftOneIn(float healthLeft) {
        return healthLeft > 0.5f ? 4 : healthLeft > 0.25f ? 3 : 2;
    }

    /** Legends' Quest (wiki): Nezikchened drains Prayer to a tenth of the player's Prayer level as the fight begins. */
    public static int nezikchenedPrayerCap(int prayerLevel) {
        return prayerLevel / 10;
    }

    /** Recipe for Disaster (wiki): Karamel's ice spell hits twice for up to 7. */
    public static final int KARAMEL_SPELL_HIT = 7;
    /** Recipe for Disaster (wiki, Mod Ash): the Culinaromancer's magic hits up to 25 with a flat 1-in-3 chance. */
    public static final int CULINAROMANCER_MAGIC_MAX = 25;

    /** Whether the Culinaromancer's spell lands, from a roll of 0-2. */
    public static boolean culinaromancerHits(int roll0to2) {
        return roll0to2 == 0;
    }

    /** The Culinaromancer's minions fought in order before him (wiki, quest-helper RFDFinal). */
    public static final java.util.List<String> CULINAROMANCER_FIGHTS = java.util.List.of("agrith_na_na", "flambeed", "karamel", "dessourt",
            "gelatinnoth_mother", "culinaromancer");

    /** Dragon Slayer I (wiki): Elvarg's dragonfire hits up to 70 (10 behind an anti-dragon shield) and drains 10% of Prayer. */
    public static final int ELVARG_DRAGONFIRE_MAX = 70;
    public static final int ELVARG_PRAYER_DRAIN_PERCENT = 10;

    // ---------------------------------------------------------------- Sins of the Father: Vanstrom Klause (wiki quest guide)

    /**
     * His three specials. The first is picked at random; then they follow this order, and none comes again until all three
     * have been used (wiki). // approx: the wiki doesn't say which order; the one it lists them in
     */
    public static final java.util.List<String> VANSTROM_SPECIALS = java.util.List.of("pets", "stare_into_darkness", "blood_orb");

    /** His {@code n}th special (0-based) for a fight whose first special was {@code first}. */
    public static String vanstromSpecial(int first, int n) {
        return VANSTROM_SPECIALS.get(Math.floorMod(first + n, VANSTROM_SPECIALS.size()));
    }

    /** Protect from Magic halves his blood blasts (wiki: "reduces the damage from this attack by 50%"). */
    public static final double VANSTROM_BLAST_PRAYED_SHARE = 0.5;
    /** His blood blast heals him for a portion of what it deals. // approx: the wiki gives no share */
    public static final double VANSTROM_BLAST_HEAL_SHARE = 0.25;
    /** The acidic bloodveld's explosion when it reaches the player: 35-50, through prayers (wiki; DPS calculator max 50). */
    public static final int VANSTROM_PET_MIN = 35;
    public static final int VANSTROM_PET_MAX = 50;
    /** Blocks from the arena centre, north or south (the side farther from the player), the bloodveld appears. */
    public static final int VANSTROM_PET_DISTANCE = 9;
    /** Blocks from the player the bloodveld explodes at. */
    public static final double VANSTROM_PET_REACH = 1.4;
    /** "Stare into darkness!": 40+ to a player who doesn't step off their tile (wiki). // approx: 35-45 */
    public static final int VANSTROM_STARE_MIN = 35;
    public static final int VANSTROM_STARE_MAX = 45;
    /** Game ticks to step away. // playability: OSRS gives about one tick ("extremely unforgiving"); the strike floor here */
    public static final int VANSTROM_STARE_TICKS = 3;
    /**
     * "Blood will be my strength!": a blood orb, visually one tile but 3x3, strengthens his magic attacks even through Protect
     * from Magic while it lasts. Luring him into it bursts it and hits him for 20+; a player stepping in takes up to 40 and bursts
     * it (wiki). // approx: the buff and how long the orb lasts
     */
    public static final double VANSTROM_ORB_RADIUS = 1.5;
    public static final int VANSTROM_ORB_TICKS = 30;
    public static final double VANSTROM_ORB_MAGIC_MULTIPLIER = 1.5;
    public static final int VANSTROM_ORB_SELF_MIN = 20;
    public static final int VANSTROM_ORB_SELF_MAX = 26;
    public static final int VANSTROM_ORB_PLAYER_MAX = 40;
    /** Second phase (wiki): every 2 seconds or so 10-15 shadows mark random tiles for lightning of up to 40. */
    public static final int VANSTROM_LIGHTNING_EVERY_TICKS = 4;
    public static final int VANSTROM_LIGHTNING_MIN_BOLTS = 10;
    public static final int VANSTROM_LIGHTNING_MAX_BOLTS = 15;
    public static final int VANSTROM_LIGHTNING_MAX = 40;
    /** Blocks around the player the shadows fall in (one always on the player's own tile). // approx */
    public static final int VANSTROM_LIGHTNING_SPREAD = 6;

    /** How many bolts a lightning volley calls down, from a random 0-5. */
    public static int vanstromBolts(int roll0to5) {
        return VANSTROM_LIGHTNING_MIN_BOLTS + Math.max(0, Math.min(5, roll0to5));
    }

    // ---------------------------------------------------------------- While Guthix Sleeps: Surok, the Stone of Jas

    /** Surok Magis (wiki): like Vorkath, six regular attacks and a special as every seventh. */
    public static final int SUROK_SPECIAL_EVERY = 7;

    /**
     * Touching the Stone of Jas raises the player's combat stats to 255 for the fight against Lucien's two tormented demons,
     * and stat drains don't touch them (wiki).
     */
    public static final int STONE_OF_JAS_LEVEL = 255;
    /** The quest flag set while the player holds the Stone's power. */
    public static final String STONE_OF_JAS_FLAG = "wgs_jas_power";
    /** The demons' fire shield: non-demonbane hits land at this share while it is up (wiki: "reduces damage slightly"). // approx */
    public static final double WGS_DEMON_SHIELD = 0.8;
    /** Auto attacks between fire bombs (the tormented demon's 8 or 9). */
    public static final int WGS_DEMON_ATTACKS_PER_BOMB = 8;
    /** The fire bomb: a short bind, then fire on the player's tile and around it, up to 45 (DPS calculator special). */
    public static final int WGS_DEMON_BOMB_MAX = 45;
    public static final int WGS_DEMON_BIND_TICKS = 2;
    /** Game ticks from the bind to the fire landing. // playability: lands after the bind, with time to step off */
    public static final int WGS_DEMON_BOMB_TICKS = 5;
    public static final double WGS_DEMON_BOMB_RADIUS = 1.5;

    /** The boost that puts a skill at the Stone's level: {@code 255 - level}. */
    public static int stoneOfJasBoost(int realLevel) {
        return STONE_OF_JAS_LEVEL - realLevel;
    }

    /** The Curse of Arrav (wiki quick guide): every third attack Arrav swings his axe ahead of him for up to 26. */
    public static final int COA_ARRAV_AXE_EVERY = 3;

    /** The Final Dawn (wiki): Lucius's triple projectile and Metzli's specials come every third attack. */
    public static final int TFD_LUCIUS_SPECIAL_EVERY = 3;
    public static final int TFD_METZLI_SPECIAL_EVERY = 3;

    /** Whether a boss's {@code attack}th attack (1-based) is its every-{@code n}th special. */
    public static boolean nthIsSpecial(int attack, int n) {
        return n > 0 && attack > 0 && attack % n == 0;
    }

    /** Song of the Elves (wiki): the Fragment of Seren makes one of her specials every fourth attack (three normal ones first). */
    public static final int FRAGMENT_ATTACKS_PER_SPECIAL = 4;

    /** Whether the Fragment of Seren's next attack, after {@code attacksMade} attacks, is one of her specials. */
    public static boolean fragmentSpecialNext(int attacksMade) {
        return (attacksMade + 1) % FRAGMENT_ATTACKS_PER_SPECIAL == 0;
    }
}
