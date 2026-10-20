package com.runecraft.content.minigames.games;

import com.runecraft.action.ActionManager;
import com.runecraft.combat.PoisonService;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameItems;
import com.runecraft.osrs.content.minigames.MinigameRules.PyramidPlunder;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pyramid Plunder in Jalsavrah (see {@link PyramidPlunder}): five minutes to loot up to eight rooms (Thieving 21/31/.../91).
 * Each room: pass the speartrap at its entrance (10 XP; a failure hurts), search the urns (a failed search lets a snake bite
 * and poison), open the Grand Gold Chest (a scarab swarm on a failure: no XP, the artefact still comes) and heave the
 * sarcophagus open with Strength (sometimes a mummy rises), then pick the four tomb doors: one leads on, the fourth always does
 * after three wrong ones, and the final room's doors lead out. The chest and sarcophagus may hold the Pharaoh's sceptre, which
 * gets the player thrown out at once. Simon Templeton buys the artefacts.
 */
public class PyramidPlunderSession extends MinigameSession {
    private static final int HALF = 10;
    private static final int SPEAR_Z = 6;
    /** Urn spots around the room's walls (x, z); the first 12 or 13 are used. */
    private static final int[][] URN_SPOTS = {{-8, 4}, {-8, 1}, {-8, -2}, {-8, -5}, {8, 4}, {8, 1}, {8, -2}, {8, -5}, {-5, -8},
            {5, -8}, {-4, 3}, {4, 3}, {0, 4}};
    private static final int[] DOOR_X = {-6, -2, 2, 6};

    private int room = 1;
    private final Set<Long> urnsLooted = new HashSet<>();
    private boolean chestOpened;
    private boolean sarcophagusOpened;
    private boolean spearPassed;
    private final int[] correctDoor = new int[PyramidPlunder.ROOMS];
    private final Set<Integer> doorsTried = new HashSet<>();
    private final Map<Long, Integer> doors = new HashMap<>();
    private int timeLeft = PyramidPlunder.GAME_TICKS;
    private int artefacts;
    /** Self-test: null rolls the sceptre normally, true always finds it, false never. */
    private Boolean forceSceptre;

    public PyramidPlunderSession(int id, int slot, ServerLevel world, String variant) {
        super(Minigame.PYRAMID_PLUNDER, id, slot, world, variant);
        for (int r = 0; r < correctDoor.length; r++) {
            correctDoor[r] = random.nextInt(PyramidPlunder.DOORS);
        }
    }

    @Override
    protected int halfX() {
        return HALF;
    }

    @Override
    protected int halfZ() {
        return HALF;
    }

    @Override
    protected int height() {
        return 7;
    }

    @Override
    protected void build() {
        room(Blocks.SMOOTH_SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.CHISELED_SANDSTONE);
        // The speartrap blocks the entrance passage: a wall across the room with the trap in it.
        BlockState wall = Blocks.CUT_SANDSTONE.defaultBlockState();
        for (int x = -HALF; x <= HALF; x++) {
            for (int y = 1; y <= 7; y++) {
                set(x, y, SPEAR_Z, wall);
            }
        }
        place(0, SPEAR_Z, "pp_speartrap");
        int urns = PyramidPlunder.URNS[room - 1];
        for (int i = 0; i < urns; i++) {
            place(URN_SPOTS[i][0], URN_SPOTS[i][1], "pp_urn");
        }
        place(0, 0, "pp_chest");
        place(0, -5, "pp_sarcophagus");
        doors.clear();
        for (int d = 0; d < DOOR_X.length; d++) {
            set(DOOR_X[d], 1, -HALF - 1, com.runecraft.content.minigames.Arena.module("pp_door"));
            set(DOOR_X[d], 2, -HALF - 1, wall);
            doors.put(rel(DOOR_X[d], 1, -HALF - 1).asLong(), d);
        }
        place(3, 9, "minigame_exit");
        urnsLooted.clear();
        doorsTried.clear();
        chestOpened = false;
        sarcophagusOpened = false;
        spearPassed = false;
        for (LivingEntity e : living(null)) {
            e.discard();
        }
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
        return rel(index % 3 - 1, 1, 8);
    }

    @Override
    protected void start() {
        broadcast("You have five minutes to plunder the pyramid. Pass the speartrap to enter room 1.");
    }

    private int thieving(ServerPlayer p) {
        return Players.data(p).skills().currentLevel(Skill.THIEVING);
    }

    private void artefact(ServerPlayer p, String item) {
        reward(p, item, 1);
        artefacts++;
    }

