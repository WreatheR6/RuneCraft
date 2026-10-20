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
 * Vorkath (OSRS Wiki "Vorkath/Strategies"): six standard attacks (claw when next to it, bone spike, magic, dragonfire
 * variants, the deadly dragonfire bomb), then a special, alternating between the acid phase (acid pools everywhere, one on
 * the player's tile, and a fireball every tick at the player's tile for 25-41 regardless of protection; stepping in acid
 * deals up to 10 that Vorkath leeches; it takes half damage meanwhile) and the frozen phase (ice breath freezes the player
 * and a zombified spawn lands ten tiles away and crawls towards them; Vorkath is immune to damage until it dies or
 * explodes, for up to 60 scaled by its remaining hitpoints; Crumble Undead kills it outright). The bomb hits the player's
 * tile for up to 121 and the tiles next to it for half. Dragon Slayer II's version fights the same way with its own max
 * hits (28 ranged, 72 dragonfire, a 30 spawn explosion).
 */
final class VorkathScript extends ScriptedBoss {
    /** Live zombified spawns (Crumble Undead kills them in one cast). */
    private static final java.util.Set<net.minecraft.world.entity.LivingEntity> SPAWNS =
            java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    /** Dragon Slayer II's quest version (lower max hits, no kill count head). */
    private final boolean quest;
    private boolean acidNext;
    private int acidTicks;
    private final List<Vec3> acid = new ArrayList<>();
    private Mob spawn;
    private ServerPlayer frozen;
    private int normalsSinceSpecial;
    /** Specials made this fight: acid phases and zombified spawns (self-test). */
    int acidPhases;
    int spawnPhases;
    /** "The Walk": hits on Vorkath per player this acid phase, and who was hit by rapid fire or acid. */
    private final java.util.Map<java.util.UUID, Integer> walkHits = new java.util.HashMap<>();
    private final java.util.Set<java.util.UUID> walkFailed = new java.util.HashSet<>();

    VorkathScript() {
        this(false);
    }

    VorkathScript(boolean quest) {
        this.quest = quest;
    }

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

