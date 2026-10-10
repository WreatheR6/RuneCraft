package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.collection.CollectionLog;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModEntities;
import com.runecraft.skill.Drops;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.List;

/** Shared helpers for boss scripts: messages, particles, minions, form swaps and kill credit. */
final class BossFx {
    private BossFx() {
    }

    static ServerWorld world(Entity e) {
        return (ServerWorld) e.getWorld();
    }

    /** Living, non-spectating players within {@code radius} blocks of the boss. */
    static List<ServerPlayerEntity> players(Entity boss, double radius) {
        return world(boss).getPlayers(p -> p.isAlive() && !p.isSpectator() && !p.isCreative()
                && p.squaredDistanceTo(boss) <= radius * radius);
    }

    /** Boss shout (red chat line) to everyone near the boss. */
    static void say(Entity boss, String message) {
        for (ServerPlayerEntity p : world(boss).getPlayers(p -> p.squaredDistanceTo(boss) < 48 * 48)) {
            p.sendMessage(Text.literal(message).formatted(Formatting.RED), false);
        }
    }

    /** Action-bar warning to everyone near the boss (used for telegraphs). */
    static void warn(Entity boss, String message) {
        for (ServerPlayerEntity p : world(boss).getPlayers(p -> p.squaredDistanceTo(boss) < 48 * 48)) {
            Msg.action(p, message);
        }
    }

    static void burst(Entity e, ParticleEffect effect, int count, double spread) {
        world(e).spawnParticles(effect, e.getX(), e.getY() + e.getHeight() / 2, e.getZ(), count, spread, spread, spread, 0.05);
    }

    static void column(ServerWorld world, Vec3d pos, ParticleEffect effect, int height) {
        for (int i = 0; i < height; i++) {
            world.spawnParticles(effect, pos.x, pos.y + i * 0.5, pos.z, 2, 0.2, 0.1, 0.2, 0.01);
        }
    }

