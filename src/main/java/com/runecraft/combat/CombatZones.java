package com.runecraft.combat;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.entity.BossEntity;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.osrs.MonsterAi;
import com.runecraft.osrs.MultiCombat;
import com.runecraft.osrs.SingleCombat;
import com.runecraft.osrs.world.SpawnTable;
import com.runecraft.world.Gielinor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Single-way and multi-way combat (rules in {@link SingleCombat}, areas in {@link MultiCombat}). Outside
 * Gielinor, in raids and in minigame instances every fight is multi.
 */
public final class CombatZones {
    private static final SingleCombat TRACKER = new SingleCombat();
    private static MinecraftServer server;

    private CombatZones() {
    }

    /** True if the entity stands in a multicombat area. */
    public static boolean isMulti(Entity e) {
        if (!Gielinor.is(e)) {
            return true;
        }
        if (com.runecraft.content.raids.RaidManager.at(e) != null) {
            return true;
        }
        if (e instanceof ServerPlayer p ? com.runecraft.content.minigames.MinigameManager.session(p) != null
                : com.runecraft.content.minigames.MinigameManager.owner(e) != null) {
            return true;
        }
        return MultiCombat.isMultiAtBlock(e.getBlockX(), e.getBlockY(), e.getBlockZ());
    }

    /** Bosses and their encounter monsters (minions, raid and minigame creatures) ignore single-way rules. */
    static boolean exempt(LivingEntity e) {
        if (e instanceof BossEntity || e instanceof DataBossEntity) {
            return true;
        }
        if (e instanceof OsrsMonsterEntity m && m.stats() != null && m.stats().profile() != null) {
            return SpawnTable.encounterOnly().containsKey(m.stats().profile().id);
        }
        return false;
    }

    /** Records a hit (or splash) for the single-way combat timers. */
    public static void onHit(LivingEntity attacker, LivingEntity target) {
        if (attacker == null || attacker == target || !Gielinor.is(target)) {
            return;
        }
        TRACKER.onHit(attacker.getId(), target.getId(), ActionManager.gameTick());
    }

    /** Whether {@code attacker} may start or continue attacking {@code target} under the single-way rules. */
    public static SingleCombat.Verdict check(LivingEntity attacker, LivingEntity target) {
        return refusal(attacker, target).verdict();
    }

    /** {@link #check} with the fighter that blocks the attack ({@link SingleCombat.Refusal#blocker}: an entity id, -1 if none). */
    public static SingleCombat.Refusal refusal(LivingEntity attacker, LivingEntity target) {
        if (attacker == null || target == null || exempt(attacker) || exempt(target)) {
            return SingleCombat.Refusal.NONE;
        }
        boolean multi = isMulti(target) || isMulti(attacker);
        boolean pvp = attacker instanceof ServerPlayer && target instanceof ServerPlayer;
        ServerLevel world = attacker.level() instanceof ServerLevel w ? w : null;
        java.util.function.IntPredicate holdsTarget = id -> engaged(world, id, target);
        java.util.function.IntPredicate holdsAttacker = id -> engaged(world, id, attacker);
        if (!multi && (singlesPlus(target) || singlesPlus(attacker))) {
            return TRACKER.refusalSinglesPlus(attacker.getId(), target.getId(), ActionManager.gameTick(), pvp, holdsTarget, holdsAttacker,
                    id -> isPlayer(world, id));
        }
        return TRACKER.refusal(attacker.getId(), target.getId(), ActionManager.gameTick(), multi, pvp, holdsTarget, holdsAttacker);
    }

    /**
     * Whether the recorded fighter {@code id} still holds {@code fighter} in single-way combat: alive, loaded and not a boss, and,
     * for a monster, still after {@code fighter} and near enough to fight it in sight (round 42, {@link MonsterAi#stillEngaged}:
     * // playability, OSRS keeps the lock 8 game ticks wherever the monster went). Players keep the OSRS lock.
     */
    private static boolean engaged(ServerLevel world, int id, LivingEntity fighter) {
        Entity e = find(world, id);
        if (!(e instanceof LivingEntity l) || !l.isAlive() || exempt(l)) {
            return false;
        }
        if (!(l instanceof net.minecraft.world.entity.Mob mob) || fighter == null) {
            return true;
        }
        if (mob.level() != fighter.level()) {
            return false;
        }
        return MonsterAi.stillEngaged(mob.getTarget() == fighter, mob.distanceTo(fighter), reach(mob), fighter.hasLineOfSight(mob));
    }

