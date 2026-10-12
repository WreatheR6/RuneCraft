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
    public static final AttackDef YAMA_FIRE = AttackDef.of("fire_orb", AttackType.RANGED, 46, 16, 3)
            .telegraph(2, "Yama's orb blazes with fire...").particle("minecraft:flame");
    public static final AttackDef YAMA_SHADOW = AttackDef.of("shadow_orb", AttackType.MAGIC, 46, 16, 3)
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
                // Yama's void flares (fire and shadow); they explode for up to 45 if left alive.
                MonsterDef.of("yama_void_flare", "Void Flare").level(130).stats(140, 1, 1, 100, 200, 1).speed(4)
                        .attack(AttackDef.of("flare_burst", AttackType.MAGIC, 45, 3, 1).special(Special.AOE, 3)
                                .telegraph(3, "The void flare pulses violently!"))
                        .attributes("demon").skin(0x3A1A4A, 0xE05010, 0x2A0A3A, 0xFF8A1E).size(0.9f, 1.4f, 1.0f).moveSpeed(0.12));
    }

    static BossDef yama() {
        return BossDef.of("yama", "Yama").level(1238).stats(2500, 320, 350, 225, 250, 210).speed(8)
                .attackBonus(50, 100, 100).defenceBonus(100, 80, 333, 0, 220).attributes("demon")
                .attack(YAMA_FIRE).attack(YAMA_SHADOW).attack(YAMA_SLASH).attack(YAMA_METEOR).attack(YAMA_GLYPH)
                .phase(66, "The Judge of Yama steps forth as void flares burst from the ground!", 1.0, true, YAMA_FIRE, YAMA_SHADOW,
                        YAMA_SLASH, YAMA_METEOR, YAMA_SHADOW_CRASH)
                // Phase 3: Yama attacks every 7 ticks with higher defences (wiki "Phase 3" stat line).
                .phase(33, "Yama: Enough! You will burn!", 1.15, true, YAMA_FIRE, YAMA_SHADOW, YAMA_SLASH, YAMA_METEOR,
                        YAMA_SHADOW_CRASH, YAMA_GLYPH)
                .minion("yama_void_flare", 2)
                // Unique table 1/120 per solo kill: soulflame horn 2/5, each oathplate piece 1/5. Pet Yami 1/2,500.
                .drops(Drop.of("soulflame_horn", 1, 1, 300), Drop.of("oathplate_helm", 1, 1, 600), Drop.of("oathplate_chest", 1, 1, 600),
                        Drop.of("oathplate_legs", 1, 1, 600), Drop.of("dossier", 1, 1, 17),
                        Drop.of("oathplate_shards", 6, 12, 10), // approx
                        Drop.of("soul_rune", 250, 500, 8), Drop.of("blood_rune", 300, 600, 8), Drop.of("fire_rune", 3000, 6000, 8),
                        Drop.of("runite_bar", 5, 10, 10), Drop.of("dragon_bones", 20, 30, 12), Drop.of("infernal_ashes", 10, 20, 10),
                        Drop.of("coins", 30000, 90000, 4), Drop.of("uncut_onyx", 1, 1, 400), Drop.of("clue_scroll_elite", 1, 1, 50)) // approx
                .pet("pet_yami").at("yamas_lair").arena(15, "lava").size(1.8f, 3.4f, 2.0f).moveSpeed(0.0);
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
        // A Slayer boss: OSRS only lets it be fought on a gryphon task (or for its elite clue step).
        return BossDef.of("shellbane_gryphon", "Shellbane gryphon").level(235).stats(400, 160, 210, 120, 100, 150).speed(5)
                .attackBonus(0, 0, 20).defenceBonus(10, 20, 40, 100, 60).flying()
                .attack(SHELLBANE_MELEE).attack(SHELLBANE_RANGED).attack(SHELLBANE_WHIRLWIND).attack(SHELLBANE_KNOCKBACK)
                .slayer(51, null)
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
                .drops(Drop.always("big_bones"), Drop.of("cowhide", 1, 3, 1), Drop.of("mooleta", 1, 1, 30),
                        Drop.of("bottomless_milk_bucket", 1, 1, 38), Drop.of("cow_slippers", 1, 1, 150),
                        Drop.of("iron_platebody", 1, 1, 12), Drop.of("mind_rune", 15, 40, 8), Drop.of("coins", 50, 400, 3),
                        Drop.of("clue_scroll_beginner", 1, 1, 40)) // approx (commons)
                .pet("pet_beef").at("brutus_pen").arena(11, "jungle").size(1.4f, 1.8f, 1.4f).moveSpeed(0.24);
    }

    static BossDef demonicBrutus() {
        // Hard mode (Desert Treasure II and an abyssal potato): only the Brutus slippers are guaranteed; Beef 1/400.
        return BossDef.of("demonic_brutus", "Demonic Brutus").level(1224).stats(750, 380, 418, 200, 272, 1).speed(5)
                .defenceBonus(182, 65, 216, 520, 418).attributes("demon")
                .attack(DEMONIC_HORNS).attack(DEMONIC_CHARGE).attack(DEMONIC_STOMP).attack(DEMONIC_FIRE)
                .phase(50, "Demonic Brutus is wreathed in hellfire!", 1.15, false, DEMONIC_HORNS, DEMONIC_CHARGE, DEMONIC_STOMP,
                        DEMONIC_FIRE)
                .drops(Drop.always("brutus_slippers"), Drop.of("cow_slippers", 1, 1, 50), Drop.of("mooleta", 1, 1, 30), // approx
                        Drop.of("bottomless_milk_bucket", 1, 1, 38), Drop.of("infernal_ashes", 3, 6, 4), Drop.of("coins", 20000, 60000, 4),
                        Drop.of("clue_scroll_elite", 1, 1, 60)) // approx
                .pet("pet_beef").at("brutus_pen").arena(12, "lava").size(1.6f, 2.2f, 1.7f).moveSpeed(0.26);
    }

    static BossDef maggotKing() {
        return BossDef.of("maggot_king", "Maggot King").level(741).stats(1500, 200, 250, 200, 200, 300).speed(5) // speed approx
                .attackBonus(400, 200, 200).defenceBonus(172, 100, 45, 150, 158)
                .attack(MAGGOT_MAGIC).attack(MAGGOT_RANGED).attack(MAGGOT_MELEE).attack(MAGGOT_SWARM)
                // "Roaring": its Defence drops from 200 to 100 while it roars.
                .phase(40, "The Maggot King roars, its hide splitting open!", 1.1, false, MAGGOT_MAGIC, MAGGOT_RANGED, MAGGOT_MELEE,
                        MAGGOT_SWARM, MAGGOT_ROAR)
                // Any unique 1/205.6: elder venator fang 1/340, crimson kisten 1/520. Maggot marquess 1/3,500 (open-stomach).
                .drops(Drop.of("elder_venator_fang", 1, 1, 340), Drop.of("crimson_kisten", 1, 1, 520),
                        Drop.of("blood_rune", 300, 600, 8), Drop.of("soul_rune", 150, 300, 8), Drop.of("runite_bar", 4, 8, 10),
                        Drop.of("coins", 25000, 70000, 4), Drop.of("clue_scroll_elite", 1, 1, 50)) // approx
                .pet("pet_maggot_marquess").at("maggot_kings_hive").arena(14, "crypt").size(2.2f, 2.6f, 2.0f).moveSpeed(0.0);
    }

    /** Uniques and pets with their OSRS equipment bonuses (wiki infobox values). */
    static void items(ItemSink items) {
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
        BossItems.simpleItem(items, "mokhaiotl_cloth", "Mokhaiotl cloth", "cloth", 0x6A3A8A, 30_000_000,
                "Woven from the Doom's flesh. Upgrades a tormented bracelet into confliction gauntlets.");
        BossItems.weaponItem(items, "eye_of_ayak", "Eye of ayak", Kind.STAFF, "staff_orb", 0x8A2A6A, 50_000_000,
                st(0, 0, 0, 30, 0, 1, 5, 5, 10, 0, 0, 0, 0, 2), 3, AttackType.MAGIC, WeaponClass.STAFF, Skill.MAGIC, 82)
                .examine("A demonic eye on a staff. Charged with demon tears.");
        BossItems.wearItem(items, "avernic_treads", "Avernic treads", Kind.ARMOUR, "boots", 0x8A1A2A, 30_000_000, Slot.FEET,
                st(5, 5, 5, 11, 15, 21, 25, 25, 10, 10, 4, 2, 1, 0), Skill.DEFENCE, 85).examine("Tribrid boots from the depths.");
        items.add("demon_tear", "Demon tear", Kind.SIMPLE, "gem", 0xC03050).value(1).stack(100000)
                .examine("Charges the eye of ayak.");
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
        BossItems.simpleItem(items, "elder_venator_fang", "Elder venator fang", "fang", 0xE0D0B0, 40_000_000,
                "Combine with a necklace of anguish to make a necklace of rupture (84 Crafting).");
        BossItems.weaponItem(items, "crimson_kisten", "Crimson kisten", Kind.MELEE_WEAPON, "club", 0x8A1A2A, 30_000_000,
                st(0, 0, 80, 0, 0, 0, 0, 0, 0, 0, 56, 0, 0, 0), 4, AttackType.CRUSH, WeaponClass.MELEE, Skill.ATTACK, 80)
                .examine("A blood-red two-handed club.");
        BossItems.petItem(items, "pet_maggot_marquess", "Maggot marquess", "Maggot King", 0xC0A060);
    }
}
