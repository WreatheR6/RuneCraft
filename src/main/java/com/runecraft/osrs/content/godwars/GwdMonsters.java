package com.runecraft.osrs.content.godwars;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Region;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.godwars.GodWarsFactions.God;
import com.runecraft.osrs.data.Drop;

import java.util.ArrayList;
import java.util.List;

/**
 * God Wars followers (they give kill count), the generals' bodyguards, boss minions and the
 * Wilderness revenants. Followers have no natural spawns: they are kept stocked around the God
 * Wars chambers by the module's camp spawner (see {@link GwdPlaces#camps()}).
 *
 * <p>Stats follow the OSRS wiki where known; values marked "approx." are estimates.
 */
public final class GwdMonsters {
    private GwdMonsters() {
    }

    public static List<MonsterDef> all() {
        List<MonsterDef> list = new ArrayList<>();
        followers(list);
        bodyguards(list);
        minions(list);
        revenants(list);
        return list;
    }

    private static Drop[] followerDrops(Drop... extra) {
        List<Drop> d = new ArrayList<>(List.of(extra));
        d.add(Drop.of("coins", 100, 1500, 3));
        d.add(Drop.herbs(16));
        // Ecumenical keys: rolled on kill in the Wilderness God Wars Dungeon only (GwdKillcount), 1/60 to 1/40 by CA tier.
        d.add(Drop.of("clue_scroll_hard", 1, 1, 128));
        return d.toArray(new Drop[0]);
    }

    /** Spiritual warrior / ranger / mage of a god (Slayer 68 / 63 / 83). Stats approx. per god. */
    private static void spirituals(List<MonsterDef> list, God god, int colour) {
        String g = god.id();
        String n = god.displayName();
        // OSRS Wiki: the Armadylean spiritual warrior is an aviansie that throws (Ranged, max 15); the others fight in melee.
        AttackDef warriorAttack = god == God.ARMADYL
                ? AttackDef.of("feather throw", AttackType.RANGED, 15, 10, 10).particle("minecraft:cloud")
                : AttackDef.of("melee", god == God.SARADOMIN || god == God.BANDOS ? AttackType.CRUSH : AttackType.SLASH, 16, 2.5, 10);
        list.add(MonsterDef.of("gwd_spiritual_warrior_" + g, "Spiritual warrior (" + n + ")").level(123)
                .stats(100, 98, 102, 98, 1, 1).speed(4).defenceBonus(80, 80, 80, 10, 60)
                .attack(warriorAttack)
                .drops(followerDrops(Drop.always("bones"), Drop.of("rune_full_helm", 1, 1, 64), Drop.of("adamant_platebody", 1, 1, 48)))
                .slayer(68, "spiritual_creatures").skin(0xB0B0C8, colour, 0x4A4A5A, colour).size(0.6f, 1.95f, 1.1f));
        list.add(MonsterDef.of("gwd_spiritual_ranger_" + g, "Spiritual ranger (" + n + ")").level(115)
                .stats(106, 1, 1, 85, 1, 104).speed(4).defenceBonus(60, 60, 60, 40, 90)
                .attack(AttackDef.of("arrow", AttackType.RANGED, 18, 10, 10).particle("minecraft:crit"))
                .drops(followerDrops(Drop.always("bones"), Drop.of("rune_arrow", 10, 25, 8), Drop.of("magic_shortbow", 1, 1, 128)))
                .slayer(63, "spiritual_creatures").skin(0xB0B0C8, colour, 0x3A5A2A, colour).size(0.6f, 1.95f, 1.1f));
        list.add(MonsterDef.of("gwd_spiritual_mage_" + g, "Spiritual mage (" + n + ")").level(121)
                .stats(75, 1, 1, 86, 160, 1).speed(5).defenceBonus(30, 30, 30, 120, 30)
                .attack(AttackDef.of("god spell", AttackType.MAGIC, 18, 10, 10).particle("minecraft:witch"))
                .drops(followerDrops(Drop.always("bones"), Drop.of("blood_rune", 10, 25, 8), Drop.of("law_rune", 10, 20, 8),
                        Drop.of("ancient_staff", 1, 1, 512)))
                .slayer(83, "spiritual_creatures").skin(0xB0B0C8, colour, colour, 0xE0E0E0).size(0.6f, 1.95f, 1.1f));
    }

