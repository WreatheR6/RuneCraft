package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef.Slot;
import com.runecraft.osrs.EquipmentDef.WeaponClass;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Drop;

import java.util.List;

import static com.runecraft.osrs.content.bosses.BossItems.st;

/**
 * Bosses released in 2025-2026: Yama (May 2025), the Doom of Mokhaiotl (July 2025), the Gemstone Crab, the
 * Shellbane gryphon (Slayer boss, November 2025), Brutus and Demonic Brutus (The Ides of Milk, February 2026) and
 * the Maggot King (The Blood Moon Rises, June 2026).
 *
 * <p>Stats are the OSRS Wiki DPS calculator values (weirdgloop/osrs-dps-calc {@code monsters.json}); max hits are the
 * wiki's. Drop rates are per kill for a solo player with full contribution: where OSRS scales them by contribution
 * or (the Doom) by delve depth, the deepest/solo rate is used.
 */
public final class ModernBosses {
    private ModernBosses() {
    }

    // ================= Yama =================
    // Auto-attacks alternate between fire (ranged) and shadow (magic) projectiles, max hit 46; the colour of the
    // charging orb tells which prayer to use. Between phases (66% / 33%) the Judge of Yama's void flares appear.
    // Yama's fire orb is his magic attack and the shadow orb his ranged one (OSRS Wiki).
    public static final AttackDef YAMA_FIRE = AttackDef.of("fire_orb", AttackType.MAGIC, 46, 16, 3)
            .telegraph(2, "Yama's orb blazes with fire...").particle("minecraft:flame");
    public static final AttackDef YAMA_SHADOW = AttackDef.of("shadow_orb", AttackType.RANGED, 46, 16, 3)
            .telegraph(2, "Yama's orb swirls with shadow...").particle("minecraft:soul_fire_flame");
    public static final AttackDef YAMA_SLASH = AttackDef.of("claw_swipe", AttackType.SLASH, 46, 4, 2);
    /** Fire meteors rain on your tile: move or take a huge unprotectable hit. */
    public static final AttackDef YAMA_METEOR = AttackDef.of("meteor_strike", AttackType.MAGIC, 70, 16, 1) // approx
            .special(Special.SKY_STRIKE, 0).telegraph(4, "Meteors streak down towards you!").particle("minecraft:lava");
    /** Shadow crash: a burst around Yama that only distance avoids. */
    public static final AttackDef YAMA_SHADOW_CRASH = AttackDef.of("shadow_crash", AttackType.MAGIC, 50, 5, 1) // approx
            .special(Special.AOE, 5).telegraph(3, "Shadows gather around Yama...").particle("minecraft:squid_ink");
    public static final AttackDef YAMA_GLYPH = AttackDef.of("glyph_burn", AttackType.MAGIC, 30, 16, 1) // approx
            .special(Special.HAZARD, 8).particle("minecraft:flame");

    // ================= Doom of Mokhaiotl =================
    public static final String DOOM_LARVA = "demonic_larva";
    public static final String DOOM_GIANT_LARVA = "giant_demonic_larva";
    public static final String DOOM_VOLATILE_EARTH = "doom_volatile_earth";
    public static final AttackDef DOOM_MELEE = AttackDef.of("crushing_claws", AttackType.CRUSH, 65, 5, 4);
    public static final AttackDef DOOM_MAGIC = AttackDef.of("void_orb", AttackType.MAGIC, 40, 16, 3) // approx
            .telegraph(2, "The Doom's eye glows violet...").particle("minecraft:witch");
    public static final AttackDef DOOM_RANGED = AttackDef.of("rock_spit", AttackType.RANGED, 40, 16, 3) // approx
            .telegraph(2, "The Doom's maw fills with rocks...").particle("minecraft:crit");
    /** The charge: 99 at the deepest delves, telegraphed so you can step out of its path. */
    public static final AttackDef DOOM_CHARGE = AttackDef.of("charge", AttackType.CRUSH, 99, 16, 1)
            .special(Special.SKY_STRIKE, 0).telegraph(4, "The Doom of Mokhaiotl rears up to charge!");
    public static final AttackDef DOOM_ACID = AttackDef.of("volatile_earth", AttackType.RANGED, 20, 16, 1) // approx
            .special(Special.HAZARD, 6).particle("minecraft:falling_lava");

    // ================= Shellbane gryphon =================
    public static final AttackDef SHELLBANE_MELEE = AttackDef.of("beak", AttackType.CRUSH, 22, 4, 5);
    public static final AttackDef SHELLBANE_RANGED = AttackDef.of("feather_volley", AttackType.RANGED, 22, 14, 4)
            .particle("minecraft:white_ash");
    public static final AttackDef SHELLBANE_WHIRLWIND = AttackDef.of("whirlwinds", AttackType.MAGIC, 64, 14, 1)
            .special(Special.SKY_STRIKE, 0).telegraph(4, "The shellbane gryphon whips up whirlwinds!").particle("minecraft:cloud");
    /** Throws the player backwards for up to 30 unavoidable damage. */
    public static final AttackDef SHELLBANE_KNOCKBACK = AttackDef.of("wing_buffet", AttackType.CRUSH, 30, 4, 1)
            .special(Special.KNOCKBACK, 6).telegraph(2, "The shellbane gryphon spreads its wings!");

