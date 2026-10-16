package com.runecraft.combat;

import com.runecraft.action.Action;
import com.runecraft.action.ActionManager;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.CombatBonuses;
import com.runecraft.osrs.CombatFormulas;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.Rune;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.osrs.Spell;
import com.runecraft.osrs.data.Bar;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModItems;
import com.runecraft.skill.Stun;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.Map;

/** The standard spellbook: combat autocasting, teleports, alchemy and superheat. */
public final class MagicService {
    private static final double SPELL_RANGE = 20.0;

    private MagicService() {
    }

    public static void setAutocast(ServerPlayerEntity player, Spell spell) {
        PlayerData data = Players.data(player);
        if (spell != null && (spell.kind() != Spell.Kind.COMBAT || data.skills().level(Skill.MAGIC) < spell.level())) {
            Msg.game(player, "You need a Magic level of " + spell.level() + " to cast this spell.");
            return;
        }
        data.setAutocast(spell);
        Msg.action(player, spell == null ? "Autocast disabled." : "Autocasting " + spell.displayName() + ".");
        ServerNetworking.sync(player);
    }

    /** Elemental staffs provide unlimited runes of their element. */
    private static boolean staffProvides(ServerPlayerEntity player, Rune rune) {
        ItemStack held = player.getMainHandStack();
        String id = held.isEmpty() ? "" : Inv.idOf(held);
        if (id.equals("steam_battlestaff")) {
            return rune == Rune.FIRE || rune == Rune.WATER;
        }
        if (id.equals("lava_battlestaff") || id.equals("mystic_lava_staff")) {
            return rune == Rune.FIRE || rune == Rune.EARTH;
        }
        if (id.equals("mud_battlestaff")) {
            return rune == Rune.WATER || rune == Rune.EARTH;
        }
        String element = rune.name().toLowerCase(java.util.Locale.ROOT);
        return id.equals("staff_of_" + element) || id.equals(element + "_battlestaff") || id.equals("mystic_" + element + "_staff");
    }

    /**
     * A blighted surge sack that pays for this wave or surge spell where sacks work (the Wilderness, Castle Wars, Soul Wars;
     * see {@link com.runecraft.osrs.data.BlightedSacks}), or null.
     */
    private static String blightedSack(ServerPlayerEntity player, Spell spell) {
        String sack = com.runecraft.osrs.data.BlightedSacks.sackFor(spell.name());
        if (sack == null || !Inv.has(player, sack, 1)) {
            return null;
        }
        com.runecraft.content.minigames.MinigameSession session = com.runecraft.content.minigames.MinigameManager.session(player);
        return com.runecraft.osrs.data.BlightedSacks.usable(sack, Gielinor.wildernessLevel(player),
                session == null ? null : session.game.id()) ? sack : null;
    }

    private static boolean hasRunes(ServerPlayerEntity player, Spell spell) {
        if (blightedSack(player, spell) != null) {
            return true;
        }
        for (Map.Entry<Rune, Integer> e : spell.runes().entrySet()) {
            if (!staffProvides(player, e.getKey()) && !Inv.has(player, e.getKey().itemId(), e.getValue())) {
                return false;
            }
        }
        return true;
    }

    private static void consumeRunes(ServerPlayerEntity player, Spell spell) {
        String sack = blightedSack(player, spell);
        if (sack != null) {
            Inv.remove(player, sack, 1);
            return;
        }
        for (Map.Entry<Rune, Integer> e : spell.runes().entrySet()) {
            if (!staffProvides(player, e.getKey())) {
                Inv.remove(player, e.getKey().itemId(), e.getValue());
            }
        }
    }

    private static boolean canCast(ServerPlayerEntity player, Spell spell) {
        if (Players.data(player).skills().currentLevel(Skill.MAGIC) < spell.level()) {
            Msg.game(player, "Your Magic level is not high enough for this spell.");
            return false;
        }
        if (!hasRunes(player, spell)) {
            Msg.game(player, "You do not have enough runes to cast this spell.");
            return false;
        }
        return true;
    }

    // ---------------- Combat ----------------

    public static void autocastAttack(ServerPlayerEntity player) {
        autocastAttack(player, null);
    }

    /** The living entity the player is looking at within spell range, or null. */
    private static LivingEntity lookTarget(ServerPlayerEntity player) {
        Vec3d eye = player.getEyePos();
        Vec3d look = player.getRotationVector();
        Vec3d end = eye.add(look.multiply(SPELL_RANGE));
        Box box = player.getBoundingBox().stretch(look.multiply(SPELL_RANGE)).expand(1.0);
        EntityHitResult hit = ProjectileUtil.raycast(player, eye, end, box,
                e -> e instanceof LivingEntity && e.isAlive() && e != player && !e.isSpectator(), SPELL_RANGE * SPELL_RANGE);
        return hit != null && hit.getEntity() instanceof LivingEntity l ? l : null;
    }

