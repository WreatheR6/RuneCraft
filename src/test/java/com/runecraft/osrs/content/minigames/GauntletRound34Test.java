package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.bosses.MinigameBosses;
import com.runecraft.osrs.content.minigames.Gauntlet.Foe;
import com.runecraft.osrs.content.minigames.Gauntlet.Mode;
import com.runecraft.osrs.content.minigames.Gauntlet.Node;
import com.runecraft.osrs.content.minigames.Gauntlet.Piece;
import com.runecraft.osrs.world.StructureDef;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round 34: the Gauntlet against the wiki for what a run walks through: gathering experience, the creatures' resource
 * drops and weapon frames, the pestle and mortar on weapon components, the Hunllef's damage through prayer and armour,
 * its tornadoes and floor, the lobby's objects and the retired summoning-stone arenas.
 */
class GauntletRound34Test {
    private static List<String[]> rows() throws Exception {
        List<String[]> rows = new ArrayList<>();
        for (String line : Files.readAllLines(Path.of("src/test/resources/osrs/minigames/gauntlet.tsv"), StandardCharsets.UTF_8)) {
            if (!line.isBlank() && !line.startsWith("#")) {
                rows.add(line.split("\t"));
            }
        }
        return rows;
    }

    @Test
    void gatheringGivesTheWikiExperience() {
        // Wiki node pages (Mining/Woodcutting/Fishing info: 10 xp; the plants' Skill info: Farming 1 xp).
        assertEquals(Skill.MINING, Node.ORE.skill);
        assertEquals(10, Node.ORE.xp);
        assertEquals(Skill.WOODCUTTING, Node.BARK.skill);
        assertEquals(10, Node.BARK.xp);
        assertEquals(Skill.FISHING, Node.FISH.skill);
        assertEquals(10, Node.FISH.xp);
        assertEquals(Skill.FARMING, Node.LINUM.skill);
        assertEquals(1, Node.LINUM.xp);
        assertEquals(Skill.FARMING, Node.GRYM.skill);
        assertEquals(1, Node.GRYM.xp);
        assertEquals(3, Node.ORE.yield);
        assertEquals(4, Node.FISH.yield);
        assertEquals(1, Node.GRYM.yield);
        // Paddlefish stop burning at 47 Cooking (wiki chart low 155, high 368).
        assertEquals(1.0, Gauntlet.cookChance(Gauntlet.PADDLEFISH_STOP_BURN), 1e-9);
        assertTrue(Gauntlet.cookChance(46) < 1.0);
        assertTrue(Gauntlet.cookChance(1) > 0.6);
    }

    @Test
    void creatureResourcesMatchTheDropLog() throws Exception {
        Map<String, Foe> tiers = Map.of("weak", Foe.WEAK, "strong", Foe.STRONG, "demi", Foe.DEMI);
        Map<String, String> keys = Map.of("Nothing", "", "Corrupted shards", Gauntlet.RES_SHARDS, "Raw paddlefish", Gauntlet.RES_FISH,
                "Grym leaf (corrupted)", Gauntlet.RES_GRYM, "Corrupted teleport crystal", Gauntlet.RES_TELEPORT);
        int seen = 0;
        for (String[] r : rows()) {
            if (r.length != 6 || !r[0].equals("Resource")) {
                continue;
            }
            Gauntlet.Table t = Gauntlet.resources(tiers.get(r[1]));
            String key = keys.get(r[2]);
            assertNotNull(key, r[2]);
            Gauntlet.Loot l = t.rows().stream().filter(x -> key.isEmpty() ? x.item() == null : key.equals(x.item())).findFirst()
                    .orElse(null);
            assertNotNull(l, r[1] + " " + r[2]);
            String[] frac = r[5].split("/");
            assertEquals(Integer.parseInt(frac[1]), t.total(), r[1] + " total");
            assertEquals(Integer.parseInt(frac[0]), l.weight(), r[1] + " " + r[2]);
            if (!key.isEmpty()) {
                assertEquals(Integer.parseInt(r[3]), l.min(), r[1] + " " + r[2]);
                assertEquals(Integer.parseInt(r[4]), l.max(), r[1] + " " + r[2]);
            }
            seen++;
        }
        assertEquals(15, seen);
        for (Foe f : Foe.values()) {
            Gauntlet.Table t = Gauntlet.resources(f);
            assertEquals(t.total(), t.rows().stream().mapToInt(Gauntlet.Loot::weight).sum(), f + " weights");
            for (Gauntlet.Loot l : t.rows()) {
                for (Mode m : Mode.values()) {
                    String item = Gauntlet.resourceItem(m, l.item());
                    assertTrue(item == null || ItemCatalog.exists(item), item);
                }
            }
        }
        assertEquals("corrupted_grym_leaf", Gauntlet.resourceItem(Mode.CORRUPTED, Gauntlet.RES_GRYM));
        assertEquals("crystal_gauntlet_teleport", Gauntlet.resourceItem(Mode.NORMAL, Gauntlet.RES_TELEPORT));
        assertNull(Gauntlet.resourceItem(Mode.NORMAL, null));
        assertEquals(Gauntlet.shards(Mode.NORMAL), Gauntlet.RES_SHARDS);
        assertEquals(Gauntlet.grym(Mode.NORMAL), Gauntlet.RES_GRYM);
        assertEquals(Gauntlet.teleport(Mode.NORMAL), Gauntlet.RES_TELEPORT);
        assertEquals(Gauntlet.RAW_PADDLEFISH, Gauntlet.resourceItem(Mode.CORRUPTED, Gauntlet.RES_FISH));
    }