    private static void followers(List<MonsterDef> list) {
        // --- Armadyl ---
        list.add(MonsterDef.of("gwd_aviansie", "Aviansie").level(79).stats(70, 1, 1, 70, 1, 80).speed(5)
                .defenceBonus(20, 20, 20, 40, 60)
                .attack(AttackDef.of("feather throw", AttackType.RANGED, 10, 10, 10).particle("minecraft:cloud"))
                .immune(AttackType.STAB, AttackType.SLASH, AttackType.CRUSH)
                .drops(followerDrops(Drop.always("bones"), Drop.of("adamantite_bar", 1, 3, 10), Drop.of("rune_arrow", 5, 15, 16)))
                .flying().model("gwd_aviansie").size(0.7f, 2.0f, 1.2f));
        list.add(MonsterDef.of("gwd_aviansie_elite", "Aviansie").level(131).stats(124, 1, 1, 110, 1, 130).speed(5)
                .defenceBonus(40, 40, 40, 60, 80)
                .attack(AttackDef.of("feather barrage", AttackType.RANGED, 15, 10, 10).particle("minecraft:cloud"))
                .immune(AttackType.STAB, AttackType.SLASH, AttackType.CRUSH)
                .drops(followerDrops(Drop.always("bones"), Drop.of("adamantite_bar", 2, 4, 6), Drop.of("runite_bar", 1, 1, 64)))
                .flying().model("gwd_aviansie_elite").size(0.7f, 2.0f, 1.3f));
        spirituals(list, God.ARMADYL, 0xD8D0B0);

        // --- Bandos ---
        list.add(MonsterDef.of("gwd_goblin", "Goblin").level(17).stats(12, 13, 14, 12, 1, 1).speed(4)
                .attack(AttackDef.of("spear", AttackType.STAB, 3, 2.5, 10))
                .drops(followerDrops(Drop.always("bones"), Drop.of("bronze_arrow", 5, 10, 8)))
                .model("gwd_goblin").size(0.5f, 1.3f, 1.0f));
        list.add(MonsterDef.of("gwd_hobgoblin", "Hobgoblin").level(47).stats(55, 40, 45, 38, 1, 1).speed(4)
                .attack(AttackDef.of("spear", AttackType.STAB, 7, 2.5, 10))
                .drops(followerDrops(Drop.always("bones"), Drop.of("limpwurt_root", 1, 1, 16)))
                .skin(0x8A7A3A, 0x6A3A2A, 0x4A3A2A, 0x5A5A3A).size(0.6f, 2.0f, 1.0f));
        list.add(MonsterDef.of("gwd_ork", "Ork").level(107).stats(110, 105, 110, 100, 1, 1).speed(4)
                .defenceBonus(30, 30, 30, 0, 20)
                .attack(AttackDef.of("melee", AttackType.CRUSH, 14, 2.5, 10))
                .drops(followerDrops(Drop.always("big_bones"), Drop.of("rune_full_helm", 1, 1, 64)))
                .skin(0x6A7A3A, 0x5A4030, 0x3A2A1A, 0x8A8A8A).size(0.8f, 2.2f, 1.1f));
        list.add(MonsterDef.of("gwd_ogre", "Ogre").level(58).stats(60, 43, 43, 43, 1, 1).speed(6)
                .attack(AttackDef.of("club", AttackType.CRUSH, 8, 2.5, 10))
                .drops(followerDrops(Drop.always("big_bones")))
                .skin(0x9A8A4A, 0x6A4A2A, 0x5A3A1A, 0x6A4A2A).size(0.9f, 2.5f, 1.1f));
        list.add(MonsterDef.of("gwd_jogre", "Jogre").level(58).stats(60, 43, 43, 43, 1, 1).speed(6)
                .attack(AttackDef.of("club", AttackType.CRUSH, 8, 2.5, 10))
                .drops(followerDrops(Drop.always("big_bones")))
                .skin(0x5A7A3A, 0x6A4A2A, 0x5A3A1A, 0x6A4A2A).size(0.9f, 2.5f, 1.1f));
        list.add(MonsterDef.of("gwd_cyclops", "Cyclops").level(76).stats(110, 60, 60, 60, 1, 1).speed(5)
                .attack(AttackDef.of("melee", AttackType.CRUSH, 11, 2.5, 10))
                .drops(followerDrops(Drop.always("big_bones"), Drop.of("rune_2h_sword", 1, 1, 128)))
                .skin(0xB89070, 0x5A4030, 0x3A2A1A, 0x202020).size(0.9f, 2.6f, 1.1f));
        spirituals(list, God.BANDOS, 0x6A7A3A);

        // --- Saradomin ---
        list.add(MonsterDef.of("gwd_knight_of_saradomin", "Knight of Saradomin").level(101).stats(135, 98, 95, 96, 1, 1).speed(4)
                .defenceBonus(90, 90, 90, 0, 80)
                .attack(AttackDef.of("sword", AttackType.SLASH, 12, 2.5, 10))
                .drops(followerDrops(Drop.always("bones"), Drop.of("rune_longsword", 1, 1, 64), Drop.of("prayer_potion", 1, 1, 24)))
                .skin(0xC8A080, 0xE8E8F0, 0x3A5AD0, 0xE0C040).size(0.6f, 1.95f, 1.05f));
        list.add(MonsterDef.of("gwd_saradomin_priest", "Saradomin priest").level(113).stats(89, 1, 1, 80, 120, 1).speed(5)
                .defenceBonus(20, 20, 20, 90, 20)
                .attack(AttackDef.of("holy light", AttackType.MAGIC, 16, 10, 10).particle("minecraft:end_rod"))
                .drops(followerDrops(Drop.always("bones"), Drop.of("prayer_potion", 1, 1, 12), Drop.of("law_rune", 5, 15, 8)))
                .skin(0xC8A080, 0xE8E8F0, 0xE8E8F0, 0x3A5AD0).size(0.6f, 1.95f, 1.0f));
        spirituals(list, God.SARADOMIN, 0x3A5AD0);

        // --- Zamorak ---
        list.add(MonsterDef.of("gwd_imp", "Imp").level(7).stats(10, 5, 5, 5, 1, 1).speed(4)
                .attack(AttackDef.of("melee", AttackType.CRUSH, 1, 2.5, 10))
                .drops(followerDrops(Drop.always("fiendish_ashes"), Drop.of("fire_rune", 2, 6, 4)))
                .model("gwd_imp").size(0.5f, 1.0f, 1.0f));
        list.add(MonsterDef.of("gwd_werewolf", "Werewolf").level(93).stats(100, 70, 80, 70, 1, 1).speed(4)
                .attack(AttackDef.of("claws", AttackType.SLASH, 12, 2.5, 10))
                .drops(followerDrops(Drop.always("big_bones"), Drop.of("grey_wolf_fur", 1, 1, 4)))
                .skin(0x6A5A4A, 0x4A3A2A, 0x3A2A1A, 0xE0E0E0).size(0.7f, 2.2f, 1.1f));
        list.add(MonsterDef.of("gwd_bloodveld", "Bloodveld").level(81).stats(120, 70, 70, 50, 1, 1).speed(4)
                .attack(AttackDef.of("tongue", AttackType.CRUSH, 9, 3.0, 10).special(Special.PRAYER_PIERCING, 0))
                .drops(followerDrops(Drop.always("vile_ashes"), Drop.of("blood_rune", 5, 15, 12)))
                .slayer(50, "bloodveld").skin(0xA03030, 0x802020, 0x601818, 0xE0B0B0).size(0.8f, 1.6f, 1.1f));
        list.add(MonsterDef.of("gwd_hellhound", "Hellhound").level(127).stats(116, 105, 104, 102, 1, 1).speed(4)
                .defenceBonus(0, 0, 0, 0, 0)
                .attack(AttackDef.of("bite", AttackType.STAB, 14, 2.5, 10))
                .drops(followerDrops(Drop.always("vile_ashes"), Drop.of("clue_scroll_hard", 1, 1, 64)))
                .model("gwd_hellhound").size(0.9f, 1.2f, 1.3f));
        list.add(MonsterDef.of("gwd_gorak", "Gorak").level(149).stats(128, 130, 140, 140, 1, 1).speed(4) // approx.
                .defenceBonus(40, 40, 40, 40, 40)
                .attack(AttackDef.of("claws", AttackType.CRUSH, 16, 2.5, 10).special(Special.PRAYER_PIERCING, 0))
                .drops(followerDrops(Drop.always("bones"), Drop.of("death_rune", 5, 15, 8)))
                .skin(0x6A5AA0, 0x4A3A80, 0x3A2A60, 0xC0C0E0).size(0.8f, 2.2f, 1.1f));
        list.add(MonsterDef.of("gwd_vampire", "Vampire").level(72).stats(60, 60, 60, 60, 1, 1).speed(4)
                .attack(AttackDef.of("bite", AttackType.STAB, 8, 2.5, 10).special(Special.LIFESTEAL, 50))
                .drops(followerDrops(Drop.always("bones"), Drop.of("chaos_rune", 5, 15, 8)))
                .skin(0xD8D0C8, 0x202030, 0x202030, 0xA02020).size(0.6f, 1.95f, 1.0f));
        list.add(MonsterDef.of("gwd_pyrefiend", "Pyrefiend").level(48).stats(45, 30, 35, 26, 1, 1).speed(4)
                .attack(AttackDef.of("fire", AttackType.MAGIC, 8, 3.0, 10).particle("minecraft:flame"))
                .drops(followerDrops(Drop.of("fire_rune", 10, 30, 4)))
                .slayer(30, "pyrefiends").skin(0xE07818, 0xC05010, 0xA03010, 0xF0D040).size(0.6f, 1.6f, 1.0f));
        // OSRS Wiki (Icefiend, level 18 God Wars Dungeon version): 20 hitpoints, magical melee up to 3, respawn 25.
        list.add(MonsterDef.of("gwd_icefiend", "Icefiend").level(18).stats(20, 12, 16, 19, 1, 1).speed(4)
                .attack(AttackDef.of("frost", AttackType.MAGIC, 3, 3.0, 10).particle("minecraft:snowflake"))
                .drops(followerDrops(Drop.of("water_rune", 5, 10, 4)))
                .slayer(1, "icefiends").skin(0xA0D8F0, 0x80B8E0, 0x6090C0, 0xFFFFFF).size(0.6f, 1.6f, 1.0f));
        spirituals(list, God.ZAMORAK, 0xB02020);
        zarosians(list);
    }