    // ================= Brutus =================
    public static final AttackDef BRUTUS_HEADBUTT = AttackDef.of("headbutt", AttackType.CRUSH, 3, 3.5, 4);
    public static final AttackDef BRUTUS_CHARGE = AttackDef.of("charge", AttackType.CRUSH, 6, 12, 1) // approx
            .special(Special.SKY_STRIKE, 0).telegraph(3, "Brutus paws at the ground...");
    public static final AttackDef DEMONIC_HORNS = AttackDef.of("horn_gore", AttackType.CRUSH, 43, 3.5, 4);
    public static final AttackDef DEMONIC_CHARGE = AttackDef.of("hellfire_charge", AttackType.CRUSH, 60, 12, 1) // approx
            .special(Special.SKY_STRIKE, 0).telegraph(3, "Demonic Brutus snorts hellfire and lowers its horns!").particle("minecraft:flame");
    public static final AttackDef DEMONIC_STOMP = AttackDef.of("stomp", AttackType.CRUSH, 25, 4, 1) // approx
            .special(Special.KNOCKBACK, 4).telegraph(2, "Demonic Brutus rears up!");
    public static final AttackDef DEMONIC_FIRE = AttackDef.of("brimstone", AttackType.MAGIC, 15, 12, 1) // approx
            .special(Special.HAZARD, 5).particle("minecraft:flame");
    /** Phase 2: Demonic Brutus revives with 487 hitpoints after dropping to 0 ("Moo moo!"). */
    public static final int DEMONIC_REVIVE_HP = 487;
    /** Phase 2: each special is accompanied by four ghost projectiles, magic-melee-ranged then a random one. */
    public static final AttackDef DEMONIC_GHOST_MAGIC = AttackDef.of("ghost_magic", AttackType.MAGIC, 30, 16, 1) // approx
            .particle("minecraft:witch");
    public static final AttackDef DEMONIC_GHOST_MELEE = AttackDef.of("ghost_melee", AttackType.CRUSH, 30, 16, 1) // approx
            .particle("minecraft:crimson_spore");
    public static final AttackDef DEMONIC_GHOST_RANGED = AttackDef.of("ghost_ranged", AttackType.RANGED, 30, 16, 1) // approx
            .particle("minecraft:crit");
    /** Basic attacks between Demonic Brutus' specials (stomp and charge alternate). */
    public static final int DEMONIC_BASICS_PER_SPECIAL = 3;

    // ================= Maggot King =================
    public static final AttackDef MAGGOT_MAGIC = AttackDef.of("bile_bolt", AttackType.MAGIC, 46, 16, 3)
            .telegraph(2, "The Maggot King's sacs swell purple...").particle("minecraft:witch");
    public static final AttackDef MAGGOT_RANGED = AttackDef.of("maggot_spit", AttackType.RANGED, 46, 16, 3)
            .telegraph(2, "The Maggot King's sacs swell green...").particle("minecraft:item_slime");
    public static final AttackDef MAGGOT_MELEE = AttackDef.of("crush", AttackType.CRUSH, 46, 5, 2); // approx
    public static final AttackDef MAGGOT_SWARM = AttackDef.of("swarm", AttackType.RANGED, 12, 16, 1) // approx
            .special(Special.HAZARD, 4).particle("minecraft:mycelium");
    public static final AttackDef MAGGOT_ROAR = AttackDef.of("roar", AttackType.MAGIC, 35, 6, 1) // approx
            .special(Special.AOE, 6).telegraph(3, "The Maggot King lets out a deafening roar!");

    public static List<BossDef> all() {
        return List.of(yama(), doom(), gemstoneCrab(), shellbane(), brutus(), demonicBrutus(), maggotKing());
    }

