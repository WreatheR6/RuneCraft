package com.runecraft.content.godwars;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.godwars.GwdBosses;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Commander Zilyana: attacks every 2 ticks. Slashes with her Saradomin sword (up to 31) when next to
 * her target, and calls down Saradomin's power on everyone in the room (magic, up to 20). She never
 * stands still for long: every few ticks she dashes to another part of her chamber. Spawns with
 * Starlight, Growler and Bree.
 */
final class ZilyanaScript extends GwdScript {
    private static final String[] SHOUTS = {"Death to the enemies of the light!", "Slay the evil ones!",
            "Saradomin lend me strength!", "By the power of Saradomin!", "May Saradomin be my sword!", "Good will always triumph!",
            "Forward! Our allies are with us!", "Saradomin is with us!", "In the name of Saradomin!", "All praise Saradomin!",
            "Attack! Find the Godsword!"};
    private final List<MobEntity> guards = new ArrayList<>();
    private double homeX;
    private double homeY;
    private double homeZ;

    @Override
    public void onSpawn(DataBossEntity boss) {
        homeX = boss.getX();
        homeY = boss.getY();
        homeZ = boss.getZ();
        guards.add(spawnMinion(boss, "starlight", 3, 0));
        guards.add(spawnMinion(boss, "growler", -3, 2));
        guards.add(spawnMinion(boss, "bree", 0, -3));
    }

    @Override
    protected AttackDef pick(DataBossEntity boss, ServerPlayerEntity target, double distance) {
        if (distance <= meleeReach(boss) && boss.getRandom().nextInt(4) != 0) {
            return GwdBosses.ZILY_MELEE;
        }
        return GwdBosses.ZILY_MAGIC;
    }

    @Override
    protected boolean isMultiTarget(AttackDef attack) {
        return attack == GwdBosses.ZILY_MAGIC;
    }

    @Override
    public void gameTick(DataBossEntity boss, long gameTick) {
        super.gameTick(boss, gameTick);
        if (boss.getTarget() == null) {
            return;
        }
        if (gameTick % 12 == 0 && boss.getRandom().nextInt(2) == 0) {
            double angle = boss.getRandom().nextDouble() * Math.PI * 2;
            double r = 3 + boss.getRandom().nextDouble() * 6;
            boss.getNavigation().startMovingTo(homeX + Math.cos(angle) * r, homeY, homeZ + Math.sin(angle) * r, 1.4);
        }
        if (gameTick % 20 == 0 && boss.getRandom().nextInt(3) == 0) {
            shout(boss, SHOUTS[boss.getRandom().nextInt(SHOUTS.length)]);
        }
        if (gameTick % 10 == 0) {
            for (MobEntity guard : guards) {
                if (guard != null && guard.isAlive() && guard.squaredDistanceTo(boss) > 20 * 20) {
                    guard.requestTeleport(boss.getX() + 2, boss.getY(), boss.getZ() + 2);
                }
            }
        }
    }
}
