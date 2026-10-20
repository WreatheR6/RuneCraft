package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.data.Drop;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Scurrius's group (public room) version against the OSRS Wiki infobox's Group column and its non-MVP table. */
class ScurriusGroupTest {
    @Test
    void groupStatsFollowTheWiki() {
        BossDef solo = ContentRegistry.boss(ScurriusGroup.BOSS);
        assertEquals(500, solo.hitpoints);
        BossVariant v = ScurriusGroup.variant(solo);
        assertEquals(1500, v.hitpoints());
        assertEquals(250, v.combatLevel());
        assertEquals(100, v.defence());
        assertEquals(100, v.magic());
        assertEquals(100, v.ranged());
        assertEquals(300, v.attack());
        assertEquals(ScurriusGroup.BOSS, v.kcKey());
        assertFalse(v.awakened());
        assertEquals(0, v.delve());
        Map<String, Integer> wiki = Map.of("bite", 13, "rat_magic", 14, "flying_rats", 13, "falling_ceiling", 24);
        for (AttackDef a : solo.attacks) {
            assertTrue(wiki.containsKey(a.name()), "unknown Scurrius attack " + a.name());
            assertEquals(wiki.get(a.name()), v.maxHit(a), a.name());
        }
        assertTrue(ScurriusGroup.group(2));
        assertFalse(ScurriusGroup.group(1));
    }

    @Test
    void nonMvpTableAndMvp() {
        for (Drop d : ScurriusGroup.NON_MVP) {
            assertTrue(ItemCatalog.exists(Drop.baseOf(d.item())), d.item());
            assertFalse(d.item().equals("scurrius_spine"), "the spine is the MVP's");
        }
        Map<UUID, Integer> damage = new LinkedHashMap<>();
        assertNull(ScurriusGroup.mvp(damage));
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        damage.put(a, 600);
        damage.put(b, 900);
        assertEquals(b, ScurriusGroup.mvp(damage));
    }
}
