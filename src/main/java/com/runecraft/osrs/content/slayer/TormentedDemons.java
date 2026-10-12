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
import java.util.Set;
import java.util.function.Consumer;

/**
 * Tormented Demons (While Guthix Sleeps, reworked 2024), in the Ancient Guthix Temple below the Lumbridge Swamp Caves.
 * Stats from the wiki DPS calculator; they count as greater demons for Slayer tasks. Their mechanics (protection prayer
 * swapped every 25% of health lost, a fire shield that weakens non-demonbane hits until it drops, fire bombs) are run by
 * {@code com.runecraft.content.slayer.TormentedDemonCombat}. Uniques: tormented synapse (osrs/content/CommonItems) and
 * burning claw, each 1/500; the synapse makes Emberlight, the scorching bow and the purging staff (skilling module).
 */
public final class TormentedDemons {
    public static final String ID = "tormented_demon";

    /** Protection prayer swap: after losing this fraction of its hitpoints since the last swap. */
    public static final double PRAYER_SWAP_FRACTION = 0.25;
    /** Game ticks the demon stops attacking after swapping its prayer. */
    public static final int PRAYER_SWAP_PAUSE_TICKS = 6;
    /** Fire shield: fraction of non-demonbane damage that still lands while it is up. */
    public static final double SHIELD_DAMAGE_FACTOR = 0.8; // approx: wiki unconfirmed (20-33% reduction)
    /** Game ticks the shield stays down after the first hit and after each fire bomb. */
    public static final int SHIELD_DOWN_TICKS = 10; // approx
    /** Auto attacks between fire bombs (8 or 9 on the wiki). */
    public static final int ATTACKS_PER_BOMB = 8;
    /** Weapons that ignore the fire shield and get their demonbane bonus. */
    public static final Set<String> DEMONBANE = Set.of("silverlight", "darklight", "arclight", "emberlight", "burning_claws",
            "scorching_bow", "purging_staff");

    /** Demonbane weapon bonuses against demons (accuracy and damage multiplier), wiki "Demonbane weapons". */
    public static final Map<String, Double> DEMONBANE_BONUS = Map.of("emberlight", 1.70, "scorching_bow", 1.30,
            "burning_claws", 1.05);

    private TormentedDemons() {
    }

    /** True when an attack with this weapon gets past the fire shield. */
    public static boolean pierces(String weaponId) {
        return weaponId != null && DEMONBANE.contains(weaponId);
    }

    /** Damage that lands through the fire shield. */
    public static int throughShield(int damage, boolean shieldUp, String weaponId) {
        if (!shieldUp || pierces(weaponId)) {
            return damage;
        }
        return (int) Math.floor(damage * SHIELD_DAMAGE_FACTOR);
    }

    /** Whether the demon swaps its prayer after losing {@code lostSinceSwap} of {@code maxHp}. */
    public static boolean swapsPrayer(int lostSinceSwap, int maxHp) {
        return lostSinceSwap >= Math.ceil(maxHp * PRAYER_SWAP_FRACTION);
    }

    static MonsterDef monster() {
        return MonsterDef.of(ID, "Tormented Demon").level(450).stats(600, 255, 255, 150, 255, 255).speed(6)
                .attackBonus(400, 300, 300)
                .defenceBonus(75, 175, 68, 5, 150) // ranged: standard 150 (light 140, heavy 90)
                .attack(AttackDef.of("claws", AttackType.SLASH, 31, 3.0, 4))
                .attack(AttackDef.of("fire_blast", AttackType.MAGIC, 31, 10, 3).particle("minecraft:flame"))
                .attack(AttackDef.of("fire_spit", AttackType.RANGED, 31, 10, 3).particle("minecraft:small_flame"))
                .attack(AttackDef.of("fire_bomb", AttackType.MAGIC, 45, 12, 1).special(AttackDef.Special.SKY_STRIKE, 0)
                        .telegraph(2, "The tormented demon hurls fire bombs at your feet!").particle("minecraft:lava"))
                .attributes("demon")
                .drops(Drop.always("infernal_ashes"), Drop.of("tormented_synapse", 1, 1, 500), Drop.of("burning_claw", 1, 1, 500),
                        Drop.of("rune_platebody", 1, 1, 13), Drop.of("dragon_dagger", 1, 1, 17), Drop.of("chaos_rune", 25, 100, 13),
                        Drop.of("fire_rune", 100, 300, 13), Drop.of("rune_scimitar", 1, 1, 26), Drop.of("prayer_potion", 1, 2, 26),
                        Drop.of("coins", 1000, 5000, 13), Kit.clue("elite", 300)) // commons beyond the wiki's /51 rows approx
                .slayer(1, "greater_demons").size(1.4f, 3.0f, 1.4f).skin(0x5A2A1A, 0x8A3A1A, 0x3A1A10, 0xF06020)
                .spawn(Region.LUMBRIDGE_SWAMP, 3, 1, 1).underground();
    }

