package com.runecraft.content.godwars;

import com.runecraft.action.ActionManager;
import com.runecraft.combat.CombatHandler;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.GameEvents;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.godwars.RevenantCaves;
import com.runecraft.osrs.content.godwars.RevenantCaves.Entrance;
import com.runecraft.osrs.content.godwars.RevenantCaves.Exit;
import com.runecraft.osrs.content.godwars.RevenantCaves.Pillar;
import com.runecraft.osrs.content.godwars.RevenantCaves.Spawn;
import com.runecraft.osrs.content.godwars.RevenantRewards;
import com.runecraft.platform.ServerLivingEntityEvents;
import com.runecraft.platform.ServerTickEvents;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.quest.QuestScripts;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import com.runecraft.world.StructurePlacer;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The Revenant Caves on the Minecraft side ({@link RevenantCaves}): the caverns and the crevice (one-way in, after the
 * 100,000 coin fee, taken from the inventory or else the bank), the stairs out to the trapdoors, the Agility pillars, the
 * fee lost on dying in the caves or to another player in the Wilderness, the thirty revenants kept on their spawn tiles
 * (each back after its own respawn time) and the Emblem Trader, who buys ancient artefacts.
 */
final class RevenantCavesService {
    /** Tag prefix of a spawn point's revenant (followed by the point's index). */
    static final String KEY = "rc_rev:";
    private static final int INTERVAL_MC_TICKS = 20;
    private static final int RANGE = 64;
    /** Game tick a point's revenant died. */
    private static final Map<Integer, Long> GONE_AT = new HashMap<>();
    private static long ticks;

    private RevenantCavesService() {
    }

