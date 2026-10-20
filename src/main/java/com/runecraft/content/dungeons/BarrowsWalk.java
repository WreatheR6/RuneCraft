package com.runecraft.content.dungeons;

import com.runecraft.combat.CombatHandler;
import com.runecraft.content.BlockHooks;
import com.runecraft.entity.BossEntity;
import com.runecraft.osrs.content.dungeons.Barrows;
import com.runecraft.osrs.content.dungeons.BarrowsRun;
import com.runecraft.osrs.data.Boss;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModEntities;
import com.runecraft.util.Inv;
import com.runecraft.world.Gielinor;
import com.runecraft.world.StructurePlacer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Self-test driver ({@code boss:barrows_run}): a whole Barrows run the way a player does it, through the real blocks and kill
 * paths: a spade dug into each of the five crypts whose brother sleeps there, the sarcophagus searched and the brother killed
 * (his kill counted in the run), the stairs back up; the sixth crypt's sarcophagus hiding the tunnel; the tunnels walked
 * through their open doors (puzzle doors answered, whatever a door wakes killed) to the chest, which first raises the hidden
 * brother, then gives the run's loot, counts a chest and sends the player back up. Real player only (the tunnels and crypts
 * must be built around them). The walk itself puts back the player's Barrows run, prayer points and spades; the kill counts,
 * collection log, inventory (the chest's loot), coin pouch and combat achievement and diary progress the real kills and chest
 * add are put back by the self-test harness ({@code TestContext} saves the real player's whole save before the checks and
 * restores it afterwards).
 */
public final class BarrowsWalk {
    private static final net.minecraft.resources.ResourceLocation HP_BUFFER = com.runecraft.RuneCraft.id("selftest_barrows_hp");
    /** The brother whose crypt hides the tunnel in the test's run (Guthan), and the run's layout seed. */
    private static final int TUNNEL = 2;
    private static final long SEED = 0x5EEDL;

    private final ServerPlayer player;
    private final ServerLevel world;
    private final StringBuilder log = new StringBuilder();
    private int woken;

    private BarrowsWalk(ServerPlayer player, ServerLevel world) {
        this.player = player;
        this.world = world;
    }

    /** Runs the whole walk and puts the player back at {@code back}; returns a summary or throws {@link AssertionError}. */
    public static String run(ServerPlayer player, BlockPos back) {
        ServerLevel world = Gielinor.world(player.getServer());
        expect(world != null, "no Gielinor");
        BarrowsState state = BarrowsState.get(player.getServer());
        BarrowsRun saved = state.replace(player.getUUID(), BarrowsRun.of(0, 0, TUNNEL, SEED, false));
        PlayerData data = Players.data(player);
        int prayer = data.prayerPoints();
        int spades = Inv.count(player, "spade");
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.removeModifier(HP_BUFFER);
            health.addTransientModifier(new AttributeModifier(HP_BUFFER, 2000, AttributeModifier.Operation.ADD_VALUE));
            player.setHealth(player.getMaxHealth());
        }
        BarrowsWalk w = new BarrowsWalk(player, world);
        try {
            return w.walk();
        } catch (AssertionError e) {
            throw new AssertionError(e.getMessage() + " (done: " + w.log + ")");
        } finally {
            if (health != null) {
                health.removeModifier(HP_BUFFER);
                player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
            }
            state.replace(player.getUUID(), saved);
            data.setPrayerPoints(prayer);
            int extra = Inv.count(player, "spade") - spades;
            if (extra > 0) {
                Inv.remove(player, "spade", extra);
            }
            com.runecraft.network.ServerNetworking.sync(player);
            if (back != null && player.isAlive()) {
                player.teleportTo(back.getX() + 0.5, back.getY(), back.getZ() + 0.5);
            }
        }
    }

    private static void expect(boolean ok, String message) {
        if (!ok) {
            throw new AssertionError(message);
        }
    }

    private void note(String s) {
        log.append(log.length() == 0 ? "" : "; ").append(s);
    }

    private BarrowsRun run() {
        return BarrowsState.get(player.getServer()).run(player.getUUID());
    }

    /** Right-clicks the scripted block at {@code pos} through BlockHooks, checking it is the expected block. */
    private void use(BlockPos pos, String expectedBlock) {
        BlockState state = world.getBlockState(pos);
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        expect(id.equals(expectedBlock), "expected " + expectedBlock + " at " + pos.toShortString() + " but found " + id);
        BlockHooks.use(player, id, pos, state);
    }

    private void heal() {
        player.setHealth(player.getMaxHealth());
        Players.data(player).setPrayerPoints(99);
    }

    /** Kills a creature as the player through the real damage path; false if it couldn't be hurt. */
    private boolean kill(net.minecraft.world.entity.LivingEntity e) {
        for (int i = 0; i < 100 && e.isAlive(); i++) {
            int d = e instanceof com.runecraft.entity.OsrsMonsterEntity m
                    ? m.modifyIncoming(player, Math.max(20, Math.round(e.getMaxHealth())), com.runecraft.osrs.AttackType.MAGIC)
                    : Math.round(e.getMaxHealth());
            if (d <= 0) {
                return false;
            }
            CombatHandler.applyDamage(player, e, d);
            heal();
        }
        return !e.isAlive();
    }

    /** Items carried, coins in the pouch included (the chest's coins go there). */
    private long carried() {
        long n = Inv.count(player, Inv.COINS);
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            n += player.getInventory().getItem(i).getCount();
        }
        return n;
    }

    private BossEntity brotherNear(Boss b) {
        EntityType<BossEntity> type = ModEntities.BOSS_TYPES.get(b);
        List<BossEntity> found = world.getEntities(type, new AABB(player.blockPosition()).inflate(32), e -> e.isAlive());
        return found.isEmpty() ? null : found.get(0);
    }

    /** Kills whatever a door woke (and any other creature that has come for the player) in the room around them. */
    private void clearAround() {
        for (Mob m : world.getEntitiesOfClass(Mob.class, new AABB(player.blockPosition()).inflate(Barrows.ROOM_SPACING),
                e -> e.isAlive() && e instanceof net.minecraft.world.entity.monster.Enemy)) {
            if (kill(m)) {
                woken++;
            }
        }
    }

    // ------------------------------------------------------------------ the walk

    private String walk() {
        heal();
        Inv.give(player, "spade", 1);
        for (Barrows.Crypt crypt : Barrows.crypts()) {
            if (crypt.index() != TUNNEL) {
                crypt(crypt);
            }
        }
        expect(run().killedCount() == 5, "five crypts gave " + run().killedCount() + " kills");
        tunnel(Barrows.crypts().get(TUNNEL));
        tunnels();
        chest();
        return log.toString();
    }

    /** Digs into a mound from its top. */
    private void dig(Barrows.Crypt crypt) {
        StructurePlacer.applyAround(world, crypt.x(), crypt.z(), 16);
        BlockPos top = Gielinor.safeSpot(world, crypt.x(), crypt.z());
        player.teleportTo(world, top.getX() + 0.5, top.getY(), top.getZ() + 0.5, 0, 0);
        expect(Barrows.moundAt(player.getBlockX(), player.getBlockY(), player.getBlockZ()) == crypt,
                "standing at " + player.blockPosition().toShortString() + " isn't on " + crypt.brother().displayName() + "'s mound");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, Inv.stack("spade", 1));
        expect(BarrowsHooks.dig(player), "digging on " + crypt.brother().displayName() + "'s mound did nothing");
        expect(Barrows.cryptAt(player.getBlockX(), player.getBlockY(), player.getBlockZ()) == crypt,
                "the spade didn't drop the player into " + crypt.brother().displayName() + "'s crypt");
    }

    private BlockPos sarcophagus(Barrows.Crypt crypt) {
        int[] s = crypt.sarcophagus();
        return new BlockPos(s[0], s[1], s[2]);
    }

    /** A crypt whose brother sleeps in it: dig in, search the sarcophagus, kill him, climb back up. */
    private void crypt(Barrows.Crypt crypt) {
        dig(crypt);
        use(sarcophagus(crypt), Barrows.SARCOPHAGUS);
        BossEntity brother = brotherNear(crypt.brother());
        expect(brother != null, "searching " + crypt.brother().displayName() + "'s sarcophagus raised nobody");
        expect(run().summoned(crypt.index()), crypt.brother().displayName() + " wasn't marked summoned in the run");
        expect(kill(brother), crypt.brother().displayName() + " couldn't be killed");
        expect(run().killed(crypt.index()), crypt.brother().displayName() + "'s kill didn't count in the run");
        use(sarcophagus(crypt), Barrows.SARCOPHAGUS); // searched again: empty
        expect(brotherNear(crypt.brother()) == null, crypt.brother().displayName() + " rose again from an emptied sarcophagus");
        int[] st = crypt.stairs();
        use(new BlockPos(st[0], st[1], st[2]), Barrows.CRYPT_STAIRS);
        expect(Barrows.cryptAt(player.getBlockX(), player.getBlockY(), player.getBlockZ()) == null,
                "the stairs didn't lead out of " + crypt.brother().displayName() + "'s crypt");
        note(crypt.brother().entityId());
    }

    /** The tunnel brother's crypt: his sarcophagus hides the tunnel. */
    private void tunnel(Barrows.Crypt crypt) {
        dig(crypt);
        expect(run().search(crypt.index()) == BarrowsRun.Search.TUNNEL, "the run's hidden brother isn't " + crypt.brother().displayName());
        player.setShiftKeyDown(true); // sneak while searching: straight in
        try {
            use(sarcophagus(crypt), Barrows.SARCOPHAGUS);
        } finally {
            player.setShiftKeyDown(false);
        }
        expect(Barrows.inTunnels(player.getBlockX(), player.getBlockY(), player.getBlockZ()),
                "the hidden tunnel didn't lead into the tunnels (" + player.blockPosition().toShortString() + ")");
        expect(Barrows.roomAt(player.getBlockX(), player.getBlockZ()) == run().startRoom(), "the tunnel didn't land in the run's start room");
        note("tunnel in " + crypt.brother().entityId() + "'s crypt");
    }

    /** The rooms from the start room to the chest room through open doors (edges, in order). */
    private List<Integer> route() {
        boolean[] open = run().openDoors();
        int start = run().startRoom();
        int[] via = new int[9];
        java.util.Arrays.fill(via, -2);
        via[start] = -1;
        Deque<Integer> queue = new ArrayDeque<>(List.of(start));
        while (!queue.isEmpty()) {
            int room = queue.poll();
            for (int e = 0; e < BarrowsRun.EDGES; e++) {
                int[] r = BarrowsRun.edgeRooms(e);
                int next = r[0] == room ? r[1] : r[1] == room ? r[0] : -1;
                if (open[e] && next >= 0 && via[next] == -2) {
                    via[next] = e;
                    queue.add(next);
                }
            }
        }
        expect(via[Barrows.CHEST_ROOM] != -2, "no open way from the start room to the chest");
        List<Integer> edges = new ArrayList<>();
        for (int room = Barrows.CHEST_ROOM; via[room] >= 0; ) {
            int e = via[room];
            edges.add(0, e);
            int[] r = BarrowsRun.edgeRooms(e);
            room = r[0] == room ? r[1] : r[0];
        }
        return edges;
    }

    /** Walks the tunnels door by door to the chest room. */
    private void tunnels() {
        int room = run().startRoom();
        int doors = 0;
        int puzzles = 0;
        for (int e : route()) {
            int[] r = BarrowsRun.edgeRooms(e);
            int next = r[0] == room ? r[1] : r[0];
            int[] d = Barrows.doorAt(e);
            int[] from = Barrows.roomCentre(room);
            // stand in the corridor just short of the door, on this room's side
            int sx = d[0] + Integer.signum(from[0] - d[0]);
            int sz = d[1] + Integer.signum(from[1] - d[1]);
            StructurePlacer.applyAround(world, d[0], d[1], 16);
            player.teleportTo(world, sx + 0.5, Barrows.TUNNEL_FLOOR + 1, sz + 0.5, 0, 0);
            BlockPos door = new BlockPos(d[0], Barrows.TUNNEL_FLOOR + 1, d[1]);
            use(door, Barrows.TUNNEL_DOOR);
            int choice = BarrowsHooks.selfTestPuzzleChoice(player);
            if (choice >= 0) {
                expect(next == Barrows.CHEST_ROOM, "a puzzle was asked on a door that doesn't lead into the chest room");
                BarrowsHooks.answerPuzzle(player, choice);
                expect(run().solved(e), "the right answer didn't solve the puzzle door");
                puzzles++;
            }
            int[] to = Barrows.roomCentre(next);
            double before = Math.hypot(sx - to[0], sz - to[1]);
            double after = Math.hypot(player.getX() - to[0], player.getZ() - to[1]);
            expect(after < before - 1, "the door between rooms " + room + " and " + next + " didn't let the player through");
            clearAround();
            player.teleportTo(world, to[0] + 0.5, Barrows.TUNNEL_FLOOR + 1, to[1] + 0.5, 0, 0);
            room = next;
            doors++;
        }
        expect(room == Barrows.CHEST_ROOM, "the walk ended in room " + room + ", not the chest room");
        note(doors + " doors (" + puzzles + " puzzles), " + woken + " creatures woken and killed");
    }

    /** The chest: it raises the hidden brother first (unless a door already did), then gives the loot. */
    private void chest() {
        int[] c = Barrows.chest();
        BlockPos pos = new BlockPos(c[0], c[1], c[2]);
        Boss hidden = Barrows.BROTHERS.get(TUNNEL);
        if (!run().hiddenSummoned()) {
            use(pos, Barrows.CHEST);
            expect(run().hiddenSummoned(), "opening the chest didn't raise the hidden brother");
        }
        BossEntity brother = brotherNear(hidden);
        if (brother != null) {
            expect(kill(brother), hidden.displayName() + " couldn't be killed");
        }
        expect(run().killedCount() == 6, "the run counts " + run().killedCount() + " brothers killed before the chest");
        int potential = run().rewardPotential();
        expect(potential >= 656 + 12, "reward potential " + potential + " with all six brothers killed");
        PlayerData data = Players.data(player);
        int chests = data.killCount("barrows_chests");
        long items = carried();
        use(pos, Barrows.CHEST);
        expect(data.killCount("barrows_chests") == chests + 1, "looting the chest didn't count a Barrows chest");
        expect(carried() > items, "the chest gave nothing");
        expect(!Barrows.underground(player.getBlockX(), player.getBlockY(), player.getBlockZ()),
                "the player wasn't sent back to the surface after looting");
        expect(run().killedCount() == 0, "the run wasn't reset after the chest");
        note("chest looted (potential " + potential + "), back on the surface");
    }
}
