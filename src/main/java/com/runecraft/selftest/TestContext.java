package com.runecraft.selftest;

import com.runecraft.action.ActionManager;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.content.quests.QuestsState;
import com.runecraft.content.slayer.SlayerState;
import com.runecraft.content.systems.SysNet;
import com.runecraft.content.systems.SysState;
import com.runecraft.network.ServerNetworking;
import com.runecraft.content.raids.RaidInstance;
import com.runecraft.content.raids.RaidManager;
import com.runecraft.osrs.Experience;
import com.runecraft.osrs.QuestInfo;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.skill.XpService;
import com.runecraft.platform.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

/**
 * The self-test's world: a fake player ({@link com.runecraft.platform.FakePlayer}) (or, in real mode, the online player who ran the command) in Gielinor standing on a barrier-walled platform high above
 * Lumbridge (chunks the smoke test force-loads), reset to fresh, maxed player data before every check.
 */
public final class TestContext {
    /** Platform centre: above Lumbridge, clear of every building and inside the smoke test's force-loaded area. */
    public static final BlockPos PLATFORM = new BlockPos(40, 250, 40);
    public static final int RADIUS = 12;

    public final MinecraftServer server;
    public final ServerLevel world;
    public ServerPlayer player;
    /** The online player the checks run against in real mode, else null (a fake player is used). */
    private final ServerPlayer real;
    private CompoundTag savedData;
    private final List<ItemStack> savedInventory = new ArrayList<>();
    private ServerLevel savedWorld;
    private double savedX;
    private double savedY;
    private double savedZ;
    private boolean savedCreative;
    private boolean savedSpectator;
    /** Real mode: the player's per-module world-save state (systems, slayer, minigames, quests), see {@link #moduleStates}. */
    private final List<Object> savedModuleState = new ArrayList<>();
    /** Blocks the checks placed (skill nodes, stations, patches), cleared after every check. */
    private final java.util.Set<BlockPos> placed = new java.util.LinkedHashSet<>();

