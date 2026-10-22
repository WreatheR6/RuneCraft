package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Region;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Drop;
import com.runecraft.osrs.model.ModelSpec;

import java.util.List;

import static com.runecraft.osrs.content.slayer.Kit.crush;
import static com.runecraft.osrs.content.slayer.Kit.m;
import static com.runecraft.osrs.content.slayer.Kit.stab;

/**
 * Slayer monsters of the Sailing islands (round 44): frost dragons and gryphons, from their OSRS Wiki infoboxes and drop
 * tables (Kiyogitpy/osrs-wiki-data, July 2026; x/130 and x/128 rates rounded to 1 in n). They live in the dungeons module's
 * Grimstone Dungeon and the Great Conch's gryphon caves ({@code SeaSlayerDungeons}). The frost dragons' better dragon metal
 * sheet rate on task (1/40) is an extra roll ({@link SlayerRewards#ON_TASK_DROPS}). Left out: the horn of plenty (gryphons'
 * tertiary, a Hunter item) and the icy dragonfire's freeze.
 */
final class SeaSlayerMonsters {
    private SeaSlayerMonsters() {
    }

    static List<MonsterDef> all() {
        return List.of(
                // Frost dragon dragonfire is icy in OSRS (a brief freeze when unprotected): not built.
                m("frost_dragon", "Frost dragon", 202, 230, 180, 150, 150, 150, 1).speed(4).attack(stab(16))
                        .attack(Kit.dragonfire(50, 4)).defenceBonus(25, 90, 15, 50, 50).rangedDefence(70, 30).weakness("fire", 100)
                        .drops(Drop.always("frost_dragon_bones"), Drop.of("dragon_metal_sheet", 1, 1, 100), // 1/40 on task: SlayerRewards.ON_TASK_DROPS
                                Drop.of("rune_2h_sword", 1, 1, 65), Drop.of("rune_kiteshield", 1, 1, 65), Drop.of("rune_longsword", 1, 1, 65),
                                Drop.of("rune_pickaxe", 1, 1, 65), Drop.of("adamant_platebody", 1, 1, 33), Drop.of("adamant_warhammer", 1, 1, 33),
                                Drop.of("death_rune", 20, 30, 26), Drop.of("water_rune", 100, 200, 13), Drop.of("water_rune", 500, 500, 26),
                                Drop.of("air_rune", 100, 200, 12), Drop.of("blood_rune", 10, 20, 26), Drop.of("chaos_rune", 60, 80, 16),
                                Drop.of("mist_rune", 40, 60, 13), Drop.of("nature_rune", 10, 20, 33), Drop.of("dragon_cannonball", 12, 20, 65),
                                Drop.of("rune_cannonball", 20, 35, 33), Drop.of("rune_knife", 10, 20, 26), Drop.of("dragon_arrowtips", 5, 10, 33),
                                Drop.of("superior_dragon_bones", 1, 1, 43), Drop.of("runite_ore", 1, 1, 43), Drop.of("adamantite_ore", 1, 1, 43),
                                Drop.of("dragon_nails", 10, 20, 65), Drop.of("coins", 50, 100, 13), Drop.of("coins", 200, 500, 13),
                                Drop.of("apple_pie", 1, 1, 26), Drop.rare(65), Kit.clue("hard", 128), Drop.of("draconic_visage", 1, 1, 10000))
                        .slayer(1, "frost_dragons").model("frost_dragon").size(1.6f, 2.0f, 1.3f).underground()
                        .spawn(Region.UNCHARTED_ISLES, 3, 1, 3),
                m("gryphon", "Gryphon", 95, 110, 65, 75, 50, 50, 1).speed(5).attack(crush(8))
                        // approx: the wing buffet knocks back a player wearing under ~30 kg (weight isn't modelled)
                        .attack(AttackDef.of("wing buffet", AttackType.CRUSH, 20, 2.5, 2).special(AttackDef.Special.KNOCKBACK, 2))
                        .defenceBonus(10, 20, 40, 100, 60).weakness("air", 50).passive()
                        .drops(Drop.always("bones"), Drop.of("gryphon_feather", 1, 3, 1), Drop.of("onion", 1, 1, 13),
                                Drop.of("strawberry", 1, 1, 13), Drop.of("watermelon", 1, 1, 13), Drop.of("raw_lobster", 1, 1, 13),
                                Drop.of("raw_swordfish", 1, 1, 13), Drop.of("raw_shark", 1, 1, 16), Drop.of("potato", 1, 1, 16),
                                Drop.of("cabbage", 1, 1, 128), Drop.of("tomato", 1, 1, 128), Drop.of("sweetcorn", 1, 1, 128), Drop.allotmentSeeds(11),
                                Drop.of("elkhorn_frag", 1, 1, 128), Drop.of("pillar_frag", 1, 1, 128), Drop.herbs(12),
                                Drop.of("gryphon_feather", 10, 25, 8), Drop.of("mithril_cannonball", 8, 12, 21), Drop.of("basket", 1, 1, 32),
                                Drop.of("empty_sack", 1, 1, 32), Drop.of("adamant_cannonball", 5, 9, 64), Drop.gems(128),
                                Kit.clue("hard", 100))
                        .slayer(51, "gryphons").model("gryphon").size(1.2f, 1.4f, 1.0f).underground().spawn(Region.UNCHARTED_ISLES, 3, 1, 3));
    }

    static List<ModelSpec.Variant> variants() {
        return List.of(
                new ModelSpec.Variant("frost_dragon", "blue_dragon", 1.0f, c -> mix(c, 0xE0F0FF, 0.6f)),
                new ModelSpec.Variant("gryphon", "shellbane_gryphon", 0.75f, c -> mix(c, 0xB08A5A, 0.3f)));
    }

    private static int mix(int rgb, int target, float amount) {
        int r = Math.round(((rgb >> 16) & 0xFF) * (1 - amount) + ((target >> 16) & 0xFF) * amount);
        int g = Math.round(((rgb >> 8) & 0xFF) * (1 - amount) + ((target >> 8) & 0xFF) * amount);
        int b = Math.round((rgb & 0xFF) * (1 - amount) + (target & 0xFF) * amount);
        return (r << 16) | (g << 8) | b;
    }
}