    /**
     * Casts the autocast spell (or the powered staff's spell) at {@code forced}, or at whatever the player is looking
     * at when {@code forced} is null (auto-retaliate passes the attacker).
     */
    public static void autocastAttack(ServerPlayerEntity player, LivingEntity forced) {
        PlayerData data = Players.data(player);
        String weaponId = Inv.idOf(player.getMainHandStack());
        if (CombatBonuses.poweredStaffMaxHit(weaponId, data.skills().currentLevel(Skill.MAGIC)) >= 0) {
            poweredStaffAttack(player, weaponId, forced);
            return;
        }
        Spell spell = data.autocast();
        if (spell == null) {
            Msg.action(player, "Choose a combat spell to autocast in your spellbook (press K).");
            return;
        }
        long now = ActionManager.mcTick();
        if (now - data.lastAttackTick < 5L * ActionManager.MC_TICKS_PER_GAME_TICK) {
            return;
        }
        Vec3d eye = player.getEyePos();
        LivingEntity target = forced != null ? forced : lookTarget(player);
        if (target == null) {
            Msg.action(player, "No target.");
            return;
        }
        if (target instanceof ServerPlayerEntity other && !CombatHandler.canAttackPlayer(player, other)) {
            Msg.game(player, "You can't attack that player.");
            return;
        }
        if (!CombatHandler.singleWayAllows(player, target)) {
            return;
        }
        if (!canCast(player, spell)) {
            return;
        }
        data.lastAttackTick = now;
        consumeRunes(player, spell);
        player.swingHand(Hand.MAIN_HAND, true);
        trail((ServerWorld) player.getWorld(), eye, target.getPos().add(0, target.getHeight() / 2, 0), spell);
        XpService.addTenths(player, Skill.MAGIC, spell.xpTenths());
        if (!Gielinor.is(player)) {
            target.damage(player.getDamageSources().indirectMagic(player, player), spell.maxHitAt(data.skills().currentLevel(Skill.MAGIC)) / 2.0f + 1);
            return;
        }
        // Level-scaled spell max hit, magic damage, prayers and gear effects: PlayerHitCalc.
        com.runecraft.osrs.PlayerHitCalc.Input in = HitCalcBridge.input(player, target, AttackType.MAGIC, null, 0);
        HitCalcBridge.spell(in, spell);
        long attackRoll = HitCalcBridge.attackRoll(player, target, in);
        long defenceRoll = HitCalcBridge.defenceRoll(target, in);
        int maxHit = HitCalcBridge.maxHit(player, target, in);
        boolean splash = player.getRandom().nextDouble() >= com.runecraft.osrs.Chance.hitChance(attackRoll, defenceRoll);
        int damage = splash ? 0 : player.getRandom().nextInt(Math.max(0, maxHit) + 1);
        damage = HitCalcBridge.afterRoll(in, damage); // vampyre tiers
        damage = CombatHandler.hookAfterRoll(player, target, AttackType.MAGIC, damage, maxHit);
        if (target instanceof OsrsMonsterEntity monster) {
            damage = monster.modifyIncoming(player, damage, AttackType.MAGIC);
        }
        damage = CombatHandler.protectionAgainstPlayer(target, AttackType.MAGIC, damage);
        if (splash) {
            ((ServerWorld) player.getWorld()).spawnParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 1, target.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
        }
        int dealt = CombatHandler.applyDamage(player, target, damage);
        CombatHandler.hookAfterHit(player, target, AttackType.MAGIC, dealt);
        if (dealt > 0) {
            XpService.addTenths(player, Skill.MAGIC, dealt * 20);
            XpService.addTenths(player, Skill.HITPOINTS, CombatFormulas.hitpointsXpTenths(dealt));
        }
    }

