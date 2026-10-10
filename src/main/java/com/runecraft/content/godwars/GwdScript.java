package com.runecraft.content.godwars;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.BossScript;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.Mechanics;
import com.runecraft.collection.CollectionLog;
import com.runecraft.combat.CombatHandler;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.godwars.GodWarsFactions;
import com.runecraft.osrs.content.godwars.WildernessBosses;
import com.runecraft.registry.ModEntities;
import com.runecraft.util.Inv;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Shared machinery for this module's boss scripts:
 * <ul>
 *   <li>an attack "rotation": {@link #pick} decides the next attack once, and it is kept until it lands
 *       (the engine asks for a style every tick);</li>
 *   <li>multi-target attacks ({@link #isMultiTarget}) are repeated on every other player in the room;</li>
 *   <li>delayed ground strikes (shadow traps, explosive books...) that can be dodged by moving;</li>
 *   <li>pet top-up so the combined pet chance matches the real OSRS rate.</li>
 * </ul>
 */
abstract class GwdScript implements BossScript {
    /** A marked spot that explodes after a delay, hitting players within {@code radius} blocks. */
    protected record Strike(ServerWorld world, Vec3d pos, long dueGameTick, int maxHit, double radius, ParticleEffect warning,
                            ParticleEffect blast, BiConsumer<ServerPlayerEntity, Integer> onHit) {
    }

    protected AttackDef current;
    protected long chosenAt = -1000;
    private boolean spreading;
    private final List<Strike> strikes = new ArrayList<>();

    /** Chooses the next attack (called only when the previous one has landed). Null = engine default. */
    protected AttackDef pick(DataBossEntity boss, ServerPlayerEntity target, double distance) {
        return null;
    }

    /** Whether this attack also hits every other player near the boss. */
    protected boolean isMultiTarget(AttackDef attack) {
        return false;
    }

    /** Hook called when an attack lands on a player; returns the damage to deal. */
    protected int landed(DataBossEntity boss, ServerPlayerEntity player, int damage, AttackDef attack) {
        return damage;
    }

    /** Distance (centre to centre) within which this boss can melee. */
    protected static double meleeReach(DataBossEntity boss) {
        return boss.getWidth() / 2 + 1.6;
    }

    @Override
    public AttackDef chooseAttack(DataBossEntity boss, ServerPlayerEntity target, double distance) {
        long now = ActionManager.gameTick();
        BossDef def = boss.def();
        int speed = def == null ? 4 : def.attackSpeed;
        boolean stale = now - chosenAt > speed * 2L + 2;
        boolean outOfReach = current != null && current.melee() && distance > meleeReach(boss) + 2.5;
        if (current == null || stale || outOfReach) {
            current = pick(boss, target, distance);
            chosenAt = now;
        }
        return current;
    }

    @Override
    public int onHitPlayer(DataBossEntity boss, ServerPlayerEntity player, int damage, AttackType type) {
        if (spreading) {
            return landed(boss, player, damage, current);
        }
        AttackDef used = current;
        int result = landed(boss, player, damage, used);
        if (used != null && isMultiTarget(used)) {
            splash(boss, used, player);
        }
        current = null;
        return result;
    }

    /** Repeats an attack on every other player within 20 blocks (each hit is rolled separately). */
    protected void splash(DataBossEntity boss, AttackDef attack, ServerPlayerEntity except) {
        spreading = true;
        try {
            for (ServerPlayerEntity p : players(boss, 20)) {
                if (p != except) {
                    Mechanics.perform(boss, p, attack, 1.0);
                }
            }
        } finally {
            spreading = false;
        }
    }

    @Override
    public void gameTick(DataBossEntity boss, long gameTick) {
        Iterator<Strike> it = strikes.iterator();
        List<Strike> due = new ArrayList<>();
        while (it.hasNext()) {
            Strike s = it.next();
            if (gameTick >= s.dueGameTick()) {
                due.add(s);
                it.remove();
            } else if (s.warning() != null) {
                s.world().spawnParticles(s.warning(), s.pos().x, s.pos().y + 0.1, s.pos().z, 8, s.radius() / 2, 0.05,
                        s.radius() / 2, 0.01);
            }
        }
        for (Strike s : due) {
            if (s.blast() != null) {
                s.world().spawnParticles(s.blast(), s.pos().x, s.pos().y + 0.5, s.pos().z, 12, s.radius() / 2, 0.4,
                        s.radius() / 2, 0.02);
            }
            if (!boss.isAlive()) {
                continue;
            }
            for (ServerPlayerEntity p : s.world().getPlayers(pl -> pl.isAlive() && !pl.isSpectator()
                    && pl.getPos().squaredDistanceTo(s.pos()) <= s.radius() * s.radius())) {
                int dmg = boss.getRandom().nextInt(s.maxHit() + 1);
                int dealt = CombatHandler.applyDamage(boss, p, dmg);
                if (s.onHit() != null) {
                    s.onHit().accept(p, dealt);
                }
            }
        }
    }

    /** Marks a spot that explodes after {@code delay} game ticks. */
    protected void strike(DataBossEntity boss, Vec3d pos, int delay, int maxHit, double radius, ParticleEffect warning,
                          ParticleEffect blast, BiConsumer<ServerPlayerEntity, Integer> onHit) {
        strikes.add(new Strike((ServerWorld) boss.getWorld(), pos, ActionManager.gameTick() + delay, maxHit, radius, warning, blast,
                onHit));
    }

    protected static List<ServerPlayerEntity> players(DataBossEntity boss, double radius) {
        return ((ServerWorld) boss.getWorld()).getPlayers(p -> p.isAlive() && !p.isSpectator() && !p.isCreative()
                && p.squaredDistanceTo(boss) <= radius * radius);
    }

    /** Overhead-style shout shown to nearby players. */
    protected static void shout(DataBossEntity boss, String text) {
        BossDef def = boss.def();
        String name = def == null ? "Boss" : def.name;
        for (ServerPlayerEntity p : players(boss, 40)) {
            p.sendMessage(Text.literal(name + ": " + text).formatted(Formatting.YELLOW), false);
        }
    }

    /** Spawns a module creature next to the boss and sets it on the boss's target. */
    protected static MobEntity spawnMinion(DataBossEntity boss, String id, double dx, double dz) {
        EntityType<?> type = ModEntities.typeById(id);
        if (type == null) {
            return null;
        }
        ServerWorld world = (ServerWorld) boss.getWorld();
        var e = type.create(world);
        if (!(e instanceof MobEntity mob)) {
            return null;
        }
        mob.refreshPositionAndAngles(boss.getX() + dx, boss.getY(), boss.getZ() + dz, boss.getRandom().nextFloat() * 360f, 0);
        mob.initialize(world, world.getLocalDifficulty(mob.getBlockPos()), SpawnReason.MOB_SUMMONED, null, null);
        if (boss.getTarget() != null) {
            mob.setTarget(boss.getTarget());
        }
        world.spawnEntity(mob);
        return mob;
    }

    @Override
    public void onDeath(DataBossEntity boss, ServerPlayerEntity killer) {
        topUpPet(boss, killer);
    }

    /** Extra pet roll so the overall chance equals the boss's real OSRS pet rate. */
    protected static void topUpPet(DataBossEntity boss, ServerPlayerEntity killer) {
        BossDef def = boss.def();
        if (def == null || def.pet == null || killer == null) {
            return;
        }
        double chance = GodWarsFactions.extraPetChance(WildernessBosses.realPetRate(def.id));
        if (chance > 0 && boss.getRandom().nextDouble() < chance) {
            givePet(killer, def.pet);
        }
    }

    static void givePet(ServerPlayerEntity player, String pet) {
        Inv.give(player, pet, 1);
        CollectionLog.obtain(player, pet);
        player.getServer().getPlayerManager().broadcast(Text.literal(player.getName().getString()
                + " has a funny feeling like they're being followed.").formatted(Formatting.GOLD), false);
    }
}