    /**
     * The Ancient Prison's Zarosian followers (OSRS Wiki infoboxes): spiritual warriors (158, crush 20), rangers (158,
     * ranged 20), mages (182, magic 38) and the prison's blood reavers (174, magic 20; Nex's chamber has her own,
     * {@code blood_reaver}). They leave alone a player wearing a Zarosian item and give Zarosian essence.
     */
    private static void zarosians(List<MonsterDef> list) {
        int purple = 0x5A2A6A;
        list.add(MonsterDef.of("gwd_spiritual_warrior_zaros", "Spiritual warrior (Zaros)").level(158).stats(100, 190, 190, 100, 50, 1)
                .speed(5).defenceBonus(100, 100, 80, 0, 250)
                .attack(AttackDef.of("melee", AttackType.CRUSH, 20, 2.5, 10))
                .drops(prisonDrops(Drop.always("bones"), Drop.herbs(16)))
                .slayer(68, "spiritual_creatures").skin(0xB0B0C8, purple, 0x3A2A4A, purple).size(0.6f, 1.95f, 1.1f));
        list.add(MonsterDef.of("gwd_spiritual_ranger_zaros", "Spiritual ranger (Zaros)").level(158).stats(110, 1, 1, 100, 100, 190)
                .speed(5).defenceBonus(20, 0, 20, 300, 50)
                .attack(AttackDef.of("arrow", AttackType.RANGED, 20, 10, 10).particle("minecraft:crit"))
                .drops(prisonDrops(Drop.always("bones"), Drop.of("rune_arrow", 15, 30, 8)))
                .slayer(63, "spiritual_creatures").skin(0xB0B0C8, purple, 0x2A3A2A, purple).size(0.6f, 1.95f, 1.1f));
        list.add(MonsterDef.of("gwd_spiritual_mage_zaros", "Spiritual mage (Zaros)").level(182).stats(125, 1, 1, 100, 200, 1)
                .speed(5).defenceBonus(420, 400, 420, 200, 0).attackBonus(0, 52, 0)
                .attack(AttackDef.of("god spell", AttackType.MAGIC, 38, 10, 10).particle("minecraft:witch"))
                .drops(prisonDrops(Drop.always("bones"), Drop.of("blood_rune", 20, 40, 8)))
                .slayer(83, "spiritual_creatures").skin(0xB0B0C8, purple, purple, 0xE0E0E0).size(0.6f, 1.95f, 1.1f));
        list.add(MonsterDef.of("gwd_blood_reaver", "Blood Reaver").level(174).stats(125, 1, 1, 100, 190, 1).speed(5)
                .defenceBonus(20, 80, 120, 300, 55)
                .attack(AttackDef.of("blood spell", AttackType.MAGIC, 20, 10, 10).particle("minecraft:damage_indicator"))
                .drops(prisonDrops(Drop.of("blood_rune", 10, 30, 8)))
                .model("blood_reaver").size(0.8f, 1.6f, 1.1f));
    }

