package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.data.Drop;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Scurrius in the public room (OSRS Wiki "Scurrius", Group version): fought by a group he is stronger than in a private
 * instance: 1,500 hitpoints instead of 500, combat level 250, Defence/Magic/Ranged 100, and magic 14, ranged 13 and rockfall
 * 24 max hits (melee stays 13). Only the player who dealt the most damage (the MVP) rolls his regular table (and the pet and
 * spine); everyone else rolls the smaller non-MVP table. Here the group version is fought when he is summoned with another
 * player in the lair.
 */
public final class ScurriusGroup {
    private ScurriusGroup() {
    }

    public static final String BOSS = "scurrius";
    /** The {@link BossVariant#kind()} of the group version. */
    public static final String GROUP = "group";
    public static final int HITPOINTS = 1500;
    public static final int COMBAT = 250;
    public static final int ATTACK = 300;
    public static final int STRENGTH = 100;
    public static final int DEFENCE = 100;
    public static final int MAGIC = 100;
    public static final int RANGED = 100;
    /** Max hits by attack name (ClassicBosses' Scurrius attacks). */
    public static final Map<String, Integer> MAX_HITS = Map.of("bite", 13, "rat_magic", 14, "flying_rats", 13, "falling_ceiling", 24);

    /** The non-MVP table (wiki /33 each roll; big bones always). The spine goes to one player only: the MVP's roll. */
    public static final List<Drop> NON_MVP = List.of(Drop.always("big_bones"),
            Drop.of("adamant_arrow", 20, 35, 11), Drop.of("rune_arrow", 20, 35, 11), Drop.of("chaos_rune", 40, 70, 11),
            Drop.of("death_rune", 30, 50, 11), Drop.of("law_rune", 5, 15, 11),
            Drop.of("trout", 1, 3, 11), Drop.of("tuna", 1, 3, 11), Drop.of("lobster", 1, 3, 11), Drop.of("shark", 1, 1, 33),
            Drop.of("ranging_potion(1)", 1, 1, 11), Drop.of("super_strength(1)", 1, 1, 11), Drop.of("cheese", 1, 1, 33));

    /** Whether a summon starts the group version: two or more players in the lair. */
    public static boolean group(int playersInLair) {
        return playersInLair >= 2;
    }

    /** The group version of Scurrius ({@code scurrius} is his solo BossDef); its kills count as Scurrius kills. */
    public static BossVariant variant(BossDef scurrius) {
        return new BossVariant(scurrius.id, GROUP, 1, scurrius.name + " (group)", scurrius.id, COMBAT, HITPOINTS, ATTACK, STRENGTH,
                DEFENCE, MAGIC, RANGED, 0, MAX_HITS, 1.0, scurrius.drops, -1);
    }

    /** The MVP: the player who dealt the most damage (null when nobody did). */
    public static UUID mvp(Map<UUID, Integer> damage) {
        UUID best = null;
        int most = 0;
        for (Map.Entry<UUID, Integer> e : damage.entrySet()) {
            if (e.getValue() > most) {
                most = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }
}
