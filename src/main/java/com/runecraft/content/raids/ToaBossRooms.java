package com.runecraft.content.raids;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.Mechanics;
import com.runecraft.combat.PoisonService;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.raids.Invocation;
import com.runecraft.osrs.content.raids.RaidScaling;
import com.runecraft.osrs.content.raids.RaidsContent;
import com.runecraft.osrs.content.raids.RoomDef;
import com.runecraft.osrs.content.raids.ToaBossRules;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The Tombs of Amascut's path bosses (OSRS wiki boss pages and "Tombs of Amascut/Strategies"). Each fight's room opens
 * no corridor: the boss leaves a teleport crystal back to the Nexus. The invocations' effects come from
 * {@link Invocation}, the other numbers from {@link ToaBossRules}.
 */
final class ToaBossRooms {
    private ToaBossRooms() {
    }

    /** Base of the path boss rooms. */
    abstract static class PathBoss extends RoomLogic {
        PathBoss(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public boolean exitsEast() {
            return false;
        }

        long now() {
            return RaidEffects.now();
        }

        int hurt(LivingEntity source, ServerPlayer p, int damage) {
            return RaidEffects.hit(source, p, damage, null, true);
        }

        /** Kills the room's boss through the real hit path (self-test), handling the fight's blockers with {@code unblock}. */
        String grind(RaidSelfTest t, DataBossEntity boss, AttackType style, int damage, Runnable unblock) {
            int guard = 0;
            int attacksBefore = raid.attacksOnParty();
            while (!done && guard++ < 6000) {
                unblock.run();
                if (done) {
                    break;
                }
                if (alive(boss) && bossVulnerable(boss) && !selfTestHolds()) {
                    t.hit(boss, damage, styleFor(boss, style));
                }
                selfTestAutoAttack(t, boss, guard);
                t.tick(1);
            }
            RaidSelfTest.expect(done, def.name() + ": the boss did not die (" + (alive(boss) ? raid.hitpoints(boss) + " hitpoints left" : "gone")
                    + ")");
            RaidSelfTest.expect(raid.attacksOnParty() > attacksBefore, def.name() + ": the boss never attacked");
            return def.name() + " killed in " + guard + " ticks";
        }

        /**
         * Self-test: entities don't tick while a check runs, so a boss that attacks through its own AI (Akkha, Ba-Ba) gets
         * its auto-attack driven here at its attack speed, through the real path (its style choice, the attack, the room's
         * outgoing hook and specials).
         */
        void selfTestAutoAttack(RaidSelfTest t, DataBossEntity boss, int tick) {
            if (!alive(boss) || boss.isNoAi() || boss.stats() == null) {
                return;
            }
            int speed = Math.max(1, boss.stats().attackSpeed());
            if (tick % speed == 0) {
                ServerPlayer p = t.player();
                AttackType s = boss.chooseStyle(p, boss.distanceToSqr(p));
                boss.performAttack(p, s);
            }
        }

        /** Self-test: whether a player would be busy with something else than the boss right now (Ba-Ba's boulders). */
        boolean selfTestHolds() {
            return false;
        }

        /** The style the self-test hits a boss with (rooms override to dodge protection prayers). */
        AttackType styleFor(DataBossEntity boss, AttackType wanted) {
            return wanted;
        }
    }

    // ================================================================== Kephri

    /**
     * Kephri (OSRS wiki): three shield bars, then her 80-hitpoint body. Each time a shield breaks she is dazed and calls
     * her children: overlords (a Spitting Scarab, then a Soldier and an Arcane Scarab; More Overlords adds one) and 28,
     * then 24, scarab swarms that recharge her shield when they reach her (in trios at path level 2+). While shielded she
     * throws fireballs and bomber scarabs, Dung Strikes and lays eggs: small eggs burst, dark eggs hatch Agile Scarabs.
     */
    static final class Kephri extends PathBoss {
        /** Arcane Scarab relocation spots around Kephri (OSRS: south-east, north-west, mid-south, north-east). */
        private static final int[][] ARCANE_SPOTS = {{6, 8}, {-4, -10}, {0, 12}, {6, -8}};
        static final String[] CHILDREN = {"toa_scarab_swarm", "toa_soldier_scarab", "toa_spitting_scarab", "toa_arcane_scarab",
                "toa_agile_scarab", com.runecraft.osrs.content.raids.ToaBosses.KEPHRI_EGG,
                com.runecraft.osrs.content.raids.ToaBosses.KEPHRI_DARK_EGG};
        /** Kephri's fireball max hit (OSRS infobox) and the share Protect from Magic lets through (wiki: ~66% blocked). */
        static final int KEPHRI_MAX_HIT = 24;
        static final double KEPHRI_PROTECTED_SHARE = 1 / 3.0;
        /** The Spitting Scarab's max hit (OSRS infobox: 10). */
        static final int SPITTING_MAX_HIT = 10;
        DataBossEntity kephri;
        final List<LivingEntity> guards = new ArrayList<>();
        int broken;
        boolean dazed;
        private int leaked;
        private int overcharge;
        private LivingEntity arcane;
        private int arcaneSpot;
        private int arcaneCharge;
        private int arcaneHits;

        Kephri(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            BlockPos c = center();
            RaidBuilder.fill(world(), c.getX() + 12, c.getY(), c.getZ() - 4, c.getX() + 17, c.getY(), c.getZ() + 4,
                    Blocks.CHISELED_SANDSTONE.defaultBlockState());
        }

        @Override
        public void start() {
            broken = 0;
            dazed = false;
            leaked = 0;
            overcharge = 0;
            guards.clear();
            eggs.clear();
            arcane = null;
            kephri = spawnBoss("kephri", at(14, 0));
            if (kephri != null) {
                // Every attack of hers is the room's: fireballs, Dung Strike, eggs.
                kephri.setNoAi(true);
                anchor(kephri);
            }
        }

        private boolean guarded() {
            for (LivingEntity g : guards) {
                if (alive(g)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean bossVulnerable(DataBossEntity boss) {
            return !dazed && !guarded();
        }

        /** A shield bar never takes Kephri below 1 hitpoint: breaking it starts an intermission instead. */
        @Override
        public int bossIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (boss != kephri || broken >= 3 || damage <= 0) {
                return damage;
            }
            int absorbed = Math.min(damage, overcharge);
            overcharge -= absorbed;
            damage -= absorbed;
            int hp = raid.hitpoints(kephri);
            if (damage >= hp) {
                later(1, this::shieldBroken);
                return Math.max(0, hp - 1);
            }
            return damage;
        }

        /** Poison, venom, recoil or Vengeance (which skip bossIncoming) can't kill her through a shield either. */
        @Override
        public boolean bossAllowDeath(DataBossEntity boss) {
            if (boss != kephri || broken >= 3) {
                return true;
            }
            raid.setCurrentHitpoints(kephri, 1);
            if (!dazed) {
                later(1, this::shieldBroken);
            }
            return false;
        }

        private void shieldBroken() {
            if (!alive(kephri) || dazed || broken >= 3) {
                return;
            }
            broken++;
            Set<Invocation> inv = raid.invocations();
            if (broken >= 3) {
                // OSRS: the third shield exposes her 80-hitpoint body; she Dung Strikes every player once.
                int body = RaidScaling.toaScaledHp(80, raid.raidLevel(), raid.pathLevel(), raid.partySize(), true);
                raid.setCurrentHitpoints(kephri, body);
                warn("Kephri's swarm disperses - she is vulnerable!");
                for (ServerPlayer p : players()) {
                    dungStrike(p);
                }
                return;
            }
            dazed = true;
            warn("Kephri's shield shatters! Her children rush to her aid!");
            guards.clear();
            for (String id : Invocation.kephriOverlords(inv, broken)) {
                int[] spot = id.equals("toa_arcane_scarab") ? ARCANE_SPOTS[0] : id.equals("toa_spitting_scarab") ? new int[]{6, -8} : new int[]{4, 0};
                LivingEntity g = spawnMob(id, at(spot[0], spot[1] + (guards.size() > 2 ? 3 : 0)), false);
                if (g != null) {
                    guards.add(g);
                    if (id.equals("toa_arcane_scarab")) {
                        arcane = g;
                        arcaneSpot = 0;
                        arcaneCharge = 0;
                        arcaneHits = 0;
                    }
                }
            }
            // OSRS: 28 (then 24) swarms from six points around the room, clockwise, faster and faster; from path level 2
            // some come as trios.
            int swarms = Invocation.kephriSwarms(broken);
            int delay = 0;
            int start = rnd().nextInt(6);
            boolean trios = raid.pathLevel() >= 2;
            for (int i = 0; i < swarms; i++) {
                delay += Invocation.kephriSwarmDelay(i);
                int point = (start + i) % 6;
                int n = trios && i % 4 == 3 ? 3 : 1; // approx: every fourth spawn a trio
                later(delay, () -> {
                    if (alive(kephri) && dazed) {
                        double a = Math.PI / 3 * point + Math.PI / 6;
                        for (int k = 0; k < n; k++) {
                            spawnMob("toa_scarab_swarm", at((int) Math.round(Math.cos(a) * 16) + k, (int) Math.round(Math.sin(a) * 16)), false);
                        }
                    }
                });
            }
            later(delay + 4, this::endIntermission);
        }

        /** The swarms are spent: once her overlords are dead the shield is back at full strength. */
        private void endIntermission() {
            if (!alive(kephri) || !dazed) {
                return;
            }
            if (guarded()) {
                later(5, this::endIntermission);
                return;
            }
            dazed = false;
            int max = raid.maxHitpoints(kephri);
            raid.setCurrentHitpoints(kephri, max);
            // OSRS: swarms that reached her overcharge the shield, up to about 15% over its capacity (~10% each).
            overcharge = Math.min(max * 15 / 100, leaked * max / 10);
            leaked = 0;
            warn("Kephri's shield regenerates!" + (overcharge > 0 ? " Her swarms overcharge it!" : ""));
        }

        @Override
        public void onCreatureHit(ServerPlayer player, LivingEntity target, int damage) {
            if (target == arcane && alive(arcane)) {
                arcaneHits++;
                // OSRS: three hits (+1 per extra player), or one over 39, make the Arcane Scarab relocate.
                if (damage > 39 || arcaneHits >= Invocation.arcaneScarabRelocateHits(raid.partySize())) {
                    relocateArcane();
                }
            }
        }

        private void relocateArcane() {
            arcaneSpot = (arcaneSpot + 1) % ARCANE_SPOTS.length;
            arcaneHits = 0;
            arcaneCharge = 0;
            BlockPos to = at(ARCANE_SPOTS[arcaneSpot][0], ARCANE_SPOTS[arcaneSpot][1]);
            arcane.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
            say("The Arcane Scarab scuttles away, dispersing its swarm.");
        }

        private void dungStrike(ServerPlayer p) {
            Msg.red(p, "Flies buzz around you...");
            later(3, () -> {
                if (!alive(kephri) || !p.isAlive()) {
                    return;
                }
                Msg.red(p, "Kephri throws you back!");
                Vec3 push = p.position().subtract(kephri.position()).multiply(1, 0, 1).normalize().scale(1.4);
                p.push(push.x, 0.4, push.z);
                p.hurtMarked = true;
                hurt(kephri, p, 5 + rnd().nextInt(11)); // approx: "light damage"
                world().sendParticles(ParticleTypes.ITEM_SLIME, p.getX(), p.getY() + 0.5, p.getZ(), 12, 0.6, 0.3, 0.6, 0.05);
            });
        }

        /**
         * Kephri's fireball (and a bomber scarab's): lands where the target stood, max hit 24 (OSRS); 3x3 and harder with
         * Aerial Assault. Protect from Magic takes about two thirds off (OSRS wiki).
         */
        private void fireball(ServerPlayer p) {
            Set<Invocation> inv = raid.invocations();
            int radius = Invocation.kephriFireballRadius(inv);
            int max = (int) Math.round(KEPHRI_MAX_HIT * Invocation.kephriFireballDamage(inv));
            RaidEffects.strike(world(), p.position(), 0.8 + 1.5 * radius, max, 3, kephri, ParticleTypes.FLAME, AttackType.MAGIC,
                    KEPHRI_PROTECTED_SHARE);
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            if (!alive(kephri)) {
                return;
            }
            Set<Invocation> inv = raid.invocations();
            keepAggressive("toa_soldier_scarab", "toa_spitting_scarab", "toa_agile_scarab");
            // OSRS: faster auto-attacks every two path levels.
            int every = 6 - Math.min(4, raid.pathLevel()) / 2;
            if (tick % every == 0 && !dazed) {
                ServerPlayer p = randomPlayer();
                if (p != null) {
                    fireball(p);
                }
            }
            if (broken < 3 && !dazed) {
                shieldSpecials(tick, inv);
            }
            if (dazed) {
                intermission(tick);
            }
            // OSRS: swarms only come outside her intermissions with the Medic! invocation.
            if (tick % 20 == 0 && !dazed && inv.contains(Invocation.MEDIC)) {
                for (int i = 0; i < 2; i++) {
                    LivingEntity swarm = spawnMob("toa_scarab_swarm", at(-6 + rnd().nextInt(12), i % 2 == 0 ? -18 : 18), false);
                    if (swarm != null && Invocation.kephriMedicSwarmSpeed(inv) < 1.0) {
                        // OSRS: Medic!'s swarms move 50% slower (Slowness III: -45%).
                        swarm.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 600, 2, false, false));
                    }
                }
            }
            if (tick % 2 == 0 && (dazed || guarded())) {
                world().sendParticles(ParticleTypes.ENCHANT, kephri.getX(), kephri.getY() + 1, kephri.getZ(), 10, 1.2, 0.8, 1.2, 0.2);
            }
            // OSRS: a swarm that reaches her recharges about 10% of a shield (heals her body in the last phase).
            int reached = healers("toa_scarab_swarm", kephri, raid.maxHitpoints(kephri) * 0.01,
                    "A scarab swarm reaches Kephri and recharges her shield!");
            if (dazed) {
                leaked += reached;
            }
            agileScarabs();
            tickEggs();
        }

