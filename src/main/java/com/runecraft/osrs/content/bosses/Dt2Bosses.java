package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.data.Drop;

import java.util.ArrayList;
import java.util.List;

/**
 * Desert Treasure II bosses: Duke Sucellus, The Leviathan, The Whisperer and Vardorvis. Each drops
 * chromium ingots, its vestige (for the improved Fremennik rings), a soulreaper axe piece and virtus.
 */
public final class Dt2Bosses {
    private Dt2Bosses() {
    }

    // ---- Duke Sucellus ----
    public static final AttackDef DUKE_SLAM = AttackDef.of("fist_slam", AttackType.CRUSH, 46, 5, 3);
    public static final AttackDef DUKE_MAGIC = AttackDef.of("magic_blast", AttackType.MAGIC, 40, 16, 4).particle("minecraft:witch");
    /** The freezing gaze hits everyone the Duke can see; hide behind a pillar. */
    public static final AttackDef DUKE_GAZE = AttackDef.of("freezing_gaze", AttackType.MAGIC, 75, 16, 1)
            .special(Special.PRAYER_PIERCING, 0).telegraph(5, "Duke Sucellus' eyes begin to glow... break his line of sight!")
            .particle("minecraft:snowflake");
    public static final AttackDef DUKE_VENTS = AttackDef.of("gas_vent", AttackType.MAGIC, 20, 16, 2).special(Special.HAZARD, 5)
            .particle("minecraft:sneeze").message("Poisonous gas erupts from the vents!");

    // ---- The Leviathan ----
    public static final AttackDef LEVI_RANGED = AttackDef.of("boulder", AttackType.RANGED, 40, 16, 1).particle("minecraft:crit");
    public static final AttackDef LEVI_MAGIC = AttackDef.of("abyssal_orb", AttackType.MAGIC, 40, 16, 1).particle("minecraft:witch");
    public static final AttackDef LEVI_MELEE = AttackDef.of("shadow_strike", AttackType.CRUSH, 40, 16, 1).particle("minecraft:squid_ink");
    public static final AttackDef LEVI_DEBRIS = AttackDef.of("falling_debris", AttackType.CRUSH, 45, 16, 1).special(Special.SKY_STRIKE, 0)
            .telegraph(3, "Debris shakes loose from the cavern roof!").particle("minecraft:ash");
    public static final AttackDef LEVI_LIGHTNING = AttackDef.of("lightning", AttackType.MAGIC, 35, 16, 1).special(Special.SKY_STRIKE, 0)
            .telegraph(3, "The Leviathan's shell crackles with lightning!").particle("minecraft:electric_spark");

    // ---- The Whisperer ----
    public static final AttackDef WHISPER_MAGIC = AttackDef.of("soul_bolt", AttackType.MAGIC, 35, 16, 3).particle("minecraft:soul");
    public static final AttackDef WHISPER_RANGED = AttackDef.of("tentacle_lash", AttackType.RANGED, 35, 16, 3).particle("minecraft:squid_ink");
    public static final AttackDef WHISPER_SCREECH = AttackDef.of("screech", AttackType.MAGIC, 70, 16, 1).special(Special.AOE, 6)
            .telegraph(5, "The Whisperer lets out a piercing screech... get away!").particle("minecraft:sonic_boom");
    public static final AttackDef WHISPER_SIPHON = AttackDef.of("soul_siphon", AttackType.MAGIC, 20, 16, 1).special(Special.LIFESTEAL, 100)
            .particle("minecraft:sculk_soul").message("The Whisperer siphons your soul!");

    // ---- Vardorvis ----
    public static final AttackDef VARD_SLASH = AttackDef.of("axe_swing", AttackType.SLASH, 35, 4, 6);
    public static final AttackDef VARD_AXES = AttackDef.of("swinging_axes", AttackType.SLASH, 50, 16, 2).special(Special.SKY_STRIKE, 0)
            .telegraph(3, "Axes swing out from the walls!").particle("minecraft:sweep_attack");
    public static final AttackDef VARD_HEAD_RANGED = AttackDef.of("head_ranged", AttackType.RANGED, 30, 16, 1)
            .telegraph(2, "Vardorvis' head emerges and glares at you - its eyes glow green!").particle("minecraft:crit");
    public static final AttackDef VARD_HEAD_MAGIC = AttackDef.of("head_magic", AttackType.MAGIC, 30, 16, 1)
            .telegraph(2, "Vardorvis' head emerges and glares at you - its eyes glow blue!").particle("minecraft:witch");
    public static final AttackDef VARD_BLOOD = AttackDef.of("blood_splat", AttackType.MAGIC, 20, 16, 1).special(Special.SKY_STRIKE, 0)
            .telegraph(3, "Blood splatters across the ground - don't let it heal him!").particle("minecraft:damage_indicator");

    public static final int TABLET_RATE = 25;
    public static final int QUARTZ_RATE = 200;
    /** Each boss's teleport tablet and quartz (for the ancient sceptres and the Zaros icons). */
    public static final List<String> TABLETS = List.of("frozen_tablet", "scarred_tablet", "sirenic_tablet", "strangled_tablet");
    public static final List<String> QUARTZES = List.of("ice_quartz", "smoke_quartz", "shadow_quartz", "blood_quartz");

