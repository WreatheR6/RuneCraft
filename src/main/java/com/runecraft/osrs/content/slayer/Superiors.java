package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Drop;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Superior slayer creatures ("Bigger and Badder"). They never spawn naturally: killing their base
 * creature on task has a 1/200 chance to summon one. Unlike ordinary monsters their Slayer XP is
 * not their hitpoints (Greater abyssal demon: 400 HP, 4,200 XP), so both are stored. Hitpoints and XP
 * are the OSRS Wiki infobox values (the combat stats come from {@code MonsterStatsTable}).
 */
public final class Superiors {
    public static final int MAX_HP = 1000;

    /**
     * @param base     id of the creature whose death may summon it
     * @param template this module's creature the superior is modelled on (the base itself, or a lookalike when
     *                 the base belongs to another module: twisted banshee, warped jelly, deviant spectre)
     * @param slayer   Slayer level of the base creature (drives the unique drop rate)
     * @param hp       OSRS hitpoints
     * @param xp       OSRS Slayer XP for the kill
     */
    public record Superior(String id, String base, String template, String name, int level, int slayer, int hp,
                           double xp, float scale) {
    }

    private static final Map<String, Superior> BY_BASE = new LinkedHashMap<>();
    private static final Map<String, Superior> BY_ID = new LinkedHashMap<>();

    private Superiors() {
    }

    private static void add(String base, String id, String name, int level, int slayer, int hp, double xp, float scale) {
        add(base, base, id, name, level, slayer, hp, xp, scale);
    }

    private static void add(String base, String template, String id, String name, int level, int slayer, int hp, double xp,
                            float scale) {
        Superior s = new Superior(id, base, template, name, level, slayer, hp, xp, scale);
        BY_BASE.put(base, s);
        BY_ID.put(id, s);
    }

    /** Another creature that summons an existing superior (greater nechryael: Nechryarch). */
    private static void alsoFrom(String base, String superiorId) {
        BY_BASE.put(base, BY_ID.get(superiorId));
    }

    static {
        add("crawling_hand", "crushing_hand", "Crushing hand", 45, 5, 55, 550, 1.6f);
        add("cave_crawler", "chasm_crawler", "Chasm Crawler", 68, 10, 60, 600, 1.6f);
        add("banshee", "screaming_banshee", "Screaming banshee", 70, 15, 61, 610, 1.3f);
        add("twisted_banshee", "banshee", "screaming_twisted_banshee", "Screaming twisted banshee", 144, 15, 220, 2200, 1.5f);
        add("rockslug", "giant_rockslug", "Giant rockslug", 86, 20, 77, 770, 1.8f);
        add("cockatrice", "cockathrice", "Cockathrice", 89, 25, 95, 950, 1.5f);
        add("pyrefiend", "flaming_pyrelord", "Flaming pyrelord", 97, 30, 125, 1250, 1.6f);
        add("basilisk", "monstrous_basilisk", "Monstrous basilisk", 135, 40, 170, 1700, 1.6f);
        add("infernal_mage", "malevolent_mage", "Malevolent Mage", 162, 45, 175, 1750, 1.2f);
        add("bloodveld", "insatiable_bloodveld", "Insatiable Bloodveld", 202, 50, 290, 2900, 1.4f);
        add("mutated_bloodveld", "insatiable_mutated_bloodveld", "Insatiable mutated Bloodveld", 278, 50, 410, 4100, 1.5f);
        add("jelly", "vitreous_jelly", "Vitreous Jelly", 206, 52, 190, 1900, 1.5f);
        add("warped_jelly", "jelly", "vitreous_warped_jelly", "Vitreous warped Jelly", 241, 52, 220, 2200, 1.6f);
        add("turoth", "spiked_turoth", "Spiked Turoth", 244, 55, 195, 1998, 1.3f);
        add("cave_horror", "cave_abomination", "Cave abomination", 206, 58, 130, 1300, 1.3f);
        add("aberrant_spectre", "abhorrent_spectre", "Abhorrent spectre", 253, 60, 250, 2500, 1.4f);
        add("deviant_spectre", "aberrant_spectre", "repugnant_spectre", "Repugnant spectre", 335, 60, 380, 4085, 1.5f);
        add("basilisk_knight", "basilisk_sentinel", "Basilisk Sentinel", 358, 60, 520, 5590, 1.3f);
        add("wyrm", "shadow_wyrm", "Shadow Wyrm", 259, 62, 290, 3520, 1.5f);
        add("dust_devil", "choke_devil", "Choke devil", 264, 65, 300, 3000, 1.5f);
        add("kurask", "king_kurask", "King kurask", 295, 70, 270, 2767.4, 1.4f);
        add("gargoyle", "marble_gargoyle", "Marble gargoyle", 349, 75, 270, 2768, 1.5f);
        add("nechryael", "nechryarch", "Nechryarch", 300, 80, 320, 3280, 1.4f);
        alsoFrom("greater_nechryael", "nechryarch");
        add("drake", "guardian_drake", "Guardian Drake", 376, 84, 590, 7087, 1.4f);
        add("abyssal_demon", "greater_abyssal_demon", "Greater abyssal demon", 342, 85, 400, 4200, 1.5f);
        add("dark_beast", "night_beast", "Night beast", 374, 90, 550, 6462, 1.5f);
        add("smoke_devil", "nuclear_smoke_devil", "Nuclear smoke devil", 280, 93, 240, 2400, 1.5f);
        add("hydra", "colossal_hydra", "Colossal Hydra", 309, 95, 750, 8625, 1.4f);
        // Round 44: the superiors of the wiki's table that were missing (infobox values). The mutated terrorbird and
        // tortoise are still missing: their warped bases belong to the dungeons module and nothing here looks like them.
        add("pyrelord", "infernal_pyrelord", "Infernal pyrelord", 134, 30, 184, 1840, 1.5f);
        add("chilled_jelly", "vitreous_chilled_jelly", "Vitreous Chilled Jelly", 241, 52, 220, 2200, 1.6f);
        add("lava_strykewyrm", "wyrm", "magma_strykewyrm", "Magma strykewyrm", 249, 62, 340, 3655, 1.5f);
        add("elder_custodian_stalker", "ancient_custodian", "Ancient Custodian", 239, 76, 330, 3382, 1.3f);
        add("araxyte", "dreadborn_araxyte", "Dreadborn Araxyte", 281, 92, 350, 4462.5, 1.4f);
        add("gryphon", "dire_gryphon", "Dire gryphon", 209, 51, 280, 2800, 1.4f);
    }

