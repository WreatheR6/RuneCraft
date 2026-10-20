package com.runecraft.content.raids;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.boss.Mechanics;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.raids.RoomDef;
import com.runecraft.registry.ModEntities;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Behaviour of one raid room. The default is a combat room: spawn the room's bosses when it starts
 * and complete once every tracked creature is dead. Subclasses add decorations, puzzles and
 * room-wide mechanics.
 */
public class RoomLogic {
    public final RaidInstance raid;
    public final RoomDef def;
    public final int index;
    /** Tracked creatures: the room completes when all of them are dead (combat rooms). */
    protected final List<LivingEntity> spawned = new ArrayList<>();
    boolean started;
    boolean done;

    public RoomLogic(RaidInstance raid, RoomDef def, int index) {
        this.raid = raid;
        this.def = def;
        this.index = index;
    }

    /** Creates the logic for a room by id. */
    static RoomLogic create(RaidInstance raid, RoomDef def, int index) {
        return switch (def.id()) {
            case "cox_lobby", "tob_lobby", "toa_lobby" -> new CommonRooms.Lobby(raid, def, index);
            case "cox_reward", "tob_reward", "toa_reward" -> new CommonRooms.Reward(raid, def, index);
            case "tekton" -> new CoxRooms.Tekton(raid, def, index);
            case "vasa" -> new CoxRooms.Vasa(raid, def, index);
            case "guardians" -> new CoxRooms.Guardians(raid, def, index);
            case "shamans" -> new CoxRooms.Shamans(raid, def, index);
            case "muttadiles" -> new CoxRooms.Muttadiles(raid, def, index);
            case "vanguards" -> new CoxRooms.Vanguards(raid, def, index);
            case "vespula" -> new CoxRooms.Vespula(raid, def, index);
            case "ice_demon" -> new CoxRooms.IceDemon(raid, def, index);
            case "thieving" -> new CoxRooms.Thieving(raid, def, index);
            case "tightrope" -> new CoxRooms.Tightrope(raid, def, index);
            case "crystal_crabs" -> new CoxRooms.CrystalCrabs(raid, def, index);
            case "olm" -> new CoxRooms.Olm(raid, def, index);
            case "cox_scavengers" -> new CoxRooms.Scavengers(raid, def, index);
            case "cox_farming", "cox_hunting" -> new CoxRooms.Farming(raid, def, index);
            case "maiden" -> new TobRooms.Maiden(raid, def, index);
            case "bloat" -> new TobRooms.Bloat(raid, def, index);
            case "nylocas" -> new TobRooms.Nylocas(raid, def, index);
            case "sotetseg" -> new TobRooms.Sotetseg(raid, def, index);
            case "xarpus" -> new TobRooms.Xarpus(raid, def, index);
            case "verzik" -> new TobRooms.Verzik(raid, def, index);
            case "scabaras_puzzle" -> new ToaRooms.Scabaras(raid, def, index);
            case "het_puzzle" -> new ToaRooms.Het(raid, def, index);
            case "apmeken_puzzle" -> new ToaRooms.Apmeken(raid, def, index);
            case "crondis_puzzle" -> new ToaRooms.Crondis(raid, def, index);
            case "kephri" -> new ToaRooms.Kephri(raid, def, index);
            case "akkha" -> new ToaRooms.Akkha(raid, def, index);
            case "baba" -> new ToaRooms.Baba(raid, def, index);
            case "zebak" -> new ToaRooms.Zebak(raid, def, index);
            case "wardens" -> new ToaRooms.Wardens(raid, def, index);
            default -> new RoomLogic(raid, def, index);
        };
    }

    // ------------------------------------------------------------------ lifecycle

    /** Extra blocks after the room shell is built. */
    public void decorate() {
    }

    /**
     * The creatures this room spawns: CoX Skeletal Mystics and Lizardman shamans grow in number with
     * the party (OSRS: 3 + 1 per 3 players up to 12, and 2 + 1 per 5 players up to 5).
     */
    protected List<String> roomCreatures() {
        List<String> bosses = def.bosses();
        int n = switch (def.id()) {
            case "mystics" -> com.runecraft.osrs.content.raids.RaidScaling.coxMysticCount(raid.partySize());
            case "shamans" -> com.runecraft.osrs.content.raids.RaidScaling.coxShamanCount(raid.partySize());
            default -> -1;
        };
        if (n < 0 || bosses.isEmpty()) {
            return bosses;
        }
        return java.util.Collections.nCopies(n, bosses.get(0));
    }

