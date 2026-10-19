package com.runecraft.content.bosses;

import com.runecraft.boss.BossScripts;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.collection.CollectionLog;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.Equipment;
import com.runecraft.combat.MonsterStats;
import com.runecraft.combat.PoisonService;
import com.runecraft.content.systems.CombatAchievementService;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.bosses.BossVariant;
import com.runecraft.osrs.content.bosses.DoomDelves;
import com.runecraft.osrs.content.bosses.ModernBosses;
import com.runecraft.osrs.content.slayer.TormentedDemons;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModItems;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.platform.ServerLivingEntityEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Doom of Mokhaiotl's delves. Each defeat sends the Doom one level deeper and holds that level's loot for the
 * player: using the summoning stone again follows it down (the next level is harder), sneaking and using the stone
 * claims the loot and ends the run. Dying on a delve run forfeits the loot. Runs, demonic charge stacks and held loot
 * are kept in the world save ({@link DoomDelveState}). The deepest level reached is kept as a personal best
 * ({@link DoomDelves#DEEPEST_KEY} in the kill counts), as is the number of deep delves (9+) completed.
 */
final class DoomDelveService {
    private DoomDelveService() {
    }

    /** Acid blood left on each player's last level (it carries over to the next level, except delve 6). */
    private static final Map<UUID, List<Vec3>> ACID = new HashMap<>();

    static void init() {
        BossScripts.register(DoomDelves.BOSS, DoomScript::new);
        DataBossService.requirement(DoomDelves.BOSS, DoomDelveService::beforeSummon);
        DataBossService.variant(DoomDelves.BOSS, DoomDelveService::nextLevel);
        DataBossService.onKilled(DoomDelves.BOSS, DoomDelveService::completed);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer p) {
                ACID.remove(p.getUUID());
                DoomDelveState.Run run = state(p).remove(p.getUUID());
                if (run != null && !run.pending.isEmpty()) {
                    Msg.red(p, "Your delve run is over; the Doom's loot is lost.");
                }
            }
        });
        // Demonic larvae: non-demonbane hits deal at most 1, a demonbane hit kills a small larva outright.
        CombatHooks.addModifier(new CombatHooks.PlayerAttackModifier() {
            @Override
            public int afterRoll(ServerPlayer player, LivingEntity target, AttackType type, int damage) {
                if (!isLarva(target)) {
                    return damage;
                }
                return DoomDelves.larvaHit(damage, demonbane(player));
            }
        });
    }

    private static boolean isLarva(LivingEntity e) {
        MonsterStats stats = MonsterStats.of(e);
        if (stats.profile() == null) {
            return false;
        }
        String id = stats.profile().id;
        return ModernBosses.DOOM_LARVA.equals(id) || ModernBosses.DOOM_GIANT_LARVA.equals(id);
    }

    static boolean demonbane(ServerPlayer p) {
        return TormentedDemons.pierces(BossFx.heldId(p)); // approx: demonbane spells and the eye of ayak are not counted
    }

    private static DoomDelveState state(ServerPlayer p) {
        return DoomDelveState.get(p.getServer());
    }

    /** Sneak-using the stone claims the loot held from the run instead of descending. */
    private static boolean beforeSummon(ServerPlayer player, BossDef boss) {
        DoomDelveState.Run run = state(player).get(player.getUUID());
        if (player.isShiftKeyDown() && run != null) {
            claim(player, run);
            return false;
        }
        return true;
    }

    private static BossVariant nextLevel(ServerPlayer player, BossDef boss) {
        DoomDelveState.Run run = state(player).getOrCreate(player.getUUID());
        int level = run.completed + 1;
        if (level > 1) {
            Msg.game(player, "You follow the Doom of Mokhaiotl down the burrow hole to delve level " + level + ".");
        } else {
            ACID.remove(player.getUUID());
        }
        return DoomDelves.variant(boss, level);
    }

    private static void completed(ServerPlayer player, DataBossEntity boss) {
        int level = Math.max(1, boss.delve());
        DoomDelveState st = state(player);
        DoomDelveState.Run run = st.getOrCreate(player.getUUID());
        run.completed = level;
        List<DoomDelves.Loot> loot = DoomDelves.roll(level, new java.util.Random(player.getRandom().nextLong()));
        run.pending.addAll(loot);
        st.setDirty();
        PlayerData data = Players.data(player);
        int best = data.killCount(DoomDelves.DEEPEST_KEY);
        if (level > best) {
            data.killCounts().put(DoomDelves.DEEPEST_KEY, level);
            Msg.game(player, "New personal best: delve level " + level + ".");
        }
        if (level >= DoomDelves.DEEPEST) {
            data.incrementKillCount(DoomDelves.DEEP_DELVES_KEY);
        }
        boolean unique = loot.stream().anyMatch(l -> DoomDelves.UNIQUES.contains(l.item()));
        Msg.game(player, "Delve level " + level + " complete. Loot held: " + summary(run.pending) + ".");
        if (unique) {
            Msg.red(player, "A light shines from the burrow hole... delving deeper would risk your unique reward!");
        }
        Msg.game(player, "Use the summoning stone to delve deeper (level " + (level + 1) + "), or sneak and use it to claim your loot.");
    }

    private static void claim(ServerPlayer player, DoomDelveState.Run run) {
        state(player).remove(player.getUUID());
        ACID.remove(player.getUUID());
        if (run.pending.isEmpty()) {
            Msg.game(player, "You have no loot waiting from the Doom.");
            return;
        }
        for (DoomDelves.Loot l : run.pending) {
            Inv.give(player, l.item(), l.count());
            CollectionLog.obtain(player, l.item());
        }
        Msg.game(player, "You claim your rewards from " + run.completed + " delve level" + (run.completed == 1 ? "" : "s") + ".");
    }

    /** Stacks of demonic charge on this player's run (they carry over between levels). */
    static int addStacks(ServerPlayer player, int n) {
        DoomDelveState st = state(player);
        DoomDelveState.Run run = st.getOrCreate(player.getUUID());
        int before = run.stacks;
        run.stacks += n;
        st.setDirty();
        return before;
    }

    private static String summary(List<DoomDelves.Loot> loot) {
        Map<String, Integer> merged = new java.util.LinkedHashMap<>();
        for (DoomDelves.Loot l : loot) {
            merged.merge(l.item(), l.count(), Integer::sum);
        }
        List<String> parts = new ArrayList<>();
        merged.forEach((item, n) -> parts.add(n + " x " + item.replace('_', ' ')));
        return String.join(", ", parts);
    }

    private static boolean wearsShield(ServerPlayer p) {
        if (Equipment.twoHanded(p)) {
            return false;
        }
        ItemDef def = ModItems.def(p.getOffhandItem());
        return def != null && def.equipment() != null && def.equipment().slot() == EquipmentDef.Slot.SHIELD;
    }

    /**
     * The fight (OSRS Wiki "Doom of Mokhaiotl" and its strategy guides; timings the wiki doesn't give are approx):
     * <ul>
     *   <li>Standard attacks: magic and ranged orbs, claws when you stand next to it.</li>
     *   <li>Special Beam Cannon: every few attacks the Doom charges a beam (60, 80 from delve 3, 99 from delve 6). Only a
     *       melee hit interrupts it (the melee punish: a fifth of your melee strength bonus as extra damage and a 6-8 tick
     *       delay to its next attack). It is exposed meanwhile.</li>
     *   <li>Demonic larvae (grubs) crawl to the Doom; one that reaches it heals the Doom and hurts you, more for every
     *       stack of demonic charge (stacks carry over between levels). Killed larvae explode, hurting the Doom or its
     *       shield.</li>
     *   <li>Shield phase (delve 3+): at 75% hitpoints it raises a 500-point shield only demonbane breaks while larvae
     *       drop constantly; a full charge bar fires the beam.</li>
     *   <li>Burrow ("car") phase (delve 5+): after the shield it burrows, trampling across the arena while rocks fall,
     *       then slams: shelter by a fallen rock.</li>
     *   <li>Shockwave: about a minute in, destroy two volatile earth to raise an earthen shield (it moves from the second
     *       towards the first, clearing acid blood) and stand in it when the shockwave lands.</li>
     *   <li>Acid blood (delve 3+): sprayed when it is damaged; venomous, and it stays for the next level (delve 6 wipes it).</li>
     * </ul>
     */
    static final class DoomScript extends ScriptedBoss {
        private record Larva(Mob mob, boolean giant) {
        }

        private int level;
        private boolean started;
        private long lastSplat;
        private final List<Vec3> acid = new ArrayList<>();
        private final List<Larva> larvae = new ArrayList<>();
        private long nextLarva;
        // Beam / melee punish
        private int attacksSinceBeam;
        private int beamTicks;
        private long exposedUntil;
        // Shield phase
        private boolean shield;
        private int shieldPoints;
        private int shieldTicks;
        private int shieldsDone;
        private int attacksSinceShield;
        // Burrow phase
        private int burrowTicks;
        private final List<Vec3> rocks = new ArrayList<>();
        // Shockwave
        private long nextShockwave;
        private long shockwaveDue = -1;
        private final List<Mob> earth = new ArrayList<>();
        private final List<Vec3> earthBroken = new ArrayList<>();
        private Vec3 earthenShield;
        private Vec3 earthenTarget;
        private int cleared;

        @Override
        protected boolean driven() {
            return true;
        }

        @Override
        protected int attackDelay(DataBossEntity boss, AttackDef attack) {
            return DoomDelves.attackSpeed(Math.max(1, boss.delve()));
        }

        @Override
        protected boolean canAttack(DataBossEntity boss) {
            return beamTicks <= 0 && !shield && burrowTicks <= 0 && shockwaveDue < 0;
        }

        @Override
        public boolean isVulnerable(DataBossEntity boss) {
            return burrowTicks <= 0;
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            if (level >= DoomDelves.SHIELD_FROM && attacksSinceShield >= (shieldsDone == 0 ? DoomDelves.SHIELD_MIN_ATTACKS : 12) // approx
                    && boss.getHealth() <= boss.getMaxHealth() * DoomDelves.SHIELD_BELOW) {
                startShield(boss);
                return null;
            }
            if (attacksSinceBeam >= DoomDelves.ATTACKS_PER_BEAM) {
                startBeam(boss);
                return null;
            }
            attacksSinceBeam++;
            attacksSinceShield++;
            if (distance <= 2.5 && boss.getRandom().nextBoolean()) {
                return ModernBosses.DOOM_MELEE;
            }
            return boss.getRandom().nextBoolean() ? ModernBosses.DOOM_MAGIC : ModernBosses.DOOM_RANGED;
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
            if (!boss.isAlive()) {
                return;
            }
            level = Math.max(1, boss.delve());
            if (!started && target != null) {
                started = true;
                List<Vec3> carried = ACID.remove(target.getUUID());
                if (carried != null && level != DoomDelves.ACID_WIPE_LEVEL) {
                    acid.addAll(carried);
                } else if (carried != null && !carried.isEmpty()) {
                    Msg.game(target, "The acid blood is washed away as you reach delve level " + level + ".");
                }
                nextShockwave = now + DoomDelves.SHOCKWAVE_INTERVAL;
                nextLarva = now + DoomDelves.larvaInterval(level);
            }
            if (!started) {
                return;
            }
            List<ServerPlayer> near = BossFx.players(boss, 24);
            for (ServerPlayer p : near) {
                if (!wearsShield(p)) {
                    CombatAchievementService.flag(p, DoomDelves.FLAG_NO_SHIELD);
                }
            }
            tickAcid(boss, near);
            tickLarvae(boss, target, near);
            if (beamTicks > 0) {
                beamTicks--;
                BossFx.burst(boss, ParticleTypes.ELECTRIC_SPARK, 8, 1.0);
                if (beamTicks == 0) {
                    fireBeam(boss, target);
                }
            }
            if (shield) {
                tickShield(boss, target);
            }
            if (burrowTicks > 0) {
                tickBurrow(boss, target, near);
            }
            tickShockwave(boss, near);
            if (!shield && burrowTicks <= 0 && now >= nextLarva && target != null) {
                nextLarva = now + DoomDelves.larvaInterval(level);
                boolean giant = DoomDelves.giantLarvae(level);
                for (int i = 0; i < DoomDelves.larvaeOutsideShield(level); i++) {
                    spawnLarva(boss, target, giant);
                }
            }
        }

        // ---------------- Special Beam Cannon and the melee punish ----------------

        private void startBeam(DataBossEntity boss) {
            attacksSinceBeam = 0;
            beamTicks = DoomDelves.BEAM_CHARGE_TICKS;
            BossFx.say(boss, "The Doom of Mokhaiotl begins charging a beam... punish it with melee!");
            BossFx.sound(boss, SoundEvents.BEACON_POWER_SELECT, 0.6f);
        }

        private void fireBeam(DataBossEntity boss, ServerPlayer target) {
            if (target == null || !target.isAlive()) {
                return;
            }
            BossFx.say(boss, "The Doom of Mokhaiotl fires its beam!");
            BossFx.world(boss).sendParticles(ParticleTypes.SONIC_BOOM, target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
            CombatHooks.nameAttack("charge", AttackType.CRUSH);
            CombatHandler.applyDamage(boss, target, boss.getRandom().nextInt(DoomDelves.chargeMaxHit(level) + 1));
        }

        // ---------------- Shield phase ----------------

        private void startShield(DataBossEntity boss) {
            shield = true;
            shieldPoints = DoomDelves.SHIELD_POINTS;
            shieldTicks = 0;
            beamTicks = 0;
            BossFx.say(boss, "The Doom of Mokhaiotl raises a demonic shield and begins to charge! Only demonbane can break it.");
            BossFx.burst(boss, ParticleTypes.ENCHANT, 30, 1.5);
        }

        private void tickShield(DataBossEntity boss, ServerPlayer target) {
            shieldTicks++;
            BossFx.ring(BossFx.world(boss), boss.position(), boss.getBbWidth() / 2 + 0.5, ParticleTypes.SOUL_FIRE_FLAME);
            if (target != null && shieldTicks % DoomDelves.shieldLarvaInterval(level) == 0) {
                spawnLarva(boss, target, false);
            }
            if (shieldTicks >= DoomDelves.SHIELD_CHARGE_TICKS) {
                fireBeam(boss, target);
                endShield(boss, false);
            }
        }

        private void damageShield(DataBossEntity boss, int amount) {
            shieldPoints -= amount;
            if (shieldPoints <= 0) {
                BossFx.say(boss, "The Doom's shield shatters!");
                endShield(boss, true);
            } else {
                BossFx.warn(boss, "The Doom's shield: " + shieldPoints + "/" + DoomDelves.SHIELD_POINTS);
            }
        }

        private void endShield(DataBossEntity boss, boolean broken) {
            shield = false;
            shieldsDone++;
            attacksSinceShield = 0;
            cooldown = Math.max(cooldown, 3);
            if (level >= DoomDelves.BURROW_FROM) {
                startBurrow(boss);
            }
        }

        // ---------------- Burrow ("car") phase ----------------

        private void startBurrow(DataBossEntity boss) {
            burrowTicks = DoomDelves.BURROW_TICKS;
            rocks.clear();
            for (int i = 0; i < 4; i++) {
                rocks.add(around(boss, homeVec(), 7));
            }
            BossFx.say(boss, "The Doom of Mokhaiotl burrows underground! Rocks fall from the ceiling - shelter by one before it slams!");
            BossFx.burst(boss, ParticleTypes.CAMPFIRE_COSY_SMOKE, 20, 1.5);
        }

        private void tickBurrow(DataBossEntity boss, ServerPlayer target, List<ServerPlayer> near) {
            burrowTicks--;
            ServerLevel world = BossFx.world(boss);
            for (Vec3 r : rocks) {
                BossFx.column(world, r, ParticleTypes.CAMPFIRE_COSY_SMOKE, 3);
            }
            if (target != null && burrowTicks > 3 && burrowTicks % DoomDelves.TRAMPLE_INTERVAL == 0) {
                Vec3 d = target.position().subtract(boss.position());
                double len = Math.sqrt(d.x * d.x + d.z * d.z);
                if (len > 0.1) {
                    double step = Math.min(len, 4);
                    boss.teleportTo(boss.getX() + d.x / len * step, boss.getY(), boss.getZ() + d.z / len * step);
                }
                BossFx.burst(boss, ParticleTypes.EXPLOSION, 2, 0.8);
                for (ServerPlayer p : near) {
                    if (p.distanceToSqr(boss) <= 2.25 * 2.25) {
                        CombatHooks.nameAttack("trample", AttackType.CRUSH);
                        CombatHandler.applyDamage(boss, p, boss.getRandom().nextInt(DoomDelves.standardMaxHit(level) + 1)); // approx
                    }
                }
            }
            if (burrowTicks == 3) {
                BossFx.warn(boss, "The Doom of Mokhaiotl rises for a slam - get behind a rock!");
            }
            if (burrowTicks == 0) {
                BossFx.burst(boss, ParticleTypes.EXPLOSION_EMITTER, 1, 0.5);
                BossFx.sound(boss, SoundEvents.GENERIC_EXPLODE, 0.7f);
                for (ServerPlayer p : near) {
                    boolean sheltered = rocks.stream().anyMatch(r -> p.position().distanceToSqr(r) <= 1.5 * 1.5);
                    if (!sheltered) {
                        CombatHooks.nameAttack("slam", AttackType.CRUSH);
                        CombatHandler.applyDamage(boss, p, boss.getRandom().nextInt(DoomDelves.chargeMaxHit(level) + 1)); // approx
                    }
                    CombatAchievementService.flag(p, DoomDelves.FLAG_BURROWED);
                }
                rocks.clear();
                cooldown = Math.max(cooldown, 2);
            }
        }

        // ---------------- Demonic larvae ----------------

        private void spawnLarva(DataBossEntity boss, ServerPlayer target, boolean giant) {
            Vec3 d = target.position().subtract(boss.position());
            double len = Math.max(0.1, Math.sqrt(d.x * d.x + d.z * d.z));
            double side = (boss.getRandom().nextDouble() - 0.5) * 6;
            double x = boss.getX() + d.x / len * 7 - d.z / len * side;
            double z = boss.getZ() + d.z / len * 7 + d.x / len * side;
            Vec3 at = BossFx.ground(BossFx.world(boss), x, z, boss.getY());
            Mob mob = BossFx.spawnMinion(boss, giant ? ModernBosses.DOOM_GIANT_LARVA : ModernBosses.DOOM_LARVA, at, null);
            if (mob != null) {
                larvae.add(new Larva(mob, giant));
            }
        }

        private void tickLarvae(DataBossEntity boss, ServerPlayer target, List<ServerPlayer> near) {
            Iterator<Larva> it = larvae.iterator();
            while (it.hasNext()) {
                Larva l = it.next();
                Mob mob = l.mob();
                if (!mob.isAlive()) {
                    if (mob.isDeadOrDying()) {
                        explode(boss, mob, near);
                    }
                    it.remove();
                    continue;
                }
                Vec3 d = boss.position().subtract(mob.position());
                double len = Math.sqrt(d.x * d.x + d.z * d.z);
                if (len <= boss.getBbWidth() / 2 + 0.6) {
                    absorb(boss, l, target, near);
                    mob.discard();
                    it.remove();
                    continue;
                }
                double step = Math.min(len, 0.6); // approx: about a tile a tick
                mob.teleportTo(mob.getX() + d.x / len * step, mob.getY(), mob.getZ() + d.z / len * step);
            }
        }

        private void absorb(DataBossEntity boss, Larva l, ServerPlayer target, List<ServerPlayer> near) {
            ServerPlayer victim = target != null ? target : near.isEmpty() ? null : near.get(0);
            int stacks = victim != null ? addStacks(victim, DoomDelves.larvaStacks(l.giant())) : 0;
            boss.heal(DoomDelves.larvaHeal(stacks, l.giant()));
            BossFx.burst(boss, ParticleTypes.HEART, 4, 0.8);
            if (victim != null) {
                CombatHooks.nameAttack("larva", null);
                CombatHandler.applyDamage(boss, victim, DoomDelves.larvaDamage(stacks, l.giant(), level));
                Msg.red(victim, "A larva reaches the Doom, which grows stronger (demonic charge " + (stacks + DoomDelves.larvaStacks(l.giant())) + ").");
            }
            for (ServerPlayer p : near) {
                CombatAchievementService.flag(p, DoomDelves.FLAG_GRUB_ABSORBED);
            }
        }

        private void explode(DataBossEntity boss, Mob mob, List<ServerPlayer> near) {
            ServerLevel world = BossFx.world(boss);
            world.sendParticles(ParticleTypes.EXPLOSION, mob.getX(), mob.getY() + 0.3, mob.getZ(), 1, 0, 0, 0, 0);
            for (ServerPlayer p : near) {
                if (p.position().distanceToSqr(mob.position()) <= 1.5 * 1.5) {
                    CombatHooks.nameAttack("larva_explosion", null);
                    CombatHandler.applyDamage(boss, p, 1 + boss.getRandom().nextInt(5)); // approx
                }
            }
            double reach = boss.getBbWidth() / 2 + 1.5;
            if (mob.distanceToSqr(boss) <= reach * reach) {
                if (shield) {
                    damageShield(boss, DoomDelves.LARVA_SHIELD_DAMAGE);
                } else {
                    boss.setHealth(Math.max(1, boss.getHealth() - (5 + boss.getRandom().nextInt(6))));
                }
            }
        }

        // ---------------- Shockwave, volatile earth and the earthen shield ----------------

        private void tickShockwave(DataBossEntity boss, List<ServerPlayer> near) {
            if (shockwaveDue < 0) {
                if (now >= nextShockwave && !shield && burrowTicks <= 0 && beamTicks <= 0 && !near.isEmpty()) {
                    shockwaveDue = now + DoomDelves.SHOCKWAVE_WINDUP;
                    earthBroken.clear();
                    earthenShield = null;
                    cleared = 0;
                    for (int i = 0; i < 3; i++) {
                        Mob e = BossFx.spawnMinion(boss, ModernBosses.DOOM_VOLATILE_EARTH, around(boss, homeVec(), 8), null);
                        if (e != null) {
                            earth.add(e);
                        }
                    }
                    BossFx.say(boss, "The Doom of Mokhaiotl gathers a shockwave! Destroy two volatile earth to raise an earthen shield.");
                }
                return;
            }
            Iterator<Mob> it = earth.iterator();
            while (it.hasNext()) {
                Mob e = it.next();
                if (!e.isAlive()) {
                    if (e.isDeadOrDying() && earthBroken.size() < 2) {
                        earthBroken.add(e.position());
                    }
                    it.remove();
                }
            }
            if (earthenShield == null && earthBroken.size() >= 2) {
                earthenShield = earthBroken.get(1);
                earthenTarget = earthBroken.get(0);
                for (Mob e : earth) {
                    e.discard();
                }
                earth.clear();
                BossFx.warn(boss, "An earthen shield rises from the rubble!");
            }
            if (earthenShield != null) {
                Vec3 d = earthenTarget.subtract(earthenShield);
                double len = Math.sqrt(d.x * d.x + d.z * d.z);
                if (len > 0.05) {
                    double step = Math.min(len, 0.5);
                    earthenShield = earthenShield.add(d.x / len * step, 0, d.z / len * step);
                }
                Iterator<Vec3> a = acid.iterator();
                while (a.hasNext()) {
                    Vec3 splat = a.next();
                    double dx = splat.x - earthenShield.x;
                    double dz = splat.z - earthenShield.z;
                    if (dx * dx + dz * dz <= 1.0) {
                        a.remove();
                        cleared++;
                    }
                }
                BossFx.ring(BossFx.world(boss), earthenShield, 2.0, ParticleTypes.WAX_ON);
            }
            if (now >= shockwaveDue) {
                BossFx.sound(boss, SoundEvents.GENERIC_EXPLODE, 0.5f);
                BossFx.ring(BossFx.world(boss), boss.position(), 10, ParticleTypes.EXPLOSION);
                for (ServerPlayer p : near) {
                    boolean safe = earthenShield != null && p.position().distanceToSqr(earthenShield) <= 2.5 * 2.5;
                    if (!safe) {
                        CombatHooks.nameAttack("shockwave", AttackType.MAGIC);
                        CombatHandler.applyDamage(boss, p, boss.getRandom().nextInt(DoomDelves.standardMaxHit(level) + 1)); // approx
                    }
                    if (cleared >= DoomDelves.MOPPING_UP_SPLATS) {
                        CombatAchievementService.flag(p, DoomDelves.FLAG_MOPPING_UP);
                    }
                }
                for (Mob e : earth) {
                    e.discard();
                }
                earth.clear();
                earthenShield = null;
                shockwaveDue = -1;
                nextShockwave = now + DoomDelves.SHOCKWAVE_INTERVAL;
            }
        }

        // ---------------- Acid blood ----------------

        private void tickAcid(DataBossEntity boss, List<ServerPlayer> near) {
            if (acid.isEmpty()) {
                return;
            }
            ServerLevel world = BossFx.world(boss);
            for (Vec3 a : acid) {
                world.sendParticles(ParticleTypes.SNEEZE, a.x, a.y + 0.1, a.z, 2, 0.4, 0.02, 0.4, 0.0);
            }
            for (ServerPlayer p : near) {
                for (Vec3 a : acid) {
                    if (p.position().distanceToSqr(a) < 0.8 * 0.8) {
                        CombatHooks.nameAttack("hazard", null);
                        CombatHandler.applyDamage(boss, p, 1 + boss.getRandom().nextInt(7));
                        PoisonService.venom(p, now);
                        break;
                    }
                }
            }
        }

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (burrowTicks > 0) {
                return 0;
            }
            if (shield) {
                if (demonbane(attacker) && damage > 0) {
                    damageShield(boss, damage * DoomDelves.SHIELD_DEMONBANE_FACTOR);
                } else {
                    Msg.action(attacker, "Only demonbane can harm the Doom's shield.");
                }
                return 0;
            }
            int d = damage;
            if (beamTicks > 0 && type != null && type.isMelee()) {
                int str = Equipment.total(attacker, Players.data(attacker).skills()).meleeStrength();
                d += DoomDelves.punishBonus(str);
                beamTicks = 0;
                cooldown = Math.max(cooldown, DoomDelves.punishDelay(level));
                exposedUntil = now + 2;
                Msg.game(attacker, "You punish the Doom of Mokhaiotl, interrupting its beam!");
                BossFx.burst(boss, ParticleTypes.CRIT, 15, 1.0);
            }
            boolean exposed = beamTicks > 0 || now <= exposedUntil;
            if (exposed && d >= boss.getHealth()) {
                CombatAchievementService.flag(attacker, DoomDelves.FLAG_EXPOSED);
            }
            if (level >= DoomDelves.ACID_FROM && d > 0 && now - lastSplat >= 3) { // approx: at most one spray every 3 ticks
                lastSplat = now;
                int splats = level >= 8 ? 4 : 2 + boss.getRandom().nextInt(2);
                for (int i = 0; i < splats && acid.size() < 80; i++) {
                    double angle = boss.getRandom().nextDouble() * Math.PI * 2;
                    double r = 2 + boss.getRandom().nextDouble() * 3;
                    acid.add(BossFx.ground(BossFx.world(boss), boss.getX() + Math.cos(angle) * r, boss.getZ() + Math.sin(angle) * r, boss.getY()));
                }
                BossFx.burst(boss, ParticleTypes.SNEEZE, 12, 1.5);
            }
            return d;
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayer killer) {
            for (Larva l : larvae) {
                l.mob().discard();
            }
            larvae.clear();
            for (Mob e : earth) {
                e.discard();
            }
            earth.clear();
            if (killer != null && !acid.isEmpty()) {
                ACID.put(killer.getUUID(), new ArrayList<>(acid));
            }
        }
    }
}
