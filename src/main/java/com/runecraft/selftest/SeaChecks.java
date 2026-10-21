package com.runecraft.selftest;

import com.runecraft.content.skilling.SailingLockboxHooks;
import com.runecraft.content.skilling.SailingSelfTest;
import com.runecraft.content.skilling.SeaHooks;
import com.runecraft.osrs.Experience;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.skilling.SailingCrew;
import com.runecraft.osrs.content.skilling.SailingData;
import com.runecraft.osrs.content.skilling.SailingLockboxes;
import com.runecraft.osrs.content.skilling.SailingSea;
import com.runecraft.osrs.content.skilling.SailingTraining;
import com.runecraft.osrs.world.StructureDef;
import com.runecraft.player.Players;
import com.runecraft.registry.ModBlocks;
import com.runecraft.util.Inv;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/**
 * Sailing at sea: sorting salvage at a salvaging station, one boat-combat kill with a cannon, repair kits, the crew
 * (recruiting, repairing from the cargo hold, manning the cannon), incendiary cannonballs and shipwright wreck recovery.
 */
final class SeaChecks {
    private SeaChecks() {
    }

    static List<SelfTest.Check> all(TestContext c) {
        List<SelfTest.Check> checks = new ArrayList<>();
        checks.add(new SelfTest.Check("sailing:sort_salvage", () -> sort(c)));
        checks.add(new SelfTest.Check("sailing:cannon_kill", () -> cannonKill(c)));
        checks.add(new SelfTest.Check("sailing:repair_kit", () -> repairKit(c)));
        checks.add(new SelfTest.Check("sailing:crew", () -> crew(c)));
        checks.add(new SelfTest.Check("sailing:incendiary", () -> incendiary(c)));
        checks.add(new SelfTest.Check("sailing:wreck_recovery", () -> wreckRecovery(c)));
        checks.add(new SelfTest.Check("sailing:crew_special_ammo", () -> crewSpecialAmmo(c)));
        checks.add(new SelfTest.Check("sailing:lockbox", () -> lockbox(c)));
        // Named "skill:" so the client's real-player run ('/rc selftest real ... skill: ...') walks it too.
        checks.add(new SelfTest.Check("skill:sailing:walk", () -> walk(c)));
        return checks;
    }

    /**
     * The whole skill walked on a skiff in a pool by the platform: build the boat at the shipyard, sail and trim, salvage
     * and sort, open a lockbox, trawl, sink a tern with the cannon, open a lost crate, deliver a courier task, earn
     * Barracuda Trial ranks, build a wind catcher and paint the trim ({@link SailingSelfTest}).
     */
    private static String walk(TestContext c) {
        c.completeQuests(null);
        return SailingSelfTest.walk(c.player, c.near(0, 5), c::place);
    }

    /** The crew fire the cannonball you loaded: an incendiary ball from the cargo hold sets the creature alight. */
    private static String crewSpecialAmmo(TestContext c) {
        ServerPlayer p = c.player;
        level(p, Skill.SAILING, 99);
        level(p, Skill.RANGED, 99);
        List<String> before = SeaHooks.crewOrder(p);
        String ammoBefore = SeaHooks.loadedAmmo(p);
        String ball = "bronze_incendiary_cannonball";
        SailingCrew.Crewmate jim = SailingCrew.crewmate("jobless_jim");
        Mob mob = null;
        try {
            SeaHooks.setCrew(p, new ArrayList<>(List.of("jobless_jim")));
            SeaHooks.clearHold(p, "skiff");
            SeaHooks.stockHold(p, "skiff", "bronze_cannonball", 50);
            SeaHooks.stockHold(p, "skiff", ball, 50);
            SeaHooks.loadAmmo(p, ball);
            BlockPos at = c.near(0, -6);
            SailingSea.SeaCreature shark = SailingSea.creature("bull_shark");
            if (shark == null) {
                shark = SailingSea.CREATURES.get(SailingSea.CREATURES.size() - 1);
            }
            mob = SeaHooks.spawnCreature(c.world, shark, new Vec3(at.getX() + 0.5, at.getY(), at.getZ() + 0.5), p);
            if (mob == null) {
                throw new SelfTest.Skip("no " + shark.id() + " entity");
            }
            int shots = 0;
            while (mob.isAlive() && SeaHooks.burns(mob) == 0 && shots < 50) {
                SeaHooks.crewFire(p, "skiff", mob, SailingSea.CANNONS.get(0), jim);
                shots++;
            }
            if (SeaHooks.inHold(p, "skiff", ball) != 50 - shots || SeaHooks.inHold(p, "skiff", "bronze_cannonball") != 50) {
                throw new AssertionError("crew fired " + (50 - SeaHooks.inHold(p, "skiff", ball)) + " incendiary and "
                        + (50 - SeaHooks.inHold(p, "skiff", "bronze_cannonball")) + " plain balls in " + shots + " shots");
            }
            if (mob.isAlive() && SeaHooks.burns(mob) == 0) {
                throw new AssertionError(shots + " crew incendiary shots never set the " + shark.name() + " alight");
            }
            return "crew fired the loaded incendiary balls, burning after " + shots + " shots";
        } finally {
            if (mob != null && mob.isAlive()) {
                mob.discard();
            }
            SeaHooks.clearHold(p, "skiff");
            SeaHooks.setCrew(p, before);
            SeaHooks.loadAmmo(p, ammoBefore);
        }
    }

