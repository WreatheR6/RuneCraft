package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.PoisonService;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.bosses.ZulrahVorkath;
import com.runecraft.osrs.content.bosses.ZulrahVorkath.Form;
import com.runecraft.osrs.content.bosses.ZulrahVorkath.Spot;
import com.runecraft.osrs.content.bosses.ZulrahVorkath.Step;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Zulrah: follows one of four fixed rotations. Each step it surfaces at a spot in one of its forms
 * (serpentine = ranged, immune to melee; magma = stares at your tile then slams it; tanzanite =
 * magic and ranged; "Jad" phase = rapidly alternating), attacks a set number of times, then dives
 * (invulnerable) and resurfaces as the next step. Clouds of toxic fumes and snakelings are spat on
 * some steps. The three forms are separate entities (separate models) sharing one hitpoint pool.
 */
final class ZulrahScript extends ScriptedBoss {
    private record Cloud(Vec3d pos, long until) {
    }

    private final String formId;
    private int rotation = -1;
    private int step;
    private int attacksLeft;
    private int surfacing;
    private int diving = -1;
    private boolean jadMagic;
    private final List<Cloud> clouds = new ArrayList<>();
    private final List<MobEntity> snakelings = new ArrayList<>();

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
        return rot.get(step % rot.size());
    }

    private void restore(DataBossEntity boss) {
        String tag = boss.instanceTag;
        if (tag != null && tag.startsWith("zulrah;")) {
            String[] p = tag.split(";");
            rotation = Integer.parseInt(p[1]);
            step = Integer.parseInt(p[2]);
            home = new BlockPos(Integer.parseInt(p[3]), Integer.parseInt(p[4]), Integer.parseInt(p[5]));
        } else {
            rotation = boss.getRandom().nextInt(ZulrahVorkath.ROTATIONS.size());
            step = 0;
            home = boss.getBlockPos();
        }
        surface(boss);
    }

    private void surface(DataBossEntity boss) {
        Step s = current();
        Spot spot = s.spot();
        Vec3d to = BossFx.ground(BossFx.world(boss), home.getX() + 0.5 + spot.dx, home.getZ() + 0.5 + spot.dz, home.getY());
        boss.requestTeleport(to.x, to.y, to.z);
        attacksLeft = s.attacks();
        surfacing = 2;
        jadMagic = s.form() == Form.JAD_MAGIC;
        BossFx.burst(boss, ParticleTypes.SPLASH, 40, 1.5);
        BossFx.sound(boss, SoundEvents.ENTITY_GENERIC_SPLASH, 0.6f);
        String hint = switch (s.form()) {
            case SERPENTINE -> "Zulrah surfaces in its serpentine form! (ranged - protect from missiles)";
            case MAGMA -> "Zulrah surfaces in its magma form! Don't stand where it stares!";
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
        return rotation >= 0 && surfacing <= 0 && diving < 0 && attacksLeft > 0;
    }

    @Override
    protected AttackDef nextAttack(DataBossEntity boss, ServerPlayerEntity target, double distance) {
        Step s = current();
        int done = s.attacks() - attacksLeft;
        if (s.clouds() && done < 2) {
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
    protected void afterAttack(DataBossEntity boss, ServerPlayerEntity target, AttackDef attack) {
        attacksLeft--;
        if (attacksLeft == 3 && current().snakelings()) {
            snakelings.removeIf(m -> !m.isAlive());
            if (snakelings.size() < 4) {
                MobEntity m = BossFx.spawnMinion(boss, "snakeling", around(boss, target.getPos(), 3), target);
                if (m != null) {
                    snakelings.add(m);
                }
            }
        }
        if (attacksLeft <= 0) {
            diving = 3;
            cooldown = 0;
        }
    }

    private void spitClouds(DataBossEntity boss, ServerPlayerEntity target) {
        for (int i = 0; i < 2; i++) {
            Vec3d at = around(boss, target.getPos(), 3);
            clouds.add(new Cloud(at, now + 30));
        }
        BossFx.warn(boss, "Zulrah spits clouds of toxic fumes!");
    }

    @Override
    public int onHitPlayer(DataBossEntity boss, ServerPlayerEntity player, int damage, AttackType type) {
        if (last == ZulrahVorkath.ZULRAH_TAIL_SLAM && damage > 0) {
            BossFx.stun(player, 3);
            com.runecraft.util.Msg.red(player, "Zulrah's tail slam stuns you!");
        }
        return damage;
    }

    @Override
    protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
        if (rotation < 0) {
            restore(boss);
            return;
        }
        if (surfacing > 0) {
            surfacing--;
        }
        tickClouds(boss);
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
        Step next = current();
        String state = "zulrah;" + rotation + ";" + step + ";" + home.getX() + ";" + home.getY() + ";" + home.getZ();
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
            BossFx.world(boss).spawnParticles(ParticleTypes.SNEEZE, c.pos().x, c.pos().y + 0.4, c.pos().z, 12, 1.0, 0.3, 1.0, 0.0);
            for (ServerPlayerEntity p : BossFx.world(boss).getPlayers(pl -> pl.isAlive() && pl.getPos().squaredDistanceTo(c.pos()) < 2.5)) {
                CombatHandler.applyDamage(boss, p, 1 + p.getRandom().nextInt(5));
                if (!PoisonService.isEnvenomed(p)) {
                    PoisonService.venom(p, now);
                }
            }
        }
    }

    @Override
    public void onDeath(DataBossEntity boss, ServerPlayerEntity killer) {
        snakelings.forEach(MobEntity::discard);
    }
}
