package com.runecraft.content.godwars;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.godwars.GwdBosses;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

/**
 * Commander Zilyana: attacks every 2 ticks. Slashes with her Saradomin sword (up to 27) and, only when next to her
 * target, calls down Saradomin's power on everyone in the room (magic, up to 20). She never stands still for long: every
 * few ticks she dashes to another part of her chamber. Starlight, Growler and Bree are her chamber's own bodyguards in
 * the God Wars Dungeon; summoned anywhere else she brings them along.
 */
final class ZilyanaScript extends GwdScript {
    private static final String[] SHOUTS = {"Death to the enemies of the light!", "Slay the evil ones!",
            "Saradomin lend me strength!", "By the power of Saradomin!", "May Saradomin be my sword!", "Good will always triumph!",
            "Forward! Our allies are with us!", "Saradomin is with us!", "In the name of Saradomin!", "All praise Saradomin!",
            "Attack! Find the Godsword!"};
    private final List<Mob> guards = new ArrayList<>();
    private double homeX;
    private double homeY;
    private double homeZ;

    @Override
    public void onSpawn(DataBossEntity boss) {
        homeX = boss.getX();
        homeY = boss.getY();
        homeZ = boss.getZ();
        if (!inDungeon(boss)) {
            guards.add(spawnGuard(boss, "starlight", 3, 0));
            guards.add(spawnGuard(boss, "growler", -3, 2));
            guards.add(spawnGuard(boss, "bree", 0, -3));
        }
    }

    /** OSRS Wiki: she only casts her magic when she is in melee distance; further away she closes in to slash. */
    @Override
    protected AttackDef pick(DataBossEntity boss, ServerPlayer target, double distance) {
        if (distance <= meleeReach(boss) && boss.getRandom().nextInt(4) == 0) {
            return GwdBosses.ZILY_MAGIC;
        }
        return GwdBosses.ZILY_MELEE;
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
            boss.getNavigation().moveTo(homeX + Math.cos(angle) * r, homeY, homeZ + Math.sin(angle) * r, 1.4);
        }
        if (gameTick % 20 == 0 && boss.getRandom().nextInt(3) == 0) {
            shout(boss, SHOUTS[boss.getRandom().nextInt(SHOUTS.length)]);
        }
        if (gameTick % 10 == 0) {
            for (Mob guard : guards) {
                if (guard != null && guard.isAlive() && guard.distanceToSqr(boss) > 20 * 20) {
                    guard.teleportTo(boss.getX() + 2, boss.getY(), boss.getZ() + 2);
                }
            }
        }
    }
}
