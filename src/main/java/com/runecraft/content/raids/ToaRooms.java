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
