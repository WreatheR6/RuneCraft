package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.CollectionLogData;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.data.Drop;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Champions' Challenge (OSRS): champion scrolls drop at 1/5,000 from the champions' kin; Larxus in the
 * Champions' Guild (32 quest points) takes a scroll and sends you into the arena against that champion, who
 * sets a rule for the fight. Beating all ten earns Leon d'Cour's scroll, and beating Leon the Champion's cape.
 * Champion stats are the OSRS wiki's (DPS calculator data); rules are the wiki's champion table.
 */
public final class ChampionsChallenge {
    /** OSRS: every champion scroll is a 1/5,000 drop from its kin. */
    public static final int SCROLL_RATE = 5000;
    /** Larxus's quest point requirement (the Champions' Guild). */
    public static final int QUEST_POINTS = 32;
    public static final String LEON = "leon_dcour";
    public static final String LEON_SCROLL = "leons_champion_scroll";
    public static final String CAPE = "champions_cape";
    public static final String LARXUS = "mg_larxus";

    /** A champion's rule for the fight. */
    public enum Rule {
        NO_SPECIAL_ATTACKS("No special attacks"),
        MAGIC_ONLY("Only Magic attacks"),
        RANGED_ONLY("Only Ranged attacks"),
        NO_MAGIC("No Magic attacks"),
        MELEE_ONLY("Only Melee attacks"),
        NO_MELEE("No Melee attacks"),
        WEAPON_ONLY("Only a weapon: no armour and no items"),
        NO_PRAYER("No Prayer"),
        NO_RANGED("No Ranged attacks"),
        NO_EQUIPMENT("No weapons or armour (items allowed)"),
        NO_INVENTORY("No items in your inventory"); // approx: Leon's rule (sources differ)

        private final String text;

        Rule(String text) {
            this.text = text;
        }

        public String text() {
            return text;
        }

        /** Whether an attack of this style is allowed under the rule. */
        public boolean allows(AttackType type) {
            boolean melee = type != AttackType.MAGIC && type != AttackType.RANGED;
            return switch (this) {
                case MAGIC_ONLY -> type == AttackType.MAGIC;
                case RANGED_ONLY -> type == AttackType.RANGED;
                case NO_MAGIC -> type != AttackType.MAGIC;
                case MELEE_ONLY -> melee;
                case NO_MELEE -> !melee;
                case NO_RANGED -> type != AttackType.RANGED;
                default -> true;
            };
        }

        /** No armour, cape, jewellery or ammunition may be worn. */
        public boolean noArmour() {
            return this == WEAPON_ONLY || this == NO_EQUIPMENT;
        }

        /** No weapon may be wielded. */
        public boolean noWeapon() {
            return this == NO_EQUIPMENT;
        }

        /** The inventory must be empty (the wielded weapon aside). */
        public boolean emptyInventory() {
            return this == WEAPON_ONLY || this == NO_INVENTORY;
        }
    }

    /** A champion: its creature id, the scroll that summons it and its rule. */
    public record Champion(String id, String name, String scroll, Rule rule) {
    }

    public static final List<Champion> CHAMPIONS = List.of(
            new Champion("imp_champion", "Imp Champion", "imp_champion_scroll", Rule.NO_SPECIAL_ATTACKS),
            new Champion("goblin_champion", "Goblin Champion", "goblin_champion_scroll", Rule.MAGIC_ONLY),
            new Champion("skeleton_champion", "Skeleton Champion", "skeleton_champion_scroll", Rule.RANGED_ONLY),
            new Champion("zombies_champion", "Zombies Champion", "zombie_champion_scroll", Rule.NO_MAGIC),
            new Champion("giant_champion", "Giant Champion", "giant_champion_scroll", Rule.MELEE_ONLY),
            new Champion("hobgoblin_champion", "Hobgoblin Champion", "hobgoblin_champion_scroll", Rule.NO_MELEE),
            new Champion("ghoul_champion", "Ghoul Champion", "ghoul_champion_scroll", Rule.WEAPON_ONLY),
            new Champion("earth_warrior_champion", "Earth Warrior Champion", "earth_warrior_champion_scroll", Rule.NO_PRAYER),
            new Champion("jogre_champion", "Jogre Champion", "jogre_champion_scroll", Rule.NO_RANGED),
            new Champion("lesser_demon_champion", "Lesser Demon Champion", "lesser_demon_champion_scroll", Rule.NO_EQUIPMENT));