    /**
     * Powered staves (tridents, sanguinesti staff, Tumeken's shadow...): a built-in spell needing no runes,
     * whose max hit scales with Magic level, at the staff's own attack speed. Gives 2 Magic XP per damage
     * and no base cast XP.
     */
    static void poweredStaffAttack(ServerPlayerEntity player, String weaponId, LivingEntity forced) {
        PlayerData data = Players.data(player);
        SkillSet skills = data.skills();
        int speed = Equipment.weaponSpeed(player, skills);
        long now = ActionManager.mcTick();
        if (now - data.lastAttackTick < (long) speed * ActionManager.MC_TICKS_PER_GAME_TICK) {
            return;
        }
        Vec3d eye = player.getEyePos();
        LivingEntity target = forced != null ? forced : lookTarget(player);
        if (target == null) {
            Msg.action(player, "No target.");
            return;
        }
        if (target instanceof ServerPlayerEntity other && !CombatHandler.canAttackPlayer(player, other)) {
            Msg.game(player, "You can't attack that player.");
            return;
        }
        if (!CombatHandler.singleWayAllows(player, target)) {
            return;
        }
        if (Equipment.weapon(player, skills) == null) {
            Msg.game(player, "You don't have the Magic level to wield this staff.");
            return;
        }
        String denied = CombatHooks.deniedAttack(player, AttackType.MAGIC);
        if (denied != null) {
            data.lastAttackTick = now;
            Msg.game(player, denied);
            return;
        }
        data.lastAttackTick = now;
        player.swingHand(Hand.MAIN_HAND, true);
        trail((ServerWorld) player.getWorld(), eye, target.getPos().add(0, target.getHeight() / 2, 0), Spell.WATER_SURGE);
        // Built-in spell max hit, Tumeken's shadow's x3, accurate +2 and gear effects: PlayerHitCalc.
        com.runecraft.osrs.PlayerHitCalc.Input in = HitCalcBridge.input(player, target, AttackType.MAGIC, null, 0);
        long attackRoll = HitCalcBridge.attackRoll(player, target, in);
        long defenceRoll = HitCalcBridge.defenceRoll(target, in);
        int maxHit = HitCalcBridge.maxHit(player, target, in);
        boolean splash = player.getRandom().nextDouble() >= com.runecraft.osrs.Chance.hitChance(attackRoll, defenceRoll);
        int damage = splash ? 0 : player.getRandom().nextInt(Math.max(0, maxHit) + 1);
        damage = HitCalcBridge.afterRoll(in, damage); // vampyre tiers
        damage = CombatHandler.hookAfterRoll(player, target, AttackType.MAGIC, damage, maxHit);
        if (target instanceof OsrsMonsterEntity monster) {
            damage = monster.modifyIncoming(player, damage, AttackType.MAGIC);
        }
        damage = CombatHandler.protectionAgainstPlayer(target, AttackType.MAGIC, damage);
        if (splash) {
            ((ServerWorld) player.getWorld()).spawnParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 1, target.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
        }
        int dealt = CombatHandler.applyDamage(player, target, damage);
        CombatHandler.hookAfterHit(player, target, AttackType.MAGIC, dealt);
        if (dealt > 0) {
            XpService.addTenths(player, Skill.MAGIC, CombatBonuses.poweredStaffXpTenths(dealt));
            XpService.addTenths(player, Skill.HITPOINTS, CombatFormulas.hitpointsXpTenths(dealt));
            if (weaponId.startsWith("trident_of_the_swamp") && player.getRandom().nextInt(4) == 0) {
                PoisonService.venom(target, ActionManager.gameTick());
            } else if (weaponId.endsWith("sanguinesti_staff") && player.getRandom().nextInt(6) == 0) {
                player.heal(dealt / 2f);
            }
        }
    }

    /** A monster casting a spell at a player. */
    public static void npcCast(LivingEntity caster, ServerPlayerEntity target) {
        trail((ServerWorld) caster.getWorld(), caster.getEyePos(), target.getPos().add(0, 1, 0), Spell.WATER_STRIKE);
        CombatHandler.npcAttack(caster, target, AttackType.MAGIC);
    }

