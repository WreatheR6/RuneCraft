package com.runecraft.osrs;

import com.runecraft.osrs.content.minigames.MinigameRules;
import com.runecraft.osrs.content.systems.SpecialAttacks;
import com.runecraft.osrs.content.systems.Spellbooks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reference cases for combat formulas and gear effects (OSRS wiki / DPS calculator values). */
class CombatBonusesTest {
    @Test
    void referenceMeleeSetups() {
        // Abyssal whip (+82 str) alone at 99 Strength, aggressive, no prayer: max hit 25.
        assertEquals(25, CombatFormulas.meleeMaxHit(CombatFormulas.effectiveStrength(99, 1.0, CombatStyle.AGGRESSIVE), 82));
        // 118 Strength (super combat), Piety, aggressive: eff 156; +136 strength bonus -> 49.
        int eff = CombatFormulas.effectiveStrength(118, Prayer.PIETY.strengthMultiplier(), CombatStyle.AGGRESSIVE);
        assertEquals(156, eff);
        assertEquals(49, CombatFormulas.meleeMaxHit(eff, 136));
        // Controlled gives +1 to each level; accurate +3 attack.
        assertEquals(108, CombatFormulas.effectiveAttack(99, 1.0, CombatStyle.CONTROLLED));
        assertEquals(110, CombatFormulas.effectiveAttack(99, 1.0, CombatStyle.ACCURATE));
    }

    @Test
    void dharoks() {
        // Base 50 at 99 HP, standing on 1 HP: 50 + floor(50 * 98 * 99 / 10000) = 98.
        assertEquals(98, CombatBonuses.dharokMaxHit(50, 99, 1));
        assertEquals(50, CombatBonuses.dharokMaxHit(50, 99, 99));
        assertEquals(1.9702, CombatBonuses.dharok(99, 1), 1e-9);
    }

    @Test
    void salveAndSlayerValues() {
        assertEquals(7.0 / 6.0, CombatBonuses.salve("salve_amulet", AttackType.SLASH), 1e-9);
        assertEquals(1.0, CombatBonuses.salve("salve_amulet", AttackType.RANGED), 1e-9);
        assertEquals(1.2, CombatBonuses.salve("salve_amulet_e", AttackType.CRUSH), 1e-9);
        assertEquals(1.15, CombatBonuses.salve("salve_amulet_i", AttackType.MAGIC), 1e-9);
        assertEquals(7.0 / 6.0, CombatBonuses.salve("salve_amulet_i", AttackType.RANGED), 1e-9);
        assertEquals(1.2, CombatBonuses.salve("salve_amulet_ei", AttackType.MAGIC), 1e-9);
        assertEquals(1.0, CombatBonuses.salve("amulet_of_glory", AttackType.STAB), 1e-9);
    }

    @Test
    void obsidianAndCrystal() {
        // Obsidian armour (+10%) and berserker necklace (+20%) add up to +30%.
        assertEquals(1.3, 1.1 * CombatBonuses.berserkerOnTop(true), 1e-9);
        assertEquals(1.2, CombatBonuses.berserkerOnTop(false), 1e-9);
        assertEquals(1.30, CombatBonuses.crystalAccuracy(true, true, true), 1e-9);
        assertEquals(1.15, CombatBonuses.crystalDamage(true, true, true), 1e-9);
        assertTrue(CombatBonuses.isCrystalBow("bow_of_faerdhinen"));
        assertFalse(CombatBonuses.isCrystalBow("magic_shortbow"));
    }

    @Test
    void twistedBow() {
        // Outside CoX the magic value caps at 250: accuracy capped at 140%, damage 215%.
        int m = CombatBonuses.twistedBowMagic(400, 0, false);
        assertEquals(250, m);
        assertEquals(140, CombatBonuses.twistedBowAccuracy(m));
        assertEquals(215, CombatBonuses.twistedBowDamage(m));
        // In the Chambers of Xeric (cap 350) the damage multiplier reaches 248% and never the 250% cap.
        int cox = CombatBonuses.twistedBowMagic(400, 0, true);
        assertEquals(140, CombatBonuses.twistedBowAccuracy(cox));
        assertEquals(248, CombatBonuses.twistedBowDamage(cox));
        // Low-magic targets are hit much less hard.
        assertTrue(CombatBonuses.twistedBowDamage(1) < 100);
    }

    @Test
    void poweredStaves() {
        assertEquals(20, CombatBonuses.poweredStaffMaxHit("trident_of_the_seas", 75));
        assertEquals(28, CombatBonuses.poweredStaffMaxHit("trident_of_the_seas", 99));
        assertEquals(31, CombatBonuses.poweredStaffMaxHit("trident_of_the_swamp", 99));
        assertEquals(33, CombatBonuses.poweredStaffMaxHit("sanguinesti_staff", 99)); // wiki / DPS calculator: floor(Magic / 3)
        assertEquals(34, CombatBonuses.poweredStaffMaxHit("tumekens_shadow", 99));
        assertEquals(31, CombatBonuses.poweredStaffMaxHit("dawnbringer", 99)); // halved to 15 by PlayerHitCalc
        assertEquals(-1, CombatBonuses.poweredStaffMaxHit("staff_of_air", 99));
        // Shadow triples magic damage, capped at +100%.
        assertEquals(30, CombatBonuses.shadowMagicDamage(10, false));
        assertEquals(100, CombatBonuses.shadowMagicDamage(40, false));
        assertEquals(40, CombatBonuses.shadowMagicDamage(10, true));
        // 99 Magic shadow with +1% (staff only): floor(34 * 1.03) = 35.
        assertEquals(35, CombatFormulas.magicMaxHit(34, CombatBonuses.shadowMagicDamage(1, false)));
    }

