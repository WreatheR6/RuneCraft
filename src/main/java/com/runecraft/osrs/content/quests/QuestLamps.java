package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.CommonItems;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Quest experience lamps, tomes and skill choices: OSRS hands out most quest XP that the player places themselves as an item
 * to rub (antique lamps, the combat/dreamy/blessed/ancient lamps, tomes of experience) or as a choice in the closing dialogue
 * (Daero's and Duke's training, Ellen, King Sorvott, Irena, Father Reen). Values follow each item's wiki page and the quests'
 * Rewards sections (snapshot: {@code src/test/resources/osrs/quests/quest_lamps.tsv}); the Minecraft side
 * ({@code com.runecraft.content.quests.QuestLampService}) grants them on completion and opens the skill choice.
 *
 * <p>Lamps rub once; the combat lamp (two wishes) and tomes (three or six chapters) are one item with several uses. A
 * {@link Kind#CHOICE} has no item: the player picks when the quest completes, or later by talking to its NPC. The per-player
 * count of uses taken ({@code QuestsState}) lets a lost lamp be reclaimed from its NPC, as OSRS allows for most of them.
 */
public final class QuestLamps {
    private QuestLamps() {
    }

    public enum Kind {
        /** An item rubbed once per use. */
        LAMP,
        /** A book read one chapter at a time; it crumbles after the last. */
        TOME,
        /** A choice in dialogue: no item. */
        CHOICE
    }

    /** Attack, Strength, Defence, Hitpoints, Ranged and Magic. */
    public static final Set<Skill> COMBAT = Set.copyOf(EnumSet.of(Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.HITPOINTS,
            Skill.RANGED, Skill.MAGIC));
    /** {@link #COMBAT} and Prayer. */
    public static final Set<Skill> COMBAT_PRAYER = Set.copyOf(EnumSet.of(Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.HITPOINTS,
            Skill.RANGED, Skill.MAGIC, Skill.PRAYER));
    /** Attack, Strength, Defence and Hitpoints. */
    public static final Set<Skill> MELEE = Set.copyOf(EnumSet.of(Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.HITPOINTS));
    /** Any skill. */
    public static final Set<Skill> ANY = Set.copyOf(EnumSet.allOf(Skill.class));

    /**
     * One lamp, tome or choice: {@code xp} per use and {@code uses} per item (wishes, chapters; choices given at once), in one
     * of {@code skills} at {@code minLevel} or above (unboosted). {@code packages}, when not empty, replace the skill buttons
     * with fixed XP sets (Daero's training). {@code npc} gives it back (a lost item) or re-offers an unmade choice; null when
     * OSRS has nobody to reclaim it from.
     */
    public record Lamp(String id, String name, Kind kind, int xp, int uses, Set<Skill> skills, int minLevel, String npc,
                       List<Package> packages, String examine) {
        public boolean anySkill() {
            return skills.size() == Skill.values().length;
        }

        /** Whether XP from this lamp may go into {@code skill} at that (unboosted) level. */
        public boolean allows(Skill skill, int level) {
            return skills.contains(skill) && level >= minLevel;
        }

        /** The single skill of a fixed-skill lamp (Path of Glouphrie, the dusty lamp), else null. */
        public Skill fixedSkill() {
            return packages.isEmpty() && skills.size() == 1 ? skills.iterator().next() : null;
        }

        /** "10,000 XP in any skill of level 40 or above". */
        public String describe() {
            String amount = String.format(Locale.ROOT, "%,d", xp) + " XP";
            if (!packages.isEmpty()) {
                return "a choice of training (" + String.join(" or ", packages.stream().map(Package::label).toList()) + ")";
            }
            String where;
            if (anySkill()) {
                where = "any skill";
            } else if (skills.equals(COMBAT)) {
                where = "Attack, Strength, Defence, Hitpoints, Ranged or Magic";
            } else if (skills.equals(COMBAT_PRAYER)) {
                where = "a combat skill or Prayer";
            } else if (skills.size() == 1) {
                where = skills.iterator().next().displayName();
            } else {
                List<String> names = new ArrayList<>();
                for (Skill s : Skill.values()) {
                    if (skills.contains(s)) {
                        names.add(s.displayName());
                    }
                }
                where = String.join(", ", names.subList(0, names.size() - 1)) + " or " + names.get(names.size() - 1);
            }
            return amount + " in " + where + (minLevel > 1 ? " (level " + minLevel + "+)" : "");
        }
    }

    /** A fixed set of XP chosen as one (Daero: 35,000 in two skills and 20,000 in the other two). */
    public record Package(String label, Map<Skill, Integer> xp) {
    }

    /**
     * What a quest hands out: {@code count} items of the lamp (for a choice, {@code count} times its uses). {@code fewerIfGranted}
     * names another quest whose own grant of the same lamp takes one off this one (A Kingdom Divided gives one lamp fewer to
     * players who claimed Architectural Alliance's). {@code needsChoice} must be made before this one can be (Duke trains only
     * players Daero trained).
     */
    public record Grant(String quest, String lamp, int count, String fewerIfGranted, String needsChoice) {
    }

    private static final Map<String, Lamp> LAMPS = new LinkedHashMap<>();
    private static final List<Grant> GRANTS = new ArrayList<>();
    /** Old reward items that now work as a lamp (saves from before the lamps existed). */
    private static final Map<String, String> ALIASES = Map.of("book_of_knowledge", "lamp_of_knowledge");

    private static Lamp lamp(String id, String name, Kind kind, int xp, int uses, Set<Skill> skills, int minLevel, String npc,
                             String examine) {
        Lamp l = new Lamp(id, name, kind, xp, uses, skills, minLevel, npc, List.of(), examine);
        LAMPS.put(id, l);
        return l;
    }

    private static void grant(String quest, String lamp, int count) {
        GRANTS.add(new Grant(quest, lamp, count, null, null));
    }

    private static final String RUB = "Good for rubbing.";

    static {
        // ---- antique lamps (wiki: Antique lamp, and each "Antique lamp (<quest>)" page) ----
        lamp("quest_lamp_a_kingdom_divided", "Antique lamp", Kind.LAMP, 10_000, 1, ANY, 40, "commander_fullore", RUB);
        GRANTS.add(new Grant("a_kingdom_divided", "quest_lamp_a_kingdom_divided", 2, "architectural_alliance", null));
        // Architectural Alliance (removed from OSRS on 10 January 2024): Hosa's lamp, recoloured into A Kingdom Divided's; the two
        // quests give two lamps between them (wiki: a player who claimed Hosa's gets one from Commander Fullore)
        GRANTS.add(new Grant("architectural_alliance", "quest_lamp_a_kingdom_divided", 1, "a_kingdom_divided", null));
        lamp("quest_lamp_a_night_at_the_theatre", "Antique lamp", Kind.LAMP, 20_000, 1, COMBAT, 50, "quest_mysterious_stranger_tob", RUB);
        grant("a_night_at_the_theatre", "quest_lamp_a_night_at_the_theatre", 4);
        // A Tail of Two Cats (in the present) and Fairytale II - Cure a Queen (the Fairy Queen) give the same lamp
        lamp("quest_lamp_a_tail_of_two_cats", "Antique lamp", Kind.LAMP, 2_500, 1, ANY, 30, null, "I wonder what happens if I rub it...");
        grant("a_tail_of_two_cats", "quest_lamp_a_tail_of_two_cats", 2);
        grant("fairytale_ii_cure_a_queen", "quest_lamp_a_tail_of_two_cats", 1);
        lamp("quest_lamp_client_of_kourend", "Antique lamp", Kind.LAMP, 500, 1, ANY, 1, "veos_port_sarim", "I love rubbing things!");
        grant("client_of_kourend", "quest_lamp_client_of_kourend", 2);
        lamp("quest_lamp_into_the_tombs", "Antique lamp", Kind.LAMP, 50_000, 1, COMBAT, 60, "maisa", RUB);
        grant("into_the_tombs", "quest_lamp_into_the_tombs", 1);
        lamp("quest_lamp_kings_ransom", "Antique lamp", Kind.LAMP, 5_000, 1, ANY, 50, null, "I wonder what happens if I rub it...");
        grant("kings_ransom", "quest_lamp_kings_ransom", 1);
        // Legends' Quest: Radimus Erkle's four lamps, twelve skills, no level needed
        lamp("quest_lamp_legends_quest", "Antique lamp", Kind.LAMP, 30_000, 1, Set.copyOf(EnumSet.of(Skill.ATTACK, Skill.DEFENCE,
                Skill.STRENGTH, Skill.HITPOINTS, Skill.PRAYER, Skill.MAGIC, Skill.WOODCUTTING, Skill.CRAFTING, Skill.SMITHING,
                Skill.HERBLORE, Skill.AGILITY, Skill.THIEVING)), 1, "radimus_erkle", RUB);
        grant("legends_quest", "quest_lamp_legends_quest", 4);
        lamp("quest_lamp_one_small_favour", "Antique lamp", Kind.LAMP, 10_000, 1, ANY, 30, "yanni_salika",
                "I wonder what happens if I rub it...");
        grant("one_small_favour", "quest_lamp_one_small_favour", 2);
        lamp("quest_lamp_recipe_for_disaster", "Antique lamp", Kind.LAMP, 20_000, 1, ANY, 50, null, "I wonder what happens if I rub it...");
        grant("recipe_for_disaster", "quest_lamp_recipe_for_disaster", 1);
        lamp("quest_lamp_the_final_dawn", "Antique lamp", Kind.LAMP, 55_000, 1, COMBAT_PRAYER, 40, "prince_itzla_arkan", "Give it a rub.");
        grant("the_final_dawn", "quest_lamp_the_final_dawn", 1);
        lamp("quest_lamp_x_marks_the_spot", "Antique lamp", Kind.LAMP, 300, 1, ANY, 1, "veos_port_sarim", "I wonder what happens if I rub it...");
        grant("x_marks_the_spot", "quest_lamp_x_marks_the_spot", 1);
        // ---- the named lamps (wiki: Experience lamp) ----
        lamp("combat_lamp", "Combat lamp", Kind.LAMP, 7_000, 2, COMBAT, 1, "high_priest_sophanem",
                "A mystical lamp, engraved with scenes of carnage.");
        grant("contact", "combat_lamp", 1);
        lamp("dreamy_lamp", "Dreamy lamp", Kind.LAMP, 15_000, 1, Set.copyOf(EnumSet.of(Skill.STRENGTH, Skill.RANGED, Skill.MAGIC,
                Skill.HITPOINTS, Skill.DEFENCE)), 1, "oneiromancer", "A lunar-styled lamp. I wonder what's inside?");
        grant("dream_mentor", "dreamy_lamp", 1);
        lamp("blessed_lamp", "Blessed lamp", Kind.LAMP, 5_000, 1, ANY, 30, "quest_tranquility_harmony", "This will answer my prayers.");
        grant("the_great_brain_robbery", "blessed_lamp", 1);
        // approx: OSRS reclaims it from the Mysterious Bandit in the Ancient Vault, who isn't placed; Azzanadra gives it back
        lamp("ancient_lamp", "Ancient lamp", Kind.LAMP, 100_000, 1, COMBAT_PRAYER, 60, "azzanadra", RUB);
        grant("desert_treasure_ii", "ancient_lamp", 3);
        lamp("lamp_of_knowledge", "Lamp of knowledge", Kind.LAMP, 10_000, 1, ANY, 40, "quest_logosia", "Knowledge!");
        grant("in_search_of_knowledge", "lamp_of_knowledge", 1);
        lamp("dusty_lamp", "Dusty lamp", Kind.LAMP, 20_000, 1, Set.of(Skill.PRAYER), 1, "quest_strange_old_man_barrows", RUB);
        grant("his_faithful_servants", "dusty_lamp", 1);
        lamp("magic_lamp_ides_of_milk", "Magic lamp", Kind.LAMP, 1_000, 1, COMBAT_PRAYER, 1, "quest_gillie_groats", RUB);
        grant("the_ides_of_milk", "magic_lamp_ides_of_milk", 1);
        // The Path of Glouphrie: four magic lamps, each of one skill (wiki: Magic lamp (The Path of Glouphrie))
        lamp("magic_lamp_strength", "Magic lamp (strength)", Kind.LAMP, 30_000, 1, Set.of(Skill.STRENGTH), 1, null, "Give it a rub.");
        lamp("magic_lamp_slayer", "Magic lamp (slayer)", Kind.LAMP, 20_000, 1, Set.of(Skill.SLAYER), 1, null, "Give it a rub.");
        lamp("magic_lamp_thieving", "Magic lamp (thieving)", Kind.LAMP, 5_000, 1, Set.of(Skill.THIEVING), 1, null, "Give it a rub.");
        lamp("magic_lamp_magic", "Magic lamp (magic)", Kind.LAMP, 5_000, 1, Set.of(Skill.MAGIC), 1, null, "Give it a rub.");
        grant("the_path_of_glouphrie", "magic_lamp_strength", 1);
        grant("the_path_of_glouphrie", "magic_lamp_slayer", 1);
        grant("the_path_of_glouphrie", "magic_lamp_thieving", 1);
        grant("the_path_of_glouphrie", "magic_lamp_magic", 1);
        // ---- the Varrock Museum's antique lamps (wiki: Antique lamp (Varrock Museum)): the items are CommonItems', handed out by
        // Historian Minas for the timeline quests and by the specimen crates (minigames, MuseumService); none can be replaced ----
        lamp(CommonItems.MUSEUM_LAMP_FINDS, "Antique lamp", Kind.LAMP, 500, 1, ANY, 10, null, "I wonder what happens if I rub it...");
        lamp(CommonItems.MUSEUM_LAMP_TIMELINE, "Antique lamp", Kind.LAMP, 1_000, 1, ANY, 20, null, "I wonder what happens if I rub it...");
        lamp(CommonItems.MUSEUM_LAMP_DEFENDER, "Antique lamp", Kind.LAMP, 5_000, 1, ANY, 30, null, "I wonder what happens if I rub it...");
        lamp(CommonItems.MUSEUM_LAMP_EMPTY_LORD, "Antique lamp", Kind.LAMP, 10_000, 1, ANY, 50, null, "I wonder what happens if I rub it...");
        // ---- tomes of experience (wiki: Tome of experience (<quest>)) ----
        lamp("tome_of_experience_hallowvale", "Tome of experience", Kind.TOME, 2_000, 3, ANY, 30, "veliaf_hurtz",
                "A tome of experience that awards 2,000 XP per chapter.");
        grant("darkness_of_hallowvale", "tome_of_experience_hallowvale", 1);
        lamp("tome_of_experience_hope", "Tome of experience", Kind.TOME, 2_500, 3, ANY, 35, "ivan_strom",
                "A tome of experience that awards 2,500 XP per chapter.");
        grant("a_taste_of_hope", "tome_of_experience_hope", 1);
        lamp("tome_of_experience_sins", "Tome of experience", Kind.TOME, 15_000, 6, ANY, 60, "ivan_strom",
                "A tome of experience that awards 15,000 XP per chapter.");
        grant("sins_of_the_father", "tome_of_experience_sins", 1);
        // approx: OSRS reclaims it from Efaritay Hallow at Castle Drakan, who isn't placed; Veliaf gives it back
        lamp("tome_of_experience_blood_moon", "Tome of experience", Kind.TOME, 30_000, 6, ANY, 70, "veliaf_hurtz",
                "A tome of experience that awards 30,000 XP per chapter.");
        grant("the_blood_moon_rises", "tome_of_experience_blood_moon", 1);
        // ---- choices in dialogue (no item) ----
        Map<Skill, Integer> attDef = new EnumMap<>(Skill.class);
        attDef.put(Skill.ATTACK, 35_000);
        attDef.put(Skill.DEFENCE, 35_000);
        attDef.put(Skill.STRENGTH, 20_000);
        attDef.put(Skill.HITPOINTS, 20_000);
        Map<Skill, Integer> strHp = new EnumMap<>(Skill.class);
        strHp.put(Skill.STRENGTH, 35_000);
        strHp.put(Skill.HITPOINTS, 35_000);
        strHp.put(Skill.ATTACK, 20_000);
        strHp.put(Skill.DEFENCE, 20_000);
        // Monkey Madness I: Daero trains Attack and Defence or Strength and Hitpoints (35,000 each, 20,000 in the other two)
        LAMPS.put("mm1_daero_training", new Lamp("mm1_daero_training", "Daero's training", Kind.CHOICE, 0, 1, MELEE, 1, "daero",
                List.of(new Package("Attack and Defence", Map.copyOf(attDef)), new Package("Strength and Hitpoints", Map.copyOf(strHp))),
                "Daero's combat training."));
        grant("monkey_madness_i", "mm1_daero_training", 1);
        // Monkey Madness II: Duke's two 50,000 XP rewards, only for players Daero trained. // approx: Duke (Ape Atoll's east hill)
        // isn't placed; King Narnode passes them on
        lamp("mm2_duke_training", "Duke's training", Kind.CHOICE, 50_000, 2, COMBAT, 1, "king_narnode", "Duke's combat training.");
        GRANTS.add(new Grant("monkey_madness_ii", "mm2_duke_training", 1, null, "mm1_daero_training"));
        // Dragon Slayer II: Ellen in the Myths' Guild, four times 25,000 XP. // approx: Ellen isn't placed; Alec Kincade offers it
        lamp("ds2_ellen_training", "Ellen's training", Kind.CHOICE, 25_000, 4, COMBAT, 1, "alec_kincade", "Ellen's combat training.");
        grant("dragon_slayer_ii", "ds2_ellen_training", 1);
        // The Fremennik Isles: King Sorvott's two 10,000 XP rewards in Attack, Strength, Defence or Hitpoints
        lamp("fremennik_isles_reward", "King Sorvott's reward", Kind.CHOICE, 10_000, 2, MELEE, 1, "gjuki_sorvott", "Combat training.");
        grant("the_fremennik_isles", "fremennik_isles_reward", 1);
        // The Tourist Trap: Irena's two 4,650 XP rewards in Agility, Fletching, Smithing or Thieving
        lamp("tourist_trap_reward", "Irena's reward", Kind.CHOICE, 4_650, 2, Set.copyOf(EnumSet.of(Skill.AGILITY, Skill.FLETCHING,
                Skill.SMITHING, Skill.THIEVING)), 1, "irena", "Skill training.");
        grant("the_tourist_trap", "tourist_trap_reward", 1);
        // Shadow of the Storm: 10,000 XP in a combat skill other than Prayer
        lamp("shadow_of_the_storm_reward", "Father Reen's reward", Kind.CHOICE, 10_000, 1, COMBAT, 1, "father_reen", "Combat training.");
        grant("shadow_of_the_storm", "shadow_of_the_storm_reward", 1);
    }

    /** Every lamp, tome and choice, in declaration order. */
    public static List<Lamp> all() {
        return List.copyOf(LAMPS.values());
    }

    /** The lamp with this item id (or an old reward item that now works as one), or null. */
    public static Lamp get(String id) {
        if (id == null) {
            return null;
        }
        Lamp l = LAMPS.get(id);
        return l != null ? l : LAMPS.get(ALIASES.get(id));
    }

    /** The lamp an old reward item stands for, or the id itself. */
    public static String canonical(String id) {
        return ALIASES.getOrDefault(id, id);
    }

    public static List<Grant> grants() {
        return List.copyOf(GRANTS);
    }

    /** What completing {@code quest} hands out. */
    public static List<Grant> grants(String quest) {
        List<Grant> out = new ArrayList<>();
        for (Grant g : GRANTS) {
            if (g.quest().equals(quest)) {
                out.add(g);
            }
        }
        return out;
    }

    /** Every quest that hands out a lamp, tome or choice. */
    public static Set<String> quests() {
        Set<String> out = new java.util.LinkedHashSet<>();
        GRANTS.forEach(g -> out.add(g.quest()));
        return out;
    }

    /**
     * How many items (lamps, tomes) or choices a grant hands over, given which quests already granted the player something
     * (A Kingdom Divided and Architectural Alliance give two lamps between them, whichever comes first).
     */
    public static int count(Grant g, java.util.function.Predicate<String> grantedQuest) {
        return Math.max(0, g.count() - (g.fewerIfGranted() != null && grantedQuest.test(g.fewerIfGranted()) ? 1 : 0));
    }

    /**
     * XP a quest's fixed-skill lamps hand out per skill (The Path of Glouphrie's four magic lamps, the dusty lamp): the wiki
     * lists these with the quest's fixed XP.
     */
    public static Map<Skill, Double> fixedXp(String quest) {
        Map<Skill, Double> out = new EnumMap<>(Skill.class);
        for (Grant g : grants(quest)) {
            Lamp l = LAMPS.get(g.lamp());
            Skill s = l.fixedSkill();
            if (s != null) {
                out.merge(s, (double) l.xp() * l.uses() * g.count(), Double::sum);
            }
        }
        return out;
    }

    /** XP of one use of a package (Daero) or the lamp's flat XP. */
    public static Map<Skill, Integer> xpOf(Lamp l, Skill skill, int pkg) {
        if (!l.packages().isEmpty()) {
            return pkg >= 0 && pkg < l.packages().size() ? l.packages().get(pkg).xp() : Map.of();
        }
        return skill == null || !l.skills().contains(skill) ? Map.of() : Map.of(skill, l.xp());
    }

    /**
     * Uses still owed of a lamp across the quests that granted it: {@code granted(grant)} is how many items (or choices) that
     * grant handed the player (0 if none), {@code used} the player's uses taken so far.
     */
    public static int owedUses(String lamp, java.util.function.ToIntFunction<Grant> granted, int used) {
        Lamp l = LAMPS.get(lamp);
        if (l == null) {
            return 0;
        }
        int total = 0;
        for (Grant g : GRANTS) {
            if (g.lamp().equals(lamp)) {
                total += granted.applyAsInt(g) * l.uses();
            }
        }
        return Math.max(0, total - used);
    }

    /** Items of a lamp a player may reclaim: whole items still owed beyond those they carry or bank. */
    public static int reclaimable(String lamp, java.util.function.ToIntFunction<Grant> granted, int used, int held) {
        Lamp l = LAMPS.get(lamp);
        if (l == null || l.kind() == Kind.CHOICE || l.npc() == null) {
            return 0;
        }
        int owed = owedUses(lamp, granted, used);
        int items = (owed + l.uses() - 1) / l.uses();
        return Math.max(0, items - held);
    }

    /** Whether a lamp's item is defined in CommonItems (the Varrock Museum's lamps, which another module hands out). */
    public static boolean commonItem(String id) {
        return id.equals(CommonItems.MUSEUM_LAMP_FINDS) || id.equals(CommonItems.MUSEUM_LAMP_TIMELINE)
                || id.equals(CommonItems.MUSEUM_LAMP_DEFENDER) || id.equals(CommonItems.MUSEUM_LAMP_EMPTY_LORD);
    }

    /** Registers the lamp and tome items. */
    static void items(QuestBook b) {
        b.items(s -> {
            for (Lamp l : LAMPS.values()) {
                if (l.kind() == Kind.CHOICE || commonItem(l.id())) {
                    continue;
                }
                s.add(l.id(), l.name(), ItemDef.Kind.SIMPLE, l.kind() == Kind.TOME ? "book" : "lamp",
                        l.kind() == Kind.TOME ? 0x6A3A8A : l.id().startsWith("magic_lamp") ? 0x5AC8E0 : 0xE8C030)
                        .value(l.kind() == Kind.TOME ? 1 : 50).stack(1).examine(l.examine() + " (" + l.describe()
                                + (l.uses() > 1 ? ", " + l.uses() + (l.kind() == Kind.TOME ? " chapters)" : " wishes)") : ")"));
            }
        });
    }
}
