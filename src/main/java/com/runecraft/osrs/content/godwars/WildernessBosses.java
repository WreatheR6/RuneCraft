package com.runecraft.osrs.content.godwars;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.data.Drop;

import java.util.ArrayList;
import java.util.List;

/**
 * The Wilderness bosses: Callisto & Artio, Venenatis & Spindel, Vet'ion & Calvar'ion, Scorpia, the
 * Chaos Elemental, the Chaos Fanatic, the Crazy Archaeologist and the King Black Dragon. Stats and
 * rates follow the OSRS wiki (post-2023 rework for the "trio"); values marked approx. are estimates.
 */
public final class WildernessBosses {
    private WildernessBosses() {
    }

    // --- King Black Dragon breaths (extra effects are applied by its script) ---
    public static final AttackDef KBD_MELEE = AttackDef.of("claws", AttackType.SLASH, 25, 3.5, 4);
    public static final AttackDef KBD_FIRE = AttackDef.of("dragonfire", AttackType.MAGIC, 65, 14, 3)
            .special(Special.DRAGONFIRE, 0).particle("minecraft:flame");
    public static final AttackDef KBD_POISON = AttackDef.of("toxic breath", AttackType.MAGIC, 65, 14, 1)
            .special(Special.DRAGONFIRE, 0).particle("minecraft:item_slime");
    public static final AttackDef KBD_ICE = AttackDef.of("icy breath", AttackType.MAGIC, 65, 14, 1)
            .special(Special.DRAGONFIRE, 0).particle("minecraft:snowflake");
    public static final AttackDef KBD_SHOCK = AttackDef.of("shocking breath", AttackType.MAGIC, 65, 14, 1)
            .special(Special.DRAGONFIRE, 0).particle("minecraft:electric_spark");

    // --- Chaos Elemental ---
    public static final AttackDef CHAOS_PRIMARY = AttackDef.of("confusion", AttackType.MAGIC, 28, 16, 6).particle("minecraft:witch");
    public static final AttackDef CHAOS_PRIMARY_RANGED = AttackDef.of("confusion (ranged)", AttackType.RANGED, 28, 16, 4)
            .particle("minecraft:crit");
    public static final AttackDef CHAOS_PRIMARY_MELEE = AttackDef.of("confusion (melee)", AttackType.CRUSH, 28, 16, 4)
            .particle("minecraft:angry_villager");
    public static final AttackDef CHAOS_DISCORD = AttackDef.of("discord", AttackType.MAGIC, 0, 16, 2)
            .particle("minecraft:portal").message("The Chaos Elemental's discord pulls at you!");
    public static final AttackDef CHAOS_MADNESS = AttackDef.of("madness", AttackType.MAGIC, 0, 16, 2)
            .particle("minecraft:enchant").message("The Chaos Elemental's madness grips you!");

