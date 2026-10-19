package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.combat.CombatHandler;
import com.runecraft.content.systems.CombatAchievementService;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.bosses.ZulrahVorkath;
import com.runecraft.osrs.content.systems.CombatAchievements;
import com.runecraft.util.Inv;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Vorkath (OSRS Wiki): six standard attacks (claw, bone spike, magic, dragonfire variants, dragonfire bomb), then a
 * special, alternating between the acid phase (acid pools everywhere and a fireball every tick at your tile for 25-41
 * regardless of protection; stepping in acid deals up to 10 that Vorkath leeches; he takes half damage meanwhile) and
 * the frozen phase (ice breath freezes you and a zombified spawn crawls towards you; Vorkath is immune to damage until
 * it dies or explodes, for up to 60 scaled by its remaining hitpoints; Crumble Undead kills it outright).
 */
final class VorkathScript extends ScriptedBoss {
    /** Live zombified spawns (Crumble Undead kills them in one cast). */
    private static final java.util.Set<net.minecraft.world.entity.LivingEntity> SPAWNS =
            java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    private boolean acidNext;
    private int acidTicks;
    private final List<Vec3> acid = new ArrayList<>();
    private Mob spawn;
    private ServerPlayer frozen;
    private int normalsSinceSpecial;
    /** "The Walk": hits on Vorkath per player this acid phase, and who was hit by rapid fire or acid. */
    private final java.util.Map<java.util.UUID, Integer> walkHits = new java.util.HashMap<>();
    private final java.util.Set<java.util.UUID> walkFailed = new java.util.HashSet<>();

    static void registerCrumbleUndead() {
        com.runecraft.combat.CombatHooks.addModifier(new com.runecraft.combat.CombatHooks.PlayerAttackModifier() {
            @Override
            public int afterRoll(ServerPlayer player, net.minecraft.world.entity.LivingEntity target, AttackType type, int damage) {
                if (target != null && type == AttackType.MAGIC && SPAWNS.contains(target)
                        && "crumble_undead".equals(com.runecraft.content.systems.SysState.of(player).autocast)) {
                    CombatAchievementService.flag(player, CombatAchievements.FLAG_VORKATH_CRUMBLE);
                    return Math.max(damage, (int) Math.ceil(target.getHealth()));
                }
                return damage;
            }
        });
    }

    @Override
    public void onSpawn(DataBossEntity boss) {
        acidNext = boss.getRandom().nextBoolean();
        BossFx.say(boss, "Vorkath awakens from its icy slumber!");
    }

    @Override
    protected boolean driven() {
        return true;
    }

    private boolean spawnAlive() {
        return spawn != null && spawn.isAlive();
    }

    @Override
    protected boolean canAttack(DataBossEntity boss) {
        return acidTicks <= 0 && !spawnAlive();
    }

    @Override
    public boolean isVulnerable(DataBossEntity boss) {
        return !spawnAlive();
    }