    public static List<MonsterDef> minions() {
        return List.of(
                // Yama's void flares (fire and shadow): static orbs that charge for ~15 seconds and then explode for up to 44
                // (scaled by their remaining health), healing Yama; the Yama script times and resolves the explosion.
                MonsterDef.of("yama_void_flare", "Void Flare").level(130).stats(140, 1, 1, 100, 200, 1).speed(4)
                        .attack(AttackDef.of("inert", AttackType.MAGIC, 0, 0.5, 1)).passive()
                        .attributes("demon").skin(0x3A1A4A, 0xE05010, 0x2A0A3A, 0xFF8A1E).size(0.9f, 1.4f, 1.0f).moveSpeed(0.0),
                // The Doom of Mokhaiotl's demonic larvae (2 hitpoints, a giant one 4) crawl to the Doom and are moved by its script;
                // volatile earth (1 hitpoint) raises the earthen shield against the shockwave.
                MonsterDef.of(DOOM_LARVA, "Demonic larva").level(1).stats(DoomDelves.larvaHitpoints(false), 1, 1, 1, 1, 1)
                        .attack(AttackDef.of("inert", AttackType.MAGIC, 0, 0.5, 1)).passive().attributes("demon")
                        .skin(0xC8B48A, 0x9A7A5A, 0x7A5A3A, 0xE0D0A0).size(0.6f, 0.4f, 0.8f).moveSpeed(0.0),
                MonsterDef.of(DOOM_GIANT_LARVA, "Giant demonic larva").level(1).stats(DoomDelves.larvaHitpoints(true), 1, 1, 1, 1, 1)
                        .attack(AttackDef.of("inert", AttackType.MAGIC, 0, 0.5, 1)).passive().attributes("demon")
                        .skin(0xA03A2A, 0x7A2A1A, 0x5A1A10, 0xE07A3A).size(1.0f, 0.7f, 1.2f).moveSpeed(0.0),
                MonsterDef.of(DOOM_VOLATILE_EARTH, "Volatile earth").level(1).stats(1, 1, 1, 1, 1, 1)
                        .attack(AttackDef.of("inert", AttackType.MAGIC, 0, 0.5, 1)).passive()
                        .skin(0x6A5A4A, 0x8A6A3A, 0x4A3A2A, 0xE0A040).size(1.0f, 1.0f, 1.0f).moveSpeed(0.0),
                // The Judge of Yama fought between Yama's phases (400 hitpoints; its fire spells are cast by the Yama script).
                MonsterDef.of(BossMechanics.YAMA_JUDGE, "Judge of Yama").level(168).stats(BossMechanics.JUDGE_HITPOINTS, 150, 150, 130, 150, 1)
                        .attack(AttackDef.of("inert", AttackType.MAGIC, 0, 0.5, 1)).passive().attributes("demon").model("judge_of_yama")
                        .skin(0x5A1A2A, 0x1A0A10, 0x2A0A10, 0xFF6020).size(1.0f, 2.6f, 1.0f).moveSpeed(0.0),
                // Amoxliatl's unstable ice: only melee breaks it (in one hit); left alone it shatters after 15 ticks.
                MonsterDef.of(BossMechanics.UNSTABLE_ICE, "Unstable ice").level(1).stats(20, 1, 1, 1, 1, 1) // hitpoints approx
                        .attack(AttackDef.of("inert", AttackType.MAGIC, 0, 0.5, 1)).passive()
                        .skin(0xB8E8FF, 0x90D0F0, 0x70B0E0, 0xFFFFFF).size(0.9f, 1.0f, 1.0f).moveSpeed(0.0),
                // The Maggot King's ur-maggot larvae: a bow kills one in the air; landed, it crawls along its row (moved by the script).
                MonsterDef.of(BossMechanics.UR_MAGGOT_LARVA, "Ur-maggot larvae").level(1).stats(10, 1, 1, 1, 1, 1) // hitpoints approx
                        .attack(AttackDef.of("inert", AttackType.MAGIC, 0, 0.5, 1)).passive()
                        .skin(0xD8C8A0, 0xC0B080, 0xA09060, 0x8A2A2A).size(0.6f, 0.4f, 0.9f).moveSpeed(0.0),
                // Bryophyta's growthlings (OSRS Wiki): level 37, 10 hitpoints, slash 5 every 4 ticks; only an axe or magic
                // secateurs finishes one (the Bryophyta script in content/bosses/OldBossScripts sets that rule).
                MonsterDef.of(BossMechanics.GROWTHLING, "Growthling").level(37).stats(10, 40, 38, 40, 1, 1).speed(4)
                        .attack(AttackDef.of("melee", AttackType.SLASH, 5, 1.5, 1))
                        .skin(0x4A7A2A, 0x3A6A1A, 0x2A4A10, 0x9AC04A).size(0.6f, 0.8f, 0.8f),
                // Vardorvis' Strangle spores: destroy them all (any hit) before the tendrils tighten (approx: the OSRS spores
                // are clicked on the screen; here they hover around the strangled player).
                MonsterDef.of(BossMechanics.STRANGLER_SPORE, "Spore").level(1).stats(1, 1, 1, 1, 1, 1)
                        .attack(AttackDef.of("inert", AttackType.MAGIC, 0, 0.5, 1)).passive()
                        .skin(0x8AC04A, 0x6A9A2A, 0x4A7A1A, 0xD0FF60).size(0.5f, 0.5f, 0.7f).moveSpeed(0.0),
                // Phosani's sleepwalkers: 10 hitpoints, die to any hit; walk to the Nightmare (moved by the script).
                MonsterDef.of(BossMechanics.PHOSANI_SLEEPWALKER, "Sleepwalker").level(1).stats(10, 1, 1, 1, 1, 1)
                        .attack(AttackDef.of("inert", AttackType.MAGIC, 0, 0.5, 1)).passive()
                        .skin(0x8A7AA0, 0x5A4A70, 0x3A2A50, 0xC0A0FF).size(0.6f, 1.8f, 1.0f).moveSpeed(0.0));
    }

