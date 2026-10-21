package com.runecraft.content.skilling;

import com.runecraft.action.ActionManager;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.skilling.CookingData;
import com.runecraft.osrs.content.skilling.SailingData;
import com.runecraft.osrs.content.skilling.SailingLockboxes;
import com.runecraft.osrs.content.skilling.SailingSea;
import com.runecraft.osrs.content.skilling.SailingShoals;
import com.runecraft.osrs.content.skilling.SailingTraining;
import com.runecraft.osrs.world.StructureDef;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The Sailing walk of the self-test (round 40): a player, real or fake, launches a skiff on a pool by the test platform
 * and plays the skill through its real entry points: builds parts and facilities at the boat schematics, sails and trims,
 * salvages a shipwreck and sorts the salvage, opens a lockbox and builds what its schematic unlocks, trawls a shoal, kills
 * a sea creature with the boat's cannon, opens a lost crate, delivers a courier task, earns Barracuda Trial ranks and the
 * wind mote they give, builds a wind catcher and paints the trim. Every step checks its wiki numbers; any broken step
 * throws an AssertionError naming it.
 */
public final class SailingSelfTest {
    private SailingSelfTest() {
    }

    private static int xp(ServerPlayer p, Skill s) {
        return Players.data(p).skills().xpTenths(s);
    }

    private static void check(boolean ok, String step, String what) {
        if (!ok) {
            throw new AssertionError("sailing walk, " + step + ": " + what);
        }
    }

    /** Hands over materials: a pack-sized amount into the pack, bulk stacks (10,000 runes) into the bank, as players keep them. */
    private static void give(ServerPlayer p, Map<String, Integer> mats) {
        mats.forEach((item, n) -> {
            if (n > 64) {
                Players.data(p).bank().deposit(item, "", n, 0);
            } else {
                Inv.give(p, item, n);
            }
        });
    }

    /** Builds an upgrade through the shipyard's build path, checking the materials and the Construction XP. */
    private static void build(ServerPlayer p, String type, Object upgrade, String step) {
        Map<String, Integer> mats = upgrade instanceof SailingTraining.Part part ? part.materials()
                : ((SailingTraining.Facility) upgrade).materials();
        double conXp = upgrade instanceof SailingTraining.Part part ? part.conXp() : ((SailingTraining.Facility) upgrade).conXp();
        check(SailingHooks.upgradeOptions(p, type).contains(upgrade), step, "not offered at the boat schematics");
        Map<String, Long> had = new java.util.HashMap<>();
        mats.keySet().forEach(item -> had.put(item, SailingHooks.available(p, item)));
        give(p, mats);
        int before = xp(p, Skill.CONSTRUCTION);
        check(SailingHooks.buildUpgrade(p, type, upgrade), step, "could not be built with its materials");
        check(xp(p, Skill.CONSTRUCTION) - before == Math.round(conXp * 10), step, "gave " + (xp(p, Skill.CONSTRUCTION) - before) / 10.0
                + " Construction XP, wiki " + conXp);
        for (Map.Entry<String, Integer> e : mats.entrySet()) {
            check(SailingHooks.available(p, e.getKey()) == had.get(e.getKey()), step, e.getKey() + " was not used up");
        }
    }

    private static SailingTraining.Facility facility(String id) {
        for (SailingTraining.Facility f : SailingHooks.allFacilities()) {
            if (f.id().equals(id)) {
                return f;
            }
        }
        throw new AssertionError("no facility " + id);
    }

    /** One game tick under way: the boat moves (or not) and the Sailing boat tick runs. */
    private static void sailTick(ServerPlayer p, Boat boat, boolean moving) {
        boat.setDeltaMovement(moving ? new Vec3(0.2, 0, 0) : Vec3.ZERO);
        ActionManager.advanceForSelfTest();
        SailingHooks.boatTick(p);
    }