    public static final Champion LEON_DCOUR = new Champion(LEON, "Leon d'Cour", LEON_SCROLL, Rule.NO_INVENTORY);

    /** A reward Larxus hands out and when. */
    public record Reward(String item, String when) {
    }

    /** Larxus gives Leon d'Cour's scroll once all ten champions are beaten (and Leon isn't yet). */
    public static final Reward LARXUS_REWARD = new Reward(LEON_SCROLL, "all ten champions defeated");

    private ChampionsChallenge() {
    }

    /** Every champion, Leon d'Cour last. */
    public static List<Champion> all() {
        List<Champion> out = new ArrayList<>(CHAMPIONS);
        out.add(LEON_DCOUR);
        return out;
    }

    public static Champion byId(String id) {
        for (Champion c : all()) {
            if (c.id().equals(id)) {
                return c;
            }
        }
        return null;
    }

    public static Champion byScroll(String item) {
        for (Champion c : all()) {
            if (c.scroll().equals(item)) {
                return c;
            }
        }
        return null;
    }

    /** The champion whose scroll the player would hand Larxus: the first carried scroll (Leon's last). */
    public static Champion chooseScroll(Predicate<String> carrying) {
        for (Champion c : all()) {
            if (carrying.test(c.scroll())) {
                return c;
            }
        }
        return null;
    }

    /** Whether all ten champions (not Leon) have been beaten. */
    public static boolean allTenDefeated(Predicate<String> defeated) {
        for (Champion c : CHAMPIONS) {
            if (!defeated.test(c.id())) {
                return false;
            }
        }
        return true;
    }

    /** Flag key remembering a beaten champion. */
    public static String defeatedFlag(String championId) {
        return "champion_defeated:" + championId;
    }

    /**
     * The scroll a monster drops at 1/{@value #SCROLL_RATE} (OSRS: imps, goblins, skeletons, zombies, giants,
     * hobgoblins, ghouls, earth warriors, jogres and lesser demons), or null. Champions and bosses don't.
     */
    public static String scrollFor(String monsterName) {
        if (monsterName == null) {
            return null;
        }
        String n = monsterName.toLowerCase(Locale.ROOT).trim();
        if (n.contains("champion")) {
            return null;
        }
        if (n.equals("imp")) {
            return "imp_champion_scroll";
        }
        if (n.contains("hobgoblin")) {
            return "hobgoblin_champion_scroll";
        }
        if (n.equals("goblin") || n.endsWith(" goblin")) {
            return "goblin_champion_scroll";
        }
        if (n.equals("skeleton") || n.startsWith("skeleton ") && !n.contains("hellhound") || n.endsWith(" skeleton")) {
            return "skeleton_champion_scroll";
        }
        if (n.equals("zombie") || n.startsWith("zombie ") || n.endsWith(" zombie")) {
            return "zombie_champion_scroll";
        }
        if (n.equals("hill giant") || n.equals("moss giant") || n.equals("fire giant") || n.equals("ice giant")) {
            return "giant_champion_scroll";
        }
        if (n.equals("ghoul")) {
            return "ghoul_champion_scroll";
        }
        if (n.equals("earth warrior")) {
            return "earth_warrior_champion_scroll";
        }
        if (n.equals("jogre")) {
            return "jogre_champion_scroll";
        }
        if (n.equals("lesser demon")) {
            return "lesser_demon_champion_scroll";
        }
        return null;
    }

