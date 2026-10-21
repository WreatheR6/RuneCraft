package com.runecraft.combat;

import com.runecraft.RuneCraft;
import com.runecraft.action.ActionManager;
import com.runecraft.collection.CollectionLog;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.item.OsrsEquipmentItems;
import com.runecraft.network.Net;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.CombatFormulas;
import com.runecraft.osrs.CombatStyle;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestManager;
import com.runecraft.skill.Drops;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import com.runecraft.platform.ServerLivingEntityEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Replaces Minecraft combat with OSRS combat inside Gielinor: accuracy rolls, max hits, attack
 * speeds, equipment bonuses, prayers and per-damage experience. Outside Gielinor vanilla damage
 * is kept but still awards OSRS combat experience.
 */
public final class CombatHandler {
    public static final ResourceKey<DamageType> OSRS_HIT = ResourceKey.create(Registries.DAMAGE_TYPE, RuneCraft.id("osrs_hit"));
    /** An OSRS hit with no attacker (poison, venom, typeless area damage); in #minecraft:no_knockback, so it never pushes. */
    public static final ResourceKey<DamageType> OSRS_TYPELESS = ResourceKey.create(Registries.DAMAGE_TYPE, RuneCraft.id("osrs_typeless"));

    private static boolean applying;
    private static final Map<LivingEntity, Long> NPC_LAST_ATTACK = new WeakHashMap<>();

    private CombatHandler() {
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(CombatHandler::allowDamage);
        com.runecraft.platform.AttackEntityCallback.EVENT.register(CombatHandler::attackEntity);
        ServerLivingEntityEvents.AFTER_DEATH.register(CombatHandler::afterDeath);
        CombatHooks.addModifier(new GearEffects());
        PrayerEffectsService.register();
        SkullService.register();
        SetEffectHooks.register();
        DefenceEffects.register();
    }

    private static boolean allowDamage(LivingEntity target, DamageSource source, float amount) {
        if (applying) {
            return true;
        }
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        boolean gielinor = Gielinor.is(target);

        if (attacker instanceof ServerPlayer player && target != player) {
            if (!gielinor) {
                overworldXp(player, target, amount, direct);
                return true;
            }
            if (target instanceof ServerPlayer other && !canAttackPlayer(player, other)) {
                if (direct == player || direct instanceof AbstractArrow) {
                    Msg.game(player, "You can't attack that player."); // playability: say why the hit did nothing
                }
                return false;
            }
            if ((direct instanceof AbstractArrow || direct == player) && !singleWayAllows(player, target)) {
                if (direct instanceof AbstractArrow arrow) {
                    arrow.discard();
                }
                return false;
            }
            if (direct instanceof AbstractArrow arrow) {
                playerRanged(player, target, arrow);
                return false;
            }
            if (direct == player) {
                playerMelee(player, target);
                return false;
            }
            // Any other vanilla damage a player causes (thrown potions, fire, tridents...) still obeys the monster-damage
            // gates (task-only dungeons).
            return target instanceof ServerPlayer || CombatHooks.deniedDamage(player, target) == null;
        }
        if (target instanceof ServerPlayer player && attacker instanceof LivingEntity mob && !(attacker instanceof Player)) {
            if (gielinor || mob instanceof OsrsMonsterEntity) {
                if (!CombatZones.npcMayAttack(mob, player)) {
                    if (direct instanceof Projectile projectile) {
                        projectile.discard();
                    }
                    return false;
                }
                MonsterStats stats = MonsterStats.of(mob);
                AttackType type = direct instanceof Projectile ? AttackType.RANGED
                        : stats.style().isMelee() ? stats.style() : AttackType.CRUSH;
                if (direct instanceof Projectile projectile) {
                    projectile.discard();
                }
                npcAttack(mob, player, type);
                return false;
            }
        }
        return true;
    }

    // ---------------- Players attacking ----------------

    /**
     * A melee click (vanilla hurt with the player as the direct source: a fallback behind {@link #attackEntity}, and the
     * self-tests' hits). {@code // playability}: goes through the attack clock, so a click before the weapon is ready is
     * buffered and resolves on the exact ready tick instead of being dropped; a ready click still resolves right here.
     */
    private static void playerMelee(ServerPlayer player, LivingEntity target) {
        AttackTimer.click(player, target, com.runecraft.osrs.AttackClock.Kind.MELEE);
    }