    public static List<BossDef> all() {
        List<BossDef> list = new ArrayList<>();
        Drop[] trioCommons = {Drop.of("coins", 15000, 30000, 4), Drop.of("rune_pickaxe", 1, 1, 12), Drop.of("runite_ore", 15, 20, 12),
                Drop.of("magic_logs", 150, 200, 12), Drop.of("mahogany_plank", 150, 250, 14), Drop.of("uncut_dragonstone", 3, 5, 20),
                Drop.of("dark_crab", 10, 15, 10), Drop.of("super_restore", 3, 4, 10), Drop.of("blood_rune", 200, 300, 14),
                Drop.of("death_rune", 200, 300, 14)};

        // --- Callisto & Artio ---
        list.add(trio(BossDef.of("callisto", "Callisto").level(470).stats(1000, 350, 340, 440, 1, 1).speed(5)
                .defenceBonus(150, 150, 130, 160, 230)
                .attack(AttackDef.of("claw swipe", AttackType.CRUSH, 55, 3.5, 6))
                .attack(AttackDef.of("shockwave", AttackType.RANGED, 43, 16, 4).particle("minecraft:sweep_attack"))
                .attack(AttackDef.of("roar", AttackType.MAGIC, 3, 6, 2).special(Special.KNOCKBACK, 6)
                        .message("Callisto's roar throws you backwards!"))
                .attack(AttackDef.of("bear trap", AttackType.MAGIC, 0, 16, 2).special(Special.HAZARD, 10)
                        .message("Callisto scatters bear traps!"))
                .drops(trioCommons)
                .drops(Drop.always("big_bones"), Drop.of("tyrannical_ring", 1, 1, 512), Drop.of("dragon_pickaxe", 1, 1, 256),
                        Drop.of("dragon_2h_sword", 1, 1, 256), Drop.of("voidwaker_hilt", 1, 1, 360),
                        Drop.of("claws_of_callisto", 1, 1, 618), Drop.of("clue_scroll_elite", 1, 1, 100))
                .pet("callisto_cub").at("callisto_den").arena(13, "stone"), 1.6f, 1.4f, 2.0f));
        list.add(trio(BossDef.of("artio", "Artio").level(320).stats(450, 250, 240, 200, 1, 1).speed(5)
                .defenceBonus(100, 100, 90, 110, 150)
                .attack(AttackDef.of("claw swipe", AttackType.CRUSH, 41, 3.5, 6))
                .attack(AttackDef.of("shockwave", AttackType.RANGED, 30, 16, 4).particle("minecraft:sweep_attack"))
                .attack(AttackDef.of("bear trap", AttackType.MAGIC, 0, 16, 2).special(Special.HAZARD, 8))
                .drops(Drop.always("big_bones"), Drop.of("coins", 5000, 12000, 4), Drop.of("runite_ore", 5, 8, 16),
                        Drop.of("tyrannical_ring", 1, 1, 716), Drop.of("dragon_pickaxe", 1, 1, 358),
                        Drop.of("dragon_2h_sword", 1, 1, 358), Drop.of("voidwaker_hilt", 1, 1, 912),
                        Drop.of("claws_of_callisto", 1, 1, 618), Drop.of("clue_scroll_elite", 1, 1, 100))
                .pet("callisto_cub").at("hunters_end").arena(11, "jungle"), 1.4f, 1.2f, 1.5f));

        // --- Venenatis & Spindel ---
        list.add(trio(BossDef.of("venenatis", "Venenatis").level(464).stats(850, 300, 280, 321, 150, 150).speed(4)
                .defenceBonus(170, 170, 150, 220, 170)
                .attack(AttackDef.of("venomous bite", AttackType.STAB, 50, 3.5, 4).special(Special.POISON, 8))
                .attack(AttackDef.of("dark magic", AttackType.MAGIC, 30, 16, 4).special(Special.DRAIN_PRAYER, 8)
                        .particle("minecraft:witch"))
                .attack(AttackDef.of("web", AttackType.RANGED, 20, 16, 2).special(Special.FREEZE, 5)
                        .message("Venenatis hurls her web at you, sticking you to the ground!").particle("minecraft:white_ash"))
                .phase(50, "Venenatis' spiderlings hatch!", 1.0, true)
                .drops(trioCommons)
                .drops(Drop.always("big_bones"), Drop.of("treasonous_ring", 1, 1, 512), Drop.of("dragon_pickaxe", 1, 1, 256),
                        Drop.of("dragon_2h_sword", 1, 1, 256), Drop.of("voidwaker_gem", 1, 1, 360),
                        Drop.of("fangs_of_venenatis", 1, 1, 618), Drop.of("clue_scroll_elite", 1, 1, 100))
                .pet("pet_venenatis_spiderling").minion("venenatis_spiderling", 3)
                .at("venenatis_nest").arena(13, "crypt"), 2.2f, 1.1f, 2.0f));
        list.add(trio(BossDef.of("spindel", "Spindel").level(302).stats(515, 200, 190, 220, 110, 110).speed(4)
                .defenceBonus(120, 120, 110, 160, 120)
                .attack(AttackDef.of("venomous bite", AttackType.STAB, 35, 3.5, 4).special(Special.POISON, 6))
                .attack(AttackDef.of("dark magic", AttackType.MAGIC, 22, 16, 4).special(Special.DRAIN_PRAYER, 5)
                        .particle("minecraft:witch"))
                .attack(AttackDef.of("web", AttackType.RANGED, 15, 16, 2).special(Special.FREEZE, 4).particle("minecraft:white_ash"))
                .phase(50, "Spindel's spiderlings hatch!", 1.0, true)
                .drops(Drop.always("big_bones"), Drop.of("coins", 5000, 12000, 4), Drop.of("runite_ore", 5, 8, 16),
                        Drop.of("treasonous_ring", 1, 1, 716), Drop.of("dragon_pickaxe", 1, 1, 358),
                        Drop.of("dragon_2h_sword", 1, 1, 358), Drop.of("voidwaker_gem", 1, 1, 912),
                        Drop.of("fangs_of_venenatis", 1, 1, 618), Drop.of("clue_scroll_elite", 1, 1, 100))
                .pet("pet_venenatis_spiderling").minion("venenatis_spiderling", 2)
                .at("silk_chasm").arena(11, "crypt"), 1.8f, 1.0f, 1.5f));

        // --- Vet'ion & Calvar'ion (two forms and hellhounds are handled by the script) ---
        list.add(trio(BossDef.of("vetion", "Vet'ion").level(454).stats(255, 234, 270, 395, 300, 1).speed(4)
                .defenceBonus(201, 200, 200, 250, 270)
                .attack(AttackDef.of("halberd", AttackType.SLASH, 30, 3.5, 5))
                .attack(AttackDef.of("lightning", AttackType.MAGIC, 30, 16, 4).special(Special.SKY_STRIKE, 0)
                        .particle("minecraft:electric_spark").message("Vet'ion calls down lightning!"))
                .attack(AttackDef.of("shield bash", AttackType.CRUSH, 45, 4, 2).special(Special.AOE, 3)
                        .telegraph(3, "Vet'ion raises his shield... get away!"))
                .drops(trioCommons)
                .drops(Drop.always("big_bones"), Drop.of("ring_of_the_gods", 1, 1, 512), Drop.of("dragon_pickaxe", 1, 1, 256),
                        Drop.of("dragon_2h_sword", 1, 1, 256), Drop.of("voidwaker_blade", 1, 1, 360),
                        Drop.of("skull_of_vetion", 1, 1, 618), Drop.of("clue_scroll_elite", 1, 1, 100))
                .pet("vetion_jr").minion("skeleton_hellhound", 2)
                .at("vetion_ruins").arena(13, "crypt"), 0.8f, 2.2f, 1.7f));
        list.add(trio(BossDef.of("calvarion", "Calvar'ion").level(264).stats(255, 160, 180, 250, 200, 1).speed(4)
                .defenceBonus(140, 140, 140, 170, 190)
                .attack(AttackDef.of("halberd", AttackType.SLASH, 22, 3.5, 5))
                .attack(AttackDef.of("lightning", AttackType.MAGIC, 22, 16, 4).special(Special.SKY_STRIKE, 0)
                        .particle("minecraft:electric_spark"))
                .attack(AttackDef.of("shield bash", AttackType.CRUSH, 32, 4, 2).special(Special.AOE, 3)
                        .telegraph(3, "Calvar'ion raises his shield... get away!"))
                .drops(Drop.always("big_bones"), Drop.of("coins", 5000, 12000, 4), Drop.of("runite_ore", 5, 8, 16),
                        Drop.of("ring_of_the_gods", 1, 1, 716), Drop.of("dragon_pickaxe", 1, 1, 358),
                        Drop.of("dragon_2h_sword", 1, 1, 358), Drop.of("voidwaker_blade", 1, 1, 912),
                        Drop.of("skull_of_vetion", 1, 1, 618), Drop.of("clue_scroll_elite", 1, 1, 100))
                .pet("vetion_jr").minion("skeleton_hellhound", 2)
                .at("skeletal_tomb").arena(11, "crypt"), 0.8f, 2.2f, 1.3f));

        // --- Scorpia ---
        list.add(BossDef.of("scorpia", "Scorpia").level(225).stats(200, 250, 150, 240, 1, 1).speed(4)
                .defenceBonus(80, 80, 80, 50, 80) // approx.
                .attack(AttackDef.of("sting", AttackType.STAB, 16, 3.0, 10).special(Special.POISON, 20))
                .drops(Drop.always("big_bones"), Drop.of("coins", 5000, 15000, 4), Drop.of("runite_ore", 3, 6, 16),
                        Drop.of("rune_platebody", 1, 1, 16), Drop.of("dark_crab", 5, 10, 10),
                        Drop.of("odium_shard_3", 1, 1, 256), Drop.of("malediction_shard_3", 1, 1, 256),
                        Drop.of("dragon_2h_sword", 1, 1, 256), Drop.of("clue_scroll_elite", 1, 1, 100))
                .pet("pet_scorpias_offspring").minion("scorpias_guardian", 2)
                .at("scorpias_cave").arena(12, "sand").wilderness().category("Wilderness")
                .size(1.6f, 1.0f, 1.8f));

        // --- Chaos Elemental ---
        list.add(BossDef.of("chaos_elemental", "Chaos Elemental").level(305).stats(250, 270, 270, 270, 270, 270).speed(5)
                .defenceBonus(70, 70, 70, 70, 70)
                .attack(CHAOS_PRIMARY).attack(CHAOS_PRIMARY_RANGED).attack(CHAOS_PRIMARY_MELEE)
                .attack(CHAOS_DISCORD).attack(CHAOS_MADNESS)
                .drops(Drop.of("coins", 5000, 15000, 4), Drop.of("rune_2h_sword", 1, 1, 16), Drop.of("rune_platelegs", 1, 1, 16),
                        Drop.of("death_rune", 100, 200, 10), Drop.of("blood_rune", 100, 200, 10), Drop.of("runite_ore", 5, 10, 16),
                        Drop.of("dragon_pickaxe", 1, 1, 256), Drop.of("dragon_2h_sword", 1, 1, 128),
                        Drop.of("clue_scroll_elite", 1, 1, 200))
                .pet("pet_chaos_elemental").at("rogues_castle").arena(13, "void").wilderness().category("Wilderness")
                .flying().size(1.0f, 1.8f, 1.6f));

        // --- Chaos Fanatic & Crazy Archaeologist ---
        list.add(BossDef.of("chaos_fanatic", "Chaos Fanatic").level(202).stats(225, 1, 1, 220, 230, 1).speed(4)
                .defenceBonus(80, 80, 80, 150, 80) // approx.
                .attack(AttackDef.of("chaos blast", AttackType.MAGIC, 31, 16, 8).particle("minecraft:dragon_breath"))
                .attack(AttackDef.of("green explosion", AttackType.MAGIC, 15, 16, 3).special(Special.SKY_STRIKE, 0)
                        .particle("minecraft:happy_villager").message("Chaos Fanatic: Burn!"))
                .drops(Drop.always("bones"), Drop.of("coins", 3000, 8000, 4), Drop.of("chaos_rune", 100, 200, 8),
                        Drop.of("odium_shard_1", 1, 1, 256), Drop.of("malediction_shard_1", 1, 1, 256),
                        Drop.of("clue_scroll_elite", 1, 1, 750))
                .pet("pet_chaos_elemental").at("chaos_fanatic_altar").arena(10, "void").wilderness().category("Wilderness")
                .size(0.6f, 1.95f, 1.15f));
        list.add(BossDef.of("crazy_archaeologist", "Crazy archaeologist").level(204).stats(225, 1, 1, 220, 1, 275).speed(4)
                .defenceBonus(80, 80, 80, 80, 150) // approx.
                .attack(AttackDef.of("crossbow", AttackType.RANGED, 23, 16, 8).particle("minecraft:crit"))
                .attack(AttackDef.of("melee", AttackType.CRUSH, 23, 2.5, 4))
                .attack(AttackDef.of("explosive book", AttackType.RANGED, 23, 16, 3).special(Special.SKY_STRIKE, 0)
                        .particle("minecraft:explosion").message("Crazy archaeologist: Taste my knowledge!"))
                .drops(Drop.always("bones"), Drop.of("coins", 3000, 8000, 4), Drop.of("law_rune", 50, 100, 8),
                        Drop.of("odium_shard_2", 1, 1, 256), Drop.of("malediction_shard_2", 1, 1, 256),
                        Drop.of("clue_scroll_elite", 1, 1, 200))
                .at("crazy_archaeologist_ruins").arena(10, "stone").wilderness().category("Wilderness")
                .size(0.6f, 1.95f, 1.1f));

        // --- King Black Dragon ---
        list.add(BossDef.of("king_black_dragon", "King Black Dragon").level(276).stats(240, 240, 240, 240, 240, 1).speed(4)
                .defenceBonus(70, 90, 90, 80, 70)
                .attack(KBD_MELEE).attack(KBD_FIRE).attack(KBD_POISON).attack(KBD_ICE).attack(KBD_SHOCK)
                .drops(Drop.always("dragon_bones"), Drop.always("black_dragonhide"),
                        Drop.of("coins", 1000, 5000, 4), Drop.of("rune_longsword", 1, 1, 16), Drop.of("adamant_platebody", 1, 1, 16),
                        Drop.of("runite_bar", 1, 1, 16), Drop.of("yew_logs", 100, 150, 16), Drop.of("blood_rune", 30, 50, 12),
                        Drop.of("kbd_heads", 1, 1, 128), Drop.of("dragon_pickaxe", 1, 1, 1500), Drop.of("draconic_visage", 1, 1, 5000),
                        Drop.of("clue_scroll_elite", 1, 1, 450))
                .pet("prince_black_dragon").at("kbd_lair").arena(14, "lava").wilderness().category("Wilderness")
                .size(2.0f, 1.6f, 2.2f));
        return list;
    }

