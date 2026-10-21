package com.runecraft.mixin;

import com.runecraft.combat.FreezeRules;
import java.util.Set;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.RelativeMovement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tells {@link FreezeRules} when the server itself moves a player, so a frozen player's held spot follows a teleport (OSRS
 * allows teleporting while frozen) but never a client's own move, however long (round 42). Every server teleport of a
 * player, whatever started it ({@code ServerPlayer.teleportTo}, commands, pearls, chorus fruit, RuneCraft's teleports and
 * boss scripts), ends in {@code teleport(DDDFFSet)}; vanilla also calls it from {@code handleMovePlayer} to snap a bad client
 * move back (moved too quickly / wrongly) or to resend a pending teleport, which must not count, so the move packet is
 * bracketed. Only the server thread's calls count: {@code handleMovePlayer} first runs on the network thread, where it hands
 * itself over to the server thread.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void runecraft$beginClientMove(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        FreezeRules.clientMove(((ServerGamePacketListenerImpl) (Object) this).player, true);
    }

    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void runecraft$endClientMove(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        FreezeRules.clientMove(((ServerGamePacketListenerImpl) (Object) this).player, false);
    }

    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"))
    private void runecraft$serverTeleport(double x, double y, double z, float yRot, float xRot, Set<RelativeMovement> relative,
            CallbackInfo ci) {
        FreezeRules.teleporting(((ServerGamePacketListenerImpl) (Object) this).player);
    }
}
