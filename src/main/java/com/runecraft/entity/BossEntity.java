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
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

/** A boss with its OSRS signature mechanics, a boss bar, kill counts and collection log drops. */
public class BossEntity extends OsrsMonsterEntity {
    private final ServerBossBar bossBar;
    private int attackCounter;
    private AttackType pendingStyle;
    private ServerPlayerEntity pendingTarget;
    private long pendingAt;

    public BossEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        Boss boss = boss();
        this.bossBar = new ServerBossBar(Text.literal(boss == null ? "Boss" : boss.displayName()),
                BossBar.Color.RED, BossBar.Style.NOTCHED_10);
        setPersistent();
    }

    public Boss boss() {
        return ModEntities.boss(getType());
    }

    @Override
    protected boolean shouldAggro(net.minecraft.entity.LivingEntity entity) {
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
        if (pendingStyle != null && ActionManager.mcTick() >= pendingAt) {
            if (pendingTarget != null && pendingTarget.isAlive()) {
                CombatHandler.npcAttack(this, pendingTarget, pendingStyle);
            }
            pendingStyle = null;
            pendingTarget = null;
        }
    }

    @Override
    public AttackType chooseStyle(ServerPlayerEntity target, double distanceSq) {
        Boss boss = boss();
        boolean adjacent = distanceSq < Math.pow(getWidth() * 1.4 + 1.2, 2);
        return switch (boss.mechanic()) {
            case MIXED_MAGIC -> adjacent && random.nextBoolean() ? AttackType.CRUSH : AttackType.MAGIC;
            case GRAARDOR -> adjacent && attackCounter % 3 != 2 ? AttackType.CRUSH : AttackType.RANGED;
            // OSRS Wiki: 2/3 melee, 1/3 magic, and the magic attack is only used in melee range.
            case KRIL -> adjacent && random.nextInt(3) == 0 ? AttackType.MAGIC : AttackType.SLASH;
            case JAD -> boss.meleeWhenAdjacent() && adjacent && random.nextInt(3) == 0 ? AttackType.CRUSH
                    : random.nextBoolean() ? AttackType.MAGIC : AttackType.RANGED;
            default -> boss.style();
        };
    }

    @Override
    public void performAttack(ServerPlayerEntity target, AttackType type) {
        Boss boss = boss();
        attackCounter++;
        ServerWorld world = (ServerWorld) getWorld();
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
                    world.spawnParticles(ParticleTypes.FLAME, getX(), getY() + getHeight(), getZ(), 40, 0.6, 0.6, 0.6, 0.05);
                    world.playSound(null, getBlockPos(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 2f, 0.5f);
                    Msg.action(target, "TzTok-Jad rears up!");
                } else {
                    world.spawnParticles(ParticleTypes.LAVA, getX(), getY() + 0.2, getZ(), 30, 1.0, 0.1, 1.0, 0.1);
                    world.playSound(null, getBlockPos(), SoundEvents.ENTITY_RAVAGER_STUNNED, SoundCategory.HOSTILE, 2f, 0.5f);
                    Msg.action(target, "TzTok-Jad stomps the ground!");
                }
                return;
            }
            case GRAARDOR -> {
                if (type == AttackType.RANGED) {
                    world.spawnParticles(ParticleTypes.EXPLOSION, getX(), getY() + 1, getZ(), 3, 1, 0.5, 1, 0);
                    for (ServerPlayerEntity p : world.getPlayers(pl -> pl.squaredDistanceTo(this) < 12 * 12 && pl.isAlive())) {
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
                    world.getServer().getPlayerManager().getPlayerList().stream().filter(p -> p.squaredDistanceTo(this) < 32 * 32)
                            .forEach(p -> p.sendMessage(Text.literal("K'ril Tsutsaroth: YARRRRRRR!").formatted(Formatting.RED), false));
                    com.runecraft.content.systems.CombatAchievementService.countNear(this,
                            com.runecraft.osrs.content.systems.CombatAchievements.COUNT_KRIL_YARR);
                    PlayerData data = Players.data(target);
                    boolean spectral = "spectral_spirit_shield".equals(Inv.idOf(target.getOffHandStack()));
                    data.setPrayerPoints(data.prayerPoints() - data.prayerPoints() / (spectral ? 4 : 2));
                    int dmg = 35 + random.nextInt(15);
                    swingHand(net.minecraft.util.Hand.MAIN_HAND);
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
    public int maxHitAgainst(ServerPlayerEntity player, AttackType type) {
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
    public int onHitPlayer(ServerPlayerEntity player, int damage, AttackType type) {
        Boss boss = boss();
        if (damage <= 0) {
            return damage;
        }
        switch (boss.mechanic()) {
            case GUTHAN -> {
                if (random.nextInt(4) == 0) {
                    heal(damage);
                    ((ServerWorld) getWorld()).spawnParticles(ParticleTypes.HEART, getX(), getY() + getHeight(), getZ(), 3, 0.3, 0.3, 0.3, 0);
                }
            }
            case TORAG -> {
                if (random.nextInt(4) == 0) {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1));
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
    public int modifyIncoming(ServerPlayerEntity attacker, int damage, AttackType type) {
        Boss boss = boss();
        if (boss.mechanic() == Boss.Mechanic.VAMPIRE && damage >= getHealth()
                && !(Inv.has(attacker, "stake", 1) && Inv.has(attacker, "hammer", 1))) {
            Msg.game(attacker, "The vampyre seems to regenerate! You need a stake and hammer to finish him.");
            return Math.max(0, (int) getHealth() - 1);
        }
        if (boss.mechanic() == Boss.Mechanic.DEMON) {
            boolean silverlight = "silverlight".equals(Inv.idOf(attacker.getMainHandStack()));
            if (!silverlight && damage >= getHealth()) {
                Msg.game(attacker, "Delrith is weakened, but only Silverlight can banish him!");
                return Math.max(0, (int) getHealth() - 1);
            }
        }
        return damage;
    }

    @Override
    public void onKilledBy(ServerPlayerEntity player) {
        Boss boss = boss();
        // TzHaar Fight Pit monsters (its TzTok-Jad included) drop nothing and count no kill (OSRS).
        if (com.runecraft.content.minigames.MinigameManager.owner(this)
                instanceof com.runecraft.content.minigames.games.FightPitsSession) {
            bossBar.clearPlayers();
            return;
        }
        if (boss.mechanic() == Boss.Mechanic.VAMPIRE) {
            Inv.remove(player, "stake", 1);
        }
        // As in OSRS the Barrows brothers drop nothing: their tables are rolled by the Barrows chest.
        if (boss.lair() != com.runecraft.osrs.Landmark.BARROWS) {
            for (Drops.Rolled r : Drops.roll(boss.drops(), getRandom(), false)) {
                dropStack(Drops.withDoses(Inv.stack(r.item(), r.count()), r.doses()));
                CollectionLog.obtain(player, r.item());
            }
        }
        PlayerData data = Players.data(player);
        int kc = data.incrementKillCount(boss.entityId());
        Msg.game(player, "Your " + boss.displayName() + " kill count is: " + kc + ".");
        bossBar.clearPlayers();
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        bossBar.clearPlayers();
    }
}