    static BossDef yama() {
        return BossDef.of("yama", "Yama").level(1238).stats(2500, 320, 350, 225, 250, 210).speed(8)
                .attackBonus(50, 100, 100).defenceBonus(100, 80, 333, 0, 220).attributes("demon")
                .attack(YAMA_FIRE).attack(YAMA_SHADOW).attack(YAMA_SLASH).attack(YAMA_METEOR).attack(YAMA_GLYPH)
                // The Yama script (content/bosses/ModernScripts) runs the fight: glyph specials, shadow waves, fire streaks, void
                // flares, the Judge of Yama between phases and phase 3's Shadow Crash; these phase lists only mark the thresholds.
                .phase(66, "Yama slams the pommel of his axe into the ground!", 1.0, false, YAMA_FIRE, YAMA_SHADOW,
                        YAMA_SLASH, YAMA_METEOR, YAMA_SHADOW_CRASH)
                // Phase 3: Yama attacks every 7 ticks with higher defences (wiki "Phase 3" stat line).
                .phase(33, "Yama: Enough! You will burn!", 1.0, false, YAMA_FIRE, YAMA_SHADOW, YAMA_SLASH, YAMA_METEOR,
                        YAMA_SHADOW_CRASH, YAMA_GLYPH)
                // No .minion(): the script spawns and tracks its own void flares (a BossDef minion would make the engine's
                // idle SUMMON attack spawn untracked ones).
                // Unique table 1/120 per solo kill: soulflame horn 2/5, each oathplate piece 1/5. Pet Yami 1/2,500.
                .drops(yamaDrops().toArray(new Drop[0]))
                .pet("pet_yami").at("yamas_lair").arena(15, "lava").size(1.8f, 3.4f, 2.0f).moveSpeed(0.22); // chases melee attackers
    }

    /**
     * Yama, solo kill at full contribution (OSRS Wiki "Yama" drop table, effective rates after the pre-rolls; 1/N rounded):
     * uniques (soulflame horn 2/600, each oathplate piece 1/600), dossier 1/12.1, forgotten lockbox 1/33, 12 oathplate shards
     * 1/17.07; the 78-slot main table (/95.11 effective); elite clue 1/30 (the pet is the boss's .pet()). The supply drop
     * (15/95.11, one of each pair) is split into its six lines.
     */
    static List<Drop> yamaDrops() {
        return List.of(Drop.of("soulflame_horn", 1, 1, 300), Drop.of("oathplate_helm", 1, 1, 600), Drop.of("oathplate_chest", 1, 1, 600),
                Drop.of("oathplate_legs", 1, 1, 600), Drop.of("dossier", 1, 1, 12), Drop.of("forgotten_lockbox", 1, 1, 33),
                Drop.of("oathplate_shards", 12, 12, 17),
                Drop.of("pineapple_pizza", 3, 4, 13), Drop.of("wild_pie", 3, 4, 13), Drop.of("prayer_potion(3)", 2, 2, 13),
                Drop.of("super_restore_mix", 2, 2, 13), Drop.of("super_combat_potion(1)", 1, 1, 13), Drop.of("zamorak_mix", 1, 1, 13),
                Drop.of("rune_chainbody", 8, 8, 19), Drop.of("battlestaff", 40, 40, 24), Drop.of("rune_platebody", 8, 8, 32),
                Drop.of("dragon_plateskirt", 1, 1, 48), Drop.of("dragon_platelegs", 1, 1, 48),
                Drop.of("blood_rune", 400, 400, 32), Drop.of("law_rune", 150, 150, 32), Drop.of("smoke_rune", 350, 350, 48),
                Drop.of("soul_rune", 500, 500, 48), Drop.of("soul_rune", 1000, 1000, 48), Drop.of("fire_rune", 40000, 40000, 95),
                Drop.of("wrath_rune", 800, 800, 95),
                Drop.of("aether_catalyst", 850, 850, 14), Drop.of("diabolic_worms", 90, 90, 14), Drop.of("barrel_of_demonic_tallow", 1, 1, 19),
                Drop.of("chasm_teleport_scroll", 6, 6, 24), Drop.of("emerald", 40, 40, 32), Drop.of("ruby", 40, 40, 32),
                Drop.of("diamond", 40, 40, 32), Drop.of("onyx_bolt_tips", 150, 150, 95),
                Drop.of("clue_scroll_elite", 1, 1, 30));
    }

