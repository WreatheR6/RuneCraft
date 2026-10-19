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
}
