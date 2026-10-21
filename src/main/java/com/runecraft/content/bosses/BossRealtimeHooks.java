package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.bosses.MinigameBosses;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Self-test hooks into the round 42 real-time boss patches ({@code selftest/BossRealtimeChecks}, {@code boss:realtime_*}): the
 * checks drive the fights in real time themselves; these only reach the scripts' package-private state.
 */
public final class BossRealtimeHooks {
    private BossRealtimeHooks() {
    }

    private static <T> T script(DataBossEntity boss, Class<T> type) {
        if (!type.isInstance(boss.script())) {
            throw new AssertionError(boss.def() == null ? "boss" : boss.def().id + " has no " + type.getSimpleName());
        }
        return type.cast(boss.script());
    }

    /** Holds a scripted boss's own attacks for {@code gameTicks} game ticks (its specials and the check's actions still run). */
    public static void hold(DataBossEntity boss, int gameTicks) {
        script(boss, ScriptedBoss.class).cooldown = gameTicks;
    }

    // ---------------- the Whisperer

    /** Her next volley, magic ({@code true}) and ranged projectiles in order. */
    public static void whispererVolley(DataBossEntity boss, boolean[] magic) {
        script(boss, WhispererScript.class).selfTestVolley(magic);
    }

    // ---------------- Cerberus

    /** The triple attack's styles shown over her now, the next first. */
    public static List<AttackType> cerberusTripleShown(DataBossEntity boss) {
        return script(boss, SlayerScripts.Cerberus.class).selfTestTripleShown();
    }

    /** Summons her souls now; {@code listener} sees each soul's cue and attack (null: none). */
    public static void cerberusSouls(DataBossEntity boss, SoulListener listener) {
        SlayerScripts.Cerberus c = script(boss, SlayerScripts.Cerberus.class);
        SlayerScripts.Cerberus.soulListener = listener == null ? null : listener::soul;
        c.selfTestSummonSouls(boss);
    }

    /** Stops listening to Cerberus' souls. */
    public static void cerberusSoulsDone() {
        SlayerScripts.Cerberus.soulListener = null;
    }

    /** A soul's cue ({@code attack} false) or attack: its place west to east, its style (0 ranged, 1 magic, 2 melee). */
    public interface SoulListener {
        void soul(int place, int style, boolean attack, long mc);
    }

    // ---------------- Vardorvis

    /** His head gazes now. */
    public static void vardorvisGaze(DataBossEntity boss, ServerPlayer target) {
        script(boss, EncounterScripts.Vardorvis.class).walkGaze(boss, target);
    }

    /** The gazes in the air: {landing MC, 1 magic / 0 ranged, launch MC}, in landing order. */
    public static List<long[]> vardorvisGazes(DataBossEntity boss) {
        return script(boss, EncounterScripts.Vardorvis.class).walkGazeLandings();
    }

    // ---------------- the Nightmare and Phosani

    /** Wakes her (her shield phase) at phase {@code phase} (0-2) with only standard attacks of her own. */
    public static void nightmareWake(DataBossEntity boss, int phase) {
        NightmareScript n = script(boss, NightmareScript.class);
        n.selfTestQuiet = true;
        n.wake(boss);
        n.phase = phase;
    }

    /** One of her standard attacks now (0 melee, 1 magic, 2 ranged). */
    public static AttackDef nightmareStandard(DataBossEntity boss, ServerPlayer target, int style) {
        NightmareScript n = script(boss, NightmareScript.class);
        AttackDef a = n.phosani() ? (style == 0 ? MinigameBosses.PHOSANI_MELEE : style == 1 ? MinigameBosses.PHOSANI_MAGIC
                : MinigameBosses.PHOSANI_RANGED) : (style == 0 ? MinigameBosses.NIGHTMARE_MELEE : style == 1
                ? MinigameBosses.NIGHTMARE_MAGIC : MinigameBosses.NIGHTMARE_RANGED);
        n.perform(boss, target, a);
        return a;
    }

    /** Sees each of her standard attacks wind up (null: stop). */
    public static void nightmareWindUps(WindUpListener listener) {
        NightmareScript.windUpListener = listener == null ? null : listener::windUp;
    }

    public interface WindUpListener {
        void windUp(AttackDef attack, long createdMc, long dueMc);
    }

    // ---------------- the Phantom Muspah

    /** Marks a spike under the player now. */
    public static void muspahSpikeUnder(DataBossEntity boss, ServerPlayer target) {
        script(boss, MuspahScript.class).markSpikes(boss, target, List.of());
    }

    /** Whether a tile's spike is still about to erupt, and whether it stands. */
    public static boolean muspahErupting(DataBossEntity boss, int x, int z) {
        return script(boss, MuspahScript.class).erupting.containsKey(com.runecraft.osrs.content.bosses.BossMechanics.tile(x, z));
    }

    public static boolean muspahSpike(DataBossEntity boss, int x, int z) {
        return script(boss, MuspahScript.class).spikes.contains(com.runecraft.osrs.content.bosses.BossMechanics.tile(x, z));
    }

    // ---------------- the Moons of Peril

    /** Where the lit glyph stands now, or null during a special that lets the player stand anywhere. */
    public static Vec3 moonGlyph(DataBossEntity boss) {
        MoonScripts.Moon m = script(boss, MoonScripts.Moon.class);
        return m.glyphActive() ? m.glyphPos(boss) : null;
    }

    // ---------------- Zulrah

    /** The Jad phase's two attacks (magic, ranged), cued like TzTok-Jad's. */
    public static AttackDef[] zulrahJadAttacks() {
        return new AttackDef[]{ZulrahScript.JAD_MAGIC, ZulrahScript.JAD_RANGED};
    }
}
