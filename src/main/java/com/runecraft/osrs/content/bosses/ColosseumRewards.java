package com.runecraft.osrs.content.bosses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * The Fortis Colosseum's Rewards Chest (OSRS Wiki "Rewards Chest (Fortis Colosseum)"): every cleared wave adds one roll on
 * that wave's table, and the chest holds everything rolled so far. The tables below are the wiki's, line for line, as
 * weights over each wave's common denominator (snapshot: {@code src/test/resources/osrs/minigames/colosseum_rewards.tsv}).
 * From wave 4 the table includes the uniques: echo crystals (a tenth of them 2-3), the sunfire fanatic pieces (with
 * duplicate protection: a piece not owned yet is preferred) and, from wave 7, the Tonalztics of ralos. Clearing wave 12
 * also always gives Dizana's quiver; the Smol heredit roll (1/200) comes with Sol Heredit's death.
 *
 * <p>The Colosseum's items belong to the bosses module (Sol Heredit's), so the table lives here; the minigame session
 * only asks for rolls.
 */
public final class ColosseumRewards {
    private ColosseumRewards() {
    }

    public static final String SPLINTERS = "sunfire_splinters";
    public static final String QUIVER = "dizanas_quiver";
    public static final String PET = "pet_smol_heredit";
    public static final String TONALZTICS = "tonalztics_of_ralos";
    public static final String ECHO_CRYSTAL = "echo_crystal";
    public static final List<String> SUNFIRE = List.of("sunfire_fanatic_helm", "sunfire_fanatic_cuirass", "sunfire_fanatic_chausses");

    /** One line of a wave's table: weight out of the wave's denominator. */
    public record Line(String item, int min, int max, int weight) {
    }

    /** A wave's table: lines and their common denominator. */
    public record Table(int wave, int denominator, List<Line> lines) {
        public int totalWeight() {
            return lines.stream().mapToInt(Line::weight).sum();
        }
    }

    /** One item given. */
    public record Reward(String item, int count) {
    }

    private static final List<Table> TABLES = new ArrayList<>();

    // The wiki's shared groups (each line of a group has the same weight within a wave).
    private static final Object[][] A = {{"rune_platebody", 1}, {"death_rune", 150}, {"chaos_rune", 150}, {SPLINTERS, 150},
            {"cannonball", 80}, {"rune_kiteshield", 4}, {"rune_chainbody", 1}};
    private static final Object[][] B = {{"onyx_bolts", 30}, {"snapdragon_seed", 1}, {"dragon_platelegs", 1}, {SPLINTERS, 500},
            {"earth_orb", 80}, {"rune_2h_sword", 2}, {"gold_ore", 150}};
    private static final Object[][] C = {{"dragon_bolts_unf", 200}, {"ranarr_seed", 4}, {SPLINTERS, 1100}, {"dragon_arrowtips", 150},
            {"adamantite_ore", 100}, {"death_rune", 250}};
    private static final Object[][] D = {{"onyx_bolts", 50}, {"dragon_platelegs", 2}, {SPLINTERS, 1750}, {"rune_kiteshield", 5},
            {"runite_ore", 12}, {"death_rune", 300}};
    private static final Object[][] E = {{"onyx_bolts", 75}, {SPLINTERS, 2500}, {"dragon_platelegs", 3}, {"death_rune", 300},
            {"rune_warhammer", 5}};
    private static final Object[][] F = {{"onyx_bolts", 100}, {"rune_warhammer", 8}, {"dragon_platelegs", 3}, {SPLINTERS, 3500},
            {"dragon_arrowtips", 250}};
    private static final Object[][] G = {{"cannonball", 2000}, {"dragon_plateskirt", 5}, {"dragon_arrowtips", 350},
            {"onyx_bolts", 150}};
    private static final Object[][] H = {{"uncut_onyx", 1}, {"dragon_plateskirt", 5}, {"rune_2h_sword", 9}, {"runite_ore", 35}};

    private static final class Builder {
        final int wave;
        final int denominator;
        final List<Line> lines = new ArrayList<>();

        Builder(int wave, int denominator) {
            this.wave = wave;
            this.denominator = denominator;
        }

        Builder group(Object[][] group, int weight) {
            for (Object[] g : group) {
                lines.add(new Line((String) g[0], (Integer) g[1], (Integer) g[1], weight));
            }
            return this;
        }

