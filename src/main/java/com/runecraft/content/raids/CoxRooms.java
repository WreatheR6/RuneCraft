package com.runecraft.content.raids;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.Mechanics;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.PoisonService;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.raids.RaidScaling;
import com.runecraft.osrs.content.raids.RaidsContent;
import com.runecraft.osrs.content.raids.RoomDef;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.Blocks;
import net.minecraft.block.PillarBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Chambers of Xeric rooms. */
final class CoxRooms {
    private CoxRooms() {
    }

    /** Tekton: every so often he retreats to his anvil (sparks fly at everyone), then returns enraged. */
    static final class Tekton extends RoomLogic {
        private DataBossEntity tekton;
        private Vec3d anvil;
        private int fight;
        private int anvilTicks;
        private int enrage;
        /** OSRS: at the anvil Tekton repairs himself by roughly 1.11-1.33% of his hitpoints every few ticks. */
        private static final int ANVIL_HEAL_EVERY = 3;

        Tekton(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            RaidBuilder.set(world(), at(13, 0), Blocks.ANVIL.getDefaultState());
            RaidBuilder.set(world(), at(13, -2), Blocks.LAVA_CAULDRON.getDefaultState());
            RaidBuilder.set(world(), at(13, 2), Blocks.LAVA_CAULDRON.getDefaultState());
        }

        @Override
        public void start() {
            tekton = spawnBoss("tekton", at(8, 0));
            anvil = vec(11, 0);
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            if (!alive(tekton)) {
                return;
            }
            if (anvilTicks > 0) {
                anvilTicks--;
                tekton.getNavigation().stop();
                if (anvilTicks % ANVIL_HEAL_EVERY == 0) {
                    raid.heal(tekton, Math.max(1, raid.maxHitpoints(tekton) * 12 / 1000)); // approx 1.2%
                }
                world().spawnParticles(ParticleTypes.LAVA, anvil.x + 2, anvil.y + 1, anvil.z, 6, 0.4, 0.3, 0.4, 0);
                if (anvilTicks % 2 == 0) {
                    for (ServerPlayerEntity p : players()) {
                        RaidEffects.strike(world(), p.getPos(), 1.5, 18, 3, tekton, ParticleTypes.LAVA);
                    }
                }
                if (anvilTicks == 0) {
                    enrage = 16;
                    unanchor(tekton);
                    warn("Tekton's hammer glows red hot!");
                }
            } else {
                if (enrage > 0) {
                    enrage--;
                    world().spawnParticles(ParticleTypes.FLAME, tekton.getX(), tekton.getY() + 2, tekton.getZ(), 4, 0.6, 0.6, 0.6, 0.01);
                }
                if (++fight >= 35) {
                    fight = 0;
                    anvilTicks = 8;
                    move(tekton, anvil);
                    anchor(tekton);
                    say("Tekton retreats to his anvil to sharpen his hammer.");
                    com.runecraft.content.systems.CombatAchievementService.countNear(tekton, com.runecraft.osrs.content.systems.CombatAchievements.COUNT_TEKTON_ANVIL);
                }
            }
        }

        /** OSRS: Tekton is immune to Ranged and takes 80% less Magic damage. */
        @Override
        public int bossIncoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            if (type == AttackType.RANGED) {
                Msg.game(attacker, "Tekton is immune to ranged attacks.");
                return 0;
            }
            return type == AttackType.MAGIC ? damage / 5 : damage;
        }

