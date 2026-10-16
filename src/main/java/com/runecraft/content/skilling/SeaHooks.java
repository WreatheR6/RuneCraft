package com.runecraft.content.skilling;

import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.MonsterStats;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.GameEvents;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.skilling.SailingCrew;
import com.runecraft.osrs.content.skilling.SailingData;
import com.runecraft.osrs.content.skilling.SailingSea;
import com.runecraft.osrs.content.skilling.SailingTraining;
import com.runecraft.quest.QuestApi;
import com.runecraft.quest.QuestScripts;
import com.runecraft.registry.ModEntities;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Arrays;
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
    /** The one boat paint RuneCraft has (a port task reward). */
    static final String PAINT = "shark_paint";

    static void init() {
        BlockHooks.register("port_salvaging_station", (player, pos, state) ->
                Msg.game(player, "Use salvage while standing at the salvaging station to sort it."));
        GameEvents.onKill(SeaHooks::onKill);
        for (SailingCrew.Crewmate c : SailingCrew.CREW) {
            QuestScripts.onTalk(c.id(), (player, npc) -> crewTalk(player, c));
        }
        // Sneak + use your boat with an empty hand: empty its cargo hold into your inventory.
        // Boat paint (OSRS: a cosmetic colour scheme for your boat): use the paint on one of your boats to repaint its hull.
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (hand != Hand.MAIN_HAND || !(entity instanceof BoatEntity boat) || !PAINT.equals(Inv.idOf(player.getMainHandStack()))) {
                return ActionResult.PASS;
            }
            if (world.isClient() || !(player instanceof ServerPlayerEntity sp) || SailingHooks.boatType(entity) == null) {
                return ActionResult.PASS;
            }
            boat.setVariant(BoatEntity.Type.DARK_OAK); // approx: the shark scheme's dark grey hull
            player.getMainHandStack().decrement(1);
            Msg.action(sp, "You paint your boat in the colours of a shark.");
            return ActionResult.SUCCESS;
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (hand != Hand.MAIN_HAND || !(entity instanceof BoatEntity) || !player.isSneaking() || !player.getMainHandStack().isEmpty()) {
                return ActionResult.PASS;
            }
            if (world.isClient() || !(player instanceof ServerPlayerEntity sp)) {
                return ActionResult.PASS;
            }
            String type = SailingHooks.boatType(entity);
            if (type == null) {
                return ActionResult.PASS;
            }
            withdrawAll(sp, type);
            return ActionResult.SUCCESS;
        });
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
        int crewCannon;
        int crewRepair;
        UUID speedVehicle;
        double speed = 1;
        /** The cannonball last loaded by hand (plain, chainshot or incendiary): auto-fire and the crew keep firing it. */
        String ammo;
    }

    /** The cannonball a player last loaded (null for none); used by the self-test. */
    public static String loadedAmmo(ServerPlayerEntity player) {
        Sea s = SEA.get(player.getUuid());
        return s == null ? null : s.ammo;
    }

    /** Loads a cannonball type for auto-fire and the crew (self-test and manual fire). */
    public static void loadAmmo(ServerPlayerEntity player, String item) {
        SEA.computeIfAbsent(player.getUuid(), k -> new Sea()).ammo = item;
    }

    private static final Map<UUID, Sea> SEA = new HashMap<>();

    /** A sea creature spawned around a boat: its owner and attack cooldown. */
    private static final class Spawned {
        final UUID owner;
        final SailingSea.SeaCreature creature;
        int cooldown;
        boolean engaged;
        /** Hits left on each burn (incendiary cannonballs; at most 5 stack). */
        final List<Integer> burns = new ArrayList<>();
        int burnTimer;

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
        // Hull speed (wiki Hull table) for the Sailing boats; the extras module's boats keep their own tier speed.
        double speed = vehicle instanceof com.runecraft.content.extras.SailBoatEntity ? 1 : SailingCrew.velocityMultiplier(
                SailingCrew.speedFactor(type, SailingHooks.partTier(player, type, SailingTraining.PartType.HULL)));
        if (!vehicle.getUuid().equals(s.speedVehicle) || speed != s.speed) {
            s.speedVehicle = vehicle.getUuid();
            s.speed = speed;
            sendSpeed(player, speed, vehicle.getId());
        }
        crewTick(player, vehicle, type, s);
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
                fire(player, type, mob, s.ammo);
            } else {
                s.target = null;
            }
        }
    }

    /** Called every game tick while the player is not on a boat: tidies up their sea creatures. */
    static void offBoat(ServerPlayerEntity player) {
        Sea sea = SEA.get(player.getUuid());
        if (sea != null && sea.speedVehicle != null) {
            sea.speedVehicle = null;
            sendSpeed(player, 1, -1);
        }
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
            if (!sp.burns.isEmpty() && ++sp.burnTimer >= SailingCrew.BURN_INTERVAL) {
                sp.burnTimer = 0;
                int burn = sp.burns.size();
                sp.burns.replaceAll(h -> h - 1);
                sp.burns.removeIf(h -> h <= 0);
                CombatHandler.applyDamage(player, mob, burn);
                if (!mob.isAlive()) {
                    continue;
                }
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

    /**
     * At 0 hitpoints the boat is wrecked (wiki Boat combat): you wash up at the nearest port, the courier crates, bounty
     * items and salvage in its cargo hold are lost, and a shipwright must recover the boat before it can sail again.
     */
    private static void sink(ServerPlayerEntity player, Entity vehicle, String type, Sea s) {
        SailingData.Boat kit = SailingHooks.boatOf(vehicle);
        player.stopRiding();
        vehicle.discard();
        if (kit != null) {
            SailingHooks.boatState(player).putString("wrecked." + kit.kit(), type);
        }
        int lost = 0;
        NbtCompound hold = hold(player, type);
        for (String item : new ArrayList<>(hold.getKeys())) {
            if (SailingCrew.lostOnSinking(item)) {
                lost += hold.getInt(item);
                hold.remove(item);
            }
        }
        s.hp = -1;
        s.target = null;
        SailingData.Port port = nearestPort(player);
        if (port != null) {
            BlockPos dock = Gielinor.safeSpot((ServerWorld) player.getWorld(), GielinorMap.toBlockX(port.x()), GielinorMap.toBlockZ(port.y()));
            player.requestTeleport(dock.getX() + 0.5, dock.getY(), dock.getZ() + 0.5);
        }
        Msg.red(player, "Your " + type + " has been wrecked! You wash up" + (port == null ? "" : " at " + port.name())
                + (lost > 0 ? " and the " + lost + " items of cargo in its hold are lost" : "") + ".");
        Msg.game(player, "A shipwright at the boat schematics can recover your " + type + " for "
                + SailingCrew.SHIPWRIGHT_GP_PER_HP + " coins a hitpoint.");
    }

    private static SailingData.Port nearestPort(ServerPlayerEntity player) {
        int x = GielinorMap.toOsrsX(player.getBlockX());
        int y = GielinorMap.toOsrsY(player.getBlockZ());
        SailingData.Port best = null;
        double bestD = Double.MAX_VALUE;
        for (SailingData.Port p : SailingData.PORTS) {
            double d = (double) (p.x() - x) * (p.x() - x) + (double) (p.y() - y) * (p.y() - y);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    /** True when the player has a wrecked boat waiting for a shipwright. */
    public static boolean hasWreck(ServerPlayerEntity player) {
        for (String key : SailingHooks.boatState(player).getKeys()) {
            if (key.startsWith("wrecked.")) {
                return true;
            }
        }
        return false;
    }

    /**
     * At the boat schematics: a shipwright recovers each wrecked boat for 50 coins a hitpoint of its full hitpoints
     * (wiki repair rate; the recovery fee itself is approx) and hands the kit back. True when there was a wreck.
     */
    public static boolean recoverWrecks(ServerPlayerEntity player) {
        NbtCompound state = SailingHooks.boatState(player);
        boolean any = false;
        for (String key : new ArrayList<>(state.getKeys())) {
            if (!key.startsWith("wrecked.")) {
                continue;
            }
            any = true;
            String kit = key.substring("wrecked.".length());
            String type = state.getString(key);
            int fee = maxHp(player, type) * SailingCrew.SHIPWRIGHT_GP_PER_HP;
            if (!Inv.has(player, Inv.COINS, fee)) {
                Msg.game(player, "The shipwright wants " + fee + " coins to recover your wrecked " + type + ".");
                continue;
            }
            Inv.remove(player, Inv.COINS, fee);
            state.remove(key);
            Inv.give(player, kit, 1);
            Msg.action(player, "The shipwright recovers your " + type + " for " + fee + " coins and hands you its kit.");
        }
        return any;
    }

    /** Wrecks a boat for the self-test: as if it sank, without the teleport. */
    public static void markWrecked(ServerPlayerEntity player, String kit, String type) {
        SailingHooks.boatState(player).putString("wrecked." + kit, type);
    }

    // ------------------------------------------------------------------ repair kits

    /** Applies a repair kit on your boat (wiki: heals the kit's amount, up to the boat's hitpoints). */
    static boolean applyKit(ServerPlayerEntity player, ItemStack stack, SailingCrew.Kit kit) {
        String type = SailingHooks.boatType(player.getVehicle());
        if (type == null) {
            if (player.isSneaking() && deposit(player, kit.id(), stack.getCount())) {
                return true;
            }
            Msg.game(player, "You need to be aboard your boat to repair it.");
            return true;
        }
        int max = maxHp(player, type);
        int hp = boatHp(player, type);
        if (hp >= max) {
            Msg.game(player, "Your " + type + " doesn't need repairing.");
            return true;
        }
        stack.decrement(1);
        int healed = heal(player, type, kit.heals());
        Msg.action(player, "You repair your " + type + " (+" + healed + ", " + boatHp(player, type) + "/" + max + " hitpoints).");
        return true;
    }

    /** Repairs the boat by up to {@code amount} hitpoints; returns the hitpoints restored. Used by the self-test. */
    public static int heal(ServerPlayerEntity player, String type, int amount) {
        Sea s = SEA.computeIfAbsent(player.getUuid(), k -> new Sea());
        int max = maxHp(player, type);
        if (s.hp < 0 || !type.equals(s.boat)) {
            s.hp = max;
            s.boat = type;
        }
        int before = s.hp;
        s.hp = Math.min(max, s.hp + amount);
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.PLAYERS, 0.8f, 1f);
        return s.hp - before;
    }

    /** Sets the boat's hitpoints (self-test). */
    public static void setBoatHp(ServerPlayerEntity player, String type, int hp) {
        Sea s = SEA.computeIfAbsent(player.getUuid(), k -> new Sea());
        s.boat = type;
        s.hp = hp;
    }

    // ------------------------------------------------------------------ cargo hold

    static NbtCompound hold(ServerPlayerEntity player, String type) {
        NbtCompound all = SkillingState.get(player.getServer()).entry("cargo_hold", player.getUuidAsString());
        if (!all.contains(type)) {
            all.put(type, new NbtCompound());
        }
        return all.getCompound(type);
    }

    private static Map<String, Integer> holdContents(NbtCompound hold) {
        Map<String, Integer> m = new HashMap<>();
        for (String k : hold.getKeys()) {
            m.put(k, hold.getInt(k));
        }
        return m;
    }

    private static boolean stackable(String item) {
        return ItemCatalog.exists(item) && ItemCatalog.get(item).maxStack() > 1;
    }

    /** How many of an item the boat's cargo hold holds. */
    public static int inHold(ServerPlayerEntity player, String type, String item) {
        return hold(player, type).getInt(item);
    }

    /** Puts items straight into a boat's cargo hold (self-test). */
    public static void stockHold(ServerPlayerEntity player, String type, String item, int count) {
        NbtCompound hold = hold(player, type);
        hold.putInt(item, hold.getInt(item) + count);
    }

    /** Empties a boat's cargo hold (self-test). */
    public static void clearHold(ServerPlayerEntity player, String type) {
        NbtCompound hold = hold(player, type);
        for (String k : new ArrayList<>(hold.getKeys())) {
            hold.remove(k);
        }
    }

    /**
     * Sneak + use a storable item on (or next to) your boat with a cargo hold: deposits the whole stack, as far as the
     * hold's capacity allows (wiki capacity by hold tier and boat). Returns false when there's no boat with a hold here.
     */
    public static boolean deposit(ServerPlayerEntity player, String item, int count) {
        String type = SailingHooks.boatType(SailingHooks.nearbyBoat(player));
        SailingCrew.Hold h = type == null ? null : SailingCrew.bestHold(SailingHooks.facilities(player, type));
        if (h == null || !SailingCrew.storable(item) || count <= 0) {
            return false;
        }
        NbtCompound hold = hold(player, type);
        int fit = SailingCrew.room(holdContents(hold), item, count, h.capacity(type), SeaHooks::stackable);
        int moved = fit <= 0 ? 0 : Inv.remove(player, item, fit);
        if (moved <= 0) {
            Msg.game(player, "Your " + SkillingUtil.lower(h.name()) + " is full (" + h.capacity(type) + " slots).");
            return true;
        }
        hold.putInt(item, hold.getInt(item) + moved);
        Msg.action(player, "You store " + moved + " x " + SkillingUtil.lower(SkillingUtil.name(item)) + " in the cargo hold ("
                + SailingCrew.slotsUsed(holdContents(hold), SeaHooks::stackable) + "/" + h.capacity(type) + ").");
        return true;
    }

    /** Takes everything out of the boat's cargo hold. */
    static void withdrawAll(ServerPlayerEntity player, String type) {
        NbtCompound hold = hold(player, type);
        if (hold.getKeys().isEmpty()) {
            Msg.game(player, "Your " + type + "'s cargo hold is empty. Sneak and use cargo near the boat to store it.");
            return;
        }
        List<String> names = new ArrayList<>();
        for (String item : new ArrayList<>(hold.getKeys())) {
            int n = hold.getInt(item);
            hold.remove(item);
            if (n > 0) {
                SkillingUtil.give(player, item, n);
                names.add(n + " x " + SkillingUtil.lower(SkillingUtil.name(item)));
            }
        }
        Msg.action(player, "You empty the cargo hold: " + String.join(", ", names) + ".");
    }

    /** Takes one of the items from the boat's hold (crew repairs and cannon ammunition); false when there's none. */
    private static boolean takeFromHold(ServerPlayerEntity player, String type, String item) {
        NbtCompound hold = hold(player, type);
        int n = hold.getInt(item);
        if (n <= 0) {
            return false;
        }
        if (n == 1) {
            hold.remove(item);
        } else {
            hold.putInt(item, n - 1);
        }
        return true;
    }

    // ------------------------------------------------------------------ crew

    static NbtCompound crew(ServerPlayerEntity player) {
        return SkillingState.get(player.getServer()).entry("sailing_crew", player.getUuidAsString());
    }

    /** Recruited crewmates in crew order. */
    public static List<String> crewOrder(ServerPlayerEntity player) {
        String order = crew(player).getString("order");
        return order.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(order.split(",")));
    }

    public static void setCrew(ServerPlayerEntity player, List<String> order) {
        crew(player).putString("order", String.join(",", order));
    }

    /** Talking to a crewmate recruits them (wiki levels and quests); sneak-talking dismisses them from the crew. */
    public static void crewTalk(ServerPlayerEntity player, SailingCrew.Crewmate c) {
        List<String> order = crewOrder(player);
        if (order.contains(c.id())) {
            if (player.isSneaking()) {
                order.remove(c.id());
                setCrew(player, order);
                Msg.action(player, c.name() + " leaves your crew.");
            } else {
                Msg.game(player, c.name() + ": Ready when you are, captain! (H" + c.helmsmanship() + " P" + c.privateering() + " D"
                        + c.deckhandiness() + "; sneak-talk to dismiss me.)");
            }
            return;
        }
        if (SkillingUtil.level(player, Skill.SAILING) < c.level()) {
            Msg.game(player, c.name() + ": Come back when you've a Sailing level of " + c.level() + ".");
            return;
        }
        if (c.quest() != null && !QuestApi.isComplete(player, c.quest()) && !player.getAbilities().creativeMode) {
            Msg.game(player, c.name() + ": I'll not sail with you until you've proven yourself (quest required).");
            return;
        }
        if (c.id().equals("cabin_boy_jenkins") && !Inv.has(player, "ghostspeak_amulet", 1) && !player.getAbilities().creativeMode) {
            Msg.game(player, "You hear a faint voice, but you can't make out the words. (Ghostspeak amulet needed.)");
            return;
        }
        order.add(c.id());
        setCrew(player, order);
        Msg.action(player, c.name() + " joins your crew. Crewmates sail with you on a skiff (2) or sloop (5), up to the slots your"
                + " Sailing level unlocks (" + SailingCrew.crewSlots(SkillingUtil.level(player, Skill.SAILING)) + ").");
    }

    /**
     * The crew at work (OSRS assigns them to facilities; here the best one for each job, approx): the best privateer
     * mans the cannon against creatures in range when you aren't firing it, and the best deckhand repairs the boat with
     * kits from the cargo hold once it drops below half its hitpoints.
     */
    private static void crewTick(ServerPlayerEntity player, Entity vehicle, String type, Sea s) {
        if (s.crewCannon > 0) {
            s.crewCannon--;
        }
        if (s.crewRepair > 0) {
            s.crewRepair--;
        }
        if (s.crewCannon > 0 && s.crewRepair > 0) {
            return;
        }
        List<SailingCrew.Crewmate> aboard = SailingCrew.aboard(crewOrder(player), type, SkillingUtil.level(player, Skill.SAILING));
        if (aboard.isEmpty()) {
            return;
        }
        SailingCrew.Crewmate gunner = null;
        SailingCrew.Crewmate hand = null;
        for (SailingCrew.Crewmate c : aboard) {
            if (gunner == null || c.privateering() > gunner.privateering()) {
                gunner = c;
            }
            if (hand == null || c.deckhandiness() > hand.deckhandiness()) {
                hand = c;
            }
        }
        if (s.crewRepair <= 0 && s.hp >= 0 && s.hp < maxHp(player, type) * SailingCrew.CREW_REPAIR_BELOW) {
            crewRepair(player, type, hand);
            s.crewRepair = 4; // approx: the eating delay
        }
        SailingSea.Cannon cannon = SailingSea.bestCannon(SailingHooks.facilities(player, type));
        if (s.crewCannon > 0 || s.target != null || cannon == null || !SailingCrew.canMan(cannon, gunner.privateering())
                || SkillingUtil.level(player, Skill.RANGED) < cannon.ranged()) {
            return;
        }
        MobEntity target = null;
        for (Map.Entry<MobEntity, Spawned> e : SPAWNED.entrySet()) {
            MobEntity mob = e.getKey();
            Spawned sp = e.getValue();
            if (sp.owner.equals(player.getUuid()) && mob.isAlive() && (sp.engaged || sp.creature.aggressive())
                    && mob.distanceTo(player) <= SailingSea.CANNON_RANGE) {
                target = mob;
                break;
            }
        }
        if (target != null) {
            crewFire(player, type, target, cannon, gunner);
            s.crewCannon = SailingSea.CANNON_TICKS;
        }
    }

    /** A crewmate applies the best repair kit in the cargo hold; returns the hitpoints restored (0: no kit). Used by the self-test. */
    public static int crewRepair(ServerPlayerEntity player, String type, SailingCrew.Crewmate hand) {
        for (int i = SailingCrew.KITS.size() - 1; i >= 0; i--) {
            SailingCrew.Kit kit = SailingCrew.KITS.get(i);
            if (takeFromHold(player, type, kit.id())) {
                int healed = heal(player, type, SailingCrew.crewRepair(kit, hand.deckhandiness(), RNG));
                Msg.action(player, hand.name() + " patches the hull with " + Msg.article(SkillingUtil.lower(kit.name())) + " (+" + healed
                        + ", " + boatHp(player, type) + "/" + maxHp(player, type) + ").");
                return healed;
            }
        }
        return 0;
    }

    /**
     * A crewmate fires the cannon: a cannonball from the cargo hold (or your inventory), the wiki crew max hit (your
     * Sailing level, times (Privateering + 13) / 20). Returns the damage, or -1 with no ammunition. Used by the self-test.
     */
    public static int crewFire(ServerPlayerEntity player, String type, MobEntity target, SailingSea.Cannon cannon, SailingCrew.Crewmate c) {
        SailingSea.Ball ball = null;
        String item = null;
        // The cannonball you loaded (chainshot and incendiary included) first, then the best plain ball the cannon takes.
        String loaded = loadedAmmo(player);
        SailingSea.Ball l = loaded == null ? null : SailingCrew.ballStats(loaded);
        if (l != null && l.tier() <= cannon.tier() && (takeFromHold(player, type, loaded) || Inv.remove(player, loaded, 1) > 0)) {
            ball = l;
            item = loaded;
        }
        for (int i = SailingSea.BALLS.size() - 1; i >= 0 && ball == null; i--) {
            SailingSea.Ball b = SailingSea.BALLS.get(i);
            if (b.tier() <= cannon.tier() && (takeFromHold(player, type, b.item()) || Inv.remove(player, b.item(), 1) > 0)) {
                ball = b;
                item = b.item();
            }
        }
        if (ball == null) {
            return -1;
        }
        Spawned sp = SPAWNED.get(target);
        if (sp != null) {
            sp.engaged = true;
        }
        ServerWorld world = (ServerWorld) player.getWorld();
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.5f, 1.5f);
        world.spawnParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 0.5, target.getZ(), 1, 0, 0, 0, 0);
        int max = SailingCrew.crewMaxHit(SkillingUtil.level(player, Skill.SAILING), cannon, ball, c.privateering());
        boolean hit = sp == null || RNG.nextDouble() < SailingSea.cannonAccuracy(cannon, ball, false, sp.creature);
        int dmg = hit ? RNG.nextInt(max + 1) : 0;
        if (hit) {
            special(sp, item);
        }
        CombatHandler.applyDamage(player, target, dmg);
        if (dmg > 0) {
            XpService.add(player, Skill.SAILING, dmg); // approx: crew shots give the Sailing share of a player's shot
        }
        return dmg;
    }

    /** A chainshot hit delays the creature's next attack; an incendiary hit adds a burn (up to 5). */
    private static void special(Spawned sp, String item) {
        SailingCrew.Special special = item == null ? null : SailingCrew.special(item);
        if (special == null || sp == null) {
            return;
        }
        if (special.chainshot()) {
            sp.cooldown = Math.min(sp.cooldown, 0) - SailingCrew.CHAINSHOT_TICKS;
        } else if (sp.burns.size() < SailingCrew.BURN_MAX_STACKS) {
            sp.burns.add(SailingCrew.BURN_HITS);
        }
    }

    /** Sends the hull speed of the boat entity {@code boatId} you ride (-1 and 1: none); the client applies it on water only. */
    private static void sendSpeed(ServerPlayerEntity player, double speed, int boatId) {
        if (!ServerPlayNetworking.canSend(player, SeaNet.BOAT_SPEED)) {
            return;
        }
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeDouble(speed);
        buf.writeInt(boatId);
        ServerPlayNetworking.send(player, SeaNet.BOAT_SPEED, buf);
    }

    // ------------------------------------------------------------------ cannons

    /** Right-click with cannonballs on a boat with a cannon: fire at the nearest sea creature in range. */
    static boolean useCannonball(ServerPlayerEntity player, String ball) {
        String type = SailingHooks.boatType(player.getVehicle());
        if (type == null) {
            return player.isSneaking() && deposit(player, ball, Inv.count(player, ball));
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
        s.ammo = ball; // auto-fire and the crew keep firing this cannonball while you carry it
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
        String item = pickBall(player, cannon, preferred);
        if (item == null) {
            Msg.game(player, "You have no cannonballs your " + SkillingUtil.lower(cannon.metal()) + " cannon can fire.");
            s.target = null;
            return;
        }
        fireCannon(player, target, cannon, item);
        s.cannonCooldown = SailingSea.CANNON_TICKS;
    }

    /** The cannonball item to fire: the one used (plain, chainshot or incendiary) if the cannon takes it, else the best plain one. */
    private static String pickBall(ServerPlayerEntity player, SailingSea.Cannon cannon, String preferred) {
        SailingSea.Ball p = preferred == null ? null : SailingCrew.ballStats(preferred);
        if (p != null && p.tier() <= cannon.tier() && Inv.has(player, preferred, 1)) {
            return preferred;
        }
        SailingSea.Ball best = null;
        for (SailingSea.Ball b : SailingSea.BALLS) {
            if (b.tier() <= cannon.tier() && Inv.has(player, b.item(), 1)) {
                best = b;
            }
        }
        return best == null ? null : best.item();
    }

    /**
     * One cannon shot by the player: uses a cannonball, rolls the wiki max hit (Ranged level) and an accuracy roll, and
     * gives 1 Sailing and 4 Ranged XP per damage rolled (overkill included). Chainshot delays the creature's next attack
     * (wiki: halves its speed for 8 ticks; approx) and incendiary balls burn it on a hit (wiki Burn: 1 damage every 4 ticks,
     * 10 times, up to 5 burns; approx: every hit burns). Returns the damage. Used by the self-test.
     */
    public static int fireCannon(ServerPlayerEntity player, MobEntity target, SailingSea.Cannon cannon, String item) {
        SailingSea.Ball ball = SailingCrew.ballStats(item);
        if (ball == null) {
            return 0;
        }
        if (SkillingUtil.level(player, Skill.RANGED) < cannon.ranged()) {
            Msg.game(player, "You need a Ranged level of " + cannon.ranged() + " to fire this cannon.");
            return 0;
        }
        if (Inv.remove(player, item, 1) < 1) {
            return 0;
        }
        Spawned sp = SPAWNED.get(target);
        if (sp != null) {
            sp.engaged = true;
        }
        SailingCrew.Special special = SailingCrew.special(item);
        SailingSea.SeaCreature c = sp != null ? sp.creature : null;
        ServerWorld world = (ServerWorld) player.getWorld();
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.6f, 1.4f);
        world.spawnParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 0.5, target.getZ(), 1, 0, 0, 0, 0);
        int max = SailingSea.cannonMaxHit(SkillingUtil.level(player, Skill.RANGED), cannon, ball, true);
        boolean hit = c == null || RNG.nextDouble() < SailingSea.cannonAccuracy(cannon, ball, true, c);
        int dmg = hit ? RNG.nextInt(max + 1) : 0;
        if (hit && special != null) {
            special(sp, item);
        }
        CombatHandler.applyDamage(player, target, dmg);
        if (dmg > 0) {
            XpService.add(player, Skill.SAILING, dmg);
            XpService.add(player, Skill.RANGED, dmg * 4.0);
        }
        return dmg;
    }

    /** Burn instances on a spawned sea creature (self-test). */
    public static int burns(MobEntity mob) {
        Spawned sp = SPAWNED.get(mob);
        return sp == null ? 0 : sp.burns.size();
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
                if (level < SailingSea.CRATE_LEVELS[0]) {
                    return;
                }
                int tier = SailingSea.lostCrateTier(level, RNG);
                SailingSea.Got g = SailingSea.lostCrateLoot(tier, RNG);
                SkillingUtil.give(player, g.item(), g.count());
                XpService.add(player, Skill.SAILING, SailingSea.CRATE_XP[tier]);
                Msg.action(player, "You fish a lost " + SailingSea.CRATE_NAMES[tier] + " crate out of the water: " + g.count() + " x "
                        + SkillingUtil.lower(SkillingUtil.name(g.item())) + ".");
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
