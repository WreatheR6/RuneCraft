package com.runecraft.combat;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.NpcHitCalc;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * Damage reduction on hits a player takes (round 17), applied by {@link CombatHandler#applyDamage} to every hit from a
 * monster or another player: the Elysian spirit shield (70% chance to take a quarter off, at least 1), Dinh's bulwark in
 * its Block stance (-20%) and the Justiciar set (-defence bonus / 3000 of the hit, at least 1; not in PvP and not on
 * typeless damage, i.e. a boss mechanic named without a style). The maths is
 * {@link NpcHitCalc#reduceIncoming}, tested against the OSRS Wiki DPS calculator. Dinh's bulwark can't attack in Block.
 */
public final class DefenceEffects {
    private DefenceEffects() {
    }

    /** The hit after the defender's damage-reducing gear; {@code style} is the attack's style when known. */
    public static int reduce(ServerPlayer p, LivingEntity attacker, AttackType style, boolean typeless, int damage) {
        String shield = Inv.idOf(p.getOffhandItem());
        String weapon = Inv.idOf(p.getMainHandItem());
        boolean elysian = "elysian_spirit_shield".equals(shield);
        boolean dinhs = NpcHitCalc.dinhsBlocking(weapon, Players.data(p).style());
        // OSRS Wiki: the Justiciar set effect works outside PvP only and never on typeless damage.
        boolean justiciar = !typeless && !(attacker instanceof ServerPlayer)
                && NpcHitCalc.justiciarSet(Inv.idOf(p.getItemBySlot(EquipmentSlot.HEAD)),
                Inv.idOf(p.getItemBySlot(EquipmentSlot.CHEST)), Inv.idOf(p.getItemBySlot(EquipmentSlot.LEGS)));
        if (!elysian && !dinhs && !justiciar) {
            return damage;
        }
        int bonus = 0;
        if (justiciar) {
            AttackType s = style;
            if (s == null) {
                // approx: a hit that wasn't named at all uses the attacking monster's main style
                s = MonsterStats.of(attacker).style();
            }
            bonus = HitCalcBridge.gearStats(p).defence(s);
        }
        boolean proc = elysian && p.getRandom().nextDouble() < NpcHitCalc.ELYSIAN_CHANCE;
        return NpcHitCalc.reduceIncoming(damage, proc, dinhs, justiciar, bonus);
    }

    /** Registers Dinh's bulwark's rule: no attacking in the Block stance. */
    static void register() {
        CombatHooks.addAttackGate((player, type) -> type.isMelee()
                && NpcHitCalc.dinhsBlocking(Inv.idOf(player.getMainHandItem()), Players.data(player).style())
                ? "Your bulwark gets in the way." : null);
    }
}
