package com.runecraft.content.raids;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.PoisonService;
import com.runecraft.content.systems.CombatAchievementService;
import com.runecraft.content.systems.SysCombat;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.raids.CoxRules;
import com.runecraft.osrs.content.raids.CoxSupplies;
import com.runecraft.osrs.content.raids.RaidScaling;
import com.runecraft.osrs.content.raids.RaidsContent;
import com.runecraft.osrs.content.raids.RoomDef;
import com.runecraft.osrs.content.systems.CombatAchievements;
import com.runecraft.player.Players;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.util.StackData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Chambers of Xeric rooms (round 33 rebuild after the OSRS wiki's "Chambers of Xeric/Strategies", "Great Olm" and the
 * room creatures' pages; numbers in {@link CoxRules}). Every room keeps its own tick counter, so its timers follow the
 * raid clock ({@link RaidEffects#now}) whether the server or the self-test ({@link CoxTobWalk}) drives it.
 */
final class CoxRooms {
    private CoxRooms() {
    }

    /** Registers the combat hooks the rooms need (called once from {@link CoxSupplyService#init}). */
    static void init() {
        CombatHooks.addModifier(new CombatHooks.PlayerAttackModifier() {
            /**
             * Enraged Tekton's melee defence bonuses rise from 155/165/105 to 280/290/180 (OSRS wiki): the defence roll grows
             * by (enraged + 64) / (normal + 64), the same as the attack roll shrinking by its inverse.
             */
            @Override
            public double accuracy(ServerPlayer player, LivingEntity target, AttackType type) {
                if (target == null || !melee(type) || !"tekton".equals(RaidInstance.creatureId(target))) {
                    return 1.0;
                }
                RaidInstance raid = RaidManager.at(target);
                int room = raid == null ? -1 : raid.roomAt(target.position());
                if (room >= 0 && raid.logic(room) instanceof Tekton t && t.enraged()) {
                    return Tekton.enragedAccuracy(type);
                }
                return 1.0;
            }

            @Override
            public int afterRoll(ServerPlayer player, LivingEntity target, AttackType type, int damage) {
                // OSRS: Vasa's glowing crystals take 66% less damage from magic (and none from ranged: MonsterDef immunity).
                if (target != null && type == AttackType.MAGIC && "vasa_crystal".equals(RaidInstance.creatureId(target))
                        && RaidManager.at(target) != null) {
                    return damage * 34 / 100;
                }
                // OSRS: a flying vespine soldier is out of melee reach (halberds aside); a grounded one only takes melee.
                if (target != null && "vespine_soldier".equals(RaidInstance.creatureId(target))) {
                    RaidInstance raid = RaidManager.at(target);
                    int room = raid == null ? -1 : raid.roomAt(target.position());
                    if (room >= 0 && raid.logic(room) instanceof Vespula v && v.soldierSlot(target) >= 0) {
                        boolean grounded = v.grounded(target);
                        boolean halberd = Inv.idOf(player.getMainHandItem()).contains("halberd");
                        if (grounded ? !melee(type) : melee(type) && !halberd) {
                            return 0;
                        }
                    }
                }
                return damage;
            }

            @Override
            public void afterHit(ServerPlayer player, LivingEntity target, AttackType type, int dealt) {
                if (target == null || !"cox_crystal_crab".equals(RaidInstance.creatureId(target))) {
                    return;
                }
                RaidInstance raid = RaidManager.at(target);
                int room = raid == null ? -1 : raid.roomAt(target.position());
                if (room >= 0 && raid.logic(room) instanceof CrystalCrabs crabs) {
                    crabs.struck(player, target, type);
                }
            }
        });
    }

    // ================================================================== shared helpers

    /** Marks a tool handed out by a room's tool spawn: it belongs to the Chambers and is taken back when its holder leaves. */
    static final String RAID_TOOL_TAG = "RunecraftCoxTool";

    /**
     * Hands out a room's tool spawn (OSRS: permanent item spawns), one of each the player lacks. The tools are marked
     * ({@link #RAID_TOOL_TAG}) so whoever carries them out of the raid loses them again ({@link #stripTools}).
     */
    static void toolSpawn(ServerPlayer p, String... items) {
        List<String> got = new ArrayList<>();
        for (String item : items) {
            if (!Inv.has(p, item, 1)) {
                ItemStack stack = Inv.stack(item, 1);
                StackData.update(stack, t -> t.putBoolean(RAID_TOOL_TAG, true));
                Inv.give(p, stack);
                got.add(item.replace('_', ' '));
            }
        }
        Msg.game(p, got.isEmpty() ? "You already have what you need from here." : "You take: " + String.join(", ", got) + ".");
    }

    /** Takes back every tool from a room's tool spawn that a leaving player carries; returns how many stacks went. */
    static int stripTools(ServerPlayer p) {
        int n = 0;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (StackData.contains(inv.getItem(i), RAID_TOOL_TAG)) {
                inv.setItem(i, ItemStack.EMPTY);
                n++;
            }
        }
        if (StackData.contains(p.containerMenu.getCarried(), RAID_TOOL_TAG)) {
            p.containerMenu.setCarried(ItemStack.EMPTY);
            n++;
        }
        return n;
    }

    /** The team's average current level in a skill. */
    static int averageLevel(RaidInstance raid, Skill skill) {
        int total = 0;
        int n = 0;
        for (ServerPlayer p : raid.players()) {
            total += RoomLogic.level(p, skill);
            n++;
        }
        return n == 0 ? 99 : total / n;
    }

    /**
     * A telegraphed area hit with a damage range: the area is marked now and every tick until it lands, then every player
     * within {@code radius} takes {@code min}-{@code max} (raid scaling applies; prayer doesn't help).
     */
    static void zone(ServerLevel world, Vec3 pos, double radius, int min, int max, int delay, LivingEntity source, ParticleOptions warn) {
        int n = (int) Math.max(4, radius * 8);
        world.sendParticles(warn, pos.x, pos.y + 0.15, pos.z, n, radius * 0.5, 0.02, radius * 0.5, 0);
        for (int t = 1; t < delay; t++) {
            RaidEffects.later(t, () -> world.sendParticles(warn, pos.x, pos.y + 0.15, pos.z, n, radius * 0.5, 0.02, radius * 0.5, 0));
        }
        RaidEffects.later(delay, () -> {
            world.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 0.5, pos.z, 1, 0, 0, 0, 0);
            for (ServerPlayer p : world.getPlayers(pl -> pl.isAlive() && !pl.isSpectator() && !pl.isCreative()
                    && RaidEffects.horizontalDistSq(pl.position(), pos) <= radius * radius && Math.abs(pl.getY() - pos.y) < 3)) {
                RaidEffects.hit(source, p, min + p.getRandom().nextInt(Math.max(1, max - min + 1)), null, true);
            }
        });
    }

    /** A ground pool (acid, poison) that hurts anyone standing in it every tick until it dries. */
    record Pool(Vec3 pos, long until, int min, int max, int poison) {
    }

    /** Ticks a room's pools: dried ones go, players standing in the others are hurt (and poisoned). */
    static void tickPools(RoomLogic room, List<Pool> pools, long now, LivingEntity source, ParticleOptions look) {
        pools.removeIf(p -> now >= p.until());
        for (Pool pool : pools) {
            if (now % 2 == 0) {
                room.world().sendParticles(look, pool.pos().x, pool.pos().y + 0.1, pool.pos().z, 3, 0.35, 0.02, 0.35, 0);
            }
        }
        for (ServerPlayer p : room.players()) {
            for (Pool pool : pools) {
                if (RaidEffects.horizontalDistSq(p.position(), pool.pos()) < 0.8 * 0.8) {
                    RaidEffects.hit(source, p, pool.min() + room.rnd().nextInt(Math.max(1, pool.max() - pool.min() + 1)), null, true);
                    if (pool.poison() > 0) {
                        PoisonService.poison(p, pool.poison(), ActionManager.gameTick());
                    }
                    break;
                }
            }
        }
    }

    /** Drops an item stack on the floor of a room (OSRS: CoX room rewards lie on the ground for anyone to take). */
    static void dropItem(ServerLevel world, Vec3 at, String item, int count) {
        net.minecraft.world.item.ItemStack stack = Inv.stack(item, count);
        if (!stack.isEmpty()) {
            Inv.splitStacks(stack, s -> world.addFreshEntity(new ItemEntity(world, at.x, at.y + 0.3, at.z, s)));
        }
    }

    static boolean melee(AttackType type) {
        return type == AttackType.STAB || type == AttackType.SLASH || type == AttackType.CRUSH;
    }

    private static final Skill[] DRAINABLE = {Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.MAGIC, Skill.RANGED};

    /**
     * Drained stats of CoX creatures come back at each creature's own rate (Mod Ash: a level every 8 ticks for Stone
     * Guardians, 15 for Tekton, ...; {@link CoxRules#statRegenTicks}). The server already gives every drain back one level
     * each 100 ticks ({@link SysCombat#gameTick}); this adds the difference (called each raid tick for the current room).
     */
    static void regenerateStats(List<LivingEntity> creatures, long tick) {
        for (LivingEntity e : creatures) {
            if (e.isAlive() && CoxRules.extraRegenDue(CoxRules.statRegenTicks(RaidInstance.creatureId(e)), tick)) {
                for (Skill s : DRAINABLE) {
                    restoreDrain(e, s, 1);
                }
            }
        }
    }

    /** Gives a creature back up to {@code levels} of a drained stat; returns the levels restored. */
    static int restoreDrain(LivingEntity e, Skill skill, int levels) {
        int drained = SysCombat.baseStatLevel(e, skill) - SysCombat.drainedStatLevel(e, skill);
        int back = Math.min(drained, levels);
        if (back <= 0) {
            return 0;
        }
        SysCombat.Drain d = SysCombat.drain(e);
        switch (skill) {
            case ATTACK -> d.attack = Math.max(0, d.attack - back);
            case STRENGTH -> d.strength = Math.max(0, d.strength - back);
            case MAGIC -> d.magic = Math.max(0, d.magic - back);
            case RANGED -> d.ranged = Math.max(0, d.ranged - back);
            default -> d.defence = Math.max(0, d.defence - back);
        }
        return back;
    }

    // ================================================================== Tekton

    /**
     * Tekton (OSRS): melee only (immune to ranged, 20% from magic). After 10-14 attacks, or when nobody is in reach when he
     * scans, he walks back to his anvil (stomping anyone left under him), repairs 1.33% of his health per set of sparks and
     * throws two sparks per player (3x3, 10-20); after five sets he gets up enraged (59 max hit instead of 52, melee defence
     * bonuses 280/290/180 instead of 155/165/105) for about five attacks.
     */
    static final class Tekton extends RoomLogic implements CoxTobWalk.Walkable {
        /** Game ticks Tekton may take walking back to the anvil before he is put there. // approx */
        private static final int RETURN_TICKS = 15;
        private DataBossEntity tekton;
        private Vec3 anvil;
        private int t;
        private int attacksLeft;
        private boolean atAnvil;
        private int returning;
        private int sparkSets;
        private int sparkTimer;
        private int enragedAttacks;
        private int lonely;
        private int anvilVisits;
        private int stomps;
        private int enrages;

        /** The attack roll factor against enraged Tekton for a melee style. */
        static double enragedAccuracy(AttackType type) {
            int i = type == AttackType.STAB ? 0 : type == AttackType.SLASH ? 1 : 2;
            return (CoxRules.TEKTON_DEFENCE[i] + 64.0) / (CoxRules.TEKTON_ENRAGED_DEFENCE[i] + 64.0);
        }

        boolean enraged() {
            return enragedAttacks > 0;
        }

        Tekton(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            RaidBuilder.set(world(), at(13, 0), Blocks.ANVIL.defaultBlockState());
            RaidBuilder.set(world(), at(13, -2), Blocks.LAVA_CAULDRON.defaultBlockState());
            RaidBuilder.set(world(), at(13, 2), Blocks.LAVA_CAULDRON.defaultBlockState());
        }

        @Override
        public void start() {
            tekton = spawnBoss("tekton", at(8, 0));
            anvil = vec(11, 0);
            attacksLeft = CoxRules.tektonAttacksBeforeAnvil(rnd());
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            t++;
            if (!alive(tekton)) {
                return;
            }
            if (returning > 0) {
                // OSRS: anyone left under Tekton as he walks back to the anvil takes rapid stomp damage.
                for (ServerPlayer p : players()) {
                    if (RaidEffects.horizontalDistSq(p.position(), tekton.position()) < 1.2 * 1.2) {
                        stomps++;
                        RaidEffects.hit(tekton, p, rnd().nextInt(CoxRules.TEKTON_STOMP_MAX + 1), null, true);
                    }
                }
                if (--returning == 0 || RaidEffects.horizontalDistSq(tekton.position(), anvil) < 1.5 * 1.5) {
                    returning = 0;
                    reachAnvil();
                } else {
                    tekton.getNavigation().moveTo(anvil.x, anvil.y, anvil.z, 1.0);
                }
                return;
            }
            if (atAnvil) {
                tekton.getNavigation().stop();
                if (++sparkTimer < CoxRules.TEKTON_SPARK_INTERVAL) {
                    return;
                }
                sparkTimer = 0;
                sparkSets++;
                raid.heal(tekton, Math.max(1, raid.maxHitpoints(tekton) * CoxRules.TEKTON_REPAIR_PER_10K / 10_000));
                world().sendParticles(ParticleTypes.LAVA, anvil.x + 2, anvil.y + 1, anvil.z, 8, 0.4, 0.3, 0.4, 0);
                for (ServerPlayer p : players()) {
                    for (int i = 0; i < CoxRules.TEKTON_SPARKS_PER_PLAYER; i++) {
                        Vec3 at = p.position().add(rnd().nextInt(3) - 1, 0, rnd().nextInt(3) - 1);
                        zone(world(), at, 1.5, CoxRules.TEKTON_SPARK_MIN, CoxRules.TEKTON_SPARK_MAX, 3, tekton, ParticleTypes.LAVA);
                    }
                }
                if (sparkSets >= CoxRules.TEKTON_SPARK_SETS) {
                    atAnvil = false;
                    unanchor(tekton);
                    enragedAttacks = CoxRules.TEKTON_ENRAGED_ATTACKS;
                    enrages++;
                    attacksLeft = CoxRules.tektonAttacksBeforeAnvil(rnd());
                    lonely = 0;
                    warn("Tekton's hammer glows red hot!");
                }
                return;
            }
            if (enragedAttacks > 0 && t % 2 == 0) {
                world().sendParticles(ParticleTypes.FLAME, tekton.getX(), tekton.getY() + 2, tekton.getZ(), 4, 0.6, 0.6, 0.6, 0.01);
            }
            // He scans for someone in reach every attack cycle; with nobody there for a while he goes back to the anvil.
            boolean inReach = false;
            for (ServerPlayer p : players()) {
                inReach |= p.distanceToSqr(tekton) < 4 * 4;
            }
            lonely = inReach ? 0 : lonely + 1;
            if (attacksLeft <= 0 || lonely >= 4 * CoxRules.TEKTON_ATTACK_SPEED) {
                goToAnvil();
            }
        }

        private void goToAnvil() {
            returning = RETURN_TICKS;
            enragedAttacks = 0;
            anvilVisits++;
            say("Tekton retreats to his anvil to sharpen his hammer.");
            CombatAchievementService.countNear(tekton, CombatAchievements.COUNT_TEKTON_ANVIL);
            tekton.setTarget(null);
            tekton.getNavigation().moveTo(anvil.x, anvil.y, anvil.z, 1.0);
        }

        private void reachAnvil() {
            atAnvil = true;
            sparkSets = 0;
            sparkTimer = 0;
            move(tekton, anvil);
            anchor(tekton);
        }

        /** OSRS: Tekton is immune to Ranged and takes 80% less Magic damage. */
        @Override
        public int bossIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (type == AttackType.RANGED) {
                Msg.game(attacker, "Tekton is immune to ranged attacks.");
                return 0;
            }
            return type == AttackType.MAGIC ? damage / 5 : damage;
        }

        @Override
        public int bossOutgoing(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
            attacksLeft--;
            if (enragedAttacks > 0) {
                // Enraged Tekton (fresh from the anvil) has a 59 max hit instead of 52 (OSRS).
                enragedAttacks--;
                return (int) Math.round(damage * (double) CoxRules.TEKTON_ENRAGED_MAX_HIT / CoxRules.TEKTON_MAX_HIT);
            }
            return damage;
        }

        @Override
        public void bossDeath(DataBossEntity boss, ServerPlayer killer) {
            // OSRS: a rare onyx, likelier for a tougher (bigger party) Tekton and three times rarer in a small Challenge Mode.
            int rate = CoxRules.tektonOnyxRate(raid.partySize(), raid.mode() == com.runecraft.osrs.content.raids.RaidMode.CHALLENGE);
            if (killer != null && rnd().nextInt(rate) == 0) {
                Inv.give(killer, "onyx", 1);
                raid.say(killer.getName().getString() + " finds an onyx among Tekton's remains!", net.minecraft.ChatFormatting.LIGHT_PURPLE);
            }
        }

        @Override
        public String walk(CoxTobWalk w) {
            w.expect(alive(tekton), "Tekton did not spawn");
            w.expect(w.hit(tekton, 20, AttackType.RANGED) == 0, "Tekton took ranged damage (he is immune)");
            // Nobody stands next to him, so he soon goes back to the anvil and throws sparks.
            w.moveTo(at(-16, 0));
            w.until(() -> anvilVisits > 0, 40, "Tekton never went back to his anvil");
            // Standing under him as he walks back gets you stomped.
            if (returning > 0) {
                w.moveTo(tekton.position());
                w.tick(1);
                w.expect(stomps > 0 || returning == 0, "Tekton walked back over the player without stomping");
                w.moveTo(at(-16, 0));
            }
            w.until(() -> atAnvil, 30, "Tekton never reached his anvil");
            w.until(() -> !atAnvil, 40, "Tekton never left his anvil");
            w.expect(enraged() && enrages > 0, "Tekton did not get up enraged");
            w.expect(enragedAccuracy(AttackType.CRUSH) < 0.7, "enraged Tekton's crush defence did not rise");
            // Drained stats come back quickly in the Chambers (Mod Ash: a level every 15 ticks for Tekton).
            SysCombat.drainDefence(tekton, 20);
            int drainedTo = SysCombat.drainedStatLevel(tekton, Skill.DEFENCE);
            w.tick(45);
            w.expect(SysCombat.drainedStatLevel(tekton, Skill.DEFENCE) > drainedTo, "Tekton's drained Defence did not regenerate");
            w.kill(tekton, AttackType.CRUSH, "Tekton");
            return "anvil visits " + anvilVisits + ", " + enrages + " enrage(s), " + stomps + " stomp(s)";
        }
    }

    // ================================================================== Vasa Nistirio

    /**
     * Vasa Nistirio (OSRS wiki). He opens with his teleport: half the team (the player when solo) is pulled next to him,
     * stunned and stripped of prayers, and the barrage hits everyone beside him for the stunned players' summed hitpoints
     * minus 5, shared. Then he lights one of the four glowing crystals (never the one he just used, unless he has just
     * teleported) and walks to it, throwing boulders at everyone within 9 tiles and stomping anyone under him (up to 8 a
     * tick); at the crystal he is invulnerable and stores 1% of his health every 2 ticks. One 66- or 67-tick teleport timer
     * starts when he first reaches a crystal and only runs while a crystal is lit: disabling the crystal throws its healing
     * away and he picks another, but when the timer runs out he takes the healing, shatters the crystal himself and goes
     * back to the centre to teleport the team. A disabled crystal can be lit again later.
     */
    static final class Vasa extends RoomLogic implements CoxTobWalk.Walkable {
        private enum State { CENTRE, WALKING, DRAINING, RETURNING }

        private static final int[][] CORNERS = {{-13, -13}, {13, -13}, {-13, 13}, {13, 13}};
        private DataBossEntity vasa;
        private final LivingEntity[] crystals = new LivingEntity[CORNERS.length];
        private State state = State.CENTRE;
        private int stateTicks;
        private int target = -1;
        private int previous = -1;
        private boolean justTeleported;
        private int timer = -1;
        private int pendingHeal;
        private int healTicks;
        private int disabled;
        private int drained;
        private int healed;
        private boolean teleportOnReturn;
        private int charging;
        private int t;
        private int teleports;
        private int stomps;

        Vasa(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            for (int[] c : CORNERS) {
                BlockPos p = at(c[0], c[1]).below();
                RaidBuilder.fill(world(), p.getX() - 1, p.getY(), p.getZ() - 1, p.getX() + 1, p.getY(), p.getZ() + 1,
                        Blocks.PURPUR_BLOCK.defaultBlockState());
            }
        }

        @Override
        public void start() {
            vasa = spawnBoss("vasa_nistirio", at(0, 0));
            if (vasa != null) {
                anchor(vasa);
            }
            for (int i = 0; i < CORNERS.length; i++) {
                crystals[i] = null;
                crystal(i);
            }
            state = State.CENTRE;
            // OSRS: the fight starts with his teleport attack.
            teleport();
        }

        /** The crystal in corner {@code i}, lit afresh if it was disabled (OSRS: Vasa can light it again later). */
        private LivingEntity crystal(int i) {
            if (!alive(crystals[i])) {
                LivingEntity c = spawnMob("vasa_crystal", at(CORNERS[i][0], CORNERS[i][1]), false);
                if (c != null) {
                    anchor(c);
                    if (c instanceof Mob m) {
                        m.setNoAi(true);
                    }
                }
                crystals[i] = c;
            }
            return crystals[i];
        }

        private void relocate(Vec3 to) {
            unanchor(vasa);
            move(vasa, to);
            anchor(vasa);
        }

        /** Whether the teleport timer is running (it pauses while no crystal is lit). */
        private boolean crystalLit() {
            return state == State.WALKING || state == State.DRAINING;
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            t++;
            if (!alive(vasa)) {
                return;
            }
            // OSRS: anyone standing under him takes rapid stomp damage.
            for (ServerPlayer p : players()) {
                if (RaidEffects.horizontalDistSq(p.position(), vasa.position()) < 1.2 * 1.2) {
                    stomps++;
                    RaidEffects.hit(vasa, p, rnd().nextInt(CoxRules.VASA_STOMP_MAX + 1), null, true);
                }
            }
            if (charging > 0) {
                charging--;
                return;
            }
            if (crystalLit() && timer > 0) {
                timer--;
            }
            switch (state) {
                case CENTRE -> {
                    List<Integer> choices = new ArrayList<>();
                    for (int i = 0; i < CORNERS.length; i++) {
                        if (i != previous || justTeleported) {
                            choices.add(i);
                        }
                    }
                    target = choices.get(rnd().nextInt(choices.size()));
                    crystal(target);
                    state = State.WALKING;
                    stateTicks = CoxRules.VASA_WALK_TICKS;
                    warn("Vasa Nistirio heads for a glowing crystal!");
                }
                case WALKING -> {
                    if (t % 4 == 0) {
                        boulders();
                    }
                    LivingEntity c = crystals[target];
                    if (!alive(c)) {
                        lost();
                        return;
                    }
                    if (timer == 0) {
                        // The timer ran out on the way: he gives up on the crystal and teleports the team.
                        returnTo(true);
                        return;
                    }
                    if (--stateTicks <= 0) {
                        Vec3 dir = center().getCenter().subtract(c.position()).normalize().scale(2.5);
                        relocate(c.position().add(dir.x, 0, dir.z));
                        state = State.DRAINING;
                        pendingHeal = 0;
                        healTicks = 0;
                        if (timer < 0) {
                            timer = CoxRules.vasaTeleportTimer(rnd());
                        }
                        warn("Vasa begins to drain the crystal! Destroy it before he finishes!");
                    }
                }
                case DRAINING -> {
                    LivingEntity c = crystals[target];
                    if (!alive(c)) {
                        lost();
                        return;
                    }
                    if (t % CoxRules.VASA_HEAL_INTERVAL == 0) {
                        pendingHeal += Math.max(1, raid.maxHitpoints(vasa) / 100);
                        healTicks++;
                    }
                    Vec3 a = c.position().add(0, 1.5, 0);
                    Vec3 b = vasa.position().add(0, 2, 0);
                    for (int i = 0; i <= 8; i++) {
                        Vec3 p = a.lerp(b, i / 8.0);
                        world().sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                    }
                    if (timer == 0) {
                        raid.heal(vasa, pendingHeal);
                        restoreDefence(healTicks);
                        healed += pendingHeal;
                        drained++;
                        say("Vasa drains the crystal and heals " + pendingHeal + " hitpoints!");
                        pendingHeal = 0;
                        c.discard();
                        returnTo(true);
                    }
                }
                case RETURNING -> {
                    if (--stateTicks <= 0) {
                        relocate(vec(0, 0));
                        state = State.CENTRE;
                        previous = target;
                        justTeleported = false;
                        if (teleportOnReturn) {
                            teleport();
                        }
                    }
                }
            }
        }

        /** Vasa's scaled base Defence level (party and mode scaling, before drains). */
        private int baseDefence() {
            return SysCombat.baseStatLevel(vasa, Skill.DEFENCE);
        }

        /**
         * Each heal off a crystal also gives back (10% + 1) of his base Defence that special attacks drained, up to his base
         * level (Mod Ash).
         */
        private void restoreDefence(int heals) {
            restoreDrain(vasa, Skill.DEFENCE, heals * CoxRules.vasaDefenceHeal(baseDefence()));
        }

        /** The team disabled the lit crystal: its healing is lost and he goes back for another. */
        private void lost() {
            warn("The crystal shatters and Vasa's healing is lost!");
            pendingHeal = 0;
            disabled++;
            returnTo(false);
        }

        private void returnTo(boolean teleportAfter) {
            state = State.RETURNING;
            stateTicks = CoxRules.VASA_WALK_TICKS;
            teleportOnReturn = teleportAfter;
            if (teleportAfter) {
                timer = -1;
            }
        }

        /** Boulders at every player within 9 tiles (3x3; Protect from Missiles helps). // approx: up to 18 */
        private void boulders() {
            for (ServerPlayer p : players()) {
                if (p.distanceToSqr(vasa) <= CoxRules.VASA_BOULDER_RANGE * CoxRules.VASA_BOULDER_RANGE) {
                    Vec3 at = p.position();
                    world().sendParticles(ParticleTypes.CRIT, at.x, at.y + 0.2, at.z, 8, 0.6, 0.05, 0.6, 0);
                    RaidEffects.later(3, () -> {
                        for (ServerPlayer q : players()) {
                            if (RaidEffects.horizontalDistSq(q.position(), at) <= 1.5 * 1.5) {
                                RaidEffects.hit(vasa, q, rnd().nextInt(19), AttackType.RANGED, false);
                            }
                        }
                    });
                }
            }
        }

        /**
         * Vasa's teleport (OSRS wiki): half the team (the player when solo) is pulled next to him, stunned and stripped of
         * prayers, the rest land at the edges. The barrage then hits everyone next to him for the stunned players' summed
         * hitpoints minus 5, shared between them; unstunned players can pray Protect from Magic against it.
         */
        private void teleport() {
            List<ServerPlayer> party = new ArrayList<>(players());
            if (party.isEmpty() || !alive(vasa)) {
                return;
            }
            teleports++;
            justTeleported = true;
            java.util.Collections.shuffle(party, new java.util.Random(rnd().nextLong()));
            CombatAchievementService.countNear(vasa, CombatAchievements.COUNT_VASA_TELEPORT);
            int n = Math.min(party.size(), RaidScaling.vasaTeleportTargets(party.size()));
            List<ServerPlayer> stunned = new ArrayList<>(party.subList(0, n));
            int[] hp = new int[n];
            for (int i = 0; i < n; i++) {
                ServerPlayer p = stunned.get(i);
                hp[i] = com.runecraft.combat.Overheal.hitpoints(p);
                raid.teleport(p, vasa.blockPosition().offset(i % 2 == 0 ? 2 : -2, 0, i / 2 - 1));
                com.runecraft.content.systems.SysCombat.freeze(p, RaidScaling.VASA_TELEPORT_DELAY);
                dropOverheads(p, "Vasa Nistirio teleports you to him and drains your prayers!");
                Msg.red(p, "Vasa Nistirio teleports you to him!");
            }
            for (int i = n; i < party.size(); i++) {
                ServerPlayer p = party.get(i);
                raid.teleport(p, at(i % 2 == 0 ? -17 : 17, i % 4 < 2 ? -6 : 6));
                Msg.red(p, "Vasa Nistirio throws you to the edge of the room! Run in and protect from magic.");
            }
            charging = RaidScaling.VASA_TELEPORT_DELAY;
            world().sendParticles(ParticleTypes.END_ROD, vasa.getX(), vasa.getY() + 2, vasa.getZ(), 30, 1.5, 1, 1.5, 0.05);
            RaidEffects.later(RaidScaling.VASA_TELEPORT_DELAY, () -> barrage(stunned, hp));
        }

        private void barrage(List<ServerPlayer> stunned, int[] hp) {
            if (!alive(vasa)) {
                return;
            }
            List<ServerPlayer> receivers = new ArrayList<>();
            for (ServerPlayer p : players()) {
                if (stunned.contains(p) || p.distanceToSqr(vasa) <= 3.5 * 3.5) {
                    receivers.add(p);
                }
            }
            int dmg = RaidScaling.vasaTeleportDamage(hp, receivers.size());
            world().sendParticles(ParticleTypes.END_ROD, vasa.getX(), vasa.getY() + 1, vasa.getZ(), 40, 3, 0.5, 3, 0.1);
            for (ServerPlayer p : receivers) {
                // Stunned players can't pray; the others' Protect from Magic blocks their share.
                RaidEffects.hit(vasa, p, dmg, AttackType.MAGIC, stunned.contains(p));
            }
        }

        /** OSRS: Vasa can't be harmed while he charges his teleport or drains a crystal. */
        @Override
        public boolean bossVulnerable(DataBossEntity boss) {
            return charging <= 0 && state != State.DRAINING;
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

        @Override
        public String walk(CoxTobWalk w) {
            w.expect(alive(vasa), "Vasa did not spawn");
            w.expect(teleports >= 1 && w.player().distanceToSqr(vasa) < 6 * 6, "Vasa's opening teleport did not pull the player to him");
            // Stand under him: he stomps.
            w.moveTo(vasa.position());
            w.tick(1);
            w.expect(stomps > 0, "standing under Vasa was never stomped");
            w.moveTo(at(-8, 0));
            w.until(() -> state == State.DRAINING, 30, "Vasa never started draining a crystal");
            w.expect(timer > 0, "the teleport timer did not start when Vasa reached his first crystal");
            w.expect(!bossVulnerable(vasa) && w.hit(vasa, 30, AttackType.CRUSH) == 0, "Vasa took damage while draining a crystal");
            int first = target;
            LivingEntity c1 = crystals[first];
            w.expect(w.hit(c1, 30, AttackType.RANGED) == 0, "a glowing crystal took ranged damage (it is immune)");
            w.kill(c1, AttackType.STAB, "the glowing crystal");
            int timerAfter = timer;
            w.until(() -> state == State.WALKING || state == State.DRAINING, 30, "Vasa did not pick another crystal");
            w.expect(target != first, "Vasa went straight back to the crystal the team had just disabled");
            w.expect(disabled == 1 && timer <= timerAfter, "the teleport timer restarted after a disabled crystal (it is shared)");
            // Leave this crystal alone: the shared timer runs out, he takes the healing and teleports the team.
            w.until(() -> state == State.WALKING, 10, "Vasa never walked to the next crystal");
            w.hit(vasa, Math.max(10, raid.maxHitpoints(vasa) / 5), AttackType.CRUSH);
            SysCombat.drainDefence(vasa, Math.min(60, baseDefence() - 1)); // as if drained by special attacks
            int hurt = raid.hitpoints(vasa);
            int before = teleports;
            w.until(() -> teleports > before, 90, "Vasa's teleport timer never ran out (" + timer + " left)");
            w.expect(drained == 1 && healed > 0 && raid.hitpoints(vasa) > hurt, "Vasa did not heal off the crystal he drained");
            w.expect(SysCombat.drainedStatLevel(vasa, Skill.DEFENCE) == baseDefence(), "draining a crystal did not restore Vasa's"
                    + " Defence (" + SysCombat.drainedStatLevel(vasa, Skill.DEFENCE) + "/" + baseDefence() + ")");
            w.until(() -> state == State.WALKING || state == State.DRAINING, 30, "Vasa did not light a crystal after teleporting");
            w.expect(alive(crystals[target]), "the crystal Vasa picked was not lit");
            int guard = 0;
            while (alive(vasa) && guard++ < 400) {
                if (bossVulnerable(vasa)) {
                    w.hit(vasa, Math.max(10, raid.maxHitpoints(vasa) / 8), AttackType.CRUSH);
                } else if (state == State.DRAINING && alive(crystals[target])) {
                    w.hit(crystals[target], Math.max(10, raid.maxHitpoints(crystals[target]) / 2), AttackType.STAB);
                }
                w.tick(1);
            }
            w.expect(!alive(vasa), "Vasa did not die (state " + state + ")");
            return teleports + " teleports, " + disabled + " crystal(s) disabled, " + drained + " drained (+" + healed + "), "
                    + stomps + " stomp(s)";
        }
    }

    // ================================================================== Guardians

    /**
     * Guardians (OSRS): only a pickaxe harms them; an iron pickaxe lies next to them for anyone who forgot one. Anyone who
     * tries to slip past them while one stands is knocked back for moderate damage.
     */
    static final class Guardians extends RoomLogic implements CoxTobWalk.Walkable {
        /** Blocks east of the centre beyond which a player has slipped between the Guardians. */
        private static final int PASSAGE_X = 11;
        private int pushes;

        Guardians(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            RaidBuilder.set(world(), at(-14, 4), RaidBuilder.module(RaidsContent.COX_TOOLS));
        }

        @Override
        public void start() {
            anchor(spawnBoss("cox_guardian", at(9, -4)));
            anchor(spawnBoss("cox_guardian", at(9, 4)));
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            if (done || allDead()) {
                return;
            }
            LivingEntity guard = null;
            for (LivingEntity e : spawned) {
                if (alive(e)) {
                    guard = e;
                }
            }
            for (ServerPlayer p : players()) {
                if (p.getX() > center().getX() + 0.5 + PASSAGE_X) {
                    pushes++;
                    raid.teleport(p, at(5, 0));
                    RaidEffects.hit(guard, p, CoxRules.GUARDIAN_PUSH_MIN
                            + rnd().nextInt(CoxRules.GUARDIAN_PUSH_MAX - CoxRules.GUARDIAN_PUSH_MIN + 1), null, true);
                    Msg.red(p, "The Guardian shoves you back from the passage!");
                }
            }
        }

        @Override
        public boolean useBlock(ServerPlayer player, String blockId, BlockPos pos) {
            if (!blockId.equals(RaidsContent.COX_TOOLS)) {
                return false;
            }
            toolSpawn(player, "iron_pickaxe");
            return true;
        }

        @Override
        public int bossIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (!Inv.idOf(attacker.getMainHandItem()).endsWith("_pickaxe")) {
                Msg.action(attacker, "Only a pickaxe can harm the Guardians.");
                return 0;
            }
            // OSRS: damage x (50 + Mining level + pickaxe level) / 150, the pickaxe capped at a dragon pickaxe.
            int dealt = RaidScaling.coxGuardianDamage(damage, level(attacker, Skill.MINING),
                    RaidScaling.pickaxeLevel(Inv.idOf(attacker.getMainHandItem())));
            if (dealt > 0) {
                XpService.add(attacker, Skill.MINING, dealt * 2.0);
            }
            return dealt;
        }

        @Override
        public String walk(CoxTobWalk w) {
            w.hold(null);
            LivingEntity first = spawned.isEmpty() ? null : spawned.get(0);
            w.expect(first != null, "no Guardians spawned");
            w.expect(w.hit(first, 20, AttackType.CRUSH) == 0, "a Guardian took damage without a pickaxe");
            // Slipping past them gets you shoved back.
            w.moveTo(at(PASSAGE_X + 3, 0));
            w.tick(1);
            w.expect(pushes > 0 && w.player().getX() < center().getX() + PASSAGE_X, "the Guardians let the player walk past them");
            w.use(w.block(this, RaidsContent.COX_TOOLS));
            w.expect(w.count("iron_pickaxe") > 0, "the tools next to the Guardians gave no iron pickaxe");
            w.hold("iron_pickaxe");
            for (LivingEntity g : new ArrayList<>(spawned)) {
                w.kill(g, AttackType.CRUSH, "a Guardian");
            }
            w.hold(null);
            return "killed with the room's iron pickaxe, " + pushes + " push(es)";
        }
    }

    // ================================================================== Lizardman shamans

    /**
     * Lizardman shamans (OSRS wiki "Lizardman shaman"): between attacks a shaman leaps onto a player's spot (3x3, 20-25
     * typeless), lobs a purple poison blob there (3x3, about 25% stronger than outside, up to 39, poison from 12) or summons
     * three spawns that run at the player and explode seconds later (5-10 each within two tiles); only the spawns have a
     * cooldown. Shamans can't be hit mid-leap. Their count grows with the party (2, +1 per 5 players, up to 5).
     */
    static final class Shamans extends RoomLogic implements CoxTobWalk.Walkable {
        enum Special { JUMP, ACID, SPAWNS }

        private final Map<LivingEntity, Integer> specialCooldown = new HashMap<>();
        private final Map<LivingEntity, Integer> spawnCooldown = new HashMap<>();
        private final Set<LivingEntity> jumping = new HashSet<>();
        private final Map<LivingEntity, Integer> fuses = new HashMap<>();
        private int jumpsLanded;
        private int acidLanded;
        private int spawnBlasts;
        private int spawnsSummoned;

        Shamans(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            for (int i = 0; i < 8; i++) {
                BlockPos p = at(rnd().nextInt(30) - 15, rnd().nextInt(30) - 15);
                RaidBuilder.set(world(), p, Blocks.MOSSY_COBBLESTONE_WALL.defaultBlockState());
                RaidBuilder.set(world(), p.above(), Blocks.MOSS_BLOCK.defaultBlockState());
            }
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            for (LivingEntity e : new ArrayList<>(spawned)) {
                if (!alive(e) || jumping.contains(e)) {
                    continue;
                }
                spawnCooldown.merge(e, -1, Integer::sum);
                int cd = specialCooldown.getOrDefault(e, CoxRules.SHAMAN_SPECIAL_MIN + rnd().nextInt(CoxRules.SHAMAN_SPECIAL_SPREAD)) - 1;
                specialCooldown.put(e, cd);
                if (cd > 0) {
                    continue;
                }
                specialCooldown.put(e, CoxRules.SHAMAN_SPECIAL_MIN + rnd().nextInt(CoxRules.SHAMAN_SPECIAL_SPREAD));
                ServerPlayer target = randomPlayer();
                if (target == null) {
                    continue;
                }
                List<Special> options = new ArrayList<>(List.of(Special.JUMP, Special.ACID));
                if (spawnCooldown.getOrDefault(e, 0) <= 0) {
                    options.add(Special.SPAWNS);
                }
                special(e, options.get(rnd().nextInt(options.size())), target);
            }
            for (Map.Entry<LivingEntity, Integer> f : new ArrayList<>(fuses.entrySet())) {
                LivingEntity spawn = f.getKey();
                if (!alive(spawn)) {
                    fuses.remove(spawn);
                    continue;
                }
                if (f.getValue() > 1) {
                    fuses.put(spawn, f.getValue() - 1);
                    continue;
                }
                fuses.remove(spawn);
                world().sendParticles(ParticleTypes.EXPLOSION, spawn.getX(), spawn.getY() + 0.5, spawn.getZ(), 2, 0.3, 0.3, 0.3, 0);
                for (ServerPlayer p : players()) {
                    if (RaidEffects.horizontalDistSq(p.position(), spawn.position()) <= 2.5 * 2.5) {
                        spawnBlasts++;
                        RaidEffects.hit(spawn, p, CoxRules.SPAWN_BLAST_MIN
                                + rnd().nextInt(CoxRules.SPAWN_BLAST_MAX - CoxRules.SPAWN_BLAST_MIN + 1), null, true);
                    }
                }
                spawn.discard();
            }
        }

        /** One of a shaman's special moves at a player. */
        void special(LivingEntity shaman, Special kind, ServerPlayer target) {
            Vec3 mark = target.position();
            switch (kind) {
                case JUMP -> {
                    jumping.add(shaman);
                    say("A lizardman shaman leaps into the air!");
                    world().sendParticles(ParticleTypes.CLOUD, shaman.getX(), shaman.getY(), shaman.getZ(), 15, 0.5, 0.2, 0.5, 0.05);
                    shaman.setInvisible(true);
                    mark(mark, ParticleTypes.CLOUD, 3);
                    later(3, () -> {
                        jumping.remove(shaman);
                        shaman.setInvisible(false);
                        if (!alive(shaman)) {
                            return;
                        }
                        shaman.teleportTo(mark.x, mark.y, mark.z);
                        for (ServerPlayer p : players()) {
                            if (RaidEffects.horizontalDistSq(p.position(), mark) <= 1.5 * 1.5) {
                                jumpsLanded++;
                                RaidEffects.hit(shaman, p, CoxRules.SHAMAN_JUMP_MIN
                                        + rnd().nextInt(CoxRules.SHAMAN_JUMP_MAX - CoxRules.SHAMAN_JUMP_MIN + 1), null, true);
                            }
                        }
                    });
                }
                case ACID -> {
                    Msg.red(target, "A lizardman shaman lobs a blob of purple poison at you!");
                    mark(mark, ParticleTypes.WITCH, 3);
                    later(3, () -> {
                        world().sendParticles(ParticleTypes.WITCH, mark.x, mark.y + 0.3, mark.z, 30, 1, 0.2, 1, 0.05);
                        for (ServerPlayer p : players()) {
                            if (RaidEffects.horizontalDistSq(p.position(), mark) <= 1.5 * 1.5) {
                                acidLanded++;
                                RaidEffects.hit(shaman, p, CoxRules.SHAMAN_ACID_MIN
                                        + rnd().nextInt(CoxRules.SHAMAN_ACID_MAX - CoxRules.SHAMAN_ACID_MIN + 1), null, true);
                                PoisonService.poison(p, CoxRules.SHAMAN_POISON, ActionManager.gameTick());
                            }
                        }
                    });
                }
                case SPAWNS -> {
                    spawnCooldown.put(shaman, CoxRules.SHAMAN_SPAWN_COOLDOWN);
                    say("The ground shakes as a lizardman shaman summons spawn!");
                    for (int i = 0; i < CoxRules.SHAMAN_SPAWNS; i++) {
                        Mob spawn = spawnMob("cox_lizardman_spawn", BlockPos.containing(mark).offset(i - 1, 0, 1), false);
                        if (spawn != null) {
                            spawnsSummoned++;
                            fuses.put(spawn, CoxRules.SPAWN_FUSE_TICKS);
                            spawn.setTarget(target);
                        }
                    }
                }
            }
        }

        /** Marks a 3x3 landing area every tick until it lands. */
        private void mark(Vec3 at, ParticleOptions look, int ticks) {
            for (int i = 0; i < ticks; i++) {
                later(i + 1, () -> world().sendParticles(look, at.x, at.y + 0.15, at.z, 12, 0.75, 0.02, 0.75, 0));
            }
            world().sendParticles(look, at.x, at.y + 0.15, at.z, 12, 0.75, 0.02, 0.75, 0);
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
            fuses.clear();
        }

        @Override
        public String walk(CoxTobWalk w) {
            w.expect(spawned.size() == RaidScaling.coxShamanCount(raid.partySize()),
                    "expected " + RaidScaling.coxShamanCount(raid.partySize()) + " shamans, found " + spawned.size());
            LivingEntity first = spawned.get(0);
            for (LivingEntity e : spawned) {
                specialCooldown.put(e, 1000); // the walk triggers the specials itself
            }
            w.moveTo(at(-6, 0));
            special(first, Special.JUMP, w.player());
            w.expect(!bossVulnerable((DataBossEntity) first), "a shaman could be hit in mid-leap");
            w.tick(4);
            w.expect(jumpsLanded > 0 && first.distanceToSqr(w.player()) < 2.5 * 2.5, "the shaman's leap did not land on the player");
            special(first, Special.ACID, w.player());
            w.tick(4);
            w.expect(acidLanded > 0, "the purple poison blob did not land on the player");
            special(first, Special.SPAWNS, w.player());
            w.expect(spawnsSummoned == CoxRules.SHAMAN_SPAWNS, "a summon made " + spawnsSummoned + " spawns, expected 3");
            w.until(() -> fuses.isEmpty(), CoxRules.SPAWN_FUSE_TICKS + 3, "the spawns never exploded");
            w.expect(spawnBlasts > 0, "the spawns exploded without hurting the player next to them");
            for (LivingEntity e : new ArrayList<>(spawned)) {
                w.kill(e, AttackType.RANGED, "a lizardman shaman");
            }
            return spawned.size() + " shamans; leap, poison blob and " + spawnBlasts + " spawn blast(s) landed";
        }
    }

    // ================================================================== Skeletal Mystics

    /**
     * Skeletal Mystics (OSRS wiki): they cast a bolt or a damaging Vulnerability, and punch a target that prays Protect from
     * Magic if they can reach it. Their count grows with the party (3, +1 per 3 players, up to 12).
     */
    static final class Mystics extends RoomLogic implements CoxTobWalk.Walkable {
        Mystics(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public com.runecraft.osrs.content.AttackDef bossAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            boolean melee = CoxRules.mysticMelees(RaidEffects.protectedFrom(target, AttackType.MAGIC), distance <= 2.5);
            for (com.runecraft.osrs.content.AttackDef a : attacks(boss)) {
                if (a.melee() == melee) {
                    return a;
                }
            }
            return null;
        }

        @Override
        public String walk(CoxTobWalk w) {
            int want = RaidScaling.coxMysticCount(raid.partySize());
            w.expect(spawned.size() == want, "expected " + want + " mystics, found " + spawned.size());
            DataBossEntity first = (DataBossEntity) spawned.get(0);
            var data = Players.data(w.player());
            boolean had = data.isActive(com.runecraft.osrs.Prayer.PROTECT_FROM_MAGIC);
            data.activePrayers().remove(com.runecraft.osrs.Prayer.PROTECT_FROM_MAGIC);
            com.runecraft.osrs.content.AttackDef cast = bossAttack(first, w.player(), 1.5);
            w.expect(cast != null && !cast.melee(), "a mystic next to an unprotected player did not cast");
            data.activePrayers().add(com.runecraft.osrs.Prayer.PROTECT_FROM_MAGIC);
            com.runecraft.osrs.content.AttackDef punch = bossAttack(first, w.player(), 1.5);
            com.runecraft.osrs.content.AttackDef far = bossAttack(first, w.player(), 8);
            if (!had) {
                data.activePrayers().remove(com.runecraft.osrs.Prayer.PROTECT_FROM_MAGIC);
            }
            w.expect(punch != null && punch.melee(), "a mystic did not punch a player praying Protect from Magic within reach");
            w.expect(far != null && !far.melee(), "a mystic tried to punch a player out of reach");
            for (LivingEntity e : new ArrayList<>(spawned)) {
                w.kill(e, AttackType.CRUSH, "a Skeletal Mystic");
            }
            return want + " mystics: casts, and punches through Protect from Magic";
        }
    }

    // ================================================================== Muttadiles

    /**
     * Muttadiles (OSRS wiki): the small one first while its mother lurks in the water; every 4 ticks the submerged mother
     * picks each player in sight with a 1/3 chance for a magic blast (up to 23; protection prayers only reduce it). At about
     * half health either goes to the meat tree and eats up to another half of its health, at most three times; one bound in
     * place for about 15 seconds gives up on the tree. The tree has 5 x the team's total Woodcutting health (at least 100);
     * a swing lands by the skilling roll (20% at level 1, 78% at 99) for half the Woodcutting level -7/+3, whatever the axe
     * (a bronze axe lies in the room), and gives 45 XP. The mother stomps everyone in melee range for up to 72.
     */
    static final class Muttadiles extends RoomLogic implements CoxTobWalk.Walkable {
        private DataBossEntity small;
        private DataBossEntity big;
        private boolean treeAlive = true;
        private int treeHp;
        private int treeMaxHp;
        private int t;
        private final Map<LivingEntity, Integer> feeding = new HashMap<>();
        private final Map<LivingEntity, Integer> feedCooldown = new HashMap<>();
        private final Map<LivingEntity, Integer> feeds = new HashMap<>();
        private final Map<LivingEntity, Integer> bound = new HashMap<>();
        private final Set<LivingEntity> gaveUp = new HashSet<>();
        private int waterBlasts;
        private int healed;

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
                    Blocks.WATER.defaultBlockState());
            RaidBuilder.set(world(), tree(), RaidBuilder.module(RaidsContent.MEAT_TREE));
            RaidBuilder.set(world(), tree().above(), Blocks.MANGROVE_ROOTS.defaultBlockState());
            RaidBuilder.fill(world(), tree().getX() - 1, tree().getY() + 2, tree().getZ() - 1, tree().getX() + 1, tree().getY() + 3,
                    tree().getZ() + 1, Blocks.RED_MUSHROOM_BLOCK.defaultBlockState());
            RaidBuilder.set(world(), at(-14, 4), RaidBuilder.module(RaidsContent.COX_TOOLS));
        }

        @Override
        public void start() {
            int total = 0;
            for (ServerPlayer p : raid.players()) {
                total += level(p, Skill.WOODCUTTING);
            }
            treeMaxHp = CoxRules.meatTreeHp(total);
            treeHp = treeMaxHp;
            small = spawnBoss("small_muttadile", at(9, -9));
        }

        /** Whether a muttadile is bound or frozen in place (OSRS: a bind stops it reaching the tree). */
        private static boolean held(LivingEntity m) {
            var slow = m.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
            return slow != null && slow.getAmplifier() >= 6;
        }

        @Override
        public void gameTick(long tick) {
            t++;
            for (DataBossEntity m : new DataBossEntity[]{small, big}) {
                if (!alive(m)) {
                    continue;
                }
                Integer f = feeding.get(m);
                if (f != null) {
                    if (!treeAlive || f <= 0) {
                        feeding.remove(m);
                        unanchor(m);
                        feedCooldown.put(m, 20); // approx
                    } else {
                        feeding.put(m, f - 1);
                        int heal = Math.max(1, (int) (raid.maxHitpoints(m) * CoxRules.MUTTADILE_FEED_HEAL / CoxRules.MUTTADILE_FEED_TICKS));
                        int before = raid.hitpoints(m);
                        raid.heal(m, heal);
                        healed += raid.hitpoints(m) - before;
                        CombatAchievementService.flagNear(m, CombatAchievements.FLAG_MEAT_TREE_HEAL);
                        world().sendParticles(ParticleTypes.HEART, m.getX(), m.getY() + 1.5, m.getZ(), 1, 0.3, 0.3, 0.3, 0);
                    }
                    continue;
                }
                int cd = feedCooldown.getOrDefault(m, 0);
                if (cd > 0) {
                    feedCooldown.put(m, cd - 1);
                    continue;
                }
                boolean hungry = treeAlive && !gaveUp.contains(m) && feeds.getOrDefault(m, 0) < CoxRules.MUTTADILE_MAX_FEEDS
                        && raid.healthFraction(m) < CoxRules.MUTTADILE_FEED_BELOW;
                if (!hungry) {
                    bound.remove(m);
                    continue;
                }
                if (held(m)) {
                    // OSRS: bound and unable to reach the tree for about 15 seconds, it gives up on it for good.
                    int b = bound.merge(m, 1, Integer::sum);
                    if (b >= CoxRules.MUTTADILE_FEED_GIVE_UP_TICKS) {
                        gaveUp.add(m);
                        say("The Muttadile gives up on the meat tree.");
                    }
                    continue;
                }
                move(m, Vec3.atBottomCenterOf(tree().offset(2, 0, 0)));
                anchor(m);
                feeding.put(m, CoxRules.MUTTADILE_FEED_TICKS);
                feeds.merge(m, 1, Integer::sum);
                warn("The Muttadile goes to feed on the meat tree!");
            }
            // OSRS: while the small one fights, its mother blasts players in sight with magic from the water.
            if (alive(small) && big == null && t % CoxRules.MUTTADILE_WATER_BLAST_INTERVAL == 0) {
                Vec3 from = vec(10, -9).add(0, 1, 0);
                world().sendParticles(ParticleTypes.SPLASH, from.x, from.y, from.z, 10, 1, 0.5, 1, 0.1);
                for (ServerPlayer p : players()) {
                    if (rnd().nextDouble() >= CoxRules.MUTTADILE_WATER_BLAST_CHANCE || !clearLine(from, p.getEyePosition(), p)) {
                        continue;
                    }
                    waterBlasts++;
                    RaidEffects.later(2, () -> {
                        if (p.isAlive() && inside(p.position())) {
                            // approx: protection prayers halve it ("only reduce" the ranged and magic attacks)
                            RaidEffects.hit(null, p, rnd().nextInt(CoxRules.MUTTADILE_MAGIC_MAX + 1), AttackType.MAGIC, 0.5);
                        }
                    });
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
        public boolean useBlock(ServerPlayer player, String blockId, BlockPos pos) {
            if (blockId.equals(RaidsContent.COX_TOOLS)) {
                toolSpawn(player, "bronze_axe");
                return true;
            }
            if (!blockId.equals(RaidsContent.MEAT_TREE)) {
                return false;
            }
            if (!treeAlive || cooldown(player, 4)) {
                return true;
            }
            if (!hasTool(player, "_axe")) {
                Msg.game(player, "You need an axe to chop down the meat tree. There's a bronze axe by the entrance.");
                return true;
            }
            player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            int wc = level(player, Skill.WOODCUTTING);
            if (rnd().nextDouble() < CoxRules.meatTreeAccuracy(wc)) {
                treeHp -= CoxRules.meatTreeChop(wc, rnd());
                XpService.add(player, Skill.WOODCUTTING, CoxRules.MEAT_TREE_XP);
                if (treeHp <= 0) {
                    treeAlive = false;
                    RaidBuilder.fill(world(), tree().getX() - 1, tree().getY(), tree().getZ() - 1, tree().getX() + 1, tree().getY() + 3,
                            tree().getZ() + 1, Blocks.AIR.defaultBlockState());
                    RaidBuilder.set(world(), tree(), Blocks.MANGROVE_ROOTS.defaultBlockState());
                    say("The meat tree has been chopped down! The Muttadiles can no longer feed.");
                } else {
                    Msg.action(player, "You hack at the meat tree. (" + treeHp + " health left)");
                }
            }
            return true;
        }

        @Override
        public void cleanup() {
            super.cleanup();
            for (DataBossEntity m : new DataBossEntity[]{small, big}) {
                if (alive(m)) {
                    m.discard();
                }
            }
        }

        @Override
        public String walk(CoxTobWalk w) {
            int total = 0;
            for (ServerPlayer p : raid.players()) {
                total += level(p, Skill.WOODCUTTING);
            }
            w.expect(treeMaxHp == CoxRules.meatTreeHp(total), "the meat tree has " + treeMaxHp + " health, expected "
                    + CoxRules.meatTreeHp(total) + " (5 x the team's Woodcutting)");
            // The small one feeds at half health, at most three times.
            w.moveTo(at(-10, 0));
            for (int i = 0; i < CoxRules.MUTTADILE_MAX_FEEDS; i++) {
                int want = i + 1;
                int hp = raid.hitpoints(small);
                w.hit(small, Math.max(1, hp - (int) (raid.maxHitpoints(small) * 0.4)), AttackType.RANGED);
                int low = raid.hitpoints(small);
                w.until(() -> feeds.getOrDefault(small, 0) == want, 30, "the small Muttadile did not go to feed (feed " + want + ")");
                w.until(() -> !feeding.containsKey(small), 20, "the small Muttadile never stopped feeding");
                w.expect(raid.hitpoints(small) > low, "feeding on the meat tree did not heal the small Muttadile");
            }
            w.hit(small, Math.max(1, raid.hitpoints(small) - (int) (raid.maxHitpoints(small) * 0.4)), AttackType.RANGED);
            w.tick(25);
            w.expect(feeds.getOrDefault(small, 0) == CoxRules.MUTTADILE_MAX_FEEDS && !feeding.containsKey(small),
                    "the small Muttadile fed a fourth time");
            w.expect(waterBlasts > 0, "the submerged Muttadile never blasted the player");
            w.use(w.block(this, RaidsContent.COX_TOOLS));
            w.expect(w.count("bronze_axe") > 0, "the room's bronze axe spawn gave nothing");
            w.hold("bronze_axe");
            BlockPos treePos = tree();
            int swings = 0;
            while (treeAlive && swings++ < 600) {
                w.moveTo(treePos.offset(1, 0, 0));
                w.use(treePos);
                w.tick(4);
            }
            w.expect(!treeAlive, "the meat tree was not felled after " + swings + " swings");
            w.hold(null);
            w.kill(small, AttackType.RANGED, "the small Muttadile");
            w.until(() -> alive(big), 5, "the large Muttadile did not emerge");
            w.kill(big, AttackType.RANGED, "the large Muttadile");
            return "3 feeds (+" + healed + "), " + waterBlasts + " water blast(s), tree felled in " + swings + " swings";
        }
    }

    // ================================================================== Vanguards

    /**
     * Vanguards (OSRS wiki): three linked Vanguards; if one's health strays too far from the others (40% for four players
     * or fewer, 33.3% for five or more) they shuffle and heal to full. They change places every 20-36 ticks and can't be
     * harmed in their shells; a fatal hit landing in the shell leaves 1 hitpoint, and players in their path take 3-6 stomp
     * damage. Every attack is three hits: the melee Vanguard lands all three on its target, the ranged and magic ones hit
     * the target once and throw the other two around it. One overload (+) per room is guaranteed.
     */
    static final class Vanguards extends RoomLogic implements CoxTobWalk.Walkable {
        private static final String[] IDS = {"vanguard_melee", "vanguard_ranged", "vanguard_magic"};
        private final DataBossEntity[] guards = new DataBossEntity[3];
        private final List<Vec3[]> paths = new ArrayList<>();
        private int shelled;
        private int shuffleTimer;
        private int resets;
        private int stomps;
        private int extraHits;
        private boolean overloadDropped;

        Vanguards(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void start() {
            BlockPos[] spots = {at(6, -7), at(13, 0), at(6, 7)};
            for (int i = 0; i < 3; i++) {
                guards[i] = spawnBoss(IDS[i], spots[i]);
                anchor(guards[i]);
            }
            shuffleTimer = CoxRules.vanguardShuffleTicks(rnd());
        }

        private List<DataBossEntity> live() {
            List<DataBossEntity> live = new ArrayList<>();
            for (DataBossEntity g : guards) {
                if (alive(g)) {
                    live.add(g);
                }
            }
            return live;
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            if (done) {
                return;
            }
            List<DataBossEntity> live = live();
            if (shelled > 0) {
                // OSRS: players in the shuffling Vanguards' path take 3-6 rapid stomp damage every few ticks.
                if (shelled % 2 == 0) {
                    for (ServerPlayer p : players()) {
                        for (Vec3[] path : paths) {
                            if (nearSegment(p.position(), path[0], path[1], 1.5)) {
                                stomps++;
                                RaidEffects.hit(live.isEmpty() ? null : live.get(0), p, CoxRules.VANGUARD_STOMP_MIN
                                        + rnd().nextInt(CoxRules.VANGUARD_STOMP_MAX - CoxRules.VANGUARD_STOMP_MIN + 1), null, true);
                                break;
                            }
                        }
                    }
                }
                if (--shelled == 0) {
                    paths.clear();
                    say("The Vanguards emerge from their shells.");
                }
            }
            if (live.size() > 1) {
                double min = 1;
                double max = 0;
                for (DataBossEntity g : live) {
                    double f = raid.healthFraction(g);
                    min = Math.min(min, f);
                    max = Math.max(max, f);
                }
                if (max - min > CoxRules.vanguardResetSpread(raid.partySize())) {
                    for (DataBossEntity g : live) {
                        raid.setCurrentHitpoints(g, raid.maxHitpoints(g));
                    }
                    resets++;
                    warn("The Vanguards' health is too uneven - they shuffle and heal fully!");
                    CombatAchievementService.flagNear(live.get(0), CombatAchievements.FLAG_VANGUARD_RESET);
                    shuffle(live);
                    return;
                }
            }
            if (--shuffleTimer <= 0 && live.size() > 1) {
                shuffle(live);
            }
        }

        private void shuffle(List<DataBossEntity> live) {
            shuffleTimer = CoxRules.vanguardShuffleTicks(rnd());
            shelled = CoxRules.VANGUARD_SHELL_TICKS;
            paths.clear();
            say("The Vanguards retreat into their shells and change places!");
            Vec3 first = live.get(0).position();
            for (int i = 0; i < live.size(); i++) {
                DataBossEntity g = live.get(i);
                Vec3 to = i + 1 < live.size() ? live.get(i + 1).position() : first;
                paths.add(new Vec3[]{g.position(), to});
                unanchor(g);
                move(g, to);
                anchor(g);
                g.setTarget(null);
            }
        }

        /** Whether {@code p} stands within {@code r} blocks (horizontally) of the segment from {@code a} to {@code b}. */
        private static boolean nearSegment(Vec3 p, Vec3 a, Vec3 b, double r) {
            double dx = b.x - a.x;
            double dz = b.z - a.z;
            double len = dx * dx + dz * dz;
            double k = len <= 0 ? 0 : Math.max(0, Math.min(1, ((p.x - a.x) * dx + (p.z - a.z) * dz) / len));
            double ex = a.x + k * dx - p.x;
            double ez = a.z + k * dz - p.z;
            return ex * ex + ez * ez <= r * r;
        }

        /**
         * OSRS wiki: every Vanguard attack is three hits. The melee Vanguard lands the other two on its target too; the ranged
         * and magic ones throw them at tiles around the target (protection prayers reduce, but don't stop, them).
         */
        @Override
        public int bossOutgoing(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
            String id = RaidInstance.creatureId(boss);
            int max = 0;
            for (com.runecraft.osrs.content.AttackDef a : attacks(boss)) {
                max = Math.max(max, a.maxHit());
            }
            int top = max;
            for (int i = 1; i < CoxRules.VANGUARD_HITS_PER_ATTACK; i++) {
                if (id.equals("vanguard_melee")) {
                    later(i, () -> {
                        if (alive(boss) && player.isAlive() && inside(player.position())) {
                            extraHits++;
                            RaidEffects.hit(boss, player, rnd().nextInt(top + 1), AttackType.SLASH, 0.5); // approx: prayer halves it
                        }
                    });
                } else {
                    Vec3 at = player.position().add(rnd().nextInt(5) - 2, 0, rnd().nextInt(5) - 2);
                    extraHits++;
                    RaidEffects.strike(world(), at, 1.0, top, 2, boss, id.equals("vanguard_magic") ? ParticleTypes.WITCH
                            : ParticleTypes.CRIT, type, 0.5);
                }
            }
            return damage;
        }

        @Override
        public boolean bossVulnerable(DataBossEntity boss) {
            return shelled <= 0;
        }

        @Override
        public boolean bossAllowDeath(DataBossEntity boss) {
            if (shelled > 0) {
                boss.setHealth(1f);
                return false;
            }
            return true;
        }

        @Override
        public void bossDeath(DataBossEntity boss, ServerPlayer killer) {
            if (!world().getEntitiesOfClass(ItemEntity.class, boss.getBoundingBox().inflate(3),
                    e -> "cox_overload_plus".equals(Inv.idOf(e.getItem()))).isEmpty()) {
                overloadDropped = true;
            }
            if (live().isEmpty() && !overloadDropped) {
                dropItem(world(), boss.position(), "cox_overload_plus", 1);
                overloadDropped = true;
            }
        }

        @Override
        public String walk(CoxTobWalk w) {
            List<DataBossEntity> all = live();
            w.expect(all.size() == 3, "expected three Vanguards, found " + all.size());
            w.until(() -> shelled <= 0, 10, "the Vanguards never left their shells");
            // A lopsided hit makes them reset.
            DataBossEntity first = all.get(0);
            w.hit(first, (int) (raid.maxHitpoints(first) * 0.6), AttackType.MAGIC);
            w.tick(1);
            w.expect(resets > 0 && raid.healthFraction(first) > 0.99, "an uneven Vanguard did not make them reset");
            // Standing in a shuffling Vanguard's path gets you stomped.
            if (!paths.isEmpty()) {
                Vec3[] path = paths.get(0);
                w.moveTo(path[0].lerp(path[1], 0.5));
                w.tick(2);
                w.expect(stomps > 0, "the shuffling Vanguards walked through the player without stomping");
                w.moveTo(at(-12, 0));
            }
            // Every attack is three hits.
            int before = extraHits;
            bossOutgoing(guards[0], w.player(), 0, AttackType.SLASH);
            w.tick(3);
            w.expect(extraHits - before == CoxRules.VANGUARD_HITS_PER_ATTACK - 1, "a melee Vanguard attack was not three hits");
            // Even damage brings them down together.
            int guard = 0;
            while (!live().isEmpty() && guard++ < 600) {
                if (shelled <= 0) {
                    for (DataBossEntity g : live()) {
                        w.hit(g, Math.max(5, raid.maxHitpoints(g) / 10), AttackType.MAGIC);
                    }
                }
                w.tick(1);
            }
            w.expect(live().isEmpty(), "the Vanguards did not all die (" + live().size() + " left)");
            return resets + " reset(s), " + stomps + " stomp(s), killed evenly";
        }
    }

    // ================================================================== Vespula

    /**
     * Vespula (OSRS wiki). She ignores the party until attacked. Four lux grubs (125 hitpoints) lie around the abyssal
     * portal; while she flies she periodically stings a grub, injecting her eggs, and the stung grub's meter slowly drains
     * until a vespine soldier bursts out, unless medivaemia blossoms from the roots keep it topped up (35 each). Soldiers
     * heal her and the portal while they live; once their health is low they drop to the ground, where only melee harms
     * them, and explode 20 seconds later; the grub then reappears. While she flies only magic, ranged and halberds reach
     * her; at 20% she lands for about 20 seconds. The portal drains 3 prayer points every two ticks from anyone within 7 tiles
     * while it is under attack (3 damage without prayer), and attacking it while she flies enrages her: she stings anyone
     * next to her for 5-20 and stomps anyone under her for 2-8 a tick. Destroying the portal ends the encounter.
     */
    static final class Vespula extends RoomLogic implements CoxTobWalk.Walkable {
        /** Game ticks Vespula stays on the ground. // approx: about 20 seconds */
        private static final int LANDED_TICKS = 33;
        private static final int[][] GRUBS = {{11, -6}, {11, 6}, {16, -6}, {16, 6}};
        private DataBossEntity vespula;
        private DataBossEntity portal;
        private final int[] grubHp = new int[GRUBS.length];
        private final boolean[] stung = new boolean[GRUBS.length];
        private final Mob[] soldiers = new Mob[GRUBS.length];
        private final boolean[] landed = new boolean[GRUBS.length];
        private final int[] fuse = new int[GRUBS.length];
        private final int[] respawnIn = new int[GRUBS.length];
        private int landedTicks;
        private boolean landedOnce;
        private int enragedTicks;
        private boolean provoked;
        private long portalHitAt = Long.MIN_VALUE / 2;
        private int t;
        private int stings;
        private int hatched;
        private int soldierHeals;
        private int portalDrains;

        Vespula(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            for (int i = -1; i <= 1; i += 2) {
                RaidBuilder.pillar(world(), center().offset(4, 0, i * 9), 0, 4, Blocks.JUNGLE_LOG.defaultBlockState());
                RaidBuilder.set(world(), center().offset(4, 5, i * 9), Blocks.BEE_NEST.defaultBlockState());
                RaidBuilder.set(world(), at(-12, i * 14), RaidBuilder.module(RaidsContent.COX_MEDIVAEMIA_ROOT));
            }
            for (int[] g : GRUBS) {
                RaidBuilder.set(world(), at(g[0], g[1]), RaidBuilder.module(RaidsContent.COX_LUX_GRUB));
            }
        }

        @Override
        public void start() {
            java.util.Arrays.fill(grubHp, CoxRules.LUX_GRUB_HP);
            java.util.Arrays.fill(stung, false);
            landedTicks = 0;
            landedOnce = false;
            enragedTicks = 0;
            provoked = false;
            portal = spawnBoss("vespula_portal", at(15, 0));
            if (portal != null) {
                anchor(portal);
                portal.setNoAi(true);
            }
            vespula = spawnBoss("vespula", at(8, 0), false);
        }

        private boolean flying() {
            return alive(vespula) && landedTicks <= 0;
        }

        private int grubAt(BlockPos pos) {
            for (int i = 0; i < GRUBS.length; i++) {
                if (at(GRUBS[i][0], GRUBS[i][1]).equals(pos)) {
                    return i;
                }
            }
            return -1;
        }

        /** The grub slot whose soldier this is, or -1. */
        int soldierSlot(LivingEntity e) {
            for (int i = 0; i < soldiers.length; i++) {
                if (soldiers[i] == e) {
                    return i;
                }
            }
            return -1;
        }

        /** Whether a vespine soldier is on the ground (then only melee harms it; in the air, melee can't reach it). */
        boolean grounded(LivingEntity soldier) {
            int i = soldierSlot(soldier);
            return i >= 0 && landed[i];
        }

        private boolean portalUnderAttack() {
            return t - portalHitAt <= CoxRules.PORTAL_UNDER_ATTACK_TICKS;
        }

        @Override
        public void gameTick(long tick) {
            t++;
            if (!alive(portal)) {
                complete();
                return;
            }
            if (alive(vespula) && !provoked) {
                // OSRS: Vespula isn't aggressive; she only fights back once attacked.
                vespula.setTarget(null);
            }
            if (alive(vespula) && !landedOnce && raid.healthFraction(vespula) <= 0.2) {
                landedOnce = true;
                landedTicks = LANDED_TICKS;
                vespula.setNoAi(true);
                warn("Vespula crashes to the ground, exhausted! Destroy the portal now!");
                CombatAchievementService.flagNear(portal, CombatAchievements.FLAG_VESPULA_LANDED);
            }
            if (landedTicks > 0 && --landedTicks == 0 && alive(vespula)) {
                vespula.setNoAi(false);
                say("Vespula takes to the air again.");
            }
            // While she flies she stings a healthy grub now and then, injecting her eggs.
            if (flying() && t % CoxRules.VESPULA_STING_INTERVAL == 0) {
                List<Integer> healthy = new ArrayList<>();
                for (int i = 0; i < GRUBS.length; i++) {
                    if (grubHp[i] > 0 && !stung[i]) {
                        healthy.add(i);
                    }
                }
                if (!healthy.isEmpty()) {
                    int g = healthy.get(rnd().nextInt(healthy.size()));
                    stung[g] = true;
                    stings++;
                    BlockPos pos = at(GRUBS[g][0], GRUBS[g][1]);
                    world().sendParticles(ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.05);
                    say("Vespula stings a lux grub, injecting her eggs!");
                }
            }
            for (int i = 0; i < GRUBS.length; i++) {
                if (stung[i] && grubHp[i] > 0 && soldiers[i] == null) {
                    grubHp[i] -= CoxRules.GRUB_DRAIN_PER_TICK;
                    if (grubHp[i] <= 0) {
                        hatch(i, at(GRUBS[i][0], GRUBS[i][1]));
                    }
                }
            }
            for (int i = 0; i < GRUBS.length; i++) {
                Mob s = soldiers[i];
                if (s != null) {
                    if (!alive(s)) {
                        soldiers[i] = null;
                        respawnIn[i] = CoxRules.GRUB_RESPAWN_TICKS;
                        continue;
                    }
                    if (!landed[i] && raid.healthFraction(s) <= CoxRules.SOLDIER_LAND_BELOW) {
                        landed[i] = true;
                        fuse[i] = CoxRules.SOLDIER_FUSE_TICKS;
                        s.setNoGravity(false);
                        say("A vespine soldier drops to the ground - finish it in melee before it explodes!");
                    }
                    if (landed[i] && --fuse[i] <= 0) {
                        world().sendParticles(ParticleTypes.EXPLOSION, s.getX(), s.getY() + 0.5, s.getZ(), 2, 0.3, 0.3, 0.3, 0);
                        for (ServerPlayer p : players()) {
                            if (p.distanceToSqr(s) < 2.5 * 2.5) {
                                RaidEffects.hit(s, p, CoxRules.SOLDIER_BLAST_MIN
                                        + rnd().nextInt(CoxRules.SOLDIER_BLAST_MAX - CoxRules.SOLDIER_BLAST_MIN + 1), null, true);
                            }
                        }
                        s.discard();
                        soldiers[i] = null;
                        respawnIn[i] = CoxRules.GRUB_RESPAWN_TICKS;
                    }
                } else if (grubHp[i] <= 0 && respawnIn[i] > 0 && --respawnIn[i] == 0) {
                    grubHp[i] = CoxRules.LUX_GRUB_HP;
                    stung[i] = false;
                    landed[i] = false;
                    RaidBuilder.set(world(), at(GRUBS[i][0], GRUBS[i][1]), RaidBuilder.module(RaidsContent.COX_LUX_GRUB));
                }
            }
            // Vespine soldiers heal Vespula and the portal while they live. // approx: 1% per soldier every 4 ticks
            if (t % 4 == 0) {
                int alive = 0;
                for (Mob s : soldiers) {
                    alive += alive(s) ? 1 : 0;
                }
                if (alive > 0) {
                    for (DataBossEntity b : new DataBossEntity[]{vespula, portal}) {
                        if (alive(b)) {
                            soldierHeals++;
                            raid.heal(b, alive * Math.max(1, raid.maxHitpoints(b) / 100));
                        }
                    }
                }
            }
            // The portal, while under attack: -3 prayer (3 damage without prayer) to anyone within 7 tiles, every two ticks.
            if (portalUnderAttack() && t % CoxRules.PORTAL_INTERVAL == 0) {
                for (ServerPlayer p : players()) {
                    if (p.distanceToSqr(portal) <= CoxRules.PORTAL_RANGE * CoxRules.PORTAL_RANGE) {
                        portalDrains++;
                        var data = Players.data(p);
                        if (data.prayerPoints() > 0) {
                            data.setPrayerPoints(Math.max(0, data.prayerPoints() - CoxRules.PORTAL_DRAIN));
                            com.runecraft.network.ServerNetworking.sync(p);
                        } else {
                            RaidEffects.hit(portal, p, CoxRules.PORTAL_DRAIN, null, true);
                        }
                    }
                }
            }
            // Enraged: she jabs anyone next to her and stomps anyone underneath (2-8 a tick).
            if (enragedTicks > 0) {
                enragedTicks--;
                if (alive(vespula)) {
                    for (ServerPlayer p : players()) {
                        double d = p.distanceToSqr(vespula);
                        if (d < 1.2 * 1.2) {
                            RaidEffects.hit(vespula, p, CoxRules.VESPULA_STOMP_MIN
                                    + rnd().nextInt(CoxRules.VESPULA_STOMP_MAX - CoxRules.VESPULA_STOMP_MIN + 1), null, true);
                        } else if (d < 3 * 3 && t % 2 == 0) { // approx: a sting every other tick as she darts back and forth
                            RaidEffects.hit(vespula, p, CoxRules.VESPULA_ENRAGED_STING_MIN
                                    + rnd().nextInt(CoxRules.VESPULA_ENRAGED_STING_MAX - CoxRules.VESPULA_ENRAGED_STING_MIN + 1), null, true);
                        }
                    }
                }
            }
        }

        private void hatch(int g, BlockPos pos) {
            RaidBuilder.set(world(), pos, Blocks.AIR.defaultBlockState());
            Mob s = spawnMob("vespine_soldier", pos, false);
            soldiers[g] = s;
            landed[g] = false;
            hatched++;
            warn("A lux grub hatches into a vespine soldier! It will heal Vespula and the portal.");
        }

        @Override
        public boolean useBlock(ServerPlayer player, String blockId, BlockPos pos) {
            if (blockId.equals(RaidsContent.COX_MEDIVAEMIA_ROOT)) {
                if (!cooldown(player, 2)) {
                    Inv.give(player, CoxSupplies.BLOSSOM, 1);
                    Msg.action(player, "You pick a medivaemia blossom.");
                }
                return true;
            }
            if (blockId.equals(RaidsContent.COX_LUX_GRUB)) {
                int g = grubAt(pos);
                if (g < 0 || grubHp[g] <= 0) {
                    return true;
                }
                if (!Inv.has(player, CoxSupplies.BLOSSOM, 1)) {
                    Msg.game(player, "The lux grub has " + grubHp[g] + " health" + (stung[g] ? " and Vespula's eggs inside" : "")
                            + ". Medivaemia blossoms from the roots heal it.");
                    return true;
                }
                Inv.remove(player, CoxSupplies.BLOSSOM, 1);
                grubHp[g] = Math.min(CoxRules.LUX_GRUB_HP, grubHp[g] + CoxRules.BLOSSOM_HEAL);
                world().sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0);
                Msg.action(player, "You feed the lux grub a blossom. (" + grubHp[g] + "/" + CoxRules.LUX_GRUB_HP + ")");
                return true;
            }
            return false;
        }

        @Override
        public int bossIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (boss == vespula) {
                provoked = true;
                if (flying() && melee(type) && !Inv.idOf(attacker.getMainHandItem()).contains("halberd")) {
                    Msg.action(attacker, "Vespula is flying out of reach of your melee attacks.");
                    return 0;
                }
            }
            if (boss == portal) {
                portalHitAt = t;
                if (damage > 0) {
                    if (flying()) {
                        if (enragedTicks <= 0) {
                            warn("Vespula is enraged by the attack on her portal!");
                        }
                        enragedTicks = 6;
                    } else if (landedOnce) {
                        CombatAchievementService.flag(attacker, CombatAchievements.FLAG_VESPULA_LANDED);
                    }
                }
            }
            return damage;
        }

        /** OSRS: Vespula isn't aggressive; until someone attacks her, her stings are for the grubs only. */
        @Override
        public int bossOutgoing(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
            return boss == vespula && !provoked ? 0 : damage;
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
            for (int i = 0; i < soldiers.length; i++) {
                if (alive(soldiers[i])) {
                    soldiers[i].discard();
                }
                soldiers[i] = null;
            }
            for (LivingEntity s : living("vespine_soldier")) {
                s.discard();
            }
        }

        @Override
        public String walk(CoxTobWalk w) {
            w.expect(alive(vespula) && alive(portal), "Vespula or her portal did not spawn");
            w.tick(1);
            w.expect(vespula.getTarget() == null, "Vespula attacked before anyone attacked her");
            BlockPos root = w.block(this, RaidsContent.COX_MEDIVAEMIA_ROOT);
            w.moveTo(root.offset(1, 0, 0));
            w.use(root);
            w.expect(w.count(CoxSupplies.BLOSSOM) > 0, "the medivaemia roots gave no blossom");
            w.until(() -> stings > 0, CoxRules.VESPULA_STING_INTERVAL + 2, "Vespula never stung a lux grub");
            int g = -1;
            for (int i = 0; i < GRUBS.length; i++) {
                if (stung[i]) {
                    g = i;
                }
            }
            int stungGrub = g;
            int hp0 = grubHp[stungGrub];
            w.tick(3);
            w.expect(grubHp[stungGrub] < hp0, "a stung lux grub's meter did not drain");
            int before = grubHp[stungGrub];
            w.use(at(GRUBS[stungGrub][0], GRUBS[stungGrub][1]));
            w.expect(grubHp[stungGrub] == Math.min(CoxRules.LUX_GRUB_HP, before + CoxRules.BLOSSOM_HEAL),
                    "a blossom did not heal the lux grub by " + CoxRules.BLOSSOM_HEAL);
            // The portal drains prayer only while it is under attack.
            w.moveTo(portal.position().add(-5, 0, 0));
            w.tick(4);
            w.expect(portalDrains == 0, "the portal drained prayer while nobody attacked it");
            w.hit(portal, 1, AttackType.MAGIC);
            w.tick(2);
            w.expect(portalDrains > 0, "the portal under attack did not drain prayer");
            w.moveTo(at(-10, 0));
            // Leave the grubs: one hatches into a soldier that heals Vespula and the portal.
            w.until(() -> hatched > 0, 2 * CoxRules.LUX_GRUB_HP + 2 * CoxRules.VESPULA_STING_INTERVAL, "no lux grub ever hatched");
            int heals = soldierHeals;
            w.tick(5);
            w.expect(soldierHeals > heals, "a vespine soldier did not heal Vespula and the portal");
            Mob soldier = null;
            for (Mob s : soldiers) {
                if (alive(s)) {
                    soldier = s;
                }
            }
            w.expect(soldier != null, "the hatched vespine soldier is missing");
            w.expect(w.hit(soldier, 20, AttackType.SLASH) == 0, "a flying vespine soldier took melee damage");
            Mob s0 = soldier;
            int guard = 0;
            while (!grounded(s0) && alive(s0) && guard++ < 40) {
                w.hit(s0, Math.max(1, raid.maxHitpoints(s0) / 8), AttackType.RANGED);
                w.tick(1);
            }
            w.expect(grounded(s0), "the vespine soldier never dropped to the ground");
            w.expect(w.hit(s0, 20, AttackType.RANGED) == 0, "a grounded vespine soldier took ranged damage");
            w.kill(s0, AttackType.SLASH, "the grounded vespine soldier");
            w.expect(w.hit(vespula, 20, AttackType.SLASH) == 0, "Vespula took melee damage while flying");
            guard = 0;
            while (!landedOnce && alive(vespula) && guard++ < 200) {
                w.hit(vespula, Math.max(5, raid.maxHitpoints(vespula) / 10), AttackType.RANGED);
                w.tick(1);
            }
            w.expect(provoked, "Vespula never noticed she was attacked");
            w.expect(landedOnce, "Vespula never landed at 20% health");
            w.kill(portal, AttackType.MAGIC, "the abyssal portal");
            return stings + " sting(s), grub healed, " + hatched + " soldier(s) hatched and grounded, portal drained "
                    + portalDrains + " time(s), Vespula landed";
        }
    }

    // ================================================================== Ice demon

    /**
     * Ice demon (OSRS wiki "Ice demon", "Brazier", "Kindling", Mod Ash). Chop kindling from the saplings (a bronze axe and a
     * tinderbox lie in the room; Woodcutting / 12 at most per chop, 28 carried at most); a sapling falls with a 1/X chance
     * (X = max(3, twice the party) x 5 here) and grows back a few seconds later. Lighting a brazier is a Firemaking roll (8%
     * at level 1 to 78% at 99, 32 points); every kindling burnt in one is worth 36 points and 48 Firemaking XP. Lit braziers
     * burn their kindling on a timer, and the burning kindling thaws the demon (54 kindling for a solo team); a brazier that
     * runs out goes dark and must be lit again. One unattackable icefiend per player (up to four) stands by a brazier and
     * tries to take a charge off it every 3 or 4 ticks, succeeding 1 time in 6. Once free the demon takes 67% less damage
     * unless the weapon is demonbane (+15%) or the spell a fire spell (+150%).
     */
    static final class IceDemon extends RoomLogic implements CoxTobWalk.Walkable {
        private static final int[][] BRAZIERS = {{-7, -7}, {7, -7}, {-7, 7}, {7, 7}};
        private DataBossEntity demon;
        private int heat;
        private int needed;
        private boolean thawed;
        /** Kindling burning in each brazier (0: dark). */
        private final int[] charges = new int[BRAZIERS.length];
        private final Mob[] fiends = new Mob[BRAZIERS.length];
        private final int[] fiendDelay = new int[BRAZIERS.length];
        /** Felled saplings and the raid tick they grow back. */
        private final Map<BlockPos, Long> stumps = new HashMap<>();
        /** OSRS: the braziers are worth up to max(6,000, 10 x the party's total Firemaking levels) points. */
        private int pointsCap;
        private int pointsGiven;
        private int t;
        private int felled;
        private int fiendTakes;
        private int fiendTries;
        private int lightings;

        IceDemon(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            for (int[] b : BRAZIERS) {
                RaidBuilder.set(world(), at(b[0], b[1]), RaidBuilder.module(RaidsContent.BRAZIER));
            }
            for (int x = -12; x <= 12; x += 8) {
                RaidBuilder.set(world(), at(x, -18), RaidBuilder.module(RaidsContent.SAPLING));
                RaidBuilder.set(world(), at(x, 18), RaidBuilder.module(RaidsContent.SAPLING));
            }
            RaidBuilder.set(world(), at(-14, 4), RaidBuilder.module(RaidsContent.COX_TOOLS));
            ice(Blocks.BLUE_ICE.defaultBlockState());
        }

        private void ice(BlockState state) {
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    RaidBuilder.pillar(world(), center().offset(sx * 2, 0, sz * 2), 0, 3, state);
                }
            }
        }

        private int brazierAt(BlockPos pos) {
            for (int i = 0; i < BRAZIERS.length; i++) {
                if (at(BRAZIERS[i][0], BRAZIERS[i][1]).equals(pos)) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public void start() {
            needed = CoxRules.iceDemonKindling(raid.partySize());
            int firemaking = 0;
            for (ServerPlayer p : raid.players()) {
                firemaking += level(p, Skill.FIREMAKING);
            }
            pointsCap = RaidScaling.coxIceDemonPointsCap(firemaking);
            demon = spawnBoss("cox_ice_demon", at(0, 0));
            if (demon != null) {
                demon.setNoAi(true);
                anchor(demon);
            }
            for (int i = 0; i < CoxRules.icefiends(raid.partySize()); i++) {
                int[] b = BRAZIERS[i];
                Mob f = spawnMob("cox_icefiend", at(b[0] + Integer.signum(b[0]), b[1] + Integer.signum(b[1])), false);
                if (f != null) {
                    f.setNoAi(true);
                    f.setInvulnerable(true);
                    anchor(f);
                    fiends[i] = f;
                    fiendDelay[i] = CoxRules.icefiendDelay(rnd());
                }
            }
        }

        @Override
        public void reset() {
            super.reset();
            heat = 0;
            thawed = false;
            java.util.Arrays.fill(charges, 0);
        }

        @Override
        public boolean bossVulnerable(DataBossEntity boss) {
            return thawed;
        }

        @Override
        public int bossIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            boolean demonbane = Inv.idOf(attacker.getMainHandItem()).matches(".*(" + CombatAchievements.DEMONBANE + ").*");
            com.runecraft.osrs.Spell spell = Players.data(attacker).autocast();
            boolean fire = type == AttackType.MAGIC && spell != null && spell.name().contains("FIRE");
            return CoxRules.iceDemonDamage(damage, demonbane, fire);
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            t++;
            long now = RaidEffects.now();
            for (Map.Entry<BlockPos, Long> e : new ArrayList<>(stumps.entrySet())) {
                if (now >= e.getValue()) {
                    stumps.remove(e.getKey());
                    RaidBuilder.set(world(), e.getKey(), RaidBuilder.module(RaidsContent.SAPLING));
                }
            }
            if (thawed) {
                return;
            }
            if (alive(demon) && tick % 3 == 0) {
                world().sendParticles(ParticleTypes.SNOWFLAKE, demon.getX(), demon.getY() + 1.5, demon.getZ(), 8, 1, 1, 1, 0.01);
            }
            for (int i = 0; i < BRAZIERS.length; i++) {
                if (charges[i] <= 0) {
                    continue;
                }
                // The icefiend at this brazier tries to take a charge off it.
                if (alive(fiends[i]) && --fiendDelay[i] <= 0) {
                    fiendDelay[i] = CoxRules.icefiendDelay(rnd());
                    fiendTries++;
                    if (rnd().nextInt(CoxRules.ICEFIEND_SUCCESS) == 0) {
                        charges[i]--;
                        fiendTakes++;
                        BlockPos p = at(BRAZIERS[i][0], BRAZIERS[i][1]);
                        world().sendParticles(ParticleTypes.SNOWFLAKE, p.getX() + 0.5, p.getY() + 1.2, p.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
                    }
                }
                if (charges[i] > 0 && t % CoxRules.BRAZIER_BURN_TICKS == 0) {
                    charges[i]--;
                    heat++;
                }
                if (charges[i] <= 0) {
                    BlockPos p = at(BRAZIERS[i][0], BRAZIERS[i][1]);
                    RaidBuilder.set(world(), p.above(), Blocks.AIR.defaultBlockState());
                    say("A brazier burns out.");
                }
            }
            if (heat >= needed && alive(demon)) {
                thaw();
            }
        }

        private void thaw() {
            thawed = true;
            ice(Blocks.AIR.defaultBlockState());
            for (int i = 0; i < BRAZIERS.length; i++) {
                if (alive(fiends[i])) {
                    fiends[i].discard();
                }
            }
            demon.setNoAi(false);
            unanchor(demon);
            warn("The ice cracks - the Ice demon breaks free!");
        }

        private void award(ServerPlayer player, int points) {
            int give = Math.max(0, Math.min(points, pointsCap - pointsGiven));
            pointsGiven += give;
            raid.awardActionPoints(player, give);
        }

        @Override
        public boolean useBlock(ServerPlayer player, String blockId, BlockPos pos) {
            if (blockId.equals(RaidsContent.COX_TOOLS)) {
                toolSpawn(player, "bronze_axe", "tinderbox");
                return true;
            }
            if (blockId.equals(RaidsContent.SAPLING)) {
                if (cooldown(player, 2)) {
                    return true;
                }
                if (!hasTool(player, "_axe")) {
                    Msg.game(player, "You need an axe to cut the sapling. There's a bronze axe by the entrance.");
                    return true;
                }
                int have = Inv.count(player, "cox_kindling");
                if (have >= CoxRules.KINDLING_CAP) {
                    Msg.game(player, "You can't carry any more kindling.");
                    return true;
                }
                player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                int wc = level(player, Skill.WOODCUTTING);
                if (rnd().nextDouble() < CoxRules.saplingChopChance(wc)) {
                    int n = Math.min(CoxRules.kindlingPerChop(wc, rnd()), CoxRules.KINDLING_CAP - have);
                    Inv.give(player, "cox_kindling", n);
                    XpService.add(player, Skill.WOODCUTTING, CoxRules.kindlingXp(n));
                    Msg.action(player, "You cut some kindling.");
                    if (rnd().nextInt(CoxRules.saplingDepletion(raid.partySize(), true)) == 0) {
                        felled++;
                        stumps.put(pos.immutable(), RaidEffects.now() + CoxRules.SAPLING_RESPAWN_TICKS);
                        RaidBuilder.set(world(), pos, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
                        Msg.game(player, "The sapling falls. It will grow back shortly.");
                    }
                }
                return true;
            }
            if (blockId.equals(RaidsContent.BRAZIER)) {
                int b = brazierAt(pos);
                if (b < 0) {
                    return true;
                }
                if (thawed) {
                    Msg.game(player, "The Ice demon is already free.");
                    return true;
                }
                int n = Inv.count(player, "cox_kindling");
                if (n <= 0) {
                    Msg.game(player, "You need some kindling to fuel the brazier.");
                    return true;
                }
                if (charges[b] <= 0) {
                    if (!Inv.has(player, "tinderbox", 1)) {
                        Msg.game(player, "You need a tinderbox to light the brazier. There's one by the entrance.");
                        return true;
                    }
                    if (cooldown(player, 2)) {
                        return true;
                    }
                    if (rnd().nextDouble() >= CoxRules.brazierLightChance(level(player, Skill.FIREMAKING))) {
                        Msg.action(player, "You attempt to light the brazier...");
                        return true;
                    }
                    lightings++;
                    award(player, CoxRules.BRAZIER_LIGHT_POINTS);
                    RaidBuilder.set(world(), pos.above(), Blocks.CAMPFIRE.defaultBlockState());
                    Msg.action(player, "The brazier catches light.");
                }
                Inv.remove(player, "cox_kindling", n);
                XpService.add(player, Skill.FIREMAKING, n * CoxRules.KINDLING_FIREMAKING_XP);
                award(player, n * CoxRules.KINDLING_POINTS);
                charges[b] += n;
                world().sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 20, 0.3, 0.5, 0.3, 0.02);
                say("The braziers warm the room... (" + Math.min(heat, needed) + "/" + needed + " burnt)");
                return true;
            }
            return false;
        }

        @Override
        public void cleanup() {
            super.cleanup();
            for (Mob f : fiends) {
                if (alive(f)) {
                    f.discard();
                }
            }
        }

        @Override
        public String walk(CoxTobWalk w) {
            w.expect(!bossVulnerable(demon), "the Ice demon could be attacked while frozen");
            int wantFiends = CoxRules.icefiends(raid.partySize());
            int fiendCount = 0;
            for (Mob f : fiends) {
                fiendCount += alive(f) ? 1 : 0;
            }
            w.expect(fiendCount == wantFiends, "expected " + wantFiends + " icefiend(s), found " + fiendCount);
            w.expect(w.hit(fiends[0], 10, AttackType.SLASH) == 0 && alive(fiends[0]), "an icefiend could be harmed");
            w.use(w.block(this, RaidsContent.COX_TOOLS));
            w.expect(w.count("bronze_axe") > 0 && w.count("tinderbox") > 0, "the room's tools gave no bronze axe and tinderbox");
            List<BlockPos> saplings = w.blocks(this, RaidsContent.SAPLING);
            List<BlockPos> braziers = w.blocks(this, RaidsContent.BRAZIER);
            w.expect(braziers.size() == 4, "expected four braziers, found " + braziers.size());
            BlockPos fiendBrazier = at(BRAZIERS[0][0], BRAZIERS[0][1]);
            // Chop up to the 28 kindling cap; saplings fall now and then and grow back.
            int chops = 0;
            while (w.count("cox_kindling") < CoxRules.KINDLING_CAP && chops++ < 400) {
                BlockPos s = saplings.get(chops % saplings.size());
                if (stumps.containsKey(s)) {
                    continue;
                }
                w.moveTo(s.offset(1, 0, 0));
                w.use(s);
                w.tick(2);
            }
            w.expect(w.count("cox_kindling") == CoxRules.KINDLING_CAP, "chopping stopped at " + w.count("cox_kindling") + " kindling");
            BlockPos s0 = saplings.get(0);
            w.moveTo(s0.offset(1, 0, 0));
            w.use(s0);
            w.tick(2);
            w.expect(w.count("cox_kindling") == CoxRules.KINDLING_CAP, "a player carrying 28 kindling chopped more");
            // Light the icefiend's brazier and fill it: the icefiend takes charges off it.
            int tries = 0;
            while (charges[0] <= 0 && tries++ < 200) {
                w.moveTo(fiendBrazier.offset(-1, 0, 0));
                w.use(fiendBrazier);
                w.tick(2);
            }
            w.expect(charges[0] > 0 && lightings > 0, "the brazier was never lit");
            int guard = 0;
            while (!thawed && guard++ < 1500) {
                if (w.count("cox_kindling") == 0) {
                    BlockPos s = saplings.get(guard % saplings.size());
                    if (!stumps.containsKey(s)) {
                        w.moveTo(s.offset(1, 0, 0));
                        w.use(s);
                    }
                } else {
                    // Keep feeding the icefiend's brazier (lighting it again whenever it burns out).
                    w.moveTo(fiendBrazier.offset(-1, 0, 0));
                    w.use(fiendBrazier);
                }
                w.tick(2);
            }
            w.expect(thawed, "the Ice demon never thawed (" + heat + "/" + needed + " burnt, " + fiendTakes + " taken by icefiends)");
            w.expect(fiendTries > 0, "the icefiend never tried to put out its brazier");
            for (Mob f : fiends) {
                w.expect(!alive(f), "an icefiend outlived the thaw");
            }
            if (felled > 0) {
                w.tick(CoxRules.SAPLING_RESPAWN_TICKS + 1);
                w.expect(stumps.isEmpty(), "a felled sapling never grew back");
            }
            w.kill(demon, AttackType.MAGIC, "the Ice demon");
            return needed + " kindling burnt, " + chops + " chops, " + felled + " sapling(s) felled, " + fiendTakes + "/"
                    + fiendTries + " icefiend tries took a charge";
        }
    }

    // ================================================================== Thieving

    /**
     * Thieving (OSRS wiki "Corrupted scavenger", "Chest", "Trough"): the corrupted scavenger must be fed cavern grubs from
     * the room's chests. Each attempt at a chest (one a tick) opens it by the skilling roll, 39% at level 1 to 61% at 99, 21
     * points better with a lockpick or hair clip; an opened chest holds 0 to Thieving / 25 grubs, at least 1 (2 from 95), and
     * locks itself shortly after, except three poisoned ones (1-3 damage to anyone next to it, no poison) and one that holds
     * a one-off batch of psykk bats. The scavenger eats the grubs from the trough and needs 30 solo, 16 per player in a team;
     * left without food for 60 seconds it grows hungry again. Every grub fed is worth 115 points, up to max(4,500, total
     * Thieving / 6 x 150). The sleeping scavenger leaves 5 stinkhorn, 5 endarkened juice and 4 cicely.
     */
    static final class Thieving extends RoomLogic implements CoxTobWalk.Walkable {
        private static final int GRUBS = 0;
        private static final int POISON = 1;
        private static final int BATS = 2;
        private final Map<BlockPos, Integer> chests = new HashMap<>();
        private final Map<BlockPos, Integer> openedAt = new HashMap<>();
        private boolean batsTaken;
        private int needed;
        /** Grubs waiting in the trough, and how full the scavenger is. */
        private int trough;
        private int eaten;
        private int lastMeal;
        private int pointGrubs;
        private int pointGrubsGiven;
        private int t;
        private int attempts;
        private int hungerLost;

        Thieving(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            List<BlockPos> spots = new ArrayList<>();
            for (int x = -12; x <= 6; x += 6) {
                for (int z = -12; z <= 12; z += 8) {
                    BlockPos p = at(x, z);
                    RaidBuilder.set(world(), p, RaidBuilder.module(RaidsContent.GRUBBY_CHEST));
                    spots.add(p);
                }
            }
            java.util.Collections.shuffle(spots, new java.util.Random(rnd().nextLong()));
            for (int i = 0; i < spots.size(); i++) {
                chests.put(spots.get(i), i < CoxRules.POISON_CHESTS ? POISON : i == CoxRules.POISON_CHESTS ? BATS : GRUBS);
            }
            RaidBuilder.set(world(), at(14, 0), RaidBuilder.module(RaidsContent.TROUGH));
            RaidBuilder.set(world(), at(15, 0), Blocks.HAY_BLOCK.defaultBlockState());
        }

        @Override
        public void start() {
            needed = CoxRules.troughGrubs(raid.partySize());
            int thieving = 0;
            for (ServerPlayer p : raid.players()) {
                thieving += level(p, Skill.THIEVING);
            }
            pointGrubs = CoxRules.pointGrubs(thieving);
        }

        @Override
        public void gameTick(long tick) {
            t++;
            if (done) {
                return;
            }
            if (trough > 0) {
                if (t % CoxRules.SCAVENGER_EAT_TICKS == 0) {
                    trough--;
                    eaten++;
                    lastMeal = t;
                    if (eaten >= needed) {
                        say("The corrupted scavenger has eaten its fill and falls asleep.");
                        Vec3 at = vec(12, 0);
                        dropItem(world(), at, CoxSupplies.STINKHORN, CoxRules.THIEVING_STINKHORN);
                        dropItem(world(), at, CoxSupplies.ENDARKENED, CoxRules.THIEVING_ENDARKENED);
                        dropItem(world(), at, CoxSupplies.CICELY, CoxRules.THIEVING_CICELY);
                        complete();
                    }
                }
            } else if (eaten > 0 && t - lastMeal > CoxRules.SCAVENGER_HUNGER_TICKS && t % CoxRules.SCAVENGER_HUNGER_RATE == 0) {
                // OSRS: left without food for 60 seconds, the scavenger grows hungry again.
                eaten--;
                hungerLost++;
            }
        }

        @Override
        public boolean useBlock(ServerPlayer player, String blockId, BlockPos pos) {
            if (blockId.equals(RaidsContent.GRUBBY_CHEST)) {
                if (cooldown(player, 1)) {
                    return true;
                }
                Integer kind = chests.get(pos);
                if (kind == null) {
                    return true;
                }
                Integer opened = openedAt.get(pos);
                if (opened != null && (kind == POISON || t - opened < CoxRules.CHEST_RELOCK_TICKS)) {
                    Msg.game(player, kind == POISON ? "The chest is locked tight." : "The chest is empty. It will lock itself again soon.");
                    return true;
                }
                player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                attempts++;
                boolean pick = hasTool(player, "lockpick") || hasTool(player, "hair_clip");
                if (rnd().nextDouble() >= CoxRules.chestOpenChance(level(player, Skill.THIEVING), pick)) {
                    Msg.action(player, "You fail to pick the lock.");
                    return true;
                }
                openedAt.put(pos, t);
                XpService.add(player, Skill.THIEVING, 30); // approx
                if (kind == POISON) {
                    Msg.red(player, "Poison gas bursts out of the chest!");
                    world().sendParticles(ParticleTypes.SNEEZE, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 20, 0.8, 0.5, 0.8, 0);
                    for (ServerPlayer p : players()) {
                        if (p.distanceToSqr(Vec3.atCenterOf(pos)) < 2.5 * 2.5) {
                            RaidEffects.hit(null, p, 1 + rnd().nextInt(3), null, true);
                        }
                    }
                } else if (kind == BATS && !batsTaken) {
                    batsTaken = true;
                    int n = CoxRules.batChest(raid.partySize());
                    Inv.give(player, "psykk_bat", n);
                    Msg.game(player, "You find " + n + " psykk bats in the chest!");
                } else {
                    int n = CoxRules.grubs(level(player, Skill.THIEVING), rnd());
                    Inv.give(player, "cavern_grubs", n);
                    Msg.game(player, "You find " + n + " cavern grub" + (n > 1 ? "s" : "") + " in the chest.");
                }
                return true;
            }
            if (blockId.equals(RaidsContent.TROUGH)) {
                int n = Inv.count(player, "cavern_grubs");
                if (n <= 0) {
                    Msg.game(player, "The scavenger is hungry for cavern grubs. (" + eaten + "/" + needed + ")");
                    return true;
                }
                Inv.remove(player, "cavern_grubs", n);
                int scoring = Math.max(0, Math.min(n, pointGrubs - pointGrubsGiven));
                pointGrubsGiven += scoring;
                raid.awardActionPoints(player, scoring * CoxRules.GRUB_POINTS);
                trough += n;
                lastMeal = t;
                say(player.getName().getString() + " fills the trough with " + n + " grubs. (" + Math.min(eaten + trough, needed) + "/"
                        + needed + ")");
                return true;
            }
            return false;
        }

        @Override
        public String walk(CoxTobWalk w) {
            List<BlockPos> list = w.blocks(this, RaidsContent.GRUBBY_CHEST);
            w.expect(list.size() == chests.size() && !list.isEmpty(), "the grubby chests are missing");
            BlockPos trough = w.block(this, RaidsContent.TROUGH);
            int opens = 0;
            int i = 0;
            boolean hungerChecked = false;
            while (!done && opens++ < 2000) {
                BlockPos chest = list.get(i++ % list.size());
                w.moveTo(chest.offset(1, 0, 0));
                w.use(chest);
                w.tick(1);
                if (done) {
                    // The scavenger ate its last grub this tick and fell asleep: the room is clear and the trough takes
                    // no more grubs (the walk keeps feeding while it eats, up to the scoring cap), so any grubs still
                    // carried are left over.
                    break;
                }
                int held = w.count("cavern_grubs");
                if (held >= 6 || eaten + this.trough + held >= needed) {
                    w.moveTo(trough.offset(-1, 0, 0));
                    int before = raid.points(w.player().getUUID());
                    int scoring = Math.max(0, Math.min(held, pointGrubs - pointGrubsGiven));
                    w.use(trough);
                    w.expect(raid.points(w.player().getUUID()) - before == scoring * CoxRules.GRUB_POINTS
                            || before + scoring * CoxRules.GRUB_POINTS >= RaidScaling.COX_POINTS_CAP,
                            "feeding " + held + " grubs gave " + (raid.points(w.player().getUUID()) - before) + " points, expected "
                                    + scoring * CoxRules.GRUB_POINTS);
                    if (!hungerChecked && eaten + this.trough < needed) {
                        // Left without food for a minute, the scavenger grows hungry again.
                        hungerChecked = true;
                        w.until(() -> this.trough == 0, 40, "the scavenger never ate the grubs in the trough");
                        int full = eaten;
                        w.tick(CoxRules.SCAVENGER_HUNGER_TICKS + 3 * CoxRules.SCAVENGER_HUNGER_RATE);
                        w.expect(eaten < full && hungerLost > 0, "the scavenger did not grow hungry after a minute without food");
                    }
                }
            }
            w.until(() -> done, 60, "the corrupted scavenger was not fed (" + eaten + "/" + needed + ")");
            return needed + " grubs, " + pointGrubsGiven + "/" + pointGrubs + " scored, " + attempts + " lock attempts, hunger -"
                    + hungerLost + (batsTaken ? ", psykk bats found" : "");
        }
    }

    // ================================================================== Tightrope

    /**
     * Tightrope (OSRS): deathly mages and rangers guard a chasm; crossing the rope needs 80-100% of the team's average
     * Agility, and anyone crossing while they aren't fighting takes all their hits at once. The keystone crystal on the far
     * side (2,000 points) dispels the shimmering barrier, which kills the surviving guards and opens the way.
     */
    static final class Tightrope extends RoomLogic implements CoxTobWalk.Walkable {
        private int agility;
        private boolean keystoneTaken;
        private boolean crossed;
        /** Deathly mages and rangers the party killed (CA "No Time for Death": clear the room without killing any). */
        private int guardKills;
        private boolean noKillsFlagged;

        Tightrope(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            BlockPos c = center();
            int y = c.getY();
            int h = RaidBuilder.HALF;
            RaidBuilder.fill(world(), c.getX() - 3, y - 4, c.getZ() - h, c.getX() + 3, y - 4, c.getZ() + h,
                    Blocks.OBSIDIAN.defaultBlockState());
            RaidBuilder.fill(world(), c.getX() - 3, y - 3, c.getZ() - h, c.getX() + 3, y - 3, c.getZ() + h,
                    Blocks.MAGMA_BLOCK.defaultBlockState());
            RaidBuilder.fill(world(), c.getX() - 3, y - 2, c.getZ() - h, c.getX() + 3, y, c.getZ() + h, Blocks.AIR.defaultBlockState());
            RaidBuilder.fill(world(), c.getX() - 3, y, c.getZ(), c.getX() + 3, y, c.getZ(),
                    Blocks.CHAIN.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
            RaidBuilder.set(world(), at(-5, 1), RaidBuilder.module(RaidsContent.TIGHTROPE));
            RaidBuilder.fill(world(), c.getX() + 8, y + 1, c.getZ() - 9, c.getX() + 12, y + 1, c.getZ() + 9,
                    Blocks.PURPUR_SLAB.defaultBlockState());
            RaidBuilder.set(world(), at(15, -3), RaidBuilder.module(RaidsContent.COX_KEYSTONE));
            RaidBuilder.set(world(), at(18, 0), RaidBuilder.module(RaidsContent.COX_SHIMMERING_BARRIER));
        }

        @Override
        public void start() {
            agility = CoxRules.tightropeLevel(averageLevel(raid, Skill.AGILITY), rnd());
            int pairs = raid.partySize() > 2 ? 2 : 1;
            for (int i = 0; i < pairs; i++) {
                spawnMob("deathly_ranger", at(10, -7 + i * 3).above(), true);
                spawnMob("deathly_mage", at(10, 7 - i * 3).above(), true);
            }
        }

        @Override
        public void gameTick(long tick) {
            for (ServerPlayer p : raid.activePlayers()) {
                double dx = p.getX() - (center().getX() + 0.5);
                double dz = p.getZ() - (center().getZ() + 0.5);
                if (Math.abs(dx) > RaidBuilder.HALF + 0.5 || Math.abs(dz) > RaidBuilder.HALF + 0.5) {
                    continue;
                }
                if (p.getY() < center().getY() - 0.5 && Math.abs(dx) < 5) {
                    raid.teleport(p, at(-6, 0));
                    RaidEffects.hit(null, p, 5, null, true);
                    Msg.red(p, "You fall into the chasm and climb back out.");
                }
            }
        }

        @Override
        public boolean useBlock(ServerPlayer player, String blockId, BlockPos pos) {
            if (blockId.equals(RaidsContent.TIGHTROPE)) {
                if (cooldown(player, 3)) {
                    return true;
                }
                if (level(player, Skill.AGILITY) < agility) {
                    Msg.game(player, "You need an Agility level of " + agility + " to cross the tightrope.");
                    return true;
                }
                raid.teleport(player, at(5, 0));
                XpService.add(player, Skill.AGILITY, 40); // approx
                crossed = true;
                Msg.game(player, "You carefully cross the tightrope.");
                // OSRS: guards that aren't already fighting focus the crosser and land all their hits at once.
                for (LivingEntity e : spawned) {
                    if (alive(e) && (!(e instanceof Mob m) || m.getTarget() != player)) {
                        RaidEffects.hit(e, player, rnd().nextInt(19), e.getType() == com.runecraft.registry.ModEntities.typeById("deathly_mage")
                                ? AttackType.MAGIC : AttackType.RANGED, false);
                        if (e instanceof Mob m) {
                            m.setTarget(player);
                        }
                    }
                }
                return true;
            }
            if (blockId.equals(RaidsContent.COX_KEYSTONE)) {
                // The crystal stays: a keystone lost to a leaver, the chasm or despawning can be taken again, so the barrier
                // can always be dispelled. Only the first one is worth points (OSRS: 2,000).
                if (Inv.has(player, CoxSupplies.KEYSTONE, 1)) {
                    Msg.game(player, "You already have a keystone crystal. Use it on the shimmering barrier.");
                    return true;
                }
                Inv.give(player, CoxSupplies.KEYSTONE, 1);
                if (!keystoneTaken) {
                    keystoneTaken = true;
                    raid.awardActionPoints(player, RaidScaling.COX_TIGHTROPE_POINTS);
                }
                Msg.game(player, "You take the keystone crystal. Use it on the shimmering barrier.");
                return true;
            }
            if (blockId.equals(RaidsContent.COX_SHIMMERING_BARRIER)) {
                if (!Inv.has(player, CoxSupplies.KEYSTONE, 1)) {
                    Msg.game(player, "A shimmering barrier blocks the way. Perhaps the crystal across the chasm dispels it.");
                    return true;
                }
                Inv.remove(player, CoxSupplies.KEYSTONE, 1);
                RaidBuilder.set(world(), pos, Blocks.AIR.defaultBlockState());
                if (guardKills == 0) {
                    noKillsFlagged = true;
                    for (ServerPlayer p : players()) {
                        CombatAchievementService.flag(p, CombatAchievements.FLAG_COX_TIGHTROPE_NO_KILLS);
                    }
                }
                int killed = 0;
                for (LivingEntity e : spawned) {
                    if (alive(e)) {
                        e.kill();
                        killed++;
                    }
                }
                say("The keystone dispels the barrier" + (killed > 0 ? " and the remaining guards crumble." : "."));
                complete();
                return true;
            }
            return false;
        }

        @Override
        public void onCreatureHit(ServerPlayer player, LivingEntity target, int damage) {
            if (spawned.contains(target) && (!target.isAlive() || target.getHealth() <= 0)) {
                guardKills++;
            }
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
        public String walk(CoxTobWalk w) {
            BlockPos rope = w.block(this, RaidsContent.TIGHTROPE);
            w.moveTo(rope.offset(-1, 0, 0));
            w.use(rope);
            w.expect(crossed && w.player().getX() > center().getX() + 3, "the player did not cross the tightrope (needs Agility " + agility + ")");
            BlockPos keystone = w.block(this, RaidsContent.COX_KEYSTONE);
            w.moveTo(keystone.offset(1, 0, 0));
            int before = raid.points(w.player().getUUID());
            w.use(keystone);
            w.expect(w.count(CoxSupplies.KEYSTONE) == 1, "the keystone crystal was not taken");
            w.expect(raid.points(w.player().getUUID()) > before || before >= RaidScaling.COX_POINTS_CAP, "the keystone crystal gave no points");
            // A lost crystal (left in the chasm, stripped from a leaver) can be taken again, for no more points.
            w.take(CoxSupplies.KEYSTONE);
            int after = raid.points(w.player().getUUID());
            w.use(keystone);
            w.use(keystone);
            w.expect(w.count(CoxSupplies.KEYSTONE) == 1, "a lost keystone crystal could not be taken again (or two were given)");
            w.expect(raid.points(w.player().getUUID()) == after, "a second keystone crystal gave points again");
            BlockPos barrier = w.block(this, RaidsContent.COX_SHIMMERING_BARRIER);
            w.moveTo(barrier.offset(-1, 0, 0));
            w.use(barrier);
            w.expect(done && allDead(), "the keystone did not dispel the barrier and the guards");
            w.expect(noKillsFlagged, "clearing the room without killing a guard did not count for No Time for Death");
            return "crossed at Agility " + agility + ", keystone used, no guards killed";
        }
    }

    // ================================================================== Crystal crabs

    /**
     * Crystal crabs (OSRS wiki "Crab puzzle", "Jewelled Crab", "Energy focus"): a carved sculpture on one wall sends one white
     * orb at a time across the room. Four crystals must be lit: the black one facing the sculpture takes the white orb itself;
     * the yellow, cyan and magenta crystals on the wall to the orb's right take a blue, red and green orb. Striking a crab
     * colours it for about five seconds (red melee, blue magic, green ranged). An orb that meets a crab bounces off it,
     * always turning right from its own point of view: a white orb takes the crab's colour, a coloured orb keeps its colour
     * off a crab of the same colour and disappears against any other. Every layout needs three crabs (one in the orb's lane
     * at each coloured crystal's row); bigger parties get more crabs, which must be lured out of the orb's way. A player the
     * orb runs into takes 10-30 damage. A hammer (one lies in the room) or a blunt weapon stuns a crab in place (50-60 ticks
     * solo, less for bigger parties). The room is worth 1,600 points, 400 per crystal.
     */
    static final class CrystalCrabs extends RoomLogic implements CoxTobWalk.Walkable {
        private final boolean[] lit = new boolean[4];
        private final Map<LivingEntity, CoxRules.Colour> colour = new HashMap<>();
        private final Map<LivingEntity, Integer> colourTicks = new HashMap<>();
        private final Map<LivingEntity, Integer> stunTicks = new HashMap<>();
        private final Map<LivingEntity, Vec3> stunAt = new HashMap<>();
        private final Map<LivingEntity, UUID> striker = new HashMap<>();
        private CoxRules.CrabLayout layout;
        /** The orb (relative to the room centre, in blocks), its heading and colour; null while none is out. */
        private Vec3 orb;
        private int orbDx;
        private int orbDz;
        private CoxRules.Colour orbColour;
        private UUID orbStriker;
        private LivingEntity lastCrab;
        private int wanted;
        private int t;
        private int orbs;
        private int bounces;
        /** Orbs lost after the first crystal lit (CA "Playing with Lasers"). */
        private int wasted;
        /** How the last orb was lost before reaching a crystal (self-test diagnostics), or null. */
        private String lost;

        CrystalCrabs(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        private static BlockState crystalBlock(int i, boolean on) {
            if (on) {
                return Blocks.SEA_LANTERN.defaultBlockState();
            }
            return switch (i) {
                case 0 -> Blocks.YELLOW_STAINED_GLASS.defaultBlockState();
                case 1 -> Blocks.CYAN_STAINED_GLASS.defaultBlockState();
                case 2 -> Blocks.MAGENTA_STAINED_GLASS.defaultBlockState();
                default -> Blocks.BLACK_STAINED_GLASS.defaultBlockState();
            };
        }

        /** Crystal {@code i}: 0-2 the side crystals (yellow, cyan, magenta), 3 the black crystal facing the sculpture. */
        BlockPos crystal(int i) {
            int[] c = layout.crystal(i);
            return at(c[0], c[1]);
        }

        /** Where a crab must stand in the orb's lane to bounce it into side crystal {@code i}. */
        Vec3 turnPoint(int i) {
            return vec(0, layout.rows()[i]);
        }

        boolean lit(int i) {
            return lit[i];
        }

        private void paintCrystal(int i) {
            RaidBuilder.set(world(), crystal(i), crystalBlock(i, lit[i]));
            RaidBuilder.set(world(), crystal(i).above(), crystalBlock(i, lit[i]));
        }

        @Override
        public void decorate() {
            layout = CoxRules.crabLayout(rnd());
            for (int i = 0; i < 4; i++) {
                paintCrystal(i);
            }
            int[] s = layout.start();
            BlockPos sculpture = at(0, s[1] - layout.lane());
            RaidBuilder.set(world(), sculpture, Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState());
            RaidBuilder.set(world(), sculpture.above(), Blocks.END_ROD.defaultBlockState());
            for (int i = 0; i < 3; i++) {
                RaidBuilder.set(world(), at(0, layout.rows()[i]).below(), Blocks.GLOWSTONE.defaultBlockState());
            }
            RaidBuilder.set(world(), at(-14, -4 * layout.lane()), RaidBuilder.module(RaidsContent.COX_TOOLS));
        }

        @Override
        public void start() {
            java.util.Arrays.fill(lit, false);
            for (int i = 0; i < 4; i++) {
                paintCrystal(i);
            }
            colour.clear();
            stunTicks.clear();
            orb = null;
            lost = null;
            wanted = CoxRules.crabCount(raid.partySize());
            for (int i = 0; i < wanted; i++) {
                spawnCrab();
            }
        }

        private void spawnCrab() {
            int side = rnd().nextBoolean() ? 1 : -1;
            spawnMob("cox_crystal_crab", at(side * (6 + rnd().nextInt(8)), rnd().nextInt(25) - 12), true);
        }

        List<LivingEntity> crabs() {
            return living("cox_crystal_crab");
        }

        CoxRules.Colour colourOf(LivingEntity crab) {
            return colour.getOrDefault(crab, CoxRules.Colour.WHITE);
        }

        boolean stunned(LivingEntity crab) {
            return stunTicks.getOrDefault(crab, 0) > 0;
        }

        /** A player struck a crab: blunt melee stuns it (and colours it red), any other hit colours it by style. */
        void struck(ServerPlayer player, LivingEntity crab, AttackType type) {
            raid.setCurrentHitpoints(crab, raid.maxHitpoints(crab));
            String weapon = Inv.idOf(player.getMainHandItem());
            boolean blunt = melee(type) && (weapon.equals("hammer") || weapon.contains("warhammer") || weapon.contains("maul")
                    || weapon.contains("bludgeon") || weapon.contains("torags_hammers"));
            if (blunt) {
                stunTicks.put(crab, CoxRules.crabStunTicks(raid.partySize(), rnd()));
                stunAt.put(crab, crab.position());
                Msg.action(player, "You stun the crystal crab.");
            }
            colour.put(crab, CoxRules.crabColour(type));
            colourTicks.put(crab, CoxRules.CRAB_COLOUR_TICKS);
            striker.put(crab, player.getUUID());
            world().sendParticles(look(CoxRules.crabColour(type)), crab.getX(), crab.getY() + 0.6, crab.getZ(), 12, 0.4, 0.3, 0.4, 0.02);
        }

        private static ParticleOptions look(CoxRules.Colour c) {
            return switch (c) {
                case WHITE -> ParticleTypes.END_ROD;
                case BLUE -> ParticleTypes.SOUL_FIRE_FLAME;
                case GREEN -> ParticleTypes.HAPPY_VILLAGER;
                default -> ParticleTypes.FLAME;
            };
        }

        @Override
        public boolean useBlock(ServerPlayer player, String blockId, BlockPos pos) {
            if (!blockId.equals(RaidsContent.COX_TOOLS)) {
                return false;
            }
            toolSpawn(player, "hammer");
            return true;
        }

        @Override
        public void gameTick(long tick) {
            t++;
            List<LivingEntity> crabs = crabs();
            if (crabs.size() < wanted && t % 10 == 0) {
                spawnCrab();
            }
            for (LivingEntity crab : crabs) {
                int c = colourTicks.getOrDefault(crab, 0);
                if (c > 0 && --c == 0) {
                    colour.remove(crab);
                }
                colourTicks.put(crab, c);
                int s = stunTicks.getOrDefault(crab, 0);
                if (s > 0) {
                    stunTicks.put(crab, s - 1);
                    Vec3 home = stunAt.get(crab);
                    if (home != null && crab.position().distanceToSqr(home) > 0.04) {
                        crab.teleportTo(home.x, home.y, home.z);
                    }
                    if (crab instanceof Mob m) {
                        m.getNavigation().stop();
                    }
                    if (t % 3 == 0) {
                        world().sendParticles(ParticleTypes.CRIT, crab.getX(), crab.getY() + 0.8, crab.getZ(), 2, 0.2, 0.1, 0.2, 0);
                    }
                } else if (crab instanceof Mob m && t % 10 == 0) {
                    // OSRS: crabs are aggressive with a short reach; they wander towards nearby players.
                    ServerPlayer near = null;
                    for (ServerPlayer p : players()) {
                        if (p.distanceToSqr(crab) < 6 * 6 && (near == null || p.distanceToSqr(crab) < near.distanceToSqr(crab))) {
                            near = p;
                        }
                    }
                    if (near != null) {
                        m.getNavigation().moveTo(near, 0.8);
                    }
                }
                if (colour.containsKey(crab) && t % 2 == 0) {
                    world().sendParticles(look(colour.get(crab)), crab.getX(), crab.getY() + 0.6, crab.getZ(), 2, 0.2, 0.2, 0.2, 0);
                }
            }
            tickOrb(crabs);
            boolean all = true;
            for (boolean b : lit) {
                all &= b;
            }
            if (all) {
                say("Every crystal shines - the way onward opens.");
                if (wasted == 0) {
                    for (ServerPlayer p : players()) {
                        CombatAchievementService.flag(p, CombatAchievements.FLAG_COX_CRABS_PERFECT);
                    }
                }
                complete();
            }
        }

        private boolean anyLit() {
            for (boolean b : lit) {
                if (b) {
                    return true;
                }
            }
            return false;
        }

        /** The orb is gone without lighting anything (a wasted orb once a crystal is lit). */
        private void lose(String why) {
            lost = orbColour + " orb " + why;
            if (anyLit()) {
                wasted++;
            }
            orb = null;
        }

        private Vec3 orbWorld() {
            return vec(orb.x, orb.z);
        }

        private void tickOrb(List<LivingEntity> crabs) {
            if (orb == null) {
                int[] s = layout.start();
                orb = new Vec3(s[0], 0, s[1]);
                orbDx = s[2];
                orbDz = s[3];
                orbColour = CoxRules.Colour.WHITE;
                orbStriker = null;
                lastCrab = null;
                orbs++;
            }
            orb = orb.add(orbDx, 0, orbDz);
            Vec3 w = orbWorld();
            world().sendParticles(look(orbColour), w.x, w.y + 0.6, w.z, 6, 0.15, 0.15, 0.15, 0);
            // Reached a crystal?
            for (int i = 0; i < 4; i++) {
                int[] c = layout.crystal(i);
                if (Math.round(orb.x) == c[0] && Math.round(orb.z) == c[1]) {
                    if (!lit[i] && orbColour == CoxRules.crystalWants(i)) {
                        lit[i] = true;
                        paintCrystal(i);
                        ServerPlayer who = orbStriker == null ? randomPlayer() : world().getServer().getPlayerList().getPlayer(orbStriker);
                        if (who != null) {
                            raid.awardActionPoints(who, RaidScaling.COX_CRABS_POINTS / 4); // approx: an even split per crystal
                        }
                        int n = 0;
                        for (boolean b : lit) {
                            n += b ? 1 : 0;
                        }
                        say("A crystal flares with light! (" + n + "/4)");
                        orb = null;
                    } else {
                        lose("reached crystal " + i + (lit[i] ? " (already lit)" : " with the wrong colour"));
                    }
                    return;
                }
            }
            if (Math.abs(orb.x) >= CoxRules.CRAB_ROOM_REACH || Math.abs(orb.z) >= CoxRules.CRAB_ROOM_REACH) {
                lose("hit the wall at " + Math.round(orb.x) + ", " + Math.round(orb.z));
                return;
            }
            for (ServerPlayer p : players()) {
                if (RaidEffects.horizontalDistSq(p.position(), w) < 0.7 * 0.7) {
                    world().sendParticles(ParticleTypes.EXPLOSION, w.x, w.y + 0.5, w.z, 1, 0, 0, 0, 0);
                    RaidEffects.hit(null, p, CoxRules.orbDamage(Math.round(p.getHealth())), null, true);
                    Msg.red(p, "The orb explodes against you!");
                    lose("exploded against " + p.getName().getString());
                    return;
                }
            }
            if (lastCrab != null && RaidEffects.horizontalDistSq(lastCrab.position(), w) > 1.5 * 1.5) {
                lastCrab = null;
            }
            for (LivingEntity crab : crabs) {
                if (crab == lastCrab || RaidEffects.horizontalDistSq(crab.position(), w) > 0.9 * 0.9) {
                    continue;
                }
                CoxRules.Colour next = CoxRules.reflect(orbColour, colourOf(crab));
                if (next == null) {
                    lose("absorbed by a " + colourOf(crab) + " crab at " + Math.round(orb.x) + ", " + Math.round(orb.z));
                    return;
                }
                // The orb bounces off the crab, turning right from its own point of view.
                bounces++;
                if (orbColour == CoxRules.Colour.WHITE) {
                    orbStriker = striker.get(crab);
                }
                orbColour = next;
                int[] turn = CoxRules.turnRight(orbDx, orbDz);
                orbDx = turn[0];
                orbDz = turn[1];
                orb = new Vec3(Math.round(orb.x), 0, Math.round(orb.z));
                lastCrab = crab;
                break;
            }
        }

        /**
         * Whether a crab at {@code pos} stands where an orb can meet it: the lane from the sculpture to the black crystal, or a
         * side crystal's row out to its crystal (with half a block to spare over the orb's reach).
         */
        boolean onOrbPath(Vec3 pos) {
            double dx = pos.x - (center().getX() + 0.5);
            double dz = pos.z - (center().getZ() + 0.5);
            if (Math.abs(dx) < 1.5) {
                return true;
            }
            int right = CoxRules.turnRight(0, layout.lane())[0];
            for (int row : layout.rows()) {
                if (Math.abs(dz - row) < 1.5 && dx * right > -1.5) {
                    return true;
                }
            }
            return false;
        }

        /** Self-test walk: lures every crab but {@code keep} that stands on an orb path to a spot clear of them. */
        private void lureOffPaths(LivingEntity keep) {
            int right = CoxRules.turnRight(0, layout.lane())[0];
            int spot = 0;
            for (LivingEntity crab : crabs()) {
                if (crab == keep || !onOrbPath(crab.position())) {
                    continue;
                }
                // Park them on the side away from the crystals, spread along the wall.
                Vec3 to = vec(-right * (8 + 3 * (spot % 3)), -14 + 7 * (spot / 3 % 5));
                spot++;
                stunTicks.remove(crab);
                stunAt.remove(crab);
                crab.teleportTo(to.x, to.y, to.z);
            }
        }

        private String lostNote() {
            return lost == null ? "" : "; last orb lost: " + lost;
        }

        @Override
        public void reset() {
            super.reset();
            java.util.Arrays.fill(lit, false);
            for (int i = 0; i < 4; i++) {
                paintCrystal(i);
            }
            orb = null;
        }

        @Override
        public String walk(CoxTobWalk w) {
            w.until(() -> crabs().size() >= 3, 20, "fewer than three crystal crabs appeared");
            w.use(w.block(this, RaidsContent.COX_TOOLS));
            w.expect(w.count("hammer") > 0, "the room's hammer spawn gave nothing");
            int right = CoxRules.turnRight(0, layout.lane())[0];
            AttackType[] styles = {AttackType.MAGIC, AttackType.SLASH, AttackType.RANGED};
            w.moveTo(vec(-right * 12, 0));
            // Before any crystal is lit: a crab struck the wrong colour sends the orb into the yellow crystal as a green orb,
            // which does nothing.
            LivingEntity test = crabs().get(0);
            Vec3 p0 = turnPoint(0);
            test.teleportTo(p0.x, p0.y, p0.z);
            lureOffPaths(test);
            w.hold("hammer");
            w.hit(test, 1, AttackType.CRUSH);
            w.expect(stunned(test), "a hammer did not stun the crystal crab");
            w.hold(null);
            int startOrbs = orbs;
            int guard = 0;
            while (guard++ < 160 && (orbs == startOrbs || orb != null)) {
                if (colourTicks.getOrDefault(test, 0) < 3 || colourOf(test) != CoxRules.Colour.GREEN) {
                    w.hit(test, 1, AttackType.RANGED);
                }
                w.tick(1);
            }
            w.expect(!lit[0] && bounces > 0 && lost != null && lost.contains("crystal 0 with the wrong colour"),
                    "a green orb bounced into the yellow crystal did not just vanish" + lostNote());
            test.teleportTo(test.getX() - right * 8, test.getY(), test.getZ() + 3);
            stunTicks.remove(test);
            // Clear the lane (and the side rows) of crabs so the white orb crosses into the black crystal.
            lureOffPaths(null);
            for (int i = 0; i < 80 && !lit[3]; i++) {
                w.tick(1);
            }
            w.expect(lit[3], "the white orb never lit the black crystal (waited 80 ticks)" + lostNote());
            for (int i = 0; i < 3; i++) {
                final int crystal = i;
                LivingEntity crab = crabs().get(0);
                Vec3 p = turnPoint(crystal);
                crab.teleportTo(p.x, p.y, p.z); // lured there
                // OSRS Wiki (Crab puzzle): the other crabs are lured away, or they run into the orb. The walk drives no
                // creature AI, so a crab spawned on this crystal's row would stand in the bounced orb's way for good.
                lureOffPaths(crab);
                w.moveTo(new Vec3(p.x - right * 2.5, p.y, p.z)); // beside the lane, away from the bounced orb
                w.hold("hammer");
                w.hit(crab, 1, AttackType.CRUSH);
                w.expect(stunned(crab), "a hammer did not stun the crystal crab");
                w.hold(null);
                CoxRules.Colour want = CoxRules.crystalWants(crystal);
                guard = 0;
                while (!lit[crystal] && guard++ < 100) {
                    if (colourTicks.getOrDefault(crab, 0) < 3 || colourOf(crab) != want) {
                        w.hit(crab, 1, styles[crystal]);
                    }
                    w.tick(1);
                }
                w.expect(lit[crystal], "the " + want + " orb never lit its crystal (crab " + colourOf(crab) + ")" + lostNote());
                crab.teleportTo(crab.getX() - right * 10, crab.getY(), crab.getZ()); // lead it away from the lane
                stunTicks.remove(crab);
            }
            w.until(() -> done, 5, "lighting every crystal did not finish the room");
            w.expect(wasted == 0, wasted + " orb(s) wasted after the first crystal lit" + lostNote());
            return orbs + " orbs, " + bounces + " bounce(s), all four crystals lit";
        }
    }

    // ================================================================== the Great Olm

    /**
     * The Great Olm (OSRS wiki). The head rises from the north or south wall with a claw on either side. Each power phase
     * uses one power (acid, flame or crystal, never the last phase's); the penultimate phase uses all of them, its left hand
     * heals from damage for two attacks after each teleport, and a hand left disabled for about 30 seconds while the other
     * lives recovers. When both hands are down the Olm sinks, crystals fall (7-8 at players) and it rises on the other wall.
     * After the penultimate phase it makes its final stand: the head can be killed while crystals fall constantly and its
     * Life Siphon drains anyone off its two pools. Every 4 ticks it takes a step of its rotation (standard attack, empty,
     * standard, Crystal Burst, standard, empty, standard, Lightning, standard, empty, standard, Teleport); a standard attack
     * may be a prayer sphere or, 1 in 3 (1 in 2 in the final stand), one of its powers' attacks instead. The head takes 66%
     * less from everything but ranged and heals back damage until the final stand; the mage hand from everything but magic;
     * the melee hand from everything but melee, and a heavy hit (1/20 of its health) makes it clench.
     */
    static final class Olm extends RoomLogic implements CoxTobWalk.Walkable {
        private enum Stage { FIGHT, TRANSITION, HEAD }

        private DataBossEntity head;
        private DataBossEntity left;
        private DataBossEntity right;
        private Stage stage = Stage.FIGHT;
        private int phase;
        private int phases;
        private CoxRules.OlmPower power;
        private final Set<String> usedThisPhase = new HashSet<>();
        private boolean south;
        private int t;
        private int step;
        private int clench;
        private long handDownAt = -1;
        private int healingHand;
        private int healingHandIn = -1;
        private int transitionLeft;
        private AttackType style = AttackType.MAGIC;
        private final List<Pool> pools = new ArrayList<>();
        private final Map<UUID, Integer> burning = new HashMap<>();
        private final List<Vec3> siphon = new ArrayList<>();
        private int siphonTicks;
        private final Map<UUID, Integer> drip = new HashMap<>();
        private int phasesSeen;
        /** "Perfect Olm": failed by the listed specials, a claw regrowing or the same acid pool two ticks running. */
        private boolean imperfect;
        private boolean penultimateSpecial;
        private final Map<UUID, Pool> lastPool = new HashMap<>();
        private int swaps;
        private int lastSwapDamage = -1;

        Olm(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            BlockPos c = center();
            int y = c.getY();
            for (int side = -1; side <= 1; side += 2) {
                int z0 = c.getZ() + side * 18;
                int z1 = c.getZ() + side * RaidBuilder.HALF;
                RaidBuilder.fill(world(), c.getX() - RaidBuilder.HALF, y + 1, Math.min(z0, z1), c.getX() + RaidBuilder.HALF,
                        y + RaidBuilder.HEIGHT, Math.max(z0, z1), Blocks.BLACKSTONE.defaultBlockState());
                RaidBuilder.fill(world(), c.getX() - 12, y, c.getZ() + side * 13, c.getX() + 12, y, c.getZ() + side * 17,
                        Blocks.POLISHED_BLACKSTONE.defaultBlockState());
                for (int i = 0; i < 8; i++) {
                    BlockPos p = new BlockPos(c.getX() + rnd().nextInt(RaidBuilder.HALF * 2) - RaidBuilder.HALF,
                            y + 1 + rnd().nextInt(RaidBuilder.HEIGHT - 1), c.getZ() + side * 18);
                    RaidBuilder.set(world(), p, Blocks.AMETHYST_BLOCK.defaultBlockState());
                }
            }
        }

        private int wall() {
            return south ? 15 : -15;
        }

        @Override
        public void start() {
            phases = RaidScaling.olmPhases(raid.partySize());
            phase = 0;
            power = null;
            pools.clear();
            stage = Stage.FIGHT;
            south = rnd().nextBoolean();
            head = spawnBoss("great_olm", at(0, wall()));
            if (head != null) {
                head.setNoAi(true);
                anchor(head);
            }
            nextPhase();
        }

        private boolean penultimate() {
            return phase >= phases;
        }

        private DataBossEntity spawnClaw(String id, int x) {
            DataBossEntity claw = spawnBoss(id, at(x, wall()));
            if (claw != null) {
                claw.setNoAi(true);
                claw.setYRot(south ? 180 : 0);
                anchor(claw);
            }
            return claw;
        }

        /** Olm's left hand is on its left as it faces the arena: east on the north wall, west on the south wall. */
        private int leftX() {
            return south ? -8 : 8;
        }

        private void nextPhase() {
            phase++;
            phasesSeen++;
            stage = Stage.FIGHT;
            step = 0;
            clench = 0;
            handDownAt = -1;
            healingHand = 0;
            healingHandIn = -1;
            usedThisPhase.clear();
            if (head != null) {
                unanchor(head);
                move(head, vec(0, wall()));
                head.setYRot(south ? 180 : 0);
                anchor(head);
            }
            left = spawnClaw("olm_left_claw", leftX());
            right = spawnClaw("olm_right_claw", -leftX());
            if (penultimate()) {
                power = null;
                warn("The Great Olm's head glows with every power it has! (Phase " + phase + "/" + phases + ")");
            } else {
                CoxRules.OlmPower previous = power;
                do {
                    power = CoxRules.OlmPower.values()[rnd().nextInt(3)];
                } while (power == previous);
                warn("The Great Olm rises with the power of " + power.name().toLowerCase(java.util.Locale.ROOT) + ". (Phase "
                        + phase + "/" + phases + ")");
            }
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
            return stage != Stage.TRANSITION;
        }

        @Override
        public boolean bossAllowDeath(DataBossEntity boss) {
            if (boss == head && stage != Stage.HEAD) {
                raid.setCurrentHitpoints(head, raid.maxHitpoints(head) / 2);
                return false;
            }
            return true;
        }

        @Override
        public int bossIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            CoxRules.OlmPart part = boss == head ? CoxRules.OlmPart.HEAD : boss == left ? CoxRules.OlmPart.LEFT : CoxRules.OlmPart.RIGHT;
            int d = CoxRules.olmMitigated(part, type, damage);
            if (boss == head && stage != Stage.HEAD && d > 0) {
                // OSRS: until the final stand the head heals back any damage after a few ticks.
                int heal = d;
                RaidEffects.later(CoxRules.OLM_HEAD_AUTOHEAL_TICKS, () -> {
                    if (alive(head) && stage != Stage.HEAD) {
                        raid.heal(head, heal);
                    }
                });
                return d;
            }
            if (boss != left) {
                return d;
            }
            if (healingHand > 0 && d > 0) {
                raid.heal(left, d);
                Msg.action(attacker, "The Great Olm's left claw absorbs the blow and heals!");
                return 0;
            }
            if (clench > 0) {
                Msg.game(attacker, "The Great Olm's left claw is clenched shut.");
                return 0;
            }
            if (melee(type) && !penultimate() && alive(right) && d >= RaidScaling.olmClenchThreshold(raid.maxHitpoints(left))) {
                clench = CoxRules.OLM_CLENCH_TICKS;
                warn("The Great Olm's left claw clenches to protect itself!");
            }
            return d;
        }

        @Override
        public void gameTick(long tick) {
            t++;
            if (head == null) {
                return;
            }
            if (!alive(head)) {
                complete();
                return;
            }
            List<ServerPlayer> players = players();
            tickPools(this, pools, t, head, ParticleTypes.ITEM_SLIME);
            perfectTick(players);
            tickBurns(players);
            tickDrips(players);
            tickSiphon(players);
            if (healingHand > 0) {
                healingHand--;
            }
            switch (stage) {
                case TRANSITION -> {
                    if (t % CoxRules.OLM_FALLING_INTERVAL == 0 && !players.isEmpty()) {
                        fallingCrystal(players.get(rnd().nextInt(players.size())).position());
                        if (--transitionLeft <= 0) {
                            south = !south;
                            nextPhase();
                        }
                    }
                    return;
                }
                case HEAD -> {
                    if (!players.isEmpty() && t % CoxRules.OLM_FALLING_INTERVAL == 0) {
                        ServerPlayer p = players.get(rnd().nextInt(players.size()));
                        fallingCrystal(t % (3 * CoxRules.OLM_FALLING_INTERVAL) == 0 ? p.position()
                                : p.position().add(rnd().nextInt(9) - 4, 0, rnd().nextInt(9) - 4));
                    }
                }
                default -> checkHands();
            }
            if (stage == Stage.FIGHT && !alive(left) && !alive(right)) {
                pools.clear();
                if (!penultimate()) {
                    stage = Stage.TRANSITION;
                    transitionLeft = CoxRules.olmTransitionCrystals(rnd());
                    warn("The Great Olm retreats into the ground and the cavern shakes!");
                    return;
                }
                stage = Stage.HEAD;
                warn("The Great Olm makes its final stand - its head is now vulnerable!");
            }
            if (t % CoxRules.OLM_STEP_TICKS == 0 && !players.isEmpty()) {
                rotationStep(players);
            }
        }

        /** "Perfect Olm" (OSRS): notes back-to-back hits from one acid pool and keeps a failed run flagged. */
        private void perfectTick(List<ServerPlayer> players) {
            for (ServerPlayer p : players) {
                Pool on = null;
                for (Pool pool : pools) {
                    if (RaidEffects.horizontalDistSq(p.position(), pool.pos()) < 0.8 * 0.8) {
                        on = pool;
                        break;
                    }
                }
                Pool before = on == null ? lastPool.remove(p.getUUID()) : lastPool.put(p.getUUID(), on);
                if (on != null && on == before) {
                    imperfect = true;
                }
            }
            if (imperfect && alive(head)) {
                CombatAchievementService.flagNear(head, CombatAchievements.FLAG_OLM_IMPERFECT);
            }
            if (penultimateSpecial && alive(head)) {
                CombatAchievementService.flagNear(head, CombatAchievements.FLAG_OLM_PENULTIMATE_SPECIAL);
            }
        }

        /** A hit from a special that fails "Perfect Olm" if it lands. */
        private int special(LivingEntity source, ServerPlayer p, int dmg) {
            int dealt = RaidEffects.hit(source, p, dmg, null, true);
            if (dealt > 0) {
                imperfect = true;
            }
            return dealt;
        }

        private void checkHands() {
            if (clench > 0) {
                clench--;
                if (!alive(right) || clench == 0) {
                    clench = 0;
                    if (alive(left)) {
                        say("The Great Olm's left claw unclenches.");
                    }
                }
            }
            boolean l = alive(left);
            boolean r = alive(right);
            if (l == r || !penultimate()) {
                handDownAt = -1;
                return;
            }
            // OSRS penultimate phase: both hands must go down at once; a disabled hand recovers in about 30 seconds.
            if (handDownAt < 0) {
                handDownAt = t;
                warn("One of the Great Olm's hands is down - disable the other before it recovers!");
            } else if (t - handDownAt > CoxRules.OLM_HAND_RECOVER_TICKS) {
                if (!l) {
                    left = spawnClaw("olm_left_claw", leftX());
                } else {
                    right = spawnClaw("olm_right_claw", -leftX());
                }
                // OSRS: no points for damaging a hand the Olm took back.
                raid.emptyPointsPool(l ? right : left);
                imperfect = true; // "Perfect Olm": a claw regrew
                handDownAt = -1;
                warn("The Great Olm recovers its " + (l ? "right" : "left") + " hand!");
            }
        }

        private void rotationStep(List<ServerPlayer> players) {
            CoxRules.OlmStep s = CoxRules.OLM_ROTATION.get(step % CoxRules.OLM_ROTATION.size());
            step++;
            if (healingHandIn >= 0 && --healingHandIn == 0 && alive(left) && penultimate()) {
                healingHand = 2 * CoxRules.OLM_STEP_TICKS;
                penultimateSpecial = true;
                say("The Great Olm's left claw glows - attacking it will heal it!");
            }
            boolean handSpecials = stage == Stage.FIGHT && alive(left) && clench == 0;
            if (handSpecials && penultimate() && (s == CoxRules.OlmStep.CRYSTAL_BURST || s == CoxRules.OlmStep.LIGHTNING
                    || s == CoxRules.OlmStep.TELEPORT)) {
                penultimateSpecial = true;
            }
            switch (s) {
                case STANDARD -> standard(players);
                case CRYSTAL_BURST -> {
                    if (handSpecials) {
                        crystalBurst(players);
                    }
                }
                case LIGHTNING -> {
                    if (handSpecials) {
                        lightning();
                    }
                }
                case TELEPORT -> {
                    if (handSpecials) {
                        portals(players);
                        healingHandIn = 2;
                    } else if (stage == Stage.HEAD) {
                        lifeSiphon();
                    }
                }
                default -> {
                }
            }
        }

        private boolean headPhase() {
            return stage == Stage.HEAD;
        }

        /** A standard attack: a power's attack (1/3, 1/2 in the final stand), a prayer sphere, or a magic/ranged attack. */
        private void standard(List<ServerPlayer> players) {
            ServerPlayer target = players.get(rnd().nextInt(players.size()));
            List<String> powers = new ArrayList<>();
            for (CoxRules.OlmPower p : CoxRules.OlmPower.values()) {
                if (power == null || power == p) {
                    for (String a : switch (p) {
                        case ACID -> new String[]{"acid_spray", "acid_drip"};
                        case FLAME -> new String[]{"deep_burn", "fire_wall"};
                        default -> new String[]{"falling_crystals", "crystal_bombs"};
                    }) {
                        powers.add(a);
                    }
                }
            }
            if (rnd().nextDouble() < CoxRules.olmPowerChance(headPhase())) {
                List<String> fresh = new ArrayList<>(powers);
                fresh.removeAll(usedThisPhase);
                if (fresh.isEmpty()) {
                    usedThisPhase.clear();
                    fresh = powers;
                }
                String a = fresh.get(rnd().nextInt(fresh.size()));
                usedThisPhase.add(a);
                power(a, target, players);
                return;
            }
            if (rnd().nextDouble() < CoxRules.OLM_SPHERE_CHANCE) {
                sphere(target);
                return;
            }
            if (rnd().nextDouble() < CoxRules.OLM_STYLE_SWITCH) {
                style = style == AttackType.MAGIC ? AttackType.RANGED : AttackType.MAGIC;
            }
            AttackType st = style;
            // OSRS: 27/28/29 (38/39/41 in Challenge Mode) for a solo raid, scaled up by the party's offence bonus.
            int max = (int) Math.round(CoxRules.olmMaxHit(penultimate(), headPhase(), raid.mode() == com.runecraft.osrs.content.raids.RaidMode.CHALLENGE)
                    * RaidScaling.coxOffence(raid.partySize(), false));
            Vec3 a = head.getEyePosition();
            Vec3 b = target.getEyePosition();
            for (int i = 0; i <= 8; i++) {
                Vec3 v = a.lerp(b, i / 8.0);
                world().sendParticles(st == AttackType.MAGIC ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.END_ROD, v.x, v.y, v.z, 1, 0, 0, 0, 0);
            }
            RaidEffects.later(2, () -> {
                if (!alive(head) || !target.isAlive() || !inside(target.position())) {
                    return;
                }
                int dmg = rnd().nextInt(max + 1);
                // OSRS: the right protection prayer takes off roughly 75-80%.
                if (RaidEffects.protectedFrom(target, st)) {
                    dmg = dmg * 22 / 100;
                }
                RaidEffects.hitRaw(head, target, dmg);
            });
        }

        /** Prayer spheres: an active protection prayer is knocked off (and prayer halved); the right one blocks it fully. */
        private void sphere(ServerPlayer p) {
            AttackType[] styles = {AttackType.CRUSH, AttackType.RANGED, AttackType.MAGIC};
            String[] names = {"red", "green", "purple"};
            int s = rnd().nextInt(3);
            var data = Players.data(p);
            boolean praying = data.isActive(com.runecraft.osrs.Prayer.PROTECT_FROM_MELEE) || data.isActive(com.runecraft.osrs.Prayer.PROTECT_FROM_MISSILES)
                    || data.isActive(com.runecraft.osrs.Prayer.PROTECT_FROM_MAGIC);
            if (praying) {
                dropOverheads(p, "The sphere disrupts your protection prayer!");
                data.setPrayerPoints(data.prayerPoints() / 2);
                com.runecraft.network.ServerNetworking.sync(p);
            }
            Msg.red(p, "The Great Olm fires a " + names[s] + " sphere at you!");
            world().sendParticles(s == 0 ? ParticleTypes.FLAME : s == 1 ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.WITCH,
                    head.getX(), head.getY() + 1.5, head.getZ(), 10, 0.4, 0.4, 0.4, 0.02);
            DataBossEntity source = head;
            RaidEffects.later(4, () -> {
                if (!p.isAlive() || !alive(source) || !inside(p.position())) {
                    return;
                }
                if (!RaidEffects.protectedFrom(p, styles[s])) {
                    special(source, p, CoxRules.olmSphereDamage(Math.round(p.getHealth())));
                }
            });
        }

        private void power(String attack, ServerPlayer target, List<ServerPlayer> players) {
            switch (attack) {
                case "acid_spray" -> {
                    say("The Great Olm sprays acid across the chamber!");
                    for (ServerPlayer p : players) {
                        for (int i = 0; i < 3; i++) {
                            Vec3 at = p.position().add(rnd().nextInt(9) - 4, 0, rnd().nextInt(9) - 4);
                            pools.add(new Pool(at, t + CoxRules.OLM_ACID_TICKS, CoxRules.OLM_ACID_MIN, CoxRules.OLM_ACID_MAX,
                                    CoxRules.OLM_ACID_POISON));
                        }
                    }
                }
                case "acid_drip" -> {
                    Msg.red(target, "The Great Olm covers you in acid! Keep moving.");
                    drip.put(target.getUUID(), CoxRules.OLM_ACID_TICKS);
                }
                case "deep_burn" -> {
                    say(target.getName().getString() + " is burning! (Burn with me!)");
                    burning.put(target.getUUID(), CoxRules.OLM_BURN_HITS);
                }
                case "fire_wall" -> fireWall(target);
                case "falling_crystals" -> {
                    Msg.red(target, "The Great Olm lets out a cry - crystals fall towards you!");
                    for (int i = 0; i < CoxRules.OLM_FALLING_POWER_CRYSTALS; i++) {
                        RaidEffects.later(1 + i, () -> {
                            if (target.isAlive() && alive(head) && inside(target.position())) {
                                zone(world(), target.position(), 0.8, CoxRules.OLM_FALLING_POWER_MIN, CoxRules.OLM_FALLING_POWER_MAX, 3, head,
                                        ParticleTypes.END_ROD);
                            }
                        });
                    }
                }
                default -> crystalBombs(players);
            }
        }

        private void tickBurns(List<ServerPlayer> players) {
            if (burning.isEmpty() || t % CoxRules.OLM_BURN_INTERVAL != 0) {
                return;
            }
            Map<UUID, Integer> spread = new HashMap<>();
            for (ServerPlayer p : players) {
                Integer left = burning.get(p.getUUID());
                if (left == null) {
                    continue;
                }
                RaidEffects.hit(head, p, CoxRules.OLM_BURN_DAMAGE, null, true);
                var skills = Players.data(p).skills();
                for (Skill s : new Skill[]{Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC}) {
                    skills.setBoost(s, skills.boost(s) - CoxRules.OLM_BURN_DRAIN);
                }
                com.runecraft.network.ServerNetworking.sync(p);
                world().sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1, p.getZ(), 6, 0.3, 0.5, 0.3, 0.01);
                for (ServerPlayer q : players) {
                    if (q != p && !burning.containsKey(q.getUUID()) && q.distanceToSqr(p) < 1.6 * 1.6) {
                        Msg.red(q, "I will burn with you.");
                        spread.put(q.getUUID(), CoxRules.OLM_BURN_HITS);
                        spread.put(p.getUUID(), CoxRules.OLM_BURN_HITS); // spreading resets the timer
                    }
                }
                if (!spread.containsKey(p.getUUID())) {
                    if (left - 1 <= 0) {
                        burning.remove(p.getUUID());
                    } else {
                        burning.put(p.getUUID(), left - 1);
                    }
                }
            }
            burning.putAll(spread);
            burning.keySet().removeIf(u -> world().getServer().getPlayerList().getPlayer(u) == null);
        }

        private void tickDrips(List<ServerPlayer> players) {
            if (drip.isEmpty()) {
                return;
            }
            for (ServerPlayer p : players) {
                Integer left = drip.get(p.getUUID());
                if (left == null) {
                    continue;
                }
                pools.add(new Pool(p.position(), t + CoxRules.OLM_ACID_TICKS, CoxRules.OLM_ACID_MIN, CoxRules.OLM_ACID_MAX,
                        CoxRules.OLM_ACID_POISON));
                if (left <= 1) {
                    drip.remove(p.getUUID());
                } else {
                    drip.put(p.getUUID(), left - 1);
                }
            }
            drip.keySet().removeIf(u -> world().getServer().getPlayerList().getPlayer(u) == null);
        }

        /**
         * One Fire Wall attack: the target's position when the Olm breathed it and the players it caught (for the self-test).
         * Each attack is its own object: two walls breathed at a player who has not moved in between share one position.
         */
        static final class FireWall {
            final Vec3 centre;
            int trapped;

            FireWall(Vec3 centre) {
                this.centre = centre;
            }
        }

        /** Fire walls still burning; a doused wall no longer burns anyone (OSRS wiki). */
        private final List<FireWall> walls = new ArrayList<>();
        private int doused;

        /**
         * A player douses the fire wall with water (OSRS wiki: water and ice spells, Humidify, a filled watering can or a
         * water-filled gourd vial put out a segment, freeing whoever it trapped). The segment poured on belongs to the
         * nearest burning wall within reach. Returns whether there was a wall to douse near the player.
         */
        boolean douse(ServerPlayer p) {
            FireWall wall = null;
            double best = 4.5 * 4.5;
            for (FireWall w : walls) {
                double d = RaidEffects.horizontalDistSq(p.position(), w.centre);
                if (d <= best) {
                    best = d;
                    wall = w;
                }
            }
            if (wall == null) {
                return false;
            }
            walls.remove(wall);
            doused++;
            Vec3 c = wall.centre;
            world().sendParticles(ParticleTypes.SPLASH, c.x, c.y + 0.5, c.z, 40, 2, 0.5, 3, 0.1);
            world().sendParticles(ParticleTypes.CLOUD, c.x, c.y + 0.5, c.z, 20, 2, 0.5, 3, 0.02);
            say(p.getName().getString() + " douses the wall of fire.");
            return true;
        }

        /** Fire Wall: two walls of fire either side of the target; leap out of a wall (8-10) or burn inside (50-65). */
        FireWall fireWall(ServerPlayer target) {
            Vec3 c = target.position();
            FireWall wall = new FireWall(c);
            walls.add(wall);
            say("The Great Olm breathes walls of fire around " + target.getName().getString() + "!");
            for (ServerPlayer p : players()) {
                for (int side = -1; side <= 1; side += 2) {
                    if (Math.abs(p.getX() - (c.x + side * 2)) < 0.7 && Math.abs(p.getZ() - c.z) <= 3.5) {
                        special(head, p, CoxRules.OLM_WALL_LEAP_MIN + rnd().nextInt(CoxRules.OLM_WALL_LEAP_MAX - CoxRules.OLM_WALL_LEAP_MIN + 1));
                        p.push(side * 0.6, 0.2, 0);
                        p.hurtMarked = true;
                    }
                }
            }
            for (int i = 0; i < CoxRules.OLM_WALL_TICKS; i++) {
                RaidEffects.later(i, () -> {
                    if (!walls.contains(wall)) {
                        return;
                    }
                    for (int dz = -3; dz <= 3; dz++) {
                        for (int side = -1; side <= 1; side += 2) {
                            world().sendParticles(ParticleTypes.FLAME, c.x + side * 2, c.y + 0.3, c.z + dz, 2, 0.1, 0.4, 0.1, 0);
                        }
                    }
                });
            }
            RaidEffects.later(CoxRules.OLM_WALL_TICKS, () -> {
                if (!walls.remove(wall)) {
                    return; // doused
                }
                for (ServerPlayer p : players()) {
                    if (Math.abs(p.getX() - c.x) < 2 && Math.abs(p.getZ() - c.z) <= 3.5) {
                        wall.trapped++;
                        special(head, p, CoxRules.OLM_WALL_MIN + rnd().nextInt(CoxRules.OLM_WALL_MAX - CoxRules.OLM_WALL_MIN + 1));
                        Msg.red(p, "You are caught between the walls of fire!");
                    }
                }
            });
            return wall;
        }

        /** Crystal bombs: 2-3 bombs near the players explode over 4 tiles (15, +15 for every tile closer). */
        private void crystalBombs(List<ServerPlayer> players) {
            say("The Great Olm fires crystal bombs!");
            for (int i = 0; i < CoxRules.olmCrystalBombs(raid.partySize()); i++) {
                ServerPlayer p = players.get(rnd().nextInt(players.size()));
                Vec3 at = p.position().add(rnd().nextInt(7) - 3, 0, rnd().nextInt(7) - 3);
                for (int k = 0; k < CoxRules.OLM_BOMB_FUSE; k++) {
                    RaidEffects.later(k, () -> world().sendParticles(ParticleTypes.END_ROD, at.x, at.y + 0.5, at.z, 4, 0.2, 0.3, 0.2, 0));
                }
                RaidEffects.later(CoxRules.OLM_BOMB_FUSE, () -> {
                    world().sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + 0.5, at.z, 1, 0, 0, 0, 0);
                    for (ServerPlayer q : players()) {
                        int dmg = CoxRules.olmCrystalBombDamage(Math.sqrt(RaidEffects.horizontalDistSq(q.position(), at)));
                        if (dmg > 0) {
                            special(head, q, dmg);
                        }
                    }
                });
            }
        }

        /** A falling crystal: 16-25 directly under it, 10-16 one tile off. */
        private void fallingCrystal(Vec3 at) {
            world().sendParticles(ParticleTypes.END_ROD, at.x, at.y + 0.2, at.z, 10, 0.5, 0.05, 0.5, 0);
            DataBossEntity source = head;
            RaidEffects.later(3, () -> {
                world().sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, 1, 0, 0, 0, 0);
                for (ServerPlayer p : players()) {
                    double d = RaidEffects.horizontalDistSq(p.position(), at);
                    if (d < 0.6 * 0.6) {
                        RaidEffects.hit(source, p, CoxRules.OLM_CRYSTAL_DIRECT_MIN
                                + rnd().nextInt(CoxRules.OLM_CRYSTAL_DIRECT_MAX - CoxRules.OLM_CRYSTAL_DIRECT_MIN + 1), null, true);
                    } else if (d < 1.6 * 1.6) {
                        RaidEffects.hit(source, p, CoxRules.OLM_CRYSTAL_NEAR_MIN
                                + rnd().nextInt(CoxRules.OLM_CRYSTAL_NEAR_MAX - CoxRules.OLM_CRYSTAL_NEAR_MIN + 1), null, true);
                    }
                }
            });
        }

        /** Crystal Burst: a seedling under every player bursts a few ticks later for heavy damage. // approx: 20-40 */
        private void crystalBurst(List<ServerPlayer> players) {
            say("Crystals burst from the ground!");
            for (ServerPlayer p : players) {
                Vec3 at = p.position();
                zone(world(), at, 1.0, 20, 40, 4, left, ParticleTypes.END_ROD);
                RaidEffects.later(4, () -> {
                    for (ServerPlayer q : players()) {
                        if (RaidEffects.horizontalDistSq(q.position(), at) <= 1.0) {
                            imperfect = true; // "Perfect Olm": caught by a Crystal Burst
                        }
                    }
                });
            }
        }

        /** Lightning: bolts run across the chamber; anyone zapped loses their overhead prayers, is hit and bound. */
        private void lightning() {
            say("The Great Olm's left claw crackles - lightning strikes!");
            for (int lane = 0; lane < 2; lane++) {
                double z = center().getZ() + 0.5 + rnd().nextInt(25) - 12;
                for (int k = 0; k < 3; k++) {
                    RaidEffects.later(k, () -> {
                        for (int x = -18; x <= 18; x += 2) {
                            world().sendParticles(ParticleTypes.ELECTRIC_SPARK, center().getX() + 0.5 + x, center().getY() + 1.2, z, 2, 0.4, 0.2, 0.1, 0);
                        }
                    });
                }
                DataBossEntity source = left;
                RaidEffects.later(3, () -> {
                    for (ServerPlayer p : players()) {
                        if (Math.abs(p.getZ() - z) < 1.0) {
                            dropOverheads(p, "The lightning disables your protection prayers!");
                            RaidEffects.hit(source, p, 10 + rnd().nextInt(16), null, true); // approx: "moderate damage"
                            com.runecraft.content.systems.SysCombat.freeze(p, 3);
                        }
                    }
                });
            }
        }

        /**
         * Teleport (OSRS wiki): up to six players are paired and swap places, taking 5 per tile they stood apart (nothing on
         * top of each other, at most 50); with an odd number the one left over is spared. A solo player is sent to a random
         * tile up to ten tiles away, hurt the same way by the distance.
         */
        void portals(List<ServerPlayer> players) {
            say("Portals open beneath you!");
            List<ServerPlayer> list = new ArrayList<>(players);
            java.util.Collections.shuffle(list, rnd());
            if (list.size() == 1) {
                ServerPlayer p = list.get(0);
                Vec3 from = p.position();
                Vec3 to = null;
                for (int tries = 0; tries < 20 && to == null; tries++) {
                    double a = rnd().nextDouble() * Math.PI * 2;
                    double r = rnd().nextDouble() * CoxRules.OLM_SWAP_SOLO_RANGE;
                    Vec3 c = new Vec3(from.x + Math.cos(a) * r, from.y, from.z + Math.sin(a) * r);
                    BlockPos b = BlockPos.containing(c);
                    if (inside(c) && world().getBlockState(b).isAir() && !world().getBlockState(b.below()).isAir()) {
                        to = new Vec3(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
                    }
                }
                Vec3 dest = to == null ? from : to;
                world().sendParticles(ParticleTypes.PORTAL, dest.x, dest.y + 0.5, dest.z, 20, 0.4, 0.4, 0.4, 0.1);
                later(4, () -> {
                    if (p.isAlive() && inside(p.position())) {
                        int dmg = CoxRules.olmTeleportDamage(Math.sqrt(RaidEffects.horizontalDistSq(p.position(), dest)));
                        p.teleportTo(dest.x, dest.y, dest.z);
                        swaps++;
                        lastSwapDamage = dmg;
                        specialRaw(head, p, dmg);
                    }
                });
                return;
            }
            int paired = Math.min(CoxRules.OLM_SWAP_MAX_PLAYERS, list.size() - list.size() % 2);
            for (int i = 0; i + 1 < paired; i += 2) {
                ServerPlayer a = list.get(i);
                ServerPlayer b = list.get(i + 1);
                later(4, () -> {
                    if (a.isAlive() && b.isAlive()) {
                        Vec3 pa = a.position();
                        Vec3 pb = b.position();
                        int dmg = CoxRules.olmTeleportDamage(Math.sqrt(RaidEffects.horizontalDistSq(pa, pb)));
                        a.teleportTo(pb.x, pb.y, pb.z);
                        b.teleportTo(pa.x, pa.y, pa.z);
                        swaps++;
                        lastSwapDamage = dmg;
                        specialRaw(head, a, dmg);
                        specialRaw(head, b, dmg);
                    }
                });
            }
        }

        /** A special whose damage is the wiki's fixed number (not scaled by the party): fails "Perfect Olm" if it lands. */
        private int specialRaw(LivingEntity source, ServerPlayer p, int dmg) {
            if (dmg <= 0) {
                return 0;
            }
            int dealt = RaidEffects.hitRaw(source, p, dmg);
            if (dealt > 0) {
                imperfect = true;
            }
            return dealt;
        }

        /** Life Siphon (final stand): two pools; anyone not on one within 6 seconds loses up to 18, healing the Olm 5x. */
        private void lifeSiphon() {
            siphon.clear();
            for (int i = 0; i < 2; i++) {
                siphon.add(vec(rnd().nextInt(25) - 12, rnd().nextInt(17) - 8));
            }
            siphonTicks = CoxRules.OLM_SIPHON_TICKS;
            say("The Great Olm spits out two blue pools - stand on one!");
        }

        private void tickSiphon(List<ServerPlayer> players) {
            if (siphonTicks <= 0) {
                return;
            }
            for (Vec3 v : siphon) {
                world().sendParticles(ParticleTypes.SOUL_FIRE_FLAME, v.x, v.y + 0.1, v.z, 4, 0.4, 0.02, 0.4, 0);
            }
            if (--siphonTicks > 0) {
                return;
            }
            int total = 0;
            for (ServerPlayer p : players) {
                boolean safe = false;
                for (Vec3 v : siphon) {
                    safe |= RaidEffects.horizontalDistSq(p.position(), v) < 1.0;
                }
                if (!safe) {
                    total += special(head, p, Math.min(CoxRules.OLM_SIPHON_MAX, 1 + rnd().nextInt(CoxRules.OLM_SIPHON_MAX)));
                }
            }
            if (total > 0 && alive(head)) {
                raid.heal(head, total * CoxRules.OLM_SIPHON_HEAL);
                say("The Great Olm siphons life from you and heals!");
            }
            siphon.clear();
        }

        @Override
        public void cleanup() {
            super.cleanup();
            pools.clear();
            burning.clear();
            drip.clear();
            siphon.clear();
            lastPool.clear();
            walls.clear();
        }

        @Override
        public String walk(CoxTobWalk w) {
            w.expect(alive(head) && alive(left) && alive(right), "the Great Olm did not rise with both claws");
            w.moveTo(at(0, south ? 5 : -5));
            // The head heals back damage before the final stand; the mage hand shrugs off most melee.
            int before = raid.hitpoints(head);
            w.hit(head, 40, AttackType.RANGED);
            w.tick(CoxRules.OLM_HEAD_AUTOHEAL_TICKS + 1);
            w.expect(raid.hitpoints(head) >= before, "the Olm's head did not heal back damage before its final stand");
            int dealt = w.hit(right, 50, AttackType.SLASH);
            w.expect(dealt <= 17, "the Olm's mage hand took " + dealt + " from a 50 melee hit (expected 66% less)");
            // The swap: a solo player lands on a random tile up to ten tiles away, hurt 5 per tile.
            Vec3 before0 = w.player().position();
            portals(List.of(w.player()));
            w.tick(5);
            double moved = Math.sqrt(RaidEffects.horizontalDistSq(before0, w.player().position()));
            w.expect(swaps == 1 && moved <= CoxRules.OLM_SWAP_SOLO_RANGE + 1, "the Olm's swap sent the player " + moved + " tiles");
            w.expect(lastSwapDamage == CoxRules.olmTeleportDamage(moved), "the swap hurt " + lastSwapDamage + " for " + moved + " tiles");
            w.moveTo(at(0, south ? 5 : -5));
            // A heavy melee hit makes the left hand clench for about 30 seconds.
            w.hit(left, Math.max(RaidScaling.olmClenchThreshold(raid.maxHitpoints(left)), 1) * 3, AttackType.SLASH);
            w.expect(clench == CoxRules.OLM_CLENCH_TICKS, "a heavy hit did not clench the left hand for " + CoxRules.OLM_CLENCH_TICKS
                    + " ticks (" + clench + ")");
            w.expect(w.hit(left, 10, AttackType.SLASH) == 0, "the clenched left hand took damage");
            // A fire wall traps the player; a water-filled gourd vial puts it out before it burns. The player may already carry
            // vials filled in a resource room: the one poured must be the one given here.
            Vec3 trap = w.player().position();
            FireWall first = fireWall(w.player());
            int vials = w.count(CoxSupplies.WATER_GOURD);
            int dousedBefore = doused;
            w.give(CoxSupplies.WATER_GOURD, 1);
            w.expect(CoxSupplyService.useWaterGourd(w.player()), "a water-filled gourd vial did not douse the fire wall");
            for (int i = 0; i <= CoxRules.OLM_WALL_TICKS; i++) {
                w.moveTo(trap); // stay inside (the Olm's own swaps and bursts would move the player)
                w.tick(1);
            }
            w.expect(doused == dousedBefore + 1 && first.trapped == 0 && w.count(CoxSupplies.WATER_GOURD) == vials,
                    "the doused fire wall still burned the player (or the vial was not used): caught " + first.trapped + ", "
                    + w.count(CoxSupplies.WATER_GOURD) + " vial(s) left (" + vials + " before the one given), doused "
                    + (doused - dousedBefore));
            w.moveTo(trap);
            FireWall second = fireWall(w.player());
            for (int i = 0; i <= CoxRules.OLM_WALL_TICKS; i++) {
                w.moveTo(trap);
                w.tick(1);
            }
            w.expect(second.trapped == 1, "a fire wall left alone did not burn the trapped player (caught " + second.trapped + ")");
            int guard = 0;
            int lastPhase = phase;
            while (stage != Stage.HEAD && guard++ < 3000) {
                if (stage == Stage.FIGHT) {
                    // Take both hands down together (a hand left down too long recovers in the penultimate phase).
                    if (alive(right)) {
                        w.hit(right, Math.max(10, raid.maxHitpoints(right) / 6), AttackType.MAGIC);
                    }
                    if (alive(left) && (!alive(right) || raid.healthFraction(left) > raid.healthFraction(right))) {
                        w.hit(left, Math.max(10, Math.min(raid.maxHitpoints(left) / 21, raid.hitpoints(left))), AttackType.SLASH);
                    }
                }
                w.tick(1);
                if (phase != lastPhase) {
                    lastPhase = phase;
                    w.expect(alive(left) && alive(right), "the Olm rose for phase " + phase + " without both claws");
                }
            }
            w.expect(stage == Stage.HEAD, "the Olm never reached its final stand (phase " + phase + "/" + phases + ", stage " + stage + ")");
            w.expect(phasesSeen == phases, "the Olm went through " + phasesSeen + " hand phases, expected " + phases);
            w.kill(head, AttackType.RANGED, "the Great Olm's head");
            return phases + " hand phases and the final stand";
        }
    }

    // ================================================================== resource rooms

    /**
     * Scavengers (OSRS): scavenger beasts roam the ruins and drop tools, cave worms and the potion secondaries (see
     * {@link CoxSupplies#scavengerDrop}). The way on is open from the start.
     */
    static final class Scavengers extends RoomLogic implements CoxTobWalk.Walkable {
        private final List<LivingEntity> beasts = new ArrayList<>();

        Scavengers(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            for (int[] r : new int[][]{{-8, -10}, {4, 9}, {10, -6}, {-2, 12}}) {
                RaidBuilder.set(world(), at(r[0], r[1]), Blocks.MOSSY_COBBLESTONE.defaultBlockState());
                RaidBuilder.set(world(), at(r[0], r[1]).above(), Blocks.COBBLESTONE_WALL.defaultBlockState());
            }
        }

        @Override
        public void start() {
            beasts.clear();
            // approx: four scavengers, one more per two extra players (OSRS: the count grows with the party)
            int n = 4 + Math.max(0, raid.partySize() - 1) / 2;
            for (int i = 0; i < n; i++) {
                LivingEntity b = spawnMob("cox_scavenger_beast", at(-6 + rnd().nextInt(16), -12 + rnd().nextInt(25)), false);
                if (b != null) {
                    beasts.add(b);
                }
            }
            complete();
        }

        /** The scavengers stay when the exit opens. */
        @Override
        protected void onComplete() {
        }

        @Override
        public void onCreatureHit(ServerPlayer player, LivingEntity target, int damage) {
            if (!beasts.contains(target) || (target.isAlive() && target.getHealth() > 0)) {
                return;
            }
            beasts.remove(target);
            for (Map.Entry<String, Integer> e : CoxSupplies.scavengerDrop(rnd()).entrySet()) {
                dropItem(world(), target.position(), e.getKey(), e.getValue());
            }
        }

        @Override
        public void cleanup() {
            super.cleanup();
            beasts.forEach(LivingEntity::discard);
            beasts.clear();
        }

        @Override
        public String walk(CoxTobWalk w) {
            w.expect(!beasts.isEmpty(), "no scavenger beasts spawned");
            LivingEntity beast = beasts.get(0);
            Vec3 at = beast.position();
            w.kill(beast, AttackType.SLASH, "a scavenger beast");
            int drops = world().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(BlockPos.containing(at)).inflate(3),
                    e -> true).size();
            w.expect(drops > 0, "a dead scavenger beast dropped nothing");
            return beasts.size() + 1 + " scavengers, " + drops + " drop(s)";
        }
    }

    /**
     * A resource room (OSRS): two herb patches (rake the weeds for seeds, plant with the old tools' seed dibber; herbs grow
     * in 30 seconds), a gourd tree and a geyser for potion vials, a fire to cook on, and either three fishing spots (a
     * fishing rod and cave worms; one spot hides a cave snake) or bats (a butterfly net, or bare hands at 99 Hunter). The
     * food tier follows the team's level and food can burn.
     */
    static final class Farming extends RoomLogic implements CoxTobWalk.Walkable {
        private static final int CLEAR = 1;
        private static final int GROWING = 2;
        private static final int GROWN = 3;

        private final boolean fishing;
        /** Patch state and herb ordinal. */
        private final Map<BlockPos, int[]> patches = new HashMap<>();
        private final Map<BlockPos, Long> grownAt = new HashMap<>();
        private final List<BlockPos> spots = new ArrayList<>();

        Farming(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
            this.fishing = !def.id().equals("cox_hunting");
        }

        @Override
        public void decorate() {
            for (int z = -3; z <= 3; z += 6) {
                BlockPos p = at(-8, z);
                RaidBuilder.set(world(), p.below(), Blocks.COARSE_DIRT.defaultBlockState());
                RaidBuilder.set(world(), p, RaidBuilder.module(RaidsContent.HERB_PATCH));
                patches.put(p, new int[]{CLEAR, 0});
            }
            RaidBuilder.set(world(), at(-12, 0), RaidBuilder.module(RaidsContent.COX_OLD_TOOLS));
            // approx: the weeds stand for the ones at the start and end of each floor
            for (int z = -10; z <= 10; z += 20) {
                RaidBuilder.set(world(), at(-14, z), RaidBuilder.module(RaidsContent.COX_WEEDS));
            }
            BlockPos c = center();
            if (fishing) {
                RaidBuilder.fill(world(), c.getX() + 5, c.getY(), c.getZ() - 13, c.getX() + 11, c.getY(), c.getZ() - 8,
                        Blocks.WATER.defaultBlockState());
                for (int x = 6; x <= 10; x += 2) {
                    BlockPos s = at(x, -7);
                    RaidBuilder.set(world(), s, RaidBuilder.module(RaidsContent.FISHING_POOL));
                    spots.add(s);
                }
            } else {
                for (int x = 6; x <= 10; x += 4) {
                    BlockPos s = at(x, 10);
                    RaidBuilder.set(world(), s, RaidBuilder.module(RaidsContent.BAT_ROOST));
                    spots.add(s);
                }
            }
            // approx: OSRS cooks on the fires of the Tekton room; a fire stands here so the food can always be cooked
            RaidBuilder.set(world(), at(0, 0), RaidBuilder.module(RaidsContent.COOKING_FIRE));
            RaidBuilder.set(world(), at(-2, -12), RaidBuilder.module(RaidsContent.GOURD_TREE));
            RaidBuilder.set(world(), at(2, -12), RaidBuilder.module(RaidsContent.GEYSER));
        }

        @Override
        public void start() {
            complete();
        }

        @Override
        protected void onComplete() {
        }

        /**
         * The fishing spot hiding the cave snake; it moves between the spots now and then. // approx: every 20 ticks. The
         * room completes at once (it is never ticked), so it reads the raid clock.
         */
        int snakeSpot() {
            long period = RaidEffects.now() / 20;
            return spots.isEmpty() ? -1 : (int) Math.floorMod(period * 2654435761L ^ index, spots.size());
        }

        @Override
        public boolean useBlock(ServerPlayer player, String blockId, BlockPos pos) {
            switch (blockId) {
                case RaidsContent.HERB_PATCH -> patch(player, pos);
                case RaidsContent.COX_OLD_TOOLS -> toolSpawn(player, "rake", "spade", "seed_dibber");
                case RaidsContent.COX_WEEDS -> rake(player);
                case RaidsContent.FISHING_POOL -> gather(player, Skill.FISHING, pos);
                case RaidsContent.BAT_ROOST -> gather(player, Skill.HUNTER, pos);
                case RaidsContent.COOKING_FIRE -> cook(player);
                case RaidsContent.GOURD_TREE -> {
                    if (cooldown(player, 2)) {
                        return true;
                    }
                    // OSRS "Pick-lots": sneak to fill your free inventory slots at once.
                    int n = player.isShiftKeyDown() ? Math.max(1, freeSlots(player)) : 1;
                    Inv.give(player, CoxSupplies.EMPTY_GOURD, n);
                    Msg.game(player, n == 1 ? "You pick an empty gourd vial." : "You pick " + n + " empty gourd vials.");
                }
                case RaidsContent.GEYSER -> {
                    String empty = CoxSupplies.EMPTY_GOURD;
                    int n = Inv.count(player, empty);
                    if (n <= 0) {
                        Msg.game(player, "Use the geyser to fill empty gourd vials from the gourd tree.");
                        return true;
                    }
                    Inv.remove(player, empty, n);
                    Inv.give(player, CoxSupplies.WATER_GOURD, n);
                    Msg.game(player, "You fill " + n + " gourd vial" + (n > 1 ? "s" : "") + " from the geyser.");
                }
                default -> {
                    return false;
                }
            }
            return true;
        }

        private static int freeSlots(ServerPlayer p) {
            int n = 0;
            for (int i = 0; i < 36; i++) {
                n += p.getInventory().getItem(i).isEmpty() ? 1 : 0;
            }
            return n;
        }

        /** Weeds (OSRS): a rake turns up 1-3 golpar, buchu or noxifer seeds. */
        private void rake(ServerPlayer player) {
            if (cooldown(player, 3)) {
                return;
            }
            if (!Inv.has(player, "rake", 1)) {
                Msg.game(player, "You need a rake. The old tools have one.");
                return;
            }
            player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            Map.Entry<String, Integer> seeds = CoxSupplies.rakeSeeds(rnd());
            Inv.give(player, seeds.getKey(), seeds.getValue());
            XpService.add(player, Skill.FARMING, 4); // approx
            Msg.game(player, "You rake the weeds and find " + seeds.getValue() + " " + seeds.getKey().replace('_', ' ') + (seeds.getValue() > 1 ? "s." : "."));
        }

        private void patch(ServerPlayer player, BlockPos pos) {
            int[] patch = patches.computeIfAbsent(pos, k -> new int[]{CLEAR, 0});
            if (patch[0] == GROWING && RaidEffects.now() >= grownAt.getOrDefault(pos, 0L)) {
                patch[0] = GROWN;
            }
            int farming = level(player, Skill.FARMING);
            CoxSupplies.Herb[] herbs = CoxSupplies.Herb.values();
            switch (patch[0]) {
                case CLEAR -> {
                    CoxSupplies.Herb pick = null;
                    for (CoxSupplies.Herb h : herbs) {
                        if (Inv.has(player, h.seed(), 1) && farming >= h.farmingLevel) {
                            pick = h;
                        }
                    }
                    if (pick == null) {
                        Msg.game(player, "You have no seeds you can plant here. Rake the weeds or loot the demi-bosses for seeds.");
                        return;
                    }
                    if (!Inv.has(player, "seed_dibber", 1)) {
                        Msg.game(player, "You need a seed dibber to plant seeds. The old tools have one.");
                        return;
                    }
                    Inv.remove(player, pick.seed(), 1);
                    XpService.add(player, Skill.FARMING, pick.plantXp);
                    patch[0] = GROWING;
                    patch[1] = pick.ordinal();
                    grownAt.put(pos, RaidEffects.now() + CoxSupplies.HERB_GROW_TICKS);
                    Msg.game(player, "You plant the " + pick.seed().replace('_', ' ') + ". It will grow in 30 seconds.");
                }
                case GROWING -> Msg.game(player, "The " + herbs[patch[1]].name.toLowerCase(java.util.Locale.ROOT) + " is still growing.");
                default -> {
                    CoxSupplies.Herb herb = herbs[patch[1]];
                    int n = CoxSupplies.herbYield(herb, farming);
                    Inv.give(player, herb.grimy(), n);
                    XpService.add(player, Skill.FARMING, herb.harvestXp * n);
                    patch[0] = CLEAR;
                    Msg.game(player, "You pick " + n + " grimy " + herb.name.toLowerCase(java.util.Locale.ROOT) + (n > 1 ? "s." : "."));
                }
            }
        }

        /** One catch per click: the tier the team's level allows (fish need cave worms; one fishing spot hides a cave snake). */
        private void gather(ServerPlayer player, Skill skill, BlockPos pos) {
            if (cooldown(player, 3)) {
                return;
            }
            boolean fish = skill == Skill.FISHING;
            int lvl = level(player, skill);
            String tool = fish ? "fishing_rod" : "butterfly_net";
            if (!Inv.has(player, tool, 1) && (fish || lvl < 99)) {
                Msg.game(player, fish ? "You need a fishing rod to fish here. Scavengers drop them."
                        : "You need a butterfly net to catch the bats (or 99 Hunter). Scavengers drop them.");
                return;
            }
            String bait = CoxSupplies.CAVE_WORMS;
            if (fish && !Inv.has(player, bait, 1)) {
                Msg.game(player, "You need cave worms as bait. Scavengers drop them.");
                return;
            }
            player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            if (fish && spots.indexOf(pos) == snakeSpot()) {
                Msg.red(player, "A cave snake lunges out of the water at you!");
                RaidEffects.hit(null, player, 1 + rnd().nextInt(5), null, true); // approx
                return;
            }
            CoxSupplies.Food food = CoxSupplies.teamCatch(skill, averageLevel(raid, skill));
            if (food == null || lvl < food.level()) {
                food = CoxSupplies.catchFor(skill, lvl);
            }
            if (food == null || rnd().nextDouble() > 0.5 + lvl / 250.0) { // approx: catch chance
                Msg.game(player, fish ? "You fail to catch anything." : "The bat flutters out of reach.");
                return;
            }
            if (fish) {
                Inv.remove(player, bait, 1);
            }
            Inv.give(player, food.raw(), 1);
            XpService.add(player, skill, food.gatherXp());
            Msg.game(player, "You catch a raw " + food.name().toLowerCase(java.util.Locale.ROOT) + ".");
        }

        /** Cooks every raw fish and bat the player's Cooking level allows; some may burn. */
        private void cook(ServerPlayer player) {
            if (cooldown(player, 2)) {
                return;
            }
            int lvl = level(player, Skill.COOKING);
            int cooked = 0;
            int burnt = 0;
            for (CoxSupplies.Food f : CoxSupplies.FOODS) {
                int n = Inv.count(player, f.raw());
                if (n <= 0) {
                    continue;
                }
                if (lvl < f.level()) {
                    Msg.game(player, "You need a Cooking level of " + f.level() + " to cook the " + f.name().toLowerCase(java.util.Locale.ROOT) + ".");
                    continue;
                }
                Inv.remove(player, f.raw(), n);
                for (int i = 0; i < n; i++) {
                    if (rnd().nextDouble() < CoxSupplies.burnChance(lvl, f)) {
                        Inv.give(player, CoxSupplies.burnt(f), 1);
                        burnt++;
                    } else {
                        Inv.give(player, f.cooked(), 1);
                        XpService.add(player, Skill.COOKING, f.cookXp());
                        cooked++;
                    }
                }
            }
            Msg.game(player, cooked + burnt > 0 ? "You cook " + cooked + " piece" + (cooked == 1 ? "" : "s") + " of food"
                    + (burnt > 0 ? " and burn " + burnt : "") + "." : "You have nothing to cook.");
        }

        @Override
        public String walk(CoxTobWalk w) {
            w.use(w.block(this, RaidsContent.COX_OLD_TOOLS));
            w.expect(w.count("rake") > 0 && w.count("seed_dibber") > 0, "the old tools gave no rake or seed dibber");
            BlockPos weeds = w.block(this, RaidsContent.COX_WEEDS);
            int rakes = 0;
            while (w.count(CoxSupplies.Herb.GOLPAR.seed()) == 0 && rakes++ < 60) {
                w.moveTo(weeds.offset(1, 0, 0));
                w.use(weeds);
                w.tick(3);
            }
            w.expect(w.count(CoxSupplies.Herb.GOLPAR.seed()) > 0, "raking the weeds gave no golpar seed in " + rakes + " rakes");
            for (CoxSupplies.Herb h : new CoxSupplies.Herb[]{CoxSupplies.Herb.BUCHU, CoxSupplies.Herb.NOXIFER}) {
                w.take(h.seed()); // plant golpar only
            }
            BlockPos patch = w.block(this, RaidsContent.HERB_PATCH);
            w.moveTo(patch.offset(1, 0, 0));
            w.use(patch);
            w.expect(patches.get(patch)[0] == GROWING, "planting a golpar seed did not start a herb growing");
            w.tick(CoxSupplies.HERB_GROW_TICKS + 1);
            w.use(patch);
            w.expect(w.count(CoxSupplies.Herb.GOLPAR.grimy()) > 0, "picking the grown patch gave no grimy golpar");
            BlockPos tree = w.block(this, RaidsContent.GOURD_TREE);
            w.use(tree);
            w.use(w.block(this, RaidsContent.GEYSER));
            w.expect(w.count(CoxSupplies.WATER_GOURD) > 0, "the gourd tree and geyser gave no water-filled gourd vial");
            w.give(fishing ? "fishing_rod" : "butterfly_net", 1);
            if (fishing) {
                w.give(CoxSupplies.CAVE_WORMS, 20);
            }
            int tries = 0;
            while (countRaw(w) == 0 && tries++ < 60) {
                BlockPos s = spots.get(fishing ? (snakeSpot() + 1) % spots.size() : 0);
                w.moveTo(s.offset(0, 0, fishing ? 1 : -1));
                w.use(s);
                w.tick(3);
            }
            w.expect(countRaw(w) > 0, "nothing was caught at the " + (fishing ? "fishing spots" : "bats") + " in " + tries + " tries");
            BlockPos fire = w.block(this, RaidsContent.COOKING_FIRE);
            w.moveTo(fire.offset(1, 0, 0));
            w.use(fire);
            w.expect(countRaw(w) == 0, "cooking on the fire left raw food");
            return (fishing ? "fishing" : "hunting") + " room: herbs grown, vials filled, food caught and cooked";
        }

        private static int countRaw(CoxTobWalk w) {
            int n = 0;
            for (CoxSupplies.Food f : CoxSupplies.FOODS) {
                n += w.count(f.raw());
            }
            return n;
        }
    }
}
