package com.runecraft.osrs.content.raids;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.MonsterDef;

import java.util.List;

/**
 * Tombs of Amascut creatures at raid level 0 for a solo raider; the raid scales hitpoints and
 * damage by raid level and party size. Stats are OSRS values where known, approximations otherwise.
 */
public final class ToaBosses {
    private ToaBosses() {
    }

    private static BossDef raid(String id, String name) {
        return BossDef.of(id, name).raidOnly().category("Raids").arena(18, "sand");
    }

    public static List<BossDef> bosses() {
        return List.of(
                raid("kephri", "Kephri").level(341).stats(150, 0, 0, 80, 125, 0).speed(6)
                        .defenceBonus(60, 300, 100, 200, 300)
                        .attack(AttackDef.of("fireball", AttackType.MAGIC, 24, 25, 10).special(Special.SKY_STRIKE, 0)
                                .particle("minecraft:flame").message("Kephri launches a fireball!"))
                        .attack(AttackDef.of("scarab bolt", AttackType.MAGIC, 14, 25, 6).particle("minecraft:flame"))
                        .minion("toa_soldier_scarab", 2)
                        .size(1.6f, 1.4f, 2.0f).moveSpeed(0.0),
                raid("akkha", "Akkha").level(337).stats(400, 100, 140, 80, 100, 100).speed(6)
                        .defenceBonus(60, 120, 120, 10, 60)
                        .attack(AttackDef.of("halberd sweep", AttackType.SLASH, 26, 3, 10))
                        .attack(AttackDef.of("arrow volley", AttackType.RANGED, 24, 20, 10).particle("minecraft:crit"))
                        .attack(AttackDef.of("shadow bolt", AttackType.MAGIC, 24, 20, 10).particle("minecraft:witch"))
                        .minion("akkhas_shadow", 1)
                        .size(0.9f, 2.4f, 1.4f).moveSpeed(0.24),
                raid("baba", "Ba-Ba").level(359).stats(380, 150, 160, 80, 100, 0).speed(6)
                        .defenceBonus(80, 160, 240, 280, 200)
                        .attack(AttackDef.of("slam", AttackType.CRUSH, 24, 3, 10))
                        .attack(AttackDef.of("shockwave", AttackType.CRUSH, 20, 5, 4).special(Special.AOE, 4)
                                .telegraph(2, "Ba-Ba pounds the ground!").particle("minecraft:explosion"))
                        .attack(AttackDef.of("falling rocks", AttackType.RANGED, 22, 25, 5).special(Special.SKY_STRIKE, 0)
                                .particle("minecraft:cloud"))
                        .minion("toa_baboon_brawler", 2)
                        .size(1.2f, 1.9f, 1.6f).moveSpeed(0.26),
                raid("zebak", "Zebak").level(371).stats(580, 250, 140, 70, 100, 120).speed(7)
                        .defenceBonus(160, 160, 260, 200, 110)
                        .attack(AttackDef.of("jaws", AttackType.STAB, 33, 3, 6))
                        .attack(AttackDef.of("acid rock", AttackType.RANGED, 30, 25, 10).particle("minecraft:item_slime"))
                        .attack(AttackDef.of("blood magic", AttackType.MAGIC, 30, 25, 10).special(Special.LIFESTEAL, 50)
                                .particle("minecraft:damage_indicator"))
                        .attack(AttackDef.of("poison spit", AttackType.RANGED, 6, 25, 3).special(Special.HAZARD, 5)
                                .particle("minecraft:item_slime"))
                        .size(1.4f, 1.5f, 2.0f).moveSpeed(0.18),
                raid("wardens_obelisk", "Obelisk").level(298).stats(260, 200, 150, 100, 100, 100).speed(4)
                        .defenceBonus(70, 70, 70, 50, 20)
                        .attack(AttackDef.of("energy siphon", AttackType.MAGIC, 18, 30, 10).particle("minecraft:end_rod"))
                        .attack(AttackDef.of("pyramid strike", AttackType.MAGIC, 22, 30, 6).special(Special.SKY_STRIKE, 0)
                                .particle("minecraft:end_rod"))
                        .size(1.2f, 3.0f, 1.0f).moveSpeed(0.0),
                raid("elidinis_warden", "Elidinis' Warden").level(544).stats(880, 150, 150, 150, 150, 150).speed(7)
                        .defenceBonus(40, 40, 20, 20, 20)
                        .attack(AttackDef.of("divine magic", AttackType.MAGIC, 40, 30, 10).telegraph(2, "Elidinis' Warden charges a magic orb...")
                                .particle("minecraft:soul_fire_flame"))
                        .attack(AttackDef.of("divine arrows", AttackType.RANGED, 40, 30, 10).telegraph(2, "Elidinis' Warden draws a bow...")
                                .particle("minecraft:crit"))
                        .attack(AttackDef.of("slam", AttackType.CRUSH, 35, 5, 4).special(Special.AOE, 4).particle("minecraft:explosion"))
                        .size(1.4f, 3.0f, 1.6f).moveSpeed(0.0),
                raid("tumekens_warden", "Tumeken's Warden").level(544).stats(880, 150, 150, 150, 150, 150).speed(8)
                        .defenceBonus(40, 40, 20, 20, 20)
                        .attack(AttackDef.of("divine magic", AttackType.MAGIC, 40, 30, 10).telegraph(2, "Tumeken's Warden charges a magic orb...")
                                .particle("minecraft:flame"))
                        .attack(AttackDef.of("divine arrows", AttackType.RANGED, 40, 30, 10).telegraph(2, "Tumeken's Warden draws a bow...")
                                .particle("minecraft:crit"))
                        .attack(AttackDef.of("slam", AttackType.CRUSH, 35, 5, 4).special(Special.AOE, 4).particle("minecraft:explosion"))
                        .phase(30, "The Warden's skulls begin to glow!", 1.25, false)
                        .size(1.4f, 3.0f, 1.6f).moveSpeed(0.0));
    }