    public static Superior forBase(String baseId) {
        return BY_BASE.get(baseId);
    }

    public static Superior byId(String id) {
        return BY_ID.get(id);
    }

    public static Map<String, Superior> all() {
        return BY_ID;
    }

    /**
     * OSRS superior unique tables: each of the two tables is rolled with chance 1/(200 - (s+55)^2/125), where
     * s is the base creature's Slayer level. Table one holds the imbued heart (1/8) and the dust and mist
     * battlestaves (7/16 each); table two the eternal gem (1/8).
     *
     * @return the table denominator 200 - (s+55)^2/125
     */
    public static double uniqueTableDenominator(int slayerLevel) {
        return 200 - (slayerLevel + 55) * (slayerLevel + 55) / 125.0;
    }

    /** Whole-number drop rarities {heart, eternal gem, dust battlestaff, mist battlestaff} for a Slayer level. */
    public static int[] uniqueRarities(int slayerLevel) {
        double t = uniqueTableDenominator(slayerLevel);
        int eighth = (int) Math.round(t * 8);
        int staff = (int) Math.round(t * 16 / 7);
        return new int[]{eighth, eighth, staff, staff};
    }

    /** Builds the superior MonsterDefs from their templates (stronger stats, same category and mechanics). */
    static List<MonsterDef> build(List<MonsterDef> bases) {
        Map<String, MonsterDef> byId = new LinkedHashMap<>();
        for (MonsterDef m : bases) {
            byId.put(m.id, m);
        }
        List<MonsterDef> out = new ArrayList<>();
        for (Superior s : BY_ID.values()) {
            MonsterDef base = byId.get(s.template());
            if (base == null) {
                continue;
            }
            double k = Math.max(1.2, s.level() / (double) Math.max(1, base.combatLevel));
            MonsterDef d = MonsterDef.of(s.id(), s.name()).level(s.level())
                    .stats(Math.min(MAX_HP, s.hp()), boost(base.attack, k), boost(base.strength, k), boost(base.defence, k),
                            boost(base.magic, k), boost(base.ranged, k))
                    .speed(base.attackSpeed)
                    .defenceBonus(base.defenceBonus(com.runecraft.osrs.AttackType.STAB) + 20,
                            base.defenceBonus(com.runecraft.osrs.AttackType.SLASH) + 20,
                            base.defenceBonus(com.runecraft.osrs.AttackType.CRUSH) + 20,
                            base.defenceBonus(com.runecraft.osrs.AttackType.MAGIC) + 20,
                            base.defenceBonus(com.runecraft.osrs.AttackType.RANGED) + 20)
                    .slayer(Math.max(base.slayerLevel, s.slayer()), base.slayerCategory)
                    .size(base.width, base.height, base.scale * s.scale())
                    .moveSpeed(base.moveSpeed);
            for (AttackDef a : base.attacks) {
                d.attack(new AttackDef(a.name(), a.style(), (int) Math.ceil(a.maxHit() * Math.min(2.0, 1 + (k - 1) * 0.5)),
                        a.range(), a.weight(), a.special(), a.param(), a.telegraphTicks(), a.particle(), a.message()));
            }
            d.immuneTo.addAll(base.immuneTo);
            d.model = base.model;
            d.skin = base.skin.clone();
            d.flying = base.flying;
            d.poisonous = base.poisonous;
            d.drops(base.drops.toArray(new Drop[0]));
            int[] r = uniqueRarities(s.slayer());
            d.drops(Drop.of("imbued_heart", 1, 1, r[0]), Drop.of("eternal_gem", 1, 1, r[1]),
                    Drop.of("dust_battlestaff", 1, 1, r[2]), Drop.of("mist_battlestaff", 1, 1, r[3]));
            out.add(d);
        }
        return out;
    }

    private static int boost(int stat, double k) {
        return stat <= 1 ? 1 : (int) Math.min(400, Math.round(stat * Math.min(2.5, k)));
    }
}
