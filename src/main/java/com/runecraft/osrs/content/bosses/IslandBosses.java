package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.data.Drop;

import java.util.List;

/**
 * The Deranged archaeologist (Fossil Island, September 2017), at the south end of the Tar Swamp (cache tile 3683, 3706).
 * Stats are the wiki DPS calculator's; drops are his 131-slot table as independent 1/x rolls. His "Learn to read!"
 * book shower is run by the bosses module's Minecraft-side DerangedArchaeologistScript.
 */
public final class IslandBosses {
    private IslandBosses() {
    }

    public static final String DERANGED = "deranged_archaeologist";
    /** Book shower: max hit on the tile a book lands on (2x his standard ranged max hit, Mod Ash). */
    public static final int BOOK_DIRECT_MAX = 56;
    /** ... and on the eight tiles around it (1/3 of the direct hit). */
    public static final int BOOK_SPLASH_MAX = 18;
    /** Game ticks from "Learn to read!" until the books explode. */
    public static final int BOOK_DELAY_TICKS = 4;
    /** The central book bursts into two more books, landing this many ticks later. */
    public static final int BOOK_SECOND_DELAY_TICKS = 3; // approx
    /** Internal cooldown of the special (25 ticks) and its chance to replace an attack (1/3). */
    public static final int SPECIAL_COOLDOWN_TICKS = 25;
    public static final int SPECIAL_CHANCE = 3;

    /** Ranged (his primary attack) and melee, which he only uses on a player standing next to him. Max hit 25 each. */
    public static final AttackDef DERANGED_RANGED = AttackDef.of("ranged", AttackType.RANGED, 25, 16, 3).particle("minecraft:crit");
    public static final AttackDef DERANGED_MELEE = AttackDef.of("melee", AttackType.CRUSH, 25, 1.5, 6);

    public static List<BossDef> all() {
        return List.of(deranged());
    }

    static BossDef deranged() {
        return BossDef.of(DERANGED, "Deranged archaeologist").level(276).stats(200, 280, 160, 280, 1, 320).speed(3)
                .attackBonus(280, 0, 90).defenceBonus(20, 20, 50, 300, 300)
                .attack(DERANGED_RANGED).attack(DERANGED_MELEE)
                .drops(Drop.always("bones"),
                        Drop.of("steel_ring", 1, 1, 44),                                                 // 3/131
                        Drop.of("black_dhide_body", 1, 1, 22), Drop.of("rune_2h_sword", 1, 1, 44),     // 6/131, 3/131
                        Drop.of("rune_sword", 1, 1, 33),                                                 // 4/131
                        Drop.of("water_rune", 100, 100, 22), Drop.of("mud_rune", 40, 40, 22), Drop.of("rune_knife", 25, 25, 22),
                        Drop.of("dragon_arrow", 60, 60, 131), Drop.of("cannonball", 80, 80, 33),
                        Drop.treeHerbSeeds(131),
                        Drop.of("grimy_dwarf_weed", 4, 4, 16), Drop.of("white_berries", 10, 10, 19),   // 8/131, 7/131
                        Drop.of("black_dragonhide", 8, 8, 26), Drop.of("gold_ore", 10, 10, 22),
                        Drop.of("uncut_diamond", 5, 5, 26), Drop.of("onyx_bolt_tips", 6, 6, 22),
                        Drop.of("anchovy_pizza", 2, 2, 33), Drop.of("prayer_potion", 1, 1, 16),
                        Drop.of("potato_with_cheese", 3, 3, 16), Drop.of("shark", 2, 2, 16),
                        Drop.rare(22),                                                                   // 6/131
                        // Fossil Island tertiaries (approx: the wiki's FossilDropLines template).
                        Drop.of("numulite", 5, 10, 28), Drop.of("unidentified_small_fossil", 1, 1, 24),
                        Drop.of("unidentified_medium_fossil", 1, 1, 48), Drop.of("unidentified_large_fossil", 1, 1, 96),
                        Drop.of("unidentified_rare_fossil", 1, 1, 480),
                        Drop.of("clue_scroll_elite", 1, 1, 200))
                // Konar, Nieve and Duradel's crazy archaeologist boss task also counts him (slayer SlayerBossTasks).
                .at(BossPlaces.DERANGED_SWAMP).arena(9, "jungle").category("Bosses").size(0.6f, 1.95f, 1.0f).moveSpeed(0.25);
    }

    static void items(ItemSink items) {
        // The steel ring (+24 stab/slash/crush defence) is with the other boss rings in BossItems.
        // Plucked from the growth in the Scar after Desert Treasure II; fed to Brutus it starts the Demonic Brutus fight.
        items.add("abyssal_potato", "Abyssal potato", Kind.SIMPLE, "produce", 0x4A2A4A).value(1)
                .examine("Seems like something a bovine would still eat.");
    }
}