    /**
     * Vorkath's kill loot (registered as its loot roller): its table rolled as any boss's, except that the 50th kill always
     * drops Vorkath's head, and only one (OSRS Wiki, Mod Ash: "there's only a single roll, with its RNG skipped for the 50th
     * kill").
     */
    static List<com.runecraft.skill.Drops.Rolled> rollLoot(ServerPlayer killer, DataBossEntity boss,
            List<com.runecraft.osrs.data.Drop> table) {
        int kc = com.runecraft.player.Players.data(killer).killCount("vorkath") + 1; // the kill count is added after the loot
        List<com.runecraft.osrs.data.Drop> lines = new ArrayList<>(com.runecraft.osrs.content.bosses.BossLoot.pick("vorkath", table,
                boss.getRandom()::nextInt));
        if (kc == ZulrahVorkath.HEAD_KILL) {
            lines.removeIf(d -> d.item().equals(ZulrahVorkath.HEAD));
            lines.add(com.runecraft.osrs.data.Drop.always(ZulrahVorkath.HEAD));
            com.runecraft.util.Msg.game(killer, "You are rewarded with Vorkath's head for your 50th kill.");
        }
        return com.runecraft.skill.Drops.resolve(lines, boss.getRandom());
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

    boolean acidPhase() {
        return acidTicks > 0;
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
        bomb(boss, target);
        return null;
    }

    /** The quest version's lower max hits (OSRS Wiki infobox: 28 ranged, 72 dragonfire). */
    private double questScale(AttackDef a) {
        if (!quest) {
            return 1.0;
        }
        if (a == ZulrahVorkath.VORKATH_SPIKE) {
            return (double) ZulrahVorkath.DS2_RANGED_MAX / a.maxHit();
        }
        if (a == ZulrahVorkath.VORKATH_DRAGONFIRE || a == ZulrahVorkath.VORKATH_PINK_FIRE || a == ZulrahVorkath.VORKATH_VENOM_FIRE) {
            return (double) ZulrahVorkath.DS2_DRAGONFIRE_MAX / ZulrahVorkath.VORKATH_DRAGONFIRE.maxHit();
        }
        return 1.0;
    }

    @Override
    protected void perform(DataBossEntity boss, ServerPlayer target, AttackDef a) {
        last = a;
        attackCount++;
        com.runecraft.boss.Mechanics.perform(boss, target, a, multiplier(boss) * boss.variantHitScale(a) * questScale(a));
    }

    /**
     * The deadly dragonfire bomb, launched at the player's tile: up to 121 there, half next to it, regardless of dragonfire
     * protection (OSRS Wiki). Package-private for the self-test.
     */
    void bomb(DataBossEntity boss, ServerPlayer target) {
        com.runecraft.boss.Mechanics.attackListener.onAttack(boss, target, ZulrahVorkath.VORKATH_BOMB);
        last = ZulrahVorkath.VORKATH_BOMB;
        attackCount++;
        cooldown = Math.max(1, attackDelay(boss, ZulrahVorkath.VORKATH_BOMB)) - 1;
        BossFx.warn(boss, ZulrahVorkath.VORKATH_BOMB.message());
        namedStrike("dragonfire_bomb", boss, target.position(), ZulrahVorkath.VORKATH_BOMB.telegraphTicks(), 0, 3.0,
                ParticleTypes.FLAME, (hit, at) -> {
                    for (ServerPlayer p : hit) {
                        double dx = p.getX() - at.x;
                        double dz = p.getZ() - at.z;
                        int max = ZulrahVorkath.bombMax(maxHit(boss, ZulrahVorkath.VORKATH_BOMB), dx * dx + dz * dz <= 1.0);
                        com.runecraft.combat.CombatHooks.nameAttack("dragonfire_bomb", AttackType.MAGIC);
                        CombatHandler.applyDamage(boss.isAlive() ? boss : null, p, p.getRandom().nextInt(max + 1));
                    }
                });
    }

    /** The acid phase. Package-private for the self-test. */
    void startAcid(DataBossEntity boss, ServerPlayer target) {
        BossFx.say(boss, "Vorkath spews acid across the island! Keep moving!");
        com.runecraft.boss.Mechanics.attackListener.onAttack(boss, target, ZulrahVorkath.VORKATH_RAPID_FIRE);
        acid.clear();
        acid.add(target.position()); // one pool always lands where the player stood
        for (int i = 0; i < 27; i++) {
            acid.add(around(boss, homeVec(), 11));
        }
        acidTicks = ZulrahVorkath.RAPID_FIRE_SHOTS;
        cooldown = ZulrahVorkath.RAPID_FIRE_SHOTS + 2;
        walkHits.clear();
        walkFailed.clear();
        acidPhases++;
    }

    /** The frozen phase: ice breath and a zombified spawn ten tiles away. Package-private for the self-test. */
    void startSpawn(DataBossEntity boss, ServerPlayer target) {
        perform(boss, target, ZulrahVorkath.VORKATH_FREEZE);
        spawn = BossFx.spawnMinion(boss, "zombified_spawn", spawnPoint(boss, target), target);
        if (spawn != null) {
            SPAWNS.add(spawn);
        }
        frozen = target;
        spawnPhases++;
        BossFx.sound(boss, SoundEvents.ZOMBIE_AMBIENT, 0.6f);
    }

    /**
     * Where the zombified spawn lands: ten tiles (twenty blocks) from the player, across the island; pulled in to stay inside
     * its ice ring (approx: the arena here is smaller than Ungael's crater).
     */
    private Vec3 spawnPoint(DataBossEntity boss, ServerPlayer target) {
        Vec3 c = homeVec();
        Vec3 dir = new Vec3(c.x - target.getX(), 0, c.z - target.getZ());
        if (dir.lengthSqr() < 1.0E-4) {
            dir = target.position().subtract(boss.position()).multiply(1, 0, 1);
        }
        dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : dir.normalize();
        double reach = Math.max(4.0, (boss.def() == null ? 13 : boss.def().arenaRadius) - 2.0);
        double best = 4.0;
        for (double d = ZulrahVorkath.SPAWN_DISTANCE_BLOCKS; d >= 4.0; d -= 1.0) {
            double x = target.getX() + dir.x * d - c.x;
            double z = target.getZ() + dir.z * d - c.z;
            if (x * x + z * z <= reach * reach) {
                best = d;
                break;
            }
        }
        return BossFx.ground(BossFx.world(boss), target.getX() + dir.x * best, target.getZ() + dir.z * best, target.getY());
    }

    Mob spawn() {
        return spawn;
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
                int max = quest ? ZulrahVorkath.DS2_SPAWN_EXPLOSION_MAX : ZulrahVorkath.SPAWN_EXPLOSION_MAX;
                CombatHandler.applyDamage(boss, frozen, ZulrahVorkath.spawnExplosion(spawn.getHealth(), spawn.getMaxHealth(), max));
                BossFx.unstun(frozen);
                SPAWNS.remove(spawn);
                spawn.discard();
            }
        }
    }

    @Override
    public void onDeath(DataBossEntity boss, ServerPlayer killer) {
        onRemoved(boss);
    }

    /** Killed or reset: his zombified spawn goes and a player he froze can move again. */
    @Override
    public void onRemoved(DataBossEntity boss) {
        if (spawn != null) {
            SPAWNS.remove(spawn);
            spawn.discard();
        }
        if (frozen != null) {
            BossFx.unstun(frozen);
        }
    }
}
