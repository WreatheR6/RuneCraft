package com.runecraft.selftest;

import com.runecraft.combat.CombatHandler;
import com.runecraft.content.slayer.SlayerService;
import com.runecraft.content.slayer.SlayerState;
import com.runecraft.osrs.content.dungeons.DungeonBlocks;
import com.runecraft.osrs.content.dungeons.Dungeons;
import com.runecraft.registry.ModBlocks;
import com.runecraft.registry.ModEntities;
import com.runecraft.world.StructurePlacer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;

/** World and dungeon rules: the Kalphite Cave's task-only gate on every damage path, the Temple of Marimbo's locked door. */
final class DungeonWorldChecks {
    private DungeonWorldChecks() {
    }

    static List<SelfTest.Check> all(TestContext c) {
        List<SelfTest.Check> checks = new ArrayList<>();
        checks.add(new SelfTest.Check("dungeon:kalphite_cave_task_only", () -> kalphiteGate(c)));
        checks.add(new SelfTest.Check("dungeon:marimbo_wooden_door", () -> marimboDoor(c)));
        return checks;
    }

    /** Damage dealt straight through CombatHandler.applyDamage (the cannon's and spells' path) is stopped without a task. */
    private static String kalphiteGate(TestContext c) {
        ServerPlayer p = c.player;
        Dungeons.Camp camp = null;
        for (Dungeons.Dungeon d : Dungeons.all()) {
            if (d.id().equals("dng_kalphite_cave") && !d.camps().isEmpty()) {
                camp = d.camps().get(0);
            }
        }
        if (camp == null) {
            throw new AssertionError("the Kalphite Cave has no camps");
        }
        EntityType<?> type = ModEntities.typeById(camp.creatures().get(0));
        if (type == null) {
            throw new SelfTest.Skip("no " + camp.creatures().get(0) + " entity");
        }
        BlockPos at = new BlockPos(camp.centreX(), camp.feetY(), camp.centreZ());
        if (Dungeons.taskOnly(at.getX(), at.getY(), at.getZ()) == null) {
            throw new AssertionError("the Kalphite Cave camp at " + at.toShortString() + " is not task-only");
        }
        StructurePlacer.applyAround(c.world, at.getX(), at.getZ(), 4);
        SlayerState.Profile profile = SlayerService.profile(p);
        String task = profile.task;
        profile.task = null;
        LivingEntity mob = null;
        try {
            Entity e = type.create(c.world);
            if (!(e instanceof LivingEntity living)) {
                throw new AssertionError(camp.creatures().get(0) + " is not a living entity");
            }
            mob = living;
            mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
            if (mob instanceof Mob m) {
                m.finalizeSpawn(c.world, c.world.getCurrentDifficultyAt(at), MobSpawnType.SPAWNER, null, null);
                m.setPersistenceRequired();
                m.setNoAi(true);
            }
            c.world.addFreshEntity(mob);
            float before = mob.getHealth();
            int dealt = CombatHandler.applyDamage(p, mob, 3);
            if (dealt != 0 || mob.getHealth() < before) {
                throw new AssertionError("a player without a Kalphite task dealt " + dealt + " damage in the Kalphite Cave");
            }
            return "no damage without the task";
        } finally {
            profile.task = task;
            if (mob != null) {
                mob.discard();
            }
        }
    }

    /** The wooden door to the great western cave stands in the trapped tunnel; its west side is the locked side. */
    private static String marimboDoor(TestContext c) {
        int[] d = DungeonBlocks.marimboDoor();
        StructurePlacer.applyAround(c.world, d[0], d[2], 4);
        BlockPos pos = new BlockPos(d[0], d[1], d[2]);
        if (c.world.getBlockState(pos).getBlock() != ModBlocks.MODULE_BLOCKS.get(DungeonBlocks.MARIMBO_DOOR)) {
            throw new AssertionError("no wooden door at " + pos.toShortString() + ": " + c.world.getBlockState(pos));
        }
        if (!DungeonBlocks.insideMarimboDoor(d[0] - 0.5) || DungeonBlocks.insideMarimboDoor(d[0] + 1.5)) {
            throw new AssertionError("the door's locked side is wrong");
        }
        return "door at " + pos.toShortString();
    }
}
