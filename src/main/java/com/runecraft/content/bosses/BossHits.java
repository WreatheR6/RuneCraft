package com.runecraft.content.bosses;

import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.MonsterStats;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.CombatFormulas;
import com.runecraft.osrs.Prayer;
import com.runecraft.player.Players;
import java.util.Random;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Hits that boss scripts roll themselves instead of through {@code Mechanics.perform}, for attacks with their own prayer
 * rules (The Nightmare's 20% through prayer, the Phantom Muspah's magic orb prayed in flight) or attackers driven by the
 * script (husks, parasites).
 */
final class BossHits {
    private BossHits() {
    }

    /** The protection prayer against a style. */
    static Prayer protection(AttackType style) {
        return style == null ? null : style == AttackType.MAGIC ? Prayer.PROTECT_FROM_MAGIC
                : style == AttackType.RANGED ? Prayer.PROTECT_FROM_MISSILES : Prayer.PROTECT_FROM_MELEE;
    }

    /** Whether the player prays against {@code style}, read through the one protection chokepoint (CombatHandler.isProtected). */
    static boolean protectedFrom(ServerPlayer p, AttackType style) {
        return style != null && CombatHandler.isProtected(Players.data(p), style);
    }

    /**
     * Whether the player prays a protection prayer, but not the one against {@code style}. Read strictly from the active
     * prayers on purpose (no switch grace): Phosani's 120% punishes the wrong prayer that is up right now.
     */
    static boolean wrongProtection(ServerPlayer p, AttackType style) {
        var active = Players.data(p).activePrayers();
        Prayer right = protection(style);
        for (Prayer pr : new Prayer[]{Prayer.PROTECT_FROM_MELEE, Prayer.PROTECT_FROM_MAGIC, Prayer.PROTECT_FROM_MISSILES}) {
            if (pr != right && active.contains(pr)) {
                return true;
            }
        }
        return false;
    }

    /** A monster's attack roll with its own levels and bonuses (curse drains included). */
    static long attackRoll(OsrsMonsterEntity mob, AttackType style) {
        MonsterStats s = mob.stats();
        int level = style == AttackType.MAGIC ? s.magic() : style == AttackType.RANGED ? s.ranged() : s.attack();
        return CombatHandler.npcAttackRoll(mob, s, style, level);
    }

    /** An accuracy-rolled hit (or one that always hits) of up to {@code maxHit}, before any prayer. */
    static int roll(ServerPlayer target, AttackType style, int maxHit, long attackRoll, boolean alwaysHits, Random r) {
        if (alwaysHits || style == null) {
            return r.nextInt(Math.max(0, maxHit) + 1);
        }
        return CombatFormulas.roll(r, attackRoll, CombatHandler.defenceRoll(target, style), maxHit);
    }

    /**
     * Deals a hit under an attack name (Combat Achievement "no damage from" rules; null style: typeless), with the reactions
     * any hit has (Vengeance, recoil); returns the damage dealt.
     */
    static int deal(LivingEntity attacker, ServerPlayer p, String name, AttackType style, int damage) {
        CombatHooks.nameAttack(name, style);
        int dealt = CombatHandler.applyDamage(attacker != null && attacker.isAlive() ? attacker : null, p, Math.max(0, damage));
        if (dealt > 0 && attacker != null) {
            CombatHooks.hurt(p, attacker, dealt);
        }
        return dealt;
    }
}
