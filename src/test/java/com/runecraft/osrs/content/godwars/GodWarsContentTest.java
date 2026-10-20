package com.runecraft.osrs.content.godwars;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Recipes;
import com.runecraft.osrs.Region;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.godwars.GodWarsFactions.God;
import com.runecraft.osrs.data.Drop;
import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GodWarsContentTest {
    private final GodWarsWildernessContent module = new GodWarsWildernessContent();

    private static BossDef boss(String id) {
        BossDef b = ContentRegistry.boss(id);
        assertNotNull(b, id);
        return b;
    }

    private static int rarity(BossDef b, String item) {
        for (Drop d : b.drops) {
            if (d.item().equals(item)) {
                return d.rarity();
            }
        }
        return -1;
    }

    private static int maxHit(BossDef b, String attack) {
        for (AttackDef a : b.attacks) {
            if (a.name().equals(attack)) {
                return a.maxHit();
            }
        }
        return -1;
    }

    private static long count(ModelSpec spec, Role role) {
        return spec.parts().stream().filter(p -> p.role == role).count();
    }

    @Test
    void everyFollowerExistsAndEveryGwdMonsterGivesKillcount() {
        for (Map.Entry<String, God> e : GodWarsFactions.followers().entrySet()) {
            assertNotNull(ContentRegistry.monster(e.getKey()), "follower " + e.getKey());
        }
        for (MonsterDef m : module.monsters()) {
            if (m.id.startsWith("gwd_")) {
                assertTrue(GodWarsFactions.followerGod(m.id) != null || GodWarsFactions.zarosEssence(m.id) > 0,
                        m.id + " gives no kill count");
            }
        }
        for (God god : God.values()) {
            assertTrue(GodWarsFactions.followers().containsValue(god), god + " has followers");
            assertTrue(GodWarsFactions.generals().containsValue(god), god + " has a general");
        }
        assertEquals(God.ARMADYL, GodWarsFactions.generalGod("kreearra"));
        assertEquals(God.BANDOS, GodWarsFactions.generalGod("general_graardor"));
        assertNull(GodWarsFactions.followerGod("revenant_knight"));
    }

    @Test
    void killcountEntryCosts40() {
        assertEquals(40, GodWarsFactions.REQUIRED_KILLCOUNT);
        assertEquals(-1, GodWarsFactions.afterEntry(39));
        assertEquals(0, GodWarsFactions.afterEntry(40));
        assertEquals(17, GodWarsFactions.afterEntry(57));
    }

    @Test
    void petTopUpGivesTheRealRate() {
        for (int rate : new int[]{300, 500, 1000, 1500, 2016, 2800}) {
            double p = GodWarsFactions.extraPetChance(rate);
            double combined = 1 - (1 - 1.0 / GodWarsFactions.ENGINE_PET_RATE) * (1 - p);
            assertEquals(1.0 / rate, combined, 1e-12, "rate " + rate);
        }
        assertEquals(0, GodWarsFactions.extraPetChance(5000));
        assertEquals(0, GodWarsFactions.extraPetChance(3000));
    }

    @Test
    void kreearraIsFaithful() {
        BossDef kree = boss("kreearra");
        assertEquals(580, kree.combatLevel);
        assertEquals(255, kree.hitpoints);
        assertEquals(3, kree.attackSpeed);
        assertEquals(69, maxHit(kree, "tornado")); // wiki infobox: 69 (ranged)
        assertEquals(21, maxHit(kree, "wind blast"));
        assertEquals(25, maxHit(kree, "talon swipe")); // wiki infobox: 25 (Melee)
        assertTrue(kree.immuneTo.contains(AttackType.SLASH) && !kree.immuneTo.contains(AttackType.RANGED));
        for (String piece : new String[]{"armadyl_helmet", "armadyl_chestplate", "armadyl_chainskirt"}) {
            assertEquals(381, rarity(kree, piece));
        }
        assertEquals(508, rarity(kree, "armadyl_hilt"));
        assertEquals(762, rarity(kree, "godsword_shard_2"));
        assertEquals("pet_kreearra", kree.pet);
        assertTrue(kree.flying);
    }

    @Test
    void zilyanaIsFaithful() {
        BossDef z = boss("commander_zilyana");
        assertEquals(596, z.combatLevel);
        assertEquals(2, z.attackSpeed);
        assertEquals(27, maxHit(z, "saradomin sword")); // wiki infobox: 27 (melee)
        assertEquals(AttackType.CRUSH, GwdBosses.ZILY_MELEE.style()); // wiki infobox: attack style Crush, Magic
        assertEquals(AttackType.MAGIC, GwdBosses.ZILY_MAGIC.style());
        assertEquals(20, maxHit(z, "saradomin strike"));
        assertEquals(127, rarity(z, "saradomin_sword"));
        assertEquals(254, rarity(z, "saradomins_light"));
        assertEquals(508, rarity(z, "armadyl_crossbow"));
        assertEquals(508, rarity(z, "saradomin_hilt"));
    }

    @Test
    void nexHasFivePhasesAndFourGenerals() {
        BossDef nex = boss("nex");
        assertEquals(1001, nex.combatLevel);
        assertEquals(3400, nex.hitpoints);
        assertEquals(4, nex.phases.size());
        int[] thresholds = {80, 60, 40, 20};
        for (int i = 0; i < 4; i++) {
            assertEquals(thresholds[i], nex.phases.get(i).belowPercent());
            assertFalse(nex.phases.get(i).attacks().isEmpty());
            BossDef minion = boss(GwdBosses.NEX_MINIONS[i]);
            assertTrue(minion.raidOnly, minion.id + " must not get a summoning stone");
            assertTrue(nex.phases.get(i).message().startsWith(minion.name), nex.phases.get(i).message());
        }
        assertEquals(258, rarity(nex, "torva_platebody"));
        assertEquals(172, rarity(nex, "zaryte_vambraces"));
        assertEquals(516, rarity(nex, "ancient_hilt"));
        assertEquals(500, WildernessBosses.realPetRate("nex"));
        assertNotNull(ContentRegistry.monster(nex.minion));
    }

    @Test
    void builtInGeneralsAreNotDuplicatedButGetModelsAndExtras() {
        assertNull(ContentRegistry.boss("general_graardor"));
        assertNull(ContentRegistry.boss("kril_tsutsaroth"));
        assertNotNull(com.runecraft.osrs.data.Boss.byEntity("general_graardor"));
        assertTrue(ContentRegistry.models().containsKey("general_graardor"));
        assertTrue(ContentRegistry.models().containsKey("kril_tsutsaroth"));
        assertEquals(508, GwdBosses.builtinExtraDrops("general_graardor").get(0).rarity());
        assertEquals("bandos_hilt", GwdBosses.builtinExtraDrops("general_graardor").get(0).item());
        assertEquals("zamorak_hilt", GwdBosses.builtinExtraDrops("kril_tsutsaroth").get(0).item());
        for (String id : new String[]{"general_graardor", "kril_tsutsaroth"}) {
            for (Drop d : GwdBosses.builtinExtraDrops(id)) {
                assertTrue(ItemCatalog.exists(d.item()), d.item());
            }
            assertNotNull(ItemCatalog.get(GwdBosses.builtinPet(id)));
        }
        assertTrue(GwdBosses.builtinExtraDrops("obor").isEmpty());
    }

    @Test
    void wildernessBossesLiveInTheWilderness() {
        for (BossDef b : WildernessBosses.all()) {
            assertTrue(b.wilderness, b.id);
            PlaceDef place = ContentRegistry.place(b.place);
            assertNotNull(place, b.id + " place");
            assertEquals(Region.WILDERNESS, GielinorMap.regionAt(place.osrsX(), place.osrsY()), b.id);
            assertTrue(place.osrsY() > GielinorMap.WILDERNESS_DITCH, b.id);
        }
        assertEquals(1000, boss("callisto").hitpoints);
        assertEquals(255, boss("vetion").hitpoints);
        assertEquals(16, maxHit(boss("scorpia"), "sting"));
        assertEquals(65, maxHit(boss("king_black_dragon"), "dragonfire"));
        assertEquals(AttackDef.Special.DRAGONFIRE, WildernessBosses.KBD_POISON.special());
        assertEquals(AttackDef.Special.DRAGONFIRE, WildernessBosses.KBD_ICE.special());
        assertEquals(AttackDef.Special.DRAGONFIRE, WildernessBosses.KBD_SHOCK.special());
        assertEquals(128, rarity(boss("king_black_dragon"), "kbd_heads"));
        assertEquals(196, rarity(boss("callisto"), "claws_of_callisto"));
        assertEquals(196, rarity(boss("venenatis"), "fangs_of_venenatis"));
        assertEquals(196, rarity(boss("vetion"), "skull_of_vetion"));
        assertEquals(256, rarity(boss("scorpia"), "odium_shard_3"));
        assertEquals(256, rarity(boss("chaos_fanatic"), "malediction_shard_1"));
        assertEquals(256, rarity(boss("crazy_archaeologist"), "odium_shard_2"));
        assertEquals(64, rarity(boss("chaos_elemental"), "dragon_2h_sword")); // wiki 2/128 (BossDropsTable)
        assertEquals(300, WildernessBosses.realPetRate("chaos_elemental"));
    }

    /** The dungeon lies at its OSRS tiles; its entrance is the hole on the God Wars Dungeon landmark north of Trollheim. */
    @Test
    void godWarsDungeonIsAtItsRealTiles() {
        Landmark gwd = Landmark.GOD_WARS_DUNGEON;
        assertEquals(gwd.osrsX(), GwdDungeon.HOLE_TILE[0]);
        assertEquals(gwd.osrsY(), GwdDungeon.HOLE_TILE[1]);
        PlaceDef p = ContentRegistry.place("gwd_ancient_prison");
        assertNotNull(p);
        assertEquals(PlaceDef.Style.NONE, p.style());
        assertTrue(Math.abs(p.osrsX() - 2883) <= 3 && Math.abs(p.osrsY() - 5280) <= 3, "the frozen door at its OSRS tile");
        assertNull(ContentRegistry.place("gwd_armadyl_eyrie"));
        for (String id : new String[]{"kreearra", "commander_zilyana", "nex"}) {
            assertNull(boss(id).place, id + " has no surface arena: it waits in its chamber");
        }
    }

    @Test
    void placesAreFarEnoughApartForTheirArenas() {
        List<PlaceDef> places = new ArrayList<>(GwdPlaces.all());
        for (int i = 0; i < places.size(); i++) {
            for (int j = i + 1; j < places.size(); j++) {
                PlaceDef a = places.get(i);
                PlaceDef b = places.get(j);
                double d = Math.hypot(a.blockX() - b.blockX(), a.blockZ() - b.blockZ());
                assertTrue(d >= 56, a.id() + " too close to " + b.id());
            }
        }
    }

    @Test
    void campsReferenceRealCreaturesAndPlaces() {
        for (GwdPlaces.Camp camp : GwdPlaces.camps()) {
            assertTrue(ContentRegistry.place(camp.place()) != null || Landmark.byName(camp.place()) != null, camp.place());
            assertTrue(camp.cap() > 0 && camp.radius() > 0);
            for (String id : camp.creatures()) {
                assertNotNull(ContentRegistry.monster(id), id);
            }
        }
        // Every follower can actually be found somewhere (the dungeon's spawn tiles).
        Set<String> camped = new HashSet<>();
        GwdSpawns.all().forEach(s -> camped.add(s.creature()));
        for (String follower : GodWarsFactions.followers().keySet()) {
            assertTrue(camped.contains(follower), follower + " has no spawn");
        }
    }

    @Test
    void godswordsCanBeAssembledFromShardsAndHilts() {
        Map<String, Recipe> byOutput = new HashMap<>();
        for (Recipe r : Recipes.all()) {
            r.outputs().keySet().forEach(o -> byOutput.putIfAbsent(o, r));
        }
        for (String pair : new String[]{"godsword_shards_1_2", "godsword_shards_1_3", "godsword_shards_2_3"}) {
            Recipe r = byOutput.get(pair);
            assertNotNull(r, pair);
            assertEquals(80, r.level());
        }
        assertNotNull(byOutput.get("godsword_blade"));
        for (God god : God.values()) {
            Recipe r = byOutput.get(god.godsword());
            assertNotNull(r, god.godsword());
            assertTrue(r.inputs().containsKey("godsword_blade") && r.inputs().containsKey(god.hilt()));
            ItemDef gs = ItemCatalog.get(god.godsword());
            assertEquals(132, gs.equipment().stats().slashAttack());
            assertEquals(132, gs.equipment().stats().meleeStrength());
            assertEquals(6, gs.equipment().attackSpeed());
        }
        for (String out : new String[]{"ancient_godsword", "zaryte_crossbow", "frozen_key", "odium_ward", "malediction_ward",
                "voidwaker", "webweaver_bow", "ursine_chainmace", "accursed_sceptre"}) {
            assertNotNull(byOutput.get(out), out);
        }
        assertEquals(4, byOutput.get("frozen_key").inputs().size());
    }

    @Test
    void uniquesHaveRealStats() {
        assertEquals(33, ItemCatalog.get("armadyl_chestplate").equipment().stats().rangedAttack());
        assertEquals(100, ItemCatalog.get("armadyl_crossbow").equipment().stats().rangedAttack());
        assertEquals(82, ItemCatalog.get("saradomin_sword").equipment().stats().meleeStrength());
        assertEquals(142, ItemCatalog.get("torva_platebody").equipment().stats().rangedDefence());
        assertEquals(4, ItemCatalog.get("odium_ward").equipment().stats().rangedStrength());
        assertEquals(2, ItemCatalog.get("malediction_ward").equipment().stats().magicDamagePercent());
        assertTrue(ItemCatalog.exists("dragon_pickaxe"));
    }

    @Test
    void everyBossAndMinionHasACustomModel() {
        Map<String, ModelSpec> models = ContentRegistry.models();
        for (BossDef b : module.bosses()) {
            assertTrue(models.containsKey(b.model != null ? b.model : b.id), b.id + " has no model");
        }
        for (MonsterDef m : module.monsters()) {
            boolean biped = m.id.startsWith("gwd_spiritual") || List.of("gwd_hobgoblin", "gwd_ork", "gwd_ogre", "gwd_jogre",
                    "gwd_cyclops", "gwd_knight_of_saradomin", "gwd_saradomin_priest", "gwd_werewolf", "gwd_bloodveld", "gwd_gorak",
                    "gwd_vampire", "gwd_pyrefiend").contains(m.id);
            if (!biped) {
                assertTrue(models.containsKey(m.model != null ? m.model : m.id), m.id + " has no model");
            }
        }
        for (ModelSpec spec : module.models()) {
            int boxes = spec.parts().stream().mapToInt(p -> p.boxes.size()).sum();
            assertTrue(boxes <= 40, spec.id() + " has " + boxes + " boxes");
            assertTrue(spec.parts().size() >= 6, spec.id() + " is too simple");
        }
    }

    @Test
    void signatureModelFeatures() {
        Map<String, ModelSpec> models = ContentRegistry.models();
        ModelSpec kbd = models.get("king_black_dragon");
        assertEquals(3, count(kbd, Role.NECK));
        assertEquals(3, count(kbd, Role.HEAD));
        assertEquals(3, count(kbd, Role.JAW));
        assertEquals(1, count(kbd, Role.WING_LEFT));
        assertEquals(8, count(models.get("venenatis"), Role.LEG_EXTRA));
        assertTrue(count(models.get("scorpia"), Role.TAIL) >= 3);
        assertEquals(6, count(models.get("scorpia"), Role.LEG_EXTRA));
        assertTrue(count(models.get("chaos_elemental"), Role.SPIN) >= 2);
        assertEquals(1, count(models.get("chaos_elemental"), Role.FLOAT));
        for (String winged : new String[]{"kreearra", "commander_zilyana", "nex", "kril_tsutsaroth"}) {
            assertEquals(1, count(models.get(winged), Role.WING_LEFT), winged);
            assertEquals(1, count(models.get(winged), Role.WING_RIGHT), winged);
        }
        assertTrue(count(models.get("kril_tsutsaroth"), Role.TAIL) >= 1);
        assertEquals(4, count(models.get("callisto"), Role.LEG_FRONT_LEFT) + count(models.get("callisto"), Role.LEG_FRONT_RIGHT)
                + count(models.get("callisto"), Role.LEG_BACK_LEFT) + count(models.get("callisto"), Role.LEG_BACK_RIGHT));
    }
}
