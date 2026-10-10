package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.combat.CombatHandler;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.bosses.Dt2Bosses;
import com.runecraft.osrs.content.bosses.MinigameBosses;
import com.runecraft.osrs.content.bosses.VarlamoreBosses;
import com.runecraft.util.Msg;
import net.minecraft.block.Blocks;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** Scripts for the Gauntlet, Nightmare, Muspah, Desert Treasure II and Varlamore bosses. */
final class EncounterScripts {
    private EncounterScripts() {
    }

    private enum Category { MELEE, RANGED, MAGIC }

    private static Category category(AttackType t) {
        return t.isMelee() ? Category.MELEE : t == AttackType.RANGED ? Category.RANGED : Category.MAGIC;
    }

    /**
     * Crystalline/Corrupted Hunllef: switches between ranged and magic every 4 attacks (announced),
     * protects itself from one combat style and only switches after 6 hits from another style, stomps
     * anyone underneath it, sends chasing tornadoes and (corrupted) corrupts floor tiles.
     */
    static final class Hunllef extends ScriptedBoss {
        private final boolean corrupted;
        private boolean magic;
        private int styleAttacks;
        private Category protectedStyle = Category.MELEE;
        private int hitsTaken;
        private final List<MobEntity> tornadoes = new ArrayList<>();
        private int tornadoLife;
        private int tornadoTimer = 20;
        private int tileTimer = 10;

        Hunllef(boolean corrupted) {
            this.corrupted = corrupted;
        }

        @Override
        public void onSpawn(DataBossEntity boss) {
            BossFx.say(boss, "The Hunllef protects itself from melee. Land 6 hits with another style to make it switch.");
        }

        @Override
        protected boolean driven() {
            return true;
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayerEntity target, double distance) {
            double dx = target.getX() - boss.getX();
            double dz = target.getZ() - boss.getZ();
            if (dx * dx + dz * dz < boss.getWidth() * boss.getWidth() / 4) {
                return MinigameBosses.HUNLLEF_STOMP;
            }
            if (styleAttacks >= MinigameBosses.HUNLLEF_ATTACKS_PER_STYLE) {
                styleAttacks = 0;
                magic = !magic;
                BossFx.warn(boss, "The Hunllef switches to " + (magic ? "magic" : "ranged") + " attacks!");
                BossFx.burst(boss, magic ? ParticleTypes.WITCH : ParticleTypes.CRIT, 20, 1.0);
            }
            styleAttacks++;
            if (magic) {
                if (boss.getRandom().nextInt(6) == 0) {
                    return MinigameBosses.HUNLLEF_PRAYER_DISABLE;
                }
                return corrupted ? MinigameBosses.CORRUPTED_MAGIC : MinigameBosses.HUNLLEF_MAGIC;
            }
            return corrupted ? MinigameBosses.CORRUPTED_RANGED : MinigameBosses.HUNLLEF_RANGED;
        }