    /**
     * A lost schematic lockbox: locked without its small key, digging on the key's spot finds it, the key opens the
     * lockbox, and reading the schematic unlocks the dragon cannon at the boat schematics.
     */
    private static String lockbox(TestContext c) {
        ServerPlayer p = c.player;
        level(p, Skill.SAILING, 99);
        SailingLockboxes.Lockbox l = SailingLockboxes.byIsland("deepfin_point");
        SailingTraining.Facility cannon = null;
        for (SailingTraining.Facility f : SailingSea.FACILITIES) {
            if (f.id().equals("dragon_cannon")) {
                cannon = f;
            }
        }
        if (l == null || cannon == null) {
            throw new AssertionError("no Deepfin Point lockbox or dragon cannon facility");
        }
        boolean creative = p.getAbilities().instabuild;
        p.getAbilities().instabuild = false;
        SailingLockboxHooks.forget(p);
        try {
            if (SailingLockboxHooks.missing(p, cannon) == null) {
                throw new AssertionError("the dragon cannon can be built without its schematic");
            }
            BlockPos box = new BlockPos(StructureDef.bx(l.x()), c.near(0, 0).getY(), StructureDef.bz(l.y()));
            if (SailingLockboxHooks.open(p, box)) {
                throw new AssertionError("opened the lockbox without its small key");
            }
            Inv.give(p, l.keyId(), 1);
            if (!SailingLockboxHooks.open(p, box) || !Inv.has(p, l.schematic(), 1) || Inv.has(p, l.keyId(), 1)) {
                throw new AssertionError("the small key didn't open the lockbox for its schematic");
            }
            ItemStack schematic = new ItemStack(com.runecraft.registry.ModItems.get(l.schematic()), 1);
            SailingLockboxHooks.read(p, schematic, l);
            if (!schematic.isEmpty() || SailingLockboxHooks.missing(p, cannon) != null) {
                throw new AssertionError("reading the schematic didn't unlock the dragon cannon");
            }
            return "key opened the " + l.islandName() + " lockbox; the dragon cannon schematic unlocks the build";
        } finally {
            SailingLockboxHooks.forget(p);
            Inv.remove(p, l.keyId(), Inv.count(p, l.keyId()));
            Inv.remove(p, l.schematic(), Inv.count(p, l.schematic()));
            p.getAbilities().instabuild = creative;
        }
    }

    private static void level(ServerPlayer p, Skill skill, int level) {
        Players.data(p).skills().setXpTenths(skill, Experience.xpForLevel(level) * 10);
    }

    private static String sort(TestContext c) {
        ServerPlayer p = c.player;
        level(p, Skill.SAILING, 99);
        Block station = ModBlocks.MODULE_BLOCKS.get("port_salvaging_station");
        if (station == null) {
            throw new AssertionError("no port_salvaging_station block");
        }
        BlockPos at = c.near(2, 0);
        int sorted = 0;
        int items = 0;
        try {
            for (SailingData.Wreck w : SailingData.WRECKS) {
                ItemStack stack = new ItemStack(com.runecraft.registry.ModItems.get(w.salvage()), 1);
                c.world.removeBlock(at, false);
                if (SeaHooks.sort(p, stack, w)) {
                    throw new AssertionError("sorted " + w.salvage() + " without a salvaging station");
                }
                c.world.setBlockAndUpdate(at, station.defaultBlockState());
                int xp = Players.data(p).skills().xpTenths(Skill.SAILING);
                long before = p.getInventory().items.stream().filter(s -> !s.isEmpty()).count();
                if (!SeaHooks.sort(p, stack, w)) {
                    throw new AssertionError("couldn't sort " + w.salvage() + " next to a salvaging station");
                }
                if (!stack.isEmpty()) {
                    throw new AssertionError("sorting didn't use up the " + w.salvage());
                }
                int gained = Players.data(p).skills().xpTenths(Skill.SAILING) - xp;
                if (gained != Math.round(w.sortXp() * 10)) {
                    throw new AssertionError("sorting " + w.salvage() + " gave " + gained / 10.0 + " XP, expected " + w.sortXp());
                }
                long after = p.getInventory().items.stream().filter(s -> !s.isEmpty()).count();
                items += Math.max(0, after - before);
                sorted++;
            }
        } finally {
            c.world.removeBlock(at, false);
        }
        return sorted + " salvage types sorted (" + items + " new stacks)";
    }

