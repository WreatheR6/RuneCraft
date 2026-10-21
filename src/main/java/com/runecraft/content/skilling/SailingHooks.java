package com.runecraft.content.skilling;

import com.runecraft.action.Action;
import com.runecraft.action.ActionManager;
import com.runecraft.content.BlockHooks;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.skilling.CookingData;
import com.runecraft.osrs.content.skilling.SailingCrew;
import com.runecraft.osrs.content.skilling.SailingData;
import com.runecraft.osrs.content.skilling.SailingShoals;
import com.runecraft.osrs.content.skilling.SailingTraining;
import com.runecraft.quest.QuestApi;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.LandmarkBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

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
            int extra = SailingData.EXTRA_WRECK_SITES[SailingData.WRECKS.indexOf(w)].length / 2;
            for (int n = 2; n < extra + 2; n++) {
                LandmarkBuilder.registerCustom(w.blockId() + "_site_" + n, (world, centre) -> placeAtSea(world, centre, w.blockId(), 0));
            }
        }
        BlockHooks.register("sea_chart_marker", (player, pos, state) -> chart(player, pos));
        SeaHooks.init();
        SailingLockboxHooks.init();
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
                // Most OSRS ports have a salvaging station on the dock.
                placeAtSea(world, centre, "port_salvaging_station", -2);
                if (shipwright) {
                    placeAtSea(world, centre, "boat_schematics", 2);
                }
            });
        }
        // Trawling shoals: a marker on the water at each wiki map pin; clicking it from your boat lowers the nets.
        BlockHooks.register(SailingData.TRAWLING_SHOAL, (player, pos, state) -> {
            SailingShoals.Shoal s = shoalNear(pos, 3);
            if (s != null && boatType(player.getVehicle()) != null && shoalNear(player.blockPosition(), SailingShoals.RANGE) == s) {
                startTrawl(player, null);
            } else if (s != null) {
                Msg.game(player, "A " + SkillingUtil.lower(s.name()) + " (Fishing " + s.level() + "). Sail your boat over it and lower a"
                        + " trawling net.");
            }
        });
        for (SailingShoals.Shoal s : SailingShoals.SHOALS) {
            for (int i = 0; i < s.pins().size(); i++) {
                LandmarkBuilder.registerCustom(SailingData.shoalPlace(s, i), (world, centre) -> placeAtSea(world, centre,
                        SailingData.TRAWLING_SHOAL, 0));
            }
        }
        LandmarkBuilder.registerCustom("fishing_platform_deep_sea", (world, centre) -> {
            placeAtSea(world, centre, "deep_sea_fishing_spot", 0);
            placeAtSea(world, centre, "deep_sea_fishing_spot", 4);
        });
    }

    /** Ports with a shipyard (boat schematics): the shipwright ports of the extras module. */
    private static final Set<String> SHIPWRIGHT_PORTS = Set.of("port_sarim", "rellekka", "port_piscarilius", "aldarin");

    /** Puts a module block on the water surface (or on the ground if the spot turned out to be land). */
    static void placeAtSea(ServerLevel world, BlockPos centre, String blockId, int dx) {
        BlockState block = SkillingUtil.moduleBlock(blockId);
        if (block == null) {
            return;
        }
        BlockPos at = centre.offset(dx, 0, 0);
        BlockState here = world.getBlockState(at);
        if (here.getFluidState().is(FluidTags.WATER) || here.canBeReplaced()) {
            world.setBlock(at, block, Block.UPDATE_CLIENTS);
        } else {
            world.setBlock(at.above(), block, Block.UPDATE_CLIENTS);
        }
    }

    // ------------------------------------------------------------------ boats

    static void launch(ServerPlayer player, ItemStack stack, SailingData.Boat boat) {
        String why = SailingData.canSail(pandemoniumDone(player), SkillingUtil.level(player, Skill.SAILING), boat);
        if (why != null) {
            Msg.game(player, why);
            return;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(5.0));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.SOURCE_ONLY, player));
        if (hit.getType() != HitResult.Type.BLOCK || !player.level().getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) {
            Msg.game(player, "Use the kit while looking at water to launch your " + SkillingUtil.lower(boat.name()) + ".");
            return;
        }
        launchAt(player, stack, boat, hit.getLocation());
    }

    /** Launches the kit's boat at a spot on the water (levels already checked); used by the self-test too. */
    public static Boat launchAt(ServerPlayer player, ItemStack stack, SailingData.Boat boat, Vec3 p) {
        Boat entity = new Boat(player.level(), p.x, p.y, p.z);
        entity.setVariant(Boat.Type.valueOf(boat.variant()));
        entity.setYRot(player.getYRot());
        entity.addTag(BOAT_TAG + boat.kit());
        entity.setCustomName(net.minecraft.network.chat.Component.literal(player.getName().getString() + "'s " + SkillingUtil.lower(boat.name())));
        SeaHooks.applyTrim(player, entity, boat.type());
        player.level().addFreshEntity(entity);
        stack.shrink(1);
        player.level().playSound(null, entity.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1f, 1f);
        Msg.action(player, "You launch your " + SkillingUtil.lower(boat.name()) + ".");
        return entity;
    }

    /** Sailing is taught by the Pandemonium quest (creative players skip it). */
    static boolean pandemoniumDone(ServerPlayer player) {
        return player.getAbilities().instabuild || QuestApi.isComplete(player, SailingData.SAILING_QUEST);
    }

    static SailingData.Boat boatOf(Entity vehicle) {
        if (!(vehicle instanceof Boat)) {
            return null;
        }
        for (String tag : vehicle.getTags()) {
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
    static void boatTick(ServerPlayer player) {
        Entity vehicle = player.getVehicle();
        SailingData.Boat boat = boatOf(vehicle);
        if (boat != null && SkillingUtil.level(player, Skill.SAILING) < boat.level()) {
            player.stopRiding();
            Msg.game(player, "You need a Sailing level of " + boat.level() + " to sail this " + SkillingUtil.lower(boat.name()) + ".");
            return;
        }
        String type = boatType(vehicle);
        if (type == null) {
            TICKS.remove(player.getUUID());
            SeaHooks.offBoat(player);
            if (TRIALS.containsKey(player.getUUID())) {
                trialTick(player);
            }
            return;
        }
        int[] t = TICKS.computeIfAbsent(player.getUUID(), k -> new int[2]);
        Vec3 v = vehicle.getDeltaMovement();
        boolean moving = v.x * v.x + v.z * v.z > 0.01;
        if (moving && ++t[0] >= SailingTraining.TRIM_TICKS) {
            t[0] = 0;
            trimSails(player, type);
        } else if (moving && t[0] == SailingTraining.TRIM_TICKS / 2) {
            releaseMote(player, type); // playability: OSRS releases a stored mote from the catcher's menu
        }
        if (++t[1] >= SailingTraining.EXTRACTOR_TICKS && hasFacility(player, type, "crystal_extractor")) {
            t[1] = 0;
            XpService.add(player, Skill.SAILING, SailingTraining.EXTRACTOR_XP);
            Msg.action(player, "You harvest a wind mote from the crystal extractor.");
            storeMote(player, type, true);
        }
        SeaHooks.boatTick(player, vehicle, type);
        if (TRIALS.containsKey(player.getUUID())) {
            trialTick(player);
        }
    }

    /** Clears a player's in-memory sailing state: trim and extractor timers, stored wind motes, a trial run (self-test). */
    static void resetForTest(ServerPlayer player) {
        TICKS.remove(player.getUUID());
        MOTES.remove(player.getUUID());
        TRIALS.remove(player.getUUID());
    }

    /** Wind motes stored in a boat's catcher: {from trimming, from the crystal extractor}. */
    private static final Map<UUID, int[]> MOTES = new HashMap<>();

    /** The boat's wind or gale catcher capacity (0: none). */
    static int catcherMotes(ServerPlayer player, String type) {
        return hasFacility(player, type, "gale_catcher") ? SailingTraining.GALE_CATCHER_MOTES
                : hasFacility(player, type, "wind_catcher") ? SailingTraining.WIND_CATCHER_MOTES : 0;
    }

    /**
     * Trims the sails: the sails' trimming XP (wiki Mast and sails), 25% less with a wind or gale catcher, which stores a
     * wind mote. Returns the XP given. Used every 30 seconds under way and by the self-test.
     */
    public static double trimSails(ServerPlayer player, String type) {
        double xp = SailingTraining.trimXp(type, partTier(player, type, SailingTraining.PartType.SAILS));
        if (catcherMotes(player, type) > 0) {
            xp *= SailingTraining.CATCHER_TRIM_FACTOR;
            storeMote(player, type, false);
        }
        XpService.add(player, Skill.SAILING, xp);
        Msg.action(player, "A gust of wind catches your sails and you trim them.");
        return xp;
    }

    private static void storeMote(ServerPlayer player, String type, boolean extractor) {
        int cap = catcherMotes(player, type);
        int[] m = MOTES.computeIfAbsent(player.getUUID(), k -> new int[2]);
        if (cap > 0 && m[0] + m[1] < cap) {
            m[extractor ? 1 : 0]++;
        }
    }

    /** Wind motes stored in the player's catcher (self-test). */
    public static int storedMotes(ServerPlayer player) {
        int[] m = MOTES.get(player.getUUID());
        return m == null ? 0 : m[0] + m[1];
    }

    /**
     * Releases a stored wind mote for a burst of speed: 40 Sailing XP (70 with a gale catcher), or 10 for a mote from the
     * crystal extractor (wiki). Returns the XP given (0: no mote stored).
     */
    public static double releaseMote(ServerPlayer player, String type) {
        int[] m = MOTES.get(player.getUUID());
        if (m == null || m[0] + m[1] == 0 || catcherMotes(player, type) == 0) {
            return 0;
        }
        boolean extractor = m[0] == 0;
        m[extractor ? 1 : 0]--;
        double xp = extractor ? SailingTraining.EXTRACTOR_MOTE_XP : hasFacility(player, type, "gale_catcher") ? SailingTraining.GALE_MOTE_XP
                : SailingTraining.WIND_MOTE_XP;
        XpService.add(player, Skill.SAILING, xp);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5f, 0.6f);
        Msg.action(player, "You release a wind mote and your boat surges forward.");
        return xp;
    }

    /**
     * Raw food used aboard a boat with a range opens the cooking menu there (wiki Range (facility)). Only the dish the range
     * heats counts ({@link #cookedOnRange}); toppings and food that are also some recipe's ingredient (a tomato, cheese, a
     * banana) are still eaten or drunk. True when it opened the menu.
     */
    static boolean cookAboard(ServerPlayer player, String item) {
        String type = boatType(player.getVehicle());
        if (type == null || !hasFacility(player, type, "range") || !cookedOnRange(item)) {
            return false;
        }
        com.runecraft.menu.MenuService.open(player, com.runecraft.osrs.MenuType.COOKING, null, true);
        return true;
    }

    /**
     * Whether a range cooks this item: the dish put on the heat in a heat recipe, not a topping prepared into it by hand
     * (cheese on a batta) and not food or a potion ({@link com.runecraft.osrs.content.skilling.SailingTraining#cookedOnRange}).
     */
    static boolean cookedOnRange(String item) {
        return com.runecraft.osrs.content.skilling.SailingTraining.cookedOnRange(item);
    }

    static boolean nearBoat(ServerPlayer player) {
        return player.getVehicle() instanceof Boat
                || !player.level().getEntitiesOfClass(Boat.class, player.getBoundingBox().inflate(6), b -> true).isEmpty();
    }

    // ------------------------------------------------------------------ salvaging

    static void salvage(ServerPlayer player, BlockPos pos, SailingData.Wreck w) {
        if (!SkillingUtil.need(player, Skill.SAILING, w.level(), "salvage this shipwreck")) {
            return;
        }
        // A salvaging hook built on the boat (best tier counts) or the hand-held hook from Pandemonium (bronze).
        Entity boat = nearbyBoat(player);
        String boatType = boatType(boat);
        int hookTier = boatType == null ? -1 : com.runecraft.osrs.content.skilling.SailingSea.hookTier(facilities(player, boatType));
        if (hookTier < 0 && !SkillingUtil.needItem(player, "salvaging_hook", "You need a salvaging hook on your boat to salvage shipwrecks.")) {
            return;
        }
        int tier = Math.max(0, hookTier);
        ServerLevel world = (ServerLevel) player.level();
        if (GatheringHooks.depleted(world, pos)) {
            Msg.game(player, "There's nothing left to salvage here for now.");
            return;
        }
        if (!nearBoat(player)) {
            Msg.game(player, "You need to be on (or next to) a boat to salvage shipwrecks.");
            return;
        }
        Msg.action(player, "You lower your salvaging hook into the wreck.");
        ActionManager.start(player, new SkillingUtil.BlockAction(pos, w.blockId(), com.runecraft.osrs.content.skilling.SailingSea.SALVAGE_TICKS) {
            private int crewTicks;

            @Override
            boolean roll(ServerPlayer p, RandomSource random) {
                if ((hookTier < 0 && !Inv.has(p, "salvaging_hook", 1)) || GatheringHooks.depleted(world, pos)) {
                    return false;
                }
                world.playSound(null, pos, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.PLAYERS, 0.5f, 0.8f);
                double chance = com.runecraft.osrs.content.skilling.SailingSea.salvageChance(w, SkillingUtil.level(p, Skill.SAILING), tier);
                // The best deckhand aboard works a hook alongside you (approx: OSRS assigns crew to each hook, and they keep
                // salvaging while you do something else): every 5 ticks, the hook's low/high x Deckhandiness x 10%, for that
                // share of the XP (OSRS Wiki, Shipwreck salvaging and its calculator).
                crewTicks += com.runecraft.osrs.content.skilling.SailingSea.SALVAGE_TICKS;
                while (hookTier >= 0 && crewTicks >= SailingCrew.CREW_SALVAGE_TICKS) {
                    crewTicks -= SailingCrew.CREW_SALVAGE_TICKS;
                    SailingCrew.Crewmate hand = SailingCrew.salvager(
                            SailingCrew.aboard(SeaHooks.crewOrder(p), boatType, SkillingUtil.level(p, Skill.SAILING)), hookTier);
                    if (hand != null && random.nextDouble() < SailingCrew.crewSalvageChance(w, SkillingUtil.level(p, Skill.SAILING), hookTier,
                            hand.deckhandiness())) {
                        SkillingUtil.give(p, w.salvage(), 1);
                        XpService.add(p, Skill.SAILING, w.xp() * SailingCrew.salvageShare(hand.deckhandiness()));
                        Msg.action(p, hand.name() + " hauls up some salvage.");
                    }
                }
                if (random.nextDouble() < chance) {
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

    private static Set<Integer> charted(CompoundTag charted) {
        Set<Integer> done = new HashSet<>();
        for (String k : charted.getAllKeys()) {
            if (k.startsWith("t") && charted.getBoolean(k)) {
                done.add(Integer.parseInt(k.substring(1)));
            }
        }
        return done;
    }

    private static void chart(ServerPlayer player, BlockPos pos) {
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
        CompoundTag charted = SkillingState.get(player.getServer()).entry("charted", player.getStringUUID());
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
    private static CompoundTag tasks(ServerPlayer player) {
        return SkillingState.get(player.getServer()).entry("courier_tasks", player.getStringUUID());
    }

    static void portBoard(ServerPlayer player, SailingData.Port here) {
        if (!pandemoniumDone(player)) {
            Msg.game(player, "You need to complete Pandemonium before you can take port tasks.");
            return;
        }
        int level = SkillingUtil.level(player, Skill.SAILING);
        CompoundTag held = tasks(player);
        boolean acted = false;
        // Deliver, then collect, at this port's ledger.
        for (String key : new ArrayList<>(held.getAllKeys())) {
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
                if (player.getRandom().nextInt(com.runecraft.osrs.content.skilling.SailingCrew.SHARK_PAINT_RATE) == 0) {
                    SkillingUtil.give(player, "shark_paint", 1); // wiki: 1/36 per port task
                    Msg.action(player, "You've received some paint!");
                }
                acted = true;
            } else if (!got && t.cargo().equals(here.id())) {
                Inv.give(player, "cargo_crate", t.qty());
                held.putBoolean(key, true);
                Msg.action(player, "You collect the " + SkillingUtil.lower(t.cargoName()) + " (" + t.qty() + " crate" + (t.qty() == 1 ? ""
                        : "s") + ") for " + port(t.destination()).name() + ".");
                acted = true;
            }
        }
        if (acted || SeaHooks.portBoard(player, here, held.getAllKeys().size())) {
            return;
        }
        if (player.isShiftKeyDown()) {
            if (held.getAllKeys().isEmpty()) {
                Msg.game(player, "You have no port tasks to cancel.");
                return;
            }
            String key = held.getAllKeys().iterator().next();
            SailingTraining.CourierTask t = SailingTraining.courier(Integer.parseInt(key));
            if (held.getBoolean(key) && t != null) {
                Inv.remove(player, "cargo_crate", t.qty());
            }
            held.remove(key);
            Msg.game(player, "You cancel a courier task.");
            return;
        }
        for (String key : held.getAllKeys()) {
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
        if (held.getAllKeys().size() >= SailingTraining.taskSlots(level)) {
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
    static Entity nearbyBoat(ServerPlayer player) {
        if (boatType(player.getVehicle()) != null) {
            return player.getVehicle();
        }
        for (Boat b : player.level().getEntitiesOfClass(Boat.class, player.getBoundingBox().inflate(6), e -> true)) {
            if (boatType(b) != null) {
                return b;
            }
        }
        return null;
    }

    /** Every boat facility: the round-8 ones, salvaging hooks, cannons and cargo holds. */
    static List<SailingTraining.Facility> allFacilities() {
        return com.runecraft.osrs.content.skilling.SailingCrew.allFacilities();
    }

    static CompoundTag boatState(ServerPlayer player) {
        return SkillingState.get(player.getServer()).entry("boats", player.getStringUUID());
    }

    static int partTier(ServerPlayer player, String boat, SailingTraining.PartType type) {
        return boatState(player).getInt(boat + "." + type.name());
    }

    static boolean hasFacility(ServerPlayer player, String boat, String facility) {
        return boatState(player).getBoolean(boat + ".f." + facility);
    }

    static Map<SailingTraining.PartType, Integer> tiers(ServerPlayer player, String boat) {
        Map<SailingTraining.PartType, Integer> m = new HashMap<>();
        for (SailingTraining.PartType t : SailingTraining.PartType.values()) {
            m.put(t, partTier(player, boat, t));
        }
        return m;
    }

    static Set<String> facilities(ServerPlayer player, String boat) {
        Set<String> s = new HashSet<>();
        for (SailingTraining.Facility f : allFacilities()) {
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

    private static boolean hasAll(ServerPlayer player, Map<String, Integer> mats) {
        for (Map.Entry<String, Integer> e : mats.entrySet()) {
            if (available(player, e.getKey()) < e.getValue()) {
                return false;
            }
        }
        return true;
    }

    /**
     * A building material in the pack plus the bank (playability: OSRS carries 10,000 air runes or 1,000 fishing bait in
     * one stack; a Minecraft stack holds 64, so the shipwright also draws what the pack can't carry from the bank).
     */
    static long available(ServerPlayer player, String item) {
        return Inv.count(player, item) + com.runecraft.player.Players.data(player).bank().count(item, "");
    }

    /** Uses up a building material: the pack first, then the bank. */
    static void useMaterial(ServerPlayer player, String item, int n) {
        int got = Inv.remove(player, item, n);
        if (got < n) {
            com.runecraft.player.Players.data(player).bank().withdraw(item, "", n - got);
        }
    }

    /** Boat schematics: click to cycle through the next upgrades for your boat, sneak-click to build the one shown. */
    private static void schematics(ServerPlayer player) {
        if (SeaHooks.recoverWrecks(player)) {
            return;
        }
        Entity boat = nearbyBoat(player);
        String type = boatType(boat);
        if (type == null) {
            Msg.game(player, "Moor your boat next to the boat schematics to upgrade it.");
            return;
        }
        List<Object> options = upgradeOptions(player, type);
        if (options.isEmpty()) {
            Msg.game(player, "Your " + type + " is fully upgraded.");
            return;
        }
        CompoundTag state = boatState(player);
        int sel = Math.floorMod(state.getInt("sel"), options.size());
        if (!player.isShiftKeyDown()) {
            sel = (sel + 1) % options.size();
            state.putInt("sel", sel);
        }
        Object o = options.get(sel);
        if (!player.isShiftKeyDown()) {
            String tool = o instanceof SailingTraining.Facility f ? SailingTraining.FACILITY_TOOLS.get(f.id()) : null;
            Msg.game(player, "[" + (sel + 1) + "/" + options.size() + "] " + upgradeName(o) + " for your " + type + ": Sailing "
                    + (o instanceof SailingTraining.Part p ? p.sailing() : ((SailingTraining.Facility) o).sailing()) + ", Construction "
                    + (o instanceof SailingTraining.Part p ? p.construction() : ((SailingTraining.Facility) o).construction()) + ". Needs "
                    + materials(upgradeMaterials(o)) + " and a hammer" + (tool == null ? "" : " (and " + Msg.article(SkillingUtil.lower(
                    SkillingUtil.name(tool))) + ")") + ". Sneak and use the schematics to build it.");
            return;
        }
        buildUpgrade(player, type, o);
    }

    /** The next tier of each core part and every facility not yet built (a raft only takes salvaging hooks here). */
    static List<Object> upgradeOptions(ServerPlayer player, String type) {
        List<Object> options = new ArrayList<>();
        for (SailingTraining.PartType t : SailingTraining.PartType.values()) {
            SailingTraining.Part next = SailingTraining.part(type, t, partTier(player, type, t) + 1);
            if (next != null) {
                options.add(next);
            }
        }
        for (SailingTraining.Facility f : allFacilities()) {
            if (!hasFacility(player, type, f.id()) && (!type.equals("raft") || f.id().endsWith("_salvaging_hook"))) {
                options.add(f);
            }
        }
        return options;
    }

    private static String upgradeName(Object o) {
        return o instanceof SailingTraining.Part p ? p.name() : ((SailingTraining.Facility) o).name();
    }

    private static Map<String, Integer> upgradeMaterials(Object o) {
        return o instanceof SailingTraining.Part p ? p.materials() : ((SailingTraining.Facility) o).materials();
    }

    /**
     * Builds a core part or facility on the boat type: the Sailing and Construction levels, its lost schematic, a hammer
     * (and the facility's tool, kept), then the materials for the wiki's Construction XP. True when it was built.
     */
    static boolean buildUpgrade(ServerPlayer player, String type, Object o) {
        int sailing = o instanceof SailingTraining.Part p ? p.sailing() : ((SailingTraining.Facility) o).sailing();
        int con = o instanceof SailingTraining.Part p ? p.construction() : ((SailingTraining.Facility) o).construction();
        double xp = o instanceof SailingTraining.Part p ? p.conXp() : ((SailingTraining.Facility) o).conXp();
        Map<String, Integer> mats = upgradeMaterials(o);
        if (o instanceof SailingTraining.Part p) {
            String why = SailingTraining.canBuild(p, SkillingUtil.level(player, Skill.SAILING), SkillingUtil.level(player, Skill.CONSTRUCTION),
                    partTier(player, type, p.type()));
            if (why != null) {
                Msg.game(player, why);
                return false;
            }
        } else if (!SkillingUtil.need(player, Skill.SAILING, sailing, "build this") || !SkillingUtil.need(player, Skill.CONSTRUCTION, con,
                "build this")) {
            return false;
        }
        String noSchematic = SailingLockboxHooks.missing(player, o);
        if (noSchematic != null) {
            Msg.game(player, noSchematic);
            return false;
        }
        if (!SkillingUtil.needItem(player, "hammer", "You need a hammer to build boat parts.")) {
            return false;
        }
        String tool = o instanceof SailingTraining.Facility f ? SailingTraining.FACILITY_TOOLS.get(f.id()) : null;
        if (tool != null && !SkillingUtil.needItem(player, tool, "You need " + Msg.article(SkillingUtil.lower(SkillingUtil.name(tool)))
                + " to build that.")) {
            return false;
        }
        if (!hasAll(player, mats)) {
            Msg.game(player, "You need " + materials(mats) + ".");
            return false;
        }
        mats.forEach((item, n) -> useMaterial(player, item, n));
        CompoundTag state = boatState(player);
        if (o instanceof SailingTraining.Part p) {
            state.putInt(type + "." + p.type().name(), p.tier());
        } else {
            state.putBoolean(type + ".f." + ((SailingTraining.Facility) o).id(), true);
        }
        XpService.add(player, Skill.CONSTRUCTION, xp);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.6f, 1.2f);
        Msg.action(player, "You build " + Msg.article(SkillingUtil.lower(upgradeName(o))) + " on your " + type + ".");
        return true;
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

    private static void trialBuoy(ServerPlayer player, BlockPos pos) {
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
        if (TRIALS.containsKey(player.getUUID())) {
            TRIALS.remove(player.getUUID());
            Msg.game(player, "You abandon the trial.");
            return;
        }
        reclaimTrialRewards(player, trial);
        CompoundTag best = SkillingState.get(player.getServer()).entry("trials", player.getStringUUID());
        int rank = Math.floorMod(best.getInt(trial.id() + ".pick"), 3);
        if (!player.isShiftKeyDown()) {
            rank = (rank + 1) % 3;
            best.putInt(trial.id() + ".pick", rank);
            SailingTraining.Rank r = trial.ranks().get(rank);
            Msg.game(player, trial.name() + " - " + r.name() + " rank: " + r.crates() + " lost supplies" + (r.objectives() > 0 ? ", "
                    + r.objectives() + " deliveries" : "") + ", target " + r.targetSeconds() + "s, " + (int) r.xp()
                    + " Sailing XP. Sneak and use the buoy from your skiff to start.");
            return;
        }
        if (!rankOpen(player, trial, rank)) {
            Msg.game(player, "You need to earn the " + trial.ranks().get(rank - 1).name() + " rank of " + trial.name() + " first.");
            return;
        }
        Entity boat = player.getVehicle();
        String type = boatType(boat);
        String why = SailingTraining.trialBlocker(trial, SkillingUtil.level(player, Skill.SAILING), type,
                type == null ? Map.of() : tiers(player, type), type == null ? Set.of() : facilities(player, type),
                trial.quest() == null || player.getAbilities().instabuild || QuestApi.isComplete(player, trial.quest()));
        if (why != null) {
            Msg.game(player, why);
            return;
        }
        TrialRun run = new TrialRun(trial, rank, ActionManager.gameTick());
        TRIALS.put(player.getUUID(), run);
        Msg.action(player, trial.name() + " (" + run.r().name() + ") begins! Collect the lost supplies marked by glowing water.");
    }

    private static void trialTick(ServerPlayer player) {
        TrialRun run = TRIALS.get(player.getUUID());
        if (run == null) {
            return;
        }
        ServerLevel world = (ServerLevel) player.level();
        int x = GielinorMap.toOsrsX(player.getBlockX());
        int y = GielinorMap.toOsrsY(player.getBlockZ());
        boolean onSkiff = "skiff".equals(boatType(player.getVehicle()));
        if (!onSkiff || Math.max(Math.abs(x - run.trial.x()), Math.abs(y - run.trial.y())) > 110) {
            if (++run.outside > 17) { // 10 seconds outside the trial area (wiki)
                TRIALS.remove(player.getUUID());
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
        world.sendParticles(ParticleTypes.GLOW, bx + 1, world.getSeaLevel() + 1, bz + 1, 6, 0.6, 0.4, 0.6, 0.02);
        if (Math.abs(player.getX() - bx - 1) <= 4 && Math.abs(player.getZ() - bz - 1) <= 4) {
            if (run.crates < r.crates()) {
                run.crates++;
                Msg.action(player, "You pick up some lost supplies (" + run.crates + "/" + r.crates() + ").");
            } else {
                run.objectives++;
                Msg.action(player, (run.objectives % 2 == 1 ? "You load the shipment" : "You make the delivery") + " ("
                        + run.objectives + "/" + r.objectives() + ").");
            }
            world.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 1.2f);
        }
    }

    private static void finishTrial(ServerPlayer player, TrialRun run) {
        TRIALS.remove(player.getUUID());
        SailingTraining.Rank r = run.r();
        double seconds = (ActionManager.gameTick() - run.start) * 0.6;
        XpService.add(player, Skill.SAILING, r.xp());
        CompoundTag best = SkillingState.get(player.getServer()).entry("trials", player.getStringUUID());
        String key = run.trial.id() + "." + r.name();
        boolean target = seconds <= r.targetSeconds();
        Msg.action(player, "You complete " + run.trial.name() + " (" + r.name() + ") in " + (int) seconds + "s"
                + (target ? ", beating the target time!" : " (target " + r.targetSeconds() + "s)."));
        if (target && !best.getBoolean(key)) {
            // The rank is earned: a one-time bonus and the rank's reward (wiki trial pages), and the next rank opens.
            best.putBoolean(key, true);
            XpService.add(player, Skill.SAILING, r.bonus());
            Msg.action(player, "You earn the " + r.name() + " rank and " + (int) r.bonus() + " bonus Sailing XP!");
            if (r.reward() != null) {
                SkillingUtil.give(player, r.reward(), 1);
                Msg.action(player, "You receive " + Msg.article(SkillingUtil.lower(SkillingUtil.name(r.reward()))) + ".");
            }
        }
        reclaimTrialRewards(player, run.trial);
    }

    /**
     * Hands back a trial's rank reward (the captured wind mote, the heart of ithell) to a player who earned the rank but
     * has it in neither the pack nor the bank: it is a reusable building tool, and the wiki has Gurtob in Backwater and
     * Gwyna in Porth Gwenith give another if it is lost or destroyed (here the trial buoy, or finishing a run, does).
     * Also covers saves from before the reward was handed out. Returns the items given.
     */
    public static List<String> reclaimTrialRewards(ServerPlayer player, SailingTraining.Trial trial) {
        CompoundTag best = SkillingState.get(player.getServer()).entry("trials", player.getStringUUID());
        List<String> given = new ArrayList<>();
        for (SailingTraining.Rank r : trial.ranks()) {
            if (r.reward() != null && best.getBoolean(trial.id() + "." + r.name()) && available(player, r.reward()) == 0) {
                SkillingUtil.give(player, r.reward(), 1);
                given.add(r.reward());
                Msg.action(player, "The trial master hands you another " + SkillingUtil.lower(SkillingUtil.name(r.reward()))
                        + " for your " + r.name() + " rank.");
            }
        }
        return given;
    }

    /** Uses the trial buoy at a block as a player would (self-test). */
    public static void useTrialBuoy(ServerPlayer player, BlockPos pos) {
        trialBuoy(player, pos);
    }

    /** Whether a trial's rank can be attempted: the rank before it must have been earned (wiki: Swordfish first). */
    static boolean rankOpen(ServerPlayer player, SailingTraining.Trial trial, int rank) {
        CompoundTag best = SkillingState.get(player.getServer()).entry("trials", player.getStringUUID());
        return rank == 0 || best.getBoolean(trial.id() + "." + trial.ranks().get(rank - 1).name());
    }

    /** Ends a trial at once as if its last objective was just reached after {@code seconds} (self-test). */
    public static void finishTrialForTest(ServerPlayer player, SailingTraining.Trial trial, int rank, double seconds) {
        TrialRun run = new TrialRun(trial, rank, ActionManager.gameTick() - Math.round(seconds / 0.6));
        finishTrial(player, run);
    }

    // ------------------------------------------------------------------ trawling

    /** The best trawling net built on a boat, or null. */
    static SailingTraining.Net net(ServerPlayer player, String boat) {
        SailingTraining.Net best = null;
        for (SailingTraining.Net n : SailingTraining.NETS) {
            if (boat != null && hasFacility(player, boat, n.facility())) {
                best = n;
            }
        }
        return best;
    }

    /**
     * The net a crewmate works beside the player: the second-best net built, or null with fewer than two. One person
     * operates each net facility (wiki Deep sea trawling: a crewmate operating a net rolls every 5 ticks instead of 3;
     * Trawling net: a boat can carry two nets), and the player works the best one.
     */
    static SailingTraining.Net crewNet(ServerPlayer player, String boat) {
        SailingTraining.Net best = null;
        SailingTraining.Net second = null;
        for (SailingTraining.Net n : SailingTraining.NETS) {
            if (boat != null && hasFacility(player, boat, n.facility())) {
                second = best;
                best = n;
            }
        }
        return second;
    }

    /**
     * A fathom stone points out the cardinal direction of the nearest shoal; a fathom pearl shows where it is (wiki:
     * the pearl puts a hint arrow over the shoal; here the direction and distance in tiles). Returns the message, or null.
     */
    public static String fathom(ServerPlayer player, String boat) {
        boolean pearl = hasFacility(player, boat, "fathom_pearl");
        if (!pearl && !hasFacility(player, boat, "fathom_stone")) {
            return null;
        }
        int x = GielinorMap.toOsrsX(player.getBlockX());
        int y = GielinorMap.toOsrsY(player.getBlockZ());
        int[] pin = SailingShoals.nearestPin(x, y);
        String dir = compass(pin[0] - x, pin[1] - y, pearl);
        String msg = pearl ? "The fathom pearl glows: a shoal swims " + Math.max(Math.abs(pin[0] - x), Math.abs(pin[1] - y))
                + " tiles to the " + dir + "." : "The fathom stone tugs towards the " + dir + ".";
        Msg.action(player, msg);
        return msg;
    }

    /** "north", "south-east"...: cardinal only for the fathom stone, with the diagonals for the pearl (OSRS y grows north). */
    static String compass(int dx, int dy, boolean diagonals) {
        String ns = dy > 0 ? "north" : "south";
        String ew = dx > 0 ? "east" : "west";
        if (!diagonals) {
            return Math.abs(dy) >= Math.abs(dx) ? ns : ew;
        }
        if (Math.abs(dy) > 2 * Math.abs(dx)) {
            return ns;
        }
        if (Math.abs(dx) > 2 * Math.abs(dy)) {
            return ew;
        }
        return ns + "-" + ew;
    }

    /** The shoal whose marker or pin is at (or within range of) a block position, or null. */
    static SailingShoals.Shoal shoalNear(BlockPos pos, int range) {
        return SailingShoals.nearest(GielinorMap.toOsrsX(pos.getX()), GielinorMap.toOsrsY(pos.getZ()), range);
    }

    /** Using the trawling net (or clicking a shoal marker) from your boat: lower the nets into the shoal in range. */
    static void trawl(ServerPlayer player) {
        startTrawl(player, null);
    }

    /**
     * Lowers the boat's trawling net into a shoal: the given one, else the one in range of the boat (wiki Deep sea
     * trawling: a net facility, the shoal's Fishing level, a boat in range). Returns the running trawl, or null.
     */
    public static Trawl startTrawl(ServerPlayer player, SailingShoals.Shoal shoal) {
        String boat = boatType(player.getVehicle());
        if (boat == null) {
            Msg.game(player, "You need to be sailing your boat to lower a trawling net.");
            return null;
        }
        SailingTraining.Net net = net(player, boat);
        if (net == null) {
            Msg.game(player, "Your boat needs a trawling net facility (built at the boat schematics, level 56 Sailing) to trawl.");
            return null;
        }
        SailingShoals.Shoal s = shoal != null ? shoal : shoalNear(player.blockPosition(), SailingShoals.RANGE);
        if (s == null) {
            Msg.game(player, "There's no shoal in range of your net. Trawling shoals swim at their marked spots out at sea.");
            fathom(player, boat);
            return null;
        }
        if (!SkillingUtil.need(player, Skill.FISHING, s.level(), "trawl the " + SkillingUtil.lower(s.name()))) {
            return null;
        }
        Trawl t = new Trawl(s, net, boat, shoal == null);
        Msg.action(player, "You lower your " + SkillingUtil.lower(SailingTraining.facility(net.facility()).name()) + " into the "
                + SkillingUtil.lower(s.name()) + ".");
        ActionManager.start(player, t, true);
        return t;
    }

    /**
     * A trawling net working a shoal: every 3 ticks the net's Sailing XP (halved at the wrong depth) and a catch roll at
     * the shoal's trawling-net chance; on a boat with a second net, every 5 ticks the best deckhand able to work it rolls
     * a flat 4% on that net (crew Sailing XP scaled, see {@link SailingShoals#CREW_XP_FACTOR}). The shoal's stops cycle on the spot
     * (OSRS: along its route): the stop depth (shallow, or deep for three-depth shoals) then moderate halfway; a resuming
     * shoal turns mixed 10% of the time for two stops; a chum station spreads one offcut per stop (see {@link SailingShoals}).
     */
    public static final class Trawl implements Action {
        final SailingShoals.Shoal shoal;
        final SailingTraining.Net net;
        final String boat;
        /** False for a trawl the self-test drives at a shoal far away (the boat-in-range check is skipped). */
        final boolean checkRange;
        int ticks;
        int stopTick;
        boolean deepStop;
        int mixedStops;
        boolean baited;
        int crewTicks;
        /** Catches (the crew's among them) and fish so far, and whether a mixed shoal swam in its place (self-test). */
        public int catches;
        public int crewCatches;
        public int fish;
        public boolean everMixed;

        Trawl(SailingShoals.Shoal shoal, SailingTraining.Net net, String boat, boolean checkRange) {
            this.shoal = shoal;
            this.net = net;
            this.boat = boat;
            this.checkRange = checkRange;
        }

        /** The mixed shoal swimming in place of this one, or null. */
        public SailingShoals.Mixed mixed() {
            return mixedStops > 0 ? SailingShoals.mixedFor(shoal) : null;
        }

        /** Forces the next stops to be a mixed shoal (self-test). */
        public void forceMixed() {
            mixedStops = SailingShoals.MIXED_STOPS;
            everMixed = true;
        }

        @Override
        public boolean tick(ServerPlayer p, long gameTick) {
            if (!boat.equals(boatType(p.getVehicle())) || net(p, boat) == null) {
                Msg.game(p, "You haul in your trawling net.");
                return false;
            }
            if (checkRange && shoalNear(p.blockPosition(), SailingShoals.RANGE + 4) != shoal) {
                Msg.game(p, "The shoal swims out of reach of your net.");
                return false;
            }
            ServerLevel world = (ServerLevel) p.level();
            if (stopTick == 0) {
                newStop(p);
            }
            if (++stopTick >= shoal.stopTicks()) {
                stopTick = 0;
            }
            if (ticks % 10 == 0) {
                world.sendParticles(ParticleTypes.FISHING, p.getX(), world.getSeaLevel() + 0.2, p.getZ(), 12, 2.5, 0.1, 2.5, 0.05);
            }
            ticks++;
            if (ticks % SailingShoals.PLAYER_ROLL_TICKS == 0) {
                roll(p, net, false);
            }
            if (++crewTicks >= SailingShoals.CREW_ROLL_TICKS) {
                crewTicks = 0;
                SailingTraining.Net second = crewNet(p, boat);
                if (second != null && crewTrawler(p, second) != null) {
                    roll(p, second, true);
                }
            }
            return true;
        }

        /** A shoal stopping: its depth for this stop, the mixed-shoal roll, and one offcut spread from a chum station. */
        private void newStop(ServerPlayer p) {
            RandomSource random = p.getRandom();
            deepStop = random.nextBoolean();
            if (mixedStops > 0) {
                mixedStops--;
            } else if (ticks > 0 && random.nextDouble() < SailingShoals.MIXED_CHANCE && SailingShoals.mixedFor(shoal) != null) {
                mixedStops = SailingShoals.MIXED_STOPS;
                everMixed = true;
                Msg.action(p, "The shoal shimmers: a " + SkillingUtil.lower(SailingShoals.mixedFor(shoal).name()) + " swims in its place!");
            }
            baited = spreadOffcuts(p);
        }

        /** Spreads one offcut the shoal takes from the cargo hold or inventory, if a chum station is built (wiki). */
        private boolean spreadOffcuts(ServerPlayer p) {
            Set<String> fac = facilities(p, boat);
            boolean fine = fac.contains("advanced_chum_station") || fac.contains("chum_spreader");
            if (!fine && !fac.contains("chum_station")) {
                return false;
            }
            for (String bait : SailingShoals.bait(shoal)) {
                if ((fine || bait.equals(SailingShoals.OFFCUTS)) && SeaHooks.takeOne(p, boat, bait)) {
                    Msg.action(p, "You spread some " + SkillingUtil.lower(SkillingUtil.name(bait)) + " to bait the shoal.");
                    return true;
                }
            }
            return false;
        }

        /** The deckhand aboard who can work the given net (Deckhandiness 1 rope .. 4 cotton), or null. */
        private SailingCrew.Crewmate crewTrawler(ServerPlayer p, SailingTraining.Net n) {
            int need = SailingTraining.NETS.indexOf(n) + 1;
            SailingCrew.Crewmate best = null;
            for (SailingCrew.Crewmate c : SailingCrew.aboard(SeaHooks.crewOrder(p), boat, SkillingUtil.level(p, Skill.SAILING))) {
                if (c.deckhandiness() >= need && (best == null || c.deckhandiness() > best.deckhandiness())) {
                    best = c;
                }
            }
            return best;
        }

        /** One roll of a net (the player's, or a crewmate's flat 4% for fish only). Returns the fish caught. */
        int roll(ServerPlayer p, SailingTraining.Net net, boolean crew) {
            int depth = SailingShoals.depthAt(shoal.maxDepth(), shoal.stopTicks(), stopTick, deepStop);
            boolean right = net.depth() >= depth; // the net follows the shoal's depth where it can reach (playability)
            double xp = (right ? net.xpPerRoll() : net.xpPerRoll() / 2) * (crew ? SailingShoals.CREW_XP_FACTOR : 1);
            XpService.add(p, Skill.SAILING, xp);
            SailingShoals.Mixed m = mixed();
            int fishing = SkillingUtil.level(p, Skill.FISHING);
            double chance = crew ? SailingShoals.CREW_CHANCE : m != null ? SailingShoals.catchChance(fishing, m.low(), m.high())
                    : SailingShoals.catchChance(fishing, shoal.low(), shoal.high());
            RandomSource random = p.getRandom();
            if (random.nextDouble() >= chance) {
                return 0;
            }
            CookingData.NewFish f = fish(m != null ? SailingShoals.mixedFish(m, new java.util.Random(random.nextLong())) : shoal.fish());
            int n = SailingShoals.fishCaught(net.maxFish(), right, baited, new java.util.Random(random.nextLong()));
            if (f == null || n <= 0) {
                return 0;
            }
            SkillingUtil.give(p, f.raw(), n);
            catches++;
            crewCatches += crew ? 1 : 0;
            fish += n;
            if (!crew) {
                XpService.add(p, Skill.FISHING, f.fishXp()); // wiki: the shoal's Fishing XP once per successful catch
            }
            Msg.action(p, (crew ? "Your crew's net" : "Your net") + " brings up " + n + " x " + SkillingUtil.lower(f.name())
                    + (right ? "" : " (the net is at the wrong depth)") + ".");
            return n;
        }
    }

    private static CookingData.NewFish fish(String id) {
        for (CookingData.NewFish f : CookingData.FISH) {
            if (f.id().equals(id)) {
                return f;
            }
        }
        return null;
    }
}
