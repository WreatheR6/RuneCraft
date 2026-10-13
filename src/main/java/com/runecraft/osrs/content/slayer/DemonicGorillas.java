package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.CollectionLogData;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentDef.Slot;
import com.runecraft.osrs.EquipmentDef.WeaponClass;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Region;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Drop;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Demonic and tortured gorillas (Monkey Madness II) in the Crash Site Cavern north-west of the Grand Tree. Stats are
 * the wiki DPS calculator's ({@code monsters.json}); drops are the wiki's 500-slot (demonic) and 1,000-slot (tortured)
 * tables, written as independent 1/x rolls.
 *
 * <p>Mechanics (wiki "Demonic gorilla/Strategies", July 2026), run by {@code com.runecraft.content.slayer.GorillaEntity}:
 * <ul>
 *   <li>They use all three combat styles and switch to another random style after three missed attacks (tortured
 *   gorillas: four). A prayed-against attack counts as a miss; the boulder never counts.</li>
 *   <li>Demonic gorillas pray against one style. Once they have been hit at least four separate times for a total of 70
 *   damage since their last switch, they pray against the style that last damaged them.</li>
 *   <li>While on Ranged or Magic they sometimes drop a boulder on the player's tile: 1/3 of the player's current
 *   hitpoints unless they step off it.</li>
 * </ul>
 * Uniques: zenyte shard (makes uncut zenyte) and the ballista components (light and heavy ballista).
 */
public final class DemonicGorillas {
    public static final String DEMONIC = "demonic_gorilla";
    public static final String TORTURED = "tortured_gorilla";

    /** Damage a demonic gorilla must take (since its last prayer switch) before it switches its protection prayer. */
    public static final int PRAYER_SWITCH_DAMAGE = 70;
    /** ... over at least this many separate damaging hits (22 July 2026 update). */
    public static final int PRAYER_SWITCH_HITS = 4;
    /** Missed attacks before a demonic gorilla changes attack style. */
    public static final int DEMONIC_MISSES = 3;
    /** Tortured gorillas wait for four misses. */
    public static final int TORTURED_MISSES = 4;
    /** Chance of the boulder toss instead of a Ranged/Magic attack. */
    public static final int BOULDER_CHANCE = 5; // approx: the wiki only says "sometimes"
    /** Game ticks between the chest-beat and the boulder landing. */
    public static final int BOULDER_DELAY_TICKS = 3; // approx
    /** Game ticks out of combat before a gorilla resets its attack style (Poll 79 change). */
    public static final int STYLE_RESET_TICKS = 50; // approx

    /** The three combat styles a gorilla attacks with and prays against. */
    public enum Style {
        MELEE, RANGED, MAGIC;

        public static Style of(AttackType type) {
            return type == AttackType.MAGIC ? MAGIC : type == AttackType.RANGED ? RANGED : MELEE;
        }

        public String prayerName() {
            return switch (this) {
                case MELEE -> "Protect from Melee";
                case RANGED -> "Protect from Missiles";
                case MAGIC -> "Protect from Magic";
            };
        }
    }

    /** Boulder damage: a third of the player's current hitpoints (so it can never kill on its own). */
    public static int boulderDamage(int currentHitpoints) {
        return Math.max(0, currentHitpoints / 3);
    }

    /** One gorilla's combat state: its protection prayer, its attack style and the counters that switch them. */
    public static final class Tracker {
        private final boolean prays;
        private final int missesToSwitch;
        private Style prayer;
        private Style attackStyle;
        private int misses;
        private int damage;
        private int hits;

        public Tracker(boolean demonic, Random random) {
            this.prays = demonic;
            this.missesToSwitch = demonic ? DEMONIC_MISSES : TORTURED_MISSES;
            this.prayer = demonic ? Style.values()[random.nextInt(3)] : null;
            this.attackStyle = Style.values()[random.nextInt(3)];
        }

        /** The style it protects itself from, or null (tortured gorillas don't pray). */
        public Style prayer() {
            return prayer;
        }

        public Style attackStyle() {
            return attackStyle;
        }

        public int misses() {
            return misses;
        }

        /** Whether an attack of this style is fully blocked by its protection prayer. */
        public boolean blocks(Style style) {
            return prays && prayer == style;
        }

