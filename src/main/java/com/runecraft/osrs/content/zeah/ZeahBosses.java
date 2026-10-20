package com.runecraft.osrs.content.zeah;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.data.Drop;

import java.util.List;

/**
 * The Moons of Peril (Blue Moon, Eclipse Moon and Blood Moon) beneath Cam Torum, the only Kourend /
 * Varlamore bosses not already provided by the bosses module (Sol Heredit, the Hueycoatl, Amoxliatl,
 * Skotizo, Sarachnis, the Alchemical Hydra and Hespori live there).
 *
 * <p>The three Moons share the Lunar Chest (the bosses module's MoonScripts records each defeat and the chest pays out
 * per Moon defeated since the last claim); their drop tables here are that chest's lines.
 */
public final class ZeahBosses {
    private ZeahBosses() {
    }

    /**
     * Lunar Chest (OSRS Wiki): each Moon defeated gives a 1/56 chance at one piece of its set, so 1/224 for each of its four
     * pieces. Was 1/56 per piece (four times too common).
     */
    public static final int MOON_UNIQUE_RARITY = 224;

    // The Moons' standard attack (OSRS Wiki "Moons of Peril/Strategies"): three typeless hits of up to 4, 8 and 20 every 6
    // ticks, a miss ending the stack; the bosses module's MoonScripts rolls them (and the Curse of the Moon, Eyatlalli's glyph
    // and each Moon's two specials). The names are what her hits report (Perilous Dancer allows only these).
    public static final AttackDef BLUE_SPEAR = AttackDef.of("spear thrust", AttackType.SLASH, 20, 4, 1);
    public static final AttackDef ECLIPSE_SHIELD = AttackDef.of("shield bash", AttackType.SLASH, 20, 4, 1);
    /** An unparried clone of her Clones special (MoonScripts). */
    public static final AttackDef ECLIPSE_CLONES = AttackDef.of("moonlight clones", AttackType.SLASH, 28, 4, 1); // approx max
    public static final AttackDef BLOOD_SLASH = AttackDef.of("macuahuitl flurry", AttackType.SLASH, 20, 4, 1);

    public static List<BossDef> all() {
        return List.of(blueMoon(), eclipseMoon(), bloodMoon());
    }

    /**
     * The Lunar Chest's standard loot (OSRS Wiki, out of 30, as 1/N lines; the chest rolls it 1, 3 or 6 times for 1, 2 or 3
     * Moons defeated, and not at all after a unique). Not in RuneCraft: sun-kissed bones (3/30). Swamp tar is 4/30 (1/7.5),
     * kept at 1/8 (whole rates only; approx).
     */
    private static final List<Drop> LUNAR_STANDARD = List.of(
            Drop.of("atlatl_dart", 72, 120, 6), Drop.of("blessed_bone_shards", 160, 179, 15), Drop.of("wyrmling_bones", 42, 54, 30),
            Drop.of("swamp_tar", 79, 119, 8), Drop.of("water_orb", 30, 45, 15), Drop.of("supercompost", 6, 12, 10),
            Drop.of("soft_clay", 15, 25, 10), Drop.of("grimy_harralander", 12, 18, 10), Drop.of("grimy_irit_leaf", 12, 18, 30),
            Drop.of("maple_seed", 1, 2, 15), Drop.of("yew_seed", 1, 1, 30));

    private static Drop[] lunarChest(List<String> set) {
        List<Drop> out = new java.util.ArrayList<>();
        for (String item : set) {
            out.add(Drop.of(item, 1, 1, MOON_UNIQUE_RARITY));
        }
        out.addAll(LUNAR_STANDARD);
        return out.toArray(new Drop[0]);
    }

    // Stats, defences and attack speed (6) come from MonsterStatsTable (osrs-dps-calc): 329, 500 hitpoints. No health
    // phases: the Moons only change through their specials.
    static BossDef blueMoon() {
        return BossDef.of("blue_moon", "Blue Moon").level(329).stats(500, 258, 100, 60, 100, 1).speed(6)
                .defenceBonus(100, 100, 0, 500, 500)
                .attack(BLUE_SPEAR)
                .drops(lunarChest(ZeahItems.BLUE_MOON))
                .at(ZeahPlaces.MOONS_OF_PERIL).arena(11, "ice").skin(0xA0B8E0, 0x3A5AA0, 0x2A3A6A, 0xE0E8FF)
                .size(0.9f, 2.4f, 1.3f).moveSpeed(0.26);
    }

    static BossDef eclipseMoon() {
        return BossDef.of("eclipse_moon", "Eclipse Moon").level(329).stats(500, 258, 100, 60, 100, 1).speed(6)
                .defenceBonus(0, 100, 100, 500, 500)
                .attack(ECLIPSE_SHIELD).attack(ECLIPSE_CLONES)
                .drops(lunarChest(ZeahItems.ECLIPSE_MOON))
                .at(ZeahPlaces.MOONS_OF_PERIL).arena(11, "void").skin(0xE0C080, 0xC08A2A, 0x6A4A1A, 0x2A2A2A)
                .size(0.9f, 2.4f, 1.3f).moveSpeed(0.26);
    }

    static BossDef bloodMoon() {
        return BossDef.of("blood_moon", "Blood Moon").level(329).stats(500, 258, 100, 60, 100, 1).speed(6)
                .defenceBonus(100, 0, 100, 500, 500)
                .attack(BLOOD_SLASH)
                .drops(lunarChest(ZeahItems.BLOOD_MOON))
                .at(ZeahPlaces.MOONS_OF_PERIL).arena(11, "jungle").skin(0xC08070, 0x8A1A1A, 0x4A1010, 0xE0C040)
                .size(0.9f, 2.4f, 1.3f).moveSpeed(0.28);
    }
}