    public static List<MonsterDef> monsters() {
        return List.of(
                MonsterDef.of("toa_scarab_swarm", "Scarab swarm").level(8).stats(10, 1, 1, 0, 1, 1).speed(4).passive().defenceBonus(-100, -100, -100, -100, -100)
                        .attack(AttackDef.of("nip", AttackType.STAB, 1, 2, 10)).model("toa_scarab")
                        .size(0.6f, 0.5f, 0.7f).moveSpeed(0.18),
                MonsterDef.of("toa_soldier_scarab", "Soldier scarab").level(89).stats(40, 75, 80, 80, 100, 95).speed(7).defenceBonus(15, 250, 30, -15, 250)
                        .attack(AttackDef.of("pincer", AttackType.CRUSH, 9, 2.5, 10)).model("toa_scarab")
                        .size(0.8f, 0.6f, 1.0f).moveSpeed(0.26),
                MonsterDef.of("toa_spitting_scarab", "Spitting scarab").level(89).stats(40, 1, 80, 80, 100, 95).speed(3).defenceBonus(15, 250, 30, 250, 125)
                        .attack(AttackDef.of("spit", AttackType.RANGED, 8, 10, 10).particle("minecraft:item_slime")).model("toa_scarab")
                        .size(0.8f, 0.6f, 1.0f).moveSpeed(0.22),
                MonsterDef.of("toa_arcane_scarab", "Arcane scarab").level(89).stats(40, 75, 80, 80, 100, 95).speed(6).defenceBonus(15, 250, 30, 75, 50)
                        .attack(AttackDef.of("arcane bolt", AttackType.MAGIC, 8, 10, 10).particle("minecraft:witch")).model("toa_scarab")
                        .size(0.8f, 0.6f, 1.0f).moveSpeed(0.22),
                MonsterDef.of("akkhas_shadow", "Akkha's Shadow").level(108).stats(70, 100, 140, 30, 100, 100).speed(6).defenceBonus(60, 120, 120, 10, 60)
                        .attack(AttackDef.of("shadow strike", AttackType.MAGIC, 15, 12, 10).particle("minecraft:squid_ink"))
                        .attack(AttackDef.of("shadow slash", AttackType.SLASH, 15, 2.5, 10))
                        .model("akkha").size(0.9f, 2.4f, 1.2f).moveSpeed(0.26),
                MonsterDef.of("toa_baboon_brawler", "Baboon brawler").level(56).stats(25, 40, 40, 12, 40, 40).speed(4).defenceBonus(900, 900, 900, -60, 900)
                        .attack(AttackDef.of("punch", AttackType.CRUSH, 8, 2.5, 10)).model("toa_baboon")
                        .size(0.7f, 1.1f, 1.0f).moveSpeed(0.3),
                MonsterDef.of("toa_baboon_thrower", "Baboon thrower").level(56).stats(30, 40, 40, 12, 40, 40).speed(4).defenceBonus(-50, -50, -50, 900, 900)
                        .attack(AttackDef.of("rock throw", AttackType.RANGED, 7, 10, 10)).model("toa_baboon")
                        .size(0.7f, 1.1f, 1.0f).moveSpeed(0.26),
                MonsterDef.of("toa_baboon_mage", "Baboon mage").level(56).stats(20, 40, 40, 12, 40, 40).speed(4).defenceBonus(900, 900, 900, 900, -50)
                        .attack(AttackDef.of("blast", AttackType.MAGIC, 7, 10, 10).particle("minecraft:witch")).model("toa_baboon")
                        .size(0.7f, 1.1f, 1.0f).moveSpeed(0.26),
                MonsterDef.of("baba_boulder", "Boulder").level(1).stats(40, 1, 1, 1, 1, 1).speed(4).passive()
                        .attack(AttackDef.of("roll", AttackType.CRUSH, 1, 1.5, 10))
                        .size(1.0f, 1.0f, 1.2f).moveSpeed(0.22));
    }
}
