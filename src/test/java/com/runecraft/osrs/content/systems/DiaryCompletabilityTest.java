package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.Spell;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Boss;
import com.runecraft.osrs.data.Monster;
import com.runecraft.osrs.world.Elevation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every diary task must be completable with the game's mechanics (things it references exist). */
class DiaryCompletabilityTest {
    private static List<String> monsterNames() {
        List<String> names = new ArrayList<>();
        for (MonsterDef m : ContentRegistry.monsters()) {
            names.add(m.name.toLowerCase(Locale.ROOT));
        }
        for (BossDef b : ContentRegistry.bosses()) {
            names.add(b.name.toLowerCase(Locale.ROOT));
        }
        for (Monster m : Monster.values()) {
            names.add(m.displayName().toLowerCase(Locale.ROOT));
        }
        for (Boss b : Boss.values()) {
            names.add(b.displayName().toLowerCase(Locale.ROOT));
        }
        return names;
    }

    private static Set<String> bossIds() {
        Set<String> ids = new HashSet<>();
        for (BossDef b : ContentRegistry.bosses()) {
            ids.add(b.id);
        }
        for (Boss b : Boss.values()) {
            ids.add(b.entityId());
        }
        ids.add("barrows_chests");
        return ids;
    }

    static boolean spellExists(String id) {
        if (Spellbooks.get(id) != null) {
            return true;
        }
        try {
            Spell.valueOf(id.toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    static boolean itemTargetExists(String target) {
        for (String alt : target.split("\\|")) {
            if (alt.startsWith("*")) {
                String part = alt.substring(1);
                if (ItemCatalog.all().stream().anyMatch(d -> d.id().contains(part))) {
                    return true;
                }
            } else if (ItemCatalog.get(alt) != null) {
                return true;
            }
        }
        return false;
    }

    /** Some tile within the radius is land (not open sea). */
    static boolean onLand(int x, int y, int r) {
        int step = Math.max(1, r / 3);
        for (int dx = -r; dx <= r; dx += step) {
            for (int dy = -r; dy <= r; dy += step) {
                if (!Elevation.isOcean(GielinorMap.toBlockX(x + dx), GielinorMap.toBlockZ(y + dy))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    void everyTaskReferencesSomethingThatExists() {
        List<String> monsters = monsterNames();
        Set<String> bosses = bossIds();
        List<String> problems = new ArrayList<>();
        for (Diaries.Task t : Diaries.tasks()) {
            String bad = switch (t.type()) {
                case LEVEL, XP_IN -> {
                    Skill.valueOf(t.target());
                    yield t.level() >= 1 && t.level() <= 99 ? null : "level " + t.level();
                }
                case KILL -> {
                    for (String alt : t.target().split("\\|")) {
                        if (alt.isEmpty() || monsters.stream().noneMatch(n -> n.contains(alt))) {
                            yield "no monster named like '" + alt + "'";
                        }
                    }
                    yield null;
                }
                case MAKE, HAVE, OBTAIN -> itemTargetExists(t.target()) ? null : "no item " + t.target();
                case QUEST -> ContentRegistry.quest(t.target()) != null ? null : "no quest " + t.target();
                case VISIT, SKILLCAPE -> {
                    int[] v = Diaries.visitTarget(t.target());
                    yield v[2] > 0 && onLand(v[0], v[1], v[2]) ? null : "visit spot at sea " + t.target();
                }
                case KC -> {
                    for (String b : t.target().split("[|&]")) {
                        if (!bosses.contains(b)) {
                            yield "no boss " + b;
                        }
                    }
                    yield null;
                }
                case CAST -> {
                    for (String s : t.target().split("\\|")) {
                        if (!spellExists(s)) {
                            yield "no spell " + s;
                        }
                    }
                    yield null;
                }
                case CLUE -> ClueScrolls.Tier.valueOf(t.target().toUpperCase(Locale.ROOT)) != null ? null : "clue";
                default -> null;
            };
            if (bad == null && t.restrict() != null && t.restrict().indexOf(',') < 0 && Diaries.area(t.restrict()) == null) {
                bad = "bad restriction " + t.restrict();
            }
            if (bad == null && t.restrict() != null && t.restrict().indexOf(',') > 0) {
                int[] c = Diaries.visitTarget(t.restrict());
                if (!onLand(c[0], c[1], c[2])) {
                    bad = "restriction at sea " + t.restrict();
                }
            }
            for (Map.Entry<Skill, Integer> e : t.allReqs().entrySet()) {
                if (e.getValue() < 1 || e.getValue() > 99) {
                    bad = "requirement " + e;
                }
            }
            if (bad != null) {
                problems.add(t.id() + " (" + t.description() + "): " + bad);
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    /** Every task type is completed by a game event or a periodic state check (DiaryService wires both lists). */
    @Test
    void everyTaskHasATrigger() {
        java.util.Set<Diaries.Type> triggered = java.util.EnumSet.noneOf(Diaries.Type.class);
        triggered.addAll(Diaries.EVENT_TYPES);
        triggered.addAll(Diaries.PERIODIC_TYPES);
        for (Diaries.Task t : Diaries.tasks()) {
            assertTrue(triggered.contains(t.type()), t.id() + " (" + t.description() + ") has no trigger for " + t.type());
        }
    }

    /**
     * Make events only fire for make-menu recipes, so every MAKE target must be a recipe output (a wildcard
     * matching some unrelated item, e.g. "*spider" matching red spiders' eggs, is not enough).
     */
    @Test
    void makeTasksNameARecipeOutput() {
        Set<String> outputs = new HashSet<>();
        for (com.runecraft.osrs.Recipe r : com.runecraft.osrs.Recipes.all()) {
            outputs.addAll(r.outputs().keySet());
        }
        List<String> problems = new ArrayList<>();
        for (Diaries.Task t : Diaries.tasks()) {
            if (t.type() == Diaries.Type.MAKE && outputs.stream().noneMatch(o -> Diaries.itemMatches(t.target(), o))) {
                problems.add(t.id() + " (" + t.description() + "): no recipe makes " + t.target());
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void taskCountsFollowOsrs() {
        // OSRS task counts per tier (easy, medium, hard, elite); a few tiers merge tasks the game can't tell apart.
        Map<String, int[]> osrs = new HashMap<>();
        osrs.put("ardougne", new int[]{10, 12, 12, 8});
        osrs.put("desert", new int[]{11, 12, 10, 6});
        osrs.put("falador", new int[]{11, 14, 11, 6});
        osrs.put("fremennik", new int[]{10, 9, 9, 6});
        osrs.put("kandarin", new int[]{11, 14, 11, 7});
        osrs.put("karamja", new int[]{10, 19, 10, 5});
        osrs.put("kourend", new int[]{12, 13, 10, 8});
        osrs.put("lumbridge", new int[]{12, 12, 11, 6});
        osrs.put("morytania", new int[]{11, 11, 10, 6});
        osrs.put("varrock", new int[]{14, 13, 10, 5});
        osrs.put("western", new int[]{11, 13, 13, 7});
        osrs.put("wilderness", new int[]{12, 11, 10, 11});
        int total = 0;
        for (Diaries.Area a : Diaries.AREAS) {
            for (Diaries.Tier t : Diaries.Tier.values()) {
                int n = Diaries.tasks(a.id(), t).size();
                assertEquals(osrs.get(a.id())[t.ordinal()], n, a.id() + " " + t);
                total += n;
            }
        }
        assertEquals(496, total);
    }

    @Test
    void requirementsAndMatchers() {
        // Ardougne elite needs 91 Cooking (manta ray), 94 Magic (Ice Barrage) and 90 Agility (rooftop).
        Map<Skill, Integer> ardyElite = Diaries.requirements("ardougne", Diaries.Tier.ELITE);
        assertEquals(91, ardyElite.get(Skill.COOKING));
        assertEquals(94, ardyElite.get(Skill.MAGIC));
        assertEquals(90, ardyElite.get(Skill.AGILITY));
        assertTrue(Diaries.questRequirements("lumbridge", Diaries.Tier.HARD).contains("recipe_for_disaster"));
        assertTrue(Diaries.killMatches("bronze dragon|iron dragon", "Iron dragon"));
        assertFalse(Diaries.killMatches("bronze dragon|iron dragon", "Steel dragon"));
        assertTrue(Diaries.itemMatches("fire_cape|infernal_cape", "infernal_cape"));
        assertTrue(Diaries.spellMatches("saradomin_strike|flames_of_zamorak", "flames_of_zamorak"));
        Map<String, Integer> kc = new HashMap<>(Map.of("dagannoth_rex", 1, "dagannoth_prime", 2));
        assertFalse(Diaries.kcMet("dagannoth_rex&dagannoth_prime&dagannoth_supreme", 1, kc));
        kc.put("dagannoth_supreme", 1);
        assertTrue(Diaries.kcMet("dagannoth_rex&dagannoth_prime&dagannoth_supreme", 1, kc));
        assertTrue(Diaries.kcMet("callisto|artio", 1, Map.of("artio", 1)));
        assertTrue(Diaries.kcMet("barrows_chests", 1, Map.of("barrows_chests", 3)));
        assertFalse(Diaries.kcMet("barrows_chests", 1, Map.of("ahrim", 3)));
        assertTrue(Diaries.restrictContains("3359,2970,40", null, 3360, 2980));
        assertFalse(Diaries.restrictContains("3359,2970,40", "KHARIDIAN_DESERT", 3200, 2980));
        assertTrue(Diaries.restrictContains("desert", "KHARIDIAN_DESERT", 0, 0));
    }

    @Test
    void rewardEffects() {
        // Ardougne cloak: unlimited monastery teleports at every tier, farm teleports 3/5/unlimited from tier 2.
        DiaryRewards.Tele farm = DiaryRewards.teleports("ardougne", 2).get(1);
        assertEquals("Ardougne Farm", farm.dest().name());
        assertEquals(3, farm.perDay(2));
        assertEquals(5, farm.perDay(3));
        assertEquals(0, farm.perDay(4));
        assertEquals(1, DiaryRewards.teleports("ardougne", 1).size());
        assertEquals(0, DiaryRewards.teleports("ardougne", 1).get(0).perDay(1));
        assertEquals(1, DiaryRewards.teleports("karamja", 3).size());
        assertEquals(2, DiaryRewards.teleports("karamja", 4).size());
        assertTrue(DiaryRewards.teleports("desert", 1).isEmpty());
        assertEquals(50, DiaryRewards.faladorRestorePercent(1));
        assertEquals(100, DiaryRewards.faladorRestorePercent(2));
        assertEquals(2, DiaryRewards.faladorRestoresPerDay(4));
        assertTrue(DiaryRewards.varrockOre(1, "coal"));
        assertFalse(DiaryRewards.varrockOre(1, "mithril_ore"));
        assertTrue(DiaryRewards.varrockOre(4, "runite_ore"));
        assertTrue(DiaryRewards.varrockBar(3, "adamantite_bar"));
        assertFalse(DiaryRewards.varrockBar(3, "runite_bar"));
        assertEquals("ardougne", DiaryRewards.areaOf("ardougne_cloak_3").id());
        assertEquals(3, DiaryRewards.tierOf("ardougne_cloak_3"));
        for (Diaries.Area a : Diaries.AREAS) {
            for (int tier = 1; tier <= 4; tier++) {
                String id = a.rewardBase() + "_" + tier;
                assertNotNull(ItemCatalog.get(id), id);
                assertEquals(a, DiaryRewards.areaOf(id));
                Jewellery.Worn worn = Jewellery.worn(id);
                if (worn != null) {
                    assertEquals(DiaryRewards.destinations(a.id(), tier), worn.teleports(), id);
                }
            }
        }
        for (String[] ids : new String[][]{{"clay", "copper_ore", "tin_ore", "iron_ore", "coal", "gold_ore", "mithril_ore",
                "adamantite_ore", "runite_ore", "bronze_bar", "iron_bar", "steel_bar", "mithril_bar", "adamantite_bar", "runite_bar"}}) {
            for (String id : ids) {
                assertNotNull(ItemCatalog.get(id), id);
            }
        }
    }
}
