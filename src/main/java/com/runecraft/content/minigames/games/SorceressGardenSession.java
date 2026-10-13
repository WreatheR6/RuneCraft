package com.runecraft.content.minigames.games;

import com.runecraft.action.ActionManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameRules;
import com.runecraft.osrs.content.minigames.SorceressGarden;
import com.runecraft.osrs.content.minigames.SorceressGarden.Season;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Sorceress's Garden: a central garden with the fountain (drink to leave) and the four seasonal
 * gardens around it (winter west, spring east, autumn south, summer north). Each garden's gate needs
 * its Thieving level; inside, elementals patrol a hedged corridor and teleport anyone within two
 * squares back to the central garden. Hide in the alcoves, reach the clearing and pick a sq'irk
 * (Farming XP) or two herbs (50 Farming XP); either sends you back to the fountain.
 */
public class SorceressGardenSession extends MinigameSession {
    /** Distance from the centre to each garden's gate; the central garden is 13x13. */
    private static final int GATE = 7;
    private static final int HEDGE = 3;
    private final Map<Season, List<MobEntity>> elementals = new EnumMap<>(Season.class);
    /** Game tick until which a player who was just teleported can't be caught again. */
    private final Map<UUID, Long> grace = new HashMap<>();

    public SorceressGardenSession(int id, int slot, ServerWorld world, String variant) {
        super(Minigame.SORCERESS_GARDEN, id, slot, world, variant);
    }

    /** Entry check: Osman only sends players who rescued Prince Ali. */
    public static String requirement(ServerPlayerEntity player) {
        if (ContentRegistry.quest(SorceressGarden.QUEST) != null && !QuestApi.isComplete(player, SorceressGarden.QUEST)) {
            return "Apprentice: The sorceress only lets friends of Osman into her garden. (Complete Prince Ali Rescue.)";
        }
        return null;
    }

    @Override
    protected int halfX() {
        return GATE + SorceressGarden.TREE;
    }

    @Override
    protected int halfZ() {
        return halfX();
    }

    @Override
    protected int height() {
        return 6;
    }

    // ------------------------------------------------------------------ geometry

    private static int dirX(Season s) {
        return s == Season.WINTER ? -1 : s == Season.SPRING ? 1 : 0;
    }

    private static int dirZ(Season s) {
        return s == Season.AUTUMN ? 1 : s == Season.SUMMER ? -1 : 0;
    }

    /** Arena offset of garden square (u along the garden away from the gate, v across it). */
    private static int ox(Season s, int u, int v) {
        return dirX(s) * (GATE + u) + (dirX(s) == 0 ? v : 0);
    }

    private static int oz(Season s, int u, int v) {
        return dirZ(s) * (GATE + u) + (dirZ(s) == 0 ? v : 0);
    }

