package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Drop;

import java.util.ArrayList;
import java.util.List;

/**
 * Round 28 creatures (stats from osrs-dps-calc monsters.json, drops from the wiki's tables, rarities x/128 as 1/n):
 * Neypotzli's moonlight cockatrices, the Troll Stronghold's three Troll generals (the quests module's "troll_general"
 * stays the Troll Stronghold quest's boss) and the prison guards Twig and Berry; the surface creatures of the same round
 * (Ice Path ice trolls, Meiyerditch Vyrewatch) are the slayer module's {@code SlayerTaskMonsters3}. Noted stacks drop as
 * plain stacks.
 */
final class DungeonMonstersExtra {
    private DungeonMonstersExtra() {
    }

    private static MonsterDef m(String id, String name, int level, int hp, int att, int str, int def, int mag, int rng) {
        return MonsterDef.of(id, name).level(level).stats(hp, att, str, def, mag, rng);
    }

    private static AttackDef melee(AttackType type, int max) {
        return AttackDef.of("melee", type, max, 2.5, 10);
    }

    static List<MonsterDef> all() {
        List<MonsterDef> list = new ArrayList<>();
        // Moonlight Cockatrice (Neypotzli's Earthbound Cavern): wiki 49, 52 hp, 31/52/39, stab max 6, slayer 25 (cockatrice
        // task, so the mirror shield's gaze protection applies), drops nearly the cockatrice's.
        list.add(m("moonlight_cockatrice", "Moonlight Cockatrice", 49, 52, 31, 52, 39, 1, 1).speed(4).attack(melee(AttackType.STAB, 6))
                .defenceBonus(14, 14, 0, 14, 0).underground()
                .drops(Drop.always("bones"), Drop.of("mithril_sword", 1, 1, 43), Drop.of("mithril_dagger", 1, 1, 43),
                        Drop.of("iron_boots", 1, 1, 128), Drop.of("mithril_javelin", 5, 5, 128), Drop.of("mithril_longsword", 1, 1, 128),
                        Drop.of("nature_rune", 5, 5, 21), Drop.of("nature_rune", 8, 8, 32), Drop.of("nature_rune", 10, 10, 64),
                        Drop.of("law_rune", 5, 5, 43), Drop.of("water_rune", 10, 10, 64), Drop.of("fire_rune", 15, 15, 64),
                        Drop.herbs(13), Drop.seeds(7), Drop.of("coins", 125, 125, 11), Drop.of("coins", 50, 50, 8),
                        Drop.of("coins", 15, 15, 11), Drop.of("limpwurt_root", 1, 1, 6), Drop.gems(64),
                        Drop.of("clue_scroll_medium", 1, 1, 128), Drop.of("cockatrice_head", 1, 1, 1000))
                .slayer(25, "cockatrice").model("cockatrice").skin(0x6A7AA8, 0x4A5A88, 0x3A4A78, 0xC8D8FF).size(0.9f, 1.5f, 1.15f));
        // Troll general (top floor of the Troll Stronghold): 113, 140 hp, 70/140/40, crush max 38 (the hall's sword-wielder
        // slashes), +60 attack +100 strength, 35/60/35 stab/slash/crush defence, 200 magic, 120 ranged (approx: the
        // dps calculator's 120 standard / 200 light and heavy).
        list.add(general("stronghold_troll_general", AttackType.CRUSH));
        // Twig and Berry, the prison guards (pickpocketed for the cell keys in OSRS): 71, 90 hp, 40/90/25, crush max 13.
        list.add(guard("twig_troll", "Twig", 0x6A6A5A, 7));
        list.add(guard("berry_troll", "Berry", 0x6A5A5A, 6));
        return list;
    }

    private static MonsterDef general(String id, AttackType type) {
        MonsterDef d = m(id, "Troll general", 113, 140, 70, 140, 40, 1, 1).speed(4).attack(melee(type, 38)).attackBonus(60, 0, 0)
                .defenceBonus(35, 60, 35, 200, 120).underground()
                .drops(Drop.always("big_bones"), Drop.of("steel_platebody", 1, 1, 32), Drop.of("black_warhammer", 1, 1, 43),
                        Drop.of("steel_warhammer", 1, 1, 43), Drop.of("adamant_axe", 1, 1, 64), Drop.of("adamant_sq_shield", 1, 1, 128),
                        Drop.of("granite_shield", 1, 1, 128), Drop.of("mithril_platebody", 1, 1, 128), Drop.of("rune_warhammer", 1, 1, 128),
                        Drop.of("earth_rune", 80, 80, 16), Drop.of("nature_rune", 16, 16, 26), Drop.of("law_rune", 4, 4, 43),
                        Drop.of("earth_rune", 65, 65, 128), Drop.of("earth_rune", 25, 25, 128), Drop.herbs(9),
                        Drop.of("coins", 40, 40, 4), Drop.of("coins", 135, 135, 5), Drop.of("coins", 190, 190, 13),
                        Drop.of("coins", 20, 20, 32), Drop.of("coins", 420, 420, 128), Drop.of("coal", 6, 6, 43),
                        Drop.of("raw_tuna", 4, 4, 64), Drop.of("long_bone", 1, 1, 400), Drop.of("curved_bone", 1, 1, 5012))
                .slayer(1, "trolls").model("troll_general").skin(0x5A6A4A, 0x6A2A1A, 0x4A3A2A, 0xC0C0B0).size(1.2f, 2.6f, 1.0f);
        d.slayerXp = 150.5; // wiki
        return d;
    }

    private static MonsterDef guard(String id, String name, int tint, int natures) {
        return m(id, name, 71, 90, 40, 90, 25, 1, 1).speed(6).attack(melee(AttackType.CRUSH, 13)).attackBonus(20, 0, 0)
                .defenceBonus(0, 0, 10, 200, 200).underground()
                .drops(Drop.always("big_bones"), Drop.of("steel_med_helm", 1, 1, 32), Drop.of("black_warhammer", 1, 1, 43),
                        Drop.of("steel_warhammer", 1, 1, 43), Drop.of("adamant_med_helm", 1, 1, 128), Drop.of("adamant_warhammer", 1, 1, 128),
                        Drop.of("mithril_sq_shield", 1, 1, 128), Drop.of("earth_rune", 60, 60, 16), Drop.of("nature_rune", natures, natures, 26),
                        Drop.of("law_rune", 2, 2, 43), Drop.herbs(9), Drop.of("coins", 35, 35, 4), Drop.of("coins", 100, 100, 13),
                        Drop.of("coins", 50, 50, 21), Drop.of("coins", 250, 250, 128), Drop.of("coal", 3, 3, 43),
                        Drop.of("raw_mackerel", 3, 3, 64), Drop.of("long_bone", 1, 1, 400))
                .slayer(1, "trolls").model("mountain_troll").skin(tint, 0x4A3A2A, 0x3A2A1A, 0xA09080).size(1.1f, 2.4f, 1.0f);
    }
}
