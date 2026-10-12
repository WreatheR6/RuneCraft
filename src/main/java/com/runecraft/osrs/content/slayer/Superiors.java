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
 * not their hitpoints: OSRS gives roughly ten times the hitpoints (Greater abyssal demon: 400 HP,
 * 4,200 XP), so both are stored.
 */
public final class Superiors {
    public static final int MAX_HP = 1000;

    /**
     * @param hp OSRS hitpoints
     * @param xp OSRS Slayer XP for the kill (about ten times the hitpoints)
     */
    public record Superior(String id, String base, String name, int level, int hp, int xp, float scale) {
    }

    private static final Map<String, Superior> BY_BASE = new LinkedHashMap<>();
    private static final Map<String, Superior> BY_ID = new LinkedHashMap<>();

    private Superiors() {
    }

    /** A superior whose hitpoints are xp/10 (the usual OSRS ratio; approx where not listed explicitly). */
    private static void add(String base, String id, String name, int level, int xp, float scale) {
        add(base, id, name, level, Math.round(xp / 10f), xp, scale);
    }

    private static void add(String base, String id, String name, int level, int hp, int xp, float scale) {
        Superior s = new Superior(id, base, name, level, hp, xp, scale);
        BY_BASE.put(base, s);
        BY_ID.put(id, s);
    }

    static {
        add("crawling_hand", "crushing_hand", "Crushing hand", 45, 550, 1.6f);
        add("cave_crawler", "chasm_crawler", "Chasm Crawler", 68, 600, 1.6f);
        add("banshee", "screaming_banshee", "Screaming banshee", 70, 610, 1.3f);
        add("rockslug", "giant_rockslug", "Giant rockslug", 86, 770, 1.8f);
        add("cockatrice", "cockathrice", "Cockathrice", 89, 950, 1.5f);
        add("pyrefiend", "flaming_pyrelord", "Flaming pyrelord", 97, 1250, 1.6f);
        add("basilisk", "monstrous_basilisk", "Monstrous basilisk", 135, 1700, 1.6f);
        add("infernal_mage", "malevolent_mage", "Malevolent Mage", 162, 175, 1750, 1.2f); // approx XP (~10x HP)
        add("bloodveld", "insatiable_bloodveld", "Insatiable Bloodveld", 202, 2900, 1.4f);
        add("mutated_bloodveld", "insatiable_mutated_bloodveld", "Insatiable mutated Bloodveld", 278, 4100, 1.5f);
        add("jelly", "vitreous_jelly", "Vitreous Jelly", 206, 1900, 1.5f);
        add("turoth", "spiked_turoth", "Spiked Turoth", 244, 195, 2200, 1.3f);
        add("cave_horror", "cave_abomination", "Cave abomination", 206, 1300, 1.3f);
        add("aberrant_spectre", "abhorrent_spectre", "Abhorrent spectre", 253, 2500, 1.4f);
        add("wyrm", "shadow_wyrm", "Shadow Wyrm", 259, 290, 2800, 1.5f);
        add("dust_devil", "choke_devil", "Choke devil", 264, 3000, 1.5f);
        add("kurask", "king_kurask", "King kurask", 295, 270, 2767, 1.4f);
        add("gargoyle", "marble_gargoyle", "Marble gargoyle", 349, 270, 3044, 1.5f);
        add("nechryael", "nechryarch", "Nechryarch", 300, 320, 3068, 1.4f);
        add("drake", "guardian_drake", "Guardian Drake", 376, 590, 5900, 1.4f); // approx XP (~10x HP)
        add("abyssal_demon", "greater_abyssal_demon", "Greater abyssal demon", 342, 400, 4200, 1.5f);
        add("dark_beast", "night_beast", "Night beast", 374, 550, 6462, 1.5f);
        add("smoke_devil", "nuclear_smoke_devil", "Nuclear smoke devil", 280, 2400, 1.5f);
        add("hydra", "colossal_hydra", "Colossal Hydra", 309, 750, 7500, 1.4f); // approx XP (~10x HP)
        add("basilisk_knight", "basilisk_sentinel", "Basilisk Sentinel", 358, 520, 5200, 1.3f); // approx XP (~10x HP)
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
     * OSRS superior unique table: the chance of a unique is 8/(200 - s^2/200) where s is the
     * Slayer level; uniques are imbued heart (1/8), eternal gem (1/8), dust and mist battlestaff (3/8 each).
     */
    static int uniqueDenominator(int slayerLevel) {
        return Math.max(10, 200 - slayerLevel * slayerLevel / 200);
    }

    /** Builds the superior MonsterDefs from their base creatures (stronger stats, same category and mechanics). */
    static List<MonsterDef> build(List<MonsterDef> bases) {
        List<MonsterDef> out = new ArrayList<>();
        for (MonsterDef base : bases) {
            Superior s = BY_BASE.get(base.id);
            if (s == null) {
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
                    .slayer(base.slayerLevel, base.slayerCategory)
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
            int den = uniqueDenominator(base.slayerLevel);
            d.drops(base.drops.toArray(new Drop[0]));
            d.drops(Drop.of("imbued_heart", 1, 1, den), Drop.of("eternal_gem", 1, 1, den),
                    Drop.of("dust_battlestaff", 1, 1, Math.max(4, den / 3)), Drop.of("mist_battlestaff", 1, 1, Math.max(4, den / 3)));
            out.add(d);
        }
        return out;
    }

    private static int boost(int stat, double k) {
        return stat <= 1 ? 1 : (int) Math.min(400, Math.round(stat * Math.min(2.5, k)));
    }
}
