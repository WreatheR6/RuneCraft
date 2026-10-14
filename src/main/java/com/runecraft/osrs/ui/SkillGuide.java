package com.runecraft.osrs.ui;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Recipes;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.Spell;
import com.runecraft.osrs.content.skilling.ConstructionData;
import com.runecraft.osrs.content.skilling.CookingData;
import com.runecraft.osrs.content.skilling.FarmingData;
import com.runecraft.osrs.content.skilling.GatheringData;
import com.runecraft.osrs.content.skilling.HunterData;
import com.runecraft.osrs.content.skilling.MiningData;
import com.runecraft.osrs.content.skilling.RunecraftData;
import com.runecraft.osrs.content.skilling.SailingData;
import com.runecraft.osrs.content.skilling.SailingTraining;
import com.runecraft.osrs.content.skilling.ThievingData;
import com.runecraft.osrs.content.slayer.SlayerCategories;
import com.runecraft.osrs.data.AgilityCourse;
import com.runecraft.osrs.data.Allotment;
import com.runecraft.osrs.data.Fish;
import com.runecraft.osrs.data.Herb;
import com.runecraft.osrs.data.HunterCreature;
import com.runecraft.osrs.data.Rock;
import com.runecraft.osrs.data.RuneAltar;
import com.runecraft.osrs.data.Thieving;
import com.runecraft.osrs.data.Tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The OSRS skill guide (opened by clicking a skill): every level unlock the game knows about,
 * gathered from the existing data tables (trees, rocks, fish, recipes, equipment requirements,
 * prayers, spells, thieving targets, agility courses, slayer categories, Sailing boats...).
 * Entries are sorted by level then name, de-duplicated (lowest level wins), and built lazily once.
 */
public final class SkillGuide {
    /** One line of the guide: the level and what it unlocks. */
    public record Entry(int level, String text) {
    }

    private static Map<Skill, List<Entry>> cache;

    private SkillGuide() {
    }

    public static synchronized List<Entry> entries(Skill skill) {
        if (cache == null) {
            cache = build();
        }
        return cache.getOrDefault(skill, List.of());
    }

    /** Number of entries at or below {@code level}. */
    public static int unlockedCount(Skill skill, int level) {
        int n = 0;
        for (Entry e : entries(skill)) {
            if (e.level() <= level) {
                n++;
            }
        }
        return n;
    }

    private static final class Builder {
        final Map<Skill, Map<String, Entry>> map = new EnumMap<>(Skill.class);

        void add(Skill skill, int level, String text) {
            if (skill == null || text == null || text.isBlank()) {
                return;
            }
            String t = text.trim();
            if (t.endsWith("(flatpack)")) {
                return; // the furniture itself is listed already
            }
            t = Character.toUpperCase(t.charAt(0)) + t.substring(1);
            int lvl = Math.max(1, Math.min(99, level));
            String key = t.toLowerCase(Locale.ROOT);
            Map<String, Entry> m = map.computeIfAbsent(skill, k -> new HashMap<>());
            Entry old = m.get(key);
            if (old == null || old.level() > lvl) {
                m.put(key, new Entry(lvl, t));
            }
        }
    }

    private static Map<Skill, List<Entry>> build() {
        Builder b = new Builder();
        // Each source is guarded so one broken table never empties the whole guide.
        safe(() -> gathering(b));
        safe(() -> production(b));
        safe(() -> combat(b));
        safe(() -> support(b));
        safe(() -> modules(b));
        Map<Skill, List<Entry>> out = new EnumMap<>(Skill.class);
        Comparator<Entry> order = Comparator.comparingInt(Entry::level).thenComparing(e -> e.text().toLowerCase(Locale.ROOT));
        for (Map.Entry<Skill, Map<String, Entry>> e : b.map.entrySet()) {
            List<Entry> list = new ArrayList<>(e.getValue().values());
            list.sort(order);
            out.put(e.getKey(), Collections.unmodifiableList(list));
        }
        return out;
    }

