package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.collection.CollectionLog;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModEntities;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Shared helpers for boss scripts: messages, particles, minions, form swaps and kill credit. */
final class BossFx {
    private BossFx() {
    }

    static ServerLevel world(Entity e) {
        return (ServerLevel) e.level();
    }

    /** Living, non-spectating players within {@code radius} blocks of the boss. */
    static List<ServerPlayer> players(Entity boss, double radius) {
        return world(boss).getPlayers(p -> p.isAlive() && !p.isSpectator() && !p.isCreative()
                && p.distanceToSqr(boss) <= radius * radius);
    }

    /** Boss shout (red chat line) to everyone near the boss. */
    static void say(Entity boss, String message) {
        for (ServerPlayer p : world(boss).getPlayers(p -> p.distanceToSqr(boss) < 48 * 48)) {
            p.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.RED), false);
        }
    }

    /**
     * Telegraph to everyone near the boss: the centre-screen warning line ({@link Msg#telegraph}), which eating messages and
     * status lines (the Whisperer's sanity) on the action bar no longer overwrite. // playability: OSRS shows these as
     * overhead text or a game message read while acting on ticks
     */
    static void warn(Entity boss, String message) {
        for (ServerPlayer p : world(boss).getPlayers(p -> p.distanceToSqr(boss) < 48 * 48)) {
            Msg.telegraph(p, message, null);
        }
    }

    /**
     * A telegraph on the cast bar of every player within {@code radius} blocks of the boss (round 42, {@code // playability}):
     * {@code label} (null: the boss's name), the style's colour and protection key ({@code style} null: an area hit to step
     * out of), and a countdown to {@code landInMc}. For scripts whose attack lands at a known tick; the warning text itself
     * still goes through {@link #warn}.
     */
    static void cast(Entity boss, String label, AttackType style, int landInMc, double radius) {
        int barStyle = com.runecraft.osrs.CastBar.styleOf(style);
        int flags = style == null ? com.runecraft.osrs.CastBar.FLAG_DODGE : 0;
        for (ServerPlayer p : players(boss, radius)) {
            com.runecraft.network.HudNet.cast(p, boss, barStyle, flags, landInMc, label);
        }
    }

    static void burst(Entity e, ParticleOptions effect, int count, double spread) {
        world(e).sendParticles(effect, e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(), count, spread, spread, spread, 0.05);
    }

    static void column(ServerLevel world, Vec3 pos, ParticleOptions effect, int height) {
        for (int i = 0; i < height; i++) {
            world.sendParticles(effect, pos.x, pos.y + i * 0.5, pos.z, 2, 0.2, 0.1, 0.2, 0.01);
        }
    }

    static void ring(ServerLevel world, Vec3 centre, double radius, ParticleOptions effect) {
        int points = (int) Math.max(8, radius * 8);
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points;
            world.sendParticles(effect, centre.x + Math.cos(a) * radius, centre.y + 0.1, centre.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
    }

    /**
     * A ground strike's marker ({@link com.runecraft.boss.StrikeMarkers#ring}, shared with the raids and the God Wars bosses):
     * a high-contrast dust outline at its true radius (the area that is hit), orange, brighter and redder in its last moments,
     * with the content's particle as a sparse fill. // playability: OSRS marks the tiles once and the hit is tile-exact; here
     * the outline is redrawn while the strike is pending and the hit test matches it. Not {@link #ring}: that one also marks
     * safe spots and stations (Doom's earthen shield, Zalcano's furnace), which must not turn danger-orange.
     */
    static void strikeRing(ServerLevel world, Vec3 centre, double radius, ParticleOptions fill, boolean landing, int points) {
        com.runecraft.boss.StrikeMarkers.ring(world, centre, radius, fill, landing, points);
    }

    static void sound(Entity e, net.minecraft.sounds.SoundEvent sound, float pitch) {
        world(e).playSound(null, e.getX(), e.getY(), e.getZ(), sound, SoundSource.HOSTILE, 1.0f, pitch);
    }

    /** Ground position (top solid block + 1) at a horizontal location. */
    static Vec3 ground(ServerLevel world, double x, double z, double fallbackY) {
        int top = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
        double y = Math.abs(top - fallbackY) > 6 ? fallbackY : top;
        return new Vec3(x, y, z);
    }

    /** Spawns a module creature (minion) at a position, targeting {@code target}. */
    static Mob spawnMinion(Entity boss, String creatureId, Vec3 pos, ServerPlayer target) {
        EntityType<?> type = ModEntities.typeById(creatureId);
        if (type == null) {
            return null;
        }
        ServerLevel world = world(boss);
        Entity e = type.create(world);
        if (!(e instanceof Mob mob)) {
            return null;
        }
        mob.moveTo(pos.x, pos.y, pos.z, boss.getYRot(), 0);
        mob.finalizeSpawn(world, world.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        if (target != null) {
            mob.setTarget(target);
        }
        if (boss instanceof DataBossEntity d) {
            d.claimMinion(mob); // a reset of the boss clears it
        } else if (boss instanceof com.runecraft.entity.BossEntity b) {
            b.claimMinion(mob);
        }
        world.addFreshEntity(mob);
        world.sendParticles(ParticleTypes.POOF, pos.x, pos.y + 0.5, pos.z, 10, 0.3, 0.3, 0.3, 0.02);
        return mob;
    }

    /** Spawns another data boss (companion or next form) at a position. */
    static DataBossEntity spawnBoss(Entity near, String bossId, BlockPos pos, ServerPlayer target) {
        BossDef def = ContentRegistry.boss(bossId);
        if (def == null) {
            return null;
        }
        DataBossEntity spawned = DataBossService.spawn(world(near), pos, def, target);
        if (spawned != null && near instanceof DataBossEntity from) {
            spawned.copyHomeFrom(from); // same lair: the same leash and reset as the boss that brought it
        }
        return spawned;
    }

    /**
     * Replaces a boss with another form sharing its hitpoints (or full health when {@code fullHealth}).
     * {@code state} is handed to the new form's script through {@link DataBossEntity#instanceTag}.
     */
    static DataBossEntity swapForm(DataBossEntity boss, String bossId, BlockPos at, boolean fullHealth, String state) {
        ServerPlayer target = boss.getTarget() instanceof ServerPlayer p ? p : null;
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
    static void creditKill(DataBossEntity form, ServerPlayer killer, String mainId) {
        BossDef def = ContentRegistry.boss(mainId);
        if (def == null || killer == null) {
            form.discard();
            return;
        }
        // the main boss's kill loot, through the drop transformers like any kill: where the form died, or under the killer (Zulrah)
        com.runecraft.boss.DataBossService.dropKillLoot(killer, form, def.id, def.drops);
        int petRate = com.runecraft.osrs.content.extras.Pets.engineRate(def.id);
        if (def.pet != null && petRate > 0 && form.getRandom().nextInt(petRate) == 0) {
            Inv.give(killer, def.pet, 1);
            CollectionLog.obtain(killer, def.pet);
            killer.getServer().getPlayerList().broadcastSystemMessage(Component.literal(killer.getName().getString()
                    + " has a funny feeling like they're being followed.").withStyle(ChatFormatting.GOLD), false);
        }
        PlayerData data = Players.data(killer);
        int kc = data.incrementKillCount(def.id);
        Msg.game(killer, "Your " + def.name + " kill count is: " + kc + ".");
        // The form is discarded rather than killed, so do here what a death does: the Slayer task (hydras, boss tasks), the
        // kill listeners (Combat Achievement kill counts, diaries, clue scrolls) and the form's own clean-up (snakelings...).
        com.runecraft.combat.MonsterStats stats = com.runecraft.combat.MonsterStats.from(def);
        com.runecraft.combat.Slayer.onKill(killer, form, stats);
        form.script().onDeath(form, killer);
        com.runecraft.content.GameEvents.fireKill(killer, form, stats);
        burst(form, ParticleTypes.EXPLOSION, 6, 1.0);
        sound(form, SoundEvents.GENERIC_EXPLODE.value(), 0.8f);
        form.discard();
    }

    /** Switches off all of a player's prayers. */
    static void disablePrayers(ServerPlayer p, String message) {
        PlayerData data = Players.data(p);
        if (!data.activePrayers().isEmpty()) {
            data.activePrayers().clear();
            ServerNetworking.sync(p);
            Msg.red(p, message);
        }
    }

    /** Switches off one prayer, leaving the others on (e.g. Phosani's Prayer Deactivation of a protection prayer). */
    static void disablePrayer(ServerPlayer p, Prayer prayer, String message) {
        if (Players.data(p).activePrayers().remove(prayer)) {
            ServerNetworking.sync(p);
            Msg.red(p, message);
        }
    }

    /**
     * Whether a prayer is up. The three protection prayers are read through the one protection chokepoint,
     * {@link com.runecraft.combat.CombatHandler#isProtected} (which the prayer scope gives its switch grace), so every boss
     * check sees the same answer as a monster's hit; other prayers are read directly.
     */
    static boolean protects(ServerPlayer p, Prayer prayer) {
        AttackType style = prayer == Prayer.PROTECT_FROM_MAGIC ? AttackType.MAGIC
                : prayer == Prayer.PROTECT_FROM_MISSILES ? AttackType.RANGED
                : prayer == Prayer.PROTECT_FROM_MELEE ? AttackType.CRUSH : null;
        if (style == null) {
            return Players.data(p).isActive(prayer);
        }
        return com.runecraft.combat.CombatHandler.isProtected(Players.data(p), style);
    }

    static void drainPrayer(ServerPlayer p, int points) {
        PlayerData data = Players.data(p);
        data.setPrayerPoints(data.prayerPoints() - points);
        ServerNetworking.sync(p);
    }

    static void drainStat(ServerPlayer p, Skill skill, int amount) {
        PlayerData data = Players.data(p);
        data.skills().setBoost(skill, data.skills().boost(skill) - amount);
        ServerNetworking.sync(p);
    }

    /** Stuns/binds a player for {@code gameTicks} (heavy slowness, like a freeze). */
    static void stun(ServerPlayer p, int gameTicks) {
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, gameTicks * 12, 10, false, true));
    }

    static void unstun(ServerPlayer p) {
        p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
    }

    static String heldId(ServerPlayer p) {
        String id = Inv.idOf(p.getMainHandItem());
        return id == null ? "" : id;
    }
}
