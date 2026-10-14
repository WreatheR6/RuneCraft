package com.runecraft.selftest;

import com.runecraft.content.skilling.SeaHooks;
import com.runecraft.osrs.Experience;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.skilling.SailingCrew;
import com.runecraft.osrs.content.skilling.SailingData;
import com.runecraft.osrs.content.skilling.SailingSea;
import com.runecraft.player.Players;
import com.runecraft.registry.ModBlocks;
import com.runecraft.util.Inv;
import net.minecraft.block.Block;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

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
        return checks;
    }

    private static void level(ServerPlayerEntity p, Skill skill, int level) {
        Players.data(p).skills().setXpTenths(skill, Experience.xpForLevel(level) * 10);
    }

    private static String sort(TestContext c) {
        ServerPlayerEntity p = c.player;
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
                c.world.setBlockState(at, station.getDefaultState());
                int xp = Players.data(p).skills().xpTenths(Skill.SAILING);
                long before = p.getInventory().main.stream().filter(s -> !s.isEmpty()).count();
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
                long after = p.getInventory().main.stream().filter(s -> !s.isEmpty()).count();
                items += Math.max(0, after - before);
                sorted++;
            }
        } finally {
            c.world.removeBlock(at, false);
        }
        return sorted + " salvage types sorted (" + items + " new stacks)";
    }

    private static String cannonKill(TestContext c) {
        ServerPlayerEntity p = c.player;
        level(p, Skill.RANGED, 99);
        level(p, Skill.SAILING, 99);
        SailingSea.SeaCreature tern = SailingSea.creature("tern");
        SailingSea.Bounty bounty = SailingSea.BOUNTIES.stream().filter(b -> b.creature().equals("tern")).findFirst().orElse(null);
        if (tern == null || bounty == null) {
            throw new AssertionError("no tern or tern bounty task");
        }
        SeaHooks.takeBounty(p, bounty);
        BlockPos at = c.near(0, -6);
        MobEntity mob = SeaHooks.spawnCreature(c.world, tern, new Vec3d(at.getX() + 0.5, at.getY(), at.getZ() + 0.5), p);
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
        ServerPlayerEntity p = c.player;
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
        ServerPlayerEntity p = c.player;
        level(p, Skill.SAILING, 99);
        level(p, Skill.RANGED, 99);
        List<String> before = SeaHooks.crewOrder(p);
        SailingCrew.Crewmate jim = SailingCrew.crewmate("jobless_jim");
        MobEntity mob = null;
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
            mob = SeaHooks.spawnCreature(c.world, SailingSea.creature("tern"), new Vec3d(at.getX() + 0.5, at.getY(), at.getZ() + 0.5), p);
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
        ServerPlayerEntity p = c.player;
        level(p, Skill.RANGED, 99);
        BlockPos at = c.near(0, -6);
        SailingSea.SeaCreature shark = SailingSea.creature("bull_shark");
        if (shark == null) {
            shark = SailingSea.CREATURES.get(SailingSea.CREATURES.size() - 1);
        }
        MobEntity mob = SeaHooks.spawnCreature(c.world, shark, new Vec3d(at.getX() + 0.5, at.getY(), at.getZ() + 0.5), p);
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
        ServerPlayerEntity p = c.player;
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
