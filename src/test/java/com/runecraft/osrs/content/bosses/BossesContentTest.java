package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Recipes;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.data.Drop;
import com.runecraft.osrs.model.ModelSpec;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Data checks for the bosses module: stats, drops, models, places, rotations and recipes. */
class BossesContentTest {
    private final BossesContent module = new BossesContent();
    private final List<BossDef> bosses = module.bosses();

    private BossDef boss(String id) {
        return bosses.stream().filter(b -> b.id.equals(id)).findFirst().orElseThrow(() -> new AssertionError("missing boss " + id));
    }

    private static AttackDef attack(BossDef b, String name) {
        return b.attacks.stream().filter(a -> a.name().equals(name)).findFirst().orElseThrow(() -> new AssertionError(b.id + " " + name));
    }

    @Test
    void everyRequestedBossExists() {
        for (String id : new String[]{"giant_mole", "kalphite_queen", "kalphite_queen_airborne", "dagannoth_rex", "dagannoth_prime",
                "dagannoth_supreme", "sarachnis", "zulrah", "zulrah_magma", "zulrah_tanzanite", "vorkath", "kraken",
                "thermonuclear_smoke_devil", "cerberus", "abyssal_sire", "alchemical_hydra", "alchemical_hydra_blue",
                "alchemical_hydra_red", "alchemical_hydra_enraged", "dusk", "dawn", "corporeal_beast", "skotizo", "hespori",
                "crystalline_hunllef", "corrupted_hunllef", "the_nightmare", "phosanis_nightmare", "phantom_muspah", "duke_sucellus",
                "the_leviathan", "the_whisperer", "vardorvis", "scurrius", "araxxor", "the_hueycoatl", "amoxliatl",
                "branda_the_fire_queen", "eldric_the_ice_king", "zalcano", "the_mimic", "sol_heredit", "yama", "doom_of_mokhaiotl",
                "gemstone_crab", "shellbane_gryphon", "brutus", "demonic_brutus", "maggot_king"}) {
            assertNotNull(ContentRegistry.boss(id), id);
        }
    }

    @Test
    void realStatsAndMaxHits() {
        assertEquals(725, boss("zulrah").combatLevel);
        assertEquals(500, boss("zulrah").hitpoints);
        assertEquals(41, boss("zulrah").maxHit());
        assertEquals(732, boss("vorkath").combatLevel);
        assertEquals(750, boss("vorkath").hitpoints);
        assertEquals(73, attack(boss("vorkath"), "dragonfire").maxHit());
        assertEquals(121, attack(boss("vorkath"), "dragonfire_bomb").maxHit());
        assertEquals(2000, boss("corporeal_beast").hitpoints);
        assertEquals(785, boss("corporeal_beast").combatLevel);
        assertEquals(600, boss("cerberus").hitpoints);
        assertEquals(23, attack(boss("cerberus"), "bite").maxHit());
        assertEquals(255, boss("kalphite_queen").hitpoints);
        assertEquals(31, boss("kalphite_queen").maxHit());
        assertEquals(1100, boss("alchemical_hydra").hitpoints);
        assertEquals(95, boss("alchemical_hydra").slayerLevel);
        assertEquals(91, boss("cerberus").slayerLevel);
        assertEquals(87, boss("kraken").slayerLevel);
        assertEquals(93, boss("thermonuclear_smoke_devil").slayerLevel);
        assertEquals(85, boss("abyssal_sire").slayerLevel);
        assertEquals(75, boss("dusk").slayerLevel);
        assertEquals(200, boss("giant_mole").hitpoints);
        assertEquals(21, boss("giant_mole").maxHit());
        assertEquals(50, boss("dagannoth_prime").maxHit());
    }

