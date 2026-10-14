package com.runecraft.content.skilling;

import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.MonsterStats;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.GameEvents;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.skilling.SailingData;
import com.runecraft.osrs.content.skilling.SailingSea;
import com.runecraft.osrs.content.skilling.SailingTraining;
import com.runecraft.registry.ModEntities;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Sailing at sea (OSRS Wiki): sorting salvage at a salvaging station with the wiki loot tables, boat combat (cannons
 * firing cannonballs at sea creatures, creatures hitting the boat's hitpoints through the keel's armour, sinking),
 * bounty tasks from the port notice boards, ocean encounters while under way and sailing hazards.
 */
public final class SeaHooks {
    private SeaHooks() {
    }

    private static final Random RNG = new Random();
    static final String SEA_TAG = "runecraft_sea_creature";

    static void init() {
        BlockHooks.register("port_salvaging_station", (player, pos, state) ->
                Msg.game(player, "Use salvage while standing at the salvaging station to sort it."));
        GameEvents.onKill(SeaHooks::onKill);
    }

    // ------------------------------------------------------------------ salvage sorting

    /** Next to a port salvaging station, or on/next to your boat with a salvaging station built on it. */
    public static boolean atStation(ServerPlayerEntity player) {
        BlockState station = SkillingUtil.moduleBlock("port_salvaging_station");
        if (station != null) {
            BlockPos p = player.getBlockPos();
            for (BlockPos q : BlockPos.iterate(p.add(-4, -2, -4), p.add(4, 2, 4))) {
                if (player.getWorld().getBlockState(q).isOf(station.getBlock())) {
                    return true;
                }
            }
        }
        String type = SailingHooks.boatType(SailingHooks.nearbyBoat(player));
        return type != null && SailingHooks.hasFacility(player, type, "salvaging_station");
    }

    /** Sorts one salvage from the stack (wiki loot table and sorting XP); false when the player can't sort here. */
    public static boolean sort(ServerPlayerEntity player, ItemStack stack, SailingData.Wreck wreck) {
        if (!atStation(player)) {
            Msg.game(player, "You need to be at a salvaging station (at a port, or built on your boat) to sort salvage.");
            return false;
        }
        if (!SkillingUtil.need(player, Skill.SAILING, wreck.level(), "sort this salvage")) {
            return false;
        }
        stack.decrement(1);
        List<SailingSea.Got> got = SailingSea.sort(wreck.salvage(), RNG);
        XpService.add(player, Skill.SAILING, wreck.sortXp());
        List<String> names = new ArrayList<>();
        for (SailingSea.Got g : got) {
            SkillingUtil.give(player, g.item(), g.count());
            names.add((g.count() > 1 ? g.count() + " x " : "") + SkillingUtil.lower(SkillingUtil.name(g.item())));
        }
        Msg.action(player, "You sort through the salvage" + (names.isEmpty() ? "." : " and find " + String.join(", ", names) + "."));
        return true;
    }

    // ------------------------------------------------------------------ bounty tasks

    static NbtCompound bounties(ServerPlayerEntity player) {
        return SkillingState.get(player.getServer()).entry("bounty_tasks", player.getUuidAsString());
    }

    /** Takes a bounty task (no level checks); used by the board and the self-test. */
    public static void takeBounty(ServerPlayerEntity player, SailingSea.Bounty b) {
        bounties(player).putBoolean(String.valueOf(b.id()), true);
    }

    public static void clearBounties(ServerPlayerEntity player) {
        NbtCompound held = bounties(player);
        for (String key : new ArrayList<>(held.getKeys())) {
            held.remove(key);
        }
    }

    public static boolean hasBounty(ServerPlayerEntity player, int id) {
        return bounties(player).contains(String.valueOf(id));
    }

