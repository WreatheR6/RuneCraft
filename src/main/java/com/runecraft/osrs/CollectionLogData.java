package com.runecraft.osrs;

import com.runecraft.osrs.data.Boss;
import com.runecraft.osrs.data.Drop;
import com.runecraft.osrs.data.Gem;
import com.runecraft.osrs.data.Herb;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Collection log pages: each page lists the notable items it tracks. */
public final class CollectionLogData {
    public record Page(String category, String name, List<String> items) {
    }

    private static final List<Page> PAGES = new ArrayList<>();
    private static final Set<String> TRACKED = new LinkedHashSet<>();

    static {
        for (Boss b : Boss.values()) {
            if (b.questBoss()) {
                continue;
            }
            List<String> uniques = new ArrayList<>();
            for (Drop d : b.drops()) {
                if (logged(d)) {
                    uniques.add(d.item());
                }
            }
            if (b == Boss.TZTOK_JAD) {
                uniques.add(0, "fire_cape");
            }
            if (!uniques.isEmpty()) {
                page("Bosses", b.displayName(), uniques);
            }
        }
        page("Bosses", "Boss keys", List.of("giant_key", "mossy_key"));
        page("Skilling", "Rooftop Agility", List.of("mark_of_grace"));
        page("Skilling", "Hunter", List.of("ferret", "grey_chinchompa", "red_chinchompa", "black_chinchompa"));
        List<String> gems = new ArrayList<>();
        for (Gem g : Gem.values()) {
            gems.add(g.uncutItem());
        }
        page("Skilling", "Gems", gems);
        List<String> herbs = new ArrayList<>();
        for (Herb h : Herb.values()) {
            herbs.add(h.grimyItem());
        }
        page("Other", "Herbs", herbs);
        page("Other", "Rune altars", List.of("air_rune", "mind_rune", "water_rune", "earth_rune", "fire_rune", "body_rune",
                "cosmic_rune", "chaos_rune", "astral_rune", "nature_rune", "law_rune", "death_rune", "blood_rune",
                "soul_rune", "wrath_rune"));
        page("Other", "Quest rewards", List.of("silverlight", "air_talisman", "amulet_of_accuracy"));
        for (com.runecraft.osrs.content.BossDef b : com.runecraft.osrs.content.ContentRegistry.bosses()) {
            List<String> uniques = new ArrayList<>();
            for (Drop d : b.drops) {
                if (logged(d) && !uniques.contains(d.item())) {
                    uniques.add(d.item());
                }
            }
            if (b.pet != null && !uniques.contains(b.pet)) {
                uniques.add(b.pet);
            }
            if (!uniques.isEmpty()) {
                page(b.category, b.name, uniques);
            }
        }
        for (com.runecraft.osrs.content.ContentModule m : com.runecraft.osrs.content.Modules.ALL) {
            for (Page p : m.collectionLog()) {
                page(p.category(), p.name(), p.items());
            }
        }
    }

    private CollectionLogData() {
    }

    /** A boss drop goes on its log page when rare (1/24 or rarer); tertiary clue scrolls never do. */
    static boolean logged(Drop d) {
        return d.rarity() >= 24 && !d.item().startsWith("#") && !d.item().startsWith("clue_scroll");
    }

    private static void page(String category, String name, List<String> items) {
        PAGES.add(new Page(category, name, List.copyOf(items)));
        TRACKED.addAll(items);
    }

    public static List<Page> pages() {
        return PAGES;
    }

    public static boolean tracked(String item) {
        return TRACKED.contains(item);
    }

    public static int totalSlots() {
        return TRACKED.size();
    }

    public static Map<String, List<Page>> byCategory() {
        Map<String, List<Page>> map = new LinkedHashMap<>();
        for (Page p : PAGES) {
            map.computeIfAbsent(p.category(), k -> new ArrayList<>()).add(p);
        }
        return map;
    }
}
