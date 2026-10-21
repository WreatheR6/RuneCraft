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
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

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
    private final Map<Season, List<Mob>> elementals = new EnumMap<>(Season.class);
    /** Game tick until which a player who was just teleported can't be caught again. */
    private final Map<UUID, Long> grace = new HashMap<>();

    public SorceressGardenSession(int id, int slot, ServerLevel world, String variant) {
        super(Minigame.SORCERESS_GARDEN, id, slot, world, variant);
    }

    /** Entry check: Osman only sends players who rescued Prince Ali. */
    public static String requirement(ServerPlayer player) {
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
            set(ox(s, u, v), y, oz(s, u, v), Blocks.AIR.defaultBlockState());
        }
    }

    @Override
    protected void build() {
        room(Blocks.GRASS_BLOCK, Blocks.MOSSY_STONE_BRICKS, Blocks.MOSS_BLOCK);
        BlockState hedge = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
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
            set(c[0], 1, c[1], Blocks.FLOWERING_AZALEA.defaultBlockState());
        }
    }

    private static BlockState flower(Season s) {
        return switch (s) {
            case WINTER -> Blocks.LILY_OF_THE_VALLEY.defaultBlockState();
            case SPRING -> Blocks.OXEYE_DAISY.defaultBlockState();
            case AUTUMN -> Blocks.ORANGE_TULIP.defaultBlockState();
            case SUMMER -> Blocks.POPPY.defaultBlockState();
        };
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
        return rel(0, 1, 3);
    }

    @Override
    protected void start() {
        for (Season s : Season.values()) {
            List<Mob> list = new ArrayList<>();
            for (SorceressGarden.Patrol p : SorceressGarden.patrols(s)) {
                list.add(spawnElemental(s, p));
            }
            elementals.put(s, list);
        }
        broadcast("You appear in the middle of the sorceress's garden. Drink from the fountain to leave.");
    }

    private Mob spawnElemental(Season s, SorceressGarden.Patrol p) {
        Mob mob = spawn(s.elemental(), rel(ox(s, p.u(ticks), p.v(ticks)), 1, oz(s, p.u(ticks), p.v(ticks))));
        if (mob != null) {
            mob.setNoAi(true);
            mob.setInvulnerable(true);
        }
        return mob;
    }

    // ------------------------------------------------------------------ patrols

    @Override
    protected void gameTick() {
        for (Season s : Season.values()) {
            List<SorceressGarden.Patrol> patrols = SorceressGarden.patrols(s);
            List<Mob> mobs = elementals.get(s);
            if (mobs == null) {
                continue;
            }
            for (int i = 0; i < patrols.size(); i++) {
                SorceressGarden.Patrol p = patrols.get(i);
                Mob mob = mobs.get(i);
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
                float yaw = facing(next.getX() - at.getX(), next.getZ() - at.getZ(), mob.getYRot());
                mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 0);
                mob.setYHeadRot(yaw);
                mob.setYBodyRot(yaw);
            }
        }
        for (ServerPlayer p : online()) {
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
    private void watch(ServerPlayer p) {
        if (grace.getOrDefault(p.getUUID(), 0L) > ticks) {
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
            world.sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 30, 0.3, 0.6, 0.3, 0.1);
            Msg.red(p, "An elemental force emanating from the garden teleports you away.");
            toCentre(p, s);
        }
    }

    /** Back to the central garden, just in front of a garden's gate (or by the fountain). */
    private void toCentre(ServerPlayer p, Season s) {
        BlockPos at = s == null ? rel(0, 1, 3) : rel(ox(s, -2, 0), 1, oz(s, -2, 0));
        p.teleportTo(world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getYRot(), p.getXRot());
        grace.put(p.getUUID(), ticks + 2);
        ActionManager.cancel(p);
    }

    // ------------------------------------------------------------------ objects

    @Override
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
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

    private void gate(ServerPlayer player, Season s) {
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
        player.teleportTo(world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, player.getYRot(), player.getXRot());
        grace.put(player.getUUID(), ticks + 1);
        Msg.game(player, "You open the gate and slip into the " + s.displayName() + " garden.");
    }

    private void pickSqirk(ServerPlayer player, Season s) {
        if (!Inv.hasRoomFor(player, s.fruit())) {
            Msg.game(player, "You don't have enough inventory space to pick a sq'irk.");
            return;
        }
        Inv.give(player, s.fruit(), 1);
        xp(player, Skill.FARMING, s.farmingXp());
        Msg.game(player, "You pick " + (s == Season.AUTUMN ? "an " : "a ") + s.id() + " sq'irk.");
        // Rocky (the Thieving pet) can be found picking sq'irks: 1/(10,339 - 25 x Thieving level).
        int thieving = Players.data(player).skills().level(Skill.THIEVING);
        if (random.nextDouble() < SorceressGarden.rockyChance(thieving) && com.runecraft.osrs.ItemCatalog.get("pet_rocky") != null
                && !Players.data(player).collectionLog().contains("pet_rocky")) {
            reward(player, "pet_rocky", 1);
            player.getServer().getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.literal(player.getName().getString()
                    + " has a funny feeling like they're being followed.").withStyle(net.minecraft.ChatFormatting.GOLD), false);
        }
        Msg.game(player, "An elemental force emanating from the garden teleports you away.");
        toCentre(player, null);
    }

    private void pickHerbs(ServerPlayer player, Season s) {
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
    protected void onDeath(ServerPlayer player) {
        toCentre(player, null);
    }

    // ------------------------------------------------------------------ self-test hooks

    public long selfTestTicks() {
        return ticks;
    }

    /** The arena position (feet) of garden square (u, v) of a season. */
    public BlockPos selfTestSquare(Season s, int u, int v) {
        return rel(ox(s, u, v), 1, oz(s, u, v));
    }

    public BlockPos selfTestGate(Season s) {
        return selfTestSquare(s, 0, 0);
    }

    public BlockPos selfTestTree(Season s) {
        return selfTestSquare(s, SorceressGarden.TREE, 0);
    }

    public BlockPos selfTestHerbs(Season s) {
        return selfTestSquare(s, SorceressGarden.TREE, SorceressGarden.CLEARING_HALF_WIDTH);
    }

    public BlockPos selfTestFountain() {
        return rel(0, 1, 0);
    }

    /** The garden the player stands in (null: the central garden) and the square, as {u, v}. */
    public Season selfTestGarden(ServerPlayer p) {
        BlockPos r = relPos(p);
        return gardenAt(r.getX(), r.getZ());
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        BlockPos r = relPos(player);
        Season s = gardenAt(r.getX(), r.getZ());
        List<String> lines = new ArrayList<>();
        lines.add("Sorceress's Garden");
        lines.add(s == null ? "Central garden" : s.displayName() + " garden");
        lines.add("Thieving: " + Players.data(player).skills().currentLevel(Skill.THIEVING));
        return lines;
    }
}
