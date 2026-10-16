package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.combat.CombatHandler;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.bosses.ClassicBosses;
import com.runecraft.osrs.content.bosses.SkotizoRules;
import com.runecraft.skill.XpService;
import com.runecraft.collection.CollectionLog;
import com.runecraft.content.systems.CombatAchievementService;
import com.runecraft.osrs.content.systems.CombatAchievements;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** Scripts for the classic bosses (Mole, Kalphite Queen, Dagannoth Kings, Corp, Skotizo, Hespori, Scurrius, Zalcano, Sol). */
final class ClassicScripts {
    private ClassicScripts() {
    }

    /** Giant Mole: below 50% it may burrow away to another part of its lair, snuffing out light sources. */
    static final class GiantMole extends ScriptedBoss {
        private int burrowIn = -1;
        private int burrowCooldown;
        /** Game ticks left of the "just resurfaced" window (Whack-a-Mole: a kill within 10 seconds). */
        private int surfacedFor;

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            // OSRS Wiki: each attack has a 1/4 chance to make her burrow while she is between 50% and 5% of her hitpoints.
            double hp = percent(boss);
            if (hp < 50 && hp > 5 && burrowIn < 0 && burrowCooldown == 0 && boss.getRandom().nextInt(4) == 0) {
                burrowIn = 2;
                BossFx.warn(boss, "The Giant Mole starts to dig...");
            }
            return damage;
        }

