package com.runecraft.osrs.content.godwars;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.HybridStyles;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Round 44 God Wars numbers against the OSRS Wiki infoboxes and osrs-dps-calc's monsters.json. */
class GwdRound44Test {
    @Test
    void ecumenicalKeyCapFollowsTheWildernessDiary() {
        // OSRS Wiki, Ecumenical key: three, four with the medium Wilderness Diary, five with the hard one.
        assertEquals(3, GodWarsFactions.ecumenicalKeyCap(false, false));
        assertEquals(4, GodWarsFactions.ecumenicalKeyCap(true, false));
        assertEquals(5, GodWarsFactions.ecumenicalKeyCap(true, true));
        assertEquals(5, GodWarsFactions.ecumenicalKeyCap(false, true));
    }

    @Test
    void goraksTouchDrainsOneToFourOfAnyStatButHitpoints() {
        Set<Skill> seen = new HashSet<>();
        for (int s = 0; s < Skill.count() * 2; s++) {
            for (int d = 0; d < 8; d++) {
                GodWarsFactions.GorakDrain drain = GodWarsFactions.goraksTouch(s, d);
                assertFalse(drain.skill() == Skill.HITPOINTS);
                assertTrue(drain.amount() >= 1 && drain.amount() <= 4, "drain " + drain.amount());
                seen.add(drain.skill());
            }
        }
        assertEquals(Skill.count() - 1, seen.size(), "every skill but Hitpoints can be drained");
        assertTrue(GodWarsFactions.gorak("gwd_gorak"));
        assertFalse(GodWarsFactions.gorak("gwd_vampire"));
    }

    @Test
    void bloodReaversHealTheirAttackerHalfTheDamage() {
        // OSRS Wiki, Blood Reaver: "Damaging a Blood Reaver heals the player for 50% of the damage dealt. This applies to
        // Blood Reavers in both the Ancient Prison and in the Nex encounter."
        assertEquals(10, GodWarsFactions.reaverLeech("blood_reaver", 21));
        assertEquals(12, GodWarsFactions.reaverLeech("gwd_blood_reaver", 24));
        assertEquals(0, GodWarsFactions.reaverLeech("blood_reaver", 0));
        assertEquals(0, GodWarsFactions.reaverLeech("gwd_spiritual_mage_zaros", 30));
    }

    @Test
    void umbraDrainsFifteenPercentAttack() {
        assertEquals(14, GwdBosses.umbraDrain(99));
        assertEquals(17, GwdBosses.umbraDrain(118));
        assertEquals(0, GwdBosses.umbraDrain(5));
    }

    @Test
    void kreearraRollsHybridAccuracy() {
        // Magical melee: a melee roll against magic defence, blocked by Protect from Melee.
        HybridStyles.Hybrid swipe = HybridStyles.of("kreearra", GwdBosses.KREE_MELEE.name());
        assertNotNull(swipe);
        assertEquals(AttackType.SLASH, swipe.accuracy());
        assertEquals(AttackType.MAGIC, swipe.defence());
        assertEquals(AttackType.SLASH, GwdBosses.KREE_MELEE.style());
        // Ranged magic (Mod Ash): Magic accuracy against ranged defence, blocked by Protect from Magic.
        HybridStyles.Hybrid wind = HybridStyles.of("kreearra", GwdBosses.KREE_WIND.name());
        assertNotNull(wind);
        assertEquals(AttackType.MAGIC, wind.accuracy());
        assertEquals(AttackType.RANGED, wind.defence());
        assertNull(HybridStyles.of("kreearra", GwdBosses.KREE_TORNADO.name()));
        assertNull(HybridStyles.of(null, "talon swipe"));
    }

