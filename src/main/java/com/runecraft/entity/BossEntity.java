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

        /**
         * The boss bar's status line and colour (round 43, {@code // playability}): {@code status} is the default one
         * ({@link BossEntity#defaultBossBarStatus}); a script that counts something down adds to it with {@code status.with(...)}.
         */
        default com.runecraft.osrs.BossBarStatus.Status bossBarStatus(BossEntity boss, com.runecraft.osrs.BossBarStatus.Status status) {
            return status;
        }
    }

    private static final java.util.Map<Boss, java.util.function.Supplier<Script>> SCRIPTS = new java.util.EnumMap<>(Boss.class);
    /** Verac's max hit when his hit goes through Protect from Melee (OSRS Wiki: 15 instead of 23). */
    public static final int VERAC_THROUGH_PRAYER_MAX = 15;

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
    /** The style rolled for the next attack, kept until the attack is made or the target's reach changes (see baseStyle). */
    private AttackType rolledStyle;
    private boolean rolledAdjacent;
    /** The script's after-attack hook of a melee attack being delivered, run once its swing resolves (takeMeleeAfter). */
    private Runnable meleeAfter;

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

    /** Tags a minion summoned in this boss's lair so its reset clears it (as {@link com.runecraft.boss.DataBossEntity#claimMinion}). */
    public void claimMinion(net.minecraft.world.entity.Entity minion) {
        if (home != null && minion != null) {
            minion.addTag(com.runecraft.osrs.content.bosses.BossLair.minionTag(home.asLong()));
        }
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
        double presence = com.runecraft.osrs.content.bosses.BossLair.PRESENCE_RANGE;
        boolean someone = getTarget() instanceof ServerPlayer t && t.isAlive() && t.distanceToSqr(this) < presence * presence;
        if (!someone) {
            for (ServerPlayer p : ((ServerLevel) level()).players()) {
                if (p.isAlive() && !p.isSpectator() && !p.isCreative() && p.distanceToSqr(this) < presence * presence) {
                    someone = true;
                    break;
                }
            }
        }
        lonelyTicks = com.runecraft.osrs.content.bosses.BossLair.lonely(lonelyTicks, someone);
        if (com.runecraft.osrs.content.bosses.BossLair.resets(lonelyTicks)) {
            com.runecraft.boss.DataBossEntity.clearLair((ServerLevel) level(), home); // minions it summoned go with it
            discard();
            return false;
        }
        net.minecraft.world.entity.LivingEntity target = getTarget();
        double targetDistSq = target == null ? 0 : target.distanceToSqr(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
        // put back only once its fight has left the lair: a target still in it is chased (no free hits from past the leash)
        if (com.runecraft.osrs.content.bosses.BossLair.returnsHome(distanceToSqr(home.getX() + 0.5, home.getY(), home.getZ() + 0.5),
                LEASH, target != null, targetDistSq)) {
            getNavigation().stop();
            teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
            if (target != null) {
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
        refreshBossBar();
        if (script() != null && tickCount % ActionManager.MC_TICKS_PER_GAME_TICK == 0 && isAlive()) {
            script.gameTick(this);
        }
        landTelegraph();
    }

    /**
     * The boss bar: health, then the status line and colour as a data boss's ({@code DataBossEntity.refreshBossBar}, round 43,
     * {@code // playability}): the colour of the style the boss attacks with (Graardor and K'ril red, Jad white, Bryophyta blue,
     * each Barrows brother his own), the curse on its target's protection prayers, and whatever its module script adds. The
     * name and colour are only sent when they change.
     */
    public void refreshBossBar() {
        bossBar.setProgress(Math.max(0f, Math.min(1f, getHealth() / getMaxHealth())));
        Boss boss = boss();
        if (boss == null) {
            return;
        }
        com.runecraft.osrs.BossBarStatus.Status status = defaultBossBarStatus();
        if (script() != null) {
            status = script.bossBarStatus(this, status);
        }
        if (status == null) {
            return;
        }
        bossBar.setName(Component.literal(com.runecraft.osrs.BossBarStatus.title(boss.displayName(), status)));
        if (status.colour() != null) {
            bossBar.setColor(BossEvent.BossBarColor.valueOf(status.colour().name()));
        }
    }

    /**
     * The default boss bar status of a built-in boss: no phases, the curse on its target's protection prayers, and the colour of
     * its style roll ({@link com.runecraft.osrs.BossBarStatus#attacksOf}); while Jad's telegraph is on its way, that attack's
     * colour (blue as he rears up, green as he stomps).
     */
    public com.runecraft.osrs.BossBarStatus.Status defaultBossBarStatus() {
        Boss boss = boss();
        int[] curse = getTarget() instanceof ServerPlayer target ? com.runecraft.network.HudNet.curseMap(target) : null;
        java.util.List<com.runecraft.osrs.BossBarStatus.Attack> attacks = pendingStyle != null
                ? java.util.List.of(new com.runecraft.osrs.BossBarStatus.Attack(pendingStyle, 1))
                : com.runecraft.osrs.BossBarStatus.attacksOf(boss);
        return com.runecraft.osrs.BossBarStatus.of(-1, 0, attacks, curse);
    }

    /** The boss bar's title and colour as players see them (self-tests). */
    public String bossBarTitle() {
        return bossBar.getName().getString();
    }

    public BossEvent.BossBarColor bossBarColor() {
        return bossBar.getColor();
    }

    /**
     * Self-test hook ({@code com.runecraft.selftest}): lands a due telegraphed hit (Jad's) now, as the boss's AI step does every
     * Minecraft tick, without ticking its goals.
     */
    public void tickTelegraphForSelfTest() {
        if (isAlive()) {
            landTelegraph();
        }
    }

    private void landTelegraph() {
        if (pendingStyle != null && ActionManager.mcTick() >= pendingAt) {
            // playability: the telegraphed hit lands only on a target still there (alive, same level, in range + 4), rolled
            // through npcAttack without its per-monster cooldown: the goal stamped Jad's cadence at launch, so his claw 8 ticks
            // after a magic attack is no longer dropped by the double cooldown.
            ServerPlayer t = pendingTarget;
            double range = rangedReach > 0 ? rangedReach : 10.0;
            if (t != null && t.isAlive() && !t.isRemoved() && t.level() == level()
                    && com.runecraft.osrs.ThreatTiming.inRange(Math.max(0, distanceTo(t) - getBbWidth() / 2 - t.getBbWidth() / 2), range)) {
                CombatHandler.npcAttack(this, t, pendingStyle, true);
            } else if (t != null && t.isAlive() && t.level() == level()) {
                ServerNetworking.hitsplat(t, 0, com.runecraft.network.Net.SPLAT_MISS);
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

    /**
     * Whether a target at this squared (centre) distance is in melee reach: the bounding-box gap rule (// playability: was
     * {@code width * 1.4 + 1.2} from the centre, which let a big boss hit from well outside its body), for a player-sized target.
     */
    public boolean adjacent(double distanceSq) {
        return Math.sqrt(distanceSq) - getBbWidth() / 2 - 0.3 <= com.runecraft.osrs.ThreatTiming.MELEE_REACH;
    }

    /**
     * The boss's own style choice. It is rolled once and kept until the attack is made, or until the target moves in or out of
     * melee reach (the choice depends on it): the goal asks every tick, and a re-roll every tick made a mixed-style boss stutter
     * between walking up and standing to cast (// playability; the odds per attack are unchanged).
     */
    private AttackType baseStyle(ServerPlayer target, double distanceSq) {
        boolean adjacent = adjacent(distanceSq);
        if (rolledStyle == null || rolledAdjacent != adjacent) {
            rolledStyle = rollStyle(adjacent);
            rolledAdjacent = adjacent;
        }
        return rolledStyle;
    }

    private AttackType rollStyle(boolean adjacent) {
        Boss boss = boss();
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
        rolledStyle = null; // the next attack's style is rolled afresh
        if (script() != null && script.performAttack(this, target, type)) {
            return;
        }
        // A melee swing now lands after its wind-up: the script's after-attack hook (Obor's knockback slam, Bryophyta's
        // growthlings) runs once it has resolved, so the slam pushes the player after the hit instead of out of its reach.
        meleeAfter = script != null && type.isMelee() ? () -> script.afterAttack(this, target, type) : null;
        bossAttack(target, type);
        Runnable after = meleeAfter;
        meleeAfter = null;
        if (script != null && (after != null || !type.isMelee())) {
            script.afterAttack(this, target, type); // not delivered later (a telegraph, a slam, a ranged or magic attack)
        }
    }

    @Override
    protected Runnable takeMeleeAfter() {
        Runnable after = meleeAfter;
        meleeAfter = null;
        return after;
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
                    com.runecraft.boss.ThreatCues.announce(this, target, "TzTok-Jad rears up!", AttackType.MAGIC, (int) (pendingAt - ActionManager.mcTick()));
                } else {
                    world.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.2, getZ(), 30, 1.0, 0.1, 1.0, 0.1);
                    world.playSound(null, blockPosition(), SoundEvents.RAVAGER_STUNNED, SoundSource.HOSTILE, 2f, 0.5f);
                    com.runecraft.boss.ThreatCues.announce(this, target, "TzTok-Jad stomps the ground!", AttackType.RANGED, (int) (pendingAt - ActionManager.mcTick()));
                }
                return;
            }
            case GRAARDOR -> {
                if (type == AttackType.RANGED) {
                    world.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 1, getZ(), 3, 1, 0.5, 1, 0);
                    // everyone in the room: in the God Wars Dungeon his chamber only, not past its walls
                    var room = com.runecraft.osrs.content.godwars.GwdDungeon.areaAt(getBlockX(), getBlockY(), getBlockZ());
                    for (ServerPlayer p : world.getPlayers(pl -> pl.distanceToSqr(this) < 12 * 12 && pl.isAlive()
                            && com.runecraft.osrs.content.godwars.GwdDungeon.inRoom(room, pl.getBlockX(), pl.getBlockY(), pl.getBlockZ()))) {
                        CombatHandler.npcAttack(this, p, AttackType.RANGED, true); // one slam, everyone in the room
                    }
                    return;
                }
            }
            case KRIL -> {
                // Protect from Melee read through the protection chokepoint (its switch grace), as the hit it replaces would be
                if (type.isMelee() && CombatHandler.isProtected(Players.data(target), type) && random.nextInt(9) == 0) {
                    // "YARRRRRRR!" prayer smash (OSRS Wiki): 1 in 9 melee hits against Protect from Melee; always 35-49
                    // through prayer, and drains half the target's prayer points (a quarter with a spectral spirit shield).
                    world.getServer().getPlayerList().getPlayers().stream().filter(p -> p.distanceToSqr(this) < 32 * 32)
                            .forEach(p -> p.displayClientMessage(Component.literal("K'ril Tsutsaroth: YARRRRRRR!").withStyle(ChatFormatting.RED), false));
                    com.runecraft.content.systems.CombatAchievementService.countNear(this,
                            com.runecraft.osrs.content.systems.CombatAchievements.COUNT_KRIL_YARR);
                    swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    // playability: lands after the swing's wind-up, on a target still in reach (OSRS: on the swing tick)
                    com.runecraft.boss.Mechanics.deliverMelee(this, target, com.runecraft.osrs.ThreatTiming.MELEE_REACH, () -> {
                        PlayerData data = Players.data(target);
                        boolean spectral = "spectral_spirit_shield".equals(Inv.idOf(target.getOffhandItem()));
                        data.setPrayerPoints(data.prayerPoints() - data.prayerPoints() / (spectral ? 4 : 2));
                        int dmg = 35 + random.nextInt(15);
                        CombatHandler.applyDamage(this, target, dmg);
                        ServerNetworking.sync(target);
                    }, takeMeleeAfter());
                    return;
                }
            }
            default -> {
            }
        }
        super.performAttack(target, type);
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
            // Verac's Defiler: a quarter of his hits go through Protect from Melee, at a max hit of 15 instead of 23 (OSRS Wiki)
            return damage * VERAC_THROUGH_PRAYER_MAX / Math.max(1, boss.maxHit());
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
                // Blighted Aura (OSRS Wiki): a 20% chance on a successful hit to lower Strength by 5.
                if (random.nextInt(5) == 0) {
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
        if (home != null && !com.runecraft.osrs.content.bosses.BossLair.inReach(
                attacker.distanceToSqr(home.getX() + 0.5, home.getY(), home.getZ() + 0.5), LEASH)) {
            Msg.action(attacker, "You can't reach " + boss().displayName() + " from out here.");
            return 0;
        }
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
        // OSRS keeps no kill count for quest bosses (wiki, Boss: "Excluding minigame and quest bosses, each boss has its own
        // hiscore table"): Count Draynor's and Delrith's kills are their quests' (QuestManager.onKill).
        if (!boss.questBoss()) {
            PlayerData data = Players.data(player);
            int kc = data.incrementKillCount(boss.entityId());
            Msg.game(player, "Your " + boss.displayName() + " kill count is: " + kc + ".");
        }
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