    private static EquipmentStats s(int sa, int sl, int cr, int ma, int ra, int sd, int sld, int cd, int md, int rd,
                                    int str, int rstr, int mdmg, int prayer) {
        return new EquipmentStats(sa, sl, cr, ma, ra, sd, sld, cd, md, rd, str, rstr, mdmg, prayer);
    }

    static void items(ItemSink items) {
        items.add("burning_claw", "Burning claw", Kind.SIMPLE, "fang", 0xE05020).value(400_000).stack(1)
                .examine("A smouldering claw from a tormented demon. Two would make a fine pair of claws.");
        items.add("burning_claws", "Burning claws", Kind.MELEE_WEAPON, "claws", 0xE05020).value(800_000).stack(1).tier("dragon")
                .equipment(new EquipmentDef(Slot.WEAPON, s(43, 54, 0, 0, 0, 3, 6, 1, 0, 0, 32, 0, 0, 0),
                        Map.of(Skill.ATTACK, 60), 4, AttackType.SLASH, WeaponClass.MELEE))
                .examine("Claws that burn demons. Special attack: three fiery slashes.");
        items.add("emberlight", "Emberlight", Kind.MELEE_WEAPON, "longsword", 0xF08030).value(800_000).stack(1).tier("dragon")
                .equipment(new EquipmentDef(Slot.WEAPON, s(63, 70, 0, 0, 0, 0, 3, 2, 5, 0, 13, 0, 0, 0),
                        Map.of(Skill.ATTACK, 77), 4, AttackType.SLASH, WeaponClass.MELEE)) // Attack requirement approx
                .examine("Arclight reforged with a tormented synapse: +70% accuracy and damage against demons.");
        items.add("scorching_bow", "Scorching bow", Kind.BOW, "longbow", 0xE05020).value(800_000).stack(1).tier("dragon")
                .equipment(new EquipmentDef(Slot.WEAPON, s(0, 0, 0, 0, 124, 0, 0, 0, 0, 0, 0, 40, 0, 0),
                        Map.of(Skill.RANGED, 77), 5, AttackType.RANGED, WeaponClass.BOW))
                .examine("A magic longbow burning with a tormented synapse: +30% accuracy and damage against demons.");
    }

    static void recipes(Consumer<Recipe> recipes) {
        recipes.accept(Recipe.builder("make_burning_claws", MenuType.SMITHING, "Burning claws").in("burning_claw", 2)
                .out("burning_claws", 1).ticks(3).build());
        recipes.accept(Recipe.builder("fletch_scorching_bow", MenuType.FLETCHING, "Scorching bow").skill(Skill.FLETCHING, 74, 0)
                .in("magic_longbow_u", 1).in("tormented_synapse", 1).out("scorching_bow", 1).ticks(3).build()); // XP approx
        // Emberlight is Arclight (74 Smithing) + a tormented synapse; Arclight isn't an item in this world yet.
    }

    static CollectionLogData.Page collectionLog() {
        return new CollectionLogData.Page("Other", "Tormented Demons", List.of("tormented_synapse", "burning_claw"));
    }
}