    /**
     * The Maggot King (OSRS Wiki "Maggot King", 15 July 2026 rates): uniques 1/340 and 1/520, the 159-slot main table (1/N
     * rounded; stymphike tartare and the dull ancient medal come together) and the elite clue 1/40.
     */
    static List<Drop> maggotKingDrops() {
        return List.of(Drop.of("elder_venator_fang", 1, 1, 340), Drop.of("crimson_kisten", 1, 1, 520),
                Drop.of("stymphike_tartare", 2, 4, 4), Drop.of("dull_ancient_medal", 1, 1, 4),
                Drop.of("uncut_ruby", 6, 13, 11), Drop.of("stymphike_feather", 51, 74, 16), Drop.of("mort_myre_fungus", 50, 76, 16),
                Drop.of("letvek", 23, 34, 16), Drop.of("runite_ore", 12, 15, 32), Drop.of("blood_sac", 8, 11, 32),
                Drop.of("gold_ore", 24, 32, 32), Drop.of("mithril_ore", 39, 41, 32), Drop.of("adamantite_ore", 30, 40, 16),
                Drop.of("orikalkum_gravel", 4, 5, 40), Drop.of("venator_fang", 112, 179, 80), Drop.of("venator_tooth", 178, 219, 80),
                Drop.of("tarnished_spear", 1, 1, 13), Drop.of("tarnished_ring", 1, 1, 27), Drop.of("tarnished_bracelet", 1, 1, 53),
                Drop.of("tarnished_necklace", 1, 1, 53), Drop.of("tarnished_amulet", 1, 1, 53), Drop.of("tarnished_battleaxe", 1, 1, 80),
                Drop.of("tarnished_longsword", 1, 1, 80), Drop.of("tarnished_halberd", 1, 1, 80), Drop.of("tarnished_2h_sword", 1, 1, 80),
                Drop.of("clue_scroll_elite", 1, 1, 40));
    }

    static BossDef doom() {
        // The "Deep Delve" (delve 9+) stat line: drops roll at their best rate from delve 9 on.
        return BossDef.of("doom_of_mokhaiotl", "Doom of Mokhaiotl").level(908).stats(625, 300, 190, 90, 275, 110).speed(4)
                .attackBonus(210, 125, 120).defenceBonus(300, 300, 60, 160, 160).attributes("demon")
                .attack(DOOM_MELEE).attack(DOOM_MAGIC).attack(DOOM_RANGED).attack(DOOM_CHARGE).attack(DOOM_ACID)
                .phase(50, "The Doom of Mokhaiotl burrows into its shell and the cavern shakes!", 1.1, false,
                        DOOM_MELEE, DOOM_MAGIC, DOOM_RANGED, DOOM_CHARGE, DOOM_ACID)
                // Delve 9+: each unique 1/540, Dom 1/250 (rolled by the pet engine).
                .drops(Drop.of("demon_tear", 20, 60, 1), // approx
                        Drop.of("mokhaiotl_cloth", 1, 1, 540), Drop.of("eye_of_ayak", 1, 1, 540), Drop.of("avernic_treads", 1, 1, 540),
                        Drop.of("death_rune", 200, 400, 8), Drop.of("soul_rune", 150, 300, 8), Drop.of("runite_ore", 6, 12, 10),
                        Drop.of("adamantite_bar", 10, 20, 10), Drop.of("coins", 25000, 75000, 4), Drop.of("clue_scroll_elite", 1, 1, 40)) // approx
                .pet("pet_dom").at("mokhaiotl_lair").arena(14, "crypt").size(2.2f, 2.4f, 2.2f).moveSpeed(0.14);
    }

    static BossDef gemstoneCrab() {
        // An AFK boss: never attacks, 50,000 hitpoints, burrows away after ten minutes. Top damage dealers then mine its
        // shell for gems.
        return BossDef.of("gemstone_crab", "Gemstone Crab").level(160).stats(50000, 1, 1, 1, 1, 1).speed(7)
                .defenceBonus(0, 0, 0, 0, 0).attack(BossMinions.inert()).passive()
                .drops(Drop.of("uncut_sapphire", 1, 3, 2), Drop.of("uncut_emerald", 1, 2, 3), Drop.of("uncut_ruby", 1, 2, 4), // approx
                        Drop.of("uncut_diamond", 1, 1, 8), Drop.of("uncut_dragonstone", 1, 1, 64), Drop.of("uncut_opal", 2, 5, 3),
                        Drop.of("uncut_jade", 2, 4, 3), Drop.of("uncut_red_topaz", 1, 3, 4), Drop.of("clue_scroll_hard", 1, 1, 40))
                .at("gemstone_crab_mine").arena(12, "jungle").size(2.4f, 1.6f, 2.0f).moveSpeed(0.0);
    }