    /**
     * Port notice board, before courier tasks: drops off a castaway, hands in finished bounties (XP and as many coins),
     * cancels a bounty when sneaking, or sometimes offers one of the board's bounty tasks. True when it did something.
     */
    static boolean portBoard(ServerPlayerEntity player, SailingData.Port here, int couriers) {
        int level = SkillingUtil.level(player, Skill.SAILING);
        boolean acted = false;
        if (Inv.has(player, "castaway", 1)) {
            Inv.remove(player, "castaway", 1);
            XpService.add(player, Skill.SAILING, SailingSea.castawayDropXp(level));
            Msg.action(player, "You drop the castaway off safely at " + here.name() + ".");
            acted = true;
        }
        NbtCompound held = bounties(player);
        for (String key : new ArrayList<>(held.getKeys())) {
            SailingSea.Bounty b = SailingSea.bounty(Integer.parseInt(key));
            if (b == null) {
                held.remove(key);
                continue;
            }
            if (Inv.count(player, b.item()) >= b.qty()) {
                Inv.remove(player, b.item(), b.qty());
                held.remove(key);
                XpService.add(player, Skill.SAILING, b.xp());
                Inv.give(player, "coins", (int) b.xp());
                Msg.action(player, "You hand in " + b.qty() + " x " + SkillingUtil.lower(b.itemName()) + " for " + (int) b.xp()
                        + " Sailing XP and " + (int) b.xp() + " coins.");
                acted = true;
            }
        }
        if (acted) {
            return true;
        }
        if (player.isSneaking()) {
            if (held.getKeys().isEmpty()) {
                return false;
            }
            String key = held.getKeys().iterator().next();
            held.remove(key);
            Msg.game(player, "You cancel a bounty task.");
            return true;
        }
        for (String key : held.getKeys()) {
            SailingSea.Bounty b = SailingSea.bounty(Integer.parseInt(key));
            Msg.game(player, "Bounty: " + b.qty() + " x " + b.itemName() + " (" + Inv.count(player, b.item()) + " so far).");
        }
        if (level < SailingSea.BOUNTY_LEVEL || level < here.level() || couriers + held.getKeys().size() >= SailingTraining.taskSlots(level)) {
            return false;
        }
        List<SailingSea.Bounty> options = new ArrayList<>();
        for (SailingSea.Bounty b : SailingSea.boardBounties(here.id(), level)) {
            boolean same = false;
            for (String key : held.getKeys()) {
                SailingSea.Bounty h = SailingSea.bounty(Integer.parseInt(key));
                same |= h != null && h.item().equals(b.item());
            }
            if (!same) {
                options.add(b);
            }
        }
        // The board shows courier and bounty tasks side by side; pick either kind (approx).
        if (options.isEmpty() || (RNG.nextBoolean() && !SailingTraining.boardTasks(here.id(), level).isEmpty())) {
            return false;
        }
        SailingSea.Bounty b = options.get(RNG.nextInt(options.size()));
        takeBounty(player, b);
        SailingSea.SeaCreature c = SailingSea.creature(b.creature());
        Msg.action(player, "You take a bounty task: bring " + b.qty() + " x " + SkillingUtil.lower(b.itemName()) + " from "
                + SkillingUtil.lower(c.name()) + "s (try " + c.location() + ") to any port.");
        return true;
    }