    TestContext(MinecraftServer server, ServerLevel world, ServerPlayer real) {
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
        platform(Blocks.SMOOTH_STONE.defaultBlockState(), Blocks.BARRIER.defaultBlockState());
        if (real == null) {
            player = FakePlayer.get(world);
            return;
        }
        player = real;
        savedData = Players.data(player).toNbt();
        savedInventory.clear();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            savedInventory.add(player.getInventory().getItem(i).copy());
        }
        savedWorld = (ServerLevel) player.level();
        savedX = player.getX();
        savedY = player.getY();
        savedZ = player.getZ();
        savedCreative = player.isCreative();
        savedSpectator = player.isSpectator();
        savedModuleState.clear();
        for (ModuleState s : moduleStates()) {
            savedModuleState.add(s.save(player.getUUID()));
        }
        // Bosses and raid rooms ignore creative and spectator players.
        if (savedCreative || savedSpectator) {
            command("gamemode survival");
        }
        player.setInvulnerable(true);
    }

    void tearDown() {
        reset();
        platform(Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState());
        Players.state(server).remove(player.getUUID());
        if (real == null) {
            return;
        }
        Players.data(player).fromNbt(savedData, server.registryAccess());
        List<ModuleState> states = moduleStates();
        for (int i = 0; i < states.size() && i < savedModuleState.size(); i++) {
            states.get(i).restore(player.getUUID(), savedModuleState.get(i));
        }
        XpService.updateMaxHealth(player);
        player.setHealth(player.getMaxHealth());
        player.getInventory().clearContent();
        for (int i = 0; i < savedInventory.size(); i++) {
            player.getInventory().setItem(i, savedInventory.get(i));
        }
        player.setInvulnerable(false);
        if (savedCreative) {
            command("gamemode creative");
        } else if (savedSpectator) {
            command("gamemode spectator");
        }
        player.teleportTo(savedWorld, savedX, savedY, savedZ, player.getYRot(), player.getXRot());
        ServerNetworking.sync(player);
        SysNet.sendState(player);
    }

    /** One module's per-player world-save state: an opaque save, and a restore (null = drop it, so it starts fresh). */
    private interface ModuleState {
        Object save(java.util.UUID player);

        void restore(java.util.UUID player, Object saved);
    }

    /**
     * Per-player state the modules keep outside PlayerData: worn extra slots, spellbook, diaries and Combat Achievements
     * (systems), the Slayer profile, minigame points and flags, quest flags and kill counters, and boss loot held for the
     * player (the Lunar Chest's Moons, the fallen Royal Titans' open choice). Reset before every check so
     * checks don't leak into each other, and put back for a real player at the end.
     */
    private List<ModuleState> moduleStates() {
        SysState sys = SysState.get(server);
        SlayerState slayer = SlayerState.of(server);
        MinigameData minigames = MinigameData.get(server);
        QuestsState quests = QuestsState.get(server);
        return List.of(
                new ModuleState() {
                    public Object save(java.util.UUID p) {
                        return sys.selfTestSave(p);
                    }

                    public void restore(java.util.UUID p, Object saved) {
                        sys.selfTestRestore(p, saved);
                    }
                },
                new ModuleState() {
                    public Object save(java.util.UUID p) {
                        return slayer.selfTestSave(p);
                    }

                    public void restore(java.util.UUID p, Object saved) {
                        slayer.selfTestRestore(p, saved);
                    }
                },
                new ModuleState() {
                    public Object save(java.util.UUID p) {
                        return minigames.selfTestSave(p);
                    }

                    public void restore(java.util.UUID p, Object saved) {
                        minigames.selfTestRestore(p, saved);
                    }
                },
                new ModuleState() {
                    public Object save(java.util.UUID p) {
                        return quests.selfTestSave(p);
                    }

                    public void restore(java.util.UUID p, Object saved) {
                        quests.selfTestRestore(p, saved);
                    }
                },
                new ModuleState() { // the Moons pacified for the Lunar Chest
                    public Object save(java.util.UUID p) {
                        return com.runecraft.content.bosses.BossSelfTests.lunarChestSave(server, p);
                    }

                    public void restore(java.util.UUID p, Object saved) {
                        com.runecraft.content.bosses.BossSelfTests.lunarChestRestore(server, p, saved);
                    }
                },
                new ModuleState() { // the fallen Royal Titans' open loot choice (a Branda kill leaves it open)
                    public Object save(java.util.UUID p) {
                        return com.runecraft.content.bosses.BossSelfTests.titansChoiceSave(server, p);
                    }

                    public void restore(java.util.UUID p, Object saved) {
                        com.runecraft.content.bosses.BossSelfTests.titansChoiceRestore(server, p, saved);
                    }
                });
    }

    /** Runs a command as the (real) player with operator rights. */
    private void command(String command) {
        server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(4), command);
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
                world.setBlock(PLATFORM.offset(dx, -1, dz), floor, Block.UPDATE_CLIENTS);
                if (Math.abs(dx) == RADIUS || Math.abs(dz) == RADIUS) {
                    for (int dy = 0; dy < 4; dy++) {
                        world.setBlock(PLATFORM.offset(dx, dy, dz), wall, Block.UPDATE_CLIENTS);
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
        Players.state(server).remove(player.getUUID());
        for (ModuleState s : moduleStates()) {
            s.restore(player.getUUID(), null);
        }
        PlayerData data = Players.data(player);
        for (Skill s : Skill.values()) {
            data.skills().setXpTenths(s, Experience.xpForLevel(99) * 10);
        }
        data.setPrayerPoints(99);
        data.setVisitedGielinor(true);
        XpService.updateMaxHealth(player);
        player.setHealth(player.getMaxHealth());
        // Poison, venom and their immunities live outside the player's data, and the fake player is never ticked, so venom
        // from one check's fight would otherwise block the next check's poison.
        com.runecraft.combat.PoisonService.clear(player);
        player.getInventory().clearContent();
        home();
    }

    /** Back to the platform centre. */
    public void home() {
        player.teleportTo(world, PLATFORM.getX() + 0.5, PLATFORM.getY(), PLATFORM.getZ() + 0.5, player.getYRot(), player.getXRot());
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

    /**
     * Places a block for a check (a tree, rock, station or patch next to the platform); it is removed again after the
     * check. Returns the position.
     */
    public BlockPos place(BlockPos pos, BlockState state) {
        placed.add(pos.immutable());
        world.setBlock(pos, state, Block.UPDATE_ALL);
        return pos;
    }

    /** Records a block a check made the game place (a lit fire, a laid trap) so it is removed after the check. */
    public void track(BlockPos pos) {
        placed.add(pos.immutable());
    }

    /** Removes every non-player entity around the platform (bosses, minions, drops, hazards) and the blocks checks placed. */
    void cleanUp() {
        ActionManager.cancel(player);
        for (BlockPos pos : placed) {
            world.setBlock(pos, pos.getY() == PLATFORM.getY() - 1 && Math.abs(pos.getX() - PLATFORM.getX()) <= RADIUS
                    && Math.abs(pos.getZ() - PLATFORM.getZ()) <= RADIUS ? Blocks.SMOOTH_STONE.defaultBlockState()
                    : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        placed.clear();
        AABB box = new AABB(Vec3.atLowerCornerOf(PLATFORM.offset(-48, -60, -48)), Vec3.atLowerCornerOf(PLATFORM.offset(48, 60, 48)));
        for (Entity e : world.getEntitiesOfClass(Entity.class, box, e -> !(e instanceof Player))) {
            e.discard();
        }
        player.getInventory().clearContent();
    }

    /** Spot next to the player on the platform. */
    public BlockPos near(int dx, int dz) {
        return PLATFORM.offset(dx, 0, dz);
    }
}
