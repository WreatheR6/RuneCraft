package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Drop;

/** Shorthands for writing slayer monster definitions. */
final class Kit {
    private Kit() {
    }

    /** A monster with OSRS combat level and stats (hitpoints, attack, strength, defence, magic, ranged). */
    static MonsterDef m(String id, String name, int level, int hp, int att, int str, int def, int mag, int rng) {
        return MonsterDef.of(id, name).level(level).stats(hp, att, str, def, mag, rng);
    }

    static AttackDef stab(int max) {
        return AttackDef.of("melee", AttackType.STAB, max, 2.5, 10);
    }

    static AttackDef slash(int max) {
        return AttackDef.of("melee", AttackType.SLASH, max, 2.5, 10);
    }

    static AttackDef crush(int max) {
        return AttackDef.of("melee", AttackType.CRUSH, max, 2.5, 10);
    }

    static AttackDef magic(int max, String particle) {
        return AttackDef.of("magic", AttackType.MAGIC, max, 10, 10).particle(particle);
    }

    static AttackDef ranged(int max, String particle) {
        return AttackDef.of("ranged", AttackType.RANGED, max, 10, 10).particle(particle);
    }

    /** Dragonfire breath (max 50 unprotected, 10 with a shield or antifire, 0 with both). */
    static AttackDef dragonfire(int max, int weight) {
        return AttackDef.of("dragonfire", AttackType.MAGIC, max, 4, weight).special(AttackDef.Special.DRAGONFIRE, 0)
                .particle("minecraft:flame");
    }

    static Drop clue(String tier, int rarity) {
        return Drop.of("clue_scroll_" + tier, 1, 1, rarity);
    }

    static Drop coins(int min, int max, int rarity) {
        return Drop.of("coins", min, max, rarity);
    }
}
