package com.runecraft.boss;

import com.runecraft.action.ActionManager;
import com.runecraft.collection.CollectionLog;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.bosses.AwakenedBosses;
import com.runecraft.osrs.content.bosses.BossVariant;
import com.runecraft.osrs.content.bosses.DoomDelves;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModEntities;
import com.runecraft.skill.Drops;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

/** A boss driven entirely by its {@link BossDef} plus an optional {@link BossScript}. */
public class DataBossEntity extends OsrsMonsterEntity {
    /** Marks a script that already scales incoming damage to {@link #trueHitpoints()} above the health cap. */
    public interface HpScaled {
    }

    private final ServerBossBar bossBar;
    private final BossScript script;
    private int phase = -1;
    /** Raid / instance owner tag so raids can track their bosses. */
    public String instanceTag;
    /** Health shown in Minecraft is capped; bosses with more OSRS hitpoints take proportionally less damage. */
    public static final int MAX_MC_HEALTH = 1024;
    /** Awakened DT2 boss or Doom delve level (null: the ordinary fight). Saved with the entity. */
    private BossVariant variant;

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

    public BossVariant variant() {
        return variant;
    }

    public boolean awakened() {
        return variant != null && variant.awakened();
    }

    /** The Doom of Mokhaiotl's delve level, or 0. */
    public int delve() {
        return variant == null ? 0 : variant.delve();
    }

    /** OSRS hitpoints of this boss (its variant's when it has one). */
    public int trueHitpoints() {
        BossDef def = def();
        return variant != null ? variant.hitpoints() : def == null ? Math.round(getMaxHealth()) : def.hitpoints;
    }

    /** Max hit multiplier of an attack for this boss's variant (1 without one). */
    public double variantHitScale(AttackDef attack) {
        return variant == null || attack == null || attack.maxHit() <= 0 ? 1.0 : variant.maxHit(attack) / (double) attack.maxHit();
    }

    /** Turns this boss into a variant (awakened, delve level): levels, attack speed, hitpoints and the boss bar. */
    public void applyVariant(BossVariant v) {
        applyVariant(v, true);
    }

    private void applyVariant(BossVariant v, boolean heal) {
        this.variant = v;
        if (v == null) {
            return;
        }
        overrideStats(s -> new com.runecraft.combat.MonsterStats(v.name(), v.combatLevel(), v.hitpoints(), v.attack(), v.strength(),
                v.defence(), v.magic(), v.ranged(), s.defenceBonus(), v.attackSpeed() > 0 ? v.attackSpeed() : s.attackSpeed(), s.style(),
                s.maxHit(), s.monster(), s.profile(), s.boss()));
        if (heal) {
            int shown = Math.min(v.hitpoints(), MAX_MC_HEALTH);
            EntityAttributeInstance inst = getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            if (inst != null) {
                inst.setBaseValue(shown);
            }
            setHealth(shown);
        }
        bossBar.setName(Text.literal(v.name()));
        bossBar.setColor(v.awakened() ? BossBar.Color.PURPLE : BossBar.Color.RED);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (variant != null) {
            nbt.putString("RcVariant", variant.kind());
            nbt.putInt("RcVariantLevel", variant.level());
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        BossDef def = def();
        if (def != null && nbt.contains("RcVariant")) {
            String kind = nbt.getString("RcVariant");
            if (BossVariant.AWAKENED.equals(kind) && AwakenedBosses.isDt2(def.id)) {
                applyVariant(AwakenedBosses.variant(def), false);
            } else if (BossVariant.DELVE.equals(kind) && DoomDelves.BOSS.equals(def.id)) {
                applyVariant(DoomDelves.variant(def, nbt.getInt("RcVariantLevel")), false);
            }
        }
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
        double m = def != null && phase >= 0 ? def.phases.get(phase).damageMultiplier() : 1.0;
        return m * variantHitScale(nextAttack);
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
        int d = script.modifyIncoming(this, attacker, super.modifyIncoming(attacker, damage, type), type);
        if (variant != null && !(script instanceof HpScaled) && d > 0 && trueHitpoints() > MAX_MC_HEALTH) {
            // Variants beyond Minecraft's health cap take proportionally less damage (scripted bosses scale it themselves).
            double scaled = d * (double) MAX_MC_HEALTH / trueHitpoints();
            int whole = (int) scaled;
            d = whole + (getRandom().nextDouble() < scaled - whole ? 1 : 0);
        }
        return d;
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
        for (Drops.Rolled r : Drops.roll(variant != null ? variant.drops() : def.drops, getRandom(), false)) {
            dropStack(com.runecraft.combat.CombatHooks.dropStack(player, this, r.item(), r.count()));
            CollectionLog.obtain(player, r.item());
        }
        int petRate = variant != null && variant.petRate() >= 0 ? variant.petRate()
                : com.runecraft.osrs.content.extras.Pets.engineRate(def.id);
        if (def.pet != null && petRate > 0 && getRandom().nextInt(petRate) == 0) {
            Inv.give(player, def.pet, 1);
            CollectionLog.obtain(player, def.pet);
            player.getServer().getPlayerManager().broadcast(Text.literal(player.getName().getString()
                    + " has a funny feeling like they're being followed.").formatted(Formatting.GOLD), false);
        }
        PlayerData data = Players.data(player);
        String kcKey = variant != null ? variant.kcKey() : def.id;
        int kc = data.incrementKillCount(kcKey);
        Msg.game(player, "Your " + (variant != null && variant.awakened() ? variant.name() : def.name) + " kill count is: " + kc + ".");
        script.onDeath(this, player);
        DataBossService.killed(player, this);
        bossBar.clearPlayers();
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        bossBar.clearPlayers();
    }
}
