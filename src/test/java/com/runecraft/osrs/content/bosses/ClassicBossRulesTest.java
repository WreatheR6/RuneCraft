package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Round 36: the classic bosses' rules against the OSRS Wiki (Corp, Kalphite Queen, Sarachnis, Scurrius, Hespori, Zulrah, Vorkath). */
class ClassicBossRulesTest {
    @Test
    void corpTakesFullDamageOnlyFromMagicAndCorpbaneStabs() {
        // OSRS Wiki "Corpbane weapons": spears and halberds enabled against it, Osmumten's fang, thunder khopesh, King's barrage.
        for (String w : new String[]{"dragon_spear", "zamorakian_spear", "guthans_warspear", "leaf_bladed_spear", "rune_halberd",
                "dragon_halberd", "noxious_halberd", "osmumtens_fang", "bone_spear", "rune_spear_p"}) {
            assertTrue(ClassicBosses.corpbane(w), w);
        }
        for (String w : new String[]{"dragon_hunter_lance", "rune_hasta", "zamorakian_hasta", "blue_moon_spear", "abyssal_whip",
                "crystal_halberd_basic", "", "tarnished_spear"}) {
            assertFalse(ClassicBosses.corpbane(w), w);
        }
        assertEquals(40, ClassicBosses.corpDamage(40, AttackType.STAB, "zamorakian_spear"));
        assertEquals(20, ClassicBosses.corpDamage(40, AttackType.SLASH, "zamorakian_spear"), "a spear on slash is halved");
        assertEquals(20, ClassicBosses.corpDamage(41, AttackType.STAB, "abyssal_dagger"), "rounded down");
        assertEquals(40, ClassicBosses.corpDamage(40, AttackType.MAGIC, "kodai_wand"), "magic deals full damage (wiki)");
        assertEquals(20, ClassicBosses.corpDamage(40, AttackType.RANGED, "twisted_bow"));
        // Protect from Magic blocks only a third of its magic.
        assertEquals(44, ClassicBosses.corpMagicThroughPrayer(65));
        assertEquals(0, ClassicBosses.corpMagicThroughPrayer(0));
    }

    @Test
    void corpAttacksFollowTheWikiWeights() {
        BossDef corp = ContentRegistry.boss("corporeal_beast");
        // In melee range: melee 40%, each magic attack 20% (2:1:1:1); out of range a third each.
        int melee = 0;
        int magic = 0;
        for (AttackDef a : corp.attacks) {
            if (a.melee()) {
                melee += a.weight();
                assertTrue(a.range() < 4, "melee only in reach");
            } else {
                magic += a.weight();
                assertEquals(AttackDef.Special.PRAYER_PIERCING, a.special(), a.name() + ": the script takes a third off under prayer");
            }
        }
        assertEquals(2, melee);
        assertEquals(3, magic);
        assertEquals(33, ClassicBosses.CORP_MELEE.maxHit());
        assertEquals(65, ClassicBosses.CORP_ORB.maxHit());
        assertEquals(55, ClassicBosses.CORP_DRAIN.maxHit());
        assertEquals(40, ClassicBosses.CORP_SPLASH_DIRECT);
        assertEquals(30, ClassicBosses.CORP_SPLINTER_DIRECT);
        assertEquals(6, ClassicBosses.CORP_SPLINTERS);
        assertEquals(30, ClassicBosses.CORP_STOMP_MIN);
        assertEquals(51, ClassicBosses.CORP_STOMP.maxHit());
        assertEquals(7, ClassicBosses.CORP_STOMP_TICKS);
        assertTrue(corp.phases.isEmpty(), "the core is rolled by the script, not a phase");
    }

    @Test
    void kalphiteQueenRangedAndMagicAlwaysHit() {
        for (AttackDef a : new AttackDef[]{ClassicBosses.KQ_SPINES, ClassicBosses.KQ_MAGIC, ClassicBosses.KQ_CHAIN_MAGIC}) {
            assertEquals(AttackDef.Special.AOE, a.special(), a.name());
            assertEquals(31, a.maxHit());
        }
        assertEquals(20, ClassicBosses.KQ_TRANSFORM_TICKS);
        assertEquals(1, ClassicBosses.KQ_SPINE_PRAYER_DRAIN);
    }

