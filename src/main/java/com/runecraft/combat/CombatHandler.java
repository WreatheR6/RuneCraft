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
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Replaces Minecraft combat with OSRS combat inside Gielinor: accuracy rolls, max hits, attack
 * speeds, equipment bonuses, prayers and per-damage experience. Outside Gielinor vanilla damage
 * is kept but still awards OSRS combat experience.
 */
public final class CombatHandler {
    public static final RegistryKey<DamageType> OSRS_HIT = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, RuneCraft.id("osrs_hit"));

    private static boolean applying;
    private static final Map<LivingEntity, Long> NPC_LAST_ATTACK = new WeakHashMap<>();

    private CombatHandler() {
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(CombatHandler::allowDamage);
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
        Entity attacker = source.getAttacker();
        Entity direct = source.getSource();
        boolean gielinor = Gielinor.is(target);

        if (attacker instanceof ServerPlayerEntity player && target != player) {
            if (!gielinor) {
                overworldXp(player, target, amount, direct);
                return true;
            }
            if (target instanceof ServerPlayerEntity other && !canAttackPlayer(player, other)) {
                return false;
            }
            if ((direct instanceof PersistentProjectileEntity || direct == player) && !singleWayAllows(player, target)) {
                if (direct instanceof PersistentProjectileEntity arrow) {
                    arrow.discard();
                }
                return false;
            }
            if (direct instanceof PersistentProjectileEntity arrow) {
                playerRanged(player, target, arrow);
                return false;
            }
            if (direct == player) {
                playerMelee(player, target);
                return false;
            }
            return true;
        }
        if (target instanceof ServerPlayerEntity player && attacker instanceof LivingEntity mob && !(attacker instanceof PlayerEntity)) {
            if (gielinor || mob instanceof OsrsMonsterEntity) {
                if (!CombatZones.npcMayAttack(mob, player)) {
                    if (direct instanceof ProjectileEntity projectile) {
                        projectile.discard();
                    }
                    return false;
                }
                MonsterStats stats = MonsterStats.of(mob);
                AttackType type = direct instanceof ProjectileEntity ? AttackType.RANGED
                        : stats.style().isMelee() ? stats.style() : AttackType.CRUSH;
                if (direct instanceof ProjectileEntity projectile) {
                    projectile.discard();
                }
                npcAttack(mob, player, type);
                return false;
            }
        }
        return true;
    }

    // ---------------- Players attacking ----------------

    private static void playerMelee(ServerPlayerEntity player, LivingEntity target) {
        PlayerData data = Players.data(player);
        SkillSet skills = data.skills();
        int speed = Equipment.weaponSpeed(player, skills);
        long now = ActionManager.mcTick();
        if (now - data.lastAttackTick < (long) speed * ActionManager.MC_TICKS_PER_GAME_TICK) {
            return;
        }
        String denied = CombatHooks.deniedAttack(player, Equipment.meleeType(player, skills));
        if (denied != null) {
            data.lastAttackTick = now;
            Msg.game(player, denied);
            return;
        }
        data.lastAttackTick = now;
        ItemStack held = player.getMainHandStack();
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
        damage = CombatHooks.afterRoll(player, target, type, damage);
        if (target instanceof OsrsMonsterEntity monster) {
            damage = monster.modifyIncoming(player, damage, type);
        }
        damage = protectionAgainstPlayer(target, type, damage);
        int dealt = applyDamage(player, target, damage);
        CombatHooks.afterHit(player, target, type, dealt);
        awardXp(player, style, dealt, false);
    }

    private static void playerRanged(ServerPlayerEntity player, LivingEntity target, PersistentProjectileEntity arrow) {
        PlayerData data = Players.data(player);
        SkillSet skills = data.skills();
        int rangedStrength = 0;
        String arrowItem = null;
        for (String tag : arrow.getCommandTags()) {
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
        boolean accurate = rng.nextDouble() < com.runecraft.osrs.Chance.hitChance(attackRoll, defenceRoll);
        int damage = accurate ? rng.nextInt(maxHit + 1) : 0;
        // Enchanted (e) bolt effects (opal to onyx, dragon variants).
        damage = HitCalcBridge.afterRoll(in, damage); // vampyre tiers (silver bolts, blisterwood stakes)
        damage = com.runecraft.content.systems.BoltProcs.apply(player, target, arrowItem, accurate, damage, maxHit);
        damage = CombatHooks.afterRoll(player, target, AttackType.RANGED, damage);
        if (target instanceof OsrsMonsterEntity monster) {
            damage = monster.modifyIncoming(player, damage, AttackType.RANGED);
        }
        damage = protectionAgainstPlayer(target, AttackType.RANGED, damage);
        int dealt = applyDamage(player, target, damage);
        CombatHooks.afterHit(player, target, AttackType.RANGED, dealt);
        awardXp(player, style, dealt, false);
        if (arrowItem != null && player.getRandom().nextFloat() < 0.8f) {
            target.dropStack(Inv.stack(arrowItem, 1));
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
        if (target instanceof ServerPlayerEntity defender) {
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
    public static java.util.function.BiFunction<ServerPlayerEntity, ServerPlayerEntity, Boolean> pvpOverride = (attacker, target) -> null;

    /**
     * PvP is only allowed in the Wilderness, between players whose combat levels differ by no more than the
     * Wilderness level.
     */
    public static boolean canAttackPlayer(ServerPlayerEntity attacker, ServerPlayerEntity target) {
        Boolean override = pvpOverride.apply(attacker, target);
        if (override != null) {
            return override;
        }
        return com.runecraft.osrs.CombatBonuses.canAttackInWilderness(Players.data(attacker).skills().combatLevel(),
                Players.data(target).skills().combatLevel(), Gielinor.wildernessLevel(attacker), Gielinor.wildernessLevel(target));
    }

    /**
     * Single-way combat check for a player starting an attack; tells the player why not ("Someone else is fighting
     * that." / "I'm already under attack.").
     */
    public static boolean singleWayAllows(ServerPlayerEntity player, LivingEntity target) {
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
        if (damage > 0 && target instanceof ServerPlayerEntity defender && isProtected(Players.data(defender), type)) {
            return com.runecraft.osrs.CombatBonuses.protectedFromPlayer(damage);
        }
        return damage;
    }

    // ---------------- Monsters attacking ----------------

    /** Performs one OSRS attack from a monster against a player (respecting attack speed). */
    public static void npcAttack(LivingEntity mob, ServerPlayerEntity player, AttackType type) {
        MonsterStats stats = MonsterStats.of(mob);
        if (!CombatZones.npcMayAttack(mob, player)) {
            return;
        }
        long now = ActionManager.mcTick();
        Long last = NPC_LAST_ATTACK.get(mob);
        if (last != null && now - last < (long) stats.attackSpeed() * ActionManager.MC_TICKS_PER_GAME_TICK) {
            return;
        }
        NPC_LAST_ATTACK.put(mob, now);
        PlayerData data = Players.data(player);
        int attackLevel = switch (type) {
            case MAGIC -> stats.magic();
            case RANGED -> stats.ranged();
            default -> stats.attack();
        };
        long attackRoll = npcAttackRoll(mob, stats, type, attackLevel);
        long defenceRoll = defenceRoll(player, type);
        int maxHit = mob instanceof OsrsMonsterEntity m ? m.maxHitAgainst(player, type) : stats.maxHit();
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
        return HitCalcBridge.npcAttackRoll(drained, stats.attackBonus(type), type);
    }

    /** Combat hook passthroughs for MagicService and module code. */
    public static double hookAccuracy(ServerPlayerEntity p, LivingEntity t, AttackType type) {
        return CombatHooks.accuracy(p, t, type);
    }

    public static double hookMaxHit(ServerPlayerEntity p, LivingEntity t, AttackType type) {
        return CombatHooks.maxHit(p, t, type);
    }

    public static int hookAfterRoll(ServerPlayerEntity p, LivingEntity t, AttackType type, int damage) {
        return CombatHooks.afterRoll(p, t, type, damage);
    }

    public static void hookAfterHit(ServerPlayerEntity p, LivingEntity t, AttackType type, int dealt) {
        CombatHooks.afterHit(p, t, type, dealt);
    }

    public static void clearCooldown(LivingEntity mob) {
        NPC_LAST_ATTACK.remove(mob);
    }

    // ---------------- Damage + XP ----------------

    public static DamageSource source(LivingEntity attacker) {
        return source(attacker, attacker);
    }

    private static DamageSource source(LivingEntity attacker, LivingEntity worldHolder) {
        RegistryEntry<DamageType> type = worldHolder.getWorld().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(OSRS_HIT);
        return attacker == null ? new DamageSource(type) : new DamageSource(type, attacker);
    }

    /** Applies OSRS damage with a hitsplat; returns the damage actually dealt (capped at remaining HP). */
    public static int applyDamage(LivingEntity attacker, LivingEntity target, int damage) {
        if (target instanceof MobEntity mob && attacker != null && mob.getTarget() == null) {
            mob.setTarget(attacker);
        }
        if (attacker != null) {
            target.setAttacker(attacker);
            CombatZones.onHit(attacker, target);
            if (target instanceof ServerPlayerEntity hurt && attacker != target
                    && !(attacker instanceof com.runecraft.entity.QuestNpcEntity)) {
                AutoRetaliate.onHurt(hurt, attacker);
            }
            if (attacker instanceof ServerPlayerEntity hitter) {
                AutoRetaliate.onPlayerAttack(hitter, target);
                if (target instanceof ServerPlayerEntity victim) {
                    SkullService.onPlayerAttack(hitter, victim);
                }
            }
        }
        if (target instanceof ServerPlayerEntity damaged) {
            if (attacker != null && attacker != damaged && damage > 0) {
                damage = DefenceEffects.reduce(damaged, attacker, CombatHooks.namedStyle(), damage);
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
            target.timeUntilRegen = 0;
            target.damage(source(attacker, target), damage);
        } finally {
            applying = false;
        }
        if (attacker != null) {
            ServerNetworking.hitsplat(target, damage, Net.SPLAT_DAMAGE);
        }
        return dealt;
    }

    /** OSRS combat XP: 4 per damage to the trained skill (1.33 each when controlled) plus 1.33 Hitpoints. */
    public static void awardXp(ServerPlayerEntity player, CombatStyle style, int damage, boolean magic) {
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

    private static void overworldXp(ServerPlayerEntity player, LivingEntity target, float amount, Entity direct) {
        int damage = (int) Math.ceil(Math.min(amount, target.getHealth()));
        if (damage <= 0) {
            return;
        }
        PlayerData data = Players.data(player);
        CombatStyle style = direct instanceof PersistentProjectileEntity ? data.style().forRanged() : data.style().forMelee();
        if (direct == player || direct instanceof PersistentProjectileEntity) {
            awardXp(player, style, damage, false);
        }
    }

    // ---------------- Deaths ----------------

    private static void afterDeath(LivingEntity entity, DamageSource source) {
        Entity attacker = source.getAttacker();
        if (entity instanceof ServerPlayerEntity) {
            return;
        }
        boolean osrsWorld = Gielinor.is(entity) || entity instanceof OsrsMonsterEntity;
        if (!osrsWorld || !(attacker instanceof ServerPlayerEntity player)) {
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
                entity.dropStack(Drops.withDoses(CombatHooks.dropStack(player, entity, r.item(), r.count()), r.doses()));
                CollectionLog.obtain(player, r.item());
            }
        }
        Slayer.onKill(player, entity, stats);
        com.runecraft.content.GameEvents.fireKill(player, entity, stats);
        QuestManager.onKill(player, entity, stats);
    }
}
