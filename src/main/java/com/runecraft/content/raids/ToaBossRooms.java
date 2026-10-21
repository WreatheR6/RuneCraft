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
import com.runecraft.osrs.content.raids.ToaLoot;
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
    /**
     * Players encased in Akkha's ice and the raid game tick it thaws (OSRS: their attacks are a tick slower until then): each
     * attack they make adds a game tick to their weapon delay (round 43).
     */
    static final Map<UUID, Long> ICED = new HashMap<>();
    /** The weapon delay each iced player was last slowed at, so one multi-hit attack is slowed once. */
    private static final Map<UUID, Long> ICED_AT = new HashMap<>();

    /** Combat hooks of the ToA bosses (round 43): Akkha's ice slows the attacks of whoever it encases. */
    static void init() {
        com.runecraft.combat.CombatHooks.addModifier(new com.runecraft.combat.CombatHooks.PlayerAttackModifier() {
            @Override
            public void afterHit(ServerPlayer player, LivingEntity target, AttackType type, int dealt) {
                Long until = ICED.get(player.getUUID());
                if (until == null) {
                    return;
                }
                if (RaidEffects.now() >= until) {
                    ICED.remove(player.getUUID());
                    ICED_AT.remove(player.getUUID());
                    return;
                }
                PlayerData d = Players.data(player);
                if (ICED_AT.getOrDefault(player.getUUID(), Long.MIN_VALUE) == d.lastAttackTick) {
                    return;
                }
                com.runecraft.combat.AttackTimer.addDelay(player, com.runecraft.action.ActionManager.MC_TICKS_PER_GAME_TICK);
                ICED_AT.put(player.getUUID(), d.lastAttackTick);
            }
        });
    }
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

        /**
         * OSRS Combat Achievements "Perfect <boss>": the fight's damage conditions ({@link #perfectDamageRule}, checked on
         * each player's tracked fight) and the room's own ({@link #perfectRoom}); a perfect kill flags the raid run for the
         * "Perfection of ..." tasks.
         */
        @Override
        public void bossDeath(DataBossEntity boss, ServerPlayer killer) {
            String flag = perfectFlag();
            if (flag == null || boss != mainBoss() || !perfectRoom()) {
                return;
            }
            for (ServerPlayer p : players()) {
                if (com.runecraft.content.systems.CombatAchievementService.fightMeets(p, boss, perfectDamageRule())) {
                    com.runecraft.content.systems.CombatAchievementService.flag(p, flag);
                }
            }
        }

        /** The raid-run flag of a perfect kill of this room's boss, or null. */
        String perfectFlag() {
            return null;
        }

        /** The room's boss. */
        abstract DataBossEntity mainBoss();

        /** The damage the party must not take for a perfect kill (CombatAchievementRules language). */
        String perfectDamageRule() {
            return "nodmg";
        }

        /** The room's own conditions for a perfect kill (invocations, party size, nothing hatched or opened...). */
        boolean perfectRoom() {
            return true;
        }

        /** Whether every one of these invocations is active. */
        boolean all(java.util.Set<Invocation> needed) {
            return raid.invocations().containsAll(needed);
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
        /** Shield the swarms that reached her during an intermission restore (they can overcharge it). */
        private int shieldPool;
        private int overcharge;
        /** CA "All out of medics": she healed above 25% after the first down. */
        boolean healedOver25;
        /** CA "Perfect Kephri": an egg hatched. */
        boolean eggHatched;
        /** The shield (percent of its capacity) she woke with after each intermission (self-test). */
        final List<Integer> wakeShields = new ArrayList<>();
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
            shieldPool = 0;
            overcharge = 0;
            healedOver25 = false;
            eggHatched = false;
            wakeShields.clear();
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
            shieldPool = 0;
            leaked = 0;
            warn("Kephri's shield shatters! Her children rush to her aid!");
            guards.clear();
            for (String id : Invocation.kephriOverlords(inv, broken)) {
                int[] spot = id.equals("toa_arcane_scarab") ? ARCANE_SPOTS[0] : id.equals("toa_spitting_scarab") ? new int[]{6, -8} : new int[]{4, 0};
                LivingEntity g = spawnMob(id, at(spot[0], spot[1] + (guards.size() > 2 ? 3 : 0)), false);
                if (g != null) {
                    guards.add(g);
                    if (id.equals("toa_arcane_scarab")) {
                        // OSRS: the Arcane Scarab doesn't attack; it only charges its explosive swarm.
                        if (g instanceof Mob m) {
                            m.setNoAi(true);
                        }
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
            int trioEvery = Invocation.kephriTrioEvery(raid.pathLevel());
            for (int i = 0; i < swarms; i++) {
                delay += Invocation.kephriSwarmDelay(i);
                int point = (start + i) % 6;
                int n = trioEvery > 0 && i % trioEvery == trioEvery - 1 ? 3 : 1;
                later(delay, () -> {
                    if (alive(kephri) && dazed) {
                        double a = Math.PI / 3 * point + Math.PI / 6;
                        for (int k = 0; k < n; k++) {
                            LivingEntity swarm = spawnMob("toa_scarab_swarm", at((int) Math.round(Math.cos(a) * 16) + k,
                                    (int) Math.round(Math.sin(a) * 16)), false);
                            markSwarm(swarm);
                        }
                    }
                });
            }
            later(delay + 4, this::endIntermission);
            RaidEffects.telegraph(players(), "Kephri calls " + swarms + " swarms - kill them before they reach her!");
        }

        /**
         * A scarab swarm outlined and its way to Kephri drawn while it lives (round 43). // playability: OSRS swarms are small
         * dark clusters on dark sand, read by their movement; here they glow and their path is dotted
         */
        private void markSwarm(LivingEntity swarm) {
            if (swarm == null) {
                return;
            }
            swarm.setGlowingTag(true);
            RaidEffects.cue(600, center().getCenter(), () -> {
                if (!alive(swarm) || !alive(kephri)) {
                    return false;
                }
                Vec3 a = swarm.position();
                Vec3 b = kephri.position();
                for (int k = 1; k <= 4; k++) {
                    Vec3 at = a.lerp(b, k / 5.0);
                    world().sendParticles(SWARM_PATH, at.x, at.y + 0.2, at.z, 1, 0, 0, 0, 0);
                }
                return true;
            });
        }

        private static final DustParticleOptions SWARM_PATH = new DustParticleOptions(new Vector3f(0.55f, 0.35f, 0.9f), 0.9f);

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
            // OSRS wiki: the shield comes back only as far as the swarms that reached her restored it (about 10% each,
            // less in big groups), overcharging it up to about 15% over its capacity; a party that leaks no swarm faces
            // an all but empty shield.
            raid.setCurrentHitpoints(kephri, Math.max(1, Math.min(max, shieldPool)));
            wakeShields.add(shieldPool * 100 / Math.max(1, max));
            overcharge = Math.min(max * 15 / 100, Math.max(0, shieldPool - max));
            warn("Kephri wakes: her swarms restored " + Math.min(115, shieldPool * 100 / Math.max(1, max)) + "% of her shield"
                    + (overcharge > 0 ? " and overcharged it!" : "."));
            checkMedics();
            shieldPool = 0;
            leaked = 0;
        }

        /** CA "All out of medics": after the first down she must never heal above 25%. */
        private void checkMedics() {
            if (broken >= 1 && broken < 3 && alive(kephri) && raid.healthFraction(kephri) > 0.25 && !healedOver25) {
                healedOver25 = true;
                com.runecraft.content.systems.CombatAchievementService.flagNear(kephri, ToaLoot.FLAG_KEPHRI_OVER_25);
            }
        }

        @Override
        DataBossEntity mainBoss() {
            return kephri;
        }

        @Override
        String perfectFlag() {
            return ToaLoot.FLAG_PERFECT_KEPHRI;
        }

        /** OSRS "Perfect Kephri": no damage from eggs, her attacks, exploding scarabs, bodyguards or dung; no egg hatches. */
        @Override
        String perfectDamageRule() {
            return "nodmg=boss,other,mechanic,hazard";
        }

        @Override
        boolean perfectRoom() {
            return raid.partySize() >= 2 && !eggHatched;
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

        /** Blocks the dung row runs on from the struck spot, away from Kephri. // approx */
        static final int DUNG_ROW_LENGTH = 6;
        /** Game ticks the dung piles lie on the floor. // approx */
        static final int DUNG_TICKS = 25;
        /** Self-test: dung strikes made, players they hit, and how far the last one threw its target. */
        int dungStrikes;
        int dungHits;
        double lastThrow;

        /**
         * Dung Strike (OSRS wiki): flies buzz round a player, warning the team; then Kephri throws them back, stunned for two
         * seconds, and dung falls along the way they flew; whoever is in the way takes light damage and is shoved aside. Round
         * 43: the flies' spot and the row behind it are marked (on the floor too) from the buzzing, so stepping aside dodges it,
         * and the dung lies on the floor afterwards. // playability: OSRS throws the target on the attack tick
         */
        private void dungStrike(ServerPlayer p) {
            Msg.red(p, "Flies buzz around you...");
            Msg.telegraph(p, "Kephri: dung strike - step aside!", null);
            dungStrikes++;
            Vec3 spot = p.position();
            Vec3 away = spot.subtract(kephri.position()).multiply(1, 0, 1);
            Vec3 dir = away.lengthSqr() < 1e-4 ? new Vec3(-1, 0, 0) : away.normalize();
            Vec3 end = spot.add(dir.scale(DUNG_ROW_LENGTH));
            RaidEffects.cue(3, center().getCenter(), () -> {
                if (!p.isAlive() || !alive(kephri)) {
                    return false;
                }
                world().sendParticles(ParticleTypes.ITEM_SLIME, p.getX(), p.getY() + 1.6, p.getZ(), 4, 0.4, 0.3, 0.4, 0.02);
                return true;
            });
            DataBossEntity source = kephri;
            java.util.function.BooleanSupplier attempt = current();
            RaidEffects.lane(world(), spot, end, 0.8).delay(3).source(source).warn(ParticleTypes.ITEM_SLIME).floor(RaidFloor.DANGER)
                    .live(() -> attempt.getAsBoolean() && alive(source))
                    .onHit(q -> {
                        dungHits++;
                        hurt(source, q, 5 + rnd().nextInt(11)); // approx: "light damage"
                        if (q == p) {
                            Msg.red(q, "Kephri throws you back!");
                            lastThrow = shove(q, dir, DUNG_ROW_LENGTH + 2);
                            com.runecraft.content.systems.SysCombat.freeze(q, 3); // OSRS: stunned for 2 seconds
                        } else {
                            Msg.red(q, "Dung falls on you!");
                            shove(q, new Vec3(-dir.z, 0, dir.x).scale(rnd().nextBoolean() ? 1 : -1), 2);
                        }
                        world().sendParticles(ParticleTypes.ITEM_SLIME, q.getX(), q.getY() + 0.5, q.getZ(), 12, 0.6, 0.3, 0.6, 0.05);
                    })
                    .onLand(() -> RaidFloor.lane(world(), spot, end, 0.5, RaidFloor.DUNG, DUNG_TICKS, source))
                    .start();
        }

        /**
         * Kephri's fireball (and a bomber scarab's): lands where the target stood, max hit 24 (OSRS); 3x3 and harder with
         * Aerial Assault. Protect from Magic takes about two thirds off (OSRS wiki).
         */
        private void fireball(ServerPlayer p) {
            Set<Invocation> inv = raid.invocations();
            int radius = Invocation.kephriFireballRadius(inv);
            int max = (int) Math.round(KEPHRI_MAX_HIT * Invocation.kephriFireballDamage(inv));
            // its shadow on the floor too (round 43). // playability: OSRS shows the shadow on the tiles
            RaidEffects.mark(world(), p.position(), 0.8 + 1.5 * radius).delay(3).source(kephri).warn(ParticleTypes.FLAME).damage(0, max)
                    .style(AttackType.MAGIC, KEPHRI_PROTECTED_SHARE).floor(RaidFloor.DANGER).start();
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
            // OSRS: a swarm that reaches her recharges about 10% of a shield (heals her body in the last phase). While she
            // is dazed it builds up the shield she wakes with.
            int hpBefore = raid.hitpoints(kephri);
            int reached = healers("toa_scarab_swarm", kephri, raid.maxHitpoints(kephri) * 0.01,
                    "A scarab swarm reaches Kephri and recharges her shield!");
            if (dazed && reached > 0) {
                leaked += reached;
                shieldPool += Math.max(0, raid.hitpoints(kephri) - hpBefore);
                raid.setCurrentHitpoints(kephri, 1);
            } else if (reached > 0) {
                checkMedics();
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
                        markSwarm(swarm);
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
                    // OSRS: "well over 65 damage" unless Protect from Magic is up (which cuts about two thirds); its infobox
                    // max hit is 22. // approx: three hits of up to 22
                    warn("The Arcane Scarab unleashes its explosive swarm!");
                    for (ServerPlayer p : players) {
                        int roll = 0;
                        for (int k = 0; k < 3; k++) {
                            roll += rnd().nextInt(ToaBossRules.ARCANE_SCARAB_MAX_HIT + 1);
                        }
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
                    eggHatched = true;
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
            // OSRS: her shield only comes back as far as the swarms that reached her restored it (none leaked here).
            RaidSelfTest.expect(wakeShields.size() == 2 && wakeShields.get(0) < 100,
                    "Kephri woke with a full shield although no swarm reached her: " + wakeShields);
            return r + ", " + eggsBroken[0] + " eggs broken, woke with " + wakeShields + "% shields";
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
        /** Game ticks the ice quadrant's element lasts. // approx */
        static final int ICE_TICKS = 10;
        DataBossEntity akkha;
        int style;
        /** Attacks made (autos and specials), and auto-attacks since the last special. */
        int attacks;
        private int autos;
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
        /** Trailing orbs players dropped by moving (self-test, CA "Dropped the ball"). */
        int orbsDropped;

        Akkha(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            BlockPos c = center();
            // The four quadrant symbols: fire, shadow, lightning, ice. The ice mark is light blue terracotta rather than packed
            // ice since round 42: steps on a slippery block don't drop trailing orbs (they may be a slide), so an icy mark
            // was a patch to walk across freely.
            net.minecraft.world.level.block.state.BlockState[] marks = {Blocks.MAGMA_BLOCK.defaultBlockState(), Blocks.CRYING_OBSIDIAN.defaultBlockState(),
                    Blocks.YELLOW_GLAZED_TERRACOTTA.defaultBlockState(), Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA.defaultBlockState()};
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
            attacks = 0;
            style = 0;
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

        /**
         * Each of Akkha's attacks, counted as it starts (a hit that later fizzles was still an attack; round 41 counted
         * landed hits): every seventh (sixth with Stay Vigilant) is a special instead of an auto-attack, after which he
         * changes style. With Keep Back his ranged and magic attacks are preceded by a melee swing at anyone next to him.
         */
        @Override
        public AttackDef bossBeforeAttack(DataBossEntity boss, ServerPlayer target, AttackDef attack) {
            if (boss != akkha) {
                return attack;
            }
            if (hidden || finalStand) {
                return AttackDef.idle(attack.style()); // vanished for Memory Blast, or merged into a shadow: no attacks
            }
            Set<Invocation> inv = raid.invocations();
            lastAttack = now();
            attacks++;
            if (Invocation.akkhaSpecialTurn(inv, attacks)) {
                // OSRS: six auto-attacks, then the special takes the seventh attack's place (no auto-attack lands).
                boolean switches = Invocation.akkhaSwitchesAfter(inv, true, autos);
                autos = 0;
                later(1, () -> specials(inv));
                if (switches) {
                    switchStyle();
                }
                return AttackDef.idle(attack.style());
            }
            autos++;
            if (!attack.melee() && Invocation.akkhaKeepBack(inv)) {
                // OSRS Keep Back: a melee hit on anyone next to him just before his ranged/magic attack, here after the
                // swing's wind-up and only on a player still in reach. // playability: OSRS lands it on the attack tick
                int meleeMax = attacks(akkha).get(0).maxHit();
                akkha.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                for (ServerPlayer p : players()) {
                    if (p.distanceToSqr(akkha) < 2.6 * 2.6) {
                        com.runecraft.boss.Mechanics.deliverMelee(akkha, p, com.runecraft.osrs.ThreatTiming.MELEE_REACH, () -> {
                            if (alive(akkha) && !hidden && !finalStand) {
                                hitStyled(p, rnd().nextInt(meleeMax + 1), AttackType.SLASH);
                            }
                        });
                    }
                }
            }
            return attack;
        }

        /**
         * One of Akkha's auto-attacks landing: protection only takes part of it off, and his ranged and magic hit everyone
         * in the arena.
         */
        @Override
        public int bossOutgoing(DataBossEntity boss, ServerPlayer player, int damage, AttackType type, AttackDef attack) {
            if (boss != akkha || hidden || finalStand) {
                return 0;
            }
            // the attack that is landing, not his current style (a hit in flight may cross a style switch)
            int max = attack != null ? attack.maxHit() : attacks(akkha).get(style).maxHit();
            if (damage == 0 && type != null && RaidEffects.protectedFrom(player, type)) {
                damage = (int) Math.round(rnd().nextInt(max + 1)
                        * (type.isMelee() ? ToaBossRules.AKKHA_PROTECTED_MELEE : ToaBossRules.AKKHA_PROTECTED_RANGED_MAGIC));
            }
            if (type != null && !type.isMelee()) {
                // OSRS: his ranged and magic attacks hit everyone in the arena.
                for (ServerPlayer p : players()) {
                    if (p != player) {
                        hitStyled(p, rnd().nextInt(max + 1), type);
                    }
                }
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

        @Override
        DataBossEntity mainBoss() {
            return akkha;
        }

        @Override
        String perfectFlag() {
            return ToaLoot.FLAG_PERFECT_AKKHA;
        }

        /**
         * OSRS "Perfect Akkha": no damage from his attacks off-prayer, his specials (orbs, memory, detonate), the shadows'
         * timers or the enrage orbs (all mechanic hits here); a group with every Akkha invocation.
         */
        @Override
        String perfectDamageRule() {
            return "nodmg=mechanic,hazard,offprayer";
        }

        @Override
        boolean perfectRoom() {
            return raid.partySize() >= 2 && all(ToaLoot.AKKHA_INVOCATIONS);
        }

        private void switchStyle() {
            style = Invocation.akkhaNextStyle(style);
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
                    shadowSlam(q);
                }
            }
        }

        /** Shadow slams landed, and the game ticks between the last one's warning and its landing (self-test). */
        int shadowSlams;
        long lastShadowSlamLead = -1;

        /**
         * A shadow's hourglass is full: it raises its spear and the whole quadrant is marked on the floor, then the slam envelops
         * whoever is still in it (Akkha's Shadow's infobox max hit of 22, plus the quadrant's element). A shadow killed meanwhile
         * spares it. Its charge then starts again from below zero (OSRS: it resumes after a short time). // playability: OSRS
         * slams as the hourglass fills, the element spreading from the edge to the centre
         */
        void shadowSlam(int q) {
            int lead = com.runecraft.osrs.content.raids.RaidRealtime.SHADOW_SLAM_LEAD_TICKS;
            shadowCharge[q] = -lead;
            String where = com.runecraft.osrs.content.raids.RaidRealtime.quarter(q == 0 || q == 3, q <= 1);
            warn("The shadow in the " + ToaBossRules.QUADRANTS[q] + " quadrant (" + where + ") raises its spear - leave that quadrant!");
            markQuadrant(q, RaidFloor.DANGER, lead);
            LivingEntity shadow = shadows[q];
            long made = now();
            later(lead, () -> {
                if (!alive(akkha) || !alive(shadow) || finalStand) {
                    return;
                }
                shadowSlams++;
                lastShadowSlamLead = now() - made;
                say("The shadow in the " + ToaBossRules.QUADRANTS[q] + " quadrant slams its spear down!");
                for (ServerPlayer p : players()) {
                    if (quadrant(p.position()) == q) {
                        hurt(akkha, p, ToaBossRules.AKKHA_SHADOW_MAX_HIT / 2 + rnd().nextInt(ToaBossRules.AKKHA_SHADOW_MAX_HIT / 2 + 1));
                        element(p, q);
                    }
                }
            });
        }

        /**
         * Marks a quadrant's floor (both centre lines left plain) with {@code kind} for {@code gameTicks} game ticks (round 43).
         * // playability: OSRS shows the quadrant's element spreading over its tiles
         */
        RaidFloor.Group markQuadrant(int q, com.runecraft.boss.HazardKind kind, int gameTicks) {
            BlockPos c = center();
            boolean west = q == 0 || q == 3;
            boolean north = q <= 1;
            int x0 = west ? c.getX() - RaidBuilder.HALF + 1 : c.getX() + 1;
            int z0 = north ? c.getZ() - RaidBuilder.HALF + 1 : c.getZ() + 1;
            return RaidFloor.rect(world(), x0, z0, x0 + RaidBuilder.HALF - 2, z0 + RaidBuilder.HALF - 2, c.getY() + 1, kind, gameTicks,
                    akkha);
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
                    // Ice (OSRS): attacks one tick slower until it thaws. Round 43: on the attack clock (it was a movement slow,
                    // which OSRS doesn't have). // approx: thaws after 10 ticks
                    Msg.red(p, "Ice encases your limbs, slowing your attacks.");
                    long until = now() + ICE_TICKS;
                    ICED.put(p.getUUID(), until);
                    RaidEffects.cue(ICE_TICKS, center().getCenter(), () -> {
                        if (!p.isAlive() || ICED.getOrDefault(p.getUUID(), 0L) != until) {
                            return false;
                        }
                        world().sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 1.2, p.getZ(), 3, 0.3, 0.4, 0.3, 0.01);
                        return true;
                    });
                    later(ICE_TICKS, () -> {
                        if (ICED.remove(p.getUUID(), until)) {
                            ICED_AT.remove(p.getUUID()); // the thawed player's last slowed attack is forgotten too
                            Msg.game(p, "The ice around you melts away.");
                        }
                    });
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

        /** Special attacks used (self-test). */
        int specialsUsed;

        /** Memory Blast, Trailing Orbs and (groups) Detonate; two at once with Double Trouble. */
        private void specials(Set<Invocation> inv) {
            if (!alive(akkha) || finalStand) {
                return;
            }
            specialsUsed++;
            List<Integer> pool = new ArrayList<>(List.of(0, 1));
            if (raid.partySize() > 1) {
                pool.add(2);
            }
            java.util.Collections.shuffle(pool, rnd());
            for (int sp : pool.subList(0, Math.min(pool.size(), Invocation.akkhaSpecialsAtOnce(inv)))) {
                switch (sp) {
                    case 0 -> memoryBlast(inv);
                    case 1 -> startTrailingOrbs();
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
            // At least the strike floor between blasts (36 MC). // playability: OSRS gives four ticks, two with Feeling Special?
            int gap = com.runecraft.osrs.content.raids.RaidRealtime.memoryBlastGap(Invocation.akkhaMemoryBlastTicks(inv));
            lastMemoryBlastGap = gap;
            memoryOrder.clear();
            memoryOrder.addAll(order);
            for (int k = 0; k < order.size(); k++) {
                int shown = order.get(k);
                later(1 + 2 * k, () -> {
                    Vec3 c = quadrantCentre(shown);
                    world().sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y + 1, c.z, 60, 3, 0.5, 3, 0.01);
                    world().playSound(null, BlockPos.containing(c), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.HOSTILE, 1f,
                            0.7f + shown * 0.15f);
                    // Round 43: the quadrant's symbol and the floor round it glow yellow while it is shown (the order to
                    // remember). // playability: OSRS lights the symbol's tiles
                    BlockPos m = BlockPos.containing(c);
                    RaidFloor.rect(world(), m.getX() - 2, m.getZ() - 2, m.getX() + 2, m.getZ() + 2, c.y, RaidFloor.SAFE, 2, akkha);
                    memoryGlows++;
                });
            }
            int start = 3 + 2 * order.size();
            later(start - 2, () -> RaidEffects.telegraph(players(), "Memory Blast: " + order.size() + " blasts, "
                    + String.format(java.util.Locale.ROOT, "%.1f", gap * 0.6) + " s apart - stand in each quadrant in the order it glowed!"));
            for (int k = 0; k < order.size(); k++) {
                int safe = order.get(k);
                long due = now() + start + (long) k * gap;
                // a rising bell in each blast's last moments. // playability
                RaidEffects.cue(start + k * gap, center().getCenter(), () -> {
                    long dueMc = due * com.runecraft.osrs.ArenaTiming.MC_PER_GAME_TICK;
                    if (com.runecraft.osrs.ArenaTiming.finalCue(dueMc, RaidEffects.clockMc())) {
                        com.runecraft.boss.StrikeMarkers.bell(world(), vec(0, 0), dueMc - RaidEffects.clockMc());
                    }
                    return alive(akkha);
                });
                later(start + k * gap, () -> {
                    if (!alive(akkha)) {
                        return;
                    }
                    memoryBlasts++;
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

        /** Memory Blast self-test notes: quadrant glows shown, blasts landed, the last gap and order. */
        int memoryGlows;
        int memoryBlasts;
        int lastMemoryBlastGap = -1;
        final List<Integer> memoryOrder = new ArrayList<>();

        /** Distance each player walked of their own accord since the last game tick, and where they were a Minecraft tick ago. */
        private final Map<UUID, Double> walked = new HashMap<>();
        private final Map<UUID, Vec3> lastMc = new HashMap<>();

        /** Trailing Orbs begin (OSRS: for ten ticks every tile a player walks leaves an orb behind them). */
        private void startTrailingOrbs() {
            warn("Magical orbs begin to materialise around you! Stand still - every step leaves an orb.");
            trailTicks = 10;
            trailLast.clear();
            walked.clear();
            lastMc.clear();
        }

        /**
         * Every Minecraft tick while the orbs materialise: adds up each player's own steps ({@link
         * com.runecraft.osrs.content.raids.RaidRealtime#ownStep}), on the ground or in the air (a jump is a step), less
         * the part pointing away from a body they overlap (Akkha or a shadow shoving them; walking round him still counts);
         * not sliding on ice, neither standing still nor teleported. No hit knocks a player back here (OSRS hits never push:
         * OsrsKnockback), so there is no knockback to leave out.
         */
        @Override
        public void bossTick(DataBossEntity boss) {
            if (boss != akkha) {
                return;
            }
            if (trailTicks <= 0) {
                lastMc.clear();
                return;
            }
            for (ServerPlayer p : players()) {
                addOwnStep(p);
            }
        }

        /** One Minecraft tick of {@link #bossTick} for one player. */
        private void addOwnStep(ServerPlayer p) {
            Vec3 now = p.position();
            Vec3 before = lastMc.put(p.getUUID(), now);
            if (before == null) {
                return;
            }
            // The bodies whose boxes overlap the player's (Minecraft only pushes overlapping boxes, away from the centre).
            List<net.minecraft.world.entity.Entity> bodies = world().getEntities(p, p.getBoundingBox(),
                    e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator());
            double[] away = new double[bodies.size() * 2];
            for (int i = 0; i < bodies.size(); i++) {
                away[2 * i] = now.x - bodies.get(i).getX();
                away[2 * i + 1] = now.z - bodies.get(i).getZ();
            }
            BlockPos below = BlockPos.containing(now.x, now.y - 0.5, now.z);
            boolean slippery = world().getBlockState(below).getBlock().getFriction() > 0.6f;
            double own = com.runecraft.osrs.content.raids.RaidRealtime.ownStep(now.x - before.x, now.z - before.z, away, slippery);
            if (own > 0) {
                walked.merge(p.getUUID(), own, Double::sum);
            }
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
                double walked = this.walked.getOrDefault(p.getUUID(), 0.0);
                this.walked.put(p.getUUID(), 0.0);
                // Only the player's own steps since the last game tick count, walked or jumped (Akkha's body shoving them,
                // ice sliding and teleports don't): see bossTick. // playability: OSRS drops an orb on each tile a player
                // walks off
                if (last != null && walked >= com.runecraft.osrs.content.raids.RaidRealtime.ORB_STEP) {
                    // CA "Dropped the ball": moving while the orbs materialise drops one behind you.
                    com.runecraft.content.systems.CombatAchievementService.flag(p, ToaLoot.FLAG_AKKHA_ORB_DROPPED);
                    orbsDropped++;
                    trail.add(new double[]{last.x, last.z, now() + 12});
                    if (Invocation.akkhaTrailingOrbs(inv) > 1 && now.subtract(last).horizontalDistanceSqr() > 1e-4) {
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

        /**
         * Self-test: the player steps up beside Akkha, two blocks from him towards the room's centre (his quadrant, inside his
         * melee reach). OSRS: Akkha walks up to whoever he fights and swings his halberd from the next tile, and since round 41
         * a swing only lands on a target still in reach; his attack goal would walk him there, but entities don't tick while a
         * check runs ({@link #selfTestAutoAttack} drives his attacks, not his steps).
         */
        private void selfTestStandBeside(RaidSelfTest t) {
            Vec3 a = akkha.position();
            t.moveTo(BlockPos.containing(a.x + (a.x < center().getX() + 0.5 ? 2 : -2), a.y, a.z));
        }

        /**
         * Self-test: Trailing Orbs follow the player's own steps, walked on the ground, jumped through the air and walked round
         * Akkha pressed against him (only the part of a step pointing away from a body is its shove). Entities don't tick
         * while a check runs, so each Minecraft tick's step is made here (0.2 blocks, walking pace) and Akkha's boss tick,
         * which adds the steps up, is called by hand; the raid game tick then drops the orbs. The special ends afterwards.
         */
        private String selfTestTrailingOrbs(RaidSelfTest t) {
            ServerPlayer p = t.player();
            int mcPerTick = com.runecraft.action.ActionManager.MC_TICKS_PER_GAME_TICK;
            int before = orbsDropped;
            startTrailingOrbs();
            Vec3 at = vec(-8, -6);
            selfTestStep(p, at, true);
            bossTick(akkha);
            t.tick(1);
            for (int leg = 0; leg < 2; leg++) {
                boolean ground = leg == 0;
                int legBefore = orbsDropped;
                for (int g = 0; g < 2; g++) {
                    for (int mc = 0; mc < mcPerTick; mc++) {
                        at = at.add(0.2, 0, 0);
                        // in the air: the top of a jump arc, 0.6 above the floor, not on the ground
                        selfTestStep(p, ground ? at : at.add(0, 0.6, 0), ground);
                        bossTick(akkha);
                    }
                    t.tick(1);
                }
                RaidSelfTest.expect(orbsDropped >= legBefore + 2, "Akkha: two ticks " + (ground ? "walking" : "jumping") + " during Trailing Orbs dropped "
                        + (orbsDropped - legBefore) + " orbs (one a tick expected)");
            }
            // Round Akkha, inside his box the whole way (a melee player pressed against him).
            Vec3 a = akkha.position();
            double r = Math.min(0.6, (akkha.getBbWidth() + p.getBbWidth()) / 2 - 0.1);
            double angle = 0;
            selfTestStep(p, new Vec3(a.x + r, a.y, a.z), true);
            bossTick(akkha); // the jump over to him is a teleport: not counted
            int roundBefore = orbsDropped;
            for (int mc = 0; mc < mcPerTick; mc++) {
                angle += 0.2 / r;
                selfTestStep(p, new Vec3(a.x + r * Math.cos(angle), a.y, a.z + r * Math.sin(angle)), true);
                RaidSelfTest.expect(p.getBoundingBox().intersects(akkha.getBoundingBox()), "Akkha: the self-test player left his box while walking round him");
                bossTick(akkha);
            }
            t.tick(1);
            RaidSelfTest.expect(orbsDropped > roundBefore, "Akkha: walking round him pressed against him dropped no trailing orb");
            trailTicks = 0;
            trail.clear();
            trailLast.clear();
            walked.clear();
            lastMc.clear();
            return (orbsDropped - before) + " trailing orbs dropped walking, jumping and pressed against Akkha";
        }

        /** Self-test: puts the player at {@code at}, on the ground or not, as one Minecraft tick's movement. */
        private void selfTestStep(ServerPlayer p, Vec3 at, boolean onGround) {
            p.teleportTo(world(), at.x, at.y, at.z, p.getYRot(), p.getXRot());
            p.setOnGround(onGround);
        }

        /**
         * Self-test (round 43): Memory Blast glows each quadrant's floor in turn, and a player who stands in each glowing quadrant
         * in that order, stepping over after each blast, is never hit; the blasts are at least the strike floor apart. A full
         * shadow marks its quadrant's floor before it slams, and a player who steps out in time is spared. The ice quadrant's
         * element slows attacks (not movement) until it thaws.
         */
        private String selfTestMemoryAndSlam(RaidSelfTest t) {
            ServerPlayer p = t.player();
            Set<Invocation> inv = raid.invocations();
            int hitsBefore = raid.attacksOnParty();
            memoryBlast(inv);
            int rounds = memoryOrder.size();
            RaidSelfTest.expect(rounds > 0 && lastMemoryBlastGap >= com.runecraft.osrs.ArenaTiming.STRIKE_FLOOR_TICKS,
                    "Akkha: Memory Blast had " + rounds + " rounds " + lastMemoryBlastGap + " ticks apart");
            t.tick(1);
            BlockPos glow = BlockPos.containing(quadrantCentre(memoryOrder.get(0))).below();
            RaidSelfTest.expect(com.runecraft.boss.FloorMarkers.marked(world(), glow), "Akkha: the first glowing quadrant's floor was not marked");
            // stand in the first safe quadrant, then step on to each next one as the blast before it lands
            t.moveTo(BlockPos.containing(quadrantCentre(memoryOrder.get(0))));
            // the first blast lands 3 + 2 x rounds ticks after the special; one tick has passed, stop a tick before it
            t.tick(1 + 2 * rounds);
            for (int k = 0; k < rounds; k++) {
                t.tick(1); // blast k lands with the player in its safe quadrant
                RaidSelfTest.expect(memoryBlasts == k + 1, "Akkha: Memory Blast " + (k + 1) + " did not land (" + memoryBlasts + ")");
                if (k + 1 < rounds) {
                    t.moveTo(BlockPos.containing(quadrantCentre(memoryOrder.get(k + 1))));
                    t.tick(lastMemoryBlastGap - 1);
                }
            }
            RaidSelfTest.expect(raid.attacksOnParty() == hitsBefore, "Akkha: the Memory Blast hit a player standing in each safe quadrant");
            RaidSelfTest.expect(memoryGlows == rounds, "Akkha: " + memoryGlows + " quadrant glows for " + rounds + " blasts");
            t.tick(lastMemoryBlastGap);
            RaidSelfTest.expect(!hidden, "Akkha did not come back after his Memory Blast");
            // A shadow's slam: its quadrant is marked, and stepping out in time spares the player.
            spawnShadows(true);
            int q = quadrant(p.position());
            shadowSlam(q);
            BlockPos inside = BlockPos.containing(quadrantCentre(q)).offset(3, -1, 3);
            RaidSelfTest.expect(com.runecraft.boss.FloorMarkers.marked(world(), inside), "Akkha: a full shadow's quadrant was not marked");
            int other = (q + 1) % 4;
            t.moveTo(BlockPos.containing(quadrantCentre(other)));
            int slamsBefore = shadowSlams;
            hitsBefore = raid.attacksOnParty();
            t.tick(com.runecraft.osrs.content.raids.RaidRealtime.SHADOW_SLAM_LEAD_TICKS + 1);
            RaidSelfTest.expect(shadowSlams == slamsBefore + 1 && lastShadowSlamLead >= com.runecraft.osrs.ArenaTiming.STRIKE_FLOOR_TICKS,
                    "Akkha: the shadow's slam landed " + lastShadowSlamLead + " ticks after its warning");
            RaidSelfTest.expect(raid.attacksOnParty() == hitsBefore, "Akkha: the shadow's slam hit a player who had left its quadrant");
            RaidSelfTest.expect(!com.runecraft.boss.FloorMarkers.marked(world(), inside), "Akkha: the slammed quadrant's floor stayed marked");
            for (int k = 0; k < 4; k++) {
                if (alive(shadows[k])) {
                    shadows[k].discard();
                }
                shadows[k] = null;
                shadowCharge[k] = 0;
            }
            String ice = selfTestIce(t);
            return rounds + " memory blasts dodged " + lastMemoryBlastGap + " ticks apart, a shadow slam dodged after " + lastShadowSlamLead
                    + " ticks, " + ice;
        }

        /**
         * Self-test: the ice quadrant's element (OSRS wiki, ToA strategies: "Ice-based attacks will temporarily slow players'
         * attack speed by 1 tick", until "The ice around you melts away.") makes each attack's weapon delay one game tick
         * longer, through the attack clock and the after-hit hook a real swing goes through, once per attack however many hits
         * it has; it adds no movement slow, and attacks are back to their speed once it thaws.
         */
        private String selfTestIce(RaidSelfTest t) {
            ServerPlayer p = t.player();
            int period = com.runecraft.osrs.AttackClock.periodMc(4); // a 4-tick weapon
            int tick = com.runecraft.action.ActionManager.MC_TICKS_PER_GAME_TICK;
            // Kephri's Dung Strike stuns its target for 2 seconds (OSRS; Slowness at freeze strength here). When her path came
            // first, her last stun is still on the player now: this walk runs inside one server tick and a player's effects
            // only count down with the server's ticks, so it never wore off, as a real player's did long ago. Only a stun is
            // worn off; any other slow stays and fails the check below.
            if (com.runecraft.combat.FreezeRules.isFreeze(p.getEffect(MobEffects.MOVEMENT_SLOWDOWN))) {
                p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            }
            RaidSelfTest.expect(!ICED.containsKey(p.getUUID()), "Akkha: the player was encased in ice before the ice check");
            long before = selfTestSwing(p, period);
            RaidSelfTest.expect(before == period, "Akkha: an attack before the ice is ready in " + before + " MC, not its " + period);
            element(p, 3);
            RaidSelfTest.expect(ICED.containsKey(p.getUUID()), "Akkha: the ice quadrant's element did not encase the player");
            RaidSelfTest.expect(!p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && !p.hasEffect(MobEffects.DIG_SLOWDOWN),
                    "Akkha: the ice quadrant's element slowed movement or mining (OSRS: only attacks)");
            long iced = selfTestSwing(p, period);
            RaidSelfTest.expect(iced == period + tick, "Akkha: an attack in the ice is ready in " + iced + " MC, not " + (period + tick)
                    + " (its " + period + " and one game tick)");
            t.tick(ICE_TICKS + 1);
            RaidSelfTest.expect(!ICED.containsKey(p.getUUID()), "Akkha: the ice never thawed");
            long thawed = selfTestSwing(p, period);
            RaidSelfTest.expect(thawed == period, "Akkha: an attack after the thaw is ready in " + thawed + " MC, not its " + period);
            return "ice: a " + period + " MC attack took " + iced + " MC, " + thawed + " after the thaw, no movement slow";
        }

        /**
         * Self-test: one two-hit attack at Akkha resolved the way a ready swing is (stamped on the attack clock, then each hit's
         * after-hit hooks; misses here), and the MC until the next attack is ready. The swing runs on a fresh attack clock and
         * the player's own is put back whole afterwards (lastAttackTick, its period, a buffered click; the client is told), so
         * the rest of the walk never sees this attack.
         */
        private long selfTestSwing(ServerPlayer p, int periodMc) {
            com.runecraft.combat.AttackTimer.Saved clock = com.runecraft.combat.AttackTimer.saveForSelfTest(p);
            try {
                com.runecraft.combat.AttackTimer.accept(p, com.runecraft.osrs.AttackClock.Kind.MELEE, periodMc);
                for (int hit = 0; hit < 2; hit++) {
                    com.runecraft.combat.CombatHandler.hookAfterHit(p, akkha, AttackType.SLASH, 0);
                }
                return com.runecraft.combat.AttackTimer.remaining(p);
            } finally {
                clock.restore();
            }
        }

        @Override
        String selfTestSolve(RaidSelfTest t) {
            int shadowsKilled = 0;
            int[] kills = {0};
            String memory = selfTestMemoryAndSlam(t);
            String orbs = selfTestTrailingOrbs(t);
            selfTestStandBeside(t);
            String r = grind(t, akkha, AttackType.MAGIC, 30, () -> {
                if (finalStand || hidden || !shadowsUp() || !alive(akkha)) {
                    return;
                }
                int q = quadrant(akkha.position());
                if (alive(shadows[q])) {
                    // Kill the nearest shadow and lure Akkha into its quadrant (moved there, as the player would draw him: the
                    // player walks into that quadrant and he follows to stand beside them).
                    for (int k = 0; k < 4; k++) {
                        if (alive(shadows[k])) {
                            t.hit(shadows[k], 200, AttackType.MAGIC);
                            if (!alive(shadows[k])) {
                                kills[0]++;
                                move(akkha, quadrantCentre(k));
                                selfTestStandBeside(t);
                            }
                            break;
                        }
                    }
                }
            });
            RaidSelfTest.expect(thresholds == 4, "Akkha: the shadows did not appear at every threshold (" + thresholds + ")");
            // OSRS (June 2025): every seventh attack (sixth with Stay Vigilant) is a special instead of an auto-attack.
            int every = Invocation.akkhaSpecialEvery(raid.invocations());
            RaidSelfTest.expect(specialsUsed <= attacks / every && (attacks < 2 * every || specialsUsed >= 1),
                    "Akkha: " + specialsUsed + " specials in " + attacks + " attacks (one every " + every + ")");
            RaidSelfTest.expect(finalStand, "Akkha: he died without his final stand");
            return r + ", " + (kills[0] + shadowsKilled) + " shadows dispelled, " + orbs + ", " + memory;
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

        /**
         * Rockfall: a rock on each player's tile, and two debris piles that stay until destroyed. Round 43: every falling rock
         * is a raid strike marked on the floor from its shadow (the debris too: it was a puff of particles and a tile-exact hit),
         * and a debris pile never lands on anyone: whoever stands there is hit and shoved off first. // playability: OSRS shows
         * the rocks' shadows on the tiles
         */
        private void rockfall() {
            DataBossEntity source = baba;
            java.util.function.BooleanSupplier attempt = current();
            for (ServerPlayer p : players()) {
                RaidEffects.mark(world(), p.position(), 0.9).delay(3).source(source).warn(ParticleTypes.CLOUD).damage(0, 30) // approx: heavy
                        .floor(RaidFloor.DANGER).start();
            }
            for (int i = 0; i < 2; i++) {
                BlockPos d = at(rnd().nextInt(24) - 14, rnd().nextInt(28) - 14);
                if (debris.containsKey(d) || sarcophagi.containsKey(d)) {
                    continue;
                }
                Vec3 c = Vec3.atBottomCenterOf(d);
                RaidEffects.mark(world(), c, 0.9).delay(3).source(source).warn(ParticleTypes.CLOUD).floor(RaidFloor.DANGER)
                        .live(() -> attempt.getAsBoolean() && alive(source))
                        .onHit(p -> hurt(source, p, 20 + rnd().nextInt(10)))
                        .onLand(() -> {
                            for (ServerPlayer p : players()) {
                                if (p.getBoundingBox().intersects(new net.minecraft.world.phys.AABB(d))) {
                                    Vec3 off = p.position().subtract(c).multiply(1, 0, 1);
                                    shove(p, off.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : off, 2);
                                }
                            }
                            RaidBuilder.set(world(), d, Blocks.COBBLESTONE.defaultBlockState());
                            debris.put(d, ToaBossRules.debrisHp(raid.partySize()));
                        }).start();
            }
        }

        /**
         * Whether a player stands next to a block (cover against a thrown boulder): within reach of its sides or corners,
         * measured from the player's centre. // playability: OSRS counts the eight tiles round it; a player standing between
         * blocks straddles two
         */
        static boolean besideBlock(Vec3 p, BlockPos b) {
            return Math.abs(p.x - (b.getX() + 0.5)) <= COVER_REACH && Math.abs(p.z - (b.getZ() + 0.5)) <= COVER_REACH;
        }

        /** How far from a cover block's centre (each axis) still counts as next to it: its tile and the ring round it, plus a little. */
        static final double COVER_REACH = 1.9;

        /**
         * Rock Throw (OSRS): a boulder at every player, landing about 6 ticks later; it is nulled by standing next to
         * debris (destroyed), an unopened sarcophagus (30 damage to it) or a baboon (12 damage to it).
         */
        private void rockThrow() {
            warn("Ba-Ba hurls boulders at everyone! Take cover next to debris, a sarcophagus or a baboon.");
            // Round 43: each boulder's shadow follows its player until it lands, and the tiles round every debris pile and
            // unopened sarcophagus (the cover) are marked on the floor. // playability: OSRS shows the boulder overhead
            List<Vec3> covers = new ArrayList<>();
            for (BlockPos d : debris.keySet()) {
                covers.add(Vec3.atBottomCenterOf(d));
            }
            for (Map.Entry<BlockPos, Integer> s : sarcophagi.entrySet()) {
                if (s.getValue() > 0) {
                    covers.add(Vec3.atBottomCenterOf(s.getKey()));
                }
            }
            for (Vec3 c : covers) {
                BlockPos b = BlockPos.containing(c);
                List<int[]> ring = new ArrayList<>();
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx != 0 || dz != 0) {
                            ring.add(new int[]{b.getX() + dx, b.getZ() + dz});
                        }
                    }
                }
                RaidFloor.columns(world(), ring, c.y, RaidFloor.SAFE, 6, baba, c);
            }
            DataBossEntity source = baba;
            for (ServerPlayer p : players()) {
                world().sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 2.5, p.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
                RaidEffects.cue(6, center().getCenter(), () -> {
                    if (!alive(source) || !p.isAlive()) {
                        return false;
                    }
                    com.runecraft.boss.StrikeMarkers.ring(world(), p.position(), 0.6, ParticleTypes.LARGE_SMOKE, false, 10);
                    return true;
                });
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
                        if (besideBlock(p.position(), d)) {
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
                        if (s.getValue() > 0 && besideBlock(p.position(), b)) {
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
                    int dmg = (raid.partySize() > 1 ? 35 : 45) + rnd().nextInt(16); // approx: massive, less in groups
                    ServerPlayer buddy = null;
                    for (ServerPlayer o : players()) {
                        if (o != p && besideBlock(o.position(), stand)) {
                            buddy = o;
                            break;
                        }
                    }
                    if (buddy != null) {
                        // OSRS (Ba-Ba page): standing next to another player also shields you; the boulder's damage is
                        // shared with them. // approx: halved for each
                        Msg.red(p, "You and " + buddy.getName().getString() + " share the boulder's impact.");
                        hurt(baba, p, dmg / 2);
                        hurt(baba, buddy, dmg / 2);
                        continue;
                    }
                    Msg.red(p, "The boulder crushes you!");
                    hurt(baba, p, dmg);
                }
                if (debrisLost) {
                    // OSRS: if debris was destroyed, Ba-Ba follows up with another Rockfall.
                    later(2, this::rockfall);
                }
            });
        }

        /**
         * Shockwave (OSRS): Ba-Ba faces her nearest target and shadows spread over the ground there - a 3x3 plus one tile out
         * on each side (5x5 plus one out with Shaking Things Up); a few ticks later the swipe hits every tile marked.
         */
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
            if (target == null) {
                return;
            }
            boolean shaking = inv.contains(Invocation.SHAKING_THINGS_UP);
            BlockPos centre = target.blockPosition();
            // Round 43: the shadow's tiles are marked on the floor and redrawn until the slam (what is marked is what it hits).
            // // playability: OSRS spreads a shadow over the tiles
            List<int[]> tiles = new ArrayList<>();
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    if (ToaBossRules.babaShockwaveHits(dx, dz, shaking)) {
                        tiles.add(new int[]{centre.getX() + dx, centre.getZ() + dz});
                    }
                }
            }
            RaidFloor.columns(world(), tiles, centre.getY(), RaidFloor.DANGER, 3, baba, Vec3.atBottomCenterOf(centre));
            DataBossEntity source = baba;
            RaidEffects.cue(3, center().getCenter(), () -> {
                if (!alive(source) || rolling) {
                    return false;
                }
                for (int[] c : tiles) {
                    world().sendParticles(ParticleTypes.CLOUD, c[0] + 0.5, centre.getY() + 0.1, c[1] + 0.5, 1, 0.3, 0, 0.3, 0);
                }
                return true;
            });
            shockwaves++;
            later(3, () -> {
                if (!alive(baba) || rolling) {
                    return;
                }
                for (ServerPlayer p : players()) {
                    if (ToaBossRules.babaShockwaveHits(p.getBlockX() - centre.getX(), p.getBlockZ() - centre.getZ(), shaking)) {
                        Msg.red(p, "Ba-Ba's shockwave knocks the wind out of you!");
                        hurt(baba, p, rnd().nextInt(21)); // approx: up to 20, "dependent on the player's position"
                    }
                }
            });
        }

        /**
         * Rolling Stones (OSRS): Ba-Ba leaps to her throne and screams, knocking everyone to the far (west) end; with Mind
         * the Gap! anyone not at the north or south edge falls into the pit. Then ten sets of five boulders roll west. The
         * scream comes {@link #SCREAM_WIND_UP_TICKS} ticks after the leap, the safe edges drawn meanwhile (round 41).
         */
        private void rollingStones() {
            rolling = true;
            setsLeft = ToaBossRules.BOULDER_SETS;
            lastCracked = -1;
            clearDebris();
            unanchor(baba);
            move(baba, vec(17, 0));
            anchor(baba);
            boolean gap = Invocation.babaMindTheGap(raid.invocations());
            warn(gap ? "Ba-Ba leaps to her throne - get to the north or south edge before she screams!"
                    : "Ba-Ba leaps to her throne and draws breath to scream!");
            nextSet = now() + SCREAM_WIND_UP_TICKS + 3;
            if (gap) {
                RaidEffects.cue(SCREAM_WIND_UP_TICKS, () -> {
                    if (!alive(baba) || !rolling) {
                        return false;
                    }
                    drawGapSafeEdges();
                    return true;
                });
            }
            later(SCREAM_WIND_UP_TICKS, this::scream);
        }

        /**
         * Game ticks from her leap to the throne to the scream that knocks everyone down the slope (into the pit, with Mind
         * the Gap!, for anyone not at the north or south edge or the top row). // playability: OSRS screams as she lands;
         * here the safe edges are drawn while she draws breath
         */
        static final int SCREAM_WIND_UP_TICKS = com.runecraft.osrs.ArenaTiming.STRIKE_FLOOR_TICKS;

        private static final net.minecraft.core.particles.DustParticleOptions SAFE_EDGE =
                new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.45f, 1.0f, 0.45f), 1.5f);

        /** Mind the Gap!: the north and south edges and the top row by her throne, where the scream can't reach the pit. */
        private void drawGapSafeEdges() {
            double y = center().getY() + 1.1;
            double cx = center().getX() + 0.5;
            double cz = center().getZ() + 0.5;
            for (int s : new int[]{-1, 1}) {
                for (int x = -RaidBuilder.HALF + 1; x < RaidBuilder.HALF; x++) {
                    world().sendParticles(SAFE_EDGE, cx + x, y, cz + s * PIT_HALF_WIDTH, 1, 0.1, 0.0, 0.1, 0.0);
                }
            }
            for (int z = -PIT_HALF_WIDTH; z <= PIT_HALF_WIDTH; z++) {
                world().sendParticles(SAFE_EDGE, cx + TOP_ROW_X, y, cz + z, 1, 0.1, 0.0, 0.1, 0.0);
            }
        }

        /** Her scream: everyone is knocked to the bottom of the slope; with Mind the Gap! anyone off the safe edges falls. */
        private void scream() {
            if (!alive(baba) || !rolling) {
                return;
            }
            world().playSound(null, baba.blockPosition(), net.minecraft.sounds.SoundEvents.RAVAGER_ROAR,
                    net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 1.2f);
            say("Ba-Ba screams!");
            boolean gap = Invocation.babaMindTheGap(raid.invocations());
            for (ServerPlayer p : players()) {
                double dz = p.getZ() - (center().getZ() + 0.5);
                double dx = p.getX() - (center().getX() + 0.5);
                // OSRS: with Mind the Gap! only the north and south sides, or the very top row by her throne, are safe.
                if (gap && Math.abs(dz) < PIT_HALF_WIDTH && dx < TOP_ROW_X) {
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
                // OSRS: team size counts half for boulders, path level +10% / +25%, raid level not at all.
                int hp = ToaBossRules.babaBoulderHp(raid.partySize(), raid.pathLevel());
                for (int i = 0; i < ToaBossRules.BOULDERS_PER_SET; i++) {
                    LivingEntity b = spawnMob("baba_boulder", at(12, -8 + i * 4), false);
                    if (b != null) {
                        raid.setHitpoints(b, hp);
                        if (i == crackedAt) {
                            cracked.add(b);
                            b.setCustomName(net.minecraft.network.chat.Component.literal("Cracked boulder"));
                            // round 43: it glows, so the one to break (ranged always max-hits it) stands out in a rolling row.
                            // // playability: OSRS shows its cracks
                            b.setGlowingTag(true);
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
                        boulderKnockback(p);
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

        /** Self-test: shockwaves made. */
        int shockwaves;

        /** The pit at the bottom (west) of the room: the middle columns, west of this line. */
        static final int PIT_X = -18;
        static final int PIT_HALF_WIDTH = 14;
        /** The very top row, beside her throne, where her scream doesn't reach the pit (OSRS Mind the Gap!). */
        static final int TOP_ROW_X = 15;

        /**
         * A rolling boulder knocks its victim four tiles back down the slope (OSRS); with Mind the Gap! a knock into the pit
         * at the bottom kills.
         */
        void boulderKnockback(ServerPlayer p) {
            double x = p.getX() - 4;
            double dz = p.getZ() - (center().getZ() + 0.5);
            double pit = center().getX() + 0.5 + PIT_X;
            if (x < pit && Math.abs(dz) < PIT_HALF_WIDTH) {
                if (Invocation.babaMindTheGap(raid.invocations())) {
                    Msg.red(p, "The boulder knocks you into the gap!");
                    RaidEffects.hitRaw(baba, p, 999);
                    return;
                }
                x = pit + 1;
            }
            p.teleportTo(Math.max(x, center().getX() + 0.5 - RaidBuilder.HALF + 1), p.getY(), p.getZ());
            p.hurtMarked = true;
        }

        private void endRolling() {
            rolling = false;
            unanchor(baba);
            say("Ba-Ba leaps down from her throne.");
            nextRockfall = now() + 6;
            nextThrow = now() + 16;
        }

        @Override
        DataBossEntity mainBoss() {
            return baba;
        }

        @Override
        String perfectFlag() {
            return ToaLoot.FLAG_PERFECT_BABA;
        }

        /** OSRS "Perfect Ba-Ba": no damage off-prayer, from her slam, the rolling boulders, the rubble or falling rocks. */
        @Override
        String perfectDamageRule() {
            return "nodmg=slam,falling_rocks,mechanic,hazard,offprayer";
        }

        /** ... in a group with every Ba-Ba invocation, and no sarcophagus opened. */
        @Override
        boolean perfectRoom() {
            if (raid.partySize() < 2 || !all(ToaLoot.BABA_INVOCATIONS)) {
                return false;
            }
            for (int hp : sarcophagi.values()) {
                if (hp <= 0) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void onCreatureHit(ServerPlayer player, LivingEntity target, int damage) {
            if (cracked.contains(target)) {
                // CA "No skipping allowed": only the uncracked boulders may be attacked.
                com.runecraft.content.systems.CombatAchievementService.flag(player, ToaLoot.FLAG_BABA_CRACKED_HIT);
            }
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

        /**
         * An opened sarcophagus launches unstable energy in a cone in front of it (OSRS): each blast lands on one tile (a
         * wider area every two path levels), with Gotta Have Faith hurting more the more prayer points you have left.
         */
        private void sarcophagusBlast(BlockPos s, Set<Invocation> inv) {
            int dz = s.getZ() < center().getZ() ? 1 : -1;
            int reach = 2 + rnd().nextInt(6);
            BlockPos at = s.offset(rnd().nextInt(reach + 1) - reach / 2, 0, dz * reach);
            int r = ToaBossRules.babaSarcophagusBlastRadius(raid.pathLevel());
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    world().sendParticles(ParticleTypes.SOUL, at.getX() + x + 0.5, at.getY() + 0.2, at.getZ() + z + 0.5, 4, 0.3, 0.1, 0.3, 0.01);
                }
            }
            later(3, () -> {
                for (ServerPlayer pl : players()) {
                    if (Math.abs(pl.getBlockX() - at.getX()) <= r && Math.abs(pl.getBlockZ() - at.getZ()) <= r) {
                        int dmg = rnd().nextInt(13) + Invocation.babaFaithDamage(inv, RoomLogic.prayerPoints(pl),
                                RoomLogic.level(pl, Skill.PRAYER)); // approx: 0-12
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
            // A rolling boulder knocks you four tiles down the slope; without Mind the Gap! the pit's edge stops you.
            ServerPlayer p = t.player();
            t.moveTo(at(-6, 0));
            double x0 = p.getX();
            boulderKnockback(p);
            RaidSelfTest.expect(Math.abs(p.getX() - (x0 - 4)) < 0.01 && p.isAlive(), "Ba-Ba: a boulder did not knock the player four tiles back");
            t.moveTo(at(-16, 0));
            boulderKnockback(p);
            RaidSelfTest.expect(p.isAlive() && p.getX() >= center().getX() + 0.5 + PIT_X, "Ba-Ba: a boulder knocked the player into the pit "
                    + "without Mind the Gap!");
            // Round 43: a shockwave's tiles are marked on the floor until it lands, then put back.
            t.moveTo(at(4, 0));
            int waves = shockwaves;
            shockwave(raid.invocations());
            BlockPos under = p.blockPosition().below();
            RaidSelfTest.expect(shockwaves == waves + 1 && com.runecraft.boss.FloorMarkers.marked(world(), under),
                    "Ba-Ba: the shockwave's tiles were not marked on the floor");
            t.tick(4);
            RaidSelfTest.expect(!com.runecraft.boss.FloorMarkers.marked(world(), under), "Ba-Ba: the shockwave's floor marks stayed after it landed");
            // cover against a thrown boulder counts from the player's centre, a little past the ring of tiles round it
            BlockPos block = at(-2, 4);
            RaidSelfTest.expect(besideBlock(Vec3.atBottomCenterOf(block).add(1.8, 0, -1.8), block)
                    && !besideBlock(Vec3.atBottomCenterOf(block).add(2.2, 0, 0), block), "Ba-Ba: the cover reach is off");
            t.moveTo(at(0, 0));
            int expectHp = ToaBossRules.babaBoulderHp(raid.partySize(), raid.pathLevel());
            String r = grind(t, baba, AttackType.CRUSH, 30, () -> {
                for (LivingEntity b : living("baba_boulder")) {
                    RaidSelfTest.expect(raid.maxHitpoints(b) == expectHp, "Ba-Ba: a boulder had " + raid.maxHitpoints(b)
                            + " hitpoints, the wiki's table says " + expectHp);
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
        /** Jugs pushed while a Great Roar was coming (CA "Perfect Zebak": at most two). */
        int jugsPushedOnRoar;
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
            jugsPushedOnRoar = 0;
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
                    // // playability: OSRS lands the bite on the attack tick; here after the jaws' wind-up, on a player still in
                    // reach, the prayer read as it lands
                    zebak.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    Mechanics.deliverMelee(zebak, p, com.runecraft.osrs.ThreatTiming.MELEE_REACH, () -> {
                        if (!alive(zebak)) {
                            return;
                        }
                        int roll = rnd().nextInt(ToaBossRules.ZEBAK_MELEE_MAX + 1);
                        if (RaidEffects.protectedFrom(p, AttackType.STAB)) {
                            roll /= 2;
                        }
                        Msg.red(p, "Zebak's jaws snap at you!");
                        RaidEffects.hit(zebak, p, roll, null, true);
                    });
                    return;
                }
            }
            ServerPlayer target = here.get(rnd().nextInt(here.size()));
            AttackType style = rnd().nextBoolean() ? AttackType.RANGED : AttackType.MAGIC;
            // OSRS: the rock (ranged) or jug (magic) only hurts on impact, so it can be prayed against late; drawn here in the
            // style's colour all the way to its target. // playability
            RaidEffects.projectile(world(), zebak.position().add(0, 2, 0), target, style, 2, () -> alive(zebak),
                    () -> RaidEffects.hit(zebak, target, rnd().nextInt(ToaBossRules.ZEBAK_RANGED_MAX + 1), style, false));
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
                // The shelters shown in green from the warning until the last of the roar's three hits. // playability
                RaidEffects.telegraph(players(), "Great Roar coming - shelter in the green behind a rock (west of it)!");
                RaidEffects.cue(roarIn + roarHits + 1, () -> {
                    if (!alive(zebak) || rocks.isEmpty()) {
                        return false;
                    }
                    drawShelters();
                    return true;
                });
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

        /**
         * The rock a player shelters behind: west of it (away from Zebak), up to three tiles, in its row (OSRS), here in a
         * cone that widens behind the rock ({@link com.runecraft.osrs.content.raids.RaidRealtime#inShelterCone}).
         * // playability: OSRS shelters exactly the three tiles behind it
         */
        BlockPos coverFor(Vec3 pos) {
            for (BlockPos r : rocks.keySet()) {
                double behind = r.getX() + 0.5 - pos.x;
                double lateral = pos.z - (r.getZ() + 0.5);
                if (com.runecraft.osrs.content.raids.RaidRealtime.inShelterCone(behind, lateral)) {
                    return r;
                }
            }
            return null;
        }

        private BlockPos coverFor(ServerPlayer p) {
            return coverFor(p.position());
        }

        /** Each rock's shelter in green on the floor (the cone {@link #coverFor} accepts), until the roar is over. */
        private void drawShelters() {
            double y = center().getY() + 1.1;
            double half = com.runecraft.osrs.content.raids.RaidRealtime.SHELTER_HALF_WIDTH;
            double spread = com.runecraft.osrs.content.raids.RaidRealtime.SHELTER_SPREAD;
            for (BlockPos r : rocks.keySet()) {
                for (double behind = 0.75; behind <= com.runecraft.osrs.content.raids.RaidRealtime.SHELTER_DEPTH; behind += 0.5) {
                    double w = half + spread * (behind - 0.5) - 0.05;
                    double x = r.getX() + 0.5 - behind;
                    world().sendParticles(RaidEffects.SAFE, x, y, r.getZ() + 0.5 - w, 1, 0, 0, 0, 0);
                    world().sendParticles(RaidEffects.SAFE, x, y, r.getZ() + 0.5 + w, 1, 0, 0, 0, 0);
                    if (behind == 0.75 || behind >= com.runecraft.osrs.content.raids.RaidRealtime.SHELTER_DEPTH - 0.25) {
                        world().sendParticles(RaidEffects.SAFE, x, y, r.getZ() + 0.5, 2, 0, 0, w * 0.5, 0);
                    }
                }
            }
        }

        /**
         * Tidal Waves (OSRS): the first from the south, the next from the north; a wave sweeps the whole room except its
         * gap, pushing and hurting anyone caught.
         */
        private void tidalWaves() {
            boolean fromSouth = specials % 4 == 1;
            warn("Zebak slams his tail - a tidal wave rises in the " + (fromSouth ? "south" : "north") + "!");
            int gap = ToaBossRules.zebakWaveGap(raid.pathLevel());
            int previous = 0;
            for (int w = 0; w < 3; w++) {
                int gapX = -14 + rnd().nextInt(28 - gap);
                int delay = 3 + w * 4;
                // Each wave's gap is marked from the warning (the first) or the wave before it crashing, until it crashes.
                // // playability: OSRS shows the wave rising; the gap is read off the water a tick or two before it hits
                int lead = delay - previous;
                Runnable mark = () -> RaidEffects.cue(lead, () -> {
                    if (!alive(zebak)) {
                        return false;
                    }
                    drawWaveGap(fromSouth, gapX, gap);
                    return true;
                });
                if (previous == 0) {
                    mark.run();
                } else {
                    later(previous, mark);
                }
                later(delay, () -> wave(fromSouth, gapX, gap));
                previous = delay;
            }
        }

        private static final net.minecraft.core.particles.DustParticleOptions WAVE_GAP =
                new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.45f, 1.0f, 0.45f), 1.6f);
        private static final net.minecraft.core.particles.DustParticleOptions WAVE_EDGE =
                new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.25f, 0.55f, 1.0f), 1.2f);

        /** The columns a tidal wave leaves dry (its gap) in dense green dust, and the side it comes from in blue. */
        private void drawWaveGap(boolean fromSouth, int gapX, int gap) {
            double y = center().getY() + 1.1;
            double cx = center().getX() + 0.5;
            double cz = center().getZ() + 0.5;
            for (int z = -14; z <= 14; z++) {
                world().sendParticles(WAVE_GAP, cx + gapX - 0.5, y, cz + z, 1, 0.05, 0.0, 0.2, 0.0);
                world().sendParticles(WAVE_GAP, cx + gapX + gap - 0.5, y, cz + z, 1, 0.05, 0.0, 0.2, 0.0);
                if (z % 3 == 0) {
                    world().sendParticles(WAVE_GAP, cx + gapX + gap / 2.0 - 0.5, y, cz + z, 2, gap * 0.25, 0.0, 0.3, 0.0);
                }
            }
            for (int x = -18; x <= 18; x += 2) {
                if (x < gapX || x >= gapX + gap) {
                    world().sendParticles(WAVE_EDGE, cx + x, y + 0.4, cz + (fromSouth ? 12 : -12), 1, 0.4, 0.2, 0.2, 0.0);
                }
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
                // OSRS: 8-11 on everyone, more for players clumped together (the blitz splashes onto them); Arterial Spray
                // widens the splash and heals Zebak more.
                int splash = Invocation.zebakBloodSplashRadius(inv);
                List<ServerPlayer> here = players();
                for (ServerPlayer p : here) {
                    world().sendParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY() + 1, p.getZ(), 6, 0.3, 0.4, 0.3, 0.1);
                    int clumped = 0;
                    for (ServerPlayer o : here) {
                        if (o != p && Math.abs(o.getBlockX() - p.getBlockX()) <= splash && Math.abs(o.getBlockZ() - p.getBlockZ()) <= splash) {
                            clumped++;
                        }
                    }
                    total += RaidEffects.hit(zebak, p, 8 + rnd().nextInt(4) + Invocation.ZEBAK_SPLASH_DAMAGE * clumped, AttackType.MAGIC, false);
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
            if (roarIn > 0) {
                jugsPushedOnRoar++;
            }
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
        DataBossEntity mainBoss() {
            return zebak;
        }

        @Override
        String perfectFlag() {
            return ToaLoot.FLAG_PERFECT_ZEBAK;
        }

        /** OSRS "Perfect Zebak": no damage from poison, his basic attacks off-prayer, blood spawns or waves. */
        @Override
        String perfectDamageRule() {
            return "nodmg=poison,mechanic,hazard,other,offprayer";
        }

        /** ... with every Zebak invocation, and no more than two jugs pushed on the roar. */
        @Override
        boolean perfectRoom() {
            return all(ToaLoot.ZEBAK_INVOCATIONS) && jugsPushedOnRoar <= 2;
        }

        @Override
        String selfTestSolve(RaidSelfTest t) {
            int[] jugsBroken = {0};
            boolean[] shelterChecked = {false};
            String r = grind(t, zebak, AttackType.RANGED, 25, () -> {
                if (!shelterChecked[0] && !rocks.isEmpty()) {
                    // The Great Roar's shelter: the tiles behind a rock in its row (OSRS), widening behind it; not beside it.
                    shelterChecked[0] = true;
                    BlockPos rock = rocks.keySet().iterator().next();
                    Vec3 c = Vec3.atBottomCenterOf(rock);
                    RaidSelfTest.expect(coverFor(c.add(-2, 0, 0)) != null && coverFor(c.add(-3, 0, 0.9)) != null,
                            "Zebak: the tiles behind a rock gave no shelter");
                    RaidSelfTest.expect(coverFor(c.add(2, 0, 0)) == null && coverFor(c.add(-1, 0, 1.2)) == null
                            || rocks.size() > 1, "Zebak: a rock sheltered a player in front of it or beside it");
                }
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
