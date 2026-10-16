package com.runecraft.content.raids;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.collection.CollectionLog;
import com.runecraft.combat.PoisonService;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.content.raids.Invocation;
import com.runecraft.osrs.content.raids.RaidItems;
import com.runecraft.osrs.content.raids.RaidLayouts;
import com.runecraft.osrs.content.raids.RaidMode;
import com.runecraft.osrs.content.raids.RaidRewards;
import com.runecraft.osrs.content.raids.RaidScaling;
import com.runecraft.osrs.content.raids.RaidType;
import com.runecraft.osrs.content.raids.RoomDef;
import com.runecraft.osrs.content.raids.RaidsContent;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * One running raid: its party, rooms, points, deaths, raid level and rewards. Rooms are built
 * lazily one ahead of the party; a room starts when a living party member walks into it and the
 * exit barrier opens when its logic reports completion.
 */
public final class RaidInstance {
    /** Entities' maximum health attribute is clamped at 1024; anything above that is held in reserve. */
    private static final int MC_MAX_HEALTH = 1024;

    public final int slot;
    public final RaidType type;
    public final ServerWorld world;
    public final List<RoomDef> rooms;
    public final Random random;
    private final List<UUID> party = new ArrayList<>();
    private final Map<UUID, String> names = new HashMap<>();
    private final Map<UUID, Integer> points = new LinkedHashMap<>();
    private final Map<UUID, Integer> deaths = new HashMap<>();
    private final Set<UUID> downed = new HashSet<>();
    private final Set<UUID> suppliesTaken = new HashSet<>();
    private final Set<UUID> claimed = new HashSet<>();
    private final Map<UUID, BlockPos> returnPos = new HashMap<>();
    private final RoomLogic[] logics;
    private final boolean[] built;
    /** Hidden hitpoints for creatures with more than 1024 HP, plus their true maximum. */
    private final Map<LivingEntity, int[]> reserve = new WeakHashMap<>();
    /** CoX: points left in each creature's damage pool (5 per hitpoint, OSRS). */
    private final Map<LivingEntity, Integer> coxPools = new WeakHashMap<>();
    /** ToB: damage each player dealt in the current room, for the room's MVP points. */
    private final Map<UUID, Integer> roomDamage = new HashMap<>();
    private final int partySize;
    private UUID leader;
    private int current;
    private boolean begun;
    private boolean finished;
    private boolean closed;
    private RaidRewards.Result result;
    private int presetIndex;
    private Set<Invocation> invocations = EnumSet.noneOf(Invocation.class);
    private int raidLevel;
    private int retriesLeft = -1;
    private RaidMode mode = RaidMode.NORMAL;
    /** ToA paths completed so far (Walk the Path levels the remaining paths). */
    private int pathsCompleted;
    /** ToA: Walk the Path level-ups each path (index in {@link RaidLayouts#TOA_PATHS}) has received. */
    private final int[] pathLevelUps = new int[RaidLayouts.TOA_PATHS.size()];
    private final boolean[] pathDone = new boolean[RaidLayouts.TOA_PATHS.size()];
    private final long startTick;
    private long endTick;
    private long roomStartTick;
    /** Game tick the party passed the start barrier (the ToA time-limit invocations count from here). */
    private long beginTick = -1;

    RaidInstance(int slot, RaidType type, ServerWorld world, List<ServerPlayerEntity> members, BlockPos entrance) {
        this.slot = slot;
        this.type = type;
        this.world = world;
        this.random = new Random(world.getRandom().nextLong() ^ System.nanoTime());
        this.rooms = RaidLayouts.layout(type, random);
        this.logics = new RoomLogic[rooms.size()];
        this.built = new boolean[rooms.size()];
        for (int i = 0; i < rooms.size(); i++) {
            logics[i] = RoomLogic.create(this, rooms.get(i), i);
        }
        for (ServerPlayerEntity p : members) {
            party.add(p.getUuid());
            names.put(p.getUuid(), p.getName().getString());
            points.put(p.getUuid(), 0);
            returnPos.put(p.getUuid(), entrance.up());
        }
        this.partySize = Math.max(1, members.size());
        this.leader = members.get(0).getUuid();
        this.startTick = ActionManager.gameTick();
        if (type == RaidType.TOA) {
            applyPreset(1);
        }
    }

    // ------------------------------------------------------------------ geometry

    public BlockPos center(int room) {
        return RaidBuilder.center(slot, room);
    }

    /** Where the party arrives in a room (just inside its west doorway). */
    public BlockPos spawn(int room) {
        return center(room).add(-RaidBuilder.HALF + 2, 1, 0);
    }

    public BlockPos gallery(int room) {
        return center(room).add(0, RaidBuilder.HEIGHT + 2, 0);
    }

