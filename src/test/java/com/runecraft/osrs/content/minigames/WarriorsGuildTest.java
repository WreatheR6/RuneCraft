package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.minigames.MinigameRules.WarriorsGuild;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Warriors' Guild rules against the OSRS Wiki (round 40). */
class WarriorsGuildTest {
    @Test
    void animatedArmourMatchesTheInfoboxes() {
        // Rune: level 138, 120 hitpoints, max hit 14, 40 tokens; adamant max hit 12; mithril 10.
        assertEquals(120, WarriorsGuild.ARMOUR[6][2]);
        assertEquals(12, WarriorsGuild.ARMOUR[5][3]);
        assertEquals(10, WarriorsGuild.ARMOUR[4][3]);
        MonsterDef rune = ContentRegistry.monster("animated_rune_armour");
        assertEquals(120, rune.hitpoints);
        assertTrue(rune.immuneTo.contains(AttackType.RANGED) && rune.immuneTo.contains(AttackType.MAGIC), "melee only");
        assertEquals(AttackType.CRUSH, ContentRegistry.monster("animated_bronze_armour").attacks.get(0).style());
        assertEquals(AttackType.STAB, ContentRegistry.monster("animated_black_armour").attacks.get(0).style());
        // Pieces back: bronze 9/10 each, iron's legs and steel's helm 9/10, black and up always.
        assertEquals(9, WarriorsGuild.returnTenths("bronze", "platebody"));
        assertEquals(9, WarriorsGuild.returnTenths("iron", "platelegs"));
        assertEquals(10, WarriorsGuild.returnTenths("iron", "full_helm"));
        assertEquals(9, WarriorsGuild.returnTenths("steel", "full_helm"));
        assertEquals(10, WarriorsGuild.returnTenths("rune", "platelegs"));
        assertEquals(40, WarriorsGuild.tokens("rune"));
    }

    @Test
    void cyclopesAndDefenders() {
        // Top floor: the next defender up to rune (rune again once shown); the basement: the dragon defender.
        assertEquals(0, WarriorsGuild.targetDefender(-1, false));
        assertEquals(WarriorsGuild.RUNE, WarriorsGuild.targetDefender(5, false));
        assertEquals(WarriorsGuild.RUNE, WarriorsGuild.targetDefender(WarriorsGuild.RUNE, false));
        assertEquals(WarriorsGuild.RUNE, WarriorsGuild.targetDefender(WarriorsGuild.DRAGON, false));
        assertEquals(WarriorsGuild.DRAGON, WarriorsGuild.targetDefender(WarriorsGuild.RUNE, true));
        assertEquals(50, WarriorsGuild.defenderChance(3));
        assertEquals(100, WarriorsGuild.defenderChance(WarriorsGuild.DRAGON));
        assertEquals(WarriorsGuild.RUNE, WarriorsGuild.defenderTier("rune_defender_t"));
        assertEquals(WarriorsGuild.DRAGON, WarriorsGuild.defenderTier("avernic_defender"));
        assertEquals(2, WarriorsGuild.defenderTier("steel_defender"));
        assertEquals(-1, WarriorsGuild.defenderTier("rune_kiteshield"));
        // Levels 56 and 76 on the top floor and 106 in the basement, all melee only.
        for (String id : new String[]{"warriors_guild_cyclops", WarriorsGuild.CYCLOPS_76, "cyclops_elite"}) {
            MonsterDef c = ContentRegistry.monster(id);
            assertNotNull(c, id);
            assertTrue(c.immuneTo.contains(AttackType.RANGED), id);
        }
        assertEquals(76, ContentRegistry.monster(WarriorsGuild.CYCLOPS_76).combatLevel);
        assertEquals(100, ContentRegistry.monster(WarriorsGuild.CYCLOPS_76).hitpoints);
        assertEquals(100, WarriorsGuild.CYCLOPS_GRACE_TICKS);
        for (String entrance : new String[]{WarriorsGuild.BASEMENT_ENTRANCE, WarriorsGuild.DUMMY_ENTRANCE, WarriorsGuild.CATAPULT_ENTRANCE,
                WarriorsGuild.SHOT_PUT_ENTRANCE, WarriorsGuild.KEG_ENTRANCE}) {
            assertEquals(Minigame.WARRIORS_GUILD, MinigameWorld.EXTRA_ENTRANCES.get(entrance), entrance);
        }
    }

    @Test
    void dummiesCatapultShotPutAndKegs() {
        assertEquals(7, WarriorsGuild.DUMMIES.size());
        assertTrue(WarriorsGuild.dummyHit("controlled", "Controlled", "STAB"));
        assertTrue(WarriorsGuild.dummyHit("stab", "Aggressive", "STAB"));
        assertFalse(WarriorsGuild.dummyHit("crush", "Accurate", "SLASH"));
        assertFalse(WarriorsGuild.dummyHit("defensive", "Accurate", "CRUSH"));
        assertEquals(15, WarriorsGuild.DUMMY_XP);
        assertEquals(2, WarriorsGuild.DUMMY_TOKENS);
        assertEquals(4, WarriorsGuild.CATAPULT_AMMO.size());
        assertEquals(10, WarriorsGuild.CATAPULT_XP);
        assertEquals(8, WarriorsGuild.CATAPULT_INTERVAL);
        // The wiki's best throw: 99 Strength, full energy, standing, dusted hands, 18lb = 14 yards for 151.2 XP.
        int x = WarriorsGuild.shotX(99, 100, "standing", true, 18);
        assertEquals(206, x);
        assertEquals(14, WarriorsGuild.shotYards(x));
        assertEquals(151.2, WarriorsGuild.shotXp(14, x), 1e-9);
        assertEquals(15, WarriorsGuild.shotTokens(14, 18));
        assertEquals(17, WarriorsGuild.shotTokens(14, 22));
        assertTrue(WarriorsGuild.shotYards(WarriorsGuild.shotX(50, 10, "spin", false, 22)) >= 1);
        assertEquals(0.0, WarriorsGuild.kegFallChance(100, 5), 1e-9);
        assertTrue(WarriorsGuild.kegFallChance(20, 5) > WarriorsGuild.kegFallChance(60, 5));
        assertEquals(5, WarriorsGuild.MAX_KEGS);
    }
}