    /** Whether a drop table already rolls an item (so the generic scroll roll is skipped). */
    public static boolean tableHas(List<Drop> table, String item) {
        if (table != null) {
            for (Drop d : table) {
                if (d.item().equals(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ creatures

    private static BossDef champion(String id, String name) {
        return BossDef.of(id, name).raidOnly().category("Minigames").speed(4);
    }

    /** The eleven champions (OSRS wiki stats; fought only in the Champions' Challenge arena). */
    public static List<BossDef> bosses() {
        List<BossDef> list = new ArrayList<>();
        list.add(champion("imp_champion", "Imp Champion").level(14).stats(40, 5, 5, 5, 1, 5)
                .attack(AttackDef.of("claw", AttackType.SLASH, 1, 2.5, 10))
                .attack(AttackDef.of("fireball", AttackType.RANGED, 1, 8, 6).particle("minecraft:flame"))
                .skin(0xB03020, 0x8A2010, 0x6A1808, 0xFFD040).size(0.6f, 1.0f, 0.7f));
        list.add(champion("goblin_champion", "Goblin Champion").level(24).stats(32, 1, 1, 14, 26, 1)
                .attack(AttackDef.of("wind blast", AttackType.MAGIC, 4, 8, 10).particle("minecraft:witch"))
                .skin(0x6A9A3A, 0x8A5A2A, 0x5A3A1A, 0xE0C040).size(0.6f, 1.4f, 0.8f));
        list.add(champion("skeleton_champion", "Skeleton Champion").level(40).stats(58, 1, 1, 34, 1, 36)
                .attack(AttackDef.of("arrow", AttackType.RANGED, 5, 8, 10).particle("minecraft:crit"))
                .skin(0xE0DCC8, 0xC8C4B0, 0xB0AC98, 0xE0C040).attributes("undead"));
        list.add(champion("zombies_champion", "Zombies Champion").level(51).stats(60, 38, 42, 42, 1, 1)
                .attack(AttackDef.of("stab", AttackType.STAB, 5, 2.5, 10))
                .skin(0x6A8A5A, 0x4A5A3A, 0x3A4A2A, 0xE0C040).attributes("undead"));
        list.add(champion("giant_champion", "Giant Champion").level(56).stats(70, 36, 44, 52, 1, 1)
                .attack(AttackDef.of("club", AttackType.CRUSH, 5, 3, 10))
                .skin(0xC8A080, 0x6A4A2A, 0x5A3A1A, 0xE0C040).size(1.0f, 3.2f, 1.6f));
        list.add(champion("hobgoblin_champion", "Hobgoblin Champion").level(56).stats(58, 44, 48, 48, 1, 44)
                .attack(AttackDef.of("spear throw", AttackType.RANGED, 6, 8, 10).particle("minecraft:crit"))
                .skin(0x9A6A4A, 0x5A6A3A, 0x3A4A2A, 0xE0C040).size(0.7f, 2.0f, 1.05f));
        list.add(champion("ghoul_champion", "Ghoul Champion").level(85).stats(100, 60, 80, 60, 1, 60)
                .attack(AttackDef.of("claw", AttackType.CRUSH, 9, 2.5, 10))
                .attack(AttackDef.of("bone throw", AttackType.RANGED, 9, 8, 5).particle("minecraft:crit"))
                .skin(0x8A9A8A, 0x5A6A5A, 0x4A5A4A, 0xE0C040).attributes("undead"));
        list.add(champion("earth_warrior_champion", "Earth Warrior Champion").level(102).stats(108, 84, 84, 84, 1, 1)
                .attack(AttackDef.of("fist", AttackType.CRUSH, 9, 2.5, 10))
                .skin(0x7A5A3A, 0x5A4A2A, 0x4A3A1A, 0xE0C040));
        list.add(champion("jogre_champion", "Jogre Champion").level(107).stats(120, 86, 86, 86, 1, 1)
                .attack(AttackDef.of("spear", AttackType.STAB, 10, 3, 10))
                .skin(0x8A8A5A, 0x6A5A3A, 0x4A3A2A, 0xE0C040).size(1.0f, 2.8f, 1.4f));
        list.add(champion("lesser_demon_champion", "Lesser Demon Champion").level(162).stats(148, 136, 140, 142, 136, 1)
                .attack(AttackDef.of("claw", AttackType.SLASH, 15, 3, 10))
                .attack(AttackDef.of("fire blast", AttackType.MAGIC, 15, 8, 4).particle("minecraft:flame"))
                .skin(0xA02018, 0x701810, 0x501008, 0xE0C040).size(1.0f, 2.6f, 1.3f).attributes("demon"));
        list.add(champion(LEON, "Leon d'Cour").level(141).stats(123, 123, 123, 123, 1, 1)
                .attack(AttackDef.of("mace", AttackType.CRUSH, 13, 2.5, 10))
                .drops(Drop.always(CAPE)) // OSRS: beating Leon earns his cape
                .skin(0xC8A080, 0xC0C0D0, 0x3A4A8A, 0xE0C040));
        return list;
    }

    /** Collection log page (OSRS "Champion's Challenge": the eleven scrolls and the cape). */
    public static CollectionLogData.Page collectionPage(String category) {
        List<String> items = new ArrayList<>();
        for (Champion c : all()) {
            items.add(c.scroll());
        }
        items.add(CAPE);
        return new CollectionLogData.Page(category, "Champion's Challenge", items);
    }
}