        /** Intermission: bomber scarabs, the Soldier's healing swarms, the Spitting Scarab's spray, the Arcane charge. */
        private void intermission(long tick) {
            List<ServerPlayer> players = players();
            if (players.isEmpty()) {
                return;
            }
            if (tick % 8 == 0) {
                ServerPlayer p = players.get(rnd().nextInt(players.size()));
                Msg.red(p, "A bomber scarab homes in on you!");
                fireball(p);
            }
            for (LivingEntity g : guards) {
                if (!alive(g)) {
                    continue;
                }
                String id = RaidInstance.creatureId(g);
                if ("toa_soldier_scarab".equals(id) && tick % 8 == 0) {
                    // OSRS: about every 5 seconds the Soldier sends a swarm that recharges Kephri, more at higher health.
                    LivingEntity swarm = spawnMob("toa_scarab_swarm", g.blockPosition(), false);
                    if (swarm != null) {
                        swarm.setHealth(Math.max(1, swarm.getMaxHealth() * (float) raid.healthFraction(g)));
                    }
                } else if ("toa_spitting_scarab".equals(id) && tick % 3 == 0) {
                    // OSRS: the Spitting Scarab hits every player within 8 tiles (poisoning from 6).
                    LivingEntity target = g instanceof Mob m ? m.getTarget() : null;
                    for (ServerPlayer p : players) {
                        if (p != target && p.distanceToSqr(g) < 8 * 8) {
                            // OSRS: "fairly accurate and can hit through prayers". // approx: half through Protect from Missiles
                            if (RaidEffects.hit(g, p, rnd().nextInt(SPITTING_MAX_HIT + 1), AttackType.RANGED, 0.5) > 0
                                    && rnd().nextInt(4) == 0) {
                                PoisonService.poison(p, 6, com.runecraft.action.ActionManager.gameTick());
                            }
                        }
                    }
                }
            }
            if (alive(arcane)) {
                arcaneCharge++;
                world().sendParticles(ParticleTypes.WITCH, arcane.getX(), arcane.getY() + 0.5, arcane.getZ(), 4 + arcaneCharge / 2, 1.0, 0.4, 1.0, 0.02);
                if (arcaneCharge >= Invocation.ARCANE_SCARAB_CHARGE_TICKS) {
                    // OSRS: well over 65 damage unless Protect from Magic is up (which cuts about two thirds).
                    warn("The Arcane Scarab unleashes its explosive swarm!");
                    for (ServerPlayer p : players) {
                        int roll = 45 + rnd().nextInt(26);
                        hurt(arcane, p, RaidEffects.protectedFrom(p, AttackType.MAGIC) ? roll / 3 : roll);
                    }
                    relocateArcane();
                }
            }
        }

        /**
         * Kephri's shield phase specials (OSRS): Dung Strike knocks a player back (two players with Blowing Mud) and
         * Mass Incubation lays small eggs that burst in a 3x3 area and dark eggs that hatch Agile Scarabs (3-5 with Lively
         * Larvae); with too many scarabs about she Dung Strikes instead.
         */
        private void shieldSpecials(long tick, Set<Invocation> inv) {
            List<ServerPlayer> players = players();
            if (players.isEmpty()) {
                return;
            }
            boolean crowded = living("toa_agile_scarab").size() >= 4; // approx: "too many scarabs"
            if (tick % 16 == 4 || (tick % 16 == 12 && crowded)) {
                List<ServerPlayer> pool = new ArrayList<>(players);
                java.util.Collections.shuffle(pool, rnd());
                int n = Invocation.kephriDungTargets(inv, players.size());
                for (ServerPlayer p : pool.subList(0, Math.min(n, pool.size()))) {
                    dungStrike(p);
                }
            } else if (tick % 16 == 12) {
                say("Kephri lays her eggs!");
                // OSRS: two small light eggs and two large dark ones (3-5 with Lively Larvae) splatter around the arena.
                for (int i = 0; i < 2; i++) {
                    layEgg(com.runecraft.osrs.content.raids.ToaBosses.KEPHRI_EGG, players);
                }
                int dark = Invocation.kephriDarkEggs(inv, rnd());
                for (int i = 0; i < dark; i++) {
                    layEgg(com.runecraft.osrs.content.raids.ToaBosses.KEPHRI_DARK_EGG, players);
                }
            }
        }

        /** Game ticks before an unattended egg bursts or hatches. // approx */
        static final int EGG_TICKS = 8;
        /** Damage of a small egg's 3x3 burst. // approx: up to 12 */
        static final int EGG_BURST_MAX = 12;
        /** Eggs on the floor and the tick they burst or hatch. */
        private final Map<LivingEntity, Long> eggs = new HashMap<>();

        private void layEgg(String id, List<ServerPlayer> players) {
            ServerPlayer p = players.get(rnd().nextInt(players.size()));
            BlockPos at = inRoom(BlockPos.containing(p.position().add(rnd().nextInt(9) - 4, 0, rnd().nextInt(9) - 4)));
            LivingEntity egg = spawnMob(id, at, false);
            if (egg != null) {
                if (egg instanceof Mob m) {
                    m.setNoAi(true);
                }
                eggs.put(egg, now() + EGG_TICKS);
            }
        }

        /**
         * Eggs left alone (OSRS): the small ones burst in a 3x3 area, the dark ones hatch an Agile Scarab. Players always
         * hit an egg for their max hit, so breaking them in time is quick.
         */
        private void tickEggs() {
            java.util.Iterator<Map.Entry<LivingEntity, Long>> it = eggs.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<LivingEntity, Long> e = it.next();
                LivingEntity egg = e.getKey();
                if (!alive(egg)) {
                    it.remove();
                    continue;
                }
                if (now() < e.getValue()) {
                    continue;
                }
                it.remove();
                Vec3 at = egg.position();
                if (com.runecraft.osrs.content.raids.ToaBosses.KEPHRI_DARK_EGG.equals(RaidInstance.creatureId(egg))) {
                    spawnMob("toa_agile_scarab", egg.blockPosition(), false);
                } else {
                    world().sendParticles(ParticleTypes.ITEM_SLIME, at.x, at.y + 0.3, at.z, 20, 1.0, 0.2, 1.0, 0.05);
                    for (ServerPlayer q : players()) {
                        if (Math.abs(q.getX() - at.x) <= 1.5 && Math.abs(q.getZ() - at.z) <= 1.5) {
                            hurt(kephri, q, rnd().nextInt(EGG_BURST_MAX + 1));
                        }
                    }
                }
                egg.discard();
            }
        }

