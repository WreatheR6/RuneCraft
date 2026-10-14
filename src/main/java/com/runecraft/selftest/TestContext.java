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
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;

/**
 * The self-test's world: a Fabric fake player (or, in real mode, the online player who ran the command) in Gielinor standing on a barrier-walled platform high above
 * Lumbridge (chunks the smoke test force-loads), reset to fresh, maxed player data before every check.
 */
public final class TestContext {
    /** Platform centre: above Lumbridge, clear of every building and inside the smoke test's force-loaded area. */
    public static final BlockPos PLATFORM = new BlockPos(40, 250, 40);
    public static final int RADIUS = 12;

    public final MinecraftServer server;
    public final ServerWorld world;
    public ServerPlayerEntity player;
    /** The online player the checks run against in real mode, else null (a fake player is used). */
    private final ServerPlayerEntity real;
    private NbtCompound savedData;
    private final List<ItemStack> savedInventory = new ArrayList<>();
    private ServerWorld savedWorld;
    private double savedX;
    private double savedY;
    private double savedZ;
    private boolean savedCreative;
    private boolean savedSpectator;

    TestContext(MinecraftServer server, ServerWorld world, ServerPlayerEntity real) {
        this.server = server;
        this.world = world;
        this.real = real;
    }

    /** True when the checks run against a real, online player. */
    public boolean realPlayer() {
        return real != null;
    }

    void setUp() {
        world.getChunk(PLATFORM.getX() >> 4, PLATFORM.getZ() >> 4);
        platform(Blocks.SMOOTH_STONE.getDefaultState(), Blocks.BARRIER.getDefaultState());
        if (real == null) {
            player = FakePlayer.get(world);
            return;
        }
        player = real;
        savedData = Players.data(player).toNbt();
        savedInventory.clear();
        for (int i = 0; i < player.getInventory().size(); i++) {
            savedInventory.add(player.getInventory().getStack(i).copy());
        }
        savedWorld = (ServerWorld) player.getWorld();
        savedX = player.getX();
        savedY = player.getY();
        savedZ = player.getZ();
        savedCreative = player.isCreative();
        savedSpectator = player.isSpectator();
        // Bosses and raid rooms ignore creative and spectator players.
        if (savedCreative || savedSpectator) {
            command("gamemode survival");
        }
        player.setInvulnerable(true);
    }

    void tearDown() {
        reset();
        platform(Blocks.AIR.getDefaultState(), Blocks.AIR.getDefaultState());
        Players.state(server).remove(player.getUuid());
        if (real == null) {
            return;
        }
        Players.data(player).fromNbt(savedData);
        XpService.updateMaxHealth(player);
        player.setHealth(player.getMaxHealth());
        player.getInventory().clear();
        for (int i = 0; i < savedInventory.size(); i++) {
            player.getInventory().setStack(i, savedInventory.get(i));
        }
        player.setInvulnerable(false);
        if (savedCreative) {
            command("gamemode creative");
        } else if (savedSpectator) {
            command("gamemode spectator");
        }
        player.teleport(savedWorld, savedX, savedY, savedZ, player.getYaw(), player.getPitch());
    }

    /** Runs a command as the (real) player with operator rights. */
    private void command(String command) {
        server.getCommandManager().executeWithPrefix(player.getCommandSource().withLevel(4), command);
    }

    /** Real mode: tops the player's health up so a boss or minigame can't kill it mid-check. */
    public void heal() {
        if (real != null && player.getHealth() < player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
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