        /**
         * Records damage taken from a style; returns true when the gorilla switches its prayer to that style.
         * Blocked hits deal no damage and don't count.
         */
        public boolean damaged(Style style, int dealt) {
            if (!prays || dealt <= 0 || prayer == style) {
                return false;
            }
            damage += dealt;
            hits++;
            if (damage >= PRAYER_SWITCH_DAMAGE && hits >= PRAYER_SWITCH_HITS) {
                damage = 0;
                hits = 0;
                prayer = style;
                return true;
            }
            return false;
        }

        /**
         * Records the outcome of one of its own attacks (boulders excluded); returns true when it switches its attack
         * style, which then is a random one of the other two.
         */
        public boolean attacked(boolean missed, Random random) {
            if (!missed) {
                return false;
            }
            if (++misses < missesToSwitch) {
                return false;
            }
            misses = 0;
            Style[] others = new Style[2];
            int n = 0;
            for (Style s : Style.values()) {
                if (s != attackStyle) {
                    others[n++] = s;
                }
            }
            attackStyle = others[random.nextInt(2)];
            return true;
        }

        /** Out of combat for a while: it picks a fresh random style (Poll 79). */
        public void resetStyle(Random random) {
            attackStyle = Style.values()[random.nextInt(3)];
            misses = 0;
        }
    }

    private DemonicGorillas() {
    }

    private static AttackDef melee(int max) {
        return AttackDef.of("melee", AttackType.CRUSH, max, 2.5, 1);
    }

    private static AttackDef ranged(int max) {
        return AttackDef.of("ranged", AttackType.RANGED, max, 10, 1).particle("minecraft:crit");
    }

    private static AttackDef magic(int max) {
        return AttackDef.of("magic", AttackType.MAGIC, max, 10, 1).particle("minecraft:happy_villager");
    }

