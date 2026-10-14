package com.runecraft.osrs.content.raids;

import com.runecraft.osrs.AttackType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The Theatre of Blood's Nylocas room (OSRS Wiki, "Theatre of Blood/Strategies/Nylocas" and "Nylocas Vasilias"):
 * 31 fixed waves of nylocas from the east, south and west lanes that try to break the room's four supports, then the
 * Nylocas Vasilias; in Hard mode a Nylocas Prinkipas joins at waves 10, 20 and 30. Diffed against
 * {@code src/test/resources/osrs/raids/nylocas_waves.tsv}.
 */
public final class TobNylocas {
    private TobNylocas() {
    }

    /** A nylocas breed: the only combat style that harms it. */
    public enum Breed {
        ISCHYROS("nylocas_ischyros", "melee"),
        TOXOBOLOS("nylocas_toxobolos", "range"),
        HAGIOS("nylocas_hagios", "mage");

        public final String creatureId;
        public final String token;

        Breed(String creatureId, String token) {
            this.creatureId = creatureId;
            this.token = token;
        }

        /** Whether an attack of this type can damage the breed (melee, ranged or magic). */
        public boolean weakTo(AttackType type) {
            return switch (this) {
                case ISCHYROS -> type.isMelee();
                case TOXOBOLOS -> type == AttackType.RANGED;
                case HAGIOS -> type == AttackType.MAGIC;
            };
        }

        public static Breed of(String token) {
            for (Breed b : values()) {
                if (b.token.equals(token)) {
                    return b;
                }
            }
            throw new IllegalArgumentException(token);
        }

        public static Breed byCreature(String creatureId) {
            for (Breed b : values()) {
                if (b.creatureId.equals(creatureId)) {
                    return b;
                }
            }
            return null;
        }
    }

    /** One nylocas of a wave: breed, big (2x2, splits into two smalls) and aggressive (attacks the team, not the pillars). */
    public record Nylo(Breed breed, boolean big, boolean aggressive) {
        public String token() {
            String t = big ? breed.token.toUpperCase(java.util.Locale.ROOT) : breed.token;
            return aggressive ? t + "!" : t;
        }
    }

    /** The lanes nylocas walk in from. */
    public enum Lane { EAST, SOUTH, WEST }

    /** A wave: the nylocas of each lane (east, south, west). */
    public record Wave(int number, List<List<Nylo>> lanes) {
        public List<Nylo> lane(Lane lane) {
            return lanes.get(lane.ordinal());
        }

        public int size() {
            return lanes.stream().mapToInt(List::size).sum();
        }
    }

    public static final int WAVES = 31;
    /** Hard mode: a Nylocas Prinkipas drops in with these waves. */
    public static final int[] PRINKIPAS_WAVES = {10, 20, 30};
    /** Hitpoints (5-man; the raid scales them like other ToB creatures) and max hits of small and big nylocas. */
    public static final int SMALL_HP = 11;
    public static final int BIG_HP = 22;
    public static final int SMALL_MAX_HIT = 17;
    public static final int BIG_MAX_HIT = 24;
    /** Nylocas self-destruct 30 seconds after spawning, next to them: up to 18 (small) or 21 (big). */
    public static final int LIFETIME_TICKS = 50;
    public static final int SMALL_EXPLOSION = 18;
    public static final int BIG_EXPLOSION = 21;
    /** A collapsing support drops rocks on the whole team for up to 50; all four collapsing kills everyone. */
    public static final int PILLAR_COLLAPSE_MAX_HIT = 50;
    public static final int PILLARS = 4;
    /** Support hitpoints. // approx: the wiki only shows the combined health bar. */
    public static final int PILLAR_HP = 100;
    /** Game ticks between waves. // approx: the wiki says only the spawn interval varies. */
    public static final int WAVE_INTERVAL_TICKS = 4;
    /** Vasilias and Prinkipas change form every 10 ticks, always to one of the other two styles. */
    public static final int FORM_TICKS = 10;
    /** Nylocas Vasilias max hit (normal mode; Hard mode 105 via {@link RaidScaling#tobHardMaxHit}). */
    public static final int VASILIAS_MAX_HIT = 70;
    public static final int PRINKIPAS_MAX_HIT = 38;

