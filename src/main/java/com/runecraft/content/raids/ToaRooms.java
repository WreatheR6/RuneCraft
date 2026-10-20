package com.runecraft.content.raids;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.Mechanics;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.raids.Invocation;
import com.runecraft.osrs.content.raids.RaidScaling;
import com.runecraft.osrs.content.raids.RaidsContent;
import com.runecraft.osrs.content.raids.RoomDef;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.phys.Vec3;

/** Tombs of Amascut rooms: the four path puzzles, the path bosses and the Wardens. */
final class ToaRooms {
    private ToaRooms() {
    }

    // ================================================================== puzzles

    /** Path of Scabaras: memorise the order the tiles light up, then step on them in that order. */
    static final class Scabaras extends RoomLogic {
        private final List<BlockPos> tiles = new ArrayList<>();
        private final List<Integer> sequence = new ArrayList<>();
        private final Map<UUID, Integer> standing = new HashMap<>();
        private int round;
        private int input;
        private int showIndex = -1;

        Scabaras(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public boolean entersFromWest() {
            return false;
        }

        @Override
        public void decorate() {
            tiles.clear();
            for (int dz = -4; dz <= 4; dz += 4) {
                for (int dx = -4; dx <= 4; dx += 4) {
                    BlockPos p = center().offset(dx, 0, dz);
                    RaidBuilder.set(world(), p, RaidBuilder.module(RaidsContent.SCABARAS_TILE));
                    tiles.add(p);
                }
            }
            RaidBuilder.pillar(world(), center().offset(12, 0, -10), 1, 4, Blocks.CHISELED_SANDSTONE.defaultBlockState());
            RaidBuilder.pillar(world(), center().offset(12, 0, 10), 1, 4, Blocks.CHISELED_SANDSTONE.defaultBlockState());
        }

        @Override
        public void start() {
            round = 0;
            nextRound();
        }

        private void nextRound() {
            round++;
            sequence.clear();
            for (int i = 0; i < 3 + round; i++) {
                sequence.add(rnd().nextInt(tiles.size()));
            }
            replay();
            say("Round " + round + "/3: watch the tiles of Scabaras...");
        }

        private void replay() {
            input = 0;
            showIndex = -2;
        }

        @Override
        public void gameTick(long tick) {
            if (showIndex >= -2 && showIndex < sequence.size() * 2) {
                showIndex++;
                if (showIndex >= 0 && showIndex % 2 == 0 && showIndex / 2 < sequence.size()) {
                    BlockPos t = tiles.get(sequence.get(showIndex / 2));
                    world().sendParticles(ParticleTypes.END_ROD, t.getX() + 0.5, t.getY() + 1.2, t.getZ() + 0.5, 25, 0.3, 0.5, 0.3, 0.02);
                    world().playSound(null, t, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1f,
                            0.6f + sequence.get(showIndex / 2) * 0.1f);
                }
                if (showIndex >= sequence.size() * 2) {
                    showIndex = Integer.MAX_VALUE;
                    say("Now step on the tiles in the same order.");
                }
                return;
            }
            for (ServerPlayer p : players()) {
                int on = tiles.indexOf(p.blockPosition().below());
                Integer before = standing.put(p.getUUID(), on);
                if (on >= 0 && (before == null || before != on)) {
                    press(p, on);
                }
            }
        }

        private void press(ServerPlayer p, int tile) {
            if (done || showIndex != Integer.MAX_VALUE) {
                return;
            }
            BlockPos t = tiles.get(tile);
            if (sequence.get(input) == tile) {
                input++;
                world().sendParticles(ParticleTypes.HAPPY_VILLAGER, t.getX() + 0.5, t.getY() + 1.2, t.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0);
                if (input >= sequence.size()) {
                    if (round >= 3) {
                        say("The tiles of Scabaras glow - the path is open!");
                        complete();
                    } else {
                        nextRound();
                    }
                }
            } else {
                warn("Wrong tile! Scarabs swarm out of the floor!");
                for (ServerPlayer pl : players()) {
                    RaidEffects.hit(null, pl, 5 + rnd().nextInt(8), null, true);
                }
                replay();
            }
        }

        @Override
        public boolean useBlock(ServerPlayer player, String blockId, BlockPos pos) {
            if (!blockId.equals(RaidsContent.SCABARAS_TILE)) {
                return false;
            }
            int i = tiles.indexOf(pos);
            if (i >= 0) {
                press(player, i);
            }
            return true;
        }
    }

    /** Path of Het: mine through Het's seal while beams of sunlight sweep the room. */
    static final class Het extends RoomLogic {
        private int progress;
        private int needed;

        Het(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public boolean entersFromWest() {
            return false;
        }

        @Override
        public void decorate() {
            RaidBuilder.set(world(), at(16, 0), RaidBuilder.module(RaidsContent.HET_SEAL));
            RaidBuilder.set(world(), at(16, 0).above(), Blocks.GOLD_BLOCK.defaultBlockState());
            RaidBuilder.pillar(world(), center().offset(16, 0, -3), 0, 4, Blocks.CHISELED_SANDSTONE.defaultBlockState());
            RaidBuilder.pillar(world(), center().offset(16, 0, 3), 0, 4, Blocks.CHISELED_SANDSTONE.defaultBlockState());
            RaidBuilder.set(world(), at(-19, 0).above(2), Blocks.GLOWSTONE.defaultBlockState());
        }

        @Override
        public void start() {
            progress = 0;
            needed = 20 + 8 * (raid.partySize() - 1);
        }

        @Override
        public void gameTick(long tick) {
            if (tick % 6 == 0) {
                ServerPlayer p = randomPlayer();
                if (p != null) {
                    say("The light of Het sweeps across the room!");
                    for (int x = -18; x <= 14; x += 2) {
                        RaidEffects.strike(world(), new Vec3(center().getX() + 0.5 + x, p.getY(), p.getZ()), 1.2, 18, 3, null,
                                ParticleTypes.END_ROD);
                    }
                }
            }
        }

        @Override
        public boolean useBlock(ServerPlayer player, String blockId, BlockPos pos) {
            if (!blockId.equals(RaidsContent.HET_SEAL)) {
                return false;
            }
            if (cooldown(player, 2)) {
                return true;
            }
            if (!hasTool(player, "_pickaxe")) {
                Msg.game(player, "You need a pickaxe to break the seal.");
                return true;
            }
            player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            progress += 1 + level(player, Skill.MINING) / 30;
            XpService.add(player, Skill.MINING, 20);
            world().sendParticles(ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.4, 0.4, 0.4, 0.1);
            Msg.action(player, "You chip at Het's seal. (" + Math.min(progress, needed) + "/" + needed + ")");
            if (progress >= needed) {
                RaidBuilder.set(world(), pos, Blocks.AIR.defaultBlockState());
                RaidBuilder.set(world(), pos.above(), Blocks.AIR.defaultBlockState());
                say("Het's seal shatters!");
                complete();
            }
            return true;
        }
    }