    static BossDef shellbane() {
        // A Slayer boss: OSRS only lets it be fought on a gryphon task (or for its elite clue step); kills count for the task.
        // The task-only rule is enforced by the Minecraft side (BossesHooks).
        return BossDef.of("shellbane_gryphon", "Shellbane gryphon").level(235).stats(400, 160, 210, 120, 100, 150).speed(5)
                .attackBonus(0, 0, 20).defenceBonus(10, 20, 40, 100, 60).flying()
                .attack(SHELLBANE_MELEE).attack(SHELLBANE_RANGED).attack(SHELLBANE_WHIRLWIND).attack(SHELLBANE_KNOCKBACK)
                .slayer(51, "gryphons")
                .drops(Drop.of("belles_folly_tarnished", 1, 1, 75), Drop.of("jar_of_feathers", 1, 1, 1300), // approx
                        Drop.of("big_bones", 1, 1, 1), Drop.of("rune_arrow", 30, 60, 8), Drop.of("rune_full_helm", 1, 1, 16),
                        Drop.of("adamantite_bar", 3, 6, 12), Drop.of("coins", 3000, 12000, 4), Drop.herbs(10), Drop.seeds(18),
                        Drop.of("clue_scroll_elite", 1, 1, 150)) // approx
                .pet("pet_gull").at("shellbane_gryphon_cave").arena(12, "sand").size(1.6f, 2.0f, 1.8f).moveSpeed(0.26);
    }

    static BossDef brutus() {
        // F2P: the Lumbridge cow field's bull, unlocked by The Ides of Milk.
        return BossDef.of("brutus", "Brutus").level(30).stats(58, 12, 25, 10, 8, 1).speed(5)
                .defenceBonus(-7, -7, -7, -3, -7)
                .attack(BRUTUS_HEADBUTT).attack(BRUTUS_CHARGE)
                // Wiki: bull bones and a raw t-bone steak always (big bones stand in; approx), pre-roll mooleta 5/150, bottomless
                // milk bucket 4/150, cow slippers 1/150, then the 81-slot members' table; beginner clue 1/15, easy clue 1/40.
                .drops(Drop.always("big_bones"), Drop.of("mooleta", 1, 1, 30),
                        Drop.of("bottomless_milk_bucket", 1, 1, 38), Drop.of("cow_slippers", 1, 1, 150),
                        Drop.of("iron_full_helm", 1, 1, 40), Drop.of("iron_platebody", 1, 1, 40), Drop.of("iron_platelegs", 1, 1, 81),
                        Drop.of("iron_plateskirt", 1, 1, 81), Drop.of("iron_arrow", 14, 14, 8), Drop.of("air_rune", 29, 29, 8),
                        Drop.of("mind_rune", 18, 18, 10), Drop.of("chaos_rune", 12, 12, 40), Drop.of("potato_seed", 3, 3, 8),
                        Drop.of("acorn", 2, 2, 16), Drop.of("cowhide", 1, 1, 8), Drop.of("oak_logs", 2, 2, 16), Drop.of("logs", 2, 2, 16),
                        Drop.of("coins", 60, 120, 3), // F2P coin rows
                        Drop.of("clue_scroll_beginner", 1, 1, 15), Drop.of("clue_scroll_easy", 1, 1, 40))
                .pet("pet_beef").at("brutus_pen").arena(11, "jungle").size(1.4f, 1.8f, 1.4f).moveSpeed(0.24);
    }

    static BossDef demonicBrutus() {
        // Hard mode: feed Brutus an abyssal potato (plucked in the Scar after Desert Treasure II). Its only drop is the Brutus
        // slippers (always); Beef 1/400 is rolled by the pet engine. Despite the name it is not a demon (wiki, DPS calculator).
        return BossDef.of("demonic_brutus", "Demonic Brutus").level(1224).stats(750, 380, 418, 200, 272, 1).speed(5)
                .defenceBonus(182, 65, 216, 520, 418)
                .attack(DEMONIC_HORNS).attack(DEMONIC_CHARGE).attack(DEMONIC_STOMP).attack(DEMONIC_FIRE)
                .phase(50, "Demonic Brutus is wreathed in hellfire!", 1.15, false, DEMONIC_HORNS, DEMONIC_CHARGE, DEMONIC_STOMP,
                        DEMONIC_FIRE)
                .drops(Drop.always("brutus_slippers"))
                .key("abyssal_potato").pet("pet_beef").at("brutus_pen").arena(12, "lava").size(1.6f, 2.2f, 1.7f).moveSpeed(0.26);
    }

