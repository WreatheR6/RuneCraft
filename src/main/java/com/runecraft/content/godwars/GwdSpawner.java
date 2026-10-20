package com.runecraft.content.godwars;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.entity.BossEntity;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.godwars.GwdDungeon;
import com.runecraft.osrs.content.godwars.GwdDungeon.Spot;
import com.runecraft.osrs.content.godwars.GwdSpawns;
import com.runecraft.osrs.data.Boss;
import com.runecraft.platform.ServerTickEvents;
import com.runecraft.registry.ModEntities;
import com.runecraft.world.Gielinor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;

/**
 * Keeps the God Wars Dungeon populated like OSRS: every follower spawn tile ({@link GwdSpawns}) and every general's and
 * bodyguard's spawn tile ({@link GwdDungeon#lairs()}) holds one creature. A point wakes while a player is within
 * {@value #RANGE} blocks of it on its level; it is stocked at once the first time, and a creature that was killed comes
 * back after its OSRS respawn time (followers 25-60 ticks, generals and bodyguards 150). A general or bodyguard that left
 * without dying (its chamber reset when everyone left) comes back as soon as someone is near again.
 *
 * <p>Spawned creatures are persistent and carry their point's key as a tag, so they are found again after a reload;
 * followers that wander off are walked back.
 */
final class GwdSpawner {
    /** Tag prefix of a spawn point's creature (followed by the point's key). */
    static final String KEY = "rc_gwd:";
    private static final int INTERVAL_MC_TICKS = 20;
    private static final int RANGE = 48;
    private static final int RANGE_Y = 10;
    /** Followers further than this from their tile (with nothing to fight) walk back; much further, they are put back. */
    private static final int WANDER = 16;

    /** One spawn point: a follower, or a general/bodyguard of a chamber. */
    record Point(String key, String creature, Spot spot, int respawnTicks, boolean lair) {
        int x() {
            return spot.blockX() + 1;
        }

        int z() {
            return spot.blockZ() + 1;
        }
    }

    private static final List<Point> POINTS = new ArrayList<>();
    private static final Map<String, Point> BY_KEY = new HashMap<>();
    /** Game tick a point's creature died (from the kill hook) or was first found missing after a death. */
    private static final Map<String, Long> GONE_AT = new HashMap<>();
    private static long ticks;

    private GwdSpawner() {
    }

