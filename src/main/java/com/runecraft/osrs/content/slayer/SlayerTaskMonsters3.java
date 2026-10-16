package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Region;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Drop;

import java.util.ArrayList;
import java.util.List;

/**
 * Round 28 surface creatures (stats from osrs-dps-calc monsters.json, drops from the wiki's tables, rarities x/128 as
 * 1/n, noted stacks as plain stacks): the level 120-124 ice trolls of the Ice Path north of the Troll Stronghold and the
 * Vyrewatch of Meiyerditch and Darkmeyer in their four tiers ({@code SpawnTable} puts them on their cache clusters).
 * The Ice Path's Trollweiss Dungeon twins and the level 87 Vyrewatch are not placed.
 */
final class SlayerTaskMonsters3 {
    private SlayerTaskMonsters3() {
    }

    private static MonsterDef m(String id, String name, int level, int hp, int att, int str, int def, int mag, int rng) {
        return MonsterDef.of(id, name).level(level).stats(hp, att, str, def, mag, rng);
    }

    private static AttackDef melee(AttackType type, int max) {
        return AttackDef.of("melee", type, max, 2.5, 10);
    }

    static List<MonsterDef> all() {
        List<MonsterDef> list = new ArrayList<>();
        // Ice trolls of the Ice Path (Desert Treasure), levels 120/121/123/124: crush, +60 attack/strength, 30/60/30
        // stab/slash/crush defence, weak to fire.
        list.add(iceTroll(120, 100, 100, 100, 120, 21, 129));
        list.add(iceTroll(121, 90, 110, 110, 110, 23, 97));
        list.add(iceTroll(123, 80, 120, 120, 100, 25, 86));
        list.add(iceTroll(124, 80, 140, 120, 80, 25, 86));
        // Vyrewatch (Meiyerditch and Darkmeyer): tier 3 vampyres (flail of Ivandis, blisterwood or Ivandis weapons only).
        list.add(vyrewatch(105, 90, 105, 86, 105, 4, 10));
        list.add(vyrewatch(110, 90, 110, 96, 110, 8, 11));
        list.add(vyrewatch(120, 105, 120, 106, 120, 8, 12));
        list.add(vyrewatch(125, 110, 120, 116, 120, 8, 13));
        return list;
    }

    private static MonsterDef iceTroll(int level, int hp, int att, int str, int def, int max, double slayerXp) {
        MonsterDef d = m("ice_troll_lvl" + level, "Ice troll", level, hp, att, str, def, 1, 1).speed(4).attack(melee(AttackType.CRUSH, max))
                .attackBonus(60, 0, 0).defenceBonus(30, 60, 30, 0, 0)
                .drops(Drop.always("big_bones"), Drop.of("black_platebody", 1, 1, 32), Drop.of("adamant_axe", 1, 1, 64),
                        Drop.of("adamant_kiteshield", 1, 1, 128), Drop.of("black_warhammer", 1, 1, 128), Drop.of("mithril_platebody", 1, 1, 128),
                        Drop.of("rune_battleaxe", 1, 1, 128), Drop.of("blood_rune", 2, 2, 16), Drop.of("blood_rune", 5, 5, 26),
                        Drop.of("law_rune", 4, 4, 43), Drop.of("death_rune", 15, 15, 128), Drop.of("water_rune", 65, 65, 128),
                        Drop.herbs(7), Drop.of("coins", 30, 30, 4), Drop.of("coins", 136, 136, 5), Drop.of("coins", 200, 200, 13),
                        Drop.of("coins", 20, 20, 32), Drop.of("lobster", 6, 6, 43), Drop.of("raw_tuna", 4, 4, 64), Drop.gems(16),
                        Drop.of("long_bone", 1, 1, 400), Drop.of("curved_bone", 1, 1, 5012))
                .slayer(1, "trolls").model("ice_troll").skin(0xA8C8E0, 0x8AAAC8, 0x6A8AA8, 0xE0F0FF).size(1.0f, 2.5f, 1.0f)
                .spawn(Region.TROLL_COUNTRY, 3, 1, 3);
        d.slayerXp = slayerXp; // wiki: 129, 97, 86 and 86 (not the hitpoints)
        return d;
    }

    private static MonsterDef vyrewatch(int level, int hp, int att, int str, int mag, int speed, int max) {
        return m("vyrewatch_lvl" + level, "Vyrewatch", level, hp, att, str, 85, mag, mag).speed(speed).attack(melee(AttackType.CRUSH, max))
                .defenceBonus(0, 0, 0, 0, 0).attributes("vampyre", "vampyre3")
                .drops(Drop.always("vampyre_dust"), Drop.of("rune_dagger", 1, 1, 16), Drop.of("adamant_platelegs", 1, 1, 48),
                        Drop.of("adamant_platebody", 1, 1, 16), Drop.of("rune_platelegs", 1, 1, 48), Drop.of("mithril_axe", 1, 1, 16),
                        Drop.of("rune_full_helm", 1, 1, 96), Drop.of("death_rune", 4, 12, 10), Drop.of("blood_rune", 4, 12, 10),
                        Drop.of("nature_rune", 6, 14, 10), Drop.of("adamant_arrow", 8, 16, 24), Drop.of("rune_javelin", 5, 15, 48),
                        Drop.herbs(96), Drop.of("adamantite_ore", 1, 1, 48), Drop.of("coal", 6, 6, 24), Drop.of("runite_bar", 1, 1, 48),
                        Drop.of("mort_myre_fungus", 2, 2, 48), Drop.of("yew_logs", 4, 4, 48), Drop.of("bark", 4, 8, 24),
                        Drop.of("coins", 100, 1000, 6), Drop.rare(96))
                .slayer(1, "vampyres").model("vyrewatch_sentinel").skin(0x6A6A70, 0x3A1018, 0x2A0A10, 0xA01010).size(0.6f, 1.95f, 1.0f)
                .flying().spawn(Region.MORYTANIA, 3, 1, 3);
    }
}
