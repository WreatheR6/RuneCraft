package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.content.bosses.DoomDelves;
import com.runecraft.osrs.content.bosses.ModernBosses;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * A whole Doom of Mokhaiotl delve run for the self-test ({@code boss:walk:doom_of_mokhaiotl}): the player follows the Doom
 * from delve 1 down to the first deep delve (9), each level spawned as the summoning stone spawns it, with that level's
 * hitpoints, and the level's mechanics answered the OSRS way (melee punish of the beam, the rock throw's prayer disable
 * and debris dodged, larvae killed with demonbane, the earthen shield raised against the shockwaves, the demonbane-only
 * shield, the car phase dodged, a car slam blocked behind a rock, acid washed away at delve 6, the giant larva, holy water
 * from a punish kill). Each kill must hold that level's loot in the burrow hole and raise the personal best; leaving ends
 * the run with the loot kept in the chest, and the stone's claim pays it out. Accelerated: the Doom is put near death
 * between mechanics instead of fought down.
 */
public final class DoomWalk {
    private DoomWalk() {
    }

    private static IllegalStateException fail(String message) {
        return new IllegalStateException(message);
    }

    public static String walk(DataBossEntity first, BossWalks.Driver d) {
        ServerPlayer p = d.player();
        ServerLevel world = (ServerLevel) first.level();
        BlockPos at = first.blockPosition();
        first.discard(); // a run starts from the stone's delve 1, not the walk's plain Doom
        DoomDelveState.Run saved = DoomDelveService.takeRun(p);
        List<DataBossEntity> spawned = new ArrayList<>();
        List<String> log = new ArrayList<>();
        try {
            int held = 0;
            for (int level = 1; level <= DoomDelves.DEEPEST; level++) {
                DataBossEntity boss = DoomDelveService.descend(p, world, at);
                if (boss == null) {
                    throw fail("delve " + level + ": the Doom didn't spawn");
                }
                spawned.add(boss);
                if (boss.delve() != level || boss.trueHitpoints() != DoomDelves.hitpoints(level)) {
                    throw fail("delve " + level + ": spawned as delve " + boss.delve() + " with " + boss.trueHitpoints()
                            + " hitpoints (wiki " + DoomDelves.hitpoints(level) + ")");
                }
                if (!(boss.script() instanceof DoomDelveService.DoomScript s)) {
                    throw fail("the Doom has no DoomScript");
                }
                d.ticks(boss, 1); // its first game tick takes its home (the spot its mechanics are laid out around)
                d.moveTo(s.homeVec().add(0, 0, -9), boss);
                d.ticks(boss, 2);
                if (s.walkLevel() != level) {
                    throw fail("delve " + level + ": the script runs delve " + s.walkLevel());
                }
                List<String> did = new ArrayList<>();
                switch (level) {
                    case 1 -> {
                        did.add(beamPunish(boss, s, d));
                        did.add(larvae(boss, s, d));
                        did.add(shockwave(boss, s, d));
                    }
                    case 2 -> did.add(rockThrow(boss, s, d));
                    case 3 -> {
                        did.add(acid(boss, s, d));
                        did.add(shield(boss, s, d, false));
                    }
                    case 5 -> did.add(shield(boss, s, d, true));
                    case 6 -> {
                        if (s.walkAcid() != 0) {
                            throw fail("delve 6 didn't wash away the acid blood (" + s.walkAcid() + " splats)");
                        }
                        did.add("acid washed away");
                        did.add(shield(boss, s, d, true));
                    }
                    case 8 -> did.add(giantLarva(boss, s, d));
                    default -> {
                    }
                }
                boolean punishKill = level == 4;
                if (punishKill) {
                    did.add(punishKill(boss, s, d));
                } else {
                    kill(boss, s, d);
                }
                if (boss.isAlive()) {
                    throw fail("delve " + level + ": the Doom survived");
                }
                if (DoomDelveService.runLevel(p) != level) {
                    throw fail("delve " + level + ": the run records level " + DoomDelveService.runLevel(p));
                }
                int nowHeld = DoomDelveService.heldLoot(p);
                if (nowHeld <= held) {
                    throw fail("delve " + level + ": no loot held in the burrow hole (" + nowHeld + " stacks, " + held + " before)");
                }
                held = nowHeld;
                if (Players.data(p).killCount(DoomDelves.DEEPEST_KEY) < level) {
                    throw fail("delve " + level + ": personal best stayed " + Players.data(p).killCount(DoomDelves.DEEPEST_KEY));
                }
                log.add(level + (did.isEmpty() ? "" : " (" + String.join(", ", did) + ")"));
            }
            if (Players.data(p).killCount(DoomDelves.DEEP_DELVES_KEY) < 1) {
                throw fail("delve 9 didn't count as a deep delve");
            }
            // leaving the ruins ends the run, the loot waits in the chest; the stone's claim pays it out
            Vec3 stood = p.position();
            p.teleportTo(stood.x + DoomDelveService.LEAVE_BLOCKS + 8, stood.y, stood.z);
            boolean left = DoomDelveService.checkLeft(p);
            p.teleportTo(stood.x, stood.y, stood.z);
            if (!left || DoomDelveService.runLevel(p) != 0) {
                throw fail("walking off didn't end the run (level " + DoomDelveService.runLevel(p) + ")");
            }
            if (DoomDelveService.heldLoot(p) != held) {
                throw fail("leaving lost loot: " + DoomDelveService.heldLoot(p) + " of " + held + " stacks kept in the chest");
            }
            p.getInventory().clearContent(); // room for the claim (the self-test restores the real inventory afterwards)
            int tearsBefore = Inv.count(p, "demon_tear");
            if (!DoomDelveService.claimForSelfTest(p) || DoomDelveService.heldLoot(p) != 0) {
                throw fail("claiming at the stone left " + DoomDelveService.heldLoot(p) + " stacks");
            }
            int tears = Inv.count(p, "demon_tear") - tearsBefore;
            // nine levels of loot outgrow a 36-slot pack: what doesn't fit lands at the player's feet (picked up from there)
            for (net.minecraft.world.entity.item.ItemEntity e : world.getEntitiesOfClass(
                    net.minecraft.world.entity.item.ItemEntity.class, p.getBoundingBox().inflate(4))) {
                if ("demon_tear".equals(Inv.idOf(e.getItem()))) {
                    tears += e.getItem().getCount();
                }
                e.discard();
            }
            if (tears < 550) {
                throw fail("claimed only " + tears + " demon tears (delves 3-9 guarantee 550)");
            }
            // a death on a run forfeits the run's loot but never the chest
            DataBossEntity again = DoomDelveService.descend(p, world, at);
            spawned.add(again);
            if (again == null || again.delve() != 1) {
                throw fail("a claimed run didn't start again at delve 1");
            }
            String helped = helperOutDamages(again, d);
            DoomDelveService.died(p);
            if (DoomDelveService.heldLoot(p) != 0 || DoomDelveService.runLevel(p) != 0) {
                throw fail("a death kept the run's loot (" + DoomDelveService.heldLoot(p) + " stacks)");
            }
            // the delve below the Ruins of Mokhaiotl, entered from the Tonali Cavern; and the Maggot King's lair (no walk of its own)
            String lairs = LairWalk.visit(p, DoomDelves.BOSS) + "; " + LairWalk.visit(p, "maggot_king");
            return "delves " + String.join("; ", log) + "; left with " + held + " stacks in the chest, claimed (" + tears
                    + " demon tears), " + helped + ", a death forfeits the run; " + lairs;
        } finally {
            for (DataBossEntity b : spawned) {
                if (b.isAlive()) {
                    b.discard();
                }
            }
            DoomDelveService.restoreRun(p, saved);
            p.setItemInHand(InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
        }
    }

    /**
     * A delve level belongs to the run that summoned it (round 43): a helper with no run who deals most of the damage gets
     * neither the level nor its loot nor a personal best (it would let a player skip straight to a deep level), the player's
     * run completes it.
     */
    private static String helperOutDamages(DataBossEntity boss, BossWalks.Driver d) {
        ServerPlayer p = d.player();
        ServerPlayer helper = com.runecraft.platform.FakePlayer.partner((ServerLevel) boss.level());
        DoomDelveState.Run helperRun = DoomDelveService.takeRun(helper);
        int helperBest = Players.data(helper).killCount(DoomDelves.DEEPEST_KEY);
        try {
            helper.teleportTo((ServerLevel) boss.level(), boss.getX() + 3, boss.getY(), boss.getZ(), 0, 0);
            int dealt = BossWalks.hit(boss, helper, boss.trueHitpoints() / 2, AttackType.RANGED);
            com.runecraft.combat.CombatZones.forget(boss);
            com.runecraft.combat.CombatZones.forget(helper);
            if (dealt <= 0) {
                throw fail("the helper's hit dealt nothing");
            }
            kill(boss, (DoomDelveService.DoomScript) boss.script(), d);
            if (boss.isAlive()) {
                throw fail("the helped delve 1 survived");
            }
            if (com.runecraft.combat.KillCredit.topDamager(boss, p) != helper) {
                throw fail("the helper (" + dealt + " damage) wasn't the top damager");
            }
            if (DoomDelveService.runLevel(helper) != 0 || DoomDelveService.heldLoot(helper) != 0
                    || Players.data(helper).killCount(DoomDelves.DEEPEST_KEY) != helperBest) {
                throw fail("the helper took the summoner's delve level (run level " + DoomDelveService.runLevel(helper) + ", "
                        + DoomDelveService.heldLoot(helper) + " stacks held)");
            }
            if (DoomDelveService.runLevel(p) != 1 || DoomDelveService.heldLoot(p) == 0) {
                throw fail("the summoner's run didn't complete the helped level (run level " + DoomDelveService.runLevel(p) + ")");
            }
            return "a helper's " + dealt + " damage left the level and its loot to the summoner's run";
        } finally {
            DoomDelveService.restoreRun(helper, helperRun);
        }
    }

    /** Puts the Doom near death and lands the last hit (breaking a shield or waiting out a burrow first). */
    private static void kill(DataBossEntity boss, DoomDelveService.DoomScript s, BossWalks.Driver d) {
        ServerPlayer p = d.player();
        for (int i = 0; i < 80 && boss.isAlive(); i++) {
            if (s.walkShielded()) {
                p.setItemInHand(InteractionHand.MAIN_HAND, Inv.stack("arclight", 1));
                BossWalks.hit(boss, p, 200, AttackType.SLASH);
            } else {
                boss.setHealth(Math.min(boss.getHealth(), 5f));
                BossWalks.hit(boss, p, 60, AttackType.RANGED);
            }
            if (boss.isAlive()) {
                d.ticks(boss, 1);
            }
        }
    }

    /** Delve 1: the melee charge (with its rock throw) is interrupted by a melee hit, which deals the punish bonus. */
    private static String beamPunish(DataBossEntity boss, DoomDelveService.DoomScript s, BossWalks.Driver d) {
        ServerPlayer p = d.player();
        s.walkStartBeam(boss, p);
        if (!s.walkCharging() || !s.walkRockThrowInFlight()) {
            throw fail("delve 1: the melee charge didn't start with a rock throw");
        }
        p.setItemInHand(InteractionHand.MAIN_HAND, Inv.stack("abyssal_whip", 1));
        float before = boss.getHealth();
        int took = BossWalks.hit(boss, p, 10, AttackType.SLASH);
        if (s.walkCharging()) {
            throw fail("delve 1: a melee hit didn't interrupt the beam");
        }
        if (took < 10 || boss.getHealth() >= before) {
            throw fail("delve 1: the melee punish dealt " + took);
        }
        d.ticks(boss, 10); // let the rock throw resolve
        return "beam punished (" + took + ")";
    }

    /** Delve 1: a larva killed by demonbane dies at once; one left alone reaches the Doom and adds demonic charge. */
    private static String larvae(DataBossEntity boss, DoomDelveService.DoomScript s, BossWalks.Driver d) {
        ServerPlayer p = d.player();
        int before = s.walkLarvae().size();
        s.walkSpawnLarva(boss, p, false);
        List<Mob> larvae = s.walkLarvae();
        if (larvae.size() <= before) {
            throw fail("delve 1: no larva spawned");
        }
        Mob larva = larvae.get(larvae.size() - 1);
        p.setItemInHand(InteractionHand.MAIN_HAND, Inv.stack("arclight", 1));
        BossWalks.attack(p, larva);
        d.ticks(boss, 1);
        if (larva.isAlive()) {
            throw fail("delve 1: a demonbane hit left the larva at " + larva.getHealth());
        }
        int stacks = DoomDelveService.runStacks(p);
        s.walkSpawnLarva(boss, p, false);
        if (BossWalks.until(boss, d, 30, () -> DoomDelveService.runStacks(p) > stacks) < 0) {
            throw fail("delve 1: a larva left alone never reached the Doom");
        }
        return "larva killed with demonbane, one absorbed (charge " + DoomDelveService.runStacks(p) + ")";
    }

    /**
     * The shockwave: of the volatile earth, the two furthest apart are broken (the second late, so the earthen shield walks
     * across the landing), and the player stands in the shield through every shockwave.
     */
    private static String shockwave(DataBossEntity boss, DoomDelveService.DoomScript s, BossWalks.Driver d) {
        ServerPlayer p = d.player();
        s.walkStartShockwave(boss);
        List<Mob> earth = new ArrayList<>(s.walkEarth());
        if (earth.size() < 2) {
            throw fail("the shockwave raised " + earth.size() + " volatile earth");
        }
        Mob a = null;
        Mob b = null;
        double best = -1;
        for (Mob x : earth) {
            for (Mob y : earth) {
                if (x != y && x.distanceToSqr(y) > best) {
                    best = x.distanceToSqr(y);
                    a = x;
                    b = y;
                }
            }
        }
        int level = s.walkLevel();
        double speed = DoomDelves.earthenShieldSpeed(level) * 2.0;
        long travel = (long) Math.floor(Math.sqrt(best) / speed);
        int waves = DoomDelves.shockwaves(level);
        // the shield rises the tick after the second earth breaks and lasts `travel` ticks: break it early enough to step in
        // before the first wave, late enough that it still stands at the last
        long lead = Math.max(2, Math.min(travel - (long) DoomDelves.SHOCKWAVE_GAP * (waves - 1) - 2, 6));
        Vec3 target = a.position(); // the shield walks from the second earth broken towards the first
        BossWalks.attack(p, a); // the first decides where the shield walks to
        Mob second = b;
        // Round 44: the player walks over to the second earth while the volatile earth winds up (the shield rises on its spot)
        Vec3 bAt = b.position();
        Vec3 toPlayer = p.position().subtract(bAt).multiply(1, 0, 1);
        Vec3 by = toPlayer.lengthSqr() < 1e-4 ? bAt : bAt.add(toPlayer.normalize().scale(1.2));
        d.walkTo(BossFx.ground(BossFx.world(boss), by.x, by.z, bAt.y), boss);
        if (BossWalks.until(boss, d, 30, () -> s.walkShockwaveTicksLeft() <= lead) < 0) {
            throw fail("the shockwave never came due");
        }
        BossWalks.attack(p, second);
        d.clearDamage();
        int waited = BossWalks.until(boss, d, 40, () -> {
            Vec3 shield = s.walkEarthenShield();
            if (shield != null) {
                // round 44: after the walking shield on foot, to where it steps next (a player keeps in its 2.5 blocks while it
                // moves a block or two a game tick, standing a step ahead of it rather than a step behind)
                Vec3 to = target.subtract(shield).multiply(1, 0, 1);
                double len = to.length();
                Vec3 ahead = len < 1e-3 ? shield : shield.add(to.scale(Math.min(speed, len) / len));
                d.walkTo(ahead, boss);
            }
            return !s.walkShockwavePending();
        });
        if (waited < 0) {
            throw fail("the shockwaves never finished");
        }
        if (d.damage("shockwave") > 0) {
            throw fail("the shockwave hit the player inside the earthen shield for " + d.damage("shockwave"));
        }
        return waves + " shockwave" + (waves == 1 ? "" : "s") + " blocked in the earthen shield";
    }

    /** Delve 2: the rock throw switches prayers off; the player steps clear of the shadows before the debris lands. */
    private static String rockThrow(DataBossEntity boss, DoomDelveService.DoomScript s, BossWalks.Driver d) {
        ServerPlayer p = d.player();
        BossWalks.pray(p, Prayer.PROTECT_FROM_MAGIC);
        s.walkThrowRock(boss, p);
        if (BossWalks.until(boss, d, DoomDelves.ROCK_THROW_TICKS + 3, () -> !s.walkShadows().isEmpty()) < 0) {
            throw fail("delve 2: the rock never exploded into debris");
        }
        if (Players.data(p).isActive(Prayer.PROTECT_FROM_MAGIC)) {
            throw fail("delve 2: the exploding rock left the player's protection prayer on");
        }
        // Round 44: the nearest spot clear of every shadow (the debris hits within DEBRIS_RADIUS of one), walked to a reaction
        // time after the shadows appear; the debris lands on the strike floor (3 game ticks) after them.
        Vec3 c = s.homeVec();
        List<com.runecraft.osrs.RealReaction.Disc> discs = new ArrayList<>();
        for (Vec3 sh : s.walkShadows()) {
            discs.add(new com.runecraft.osrs.RealReaction.Disc(sh.x, sh.z, DoomDelveService.DoomScript.DEBRIS_RADIUS, 0, "debris"));
        }
        double[] spot = com.runecraft.osrs.RealReaction.safeSpot(p.getX(), p.getZ(), discs, 0, 12,
                (x, z) -> Math.hypot(x - c.x, z - c.z) <= 11);
        if (spot == null) {
            throw fail("delve 2: no spot clear of the debris shadows");
        }
        Vec3 clear = BossFx.ground(BossFx.world(boss), spot[0], spot[1], p.getY());
        int since = BossWalks.sinceGameTick(boss);
        d.walkOut("debris", clear, since, com.runecraft.osrs.ArenaTiming.STRIKE_FLOOR_TICKS
                * com.runecraft.action.ActionManager.MC_TICKS_PER_GAME_TICK - since, boss);
        d.clearDamage();
        d.ticks(boss, 4);
        if (d.damage("debris") > 0) {
            throw fail("delve 2: debris hit the player away from its shadows for " + d.damage("debris"));
        }
        return "rock throw: prayers knocked off, debris dodged";
    }

    /** Delve 3+: blood sprays when the Doom is hurt. */
    private static String acid(DataBossEntity boss, DoomDelveService.DoomScript s, BossWalks.Driver d) {
        int before = s.walkAcid();
        d.ticks(boss, 4);
        BossWalks.hit(boss, d.player(), 5, AttackType.RANGED);
        if (s.walkAcid() <= before) {
            throw fail("delve 3: no acid blood sprayed on a hit");
        }
        return "acid sprayed";
    }

    /**
     * Delve 3+: the shield only demonbane breaks; from delve 5 the burrow follows (the eye's path dodged, a car slam blocked
     * behind a rock from delve 6), then the volatile earth.
     */
    private static String shield(DataBossEntity boss, DoomDelveService.DoomScript s, BossWalks.Driver d, boolean burrow) {
        ServerPlayer p = d.player();
        int level = s.walkLevel();
        s.walkStartShield(boss);
        if (!s.walkShielded() || s.walkShieldPoints() != DoomDelves.SHIELD_POINTS) {
            throw fail("delve " + level + ": no 500-point shield");
        }
        p.setItemInHand(InteractionHand.MAIN_HAND, Inv.stack("abyssal_whip", 1));
        float hp = boss.getHealth();
        BossWalks.hit(boss, p, 50, AttackType.SLASH);
        if (boss.getHealth() < hp || s.walkShieldPoints() != DoomDelves.SHIELD_POINTS) {
            throw fail("delve " + level + ": a non-demonbane hit got through the shield");
        }
        d.ticks(boss, 3);
        p.setItemInHand(InteractionHand.MAIN_HAND, Inv.stack("arclight", 1));
        int charge = s.walkShieldCharge();
        BossWalks.hit(boss, p, 40, AttackType.SLASH);
        if (s.walkShieldPoints() >= DoomDelves.SHIELD_POINTS || charge > 0 && s.walkShieldCharge() >= charge) {
            throw fail("delve " + level + ": demonbane didn't hurt the shield or reset its charge");
        }
        for (int i = 0; i < 10 && s.walkShielded(); i++) {
            BossWalks.hit(boss, p, 100, AttackType.SLASH);
        }
        if (s.walkShielded()) {
            throw fail("delve " + level + ": demonbane never broke the shield");
        }
        if (!burrow) {
            return "shield broken with demonbane";
        }
        if (!s.walkBurrowed()) {
            throw fail("delve " + level + ": no burrow after the shield");
        }
        d.clearDamage();
        int slams = 0;
        int blocked = 0;
        int zooms = 0;
        for (int t = 0; t < 120 && s.walkBurrowed(); t++) {
            Vec3 eye = s.walkEye();
            if (eye != null) {
                // step off the line from the Doom to its eye (round 44: on foot, the nearest spot clear of its trample; the
                // first trampling step comes the game tick after its three-tick wind-up)
                int since = BossWalks.sinceGameTick(boss);
                d.walkOut("trample", dodge(boss, s, eye, p.position()), since,
                        (DoomDelves.ZOOM_WINDUP + 1) * com.runecraft.action.ActionManager.MC_TICKS_PER_GAME_TICK - since, boss);
                zooms++;
                // from delve 6 a car slam follows from where the zoom stops (its eye), three game ticks after it: a player
                // heads for cover on its own side of the path while the Doom winds up and zooms, as rocks may be far off
                Vec3 cover = DoomDelves.carSlams(level) ? coverFrom(boss, s, eye, p.position()) : null;
                if (cover != null) {
                    d.walkTo(cover, boss);
                }
                for (int w = 0; w <= DoomDelves.ZOOM_WINDUP && s.walkEye() != null; w++) {
                    d.ticks(boss, 1);
                }
                continue;
            }
            if (s.walkSlamming()) {
                slams++;
                Vec3 shelter = shelterSpot(boss, s, p.position());
                int before = d.damage("slam");
                if (shelter != null) {
                    // round 44: behind the nearest rock on foot; the slam lands three game ticks after the Doom rises
                    int since = BossWalks.sinceGameTick(boss);
                    d.walkOut("slam", shelter, since, 3 * com.runecraft.action.ActionManager.MC_TICKS_PER_GAME_TICK - since, boss);
                } else if (slams == 1) {
                    throw fail("delve " + level + ": no rock left to hide behind for the first car slam");
                }
                for (int w = 0; w < 6 && s.walkSlamming(); w++) {
                    d.ticks(boss, 1);
                }
                if (shelter != null) {
                    if (d.damage("slam") > before) {
                        throw fail("delve " + level + ": the car slam hit the player behind a rock for " + (d.damage("slam") - before));
                    }
                    blocked++;
                }
                continue;
            }
            d.ticks(boss, 1);
        }
        if (s.walkBurrowed()) {
            throw fail("delve " + level + ": the burrow never ended");
        }
        if (d.damage("trample") > 0) {
            throw fail("delve " + level + ": trampled for " + d.damage("trample") + " although off the eye's path");
        }
        if (DoomDelves.carSlams(level) && slams == 0) {
            throw fail("delve " + level + ": no car slam");
        }
        if (!DoomDelves.carSlams(level) && slams > 0) {
            throw fail("delve " + level + ": a car slam before delve 6");
        }
        if (!s.walkShockwavePending()) {
            throw fail("delve " + level + ": no volatile earth after the burrow");
        }
        String waves = shockwave(boss, s, d);
        if (!s.walkLoopShield()) {
            throw fail("delve " + level + ": the shield doesn't come back after the shockwave");
        }
        return "shield broken, burrow (" + zooms + " zooms dodged, " + blocked + "/" + slams + " car slams blocked behind rocks), "
                + waves;
    }

    /**
     * Where to stand off the Doom's zoom from where it is to its eye: the spot nearest {@code at} that is clear of its trample
     * (half its width plus a block either side of its path, and a margin), inside the arena. Round 44: the nearest such spot, as
     * a player steps aside, not the farthest.
     */
    private static Vec3 dodge(DataBossEntity boss, DoomDelveService.DoomScript s, Vec3 eye, Vec3 at) {
        Vec3 from = boss.position();
        Vec3 dir = eye.subtract(from);
        double len = Math.max(0.1, Math.hypot(dir.x, dir.z));
        Vec3 unit = new Vec3(dir.x / len, 0, dir.z / len);
        Vec3 side = new Vec3(-unit.z, 0, unit.x);
        Vec3 c = s.homeVec();
        double clear = boss.getBbWidth() / 2 + 1.0 + com.runecraft.osrs.RealReaction.EDGE_CLEARANCE + 0.3;
        double off = (at.x - from.x) * side.x + (at.z - from.z) * side.z;
        Vec3 best = null;
        for (double sign : new double[]{Math.signum(off) == 0 ? 1 : Math.signum(off), -(Math.signum(off) == 0 ? 1 : Math.signum(off))}) {
            double move = sign * clear - off; // sideways to the clear edge on that side
            Vec3 spot = at.add(side.scale(move));
            if (Math.hypot(spot.x - c.x, spot.z - c.z) > 11) {
                continue;
            }
            if (best == null || spot.distanceToSqr(at) < best.distanceToSqr(at)) {
                best = spot;
            }
        }
        if (Math.abs(off) >= clear) {
            best = at; // already clear of its path
        }
        if (best == null) {
            best = c.add(side.scale(clear + 1));
        }
        return BossFx.ground(BossFx.world(boss), best.x, best.z, from.y);
    }

    /**
     * Cover from the car slam that follows a zoom (round 44), taken while the Doom winds up: the spot behind a rock (as seen
     * from the eye, where the zoom stops) nearest {@code at}, on the player's side of the zoom's path and clear of its
     * trample, the rock itself out of the path's reach (the zoom smashes rocks within 2.5 blocks); null when there is none.
     */
    private static Vec3 coverFrom(DataBossEntity boss, DoomDelveService.DoomScript s, Vec3 eye, Vec3 at) {
        Vec3 from = boss.position();
        double len = Math.max(0.1, Math.hypot(eye.x - from.x, eye.z - from.z));
        double sideX = -(eye.z - from.z) / len;
        double sideZ = (eye.x - from.x) / len;
        double clear = boss.getBbWidth() / 2 + 1.0 + com.runecraft.osrs.RealReaction.EDGE_CLEARANCE + 0.3;
        double atSide = (at.x - from.x) * sideX + (at.z - from.z) * sideZ;
        Vec3 c = s.homeVec();
        Vec3 best = null;
        for (Vec3 r : s.walkRocks()) {
            if (toSegment(r, from, eye) < 2.7) {
                continue; // smashed by the zoom on its way
            }
            Vec3 away = r.subtract(eye);
            double far = Math.hypot(away.x, away.z);
            if (far < 0.5) {
                continue;
            }
            Vec3 spot = r.add(away.x / far * 1.2, 0, away.z / far * 1.2);
            double off = (spot.x - from.x) * sideX + (spot.z - from.z) * sideZ;
            if (off * atSide <= 0 || toSegment(spot, from, eye) < clear || Math.hypot(spot.x - c.x, spot.z - c.z) > 11) {
                continue;
            }
            if (best == null || spot.distanceToSqr(at) < best.distanceToSqr(at)) {
                best = spot;
            }
        }
        return best == null ? null : BossFx.ground(BossFx.world(boss), best.x, best.z, boss.getY());
    }

    /** Horizontal distance from {@code v} to the segment from {@code a} to {@code b}. */
    private static double toSegment(Vec3 v, Vec3 a, Vec3 b) {
        double dx = b.x - a.x;
        double dz = b.z - a.z;
        double l2 = dx * dx + dz * dz;
        double t = l2 < 1e-9 ? 0 : Math.max(0, Math.min(1, ((v.x - a.x) * dx + (v.z - a.z) * dz) / l2));
        return Math.hypot(v.x - (a.x + t * dx), v.z - (a.z + t * dz));
    }

    /** A spot with a rock between it and the Doom, nearest {@code at} (round 44: the nearest cover, as a player takes it). */
    private static Vec3 shelterSpot(DataBossEntity boss, DoomDelveService.DoomScript s, Vec3 at) {
        Vec3 best = null;
        for (Vec3 r : s.walkRocks()) {
            Vec3 away = r.subtract(boss.position());
            double len = Math.hypot(away.x, away.z);
            if (len < 0.5) {
                continue;
            }
            Vec3 spot = r.add(away.x / len * 1.2, 0, away.z / len * 1.2);
            if (best == null || spot.distanceToSqr(at) < best.distanceToSqr(at)) {
                best = spot;
            }
        }
        return best == null ? null : BossFx.ground(BossFx.world(boss), best.x, best.z, boss.getY());
    }

    /** Delve 8+: the larvae outside the shield come as one giant larva (two demonbane hits). */
    private static String giantLarva(DataBossEntity boss, DoomDelveService.DoomScript s, BossWalks.Driver d) {
        ServerPlayer p = d.player();
        if (!DoomDelves.giantLarvae(8) || DoomDelves.larvaeOutsideShield(8) != 1) {
            throw fail("delve 8 has no giant larva");
        }
        s.walkSpawnLarva(boss, p, true);
        List<Mob> larvae = s.walkLarvae();
        Mob giant = larvae.isEmpty() ? null : larvae.get(larvae.size() - 1);
        if (giant == null || !ModernBosses.DOOM_GIANT_LARVA.equals(com.runecraft.combat.MonsterStats.of(giant).profile().id)) {
            throw fail("delve 8: no giant larva spawned");
        }
        p.setItemInHand(InteractionHand.MAIN_HAND, Inv.stack("arclight", 1));
        BossWalks.attack(p, giant);
        d.ticks(boss, 1);
        if (!giant.isAlive()) {
            throw fail("delve 8: one demonbane hit killed a giant larva (4 hitpoints)");
        }
        BossWalks.attack(p, giant);
        d.ticks(boss, 1);
        if (giant.isAlive()) {
            throw fail("delve 8: two demonbane hits left the giant larva alive");
        }
        return "giant larva killed in two demonbane hits";
    }

    /** Delve 4: a kill by a melee punish scatters holy water (prayer and special attack energy restored). */
    private static String punishKill(DataBossEntity boss, DoomDelveService.DoomScript s, BossWalks.Driver d) {
        ServerPlayer p = d.player();
        PlayerData data = Players.data(p);
        data.setPrayerPoints(10);
        com.runecraft.content.systems.SysState.of(p).specEnergy = 0;
        s.walkStartBeam(boss, p);
        boss.setHealth(Math.min(boss.getHealth(), 5f));
        p.setItemInHand(InteractionHand.MAIN_HAND, Inv.stack("abyssal_whip", 1));
        BossWalks.hit(boss, p, 10, AttackType.SLASH);
        d.ticks(boss, 2);
        if (boss.isAlive()) {
            throw fail("delve 4: the punish didn't kill the Doom");
        }
        if (data.prayerPoints() < 10 + DoomDelves.HOLY_WATER_PRAYER
                || com.runecraft.content.systems.SysState.of(p).specEnergy < DoomDelves.HOLY_WATER_SPEC) {
            throw fail("delve 4: no holy water after a punish kill (prayer " + data.prayerPoints() + ", spec "
                    + com.runecraft.content.systems.SysState.of(p).specEnergy + ")");
        }
        return "punish kill scattered holy water";
    }
}
