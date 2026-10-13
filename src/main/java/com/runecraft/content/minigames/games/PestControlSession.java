package com.runecraft.content.minigames.games;

import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.MonsterStats;
import com.runecraft.content.minigames.MinigameCombat;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameCurrencies;
import com.runecraft.osrs.content.minigames.MinigameRules.PestControl;
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
import java.util.UUID;

/**
 * Pest Control: defend the Void Knight while destroying the four portals. Portal shields drop one
 * at a time; portals spawn brawlers, defilers, ravagers, shifters, spinners, splatters and
 * torchers; spinners heal portals; pests that reach the knight attack him. Destroy every portal
 * (or survive 20 minutes) to win commendation points (3/4/5 by lander) if you dealt 50+ damage.
 */
public class PestControlSession extends MinigameSession {
    /** Extra commendation points per won game (the systems module adds the Combat Achievement tier bonus). */
    public static java.util.function.ToIntFunction<ServerPlayerEntity> extraPoints = p -> 0;

    private static final int[][] PORTAL_POS = {{-24, 0}, {24, 0}, {12, 22}, {-12, 22}};
    private static final String[] PORTAL_DIR = {"western", "eastern", "south-eastern", "south-western"};
    private final int tier;
    private final MobEntity[] portals = new MobEntity[4];
    private final boolean[] shielded = {true, true, true, true};
    private MobEntity knight;
    private final Map<UUID, Integer> activity = new HashMap<>();
    private boolean won;

    public PestControlSession(int id, int slot, ServerWorld world, String variant) {
        super(Minigame.PEST_CONTROL, id, slot, world, variant);
        this.tier = variant.contains("veteran") ? 2 : variant.contains("intermediate") ? 1 : 0;
    }

    @Override
    protected int halfX() {
        return 30;
    }

    @Override
    protected int halfZ() {
        return 30;
    }

    @Override
    protected void build() {
        room(Blocks.GRASS_BLOCK, Blocks.MOSSY_STONE_BRICKS, Blocks.COARSE_DIRT);
        // Central camp: stone walls with four gates around the Void Knight.
        for (int d = -6; d <= 6; d++) {
            for (int[] side : new int[][]{{d, -6}, {d, 6}, {-6, d}, {6, d}}) {
                if (Math.abs(d) > 1) {
                    set(side[0], 1, side[1], Blocks.STONE_BRICK_WALL.getDefaultState());
                } else {
                    set(side[0], 1, side[1], Blocks.OAK_FENCE_GATE.getDefaultState());
                }
            }
        }
        // Landing dock in the north where the lander arrives.
        for (int x = -3; x <= 3; x++) {
            for (int z = -28; z <= -24; z++) {
                set(x, 0, z, Blocks.SPRUCE_PLANKS.getDefaultState());
            }
        }
        place(0, -28, "minigame_exit");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayerEntity player, int index) {
        return rel(index % 5 - 2, 1, -25);
    }

    @Override
    protected void start() {
        knight = spawn("void_knight", rel(0, 1, 0));
        for (int i = 0; i < 4; i++) {
            portals[i] = spawn(PestControl.PORTALS[i], rel(PORTAL_POS[i][0], 1, PORTAL_POS[i][1]));
            if (portals[i] != null) {
                setHitpoints(portals[i], PestControl.PORTAL_HP[tier]);
                portals[i].addCommandTag(MinigameCombat.INVULNERABLE);
            }
        }
        broadcast("You must defend the Void Knight while the portals are unsummoned. The ritual takes twenty minutes.");
    }

