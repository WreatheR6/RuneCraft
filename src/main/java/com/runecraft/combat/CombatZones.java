package com.runecraft.combat;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.entity.BossEntity;
import com.runecraft.entity.OsrsMonsterEntity;
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
        if (attacker == null || target == null || exempt(attacker) || exempt(target)) {
            return SingleCombat.Verdict.OK;
        }
        boolean multi = isMulti(target) || isMulti(attacker);
        boolean pvp = attacker instanceof ServerPlayer && target instanceof ServerPlayer;
        ServerLevel world = attacker.level() instanceof ServerLevel w ? w : null;
        if (!multi && (singlesPlus(target) || singlesPlus(attacker))) {
            return TRACKER.checkSinglesPlus(attacker.getId(), target.getId(), ActionManager.gameTick(), pvp, id -> active(world, id),
                    id -> isPlayer(world, id));
        }
        return TRACKER.check(attacker.getId(), target.getId(), ActionManager.gameTick(), multi, pvp,
                id -> active(world, id));
    }

    /** True if the entity stands in a singles-plus area (the Revenant Caves, the Wilderness Agility Course). */
    public static boolean singlesPlus(Entity e) {
        return Gielinor.is(e) && MultiCombat.isSinglesPlusAtBlock(e.getBlockX(), e.getBlockY(), e.getBlockZ());
    }

    private static boolean isPlayer(ServerLevel world, int id) {
        return world != null && world.getEntity(id) instanceof ServerPlayer;
    }

    /** A recorded fighter still counts while it is alive, loaded and not a boss (bosses never block anyone). */
    private static boolean active(ServerLevel world, int id) {
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
        return e instanceof LivingEntity l && l.isAlive() && !exempt(l);
    }

    /** Convenience for monsters: may this NPC attack the player now? */
    public static boolean npcMayAttack(LivingEntity mob, ServerPlayer player) {
        return check(mob, player) == SingleCombat.Verdict.OK;
    }

    public static void forget(Entity e) {
        TRACKER.forget(e.getId());
    }

    /** Once per game tick (after the players). */
    public static void gameTick(MinecraftServer s, long gameTick) {
        server = s;
        if (gameTick % 50 == 0) {
            TRACKER.prune(gameTick);
        }
    }
}
