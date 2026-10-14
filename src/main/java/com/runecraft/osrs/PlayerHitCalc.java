package com.runecraft.osrs;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * A player's attack roll, max hit, the NPC's defence roll and the hit chance for one attack, with the gear effects
 * that change them, applied in the order and with the integer flooring of the OSRS Wiki DPS calculator
 * (weirdgloop/osrs-dps-calc {@code PlayerVsNPCCalc}): void and elite void, salve amulets, black mask / slayer
 * helmet (and imbued), obsidian armour, berserker necklace, crystal armour with crystal bows, the twisted bow,
 * dragon hunter weapons, demonbane (Arclight, Emberlight, Silverlight, Darklight), keris vs kalphites, golembane,
 * leafy, Inquisitor's armour, Osmumten's fang, Dharok's, the Colossal blade, powered staves, Tumeken's shadow,
 * smoke staves, tomes and elemental weaknesses. Snapshot-tested by {@code CombatReferenceTest}; the Minecraft
 * side builds an {@link Input} from the player and target ({@code combat.HitCalcBridge}).
 *
 * <p>Fill an {@link Input}, then call {@link #attackRoll}, {@link #maxHit}, {@link #defenceRoll} and
 * {@link #hitChance}.
 */
public final class PlayerHitCalc {
    private PlayerHitCalc() {
    }

    /** Everything that decides one attack. Levels are current (boosted) levels. */
    public static final class Input {
        public AttackType type = AttackType.SLASH;
        /** The selected style (ranged styles for ranged; for magic only a powered staff's accurate/longrange matters). */
        public CombatStyle style = CombatStyle.ACCURATE;
        public int attack = 1;
        public int strength = 1;
        public int ranged = 1;
        public int magic = 1;
        public int hitpointsLevel = 10;
        public int hitpoints = 10;
        public final Set<Prayer> prayers = EnumSet.noneOf(Prayer.class);
        /** Ids of every worn item (weapon, shield, armour, jewellery, cape, gloves, ammo). */
        public final Set<String> worn = new HashSet<>();
        public String weapon = "";
        /** Equipment attack bonus for {@link #type} (before Tumeken's shadow's multiplier). */
        public int attackBonus;
        public int strengthBonus;
        /** Total ranged strength (ammunition included). */
        public int rangedStrength;
        /** Total magic damage bonus in tenths of a percent (occult necklace = 100). */
        public int magicDamageTenths;
        /** A cast spell's base max hit (already scaled with Magic level), or -1 for none (weapon / powered staff). */
        public int spellMaxHit = -1;
        /** The spell's element ("fire", "water", "earth", "air") or null. */
        public String spellElement;
        public boolean standardSpell;
        public boolean boltSpell;
        // ---- target
        public int targetDefenceLevel = 1;
        /** The level the target defends magic with (its Magic level for NPCs). */
        public int targetMagicLevel = 1;
        public int targetDefenceBonus;
        /**
         * The target's {light, standard, heavy} ranged defence, or null to use {@link #targetDefenceBonus} for every
         * ranged weapon. With it, a ranged attack rolls against the class of {@link #weapon} ({@link #rangedDefenceKind}).
         */
        public int[] targetRangedDefence;
        /** max(magic level, magic attack bonus) of the target, for the twisted bow. */
        public int targetTbowMagic = 1;
        /** OSRS monster attributes: undead, demon, dragon, kalphite, leafy, golem, xerician, ... */
        public final Set<String> attributes = new HashSet<>();
        /** The target is the player's slayer task (and a slayer monster). */
        public boolean onTask;
        public int targetSize = 1;
        /** The target's elemental weakness and its severity in percent (null / 0 for none). */
        public String weaknessElement;
        public int weaknessPercent;
        /** The target is a revenant (amulet of avarice). */
        public boolean revenant;
        /** Forinthry surge: skulled while wearing the amulet of avarice (+35% instead of +20% against revenants). */
        public boolean forinthrySurge;
        /** The player is in the Wilderness (revenant weapons: +50% accuracy and damage while charged). */
        public boolean inWilderness;
        /** The weapon holds charges (revenant ether for the revenant weapons). */
        public boolean weaponCharged = true;
        /** The attack is the weapon's special attack (osrs-dps-calc {@code usingSpecialAttack}; see {@link #specialWeapon}). */
        public boolean special;
        /** Soulreaper axe soul stacks (0-5) consumed by its special attack. */
        public int soulreaperStacks;
        /** Prayer points below the Prayer level (abyssal bludgeon's Penance). */
        public int prayerMissing;

        public Input wear(String... ids) {
            for (String id : ids) {
                worn.add(id);
            }
            return this;
        }

        public Input pray(Collection<Prayer> list) {
            prayers.addAll(list);
            return this;
        }

        boolean has(String id) {
            return worn.contains(id);
        }

        boolean any(String... ids) {
            for (String id : ids) {
                if (worn.contains(id)) {
                    return true;
                }
            }
            return false;
        }

        boolean attr(String a) {
            return attributes.contains(a);
        }

        boolean melee() {
            return type.isMelee();
        }
    }

    // ---------------------------------------------------------------- gear predicates

    static boolean voidRobes(Input in) {
        return in.any("void_knight_top", "elite_void_top") && in.any("void_knight_robe", "elite_void_robe") && in.has("void_knight_gloves");
    }

    static boolean eliteVoidRobes(Input in) {
        return in.has("elite_void_top") && in.has("elite_void_robe") && in.has("void_knight_gloves");
    }

    public static boolean meleeVoid(Input in) {
        return voidRobes(in) && in.has("void_melee_helm");
    }

    public static boolean rangedVoid(Input in) {
        return voidRobes(in) && in.has("void_ranger_helm");
    }

    public static boolean magicVoid(Input in) {
        return voidRobes(in) && in.has("void_mage_helm");
    }

    static boolean blackMask(Input in) {
        return in.any("black_mask", "slayer_helmet") || imbuedMask(in);
    }

    static boolean imbuedMask(Input in) {
        return in.any("black_mask_i", "slayer_helmet_i");
    }

    static boolean tzhaarWeapon(Input in) {
        return CombatBonuses.TZHAAR_MELEE.contains(in.weapon) || in.weapon.equals("toktz_mej_tal");
    }

    static boolean obsidian(Input in) {
        return in.has("obsidian_helmet") && in.has("obsidian_platebody") && in.has("obsidian_platelegs");
    }

    static boolean dharok(Input in) {
        return in.has("dharoks_helm") && in.has("dharoks_platebody") && in.has("dharoks_platelegs") && in.weapon.equals("dharoks_greataxe");
    }

    static boolean fang(Input in) {
        return in.weapon.startsWith("osmumtens_fang");
    }

    static boolean keris(Input in) {
        return in.weapon.startsWith("keris");
    }

    static boolean smokeStaff(Input in) {
        return in.weapon.equals("smoke_battlestaff") || in.weapon.equals("mystic_smoke_staff") || in.weapon.equals("twinflame_staff");
    }

    static int crystalPieces(Input in) {
        return (in.has("crystal_helm") ? 1 : 0) + (in.has("crystal_legs") ? 2 : 0) + (in.has("crystal_body") ? 3 : 0);
    }

    static boolean salveE(Input in) {
        return in.any("salve_amulet_e", "salve_amulet_ei") && in.attr("undead");
    }

    static boolean salve(Input in) {
        return in.any("salve_amulet", "salve_amulet_i") && in.attr("undead");
    }

    /** Amulet of avarice against a revenant. */
    static boolean avarice(Input in) {
        return in.revenant && in.has("amulet_of_avarice");
    }

    static final Set<String> REV_MELEE = Set.of("viggoras_chainmace", "ursine_chainmace");
    static final Set<String> REV_RANGED = Set.of("craws_bow", "webweaver_bow");
    static final Set<String> REV_MAGIC = Set.of("thammarons_sceptre", "accursed_sceptre");

    /** A charged revenant weapon of the attack's style, used in the Wilderness: +50% accuracy and damage. */
    public static boolean revWeapon(Input in) {
        if (!in.inWilderness || !in.weaponCharged) {
            return false;
        }
        return switch (in.type) {
            case MAGIC -> REV_MAGIC.contains(in.weapon);
            case RANGED -> REV_RANGED.contains(in.weapon);
            default -> REV_MELEE.contains(in.weapon);
        };
    }

    /** True for a revenant weapon (whatever the style). */
    public static boolean isRevWeapon(String weapon) {
        return REV_MELEE.contains(weapon) || REV_RANGED.contains(weapon) || REV_MAGIC.contains(weapon);
    }

    /** Silver ammunition (silver bolts, poisoned or not). */
    static final Set<String> SILVER_AMMO = Set.of("silver_bolts", "silver_bolts_p", "silver_bolts_pp", "silver_bolts_ppp");
    /** Silver melee weapons (osrs-dps-calc {@code isWearingSilverWeapon}). */
    static final Set<String> SILVER_MELEE = Set.of("blessed_axe", "ivandis_flail", "blisterwood_flail", "hallowed_flail", "silver_sickle",
            "silver_sickle_b", "emerald_sickle", "emerald_sickle_b", "enchanted_emerald_sickle_b", "ruby_sickle_b", "enchanted_ruby_sickle_b",
            "blisterwood_sickle", "silverlight", "darklight", "arclight", "rod_of_ivandis", "wolfbane");

    /** A silver weapon (melee) or silver bolts / blisterwood stakes (ranged): hurts vampyres. */
    public static boolean silverWeapon(Input in) {
        if (in.type == AttackType.RANGED) {
            return in.weapon.equals("blisterwood_stake") || in.any(SILVER_AMMO.toArray(new String[0]));
        }
        return in.melee() && SILVER_MELEE.contains(in.weapon);
    }

    /** The target's vampyre tier (1-3; 0 when not a vampyre), from the {@code vampyre1..3} attributes. */
    public static int vampyreTier(Input in) {
        for (int t = 3; t >= 1; t--) {
            if (in.attr("vampyre" + t)) {
                return t;
            }
        }
        return 0;
    }

    /** A weapon that deals full damage to tier 2 (or tier 3) vampyres (osrs-dps-calc {@code wearingVampyrebane}). */
    public static boolean vampyrebane(Input in, int tier) {
        if (in.type == AttackType.RANGED && in.weapon.equals("blisterwood_stake")) {
            return true;
        }
        if (tier == 2 && in.weapon.equals("rod_of_ivandis")) {
            return true;
        }
        return in.melee() && (in.weapon.equals("ivandis_flail") || in.weapon.equals("blisterwood_sickle")
                || in.weapon.equals("blisterwood_flail") || in.weapon.equals("hallowed_flail") || in.weapon.equals("sunspear"));
    }

    private static int factor(int value, int num, int den) {
        return (int) ((long) value * num / den);
    }

    private static int addFactor(int value, int num, int den) {
        return value + factor(value, num, den);
    }

    // ---------------------------------------------------------------- prayers

    /** Applies the best accuracy prayer for the attack's style to a level. */
    static int prayerAccuracy(Input in, int level) {
        int pct = 100;
        for (Prayer p : in.prayers) {
            pct = Math.max(pct, (int) Math.round(100 * switch (in.type) {
                case RANGED -> p.rangedAttackMultiplier();
                case MAGIC -> p.magicMultiplier();
                default -> p.attackMultiplier();
            }));
        }
        return factor(level, pct, 100);
    }

    /** Applies the best strength prayer: Burst of Strength / Sharp Eye give +1 instead of 5% at level 20 or below. */
    static int prayerStrength(Input in, int level) {
        int pct = 100;
        for (Prayer p : in.prayers) {
            pct = Math.max(pct, (int) Math.round(100 * (in.type == AttackType.RANGED ? p.rangedStrengthMultiplier() : p.strengthMultiplier())));
        }
        if (pct == 105 && level <= 20) {
            return level + 1;
        }
        return factor(level, pct, 100);
    }

    // ---------------------------------------------------------------- attack roll

    public static long attackRoll(Input in) {
        if (in.melee()) {
            return meleeAttackRoll(in);
        }
        return in.type == AttackType.RANGED ? rangedAttackRoll(in) : magicAttackRoll(in);
    }

    static int meleeAttackRoll(Input in) {
        int eff = prayerAccuracy(in, in.attack) + 8 + in.style.forMelee().attackBonus();
        if (meleeVoid(in)) {
            eff = factor(eff, 11, 10);
        }
        int base = eff * (in.attackBonus + 64);
        int roll = base;
        if (avarice(in)) {
            roll = factor(roll, in.forinthrySurge ? 27 : 24, 20);
        } else if (salveE(in)) {
            roll = factor(roll, 6, 5);
        } else if (salve(in)) {
            roll = factor(roll, 7, 6);
        } else if (blackMask(in) && in.onTask) {
            roll = factor(roll, 7, 6);
        }
        if (tzhaarWeapon(in) && obsidian(in)) {
            roll += base / 10;
        }
        if (revWeapon(in)) {
            roll = factor(roll, 3, 2);
        }
        boolean demon = in.attr("demon");
        if (demon && (in.weapon.equals("arclight") || in.weapon.equals("emberlight"))) {
            roll = addFactor(roll, 70, 100);
        }
        if (demon && (in.weapon.equals("silverlight") || in.weapon.equals("darklight"))) {
            roll = addFactor(roll, 60, 100);
        }
        if (demon && in.weapon.equals("burning_claws")) {
            roll = addFactor(roll, 5, 100); // burning claws: demonbane 5%
        }
        if (in.attr("dragon")) {
            if (in.weapon.equals("dragon_hunter_lance")) {
                roll = factor(roll, 6, 5);
            } else if (in.weapon.equals("dragon_hunter_wand")) {
                roll = factor(roll, 7, 4);
            }
        }
        if (in.weapon.equals("keris_partisan_of_breaching") && in.attr("kalphite")) {
            roll = factor(roll, 133, 100);
        }
        boolean vampyre = vampyreTier(in) > 0;
        if (vampyre && (in.weapon.equals("blisterwood_flail") || in.weapon.equals("blisterwood_sickle"))) {
            roll = factor(roll, 105, 100);
        }
        if (vampyre && (in.weapon.equals("hallowed_flail") || in.weapon.equals("sunspear"))) {
            roll = factor(roll, 125, 100);
        }
        if (vampyre && silverWeapon(in) && in.has("efaritays_aid")) {
            roll = factor(roll, 23, 20);
        }
        if (in.weapon.equals("granite_hammer") && in.attr("golem")) {
            roll = factor(roll, 13, 10);
        }
        if (in.type == AttackType.CRUSH) {
            int inq = inquisitorPieces(in);
            if (inq > 0) {
                roll = factor(roll, 200 + inq, 200);
            }
        }
        if (in.special) {
            String w = specialWeapon(in);
            if (godsword(w) || w.equals("brine_sabre") || w.equals("barrelchest_anchor")) {
                roll = factor(roll, 2, 1);
            } else if (w.equals("osmumtens_fang") || w.equals("arkan_blade") || w.equals("granite_hammer")) {
                roll = factor(roll, 3, 2);
            } else if (w.equals("elder_maul") || w.equals("dragon_mace") || w.equals("dragon_sword") || w.equals("dragon_scimitar")
                    || w.equals("abyssal_whip") || w.equals("abyssal_dagger")) {
                roll = factor(roll, 5, 4);
            } else if (w.equals("dragon_dagger")) {
                roll = factor(roll, 23, 20);
            } else if (w.equals("soulreaper_axe")) {
                roll = factor(roll, 100 + 12 * Math.max(0, Math.min(5, in.soulreaperStacks)), 100);
            } else {
                roll = otherSpecial(roll, w, true);
            }
        }
        return roll;
    }

    // ---------------------------------------------------------------- special attacks (osrs-dps-calc usingSpecialAttack)

    private static final java.util.Map<String, String> SPECIAL_ALIASES = java.util.Map.ofEntries(
            java.util.Map.entry("dragon_dagger_p", "dragon_dagger"), java.util.Map.entry("dragon_dagger_pp", "dragon_dagger"),
            java.util.Map.entry("dragon_dagger_ppp", "dragon_dagger"), java.util.Map.entry("abyssal_dagger_p", "abyssal_dagger"),
            java.util.Map.entry("abyssal_dagger_pp", "abyssal_dagger"), java.util.Map.entry("abyssal_dagger_ppp", "abyssal_dagger"),
            java.util.Map.entry("armadyl_godsword_or", "armadyl_godsword"), java.util.Map.entry("bandos_godsword_or", "bandos_godsword"),
            java.util.Map.entry("saradomin_godsword_or", "saradomin_godsword"), java.util.Map.entry("zamorak_godsword_or", "zamorak_godsword"),
            java.util.Map.entry("volcanic_abyssal_whip", "abyssal_whip"), java.util.Map.entry("frozen_abyssal_whip", "abyssal_whip"),
            java.util.Map.entry("dragon_scimitar_or", "dragon_scimitar"), java.util.Map.entry("granite_maul_or", "granite_maul"),
            java.util.Map.entry("crystal_halberd_basic", "crystal_halberd"), java.util.Map.entry("crystal_halberd_attuned", "crystal_halberd"),
            java.util.Map.entry("crystal_halberd_perfected", "crystal_halberd"), java.util.Map.entry("osmumtens_fang_or", "osmumtens_fang"),
            java.util.Map.entry("dragon_knife_p", "dragon_knife"), java.util.Map.entry("dragon_knife_pp", "dragon_knife"),
            java.util.Map.entry("dragon_knife_ppp", "dragon_knife"), java.util.Map.entry("blazing_blowpipe", "toxic_blowpipe"),
            java.util.Map.entry("accursed_sceptre_a", "accursed_sceptre"));

    /** The weapon whose special attack this is: LMS copies, poisoned / ornamented variants and crystal halberds map to the base weapon. */
    public static String specialWeapon(Input in) {
        String w = in.weapon == null ? "" : in.weapon.startsWith("lms_") ? in.weapon.substring(4) : in.weapon;
        return SPECIAL_ALIASES.getOrDefault(w, w);
    }

    static boolean godsword(String w) {
        return w.equals("armadyl_godsword") || w.equals("bandos_godsword") || w.equals("saradomin_godsword") || w.equals("zamorak_godsword")
                || w.equals("ancient_godsword");
    }

    /** Wearing the full blood moon set (dual macuahuitl special attack). */
    static boolean bloodMoon(Input in) {
        return in.has("blood_moon_helm") && in.has("blood_moon_chestplate") && in.has("blood_moon_tassets");
    }

    /**
     * Accuracy (or max hit) multiplier of a special attack the DPS calculator doesn't model, from {@code SpecialAttacks}
     * (armadyl crossbow, dragon crossbow, abyssal tentacle, rune claws, morrigan's javelin...; floored like the calculator's factors).
     */
    static int otherSpecial(int value, String weapon, boolean accuracy) {
        com.runecraft.osrs.content.systems.SpecialAttacks.Spec s = com.runecraft.osrs.content.systems.SpecialAttacks.of(weapon);
        if (s == null || modelled(weapon)) {
            return value;
        }
        double m = accuracy ? s.accuracy() : s.damage();
        if (m <= 0 || m == 1.0) {
            return value;
        }
        return (int) Math.floor(value * m + 1e-9);
    }

    /** Weapons whose special attack accuracy and damage this class takes from the DPS calculator's model. */
    static final Set<String> MODELLED = Set.of("dragon_dagger", "dragon_longsword", "dragon_mace", "dragon_sword", "dragon_scimitar",
            "dragon_warhammer", "dragon_claws", "burning_claws", "dragon_halberd", "crystal_halberd", "armadyl_godsword", "bandos_godsword",
            "saradomin_godsword", "zamorak_godsword", "ancient_godsword", "elder_maul", "abyssal_dagger", "abyssal_bludgeon", "abyssal_whip",
            "osmumtens_fang", "voidwaker", "arclight", "emberlight", "saradomin_sword", "saradomins_blessed_sword", "barrelchest_anchor",
            "granite_hammer", "soulreaper_axe", "dual_macuahuitl", "crimson_kisten", "brine_sabre", "dark_bow", "magic_shortbow",
            "magic_shortbow_i", "magic_longbow", "magic_comp_bow", "seercull", "toxic_blowpipe", "zaryte_crossbow", "heavy_ballista",
            "light_ballista", "dragon_knife", "webweaver_bow", "tonalztics_of_ralos", "volatile_nightmare_staff", "eldritch_nightmare_staff",
            "accursed_sceptre", "eye_of_ayak", "dawnbringer", "sunspear", "arkan_blade");

    public static boolean modelled(String weapon) {
        return MODELLED.contains(weapon);
    }

    /**
     * The defence style a special attack rolls against when it isn't the attack's own style (osrs-dps-calc
     * {@code getNPCDefenceRoll}): slash for godswords, claws, dragon dagger / longsword / scimitar / halberd, crystal halberd,
     * abyssal dagger and Saradomin sword; stab for arclight, emberlight and dragon sword; magic for the voidwaker and
     * Saradomin's blessed sword; crush for the dragon mace and crimson kisten. Null when it keeps the attack's style.
     */
    public static AttackType specialDefenceType(Input in) {
        if (!in.special) {
            return null;
        }
        String w = specialWeapon(in);
        if (godsword(w) || w.equals("dragon_claws") || w.equals("dragon_dagger") || w.equals("dragon_halberd") || w.equals("dragon_longsword")
                || w.equals("dragon_scimitar") || w.equals("crystal_halberd") || w.equals("abyssal_dagger") || w.equals("saradomin_sword")
                || w.equals("arkan_blade")) {
            return AttackType.SLASH;
        }
        if (w.equals("arclight") || w.equals("emberlight") || w.equals("dragon_sword")) {
            return AttackType.STAB;
        }
        if (w.equals("voidwaker") || w.equals("saradomins_blessed_sword")) {
            return AttackType.MAGIC;
        }
        if (w.equals("dragon_mace") || w.equals("crimson_kisten")) {
            return AttackType.CRUSH;
        }
        return null;
    }

    /** True when the special attack always hits (voidwaker, Dawnbringer, magic longbow / comp bow and seercull). */
    public static boolean specialAlwaysHits(Input in) {
        if (!in.special) {
            return false;
        }
        String w = specialWeapon(in);
        return w.equals("voidwaker") || w.equals("dawnbringer") || w.equals("magic_longbow") || w.equals("magic_comp_bow") || w.equals("seercull");
    }

    /** Ranged strength of the worn ammunition (arrows / bolts), 0 without any. */
    static int ammoStrength(Input in) {
        for (String id : in.worn) {
            Integer s = com.runecraft.osrs.data.EquipmentBonuses.ammoStrength(id);
            if (s != null) {
                return s;
            }
        }
        return 0;
    }

    /** Hitsplat groups of a special attack (osrs-dps-calc's attack distribution: dragon dagger 2, webweaver bow 4, ...). */
    public static int specialHits(Input in) {
        if (!in.special) {
            return 1;
        }
        String w = specialWeapon(in);
        return switch (w) {
            case "dragon_dagger", "dragon_knife", "magic_shortbow", "magic_shortbow_i", "dark_bow", "rosewood_blowpipe" -> 2;
            case "dragon_halberd", "crystal_halberd" -> in.targetSize > 1 ? 2 : 1;
            case "webweaver_bow" -> 4;
            default -> 1;
        };
    }

    /**
     * The largest total damage of one special attack (osrs-dps-calc's attacker distribution maximum): the per-hit max times
     * the hits, the dragon / burning claws cascades, dark bow's cap of 48 per arrow, the abyssal dagger's second hit, the
     * Saradomin sword's lightning (16), the granite hammer's +5, crimson kisten's 170% and the charged Tonalztics' two throws.
     */
    public static int specialMaxTotal(Input in) {
        int[] mm = minMax(in);
        int max = mm[1];
        if (!in.special) {
            return max;
        }
        String w = specialWeapon(in);
        return switch (w) {
            case "dragon_claws" -> clawsMaxTotal(max, false);
            case "burning_claws" -> clawsMaxTotal(max, true);
            case "dark_bow" -> 2 * Math.max(mm[0], Math.min(max, 48));
            case "abyssal_dagger" -> 2 * max;
            case "saradomin_sword" -> max + 16;
            case "granite_hammer" -> max + 5;
            case "crimson_kisten" -> max * 170 / 100;
            case "tonalztics_of_ralos" -> in.weaponCharged ? 2 * max : max;
            default -> specialHits(in) * max;
        };
    }

    /**
     * Total damage range of a claws special whose first successful accuracy roll is {@code accRoll} (0-based; dragon claws
     * roll 4 times, burning claws 3): {low, high} (osrs-dps-calc {@code generateTotals}).
     */
    public static int[] clawsTotalRange(int accRoll, int max, boolean burning) {
        int rolls = burning ? 3 : 4;
        int low = max * (rolls - accRoll) / 4;
        return new int[]{low, max + low + (burning ? 0 : -1)};
    }

    /** The hitsplats of a claws special for a total {@code dmg} rolled after accuracy roll {@code accRoll} succeeded. */
    public static int[] clawsSplit(int accRoll, int dmg, boolean burning) {
        if (burning) {
            return switch (accRoll) {
                case 0 -> new int[]{dmg / 2, dmg / 4, dmg / 4};
                case 1 -> new int[]{dmg / 2 - 1, dmg / 2 - 1, 2};
                default -> new int[]{dmg - 2, 1, 1};
            };
        }
        return switch (accRoll) {
            case 0 -> new int[]{dmg / 2, dmg / 4, dmg / 8, dmg / 8 + 1};
            case 1 -> new int[]{0, dmg / 2, dmg / 4, dmg / 4 + 1};
            case 2 -> new int[]{0, 0, dmg / 2, dmg / 2 + 1};
            default -> new int[]{0, 0, 0, dmg + 1};
        };
    }

    static int clawsMaxTotal(int max, boolean burning) {
        int best = burning ? 2 : 2; // a complete miss can still deal 1 + 1
        for (int r = 0; r < (burning ? 3 : 4); r++) {
            int[] range = clawsTotalRange(r, max, burning);
            for (int d = range[0]; d <= range[1]; d++) {
                int sum = 0;
                for (int h : clawsSplit(r, d, burning)) {
                    sum += h;
                }
                best = Math.max(best, sum);
            }
        }
        return best;
    }

    /** Inquisitor's armour with a crush attack: helm +0.5%, hauberk +1%, plateskirt +1% (no extra set bonus). */
    static int inquisitorPieces(Input in) {
        return (in.has("inquisitors_great_helm") ? 1 : 0) + (in.has("inquisitors_hauberk") ? 2 : 0) + (in.has("inquisitors_plateskirt") ? 2 : 0);
    }

    static int rangedAttackRoll(Input in) {
        int eff = prayerAccuracy(in, in.ranged) + 8 + (in.style.forRanged() == CombatStyle.RANGED_ACCURATE ? 3 : 0);
        if (rangedVoid(in)) {
            eff = factor(eff, 11, 10);
        }
        int roll = eff * (in.attackBonus + 64);
        if (CombatBonuses.isCrystalBow(in.weapon)) {
            roll = factor(roll, 20 + crystalPieces(in), 20);
        }
        if (avarice(in)) {
            roll = factor(roll, in.forinthrySurge ? 27 : 24, 20);
        } else if (in.has("salve_amulet_ei") && in.attr("undead")) {
            roll = factor(roll, 6, 5);
        } else if (in.has("salve_amulet_i") && in.attr("undead")) {
            roll = factor(roll, 7, 6);
        } else if (imbuedMask(in) && in.onTask) {
            roll = factor(roll, 23, 20);
        }
        if (in.weapon.equals("twisted_bow")) {
            roll = factor(roll, CombatBonuses.twistedBowAccuracy(tbowMagic(in)), 100);
        }
        if (revWeapon(in)) {
            roll = factor(roll, 3, 2);
        }
        if (in.weapon.startsWith("dragon_hunter_crossbow") && in.attr("dragon")) {
            roll = factor(roll, 13, 10);
        }
        if (in.special) {
            String w = specialWeapon(in);
            if (w.equals("zaryte_crossbow") || w.equals("webweaver_bow") || w.equals("toxic_blowpipe")) {
                roll = factor(roll, 2, 1);
            } else if (w.equals("magic_shortbow") || w.equals("magic_shortbow_i")) {
                roll = factor(roll, 10, 7);
            } else if (w.equals("heavy_ballista") || w.equals("light_ballista")) {
                roll = factor(roll, 5, 4);
            } else if (w.equals("tonalztics_of_ralos")) {
                roll = factor(roll, 3, 2);
            } else {
                roll = otherSpecial(roll, w, true);
            }
        }
        return roll;
    }

    static int tbowMagic(Input in) {
        return Math.min(in.attr("xerician") ? 350 : 250, Math.max(0, in.targetTbowMagic));
    }

    /** Equipment magic attack after Tumeken's shadow's multiplier. */
    static int magicAttackBonus(Input in) {
        return in.weapon.equals("tumekens_shadow") ? in.attackBonus * CombatBonuses.shadowMultiplier(false) : in.attackBonus;
    }

    static boolean poweredStaff(Input in) {
        return in.spellMaxHit < 0 && CombatBonuses.poweredStaffMaxHit(in.weapon, 99) >= 0;
    }

    static int magicAttackRoll(Input in) {
        int eff = prayerAccuracy(in, in.magic) + 9;
        if (poweredStaff(in) && in.style != CombatStyle.LONGRANGE && in.style != CombatStyle.DEFENSIVE) {
            eff += 2;
        } else if (in.special && in.spellMaxHit < 0 && specialWeapon(in).endsWith("_nightmare_staff")) {
            eff += 2; // the nightmare staves' specials cast a built-in spell in the powered staff's accurate stance
        }
        if (magicVoid(in)) {
            eff = factor(eff, 29, 20);
        }
        int base = eff * (magicAttackBonus(in) + 64);
        int roll = base;
        int additive = 0;
        boolean mask = false;
        if (avarice(in)) {
            additive += in.forinthrySurge ? 35 : 20;
        } else if (in.has("salve_amulet_ei") && in.attr("undead")) {
            additive += 20;
        } else if (in.has("salve_amulet_i") && in.attr("undead")) {
            additive += 15;
        } else if (imbuedMask(in) && in.onTask) {
            mask = true;
        }
        if (smokeStaff(in) && in.standardSpell) {
            additive += 10;
        }
        if (additive != 0) {
            roll = factor(roll, 100 + additive, 100);
        }
        if (in.attr("dragon")) {
            if (in.weapon.startsWith("dragon_hunter_crossbow")) {
                roll = factor(roll, 13, 10);
            } else if (in.weapon.equals("dragon_hunter_lance")) {
                roll = factor(roll, 6, 5);
            } else if (in.weapon.equals("dragon_hunter_wand")) {
                roll = factor(roll, 7, 4);
            }
        }
        if (mask) {
            roll = factor(roll, 23, 20);
        }
        if (revWeapon(in)) {
            roll = factor(roll, 3, 2);
        }
        if (in.has("tome_of_water") && "water".equals(in.spellElement)) {
            roll = factor(roll, 6, 5);
        }
        if (in.special) {
            String w = specialWeapon(in);
            if (w.equals("accursed_sceptre") || w.equals("volatile_nightmare_staff")) {
                roll = factor(roll, 3, 2);
            } else if (w.equals("eye_of_ayak")) {
                roll = factor(roll, 2, 1);
            }
        }
        if (in.spellElement != null && in.spellElement.equals(in.weaknessElement)) {
            roll += factor(base, in.weaknessPercent, 100);
        }
        return roll;
    }

    // ---------------------------------------------------------------- max hit

    /** {min, max} of one hit (Osmumten's fang has a minimum hit). */
    public static int[] minMax(Input in) {
        int[] mm;
        if (in.melee()) {
            mm = meleeMax(in);
        } else if (in.type == AttackType.RANGED) {
            mm = rangedMinMax(in);
        } else {
            mm = new int[]{0, magicMax(in)};
        }
        mm[0] = Math.max(0, mm[0]);
        mm[1] = Math.max(Math.max(0, mm[1]), mm[0]);
        return mm;
    }

    /** The max hit of one attack, including Dharok's set and the berserker necklace's damage scaling. */
    public static int maxHit(Input in) {
        int max = minMax(in)[1];
        if (in.melee() && dharok(in)) {
            int lost = Math.max(0, in.hitpointsLevel - in.hitpoints);
            max = (int) ((long) max * (10000 + lost * in.hitpointsLevel) / 10000);
        }
        if (in.melee() && tzhaarWeapon(in) && in.any("berserker_necklace", "berserker_necklace_or")) {
            max = factor(max, 6, 5);
        }
        return max;
    }

    /**
     * True when the attack can't damage the target at all: a tier 3 vampyre without an Ivandis flail or blisterwood weapon,
     * or a tier 2 vampyre without one of those, the Rod of Ivandis, a silver weapon or Efaritay's aid.
     */
    public static boolean immune(Input in) {
        int tier = vampyreTier(in);
        if (tier == 3) {
            return !vampyrebane(in, 3);
        }
        return tier == 2 && !vampyrebane(in, 2) && !in.has("efaritays_aid") && !silverWeapon(in);
    }

    /**
     * Changes to each rolled hit (osrs-dps-calc's hit-distribution transforms, applied after {@link #maxHit}): against
     * vampyres Efaritay's aid (x1.1) and the weapon's vampyrebane multiplier (blisterwood flail / stake x1.25, sickle x1.15,
     * Ivandis flail x1.2, other silver weapons x1.1 against tier 1), then against tier 2 a non-vampyrebane weapon deals
     * half damage with Efaritay's aid or at most 10 with a silver weapon; immune targets take nothing.
     */
    public static int afterRoll(Input in, int damage) {
        if (damage <= 0) {
            return 0;
        }
        int tier = vampyreTier(in);
        if (tier == 0) {
            return damage;
        }
        if (immune(in)) {
            return 0;
        }
        boolean efaritay = in.has("efaritays_aid");
        int num = 0;
        int den = 1;
        if (in.any("blisterwood_flail", "hallowed_flail", "blisterwood_stake")) {
            num = 125;
            den = 100;
        } else if (in.has("sunspear")) {
            num = 150;
            den = 100;
        } else if (in.has("blisterwood_sickle")) {
            num = 23;
            den = 20;
        } else if (in.has("ivandis_flail")) {
            num = 6;
            den = 5;
        } else if (silverWeapon(in) && tier == 1) {
            // The calculator's Rod of Ivandis branch never matches (it spells the item "Rod of ivandis"), so the rod only
            // gets this silver x1.1, against tier 1.
            num = 11;
            den = 10;
        }
        if (num != 0) {
            if (efaritay) {
                damage = damage * 11 / 10;
            }
            damage = damage * num / den;
        }
        if (tier == 2) {
            if (!vampyrebane(in, 2) && efaritay) {
                damage = damage / 2;
            } else if (silverWeapon(in)) {
                damage = Math.min(damage, 10);
            }
        }
        return damage;
    }

    static int[] meleeMax(Input in) {
        int eff = prayerStrength(in, in.strength) + 8 + in.style.forMelee().strengthBonus();
        if (!in.special && specialWeapon(in).equals("soulreaper_axe")) {
            // Soul stacks: +6% of the Strength level each (not multiplied by prayers; consumed by the special attack)
            eff += in.strength * 6 * Math.max(0, Math.min(5, in.soulreaperStacks)) / 100;
        }
        if (meleeVoid(in)) {
            eff = factor(eff, 11, 10);
        }
        int base = (eff * (in.strengthBonus + 64) + 320) / 640;
        int max = base;
        int min = 0;
        if (avarice(in)) {
            max = factor(max, in.forinthrySurge ? 27 : 24, 20);
        } else if (salveE(in)) {
            max = factor(max, 6, 5);
        } else if (salve(in)) {
            max = factor(max, 7, 6);
        } else if (blackMask(in) && in.onTask) {
            max = factor(max, 7, 6);
        }
        boolean demon = in.attr("demon");
        if (demon && (in.weapon.equals("arclight") || in.weapon.equals("emberlight"))) {
            max = addFactor(max, 70, 100);
        }
        if (demon && in.weapon.equals("burning_claws")) {
            max = addFactor(max, 5, 100);
        }
        if (tzhaarWeapon(in) && obsidian(in)) {
            max += base / 10;
        }
        if (in.attr("dragon")) {
            if (in.weapon.equals("dragon_hunter_lance")) {
                max = factor(max, 6, 5);
            } else if (in.weapon.equals("dragon_hunter_wand")) {
                max = factor(max, 7, 5);
            }
        }
        if (keris(in) && in.attr("kalphite")) {
            max = factor(max, in.weapon.equals("keris_partisan_of_amascut") ? 115 : 133, 100);
        }
        if (in.attr("golem")) {
            if (in.weapon.equals("barronite_mace")) {
                max = factor(max, 23, 20);
            } else if (in.weapon.equals("granite_hammer")) {
                max = factor(max, 13, 10);
            }
        }
        if (revWeapon(in)) {
            max = factor(max, 3, 2);
        }
        if (demon && (in.weapon.equals("silverlight") || in.weapon.equals("darklight"))) {
            max = addFactor(max, 60, 100);
        }
        if (in.weapon.equals("leaf_bladed_battleaxe") && in.attr("leafy")) {
            max = factor(max, 47, 40);
        }
        if (in.weapon.equals("colossal_blade")) {
            max += Math.min(in.targetSize * 2, 10);
        }
        if (in.type == AttackType.CRUSH) {
            int inq = inquisitorPieces(in);
            if (inq > 0) {
                max = factor(max, 200 + inq, 200);
            }
        }
        if (fang(in)) {
            min = max * 3 / 20;
            if (!in.special) {
                max -= min; // Eviscerate keeps the full max hit
            }
        }
        if (in.special) {
            String w = specialWeapon(in);
            if (godsword(w)) {
                max = factor(max, 11, 10);
            }
            switch (w) {
                case "bandos_godsword", "saradomin_sword", "dragon_halberd", "crystal_halberd" -> max = factor(max, 11, 10);
                case "armadyl_godsword", "dragon_sword", "dragon_longsword", "saradomins_blessed_sword" -> max = factor(max, 5, 4);
                case "dragon_mace", "dragon_warhammer", "arkan_blade" -> max = factor(max, 3, 2);
                case "voidwaker" -> {
                    min = max / 2;
                    max += min;
                }
                case "dragon_dagger" -> max = factor(max, 23, 20);
                case "abyssal_dagger" -> max = factor(max, 17, 20);
                case "abyssal_bludgeon" -> max = (int) ((long) max * (200 + Math.max(0, in.prayerMissing)) / 200);
                case "barrelchest_anchor" -> max = factor(max, 110, 100);
                case "dual_macuahuitl" -> {
                    if (bloodMoon(in)) {
                        min = max / 4;
                        max += min;
                    }
                }
                case "soulreaper_axe" -> {
                    int stacks = Math.max(0, Math.min(5, in.soulreaperStacks));
                    min = max * 6 * stacks / 100;
                    max = factor(max, 100 + 6 * stacks, 100);
                }
                default -> max = otherSpecial(max, w, false);
            }
        }
        return new int[]{min, max};
    }

    static int rangedMax(Input in) {
        int eff = prayerStrength(in, in.ranged) + 8 + (in.style.forRanged() == CombatStyle.RANGED_ACCURATE ? 3 : 0);
        if (rangedVoid(in) && eliteVoidRobes(in)) {
            eff = factor(eff, 9, 8);
        } else if (rangedVoid(in)) {
            eff = factor(eff, 11, 10);
        }
        int max = (eff * (in.rangedStrength + 64) + 320) / 640;
        if (CombatBonuses.isCrystalBow(in.weapon)) {
            max = factor(max, 40 + crystalPieces(in), 40);
        }
        boolean dragonbane = in.weapon.startsWith("dragon_hunter_crossbow") && in.attr("dragon");
        boolean rev = revWeapon(in);
        if (avarice(in)) {
            max = factor(max, in.forinthrySurge ? 27 : 24, 20);
        } else if (in.has("salve_amulet_ei") && in.attr("undead")) {
            max = factor(max, 6, 5);
        } else if (in.has("salve_amulet_i") && in.attr("undead")) {
            max = factor(max, 7, 6);
        } else if (imbuedMask(in) && in.onTask) {
            int num = 23;
            if (rev) {
                rev = false;
                num += 10;
            }
            if (dragonbane) {
                dragonbane = false;
                num += 5;
            }
            max = factor(max, num, 20);
        }
        if (in.weapon.equals("twisted_bow")) {
            max = factor(max, CombatBonuses.twistedBowDamage(tbowMagic(in)), 100);
        }
        if (rev) {
            max = factor(max, 3, 2);
        }
        if (dragonbane) {
            max = factor(max, 5, 4);
        }
        if (in.weapon.equals("tonalztics_of_ralos")) {
            max = factor(max, 3, 4); // rolls 75% of the max hit (and throws twice when charged)
        }
        return max;
    }

    /** {min, max} of a ranged hit, with the special attacks' changes. */
    static int[] rangedMinMax(Input in) {
        if (!in.special) {
            return new int[]{0, rangedMax(in)};
        }
        String w = specialWeapon(in);
        if (w.equals("magic_shortbow") || w.equals("magic_shortbow_i") || w.equals("magic_longbow") || w.equals("magic_comp_bow")
                || w.equals("seercull")) {
            // Snapshot / Powershot / Soulshot: Ranged level + 10 and the arrow's strength only; prayers and other gear are ignored.
            int eff = in.ranged + 10;
            return new int[]{0, (eff * (ammoStrength(in) + 64) + 320) / 640};
        }
        int max = rangedMax(in);
        int min = 0;
        switch (w) {
            case "toxic_blowpipe" -> max = factor(max, 3, 2);
            case "webweaver_bow" -> max -= max * 6 / 10;
            case "heavy_ballista", "light_ballista" -> max = factor(max, 5, 4);
            case "dark_bow" -> {
                boolean dragon = in.any("dragon_arrow", "lms_dragon_arrow");
                min = dragon ? 8 : 5;
                max = factor(max, dragon ? 15 : 13, 10);
            }
            default -> max = otherSpecial(max, w, false);
        }
        return new int[]{min, max};
    }

    /** Base max hit of the attack's spell or powered staff (0 when neither). */
    static int magicBase(Input in) {
        if (in.special && in.spellMaxHit < 0) {
            String w = specialWeapon(in);
            // Invocate / Immolate: a spell whose base max hit scales with Magic up to 44 / 58 (osrs-dps-calc).
            if (w.equals("eldritch_nightmare_staff")) {
                return Math.max(1, Math.min(44, (99 + 44 * in.magic) / 99));
            }
            if (w.equals("volatile_nightmare_staff")) {
                return Math.max(1, Math.min(58, (99 + 58 * in.magic) / 99));
            }
        }
        if (in.spellMaxHit >= 0) {
            return in.spellMaxHit;
        }
        return Math.max(0, CombatBonuses.poweredStaffMaxHit(in.weapon, in.magic));
    }

    static int magicMax(Input in) {
        int max = magicBase(in);
        if (max == 0) {
            return 0;
        }
        if (in.special && specialWeapon(in).equals("eye_of_ayak")) {
            max = factor(max, 13, 10);
        }
        if (in.boltSpell && in.has("chaos_gauntlets")) {
            max += 3;
        }
        int baseMax = max;
        int dmg = in.magicDamageTenths;
        if (in.weapon.equals("tumekens_shadow")) {
            dmg = Math.min(1000, dmg * CombatBonuses.shadowMultiplier(false));
        }
        if (magicVoid(in) && eliteVoidRobes(in)) {
            dmg += 50;
        }
        if (smokeStaff(in) && in.standardSpell) {
            dmg += 100;
        }
        boolean mask = false;
        if (in.has("salve_amulet_ei") && in.attr("undead")) {
            dmg += 200;
        } else if (in.has("salve_amulet_i") && in.attr("undead")) {
            dmg += 150;
        } else if (avarice(in)) {
            dmg += in.forinthrySurge ? 350 : 200;
        } else if (imbuedMask(in) && in.onTask) {
            mask = true;
        }
        int prayer = 0;
        for (Prayer p : in.prayers) {
            prayer = Math.max(prayer, p.magicDamagePercent() * 10);
        }
        dmg += prayer;
        max = addFactor(max, dmg, 1000);
        if (mask) {
            max = factor(max, 23, 20);
        }
        if (in.attr("dragon")) {
            if (in.weapon.equals("dragon_hunter_lance")) {
                max = factor(max, 6, 5);
            } else if (in.weapon.equals("dragon_hunter_wand")) {
                max = factor(max, 7, 5);
            } else if (in.weapon.startsWith("dragon_hunter_crossbow")) {
                max = factor(max, 5, 4);
            }
        }
        if (revWeapon(in)) {
            max = factor(max, 3, 2);
        }
        if (in.special && specialWeapon(in).equals("accursed_sceptre")) {
            max = factor(max, 3, 2);
        }
        if (in.spellElement != null && in.spellElement.equals(in.weaknessElement)) {
            max += baseMax * in.weaknessPercent / 100;
        }
        if (in.spellElement != null && in.has("tome_of_" + in.spellElement)) {
            max = factor(max, 11, 10);
        }
        return max;
    }

    // ---------------------------------------------------------------- defence and hit chance

    /**
     * Which of a target's three ranged defences a ranged weapon rolls against (osrs-dps-calc {@code getRangedDamageType}):
     * "light" for thrown weapons (darts, knives, thrownaxes, blowpipes, Toktz-xil-ul, Morrigan's javelins...), "heavy"
     * for crossbows, ballistae and chinchompas, "mixed" (the average of the three) for salamanders, "standard" for bows.
     */
    public static String rangedDefenceKind(String weapon) {
        String w = weapon == null ? "" : weapon.startsWith("lms_") ? weapon.substring(4) : weapon;
        if (w.contains("crossbow") || w.contains("ballista") || w.endsWith("chinchompa") || w.equals("kings_barrage")) {
            return "heavy";
        }
        if (w.endsWith("salamander") || w.equals("swamp_lizard")) {
            return "mixed";
        }
        if (w.endsWith("_dart") || w.endsWith("_knife") || w.endsWith("thrownaxe") || w.endsWith("blowpipe")
                || w.startsWith("morrigans_") || w.equals("toktz_xil_ul") || w.equals("tonalztics_of_ralos")
                || w.equals("throwing_knife") || w.equals("throwing_axe") || w.equals("holy_water") || w.equals("blisterwood_stake")
                || w.equals("hunters_spear") || w.equals("sages_axe") || w.equals("mud_pie") || w.equals("spine")) {
            return "light";
        }
        return "standard";
    }

    /** Picks the ranged defence for a weapon class (the mixed average is truncated, as in the calculator). */
    public static int rangedDefence(String kind, int light, int standard, int heavy) {
        return switch (kind) {
            case "light" -> light;
            case "heavy" -> heavy;
            case "mixed" -> (light + standard + heavy) / 3;
            default -> standard;
        };
    }

    /** The defence bonus this attack rolls against (the weapon's ranged class picks among the three ranged defences). */
    public static int targetDefenceBonus(Input in) {
        if (in.type == AttackType.RANGED && in.targetRangedDefence != null) {
            int[] r = in.targetRangedDefence;
            return rangedDefence(rangedDefenceKind(in.weapon), r[0], r[1], r[2]);
        }
        return in.targetDefenceBonus;
    }

    /** The NPC's defence roll: {@code (level + 9) * (bonus + 64)} with its Magic level against magic. */
    public static long defenceRoll(Input in) {
        AttackType against = specialDefenceType(in);
        int level = (against != null ? against : in.type) == AttackType.MAGIC ? in.targetMagicLevel : in.targetDefenceLevel;
        return CombatFormulas.npcRoll(level, targetDefenceBonus(in));
    }

    public static double hitChance(Input in) {
        if (specialAlwaysHits(in)) {
            return 1.0;
        }
        long atk = attackRoll(in);
        long def = defenceRoll(in);
        if (fang(in) && in.type == AttackType.STAB) {
            return Chance.fangHitChance(atk, def);
        }
        return Chance.hitChance(atk, def);
    }
}