    /** The garden a relative position lies in, or null for the central garden. */
    private static Season gardenAt(int dx, int dz) {
        if (Math.abs(dx) < GATE && Math.abs(dz) < GATE) {
            return null;
        }
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx < 0 ? Season.WINTER : Season.SPRING;
        }
        return dz > 0 ? Season.AUTUMN : Season.SUMMER;
    }

    private static int along(Season s, int dx, int dz) {
        return dirX(s) != 0 ? dirX(s) * dx - GATE : dirZ(s) * dz - GATE;
    }

    private static int across(Season s, int dx, int dz) {
        return dirX(s) != 0 ? dz : dx;
    }

    private void air(Season s, int u, int v) {
        for (int y = 1; y <= HEDGE; y++) {
            set(ox(s, u, v), y, oz(s, u, v), Blocks.AIR.getDefaultState());
        }
    }

    @Override
    protected void build() {
        room(Blocks.GRASS_BLOCK, Blocks.MOSSY_STONE_BRICKS, Blocks.MOSS_BLOCK);
        BlockState hedge = Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
        int h = halfX();
        // Hedges everywhere except the central garden, then the gardens are carved out of them.
        for (int dx = -h; dx <= h; dx++) {
            for (int dz = -h; dz <= h; dz++) {
                if (Math.abs(dx) < GATE && Math.abs(dz) < GATE) {
                    continue;
                }
                for (int y = 1; y <= HEDGE; y++) {
                    set(dx, y, dz, hedge);
                }
            }
        }
        int w = GATE - 1;
        for (Season s : Season.values()) {
            for (int u = 1; u <= SorceressGarden.TREE; u++) {
                for (int v = -w; v <= w; v++) {
                    if (SorceressGarden.walkable(s, u, v)) {
                        air(s, u, v);
                    }
                }
            }
            int edge = SorceressGarden.CLEARING_HALF_WIDTH;
            place(ox(s, 0, 0), oz(s, 0, 0), s.gate());
            place(ox(s, SorceressGarden.TREE, 0), oz(s, SorceressGarden.TREE, 0), s.tree());
            place(ox(s, SorceressGarden.TREE, edge), oz(s, SorceressGarden.TREE, edge), s.herbs());
            set(ox(s, SorceressGarden.TREE - 1, -edge), 1, oz(s, SorceressGarden.TREE - 1, -edge), flower(s));
        }
        // The central garden: the fountain in the middle and flower beds in the corners.
        place(0, 0, "sg_fountain");
        for (int[] c : new int[][]{{-5, -5}, {5, -5}, {-5, 5}, {5, 5}}) {
            set(c[0], 1, c[1], Blocks.FLOWERING_AZALEA.getDefaultState());
        }
    }

    private static BlockState flower(Season s) {
        return switch (s) {
            case WINTER -> Blocks.LILY_OF_THE_VALLEY.getDefaultState();
            case SPRING -> Blocks.OXEYE_DAISY.getDefaultState();
            case AUTUMN -> Blocks.ORANGE_TULIP.getDefaultState();
            case SUMMER -> Blocks.POPPY.getDefaultState();
        };
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayerEntity player, int index) {
        return rel(0, 1, 3);
    }

    @Override
    protected void start() {
        for (Season s : Season.values()) {
            List<MobEntity> list = new ArrayList<>();
            for (SorceressGarden.Patrol p : SorceressGarden.patrols(s)) {
                list.add(spawnElemental(s, p));
            }
            elementals.put(s, list);
        }
        broadcast("You appear in the middle of the sorceress's garden. Drink from the fountain to leave.");
    }

    private MobEntity spawnElemental(Season s, SorceressGarden.Patrol p) {
        MobEntity mob = spawn(s.elemental(), rel(ox(s, p.u(ticks), p.v(ticks)), 1, oz(s, p.u(ticks), p.v(ticks))));
        if (mob != null) {
            mob.setAiDisabled(true);
            mob.setInvulnerable(true);
        }
        return mob;
    }

    // ------------------------------------------------------------------ patrols

    @Override
    protected void gameTick() {
        for (Season s : Season.values()) {
            List<SorceressGarden.Patrol> patrols = SorceressGarden.patrols(s);
            List<MobEntity> mobs = elementals.get(s);
            if (mobs == null) {
                continue;
            }
            for (int i = 0; i < patrols.size(); i++) {
                SorceressGarden.Patrol p = patrols.get(i);
                MobEntity mob = mobs.get(i);
                if (mob == null || !mob.isAlive()) {
                    mob = spawnElemental(s, p);
                    mobs.set(i, mob);
                    if (mob == null) {
                        continue;
                    }
                }
                int u = p.u(ticks);
                int v = p.v(ticks);
                BlockPos at = rel(ox(s, u, v), 1, oz(s, u, v));
                BlockPos next = rel(ox(s, p.u(ticks + 1), p.v(ticks + 1)), 1, oz(s, p.u(ticks + 1), p.v(ticks + 1)));
                float yaw = facing(next.getX() - at.getX(), next.getZ() - at.getZ(), mob.getYaw());
                mob.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 0);
                mob.setHeadYaw(yaw);
                mob.setBodyYaw(yaw);
            }
        }
        for (ServerPlayerEntity p : online()) {
            watch(p);
        }
    }

    /** Minecraft yaw for a step of (sx, sz) blocks (0 = south, 90 = west); keeps {@code current} when standing still. */
    private static float facing(int sx, int sz, float current) {
        if (sx > 0) {
            return -90f;
        }
        if (sx < 0) {
            return 90f;
        }
        if (sz != 0) {
            return sz < 0 ? 180f : 0f;
        }
        return current;
    }

    /** Teleports a player out if an elemental of the garden they're in can see them. */
    private void watch(ServerPlayerEntity p) {
        if (grace.getOrDefault(p.getUuid(), 0L) > ticks) {
            return;
        }
        BlockPos r = relPos(p);
        Season s = gardenAt(r.getX(), r.getZ());
        if (s == null) {
            return;
        }
        int u = along(s, r.getX(), r.getZ());
        int v = across(s, r.getX(), r.getZ());
        if (SorceressGarden.seen(s, ticks, u, v)) {
            world.spawnParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 30, 0.3, 0.6, 0.3, 0.1);
            Msg.red(p, "An elemental force emanating from the garden teleports you away.");
            toCentre(p, s);
        }
    }

    /** Back to the central garden, just in front of a garden's gate (or by the fountain). */
    private void toCentre(ServerPlayerEntity p, Season s) {
        BlockPos at = s == null ? rel(0, 1, 3) : rel(ox(s, -2, 0), 1, oz(s, -2, 0));
        p.teleport(world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getYaw(), p.getPitch());
        grace.put(p.getUuid(), ticks + 2);
        ActionManager.cancel(p);
    }

    // ------------------------------------------------------------------ objects

    @Override
    protected boolean onBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
        if (blockId.equals("sg_fountain")) {
            Msg.game(player, "You drink from the fountain and feel yourself being pulled away...");
            removePlayer(player, true);
            return true;
        }
        for (Season s : Season.values()) {
            if (blockId.equals(s.gate())) {
                gate(player, s);
                return true;
            }
            if (blockId.equals(s.tree())) {
                pickSqirk(player, s);
                return true;
            }
            if (blockId.equals(s.herbs())) {
                pickHerbs(player, s);
                return true;
            }
        }
        return false;
    }

    private void gate(ServerPlayerEntity player, Season s) {
        BlockPos r = relPos(player);
        boolean inside = gardenAt(r.getX(), r.getZ()) == s && along(s, r.getX(), r.getZ()) > 0;
        if (inside) {
            toCentre(player, s);
            return;
        }
        int thieving = Players.data(player).skills().currentLevel(Skill.THIEVING);
        if (thieving < s.level()) {
            Msg.game(player, "You need a Thieving level of " + s.level() + " to enter the " + s.displayName() + " garden.");
            return;
        }
        BlockPos at = rel(ox(s, 1, 0), 1, oz(s, 1, 0));
        player.teleport(world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, player.getYaw(), player.getPitch());
        grace.put(player.getUuid(), ticks + 1);
        Msg.game(player, "You open the gate and slip into the " + s.displayName() + " garden.");
    }

    private void pickSqirk(ServerPlayerEntity player, Season s) {
        if (!Inv.hasRoomFor(player, s.fruit())) {
            Msg.game(player, "You don't have enough inventory space to pick a sq'irk.");
            return;
        }
        Inv.give(player, s.fruit(), 1);
        xp(player, Skill.FARMING, s.farmingXp());
        Msg.game(player, "You pick " + (s == Season.AUTUMN ? "an " : "a ") + s.id() + " sq'irk.");
        Msg.game(player, "An elemental force emanating from the garden teleports you away.");
        toCentre(player, null);
    }

    private void pickHerbs(ServerPlayerEntity player, Season s) {
        for (int i = 0; i < SorceressGarden.HERBS_PER_PICK; i++) {
            MinigameRules.Loot herb = MinigameRules.pick(s.herbTable(), random.nextDouble());
            Inv.give(player, herb.item(), 1);
        }
        xp(player, Skill.FARMING, SorceressGarden.HERB_FARMING_XP);
        Msg.game(player, "You pick some herbs from the garden.");
        Msg.game(player, "An elemental force emanating from the garden teleports you away.");
        toCentre(player, null);
    }

    @Override
    protected void onDeath(ServerPlayerEntity player) {
        toCentre(player, null);
    }

    @Override
    protected List<String> hud(ServerPlayerEntity player) {
        BlockPos r = relPos(player);
        Season s = gardenAt(r.getX(), r.getZ());
        List<String> lines = new ArrayList<>();
        lines.add("Sorceress's Garden");
        lines.add(s == null ? "Central garden" : s.displayName() + " garden");
        lines.add("Thieving: " + Players.data(player).skills().currentLevel(Skill.THIEVING));
        return lines;
    }
}
