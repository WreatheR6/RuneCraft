package com.runecraft.content.raids;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.collection.CollectionLog;
import com.runecraft.combat.PoisonService;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.raids.Invocation;
import com.runecraft.osrs.content.raids.RaidItems;
import com.runecraft.osrs.content.raids.RaidLayouts;
import com.runecraft.osrs.content.raids.RaidMode;
import com.runecraft.osrs.content.raids.RaidRewards;
import com.runecraft.osrs.content.raids.RaidScaling;
import com.runecraft.osrs.content.raids.RaidType;
import com.runecraft.osrs.content.raids.RoomDef;
import com.runecraft.osrs.content.raids.RaidsContent;
import com.runecraft.osrs.content.raids.ToaLoot;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
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
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * One running raid: its party, rooms, points, deaths, raid level and rewards. Rooms are built
 * lazily one ahead of the party; a room starts when a living party member walks into it and the
 * exit barrier opens when its logic reports completion.
 *
 * <p>The Tombs of Amascut works like OSRS: past the lobby lies the Nexus, where the party leader picks each path (the
 * others follow); a path's puzzle room opens onto its boss, whose teleport crystal leads back to the Nexus, and once all
 * four paths are walked the Nexus opens the way down to the Wardens. The raid timer runs from the first path.
 */
public final class RaidInstance {
    /** Completion-count key of CoX Challenge Mode (OSRS counts it apart from normal raids). */
    public static final String COX_CM_KEY = "cox_cm";
    /** Entities' maximum health attribute is clamped at 1024; anything above that is held in reserve. */
    private static final int MC_MAX_HEALTH = 1024;
    /** Game ticks a ToA raid waits for anyone to log back in before it closes. */
    private static final int OFFLINE_GRACE_TICKS = 500;
    private static final String[] PATH_GODS = {"Scabaras", "Het", "Apmeken", "Crondis"};

    public final int slot;
    public final RaidType type;
    public final ServerLevel world;
    /** The rooms in order. Final once the raid begins (CoX re-rolls them in the lobby: a reload or Challenge Mode). */
    public List<RoomDef> rooms;
    public final Random random;
    /** CoX: the layout's floors (where a death sends you back to) and its scouting summary; null for the other raids. */
    private RaidLayouts.CoxPlan coxPlan;
    private final List<UUID> party = new ArrayList<>();
    private final Map<UUID, String> names = new HashMap<>();
    /** CoX/ToB points; ToA total reward points (starting at 5,000). */
    private final Map<UUID, Integer> points = new LinkedHashMap<>();
    /** ToA room points in the current room (they join the total when the room is cleared). */
    private final Map<UUID, Integer> roomPoints = new HashMap<>();
    private final Map<UUID, Integer> deaths = new HashMap<>();
    private final Set<UUID> downed = new HashSet<>();
    private final Set<UUID> suppliesTaken = new HashSet<>();
    private final Set<UUID> claimed = new HashSet<>();
    private final Map<UUID, BlockPos> returnPos = new HashMap<>();
    /** ToA: members who logged out mid-raid (they may log back in), with the raid tick they left. */
    private final Map<UUID, Long> offline = new HashMap<>();
    private RoomLogic[] logics;
    private boolean[] built;
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
    /** ToA: the path being walked (index in {@link RaidLayouts#TOA_PATHS}), or -1 while the party is in the Nexus. */
    private int pathInProgress = -1;
    /** ToA: each path boss's path level when it died (the pet remnants need level 4). */
    private final Map<String, Integer> pathLevelAtKill = new HashMap<>();
    private final long startTick;
    private long endTick;
    private long roomStartTick;
    /** Raid tick the raid timer started: the start barrier (CoX, ToB) or the first path (ToA, OSRS); -1 before. */
    private long beginTick = -1;
    /** ToA: raid tick the final Warden died (the time-limit invocations stop there). */
    private long wardensDownTick = -1;
    /** Raid tick since when no party member has been online (ToA), or -1. */
    private long emptySince = -1;
    /** Attacks that reached a party member (boss attacks and room mechanics), for the self-test. */
    private int attacksOnParty;
    /** Room ticks that threw (the room was then passed as cleared); the self-test fails on any. */
    private int roomFailures;
    private String lastRoomFailure;

    RaidInstance(int slot, RaidType type, ServerLevel world, List<ServerPlayer> members, BlockPos entrance) {
        this.slot = slot;
        this.type = type;
        this.world = world;
        this.random = new Random(world.getRandom().nextLong() ^ System.nanoTime());
        if (type == RaidType.COX) {
            this.coxPlan = RaidLayouts.coxPlan(random);
            this.rooms = coxPlan.rooms();
        } else {
            this.rooms = RaidLayouts.layout(type, random);
        }
        this.logics = new RoomLogic[rooms.size()];
        this.built = new boolean[rooms.size()];
        for (int i = 0; i < rooms.size(); i++) {
            logics[i] = RoomLogic.create(this, rooms.get(i), i);
        }
        for (ServerPlayer p : members) {
            party.add(p.getUUID());
            names.put(p.getUUID(), p.getName().getString());
            points.put(p.getUUID(), type == RaidType.TOA ? ToaLoot.START_POINTS : 0);
            returnPos.put(p.getUUID(), entrance.above());
        }
        this.partySize = Math.max(1, members.size());
        this.leader = members.get(0).getUUID();
        this.startTick = now();
        if (type == RaidType.TOA) {
            applyPreset(1);
        }
    }

    /** The raid clock (game ticks; see {@link RaidEffects#now}). */
    public static long now() {
        return RaidEffects.now();
    }

    // ------------------------------------------------------------------ geometry

    public BlockPos center(int room) {
        return RaidBuilder.center(slot, room);
    }

    /** Where the party arrives in a room (just inside its west doorway). */
    public BlockPos spawn(int room) {
        return center(room).offset(-RaidBuilder.HALF + 2, 1, 0);
    }

    public BlockPos gallery(int room) {
        return center(room).offset(0, RaidBuilder.HEIGHT + 2, 0);
    }

    public boolean contains(Entity e) {
        return e.level() == world && RaidBuilder.slotAt(e.getX(), e.getZ()) == slot;
    }

    public boolean inRoom(Vec3 pos, int room) {
        BlockPos c = center(room);
        return Math.abs(pos.x - (c.getX() + 0.5)) <= RaidBuilder.HALF + 0.5 && Math.abs(pos.z - (c.getZ() + 0.5)) <= RaidBuilder.HALF + 0.5
                && pos.y >= RaidBuilder.FLOOR_Y - 1 && pos.y <= RaidBuilder.FLOOR_Y + RaidBuilder.HEIGHT + 1;
    }

    /** Index of the room containing a position (-1 for corridors and elsewhere). */
    public int roomAt(Vec3 pos) {
        for (int i = 0; i < rooms.size(); i++) {
            if (inRoom(pos, i)) {
                return i;
            }
        }
        return -1;
    }

