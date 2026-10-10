package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.combat.CombatHandler;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.bosses.ZulrahVorkath;
import com.runecraft.util.Inv;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Vorkath: six standard attacks (claw, bone spike, magic, dragonfire variants, dragonfire bomb), then
 * a special, alternating between the acid phase (acid pools everywhere and a 25-shot rapid-fire
 * barrage: keep moving, and stepping in acid heals Vorkath) and the frozen phase (ice breath freezes
 * you and a zombified spawn crawls towards you: kill it before it explodes).
 */
final class VorkathScript extends ScriptedBoss {
    private boolean acidNext;
    private int acidTicks;
    private final List<Vec3d> acid = new ArrayList<>();
    private MobEntity spawn;
    private ServerPlayerEntity frozen;
    private int normalsSinceSpecial;

    @Override
    public void onSpawn(DataBossEntity boss) {
        acidNext = boss.getRandom().nextBoolean();
        BossFx.say(boss, "Vorkath awakens from its icy slumber!");
    }

    @Override
    protected boolean driven() {
        return true;
    }

    @Override
    protected boolean canAttack(DataBossEntity boss) {
        return acidTicks <= 0 && (spawn == null || !spawn.isAlive());
    }

    @Override
    protected AttackDef nextAttack(DataBossEntity boss, ServerPlayerEntity target, double distance) {
        if (normalsSinceSpecial >= 6) {
            normalsSinceSpecial = 0;
            if (acidNext) {
                startAcid(boss, target);
            } else {
                startSpawn(boss, target);
            }
            acidNext = !acidNext;
            return null;
        }
        normalsSinceSpecial++;
        if (distance < 3.5 && boss.getRandom().nextInt(3) == 0) {
            return ZulrahVorkath.VORKATH_CLAW;
        }
        int r = boss.getRandom().nextInt(11);
        if (r < 3) {
            return ZulrahVorkath.VORKATH_SPIKE;
        } else if (r < 6) {
            return ZulrahVorkath.VORKATH_MAGIC;
        } else if (r < 8) {
            return ZulrahVorkath.VORKATH_DRAGONFIRE;
        } else if (r < 9) {
            return ZulrahVorkath.VORKATH_PINK_FIRE;
        } else if (r < 10) {
            return ZulrahVorkath.VORKATH_VENOM_FIRE;
        }
        return ZulrahVorkath.VORKATH_BOMB;
    }

    private void startAcid(DataBossEntity boss, ServerPlayerEntity target) {
        BossFx.say(boss, "Vorkath spews acid across the island! Keep moving!");
        acid.clear();
        for (int i = 0; i < 28; i++) {
            acid.add(around(boss, homeVec(), 11));
        }
        acidTicks = 25;
        cooldown = 27;
    }

    private void startSpawn(DataBossEntity boss, ServerPlayerEntity target) {
        perform(boss, target, ZulrahVorkath.VORKATH_FREEZE);
        Vec3d dir = target.getPos().subtract(boss.getPos()).normalize();
        Vec3d at = BossFx.ground(BossFx.world(boss), target.getX() + dir.z * 8, target.getZ() - dir.x * 8, target.getY());
        spawn = BossFx.spawnMinion(boss, "zombified_spawn", at, target);
        frozen = target;
        BossFx.sound(boss, SoundEvents.ENTITY_ZOMBIE_AMBIENT, 0.6f);
    }

    @Override
    public int onHitPlayer(DataBossEntity boss, ServerPlayerEntity player, int damage, AttackType type) {
        if (last == ZulrahVorkath.VORKATH_PINK_FIRE) {
            BossFx.disablePrayers(player, "Vorkath's pink dragonfire turns off your prayers!");
        }
        if ((last == ZulrahVorkath.VORKATH_DRAGONFIRE || last == ZulrahVorkath.VORKATH_PINK_FIRE)
                && "dragonfire_ward".equals(Inv.idOf(player.getOffHandStack()))) {
            damage = player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE) ? 0 : Math.min(damage, player.getRandom().nextInt(11));
        }
        return damage;
    }

    @Override
    protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
        if (acidTicks > 0) {
            acidTicks--;
            for (Vec3d a : acid) {
                BossFx.world(boss).spawnParticles(ParticleTypes.SNEEZE, a.x, a.y + 0.1, a.z, 2, 0.4, 0.02, 0.4, 0);
            }
            if (target != null) {
                strike(boss, target.getPos(), 2, ZulrahVorkath.VORKATH_RAPID_FIRE.maxHit(), 0.9, ParticleTypes.SMALL_FLAME);
                for (Vec3d a : acid) {
                    if (target.getPos().squaredDistanceTo(a) < 1.0) {
                        int dmg = 1 + boss.getRandom().nextInt(10);
                        CombatHandler.applyDamage(boss, target, dmg);
                        boss.heal(dmg);
                        break;
                    }
                }
            }
            if (acidTicks == 0) {
                acid.clear();
            }
        }
        if (spawn != null) {
            if (!spawn.isAlive()) {
                if (frozen != null) {
                    BossFx.unstun(frozen);
                    com.runecraft.util.Msg.game(frozen, "The ice around you shatters as the spawn dies.");
                }
                spawn = null;
                frozen = null;
                cooldown = 2;
            } else if (frozen != null && spawn.squaredDistanceTo(frozen) < 2.25) {
                BossFx.burst(spawn, ParticleTypes.EXPLOSION, 4, 0.5);
                BossFx.sound(spawn, SoundEvents.ENTITY_GENERIC_EXPLODE, 1.2f);
                CombatHandler.applyDamage(boss, frozen, 60 + boss.getRandom().nextInt(21));
                BossFx.unstun(frozen);
                spawn.discard();
            }
        }
    }

    @Override
    public void onDeath(DataBossEntity boss, ServerPlayerEntity killer) {
        if (spawn != null) {
            spawn.discard();
        }
        if (frozen != null) {
            BossFx.unstun(frozen);
        }
        int kc = com.runecraft.player.Players.data(killer).killCount("vorkath");
        if (kc == 50) {
            Inv.give(killer, "vorkaths_head", 1);
            com.runecraft.collection.CollectionLog.obtain(killer, "vorkaths_head");
            com.runecraft.util.Msg.game(killer, "You are rewarded with Vorkath's head for your 50th kill.");
        }
    }
}
