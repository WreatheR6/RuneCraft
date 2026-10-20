package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.quests.QuestSpec.Trigger;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Round 33 review fixes: the Balance Elemental's thresholds, moved quest NPCs, Lady Vulcana and the Champion of Scabaras. */
class QuestRound33RepairTest {

    @Test
    void balanceElementalFollowsTheWikiThresholds() {
        BossDef balance = ContentRegistry.boss("balance_elemental");
        assertEquals(BalanceElemental.HITPOINTS, balance.hitpoints);
        assertEquals(6, balance.attackSpeed);
        // the phases announce the thresholds: 550 and 200 of 750 HP
        assertEquals(2, balance.phases.size());
        assertEquals(Math.round(100f * BalanceElemental.ONE_PRAYER_BELOW / BalanceElemental.HITPOINTS), balance.phases.get(0).belowPercent());
        assertEquals(Math.round(100f * BalanceElemental.TWO_PRAYERS_BELOW / BalanceElemental.HITPOINTS), balance.phases.get(1).belowPercent());
        for (BossDef.Phase p : balance.phases) {
            assertEquals(1.0, p.damageMultiplier(), "it attacks faster, not harder");
        }
        // attack speed 6, then 5 below 550 HP and 2 below 200 HP
        assertEquals(6, BalanceElemental.attackSpeed(750));
        assertEquals(6, BalanceElemental.attackSpeed(550));
        assertEquals(5, BalanceElemental.attackSpeed(549));
        assertEquals(5, BalanceElemental.attackSpeed(200));
        assertEquals(2, BalanceElemental.attackSpeed(199));
        // each form's attack exists, with the wiki's style
        for (int form = 0; form < 3; form++) {
            String name = BalanceElemental.FORM_ATTACKS[form];
            int f = form;
            assertTrue(balance.attacks.stream().anyMatch(a -> a.name().equals(name) && BalanceElemental.family(a.style()) == f), name);
        }
        // no prayers above 550 HP
        for (int form = 0; form < 3; form++) {
            for (int style = 0; style < 3; style++) {
                assertFalse(BalanceElemental.protects(form, style, 550));
            }
        }
        // one prayer below 550 HP (wiki table): fiery (ranged) prays melee, airy (melee) magic, watery (magic) ranged
        assertTrue(BalanceElemental.protects(BalanceElemental.RANGED, BalanceElemental.MELEE, 549));
        assertTrue(BalanceElemental.protects(BalanceElemental.MELEE, BalanceElemental.MAGIC, 549));
        assertTrue(BalanceElemental.protects(BalanceElemental.MAGIC, BalanceElemental.RANGED, 549));
        for (int form = 0; form < 3; form++) {
            int blocked = 0;
            for (int style = 0; style < 3; style++) {
                blocked += BalanceElemental.protects(form, style, 300) ? 1 : 0;
            }
            assertEquals(1, blocked, "one protection prayer");
        }
        // two prayers below 200 HP: one style hurts each form, a different one per form, so all three styles are needed
        Set<Integer> open = new HashSet<>();
        for (int form = 0; form < 3; form++) {
            int hurts = -1;
            for (int style = 0; style < 3; style++) {
                if (!BalanceElemental.protects(form, style, 199)) {
                    assertEquals(-1, hurts, "two protection prayers");
                    hurts = style;
                }
            }
            assertEquals(BalanceElemental.vulnerableStyle(form), hurts);
            open.add(hurts);
        }
        assertEquals(3, open.size());
        assertEquals(BalanceElemental.MELEE, BalanceElemental.family(AttackType.CRUSH));
        assertEquals(BalanceElemental.RANGED, BalanceElemental.family(AttackType.RANGED));
        assertEquals(BalanceElemental.MAGIC, BalanceElemental.family(AttackType.MAGIC));
    }

    /** Where each relocated NPC stood before round 33: place (a module place id, a Landmark or "osrs:x,y") and block offset. */
    private static final Map<String, Object[]> BEFORE = Map.ofEntries(
            Map.entry("dimintheis", new Object[]{"VARROCK", 7, 6}),
            Map.entry("edmond", new Object[]{"ARDOUGNE", -7, 6}),
            Map.entry("king_lathas", new Object[]{"ARDOUGNE", 0, -7}),
            Map.entry("lokar_searunner", new Object[]{"RELLEKKA", -4, 7}),
            Map.entry("mord_gunnars", new Object[]{"RELLEKKA", 2, 8}),
            Map.entry("vestri", new Object[]{"BURTHORPE", 6, 5}),
            Map.entry("gossip_kr", new Object[]{"SEERS_VILLAGE", 6, 6}),
            Map.entry("ali_morrisane", new Object[]{"POLLNIVNEACH", 3, 3}),
            Map.entry("wanderer_sophanem", new Object[]{"sophanem", 3, 3}),
            Map.entry("akrisae", new Object[]{"osrs:2962,3382", 0, 0}), // the old temple_knight_hq spot
            Map.entry("ivy_sophista", new Object[]{"osrs:2962,3382", 3, 2}),
            Map.entry("lord_kandur_lovakengj", new Object[]{"lovakengj", 0, 3}),
            Map.entry("lazim", new Object[]{"quest_enakhras_temple", 0, 3}));