    /**
     * The revenant maledictus (OSRS wiki/DPS calculator stats): spawned by revenant kills in the Revenant Caves, never
     * summoned, so it has no lair of its own (not part of {@link #all()}).
     */
    public static BossDef maledictus() {
        return BossDef.of(RevenantRewards.MALEDICTUS, "Revenant maledictus").level(397).stats(1250, 106, 126, 90, 130, 130).speed(5)
                .defenceBonus(201, 206, 188, 101, 231)
                .attack(AttackDef.of("spike", AttackType.RANGED, 30, 12, 8).particle("minecraft:end_rod"))
                .attack(AttackDef.of("ice spell", AttackType.MAGIC, 30, 12, 2).special(Special.FREEZE, 5)
                        .telegraph(3, "The Zarosian symbol on the maledictus glows blue!").particle("minecraft:snowflake"))
                .attack(AttackDef.of("blood spell", AttackType.MAGIC, 30, 12, 2).special(Special.LIFESTEAL, 50)
                        .telegraph(3, "The Zarosian symbol on the maledictus glows red!").particle("minecraft:damage_indicator"))
                .attack(AttackDef.of("air surge", AttackType.MAGIC, 25, 12, 2).special(Special.SKY_STRIKE, 0)
                        .telegraph(3, "The Zarosian symbol on the maledictus glows white!").particle("minecraft:cloud"))
                .wilderness().category("Wilderness").model("revenant_knight").size(1.0f, 2.6f, 1.6f).moveSpeed(0.2);
    }

    private static BossDef trio(BossDef def, float width, float height, float scale) {
        return def.wilderness().category("Wilderness").size(width, height, scale);
    }

    /** Stats drained by the King Black Dragon's shocking breath. */
    public static final Skill[] SHOCK_DRAINS = {Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC};

    /** Real pet rates (1/x) for bosses whose scripts top up the engine roll. */
    public static int realPetRate(String bossId) {
        return switch (bossId) {
            case "callisto", "venenatis", "vetion" -> 1500;
            case "artio", "spindel", "calvarion" -> 2800;
            case "scorpia" -> 2016;
            case "chaos_elemental" -> 300;
            case "chaos_fanatic" -> 1000;
            case "nex" -> 500;
            default -> GodWarsFactions.ENGINE_PET_RATE;
        };
    }
}