    /** Path of Apmeken: Apmeken's sight reveals failing pillars, vents and roof supports to repair in time. */
    static final class Apmeken extends RoomLogic {
        private final Map<BlockPos, String> fixtures = new HashMap<>();
        private BlockPos problem;
        private long deadline;
        private int cooldown;
        private int repaired;
        private int needed;

        Apmeken(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public boolean entersFromWest() {
            return false;
        }

        @Override
        public void decorate() {
            for (int x = -8; x <= 8; x += 8) {
                fixture(at(x, -14), RaidsContent.APMEKEN_PILLAR, "a pillar is cracking");
                fixture(at(x, 14), RaidsContent.APMEKEN_VENT, "a vent is spewing gas");
            }
            fixture(at(-14, -5), RaidsContent.APMEKEN_ROOF, "the roof is sagging");
            fixture(at(-14, 5), RaidsContent.APMEKEN_ROOF, "the roof is sagging");
            fixture(at(12, -8), RaidsContent.APMEKEN_ROOF, "the roof is sagging");
        }

        private void fixture(BlockPos pos, String block, String problemText) {
            RaidBuilder.set(world(), pos, RaidBuilder.module(block));
            fixtures.put(pos, problemText);
        }

        @Override
        public void start() {
            repaired = 0;
            needed = 8 + 2 * (raid.partySize() - 1);
            problem = null;
            cooldown = 4;
        }

        @Override
        public void gameTick(long tick) {
            long now = RaidEffects.now();
            if (problem == null) {
                if (--cooldown <= 0) {
                    List<BlockPos> list = new ArrayList<>(fixtures.keySet());
                    problem = list.get(rnd().nextInt(list.size()));
                    deadline = now + 12;
                    warn("Apmeken's sight reveals: " + fixtures.get(problem) + "!");
                }
            } else {
                world().sendParticles(ParticleTypes.LARGE_SMOKE, problem.getX() + 0.5, problem.getY() + 1.2, problem.getZ() + 0.5, 4, 0.3,
                        0.3, 0.3, 0.01);
                if (now > deadline) {
                    warn("You were too slow - the room shakes violently!");
                    for (ServerPlayer p : players()) {
                        RaidEffects.hit(null, p, 10 + rnd().nextInt(11), null, true);
                    }
                    problem = null;
                    cooldown = 5;
                }
            }
            if (tick % 25 == 0 && living("toa_baboon_brawler").size() + living("toa_baboon_thrower").size()
                    + living("toa_baboon_mage").size() < 2 + raid.partySize()) {
                String[] kinds = {"toa_baboon_brawler", "toa_baboon_thrower", "toa_baboon_mage"};
                spawnMob(kinds[rnd().nextInt(3)], at(rnd().nextInt(20) - 10, rnd().nextBoolean() ? -17 : 17), false);
            }
        }

        @Override
        public boolean useBlock(ServerPlayer player, String blockId, BlockPos pos) {
            if (!fixtures.containsKey(pos)) {
                return false;
            }
            if (!pos.equals(problem)) {
                Msg.game(player, "Nothing seems wrong with this right now.");
                return true;
            }
            player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            XpService.add(player, Skill.CONSTRUCTION, 30);
            repaired++;
            problem = null;
            cooldown = 4;
            say(player.getName().getString() + " fixes the problem. (" + repaired + "/" + needed + ")");
            if (repaired >= needed) {
                say("Apmeken's sight fades. The path is clear.");
                complete();
            }
            return true;
        }

        @Override
        public void cleanup() {
            super.cleanup();
            for (String k : new String[]{"toa_baboon_brawler", "toa_baboon_thrower", "toa_baboon_mage"}) {
                living(k).forEach(LivingEntity::discard);
            }
        }
    }

    /** Path of Crondis: carry water to the Palm of Resourcefulness; getting hit can spill it. */
    static final class Crondis extends RoomLogic {
        private int watered;
        private int needed;

        Crondis(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public boolean entersFromWest() {
            return false;
        }

        @Override
        public void decorate() {
            RaidBuilder.set(world(), at(-16, 0), RaidBuilder.module(RaidsContent.CRONDIS_WATER));
            BlockPos palm = at(14, 0);
            RaidBuilder.set(world(), palm, RaidBuilder.module(RaidsContent.CRONDIS_PALM));
            RaidBuilder.fill(world(), palm.getX(), palm.getY() + 1, palm.getZ(), palm.getX(), palm.getY() + 3, palm.getZ(),
                    Blocks.JUNGLE_LOG.defaultBlockState());
            RaidBuilder.fill(world(), palm.getX() - 2, palm.getY() + 4, palm.getZ() - 2, palm.getX() + 2, palm.getY() + 4, palm.getZ() + 2,
                    Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
        }

        @Override
        public void start() {
            watered = 0;
            needed = 10 + 4 * (raid.partySize() - 1);
        }

        @Override
        public void gameTick(long tick) {
            if (tick % 5 == 0) {
                ServerPlayer p = randomPlayer();
                if (p != null) {
                    Vec3 at = p.position();
                    RaidEffects.strike(world(), at, 1.3, 10, 3, null, ParticleTypes.ITEM_SLIME);
                    RaidEffects.later(3, () -> Mechanics.addHazard(world(), at, 4, 10, null));
                }
            }
        }

        @Override
        public void onPlayerHurt(ServerPlayer player, int damage) {
            if (damage > 0 && Inv.has(player, "crondis_water_container", 1) && rnd().nextInt(3) == 0) {
                Inv.remove(player, "crondis_water_container", 1);
                Msg.red(player, "You spill your water!");
            }
        }

        @Override
        public boolean useBlock(ServerPlayer player, String blockId, BlockPos pos) {
            if (blockId.equals(RaidsContent.CRONDIS_WATER)) {
                if (Inv.has(player, "crondis_water_container", 1)) {
                    Msg.game(player, "You are already carrying water.");
                } else {
                    Inv.give(player, "crondis_water_container", 1);
                    Msg.game(player, "You fill a container with water.");
                }
                return true;
            }
            if (blockId.equals(RaidsContent.CRONDIS_PALM)) {
                if (!Inv.has(player, "crondis_water_container", 1)) {
                    Msg.game(player, "The palm needs water. (" + watered + "/" + needed + ")");
                    return true;
                }
                Inv.remove(player, "crondis_water_container", 1);
                watered++;
                world().sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 20, 0.5, 0.5, 0.5, 0);
                say(player.getName().getString() + " waters the palm. (" + Math.min(watered, needed) + "/" + needed + ")");
                if (watered >= needed) {
                    say("The Palm of Resourcefulness flourishes!");
                    complete();
                }
                return true;
            }
            return false;
        }
    }

    // ================================================================== bosses

