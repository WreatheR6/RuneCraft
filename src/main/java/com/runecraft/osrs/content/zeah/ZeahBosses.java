package com.runecraft.osrs.content.zeah;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.data.Drop;

import java.util.List;

/**
 * The Moons of Peril (Blue Moon, Eclipse Moon and Blood Moon) beneath Cam Torum, the only Kourend /
 * Varlamore bosses not already provided by the bosses module (Sol Heredit, the Hueycoatl, Amoxliatl,
 * Skotizo, Sarachnis, the Alchemical Hydra and Hespori live there).
 *
 * <p>In OSRS the three moons share the Lunar Chest; here each moon rolls its own armour piece and
 * weapon so that every set can be completed. Combat stats are approximations of the wiki.
 */
public final class ZeahBosses {
    private ZeahBosses() {
    }

    /**
     * Lunar Chest (OSRS Wiki): each Moon defeated gives a 1/56 chance at one piece of its set, so 1/224 for each of its four
     * pieces. Was 1/56 per piece (four times too common).
     */
    public static final int MOON_UNIQUE_RARITY = 224;

    // ---- Blue Moon: spear and freezing weapons ----
    public static final AttackDef BLUE_SPEAR = AttackDef.of("spear thrust", AttackType.STAB, 26, 3.5, 6);
    public static final AttackDef BLUE_FREEZE = AttackDef.of("frozen weapons", AttackType.MAGIC, 18, 12, 2).special(Special.FREEZE, 4)
            .telegraph(3, "The Blue Moon raises her spear and the air turns to ice!").particle("minecraft:snowflake");
    public static final AttackDef BLUE_STORM = AttackDef.of("winter storm", AttackType.MAGIC, 30, 12, 2).special(Special.SKY_STRIKE, 0)
            .telegraph(3, "Hailstones begin to fall around you!").particle("minecraft:item_snowball");
    // ---- Eclipse Moon: shield and atlatl, pulls players into the eclipse ----
    public static final AttackDef ECLIPSE_SHIELD = AttackDef.of("shield bash", AttackType.CRUSH, 22, 3.5, 5);
    public static final AttackDef ECLIPSE_ATLATL = AttackDef.of("atlatl dart", AttackType.RANGED, 24, 12, 4).particle("minecraft:crit");
    public static final AttackDef ECLIPSE_PULL = AttackDef.of("eclipse", AttackType.MAGIC, 20, 12, 2).special(Special.PULL, 0)
            .telegraph(3, "The Eclipse Moon darkens the sky and pulls you in!").particle("minecraft:portal");
    public static final AttackDef ECLIPSE_CLONES = AttackDef.of("moonlight clones", AttackType.RANGED, 28, 12, 1)
            .special(Special.SKY_STRIKE, 0).telegraph(3, "Shadowy clones of the Eclipse Moon take aim!");
    // ---- Blood Moon: dual macuahuitl, blood rain and a jaguar ----
    public static final AttackDef BLOOD_SLASH = AttackDef.of("macuahuitl flurry", AttackType.CRUSH, 25, 3.5, 6)
            .special(Special.LIFESTEAL, 25);
    public static final AttackDef BLOOD_RAIN = AttackDef.of("blood rain", AttackType.MAGIC, 15, 12, 3).special(Special.HAZARD, 5)
            .telegraph(2, "Blood rains from above!").particle("minecraft:damage_indicator");
    public static final AttackDef BLOOD_JAGUAR = AttackDef.of("call jaguar", AttackType.CRUSH, 0, 12, 1).special(Special.SUMMON, 1)
            .message("The Blood Moon summons a blood jaguar!");

    public static List<BossDef> all() {
        return List.of(blueMoon(), eclipseMoon(), bloodMoon());
    }

    /**
     * The Lunar Chest's standard loot (OSRS Wiki, one roll out of 30 per Moon defeated, as 1/N lines; RuneCraft fights one
     * Moon at a time, so one roll). Not in RuneCraft: sun-kissed bones (3/30). Was: coins, big bones, herbs, a hard clue and
     * runes, none of which the chest gives.
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

    static BossDef blueMoon() {
        return BossDef.of("blue_moon", "Blue Moon").level(377).stats(400, 220, 220, 180, 200, 1).speed(5)
                .defenceBonus(60, 60, 30, 120, 60)
                .attack(BLUE_SPEAR).attack(BLUE_FREEZE).attack(BLUE_STORM)
                .phase(50, "The Blue Moon: The cold will claim you!", 1.15, false, BLUE_SPEAR, BLUE_FREEZE, BLUE_STORM)
                .drops(lunarChest(ZeahItems.BLUE_MOON))
                .at(ZeahPlaces.MOONS_OF_PERIL).arena(11, "ice").skin(0xA0B8E0, 0x3A5AA0, 0x2A3A6A, 0xE0E8FF)
                .size(0.9f, 2.4f, 1.3f).moveSpeed(0.26);
    }

    static BossDef eclipseMoon() {
        return BossDef.of("eclipse_moon", "Eclipse Moon").level(377).stats(400, 200, 200, 200, 180, 220).speed(5)
                .defenceBonus(80, 80, 80, 60, 30)
                .attack(ECLIPSE_SHIELD).attack(ECLIPSE_ATLATL).attack(ECLIPSE_PULL).attack(ECLIPSE_CLONES)
                .phase(50, "The Eclipse Moon: The light fades!", 1.15, false, ECLIPSE_SHIELD, ECLIPSE_ATLATL, ECLIPSE_PULL,
                        ECLIPSE_CLONES)
                .drops(lunarChest(ZeahItems.ECLIPSE_MOON))
                .at(ZeahPlaces.MOONS_OF_PERIL).arena(11, "void").skin(0xE0C080, 0xC08A2A, 0x6A4A1A, 0x2A2A2A)
                .size(0.9f, 2.4f, 1.3f).moveSpeed(0.26);
    }

    static BossDef bloodMoon() {
        return BossDef.of("blood_moon", "Blood Moon").level(377).stats(400, 240, 240, 180, 1, 1).speed(4)
                .defenceBonus(60, 60, 60, 30, 60)
                .attack(BLOOD_SLASH).attack(BLOOD_RAIN).attack(BLOOD_JAGUAR)
                .phase(50, "The Blood Moon: Your blood will feed the sun!", 1.2, true, BLOOD_SLASH, BLOOD_RAIN, BLOOD_JAGUAR)
                .minion("blood_jaguar", 1)
                .drops(lunarChest(ZeahItems.BLOOD_MOON))
                .at(ZeahPlaces.MOONS_OF_PERIL).arena(11, "jungle").skin(0xC08070, 0x8A1A1A, 0x4A1010, 0xE0C040)
                .size(0.9f, 2.4f, 1.3f).moveSpeed(0.28);
    }
}
