package com.runecraft.osrs.data;

import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.CombatProfile;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Reference OSRS combat stats for module monsters and bosses, applied by
 * {@link com.runecraft.osrs.content.ContentRegistry} after every module has added its creatures (the same
 * idea as {@link EquipmentBonuses} for items). Rows come from the OSRS Wiki's DPS calculator data
 * (weirdgloop/osrs-dps-calc {@code monsters.json}): combat level, hitpoints, the five combat skills, attack
 * speed, defence bonuses (ranged = the "standard" ranged defence), attack bonuses and monster attributes
 * (undead, demon, dragon, kalphite, vampyre, ...). Max hits are corrected only where the wiki value
 * clearly belongs to one attack; boss specials and scripted mechanics keep the module's numbers.
 *
 * <p>Raid creatures are not listed: their base stats live in the raids module and are scaled by the
 * raid code. Creatures with no wiki counterpart (minigame objects, invented quest bosses) keep the
 * module's values. Fix a wrong stat here rather than in the module file.
 */
public final class MonsterStatsTable {
    /** One creature's reference stat line. */
    public record Row(String id, int combatLevel, int hitpoints, int attack, int strength, int defence, int magic,
                      int ranged, int attackSpeed, int stabDefence, int slashDefence, int crushDefence,
                      int magicDefence, int rangedDefence, int meleeAttackBonus, int magicAttackBonus,
                      int rangedAttackBonus, Set<String> attributes) {
    }

    private static final Map<String, Row> ROWS = new LinkedHashMap<>();
    private static final Map<String, Map<String, Integer>> MAX_HITS = new HashMap<>();

    static {
        rows0();
        rows1();
        rows2();
        rows3();
        rows4();
        rows5();
        rows6();
    }

    private MonsterStatsTable() {
    }

    /** The reference row for a creature id, or null when the creature has none. */
    public static Row row(String id) {
        return ROWS.get(id);
    }

    /** Every reference row, keyed by creature id. */
    public static Map<String, Row> rows() {
        return Collections.unmodifiableMap(ROWS);
    }

    /** Corrected max hits for a creature's attacks (attack name to max hit); empty when none. */
    public static Map<String, Integer> maxHits(String id) {
        return Collections.unmodifiableMap(MAX_HITS.getOrDefault(id, Map.of()));
    }

    /** Overwrites a creature's combat stats with its reference row; returns false when it has none. */
    public static boolean apply(CombatProfile<?> p) {
        Row r = ROWS.get(p.id);
        if (r == null) {
            return false;
        }
        p.level(r.combatLevel())
                .stats(r.hitpoints(), r.attack(), r.strength(), r.defence(), r.magic(), r.ranged())
                .speed(r.attackSpeed())
                .defenceBonus(r.stabDefence(), r.slashDefence(), r.crushDefence(), r.magicDefence(), r.rangedDefence())
                .attackBonus(r.meleeAttackBonus(), r.magicAttackBonus(), r.rangedAttackBonus());
        p.attributes.clear();
        p.attributes.addAll(r.attributes());
        p.referenceStats = true;
        Map<String, Integer> max = MAX_HITS.get(p.id);
        if (max != null) {
            for (int i = 0; i < p.attacks.size(); i++) {
                AttackDef a = p.attacks.get(i);
                Integer m = max.get(a.name());
                if (m != null) {
                    p.attacks.set(i, new AttackDef(a.name(), a.style(), m, a.range(), a.weight(), a.special(), a.param(),
                            a.telegraphTicks(), a.particle(), a.message()));
                }
            }
        }
        return true;
    }

    private static void row(String id, int level, int hp, int att, int str, int def, int mage, int range, int speed,
                            int dStab, int dSlash, int dCrush, int dMagic, int dRanged, int aMelee, int aMagic,
                            int aRanged, String... attributes) {
        if (ROWS.put(id, new Row(id, level, hp, att, str, def, mage, range, speed, dStab, dSlash, dCrush, dMagic,
                dRanged, aMelee, aMagic, aRanged, Set.of(attributes))) != null) {
            throw new IllegalStateException("Duplicate monster stats row " + id);
        }
    }

    private static void maxHit(String id, String attack, int max) {
        MAX_HITS.computeIfAbsent(id, k -> new HashMap<>()).put(attack, max);
    }