    @Test
    void nexMinionsMatchTheWikiInfoboxes() {
        // osrs-dps-calc monsters.json: level 285, 500 hitpoints, Defence and Magic 200, +150 magic defence; each weak to one
        // melee style (+25) and +50 ranged defence (Umbra +25); a barrage up to 29 every 5 ticks.
        String[] ids = GwdBosses.NEX_MINIONS;
        int[][] defences = {{25, 100, 100, 150, 50}, {100, 100, 100, 150, 25}, {100, 25, 100, 150, 50}, {100, 100, 25, 150, 50}};
        for (int i = 0; i < ids.length; i++) {
            BossDef b = ContentRegistry.boss(ids[i]);
            assertNotNull(b, ids[i]);
            assertEquals(285, b.combatLevel, ids[i]);
            assertEquals(500, b.hitpoints, ids[i]);
            assertEquals(200, b.defence, ids[i]);
            assertEquals(200, b.magic, ids[i]);
            assertEquals(5, b.attackSpeed, ids[i]);
            int[] d = {b.defenceBonus(AttackType.STAB), b.defenceBonus(AttackType.SLASH), b.defenceBonus(AttackType.CRUSH),
                    b.defenceBonus(AttackType.MAGIC), b.defenceBonus(AttackType.RANGED)};
            for (int k = 0; k < 5; k++) {
                assertEquals(defences[i][k], d[k], ids[i] + " defence " + k);
            }
            for (AttackDef a : b.attacks) {
                assertEquals(29, a.maxHit(), ids[i] + " " + a.name());
                assertEquals(AttackType.MAGIC, a.style(), ids[i] + " " + a.name());
            }
        }
    }

    private static int rarity(MonsterDef m, String item) {
        for (com.runecraft.osrs.data.Drop d : m.drops) {
            if (d.item().equals(item)) {
                return d.rarity();
            }
        }
        return -1;
    }

    @Test
    void prisonFollowersDropTheWikiZarosTables() {
        // OSRS Wiki: the Zaros versions of the spiritual creatures and the Ancient Prison's blood reavers.
        for (String id : new String[]{"gwd_spiritual_warrior_zaros", "gwd_spiritual_ranger_zaros", "gwd_spiritual_mage_zaros",
                "gwd_blood_reaver"}) {
            MonsterDef m = ContentRegistry.monster(id);
            assertNotNull(m, id);
            assertEquals(-1, rarity(m, "bones"), id + ": spiritual creatures and reavers leave no bones");
            for (String piece : new String[]{"mask", "top", "legs", "gloves", "boots"}) {
                assertEquals(640, rarity(m, "ancient_ceremonial_" + piece), id + " " + piece);
            }
            assertEquals(128, rarity(m, "nihil_shard"), id + " nihil shards 1/128");
        }
        assertEquals(128, rarity(ContentRegistry.monster("gwd_spiritual_mage_zaros"), "dragon_boots"));
        assertEquals(14, rarity(ContentRegistry.monster("gwd_spiritual_mage_zaros"), "grimy_torstol"));
        assertEquals(68, rarity(ContentRegistry.monster("gwd_spiritual_warrior_zaros"), "grimy_torstol"));
        assertEquals(1, rarity(ContentRegistry.monster("gwd_blood_reaver"), "malicious_ashes"));
        assertEquals(112, rarity(ContentRegistry.monster("gwd_blood_reaver"), "clue_scroll_hard"));
        assertEquals(128, rarity(ContentRegistry.monster("gwd_spiritual_ranger_zaros"), "shark"));
    }

    @Test
    void nexAndHerReaversMatchTheWiki() {
        BossDef nex = ContentRegistry.boss("nex");
        assertNotNull(nex);
        assertEquals(3400, nex.hitpoints);
        assertEquals(40, nex.defenceBonus(AttackType.STAB));
        assertEquals(140, nex.defenceBonus(AttackType.SLASH));
        assertEquals(60, nex.defenceBonus(AttackType.CRUSH));
        assertEquals(300, nex.defenceBonus(AttackType.MAGIC));
        assertEquals(190, nex.defenceBonus(AttackType.RANGED));
        MonsterDef reaver = ContentRegistry.monster("blood_reaver");
        assertNotNull(reaver);
        assertEquals(161, reaver.combatLevel);
        assertEquals(150, reaver.hitpoints);
        MonsterDef prison = ContentRegistry.monster("gwd_blood_reaver");
        assertNotNull(prison);
        assertEquals(174, prison.combatLevel);
        assertEquals(125, prison.hitpoints);
        MonsterDef gorak = ContentRegistry.monster("gwd_gorak");
        assertNotNull(gorak);
        assertEquals(149, gorak.combatLevel);
        assertEquals(128, gorak.hitpoints);
        assertEquals(14, gorak.attacks.get(0).maxHit());
        assertEquals(AttackDef.Special.PRAYER_PIERCING, gorak.attacks.get(0).special());
    }
}