    /**
     * Drops of the Ancient Prison's followers: a piece of ancient ceremonial robes 1/200 (1/1000 each, as Nex's mages; OSRS
     * Wiki: every NPC of the prison but Nex drops them), coins and a hard clue (approx).
     */
    private static Drop[] prisonDrops(Drop... extra) {
        List<Drop> d = new ArrayList<>(List.of(extra));
        d.add(Drop.of("coins", 500, 2500, 4)); // approx
        for (String piece : new String[]{"mask", "top", "legs", "gloves", "boots"}) {
            d.add(Drop.of("ancient_ceremonial_" + piece, 1, 1, 1000));
        }
        d.add(Drop.of("clue_scroll_hard", 1, 1, 128));
        return d.toArray(new Drop[0]);
    }

    /**
     * A bodyguard's table (corrected from the wiki by {@code MonsterDropsTable}). Its frozen key piece (1/20, OSRS Wiki) only
     * drops during The Frozen Door, so the module's kill hook rolls it, not this table.
     */
    private static Drop[] guardDrops(God god) {
        return new Drop[]{Drop.always("bones"), Drop.of("coins", 1000, 3000, 2),
                Drop.of("super_restore", 1, 1, 12), Drop.of("rune_arrow", 10, 20, 8), Drop.of("clue_scroll_hard", 1, 1, 64)};
    }

