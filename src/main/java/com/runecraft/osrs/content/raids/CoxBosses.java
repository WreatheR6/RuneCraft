package com.runecraft.osrs.content.raids;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.MonsterDef;

import java.util.List;

/**
 * Chambers of Xeric creatures. Stats are the OSRS solo (scale 1) values where known and
 * approximations otherwise; the raid scales hitpoints by party size. Signature mechanics
 * (Tekton's anvil, Vasa's crystals, Vanguard resets, Olm's phases and specials...) are boss
 * scripts on the Minecraft side.
 */
public final class CoxBosses {
    private CoxBosses() {
    }

    private static BossDef raid(String id, String name) {
        return BossDef.of(id, name).raidOnly().category("Raids").arena(18, "crypt");
    }

    public static List<BossDef> bosses() {
        return List.of(
                raid("tekton", "Tekton").level(1244).stats(300, 390, 390, 205, 205, 1).speed(5)
                        .defenceBonus(155, 165, 105, 0, 0)
                        .attack(AttackDef.of("hammer stab", AttackType.STAB, 52, 3, 10))
                        .attack(AttackDef.of("hammer slash", AttackType.SLASH, 52, 3, 10))
                        .attack(AttackDef.of("hammer crush", AttackType.CRUSH, 52, 3, 10))
                        .size(1.1f, 2.6f, 1.6f).moveSpeed(0.22),
                raid("vasa_nistirio", "Vasa Nistirio").level(1065).stats(300, 1, 1, 175, 230, 230).speed(5)
                        .defenceBonus(170, 190, 40, 400, 40)
                        .attack(AttackDef.of("crystal boulder", AttackType.RANGED, 18, 16, 10).special(Special.AOE, 3)
                                .particle("minecraft:end_rod").message("Vasa hurls a crystal boulder!"))
                        .attack(AttackDef.of("crystal bolt", AttackType.MAGIC, 22, 16, 10).particle("minecraft:end_rod"))
                        .size(1.2f, 2.4f, 1.6f).moveSpeed(0.15),
                raid("cox_guardian", "Guardian").level(140).stats(250, 140, 140, 100, 1, 1).speed(5)
                        .defenceBonus(80, 180, -10, 999, 999)
                        .attack(AttackDef.of("stone fist", AttackType.CRUSH, 30, 3, 10))
                        .attack(AttackDef.of("ground slam", AttackType.CRUSH, 25, 4, 3).special(Special.AOE, 3)
                                .telegraph(2, "The Guardian raises its fists...").particle("minecraft:explosion"))
                        .size(1.0f, 2.2f, 1.4f).moveSpeed(0.0),
                raid("skeletal_mystic", "Skeletal Mystic").level(187).stats(160, 140, 140, 187, 140, 1).speed(4)
                        .defenceBonus(155, 155, 75, 140, 115)
                        .attack(AttackDef.of("shadow bolt", AttackType.MAGIC, 21, 14, 10).particle("minecraft:witch"))
                        .attack(AttackDef.of("staff bash", AttackType.CRUSH, 18, 2.5, 6))
                        .size(0.6f, 2.1f, 1.1f),
                raid("cox_lizardman_shaman", "Lizardman shaman").level(150).stats(190, 130, 130, 210, 130, 130).speed(4)
                        .defenceBonus(102, 160, 150, 160, 0)
                        .attack(AttackDef.of("acid spit", AttackType.RANGED, 21, 12, 10).special(Special.POISON, 10)
                                .particle("minecraft:item_slime"))
                        .attack(AttackDef.of("claw", AttackType.STAB, 19, 2.5, 6))
                        .attack(AttackDef.of("purple spawn", AttackType.MAGIC, 0, 14, 2).special(Special.SUMMON, 3)
                                .message("The shaman spawns purple creatures!"))
                        .minion("cox_lizardman_spawn", 3)
                        .size(0.8f, 2.2f, 1.2f),
                raid("small_muttadile", "Muttadile").level(250).stats(250, 150, 150, 138, 1, 150).speed(4)
                        .defenceBonus(-5, 72, 50, 60, 0)
                        .attack(AttackDef.of("bite", AttackType.STAB, 28, 2.5, 10))
                        .attack(AttackDef.of("water blast", AttackType.RANGED, 20, 12, 6).particle("minecraft:splash"))
                        .size(1.0f, 1.0f, 1.2f),
                raid("muttadile", "Muttadile").level(590).stats(250, 250, 250, 220, 250, 250).speed(4)
                        .defenceBonus(-5, 82, 60, 75, 0)
                        .attack(AttackDef.of("crushing bite", AttackType.STAB, 39, 3, 10))
                        .attack(AttackDef.of("water blast", AttackType.MAGIC, 35, 14, 6).particle("minecraft:splash"))
                        .attack(AttackDef.of("tail sweep", AttackType.CRUSH, 25, 4, 3).special(Special.KNOCKBACK, 4))
                        .size(1.4f, 1.3f, 1.8f).moveSpeed(0.22),
                raid("vanguard_melee", "Vanguard").level(1014).stats(180, 150, 150, 160, 150, 150).speed(4)
                        .defenceBonus(150, 150, 150, 20, 400)
                        .attack(AttackDef.of("blade swipe", AttackType.SLASH, 18, 2.5, 10))
                        .size(0.9f, 1.9f, 1.4f),
                raid("vanguard_ranged", "Vanguard").level(1014).stats(180, 150, 150, 160, 150, 150).speed(4)
                        .defenceBonus(55, 60, 100, 400, 300)
                        .attack(AttackDef.of("rock fall", AttackType.RANGED, 22, 18, 10).special(Special.SKY_STRIKE, 0)
                                .particle("minecraft:falling_dust"))
                        .size(0.9f, 1.9f, 1.4f),
                raid("vanguard_magic", "Vanguard").level(1014).stats(180, 150, 150, 160, 150, 150).speed(4)
                        .defenceBonus(315, 340, 400, 110, 50)
                        .attack(AttackDef.of("magic blast", AttackType.MAGIC, 22, 18, 10).special(Special.AOE, 3)
                                .particle("minecraft:witch"))
                        .size(0.9f, 1.9f, 1.4f),
                raid("vespula", "Vespula").level(1051).stats(200, 150, 150, 88, 88, 150).speed(4)
                        .defenceBonus(0, 0, 0, 70, 60).immune(AttackType.STAB, AttackType.SLASH, AttackType.CRUSH).flying()
                        .attack(AttackDef.of("sting", AttackType.RANGED, 14, 16, 10).special(Special.POISON, 12)
                                .particle("minecraft:crit"))
                        .attack(AttackDef.of("venomous sting", AttackType.RANGED, 15, 16, 4).special(Special.DRAIN_PRAYER, 10)
                                .message("Vespula's sting saps your prayer!"))
                        .minion("vespine_soldier", 2)
                        .phase(50, "Vespula screeches for her soldiers!", 1.1, true)
                        .size(1.3f, 1.5f, 1.6f).moveSpeed(0.3),
                raid("vespula_portal", "Abyssal portal").level(450).stats(250, 1, 1, 176, 176, 1).speed(6)
                        .defenceBonus(0, 0, 0, 60, 140)
                        .attack(AttackDef.of("abyssal pulse", AttackType.MAGIC, 12, 20, 10).special(Special.AOE, 6)
                                .particle("minecraft:portal"))
                        .minion("vespine_soldier", 1)
                        .size(1.4f, 2.6f, 1.0f).moveSpeed(0.0),
                raid("cox_ice_demon", "Ice demon").level(372).stats(140, 1, 1, 160, 390, 390).speed(4)
                        .defenceBonus(70, 70, 110, 40, 140)
                        .attack(AttackDef.of("ice barrage", AttackType.MAGIC, 22, 16, 10).special(Special.FREEZE, 8)
                                .particle("minecraft:snowflake"))
                        .attack(AttackDef.of("icicle", AttackType.RANGED, 20, 16, 6).particle("minecraft:snowflake"))
                        .size(1.0f, 2.4f, 1.6f).moveSpeed(0.2),
                raid("great_olm", "Great Olm").level(1043).stats(800, 250, 250, 150, 250, 250).speed(4)
                        .defenceBonus(200, 200, 200, 200, 50)
                        .attack(AttackDef.of("crystal orb", AttackType.MAGIC, 27, 30, 10).particle("minecraft:witch"))
                        .attack(AttackDef.of("crystal spike", AttackType.RANGED, 27, 30, 10).particle("minecraft:end_rod"))
                        .attack(AttackDef.of("acid spray", AttackType.RANGED, 8, 30, 3).special(Special.HAZARD, 6)
                                .particle("minecraft:item_slime").message("The Great Olm sprays acid!"))
                        .size(2.0f, 1.8f, 2.0f).moveSpeed(0.0),
                raid("olm_left_claw", "Great Olm (Left claw)").level(750).stats(600, 250, 250, 175, 175, 250).speed(6)
                        .defenceBonus(50, 50, 50, 200, 200)
                        .attack(AttackDef.of("crystal burst", AttackType.MAGIC, 30, 30, 10).special(Special.SKY_STRIKE, 0)
                                .particle("minecraft:end_rod").message("Crystals burst from the ground!"))
                        .size(1.4f, 1.3f, 2.0f).moveSpeed(0.0),
                raid("olm_right_claw", "Great Olm (Right claw)").level(549).stats(600, 250, 250, 175, 87, 250).speed(6)
                        .defenceBonus(200, 200, 200, 50, 200)
                        .attack(AttackDef.of("claw swipe", AttackType.SLASH, 30, 6, 10).special(Special.KNOCKBACK, 4))
                        .size(1.4f, 1.3f, 2.0f).moveSpeed(0.0));
    }

