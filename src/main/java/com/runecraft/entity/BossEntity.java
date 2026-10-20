package com.runecraft.entity;

import com.runecraft.action.ActionManager;
import com.runecraft.collection.CollectionLog;
import com.runecraft.combat.CombatHandler;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.data.Boss;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModEntities;
import com.runecraft.skill.Drops;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** A boss with its OSRS signature mechanics, a boss bar, kill counts and collection log drops. */
public class BossEntity extends OsrsMonsterEntity {
    /**
     * Behaviour a content module adds to one of these bosses (Bryophyta's growthlings, Obor's ranged attack and knockback:
     * {@code content/bosses/OldBossScripts}). One instance per boss entity; every method has a neutral default.
     */
    public interface Script {
        /** Every game tick while the boss is alive. */
        default void gameTick(BossEntity boss) {
        }

        /** The style for the next attack ({@code chosen} is the boss's own choice). */
        default AttackType chooseStyle(BossEntity boss, ServerPlayer target, double distanceSq, AttackType chosen) {
            return chosen;
        }

        /** Called before each attack; true when the script performed it itself. */
        default boolean performAttack(BossEntity boss, ServerPlayer target, AttackType type) {
            return false;
        }

        /** Called after the boss's normal attack has been made. */
        default void afterAttack(BossEntity boss, ServerPlayer target, AttackType type) {
        }

        default int maxHit(BossEntity boss, ServerPlayer player, AttackType type, int max) {
            return max;
        }

        /** Damage of an attack of {@code type} that the matching protection prayer would otherwise block entirely. */
        default int throughPrayer(BossEntity boss, AttackType type, int damage) {
            return 0;
        }

        default int modifyIncoming(BossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            return damage;
        }

        /** When the boss dies or is removed. */
        default void onRemoved(BossEntity boss) {
        }
    }

    private static final java.util.Map<Boss, java.util.function.Supplier<Script>> SCRIPTS = new java.util.EnumMap<>(Boss.class);

    /** Gives every boss of this kind a script (called once from a module's hooks). */
    public static void registerScript(Boss boss, java.util.function.Supplier<Script> factory) {
        SCRIPTS.put(boss, factory);
    }

    private final ServerBossEvent bossBar;
    private int attackCounter;
    private AttackType pendingStyle;
    private ServerPlayer pendingTarget;
    private long pendingAt;
    private Script script;
    private boolean scriptMade;
    private AttackType attackStyle;

    /** This boss's module script, or null. */
    public Script script() {
        if (!scriptMade) {
            scriptMade = true;
            Boss boss = boss();
            java.util.function.Supplier<Script> factory = boss == null ? null : SCRIPTS.get(boss);
            script = factory == null ? null : factory.get();
        }
        return script;
    }

