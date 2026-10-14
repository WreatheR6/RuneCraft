package com.runecraft.osrs.content.npcs;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Region;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Drop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Attackable townsfolk with OSRS combat stats, drop tables and pickpocketing data, plus the spots in
 * their real towns where they (and the built-in Man, Guard, Al-Kharid warrior and Barbarian) live.
 * Stats follow the OSRS wiki; where a value isn't known exactly the equal attack/strength/defence
 * that reproduces the NPC's combat level is used (marked approx.).
 */
public final class TownMonsters {
    /** One pickpocket reward: {@code min}-{@code max} of {@code item}, rolled 1/{@code weight} (weight 1 = always). */
    public record Loot(String item, int min, int max, int weight) {
        static Loot always(String item, int n) {
            return new Loot(item, n, n, 1);
        }

        static Loot of(String item, int min, int max, int weight) {
            return new Loot(item, min, max, weight);
        }
    }

    /**
     * Pickpocketing a creature. Success uses the OSRS low/high interpolation; {@code always} is given
     * every success and one line of {@code table} (weighted) is added if it's not empty.
     */
    public record Pickpocket(String creature, String name, int level, double xp, int low, int high, int stunDamage,
                             List<Loot> always, List<Loot> table) {
    }

    /** {@code count} of {@code creature} roam within {@code radius} tiles of OSRS tile ({@code x}, {@code y}). */
    public record TownSpawn(String creature, int x, int y, int count, int radius) {
    }

    private static final List<MonsterDef> MONSTERS = new ArrayList<>();
    private static final List<Pickpocket> PICKPOCKETS = new ArrayList<>();
    private static final List<TownSpawn> SPAWNS = new ArrayList<>();

    private TownMonsters() {
    }

    public static List<MonsterDef> monsters() {
        return Collections.unmodifiableList(MONSTERS);
    }

    public static List<Pickpocket> pickpockets() {
        return Collections.unmodifiableList(PICKPOCKETS);
    }

    public static List<TownSpawn> spawns() {
        return Collections.unmodifiableList(SPAWNS);
    }

    public static Pickpocket pickpocket(String creature) {
        for (Pickpocket p : PICKPOCKETS) {
            if (p.creature().equals(creature)) {
                return p;
            }
        }
        return null;
    }

    private static MonsterDef add(MonsterDef m) {
        MONSTERS.add(m);
        return m;
    }

    private static void spawn(String creature, int x, int y, int count, int radius) {
        SPAWNS.add(new TownSpawn(creature, x, y, count, radius));
    }

    private static void snail(String id, String name, int level, int hp, int def, int ranged, int maxHit, int speed, String shell,
                              String meat) {
        add(MonsterDef.of(id, name).level(level).stats(hp, 1, 1, def, 1, ranged).speed(speed).defenceBonus(0, 0, 0, 0, 0)
                .attack(AttackDef.of("acid spit", AttackType.RANGED, maxHit, 6, 10))
                .drops(Drop.always(shell), Drop.always(meat)).model("rockslug").size(0.9f, 0.7f, 0.8f)
                .spawn(Region.MORYTANIA, 1, 1, 3));
    }

    /** The "Man" drop table (shared by Man and Woman). */
    private static Drop[] manDrops() {
        return new Drop[]{Drop.always("bones"), Drop.of("coins", 3, 3, 4), Drop.of("coins", 5, 25, 12),
                Drop.of("bronze_bolts", 2, 12, 20), Drop.of("earth_rune", 4, 4, 45), Drop.of("fire_rune", 7, 7, 45),
                Drop.of("mind_rune", 9, 9, 45), Drop.of("chaos_rune", 2, 2, 64), Drop.of("fishing_bait", 1, 1, 30),
                Drop.of("copper_ore", 1, 1, 40), Drop.of("cabbage", 1, 1, 128), Drop.of("bronze_med_helm", 1, 1, 64),
                Drop.of("iron_dagger", 1, 1, 64), Drop.herbs(32)};
    }

