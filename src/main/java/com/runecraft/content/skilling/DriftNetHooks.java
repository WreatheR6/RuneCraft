package com.runecraft.content.skilling;

import com.runecraft.action.ActionManager;
import com.runecraft.content.BlockHooks;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.skilling.DriftNetFishing;
import com.runecraft.skill.Stun;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

/**
 * Drift net fishing ({@link DriftNetFishing}): set a drift net on an anchor, chase fish shoals into it (Fishing and Hunter
 * XP per shoal caught), and harvest it for one loot roll per shoal. Nets are saved per player and anchor in
 * {@link SkillingState} (OSRS: every player has their own nets), so one player's nets never block another's. Shoals that
 * swim in by themselves give only Fishing XP (wiki: Hunter XP is only for chased shoals), paid when the net is harvested
 * (approx: OSRS gives it at the catch, even in other areas).
 */
public final class DriftNetHooks {
    private static final String SECTION = "drift_nets";
    /** How far from a shoal the nets it can be chased into may be (blocks). */
    private static final int REACH = 14;

    private DriftNetHooks() {
    }

    static void init() {
        BlockHooks.register(DriftNetFishing.ANCHOR, (player, pos, state) -> anchor(player, pos));
        BlockHooks.register(DriftNetFishing.SHOAL, (player, pos, state) -> chase(player, pos));
    }

    private static boolean levels(ServerPlayerEntity player) {
        return SkillingUtil.need(player, Skill.FISHING, DriftNetFishing.FISHING_LEVEL, "fish with a drift net")
                && SkillingUtil.need(player, Skill.HUNTER, DriftNetFishing.HUNTER_LEVEL, "fish with a drift net");
    }

    /** A player's net on an anchor: keyed by player and anchor, so every player has their own net on each anchor. */
    private static String key(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        return player.getUuidAsString() + "|" + SkillingUtil.key(world, pos);
    }

    private static NbtCompound net(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        return SkillingState.get(world.getServer()).entry(SECTION, key(player, world, pos));
    }

    /** Whether the player has a net set on an anchor (doesn't create an entry). */
    private static boolean isSet(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        return SkillingState.get(world.getServer()).has(SECTION, key(player, world, pos))
                && net(player, world, pos).getBoolean("set");
    }

    /** Forgets a player's net on an anchor (self-test cleanup: the client run loads the server run's world). */
    public static void clear(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        SkillingState.get(world.getServer()).remove(SECTION, key(player, world, pos));
    }

    /** Shoals caught in a player's net on an anchor (0 when no net is set). */
    public static int caught(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        return isSet(player, world, pos) ? net(player, world, pos).getInt("fish") : 0;
    }

    /** Adds the shoals that swam in by themselves since the last look. */
    private static void drift(ServerWorld world, NbtCompound n) {
        long now = world.getTime();
        long last = n.getLong("last");
        long period = (long) DriftNetFishing.PASSIVE_TICKS * ActionManager.MC_TICKS_PER_GAME_TICK;
        if (now < last) {
            n.putLong("last", now);
            return;
        }
        int add = (int) Math.min(DriftNetFishing.CAPACITY, (now - last) / period);
        int room = DriftNetFishing.CAPACITY - n.getInt("fish");
        int got = Math.max(0, Math.min(add, room));
        n.putInt("fish", n.getInt("fish") + got);
        n.putInt("passive", n.getInt("passive") + got);
        n.putLong("last", got < add ? now : last + add * period);
    }

    private static void anchor(ServerPlayerEntity player, BlockPos pos) {
        if (!levels(player)) {
            return;
        }
        ServerWorld world = (ServerWorld) player.getWorld();
        if (!isSet(player, world, pos)) {
            if (!Inv.has(player, DriftNetFishing.NET, 1)) {
                Msg.game(player, "You need a drift net to set up here. Weave one from two jute fibre at a loom (26 Crafting).");
                return;
            }
            Inv.remove(player, DriftNetFishing.NET, 1);
            NbtCompound n = net(player, world, pos);
            n.putBoolean("set", true);
            n.putInt("fish", 0);
            n.putInt("passive", 0);
            n.putLong("last", world.getTime());
            Msg.action(player, "You set up the drift net. Chase fish shoals into it.");
            return;
        }
        NbtCompound n = net(player, world, pos);
        drift(world, n);
        if (n.getInt("fish") == 0) {
            Msg.game(player, "Your drift net hasn't caught anything yet.");
            return;
        }
        harvest(player, world, pos, n);
    }