    private static void safe(Runnable r) {
        try {
            r.run();
        } catch (RuntimeException | ExceptionInInitializerError | NoClassDefFoundError ignored) {
            // A data table failed to load; the guide just shows fewer entries.
        }
    }

    private static void gathering(Builder b) {
        for (Tree t : Tree.values()) {
            b.add(Skill.WOODCUTTING, t.level(), t.displayName());
            b.add(Skill.FIREMAKING, t.firemakingLevel(), "Light " + lower(t.logName()));
        }
        for (Rock r : Rock.values()) {
            b.add(Skill.MINING, r.level(), r.displayName());
        }
        for (Fish f : Fish.values()) {
            b.add(Skill.FISHING, f.level(), f.rawName());
        }
        for (Herb h : Herb.values()) {
            b.add(Skill.HERBLORE, h.cleanLevel(), "Clean " + lower(h.displayName()));
            b.add(Skill.FARMING, h.farmingLevel(), h.displayName() + " seed");
        }
        for (Allotment a : Allotment.values()) {
            b.add(Skill.FARMING, a.level(), a.displayName() + " seed");
        }
        for (HunterCreature c : HunterCreature.values()) {
            b.add(Skill.HUNTER, c.level(), c.displayName());
        }
        for (AgilityCourse c : AgilityCourse.values()) {
            b.add(Skill.AGILITY, c.level(), c.displayName());
        }
        for (Thieving.Pickpocket p : Thieving.Pickpocket.values()) {
            b.add(Skill.THIEVING, p.level(), "Pickpocket " + p.displayName());
        }
        for (Thieving.Stall s : Thieving.Stall.values()) {
            b.add(Skill.THIEVING, s.level(), s.displayName());
        }
        for (RuneAltar a : RuneAltar.values()) {
            b.add(Skill.RUNECRAFT, a.level(), a.rune().displayName());
        }
    }

    private static void production(Builder b) {
        for (Recipe r : Recipes.all()) {
            if (r.skill() != null && r.level() > 0) {
                b.add(r.skill(), r.level(), r.name());
            }
        }
    }

    private static void combat(Builder b) {
        for (ItemDef def : ItemCatalog.all()) {
            if (def.equipment() == null || def.equipment().requirements() == null) {
                continue;
            }
            for (Map.Entry<Skill, Integer> req : def.equipment().requirements().entrySet()) {
                if (req.getValue() != null && req.getValue() > 1) {
                    b.add(req.getKey(), req.getValue(), def.name());
                }
            }
        }
        for (Prayer p : Prayer.values()) {
            b.add(Skill.PRAYER, p.level(), p.displayName());
        }
        for (Spell s : Spell.values()) {
            b.add(Skill.MAGIC, s.level(), s.displayName());
        }
        b.add(Skill.HITPOINTS, 10, "Starting Hitpoints level");
        b.add(Skill.HITPOINTS, 99, "Hitpoints cape");
    }

    private static void support(Builder b) {
        for (SlayerCategories.Category c : SlayerCategories.all().values()) {
            if (c.level() > 1) {
                b.add(Skill.SLAYER, c.level(), c.name());
            }
        }
    }