    /**
     * Kephri (OSRS wiki): three shield bars, then her 80-hitpoint body. Each time a shield breaks she is dazed and calls
     * her children: overlords (a Spitting Scarab, then a Soldier and an Arcane Scarab; More Overlords adds one) and 28,
     * then 24, scarab swarms that recharge her shield when they reach her. While shielded she throws fireballs and
     * bomber scarabs, Dung Strikes and lays eggs: small eggs burst, dark eggs hatch Agile Scarabs.
     */
    static final class Kephri extends RoomLogic {
        /** Arcane Scarab relocation spots around Kephri (OSRS: south-east, north-west, mid-south, north-east). */
        private static final int[][] ARCANE_SPOTS = {{6, 8}, {-4, -10}, {0, 12}, {6, -8}};
        private DataBossEntity kephri;
        private final List<LivingEntity> guards = new ArrayList<>();
        /** Shield bars broken so far (3 = her body is exposed). */
        private int broken;
        private boolean dazed;
        /** Swarms that reached her during the current intermission, and the shield overcharge they left. */
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
        public boolean exitsEast() {
            return false;
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
            arcane = null;
            kephri = spawnBoss("kephri", at(14, 0));
            anchor(kephri);
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
                RaidEffects.later(1, this::shieldBroken);
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
                RaidEffects.later(1, this::shieldBroken);
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
            // OSRS: 28 (then 24) swarms from six points around the room, clockwise, faster and faster.
            int swarms = Invocation.kephriSwarms(broken);
            int delay = 0;
            int start = rnd().nextInt(6);
            for (int i = 0; i < swarms; i++) {
                delay += Invocation.kephriSwarmDelay(i);
                int point = (start + i) % 6;
                RaidEffects.later(delay, () -> {
                    if (alive(kephri) && dazed) {
                        // Six points on a ring around the room's centre, half a step off her axis so none is at her side.
                        double a = Math.PI / 3 * point + Math.PI / 6;
                        spawnMob("toa_scarab_swarm", at((int) Math.round(Math.cos(a) * 16), (int) Math.round(Math.sin(a) * 16)), false);
                    }
                });
            }
            int finish = delay + 4;
            RaidEffects.later(finish, this::endIntermission);
        }

        /** The swarms are spent: once her overlords are dead the shield is back at full strength. */
        private void endIntermission() {
            if (!alive(kephri) || !dazed) {
                return;
            }
            if (guarded()) {
                RaidEffects.later(5, this::endIntermission);
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
            RaidEffects.later(3, () -> {
                if (!alive(kephri) || !p.isAlive()) {
                    return;
                }
                Msg.red(p, "Kephri throws you back!");
                Vec3 push = p.position().subtract(kephri.position()).multiply(1, 0, 1).normalize().scale(1.4);
                p.push(push.x, 0.4, push.z);
                p.hurtMarked = true;
                RaidEffects.hit(kephri, p, 5 + rnd().nextInt(11), null, true); // approx: dung strike damage
                world().sendParticles(ParticleTypes.ITEM_SLIME, p.getX(), p.getY() + 0.5, p.getZ(), 12, 0.6, 0.3, 0.6, 0.05);
            });
        }

        /** Kephri's fireball (and a bomber scarab's): lands where the target stood; 3x3 and harder with Aerial Assault. */
        private void fireball(ServerPlayer p) {
            Set<Invocation> inv = raid.invocations();
            int radius = Invocation.kephriFireballRadius(inv);
            int max = (int) Math.round(24 * Invocation.kephriFireballDamage(inv));
            RaidEffects.strike(world(), p.position(), 0.8 + 1.5 * radius, max, 3, kephri, ParticleTypes.FLAME, AttackType.MAGIC, true);
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            if (!alive(kephri)) {
                return;
            }
            Set<Invocation> inv = raid.invocations();
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
                        swarm.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 20 * 600, 2, false, false));
                    }
                }
            }
            if (tick % 2 == 0 && (dazed || guarded())) {
                world().sendParticles(ParticleTypes.ENCHANT, kephri.getX(), kephri.getY() + 1, kephri.getZ(), 10, 1.2, 0.8, 1.2, 0.2);
            }
            // OSRS: a swarm that reaches her recharges about 10% of a shield (heals her body in the last phase): 1% of her
            // maximum per hitpoint of the 10-hitpoint swarm.
            int reached = healers("toa_scarab_swarm", kephri, raid.maxHitpoints(kephri) * 0.01,
                    "A scarab swarm reaches Kephri and recharges her shield!");
            if (dazed) {
                leaked += reached;
            }
            agileScarabs();
        }

        /** Intermission: bomber scarabs, the Soldier's healing swarms, the Spitting Scarab's spray, the Arcane charge. */
        private void intermission(long tick) {
            List<ServerPlayer> players = players();
            if (players.isEmpty()) {
                return;
            }
            if (tick % 8 == 0) {
                // OSRS: bomber scarabs fly at players like Kephri's fireball (same flight distance, Aerial Assault applies).
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
                    LivingEntity target = g instanceof net.minecraft.world.entity.Mob m ? m.getTarget() : null;
                    for (ServerPlayer p : players) {
                        if (p != target && p.distanceToSqr(g) < 8 * 8) {
                            if (RaidEffects.hit(g, p, rnd().nextInt(9), AttackType.RANGED, false) > 0 && rnd().nextInt(4) == 0) {
                                com.runecraft.combat.PoisonService.poison(p, 6, com.runecraft.action.ActionManager.gameTick());
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
                        RaidEffects.hit(arcane, p, RaidEffects.protectedFrom(p, AttackType.MAGIC) ? roll / 3 : roll, null, true);
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
                for (int i = 0; i < 2; i++) {
                    ServerPlayer p = players.get(rnd().nextInt(players.size()));
                    Vec3 at = p.position().add(rnd().nextInt(7) - 3, 0, rnd().nextInt(7) - 3);
                    // OSRS: unattended small eggs explode in a 3x3 area. // approx: 8 ticks, up to 12
                    RaidEffects.strike(world(), at, 1.6, 12, 8, kephri, ParticleTypes.ITEM_SLIME, null, true);
                }
                int dark = Invocation.kephriDarkEggs(inv, rnd());
                for (int i = 0; i < dark; i++) {
                    ServerPlayer p = players.get(rnd().nextInt(players.size()));
                    BlockPos egg = inRoom(BlockPos.containing(p.position().add(rnd().nextInt(9) - 4, 0, rnd().nextInt(9) - 4)));
                    world().sendParticles(ParticleTypes.SQUID_INK, egg.getX() + 0.5, egg.getY() + 0.3, egg.getZ() + 0.5, 8, 0.3, 0.1, 0.3, 0.01);
                    RaidEffects.later(8, () -> {
                        if (!alive(kephri)) {
                            return;
                        }
                        // A player standing on the dark egg crushes it before it hatches. // approx: OSRS eggs are attacked
                        for (ServerPlayer q : players()) {
                            if (q.blockPosition().distSqr(egg) <= 2) {
                                Msg.game(q, "You crush a dark egg.");
                                return;
                            }
                        }
                        spawnMob("toa_agile_scarab", egg, false);
                    });
                }
            }
        }

        /** Keeps a spot inside the room's walls. */
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
                if (near != null && s instanceof net.minecraft.world.entity.Mob m) {
                    Vec3 away = s.position().subtract(near.position()).multiply(1, 0, 1).normalize().scale(4);
                    m.getNavigation().moveTo(s.getX() + away.x, s.getY(), s.getZ() + away.z, 1.4);
                }
            }
        }

        @Override
        public void cleanup() {
            super.cleanup();
            for (String k : new String[]{"toa_scarab_swarm", "toa_soldier_scarab", "toa_spitting_scarab", "toa_arcane_scarab",
                    "toa_agile_scarab"}) {
                living(k).forEach(LivingEntity::discard);
            }
        }
    }

