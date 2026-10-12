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
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

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
    private record Target(int entityId, Vec3d anchor, double anchorDistance) {
    }

    private static final Map<UUID, Target> TARGETS = new ConcurrentHashMap<>();
    /** Last combat state sent to each client: bit 0 auto-retaliate, bit 1 in a multi area. */
    private static final Map<UUID, Integer> SENT = new ConcurrentHashMap<>();
    private static final double MAGIC_RANGE = 16.0;

    private AutoRetaliate() {
    }

    public static void toggle(ServerPlayerEntity player) {
        PlayerData data = Players.data(player);
        data.setAutoRetaliate(!data.autoRetaliate());
        if (!data.autoRetaliate()) {
            TARGETS.remove(player.getUuid());
        }
        Msg.action(player, "Auto Retaliate: " + (data.autoRetaliate() ? "On" : "Off"));
        sendState(player, true);
    }

    /** A player was hit by {@code attacker}. */
    public static void onHurt(ServerPlayerEntity player, LivingEntity attacker) {
        PlayerData data = Players.data(player);
        if (attacker == null || attacker == player || !attacker.isAlive() || !data.autoRetaliate() || player.isSpectator()
                || player.isCreative() || player.isDead()) {
            return;
        }
        if (attacker instanceof ServerPlayerEntity other && !CombatHandler.canAttackPlayer(player, other)) {
            return;
        }
        Target current = TARGETS.get(player.getUuid());
        if (current != null && player.getWorld().getEntityById(current.entityId()) instanceof LivingEntity l && l.isAlive()) {
            return;
        }
        // Already fighting by hand: keep doing that.
        long speed = Equipment.weaponSpeed(player, data.skills());
        if (ActionManager.mcTick() - data.lastAttackTick < (speed + 2) * ActionManager.MC_TICKS_PER_GAME_TICK) {
            return;
        }
        ActionManager.cancel(player);
        TARGETS.put(player.getUuid(), new Target(attacker.getId(), player.getPos(), player.distanceTo(attacker)));
    }

    /** The player hit something; attacking anything other than the retaliation target ends retaliation. */
    public static void onPlayerAttack(ServerPlayerEntity player, LivingEntity target) {
        Target t = TARGETS.get(player.getUuid());
        if (t != null && t.entityId() != target.getId()) {
            TARGETS.remove(player.getUuid());
        }
    }

    public static void stop(ServerPlayerEntity player) {
        TARGETS.remove(player.getUuid());
    }

    public static void forget(UUID player) {
        TARGETS.remove(player);
        SENT.remove(player);
    }

    /** Every game tick for each player. */
    public static void gameTick(ServerPlayerEntity player, long gameTick) {
        if (gameTick % 5 == 0) {
            sendState(player, false);
        }
        Target t = TARGETS.get(player.getUuid());
        if (t == null) {
            return;
        }
        PlayerData data = Players.data(player);
        Entity e = player.getWorld().getEntityById(t.entityId());
        if (!(e instanceof LivingEntity target) || !target.isAlive() || player.isDead() || !data.autoRetaliate()
                || ActionManager.isBusy(player) || player.squaredDistanceTo(target) > 32 * 32) {
            TARGETS.remove(player.getUuid());
            return;
        }
        Vec3d pos = player.getPos();
        double dx = pos.x - t.anchor().x;
        double dz = pos.z - t.anchor().z;
        if (dx * dx + dz * dz > 1.5 * 1.5) {
            double dist = player.distanceTo(target);
            if (dist >= t.anchorDistance()) {
                // Walked away: retaliation ends, as clicking elsewhere does in OSRS.
                TARGETS.remove(player.getUuid());
                return;
            }
            t = new Target(t.entityId(), pos, dist);
            TARGETS.put(player.getUuid(), t);
        }
        swing(player, data, target);
    }

    private static void swing(ServerPlayerEntity player, PlayerData data, LivingEntity target) {
        long now = ActionManager.mcTick();
        int speed = Equipment.weaponSpeed(player, data.skills());
        ItemDef def = ModItems.def(player.getMainHandStack());
        EquipmentDef weapon = def == null ? null : def.equipment();
        String weaponId = Inv.idOf(player.getMainHandStack());
        boolean staff = def != null && (def.kind() == ItemDef.Kind.STAFF
                || weapon != null && weapon.weaponClass() == EquipmentDef.WeaponClass.STAFF);
        boolean powered = CombatBonuses.poweredStaffMaxHit(weaponId, 1) >= 0;
        if (staff || powered) {
            if (player.squaredDistanceTo(target) > MAGIC_RANGE * MAGIC_RANGE) {
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
        double reach = 3.0 + target.getWidth() / 2.0;
        if (player.squaredDistanceTo(target) > reach * reach || !player.canSee(target)) {
            return;
        }
        player.swingHand(Hand.MAIN_HAND, true);
        player.attack(target);
    }

    /** Sends the Combat Options state (auto-retaliate, multi-combat area) when it changed or {@code force}. */
    public static void sendState(ServerPlayerEntity player, boolean force) {
        int state = (Players.data(player).autoRetaliate() ? 1 : 0) | (CombatZones.isMulti(player)
                && com.runecraft.world.Gielinor.is(player) ? 2 : 0);
        Integer last = SENT.put(player.getUuid(), state);
        if (!force && last != null && last == state) {
            return;
        }
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean((state & 1) != 0);
        buf.writeBoolean((state & 2) != 0);
        ServerPlayNetworking.send(player, Net.COMBAT_STATE, buf);
    }
}
