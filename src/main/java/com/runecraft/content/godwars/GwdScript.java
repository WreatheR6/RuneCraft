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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;

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
    protected record Strike(ServerLevel world, Vec3 pos, long dueGameTick, int maxHit, double radius, ParticleOptions warning,
                            ParticleOptions blast, BiConsumer<ServerPlayer, Integer> onHit, String name) {
    }

    protected AttackDef current;
    protected long chosenAt = -1000;
    private boolean spreading;
    private final List<Strike> strikes = new ArrayList<>();

    /** Chooses the next attack (called only when the previous one has landed). Null = engine default. */
    protected AttackDef pick(DataBossEntity boss, ServerPlayer target, double distance) {
        return null;
    }

    /** Whether this attack also hits every other player near the boss. */
    protected boolean isMultiTarget(AttackDef attack) {
        return false;
    }

    /** Hook called when an attack lands on a player; returns the damage to deal. */
    protected int landed(DataBossEntity boss, ServerPlayer player, int damage, AttackDef attack) {
        return damage;
    }

    /** Distance (centre to centre) within which this boss can melee. */
    protected static double meleeReach(DataBossEntity boss) {
        return boss.getBbWidth() / 2 + 1.6;
    }

    @Override
    public AttackDef chooseAttack(DataBossEntity boss, ServerPlayer target, double distance) {
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
    public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
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
    protected void splash(DataBossEntity boss, AttackDef attack, ServerPlayer except) {
        spreading = true;
        try {
            for (ServerPlayer p : players(boss, 20)) {
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
                s.world().sendParticles(s.warning(), s.pos().x, s.pos().y + 0.1, s.pos().z, 8, s.radius() / 2, 0.05,
                        s.radius() / 2, 0.01);
            }
        }
        for (Strike s : due) {
            if (s.blast() != null) {
                s.world().sendParticles(s.blast(), s.pos().x, s.pos().y + 0.5, s.pos().z, 12, s.radius() / 2, 0.4,
                        s.radius() / 2, 0.02);
            }
            if (!boss.isAlive()) {
                continue;
            }
            for (ServerPlayer p : s.world().getPlayers(pl -> pl.isAlive() && !pl.isSpectator()
                    && pl.position().distanceToSqr(s.pos()) <= s.radius() * s.radius())) {
                int dmg = boss.getRandom().nextInt(s.maxHit() + 1);
                if (s.name() != null) {
                    com.runecraft.combat.CombatHooks.nameAttack(s.name(), null); // "no damage from ..." Combat Achievements
                }
                int dealt = CombatHandler.applyDamage(boss, p, dmg);
                if (s.onHit() != null) {
                    s.onHit().accept(p, dealt);
                }
            }
        }
    }

    /** Marks a spot that explodes after {@code delay} game ticks. */
    protected void strike(DataBossEntity boss, Vec3 pos, int delay, int maxHit, double radius, ParticleOptions warning,
                          ParticleOptions blast, BiConsumer<ServerPlayer, Integer> onHit) {
        strike(null, boss, pos, delay, maxHit, radius, warning, blast, onHit);
    }

    /** {@link #strike} whose damage carries an attack name (e.g. "green explosion", for the "no damage from" tasks). */
    protected void strike(String name, DataBossEntity boss, Vec3 pos, int delay, int maxHit, double radius, ParticleOptions warning,
                          ParticleOptions blast, BiConsumer<ServerPlayer, Integer> onHit) {
        strikes.add(new Strike((ServerLevel) boss.level(), pos, ActionManager.gameTick() + delay, maxHit, radius, warning, blast,
                onHit, name));
    }

    protected static List<ServerPlayer> players(DataBossEntity boss, double radius) {
        return ((ServerLevel) boss.level()).getPlayers(p -> p.isAlive() && !p.isSpectator() && !p.isCreative()
                && p.distanceToSqr(boss) <= radius * radius);
    }

    /** Overhead-style shout shown to nearby players. */
    protected static void shout(DataBossEntity boss, String text) {
        BossDef def = boss.def();
        String name = def == null ? "Boss" : def.name;
        for (ServerPlayer p : players(boss, 40)) {
            p.displayClientMessage(Component.literal(name + ": " + text).withStyle(ChatFormatting.YELLOW), false);
        }
    }

    /** Spawns a module creature next to the boss and sets it on the boss's target. */
    protected static Mob spawnMinion(DataBossEntity boss, String id, double dx, double dz) {
        EntityType<?> type = ModEntities.typeById(id);
        if (type == null) {
            return null;
        }
        ServerLevel world = (ServerLevel) boss.level();
        var e = type.create(world);
        if (!(e instanceof Mob mob)) {
            return null;
        }
        mob.moveTo(boss.getX() + dx, boss.getY(), boss.getZ() + dz, boss.getRandom().nextFloat() * 360f, 0);
        mob.finalizeSpawn(world, world.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        if (boss.getTarget() != null) {
            mob.setTarget(boss.getTarget());
        }
        world.addFreshEntity(mob);
        return mob;
    }

    @Override
    public void onDeath(DataBossEntity boss, ServerPlayer killer) {
        topUpPet(boss, killer);
    }

    /** Extra pet roll so the overall chance equals the boss's real OSRS pet rate. */
    protected static void topUpPet(DataBossEntity boss, ServerPlayer killer) {
        BossDef def = boss.def();
        if (def == null || def.pet == null || killer == null) {
            return;
        }
        double chance = GodWarsFactions.extraPetChance(WildernessBosses.realPetRate(def.id));
        if (chance > 0 && boss.getRandom().nextDouble() < chance) {
            givePet(killer, def.pet);
        }
    }

    static void givePet(ServerPlayer player, String pet) {
        Inv.give(player, pet, 1);
        CollectionLog.obtain(player, pet);
        player.getServer().getPlayerList().broadcastSystemMessage(Component.literal(player.getName().getString()
                + " has a funny feeling like they're being followed.").withStyle(ChatFormatting.GOLD), false);
    }
}
