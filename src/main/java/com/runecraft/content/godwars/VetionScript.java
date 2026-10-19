package com.runecraft.content.godwars;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Vet'ion and Calvar'ion. At half health in each form the skeleton summons two hellhounds and is
 * immune until they're dead ("Kill, my pets!"). Killing the first form only angers him: he rises
 * again as Vet'ion Reborn with full health ("Do it again!!") and summons greater hellhounds at half
 * health of the second form. Lightning strikes (move off the marked tile) and a shield bash around
 * him come from the attack data; extra lightning bolts fall around his target.
 */
final class VetionScript extends GwdScript {
    private final boolean calvarion;
    private int form = 1;
    private boolean houndsSummoned;
    private final List<Mob> hounds = new ArrayList<>();

    VetionScript(boolean calvarion) {
        this.calvarion = calvarion;
    }

    private String name() {
        return calvarion ? "Calvar'ion" : "Vet'ion";
    }

    @Override
    public boolean isVulnerable(DataBossEntity boss) {
        hounds.removeIf(h -> h == null || !h.isAlive());
        return hounds.isEmpty();
    }

    @Override
    public int modifyIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
        if (form == 1 && damage >= boss.getHealth()) {
            rebirth(boss);
            return 0;
        }
        return damage;
    }

    private void rebirth(DataBossEntity boss) {
        form = 2;
        houndsSummoned = false;
        boss.setHealth(boss.getMaxHealth());
        boss.setCustomName(Component.literal(name() + " Reborn"));
        shout(boss, "Do it again!!");
        ServerLevel world = (ServerLevel) boss.level();
        world.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, boss.getX(), boss.getY() + 1.5, boss.getZ(), 60, 0.8, 1.2, 0.8, 0.05);
        for (ServerPlayer p : players(boss, 24)) {
            Msg.red(p, name() + " rises again, wreathed in flame!");
        }
    }

    @Override
    public void gameTick(DataBossEntity boss, long gameTick) {
        super.gameTick(boss, gameTick);
        if (!houndsSummoned && boss.getHealth() <= boss.getMaxHealth() / 2) {
            houndsSummoned = true;
            shout(boss, form == 1 ? "Kill, my pets!" : "Bahh! Go, Dogs!!");
            String hound = form == 1 ? "skeleton_hellhound" : "greater_skeleton_hellhound";
            hounds.add(spawnMinion(boss, hound, 2, 0));
            hounds.add(spawnMinion(boss, hound, -2, 0));
        }
        // Extra lightning around his target every few ticks.
        if (gameTick % 9 == 0 && boss.getTarget() instanceof ServerPlayer target && target.distanceToSqr(boss) < 24 * 24) {
            ServerLevel world = (ServerLevel) boss.level();
            for (int i = 0; i < 2; i++) {
                double x = target.getX() + boss.getRandom().nextInt(5) - 2;
                double z = target.getZ() + boss.getRandom().nextInt(5) - 2;
                Vec3 pos = new Vec3(x, target.getY(), z);
                strike(boss, pos, 3, calvarion ? 22 : 30, 1.2, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.FLASH, (p, dmg) -> {
                });
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(world);
                if (bolt != null) {
                    bolt.setVisualOnly(true);
                    bolt.moveTo(x, target.getY(), z);
                    pendingBolts.add(new Bolt(bolt, gameTick + 3));
                }
            }
        }
        if (!pendingBolts.isEmpty()) {
            ServerLevel world = (ServerLevel) boss.level();
            pendingBolts.removeIf(b -> {
                if (b.due() <= gameTick) {
                    world.addFreshEntity(b.bolt());
                    return true;
                }
                return false;
            });
        }
    }

    /** Cosmetic lightning that lands together with its strike. */
    private record Bolt(LightningBolt bolt, long due) {
    }

    private final List<Bolt> pendingBolts = new ArrayList<>();

    @Override
    public void onDeath(DataBossEntity boss, ServerPlayer killer) {
        shout(boss, "Urk! I... failed...");
        for (Mob h : hounds) {
            if (h != null && h.isAlive()) {
                h.discard();
            }
        }
        super.onDeath(boss, killer);
    }
}