    static BossDef maggotKing() {
        return BossDef.of("maggot_king", "Maggot King").level(741).stats(1500, 200, 250, 200, 200, 300).speed(5) // speed approx
                .attackBonus(400, 200, 200).defenceBonus(172, 100, 45, 150, 158)
                .attack(MAGGOT_MAGIC).attack(MAGGOT_RANGED).attack(MAGGOT_MELEE).attack(MAGGOT_SWARM)
                // "Roaring": its Defence drops from 200 to 100 while it roars.
                .phase(40, "The Maggot King roars, its hide splitting open!", 1.1, false, MAGGOT_MAGIC, MAGGOT_RANGED, MAGGOT_MELEE,
                        MAGGOT_SWARM, MAGGOT_ROAR)
                // Any unique 1/205.6: elder venator fang 1/340, crimson kisten 1/520. Maggot marquess 1/3,500 (open-stomach).
                .drops(maggotKingDrops().toArray(new Drop[0]))
                .pet("pet_maggot_marquess").at("maggot_kings_hive").arena(14, "crypt").size(2.2f, 2.6f, 2.0f).moveSpeed(0.0);
    }

    /** Uniques and pets with their OSRS equipment bonuses (wiki infobox values). */
    static void items(ItemSink items) {
        // Round 29: Yama and Maggot King common loot (OSRS Wiki names; values approx where the wiki gives none).
        items.add("diabolic_worms", "Diabolic worms", Kind.SIMPLE, "bait", 0x8A2A2A).value(60).stack(100000) // value approx
                .examine("Worms from the depths of the Chasm of Fire.");
        items.add("barrel_of_demonic_tallow", "Barrel of demonic tallow (full)", Kind.SIMPLE, "pot_item", 0x5A3A2A).value(1000) // value approx
                .examine("A barrel of demonic tallow.");
        items.add("chasm_teleport_scroll", "Chasm teleport scroll", Kind.SIMPLE, "note", 0xC04020).value(200).stack(100000) // value approx
                .examine("A scroll that teleports you to the Chasm of Fire.");
        items.add("forgotten_lockbox", "Forgotten lockbox", Kind.SIMPLE, "casket", 0x6A5A4A).value(1000) // value approx
                .examine("A lockbox left behind by one of Yama's victims.");
        items.add("stymphike_tartare", "Stymphike tartare", Kind.FOOD, "meat_cooked", 0xB04040).heal(18).value(300) // heal approx
                .examine("Raw, but perfectly safe to eat.");
        items.add("dull_ancient_medal", "Dull ancient medal", Kind.SIMPLE, "mark", 0xA08A5A).value(500).stack(100000) // value approx
                .examine("An old medal, dull with age.");
        items.add("stymphike_feather", "Stymphike feather", Kind.SIMPLE, "secondary", 0x8A6A4A).value(20).stack(100000) // value approx
                .examine("A sharp feather from a stymphike.");
        items.add("letvek", "Letvek", Kind.SIMPLE, "secondary", 0x6A8A3A).value(50).stack(100000) // value approx
                .examine("A pungent Varlamoran root.");
        items.add("orikalkum_gravel", "Orikalkum gravel", Kind.SIMPLE, "ore", 0xC08A3A).value(500).stack(100000) // value approx
                .examine("Gravel with flecks of orikalkum.");
        items.add("blood_sac", "Blood sac", Kind.SIMPLE, "secondary", 0x8A1A1A).value(100).stack(100000) // value approx
                .examine("A sac full of blood.");
        items.add("venator_fang", "Venator fang", Kind.SIMPLE, "secondary", 0xE0D8C0).value(30).stack(100000) // value approx
                .examine("A small fang from a venator.");
        items.add("venator_tooth", "Venator tooth", Kind.SIMPLE, "secondary", 0xD8D0B8).value(20).stack(100000) // value approx
                .examine("A tooth from a venator.");
        String[][] tarnished = {{"spear", "spear"}, {"ring", "ring"}, {"bracelet", "ring"}, {"necklace", "necklace"}, {"amulet", "amulet"},
                {"battleaxe", "battleaxe"}, {"longsword", "longsword"}, {"halberd", "spear"}, {"2h_sword", "2h_sword"}};
        for (String[] t : tarnished) {
            String name = "Tarnished " + t[0].replace("2h_sword", "2h sword");
            items.add("tarnished_" + t[0], name, Kind.SIMPLE, t[1], 0x7A6A4A).value(5000) // value approx
                    .examine("Old and tarnished. It might be worth something to a collector.");
        }
        // ---------------- Yama ----------------
        BossItems.wearItem(items, "oathplate_helm", "Oathplate helm", Kind.ARMOUR, "full_helm", 0x5A2A2A, 15_000_000, Slot.HEAD,
                st(0, 10, 0, -2, -7, 50, 72, 45, 0, 50, 6, 0, 0, 0), Skill.DEFENCE, 85).examine("A helm sworn to Yama.");
        BossItems.wearItem(items, "oathplate_chest", "Oathplate chest", Kind.ARMOUR, "platebody", 0x5A2A2A, 30_000_000, Slot.CHEST,
                st(0, 16, 0, -16, -18, 105, 128, 100, -5, 112, 4, 0, 0, 0), Skill.DEFENCE, 85).examine("A chestplate sworn to Yama.");
        BossItems.wearItem(items, "oathplate_legs", "Oathplate legs", Kind.ARMOUR, "platelegs", 0x5A2A2A, 25_000_000, Slot.LEGS,
                st(0, 12, 0, -12, -14, 75, 100, 73, -3, 81, 2, 0, 0, 0), Skill.DEFENCE, 85).examine("Platelegs sworn to Yama.");
        BossItems.weaponItem(items, "soulflame_horn", "Soulflame horn", Kind.MELEE_WEAPON, "horn", 0xE05010, 40_000_000,
                st(2, 0, 21, 0, 0, 0, 7, 3, 11, 0, 23, 0, 1, 0), 5, AttackType.CRUSH, WeaponClass.MELEE, Skill.ATTACK, 75)
                .examine("Its special attack empowers your next spell or attack.");
        items.add("oathplate_shards", "Oathplate shards", Kind.SIMPLE, "shard", 0x7A3A2A).value(50_000).stack(100000)
                .examine("Fragments of oathplate. Used to smith oathplate armour.");
        BossItems.simpleItem(items, "dossier", "Dossier", "book", 0x6A4A2A, 1, "Details of a contract for Yama.");
        BossItems.petItem(items, "pet_yami", "Yami", "Yama", 0x5A2A4A);

        // ---------------- Doom of Mokhaiotl ----------------
        // mokhaiotl_cloth, demon_tear and the Maggot King's elder_venator_fang are in osrs/content/CommonItems (the systems
        // module upgrades them into confliction gauntlets and the necklace of rupture).
        BossItems.weaponItem(items, "eye_of_ayak", "Eye of ayak", Kind.STAFF, "staff_orb", 0x8A2A6A, 50_000_000,
                st(0, 0, 0, 30, 0, 1, 5, 5, 10, 0, 0, 0, 0, 2), 3, AttackType.MAGIC, WeaponClass.STAFF, Skill.MAGIC, 82)
                .examine("A demonic eye on a staff. Charged with demon tears.");
        BossItems.wearItem(items, "avernic_treads", "Avernic treads", Kind.ARMOUR, "boots", 0x8A1A2A, 30_000_000, Slot.FEET,
                st(5, 5, 5, 11, 15, 21, 25, 25, 10, 10, 4, 2, 1, 0), Skill.DEFENCE, 85).examine("Tribrid boots from the depths.");
        BossItems.petItem(items, "pet_dom", "Dom", "Doom of Mokhaiotl", 0x6A3A8A);

        // ---------------- Shellbane gryphon ----------------
        BossItems.simpleItem(items, "belles_folly_tarnished", "Belle's folly (tarnished)", "tail", 0x8A7A5A, 2_000_000,
                "A tarnished relic of the Great Conch.");
        BossItems.jarItem(items, "jar_of_feathers", "Jar of feathers", 0xC0B090);
        BossItems.petItem(items, "pet_gull", "Gull", "Shellbane gryphon", 0xC0B090);

        // ---------------- Brutus ----------------
        BossItems.wearItem(items, "mooleta", "Mooleta", Kind.SHIELD, "defender", 0xF0F0E0, 1, Slot.SHIELD,
                st(5, 7, 7, -1, -1, 4, 8, 8, -2, -1, 1, 0, 0, 0)).examine("A defender shaped like a cow's head.");
        BossItems.wearItem(items, "cow_slippers", "Cow slippers", Kind.ARMOUR, "boots", 0xF0F0E0, 1, Slot.FEET,
                st(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)).examine("Moo.");
        BossItems.wearItem(items, "brutus_slippers", "Brutus slippers", Kind.ARMOUR, "boots", 0x8A1A1A, 1, Slot.FEET,
                st(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)).examine("Slippers of a demonic bull.");
        BossItems.simpleItem(items, "bottomless_milk_bucket", "Bottomless milk bucket", "bucket", 0xF0F0F0, 1,
                "It never runs out of milk.");
        BossItems.petItem(items, "pet_beef", "Beef", "Brutus", 0x6A4A2A);

        // ---------------- Maggot King ----------------
        BossItems.weaponItem(items, "crimson_kisten", "Crimson kisten", Kind.MELEE_WEAPON, "club", 0x8A1A2A, 30_000_000,
                st(0, 0, 80, 0, 0, 0, 0, 0, 0, 0, 56, 0, 0, 0), 4, AttackType.CRUSH, WeaponClass.MELEE, Skill.ATTACK, 80)
                .examine("A blood-red two-handed club.");
        BossItems.petItem(items, "pet_maggot_marquess", "Maggot marquess", "Maggot King", 0xC0A060);
    }
}
