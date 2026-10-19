package com.runecraft.content.systems;

import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.systems.Libation;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.platform.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;

/**
 * Blessed bone shard sacrifices ({@link Libation}): right-click the shards next to an altar with jugs of wine to offer up
 * to 100 of them for 5 Prayer XP each, draining 2 prayer points. The poured wine's remaining allowance is a per-player
 * charge.
 */
public final class PrayerOfferings {
    private PrayerOfferings() {
    }

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.isEmpty() || !Libation.SHARDS.equals(Inv.idOf(stack))) {
                return InteractionResultHolder.pass(stack);
            }
            if (world.isClientSide()) {
                return InteractionResultHolder.sidedSuccess(stack, true);
            }
            if (player instanceof ServerPlayer sp) {
                if (!nearAltar(sp)) {
                    Msg.game(sp, "You need to be at an altar to sacrifice blessed bone shards.");
                } else {
                    sacrifice(sp);
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, false);
        });
    }

    private static boolean nearAltar(ServerPlayer p) {
        BlockPos at = p.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-4, -2, -4), at.offset(4, 2, 4))) {
            if (BuiltInRegistries.BLOCK.getKey(p.level().getBlockState(pos).getBlock()).getPath().contains("altar")) {
                return true;
            }
        }
        return false;
    }

    /** One sacrifice action; returns the shards offered (0 if refused). */
    public static int sacrifice(ServerPlayer p) {
        PlayerData data = Players.data(p);
        if (data.skills().level(Skill.PRAYER) < Libation.LEVEL) {
            Msg.game(p, "You need a Prayer level of " + Libation.LEVEL + " to sacrifice blessed bone shards.");
            return 0;
        }
        if (data.prayerPoints() < Libation.MIN_PRAYER_POINTS) {
            Msg.game(p, "You need at least " + Libation.MIN_PRAYER_POINTS + " prayer points to make a sacrifice.");
            return 0;
        }
        SysState.SysPlayer sp = SysState.of(p);
        int allowance = sp.playerCharges.getOrDefault(Libation.CHARGE_KEY, 0);
        Libation.Offer offer = Libation.offer(Inv.count(p, Libation.SHARDS), allowance, Inv.count(p, Libation.WINE));
        if (offer.shards() <= 0) {
            Msg.game(p, "You need a jug of wine to pour into the bowl before you can sacrifice any shards.");
            return 0;
        }
        if (offer.jugsUsed() > 0) {
            Inv.remove(p, Libation.WINE, offer.jugsUsed());
            Inv.give(p, "jug", offer.jugsUsed());
        }
        Inv.remove(p, Libation.SHARDS, offer.shards());
        sp.playerCharges.put(Libation.CHARGE_KEY, offer.allowanceLeft());
        data.setPrayerPoints(data.prayerPoints() - Libation.PRAYER_COST);
        XpService.add(p, Skill.PRAYER, offer.xp());
        ServerNetworking.sync(p);
        Msg.action(p, "You sacrifice " + offer.shards() + " blessed bone shards. (" + offer.allowanceLeft() + " more can be offered before the bowl needs more wine.)");
        return offer.shards();
    }
}
