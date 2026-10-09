package com.runecraft.boss;

import com.runecraft.action.ActionManager;
import com.runecraft.collection.CollectionLog;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModEntities;
import com.runecraft.skill.Drops;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

/** A boss driven entirely by its {@link BossDef} plus an optional {@link BossScript}. */
public class DataBossEntity extends OsrsMonsterEntity {
    private final ServerBossBar bossBar;
    private final BossScript script;
    private int phase = -1;
    /** Raid / instance owner tag so raids can track their bosses. */
    public String instanceTag;

    public DataBossEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        BossDef def = def();
        this.bossBar = new ServerBossBar(Text.literal(def == null ? "Boss" : def.name), BossBar.Color.RED, BossBar.Style.NOTCHED_10);
        this.script = BossScripts.create(def == null ? "" : def.id);
        setPersistent();
    }

    public BossDef def() {
        return ModEntities.dataBoss(getType());
    }

    public BossScript script() {
        return script;
    }

    public int phase() {
        return phase;
    }

    @Override
    protected boolean shouldAggro(LivingEntity entity) {
        return entity instanceof ServerPlayerEntity p && !p.isCreative() && !p.isSpectator();
    }

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        bossBar.removePlayer(player);
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        bossBar.setPercent(getHealth() / getMaxHealth());
        script.tick(this);
        if (age % ActionManager.MC_TICKS_PER_GAME_TICK == 0) {
            script.gameTick(this, ActionManager.gameTick());
        }
        BossDef def = def();
        if (def != null) {
            float percent = getHealth() * 100f / getMaxHealth();
            for (int i = phase + 1; i < def.phases.size(); i++) {
                BossDef.Phase p = def.phases.get(i);
                if (percent <= p.belowPercent()) {
                    phase = i;
                    if (p.message() != null) {
                        for (ServerPlayerEntity pl : ((ServerWorld) getWorld()).getPlayers(x -> x.squaredDistanceTo(this) < 48 * 48)) {
                            pl.sendMessage(Text.literal(def.name + ": " + p.message()).formatted(Formatting.RED), false);
                        }
                    }
                    if (p.summonMinions()) {
                        summonMinions(def.minionCount);
                    }
                    script.onPhase(this, i);
                }
            }
        }
    }

    @Override
    protected List<AttackDef> attackList() {
        BossDef def = def();
        if (def == null) {
            return null;
        }
        if (phase >= 0 && !def.phases.get(phase).attacks().isEmpty()) {
            return def.phases.get(phase).attacks();
        }
        return def.attacks.isEmpty() ? null : def.attacks;
    }

    @Override
    protected double damageMultiplier() {
        BossDef def = def();
        return def != null && phase >= 0 ? def.phases.get(phase).damageMultiplier() : 1.0;
    }

    @Override
    public AttackType chooseStyle(ServerPlayerEntity target, double distanceSq) {
        AttackDef custom = script.chooseAttack(this, target, Math.sqrt(distanceSq));
        if (custom != null) {
            nextAttack = custom;
            return custom.style();
        }
        return super.chooseStyle(target, distanceSq);
    }

    @Override
    public int modifyIncoming(ServerPlayerEntity attacker, int damage, AttackType type) {
        if (!script.isVulnerable(this)) {
            Msg.action(attacker, "Your attack has no effect right now.");
            return 0;
        }
        return script.modifyIncoming(this, attacker, super.modifyIncoming(attacker, damage, type), type);
    }

    @Override
    public int onHitPlayer(ServerPlayerEntity player, int damage, AttackType type) {
        return script.onHitPlayer(this, player, damage, type);
    }

    /** Spawns the BossDef's minions around the boss. */
    public void summonMinions(int count) {
        BossDef def = def();
        if (def == null || def.minion == null) {
            return;
        }
        EntityType<?> type = ModEntities.typeById(def.minion);
        if (type == null) {
            return;
        }
        ServerWorld world = (ServerWorld) getWorld();
        for (int i = 0; i < count; i++) {
            var e = type.create(world);
            if (e instanceof MobEntity mob) {
                double angle = random.nextDouble() * Math.PI * 2;
                mob.refreshPositionAndAngles(getX() + Math.cos(angle) * 3, getY(), getZ() + Math.sin(angle) * 3, 0, 0);
                mob.initialize(world, world.getLocalDifficulty(mob.getBlockPos()), SpawnReason.MOB_SUMMONED, null, null);
                if (getTarget() != null) {
                    mob.setTarget(getTarget());
                }
                world.spawnEntity(mob);
            }
        }
    }

    @Override
    public void onKilledBy(ServerPlayerEntity player) {
        BossDef def = def();
        if (def == null) {
            return;
        }
        for (Drops.Rolled r : Drops.roll(def.drops, getRandom(), false)) {
            dropStack(Inv.stack(r.item(), r.count()));
            CollectionLog.obtain(player, r.item());
        }
        if (def.pet != null && getRandom().nextInt(3000) == 0) {
            Inv.give(player, def.pet, 1);
            CollectionLog.obtain(player, def.pet);
            player.getServer().getPlayerManager().broadcast(Text.literal(player.getName().getString()
                    + " has a funny feeling like they're being followed.").formatted(Formatting.GOLD), false);
        }
        PlayerData data = Players.data(player);
        int kc = data.incrementKillCount(def.id);
        Msg.game(player, "Your " + def.name + " kill count is: " + kc + ".");
        script.onDeath(this, player);
        bossBar.clearPlayers();
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        bossBar.clearPlayers();
    }
}
