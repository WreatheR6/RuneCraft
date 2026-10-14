package com.runecraft.osrs.content.raids;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.MonsterDef;

import java.util.List;

/**
 * Theatre of Blood creatures (normal mode, 5-man hitpoints; the raid scales them down for smaller
 * parties as OSRS does). Stats are OSRS values where known, approximations otherwise.
 */
public final class TobBosses {
    private TobBosses() {
    }

    private static BossDef raid(String id, String name) {
        return BossDef.of(id, name).raidOnly().category("Raids").arena(18, "crypt");
    }

    public static List<BossDef> bosses() {
        return List.of(
                raid("the_maiden_of_sugadinti", "The Maiden of Sugadinti").level(940).stats(3500, 350, 350, 200, 350, 350).speed(10)
                        .defenceBonus(0, 0, 0, 0, 0)
                        .attack(AttackDef.of("blood splat", AttackType.MAGIC, 36, 20, 10).special(Special.SKY_STRIKE, 0)
                                .particle("minecraft:damage_indicator"))
                        .attack(AttackDef.of("blood throw", AttackType.MAGIC, 36, 20, 8).particle("minecraft:damage_indicator"))
                        .attack(AttackDef.of("tendril lash", AttackType.CRUSH, 30, 4, 4))
                        .size(1.6f, 2.2f, 1.8f).moveSpeed(0.0),
                raid("pestilent_bloat", "Pestilent Bloat").level(870).stats(2000, 250, 340, 100, 150, 180).speed(4)
                        .defenceBonus(40, 20, 40, 600, 800)
                        .attack(AttackDef.of("flies", AttackType.RANGED, 20, 20, 10).special(Special.PRAYER_PIERCING, 0)
                                .particle("minecraft:smoke"))
                        .size(1.6f, 1.6f, 2.0f).moveSpeed(0.24),
                raid("nylocas_vasilias", "Nylocas Vasilias").level(800).stats(2500, 400, 350, 50, 50, 350).speed(4)
                        .defenceBonus(0, 0, 0, 0, 0)
                        .attack(AttackDef.of("bite", AttackType.STAB, 70, 3, 10))
                        .size(1.5f, 1.3f, 2.0f).moveSpeed(0.22),
                // Hard mode only: drops in with waves 10, 20 and 30 and changes style like the Vasilias (OSRS wiki).
                raid("nylocas_prinkipas", "Nylocas Prinkipas").level(400).stats(400, 200, 175, 25, 25, 175).speed(4)
                        .defenceBonus(0, 0, 0, 0, 0)
                        .attack(AttackDef.of("bite", AttackType.STAB, 38, 3, 10))
                        .model("nylocas_vasilias").size(1.2f, 1.0f, 1.6f).moveSpeed(0.22),
                raid("sotetseg", "Sotetseg").level(995).stats(4000, 250, 250, 200, 250, 250).speed(5)
                        .defenceBonus(70, 70, 70, 30, 150)
                        .attack(AttackDef.of("red orb", AttackType.MAGIC, 50, 25, 10).particle("minecraft:flame"))
                        .attack(AttackDef.of("green orb", AttackType.RANGED, 50, 25, 10).particle("minecraft:happy_villager"))
                        .attack(AttackDef.of("melee slam", AttackType.CRUSH, 45, 4, 8))
                        .size(1.6f, 2.0f, 1.8f).moveSpeed(0.0),
                raid("xarpus", "Xarpus").level(960).stats(5000, 1, 1, 250, 220, 100).speed(4)
                        .defenceBonus(0, 0, 0, 0, 160)
                        .attack(AttackDef.of("acid spit", AttackType.RANGED, 35, 25, 10).special(Special.HAZARD, 8)
                                .particle("minecraft:item_slime"))
                        .size(1.4f, 2.4f, 1.8f).moveSpeed(0.0),
                raid("verzik_vitur", "Verzik Vitur").level(1040).stats(2000, 400, 400, 20, 400, 400).speed(14)
                        .defenceBonus(20, 20, 20, 20, 20)
                        .attack(AttackDef.of("electric blast", AttackType.MAGIC, 137, 40, 10)
                                .particle("minecraft:electric_spark").message("Verzik charges a blast of energy!"))
                        .size(1.2f, 2.2f, 1.6f).moveSpeed(0.0),
                raid("verzik_vitur_p2", "Verzik Vitur").level(1265).stats(3500, 400, 400, 200, 400, 400).speed(4)
                        .defenceBonus(100, 60, 100, 70, 250)
                        .attack(AttackDef.of("bouncing bomb", AttackType.RANGED, 40, 25, 10).particle("minecraft:flame"))
                        .attack(AttackDef.of("electric bolt", AttackType.MAGIC, 40, 25, 10).particle("minecraft:electric_spark"))
                        .attack(AttackDef.of("bounce", AttackType.CRUSH, 30, 3, 6).special(Special.KNOCKBACK, 6))
                        .minion("nylocas_matomenos", 2)
                        .size(1.4f, 2.2f, 1.8f).moveSpeed(0.22),
                raid("verzik_vitur_p3", "Verzik Vitur").level(1520).stats(3500, 400, 400, 150, 300, 300).speed(7)
                        .defenceBonus(70, 30, 70, 100, 230)
                        .attack(AttackDef.of("spider bite", AttackType.STAB, 34, 3, 10))
                        .attack(AttackDef.of("ranged spit", AttackType.RANGED, 34, 25, 10).particle("minecraft:crit"))
                        .attack(AttackDef.of("magic web", AttackType.MAGIC, 34, 25, 10).particle("minecraft:witch"))
                        .phase(20, "Verzik Vitur: I'm not finished with you just yet!", 1.15, false)
                        .size(1.8f, 1.6f, 2.0f).moveSpeed(0.24));
    }

