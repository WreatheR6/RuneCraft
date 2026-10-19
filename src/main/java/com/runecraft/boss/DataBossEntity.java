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
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** A boss driven entirely by its {@link BossDef} plus an optional {@link BossScript}. */
public class DataBossEntity extends OsrsMonsterEntity {
    /** Marks a script that already scales incoming damage to {@link #trueHitpoints()} above the health cap. */
    public interface HpScaled {
    }

    private final ServerBossEvent bossBar;
    private final BossScript script;
    private int phase = -1;
    /** Raid / instance owner tag so raids can track their bosses. */
    public String instanceTag;
    /** Health shown in Minecraft is capped; bosses with more OSRS hitpoints take proportionally less damage. */
    public static final int MAX_MC_HEALTH = 1024;
    /** Awakened DT2 boss or Doom delve level (null: the ordinary fight). Saved with the entity. */
    private BossVariant variant;

    public DataBossEntity(EntityType<? extends Monster> type, Level world) {
        super(type, world);
        BossDef def = def();
        this.bossBar = new ServerBossEvent(Component.literal(def == null ? "Boss" : def.name), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
        this.script = BossScripts.create(def == null ? "" : def.id);
        setPersistenceRequired();
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
            AttributeInstance inst = getAttribute(Attributes.MAX_HEALTH);
            if (inst != null) {
                inst.setBaseValue(shown);
            }
            setHealth(shown);
        }
        bossBar.setName(Component.literal(v.name()));
        bossBar.setColor(v.awakened() ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.RED);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        if (variant != null) {
            nbt.putString("RcVariant", variant.kind());
            nbt.putInt("RcVariantLevel", variant.level());
        }
        CompoundTag scriptData = new CompoundTag();
        script.writeNbt(this, scriptData);
        if (!scriptData.isEmpty()) {
            nbt.put("RcScript", scriptData);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        BossDef def = def();
        if (def != null && nbt.contains("RcVariant")) {
            String kind = nbt.getString("RcVariant");
            if (BossVariant.AWAKENED.equals(kind) && AwakenedBosses.isDt2(def.id)) {
                applyVariant(AwakenedBosses.variant(def), false);
            } else if (BossVariant.DELVE.equals(kind) && DoomDelves.BOSS.equals(def.id)) {
                applyVariant(DoomDelves.variant(def, nbt.getInt("RcVariantLevel")), false);
            }
        }
        if (nbt.contains("RcScript")) {
            script.readNbt(this, nbt.getCompound("RcScript"));
        }
    }

    @Override
    protected boolean shouldAggro(LivingEntity entity) {
        return entity instanceof ServerPlayer p && !p.isCreative() && !p.isSpectator();
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossBar.setProgress(getHealth() / getMaxHealth());
        script.tick(this);
        if (tickCount % ActionManager.MC_TICKS_PER_GAME_TICK == 0) {
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
                        for (ServerPlayer pl : ((ServerLevel) level()).getPlayers(x -> x.distanceToSqr(this) < 48 * 48)) {
                            pl.displayClientMessage(Component.literal(def.name + ": " + p.message()).withStyle(ChatFormatting.RED), false);
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
    public AttackType chooseStyle(ServerPlayer target, double distanceSq) {
        AttackDef custom = script.chooseAttack(this, target, Math.sqrt(distanceSq));
        if (custom != null) {
            nextAttack = custom;
            return custom.style();
        }
        return super.chooseStyle(target, distanceSq);
    }

    @Override
    public void performAttack(ServerPlayer target, AttackType type) {
        if (nextAttack != null && attackList() != null) {
            AttackDef chosen = script.beforeAttack(this, target, nextAttack);
            if (chosen != null) {
                nextAttack = chosen;
            }
        }
        super.performAttack(target, type);
    }

    @Override
    public int modifyIncoming(ServerPlayer attacker, int damage, AttackType type) {
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
    public int onHitPlayer(ServerPlayer player, int damage, AttackType type) {
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
        ServerLevel world = (ServerLevel) level();
        for (int i = 0; i < count; i++) {
            var e = type.create(world);
            if (e instanceof Mob mob) {
                double angle = random.nextDouble() * Math.PI * 2;
                mob.moveTo(getX() + Math.cos(angle) * 3, getY(), getZ() + Math.sin(angle) * 3, 0, 0);
                mob.finalizeSpawn(world, world.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                if (getTarget() != null) {
                    mob.setTarget(getTarget());
                }
                world.addFreshEntity(mob);
            }
        }
    }

    @Override
    public void onKilledBy(ServerPlayer player) {
        BossDef def = def();
        if (def == null) {
            return;
        }
        for (Drops.Rolled r : Drops.roll(variant != null ? variant.drops() : def.drops, getRandom(), false)) {
            Inv.spawnAtLocation(this, Drops.withDoses(com.runecraft.combat.CombatHooks.dropStack(player, this, r.item(), r.count()), r.doses()));
            CollectionLog.obtain(player, r.item());
        }
        int petRate = variant != null && variant.petRate() >= 0 ? variant.petRate()
                : com.runecraft.osrs.content.extras.Pets.engineRate(def.id);
        if (def.pet != null && petRate > 0 && getRandom().nextInt(petRate) == 0) {
            Inv.give(player, def.pet, 1);
            CollectionLog.obtain(player, def.pet);
            player.getServer().getPlayerList().broadcastSystemMessage(Component.literal(player.getName().getString()
                    + " has a funny feeling like they're being followed.").withStyle(ChatFormatting.GOLD), false);
        }
        PlayerData data = Players.data(player);
        String kcKey = variant != null ? variant.kcKey() : def.id;
        int kc = data.incrementKillCount(kcKey);
        Msg.game(player, "Your " + (variant != null && variant.awakened() ? variant.name() : def.name) + " kill count is: " + kc + ".");
        script.onDeath(this, player);
        DataBossService.killed(player, this);
        bossBar.removeAllPlayers();
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        bossBar.removeAllPlayers();
        if (!level().isClientSide() && reason != RemovalReason.UNLOADED_TO_CHUNK) {
            script.onRemoved(this); // gone for good (not a chunk unload that saves the boss to come back)
        }
    }
}
