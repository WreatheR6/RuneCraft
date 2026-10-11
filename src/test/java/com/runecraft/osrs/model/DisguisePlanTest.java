package com.runecraft.osrs.model;

import com.runecraft.osrs.content.ContentRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisguisePlanTest {
    @Test
    void rockCrabDisguisesAsItsBoulder() {
        ModelSpec spec = ContentRegistry.models().get("rock_crab");
        assertNotNull(spec, "rock_crab model");
        DisguisePlan plan = DisguisePlan.of(spec, DisguisePlan.SHELL);
        assertNotNull(plan);
        assertTrue(plan.shown().containsAll(List.of("boulder", "boulder_rim", "boulder_top", "lichen", "barnacles")),
                plan.shown().toString());
        assertEquals(Set.of("body"), plan.ancestors());
        List<String> hidden = new ArrayList<>();
        for (ModelSpec.Part p : spec.parts()) {
            if (!plan.visible(p.name)) {
                hidden.add(p.name);
            }
        }
        // legs, claws, eyes and face are all hidden: nothing of the crab shows
        assertTrue(hidden.contains("face"), hidden.toString());
        assertTrue(hidden.stream().anyMatch(n -> n.startsWith("leg")), hidden.toString());
        assertTrue(hidden.stream().anyMatch(n -> n.startsWith("arm")), hidden.toString());
        assertTrue(hidden.stream().noneMatch(n -> n.startsWith("boulder")));
        // rests on the ground: the rim's bottom (y 19) is lowered to y 24
        assertEquals(5f, plan.drop(), 0.01f);
    }

    @Test
    void modelsWithoutAShellHaveNoDisguise() {
        ModelSpec imp = ContentRegistry.models().get("imp");
        assertNotNull(imp);
        assertNull(DisguisePlan.of(imp, DisguisePlan.SHELL));
    }
}
