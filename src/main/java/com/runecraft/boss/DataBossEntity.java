package com.runecraft.boss;

import com.runecraft.action.ActionManager;
import com.runecraft.collection.CollectionLog;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.bosses.AwakenedBosses;
import com.runecraft.osrs.content.bosses.BossLair;
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
    /**
     * Where a boss summoned at a stone first stood, and how far it may stray from there (null for bosses spawned by other
     * code: raid rooms, quests, the Dagannoth Kings' lair). Saved with the entity.
     */
    private net.minecraft.core.BlockPos home;
    private int leash;
    /** Game ticks in a row with no player around (a summoned boss resets after {@link BossLair#RESET_TICKS}). */
    private int lonelyTicks;
    /** A summoned boss with nobody near it for this long (game ticks: one minute) resets, like an OSRS instance left. */
    public static final int RESET_TICKS = BossLair.RESET_TICKS;
    /** Range within which a player keeps a summoned boss's fight going. */
    private static final double PRESENCE_RANGE = BossLair.PRESENCE_RANGE;

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

    /**
     * Marks a boss summoned at a stone: it stays within {@code leash} blocks of {@code at} (OSRS bosses never leave their
     * rooms; one lured further walks back) and resets (despawns) when nobody has been near it for a minute.
     */
    public void markSummoned(net.minecraft.core.BlockPos at, int leash) {
        this.home = at.immutable();
        this.leash = Math.max(8, leash);
    }

    /** A boss's next form or companion (Zulrah's forms, Dawn, Eldric) shares its lair: the same leash and reset. */
    public void copyHomeFrom(DataBossEntity other) {
        if (other != null && other.home != null) {
            markSummoned(other.home, other.leash);
        }
    }

    /**
     * Tags a minion summoned in this boss's lair (saved with the minion), so a reset clears it even when the script that
     * spawned it is gone (a reload gives the boss a fresh script). Nothing for a boss with no lair.
     */
    public void claimMinion(net.minecraft.world.entity.Entity minion) {
        if (home != null && minion != null) {
            minion.addTag(BossLair.minionTag(home.asLong()));
        }
    }

    /** Discards every minion still alive that was summoned in the lair at {@code home} (a boss's reset). */
    public static void clearLair(ServerLevel world, net.minecraft.core.BlockPos home) {
        String tag = BossLair.minionTag(home.asLong());
        for (Mob m : world.getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(home).inflate(64, 32, 64),
                e -> e.getTags().contains(tag))) {
            m.discard();
        }
    }

    /** Whether an attack from this player reaches the boss: false from outside its lair (summoned bosses only). */
    public boolean inLairReach(LivingEntity attacker) {
        return home == null || BossLair.inReach(attacker.distanceToSqr(home.getX() + 0.5, home.getY(), home.getZ() + 0.5), leash);
    }

    /** One game tick of a summoned boss's leash and reset; false when it reset (it is gone). */
    private boolean tickHome() {
        if (home == null) {
            return true;
        }
        LivingEntity target = getTarget();
        boolean someone = target instanceof ServerPlayer t && t.isAlive() && t.distanceToSqr(this) < PRESENCE_RANGE * PRESENCE_RANGE;
        if (!someone) {
            for (ServerPlayer p : ((ServerLevel) level()).players()) {
                if (p.isAlive() && !p.isSpectator() && !p.isCreative() && p.distanceToSqr(this) < PRESENCE_RANGE * PRESENCE_RANGE) {
                    someone = true;
                    break;
                }
            }
        }
        lonelyTicks = BossLair.lonely(lonelyTicks, someone);
        if (BossLair.resets(lonelyTicks)) {
            clearLair((ServerLevel) level(), home); // minions it summoned go with it
            discard(); // the script's onRemoved clears what else it placed
            return false;
        }
        double dx = getX() - (home.getX() + 0.5);
        double dz = getZ() - (home.getZ() + 0.5);
        double targetDistSq = target == null ? 0 : target.distanceToSqr(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
        // put back only once its fight has left the lair: a target still in it is chased (no free hits from past the leash)
        if (BossLair.returnsHome(dx * dx + dz * dz, leash, target != null, targetDistSq)) {
            getNavigation().stop();
            teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
            if (target != null) {
                setTarget(null); // its target ran off: it waits in its lair
            }
        }
        return true;
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
        if (home != null) {
            nbt.putLong("RcHome", home.asLong());
            nbt.putInt("RcLeash", leash);
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
        if (nbt.contains("RcHome")) {
            markSummoned(net.minecraft.core.BlockPos.of(nbt.getLong("RcHome")), nbt.getInt("RcLeash"));
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

    /**
     * Updates the boss bar from the script's {@link BossScript#bossBarProgress}. It runs with the AI, so whatever drives a
     * boss without AI (raid rooms) calls it.
     */
    public void refreshBossBar() {
        bossBar.setProgress(Math.max(0f, Math.min(1f, script.bossBarProgress(this))));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        refreshBossBar();
        script.tick(this);
        if (tickCount % ActionManager.MC_TICKS_PER_GAME_TICK == 0) {
            if (!tickHome()) {
                return;
            }
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
        if (!inLairReach(attacker)) {
            BossDef def = def();
            Msg.action(attacker, "You can't reach " + (def == null ? "the boss" : def.name) + " from out here.");
            return 0;
        }
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
                claimMinion(mob);
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
        for (Drops.Rolled r : DataBossService.rollLoot(player, this, variant != null ? variant.drops() : def.drops)) {
            Inv.spawnAtLocation(this, Drops.withDoses(com.runecraft.combat.CombatHooks.dropStack(player, this, r.item(), r.count()), r.doses()));
            CollectionLog.obtain(player, r.item());
        }
        if (variant == null && DataBossService.isCompanion(def.id)) {
            // part of the main boss's kill (Eldric, Dawn, Nex's generals): no kill count or pet of its own
            script.onDeath(this, player);
            DataBossService.killed(player, this);
            bossBar.removeAllPlayers();
            return;
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
