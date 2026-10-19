package com.runecraft.content.godwars;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.godwars.GwdBosses;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

/**
 * Kree'arra: flies (immune to melee), so it is fought with ranged or magic. Its tornado (ranged, up
 * to 71) and wind blast (magic, up to 21) hit everyone in the room; the tornado knocks players back.
 * When someone stands right next to it, it may claw them instead. Spawns with its three bodyguards.
 */
final class KreeArraScript extends GwdScript {
    private final List<Mob> guards = new ArrayList<>();

    @Override
    public void onSpawn(DataBossEntity boss) {
        guards.add(spawnMinion(boss, "flight_kilisa", 4, 0));
        guards.add(spawnMinion(boss, "wingman_skree", -4, 2));
        guards.add(spawnMinion(boss, "flockleader_geerin", 0, -4));
        shout(boss, "Kraaaw!");
    }

    @Override
    protected AttackDef pick(DataBossEntity boss, ServerPlayer target, double distance) {
        if (distance <= meleeReach(boss) && boss.getRandom().nextInt(3) == 0) {
            return GwdBosses.KREE_MELEE;
        }
        return boss.getRandom().nextInt(3) == 0 ? GwdBosses.KREE_WIND : GwdBosses.KREE_TORNADO;
    }

    @Override
    protected boolean isMultiTarget(AttackDef attack) {
        return attack == GwdBosses.KREE_TORNADO || attack == GwdBosses.KREE_WIND;
    }

    @Override
    public void gameTick(DataBossEntity boss, long gameTick) {
        super.gameTick(boss, gameTick);
        // Bodyguards stay with their general.
        if (gameTick % 10 == 0) {
            for (Mob guard : guards) {
                if (guard != null && guard.isAlive() && guard.distanceToSqr(boss) > 20 * 20) {
                    guard.teleportTo(boss.getX() + 2, boss.getY(), boss.getZ() + 2);
                }
            }
        }
    }

    @Override
    public void onDeath(DataBossEntity boss, ServerPlayer killer) {
        super.onDeath(boss, killer);
        shout(boss, "Kraaaaaaaw...");
    }
}
