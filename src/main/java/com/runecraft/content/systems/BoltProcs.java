package com.runecraft.content.systems;

import com.runecraft.action.ActionManager;
import com.runecraft.combat.MonsterStats;
import com.runecraft.combat.PoisonService;
import com.runecraft.network.Net;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.systems.BoltEffects;
import com.runecraft.osrs.content.systems.Diaries;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.skill.Stun;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.Locale;
import java.util.Random;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/** Enchanted (e) bolt special effects on a player's crossbow shot (see {@link BoltEffects}). */
public final class BoltProcs {
    private BoltProcs() {
    }

    /** Hook from the ranged attack: the damage to deal after the bolt's effect (unchanged for other ammo). */
    private static String lastShot;
    private static long lastShotTick = -1;

    /**
     * The ammunition of the ranged shot being resolved right now (set by {@link #apply} just before the target's
     * {@code modifyIncoming}), or null; read once. Boss scripts use it for bolt effects with their own rules (the Phantom
     * Muspah's prayer shield and enchanted sapphire bolts).
     */
    public static String takeShot() {
        String s = lastShotTick == ActionManager.mcTick() ? lastShot : null;
        lastShot = null;
        return s;
    }

    public static int apply(ServerPlayer p, LivingEntity target, String ammo, boolean accurate, int damage, int maxHit) {
        lastShot = ammo;
        lastShotTick = ActionManager.mcTick();
        BoltEffects.Enchant en = BoltEffects.ofItem(ammo);
        if (en == null) {
            return damage;
        }
        boolean kandarin = Diaries.tierComplete(SysState.of(p).diaryDone, "kandarin", Diaries.Tier.HARD);
        boolean zaryte = Inv.idOf(p.getMainHandItem()).startsWith("zaryte_crossbow");
        boolean vsPlayer = target instanceof ServerPlayer;
        BoltEffects.Context ctx = new BoltEffects.Context(accurate, damage, maxHit,
                Players.data(p).skills().currentLevel(Skill.RANGED), (int) Math.ceil(target.getHealth()), (int) Math.ceil(p.getHealth()),
                zaryte, attribute(target, "fiery", "fire", "infernal", "lava", "tzhaar", "pyrefiend", "hellhound"),
                attribute(target, "dragon", "dragon", "wyvern", "wyrm", "drake", "hydra", "vorkath", "galvek"),
                SysCombat.isUndead(target), vsPlayer);
        BoltEffects.Outcome o = BoltEffects.roll(en, BoltEffects.chance(en, kandarin), ctx, new Random(p.getRandom().nextLong()));
        if (!o.activated()) {
            return damage;
        }
        ((ServerLevel) p.level()).sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY(0.6), target.getZ(),
                12, 0.3, 0.4, 0.3, 0.1);
        if (o.selfDamage() > 0) {
            p.setHealth(Math.max(1f, p.getHealth() - o.selfDamage()));
            ServerNetworking.hitsplat(p, o.selfDamage(), Net.SPLAT_DAMAGE);
        }
        if (o.heal() > 0) {
            p.heal(o.heal());
        }
        if (o.poison() > 0) {
            PoisonService.poison(target, o.poison(), ActionManager.gameTick());
        }
        if (o.stunTicks() > 0) {
            if (target instanceof ServerPlayer other) {
                Stun.apply(other, o.stunTicks());
            } else {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, o.stunTicks() * 12, 10, false, true));
            }
        }
        if (o.drain() > 0 && target instanceof ServerPlayer other) {
            PlayerData d = Players.data(other);
            if (en.effect() == BoltEffects.Effect.CLEAR_MIND) {
                int drained = Math.min(d.prayerPoints(), o.drain());
                d.setPrayerPoints(d.prayerPoints() - drained);
                PlayerData mine = Players.data(p);
                mine.setPrayerPoints(Math.min(mine.skills().level(Skill.PRAYER), mine.prayerPoints() + drained / 2));
                ServerNetworking.sync(p);
            } else {
                d.skills().setBoost(Skill.MAGIC, d.skills().boost(Skill.MAGIC) - o.drain());
            }
            ServerNetworking.sync(other);
        }
        Msg.game(p, "Your " + en.name().toLowerCase(Locale.ROOT) + " bolt effect activates: "
                + en.effect().name().toLowerCase(Locale.ROOT).replace('_', ' ') + "!");
        return o.damage();
    }

    /** A monster attribute from its wiki reference stats, else guessed from the name. */
    private static boolean attribute(LivingEntity target, String attribute, String... nameHints) {
        if (target instanceof ServerPlayer) {
            return false;
        }
        MonsterStats stats = MonsterStats.of(target);
        if (stats.profile() != null && stats.profile().referenceStats) {
            return stats.profile().hasAttribute(attribute);
        }
        String n = stats.name().toLowerCase(Locale.ROOT);
        for (String h : nameHints) {
            if (n.contains(h)) {
                return true;
            }
        }
        return false;
    }
}
