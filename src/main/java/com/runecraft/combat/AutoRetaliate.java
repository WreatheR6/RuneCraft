package com.runecraft.combat;

import com.runecraft.action.ActionManager;
import com.runecraft.network.Net;
import com.runecraft.osrs.AttackClock;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Msg;
import com.runecraft.platform.PacketByteBufs;
import com.runecraft.platform.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OSRS auto-retaliate (Combat Options tab, on by default): when something hits a player who isn't already
 * fighting, the player fights back automatically - melee while the attacker is in reach, the autocast spell / powered
 * staff when wielding a casting staff, a bow shot when wielding a bow. It stops when the player walks away, starts
 * another action, attacks something else or the attacker dies. Like OSRS, being attacked interrupts skilling.
 * <p>Round 41 ({@code // playability}): it runs on the player's {@link AttackTimer} clock every MC tick and resolves
 * through the same resolvers as a click (never vanilla's Player.attack), so the timer HUD follows it and a manual click on
 * the same target merges into the same attack instead of a second cooldown. OSRS turns the player to face the attacker;
 * here it only swings at a target within {@value AttackClock#RETALIATE_FACING_DEGREES} degrees of the look direction, and
 * "walked away" means out of reach (plus a block) for more than {@value AttackClock#WALK_AWAY_MC} MC ticks.
 */
public final class AutoRetaliate {
    /** The retaliation target, and since when (MC tick) it has been out of reach; -1 while in reach. */
    private record Target(int entityId, long outsideSince) {
    }

    private static final Map<UUID, Target> TARGETS = new ConcurrentHashMap<>();
    /** Last combat state sent to each client: bit 0 auto-retaliate, bit 1 in a multi area. */
    private static final Map<UUID, Integer> SENT = new ConcurrentHashMap<>();
    private static final double MAGIC_RANGE = 16.0;
    /** Bow retaliation range in blocks (OSRS shortbows reach 7 tiles, longbows 9-10). // approx */
    private static final double BOW_RANGE = 12.0;
    /** "Already fighting by hand" lasts the attack period plus 2 game ticks after the player's own last attack. */
    private static final int BUSY_FIGHTING_MC = 2 * ActionManager.MC_TICKS_PER_GAME_TICK;

    private AutoRetaliate() {
    }

    public static void toggle(ServerPlayer player) {
        PlayerData data = Players.data(player);
        data.setAutoRetaliate(!data.autoRetaliate());
        if (!data.autoRetaliate()) {
            TARGETS.remove(player.getUUID());
        }
        Msg.action(player, "Auto Retaliate: " + (data.autoRetaliate() ? "On" : "Off"));
        sendState(player, true);
    }

    /** A player was hit by {@code attacker}. */
    public static void onHurt(ServerPlayer player, LivingEntity attacker) {
        PlayerData data = Players.data(player);
        if (attacker == null || attacker == player || !attacker.isAlive() || !data.autoRetaliate() || player.isSpectator()
                || player.isCreative() || player.isDeadOrDying()) {
            return;
        }
        if (attacker instanceof ServerPlayer other && !CombatHandler.canAttackPlayer(player, other)) {
            return;
        }
        Target current = TARGETS.get(player.getUUID());
        if (current != null && player.level().getEntity(current.entityId()) instanceof LivingEntity l && l.isAlive()) {
            return;
        }
        // Already fighting by hand: keep doing that.
        if (AttackTimer.recentlyAttacked(player, BUSY_FIGHTING_MC)) {
            return;
        }
        ActionManager.cancel(player);
        TARGETS.put(player.getUUID(), new Target(attacker.getId(), -1));
    }

    /** The player hit something; attacking anything other than the retaliation target ends retaliation. */
    public static void onPlayerAttack(ServerPlayer player, LivingEntity target) {
        Target t = TARGETS.get(player.getUUID());
        if (t != null && t.entityId() != target.getId()) {
            TARGETS.remove(player.getUUID());
        }
    }

    public static void stop(ServerPlayer player) {
        TARGETS.remove(player.getUUID());
    }

    /** True while the player is retaliating against {@code target}. */
    public static boolean retaliating(ServerPlayer player, LivingEntity target) {
        Target t = TARGETS.get(player.getUUID());
        return t != null && target != null && t.entityId() == target.getId();
    }

    public static void forget(UUID player) {
        TARGETS.remove(player);
        SENT.remove(player);
    }

    /** Every game tick for each player: the Combat Options state for the client. */
    public static void gameTick(ServerPlayer player, long gameTick) {
        if (gameTick % 5 == 0) {
            sendState(player, false);
        }
    }

    /** Every MC tick for each player ({@code PlayerEvents.everyMcTick}, {@link AttackTimer#tickForSelfTest}). */
    public static void mcTick(ServerPlayer player) {
        Target t = TARGETS.get(player.getUUID());
        if (t == null) {
            return;
        }
        PlayerData data = Players.data(player);
        Entity e = player.level().getEntity(t.entityId());
        if (!(e instanceof LivingEntity target) || !target.isAlive() || player.isDeadOrDying() || !data.autoRetaliate()
                || ActionManager.isBusy(player) || player.distanceToSqr(target) > 32 * 32) {
            TARGETS.remove(player.getUUID());
            return;
        }
        AttackClock.Kind kind = AttackTimer.kindFor(player);
        long now = ActionManager.mcTick();
        if (!inReach(player, target, kind, 1.0)) {
            if (t.outsideSince() < 0) {
                TARGETS.put(player.getUUID(), new Target(t.entityId(), now));
            } else if (now - t.outsideSince() > AttackClock.WALK_AWAY_MC) {
                TARGETS.remove(player.getUUID()); // walked away: retaliation ends, as clicking elsewhere does in OSRS
            }
            return;
        }
        if (t.outsideSince() >= 0) {
            TARGETS.put(player.getUUID(), new Target(t.entityId(), -1));
        }
        if (!AttackTimer.ready(player) || AttackTimer.hasPending(player) || !inReach(player, target, kind, 0)) {
            return;
        }
        Vec3 look = player.getLookAngle();
        if (!AttackClock.facing(look.x, look.z, target.getX() - player.getX(), target.getZ() - player.getZ(),
                AttackClock.RETALIATE_FACING_DEGREES)) {
            return;
        }
        if (kind == AttackClock.Kind.MELEE ? !player.hasLineOfSight(target) : !AttackTimer.clearShot(player, target)) {
            return;
        }
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        if (kind == AttackClock.Kind.MELEE && !com.runecraft.world.Gielinor.is(player)) {
            // Outside Gielinor combat stays vanilla (CombatHandler only adds OSRS XP): the old vanilla swing, on the clock.
            AttackTimer.accept(player, kind, AttackClock.periodMc(Equipment.weaponSpeed(player, data.skills())));
            player.attack(target);
            return;
        }
        AttackTimer.resolve(player, target, kind, AttackTimer.resolver(kind));
    }

    /** Whether the target is within this kind of attack's reach, plus {@code slack} blocks. */
    private static boolean inReach(ServerPlayer player, LivingEntity target, AttackClock.Kind kind, double slack) {
        return switch (kind) {
            case SPELL -> player.distanceTo(target) <= MAGIC_RANGE + slack;
            case RANGED, THROWN -> player.distanceTo(target) <= BOW_RANGE + slack;
            case MELEE -> player.canInteractWithEntity(target, 0.5 + slack);
        };
    }

    /** Sends the Combat Options state (auto-retaliate, multi-combat area) when it changed or {@code force}. */
    public static void sendState(ServerPlayer player, boolean force) {
        int state = (Players.data(player).autoRetaliate() ? 1 : 0) | (CombatZones.isMulti(player)
                && com.runecraft.world.Gielinor.is(player) ? 2 : 0);
        Integer last = SENT.put(player.getUUID(), state);
        if (!force && last != null && last == state) {
            return;
        }
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean((state & 1) != 0);
        buf.writeBoolean((state & 2) != 0);
        ServerPlayNetworking.send(player, Net.COMBAT_STATE, buf);
    }
}