    private static void modules(Builder b) {
        for (GatheringData.ModTree t : GatheringData.TREES) {
            b.add(Skill.WOODCUTTING, t.level(), t.name());
        }
        for (GatheringData.Spot s : GatheringData.SPOTS) {
            for (GatheringData.Catch c : s.catches()) {
                b.add(Skill.FISHING, c.level(), itemName(c.item()));
            }
        }
        for (MiningData.MineRock r : MiningData.ROCKS) {
            b.add(Skill.MINING, r.level(), r.name());
        }
        for (CookingData.NewFish f : CookingData.FISH) {
            b.add(Skill.FISHING, f.fishLevel(), f.name());
        }
        for (ThievingData.Target t : ThievingData.TARGETS) {
            b.add(Skill.THIEVING, t.level(), "Pickpocket " + t.name());
        }
        for (ThievingData.Container c : ThievingData.CONTAINERS) {
            b.add(Skill.THIEVING, c.level(), c.name());
        }
        for (com.runecraft.osrs.content.skilling.AgilityShortcuts.Shortcut s
                : com.runecraft.osrs.content.skilling.AgilityShortcuts.SHORTCUTS) {
            if (s.level() > 0) {
                b.add(Skill.AGILITY, s.level(), "Shortcut: " + s.name());
            }
        }
        for (com.runecraft.osrs.content.skilling.AgilityShortcuts.Shortcut s
                : com.runecraft.osrs.content.skilling.AgilityShortcuts.INDOOR) {
            if (s.level() > 0) {
                b.add(Skill.AGILITY, s.level(), "Shortcut: " + s.name());
            }
        }
        for (HunterData.Impling i : HunterData.IMPLINGS) {
            b.add(Skill.HUNTER, i.level(), i.name());
        }
        for (HunterData.TrapSpot t : HunterData.TRAPS) {
            b.add(Skill.HUNTER, t.level(), t.creature());
        }
        for (FarmingData.Crop c : FarmingData.CROPS) {
            b.add(Skill.FARMING, c.level(), c.name());
        }
        for (RunecraftData.Combination c : RunecraftData.COMBINATIONS) {
            b.add(Skill.RUNECRAFT, c.level(), c.name());
        }
        for (RunecraftData.Pouch p : RunecraftData.POUCHES) {
            b.add(Skill.RUNECRAFT, p.level(), p.name());
        }
        for (ConstructionData.Furniture f : ConstructionData.FURNITURE) {
            b.add(Skill.CONSTRUCTION, f.level(), f.name());
        }
        for (SailingData.Boat boat : SailingData.BOATS) {
            b.add(Skill.SAILING, boat.level(), "Sail a " + lower(boat.name()));
        }
        for (SailingData.Wreck w : SailingData.WRECKS) {
            b.add(Skill.SAILING, w.level(), "Salvage: " + w.name());
        }
        for (SailingTraining.ChartType c : SailingTraining.ChartType.values()) {
            b.add(Skill.SAILING, c.level, "Sea charting: " + lower(c.name()) + " tasks");
        }
        for (SailingTraining.Trial t : SailingTraining.TRIALS) {
            b.add(Skill.SAILING, t.level(), "Barracuda Trials: " + t.name());
        }
        for (SailingTraining.Facility f : com.runecraft.osrs.content.skilling.SailingCrew.allFacilities()) {
            b.add(Skill.SAILING, f.sailing(), "Build: " + f.name());
        }
        for (com.runecraft.osrs.content.skilling.SailingCrew.Crewmate c : com.runecraft.osrs.content.skilling.SailingCrew.CREW) {
            b.add(Skill.SAILING, c.level(), "Recruit crewmate: " + c.name() + " (" + c.location() + ")");
        }
        for (SailingTraining.Part p : SailingTraining.PARTS) {
            if (p.tier() > 0 && !p.boat().equals("raft")) {
                b.add(Skill.SAILING, p.sailing(), "Build: " + p.name() + (p.type() == SailingTraining.PartType.KEEL ? ""
                        : " (" + p.boat() + ")"));
            }
        }
        for (SailingData.Port p : SailingData.PORTS) {
            b.add(Skill.SAILING, p.level(), "Port tasks: " + p.name());
        }
    }

    private static String itemName(String id) {
        ItemDef def = ItemCatalog.get(id);
        if (def != null) {
            return def.name();
        }
        String s = id.replace("minecraft:", "").replace('_', ' ');
        return s.isEmpty() ? id : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String lower(String s) {
        return s.toLowerCase(Locale.ROOT);
    }
}