    static void ring(ServerWorld world, Vec3d centre, double radius, ParticleEffect effect) {
        int points = (int) Math.max(8, radius * 8);
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points;
            world.spawnParticles(effect, centre.x + Math.cos(a) * radius, centre.y + 0.1, centre.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
    }

    static void sound(Entity e, net.minecraft.sound.SoundEvent sound, float pitch) {
        world(e).playSound(null, e.getX(), e.getY(), e.getZ(), sound, SoundCategory.HOSTILE, 1.0f, pitch);
    }

    /** Ground position (top solid block + 1) at a horizontal location. */
    static Vec3d ground(ServerWorld world, double x, double z, double fallbackY) {
        int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
        double y = Math.abs(top - fallbackY) > 6 ? fallbackY : top;
        return new Vec3d(x, y, z);
    }

    /** Spawns a module creature (minion) at a position, targeting {@code target}. */
    static MobEntity spawnMinion(Entity boss, String creatureId, Vec3d pos, ServerPlayerEntity target) {
        EntityType<?> type = ModEntities.typeById(creatureId);
        if (type == null) {
            return null;
        }
        ServerWorld world = world(boss);
        Entity e = type.create(world);
        if (!(e instanceof MobEntity mob)) {
            return null;
        }
        mob.refreshPositionAndAngles(pos.x, pos.y, pos.z, boss.getYaw(), 0);
        mob.initialize(world, world.getLocalDifficulty(mob.getBlockPos()), SpawnReason.MOB_SUMMONED, null, null);
        if (target != null) {
            mob.setTarget(target);
        }
        world.spawnEntity(mob);
        world.spawnParticles(ParticleTypes.POOF, pos.x, pos.y + 0.5, pos.z, 10, 0.3, 0.3, 0.3, 0.02);
        return mob;
    }

    /** Spawns another data boss (companion or next form) at a position. */
    static DataBossEntity spawnBoss(Entity near, String bossId, BlockPos pos, ServerPlayerEntity target) {
        BossDef def = ContentRegistry.boss(bossId);
        if (def == null) {
            return null;
        }
        return DataBossService.spawn(world(near), pos, def, target);
    }

    /**
     * Replaces a boss with another form sharing its hitpoints (or full health when {@code fullHealth}).
     * {@code state} is handed to the new form's script through {@link DataBossEntity#instanceTag}.
     */
    static DataBossEntity swapForm(DataBossEntity boss, String bossId, BlockPos at, boolean fullHealth, String state) {
        ServerPlayerEntity target = boss.getTarget() instanceof ServerPlayerEntity p ? p : null;
        DataBossEntity next = spawnBoss(boss, bossId, at, target);
        if (next == null) {
            return null;
        }
        if (!fullHealth) {
            next.setHealth(Math.max(1f, Math.min(next.getMaxHealth(), boss.getHealth() / boss.getMaxHealth() * next.getMaxHealth())));
        }
        next.instanceTag = state;
        burst(boss, ParticleTypes.CLOUD, 30, 1.0);
        boss.discard();
        return next;
    }

    /**
     * Awards a kill of {@code mainId} (drops, collection log, pet, kill count) when a secondary form of the
     * boss is defeated, then removes the form. Used by Zulrah's, Hydra's and the Kalphite Queen's later forms.
     */
    static void creditKill(DataBossEntity form, ServerPlayerEntity killer, String mainId) {
        BossDef def = ContentRegistry.boss(mainId);
        if (def == null || killer == null) {
            form.discard();
            return;
        }
        for (Drops.Rolled r : Drops.roll(def.drops, form.getRandom(), false)) {
            form.dropStack(Inv.stack(r.item(), r.count()));
            CollectionLog.obtain(killer, r.item());
        }
        if (def.pet != null && form.getRandom().nextInt(3000) == 0) {
            Inv.give(killer, def.pet, 1);
            CollectionLog.obtain(killer, def.pet);
            killer.getServer().getPlayerManager().broadcast(Text.literal(killer.getName().getString()
                    + " has a funny feeling like they're being followed.").formatted(Formatting.GOLD), false);
        }
        PlayerData data = Players.data(killer);
        int kc = data.incrementKillCount(def.id);
        Msg.game(killer, "Your " + def.name + " kill count is: " + kc + ".");
        burst(form, ParticleTypes.EXPLOSION, 6, 1.0);
        sound(form, SoundEvents.ENTITY_GENERIC_EXPLODE, 0.8f);
        form.discard();
    }

    /** Switches off all of a player's prayers. */
    static void disablePrayers(ServerPlayerEntity p, String message) {
        PlayerData data = Players.data(p);
        if (!data.activePrayers().isEmpty()) {
            data.activePrayers().clear();
            ServerNetworking.sync(p);
            Msg.red(p, message);
        }
    }

    static boolean protects(ServerPlayerEntity p, Prayer prayer) {
        return Players.data(p).isActive(prayer);
    }

    static void drainPrayer(ServerPlayerEntity p, int points) {
        PlayerData data = Players.data(p);
        data.setPrayerPoints(data.prayerPoints() - points);
        ServerNetworking.sync(p);
    }

    static void drainStat(ServerPlayerEntity p, Skill skill, int amount) {
        PlayerData data = Players.data(p);
        data.skills().setBoost(skill, data.skills().boost(skill) - amount);
        ServerNetworking.sync(p);
    }

    /** Stuns/binds a player for {@code gameTicks} (heavy slowness, like a freeze). */
    static void stun(ServerPlayerEntity p, int gameTicks) {
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, gameTicks * 12, 10, false, true));
    }

    static void unstun(ServerPlayerEntity p) {
        p.removeStatusEffect(StatusEffects.SLOWNESS);
    }

    static String heldId(ServerPlayerEntity p) {
        String id = Inv.idOf(p.getMainHandStack());
        return id == null ? "" : id;
    }
}