    private static String cannonKill(TestContext c) {
        ServerPlayer p = c.player;
        level(p, Skill.RANGED, 99);
        level(p, Skill.SAILING, 99);
        SailingSea.SeaCreature tern = SailingSea.creature("tern");
        SailingSea.Bounty bounty = SailingSea.BOUNTIES.stream().filter(b -> b.creature().equals("tern")).findFirst().orElse(null);
        if (tern == null || bounty == null) {
            throw new AssertionError("no tern or tern bounty task");
        }
        SeaHooks.takeBounty(p, bounty);
        BlockPos at = c.near(0, -6);
        Mob mob = SeaHooks.spawnCreature(c.world, tern, new Vec3(at.getX() + 0.5, at.getY(), at.getZ() + 0.5), p);
        if (mob == null) {
            throw new SelfTest.Skip("no tern entity");
        }
        if ((int) mob.getMaxHealth() != tern.hp()) {
            mob.discard();
            throw new AssertionError("tern has " + mob.getMaxHealth() + " hitpoints, the wiki says " + tern.hp());
        }
        SailingSea.Cannon cannon = SailingSea.CANNONS.get(0);
        SailingSea.Ball ball = SailingSea.ball("bronze_cannonball");
        Inv.give(p, ball.item(), 200);
        int sailing = Players.data(p).skills().xpTenths(Skill.SAILING);
        int ranged = Players.data(p).skills().xpTenths(Skill.RANGED);
        int shots = 0;
        int damage = 0;
        try {
            while (mob.isAlive() && shots < 200) {
                damage += SeaHooks.fireCannon(p, mob, cannon, ball.item());
                shots++;
            }
            if (mob.isAlive()) {
                throw new AssertionError(shots + " bronze cannon shots didn't kill a tern (" + damage + " damage)");
            }
            int s = Players.data(p).skills().xpTenths(Skill.SAILING) - sailing;
            int r = Players.data(p).skills().xpTenths(Skill.RANGED) - ranged;
            if (s != damage * 10 || r != damage * 40) {
                throw new AssertionError(damage + " damage gave " + s / 10.0 + " Sailing and " + r / 10.0 + " Ranged XP (expected 1 and 4 per damage)");
            }
            if (Inv.count(p, ball.item()) != 200 - shots) {
                throw new AssertionError(shots + " shots used " + (200 - Inv.count(p, ball.item())) + " cannonballs");
            }
            return shots + " shots, " + damage + " damage, bounty parts: " + Inv.count(p, bounty.item());
        } finally {
            if (mob.isAlive()) {
                mob.discard();
            }
            Inv.remove(p, ball.item(), Inv.count(p, ball.item()));
            Inv.remove(p, bounty.item(), Inv.count(p, bounty.item()));
            SeaHooks.clearBounties(p);
        }
    }

    private static String repairKit(TestContext c) {
        ServerPlayer p = c.player;
        int max = SeaHooks.boatHp(p, "skiff");
        SeaHooks.setBoatHp(p, "skiff", 5);
        int healed = SeaHooks.heal(p, "skiff", SailingCrew.kit("oak_repair_kit").heals());
        if (healed != Math.min(10, max - 5) || SeaHooks.boatHp(p, "skiff") != 5 + healed) {
            throw new AssertionError("an oak repair kit healed " + healed + " (boat at " + SeaHooks.boatHp(p, "skiff") + "/" + max + ")");
        }
        SeaHooks.setBoatHp(p, "skiff", max - 2);
        int capped = SeaHooks.heal(p, "skiff", 50);
        SeaHooks.setBoatHp(p, "skiff", -1);
        if (capped != 2) {
            throw new AssertionError("a rosewood kit on a boat 2 hitpoints down healed " + capped);
        }
        return "oak kit +" + healed + ", capped at full (" + max + ")";
    }