    public static List<MonsterDef> monsters() {
        // The wave nylocas are passive: the Nylocas room sends most at the supports and sets the aggressive ones on the team.
        return List.of(
                MonsterDef.of("nylocas_matomenos", "Nylocas Matomenos").level(115).stats(100, 100, 100, 100, 100, 100).speed(4).passive()
                        .attack(AttackDef.of("nip", AttackType.STAB, 4, 2.5, 10)).model("nylocas_matomenos")
                        .size(0.8f, 0.7f, 1.0f).moveSpeed(0.2),
                MonsterDef.of("blood_spawn", "Blood spawn").level(55).stats(120, 1, 1, 0, 0, 1).speed(4).passive()
                        .attack(AttackDef.of("splash", AttackType.MAGIC, 3, 4, 10))
                        .size(0.7f, 0.9f, 1.0f).moveSpeed(0.22),
                MonsterDef.of("nylocas_ischyros", "Nylocas Ischyros").level(162).stats(11, 200, 160, 1, 1, 200).speed(3).passive()
                        .immune(AttackType.MAGIC, AttackType.RANGED)
                        .attack(AttackDef.of("bite", AttackType.STAB, 17, 2.5, 10))
                        .size(0.8f, 0.7f, 1.0f).moveSpeed(0.24),
                MonsterDef.of("nylocas_toxobolos", "Nylocas Toxobolos").level(162).stats(11, 200, 160, 1, 1, 200).speed(3).passive()
                        .immune(AttackType.STAB, AttackType.SLASH, AttackType.CRUSH, AttackType.MAGIC)
                        .attack(AttackDef.of("spit", AttackType.RANGED, 17, 10, 10).particle("minecraft:crit"))
                        .size(0.8f, 0.7f, 1.0f).moveSpeed(0.24),
                MonsterDef.of("nylocas_hagios", "Nylocas Hagios").level(162).stats(11, 200, 160, 1, 1, 200).speed(3).passive()
                        .immune(AttackType.STAB, AttackType.SLASH, AttackType.CRUSH, AttackType.RANGED)
                        .attack(AttackDef.of("blast", AttackType.MAGIC, 17, 10, 10).particle("minecraft:witch"))
                        .size(0.8f, 0.7f, 1.0f).moveSpeed(0.24),
                MonsterDef.of("nylocas_athanatos", "Nylocas Athanatos").level(107).stats(180, 1, 1, 50, 50, 1).speed(4).passive()
                        .attack(AttackDef.of("nip", AttackType.STAB, 4, 2.5, 10))
                        .size(1.0f, 0.9f, 1.2f).moveSpeed(0.14));
    }
}