    /** How far a monster's attacks reach (blocks), for the single-way lock. */
    private static double reach(net.minecraft.world.entity.Mob mob) {
        if (mob instanceof OsrsMonsterEntity m) {
            return m.attackReach();
        }
        MonsterStats s = MonsterStats.of(mob);
        return s != null && !s.style().isMelee() ? 10.0 : com.runecraft.osrs.ThreatTiming.MELEE_REACH;
    }

    /**
     * {@link #check} for a player starting an attack; when refused, tells them why in the OSRS words ("I'm already under attack." /
     * "Someone else is fighting that.") and names the fighter in the way on the action bar, marked above its head for that player
     * alone (round 42 // playability: OSRS doesn't name it). The chat line is said at most once every 3 seconds per player, so a
     * held attack key doesn't flood the chat.
     */
    public static boolean allows(ServerPlayer player, LivingEntity target) {
        SingleCombat.Refusal r = refusal(player, target);
        if (r.verdict() == SingleCombat.Verdict.OK) {
            return true;
        }
        long now = ActionManager.mcTick();
        Long last = TOLD.get(player.getUUID());
        if (last == null || now - last >= 60 || now < last) {
            TOLD.put(player.getUUID(), now);
            com.runecraft.util.Msg.game(player, r.verdict().message);
        }
        ServerLevel world = player.level() instanceof ServerLevel w ? w : null;
        Entity blocker = find(world, r.blocker());
        String line = blockerLine(r.verdict(), blocker);
        if (line != null) {
            com.runecraft.util.Msg.action(player, line);
            if (world != null && blocker.level() == world) {
                world.sendParticles(player, net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER, true, blocker.getX(),
                        blocker.getY() + blocker.getBbHeight() + 0.5, blocker.getZ(), 3, 0.25, 0.1, 0.25, 0);
            }
        }
        RefusalListener l = refusalListener;
        if (l != null) {
            l.refused(player, r, line);
        }
        return false;
    }

    /** When each player was last told the OSRS refusal line (MC tick). */
    private static final java.util.Map<java.util.UUID, Long> TOLD = new java.util.concurrent.ConcurrentHashMap<>();

    /** Self-test hook: every refused player attack with its action-bar line. */
    public static RefusalListener refusalListener;

    /** Hears {@link #allows} refuse an attack. */
    @FunctionalInterface
    public interface RefusalListener {
        void refused(ServerPlayer player, SingleCombat.Refusal refusal, String line);
    }

    /** The action-bar line naming who is in the way, or null when nobody is known. */
    public static String blockerLine(SingleCombat.Verdict verdict, Entity blocker) {
        if (blocker == null || verdict == SingleCombat.Verdict.OK) {
            return null;
        }
        String name = blocker instanceof ServerPlayer p ? p.getGameProfile().getName() : displayName(blocker);
        return verdict == SingleCombat.Verdict.ATTACKER_BUSY ? "You are fighting " + name + "." : capitalised(name) + " is fighting that.";
    }

    private static String displayName(Entity e) {
        if (e instanceof LivingEntity l) {
            MonsterStats s = MonsterStats.of(l);
            if (s != null && s.name() != null && !s.name().isEmpty()) {
                String n = s.name();
                return "the " + Character.toLowerCase(n.charAt(0)) + n.substring(1) + " (level " + s.combatLevel() + ")";
            }
        }
        return e.getName().getString();
    }

    private static String capitalised(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** True if the entity stands in a singles-plus area (the Revenant Caves, the Wilderness Agility Course). */
    public static boolean singlesPlus(Entity e) {
        return Gielinor.is(e) && MultiCombat.isSinglesPlusAtBlock(e.getBlockX(), e.getBlockY(), e.getBlockZ());
    }

    private static boolean isPlayer(ServerLevel world, int id) {
        return world != null && world.getEntity(id) instanceof ServerPlayer;
    }

    /** The entity with this id in {@code world} or any loaded level, or null. */
    private static Entity find(ServerLevel world, int id) {
        if (id < 0) {
            return null;
        }
        Entity e = null;
        if (world != null) {
            e = world.getEntity(id);
        }
        if (e == null && server != null) {
            for (ServerLevel w : server.getAllLevels()) {
                e = w.getEntity(id);
                if (e != null) {
                    break;
                }
            }
        }
        return e;
    }

    /** Convenience for monsters: may this NPC attack the player now? */
    public static boolean npcMayAttack(LivingEntity mob, ServerPlayer player) {
        return check(mob, player) == SingleCombat.Verdict.OK;
    }

    public static void forget(Entity e) {
        TRACKER.forget(e.getId());
        TOLD.remove(e.getUUID());
    }

    /** Once per game tick (after the players). */
    public static void gameTick(MinecraftServer s, long gameTick) {
        server = s;
        if (gameTick % 50 == 0) {
            TRACKER.prune(gameTick);
        }
    }
}
