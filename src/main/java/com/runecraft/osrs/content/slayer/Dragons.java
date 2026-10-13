package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Region;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Drop;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.slayer.Kit.clue;
import static com.runecraft.osrs.content.slayer.Kit.coins;
import static com.runecraft.osrs.content.slayer.Kit.dragonfire;
import static com.runecraft.osrs.content.slayer.Kit.m;
import static com.runecraft.osrs.content.slayer.Kit.magic;
import static com.runecraft.osrs.content.slayer.Kit.ranged;
import static com.runecraft.osrs.content.slayer.Kit.slash;
import static com.runecraft.osrs.content.slayer.Kit.stab;

/**
 * Chromatic, metallic, brutal, baby and lava dragons. All adult dragons breathe dragonfire (max 50;
 * an anti-dragon shield and/or antifire reduce it) and drop dragon bones; most can drop a draconic
 * visage (1/10,000).
 */
final class Dragons {
    private Dragons() {
    }

    private static MonsterDef dragon(MonsterDef d, String category, String model, float scale) {
        return d.slayer(1, category).model(model).size(1.6f, 2.0f, scale);
    }

    private static Drop[] common(String hide, Drop... extra) {
        List<Drop> list = new ArrayList<>();
        list.add(Drop.always("dragon_bones"));
        if (hide != null) {
            list.add(Drop.always(hide));
        }
        list.addAll(List.of(extra));
        list.add(Drop.of("draconic_visage", 1, 1, 10000));
        return list.toArray(new Drop[0]);
    }