    /** Harvests a net: one loot roll per shoal; the net is destroyed. */
    private static void harvest(ServerPlayerEntity player, ServerWorld world, BlockPos pos, NbtCompound n) {
        int fish = n.getInt("fish");
        int passive = Math.min(fish, n.getInt("passive"));
        int fishing = SkillingUtil.level(player, Skill.FISHING);
        if (passive > 0) {
            // Wiki: shoals that swim in by themselves give Fishing XP only; Hunter XP is only for chased shoals.
            XpService.add(player, Skill.FISHING, passive * DriftNetFishing.fishingXp(fishing));
        }
        java.util.Random random = new java.util.Random(player.getRandom().nextLong());
        for (int i = 0; i < fish; i++) {
            Object[] loot = DriftNetFishing.roll(fishing, !Inv.has(player, DriftNetFishing.CLUE, 1), random);
            if (loot != null) {
                SkillingUtil.give(player, (String) loot[0], (Integer) loot[1]);
            }
        }
        clear(player, world, pos);
        Msg.action(player, "You harvest the drift net (" + fish + " fish shoal" + (fish == 1 ? "" : "s") + "). The net is destroyed.");
    }

    /** Anchors near a shoal holding this player's net with room left, nearest first. */
    private static List<BlockPos> nets(ServerPlayerEntity player, ServerWorld world, BlockPos shoal) {
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : BlockPos.iterate(shoal.add(-REACH, -3, -REACH), shoal.add(REACH, 3, REACH))) {
            if (SkillingUtil.blockId(world.getBlockState(p)).equals(DriftNetFishing.ANCHOR)) {
                out.add(p.toImmutable());
            }
        }
        out.sort((a, b) -> Double.compare(a.getSquaredDistance(shoal), b.getSquaredDistance(shoal)));
        out.removeIf(p -> !isSet(player, world, p));
        return out;
    }

    private static void chase(ServerPlayerEntity player, BlockPos pos) {
        if (!Stun.check(player) || !levels(player)) {
            return;
        }
        ServerWorld world = (ServerWorld) player.getWorld();
        List<BlockPos> anchors = nets(player, world, pos);
        if (anchors.isEmpty()) {
            Msg.game(player, "Set up a drift net on one of the anchors first.");
            return;
        }
        Msg.action(player, "You try to chase the fish shoal into your drift net.");
        SkillingUtil.start(player, new SkillingUtil.BlockAction(pos, DriftNetFishing.SHOAL, DriftNetFishing.CHASE_TICKS) {
            @Override
            boolean roll(ServerPlayerEntity p, Random random) {
                BlockPos target = null;
                for (BlockPos a : anchors) {
                    if (isSet(p, world, a)) {
                        NbtCompound n = net(p, world, a);
                        drift(world, n);
                        if (n.getInt("fish") < DriftNetFishing.CAPACITY) {
                            target = a;
                            break;
                        }
                    }
                }
                if (target == null) {
                    Msg.game(p, "Your drift nets are full.");
                    return false;
                }
                world.playSound(null, pos, SoundEvents.ENTITY_FISHING_BOBBER_SPLASH, SoundCategory.PLAYERS, 0.4f, 1.4f);
                boolean tool = DriftNetFishing.CHASE_TOOLS.contains(SkillingUtil.heldId(p));
                if (random.nextDouble() >= DriftNetFishing.chaseChance(tool)) {
                    return true;
                }
                catchShoal(p, world, target);
                return true;
            }
        });
    }

    /**
     * A chased shoal swims into the net on {@code anchor}: Fishing and Hunter XP for the catch. Returns false if no net of
     * the player's with room is set there. Public for the self-test (the fake player is never ticked).
     */
    public static boolean catchShoal(ServerPlayerEntity player, ServerWorld world, BlockPos anchor) {
        if (!isSet(player, world, anchor)) {
            return false;
        }
        NbtCompound n = net(player, world, anchor);
        if (n.getInt("fish") >= DriftNetFishing.CAPACITY) {
            return false;
        }
        n.putInt("fish", n.getInt("fish") + 1);
        XpService.add(player, Skill.FISHING, DriftNetFishing.fishingXp(SkillingUtil.level(player, Skill.FISHING)));
        XpService.add(player, Skill.HUNTER, DriftNetFishing.hunterXp(SkillingUtil.level(player, Skill.HUNTER)));
        if (n.getInt("fish") >= DriftNetFishing.CAPACITY) {
            Msg.red(player, "Your drift net is full.");
        } else {
            Msg.action(player, "The fish shoal swims into your drift net.");
        }
        return true;
    }
}