    static void register() {
        BlockHooks.register(RevenantCaves.BLOCK_CAVERN, RevenantCavesService::enter);
        BlockHooks.register(RevenantCaves.BLOCK_CREVICE, RevenantCavesService::enter);
        BlockHooks.register(RevenantCaves.BLOCK_STAIRS, RevenantCavesService::climb);
        BlockHooks.register(RevenantCaves.BLOCK_PILLAR, RevenantCavesService::pillar);
        BlockHooks.register(RevenantCaves.BLOCK_TRAPDOOR, (player, pos, state) ->
                Msg.game(player, "There is nothing here to open it. Maybe it only opens from the inside?"));
        GameEvents.onKill((player, entity, stats) -> onKill(entity));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer p) {
                boolean byPlayer = source.getEntity() instanceof ServerPlayer killer && killer != p;
                if (inCaves(p) || (byPlayer && Gielinor.wildernessLevel(p) > 0)) {
                    loseFee(p);
                }
            }
        });
        QuestScripts.onTalk(RevenantCaves.EMBLEM_TRADER_NPC, (player, npc) -> emblemTrader(player));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++ticks % INTERVAL_MC_TICKS != 0) {
                return;
            }
            ServerLevel world = Gielinor.world(server);
            if (world == null) {
                return;
            }
            try {
                List<ServerPlayer> inside = new ArrayList<>();
                for (ServerPlayer p : world.players()) {
                    if (p.isAlive() && !p.isSpectator() && inCaves(p)) {
                        inside.add(p);
                    }
                }
                if (!inside.isEmpty()) {
                    stock(world, inside);
                }
            } catch (RuntimeException e) {
                com.runecraft.RuneCraft.LOGGER.error("Revenant Caves spawner failed", e);
            }
        });
    }

    /** True while the player stands in the caves. */
    static boolean inCaves(LivingEntity e) {
        return Gielinor.is(e) && RevenantCaves.contains(e.getBlockX(), e.getBlockY(), e.getBlockZ());
    }

    // ------------------------------------------------------------------ the fee

    static boolean feePaid(ServerPlayer p) {
        return WildernessState.get(p.getServer()).get(p.getUUID(), WildernessState.REV_FEE) > 0;
    }

    /** Pays the fee from the inventory, else the bank; false (with a message) when neither holds 100,000 coins. */
    static boolean payFee(ServerPlayer p) {
        if (feePaid(p)) {
            return true;
        }
        if (Inv.count(p, Inv.COINS) >= RevenantCaves.FEE) {
            Inv.remove(p, Inv.COINS, RevenantCaves.FEE);
            Msg.game(p, "You pay the 100,000 coin fee to enter the Revenant Caves.");
        } else if (Players.data(p).bank().count(Inv.COINS, "") >= RevenantCaves.FEE) {
            Players.data(p).bank().withdraw(Inv.COINS, "", RevenantCaves.FEE);
            Msg.game(p, "The 100,000 coin fee to enter the Revenant Caves is taken from your bank.");
        } else {
            Msg.game(p, "You need to pay a fee of 100,000 coins to enter the Revenant Caves. It can be taken from your bank.");
            return false;
        }
        WildernessState.get(p.getServer()).set(p.getUUID(), WildernessState.REV_FEE, 1);
        return true;
    }

    static void loseFee(ServerPlayer p) {
        if (feePaid(p)) {
            WildernessState.get(p.getServer()).set(p.getUUID(), WildernessState.REV_FEE, 0);
            Msg.red(p, "You have lost your Revenant Caves fee: it must be paid again to re-enter the caves.");
        }
    }

    // ------------------------------------------------------------------ ways in and out

    private static int[] tile(BlockPos pos) {
        return new int[]{com.runecraft.osrs.GielinorMap.toOsrsX(pos.getX()), com.runecraft.osrs.GielinorMap.toOsrsY(pos.getZ())};
    }

    /** A cavern or the crevice: pay the fee (once) and go down. */
    static void enter(ServerPlayer player, BlockPos pos, BlockState state) {
        int[] t = tile(pos);
        Entrance e = RevenantCaves.entranceAt(t[0], t[1]);
        if (e == null) {
            e = nearestEntrance(t[0], t[1]);
        }
        enter(player, e);
    }

    /** Goes down an entrance (the self-test uses it with the entrance id). */
    static boolean enter(ServerPlayer player, Entrance e) {
        if (!payFee(player)) {
            return false;
        }
        Msg.game(player, e.block().equals(RevenantCaves.BLOCK_CREVICE) ? "You jump down the crevice into the Revenant Caves."
                : "You enter the cavern. What's the worst that could happen?");
        land(player, e.toX(), e.toY());
        return true;
    }

    private static Entrance nearestEntrance(int x, int y) {
        Entrance best = RevenantCaves.ENTRANCES.get(0);
        long bestD = Long.MAX_VALUE;
        for (Entrance e : RevenantCaves.ENTRANCES) {
            long d = (long) (e.x1() - x) * (e.x1() - x) + (long) (e.y1() - y) * (e.y1() - y);
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    /** Puts a player on a cave tile (carving the chunks there first). */
    static void land(ServerPlayer player, int x, int y) {
        ServerLevel world = Gielinor.world(player.getServer());
        int bx = RevenantCaves.blockX(x);
        int bz = RevenantCaves.blockZ(y);
        StructurePlacer.applyAround(world, bx, bz, 24);
        player.teleportTo(world, bx + 1.0, RevenantCaves.feetY(), bz + 1.0, player.getYRot(), player.getXRot());
    }

    /** The stairs out: one way, to the trapdoor in level 17 or 36. */
    static void climb(ServerPlayer player, BlockPos pos, BlockState state) {
        int[] t = RevenantCaves.tileAt(pos.getX(), pos.getZ());
        Exit e = RevenantCaves.exitAt(t[0], t[1]);
        if (e == null) {
            e = RevenantCaves.EXITS.get(0);
        }
        climb(player, e);
    }

    static void climb(ServerPlayer player, Exit e) {
        ServerLevel world = Gielinor.world(player.getServer());
        int[] to = RevenantCaves.exitArrival(e);
        BlockPos spot = Gielinor.safeSpot(world, com.runecraft.osrs.GielinorMap.toBlockX(to[0]) + 1,
                com.runecraft.osrs.GielinorMap.toBlockZ(to[1]) + 1);
        Msg.game(player, "You climb up the stairs. It's not any safer out here.");
        player.teleportTo(world, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, player.getYRot(), player.getXRot());
    }

    // ------------------------------------------------------------------ the Agility pillars

    static void pillar(ServerPlayer player, BlockPos pos, BlockState state) {
        int[] t = RevenantCaves.tileAt(pos.getX(), pos.getZ());
        Pillar p = RevenantCaves.pillarAt(t[0], t[1]);
        if (p == null) {
            return;
        }
        int[] here = RevenantCaves.tileAt(player.getBlockX(), player.getBlockZ());
        long da = (long) (here[0] - p.ax()) * (here[0] - p.ax()) + (long) (here[1] - p.ay()) * (here[1] - p.ay());
        long db = (long) (here[0] - p.bx()) * (here[0] - p.bx()) + (long) (here[1] - p.by()) * (here[1] - p.by());
        jump(player, p, da <= db);
    }

    /** Jumps a pillar from end A (or B) to the other end; false when the player's Agility is too low. */
    static boolean jump(ServerPlayer player, Pillar p, boolean fromA) {
        int agility = Players.data(player).skills().currentLevel(Skill.AGILITY);
        if (agility < p.level()) {
            Msg.game(player, "You need an Agility level of " + p.level() + " to jump across the pillars.");
            return false;
        }
        boolean fail = player.getRandom().nextDouble() < RevenantCaves.failChance(p, agility);
        if (fail) {
            int dmg = RevenantCaves.FAIL_MIN + player.getRandom().nextInt(RevenantCaves.FAIL_MAX - RevenantCaves.FAIL_MIN + 1);
            CombatHandler.applyDamage(null, player, dmg);
            XpService.add(player, Skill.AGILITY, p.failXp());
            Msg.red(player, "You slip on the pillar and hurt yourself, but make it across.");
        } else {
            XpService.add(player, Skill.AGILITY, p.xp());
            Msg.game(player, "You jump across the pillars.");
        }
        land(player, fromA ? p.bx() : p.ax(), fromA ? p.by() : p.ay());
        return true;
    }

    // ------------------------------------------------------------------ the Emblem Trader

    /** The Emblem Trader buys every ancient artefact the player carries (wiki: their value in coins) and offers a skull. */
    static void emblemTrader(ServerPlayer player) {
        long paid = 0;
        for (String artefact : RevenantRewards.ARTEFACTS) {
            int n = Inv.count(player, artefact);
            if (n > 0) {
                Inv.remove(player, artefact, n);
                paid += (long) n * RevenantRewards.artefactValue(artefact);
            }
        }
        if (paid > 0) {
            Inv.give(player, Inv.COINS, (int) Math.min(Integer.MAX_VALUE, paid));
            QuestApi.say(player, "Emblem Trader", "Ancient artefacts! Here's " + String.format("%,d", paid) + " coins for them.");
            return;
        }
        com.runecraft.combat.SkullService.emblemTrader(player, "Emblem Trader");
    }

    // ------------------------------------------------------------------ the revenants

    private static void onKill(LivingEntity entity) {
        Integer key = keyOf(entity);
        if (key != null) {
            GONE_AT.put(key, ActionManager.gameTick());
        }
    }

    private static Integer keyOf(Entity e) {
        for (String tag : e.getTags()) {
            if (tag.startsWith(KEY)) {
                try {
                    return Integer.parseInt(tag.substring(KEY.length()));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private static AABB box() {
        int[] b = RevenantCaves.blockBox();
        return new AABB(b[0], RevenantCaves.FLOOR - 1, b[1], b[2] + 1, RevenantCaves.FLOOR + RevenantCaves.HEIGHT + 2, b[3] + 1);
    }

    /** One pass over the spawn points near the given players (also used by the self-test). */
    static void stock(ServerLevel world, List<ServerPlayer> near) {
        Map<Integer, Entity> present = new HashMap<>();
        for (Entity e : world.getEntitiesOfClass(LivingEntity.class, box(), e -> e.isAlive())) {
            Integer key = keyOf(e);
            if (key == null) {
                continue;
            }
            if (present.containsKey(key) || key < 0 || key >= RevenantCaves.SPAWNS.size()) {
                e.discard();
            } else {
                present.put(key, e);
                if (e instanceof OsrsMonsterEntity mob) {
                    RevenantBehaviour.attach(mob);
                }
            }
        }
        long now = ActionManager.gameTick();
        for (int i = 0; i < RevenantCaves.SPAWNS.size(); i++) {
            Spawn s = RevenantCaves.SPAWNS.get(i);
            Entity e = present.get(i);
            if (e != null) {
                if (e instanceof Mob mob) {
                    walkBack(mob, s);
                }
                continue;
            }
            if (!active(s, near)) {
                continue;
            }
            Long gone = GONE_AT.get(i);
            RevenantRewards.Revenant rev = RevenantRewards.revenant(s.creature());
            if (gone != null && rev != null && now - gone < rev.respawnTicks()) {
                continue;
            }
            if (spawn(world, i) != null) {
                GONE_AT.remove(i);
            }
        }
    }

    private static boolean active(Spawn s, List<ServerPlayer> near) {
        int x = RevenantCaves.blockX(s.x());
        int z = RevenantCaves.blockZ(s.y());
        for (ServerPlayer p : near) {
            if (Math.abs(p.getX() - x) < RANGE && Math.abs(p.getZ() - z) < RANGE) {
                return true;
            }
        }
        return false;
    }

    private static void walkBack(Mob mob, Spawn s) {
        if (mob.getTarget() != null) {
            return;
        }
        double x = RevenantCaves.blockX(s.x()) + 1;
        double z = RevenantCaves.blockZ(s.y()) + 1;
        double d2 = mob.distanceToSqr(x, RevenantCaves.feetY(), z);
        if (d2 > 3.0 * 16 * 16) {
            mob.teleportTo(x, RevenantCaves.feetY(), z);
        } else if (d2 > 16 * 16) {
            mob.getNavigation().moveTo(x, RevenantCaves.feetY(), z, 1.0);
        }
    }

    /** Spawns a point's revenant on its tile (once the tile is carved and loaded); null when it can't yet. */
    static Mob spawn(ServerLevel world, int index) {
        Spawn s = RevenantCaves.SPAWNS.get(index);
        BlockPos pos = new BlockPos(RevenantCaves.blockX(s.x()), RevenantCaves.feetY(), RevenantCaves.blockZ(s.y()));
        if (!world.hasChunkAt(pos) || !world.getBlockState(pos).isAir() || !world.getBlockState(pos.above()).isAir()) {
            return null;
        }
        EntityType<?> type = com.runecraft.registry.ModEntities.typeById(s.creature());
        Entity e = type == null ? null : type.create(world);
        if (!(e instanceof Mob mob)) {
            return null;
        }
        mob.moveTo(pos.getX() + 1.0, pos.getY(), pos.getZ() + 1.0, world.getRandom().nextFloat() * 360f, 0);
        mob.finalizeSpawn(world, world.getCurrentDifficultyAt(pos), MobSpawnType.SPAWNER, null);
        mob.setPersistenceRequired();
        mob.addTag(KEY + index);
        world.addFreshEntity(mob);
        if (mob instanceof OsrsMonsterEntity osrs) {
            RevenantBehaviour.attach(osrs);
        }
        return mob;
    }

    /** The live revenant of a spawn point, or null (the self-test). */
    static Mob find(ServerLevel world, int index) {
        for (Mob m : world.getEntitiesOfClass(Mob.class, box(), e -> e.isAlive())) {
            Integer key = keyOf(m);
            if (key != null && key == index) {
                return m;
            }
        }
        return null;
    }

    /** Forgets a spawn point's respawn timer (the self-test). */
    static void resetTimer(int index) {
        GONE_AT.remove(index);
    }

    /** Game ticks until a spawn point's revenant may come back (0 when it may now). */
    static long respawnLeft(int index) {
        Long gone = GONE_AT.get(index);
        RevenantRewards.Revenant rev = RevenantRewards.revenant(RevenantCaves.SPAWNS.get(index).creature());
        if (gone == null || rev == null) {
            return 0;
        }
        return Math.max(0, rev.respawnTicks() - (ActionManager.gameTick() - gone));
    }
}