        @Override
        public int bossOutgoing(DataBossEntity boss, ServerPlayerEntity player, int damage, AttackType type) {
            // Enraged Tekton (fresh from the anvil) has a 59 max hit instead of 52 (OSRS).
            return enrage > 0 ? (int) Math.round(damage * 59 / 52.0) : damage;
        }
    }

    /** Vasa Nistirio: siphons power from glowing crystals and teleports players to him. */
    static final class Vasa extends RoomLogic {
        private DataBossEntity vasa;
        private final List<LivingEntity> crystals = new ArrayList<>();
        private LivingEntity siphon;
        private int siphonTicks;
        private int siphonCooldown = 20;
        private int teleportCooldown = 15;

        Vasa(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    BlockPos p = center().add(sx * 13, 0, sz * 13);
                    RaidBuilder.fill(world(), p.getX() - 1, p.getY(), p.getZ() - 1, p.getX() + 1, p.getY(), p.getZ() + 1,
                            Blocks.PURPUR_BLOCK.getDefaultState());
                }
            }
        }

        @Override
        public void start() {
            vasa = spawnBoss("vasa_nistirio", at(4, 0));
            crystals.clear();
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    LivingEntity c = spawnMob("vasa_crystal", at(sx * 13, sz * 13), false);
                    if (c != null) {
                        crystals.add(c);
                        anchor(c);
                    }
                }
            }
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            if (!alive(vasa)) {
                return;
            }
            if (siphon != null) {
                if (!alive(siphon)) {
                    siphon = null;
                    CombatHandler.applyDamage(null, vasa, Math.max(1, raid.maxHitpoints(vasa) / 20));
                    vasa.setAiDisabled(true);
                    RaidEffects.later(4, () -> vasa.setAiDisabled(false));
                    warn("The crystal shatters and Vasa is stunned!");
                    return;
                }
                siphonTicks--;
                raid.heal(vasa, Math.max(1, raid.maxHitpoints(vasa) / 100));
                Vec3d a = siphon.getPos().add(0, 1.5, 0);
                Vec3d b = vasa.getPos().add(0, 2, 0);
                for (int i = 0; i <= 8; i++) {
                    Vec3d p = a.lerp(b, i / 8.0);
                    world().spawnParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                }
                if (siphonTicks <= 0) {
                    siphon = null;
                    unanchor(vasa);
                    siphonCooldown = 30;
                }
                return;
            }
            List<LivingEntity> live = new ArrayList<>();
            for (LivingEntity c : crystals) {
                if (alive(c)) {
                    live.add(c);
                }
            }
            if (--siphonCooldown <= 0 && !live.isEmpty()) {
                siphon = live.get(rnd().nextInt(live.size()));
                Vec3d dir = center().toCenterPos().subtract(siphon.getPos()).normalize().multiply(2.5);
                move(vasa, siphon.getPos().add(dir.x, 0, dir.z));
                anchor(vasa);
                siphonTicks = 20;
                warn("Vasa begins to draw power from a glowing crystal! Destroy it!");
                return;
            }
            if (charging > 0) {
                charging--;
            }
            if (--teleportCooldown <= 0) {
                teleportCooldown = 25;
                teleport();
            }
        }

        /** Game ticks left of Vasa charging his teleport barrage (he can't be harmed meanwhile). */
        private int charging;

        /**
         * Vasa's teleport (OSRS wiki): half the team (the player when solo) is pulled next to him, stunned and stripped of
         * prayers, the rest land at the edges. The barrage then hits everyone next to him for the stunned players'
         * summed hitpoints minus 5, shared between them; unstunned players can pray Protect from Magic against it.
         */
        private void teleport() {
            List<ServerPlayerEntity> party = new ArrayList<>(players());
            if (party.isEmpty()) {
                return;
            }
            java.util.Collections.shuffle(party, new java.util.Random(rnd().nextLong()));
            com.runecraft.content.systems.CombatAchievementService.countNear(vasa, com.runecraft.osrs.content.systems.CombatAchievements.COUNT_VASA_TELEPORT);
            int n = Math.min(party.size(), RaidScaling.vasaTeleportTargets(party.size()));
            List<ServerPlayerEntity> stunned = new ArrayList<>(party.subList(0, n));
            int[] hp = new int[n];
            for (int i = 0; i < n; i++) {
                ServerPlayerEntity p = stunned.get(i);
                hp[i] = com.runecraft.combat.Overheal.hitpoints(p);
                raid.teleport(p, vasa.getBlockPos().add(i % 2 == 0 ? 2 : -2, 0, i / 2 - 1));
                com.runecraft.content.systems.SysCombat.freeze(p, RaidScaling.VASA_TELEPORT_DELAY);
                dropOverheads(p, "Vasa Nistirio teleports you to him and drains your prayers!");
                Msg.red(p, "Vasa Nistirio teleports you to him!");
            }
            for (int i = n; i < party.size(); i++) {
                ServerPlayerEntity p = party.get(i);
                raid.teleport(p, at(i % 2 == 0 ? -17 : 17, i % 4 < 2 ? -6 : 6));
                Msg.red(p, "Vasa Nistirio throws you to the edge of the room! Run in and protect from magic.");
            }
            charging = RaidScaling.VASA_TELEPORT_DELAY;
            world().spawnParticles(ParticleTypes.END_ROD, vasa.getX(), vasa.getY() + 2, vasa.getZ(), 30, 1.5, 1, 1.5, 0.05);
            RaidEffects.later(RaidScaling.VASA_TELEPORT_DELAY, () -> barrage(stunned, hp));
        }

        private void barrage(List<ServerPlayerEntity> stunned, int[] hp) {
            if (!alive(vasa)) {
                return;
            }
            List<ServerPlayerEntity> receivers = new ArrayList<>();
            for (ServerPlayerEntity p : players()) {
                if (stunned.contains(p) || p.squaredDistanceTo(vasa) <= 3.5 * 3.5) {
                    receivers.add(p);
                }
            }
            int dmg = RaidScaling.vasaTeleportDamage(hp, receivers.size());
            world().spawnParticles(ParticleTypes.END_ROD, vasa.getX(), vasa.getY() + 1, vasa.getZ(), 40, 3, 0.5, 3, 0.1);
            for (ServerPlayerEntity p : receivers) {
                // Stunned players can't pray; the others' Protect from Magic blocks their share.
                RaidEffects.hit(vasa, p, dmg, AttackType.MAGIC, stunned.contains(p));
            }
        }

        @Override
        public boolean bossVulnerable(DataBossEntity boss) {
            return charging <= 0;
        }

        @Override
        public int bossIncoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            return siphon != null ? damage / 2 : damage;
        }

        @Override
        public void cleanup() {
            super.cleanup();
            for (LivingEntity c : crystals) {
                if (alive(c)) {
                    c.discard();
                }
            }
        }
    }

    /** Guardians: stone statues that can only be chipped apart with a pickaxe. */
    static final class Guardians extends RoomLogic {
        Guardians(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void start() {
            anchor(spawnBoss("cox_guardian", at(9, -4)));
            anchor(spawnBoss("cox_guardian", at(9, 4)));
        }

        @Override
        public int bossIncoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            if (!Inv.idOf(attacker.getMainHandStack()).endsWith("_pickaxe")) {
                Msg.action(attacker, "Only a pickaxe can harm the Guardians.");
                return 0;
            }
            // OSRS: damage x (50 + Mining level + pickaxe level) / 150, the pickaxe capped at a dragon pickaxe.
            int dealt = RaidScaling.coxGuardianDamage(damage, level(attacker, Skill.MINING),
                    RaidScaling.pickaxeLevel(Inv.idOf(attacker.getMainHandStack())));
            if (dealt > 0) {
                XpService.add(attacker, Skill.MINING, dealt * 2.0);
            }
            return dealt;
        }
    }

    /** Lizardman shamans: leap onto players and release purple spawn that explode. */
    static final class Shamans extends RoomLogic {
        private final Map<LivingEntity, Integer> jumpCooldown = new HashMap<>();
        private final Set<LivingEntity> jumping = new HashSet<>();

        Shamans(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            for (int i = 0; i < 8; i++) {
                BlockPos p = at(rnd().nextInt(30) - 15, rnd().nextInt(30) - 15);
                RaidBuilder.set(world(), p, Blocks.MOSSY_COBBLESTONE_WALL.getDefaultState());
                RaidBuilder.set(world(), p.up(), Blocks.MOSS_BLOCK.getDefaultState());
            }
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            for (LivingEntity e : spawned) {
                if (!alive(e) || jumping.contains(e)) {
                    continue;
                }
                int cd = jumpCooldown.getOrDefault(e, 8 + rnd().nextInt(10)) - 1;
                jumpCooldown.put(e, cd);
                if (cd > 0) {
                    continue;
                }
                jumpCooldown.put(e, 14 + rnd().nextInt(8));
                ServerPlayerEntity target = randomPlayer();
                if (target == null) {
                    continue;
                }
                Vec3d mark = target.getPos();
                jumping.add(e);
                say("A lizardman shaman leaps into the air!");
                world().spawnParticles(ParticleTypes.CLOUD, e.getX(), e.getY(), e.getZ(), 15, 0.5, 0.2, 0.5, 0.05);
                e.setInvisible(true);
                RaidEffects.strike(world(), mark, 2.5, 30, 3, e, ParticleTypes.ITEM_SLIME);
                RaidEffects.later(3, () -> {
                    jumping.remove(e);
                    e.setInvisible(false);
                    if (alive(e)) {
                        e.requestTeleport(mark.x, mark.y, mark.z);
                    }
                });
            }
            for (LivingEntity spawn : living("cox_lizardman_spawn")) {
                if (spawn.age > 6 * ActionManager.MC_TICKS_PER_GAME_TICK) {
                    world().spawnParticles(ParticleTypes.EXPLOSION, spawn.getX(), spawn.getY() + 0.5, spawn.getZ(), 2, 0.3, 0.3, 0.3, 0);
                    for (ServerPlayerEntity p : players()) {
                        if (p.squaredDistanceTo(spawn) < 2.5 * 2.5) {
                            RaidEffects.hit(null, p, rnd().nextInt(21), null, true);
                        }
                    }
                    spawn.discard();
                }
            }
        }

        @Override
        public boolean bossVulnerable(DataBossEntity boss) {
            return !jumping.contains(boss);
        }

        @Override
        public void cleanup() {
            super.cleanup();
            for (LivingEntity s : living("cox_lizardman_spawn")) {
                s.discard();
            }
        }
    }

    /** Muttadiles: the small one first, then its mother; both heal at the meat tree until it is chopped. */
    static final class Muttadiles extends RoomLogic {
        private DataBossEntity small;
        private DataBossEntity big;
        private boolean treeAlive = true;
        private int chops;
        private final Map<LivingEntity, Integer> feeding = new HashMap<>();
        private final Map<LivingEntity, Integer> feedCooldown = new HashMap<>();

        Muttadiles(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        private BlockPos tree() {
            return at(-4, 10);
        }

        @Override
        public void decorate() {
            BlockPos c = center();
            RaidBuilder.fill(world(), c.getX() + 4, c.getY(), c.getZ() - 14, c.getX() + 15, c.getY(), c.getZ() - 4,
                    Blocks.WATER.getDefaultState());
            RaidBuilder.set(world(), tree(), RaidBuilder.module(RaidsContent.MEAT_TREE));
            RaidBuilder.set(world(), tree().up(), Blocks.MANGROVE_ROOTS.getDefaultState());
            RaidBuilder.fill(world(), tree().getX() - 1, tree().getY() + 2, tree().getZ() - 1, tree().getX() + 1, tree().getY() + 3,
                    tree().getZ() + 1, Blocks.RED_MUSHROOM_BLOCK.getDefaultState());
        }

        @Override
        public void start() {
            chops = 6 + 2 * (raid.partySize() - 1);
            small = spawnBoss("small_muttadile", at(9, -9));
        }

        @Override
        public void gameTick(long tick) {
            for (DataBossEntity m : new DataBossEntity[]{small, big}) {
                if (!alive(m)) {
                    continue;
                }
                Integer f = feeding.get(m);
                if (f != null) {
                    if (!treeAlive || f <= 0) {
                        feeding.remove(m);
                        unanchor(m);
                        feedCooldown.put(m, 30);
                    } else {
                        feeding.put(m, f - 1);
                        raid.heal(m, Math.max(1, raid.maxHitpoints(m) * 3 / 100));
                        com.runecraft.content.systems.CombatAchievementService.flagNear(m,
                                com.runecraft.osrs.content.systems.CombatAchievements.FLAG_MEAT_TREE_HEAL);
                        world().spawnParticles(ParticleTypes.HEART, m.getX(), m.getY() + 1.5, m.getZ(), 1, 0.3, 0.3, 0.3, 0);
                    }
                    continue;
                }
                int cd = feedCooldown.getOrDefault(m, 0);
                if (cd > 0) {
                    feedCooldown.put(m, cd - 1);
                } else if (treeAlive && raid.healthFraction(m) < 0.4) {
                    move(m, Vec3d.ofBottomCenter(tree().add(2, 0, 0)));
                    anchor(m);
                    feeding.put(m, 8);
                    warn("The Muttadile goes to feed on the meat tree!");
                }
            }
            if (small != null && !alive(small) && big == null) {
                big = spawnBoss("muttadile", at(9, -9));
                warn("The mother Muttadile emerges from the water!");
            }
            if (big != null && !alive(big) && !alive(small)) {
                complete();
            }
        }

        @Override
        public boolean useBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
            if (!blockId.equals(RaidsContent.MEAT_TREE)) {
                return false;
            }
            if (!treeAlive || cooldown(player, 2)) {
                return true;
            }
            if (!hasTool(player, "_axe")) {
                Msg.game(player, "You need an axe to chop down the meat tree.");
                return true;
            }
            player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            if (rnd().nextDouble() < 0.3 + level(player, Skill.WOODCUTTING) / 150.0) {
                XpService.add(player, Skill.WOODCUTTING, 25);
                if (--chops <= 0) {
                    treeAlive = false;
                    RaidBuilder.fill(world(), tree().getX() - 1, tree().getY(), tree().getZ() - 1, tree().getX() + 1, tree().getY() + 3,
                            tree().getZ() + 1, Blocks.AIR.getDefaultState());
                    RaidBuilder.set(world(), tree(), Blocks.MANGROVE_ROOTS.getDefaultState());
                    say("The meat tree has been chopped down! The Muttadiles can no longer feed.");
                } else {
                    Msg.action(player, "You chop at the meat tree. (" + chops + " chops left)");
                }
            }
            return true;
        }
    }

    /** Vanguards: shells, rotation, and the health reset if one falls too far behind the others. */
    static final class Vanguards extends RoomLogic {
        private static final String[] IDS = {"vanguard_melee", "vanguard_ranged", "vanguard_magic"};
        private final DataBossEntity[] guards = new DataBossEntity[3];
        private final BlockPos[] spots = new BlockPos[3];
        private int shelled;
        private int resetCooldown;
        private int deadTicks;

        Vanguards(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void start() {
            spots[0] = at(6, -7);
            spots[1] = at(13, 0);
            spots[2] = at(6, 7);
            for (int i = 0; i < 3; i++) {
                guards[i] = spawnBoss(IDS[i], spots[i]);
                anchor(guards[i]);
            }
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            if (done) {
                return;
            }
            List<DataBossEntity> live = new ArrayList<>();
            for (DataBossEntity g : guards) {
                if (alive(g)) {
                    live.add(g);
                }
            }
            if (shelled > 0 && --shelled == 0) {
                say("The Vanguards emerge from their shells.");
            }
            if (raid.roomTicks() % 30 == 29 && live.size() > 1) {
                shelled = 4;
                say("The Vanguards retreat into their shells and change places!");
                Vec3d first = live.get(0).getPos();
                for (int i = 0; i < live.size(); i++) {
                    DataBossEntity g = live.get(i);
                    Vec3d to = i + 1 < live.size() ? live.get(i + 1).getPos() : first;
                    unanchor(g);
                    move(g, to);
                    anchor(g);
                }
            }
            if (resetCooldown > 0) {
                resetCooldown--;
            } else if (live.size() > 1) {
                double min = 1;
                double max = 0;
                double sum = 0;
                for (DataBossEntity g : live) {
                    double f = raid.healthFraction(g);
                    min = Math.min(min, f);
                    max = Math.max(max, f);
                    sum += f;
                }
                if (max - min > 0.3) {
                    double avg = sum / live.size();
                    for (DataBossEntity g : live) {
                        raid.setCurrentHitpoints(g, (int) Math.round(avg * raid.maxHitpoints(g)));
                    }
                    warn("The Vanguards' health is too uneven - they reset!");
                    com.runecraft.content.systems.CombatAchievementService.flagNear(live.get(0),
                            com.runecraft.osrs.content.systems.CombatAchievements.FLAG_VANGUARD_RESET);
                    resetCooldown = 8;
                }
            }
            if (!live.isEmpty() && live.size() < 3) {
                if (++deadTicks > 17) {
                    deadTicks = 0;
                    double avg = 0;
                    for (DataBossEntity g : live) {
                        avg += raid.healthFraction(g);
                    }
                    avg /= live.size();
                    for (int i = 0; i < 3; i++) {
                        if (!alive(guards[i])) {
                            guards[i] = spawnBoss(IDS[i], spots[i]);
                            if (guards[i] != null) {
                                raid.setCurrentHitpoints(guards[i], (int) Math.round(avg * raid.maxHitpoints(guards[i])));
                                anchor(guards[i]);
                            }
                        }
                    }
                    warn("You were too slow: the fallen Vanguard rises again!");
                }
            } else {
                deadTicks = 0;
            }
        }

        @Override
        public boolean bossVulnerable(DataBossEntity boss) {
            return shelled <= 0;
        }
    }

    /**
     * Vespula (OSRS): four lux grubs lie around the abyssal portal and Vespula stings them as she flies; a grub stung
     * to death hatches into a vespine soldier, and living soldiers heal Vespula and the portal. At 20% health she
     * lands for about 20 seconds, when the portal can be attacked safely; attacking the portal while she flies
     * enrages her and she stings everyone near it. Destroying the portal ends the encounter.
     */
    static final class Vespula extends RoomLogic {
        /** Stings that hatch a lux grub. // approx: OSRS grubs have a health bar Vespula's stings deplete */
        private static final int GRUB_STINGS = 4;
        /** Game ticks Vespula stays on the ground (OSRS: about 20 seconds). */
        private static final int LANDED_TICKS = 33;
        private static final int[][] GRUBS = {{11, -6}, {11, 6}, {16, -6}, {16, 6}};
        private DataBossEntity vespula;
        private DataBossEntity portal;
        private final int[] grubStings = new int[GRUBS.length];
        private int landedTicks;
        private boolean landedOnce;
        private int enragedTicks;

        Vespula(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            for (int i = -1; i <= 1; i += 2) {
                RaidBuilder.pillar(world(), center().add(4, 0, i * 9), 0, 4, Blocks.JUNGLE_LOG.getDefaultState());
                RaidBuilder.set(world(), center().add(4, 5, i * 9), Blocks.BEE_NEST.getDefaultState());
            }
            for (int[] g : GRUBS) {
                RaidBuilder.set(world(), at(g[0], g[1]), Blocks.HONEYCOMB_BLOCK.getDefaultState());
            }
        }

        @Override
        public void start() {
            java.util.Arrays.fill(grubStings, 0);
            landedTicks = 0;
            landedOnce = false;
            enragedTicks = 0;
            portal = spawnBoss("vespula_portal", at(15, 0));
            if (portal != null) {
                anchor(portal);
                // OSRS: the portal itself never attacks.
                portal.setAiDisabled(true);
            }
            vespula = spawnBoss("vespula", at(8, 0), false);
        }

        private boolean flying() {
            return alive(vespula) && landedTicks <= 0;
        }

        @Override
        public void gameTick(long tick) {
            if (!alive(portal)) {
                complete();
                return;
            }
            if (alive(vespula) && !landedOnce && raid.healthFraction(vespula) <= 0.2) {
                landedOnce = true;
                landedTicks = LANDED_TICKS;
                vespula.setAiDisabled(true);
                warn("Vespula crashes to the ground, exhausted! Destroy the portal now!");
                com.runecraft.content.systems.CombatAchievementService.flagNear(portal,
                        com.runecraft.osrs.content.systems.CombatAchievements.FLAG_VESPULA_LANDED);
            }
            if (landedTicks > 0 && --landedTicks == 0 && alive(vespula)) {
                vespula.setAiDisabled(false);
                say("Vespula takes to the air again.");
            }
            // Vespula stings the lux grubs while she flies.
            if (flying() && tick % 8 == 0) {
                List<Integer> live = new ArrayList<>();
                for (int i = 0; i < GRUBS.length; i++) {
                    if (grubStings[i] < GRUB_STINGS) {
                        live.add(i);
                    }
                }
                if (!live.isEmpty()) {
                    int g = live.get(rnd().nextInt(live.size()));
                    BlockPos pos = at(GRUBS[g][0], GRUBS[g][1]);
                    world().spawnParticles(ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.05);
                    if (++grubStings[g] >= GRUB_STINGS) {
                        RaidBuilder.set(world(), pos, Blocks.AIR.getDefaultState());
                        spawnMob("vespine_soldier", pos, false);
                        warn("A lux grub hatches into a vespine soldier! Its kin will heal Vespula and the portal.");
                    }
                }
            }
            // Vespine soldiers heal Vespula and the portal while they live. // approx rate
            if (tick % 4 == 0) {
                int soldiers = living("vespine_soldier").size();
                if (soldiers > 0) {
                    for (DataBossEntity b : new DataBossEntity[]{vespula, portal}) {
                        if (alive(b)) {
                            raid.heal(b, soldiers * Math.max(1, raid.maxHitpoints(b) / 100));
                        }
                    }
                }
            }
            // Enraged: she darts back and forth in front of the portal, stinging anyone close to it.
            if (enragedTicks > 0) {
                enragedTicks--;
                if (tick % 2 == 0 && alive(vespula)) {
                    for (ServerPlayerEntity p : players()) {
                        if (p.squaredDistanceTo(portal) < 5 * 5) {
                            RaidEffects.hit(vespula, p, 5 + rnd().nextInt(16), null, true);
                        }
                    }
                }
            }
        }

        @Override
        public int bossIncoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            if (boss == portal && damage > 0) {
                if (flying()) {
                    if (enragedTicks <= 0) {
                        warn("Vespula is enraged by the attack on her portal!");
                    }
                    enragedTicks = 6;
                } else if (landedOnce) {
                    com.runecraft.content.systems.CombatAchievementService.flag(attacker,
                            com.runecraft.osrs.content.systems.CombatAchievements.FLAG_VESPULA_LANDED);
                }
            }
            return damage;
        }

        @Override
        protected void onComplete() {
            if (alive(vespula)) {
                say("The portal collapses and Vespula plummets to the ground!");
            }
            cleanup();
        }

        @Override
        public void cleanup() {
            super.cleanup();
            if (alive(vespula)) {
                vespula.discard();
            }
            for (LivingEntity s : living("vespine_soldier")) {
                s.discard();
            }
        }
    }

    /** Ice demon: chop kindling from the saplings and burn it in the braziers to thaw the demon. */
    static final class IceDemon extends RoomLogic {
        private DataBossEntity demon;
        private int heat;
        private int needed;
        private boolean thawed;
        /** OSRS: the braziers are worth up to max(6,000, 10 x the party's total Firemaking levels) points. */
        private int pointsCap;

        IceDemon(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    RaidBuilder.set(world(), at(sx * 7, sz * 7), RaidBuilder.module(RaidsContent.BRAZIER));
                }
            }
            for (int x = -12; x <= 12; x += 8) {
                RaidBuilder.set(world(), at(x, -18), RaidBuilder.module(RaidsContent.SAPLING));
                RaidBuilder.set(world(), at(x, 18), RaidBuilder.module(RaidsContent.SAPLING));
            }
            ice(Blocks.BLUE_ICE.getDefaultState());
        }

        private void ice(net.minecraft.block.BlockState state) {
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    RaidBuilder.pillar(world(), center().add(sx * 2, 0, sz * 2), 0, 3, state);
                }
            }
        }

        @Override
        public void start() {
            needed = 8 + 4 * (raid.partySize() - 1);
            int firemaking = 0;
            for (ServerPlayerEntity p : raid.players()) {
                firemaking += level(p, Skill.FIREMAKING);
            }
            pointsCap = RaidScaling.coxIceDemonPointsCap(firemaking);
            demon = spawnBoss("cox_ice_demon", at(0, 0));
            if (demon != null) {
                demon.setAiDisabled(true);
                anchor(demon);
            }
        }

        @Override
        public void reset() {
            super.reset();
            heat = 0;
            thawed = false;
        }

        @Override
        public boolean bossVulnerable(DataBossEntity boss) {
            return thawed;
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            if (!thawed && alive(demon) && tick % 3 == 0) {
                world().spawnParticles(ParticleTypes.SNOWFLAKE, demon.getX(), demon.getY() + 1.5, demon.getZ(), 8, 1, 1, 1, 0.01);
            }
        }

        @Override
        public boolean useBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
            if (blockId.equals(RaidsContent.SAPLING)) {
                if (cooldown(player, 2)) {
                    return true;
                }
                if (!hasTool(player, "_axe")) {
                    Msg.game(player, "You need an axe to cut the sapling.");
                    return true;
                }
                player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                if (rnd().nextDouble() < 0.4 + level(player, Skill.WOODCUTTING) / 150.0) {
                    Inv.give(player, "cox_kindling", 1);
                    XpService.add(player, Skill.WOODCUTTING, 15);
                    Msg.action(player, "You cut some kindling.");
                }
                return true;
            }
            if (blockId.equals(RaidsContent.BRAZIER)) {
                if (thawed) {
                    Msg.game(player, "The Ice demon is already free.");
                    return true;
                }
                int n = Inv.count(player, "cox_kindling");
                if (n <= 0) {
                    Msg.game(player, "You need some kindling to fuel the brazier.");
                    return true;
                }
                Inv.remove(player, "cox_kindling", n);
                XpService.add(player, Skill.FIREMAKING, n * 25.0);
                // The brazier points are shared out over the kindling the room needs.
                raid.awardActionPoints(player, Math.max(0, Math.min(n, needed - heat)) * pointsCap / Math.max(1, needed));
                heat += n;
                RaidBuilder.set(world(), pos.up(), Blocks.CAMPFIRE.getDefaultState());
                world().spawnParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 20, 0.3, 0.5, 0.3, 0.02);
                say("The braziers warm the room... (" + Math.min(heat, needed) + "/" + needed + ")");
                if (heat >= needed && alive(demon)) {
                    thawed = true;
                    ice(Blocks.AIR.getDefaultState());
                    demon.setAiDisabled(false);
                    unanchor(demon);
                    warn("The ice cracks - the Ice demon breaks free!");
                }
                return true;
            }
            return false;
        }
    }

    /** Thieving: search grubby chests for cavern grubs and feed the scavenger's trough. */
    static final class Thieving extends RoomLogic {
        private final Map<BlockPos, Integer> chests = new HashMap<>();
        private final Set<BlockPos> searched = new HashSet<>();
        private int needed;
        private int deposited;
        /** OSRS: the trough is worth up to max(4,500, 150 per 6 total Thieving levels) points. */
        private int pointsCap;

        Thieving(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            for (int x = -12; x <= 6; x += 6) {
                for (int z = -12; z <= 12; z += 8) {
                    BlockPos p = at(x, z);
                    RaidBuilder.set(world(), p, RaidBuilder.module(RaidsContent.GRUBBY_CHEST));
                    chests.put(p, 0);
                }
            }
            RaidBuilder.set(world(), at(14, 0), RaidBuilder.module(RaidsContent.TROUGH));
            RaidBuilder.set(world(), at(15, 0), Blocks.HAY_BLOCK.getDefaultState());
            refill();
        }

        private void refill() {
            searched.clear();
            for (BlockPos p : chests.keySet()) {
                int r = rnd().nextInt(10);
                chests.put(p, r < 4 ? 1 : r < 6 ? 2 : 0);
            }
        }

        @Override
        public void start() {
            needed = 12 + 6 * (raid.partySize() - 1);
            int thieving = 0;
            for (ServerPlayerEntity p : raid.players()) {
                thieving += level(p, Skill.THIEVING);
            }
            pointsCap = RaidScaling.coxThievingPointsCap(thieving);
        }

        @Override
        public void gameTick(long tick) {
        }

        @Override
        public boolean useBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
            if (blockId.equals(RaidsContent.GRUBBY_CHEST)) {
                if (cooldown(player, 2)) {
                    return true;
                }
                if (searched.contains(pos)) {
                    Msg.game(player, "This chest has already been searched.");
                    return true;
                }
                player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                if (rnd().nextDouble() > 0.55 + level(player, Skill.THIEVING) / 250.0) {
                    Msg.game(player, "You fail to pick the lock, and trigger a trap!");
                    RaidEffects.hit(null, player, 1 + rnd().nextInt(3), null, true);
                    return true;
                }
                searched.add(pos);
                int content = chests.getOrDefault(pos, 0);
                XpService.add(player, Skill.THIEVING, 30);
                if (content == 1) {
                    int n = 1 + rnd().nextInt(3);
                    Inv.give(player, "cavern_grubs", n);
                    Msg.game(player, "You find " + n + " cavern grub" + (n > 1 ? "s" : "") + " in the chest.");
                } else if (content == 2) {
                    Msg.red(player, "A poisonous scorpion leaps out and stings you!");
                    PoisonService.poison(player, 4, ActionManager.gameTick());
                    RaidEffects.hit(null, player, 3, null, true);
                } else {
                    Msg.game(player, "The chest is empty.");
                }
                if (searched.size() >= chests.size()) {
                    refill();
                    say("You hear scuttling as the chests are refilled.");
                }
                return true;
            }
            if (blockId.equals(RaidsContent.TROUGH)) {
                int n = Inv.count(player, "cavern_grubs");
                if (n <= 0) {
                    Msg.game(player, "The scavenger is hungry for cavern grubs. (" + deposited + "/" + needed + ")");
                    return true;
                }
                Inv.remove(player, "cavern_grubs", n);
                raid.awardActionPoints(player, Math.max(0, Math.min(n, needed - deposited)) * pointsCap / Math.max(1, needed));
                deposited += n;
                say(player.getName().getString() + " feeds " + n + " grubs to the scavenger. (" + Math.min(deposited, needed) + "/" + needed + ")");
                if (deposited >= needed) {
                    say("The corrupted scavenger has eaten its fill and falls asleep.");
                    complete();
                }
                return true;
            }
            return false;
        }
    }

    /** Tightrope: kill the deathly mages and rangers, then cross the rope over the chasm. */
    static final class Tightrope extends RoomLogic {
        private boolean crossed;
        private boolean crystalTaken;

        Tightrope(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            BlockPos c = center();
            int y = c.getY();
            int h = RaidBuilder.HALF;
            RaidBuilder.fill(world(), c.getX() - 3, y - 4, c.getZ() - h, c.getX() + 3, y - 4, c.getZ() + h,
                    Blocks.OBSIDIAN.getDefaultState());
            RaidBuilder.fill(world(), c.getX() - 3, y - 3, c.getZ() - h, c.getX() + 3, y - 3, c.getZ() + h,
                    Blocks.MAGMA_BLOCK.getDefaultState());
            RaidBuilder.fill(world(), c.getX() - 3, y - 2, c.getZ() - h, c.getX() + 3, y, c.getZ() + h, Blocks.AIR.getDefaultState());
            RaidBuilder.fill(world(), c.getX() - 3, y, c.getZ(), c.getX() + 3, y, c.getZ(),
                    Blocks.CHAIN.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.X));
            RaidBuilder.set(world(), at(-5, 1), RaidBuilder.module(RaidsContent.TIGHTROPE));
            RaidBuilder.fill(world(), c.getX() + 8, y + 1, c.getZ() - 9, c.getX() + 12, y + 1, c.getZ() + 9,
                    Blocks.PURPUR_SLAB.getDefaultState());
        }

        @Override
        public void start() {
            int pairs = raid.partySize() > 2 ? 2 : 1;
            for (int i = 0; i < pairs; i++) {
                spawnMob("deathly_ranger", at(10, -7 + i * 3).up(), true);
                spawnMob("deathly_mage", at(10, 7 - i * 3).up(), true);
            }
        }

        @Override
        public void gameTick(long tick) {
            for (ServerPlayerEntity p : raid.activePlayers()) {
                double dx = p.getX() - (center().getX() + 0.5);
                double dz = p.getZ() - (center().getZ() + 0.5);
                if (Math.abs(dx) > RaidBuilder.HALF + 0.5 || Math.abs(dz) > RaidBuilder.HALF + 0.5) {
                    continue;
                }
                if (p.getY() < center().getY() - 0.5 && Math.abs(dx) < 5) {
                    raid.teleport(p, at(-6, 0));
                    RaidEffects.hit(null, p, 5, null, true);
                    Msg.red(p, "You fall into the chasm and climb back out.");
                } else if (p.getX() > center().getX() + 4 && inside(p.getPos())) {
                    crossed = true;
                    takeCrystal(p);
                }
            }
            if (crossed && allDead()) {
                complete();
            }
        }

        /** OSRS: the first player across claims the crystal on the far side, worth 2,000 points. */
        private void takeCrystal(ServerPlayerEntity p) {
            if (!crystalTaken) {
                crystalTaken = true;
                raid.awardActionPoints(p, RaidScaling.COX_TIGHTROPE_POINTS);
                Msg.game(p, "You claim the crystal on the far side of the chasm.");
            }
        }

        @Override
        public void reset() {
            super.reset();
            crossed = false;
        }

        @Override
        protected void onComplete() {
            BlockPos c = center();
            RaidBuilder.fill(world(), c.getX() - 3, c.getY(), c.getZ() - RaidBuilder.HALF, c.getX() + 3, c.getY(),
                    c.getZ() + RaidBuilder.HALF, RaidBuilder.palette(def.style()).floor());
            say("A bridge forms across the chasm.");
            cleanup();
        }

        @Override
        public boolean useBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
            if (!blockId.equals(RaidsContent.TIGHTROPE)) {
                return false;
            }
            if (cooldown(player, 3)) {
                return true;
            }
            if (rnd().nextDouble() < 0.6 + level(player, Skill.AGILITY) / 200.0) {
                raid.teleport(player, at(5, 0));
                XpService.add(player, Skill.AGILITY, 40);
                Msg.game(player, "You carefully cross the tightrope.");
                crossed = true;
                takeCrystal(player);
            } else {
                raid.teleport(player, at(0, 3).down(3));
                Msg.red(player, "You lose your balance!");
            }
            return true;
        }
    }

    /**
     * Crystal crabs, a simplified take on the OSRS light-beam puzzle: four dark crystals stand in the
     * corners and crystal crabs scuttle about. Striking a crab makes it shimmer for a few seconds (and
     * knocks it along); a shimmering crab that reaches a dark crystal focuses its light into it. Lighting
     * every crystal finishes the room. The room is worth 1,600 points (OSRS), 400 to whoever lights each crystal.
     */
    static final class CrystalCrabs extends RoomLogic {
        private static final int SHIMMER_TICKS = 8;
        private final List<BlockPos> crystals = new ArrayList<>();
        private final Set<BlockPos> lit = new HashSet<>();
        private final Map<LivingEntity, Integer> shimmer = new HashMap<>();
        private final Map<LivingEntity, java.util.UUID> striker = new HashMap<>();
        private int wanted;

        CrystalCrabs(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            crystals.clear();
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    BlockPos p = at(sx * 13, sz * 13);
                    crystals.add(p);
                    RaidBuilder.set(world(), p, Blocks.TINTED_GLASS.getDefaultState());
                    RaidBuilder.set(world(), p.up(), Blocks.TINTED_GLASS.getDefaultState());
                }
            }
            RaidBuilder.set(world(), center().up(RaidBuilder.HEIGHT), Blocks.SEA_LANTERN.getDefaultState());
        }

        @Override
        public void start() {
            lit.clear();
            shimmer.clear();
            striker.clear();
            wanted = Math.min(8, 4 + raid.partySize() / 2);
            for (int i = 0; i < wanted; i++) {
                spawnCrab();
            }
        }

        private void spawnCrab() {
            spawnMob("cox_crystal_crab", at(rnd().nextInt(13) - 6, rnd().nextInt(13) - 6), true);
        }

        @Override
        public void onCreatureHit(ServerPlayerEntity player, LivingEntity target, int damage) {
            if (!living("cox_crystal_crab").contains(target)) {
                return;
            }
            // The crabs cannot be killed: every strike restores them and makes them shimmer.
            raid.setCurrentHitpoints(target, raid.maxHitpoints(target));
            shimmer.put(target, SHIMMER_TICKS);
            striker.put(target, player.getUuid());
            world().spawnParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + 0.6, target.getZ(), 12, 0.4, 0.3, 0.4, 0.05);
            Msg.action(player, "The crystal crab shimmers with light.");
        }

        @Override
        public void gameTick(long tick) {
            List<LivingEntity> crabs = living("cox_crystal_crab");
            if (crabs.size() < wanted && tick % 10 == 0) {
                spawnCrab();
            }
            for (LivingEntity crab : crabs) {
                Integer left = shimmer.get(crab);
                if (left == null || left <= 0) {
                    continue;
                }
                shimmer.put(crab, left - 1);
                world().spawnParticles(ParticleTypes.GLOW, crab.getX(), crab.getY() + 0.5, crab.getZ(), 3, 0.3, 0.2, 0.3, 0);
                for (BlockPos c : crystals) {
                    if (lit.contains(c) || RaidEffects.horizontalDistSq(crab.getPos(), Vec3d.ofBottomCenter(c)) > 2.5 * 2.5) {
                        continue;
                    }
                    lit.add(c);
                    shimmer.remove(crab);
                    RaidBuilder.set(world(), c, Blocks.SEA_LANTERN.getDefaultState());
                    RaidBuilder.set(world(), c.up(), Blocks.SEA_LANTERN.getDefaultState());
                    ServerPlayerEntity who = world().getServer().getPlayerManager().getPlayer(striker.get(crab));
                    if (who != null) {
                        raid.awardActionPoints(who, RaidScaling.COX_CRABS_POINTS / crystals.size());
                    }
                    say("A crystal flares with light! (" + lit.size() + "/" + crystals.size() + ")");
                    break;
                }
            }
            if (!crystals.isEmpty() && lit.size() >= crystals.size()) {
                say("Every crystal shines - the way onward opens.");
                complete();
            }
        }

        @Override
        public void reset() {
            super.reset();
            for (BlockPos c : crystals) {
                RaidBuilder.set(world(), c, Blocks.TINTED_GLASS.getDefaultState());
                RaidBuilder.set(world(), c.up(), Blocks.TINTED_GLASS.getDefaultState());
            }
            lit.clear();
        }
    }

    /**
     * The Great Olm: both claws must die each phase; the head is only vulnerable once the claws of
     * the final phase are dead. Each phase has a special (acid, crystals or flames) on top of
     * falling crystals and the left claw's lightning, crystal bursts and teleport portals.
     */
    static final class Olm extends RoomLogic {
        private static final String[] SPECIALS = {"acid", "crystal", "flame"};
        private DataBossEntity head;
        private DataBossEntity left;
        private DataBossEntity right;
        private int phase;
        private int phases;
        private String special;
        private boolean headExposed;
        private int clawSpecial;
        /** Game tick one claw died while the other lived (-1 when both are alive or both dead). */
        private long clawDownTick = -1;
        /** Game ticks the left (melee) claw stays clenched. */
        private int clenchTicks;
        /** Healing pools: a player standing in one heals the Olm. */
        private final List<Vec3d> pools = new ArrayList<>();
        private int poolTicks;

        Olm(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            BlockPos c = center();
            int y = c.getY();
            RaidBuilder.fill(world(), c.getX() + 18, y + 1, c.getZ() - RaidBuilder.HALF, c.getX() + RaidBuilder.HALF, y + RaidBuilder.HEIGHT,
                    c.getZ() + RaidBuilder.HALF, Blocks.BLACKSTONE.getDefaultState());
            RaidBuilder.fill(world(), c.getX() + 13, y, c.getZ() - 12, c.getX() + 17, y, c.getZ() + 12,
                    Blocks.POLISHED_BLACKSTONE.getDefaultState());
            for (int i = 0; i < 14; i++) {
                BlockPos p = c.add(18, 1 + rnd().nextInt(RaidBuilder.HEIGHT - 1), rnd().nextInt(RaidBuilder.HALF * 2) - RaidBuilder.HALF);
                RaidBuilder.set(world(), p, Blocks.AMETHYST_BLOCK.getDefaultState());
            }
        }

        @Override
        public void start() {
            phases = RaidScaling.olmPhases(raid.partySize());
            phase = 0;
            special = null;
            pools.clear();
            headExposed = false;
            head = spawnBoss("great_olm", at(15, 0));
            anchor(head);
            nextPhase();
        }

        private void nextPhase() {
            phase++;
            left = spawnBoss("olm_left_claw", at(15, 9));
            right = spawnBoss("olm_right_claw", at(15, -9));
            anchor(left);
            anchor(right);
            clawDownTick = -1;
            clenchTicks = 0;
            // OSRS: a phase never repeats the previous phase's type.
            String previous = special;
            do {
                special = SPECIALS[rnd().nextInt(SPECIALS.length)];
            } while (special.equals(previous));
            warn("The Great Olm rises with the power of " + special + ". (Phase " + phase + "/" + phases + ")");
        }

        @Override
        public void reset() {
            super.reset();
            head = null;
            left = null;
            right = null;
        }

        @Override
        public boolean bossVulnerable(DataBossEntity boss) {
            return boss != head || headExposed;
        }

        /**
         * OSRS: a heavy melee hit on the left (melee) claw, at least 1/20 of its health, makes it clench:
         * it takes no damage and the Olm stops its left-claw specials until it lets go after a while or the
         * right claw is disabled. The claw never clenches in the final phase.
         */
        @Override
        public int bossIncoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            if (boss != left) {
                return damage;
            }
            if (clenchTicks > 0) {
                Msg.game(attacker, "The Great Olm's left claw is clenched shut.");
                return 0;
            }
            boolean melee = type == AttackType.STAB || type == AttackType.SLASH || type == AttackType.CRUSH;
            if (melee && phase < phases && alive(right) && damage >= RaidScaling.olmClenchThreshold(raid.maxHitpoints(left))) {
                clenchTicks = RaidScaling.OLM_CLENCH_TICKS;
                warn("The Great Olm's left claw clenches to protect itself!");
            }
            return damage;
        }

        /**
         * OSRS: both claws must go down close together; if one stays dead too long while the other lives,
         * the dead claw regrows at full health.
         */
        private void checkClaws(long tick) {
            if (clenchTicks > 0) {
                clenchTicks--;
                if (!alive(right) || clenchTicks == 0) {
                    clenchTicks = 0;
                    if (alive(left)) {
                        say("The Great Olm's left claw unclenches.");
                    }
                }
            }
            boolean l = alive(left);
            boolean r = alive(right);
            if (l == r) {
                clawDownTick = -1;
                return;
            }
            if (clawDownTick < 0) {
                clawDownTick = tick;
                warn("One of the Great Olm's claws is down - finish the other before it regrows!");
                return;
            }
            if (tick - clawDownTick > RaidScaling.OLM_CLAW_REVIVE_TICKS) {
                if (!l) {
                    left = spawnBoss("olm_left_claw", at(15, 9));
                    anchor(left);
                } else {
                    right = spawnBoss("olm_right_claw", at(15, -9));
                    anchor(right);
                }
                clawDownTick = -1;
                warn("The Great Olm's " + (l ? "right" : "left") + " claw regrows! Both claws must fall together.");
            }
        }

        @Override
        public void gameTick(long tick) {
            if (head == null) {
                return;
            }
            if (!alive(head)) {
                complete();
                return;
            }
            autoAttack(head, tick, 4);
            List<ServerPlayerEntity> players = players();
            if (players.isEmpty()) {
                return;
            }
            // Falling crystals.
            if (tick % (headExposed ? 3 : 5) == 0) {
                ServerPlayerEntity p = players.get(rnd().nextInt(players.size()));
                Vec3d at = p.getPos().add(rnd().nextDouble() * 2 - 1, 0, rnd().nextDouble() * 2 - 1);
                RaidEffects.strike(world(), at, 1.3, 20, 3, head, ParticleTypes.END_ROD);
            }
            // Phase special.
            if (tick % 12 == 0) {
                switch (special) {
                    case "acid" -> {
                        say("The Great Olm sprays acid!");
                        for (ServerPlayerEntity p : players) {
                            Mechanics.addHazard(world(), p.getPos(), 6, 12, head);
                        }
                        ServerPlayerEntity drip = players.get(rnd().nextInt(players.size()));
                        PoisonService.poison(drip, 4, ActionManager.gameTick());
                        Msg.red(drip, "The Great Olm's acid drips onto you!");
                    }
                    case "crystal" -> {
                        say("The Great Olm fires crystal bombs!");
                        for (int i = 0; i < 2 + players.size(); i++) {
                            RaidEffects.strike(world(), vec(rnd().nextInt(26) - 16, rnd().nextInt(30) - 15), 3.0, 40, 6, head,
                                    ParticleTypes.END_ROD);
                        }
                    }
                    default -> {
                        ServerPlayerEntity p = players.get(rnd().nextInt(players.size()));
                        say("The Great Olm breathes a wall of flame!");
                        for (int x = -18; x <= 12; x += 2) {
                            RaidEffects.strike(world(), new Vec3d(center().getX() + 0.5 + x, p.getY(), p.getZ()), 1.3, 30, 4, head,
                                    ParticleTypes.FLAME);
                        }
                    }
                }
            }
            // Left claw specials.
            // Prayer spheres: every so often the head sends each player an orb to pray against.
            if (tick % 16 == 8) {
                spheres(players);
            }
            tickPools();
            checkClaws(tick);
            if (alive(left) && clenchTicks == 0 && tick % 20 == 10) {
                switch (clawSpecial++ % 4) {
                    case 0 -> {
                        say("The Great Olm's left claw crackles - lightning strikes!");
                        for (int lane = 0; lane < 2; lane++) {
                            int z = rnd().nextInt(32) - 16;
                            for (int x = -18; x <= 12; x += 2) {
                                RaidEffects.strike(world(), vec(x, z), 1.5, 20, 3, left, ParticleTypes.ELECTRIC_SPARK);
                            }
                        }
                    }
                    case 1 -> {
                        say("Crystals burst from the ground!");
                        for (ServerPlayerEntity p : players) {
                            RaidEffects.strike(world(), p.getPos(), 1.5, 30, 3, left, ParticleTypes.END_ROD);
                        }
                    }
                    case 2 -> portals(players);
                    default -> healingPools(players);
                }
            }
            if (!alive(left) && !alive(right)) {
                if (phase < phases) {
                    nextPhase();
                } else if (!headExposed) {
                    headExposed = true;
                    warn("The Great Olm's claws are dead - its head is now vulnerable!");
                }
            }
        }

        /**
         * Spheres: a red (melee), green (ranged) or blue (magic) orb flies at each player; without the
         * matching protection prayer it hits hard and halves their prayer points.
         */
        private void spheres(List<ServerPlayerEntity> players) {
            AttackType[] styles = {AttackType.CRUSH, AttackType.RANGED, AttackType.MAGIC};
            String[] names = {"red", "green", "blue"};
            for (ServerPlayerEntity p : players) {
                int s = rnd().nextInt(3);
                Msg.red(p, "The Great Olm fires a " + names[s] + " sphere at you!");
                world().spawnParticles(s == 0 ? ParticleTypes.FLAME : s == 1 ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.SOUL_FIRE_FLAME,
                        head.getX(), head.getY() + 1.5, head.getZ(), 10, 0.4, 0.4, 0.4, 0.02);
                DataBossEntity source = head;
                RaidEffects.later(3, () -> {
                    if (!p.isAlive() || !alive(source)) {
                        return;
                    }
                    if (!RaidEffects.protectedFrom(p, styles[s])) {
                        var data = com.runecraft.player.Players.data(p);
                        data.setPrayerPoints(data.prayerPoints() / 2);
                        Msg.red(p, "The sphere drains your prayer!");
                    }
                    RaidEffects.hit(source, p, rnd().nextInt(28), styles[s], false); // approx max hit
                });
            }
        }

        /** Healing pools: marked pools appear around the room; each player standing in one heals the Olm. */
        private void healingPools(List<ServerPlayerEntity> players) {
            say("The Great Olm creates healing pools - stay out of them!");
            pools.clear();
            for (int i = 0; i < 3 + players.size(); i++) {
                pools.add(vec(rnd().nextInt(26) - 16, rnd().nextInt(30) - 15));
            }
            poolTicks = 20;
        }

        private void tickPools() {
            if (poolTicks <= 0) {
                pools.clear();
                return;
            }
            poolTicks--;
            for (Vec3d pool : pools) {
                world().spawnParticles(ParticleTypes.HEART, pool.x, pool.y + 0.2, pool.z, 2, 0.6, 0.05, 0.6, 0);
                world().spawnParticles(ParticleTypes.GLOW, pool.x, pool.y + 0.1, pool.z, 6, 0.7, 0.02, 0.7, 0);
            }
            int standing = 0;
            for (ServerPlayerEntity p : players()) {
                for (Vec3d pool : pools) {
                    if (RaidEffects.horizontalDistSq(p.getPos(), pool) <= 1.2 * 1.2) {
                        standing++;
                        break;
                    }
                }
            }
            if (standing > 0) {
                // approx: each player in a pool heals the Olm 10 hitpoints per game tick.
                for (DataBossEntity part : new DataBossEntity[]{head, left, right}) {
                    if (alive(part) && (part != head || headExposed)) {
                        raid.heal(part, 10 * standing);
                    }
                }
            }
        }

        /** Teleport portals: pairs of players swap places; a lone player is blasted. */
        private void portals(List<ServerPlayerEntity> players) {
            say("Portals open beneath you!");
            List<ServerPlayerEntity> list = new ArrayList<>(players);
            java.util.Collections.shuffle(list, rnd());
            for (int i = 0; i + 1 < list.size(); i += 2) {
                ServerPlayerEntity a = list.get(i);
                ServerPlayerEntity b = list.get(i + 1);
                RaidEffects.later(4, () -> {
                    if (a.isAlive() && b.isAlive()) {
                        Vec3d pa = a.getPos();
                        a.requestTeleport(b.getX(), b.getY(), b.getZ());
                        b.requestTeleport(pa.x, pa.y, pa.z);
                    }
                });
            }
            if (list.size() % 2 == 1) {
                ServerPlayerEntity p = list.get(list.size() - 1);
                RaidEffects.strike(world(), p.getPos(), 1.4, 25, 4, left, ParticleTypes.PORTAL);
            }
        }
    }
}
