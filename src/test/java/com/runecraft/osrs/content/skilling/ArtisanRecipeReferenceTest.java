package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Recipes;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.data.Gem;
import com.runecraft.osrs.data.Herb;
import com.runecraft.osrs.data.RuneAltar;
import com.runecraft.osrs.data.Tree;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Diffs the artisan skills' recipe data (Smithing, Crafting, Fletching, Herblore, Cooking recipe list,
 * Firemaking, Runecraft, herb cleaning) against a snapshot of the RuneLite skill calculator's per-action
 * level/XP tables (wiki-derived; {@code src/test/resources/osrs/skill_action_reference.tsv}, extracted from
 * {@code runelite-client/.../skillcalculator/skills/*Action.java}).
 *
 * <p>Recipes are matched to a reference action by their output item id (with a few explicit aliases); the
 * XP may be per recipe or per item made (arrows, bolts, darts give XP per item). Recipes with no reference
 * action are not checked here.
 */
class ArtisanRecipeReferenceTest {
    private static final String FIXTURE = "osrs/skill_action_reference.tsv";
    private static final double TOLERANCE = 0.051;
    private static final Set<Skill> SKILLS = Set.of(Skill.SMITHING, Skill.CRAFTING, Skill.FLETCHING, Skill.HERBLORE,
            Skill.COOKING);

    private record Ref(int level, double xp) {
    }

    /** Recipe id -> reference action, where the output id doesn't spell the action name. */
    private static final Map<String, String> ALIASES = new HashMap<>();

    /** Recipes whose XP is not comparable to the reference (level still checked). */
    private static final Set<String> XP_EXEMPT = Set.of(
            // RuneLite lists the XP of the whole chain (base food + topping); RuneCraft awards each step separately.
            "make_meat_pizza", "make_anchovy_pizza", "make_pineapple_pizza", "make_chocolate_cake",
            "make_potato_with_cheese", "make_tuna_potato",
            // Wiki: 40.5 (RuneLite rounds to 40); manta ray is 216.25 (wiki shows 216.2, RuneLite 216.3).
            "make_potato_with_butter", "cook_manta_ray");

    /** Recipes that match a reference action by name but are a different method (no check at all). */
    private static final Set<String> SKIP = Set.of(
            // RuneLite's ARROW_SHAFT is for normal logs only.
            "fletch_arrow_shaft_oak", "fletch_arrow_shaft_willow", "fletch_arrow_shaft_maple", "fletch_arrow_shaft_yew",
            "fletch_arrow_shaft_magic", "fletch_arrow_shaft_redwood");

    static {
        ALIASES.put("craft_leather_cowl", "COWL");
        ALIASES.put("craft_hard_leather_body", "HARDLEATHER_BODY");
        for (String hide : List.of("green", "blue", "red", "black")) {
            ALIASES.put("craft_" + hide + "_dhide_vambraces", hide.toUpperCase(Locale.ROOT) + "_DHIDE_VAMB");
        }
        ALIASES.put("craft_gold_amulet", "GOLD_AMULET_U");
        ALIASES.put("shape_pot", "UNFIRED_POT");
        ALIASES.put("fire_pot", "POT");
        ALIASES.put("shape_plant_pot", "UNFIRED_PLANT_POT");
        ALIASES.put("fire_plant_pot", "EMPTY_PLANT_POT");
        ALIASES.put("shape_pot_lid", "UNFIRED_POT_LID");
        ALIASES.put("blow_empty_fishbowl", "FISHBOWL");
        ALIASES.put("blow_empty_light_orb", "LIGHT_ORB");
        ALIASES.put("craft_unstrung_symbol", "HOLY_SYMBOL");
        ALIASES.put("craft_unstrung_emblem", "UNHOLY_SYMBOL");
        ALIASES.put("smith_adamant_nails", "ADAMANTITE_NAILS");
        ALIASES.put("smith_adamant_limbs", "ADAMANTITE_LIMBS");
        ALIASES.put("smith_rune_bolts_unf", "RUNITE_BOLTS_UNF");
        ALIASES.put("smith_rune_limbs", "RUNITE_LIMBS");
        ALIASES.put("smith_cannonball", "STEEL_CANNONBALL");
        ALIASES.put("fletch_rune_crossbow_u", "RUNITE_CROSSBOW_U");
        ALIASES.put("fletch_rune_bolts", "RUNITE_BOLTS");
        ALIASES.put("mix_anti_venom", "ANTIVENOM_4");
        ALIASES.put("mix_anti_venom_plus", "ANTIVENOM_PLUS_4");
        for (String d : List.of("super_attack", "super_strength", "super_defence", "ranging", "magic", "bastion",
                "battlemage", "super_combat")) {
            ALIASES.put("mix_divine_" + d, "DIVINE_" + d.toUpperCase(Locale.ROOT) + "_POTION_4");
        }
    }

    private static List<String> fixtureLines() throws IOException {
        Path p = Path.of("src/test/resources", FIXTURE);
        if (Files.exists(p)) {
            return Files.readAllLines(p, StandardCharsets.UTF_8);
        }
        try (InputStream in = ArtisanRecipeReferenceTest.class.getClassLoader().getResourceAsStream(FIXTURE)) {
            assertNotNull(in, "missing " + FIXTURE);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
    }

    /** "SKILL:ACTION" -> reference row (first row wins). */
    private static Map<String, Ref> reference() throws IOException {
        Map<String, Ref> out = new HashMap<>();
        for (String line : fixtureLines()) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\t");
            out.putIfAbsent(f[0] + ":" + f[1], new Ref(Integer.parseInt(f[2]), Double.parseDouble(f[3])));
        }
        return out;
    }

    private static List<String> candidates(String outputId) {
        String base = outputId.replace("minecraft:", "").toUpperCase(Locale.ROOT);
        String noPotion = base.replace("_POTION", "");
        return List.of(base, base + "S", base.endsWith("S") ? base.substring(0, base.length() - 1) : base,
                base + "_3", base + "_4", base + "_POTION_3", base + "_POTION_4", noPotion + "_3",
                base.replace("ADAMANTITE", "ADAMANT"), base.replace("RUNITE", "RUNE"));
    }

    @Test
    void recipesMatchReferenceLevelsAndXp() throws IOException {
        Map<String, Ref> ref = reference();
        List<String> errors = new ArrayList<>();
        int checked = 0;
        for (Recipe r : Recipes.all()) {
            if (r.skill() == null || !SKILLS.contains(r.skill()) || r.outputs().isEmpty() || SKIP.contains(r.id())) {
                continue;
            }
            Map.Entry<String, Integer> out = r.outputs().entrySet().iterator().next();
            String key = null;
            String alias = ALIASES.get(r.id());
            if (alias != null) {
                key = r.skill().name() + ":" + alias;
                if (!ref.containsKey(key)) {
                    errors.add(r.id() + ": alias " + alias + " not in reference");
                    continue;
                }
            } else {
                for (String c : candidates(out.getKey())) {
                    if (ref.containsKey(r.skill().name() + ":" + c)) {
                        key = r.skill().name() + ":" + c;
                        break;
                    }
                }
            }
            if (key == null) {
                continue;
            }
            checked++;
            Ref e = ref.get(key);
            if (r.level() != e.level()) {
                errors.add(r.id() + ": level " + r.level() + " != " + e.level() + " (" + key + ")");
            }
            double xp = r.xpTenths() / 10.0;
            double perItem = xp / out.getValue();
            if (!XP_EXEMPT.contains(r.id()) && Math.abs(xp - e.xp()) > TOLERANCE && Math.abs(perItem - e.xp()) > TOLERANCE) {
                errors.add(r.id() + ": xp " + xp + " (" + perItem + " each) != " + e.xp() + " (" + key + ")");
            }
        }
        assertTrue(errors.isEmpty(), String.join("\n", errors));
        assertTrue(checked >= 470, "only " + checked + " recipes matched the reference");
    }

    @Test
    void herbCleaningMatchesReference() throws IOException {
        Map<String, Ref> ref = reference();
        for (Herb h : Herb.values()) {
            Ref e = ref.get("HERBLORE:" + h.cleanItem().toUpperCase(Locale.ROOT));
            assertNotNull(e, h.name());
            assertEquals(e.level(), h.cleanLevel(), h.name());
            assertEquals(e.xp(), h.cleanXp(), TOLERANCE, h.name());
        }
    }

    @Test
    void firemakingMatchesReference() throws IOException {
        Map<String, Ref> ref = reference();
        for (Tree t : Tree.values()) {
            Ref e = ref.get("FIREMAKING:" + t.logItem().toUpperCase(Locale.ROOT));
            assertNotNull(e, t.name());
            assertEquals(e.level(), t.firemakingLevel(), t.name());
            assertEquals(e.xp(), t.firemakingXp(), TOLERANCE, t.name());
        }
        for (GatheringData.ModTree t : GatheringData.TREES) {
            if (t.fmLevel() > 0) {
                Ref e = ref.get("FIREMAKING:" + t.log().toUpperCase(Locale.ROOT));
                assertNotNull(e, t.log());
                assertEquals(e.level(), t.fmLevel(), t.log());
                assertEquals(e.xp(), t.fmXp(), TOLERANCE, t.log());
            }
        }
    }

    @Test
    void runecraftMatchesReference() throws IOException {
        Map<String, Ref> ref = reference();
        for (RuneAltar a : RuneAltar.values()) {
            if (a == RuneAltar.SOUL) {
                continue; // approx: RuneCraft binds pure essence instead of dark essence fragments
            }
            String name = a == RuneAltar.BLOOD ? "TRUE_BLOOD" : a.rune().name();
            Ref e = ref.get("RUNECRAFT:" + name + "_RUNE");
            assertNotNull(e, a.name());
            assertEquals(e.level(), a.level(), a.name());
            assertEquals(e.xp(), a.xp(), TOLERANCE, a.name());
            Ref tiara = ref.get("RUNECRAFT:" + a.rune().name() + "_TIARA");
            if (tiara != null) {
                assertEquals(tiara.xp(), RunecraftData.tiaraXp(a), TOLERANCE, a.name() + " tiara");
            }
        }
        // Combination runes: the reference lists the better altar's XP.
        Map<String, Double> best = new HashMap<>();
        for (RunecraftData.Combination c : RunecraftData.COMBINATIONS) {
            best.merge(c.id(), c.xp(), Math::max);
            Ref e = ref.get("RUNECRAFT:" + c.id().toUpperCase(Locale.ROOT));
            assertNotNull(e, c.id());
            assertEquals(e.level(), c.level(), c.id());
        }
        best.forEach((id, xp) -> assertEquals(ref.get("RUNECRAFT:" + id.toUpperCase(Locale.ROOT)).xp(), xp, TOLERANCE, id));
    }

    @Test
    void semiPreciousGemsCanBeCrushed() {
        Recipe opal = Recipes.get("cut_opal");
        // OSRS Wiki: opal (L-1)*122/98+129 over 256, jade (L-1)*145/98+101, red topaz (L-1)*150/98+91.
        assertEquals(129 / 256.0, opal.successChance(1), 1e-9);
        assertEquals(101 / 256.0, Recipes.get("cut_jade").successChance(1), 1e-9);
        assertEquals(91 / 256.0, Recipes.get("cut_red_topaz").successChance(1), 1e-9);
        assertEquals(1.0, opal.successChance(Gem.NO_CRUSH_LEVEL), 1e-9);
        assertEquals(Gem.CRUSHED_GEM, opal.failItem());
        assertEquals(38, opal.failXpTenths());
        assertEquals(63, Recipes.get("cut_red_topaz").failXpTenths());
        assertEquals(1.0, Recipes.get("cut_sapphire").successChance(20), 1e-9);
        assertEquals(null, Recipes.get("cut_sapphire").failItem());
    }

    @Test
    void recipesForExistingItemsArePresent() {
        // Diary task "Smith a Dragon sq shield" and barbarian-smithed spears need these.
        assertEquals(60, Recipes.get("smith_dragon_sq_shield").level());
        assertEquals(5, Recipes.get("smith_bronze_spear").level());
        assertEquals(90, Recipes.get("smith_rune_spear").level());
        assertEquals(150.0, Recipes.get("smith_rune_spear").xpTenths() / 10.0, 1e-9);
        // OSRS has no pickaxe smithing.
        assertEquals(null, Recipes.get("smith_bronze_pickaxe"));
    }
}
