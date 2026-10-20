package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.CollectionLogSources;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.npcs.ShopDef;
import com.runecraft.osrs.content.npcs.Shops;
import com.runecraft.osrs.content.minigames.MinigameRules;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Round 28: every teleport item (worn or held) has an in-game source, like the common equipment obtainability test. */
class TeleportItemSourcesTest {
    /**
     * Teleport items whose source the pure data can't see, with where they come from. Keep this short: prefer giving the
     * item a data source (shop, reward, recipe, drop, {@link ItemSources} trade).
     */
    private static final Map<String, String> OTHER_SOURCES = Map.ofEntries(
            Map.entry("royal_seed_pod", "Monkey Madness II quest script reward"),
            Map.entry("pharaohs_sceptre", "Pyramid Plunder sarcophagus roll (minigames session)"),
            Map.entry("ectophial", "Ghosts Ahoy quest script reward"),
            Map.entry("enchanted_lyre", "the Fossegrimen: The Fremennik Trials, then a raw fish offering (LyreService)"),
            Map.entry("slayer_ring_eternal", "Slayer reward crafting: eternal gem and a gold bar (SlayerRewards.Craft)"),
            Map.entry("max_cape", "CapeService: the Wise Old Man's cape stand at 99 in every skill"),
            Map.entry("achievement_diary_cape", "CapeService: every achievement diary complete"));

    /** Teleport items that still have no source in RuneCraft (backlog). Remove an entry when its source is added. */
    private static final Set<String> KNOWN_MISSING = Set.of(
            "amulet_of_eternal_glory", // OSRS: a rare roll when recharging glories at the Fountain of Heroes; not modelled
            "drakans_medallion");      // OSRS: Sins of the Father reward; the quest doesn't give it yet

    @Test
    void everyTeleportItemIsObtainable() {
        Set<String> sources = CollectionLogSources.all();
        sources.addAll(Jewellery.ENCHANT.values()); // enchant spells give the full-charge jewellery
        for (Object[] imbue : com.runecraft.osrs.content.minigames.MinigameRules.Imbues.ITEMS) {
            sources.add((String) imbue[1]); // Nightmare Zone / Soul Wars imbues
        }
        for (String[] pair : com.runecraft.osrs.content.minigames.LastManStanding.IMBUE_SCROLLS.values()) {
            sources.add(pair[1]); // LMS imbue scrolls (ring of wealth (i) keeps the ring's charges)
        }
        for (com.runecraft.osrs.Skill skill : com.runecraft.osrs.Skill.values()) {
            sources.add(Capes.capeId(skill, false)); // skill masters sell the capes at 99 (CapeService)
            sources.add(Capes.capeId(skill, true));
        }
        Set<String> orphans = new TreeSet<>();
        List<Jewellery.Worn> all = new ArrayList<>(Jewellery.all());
        all.addAll(Jewellery.allHeld());
        int checked = 0;
        for (Jewellery.Worn w : all) {
            if (w.teleports().isEmpty() || !ItemCatalog.exists(w.id())) {
                continue;
            }
            checked++;
            if (sources.contains(w.id()) || OTHER_SOURCES.containsKey(w.id()) || KNOWN_MISSING.contains(w.id())
                    || DiaryRewards.areaOf(w.id()) != null) { // diary rewards are claimed from the diary NPCs
                continue;
            }
            // A charged state counts when any charge state of its base (or the uncharged base) can be obtained: used
            // charges come from wearing it, the full one from enchanting/recharging.
            String base = w.chargeBase();
            boolean viaBase = base != null && (sources.contains(base) || all.stream().anyMatch(o -> base.equals(o.chargeBase())
                    && sources.contains(o.id())));
            if (!viaBase) {
                orphans.add(w.id());
            }
        }
        assertTrue(checked > 40, "only " + checked + " teleport items found");
        assertTrue(orphans.isEmpty(), "no drop, shop, recipe, reward or trade gives these teleport items: " + orphans);
    }

    @Test
    void round27TeleportItemsHaveTheirWikiSources() {
        assertEquals(300, price("diangos_toy_store", "chronicle"), "Chronicle: 300 coins at Diango's");
        assertEquals(150, price("diangos_toy_store", "teleport_card"), "teleport card: 150 coins at Diango's");
        assertEquals(10_000, price("mythical_cape_store", "mythical_cape"));
        assertEquals(99_000, price("olafs_music_cape", "music_cape"));
        assertEquals("dragon_slayer_ii", Shops.QUEST_GATES.get("mythical_cape_store")[0]);
        assertEquals("fremennik_trials", Shops.QUEST_GATES.get("yrsas_accoutrements")[0]);
        assertTrue(MinigameRules.GnomeRestaurant.HARD_TIPS.stream()
                .anyMatch(l -> l.item().equals("grand_seed_pod") && l.min() == 5 && l.max() == 5), "grand seed pods are tipped 5 at a time");
        assertTrue(MinigameRules.GnomeRestaurant.TIPS.stream().noneMatch(l -> l.item().equals("grand_seed_pod")),
                "only the four hard-delivery customers tip grand seed pods");
        assertEquals(750, ItemSources.elunedCost(0));
        assertEquals(600, ItemSources.elunedCost(1));
        assertEquals(150, ItemSources.elunedCost(4));
        assertEquals(150, ItemSources.elunedCost(9));
        assertEquals(3, ItemSources.elunedCharges(false));
        assertEquals(5, ItemSources.elunedCharges(true));
        // Basalts: 1 basalt + 3 efh (icy) or 3 urt (stony) + 1 te salt.
        assertEquals("icy_basalt", ItemSources.basaltFor(counts(Map.of("basalt", 1, "te_salt", 1, "efh_salt", 3))).item());
        assertEquals("stony_basalt", ItemSources.basaltFor(counts(Map.of("basalt", 1, "te_salt", 1, "urt_salt", 3))).item());
        assertEquals(null, ItemSources.basaltFor(counts(Map.of("basalt", 1, "efh_salt", 3))));
        for (ItemSources.SaltRock r : ItemSources.SALT_ROCKS) {
            assertTrue(ItemCatalog.exists(r.item()), r.item());
            assertEquals(72, r.level());
        }
        for (String id : new String[]{"eternal_teleport_crystal", "enhanced_crystal_teleport_seed", "tarnished_locket", "grand_seed_pod"}) {
            assertTrue(ItemCatalog.exists(id), id);
        }
        assertNotNull(com.runecraft.osrs.Recipes.get("sing_eternal_teleport_crystal"));
        assertNotNull(com.runecraft.osrs.Recipes.get("mix_extended_anti_venom_plus"));
    }

    private static java.util.function.ToIntFunction<String> counts(Map<String, Integer> m) {
        return id -> m.getOrDefault(id, 0);
    }

    private static int price(String shopId, String item) {
        ShopDef shop = Shops.get(shopId);
        assertNotNull(shop, shopId);
        ShopDef.Stock s = shop.entry(item);
        assertNotNull(s, shopId + " sells " + item);
        return (int) ((long) s.resolvedValue() * shop.sellPm() / 1000);
    }
}
