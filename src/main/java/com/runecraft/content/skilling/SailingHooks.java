package com.runecraft.content.skilling;

import com.runecraft.action.Action;
import com.runecraft.action.ActionManager;
import com.runecraft.content.BlockHooks;
import com.runecraft.osrs.Chance;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.skilling.CookingData;
import com.runecraft.osrs.content.skilling.SailingData;
import com.runecraft.osrs.content.skilling.SailingTraining;
import com.runecraft.quest.QuestApi;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.LandmarkBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Sailing: launching boats, sail trimming, salvaging shipwrecks, sea charting (wiki tasks), courier
 * tasks between notice boards (wiki tasks), boat parts and facilities at boat schematics, the
 * Barracuda Trials, the crystal extractor and deep sea trawling.
 */
final class SailingHooks {
    static final String BOAT_TAG = "runecraft_boat_";

    private SailingHooks() {
    }

    static void init() {
        for (SailingData.Wreck w : SailingData.WRECKS) {
            BlockHooks.register(w.blockId(), (player, pos, state) -> salvage(player, pos, w));
            LandmarkBuilder.registerCustom(w.blockId() + "_site", (world, centre) -> placeAtSea(world, centre, w.blockId(), 0));
        }
        BlockHooks.register("sea_chart_marker", (player, pos, state) -> chart(player, pos));
        for (SailingTraining.ChartTask c : SailingTraining.CHARTS) {
            if (!c.underground()) {
                LandmarkBuilder.registerCustom(c.placeId(), (world, centre) -> placeAtSea(world, centre, "sea_chart_marker", 0));
            }
        }
        BlockHooks.register("boat_schematics", (player, pos, state) -> schematics(player));
        BlockHooks.register("barracuda_trial_buoy", (player, pos, state) -> trialBuoy(player, pos));
        for (SailingTraining.Trial t : SailingTraining.TRIALS) {
            LandmarkBuilder.registerCustom(t.placeId(), (world, centre) -> placeAtSea(world, centre, "barracuda_trial_buoy", 0));
        }
        for (int i = 0; i < SailingData.PORTS.size(); i++) {
            SailingData.Port p = SailingData.PORTS.get(i);
            BlockHooks.register(p.board(), (player, pos, state) -> portBoard(player, p));
            boolean camp = i < 9; // the first nine ports have a camp plaza holding their board
            boolean shipwright = SHIPWRIGHT_PORTS.contains(p.id());
            LandmarkBuilder.registerCustom(p.id() + "_docks", (world, centre) -> {
                if (!camp) {
                    placeAtSea(world, centre, p.board(), 0);
                }
                if (shipwright) {
                    placeAtSea(world, centre, "boat_schematics", 2);
                }
            });
        }
        LandmarkBuilder.registerCustom("fishing_platform_deep_sea", (world, centre) -> {
            placeAtSea(world, centre, "deep_sea_fishing_spot", 0);
            placeAtSea(world, centre, "deep_sea_fishing_spot", 4);
        });
    }

    /** Ports with a shipyard (boat schematics): the shipwright ports of the extras module. */
    private static final Set<String> SHIPWRIGHT_PORTS = Set.of("port_sarim", "rellekka", "port_piscarilius", "aldarin");

    /** Puts a module block on the water surface (or on the ground if the spot turned out to be land). */
    private static void placeAtSea(ServerWorld world, BlockPos centre, String blockId, int dx) {
        BlockState block = SkillingUtil.moduleBlock(blockId);
        if (block == null) {
            return;
        }
        BlockPos at = centre.add(dx, 0, 0);
        BlockState here = world.getBlockState(at);
        if (here.getFluidState().isIn(FluidTags.WATER) || here.isReplaceable()) {
            world.setBlockState(at, block, Block.NOTIFY_LISTENERS);
        } else {
            world.setBlockState(at.up(), block, Block.NOTIFY_LISTENERS);
        }
    }

    // ------------------------------------------------------------------ boats

