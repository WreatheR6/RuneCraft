package com.runecraft.osrs.content.systems;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Enchanted crossbow bolts: the Enchant Crossbow Bolt spell table (level, XP, runes per bolt type, 10 bolts per cast)
 * and each (e) bolt's special effect with its activation chance, as on the OSRS wiki and in the wiki DPS calculator.
 * The dragon-tipped (e) bolts share their gem's effect. The Kandarin hard diary raises every chance by 10%
 * (multiplicatively); the zaryte crossbow strengthens the effects.
 */
public final class BoltEffects {
    public enum Effect {
        /** Opal: +10% of the Ranged level (zaryte: /9), even on a miss. */
        LUCKY_LIGHTNING,
        /** Jade: knocks the target down (players are stunned). */
        EARTHS_FURY,
        /** Pearl: +1/20 of the Ranged level (1/15 against fiery monsters), even on a miss. */
        SEA_CURSE,
        /** Topaz: drains a player's Magic. */
        DOWN_TO_EARTH,
        /** Sapphire: drains a player's Prayer, half of it going to the attacker. */
        CLEAR_MIND,
        /** Emerald: poisons the target (5 damage). */
        MAGICAL_POISON,
        /** Ruby: 20% of the target's current hitpoints (cap 100), costing the attacker 10% of theirs. */
        BLOOD_FORFEIT,
        /** Diamond: ignores accuracy and rolls up to 115% of the max hit. */
        ARMOUR_PIERCING,
        /** Dragonstone: on an accurate hit, +20% of the Ranged level; nothing against fiery monsters and dragons. */
        DRAGONS_BREATH,
        /** Onyx: on an accurate hit, rolls up to 120% of the max hit and heals 25% of the damage; nothing against undead. */
        LIFE_LEECH
    }

    /** One bolt type for Enchant Crossbow Bolt: plain bolts, the (e) result, level, XP per cast and runes per cast. */
    public record Enchant(String prefix, String name, int level, double xp, Map<String, Integer> runes, Effect effect,
                          double chance) {
        public String bolts() {
            return prefix + "_bolts";
        }

        public String enchanted() {
            return prefix + "_bolts_e";
        }

        public String dragonBolts() {
            return prefix + "_dragon_bolts";
        }

        public String dragonEnchanted() {
            return prefix + "_dragon_bolts_e";
        }
    }

    /** Bolts enchanted by one cast. */
    public static final int BOLTS_PER_CAST = 10;
    /** Kandarin hard diary: activation chance x1.1. */
    public static final double KANDARIN_FACTOR = 1.1;

    public static final List<Enchant> ENCHANTS = List.of(
            e("opal", "Opal", 4, 9, Effect.LUCKY_LIGHTNING, 0.05, "cosmic", 1, "air", 2),
            e("sapphire", "Sapphire", 7, 17.5, Effect.CLEAR_MIND, 0.05, "cosmic", 1, "water", 1, "mind", 1),
            e("jade", "Jade", 14, 19, Effect.EARTHS_FURY, 0.06, "cosmic", 1, "earth", 2),
            e("pearl", "Pearl", 24, 29, Effect.SEA_CURSE, 0.06, "cosmic", 1, "water", 2),
            e("emerald", "Emerald", 27, 37, Effect.MAGICAL_POISON, 0.55, "cosmic", 1, "air", 3, "nature", 1),
            e("topaz", "Topaz", 29, 33, Effect.DOWN_TO_EARTH, 0.04, "cosmic", 1, "fire", 2),
            e("ruby", "Ruby", 49, 59, Effect.BLOOD_FORFEIT, 0.06, "cosmic", 1, "fire", 5, "blood", 1),
            e("diamond", "Diamond", 57, 67, Effect.ARMOUR_PIERCING, 0.10, "cosmic", 1, "earth", 10, "law", 2),
            e("dragonstone", "Dragonstone", 68, 78, Effect.DRAGONS_BREATH, 0.06, "cosmic", 1, "earth", 15, "soul", 1),
            e("onyx", "Onyx", 87, 97, Effect.LIFE_LEECH, 0.11, "cosmic", 1, "fire", 20, "death", 1));

    private static final Map<String, Enchant> BY_ITEM = new LinkedHashMap<>();

    static {
        for (Enchant en : ENCHANTS) {
            BY_ITEM.put(en.enchanted(), en);
            BY_ITEM.put(en.dragonEnchanted(), en);
        }
    }

    private BoltEffects() {
    }