    @Test
    void magicMaxHitsAndPrayers() {
        // Ice barrage (30) with an occult necklace (+10%) = 33.
        assertEquals(33, CombatFormulas.magicMaxHit(30, 10));
        assertEquals(4, Prayer.AUGURY.magicDamagePercent());
        assertEquals(2, Prayer.MYSTIC_MIGHT.magicDamagePercent());
        assertEquals(1, Prayer.MYSTIC_LORE.magicDamagePercent());
        assertEquals(0, Prayer.PIETY.magicDamagePercent());
        assertEquals(1.25, Prayer.AUGURY.magicMultiplier(), 1e-9);
        assertEquals(1.20, Prayer.RIGOUR.rangedAttackMultiplier(), 1e-9);
        assertEquals(1.23, Prayer.RIGOUR.rangedStrengthMultiplier(), 1e-9);
        // Drain resistance 60 + 2 * prayer bonus; Piety drains 24 per tick.
        assertEquals(24, Prayer.PIETY.drainEffect());
        assertEquals(12, Prayer.PROTECT_FROM_MELEE.drainEffect());
        // Magic XP: base + 2 per damage (Fire Surge 50.5 + 24 damage = 98.5 XP).
        assertEquals(985, CombatBonuses.magicXpTenths(Spell.FIRE_SURGE.xpTenths(), 24));
    }

    @Test
    void voidValues() {
        assertEquals(1.45, MinigameRules.PestControl.voidAccuracy(2), 1e-9);
        assertEquals(1.10, MinigameRules.PestControl.voidDamage(0, false), 1e-9);
        assertEquals(1.125, MinigameRules.PestControl.voidDamage(1, true), 1e-9);
        assertEquals(1.05, MinigameRules.PestControl.voidDamage(2, true), 1e-9);
    }

    @Test
    void pvpRules() {
        assertEquals(30, CombatBonuses.protectedFromPlayer(50));
        assertEquals(0, CombatBonuses.protectedFromPlayer(1));
        assertTrue(CombatBonuses.canAttackInWilderness(100, 110, 10, 12));
        assertFalse(CombatBonuses.canAttackInWilderness(100, 111, 10, 12));
        assertFalse(CombatBonuses.canAttackInWilderness(100, 100, 0, 5));
        assertFalse(CombatBonuses.canAttackInWilderness(100, 105, 20, 4));
    }

    @Test
    void aggressionAndTolerance() {
        // A level 2 goblin attacks players up to combat level 4.
        assertTrue(CombatBonuses.aggressiveTo(2, 4));
        assertFalse(CombatBonuses.aggressiveTo(2, 5));
        CombatBonuses.Tolerance t = new CombatBonuses.Tolerance();
        assertFalse(t.update(100, 100, 0));
        assertFalse(t.update(105, 95, 999));
        assertTrue(t.update(110, 90, 1000));
        // Leaving the 21x21 area restarts the 10 minutes.
        assertFalse(t.update(111, 100, 1001));
        assertTrue(t.update(111, 100, 2001));
    }

    @Test
    void freezeDurationsAreMultiplesOfEightTicks() {
        assertEquals(8, Spellbooks.freezeTicks(Spellbooks.get("ice_rush")));
        assertEquals(16, Spellbooks.freezeTicks(Spellbooks.get("ice_burst")));
        assertEquals(24, Spellbooks.freezeTicks(Spellbooks.get("ice_blitz")));
        assertEquals(32, Spellbooks.freezeTicks(Spellbooks.get("ice_barrage")));
        assertEquals(8, Spellbooks.freezeTicks(Spellbooks.get("bind")));
        assertEquals(16, Spellbooks.freezeTicks(Spellbooks.get("snare")));
        assertEquals(24, Spellbooks.freezeTicks(Spellbooks.get("entangle")));
        assertEquals(32, SpecialAttacks.of("zamorak_godsword").param());
    }

    @Test
    void specialAttackReferenceValues() {
        SpecialAttacks.Spec ags = SpecialAttacks.of("armadyl_godsword");
        assertEquals(50, ags.cost());
        assertEquals(1.375, ags.damage(), 1e-9);
        assertEquals(2.0, ags.accuracy(), 1e-9);
        assertEquals(25, SpecialAttacks.of("dragon_dagger").cost());
        assertEquals(55, SpecialAttacks.of("dragon_scimitar").cost());
        assertEquals(50, SpecialAttacks.of("dragon_warhammer").cost());
        assertEquals(30, SpecialAttacks.of("dragon_warhammer").param());
        assertEquals(50, SpecialAttacks.of("toxic_blowpipe").cost());
        assertEquals(55, SpecialAttacks.of("dark_bow").cost());
        // SGS: heal half (min 10), prayer a quarter (min 5).
        assertEquals(10, SpecialAttacks.sgsRestore(12)[0]);
        assertEquals(20, SpecialAttacks.sgsRestore(40)[0]);
        assertEquals(10, SpecialAttacks.sgsRestore(40)[1]);
    }

    @Test
    void boostsAndDrainsDecayOnSeparateTimers() {
        SkillSet s = new SkillSet();
        s.setBoost(Skill.ATTACK, 5);
        s.setBoost(Skill.DEFENCE, -5);
        s.decay(false, true, true);
        assertEquals(5, s.boost(Skill.ATTACK));
        assertEquals(-4, s.boost(Skill.DEFENCE));
        s.decay(true, false, true);
        assertEquals(4, s.boost(Skill.ATTACK));
        assertEquals(-4, s.boost(Skill.DEFENCE));
    }
}
