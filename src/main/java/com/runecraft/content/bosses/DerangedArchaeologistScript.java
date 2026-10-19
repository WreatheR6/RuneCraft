package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.combat.CombatHandler;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.bosses.IslandBosses;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * The Deranged archaeologist (see {@link IslandBosses}): ranged attacks, melee only against a player standing next to
 * him, and "Learn to read!" - three books thrown around the player's tile that explode four ticks later (56 on the tile a
 * book lands on, 18 on the tiles around it, ignoring armour and prayers); the book aimed at the player bursts into two
 * more. The special has a 25-tick cooldown and a 1/3 chance to replace an attack.
 */
final class DerangedArchaeologistScript extends ScriptedBoss {
    /** One OSRS tile is two blocks: a book's own tile, and the ring of tiles around it. */
    private static final double DIRECT = 1.0;
    private static final double SPLASH = 3.0;
    private long specialReadyAt;

    @Override
    protected boolean driven() {
        return true;
    }

    @Override
    protected boolean wantsMelee(DataBossEntity boss, ServerPlayer target) {
        return false;
    }

    @Override
    protected AttackDef nextAttack(DataBossEntity boss, ServerPlayer target, double distance) {
        if (now >= specialReadyAt && boss.getRandom().nextInt(IslandBosses.SPECIAL_CHANCE) == 0) {
            specialReadyAt = now + IslandBosses.SPECIAL_COOLDOWN_TICKS;
            learnToRead(boss, target);
            return IDLE_RANGED;
        }
        return distance <= IslandBosses.DERANGED_MELEE.range() ? IslandBosses.DERANGED_MELEE : IslandBosses.DERANGED_RANGED;
    }

    private void learnToRead(DataBossEntity boss, ServerPlayer target) {
        BossFx.say(boss, "Deranged archaeologist: Learn to read!");
        BossFx.warn(boss, "The Deranged archaeologist hurls a shower of books!");
        Vec3 centre = target.position();
        book(boss, centre, IslandBosses.BOOK_DELAY_TICKS, true);
        book(boss, around(boss, centre, 4.0), IslandBosses.BOOK_DELAY_TICKS, false);
        book(boss, around(boss, centre, 4.0), IslandBosses.BOOK_DELAY_TICKS, false);
    }

    /** One exploding book; the central one splits into two more when it lands. */
    private void book(DataBossEntity boss, Vec3 at, int delay, boolean splits) {
        strike(boss, at, delay, 0, SPLASH, ParticleTypes.FLAME, (hit, pos) -> {
            for (ServerPlayer p : hit) {
                boolean direct = p.position().distanceToSqr(pos) <= DIRECT * DIRECT;
                int max = direct ? IslandBosses.BOOK_DIRECT_MAX : IslandBosses.BOOK_SPLASH_MAX;
                CombatHandler.applyDamage(boss.isAlive() ? boss : null, p, p.getRandom().nextInt(max + 1));
            }
            if (splits) {
                book(boss, around(boss, pos, 4.0), IslandBosses.BOOK_SECOND_DELAY_TICKS, false);
                book(boss, around(boss, pos, 4.0), IslandBosses.BOOK_SECOND_DELAY_TICKS, false);
            }
        });
    }
}