    static {
        // ------------------------------------------------------------- Misthalin / Asgarnia townsfolk
        add(MonsterDef.of("woman", "Woman").level(2).stats(7, 1, 1, 1, 1, 1).speed(4).defenceBonus(-21, -21, -21, -21, -21)
                .attack(AttackDef.melee(1)).drops(manDrops()).passive()
                .skin(0xC8A080, 0xA04A6A, 0x6A3A4A, 0x6A4A2A)
                .spawn(Region.MISTHALIN, 3, 1, 1).spawn(Region.ASGARNIA, 2, 1, 1).spawn(Region.KANDARIN, 2, 1, 1));
        add(MonsterDef.of("farmer", "Farmer").level(7).stats(12, 8, 8, 8, 1, 1).speed(4).defenceBonus(0, 0, 0, 0, 0)
                .attack(AttackDef.melee(1)).passive()
                .drops(Drop.always("bones"), Drop.of("coins", 9, 9, 4), Drop.of("potato_seed", 1, 4, 8), Drop.of("onion_seed", 1, 3, 12),
                        Drop.of("cabbage_seed", 1, 3, 16), Drop.of("raw_bird_meat", 1, 1, 20), Drop.herbs(32), Drop.seeds(40))
                .skin(0xC8A080, 0x6A8A3A, 0x6A4A2A, 0xC0A060)
                .spawn(Region.MISTHALIN, 2, 1, 1).spawn(Region.KANDARIN, 2, 1, 1));
        add(MonsterDef.of("white_knight", "White Knight").level(36).stats(52, 30, 30, 30, 1, 1).speed(4) // approx. stats
                .defenceBonus(40, 45, 40, -10, 40).attack(AttackDef.of("melee", AttackType.SLASH, 5, 2.5, 10)).passive()
                .drops(Drop.always("bones"), Drop.of("coins", 5, 50, 3), Drop.of("iron_bolts", 2, 10, 16),
                        Drop.of("steel_full_helm", 1, 1, 64), Drop.of("steel_longsword", 1, 1, 64), Drop.of("law_rune", 2, 2, 40),
                        Drop.herbs(24))
                .skin(0xC8A080, 0xE0E0E8, 0xC0C0C8, 0xE0E0E8));
        add(MonsterDef.of("monk", "Monk").level(2).stats(15, 1, 1, 1, 1, 1).speed(4).defenceBonus(0, 0, 0, 0, 0)
                .attack(AttackDef.melee(1)).passive().drops(Drop.always("bones"))
                .skin(0xC8A080, 0x6A4A2A, 0x6A4A2A, 0x6A4A2A));
        add(MonsterDef.of("dwarf", "Dwarf").level(7).stats(16, 8, 8, 8, 1, 1).speed(4).defenceBonus(5, 5, 5, 0, 5) // approx. stats
                .attack(AttackDef.melee(2)).passive()
                .drops(Drop.always("bones"), Drop.of("coins", 4, 30, 4), Drop.of("hammer", 1, 1, 16), Drop.of("bronze_pickaxe", 1, 1, 32),
                        Drop.of("copper_ore", 1, 1, 16), Drop.of("tin_ore", 1, 1, 16), Drop.of("iron_ore", 1, 1, 20),
                        Drop.of("coal", 1, 1, 32), Drop.of("bronze_bar", 1, 1, 32), Drop.of("iron_bar", 1, 1, 64))
                .skin(0xC08A68, 0x6A3A1A, 0x4A3A2A, 0xA04020).size(0.6f, 1.4f, 1.0f)
                .spawn(Region.ASGARNIA, 1, 1, 2));
        add(MonsterDef.of("highwayman", "Highwayman").level(5).stats(13, 2, 2, 2, 1, 1).speed(4).defenceBonus(0, 0, 0, 0, 0)
                .attack(AttackDef.of("melee", AttackType.SLASH, 1, 2.5, 10))
                .drops(Drop.always("bones"), Drop.of("coins", 1, 20, 3), Drop.of("bronze_dagger", 1, 1, 30))
                .skin(0xC8A080, 0x2A2A2A, 0x2A2A2A, 0x1A1A1A).spawn(Region.ASGARNIA, 1, 1, 1));
        add(MonsterDef.of("mugger", "Mugger").level(6).stats(8, 5, 5, 5, 1, 1).speed(4).defenceBonus(0, 0, 0, 0, 0)
                .attack(AttackDef.melee(1))
                .drops(Drop.always("bones"), Drop.of("coins", 1, 10, 3), Drop.of("rope", 1, 1, 20), Drop.herbs(40))
                .skin(0xC8A080, 0x4A4A3A, 0x3A3A2A, 0x2A2A2A).spawn(Region.MISTHALIN, 1, 1, 1));

        // The Dark Wizards' Tower's level 11 and level 22-23 dark wizards (the built-in Dark wizard is level 7 and
        // the vanilla witch stands in for level 20). Stats from MonsterStatsTable.
        add(MonsterDef.of("dark_wizard_lvl11", "Dark wizard").level(11).stats(15, 5, 5, 10, 10, 1).speed(4)
                .defenceBonus(0, 0, 0, 5, 0).attack(AttackDef.of("magic", AttackType.MAGIC, 5, 10, 10).particle("minecraft:witch"))
                .drops(Drop.always("bones"), Drop.of("mind_rune", 3, 10, 4), Drop.of("chaos_rune", 1, 5, 8),
                        Drop.of("body_rune", 2, 8, 8), Drop.of("nature_rune", 1, 3, 25), Drop.herbs(40))
                .model("dark_wizard").skin(0xC8A080, 0x2A2A4A, 0x1A1A2A, 0x101018).spawn(Region.ASGARNIA, 2, 1, 2));
        add(MonsterDef.of("dark_wizard_lvl23", "Dark wizard").level(23).stats(25, 20, 10, 20, 25, 1).speed(4)
                .defenceBonus(0, 0, 0, 5, 0).attack(AttackDef.of("magic", AttackType.MAGIC, 5, 10, 10).particle("minecraft:witch"))
                .drops(Drop.always("bones"), Drop.of("chaos_rune", 2, 6, 6), Drop.of("nature_rune", 1, 4, 12),
                        Drop.of("eye_of_newt", 1, 2, 4), Drop.herbs(20))
                .model("dark_wizard").skin(0xC8A080, 0x4A1A1A, 0x2A1010, 0x101018).spawn(Region.ASGARNIA, 2, 1, 2));

        // ------------------------------------------------------------- Ardougne
        add(MonsterDef.of("ardougne_knight", "Knight of Ardougne").level(46).stats(52, 38, 38, 38, 1, 1).speed(4) // approx. att/str/def
                .defenceBonus(45, 50, 45, -10, 45).attack(AttackDef.of("melee", AttackType.SLASH, 5, 2.5, 10)).passive()
                .drops(Drop.always("bones"), Drop.of("coins", 4, 25, 2), Drop.of("coins", 30, 60, 8), Drop.of("bronze_bolts", 2, 12, 12),
                        Drop.of("earth_rune", 4, 9, 32), Drop.of("chaos_rune", 2, 2, 48), Drop.of("iron_ore", 1, 1, 64), Drop.herbs(20))
                .skin(0xC8A080, 0xA0A0A8, 0x6A6A70, 0x9A9AA0));
        add(MonsterDef.of("paladin", "Paladin").level(62).stats(57, 54, 54, 54, 1, 1).speed(4) // approx. att/str/def
                .defenceBonus(60, 65, 60, -10, 60).attack(AttackDef.of("melee", AttackType.SLASH, 6, 2.5, 10)).passive()
                .drops(Drop.always("bones"), Drop.of("coins", 20, 80, 2), Drop.of("chaos_rune", 2, 10, 8), Drop.of("iron_ore", 1, 1, 32),
                        Drop.of("steel_full_helm", 1, 1, 64), Drop.of("law_rune", 2, 4, 32), Drop.herbs(16))
                .skin(0xC8A080, 0xE0E0E8, 0xC0C0C8, 0xE0C040));
        add(MonsterDef.of("hero", "Hero").level(69).stats(82, 54, 54, 54, 1, 1).speed(4) // approx. att/str/def
                .defenceBonus(30, 30, 30, 0, 30).attack(AttackDef.of("melee", AttackType.SLASH, 6, 2.5, 10)).passive()
                .drops(Drop.always("bones"), Drop.of("coins", 50, 300, 2), Drop.of("death_rune", 2, 2, 16),
                        Drop.of("blood_rune", 1, 1, 24), Drop.of("fire_orb", 1, 1, 40), Drop.of("diamond", 1, 1, 64),
                        Drop.of("gold_ore", 1, 1, 32), Drop.of("jug_of_wine", 1, 1, 24), Drop.herbs(16), Drop.gems(64))
                .skin(0xC8A080, 0x7A2A2A, 0x3A3A3A, 0xE0C040));

        // ------------------------------------------------------------- Gnomes
        add(MonsterDef.of("gnome", "Gnome").level(1).stats(3, 1, 1, 1, 1, 1).speed(4).defenceBonus(0, 0, 0, 0, 0) // approx.
                .attack(AttackDef.melee(1)).passive()
                .drops(Drop.always("bones"), Drop.of("coins", 1, 30, 4), Drop.of("earth_rune", 1, 4, 16), Drop.of("fire_orb", 1, 1, 128))
                .skin(0xC09070, 0x3A8A3A, 0x6A4A2A, 0x9A2A2A).size(0.5f, 1.1f, 0.8f));
        add(MonsterDef.of("gnome_guard", "Gnome guard").level(23).stats(31, 17, 17, 17, 1, 1).speed(4) // approx. att/str/def
                .defenceBonus(10, 10, 10, 0, 10).attack(AttackDef.of("melee", AttackType.STAB, 3, 2.5, 10)).passive()
                .drops(Drop.always("bones"), Drop.of("coins", 5, 40, 3), Drop.of("iron_arrow", 2, 8, 12), Drop.herbs(30))
                .skin(0xC09070, 0x8A8A9A, 0x6A4A2A, 0x6A6A7A).size(0.5f, 1.1f, 0.85f));

        // ------------------------------------------------------------- Prifddinas
        // The elven city guards (wiki "Guard (Prifddinas)": level 108, not aggressive, respawn 30 ticks, task: elves).
        // Their 128-slot table is rolled as independent 1/x chances; crystal teleport seeds and teleport crystals
        // aren't in RuneCraft.
        add(MonsterDef.of("prifddinas_guard", "Guard").level(108).stats(105, 95, 95, 80, 1, 1).speed(4)
                .defenceBonus(50, 70, 70, 60, 50).attack(AttackDef.of("melee", AttackType.STAB, 10, 2.5, 10)).passive()
                .drops(Drop.always("bones"), Drop.of("green_dhide_chaps", 1, 1, 43), Drop.of("green_dhide_body", 1, 1, 32),
                        Drop.of("mithril_spear", 1, 1, 64), Drop.of("mithril_kiteshield", 1, 1, 128),
                        Drop.of("adamant_full_helm", 1, 1, 128), Drop.of("rune_dagger", 1, 1, 128), Drop.of("water_rune", 70, 70, 16),
                        Drop.of("nature_rune", 12, 12, 26), Drop.of("law_rune", 2, 2, 43), Drop.of("fire_rune", 37, 37, 64),
                        Drop.of("coins", 20, 20, 26), Drop.of("coins", 44, 44, 4), Drop.of("coins", 180, 180, 13),
                        Drop.of("bass", 1, 1, 43), Drop.of("shark", 1, 1, 43), Drop.of("adamantite_ore", 1, 1, 64),
                        Drop.of("crystal_shard", 3, 5, 24), Drop.of("ensouled_elf_head", 1, 1, 50), Drop.of("clue_scroll_hard", 1, 1, 128))
                .slayer(1, "elves").skin(0xE8D8C0, 0xB8D8E8, 0x6A8AA0, 0xE0F0FF).size(0.6f, 2.0f, 1.0f));

        // ------------------------------------------------------------- Desert, Karamja
        add(MonsterDef.of("desert_bandit", "Bandit").level(57).stats(64, 45, 45, 45, 1, 1).speed(4) // approx. stats
                .defenceBonus(15, 15, 15, 0, 15).attack(AttackDef.of("melee", AttackType.SLASH, 6, 2.5, 10))
                .drops(Drop.always("bones"), Drop.of("coins", 10, 100, 2), Drop.of("antipoison", 1, 1, 24), Drop.of("lockpick", 1, 1, 32),
                        Drop.of("iron_scimitar", 1, 1, 48), Drop.herbs(20))
                .skin(0xA07850, 0xE0D8C0, 0x8A6A4A, 0x3A2A1A));
        add(MonsterDef.of("pirate", "Pirate").level(23).stats(20, 18, 18, 18, 1, 1).speed(4) // approx. stats
                .defenceBonus(5, 5, 5, 0, 5).attack(AttackDef.of("melee", AttackType.SLASH, 3, 2.5, 10))
                .drops(Drop.always("bones"), Drop.of("coins", 4, 40, 3), Drop.of("bronze_scimitar", 1, 1, 24), Drop.of("iron_scimitar", 1, 1, 48),
                        Drop.of("karamjan_rum", 1, 1, 64), Drop.of("chaos_rune", 2, 2, 40), Drop.herbs(40))
                .skin(0xC8A080, 0xA02A2A, 0x3A3A4A, 0x1A1A1A).spawn(Region.KARAMJA, 1, 1, 2));

        // ------------------------------------------------------------- TzHaar
        add(MonsterDef.of("tzhaar_hur_citizen", "TzHaar-Hur").level(74).stats(80, 60, 60, 60, 1, 1).speed(4) // approx. att/str/def
                .defenceBonus(20, 20, 20, 0, 20).attack(AttackDef.of("melee", AttackType.CRUSH, 9, 2.5, 10)).passive()
                .drops(Drop.of("tokkul", 3, 14, 1), Drop.of("uncut_sapphire", 1, 1, 24), Drop.of("uncut_emerald", 1, 1, 48),
                        Drop.of("uncut_ruby", 1, 1, 96), Drop.of("uncut_diamond", 1, 1, 192))
                .skin(0x8A3A1A, 0x5A2A1A, 0x4A2A1A, 0xE06010));
        add(MonsterDef.of("tzhaar_ket", "TzHaar-Ket").level(149).stats(140, 127, 127, 127, 1, 1).speed(4) // approx. att/str/def
                .defenceBonus(40, 40, 40, 0, 40).attack(AttackDef.of("melee", AttackType.CRUSH, 16, 2.5, 10)).passive()
                .drops(Drop.of("tokkul", 5, 40, 1), Drop.of("uncut_ruby", 1, 1, 48), Drop.of("uncut_diamond", 1, 1, 96),
                        Drop.of("toktz_ket_xil", 1, 1, 512), Drop.of("tzhaar_ket_em", 1, 1, 512), Drop.of("obsidian_helmet", 1, 1, 1024))
                .skin(0x8A3A1A, 0x3A1A0A, 0x3A1A0A, 0xE06010).size(0.7f, 2.2f, 1.1f));
        add(MonsterDef.of("tzhaar_xil", "TzHaar-Xil").level(133).stats(120, 70, 70, 120, 1, 140).speed(4) // approx. stats
                .defenceBonus(30, 30, 30, 0, 30).attack(AttackDef.of("ranged", AttackType.RANGED, 14, 8, 10)).passive()
                .drops(Drop.of("tokkul", 5, 40, 1), Drop.of("uncut_emerald", 1, 1, 48), Drop.of("uncut_diamond", 1, 1, 96),
                        Drop.of("toktz_xil_ak", 1, 1, 512), Drop.of("toktz_xil_ek", 1, 1, 512))
                .skin(0x8A3A1A, 0x4A2A1A, 0x4A2A1A, 0xE0A010));
        add(MonsterDef.of("tzhaar_mej", "TzHaar-Mej").level(103).stats(75, 40, 40, 114, 114, 1).speed(4) // approx. stats
                .defenceBonus(20, 20, 20, 40, 20).attack(AttackDef.of("magic", AttackType.MAGIC, 13, 8, 10)).passive()
                .drops(Drop.of("tokkul", 5, 40, 1), Drop.of("uncut_sapphire", 1, 1, 24), Drop.of("uncut_ruby", 1, 1, 96),
                        Drop.of("toktz_mej_tal", 1, 1, 512), Drop.of("obsidian_cape", 1, 1, 1024))
                .skin(0x8A3A1A, 0x2A1A3A, 0x2A1A3A, 0xA040E0));

        // ------------------------------------------------------------- wild creatures on the towns' doorsteps
        // (their spawn points are in world.SpawnTable: rock crabs on the Rellekka and Waterbirth shores, imps
        // around Lumbridge, Draynor, Falador and Varrock)
        add(MonsterDef.of("rock_crab", "Rock crab").level(13).stats(50, 1, 1, 1, 1, 1).speed(4).defenceBonus(0, 0, 0, 0, 0)
                .attack(AttackDef.of("pinch", AttackType.CRUSH, 1, 2.5, 10))
                .drops(Drop.of("seaweed", 1, 1, 16), Drop.of("fishing_bait", 1, 5, 16), Drop.of("oyster", 1, 1, 32),
                        Drop.of("copper_ore", 1, 1, 40), Drop.of("tin_ore", 1, 1, 40), Drop.of("iron_ore", 1, 1, 40),
                        Drop.of("coal", 1, 1, 64), Drop.of("casket", 1, 1, 128), Drop.of("clue_scroll_easy", 1, 1, 128)) // approx. table
                .skin(0x7A7468, 0x8A5A3A, 0x5E5A50, 0x6A7A3A).size(1.0f, 0.8f, 1.0f).moveSpeed(0.2)
                .spawn(Region.FREMENNIK_PROVINCE, 3, 2, 4));
        add(MonsterDef.of("imp", "Imp").level(2).stats(8, 4, 3, 2, 1, 1).speed(4).defenceBonus(0, 0, 0, 0, 0) // approx. att/str/def
                .attack(AttackDef.melee(1)).passive()
                .drops(Drop.of("imp_champion_scroll", 1, 1, 5000), Drop.always("fiendish_ashes"), Drop.of("black_bead", 1, 1, 12), Drop.of("red_bead", 1, 1, 12), Drop.of("white_bead", 1, 1, 12),
                        Drop.of("yellow_bead", 1, 1, 12), Drop.of("burnt_bread", 1, 1, 16), Drop.of("burnt_meat", 1, 1, 16),
                        Drop.of("cabbage", 1, 1, 20), Drop.of("bread_dough", 1, 1, 24), Drop.of("pot_of_flour", 1, 1, 24),
                        Drop.of("ball_of_wool", 1, 1, 24), Drop.of("jug", 1, 1, 32), Drop.of("empty_cup", 1, 1, 32),
                        Drop.of("tinderbox", 1, 1, 40), Drop.of("hammer", 1, 1, 40), Drop.of("needle", 1, 1, 48),
                        Drop.of("mind_talisman", 1, 1, 64), Drop.of("clue_scroll_beginner", 1, 1, 128))
                .skin(0xB02A20, 0xB02A20, 0x6A1410, 0x2A1A14).size(0.5f, 1.2f, 1.0f).moveSpeed(0.32)
                .spawn(Region.MISTHALIN, 3, 1, 2).spawn(Region.ASGARNIA, 2, 1, 2));

        // ------------------------------------------------------------- pickpocketing (OSRS levels, XP and loot)
        PICKPOCKETS.add(new Pickpocket("woman", "Woman", 1, 8.0, 180, 240, 1, List.of(Loot.always("coins", 3)), List.of()));
        PICKPOCKETS.add(new Pickpocket("farmer", "Farmer", 10, 14.5, 150, 240, 1, List.of(Loot.always("coins", 9)),
                List.of(Loot.of("potato_seed", 1, 1, 1), Loot.of("coins", 1, 1, 9))));
        PICKPOCKETS.add(new Pickpocket("desert_bandit", "Bandit", 53, 79.5, 50, 240, 3, List.of(Loot.always("coins", 30)),
                List.of(Loot.of("antipoison", 1, 1, 1), Loot.of("lockpick", 1, 1, 1), Loot.of("coins", 1, 1, 8))));
        PICKPOCKETS.add(new Pickpocket("ardougne_knight", "Knight of Ardougne", 55, 84.3, 50, 240, 3,
                List.of(Loot.always("coins", 50)), List.of()));
        PICKPOCKETS.add(new Pickpocket("paladin", "Paladin", 70, 151.75, 50, 150, 3, List.of(Loot.always("coins", 80)),
                List.of(Loot.of("chaos_rune", 2, 2, 1), Loot.of("coins", 1, 1, 7))));
        PICKPOCKETS.add(new Pickpocket("gnome", "Gnome", 75, 198.5, 8, 120, 1, List.of(),
                List.of(Loot.of("coins", 300, 300, 40), Loot.of("earth_rune", 1, 1, 15), Loot.of("gold_ore", 1, 1, 10),
                        Loot.of("fire_orb", 1, 1, 5), Loot.of("uncut_sapphire", 1, 1, 10), Loot.of("mithril_ore", 1, 1, 10))));
        PICKPOCKETS.add(new Pickpocket("hero", "Hero", 80, 273.3, 6, 100, 4, List.of(),
                List.of(Loot.of("coins", 200, 300, 70), Loot.of("death_rune", 2, 2, 8), Loot.of("blood_rune", 1, 1, 6),
                        Loot.of("fire_orb", 1, 1, 4), Loot.of("diamond", 1, 1, 2), Loot.of("gold_ore", 1, 1, 4),
                        Loot.of("jug_of_wine", 1, 1, 6))));
        // Cave goblins of Dorgesh-Kaan (the dungeons module's creature): 36 Thieving, 40 XP; coins loose (no pouch) with
        // iron ore, tinderboxes and swamp tar (their goblin food and lanterns are not in the game; weights approx.).
        PICKPOCKETS.add(new Pickpocket("cave_goblin", "Cave goblin", 36, 40.0, 100, 240, 1, List.of(),
                List.of(Loot.of("coins", 11, 48, 20), Loot.of("iron_ore", 1, 1, 4), Loot.of("tinderbox", 1, 1, 3),
                        Loot.of("swamp_tar", 1, 1, 3))));
        PICKPOCKETS.add(new Pickpocket("tzhaar_hur_citizen", "TzHaar-Hur", 90, 103.4, 40, 200, 4, List.of(),
                List.of(Loot.of("tokkul", 3, 14, 70), Loot.of("uncut_sapphire", 1, 1, 6), Loot.of("uncut_emerald", 1, 1, 4),
                        Loot.of("uncut_ruby", 1, 1, 2), Loot.of("uncut_diamond", 1, 1, 1))));

        // ------------------------------------------------------------- wildlife the diaries ask for
        // Stats from the OSRS Wiki DPS calculator (weirdgloop/osrs-dps-calc monsters.json), drops from osrsreboxed-db,
        // homes from the cache spawn list (mejrs/data_osrs). None of them is aggressive except the snails.
        add(MonsterDef.of("vulture", "Vulture").level(31).stats(10, 40, 40, 10, 1, 1).speed(4).defenceBonus(0, 0, 0, 0, 0)
                .attack(AttackDef.of("peck", AttackType.CRUSH, 5, 2.5, 10)).passive()
                .drops(Drop.always("bones")).size(1.4f, 1.2f, 1.0f)
                .spawn(Region.KHARIDIAN_DESERT, 1, 1, 3));
        add(MonsterDef.of("duck", "Duck").level(1).stats(3, 1, 1, 1, 1, 1).speed(4).defenceBonus(-42, -42, -42, -42, -42)
                .attackBonus(-47, 0, 0).attack(AttackDef.of("peck", AttackType.STAB, 0, 2.0, 10)).passive()
                .drops(Drop.always("bones")).size(0.6f, 0.6f, 1.0f)
                .spawn(Region.ASGARNIA, 1, 1, 2).spawn(Region.MISTHALIN, 1, 1, 2));
        add(MonsterDef.of("terrorbird", "Terrorbird").level(28).stats(34, 23, 23, 19, 1, 1).speed(4).defenceBonus(0, 0, 0, 0, 0)
                .attack(AttackDef.of("peck", AttackType.STAB, 3, 2.5, 10)).passive()
                .drops(Drop.always("bones")).size(1.2f, 2.2f, 1.0f)
                .spawn(Region.KANDARIN, 1, 1, 3));
        // Mort Myre and Haunted Woods snails (round and pointed shells): they spit acid at range and always drop their
        // shell and their corpse.
        snail("myre_blamish_snail", "Myre Blamish Snail", 9, 8, 22, 5, 2, 4, "blamish_myre_shell_round", "thin_snail");
        snail("myre_blamish_snail_pointed", "Myre Blamish Snail", 10, 13, 12, 8, 2, 4, "blamish_myre_shell_pointed", "thin_snail");
        snail("ochre_blamish_snail", "Ochre Blamish Snail", 10, 10, 18, 7, 2, 6, "blamish_ochre_shell_round", "thin_snail");
        snail("ochre_blamish_snail_pointed", "Ochre Blamish Snail", 15, 20, 25, 8, 2, 6, "blamish_ochre_shell_pointed", "thin_snail");
        snail("blood_blamish_snail", "Blood Blamish Snail", 20, 13, 45, 12, 2, 6, "blamish_red_shell_round", "fat_snail");
        snail("blood_blamish_snail_pointed", "Blood Blamish Snail", 20, 10, 30, 21, 3, 6, "blamish_red_shell_pointed", "fat_snail");
        snail("bruise_blamish_snail", "Bruise Blamish Snail", 20, 12, 40, 15, 2, 6, "blamish_blue_shell_round", "fat_snail");
        snail("bruise_blamish_snail_pointed", "Bruise Blamish Snail", 20, 15, 27, 20, 3, 6, "blamish_blue_shell_pointed", "fat_snail");
        snail("bark_blamish_snail", "Bark Blamish Snail", 15, 22, 20, 10, 2, 6, "blamish_bark_shell", "lean_snail");

        // ------------------------------------------------------------- where they live (OSRS tiles)
        // Built-in creatures: man, guard, al_kharid_warrior, barbarian, dark_wizard.
        spawn("man", 3222, 3218, 2, 8);         // Lumbridge
        spawn("woman", 3229, 3208, 2, 8);       // south of the castle (cache: 3211-3244, 3203-3213)
        spawn("man", 3230, 3400, 3, 10);        // Varrock, south of the square (cache: 3194-3238, 3395-3406)
        spawn("woman", 3218, 3396, 1, 6);
        spawn("guard", 3212, 3462, 3, 8);       // Varrock Palace
        spawn("guard", 3176, 3428, 2, 6);       // Varrock west gate
        spawn("guard", 3270, 3428, 2, 6);       // Varrock east gate
        spawn("mugger", 3249, 3391, 1, 6);
        spawn("man", 3093, 3500, 2, 8);         // Edgeville
        spawn("monk", 3052, 3490, 3, 6);        // Edgeville Monastery
        spawn("man", 3093, 3250, 2, 8);         // Draynor Village
        spawn("woman", 3086, 3256, 1, 6);
        spawn("farmer", 3189, 3277, 2, 8);      // Fred's farm, Lumbridge
        spawn("farmer", 3175, 3330, 2, 12);     // the farms north of Lumbridge (cache: 3162-3192, 3324-3338)
        spawn("dark_wizard", 3227, 3369, 4, 6); // Stone circle south of Varrock
        spawn("barbarian", 3082, 3420, 5, 8);   // Barbarian Village
        spawn("al_kharid_warrior", 3293, 3172, 5, 6); // Al Kharid palace
        spawn("man", 3293, 3185, 2, 8);         // Al Kharid
        spawn("guard", 2965, 3392, 3, 10);
        spawn("white_knight", 2965, 3345, 4, 8); // White Knights' Castle
        spawn("dwarf", 3018, 3450, 3, 8);       // Ice Mountain / Dwarven Mine entrance
        spawn("highwayman", 2995, 3290, 2, 8);  // south of Falador
        spawn("man", 2618, 3294, 1, 4);         // East Ardougne (the cache has one man and one woman)
        spawn("woman", 2612, 3316, 1, 4);
        spawn("guard", 2650, 3305, 2, 10);
        spawn("ardougne_knight", 2662, 3302, 3, 10);
        spawn("paladin", 2588, 3297, 3, 6);     // Ardougne Castle
        spawn("hero", 2648, 3302, 3, 16);       // East Ardougne market (cache: 2630-2667, 3288-3316)
        spawn("farmer", 2633, 3364, 2, 8);      // Ardougne farms
        spawn("gnome", 2461, 3444, 4, 10);      // Tree Gnome Stronghold
        spawn("gnome", 2465, 3495, 3, 8);
        spawn("gnome_guard", 2461, 3385, 2, 6); // Stronghold gate
        spawn("desert_bandit", 3171, 2976, 5, 8); // Bandit Camp
        spawn("pirate", 2790, 3175, 3, 8);      // Brimhaven
        // Wildlife at their cache spawn clusters.
        spawn("vulture", 3335, 2863, 3, 4);     // east of Pollnivneach's southern road (cache: 3333-3338, 2860-2864)
        spawn("vulture", 3298, 2864, 3, 4);
        spawn("vulture", 3217, 2842, 3, 4);
        spawn("duck", 2980, 3357, 4, 3);        // Falador Park ponds (cache: 2977-2992, 3356-3384)
        spawn("duck", 2991, 3382, 4, 3);
        spawn("duck", 3168, 3351, 8, 10);       // the Champions' Guild pond (cache: 3152-3177, 3345-3357)
        spawn("duck", 3240, 3236, 6, 8);        // the River Lum by Lumbridge Castle
        spawn("terrorbird", 2378, 3434, 9, 6);  // the Gnome Stronghold's terrorbird enclosure
        spawn("terrorbird", 2501, 3140, 2, 3);  // Tree Gnome Village
        spawn("myre_blamish_snail", 3436, 3294, 8, 12);         // Mort Myre Swamp (cache: 3421-3448, 3284-3308)
        spawn("myre_blamish_snail_pointed", 3436, 3294, 7, 12);
        spawn("myre_blamish_snail", 3418, 3368, 5, 10);
        spawn("myre_blamish_snail_pointed", 3418, 3368, 4, 10);
        spawn("myre_blamish_snail", 3466, 3422, 4, 10);
        spawn("myre_blamish_snail_pointed", 3466, 3422, 4, 10);
        spawn("myre_blamish_snail", 3502, 3389, 8, 12);
        spawn("ochre_blamish_snail", 3466, 3409, 3, 8);
        spawn("ochre_blamish_snail_pointed", 3466, 3409, 2, 8);
        spawn("ochre_blamish_snail", 3421, 3380, 2, 10);
        spawn("ochre_blamish_snail_pointed", 3472, 3356, 2, 8);
        spawn("blood_blamish_snail", 3625, 3429, 4, 10);        // south of Port Phasmatys
        spawn("blood_blamish_snail_pointed", 3636, 3446, 2, 4);
        spawn("blood_blamish_snail_pointed", 3528, 3435, 2, 8); // the Haunted Woods
        spawn("bruise_blamish_snail", 3632, 3436, 2, 6);
        spawn("bruise_blamish_snail_pointed", 3626, 3454, 2, 6);
        spawn("bruise_blamish_snail", 3486, 3440, 1, 3);
        spawn("bruise_blamish_snail_pointed", 3486, 3440, 1, 3);
        spawn("bark_blamish_snail", 3502, 3426, 4, 8);
        spawn("bark_blamish_snail", 3433, 3311, 3, 5);
        // Prifddinas guards at their cache tiles (plane 0); the south gate's pair stands in this city's south gate,
        // 14 tiles north of the real one (approx).
        spawn("prifddinas_guard", 3264, 6040, 2, 2);
        spawn("prifddinas_guard", 3264, 6066, 1, 3);
        spawn("prifddinas_guard", 3318, 6080, 2, 3);
        spawn("prifddinas_guard", 3210, 6080, 2, 3);
        spawn("prifddinas_guard", 3248, 6082, 1, 3);
        spawn("prifddinas_guard", 3275, 6094, 5, 8);
        spawn("prifddinas_guard", 3264, 6133, 2, 3);
    }
}
