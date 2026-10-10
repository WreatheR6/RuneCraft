package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Drop;

import java.util.List;

/**
 * Creatures spawned by bosses (never naturally). Stationary objects that OSRS represents as NPCs
 * (Skotizo's altars, Nightmare totems, Sire's respiratory systems, Hespori's flowers) are passive
 * monsters with a harmless attack; their boss scripts drive the real mechanic.
 */
public final class BossMinions {
    private BossMinions() {
    }

    /** A harmless placeholder attack for passive objects. */
    static AttackDef inert() {
        return AttackDef.of("inert", AttackType.MAGIC, 0, 0.5, 1);
    }

    public static List<MonsterDef> all() {
        return List.of(
                // Zulrah
                MonsterDef.of("snakeling", "Snakeling").level(90).stats(1, 85, 85, 50, 85, 1).speed(3)
                        .attack(AttackDef.of("bite", AttackType.STAB, 15, 2.5, 5).special(Special.VENOM, 0))
                        .attack(AttackDef.of("spit", AttackType.MAGIC, 15, 8, 5).special(Special.VENOM, 0)
                                .particle("minecraft:sneeze"))
                        .poisonous().size(0.5f, 0.6f, 1.0f).moveSpeed(0.3),
                // Vorkath
                MonsterDef.of("zombified_spawn", "Zombified spawn").level(64).stats(38, 1, 1, 1, 1, 1).speed(4)
                        .attack(AttackDef.of("explode", AttackType.CRUSH, 60, 1.5, 1).special(Special.AOE, 2)
                                .message("The zombified spawn explodes!"))
                        .size(0.6f, 1.2f, 1.0f).moveSpeed(0.16),
                // Kraken
                MonsterDef.of("enormous_tentacle", "Enormous tentacle").level(112).stats(120, 1, 1, 70, 150, 1).speed(4)
                        .defenceBonus(0, 0, 0, 0, 270).immune(AttackType.STAB, AttackType.SLASH, AttackType.CRUSH)
                        .attack(AttackDef.of("water_splash", AttackType.MAGIC, 4, 12, 1).particle("minecraft:splash"))
                        .size(0.8f, 2.4f, 1.0f).moveSpeed(0.0).slayer(87, "kraken"),
                // Cerberus' summoned souls (ranged, magic, melee); the Cerberus script performs their attacks.
                soul("summoned_soul_ranged", "Summoned Soul (ranged)"),
                soul("summoned_soul_magic", "Summoned Soul (magic)"),
                soul("summoned_soul_melee", "Summoned Soul (melee)"),
                // Abyssal Sire
                MonsterDef.of("respiratory_system", "Respiratory system").level(1).stats(100, 1, 1, 30, 1, 1)
                        .attack(inert()).passive().size(1.0f, 1.4f, 1.0f).moveSpeed(0.0).slayer(85, "abyssal_demons"),
                MonsterDef.of("abyssal_spawn", "Spawn").level(60).stats(30, 50, 50, 30, 1, 1).speed(4)
                        .attack(AttackDef.of("lash", AttackType.CRUSH, 8, 2.5, 1).special(Special.POISON, 4))
                        .size(0.6f, 0.7f, 1.0f).moveSpeed(0.3),
                MonsterDef.of("abyssal_scion", "Scion").level(100).stats(50, 90, 90, 60, 90, 90).speed(4)
                        .attack(AttackDef.of("stab", AttackType.STAB, 20, 2.5, 4))
                        .attack(AttackDef.of("bolt", AttackType.MAGIC, 20, 9, 3))
                        .attack(AttackDef.of("spine", AttackType.RANGED, 20, 9, 3))
                        .size(0.8f, 1.6f, 1.0f).moveSpeed(0.26),
                // Corporeal Beast
                MonsterDef.of("dark_energy_core", "Dark energy core").level(75).stats(25, 1, 1, 1, 1, 1).speed(2)
                        .attack(AttackDef.of("leech", AttackType.CRUSH, 13, 2.0, 1).special(Special.LIFESTEAL, 100)
                                .particle("minecraft:soul"))
                        .size(0.6f, 0.6f, 1.0f).moveSpeed(0.3).flying(),
                // Skotizo
                MonsterDef.of("awakened_altar", "Awakened altar").level(1).stats(100, 1, 1, 30, 1, 1)
                        .attack(inert()).passive().size(1.0f, 1.2f, 1.0f).moveSpeed(0.0),
                MonsterDef.of("reanimated_demon_spawn", "Reanimated demon spawn").level(87).stats(87, 70, 70, 60, 1, 1).speed(4)
                        .attack(AttackDef.of("claw", AttackType.SLASH, 8, 2.5, 1))
                        .skin(0x5A2A2A, 0x3A1A1A, 0x2A1010, 0xA02020).size(0.6f, 1.6f, 1.0f).moveSpeed(0.25),
                // Hespori
                MonsterDef.of("hesporis_flower", "Flower").level(1).stats(1, 1, 1, 1, 1, 1)
                        .attack(inert()).passive().size(0.7f, 1.0f, 1.0f).moveSpeed(0.0),
                // Sarachnis
                MonsterDef.of("spawn_of_sarachnis", "Spawn of Sarachnis").level(92).stats(25, 80, 80, 40, 80, 80).speed(4)
                        .attack(AttackDef.of("bite", AttackType.STAB, 12, 2.5, 4).special(Special.POISON, 6))
                        .attack(AttackDef.of("venom_spit", AttackType.RANGED, 12, 8, 3).particle("minecraft:sneeze"))
                        .attack(AttackDef.of("web_magic", AttackType.MAGIC, 12, 8, 3))
                        .size(0.7f, 0.5f, 1.0f).moveSpeed(0.28),
                // Crystalline / Corrupted Hunllef tornadoes (driven by the Hunllef script)
                MonsterDef.of("hunllef_tornado", "Tornado").level(1).stats(1, 1, 1, 1, 1, 1)
                        .attack(inert()).passive().immune(AttackType.values())
                        .size(0.7f, 2.0f, 1.0f).moveSpeed(0.0),
                // The Nightmare
                MonsterDef.of("nightmare_totem", "Totem").level(1).stats(50, 1, 1, 1, 1, 1)
                        .attack(inert()).passive().size(1.0f, 2.4f, 1.0f).moveSpeed(0.0),
                MonsterDef.of("nightmare_husk", "Husk").level(48).stats(20, 40, 40, 20, 60, 60).speed(4)
                        .attack(AttackDef.of("grasp", AttackType.MAGIC, 6, 8, 1))
                        .skin(0x6A6A5A, 0x4A4A3A, 0x3A3A2A, 0x9A4AC0).size(0.6f, 1.8f, 1.0f).moveSpeed(0.1),
                MonsterDef.of("nightmare_parasite", "Parasite").level(52).stats(40, 60, 60, 20, 60, 1).speed(4)
                        .attack(AttackDef.of("bite", AttackType.STAB, 10, 2.5, 1).special(Special.LIFESTEAL, 50))
                        .size(0.6f, 0.5f, 1.0f).moveSpeed(0.3),
                // Scurrius
                MonsterDef.of("scurrius_giant_rat", "Giant rat").level(18).stats(10, 15, 15, 10, 1, 1).speed(4)
                        .attack(AttackDef.of("bite", AttackType.STAB, 3, 2.5, 1))
                        .size(0.7f, 0.5f, 1.0f).moveSpeed(0.3),
                // Araxxor's araxytes (hatched from eggs)
                araxyte("acidic_araxyte", "Acidic araxyte", AttackDef.of("acid_spit", AttackType.RANGED, 18, 8, 1)
                        .special(Special.HAZARD, 4).particle("minecraft:sneeze")),
                araxyte("mirrorback_araxyte", "Mirrorback araxyte", AttackDef.of("bite", AttackType.STAB, 16, 2.5, 1)),
                araxyte("ruptura_araxyte", "Ruptura araxyte", AttackDef.of("rupture", AttackType.CRUSH, 25, 2.0, 1)
                        .special(Special.AOE, 2).particle("minecraft:explosion")),
                // Zalcano
                MonsterDef.of("zalcano_golem", "Golem").level(1).stats(150, 80, 80, 60, 1, 1).speed(5)
                        .attack(AttackDef.of("slam", AttackType.CRUSH, 20, 2.5, 1))
                        .skin(0x7A6A5A, 0x5A4A3A, 0x4A3A2A, 0xE07020).size(0.9f, 2.0f, 1.2f).moveSpeed(0.12),
                // The Whisperer's shadow tentacles
                MonsterDef.of("whisperer_tentacle", "Tentacle").level(1).stats(40, 1, 1, 20, 1, 1).speed(4)
                        .attack(AttackDef.of("lash", AttackType.MAGIC, 10, 6, 1).particle("minecraft:squid_ink"))
                        .size(0.6f, 2.0f, 1.0f).moveSpeed(0.0),
                // Royal Titans' elementals
                MonsterDef.of("titan_ice_elemental", "Ice elemental").level(50).stats(40, 40, 40, 40, 40, 1).speed(4)
                        .attack(AttackDef.of("frost", AttackType.MAGIC, 8, 8, 1).special(Special.FREEZE, 3)
                                .particle("minecraft:snowflake"))
                        .skin(0xA0D0F0, 0x80B0E0, 0x6090C0, 0xFFFFFF).size(0.6f, 1.6f, 1.0f).moveSpeed(0.2),
                MonsterDef.of("titan_fire_elemental", "Fire elemental").level(50).stats(40, 40, 40, 40, 40, 1).speed(4)
                        .attack(AttackDef.of("flame", AttackType.MAGIC, 10, 8, 1).particle("minecraft:flame"))
                        .skin(0xE07020, 0xC04010, 0x802010, 0xFFD040).size(0.6f, 1.6f, 1.0f).moveSpeed(0.2))
                .stream().map(BossMinions::noDrops).toList();
    }

    private static MonsterDef soul(String id, String name) {
        return MonsterDef.of(id, name).level(1).stats(1, 1, 1, 1, 1, 1).attack(inert()).passive()
                .immune(AttackType.values()).flying().size(0.6f, 1.8f, 1.0f).moveSpeed(0.0);
    }

    private static MonsterDef araxyte(String id, String name, AttackDef attack) {
        return MonsterDef.of(id, name).level(96).stats(70, 90, 90, 60, 60, 90).speed(4).attack(attack)
                .defenceBonus(40, 40, 40, 20, 40).size(0.9f, 0.8f, 1.0f).moveSpeed(0.28).slayer(92, "araxytes");
    }

    /** Boss minions only drop ashes/bones where OSRS gives them anything at all. */
    private static MonsterDef noDrops(MonsterDef m) {
        if (m.id.equals("reanimated_demon_spawn") || m.id.startsWith("abyssal_scion")) {
            m.drops(Drop.always("bones"));
        }
        return m;
    }
}
