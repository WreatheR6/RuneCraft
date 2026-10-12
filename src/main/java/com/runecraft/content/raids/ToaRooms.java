package com.runecraft.content.raids;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.Mechanics;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
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
            autoAttack(kephri, tick, 5);
            double hp = raid.healthFraction(kephri);
            for (int t : new int[]{66, 33}) {
                if (hp * 100 <= t && shields.add(t)) {
                    warn("Kephri's shield regenerates! Her children rush to her aid!");
                    guards.clear();
                    for (String id : new String[]{"toa_soldier_scarab", "toa_spitting_scarab", "toa_arcane_scarab"}) {
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
            if (tick % 20 == 0) {
                for (int i = 0; i < 2; i++) {
                    spawnMob("toa_scarab_swarm", at(-6 + rnd().nextInt(12), i % 2 == 0 ? -18 : 18), false);
                }
            }
            if (tick % 2 == 0 && shielded()) {
                world().spawnParticles(ParticleTypes.ENCHANT, kephri.getX(), kephri.getY() + 1, kephri.getZ(), 10, 1.2, 0.8, 1.2, 0.2);
            }
            healers("toa_scarab_swarm", kephri, raid.maxHitpoints(kephri) * 0.02, "A scarab swarm reaches Kephri and heals her!");
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
            if (raid.roomTicks() % 12 == 0) {
                style = rnd().nextInt(3);
                say("Akkha switches to " + STYLES[style] + "!");
            }
            autoAttack(akkha, tick, 5);
            double hp = raid.healthFraction(akkha);
            for (int t : new int[]{80, 60, 40, 20}) {
                if (hp * 100 <= t && shadows.add(t)) {
                    shadow = spawnMob("akkhas_shadow", BlockPos.ofFloored(akkha.getPos()).add(rnd().nextInt(7) - 3, 0, 4), false);
                    warn("Akkha splits off a shadow! Kill it to make him vulnerable.");
                }
            }
            if (tick % 25 == 12) {
                warn("Akkha's orbs of darkness gather over the room!");
                List<int[]> quadrants = new ArrayList<>(List.of(new int[]{-1, -1}, new int[]{-1, 1}, new int[]{1, -1}, new int[]{1, 1}));
                java.util.Collections.shuffle(quadrants, rnd());
                for (int q = 0; q < 2; q++) {
                    int[] quad = quadrants.get(q);
                    for (int gx = 0; gx < 3; gx++) {
                        for (int gz = 0; gz < 3; gz++) {
                            Vec3d at = vec(quad[0] * (3 + gx * 6), quad[1] * (3 + gz * 6));
                            RaidEffects.strike(world(), at, 3.3, 30, 4 + q * 2, akkha, ParticleTypes.SQUID_INK);
                        }
                    }
                }
            }
            if (hp < 0.2 && tick % 3 == 0) {
                ServerPlayerEntity p = randomPlayer();
                if (p != null) {
                    RaidEffects.strike(world(), p.getPos().add(rnd().nextInt(3) - 1, 0, rnd().nextInt(3) - 1), 1.3, 15, 3, akkha,
                            ParticleTypes.WITCH);
                }
            }
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
                    warn("Ba-Ba summons rolling boulders! Destroy them!");
                    for (int i = 0; i < 3 + raid.partySize() / 2; i++) {
                        spawnMob("baba_boulder", at(-15 + i * 6, -18), false);
                    }
                }
            }
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
            int baboons = living("toa_baboon_brawler").size() + living("toa_baboon_thrower").size() + living("toa_baboon_mage").size();
            if (tick % 30 == 0 && baboons < 2 + raid.partySize()) {
                String[] kinds = {"toa_baboon_brawler", "toa_baboon_thrower", "toa_baboon_mage"};
                spawnMob(kinds[rnd().nextInt(3)], at(rnd().nextInt(10), rnd().nextBoolean() ? -10 : 10), false);
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
                    RaidEffects.strike(world(), at, 1.3, 15, 3, zebak, ParticleTypes.ITEM_SLIME);
                    RaidEffects.later(3, () -> Mechanics.addHazard(world(), at, 6, 12, zebak));
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
                if (tick % 12 == 0) {
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
            if (eAlive) {
                autoAttack(elidinis, tick, 5);
            }
            if (tAlive) {
                autoAttack(tumeken, tick + 2, 5);
            }
            if (tick % 15 == 0 && !players.isEmpty()) {
                say("Divine lightning strikes the arena!");
                for (ServerPlayerEntity p : players) {
                    RaidEffects.strike(world(), p.getPos(), 1.4, 25, 3, core, ParticleTypes.ELECTRIC_SPARK);
                }
            }
            if (phase == 3 && !players.isEmpty()) {
                if (tick % 6 == 0) {
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