    static List<MonsterDef> monsters() {
        return List.of(
                // Demonic gorilla: max hit 31 with every style (40 with the boulder, which is scripted).
                MonsterDef.of(DEMONIC, "Demonic gorilla").level(275).stats(380, 205, 195, 200, 195, 195).speed(5)
                        .attackBonus(43, 40, 43).defenceBonus(0, 0, 0, 20, 0).attributes("demon")
                        .attack(melee(31)).attack(ranged(31)).attack(magic(31))
                        .drops(Drop.always("malicious_ashes"),
                                // uniques: 5/500 onto the unique table
                                Drop.of("zenyte_shard", 1, 1, 300), Drop.of("ballista_limbs", 1, 1, 500),
                                Drop.of("ballista_spring", 1, 1, 500), Drop.of("light_frame", 1, 1, 750),
                                Drop.of("heavy_frame", 1, 1, 1500), Drop.of("monkey_tail", 1, 1, 1500),
                                Drop.of("rune_platelegs", 1, 1, 14), Drop.of("rune_plateskirt", 1, 1, 14),   // 35/500
                                Drop.of("rune_chainbody", 1, 1, 25), Drop.of("dragon_scimitar", 1, 1, 50),   // 20/500, 10/500
                                Drop.of("law_rune", 50, 75, 14), Drop.of("death_rune", 50, 75, 14),           // 35/500
                                Drop.of("rune_bolts", 100, 150, 20),                                           // 25/500
                                Drop.of("grimy_kwuarm", 7, 13, 89), Drop.of("grimy_cadantine", 7, 13, 111),   // 18/500 herb table
                                Drop.of("grimy_lantadyme", 7, 13, 148), Drop.of("grimy_dwarf_weed", 7, 13, 111),
                                Drop.treeHerbSeeds(20),                                                        // 25/500 (x2)
                                Drop.of("prayer_potion", 2, 2, 12), Drop.of("shark", 2, 3, 14),               // 40/500, 35/500
                                Drop.of("coins", 5000, 10000, 20), Drop.of("saradomin_brew", 1, 1, 20),
                                Drop.of("adamantite_bar", 6, 6, 25), Drop.of("javelin_shaft", 750, 1250, 20),
                                Drop.of("rune_javelin_heads", 45, 55, 20), Drop.of("dragon_javelin_heads", 27, 33, 20),
                                Drop.of("diamond", 4, 6, 29), Drop.of("runite_bar", 3, 3, 33),                // 17/500, 15/500
                                Drop.rare(100),                                                                // 5/500
                                Kit.clue("hard", 100), Kit.clue("elite", 500))
                        // Counts as black demons (and monkeys) for Slayer.
                        .slayer(1, "black_demons").passive().size(1.35f, 1.55f, 1.3f).moveSpeed(0.24)
                        .skin(0x2A2A2E, 0x3A3A40, 0x2A2A2E, 0x6A1A1A)
                        .spawn(Region.KANDARIN, 1, 1, 1).underground(),
                // Tortured gorilla (the level-141 cavern version): all three styles, max hit 13, no prayers.
                MonsterDef.of(TORTURED, "Tortured gorilla").level(141).stats(210, 105, 95, 95, 95, 95).speed(5)
                        .attackBonus(22, 20, 22).defenceBonus(0, 0, 0, 25, 0)
                        .attack(melee(13)).attack(ranged(13)).attack(magic(13))
                        .drops(Drop.always("big_bones"),
                                // uniques: 1/1,000 onto the unique table, ten times rarer than the demonic gorilla's
                                Drop.of("zenyte_shard", 1, 1, 3000), Drop.of("ballista_limbs", 1, 1, 5000),
                                Drop.of("ballista_spring", 1, 1, 5000), Drop.of("light_frame", 1, 1, 7500),
                                Drop.of("heavy_frame", 1, 1, 15000), Drop.of("monkey_tail", 1, 1, 15000),
                                Drop.of("rune_med_helm", 1, 1, 24), Drop.of("rune_scimitar", 1, 1, 45),      // 41/1000, 22/1000
                                Drop.of("adamant_bolts", 1, 1, 12), Drop.of("cosmic_rune", 20, 30, 13),      // 86/1000, 76/1000
                                Drop.of("earth_rune", 400, 600, 14), Drop.of("death_rune", 10, 20, 14),      // 72/1000, 69/1000
                                Drop.herbs(16),                                                                // 61/1000
                                Drop.of("coins", 1000, 3000, 10), Drop.of("shark", 1, 1, 20),                 // approx: rest of table
                                Drop.rare(100), Kit.clue("hard", 100)) // approx
                        .slayer(1, "monkeys").passive().size(1.35f, 1.55f, 1.2f).moveSpeed(0.24)
                        .skin(0x5A4A3A, 0x6A5A4A, 0x4A3A2A, 0x8A2A2A)
                        .spawn(Region.KANDARIN, 1, 1, 1).underground());
    }

    private static EquipmentStats s(int ranged, int rangedStrength) {
        return new EquipmentStats(0, 0, 0, 0, ranged, 0, 0, 0, 0, 0, 0, rangedStrength, 0, 0);
    }

    static void items(ItemSink items) {
        items.add("zenyte_shard", "Zenyte shard", Kind.SIMPLE, "shard", 0xF0A020).value(75_000)
                .examine("A shard of unstable zenyte.");
        items.add("ballista_limbs", "Ballista limbs", Kind.SIMPLE, "limbs", 0x8A6A4A).value(50_000).examine("Sturdy struts.");
        items.add("ballista_spring", "Ballista spring", Kind.SIMPLE, "clockwork", 0x9A9AA0).value(50_000)
                .examine("A spring-loaded mechanism.");
        items.add("light_frame", "Light frame", Kind.SIMPLE, "stock", 0xA0784A).value(50_000).examine("A light wooden frame.");
        items.add("heavy_frame", "Heavy frame", Kind.SIMPLE, "stock", 0x6A4A2A).value(50_000).examine("A heavy wooden frame.");
        items.add("incomplete_light_ballista", "Incomplete light ballista", Kind.SIMPLE, "crossbow_u", 0xA0784A).value(50_000)
                .examine("The limbs have been attached to the frame.");
        items.add("incomplete_heavy_ballista", "Incomplete heavy ballista", Kind.SIMPLE, "crossbow_u", 0x6A4A2A).value(50_000)
                .examine("The limbs have been attached to the frame.");
        items.add("unstrung_light_ballista", "Unstrung light ballista", Kind.SIMPLE, "crossbow_u", 0xA0784A).value(50_000)
                .examine("Needs a suitably simian rope.");
        items.add("unstrung_heavy_ballista", "Unstrung heavy ballista", Kind.SIMPLE, "crossbow_u", 0x6A4A2A).value(50_000)
                .examine("Needs a suitably simian rope.");
        // Two-handed crossbows firing javelins (osrs/data/AmmoRules), 7-tick attack speed (wiki/DPS calculator).
        items.add("light_ballista", "Light ballista", Kind.BOW, "crossbow", 0xA0784A).value(125_000).stack(1)
                .equipment(new EquipmentDef(Slot.WEAPON, s(110, 0), Map.of(Skill.RANGED, 65), 7, AttackType.RANGED, WeaponClass.BOW))
                .examine("A weapon forged from the wreckage of an airship.");
        items.add("heavy_ballista", "Heavy ballista", Kind.BOW, "crossbow", 0x6A4A2A).value(950_000).stack(1)
                .equipment(new EquipmentDef(Slot.WEAPON, s(125, 15), Map.of(Skill.RANGED, 75), 7, AttackType.RANGED, WeaponClass.BOW))
                .examine("A powerful weapon forged from the wreckage of an airship.");
    }

