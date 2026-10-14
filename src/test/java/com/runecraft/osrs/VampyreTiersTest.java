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
}