    public static List<MonsterDef> monsters() {
        return List.of(
                MonsterDef.of("cox_lizardman_spawn", "Spawn").level(55).stats(15, 120, 30, 10, 1, 50).speed(4)
                        .attack(AttackDef.of("explode", AttackType.CRUSH, 1, 2, 10)).model("cox_lizardman_spawn")
                        .skin(0x8A3AB0, 0x6A2A90, 0x6A2A90, 0xE0A0FF).size(0.5f, 0.6f, 1.0f).moveSpeed(0.32),
                MonsterDef.of("vespine_soldier", "Vespine soldier").level(93).stats(100, 150, 150, 40, 40, 1).speed(4)
                        .defenceBonus(0, 0, 0, 30, 0)
                        .attack(AttackDef.of("sting", AttackType.CRUSH, 14, 2.5, 10).special(Special.POISON, 6))
                        .skin(0xD0A020, 0x2A2A1A, 0x2A2A1A, 0xF0E060).size(0.7f, 1.0f, 1.0f).flying().moveSpeed(0.3),
                MonsterDef.of("deathly_ranger", "Deathly ranger").level(147).stats(120, 1, 1, 155, 155, 210).speed(4)
                        .defenceBonus(0, 0, 0, 0, 0)
                        .attack(AttackDef.of("arrow", AttackType.RANGED, 18, 14, 10).particle("minecraft:crit"))
                        .skin(0x3A5A3A, 0x2A4A2A, 0x1A2A1A, 0x6AA06A).size(0.6f, 1.95f, 1.0f).moveSpeed(0.0),
                MonsterDef.of("deathly_mage", "Deathly mage").level(147).stats(120, 1, 1, 155, 210, 1).speed(4)
                        .defenceBonus(0, 0, 0, 0, 0)
                        .attack(AttackDef.of("blast", AttackType.MAGIC, 22, 14, 10).particle("minecraft:witch"))
                        .skin(0x3A3A6A, 0x2A2A5A, 0x1A1A3A, 0x8A8AE0).size(0.6f, 1.95f, 1.0f).moveSpeed(0.0),
                // Crystal crabs room: the crabs cannot be killed; striking one makes it shimmer (puzzle logic).
                MonsterDef.of("cox_crystal_crab", "Crystal crab").level(1).stats(200, 1, 1, 1, 1, 1).speed(10).passive()
                        .attack(AttackDef.of("pinch", AttackType.CRUSH, 0, 1, 10)).model("rock_crab")
                        .skin(0x9AD8E8, 0x6AB8D0, 0x6AB8D0, 0xE0F8FF).size(0.9f, 0.7f, 1.0f).moveSpeed(0.18),
                MonsterDef.of("vasa_crystal", "Glowing crystal").level(1).stats(120, 1, 1, 100, 100, 1).speed(10).passive()
                        .defenceBonus(-5, 180, 180, 0, 0)
                        .attack(AttackDef.of("hum", AttackType.MAGIC, 0, 1, 10))
                        .size(0.9f, 1.6f, 1.2f).moveSpeed(0.0));
    }
}
