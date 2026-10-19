package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.TzHaarWaves;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;

/**
 * Shared wave logic for the Fight Cave and the Inferno: waves spawn at the cave's spawn points
 * once the previous wave is dead, healers (Yt-HurKot) tend to the Jads unless distracted, and
 * Yt-MejKot heal their allies.
 */
public abstract class TzHaarSession extends MinigameSession {
    protected int wave;
    protected int nextWaveIn = 8;
    protected final List<Integer> healedJads = new ArrayList<>();
    /** Combat Achievement conditions judged over the run (healers healing, Jal-ImKot digging, pillars falling). */
    protected final java.util.Set<String> caRunFlags = new java.util.HashSet<>();
    protected static final String DISTRACTED = "rcmg_distracted";

    protected TzHaarSession(Minigame game, int id, int slot, ServerLevel world, String variant) {
        super(game, id, slot, world, variant);
    }

    protected abstract int totalWaves();

    protected abstract List<String> waveMonsters(int wave);

    protected abstract BlockPos[] spawnPoints();

    /** Spawns one wave creature; subclasses override for bosses. */
    protected Mob spawnWaveCreature(String id, BlockPos at) {
        return spawn(id, at);
    }

    @Override
    protected int halfX() {
        return 20;
    }

    @Override
    protected int halfZ() {
        return 20;
    }

    @Override
    protected int height() {
        return 12;
    }

    protected void caveRoom() {
        room(Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.GILDED_BLACKSTONE);
        for (int i = 0; i < 10; i++) {
            int x = random.nextInt(34) - 17;
            int z = random.nextInt(34) - 17;
            if (Math.abs(x) + Math.abs(z) > 6) {
                set(x, 1, z, Blocks.BASALT.defaultBlockState());
                if (random.nextBoolean()) {
                    set(x, 2, z, Blocks.BASALT.defaultBlockState());
                }
            }
        }
        place(0, 17, "minigame_exit");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
        return rel(index % 3 - 1, 1, 15);
    }

    @Override
    protected void gameTick() {
        healers();
        if (wave > 0 && mobsAlive() > 0) {
            return;
        }
        if (wave >= totalWaves()) {
            return;
        }
        if (--nextWaveIn > 0) {
            return;
        }
        wave++;
        nextWaveIn = 8;
        broadcastRed("Wave: " + wave);
        BlockPos[] points = spawnPoints();
        List<String> monsters = waveMonsters(wave);
        for (int i = 0; i < monsters.size(); i++) {
            BlockPos at = points[(i + random.nextInt(points.length)) % points.length];
            Mob mob = spawnWaveCreature(monsters.get(i), at);
            if (mob != null) {
                aggro(mob);
            }
        }
    }

    @Override
    public java.util.Set<String> caFlags(ServerPlayer player) {
        return new java.util.HashSet<>(caRunFlags);
    }

    /** Yt-HurKot heal their Jad unless a player distracted them; Yt-MejKot heal nearby allies. */
    protected void healers() {
        if (ticks % TzHaarWaves.HURKOT_HEAL_TICKS != 0) {
            return;
        }
        List<LivingEntity> all = living(null);
        for (LivingEntity e : all) {
            String cid = com.runecraft.content.minigames.MinigameManager.creatureId(e);
            if (cid.equals("yt_hurkot") && e instanceof Mob healer && !healer.getTags().contains(DISTRACTED)) {
                LivingEntity jad = nearestJad(all, healer);
                if (jad != null) {
                    if (healer.distanceToSqr(jad) > 9) {
                        healer.getNavigation().moveTo(jad.getX(), jad.getY(), jad.getZ(), 1.0);
                    } else if (jad.getHealth() < jad.getMaxHealth()) {
                        jad.heal(TzHaarWaves.hurKotHeal(com.runecraft.content.minigames.MinigameManager.creatureId(jad), random));
                        caRunFlags.add(com.runecraft.osrs.content.systems.CombatAchievements.FLAG_HURKOT_HEALED);
                        world.sendParticles(ParticleTypes.HEART, jad.getX(), jad.getY() + jad.getBbHeight(), jad.getZ(), 2, 0.4, 0.3, 0.4, 0);
                    }
                }
            } else if (cid.equals("yt_mejkot")) {
                for (LivingEntity ally : all) {
                    if (ally != e && ally.distanceToSqr(e) < 16 && ally.getHealth() < ally.getMaxHealth()) {
                        ally.heal(2);
                        caRunFlags.add(com.runecraft.osrs.content.systems.CombatAchievements.FLAG_MEJKOT_HEALED);
                    }
                }
            }
        }
    }

    private static LivingEntity nearestJad(List<LivingEntity> all, LivingEntity from) {
        LivingEntity best = null;
        for (LivingEntity e : all) {
            String cid = com.runecraft.content.minigames.MinigameManager.creatureId(e);
            if ((cid.equals("tztok_jad") || cid.equals("jaltok_jad"))
                    && (best == null || e.distanceToSqr(from) < best.distanceToSqr(from))) {
                best = e;
            }
        }
        return best;
    }

    /** Spawns healers around a Jad the first time it drops below half hitpoints. */
    protected void checkJadHealers(String jadId, int healers) {
        for (LivingEntity jad : living(jadId)) {
            if (jad.getHealth() < jad.getMaxHealth() / 2 && !healedJads.contains(jad.getId())) {
                healedJads.add(jad.getId());
                for (int i = 0; i < healers; i++) {
                    Mob h = spawn("yt_hurkot", jad.blockPosition().offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2));
                    if (h != null) {
                        h.setTarget(null);
                    }
                }
                broadcastRed("Yt-HurKot healers rush to " + jad.getName().getString() + "'s side!");
            }
        }
    }

    @Override
    protected void onPlayerHit(ServerPlayer player, LivingEntity target, int dealt) {
        if (target instanceof Mob mob && com.runecraft.content.minigames.MinigameManager.creatureId(mob).equals("yt_hurkot")
                && !mob.getTags().contains(DISTRACTED)) {
            mob.addTag(DISTRACTED);
            mob.setTarget(player);
            Msg.action(player, "The Yt-HurKot turns its attention to you.");
        }
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        return List.of(game.displayName(), "Wave: " + Math.max(1, wave) + " / " + totalWaves(), "Monsters: " + mobsAlive());
    }
}
