package com.runecraft.osrs;

import com.runecraft.osrs.data.MonsterTraits;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every vampyre of the traits table has an explicit tier, and the tiers drive immunity and damage as in OSRS. */
class VampyreTiersTest {
    @Test
    void everyTraitVampyreHasATier() {
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, MonsterTraits.Traits> e : MonsterTraits.rows().entrySet()) {
            if (e.getValue().attributes().contains("vampyre") && !VampyreTiers.all().containsKey(e.getKey())) {
                missing.add(e.getKey());
            }
        }
        assertTrue(missing.isEmpty(), "vampyres without a tier: " + missing);
        assertEquals(2, VampyreTiers.tier("trek_vampyre_juvinate_3", Set.of("vampyre")));
        assertEquals(1, VampyreTiers.tier("some_new_vampyre", Set.of("vampyre")));
        assertEquals(0, VampyreTiers.tier("goblin", Set.of()));
    }

    private static PlayerHitCalc.Input vs(int tier, String weapon, String... worn) {
        PlayerHitCalc.Input in = new PlayerHitCalc.Input();
        in.type = AttackType.CRUSH;
        in.weapon = weapon;
        in.wear(weapon).wear(worn);
        in.attributes.add("vampyre" + tier);
        return in;
    }

    @Test
    void tiersGateDamage() {
        assertTrue(PlayerHitCalc.immune(vs(3, "abyssal_whip")));
        assertTrue(PlayerHitCalc.immune(vs(3, "rod_of_ivandis")));
        assertTrue(!PlayerHitCalc.immune(vs(3, "blisterwood_flail")));
        assertTrue(PlayerHitCalc.immune(vs(2, "abyssal_whip")));
        assertEquals(10, PlayerHitCalc.afterRoll(vs(2, "silver_sickle_b"), 30)); // silver weapons hit juvinates for at most 10
        assertEquals(15, PlayerHitCalc.afterRoll(vs(2, "abyssal_whip", "efaritays_aid"), 30)); // half damage with Efaritay's aid
        assertEquals(37, PlayerHitCalc.afterRoll(vs(1, "blisterwood_flail"), 30)); // x1.25
        assertEquals(33, PlayerHitCalc.afterRoll(vs(1, "silver_sickle_b"), 30)); // silver x1.1 vs tier 1
        assertEquals(30, PlayerHitCalc.afterRoll(vs(1, "abyssal_whip"), 30));
    }

    /**
     * Round 30: the calculator applies the vampyrebane multipliers before bolt effects and the immunity / tier 2 limits
     * after them, so an enchanted bolt proc (added between the two) can't hurt an immune vampyre or pass the silver cap.
     */
    @Test
    void boltEffectsSitBetweenScaleAndLimit() {
        PlayerHitCalc.Input stake = vs(1, "blisterwood_stake");
        stake.type = AttackType.RANGED;
        assertEquals(37, PlayerHitCalc.vampyreScale(stake, 30));
        assertEquals(37, PlayerHitCalc.vampyreLimit(stake, 37));
        PlayerHitCalc.Input ruby = vs(3, "rune_crossbow", "ruby_bolts_e");
        ruby.type = AttackType.RANGED;
        assertEquals(25, PlayerHitCalc.vampyreScale(ruby, 25)); // immune: unchanged until the limit
        assertEquals(0, PlayerHitCalc.vampyreLimit(ruby, 25 + 40)); // a ruby proc on a vyrewatch still deals nothing
        PlayerHitCalc.Input silver = vs(2, "rune_crossbow", "silver_bolts");
        silver.type = AttackType.RANGED;
        assertEquals(10, PlayerHitCalc.vampyreLimit(silver, PlayerHitCalc.vampyreScale(silver, 30)));
        for (int d = 0; d <= 60; d += 7) { // afterRoll is still the two together
            for (String w : new String[]{"blisterwood_flail", "silver_sickle_b", "abyssal_whip", "ivandis_flail", "rod_of_ivandis"}) {
                for (int tier = 1; tier <= 3; tier++) {
                    PlayerHitCalc.Input in = vs(tier, w, "efaritays_aid");
                    assertEquals(PlayerHitCalc.afterRoll(in, d), PlayerHitCalc.vampyreLimit(in, PlayerHitCalc.vampyreScale(in, d)));
                }
            }
        }
    }

    /** The rod of Ivandis: 10 charges, used by its Retainer special on vampyre juveniles / juvinates below half health. */
    @Test
    void rodOfIvandis() {
        com.runecraft.osrs.content.systems.Charges.Spec rod = com.runecraft.osrs.content.systems.Charges.of("rod_of_ivandis");
        assertEquals(10, rod.max());
        assertEquals(10, rod.initial());
        com.runecraft.osrs.content.systems.SpecialAttacks.Spec spec = com.runecraft.osrs.content.systems.SpecialAttacks.of("rod_of_ivandis");
        assertEquals(10, spec.cost());
        assertEquals(com.runecraft.osrs.content.systems.SpecialAttacks.Effect.RETAINER, spec.effect());
        assertEquals(50, spec.param()); // 30 seconds
        assertTrue(com.runecraft.osrs.content.systems.SpecialAttacks.retainerFailure("vampyre_juvenile", 0.4) == null);
        assertTrue(com.runecraft.osrs.content.systems.SpecialAttacks.retainerFailure("trek_vampyre_juvinate_2", 0.3) == null);
        assertTrue(com.runecraft.osrs.content.systems.SpecialAttacks.retainerFailure("vampyre_juvenile", 0.5).contains("half"));
        assertTrue(com.runecraft.osrs.content.systems.SpecialAttacks.retainerFailure("vyrewatch", 0.1).contains("juveniles"));
    }
}
