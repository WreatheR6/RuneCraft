package com.runecraft.content.minigames;

import com.runecraft.content.BlockHooks;
import com.runecraft.content.GameEvents;
import com.runecraft.content.minigames.games.GauntletSession;
import com.runecraft.content.systems.SysNet;
import com.runecraft.content.systems.SysState;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Gauntlet;
import com.runecraft.osrs.content.minigames.GauntletLobby;
import com.runecraft.osrs.content.systems.RunEnergy;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.platform.UseItemCallback;
import com.runecraft.platform.ServerPlayConnectionEvents;
import com.runecraft.world.Gielinor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Gauntlet behaviour outside the session itself: the lobby (the Gauntlet Portal in Prifddinas, the Teleport Platform back,
 * the scoreboard and bookstands), the reward chest's rolls on every Hunllef kill, the armour's damage reduction against the
 * Hunllef and its tornadoes, the egniol potion, teleport and escape crystals, crushing weapon components, and giving a stash
 * back to a player who logs in after a crash mid-run.
 */
public final class GauntletService {
    private static final Random RANDOM = new Random();

    private GauntletService() {
    }

    static void register() {
        // Wiki Reward Chest: a Crystalline Hunllef kill rolls the regular table twice, a Corrupted one the corrupted table three
        // times; the 100% and tertiary rows are the Hunllef's own drops (bosses module), rolled below into the same chest.
        GameEvents.onKill((player, entity, stats) -> {
            Gauntlet.Mode mode = Gauntlet.modeOfBoss(MinigameManager.creatureId(entity));
            if (mode == null) {
                return;
            }
            Gauntlet.Table table = Gauntlet.completion(mode);
            MinigameSession s = MinigameManager.session(player);
            for (int i = 0; i < table.rolls(); i++) {
                Gauntlet.Loot l = table.roll(RANDOM);
                int n = l.min() + RANDOM.nextInt(l.max() - l.min() + 1);
                if (s instanceof GauntletSession g) {
                    g.owe(l.item(), n);
                } else {
                    Inv.give(player, l.item(), n);
                }
            }
        });
        // The Hunllef drops nothing on the floor in OSRS: its shards, seeds, clue and cape wait in the reward chest with the
        // rest of the run's loot (logged when won). The Gauntlet cape only comes when the player has none (wiki).
        for (Gauntlet.Mode mode : Gauntlet.Mode.values()) {
            com.runecraft.boss.DataBossService.loot(mode.boss, (killer, boss, table) -> {
                List<com.runecraft.skill.Drops.Rolled> rolled = com.runecraft.skill.Drops.roll(table, boss.getRandom(), false);
                if (!(MinigameManager.session(killer) instanceof GauntletSession g)) {
                    return rolled;
                }
                for (com.runecraft.skill.Drops.Rolled r : rolled) {
                    if (r.item().equals("gauntlet_cape") && ownsCape(killer)) {
                        continue;
                    }
                    g.owe(r.item(), r.count());
                    com.runecraft.collection.CollectionLog.obtain(killer, r.item());
                }
                return new ArrayList<>();
            });
        }
        com.runecraft.combat.CombatHooks.addIncomingModifier(GauntletService::armour);
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (world.isClientSide() || !(player instanceof ServerPlayer sp) || stack.isEmpty()) {
                return InteractionResultHolder.pass(stack);
            }
            String id = Inv.idOf(stack);
            if (id.equals(Gauntlet.EGNIOL)) {
                if (!sp.getCooldowns().isOnCooldown(stack.getItem())) {
                    drinkEgniol(sp, stack, hand);
                }
                return InteractionResultHolder.success(stack);
            }
            if (id.equals(Gauntlet.ESCAPE_CRYSTAL)) {
                if (MinigameManager.session(sp) instanceof GauntletSession g) {
                    stack.shrink(1);
                    Msg.action(sp, "The crystal shatters and you escape the Gauntlet.");
                    // Wiki: an escape crystal only leaves loot when it is used during the Hunllef fight.
                    g.abandon(!g.inBossFight());
                } else {
                    Msg.game(sp, "The crystal only works inside the Gauntlet.");
                }
                return InteractionResultHolder.success(stack);
            }
            for (Gauntlet.Mode m : Gauntlet.Mode.values()) {
                if (id.equals(Gauntlet.teleport(m))) {
                    if (MinigameManager.session(sp) instanceof GauntletSession g) {
                        if (g.teleportToStart(sp)) {
                            stack.shrink(1);
                            Msg.action(sp, "The crystal shatters and returns you to the starting room.");
                        }
                    } else {
                        Msg.game(sp, "The crystal only works inside the Gauntlet.");
                    }
                    return InteractionResultHolder.success(stack);
                }
                if (Gauntlet.pieceOfComponent(m, id) != null && MinigameManager.session(sp) instanceof GauntletSession) {
                    GauntletSession.offerCrush(sp, id);
                    return InteractionResultHolder.success(stack);
                }
            }
            return InteractionResultHolder.pass(stack);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer p = handler.getPlayer();
            if (GauntletStash.has(p) && !(MinigameManager.session(p) instanceof GauntletSession)) {
                GauntletStash.restore(p);
                Msg.game(p, "Your belongings from before the Gauntlet have been returned.");
            }
        });
        // The lobby's objects, used outside a run (inside one the session handles its own platform and books).
        // The chest is one of the Gauntlet's blocks routed to the session: outside a run it opens here (its direct hook in
        // MinigamesHooks is replaced by that routing, which left the chest saying "Nothing interesting happens").
        BlockHooks.registerFallback(Gauntlet.REWARD_CHEST, (player, pos, state) -> GauntletSession.rewardChest(player));
        BlockHooks.registerFallback(Gauntlet.PORTAL, (player, pos, state) -> enterLobby(player));
        BlockHooks.registerFallback(Gauntlet.TELEPORT_PLATFORM, (player, pos, state) -> leaveLobby(player));
        BlockHooks.registerFallback(Gauntlet.SCOREBOARD, (player, pos, state) -> GauntletSession.scoreboard(player));
        BlockHooks.registerFallback(Gauntlet.RECIPES_BOOK, (player, pos, state) -> GauntletSession.recipes(player));
        BlockHooks.registerFallback(Gauntlet.EGNIOL_BOOK, (player, pos, state) -> GauntletSession.egniolBook(player));
    }

    /** Whether the player already has a Gauntlet cape (carried, banked or in the run's stash). */
    static boolean ownsCape(ServerPlayer p) {
        String cape = "gauntlet_cape";
        return Inv.has(p, cape, 1) || Players.data(p).bank().count(cape, "") > 0 || GauntletStash.stashHas(p, cape);
    }

    /** The Gauntlet Portal: into the lobby (built first if its chunks have not loaded yet). */
    public static boolean enterLobby(ServerPlayer player) {
        ServerLevel world = Gielinor.world(player.getServer());
        if (world == null) {
            return false;
        }
        if (!com.runecraft.quest.QuestApi.isComplete(player, "song_of_the_elves")) {
            Msg.game(player, "You need to complete Song of the Elves to enter the Gauntlet.");
            return false;
        }
        int x = GauntletLobby.blockX(GauntletLobby.ARRIVE_X);
        int z = GauntletLobby.blockZ(GauntletLobby.ARRIVE_Y);
        com.runecraft.world.StructurePlacer.applyAround(world, x, z, 40);
        player.teleportTo(world, x + 1.0, GauntletLobby.GROUND, z + 1.0, 180f, 0f);
        Msg.action(player, "You step through the portal into the Gauntlet's hub.");
        return true;
    }

    /** The lobby's Teleport Platform: back to the Gauntlet Portal in Prifddinas. */
    public static boolean leaveLobby(ServerPlayer player) {
        ServerLevel world = Gielinor.world(player.getServer());
        if (world == null) {
            return false;
        }
        BlockPos at = Gielinor.safeSpot(world, GauntletLobby.blockX(GauntletLobby.PORTAL_X), GauntletLobby.blockZ(GauntletLobby.PORTAL_Y) + 3);
        player.teleportTo(world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180f, 0f);
        return true;
    }

    /** Whether a position is inside the lobby hall (self-test). */
    public static boolean inLobby(BlockPos pos) {
        int x1 = GauntletLobby.blockX(GauntletLobby.X1);
        int x2 = GauntletLobby.blockX(GauntletLobby.X2) + 1;
        int z1 = GauntletLobby.blockZ(GauntletLobby.Y2);
        int z2 = GauntletLobby.blockZ(GauntletLobby.Y1) + 1;
        return pos.getX() >= x1 && pos.getX() <= x2 && pos.getZ() >= z1 && pos.getZ() <= z2
                && pos.getY() >= GauntletLobby.GROUND - 1 && pos.getY() < GauntletLobby.GROUND + GauntletLobby.HEIGHT;
    }

    /** Worn crystal or corrupted armour lowers the Hunllef's and its tornadoes' hits by the sum of the pieces' tiers. */
    static int armour(ServerPlayer p, LivingEntity attacker, int damage) {
        if (damage <= 0 || attacker == null || !(MinigameManager.session(p) instanceof GauntletSession g)) {
            return damage;
        }
        String id = MinigameManager.creatureId(attacker);
        if (!id.equals(g.mode().boss) && !id.equals("hunllef_tornado")) {
            return damage;
        }
        int sum = Gauntlet.armourTier(Inv.idOf(p.getItemBySlot(EquipmentSlot.HEAD)))
                + Gauntlet.armourTier(Inv.idOf(p.getItemBySlot(EquipmentSlot.CHEST)))
                + Gauntlet.armourTier(Inv.idOf(p.getItemBySlot(EquipmentSlot.LEGS)));
        return (int) Math.round(damage * Gauntlet.hunllefDamageScale(g.mode(), sum));
    }

    /** Egniol potion: restores floor(Prayer / 4) + 7 prayer points and 40% run energy, and acts as a stamina potion. */
    static void drinkEgniol(ServerPlayer p, ItemStack stack, InteractionHand hand) {
        PlayerData data = Players.data(p);
        int level = data.skills().level(Skill.PRAYER);
        data.setPrayerPoints(Math.min(level, data.prayerPoints() + Gauntlet.egniolPrayer(level)));
        SysState.SysPlayer sp = SysState.of(p);
        sp.runEnergy = Math.min(RunEnergy.MAX, sp.runEnergy + RunEnergy.MAX * 40 / 100);
        sp.staminaUntil = com.runecraft.action.ActionManager.gameTick() + 200;
        p.level().playSound(null, p.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.5f, 1.0f);
        p.getCooldowns().addCooldown(stack.getItem(), 3 * com.runecraft.action.ActionManager.MC_TICKS_PER_GAME_TICK);
        int used = stack.getDamageValue() + 1;
        if (used >= 4) {
            p.setItemInHand(hand, Inv.stack(Gauntlet.VIAL, 1));
            Msg.action(p, "You drink the last of your egniol potion.");
        } else {
            stack.setDamageValue(used);
            Msg.action(p, "You drink some of your egniol potion. You have " + (4 - used) + " dose" + (used == 3 ? "" : "s") + " left.");
        }
        SysNet.sendState(p);
        com.runecraft.network.ServerNetworking.sync(p);
    }
}
