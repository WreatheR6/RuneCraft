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
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
        public void decorate() {
            tiles.clear();
            for (int dz = -4; dz <= 4; dz += 4) {
                for (int dx = -4; dx <= 4; dx += 4) {
                    BlockPos p = center().add(dx, 0, dz);
                    RaidBuilder.set(world(), p, RaidBuilder.module(RaidsContent.SCABARAS_TILE));
                    tiles.add(p);
                }
            }
            RaidBuilder.pillar(world(), center().add(12, 0, -10), 1, 4, Blocks.CHISELED_SANDSTONE.getDefaultState());
            RaidBuilder.pillar(world(), center().add(12, 0, 10), 1, 4, Blocks.CHISELED_SANDSTONE.getDefaultState());
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
                    world().spawnParticles(ParticleTypes.END_ROD, t.getX() + 0.5, t.getY() + 1.2, t.getZ() + 0.5, 25, 0.3, 0.5, 0.3, 0.02);
                    world().playSound(null, t, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.BLOCKS, 1f,
                            0.6f + sequence.get(showIndex / 2) * 0.1f);
                }
                if (showIndex >= sequence.size() * 2) {
                    showIndex = Integer.MAX_VALUE;
                    say("Now step on the tiles in the same order.");
                }
                return;
            }
            for (ServerPlayerEntity p : players()) {
                int on = tiles.indexOf(p.getBlockPos().down());
                Integer before = standing.put(p.getUuid(), on);
                if (on >= 0 && (before == null || before != on)) {
                    press(p, on);
                }
            }
        }

        private void press(ServerPlayerEntity p, int tile) {
            if (done || showIndex != Integer.MAX_VALUE) {
                return;
            }
            BlockPos t = tiles.get(tile);
            if (sequence.get(input) == tile) {
                input++;
                world().spawnParticles(ParticleTypes.HAPPY_VILLAGER, t.getX() + 0.5, t.getY() + 1.2, t.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0);
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
                for (ServerPlayerEntity pl : players()) {
                    RaidEffects.hit(null, pl, 5 + rnd().nextInt(8), null, true);
                }
                replay();
            }
        }

        @Override
        public boolean useBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
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
        public void decorate() {
            RaidBuilder.set(world(), at(16, 0), RaidBuilder.module(RaidsContent.HET_SEAL));
            RaidBuilder.set(world(), at(16, 0).up(), Blocks.GOLD_BLOCK.getDefaultState());
            RaidBuilder.pillar(world(), center().add(16, 0, -3), 0, 4, Blocks.CHISELED_SANDSTONE.getDefaultState());
            RaidBuilder.pillar(world(), center().add(16, 0, 3), 0, 4, Blocks.CHISELED_SANDSTONE.getDefaultState());
            RaidBuilder.set(world(), at(-19, 0).up(2), Blocks.GLOWSTONE.getDefaultState());
        }

        @Override
        public void start() {
            progress = 0;
            needed = 20 + 8 * (raid.partySize() - 1);
        }

        @Override
        public void gameTick(long tick) {
            if (tick % 6 == 0) {
                ServerPlayerEntity p = randomPlayer();
                if (p != null) {
                    say("The light of Het sweeps across the room!");
                    for (int x = -18; x <= 14; x += 2) {
                        RaidEffects.strike(world(), new Vec3d(center().getX() + 0.5 + x, p.getY(), p.getZ()), 1.2, 18, 3, null,
                                ParticleTypes.END_ROD);
                    }
                }
            }
        }

        @Override
        public boolean useBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
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
            player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            progress += 1 + level(player, Skill.MINING) / 30;
            XpService.add(player, Skill.MINING, 20);
            world().spawnParticles(ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.4, 0.4, 0.4, 0.1);
            Msg.action(player, "You chip at Het's seal. (" + Math.min(progress, needed) + "/" + needed + ")");
            if (progress >= needed) {
                RaidBuilder.set(world(), pos, Blocks.AIR.getDefaultState());
                RaidBuilder.set(world(), pos.up(), Blocks.AIR.getDefaultState());
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
            long now = ActionManager.gameTick();
            if (problem == null) {
                if (--cooldown <= 0) {
                    List<BlockPos> list = new ArrayList<>(fixtures.keySet());
                    problem = list.get(rnd().nextInt(list.size()));
                    deadline = now + 12;
                    warn("Apmeken's sight reveals: " + fixtures.get(problem) + "!");
                }
            } else {
                world().spawnParticles(ParticleTypes.LARGE_SMOKE, problem.getX() + 0.5, problem.getY() + 1.2, problem.getZ() + 0.5, 4, 0.3,
                        0.3, 0.3, 0.01);
                if (now > deadline) {
                    warn("You were too slow - the room shakes violently!");
                    for (ServerPlayerEntity p : players()) {
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
        public boolean useBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
            if (!fixtures.containsKey(pos)) {
                return false;
            }
            if (!pos.equals(problem)) {
                Msg.game(player, "Nothing seems wrong with this right now.");
                return true;
            }
            player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
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
        public void decorate() {
            RaidBuilder.set(world(), at(-16, 0), RaidBuilder.module(RaidsContent.CRONDIS_WATER));
            BlockPos palm = at(14, 0);
            RaidBuilder.set(world(), palm, RaidBuilder.module(RaidsContent.CRONDIS_PALM));
            RaidBuilder.fill(world(), palm.getX(), palm.getY() + 1, palm.getZ(), palm.getX(), palm.getY() + 3, palm.getZ(),
                    Blocks.JUNGLE_LOG.getDefaultState());
            RaidBuilder.fill(world(), palm.getX() - 2, palm.getY() + 4, palm.getZ() - 2, palm.getX() + 2, palm.getY() + 4, palm.getZ() + 2,
                    Blocks.JUNGLE_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true));
        }

        @Override
        public void start() {
            watered = 0;
            needed = 10 + 4 * (raid.partySize() - 1);
        }

        @Override
        public void gameTick(long tick) {
            if (tick % 5 == 0) {
                ServerPlayerEntity p = randomPlayer();
                if (p != null) {
                    Vec3d at = p.getPos();
                    RaidEffects.strike(world(), at, 1.3, 10, 3, null, ParticleTypes.ITEM_SLIME);
                    RaidEffects.later(3, () -> Mechanics.addHazard(world(), at, 4, 10, null));
                }
            }
        }

        @Override
        public void onPlayerHurt(ServerPlayerEntity player, int damage) {
            if (damage > 0 && Inv.has(player, "crondis_water_container", 1) && rnd().nextInt(3) == 0) {
                Inv.remove(player, "crondis_water_container", 1);
                Msg.red(player, "You spill your water!");
            }
        }

        @Override
        public boolean useBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
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
                world().spawnParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 20, 0.5, 0.5, 0.5, 0);
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

    /** Kephri: her shield regenerates at 66% and 33% while her scarab guardians live; swarms heal her. */
    static final class Kephri extends RoomLogic {
        private DataBossEntity kephri;
        private final List<LivingEntity> guards = new ArrayList<>();
        private final Set<Integer> shields = new HashSet<>();

        Kephri(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            BlockPos c = center();
            RaidBuilder.fill(world(), c.getX() + 12, c.getY(), c.getZ() - 4, c.getX() + 17, c.getY(), c.getZ() + 4,
                    Blocks.CHISELED_SANDSTONE.getDefaultState());
        }

        @Override
        public void start() {
            shields.clear();
            guards.clear();
            kephri = spawnBoss("kephri", at(14, 0));
            anchor(kephri);
        }

        private boolean shielded() {
            for (LivingEntity g : guards) {
                if (alive(g)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean bossVulnerable(DataBossEntity boss) {
            return !shielded();
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            if (!alive(kephri)) {
                return;
            }
            Set<Invocation> inv = raid.invocations();
            // OSRS: Kephri's fireball lands where the target stood (3x3 with Aerial Assault); faster every two path levels.
            int every = 6 - Math.min(4, raid.pathLevel()) / 2;
            if (tick % every == 0) {
                ServerPlayerEntity p = randomPlayer();
                if (p != null) {
                    int radius = Invocation.kephriFireballRadius(inv);
                    int max = (int) Math.round(24 * Invocation.kephriFireballDamage(inv));
                    RaidEffects.strike(world(), p.getPos(), 0.8 + 1.5 * radius, max, 3, kephri, ParticleTypes.FLAME, AttackType.MAGIC, true);
                }
            }
            double hp = raid.healthFraction(kephri);
            for (int t : new int[]{66, 33}) {
                if (hp * 100 <= t && shields.add(t)) {
                    warn("Kephri's shield regenerates! Her children rush to her aid!");
                    guards.clear();
                    List<String> ids = new ArrayList<>(List.of("toa_soldier_scarab", "toa_spitting_scarab", "toa_arcane_scarab"));
                    // OSRS More Overlords: an extra Soldier Scarab in the first shield phase, a Spitting Scarab in the second.
                    String extra = Invocation.kephriExtraOverlord(inv, shields.size());
                    if (extra != null) {
                        ids.add(extra);
                    }
                    for (String id : ids) {
                        LivingEntity g = spawnMob(id, at(8, rnd().nextInt(13) - 6), false);
                        if (g != null) {
                            guards.add(g);
                        }
                    }
                    for (int i = 0; i < 4 + raid.partySize(); i++) {
                        spawnMob("toa_scarab_swarm", at(-6 + rnd().nextInt(12), i % 2 == 0 ? -18 : 18), false);
                    }
                }
            }
            if (shielded()) {
                shieldSpecials(tick, inv);
            }
            // OSRS: swarms only come outside her shield phases with the Medic! invocation.
            if (tick % 20 == 0 && raid.invocations().contains(Invocation.MEDIC)) {
                for (int i = 0; i < 2; i++) {
                    LivingEntity swarm = spawnMob("toa_scarab_swarm", at(-6 + rnd().nextInt(12), i % 2 == 0 ? -18 : 18), false);
                    if (swarm != null && Invocation.kephriMedicSwarmSpeed(inv) < 1.0) {
                        // OSRS: Medic!'s swarms move 50% slower (Slowness III: -45%).
                        swarm.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                                net.minecraft.entity.effect.StatusEffects.SLOWNESS, 20 * 600, 2, false, false));
                    }
                }
            }
            if (tick % 2 == 0 && shielded()) {
                world().spawnParticles(ParticleTypes.ENCHANT, kephri.getX(), kephri.getY() + 1, kephri.getZ(), 10, 1.2, 0.8, 1.2, 0.2);
            }
            healers("toa_scarab_swarm", kephri, raid.maxHitpoints(kephri) * 0.02, "A scarab swarm reaches Kephri and heals her!");
        }

        /**
         * Kephri's shield phase specials (OSRS): Dung Strike knocks a player back (two players with Blowing Mud) and
         * Mass Incubation lays eggs that burst in a 3x3 area (four with Lively Larvae).
         */
        private void shieldSpecials(long tick, Set<Invocation> inv) {
            List<ServerPlayerEntity> players = players();
            if (players.isEmpty()) {
                return;
            }
            if (tick % 16 == 4) {
                List<ServerPlayerEntity> pool = new ArrayList<>(players);
                java.util.Collections.shuffle(pool, rnd());
                int n = Invocation.kephriDungTargets(inv, players.size());
                for (ServerPlayerEntity p : pool.subList(0, Math.min(n, pool.size()))) {
                    Msg.red(p, "Flies buzz around you...");
                    RaidEffects.later(3, () -> {
                        if (!alive(kephri) || !p.isAlive()) {
                            return;
                        }
                        Vec3d push = p.getPos().subtract(kephri.getPos()).multiply(1, 0, 1).normalize().multiply(1.4);
                        p.addVelocity(push.x, 0.4, push.z);
                        p.velocityModified = true;
                        RaidEffects.hit(kephri, p, 5 + rnd().nextInt(11), null, true); // approx: dung strike damage
                        world().spawnParticles(ParticleTypes.ITEM_SLIME, p.getX(), p.getY() + 0.5, p.getZ(), 12, 0.6, 0.3, 0.6, 0.05);
                    });
                }
            }
            if (tick % 16 == 12) {
                int eggs = Invocation.kephriEggs(inv);
                say("Kephri lays her eggs!");
                for (int i = 0; i < eggs; i++) {
                    ServerPlayerEntity p = players.get(rnd().nextInt(players.size()));
                    Vec3d at = p.getPos().add(rnd().nextInt(7) - 3, 0, rnd().nextInt(7) - 3);
                    // OSRS: unattended eggs explode in a 3x3 area. // approx: 8 ticks, up to 12
                    RaidEffects.strike(world(), at, 1.6, 12, 8, kephri, ParticleTypes.ITEM_SLIME, null, true);
                }
            }
        }

        @Override
        public void cleanup() {
            super.cleanup();
            for (String k : new String[]{"toa_scarab_swarm", "toa_soldier_scarab", "toa_spitting_scarab", "toa_arcane_scarab"}) {
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
        private final Map<UUID, Vec3d> trailLast = new HashMap<>();

        Akkha(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    RaidBuilder.set(world(), at(sx * 10, sz * 10), Blocks.SOUL_LANTERN.getDefaultState());
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
        public AttackDef bossAttack(DataBossEntity boss, ServerPlayerEntity target, double distance) {
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
                    for (ServerPlayerEntity p : players()) {
                        if (p.squaredDistanceTo(akkha) < 2.6 * 2.6) {
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
                    shadow = spawnMob("akkhas_shadow", BlockPos.ofFloored(akkha.getPos()).add(rnd().nextInt(7) - 3, 0, 4), false);
                    warn("Akkha splits off a shadow! Kill it to make him vulnerable.");
                }
            }
            trailingOrbs(inv);
            if (hp < 0.2 && tick % 3 == 0) {
                ServerPlayerEntity p = randomPlayer();
                if (p != null) {
                    RaidEffects.strike(world(), p.getPos().add(rnd().nextInt(3) - 1, 0, rnd().nextInt(3) - 1), 1.3, 15, 3, akkha,
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
        private int quadrant(Vec3d pos) {
            return (pos.x < center().getX() + 0.5 ? 0 : 2) + (pos.z < center().getZ() + 0.5 ? 0 : 1);
        }

        private Vec3d quadrantCentre(int q) {
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
                    Vec3d c = quadrantCentre(shown);
                    world().spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y + 1, c.z, 40, 3, 0.5, 3, 0.01);
                });
            }
            int start = 3 + 2 * rounds;
            for (int k = 0; k < rounds; k++) {
                int safe = order.get(k);
                RaidEffects.later(start + k * gap, () -> {
                    if (!alive(akkha)) {
                        return;
                    }
                    for (ServerPlayerEntity p : players()) {
                        if (quadrant(p.getPos()) != safe) {
                            world().spawnParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 1, p.getZ(), 10, 0.4, 0.4, 0.4, 0.02);
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
            for (ServerPlayerEntity p : players()) {
                Vec3d now = p.getPos();
                Vec3d last = trailLast.put(p.getUuid(), now);
                if (last != null && RaidEffects.horizontalDistSq(last, now) > 0.25) {
                    Mechanics.addHazard(world(), last, 10, 12, akkha); // approx: orb damage
                    world().spawnParticles(ParticleTypes.WITCH, last.x, last.y + 0.5, last.z, 6, 0.2, 0.2, 0.2, 0);
                    if (Invocation.akkhaTrailingOrbs(inv) > 1) {
                        Vec3d ahead = now.add(now.subtract(last).normalize().multiply(1.5));
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
                List<ServerPlayerEntity> players = players();
                for (ServerPlayerEntity a : players) {
                    world().spawnParticles(ParticleTypes.EXPLOSION, a.getX(), a.getY() + 1, a.getZ(), 1, 0, 0, 0, 0);
                    for (ServerPlayerEntity b : players) {
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
        private final Map<Vec3d, Long> peels = new HashMap<>();
        /** Boulders alive last tick (a destroyed one counts towards "I'm in a rush"). */
        private final Set<LivingEntity> knownBoulders = new HashSet<>();

        Baba(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            for (int i = 0; i < 6; i++) {
                RaidBuilder.set(world(), at(rnd().nextInt(30) - 15, rnd().nextBoolean() ? -19 : 19), Blocks.JUNGLE_LEAVES.getDefaultState()
                        .with(LeavesBlock.PERSISTENT, true));
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
                if (b.isDead()) {
                    com.runecraft.content.systems.CombatAchievementService.countNear(baba, com.runecraft.osrs.content.systems.CombatAchievements.COUNT_BABA_BOULDER);
                }
            }
            knownBoulders.clear();
            knownBoulders.addAll(living("baba_boulder"));
            for (LivingEntity b : living("baba_boulder")) {
                TobRooms.walk(b, vec(b.getX() - center().getX() - 0.5, 19));
                for (ServerPlayerEntity p : players()) {
                    if (p.squaredDistanceTo(b) < 1.4 * 1.4 && !cooldown(p, 2)) {
                        RaidEffects.hit(null, p, 15 + rnd().nextInt(10), null, true);
                        Msg.red(p, "A rolling boulder crushes you!");
                    }
                }
                if (b.getZ() > center().getZ() + 17) {
                    world().spawnParticles(ParticleTypes.EXPLOSION, b.getX(), b.getY() + 0.5, b.getZ(), 2, 0.4, 0.4, 0.4, 0);
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
            for (ServerPlayerEntity p : players()) {
                double dx = p.getX() - (center().getX() + 0.5);
                double dz = p.getZ() - (center().getZ() + 0.5);
                if (gap && Math.abs(dx) < 3.5 && Math.abs(dz) < 14) { // approx: the room's north and south edges are safe
                    Msg.red(p, "You are flung into the gap!");
                    RaidEffects.hit(baba, p, 999, null, true);
                    continue;
                }
                p.addVelocity(0, 0.4, 1.4);
                p.velocityModified = true;
            }
        }

        /** Shockwave: Ba-Ba slams the ground around her target, 5x5 (7x7 with Shaking Things Up). */
        private void slam(Set<Invocation> inv) {
            ServerPlayerEntity target = null;
            double best = 6 * 6;
            for (ServerPlayerEntity p : players()) {
                double d = p.squaredDistanceTo(baba);
                if (d < best) {
                    best = d;
                    target = p;
                }
            }
            if (target != null) {
                RaidEffects.strike(world(), target.getPos(), Invocation.babaSlamRadius(inv) + 0.5, 20, 3, baba, ParticleTypes.CLOUD,
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
            ServerPlayerEntity p = randomPlayer();
            if (p == null) {
                return;
            }
            say("A baboon smashes open a sarcophagus! Energy pours out.");
            Vec3d at = p.getPos();
            double radius = 1.2 + 0.6 * (Math.min(4, raid.pathLevel()) / 2);
            world().spawnParticles(ParticleTypes.SOUL, at.x, at.y + 0.2, at.z, 20, radius / 2, 0.1, radius / 2, 0.01);
            RaidEffects.later(3, () -> {
                for (ServerPlayerEntity pl : players()) {
                    if (RaidEffects.horizontalDistSq(pl.getPos(), at) <= radius * radius) {
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
                if (b.isDead()) {
                    for (int i = 0; i < per; i++) {
                        peels.put(b.getPos().add(rnd().nextDouble() * 2 - 1, 0, rnd().nextDouble() * 2 - 1), tick + 100);
                    }
                }
            }
            peels.entrySet().removeIf(e -> e.getValue() < tick);
            java.util.Iterator<Map.Entry<Vec3d, Long>> pi = peels.entrySet().iterator();
            while (pi.hasNext()) {
                Vec3d peel = pi.next().getKey();
                if (tick % 4 == 0) {
                    world().spawnParticles(ParticleTypes.FALLING_HONEY, peel.x, peel.y + 0.3, peel.z, 2, 0.2, 0.1, 0.2, 0);
                }
                for (ServerPlayerEntity p : players()) {
                    if (RaidEffects.horizontalDistSq(p.getPos(), peel) < 0.7 * 0.7) {
                        Msg.red(p, "You slip on a banana peel!");
                        RaidEffects.hit(null, p, 1 + rnd().nextInt(3), null, true); // approx: a small amount of damage
                        p.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                                net.minecraft.entity.effect.StatusEffects.SLOWNESS, Invocation.BANANA_STUN_TICKS * 12, 9, false, false));
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

        Zebak(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            for (int x : new int[]{-6, 2}) {
                for (int z = -14; z <= 14; z += 7) {
                    RaidBuilder.set(world(), at(x + (z % 2 == 0 ? 0 : 1), z), Blocks.DECORATED_POT.getDefaultState());
                }
            }
            BlockPos c = center();
            RaidBuilder.fill(world(), c.getX() + 16, c.getY(), c.getZ() - 8, c.getX() + 19, c.getY(), c.getZ() + 8,
                    Blocks.WATER.getDefaultState());
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
                        world().spawnParticles(ParticleTypes.SPLASH, center().getX() + 15, center().getY() + 1.5, center().getZ() + z, 20, 0.5,
                                0.6, 0.5, 0.2);
                    }
                    for (ServerPlayerEntity p : players()) {
                        Vec3d eye = p.getEyePos();
                        Vec3d east = new Vec3d(zebak.getX() - 2, eye.y - 0.8, eye.z);
                        boolean covered = !clearLine(p.getPos().add(0, 0.5, 0), new Vec3d(east.x, p.getY() + 0.5, east.z), p);
                        if (!covered) {
                            RaidEffects.hit(zebak, p, 25 + rnd().nextInt(26), null, true);
                            p.addVelocity(-1.2, 0.4, 0);
                            p.velocityModified = true;
                            Msg.red(p, "The wave crashes into you!");
                        }
                    }
                });
            }
            if (tick % 8 == 0) {
                ServerPlayerEntity p = randomPlayer();
                if (p != null) {
                    Vec3d at = p.getPos();
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
                for (ServerPlayerEntity p : players()) {
                    world().spawnParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY() + 1, p.getZ(), 6, 0.3, 0.4, 0.3, 0.1);
                    total += RaidEffects.hit(zebak, p, 8 + rnd().nextInt(4), null, true);
                }
                double share = raid.invocations().contains(Invocation.ARTERIAL_SPRAY) ? 1.0 : 2 / 3.0;
                if (total > 0) {
                    raid.heal(zebak, (int) Math.round(total * share));
                }
            }
        }
    }

    /**
     * The Wardens: the obelisk, then both Wardens, then the enraged survivor. In phase 2 a Warden only
     * loses hitpoints while its core is ejected (OSRS): hitting its body charges it until the core pops
     * out for a few seconds, and one ejection can take at most half its hitpoints.
     */
    static final class Wardens extends RoomLogic {
        /** Game ticks an ejected core stays out. // approx */
        private static final int CORE_TICKS = 20;
        private DataBossEntity obelisk;
        private DataBossEntity elidinis;
        private DataBossEntity tumeken;
        private int phase;
        private final java.util.Map<LivingEntity, Integer> charge = new java.util.HashMap<>();
        private final java.util.Map<LivingEntity, Integer> coreTicks = new java.util.HashMap<>();
        private final java.util.Map<LivingEntity, Integer> coreDamage = new java.util.HashMap<>();

        /** Whether the fight is in its final (enraged) phase. */
        boolean phaseThree() {
            return phase == 3;
        }

        Wardens(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            BlockPos c = center();
            RaidBuilder.fill(world(), c.getX() + 10, c.getY(), c.getZ() - 10, c.getX() + 18, c.getY(), c.getZ() + 10,
                    Blocks.GOLD_BLOCK.getDefaultState());
            RaidBuilder.pillar(world(), c.add(18, 0, -14), 1, 9, Blocks.PRISMARINE_BRICKS.getDefaultState());
            RaidBuilder.pillar(world(), c.add(18, 0, 14), 1, 9, Blocks.CUT_SANDSTONE.getDefaultState());
        }

        @Override
        public void start() {
            phase = 1;
            elidinis = null;
            tumeken = null;
            obelisk = spawnBoss("wardens_obelisk", at(12, 0));
            anchor(obelisk);
        }

        @Override
        public void reset() {
            super.reset();
            obelisk = null;
        }

        /** The Warden to show and strike at: one with an ejected core, else a living one. */
        private DataBossEntity exposed() {
            if (coreTicks.getOrDefault(elidinis, 0) > 0 && alive(elidinis)) {
                return elidinis;
            }
            if (coreTicks.getOrDefault(tumeken, 0) > 0 && alive(tumeken)) {
                return tumeken;
            }
            return alive(elidinis) ? elidinis : tumeken;
        }

        private boolean coreOut(LivingEntity warden) {
            return coreTicks.getOrDefault(warden, 0) > 0;
        }

        @Override
        public int bossIncoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            if (phase != 2 || (boss != elidinis && boss != tumeken) || damage <= 0) {
                return damage;
            }
            int max = raid.maxHitpoints(boss);
            if (!coreOut(boss)) {
                int c = charge.getOrDefault(boss, 0) + damage;
                if (c >= RaidScaling.toaCoreChargeNeeded(max)) {
                    charge.put(boss, 0);
                    coreTicks.put(boss, CORE_TICKS);
                    com.runecraft.content.systems.CombatAchievementService.countNear(boss, com.runecraft.osrs.content.systems.CombatAchievements.COUNT_WARDEN_CORE);
                    coreDamage.put(boss, 0);
                    warn((boss.def() == null ? "The Warden" : boss.def().name) + " staggers - its core is ejected! Strike it now!");
                } else {
                    charge.put(boss, c);
                }
                return 0;
            }
            int dealt = coreDamage.getOrDefault(boss, 0);
            int allowed = Math.max(0, Math.min(damage, RaidScaling.toaCoreDamageCap(max) - dealt));
            coreDamage.put(boss, dealt + allowed);
            if (dealt + allowed >= RaidScaling.toaCoreDamageCap(max)) {
                coreTicks.put(boss, 0);
                say("The Warden's core retreats back into its body.");
            }
            return allowed;
        }

        @Override
        public int bossOutgoing(DataBossEntity boss, ServerPlayerEntity player, int damage, AttackType type) {
            return phase == 3 ? (int) Math.round(damage * 1.3) : damage;
        }

        @Override
        public void gameTick(long tick) {
            if (obelisk == null) {
                return;
            }
            List<ServerPlayerEntity> players = players();
            if (phase == 1) {
                if (!alive(obelisk)) {
                    phase = 2;
                    elidinis = spawnBoss("elidinis_warden", at(14, -6));
                    tumeken = spawnBoss("tumekens_warden", at(14, 6));
                    anchor(elidinis);
                    anchor(tumeken);
                    warn("The obelisk shatters - the Wardens awaken!");
                    return;
                }
                autoAttack(obelisk, tick, 5);
                // OSRS Ancient Haste: the Wardens charge faster in phase 1.
                if (tick % Invocation.wardensChargeEvery(raid.invocations()) == 0) {
                    say("The obelisk crackles with energy!");
                    for (ServerPlayerEntity p : players) {
                        RaidEffects.strike(world(), p.getPos(), 1.4, 20, 3, obelisk, ParticleTypes.END_ROD);
                    }
                }
                return;
            }
            boolean eAlive = alive(elidinis);
            boolean tAlive = alive(tumeken);
            if (!eAlive && !tAlive) {
                complete();
                return;
            }
            if (phase == 2 && eAlive != tAlive) {
                phase = 3;
                DataBossEntity survivor = eAlive ? elidinis : tumeken;
                warn((survivor.def() == null ? "The Warden" : survivor.def().name) + " absorbs its partner's power! The skulls awaken!");
            }
            for (DataBossEntity w : new DataBossEntity[]{elidinis, tumeken}) {
                int t = coreTicks.getOrDefault(w, 0);
                if (t > 0) {
                    coreTicks.put(w, t - 1);
                    if (t == 1 && alive(w)) {
                        say("The Warden's core retreats back into its body.");
                    }
                }
            }
            DataBossEntity core = exposed();
            if (phase == 2 && coreOut(core) && alive(core)) {
                world().spawnParticles(ParticleTypes.END_ROD, core.getX(), core.getY() + core.getHeight() * 0.6, core.getZ(), 3, 0.3, 0.3,
                        0.3, 0.01);
            }
            Set<Invocation> inv = raid.invocations();
            // OSRS Acceleration: the phase 2 Warden attacks faster and the obelisk charges faster.
            int every = phase == 2 ? Invocation.wardensAttackEvery(inv) : 5;
            if (eAlive) {
                autoAttack(elidinis, tick, every);
            }
            if (tAlive) {
                autoAttack(tumeken, tick + 2, every);
            }
            int strikeEvery = phase == 2 ? Invocation.wardensObeliskEvery(inv) : 15;
            if (tick % strikeEvery == 0 && !players.isEmpty()) {
                say("Divine lightning strikes the arena!");
                // OSRS Penetration: the phase 2 obelisk hits harder and its hits switch off overhead prayers.
                int max = (int) Math.round(25 * (phase == 2 ? Invocation.wardensObeliskDamage(inv) : 1.0));
                for (ServerPlayerEntity p : players) {
                    RaidEffects.strike(world(), p.getPos(), 1.4, max, 3, core, ParticleTypes.ELECTRIC_SPARK);
                }
                if (phase == 2 && Invocation.wardensPenetration(inv)) {
                    RaidEffects.later(3, () -> {
                        for (ServerPlayerEntity p : players()) {
                            RoomLogic.dropOverheads(p, "The obelisk's energy pierces your prayers!");
                        }
                    });
                }
            }
            if (phase == 3 && !players.isEmpty()) {
                // OSRS: Overclocked, Overclocked 2 and Insanity bring the phase 3 slam 1/2/3 ticks sooner.
                if (tick % Math.max(3, 6 - Invocation.wardensSlamFaster(inv)) == 0) {
                    ServerPlayerEntity p = players.get(rnd().nextInt(players.size()));
                    RaidEffects.strike(world(), p.getPos().add(rnd().nextInt(5) - 2, 0, rnd().nextInt(5) - 2), 1.5, 25, 3, core,
                            ParticleTypes.FLAME);
                }
                if (tick % 20 == 10) {
                    warn("The ceiling crumbles!");
                    for (int i = 0; i < 6; i++) {
                        RaidEffects.strike(world(), vec(rnd().nextInt(30) - 18, rnd().nextInt(34) - 17), 2.0, 35, 4, core,
                                ParticleTypes.CLOUD);
                    }
                }
            }
        }
    }
}