    /**
     * The walk. {@code at} is a free spot on the test platform (the pool is dug around it), {@code place} places a block
     * and has it cleared after the check.
     */
    public static String walk(ServerPlayer p, BlockPos at, BiFunction<BlockPos, BlockState, BlockPos> place) {
        ServerLevel world = (ServerLevel) p.level();
        List<String> notes = new ArrayList<>();
        // A pool: a basin of stone with water a block deep.
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                place.apply(at.offset(dx, -2, dz), Blocks.STONE.defaultBlockState());
                place.apply(at.offset(dx, -1, dz), Blocks.WATER.defaultBlockState());
            }
        }
        Boat boat = null;
        Mob creature = null;
        // Fresh sea state: no wind motes, trim or encounter timers left over from earlier sailing.
        SailingHooks.resetForTest(p);
        SeaHooks.resetForTest(p);
        try {
            // 1. Launch a skiff from its kit.
            Inv.give(p, "skiff_kit", 1);
            ItemStack kit = p.getInventory().items.stream().filter(s -> "skiff_kit".equals(Inv.idOf(s))).findFirst().orElse(ItemStack.EMPTY);
            check(!kit.isEmpty(), "launch", "no skiff kit in the pack");
            boat = SailingHooks.launchAt(p, kit, SailingData.boat("skiff_kit"), Vec3.atCenterOf(at).add(0, -0.4, 0));
            check(kit.isEmpty(), "launch", "the kit was not used up");
            p.startRiding(boat, true);
            boat.positionRider(p); // aboard at once (the walk runs inside one server tick)
            String type = SailingHooks.boatType(p.getVehicle());
            check("skiff".equals(type), "launch", "the player is on " + type + ", not a skiff");
            notes.add("skiff launched");

            // 2. The shipyard: an oak hull, then a hook, a cannon, a trawling net and a cargo hold.
            Inv.give(p, "hammer", 1);
            build(p, type, SailingTraining.part(type, SailingTraining.PartType.HULL, 1), "oak hull");
            check(SailingHooks.partTier(p, type, SailingTraining.PartType.HULL) == 1, "oak hull", "hull tier not raised");
            check(SeaHooks.boatHp(p, type) == SailingSea.boatHp(type, 1, 0), "oak hull", "boat hitpoints " + SeaHooks.boatHp(p, type));
            for (String f : List.of("bronze_salvaging_hook", "bronze_cannon", "rope_trawling_net", "basic_cargo_hold")) {
                build(p, type, facility(f), f);
                check(SailingHooks.hasFacility(p, type, f), f, "not on the boat");
            }
            notes.add("built hull, hook, cannon, net, hold");

            // 3. Sail: the sails are trimmed every 30 seconds under way for the wiki trimming XP.
            int sailing = xp(p, Skill.SAILING);
            for (int i = 0; i < SailingTraining.TRIM_TICKS; i++) {
                sailTick(p, boat, true);
            }
            double trim = SailingTraining.trimXp(type, 0);
            check(xp(p, Skill.SAILING) - sailing == Math.round(trim * 10), "sail", "trimming gave " + (xp(p, Skill.SAILING) - sailing) / 10.0
                    + " Sailing XP, wiki " + trim);
            notes.add("trimmed for " + trim);

            // 4. Salvage a small shipwreck with the boat's hook (the real 4-tick action).
            SailingData.Wreck wreck = SailingData.wreck("small_shipwreck");
            BlockState wreckBlock = SkillingUtil.moduleBlock(wreck.blockId());
            check(wreckBlock != null, "salvage", "no " + wreck.blockId() + " block");
            BlockPos wreckPos = place.apply(at.offset(5, 0, 0), wreckBlock);
            sailing = xp(p, Skill.SAILING);
            int ticks = 0;
            int moves = 0;
            while (Inv.count(p, wreck.salvage()) == 0 && ticks < 2000) {
                if (!ActionManager.isBusy(p)) {
                    if (GatheringHooks.depleted(world, wreckPos) && moves < 3) {
                        wreckPos = place.apply(at.offset(5, 0, ++moves), wreckBlock);
                    }
                    SailingHooks.salvage(p, wreckPos, wreck);
                    check(ActionManager.isBusy(p), "salvage", "the salvaging hook was not lowered");
                }
                ActionManager.tickNow(p);
                ticks++;
            }
            ActionManager.cancel(p);
            int hauled = Inv.count(p, wreck.salvage());
            check(hauled > 0, "salvage", "no salvage in " + ticks + " ticks");
            check(xp(p, Skill.SAILING) - sailing == Math.round(hauled * wreck.xp() * 10), "salvage", "XP for " + hauled + " salvage: "
                    + (xp(p, Skill.SAILING) - sailing) / 10.0);
            notes.add(hauled + " salvage in " + ticks + " ticks");

            // 5. A lockbox: locked without its key, the key gives the schematic, reading it unlocks the salvaging station,
            // which then sorts the salvage at sea.
            SailingLockboxes.Lockbox chin = SailingLockboxes.byIsland("chinchompa_island");
            SailingTraining.Facility station = facility("salvaging_station");
            boolean creative = p.getAbilities().instabuild;
            p.getAbilities().instabuild = false;
            try {
                give(p, station.materials());
                check(!SailingHooks.buildUpgrade(p, type, station), "lockbox", "the salvaging station was built without its schematic");
                BlockPos box = new BlockPos(StructureDef.bx(chin.x()), at.getY(), StructureDef.bz(chin.y()));
                check(!SailingLockboxHooks.open(p, box), "lockbox", "opened without its small key");
                Inv.give(p, chin.keyId(), 1);
                check(SailingLockboxHooks.open(p, box) && Inv.has(p, chin.schematic(), 1), "lockbox", "the key gave no schematic");
                ItemStack schematic = p.getInventory().items.stream().filter(s -> chin.schematic().equals(Inv.idOf(s))).findFirst()
                        .orElseThrow();
                SailingLockboxHooks.read(p, schematic, chin);
                check(SailingLockboxHooks.missing(p, station) == null, "lockbox", "reading the schematic didn't unlock the station");
                Inv.remove(p, "teak_plank", 64);
                Inv.remove(p, "steel_nails", 64);
                build(p, type, station, "salvaging station");
            } finally {
                p.getAbilities().instabuild = creative;
            }
            check(SeaHooks.atStation(p), "sort", "the boat's salvaging station doesn't count as a station");
            ItemStack salvage = p.getInventory().items.stream().filter(s -> wreck.salvage().equals(Inv.idOf(s))).findFirst().orElseThrow();
            sailing = xp(p, Skill.SAILING);
            check(SeaHooks.sort(p, salvage, wreck), "sort", "couldn't sort salvage on the boat");
            check(xp(p, Skill.SAILING) - sailing == Math.round(wreck.sortXp() * 10), "sort", "sorting XP " + (xp(p, Skill.SAILING) - sailing) / 10.0);
            notes.add("lockbox opened, salvage sorted aboard");

            // 6. Trawl a giant krill shoal with the rope net (the real 3-tick action; the shoal is far away, so the trawl
            // is started on it directly).
            SailingShoals.Shoal krill = SailingShoals.shoal("giant_krill_shoal");
            CookingData.NewFish krillFish = CookingData.FISH.stream().filter(f -> f.id().equals("giant_krill")).findFirst().orElseThrow();
            int fishing = xp(p, Skill.FISHING);
            SailingHooks.Trawl trawl = SailingHooks.startTrawl(p, krill);
            check(trawl != null && ActionManager.isBusy(p), "trawl", "the net was not lowered");
            ticks = 0;
            while (trawl.catches == 0 && ticks < 1500 && ActionManager.isBusy(p)) {
                ActionManager.tickNow(p);
                ticks++;
            }
            ActionManager.cancel(p);
            check(trawl.catches > 0, "trawl", "nothing caught in " + ticks + " ticks");
            check(Inv.count(p, krillFish.raw()) + Inv.count(p, "raw_haddock") + Inv.count(p, "raw_yellowfin") + Inv.count(p, "raw_halibut")
                    == trawl.fish, "trawl", "the fish caught didn't reach the pack");
            int fishXp = xp(p, Skill.FISHING) - fishing;
            check(fishXp > 0 && trawl.fish <= trawl.catches * 2, "trawl", trawl.fish + " fish in " + trawl.catches
                    + " catches with a rope net (max 2 a catch), " + fishXp / 10.0 + " Fishing XP");
            check(trawl.crewCatches == 0, "trawl", "a crewmate worked the boat's only net (one operator per net)");
            if (!trawl.everMixed) {
                check(fishXp == Math.round(trawl.catches * krillFish.fishXp() * 10), "trawl", "Fishing XP " + fishXp / 10.0
                        + " for " + trawl.catches + " catches (wiki: 112.5 a catch)");
            }
            notes.add(trawl.fish + " fish in " + trawl.catches + " catches");

            // 7. Boat combat: a tern shot down with the bronze cannon (the player mans it; the boat keeps firing).
            SailingSea.SeaCreature tern = SailingSea.creature("tern");
            creature = SeaHooks.spawnCreature(world, tern, Vec3.atCenterOf(at.offset(0, 0, -6)), p);
            check(creature != null, "combat", "no tern spawned");
            Inv.give(p, "bronze_cannonball", 100);
            int balls = Inv.count(p, "bronze_cannonball"); // sorted salvage may have given a few already
            int ranged = xp(p, Skill.RANGED);
            sailing = xp(p, Skill.SAILING);
            check(SeaHooks.useCannonball(p, "bronze_cannonball"), "combat", "the cannon would not fire");
            ticks = 0;
            while (creature.isAlive() && ticks < 600) {
                sailTick(p, boat, false);
                if (++ticks % SailingSea.CANNON_TICKS == 0 && creature.isAlive()) {
                    SeaHooks.useCannonball(p, "bronze_cannonball"); // keep the cannon on the tern (fires when it is ready)
                }
            }
            check(!creature.isAlive(), "combat", "the tern survived " + ticks + " ticks of cannon fire");
            int shots = balls - Inv.count(p, "bronze_cannonball");
            int rangedXp = xp(p, Skill.RANGED) - ranged;
            check(shots > 0 && rangedXp > 0 && rangedXp % 40 == 0, "combat", shots + " shots, " + rangedXp / 10.0 + " Ranged XP (4 a damage)");
            check(SeaHooks.boatHp(p, type) > 0 && "skiff".equals(SailingHooks.boatType(p.getVehicle())), "combat", "the skiff sank");
            notes.add("tern sunk in " + shots + " shots");

            // 8. A lost crate at 99 Sailing: one of the eight wiki loot lines and its XP.
            sailing = xp(p, Skill.SAILING);
            int tier = SailingSea.lostCrateTier(99, new java.util.Random());
            SailingSea.Got crate = SeaHooks.openLostCrate(p, tier);
            check(Inv.has(p, com.runecraft.osrs.data.Drop.baseOf(crate.item()), 1), "crate", "no " + crate.item() + " from the crate");
            check(xp(p, Skill.SAILING) - sailing == Math.round(SailingSea.CRATE_XP[tier] * 10), "crate", "crate XP");
            notes.add("lost " + SailingSea.CRATE_NAMES[tier] + " crate: " + crate.item());

            // 9. A courier task from Port Sarim's notice board, collected and delivered.
            SailingData.Port sarim = SailingData.port("port_sarim");
            CompoundTag held = SkillingState.get(p.getServer()).entry("courier_tasks", p.getStringUUID());
            for (int i = 0; i < 40 && held.getAllKeys().isEmpty(); i++) {
                SeaHooks.clearBounties(p);
                SailingHooks.portBoard(p, sarim);
            }
            check(!held.getAllKeys().isEmpty(), "port task", "no courier task taken from " + sarim.name());
            SeaHooks.clearBounties(p);
            SailingTraining.CourierTask task = SailingTraining.courier(Integer.parseInt(held.getAllKeys().iterator().next()));
            if (!held.getBoolean(String.valueOf(task.id()))) {
                SailingHooks.portBoard(p, SailingData.port(task.cargo()));
            }
            check(Inv.count(p, "cargo_crate") == task.qty(), "port task", "collected " + Inv.count(p, "cargo_crate") + " of " + task.qty()
                    + " cargo crates");
            sailing = xp(p, Skill.SAILING);
            int coins = Inv.count(p, Inv.COINS);
            SailingHooks.portBoard(p, SailingData.port(task.destination()));
            check(held.getAllKeys().isEmpty() && Inv.count(p, "cargo_crate") == 0, "port task", "the delivery was not taken");
            check(xp(p, Skill.SAILING) - sailing == Math.round(SailingTraining.courierXp(task) * 10)
                    && Inv.count(p, Inv.COINS) - coins == SailingTraining.courierCoins(task), "port task", "delivery reward");
            notes.add("courier task " + task.id() + " delivered");

            // 10. The Jubbly Jive: Swordfish then Shark beaten for their one-time bonuses and the captured wind mote; then a
            // wind catcher stores a mote from trimming (25% less trimming XP) and releases it for 40 XP.
            SailingTraining.Trial jive = SailingTraining.trial("jubbly_jive");
            check(!SailingHooks.rankOpen(p, jive, 1), "trials", "the Shark rank was open before Swordfish");
            sailing = xp(p, Skill.SAILING);
            SailingHooks.finishTrialForTest(p, jive, 0, 100);
            SailingTraining.Rank sword = jive.ranks().get(0);
            check(xp(p, Skill.SAILING) - sailing == Math.round((sword.xp() + sword.bonus()) * 10), "trials", "Swordfish XP");
            check(SailingHooks.rankOpen(p, jive, 1), "trials", "Swordfish didn't open the Shark rank");
            SailingHooks.finishTrialForTest(p, jive, 1, 170);
            check(Inv.has(p, "captured_wind_mote", 1), "trials", "no captured wind mote for the Shark rank");
            SailingTraining.Facility catcher = facility("wind_catcher");
            build(p, type, catcher, "wind catcher");
            check(Inv.has(p, "captured_wind_mote", 1), "wind catcher", "the captured wind mote was used up");
            // A lost mote is handed back (wiki: Gurtob in Backwater gives another): use the trial buoy without one.
            Inv.remove(p, "captured_wind_mote", Inv.count(p, "captured_wind_mote"));
            Players.data(p).bank().withdraw("captured_wind_mote", "", Players.data(p).bank().count("captured_wind_mote", ""));
            check(!Inv.has(p, "captured_wind_mote", 1), "trials", "couldn't clear the captured wind mote");
            if (p.isShiftKeyDown()) {
                SailingHooks.reclaimTrialRewards(p, jive); // sneaking at the buoy would start a run
            } else {
                SailingHooks.useTrialBuoy(p, new BlockPos(com.runecraft.osrs.GielinorMap.toBlockX(jive.objectiveB()[0]), world.getSeaLevel(),
                        com.runecraft.osrs.GielinorMap.toBlockZ(jive.objectiveB()[1])));
            }
            check(Inv.count(p, "captured_wind_mote") == 1, "trials", "the trial buoy didn't hand back a lost captured wind mote");
            check(SailingHooks.reclaimTrialRewards(p, jive).isEmpty(), "trials", "a second captured wind mote was handed out");
            sailing = xp(p, Skill.SAILING);
            double trimmed = SailingHooks.trimSails(p, type);
            check(Math.abs(trimmed - trim * SailingTraining.CATCHER_TRIM_FACTOR) < 1e-9 && SailingHooks.storedMotes(p) == 1, "wind catcher",
                    "trim with a catcher gave " + trimmed + ", motes " + SailingHooks.storedMotes(p));
            check(SailingHooks.releaseMote(p, type) == SailingTraining.WIND_MOTE_XP && SailingHooks.storedMotes(p) == 0, "wind catcher",
                    "releasing the mote");
            notes.add("Jubbly Jive ranks, wind catcher");

            // 11. Shark paint on the skiff's trim (wiki: 25 Sailing, 20 Construction, one paint, 10 Construction XP).
            Inv.remove(p, "shark_paint", Inv.count(p, "shark_paint")); // a port task may have paid some (1/36)
            Inv.give(p, "shark_paint", 1);
            int con = xp(p, Skill.CONSTRUCTION);
            check(SeaHooks.paintBoat(p, boat, "shark_paint") == null, "paint", "couldn't paint the skiff");
            check(boat.getVariant() == SeaHooks.PAINTS.get("shark_paint") && "shark_paint".equals(SeaHooks.trim(p, type))
                    && xp(p, Skill.CONSTRUCTION) - con == 100 && !Inv.has(p, "shark_paint", 1), "paint", "trim not applied");
            notes.add("trimmed with shark paint");

            // 12. A range aboard cooks what it heats; food that is also an ingredient is still eaten (no cooking menu).
            check(SailingHooks.cookedOnRange("raw_shrimps") && SailingHooks.cookedOnRange("uncooked_pizza"), "range", "raw food not cookable aboard");
            for (String food : List.of("tomato", "cheese", "banana", "minecraft:milk_bucket", "plain_pizza")) {
                check(!SailingHooks.cookedOnRange(food), "range", food + " would open the cooking menu instead of being eaten");
            }
            return String.join(", ", notes);
        } finally {
            ActionManager.cancel(p);
            p.stopRiding();
            if (creature != null && creature.isAlive()) {
                creature.discard();
            }
            if (boat != null) {
                boat.discard();
            }
            SeaHooks.clearHold(p, "skiff");
            SeaHooks.clearBounties(p);
            SailingHooks.resetForTest(p);
            SeaHooks.resetForTest(p);
        }
    }
}