    @Test
    void immunitiesPerForm() {
        // The Dagannoth Kings have no immunities in OSRS, only huge defence bonuses against two styles.
        assertTrue(boss("dagannoth_rex").immuneTo.isEmpty());
        assertTrue(boss("dagannoth_rex").defenceBonus(AttackType.SLASH) > 200 && boss("dagannoth_rex").defenceBonus(AttackType.MAGIC) < 20);
        assertTrue(boss("dagannoth_prime").defenceBonus(AttackType.MAGIC) > 200
                && boss("dagannoth_prime").defenceBonus(AttackType.RANGED) < 20);
        assertTrue(boss("dagannoth_supreme").defenceBonus(AttackType.RANGED) > 500
                && boss("dagannoth_supreme").defenceBonus(AttackType.SLASH) < 20);
        assertTrue(boss("zulrah").immuneTo.contains(AttackType.STAB));
        assertTrue(boss("dusk").immuneTo.contains(AttackType.MAGIC));
        assertTrue(boss("dawn").immuneTo.contains(AttackType.CRUSH));
        assertTrue(boss("kraken").immuneTo.contains(AttackType.CRUSH));
        // Zulrah's forms have different magic/ranged defences.
        assertTrue(boss("zulrah").defenceBonus(AttackType.MAGIC) < 0);
        assertEquals(300, boss("zulrah_tanzanite").defenceBonus(AttackType.MAGIC));
        assertEquals(300, boss("zulrah_magma").defenceBonus(AttackType.RANGED));
    }

    @Test
    void bigAttacksAreTelegraphed() {
        assertTrue(attack(boss("vorkath"), "dragonfire_bomb").telegraphTicks() >= 3);
        assertEquals(AttackDef.Special.SKY_STRIKE, attack(boss("vorkath"), "dragonfire_bomb").special());
        assertTrue(attack(boss("zulrah_magma"), "tail_slam").telegraphTicks() >= 3);
        assertTrue(attack(boss("cerberus"), "lava_pool").telegraphTicks() >= 3);
        assertTrue(Dt2Bosses.DUKE_GAZE.telegraphTicks() >= 4);
        assertTrue(SlayerBosses.SIRE_EXPLOSION.telegraphTicks() >= 6);
        assertTrue(SlayerBosses.DUSK_EXPLOSION.telegraphTicks() >= 4);
        assertTrue(Dt2Bosses.WHISPER_SCREECH.telegraphTicks() >= 4);
    }

    @Test
    void everyBossHasACustomModelAndPlace() {
        Map<String, ModelSpec> models = ContentRegistry.models();
        for (BossDef b : bosses) {
            assertEquals(b.id, b.model, b.id);
            assertTrue(models.containsKey(b.id), "no model for " + b.id);
            if (!b.raidOnly) {
                assertTrue(Landmark.byName(b.place) != null || ContentRegistry.place(b.place) != null, b.id + " place " + b.place);
            }
        }
        for (MonsterDef m : module.monsters()) {
            assertTrue(models.containsKey(m.id) || m.skin != null, m.id);
        }
    }

    @Test
    void modelsAreDetailedButWithinBudget() {
        for (ModelSpec m : module.models()) {
            int boxes = m.parts().stream().mapToInt(p -> p.boxes.size()).sum();
            assertTrue(boxes <= 44, m.id() + " has " + boxes + " boxes");
            assertTrue(boxes >= 3, m.id());
            assertTrue(m.texWidth() <= 512, m.id() + " texture " + m.texWidth());
            assertTrue(m.parts().stream().anyMatch(p -> p.role == ModelSpec.Role.HEAD || p.role == ModelSpec.Role.SPIN
                    || p.role == ModelSpec.Role.TENTACLE || p.role == ModelSpec.Role.FLOAT), m.id() + " has no animated focus part");
        }
        // Signature anatomy
        assertEquals(3, countRole("cerberus", ModelSpec.Role.HEAD));
        assertEquals(4, countRole("alchemical_hydra", ModelSpec.Role.HEAD));
        assertEquals(1, countRole("alchemical_hydra_enraged", ModelSpec.Role.HEAD));
        assertTrue(countRole("kraken", ModelSpec.Role.TENTACLE) >= 8);
        assertTrue(countRole("zulrah", ModelSpec.Role.NECK) >= 3);
        assertTrue(countRole("zulrah", ModelSpec.Role.TAIL) >= 3);
        assertTrue(countRole("kalphite_queen", ModelSpec.Role.LEG_EXTRA) >= 6);
        assertTrue(countRole("araxxor", ModelSpec.Role.LEG_EXTRA) >= 8);
        assertTrue(countRole("vorkath", ModelSpec.Role.WING_LEFT) == 1);
    }