    private static void trail(ServerWorld world, Vec3d from, Vec3d to, Spell spell) {
        Vector3f color = switch (spell.name().substring(0, spell.name().indexOf('_'))) {
            case "WIND" -> new Vector3f(0.9f, 0.9f, 0.95f);
            case "WATER" -> new Vector3f(0.2f, 0.4f, 1.0f);
            case "EARTH" -> new Vector3f(0.5f, 0.35f, 0.15f);
            case "FIRE" -> new Vector3f(1.0f, 0.35f, 0.05f);
            default -> new Vector3f(0.7f, 0.2f, 0.9f);
        };
        DustParticleEffect dust = new DustParticleEffect(color, 1.2f);
        Vec3d delta = to.subtract(from);
        int steps = (int) Math.max(4, delta.length() * 3);
        for (int i = 0; i <= steps; i++) {
            Vec3d p = from.add(delta.multiply(i / (double) steps));
            world.spawnParticles(dust, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        world.playSound(null, from.x, from.y, from.z, SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 0.5f, 1.4f);
    }

    // ---------------- Spellbook ----------------

    public static void castFromSpellbook(ServerPlayerEntity player, Spell spell) {
        if (!Stun.check(player)) {
            return;
        }
        switch (spell.kind()) {
            case COMBAT -> setAutocast(player, spell);
            case TELEPORT -> teleport(player, spell);
            case ALCHEMY -> alch(player, spell);
            case UTILITY -> superheat(player, spell);
        }
    }

    private static void teleport(ServerPlayerEntity player, Spell spell) {
        Landmark dest = spell.destination();
        if (Teleblock.blocked(player)) {
            return;
        }
        int wild = Gielinor.wildernessLevel(player);
        if (wild > 20) {
            Msg.game(player, "A mysterious force blocks your teleport spell!");
            Msg.game(player, "You can't use this teleport after level 20 Wilderness.");
            return;
        }
        if (spell == Spell.HOME_TELEPORT) {
            Msg.action(player, "You begin casting Home Teleport...");
            ActionManager.start(player, new Action() {
                int ticks;

                @Override
                public boolean tick(ServerPlayerEntity p, long gameTick) {
                    ((ServerWorld) p.getWorld()).spawnParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1, p.getZ(), 15, 0.5, 0.8, 0.5, 0.5);
                    if (++ticks >= 16) {
                        Gielinor.teleport(p, Landmark.LUMBRIDGE);
                        return false;
                    }
                    return true;
                }
            });
            return;
        }
        if (!Gielinor.is(player)) {
            Msg.game(player, "You can only cast this teleport in Gielinor. Use Home Teleport to travel there.");
            return;
        }
        if (!canCast(player, spell)) {
            return;
        }
        consumeRunes(player, spell);
        XpService.addTenths(player, Skill.MAGIC, spell.xpTenths());
        ((ServerWorld) player.getWorld()).spawnParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1, player.getZ(), 60, 0.5, 1, 0.5, 0.5);
        int[] tile = spell.tile();
        if (tile != null && Gielinor.world(player.getServer()) != null) {
            ServerWorld world = Gielinor.world(player.getServer());
            net.minecraft.util.math.BlockPos pos = Gielinor.safeSpot(world, com.runecraft.osrs.GielinorMap.toBlockX(tile[0]),
                    com.runecraft.osrs.GielinorMap.toBlockZ(tile[1]));
            player.teleport(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYaw(), player.getPitch());
        } else {
            Gielinor.teleport(player, dest);
        }
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1f, 1f);
        com.runecraft.content.GameEvents.fireCast(player, spell.name().toLowerCase(java.util.Locale.ROOT));
    }

    private static void alch(ServerPlayerEntity player, Spell spell) {
        ItemStack held = player.getMainHandStack();
        ItemDef def = ModItems.def(held);
        if (def == null || def.id().equals("coins")) {
            Msg.game(player, "Hold the item you want to alchemise in your main hand.");
            return;
        }
        if (!canCast(player, spell)) {
            return;
        }
        consumeRunes(player, spell);
        held.decrement(1);
        int coins = spell == Spell.HIGH_LEVEL_ALCHEMY ? def.highAlch() : def.lowAlch();
        Inv.give(player, "coins", coins);
        XpService.addTenths(player, Skill.MAGIC, spell.xpTenths());
        ((ServerWorld) player.getWorld()).spawnParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.2, player.getZ(), 20, 0.3, 0.3, 0.3, 0.05);
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.6f, 1.2f);
        Msg.action(player, "You receive " + coins + " coins.");
        com.runecraft.content.GameEvents.fireCast(player, spell.name().toLowerCase(java.util.Locale.ROOT));
        ServerNetworking.sync(player);
    }

    private static void superheat(ServerPlayerEntity player, Spell spell) {
        int smithing = Players.data(player).skills().currentLevel(Skill.SMITHING);
        for (int i = Bar.values().length - 1; i >= 0; i--) {
            Bar bar = Bar.values()[i];
            if (smithing < bar.level()) {
                continue;
            }
            boolean ok = bar.ores().entrySet().stream().allMatch(e -> Inv.has(player, e.getKey(), e.getValue()));
            if (!ok) {
                continue;
            }
            if (!canCast(player, spell)) {
                return;
            }
            consumeRunes(player, spell);
            bar.ores().forEach((ore, n) -> Inv.remove(player, ore, n));
            Inv.give(player, bar.itemId(), 1);
            XpService.addTenths(player, Skill.MAGIC, spell.xpTenths());
            XpService.add(player, Skill.SMITHING, bar.xp());
            com.runecraft.content.GameEvents.fireCast(player, spell.name().toLowerCase(java.util.Locale.ROOT));
            Msg.action(player, "You superheat the ore into " + com.runecraft.util.Msg.article(ItemCatalog.get(bar.itemId()).name().toLowerCase()) + ".");
            return;
        }
        Msg.game(player, "You have no ore you can superheat.");
    }

}
