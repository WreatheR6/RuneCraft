package com.runecraft.combat;

import com.runecraft.RuneCraft;
import com.runecraft.action.ActionManager;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.PrayerGrace;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * The late side of the protection switch grace (round 42), in the one place every monster hit lands: CombatHandler.npcAttack
 * (built-in monsters, Jad, Graardor's slam) and Mechanics' AttackDef hits (module monsters and data bosses).
 *
 * <p>// playability: OSRS reads the overhead prayer on the exact tick a hit resolves. A real-time player who presses the right
 * protection a hair after the impact (key, network and server-tick latency) would lose the hit. A monster hit that lands while
 * its protection doesn't count, on a player who is switching protections and could switch this one on
 * ({@link PrayerGrace#holdLate}), is held for up to {@link PrayerGrace#LATE_MC} Minecraft ticks (0.2 s) and judged again: the
 * moment the right protection counts it resolves blocked, as an on-time hit would; otherwise it lands unchanged when the window
 * ends. Its roll, max hit and block amount are those of an on-time hit; a player who prays nothing is never held up. Together
 * with the 8 MC back grace ({@link PrayerGrace#protectedWithGrace}) a switch within 0.6 s of the impact (one OSRS game tick)
 * decides the hit, on either side.
 */
public final class LateProtection {
    /** One held hit. */
    private record Held(LivingEntity source, ServerPlayer target, AttackType style, ServerLevel level, long heldAt, Runnable resolve) {
    }

    /** Self-test recorder: hears each hit held and how it resolved ({@code blocked}: the protection counted by then). */
    public interface Listener {
        default void held(ServerPlayer target, AttackType style, long mc) {
        }

        default void resolved(ServerPlayer target, AttackType style, long heldAt, long mc, boolean blocked) {
        }
    }

    public static Listener listener = new Listener() {
    };

    private static final List<Held> HELD = new ArrayList<>();
    /**
     * A held hit not judged within this many MC was never ticked (the server judges every hit within {@link PrayerGrace#LATE_MC}
     * MC): a self-test that stopped advancing time left it behind, and it is dropped instead of landing on a later check.
     */
    private static final long STALE_MC = PrayerGrace.LATE_MC + 2L * ActionManager.MC_TICKS_PER_GAME_TICK;

    private LateProtection() {
    }

    /**
     * Lands a monster hit on {@code target}: returns false when it should resolve now (the caller runs it), or true when it was
     * held for the late window; {@code resolve} then runs within {@link PrayerGrace#LATE_MC} ticks (in the attacking monster's
     * resolving context, so the attacks its on-hit hook starts land with it), and reads the protection itself.
     */
    public static boolean hold(LivingEntity source, ServerPlayer target, AttackType style, Runnable resolve) {
        if (style == null || target == null || !target.isAlive()) {
            return false;
        }
        PlayerData data = Players.data(target);
        if (CombatHandler.isProtected(data, style)) {
            return false; // blocked on time: nothing to wait for
        }
        long now = ActionManager.mcTick();
        boolean anyOn = false;
        for (Prayer p : PrayerGrace.PROTECTIONS) {
            anyOn |= data.isActive(p);
        }
        if (!PrayerGrace.holdLate(false, PrayerService.canSwitchOn(target, PrayerGrace.protectionFor(style)), anyOn,
                PrayerGrace.lastSwitch(data.overheadOnAt, data.overheadOffAt), now)) {
            return false;
        }
        HELD.add(new Held(source, target, style, (ServerLevel) target.level(), now, com.runecraft.boss.Mechanics.inResolvingContext(resolve)));
        listener.held(target, style, now);
        return true;
    }

    /**
     * Every Minecraft tick (from Mechanics' tick, so the self-tests' {@code Mechanics.tickForSelfTest} drives it too): resolves
     * each held hit whose protection now counts or whose window ended; drops one whose target died, left or changed level, or
     * whose attacker was removed from the world (a monster that died a moment ago still lands it, as a projectile in flight does).
     */
    public static void tick() {
        if (HELD.isEmpty()) {
            return;
        }
        long now = ActionManager.mcTick();
        for (Held h : HELD.toArray(new Held[0])) {
            if (!h.target().isAlive() || h.target().isRemoved() || h.target().level() != h.level()
                    || h.source() != null && h.source().isRemoved() && h.source().getRemovalReason() != net.minecraft.world.entity.Entity.RemovalReason.KILLED) {
                HELD.remove(h);
                continue;
            }
            if (now - h.heldAt() > STALE_MC) {
                HELD.remove(h); // nobody ticked it for two game ticks (a self-test that stopped driving time): not a live hit
                continue;
            }
            boolean protectedNow = CombatHandler.isProtected(Players.data(h.target()), h.style());
            if (!PrayerGrace.lateDue(protectedNow, h.heldAt(), now)) {
                continue;
            }
            HELD.remove(h);
            listener.resolved(h.target(), h.style(), h.heldAt(), now, protectedNow);
            try {
                h.resolve().run();
            } catch (RuntimeException e) {
                RuneCraft.LOGGER.error("RuneCraft: a held monster hit on {} failed", h.target().getName().getString(), e);
            }
        }
    }

    /** Hits held on a player right now (self-tests). */
    public static int heldOn(ServerPlayer target) {
        int n = 0;
        for (Held h : HELD) {
            if (h.target() == target) {
                n++;
            }
        }
        return n;
    }

    /** Self-test hook: lands every hit held on {@code target} now, as its window ending would. */
    public static void flush(ServerPlayer target) {
        for (Held h : HELD.toArray(new Held[0])) {
            if (h.target() == target) {
                HELD.remove(h);
                h.resolve().run();
            }
        }
    }
}