    /** Index of the room with this id, or -1. */
    public int roomIndex(String id) {
        for (int i = 0; i < rooms.size(); i++) {
            if (rooms.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------ party

    public List<UUID> party() {
        return party;
    }

    public boolean isLeader(ServerPlayer p) {
        return p.getUUID().equals(leader);
    }

    public List<ServerPlayer> players() {
        List<ServerPlayer> list = new ArrayList<>();
        for (UUID u : party) {
            ServerPlayer p = world.getServer().getPlayerList().getPlayer(u);
            if (p != null && !offline.containsKey(u)) {
                list.add(p);
            }
        }
        return list;
    }

    /** Party members who are alive, not downed and inside the instance. */
    public List<ServerPlayer> activePlayers() {
        List<ServerPlayer> list = new ArrayList<>();
        for (ServerPlayer p : players()) {
            if (p.isAlive() && !downed.contains(p.getUUID()) && contains(p)) {
                list.add(p);
            }
        }
        return list;
    }

    public boolean isDowned(ServerPlayer p) {
        return downed.contains(p.getUUID());
    }

    public int partySize() {
        return partySize;
    }

    public void say(String message, ChatFormatting color) {
        for (ServerPlayer p : players()) {
            p.displayClientMessage(Component.literal(message).withStyle(color), false);
        }
    }

    public void say(String message) {
        say(message, ChatFormatting.WHITE);
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

    /** Sets the difficulty mode (party leader, before the raid begins). CoX Challenge Mode has its own fixed layout. */
    public boolean setMode(RaidMode m) {
        if (begun || m == null || !m.availableFor(type)) {
            return false;
        }
        boolean changed = mode != m;
        mode = m;
        if (changed && type == RaidType.COX) {
            relayout(m == RaidMode.CHALLENGE ? RaidLayouts.coxChallengePlan(random) : RaidLayouts.coxPlan(random));
        }
        return true;
    }

    /** CoX: the scouted layout, floor by floor (null for the other raids). */
    public String scout() {
        return coxPlan == null ? null : coxPlan.scout();
    }

    /** CoX: the first room of the floor room {@code room} is on (the respawn point after a death). */
    public int floorStart(int room) {
        return coxPlan == null ? 0 : coxPlan.floorStart(room);
    }

    /**
     * CoX "reload" (OSRS: the stairs in the lobby re-roll the layout before the raid starts; Challenge Mode's layout is
     * fixed). Returns false once the raid has begun or in Challenge Mode.
     */
    public boolean reloadLayout() {
        if (type != RaidType.COX || begun || mode == RaidMode.CHALLENGE) {
            return false;
        }
        relayout(RaidLayouts.coxPlan(random));
        return true;
    }

    /** Swaps in a new CoX room sequence while the party waits in the lobby, rebuilding the first room. */
    private void relayout(RaidLayouts.CoxPlan plan) {
        if (begun || plan.rooms().size() > RaidLayouts.MAX_ROOMS) {
            return;
        }
        for (int i = 1; i < logics.length; i++) {
            logics[i].cleanup();
        }
        RoomLogic lobby = logics[0];
        coxPlan = plan;
        rooms = plan.rooms();
        logics = new RoomLogic[rooms.size()];
        built = new boolean[rooms.size()];
        logics[0] = lobby;
        built[0] = true;
        for (int i = 1; i < rooms.size(); i++) {
            logics[i] = RoomLogic.create(this, rooms.get(i), i);
        }
        for (int i = 1; i < Math.min(3, rooms.size()); i++) {
            RaidBuilder.prefetch(world, slot, i);
        }
        ensureBuilt(1);
        say("Layout: " + plan.scout(), ChatFormatting.AQUA);
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

    /** The level of ToA path {@code path} (index in {@link RaidLayouts#TOA_PATHS}), e.g. for the Wardens' phantoms. */
    public int pathLevelOf(int path) {
        return path < 0 || path >= pathLevelUps.length ? 0 : RaidScaling.toaPathLevel(invocations, pathLevelUps[path]);
    }

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

    /** Whether ToA path {@code path} (index in {@link RaidLayouts#TOA_PATHS}) is done. */
    public boolean pathDone(int path) {
        return path >= 0 && path < pathDone.length && pathDone[path];
    }

    /** The ToA path being walked, or -1. */
    public int pathInProgress() {
        return pathInProgress;
    }

    public int raidLevel() {
        return raidLevel;
    }

    /**
     * A player's points: CoX/ToB points; ToA reward points (the total minus the starting 5,000) plus the room points of
     * the room in progress.
     */
    public int points(UUID u) {
        if (type == RaidType.TOA) {
            return ToaLoot.rewardPoints(points.getOrDefault(u, ToaLoot.START_POINTS)) + roomPoints.getOrDefault(u, 0);
        }
        return points.getOrDefault(u, 0);
    }

    /** ToA total reward points (including the starting 5,000; room points in progress not yet added). */
    int toaTotal(UUID u) {
        return points.getOrDefault(u, ToaLoot.START_POINTS);
    }

    public int totalPoints() {
        int t = 0;
        for (UUID u : party) {
            t += points(u);
        }
        return t;
    }

    public void addPoints(UUID u, int amount) {
        if (!points.containsKey(u) || amount == 0 || type == RaidType.TOB) {
            return;
        }
        if (type == RaidType.TOA) {
            // OSRS: damage earns room points (at most 20,000 a room); they join the total when the room is cleared.
            roomPoints.put(u, Math.max(0, Math.min(ToaLoot.ROOM_POINTS_CAP, roomPoints.getOrDefault(u, 0) + amount)));
            return;
        }
        // OSRS: the 131,071 cap only limits the common loot; points past it still count towards the uniques.
        points.put(u, Math.max(0, points.get(u) + amount));
    }

    /** The creature id of a raid entity (boss definition id, or entity type path). */
    public static String creatureId(LivingEntity e) {
        if (e instanceof DataBossEntity boss && boss.def() != null) {
            return boss.def().id;
        }
        return BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
    }

    /**
     * Points for damage dealt to a creature in this raid (OSRS): CoX draws on the creature's pool of 5
     * points per hitpoint (full value while fresh, decaying as it drains); ToA gives room points per damage
     * times the creature's multiplier; ToB counts the damage towards the room's MVP.
     */
    public void onDamage(ServerPlayer player, LivingEntity target, int dealt) {
        if (dealt <= 0) {
            return;
        }
        int room = roomAt(target.position());
        if (room >= 0) {
            logics[room].onCreatureHit(player, target, dealt);
        }
        switch (type) {
            case COX -> {
                int max = maxHitpoints(target);
                int pool = coxPools.getOrDefault(target, max * RaidScaling.COX_DAMAGE_POOL_PER_HP);
                int earned = RaidScaling.coxDamagePoints(pool, max, dealt);
                coxPools.put(target, pool - earned);
                addPoints(player.getUUID(), earned);
            }
            case TOA -> {
                boolean p3 = logics[current] instanceof ToaWardens w && w.phaseThree();
                addPoints(player.getUUID(), (int) Math.floor(dealt * RaidScaling.toaPointsPerDamage(creatureId(target), p3)));
            }
            case TOB -> roomDamage.merge(player.getUUID(), dealt, Integer::sum);
        }
        topUp(target);
    }

    /** Points a player earned for a puzzle action (CoX kindling, grubs, crystals; ToA puzzles, repairs, watering). */
    /** CoX: damage to this creature earns no more points (OSRS: an Olm hand that recovered in the penultimate phase). */
    public void emptyPointsPool(LivingEntity e) {
        coxPools.put(e, 0);
    }

    public void awardActionPoints(ServerPlayer player, int amount) {
        if (amount > 0) {
            addPoints(player.getUUID(), amount);
        }
    }

    /** A boss attack or room mechanic reached a party member (counted for the self-test). */
    void noteAttack() {
        attacksOnParty++;
    }

    int attacksOnParty() {
        return attacksOnParty;
    }

    /** Room ticks that threw so far (self-test). */
    int roomFailures() {
        return roomFailures;
    }

    /** The last room tick that threw: room id and exception, or null (self-test). */
    String lastRoomFailure() {
        return lastRoomFailure;
    }

    /** Sets a creature's hitpoints, keeping anything above Minecraft's 1024 cap in reserve. */
    public void setHitpoints(LivingEntity e, int total) {
        total = Math.max(1, total);
        AttributeInstance inst = e.getAttribute(Attributes.MAX_HEALTH);
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
        // OSRS hits never knock anything back. Bosses the rooms drive without AI (Kephri, Zebak, the Wardens) aren't held
        // in place by their anchors, so player hits would slowly push them off their spots.
        AttributeInstance knockback = e.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockback != null) {
            knockback.setBaseValue(1.0);
        }
    }

    /** CoX Guardian hitpoints from the party's average Mining level (OSRS), +50% in Challenge Mode. */
    private int guardianHp() {
        int total = 0;
        int n = 0;
        for (ServerPlayer p : players()) {
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

    /** Raises a creature's Defence level (the enraged phase 3 Warden: +30, OSRS). */
    void raiseDefence(LivingEntity e, int levels) {
        if (e instanceof com.runecraft.entity.OsrsMonsterEntity m) {
            m.overrideStats(s -> withLevels(s, -1, -1, s.defence() + levels, -1, -1));
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

    /** The preset the invocations match, or -1 for a custom selection. */
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

    /**
     * Turns one invocation on or off (OSRS invocation interface): turning one on replaces any other of its group, and an
     * invocation that needs another (Arterial Spray, Overclocked 2...) can only be turned on after it; turning one off
     * also turns off the invocations that need it. Returns a message for the player, or null on success.
     */
    public String toggleInvocation(Invocation i) {
        if (begun) {
            return "The invocations are locked in: raid level " + raidLevel + ".";
        }
        if (invocations.contains(i)) {
            invocations.remove(i);
            boolean changed = true;
            while (changed) {
                changed = invocations.removeIf(x -> x.requires() != null && !invocations.contains(x.requires()));
            }
        } else {
            if (i.requires() != null && !invocations.contains(i.requires())) {
                return i.displayName() + " needs " + i.requires().displayName() + " first.";
            }
            if (i.group() != null) {
                invocations.removeIf(x -> i.group().equals(x.group()));
            }
            invocations.add(i);
        }
        raidLevel = Invocation.raidLevel(invocations);
        presetIndex = -1;
        for (int p = 0; p < Invocation.presets().size(); p++) {
            if (Invocation.presets().get(p).invocations().equals(invocations)) {
                presetIndex = p;
            }
        }
        return null;
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

    /** The raid timer: from the start barrier (CoX: when the leader starts the raid) or the first path (ToA) until the end. */
    public long elapsedTicks() {
        long from = type == RaidType.TOB ? startTick : beginTick;
        if (from < 0) {
            return 0;
        }
        return (finished ? endTick : now()) - from;
    }

    public long roomTicks() {
        return now() - roomStartTick;
    }

    /** Wipes the party can still recover from (-1: unlimited). */
    public int retriesLeft() {
        return retriesLeft;
    }

    void ensureBuilt(int i) {
        if (i < 0 || i >= rooms.size() || built[i]) {
            return;
        }
        built[i] = true;
        // Rooms reached by teleport (the ToA paths and the Wardens) keep their west wall; a room only gets an exit corridor
        // when the next room is entered through it.
        boolean west = i > 0 && logics[i].entersFromWest() && logics[i - 1].exitsEast();
        boolean east = i < rooms.size() - 1 && logics[i].exitsEast();
        RaidBuilder.buildRoomShell(world, slot, i, rooms.get(i).style(), west, east);
        try {
            logics[i].decorate();
        } catch (RuntimeException e) {
            com.runecraft.RuneCraft.LOGGER.error("Failed to decorate raid room {}", rooms.get(i).id(), e);
        }
    }

    /** Builds the lobby and first room and brings the party in. */
    void open(List<ServerPlayer> members) {
        for (int i = 0; i < Math.min(3, rooms.size()); i++) {
            RaidBuilder.prefetch(world, slot, i);
        }
        ensureBuilt(0);
        ensureBuilt(1);
        RaidBuilder.closeExit(world, slot, 0, RaidBuilder.module(RaidsContent.START_BARRIER));
        current = 0;
        roomStartTick = now();
        logics[0].started = true;
        BlockPos spawn = spawn(0);
        for (ServerPlayer p : members) {
            RaidsState.get(world.getServer()).setLastEntrance(p.getUUID(), returnPos.get(p.getUUID()));
            teleport(p, spawn.offset(0, 0, random.nextInt(3) - 1));
            p.displayClientMessage(Component.literal("You enter the " + type.displayName() + ".").withStyle(ChatFormatting.GOLD), false);
            Msg.game(p, rooms.get(0).hint());
        }
        if (type == RaidType.TOA) {
            say("Raid level: " + raidLevel + " (" + Invocation.presets().get(presetIndex).name() + "). Use the obelisk to choose "
                    + "the invocations.", ChatFormatting.YELLOW);
        } else if (type == RaidType.COX) {
            // OSRS: the party scouts the layout before starting; the leader can reload it (/raids reload) or pick Challenge
            // Mode, whose layout is fixed.
            say("Layout: " + coxPlan.scout(), ChatFormatting.AQUA);
            say("Mode: " + mode.displayName() + ". The leader can change it with /raids mode <normal|challenge>, or re-roll the "
                    + "layout with /raids reload.", ChatFormatting.YELLOW);
        } else {
            say("Mode: " + mode.displayName() + ". The leader can change it with /raids mode "
                    + (type == RaidType.COX ? "<normal|challenge>" : "<entry|normal|hard>") + ".", ChatFormatting.YELLOW);
        }
    }

    public void teleport(ServerPlayer p, BlockPos pos) {
        p.teleportTo(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, -90f, 0f);
        p.fallDistance = 0;
    }

    /** The start barrier: leaves the lobby and locks in the raid level. */
    public void begin(ServerPlayer player) {
        if (begun) {
            return;
        }
        if (type == RaidType.TOA && !isLeader(player)) {
            Msg.game(player, "Your party leader must lead the way into the tomb.");
            return;
        }
        begun = true;
        RaidBuilder.openExit(world, slot, 0);
        logics[0].done = true;
        if (type == RaidType.TOA) {
            // OSRS: the attempts invocations count wipes over the whole raid, not per room.
            retriesLeft = Invocation.roomRetries(invocations);
            say("The invocations are locked in: raid level " + raidLevel + ".", ChatFormatting.GOLD);
            int limit = Invocation.timeLimitMinutes(invocations);
            if (limit > 0) {
                say("You have " + limit + " minutes from the first path to the Wardens' defeat, or the raid level drops by "
                        + Invocation.timeLimitPenalty(invocations) + ".", ChatFormatting.YELLOW);
            }
            // Generate the paths' chunks in the background while the party picks its first path.
            for (int i = 2; i < rooms.size(); i++) {
                RaidBuilder.prefetch(world, slot, i);
            }
        } else {
            beginTick = now();
            say(player.getName().getString() + " has started the raid (" + mode.displayName() + "). Good luck!", ChatFormatting.GOLD);
        }
        world.playSound(null, center(0), SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1f, 0.6f);
    }

    void gameTick(long tick) {
        if (closed) {
            return;
        }
        for (UUID u : new ArrayList<>(party)) {
            ServerPlayer p = world.getServer().getPlayerList().getPlayer(u);
            if (p != null && !offline.containsKey(u) && (!contains(p) || p.level() != world)) {
                Msg.game(p, "You have left the " + type.displayName() + ".");
                removePlayer(u, false);
            }
        }
        if (party.isEmpty()) {
            RaidManager.close(this);
            return;
        }
        if (players().isEmpty()) {
            // ToA: everyone logged out; the raid waits a while for someone to come back.
            if (emptySince < 0) {
                emptySince = now();
            } else if (now() - emptySince > OFFLINE_GRACE_TICKS) {
                RaidManager.close(this);
                return;
            }
        } else {
            emptySince = -1;
        }
        for (LivingEntity e : new ArrayList<>(reserve.keySet())) {
            topUp(e);
        }
        RoomLogic logic = logics[current];
        if (logic.started && !logic.done && type != RaidType.COX && !downed.isEmpty() && !players().isEmpty()
                && activePlayers().isEmpty()) {
            // Everyone still here is down (the others left or logged out): the party has wiped.
            wipe();
            if (closed) {
                return;
            }
        }
        if (logic.started && !logic.done) {
            try {
                logic.gameTick(tick);
            } catch (RuntimeException e) {
                roomFailures++;
                lastRoomFailure = logic.def.id() + ": " + e;
                com.runecraft.RuneCraft.LOGGER.error("Raid room {} failed", logic.def.id(), e);
                logic.complete();
            }
        }
        if (type == RaidType.COX) {
            CoxRooms.regenerateStats(logics[current].spawned, tick); // CoX creatures' own stat regeneration (Mod Ash)
        }
        if (logics[current].done && current + 1 < rooms.size() && logics[current].exitsEast()) {
            for (ServerPlayer p : activePlayers()) {
                if (inRoom(p.position(), current + 1)) {
                    startRoom(current + 1);
                    break;
                }
            }
        }
        // Bosses the room drives without AI (Kephri, Zebak, the Wardens) don't update their own boss bar, and their AI
        // tick doesn't hold them on their anchor against players walking into them.
        for (LivingEntity e : logics[current].spawned) {
            if (e instanceof DataBossEntity b && b.isNoAi() && b.isAlive()) {
                b.refreshBossBar();
                logics[current].keepAnchored(b);
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
        long tick = now();
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

    void startRoom(int i) {
        current = i;
        roomStartTick = now();
        ensureBuilt(i);
        if (logics[i].exitsEast()) {
            ensureBuilt(i + 1);
            if (i + 2 < rooms.size()) {
                RaidBuilder.prefetch(world, slot, i + 2);
            }
        }
        RoomDef def = rooms.get(i);
        RoomLogic logic = logics[i];
        logic.started = true;
        roomPoints.clear();
        for (ServerPlayer p : players()) {
            p.displayClientMessage(Component.literal(def.name()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
            Msg.game(p, def.hint());
            if (type == RaidType.TOA && def.combat()) {
                // OSRS: entering a boss chamber restores run energy.
                com.runecraft.content.systems.RunEnergyService.restore(p, 10_000);
            }
        }
        logic.start();
        if (def.kind() == RoomDef.Kind.REWARD) {
            completeRaid();
        }
    }

    /** Makes an already started room the current one (the party came back to the ToA Nexus). */
    private void moveTo(int i) {
        if (i >= 0 && i != current && logics[i].started) {
            current = i;
        }
    }

    // ------------------------------------------------------------------ the Tombs of Amascut's Nexus

    /** The Nexus room logic, or null outside the Tombs of Amascut. */
    ToaNexus nexus() {
        int i = roomIndex("toa_nexus");
        return i >= 0 && logics[i] instanceof ToaNexus n ? n : null;
    }

    /** One of the Nexus's entrances was used: a path, or the way down to the Wardens. */
    void useNexusEntrance(ServerPlayer p, BlockPos pos) {
        ToaNexus n = nexus();
        int i = n == null ? -1 : n.entranceAt(pos);
        if (i < 0) {
            Msg.game(p, "Nothing interesting happens.");
        } else if (i == ToaNexus.ENTRANCES.length - 1) {
            enterWardens(p);
        } else {
            enterPath(p, i);
        }
    }

    /**
     * A path entrance in the Nexus (OSRS): the party leader starts a path and the others follow; while a path is being
     * walked the other entrances are blocked, and a finished path is closed for good.
     */
    void enterPath(ServerPlayer p, int path) {
        if (type != RaidType.TOA || path < 0 || path >= RaidLayouts.TOA_PATHS.size()) {
            return;
        }
        String god = PATH_GODS[path];
        if (pathDone[path]) {
            Msg.game(p, "The Path of " + god + " has been completed.");
            return;
        }
        if (pathInProgress >= 0 && pathInProgress != path) {
            Msg.game(p, "Another path has been started.");
            return;
        }
        int puzzle = roomIndex(RaidLayouts.TOA_PATHS.get(path).get(0));
        if (pathInProgress < 0) {
            if (!isLeader(p)) {
                Msg.game(p, "Your party leader must enter a path first.");
                return;
            }
            pathInProgress = path;
            if (beginTick < 0) {
                // OSRS: the raid timer (and the time-limit invocations) start with the first path.
                beginTick = now();
            }
            startRoom(puzzle);
            say(p.getName().getString() + " walks the Path of " + god + ".", ChatFormatting.YELLOW);
        }
        int target = logics[puzzle].done ? puzzle + 1 : puzzle;
        teleport(p, spawn(target));
        if (target == current && logics[target].started) {
            Msg.game(p, rooms.get(target).hint());
        }
    }

    /** The way down to the Wardens, open once all four paths are walked. */
    void enterWardens(ServerPlayer p) {
        if (type != RaidType.TOA) {
            return;
        }
        if (pathsCompleted < RaidLayouts.TOA_PATHS.size()) {
            Msg.game(p, "The way below is sealed until all four paths have been walked (" + pathsCompleted + "/4).");
            return;
        }
        int wardens = roomIndex("wardens");
        if (!logics[wardens].started) {
            if (!isLeader(p)) {
                Msg.game(p, "Your party leader must go down first.");
                return;
            }
            startRoom(wardens);
        }
        teleport(p, spawn(wardens));
    }

    /** A path boss's teleport crystal: back to the Nexus. */
    void returnToNexus(ServerPlayer p) {
        int nexus = roomIndex("toa_nexus");
        if (nexus < 0) {
            return;
        }
        if (logics[current].done) {
            moveTo(nexus);
        }
        teleport(p, spawn(nexus).offset(2, 0, 0));
        Msg.game(p, pathsCompleted >= RaidLayouts.TOA_PATHS.size() ? "All four paths are walked: the way to the Wardens is open."
                : "You return to the Nexus. Paths walked: " + pathsCompleted + "/4.");
    }

    void onRoomComplete(RoomLogic logic) {
        RoomDef def = logic.def;
        long secs = roomTicks() * 6 / 10;
        boolean scored = def.kind() != RoomDef.Kind.LOBBY && def.kind() != RoomDef.Kind.REWARD && def.kind() != RoomDef.Kind.RESOURCE
                && def.kind() != RoomDef.Kind.NEXUS;
        if (scored) {
            say(def.name() + " complete! Duration: " + secs / 60 + ":" + String.format("%02d", secs % 60), ChatFormatting.GREEN);
        }
        if (type == RaidType.TOA) {
            toaRoomPoints(def, scored);
        } else {
            for (UUID u : party) {
                if (type == RaidType.TOB) {
                    if (!downed.contains(u)) {
                        points.merge(u, RaidScaling.TOB_POINTS_PER_ROOM, Integer::sum);
                    }
                } else if (!downed.contains(u)) {
                    addPoints(u, def.completionPoints());
                }
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
                say(name(mvp) + " was the MVP of " + def.name() + ".", ChatFormatting.YELLOW);
            }
        }
        roomDamage.clear();
        if (logic.exitsEast()) {
            RaidBuilder.openExit(world, slot, logic.index);
            world.playSound(null, center(logic.index), SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1f, 0.8f);
        }
        reviveAll(logic.index);
        if (type == RaidType.TOA) {
            toaRoomCleared(logic, def);
        }
    }

    /**
     * ToA room points (OSRS): everyone's room points join their total, and the player with the most room points (the
     * room's MVP) adds 300 x team size.
     */
    private void toaRoomPoints(RoomDef def, boolean scored) {
        UUID mvp = null;
        for (UUID u : party) {
            int rp = roomPoints.getOrDefault(u, 0);
            if (rp > 0 && (mvp == null || rp > roomPoints.getOrDefault(mvp, 0))) {
                mvp = u;
            }
            points.put(u, ToaLoot.addRoom(toaTotal(u), rp));
        }
        roomPoints.clear();
        if (scored && mvp != null) {
            points.put(mvp, Math.min(ToaLoot.TOTAL_POINTS_CAP, toaTotal(mvp) + ToaLoot.mvpBonus(partySize)));
            if (partySize > 1) {
                say(name(mvp) + " was the MVP of " + def.name() + ".", ChatFormatting.YELLOW);
            }
        }
    }

    /** ToA after a room: poison cured; a path boss ends its path (restore, teleport crystal, Walk the Path, supplies). */
    private void toaRoomCleared(RoomLogic logic, RoomDef def) {
        for (ServerPlayer p : players()) {
            // OSRS: clearing any challenge cures poison and venom; a boss fully restores the party.
            PoisonService.cure(p, true, 0, ActionManager.gameTick());
            if (def.combat()) {
                restoreAfterBoss(p);
            }
        }
        if (def.kind() == RoomDef.Kind.FINAL) {
            wardensDownTick = now();
        }
        if (def.kind() != RoomDef.Kind.COMBAT) {
            return;
        }
        pathsCompleted++;
        int path = pathIndex(def.id());
        if (path >= 0) {
            pathDone[path] = true;
            pathLevelAtKill.put(def.bosses().isEmpty() ? def.id() : def.bosses().get(0), RaidScaling.toaPathLevel(invocations,
                    pathLevelUps[path]));
        }
        pathInProgress = -1;
        logic.placeExitCrystal();
        ToaNexus n = nexus();
        if (n != null) {
            n.refresh();
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
                say("You hear a mysterious rumbling coming from the Path of " + PATH_GODS[i] + ".", ChatFormatting.LIGHT_PURPLE);
            }
        }
        say("A teleport crystal appears. It leads back to the Nexus.", ChatFormatting.AQUA);
        if (com.runecraft.osrs.content.raids.ToaSupplies.offeredAfter(pathsCompleted)) {
            // OSRS: the Helpful Spirit offers supplies after the second and the fourth path.
            ToaSupplyService.offer(this);
        }
    }

    /** OSRS: clearing a ToA boss restores hitpoints, prayer, run energy and special attack, and undoes stat drains. */
    private static void restoreAfterBoss(ServerPlayer p) {
        var data = com.runecraft.player.Players.data(p);
        for (Skill s : new Skill[]{Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC}) {
            if (data.skills().boost(s) < 0) {
                data.skills().setBoost(s, 0);
            }
        }
        if (data.prayerPoints() < data.skills().level(Skill.PRAYER)) {
            data.setPrayerPoints(data.skills().level(Skill.PRAYER));
        }
        p.setHealth(Math.max(p.getHealth(), p.getMaxHealth()));
        com.runecraft.content.systems.RunEnergyService.restore(p, 10_000);
        com.runecraft.content.systems.SpecialAttackService.restoreEnergy(p, com.runecraft.osrs.content.systems.SpecialAttacks.MAX_ENERGY);
        com.runecraft.network.ServerNetworking.sync(p);
    }

    /** Brings downed players back into a room (after it is cleared or retried). */
    private void reviveAll(int room) {
        for (ServerPlayer p : players()) {
            if (downed.remove(p.getUUID())) {
                restore(p);
                teleport(p, center(room).offset(RaidBuilder.HALF - 3, 1, random.nextInt(5) - 2));
                Msg.game(p, "You are revived.");
            }
        }
    }

    private void completeRaid() {
        if (finished) {
            return;
        }
        finished = true;
        endTick = now();
        boolean withinTarget = elapsedTicks() <= RaidScaling.targetTicks(type, mode, partySize);
        // The mode the raid was played at (before any time-limit penalty).
        int modeLevel = raidLevel;
        int limit = type == RaidType.TOA ? Invocation.timeLimitMinutes(invocations) : 0;
        long stop = wardensDownTick >= 0 ? wardensDownTick : endTick;
        if (limit > 0 && beginTick >= 0 && stop - beginTick > limit * 100L) {
            // OSRS: missing a time-limit invocation's limit lowers the raid level (and with it the loot).
            raidLevel = Math.max(0, raidLevel - Invocation.timeLimitPenalty(invocations));
            say("You missed the " + limit + "-minute time limit: the raid level drops to " + raidLevel + ".", ChatFormatting.RED);
        }
        if (type == RaidType.COX && mode == RaidMode.CHALLENGE && withinTarget) {
            // OSRS: a Challenge Mode raid within the target time adds 5,000 points per player to the team.
            for (UUID u : party) {
                addPoints(u, RaidScaling.COX_CM_TARGET_BONUS);
            }
            say("Challenge Mode within the target time: +" + RaidScaling.COX_CM_TARGET_BONUS + " points each.", ChatFormatting.AQUA);
        }
        Map<String, Integer> pts = new LinkedHashMap<>();
        // ToA (OSRS): raiders who left before the end still count towards the team's unique chance (and can draw the
        // unique, which then goes to no one); the others only roll for whoever is still here.
        // CoX (OSRS, January 2025): the same, so leaving no longer hands your loot chance to the others.
        for (UUID u : type == RaidType.TOB ? party : points.keySet()) {
            pts.put(u.toString(), type == RaidType.TOA ? ToaLoot.rewardPoints(toaTotal(u)) : points(u));
        }
        if (type == RaidType.TOA) {
            Map<String, ToaLoot.Raider> raiders = new HashMap<>();
            for (ServerPlayer p : players()) {
                raiders.put(p.getUUID().toString(), raider(p));
            }
            result = ToaLoot.roll(pts, raidLevel, raiders, totalDeaths() == 0, invocations, pathLevelAtKill, random);
        } else {
            // Only the raiders still here can receive the extras (tablet, colour kit, dust).
            Set<String> present = new HashSet<>();
            for (UUID u : party) {
                present.add(u.toString());
            }
            result = RaidRewards.roll(type, mode, pts, raidLevel, withinTarget, present, random);
        }
        long secs = elapsedTicks() * 6 / 10;
        say("Congratulations - your raid is complete! Duration: " + secs / 60 + ":" + String.format("%02d", secs % 60),
                ChatFormatting.GOLD);
        if (type != RaidType.TOB) {
            say("Total points: " + totalPoints() + ".", ChatFormatting.YELLOW);
        }
        if (!RaidRewards.extras(type, mode).isEmpty()) {
            long target = RaidScaling.targetTicks(type, mode, partySize) * 6 / 10;
            say(withinTarget ? "You beat the " + mode.displayName() + " target time of " + target / 60 + " minutes!"
                    : "You missed the " + mode.displayName() + " target time of " + target / 60 + " minutes.", ChatFormatting.YELLOW);
        }
        RaidsState state = RaidsState.get(world.getServer());
        for (ServerPlayer p : players()) {
            boolean purple = result.purple(p.getUUID().toString());
            // OSRS keeps the Challenge Mode completion count apart from the normal one.
            boolean cm = type == RaidType.COX && mode == RaidMode.CHALLENGE;
            int kc = state.addCompletion(p.getUUID(), cm ? COX_CM_KEY : type.id(), points(p.getUUID()), purple);
            if (type == RaidType.TOB) {
                // OSRS: Hard mode needs a Normal (or Hard) mode completion, so the modes are counted apart too.
                state.addCount(p.getUUID(), TobRooms.modeKey(mode));
                // A Night at the Theatre: the Mysterious Stranger waits for a Theatre run in any mode
                com.runecraft.content.quests.QuestsState.get(p).setFlag(p.getUUID(),
                        com.runecraft.osrs.content.quests.QuestBossRules.TOB_COMPLETED_FLAG);
            }
            if (type == RaidType.TOA) {
                // OSRS keeps Entry, Normal and Expert mode completion counts apart.
                int modeKc = state.addCount(p.getUUID(), ToaLoot.modeKey(modeLevel));
                Msg.game(p, "Your completed " + ToaLoot.modeCountName(modeLevel) + " count is: " + modeKc + ".");
            } else {
                Msg.game(p, "Your completed " + type.displayName() + (cm ? " Challenge Mode" : "") + " count is: " + kc + ".");
            }
            if (type != RaidType.TOB) {
                Msg.game(p, "Personal points: " + points(p.getUUID()) + ".");
            }
        }
        for (String who : result.purpleRecipients()) {
            if (!party.contains(UUID.fromString(who))) {
                continue; // drawn by a raider who left: nobody gets it
            }
            String name = names.getOrDefault(UUID.fromString(who), "Someone");
            String item = result.lootFor(who).isEmpty() ? "" : result.lootFor(who).get(0).item();
            ItemDef def = ItemCatalog.get(item);
            world.getServer().getPlayerList().broadcastSystemMessage(Component.literal(name + " received special loot from a raid: "
                    + (def == null ? item : def.name()) + "!").withStyle(ChatFormatting.LIGHT_PURPLE), false);
            world.playSound(null, center(current), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1f, 1f);
        }
        if (result.purpleRecipients().stream().noneMatch(w -> party.contains(UUID.fromString(w)))) {
            say(type == RaidType.TOA ? "Osmumten's sarcophagus glows white: the chests hold standard loot."
                    : "The chest contains standard loot.", ChatFormatting.GRAY);
        } else if (type == RaidType.TOA) {
            say("Osmumten's sarcophagus glows purple!", ChatFormatting.LIGHT_PURPLE);
        }
    }

    /** What the ToA chest needs to know about a raider: completions, a thread before, jewels and rewards owned. */
    private ToaLoot.Raider raider(ServerPlayer p) {
        var data = com.runecraft.player.Players.data(p);
        int kc = RaidsState.get(world.getServer()).completions(p.getUUID(), type.id());
        Set<String> owned = new HashSet<>();
        for (String id : List.of("masori_crafting_kit", "menaphite_ornament_kit", "cursed_phalanx", "remnant_of_akkha",
                "remnant_of_baba", "remnant_of_kephri", "remnant_of_zebak", "ancient_remnant")) {
            if (owns(p, id)) {
                owned.add(id);
            }
        }
        Set<String> jewels = new HashSet<>();
        String[] partisans = {"keris_partisan_of_corruption", "keris_partisan_of_the_sun", "keris_partisan_of_breaching",
                "keris_partisan_of_amascut"};
        for (int i = 0; i < ToaLoot.JEWELS.size(); i++) {
            if (owns(p, ToaLoot.JEWELS.get(i)) || owns(p, partisans[i])) {
                jewels.add(ToaLoot.JEWELS.get(i));
            }
        }
        return new ToaLoot.Raider(kc, data.collectionLog().contains(ToaLoot.THREAD), jewels, owned);
    }

    /** Whether a player carries or banks an item (OSRS challenge rewards and jewels check both). */
    private static boolean owns(ServerPlayer p, String item) {
        if (Inv.has(p, item, 1)) {
            return true;
        }
        var bank = com.runecraft.player.Players.data(p).bank();
        return bank != null && bank.count(item, null) > 0;
    }

    /** The reward chest: hands over this player's loot once. */
    public void claim(ServerPlayer p) {
        if (!finished || result == null) {
            Msg.game(p, "The chest is sealed until the raid is complete.");
            return;
        }
        if (!claimed.add(p.getUUID())) {
            Msg.game(p, "You have already claimed your reward. Use the exit to leave.");
            return;
        }
        List<RaidRewards.Loot> loot = result.lootFor(p.getUUID().toString());
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
        if (result.petRecipients().contains(p.getUUID().toString())) {
            world.getServer().getPlayerList().broadcastSystemMessage(Component.literal(p.getName().getString()
                    + " has a funny feeling like they're being followed.").withStyle(ChatFormatting.GOLD), false);
        }
    }

    public boolean hasClaimed(ServerPlayer p) {
        return claimed.contains(p.getUUID());
    }

    /** The loot rolled at the end of the raid (null before). */
    RaidRewards.Result result() {
        return result;
    }

    /** Records that a player used Helpful Spirit supplies or honey locusts ("Helpful spirit who?"). */
    public void markSuppliesTaken(UUID u) {
        suppliesTaken.add(u);
    }

    public void takeSupplies(ServerPlayer p) {
        if (type == RaidType.TOA) {
            // OSRS: the tomb has no free supply chest (the Helpful Spirit offers bundles after the second and fourth path),
            // but a deposit pot beside the spirit acts as a bank deposit box to make room. The tomb's own supplies can't be
            // banked (they would leave the raid and skip the Helpful Spirit's limits next time).
            com.runecraft.bank.BankService.openDepositBox(p, RaidItems.RAID_ONLY_ITEMS::contains);
            Msg.game(p, "You can deposit items here. The Helpful Spirit brings supplies after the second and fourth paths.");
            return;
        }
        if (type == RaidType.COX) {
            // OSRS: the Chambers hand out no supplies; they come from the resource rooms (scavengers, farming, fishing,
            // hunting) and raid herblore.
            Msg.game(p, "The storage unit is for your own items. Gather and brew your supplies in the resource rooms.");
            return;
        }
        if (type == RaidType.TOB) {
            // OSRS: the Theatre's supply chests after the Bloat and Sotetseg sell supplies for points (round 33).
            TobRooms.supplyChest(this, p);
            return;
        }
        if (!suppliesTaken.add(p.getUUID())) {
            Msg.game(p, "You have already taken your share of the supplies.");
            return;
        }
        Inv.give(p, "saradomin_brew", 2);
        Inv.give(p, "super_restore", 2);
        Msg.game(p, "You take some supplies from the storage unit.");
    }

    // ------------------------------------------------------------------ deaths

    /** Called instead of a party member dying. */
    void onDeath(ServerPlayer p) {
        UUID u = p.getUUID();
        if (type == RaidType.TOB) {
            TobRooms.playerDied(this, p); // the Dawnbringer drops where they fell
        }
        deaths.merge(u, 1, Integer::sum);
        restore(p);
        RoomDef room = rooms.get(current);
        int at = roomAt(p.position());
        boolean fighting = logics[current].started && !logics[current].done && (type != RaidType.TOA || at == current || at < 0);
        switch (type) {
            case COX -> {
                int before = points(u);
                int team = totalPoints();
                if (before > 0 && !RaidScaling.coxDeathHitsTeam(before, team)) {
                    points.put(u, RaidScaling.coxDeathPenalty(before));
                    say(p.getName().getString() + " has died and lost " + (before - points(u)) + " points.", ChatFormatting.RED);
                } else {
                    // OSRS: dying with (almost) no points costs the whole team 5% instead.
                    for (UUID v : party) {
                        points.put(v, RaidScaling.coxTeamDeathPenalty(points.getOrDefault(v, 0)));
                    }
                    say(p.getName().getString() + " has died with too few points: the team loses 5% of its points.", ChatFormatting.RED);
                }
                // OSRS: you respawn at the start of the floor you have reached.
                teleport(p, spawn(floorStart(current)));
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
                // OSRS: a death costs 20% of your total reward points, at least 1,000.
                int before = toaTotal(u);
                points.put(u, ToaLoot.afterDeath(before));
                say(p.getName().getString() + " has died and lost " + (before - toaTotal(u)) + " points.", ChatFormatting.RED);
                if (fighting) {
                    down(p, room.name());
                } else {
                    teleport(p, spawn(at >= 0 ? at : current));
                }
            }
        }
        if (fighting && type != RaidType.COX && activePlayers().isEmpty()) {
            wipe();
        }
    }

    private void down(ServerPlayer p, String room) {
        downed.add(p.getUUID());
        teleport(p, gallery(current));
        Msg.red(p, "You have fallen. You will rejoin your party when " + room + " is defeated.");
    }

    private void wipe() {
        boolean entry = type == RaidType.TOB && mode == RaidMode.ENTRY;
        if ((type == RaidType.TOA && retriesLeft != 0) || entry) {
            if (retriesLeft > 0) {
                retriesLeft--;
            }
            say("Your party has fallen! The room resets" + (retriesLeft >= 0 && !entry ? " (" + retriesLeft + " attempts left)." : "."),
                    ChatFormatting.RED);
            if (entry) {
                // OSRS (March 2023): Entry mode has unlimited attempts, and a wipe hands out bandages.
                TobRooms.entryWipe(this);
            } else {
                // OSRS: wiping hands out honey locusts (not with On a Diet).
                ToaSupplyService.wiped(this);
            }
            RoomLogic logic = logics[current];
            logic.reset();
            for (ServerPlayer p : players()) {
                downed.remove(p.getUUID());
                restore(p);
                teleport(p, spawn(current));
            }
            roomStartTick = now();
            roomPoints.clear();
            logic.started = true;
            logic.start();
            return;
        }
        say("Your party has fallen. The " + type.displayName() + " is lost.", ChatFormatting.DARK_RED);
        for (UUID u : new ArrayList<>(party)) {
            removePlayer(u, true);
        }
        RaidManager.close(this);
    }

    private static void restore(ServerPlayer p) {
        p.setHealth(p.getMaxHealth());
        p.clearFire();
        p.removeAllEffects();
        PoisonService.cure(p, true, 0, ActionManager.gameTick());
        p.fallDistance = 0;
    }

    // ------------------------------------------------------------------ logging out and back in (ToA)

    /**
     * Whether a member logging out stays in the party (OSRS: ToA lets you log back in and rejoin; in ToB a logout during a
     * fight counts as a death and logging back in puts you in purgatory with your party).
     */
    boolean keepsOfflineMembers() {
        return type == RaidType.TOA || type == RaidType.TOB;
    }

    /**
     * A member logged out. OSRS: logging out outside a safe area (the lobby, the Nexus, a cleared room) counts as a death
     * in the room being fought; the player rejoins downed when they log back in.
     */
    void disconnected(ServerPlayer p) {
        UUID u = p.getUUID();
        int at = roomAt(p.position());
        boolean unsafe = at == current && logics[current].started && !logics[current].done
                && rooms.get(current).kind() != RoomDef.Kind.NEXUS && !downed.contains(u);
        offline.put(u, now());
        if (unsafe) {
            deaths.merge(u, 1, Integer::sum);
            int before = type == RaidType.TOA ? toaTotal(u) : points(u);
            if (type == RaidType.TOB) {
                TobRooms.playerDied(this, p);
                points.put(u, Math.max(0, before - RaidScaling.TOB_DEATH_PENALTY));
            } else {
                points.put(u, ToaLoot.afterDeath(before));
            }
            downed.add(u);
            int after = type == RaidType.TOA ? toaTotal(u) : points(u);
            say(p.getName().getString() + " has logged out mid-fight and lost " + (before - after) + " points.", ChatFormatting.RED);
            if (activePlayers().isEmpty() && !players().isEmpty()) {
                wipe();
            }
        } else {
            say(p.getName().getString() + " has logged out. They may log back in to rejoin.", ChatFormatting.GRAY);
        }
    }

    /** A member who logged out mid-raid is back. */
    void reconnected(ServerPlayer p) {
        UUID u = p.getUUID();
        if (offline.remove(u) == null) {
            return;
        }
        if (downed.contains(u)) {
            teleport(p, gallery(current));
            Msg.red(p, "You rejoin the raid. You will rejoin your party when " + rooms.get(current).name() + " is defeated.");
        } else if (roomAt(p.position()) < 0) {
            teleport(p, spawn(current));
        }
        say(p.getName().getString() + " has rejoined the raid.", ChatFormatting.GRAY);
        if (logics[current].started && !logics[current].done && activePlayers().isEmpty()) {
            wipe();
        }
    }

    // ------------------------------------------------------------------ leaving

    /** Removes a player from the party (optionally sending them back to the entrance). */
    public void removePlayer(UUID u, boolean teleportOut) {
        if (!party.contains(u)) {
            return;
        }
        ServerPlayer p = world.getServer().getPlayerList().getPlayer(u);
        if (type == RaidType.TOA && finished && result != null && !claimed.contains(u)) {
            // OSRS: loot left in the chest waits in the lobby's Rewards Niche (here: the grouping obelisk outside).
            if (p != null && !offline.containsKey(u)) {
                claim(p);
            } else {
                claimed.add(u);
                List<String> loot = new ArrayList<>();
                for (RaidRewards.Loot l : result.lootFor(u.toString())) {
                    loot.add(l.item() + ":" + l.count());
                }
                RaidsState.get(world.getServer()).addUnclaimed(u, loot);
            }
        }
        party.remove(u);
        downed.remove(u);
        offline.remove(u);
        RaidManager.unbind(u);
        ToaSupplyService.forget(u);
        CoxSupplyService.forget(u);
        if (p != null) {
            if (type == RaidType.TOB && !closed) {
                TobRooms.dropDawnbringer(this, p); // OSRS: a leaver's Dawnbringer drops to the floor for the others
            }
            stripRaidItems(p);
            RaidNet.clearHud(p);
            if (teleportOut) {
                restore(p);
                BlockPos back = returnPos.getOrDefault(u, p.getServer().overworld().getSharedSpawnPos());
                ServerLevel gielinor = com.runecraft.world.Gielinor.world(p.getServer());
                p.teleportTo(gielinor == null ? world : gielinor, back.getX() + 0.5, back.getY(), back.getZ() + 0.5, p.getYRot(), 0);
            }
        }
        if (u.equals(leader) && !party.isEmpty()) {
            leader = party.get(0);
            if (type == RaidType.TOA) {
                say(name(leader) + " now leads the party.", ChatFormatting.GRAY);
            }
        }
    }

    /** Takes away the items that only exist inside a raid (supplies, raid potions, tools) when a player leaves it. */
    static void stripRaidItems(ServerPlayer p) {
        for (String item : RaidItems.RAID_ONLY_ITEMS) {
            Inv.remove(p, item, Inv.count(p, item));
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

    boolean closed() {
        return closed;
    }

    public String name(UUID u) {
        return names.getOrDefault(u, "?");
    }

    // ------------------------------------------------------------------ HUD

    /** A short status line for the HUD: ToA path level, attempts left and time-limit countdown; the CoX/ToB mode. */
    String hudStatus() {
        if (type != RaidType.TOA) {
            return mode == RaidMode.NORMAL ? "" : mode.displayName();
        }
        StringBuilder sb = new StringBuilder();
        if (inPath()) {
            sb.append("Path level ").append(pathLevel());
        } else if (begun) {
            sb.append("Paths ").append(pathsCompleted).append("/4");
        }
        if (retriesLeft >= 0 && begun) {
            sb.append(sb.length() == 0 ? "" : "  ").append("Attempts ").append(retriesLeft + 1);
        }
        int limit = Invocation.timeLimitMinutes(invocations);
        if (limit > 0 && beginTick >= 0 && !finished) {
            long left = Math.max(0, limit * 100L - (now() - beginTick)) * 6 / 10;
            sb.append(sb.length() == 0 ? "" : "  ").append(String.format("Limit %d:%02d", left / 60, left % 60));
        }
        String note = logics[current].started && !logics[current].done ? logics[current].hudNote() : null;
        if (note != null && !note.isEmpty()) {
            sb.append(sb.length() == 0 ? "" : "  ").append(note);
        }
        return sb.toString();
    }
}