    /** Waves stall while this many nylocas are alive: 12 before wave 20, 24 from wave 20 on. */
    public static int cap(int nextWave) {
        return nextWave < 20 ? 12 : 24;
    }

    public static boolean prinkipasWave(int wave) {
        for (int w : PRINKIPAS_WAVES) {
            if (w == wave) {
                return true;
            }
        }
        return false;
    }

    /** Support damage from one nylocas attack. // approx */
    public static int pillarDamage(boolean big, Random rnd) {
        return rnd.nextInt(big ? 5 : 3);
    }

    /** A Vasilias/Prinkipas form change: always one of the other two forms (0 melee, 1 ranged, 2 magic). */
    public static int nextForm(int form, Random rnd) {
        return (form + 1 + rnd.nextInt(2)) % 3;
    }

    /** Whether an attack type matches a Vasilias/Prinkipas form (0 melee, 1 ranged, 2 magic). */
    public static boolean formWeakTo(int form, AttackType type) {
        return switch (form) {
            case 0 -> type.isMelee();
            case 1 -> type == AttackType.RANGED;
            default -> type == AttackType.MAGIC;
        };
    }

    // Wiki wave chart: lanes east | south | west; melee/range/mage, UPPER = big, ! = aggressive, - = none.
    private static final String[] DATA = {
            "melee|mage|range!",
            "range|melee!|mage",
            "mage!|range|melee",
            "melee|MAGE|range",
            "mage|melee|RANGE",
            "MELEE|range|mage",
            "MELEE|mage+RANGE!|-",
            "range|melee|MAGE!",
            "mage|-|RANGE!+melee",
            "range!+RANGE|range+range|range+range!",
            "mage!+mage|mage+mage|MAGE!",
            "melee!+melee|MELEE|melee+melee!",
            "MELEE!|range+melee|range+mage!",
            "RANGE!|mage+range|mage+melee!",
            "mage!+range|MAGE|melee+range!",
            "range|range|range",
            "MAGE|MAGE|MAGE",
            "RANGE|RANGE|RANGE!",
            "MAGE!|MAGE|MAGE",
            "MELEE|MELEE!|MELEE",
            "range+range!|melee+melee!|mage+range",
            "MELEE!|melee+melee|MELEE",
            "MELEE|MELEE!|melee+range",
            "MELEE!|MAGE!|MELEE!",
            "MAGE|RANGE|MELEE!",
            "MAGE|MELEE|MAGE!",
            "MAGE|RANGE!|MAGE",
            "mage+melee|melee+range|melee+mage!",
            "melee+mage!|MELEE|mage!+range",
            "MAGE!|melee+melee|RANGE",
            "mage+range|range+range|mage+range",
    };

    private static final List<Wave> WAVE_LIST = parse();

    private static List<Wave> parse() {
        List<Wave> list = new ArrayList<>();
        for (int i = 0; i < DATA.length; i++) {
            String[] lanes = DATA[i].split("\\|", -1);
            List<List<Nylo>> parsed = new ArrayList<>();
            for (String lane : lanes) {
                parsed.add(parseLane(lane));
            }
            list.add(new Wave(i + 1, Collections.unmodifiableList(parsed)));
        }
        return Collections.unmodifiableList(list);
    }

    /** Parses one lane cell ("melee+RANGE!", "-"). */
    public static List<Nylo> parseLane(String cell) {
        List<Nylo> out = new ArrayList<>();
        if (cell.isBlank() || cell.equals("-")) {
            return out;
        }
        for (String tok : cell.split("\\+")) {
            boolean aggressive = tok.endsWith("!");
            String word = aggressive ? tok.substring(0, tok.length() - 1) : tok;
            boolean big = word.equals(word.toUpperCase(java.util.Locale.ROOT));
            out.add(new Nylo(Breed.of(word.toLowerCase(java.util.Locale.ROOT)), big, aggressive));
        }
        return out;
    }

    public static List<Wave> waves() {
        return WAVE_LIST;
    }

    public static Wave wave(int number) {
        return WAVE_LIST.get(number - 1);
    }
}
