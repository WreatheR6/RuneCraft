package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Recipes;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.bosses.BossMechanics.YamaContract;
import com.runecraft.osrs.content.systems.CombatAchievements;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Round 32 boss content: Hueycoatl max hits, Zalcano's tephra, the elemental sceptres, Yama's sigil and acquisition contracts. */
class Round32BossesTest {
    @Test
    void hueycoatlHitsAsTheWiki() {
        // Feathers (Rain Fire) up to 14, lightning up to 16, the tail slam shockwave 20-25.
        assertEquals(14, VarlamoreBosses.HUEY_MELEE.maxHit());
        assertEquals(14, VarlamoreBosses.HUEY_RANGED.maxHit());
        assertEquals(14, VarlamoreBosses.HUEY_MAGIC.maxHit());
        assertEquals(16, VarlamoreBosses.HUEY_LIGHTNING.maxHit());
        assertEquals(BossMechanics.HUEY_SHOCKWAVE_MAX, VarlamoreBosses.HUEY_TAIL.maxHit());
    }

    @Test
    void zalcanoTephraFormulas() {
        // 5 + floor((Smithing + Runecraft) / 14), +50% on a blue symbol.
        assertEquals(15, BossMechanics.tephraMaxHit(70, 70, false));
        assertEquals(22, BossMechanics.tephraMaxHit(70, 70, true));
        assertEquals(19, BossMechanics.tephraMaxHit(99, 99, false));
        // Mining her: 30/31/32/33 by Mining level, +1 dragon/infernal (61), +2 crystal (71), +0 rune (41).
        assertEquals(30, BossMechanics.zalcanoMiningMaxHit(70, 41));
        assertEquals(32, BossMechanics.zalcanoMiningMaxHit(80, 61));
        assertEquals(35, BossMechanics.zalcanoMiningMaxHit(99, 71));
        assertEquals(50, BossMechanics.golemArmourHeal(1.0));
        assertEquals(25, BossMechanics.golemArmourHeal(0.5));
        assertEquals(0, BossMechanics.golemArmourHeal(-1));
        for (String id : BossMechanics.TEPHRA_ITEMS) {
            assertNotNull(ItemCatalog.get(id), id);
        }
        BossDef z = ContentRegistry.boss("zalcano");
        assertEquals(5, z.attackSpeed);
        assertEquals(14, ContentRegistry.monster("zalcano_golem").hitpoints);
        CombatAchievements.Task teamPlayer = CombatAchievements.tasks().stream().filter(t -> t.wikiId() == 330).findFirst().orElseThrow();
        assertNotEquals(CombatAchievements.Track.UNTRACKED, teamPlayer.track());
    }

    @Test
    void elementalAncientSceptres() {
        assertNotNull(ItemCatalog.get("ancient_sceptre"));
        for (String e : List.of("blood", "ice", "smoke", "shadow")) {
            String id = e + "_ancient_sceptre";
            assertNotNull(ItemCatalog.get(id), id);
            Recipe make = Recipes.get("make_" + id);
            assertEquals(Map.of("ancient_sceptre", 1, e + "_quartz", 1), make.inputs());
            assertEquals(Map.of(id, 1), make.outputs());
            assertEquals(Map.of("ancient_sceptre", 1, e + "_quartz", 1), Recipes.get("dismantle_" + id).outputs());
            assertTrue(com.runecraft.osrs.data.Untradeables.untradeable(id), id);
        }
    }

    @Test
    void ancientVaultChest() {
        // One table per quartz (ice, smoke, shadow, blood), five rewards at 1/5 each, every one a real item.
        assertEquals(Dt2Bosses.QUARTZES.size(), Dt2Bosses.VAULT_CHEST.size());
        for (List<Object[]> table : Dt2Bosses.VAULT_CHEST) {
            assertEquals(5, table.size());
            for (Object[] r : table) {
                assertNotNull(ItemCatalog.get((String) r[0]), (String) r[0]);
            }
        }
        assertEquals("mist_rune", Dt2Bosses.VAULT_CHEST.get(0).get(0)[0]);
        assertEquals("tuna_potato", Dt2Bosses.VAULT_CHEST.get(3).get(3)[0]);
        assertNotNull(ContentRegistry.place("ancient_vault"));
    }

    @Test
    void yamaSigilAndAcquisitionContracts() {
        // The five sigil contracts keep their ordinals (saved/compared) and come back with their sigil piece.
        assertEquals(4, YamaContract.BLOODIED_BLOWS.ordinal());
        assertEquals("purifying_sigil_top", YamaContract.FORFEIT_BREATH.reward());
        assertEquals("purifying_sigil_bottom", YamaContract.GLYPHIC_ATTENUATION.reward());
        assertEquals("purifying_sigil_left", YamaContract.SENSORY_CLOUDING.reward());
        assertEquals("purifying_sigil_right", YamaContract.DIVINE_SEVERANCE.reward());
        assertEquals("purifying_sigil_middle", YamaContract.BLOODIED_BLOWS.reward());
        assertTrue(YamaContract.DIVINE_SEVERANCE.returnedOnKill());
        assertFalse(YamaContract.CATALYST_ACQUISITION.returnedOnKill());
        assertEquals(2000, YamaContract.CATALYST_ACQUISITION.rewardQuantity());
        assertEquals(250, YamaContract.WORM_ACQUISITION.rewardQuantity());
        assertEquals(45, YamaContract.SHARD_ACQUISITION.rewardQuantity());
        assertEquals(4, YamaContract.SHARD_ACQUISITION.flares);
        for (YamaContract c : YamaContract.values()) {
            assertNotNull(ItemCatalog.get(c.item), c.item);
            if (c != YamaContract.FAMILIAR_ACQUISITION) { // its reward is a pet chance, not an item
                assertNotNull(ItemCatalog.get(c.reward()), c.reward());
            }
            assertEquals(c, YamaContract.byItem(c.item));
        }
        Recipe sigil = Recipes.get("make_purifying_sigil");
        assertEquals(5, sigil.inputs().size());
        assertEquals(2500, Recipes.get("make_radiant_oathplate_chest").inputs().get("aether_rune"));
        // Dossier: weights out of 1,740, every entry a real item.
        assertEquals(BossMechanics.DOSSIER_TOTAL, BossMechanics.DOSSIER_TABLE.stream().mapToInt(e -> (Integer) e[3]).sum());
        for (Object[] e : BossMechanics.DOSSIER_TABLE) {
            assertNotNull(ItemCatalog.get((String) e[0]), (String) e[0]);
        }
        assertEquals("contract_of_bloodied_blows", BossMechanics.dossierRoll(0)[0]);
        assertEquals("contract_of_oathplate_acquisition", BossMechanics.dossierRoll(1739)[0]);
    }
}
