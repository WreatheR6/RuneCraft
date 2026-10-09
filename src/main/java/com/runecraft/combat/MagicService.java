package com.runecraft.combat;

import com.runecraft.action.Action;
import com.runecraft.action.ActionManager;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.AttackType;
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
        return id.equals("staff_of_" + rune.name().toLowerCase(java.util.Locale.ROOT));
    }

    private static boolean hasRunes(ServerPlayerEntity player, Spell spell) {
        for (Map.Entry<Rune, Integer> e : spell.runes().entrySet()) {
            if (!staffProvides(player, e.getKey()) && !Inv.has(player, e.getKey().itemId(), e.getValue())) {
                return false;
            }
        }
        return true;
    }

    private static void consumeRunes(ServerPlayerEntity player, Spell spell) {
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
        PlayerData data = Players.data(player);
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
        Vec3d look = player.getRotationVec(1.0f);
        Vec3d end = eye.add(look.multiply(SPELL_RANGE));
        Box box = player.getBoundingBox().stretch(look.multiply(SPELL_RANGE)).expand(1.0);
        EntityHitResult hit = ProjectileUtil.raycast(player, eye, end, box,
                e -> e instanceof LivingEntity && e.isAlive() && e != player && !e.isSpectator(), SPELL_RANGE * SPELL_RANGE);
        if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
            Msg.action(player, "No target.");
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
            target.damage(player.getDamageSources().indirectMagic(player, player), spell.maxHit() / 2.0f + 1);
            return;
        }
        SkillSet skills = data.skills();
        EquipmentStats eq = Equipment.total(player, skills);
        int eff = CombatFormulas.effectiveMagic(skills.currentLevel(Skill.MAGIC), PrayerService.magic(data));
        long attackRoll = CombatFormulas.attackRoll(eff, eq.magicAttack());
        long defenceRoll = CombatHandler.defenceRoll(target, AttackType.MAGIC);
        int maxHit = CombatFormulas.magicMaxHit(spell.maxHit(), eq.magicDamagePercent());
        int damage = CombatFormulas.roll(new java.util.Random(player.getRandom().nextLong()), attackRoll, defenceRoll, maxHit);
        boolean splash = player.getRandom().nextDouble() >= com.runecraft.osrs.Chance.hitChance(attackRoll, defenceRoll) && damage == 0;
        if (target instanceof OsrsMonsterEntity monster) {
            damage = monster.modifyIncoming(player, damage, AttackType.MAGIC);
        }
        if (splash) {
            ((ServerWorld) player.getWorld()).spawnParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 1, target.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
        }
        int dealt = CombatHandler.applyDamage(player, target, damage);
        if (dealt > 0) {
            XpService.addTenths(player, Skill.MAGIC, dealt * 20);
            XpService.addTenths(player, Skill.HITPOINTS, CombatFormulas.hitpointsXpTenths(dealt));
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
        Gielinor.teleport(player, dest);
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1f, 1f);
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
            Msg.action(player, "You superheat the ore into " + com.runecraft.util.Msg.article(ItemCatalog.get(bar.itemId()).name().toLowerCase()) + ".");
            return;
        }
        Msg.game(player, "You have no ore you can superheat.");
    }

}
