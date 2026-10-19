package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.combat.CombatHandler;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
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
 */
final class ZulrahScript extends ScriptedBoss {
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
    private final List<Snakeling> snakelings = new ArrayList<>();

    ZulrahScript(String formId) {
        this.formId = formId;
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
        BossFx.burst(boss, ParticleTypes.SPLASH, 40, 1.5);
        BossFx.sound(boss, SoundEvents.GENERIC_SPLASH, 0.6f);
        String hint = switch (s.form()) {
            case SERPENTINE -> "Zulrah surfaces in its green form! (ranged - protect from missiles)";
            case MAGMA -> "Zulrah surfaces in its crimson form! Don't stand where it stares!";
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
            case TANZANITE -> boss.getRandom().nextBoolean() ? ZulrahVorkath.ZULRAH_MAGIC : ZulrahVorkath.ZULRAH_RANGED;
            case JAD_RANGED, JAD_MAGIC -> {
                AttackDef a = jadMagic ? ZulrahVorkath.ZULRAH_MAGIC : ZulrahVorkath.ZULRAH_RANGED;
                jadMagic = !jadMagic;
                yield a;
            }
        };
    }

    @Override
    protected int attackDelay(DataBossEntity boss, AttackDef attack) {
        return attack == ZulrahVorkath.ZULRAH_TAIL_SLAM ? 6 : 3;
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
        return ZulrahVorkath.capDamage(damage, boss.getRandom().nextInt(6));
    }

    private void spawnSnakeling(DataBossEntity boss, ServerPlayer target) {
        snakelings.removeIf(sn -> !sn.mob().isAlive());
        BossFx.world(boss).sendParticles(ParticleTypes.END_ROD, boss.getX(), boss.getY() + 2.5, boss.getZ(), 6, 0.3, 0.3, 0.3, 0.02);
        if (snakelings.size() >= 8) {
            return;
        }
        Mob m = BossFx.spawnMinion(boss, "snakeling", around(boss, target.position(), 3), target);
        if (m != null) {
            snakelings.add(new Snakeling(m, now + ZulrahVorkath.SNAKELING_LIFE_TICKS));
        }
    }

    private void spitClouds(DataBossEntity boss, ServerPlayer target) {
        for (int i = 0; i < 2; i++) {
            Vec3 at = around(boss, target.position(), 3);
            clouds.add(new Cloud(at, now + 30));
        }
        BossFx.warn(boss, "Zulrah spits clouds of toxic fumes!");
    }

    @Override
    public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
        if (last == ZulrahVorkath.ZULRAH_TAIL_SLAM && damage > 0) {
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
        Iterator<Cloud> it = clouds.iterator();
        while (it.hasNext()) {
            Cloud c = it.next();
            if (now > c.until()) {
                it.remove();
                continue;
            }
            BossFx.world(boss).sendParticles(ParticleTypes.SNEEZE, c.pos().x, c.pos().y + 0.4, c.pos().z, 12, 1.0, 0.3, 1.0, 0.0);
            for (ServerPlayer p : BossFx.world(boss).getPlayers(pl -> pl.isAlive() && pl.position().distanceToSqr(c.pos()) < 2.5)) {
                // The clouds deal venom-like damage every tick but don't envenom (OSRS Wiki).
                com.runecraft.combat.CombatHooks.nameAttack("toxic_cloud", null);
                CombatHandler.applyDamage(boss, p, 1 + p.getRandom().nextInt(5));
            }
        }
    }

    @Override
    public void onDeath(DataBossEntity boss, ServerPlayer killer) {
        snakelings.forEach(sn -> sn.mob().discard());
    }
}