    private static Enchant e(String prefix, String name, int level, double xp, Effect effect, double chance, Object... runes) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < runes.length; i += 2) {
            m.put(runes[i] + "_rune", (Integer) runes[i + 1]);
        }
        return new Enchant(prefix, name, level, xp, m, effect, chance);
    }

    /** The enchantment that an (e) bolt item carries, or null for any other ammo. */
    public static Enchant ofItem(String ammoId) {
        return ammoId == null ? null : BY_ITEM.get(com.runecraft.osrs.data.EquipmentBonuses.reference(ammoId));
    }

    /** The table entry for plain (or dragon) bolts that the spell can enchant, or null. */
    public static Enchant forPlainBolts(String boltsId) {
        for (Enchant en : ENCHANTS) {
            if (en.bolts().equals(boltsId) || en.dragonBolts().equals(boltsId)) {
                return en;
            }
        }
        return null;
    }

    /** The (e) item made from plain or dragon bolts. */
    public static String enchantedOf(String boltsId) {
        Enchant en = forPlainBolts(boltsId);
        if (en == null) {
            return null;
        }
        return boltsId.equals(en.dragonBolts()) ? en.dragonEnchanted() : en.enchanted();
    }

    /** Activation chance, with the Kandarin hard diary bonus. */
    public static double chance(Enchant en, boolean kandarinHard) {
        return en.chance() * (kandarinHard ? KANDARIN_FACTOR : 1.0);
    }

    /** What a bolt shot does to its target and to the shooter. */
    public record Context(boolean accurate, int damage, int maxHit, int rangedLevel, int targetHp, int shooterHp,
                          boolean zaryte, boolean fiery, boolean dragon, boolean undead, boolean againstPlayer) {
    }

    /**
     * Result of a shot: the damage dealt, damage the shooter takes (ruby), hitpoints healed (onyx), poison to apply
     * (emerald), Magic/Prayer drained (topaz/sapphire), stun ticks (jade) and whether the effect activated.
     */
    public record Outcome(int damage, int selfDamage, int heal, int poison, int drain, int stunTicks, boolean activated) {
        static Outcome none(int damage) {
            return new Outcome(damage, 0, 0, 0, 0, 0, false);
        }
    }

    /**
     * Rolls the bolt effect for one shot (wiki / DPS calculator rules). {@code chance} already includes the Kandarin
     * diary bonus.
     */
    public static Outcome roll(Enchant en, double chance, Context c, Random random) {
        boolean immune = switch (en.effect()) {
            case DRAGONS_BREATH -> c.fiery() || c.dragon();
            case LIFE_LEECH -> c.undead();
            case DOWN_TO_EARTH, CLEAR_MIND -> !c.againstPlayer();
            default -> false;
        };
        boolean needsHit = en.effect() == Effect.DRAGONS_BREATH || en.effect() == Effect.LIFE_LEECH;
        if (immune || (needsHit && !c.accurate()) || random.nextDouble() >= chance) {
            return Outcome.none(c.damage());
        }
        int lvl = c.rangedLevel();
        boolean z = c.zaryte();
        return switch (en.effect()) {
            case LUCKY_LIGHTNING -> proc(c.damage() + lvl / (z ? 9 : 10));
            case SEA_CURSE -> {
                int div = c.fiery() ? 15 : 20;
                yield proc(c.damage() + lvl / (z ? div - 2 : div));
            }
            case DRAGONS_BREATH -> proc(c.damage() + lvl * 2 / (z ? 9 : 10));
            case ARMOUR_PIERCING -> proc(random.nextInt(c.maxHit() * (z ? 126 : 115) / 100 + 1));
            case LIFE_LEECH -> {
                int dmg = random.nextInt(c.maxHit() * (z ? 132 : 120) / 100 + 1);
                yield new Outcome(dmg, 0, dmg / 4, 0, 0, 0, true);
            }
            case BLOOD_FORFEIT -> {
                int cap = z ? 110 : 100;
                int dmg = Math.min(cap, c.targetHp() * (z ? 22 : 20) / 100);
                yield new Outcome(dmg, c.shooterHp() / 10, 0, 0, 0, 0, true);
            }
            case MAGICAL_POISON -> new Outcome(c.damage(), 0, 0, 5, 0, 0, true);
            case EARTHS_FURY -> new Outcome(c.damage(), 0, 0, 0, 0, 5, true); // approx: a 3-second knock-down
            case DOWN_TO_EARTH -> new Outcome(c.damage(), 0, 0, 0, Math.max(1, lvl / 20), 0, true); // approx drain
            case CLEAR_MIND -> new Outcome(c.damage(), 0, 0, 0, Math.max(1, lvl / 20), 0, true); // approx: 5% of Ranged
        };
    }

    private static Outcome proc(int damage) {
        return new Outcome(damage, 0, 0, 0, 0, 0, true);
    }
}