    public boolean contains(Entity e) {
        return e.getWorld() == world && RaidBuilder.slotAt(e.getX(), e.getZ()) == slot;
    }

    public boolean inRoom(Vec3d pos, int room) {
        BlockPos c = center(room);
        return Math.abs(pos.x - (c.getX() + 0.5)) <= RaidBuilder.HALF + 0.5 && Math.abs(pos.z - (c.getZ() + 0.5)) <= RaidBuilder.HALF + 0.5
                && pos.y >= RaidBuilder.FLOOR_Y - 1 && pos.y <= RaidBuilder.FLOOR_Y + RaidBuilder.HEIGHT + 1;
    }

    /** Index of the room containing a position (-1 for corridors and elsewhere). */
    public int roomAt(Vec3d pos) {
        for (int i = 0; i < rooms.size(); i++) {
            if (inRoom(pos, i)) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------ party

    public List<UUID> party() {
        return party;
    }

    public boolean isLeader(ServerPlayerEntity p) {
        return p.getUuid().equals(leader);
    }

    public List<ServerPlayerEntity> players() {
        List<ServerPlayerEntity> list = new ArrayList<>();
        for (UUID u : party) {
            ServerPlayerEntity p = world.getServer().getPlayerManager().getPlayer(u);
            if (p != null) {
                list.add(p);
            }
        }
        return list;
    }

    /** Party members who are alive, not downed and inside the instance. */
    public List<ServerPlayerEntity> activePlayers() {
        List<ServerPlayerEntity> list = new ArrayList<>();
        for (ServerPlayerEntity p : players()) {
            if (p.isAlive() && !downed.contains(p.getUuid()) && contains(p)) {
                list.add(p);
            }
        }
        return list;
    }

    public boolean isDowned(ServerPlayerEntity p) {
        return downed.contains(p.getUuid());
    }

    public int partySize() {
        return partySize;
    }

    public void say(String message, Formatting color) {
        for (ServerPlayerEntity p : players()) {
            p.sendMessage(Text.literal(message).formatted(color), false);
        }
    }

    public void say(String message) {
        say(message, Formatting.WHITE);
    }

    // ------------------------------------------------------------------ scaling and points

    public double hpMultiplier() {
        return switch (type) {
            case COX -> RaidScaling.coxHp(partySize);
            case TOB -> RaidScaling.tobHp(partySize);
            case TOA -> RaidScaling.toaHp(raidLevel, partySize);
        };
    }

    /**
     * Damage multiplier for the raid's creatures: CoX offensive scaling by party size (and Challenge
     * Mode), weaker ToB Entry mode monsters, ToA raid level and path level.
     */
    public double damageMultiplier() {
        return switch (type) {
            case COX -> RaidScaling.coxOffence(partySize, mode == RaidMode.CHALLENGE);
            case TOB -> mode == RaidMode.ENTRY ? 0.5 : 1.0; // approx: entry-mode max hits are about half
            case TOA -> RaidScaling.toaDamage(raidLevel)
                    * (inPath() ? RaidScaling.toaPathMultiplier(pathLevel()) : 1.0);
        };
    }

    public RaidMode mode() {
        return mode;
    }

    /** Sets the difficulty mode (party leader, before the raid begins). */
    public boolean setMode(RaidMode m) {
        if (begun || m == null || !m.availableFor(type)) {
            return false;
        }
        mode = m;
        return true;
    }

    /** Whether the current room belongs to a ToA path (puzzle or path boss). */
    public boolean inPath() {
        RoomDef r = rooms.get(current);
        return type == RaidType.TOA && (r.kind() == RoomDef.Kind.COMBAT || r.kind() == RoomDef.Kind.PUZZLE);
    }

    /** The level of the ToA path being attempted (the Wardens and other rooms count as level 0 paths). */
    public int pathLevel() {
        int path = pathIndex(rooms.get(current).id());
        return path < 0 ? 0 : RaidScaling.toaPathLevel(invocations, pathLevelUps[path]);
    }

    private static final String[] PATH_GODS = {"Scabaras", "Het", "Apmeken", "Crondis"};

    /** Index in {@link RaidLayouts#TOA_PATHS} of the path a room belongs to, or -1. */
    static int pathIndex(String roomId) {
        for (int i = 0; i < RaidLayouts.TOA_PATHS.size(); i++) {
            if (RaidLayouts.TOA_PATHS.get(i).contains(roomId)) {
                return i;
            }
        }
        return -1;
    }

    /** ToA paths completed so far. */
    public int pathsCompleted() {
        return pathsCompleted;
    }

    public int raidLevel() {
        return raidLevel;
    }

    public int points(UUID u) {
        return points.getOrDefault(u, 0);
    }

    public int totalPoints() {
        int t = 0;
        for (int p : points.values()) {
            t += p;
        }
        return t;
    }

    public void addPoints(UUID u, int amount) {
        if (!points.containsKey(u) || amount == 0 || type == RaidType.TOB) {
            return;
        }
        int cap = type == RaidType.COX ? RaidScaling.COX_POINTS_CAP : RaidScaling.TOA_POINTS_CAP;
        points.put(u, Math.max(0, Math.min(cap, points.get(u) + amount)));
    }

    /** The creature id of a raid entity (boss definition id, or entity type path). */
    public static String creatureId(LivingEntity e) {
        if (e instanceof DataBossEntity boss && boss.def() != null) {
            return boss.def().id;
        }
        return Registries.ENTITY_TYPE.getId(e.getType()).getPath();
    }

    /**
     * Points for damage dealt to a creature in this raid (OSRS): CoX draws on the creature's pool of 5
     * points per hitpoint (full value while fresh, decaying as it drains); ToA gives 1 point per damage
     * times the creature's multiplier; ToB counts the damage towards the room's MVP.
     */
    public void onDamage(ServerPlayerEntity player, LivingEntity target, int dealt) {
        if (dealt <= 0) {
            return;
        }
        int room = roomAt(target.getPos());
        if (room >= 0) {
            logics[room].onCreatureHit(player, target, dealt);
        }
        switch (type) {
            case COX -> {
                int max = maxHitpoints(target);
                int pool = coxPools.getOrDefault(target, max * RaidScaling.COX_DAMAGE_POOL_PER_HP);
                int earned = RaidScaling.coxDamagePoints(pool, max, dealt);
                coxPools.put(target, pool - earned);
                addPoints(player.getUuid(), earned);
            }
            case TOA -> {
                boolean p3 = logics[current] instanceof ToaRooms.Wardens w && w.phaseThree();
                addPoints(player.getUuid(), (int) Math.floor(dealt * RaidScaling.toaPointsPerDamage(creatureId(target), p3)));
            }
            case TOB -> roomDamage.merge(player.getUuid(), dealt, Integer::sum);
        }
        topUp(target);
    }

    /** Points a player earned for a CoX puzzle action (kindling, grubs, the tightrope crystal, crabs). */
    public void awardActionPoints(ServerPlayerEntity player, int amount) {
        if (amount > 0) {
            addPoints(player.getUuid(), amount);
        }
    }

    /** Sets a creature's hitpoints, keeping anything above Minecraft's 1024 cap in reserve. */
    public void setHitpoints(LivingEntity e, int total) {
        total = Math.max(1, total);
        EntityAttributeInstance inst = e.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        int shown = Math.min(total, MC_MAX_HEALTH);
        if (inst != null) {
            inst.setBaseValue(shown);
        }
        e.setHealth(shown);
        if (total > shown) {
            reserve.put(e, new int[]{total - shown, total});
        } else {
            reserve.remove(e);
        }
    }

    /** Scales a freshly spawned creature's hitpoints for this raid's party size, mode and raid/path level. */
    public void scale(LivingEntity e) {
        int base = Math.round(e.getMaxHealth());
        String id = creatureId(e);
        if (e instanceof DataBossEntity boss && boss.def() != null) {
            base = boss.def().hitpoints;
        }
        int hp = switch (type) {
            case COX -> id.equals("cox_guardian") ? guardianHp()
                    : RaidScaling.coxScaledHp(id, base, partySize, mode == RaidMode.CHALLENGE);
            case TOB -> RaidScaling.tobScaledHp(id, base, partySize, mode);
            case TOA -> RaidScaling.toaScaledHp(id, base, raidLevel, pathLevel(), partySize, inPath());
        };
        setHitpoints(e, hp);
        coxPools.remove(e);
        scaleLevels(e, id);
    }

    /** CoX Guardian hitpoints from the party's average Mining level (OSRS), +50% in Challenge Mode. */
    private int guardianHp() {
        int total = 0;
        int n = 0;
        for (ServerPlayerEntity p : players()) {
            total += RoomLogic.level(p, com.runecraft.osrs.Skill.MINING);
            n++;
        }
        int hp = RaidScaling.coxGuardianHp(partySize, n == 0 ? 99 : total / n);
        return mode == RaidMode.CHALLENGE ? hp + hp / 2 : hp;
    }

    /** CoX Defence (and defensive Magic) scaling, and ToB Hard mode level changes. */
    private void scaleLevels(LivingEntity e, String id) {
        if (!(e instanceof com.runecraft.entity.OsrsMonsterEntity m)) {
            return;
        }
        if (type == RaidType.COX) {
            boolean cm = mode == RaidMode.CHALLENGE;
            m.overrideStats(s -> withLevels(s, -1, -1, RaidScaling.coxScaledDefence(id, s.defence(), partySize, cm),
                    RaidScaling.coxMagicIsDefensive(id) ? RaidScaling.coxScaledDefence(id, s.magic(), partySize, cm) : -1, -1));
        } else if (type == RaidType.TOB && mode == RaidMode.HARD) {
            int[] l = RaidScaling.tobHardLevels(id);
            if (l != null) {
                m.overrideStats(s -> withLevels(s, l[0], l[1], l[2], l[3], l[4]));
            }
        }
    }

    private static com.runecraft.combat.MonsterStats withLevels(com.runecraft.combat.MonsterStats s, int atk, int str, int def,
                                                               int mag, int rng) {
        return new com.runecraft.combat.MonsterStats(s.name(), s.combatLevel(), s.hitpoints(), atk < 0 ? s.attack() : atk,
                str < 0 ? s.strength() : str, def < 0 ? s.defence() : def, mag < 0 ? s.magic() : mag, rng < 0 ? s.ranged() : rng,
                s.defenceBonus(), s.attackSpeed(), s.style(), s.maxHit(), s.monster(), s.profile(), s.boss());
    }

    /** {@link #damageMultiplier()} for a particular creature (ToB Hard mode max hit changes). */
    public double damageMultiplier(LivingEntity source) {
        double m = damageMultiplier();
        if (type == RaidType.TOB && mode == RaidMode.HARD && source != null) {
            m *= RaidScaling.tobHardMaxHit(creatureId(source));
        }
        return m;
    }

    /** True hitpoints (including reserve). */
    public int hitpoints(LivingEntity e) {
        int[] r = reserve.get(e);
        return (int) Math.ceil(e.getHealth()) + (r == null ? 0 : r[0]);
    }

    public int maxHitpoints(LivingEntity e) {
        int[] r = reserve.get(e);
        return r == null ? Math.round(e.getMaxHealth()) : r[1];
    }

    public double healthFraction(LivingEntity e) {
        return hitpoints(e) / (double) Math.max(1, maxHitpoints(e));
    }

    /** Sets a creature's current true hitpoints (Vanguard resets). */
    public void setCurrentHitpoints(LivingEntity e, int hp) {
        hp = Math.max(1, Math.min(maxHitpoints(e), hp));
        float shown = Math.min(hp, e.getMaxHealth());
        e.setHealth(shown);
        int[] r = reserve.get(e);
        if (r != null) {
            r[0] = hp - Math.round(shown);
        }
    }

    /** Heals a creature (refilling its reserve once its visible health is full). */
    public void heal(LivingEntity e, int amount) {
        int[] r = reserve.get(e);
        float missing = e.getMaxHealth() - e.getHealth();
        int toHealth = (int) Math.min(amount, missing);
        e.heal(toHealth);
        if (r != null) {
            r[0] = Math.min(r[1] - Math.round(e.getMaxHealth()), r[0] + (amount - toHealth));
        }
    }

    /** Moves reserve hitpoints into visible health. */
    public void topUp(LivingEntity e) {
        int[] r = reserve.get(e);
        if (r == null || r[0] <= 0 || !e.isAlive()) {
            return;
        }
        int missing = (int) Math.floor(e.getMaxHealth() - e.getHealth());
        int move = Math.min(missing, r[0]);
        if (move > 0) {
            e.setHealth(e.getHealth() + move);
            r[0] -= move;
        }
    }

    // ------------------------------------------------------------------ ToA invocations

    public int presetIndex() {
        return presetIndex;
    }

    public void applyPreset(int index) {
        List<Invocation.Preset> presets = Invocation.presets();
        presetIndex = Math.floorMod(index, presets.size());
        invocations = EnumSet.noneOf(Invocation.class);
        invocations.addAll(presets.get(presetIndex).invocations());
        raidLevel = Invocation.raidLevel(invocations);
    }

    public void setCustomLevel(int level) {
        raidLevel = Math.max(0, Math.min(600, level / 5 * 5));
    }

    public Set<Invocation> invocations() {
        return invocations;
    }

    public boolean begun() {
        return begun;
    }

    // ------------------------------------------------------------------ flow

    public RoomLogic logic() {
        return logics[current];
    }

    public RoomLogic logic(int i) {
        return logics[i];
    }

    public int currentRoom() {
        return current;
    }

    public boolean finished() {
        return finished;
    }

    /** Deaths of one party member so far (for Combat Achievements). */
    public int deaths(UUID u) {
        return deaths.getOrDefault(u, 0);
    }

    /** Deaths of the whole party so far. */
    public int totalDeaths() {
        int sum = 0;
        for (int d : deaths.values()) {
            sum += d;
        }
        return sum;
    }

    /** Whether anyone took supplies from the storage unit / Helpful Spirit. */
    public boolean suppliesTaken() {
        return !suppliesTaken.isEmpty();
    }

    public long elapsedTicks() {
        return (finished ? endTick : ActionManager.gameTick()) - startTick;
    }

    public long roomTicks() {
        return ActionManager.gameTick() - roomStartTick;
    }

    void ensureBuilt(int i) {
        if (i < 0 || i >= rooms.size() || built[i]) {
            return;
        }
        built[i] = true;
        RaidBuilder.buildRoom(world, slot, i, rooms.get(i).style(), i == 0, i == rooms.size() - 1);
        try {
            logics[i].decorate();
        } catch (RuntimeException e) {
            com.runecraft.RuneCraft.LOGGER.error("Failed to decorate raid room {}", rooms.get(i).id(), e);
        }
    }

    /** Builds the lobby and first room and brings the party in. */
    void open(List<ServerPlayerEntity> members) {
        for (int i = 0; i < Math.min(3, rooms.size()); i++) {
            RaidBuilder.prefetch(world, slot, i);
        }
        ensureBuilt(0);
        ensureBuilt(1);
        RaidBuilder.closeExit(world, slot, 0, RaidBuilder.module(RaidsContent.START_BARRIER));
        current = 0;
        roomStartTick = ActionManager.gameTick();
        logics[0].started = true;
        BlockPos spawn = spawn(0);
        for (ServerPlayerEntity p : members) {
            RaidsState.get(world.getServer()).setLastEntrance(p.getUuid(), returnPos.get(p.getUuid()));
            teleport(p, spawn.add(0, 0, random.nextInt(3) - 1));
            p.sendMessage(Text.literal("You enter the " + type.displayName() + ".").formatted(Formatting.GOLD), false);
            Msg.game(p, rooms.get(0).hint());
        }
        if (type == RaidType.TOA) {
            say("Raid level: " + raidLevel + " (" + Invocation.presets().get(presetIndex).name() + "). Use the obelisk to change it.",
                    Formatting.YELLOW);
        } else {
            say("Mode: " + mode.displayName() + ". The leader can change it with /raids mode "
                    + (type == RaidType.COX ? "<normal|challenge>" : "<entry|normal|hard>") + ".", Formatting.YELLOW);
        }
    }

    public void teleport(ServerPlayerEntity p, BlockPos pos) {
        p.teleport(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, -90f, 0f);
        p.fallDistance = 0;
    }

    /** The start barrier: leaves the lobby and locks in the raid level. */
    public void begin(ServerPlayerEntity player) {
        if (begun) {
            return;
        }
        begun = true;
        RaidBuilder.openExit(world, slot, 0);
        logics[0].done = true;
        beginTick = ActionManager.gameTick();
        if (type == RaidType.TOA) {
            // OSRS: the attempts invocations count wipes over the whole raid, not per room.
            retriesLeft = Invocation.roomRetries(invocations);
            say("The raid begins at raid level " + raidLevel + ".", Formatting.GOLD);
            int limit = Invocation.timeLimitMinutes(invocations);
            if (limit > 0) {
                say("You have " + limit + " minutes to complete the raid, or the raid level drops by "
                        + Invocation.timeLimitPenalty(invocations) + ".", Formatting.YELLOW);
            }
        } else {
            say(player.getName().getString() + " has started the raid (" + mode.displayName() + "). Good luck!", Formatting.GOLD);
        }
        world.playSound(null, center(0), SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.BLOCKS, 1f, 0.6f);
    }

    void gameTick(long tick) {
        if (closed) {
            return;
        }
        for (UUID u : new ArrayList<>(party)) {
            ServerPlayerEntity p = world.getServer().getPlayerManager().getPlayer(u);
            if (p != null && (!contains(p) || p.getWorld() != world)) {
                Msg.game(p, "You have left the " + type.displayName() + ".");
                removePlayer(u, false);
            }
        }
        if (party.isEmpty()) {
            RaidManager.close(this);
            return;
        }
        for (LivingEntity e : new ArrayList<>(reserve.keySet())) {
            topUp(e);
        }
        RoomLogic logic = logics[current];
        if (logic.started && !logic.done) {
            try {
                logic.gameTick(tick);
            } catch (RuntimeException e) {
                com.runecraft.RuneCraft.LOGGER.error("Raid room {} failed", logic.def.id(), e);
                logic.complete();
            }
        }
        if (logics[current].done && current + 1 < rooms.size()) {
            for (ServerPlayerEntity p : activePlayers()) {
                if (inRoom(p.getPos(), current + 1)) {
                    startRoom(current + 1);
                    break;
                }
            }
        }
        RaidNet.sendHud(this);
    }

    /**
     * Self-test hook ({@code com.runecraft.selftest}, via {@link RaidManager#selfTestRun}): ticks the lobby, then
     * starts every room in turn (the reward room last) and runs its logic for up to {@code ticks} game ticks,
     * letting exceptions through. Returns how many rooms were started.
     */
    int selfTestRun(int ticks) {
        long tick = ActionManager.gameTick();
        for (int i = 0; i < ticks && !closed; i++) {
            gameTick(tick + i);
        }
        int ran = 0;
        for (int r = 1; r < rooms.size() && !closed; r++) {
            logics[current].complete();
            startRoom(r);
            ran++;
            RoomLogic logic = logics[r];
            for (int i = 0; i < ticks && !logic.done && !closed; i++) {
                logic.gameTick(tick + i);
            }
        }
        return ran;
    }

    private void startRoom(int i) {
        current = i;
        roomStartTick = ActionManager.gameTick();
        ensureBuilt(i);
        ensureBuilt(i + 1);
        if (i + 2 < rooms.size()) {
            RaidBuilder.prefetch(world, slot, i + 2);
        }
        RoomDef def = rooms.get(i);
        RoomLogic logic = logics[i];
        logic.started = true;
        for (ServerPlayerEntity p : players()) {
            p.sendMessage(Text.literal(def.name()).formatted(Formatting.GOLD, Formatting.BOLD), true);
            Msg.game(p, def.hint());
        }
        logic.start();
        if (def.kind() == RoomDef.Kind.REWARD) {
            completeRaid();
        }
    }

    void onRoomComplete(RoomLogic logic) {
        RoomDef def = logic.def;
        long secs = roomTicks() * 6 / 10;
        if (def.kind() != RoomDef.Kind.LOBBY && def.kind() != RoomDef.Kind.REWARD) {
            say(def.name() + " complete! Duration: " + secs / 60 + ":" + String.format("%02d", secs % 60), Formatting.GREEN);
        }
        for (UUID u : party) {
            if (type == RaidType.TOB) {
                if (!downed.contains(u)) {
                    points.merge(u, RaidScaling.TOB_POINTS_PER_ROOM, Integer::sum);
                }
            } else if (!downed.contains(u)) {
                addPoints(u, def.completionPoints());
            }
        }
        if (type == RaidType.TOB && def.combat()) {
            // OSRS: the room's MVP (most damage dealt) earns bonus points.
            UUID mvp = null;
            for (Map.Entry<UUID, Integer> e : roomDamage.entrySet()) {
                if (party.contains(e.getKey()) && (mvp == null || e.getValue() > roomDamage.get(mvp))) {
                    mvp = e.getKey();
                }
            }
            if (mvp != null) {
                points.merge(mvp, RaidScaling.tobMvpPoints(def.id()), Integer::sum);
                say(name(mvp) + " was the MVP of " + def.name() + ".", Formatting.YELLOW);
            }
        }
        roomDamage.clear();
        RaidBuilder.openExit(world, slot, logic.index);
        world.playSound(null, center(logic.index), SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundCategory.BLOCKS, 1f, 0.8f);
        reviveAll(logic.index);
        if (type == RaidType.TOA) {
            if (def.kind() == RoomDef.Kind.COMBAT) {
                pathsCompleted++;
                int path = pathIndex(def.id());
                if (path >= 0) {
                    pathDone[path] = true;
                }
                if (invocations.contains(Invocation.WALK_THE_PATH)) {
                    // OSRS Walk the Path: the first path levels two random other paths, the next ones one each.
                    List<Integer> remaining = new ArrayList<>();
                    for (int i = 0; i < pathDone.length; i++) {
                        if (!pathDone[i]) {
                            remaining.add(i);
                        }
                    }
                    for (int i : RaidScaling.walkThePathLevelUps(remaining, pathsCompleted, random)) {
                        pathLevelUps[i]++;
                        say("You hear a mysterious rumbling coming from the Path of " + PATH_GODS[i] + ".", Formatting.LIGHT_PURPLE);
                    }
                }
                if (com.runecraft.osrs.content.raids.ToaSupplies.offeredAfter(pathsCompleted)) {
                    // OSRS: the Helpful Spirit offers supplies after the second and the fourth path.
                    ToaSupplyService.offer(this);
                }
            }
        }
    }

    /** Brings downed players back into a room (after it is cleared or retried). */
    private void reviveAll(int room) {
        for (ServerPlayerEntity p : players()) {
            if (downed.remove(p.getUuid())) {
                restore(p);
                teleport(p, center(room).add(RaidBuilder.HALF - 3, 1, random.nextInt(5) - 2));
                Msg.game(p, "You are revived.");
            }
        }
    }

    private void completeRaid() {
        if (finished) {
            return;
        }
        finished = true;
        endTick = ActionManager.gameTick();
        Map<String, Integer> pts = new LinkedHashMap<>();
        for (UUID u : party) {
            pts.put(u.toString(), points(u));
        }
        boolean withinTarget = elapsedTicks() <= RaidScaling.targetTicks(type, mode, partySize);
        int limit = type == RaidType.TOA ? Invocation.timeLimitMinutes(invocations) : 0;
        if (limit > 0 && beginTick >= 0 && endTick - beginTick > limit * 100L) {
            // OSRS: missing a time-limit invocation's limit lowers the raid level (and with it the loot).
            raidLevel = Math.max(0, raidLevel - Invocation.timeLimitPenalty(invocations));
            say("You missed the " + limit + "-minute time limit: the raid level drops to " + raidLevel + ".", Formatting.RED);
        }
        result = RaidRewards.roll(type, mode, pts, raidLevel, withinTarget, random);
        long secs = elapsedTicks() * 6 / 10;
        say("Congratulations - your raid is complete! Duration: " + secs / 60 + ":" + String.format("%02d", secs % 60),
                Formatting.GOLD);
        if (type != RaidType.TOB) {
            say("Total points: " + totalPoints() + ".", Formatting.YELLOW);
        }
        if (!RaidRewards.extras(type, mode).isEmpty()) {
            long target = RaidScaling.targetTicks(type, mode, partySize) * 6 / 10;
            say(withinTarget ? "You beat the " + mode.displayName() + " target time of " + target / 60 + " minutes!"
                    : "You missed the " + mode.displayName() + " target time of " + target / 60 + " minutes.", Formatting.YELLOW);
        }
        RaidsState state = RaidsState.get(world.getServer());
        for (ServerPlayerEntity p : players()) {
            boolean purple = result.purple(p.getUuid().toString());
            int kc = state.addCompletion(p.getUuid(), type.id(), points(p.getUuid()), purple);
            Msg.game(p, "Your completed " + type.displayName() + " count is: " + kc + ".");
            if (type != RaidType.TOB) {
                Msg.game(p, "Personal points: " + points(p.getUuid()) + ".");
            }
        }
        for (String who : result.purpleRecipients()) {
            String name = names.getOrDefault(UUID.fromString(who), "Someone");
            String item = result.lootFor(who).isEmpty() ? "" : result.lootFor(who).get(0).item();
            ItemDef def = ItemCatalog.get(item);
            world.getServer().getPlayerManager().broadcast(Text.literal(name + " received special loot from a raid: "
                    + (def == null ? item : def.name()) + "!").formatted(Formatting.LIGHT_PURPLE), false);
            world.playSound(null, center(current), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1f, 1f);
        }
        if (result.purpleRecipients().isEmpty()) {
            say("The chest contains standard loot.", Formatting.GRAY);
        }
    }

    /** The reward chest: hands over this player's loot once. */
    public void claim(ServerPlayerEntity p) {
        if (!finished || result == null) {
            Msg.game(p, "The chest is sealed until the raid is complete.");
            return;
        }
        if (!claimed.add(p.getUuid())) {
            Msg.game(p, "You have already claimed your reward. Use the exit to leave.");
            return;
        }
        List<RaidRewards.Loot> loot = result.lootFor(p.getUuid().toString());
        if (loot.isEmpty()) {
            Msg.game(p, "The chest is empty for you.");
            return;
        }
        for (RaidRewards.Loot l : loot) {
            Inv.give(p, l.item(), l.count());
            CollectionLog.obtain(p, l.item());
            ItemDef def = ItemCatalog.get(l.item());
            Msg.game(p, "You find: " + l.count() + " x " + (def == null ? l.item() : def.name()) + ".");
        }
        if (result.petRecipients().contains(p.getUuid().toString())) {
            world.getServer().getPlayerManager().broadcast(Text.literal(p.getName().getString()
                    + " has a funny feeling like they're being followed.").formatted(Formatting.GOLD), false);
        }
    }

    public boolean hasClaimed(ServerPlayerEntity p) {
        return claimed.contains(p.getUuid());
    }

    /** Records that a player used Helpful Spirit supplies or honey locusts ("Helpful spirit who?"). */
    public void markSuppliesTaken(UUID u) {
        suppliesTaken.add(u);
    }

    public void takeSupplies(ServerPlayerEntity p) {
        if (type == RaidType.TOA) {
            // OSRS: the tomb has no free supply chest; the Helpful Spirit offers bundles after the second and fourth path.
            Msg.game(p, "The storage unit is for your own items. The Helpful Spirit brings supplies after the second and fourth paths.");
            return;
        }
        if (!suppliesTaken.add(p.getUuid())) {
            Msg.game(p, "You have already taken your share of the supplies.");
            return;
        }
        switch (type) {
            case COX -> {
                Inv.give(p, "saradomin_brew", 3);
                Inv.give(p, "super_restore", 3);
                Inv.give(p, "shark", 6);
            }
            case TOB -> {
                Inv.give(p, "saradomin_brew", 2);
                Inv.give(p, "super_restore", 2);
            }
            case TOA -> {
            }
        }
        Msg.game(p, "You take some supplies from the storage unit.");
    }

    // ------------------------------------------------------------------ deaths

    /** Called instead of a party member dying. */
    void onDeath(ServerPlayerEntity p) {
        UUID u = p.getUuid();
        deaths.merge(u, 1, Integer::sum);
        restore(p);
        RoomDef room = rooms.get(current);
        boolean fighting = logics[current].started && !logics[current].done;
        switch (type) {
            case COX -> {
                int before = points(u);
                points.put(u, RaidScaling.coxDeathPenalty(before));
                say(p.getName().getString() + " has died and lost " + (before - points(u)) + " points.", Formatting.RED);
                teleport(p, spawn(0));
            }
            case TOB -> {
                points.put(u, Math.max(0, points(u) - RaidScaling.TOB_DEATH_PENALTY));
                if (fighting) {
                    down(p, room.name());
                } else {
                    teleport(p, spawn(current));
                }
            }
            case TOA -> {
                int before = points(u);
                points.put(u, RaidScaling.toaDeathPenalty(before));
                say(p.getName().getString() + " has died and lost " + (before - points(u)) + " points.", Formatting.RED);
                if (fighting) {
                    down(p, room.name());
                } else {
                    teleport(p, spawn(current));
                }
            }
        }
        if (fighting && type != RaidType.COX && activePlayers().isEmpty()) {
            wipe();
        }
    }

    private void down(ServerPlayerEntity p, String room) {
        downed.add(p.getUuid());
        teleport(p, gallery(current));
        Msg.red(p, "You have fallen. You will rejoin your party when " + room + " is defeated.");
    }

    private void wipe() {
        if (type == RaidType.TOA && retriesLeft != 0) {
            if (retriesLeft > 0) {
                retriesLeft--;
            }
            say("Your party has fallen! The room resets" + (retriesLeft >= 0 ? " (" + retriesLeft + " attempts left)." : "."),
                    Formatting.RED);
            // OSRS: wiping hands out honey locusts (not with On a Diet).
            ToaSupplyService.wiped(this);
            RoomLogic logic = logics[current];
            logic.reset();
            for (ServerPlayerEntity p : players()) {
                downed.remove(p.getUuid());
                restore(p);
                teleport(p, spawn(current));
            }
            roomStartTick = ActionManager.gameTick();
            logic.started = true;
            logic.start();
            return;
        }
        say("Your party has fallen. The " + type.displayName() + " is lost.", Formatting.DARK_RED);
        for (UUID u : new ArrayList<>(party)) {
            removePlayer(u, true);
        }
        RaidManager.close(this);
    }

    private static void restore(ServerPlayerEntity p) {
        p.setHealth(p.getMaxHealth());
        p.extinguish();
        p.clearStatusEffects();
        PoisonService.cure(p, true, 0, ActionManager.gameTick());
        p.fallDistance = 0;
    }

    // ------------------------------------------------------------------ leaving

    /** Removes a player from the party (optionally sending them back to the entrance). */
    public void removePlayer(UUID u, boolean teleportOut) {
        if (!party.remove(u)) {
            return;
        }
        downed.remove(u);
        RaidManager.unbind(u);
        ToaSupplyService.forget(u);
        ServerPlayerEntity p = world.getServer().getPlayerManager().getPlayer(u);
        if (p != null) {
            for (String item : RaidItems.RAID_ONLY_ITEMS) {
                Inv.remove(p, item, Inv.count(p, item));
            }
            RaidNet.clearHud(p);
            if (teleportOut) {
                restore(p);
                BlockPos back = returnPos.getOrDefault(u, p.getServer().getOverworld().getSpawnPos());
                ServerWorld gielinor = com.runecraft.world.Gielinor.world(p.getServer());
                p.teleport(gielinor == null ? world : gielinor, back.getX() + 0.5, back.getY(), back.getZ() + 0.5, p.getYaw(), 0);
            }
        }
        if (u.equals(leader) && !party.isEmpty()) {
            leader = party.get(0);
        }
    }

    /** Releases everything the raid used. */
    void shutdown() {
        if (closed) {
            return;
        }
        closed = true;
        for (RoomLogic l : logics) {
            try {
                l.cleanup();
            } catch (RuntimeException ignored) {
                // best effort
            }
        }
        RaidBuilder.clearEntities(world, RaidBuilder.slotBox(slot));
        RaidEffects.clear(world, RaidBuilder.slotBox(slot));
        com.runecraft.boss.Mechanics.clearHazards(world, RaidBuilder.slotBox(slot));
        RaidBuilder.unloadSlot(world, slot);
    }

    public String name(UUID u) {
        return names.getOrDefault(u, "?");
    }
}
