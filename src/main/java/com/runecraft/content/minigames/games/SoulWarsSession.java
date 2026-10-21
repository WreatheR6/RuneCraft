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
 * fragments (2 each) and offer them at the Soul Obelisk (1% each, 1/3 as much without control of it) to
 * weaken the enemy Avatar; bury the bones dropped by fallen enemies in a graveyard (1% back on your own
 * Avatar, 1/3 as much without control of it). The obelisk and graveyards belong to the team with more
 * players near them. A player who dies drops a set of bones and everything gathered, and respawns at their
 * base. The team with more Avatar kills after 15 minutes wins (five kills end it early). Zeal: up to 60 for
 * a win, 50 for a draw and 40 for a loss, scaled by contribution. Players on opposing teams may fight.
 */
public class SoulWarsSession extends MinigameSession {
    private static final int BLUE = 0;
    private static final int RED = 1;
    private static final String[] TEAM = {"Blue", "Red"};
    /** The Avatar each team attacks: blue attacks Destruction (red's), red attacks Creation (blue's). */
    private static final String[] ENEMY_AVATAR = {"avatar_of_destruction", "avatar_of_creation"};
    private final Map<UUID, Integer> teams = new HashMap<>();
    private final Map<UUID, int[]> contribution = new HashMap<>(); // fragments and bones, captures, avatar damage
    /** Damage reduction (thirds of a percent) of the Avatar each team attacks. */
    private final int[] reduction = {SoulWars.FULL_REDUCTION_THIRDS, SoulWars.FULL_REDUCTION_THIRDS};
    private final int[] kills = new int[2];

    /** Controlling team of the obelisk and the two graveyards (-1 none), and where they stand. */
    private final int[] owner = {-1, -1, -1};
    private static final int[][] POINTS = {{0, 0}, {-12, 10}, {12, -10}};
    private static final String[] POINT_NAMES = {"Soul Obelisk", "western graveyard", "eastern graveyard"};

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
        reduction[attackingTeam] = SoulWars.FULL_REDUCTION_THIRDS;
    }

    @Override
    protected void gameTick() {
        // Forgotten Souls wander the island (not aggressive); the level 76 ones half the time. // approx: the split is unknown
        if (ticks % 20 == 0 && living("forgotten_soul").size() + living("forgotten_soul_76").size() < 6) {
            spawn(random.nextBoolean() ? "forgotten_soul" : "forgotten_soul_76", randomFloor(0, 0, 14));
        }
        if (ticks % 5 == 0) {
            controlTick();
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
                int fragments = Inv.remove(player, "soul_fragment", 1000);
                if (fragments == 0) {
                    Msg.game(player, "Kill Forgotten Souls for soul fragments to offer here.");
                    return true;
                }
                // Each fragment takes 1% off the enemy Avatar's damage reduction (a third as much without control).
                int before = SoulWars.percent(reduction[t]);
                reduction[t] = SoulWars.offer(reduction[t], fragments, owner[0] == t);
                contrib(player)[0] += fragments;
                Msg.game(player, "You offer " + fragments + " soul fragment" + (fragments > 1 ? "s" : "") + ". The enemy Avatar's damage "
                        + "reduction falls from " + before + "% to " + SoulWars.percent(reduction[t]) + "%.");
            }
            case "sw_graveyard" -> bury(player, pos.getX() < origin.getX() ? 1 : 2);
            default -> {
                return false;
            }
        }
        return true;
    }

    /** Buries the carried bones at a graveyard: 1% back on the player's own Avatar each (a third without control). */
    private void bury(ServerPlayer player, int point) {
        int t = team(player);
        int own = 1 - t; // the Avatar the other team attacks
        int bones = Inv.remove(player, "soul_wars_bones", 1000);
        if (bones == 0) {
            Msg.game(player, "Bury the bones of fallen enemies here to restore your Avatar's damage reduction.");
            return;
        }
        int before = SoulWars.percent(reduction[own]);
        reduction[own] = SoulWars.bury(reduction[own], bones, owner[point] == t);
        contrib(player)[0] += bones;
        Msg.game(player, "You bury " + bones + " set" + (bones > 1 ? "s" : "") + " of bones. Your Avatar's damage reduction rises "
                + "from " + before + "% to " + SoulWars.percent(reduction[own]) + "%.");
    }

    /** The obelisk and graveyards go to the team with more players near them; those present share the capture. */
    private void controlTick() {
        for (int point = 0; point < POINTS.length; point++) {
            BlockPos at = rel(POINTS[point][0], 1, POINTS[point][1]);
            int[] near = new int[2];
            List<ServerPlayer> present = new java.util.ArrayList<>();
            for (ServerPlayer p : online()) {
                if (p.blockPosition().distSqr(at) <= SoulWars.CONTROL_RANGE * SoulWars.CONTROL_RANGE) {
                    near[team(p)]++;
                    present.add(p);
                }
            }
            int to = SoulWars.control(owner[point], near[BLUE], near[RED]);
            if (to != owner[point]) {
                owner[point] = to;
                for (ServerPlayer p : present) {
                    if (team(p) == to) {
                        contrib(p)[1]++;
                    }
                }
                broadcast("The " + TEAM[to] + " team has taken control of the " + POINT_NAMES[point] + "!");
            }
        }
    }

    /** Who controls the obelisk (0) or a graveyard (1, 2): -1 none, 0 blue, 1 red (public for the self-test). */
    public int owner(int point) {
        return owner[point];
    }

    /** The damage reduction, in thirds of a percent, of the Avatar a team attacks (public for the self-test). */
    public int reduction(int attackingTeam) {
        return reduction[attackingTeam];
    }

    @Override
    protected double damageMultiplier(ServerPlayer player, LivingEntity target, AttackType type) {
        int t = team(player);
        if (MinigameManager.creatureId(target).equals(ENEMY_AVATAR[t])) {
            return (SoulWars.FULL_REDUCTION_THIRDS - reduction[t]) / (double) SoulWars.FULL_REDUCTION_THIRDS;
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

    /**
     * A death drops one set of bones plus every bone and soul fragment carried where the player fell (OSRS Wiki "Bones (Soul
     * Wars)"), and the player respawns at their base.
     */
    @Override
    protected void onDeath(ServerPlayer player) {
        int bones = SoulWars.BONES_PER_DEATH + Inv.remove(player, "soul_wars_bones", 1000);
        int fragments = Inv.remove(player, "soul_fragment", 1000);
        drop(player.blockPosition(), "soul_wars_bones", bones);
        drop(player.blockPosition(), "soul_fragment", fragments);
        Msg.red(player, "Oh dear, you are dead! You respawn at your team's base.");
        BlockPos at = spawnPoint(player, team(player));
        player.teleportTo(world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, player.getYRot(), player.getXRot());
    }

    private void drop(BlockPos at, String item, int count) {
        if (count <= 0) {
            return;
        }
        net.minecraft.world.item.ItemStack stack = Inv.stack(item, count);
        if (!stack.isEmpty()) {
            world.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(world, at.getX() + 0.5, at.getY() + 0.5,
                    at.getZ() + 0.5, stack));
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
                "Enemy Avatar: " + SoulWars.percent(reduction[t]) + "%  Your Avatar: " + SoulWars.percent(reduction[1 - t]) + "%",
                "Obelisk: " + (owner[0] < 0 ? "none" : TEAM[owner[0]]),
                "Fragments/bones " + c[0] + "  Captures " + c[1] + "  Avatar damage " + c[2],
                "Time left: " + time(SoulWars.GAME_TICKS - ticks));
    }
}