    /** The sceptre roll of the chest and sarcophagus; the guardian mummy throws the finder out at once. */
    private boolean sceptre(ServerPlayer p) {
        boolean found = forceSceptre != null ? forceSceptre : random.nextInt(PyramidPlunder.SCEPTRE_CHANCE[room - 1]) == 0;
        if (!found) {
            return false;
        }
        reward(p, "pharaohs_sceptre", 1);
        p.getServer().getPlayerList().broadcastSystemMessage(Component.literal(p.getName().getString()
                + " has found the Pharaoh's sceptre!").withStyle(ChatFormatting.GOLD), false);
        Msg.red(p, "You find the Pharaoh's sceptre! The guardian mummy is furious and throws you out of the pyramid.");
        end(true);
        return true;
    }

    private void hurtTrap(ServerPlayer p) {
        hurt(p, 1 + random.nextInt(PyramidPlunder.TRAP_MAX_DAMAGE));
    }

    @Override
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
        switch (blockId) {
            case "pp_speartrap" -> spear(player);
            case "pp_urn" -> urn(player, pos);
            case "pp_chest" -> chest(player);
            case "pp_sarcophagus" -> sarcophagus(player);
            case "pp_door" -> door(player, pos);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void spear(ServerPlayer p) {
        if (spearPassed && relPos(p).getZ() < SPEAR_Z) {
            Msg.game(p, "You've already passed the speartrap.");
            return;
        }
        if (random.nextDouble() < PyramidPlunder.spearChance(thieving(p), room)) {
            spearPassed = true;
            xp(p, Skill.THIEVING, PyramidPlunder.SPEAR_XP);
            BlockPos to = rel(0, 1, SPEAR_Z - 1);
            p.teleportTo(world, to.getX() + 0.5, to.getY(), to.getZ() + 0.5, p.getYRot(), p.getXRot());
            Msg.game(p, "You make it past the speartrap.");
        } else {
            hurtTrap(p);
            Msg.red(p, "The spears catch you as you try to pass!");
        }
    }

    private void urn(ServerPlayer p, BlockPos pos) {
        if (urnsLooted.contains(pos.asLong())) {
            Msg.game(p, "You've already looted this urn.");
            return;
        }
        if (random.nextDouble() < PyramidPlunder.urnChance(thieving(p), room)) {
            urnsLooted.add(pos.asLong());
            artefact(p, PyramidPlunder.artefact(PyramidPlunder.URN, room, random.nextDouble()));
            xp(p, Skill.THIEVING, PyramidPlunder.URN_XP[room - 1]);
            Msg.game(p, "You search the urn and find an artefact.");
        } else {
            int dmg = 1 + random.nextInt(PyramidPlunder.TRAP_MAX_DAMAGE);
            hurt(p, dmg);
            PoisonService.poison(p, dmg, ActionManager.gameTick());
            Msg.red(p, "A snake bites you!");
        }
    }

    private void chest(ServerPlayer p) {
        if (chestOpened) {
            Msg.game(p, "The chest is empty.");
            return;
        }
        chestOpened = true;
        boolean swarm = random.nextDouble() >= PyramidPlunder.chestChance(thieving(p));
        if (sceptre(p)) {
            return;
        }
        artefact(p, PyramidPlunder.artefact(PyramidPlunder.CHEST, room, random.nextDouble()));
        if (swarm) {
            hurt(p, PyramidPlunder.SCARAB_DAMAGE);
            Mob s = spawn("pp_scarab_swarm", rel(0, 1, 1));
            if (s != null) {
                s.setTarget(p);
            }
            Msg.red(p, "A swarm of scarabs pours out of the chest!");
        } else {
            xp(p, Skill.THIEVING, PyramidPlunder.CHEST_XP[room - 1]);
            Msg.game(p, "You open the Grand Gold Chest.");
        }
    }

    private void sarcophagus(ServerPlayer p) {
        if (sarcophagusOpened) {
            Msg.game(p, "The sarcophagus has been plundered.");
            return;
        }
        int strength = Players.data(p).skills().currentLevel(Skill.STRENGTH);
        if (random.nextDouble() >= PyramidPlunder.sarcophagusChance(strength, room)) {
            Msg.game(p, "You fail to move the lid.");
            return;
        }
        sarcophagusOpened = true;
        xp(p, Skill.STRENGTH, PyramidPlunder.SARCOPHAGUS_XP[room - 1]);
        if (random.nextDouble() < PyramidPlunder.mummyChance(room)) {
            Mob m = spawn("pp_mummy", rel(1, 1, -4));
            if (m != null) {
                m.setTarget(p);
            }
            Msg.red(p, "A mummy rises from the sarcophagus!");
        } else if (sceptre(p)) {
            return;
        }
        artefact(p, PyramidPlunder.artefact(PyramidPlunder.SARCOPHAGUS, room, random.nextDouble()));
        Msg.game(p, "You heave the lid off the sarcophagus.");
    }

    private void door(ServerPlayer p, BlockPos pos) {
        Integer d = doors.get(pos.asLong());
        if (d == null) {
            return;
        }
        if (room >= PyramidPlunder.ROOMS) {
            Msg.game(p, "The door leads out of the pyramid.");
            end(true);
            return;
        }
        int need = PyramidPlunder.ROOM_LEVELS[room];
        if (Players.data(p).skills().level(Skill.THIEVING) < need) {
            Msg.game(p, "You need a Thieving level of " + need + " to enter the next room. Leave by the exit.");
            return;
        }
        if (doorsTried.contains(d)) {
            Msg.game(p, "This door leads to a dead end.");
            return;
        }
        boolean lockpick = Inv.has(p, "lockpick", 1);
        if (random.nextDouble() >= PyramidPlunder.doorChance(thieving(p), room, lockpick)) {
            Msg.game(p, "You fail to pick the lock.");
            return;
        }
        double xp = PyramidPlunder.DOOR_XP[room - 1];
        xp(p, Skill.THIEVING, lockpick ? xp / 2 : xp);
        boolean through = d == correctDoor[room - 1] || doorsTried.size() >= PyramidPlunder.DOORS - 1;
        if (!through) {
            doorsTried.add(d);
            Msg.game(p, "You pick the lock, but the door only leads to a dead end.");
            return;
        }
        room++;
        build();
        for (ServerPlayer q : online()) {
            BlockPos at = spawnPoint(q, 0);
            q.teleportTo(world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, q.getYRot(), q.getXRot());
        }
        broadcast("You enter room " + room + " of the pyramid.");
    }

    @Override
    protected void gameTick() {
        if (--timeLeft <= 0) {
            broadcast("Your time in the pyramid is up! The guardian mummy shows you out.");
            end(true);
        }
    }

    @Override
    protected void onEnd(boolean success) {
        for (ServerPlayer p : online()) {
            MinigameData data = MinigameData.of(p);
            data.incrementStat(p.getUUID(), "kc:" + game.id());
            if (room > data.stat(p.getUUID(), "pyramid_plunder_room")) {
                data.setStat(p.getUUID(), "pyramid_plunder_room", room);
            }
        }
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        return List.of("Pyramid Plunder", "Room: " + room + " / " + PyramidPlunder.ROOMS, "Time left: " + time(timeLeft),
                "Artefacts: " + artefacts);
    }

    /** Simon Templeton buys every artefact in the inventory. */
    public static void sellArtefacts(ServerPlayer player) {
        int total = 0;
        for (Object[] a : MinigameItems.ARTEFACTS) {
            int n = Inv.remove(player, (String) a[0], 1000);
            total += n * (Integer) a[2];
        }
        if (total == 0) {
            Msg.game(player, "Simon Templeton: Bring me artefacts from the pyramids and I'll pay you handsomely!");
            return;
        }
        Inv.give(player, "coins", total);
        Msg.game(player, "Simon Templeton pays you " + String.format("%,d", total) + " coins for your artefacts.");
    }

    // ------------------------------------------------------------------ self-test hooks

    public int selfTestRoom() {
        return room;
    }

    public int selfTestTimeLeft() {
        return timeLeft;
    }

    public void selfTestSceptre(Boolean always) {
        forceSceptre = always;
    }

    public BlockPos selfTestSpear() {
        return rel(0, 1, SPEAR_Z);
    }

    public BlockPos selfTestSpot(int x, int z) {
        return rel(x, 1, z);
    }

    public List<BlockPos> selfTestUrns() {
        List<BlockPos> out = new ArrayList<>();
        for (int i = 0; i < PyramidPlunder.URNS[room - 1]; i++) {
            out.add(rel(URN_SPOTS[i][0], 1, URN_SPOTS[i][1]));
        }
        return out;
    }

    public boolean selfTestUrnLooted(BlockPos pos) {
        return urnsLooted.contains(pos.asLong());
    }

    public BlockPos selfTestChest() {
        return rel(0, 1, 0);
    }

    public BlockPos selfTestSarcophagus() {
        return rel(0, 1, -5);
    }

    public boolean selfTestSarcophagusOpened() {
        return sarcophagusOpened;
    }

    public boolean selfTestSpearPassed() {
        return spearPassed;
    }

    public List<BlockPos> selfTestDoors() {
        List<BlockPos> out = new ArrayList<>();
        for (int x : DOOR_X) {
            out.add(rel(x, 1, -HALF - 1));
        }
        return out;
    }

    public int selfTestCorrectDoor() {
        return correctDoor[room - 1];
    }

    public int selfTestDoorsTried() {
        return doorsTried.size();
    }

    public int selfTestMobs() {
        return mobsAlive();
    }
}
