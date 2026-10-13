package com.runecraft.content.minigames.games;

import com.runecraft.combat.CombatHandler;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameRules.BarbarianAssault;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Barbarian Assault: ten waves of Penance fighters, rangers, runners and healers, then the
 * Penance Queen. Each player picks a role at its entrance: Attackers hit fighters and rangers
 * harder, Defenders stop runners (runners escaping costs the team), Collectors gather penance eggs
 * to load into the egg launcher (which blasts the Queen), Healers heal the team at the spring and
 * crush the penance healers. Clearing waves earns honour points in your role.
 */
public class BarbarianAssaultSession extends MinigameSession {
    private final Map<UUID, Integer> roles = new HashMap<>();
    private int wave;
    private long waveStart;
    private int nextWaveIn = 10;
    private int escaped;
    private boolean cleared = true;
    private MobEntity queen;

    public BarbarianAssaultSession(int id, int slot, ServerWorld world, String variant) {
        super(Minigame.BARBARIAN_ASSAULT, id, slot, world, variant);
    }

    @Override
    protected int halfX() {
        return 22;
    }

    @Override
    protected int halfZ() {
        return 22;
    }

    @Override
    protected void build() {
        room(Blocks.COARSE_DIRT, Blocks.SPRUCE_LOG, Blocks.MUD_BRICKS);
        for (int x = -4; x <= 4; x++) {
            set(x, 1, -19, Blocks.AIR.getDefaultState());
            set(x, 0, -21, Blocks.BLACKSTONE.getDefaultState());
        }
        place(-4, 16, "ba_egg_launcher");
        place(4, 16, "ba_healer_spring");
        place(0, 20, "minigame_exit");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayerEntity player, int index) {
        return rel(index % 5 - 2, 1, 17);
    }

    private int role(ServerPlayerEntity p) {
        return roles.getOrDefault(p.getUuid(), 0);
    }

    private static String roleName(int r) {
        String n = BarbarianAssault.ROLES[r];
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    @Override
    protected void start() {
        for (ServerPlayerEntity p : online()) {
            String v = MinigameManager.chosenVariant(p);
            int r = 0;
            for (int i = 0; i < BarbarianAssault.ROLES.length; i++) {
                if (v.contains(BarbarianAssault.ROLES[i])) {
                    r = i;
                }
            }
            roles.put(p.getUuid(), r);
            Msg.game(p, "Your role this game is: " + roleName(r) + ".");
        }
    }

    @Override
    protected void gameTick() {
        if (wave > 0) {
            // Runners escape into the cave if not stopped within a minute.
            if (ticks - waveStart > 100) {
                for (LivingEntity runner : living("penance_runner")) {
                    runner.discard();
                    escaped++;
                    broadcastRed("A penance runner has escaped!");
                }
            }
            for (LivingEntity runner : living("penance_runner")) {
                if (runner instanceof MobEntity m && m.getNavigation().isIdle()) {
                    BlockPos to = randomFloor(0, -10, 10);
                    m.getNavigation().startMovingTo(to.getX(), to.getY(), to.getZ(), 1.2);
                }
            }
            if (queen != null && queen.isAlive()) {
                return;
            }
            if (mobsAlive() > 0) {
                return;
            }
            if (wave >= BarbarianAssault.WAVES) {
                return;
            }
            if (!cleared) {
                cleared = true;
                completeWave();
            }
        }
        if (--nextWaveIn > 0) {
            return;
        }
        nextWaveIn = 10;
        cleared = false;
        wave++;
        waveStart = ticks;
        broadcastRed("Wave " + wave + " begins!");
        int[] counts = BarbarianAssault.wave(wave);
        for (int type = 0; type < 4; type++) {
            for (int n = 0; n < counts[type]; n++) {
                MobEntity mob = spawn(BarbarianAssault.PENANCE[type], randomFloor(0, -17, 4));
                if (mob != null) {
                    setHitpoints(mob, BarbarianAssault.hitpoints(type, wave));
                    if (type != 2) {
                        aggro(mob);
                    }
                }
            }
        }
        if (wave == BarbarianAssault.WAVES) {
            queen = spawn("penance_queen", rel(0, 1, -16));
            if (queen != null) {
                aggro(queen);
            }
            broadcastRed("The Penance Queen emerges! Collectors, load eggs into the launcher!");
        }
    }

    private void completeWave() {
        for (ServerPlayerEntity p : online()) {
            int r = role(p);
            int honour = giveHonour(p, r, BarbarianAssault.waveHonour(r, wave, escaped));
            Msg.game(p, "Wave " + wave + " complete! You earn " + honour + " " + roleName(r) + " honour points"
                    + (escaped > 0 ? " (" + escaped + " runner" + (escaped > 1 ? "s" : "") + " escaped)." : "."));
        }
        escaped = 0;
    }

    /** Adds honour points in a role, capped at 5,000; returns the amount added. */
    private int giveHonour(ServerPlayerEntity p, int role, int amount) {
        String key = "ba_" + BarbarianAssault.ROLES[role];
        int add = BarbarianAssault.addHonour(MinigameData.of(p).points(p.getUuid(), key), amount);
        addPoints(p, "pts:" + key, add);
        return add;
    }

    @Override
    protected double damageMultiplier(ServerPlayerEntity player, LivingEntity target, AttackType type) {
        String cid = MinigameManager.creatureId(target);
        return switch (role(player)) {
            case 0 -> cid.equals("penance_fighter") || cid.equals("penance_ranger") ? 1.25 : 1.0;
            case 1 -> cid.equals("penance_runner") ? 2.0 : 1.0;
            case 3 -> cid.equals("penance_healer") ? 1.5 : 1.0;
            default -> 1.0;
        };
    }

    @Override
    protected void onKill(ServerPlayerEntity player, LivingEntity entity, String creatureId) {
        if (creatureId.equals("penance_fighter") || creatureId.equals("penance_ranger")) {
            for (ServerPlayerEntity p : online()) {
                if (role(p) == 2) {
                    Inv.give(p, "penance_egg", 1 + random.nextInt(2));
                }
            }
            if (online().stream().noneMatch(p -> role(p) == 2)) {
                Inv.give(player, "penance_egg", 1);
            }
        } else if (creatureId.equals("penance_queen")) {
            broadcastRed("The Penance Queen has been defeated!");
            end(true);
        }
    }

    @Override
    protected boolean onBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
        if (blockId.equals("ba_egg_launcher")) {
            int eggs = Inv.remove(player, "penance_egg", 1000);
            if (eggs == 0) {
                Msg.game(player, "You need penance eggs to load the launcher.");
                return true;
            }
            if (queen != null && queen.isAlive()) {
                int dmg = eggs * BarbarianAssault.omegaEggDamage();
                CombatHandler.applyDamage(player, queen, dmg);
                world.spawnParticles(ParticleTypes.EXPLOSION, queen.getX(), queen.getY() + 1, queen.getZ(), eggs, 1, 1, 1, 0);
                Msg.game(player, "You fire " + eggs + " omega egg" + (eggs > 1 ? "s" : "") + " at the Penance Queen!");
                if (!queen.isAlive()) {
                    broadcastRed("The Penance Queen has been defeated!");
                    end(true);
                }
            } else {
                addPoints(player, "pts:ba_collector", eggs / 2);
                Msg.game(player, "You load " + eggs + " eggs into the cannon for the attackers. (+" + eggs / 2 + " collector points)");
            }
            return true;
        }
        if (blockId.equals("ba_healer_spring")) {
            int healed = 0;
            for (ServerPlayerEntity p : online()) {
                if (p.squaredDistanceTo(player) < 64 && p.getHealth() < p.getMaxHealth()) {
                    p.heal(role(player) == 3 ? 10 : 4);
                    healed++;
                }
            }
            if (role(player) == 3 && healed > 1) {
                addPoints(player, "pts:ba_healer", 1);
            }
            Msg.game(player, "You draw healing water from the spring.");
            return true;
        }
        return false;
    }