    static void register() {
        List<GwdSpawns.Spawn> spawns = GwdSpawns.all();
        for (int i = 0; i < spawns.size(); i++) {
            GwdSpawns.Spawn s = spawns.get(i);
            add(new Point("f" + i, s.creature(), new Spot(s.plane(), s.x(), s.y()), s.respawnTicks(), false));
        }
        for (GwdDungeon.Lair l : GwdDungeon.lairs()) {
            add(new Point(l.creature(), l.creature(), l.spot(), GwdDungeon.GENERAL_RESPAWN_TICKS, true));
        }
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++ticks % INTERVAL_MC_TICKS != 0) {
                return;
            }
            ServerLevel world = Gielinor.world(server);
            if (world == null) {
                return;
            }
            try {
                tick(world);
            } catch (RuntimeException e) {
                com.runecraft.RuneCraft.LOGGER.error("God Wars Dungeon spawner failed", e);
            }
        });
    }

    private static void add(Point p) {
        POINTS.add(p);
        BY_KEY.put(p.key(), p);
    }

    /** Kill hook: starts the point's respawn timer. */
    static void onKill(LivingEntity entity) {
        String key = keyOf(entity);
        if (key != null) {
            GONE_AT.put(key, ActionManager.gameTick());
        }
    }

    private static String keyOf(Entity e) {
        for (String tag : e.getTags()) {
            if (tag.startsWith(KEY)) {
                return tag.substring(KEY.length());
            }
        }
        return null;
    }

    /** The dungeon's whole box (every level). */
    static AABB box() {
        int[] b = GwdDungeon.blockBox();
        return new AABB(b[0], GwdDungeon.floorY(0) - 2, b[1], b[2] + 1, GwdDungeon.floorY(2) + GwdDungeon.height(2) + 2, b[3] + 1);
    }

    /** Players inside the dungeon (real ones: the self-test's fake player isn't in the world's list). */
    static List<ServerPlayer> playersInside(ServerLevel world) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : world.players()) {
            if (p.isAlive() && !p.isSpectator() && GwdDungeon.contains(p.getBlockX(), p.getBlockY(), p.getBlockZ())) {
                out.add(p);
            }
        }
        return out;
    }

    private static void tick(ServerLevel world) {
        List<ServerPlayer> inside = playersInside(world);
        if (inside.isEmpty()) {
            return;
        }
        stock(world, inside);
    }

    /** One pass over every point near the given players (also used by the self-test with its own player list). */
    static void stock(ServerLevel world, List<ServerPlayer> near) {
        Map<String, Entity> present = new HashMap<>();
        for (Entity e : world.getEntitiesOfClass(LivingEntity.class, box(), e -> e.isAlive())) {
            String key = keyOf(e);
            if (key == null) {
                continue;
            }
            if (present.containsKey(key) || !BY_KEY.containsKey(key)) {
                e.discard(); // a duplicate, or a point that no longer exists
            } else {
                present.put(key, e);
            }
        }
        long now = ActionManager.gameTick();
        for (Point p : POINTS) {
            Entity e = present.get(p.key());
            if (e != null) {
                if (!p.lair() && e instanceof Mob mob) {
                    walkBack(mob, p);
                }
                continue;
            }
            if (!active(p, near)) {
                continue;
            }
            Long gone = GONE_AT.get(p.key());
            if (gone != null && now - gone < p.respawnTicks()) {
                continue;
            }
            if (spawn(world, p) != null) {
                GONE_AT.remove(p.key());
            }
        }
    }

    private static boolean active(Point p, List<ServerPlayer> near) {
        int feet = p.spot().feetY();
        for (ServerPlayer pl : near) {
            if (Math.abs(pl.getX() - p.x()) < RANGE && Math.abs(pl.getZ() - p.z()) < RANGE && Math.abs(pl.getY() - feet) < RANGE_Y) {
                return true;
            }
        }
        return false;
    }

    private static void walkBack(Mob mob, Point p) {
        if (mob.getTarget() != null) {
            return;
        }
        double d2 = mob.distanceToSqr(p.x(), p.spot().feetY(), p.z());
        if (d2 > 3.0 * WANDER * WANDER) {
            mob.teleportTo(p.x(), p.spot().feetY(), p.z());
        } else if (d2 > WANDER * WANDER) {
            mob.getNavigation().moveTo(p.x(), p.spot().feetY(), p.z(), 1.0);
        }
    }

    /** Spawns a point's creature on its tile (once the tile is carved and loaded); null when it can't yet. */
    static LivingEntity spawn(ServerLevel world, Point p) {
        BlockPos pos = new BlockPos(p.spot().blockX(), p.spot().feetY(), p.spot().blockZ());
        if (!world.hasChunkAt(pos) || !world.getBlockState(pos).isAir() || !world.getBlockState(pos.above()).isAir()) {
            return null;
        }
        LivingEntity spawned;
        BossDef def = ContentRegistry.boss(p.creature());
        Boss legacy = Boss.byEntity(p.creature());
        if (def != null) {
            DataBossEntity boss = DataBossService.spawn(world, pos, def, null);
            if (boss == null) {
                return null;
            }
            boss.markSummoned(pos, leash(p));
            spawned = boss;
        } else if (legacy != null) {
            EntityType<BossEntity> type = ModEntities.BOSS_TYPES.get(legacy);
            BossEntity boss = type == null ? null : type.create(world);
            if (boss == null) {
                return null;
            }
            boss.moveTo(p.x(), pos.getY(), p.z(), world.getRandom().nextFloat() * 360f, 0);
            boss.finalizeSpawn(world, world.getCurrentDifficultyAt(pos), MobSpawnType.SPAWNER, null);
            boss.markSummoned(pos);
            world.addFreshEntity(boss);
            spawned = boss;
        } else {
            EntityType<?> type = ModEntities.typeById(p.creature());
            Entity e = type == null ? null : type.create(world);
            if (!(e instanceof Mob mob)) {
                return null;
            }
            mob.moveTo(p.x(), pos.getY(), p.z(), world.getRandom().nextFloat() * 360f, 0);
            mob.finalizeSpawn(world, world.getCurrentDifficultyAt(pos), MobSpawnType.SPAWNER, null);
            mob.setPersistenceRequired();
            world.addFreshEntity(mob);
            spawned = mob;
        }
        spawned.addTag(KEY + p.key());
        return spawned;
    }

    /** A general stays in its chamber: leashed to its spawn tile within the chamber's size. */
    private static int leash(Point p) {
        return 20;
    }

    /** The creature of a general's or bodyguard's point, if it is alive in the dungeon (the self-test). */
    static LivingEntity find(ServerLevel world, String creature) {
        for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, box(), e -> e.isAlive())) {
            if (creature.equals(keyOf(e))) {
                return e;
            }
        }
        return null;
    }

    /** Spawns a general's or bodyguard's point now, ignoring its timer (the self-test). */
    static LivingEntity spawnNow(ServerLevel world, String creature) {
        Point p = BY_KEY.get(creature);
        if (p == null) {
            return null;
        }
        GONE_AT.remove(p.key());
        return spawn(world, p);
    }

    /** Forgets a point's respawn timer (the self-test). */
    static void resetTimer(String key) {
        GONE_AT.remove(key);
    }

    /** Game ticks until a point may come back (0 when it may now; the self-test). */
    static long respawnLeft(String key) {
        Point p = BY_KEY.get(key);
        Long gone = GONE_AT.get(key);
        if (p == null || gone == null) {
            return 0;
        }
        return Math.max(0, p.respawnTicks() - (ActionManager.gameTick() - gone));
    }
}
