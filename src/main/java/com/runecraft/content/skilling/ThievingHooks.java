package com.runecraft.content.skilling;

import com.runecraft.combat.CombatHandler;
import com.runecraft.content.BlockHooks;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.Chance;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.skilling.ThievingData;
import com.runecraft.quest.QuestScripts;
import com.runecraft.skill.Stun;
import com.runecraft.skill.XpService;
import com.runecraft.util.Msg;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

/** Pickpocketing module NPCs, thieving chests and extra stalls. */
final class ThievingHooks {
    private ThievingHooks() {
    }

    static void init() {
        for (ThievingData.Target t : ThievingData.TARGETS) {
            QuestScripts.onTalk(t.npcId(), (player, npc) -> pickpocket(player, npc, t));
        }
        for (ThievingData.Container c : ThievingData.CONTAINERS) {
            BlockHooks.register(c.blockId(), (player, pos, state) -> steal(player, pos, c));
        }
    }

    private static void pickpocket(ServerPlayerEntity player, QuestNpcEntity npc, ThievingData.Target t) {
        if (!Stun.check(player)) {
            return;
        }
        if (!SkillingUtil.need(player, Skill.THIEVING, t.level(), "pickpocket the " + SkillingUtil.lower(t.name()))) {
            return;
        }
        Msg.action(player, "You attempt to pick the " + SkillingUtil.lower(t.name()) + "'s pocket.");
        player.swingHand(Hand.MAIN_HAND, true);
        int level = SkillingUtil.level(player, Skill.THIEVING);
        if (player.getRandom().nextDouble() < Chance.lerp(level, t.low(), t.high())) {
            SkillingUtil.giveAll(player, t.always(), player.getRandom());
            SkillingUtil.roll(player, t.table(), player.getRandom());
            XpService.add(player, Skill.THIEVING, t.xp());
            Msg.action(player, "You pick the " + SkillingUtil.lower(t.name()) + "'s pocket.");
            ServerNetworking.sync(player);
        } else {
            Msg.game(player, "You fail to pick the " + SkillingUtil.lower(t.name()) + "'s pocket.");
            Msg.red(player, "You have been stunned!");
            npc.swingHand(Hand.MAIN_HAND);
            CombatHandler.applyDamage(npc, player, 1 + player.getRandom().nextInt(t.stunDamage()));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 4, false, false));
            Stun.apply(player, 8);
        }
    }

    private static void steal(ServerPlayerEntity player, BlockPos pos, ThievingData.Container c) {
        if (!Stun.check(player)) {
            return;
        }
        ServerWorld world = (ServerWorld) player.getWorld();
        if (GatheringHooks.depleted(world, pos)) {
            Msg.game(player, c.chest() ? "The chest is empty." : "The stall is empty.");
            return;
        }
        if (!SkillingUtil.need(player, Skill.THIEVING, c.level(), c.chest() ? "open this chest" : "steal from the "
                + SkillingUtil.lower(c.name()))) {
            return;
        }
        player.swingHand(Hand.MAIN_HAND, true);
        if (c.chest()) {
            Msg.action(player, "You search the chest for traps... You find a trap on the chest and disable it.");
            world.playSound(null, pos, SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 1f, 1f);
        }
        SkillingUtil.giveAll(player, c.always(), player.getRandom());
        SkillingUtil.roll(player, c.table(), player.getRandom());
        XpService.add(player, Skill.THIEVING, c.xp());
        Msg.action(player, c.chest() ? "You find treasure inside!" : "You steal from the " + SkillingUtil.lower(c.name()) + ".");
        GatheringHooks.deplete(world, pos, c.respawnTicks());
        ServerNetworking.sync(player);
    }
}