    private static void rows0() {
        // Baby impling (Overworld)
        row("baby_impling", 0, 10, 1, 1, 1, 1, 1, 4, -10, -10, -10, 10, -10, -10, 0, 0);
        // Young impling
        row("young_impling", 0, 10, 1, 1, 1, 1, 1, 4, -10, -10, -10, 10, -10, -10, 0, 0);
        // Gourmet impling
        row("gourmet_impling", 0, 10, 1, 1, 1, 1, 1, 4, -10, -10, -10, 10, -10, -10, 0, 0);
        // Earth impling
        row("earth_impling", 0, 10, 1, 1, 1, 1, 1, 4, -10, -10, -10, 10, -10, -10, 0, 0);
        // Essence impling
        row("essence_impling", 0, 10, 1, 1, 1, 1, 1, 4, -10, -10, -10, 10, -10, -10, 0, 0);
        // Eclectic impling
        row("eclectic_impling", 0, 10, 1, 1, 1, 1, 1, 4, -10, -10, -10, 10, -10, -10, 0, 0);
        // Nature impling
        row("nature_impling", 0, 10, 1, 1, 1, 1, 1, 4, -10, -10, -10, 20, -10, -10, 0, 0);
        // Magpie impling
        row("magpie_impling", 0, 10, 1, 1, 1, 1, 1, 4, -10, -10, -10, 30, -10, -10, 0, 0);
        // Ninja impling
        row("ninja_impling", 0, 10, 1, 1, 1, 1, 1, 4, -10, -10, -10, 40, -10, -10, 0, 0);
        // Dragon impling
        row("dragon_impling", 0, 10, 1, 1, 1, 1, 1, 4, -10, -10, -10, 50, -10, -10, 0, 0);
        // Lucky impling (Overworld)
        row("lucky_impling", 0, 10, 1, 1, 1, 1, 1, 4, -10, -10, -10, 50, -10, -10, 0, 0);
        // Giant bat (Normal)
        row("giant_bat", 27, 32, 22, 22, 22, 1, 1, 4, 10, 10, 12, 10, 8, 0, 0, 0);
        maxHit("giant_bat", "melee", 3); // was 4
        // Grizzly bear (Level 21)
        row("grizzly_bear", 21, 27, 17, 18, 15, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Scorpion (Level 14)
        row("scorpion", 14, 17, 11, 12, 11, 1, 1, 4, 5, 15, 15, 0, 5, 0, 0, 0);
        maxHit("scorpion", "melee", 2); // was 3
        // King Scorpion
        row("king_scorpion", 32, 30, 30, 29, 23, 1, 1, 4, 5, 15, 15, 0, 5, 0, 0, 0);
        // Minotaur (Level 27)
        row("minotaur", 27, 22, 23, 25, 25, 1, 1, 4, -10, -10, -10, -21, -10, 0, 0, 0);
        maxHit("minotaur", "melee", 3); // was 4
        // Ghost (Level 19)
        row("ghost", 19, 25, 13, 13, 18, 1, 1, 4, 5, 5, 5, -5, 5, 0, 0, 0, "spectral", "undead");
        // Icefiend (Level 18)
        // Icefiend (Level 13): the Ice Mountain icefiends, the only ones placed (level 18 lives in the God Wars Dungeon)
        row("icefiend", 13, 15, 10, 10, 12, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "demon");
        // Hobgoblin (Hobgoblin)
        row("hobgoblin", 28, 29, 22, 24, 24, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("hobgoblin", "melee", 3); // was 5
        // Monkey (monster) (Common)
        row("monkey", 3, 6, 2, 3, 2, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Earth warrior
        row("earth_warrior", 51, 54, 42, 42, 42, 1, 1, 4, 30, 40, 20, 10, 30, 0, 0, 0);
        maxHit("earth_warrior", "melee", 5); // was 6
        // Ice warrior (Normal)
        row("ice_warrior", 57, 59, 47, 47, 47, 1, 1, 4, 30, 40, 20, 10, 30, 0, 0, 0);
        maxHit("ice_warrior", "melee", 6); // was 7
        // Crawling Hand (Level 8 (1))
        row("crawling_hand", 8, 16, 8, 4, 4, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "undead");
        maxHit("crawling_hand", "melee", 1); // was 2
        // Cave bug (Level 6)
        row("cave_bug", 6, 5, 6, 5, 6, 1, 1, 4, 10, 10, 5, 5, 10, 0, 0, 0);
        // Cave crawler (Icy (1))
        row("cave_crawler", 23, 22, 22, 18, 18, 1, 1, 4, 10, 10, 5, 5, 10, 0, 0, 0);
        // Banshee
        row("banshee", 23, 22, 22, 15, 22, 1, 1, 4, 5, 5, 5, 0, 5, 0, 0, 0, "spectral", "undead");
        // Cave slime
        row("cave_slime", 23, 25, 13, 13, 35, 13, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("cave_slime", "melee", 2); // was 4
        // Rockslug (Adult)
        row("rockslug", 29, 27, 22, 27, 27, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Lizard
        row("lizard", 42, 40, 38, 36, 35, 1, 1, 4, 5, 15, 15, 0, 5, 0, 0, 0);
        // Desert Lizard (Green)
        row("desert_lizard", 24, 25, 20, 22, 20, 1, 1, 4, 5, 15, 15, 0, 5, 0, 0, 0);
        maxHit("desert_lizard", "melee", 3); // was 4
        // Cockatrice
        row("cockatrice", 37, 37, 22, 37, 37, 1, 1, 4, 10, 10, 0, 10, 0, 0, 0, 0);
        maxHit("cockatrice", "melee", 5); // was 3
        // Pyrefiend (Level 43)
        row("pyrefiend", 43, 45, 52, 30, 22, 1, 1, 4, 10, 10, 10, 0, 10, 0, 0, 0, "demon", "fiery");
        // Mogre
        row("mogre", 60, 48, 58, 55, 48, 1, 1, 6, 0, 0, 0, 0, 0, 22, 0, 0);
        // Harpie Bug Swarm
        row("harpie_bug_swarm", 46, 25, 54, 46, 32, 1, 1, 4, 10, 5, 10, 5, 10, 0, 0, 0);
        // Wall beast (Beast)
        row("wall_beast", 49, 105, 30, 30, 16, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("wall_beast", "melee", 4); // was 6
        // Killerwatt (Attacking)
        row("killerwatt", 55, 51, 50, 50, 40, 67, 67, 2, 10, 10, 10, 20, -10, 30, 0, 0);
        // Molanisk
        row("molanisk", 51, 52, 40, 40, 50, 1, 1, 4, 45, 45, 35, 30, 55, 0, 0, 0);
        // Basilisk
        row("basilisk", 61, 75, 30, 45, 75, 1, 1, 4, 20, 20, 0, 20, 0, 0, 0, 0);
        maxHit("basilisk", "melee", 5); // was 8
        // Sea Snake Young
        row("sea_snake_young", 90, 85, 90, 70, 70, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("sea_snake_young", "melee", 8); // was 7
        // Terror dog (Level 100)
        row("terror_dog", 100, 82, 100, 90, 74, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("terror_dog", "melee", 11); // was 10
        // Fever spider
        row("fever_spider", 49, 40, 60, 30, 40, 1, 1, 4, 20, 15, 10, 15, 15, 0, 0, 0);
        maxHit("fever_spider", "melee", 4); // was 7
        // Infernal Mage
        row("infernal_mage", 66, 60, 1, 1, 60, 75, 1, 4, 0, 0, 0, 40, 0, 0, 0, 0);
        // Brine rat
        row("brine_rat", 70, 50, 70, 79, 40, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "rat");
        maxHit("brine_rat", "melee", 9); // was 6
        // Bloodveld (Normal)
        row("bloodveld", 76, 120, 75, 45, 30, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "demon");
        // Mutated Bloodveld
        row("mutated_bloodveld", 123, 170, 110, 115, 30, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "demon");
        // Jelly (Regular)
        row("jelly", 78, 75, 45, 45, 120, 45, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Turoth (Mum)
        row("turoth", 87, 79, 56, 86, 86, 1, 1, 4, 0, 20, 20, 0, 0, 0, 0, 0, "leafy");
        // Zygomite (Level 74)
        row("mutated_zygomite", 74, 65, 65, 65, 65, 65, 65, 4, 10, 10, 10, 20, 20, 30, 0, 0);
        // Cave horror (Alpha)
        row("cave_horror", 80, 55, 80, 77, 62, 80, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Aberrant spectre
        row("aberrant_spectre", 96, 90, 1, 1, 90, 105, 1, 4, 20, 20, 20, 0, -15, 0, 0, 0, "spectral", "undead");
        // Spiritual ranger (Bandos)
        row("spiritual_ranger", 115, 131, 1, 1, 96, 1, 121, 4, 12, 14, 13, 5, 13, 0, 0, 0, "spectral");
        maxHit("spiritual_ranger", "ranged", 13); // was 14
        // Spiritual warrior (Armadyl)
        row("spiritual_warrior", 123, 98, 1, 1, 120, 1, 141, 3, 23, 25, 13, 35, 35, 0, 0, 110, "spectral");
        // Spiritual mage (Saradomin)
        row("spiritual_mage", 120, 85, 1, 1, 86, 160, 1, 4, 8, 7, 3, 16, 2, 0, 0, 0, "spectral");
        maxHit("spiritual_mage", "magic", 20); // was 18
        // Dust devil (Smoke Dungeon)
        row("dust_devil", 93, 105, 105, 70, 40, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Spitting Wyvern
        row("spitting_wyvern", 139, 200, 125, 80, 90, 125, 125, 6, 50, 70, 70, 140, 70, 0, 0, 0, "dragon");
        maxHit("spitting_wyvern", "melee", 9); // was 13
        // Taloned Wyvern
        row("taloned_wyvern", 147, 200, 120, 110, 90, 90, 90, 6, 50, 70, 70, 140, 70, 0, 0, 0, "dragon");
        // Ankou (Level 75)
        row("ankou", 75, 60, 70, 70, 60, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "spectral", "undead");
        // Hellhound (Level 122)
        row("hellhound", 122, 116, 105, 104, 102, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "demon");
        // Fire giant (Level 86)
        row("fire_giant", 86, 111, 65, 65, 65, 1, 1, 5, 0, 3, 2, 0, 0, 29, 0, 0, "fiery");
        // Kalphite Worker
        row("kalphite_worker", 28, 40, 20, 20, 20, 1, 1, 4, 5, 5, 1, 10, 10, 0, 0, 0, "kalphite");
        // Kalphite Soldier (Kalphite Cave (task only))
        row("kalphite_soldier", 85, 90, 70, 70, 70, 1, 1, 4, 25, 25, 5, 50, 30, 0, 0, 0, "kalphite");
        maxHit("kalphite_soldier", "melee", 8); // was 10
        // Kalphite Guardian
        row("kalphite_guardian", 141, 170, 110, 110, 110, 1, 1, 4, 25, 25, 5, 50, 30, 0, 0, 0, "kalphite");
        maxHit("kalphite_guardian", "melee", 12); // was 15
        // Mountain troll (Level 69)
        row("mountain_troll", 69, 90, 40, 75, 40, 1, 1, 6, 0, 0, 10, 200, 40, 20, 0, 0);
        maxHit("mountain_troll", "melee", 11); // was 8
        // Ice troll male (Variant 1)
        row("ice_troll", 82, 80, 80, 80, 40, 1, 1, 4, 30, 60, 30, 0, 0, 60, 0, 0);
        maxHit("ice_troll", "melee", 17); // was 9
        // Ogre (Level 53)
        row("ogre", 53, 60, 43, 43, 43, 1, 1, 6, 0, 0, 0, 0, 0, 22, 0, 0);
        // Level variants placed where OSRS spawns them (slayer module's SpawnVariants)
        row("ogre_lvl63", 63, 60, 54, 54, 54, 1, 1, 4, 19, 23, 24, 0, 0, 8, 0, 0);
        row("goblin_lvl5", 5, 12, 3, 1, 4, 1, 1, 6, 0, 0, 0, 0, 0, 12, 0, 0);
        row("dark_wizard_lvl11", 11, 15, 5, 5, 10, 10, 1, 4, 0, 0, 0, 5, 0, 0, 0, 0);
        row("dark_wizard_lvl23", 23, 25, 20, 10, 20, 25, 1, 4, 0, 0, 0, 5, 0, 0, 0, 0);
        row("zombie_lvl18", 18, 24, 13, 13, 18, 1, 1, 5, 0, 0, 0, 0, 0, 5, 0, 0, "undead");
        row("zombie_lvl24", 24, 30, 19, 21, 16, 1, 1, 5, 9, 8, 12, 10, 11, 5, 0, 0, "undead");
        row("skeleton_lvl25", 25, 17, 24, 24, 24, 1, 1, 4, 9, 11, -2, 1, 4, 15, 0, 0, "undead");
        // Ent (Wilderness, level 101): every placed ent is a Wilderness one
        row("ent", 101, 105, 80, 95, 75, 1, 1, 4, 50, 70, 70, 40, 30, 0, 0, 0);
        // Cyclops (Level 76)
        row("cyclops", 76, 100, 65, 65, 35, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("cyclops", "melee", 7); // was 9
        // Dagannoth (Level 74 (1))
        row("dagannoth", 74, 70, 68, 70, 50, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Elf Warrior (1)
        row("elf_warrior", 108, 105, 95, 95, 80, 1, 1, 4, 50, 70, 70, 60, 50, 0, 0, 0);
        maxHit("elf_warrior", "melee", 10); // was 11
        // Lizardman (Level 62)
        row("lizardman", 62, 60, 53, 53, 52, 1, 1, 4, -20, 20, 5, 0, 0, 28, 0, 28);
        // Lizardman shaman (Standard)
        row("lizardman_shaman", 150, 150, 120, 120, 140, 130, 120, 4, -20, 40, 30, 50, 0, 45, 0, 45);
        // Waterfiend (Normal)
        row("waterfiend", 115, 128, 1, 1, 128, 105, 105, 4, 100, 100, 10, 100, 100, 0, 0, 0, "demon");
        maxHit("waterfiend", "magic", 11); // was 14
        // Wyrm (Attacking)
        row("wyrm", 97, 120, 85, 60, 80, 80, 80, 4, 10, 50, 50, 50, 20, 0, 0, 0, "dragon");
        // Kurask (Normal)
        row("kurask", 106, 97, 67, 105, 105, 1, 1, 4, 0, 20, 20, 0, 0, 0, 0, 0, "leafy");
        maxHit("kurask", "melee", 11); // was 16
        // Skeletal Wyvern (1)
        row("skeletal_wyvern", 140, 200, 125, 116, 120, 125, 120, 6, 140, 90, 90, 80, 140, 0, 0, 0, "dragon");
        maxHit("skeletal_wyvern", "ranged", 13); // was 16
        // Gargoyle (Basement)
        row("gargoyle", 111, 105, 75, 105, 107, 1, 1, 4, 50, 60, -20, 20, 20, 0, 0, 0, "golem");
        // Nechryael (Normal)
        row("nechryael", 115, 105, 97, 97, 105, 1, 1, 4, 20, 20, 20, 0, 20, 0, 0, 0, "demon");
        maxHit("nechryael", "melee", 11); // was 9
        // Greater Nechryael (Regular)
        row("greater_nechryael", 200, 205, 197, 197, 85, 1, 1, 4, 50, 50, 50, 0, 50, 0, 0, 0, "demon");
        maxHit("greater_nechryael", "melee", 21); // was 18
        // Abyssal demon (Standard)
        row("abyssal_demon", 124, 150, 97, 67, 135, 1, 1, 4, 20, 20, 20, 0, 20, 0, 0, 0, "demon");
        // Cave kraken (Cave kraken)
        row("cave_kraken", 127, 125, 1, 1, 150, 120, 1, 6, 0, 0, 0, -63, 100, 0, 0, 0);
        // Dark beast
        row("dark_beast", 182, 220, 140, 160, 120, 160, 1, 4, 30, 40, 100, 90, 100, 0, 0, 0);
        // Drake
        row("drake", 192, 225, 140, 118, 120, 112, 140, 4, 5, 60, 60, 20, 100, 0, 0, 40, "dragon");
        maxHit("drake", "ranged", 15); // was 14
        // Smoke devil
        row("smoke_devil", 160, 185, 140, 130, 275, 1, 195, 4, 0, 0, 0, 600, 44, 0, 0, 0);
        // Hydra
        row("hydra", 194, 300, 1, 1, 100, 210, 210, 6, 160, 160, 160, 160, 0, 0, 0, 0, "dragon");
        // Basilisk Knight
        row("basilisk_knight", 204, 300, 186, 186, 186, 186, 1, 4, 30, 30, -15, 30, 20, 0, 0, 0);
        // Baby green dragon (1)
        row("baby_green_dragon", 48, 50, 40, 40, 40, 1, 1, 4, 0, 50, 50, 40, 30, 0, 0, 0, "dragon");
        // Baby blue dragon (1)
        row("baby_blue_dragon", 48, 50, 40, 40, 40, 1, 1, 4, 0, 50, 50, 40, 30, 0, 0, 0, "dragon");
        // Baby red dragon (1)
        row("baby_red_dragon", 48, 50, 40, 40, 40, 1, 1, 4, 0, 50, 50, 40, 30, 0, 0, 0, "dragon");
        maxHit("baby_red_dragon", "melee", 5); // was 7
        // Baby black dragon (Normal)
        row("baby_black_dragon", 83, 80, 70, 70, 70, 1, 1, 4, 0, 50, 50, 40, 30, 0, 0, 0, "dragon");
        maxHit("baby_black_dragon", "melee", 11); // was 9
        // Green dragon (Level 79)
        row("green_dragon", 79, 75, 68, 68, 68, 68, 1, 4, 0, 40, 40, 30, 50, 0, 0, 0, "dragon", "fiery");
        // Blue dragon (1)
        row("blue_dragon", 111, 105, 95, 95, 95, 1, 1, 4, 0, 70, 70, 60, 50, 0, 0, 0, "dragon", "fiery");
        // Red dragon (1)
        row("red_dragon", 152, 140, 130, 130, 130, 1, 1, 4, 0, 70, 70, 60, 50, 0, 0, 0, "dragon", "fiery");
        maxHit("red_dragon", "melee", 14); // was 15
        // Black dragon (Level 227)
        row("black_dragon", 227, 190, 200, 200, 200, 100, 1, 4, 0, 70, 70, 60, 50, 0, 0, 0, "dragon", "fiery");
        // Brutal green dragon
        row("brutal_green_dragon", 227, 175, 268, 168, 168, 168, 1, 4, 0, 70, 70, 60, 50, 0, 0, 0, "dragon", "fiery");
        // Brutal blue dragon (Catacombs of Kourend)
        row("brutal_blue_dragon", 271, 245, 298, 198, 198, 198, 1, 4, 0, 70, 70, 60, 50, 0, 0, 0, "dragon", "fiery");
    }

    private static void rows1() {
        // Brutal red dragon
        row("brutal_red_dragon", 289, 285, 310, 210, 198, 250, 1, 4, 0, 70, 70, 60, 50, 0, 0, 0, "dragon", "fiery");
        // Brutal black dragon
        row("brutal_black_dragon", 318, 315, 330, 210, 258, 250, 1, 4, 0, 70, 70, 60, 50, 0, 0, 0, "dragon", "fiery");
        // Lava dragon
        row("lava_dragon", 252, 230, 240, 220, 220, 1, 1, 4, 20, 90, 90, 80, 70, 0, 0, 0, "dragon", "fiery");
        maxHit("lava_dragon", "melee", 23); // was 20
        // Bronze dragon (Standard)
        row("bronze_dragon", 131, 122, 112, 112, 112, 100, 1, 4, 0, 70, 70, 30, 90, 0, 0, 0, "dragon", "fiery");
        maxHit("bronze_dragon", "melee", 12); // was 16
        // Iron dragon (Standard)
        row("iron_dragon", 189, 165, 165, 165, 165, 100, 1, 4, 0, 70, 70, 30, 90, 0, 0, 0, "dragon", "fiery");
        maxHit("iron_dragon", "melee", 17); // was 21
        // Steel dragon (Level 246)
        row("steel_dragon", 246, 210, 215, 215, 215, 100, 1, 4, 0, 70, 70, 30, 90, 0, 0, 0, "dragon", "fiery");
        maxHit("steel_dragon", "melee", 22); // was 28
        // Mithril dragon
        row("mithril_dragon", 304, 254, 268, 268, 268, 168, 168, 4, 0, 100, 70, 30, 90, 0, 0, 0, "dragon", "fiery");
        // Adamant dragon
        row("adamant_dragon", 338, 295, 280, 280, 272, 186, 186, 4, 20, 110, 85, 30, 95, 0, 0, 0, "dragon", "fiery");
        maxHit("adamant_dragon", "melee", 29); // was 33
        // Rune dragon
        row("rune_dragon", 380, 330, 284, 284, 276, 196, 246, 4, 20, 115, 90, 30, 95, 0, 0, 0, "dragon", "fiery");
        maxHit("rune_dragon", "melee", 29); // was 38
        // Crushing hand
        row("crushing_hand", 45, 55, 45, 42, 14, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "undead");
        maxHit("crushing_hand", "melee", 5); // was 4
        // Chasm Crawler (Standard)
        row("chasm_crawler", 68, 60, 64, 59, 55, 1, 1, 4, 41, 43, 23, 24, 20, 0, 0, 0);
        maxHit("chasm_crawler", "melee", 7); // was 6
        // Screaming banshee
        row("screaming_banshee", 70, 61, 65, 61, 56, 1, 1, 4, 13, 13, 13, 0, 13, 0, 0, 0, "spectral", "undead");
        // Giant rockslug
        row("giant_rockslug", 86, 77, 72, 77, 77, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("giant_rockslug", "melee", 9); // was 8
        // Cockathrice
        row("cockathrice", 89, 95, 63, 78, 78, 1, 1, 4, 20, 20, 0, 20, 0, 0, 0, 0);
        maxHit("cockathrice", "melee", 9); // was 6
        // Flaming pyrelord
        row("flaming_pyrelord", 97, 125, 98, 65, 52, 1, 1, 4, 18, 18, 18, 0, 18, 0, 0, 0, "demon");
        // Monstrous basilisk
        row("monstrous_basilisk", 135, 170, 88, 98, 130, 1, 1, 4, 35, 35, 0, 35, 0, 0, 0, 0);
        maxHit("monstrous_basilisk", "melee", 11); // was 13
        // Malevolent Mage
        row("malevolent_mage", 162, 175, 1, 1, 135, 175, 1, 4, 0, 0, 0, 75, 0, 0, 0, 0);
        maxHit("malevolent_mage", "magic", 20); // was 14
        // Insatiable Bloodveld
        row("insatiable_bloodveld", 202, 290, 190, 145, 85, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "demon");
        // Insatiable mutated Bloodveld
        row("insatiable_mutated_bloodveld", 278, 410, 250, 190, 130, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "demon");
        // Vitreous Jelly
        row("vitreous_jelly", 206, 190, 170, 150, 220, 150, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Spiked Turoth
        row("spiked_turoth", 244, 195, 204, 280, 154, 1, 1, 4, 0, 40, 40, 0, 0, 0, 0, 0, "leafy");
        maxHit("spiked_turoth", "melee", 29); // was 17
        // Cave abomination
        row("cave_abomination", 206, 130, 230, 195, 142, 230, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Abhorrent spectre
        row("abhorrent_spectre", 253, 250, 1, 1, 180, 300, 1, 4, 40, 40, 40, 0, -30, 0, 0, 0, "spectral", "undead");
        maxHit("abhorrent_spectre", "magic", 31); // was 15
        // Choke devil
        row("choke_devil", 264, 300, 260, 230, 120, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Shadow Wyrm (Attacking)
        row("shadow_wyrm", 259, 290, 300, 180, 125, 160, 125, 4, 20, 100, 100, 50, 0, 0, 0, 0, "dragon");
        // King kurask
        row("king_kurask", 295, 270, 190, 320, 250, 1, 1, 4, 0, 50, 50, 0, 0, 0, 0, 0, "leafy");
        maxHit("king_kurask", "melee", 33); // was 31
        // Marble gargoyle
        row("marble_gargoyle", 349, 270, 230, 250, 190, 1, 220, 4, 50, 50, 0, 50, 50, 0, 0, 0, "golem");
        // Nechryarch
        row("nechryarch", 300, 320, 310, 260, 140, 1, 1, 4, 30, 30, 30, 0, 30, 0, 0, 0, "demon");
        maxHit("nechryarch", "melee", 27); // was 17
        // Greater abyssal demon
        row("greater_abyssal_demon", 342, 400, 300, 260, 240, 1, 1, 4, 50, 50, 50, 0, 50, 0, 0, 0, "demon");
        // Night beast
        row("night_beast", 374, 550, 270, 290, 220, 300, 1, 4, 75, 80, 200, 190, 200, 0, 0, 0);
        // Guardian Drake
        row("guardian_drake", 376, 590, 270, 280, 200, 300, 240, 4, 20, 100, 100, 20, 150, 0, 0, 40, "dragon");
        maxHit("guardian_drake", "melee", 35); // was 23
        // Nuclear smoke devil
        row("nuclear_smoke_devil", 280, 240, 240, 230, 390, 1, 280, 4, 0, 0, 0, 850, 80, 0, 0, 0);
        // Colossal Hydra
        row("colossal_hydra", 309, 750, 1, 1, 100, 250, 250, 6, 100, 200, 200, 200, 20, 0, 0, 0, "dragon");
        // Basilisk Sentinel
        row("basilisk_sentinel", 358, 520, 274, 274, 274, 274, 1, 4, 50, 50, 10, 50, 10, 12, 0, 0);
        // Screaming twisted banshee
        row("screaming_twisted_banshee", 144, 220, 103, 110, 80, 1, 1, 4, 25, 25, 25, 0, 25, 0, 0, 0, "spectral", "undead");
        maxHit("screaming_twisted_banshee", "melee", 12);
        // Vitreous Warped Jelly
        row("vitreous_warped_jelly", 241, 220, 200, 180, 250, 180, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("vitreous_warped_jelly", "melee", 19);
        // Repugnant spectre
        row("repugnant_spectre", 335, 380, 1, 1, 220, 380, 1, 4, 120, 120, 120, 0, 115, 0, 0, 0, "spectral", "undead");
        maxHit("repugnant_spectre", "magic", 39);
        // Aviansie (Level 79 (1))
        row("gwd_aviansie", 79, 83, 1, 1, 70, 1, 85, 4, 0, 0, 0, 0, 0, 0, 0, 0, "flying");
        maxHit("gwd_aviansie", "feather throw", 9); // was 10
        // Aviansie (Level 131)
        row("gwd_aviansie_elite", 131, 115, 1, 1, 175, 1, 143, 4, 0, 0, 0, 0, 0, 0, 0, 0, "flying");
        // Spiritual warrior (Armadyl)
        row("gwd_spiritual_warrior_armadyl", 123, 98, 1, 1, 120, 1, 141, 3, 23, 25, 13, 35, 35, 0, 0, 110, "spectral");
        // Spiritual ranger (Armadyl)
        row("gwd_spiritual_ranger_armadyl", 127, 89, 1, 1, 130, 1, 150, 3, 17, 16, 8, 35, 41, 0, 0, 0, "spectral");
        maxHit("gwd_spiritual_ranger_armadyl", "arrow", 16); // was 18
        // Spiritual mage (Armadyl)
        row("gwd_spiritual_mage_armadyl", 123, 86, 1, 1, 111, 150, 1, 3, 9, 12, 5, 45, 28, 0, 0, 0, "spectral");
        maxHit("gwd_spiritual_mage_armadyl", "god spell", 17); // was 18
        // Hobgoblin (Hobgoblin (GWD))
        row("gwd_hobgoblin", 47, 52, 39, 39, 35, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("gwd_hobgoblin", "spear", 5); // was 7
        // Ork
        row("gwd_ork", 107, 110, 100, 100, 60, 1, 1, 5, 12, 14, 13, 5, 13, 19, 0, 0);
        maxHit("gwd_ork", "melee", 12); // was 14
        // Ogre (GWD)
        row("gwd_ogre", 58, 70, 46, 48, 43, 1, 1, 6, 0, 0, 0, 0, 0, 22, 0, 0);
        // Jogre (GWD)
        row("gwd_jogre", 58, 70, 46, 48, 43, 1, 1, 6, 0, 0, 0, 0, 0, 22, 0, 0);
        // Cyclops (Level 76)
        row("gwd_cyclops", 76, 100, 65, 65, 35, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("gwd_cyclops", "melee", 7); // was 11
        // Spiritual warrior (Bandos)
        row("gwd_spiritual_warrior_bandos", 134, 131, 116, 120, 100, 1, 1, 5, 12, 14, 13, 5, 13, 29, 0, 110, "spectral");
        // Spiritual ranger (Bandos)
        row("gwd_spiritual_ranger_bandos", 115, 131, 1, 1, 96, 1, 121, 4, 12, 14, 13, 5, 13, 0, 0, 0, "spectral");
        maxHit("gwd_spiritual_ranger_bandos", "arrow", 13); // was 18
        // Spiritual mage (Bandos)
        row("gwd_spiritual_mage_bandos", 121, 106, 1, 1, 103, 142, 1, 5, 12, 14, 13, 35, 13, 0, 0, 0, "spectral");
        maxHit("gwd_spiritual_mage_bandos", "god spell", 17); // was 18
        // Knight of Saradomin (Level 101)
        row("gwd_knight_of_saradomin", 101, 108, 75, 90, 82, 60, 1, 6, 12, 14, 13, 0, 13, 13, 0, 0);
        // Saradomin priest
        row("gwd_saradomin_priest", 113, 89, 120, 46, 120, 125, 1, 5, 12, 14, 13, 5, 13, 9, 0, 0);
        maxHit("gwd_saradomin_priest", "holy light", 20); // was 16
        // Spiritual warrior (Saradomin)
        row("gwd_spiritual_warrior_saradomin", 125, 110, 110, 106, 110, 1, 1, 5, 12, 14, 13, 5, 13, 9, 0, 110, "spectral");
        maxHit("gwd_spiritual_warrior_saradomin", "melee", 13); // was 16
        // Spiritual ranger (Saradomin)
        row("gwd_spiritual_ranger_saradomin", 122, 106, 1, 1, 100, 1, 146, 4, 3, 5, 13, 16, 23, 0, 0, 0, "spectral");
        maxHit("gwd_spiritual_ranger_saradomin", "arrow", 16); // was 18
        // Spiritual mage (Saradomin)
        row("gwd_spiritual_mage_saradomin", 120, 85, 1, 1, 86, 160, 1, 4, 8, 7, 3, 16, 2, 0, 0, 0, "spectral");
        maxHit("gwd_spiritual_mage_saradomin", "god spell", 20); // was 18
        // Imp (GWD)
        row("gwd_imp", 7, 10, 5, 5, 6, 1, 1, 4, -42, -42, -42, -42, -42, -42, 0, 0, "demon");
        // Werewolf (God Wars Dungeon (1))
        row("gwd_werewolf", 93, 92, 77, 76, 85, 1, 1, 4, -21, -21, -21, -21, -21, 0, 0, 0);
        maxHit("gwd_werewolf", "claws", 9); // was 12
        // Bloodveld (GWD)
        row("gwd_bloodveld", 81, 134, 80, 46, 30, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "demon");
        // Hellhound (Level 127)
        row("gwd_hellhound", 127, 116, 107, 116, 106, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "demon");
        maxHit("gwd_hellhound", "bite", 13); // was 14
        // Gorak (GWD)
        row("gwd_gorak", 149, 128, 133, 126, 135, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Pyrefiend (Level 48)
        row("gwd_pyrefiend", 48, 48, 60, 36, 22, 1, 1, 4, 10, 10, 10, 0, 10, 0, 0, 0, "demon", "fiery");
        // Spiritual warrior (Zamorak)
        row("gwd_spiritual_warrior_zamorak", 115, 100, 100, 100, 100, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 110, "spectral");
        maxHit("gwd_spiritual_warrior_zamorak", "melee", 11); // was 16
        // Spiritual ranger (Zamorak)
        row("gwd_spiritual_ranger_zamorak", 118, 120, 1, 1, 80, 1, 140, 4, 0, 0, 0, 0, 0, 0, 0, 0, "spectral");
        maxHit("gwd_spiritual_ranger_zamorak", "arrow", 15); // was 18
        // Spiritual mage (Zamorak)
        row("gwd_spiritual_mage_zamorak", 121, 75, 1, 1, 61, 180, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "spectral");
        maxHit("gwd_spiritual_mage_zamorak", "god spell", 19); // was 18
        // Flight Kilisa
        row("flight_kilisa", 159, 133, 124, 118, 175, 50, 169, 5, 0, 0, 0, 0, 0, 0, 0, 0, "flying");
        maxHit("flight_kilisa", "melee", 15); // was 16
        // Wingman Skree
        row("wingman_skree", 143, 121, 80, 50, 160, 150, 100, 5, 0, 0, 0, 0, 0, 45, 0, 0, "flying");
        maxHit("wingman_skree", "wind strike", 16); // was 15
        // Flockleader Geerin
        row("flockleader_geerin", 149, 132, 80, 80, 175, 50, 150, 5, 0, 0, 0, 0, 0, 0, 0, 60, "flying");
        maxHit("flockleader_geerin", "feathers", 25); // was 15
        // Starlight
        row("starlight", 149, 160, 120, 125, 120, 125, 1, 5, 12, 14, 13, 5, 13, 60, 0, 0);
        maxHit("starlight", "melee", 15); // was 16
        // Growler
        row("growler", 139, 146, 100, 101, 120, 150, 1, 5, 12, 14, 14, 18, 5, 10, 0, 0);
        // Bree
        row("bree", 146, 162, 110, 80, 130, 80, 150, 5, 12, 14, 14, 18, 5, 10, 0, 0);
        // Sergeant Strongstack
        row("sergeant_strongstack", 141, 128, 124, 118, 125, 50, 50, 5, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("sergeant_strongstack", "melee", 15); // was 17
        // Sergeant Steelwill
        row("sergeant_steelwill", 142, 127, 80, 50, 150, 150, 1, 5, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("sergeant_steelwill", "magic", 15); // was 16
        // Sergeant Grimspike
        row("sergeant_grimspike", 142, 146, 80, 80, 132, 50, 150, 5, 0, 0, 0, 0, 0, 0, 0, 20);
        // Tstanon Karlak
        row("tstanon_karlak", 145, 142, 124, 118, 125, 50, 50, 5, 0, 0, 0, -5, 0, 0, 0, 0, "demon");
        // Zakl'n Gritch
        row("zakln_gritch", 142, 150, 83, 76, 127, 50, 150, 5, 0, 0, 0, -5, 0, 0, 0, 20, "demon");
        // Balfrug Kreeyath
        row("balfrug_kreeyath", 151, 161, 115, 60, 153, 150, 1, 5, 0, 0, 0, 10, 0, 0, 0, 0, "demon");
        // Blood Reaver (Nex's chamber)
        row("blood_reaver", 161, 150, 1, 1, 100, 190, 1, 5, 20, 80, 120, 300, 55, 0, 0, 0, "demon");
        // Skeleton Hellhound (Vet'ion)
        row("skeleton_hellhound", 194, 30, 210, 250, 150, 1, 1, 4, 101, 103, 10, 180, 266, 0, 0, 0, "undead");
        maxHit("skeleton_hellhound", "bite", 26); // was 15
        // Greater Skeleton Hellhound (Vet'ion)
        row("greater_skeleton_hellhound", 231, 30, 228, 295, 180, 1, 1, 4, 150, 163, 20, 210, 275, 0, 0, 0, "undead");
        maxHit("greater_skeleton_hellhound", "bite", 32); // was 20
        // Venenatis' Spiderling (monster)
        row("venenatis_spiderling", 12, 5, 15, 20, 1, 10, 20, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("venenatis_spiderling", "bite", 3); // was 6
        // Scorpia's offspring (monster)
        row("scorpias_offspring", 15, 2, 1, 1, 1, 1, 30, 4, 0, 0, 0, -40, 0, 0, 0, 900);
        // Scorpia's guardian
        row("scorpias_guardian", 47, 70, 1, 1, 60, 30, 30, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Revenant imp
        row("revenant_imp", 7, 10, 5, 5, 4, 9, 5, 5, 0, 0, 0, 5, 0, 0, 5, 0, "spectral", "undead");
        // Revenant goblin
        row("revenant_goblin", 15, 14, 13, 14, 14, 12, 15, 5, 25, 28, 31, 1, 31, 6, 37, 21, "spectral", "undead");
        // Revenant pyrefiend
        row("revenant_pyrefiend", 52, 48, 60, 40, 33, 67, 40, 5, 45, 40, 50, 15, 10, 0, 0, 0, "spectral", "undead");
        // Revenant hobgoblin
        row("revenant_hobgoblin", 60, 72, 50, 50, 41, 55, 60, 4, 65, 60, 68, 30, 50, 20, 5, 25, "spectral", "undead");
        // Revenant cyclops
        row("revenant_cyclops", 82, 110, 60, 73, 49, 65, 70, 6, 110, 130, 135, 10, 135, 53, 0, 0, "spectral", "undead");
        // Revenant hellhound
        row("revenant_hellhound", 90, 80, 76, 80, 80, 104, 80, 5, 98, 140, 142, 62, 140, 38, 30, 0, "spectral", "undead");
        // Revenant demon
        row("revenant_demon", 98, 80, 83, 76, 80, 120, 80, 5, 124, 118, 130, 85, 90, 30, 50, 40, "spectral", "undead");
        // Revenant ork
        row("revenant_ork", 105, 105, 99, 100, 60, 110, 130, 5, 118, 150, 146, 50, 148, 60, 0, 50, "spectral", "undead");
        // Revenant dark beast
        row("revenant_dark_beast", 120, 140, 93, 110, 80, 130, 135, 5, 113, 152, 155, 70, 158, 65, 0, 45, "spectral", "undead");
        // Revenant knight
        row("revenant_knight", 126, 143, 100, 119, 80, 146, 146, 5, 145, 200, 180, 95, 190, 69, 55, 55, "spectral", "undead");
    }

    private static void rows2() {
        // Revenant dragon
        row("revenant_dragon", 135, 155, 106, 126, 87, 150, 151, 5, 143, 206, 188, 101, 197, 72, 61, 60, "spectral", "undead");
        // Snakeling (Melee)
        row("snakeling", 90, 1, 140, 138, 1, 1, 1, 3, -40, -40, -40, -40, -40, 120, 0, 0);
        // Zombified Spawn (Post-quest)
        row("zombified_spawn", 64, 38, 82, 82, 6, 1, 1, 4, 3, 3, 3, -100, 3, 0, 0, 0, "undead");
        // Enormous Tentacle (Enormous Tentacle)
        row("enormous_tentacle", 112, 120, 1, 1, 1, 1, 150, 4, 0, 0, 0, -15, 270, 0, 0, 0);
        // Respiratory system
        row("respiratory_system", 0, 50, 1, 1, 80, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "demon");
        // Spawn
        row("abyssal_spawn", 55, 15, 120, 30, 10, 1, 50, 4, 0, 0, 0, -10, 0, 0, 0, 0);
        // Scion
        row("abyssal_scion", 93, 50, 120, 90, 50, 1, 100, 4, 0, 0, 0, -10, 0, 30, 0, 30);
        maxHit("abyssal_scion", "spine", 16); // was 20
        // Dark energy core
        row("dark_energy_core", 75, 25, 1, 1, 20, 1, 1, 2, 10, 10, 10, -5, 10, 0, 0, 0);
        // Reanimated demon spawn
        row("reanimated_demon_spawn", 87, 85, 80, 70, 71, 1, 1, 4, 0, 0, 0, -10, 0, 0, 0, 0, "demon");
        // Flower
        row("hesporis_flower", 0, 10, 1, 1, 1, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Spawn of Sarachnis (Level 107)
        row("spawn_of_sarachnis", 107, 30, 150, 120, 50, 1, 1, 3, 50, 50, 20, -10, 150, 50, 0, 0);
        maxHit("spawn_of_sarachnis", "bite", 13); // was 12
        // Husk (Magic)
        row("nightmare_husk", 48, 20, 1, 1, 20, 80, 80, 4, 20, 20, 20, 80, 80, 0, 0, 0);
        maxHit("nightmare_husk", "grasp", 14); // was 6
        // Parasite (Weakened)
        row("nightmare_parasite", 57, 40, 1, 1, 40, 60, 60, 4, 50, 70, 10, 100, 100, 0, 12, 12);
        // Giant rat (Scurrius)
        row("scurrius_giant_rat", 46, 15, 100, 25, 10, 1, 1, 4, 0, 0, 0, 0, 0, 68, 0, 0, "rat");
        maxHit("scurrius_giant_rat", "bite", 4); // was 3
        // Acidic Araxyte
        row("acidic_araxyte", 114, 58, 150, 120, 50, 1, 60, 6, 25, 25, 25, -10, 25, 180, 0, 450);
        // Mirrorback Araxyte
        row("mirrorback_araxyte", 114, 58, 150, 120, 50, 1, 60, 6, 25, 25, 25, -10, 25, 180, 0, 450);
        maxHit("mirrorback_araxyte", "bite", 15); // was 16
        // Ruptura Araxyte
        row("ruptura_araxyte", 114, 58, 150, 120, 50, 1, 60, 6, 25, 25, 25, -10, 25, 180, 0, 450);
        // Ice elemental (Royal Titans)
        row("titan_ice_elemental", 37, 40, 30, 30, 30, 30, 30, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Fire elemental (Royal Titans)
        row("titan_fire_elemental", 37, 40, 35, 35, 20, 20, 20, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Tz-Kih
        row("tz_kih", 22, 10, 20, 30, 15, 15, 30, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Tz-Kek (Level 45)
        row("tz_kek", 45, 20, 40, 60, 30, 30, 60, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Tz-Kek (Level 22)
        row("tz_kek_small", 22, 10, 20, 30, 15, 15, 30, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Tok-Xil (Standard)
        row("tok_xil", 90, 40, 80, 120, 60, 60, 120, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Yt-MejKot (Standard)
        row("yt_mejkot", 180, 80, 160, 240, 120, 120, 240, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Ket-Zek (Standard)
        row("ket_zek", 360, 160, 320, 480, 240, 240, 480, 4, 0, 0, 0, 0, 0, 0, 60, 0);
        maxHit("ket_zek", "magic", 52); // was 49
        // Yt-HurKot (Level 108)
        row("yt_hurkot", 108, 60, 140, 100, 60, 120, 120, 4, 0, 0, 0, 100, 100, 0, 0, 0);
        // Jal-Nib
        row("jal_nib", 32, 10, 1, 1, 15, 15, 1, 4, -20, -20, -20, -20, -20, 0, 0, 0);
        // Jal-MejRah
        row("jal_mejrah", 85, 25, 1, 1, 55, 120, 120, 3, 30, 30, 30, -20, 45, 0, 0, 30);
        // Jal-Ak
        row("jal_ak", 165, 40, 160, 160, 95, 160, 160, 6, 25, 25, 25, 25, 25, 0, 45, 45);
        // Jal-AkRek-Ket
        row("jal_akrek_ket", 70, 15, 120, 120, 95, 1, 1, 4, 25, 25, 25, 0, 0, 0, 0, 0);
        // Jal-AkRek-Xil
        row("jal_akrek_xil", 70, 15, 1, 1, 95, 1, 120, 4, 0, 0, 0, 0, 25, 0, 0, 25);
        // Jal-AkRek-Mej
        row("jal_akrek_mej", 70, 15, 1, 1, 95, 120, 1, 4, 0, 0, 0, 25, 0, 0, 25, 0);
        // Jal-ImKot
        row("jal_imkot", 240, 75, 210, 290, 120, 120, 220, 4, 65, 65, 65, 30, 50, 0, 0, 0);
        // Jal-Xil
        row("jal_xil", 370, 125, 140, 180, 60, 90, 250, 4, 0, 0, 0, 0, 0, 0, 0, 40);
        // Jal-Zek
        row("jal_zek", 490, 220, 370, 510, 260, 300, 510, 4, 0, 0, 0, 0, 0, 0, 80, 0);
        // JalTok-Jad
        row("jaltok_jad", 900, 350, 750, 1020, 480, 510, 1020, 8, 0, 0, 0, 0, 0, 0, 100, 80);
        // Jal-MejJak
        row("jal_mejjak", 250, 75, 1, 1, 100, 1, 1, 3, 0, 0, 0, 0, 0, 0, 0, 0);
        // Void Knight (Pest Control) (1)
        row("void_knight", 0, 200, 1, 1, 50, 50, 1, 4, 100, 100, 100, 100, 100, 0, 0, 0);
        // Brawler (Level 129)
        row("brawler", 129, 143, 67, 132, 118, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("brawler", "melee", 14); // was 15
        // Defiler (Level 97)
        row("defiler", 97, 97, 1, 1, 65, 1, 117, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("defiler", "ranged", 13); // was 10
        // Ravager (Level 106)
        row("ravager", 106, 83, 67, 157, 53, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("ravager", "melee", 17); // was 12
        // Shifter (Level 104)
        row("shifter", 104, 83, 110, 83, 83, 60, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Spinner (Level 74)
        row("spinner", 74, 73, 27, 27, 93, 67, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("spinner", "melee", 4); // was 6
        // Splatter (Level 54)
        row("splatter", 54, 43, 32, 32, 93, 32, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Torcher (Level 79)
        row("torcher", 79, 57, 1, 1, 73, 97, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Penance Fighter (Wave 6)
        row("penance_fighter", 56, 49, 50, 48, 48, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "penance");
        maxHit("penance_fighter", "melee", 6); // was 10
        // Penance Ranger (Wave 7/10)
        row("penance_ranger", 57, 50, 63, 1, 61, 1, 62, 4, 0, 0, 0, 0, 0, 0, 0, 0, "penance");
        maxHit("penance_ranger", "ranged", 7); // was 9
        // Animated Bronze Armour
        row("animated_bronze_armour", 11, 10, 10, 10, 10, 1, 1, 4, 50, 25, 19, 400, 400, 4, 0, 0);
        // Animated Iron Armour
        row("animated_iron_armour", 23, 20, 20, 20, 20, 1, 1, 4, 50, 25, 19, 400, 400, 4, 0, 0);
        // Animated Steel Armour
        row("animated_steel_armour", 46, 40, 40, 40, 40, 1, 1, 4, 50, 25, 19, 400, 400, 4, 0, 0);
        // Animated Black Armour
        row("animated_black_armour", 69, 60, 60, 60, 60, 1, 1, 4, 50, 25, 19, 400, 400, 4, 0, 0);
        // Animated Mithril Armour
        row("animated_mithril_armour", 92, 80, 80, 80, 80, 1, 1, 4, 50, 25, 19, 400, 400, 4, 0, 0);
        maxHit("animated_mithril_armour", "melee", 10); // was 9
        // Animated Adamant Armour
        row("animated_adamant_armour", 113, 99, 99, 99, 99, 1, 1, 4, 50, 25, 19, 400, 400, 4, 0, 0);
        maxHit("animated_adamant_armour", "melee", 12); // was 11
        // Animated Rune Armour
        row("animated_rune_armour", 138, 120, 120, 120, 120, 1, 1, 4, 50, 25, 19, 400, 400, 4, 0, 0);
        // Cyclops (Level 56)
        row("warriors_guild_cyclops", 56, 75, 47, 50, 26, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Cyclops (Level 106)
        row("cyclops_elite", 106, 150, 95, 75, 55, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("cyclops_elite", "melee", 8); // was 11
        // Loar Shade (Shade)
        row("loar_shade", 40, 38, 45, 30, 26, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "shade", "spectral", "undead");
        maxHit("loar_shade", "melee", 4); // was 6
        // Phrin Shade (Shade)
        row("phrin_shade", 60, 56, 64, 47, 42, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "shade", "spectral", "undead");
        maxHit("phrin_shade", "melee", 6); // was 8
        // Riyl Shade (Shade)
        row("riyl_shade", 80, 76, 88, 55, 60, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "shade", "spectral", "undead");
        maxHit("riyl_shade", "melee", 6); // was 11
        // Asyn Shade (Shade)
        row("asyn_shade", 100, 90, 102, 84, 70, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "shade", "spectral", "undead");
        maxHit("asyn_shade", "melee", 9); // was 14
        // Fiyr Shade (Shade)
        row("fiyr_shade", 120, 110, 120, 100, 85, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "shade", "spectral", "undead");
        maxHit("fiyr_shade", "melee", 11); // was 17
        // Woman (1)
        row("woman", 2, 7, 1, 1, 1, 1, 1, 4, -21, -21, -21, -21, -21, 0, 0, 0);
        // Farmer (Green shirt, long sleeves)
        row("farmer", 7, 12, 3, 4, 8, 1, 1, 6, 0, 0, 0, 0, 0, 5, 0, 0);
        // White Knight (Initiate (f))
        row("white_knight", 36, 52, 27, 29, 21, 1, 1, 7, 83, 76, 70, -11, 74, 30, 0, 0);
        maxHit("white_knight", "melee", 6); // was 5
        // Monk (Level 5)
        row("monk", 5, 15, 2, 2, 3, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Dwarf (Worker (8))
        row("dwarf", 7, 10, 6, 6, 6, 1, 1, 4, 0, 0, 0, 5, 10, 5, 0, 0);
        // Highwayman (Hood)
        row("highwayman", 5, 13, 2, 2, 4, 1, 1, 4, 0, 3, 2, 0, 2, 6, 0, 0);
        // Mugger (Regular)
        row("mugger", 6, 8, 5, 5, 5, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Knight of Ardougne (Female)
        row("ardougne_knight", 46, 52, 38, 40, 31, 1, 1, 5, 39, 40, 36, -11, 36, 8, 0, 0);
        // Paladin
        row("paladin", 62, 57, 54, 54, 54, 1, 1, 5, 87, 84, 76, -10, 79, 20, 0, 0);
        maxHit("paladin", "melee", 8); // was 6
        // Hero (1)
        row("hero", 69, 82, 54, 55, 54, 1, 1, 5, 87, 84, 76, -10, 79, 20, 0, 0);
        maxHit("hero", "melee", 9); // was 6
        // Gnome (Level 1, 1)
        row("gnome", 1, 3, 1, 1, 1, 1, 1, 4, -42, -42, -42, -42, -42, -42, 0, 0);
        // Gnome guard (Halberd)
        row("gnome_guard", 23, 31, 17, 17, 17, 1, 1, 6, 0, 0, 0, 0, 0, 8, 0, 0);
        // Pirate (Brimhaven)
        row("pirate", 23, 20, 21, 21, 21, 1, 1, 5, 3, 2, 0, 0, 0, 8, 0, 0);
        // TzHaar-Hur
        row("tzhaar_hur_citizen", 74, 80, 60, 60, 60, 80, 1, 5, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("tzhaar_hur_citizen", "melee", 7); // was 9
        // TzHaar-Ket (Level 149)
        row("tzhaar_ket", 149, 140, 120, 140, 120, 40, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("tzhaar_ket", "melee", 15); // was 16
        // TzHaar-Xil (Knife)
        row("tzhaar_xil", 133, 120, 140, 100, 100, 40, 120, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // TzHaar-Mej
        row("tzhaar_mej", 103, 100, 80, 80, 80, 120, 1, 5, 0, 0, 0, 0, 0, 0, 0, 0);
        // Rock Crab (Active)
        row("rock_crab", 13, 50, 1, 1, 1, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Imp (Normal)
        row("imp", 2, 8, 1, 1, 1, 1, 1, 4, -42, -42, -42, -42, -42, -42, 0, 0, "demon");
        // Sand Crab (Active)
        row("sand_crab", 15, 60, 1, 1, 1, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // King Sand Crab (Active)
        row("king_sand_crab", 107, 200, 50, 50, 100, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("king_sand_crab", "melee", 6); // was 9
        // Lizardman brute (Standard)
        row("lizardman_brute", 75, 60, 65, 65, 65, 1, 1, 4, -20, 30, 10, 0, 0, 34, 0, 34);
        // Deviant spectre
        row("deviant_spectre", 169, 190, 1, 1, 90, 205, 1, 4, 80, 80, 80, 0, 85, 0, 0, 0, "spectral", "undead");
        maxHit("deviant_spectre", "spectral touch", 8); // was 21
        // Twisted Banshee
        row("twisted_banshee", 89, 100, 75, 85, 50, 1, 1, 4, 15, 15, 15, 0, 15, 0, 0, 0, "spectral", "undead");
        // Warped Jelly
        row("warped_jelly", 112, 140, 95, 90, 70, 95, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Abyssal demon (Standard)
        row("catacomb_abyssal_demon", 124, 150, 97, 67, 135, 1, 1, 4, 20, 20, 20, 0, 20, 0, 0, 0, "demon");
        // Greater demon (Level 92)
        row("catacomb_greater_demon", 92, 87, 76, 78, 81, 1, 1, 4, 0, 0, 0, -10, 0, 0, 0, 0, "demon");
        // Black demon (Level 172)
        row("catacomb_black_demon", 172, 157, 145, 148, 152, 1, 1, 4, 0, 0, 0, -10, 0, 0, 0, 0, "demon");
        // Hellhound (Level 122)
        row("catacomb_hellhound", 122, 116, 105, 104, 102, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "demon");
        // Fire giant (Level 86)
        row("catacomb_fire_giant", 86, 111, 65, 65, 65, 1, 1, 5, 0, 3, 2, 0, 0, 29, 0, 0, "fiery");
        // Moss giant (Level 42)
        row("catacomb_moss_giant", 42, 60, 30, 30, 30, 1, 1, 6, 0, 0, 0, 0, 0, 33, 0, 0);
        // Hill Giant
        row("catacomb_hill_giant", 28, 35, 18, 22, 26, 1, 1, 6, 0, 0, 0, 0, 0, 18, 0, 0);
        // Dagannoth (Level 74 (1))
        row("catacomb_dagannoth", 74, 70, 68, 70, 50, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Ankou (Level 75)
        row("catacomb_ankou", 75, 60, 70, 70, 60, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "spectral", "undead");
        // Dust devil (Smoke Dungeon)
        row("catacomb_dust_devil", 93, 105, 105, 70, 40, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Sulphur Nagua
        row("sulphur_nagua", 98, 100, 90, 105, 40, 50, 1, 4, 50, 50, 10, 200, 200, 90, 0, 0, "spectral");
    }

    private static void rows3() {
        // Blood jaguar
        row("blood_jaguar", 40, 100, 40, 40, 28, 1, 1, 6, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("blood_jaguar", "melee", 5); // was 10
        // Poison spider (Level 64)
        row("poison_spider", 64, 64, 50, 58, 52, 1, 1, 4, 20, 17, 10, 14, 14, 0, 0, 0);
        maxHit("poison_spider", "melee", 7); // was 6
        // Chaos druid
        row("chaos_druid", 13, 20, 8, 8, 12, 10, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Giant rat (Level 6)
        row("giant_rat", 6, 10, 6, 5, 2, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "rat");
        maxHit("giant_rat", "melee", 1); // was 2
        // Flesh Crawler (Level 41)
        row("flesh_crawler", 41, 25, 100, 2, 10, 1, 1, 3, 15, 15, 15, 15, 15, 0, 0, 0);
        maxHit("flesh_crawler", "melee", 1); // was 4
        // Catablepon (Level 64)
        row("catablepon", 64, 70, 55, 50, 50, 45, 1, 4, 50, 40, 30, 30, 50, 0, 0, 0);
        // Giant spider (Level 50)
        row("giant_spider", 50, 50, 41, 51, 31, 1, 1, 4, 10, 10, 10, 10, 10, 10, 0, 0);
        maxHit("giant_spider", "melee", 7); // was 5
        // Wild dog (Normal)
        row("wild_dog", 63, 62, 53, 54, 54, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("wild_dog", "melee", 6); // was 8
        // Cave goblin (monster) (Backpack)
        row("cave_goblin", 3, 10, 1, 1, 1, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Cave goblin guard (Level 24)
        row("cave_goblin_guard", 24, 26, 22, 16, 22, 1, 1, 6, 0, 15, 19, -3, 12, 0, -4, 0);
        // Abyssal leech
        row("abyssal_leech", 41, 10, 95, 5, 25, 1, 1, 2, 50, 50, 0, 50, 10, 100, 0, 0);
        maxHit("abyssal_leech", "melee", 2); // was 6
        // Abyssal guardian
        row("abyssal_guardian", 59, 50, 30, 90, 30, 1, 1, 4, 70, 70, 0, 150, 70, 15, 0, 0);
        // Abyssal walker
        row("abyssal_walker", 81, 95, 5, 100, 95, 1, 1, 5, 75, 75, 0, 75, 75, 5, 0, 0);
        maxHit("abyssal_walker", "melee", 13); // was 11
        // Ammonite Crab
        row("ammonite_crab", 25, 100, 1, 1, 1, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Long-tailed Wyvern
        row("long_tailed_wyvern", 152, 200, 125, 120, 90, 90, 90, 6, 70, 70, 70, 140, 120, 0, 0, 0, "dragon");
        // Ancient Wyvern
        row("ancient_wyvern", 210, 300, 150, 150, 150, 90, 90, 5, 50, 70, 70, 170, 120, 0, 0, 0, "dragon");
        // Monkey Guard (Bearded)
        row("monkey_guard", 167, 130, 130, 130, 200, 130, 1, 4, 0, 0, 0, 0, 0, 50, 0, 0);
        maxHit("monkey_guard", "melee", 14); // was 19
        // Monkey Archer (Ape Atoll gate)
        row("monkey_archer", 86, 50, 80, 80, 80, 1, 110, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("monkey_archer", "ranged", 20); // was 8
        // Monkey Zombie (Level 98)
        row("monkey_zombie", 98, 60, 150, 60, 60, 60, 1, 4, 0, 0, 0, 0, 0, 35, 0, 0, "undead");
        maxHit("monkey_zombie", "melee", 7); // was 11
        // Suqah (1)
        row("suqah", 111, 105, 95, 95, 95, 1, 1, 6, 50, 70, 70, 90, 30, 0, 0, 0);
        // Yak
        row("yak", 22, 50, 20, 10, 1, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("yak", "melee", 2); // was 3
        // Wallasalki (1)
        row("wallasalki", 98, 120, 1, 1, 80, 100, 1, 6, 100, 150, 175, 250, -10, 0, 0, 0);
        maxHit("wallasalki", "magic", 30); // was 12
        // Rock lobster (Active)
        row("rock_lobster", 127, 150, 100, 100, 100, 1, 1, 2, 100, 100, 50, 50, 150, 0, 0, 0);
        maxHit("rock_lobster", "melee", 11); // was 12
        // Jungle horror (Guard)
        row("jungle_horror", 70, 45, 70, 70, 55, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Jogre (Normal)
        row("jogre", 53, 60, 43, 43, 43, 1, 1, 6, 0, 0, 0, 0, 0, 22, 0, 0);
        maxHit("jogre", "melee", 7); // was 8
        // Ogress Warrior (1)
        row("ogress_warrior", 82, 82, 68, 70, 82, 60, 1, 4, 10, 12, 12, 14, 16, 0, 0, 0);
        maxHit("ogress_warrior", "melee", 8); // was 12
        // Ogress Shaman
        row("ogress_shaman", 82, 82, 68, 20, 82, 68, 1, 4, 12, 14, 14, 16, 8, 0, 0, 0);
        maxHit("ogress_shaman", "magic", 8); // was 12
        // Mudskipper (Level 31)
        row("mudskipper", 31, 20, 30, 30, 29, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Kree'arra
        row("kreearra", 580, 255, 300, 200, 260, 200, 380, 3, 180, 180, 180, 200, 200, 136, 0, 120, "flying");
        // Commander Zilyana
        row("commander_zilyana", 596, 255, 280, 196, 300, 300, 250, 2, 100, 100, 100, 100, 100, 195, 200, 0);
        // Nex
        row("nex", 1001, 3400, 315, 200, 260, 230, 350, 4, 40, 140, 60, 300, 190, 200, 100, 150);
        // Fumus
        row("fumus", 285, 500, 1, 1, 200, 200, 1, 5, 25, 100, 100, 150, 50, 0, 0, 0);
        maxHit("fumus", "smoke", 29); // was 18
        // Umbra
        row("umbra", 285, 500, 1, 1, 200, 200, 1, 5, 100, 100, 100, 150, 25, 0, 0, 0);
        // Cruor
        row("cruor", 285, 500, 1, 1, 200, 200, 1, 5, 100, 25, 100, 150, 50, 0, 0, 0);
        maxHit("cruor", "blood", 29); // was 18
        // Glacies
        row("glacies", 285, 500, 1, 1, 200, 200, 1, 5, 100, 100, 25, 150, 50, 0, 0, 0);
        maxHit("glacies", "ice", 29); // was 18
        // Callisto
        row("callisto", 470, 1000, 350, 300, 225, 140, 200, 4, 150, 130, 125, 0, 50, 0, 0, 100);
        // Artio
        row("artio", 320, 450, 250, 270, 150, 90, 120, 5, 125, 110, 110, 0, 40, 0, 0, 60);
        // Venenatis
        row("venenatis", 464, 850, 300, 200, 321, 300, 350, 4, 100, 100, 10, 300, 150, 0, 0, 0);
        // Spindel
        row("spindel", 302, 515, 200, 130, 225, 235, 286, 4, 70, 70, 10, 205, 103, 0, 0, 0);
        // Vet'ion (Normal)
        row("vetion", 454, 255, 430, 430, 395, 300, 1, 6, 201, 200, -10, 250, 270, 0, 0, 0, "undead");
        // Calvar'ion (Normal)
        row("calvarion", 264, 150, 250, 250, 225, 178, 1, 6, 130, 128, -10, 198, 211, 0, 0, 0, "undead");
        // Scorpia
        row("scorpia", 225, 200, 250, 150, 180, 1, 1, 4, 246, 284, 284, 44, 284, 60, 0, 0);
        // Chaos Elemental
        row("chaos_elemental", 305, 250, 270, 270, 270, 270, 270, 4, 70, 70, 70, 70, 70, 0, 0, 0);
        // Chaos Fanatic
        row("chaos_fanatic", 202, 225, 1, 1, 220, 200, 1, 2, 260, 260, 250, 280, 50, 0, 0, 75);
        // Crazy archaeologist
        row("crazy_archaeologist", 204, 225, 160, 90, 240, 1, 180, 3, 5, 5, 30, 250, 250, 250, 0, 75);
        // King Black Dragon
        row("king_black_dragon", 276, 240, 240, 240, 240, 240, 1, 4, 40, 90, 90, 80, 70, 0, 0, 0, "dragon", "fiery");
        // Giant Mole
        row("giant_mole", 230, 200, 200, 200, 200, 200, 1, 4, 60, 80, 100, 80, 60, 0, 0, 0);
        // Kalphite Queen (Crawling)
        row("kalphite_queen", 333, 255, 300, 300, 300, 150, 1, 4, 50, 50, 10, 100, 100, 0, 0, 0, "kalphite");
        // Kalphite Queen (Airborne)
        row("kalphite_queen_airborne", 333, 255, 300, 300, 300, 150, 1, 4, 100, 100, 100, 10, 10, 0, 0, 0, "kalphite");
        // Dagannoth Rex
        row("dagannoth_rex", 303, 255, 255, 255, 255, 1, 255, 4, 255, 255, 255, 10, 255, 0, 0, 0);
        // Dagannoth Prime
        row("dagannoth_prime", 303, 255, 255, 255, 255, 255, 1, 4, 255, 255, 255, 255, 10, 0, 0, 0);
        // Dagannoth Supreme
        row("dagannoth_supreme", 303, 255, 255, 255, 128, 255, 255, 4, 10, 10, 10, 255, 550, 0, 0, 0);
        // Sarachnis
        row("sarachnis", 318, 400, 200, 240, 150, 150, 300, 4, 60, 40, 10, 150, 300, 30, 0, 15);
        // Corporeal Beast
        row("corporeal_beast", 785, 2000, 320, 320, 310, 350, 150, 4, 25, 200, 100, 150, 230, 50, 0, 0);
        // Skotizo
        row("skotizo", 321, 450, 240, 250, 200, 280, 1, 5, 80, 80, 80, 80, 130, 160, 0, 0, "demon");
        // Hespori
        row("hespori", 284, 300, 1, 1, 120, 126, 150, 6, 60, 20, 60, 80, 80, 0, 150, 150);
        // Scurrius (Group)
        row("scurrius", 250, 1500, 300, 100, 100, 100, 100, 4, 20, 20, 20, 10, 20, 150, 150, 150, "rat");
        // The Mimic
        row("the_mimic", 186, 230, 185, 120, 120, 60, 1, 3, 160, 165, 150, 30, 145, 135, 180, 0);
        // Zulrah (Serpentine)
        row("zulrah", 725, 500, 1, 1, 300, 300, 300, 3, 0, 0, 0, -45, 50, 0, 50, 50);
        // Zulrah (Magma)
        row("zulrah_magma", 725, 500, 1, 1, 300, 300, 300, 3, 0, 0, 0, 0, 300, 0, 50, 50);
        // Zulrah (Tanzanite)
        row("zulrah_tanzanite", 725, 500, 1, 1, 300, 300, 300, 3, 0, 0, 0, 300, 0, 0, 50, 50);
        // Vorkath (Post-quest)
        row("vorkath", 732, 750, 560, 308, 214, 150, 308, 5, 26, 108, 108, 240, 26, 16, 150, 78, "dragon", "fiery", "undead");
        // Kraken (Kraken)
        row("kraken", 291, 255, 1, 1, 1, 1, 1, 4, 0, 0, 0, 130, 300, 0, 0, 0);
        // Thermonuclear smoke devil
        row("thermonuclear_smoke_devil", 301, 240, 230, 220, 360, 1, 310, 2, 11, 4, 9, 800, 900, 0, 0, 0);
        // Cerberus
        row("cerberus", 318, 600, 220, 220, 100, 220, 220, 6, 50, 100, 25, 65, 100, 50, 50, 50, "demon");
        // Abyssal Sire (Phase 2)
        row("abyssal_sire", 350, 425, 180, 136, 250, 200, 1, 7, 40, 60, 50, 20, 60, 65, 0, 0, "demon");
        // Alchemical Hydra (Serpentine)
        row("alchemical_hydra", 426, 1100, 100, 100, 100, 260, 260, 6, 75, 150, 150, 150, 45, 0, 45, 45, "dragon");
        // Alchemical Hydra (Electric)
        row("alchemical_hydra_blue", 426, 1100, 100, 100, 100, 260, 260, 6, 75, 150, 150, 150, 45, 0, 45, 45, "dragon");
        // Alchemical Hydra (Fire)
        row("alchemical_hydra_red", 426, 1100, 100, 100, 100, 260, 260, 6, 75, 150, 150, 150, 45, 0, 45, 45, "dragon");
        // Alchemical Hydra (Extinguished)
        row("alchemical_hydra_enraged", 426, 1100, 100, 100, 100, 260, 260, 4, 75, 150, 150, 150, 45, 0, 45, 45, "dragon");
        // Dusk (Second form)
        row("dusk", 328, 450, 300, 250, 150, 250, 250, 6, 0, 0, 0, 0, 0, 0, 0, 0, "golem");
        maxHit("dusk", "claw", 26); // was 28
        // Dawn
        row("dawn", 228, 450, 140, 140, 100, 100, 140, 6, 0, 0, 0, 80, 0, 0, 0, 0, "flying", "golem");
        // Araxxor
        row("araxxor", 890, 1020, 320, 320, 135, 190, 210, 6, 160, 75, 15, 237, 218, 218, 260, 283);
        maxHit("araxxor", "acid_spit", 34); // was 30
        maxHit("araxxor", "web_magic", 21); // was 30
        // Crystalline Hunllef
        row("crystalline_hunllef", 674, 600, 240, 240, 240, 240, 240, 5, 20, 20, 20, 20, 20, 76, 76, 76);
        // Corrupted Hunllef
        row("corrupted_hunllef", 894, 1000, 240, 240, 240, 240, 240, 5, 20, 20, 20, 20, 20, 90, 90, 90);
        // The Nightmare
        row("the_nightmare", 814, 2400, 150, 150, 150, 150, 150, 6, 120, 180, 40, 600, 600, 140, 140, 140);
        // Phosani's Nightmare
        row("phosanis_nightmare", 1024, 3200, 150, 150, 150, 150, 150, 6, 120, 180, 40, 600, 600, 220, 220, 220);
        // Phantom Muspah (Ranged)
        row("phantom_muspah", 741, 850, 280, 280, 200, 150, 280, 6, 185, 134, 120, 437, 56, 393, 280, 64, "spectral");
        maxHit("phantom_muspah", "ice_shard", 61); // was 33
        // Sol Heredit
        row("sol_heredit", 1563, 1500, 350, 400, 200, 300, 350, 5, 65, 5, 30, 750, 825, 250, 0, 150);
        // Duke Sucellus (Post-quest, Awake)
        row("duke_sucellus", 758, 485, 300, 345, 275, 310, 1, 5, 255, 45, 190, 440, 320, 200, 150, 0, "demon");
        maxHit("duke_sucellus", "fist_slam", 56); // was 46
        // The Leviathan (Post-quest)
        row("the_leviathan", 798, 900, 300, 360, 250, 160, 160, 4, 260, 190, 230, 280, 50, 200, 160, 300);
        // The Whisperer (Post-quest)
        row("the_whisperer", 791, 900, 280, 280, 250, 180, 180, 10, 180, 300, 220, 10, 300, 140, 190, 160);
        // Vardorvis (Post-quest)
        row("vardorvis", 784, 700, 280, 1, 1, 215, 1, 5, 215, 65, 85, 580, 580, 190, 0, 0);
        maxHit("vardorvis", "axe_swing", 43); // was 35
        // The Hueycoatl (Normal)
        row("the_hueycoatl", 642, 2500, 150, 50, 125, 50, 50, 5, 100, 100, 0, 200, 350, 100, 200, 250, "dragon");
        // Amoxliatl (Post-quest: the repeatable boss; the 226/400 HP version is the quest fight)
        row("amoxliatl", 263, 520, 1, 1, 80, 170, 1, 8, 100, 100, 40, 100, 200, 0, 160, 0, "spectral");
        // Branda the Fire Queen
        row("branda_the_fire_queen", 350, 600, 300, 250, 100, 100, 150, 7, 12, 12, 0, 700, 700, 150, 100, 100, "fiery");
        maxHit("branda_the_fire_queen", "molten_fist", 26); // was 35
        // Eldric the Ice King
        row("eldric_the_ice_king", 350, 600, 300, 250, 100, 100, 150, 7, 12, 12, 0, 700, 700, 150, 100, 100);
        maxHit("eldric_the_ice_king", "frozen_fist", 26); // was 35
        // Melzar the Mad
        row("melzar_the_mad", 43, 44, 37, 37, 34, 40, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Elvarg
        row("elvarg", 83, 80, 70, 70, 70, 70, 1, 4, 20, 40, 40, 30, 20, 0, 0, 0);
        // Jonny the Beard
        row("jonny_the_beard", 2, 8, 1, 1, 1, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Ithoi the Navigator (Attackable)
        row("ithoi_the_navigator", 35, 55, 1, 1, 30, 30, 1, 5, 0, 0, 0, 0, 0, 0, 0, 0);
        // Tree spirit (Level 79)
        row("tree_spirit", 79, 85, 65, 65, 65, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("tree_spirit", "branch_slam", 7); // was 13
        // Sir Mordred
        row("sir_mordred", 39, 38, 33, 33, 33, 1, 1, 4, 99, 82, 45, 0, 0, 20, 0, 0);
        maxHit("sir_mordred", "sword", 5); // was 7
        // Black Knight Titan
        row("black_knight_titan", 120, 142, 91, 100, 91, 1, 1, 7, 18, 27, 18, 1000, 1000, 27, 0, 0);
        maxHit("black_knight_titan", "titan_sword", 15); // was 18
        // Chronozon
        row("chronozon", 170, 60, 173, 172, 173, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "demon");
    }

    private static void rows4() {
        // Ice Queen
        row("ice_queen", 111, 104, 95, 94, 95, 1, 1, 4, 30, 40, 20, 10, 30, 0, 0, 0);
        maxHit("ice_queen", "staff", 10); // was 14
        // Entrana firebird
        row("entranan_firebird", 2, 5, 1, 1, 1, 1, 1, 4, -26, -26, -26, -26, -26, -31, 0, 0);
        maxHit("entranan_firebird", "peck", 1); // was 2
        // Bouncer
        row("bouncer", 137, 116, 120, 120, 120, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "demon");
        maxHit("bouncer", "bite", 13); // was 17
        // General Khazard
        row("general_khazard", 142, 170, 75, 78, 80, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("general_khazard", "slash", 9); // was 26
        // Witch's experiment (Wolf)
        row("witchs_experiment", 53, 51, 48, 40, 49, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Khazard warlord
        row("khazard_warlord", 112, 170, 75, 78, 80, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("khazard_warlord", "slash", 10); // was 16
        // Black demon (Level 172)
        row("glough_black_demon", 172, 157, 145, 148, 152, 1, 1, 4, 0, 0, 0, -10, 0, 0, 0, 0, "demon");
        // Jungle Demon
        row("jungle_demon", 195, 170, 170, 170, 170, 170, 1, 6, 0, 50, 0, 50, 0, 50, 0, 0, "demon");
        // Nazastarool (Zombie)
        row("nazastarool", 91, 70, 85, 80, 80, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "undead");
        // Nezikchened (Normal)
        row("nezikchened", 187, 150, 165, 168, 167, 160, 160, 5, 0, 0, 0, 0, 0, 0, 0, 0, "demon");
        maxHit("nezikchened", "claws", 18); // was 20
        // The Draugen
        row("draugen", 69, 60, 60, 60, 60, 1, 1, 4, 100, 100, 100, 500, 500, 0, 0, 0, "undead");
        maxHit("draugen", "crush", 7); // was 10
        // Me (Type A)
        row("me", 79, 45, 75, 78, 74, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("me", "mirror_strike", 9); // was 12
        // The Kendal
        row("the_kendal", 70, 50, 65, 65, 60, 1, 1, 4, 10, 10, 10, -10, 20, 5, 0, 0);
        // Dagannoth mother
        row("dagannoth_mother", 100, 120, 78, 78, 81, 1, 50, 4, 150, 150, 150, 50, 50, 0, 0, 0);
        maxHit("dagannoth_mother", "bite", 9); // was 15
        // Fire Warrior of Lesarkus
        row("fire_warrior_of_lesarkus", 84, 59, 78, 78, 78, 62, 1, 4, 34, 37, 35, -7, 35, -12, 0, 0);
        maxHit("fire_warrior_of_lesarkus", "burning_blade", 8); // was 9
        // Mercenary Captain
        row("mercenary_captain", 47, 80, 32, 29, 32, 1, 1, 4, 17, 15, 19, -3, 19, 9, 0, 0);
        maxHit("mercenary_captain", "scimitar", 5); // was 8
        // Kamil
        row("kamil", 154, 130, 190, 80, 135, 1, 1, 4, 35, 60, 35, 0, 0, 60, 0, 0);
        maxHit("kamil", "ice_sword", 23); // was 24
        // Dessous
        row("dessous", 139, 200, 99, 99, 99, 1, 1, 3, 10, 150, 150, 0, 0, 50, 0, 0, "vampyre");
        maxHit("dessous", "vampyric_slash", 19); // was 26
        // Fareed
        row("fareed", 167, 130, 190, 120, 135, 1, 1, 4, 100, 100, 100, 0, 0, 0, 0, 0);
        // Damis (First form)
        row("damis", 103, 90, 90, 90, 90, 1, 1, 4, 60, 60, 60, 60, 60, 0, 0, 0);
        // Temple Guardian
        row("temple_guardian", 30, 45, 20, 20, 20, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("temple_guardian", "bite", 3); // was 5
        // Treus Dayth (In combat)
        row("treus_dayth", 95, 100, 70, 70, 100, 1, 1, 4, 5, 5, 5, -5, 5, 0, 0, 0, "spectral", "undead");
        // Skeleton Hellhound
        row("quest_skeleton_hellhound", 97, 55, 70, 110, 100, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "demon");
        // Ranis Drakan (A Taste of Hope)
        row("ranis_drakan", 233, 400, 120, 120, 120, 120, 1, 5, 60, 60, 60, 60, 60, 0, 0, 0, "vampyre");
        // Vanstrom Klause (Vampyre)
        row("vanstrom_klause", 169, 155, 150, 136, 150, 150, 150, 8, 0, 0, 0, 0, 0, 0, 0, 0, "vampyre");
        maxHit("vanstrom_klause", "vampyric_claw", 16); // was 25
        // Dad
        row("dad", 101, 120, 60, 120, 50, 1, 1, 8, 25, 25, 40, 200, 200, 40, 0, 0);
        maxHit("dad", "club", 27); // was 18
        // Troll general (Hammer (brown))
        row("troll_general", 113, 140, 70, 140, 40, 1, 1, 4, 35, 60, 35, 200, 120, 60, 0, 0);
        maxHit("troll_general", "great_club", 38); // was 20
        // Kalrag
        row("kalrag", 89, 78, 78, 78, 78, 1, 1, 4, 20, 17, 12, 13, 13, 0, 0, 0);
        maxHit("kalrag", "bite", 9); // was 13
        // Fragment of Seren
        row("fragment_of_seren", 494, 1000, 242, 288, 102, 102, 235, 7, 320, 220, 320, 10, 480, 236, 0, 224);
        // Kruk
        row("kruk", 207, 210, 130, 130, 150, 130, 170, 4, 100, 100, 100, 250, 100, 160, 0, 150);
        maxHit("kruk", "slam", 33); // was 30
        // Vorkath (Dragon Slayer II)
        row("ds2_vorkath", 392, 460, 560, 268, 164, 148, 268, 5, 66, 126, 126, 204, 80, 14, 148, 96, "dragon", "fiery", "undead");
        // Galvek (Air)
        row("galvek", 608, 1200, 632, 268, 188, 160, 246, 6, 80, 140, 140, 280, 86, 34, 160, 180, "dragon", "fiery");
        // Balance Elemental (Magic)
        row("balance_elemental", 524, 750, 275, 286, 275, 275, 275, 6, 5, 0, -40, -40, 0, 0, 0, 0);
        // Slug Prince
        row("slug_prince", 62, 70, 50, 50, 50, 1, 1, 4, 30, 30, 30, 99, 99, 0, 0, 0);
        maxHit("slug_prince", "slime", 6); // was 9
        // Ogre shaman (1)
        row("ogre_shaman", 113, 1, 1, 1, 1, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Chompy bird
        row("chompy_bird", 6, 10, 5, 5, 3, 1, 1, 5, 0, 0, 0, 0, 0, 0, 0, 0);
        // Slash Bash
        row("slash_bash", 111, 100, 100, 120, 60, 1, 100, 6, 30, 30, 30, 0, 0, 22, 0, 0, "undead");
        // Evil Creature (Standard)
        row("evil_creature", 1, 1, 1, 1, 1, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Moss Guardian
        row("moss_guardian", 84, 120, 60, 60, 60, 1, 1, 6, 0, 0, 0, 0, 0, 66, 0, 0);
        maxHit("moss_guardian", "club", 14); // was 13
        // Shadow (Temple of Light)
        row("shadow_of_light", 73, 15, 90, 120, 5, 90, 1, 4, 30, 30, 30, 30, 30, 0, 0, 0);
        maxHit("shadow_of_light", "shadow_touch", 13); // was 11
        // The Inadequacy
        row("the_inadequacy", 343, 180, 564, 170, 240, 1, 170, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("the_inadequacy", "crush", 18); // was 24
        // Agrith Naar
        row("agrith_naar", 100, 95, 83, 90, 82, 100, 100, 4, 0, 0, 0, 0, 0, 0, 0, 0, "demon");
        maxHit("agrith_naar", "claws", 10); // was 15
        // King Roald (In combat)
        row("possessed_king_roald", 47, 75, 35, 30, 30, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("possessed_king_roald", "punch", 3); // was 8
        // Arrav
        row("arrav", 339, 600, 220, 220, 150, 150, 1, 5, 80, 80, 40, 160, 80, 100, 0, 0, "undead");
        maxHit("arrav", "zombie_sword", 30); // was 22
        // Gadderanks
        row("gadderanks", 35, 20, 20, 20, 70, 1, 1, 4, 10, 10, 10, 10, 10, 0, 0, 0);
        maxHit("gadderanks", "sword", 3); // was 5
        // Tanglefoot
        row("tanglefoot", 111, 102, 97, 97, 91, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("tanglefoot", "vine_whip", 11); // was 15
        // Gorak (Level 145)
        row("gorak", 145, 112, 130, 130, 131, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("gorak", "claw", 14); // was 16
        // Warped Terrorbird (Level 138)
        row("warped_terrorbird", 138, 200, 80, 150, 60, 160, 70, 4, 50, 50, 0, 0, 62, 0, 0, 160);
        // Ulfric
        row("ulfric", 100, 60, 100, 100, 82, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "undead");
        maxHit("ulfric", "skeletal_axe", 11); // was 14
        // Ice Troll King
        row("ice_troll_king", 122, 150, 100, 100, 80, 1, 1, 4, 45, 45, 45, 2000, 2000, 60, 0, 0);
        maxHit("ice_troll_king", "club", 21); // was 20
        // Giant Sea Snake
        row("giant_sea_snake", 149, 100, 170, 90, 160, 1, 130, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("giant_sea_snake", "bite", 14); // was 16
        // Glod
        row("glod", 138, 160, 115, 120, 110, 1, 1, 4, 105, 110, 130, 125, 80, 0, 0, 0);
        // Giant Scarab
        row("giant_scarab", 191, 130, 169, 190, 169, 1, 190, 4, 70, 99, 99, 159, 103, 0, 0, 0, "kalphite");
        // Champion of Scabaras
        row("champion_of_scabaras", 379, 600, 160, 160, 160, 160, 1, 4, 120, 160, 160, 340, 5, 28, 28, 0, "kalphite");
        // Sea Troll Queen
        row("sea_troll_queen", 170, 200, 100, 70, 100, 150, 1, 4, 20, 40, 40, 40, 0, 100, 0, 0);
        maxHit("sea_troll_queen", "crush", 16); // was 25
        // Corrupt Lizardman
        row("corrupt_lizardman", 46, 50, 38, 38, 38, 1, 38, 4, -10, 25, 0, 0, 0, 22, 0, 22);
        maxHit("corrupt_lizardman", "spear", 6); // was 18
        maxHit("corrupt_lizardman", "acid", 6); // was 14
        // Judge of Yama
        row("judge_of_yama", 168, 400, 200, 200, 150, 250, 200, 5, 0, 0, 0, 0, 0, 0, 0, 0, "demon");
        // Agrith-Na-Na
        row("agrith_na_na", 146, 200, 83, 150, 82, 100, 100, 4, 100, 100, 60, 100, 100, 100, 0, 0);
        maxHit("agrith_na_na", "claws", 16); // was 20
        maxHit("agrith_na_na", "fire_blast", 16); // was 18
        // Flambeed
        row("flambeed", 149, 210, 120, 120, 75, 1, 1, 4, 50, 50, 5, 5, 50, 100, 0, 0);
        maxHit("flambeed", "flaming_fists", 25); // was 20
        // Karamel
        row("karamel", 136, 250, 1, 1, 100, 1, 100, 3, 150, 150, 150, 150, 150, 0, 0, 134);
        // Dessourt
        row("dessourt", 121, 130, 99, 99, 99, 1, 1, 3, 10, 150, 150, 0, 0, 50, 0, 0);
        maxHit("dessourt", "ghostly_blast", 19); // was 23
        // Gelatinnoth Mother
        row("gelatinnoth_mother", 130, 240, 78, 78, 81, 1, 50, 4, 150, 150, 150, 50, 50, 0, 0, 0);
        maxHit("gelatinnoth_mother", "slam", 9); // was 17
        // Culinaromancer
        row("culinaromancer", 75, 150, 10, 100, 10, 1, 1, 4, -10, -10, -10, -10, -10, 0, 0, 0);
        // Sigmund (Another Slice of H.A.M.)
        row("quest_sigmund", 64, 70, 55, 55, 44, 1, 1, 4, 0, 0, 0, 0, 0, 0, 8, 0);
        maxHit("quest_sigmund", "sword", 6); // was 11
        maxHit("quest_sigmund", "crossbow", 6); // was 10
        maxHit("quest_sigmund", "rune_spell", 6); // was 10
        // Giant Roc
        row("quest_giant_roc", 172, 250, 130, 130, 100, 1, 130, 6, 0, 0, 0, 150, 0, 0, 0, 0);
        maxHit("quest_giant_roc", "peck", 14); // was 20
        // Mother
        row("quest_mother", 198, 235, 150, 150, 170, 60, 105, 4, 0, 0, 0, 200, 0, 60, 0, 0);
        maxHit("quest_mother", "crush", 16); // was 25
        // The Shaikahan
        row("quest_shaikahan", 83, 100, 80, 80, 25, 1, 1, 4, 0, 0, 0, 10000, 10000, 0, 0, 0);
        maxHit("quest_shaikahan", "claws", 9); // was 12
        maxHit("quest_shaikahan", "pounce", 9); // was 14
        // Arrg
        row("quest_arrg", 113, 140, 70, 140, 40, 1, 70, 4, 35, 60, 35, 200, 200, 60, 0, 0);
        maxHit("quest_arrg", "crush", 38); // was 20
        // Slagilith
        row("quest_slagilith", 92, 60, 60, 120, 75, 1, 1, 4, 50, 50, 5, 5, 50, 10, 0, 0);
        maxHit("quest_slagilith", "slam", 13); // was 15
        // Fear reaper (Level 42)
        row("quest_fear_reaper", 42, 25, 39, 41, 40, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        maxHit("quest_fear_reaper", "scythe", 5); // was 8
        maxHit("quest_fear_reaper", "dread", 5); // was 6
        // Barrelchest
        row("quest_barrelchest", 190, 134, 170, 145, 140, 90, 1, 5, 0, 0, 0, 0, 0, 80, 0, 0);
        maxHit("quest_barrelchest", "anchor", 35); // was 32
        // Basilisk Youngling
        row("quest_basilisk_boss", 57, 60, 25, 40, 70, 1, 1, 4, 20, 20, 0, 20, 0, 0, 0, 0);
        maxHit("quest_basilisk_boss", "bite", 5); // was 24
        maxHit("quest_basilisk_boss", "gaze", 5); // was 18
        // Sourhog
        row("quest_sourhog", 37, 40, 35, 30, 25, 25, 35, 5, 0, 30, 10, 30, 0, 0, 0, 0);
        // Blood Moon
        row("quest_blood_moon", 329, 500, 258, 100, 60, 100, 1, 6, 100, 0, 100, 500, 500, 100, 0, 0);
        // Blue Moon
        row("quest_blue_moon", 329, 500, 258, 100, 60, 100, 1, 6, 100, 100, 0, 500, 500, 100, 0, 0);
        // Eclipse Moon (Regular)
        row("quest_eclipse_moon", 329, 500, 258, 100, 60, 100, 1, 6, 0, 100, 100, 500, 500, 100, 0, 0);
        // Tarn (Ghost)
        row("quest_tarn_razorlor", 69, 80, 40, 80, 40, 1, 1, 4, 50, 50, 50, 50, 50, 0, 0, 0, "undead");
        maxHit("quest_tarn_razorlor", "claw", 9); // was 12
        maxHit("quest_tarn_razorlor", "drain", 9); // was 10
        // Kolodion (Demon)
        row("quest_kolodion", 112, 107, 85, 98, 105, 400, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Justiciar Zachariah
        row("quest_justiciar_zachariah", 348, 320, 500, 250, 100, 180, 1, 6, 200, 200, 200, -60, 200, 200, 80, 0);
        // Derwen
        row("quest_derwen", 235, 320, 250, 150, 100, 80, 1, 6, 200, 200, 200, -60, 200, 0, 180, 0);
        maxHit("quest_derwen", "smite", 43); // was 22
        // Porazdir
        row("quest_porazdir", 235, 320, 250, 150, 100, 180, 1, 6, 200, 200, 200, -60, 200, 0, 80, 0, "demon");
        maxHit("quest_porazdir", "smite", 43); // was 22
        // TzKal-Zuk (Normal)
        row("tzkal_zuk", 1400, 1200, 350, 600, 260, 150, 400, 10, 0, 0, 0, 350, 100, 0, 550, 550);
    }

    private static void rows5() {
        // Penance Queen
        row("penance_queen", 209, 250, 260, 92, 132, 1, 116, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Blue Moon
        row("blue_moon", 329, 500, 258, 100, 60, 100, 1, 6, 100, 100, 0, 500, 500, 100, 0, 0);
        // Eclipse Moon (Regular)
        row("eclipse_moon", 329, 500, 258, 100, 60, 100, 1, 6, 0, 100, 100, 500, 500, 100, 0, 0);
        // Blood Moon
        row("blood_moon", 329, 500, 258, 100, 60, 100, 1, 6, 100, 0, 100, 500, 500, 100, 0, 0);
    }

    /** Dungeon, minigame and lair creatures added after the first sweep (same source, matched by hand). */
    private static void rows6() {
        // Dagannoth (Waterbirth Island, Level 88)
        row("dagannoth_lvl88", 88, 85, 10, 10, 75, 1, 100, 3, 10, 10, 90, 200, 150, 0, 0, 0);
        // Dagannoth (Waterbirth Island, Level 90)
        row("dagannoth_lvl90", 90, 95, 76, 78, 65, 1, 1, 4, 50, 50, 50, 50, 50, 50, 0, 0);
        // Dagannoth (Level 92)
        row("dagannoth_lvl92", 92, 120, 68, 70, 71, 1, 1, 4, 0, 0, 0, 50, 50, 0, 0, 0);
        // Dagannoth spawn
        row("dagannoth_spawn", 42, 35, 50, 35, 25, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Dagannoth fledgeling
        row("dagannoth_fledgeling", 70, 100, 50, 50, 50, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Giant Rock Crab
        row("giant_rock_crab", 137, 180, 50, 80, 200, 1, 1, 4, 225, 200, 175, 10, 250, 0, 0, 0);
        // Monkey Zombie (Level 82)
        row("monkey_zombie_lvl82", 82, 60, 100, 60, 60, 60, 1, 4, 0, 0, 0, 0, 0, 15, 0, 0, "undead");
        // Monkey Zombie (Level 129)
        row("monkey_zombie_lvl129", 129, 90, 150, 110, 90, 90, 1, 4, 0, 0, 0, 0, 0, 35, 0, 0, "undead");
        // Skeleton (Ape Atoll)
        row("skeleton_lvl142", 142, 77, 180, 90, 110, 110, 1, 4, 0, 0, 0, 0, 0, 50, 0, 0, "undead");
        // Maniacal monkey
        row("maniacal_monkey", 140, 65, 200, 175, 10, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Maniacal Monkey Archer
        row("maniacal_monkey_archer", 132, 60, 120, 80, 80, 1, 200, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Guard (Prifddinas)
        row("prifddinas_guard", 108, 105, 95, 95, 80, 1, 1, 4, 50, 70, 70, 60, 50, 0, 0, 0);
        // Urium Shade (Shade)
        row("urium_shade", 140, 130, 138, 116, 100, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "shade", "spectral", "undead");
        // Demonic gorilla
        row("demonic_gorilla", 275, 380, 205, 195, 200, 195, 195, 5, 0, 0, 0, 20, 0, 43, 40, 43, "demon");
        // Tortured gorilla (Level 141)
        row("tortured_gorilla", 141, 210, 105, 95, 95, 95, 95, 5, 0, 0, 0, 25, 0, 22, 20, 22);
        // Temple Trekking: Vampyre Juvinate (Level 59 / 90 / 119)
        row("trek_vampyre_juvinate_1", 59, 50, 95, 16, 45, 50, 50, 4, 0, 0, 0, 0, 0, 0, 0, 0, "vampyre");
        row("trek_vampyre_juvinate_2", 90, 100, 113, 46, 55, 50, 50, 4, 0, 0, 0, 0, 0, 0, 0, 0, "vampyre");
        row("trek_vampyre_juvinate_3", 119, 150, 127, 76, 65, 50, 50, 4, 0, 0, 0, 0, 0, 0, 0, 0, "vampyre");
        // Ghast (Level 79 / 109 / 139)
        row("trek_ghast_1", 79, 115, 80, 30, 60, 30, 30, 8, 0, 0, 0, 0, 0, 0, 0, 0, "spectral");
        row("trek_ghast_2", 109, 135, 110, 70, 70, 30, 30, 8, 0, 0, 0, 0, 0, 0, 0, 0, "spectral");
        row("trek_ghast_3", 139, 160, 135, 110, 80, 30, 30, 8, 0, 0, 0, 0, 0, 0, 0, 0, "spectral");
        // Riyl shadow, Asyn shadow (Temple Trekking), Shade (Level 140)
        row("trek_shade_1", 80, 76, 87, 56, 60, 50, 50, 4, 0, 0, 0, 0, 0, 0, 0, 0, "spectral", "undead");
        row("trek_shade_2", 110, 94, 110, 96, 80, 50, 50, 4, 0, 0, 0, 0, 0, 0, 0, 0, "spectral", "undead");
        row("trek_shade_3", 140, 115, 130, 136, 100, 50, 50, 4, 0, 0, 0, 0, 0, 0, 0, 0, "spectral", "undead");
        // Swamp snake (Level 80 / 109 / 139)
        row("trek_swamp_snake_1", 80, 120, 50, 50, 70, 30, 30, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        row("trek_swamp_snake_2", 109, 125, 70, 110, 80, 30, 30, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        row("trek_swamp_snake_3", 139, 130, 90, 170, 90, 30, 30, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Giant snail (Level 80 / 109 / 139)
        row("trek_giant_snail_1", 80, 125, 30, 30, 60, 30, 70, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        row("trek_giant_snail_2", 109, 150, 30, 30, 75, 30, 110, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        row("trek_giant_snail_3", 139, 160, 30, 30, 86, 30, 160, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        // Nail beast (Level 69 / 98 / 141); the wiki max hit is per hit of a three-hit attack
        row("trek_nail_beast_1", 69, 55, 150, 6, 2, 1, 1, 8, 0, 0, 0, 0, 0, 0, 0, 0);
        row("trek_nail_beast_2", 98, 65, 215, 16, 30, 1, 1, 8, 0, 0, 0, 0, 0, 0, 0, 0);
        row("trek_nail_beast_3", 141, 75, 320, 26, 40, 1, 1, 8, 0, 0, 0, 0, 0, 0, 0, 0);
        // Undead Lumberjack (Level 30 / 50 / 70)
        row("trek_undead_lumberjack_1", 30, 10, 73, 6, 10, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "undead");
        row("trek_undead_lumberjack_2", 50, 14, 117, 16, 14, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "undead");
        row("trek_undead_lumberjack_3", 70, 18, 163, 26, 18, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "undead");
        // Zombie (Level 13)
        row("trek_zombie", 13, 22, 8, 9, 10, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0, "undead");
    }
}