        @Override
        public int onHitPlayer(DataBossEntity boss, ServerPlayerEntity player, int damage, AttackType type) {
            if (last == MinigameBosses.HUNLLEF_PRAYER_DISABLE) {
                BossFx.disablePrayers(player, "The Hunllef's magic attack switches off your prayers!");
            }
            return damage;
        }

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            Category c = category(type);
            if (c == protectedStyle) {
                Msg.action(attacker, "The Hunllef's prayer protects it from " + c.name().toLowerCase() + "!");
                return 0;
            }
            if (++hitsTaken >= MinigameBosses.HUNLLEF_HITS_PER_PRAYER) {
                hitsTaken = 0;
                protectedStyle = c;
                BossFx.say(boss, "The Hunllef switches its prayer to protect from " + c.name().toLowerCase() + "!");
            }
            return damage;
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            if (target == null) {
                return;
            }
            BossFx.world(boss).spawnParticles(switch (protectedStyle) {
                case MELEE -> ParticleTypes.CRIT;
                case RANGED -> ParticleTypes.HAPPY_VILLAGER;
                case MAGIC -> ParticleTypes.WITCH;
            }, boss.getX(), boss.getY() + boss.getHeight() + 0.5, boss.getZ(), 3, 0.3, 0.1, 0.3, 0);
            int count = corrupted ? 2 : 1;
            if (percent(boss) < 66) {
                count++;
            }
            if (percent(boss) < 33) {
                count++;
            }
            if (--tornadoTimer <= 0) {
                tornadoTimer = 25;
                tornadoLife = 14;
                BossFx.warn(boss, "The Hunllef summons tornadoes!");
                for (int i = 0; i < count; i++) {
                    MobEntity t = BossFx.spawnMinion(boss, "hunllef_tornado", around(boss, homeVec(), 8), null);
                    if (t != null) {
                        tornadoes.add(t);
                    }
                }
            }
            if (tornadoLife > 0) {
                tornadoLife--;
                for (MobEntity t : tornadoes) {
                    Vec3d d = target.getPos().subtract(t.getPos());
                    double len = d.horizontalLength();
                    if (len < 1.2) {
                        CombatHandler.applyDamage(boss, target, (corrupted ? 15 : 10) + boss.getRandom().nextInt(11));
                    } else {
                        Vec3d step = new Vec3d(d.x / len, 0, d.z / len);
                        t.requestTeleport(t.getX() + step.x, t.getY(), t.getZ() + step.z);
                    }
                }
                if (tornadoLife == 0) {
                    tornadoes.forEach(MobEntity::discard);
                    tornadoes.clear();
                }
            }
            if (corrupted && --tileTimer <= 0) {
                tileTimer = 12;
                BossFx.warn(boss, "The floor begins to corrupt!");
                for (int i = 0; i < 7; i++) {
                    strike(boss, around(boss, homeVec(), 10), 4, 20, 1.4, ParticleTypes.CRIMSON_SPORE);
                }
            }
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayerEntity killer) {
            tornadoes.forEach(MobEntity::discard);
        }
    }

    /**
     * The Nightmare / Phosani's Nightmare: shielded at the start of each phase until four totems are
     * charged (destroyed); husks bind you until killed, parasites hatch from players, and the curse
     * switches off protection prayers.
     */
    static final class Nightmare extends ScriptedBoss {
        private final List<MobEntity> totems = new ArrayList<>();
        private final List<MobEntity> husks = new ArrayList<>();
        private boolean shielded;
        private int parasiteIn = -1;
        private ServerPlayerEntity host;
        private int curseTimer = 40;

        @Override
        public void onSpawn(DataBossEntity boss) {
            home = boss.getBlockPos();
            raiseShield(boss);
        }

        private void raiseShield(DataBossEntity boss) {
            totems.forEach(MobEntity::discard);
            totems.clear();
            Vec3d c = homeVec();
            for (int[] o : new int[][]{{-9, -9}, {9, -9}, {-9, 9}, {9, 9}}) {
                MobEntity t = BossFx.spawnMinion(boss, "nightmare_totem", BossFx.ground(BossFx.world(boss), c.x + o[0], c.z + o[1], c.y), null);
                if (t != null) {
                    totems.add(t);
                }
            }
            shielded = true;
            BossFx.say(boss, "The Nightmare is shielded! Charge the four totems to break it.");
        }

        @Override
        public boolean isVulnerable(DataBossEntity boss) {
            return !shielded;
        }

        @Override
        public void onPhase(DataBossEntity boss, int phase) {
            raiseShield(boss);
            ServerPlayerEntity target = target(boss);
            if (target != null) {
                husks.clear();
                for (int i = 0; i < 2; i++) {
                    MobEntity h = BossFx.spawnMinion(boss, "nightmare_husk", around(boss, target.getPos(), 2), target);
                    if (h != null) {
                        husks.add(h);
                    }
                }
                BossFx.stun(target, 30);
                Msg.red(target, "Husks grab hold of you! Kill them to break free.");
            }
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            if (shielded) {
                BossFx.ring(BossFx.world(boss), boss.getPos(), 2.0, ParticleTypes.WITCH);
                if (totems.stream().noneMatch(MobEntity::isAlive)) {
                    shielded = false;
                    BossFx.say(boss, "The totems blast the Nightmare, shattering her shield!");
                    float blast = mcHp(boss, 800);
                    boss.setHealth(Math.max(1, boss.getHealth() - Math.min(blast, boss.getMaxHealth() * 0.1f)));
                    BossFx.sound(boss, SoundEvents.ENTITY_GENERIC_EXPLODE, 0.7f);
                }
            }
            if (!husks.isEmpty() && husks.stream().noneMatch(MobEntity::isAlive)) {
                husks.clear();
                if (target != null) {
                    BossFx.unstun(target);
                }
            }
            if (target == null) {
                return;
            }
            if (boss.phase() >= 0 && parasiteIn < 0 && boss.getRandom().nextInt(45) == 0) {
                parasiteIn = 18;
                host = target;
                Msg.red(target, "The Nightmare has impregnated you with a deadly parasite!");
            }
            if (parasiteIn > 0 && --parasiteIn == 0) {
                if (host != null && host.isAlive()) {
                    CombatHandler.applyDamage(boss, host, 10 + boss.getRandom().nextInt(16));
                    BossFx.spawnMinion(boss, "nightmare_parasite", host.getPos(), host);
                    Msg.red(host, "The parasite bursts out of you!");
                }
                parasiteIn = -1;
            }
            if (--curseTimer <= 0) {
                curseTimer = 45 + boss.getRandom().nextInt(20);
                BossFx.disablePrayers(target, "The Nightmare curses you, shuffling your prayers!");
            }
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayerEntity killer) {
            totems.forEach(MobEntity::discard);
            husks.forEach(MobEntity::discard);
        }
    }

    /** Phantom Muspah: ranged form, then a melee charge (faster), then a frozen shield that soaks damage. */
    static final class Muspah extends ScriptedBoss {
        private int shield;

        @Override
        public void onPhase(DataBossEntity boss, int phase) {
            if (phase == 0) {
                var speed = boss.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
                if (speed != null) {
                    speed.setBaseValue(0.3);
                }
            } else if (phase == 1) {
                shield = 350;
                var speed = boss.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
                if (speed != null) {
                    speed.setBaseValue(0.18);
                }
            }
        }

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            if (shield > 0) {
                shield -= damage;
                BossFx.burst(boss, ParticleTypes.SNOWFLAKE, 8, 1.0);
                if (shield <= 0) {
                    BossFx.say(boss, "The Phantom Muspah's shield shatters!");
                    return -shield;
                }
                Msg.action(attacker, "The frozen shield absorbs your attack. (" + shield + " left)");
                return 0;
            }
            return damage;
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            if (shield > 0) {
                BossFx.ring(BossFx.world(boss), boss.getPos(), 2.2, ParticleTypes.SNOWFLAKE);
            }
        }
    }

    /**
     * Duke Sucellus: fist slams and magic; every few attacks gas vents erupt; periodically his freezing
     * gaze hits everyone he can see - hide behind one of the pillars.
     */
    static final class Duke extends ScriptedBoss {
        private int gaze = 16;
        private boolean pillars;

        @Override
        protected boolean driven() {
            return true;
        }

        @Override
        protected boolean canAttack(DataBossEntity boss) {
            return gaze > 5;
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayerEntity target, double distance) {
            if (attackCount % 5 == 4) {
                BossFx.warn(boss, "Poisonous gas erupts from the vents!");
                for (int i = 0; i < 3; i++) {
                    strike(boss, around(boss, target.getPos(), 4), 2, 20, 1.3, ParticleTypes.SNEEZE, (hit, pos) ->
                            com.runecraft.boss.Mechanics.addHazard(BossFx.world(boss), pos, 5, 8, boss));
                }
                attackCount++;
                cooldown = 3;
                return null;
            }
            return distance < 5 ? Dt2Bosses.DUKE_SLAM : Dt2Bosses.DUKE_MAGIC;
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            if (!pillars) {
                pillars = true;
                BlockPos c = home;
                for (int[] o : new int[][]{{-5, -3}, {5, -3}, {-5, 5}, {5, 5}}) {
                    for (int y = 1; y <= 3; y++) {
                        BossFx.world(boss).setBlockState(c.add(o[0], y, o[1]), Blocks.CHISELED_DEEPSLATE.getDefaultState());
                    }
                }
                BossFx.say(boss, "Duke Sucellus stirs from his slumber...");
            }
            if (target == null) {
                return;
            }
            gaze--;
            if (gaze == 5) {
                BossFx.say(boss, "Duke Sucellus' eyes begin to glow... break his line of sight!");
            }
            if (gaze > 0 && gaze <= 5) {
                BossFx.burst(boss, ParticleTypes.SNOWFLAKE, 10, 1.5);
            }
            if (gaze <= 0) {
                gaze = 20;
                for (ServerPlayerEntity p : BossFx.players(boss, 24)) {
                    if (boss.canSee(p)) {
                        BossFx.stun(p, 6);
                        CombatHandler.applyDamage(boss, p, 40 + boss.getRandom().nextInt(36));
                        Msg.red(p, "The Duke's freezing gaze locks onto you!");
                    } else {
                        Msg.game(p, "You hide from the Duke's gaze.");
                    }
                }
            }
        }
    }

    /**
     * The Leviathan: volleys of 3-6 projectiles, each announced, alternating between boulders (ranged),
     * orbs (magic) and shadows (melee) - switch prayers for every one. Debris and lightning fall between
     * volleys; below 20% it enrages and fires faster.
     */
    static final class Leviathan extends ScriptedBoss {
        private final List<AttackDef> volley = new ArrayList<>();
        private int pause;

        @Override
        protected boolean driven() {
            return true;
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayerEntity target, double distance) {
            if (volley.isEmpty()) {
                int n = 3 + boss.getRandom().nextInt(4);
                AttackDef[] styles = {Dt2Bosses.LEVI_RANGED, Dt2Bosses.LEVI_MAGIC, Dt2Bosses.LEVI_MELEE};
                for (int i = 0; i < n; i++) {
                    volley.add(styles[boss.getRandom().nextInt(3)]);
                }
                BossFx.warn(boss, "The Leviathan prepares a barrage of " + n + " projectiles!");
                for (int i = 0; i < 3; i++) {
                    strike(boss, around(boss, target.getPos(), 5), 3, Dt2Bosses.LEVI_DEBRIS.maxHit(), 1.3, ParticleTypes.ASH);
                }
                cooldown = 2;
                return null;
            }
            AttackDef a = volley.remove(0);
            Msg.action(target, a == Dt2Bosses.LEVI_RANGED ? "Boulder! (ranged)" : a == Dt2Bosses.LEVI_MAGIC ? "Abyssal orb! (magic)"
                    : "Shadow! (melee)");
            if (volley.isEmpty()) {
                pause = percent(boss) < 20 ? 3 : 6;
            }
            return a;
        }

        @Override
        protected int attackDelay(DataBossEntity boss, AttackDef attack) {
            if (pause > 0) {
                int p = pause;
                pause = 0;
                return p;
            }
            return percent(boss) < 20 ? 1 : 2;
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            if (target != null && boss.getRandom().nextInt(30) == 0) {
                strike(boss, target.getPos(), 3, Dt2Bosses.LEVI_LIGHTNING.maxHit(), 1.3, ParticleTypes.ELECTRIC_SPARK);
                BossFx.warn(boss, "The Leviathan's shell crackles with lightning!");
            }
        }
    }

    /**
     * The Whisperer: soul bolts and tentacle lashes, a piercing screech every tenth attack (run!) and,
     * below 50%, the Shadow Realm: she becomes untouchable until you destroy her tentacles in time.
     */
    static final class Whisperer extends ScriptedBoss {
        private final List<MobEntity> tentacles = new ArrayList<>();
        private int shadowTicks = -1;

        @Override
        protected boolean driven() {
            return true;
        }

        @Override
        protected boolean canAttack(DataBossEntity boss) {
            return shadowTicks < 0;
        }

        @Override
        public boolean isVulnerable(DataBossEntity boss) {
            return shadowTicks < 0;
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayerEntity target, double distance) {
            if (attackCount > 0 && attackCount % 10 == 0) {
                return Dt2Bosses.WHISPER_SCREECH;
            }
            if (boss.getRandom().nextInt(8) == 0) {
                return Dt2Bosses.WHISPER_SIPHON;
            }
            return attackCount % 2 == 0 ? Dt2Bosses.WHISPER_MAGIC : Dt2Bosses.WHISPER_RANGED;
        }

        @Override
        public void onPhase(DataBossEntity boss, int phase) {
            ServerPlayerEntity target = target(boss);
            if (target == null) {
                return;
            }
            shadowTicks = 30;
            for (int i = 0; i < 3; i++) {
                MobEntity t = BossFx.spawnMinion(boss, "whisperer_tentacle", around(boss, target.getPos(), 5), target);
                if (t != null) {
                    tentacles.add(t);
                }
            }
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 30 * 12, 0, false, false));
            BossFx.say(boss, "You are dragged into the Shadow Realm! Destroy the tentacles before your soul is consumed!");
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            if (shadowTicks < 0) {
                return;
            }
            shadowTicks--;
            boolean cleared = tentacles.stream().noneMatch(MobEntity::isAlive);
            if (cleared || shadowTicks == 0) {
                if (!cleared && target != null) {
                    CombatHandler.applyDamage(boss, target, 30 + boss.getRandom().nextInt(21));
                    Msg.red(target, "The Whisperer consumes part of your soul!");
                }
                tentacles.forEach(MobEntity::discard);
                tentacles.clear();
                shadowTicks = -1;
                if (target != null) {
                    target.removeStatusEffect(StatusEffects.DARKNESS);
                }
                BossFx.say(boss, "You escape the Shadow Realm.");
            }
        }
    }

    /**
     * Vardorvis: relentless melee that grows stronger as he weakens; his parasitic head emerges to fire a
     * telegraphed ranged or magic attack; axes swing out of the walls; blood splats heal him unless you
     * stand on them.
     */
    static final class Vardorvis extends ScriptedBoss {
        @Override
        protected boolean driven() {
            return true;
        }

        @Override
        protected boolean wantsMelee(DataBossEntity boss, ServerPlayerEntity target) {
            return true;
        }

        @Override
        protected double multiplier(DataBossEntity boss) {
            return 1.0 + (1.0 - boss.getHealth() / boss.getMaxHealth()) * 0.5;
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayerEntity target, double distance) {
            if (attackCount % 6 == 5) {
                return boss.getRandom().nextBoolean() ? Dt2Bosses.VARD_HEAD_RANGED : Dt2Bosses.VARD_HEAD_MAGIC;
            }
            if (attackCount % 8 == 7) {
                BossFx.warn(boss, "Axes swing out from the walls!");
                Vec3d p = target.getPos();
                Vec3d dir = p.subtract(boss.getPos()).normalize();
                for (int i = -4; i <= 4; i++) {
                    Vec3d at = p.add(dir.multiply(i * 1.5));
                    strike(boss, BossFx.ground(BossFx.world(boss), at.x, at.z, p.y), 3, Dt2Bosses.VARD_AXES.maxHit(), 1.1,
                            ParticleTypes.SWEEP_ATTACK);
                }
                attackCount++;
                cooldown = 2;
                return null;
            }
            if (attackCount % 11 == 10) {
                BossFx.warn(boss, "Blood splatters across the ground - absorb it before it heals him!");
                for (int i = 0; i < 4; i++) {
                    strike(boss, around(boss, target.getPos(), 4), 5, 0, 1.0, ParticleTypes.DAMAGE_INDICATOR, (hit, pos) -> {
                        if (hit.isEmpty()) {
                            boss.heal(8);
                        } else {
                            for (ServerPlayerEntity h : hit) {
                                CombatHandler.applyDamage(boss, h, 2 + h.getRandom().nextInt(5));
                            }
                        }
                    });
                }
                attackCount++;
                cooldown = 2;
                return null;
            }
            return Dt2Bosses.VARD_SLASH;
        }
    }

    /** The Hueycoatl: every attack is announced by its glowing head colour; thunderstorms and tail sweeps. */
    static final class Hueycoatl extends ScriptedBoss {
        @Override
        protected boolean driven() {
            return true;
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayerEntity target, double distance) {
            if (attackCount % 6 == 5) {
                BossFx.warn(boss, "Thunder rumbles overhead!");
                for (int i = 0; i < 5; i++) {
                    strike(boss, i == 0 ? target.getPos() : around(boss, target.getPos(), 5), 3, VarlamoreBosses.HUEY_LIGHTNING.maxHit(),
                            1.4, ParticleTypes.ELECTRIC_SPARK);
                }
                attackCount++;
                cooldown = 3;
                return null;
            }
            if (distance < 6 && boss.getRandom().nextInt(5) == 0) {
                return VarlamoreBosses.HUEY_TAIL;
            }
            AttackDef[] styles = {VarlamoreBosses.HUEY_MELEE, VarlamoreBosses.HUEY_RANGED, VarlamoreBosses.HUEY_MAGIC};
            return styles[boss.getRandom().nextInt(3)];
        }
    }

    /**
     * Royal Titans (Branda): Eldric fights alongside her. Standing together they heal each other, and
     * Branda refuses to fall while Eldric lives. Elementals are summoned to their sides.
     */
    static final class RoyalTitans extends ScriptedBoss {
        private DataBossEntity eldric;
        private int elementalTimer = 25;
        private final List<MobEntity> elementals = new ArrayList<>();

        @Override
        public void onSpawn(DataBossEntity boss) {
            ServerPlayerEntity target = boss.getTarget() instanceof ServerPlayerEntity p ? p : null;
            eldric = BossFx.spawnBoss(boss, "eldric_the_ice_king", boss.getBlockPos().add(5, 0, 0), target);
            BossFx.say(boss, "Branda the Fire Queen and Eldric the Ice King rise! Keep them apart.");
        }

        private boolean eldricAlive() {
            return eldric != null && eldric.isAlive();
        }

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            if (eldricAlive() && damage >= boss.getHealth()) {
                Msg.action(attacker, "Branda refuses to fall while her brother still stands!");
                return Math.max(0, (int) Math.ceil(boss.getHealth()) - 1);
            }
            return damage;
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            if (eldricAlive() && eldric.squaredDistanceTo(boss) < 16) {
                boss.heal(3);
                eldric.heal(3);
                BossFx.burst(boss, ParticleTypes.HEART, 2, 0.5);
                if (now % 10 == 0) {
                    BossFx.warn(boss, "The Royal Titans draw strength from each other - separate them!");
                }
            }
            if (target != null && --elementalTimer <= 0) {
                elementalTimer = 30;
                elementals.removeIf(m -> !m.isAlive());
                if (elementals.size() < 2) {
                    MobEntity f = BossFx.spawnMinion(boss, "titan_fire_elemental", around(boss, boss.getPos(), 3), target);
                    if (f != null) {
                        elementals.add(f);
                    }
                    if (eldricAlive()) {
                        MobEntity i = BossFx.spawnMinion(boss, "titan_ice_elemental", around(boss, eldric.getPos(), 3), target);
                        if (i != null) {
                            elementals.add(i);
                        }
                    }
                }
            }
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayerEntity killer) {
            elementals.forEach(MobEntity::discard);
        }
    }
}
