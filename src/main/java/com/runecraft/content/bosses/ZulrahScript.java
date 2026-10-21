package com.runecraft.content.bosses;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.ThreatCues;
import com.runecraft.combat.CombatHandler;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.bosses.BossRealtime;
import com.runecraft.osrs.content.bosses.ZulrahVorkath;
import com.runecraft.osrs.content.bosses.ZulrahVorkath.Form;
import com.runecraft.osrs.content.bosses.ZulrahVorkath.Spot;
import com.runecraft.osrs.content.bosses.ZulrahVorkath.Step;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Zulrah: follows one of four fixed rotations (OSRS Wiki, Zulrah/Strategies). Each phase it surfaces at a spot in
 * one of its forms (green = ranged; crimson = stares at your tile then slams it; tanzanite = magic and ranged; green
 * "Jad" phase = alternating ranged and magic every attack) and performs a fixed sequence of attacks, venom cloud
 * barrages and snakeling orbs, then dives (invulnerable) and resurfaces as the next phase. The three forms are separate
 * entities (separate models) sharing one hitpoint pool. Hits above 50 deal 45-50; venom clouds hurt but don't
 * envenom; snakelings die off after 40 seconds.
 *
 * <p>Real time (round 42, // playability): a ring of the form's style colour floats over its head while it is up (green
 * ranged, crimson melee, tanzanite magic; the Jad phase shows the colour of its next attack), and each Jad-phase attack is cued
 * like TzTok-Jad's, its style's colour glowing {@value BossRealtime#ZULRAH_JAD_LEAD_TICKS} game ticks before it lands, so the
 * alternation can be read and prayed attack by attack (OSRS: only the projectile, within its short hit delay). Its 3-tick
 * cadence and every max hit are unchanged.
 */
final class ZulrahScript extends ScriptedBoss {
    /** The Jad phase's attacks: the same venom spit and bolt, cued {@value BossRealtime#ZULRAH_JAD_LEAD_TICKS} ticks ahead. */
    static final AttackDef JAD_RANGED = ZulrahVorkath.ZULRAH_RANGED.telegraph(BossRealtime.ZULRAH_JAD_LEAD_TICKS, "Zulrah: ranged!");
    static final AttackDef JAD_MAGIC = ZulrahVorkath.ZULRAH_MAGIC.telegraph(BossRealtime.ZULRAH_JAD_LEAD_TICKS, "Zulrah: magic!");

    private record Cloud(Vec3 pos, long until) {
    }

    private record Snakeling(Mob mob, long until) {
    }

    private final String formId;
    private int rotation = -1;
    private int step;
    /** After the first full rotation, phase 1 is {@link ZulrahVorkath#LOOP_START} instead of the opening. */
    private boolean looped;
    private int action;
    private int surfacing;
    private int diving = -1;
    private boolean jadMagic;
    private final List<Cloud> clouds = new ArrayList<>();
    /** The venom clouds' 3x3-tile areas as green floor blocks while they last (round 43). */
    private com.runecraft.boss.FloorMarkers.Layer cloudFloor;
    private final List<Snakeling> snakelings = new ArrayList<>();

    ZulrahScript(String formId) {
        this.formId = formId;
    }

    /**
     * Self-test: told of every surfacing ("surface FORM SPOT rotation/phase"), venom cloud barrage ("cloud") and snakeling orb
     * ("snakeling"), across the form swaps (each form is its own entity and script). Null when nobody listens.
     */
    static java.util.function.Consumer<String> trace;

    private static void trace(String event) {
        if (trace != null) {
            trace.accept(event);
        }
    }

    /** The rotation (0-3) and the phase within it this form is in, or null before it surfaced (self-test). */
    int[] position() {
        return rotation < 0 ? null : new int[]{rotation, step};
    }

    @Override
    protected boolean driven() {
        return true;
    }

    @Override
    protected String creditAs() {
        return formId.equals("zulrah") ? null : "zulrah";
    }

    private Step current() {
        List<Step> rot = ZulrahVorkath.ROTATIONS.get(rotation);
        return step == 0 && looped ? ZulrahVorkath.LOOP_START : rot.get(step % rot.size());
    }

    private void restore(DataBossEntity boss) {
        String tag = boss.instanceTag;
        if (tag != null && tag.startsWith("zulrah;")) {
            String[] p = tag.split(";");
            rotation = Integer.parseInt(p[1]);
            step = Integer.parseInt(p[2]);
            home = new BlockPos(Integer.parseInt(p[3]), Integer.parseInt(p[4]), Integer.parseInt(p[5]));
            looped = p.length > 6 && p[6].equals("1");
        } else {
            rotation = boss.getRandom().nextInt(ZulrahVorkath.ROTATIONS.size());
            step = 0;
            home = boss.blockPosition();
            // A fresh spawn of a later form (not handed a state by swapForm): start at a phase of that form,
            // so a crimson or tanzanite Zulrah doesn't fight a green phase and swap form on its first dive.
            if (!current().form().bossId.equals(formId)) {
                findStep:
                for (int r = 0; r < ZulrahVorkath.ROTATIONS.size(); r++) {
                    List<Step> rot = ZulrahVorkath.ROTATIONS.get((rotation + r) % ZulrahVorkath.ROTATIONS.size());
                    for (int i = 0; i < rot.size(); i++) {
                        if (rot.get(i).form().bossId.equals(formId)) {
                            rotation = (rotation + r) % ZulrahVorkath.ROTATIONS.size();
                            step = i;
                            break findStep;
                        }
                    }
                }
            }
        }
        surface(boss);
    }

    private void surface(DataBossEntity boss) {
        Step s = current();
        Spot spot = s.spot();
        Vec3 to = BossFx.ground(BossFx.world(boss), home.getX() + 0.5 + spot.dx, home.getZ() + 0.5 + spot.dz, home.getY());
        boss.teleportTo(to.x, to.y, to.z);
        action = 0;
        surfacing = 2;
        jadMagic = s.form() == Form.JAD_MAGIC;
        trace("surface " + s.form() + " " + s.spot() + " " + rotation + "/" + step);
        BossFx.burst(boss, ParticleTypes.SPLASH, 40, 1.5);
        BossFx.sound(boss, SoundEvents.GENERIC_SPLASH, 0.6f);
        String hint = switch (s.form()) {
            case SERPENTINE -> "Zulrah surfaces in its green form! (ranged - protect from missiles)";
            case MAGMA -> "Zulrah surfaces in its crimson form (melee)! Don't stand where it stares!";
            case TANZANITE -> "Zulrah surfaces in its tanzanite form! (magic and ranged)";
            case JAD_RANGED -> "Zulrah begins to alternate its attacks rapidly, starting with ranged!";
            case JAD_MAGIC -> "Zulrah begins to alternate its attacks rapidly, starting with magic!";
        };
        BossFx.warn(boss, hint);
    }

    @Override
    public boolean isVulnerable(DataBossEntity boss) {
        return surfacing <= 0 && diving < 0;
    }

    @Override
    protected boolean canAttack(DataBossEntity boss) {
        return rotation >= 0 && surfacing <= 0 && diving < 0 && action < current().actions().length();
    }

    @Override
    protected AttackDef nextAttack(DataBossEntity boss, ServerPlayer target, double distance) {
        Step s = current();
        char what = s.actions().charAt(action);
        if (what == 'S') {
            spawnSnakeling(boss, target);
            next(boss);
            cooldown = 2;
            return null;
        }
        if (what == 'C') {
            spitClouds(boss, target);
            return ZulrahVorkath.ZULRAH_CLOUD;
        }
        return switch (s.form()) {
            case SERPENTINE -> ZulrahVorkath.ZULRAH_RANGED;
            case MAGMA -> ZulrahVorkath.ZULRAH_TAIL_SLAM;
            case TANZANITE -> boss.getRandom().nextInt(4) < ZulrahVorkath.TANZANITE_MAGIC_IN_4
                    ? ZulrahVorkath.ZULRAH_MAGIC : ZulrahVorkath.ZULRAH_RANGED;
            case JAD_RANGED, JAD_MAGIC -> {
                // playability: cued like TzTok-Jad's (OSRS: only the projectile shows the style)
                AttackDef a = jadMagic ? JAD_MAGIC : JAD_RANGED;
                jadMagic = !jadMagic;
                yield a;
            }
        };
    }

    @Override
    protected int attackDelay(DataBossEntity boss, AttackDef attack) {
        return attack == ZulrahVorkath.ZULRAH_TAIL_SLAM ? 6 : 3;
    }

    /** The style colour of the form that is up (the Jad phase: its next attack's), or -1 while it is under water. */
    int formRgb() {
        if (rotation < 0 || diving >= 0) {
            return -1;
        }
        return switch (current().form()) {
            case SERPENTINE -> ThreatCues.RANGED_RGB;
            case MAGMA -> ThreatCues.MELEE_RGB;
            case TANZANITE -> ThreatCues.MAGIC_RGB;
            case JAD_RANGED, JAD_MAGIC -> jadMagic ? ThreatCues.MAGIC_RGB : ThreatCues.RANGED_RGB;
        };
    }

    /**
     * Every 4 Minecraft ticks: the form's colour as a ring over its head (the Jad phase: its next attack's). // playability: OSRS
     * shows the form by its model's colour alone, which blocky models and particles can't carry
     */
    @Override
    public void tick(DataBossEntity boss) {
        super.tick(boss);
        int rgb = formRgb();
        if (rgb < 0 || ActionManager.mcTick() % 4 != 0) {
            return;
        }
        var world = BossFx.world(boss);
        var dust = ThreatCues.dust(rgb, 1.6f);
        double y = boss.getY() + boss.getBbHeight() + 0.6;
        for (int i = 0; i < 10; i++) {
            double a = Math.PI * 2 * i / 10;
            world.sendParticles(dust, boss.getX() + Math.cos(a) * 0.7, y, boss.getZ() + Math.sin(a) * 0.7, 1, 0, 0, 0, 0);
        }
    }

    @Override
    protected void afterAttack(DataBossEntity boss, ServerPlayer target, AttackDef attack) {
        next(boss);
    }

    /** Moves on to the phase's next action, diving after the last one. */
    private void next(DataBossEntity boss) {
        action++;
        if (action >= current().actions().length()) {
            diving = 3;
            cooldown = 0;
        }
    }

    @Override
    protected int incoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
        if (type.isMelee() && attacker != null && !ZulrahVorkath.reachesZulrah(BossFx.heldId(attacker))) {
            com.runecraft.util.Msg.action(attacker, "Zulrah is out of reach in the swamp: only a halberd can reach it.");
            return 0;
        }
        return ZulrahVorkath.capDamage(damage, boss.getRandom().nextInt(6));
    }

    private void spawnSnakeling(DataBossEntity boss, ServerPlayer target) {
        snakelings.removeIf(sn -> !sn.mob().isAlive());
        trace("snakeling");
        BossFx.world(boss).sendParticles(ParticleTypes.END_ROD, boss.getX(), boss.getY() + 2.5, boss.getZ(), 6, 0.3, 0.3, 0.3, 0.02);
        if (snakelings.size() >= 8) {
            return;
        }
        Mob m = BossFx.spawnMinion(boss, "snakeling", around(boss, target.position(), 3), target);
        if (m != null) {
            snakelings.add(new Snakeling(m, now + ZulrahVorkath.SNAKELING_LIFE_TICKS));
        }
    }

    /**
     * A barrage of two venom clouds. The four-barrage phases fill the middle of the shrine (the opening leaves its east and
     * west tips clear, OSRS Wiki); other barrages land around the player. approx: cloud spots and their 30-tick life.
     */
    private void spitClouds(DataBossEntity boss, ServerPlayer target) {
        Step s = current();
        boolean fill = s == ZulrahVorkath.OPENING || s == ZulrahVorkath.LOOP_START || s.cloudBarrages() >= 4;
        for (int i = 0; i < 2; i++) {
            Vec3 at = fill ? around(boss, homeVec(), 5) : around(boss, target.position(), 6);
            clouds.add(new Cloud(at, now + 30));
            // drawn where it lands at once; it hurts from the next game tick (12 MC on, as hazards arm). // playability: OSRS
            // the cloud projectile's landing spot, read on the tick
            BossFx.world(boss).sendParticles(ParticleTypes.SNEEZE, at.x, at.y + 0.4, at.z, 24, 1.4, 0.3, 1.4, 0.0);
        }
        trace("cloud");
        BossFx.warn(boss, "Zulrah spits clouds of toxic fumes!");
        showClouds(boss);
    }

    /**
     * The clouds' areas on the floor (round 43): exactly the square {@link ZulrahVorkath#inCloud} hurts in, green from the moment
     * a cloud lands until it fades (put back then, when this form dives away or dies, or when the server stops); only where
     * the shrine has a plain floor block (not over the swamp's water). // playability: OSRS shows the cloud over its tiles
     */
    private void showClouds(DataBossEntity boss) {
        if (cloudFloor == null) {
            cloudFloor = new com.runecraft.boss.FloorMarkers.Layer(boss);
        }
        com.runecraft.boss.FloorMarkers.Shape shape = new com.runecraft.boss.FloorMarkers.Shape(BossFx.world(boss));
        for (Cloud c : clouds) {
            shape.box(c.pos(), ZulrahVorkath.CLOUD_HALF_WIDTH, com.runecraft.boss.HazardKind.POISON);
        }
        cloudFloor.show(shape);
    }

    /** Self-test: the floor blocks the venom clouds mark now. */
    int cloudFloorMarked() {
        return cloudFloor == null ? 0 : cloudFloor.size();
    }

    @Override
    public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
        if (last == ZulrahVorkath.ZULRAH_TAIL_SLAM && damage > 0) {
            damage = Math.max(damage, ZulrahVorkath.ZULRAH_TAIL_SLAM_MIN); // 20-30 when it lands
            BossFx.stun(player, 3);
            com.runecraft.util.Msg.red(player, "Zulrah's tail slam stuns you!");
        }
        return damage;
    }

    @Override
    protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
        if (rotation < 0) {
            restore(boss);
            return;
        }
        if (surfacing > 0) {
            surfacing--;
        }
        tickClouds(boss);
        for (Iterator<Snakeling> it = snakelings.iterator(); it.hasNext(); ) {
            Snakeling sn = it.next();
            if (!sn.mob().isAlive()) {
                it.remove();
            } else if (now >= sn.until()) {
                BossFx.burst(sn.mob(), ParticleTypes.SMOKE, 6, 0.3);
                sn.mob().discard();
                it.remove();
            }
        }
        if (diving > 0) {
            diving--;
            BossFx.burst(boss, ParticleTypes.BUBBLE, 20, 1.0);
            if (diving == 0) {
                dive(boss);
            }
        }
    }

    private void dive(DataBossEntity boss) {
        BossFx.burst(boss, ParticleTypes.SPLASH, 40, 1.5);
        step++;
        if (step >= ZulrahVorkath.ROTATIONS.get(rotation).size()) {
            // A full rotation is over: the next one (possibly the same) starts with the green ranged phase.
            rotation = boss.getRandom().nextInt(ZulrahVorkath.ROTATIONS.size());
            step = 0;
            looped = true;
        }
        Step next = current();
        String state = "zulrah;" + rotation + ";" + step + ";" + home.getX() + ";" + home.getY() + ";" + home.getZ() + ";" + (looped ? 1 : 0);
        if (!next.form().bossId.equals(formId)) {
            BossFx.swapForm(boss, next.form().bossId, home, false, state);
            return;
        }
        diving = -1;
        surface(boss);
    }

    private void tickClouds(DataBossEntity boss) {
        if (cloudFloor != null || !clouds.isEmpty()) {
            clouds.removeIf(c -> now > c.until());
            showClouds(boss);
        }
        Iterator<Cloud> it = clouds.iterator();
        while (it.hasNext()) {
            Cloud c = it.next();
            if (now > c.until()) {
                it.remove();
                continue;
            }
            BossFx.world(boss).sendParticles(ParticleTypes.SNEEZE, c.pos().x, c.pos().y + 0.4, c.pos().z, 16, 1.4, 0.3, 1.4, 0.0);
            // A cloud covers a 3x3-tile area (OSRS Wiki): three blocks either way of its centre.
            for (ServerPlayer p : BossFx.world(boss).getPlayers(pl -> pl.isAlive() && ZulrahVorkath.inCloud(
                    pl.getX() - c.pos().x, pl.getY() - c.pos().y, pl.getZ() - c.pos().z))) {
                // The clouds deal venom-like damage every tick but don't envenom (OSRS Wiki).
                com.runecraft.combat.CombatHooks.nameAttack("toxic_cloud", null);
                CombatHandler.applyDamage(boss, p, 1 + p.getRandom().nextInt(5));
            }
        }
    }

    @Override
    public void onDeath(DataBossEntity boss, ServerPlayer killer) {
        snakelings.forEach(sn -> sn.mob().discard());
        if (cloudFloor != null) {
            cloudFloor.clear();
        }
    }
}
