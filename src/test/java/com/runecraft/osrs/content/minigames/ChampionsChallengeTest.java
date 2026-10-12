package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.minigames.ChampionsChallenge.Champion;
import com.runecraft.osrs.content.minigames.ChampionsChallenge.Rule;
import com.runecraft.osrs.data.Untradeables;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Champions' Challenge: champions, rules, scrolls and the cape. */
class ChampionsChallengeTest {
    @Test
    void championsMatchTheWiki() {
        assertEquals(10, ChampionsChallenge.CHAMPIONS.size());
        assertEquals(11, ChampionsChallenge.all().size());
        // Hitpoints and combat levels (OSRS wiki / DPS calculator).
        Map<String, int[]> wiki = Map.ofEntries(
                Map.entry("imp_champion", new int[]{14, 40}), Map.entry("goblin_champion", new int[]{24, 32}),
                Map.entry("skeleton_champion", new int[]{40, 58}), Map.entry("zombies_champion", new int[]{51, 60}),
                Map.entry("giant_champion", new int[]{56, 70}), Map.entry("hobgoblin_champion", new int[]{56, 58}),
                Map.entry("ghoul_champion", new int[]{85, 100}), Map.entry("earth_warrior_champion", new int[]{102, 108}),
                Map.entry("jogre_champion", new int[]{107, 120}), Map.entry("lesser_demon_champion", new int[]{162, 148}),
                Map.entry("leon_dcour", new int[]{141, 123}));
        for (Champion c : ChampionsChallenge.all()) {
            BossDef b = ContentRegistry.boss(c.id());
            assertNotNull(b, c.id());
            assertTrue(b.raidOnly, c.id() + " is fought in the arena only");
            assertEquals(wiki.get(c.id())[0], b.combatLevel, c.id());
            assertEquals(wiki.get(c.id())[1], b.hitpoints, c.id());
            assertNotNull(ItemCatalog.get(c.scroll()), c.scroll());
            assertTrue(Untradeables.untradeable(c.scroll()), c.scroll());
            assertEquals(c, ChampionsChallenge.byScroll(c.scroll()));
        }
        assertNotNull(ItemCatalog.get(ChampionsChallenge.CAPE));
        assertTrue(Untradeables.untradeable(ChampionsChallenge.CAPE));
        assertNotNull(ContentRegistry.npc(ChampionsChallenge.LARXUS));
    }

    @Test
    void rules() {
        assertEquals(Rule.MAGIC_ONLY, ChampionsChallenge.byId("goblin_champion").rule());
        assertTrue(Rule.MAGIC_ONLY.allows(AttackType.MAGIC));
        assertFalse(Rule.MAGIC_ONLY.allows(AttackType.SLASH));
        assertFalse(Rule.RANGED_ONLY.allows(AttackType.MAGIC));
        assertTrue(Rule.RANGED_ONLY.allows(AttackType.RANGED));
        assertFalse(Rule.NO_MAGIC.allows(AttackType.MAGIC));
        assertTrue(Rule.MELEE_ONLY.allows(AttackType.CRUSH));
        assertFalse(Rule.MELEE_ONLY.allows(AttackType.RANGED));
        assertFalse(Rule.NO_MELEE.allows(AttackType.STAB));
        assertTrue(Rule.NO_MELEE.allows(AttackType.RANGED));
        assertFalse(Rule.NO_RANGED.allows(AttackType.RANGED));
        assertTrue(Rule.NO_PRAYER.allows(AttackType.MAGIC));
        assertTrue(Rule.NO_EQUIPMENT.noWeapon() && Rule.NO_EQUIPMENT.noArmour() && !Rule.NO_EQUIPMENT.emptyInventory());
        assertTrue(Rule.WEAPON_ONLY.noArmour() && !Rule.WEAPON_ONLY.noWeapon() && Rule.WEAPON_ONLY.emptyInventory());
        assertEquals(Rule.NO_PRAYER, ChampionsChallenge.byId("earth_warrior_champion").rule());
        assertEquals(Rule.NO_RANGED, ChampionsChallenge.byId("jogre_champion").rule());
        assertEquals(Rule.NO_SPECIAL_ATTACKS, ChampionsChallenge.byId("imp_champion").rule());
    }

    @Test
    void scrollsAndProgress() {
        assertEquals("imp_champion_scroll", ChampionsChallenge.scrollFor("Imp"));
        assertEquals("goblin_champion_scroll", ChampionsChallenge.scrollFor("Goblin"));
        assertEquals("hobgoblin_champion_scroll", ChampionsChallenge.scrollFor("Hobgoblin"));
        assertEquals("giant_champion_scroll", ChampionsChallenge.scrollFor("Hill giant"));
        assertEquals("giant_champion_scroll", ChampionsChallenge.scrollFor("Moss giant"));
        assertEquals("lesser_demon_champion_scroll", ChampionsChallenge.scrollFor("Lesser demon"));
        assertEquals("zombie_champion_scroll", ChampionsChallenge.scrollFor("Zombie"));
        assertNull(ChampionsChallenge.scrollFor("Giant spider"));
        assertNull(ChampionsChallenge.scrollFor("Greater demon"));
        assertNull(ChampionsChallenge.scrollFor("Goblin Champion"));
        assertNull(ChampionsChallenge.scrollFor("Skeleton Hellhound"));
        // The first carried scroll is used; Leon's only when no other is carried.
        assertEquals("imp_champion", ChampionsChallenge.chooseScroll(Set.of("imp_champion_scroll", "leons_champion_scroll")::contains).id());
        assertEquals("leon_dcour", ChampionsChallenge.chooseScroll(Set.of("leons_champion_scroll")::contains).id());
        assertNull(ChampionsChallenge.chooseScroll(Set.<String>of()::contains));
        assertFalse(ChampionsChallenge.allTenDefeated(Set.of("imp_champion")::contains));
        Set<String> ten = new java.util.HashSet<>();
        ChampionsChallenge.CHAMPIONS.forEach(c -> ten.add(c.id()));
        assertTrue(ChampionsChallenge.allTenDefeated(ten::contains));
        for (String item : ChampionsChallenge.collectionPage("Minigames").items()) {
            assertTrue(ItemCatalog.exists(item), item);
        }
    }
}