    /** The party entered: spawn the room's creatures. */
    public void start() {
        List<String> bosses = roomCreatures();
        for (int i = 0; i < bosses.size(); i++) {
            // Up to 5 in a line 7 blocks apart; larger groups (12 mystics) form two staggered rows.
            int row = bosses.size() > 5 ? i % 2 : 0;
            int perRow = bosses.size() > 5 ? (bosses.size() + 1) / 2 : bosses.size();
            int col = bosses.size() > 5 ? i / 2 : i;
            double gap = perRow <= 1 ? 0 : Math.min(7, 32.0 / (perRow - 1));
            double spread = perRow <= 1 ? 0 : (col - (perRow - 1) / 2.0) * gap;
            spawnBoss(bosses.get(i), at(8 + row * 4, (int) Math.round(spread)));
        }
    }

    /** Every game tick while the room is active. */
    public void gameTick(long tick) {
        if (!spawned.isEmpty() && allDead()) {
            complete();
        }
    }

    public final void complete() {
        if (!done) {
            done = true;
            onComplete();
            raid.onRoomComplete(this);
        }
    }

    /** Hook before the exit opens. */
    protected void onComplete() {
        cleanup();
    }

    /** A wiped ToA party retries the room. */
    public void reset() {
        cleanup();
        started = false;
    }

    /** Removes everything this room spawned. */
    public void cleanup() {
        for (LivingEntity e : spawned) {
            if (e.isAlive()) {
                e.discard();
            }
        }
        spawned.clear();
    }

    /** The boss shown on the raid HUD (first living tracked boss). */
    public DataBossEntity hudBoss() {
        for (LivingEntity e : spawned) {
            if (e instanceof DataBossEntity b && alive(b)) {
                return b;
            }
        }
        return null;
    }

    /** Whether this raid runs in ToB Hard mode. */
    protected boolean hard() {
        return raid.mode() == com.runecraft.osrs.content.raids.RaidMode.HARD;
    }

    /** A party member damaged a creature in this room (after the hit landed). */
    public void onCreatureHit(ServerPlayer player, LivingEntity target, int damage) {
    }

    /** A party member in this room took damage. */
    public void onPlayerHurt(ServerPlayer player, int damage) {
    }

    /** A scripted block inside this room was used; return true if handled. */
    public boolean useBlock(ServerPlayer player, String blockId, BlockPos pos) {
        return false;
    }

    // ------------------------------------------------------------------ anchoring

    private final java.util.Map<LivingEntity, Vec3> anchors = new java.util.WeakHashMap<>();

    /** Keeps a stationary creature (Olm, Maiden, Sotetseg...) exactly where it is. */
    public void anchor(LivingEntity e) {
        if (e != null) {
            anchors.put(e, e.position());
        }
    }

    public void unanchor(LivingEntity e) {
        anchors.remove(e);
    }

    void keepAnchored(LivingEntity e) {
        Vec3 home = anchors.get(e);
        if (home == null) {
            return;
        }
        e.setDeltaMovement(0, Math.min(0, e.getDeltaMovement().y), 0);
        if (e.position().distanceToSqr(home) > 0.25) {
            e.teleportTo(home.x, home.y, home.z);
        }
    }

    // ------------------------------------------------------------------ boss hooks (from RaidScript)

    /** Every Minecraft tick for each boss in this room. */
    public void bossTick(DataBossEntity boss) {
    }

    public AttackDef bossAttack(DataBossEntity boss, ServerPlayer target, double distance) {
        return null;
    }

    public boolean bossVulnerable(DataBossEntity boss) {
        return true;
    }