    /** Akkha: style rotation, shadows at every 20%, orbs of darkness over the quadrants, unstable orbs when enraged. */
    static final class Akkha extends RoomLogic {
        private static final String[] STYLES = {"melee", "ranged", "magic"};
        private DataBossEntity akkha;
        private LivingEntity shadow;
        private final Set<Integer> shadows = new HashSet<>();
        private int style;
        /** Auto-attack cycles since the fight began (OSRS: a special attack follows every six autos). */
        private int autos;
        /** Attacks made with the current style (Stay Vigilant needs two before another switch). */
        private int styleAttacks;
        /** Game ticks left of Trailing Orbs, and where each player stood last tick. */
        private int trailTicks;
        private final Map<UUID, Vec3> trailLast = new HashMap<>();

        Akkha(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public boolean exitsEast() {
            return false;
        }

        @Override
        public void decorate() {
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    RaidBuilder.set(world(), at(sx * 10, sz * 10), Blocks.SOUL_LANTERN.defaultBlockState());
                }
            }
        }

        @Override
        public void start() {
            shadows.clear();
            shadow = null;
            autos = 0;
            style = 0;
            styleAttacks = 0;
            trailTicks = 0;
            trailLast.clear();
            akkha = spawnBoss("akkha", at(8, 0));
        }

        @Override
        public boolean bossVulnerable(DataBossEntity boss) {
            return !alive(shadow);
        }

        @Override
        public AttackDef bossAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            List<AttackDef> list = attacks(boss);
            return list.size() >= 3 ? list.get(style) : null;
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            if (!alive(akkha)) {
                return;
            }
            Set<Invocation> inv = raid.invocations();
            autoAttack(akkha, tick, 6);
            if (tick % 6 == 0) {
                // One auto-attack cycle (Akkha attacks every 6 ticks). // approx: counted on the clock, not per landed attack
                autos++;
                if (Invocation.akkhaKeepBack(inv) && style != 0) {
                    // OSRS Keep Back: a melee hit lands on anyone next to him just before his ranged/magic attack.
                    for (ServerPlayer p : players()) {
                        if (p.distanceToSqr(akkha) < 2.6 * 2.6) {
                            RaidEffects.hit(akkha, p, rnd().nextInt(27), AttackType.SLASH, false);
                        }
                    }
                }
                styleAttacks++;
                boolean special = autos % Invocation.akkhaSpecialEvery(inv) == 0;
                if (special) {
                    specials(inv);
                }
                // OSRS: he changes style after each special; with Stay Vigilant a 1/4 roll after an auto-attack and a
                // 1/5 roll after a special, never before two attacks with a style.
                int oneIn = Invocation.akkhaSwitchOneIn(inv, special);
                boolean early = inv.contains(Invocation.STAY_VIGILANT) && styleAttacks < Invocation.AKKHA_MIN_STYLE_ATTACKS;
                if (oneIn > 0 && !early && rnd().nextInt(oneIn) == 0) {
                    switchStyle();
                }
            }
            double hp = raid.healthFraction(akkha);
            for (int t : new int[]{80, 60, 40, 20}) {
                if (hp * 100 <= t && shadows.add(t)) {
                    shadow = spawnMob("akkhas_shadow", BlockPos.containing(akkha.position()).offset(rnd().nextInt(7) - 3, 0, 4), false);
                    warn("Akkha splits off a shadow! Kill it to make him vulnerable.");
                }
            }
            trailingOrbs(inv);
            if (hp < 0.2 && tick % 3 == 0) {
                ServerPlayer p = randomPlayer();
                if (p != null) {
                    RaidEffects.strike(world(), p.position().add(rnd().nextInt(3) - 1, 0, rnd().nextInt(3) - 1), 1.3, 15, 3, akkha,
                            ParticleTypes.WITCH);
                }
            }
        }

        private void switchStyle() {
            // OSRS: always the same cycle, melee -> ranged -> magic.
            style = Invocation.akkhaNextStyle(style);
            styleAttacks = 0;
            say("Akkha switches to " + STYLES[style] + "!");
        }

        /** Quadrant (0-3) of a position: west/east times north/south of the room centre. */
        private int quadrant(Vec3 pos) {
            return (pos.x < center().getX() + 0.5 ? 0 : 2) + (pos.z < center().getZ() + 0.5 ? 0 : 1);
        }

        private Vec3 quadrantCentre(int q) {
            return vec(q < 2 ? -9 : 9, q % 2 == 0 ? -9 : 9);
        }

        /**
         * Akkha's special attacks (OSRS): Memory Blast, Trailing Orbs and (in groups) Detonate; two at once with
         * Double Trouble.
         */
        private void specials(Set<Invocation> inv) {
            List<Integer> pool = new ArrayList<>(List.of(0, 1));
            if (raid.partySize() > 1) {
                pool.add(2);
            }
            java.util.Collections.shuffle(pool, rnd());
            for (int sp : pool.subList(0, Math.min(pool.size(), Invocation.akkhaSpecialsAtOnce(inv)))) {
                switch (sp) {
                    case 0 -> memoryBlast(inv);
                    case 1 -> {
                        warn("Akkha's orbs begin to trail you! Stand still or dodge them.");
                        trailTicks = 8;
                        trailLast.clear();
                    }
                    default -> detonate(inv);
                }
            }
        }