    @Test
    void sarachnisAndHerSpawns() {
        BossDef s = ContentRegistry.boss("sarachnis");
        assertEquals(2, s.attacks.size());
        assertEquals(31, ClassicBosses.SARACHNIS_MELEE.maxHit());
        assertEquals(31, ClassicBosses.SARACHNIS_RANGED.maxHit());
        assertEquals(4, ClassicBosses.SARACHNIS_WEB_EVERY);
        assertEquals(6, ClassicBosses.SARACHNIS_WEB_BIND_TICKS);
        MonsterDef melee = ContentRegistry.monster(ClassicBosses.SARACHNIS_SPAWN_MELEE);
        MonsterDef mage = ContentRegistry.monster(ClassicBosses.SARACHNIS_SPAWN_MAGE);
        assertNotNull(melee);
        assertNotNull(mage);
        assertEquals(107, melee.combatLevel);
        assertEquals(68, mage.combatLevel);
        assertEquals(30, melee.hitpoints);
        assertEquals(30, mage.hitpoints);
        assertEquals(13, melee.maxHit());
        assertEquals(11, mage.maxHit());
        assertEquals(AttackType.MAGIC, mage.primaryStyle());
        assertEquals(AttackType.CRUSH, melee.primaryStyle());
    }

    @Test
    void scurriusIsTheSoloInstanceVersion() {
        BossDef s = ContentRegistry.boss("scurrius");
        assertEquals(500, s.hitpoints);
        assertEquals(200, s.combatLevel);
        assertEquals(13, ClassicBosses.SCURRIUS_TAIL.maxHit());
        assertEquals(8, ClassicBosses.SCURRIUS_BOLTS.maxHit());
        assertEquals(7, ClassicBosses.SCURRIUS_FUR.maxHit());
        assertEquals(22, ClassicBosses.SCURRIUS_BRICKS.maxHit());
        assertEquals(15, ClassicBosses.SCURRIUS_BRICKS_MIN);
        assertEquals(6, ClassicBosses.SCURRIUS_RATS);
        assertTrue(s.phases.isEmpty(), "the script runs his food piles and the centre");
    }

    @Test
    void hesporiIsCutNotBlockedByPrayer() {
        assertEquals(2, ClassicBosses.hesporiPrayed(8, AttackType.RANGED, 8));
        assertEquals(1, ClassicBosses.hesporiPrayed(4, AttackType.RANGED, 8));
        assertEquals(3, ClassicBosses.hesporiPrayed(14, AttackType.MAGIC, 14));
        assertEquals(0, ClassicBosses.hesporiPrayed(0, AttackType.MAGIC, 14));
    }

    @Test
    void zulrahReachAndClouds() {
        assertTrue(ZulrahVorkath.inCloud(2.9, 0, -2.9));
        assertFalse(ZulrahVorkath.inCloud(3.5, 0, 0));
        assertTrue(ContentRegistry.boss("zulrah").immuneTo.isEmpty());
        assertTrue(ContentRegistry.boss("zulrah_tanzanite").immuneTo.isEmpty());
    }

    @Test
    void skotizoShardsAndHardClue() {
        int[] counts = new int[6];
        int total = 0;
        for (int r = 0; r < 100; r++) {
            int n = ClassicBosses.skotizoShards(r);
            counts[n]++;
            total += n;
        }
        assertEquals(75, counts[1]);
        assertEquals(15, counts[2]);
        assertEquals(5, counts[3]);
        assertEquals(4, counts[4]);
        assertEquals(1, counts[5]);
        assertEquals(141, total, "1.41 shards a kill (wiki)");
        assertTrue(ContentRegistry.boss("skotizo").drops.stream()
                .anyMatch(d -> d.item().equals("clue_scroll_hard") && d.rarity() == 1), "a hard clue on every kill");
    }

    @Test
    void vorkathBombHeadAndQuestVersion() {
        assertEquals(121, ZulrahVorkath.bombMax(121, true));
        assertEquals(60, ZulrahVorkath.bombMax(121, false));
        assertEquals(50, ZulrahVorkath.HEAD_KILL);
        assertEquals(30, ZulrahVorkath.spawnExplosion(38, 38, ZulrahVorkath.DS2_SPAWN_EXPLOSION_MAX));
        assertEquals(60, ZulrahVorkath.spawnExplosion(38, 38));
        assertEquals(20, ZulrahVorkath.SPAWN_DISTANCE_BLOCKS);
    }

    @Test
    void mimicMatchesTheWikiInfobox() {
        BossDef m = ContentRegistry.boss("the_mimic");
        assertEquals(230, m.hitpoints);
        assertEquals(185, m.attack);
        assertEquals(120, m.strength);
        assertEquals(120, m.defence);
        assertEquals(3, m.attackSpeed);
        assertEquals(135, m.attackBonus(AttackType.CRUSH));
        assertEquals(180, m.attackBonus(AttackType.MAGIC));
        assertEquals(165, m.defenceBonus(AttackType.SLASH));
        assertEquals(30, m.defenceBonus(AttackType.MAGIC), "weak to magic");
        assertEquals(23, m.attacks.stream().mapToInt(a -> a.maxHit()).max().orElse(0), "max hit 23");
    }
}