    public static List<BossDef> all() {
        return List.of(duke(), leviathan(), whisperer(), vardorvis());
    }

    /**
     * OSRS (Desert Treasure II - Tweaks and Drop Rates): a 1/n roll for the tradeable unique table, which gives a vestige
     * roll 3/8 (three rolls make the vestige), a chromium ingot 3/8, the boss's axe piece/weapon 1/8 and a virtus piece
     * 1/8 (each of the three equally); the awakener's orb is rolled next. Awakened kills roll the unique table at 3/n
     * ({@link AwakenedBosses#drops}).
     */
    static List<Drop> dt2Drops(String bossId, String vestige, String axePiece, String tablet, String quartz) {
        int n = AwakenedBosses.uniqueTableRate(bossId);
        List<Drop> d = new ArrayList<>(List.of(Drop.of("chromium_ingot", 1, 1, Math.round(8 * n / 3f)), Drop.of(vestige, 1, 1, 8 * n),
                Drop.of(axePiece, 1, 1, 8 * n), Drop.of("virtus_mask", 1, 1, 24 * n), Drop.of("virtus_robe_top", 1, 1, 24 * n),
                Drop.of("virtus_robe_bottom", 1, 1, 24 * n), Drop.of(AwakenedBosses.ORB, 1, 1, AwakenedBosses.orbRate(bossId)),
                Drop.of("runite_bar", 4, 8, 8), // approx: the standard table below is a simplification
                Drop.of("death_rune", 200, 300, 8), Drop.of("soul_rune", 150, 250, 8), Drop.of("dragon_platelegs", 1, 1, 16),
                Drop.of("coins", 30000, 90000, 4), Drop.of("clue_scroll_elite", 1, 1, 160)));
        // Wiki: the boss's tablet (one-off teleport to the lair) 1/25 and its quartz 1/200, both rolled after the unique
        // table (shown as ~1/25.8 and ~1/206). Before the first quartz OSRS improves its rate from 1/200 to 1/50 over the
        // first 300 kills; that ramp isn't modelled. // approx
        d.add(Drop.of(tablet, 1, 1, TABLET_RATE));
        d.add(Drop.of(quartz, 1, 1, QUARTZ_RATE));
        return d;
    }

    static BossDef duke() {
        BossDef b = BossDef.of("duke_sucellus", "Duke Sucellus").level(1146).stats(485, 300, 300, 200, 300, 1).speed(5)
                .defenceBonus(20, 250, 250, 300, 300)
                .attack(DUKE_SLAM).attack(DUKE_MAGIC).attack(DUKE_GAZE).attack(DUKE_VENTS)
                .pet("pet_baron").at("ghorrock_prison").arena(13, "ice").size(1.8f, 2.4f, 2.0f).moveSpeed(0.0);
        b.drops.addAll(dt2Drops(b.id, "magus_vestige", "eye_of_the_duke", "frozen_tablet", "ice_quartz"));
        return b;
    }

    static BossDef leviathan() {
        BossDef b = BossDef.of("the_leviathan", "The Leviathan").level(1062).stats(900, 300, 300, 250, 300, 300).speed(4)
                .defenceBonus(300, 300, 300, 200, 40)
                .attack(LEVI_RANGED).attack(LEVI_MAGIC).attack(LEVI_MELEE).attack(LEVI_DEBRIS)
                .pet("pet_lilviathan").at("the_scar").arena(14, "water").size(2.2f, 2.2f, 2.0f).moveSpeed(0.0);
        b.drops.addAll(dt2Drops(b.id, "venator_vestige", "leviathans_lure", "scarred_tablet", "smoke_quartz"));
        return b;
    }

    static BossDef whisperer() {
        BossDef b = BossDef.of("the_whisperer", "The Whisperer").level(1227).stats(900, 250, 250, 200, 300, 300).speed(5)
                .defenceBonus(200, 200, 200, 20, 120)
                .attack(WHISPER_MAGIC).attack(WHISPER_RANGED).attack(WHISPER_SIPHON)
                .phase(50, "The Whisperer drags you into the Shadow Realm!", 1.1, false)
                .pet("pet_wisp").at("lassar_undercity").arena(13, "void").flying().size(1.8f, 2.6f, 2.0f).moveSpeed(0.18);
        b.drops.addAll(dt2Drops(b.id, "bellator_vestige", "sirens_staff", "sirenic_tablet", "shadow_quartz"));
        return b;
    }

    static BossDef vardorvis() {
        BossDef b = BossDef.of("vardorvis", "Vardorvis").level(1009).stats(700, 300, 300, 250, 1, 1).speed(5)
                .defenceBonus(40, 200, 200, 300, 300)
                .attack(VARD_SLASH).attack(VARD_AXES).attack(VARD_BLOOD)
                .pet("pet_butch").at("stranglewood").arena(12, "crypt").size(0.9f, 2.4f, 1.6f).moveSpeed(0.28);
        b.drops.addAll(dt2Drops(b.id, "ultor_vestige", "executioners_axe_head", "strangled_tablet", "blood_quartz"));
        return b;
    }
}