    /** The generals' three bodyguards each: one melee, one magic, one ranged. */
    private static void bodyguards(List<MonsterDef> list) {
        // Kree'arra's flock (fly: immune to melee like their general).
        list.add(MonsterDef.of("flight_kilisa", "Flight Kilisa").level(159).stats(159, 124, 118, 175, 1, 1).speed(5)
                .defenceBonus(80, 80, 80, 80, 80)
                .attack(AttackDef.of("melee", AttackType.SLASH, 16, 3.0, 10))
                .drops(guardDrops(God.ARMADYL)).flying().size(0.8f, 2.0f, 1.4f));
        list.add(MonsterDef.of("wingman_skree", "Wingman Skree").level(143).stats(121, 1, 1, 160, 150, 1).speed(5)
                .defenceBonus(60, 60, 60, 60, 60)
                .attack(AttackDef.of("wind strike", AttackType.MAGIC, 15, 12, 10).particle("minecraft:cloud"))
                .drops(guardDrops(God.ARMADYL)).flying().size(0.8f, 2.0f, 1.4f));
        list.add(MonsterDef.of("flockleader_geerin", "Flockleader Geerin").level(149).stats(132, 1, 1, 175, 1, 150).speed(5)
                .defenceBonus(60, 60, 60, 60, 60)
                .attack(AttackDef.of("feathers", AttackType.RANGED, 15, 12, 10).particle("minecraft:crit"))
                .drops(guardDrops(God.ARMADYL)).flying().size(0.8f, 2.0f, 1.4f));
        // Commander Zilyana's guard.
        list.add(MonsterDef.of("starlight", "Starlight").level(149).stats(160, 120, 125, 120, 1, 1).speed(2)
                .defenceBonus(20, 20, 20, 20, 20)
                .attack(AttackDef.of("melee", AttackType.SLASH, 16, 2.5, 10))
                .drops(guardDrops(God.SARADOMIN)).size(0.6f, 1.95f, 1.2f));
        list.add(MonsterDef.of("growler", "Growler").level(139).stats(146, 1, 1, 120, 150, 1).speed(5)
                .defenceBonus(20, 20, 20, 20, 20)
                .attack(AttackDef.of("magic roar", AttackType.MAGIC, 16, 12, 10).particle("minecraft:end_rod"))
                .drops(guardDrops(God.SARADOMIN)).size(0.9f, 1.3f, 1.3f));
        list.add(MonsterDef.of("bree", "Bree").level(146).stats(162, 1, 1, 130, 1, 150).speed(5)
                .defenceBonus(20, 20, 20, 20, 20)
                .attack(AttackDef.of("arrows", AttackType.RANGED, 16, 12, 10).particle("minecraft:crit"))
                .drops(guardDrops(God.SARADOMIN)).size(0.9f, 2.0f, 1.2f));
        // General Graardor's sergeants.
        list.add(MonsterDef.of("sergeant_strongstack", "Sergeant Strongstack").level(141).stats(128, 124, 118, 126, 1, 1).speed(5)
                .defenceBonus(5, 5, 5, -5, 5)
                .attack(AttackDef.of("melee", AttackType.CRUSH, 17, 2.5, 10))
                .drops(guardDrops(God.BANDOS)).model("sergeant_strongstack").size(0.6f, 1.4f, 1.3f));
        list.add(MonsterDef.of("sergeant_steelwill", "Sergeant Steelwill").level(142).stats(127, 1, 1, 150, 150, 1).speed(5)
                .defenceBonus(5, 5, 5, 5, 5)
                .attack(AttackDef.of("magic", AttackType.MAGIC, 16, 12, 10).particle("minecraft:witch"))
                .drops(guardDrops(God.BANDOS)).model("sergeant_steelwill").size(0.6f, 1.4f, 1.3f));
        list.add(MonsterDef.of("sergeant_grimspike", "Sergeant Grimspike").level(142).stats(146, 1, 1, 132, 1, 150).speed(5)
                .defenceBonus(5, 5, 5, 5, 5)
                .attack(AttackDef.of("arrows", AttackType.RANGED, 21, 12, 10).particle("minecraft:crit"))
                .drops(guardDrops(God.BANDOS)).model("sergeant_grimspike").size(0.6f, 1.4f, 1.3f));
        // K'ril Tsutsaroth's demons.
        list.add(MonsterDef.of("tstanon_karlak", "Tstanon Karlak").level(145).stats(142, 124, 118, 125, 1, 1).speed(5)
                .defenceBonus(10, 10, 10, 10, 10)
                .attack(AttackDef.of("melee", AttackType.SLASH, 15, 2.5, 10))
                .drops(guardDrops(God.ZAMORAK)).size(0.8f, 2.2f, 1.3f));
        list.add(MonsterDef.of("zakln_gritch", "Zakl'n Gritch").level(142).stats(150, 1, 1, 127, 1, 150).speed(5)
                .defenceBonus(10, 10, 10, 10, 10)
                .attack(AttackDef.of("fire arrows", AttackType.RANGED, 21, 12, 10).particle("minecraft:flame"))
                .drops(guardDrops(God.ZAMORAK)).size(0.8f, 2.2f, 1.3f));
        list.add(MonsterDef.of("balfrug_kreeyath", "Balfrug Kreeyath").level(151).stats(161, 1, 1, 153, 150, 1).speed(5)
                .defenceBonus(10, 10, 10, 10, 10)
                .attack(AttackDef.of("magic", AttackType.MAGIC, 16, 12, 10).particle("minecraft:soul_fire_flame"))
                .drops(guardDrops(God.ZAMORAK)).size(0.8f, 2.2f, 1.3f));
    }

