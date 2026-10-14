package com.runecraft.selftest;

import com.runecraft.content.systems.CannonService;
import com.runecraft.content.systems.SysState;
import com.runecraft.content.systems.SysTicks;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.systems.Multicannon;
import com.runecraft.player.Players;
import com.runecraft.registry.ModBlocks;
import com.runecraft.registry.ModEntities;
import com.runecraft.util.Inv;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Dwarf multicannon: set up, load, fire at a test monster, granite cannonballs and pick up. */
final class CannonChecks {
    private CannonChecks() {
    }

    static List<SelfTest.Check> all(TestContext c) {
        List<SelfTest.Check> checks = new ArrayList<>();
        checks.add(new SelfTest.Check("cannon:setup_load_fire_pickup", () -> cycle(c)));
        checks.add(new SelfTest.Check("cannon:granite_cannonballs", () -> granite(c)));
        return checks;
    }

    private static void clean(ServerPlayerEntity p) {
        CannonService.pickUp(p);
        SysState.SysPlayer sp = SysState.of(p);
        sp.cannonOut = false;
        sp.cannonSteel = 0;
        sp.cannonGranite = 0;
    }

    private static String cycle(TestContext c) {
        ServerPlayerEntity p = c.player;
        clean(p);
        c.completeQuests(null);
        for (String part : Multicannon.PARTS) {
            Inv.give(p, part, 1);
        }
        BlockPos centre = c.near(-4, 0);
        try {
            if (!CannonService.setUp(p, centre, true)) {
                throw new AssertionError("couldn't set up a cannon at " + centre.toShortString());
            }
            CannonService.Cannon cannon = CannonService.of(p);
            if (cannon == null || !cannon.complete()) {
                throw new AssertionError("the cannon isn't assembled after setting up");
            }
            if (c.world.getBlockState(centre).getBlock() != ModBlocks.MODULE_BLOCKS.get(Multicannon.BLOCK)) {
                throw new AssertionError("no cannon block at " + centre.toShortString());
            }
            for (String part : Multicannon.PARTS) {
                if (Inv.has(p, part, 1)) {
                    throw new AssertionError(part + " is still in the inventory after setting up");
                }
            }
            Inv.give(p, Multicannon.STEEL_BALL, 100);
            int loaded = CannonService.load(p);
            int capacity = Multicannon.capacity(com.runecraft.content.systems.CombatAchievementService.tier(p));
            if (loaded != capacity) {
                throw new AssertionError("loaded " + loaded + " cannonballs, expected the capacity " + capacity);
            }
            EntityType<?> type = ModEntities.typeById("moss_giant");
            if (type == null) {
                throw new SelfTest.Skip("no moss_giant entity");
            }
            Entity e = type.create(c.world);
            if (!(e instanceof LivingEntity target)) {
                throw new AssertionError("moss_giant is not a living entity");
            }
            // Three tiles north of the cannon's centre: in the north direction's area.
            BlockPos at = centre.add(0, 0, -3);
            target.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
            if (target instanceof MobEntity mob) {
                mob.initialize(c.world, c.world.getLocalDifficulty(at), SpawnReason.SPAWNER, null, null);
                mob.setPersistent();
                mob.setAiDisabled(true);
            }
            c.world.spawnEntity(target);
            float hp = target.getHealth();
            int xp = Players.data(p).skills().xpTenths(Skill.RANGED);
            long now = SysTicks.now();
            int facing = cannon.facing;
            CannonService.tick(p, cannon, now);
            if (cannon.facing != Multicannon.next(facing)) {
                throw new AssertionError("the barrel didn't turn one direction in a tick");
            }
            for (int i = 1; i < 80 && target.isAlive() && target.getHealth() >= hp; i++) {
                CannonService.tick(p, cannon, now + i);
            }
            if (cannon.fired == 0) {
                throw new AssertionError("the cannon never fired at a moss giant 3 tiles north");
            }
            if (cannon.balls() != capacity - cannon.fired) {
                throw new AssertionError(cannon.fired + " shots but " + cannon.balls() + " balls left of " + capacity);
            }
            int gained = Players.data(p).skills().xpTenths(Skill.RANGED) - xp;
            if (target.isAlive() && target.getHealth() >= hp) {
                throw new AssertionError(cannon.fired + " shots never damaged the moss giant");
            }
            if (gained <= 0 || gained % Multicannon.XP_TENTHS_PER_DAMAGE != 0) {
                throw new AssertionError("cannon damage gave " + gained / 10.0 + " Ranged XP (expected 2 per damage)");
            }
            int left = cannon.balls();
            target.discard();
            if (!CannonService.pickUp(p)) {
                throw new AssertionError("couldn't pick the cannon up");
            }
            for (String part : Multicannon.PARTS) {
                if (!Inv.has(p, part, 1)) {
                    throw new AssertionError(part + " wasn't returned on pick-up");
                }
            }
            if (Inv.count(p, Multicannon.STEEL_BALL) != 100 - capacity + left) {
                throw new AssertionError("pick-up returned the wrong number of cannonballs");
            }
            if (!c.world.getBlockState(centre).isAir()) {
                throw new AssertionError("the cannon block is still there after picking it up");
            }
            return cannon.fired + " shots, " + gained / 10 + " Ranged XP, " + left + " balls back";
        } finally {
            clean(p);
            c.world.removeBlock(centre, false);
        }
    }

    private static String granite(TestContext c) {
        ServerPlayerEntity p = c.player;
        Inv.give(p, Multicannon.STEEL_BALL, 5);
        Inv.give(p, Multicannon.GRANITE_DUST, 3);
        int made = CannonService.coatGranite(p);
        if (made != 3 || Inv.count(p, Multicannon.GRANITE_BALL) != 3 || Inv.count(p, Multicannon.STEEL_BALL) != 2) {
            throw new AssertionError("coating 5 cannonballs with 3 granite dust made " + made);
        }
        return "3 granite cannonballs";
    }
}