        @Override
        public boolean isVulnerable(DataBossEntity boss) {
            return burrowIn < 0;
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            if (burrowCooldown > 0) {
                burrowCooldown--;
            }
            if (surfacedFor > 0 && --surfacedFor == 0) {
                CombatAchievementService.unflagNear(boss, CombatAchievements.FLAG_MOLE_SURFACED);
            }
            if (burrowIn > 0) {
                burrowIn--;
                BossFx.burst(boss, ParticleTypes.CAMPFIRE_COSY_SMOKE, 12, 1.0);
                if (burrowIn == 0) {
                    Vec3d to = around(boss, homeVec(), 10);
                    boss.requestTeleport(to.x, to.y, to.z);
                    BossFx.sound(boss, SoundEvents.BLOCK_GRAVEL_BREAK, 0.6f);
                    for (ServerPlayerEntity p : BossFx.players(boss, 24)) {
                        p.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0, false, false));
                    }
                    BossFx.say(boss, "The Giant Mole digs away! Your light source is extinguished.");
                    CombatAchievementService.countNear(boss, CombatAchievements.COUNT_MOLE_BURROW);
                    CombatAchievementService.flagNear(boss, CombatAchievements.FLAG_MOLE_SURFACED);
                    surfacedFor = 17; // 10 seconds
                    burrowIn = -1;
                    burrowCooldown = 15;
                }
            }
        }
    }

    /** Kalphite Queen (crawling): Protect from Magic and Missiles (high magic/ranged defence); at 0 hp she takes flight. */
    static final class KalphiteQueen extends ScriptedBoss {
        private boolean transforming;
        private int wait;

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            if (transforming) {
                return 0;
            }
            // Her protection prayers only raise her defence (OSRS Wiki), which her defence bonuses already model: no
            // extra damage reduction.
            if (damage >= boss.getHealth()) {
                damage = (int) Math.ceil(boss.getHealth()) - 1;
                transforming = true;
                wait = 3;
                BossFx.say(boss, "The Kalphite Queen's shell splits open...");
            }
            return damage;
        }

        @Override
        public boolean isVulnerable(DataBossEntity boss) {
            return !transforming;
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            if (transforming) {
                BossFx.burst(boss, ParticleTypes.CRIT, 20, 1.2);
                if (--wait <= 0) {
                    BossFx.say(boss, "The Kalphite Queen emerges from her shell and takes to the air!");
                    BossFx.swapForm(boss, "kalphite_queen_airborne", boss.getBlockPos(), true, null);
                }
            }
        }
    }

    /** Kalphite Queen (airborne): Protect from Melee (high melee defence, no damage cut); lightning chains to everyone nearby. */
    static final class KalphiteQueenAirborne extends ScriptedBoss {

        @Override
        protected String creditAs() {
            return "kalphite_queen";
        }

        /** A fresh form: Defence lowered on the grounded form no longer counts for "Chitin Penetrator". */
        @Override
        public void onSpawn(DataBossEntity boss) {
            CombatAchievementService.unflagNear(boss, CombatAchievements.FLAG_DEFENCE_LOWERED);
        }
    }

    /** Dagannoth Rex brings his brothers: Prime and Supreme spawn alongside him. */
    static final class DagannothRex extends ScriptedBoss {
        @Override
        public void onSpawn(DataBossEntity boss) {
            ServerPlayerEntity target = boss.getTarget() instanceof ServerPlayerEntity p ? p : null;
            // The lair keeps the Kings present (dungeons module): only wake the brothers that aren't already there.
            for (String brother : new String[]{"dagannoth_prime", "dagannoth_supreme"}) {
                net.minecraft.entity.EntityType<DataBossEntity> type = com.runecraft.registry.ModEntities.DATA_BOSS_TYPES.get(brother);
                if (type != null && !boss.getWorld().getEntitiesByType(type, boss.getBoundingBox().expand(48), e -> e.isAlive()).isEmpty()) {
                    continue;
                }
                BossFx.spawnBoss(boss, brother, boss.getBlockPos().add(brother.endsWith("prime") ? -4 : 4, 0, 3), target);
            }
            BossFx.say(boss, "The Dagannoth Kings rise! Rex resists melee, Prime resists magic and Supreme resists ranged.");
        }
    }

    /**
     * Corporeal Beast: only stab attacks with a spear or halberd deal full damage (everything else is halved); stomps
     * anyone standing beneath it. The dark energy core (OSRS Wiki): a 1/8 chance after it takes a hit of 32 or more, or
     * when it attacks below 1,000 hitpoints; the core hits players next to it for 5-13 every 2 ticks, healing the Beast
     * for half, and jumps to a player when nobody is next to it.
     */
    static final class Corp extends ScriptedBoss {
        private MobEntity core;
        private boolean coreRoll;
        private int coreTick;

        @Override
        public AttackDef chooseAttack(DataBossEntity boss, ServerPlayerEntity target, double distance) {
            if (boss.getHealth() < mcHp(boss, 1000)) {
                coreRoll = true; // rolled when it attacks below 1,000 hitpoints
            }
            double dx = target.getX() - boss.getX();
            double dz = target.getZ() - boss.getZ();
            return dx * dx + dz * dz < boss.getWidth() * boss.getWidth() / 4 ? ClassicBosses.CORP_STOMP : null;
        }

        static boolean spearOrHalberd(String id) {
            return id.contains("spear") || id.contains("halberd") || id.contains("hasta") || id.endsWith("_lance");
        }

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            int d = type == AttackType.STAB && spearOrHalberd(BossFx.heldId(attacker)) ? damage : damage / 2;
            if (d >= 32) {
                coreRoll = true;
            }
            return d;
        }

        private boolean coreAlive() {
            return core != null && core.isAlive();
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            if (coreRoll) {
                coreRoll = false;
                if (!coreAlive() && target != null && boss.getRandom().nextInt(8) == 0) {
                    BossFx.say(boss, "The Corporeal Beast releases a dark energy core!");
                    core = BossFx.spawnMinion(boss, "dark_energy_core", around(boss, target.getPos(), 2), target);
                }
            }
            if (!coreAlive() || ++coreTick % 2 != 0) {
                return;
            }
            List<ServerPlayerEntity> adjacent = BossFx.world(boss).getPlayers(p -> p.isAlive() && !p.isSpectator()
                    && p.squaredDistanceTo(core) < 2.0 * 2.0);
            if (adjacent.isEmpty()) {
                ServerPlayerEntity to = target != null ? target : null;
                for (ServerPlayerEntity p : BossFx.players(boss, 24)) {
                    // The core prefers the northernmost player (then the easternmost).
                    if (to == null || p.getZ() < to.getZ() - 1 || Math.abs(p.getZ() - to.getZ()) <= 1 && p.getX() > to.getX()) {
                        to = p;
                    }
                }
                if (to != null) {
                    Vec3d at = to.getPos().add(1, 0, 0);
                    core.requestTeleport(at.x, at.y, at.z);
                    BossFx.burst(core, ParticleTypes.SOUL, 10, 0.5);
                }
                return;
            }
            for (ServerPlayerEntity p : adjacent) {
                int dmg = 5 + p.getRandom().nextInt(9);
                com.runecraft.combat.CombatHooks.nameAttack("dark_core", null);
                CombatHandler.applyDamage(core, p, dmg);
                if (boss.isAlive()) {
                    boss.heal(mcHp(boss, dmg / 2));
                }
            }
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayerEntity killer) {
            if (coreAlive()) {
                core.discard();
            }
        }
    }

    /**
     * Skotizo (OSRS wiki): he randomly awakens the four altars of his lair; each awakened altar cuts the damage he takes
     * by 25% (15% with Arclight/Emberlight, which also break an altar in one hit). Below a third of his hitpoints he
     * summons three reanimated demon spawns (again whenever they are all slain), and while an altar is active a random
     * timer summons a dark ankou, one at a time.
     */
    static final class Skotizo extends ScriptedBoss {
        private final MobEntity[] altars = new MobEntity[SkotizoRules.ALTARS];
        private final List<MobEntity> spawns = new ArrayList<>();
        private MobEntity ankou;
        private int altarTimer = -1;
        private int ankouTimer = -1;
        private int resummon = -1;
        private boolean summoned;

        private static final int[][] SPOTS = {{0, -9}, {9, 0}, {0, 9}, {-9, 0}};

        /** Arclight and Emberlight disable an awakened altar in one hit (registered once by the module). */
        static void registerAltarBreaker() {
            com.runecraft.combat.CombatHooks.addModifier(new com.runecraft.combat.CombatHooks.PlayerAttackModifier() {
                @Override
                public int afterRoll(ServerPlayerEntity player, net.minecraft.entity.LivingEntity target, AttackType type, int damage) {
                    if (target != null && target.getType() == com.runecraft.registry.ModEntities.typeById("awakened_altar")
                            && SkotizoRules.altarBreaker(BossFx.heldId(player))) {
                        return Math.max(damage, (int) Math.ceil(target.getHealth()));
                    }
                    return damage;
                }
            });
        }

        int awake() {
            int n = 0;
            for (MobEntity a : altars) {
                if (a != null && a.isAlive()) {
                    n++;
                }
            }
            return n;
        }

        private void awaken(DataBossEntity boss, int i) {
            Vec3d c = homeVec();
            altars[i] = BossFx.spawnMinion(boss, "awakened_altar", BossFx.ground(BossFx.world(boss), c.x + SPOTS[i][0], c.z + SPOTS[i][1],
                    c.y), null);
        }

        private int between(DataBossEntity boss, int min, int max) {
            return min + boss.getRandom().nextInt(max - min + 1);
        }

        @Override
        public void onSpawn(DataBossEntity boss) {
            home = boss.getBlockPos();
            altarTimer = between(boss, SkotizoRules.ALTAR_MIN_TICKS, SkotizoRules.ALTAR_MAX_TICKS);
            BossFx.say(boss, "Skotizo awakens! Destroy the altars he awakens, or they will shield him.");
        }

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            boolean breaker = attacker != null && SkotizoRules.altarBreaker(BossFx.heldId(attacker));
            return SkotizoRules.damage(damage, awake(), breaker);
        }

        @Override
        public void onPhase(DataBossEntity boss, int phase) {
            summoned = true;
            summonSpawns(boss);
        }

        private void summonSpawns(DataBossEntity boss) {
            spawns.removeIf(m -> !m.isAlive());
            ServerPlayerEntity target = target(boss);
            Vec3d c = boss.getPos();
            for (int i = spawns.size(); i < SkotizoRules.DEMON_SPAWNS; i++) {
                double a = Math.PI * 2 * i / SkotizoRules.DEMON_SPAWNS;
                MobEntity m = BossFx.spawnMinion(boss, "reanimated_demon_spawn",
                        BossFx.ground(BossFx.world(boss), c.x + Math.cos(a) * 2.5, c.z + Math.sin(a) * 2.5, c.y), target);
                if (m != null) {
                    spawns.add(m);
                }
            }
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            int awake = awake();
            // "Demonic Weakening": the kill counts while no altar is active.
            if (awake == 0) {
                CombatAchievementService.flagNear(boss, CombatAchievements.FLAG_SKOTIZO_NO_ALTARS);
            } else {
                CombatAchievementService.unflagNear(boss, CombatAchievements.FLAG_SKOTIZO_NO_ALTARS);
            }
            if (altarTimer > 0 && --altarTimer == 0) {
                altarTimer = between(boss, SkotizoRules.ALTAR_MIN_TICKS, SkotizoRules.ALTAR_MAX_TICKS);
                List<Integer> dormant = new ArrayList<>();
                for (int i = 0; i < altars.length; i++) {
                    if (altars[i] == null || !altars[i].isAlive()) {
                        dormant.add(i);
                    }
                }
                if (!dormant.isEmpty()) {
                    awaken(boss, dormant.get(boss.getRandom().nextInt(dormant.size())));
                    BossFx.say(boss, "Skotizo awakens one of the altars!");
                }
            }
            // A dark ankou timer runs while an altar is active; only one ankou at a time.
            boolean ankouAlive = ankou != null && ankou.isAlive();
            if (awake == 0 || ankouAlive) {
                ankouTimer = -1;
            } else if (ankouTimer < 0) {
                ankouTimer = between(boss, SkotizoRules.ANKOU_MIN_TICKS, SkotizoRules.ANKOU_MAX_TICKS);
            } else if (--ankouTimer <= 0) {
                ankouTimer = -1;
                ankou = BossFx.spawnMinion(boss, "dark_ankou", around(boss, homeVec(), 7), null);
                BossFx.warn(boss, "A dark ankou rises from the shadows.");
            }
            if (summoned) {
                spawns.removeIf(m -> !m.isAlive());
                if (spawns.isEmpty()) {
                    if (resummon < 0) {
                        resummon = SkotizoRules.RESUMMON_TICKS;
                    } else if (--resummon <= 0) {
                        resummon = -1;
                        BossFx.say(boss, "Skotizo: Gar mulno ful taglo!");
                        summonSpawns(boss);
                    }
                }
            }
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayerEntity killer) {
            for (MobEntity a : altars) {
                if (a != null && a.isAlive()) {
                    a.discard();
                }
            }
            spawns.forEach(MobEntity::discard);
            if (ankou != null && ankou.isAlive()) {
                ankou.discard();
            }
            if (boss.getRandom().nextInt(65) == 0) {
                Inv.give(killer, "pet_skotos", 1);
                CollectionLog.obtain(killer, "pet_skotos");
                Msg.red(killer, "You have a funny feeling like you're being followed.");
            }
        }
    }

    /** Hespori: at 2/3 and 1/3 health it becomes invulnerable until its flowers are cut down. */
    static final class Hespori extends ScriptedBoss {
        private final List<MobEntity> flowers = new ArrayList<>();

        @Override
        public void onPhase(DataBossEntity boss, int phase) {
            flowers.clear();
            Vec3d c = boss.getPos();
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                MobEntity f = BossFx.spawnMinion(boss, "hesporis_flower",
                        BossFx.ground(BossFx.world(boss), c.x + Math.cos(a) * 5, c.z + Math.sin(a) * 5, c.y), null);
                if (f != null) {
                    flowers.add(f);
                }
            }
            BossFx.say(boss, "Hespori's flowers bloom, shielding it! Cut them down.");
        }

        @Override
        public boolean isVulnerable(DataBossEntity boss) {
            return flowers.stream().noneMatch(MobEntity::isAlive);
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayerEntity killer) {
            flowers.forEach(MobEntity::discard);
        }
    }

    /** Scurrius: once below 40% he runs to his food pile and eats. */
    static final class Scurrius extends ScriptedBoss {
        private boolean ate;

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            if (!ate && percent(boss) < 40) {
                ate = true;
                BossFx.say(boss, "Scurrius scurries to his food pile and starts to eat!");
                boss.heal(30);
                BossFx.burst(boss, ParticleTypes.HAPPY_VILLAGER, 15, 1.0);
            }
        }
    }

    /** Zalcano: her armour can only be broken by mining it; damage scales with Mining level. */
    static final class Zalcano extends ScriptedBoss {
        @Override
        protected int incoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            String held = BossFx.heldId(attacker);
            if (!held.endsWith("pickaxe")) {
                Msg.action(attacker, "Zalcano's rocky armour can only be broken with a pickaxe.");
                return 0;
            }
            int mining = com.runecraft.player.Players.data(attacker).skills().currentLevel(Skill.MINING);
            int dealt = 1 + boss.getRandom().nextInt(Math.max(1, mining / 6));
            XpService.add(attacker, Skill.MINING, dealt * 2.5);
            BossFx.burst(boss, ParticleTypes.LAVA, 4, 0.6);
            return dealt;
        }
    }

}