    static void launch(ServerPlayerEntity player, ItemStack stack, SailingData.Boat boat) {
        String why = SailingData.canSail(pandemoniumDone(player), SkillingUtil.level(player, Skill.SAILING), boat);
        if (why != null) {
            Msg.game(player, why);
            return;
        }
        Vec3d eye = player.getEyePos();
        Vec3d end = eye.add(player.getRotationVector().multiply(5.0));
        BlockHitResult hit = player.getWorld().raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.SOURCE_ONLY, player));
        if (hit.getType() != HitResult.Type.BLOCK || !player.getWorld().getFluidState(hit.getBlockPos()).isIn(FluidTags.WATER)) {
            Msg.game(player, "Use the kit while looking at water to launch your " + SkillingUtil.lower(boat.name()) + ".");
            return;
        }
        Vec3d p = hit.getPos();
        BoatEntity entity = new BoatEntity(player.getWorld(), p.x, p.y, p.z);
        entity.setVariant(BoatEntity.Type.valueOf(boat.variant()));
        entity.setYaw(player.getYaw());
        entity.addCommandTag(BOAT_TAG + boat.kit());
        entity.setCustomName(net.minecraft.text.Text.literal(player.getName().getString() + "'s " + SkillingUtil.lower(boat.name())));
        player.getWorld().spawnEntity(entity);
        stack.decrement(1);
        player.getWorld().playSound(null, entity.getBlockPos(), SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.PLAYERS, 1f, 1f);
        Msg.action(player, "You launch your " + SkillingUtil.lower(boat.name()) + ".");
    }

    /** Sailing is taught by the Pandemonium quest (creative players skip it). */
    static boolean pandemoniumDone(ServerPlayerEntity player) {
        return player.getAbilities().creativeMode || QuestApi.isComplete(player, SailingData.SAILING_QUEST);
    }

    static SailingData.Boat boatOf(Entity vehicle) {
        if (!(vehicle instanceof BoatEntity)) {
            return null;
        }
        for (String tag : vehicle.getCommandTags()) {
            if (tag.startsWith(BOAT_TAG)) {
                return SailingData.boat(tag.substring(BOAT_TAG.length()));
            }
        }
        return null;
    }

    private static final Map<UUID, int[]> TICKS = new HashMap<>();

    /**
     * Called every game tick: boat level requirements, sail trimming (wiki trim XP of the boat's sails every 30
     * seconds under way; automatic here, approx), the crystal extractor (250 XP every 109 ticks) and Barracuda Trials.
     */
    static void boatTick(ServerPlayerEntity player) {
        Entity vehicle = player.getVehicle();
        SailingData.Boat boat = boatOf(vehicle);
        if (boat != null && SkillingUtil.level(player, Skill.SAILING) < boat.level()) {
            player.stopRiding();
            Msg.game(player, "You need a Sailing level of " + boat.level() + " to sail this " + SkillingUtil.lower(boat.name()) + ".");
            return;
        }
        String type = boatType(vehicle);
        if (type == null) {
            TICKS.remove(player.getUuid());
            if (TRIALS.containsKey(player.getUuid())) {
                trialTick(player);
            }
            return;
        }
        int[] t = TICKS.computeIfAbsent(player.getUuid(), k -> new int[2]);
        Vec3d v = vehicle.getVelocity();
        if (v.x * v.x + v.z * v.z > 0.01 && ++t[0] >= SailingTraining.TRIM_TICKS) {
            t[0] = 0;
            double xp = SailingTraining.trimXp(type, partTier(player, type, SailingTraining.PartType.SAILS));
            XpService.add(player, Skill.SAILING, xp);
            Msg.action(player, "A gust of wind catches your sails and you trim them.");
        }
        if (++t[1] >= SailingTraining.EXTRACTOR_TICKS && hasFacility(player, type, "crystal_extractor")) {
            t[1] = 0;
            XpService.add(player, Skill.SAILING, SailingTraining.EXTRACTOR_XP);
            Msg.action(player, "You harvest a wind mote from the crystal extractor.");
        }
        if (TRIALS.containsKey(player.getUuid())) {
            trialTick(player);
        }
    }

    private static boolean nearBoat(ServerPlayerEntity player) {
        return player.getVehicle() instanceof BoatEntity
                || !player.getWorld().getEntitiesByClass(BoatEntity.class, player.getBoundingBox().expand(6), b -> true).isEmpty();
    }

    // ------------------------------------------------------------------ salvaging

    private static void salvage(ServerPlayerEntity player, BlockPos pos, SailingData.Wreck w) {
        if (!SkillingUtil.need(player, Skill.SAILING, w.level(), "salvage this shipwreck")) {
            return;
        }
        if (!SkillingUtil.needItem(player, "salvaging_hook", "You need a salvaging hook to salvage shipwrecks.")) {
            return;
        }
        ServerWorld world = (ServerWorld) player.getWorld();
        if (GatheringHooks.depleted(world, pos)) {
            Msg.game(player, "There's nothing left to salvage here for now.");
            return;
        }
        if (!nearBoat(player)) {
            Msg.game(player, "You need to be on (or next to) a boat to salvage shipwrecks.");
            return;
        }
        Msg.action(player, "You lower your salvaging hook into the wreck.");
        ActionManager.start(player, new SkillingUtil.BlockAction(pos, w.blockId(), 5) {
            @Override
            boolean roll(ServerPlayerEntity p, Random random) {
                if (!Inv.has(p, "salvaging_hook", 1) || GatheringHooks.depleted(world, pos)) {
                    return false;
                }
                world.playSound(null, pos, SoundEvents.ENTITY_FISHING_BOBBER_SPLASH, SoundCategory.PLAYERS, 0.5f, 0.8f);
                if (random.nextDouble() < Chance.lerp(SkillingUtil.level(p, Skill.SAILING), w.low(), w.high())) {
                    SkillingUtil.give(p, w.salvage(), 1);
                    XpService.add(p, Skill.SAILING, w.xp());
                    Msg.action(p, "You haul up some salvage.");
                    if (random.nextInt(8) == 0) {
                        GatheringHooks.deplete(world, pos, 60);
                        Msg.game(p, "The wreck has been picked clean for now.");
                        return false;
                    }
                }
                return true;
            }
        }, true);
    }

    // ------------------------------------------------------------------ sea charting

    private static Set<Integer> charted(NbtCompound charted) {
        Set<Integer> done = new HashSet<>();
        for (String k : charted.getKeys()) {
            if (k.startsWith("t") && charted.getBoolean(k)) {
                done.add(Integer.parseInt(k.substring(1)));
            }
        }
        return done;
    }

    private static void chart(ServerPlayerEntity player, BlockPos pos) {
        int x = GielinorMap.toOsrsX(pos.getX());
        int y = GielinorMap.toOsrsY(pos.getZ());
        SailingTraining.ChartTask task = SailingTraining.nearestChart(x, y, 8);
        if (task == null) {
            Msg.game(player, "There's nothing to chart here.");
            return;
        }
        List<String> tools = new ArrayList<>();
        for (SailingTraining.ChartType t : SailingTraining.ChartType.values()) {
            if (t.tool != null && Inv.has(player, t.tool, 1)) {
                tools.add(t.tool);
            }
        }
        String why = SailingTraining.canChart(task, SkillingUtil.level(player, Skill.SAILING), tools);
        if (why != null) {
            Msg.game(player, why);
            return;
        }
        if ((task.type() == SailingTraining.ChartType.SPYGLASS || task.type() == SailingTraining.ChartType.CURRENT) && !nearBoat(player)) {
            Msg.game(player, "You need to be on your boat to chart this.");
            return;
        }
        NbtCompound charted = SkillingState.get(player.getServer()).entry("charted", player.getUuidAsString());
        if (charted.getBoolean("t" + task.id())) {
            Msg.game(player, "You have already charted this. (" + task.task() + ")");
            return;
        }
        charted.putBoolean("t" + task.id(), true);
        Set<Integer> done = charted(charted);
        Msg.action(player, task.task() + " - charted (" + done.size() + "/" + SailingTraining.CHARTS.size() + ").");
        for (SailingTraining.ChartReward r : SailingTraining.chartRewards(task, done)) {
            XpService.add(player, Skill.SAILING, r.xp());
            if (!r.reason().endsWith(" charting")) {
                Msg.action(player, "You receive " + (int) r.xp() + " Sailing XP for " + r.reason() + ".");
            }
        }
    }

    // ------------------------------------------------------------------ port tasks (courier)

    private static SailingData.Port port(String id) {
        return SailingData.port(id);
    }

    /** Accepted courier tasks: ids, with a "got" flag once the cargo is collected. */
    private static NbtCompound tasks(ServerPlayerEntity player) {
        return SkillingState.get(player.getServer()).entry("courier_tasks", player.getUuidAsString());
    }

    private static void portBoard(ServerPlayerEntity player, SailingData.Port here) {
        if (!pandemoniumDone(player)) {
            Msg.game(player, "You need to complete Pandemonium before you can take port tasks.");
            return;
        }
        int level = SkillingUtil.level(player, Skill.SAILING);
        NbtCompound held = tasks(player);
        boolean acted = false;
        // Deliver, then collect, at this port's ledger.
        for (String key : new ArrayList<>(held.getKeys())) {
            SailingTraining.CourierTask t = SailingTraining.courier(Integer.parseInt(key));
            if (t == null) {
                held.remove(key);
                continue;
            }
            boolean got = held.getBoolean(key);
            if (got && t.destination().equals(here.id())) {
                if (!nearBoat(player)) {
                    Msg.game(player, "Bring your boat to the dock to unload the " + SkillingUtil.lower(t.cargoName()) + ".");
                    acted = true;
                    continue;
                }
                if (Inv.count(player, "cargo_crate") < t.qty()) {
                    Msg.game(player, "You need " + t.qty() + " cargo crates for this delivery (" + t.cargoName() + ").");
                    acted = true;
                    continue;
                }
                Inv.remove(player, "cargo_crate", t.qty());
                held.remove(key);
                double xp = SailingTraining.courierXp(t);
                XpService.add(player, Skill.SAILING, xp);
                Inv.give(player, "coins", SailingTraining.courierCoins(t));
                Msg.action(player, "You deliver the " + SkillingUtil.lower(t.cargoName()) + " to " + here.name() + " for " + (int) xp
                        + " Sailing XP and " + SailingTraining.courierCoins(t) + " coins.");
                acted = true;
            } else if (!got && t.cargo().equals(here.id())) {
                Inv.give(player, "cargo_crate", t.qty());
                held.putBoolean(key, true);
                Msg.action(player, "You collect the " + SkillingUtil.lower(t.cargoName()) + " (" + t.qty() + " crate" + (t.qty() == 1 ? ""
                        : "s") + ") for " + port(t.destination()).name() + ".");
                acted = true;
            }
        }
        if (acted) {
            return;
        }
        if (player.isSneaking()) {
            if (held.getKeys().isEmpty()) {
                Msg.game(player, "You have no port tasks to cancel.");
                return;
            }
            String key = held.getKeys().iterator().next();
            SailingTraining.CourierTask t = SailingTraining.courier(Integer.parseInt(key));
            if (held.getBoolean(key) && t != null) {
                Inv.remove(player, "cargo_crate", t.qty());
            }
            held.remove(key);
            Msg.game(player, "You cancel a courier task.");
            return;
        }
        for (String key : held.getKeys()) {
            SailingTraining.CourierTask t = SailingTraining.courier(Integer.parseInt(key));
            if (t != null) {
                Msg.game(player, "Task: " + t.cargoName() + " from " + port(t.cargo()).name() + " to " + port(t.destination()).name()
                        + (held.getBoolean(key) ? " (collected)" : ""));
            }
        }
        if (level < here.level()) {
            Msg.game(player, "You need a Sailing level of " + here.level() + " to take tasks from " + here.name() + ".");
            return;
        }
        if (held.getKeys().size() >= SailingTraining.taskSlots(level)) {
            Msg.game(player, "You can't take on more than " + SailingTraining.taskSlots(level) + " port tasks at your level. "
                    + "(Sneak to cancel one.)");
            return;
        }
        List<SailingTraining.CourierTask> options = new ArrayList<>();
        for (SailingTraining.CourierTask t : SailingTraining.boardTasks(here.id(), level)) {
            if (!held.contains(String.valueOf(t.id()))) {
                options.add(t);
            }
        }
        if (options.isEmpty()) {
            Msg.game(player, "There are no tasks on this notice board you can take yet.");
            return;
        }
        SailingTraining.CourierTask t = options.get(player.getRandom().nextInt(options.size()));
        held.putBoolean(String.valueOf(t.id()), false);
        Msg.action(player, "You take a courier task: " + t.cargoName() + " (" + t.qty() + ") from " + port(t.cargo()).name() + " to "
                + port(t.destination()).name() + ".");
        if (t.cargo().equals(here.id())) {
            portBoard(player, here);
        }
    }

    // ------------------------------------------------------------------ boat parts and facilities

    /** The Sailing boat type of a vehicle ("raft", "skiff", "sloop"), or null. */
    static String boatType(Entity vehicle) {
        if (vehicle instanceof com.runecraft.content.extras.SailBoatEntity sail) {
            return sail.tier().id();
        }
        SailingData.Boat kit = boatOf(vehicle);
        return kit == null ? null : kit.type();
    }

    /** The boat a player is on or standing next to. */
    private static Entity nearbyBoat(ServerPlayerEntity player) {
        if (boatType(player.getVehicle()) != null) {
            return player.getVehicle();
        }
        for (BoatEntity b : player.getWorld().getEntitiesByClass(BoatEntity.class, player.getBoundingBox().expand(6), e -> true)) {
            if (boatType(b) != null) {
                return b;
            }
        }
        return null;
    }

    private static NbtCompound boatState(ServerPlayerEntity player) {
        return SkillingState.get(player.getServer()).entry("boats", player.getUuidAsString());
    }

    static int partTier(ServerPlayerEntity player, String boat, SailingTraining.PartType type) {
        return boatState(player).getInt(boat + "." + type.name());
    }

    static boolean hasFacility(ServerPlayerEntity player, String boat, String facility) {
        return boatState(player).getBoolean(boat + ".f." + facility);
    }

    private static Map<SailingTraining.PartType, Integer> tiers(ServerPlayerEntity player, String boat) {
        Map<SailingTraining.PartType, Integer> m = new HashMap<>();
        for (SailingTraining.PartType t : SailingTraining.PartType.values()) {
            m.put(t, partTier(player, boat, t));
        }
        return m;
    }

    private static Set<String> facilities(ServerPlayerEntity player, String boat) {
        Set<String> s = new HashSet<>();
        for (SailingTraining.Facility f : SailingTraining.FACILITIES) {
            if (hasFacility(player, boat, f.id())) {
                s.add(f.id());
            }
        }
        return s;
    }

    private static String materials(Map<String, Integer> mats) {
        StringBuilder sb = new StringBuilder();
        mats.forEach((item, n) -> sb.append(sb.length() == 0 ? "" : ", ").append(n).append(" x ").append(SkillingUtil.name(item)));
        return sb.toString();
    }

    private static boolean hasAll(ServerPlayerEntity player, Map<String, Integer> mats) {
        for (Map.Entry<String, Integer> e : mats.entrySet()) {
            if (!Inv.has(player, e.getKey(), e.getValue())) {
                return false;
            }
        }
        return true;
    }

    /** Boat schematics: click to cycle through the next upgrades for your boat, sneak-click to build the one shown. */
    private static void schematics(ServerPlayerEntity player) {
        Entity boat = nearbyBoat(player);
        String type = boatType(boat);
        if (type == null) {
            Msg.game(player, "Moor your boat next to the boat schematics to upgrade it.");
            return;
        }
        List<Object> options = new ArrayList<>();
        for (SailingTraining.PartType t : SailingTraining.PartType.values()) {
            SailingTraining.Part next = SailingTraining.part(type, t, partTier(player, type, t) + 1);
            if (next != null) {
                options.add(next);
            }
        }
        for (SailingTraining.Facility f : SailingTraining.FACILITIES) {
            if (!hasFacility(player, type, f.id()) && !type.equals("raft")) {
                options.add(f);
            }
        }
        if (options.isEmpty()) {
            Msg.game(player, "Your " + type + " is fully upgraded.");
            return;
        }
        NbtCompound state = boatState(player);
        int sel = Math.floorMod(state.getInt("sel"), options.size());
        if (!player.isSneaking()) {
            sel = (sel + 1) % options.size();
            state.putInt("sel", sel);
        }
        Object o = options.get(sel);
        String name = o instanceof SailingTraining.Part p ? p.name() : ((SailingTraining.Facility) o).name();
        int sailing = o instanceof SailingTraining.Part p ? p.sailing() : ((SailingTraining.Facility) o).sailing();
        int con = o instanceof SailingTraining.Part p ? p.construction() : ((SailingTraining.Facility) o).construction();
        double xp = o instanceof SailingTraining.Part p ? p.conXp() : ((SailingTraining.Facility) o).conXp();
        Map<String, Integer> mats = o instanceof SailingTraining.Part p ? p.materials() : ((SailingTraining.Facility) o).materials();
        if (!player.isSneaking()) {
            Msg.game(player, "[" + (sel + 1) + "/" + options.size() + "] " + name + " for your " + type + ": Sailing " + sailing
                    + ", Construction " + con + ". Needs " + materials(mats) + " and a hammer. Sneak and use the schematics to build it.");
            return;
        }
        if (o instanceof SailingTraining.Part p) {
            String why = SailingTraining.canBuild(p, SkillingUtil.level(player, Skill.SAILING), SkillingUtil.level(player, Skill.CONSTRUCTION),
                    partTier(player, type, p.type()));
            if (why != null) {
                Msg.game(player, why);
                return;
            }
        } else if (!SkillingUtil.need(player, Skill.SAILING, sailing, "build this") || !SkillingUtil.need(player, Skill.CONSTRUCTION, con,
                "build this")) {
            return;
        }
        if (!SkillingUtil.needItem(player, "hammer", "You need a hammer to build boat parts.")) {
            return;
        }
        if (!hasAll(player, mats)) {
            Msg.game(player, "You need " + materials(mats) + ".");
            return;
        }
        mats.forEach((item, n) -> Inv.remove(player, item, n));
        if (o instanceof SailingTraining.Part p) {
            state.putInt(type + "." + p.type().name(), p.tier());
        } else {
            state.putBoolean(type + ".f." + ((SailingTraining.Facility) o).id(), true);
        }
        XpService.add(player, Skill.CONSTRUCTION, xp);
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_ANVIL_USE, SoundCategory.PLAYERS, 0.6f, 1.2f);
        Msg.action(player, "You build " + Msg.article(SkillingUtil.lower(name)) + " on your " + type + ".");
    }

    // ------------------------------------------------------------------ Barracuda Trials

    /** A trial in progress: which trial and rank, start tick, crates collected, objectives done, ticks outside the area. */
    private static final class TrialRun {
        final SailingTraining.Trial trial;
        final int rank;
        final long start;
        int crates;
        int objectives;
        int outside;

        TrialRun(SailingTraining.Trial trial, int rank, long start) {
            this.trial = trial;
            this.rank = rank;
            this.start = start;
        }

        SailingTraining.Rank r() {
            return trial.ranks().get(rank);
        }

        int[] nextCrate() {
            return trial.crates().get(crates % trial.crates().size());
        }

        int[] nextObjective() {
            return objectives % 2 == 0 ? trial.objectiveA() : trial.objectiveB();
        }
    }

    private static final Map<UUID, TrialRun> TRIALS = new HashMap<>();

    private static void trialBuoy(ServerPlayerEntity player, BlockPos pos) {
        int x = GielinorMap.toOsrsX(pos.getX());
        int y = GielinorMap.toOsrsY(pos.getZ());
        SailingTraining.Trial trial = null;
        for (SailingTraining.Trial t : SailingTraining.TRIALS) {
            if (Math.abs(t.objectiveB()[0] - x) < 20 && Math.abs(t.objectiveB()[1] - y) < 20) {
                trial = t;
            }
        }
        if (trial == null) {
            return;
        }
        if (TRIALS.containsKey(player.getUuid())) {
            TRIALS.remove(player.getUuid());
            Msg.game(player, "You abandon the trial.");
            return;
        }
        NbtCompound best = SkillingState.get(player.getServer()).entry("trials", player.getUuidAsString());
        int rank = Math.floorMod(best.getInt(trial.id() + ".pick"), 3);
        if (!player.isSneaking()) {
            rank = (rank + 1) % 3;
            best.putInt(trial.id() + ".pick", rank);
            SailingTraining.Rank r = trial.ranks().get(rank);
            Msg.game(player, trial.name() + " - " + r.name() + " rank: " + r.crates() + " lost supplies" + (r.objectives() > 0 ? ", "
                    + r.objectives() + " deliveries" : "") + ", target " + r.targetSeconds() + "s, " + (int) r.xp()
                    + " Sailing XP. Sneak and use the buoy from your skiff to start.");
            return;
        }
        Entity boat = player.getVehicle();
        String type = boatType(boat);
        String why = SailingTraining.trialBlocker(trial, SkillingUtil.level(player, Skill.SAILING), type,
                type == null ? Map.of() : tiers(player, type), type == null ? Set.of() : facilities(player, type),
                trial.quest() == null || player.getAbilities().creativeMode || QuestApi.isComplete(player, trial.quest()));
        if (why != null) {
            Msg.game(player, why);
            return;
        }
        TrialRun run = new TrialRun(trial, rank, ActionManager.gameTick());
        TRIALS.put(player.getUuid(), run);
        Msg.action(player, trial.name() + " (" + run.r().name() + ") begins! Collect the lost supplies marked by glowing water.");
    }

    private static void trialTick(ServerPlayerEntity player) {
        TrialRun run = TRIALS.get(player.getUuid());
        if (run == null) {
            return;
        }
        ServerWorld world = (ServerWorld) player.getWorld();
        int x = GielinorMap.toOsrsX(player.getBlockX());
        int y = GielinorMap.toOsrsY(player.getBlockZ());
        boolean onSkiff = "skiff".equals(boatType(player.getVehicle()));
        if (!onSkiff || Math.max(Math.abs(x - run.trial.x()), Math.abs(y - run.trial.y())) > 110) {
            if (++run.outside > 17) { // 10 seconds outside the trial area (wiki)
                TRIALS.remove(player.getUuid());
                Msg.game(player, "You left the trial area and your progress was lost.");
            }
            return;
        }
        run.outside = 0;
        SailingTraining.Rank r = run.r();
        int[] target = run.crates < r.crates() ? run.nextCrate() : run.objectives < r.objectives() ? run.nextObjective() : null;
        if (target == null) {
            finishTrial(player, run);
            return;
        }
        int bx = GielinorMap.toBlockX(target[0]);
        int bz = GielinorMap.toBlockZ(target[1]);
        world.spawnParticles(ParticleTypes.GLOW, bx + 1, world.getSeaLevel() + 1, bz + 1, 6, 0.6, 0.4, 0.6, 0.02);
        if (Math.abs(player.getX() - bx - 1) <= 4 && Math.abs(player.getZ() - bz - 1) <= 4) {
            if (run.crates < r.crates()) {
                run.crates++;
                Msg.action(player, "You pick up some lost supplies (" + run.crates + "/" + r.crates() + ").");
            } else {
                run.objectives++;
                Msg.action(player, (run.objectives % 2 == 1 ? "You load the shipment" : "You make the delivery") + " ("
                        + run.objectives + "/" + r.objectives() + ").");
            }
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.6f, 1.2f);
        }
    }

    private static void finishTrial(ServerPlayerEntity player, TrialRun run) {
        TRIALS.remove(player.getUuid());
        SailingTraining.Rank r = run.r();
        double seconds = (ActionManager.gameTick() - run.start) * 0.6;
        XpService.add(player, Skill.SAILING, r.xp());
        NbtCompound best = SkillingState.get(player.getServer()).entry("trials", player.getUuidAsString());
        String key = run.trial.id() + "." + r.name();
        boolean target = seconds <= r.targetSeconds();
        Msg.action(player, "You complete " + run.trial.name() + " (" + r.name() + ") in " + (int) seconds + "s"
                + (target ? ", beating the target time!" : " (target " + r.targetSeconds() + "s)."));
        if (target && !best.getBoolean(key)) {
            best.putBoolean(key, true);
            if (run.trial.id().equals("gwenith_glide") && r.name().equals("Shark")) {
                SkillingUtil.give(player, "heart_of_ithell", 1);
                Msg.action(player, "Gwyna hands you the Heart of Ithell.");
            }
        }
    }

    // ------------------------------------------------------------------ trawling

    private static final List<CookingData.NewFish> TRAWL = new ArrayList<>();

    static {
        for (CookingData.NewFish f : CookingData.FISH) {
            if (List.of("giant_krill", "haddock", "yellowfin", "halibut", "bluefin", "marlin").contains(f.id())) {
                TRAWL.add(0, f);   // best first
            }
        }
    }

    /** The best trawling net built on a boat, or null. */
    private static SailingTraining.Net net(ServerPlayerEntity player, String boat) {
        SailingTraining.Net best = null;
        for (SailingTraining.Net n : SailingTraining.NETS) {
            if (boat != null && hasFacility(player, boat, n.facility())) {
                best = n;
            }
        }
        return best;
    }

    static void trawl(ServerPlayerEntity player) {
        String boat = boatType(player.getVehicle());
        if (!(player.getVehicle() instanceof BoatEntity)) {
            Msg.game(player, "You need to be sailing a boat to cast a trawling net.");
            return;
        }
        SailingTraining.Net net = net(player, boat);
        if (net == null) {
            Msg.game(player, "Your boat needs a trawling net facility (built at boat schematics, level 56 Sailing) to trawl.");
            return;
        }
        if (SkillingUtil.level(player, Skill.FISHING) < TRAWL.get(TRAWL.size() - 1).fishLevel()) {
            Msg.game(player, "You need a Fishing level of " + TRAWL.get(TRAWL.size() - 1).fishLevel() + " to trawl.");
            return;
        }
        Msg.action(player, "You lower your " + SkillingUtil.lower(SailingTraining.facility(net.facility()).name()) + " into the sea.");
        ActionManager.start(player, new Action() {
            int counter;

            @Override
            public boolean tick(ServerPlayerEntity p, long gameTick) {
                if (!(p.getVehicle() instanceof BoatEntity) || !Inv.has(p, "trawling_net", 1)) {
                    Msg.game(p, "You haul in your trawling net.");
                    return false;
                }
                if (++counter < SailingTraining.TRAWL_ROLL_TICKS) {
                    return true;
                }
                counter = 0;
                if (!p.getWorld().getFluidState(p.getVehicle().getBlockPos()).isIn(FluidTags.WATER)
                        && !p.getWorld().getFluidState(p.getVehicle().getBlockPos().down()).isIn(FluidTags.WATER)) {
                    return true;
                }
                // Every roll gives a token of Sailing XP; a successful roll catches fish for Fishing XP (wiki).
                XpService.add(p, Skill.SAILING, net.xpPerRoll());
                int fishing = SkillingUtil.level(p, Skill.FISHING);
                Random random = p.getRandom();
                for (CookingData.NewFish f : TRAWL) {
                    if (fishing >= f.fishLevel() && SailingTraining.shoalDepth(f.id()) <= net.depth()
                            && random.nextDouble() < Chance.lerp(fishing, 12, 48) / 2) { // approx catch rate
                        int n = 1 + random.nextInt(net.maxFish());
                        SkillingUtil.give(p, f.raw(), n);
                        XpService.add(p, Skill.FISHING, f.fishXp() * n);
                        Msg.action(p, "Your net brings up " + n + " x " + SkillingUtil.lower(f.name()) + ".");
                        return true;
                    }
                }
                return true;
            }
        }, true);
    }
}