    @Test
    void weaponFramesFollowTheWikiRules() {
        Random r = new Random(1);
        // The first weak creature of a run always drops one; demi-bosses always do.
        assertTrue(Gauntlet.dropsFrame(Foe.WEAK, 1, false, r));
        assertTrue(Gauntlet.dropsFrame(Foe.DEMI, 5, true, r));
        // A second strong kill guarantees one if the first gave none.
        assertTrue(Gauntlet.dropsFrame(Foe.STRONG, 2, false, r));
        int weak = 0;
        int strong = 0;
        int n = 70_000;
        for (int i = 0; i < n; i++) {
            weak += Gauntlet.dropsFrame(Foe.WEAK, 3, true, r) ? 1 : 0;
            strong += Gauntlet.dropsFrame(Foe.STRONG, 3, true, r) ? 1 : 0;
        }
        assertEquals(0.25, weak / (double) n, 0.01);
        assertEquals(2 / 7.0, strong / (double) n, 0.01);
    }

    @Test
    void componentsCrushIntoShards() {
        assertEquals(80, Gauntlet.COMPONENT_SHARDS);
        for (Mode m : Mode.values()) {
            for (Piece p : new Piece[]{Piece.HALBERD, Piece.STAFF, Piece.BOW}) {
                assertEquals(p, Gauntlet.pieceOfComponent(m, Gauntlet.component(m, p)));
            }
            assertNull(Gauntlet.pieceOfComponent(m, Gauntlet.frame(m)));
        }
        assertNull(Gauntlet.pieceOfComponent(Mode.NORMAL, Gauntlet.component(Mode.CORRUPTED, Piece.BOW)));
    }

    @Test
    void hunllefDamageThroughPrayerAndArmour() {
        // Unprayed max hits are the NPC formula's ((240 + 9) * (bonus + 64) + 320) / 640: 50 and 68 (wiki infoboxes).
        assertEquals(50, ((240 + 9) * (64 + 64) + 320) / 640);
        assertEquals(68, ((240 + 9) * (112 + 64) + 320) / 640);
        assertEquals(Gauntlet.hunllefUnprayedMax(Mode.NORMAL), MinigameBosses.HUNLLEF_RANGED.maxHit());
        assertEquals(Gauntlet.hunllefUnprayedMax(Mode.NORMAL), MinigameBosses.HUNLLEF_MAGIC.maxHit());
        assertEquals(Gauntlet.hunllefUnprayedMax(Mode.CORRUPTED), MinigameBosses.CORRUPTED_RANGED.maxHit());
        assertEquals(Gauntlet.hunllefUnprayedMax(Mode.CORRUPTED), MinigameBosses.CORRUPTED_MAGIC.maxHit());
        assertEquals(com.runecraft.osrs.content.AttackDef.Special.PRAYER_PIERCING, MinigameBosses.HUNLLEF_RANGED.special());
        // Through the right prayer the top roll becomes the wiki's 12 / 16; armour then lowers it to 10/8/6 and 13/10/8.
        assertEquals(Gauntlet.hunllefMaxHit(Mode.NORMAL, 0), MinigameBosses.HUNLLEF_PRAYED_MAX);
        assertEquals(Gauntlet.hunllefMaxHit(Mode.CORRUPTED, 0), MinigameBosses.CORRUPTED_PRAYED_MAX);
        assertEquals(12, Gauntlet.prayedDamage(Mode.NORMAL, 50));
        assertEquals(16, Gauntlet.prayedDamage(Mode.CORRUPTED, 68));
        assertEquals(0, Gauntlet.prayedDamage(Mode.CORRUPTED, 0));
        // Unprayed with full T1 corrupted armour: 55 (wiki 55).
        assertEquals(55, (int) Math.round(68 * Gauntlet.hunllefDamageScale(Mode.CORRUPTED, 3)));
        // Tornadoes: 1/2/3 (crystalline) and 2/3/4 (corrupted) by health, for 20 ticks, 10-20 / 15-30 without armour.
        assertEquals(1, Gauntlet.tornadoes(Mode.NORMAL, 100));
        assertEquals(2, Gauntlet.tornadoes(Mode.NORMAL, 50));
        assertEquals(3, Gauntlet.tornadoes(Mode.NORMAL, 10));
        assertEquals(2, Gauntlet.tornadoes(Mode.CORRUPTED, 90));
        assertEquals(4, Gauntlet.tornadoes(Mode.CORRUPTED, 20));
        assertEquals(20, Gauntlet.TORNADO_TICKS);
        assertEquals(30, Gauntlet.tornadoDamage(Mode.CORRUPTED)[1]);
        assertEquals(10, Gauntlet.tornadoDamage(Mode.NORMAL)[0]);
        // Floor phases by thirds of the Hunllef's health (1000-667, 666-333, 332-).
        assertEquals(0, Gauntlet.floorPhase(66.7));
        assertEquals(1, Gauntlet.floorPhase(66.6));
        assertEquals(1, Gauntlet.floorPhase(33.3 + 0.01));
        assertEquals(2, Gauntlet.floorPhase(33.2));
        assertTrue(Gauntlet.FLOOR_WARN_TICKS[0] > Gauntlet.FLOOR_WARN_TICKS[2], "the tiles turn orange faster later on");
    }