    private static void minions(List<MonsterDef> list) {
        // Nex: blood reavers summoned during Blood siphon.
        list.add(MonsterDef.of("blood_reaver", "Blood reaver").level(122).stats(150, 100, 100, 80, 1, 1).speed(4) // approx.
                .attack(AttackDef.of("melee", AttackType.SLASH, 12, 2.5, 10).special(Special.LIFESTEAL, 25))
                .drops(Drop.always("bones")).size(0.8f, 1.6f, 1.1f));
        // Vet'ion / Calvar'ion hounds.
        list.add(MonsterDef.of("skeleton_hellhound", "Skeleton Hellhound").level(194).stats(80, 200, 160, 100, 1, 1).speed(4)
                .defenceBonus(100, 100, 100, 100, 100)
                .attack(AttackDef.of("bite", AttackType.STAB, 15, 2.5, 10)).size(0.9f, 1.2f, 1.3f));
        list.add(MonsterDef.of("greater_skeleton_hellhound", "Greater Skeleton Hellhound").level(281).stats(100, 230, 200, 130, 1, 1)
                .speed(4).defenceBonus(130, 130, 130, 130, 130)
                .attack(AttackDef.of("bite", AttackType.STAB, 20, 2.5, 10)).size(0.9f, 1.2f, 1.5f));
        // Venenatis' and Spindel's spiderlings.
        // OSRS Wiki: not poisonous; every hit drains 1 Prayer point, damaging or not (stats from MonsterStatsTable).
        list.add(MonsterDef.of("venenatis_spiderling", "Spiderling").level(57).stats(40, 60, 60, 40, 1, 1).speed(4)
                .attack(AttackDef.of("bite", AttackType.STAB, 3, 2.5, 10).special(Special.DRAIN_PRAYER, 1))
                .size(0.8f, 0.6f, 1.0f));
        // Scorpia's family.
        list.add(MonsterDef.of("scorpias_offspring", "Scorpia's offspring").level(15).stats(5, 1, 1, 1, 30, 1).speed(4)
                .attack(AttackDef.of("sting", AttackType.MAGIC, 1, 6, 10).special(Special.POISON, 4))
                .size(0.8f, 0.5f, 0.6f));
        list.add(MonsterDef.of("scorpias_guardian", "Scorpia's guardian").level(70).stats(70, 1, 1, 50, 80, 1).speed(4)
                .attack(AttackDef.of("magic", AttackType.MAGIC, 1, 6, 10)).passive()
                .size(0.8f, 0.6f, 1.0f));
    }