    static void recipes(Consumer<Recipe> recipes) {
        // Wiki: limbs on a frame (47/72 Fletching, 15/30 XP), then the spring (15/30 XP), then a monkey tail (300/600 XP).
        recipes.accept(Recipe.builder("fletch_incomplete_light_ballista", MenuType.FLETCHING, "Incomplete light ballista")
                .skill(Skill.FLETCHING, 47, 15).in("light_frame", 1).in("ballista_limbs", 1).out("incomplete_light_ballista", 1)
                .ticks(3).build());
        recipes.accept(Recipe.builder("fletch_unstrung_light_ballista", MenuType.FLETCHING, "Unstrung light ballista")
                .skill(Skill.FLETCHING, 47, 15).in("incomplete_light_ballista", 1).in("ballista_spring", 1)
                .out("unstrung_light_ballista", 1).ticks(3).build());
        recipes.accept(Recipe.builder("fletch_light_ballista", MenuType.FLETCHING, "Light ballista")
                .skill(Skill.FLETCHING, 47, 300).in("unstrung_light_ballista", 1).in("monkey_tail", 1).out("light_ballista", 1)
                .ticks(3).build());
        recipes.accept(Recipe.builder("fletch_incomplete_heavy_ballista", MenuType.FLETCHING, "Incomplete heavy ballista")
                .skill(Skill.FLETCHING, 72, 30).in("heavy_frame", 1).in("ballista_limbs", 1).out("incomplete_heavy_ballista", 1)
                .ticks(3).build());
        recipes.accept(Recipe.builder("fletch_unstrung_heavy_ballista", MenuType.FLETCHING, "Unstrung heavy ballista")
                .skill(Skill.FLETCHING, 72, 30).in("incomplete_heavy_ballista", 1).in("ballista_spring", 1)
                .out("unstrung_heavy_ballista", 1).ticks(3).build());
        recipes.accept(Recipe.builder("fletch_heavy_ballista", MenuType.FLETCHING, "Heavy ballista")
                .skill(Skill.FLETCHING, 72, 600).in("unstrung_heavy_ballista", 1).in("monkey_tail", 1).out("heavy_ballista", 1)
                .ticks(3).build());
        // Uncut zenyte: a cut onyx and a zenyte shard (70 Crafting, 15 XP). OSRS fuses them in the Ape Atoll wall of flame
        // as a small zombie monkey; here it's a gem-cutting recipe (approx).
        recipes.accept(Recipe.builder("make_uncut_zenyte", MenuType.GEM_CUTTING, "Uncut zenyte").skill(Skill.CRAFTING, 70, 15)
                .in("onyx", 1).in("zenyte_shard", 1).out("uncut_zenyte", 1).ticks(3).build());
    }

    static CollectionLogData.Page collectionLog() {
        return new CollectionLogData.Page("Other", "Demonic Gorillas", List.of("zenyte_shard", "ballista_limbs", "ballista_spring",
                "light_frame", "heavy_frame", "monkey_tail"));
    }
}
