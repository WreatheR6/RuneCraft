package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameCurrencies;
import com.runecraft.osrs.content.minigames.MinigameRules.SoulWars;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;

/**
 * Soul Wars on the Isle of Souls: the blue team fights the Avatar of Destruction and the red team
 * the Avatar of Creation. Avatars start with 100% damage reduction; kill Forgotten Souls for soul
 * fragments and offer them at the Soul Obelisk (1% each, 1/3 as much without control of it) to
 * weaken the enemy Avatar, capture the obelisk and graveyards, and damage the Avatar. The team
 * with more Avatar kills after 15 minutes wins (five kills end it early). Zeal: up to 60 for a win,
 * 50 for a draw and 40 for a loss, scaled by contribution. Players on opposing teams may fight.
 */
public class SoulWarsSession extends MinigameSession {
    private static final int BLUE = 0;
    private static final int RED = 1;
    private static final String[] TEAM = {"Blue", "Red"};
    /** The Avatar each team attacks: blue attacks Destruction (red's), red attacks Creation (blue's). */
    private static final String[] ENEMY_AVATAR = {"avatar_of_destruction", "avatar_of_creation"};
    private final Map<UUID, Integer> teams = new HashMap<>();
    private final Map<UUID, int[]> contribution = new HashMap<>(); // fragments, captures, avatar damage
    private final int[] reduction = {100, 100}; // damage reduction of the Avatar each team attacks
    private final int[] kills = new int[2];

    /** Controlling team of the obelisk and the two graveyards (-1 none). */
    private final int[] owner = {-1, -1, -1};

    public SoulWarsSession(int id, int slot, ServerLevel world, String variant) {
        super(Minigame.SOUL_WARS, id, slot, world, variant);
    }

    @Override
    protected int halfX() {
        return 26;
    }

    @Override
    protected int halfZ() {
        return 22;
    }

    @Override
    protected void build() {
        room(Blocks.GRASS_BLOCK, Blocks.DARK_OAK_LOG, Blocks.PURPUR_BLOCK);
        place(0, 0, "sw_soul_obelisk");
        place(-12, 10, "sw_graveyard");
        place(12, -10, "sw_graveyard");
        place(0, 20, "minigame_exit");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
        teams.putIfAbsent(player.getUUID(), index % 2);
        return team(player) == BLUE ? rel(-20, 1, 14 - index % 3) : rel(20, 1, -14 + index % 3);
    }

    private int team(ServerPlayer p) {
        return teams.getOrDefault(p.getUUID(), BLUE);
    }

    private int[] contrib(ServerPlayer p) {
        return contribution.computeIfAbsent(p.getUUID(), u -> new int[3]);
    }

    @Override
    protected void start() {
        for (ServerPlayer p : online()) {
            Msg.game(p, "You join the " + TEAM[team(p)] + " team. Weaken and slay the " + (team(p) == BLUE ? "Avatar of Destruction"
                    : "Avatar of Creation") + "!");
        }
        spawnAvatar(BLUE);
        if (online().stream().anyMatch(p -> team(p) == RED)) {
            spawnAvatar(RED);
        }
    }

    private void spawnAvatar(int attackingTeam) {
        BlockPos at = attackingTeam == BLUE ? rel(22, 1, -16) : rel(-22, 1, 16);
        Mob avatar = spawn(ENEMY_AVATAR[attackingTeam], at);
        if (avatar != null) {
            avatar.setPersistenceRequired();
        }
        reduction[attackingTeam] = 100;
    }

    @Override
    protected void gameTick() {
        if (ticks % 20 == 0 && living("forgotten_soul").size() < 6) {
            Mob soul = spawn("forgotten_soul", randomFloor(0, 0, 14));
            if (soul != null) {
                aggro(soul);
            }
        }
        if (ticks >= SoulWars.GAME_TICKS || kills[BLUE] >= SoulWars.AVATAR_KILLS_TO_WIN || kills[RED] >= SoulWars.AVATAR_KILLS_TO_WIN) {
            end(true);
        }
    }

