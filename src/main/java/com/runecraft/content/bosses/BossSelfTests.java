package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import net.minecraft.server.level.ServerPlayer;

/**
 * Entry points of the in-game self-test ({@code boss:} checks in {@code selftest/GameplayChecks}) into the package-private
 * boss scripts: each drives one scripted mechanic on a freshly spawned boss and returns a summary, or throws.
 */
public final class BossSelfTests {
    private BossSelfTests() {
    }

    /** The Nightmare's parasites: weakened by a cure, heal her while they live. */
    public static String nightmareParasites(DataBossEntity boss, ServerPlayer p) {
        return EncounterScripts.selfTestNightmare(boss, p);
    }

    /** The Phantom Muspah's prayer shield drains (Smite, corruption) and spikes. */
    public static String muspahShield(DataBossEntity boss, ServerPlayer p) {
        return EncounterScripts.selfTestMuspah(boss, p);
    }

    /** Araxxor's nine eggs: hatching order and destroyed eggs. */
    public static String araxxorEggs(DataBossEntity boss, ServerPlayer p) {
        return SlayerScripts.selfTestAraxxor(boss, p);
    }

    /** The Eclipse Moon's clones: a faced clone is parried, one behind the player hits. */
    public static String eclipseClones(DataBossEntity boss, ServerPlayer p) {
        return MoonScripts.selfTestEclipse(boss, p);
    }

    /** A dossier (Yama) opens into a contract or chasm teleport scrolls. */
    public static String yamaDossier(DataBossEntity boss, ServerPlayer p) {
        com.runecraft.util.Inv.give(p, "dossier", 1);
        String got = BossesHooks.openDossier(p);
        if (got == null || !com.runecraft.util.Inv.has(p, got, 1) || com.runecraft.util.Inv.has(p, "dossier", 1)) {
            throw new IllegalStateException("the dossier didn't open (" + got + ")");
        }
        com.runecraft.util.Inv.remove(p, got, com.runecraft.util.Inv.count(p, got));
        return "dossier gave " + got;
    }

    /** Zalcano's armour only takes imbued tephra; a golem killed with tephra leaves 16-24 imbued tephra. */
    public static String zalcanoTephra(DataBossEntity boss, ServerPlayer p) {
        return ClassicScripts.selfTestZalcano(boss, p);
    }

    /** The Hueycoatl's body parts block the head until they are down. */
    public static String hueycoatlPhases(DataBossEntity boss, ServerPlayer p) {
        return EncounterScripts.selfTestHueycoatl(boss, p);
    }

    /**
     * The fight self-test ({@code boss:fight:<id>}) asks this while a boss can't be hurt: does the part of its mechanic a
     * player would do that isn't killing its adds (the test clears those itself), and returns what it did, or null.
     */
    public static String assist(DataBossEntity boss, ServerPlayer p) {
        if (boss.script() instanceof SlayerScripts.Kraken k) {
            return k.selfTestDisturb(p) ? "disturbed the whirlpools" : null;
        }
        if (boss.script() instanceof ClassicScripts.Zalcano) {
            return ClassicScripts.selfTestAssistZalcano(boss, p);
        }
        return null;
    }

    /**
     * Loot a kill leaves somewhere other than the ground or the inventory: the Doom of Mokhaiotl's delve loot waits in the
     * burrow hole until claimed at the stone. Returns the number of stacks held for the player.
     */
    public static int heldLoot(String bossId, ServerPlayer p) {
        return com.runecraft.osrs.content.bosses.DoomDelves.BOSS.equals(bossId) ? DoomDelveService.heldLoot(p) : 0;
    }

    /**
     * What a Hunllef's script is doing (the Gauntlet walk in {@code content/minigames/GauntletSelfTest}): whether it is in its
     * magic cycle, the style it protects from, attacks into the current cycle, off-prayer hits taken since its last prayer
     * switch, tornadoes out, floor tiles out (and whether they are orange) and attacks made.
     */
    public record HunllefView(boolean magic, com.runecraft.osrs.AttackType prays, int styleAttacks, int offPrayerHits,
                              int tornadoes, int floorTiles, boolean floorOrange, int attacks) {
    }

    /** The Hunllef script's view, or null when the boss is not a Hunllef. */
    public static HunllefView hunllef(DataBossEntity boss) {
        return boss.script() instanceof EncounterScripts.Hunllef h ? h.view() : null;
    }

    /**
     * Whether a player's ordinary weapon attacks can take hitpoints off this boss at all. False for Zalcano, who is only hurt
     * by imbued tephra (her armour) and pickaxes (once she falls).
     */
    public static boolean realAttacksHurt(String bossId) {
        return !"zalcano".equals(bossId);
    }
}