    public BossEntity(EntityType<? extends Monster> type, Level world) {
        super(type, world);
        Boss boss = boss();
        this.bossBar = new ServerBossEvent(Component.literal(boss == null ? "Boss" : boss.displayName()),
                BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
        setPersistenceRequired();
    }

    public Boss boss() {
        return ModEntities.boss(getType());
    }

    @Override
    protected boolean shouldAggro(net.minecraft.world.entity.LivingEntity entity) {
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

    /** Where the boss was summoned (null for one placed by other code); it stays near there and resets when left alone. */
    private net.minecraft.core.BlockPos home;
    private int lonelyTicks;
    /** Blocks a summoned boss may stray from where it appeared before it walks back (its lair is a small pad). */
    private static final int LEASH = 16;

    /** Marks a boss summoned at its stone (BossService): leashed to {@code at}, reset after a minute with nobody near. */
    public void markSummoned(net.minecraft.core.BlockPos at) {
        this.home = at.immutable();
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        if (home != null) {
            nbt.putLong("RcHome", home.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("RcHome")) {
            home = net.minecraft.core.BlockPos.of(nbt.getLong("RcHome"));
        }
    }

    /** A game tick of the leash and reset (as DataBossEntity's); false when the boss reset. */
    private boolean tickHome() {
        if (home == null) {
            return true;
        }
        boolean someone = getTarget() instanceof ServerPlayer t && t.isAlive() && t.distanceToSqr(this) < 32 * 32;
        if (!someone) {
            for (ServerPlayer p : ((ServerLevel) level()).players()) {
                if (p.isAlive() && !p.isSpectator() && !p.isCreative() && p.distanceToSqr(this) < 32 * 32) {
                    someone = true;
                    break;
                }
            }
        }
        lonelyTicks = someone ? 0 : lonelyTicks + 1;
        if (lonelyTicks >= com.runecraft.boss.DataBossEntity.RESET_TICKS) {
            discard();
            return false;
        }
        if (distanceToSqr(home.getX() + 0.5, home.getY(), home.getZ() + 0.5) > LEASH * LEASH) {
            getNavigation().stop();
            teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
            if (getTarget() != null && getTarget().distanceToSqr(home.getX() + 0.5, home.getY(), home.getZ() + 0.5) > LEASH * LEASH) {
                setTarget(null);
            }
        }
        return true;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % ActionManager.MC_TICKS_PER_GAME_TICK == 0 && !tickHome()) {
            return;
        }
        bossBar.setProgress(getHealth() / getMaxHealth());
        if (script() != null && tickCount % ActionManager.MC_TICKS_PER_GAME_TICK == 0 && isAlive()) {
            script.gameTick(this);
        }
        if (pendingStyle != null && ActionManager.mcTick() >= pendingAt) {
            if (pendingTarget != null && pendingTarget.isAlive()) {
                CombatHandler.npcAttack(this, pendingTarget, pendingStyle);
            }
            pendingStyle = null;
            pendingTarget = null;
        }
    }

    @Override
    public AttackType chooseStyle(ServerPlayer target, double distanceSq) {
        AttackType chosen = baseStyle(target, distanceSq);
        return script() != null ? script.chooseStyle(this, target, distanceSq, chosen) : chosen;
    }

    /** Graardor's next attack is his ranged slam (rolled 1/3 per attack, kept until the attack is made). */
    private Boolean slamNext;

    private boolean nextIsSlam() {
        if (slamNext == null) {
            slamNext = random.nextInt(3) == 0;
        }
        return slamNext;
    }

    /** Whether a target at this squared distance is in melee reach. */
    public boolean adjacent(double distanceSq) {
        return distanceSq < Math.pow(getBbWidth() * 1.4 + 1.2, 2);
    }

    private AttackType baseStyle(ServerPlayer target, double distanceSq) {
        Boss boss = boss();
        boolean adjacent = adjacent(distanceSq);
        return switch (boss.mechanic()) {
            case MIXED_MAGIC -> adjacent && random.nextBoolean() ? AttackType.CRUSH : AttackType.MAGIC;
            // OSRS Wiki: 2/3 melee, 1/3 the ranged slam at everyone in the room, which he only uses within melee range of
            // his target (out of reach he walks up to it). The style is rolled once per attack (the goal asks every tick).
            case GRAARDOR -> adjacent && nextIsSlam() ? AttackType.RANGED : AttackType.CRUSH;
            // OSRS Wiki: 2/3 melee, 1/3 magic, and the magic attack is only used in melee range.
            case KRIL -> adjacent && random.nextInt(3) == 0 ? AttackType.MAGIC : AttackType.SLASH;
            case JAD -> boss.meleeWhenAdjacent() && adjacent && random.nextInt(3) == 0 ? AttackType.CRUSH
                    : random.nextBoolean() ? AttackType.MAGIC : AttackType.RANGED;
            default -> boss.style();
        };
    }

    @Override
    public void performAttack(ServerPlayer target, AttackType type) {
        attackStyle = type;
        if (script() != null && script.performAttack(this, target, type)) {
            return;
        }
        bossAttack(target, type);
        if (script != null) {
            script.afterAttack(this, target, type);
        }
    }

    private void bossAttack(ServerPlayer target, AttackType type) {
        Boss boss = boss();
        attackCounter++;
        slamNext = null; // Graardor rolls his next style afresh
        ServerLevel world = (ServerLevel) level();
        switch (boss.mechanic()) {
            case JAD -> {
                if (type.isMelee()) {
                    break; // the claw swipe is instant: only the magic and ranged attacks are telegraphed
                }
                // Jad telegraphs: rearing up = magic, stomping = ranged. You have 3 game ticks to switch prayers.
                pendingStyle = type;
                pendingTarget = target;
                pendingAt = ActionManager.mcTick() + 3L * ActionManager.MC_TICKS_PER_GAME_TICK;
                if (type == AttackType.MAGIC) {
                    world.sendParticles(ParticleTypes.FLAME, getX(), getY() + getBbHeight(), getZ(), 40, 0.6, 0.6, 0.6, 0.05);
                    world.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2f, 0.5f);
                    Msg.action(target, "TzTok-Jad rears up!");
                } else {
                    world.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.2, getZ(), 30, 1.0, 0.1, 1.0, 0.1);
                    world.playSound(null, blockPosition(), SoundEvents.RAVAGER_STUNNED, SoundSource.HOSTILE, 2f, 0.5f);
                    Msg.action(target, "TzTok-Jad stomps the ground!");
                }
                return;
            }
            case GRAARDOR -> {
                if (type == AttackType.RANGED) {
                    world.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 1, getZ(), 3, 1, 0.5, 1, 0);
                    for (ServerPlayer p : world.getPlayers(pl -> pl.distanceToSqr(this) < 12 * 12 && pl.isAlive())) {
                        CombatHandler.npcAttack(this, p, AttackType.RANGED);
                        resetCooldownHack();
                    }
                    return;
                }
            }
            case KRIL -> {
                if (type.isMelee() && Players.data(target).isActive(com.runecraft.osrs.Prayer.PROTECT_FROM_MELEE) && random.nextInt(9) == 0) {
                    // "YARRRRRRR!" prayer smash (OSRS Wiki): 1 in 9 melee hits against Protect from Melee; always 35-49
                    // through prayer, and drains half the target's prayer points (a quarter with a spectral spirit shield).
                    world.getServer().getPlayerList().getPlayers().stream().filter(p -> p.distanceToSqr(this) < 32 * 32)
                            .forEach(p -> p.displayClientMessage(Component.literal("K'ril Tsutsaroth: YARRRRRRR!").withStyle(ChatFormatting.RED), false));
                    com.runecraft.content.systems.CombatAchievementService.countNear(this,
                            com.runecraft.osrs.content.systems.CombatAchievements.COUNT_KRIL_YARR);
                    PlayerData data = Players.data(target);
                    boolean spectral = "spectral_spirit_shield".equals(Inv.idOf(target.getOffhandItem()));
                    data.setPrayerPoints(data.prayerPoints() - data.prayerPoints() / (spectral ? 4 : 2));
                    int dmg = 35 + random.nextInt(15);
                    swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    CombatHandler.applyDamage(this, target, dmg);
                    ServerNetworking.sync(target);
                    return;
                }
            }
            default -> {
            }
        }
        super.performAttack(target, type);
    }

    private void resetCooldownHack() {
        // AoE hits several players with one swing; npcAttack's per-mob cooldown would block the rest.
        CombatHandlerAccess.clearCooldown(this);
    }

    @Override
    public int maxHitAgainst(ServerPlayer player, AttackType type) {
        int max = baseMaxHit(type);
        return script() != null ? script.maxHit(this, player, type, max) : max;
    }

    private int baseMaxHit(AttackType type) {
        Boss boss = boss();
        return switch (boss.mechanic()) {
            case DHAROK -> {
                // Dharok's set effect: max hit grows as his hitpoints drop.
                float lost = getMaxHealth() - getHealth();
                yield (int) Math.floor(boss.maxHit() * (1.0 + lost / 100.0 * getMaxHealth() / 100.0));
            }
            case GRAARDOR -> type == AttackType.RANGED ? 35 : boss.maxHit();
            case KRIL -> type == AttackType.MAGIC ? 30 : boss.maxHit();
            case MIXED_MAGIC -> type == AttackType.MAGIC ? 16 : boss.maxHit();
            default -> boss.maxHit();
        };
    }

    @Override
    public int damageThroughPrayer(int damage) {
        if (script() != null && attackStyle != null) {
            return script.throughPrayer(this, attackStyle, damage);
        }
        Boss boss = boss();
        if (boss.mechanic() == Boss.Mechanic.VERAC && random.nextInt(4) == 0) {
            return damage; // Verac's hits can pierce prayer
        }
        if (boss.mechanic() == Boss.Mechanic.GRAARDOR || boss.mechanic() == Boss.Mechanic.KRIL) {
            return 0;
        }
        return 0;
    }

    @Override
    public int onHitPlayer(ServerPlayer player, int damage, AttackType type) {
        Boss boss = boss();
        if (damage <= 0) {
            return damage;
        }
        switch (boss.mechanic()) {
            // OSRS Wiki (Mod Ash): Graardor's slam rolls 15-35 evenly when it hits; K'ril's magic 10-30, and his melee poisons
            // (16; approx: a 1/4 chance per damaging hit).
            case GRAARDOR -> {
                if (type == AttackType.RANGED) {
                    return 15 + random.nextInt(21);
                }
            }
            case KRIL -> {
                if (type == AttackType.MAGIC) {
                    return 10 + random.nextInt(21);
                }
                if (type.isMelee() && random.nextInt(4) == 0) {
                    com.runecraft.combat.PoisonService.poison(player, 16, ActionManager.gameTick());
                }
            }
            case GUTHAN -> {
                if (random.nextInt(4) == 0) {
                    heal(damage);
                    ((ServerLevel) level()).sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight(), getZ(), 3, 0.3, 0.3, 0.3, 0);
                }
            }
            case TORAG -> {
                if (random.nextInt(4) == 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
                    Msg.action(player, "You feel drained of energy.");
                }
            }
            case AHRIM -> {
                if (random.nextInt(4) == 0) {
                    PlayerData data = Players.data(player);
                    data.skills().setBoost(Skill.STRENGTH, data.skills().boost(Skill.STRENGTH) - 5);
                    ServerNetworking.sync(player);
                }
            }
            case KARIL -> {
                if (random.nextInt(4) == 0) {
                    PlayerData data = Players.data(player);
                    int drain = (int) Math.ceil(data.skills().currentLevel(Skill.AGILITY) * 0.2);
                    data.skills().setBoost(Skill.AGILITY, data.skills().boost(Skill.AGILITY) - drain);
                    ServerNetworking.sync(player);
                }
            }
            default -> {
            }
        }
        return damage;
    }

    @Override
    public int modifyIncoming(ServerPlayer attacker, int damage, AttackType type) {
        if (script() != null) {
            damage = script.modifyIncoming(this, attacker, damage, type);
        }
        Boss boss = boss();
        if (boss.mechanic() == Boss.Mechanic.VAMPIRE && damage >= getHealth()
                && !(Inv.has(attacker, "stake", 1) && Inv.has(attacker, "hammer", 1))) {
            Msg.game(attacker, "The vampyre seems to regenerate! You need a stake and hammer to finish him.");
            return Math.max(0, (int) getHealth() - 1);
        }
        if (boss.mechanic() == Boss.Mechanic.DEMON) {
            boolean silverlight = "silverlight".equals(Inv.idOf(attacker.getMainHandItem()));
            if (!silverlight && damage >= getHealth()) {
                Msg.game(attacker, "Delrith is weakened, but only Silverlight can banish him!");
                return Math.max(0, (int) getHealth() - 1);
            }
        }
        return damage;
    }

    @Override
    public void onKilledBy(ServerPlayer player) {
        Boss boss = boss();
        // TzHaar Fight Pit monsters (its TzTok-Jad included) drop nothing and count no kill (OSRS).
        if (com.runecraft.content.minigames.MinigameManager.owner(this)
                instanceof com.runecraft.content.minigames.games.FightPitsSession) {
            bossBar.removeAllPlayers();
            return;
        }
        if (boss.mechanic() == Boss.Mechanic.VAMPIRE) {
            Inv.remove(player, "stake", 1);
        }
        // As in OSRS the Barrows brothers drop nothing: their tables are rolled by the Barrows chest.
        if (boss.lair() != com.runecraft.osrs.Landmark.BARROWS) {
            for (Drops.Rolled r : Drops.roll(boss.drops(), getRandom(), false)) {
                Inv.spawnAtLocation(this, Drops.withDoses(Inv.stack(r.item(), r.count()), r.doses()));
                CollectionLog.obtain(player, r.item());
            }
        }
        PlayerData data = Players.data(player);
        int kc = data.incrementKillCount(boss.entityId());
        Msg.game(player, "Your " + boss.displayName() + " kill count is: " + kc + ".");
        bossBar.removeAllPlayers();
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        bossBar.removeAllPlayers();
        if (script != null) {
            script.onRemoved(this);
        }
    }
}
