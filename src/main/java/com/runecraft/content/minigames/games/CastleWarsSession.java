package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameRules.CastleWars;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;

/**
 * Castle Wars: Saradomin against Zamorak. Steal the enemy standard from their castle and bring it
 * to your own flag stand while your own standard is home. Players on opposing teams fight with
 * OSRS melee; a carrier who dies drops the standard and it returns to its stand. Tickets follow
 * OSRS: 3 for a shut-out win, 2 for another win or a scoring draw, 1 for a loss or a 0-0 draw.
 */
public class CastleWarsSession extends MinigameSession {
    private static final int SARA = 0;
    private static final int ZAM = 1;
    private static final String[] TEAM = {"Saradomin", "Zamorak"};
    private static final String[] STANDARD = {"saradomin_standard", "zamorak_standard"};
    private final Map<UUID, Integer> teams = new HashMap<>();
    private final int[] score = new int[2];
    /** Who carries each team's standard (null = on its stand). */
    private final UUID[] carrier = new UUID[2];

    public CastleWarsSession(int id, int slot, ServerLevel world, String variant) {
        super(Minigame.CASTLE_WARS, id, slot, world, variant);
    }

    @Override
    protected int halfX() {
        return 30;
    }

    @Override
    protected int halfZ() {
        return 18;
    }

    @Override
    protected int height() {
        return 12;
    }

    @Override
    protected void build() {
        room(Blocks.GRASS_BLOCK, Blocks.STONE_BRICKS, Blocks.COBBLESTONE);
        castle(-24, Blocks.LAPIS_BLOCK);
        castle(24, Blocks.REDSTONE_BLOCK);
        place(-26, 0, "cw_saradomin_stand");
        place(26, 0, "cw_zamorak_stand");
        place(-22, 6, "cw_table");
        place(22, 6, "cw_table");
        place(0, 16, "minigame_exit");
        // Rocks and a river in the middle.
        for (int z = -17; z <= 17; z++) {
            if (Math.abs(z) > 2) {
                set(0, 0, z, Blocks.WATER.defaultBlockState());
                set(1, 0, z, Blocks.WATER.defaultBlockState());
            }
        }
    }