    @Override
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
        int t = team(player);
        switch (blockId) {
            case "sw_soul_obelisk" -> {
                capture(player, 0);
                int fragments = Inv.remove(player, "soul_fragment", 1000);
                if (fragments == 0) {
                    Msg.game(player, "Kill Forgotten Souls for soul fragments to offer here.");
                    return true;
                }
                // Each fragment removes 1% of the enemy Avatar's damage reduction (a third as much without control).
                int perFragment = owner[0] == t ? 3 : 1;
                int before = reduction[t];
                reduction[t] = Math.max(0, reduction[t] - fragments * perFragment / 3);
                contrib(player)[0] += fragments;
                Msg.game(player, "You offer " + fragments + " soul fragment" + (fragments > 1 ? "s" : "") + ". The enemy Avatar's damage "
                        + "reduction falls from " + before + "% to " + reduction[t] + "%.");
            }
            case "sw_graveyard" -> capture(player, pos.getX() < origin.getX() ? 1 : 2);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void capture(ServerPlayer player, int point) {
        int t = team(player);
        if (owner[point] == t) {
            return;
        }
        owner[point] = t;
        contrib(player)[1]++;
        broadcast("The " + TEAM[t] + " team has captured the " + (point == 0 ? "Soul Obelisk" : "graveyard") + "!");
    }

    @Override
    protected double damageMultiplier(ServerPlayer player, LivingEntity target, AttackType type) {
        int t = team(player);
        if (MinigameManager.creatureId(target).equals(ENEMY_AVATAR[t])) {
            return (100 - reduction[t]) / 100.0;
        }
        return 1.0;
    }

    @Override
    protected void onPlayerHit(ServerPlayer player, LivingEntity target, int dealt) {
        if (MinigameManager.creatureId(target).equals(ENEMY_AVATAR[team(player)])) {
            contrib(player)[2] += dealt;
        }
    }

    @Override
    protected boolean canAttackPlayer(ServerPlayer attacker, ServerPlayer target) {
        return team(attacker) != team(target);
    }

    @Override
    protected void onKill(ServerPlayer player, LivingEntity entity, String creatureId) {
        int t = team(player);
        if (creatureId.equals(ENEMY_AVATAR[t])) {
            kills[t]++;
            broadcastRed("The " + TEAM[t] + " team has slain the " + entity.getName().getString() + "! (" + kills[t] + ")");
            spawnAvatar(t);
        }
    }

    @Override
    protected void onEnd(boolean success) {
        for (ServerPlayer p : online()) {
            int t = team(p);
            int outcome = Integer.compare(kills[t], kills[1 - t]);
            int[] c = contrib(p);
            int zeal = SoulWars.zeal(outcome, c[0], c[1], c[2]);
            Inv.remove(p, "soul_fragment", 1000);
            Inv.remove(p, "soul_wars_bones", 1000);
            if (zeal > 0) {
                addPoints(p, MinigameCurrencies.ZEAL, zeal);
            }
            MinigameData.of(p).incrementStat(p.getUUID(), "kc:" + game.id());
            Msg.game(p, (outcome > 0 ? "Victory!" : outcome == 0 ? "It's a draw." : "Defeat.") + " You receive " + zeal + " Zeal Tokens.");
        }
    }

    @Override
    protected void onLeave(ServerPlayer player) {
        Inv.remove(player, "soul_fragment", 1000);
        Inv.remove(player, "soul_wars_bones", 1000);
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        int t = team(player);
        int[] c = contrib(player);
        return List.of("Soul Wars - " + TEAM[t] + " team", "Avatar kills: Blue " + kills[BLUE] + " - " + kills[RED] + " Red",
                "Enemy Avatar damage reduction: " + reduction[t] + "%",
                "Fragments " + c[0] + "  Captures " + c[1] + "  Avatar damage " + c[2],
                "Time left: " + time(SoulWars.GAME_TICKS - ticks));
    }
}