    public int bossIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
        return damage;
    }

    public int bossOutgoing(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
        return damage;
    }

    public void bossDeath(DataBossEntity boss, ServerPlayer killer) {
    }

    /**
     * Whether a boss may die now. Damage that skips {@link #bossIncoming} (poison, venom, recoil, Vengeance) can still be
     * fatal; returning false cancels the death, and the room must then give the boss its health back.
     */
    public boolean bossAllowDeath(DataBossEntity boss) {
        return true;
    }

    // ------------------------------------------------------------------ combat helpers

    /** Whether nothing solid lies between two points. */
    public boolean clearLine(Vec3 from, Vec3 to, Entity except) {
        return world().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                except)).getType() == HitResult.Type.MISS;
    }

    public boolean canSee(LivingEntity from, ServerPlayer player) {
        return clearLine(from.getEyePosition(), player.getEyePosition(), from);
    }

    /** The boss's current attack list (its phase's list if the phase has one). */
    public static List<AttackDef> attacks(DataBossEntity boss) {
        BossDef def = boss.def();
        if (def == null) {
            return List.of();
        }
        if (boss.phase() >= 0 && !def.phases.get(boss.phase()).attacks().isEmpty()) {
            return def.phases.get(boss.phase()).attacks();
        }
        return def.attacks;
    }

    public static double phaseMultiplier(DataBossEntity boss) {
        BossDef def = boss.def();
        return def != null && boss.phase() >= 0 ? def.phases.get(boss.phase()).damageMultiplier() : 1.0;
    }

    /**
     * Ranged auto-attack for stationary bosses: the generic attack goal only reaches 10 blocks, so
     * every {@code every} game ticks a random visible player further away is attacked.
     */
    public void autoAttack(DataBossEntity boss, long tick, int every) {
        if (!alive(boss) || tick % every != 0 || boss.isNoAi()) {
            return;
        }
        List<ServerPlayer> targets = new ArrayList<>();
        for (ServerPlayer p : players()) {
            if (p.distanceToSqr(boss) > 9.5 * 9.5 && canSee(boss, p)) {
                targets.add(p);
            }
        }
        if (targets.isEmpty()) {
            return;
        }
        ServerPlayer target = targets.get(rnd().nextInt(targets.size()));
        AttackDef attack = bossAttack(boss, target, boss.distanceTo(target));
        if (attack == null) {
            List<AttackDef> list = new ArrayList<>();
            for (AttackDef a : attacks(boss)) {
                if (!a.melee()) {
                    list.add(a);
                }
            }
            if (list.isEmpty()) {
                return;
            }
            attack = Mechanics.choose(list, 0, boss.getRandom());
        }
        Mechanics.perform(boss, target, attack, phaseMultiplier(boss));
    }

    /** Teleports a boss (and stops it walking off). */
    public void move(LivingEntity e, Vec3 pos) {
        e.teleportTo(pos.x, pos.y, pos.z);
        if (e instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        world().sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, pos.x, pos.y + 1, pos.z, 30, 0.6, 1, 0.6, 0.2);
    }

    // ------------------------------------------------------------------ helpers

    /** Whether the player carries (or wields) an item whose id ends with {@code suffix}, e.g. "_pickaxe". */
    public static boolean hasTool(ServerPlayer p, String suffix) {
        if (com.runecraft.util.Inv.idOf(p.getMainHandItem()).endsWith(suffix)) {
            return true;
        }
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack s = p.getInventory().getItem(i);
            if (!s.isEmpty() && com.runecraft.util.Inv.idOf(s).endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    public static int level(ServerPlayer p, com.runecraft.osrs.Skill skill) {
        return com.runecraft.player.Players.data(p).skills().currentLevel(skill);
    }

    /** Switches off a player's protection prayers (ToA Penetration, Ba-Ba's sarcophagi...). */
    public static void dropOverheads(ServerPlayer p, String message) {
        com.runecraft.player.PlayerData data = com.runecraft.player.Players.data(p);
        if (data.activePrayers().removeIf(pr -> pr == com.runecraft.osrs.Prayer.PROTECT_FROM_MELEE
                || pr == com.runecraft.osrs.Prayer.PROTECT_FROM_MISSILES || pr == com.runecraft.osrs.Prayer.PROTECT_FROM_MAGIC)) {
            com.runecraft.network.ServerNetworking.sync(p);
            Msg.red(p, message);
        }
    }

    /** A player's current prayer points. */
    public static int prayerPoints(ServerPlayer p) {
        return com.runecraft.player.Players.data(p).prayerPoints();
    }

    /** Per-player cooldown for skilling clicks (game ticks). */
    private final java.util.Map<java.util.UUID, Long> clickCooldown = new java.util.HashMap<>();

    public boolean cooldown(ServerPlayer p, int ticks) {
        long now = RaidEffects.now();
        Long last = clickCooldown.get(p.getUUID());
        if (last != null && now - last < ticks) {
            return true;
        }
        clickCooldown.put(p.getUUID(), now);
        return false;
    }

    public ServerLevel world() {
        return raid.world;
    }

    public Random rnd() {
        return raid.random;
    }

    public BlockPos center() {
        return raid.center(index);
    }

    /** Standing position (floor + 1) relative to the room centre. */
    public BlockPos at(int dx, int dz) {
        return center().offset(dx, 1, dz);
    }

    public Vec3 vec(double dx, double dz) {
        BlockPos c = center();
        return new Vec3(c.getX() + 0.5 + dx, c.getY() + 1, c.getZ() + 0.5 + dz);
    }

    public boolean inside(Vec3 pos) {
        return raid.inRoom(pos, index);
    }

    public List<ServerPlayer> players() {
        List<ServerPlayer> list = new ArrayList<>();
        for (ServerPlayer p : raid.activePlayers()) {
            if (inside(p.position())) {
                list.add(p);
            }
        }
        return list;
    }

    public ServerPlayer randomPlayer() {
        List<ServerPlayer> list = players();
        return list.isEmpty() ? null : list.get(rnd().nextInt(list.size()));
    }

    public void say(String message) {
        for (ServerPlayer p : raid.players()) {
            Msg.game(p, message);
        }
    }

    public void warn(String message) {
        for (ServerPlayer p : raid.players()) {
            Msg.red(p, message);
        }
    }

    public boolean allDead() {
        for (LivingEntity e : spawned) {
            if (e.isAlive() && !e.isRemoved()) {
                return false;
            }
        }
        return true;
    }

    public boolean alive(LivingEntity e) {
        return e != null && e.isAlive() && !e.isRemoved();
    }

    /** Spawns a raid boss, scaled for the party, and tracks it. */
    public DataBossEntity spawnBoss(String id, BlockPos pos) {
        return spawnBoss(id, pos, true);
    }

    public DataBossEntity spawnBoss(String id, BlockPos pos, boolean track) {
        BossDef def = ContentRegistry.boss(id);
        if (def == null) {
            return null;
        }
        DataBossEntity boss = DataBossService.spawn(world(), pos, def, randomPlayer());
        if (boss == null) {
            return null;
        }
        boss.instanceTag = "raid:" + raid.slot;
        raid.scale(boss);
        if (track) {
            spawned.add(boss);
        }
        return boss;
    }

    /** Spawns a module creature (minion), scaled for the party, optionally tracked. */
    public Mob spawnMob(String id, BlockPos pos, boolean track) {
        EntityType<?> type = ModEntities.typeById(id);
        if (type == null) {
            return null;
        }
        Entity e = type.create(world());
        if (!(e instanceof Mob mob)) {
            return null;
        }
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, rnd().nextFloat() * 360f, 0);
        mob.finalizeSpawn(world(), world().getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        mob.setPersistenceRequired();
        world().addFreshEntity(mob);
        raid.scale(mob);
        ServerPlayer target = randomPlayer();
        if (target != null && !mob.isNoAi()) {
            mob.setTarget(target);
        }
        if (track) {
            spawned.add(mob);
        }
        return mob;
    }

    /** Living creatures of a type within this room. */
    public List<LivingEntity> living(String id) {
        EntityType<?> type = ModEntities.typeById(id);
        List<LivingEntity> list = new ArrayList<>();
        if (type == null) {
            return list;
        }
        for (Entity e : world().getEntities((Entity) null, RaidBuilder.roomBox(raid.slot, index), x -> x.getType() == type)) {
            if (e instanceof LivingEntity le && le.isAlive()) {
                list.add(le);
            }
        }
        return list;
    }

    /**
     * Moves healers (Nylocas Matomenos, scarab swarms...) towards {@code boss}; any that reach it heal
     * the boss by their remaining hitpoints times {@code factor} and vanish. Returns how many reached it.
     */
    public int healers(String id, LivingEntity boss, double factor, String message) {
        int reached = 0;
        if (!alive(boss)) {
            return 0;
        }
        for (LivingEntity e : living(id)) {
            if (e instanceof Mob mob) {
                mob.setTarget(null);
                if (mob.tickCount % 10 == 0 || mob.getNavigation().isDone()) {
                    mob.getNavigation().moveTo(boss.getX(), boss.getY(), boss.getZ(), 1.0);
                }
            }
            if (e.distanceToSqr(boss) < Math.pow(boss.getBbWidth() / 2 + 1.6, 2)) {
                int heal = (int) Math.max(1, raid.hitpoints(e) * factor);
                raid.heal(boss, heal);
                world().sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, boss.getX(), boss.getY() + boss.getBbHeight(), boss.getZ(),
                        4, 0.5, 0.3, 0.5, 0);
                if (message != null) {
                    say(message);
                }
                e.discard();
                reached++;
            }
        }
        return reached;
    }
}