    private static int[] oldBlock(Object[] before) {
        String place = (String) before[0];
        int dx = (Integer) before[1];
        int dz = (Integer) before[2];
        if (place.startsWith("osrs:")) {
            String[] xy = place.substring(5).split(",");
            int x = ((GielinorMap.toBlockX(Integer.parseInt(xy[0])) >> 4) << 4) + 8;
            int z = ((GielinorMap.toBlockZ(Integer.parseInt(xy[1])) >> 4) << 4) + 8;
            return new int[]{x + dx, z + dz};
        }
        PlaceDef p = ContentRegistry.place(place);
        if (p != null) {
            return new int[]{p.blockX() + dx, p.blockZ() + dz};
        }
        Landmark l = Landmark.byName(place);
        assertNotNull(l, place);
        return new int[]{l.blockX() + dx, l.blockZ() + dz};
    }

    @Test
    void copiesLeftAtAnNpcsOldPlaceAreStale() {
        assertEquals(BEFORE.keySet(), QuestNpcMoves.RELOCATED);
        for (Map.Entry<String, Object[]> e : BEFORE.entrySet()) {
            NpcDef def = ContentRegistry.npc(e.getKey());
            assertNotNull(def, e.getKey());
            int[] home = QuestNpcMoves.homeBlock(def);
            assertNotNull(home, e.getKey());
            assertFalse(QuestNpcMoves.stale(def, home[0], home[1]), e.getKey() + " at its spot");
            assertFalse(QuestNpcMoves.stale(def, home[0] + QuestNpcMoves.TOLERANCE, home[1] - QuestNpcMoves.TOLERANCE),
                    e.getKey() + " a few tiles off its spot (a free floor nearby)");
            int[] old = oldBlock(e.getValue());
            assertTrue(QuestNpcMoves.stale(def, old[0], old[1]), e.getKey() + " left at its old place");
        }
        // NPCs that never moved are never removed, wherever they stand (e.g. Godric moved into his cell)
        NpcDef other = ContentRegistry.npcs().stream().filter(n -> !QuestNpcMoves.RELOCATED.contains(n.id())).findFirst().orElseThrow();
        assertFalse(QuestNpcMoves.stale(other, 100000, 100000));
    }

    @Test
    void oneLadyVulcanaInTheLovakengjAssembly() {
        List<NpcDef> vulcanas = ContentRegistry.npcs().stream().filter(n -> n.name().equals("Lady Vulcana Lovakengj")).toList();
        assertEquals(1, vulcanas.size(), "the zeah module's town copy was dropped");
        NpcDef vulcana = vulcanas.get(0);
        assertEquals("lord_kandur_lovakengj", vulcana.id(), "the Forsaken Tower's NPC");
        PlaceDef assembly = ContentRegistry.place(vulcana.place());
        assertEquals(1483, assembly.osrsX(), "wiki tile");
        assertEquals(3748, assembly.osrsY(), "wiki tile");
        QuestSpec fsk = QuestBook.get().spec("the_forsaken_tower");
        assertTrue(fsk.stages.get(0).journal.contains("Lovakengj Assembly"));
        assertFalse(fsk.doneLines.getOrDefault("lord_kandur_lovakengj", List.of()).isEmpty(), "her old zeah lines after the quest");
    }

    @Test
    void theChampionOfScabarasWaitsAtUllek() {
        QuestSpec bcs = QuestBook.get().spec("beneath_cursed_sands");
        QuestSpec.Stage fight = bcs.stages.get(2);
        Trigger raise = fight.triggers.get(0);
        assertEquals(Trigger.Kind.REACH, raise.kind);
        assertEquals("bcs_ruins_of_ullek", raise.target);
        assertEquals("champion_of_scabaras", raise.spawn);
        assertEquals(raise.stage, raise.next, "repeatable: raised again after a death or teleport");
        assertTrue(raise.setFlags.isEmpty() && raise.give.isEmpty() && raise.take.isEmpty(), "waits quietly while he is alive");
        Trigger kill = QuestWalk.advancing(fight);
        assertEquals(Trigger.Kind.KILL, kill.kind);
        assertEquals("champion_of_scabaras", kill.target);
        // the stage before raises him at Ullek too
        assertTrue(bcs.stages.get(1).triggers.stream().anyMatch(t -> t.kind == Trigger.Kind.REACH && t.target.equals("bcs_ruins_of_ullek")
                && "champion_of_scabaras".equals(t.spawn)));
        assertTrue(fight.journal.contains("Ruins of Ullek"));
    }
}
