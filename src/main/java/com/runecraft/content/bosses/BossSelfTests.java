package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Entry points of the in-game self-test ({@code boss:} checks in {@code selftest/GameplayChecks}) into the package-private
 * boss scripts: each drives one scripted mechanic on a freshly spawned boss and returns a summary, or throws.
 */
public final class BossSelfTests {
    private BossSelfTests() {
    }

    /** The Nightmare's parasites: weakened by a cure, heal her while they live. */
    public static String nightmareParasites(DataBossEntity boss, ServerPlayerEntity p) {
        return EncounterScripts.selfTestNightmare(boss, p);
    }

    /** The Phantom Muspah's prayer shield drains (Smite, corruption) and spikes. */
    public static String muspahShield(DataBossEntity boss, ServerPlayerEntity p) {
        return EncounterScripts.selfTestMuspah(boss, p);
    }

    /** Araxxor's nine eggs: hatching order and destroyed eggs. */
    public static String araxxorEggs(DataBossEntity boss, ServerPlayerEntity p) {
        return SlayerScripts.selfTestAraxxor(boss, p);
    }
}
