package com.runecraft.osrs;

import com.runecraft.osrs.data.DropTables;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DropTablesTest {
    @Test
    void tablesFitIn128() {
        for (String t : List.of("#gems", "#rare", "#mega")) {
            int total = DropTables.table(t).stream().mapToInt(DropTables.Entry::weight).sum();
            assertTrue(total <= DropTables.DENOMINATOR, t + " weighs " + total);
            for (DropTables.Entry e : DropTables.table(t)) {
                assertTrue(e.weight() > 0 && e.min() >= 1 && e.min() <= e.max(), e.toString());
            }
        }
    }

    @Test
    void osrsWeights() {
        Map<String, Integer> gem = weights("#gems");
        assertEquals(32, gem.get("uncut_sapphire"));
        assertEquals(16, gem.get("uncut_emerald"));
        assertEquals(8, gem.get("uncut_ruby"));
        assertEquals(2, gem.get("uncut_diamond"));
        Map<String, Integer> rare = weights("#rare");
        assertEquals(21, rare.get("coins"));
        assertEquals(20, rare.get("loop_half_of_key"));
        assertEquals(20, rare.get("tooth_half_of_key"));
        assertEquals(20, rare.get("#gems"));
        assertEquals(3000, DropTables.RARE.stream().filter(e -> e.item().equals("coins")).findFirst().orElseThrow().min());
    }

    @Test
    void rollsFollowSubTablesAndCanBeEmpty() {
        // Slot 0 of the gem table is sapphire; the last slot is empty.
        assertEquals("uncut_sapphire", DropTables.roll("#gems", bound -> 0).item());
        assertNull(DropTables.roll("#gems", bound -> bound - 1));
        Random random = new Random(7);
        int nothing = 0;
        int gems = 0;
        for (int i = 0; i < 20000; i++) {
            DropTables.Result r = DropTables.roll("#gems", random::nextInt);
            if (r == null) {
                nothing++;
            } else if (r.item().startsWith("uncut_")) {
                gems++;
            }
            assertTrue(r == null || !r.item().startsWith("#"));
        }
        // About 60/128 empty and 58/128 uncut gems.
        assertTrue(nothing > 8500 && nothing < 10200, "nothing " + nothing);
        assertTrue(gems > 8300 && gems < 9900, "gems " + gems);
        DropTables.Result coins = DropTables.roll("#rare", bound -> bound == 128 ? 20 : 0);
        assertEquals("coins", coins.item());
        assertEquals(3000, coins.count());
    }

    @Test
    void builtInTableItemsExist() {
        for (String t : List.of("#gems", "#rare", "#mega")) {
            for (DropTables.Entry e : DropTables.table(t)) {
                assertTrue(ItemCatalog.exists(e.item()), e.item());
            }
        }
    }

    private static Map<String, Integer> weights(String table) {
        Map<String, Integer> map = new HashMap<>();
        DropTables.table(table).forEach(e -> map.put(e.item(), e.weight()));
        return map;
    }
}