    @Override
    protected int incoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
        if (acidTicks > 0) {
            if (attacker != null && damage > 0) {
                walkHits.merge(attacker.getUUID(), 1, Integer::sum);
            }
            return (int) Math.floor(damage * ZulrahVorkath.ACID_PHASE_DAMAGE);
        }
        return damage;
    }

    @Override
    protected AttackDef nextAttack(DataBossEntity boss, ServerPlayer target, double distance) {
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

    private void startAcid(DataBossEntity boss, ServerPlayer target) {
        BossFx.say(boss, "Vorkath spews acid across the island! Keep moving!");
        acid.clear();
        acid.add(target.position()); // one pool always lands where the player stood
        for (int i = 0; i < 27; i++) {
            acid.add(around(boss, homeVec(), 11));
        }
        acidTicks = ZulrahVorkath.RAPID_FIRE_SHOTS;
        cooldown = ZulrahVorkath.RAPID_FIRE_SHOTS + 2;
        walkHits.clear();
        walkFailed.clear();
    }

    private void startSpawn(DataBossEntity boss, ServerPlayer target) {
        perform(boss, target, ZulrahVorkath.VORKATH_FREEZE);
        Vec3 dir = target.position().subtract(boss.position()).normalize();
        Vec3 at = BossFx.ground(BossFx.world(boss), target.getX() + dir.z * 8, target.getZ() - dir.x * 8, target.getY()); // approx: 10 tiles
        spawn = BossFx.spawnMinion(boss, "zombified_spawn", at, target);
        if (spawn != null) {
            SPAWNS.add(spawn);
        }
        frozen = target;
        BossFx.sound(boss, SoundEvents.ZOMBIE_AMBIENT, 0.6f);
    }

    @Override
    public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
        if (last == ZulrahVorkath.VORKATH_PINK_FIRE) {
            BossFx.disablePrayers(player, "Vorkath's pink dragonfire turns off your prayers!");
        }
        if ((last == ZulrahVorkath.VORKATH_DRAGONFIRE || last == ZulrahVorkath.VORKATH_PINK_FIRE)
                && "dragonfire_ward".equals(Inv.idOf(player.getOffhandItem()))) {
            damage = player.hasEffect(MobEffects.FIRE_RESISTANCE) ? 0 : Math.min(damage, player.getRandom().nextInt(11));
        }
        return damage;
    }

    @Override
    protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
        if (acidTicks > 0) {
            acidTicks--;
            for (Vec3 a : acid) {
                BossFx.world(boss).sendParticles(ParticleTypes.SNEEZE, a.x, a.y + 0.1, a.z, 2, 0.4, 0.02, 0.4, 0);
            }
            if (target != null) {
                strike(boss, target.position(), 2, 0, 0.9, ParticleTypes.SMALL_FLAME, (hit, pos) -> {
                    for (ServerPlayer h : hit) {
                        walkFailed.add(h.getUUID());
                        com.runecraft.combat.CombatHooks.nameAttack("rapid_fire", AttackType.MAGIC);
                        CombatHandler.applyDamage(boss.isAlive() ? boss : null, h, ZulrahVorkath.RAPID_FIRE_MIN
                                + h.getRandom().nextInt(ZulrahVorkath.VORKATH_RAPID_FIRE.maxHit() - ZulrahVorkath.RAPID_FIRE_MIN + 1));
                    }
                });
                for (Vec3 a : acid) {
                    if (target.position().distanceToSqr(a) < 1.0) {
                        int dmg = 1 + boss.getRandom().nextInt(ZulrahVorkath.ACID_MAX);
                        walkFailed.add(target.getUUID());
                        com.runecraft.combat.CombatHooks.nameAttack("acid_pool", null);
                        CombatHandler.applyDamage(boss, target, dmg);
                        boss.heal(mcHp(boss, dmg));
                        break;
                    }
                }
            }
            if (acidTicks == 0) {
                acid.clear();
                for (java.util.Map.Entry<java.util.UUID, Integer> e : walkHits.entrySet()) {
                    if (e.getValue() >= ZulrahVorkath.THE_WALK_HITS && !walkFailed.contains(e.getKey())
                            && BossFx.world(boss).getPlayerByUUID(e.getKey()) instanceof ServerPlayer p) {
                        CombatAchievementService.flag(p, CombatAchievements.FLAG_VORKATH_WALK);
                    }
                }
            }
        }
        if (spawn != null) {
            if (!spawn.isAlive()) {
                if (frozen != null) {
                    BossFx.unstun(frozen);
                    com.runecraft.util.Msg.game(frozen, "The ice around you shatters as the spawn dies.");
                }
                SPAWNS.remove(spawn);
                spawn = null;
                frozen = null;
                cooldown = 2;
            } else if (frozen != null && spawn.distanceToSqr(frozen) < 2.25) {
                BossFx.burst(spawn, ParticleTypes.EXPLOSION, 4, 0.5);
                BossFx.sound(spawn, SoundEvents.GENERIC_EXPLODE.value(), 1.2f);
                com.runecraft.combat.CombatHooks.nameAttack("spawn_explosion", null);
                CombatHandler.applyDamage(boss, frozen, ZulrahVorkath.spawnExplosion(spawn.getHealth(), spawn.getMaxHealth()));
                BossFx.unstun(frozen);
                SPAWNS.remove(spawn);
                spawn.discard();
            }
        }
    }

    @Override
    public void onDeath(DataBossEntity boss, ServerPlayer killer) {
        if (spawn != null) {
            SPAWNS.remove(spawn);
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