    private static void onKill(ServerPlayerEntity player, LivingEntity entity, MonsterStats stats) {
        if (!(stats.profile() instanceof MonsterDef md)) {
            return;
        }
        SailingSea.SeaCreature c = SailingSea.creature(md.id);
        if (c == null) {
            return;
        }
        NbtCompound held = bounties(player);
        for (String key : held.getKeys()) {
            SailingSea.Bounty b = SailingSea.bounty(Integer.parseInt(key));
            if (b != null && b.creature().equals(c.id()) && Inv.count(player, b.item()) < b.qty() && RNG.nextDouble() < b.chance()) {
                SkillingUtil.give(player, b.item(), 1);
                player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5f, 1.6f);
                Msg.action(player, "You harvest " + Msg.article(SkillingUtil.lower(b.itemName())) + " for your bounty ("
                        + Inv.count(player, b.item()) + "/" + b.qty() + ").");
            }
        }
    }

    // ------------------------------------------------------------------ at sea

    /** Per-player state at sea: boat hitpoints, encounter timer, hazard timer, cannon target and cooldown. */
    private static final class Sea {
        int hp = -1;
        String boat;
        int encounterTicks = SailingSea.ENCOUNTER_TICKS;
        int interval = 1;
        int hazardTicks;
        int spawnTicks;
        int cannonCooldown;
        UUID target;
    }

    private static final Map<UUID, Sea> SEA = new HashMap<>();

    /** A sea creature spawned around a boat: its owner and attack cooldown. */
    private static final class Spawned {
        final UUID owner;
        final SailingSea.SeaCreature creature;
        int cooldown;
        boolean engaged;

        Spawned(UUID owner, SailingSea.SeaCreature creature) {
            this.owner = owner;
            this.creature = creature;
        }
    }

    private static final Map<MobEntity, Spawned> SPAWNED = new HashMap<>();

    static int maxHp(ServerPlayerEntity player, String type) {
        return SailingSea.boatHp(type, SailingHooks.partTier(player, type, SailingTraining.PartType.HULL),
                SailingHooks.partTier(player, type, SailingTraining.PartType.KEEL));
    }

    /** The boat's hitpoints (full when not yet damaged). */
    public static int boatHp(ServerPlayerEntity player, String type) {
        Sea s = SEA.get(player.getUuid());
        return s == null || s.hp < 0 || !type.equals(s.boat) ? maxHp(player, type) : s.hp;
    }

    /** Called every game tick while the player is on one of their boats. */
    static void boatTick(ServerPlayerEntity player, Entity vehicle, String type) {
        Sea s = SEA.computeIfAbsent(player.getUuid(), k -> new Sea());
        if (s.hp < 0 || !type.equals(s.boat)) {
            s.hp = maxHp(player, type);
            s.boat = type;
        }
        Vec3d v = vehicle.getVelocity();
        boolean moving = v.x * v.x + v.z * v.z > 0.01;
        if (moving && --s.encounterTicks <= 0) {
            s.encounterTicks = SailingSea.ENCOUNTER_TICKS;
            if (SailingSea.encounterSpawns(s.interval, RNG)) {
                s.interval = 1;
                encounter(player, type, SailingSea.rollEncounter(RNG));
            } else {
                s.interval++;
            }
        }
        if (++s.hazardTicks >= SailingSea.HAZARD_TICKS) {
            s.hazardTicks = 0;
            hazard(player, vehicle, type, s);
        }
        if (++s.spawnTicks >= 100) {
            s.spawnTicks = 0;
            spawnNearby(player);
        }
        tickCreatures(player, vehicle, type, s);
        if (s.cannonCooldown > 0) {
            s.cannonCooldown--;
        } else if (s.target != null) {
            Entity e = ((ServerWorld) player.getWorld()).getEntity(s.target);
            if (e instanceof MobEntity mob && mob.isAlive() && mob.distanceTo(player) <= SailingSea.CANNON_RANGE) {
                fire(player, type, mob, null);
            } else {
                s.target = null;
            }
        }
    }

    /** Called every game tick while the player is not on a boat: tidies up their sea creatures. */
    static void offBoat(ServerPlayerEntity player) {
        if (SPAWNED.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<MobEntity, Spawned>> it = SPAWNED.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<MobEntity, Spawned> e = it.next();
            MobEntity mob = e.getKey();
            if (!mob.isAlive() || mob.isRemoved()) {
                it.remove();
            } else if (e.getValue().owner.equals(player.getUuid()) && (mob.getWorld() != player.getWorld() || mob.distanceTo(player) > 64)) {
                mob.discard();
                it.remove();
            }
        }
    }

    // ------------------------------------------------------------------ sea creatures

    /** Spawns a sea creature (AI off: it floats where it is and attacks boats from range). Used by the self-test too. */
    public static MobEntity spawnCreature(ServerWorld world, SailingSea.SeaCreature c, Vec3d pos, ServerPlayerEntity owner) {
        EntityType<?> type = ModEntities.typeById(c.id());
        if (type == null) {
            return null;
        }
        Entity e = type.create(world);
        if (!(e instanceof MobEntity mob)) {
            return null;
        }
        mob.refreshPositionAndAngles(pos.x, pos.y, pos.z, RNG.nextFloat() * 360f, 0);
        mob.initialize(world, world.getLocalDifficulty(mob.getBlockPos()), SpawnReason.MOB_SUMMONED, null, null);
        mob.setAiDisabled(true);
        mob.addCommandTag(SEA_TAG);
        world.spawnEntity(mob);
        SPAWNED.put(mob, new Spawned(owner.getUuid(), c));
        return mob;
    }

    private static int owned(ServerPlayerEntity player) {
        int n = 0;
        for (Map.Entry<MobEntity, Spawned> e : SPAWNED.entrySet()) {
            if (e.getValue().owner.equals(player.getUuid()) && e.getKey().isAlive()) {
                n++;
            }
        }
        return n;
    }

    /** Sea creatures live at their wiki spots: within 40 tiles of one, up to two rise near the boat. */
    private static void spawnNearby(ServerPlayerEntity player) {
        if (owned(player) >= 2) {
            return;
        }
        int x = GielinorMap.toOsrsX(player.getBlockX());
        int y = GielinorMap.toOsrsY(player.getBlockZ());
        SailingSea.SeaCreature best = null;
        double bestD = 40 * 40;
        for (SailingSea.SeaCreature c : SailingSea.CREATURES) {
            double d = (double) (c.x() - x) * (c.x() - x) + (double) (c.y() - y) * (c.y() - y);
            if (d < bestD) {
                bestD = d;
                best = c;
            }
        }
        if (best == null) {
            return;
        }
        ServerWorld world = (ServerWorld) player.getWorld();
        double angle = RNG.nextDouble() * Math.PI * 2;
        double r = 10 + RNG.nextDouble() * 4;
        BlockPos at = BlockPos.ofFloored(player.getX() + Math.cos(angle) * r, player.getY(), player.getZ() + Math.sin(angle) * r);
        for (int dy = 2; dy >= -3; dy--) {
            BlockPos q = at.up(dy);
            if (world.getFluidState(q).isIn(FluidTags.WATER) && world.getBlockState(q.up()).isAir()) {
                MobEntity mob = spawnCreature(world, best, new Vec3d(q.getX() + 0.5, q.getY() + 0.6, q.getZ() + 0.5), player);
                if (mob != null) {
                    world.spawnParticles(ParticleTypes.SPLASH, mob.getX(), mob.getY() + 0.5, mob.getZ(), 20, 0.6, 0.3, 0.6, 0.1);
                    if (best.aggressive()) {
                        Msg.red(player, "A " + SkillingUtil.lower(best.name()) + " rises from the water!");
                    }
                }
                return;
            }
        }
    }

    /** Aggressive creatures (and any you have shot at) attack your boat from up to 15 tiles away. */
    private static void tickCreatures(ServerPlayerEntity player, Entity vehicle, String type, Sea s) {
        Iterator<Map.Entry<MobEntity, Spawned>> it = SPAWNED.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<MobEntity, Spawned> e = it.next();
            MobEntity mob = e.getKey();
            Spawned sp = e.getValue();
            if (!mob.isAlive() || mob.isRemoved()) {
                it.remove();
                continue;
            }
            if (!sp.owner.equals(player.getUuid())) {
                continue;
            }
            if (mob.distanceTo(player) > 64) {
                mob.discard();
                it.remove();
                continue;
            }
            if (!(sp.creature.aggressive() || sp.engaged) || mob.distanceTo(player) > SailingSea.CANNON_RANGE || ++sp.cooldown < sp.creature.speed()) {
                continue;
            }
            sp.cooldown = 0;
            mob.lookAtEntity(player, 30f, 30f);
            int max = Math.max(0, sp.creature.maxHitAgainst(0) - SailingSea.keelReduction(type,
                    SailingHooks.partTier(player, type, SailingTraining.PartType.KEEL)));
            int dmg = max <= 0 || RNG.nextBoolean() ? 0 : 1 + RNG.nextInt(max); // approx: half the attacks land
            if (dmg > 0) {
                hurtBoat(player, vehicle, type, s, dmg, "The " + SkillingUtil.lower(sp.creature.name()) + " hits your boat");
            }
        }
    }

    private static void hurtBoat(ServerPlayerEntity player, Entity vehicle, String type, Sea s, int dmg, String what) {
        s.hp -= dmg;
        int max = maxHp(player, type);
        player.getWorld().playSound(null, vehicle.getBlockPos(), SoundEvents.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, SoundCategory.PLAYERS, 0.5f, 1f);
        Msg.action(player, what + " for " + dmg + " (" + Math.max(0, s.hp) + "/" + max + " boat hitpoints).");
        if (s.hp <= 0) {
            sink(player, vehicle, type, s);
        }
    }

    /** At 0 hitpoints the boat breaks: you are thrown into the water and the kit must be relaunched (OSRS: shipwright repair). */
    private static void sink(ServerPlayerEntity player, Entity vehicle, String type, Sea s) {
        SailingData.Boat kit = SailingHooks.boatOf(vehicle);
        player.stopRiding();
        vehicle.discard();
        if (kit != null) {
            Inv.give(player, kit.kit(), 1);
        }
        s.hp = -1;
        s.target = null;
        Msg.red(player, "Your " + type + " has been wrecked! You salvage the " + (kit == null ? "boat" : SkillingUtil.lower(kit.name()) + " kit")
                + " and swim for it.");
    }

    // ------------------------------------------------------------------ cannons

    /** Right-click with cannonballs on a boat with a cannon: fire at the nearest sea creature in range. */
    static boolean useCannonball(ServerPlayerEntity player, String ball) {
        String type = SailingHooks.boatType(player.getVehicle());
        if (type == null) {
            return false;
        }
        SailingSea.Cannon cannon = SailingSea.bestCannon(SailingHooks.facilities(player, type));
        if (cannon == null) {
            Msg.game(player, "You need a cannon built on your boat to fire cannonballs.");
            return true;
        }
        MobEntity target = null;
        double best = SailingSea.CANNON_RANGE;
        for (MobEntity mob : SPAWNED.keySet()) {
            if (mob.isAlive() && mob.getWorld() == player.getWorld() && mob.distanceTo(player) <= best) {
                best = mob.distanceTo(player);
                target = mob;
            }
        }
        if (target == null) {
            Msg.game(player, "There's nothing in range of your cannon.");
            return true;
        }
        Sea s = SEA.computeIfAbsent(player.getUuid(), k -> new Sea());
        s.target = target.getUuid();
        if (s.cannonCooldown <= 0) {
            fire(player, type, target, ball);
        }
        return true;
    }

    private static void fire(ServerPlayerEntity player, String type, MobEntity target, String preferred) {
        Sea s = SEA.computeIfAbsent(player.getUuid(), k -> new Sea());
        SailingSea.Cannon cannon = SailingSea.bestCannon(SailingHooks.facilities(player, type));
        if (cannon == null) {
            s.target = null;
            return;
        }
        SailingSea.Ball ball = pickBall(player, cannon, preferred);
        if (ball == null) {
            Msg.game(player, "You have no cannonballs your " + SkillingUtil.lower(cannon.metal()) + " cannon can fire.");
            s.target = null;
            return;
        }
        fireCannon(player, target, cannon, ball);
        s.cannonCooldown = SailingSea.CANNON_TICKS;
    }

    private static SailingSea.Ball pickBall(ServerPlayerEntity player, SailingSea.Cannon cannon, String preferred) {
        SailingSea.Ball p = preferred == null ? null : SailingSea.ball(preferred);
        if (p != null && p.tier() <= cannon.tier() && Inv.has(player, p.item(), 1)) {
            return p;
        }
        SailingSea.Ball best = null;
        for (SailingSea.Ball b : SailingSea.BALLS) {
            if (b.tier() <= cannon.tier() && Inv.has(player, b.item(), 1)) {
                best = b;
            }
        }
        return best;
    }

    /**
     * One cannon shot by the player: uses a cannonball, rolls the wiki max hit (Ranged level) and an accuracy roll, and
     * gives 1 Sailing and 4 Ranged XP per damage rolled (overkill included). Returns the damage. Used by the self-test.
     */
    public static int fireCannon(ServerPlayerEntity player, MobEntity target, SailingSea.Cannon cannon, SailingSea.Ball ball) {
        if (SkillingUtil.level(player, Skill.RANGED) < cannon.ranged()) {
            Msg.game(player, "You need a Ranged level of " + cannon.ranged() + " to fire this cannon.");
            return 0;
        }
        if (Inv.remove(player, ball.item(), 1) < 1) {
            return 0;
        }
        Spawned sp = SPAWNED.get(target);
        if (sp != null) {
            sp.engaged = true;
        }
        SailingSea.SeaCreature c = sp != null ? sp.creature : null;
        ServerWorld world = (ServerWorld) player.getWorld();
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.6f, 1.4f);
        world.spawnParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 0.5, target.getZ(), 1, 0, 0, 0, 0);
        int max = SailingSea.cannonMaxHit(SkillingUtil.level(player, Skill.RANGED), cannon, ball, true);
        boolean hit = c == null || RNG.nextDouble() < SailingSea.cannonAccuracy(cannon, ball, true, c);
        int dmg = hit ? RNG.nextInt(max + 1) : 0;
        CombatHandler.applyDamage(player, target, dmg);
        if (dmg > 0) {
            XpService.add(player, Skill.SAILING, dmg);
            XpService.add(player, Skill.RANGED, dmg * 4.0);
        }
        return dmg;
    }

    // ------------------------------------------------------------------ ocean encounters

    private static void encounter(ServerPlayerEntity player, String type, SailingSea.Encounter e) {
        int level = SkillingUtil.level(player, Skill.SAILING);
        ServerWorld world = (ServerWorld) player.getWorld();
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_DOLPHIN_SPLASH, SoundCategory.PLAYERS, 0.8f, 1f);
        switch (e) {
            case STRONG_WINDS -> {
                double xp = 3 * SailingTraining.trimXp(type, SailingHooks.partTier(player, type, SailingTraining.PartType.SAILS));
                XpService.add(player, Skill.SAILING, xp);
                Msg.action(player, "You sail into strong winds and trim your sails again and again.");
            }
            case MYSTERIOUS_GLOW -> {
                XpService.add(player, Skill.SAILING, SailingSea.glowXp(level));
                String gem = SailingSea.glowGem(level, RNG);
                SkillingUtil.give(player, gem, 1);
                Msg.action(player, "You sail through a chain of mysterious glows and find " + Msg.article(SkillingUtil.lower(SkillingUtil.name(gem)))
                        + " on deck.");
            }
            case LOST_CRATE -> {
                SailingSea.Got g = SailingSea.lostCrate(level, RNG);
                SkillingUtil.give(player, g.item(), g.count());
                Msg.action(player, "You fish a lost crate out of the water: " + g.count() + " x " + SkillingUtil.lower(SkillingUtil.name(g.item())) + ".");
            }
            case CASTAWAY -> {
                if (Inv.has(player, "castaway", 1)) {
                    Msg.game(player, "You spot a castaway, but you already have one aboard.");
                    return;
                }
                Inv.give(player, "castaway", 1);
                XpService.add(player, Skill.SAILING, SailingSea.castawayPickupXp(level));
                Msg.action(player, "You pull a castaway aboard. Drop them off at any port's notice board.");
            }
            case GIANT_CLAM -> {
                SkillingUtil.give(player, "oyster_pearl", 1); // approx: OSRS clams polish an item you fed them
                Msg.action(player, "A giant clam opens beside the boat and you take a pearl.");
            }
            case CLUE_TURTLE -> {
                String clue = SailingSea.clueTurtle(level, RNG);
                XpService.add(player, Skill.SAILING, SailingSea.CLUE_TURTLE_XP);
                SkillingUtil.give(player, clue, 1);
                Msg.action(player, "You retrieve a bottle from a clue turtle: " + SkillingUtil.lower(SkillingUtil.name(clue)) + ".");
            }
            case OCEAN_MAN -> {
                SkillingUtil.give(player, "fruit_blast", 1);
                Msg.action(player, "Ocean Man swims up and hands you a cocktail.");
            }
            case LOST_CASKET -> {
                String tier = SailingSea.clueTurtle(level, RNG).replace("clue_scroll_", "reward_casket_");
                SkillingUtil.give(player, ItemCatalog.exists(tier) ? tier : "casket", 1);
                Msg.action(player, "You fish a lost casket out of the sea!");
            }
        }
    }

    // ------------------------------------------------------------------ hazards

    private static void hazard(ServerPlayerEntity player, Entity vehicle, String type, Sea s) {
        SailingSea.Hazard h = SailingSea.hazardAt(GielinorMap.toOsrsX(player.getBlockX()), GielinorMap.toOsrsY(player.getBlockZ()));
        if (h == null || SailingSea.protectedFrom(h, SailingHooks.tiers(player, type), SailingHooks.facilities(player, type))) {
            return;
        }
        if (h.hurtsPlayer) {
            Msg.red(player, h.message);
            player.damage(player.getDamageSources().magic(), h.damage);
        } else {
            hurtBoat(player, vehicle, type, s, h.damage, h.message.replace("!", "") + ": your boat takes damage");
        }
    }
}
