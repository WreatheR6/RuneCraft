package com.runecraft.combat;

import com.runecraft.action.ActionManager;
import com.runecraft.network.Net;
import com.runecraft.osrs.CombatBonuses;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.ItemDef;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModItems;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.platform.PacketByteBufs;
import com.runecraft.platform.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OSRS auto-retaliate (Combat Options tab, on by default): when something hits a player who isn't already
 * fighting, the player fights back automatically - melee swings at the weapon's attack speed while the attacker is
 * in reach, or the autocast spell / powered staff when wielding a staff. It stops when the player walks away, starts
 * another action, attacks something else or the attacker dies. Like OSRS, being attacked interrupts skilling.
 * (Bows need the player to draw them, so ranged weapons don't retaliate on their own.)
 */
public final class AutoRetaliate {
    private record Target(int entityId, Vec3 anchor, double anchorDistance) {
    }

    private static final Map<UUID, Target> TARGETS = new ConcurrentHashMap<>();
    /** Last combat state sent to each client: bit 0 auto-retaliate, bit 1 in a multi area. */
    private static final Map<UUID, Integer> SENT = new ConcurrentHashMap<>();
    private static final double MAGIC_RANGE = 16.0;

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
        long speed = Equipment.weaponSpeed(player, data.skills());
        if (ActionManager.mcTick() - data.lastAttackTick < (speed + 2) * ActionManager.MC_TICKS_PER_GAME_TICK) {
            return;
        }
        ActionManager.cancel(player);
        TARGETS.put(player.getUUID(), new Target(attacker.getId(), player.position(), player.distanceTo(attacker)));
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

    public static void forget(UUID player) {
        TARGETS.remove(player);
        SENT.remove(player);
    }

    /** Every game tick for each player. */
    public static void gameTick(ServerPlayer player, long gameTick) {
        if (gameTick % 5 == 0) {
            sendState(player, false);
        }
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
        Vec3 pos = player.position();
        double dx = pos.x - t.anchor().x;
        double dz = pos.z - t.anchor().z;
        if (dx * dx + dz * dz > 1.5 * 1.5) {
            double dist = player.distanceTo(target);
            if (dist >= t.anchorDistance()) {
                // Walked away: retaliation ends, as clicking elsewhere does in OSRS.
                TARGETS.remove(player.getUUID());
                return;
            }
            t = new Target(t.entityId(), pos, dist);
            TARGETS.put(player.getUUID(), t);
        }
        swing(player, data, target);
    }

    private static void swing(ServerPlayer player, PlayerData data, LivingEntity target) {
        long now = ActionManager.mcTick();
        int speed = Equipment.weaponSpeed(player, data.skills());
        ItemDef def = ModItems.def(player.getMainHandItem());
        EquipmentDef weapon = def == null ? null : def.equipment();
        String weaponId = Inv.idOf(player.getMainHandItem());
        boolean staff = def != null && (def.kind() == ItemDef.Kind.STAFF
                || weapon != null && weapon.weaponClass() == EquipmentDef.WeaponClass.STAFF);
        boolean powered = CombatBonuses.poweredStaffMaxHit(weaponId, 1) >= 0;
        if (staff || powered) {
            if (player.distanceToSqr(target) > MAGIC_RANGE * MAGIC_RANGE) {
                return;
            }
            if (powered || data.autocast() != null) {
                MagicService.autocastAttack(player, target);
            } else {
                com.runecraft.content.systems.SpellService.retaliate(player, target);
            }
            return;
        }
        if (weapon != null && weapon.weaponClass() == EquipmentDef.WeaponClass.BOW) {
            return;
        }
        if (now - data.lastAttackTick < (long) speed * ActionManager.MC_TICKS_PER_GAME_TICK) {
            return;
        }
        double reach = 3.0 + target.getBbWidth() / 2.0;
        if (player.distanceToSqr(target) > reach * reach || !player.hasLineOfSight(target)) {
            return;
        }
        player.swing(InteractionHand.MAIN_HAND, true);
        player.attack(target);
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