        private BlockPos inRoom(BlockPos pos) {
            BlockPos c = center();
            int h = RaidBuilder.HALF - 2;
            return new BlockPos(Math.max(c.getX() - h, Math.min(c.getX() + h, pos.getX())), pos.getY(),
                    Math.max(c.getZ() - h, Math.min(c.getZ() + h, pos.getZ())));
        }

        /** Agile Scarabs retreat from players at running speed (OSRS). */
        private void agileScarabs() {
            for (LivingEntity s : living("toa_agile_scarab")) {
                ServerPlayer near = null;
                for (ServerPlayer p : players()) {
                    if (p.distanceToSqr(s) < 9 && (near == null || p.distanceToSqr(s) < near.distanceToSqr(s))) {
                        near = p;
                    }
                }
                if (near != null && s instanceof Mob m) {
                    Vec3 away = s.position().subtract(near.position()).multiply(1, 0, 1).normalize().scale(4);
                    m.getNavigation().moveTo(s.getX() + away.x, s.getY(), s.getZ() + away.z, 1.4);
                }
            }
        }

        @Override
        public void cleanup() {
            super.cleanup();
            for (String k : CHILDREN) {
                living(k).forEach(LivingEntity::discard);
            }
        }

        @Override
        String selfTestSolve(RaidSelfTest t) {
            int[] eggsBroken = {0};
            String r = grind(t, kephri, AttackType.STAB, 40, () -> {
                for (LivingEntity g : guards) {
                    if (alive(g)) {
                        t.hit(g, 40, AttackType.STAB);
                    }
                }
                for (LivingEntity s : living("toa_scarab_swarm")) {
                    t.hit(s, 10, AttackType.RANGED);
                }
                for (LivingEntity e : new ArrayList<>(eggs.keySet())) {
                    if (alive(e)) {
                        t.hit(e, 40, AttackType.STAB);
                        eggsBroken[0] += alive(e) ? 0 : 1;
                    }
                }
            });
            RaidSelfTest.expect(broken >= 3, "Kephri died before all three shields broke (" + broken + ")");
            return r + ", " + eggsBroken[0] + " eggs broken";
        }
    }

    // ================================================================== Akkha

    /**
     * Akkha (OSRS wiki): melee, ranged and magic in a fixed cycle, praying against the style he attacks with (players'
     * protection only blocks part of his hits); his ranged and magic hit everyone in the arena. Every seventh attack (sixth
     * with Stay Vigilant) is a special - Memory Blast, Trailing Orbs, Detonate in groups - and he then changes style. Each
     * quadrant adds its element to his specials. At 80/60/40/20% four shadows charge the quadrants: kill one and lure
     * Akkha into its quadrant to hurt him. Out of health he restores 20%, merges into a corner shadow praying ranged and
     * magic, launches unstable orbs and changes corner after three successful hits (+1 per extra player).
     */
    static final class Akkha extends PathBoss {
        private static final String[] STYLES = {"melee", "ranged", "magic"};
        DataBossEntity akkha;
        int style;
        private int autos;
        private int styleAttacks;
        private long lastAttack;
        final LivingEntity[] shadows = new LivingEntity[4];
        private final int[] shadowCharge = new int[4];
        private int thresholds;
        boolean hidden;
        private int trailTicks;
        private final Map<UUID, Vec3> trailLast = new HashMap<>();
        /** Trailing orbs on the floor: x, z, expiry tick. */
        private final List<double[]> trail = new ArrayList<>();
        boolean finalStand;
        int corner;
        private int cornerHits;
        /** Unstable orbs: x, z (relative to the centre), dx, dz. */
        private final List<double[]> unstable = new ArrayList<>();

        Akkha(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            BlockPos c = center();
            // The four quadrant symbols: fire, shadow, lightning, ice.
            net.minecraft.world.level.block.state.BlockState[] marks = {Blocks.MAGMA_BLOCK.defaultBlockState(), Blocks.CRYING_OBSIDIAN.defaultBlockState(),
                    Blocks.YELLOW_GLAZED_TERRACOTTA.defaultBlockState(), Blocks.PACKED_ICE.defaultBlockState()};
            for (int q = 0; q < 4; q++) {
                Vec3 v = quadrantCentre(q);
                BlockPos m = BlockPos.containing(v.x, c.getY(), v.z);
                RaidBuilder.fill(world(), m.getX() - 1, c.getY(), m.getZ() - 1, m.getX() + 1, c.getY(), m.getZ() + 1, marks[q]);
            }
            RaidBuilder.fill(world(), c.getX() - 20, c.getY(), c.getZ(), c.getX() + 20, c.getY(), c.getZ(), Blocks.CUT_SANDSTONE.defaultBlockState());
            RaidBuilder.fill(world(), c.getX(), c.getY(), c.getZ() - 20, c.getX(), c.getY(), c.getZ() + 20, Blocks.CUT_SANDSTONE.defaultBlockState());
        }

        @Override
        public void start() {
            for (int q = 0; q < 4; q++) {
                shadows[q] = null;
                shadowCharge[q] = 0;
            }
            thresholds = 0;
            hidden = false;
            autos = 0;
            style = 0;
            styleAttacks = 0;
            lastAttack = now();
            trailTicks = 0;
            trailLast.clear();
            trail.clear();
            unstable.clear();
            finalStand = false;
            akkha = spawnBoss("akkha", at(8, 0));
        }

        int quadrant(Vec3 pos) {
            boolean west = pos.x < center().getX() + 0.5;
            boolean north = pos.z < center().getZ() + 0.5;
            return north ? (west ? 0 : 1) : (west ? 3 : 2);
        }

        Vec3 quadrantCentre(int q) {
            return vec(q == 0 || q == 3 ? -10 : 10, q <= 1 ? -10 : 10);
        }

        Vec3 cornerSpot(int q) {
            return vec(q == 0 || q == 3 ? -16 : 16, q <= 1 ? -16 : 16);
        }

        private boolean shadowsUp() {
            return thresholds > 0;
        }

        @Override
        public boolean bossVulnerable(DataBossEntity boss) {
            if (boss != akkha) {
                return true;
            }
            if (hidden) {
                return false;
            }
            if (finalStand) {
                return true;
            }
            // OSRS: while the shadows are up he channels their immunity, unless he stands in a dispelled shadow's quadrant.
            return !shadowsUp() || !alive(shadows[quadrant(akkha.position())]);
        }

        /** Akkha prays against the style he attacks with; in his final stand against ranged and magic. */
        @Override
        public int bossIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (boss != akkha || damage <= 0) {
                return damage;
            }
            boolean melee = type.isMelee();
            boolean blocked = finalStand ? !melee : (style == 0 ? melee : style == 1 ? type == AttackType.RANGED : type == AttackType.MAGIC);
            if (blocked) {
                Msg.action(attacker, "Akkha's prayer blocks your attack.");
                return 0;
            }
            return damage;
        }

        @Override
        public void onCreatureHit(ServerPlayer player, LivingEntity target, int damage) {
            if (target == akkha && finalStand && damage > 0 && ++cornerHits >= ToaBossRules.akkhaSwapHits(raid.partySize())) {
                swapCorner();
            }
        }

        /** Out of health: the final stand (OSRS) instead of death, once. */
        @Override
        public boolean bossAllowDeath(DataBossEntity boss) {
            if (boss != akkha || finalStand) {
                return true;
            }
            beginFinalStand();
            return false;
        }

        private void beginFinalStand() {
            finalStand = true;
            hidden = false;
            raid.setCurrentHitpoints(akkha, Math.max(1, raid.maxHitpoints(akkha) * ToaBossRules.AKKHA_FINAL_STAND_PERCENT / 100));
            for (LivingEntity s : shadows) {
                if (alive(s)) {
                    s.discard();
                }
            }
            warn("Akkha makes his final stand! He merges into a shadow - only melee can reach him now.");
            corner = rnd().nextInt(4);
            akkha.setNoAi(true);
            move(akkha, cornerSpot(corner));
            anchor(akkha);
            cornerHits = 0;
        }

        private void swapCorner() {
            cornerHits = 0;
            corner = Math.floorMod(corner + 1 + rnd().nextInt(3), 4);
            unanchor(akkha);
            move(akkha, cornerSpot(corner));
            anchor(akkha);
            say("Akkha slips into another shadow.");
        }