    @Override
    protected void onEnd(boolean success) {
        if (!success) {
            return;
        }
        for (ServerPlayerEntity p : online()) {
            int r = role(p);
            // Wave 10: 80 points in your role and 5 in each of the others.
            for (int other = 0; other < BarbarianAssault.ROLES.length; other++) {
                giveHonour(p, other, other == r ? BarbarianAssault.WAVE_10_ROLE : BarbarianAssault.WAVE_10_OTHER);
            }
            int kc = MinigameData.of(p).incrementStat(p.getUuid(), "kc:" + game.id());
            String queenKey = com.runecraft.osrs.content.minigames.MinigameCurrencies.key(
                    com.runecraft.osrs.content.minigames.MinigameCurrencies.BA_QUEEN);
            if (MinigameData.of(p).points(p.getUuid(), queenKey) < BarbarianAssault.MAX_QUEEN_KILLS) {
                addPoints(p, com.runecraft.osrs.content.minigames.MinigameCurrencies.BA_QUEEN, 1);
            }
            Msg.game(p, "Congratulations! You've defeated the Penance Queen. Queen kills: " + kc + ".");
            if (random.nextInt(BarbarianAssault.PET_CHANCE) == 0) {
                reward(p, "pet_penance_queen", 1);
                p.getServer().getPlayerManager().broadcast(Text.literal(p.getName().getString()
                        + " has a funny feeling like they're being followed.").formatted(Formatting.GOLD), false);
            }
            Inv.remove(p, "penance_egg", 1000);
        }
    }

    @Override
    protected List<String> hud(ServerPlayerEntity player) {
        int r = role(player);
        int honour = MinigameData.of(player).points(player.getUuid(), "ba_" + BarbarianAssault.ROLES[r]);
        return List.of("Barbarian Assault", "Wave: " + Math.max(1, wave) + " / " + BarbarianAssault.WAVES,
                "Role: " + roleName(r) + " (" + honour + " honour)", "Penance left: " + mobsAlive(),
                queen != null && queen.isAlive() ? "Queen: " + (int) queen.getHealth() + " / " + BarbarianAssault.QUEEN_HP : "");
    }
}