    /**
     * Left-click on a Gielinor monster (NeoForge's AttackEntityEvent, cancelled by any non-PASS result).
     * {@code // playability}: SUCCESS cancels vanilla's Player.attack before its whoosh, sweep, crit, durability loss and
     * knockback (OSRS hits never push), and the click goes on the attack clock as melee, the autocast spell (a staff with
     * a spell set, a powered staff) or a bow shot at the clicked monster. Players and anything outside Gielinor: PASS
     * (the PvP minigames' own callback and the vanilla path behind allowDamage stay as they are).
     */
    private static net.minecraft.world.InteractionResult attackEntity(Player player, net.minecraft.world.level.Level world,
            net.minecraft.world.InteractionHand hand, Entity entity, net.minecraft.world.phys.EntityHitResult hit) {
        if (!(entity instanceof LivingEntity target) || entity instanceof Player
                || entity instanceof net.minecraft.world.entity.decoration.ArmorStand
                || entity instanceof com.runecraft.entity.QuestNpcEntity || player.isSpectator()
                || !Gielinor.is(player) || !Gielinor.is(entity)) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        if (world.isClientSide()) {
            return net.minecraft.world.InteractionResult.SUCCESS; // no local crit particles or sweep for an OSRS hit
        }
        if (!(player instanceof ServerPlayer attacker) || target.isInvulnerableTo(source(attacker))) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        if (singleWayAllows(attacker, target)) {
            AttackTimer.click(attacker, target, AttackTimer.kindFor(attacker));
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    /** One melee attack that the attack clock allows now: today's OSRS roll, stamped on the clock first. */
    static void resolveMelee(ServerPlayer player, LivingEntity target) {
        if (target == null) {
            return;
        }
        PlayerData data = Players.data(player);
        SkillSet skills = data.skills();
        int speed = Equipment.weaponSpeed(player, skills);
        int period = speed * ActionManager.MC_TICKS_PER_GAME_TICK;
        String denied = CombatHooks.deniedAttack(player, Equipment.meleeType(player, skills));
        if (denied != null) {
            AttackTimer.accept(player, com.runecraft.osrs.AttackClock.Kind.MELEE, period);
            Msg.game(player, denied);
            return;
        }
        AttackTimer.accept(player, com.runecraft.osrs.AttackClock.Kind.MELEE, period);
        ItemStack held = player.getMainHandItem();
        com.runecraft.osrs.ItemDef heldDef = com.runecraft.registry.ModItems.def(held);
        if (heldDef != null && heldDef.equipment() != null) {
            String unmet = heldDef.equipment().unmetRequirement(skills);
            if (unmet != null) {
                Msg.game(player, "You need " + unmet + " to wield this.");
            }
        }
        ActionManager.cancel(player);
        CombatStyle style = data.style().forMelee();
        AttackType type = Equipment.meleeType(player, skills);
        // Levels, prayers, gear totals and every gear effect (salve, slayer helmet, void, obsidian, inquisitor's,
        // demonbane, dragonbane, keris, fang, Dharok's...) come from the DPS-calculator-tested PlayerHitCalc.
        com.runecraft.osrs.PlayerHitCalc.Input in = HitCalcBridge.input(player, target, type, null, 0);
        long attackRoll = HitCalcBridge.attackRoll(player, target, in);
        long defenceRoll = HitCalcBridge.defenceRoll(target, in);
        int maxHit = HitCalcBridge.maxHit(player, target, in);
        int damage = HitCalcBridge.roll(new java.util.Random(player.getRandom().nextLong()), in, attackRoll, defenceRoll, maxHit);
        damage = HitCalcBridge.afterRoll(in, damage); // vampyre tiers (immunity, vampyrebane, Efaritay's aid)
        damage = CombatHooks.afterRoll(player, target, type, damage, maxHit);
        if (target instanceof OsrsMonsterEntity monster) {
            damage = monster.modifyIncoming(player, damage, type);
        }
        damage = protectionAgainstPlayer(target, type, damage);
        int dealt = applyDamage(player, target, damage);
        CombatHooks.afterHit(player, target, type, dealt);
        awardXp(player, style, dealt, false);
        meleeFeedback(target, dealt, maxHit);
    }

    /**
     * {@code // playability}: a sound for every resolved swing so a real-time player knows it landed (OSRS: the hitsplat
     * alone). A hit plays the strong-attack sound (the crit sound from 90% of the max hit); a miss the weak one and a puff.
     */
    private static void meleeFeedback(LivingEntity target, int dealt, int maxHit) {
        if (!(target.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }
        double chest = target.getY() + target.getBbHeight() * 0.6;
        if (dealt > 0) {
            boolean big = maxHit > 0 && dealt * 10 >= maxHit * 9;
            level.playSound(null, target.getX(), chest, target.getZ(), big ? net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_CRIT
                    : net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_STRONG, net.minecraft.sounds.SoundSource.PLAYERS, 0.9f, 1.0f);
        } else {
            level.playSound(null, target.getX(), chest, target.getZ(), net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_WEAK,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.9f, 1.0f);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, target.getX(), chest, target.getZ(), 4, 0.2, 0.2, 0.2, 0.01);
        }
    }

    private static void playerRanged(ServerPlayer player, LivingEntity target, AbstractArrow arrow) {
        PlayerData data = Players.data(player);
        SkillSet skills = data.skills();
        int rangedStrength = 0;
        String arrowItem = null;
        for (String tag : arrow.getTags()) {
            if (tag.startsWith(OsrsEquipmentItems.RANGED_STRENGTH_TAG)) {
                rangedStrength = parse(tag.substring(OsrsEquipmentItems.RANGED_STRENGTH_TAG.length()));
            } else if (tag.startsWith("runecraft_arrow_")) {
                arrowItem = tag.substring("runecraft_arrow_".length());
            }
        }
        arrow.discard();
        String denied = CombatHooks.deniedAttack(player, AttackType.RANGED);
        if (denied != null) {
            Msg.game(player, denied);
            return;
        }
        CombatStyle style = data.style().forRanged();
        com.runecraft.osrs.PlayerHitCalc.Input in = HitCalcBridge.input(player, target, AttackType.RANGED, arrowItem, rangedStrength);
        long attackRoll = HitCalcBridge.attackRoll(player, target, in);
        long defenceRoll = HitCalcBridge.defenceRoll(target, in);
        int maxHit = Math.max(1, HitCalcBridge.maxHit(player, target, in));
        java.util.Random rng = new java.util.Random(player.getRandom().nextLong());
        boolean accurate = rng.nextDouble() < HitCalcBridge.hitChance(in, attackRoll, defenceRoll); // specials can always hit
        int minHit = Math.min(HitCalcBridge.minHit(in), maxHit); // dark bow special: 5 / 8
        int damage = accurate ? minHit + rng.nextInt(maxHit - minHit + 1) : 0;
        // Vampyrebane multipliers, then enchanted (e) bolt effects (opal to onyx, dragon variants), then the vampyre
        // immunity / tier 2 limits: the calculator's order, so a ruby or dragonstone proc can't hurt an immune vyrewatch.
        damage = HitCalcBridge.vampyreScale(in, damage); // silver bolts, blisterwood stakes
        damage = com.runecraft.content.systems.BoltProcs.apply(player, target, arrowItem, accurate, damage, maxHit);
        damage = HitCalcBridge.vampyreLimit(in, damage);
        damage = CombatHooks.afterRoll(player, target, AttackType.RANGED, damage, maxHit);
        if (target instanceof OsrsMonsterEntity monster) {
            damage = monster.modifyIncoming(player, damage, AttackType.RANGED);
        }
        damage = protectionAgainstPlayer(target, AttackType.RANGED, damage);
        int dealt = applyDamage(player, target, damage);
        CombatHooks.afterHit(player, target, AttackType.RANGED, dealt);
        awardXp(player, style, dealt, false);
        if (arrowItem != null && player.getRandom().nextFloat() < 0.8f) {
            target.spawnAtLocation(Inv.stack(arrowItem, 1));
        }
    }

    private static int parse(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Defence roll of any target against an attack style (players: NpcHitCalc through HitCalcBridge). */
    public static long defenceRoll(LivingEntity target, AttackType type) {
        if (target instanceof ServerPlayer defender) {
            return HitCalcBridge.playerDefenceRoll(defender, type);
        }
        MonsterStats stats = MonsterStats.of(target);
        int level = type == AttackType.MAGIC ? stats.magic() : stats.defence();
        return CombatFormulas.npcRoll(level, stats.defenceBonus(type));
    }

    /**
     * PvP rules of a PvP minigame both players are in (Last Man Standing, Castle Wars, the Fight Pit): true or false decides,
     * null falls back to the Wilderness rule. Set by the minigames module.
     */
    public static java.util.function.BiFunction<ServerPlayer, ServerPlayer, Boolean> pvpOverride = (attacker, target) -> null;

    /**
     * PvP is only allowed in the Wilderness, between players whose combat levels differ by no more than the
     * Wilderness level.
     */
    public static boolean canAttackPlayer(ServerPlayer attacker, ServerPlayer target) {
        Boolean override = pvpOverride.apply(attacker, target);
        if (override != null) {
            return override;
        }
        if (Gielinor.inSafeZone(attacker) || Gielinor.inSafeZone(target)) {
            return false; // the Ferox Enclave: no player fights inside its walls
        }
        return com.runecraft.osrs.CombatBonuses.canAttackInWilderness(Players.data(attacker).skills().combatLevel(),
                Players.data(target).skills().combatLevel(), Gielinor.wildernessLevel(attacker), Gielinor.wildernessLevel(target));
    }

    /**
     * Single-way combat check for a player starting an attack; tells the player why not ("Someone else is fighting
     * that." / "I'm already under attack.").
     */
    public static boolean singleWayAllows(ServerPlayer player, LivingEntity target) {
        com.runecraft.osrs.SingleCombat.Verdict v = CombatZones.check(player, target);
        if (v != com.runecraft.osrs.SingleCombat.Verdict.OK) {
            Msg.game(player, v.message);
            return false;
        }
        return true;
    }

    /** True if the player has the overhead protection prayer for this attack style active. */
    public static boolean isProtected(PlayerData data, AttackType type) {
        return com.runecraft.osrs.NpcHitCalc.protectedFrom(data.activePrayers(), type);
    }

    /** Protection prayers only block 40% of damage dealt by other players. */
    public static int protectionAgainstPlayer(LivingEntity target, AttackType type, int damage) {
        if (damage > 0 && target instanceof ServerPlayer defender && isProtected(Players.data(defender), type)) {
            return com.runecraft.osrs.CombatBonuses.protectedFromPlayer(damage);
        }
        return damage;
    }

    // ---------------- Monsters attacking ----------------

    /** Performs one OSRS attack from a monster against a player (respecting attack speed). */
    public static void npcAttack(LivingEntity mob, ServerPlayer player, AttackType type) {
        npcAttack(mob, player, type, false);
    }

    /**
     * Performs one OSRS attack from a monster against a player. {@code ownCadence}: the caller already keeps the attack speed
     * (OsrsAttackGoal stamps it when the attack starts, Jad's telegraph, Graardor's slam on everyone in the room), so the hit,
     * which now lands after a wind-up, flight or telegraph (// playability: OSRS rolls a melee hit on the swing tick), is never
     * dropped by the per-monster cooldown below. Vanilla-AI monsters hitting through {@code allowDamage} pass false.
     */
    public static void npcAttack(LivingEntity mob, ServerPlayer player, AttackType type, boolean ownCadence) {
        MonsterStats stats = MonsterStats.of(mob);
        if (!CombatZones.npcMayAttack(mob, player)) {
            return;
        }
        long now = ActionManager.mcTick();
        Long last = NPC_LAST_ATTACK.get(mob);
        if (!ownCadence && last != null && now - last < (long) stats.attackSpeed() * ActionManager.MC_TICKS_PER_GAME_TICK) {
            return;
        }
        NPC_LAST_ATTACK.put(mob, now);
        PlayerData data = Players.data(player);
        int attackLevel = switch (type) {
            case MAGIC -> stats.magic();
            case RANGED -> stats.ranged();
            default -> stats.attack();
        };
        long attackRoll = CombatHooks.versusAttackRoll(mob, player, type, npcAttackRoll(mob, stats, type, attackLevel));
        long defenceRoll = defenceRoll(player, type);
        int maxHit = CombatHooks.versusMaxHit(mob, player, type, mob instanceof OsrsMonsterEntity m ? m.maxHitAgainst(player, type) : stats.maxHit());
        int damage = CombatFormulas.roll(new java.util.Random(mob.getRandom().nextLong()), attackRoll, defenceRoll, maxHit);
        if (isProtected(data, type)) {
            damage = mob instanceof OsrsMonsterEntity m ? m.damageThroughPrayer(damage) : 0;
        }
        if (mob instanceof OsrsMonsterEntity m) {
            damage = m.onHitPlayer(player, damage, type);
        }
        CombatHooks.nameAttack(type.name().toLowerCase(java.util.Locale.ROOT), type);
        int dealtToPlayer = applyDamage(mob, player, damage);
        if (dealtToPlayer > 0) {
            CombatHooks.hurt(player, mob, dealtToPlayer);
        }
    }

    /**
     * A monster's attack roll: its attack/ranged/magic level (after curse-spell drains such as Confuse and Stun)
     * and its OSRS attack bonus for the style.
     */
    public static long npcAttackRoll(LivingEntity mob, MonsterStats stats, AttackType type, int level) {
        int drained = com.runecraft.content.systems.SysCombat.drainedAttackLevel(mob, type, level);
        long roll = HitCalcBridge.npcAttackRoll(drained, stats.attackBonus(type), type);
        double m = CombatHooks.npcAccuracy(mob, type);
        return m == 1.0 ? roll : (long) (roll * m);
    }

    /** Combat hook passthroughs for MagicService and module code. */
    public static double hookAccuracy(ServerPlayer p, LivingEntity t, AttackType type) {
        return CombatHooks.accuracy(p, t, type);
    }

    public static double hookMaxHit(ServerPlayer p, LivingEntity t, AttackType type) {
        return CombatHooks.maxHit(p, t, type);
    }

    public static int hookAfterRoll(ServerPlayer p, LivingEntity t, AttackType type, int damage) {
        return CombatHooks.afterRoll(p, t, type, damage);
    }

    /** {@link #hookAfterRoll} for a roll whose max hit is known (targets that always take the max hit get it). */
    public static int hookAfterRoll(ServerPlayer p, LivingEntity t, AttackType type, int damage, int maxHit) {
        return CombatHooks.afterRoll(p, t, type, damage, maxHit);
    }

    public static void hookAfterHit(ServerPlayer p, LivingEntity t, AttackType type, int dealt) {
        CombatHooks.afterHit(p, t, type, dealt);
    }

    public static void clearCooldown(LivingEntity mob) {
        NPC_LAST_ATTACK.remove(mob);
    }

    /** True while {@link #applyDamage} is hurting its target (OsrsKnockback cancels the vanilla knockback of that hurt). */
    public static boolean applyingOsrsHit() {
        return applying;
    }

    // ---------------- Damage + XP ----------------

    public static DamageSource source(LivingEntity attacker) {
        return source(attacker, attacker);
    }

    private static DamageSource source(LivingEntity attacker, LivingEntity worldHolder) {
        Holder<DamageType> type = worldHolder.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(attacker == null ? OSRS_TYPELESS : OSRS_HIT);
        return attacker == null ? new DamageSource(type) : new DamageSource(type, attacker);
    }

    /** Applies OSRS damage with a hitsplat; returns the damage actually dealt (capped at remaining HP). */
    public static int applyDamage(LivingEntity attacker, LivingEntity target, int damage) {
        if (attacker != null && !(target instanceof ServerPlayer) && target.isInvulnerable()) {
            // Scenery creatures (GotR's abyssal creatures, Sorceress's Garden elementals) can't be hurt by any path (melee,
            // ranged, magic, cannon, specials), so they never give combat XP.
            ServerNetworking.hitsplat(target, 0, Net.SPLAT_MISS);
            return 0;
        }
        if (attacker instanceof ServerPlayer gated && !(target instanceof ServerPlayer)) {
            String denied = CombatHooks.deniedDamage(gated, target);
            if (denied != null) {
                if (damage > 0) {
                    Msg.game(gated, denied);
                }
                ServerNetworking.hitsplat(target, 0, Net.SPLAT_MISS);
                return 0;
            }
        }
        if (target instanceof Mob mob && attacker != null && mob.getTarget() == null) {
            mob.setTarget(attacker);
        }
        if (attacker != null) {
            target.setLastHurtByMob(attacker);
            CombatZones.onHit(attacker, target);
            if (target instanceof ServerPlayer hurt && attacker != target
                    && !(attacker instanceof com.runecraft.entity.QuestNpcEntity)) {
                AutoRetaliate.onHurt(hurt, attacker);
            }
            if (attacker instanceof ServerPlayer hitter) {
                AutoRetaliate.onPlayerAttack(hitter, target);
                if (target instanceof ServerPlayer victim) {
                    SkullService.onPlayerAttack(hitter, victim);
                }
            }
        }
        if (target instanceof ServerPlayer damaged) {
            if (attacker != null && attacker != damaged && damage > 0) {
                damage = DefenceEffects.reduce(damaged, attacker, CombatHooks.namedStyle(), CombatHooks.typelessNamed(), damage);
                damage = CombatHooks.incoming(damaged, attacker, damage);
            }
            CombatHooks.damaged(damaged, attacker, Math.max(0, damage));
        } else {
            CombatHooks.clearAttackName();
        }
        if (damage <= 0) {
            ServerNetworking.hitsplat(target, 0, Net.SPLAT_MISS);
            return 0;
        }
        int dealt = Math.min(damage, (int) Math.ceil(target.getHealth()));
        applying = true;
        try {
            target.invulnerableTime = 0;
            target.hurt(source(attacker, target), damage);
        } finally {
            applying = false;
        }
        if (attacker != null) {
            ServerNetworking.hitsplat(target, damage, Net.SPLAT_DAMAGE);
        }
        return dealt;
    }

    /** OSRS combat XP: 4 per damage to the trained skill (1.33 each when controlled) plus 1.33 Hitpoints. */
    public static void awardXp(ServerPlayer player, CombatStyle style, int damage, boolean magic) {
        if (damage <= 0) {
            return;
        }
        switch (style) {
            case ACCURATE -> XpService.addTenths(player, Skill.ATTACK, CombatFormulas.meleeXpTenths(damage));
            case AGGRESSIVE -> XpService.addTenths(player, Skill.STRENGTH, CombatFormulas.meleeXpTenths(damage));
            case DEFENSIVE -> XpService.addTenths(player, Skill.DEFENCE, CombatFormulas.meleeXpTenths(damage));
            case CONTROLLED -> {
                XpService.addTenths(player, Skill.ATTACK, CombatFormulas.controlledXpTenths(damage));
                XpService.addTenths(player, Skill.STRENGTH, CombatFormulas.controlledXpTenths(damage));
                XpService.addTenths(player, Skill.DEFENCE, CombatFormulas.controlledXpTenths(damage));
            }
            case RANGED_ACCURATE, RAPID -> XpService.addTenths(player, Skill.RANGED, CombatFormulas.meleeXpTenths(damage));
            case LONGRANGE -> {
                XpService.addTenths(player, Skill.RANGED, damage * 20);
                XpService.addTenths(player, Skill.DEFENCE, damage * 20);
            }
        }
        XpService.addTenths(player, Skill.HITPOINTS, CombatFormulas.hitpointsXpTenths(damage));
    }

    private static void overworldXp(ServerPlayer player, LivingEntity target, float amount, Entity direct) {
        int damage = (int) Math.ceil(Math.min(amount, target.getHealth()));
        if (damage <= 0) {
            return;
        }
        PlayerData data = Players.data(player);
        CombatStyle style = direct instanceof AbstractArrow ? data.style().forRanged() : data.style().forMelee();
        if (direct == player || direct instanceof AbstractArrow) {
            awardXp(player, style, damage, false);
        }
    }

    // ---------------- Deaths ----------------

    /** Set by {@link PoisonService} around a poison or venom tick: who a monster killed by it is credited to. */
    static ServerPlayer typelessKillCredit;

    private static void afterDeath(LivingEntity entity, DamageSource source) {
        Entity attacker = source.getEntity() != null ? source.getEntity() : typelessKillCredit;
        if (entity instanceof ServerPlayer) {
            return;
        }
        boolean osrsWorld = Gielinor.is(entity) || entity instanceof OsrsMonsterEntity;
        if (!osrsWorld || !(attacker instanceof ServerPlayer player)) {
            return;
        }
        MonsterStats stats = MonsterStats.of(entity);
        if (entity instanceof OsrsMonsterEntity custom) {
            custom.onKilledBy(player);
        }
        java.util.List<com.runecraft.osrs.data.Drop> table = stats.monster() != null ? stats.monster().drops()
                : stats.profile() instanceof com.runecraft.osrs.content.MonsterDef md ? md.drops : null;
        if (table != null) {
            for (Drops.Rolled r : Drops.roll(table, entity.getRandom(), false)) {
                Inv.spawnAtLocation(entity, Drops.withDoses(CombatHooks.dropStack(player, entity, r.item(), r.count()), r.doses()));
                CollectionLog.obtain(player, r.item());
            }
        }
        Slayer.onKill(player, entity, stats);
        com.runecraft.content.GameEvents.fireKill(player, entity, stats);
        QuestManager.onKill(player, entity, stats);
    }
}