        @Override
        public AttackDef bossAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            List<AttackDef> list = attacks(boss);
            return list.size() >= 3 ? list.get(style) : null;
        }

        /** Each of Akkha's auto-attacks: partial protection, ranged/magic on everyone, Keep Back, specials. */
        @Override
        public int bossOutgoing(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
            if (boss != akkha || hidden || finalStand) {
                return 0;
            }
            Set<Invocation> inv = raid.invocations();
            int max = attacks(akkha).get(style).maxHit();
            if (damage == 0 && RaidEffects.protectedFrom(player, type)) {
                damage = (int) Math.round(rnd().nextInt(max + 1)
                        * (type.isMelee() ? ToaBossRules.AKKHA_PROTECTED_MELEE : ToaBossRules.AKKHA_PROTECTED_RANGED_MAGIC));
            }
            if (style != 0) {
                if (Invocation.akkhaKeepBack(inv)) {
                    // OSRS Keep Back: a melee hit lands on anyone next to him just before his ranged/magic attack.
                    for (ServerPlayer p : players()) {
                        if (p.distanceToSqr(akkha) < 2.6 * 2.6) {
                            hitStyled(p, rnd().nextInt(attacks(akkha).get(0).maxHit() + 1), AttackType.SLASH);
                        }
                    }
                }
                // OSRS: his ranged and magic attacks hit everyone in the arena.
                for (ServerPlayer p : players()) {
                    if (p != player) {
                        hitStyled(p, rnd().nextInt(max + 1), type);
                    }
                }
            }
            lastAttack = now();
            autos++;
            styleAttacks++;
            boolean special = autos % Invocation.akkhaSpecialEvery(inv) == 0;
            if (special) {
                later(1, () -> specials(inv));
            }
            int oneIn = Invocation.akkhaSwitchOneIn(inv, special);
            boolean early = inv.contains(Invocation.STAY_VIGILANT) && styleAttacks < Invocation.AKKHA_MIN_STYLE_ATTACKS;
            if (oneIn > 0 && !early && rnd().nextInt(oneIn) == 0) {
                switchStyle();
            }
            return damage;
        }

        /** One of Akkha's styled hits on a player, protection letting part of it through. */
        private void hitStyled(ServerPlayer p, int roll, AttackType type) {
            if (RaidEffects.protectedFrom(p, type)) {
                roll = (int) Math.round(roll * (type.isMelee() ? ToaBossRules.AKKHA_PROTECTED_MELEE : ToaBossRules.AKKHA_PROTECTED_RANGED_MAGIC));
            }
            hurt(akkha, p, roll);
        }

        private void switchStyle() {
            style = Invocation.akkhaNextStyle(style);
            styleAttacks = 0;
            warn("Akkha switches to " + STYLES[style] + " - and prays against it!");
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            if (!alive(akkha)) {
                return;
            }
            keepBossesAggressive();
            Set<Invocation> inv = raid.invocations();
            if (!finalStand) {
                double hp = raid.healthFraction(akkha) * 100;
                while (thresholds < ToaBossRules.SHADOW_THRESHOLDS.length && hp <= ToaBossRules.SHADOW_THRESHOLDS[thresholds]) {
                    thresholds++;
                    spawnShadows(thresholds == 1);
                }
                if (shadowsUp()) {
                    shadowCharges(tick);
                }
                if (now() - lastAttack > ToaBossRules.AKKHA_STUCK_TICKS) {
                    // OSRS: he changes style if he can't attack for a minute.
                    lastAttack = now();
                    switchStyle();
                }
            } else {
                finalStandOrbs(tick);
            }
            trailingOrbs(inv);
        }

        /** Four shadows in the corners (OSRS), all back at full health at each later threshold. */
        private void spawnShadows(boolean first) {
            warn(first ? "Shadows appear throughout the room!" : "The shadows in the room are restored!");
            for (int q = 0; q < 4; q++) {
                shadowCharge[q] = 0;
                if (alive(shadows[q])) {
                    raid.setCurrentHitpoints(shadows[q], raid.maxHitpoints(shadows[q]));
                    continue;
                }
                Vec3 c = cornerSpot(q);
                LivingEntity s = spawnMob("akkhas_shadow", BlockPos.containing(c.x, c.y, c.z), false);
                if (s instanceof Mob m) {
                    // OSRS: the shadows don't attack; they charge their quadrant's element.
                    m.setNoAi(true);
                }
                shadows[q] = s;
            }
        }

        /**
         * Each living shadow charges its quadrant; a full charge envelops the quadrant in its element (heavy damage and
         * the element's effect). Killing the shadow spares the quadrant. // approx: 40 ticks to charge
         */
        private void shadowCharges(long tick) {
            if (hidden) {
                return;
            }
            for (int q = 0; q < 4; q++) {
                if (!alive(shadows[q])) {
                    continue;
                }
                shadowCharge[q]++;
                if (shadowCharge[q] % 5 == 0) {
                    world().sendParticles(ParticleTypes.SOUL, shadows[q].getX(), shadows[q].getY() + 2.6, shadows[q].getZ(), 2 + shadowCharge[q] / 8,
                            0.3, 0.2, 0.3, 0.01);
                }
                if (shadowCharge[q] >= 40) {
                    shadowCharge[q] = 0;
                    warn("The shadow in the " + ToaBossRules.QUADRANTS[q] + " quadrant slams its spear down!");
                    for (ServerPlayer p : players()) {
                        if (quadrant(p.position()) == q) {
                            hurt(akkha, p, 20 + rnd().nextInt(16)); // approx: heavy
                            element(p, q);
                        }
                    }
                }
            }
        }

        /** A quadrant's element on a player hit there by a special (OSRS). */
        private void element(ServerPlayer p, int q) {
            switch (q) {
                case 0 -> {
                    // Fire: a burn of 5 a hit that spreads to players next to them.
                    Msg.red(p, "You are set alight!");
                    burn(p, 5);
                }
                case 1 -> {
                    // Shadow: rapid low damage and combat stats drained by 3.
                    Msg.red(p, "Shadows sap your strength!");
                    PlayerData d = Players.data(p);
                    for (Skill s : new Skill[]{Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC}) {
                        d.skills().setBoost(s, d.skills().boost(s) - 3);
                    }
                    for (int i = 1; i <= 4; i++) {
                        later(i, () -> hurt(akkha, p, 1 + rnd().nextInt(3)));
                    }
                    com.runecraft.network.ServerNetworking.sync(p);
                }
                case 2 -> dropOverheads(p, "Lightning disables your protection prayers!");
                default -> {
                    // Ice: attacks one tick slower until it thaws. // approx: shown as a slow
                    Msg.red(p, "Ice encases your limbs, slowing you.");
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1, false, true));
                    p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 120, 1, false, true));
                    later(10, () -> Msg.game(p, "The ice around you melts away."));
                }
            }
        }

        private void burn(ServerPlayer p, int times) {
            if (times <= 0 || !alive(akkha)) {
                return;
            }
            later(4, () -> {
                if (!p.isAlive() || raid.isDowned(p)) {
                    return;
                }
                RaidEffects.hitRaw(akkha, p, 5);
                world().sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1, p.getZ(), 6, 0.3, 0.5, 0.3, 0.01);
                for (ServerPlayer o : players()) {
                    if (o != p && o.distanceToSqr(p) < 1.5 * 1.5 && rnd().nextInt(3) == 0) {
                        Msg.red(o, "The flames spread to you!");
                        burn(o, 3);
                    }
                }
                burn(p, times - 1);
            });
        }

        /** Memory Blast, Trailing Orbs and (groups) Detonate; two at once with Double Trouble. */
        private void specials(Set<Invocation> inv) {
            if (!alive(akkha) || finalStand) {
                return;
            }
            List<Integer> pool = new ArrayList<>(List.of(0, 1));
            if (raid.partySize() > 1) {
                pool.add(2);
            }
            java.util.Collections.shuffle(pool, rnd());
            for (int sp : pool.subList(0, Math.min(pool.size(), Invocation.akkhaSpecialsAtOnce(inv)))) {
                switch (sp) {
                    case 0 -> memoryBlast(inv);
                    case 1 -> {
                        warn("Magical orbs begin to materialise around you!");
                        trailTicks = 10;
                        trailLast.clear();
                    }
                    default -> detonate(inv);
                }
            }
        }

        /**
         * Memory Blast (OSRS): Akkha vanishes while the quadrants glow in order (each next to the last); then the blast
         * engulfs every quadrant but the safe one, in turn, with four ticks (two with Feeling Special?) to move between
         * them.
         */
        private void memoryBlast(Set<Invocation> inv) {
            hidden = true;
            warn("Sections of the room start to glow...");
            List<Integer> order = ToaBossRules.memoryBlastOrder(ToaBossRules.memoryBlastRounds(raid.pathLevel()), rnd());
            int gap = Invocation.akkhaMemoryBlastTicks(inv);
            for (int k = 0; k < order.size(); k++) {
                int shown = order.get(k);
                later(1 + 2 * k, () -> {
                    Vec3 c = quadrantCentre(shown);
                    world().sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y + 1, c.z, 60, 3, 0.5, 3, 0.01);
                    world().playSound(null, BlockPos.containing(c), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.HOSTILE, 1f,
                            0.7f + shown * 0.15f);
                });
            }
            int start = 3 + 2 * order.size();
            for (int k = 0; k < order.size(); k++) {
                int safe = order.get(k);
                later(start + k * gap, () -> {
                    if (!alive(akkha)) {
                        return;
                    }
                    for (ServerPlayer p : players()) {
                        int q = quadrant(p.position());
                        if (q != safe) {
                            world().sendParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 1, p.getZ(), 10, 0.4, 0.4, 0.4, 0.02);
                            hurt(akkha, p, 25 + rnd().nextInt(21)); // approx: heavy, the infobox's 55 being its top
                            element(p, q);
                        }
                    }
                });
            }
            later(start + order.size() * gap, () -> hidden = false);
        }

        /** Trailing Orbs: every step a player takes leaves an orb behind (and one ahead with Feeling Special?). */
        private void trailingOrbs(Set<Invocation> inv) {
            trail.removeIf(o -> o[2] < now());
            for (double[] o : trail) {
                world().sendParticles(ParticleTypes.WITCH, o[0], center().getY() + 1.4, o[1], 2, 0.1, 0.1, 0.1, 0);
                for (ServerPlayer p : players()) {
                    if ((p.getX() - o[0]) * (p.getX() - o[0]) + (p.getZ() - o[1]) * (p.getZ() - o[1]) < 0.6 * 0.6) {
                        o[2] = -1;
                        hurt(akkha, p, 12 + rnd().nextInt(12)); // approx: heavy
                        element(p, quadrant(p.position()));
                    }
                }
            }
            if (trailTicks <= 0) {
                return;
            }
            if (--trailTicks == 0) {
                say("The magical orbs stop materialising.");
            }
            for (ServerPlayer p : players()) {
                Vec3 now = p.position();
                Vec3 last = trailLast.put(p.getUUID(), now);
                if (last != null && RaidEffects.horizontalDistSq(last, now) > 0.25) {
                    trail.add(new double[]{last.x, last.z, now() + 12});
                    if (Invocation.akkhaTrailingOrbs(inv) > 1) {
                        Vec3 ahead = now.add(now.subtract(last).normalize().scale(1.5));
                        trail.add(new double[]{ahead.x, ahead.z, now() + 12});
                    }
                }
            }
        }

        /** Detonate (groups only): each player explodes in a + (and x with Feeling Special?) hitting others in line. */
        private void detonate(Set<Invocation> inv) {
            warn("You have been marked for detonation!");
            boolean diagonals = Invocation.akkhaDetonateDiagonals(inv, raid.partySize());
            later(4, () -> {
                List<ServerPlayer> players = players();
                for (ServerPlayer a : players) {
                    world().sendParticles(ParticleTypes.EXPLOSION, a.getX(), a.getY() + 1, a.getZ(), 1, 0, 0, 0, 0);
                    for (ServerPlayer b : players) {
                        if (a == b) {
                            continue;
                        }
                        double dx = Math.abs(a.getX() - b.getX());
                        double dz = Math.abs(a.getZ() - b.getZ());
                        boolean sameTile = dx < 0.6 && dz < 0.6;
                        boolean inLine = (dx < 0.8 && dz <= 1.5) || (dz < 0.8 && dx <= 1.5) || (diagonals && Math.abs(dx - dz) < 0.8 && dx <= 1.5);
                        if (!sameTile && inLine) {
                            hurt(akkha, b, 20 + rnd().nextInt(16)); // approx
                        }
                    }
                }
            });
        }

        /** Final stand: unstable orbs cross the arena in straight lines (OSRS: capped at 25; Protect from Magic -25%). */
        private void finalStandOrbs(long tick) {
            if (tick % 3 == 0) {
                boolean alongX = rnd().nextBoolean();
                int lane = rnd().nextInt(2 * RaidBuilder.HALF - 1) - RaidBuilder.HALF + 1;
                int side = rnd().nextBoolean() ? 1 : -1;
                unstable.add(alongX ? new double[]{-side * RaidBuilder.HALF, lane, side, 0} : new double[]{lane, -side * RaidBuilder.HALF, 0, side});
            }
            java.util.Iterator<double[]> it = unstable.iterator();
            while (it.hasNext()) {
                double[] o = it.next();
                o[0] += o[2];
                o[1] += o[3];
                if (Math.abs(o[0]) > RaidBuilder.HALF || Math.abs(o[1]) > RaidBuilder.HALF) {
                    it.remove();
                    continue;
                }
                Vec3 at = vec(o[0], o[1]);
                world().sendParticles(ParticleTypes.END_ROD, at.x, at.y + 0.8, at.z, 3, 0.15, 0.15, 0.15, 0);
                for (ServerPlayer p : players()) {
                    if (RaidEffects.horizontalDistSq(p.position(), at) < 0.8 * 0.8) {
                        // OSRS: Protect from Magic takes 25% off before the orbs' cap of 25.
                        RaidEffects.hitRaw(akkha, p, ToaBossRules.unstableOrbDamage(raid.damageMultiplier(), raid.pathLevel(),
                                RaidEffects.protectedFrom(p, AttackType.MAGIC)));
                        it.remove();
                        break;
                    }
                }
            }
        }

        @Override
        public void cleanup() {
            super.cleanup();
            living("akkhas_shadow").forEach(LivingEntity::discard);
            trail.clear();
            unstable.clear();
        }

        @Override
        AttackType styleFor(DataBossEntity boss, AttackType wanted) {
            if (finalStand) {
                return AttackType.SLASH;
            }
            return style == 2 ? AttackType.RANGED : AttackType.MAGIC;
        }

        @Override
        String selfTestSolve(RaidSelfTest t) {
            int shadowsKilled = 0;
            int[] kills = {0};
            String r = grind(t, akkha, AttackType.MAGIC, 30, () -> {
                if (finalStand || hidden || !shadowsUp() || !alive(akkha)) {
                    return;
                }
                int q = quadrant(akkha.position());
                if (alive(shadows[q])) {
                    // Kill the nearest shadow and lure Akkha into its quadrant (moved there, as the player would draw him).
                    for (int k = 0; k < 4; k++) {
                        if (alive(shadows[k])) {
                            t.hit(shadows[k], 200, AttackType.MAGIC);
                            if (!alive(shadows[k])) {
                                kills[0]++;
                                move(akkha, quadrantCentre(k));
                            }
                            break;
                        }
                    }
                }
            });
            RaidSelfTest.expect(thresholds == 4, "Akkha: the shadows did not appear at every threshold (" + thresholds + ")");
            RaidSelfTest.expect(finalStand, "Akkha: he died without his final stand");
            return r + ", " + (kills[0] + shadowsKilled) + " shadows dispelled";
        }
    }

    // ================================================================== Ba-Ba

    /**
     * Ba-Ba (OSRS wiki): melee (fully blocked by Protect from Melee), Rockfall on every player plus two debris piles, Rock
     * Throw at everyone (only standing next to debris, an unopened sarcophagus or a baboon saves you), a directional
     * Shockwave, and baboons that hack the sarcophagi open. At 66% and 33% she leaps to her throne and screams, knocking
     * everyone west (into the pit with Mind the Gap!), then rolls ten sets of five boulders, one of them cracked, down the
     * room.
     */
    static final class Baba extends PathBoss {
        DataBossEntity baba;
        private int phase;
        boolean rolling;
        private int setsLeft;
        private long nextSet;
        private int lastCracked = -1;
        private final Set<LivingEntity> cracked = new HashSet<>();
        /** Debris piles (block positions) and their hitpoints. */
        final Map<BlockPos, Integer> debris = new HashMap<>();
        /** Sarcophagi (block positions) and their hitpoints (0 = open). */
        final Map<BlockPos, Integer> sarcophagi = new HashMap<>();
        private final Set<LivingEntity> baboons = new HashSet<>();
        private final Map<Vec3, Long> peels = new HashMap<>();
        private final Set<LivingEntity> knownBoulders = new HashSet<>();
        private long nextRockfall;
        private long nextThrow;
        private long nextShockwave;

        Baba(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            sarcophagi.clear();
            for (int x = -12; x <= 12; x += 8) {
                for (int z : new int[]{-17, 17}) {
                    BlockPos s = at(x, z);
                    RaidBuilder.set(world(), s, Blocks.CHISELED_SANDSTONE.defaultBlockState());
                    RaidBuilder.set(world(), s.above(), Blocks.CARVED_PUMPKIN.defaultBlockState());
                }
            }
            for (int i = 0; i < 6; i++) {
                RaidBuilder.set(world(), at(rnd().nextInt(30) - 15, rnd().nextBoolean() ? -20 : 20), Blocks.JUNGLE_LEAVES.defaultBlockState()
                        .setValue(LeavesBlock.PERSISTENT, true));
            }
            // Her throne at the east end.
            BlockPos c = center();
            RaidBuilder.fill(world(), c.getX() + 15, c.getY(), c.getZ() - 3, c.getX() + 19, c.getY(), c.getZ() + 3, Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
        }

        @Override
        public void start() {
            phase = 0;
            rolling = false;
            baboons.clear();
            peels.clear();
            cracked.clear();
            clearDebris();
            sarcophagi.clear();
            for (int x = -12; x <= 12; x += 8) {
                for (int z : new int[]{-17, 17}) {
                    BlockPos s = at(x, z);
                    sarcophagi.put(s, ToaBossRules.SARCOPHAGUS_HP);
                    RaidBuilder.set(world(), s.above(), Blocks.CARVED_PUMPKIN.defaultBlockState());
                }
            }
            baba = spawnBoss("baba", at(8, 0));
            long n = now();
            nextRockfall = n + 10;
            nextThrow = n + 20;
            nextShockwave = n + 14;
        }

        private void clearDebris() {
            for (BlockPos d : debris.keySet()) {
                RaidBuilder.set(world(), d, Blocks.AIR.defaultBlockState());
            }
            debris.clear();
        }

        @Override
        public AttackDef bossAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            // Her own attack is the melee slam (harmless from her throne while the boulders roll); everything else is the
            // room's, so her other listed attacks are never picked.
            List<AttackDef> list = attacks(boss);
            return list.isEmpty() ? null : list.get(0);
        }

        @Override
        public int bossOutgoing(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
            return rolling ? 0 : damage;
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            if (!alive(baba)) {
                return;
            }
            keepBossesAggressive();
            keepAggressive("toa_baba_baboon");
            double hp = raid.healthFraction(baba) * 100;
            if (phase < 2 && hp <= (phase == 0 ? 66.6 : 33.3) && !rolling) {
                phase++;
                rollingStones();
            }
            Set<Invocation> inv = raid.invocations();
            if (rolling) {
                boulders(tick, inv);
            } else {
                long n = now();
                if (n >= nextRockfall) {
                    rockfall();
                    nextRockfall = n + ToaBossRules.babaRockfallEvery(raid.pathLevel());
                }
                if (n >= nextThrow) {
                    rockThrow();
                    nextThrow = n + 30; // approx
                }
                if (n >= nextShockwave) {
                    shockwave(inv);
                    nextShockwave = n + 12; // approx
                }
            }
            baboons(tick, inv);
            bananas(tick, inv);
        }

        /** Rockfall: a rock on each player's tile, and two debris piles that stay until destroyed. */
        private void rockfall() {
            for (ServerPlayer p : players()) {
                RaidEffects.strike(world(), p.position(), 0.9, 30, 3, baba, ParticleTypes.CLOUD, null, true); // approx: heavy
            }
            for (int i = 0; i < 2; i++) {
                BlockPos d = at(rnd().nextInt(24) - 14, rnd().nextInt(28) - 14);
                if (debris.containsKey(d) || sarcophagi.containsKey(d)) {
                    continue;
                }
                world().sendParticles(ParticleTypes.CLOUD, d.getX() + 0.5, d.getY() + 0.2, d.getZ() + 0.5, 30, 1.2, 0.05, 1.2, 0);
                later(3, () -> {
                    if (!alive(baba)) {
                        return;
                    }
                    for (ServerPlayer p : players()) {
                        if (p.blockPosition().equals(d)) {
                            hurt(baba, p, 20 + rnd().nextInt(10));
                            p.push(1, 0.2, 0);
                            p.hurtMarked = true;
                        }
                    }
                    RaidBuilder.set(world(), d, Blocks.COBBLESTONE.defaultBlockState());
                    debris.put(d, ToaBossRules.debrisHp(raid.partySize()));
                });
            }
        }

        /**
         * Rock Throw (OSRS): a boulder at every player, landing about 6 ticks later; it is nulled by standing next to
         * debris (destroyed), an unopened sarcophagus (30 damage to it) or a baboon (12 damage to it).
         */
        private void rockThrow() {
            warn("Ba-Ba hurls boulders at everyone! Take cover next to debris, a sarcophagus or a baboon.");
            for (ServerPlayer p : players()) {
                world().sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 2.5, p.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
            }
            later(6, () -> {
                if (!alive(baba)) {
                    return;
                }
                boolean debrisLost = false;
                for (ServerPlayer p : players()) {
                    BlockPos stand = p.blockPosition();
                    BlockPos cover = null;
                    for (BlockPos d : debris.keySet()) {
                        if (d.distManhattan(stand) <= 1 || (Math.abs(d.getX() - stand.getX()) <= 1 && Math.abs(d.getZ() - stand.getZ()) <= 1)) {
                            cover = d;
                            break;
                        }
                    }
                    if (cover != null) {
                        debris.remove(cover);
                        RaidBuilder.set(world(), cover, Blocks.AIR.defaultBlockState());
                        debrisLost = true;
                        continue;
                    }
                    BlockPos sarc = null;
                    for (Map.Entry<BlockPos, Integer> s : sarcophagi.entrySet()) {
                        BlockPos b = s.getKey();
                        if (s.getValue() > 0 && Math.abs(b.getX() - stand.getX()) <= 1 && Math.abs(b.getZ() - stand.getZ()) <= 1) {
                            sarc = b;
                            break;
                        }
                    }
                    if (sarc != null) {
                        damageSarcophagus(sarc, ToaBossRules.THROW_ON_SARCOPHAGUS);
                        continue;
                    }
                    LivingEntity shield = null;
                    for (LivingEntity b : living("toa_baba_baboon")) {
                        if (b.distanceToSqr(p) < 1.6 * 1.6) {
                            shield = b;
                            break;
                        }
                    }
                    if (shield != null) {
                        shield.hurt(world().damageSources().generic(), ToaBossRules.THROW_ON_BABOON);
                        continue;
                    }
                    Msg.red(p, "The boulder crushes you!");
                    hurt(baba, p, (raid.partySize() > 1 ? 35 : 45) + rnd().nextInt(16)); // approx: massive, less in groups
                }
                if (debrisLost) {
                    // OSRS: if debris was destroyed, Ba-Ba follows up with another Rockfall.
                    later(2, this::rockfall);
                }
            });
        }

        /** Shockwave: Ba-Ba slams the ground in front of her target, 5x5 (7x7 with Shaking Things Up). */
        private void shockwave(Set<Invocation> inv) {
            ServerPlayer target = null;
            double best = 7 * 7;
            for (ServerPlayer p : players()) {
                double d = p.distanceToSqr(baba);
                if (d < best) {
                    best = d;
                    target = p;
                }
            }
            if (target != null) {
                RaidEffects.strike(world(), target.position(), Invocation.babaSlamRadius(inv) + 0.5, 20, 3, baba, ParticleTypes.CLOUD,
                        AttackType.CRUSH, true);
            }
        }

        /**
         * Rolling Stones (OSRS): Ba-Ba leaps to her throne and screams, knocking everyone to the far (west) end; with Mind
         * the Gap! anyone not at the north or south edge falls into the pit. Then ten sets of five boulders roll west.
         */
        private void rollingStones() {
            rolling = true;
            setsLeft = ToaBossRules.BOULDER_SETS;
            lastCracked = -1;
            clearDebris();
            unanchor(baba);
            move(baba, vec(17, 0));
            anchor(baba);
            warn("Ba-Ba leaps to her throne and screams!");
            boolean gap = Invocation.babaMindTheGap(raid.invocations());
            for (ServerPlayer p : players()) {
                double dz = p.getZ() - (center().getZ() + 0.5);
                if (gap && Math.abs(dz) < 14) { // approx: the room's north and south edges are safe
                    Msg.red(p, "You are flung into the gap!");
                    RaidEffects.hitRaw(baba, p, 999);
                    continue;
                }
                p.teleportTo(center().getX() + 0.5 - 16, center().getY() + 1, p.getZ());
                p.hurtMarked = true;
            }
            nextSet = now() + 3;
        }

        private void boulders(long tick, Set<Invocation> inv) {
            if (setsLeft > 0 && now() >= nextSet) {
                setsLeft--;
                // OSRS: one cracked boulder a column (ranged always max-hits it), near the last column's.
                int crackedAt = ToaBossRules.crackedBoulder(lastCracked, rnd());
                lastCracked = crackedAt;
                int hp = RaidScaling.toaScaledHp(ToaBossRules.babaBoulderHp(raid.pathLevel()), 0, 0, raid.partySize(), false);
                for (int i = 0; i < ToaBossRules.BOULDERS_PER_SET; i++) {
                    LivingEntity b = spawnMob("baba_boulder", at(12, -8 + i * 4), false);
                    if (b != null) {
                        raid.setHitpoints(b, hp);
                        if (i == crackedAt) {
                            cracked.add(b);
                            b.setCustomName(net.minecraft.network.chat.Component.literal("Cracked boulder"));
                        }
                    }
                }
                // OSRS: Boulderdash sends them twice as fast.
                nextSet = now() + (inv.contains(Invocation.BOULDERDASH) ? 3 : 6);
            }
            for (LivingEntity b : knownBoulders) {
                if (b.isDeadOrDying()) {
                    com.runecraft.content.systems.CombatAchievementService.countNear(baba,
                            com.runecraft.osrs.content.systems.CombatAchievements.COUNT_BABA_BOULDER);
                }
            }
            knownBoulders.clear();
            List<LivingEntity> rolling = living("baba_boulder");
            knownBoulders.addAll(rolling);
            for (LivingEntity b : rolling) {
                TobRooms.walk(b, vec(-19, b.getZ() - center().getZ() - 0.5));
                for (ServerPlayer p : players()) {
                    if (p.distanceToSqr(b) < 1.4 * 1.4 && !cooldown(p, 2)) {
                        Msg.red(p, "A rolling boulder crushes you!");
                        hurt(null, p, 15 + rnd().nextInt(10)); // approx: heavy
                        p.push(-1.2, 0.3, 0);
                        p.hurtMarked = true;
                    }
                }
                for (LivingEntity bab : living("toa_baba_baboon")) {
                    if (bab.distanceToSqr(b) < 1.4 * 1.4) {
                        bab.kill();
                    }
                }
                if (b.getX() < center().getX() - 18) {
                    world().sendParticles(ParticleTypes.EXPLOSION, b.getX(), b.getY() + 0.5, b.getZ(), 2, 0.4, 0.4, 0.4, 0);
                    b.discard();
                }
            }
            if (setsLeft <= 0 && rolling.isEmpty()) {
                endRolling();
            }
        }

        private void endRolling() {
            rolling = false;
            unanchor(baba);
            say("Ba-Ba leaps down from her throne.");
            nextRockfall = now() + 6;
            nextThrow = now() + 16;
        }

        @Override
        public void onCreatureHit(ServerPlayer player, LivingEntity target, int damage) {
            // OSRS: attacking her with melee from past the boulders' starting line ends the boulders early.
            if (target == baba && rolling && player.getX() > center().getX() + 11 && player.distanceToSqr(baba) < 4 * 4) {
                setsLeft = 0;
                say("Ba-Ba stops throwing boulders!");
            }
        }

        /** Ba-Ba's troupe: 1-4 baboons by party size (10 hitpoints); now and then one hacks at a sarcophagus. */
        private void baboons(long tick, Set<Invocation> inv) {
            int alive = living("toa_baba_baboon").size();
            if (tick % 30 == 0 && alive < RaidScaling.babaBaboons(raid.partySize())) {
                LivingEntity b = spawnMob("toa_baba_baboon", at(rnd().nextInt(10), rnd().nextBoolean() ? -12 : 12), false);
                if (b != null) {
                    raid.setHitpoints(b, RaidScaling.BABA_BABOON_HP);
                }
            }
            if (tick % 10 == 5) {
                List<BlockPos> closed = new ArrayList<>();
                for (Map.Entry<BlockPos, Integer> s : sarcophagi.entrySet()) {
                    if (s.getValue() > 0) {
                        closed.add(s.getKey());
                    }
                }
                List<LivingEntity> troupe = living("toa_baba_baboon");
                if (!closed.isEmpty() && !troupe.isEmpty() && rnd().nextInt(3) == 0) {
                    BlockPos s = closed.get(rnd().nextInt(closed.size()));
                    say("A baboon hacks at a sarcophagus!");
                    damageSarcophagus(s, 20);
                }
            }
            // Opened sarcophagi discharge unstable energy in a cone. // approx: a 3x3 blast in front of it
            if (tick % 20 == 10) {
                for (Map.Entry<BlockPos, Integer> s : sarcophagi.entrySet()) {
                    if (s.getValue() <= 0) {
                        sarcophagusBlast(s.getKey(), inv);
                    }
                }
            }
        }

        private void damageSarcophagus(BlockPos s, int damage) {
            int hp = Math.max(0, sarcophagi.getOrDefault(s, 0) - damage);
            sarcophagi.put(s, hp);
            if (hp == 0) {
                RaidBuilder.set(world(), s.above(), Blocks.JACK_O_LANTERN.defaultBlockState());
                say("A sarcophagus bursts open! Energy pours out.");
            }
        }

        private void sarcophagusBlast(BlockPos s, Set<Invocation> inv) {
            int dz = s.getZ() < center().getZ() ? 1 : -1;
            Vec3 at = Vec3.atBottomCenterOf(s.offset(0, 0, dz * (2 + Math.min(4, raid.pathLevel()) / 2)));
            double radius = 1.2 + 0.6 * (Math.min(4, raid.pathLevel()) / 2);
            world().sendParticles(ParticleTypes.SOUL, at.x, at.y + 0.2, at.z, 20, radius / 2, 0.1, radius / 2, 0.01);
            later(3, () -> {
                for (ServerPlayer pl : players()) {
                    if (RaidEffects.horizontalDistSq(pl.position(), at) <= radius * radius) {
                        int dmg = rnd().nextInt(13) + Invocation.babaFaithDamage(inv, RoomLogic.prayerPoints(pl)); // approx: 0-12
                        hurt(baba, pl, dmg);
                    }
                }
            });
        }

        /** Jungle Japes: dead baboons drop two banana peels that hurt and stun whoever steps on one (OSRS). */
        private void bananas(long tick, Set<Invocation> inv) {
            int per = Invocation.babaBananaPeels(inv);
            if (per <= 0) {
                return;
            }
            baboons.addAll(living("toa_baba_baboon"));
            java.util.Iterator<LivingEntity> it = baboons.iterator();
            while (it.hasNext()) {
                LivingEntity b = it.next();
                if (b.isAlive() && !b.isRemoved()) {
                    continue;
                }
                it.remove();
                if (b.isDeadOrDying()) {
                    for (int i = 0; i < per; i++) {
                        peels.put(b.position().add(rnd().nextDouble() * 2 - 1, 0, rnd().nextDouble() * 2 - 1), tick + 100);
                    }
                }
            }
            peels.entrySet().removeIf(e -> e.getValue() < tick);
            java.util.Iterator<Map.Entry<Vec3, Long>> pi = peels.entrySet().iterator();
            while (pi.hasNext()) {
                Vec3 peel = pi.next().getKey();
                if (tick % 4 == 0) {
                    world().sendParticles(ParticleTypes.FALLING_HONEY, peel.x, peel.y + 0.3, peel.z, 2, 0.2, 0.1, 0.2, 0);
                }
                for (ServerPlayer p : players()) {
                    if (RaidEffects.horizontalDistSq(p.position(), peel) < 0.7 * 0.7) {
                        Msg.red(p, "You slip on a banana peel!");
                        hurt(null, p, 1 + rnd().nextInt(3)); // approx: a small amount of damage
                        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Invocation.BANANA_STUN_TICKS * 12, 9, false, false));
                        pi.remove();
                        break;
                    }
                }
            }
        }

        @Override
        public void cleanup() {
            super.cleanup();
            for (String k : new String[]{"baba_boulder", "toa_baba_baboon"}) {
                living(k).forEach(LivingEntity::discard);
            }
            clearDebris();
        }

        /** While the boulders roll, players deal with them (and can't reach her throne). */
        @Override
        boolean selfTestHolds() {
            return rolling;
        }

        /** Whether this boulder is the cracked one (ranged always hits it for its maximum). */
        boolean isCracked(LivingEntity b) {
            return cracked.contains(b);
        }

        @Override
        String selfTestSolve(RaidSelfTest t) {
            int[] boulders = {0};
            String r = grind(t, baba, AttackType.CRUSH, 30, () -> {
                for (LivingEntity b : living("baba_boulder")) {
                    t.hit(b, 40, AttackType.RANGED);
                    boulders[0]++;
                }
            });
            RaidSelfTest.expect(phase == 2, "Ba-Ba: the rolling boulders did not come at 66% and 33% (" + phase + ")");
            return r + ", " + boulders[0] + " boulder hits";
        }
    }

    // ================================================================== Zebak

    /**
     * Zebak (OSRS wiki): never moves; his melee (max 38) only reaches players next to him, his rock (ranged) and jug
     * (magic) projectiles (max 16) land on their target and can be prayed against. At 85/70/55/40% he alternates his Great
     * Roar (rocks to hide behind, the roar hits three times) and Tidal Waves (waves sweep from the south, then the north,
     * with a gap to stand in); both spit jugs and acid. Acid pools stay until a jug's water washes them away (5x5, or 3x3
     * with Upset Stomach, whose acid splashes 5x5). Below 25% he enrages: attacks every 4 ticks, no more specials.
     */
    static final class Zebak extends PathBoss {
        DataBossEntity zebak;
        boolean enraged;
        private int specials;
        private long nextAttack;
        /** Acid pools: block positions on the floor level (standing positions), with the tick they were made. */
        final Map<BlockPos, Long> acid = new HashMap<>();
        /** Rocks from the Great Roar and their hitpoints. */
        final Map<BlockPos, Integer> rocks = new HashMap<>();
        final Set<BlockPos> jugs = new HashSet<>();
        private int roarIn = -1;
        private int roarHits;
        /** Live blood clouds: x, z and hitpoints. */
        private final List<double[]> clouds = new ArrayList<>();
        private final Map<UUID, Integer> acidTicks = new HashMap<>();

        Zebak(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            BlockPos c = center();
            RaidBuilder.fill(world(), c.getX() + 16, c.getY(), c.getZ() - 8, c.getX() + 19, c.getY(), c.getZ() + 8,
                    Blocks.WATER.defaultBlockState());
        }

        @Override
        public void start() {
            enraged = false;
            specials = 0;
            roarIn = -1;
            clouds.clear();
            clearArena();
            zebak = spawnBoss("zebak", at(13, 0));
            if (zebak != null) {
                zebak.setNoAi(true);
                anchor(zebak);
            }
            nextAttack = now() + 4;
        }

        private void clearArena() {
            for (BlockPos p : acid.keySet()) {
                RaidBuilder.set(world(), p.below(), RaidBuilder.palette(def.style()).floor());
            }
            acid.clear();
            for (BlockPos p : rocks.keySet()) {
                RaidBuilder.set(world(), p, Blocks.AIR.defaultBlockState());
                RaidBuilder.set(world(), p.above(), Blocks.AIR.defaultBlockState());
            }
            rocks.clear();
            for (BlockPos p : jugs) {
                RaidBuilder.set(world(), p, Blocks.AIR.defaultBlockState());
            }
            jugs.clear();
            acidTicks.clear();
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            if (!alive(zebak)) {
                return;
            }
            double hp = raid.healthFraction(zebak) * 100;
            if (!enraged && hp <= ToaBossRules.ZEBAK_ENRAGE) {
                enraged = true;
                roarIn = -1;
                warn("Zebak roars in fury - he attacks much faster now!");
            }
            while (!enraged && specials < ToaBossRules.ZEBAK_SPECIALS.length && hp <= ToaBossRules.ZEBAK_SPECIALS[specials]) {
                special(specials % 2 == 0);
                specials++;
            }
            if (now() >= nextAttack) {
                attack();
                nextAttack = now() + ToaBossRules.zebakAttackSpeed(enraged, raid.pathLevel());
            }
            if (roarIn > 0 && --roarIn == 0) {
                roar();
            }
            acidPools();
            bloodMagic(tick);
            moveClouds();
        }

        /** Melee on whoever stands next to him, else a rock (ranged) or jug (magic) projectile at a random player. */
        private void attack() {
            List<ServerPlayer> here = players();
            if (here.isEmpty()) {
                return;
            }
            for (ServerPlayer p : here) {
                if (p.distanceToSqr(zebak) < 3.5 * 3.5) {
                    // OSRS: his melee hits through prayer at reduced damage, and can make you bleed. // approx: half
                    int roll = rnd().nextInt(ToaBossRules.ZEBAK_MELEE_MAX + 1);
                    if (RaidEffects.protectedFrom(p, AttackType.STAB)) {
                        roll /= 2;
                    }
                    Msg.red(p, "Zebak's jaws snap at you!");
                    RaidEffects.hit(zebak, p, roll, null, true);
                    return;
                }
            }
            ServerPlayer target = here.get(rnd().nextInt(here.size()));
            AttackType style = rnd().nextBoolean() ? AttackType.RANGED : AttackType.MAGIC;
            world().sendParticles(style == AttackType.MAGIC ? ParticleTypes.DAMAGE_INDICATOR : ParticleTypes.CRIT, zebak.getX(), zebak.getY() + 2,
                    zebak.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
            // OSRS: the projectile only hurts on impact, so it can be prayed against late.
            later(2, () -> {
                if (alive(zebak) && target.isAlive()) {
                    RaidEffects.hit(zebak, target, rnd().nextInt(ToaBossRules.ZEBAK_RANGED_MAX + 1), style, false);
                }
            });
        }

        /** A special: jugs and acid with the Great Roar or the Tidal Waves. */
        private void special(boolean roar) {
            spitJugsAndAcid();
            if (roar) {
                warn("Zebak spits rocks across the room... hide behind one!");
                for (int i = 0; i < 2 + rnd().nextInt(2); i++) {
                    BlockPos r = at(4 - rnd().nextInt(6), -12 + i * 10 + rnd().nextInt(4));
                    if (acid.containsKey(r)) {
                        continue;
                    }
                    RaidBuilder.set(world(), r, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
                    RaidBuilder.set(world(), r.above(), Blocks.MOSSY_COBBLESTONE_WALL.defaultBlockState());
                    rocks.put(r, ToaBossRules.ZEBAK_ROCK_HP);
                }
                roarIn = 10; // approx: "several attacks after"
                roarHits = 3;
            } else {
                tidalWaves();
            }
        }

        private void spitJugsAndAcid() {
            Set<Invocation> inv = raid.invocations();
            // OSRS: two extra jugs appear since September 2022. // approx: four jugs, six acid splats
            for (int i = 0; i < 4; i++) {
                BlockPos j = at(-12 + rnd().nextInt(20), -15 + rnd().nextInt(30));
                if (rocks.containsKey(j) || jugs.contains(j)) {
                    continue;
                }
                RaidBuilder.set(world(), j, RaidBuilder.module(RaidsContent.TOA_ZEBAK_JUG));
                jugs.add(j);
            }
            int spread = inv.contains(Invocation.UPSET_STOMACH) ? 2 : 1;
            for (int i = 0; i < 6; i++) {
                BlockPos centre = at(-14 + rnd().nextInt(24), -15 + rnd().nextInt(30));
                for (int dx = -spread; dx <= spread; dx++) {
                    for (int dz = -spread; dz <= spread; dz++) {
                        if (rnd().nextInt(3) == 0 && !(dx == 0 && dz == 0)) {
                            continue;
                        }
                        BlockPos p = centre.offset(dx, 0, dz);
                        if (!inside(Vec3.atCenterOf(p)) || rocks.containsKey(p) || jugs.contains(p)) {
                            continue;
                        }
                        acid.put(p, now());
                        RaidBuilder.set(world(), p.below(), Blocks.LIME_TERRACOTTA.defaultBlockState());
                    }
                }
            }
        }

        /** Standing in acid: low damage at first, quickly dangerous, and poison (OSRS). */
        private void acidPools() {
            for (ServerPlayer p : players()) {
                BlockPos stand = p.blockPosition();
                if (acid.containsKey(stand)) {
                    int ticks = acidTicks.merge(p.getUUID(), 1, Integer::sum);
                    hurt(zebak, p, Math.min(30, 2 + ticks * ticks)); // approx: ramps up fast
                    if (!PoisonService.isPoisoned(p)) {
                        PoisonService.poison(p, 2, com.runecraft.action.ActionManager.gameTick());
                    }
                } else {
                    acidTicks.remove(p.getUUID());
                }
            }
        }

        /** The Great Roar: three hits on everyone not hidden behind a rock (each rock takes 50 a hit). */
        private void roar() {
            warn("Zebak lets out a great roar!");
            for (int k = 0; k < roarHits; k++) {
                later(1 + k, () -> {
                    if (!alive(zebak)) {
                        return;
                    }
                    for (ServerPlayer p : players()) {
                        BlockPos rock = coverFor(p);
                        if (rock != null) {
                            int left = rocks.getOrDefault(rock, 0) - ToaBossRules.ZEBAK_ROAR_ON_ROCK;
                            if (left <= 0) {
                                rocks.remove(rock);
                                RaidBuilder.set(world(), rock, Blocks.AIR.defaultBlockState());
                                RaidBuilder.set(world(), rock.above(), Blocks.AIR.defaultBlockState());
                            } else {
                                rocks.put(rock, left);
                            }
                            continue;
                        }
                        world().sendParticles(ParticleTypes.SONIC_BOOM, p.getX(), p.getY() + 1, p.getZ(), 1, 0, 0, 0, 0);
                        hurt(zebak, p, 30 + rnd().nextInt(21)); // approx: massive
                    }
                });
            }
        }

        /** The rock a player shelters behind: west of it (away from Zebak), up to three tiles, in its row. */
        private BlockPos coverFor(ServerPlayer p) {
            BlockPos stand = p.blockPosition();
            for (BlockPos r : rocks.keySet()) {
                int behind = r.getX() - stand.getX();
                if (behind >= 1 && behind <= 3 && Math.abs(r.getZ() - stand.getZ()) <= 0) {
                    return r;
                }
            }
            return null;
        }

        /**
         * Tidal Waves (OSRS): the first from the south, the next from the north; a wave sweeps the whole room except its
         * gap, pushing and hurting anyone caught.
         */
        private void tidalWaves() {
            boolean fromSouth = specials % 4 == 1;
            warn("Zebak slams his tail - a tidal wave rises in the " + (fromSouth ? "south" : "north") + "!");
            int gap = ToaBossRules.zebakWaveGap(raid.pathLevel());
            for (int w = 0; w < 3; w++) {
                int gapX = -14 + rnd().nextInt(28 - gap);
                int delay = 3 + w * 4;
                later(delay, () -> wave(fromSouth, gapX, gap));
            }
        }

        private void wave(boolean fromSouth, int gapX, int gap) {
            if (!alive(zebak)) {
                return;
            }
            for (int x = -20; x <= 20; x += 2) {
                if (x >= gapX && x < gapX + gap) {
                    continue;
                }
                world().sendParticles(ParticleTypes.SPLASH, center().getX() + 0.5 + x, center().getY() + 1.5, center().getZ() + (fromSouth ? 12 : -12),
                        12, 0.6, 0.6, 2.5, 0.2);
            }
            for (ServerPlayer p : players()) {
                int px = p.blockPosition().getX() - center().getX();
                if (px >= gapX && px < gapX + gap) {
                    continue;
                }
                Msg.red(p, "The wave crashes into you!");
                hurt(zebak, p, 15 + rnd().nextInt(11)); // approx: moderate
                p.push(0, 0.3, fromSouth ? -1.2 : 1.2);
                p.hurtMarked = true;
            }
            // The wave washes some acid away.
            acid.entrySet().removeIf(e -> {
                if (rnd().nextInt(3) == 0) {
                    RaidBuilder.set(world(), e.getKey().below(), RaidBuilder.palette(def.style()).floor());
                    return true;
                }
                return false;
            });
        }

        /** OSRS Not Just a Head: blood blitz on everyone (8-11, healing him) or blood clouds (three with Blood Thinners). */
        private void bloodMagic(long tick) {
            Set<Invocation> inv = raid.invocations();
            if (!inv.contains(Invocation.NOT_JUST_A_HEAD) || tick % 24 != 12) {
                return;
            }
            if (rnd().nextBoolean()) {
                say("Zebak casts blood magic!");
                int total = 0;
                for (ServerPlayer p : players()) {
                    world().sendParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY() + 1, p.getZ(), 6, 0.3, 0.4, 0.3, 0.1);
                    total += RaidEffects.hit(zebak, p, 8 + rnd().nextInt(4), AttackType.MAGIC, false);
                }
                double share = inv.contains(Invocation.ARTERIAL_SPRAY) ? 1.0 : 2 / 3.0;
                if (total > 0) {
                    raid.heal(zebak, (int) Math.round(total * share));
                }
            } else {
                int n = Invocation.zebakBloodClouds(inv);
                say(n > 1 ? "Zebak summons blood clouds!" : "Zebak summons a blood cloud!");
                boolean north = rnd().nextBoolean();
                for (int i = 0; i < n; i++) {
                    // OSRS: they appear by the ramps at either side of the arena.
                    clouds.add(new double[]{center().getX() + 6.5 + i * 2, center().getZ() + 0.5 + (north ? -17 : 17), Invocation.BLOOD_CLOUD_HP});
                }
            }
        }

        /** Blood clouds drift a tile a tick at the nearest player, losing 2 hitpoints a tile, and heal on contact. */
        private void moveClouds() {
            java.util.Iterator<double[]> it = clouds.iterator();
            double y = center().getY() + 1.2;
            while (it.hasNext()) {
                double[] c = it.next();
                ServerPlayer near = null;
                double best = Double.MAX_VALUE;
                for (ServerPlayer p : players()) {
                    double d = (p.getX() - c[0]) * (p.getX() - c[0]) + (p.getZ() - c[1]) * (p.getZ() - c[1]);
                    if (d < best) {
                        best = d;
                        near = p;
                    }
                }
                if (near == null) {
                    it.remove();
                    continue;
                }
                // OSRS: a cloud loses 2 hitpoints every tick and siphons a player it touches to prolong itself.
                c[2] -= Invocation.BLOOD_CLOUD_COST_PER_TILE;
                if (best <= 2.25) {
                    c[2] += hurt(zebak, near, 3 + rnd().nextInt(4));
                } else {
                    double dist = Math.sqrt(best);
                    c[0] += (near.getX() - c[0]) / dist;
                    c[1] += (near.getZ() - c[1]) / dist;
                }
                world().sendParticles(new DustParticleOptions(new Vector3f(0.6f, 0.0f, 0.0f), 2.0f), c[0], y, c[1], 8, 0.4, 0.3, 0.4, 0);
                if (c[2] <= 0) {
                    world().sendParticles(ParticleTypes.SMOKE, c[0], y, c[1], 10, 0.3, 0.3, 0.3, 0.02);
                    it.remove();
                }
            }
        }

        /** A jug: it rolls away from the player until it hits a rock or wall, then shatters and washes the acid away. */
        @Override
        public boolean useBlock(ServerPlayer player, String blockId, BlockPos pos) {
            if (!blockId.equals(RaidsContent.TOA_ZEBAK_JUG) || !jugs.contains(pos)) {
                return false;
            }
            int dx = Integer.signum(pos.getX() - player.getBlockX());
            int dz = Integer.signum(pos.getZ() - player.getBlockZ());
            if (dx == 0 && dz == 0) {
                dx = 1;
            }
            BlockPos at = pos;
            jugs.remove(pos);
            RaidBuilder.set(world(), pos, Blocks.AIR.defaultBlockState());
            for (int i = 0; i < 6; i++) {
                BlockPos next = at.offset(dx, 0, dz);
                if (!inside(Vec3.atCenterOf(next)) || rocks.containsKey(next) || jugs.contains(next)
                        || !world().getBlockState(next).isAir()) {
                    break;
                }
                at = next;
            }
            shatter(at);
            return true;
        }

        private void shatter(BlockPos at) {
            int r = raid.invocations().contains(Invocation.UPSET_STOMACH) ? 1 : 2;
            world().sendParticles(ParticleTypes.SPLASH, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 40, r, 0.3, r, 0.1);
            world().playSound(null, at, SoundEvents.DECORATED_POT_SHATTER, SoundSource.BLOCKS, 1f, 1f);
            acid.entrySet().removeIf(e -> {
                BlockPos p = e.getKey();
                if (Math.abs(p.getX() - at.getX()) <= r && Math.abs(p.getZ() - at.getZ()) <= r) {
                    RaidBuilder.set(world(), p.below(), RaidBuilder.palette(def.style()).floor());
                    return true;
                }
                return false;
            });
        }

        @Override
        public void cleanup() {
            super.cleanup();
            clouds.clear();
            if (raid.world.getServer() != null) {
                clearArena();
            }
        }

        @Override
        AttackType styleFor(DataBossEntity boss, AttackType wanted) {
            return AttackType.RANGED;
        }

        @Override
        String selfTestSolve(RaidSelfTest t) {
            int[] jugsBroken = {0};
            String r = grind(t, zebak, AttackType.RANGED, 25, () -> {
                if (!jugs.isEmpty() && !acid.isEmpty()) {
                    BlockPos j = jugs.iterator().next();
                    int before = acid.size();
                    t.moveTo(j.offset(-1, 0, 0));
                    t.use(j);
                    jugsBroken[0]++;
                    RaidSelfTest.expect(!jugs.contains(j), "Zebak: the jug did not roll away");
                    if (before > 0 && acid.size() > before) {
                        throw RaidSelfTest.fail("Zebak: breaking a jug added acid");
                    }
                }
            });
            RaidSelfTest.expect(specials >= 2 || enraged, "Zebak: no specials happened");
            return r + ", " + jugsBroken[0] + " jugs broken";
        }
    }
}
