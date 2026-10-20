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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;

/** Dwarf multicannon: set up, load, fire at a test monster, granite cannonballs and pick up. */
final class CannonChecks {
    private CannonChecks() {
    }

    static List<SelfTest.Check> all(TestContext c) {
        List<SelfTest.Check> checks = new ArrayList<>();
        checks.add(new SelfTest.Check("cannon:setup_load_fire_pickup", () -> cycle(c)));
        checks.add(new SelfTest.Check("cannon:granite_cannonballs", () -> granite(c)));
        checks.add(new SelfTest.Check("cannon:immune_sand_crab", () -> immune(c)));
        return checks;
    }

    private static void clean(ServerPlayer p) {
        CannonService.pickUp(p);
        SysState.SysPlayer sp = SysState.of(p);
        sp.cannonOut = false;
        sp.cannonSteel = 0;
        sp.cannonGranite = 0;
    }

    /** Why the cannon couldn't be set up: the area rule that forbids it, or what is in the way of its 3x3. */
    private static AssertionError setUpFailed(TestContext c, BlockPos centre) {
        String why = CannonService.prohibited(c.player, centre);
        if (why == null) {
            why = CannonService.obstruction(c.world, centre);
        }
        return new AssertionError("couldn't set up a cannon at " + centre.toShortString() + (why == null ? "" : " (" + why + ")"));
    }

    private static String cycle(TestContext c) {
        ServerPlayer p = c.player;
        clean(p);
        c.completeQuests(null);
        for (String part : Multicannon.PARTS) {
            Inv.give(p, part, 1);
        }
        BlockPos centre = c.near(-4, 0);
        try {
            if (!CannonService.setUp(p, centre, true)) {
                throw setUpFailed(c, centre);
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
            // Three OSRS tiles (six blocks) north of the cannon's centre: in the north direction's area.
            BlockPos at = centre.offset(0, 0, -3 * com.runecraft.osrs.GielinorMap.SCALE);
            target.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
            if (target instanceof Mob mob) {
                mob.finalizeSpawn(c.world, c.world.getCurrentDifficultyAt(at), MobSpawnType.SPAWNER, null);
                mob.setPersistenceRequired();
                mob.setNoAi(true);
            }
            c.world.addFreshEntity(target);
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

    /** A monster whose wiki infobox says immunecannon = Yes (sand crab) is never shot, even standing in a double-hit spot. */
    private static String immune(TestContext c) {
        ServerPlayer p = c.player;
        clean(p);
        c.completeQuests(null);
        for (String part : Multicannon.PARTS) {
            Inv.give(p, part, 1);
        }
        BlockPos centre = c.near(-4, 0);
        LivingEntity crab = null;
        try {
            if (!Multicannon.immune("sand_crab")) {
                throw new AssertionError("sand_crab is not on the cannon immunity list");
            }
            EntityType<?> type = ModEntities.typeById("sand_crab");
            if (type == null) {
                throw new SelfTest.Skip("no sand_crab entity");
            }
            if (!CannonService.setUp(p, centre, true)) {
                throw setUpFailed(c, centre);
            }
            CannonService.Cannon cannon = CannonService.of(p);
            Inv.give(p, Multicannon.STEEL_BALL, 30);
            CannonService.load(p);
            Entity e = type.create(c.world);
            if (!(e instanceof LivingEntity living)) {
                throw new AssertionError("sand_crab is not a living entity");
            }
            crab = living;
            BlockPos at = centre.offset(0, 0, -3 * com.runecraft.osrs.GielinorMap.SCALE);
            crab.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
            if (crab instanceof Mob mob) {
                mob.finalizeSpawn(c.world, c.world.getCurrentDifficultyAt(at), MobSpawnType.SPAWNER, null);
                mob.setPersistenceRequired();
                mob.setNoAi(true);
            }
            c.world.addFreshEntity(crab);
            long now = SysTicks.now();
            for (int i = 0; i < 16; i++) {
                CannonService.tick(p, cannon, now + i);
            }
            if (cannon.fired != 0) {
                throw new AssertionError("the cannon fired " + cannon.fired + " times at an immune sand crab");
            }
            return "16 ticks, no shots at a sand crab 3 tiles north";
        } finally {
            if (crab != null) {
                crab.discard();
            }
            clean(p);
            c.world.removeBlock(centre, false);
        }
    }

    private static String granite(TestContext c) {
        ServerPlayer p = c.player;
        Inv.give(p, Multicannon.STEEL_BALL, 5);
        Inv.give(p, Multicannon.GRANITE_DUST, 3);
        int made = CannonService.coatGranite(p);
        if (made != 3 || Inv.count(p, Multicannon.GRANITE_BALL) != 3 || Inv.count(p, Multicannon.STEEL_BALL) != 2) {
            throw new AssertionError("coating 5 cannonballs with 3 granite dust made " + made);
        }
        return "3 granite cannonballs";
    }
}
