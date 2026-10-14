package com.runecraft.selftest;

import com.runecraft.content.skilling.SeaHooks;
import com.runecraft.osrs.Experience;
import com.runecraft.osrs.Skill;
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

/** Sailing at sea: sorting salvage at a salvaging station, and one boat-combat kill with a cannon. */
final class SeaChecks {
    private SeaChecks() {
    }

    static List<SelfTest.Check> all(TestContext c) {
        List<SelfTest.Check> checks = new ArrayList<>();
        checks.add(new SelfTest.Check("sailing:sort_salvage", () -> sort(c)));
        checks.add(new SelfTest.Check("sailing:cannon_kill", () -> cannonKill(c)));
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
                damage += SeaHooks.fireCannon(p, mob, cannon, ball);
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
}