        /** A single line (wave 1's splinters). */
        Builder line(String item, int count, int weight) {
            lines.add(new Line(item, count, count, weight));
            return this;
        }

        /** The unique lines: echo crystal (1, and 2-3), each sunfire piece and the Tonalztics (0 before wave 7). */
        Builder uniques(int echo, int echoMore, int sunfire, int tonalztics) {
            lines.add(new Line(ECHO_CRYSTAL, 1, 1, echo));
            lines.add(new Line(ECHO_CRYSTAL, 2, 3, echoMore));
            for (String piece : SUNFIRE) {
                lines.add(new Line(piece, 1, 1, sunfire));
            }
            if (tonalztics > 0) {
                lines.add(new Line(TONALZTICS, 1, 1, tonalztics));
            }
            return this;
        }

        void add() {
            TABLES.add(new Table(wave, denominator, List.copyOf(lines)));
        }
    }

    static {
        new Builder(1, 1).line(SPLINTERS, 80, 1).add();
        new Builder(2, 7).group(A, 1).add();
        new Builder(3, 70).group(A, 9).group(B, 1).add();
        new Builder(4, 43400).group(A, 5535).group(B, 615).uniques(126, 14, 70, 0).add();
        new Builder(5, 19250).group(B, 2725).uniques(63, 7, 35, 0).add();
        new Builder(6, 16800).group(B, 2375).uniques(63, 7, 35, 0).add();
        new Builder(7, 45920).group(B, 5832).group(C, 756).uniques(189, 21, 105, 35).add();
        new Builder(8, 114240).group(B, 14472).group(D, 1876).uniques(567, 63, 315, 105).add();
        new Builder(9, 21600).group(C, 3180).group(E, 424).uniques(135, 15, 75, 25).add();
        new Builder(10, 16000).group(D, 2340).group(F, 312).uniques(135, 15, 75, 25).add();
        new Builder(11, 2080).group(E, 360).group(G, 50).uniques(27, 3, 15, 5).add();
        new Builder(12, 4800).group(F, 792).group(H, 110).uniques(135, 15, 75, 25).add();
    }

    public static List<Table> tables() {
        return Collections.unmodifiableList(TABLES);
    }

    public static Table table(int wave) {
        return TABLES.get(wave - 1);
    }

    /** Whether a line is one of the chest's uniques. */
    public static boolean unique(String item) {
        return item.equals(ECHO_CRYSTAL) || item.equals(TONALZTICS) || SUNFIRE.contains(item) || item.equals(QUIVER) || item.equals(PET);
    }

    /**
     * The chest's roll for a cleared wave. {@code owned} is the sunfire pieces the player already has (collection log), for
     * the duplicate protection; wave 12 adds Dizana's quiver.
     */
    public static List<Reward> roll(int wave, Set<String> owned, Random random) {
        Table t = table(wave);
        List<Reward> out = new ArrayList<>();
        int roll = random.nextInt(t.denominator());
        for (Line l : t.lines()) {
            if (roll < l.weight()) {
                String item = l.item();
                if (SUNFIRE.contains(item)) {
                    item = sunfirePiece(item, owned, random);
                }
                out.add(new Reward(item, l.min() + (l.max() > l.min() ? random.nextInt(l.max() - l.min() + 1) : 0)));
                break;
            }
            roll -= l.weight();
        }
        if (wave >= 12) {
            out.add(new Reward(QUIVER, 1));
        }
        return out;
    }

    /** Duplicate protection: a rolled piece the player owns becomes a random missing piece, while any are missing. */
    public static String sunfirePiece(String rolled, Set<String> owned, Random random) {
        if (!owned.contains(rolled)) {
            return rolled;
        }
        List<String> missing = new ArrayList<>();
        for (String piece : SUNFIRE) {
            if (!owned.contains(piece)) {
                missing.add(piece);
            }
        }
        return missing.isEmpty() ? rolled : missing.get(random.nextInt(missing.size()));
    }

    /** Merges a reward into an accumulated chest (stackable items add up). */
    public static void add(List<Reward> chest, Reward r) {
        for (int i = 0; i < chest.size(); i++) {
            if (chest.get(i).item().equals(r.item())) {
                chest.set(i, new Reward(r.item(), chest.get(i).count() + r.count()));
                return;
            }
        }
        chest.add(r);
    }
}
