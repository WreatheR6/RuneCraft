package com.runecraft.content.godwars;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.combat.MonsterStats;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.godwars.WildernessBosses;
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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Vet'ion and Calvar'ion (OSRS Wiki, post-2023 rework). His sword swings are cosmetic: every attack is lightning, a 3x3 strike
 * on his target's tile (move off it) plus bolts on random tiles around him (one per player, four more when solo, at half
 * damage - approx: indirect hits "deal less"), and in melee distance now and then a telegraphed shield bash. At half health
 * in each form he summons two hellhounds and is immune until they're dead ("Kill, my pets!"). Killing the first form only
 * angers him: he rises enraged with full health ("Do it again!!"), attacking every 5 ticks instead of 6, and summons greater
 * hellhounds at half health of the second form.
 */
final class VetionScript extends GwdScript {
    /** Random bolts around the arena on top of the one on the target when fighting alone (OSRS Wiki: five in all). */
    private static final int EXTRA_BOLTS = 4;

    private final boolean calvarion;
    private final AttackDef lightning;
    private final AttackDef bash;
    private int form = 1;
    private boolean houndsSummoned;
    private final List<Mob> hounds = new ArrayList<>();

    VetionScript(boolean calvarion) {
        this.calvarion = calvarion;
        this.lightning = calvarion ? WildernessBosses.CALVARION_LIGHTNING : WildernessBosses.VETION_LIGHTNING;
        this.bash = calvarion ? WildernessBosses.CALVARION_BASH : WildernessBosses.VETION_BASH;
    }

    private String name() {
        return calvarion ? "Calvar'ion" : "Vet'ion";
    }

    @Override
    protected AttackDef pick(DataBossEntity boss, ServerPlayer target, double distance) {
        if (distance <= meleeReach(boss) && boss.getRandom().nextInt(4) == 0) { // approx: "occasionally"
            return bash;
        }
        boltsAround(boss, target);
        return lightning;
    }

    /** The bolts that come with each lightning attack, on random tiles around him (the target's own comes from the attack). */
    private void boltsAround(DataBossEntity boss, ServerPlayer target) {
        ServerLevel world = (ServerLevel) boss.level();
        long now = com.runecraft.action.ActionManager.gameTick();
        int extra = Math.min(7, EXTRA_BOLTS + players(boss, 24).size() - 1);
        for (int i = 0; i < extra; i++) {
            double a = boss.getRandom().nextDouble() * Math.PI * 2;
            double r = 2 + boss.getRandom().nextDouble() * 9;
            double x = boss.getX() + Math.cos(a) * r;
            double z = boss.getZ() + Math.sin(a) * r;
            int top = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
            double y = Math.abs(top - boss.getY()) > 4 ? boss.getY() : top;
            strike("lightning", boss, new Vec3(x, y, z), 3, lightning.maxHit() / 2, 1.5, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.FLASH, null);
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(world);
            if (bolt != null) {
                bolt.setVisualOnly(true);
                bolt.moveTo(x, y, z);
                pendingBolts.add(new Bolt(bolt, now + 3));
            }
        }
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
        boss.setCustomName(Component.literal(name()));
        // Enraged: attacks every 5 ticks instead of 6 (OSRS Wiki infobox), any drained Defence restored.
        boss.overrideStats(s -> new MonsterStats(s.name(), s.combatLevel(), s.hitpoints(), s.attack(), s.strength(), s.defence(),
                s.magic(), s.ranged(), s.defenceBonus(), Math.max(1, s.attackSpeed() - 1), s.style(), s.maxHit(), s.monster(),
                s.profile(), s.boss()));
        shout(boss, "Do it again!!");
        ServerLevel world = (ServerLevel) boss.level();
        world.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, boss.getX(), boss.getY() + 1.5, boss.getZ(), 60, 0.8, 1.2, 0.8, 0.05);
        for (ServerPlayer p : players(boss, 24)) {
            Msg.red(p, name() + " rises again, enraged and wreathed in flame!");
        }
        // His blast of energy on changing form: the same lightning around him.
        if (boss.getTarget() instanceof ServerPlayer target) {
            boltsAround(boss, target);
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