        /** Memory Blast: the quadrants light up in order; afterwards the blast hits everyone outside each safe one in turn. */
        private void memoryBlast(Set<Invocation> inv) {
            warn("Akkha's quadrants glow in turn... remember the order!");
            int rounds = Invocation.akkhaMemoryBlastRounds(raid.pathLevel());
            int gap = Invocation.akkhaMemoryBlastTicks(inv);
            List<Integer> order = new ArrayList<>();
            for (int k = 0; k < rounds; k++) {
                int q;
                do {
                    q = rnd().nextInt(4);
                } while (!order.isEmpty() && order.get(order.size() - 1) == q);
                order.add(q);
                int shown = q;
                RaidEffects.later(1 + 2 * k, () -> {
                    Vec3 c = quadrantCentre(shown);
                    world().sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y + 1, c.z, 40, 3, 0.5, 3, 0.01);
                });
            }
            int start = 3 + 2 * rounds;
            for (int k = 0; k < rounds; k++) {
                int safe = order.get(k);
                RaidEffects.later(start + k * gap, () -> {
                    if (!alive(akkha)) {
                        return;
                    }
                    for (ServerPlayer p : players()) {
                        if (quadrant(p.position()) != safe) {
                            world().sendParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 1, p.getZ(), 10, 0.4, 0.4, 0.4, 0.02);
                            RaidEffects.hit(akkha, p, 15 + rnd().nextInt(16), null, true); // approx: memory blast damage
                        }
                    }
                });
            }
        }

        /** Trailing Orbs: every step a player takes leaves an orb behind (and one ahead with Feeling Special?). */
        private void trailingOrbs(Set<Invocation> inv) {
            if (trailTicks <= 0) {
                return;
            }
            trailTicks--;
            for (ServerPlayer p : players()) {
                Vec3 now = p.position();
                Vec3 last = trailLast.put(p.getUUID(), now);
                if (last != null && RaidEffects.horizontalDistSq(last, now) > 0.25) {
                    Mechanics.addHazard(world(), last, 10, 12, akkha); // approx: orb damage
                    world().sendParticles(ParticleTypes.WITCH, last.x, last.y + 0.5, last.z, 6, 0.2, 0.2, 0.2, 0);
                    if (Invocation.akkhaTrailingOrbs(inv) > 1) {
                        Vec3 ahead = now.add(now.subtract(last).normalize().scale(1.5));
                        Mechanics.addHazard(world(), ahead, 10, 12, akkha);
                    }
                }
            }
        }

        /** Detonate (groups only): each player explodes in a + (and x with Feeling Special?) hitting others in line. */
        private void detonate(Set<Invocation> inv) {
            warn("Akkha marks everyone for detonation! Don't line up with each other.");
            boolean diagonals = Invocation.akkhaDetonateDiagonals(inv, raid.partySize());
            RaidEffects.later(4, () -> {
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
                        boolean inLine = (dx < 0.8 && dz <= 8) || (dz < 0.8 && dx <= 8) || (diagonals && Math.abs(dx - dz) < 0.8 && dx <= 8);
                        if (!sameTile && inLine) {
                            RaidEffects.hit(akkha, b, 10 + rnd().nextInt(16), null, true); // approx: detonation damage
                        }
                    }
                }
            });
        }

        @Override
        public void cleanup() {
            super.cleanup();
            living("akkhas_shadow").forEach(LivingEntity::discard);
        }
    }

    /** Ba-Ba: rolling boulders at 66% and 33% (she is immune until they are destroyed), baboons and falling rocks. */
    static final class Baba extends RoomLogic {
        private DataBossEntity baba;
        private final Set<Integer> phases = new HashSet<>();
        /** Baboons seen alive (Jungle Japes: their deaths drop banana peels). */
        private final Set<LivingEntity> baboons = new HashSet<>();
        /** Banana peels on the floor, with the game tick they rot away. */
        private final Map<Vec3, Long> peels = new HashMap<>();
        /** Boulders alive last tick (a destroyed one counts towards "I'm in a rush"). */
        private final Set<LivingEntity> knownBoulders = new HashSet<>();

        Baba(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public boolean exitsEast() {
            return false;
        }

        @Override
        public void decorate() {
            for (int i = 0; i < 6; i++) {
                RaidBuilder.set(world(), at(rnd().nextInt(30) - 15, rnd().nextBoolean() ? -19 : 19), Blocks.JUNGLE_LEAVES.defaultBlockState()
                        .setValue(LeavesBlock.PERSISTENT, true));
            }
        }

        @Override
        public void start() {
            phases.clear();
            baboons.clear();
            peels.clear();
            baba = spawnBoss("baba", at(8, 0));
        }

        @Override
        public boolean bossVulnerable(DataBossEntity boss) {
            return living("baba_boulder").isEmpty();
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            if (!alive(baba)) {
                return;
            }
            autoAttack(baba, tick, 5);
            double hp = raid.healthFraction(baba);
            for (int t : new int[]{66, 33}) {
                if (hp * 100 <= t && phases.add(t)) {
                    scream();
                    warn("Ba-Ba summons rolling boulders! Destroy them!");
                    for (int i = 0; i < 3 + raid.partySize() / 2; i++) {
                        spawnMob("baba_boulder", at(-15 + i * 6, -18), false);
                    }
                    if (raid.invocations().contains(Invocation.BOULDERDASH)) {
                        // OSRS: Boulderdash makes the boulders spawn twice as fast; a second line follows.
                        RaidEffects.later(4, () -> {
                            for (int i = 0; i < 3 + raid.partySize() / 2; i++) {
                                spawnMob("baba_boulder", at(-12 + i * 6, -18), false);
                            }
                        });
                    }
                }
            }
            for (LivingEntity b : knownBoulders) {
                if (b.isDeadOrDying()) {
                    com.runecraft.content.systems.CombatAchievementService.countNear(baba, com.runecraft.osrs.content.systems.CombatAchievements.COUNT_BABA_BOULDER);
                }
            }
            knownBoulders.clear();
            knownBoulders.addAll(living("baba_boulder"));
            for (LivingEntity b : living("baba_boulder")) {
                TobRooms.walk(b, vec(b.getX() - center().getX() - 0.5, 19));
                for (ServerPlayer p : players()) {
                    if (p.distanceToSqr(b) < 1.4 * 1.4 && !cooldown(p, 2)) {
                        RaidEffects.hit(null, p, 15 + rnd().nextInt(10), null, true);
                        Msg.red(p, "A rolling boulder crushes you!");
                    }
                }
                if (b.getZ() > center().getZ() + 17) {
                    world().sendParticles(ParticleTypes.EXPLOSION, b.getX(), b.getY() + 0.5, b.getZ(), 2, 0.4, 0.4, 0.4, 0);
                    b.discard();
                }
            }
            Set<Invocation> inv = raid.invocations();
            if (tick % 12 == 6) {
                slam(inv);
            }
            if (tick % 20 == 10) {
                sarcophagus(inv);
            }
            bananas(tick, inv);
            int baboons = living("toa_baboon_brawler").size() + living("toa_baboon_thrower").size() + living("toa_baboon_mage").size();
            if (tick % 30 == 0 && baboons < RaidScaling.babaBaboons(raid.partySize())) {
                String[] kinds = {"toa_baboon_brawler", "toa_baboon_thrower", "toa_baboon_mage"};
                LivingEntity b = spawnMob(kinds[rnd().nextInt(3)], at(rnd().nextInt(10), rnd().nextBoolean() ? -10 : 10), false);
                if (b != null) {
                    // OSRS: Ba-Ba's baboons have 10 static hitpoints.
                    raid.setHitpoints(b, RaidScaling.BABA_BABOON_HP);
                }
            }
        }

        /**
         * Ba-Ba's scream at 66% and 33% knocks everyone to the bottom of the room (OSRS); with Mind the Gap! anyone in
         * the middle columns falls into the pit and dies unless they stand at the northern or southern side.
         */
        private void scream() {
            warn("Ba-Ba screams, knocking you back!");
            boolean gap = Invocation.babaMindTheGap(raid.invocations());
            for (ServerPlayer p : players()) {
                double dx = p.getX() - (center().getX() + 0.5);
                double dz = p.getZ() - (center().getZ() + 0.5);
                if (gap && Math.abs(dx) < 3.5 && Math.abs(dz) < 14) { // approx: the room's north and south edges are safe
                    Msg.red(p, "You are flung into the gap!");
                    RaidEffects.hit(baba, p, 999, null, true);
                    continue;
                }
                p.push(0, 0.4, 1.4);
                p.hurtMarked = true;
            }
        }

        /** Shockwave: Ba-Ba slams the ground around her target, 5x5 (7x7 with Shaking Things Up). */
        private void slam(Set<Invocation> inv) {
            ServerPlayer target = null;
            double best = 6 * 6;
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
         * A baboon smashes a sarcophagus open and it discharges an energy blast at a player; with Gotta Have Faith the
         * blast hits harder the more prayer points the player has (OSRS).
         */
        private void sarcophagus(Set<Invocation> inv) {
            if (living("toa_baboon_brawler").isEmpty() && living("toa_baboon_thrower").isEmpty() && living("toa_baboon_mage").isEmpty()) {
                return;
            }
            ServerPlayer p = randomPlayer();
            if (p == null) {
                return;
            }
            say("A baboon smashes open a sarcophagus! Energy pours out.");
            Vec3 at = p.position();
            double radius = 1.2 + 0.6 * (Math.min(4, raid.pathLevel()) / 2);
            world().sendParticles(ParticleTypes.SOUL, at.x, at.y + 0.2, at.z, 20, radius / 2, 0.1, radius / 2, 0.01);
            RaidEffects.later(3, () -> {
                for (ServerPlayer pl : players()) {
                    if (RaidEffects.horizontalDistSq(pl.position(), at) <= radius * radius) {
                        int dmg = rnd().nextInt(13) + Invocation.babaFaithDamage(inv, RoomLogic.prayerPoints(pl)); // approx: 0-12
                        RaidEffects.hit(baba, pl, dmg, null, true);
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
            for (String k : new String[]{"toa_baboon_brawler", "toa_baboon_thrower", "toa_baboon_mage"}) {
                baboons.addAll(living(k));
            }
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
                        RaidEffects.hit(null, p, 1 + rnd().nextInt(3), null, true); // approx: a small amount of damage
                        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, Invocation.BANANA_STUN_TICKS * 12, 9, false, false));
                        pi.remove();
                        break;
                    }
                }
            }
        }

        @Override
        public void cleanup() {
            super.cleanup();
            for (String k : new String[]{"baba_boulder", "toa_baboon_brawler", "toa_baboon_thrower", "toa_baboon_mage"}) {
                living(k).forEach(LivingEntity::discard);
            }
        }
    }

    /** Zebak: hide behind the jugs from his great waves; acid pools; enrages at 25%. */
    static final class Zebak extends RoomLogic {
        private DataBossEntity zebak;
        private boolean enraged;
        /** Live blood clouds: x, z and hitpoints. */
        private final List<double[]> clouds = new ArrayList<>();

        Zebak(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public boolean exitsEast() {
            return false;
        }

        @Override
        public void decorate() {
            for (int x : new int[]{-6, 2}) {
                for (int z = -14; z <= 14; z += 7) {
                    RaidBuilder.set(world(), at(x + (z % 2 == 0 ? 0 : 1), z), Blocks.DECORATED_POT.defaultBlockState());
                }
            }
            BlockPos c = center();
            RaidBuilder.fill(world(), c.getX() + 16, c.getY(), c.getZ() - 8, c.getX() + 19, c.getY(), c.getZ() + 8,
                    Blocks.WATER.defaultBlockState());
        }

        @Override
        public void start() {
            enraged = false;
            zebak = spawnBoss("zebak", at(13, 0));
            anchor(zebak);
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            if (!alive(zebak)) {
                return;
            }
            autoAttack(zebak, tick, enraged ? 3 : 5);
            if (!enraged && raid.healthFraction(zebak) <= 0.25) {
                enraged = true;
                warn("Zebak roars in fury!");
            }
            if (raid.roomTicks() % 30 == 20) {
                warn("Zebak calls forth a great wave! Hide behind a jug!");
                RaidEffects.later(4, () -> {
                    if (!alive(zebak)) {
                        return;
                    }
                    for (int z = -18; z <= 18; z += 2) {
                        world().sendParticles(ParticleTypes.SPLASH, center().getX() + 15, center().getY() + 1.5, center().getZ() + z, 20, 0.5,
                                0.6, 0.5, 0.2);
                    }
                    for (ServerPlayer p : players()) {
                        Vec3 eye = p.getEyePosition();
                        Vec3 east = new Vec3(zebak.getX() - 2, eye.y - 0.8, eye.z);
                        boolean covered = !clearLine(p.position().add(0, 0.5, 0), new Vec3(east.x, p.getY() + 0.5, east.z), p);
                        if (!covered) {
                            RaidEffects.hit(zebak, p, 25 + rnd().nextInt(26), null, true);
                            p.push(-1.2, 0.4, 0);
                            p.hurtMarked = true;
                            Msg.red(p, "The wave crashes into you!");
                        }
                    }
                });
            }
            if (tick % 8 == 0) {
                ServerPlayer p = randomPlayer();
                if (p != null) {
                    Vec3 at = p.position();
                    // OSRS: Upset Stomach spreads the acid pools from 3x3 to 5x5.
                    double spread = raid.invocations().contains(Invocation.UPSET_STOMACH) ? 2.3 : 1.3;
                    RaidEffects.strike(world(), at, spread, 15, 3, zebak, ParticleTypes.ITEM_SLIME);
                    RaidEffects.later(3, () -> {
                        int r = spread > 2 ? 1 : 0;
                        for (int dx = -r; dx <= r; dx++) {
                            for (int dz = -r; dz <= r; dz++) {
                                Mechanics.addHazard(world(), at.add(dx * 1.5, 0, dz * 1.5), 6, 12, zebak);
                            }
                        }
                    });
                }
            }
            // OSRS Not Just a Head: Zebak's blood magic always hits every player for 8-11 and heals him for about
            // two thirds of it (Arterial Spray: more healing). // approx: cast every 24 ticks
            if (tick % 24 == 12 && raid.invocations().contains(Invocation.NOT_JUST_A_HEAD)) {
                say("Zebak casts blood magic!");
                int total = 0;
                for (ServerPlayer p : players()) {
                    world().sendParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY() + 1, p.getZ(), 6, 0.3, 0.4, 0.3, 0.1);
                    total += RaidEffects.hit(zebak, p, 8 + rnd().nextInt(4), null, true);
                }
                double share = raid.invocations().contains(Invocation.ARTERIAL_SPRAY) ? 1.0 : 2 / 3.0;
                if (total > 0) {
                    raid.heal(zebak, (int) Math.round(total * share));
                }
            }
            // OSRS Not Just a Head: Zebak summons blood clouds (three with Blood Thinners). // approx: every 24 ticks
            int clouds = Invocation.zebakBloodClouds(raid.invocations());
            if (clouds > 0 && tick % 24 == 0) {
                say(clouds > 1 ? "Zebak summons blood clouds!" : "Zebak summons a blood cloud!");
                for (int i = 0; i < clouds; i++) {
                    this.clouds.add(new double[]{center().getX() + 10.5, center().getZ() + 0.5 + (i - (clouds - 1) / 2.0) * 3,
                            Invocation.BLOOD_CLOUD_HP});
                }
            }
            moveClouds();
        }

        /**
         * Blood clouds drift a tile a tick towards the nearest player, losing 2 hitpoints per tile, and hurt an adjacent
         * player, healing themselves by the damage (OSRS wiki).
         */
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
                if (best <= 2.25) {
                    int dealt = RaidEffects.hit(zebak, near, 3 + rnd().nextInt(4), null, true);
                    c[2] += dealt;
                } else {
                    double dist = Math.sqrt(best);
                    c[0] += (near.getX() - c[0]) / dist;
                    c[1] += (near.getZ() - c[1]) / dist;
                    c[2] -= Invocation.BLOOD_CLOUD_COST_PER_TILE;
                }
                world().sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.6f, 0.0f, 0.0f), 2.0f),
                        c[0], y, c[1], 8, 0.4, 0.3, 0.4, 0);
                if (c[2] <= 0) {
                    world().sendParticles(ParticleTypes.SMOKE, c[0], y, c[1], 10, 0.3, 0.3, 0.3, 0.02);
                    it.remove();
                }
            }
        }

        @Override
        public void cleanup() {
            super.cleanup();
            clouds.clear();
        }
    }

    /**
     * The Wardens (OSRS wiki). Phase 1: the obelisk. Phase 2: one Warden (in OSRS the one whose energy flow was blocked
     * least; here chosen at random) with its own 4,500-hitpoint pool (scaled): hitting its body fills a bar (its 140
     * "active" hitpoints, scaled) that ejects the core, a separate creature that takes the real damage, five times over,
     * for 21/29/37 ticks depending on the Warden's health. Phase 3: the other Warden is restored (880 hitpoints), its
     * phantoms join at 60% and 40% (Elidinis' Warden: Akkha's then Kephri's; Tumeken's: Zebak's then Ba-Ba's), and at
     * 5% it heals 20% once and enrages.
     */
    static final class Wardens extends RoomLogic {
        /** Live cores (melee always hits them for its maximum, OSRS). */
        static final Set<LivingEntity> CORES = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
        private DataBossEntity obelisk;
        /** The phase 2 Warden, then the restored phase 3 Warden. */
        private DataBossEntity warden;
        private boolean elidinisFirst;
        private int phase;
        private int p2Hp;
        private int p2Max;
        private int bar;
        private LivingEntity core;
        private int coreTicks;
        private final List<String> phantoms = new ArrayList<>();
        private boolean enrageHealed;
        private int akkhaAttacks;

        /** Whether the fight is in its final phase. */
        boolean phaseThree() {
            return phase == 3;
        }

        Wardens(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public boolean entersFromWest() {
            return false;
        }

        /** Whether this creature is a phase 2 Warden's ejected core. */
        static boolean isCore(LivingEntity e) {
            return e != null && CORES.contains(e);
        }

        @Override
        public void decorate() {
            BlockPos c = center();
            RaidBuilder.fill(world(), c.getX() + 10, c.getY(), c.getZ() - 10, c.getX() + 18, c.getY(), c.getZ() + 10,
                    Blocks.GOLD_BLOCK.defaultBlockState());
            RaidBuilder.pillar(world(), c.offset(18, 0, -14), 1, 9, Blocks.PRISMARINE_BRICKS.defaultBlockState());
            RaidBuilder.pillar(world(), c.offset(18, 0, 14), 1, 9, Blocks.CUT_SANDSTONE.defaultBlockState());
        }

        @Override
        public void start() {
            phase = 1;
            warden = null;
            core = null;
            coreTicks = 0;
            phantoms.clear();
            enrageHealed = false;
            akkhaAttacks = 0;
            obelisk = spawnBoss("wardens_obelisk", at(12, 0));
            anchor(obelisk);
        }

        @Override
        public void reset() {
            dropCore();
            super.reset();
            obelisk = null;
            warden = null;
        }

        @Override
        public void cleanup() {
            dropCore();
            super.cleanup();
        }

        private void dropCore() {
            if (core != null) {
                CORES.remove(core);
                core.discard();
                core = null;
            }
            coreTicks = 0;
        }

        private String name(DataBossEntity w) {
            return w == null || w.def() == null ? "The Warden" : w.def().name;
        }

        @Override
        public int bossIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (phase != 2 || boss != warden || damage <= 0) {
                return damage;
            }
            if (core == null) {
                bar += damage;
                // OSRS loot calculator: phase 2 Warden damage is worth 2 points each.
                raid.addPoints(attacker.getUUID(), damage * 2);
                if (bar >= RaidScaling.toaCoreChargeNeeded(raid.raidLevel(), raid.partySize())) {
                    ejectCore();
                }
            }
            // The Warden's own hitpoints only follow its core's pool (see onCreatureHit).
            return 0;
        }

        private void ejectCore() {
            bar = 0;
            coreTicks = RaidScaling.toaCoreExposureTicks(p2Hp / (double) Math.max(1, p2Max));
            BlockPos at = BlockPos.containing(warden.getX() - 4, warden.getY(), warden.getZ());
            core = spawnMob("toa_warden_core", at, false);
            if (core != null) {
                CORES.add(core);
            }
            com.runecraft.content.systems.CombatAchievementService.countNear(warden,
                    com.runecraft.osrs.content.systems.CombatAchievements.COUNT_WARDEN_CORE);
            warn(name(warden) + " staggers - its core is ejected! Strike the core now!");
        }

        @Override
        public void onCreatureHit(ServerPlayer player, LivingEntity target, int damage) {
            if (phase != 2 || target != core || core == null || damage <= 0) {
                return;
            }
            // OSRS: damage to the core counts five times against the Warden.
            p2Hp -= damage * RaidScaling.TOA_CORE_MULTIPLIER;
            core.setHealth(core.getMaxHealth());
            if (p2Hp <= 0) {
                restoreOther();
                return;
            }
            syncBody();
        }

        /** The phase 2 Warden's visible health follows its core's pool. */
        private void syncBody() {
            raid.setCurrentHitpoints(warden, (int) Math.ceil(raid.maxHitpoints(warden) * (p2Hp / (double) Math.max(1, p2Max))));
        }

        /** Only core damage counts in phase 2: recoil, Vengeance or poison (which skip bossIncoming) can't kill the body. */
        @Override
        public boolean bossAllowDeath(DataBossEntity boss) {
            if (phase != 2 || boss != warden) {
                return true;
            }
            syncBody();
            return false;
        }

        /** The phase 2 Warden collapses and uses its last power to restore the other one (OSRS). */
        private void restoreOther() {
            dropCore();
            String other = elidinisFirst ? "tumekens_warden" : "elidinis_warden";
            if (warden != null) {
                unanchor(warden);
                warden.discard();
            }
            warden = spawnBoss(other, at(14, 0));
            anchor(warden);
            phase = 3;
            warn("The Warden's body collapses... " + name(warden) + " is restored! The obelisk explodes!");
            if (alive(obelisk)) {
                obelisk.discard();
            }
        }

        @Override
        public int bossOutgoing(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
            return phase == 3 ? (int) Math.round(damage * 1.3) : damage; // approx: phase 3 slams
        }

        @Override
        public void gameTick(long tick) {
            if (obelisk == null && warden == null) {
                return;
            }
            List<ServerPlayer> players = players();
            Set<Invocation> inv = raid.invocations();
            if (phase == 1) {
                if (!alive(obelisk)) {
                    phase = 2;
                    elidinisFirst = rnd().nextBoolean();
                    warden = spawnBoss(elidinisFirst ? "elidinis_warden" : "tumekens_warden", at(14, 0));
                    anchor(warden);
                    p2Max = RaidScaling.toaWardenP2Hp(raid.raidLevel(), raid.partySize());
                    p2Hp = p2Max;
                    bar = 0;
                    warn("The obelisk falls silent - " + name(warden) + " awakens!");
                    return;
                }
                autoAttack(obelisk, tick, 5);
                // OSRS Ancient Haste: the Wardens charge faster in phase 1.
                if (tick % Invocation.wardensChargeEvery(inv) == 0) {
                    say("The obelisk crackles with energy!");
                    for (ServerPlayer p : players) {
                        RaidEffects.strike(world(), p.position(), 1.4, 20, 3, obelisk, ParticleTypes.END_ROD);
                    }
                }
                return;
            }
            if (!alive(warden)) {
                if (phase == 3) {
                    complete();
                } else if (phase == 2) {
                    // The body went some other way (removed, or a death the guard didn't see): don't stall the fight.
                    restoreOther();
                }
                return;
            }
            if (phase == 2) {
                phaseTwo(tick, players, inv);
            } else {
                phaseThree(tick, players, inv);
            }
        }

        private void phaseTwo(long tick, List<ServerPlayer> players, Set<Invocation> inv) {
            // Undo chip damage that skipped bossIncoming (recoil, Vengeance, poison) so it can't build up.
            syncBody();
            if (core != null) {
                coreTicks--;
                world().sendParticles(ParticleTypes.END_ROD, core.getX(), core.getY() + 0.5, core.getZ(), 3, 0.3, 0.3, 0.3, 0.01);
                if (coreTicks <= 0 || !core.isAlive()) {
                    dropCore();
                    say("The Warden's core retreats back into its body.");
                }
            }
            // OSRS Acceleration: the phase 2 Warden attacks faster and the obelisk charges faster.
            if (core == null) {
                autoAttack(warden, tick, Invocation.wardensAttackEvery(inv));
            }
            int strikeEvery = Invocation.wardensObeliskEvery(inv);
            if (tick % strikeEvery == 0 && !players.isEmpty()) {
                say("Divine lightning strikes the arena!");
                // OSRS Penetration: the phase 2 obelisk hits harder and its hits switch off overhead prayers.
                int max = (int) Math.round(25 * Invocation.wardensObeliskDamage(inv));
                for (ServerPlayer p : players) {
                    RaidEffects.strike(world(), p.position(), 1.4, max, 3, warden, ParticleTypes.ELECTRIC_SPARK);
                }
                if (Invocation.wardensPenetration(inv)) {
                    RaidEffects.later(3, () -> {
                        for (ServerPlayer p : players()) {
                            RoomLogic.dropOverheads(p, "The obelisk's energy pierces your prayers!");
                        }
                    });
                }
            }
        }

        private void phaseThree(long tick, List<ServerPlayer> players, Set<Invocation> inv) {
            double hp = raid.healthFraction(warden);
            boolean elidinis = !elidinisFirst;
            for (int[] at : new int[][]{{60, 0}, {40, 1}}) {
                if (hp * 100 <= at[0] && phantoms.size() == at[1]) {
                    String phantom = elidinis ? (at[1] == 0 ? "Akkha" : "Kephri") : (at[1] == 0 ? "Zebak" : "Ba-Ba");
                    phantoms.add(phantom);
                    warn(phantom + "'s phantom rises to assist the Warden!");
                }
            }
            if (!enrageHealed && hp <= 0.05) {
                // OSRS: at 5% the Warden heals 20% of its hitpoints once and enrages.
                enrageHealed = true;
                raid.heal(warden, raid.maxHitpoints(warden) / 5);
                warn(name(warden) + " shakes violently and draws in the last of its power!");
            }
            if (players.isEmpty()) {
                return;
            }
            // OSRS: Overclocked, Overclocked 2 and Insanity bring the slam 1/2/3 ticks sooner.
            if (tick % Math.max(3, 6 - Invocation.wardensSlamFaster(inv)) == 0) {
                ServerPlayer p = players.get(rnd().nextInt(players.size()));
                RaidEffects.strike(world(), p.position().add(rnd().nextInt(5) - 2, 0, rnd().nextInt(5) - 2), 1.5, 25, 3, warden,
                        ParticleTypes.FLAME);
            }
            if (enrageHealed && tick % 20 == 10) {
                warn("Lightning tears through the arena!");
                for (int i = 0; i < 6; i++) {
                    RaidEffects.strike(world(), vec(rnd().nextInt(30) - 18, rnd().nextInt(34) - 17), 2.0, 35, 4, warden,
                            ParticleTypes.CLOUD);
                }
            }
            for (String phantom : phantoms) {
                phantomAttack(phantom, tick, players, inv);
            }
        }

        /** The path bosses' phantoms use their originals' attacks, with the same invocations (OSRS wiki). */
        private void phantomAttack(String phantom, long tick, List<ServerPlayer> players, Set<Invocation> inv) {
            ServerPlayer p = players.get(rnd().nextInt(players.size()));
            switch (phantom) {
                case "Kephri" -> {
                    // Kephri's fireball: lands where the player stood; 3x3 and harder with Aerial Assault.
                    if (tick % 6 == 3) {
                        int radius = Invocation.kephriFireballRadius(inv);
                        int max = (int) Math.round(24 * Invocation.kephriFireballDamage(inv));
                        RaidEffects.strike(world(), p.position(), 0.8 + 1.5 * radius, max, 3, warden, ParticleTypes.FLAME, AttackType.MAGIC, true);
                    }
                }
                case "Akkha" -> {
                    // Ranged and magic, switching every three attacks (randomly with Stay Vigilant), starting with ranged.
                    if (tick % 5 == 1) { // approx: attack speed
                        boolean magic = inv.contains(Invocation.STAY_VIGILANT) ? rnd().nextBoolean() : (akkhaAttacks / 3) % 2 == 1;
                        akkhaAttacks++;
                        RaidEffects.hit(warden, p, rnd().nextInt(21), magic ? AttackType.MAGIC : AttackType.RANGED, false); // approx: max 20
                    }
                }
                case "Zebak" -> {
                    if (tick % 7 == 2) { // approx: Zebak's rock and jug attacks
                        boolean magic = rnd().nextBoolean();
                        RaidEffects.hit(warden, p, rnd().nextInt(26), magic ? AttackType.MAGIC : AttackType.RANGED, false);
                    }
                }
                default -> {
                    // Ba-Ba's phantom only launches boulders: step a tile away from the shadow.
                    if (tick % 6 == 0) {
                        RaidEffects.strike(world(), p.position(), 1.0, 25, 3, warden, ParticleTypes.CAMPFIRE_COSY_SMOKE); // approx: damage
                    }
                }
            }
        }
    }
}