    private int countRole(String model, ModelSpec.Role role) {
        return (int) ContentRegistry.models().get(model).parts().stream().filter(p -> p.role == role).count();
    }

    @Test
    void dropsPetsAndCollectionLog() {
        Set<String> pets = new HashSet<>();
        for (BossDef b : bosses) {
            for (Drop d : b.drops) {
                assertTrue(ItemCatalog.exists(d.item()), b.id + " drops unknown " + d.item());
                assertTrue(d.min() <= d.max() && d.rarity() >= 1, b.id + " " + d);
            }
            if (b.pet != null) {
                assertTrue(BossItems.PETS.containsKey(b.pet), b.id + " pet " + b.pet);
                pets.add(b.pet);
            }
            boolean main = !b.raidOnly || b.id.startsWith("dagannoth");
            // The Gemstone Crab is an AFK gem boss with no pet or collection log page.
            if (main && !b.id.equals("the_mimic") && !b.id.equals("hespori") && !b.id.equals("gemstone_crab")) {
                assertNotNull(b.pet, b.id + " has no pet");
                if (!b.id.equals("giant_mole")) {
                    assertTrue(b.drops.stream().anyMatch(d -> d.rarity() >= 24), b.id + " has no uniques for its collection log");
                }
            }
        }
        assertEquals(BossItems.PETS.keySet(), pets, "every pet item belongs to a boss");
        // Known rates
        assertTrue(boss("vorkath").drops.contains(Drop.of("vorkaths_head", 1, 1, 50)));
        assertTrue(boss("corporeal_beast").drops.contains(Drop.of("elysian_sigil", 1, 1, 4095)));
        assertTrue(boss("zulrah").drops.contains(Drop.of("tanzanite_mutagen", 1, 1, 6553)));
        assertTrue(boss("kraken").drops.contains(Drop.of("trident_of_the_seas", 1, 1, 512)));
        assertTrue(boss("cerberus").drops.contains(Drop.of("primordial_crystal", 1, 1, 520)));
    }

    @Test
    void secondaryFormsCarryNoLoot() {
        for (String id : new String[]{"zulrah_magma", "zulrah_tanzanite", "kalphite_queen_airborne", "alchemical_hydra_blue",
                "alchemical_hydra_red", "alchemical_hydra_enraged", "dawn", "eldric_the_ice_king"}) {
            assertTrue(boss(id).raidOnly, id);
            assertTrue(boss(id).drops.isEmpty(), id);
            assertTrue(boss(id).minion == null, id);
        }
    }

    @Test
    void zulrahRotations() {
        assertEquals(4, ZulrahVorkath.ROTATIONS.size());
        for (List<ZulrahVorkath.Step> rot : ZulrahVorkath.ROTATIONS) {
            ZulrahVorkath.Step first = rot.get(0);
            assertEquals(ZulrahVorkath.Form.SERPENTINE, first.form());
            assertEquals(ZulrahVorkath.Spot.CENTRE, first.spot());
            assertTrue(first.clouds());
            assertTrue(rot.stream().anyMatch(s -> s.form() == ZulrahVorkath.Form.MAGMA));
            assertTrue(rot.stream().anyMatch(s -> s.form() == ZulrahVorkath.Form.JAD_RANGED || s.form() == ZulrahVorkath.Form.JAD_MAGIC));
            for (ZulrahVorkath.Step s : rot) {
                assertNotNull(ContentRegistry.boss(s.form().bossId));
                assertTrue(s.attacks() > 0);
            }
        }
    }

    @Test
    void hydraPhases() {
        SlayerBosses.HydraPhase[] phases = SlayerBosses.HydraPhase.values();
        assertEquals(4, phases.length);
        for (int i = 0; i < phases.length; i++) {
            assertNotNull(ContentRegistry.boss(phases[i].bossId));
            assertEquals(4 - i, phases[i].heads);
            assertEquals(phases[i].heads, countRole(phases[i].bossId, ModelSpec.Role.HEAD));
            if (i > 0) {
                assertTrue(phases[i].endsAtPercent < phases[i - 1].endsAtPercent);
            }
        }
        assertEquals(null, SlayerBosses.HydraPhase.ENRAGED.vent);
        assertEquals(0.25, SlayerBosses.HYDRA_SHIELD_MULTIPLIER, 1e-9);
    }

