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
import static org.junit.jupiter.api.Assertions.assertNull;
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
            "make_potato_with_butter", "cook_manta_ray",
            // OSRS Wiki: 5.8 / 7.9 XP per spike (12 bolts); RuneLite lists 1 / 1.3 per bolt.
            "fletch_kebbit_bolts", "fletch_long_kebbit_bolts",
            // RuneLite lists the 3-dose total (160); RuneCraft awards the 4-dose steps 57 + 63 + 72 (checked below).
            "mix_sanfew_serum");

    /**
     * Multi-step dishes: XP from the earlier steps of the chain, added before comparing (gnome dishes are
     * baked half-way for 30 XP first, cocktails are shaken for 30 XP first).
     */
    private static final Map<String, Double> PRIOR_XP = new HashMap<>();

    /** Recipes that match a reference action by name but are a different method (no check at all). */
    private static final Set<String> SKIP = Set.of(
            // RuneLite's ARROW_SHAFT is for normal logs only.
            "fletch_arrow_shaft_oak", "fletch_arrow_shaft_willow", "fletch_arrow_shaft_maple", "fletch_arrow_shaft_yew",
            "fletch_arrow_shaft_magic", "fletch_arrow_shaft_redwood");

    static {
        for (BrewingData.GnomeDish d : BrewingData.GNOME_DISHES) {
            PRIOR_XP.put("cook_" + d.id(), BrewingData.HALF_BAKE_XP);
        }
        for (BrewingData.Cocktail c : BrewingData.COCKTAILS) {
            PRIOR_XP.put("pour_" + c.id(), BrewingData.COCKTAIL_MIX_XP);
        }
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
        // Brews go into the vat (no ale output until it has fermented).
        for (BrewingData.Brew b : BrewingData.BREWS) {
            ALIASES.put("brew_" + b.id(), b.id().toUpperCase(Locale.ROOT));
        }
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
        List<String> errors = new ArrayList<>();
        int checked = check(reference(), errors, new java.util.HashSet<>());
        assertTrue(errors.isEmpty(), String.join("\n", errors));
        assertTrue(checked >= 550, "only " + checked + " recipes matched the reference");
    }

    /**
     * Brewing, gnome cooking, the Zogre/huasca potions, spinning/weaving, gold tiaras and the javelin/bolt/ogre
     * arrow fletching that the reference lists all have a matching recipe.
     */
    @Test
    void artisanActionsAreAllCovered() throws IOException {
        java.util.Set<String> matched = new java.util.HashSet<>();
        check(reference(), new ArrayList<>(), matched);
        List<String> missing = new ArrayList<>();
        for (String key : List.of(
                "COOKING:CIDER", "COOKING:DWARVEN_STOUT", "COOKING:ASGARNIAN_ALE", "COOKING:GREENMANS_ALE",
                "COOKING:WIZARDS_MIND_BOMB", "COOKING:DRAGON_BITTER", "COOKING:MOONLIGHT_MEAD", "COOKING:AXEMANS_FOLLY",
                "COOKING:CHEFS_DELIGHT", "COOKING:SLAYERS_RESPITE",
                "COOKING:FRUIT_BATTA", "COOKING:TOAD_BATTA", "COOKING:WORM_BATTA", "COOKING:VEGETABLE_BATTA",
                "COOKING:CHEESE_AND_TOMATO_BATTA", "COOKING:TOAD_CRUNCHIES", "COOKING:SPICY_CRUNCHIES", "COOKING:WORM_CRUNCHIES",
                "COOKING:CHOCCHIP_CRUNCHIES", "COOKING:WORM_HOLE", "COOKING:VEG_BALL", "COOKING:TANGLED_TOADS_LEGS",
                "COOKING:CHOCOLATE_BOMB", "COOKING:FRUIT_BLAST", "COOKING:PINEAPPLE_PUNCH", "COOKING:WIZARD_BLIZZARD",
                "COOKING:SHORT_GREEN_GUY", "COOKING:DRUNK_DRAGON", "COOKING:CHOC_SATURDAY", "COOKING:BLURBERRY_SPECIAL",
                "HERBLORE:RELICYMS_BALM_3", "HERBLORE:SERUM_207_3", "HERBLORE:SANFEW_SERUM_3",
                "HERBLORE:PRAYER_REGENERATION_POTION", "HERBLORE:ANTIVENOM_4", "HERBLORE:ANTIVENOM_PLUS_4",
                "CRAFTING:BALL_OF_WOOL", "CRAFTING:BOW_STRING", "CRAFTING:CROSSBOW_STRING", "CRAFTING:MAGIC_STRING",
                "CRAFTING:EMPTY_SACK", "CRAFTING:BASKET", "CRAFTING:TIARA", "CRAFTING:GOLD_TIARA",
                "FLETCHING:DRAGON_JAVELIN", "FLETCHING:AMETHYST_BROAD_BOLTS", "FLETCHING:AMETHYST_ARROW",
                "FLETCHING:AMETHYST_JAVELIN", "FLETCHING:AMETHYST_DART", "FLETCHING:OGRE_ARROW", "FLETCHING:KEBBIT_BOLTS",
                "FLETCHING:LONG_KEBBIT_BOLTS")) {
            if (!matched.contains(key)) {
                missing.add(key);
            }
        }
        assertTrue(missing.isEmpty(), "no recipe for " + missing);
    }

    /** Checks every artisan recipe that matches a reference action; returns how many matched. */
    private static int check(Map<String, Ref> ref, List<String> errors, java.util.Set<String> matched) {
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
            matched.add(key);
            Ref e = ref.get(key);
            if (r.level() != e.level()) {
                errors.add(r.id() + ": level " + r.level() + " != " + e.level() + " (" + key + ")");
            }
            double xp = r.xpTenths() / 10.0 + PRIOR_XP.getOrDefault(r.id(), 0.0);
            double perItem = xp / out.getValue();
            if (!XP_EXEMPT.contains(r.id()) && Math.abs(xp - e.xp()) > TOLERANCE && Math.abs(perItem - e.xp()) > TOLERANCE) {
                errors.add(r.id() + ": xp " + xp + " (" + perItem + " each) != " + e.xp() + " (" + key + ")");
            }
        }
        return checked;
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

    /** Every input and tool of the brewing, gnome cooking, mix and new crafting/fletching recipes has a source item. */
    @Test
    void newArtisanInputsExist() {
        for (Recipe r : Recipes.all()) {
            String id = r.id();
            boolean scope = id.startsWith("brew_") || id.startsWith("cook_") || id.startsWith("pour_") || id.startsWith("shake_")
                    || id.startsWith("bake_half") || id.startsWith("make_raw_") || id.startsWith("weave_") || id.startsWith("spin_")
                    || id.startsWith("mix_") || id.contains("ogre") || id.contains("kebbit_bolts") || id.contains("javelin")
                    || id.contains("broad_bolts") || id.equals("craft_gold_tiara");
            if (!scope) {
                continue;
            }
            for (String in : r.inputs().keySet()) {
                if (!in.startsWith("minecraft:")) {
                    assertNotNull(com.runecraft.osrs.ItemCatalog.get(in), id + " input " + in);
                }
            }
            if (r.requiredTool() != null) {
                assertNotNull(com.runecraft.osrs.ItemCatalog.get(r.requiredTool()), id + " tool " + r.requiredTool());
            }
            for (String o : r.outputs().keySet()) {
                if (!o.startsWith("minecraft:")) {
                    assertNotNull(com.runecraft.osrs.ItemCatalog.get(o), id + " output " + o);
                }
            }
        }
    }

    @Test
    void brewingMakesEightPintsAndMatureAles() {
        for (BrewingData.Brew b : BrewingData.BREWS) {
            Recipe r = Recipes.get("brew_" + b.id());
            assertNotNull(r, b.id());
            assertEquals(com.runecraft.osrs.MenuType.BREWING, r.menu());
            // The brew goes into the vat; the glasses are filled when it has fermented.
            assertNull(r.outputs().get(b.id()));
            assertNull(r.inputs().get("beer_glass"));
            assertEquals(1, r.inputs().get("ale_yeast"));
            assertEquals(b.malt() ? Integer.valueOf(2) : null, r.inputs().get("barley_malt"), b.id());
            assertEquals(b.malt() ? Integer.valueOf(2) : null, r.inputs().get("minecraft:water_bucket"), b.id());
            assertNotNull(BrewingData.drink(b.id()), b.id());
            assertNotNull(BrewingData.drink(b.mature()), b.mature());
            assertEquals(com.runecraft.osrs.ItemDef.Kind.FOOD, com.runecraft.osrs.ItemCatalog.get(b.mature()).kind(), b.mature());
            assertEquals("beer_glass", BrewingData.emptyGlass(b.mature()));
        }
        assertEquals(b("axemans_folly").amount(), 1); // one oak root per vat
        // Outcomes out of 128: 6 mature / 106 regular / 16 bad, or 82 / 30 / 16 with "the stuff".
        int[] plain = new int[3];
        int[] stuff = new int[3];
        for (int i = 0; i < 128; i++) {
            plain[BrewingData.outcome(false, (i + 0.5) / 128).ordinal()]++;
            stuff[BrewingData.outcome(true, (i + 0.5) / 128).ordinal()]++;
        }
        assertEquals(java.util.List.of(16, 106, 6), java.util.Arrays.stream(plain).boxed().toList());
        assertEquals(java.util.List.of(16, 30, 82), java.util.Arrays.stream(stuff).boxed().toList());
        // Three aligned 640-minute cycles: between 21h20m (+1 minute) and 32h.
        long cycle = 640L * 1200;
        assertEquals(3 * cycle, BrewingData.readyAt(0, cycle));
        assertEquals(4 * cycle, BrewingData.readyAt(cycle + 5, cycle));
        assertEquals(2 * cycle + 1, BrewingData.readyAt(cycle - 1, cycle) - (cycle - 1));
    }

    private static BrewingData.Brew b(String id) {
        return BrewingData.BREWS.stream().filter(x -> x.id().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void drinksBoostAndDrain() {
        com.runecraft.osrs.SkillSet s = new com.runecraft.osrs.SkillSet();
        for (Skill sk : new Skill[]{Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.COOKING}) {
            s.setXpTenths(sk, com.runecraft.osrs.Experience.xpForLevel(50) * 10);
        }
        assertEquals(50, s.level(Skill.ATTACK));
        BrewingData.drink("asgarnian_ale").apply(s);
        assertEquals(2, s.boost(Skill.STRENGTH));
        assertEquals(-4, s.boost(Skill.ATTACK));
        // Drains stack, boosts don't.
        BrewingData.drink("asgarnian_ale").apply(s);
        assertEquals(2, s.boost(Skill.STRENGTH));
        assertEquals(-8, s.boost(Skill.ATTACK));
        BrewingData.drink("chefs_delight_m").apply(s);
        assertEquals(2 + 2, s.boost(Skill.COOKING)); // 2 + 5% of 50
        BrewingData.drink("wizard_blizzard").apply(s);
        assertEquals(4, s.boost(Skill.STRENGTH)); // 1 + 6% of 50 = 4 replaces the ale's +2
        assertEquals(-8 - 5 - 3, s.boost(Skill.ATTACK)); // chef's delight(m) 3 + 5% of 42, then 3 + 2% of the drained level (37)
        assertEquals(9, BrewingData.drink("fruit_blast").heal());
        assertEquals(6, BrewingData.drink("moonlight_mead_m").heal());
    }

    @Test
    void gnomeDishesAndCocktailsChainToTheirOsrsTotal() {
        for (BrewingData.GnomeDish d : BrewingData.GNOME_DISHES) {
            Recipe fin = Recipes.get("cook_" + d.id());
            assertNotNull(fin, d.id());
            assertEquals(d.totalXp(), fin.xpTenths() / 10.0 + BrewingData.HALF_BAKE_XP, 1e-9, d.id());
            assertTrue(fin.inputs().containsKey(d.halfBaked()), d.id());
            assertEquals(d.heal(), com.runecraft.osrs.ItemCatalog.get(d.id()).heal(), d.id());
        }
        for (BrewingData.Cocktail c : BrewingData.COCKTAILS) {
            Recipe shake = Recipes.get("shake_" + c.mixId());
            assertEquals("cocktail_shaker", shake.requiredTool(), c.id());
            Recipe pour = Recipes.get("pour_" + c.id());
            assertEquals(1, pour.inputs().get("cocktail_glass"));
            assertEquals(c.totalXp(), (shake.xpTenths() + pour.xpTenths()) / 10.0, 1e-9, c.id());
            assertNotNull(BrewingData.drink(c.id()), c.id());
        }
    }

    @Test
    void barbarianMixesAndZogrePotions() {
        for (HerbloreData.Mix m : HerbloreData.MIXES) {
            Recipe r = Recipes.get("mix_" + m.id());
            assertNotNull(r, m.id());
            assertTrue(r.inputs().containsKey(m.roe() ? "roe" : "caviar"), m.id());
            assertNotNull(com.runecraft.osrs.ItemCatalog.get(m.base()), m.base());
            assertEquals(com.runecraft.osrs.ItemDef.Kind.POTION, com.runecraft.osrs.ItemCatalog.get(m.id()).kind(), m.id());
            assertEquals(m.roe() || m.id().equals("energy_mix") || m.id().equals("combat_mix") ? 3 : 6, m.heal(), m.id());
            assertEquals(m.roe(), Recipes.get("mix_" + m.id() + "_caviar") != null, m.id());
        }
        // OSRS Wiki: restore mix is the last roe mix (24), the extended super antifire mix the last of all (99, 78 XP).
        assertEquals(24, HerbloreData.mix("restore_mix").level());
        // Mixes need Barbarian Training's Herblore part (gated by the quest id below).
        assertTrue(HerbloreData.isBarbarianMix("mix_attack_mix"));
        assertTrue(HerbloreData.isBarbarianMix("mix_attack_mix_caviar"));
        assertTrue(!HerbloreData.isBarbarianMix("mix_super_combat_potion"));
        assertNotNull(com.runecraft.osrs.content.ContentRegistry.quest(HerbloreData.BARBARIAN_TRAINING));
        // Goading potion: 54 Herblore, 132 XP, harralander potion (unf) + aldarium.
        Recipe goading = Recipes.get("mix_goading_potion");
        assertEquals(54, goading.level());
        assertEquals(132.0, goading.xpTenths() / 10.0, 1e-9);
        assertTrue(goading.inputs().containsKey("harralander_potion_unf") && goading.inputs().containsKey("aldarium"));
        assertEquals(78.0, HerbloreData.mix("extended_super_antifire_mix").xp(), 1e-9);
        // Sanfew serum: 57 + 63 + 72 XP for the 4-dose steps.
        double sanfew = (Recipes.get("mix_sanfew_step_1").xpTenths() + Recipes.get("mix_sanfew_step_2").xpTenths()
                + Recipes.get("mix_sanfew_serum").xpTenths()) / 10.0;
        assertEquals(192.0, sanfew, 1e-9);
        assertEquals(65, Recipes.get("mix_sanfew_serum").level());
        assertTrue(Recipes.get("mix_sanfew_serum").inputs().containsKey("nail_beast_nails"));
        assertTrue(Recipes.get("mix_relicyms_balm").inputs().containsKey("snake_weed"));
        assertTrue(Recipes.get("mix_serum_207").inputs().containsKey("ashes"));
        assertTrue(Recipes.get("mix_prayer_regeneration_potion").inputs().containsKey("huasca_potion_unf"));
        // Sanfew restores like a super restore.
        com.runecraft.osrs.SkillSet s = new com.runecraft.osrs.SkillSet();
        s.setXpTenths(Skill.ATTACK, com.runecraft.osrs.Experience.xpForLevel(80) * 10);
        s.setBoost(Skill.ATTACK, -30);
        HerbloreData.Effect.SANFEW.apply(s);
        assertEquals(-30 + 8 + 20, s.boost(Skill.ATTACK));
        assertEquals(66, HerbloreData.Effect.REGEN_DURATION / HerbloreData.Effect.REGEN_INTERVAL);
    }

    @Test
    void essencePouchesDegradeAndRepair() {
        RunecraftData.Pouch small = RunecraftData.pouch("small_pouch");
        RunecraftData.Pouch medium = RunecraftData.pouch("medium_pouch");
        RunecraftData.Pouch large = RunecraftData.pouch("large_pouch");
        RunecraftData.Pouch giant = RunecraftData.pouch("giant_pouch");
        assertEquals(3, RunecraftData.capacity(small, 100_000)); // the small pouch never degrades
        assertEquals(6, RunecraftData.capacity(medium, 269));
        assertEquals(3, RunecraftData.capacity(medium, 270));
        assertEquals(9, RunecraftData.capacity(large, 260));
        assertEquals(7, RunecraftData.capacity(large, 261));
        assertEquals(12, RunecraftData.capacity(giant, 129));
        assertEquals(9, RunecraftData.capacity(giant, 130));
        assertTrue(RunecraftData.degraded(giant, 130));
        assertTrue(!RunecraftData.degraded(giant, 0)); // repaired (count reset)
    }
}
