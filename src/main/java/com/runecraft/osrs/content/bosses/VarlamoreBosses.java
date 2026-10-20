package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.data.Drop;

import java.util.List;

/**
 * Newer bosses: The Hueycoatl, Amoxliatl and the Royal Titans (Branda the Fire Queen and Eldric the
 * Ice King). Their reference stats come from {@link com.runecraft.osrs.data.MonsterStatsTable}; drop rates follow the
 * wiki tables for a solo kill with full contribution.
 */
public final class VarlamoreBosses {
    private VarlamoreBosses() {
    }

    // ---- The Hueycoatl (OSRS Wiki "The Hueycoatl/Strategies"): Rain Fire launches the feathers on her back, coloured by
    // style (up to 14, 0-2 through the right protection prayer); glowing symbols call lightning (up to 16 after 4 ticks);
    // the tail slam's shockwave deals 20-25 (typeless). Was 30/30/30, 35 and 40, above anything she can hit.
    public static final AttackDef HUEY_MELEE = AttackDef.of("tail_strike", AttackType.CRUSH, 14, 16, 1)
            .telegraph(2, "The Hueycoatl's feathers glow red...").particle("minecraft:flame");
    public static final AttackDef HUEY_RANGED = AttackDef.of("earth_spike", AttackType.RANGED, 14, 16, 1)
            .telegraph(2, "The Hueycoatl's feathers glow green...").particle("minecraft:composter");
    public static final AttackDef HUEY_MAGIC = AttackDef.of("storm_bolt", AttackType.MAGIC, 14, 16, 1)
            .telegraph(2, "The Hueycoatl's feathers glow blue...").particle("minecraft:electric_spark");
    public static final AttackDef HUEY_LIGHTNING = AttackDef.of("thunderstorm", AttackType.MAGIC, 16, 16, 1).special(Special.SKY_STRIKE, 0)
            .telegraph(3, "Thunder rumbles overhead!").particle("minecraft:electric_spark");
    public static final AttackDef HUEY_TAIL = AttackDef.of("tail_sweep", AttackType.CRUSH, 25, 6, 1).special(Special.KNOCKBACK, 5)
            .telegraph(3, "The Hueycoatl coils to sweep its tail!");

    // ---- Amoxliatl: a magic-only spectral boss (0 Attack/Strength); 22 standard, 34 Icicle Crash ----
    public static final AttackDef AMOX_ICE = AttackDef.of("ice_shard", AttackType.MAGIC, 22, 14, 4).particle("minecraft:snowflake");
    public static final AttackDef AMOX_FREEZE = AttackDef.of("glacial_burst", AttackType.MAGIC, 20, 14, 2).special(Special.FREEZE, 5)
            .telegraph(3, "Amoxliatl chills the air around you!").particle("minecraft:snowflake");
    /** Icicle Crash: her prayers-off special; re-activating Protect from Magic before it lands negates it (OSRS Wiki). */
    public static final AttackDef AMOX_SPIKES = AttackDef.of("icicle_crash", AttackType.MAGIC, 34, 14, 2)
            .telegraph(3, "Amoxliatl disables your prayers and launches an icicle attack!").particle("minecraft:item_snowball");

    // ---- Royal Titans ----
    /**
     * The Titans' only auto-attack is the stomp in melee range, up to 26 (OSRS Wiki; MonsterStatsTable applies the 26). Their
     * fire/icicle walls (12), elementals and Elemental Blast (30) are scripted; the firestorm, eruption, blizzard and
     * avalanche attacks they had are not in OSRS.
     */
    public static final AttackDef BRANDA_MELEE = AttackDef.of("molten_fist", AttackType.CRUSH, 35, 4, 5);
    public static final AttackDef ELDRIC_MELEE = AttackDef.of("frozen_fist", AttackType.CRUSH, 35, 4, 5);

    public static List<BossDef> all() {
        return List.of(hueycoatl(), amoxliatl(), branda(), eldric());
    }

    static BossDef hueycoatl() {
        return BossDef.of("the_hueycoatl", "The Hueycoatl").level(1030).stats(1600, 300, 300, 250, 300, 300).speed(5)
                .defenceBonus(100, 100, 100, 100, 100)
                .attack(HUEY_MELEE).attack(HUEY_RANGED).attack(HUEY_MAGIC).attack(HUEY_LIGHTNING).attack(HUEY_TAIL)
                // Wiki (solo, full contribution): a 1/18 unique roll split into hides 11/315 (x3), tome 1/90 and wand 1/105.
                .drops(Drop.of("hueycoatl_hide", 3, 3, 29), Drop.of("tome_of_earth", 1, 1, 90), Drop.of("dragon_hunter_wand", 1, 1, 105),
                        Drop.of("dragon_bones", 10, 20, 4), Drop.of("runite_ore", 4, 8, 6), Drop.of("earth_rune", 500, 1000, 4),
                        Drop.of("coins", 20000, 60000, 4), Drop.of("clue_scroll_elite", 1, 1, 23))
                .pet("pet_huberte").at("hueycoatl_lair").arena(15, "jungle").size(1.8f, 2.6f, 2.2f).moveSpeed(0.0);
    }

    static BossDef amoxliatl() {
        return BossDef.of("amoxliatl", "Amoxliatl").level(300).stats(400, 180, 180, 160, 200, 1).speed(4)
                .defenceBonus(60, 60, 60, 120, 60)
                .attack(AMOX_ICE).attack(AMOX_FREEZE).attack(AMOX_SPIKES)
                .drops(Drop.of("frozen_tear", 2, 6, 1), Drop.of("glacial_temotli", 1, 1, 100), Drop.of("pendant_of_ates", 1, 1, 25),
                        Drop.of("water_rune", 200, 400, 4), Drop.of("adamantite_bar", 5, 10, 8), Drop.of("coins", 5000, 15000, 4))
                .pet("pet_moxi").at("neypotzli").arena(12, "ice").size(1.2f, 1.4f, 1.8f).moveSpeed(0.26);
    }

    static BossDef branda() {
        return BossDef.of("branda_the_fire_queen", "Branda the Fire Queen").level(389).stats(600, 250, 250, 200, 250, 1).speed(5)
                .defenceBonus(100, 100, 100, 60, 100)
                .attack(BRANDA_MELEE) // the stomp; walls, elementals and the Elemental Blast are scripted (OSRS Wiki)
                // Wiki (solo): giantsoul amulet 1/16, each staff crown and prayer scroll 1/75.
                .drops(Drop.always("big_bones"), Drop.of("deadeye_prayer_scroll", 1, 1, 75), Drop.of("mystic_vigour_prayer_scroll", 1, 1, 75),
                        Drop.of("giantsoul_amulet", 1, 1, 16), Drop.of("fire_element_staff_crown", 1, 1, 75),
                        Drop.of("ice_element_staff_crown", 1, 1, 75), Drop.of("fire_rune", 500, 1000, 4), Drop.of("runite_bar", 2, 5, 8),
                        Drop.of("coins", 10000, 30000, 4), Drop.of("clue_scroll_elite", 1, 1, 23))
                .pet("pet_bran").at("royal_titans_lair").arena(15, "lava").size(1.1f, 2.6f, 2.0f).moveSpeed(0.2);
    }

    static BossDef eldric() {
        return BossDef.of("eldric_the_ice_king", "Eldric the Ice King").level(389).stats(600, 250, 250, 200, 1, 250).speed(5)
                .defenceBonus(100, 100, 100, 100, 60)
                .attack(ELDRIC_MELEE)
                .raidOnly().size(1.1f, 2.6f, 2.0f).moveSpeed(0.2);
    }
}
