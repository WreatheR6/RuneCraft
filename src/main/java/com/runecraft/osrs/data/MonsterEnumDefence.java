package com.runecraft.osrs.data;

import com.runecraft.osrs.AttackType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Per-style defence bonuses, the light / standard / heavy ranged defences and the elemental weakness of the built-in
 * {@link Monster} creatures, which otherwise have a single {@link Monster#defenceBonus()} for every style (round 17). From
 * the OSRS Wiki DPS calculator's {@code monsters.json} (weirdgloop/osrs-dps-calc), locked by {@code MonsterEnumDefenceTest}
 * against {@code src/test/resources/osrs/combat/monster_enum_defence.tsv}. Sheep and Pig have no OSRS combat stats and keep
 * the single value.
 */
public final class MonsterEnumDefence {
    /** Defence bonuses (stab, slash, crush, magic, light / standard / heavy ranged) and weakness (null for none). */
    public record Row(int stab, int slash, int crush, int magic, int light, int standard, int heavy, String weakness, int weaknessPercent) {
        public int bonus(AttackType type) {
            return switch (type) {
                case STAB -> stab;
                case SLASH -> slash;
                case CRUSH -> crush;
                case MAGIC -> magic;
                case RANGED -> standard;
            };
        }

        public int[] ranged() {
            return new int[]{light, standard, heavy};
        }
    }

    private static final Map<Monster, Row> ROWS = new EnumMap<>(Monster.class);

    static {
        r(Monster.MAN, -21, -21, -21, -21, -21, -21, -21, null, 0);
        r(Monster.GOBLIN, -15, -15, -15, -15, -15, -15, -15, null, 0);
        r(Monster.AL_KHARID_WARRIOR, 12, 15, 10, -1, 12, 12, 12, null, 0);
        r(Monster.BARBARIAN, 0, 3, 2, -3, 2, 2, 2, null, 0);
        r(Monster.DARK_WIZARD, 0, 0, 0, 3, 0, 0, 0, null, 0);
        r(Monster.GUARD, 18, 25, 19, -4, 20, 20, 20, null, 0);
        r(Monster.HILL_GIANT, 0, 0, 0, 0, 0, 0, 0, "earth", 25);
        r(Monster.MOSS_GIANT, 0, 0, 0, 0, 0, 0, 0, "fire", 50);
        r(Monster.GHOUL, 0, 0, 0, 0, 0, 0, 0, null, 0);
        r(Monster.ICE_GIANT, 0, 3, 2, 0, 0, 0, 0, "fire", 100);
        r(Monster.LESSER_DEMON, 0, 0, 0, -10, 0, 0, 0, "water", 40);
        r(Monster.GREATER_DEMON, 0, 0, 0, -10, 0, 0, 0, "water", 40);
        r(Monster.BLACK_DEMON, 0, 0, 0, -10, 0, 0, 0, "water", 40);
        r(Monster.CHICKEN, -42, -42, -42, -42, -42, -42, -42, null, 0);
        r(Monster.COW, -21, -21, -21, -21, -21, -21, -21, null, 0);
        r(Monster.GIANT_RAT, 0, 0, 0, 0, 0, 0, 0, null, 0);
        r(Monster.SPIDER, -53, -53, -53, -53, -53, -53, -53, "fire", 50);
        r(Monster.GIANT_SPIDER, 0, 0, 0, 0, 0, 0, 0, "fire", 50);
        r(Monster.ZOMBIE, 0, 0, 0, 0, 0, 0, 0, "fire", 50);
        r(Monster.SKELETON, 5, 5, -5, 0, 5, 5, 5, "earth", 35);
        r(Monster.WOLF, 0, 0, 0, 0, 0, 0, 0, "fire", 25);
        r(Monster.VILLAGER, -21, -21, -21, -21, -21, -21, -21, null, 0);
        r(Monster.WITCH, 0, 0, 0, 3, 0, 0, 0, null, 0);
    }

    private MonsterEnumDefence() {
    }

    private static void r(Monster m, int stab, int slash, int crush, int magic, int light, int standard, int heavy, String weakness, int pct) {
        ROWS.put(m, new Row(stab, slash, crush, magic, light, standard, heavy, weakness, pct));
    }

    /** The row for a Monster, or null (Sheep, Pig). */
    public static Row row(Monster m) {
        return m == null ? null : ROWS.get(m);
    }

    /** Defence bonus of a Monster against a style: its row's, else the single {@link Monster#defenceBonus()}. */
    public static int bonus(Monster m, AttackType type) {
        Row r = row(m);
        return r != null ? r.bonus(type) : m.defenceBonus();
    }

    public static Map<Monster, Row> all() {
        return ROWS;
    }
}