    private record Rev(String id, String name, int level, int hp, int max, int weaponRarity, int skin, float scale) {
    }

    /**
     * Revenants switch between all three styles. The amulet of avarice, revenant weapons and ancient artefacts are
     * rolled on kill by {@link RevenantRewards} (wiki formula: level, skull and Slayer task); the lines here are the
     * ordinary table (ether, coins, bracelet: approx).
     */
    private static void revenants(List<MonsterDef> list) {
        Rev[] revs = {
                new Rev("revenant_imp", "Revenant imp", 7, 10, 3, 6000, 0x60D0B0, 0.6f),
                new Rev("revenant_goblin", "Revenant goblin", 15, 14, 4, 5000, 0x60C0A0, 0.7f),
                new Rev("revenant_pyrefiend", "Revenant pyrefiend", 52, 48, 8, 3200, 0x70D0C0, 0.9f),
                new Rev("revenant_hobgoblin", "Revenant hobgoblin", 60, 72, 10, 2800, 0x60B0A0, 1.0f),
                new Rev("revenant_cyclops", "Revenant cyclops", 82, 110, 13, 2200, 0x70C0B0, 1.3f),
                new Rev("revenant_hellhound", "Revenant hellhound", 90, 80, 14, 2000, 0x50B0A0, 1.2f),
                new Rev("revenant_demon", "Revenant demon", 98, 80, 15, 1800, 0x60A090, 1.3f),
                new Rev("revenant_ork", "Revenant ork", 105, 105, 16, 1600, 0x70B0A0, 1.2f),
                new Rev("revenant_dark_beast", "Revenant dark beast", 120, 140, 18, 1300, 0x408070, 1.3f),
                new Rev("revenant_knight", "Revenant knight", 126, 143, 20, 1200, 0x80D0C0, 1.2f),
                new Rev("revenant_dragon", "Revenant dragon", 135, 155, 22, 1000, 0x50C0B0, 1.6f),
        };
        for (Rev r : revs) {
            int lvl = r.level();
            list.add(MonsterDef.of(r.id(), r.name()).level(lvl).stats(r.hp(), lvl, lvl, lvl, lvl, lvl).speed(4)
                    .defenceBonus(lvl / 2, lvl / 2, lvl / 2, lvl / 2, lvl / 2)
                    .attack(AttackDef.of("melee", AttackType.SLASH, r.max(), 2.5, 10))
                    .attack(AttackDef.of("magic", AttackType.MAGIC, r.max(), 10, 8).particle("minecraft:soul_fire_flame"))
                    .attack(AttackDef.of("ranged", AttackType.RANGED, r.max(), 10, 8).particle("minecraft:soul"))
                    .drops(Drop.of("revenant_ether", 1 + lvl / 20, 3 + lvl / 6, 1),
                            Drop.of("coins", 100 + lvl * 20, 500 + lvl * 80, 2),
                            Drop.of("bracelet_of_ethereum", 1, 1, Math.max(60, 400 - lvl * 2)), // approx
                            Drop.of("dragon_dagger", 1, 1, 400), // approx
                            Drop.of("clue_scroll_elite", 1, 1, Math.max(150, 1000 - lvl * 6)))
                    .model(r.id()).size(0.7f, 1.8f, r.scale()).spawn(Region.WILDERNESS, 2, 1, 2));
        }
    }
}
