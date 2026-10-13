package com.runecraft.selftest;

import com.runecraft.action.ActionManager;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.content.raids.RaidInstance;
import com.runecraft.content.raids.RaidManager;
import com.runecraft.osrs.Experience;
import com.runecraft.osrs.QuestInfo;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.skill.XpService;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/**
 * The self-test's world: a Fabric fake player in Gielinor standing on a barrier-walled platform high above
 * Lumbridge (chunks the smoke test force-loads), reset to fresh, maxed player data before every check.
 */
public final class TestContext {
    /** Platform centre: above Lumbridge, clear of every building and inside the smoke test's force-loaded area. */
    public static final BlockPos PLATFORM = new BlockPos(40, 250, 40);
    public static final int RADIUS = 12;

    public final MinecraftServer server;
    public final ServerWorld world;
    public ServerPlayerEntity player;

    TestContext(MinecraftServer server, ServerWorld world) {
        this.server = server;
        this.world = world;
    }

    void setUp() {
        world.getChunk(PLATFORM.getX() >> 4, PLATFORM.getZ() >> 4);
        platform(Blocks.SMOOTH_STONE.getDefaultState(), Blocks.BARRIER.getDefaultState());
        player = FakePlayer.get(world);
    }

    void tearDown() {
        reset();
        platform(Blocks.AIR.getDefaultState(), Blocks.AIR.getDefaultState());
        Players.state(server).remove(player.getUuid());
    }

    private void platform(BlockState floor, BlockState wall) {
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                world.setBlockState(PLATFORM.add(dx, -1, dz), floor, Block.NOTIFY_LISTENERS);
                if (Math.abs(dx) == RADIUS || Math.abs(dz) == RADIUS) {
                    for (int dy = 0; dy < 4; dy++) {
                        world.setBlockState(PLATFORM.add(dx, dy, dz), wall, Block.NOTIFY_LISTENERS);
                    }
                }
            }
        }
    }

    /** Fresh, maxed player data, empty inventory, standing at the platform centre. */
    void reset() {
        ActionManager.cancel(player);
        MinigameSession session = MinigameManager.session(player);
        if (session != null) {
            session.end(false);
        }
        RaidInstance raid = RaidManager.of(player);
        if (raid != null) {
            RaidManager.selfTestClose(raid);
        }
        Players.state(server).remove(player.getUuid());
        PlayerData data = Players.data(player);
        for (Skill s : Skill.values()) {
            data.skills().setXpTenths(s, Experience.xpForLevel(99) * 10);
        }
        data.setPrayerPoints(99);
        data.setVisitedGielinor(true);
        XpService.updateMaxHealth(player);
        player.setHealth(player.getMaxHealth());
        player.getInventory().clear();
        home();
    }

    /** Back to the platform centre. */
    public void home() {
        player.teleport(world, PLATFORM.getX() + 0.5, PLATFORM.getY(), PLATFORM.getZ() + 0.5, player.getYaw(), player.getPitch());
    }

    /** Marks every quest except {@code except} (nullable) complete, without rewards. */
    public void completeQuests(QuestInfo except) {
        PlayerData data = Players.data(player);
        for (QuestInfo q : ContentRegistry.quests()) {
            if (except == null || !q.questId().equals(except.questId())) {
                data.setQuestStage(q, q.finalStage());
            }
        }
    }

    /** Removes every non-player entity around the platform (bosses, minions, drops, hazards). */
    void cleanUp() {
        ActionManager.cancel(player);
        Box box = new Box(PLATFORM.add(-48, -60, -48), PLATFORM.add(48, 60, 48));
        for (Entity e : world.getEntitiesByClass(Entity.class, box, e -> !(e instanceof PlayerEntity))) {
            e.discard();
        }
        player.getInventory().clear();
    }

    /** Spot next to the player on the platform. */
    public BlockPos near(int dx, int dz) {
        return PLATFORM.add(dx, 0, dz);
    }
}