    static List<MonsterDef> all() {
        return List.of(
                // --- Baby dragons (count towards their colour's task) ---
                dragon(m("baby_green_dragon", "Baby green dragon", 42, 50, 40, 40, 40, 1, 1).speed(4).attack(stab(5))
                        .drops(Drop.always("babydragon_bones"), Drop.herbs(30)), "green_dragons", "green_dragon", 0.5f)
                        .spawn(Region.KARAMJA, 2, 1, 2).underground(),
                dragon(m("baby_blue_dragon", "Baby blue dragon", 48, 50, 40, 40, 40, 1, 1).speed(4).attack(stab(5))
                        .drops(Drop.always("babydragon_bones"), Drop.herbs(30)), "blue_dragons", "blue_dragon", 0.5f)
                        .spawn(Region.ASGARNIA, 3, 1, 3).underground(),
                dragon(m("baby_red_dragon", "Baby red dragon", 65, 50, 50, 50, 50, 1, 1).speed(4).attack(stab(7))
                        .drops(Drop.always("babydragon_bones"), Drop.herbs(30)), "red_dragons", "red_dragon", 0.5f)
                        .spawn(Region.KARAMJA, 3, 1, 3).underground(),
                dragon(m("baby_black_dragon", "Baby black dragon", 83, 80, 70, 70, 70, 1, 1).speed(4).attack(stab(9))
                        .drops(Drop.always("babydragon_bones"), Drop.herbs(30)), "black_dragons", "black_dragon", 0.55f)
                        .spawn(Region.ASGARNIA, 2, 1, 2).underground(),

                // --- Chromatic dragons ---
                dragon(m("green_dragon", "Green dragon", 79, 75, 68, 68, 68, 68, 1).speed(4).attack(slash(8)).attack(dragonfire(50, 4))
                        .defenceBonus(40, 40, 40, 30, 40).drops(common("green_dragonhide", Drop.of("rune_dagger", 1, 1, 128),
                                Drop.of("adamant_full_helm", 1, 1, 64), Drop.of("nature_rune", 5, 15, 12), Drop.of("law_rune", 3, 5, 16),
                                Drop.herbs(16), Drop.gems(32), clue("hard", 128))), "green_dragons", "green_dragon", 1.0f)
                        .spawn(Region.WILDERNESS, 4, 1, 3),
                dragon(m("blue_dragon", "Blue dragon", 111, 105, 95, 95, 95, 95, 1).speed(4).attack(slash(10)).attack(dragonfire(50, 4))
                        .defenceBonus(50, 70, 70, 50, 60).drops(common("blue_dragonhide", Drop.of("rune_dagger", 1, 1, 128),
                                Drop.of("adamant_full_helm", 1, 1, 64), Drop.of("nature_rune", 5, 15, 12), Drop.of("law_rune", 3, 5, 16),
                                Drop.herbs(16), Drop.gems(32), clue("hard", 128))), "blue_dragons", "blue_dragon", 1.1f)
                        .spawn(Region.ASGARNIA, 3, 1, 2).underground(), // Taverley Dungeon (the Ogre Enclave isn't built)
                dragon(m("red_dragon", "Red dragon", 152, 140, 130, 130, 130, 130, 1).speed(4).attack(slash(15)).attack(dragonfire(50, 4))
                        .defenceBonus(70, 90, 90, 80, 80).drops(common("red_dragonhide", Drop.of("rune_longsword", 1, 1, 128),
                                Drop.of("rune_full_helm", 1, 1, 128), Drop.of("blood_rune", 5, 10, 16), Drop.of("law_rune", 4, 8, 16),
                                Drop.herbs(16), Drop.gems(32), clue("hard", 128))), "red_dragons", "red_dragon", 1.2f)
                        .spawn(Region.KARAMJA, 3, 1, 2).underground(),
                dragon(m("black_dragon", "Black dragon", 227, 190, 200, 200, 200, 100, 1).speed(4).attack(slash(21)).attack(dragonfire(50, 4))
                        .defenceBonus(70, 90, 90, 100, 100).drops(common("black_dragonhide", Drop.of("rune_longsword", 1, 1, 128),
                                Drop.of("rune_full_helm", 1, 1, 128), Drop.of("adamantite_bar", 1, 1, 32), Drop.of("blood_rune", 10, 15, 16),
                                Drop.herbs(16), Drop.gems(20), clue("hard", 128), clue("elite", 500))), "black_dragons", "black_dragon", 1.35f)
                        .spawn(Region.ASGARNIA, 2, 1, 2).spawn(Region.WILDERNESS, 2, 1, 2).underground(),

                // --- Brutal dragons (also cast magic) ---
                dragon(m("brutal_green_dragon", "Brutal green dragon", 227, 175, 268, 268, 238, 168, 1).speed(4).attack(slash(18))
                        .attack(magic(18, "minecraft:happy_villager")).attack(dragonfire(50, 3)).defenceBonus(120, 120, 120, 50, 120)
                        .drops(common(null, Drop.of("green_dragonhide", 2, 2, 1), Drop.of("rune_platelegs", 1, 1, 128),
                                Drop.of("death_rune", 15, 30, 8), Drop.herbs(12), Drop.gems(16), clue("hard", 100))),
                        "green_dragons", "brutal_green_dragon", 1.3f).spawn(Region.KANDARIN, 2, 1, 2).underground(),
                dragon(m("brutal_blue_dragon", "Brutal blue dragon", 271, 245, 268, 268, 238, 168, 1).speed(4).attack(slash(22))
                        .attack(magic(20, "minecraft:enchanted_hit")).attack(dragonfire(50, 3)).defenceBonus(130, 130, 130, 60, 130)
                        .drops(common(null, Drop.of("blue_dragonhide", 2, 2, 1), Drop.of("rune_platelegs", 1, 1, 128),
                                Drop.of("death_rune", 15, 30, 8), Drop.herbs(12), Drop.gems(16), clue("hard", 100))),
                        "blue_dragons", "brutal_blue_dragon", 1.4f).spawn(Region.KOUREND, 1, 1, 1).underground(),
                dragon(m("brutal_red_dragon", "Brutal red dragon", 289, 285, 268, 268, 238, 168, 1).speed(4).attack(slash(24))
                        .attack(magic(22, "minecraft:flame")).attack(dragonfire(50, 3)).defenceBonus(140, 140, 140, 70, 140)
                        .drops(common(null, Drop.of("red_dragonhide", 2, 2, 1), Drop.of("rune_platebody", 1, 1, 256),
                                Drop.of("blood_rune", 15, 30, 8), Drop.herbs(12), Drop.gems(16), clue("hard", 100))),
                        "red_dragons", "brutal_red_dragon", 1.45f).spawn(Region.KOUREND, 1, 1, 1).underground(),
                dragon(m("brutal_black_dragon", "Brutal black dragon", 318, 315, 268, 268, 238, 168, 1).speed(4).attack(slash(28))
                        .attack(magic(25, "minecraft:smoke")).attack(dragonfire(50, 3)).defenceBonus(150, 150, 150, 80, 150)
                        .drops(common(null, Drop.of("black_dragonhide", 2, 2, 1), Drop.of("rune_platebody", 1, 1, 128),
                                Drop.of("dragon_platelegs", 1, 1, 512), Drop.of("blood_rune", 20, 40, 8), Drop.herbs(12), Drop.gems(16),
                                clue("elite", 500))), "black_dragons", "brutal_black_dragon", 1.5f)
                        .spawn(Region.KOUREND, 1, 1, 1).underground(),

                // --- Lava dragons ---
                m("lava_dragon", "Lava dragon", 252, 230, 240, 240, 220, 220, 1).speed(4).attack(slash(20))
                        .attack(AttackDef.of("lava", AttackType.MAGIC, 20, 10, 3).particle("minecraft:lava"))
                        .attack(dragonfire(50, 3)).defenceBonus(60, 60, 60, 60, 60)
                        .drops(Drop.always("lava_dragon_bones"), Drop.always("black_dragonhide"), Drop.always("lava_scale"),
                                Drop.of("rune_full_helm", 1, 1, 128), Drop.of("fire_rune", 50, 150, 6), Drop.of("uncut_onyx", 1, 1, 10000),
                                Drop.of("draconic_visage", 1, 1, 10000), Drop.herbs(12), Drop.gems(16), clue("elite", 250))
                        .slayer(1, "lava_dragons").model("lava_dragon").size(1.6f, 2.0f, 1.4f).spawn(Region.WILDERNESS, 2, 1, 2),

                // --- Metallic dragons ---
                dragon(m("bronze_dragon", "Bronze dragon", 131, 122, 112, 112, 112, 1, 1).speed(4).attack(slash(16)).attack(dragonfire(50, 4))
                        .defenceBonus(50, 70, 70, 50, 120).drops(common(null, Drop.of("bronze_bar", 5, 5, 1), Drop.of("rune_full_helm", 1, 1, 128), Drop.of("adamant_platelegs", 1, 1, 64),
                                Drop.of("blood_rune", 5, 10, 16), Drop.herbs(14), Drop.gems(32), clue("hard", 128))),
                        "metal_dragons", "bronze_dragon", 1.2f).spawn(Region.KARAMJA, 2, 1, 2).underground(),
                dragon(m("iron_dragon", "Iron dragon", 189, 165, 165, 165, 165, 1, 1).speed(4).attack(slash(21)).attack(dragonfire(50, 4))
                        .defenceBonus(80, 100, 100, 50, 180).drops(common(null, Drop.of("iron_bar", 5, 5, 1),
                                Drop.of("rune_full_helm", 1, 1, 128), Drop.of("rune_dagger", 1, 1, 64), Drop.of("blood_rune", 5, 10, 16),
                                Drop.herbs(14), Drop.gems(32), clue("hard", 128))), "metal_dragons", "iron_dragon", 1.3f)
                        .spawn(Region.KARAMJA, 2, 1, 2).underground(),
                dragon(m("steel_dragon", "Steel dragon", 246, 210, 215, 215, 215, 1, 1).speed(4).attack(slash(28)).attack(dragonfire(50, 4))
                        .defenceBonus(100, 120, 120, 80, 220).drops(common(null, Drop.of("steel_bar", 5, 5, 1),
                                Drop.of("rune_full_helm", 1, 1, 64), Drop.of("rune_battleaxe", 1, 1, 128), Drop.of("death_rune", 10, 20, 12),
                                Drop.herbs(14), Drop.gems(32), clue("elite", 500))), "metal_dragons", "steel_dragon", 1.4f)
                        .spawn(Region.KARAMJA, 1, 1, 2).underground(),
                dragon(m("mithril_dragon", "Mithril dragon", 304, 255, 268, 268, 268, 168, 168).speed(5).attack(slash(28))
                        .attack(magic(18, "minecraft:enchanted_hit")).attack(ranged(18, "minecraft:crit")).attack(dragonfire(50, 3))
                        .defenceBonus(140, 160, 160, 100, 240).drops(common(null, Drop.of("mithril_bar", 3, 3, 1),
                                Drop.of("dragon_full_helm", 1, 1, 32768), Drop.of("rune_battleaxe", 1, 1, 64), Drop.of("rune_full_helm", 1, 1, 64),
                                Drop.of("blood_rune", 15, 30, 8), Drop.herbs(12), Drop.gems(16), clue("elite", 350))),
                        "metal_dragons", "mithril_dragon", 1.5f).spawn(Region.KANDARIN, 1, 1, 2).underground(),
                dragon(m("adamant_dragon", "Adamant dragon", 338, 295, 250, 250, 300, 200, 200).speed(4).attack(slash(33))
                        .attack(ranged(25, "minecraft:item_slime").special(AttackDef.Special.POISON, 8))
                        .attack(AttackDef.of("acid", AttackType.MAGIC, 20, 8, 2).special(AttackDef.Special.HAZARD, 8)
                                .particle("minecraft:item_slime").telegraph(2, "The dragon spews a pool of poison!"))
                        .attack(dragonfire(50, 3)).defenceBonus(150, 170, 170, 150, 200)
                        .drops(common(null, Drop.of("adamantite_bar", 2, 2, 1), Drop.of("dragon_limbs", 1, 1, 1000),
                                Drop.of("dragon_metal_slice", 1, 1, 1000), Drop.of("wrath_rune", 10, 25, 8), Drop.of("rune_platelegs", 1, 1, 64),
                                Drop.herbs(10), Drop.gems(16), clue("elite", 256))), "metal_dragons", "adamant_dragon", 1.6f)
                        .spawn(Region.LITHKREN, 1, 1, 1).underground(),
                dragon(m("rune_dragon", "Rune dragon", 380, 330, 255, 255, 290, 230, 230).speed(4).attack(slash(38))
                        .attack(AttackDef.of("lightning", AttackType.MAGIC, 30, 10, 3).particle("minecraft:electric_spark")
                                .message("The rune dragon fires a bolt of lightning."))
                        .attack(ranged(28, "minecraft:crit")).attack(dragonfire(50, 3)).defenceBonus(160, 180, 180, 160, 240)
                        .drops(common(null, Drop.of("runite_bar", 2, 2, 1), Drop.of("dragon_limbs", 1, 1, 800),
                                Drop.of("dragon_metal_lump", 1, 1, 5000), Drop.of("wrath_rune", 20, 40, 8), Drop.of("rune_platebody", 1, 1, 64),
                                Drop.herbs(10), Drop.gems(16), clue("elite", 200))), "metal_dragons", "rune_dragon", 1.7f)
                        .spawn(Region.LITHKREN, 1, 1, 1).underground());
    }
}
