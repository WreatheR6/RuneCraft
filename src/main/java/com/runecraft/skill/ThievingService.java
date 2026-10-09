package com.runecraft.skill;

import com.runecraft.block.StallBlock;
import com.runecraft.collection.CollectionLog;
import com.runecraft.osrs.Chance;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.data.Thieving;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/** Pickpocketing and stealing from stalls. */
public final class ThievingService {
    private ThievingService() {
    }

    public static void stealFromStall(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state, StallBlock block) {
        if (!Stun.check(player)) {
            return;
        }
        Thieving.Stall stall = block.stall();
        if (state.get(StallBlock.EMPTY)) {
            Msg.game(player, "The stall is empty.");
            return;
        }
        if (Players.data(player).skills().currentLevel(Skill.THIEVING) < stall.level()) {
            Msg.game(player, "You need a Thieving level of " + stall.level() + " to steal from the " + stall.displayName() + ".");
            return;
        }
        List<Thieving.Loot> loot = stall.loot();
        Thieving.Loot l = loot.get(world.getRandom().nextInt(loot.size()));
        player.swingHand(Hand.MAIN_HAND, true);
        Inv.give(player, l.item(), l.min());
        CollectionLog.obtain(player, l.item());
        XpService.add(player, Skill.THIEVING, stall.xp());
        Msg.action(player, "You steal from the " + stall.displayName() + ".");
        block.empty(world, pos, state);
    }

    public static void pickpocket(ServerPlayerEntity player, LivingEntity target, Thieving.Pickpocket kind) {
        if (!Stun.check(player)) {
            return;
        }
        int level = Players.data(player).skills().currentLevel(Skill.THIEVING);
        if (level < kind.level()) {
            Msg.game(player, "You need a Thieving level of " + kind.level() + " to pickpocket the " + kind.displayName() + ".");
            return;
        }
        Msg.action(player, "You attempt to pick the " + kind.displayName() + "'s pocket.");
        player.swingHand(Hand.MAIN_HAND, true);
        if (player.getRandom().nextDouble() < Chance.lerp(level, kind.low(), kind.high())) {
            for (Thieving.Loot l : kind.loot()) {
                if (l.weight() <= 1 || player.getRandom().nextInt(l.weight()) == 0) {
                    int n = l.min() + (l.max() > l.min() ? player.getRandom().nextInt(l.max() - l.min() + 1) : 0);
                    Inv.give(player, l.item(), n);
                }
            }
            XpService.add(player, Skill.THIEVING, kind.xp());
            Msg.action(player, "You pick the " + kind.displayName() + "'s pocket.");
            com.runecraft.network.ServerNetworking.sync(player);
        } else {
            Msg.game(player, "You fail to pick the " + kind.displayName() + "'s pocket.");
            Msg.red(player, "You have been stunned!");
            int damage = 1 + player.getRandom().nextInt(kind.stunDamage());
            target.swingHand(Hand.MAIN_HAND);
            com.runecraft.combat.CombatHandler.applyDamage(target, player, damage);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 4, false, false));
            Stun.apply(player, 8);
        }
    }
}
