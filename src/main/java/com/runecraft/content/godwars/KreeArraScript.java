package com.runecraft.content.godwars;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.godwars.GwdBosses;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

/**
 * Kree'arra: flies (immune to melee), so it is fought with ranged or magic. Its tornado (ranged, up to 69) and wind blast
 * (magic, up to 21) hit everyone in the room; the tornado knocks players back. OSRS Wiki: if no one is attacking it, it
 * flies up to players to claw them in melee (up to 25) as well. In its chamber of the God Wars Dungeon its three
 * bodyguards are the chamber's own (they respawn on their tiles); summoned anywhere else it brings them along.
 */
final class KreeArraScript extends GwdScript {
    /** Game ticks without being hit after which Kree'arra counts as "not under attack" and may swoop in to claw. */
    private static final int UNDISTURBED_TICKS = 8;
    private final List<Mob> guards = new ArrayList<>();
    private long lastHit = -1000;

    @Override
    public void onSpawn(DataBossEntity boss) {
        if (!inDungeon(boss)) {
            guards.add(spawnGuard(boss, "flight_kilisa", 4, 0));
            guards.add(spawnGuard(boss, "wingman_skree", -4, 2));
            guards.add(spawnGuard(boss, "flockleader_geerin", 0, -4));
        }
        shout(boss, "Kraaaw!");
    }

    @Override
    protected AttackDef pick(DataBossEntity boss, ServerPlayer target, double distance) {
        boolean undisturbed = com.runecraft.action.ActionManager.gameTick() - lastHit > UNDISTURBED_TICKS;
        if (undisturbed && distance <= meleeReach(boss) && boss.getRandom().nextInt(2) == 0) {
            return GwdBosses.KREE_MELEE;
        }
        return boss.getRandom().nextInt(3) == 0 ? GwdBosses.KREE_WIND : GwdBosses.KREE_TORNADO;
    }

    @Override
    public int modifyIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, com.runecraft.osrs.AttackType type) {
        lastHit = com.runecraft.action.ActionManager.gameTick();
        return damage;
    }

    @Override
    protected boolean isMultiTarget(AttackDef attack) {
        return attack == GwdBosses.KREE_TORNADO || attack == GwdBosses.KREE_WIND;
    }

    @Override
    public void gameTick(DataBossEntity boss, long gameTick) {
        super.gameTick(boss, gameTick);
        // Summoned bodyguards stay with their general.
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
