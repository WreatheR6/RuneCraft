package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.data.Bar;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlastFurnaceTest {

    @Test
    void halvesTheCoal() {
        assertEquals(1, BlastFurnaceData.coalPerBar(Bar.STEEL));
        assertEquals(2, BlastFurnaceData.coalPerBar(Bar.MITHRIL));
        assertEquals(3, BlastFurnaceData.coalPerBar(Bar.ADAMANTITE));
        assertEquals(4, BlastFurnaceData.coalPerBar(Bar.RUNITE));
        assertEquals(0, BlastFurnaceData.coalPerBar(Bar.GOLD));
    }

    @Test
    void steelAndThenIronWhenCoalRunsOut() {
        BlastFurnaceData.Furnace f = new BlastFurnaceData.Furnace();
        assertEquals(28, BlastFurnaceData.deposit(f, "iron_ore", 40));
        assertEquals(20, BlastFurnaceData.deposit(f, "coal", 20));
        List<BlastFurnaceData.Smelted> out = BlastFurnaceData.smelt(f, 99);
        assertEquals(20, f.bars("steel_bar"));
        // Iron never fails at the Blast Furnace: the 8 iron ore left become 8 iron bars.
        assertEquals(8, f.bars("iron_bar"));
        assertEquals(0, f.ore("iron_ore"));
        assertEquals(0, f.ore("coal"));
        assertEquals(20 * 17.5 + 8 * 12.5, out.stream().mapToDouble(BlastFurnaceData.Smelted::xp).sum(), 1e-9);
    }

    @Test
    void runiteNeedsFourCoalAndTheLevel() {
        BlastFurnaceData.Furnace f = new BlastFurnaceData.Furnace();
        BlastFurnaceData.deposit(f, "runite_ore", 28);
        BlastFurnaceData.deposit(f, "coal", 254);
        BlastFurnaceData.deposit(f, "coal", 10);
        assertEquals(254, f.ore("coal"));
        assertTrue(BlastFurnaceData.smelt(f, 84).isEmpty(), "85 Smithing needed");
        BlastFurnaceData.smelt(f, 85);
        assertEquals(28, f.bars("runite_bar"));
        assertEquals(254 - 28 * 4, f.ore("coal"));
        assertEquals(28, BlastFurnaceData.collect(f, "runite_bar", 50));
        assertEquals(0, f.bars("runite_bar"));
    }

    @Test
    void bronzeAndGold() {
        BlastFurnaceData.Furnace f = new BlastFurnaceData.Furnace();
        BlastFurnaceData.deposit(f, "copper_ore", 10);
        BlastFurnaceData.deposit(f, "tin_ore", 6);
        BlastFurnaceData.deposit(f, "gold_ore", 5);
        assertEquals(0, BlastFurnaceData.deposit(f, "logs", 5));
        BlastFurnaceData.smelt(f, 40);
        assertEquals(6, f.bars("bronze_bar"));
        assertEquals(4, f.ore("copper_ore"));
        assertEquals(5, f.bars("gold_bar"));
    }

    @Test
    void feesAndCoffer() {
        BlastFurnaceData.Furnace f = new BlastFurnaceData.Furnace();
        assertTrue(BlastFurnaceData.needsFee(59, f, 0));
        assertFalse(BlastFurnaceData.needsFee(60, f, 0));
        f.permitUntil = BlastFurnaceData.FOREMAN_TICKS;
        assertFalse(BlastFurnaceData.needsFee(1, f, 999));
        assertTrue(BlastFurnaceData.needsFee(1, f, 1000));
        // 72,000 coins pay for exactly one hour (6,000 game ticks).
        f.coffer = 72_000;
        assertTrue(BlastFurnaceData.drain(f, 6_000));
        assertEquals(0, f.coffer);
        assertFalse(BlastFurnaceData.drain(f, 1));
    }

    @Test
    void placeAndBlocksExist() {
        PlaceDef bf = ContentRegistry.places().stream().filter(p -> p.id().equals("blast_furnace")).findFirst().orElseThrow();
        assertEquals(1940, bf.osrsX());
        assertEquals(4958, bf.osrsY());
        assertTrue(ContentRegistry.places().stream().anyMatch(p -> p.blocks().contains("blast_furnace_stairs")));
        assertTrue(ContentRegistry.blocks().stream().anyMatch(b -> b.id().equals("blast_furnace_conveyor")));
    }
}
