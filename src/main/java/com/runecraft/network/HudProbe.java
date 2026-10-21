package com.runecraft.network;

import java.util.List;

/**
 * What the local client's combat HUD shows right now, published by the client once a client tick (round 42) so a real-player
 * self-test running on the integrated server can check the screen, not just the packets. Stays null on a dedicated server
 * and before the client has drawn anything. Read-only snapshots: the client thread replaces the whole record.
 */
public final class HudProbe {
    private HudProbe() {
    }

    /** One cast-bar entry: source entity, the name shown, style ({@code osrs.CastBar}), the key text, the frame and time left. */
    public record Cast(int source, String name, int style, int protection, String key, Boolean rightOverhead, float secondsLeft) {
    }

    /**
     * The combat HUD: the cast bars (soonest first), the opponent info box (entity -1 when hidden), the auto-retaliate hint
     * (empty when hidden), the HP orb tint (0 none, 1 poison, 2 venom), the protection keys' curse map, the bound protection
     * keys, the quick-eat key, whether the "Special ready" cue is showing, and the client tick it was taken.
     */
    public record Snapshot(List<Cast> casts, int opponent, String opponentName, int opponentLevel, int opponentHp,
            int opponentMaxHp, String retaliating, int poison, int[] curse, String[] protectionKeys, boolean specialReady,
            long clientTick) {
    }

    /** The newest snapshot (null when no client draws one). */
    public static volatile Snapshot latest;
}