    private static String crew(TestContext c) {
        ServerPlayer p = c.player;
        level(p, Skill.SAILING, 99);
        level(p, Skill.RANGED, 99);
        List<String> before = SeaHooks.crewOrder(p);
        SailingCrew.Crewmate jim = SailingCrew.crewmate("jobless_jim");
        Mob mob = null;
        try {
            SeaHooks.setCrew(p, new ArrayList<>());
            SeaHooks.crewTalk(p, jim);
            if (!SeaHooks.crewOrder(p).contains("jobless_jim")) {
                throw new AssertionError("talking to Jobless Jim at 99 Sailing didn't recruit him");
            }
            SeaHooks.clearHold(p, "skiff");
            SeaHooks.stockHold(p, "skiff", "oak_repair_kit", 1);
            SeaHooks.setBoatHp(p, "skiff", 5);
            int healed = SeaHooks.crewRepair(p, "skiff", jim);
            // Deckhandiness 3: the kit's 10 plus 0-2.
            if (healed < 10 || healed > 12 || SeaHooks.inHold(p, "skiff", "oak_repair_kit") != 0) {
                throw new AssertionError("Jobless Jim repaired " + healed + " with an oak kit from the hold");
            }
            SeaHooks.setBoatHp(p, "skiff", -1);
            SeaHooks.stockHold(p, "skiff", "bronze_cannonball", 5);
            BlockPos at = c.near(0, -6);
            mob = SeaHooks.spawnCreature(c.world, SailingSea.creature("tern"), new Vec3(at.getX() + 0.5, at.getY(), at.getZ() + 0.5), p);
            if (mob == null) {
                throw new SelfTest.Skip("no tern entity");
            }
            int dmg = SeaHooks.crewFire(p, "skiff", mob, SailingSea.CANNONS.get(0), jim);
            if (dmg < 0 || SeaHooks.inHold(p, "skiff", "bronze_cannonball") != 4) {
                throw new AssertionError("Jobless Jim's cannon shot: " + dmg + ", balls left in the hold "
                        + SeaHooks.inHold(p, "skiff", "bronze_cannonball"));
            }
            return "recruited, repaired +" + healed + ", cannon shot " + dmg;
        } finally {
            if (mob != null && mob.isAlive()) {
                mob.discard();
            }
            SeaHooks.clearHold(p, "skiff");
            SeaHooks.setCrew(p, before);
        }
    }

    private static String incendiary(TestContext c) {
        ServerPlayer p = c.player;
        level(p, Skill.RANGED, 99);
        BlockPos at = c.near(0, -6);
        SailingSea.SeaCreature shark = SailingSea.creature("bull_shark");
        if (shark == null) {
            shark = SailingSea.CREATURES.get(SailingSea.CREATURES.size() - 1);
        }
        Mob mob = SeaHooks.spawnCreature(c.world, shark, new Vec3(at.getX() + 0.5, at.getY(), at.getZ() + 0.5), p);
        if (mob == null) {
            throw new SelfTest.Skip("no " + shark.id() + " entity");
        }
        String ball = "bronze_incendiary_cannonball";
        Inv.give(p, ball, 50);
        int shots = 0;
        try {
            while (mob.isAlive() && SeaHooks.burns(mob) == 0 && shots < 50) {
                SeaHooks.fireCannon(p, mob, SailingSea.CANNONS.get(0), ball);
                shots++;
            }
            if (SeaHooks.burns(mob) == 0) {
                throw new AssertionError(shots + " incendiary shots never set the " + shark.name() + " alight");
            }
            if (Inv.count(p, ball) != 50 - shots) {
                throw new AssertionError(shots + " shots used " + (50 - Inv.count(p, ball)) + " incendiary cannonballs");
            }
            return "burning after " + shots + " shots";
        } finally {
            if (mob.isAlive()) {
                mob.discard();
            }
            Inv.remove(p, ball, Inv.count(p, ball));
        }
    }

    private static String wreckRecovery(TestContext c) {
        ServerPlayer p = c.player;
        SeaHooks.markWrecked(p, "skiff_kit", "skiff");
        if (!SeaHooks.hasWreck(p)) {
            throw new AssertionError("no wreck recorded");
        }
        int fee = SeaHooks.boatHp(p, "skiff") * SailingCrew.SHIPWRIGHT_GP_PER_HP;
        Inv.remove(p, "skiff_kit", Inv.count(p, "skiff_kit"));
        Inv.give(p, Inv.COINS, fee);
        int coins = Inv.count(p, Inv.COINS);
        SeaHooks.recoverWrecks(p);
        try {
            if (SeaHooks.hasWreck(p) || Inv.count(p, "skiff_kit") != 1 || Inv.count(p, Inv.COINS) != coins - fee) {
                throw new AssertionError("recovery: wreck " + SeaHooks.hasWreck(p) + ", kits " + Inv.count(p, "skiff_kit") + ", paid "
                        + (coins - Inv.count(p, Inv.COINS)) + " of " + fee);
            }
            return "skiff recovered for " + fee + " coins";
        } finally {
            Inv.remove(p, "skiff_kit", Inv.count(p, "skiff_kit"));
        }
    }
}