    @Override
    protected void gameTick() {
        if (knight == null || !knight.isAlive()) {
            broadcastRed("The Void Knight was killed, another of our Order has fallen and that Island is lost.");
            end(false);
            return;
        }
        if (ticks >= PestControl.GAME_TICKS) {
            won = true;
            end(true);
            return;
        }
        // Shields drop one at a time in random order.
        if (ticks % PestControl.SHIELD_INTERVAL == 0) {
            List<Integer> up = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                if (shielded[i] && portals[i] != null && portals[i].isAlive()) {
                    up.add(i);
                }
            }
            if (!up.isEmpty()) {
                int i = up.get(random.nextInt(up.size()));
                shielded[i] = false;
                portals[i].getCommandTags().remove(MinigameCombat.INVULNERABLE);
                broadcastRed("The " + colour(i) + ", " + PORTAL_DIR[i] + " portal shield has dropped!");
            }
        }
        List<LivingEntity> pests = pests();
        int cap = 10 + 6 * Math.max(1, playerCount());
        if (ticks % 8 == 0 && pests.size() < cap) {
            for (int i = 0; i < 4; i++) {
                if (portals[i] != null && portals[i].isAlive()) {
                    String id = PestControl.PESTS[random.nextInt(PestControl.PESTS.length)];
                    MobEntity pest = spawn(id, portals[i].getBlockPos().add(random.nextInt(5) - 2, 0, random.nextInt(5) - 2));
                    if (pest != null) {
                        setHitpoints(pest, (int) Math.max(5, pest.getMaxHealth() * PestControl.PEST_SCALE[tier]));
                        if (random.nextBoolean()) {
                            aggro(pest);
                        }
                    }
                }
            }
        }
        for (LivingEntity e : pests) {
            if (!(e instanceof MobEntity pest)) {
                continue;
            }
            String cid = MinigameManager.creatureId(pest);
            if (cid.equals("spinner") && ticks % 5 == 0) {
                for (MobEntity portal : portals) {
                    if (portal != null && portal.isAlive() && portal.squaredDistanceTo(pest) < 25 && portal.getHealth() < portal.getMaxHealth()) {
                        portal.heal(5 + random.nextInt(6));
                        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, portal.getX(), portal.getY() + 2, portal.getZ(), 3, 0.5, 0.5, 0.5, 0);
                    }
                }
                continue;
            }
            boolean engaged = pest.getTarget() instanceof ServerPlayerEntity t && t.squaredDistanceTo(pest) < 64;
            if (engaged) {
                continue;
            }
            double d = pest.squaredDistanceTo(knight);
            if (d > 9) {
                pest.getNavigation().startMovingTo(knight.getX(), knight.getY(), knight.getZ(), 1.0);
            } else if (random.nextInt(3) == 0) {
                int max = Math.max(1, MonsterStats.of(pest).maxHit());
                pest.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                CombatHandler.applyDamage(pest, knight, random.nextInt(max + 1));
                if (cid.equals("splatter")) {
                    pest.kill();
                }
            }
        }
    }

    private List<LivingEntity> pests() {
        List<LivingEntity> list = new ArrayList<>();
        for (LivingEntity e : living(null)) {
            String cid = MinigameManager.creatureId(e);
            if (!cid.equals("void_knight") && !cid.endsWith("_portal")) {
                list.add(e);
            }
        }
        return list;
    }

    private static String colour(int i) {
        return PestControl.PORTALS[i].replace("_portal", "");
    }

    @Override
    protected void onPlayerHit(ServerPlayerEntity player, LivingEntity target, int dealt) {
        activity.merge(player.getUuid(), dealt, Integer::sum);
    }

    @Override
    protected void onKill(ServerPlayerEntity player, LivingEntity entity, String creatureId) {
        for (int i = 0; i < 4; i++) {
            if (entity == portals[i]) {
                broadcastRed("The " + colour(i) + ", " + PORTAL_DIR[i] + " portal has been destroyed!");
                activity.merge(player.getUuid(), 25, Integer::sum);
                if (knight != null) {
                    knight.heal(50);
                }
            }
        }
        boolean allDead = true;
        for (MobEntity p : portals) {
            if (p != null && p.isAlive() && p != entity) {
                allDead = false;
            }
        }
        if (allDead) {
            won = true;
            end(true);
        }
    }

    @Override
    protected void onDeath(ServerPlayerEntity player) {
        Msg.red(player, "Oh dear, you are dead! You return to the lander.");
        BlockPos at = spawnPoint(player, 0);
        player.teleport(world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, player.getYaw(), player.getPitch());
    }

    @Override
    protected void onEnd(boolean success) {
        for (ServerPlayerEntity p : online()) {
            int act = activity.getOrDefault(p.getUuid(), 0);
            if (success && won && act >= PestControl.MIN_ACTIVITY) {
                int pts = PestControl.POINTS[tier] + extraPoints.applyAsInt(p);
                addPoints(p, MinigameCurrencies.PEST_CONTROL, pts);
                reward(p, "coins", com.runecraft.player.Players.data(p).skills().combatLevel() * 10);
                int total = MinigameData.of(p).points(p.getUuid(), "pest_control");
                MinigameData.of(p).incrementStat(p.getUuid(), "kc:" + game.id());
                Msg.game(p, "Congratulations! You managed to destroy all the portals! We've awarded you " + pts
                        + " Void Knight Commendation points. You now have " + total + " points.");
            } else if (success && won) {
                Msg.game(p, "The Void Knights notice your lack of zeal. You receive no points this time.");
            } else {
                Msg.game(p, "The portals were not destroyed in time.");
            }
        }
    }

    @Override
    protected List<String> hud(ServerPlayerEntity player) {
        List<String> lines = new ArrayList<>();
        lines.add("Pest Control (" + PestControl.TIERS[tier] + ")");
        lines.add("Time left: " + time(PestControl.GAME_TICKS - ticks));
        lines.add("Void Knight: " + (knight == null ? 0 : (int) knight.getHealth()) + " / " + PestControl.VOID_KNIGHT_HP);
        StringBuilder sb = new StringBuilder("Portals: ");
        for (int i = 0; i < 4; i++) {
            MobEntity p = portals[i];
            sb.append(p == null || !p.isAlive() ? "X" : (shielded[i] ? "S" : String.valueOf((int) p.getHealth()))).append(i < 3 ? " / " : "");
        }
        lines.add(sb.toString());
        lines.add("Damage dealt: " + activity.getOrDefault(player.getUuid(), 0));
        return lines;
    }
}