    private void castle(int cx, net.minecraft.world.level.block.Block accent) {
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                boolean wall = Math.abs(dx) == 5 || Math.abs(dz) == 8;
                boolean gate = Math.abs(dz) <= 1 && dx == (cx < 0 ? 5 : -5);
                if (wall && !gate) {
                    for (int y = 1; y <= 4; y++) {
                        set(cx + dx, y, dz, (y == 4 && (dx + dz) % 2 == 0 ? accent : Blocks.STONE_BRICKS).defaultBlockState());
                    }
                }
            }
        }
    }

    private int team(ServerPlayer p) {
        return teams.getOrDefault(p.getUUID(), SARA);
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
        int t = index % 2;
        return rel(t == SARA ? -22 : 22, 1, index / 2 % 5 - 2);
    }

    @Override
    protected void start() {
        int i = 0;
        for (ServerPlayer p : online()) {
            int t = i++ % 2;
            teams.put(p.getUUID(), t);
            BlockPos at = spawnPoint(p, t + 2 * (i / 2));
            p.teleportTo(world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getYRot(), p.getXRot());
            Msg.game(p, "You are on team " + TEAM[t] + ". Capture the " + TEAM[1 - t] + " standard!");
            Inv.give(p, t == SARA ? "saradomin_hood" : "zamorak_hood", 1);
        }
    }

    @Override
    protected boolean canAttackPlayer(ServerPlayer attacker, ServerPlayer target) {
        return team(attacker) != team(target);
    }

    @Override
    protected double damageMultiplier(ServerPlayer player, LivingEntity target, AttackType type) {
        if (target instanceof ServerPlayer t && (Inv.has(player, "castlewars_brace", 1) || com.runecraft.content.systems.SysState.of(player)
                .wornId(com.runecraft.osrs.content.systems.Jewellery.Slot.HANDS).startsWith("castle_wars_bracelet"))
                && (Inv.has(t, STANDARD[0], 1) || Inv.has(t, STANDARD[1], 1))) {
            return 1.2;
        }
        return 1.0;
    }

    @Override
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
        int t = team(player);
        switch (blockId) {
            case "cw_saradomin_stand", "cw_zamorak_stand" -> {
                int standTeam = blockId.equals("cw_saradomin_stand") ? SARA : ZAM;
                if (standTeam != t) {
                    if (carrier[standTeam] != null) {
                        Msg.game(player, "The " + TEAM[standTeam] + " standard has already been taken!");
                        return true;
                    }
                    carrier[standTeam] = player.getUUID();
                    Inv.give(player, STANDARD[standTeam], 1);
                    broadcastRed(player.getName().getString() + " has taken the " + TEAM[standTeam] + " standard!");
                } else if (Inv.has(player, STANDARD[1 - t], 1)) {
                    if (carrier[t] != null) {
                        Msg.game(player, "Your own standard must be on its stand to score!");
                        return true;
                    }
                    Inv.remove(player, STANDARD[1 - t], 10);
                    carrier[1 - t] = null;
                    score[t]++;
                    broadcastRed(TEAM[t] + " scores! " + TEAM[SARA] + " " + score[SARA] + " - " + score[ZAM] + " " + TEAM[ZAM]);
                } else {
                    Msg.game(player, "This is your team's flag stand. Bring the enemy standard here!");
                }
                return true;
            }
            case "cw_table" -> {
                Inv.give(player, "castle_wars_bandage", 5);
                Msg.game(player, "You take some bandages.");
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private void dropStandards(ServerPlayer p) {
        for (int i = 0; i < 2; i++) {
            if (p.getUUID().equals(carrier[i])) {
                carrier[i] = null;
                Inv.remove(p, STANDARD[i], 10);
                broadcast("The " + TEAM[i] + " standard has been returned to its stand.");
            }
        }
    }

    @Override
    protected void onDeath(ServerPlayer player) {
        dropStandards(player);
        Msg.red(player, "Oh dear, you are dead! You respawn in your castle.");
        BlockPos at = spawnPoint(player, team(player));
        player.teleportTo(world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, player.getYRot(), player.getXRot());
    }

    @Override
    protected void onLeave(ServerPlayer player) {
        dropStandards(player);
        Inv.remove(player, "castle_wars_bandage", 1000);
        Inv.remove(player, STANDARD[0], 10);
        Inv.remove(player, STANDARD[1], 10);
    }

    @Override
    protected void gameTick() {
        if (ticks >= CastleWars.GAME_TICKS) {
            end(true);
        }
    }

    @Override
    protected void onEnd(boolean success) {
        int winner = score[SARA] == score[ZAM] ? -1 : score[SARA] > score[ZAM] ? SARA : ZAM;
        for (ServerPlayer p : online()) {
            int t = team(p);
            int tickets = CastleWars.tickets(score[t], score[1 - t]);
            reward(p, "castle_wars_ticket", tickets);
            MinigameData.of(p).incrementStat(p.getUUID(), "kc:" + game.id());
            Msg.game(p, (winner < 0 ? "It's a draw!" : TEAM[winner] + " wins!") + " You receive " + tickets + " ticket"
                    + (tickets > 1 ? "s" : "") + ".");
        }
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        return List.of("Castle Wars - team " + TEAM[team(player)], "Saradomin " + score[SARA] + " - " + score[ZAM] + " Zamorak",
                "Saradomin standard: " + (carrier[SARA] == null ? "safe" : "TAKEN"),
                "Zamorak standard: " + (carrier[ZAM] == null ? "safe" : "TAKEN"),
                "Time left: " + time(CastleWars.GAME_TICKS - ticks));
    }
}
