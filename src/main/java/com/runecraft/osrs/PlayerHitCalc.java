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
        if (salveE(in)) {
            roll = factor(roll, 6, 5);
        } else if (salve(in)) {
            roll = factor(roll, 7, 6);
        } else if (blackMask(in) && in.onTask) {
            roll = factor(roll, 7, 6);
        }
        if (tzhaarWeapon(in) && obsidian(in)) {
            roll += base / 10;
        }
        boolean demon = in.attr("demon");
        if (demon && (in.weapon.equals("arclight") || in.weapon.equals("emberlight"))) {
            roll = addFactor(roll, 70, 100);
        }
        if (demon && (in.weapon.equals("silverlight") || in.weapon.equals("darklight"))) {
            roll = addFactor(roll, 60, 100);
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
        if (in.weapon.equals("granite_hammer") && in.attr("golem")) {
            roll = factor(roll, 13, 10);
        }
        if (in.type == AttackType.CRUSH) {
            int inq = inquisitorPieces(in);
            if (inq > 0) {
                roll = factor(roll, 200 + inq, 200);
            }
        }
        return roll;
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
        if (in.has("salve_amulet_ei") && in.attr("undead")) {
            roll = factor(roll, 6, 5);
        } else if (in.has("salve_amulet_i") && in.attr("undead")) {
            roll = factor(roll, 7, 6);
        } else if (imbuedMask(in) && in.onTask) {
            roll = factor(roll, 23, 20);
        }
        if (in.weapon.equals("twisted_bow")) {
            roll = factor(roll, CombatBonuses.twistedBowAccuracy(tbowMagic(in)), 100);
        }
        if (in.weapon.equals("dragon_hunter_crossbow") && in.attr("dragon")) {
            roll = factor(roll, 13, 10);
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
        }
        if (magicVoid(in)) {
            eff = factor(eff, 29, 20);
        }
        int base = eff * (magicAttackBonus(in) + 64);
        int roll = base;
        int additive = 0;
        boolean mask = false;
        if (in.has("salve_amulet_ei") && in.attr("undead")) {
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
            if (in.weapon.equals("dragon_hunter_crossbow")) {
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
        if (in.has("tome_of_water") && "water".equals(in.spellElement)) {
            roll = factor(roll, 6, 5);
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
            mm = new int[]{0, rangedMax(in)};
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

    static int[] meleeMax(Input in) {
        int eff = prayerStrength(in, in.strength) + 8 + in.style.forMelee().strengthBonus();
        if (meleeVoid(in)) {
            eff = factor(eff, 11, 10);
        }
        int base = (eff * (in.strengthBonus + 64) + 320) / 640;
        int max = base;
        int min = 0;
        if (salveE(in)) {
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
            max -= min;
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
        boolean dragonbane = in.weapon.equals("dragon_hunter_crossbow") && in.attr("dragon");
        if (in.has("salve_amulet_ei") && in.attr("undead")) {
            max = factor(max, 6, 5);
        } else if (in.has("salve_amulet_i") && in.attr("undead")) {
            max = factor(max, 7, 6);
        } else if (imbuedMask(in) && in.onTask) {
            int num = 23;
            if (dragonbane) {
                dragonbane = false;
                num += 5;
            }
            max = factor(max, num, 20);
        }
        if (in.weapon.equals("twisted_bow")) {
            max = factor(max, CombatBonuses.twistedBowDamage(tbowMagic(in)), 100);
        }
        if (dragonbane) {
            max = factor(max, 5, 4);
        }
        return max;
    }

    /** Base max hit of the attack's spell or powered staff (0 when neither). */
    static int magicBase(Input in) {
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
            } else if (in.weapon.equals("dragon_hunter_crossbow")) {
                max = factor(max, 5, 4);
            }
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

    /** The NPC's defence roll: {@code (level + 9) * (bonus + 64)} with its Magic level against magic. */
    public static long defenceRoll(Input in) {
        int level = in.type == AttackType.MAGIC ? in.targetMagicLevel : in.targetDefenceLevel;
        return CombatFormulas.npcRoll(level, in.targetDefenceBonus);
    }

    public static double hitChance(Input in) {
        long atk = attackRoll(in);
        long def = defenceRoll(in);
        if (fang(in) && in.type == AttackType.STAB) {
            return Chance.fangHitChance(atk, def);
        }
        return Chance.hitChance(atk, def);
    }
}