    @Test
    void itemsHaveRealStats() {
        ItemDef tentacle = ItemCatalog.get("abyssal_tentacle");
        assertEquals(90, tentacle.equipment().stats().slashAttack());
        assertEquals(86, tentacle.equipment().stats().meleeStrength());
        ItemDef blowpipe = ItemCatalog.get("toxic_blowpipe");
        assertEquals(20, blowpipe.equipment().stats().rangedStrength());
        assertEquals(3, blowpipe.equipment().attackSpeed());
        assertEquals(102, ItemCatalog.get("abyssal_bludgeon").equipment().stats().crushAttack());
        assertEquals(128, ItemCatalog.get("bow_of_faerdhinen").equipment().stats().rangedAttack());
        assertEquals(10, BossItems.ACCESSORIES.get("occult_necklace").stats().magicDamagePercent());
        assertEquals(12, BossItems.ACCESSORIES.get("dragonbone_necklace").stats().prayer());
        for (String id : BossItems.ACCESSORIES.keySet()) {
            assertNotNull(ItemCatalog.get(id), id);
        }
        for (String id : new String[]{"trident_of_the_seas", "trident_of_the_swamp", "magic_fang", "tanzanite_fang", "serpentine_visage",
                "serpentine_helm", "dragonbone_necklace", "vorkaths_head", "skeletal_visage", "kraken_tentacle", "occult_necklace",
                "smoke_battlestaff", "primordial_crystal", "pegasian_crystal", "eternal_crystal", "abyssal_dagger", "unsired",
                "hydra_claw", "hydra_leather", "hydra_tail", "ferocious_gloves", "dragon_hunter_lance", "brimstone_ring", "granite_maul",
                "granite_ring", "black_tourmaline_core", "spirit_shield", "arcane_sigil", "spectral_sigil", "elysian_sigil",
                "elysian_spirit_shield", "enhanced_crystal_weapon_seed", "blade_of_saeldor", "crystal_body", "inquisitors_mace",
                "nightmare_staff", "harmonised_orb", "volatile_orb", "eldritch_orb", "venator_bow", "soulreaper_axe", "virtus_robe_top",
                "chromium_ingot", "ultor_ring", "bellator_ring", "magus_ring", "venator_ring", "noxious_halberd", "echo_crystal"}) {
            assertNotNull(ItemCatalog.get(id), id);
        }
    }

    @Test
    void recipesUseOnlyKnownItems() {
        Set<String> ours = new HashSet<>();
        module.recipes(r -> ours.add(r.id()));
        assertTrue(ours.size() >= 25);
        for (String id : ours) {
            Recipe r = Recipes.get(id);
            assertNotNull(r, id);
            r.inputs().keySet().forEach(i -> assertTrue(ItemCatalog.exists(i), id + " input " + i));
            r.outputs().keySet().forEach(i -> assertTrue(ItemCatalog.exists(i), id + " output " + i));
        }
        // DT2 rings are forged from DK rings dropped in this module.
        assertTrue(Recipes.get("forge_ultor_ring").inputs().containsKey("berserker_ring"));
        assertTrue(boss("dagannoth_rex").drops.stream().anyMatch(d -> d.item().equals("berserker_ring")));
    }

    @Test
    void unsiredTable() {
        int total = BossItems.UNSIRED_TABLE.stream().mapToInt(Map.Entry::getValue).sum();
        assertEquals(128, total);
        for (Map.Entry<String, Integer> e : BossItems.UNSIRED_TABLE) {
            assertNotNull(ItemCatalog.get(e.getKey()), e.getKey());
        }
    }

    @Test
    void placesAreRealCoordinates() {
        for (PlaceDef p : module.places()) {
            assertTrue(p.osrsX() > 1000 && p.osrsX() < 4000, p.id());
            // The Sailing islands (the Great Conch) lie south of the old map edge.
            assertTrue(p.osrsY() > 2300 && p.osrsY() < 6500, p.id());
        }
        PlaceDef zul = ContentRegistry.place("zul_andra");
        assertEquals(2200, zul.osrsX());
        assertFalse(module.blocks().isEmpty());
    }
}
