package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.MonsterDef;

import java.util.List;

/**
 * The Treasure Trails guardians (OSRS Wiki "Treasure Trails", Antagonists; stats from the wiki infoboxes as the DPS
 * calculator's monsters.json has them): Saradomin and Zamorak wizards (hard coordinates), Armadylean and Bandosian
 * guards (elite coordinates), the Brassican Mage and the Ancient Wizard trio (master coordinates and hot/cold), and the
 * double agents of hard and master emote clues. They only appear when a clue summons them and drop nothing.
 */
public final class ClueGuardians {
    private ClueGuardians() {
    }

    public static List<MonsterDef> monsters() {
        return List.of(
                // Saradomin wizard: level 108, 120 HP; melee (stab) up to 14 and Saradomin Strike.
                MonsterDef.of("saradomin_wizard", "Saradomin wizard").level(108).stats(120, 100, 80, 80, 80, 1).speed(4)
                        .attackBonus(40, 0, 0).defenceBonus(0, 0, 0, 30, 0)
                        .attack(AttackDef.of("staff", AttackType.STAB, 14, 1.5, 3))
                        .attack(AttackDef.of("saradomin_strike", AttackType.MAGIC, 14, 10, 1)) // approx: magic max not on the wiki
                        .skin(0xE0B090, 0x3050C0, 0x3050C0, 0xE0E0F0).size(0.6f, 1.95f, 1.0f),
                // Zamorak wizard: level 65, 76 HP; Flames of Zamorak up to 20 (Wilderness hard coordinates).
                MonsterDef.of("zamorak_wizard", "Zamorak wizard").level(65).stats(76, 24, 24, 24, 80, 1).speed(4)
                        .defenceBonus(0, 0, 0, 3, 0)
                        .attack(AttackDef.of("flames_of_zamorak", AttackType.MAGIC, 20, 10, 1))
                        .skin(0xE0B090, 0xA02020, 0x5A1010, 0x2A2A2A).size(0.6f, 1.95f, 1.0f),
                // Armadylean guard: level 97, 132 HP, ranged up to 10, flying.
                MonsterDef.of("armadylean_guard", "Armadylean guard").level(97).stats(132, 1, 1, 82, 80, 90).speed(4)
                        .defenceBonus(0, 0, 0, 3, 0).flying()
                        .attack(AttackDef.of("feather", AttackType.RANGED, 10, 10, 1))
                        .skin(0xD8D8C8, 0xE0E0D0, 0x8A8A7A, 0xD8B030).size(0.9f, 2.2f, 1.1f),
                // Bandosian guard: level 125, 130 HP, stab up to 13, attack speed 5.
                MonsterDef.of("bandosian_guard", "Bandosian guard").level(125).stats(130, 110, 115, 80, 1, 1).speed(5)
                        .attackBonus(19, 0, 0).defenceBonus(12, 14, 13, 5, 13)
                        .attack(AttackDef.of("spear", AttackType.STAB, 13, 1.5, 1))
                        .skin(0x7A8A4A, 0x6A5A3A, 0x4A3A2A, 0x8A8A8A).size(0.8f, 2.1f, 1.1f),
                // Brassican Mage: level 140, 150 HP, typeless magic up to 16, attack speed 3 (single-way master steps).
                MonsterDef.of("brassican_mage", "Brassican Mage").level(140).stats(150, 80, 70, 80, 170, 140).speed(3)
                        .attack(AttackDef.of("brassica_blast", AttackType.MAGIC, 16, 10, 1))
                        .skin(0x6AA03A, 0x3A6A2A, 0x2A4A1A, 0x8AC04A).size(0.6f, 1.95f, 1.0f),
                // Ancient Wizards (multi-way master steps): a mage and a ranger (level 98) and a melee wizard (level 112).
                MonsterDef.of("ancient_wizard_mage", "Ancient Wizard").level(98).stats(80, 50, 30, 20, 150, 1).speed(4)
                        .attack(AttackDef.of("ancient_magic", AttackType.MAGIC, 18, 10, 1))
                        .skin(0xD8C8A8, 0x5A2A6A, 0x3A1A4A, 0x8A3A8A).size(0.6f, 1.95f, 1.0f),
                MonsterDef.of("ancient_wizard_ranger", "Ancient Wizard").level(98).stats(80, 50, 30, 20, 80, 150).speed(4)
                        .attack(AttackDef.of("ancient_bolt", AttackType.RANGED, 23, 10, 1))
                        .skin(0xD8C8A8, 0x5A2A6A, 0x3A4A2A, 0x8A3A8A).size(0.6f, 1.95f, 1.0f),
                MonsterDef.of("ancient_wizard_melee", "Ancient Wizard").level(112).stats(80, 120, 150, 20, 80, 1).speed(4)
                        .attackBonus(80, 0, 0)
                        .attack(AttackDef.of("ancient_staff", AttackType.STAB, 36, 1.5, 1))
                        .skin(0xD8C8A8, 0x5A2A6A, 0x6A2A2A, 0x8A3A8A).size(0.6f, 1.95f, 1.0f),
                // Double agents (hard emote steps level 65 in the Wilderness, 108 elsewhere; master 141): crush.
                MonsterDef.of("double_agent_65", "Double agent").level(65).stats(80, 80, 43, 24, 24, 1).speed(4)
                        .defenceBonus(3, 3, 3, 0, 0)
                        .attack(AttackDef.of("punch", AttackType.CRUSH, 5, 1.5, 1))
                        .skin(0xE0B090, 0x2A2A2A, 0x2A2A2A, 0x2A2A2A).size(0.6f, 1.95f, 1.0f),
                MonsterDef.of("double_agent_108", "Double agent").level(108).stats(120, 100, 80, 80, 80, 1).speed(4)
                        .attackBonus(40, 0, 0).defenceBonus(20, 20, 20, 0, 0)
                        .attack(AttackDef.of("punch", AttackType.CRUSH, 14, 1.5, 1))
                        .skin(0xE0B090, 0x2A2A2A, 0x2A2A2A, 0x2A2A2A).size(0.6f, 1.95f, 1.0f),
                MonsterDef.of("double_agent_141", "Double agent").level(141).stats(160, 130, 105, 100, 95, 1).speed(4)
                        .attackBonus(40, 0, 0).defenceBonus(20, 20, 20, 0, 0)
                        .attack(AttackDef.of("punch", AttackType.CRUSH, 19, 1.5, 1))
                        .skin(0xE0B090, 0x2A2A2A, 0x2A2A2A, 0x2A2A2A).size(0.6f, 1.95f, 1.0f));
    }
}
