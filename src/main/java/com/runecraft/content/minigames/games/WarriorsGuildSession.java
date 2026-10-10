package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameItems;
import com.runecraft.osrs.content.minigames.MinigameRules.WarriorsGuild;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Warriors' Guild. The animator room: place a full helm, platebody and platelegs on a magical
 * animator to fight the animated armour; it gives the armour back plus tokens (bronze 5 ... rune
 * 40). The cyclops room: costs 10 tokens to enter and 10 per minute; cyclopes drop the next
 * defender after the best one you own (1/50), and with a rune defender the elite cyclopes drop the
 * dragon defender (1/100).
 */
public class WarriorsGuildSession extends MinigameSession {
    private final boolean cyclopsRoom;
    /** Animated armour entity id -> (owner, tier) so the armour can be returned. */
    private final Map<Integer, Object[]> animated = new HashMap<>();
    private long nextCharge;

    public WarriorsGuildSession(int id, int slot, ServerWorld world, String variant) {
        super(Minigame.WARRIORS_GUILD, id, slot, world, variant);
        this.cyclopsRoom = variant.contains("cyclops");
    }

    @Override
    protected int halfX() {
        return 16;
    }

    @Override
    protected int halfZ() {
        return 16;
    }

    @Override
    protected void build() {
        room(Blocks.OAK_PLANKS, Blocks.STONE_BRICKS, Blocks.DARK_OAK_PLANKS);
        if (!cyclopsRoom) {
            place(-8, -8, "wg_armour_animator");
            place(8, -8, "wg_armour_animator");
            place(-8, 0, "wg_armour_animator");
            place(8, 0, "wg_armour_animator");
        }
        place(0, 14, "minigame_exit");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayerEntity player, int index) {
        return rel(index % 3 - 1, 1, 11);
    }

    @Override
    protected void start() {
        if (cyclopsRoom) {
            for (ServerPlayerEntity p : online()) {
                Inv.remove(p, "warrior_guild_token", WarriorsGuild.CYCLOPS_TOKENS);
                Msg.game(p, WarriorsGuild.CYCLOPS_TOKENS + " tokens are taken as you enter the cyclops room.");
            }
            nextCharge = WarriorsGuild.CYCLOPS_TICKS_PER_CHARGE;
        }
    }

    /** Index of the best defender a player owns (0 bronze .. 7 dragon), or -1. */
    private static int bestDefender(ServerPlayerEntity p) {
        int best = -1;
        for (int i = 0; i < MinigameItems.DEFENDER_TIERS.length; i++) {
            if (Inv.has(p, MinigameItems.DEFENDER_TIERS[i] + "_defender", 1)) {
                best = i;
            }
        }
        return best;
    }

    @Override
    protected void gameTick() {
        if (!cyclopsRoom) {
            return;
        }
        if (ticks >= nextCharge) {
            nextCharge = ticks + WarriorsGuild.CYCLOPS_TICKS_PER_CHARGE;
            for (ServerPlayerEntity p : online()) {
                if (Inv.remove(p, "warrior_guild_token", WarriorsGuild.CYCLOPS_TOKENS) < WarriorsGuild.CYCLOPS_TOKENS) {
                    Msg.red(p, "You have run out of tokens.");
                    removePlayer(p, true);
                }
            }
            if (isEnded()) {
                return;
            }
        }
        boolean elite = online().stream().anyMatch(p -> bestDefender(p) >= 6);
        List<LivingEntity> cyclopes = living(null);
        if (ticks % 10 == 0 && cyclopes.size() < 4 + playerCount()) {
            MobEntity c = spawn(elite ? "cyclops_elite" : "warriors_guild_cyclops", randomFloor(0, -6, 8));
            if (c != null) {
                aggro(c);
            }
        }
    }

    @Override
    protected boolean onBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
        if (!blockId.equals("wg_armour_animator")) {
            return false;
        }
        for (int i = WarriorsGuild.ARMOUR.length - 1; i >= 0; i--) {
            String tier = (String) WarriorsGuild.ARMOUR[i][0];
            if (Inv.has(player, tier + "_full_helm", 1) && Inv.has(player, tier + "_platebody", 1)
                    && Inv.has(player, tier + "_platelegs", 1)) {
                Inv.remove(player, tier + "_full_helm", 1);
                Inv.remove(player, tier + "_platebody", 1);
                Inv.remove(player, tier + "_platelegs", 1);
                MobEntity armour = spawn("animated_" + tier + "_armour", pos.up().south(2));
                if (armour != null) {
                    armour.setTarget(player);
                    animated.put(armour.getId(), new Object[]{player.getUuid(), tier});
                    world.spawnParticles(ParticleTypes.ENCHANT, armour.getX(), armour.getY() + 1, armour.getZ(), 30, 0.5, 1, 0.5, 0.5);
                    Msg.game(player, "You place your armour on the platform where it disappears... The animator hums, something appears!");
                }
                return true;
            }
        }
        Msg.game(player, "You need a full helm, platebody and platelegs of the same metal in your inventory.");
        return true;
    }

    @Override
    protected void onKill(ServerPlayerEntity player, LivingEntity entity, String creatureId) {
        Object[] a = animated.remove(entity.getId());
        if (a != null) {
            String tier = (String) a[1];
            Inv.give(player, tier + "_full_helm", 1);
            Inv.give(player, tier + "_platebody", 1);
            Inv.give(player, tier + "_platelegs", 1);
            int tokens = WarriorsGuild.tokens(tier);
            reward(player, "warrior_guild_token", tokens);
            Msg.game(player, "The animated armour collapses. You collect your armour and " + tokens + " tokens.");
            return;
        }
        if (creatureId.contains("cyclops")) {
            int best = bestDefender(player);
            int next = WarriorsGuild.nextDefender(best);
            boolean dragon = next == MinigameItems.DEFENDER_TIERS.length - 1;
            if (dragon && !creatureId.equals("cyclops_elite")) {
                return;
            }
            int chance = dragon ? WarriorsGuild.DRAGON_DEFENDER_CHANCE : WarriorsGuild.DEFENDER_CHANCE;
            if (best == next || random.nextInt(chance) != 0) {
                return;
            }
            String item = MinigameItems.DEFENDER_TIERS[next] + "_defender";
            reward(player, item, 1);
            Msg.red(player, "The cyclops dropped a " + item.replace('_', ' ') + "!");
        }
    }

    @Override
    protected void onLeave(ServerPlayerEntity player) {
        for (Object[] a : new ArrayList<>(animated.values())) {
            if (player.getUuid().equals(a[0])) {
                String tier = (String) a[1];
                Inv.give(player, tier + "_full_helm", 1);
                Inv.give(player, tier + "_platebody", 1);
                Inv.give(player, tier + "_platelegs", 1);
            }
        }
        animated.values().removeIf(a -> player.getUuid().equals(a[0]));
    }

    @Override
    protected List<String> hud(ServerPlayerEntity player) {
        int best = bestDefender(player);
        return List.of(cyclopsRoom ? "Warriors' Guild - Cyclops room" : "Warriors' Guild - Animator room",
                "Tokens: " + Inv.count(player, "warrior_guild_token"),
                cyclopsRoom ? "Next charge: " + time(nextCharge - ticks) : "Place armour on an animator",
                "Best defender: " + (best < 0 ? "none" : MinigameItems.DEFENDER_TIERS[best]));
    }
}