    @Test
    void hunllefsAreOnlyFoughtInTheGauntlet() {
        for (Mode m : Mode.values()) {
            BossDef b = ContentRegistry.boss(m.boss);
            assertNotNull(b, m.boss);
            assertTrue(b.raidOnly, m.boss + " still has a summoning-stone arena");
        }
        // Wiki infobox: the corrupted dragon's magic attack bonus is +146 (the other corrupted demi-bosses +81).
        MonsterDef dragon = ContentRegistry.monsters().stream().filter(x -> x.id.equals("corrupted_dragon")).findFirst().orElseThrow();
        assertEquals(146, dragon.magicAttackBonus);
        MonsterDef bear = ContentRegistry.monsters().stream().filter(x -> x.id.equals("corrupted_bear")).findFirst().orElseThrow();
        assertEquals(81, bear.meleeAttackBonus);
    }

    @Test
    void theLobbyHoldsItsObjectsAtTheirTiles() {
        StructureDef lobby = ContentRegistry.structures().stream().filter(s -> s.id().equals(GauntletLobby.STRUCTURE)).findFirst()
                .orElse(null);
        assertNotNull(lobby, "no Gauntlet lobby structure");
        Map<String, int[]> wants = Map.of(Gauntlet.REWARD_CHEST, GauntletLobby.REWARD_CHEST, Gauntlet.TELEPORT_PLATFORM,
                GauntletLobby.PLATFORM, Minigame.GAUNTLET.entranceBlock(), GauntletLobby.ENTRANCE, Gauntlet.CORRUPTED_ENTRANCE,
                GauntletLobby.CORRUPTED_ENTRANCE, Gauntlet.RECIPES_BOOK, GauntletLobby.RECIPES, Gauntlet.SCOREBOARD,
                GauntletLobby.SCOREBOARD, Gauntlet.EGNIOL_BOOK, GauntletLobby.EGNIOL_BOOK);
        for (Map.Entry<String, int[]> e : wants.entrySet()) {
            int x = StructureDef.bx(e.getValue()[0]);
            int z = StructureDef.bz(e.getValue()[1]);
            boolean found = lobby.ops().stream().anyMatch(op -> op.type() == StructureDef.Op.Type.FILL
                    && ("runecraft:" + e.getKey()).equals(op.block()) && op.x1() == x && op.z1() == z && op.y1() == GauntletLobby.GROUND);
            assertTrue(found, e.getKey() + " not at " + e.getValue()[0] + "," + e.getValue()[1]);
        }
        // Wiki: the reward chest's pin and the cache's objects (deposit box, scoreboard, platform, bookstands).
        assertEquals(3027, GauntletLobby.REWARD_CHEST[0]);
        assertEquals(6123, GauntletLobby.REWARD_CHEST[1]);
        assertEquals(3032, GauntletLobby.PLATFORM[0]);
        assertEquals(6118, GauntletLobby.RECIPES[1]);
        // Prifddinas only keeps the portal (wiki map 3228, 6116); everything else stands in the lobby.
        assertEquals(List.of(Gauntlet.PORTAL), MinigameWorld.placeBlocks().get(Minigame.GAUNTLET.place()));
        assertEquals(3228, Minigame.GAUNTLET.entranceX());
        assertEquals(6116, Minigame.GAUNTLET.entranceY());
        for (String[] b : Gauntlet.blocks()) {
            assertTrue(ContentRegistry.blocks().stream().anyMatch(d -> d.id().equals(b[0])), b[0]);
        }
        // The hall is sealed under the surface: its top stays below the ground there.
        assertTrue(GauntletLobby.GROUND + GauntletLobby.HEIGHT + 5 < 62, "the lobby needs rock above it");
        assertFalse(Gauntlet.runItems().contains(Gauntlet.REWARD_CHEST));
        assertTrue(Gauntlet.runItems().stream().noneMatch(id -> id.toLowerCase(Locale.ROOT).contains("seed")));
    }
}
