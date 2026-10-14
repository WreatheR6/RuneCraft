package com.runecraft.content.skilling;

import com.runecraft.content.BlockHooks;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.data.Thieving;
import com.runecraft.osrs.content.skilling.Loot;
import com.runecraft.skill.ThievingService;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.skilling.ThievingData;
import com.runecraft.quest.QuestScripts;
import com.runecraft.skill.Stun;
import com.runecraft.skill.XpService;
import com.runecraft.util.Msg;
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
        QuestScripts.onTalk(ThievingData.BLACKJACK_SELLER, (player, npc) -> blackjackSeller(player));
    }

    /** Knocked-out blackjacking targets: entity -> {game tick they wake, pickpockets left}. */
    private static final java.util.Map<java.util.UUID, long[]> KNOCKED_OUT = new java.util.HashMap<>();

    /** The blackjack seller in Pollnivneach: sells an ordinary willow blackjack once The Feud is done. */
    private static void blackjackSeller(ServerPlayerEntity player) {
        if (!com.runecraft.quest.QuestApi.isComplete(player, "the_feud")) {
            com.runecraft.quest.QuestApi.say(player, "Blackjack seller", "Blackjacks? Never heard of 'em. Maybe ask around town first.");
            return;
        }
        int price = 192; // approx.
        if (!com.runecraft.util.Inv.has(player, com.runecraft.util.Inv.COINS, price)) {
            com.runecraft.quest.QuestApi.say(player, "Blackjack seller", "A willow blackjack is " + price + " coins. Come back when you have it.");
            return;
        }
        com.runecraft.util.Inv.remove(player, com.runecraft.util.Inv.COINS, price);
        SkillingUtil.give(player, "willow_blackjack", 1);
        com.runecraft.quest.QuestApi.say(player, "Blackjack seller", "One willow blackjack. Knock 'em out, then help yourself to their pockets.");
        ServerNetworking.sync(player);
    }

    private static void pickpocket(ServerPlayerEntity player, QuestNpcEntity npc, ThievingData.Target t) {
        long now = com.runecraft.action.ActionManager.gameTick();
        long[] ko = KNOCKED_OUT.get(npc.getUuid());
        boolean unconscious = ko != null && ko[0] > now && ko[1] > 0;
        if (ko != null && !unconscious) {
            KNOCKED_OUT.remove(npc.getUuid());
        }
        // Blackjacking: with a blackjack in hand, knock the target out first; while it is out cold
        // (two pickpockets) the pickpocket always succeeds.
        if (!unconscious && ThievingData.BLACKJACK_TARGETS.contains(t.npcId())
                && ThievingData.BLACKJACKS.contains(com.runecraft.util.Inv.idOf(player.getMainHandStack()))) {
            knockOut(player, npc, t, now);
            return;
        }
        if (unconscious) {
            ko[1]--;
        }
        ThievingService.attempt(player, npc, new ThievingService.Target(t.name(), t.level(), t.xp(), t.low(), t.high(), t.stunDamage(),
                Thieving.pouchForNpc(t.npcId())), out -> {
            for (Loot l : t.always()) {
                out.accept(l.item(), l.amount(player.getRandom().nextDouble()));
            }
            if (!t.table().isEmpty()) {
                Loot l = Loot.pick(t.table(), player.getRandom().nextInt(Loot.totalWeight(t.table())));
                out.accept(l.item(), l.amount(player.getRandom().nextDouble()));
            }
            if (t.npcId().equals("ham_member") && player.getRandom().nextInt(ThievingData.HAM_CLUE_CHANCE) == 0) {
                out.accept("clue_scroll_easy", 1);
            }
        }, unconscious);
    }

    private static void knockOut(ServerPlayerEntity player, QuestNpcEntity npc, ThievingData.Target t, long now) {
        if (!Stun.check(player)) {
            return;
        }
        if (!com.runecraft.quest.QuestApi.isComplete(player, "the_feud")) {
            Msg.game(player, "You need to complete The Feud to knock people out with a blackjack.");
            return;
        }
        if (!SkillingUtil.need(player, Skill.THIEVING, t.level(), "knock out the " + SkillingUtil.lower(t.name()))) {
            return;
        }
        player.swingHand(Hand.MAIN_HAND, true);
        String who = SkillingUtil.lower(t.name());
        if (player.getRandom().nextDouble() < ThievingData.knockoutChance(t, SkillingUtil.level(player, Skill.THIEVING))) {
            KNOCKED_OUT.put(npc.getUuid(), new long[]{now + ThievingData.KNOCKOUT_TICKS, 2});
            Msg.action(player, "You smack the " + who + " over the head and render them unconscious.");
        } else {
            Msg.game(player, "Your blow only glances off the " + who + "'s head.");
            npc.swingHand(Hand.MAIN_HAND);
            com.runecraft.combat.CombatHandler.applyDamage(npc, player, Math.max(1, t.stunDamage()));
        }
    }

    /** Game tick until which each player's house key keeps a Civitas house open (not persisted). */
    private static final java.util.Map<java.util.UUID, Long> HOUSE_OPEN = new java.util.HashMap<>();

    /** True if the player is inside a Civitas house: a house key lets them in for a few minutes (and is used up). */
    private static boolean inHouse(ServerPlayerEntity player) {
        long now = com.runecraft.action.ActionManager.gameTick();
        Long until = HOUSE_OPEN.get(player.getUuid());
        if (until != null && until > now) {
            return true;
        }
        if (!com.runecraft.util.Inv.has(player, ThievingData.HOUSE_KEY, 1)) {
            Msg.game(player, "The house is locked. Wealthy citizens sometimes carry house keys...");
            return false;
        }
        com.runecraft.util.Inv.remove(player, ThievingData.HOUSE_KEY, 1);
        HOUSE_OPEN.put(player.getUuid(), now + ThievingData.HOUSE_VISIT_TICKS);
        Msg.game(player, "You unlock the house with your key. Better be quick before the owner gets back.");
        return true;
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
        if (ThievingData.LOCKPICK_CONTAINERS.contains(c.blockId()) && !com.runecraft.util.Inv.has(player, "lockpick", 1)) {
            Msg.game(player, "You need a lockpick to open this chest.");
            return;
        }
        if (ThievingData.HOUSE_CONTAINERS.contains(c.blockId()) && !inHouse(player)) {
            return;
        }
        String key = ThievingData.KEY_CONTAINERS.get(c.blockId());
        if (key != null && !com.runecraft.util.Inv.has(player, key, 1)) {
            Msg.game(player, "You need " + Msg.article(SkillingUtil.lower(SkillingUtil.name(key))) + " to open this chest.");
            return;
        }
        player.swingHand(Hand.MAIN_HAND, true);
        double chance = ThievingData.openChance(c.blockId(), SkillingUtil.level(player, Skill.THIEVING),
                com.runecraft.util.Inv.has(player, "lockpick", 1));
        if (player.getRandom().nextDouble() >= chance) {
            Msg.action(player, "You fail to pick the lock.");
            if (player.getRandom().nextInt(ThievingData.TRAP_CHANCE) == 0) {
                Msg.red(player, "You trigger a trap!");
                if (c.blockId().equals(ThievingData.SOUL_CHEST)) {
                    // OSRS: the Isle of Souls chest's trap teleports the player outside the dungeon entrance.
                    BlockPos out = com.runecraft.world.Gielinor.safeSpot(world,
                            com.runecraft.osrs.GielinorMap.toBlockX(ThievingData.SOUL_CHEST_TRAP_EXIT[0]),
                            com.runecraft.osrs.GielinorMap.toBlockZ(ThievingData.SOUL_CHEST_TRAP_EXIT[1]));
                    player.teleport(world, out.getX() + 0.5, out.getY(), out.getZ() + 0.5, player.getYaw(), player.getPitch());
                    return;
                }
                com.runecraft.combat.CombatHandler.applyDamage(null, player, 2);
                ServerNetworking.hitsplat(player, 2, com.runecraft.network.Net.SPLAT_DAMAGE);
            }
            return;
        }
        if (key != null) {
            com.runecraft.util.Inv.remove(player, key, 1);
        }
        if (c.chest()) {
            Msg.action(player, "You search the chest for traps... You find a trap on the chest and disable it.");
            world.playSound(null, pos, SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 1f, 1f);
        }
        SkillingUtil.giveAll(player, c.always(), player.getRandom());
        SkillingUtil.roll(player, c.table(), player.getRandom());
        com.runecraft.content.GameEvents.fireActivity(player, "thieve:" + c.blockId()); // Rocky's rate is per stall/chest
        XpService.add(player, Skill.THIEVING, c.xp());
        com.runecraft.content.GameEvents.fireActivity(player, "steal:" + c.blockId());
        Msg.action(player, c.chest() ? "You find treasure inside!" : "You steal from the " + SkillingUtil.lower(c.name()) + ".");
        GatheringHooks.deplete(world, pos, c.respawnTicks());
        ServerNetworking.sync(player);
    }
}
