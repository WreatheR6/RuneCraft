package com.runecraft.osrs;

import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Boss;
import com.runecraft.osrs.data.Drop;
import com.runecraft.osrs.data.Fish;
import com.runecraft.osrs.data.Gem;
import com.runecraft.osrs.data.Herb;
import com.runecraft.osrs.data.HunterCreature;
import com.runecraft.osrs.data.Monster;
import com.runecraft.osrs.data.Rock;
import com.runecraft.osrs.data.Tree;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Every way the pure data says an item can be obtained: monster/boss drops (and their pets and keys),
 * shared drop tables, recipe outputs, quest rewards, gathering (logs, ores, fish, herbs, gems, hunter loot)
 * and every static loot/reward table in the core (records with an {@code item} or {@code pet} component:
 * clue caskets, reward shops, minigame reward pools, thieving loot, pet rolls...).
 */
public final class CollectionLogSources {
    private CollectionLogSources() {
    }

    public static Set<String> all() {
        Set<String> out = new HashSet<>();
        for (Boss b : Boss.values()) {
            drops(out, b.drops());
            drops(out, com.runecraft.osrs.content.godwars.GwdBosses.builtinExtraDrops(b.entityId()));
            if (b.key() != null) {
                out.add(b.key());
            }
        }
        for (Monster m : Monster.values()) {
            drops(out, m.drops());
        }
        for (MonsterDef m : ContentRegistry.monsters()) {
            drops(out, m.drops);
        }
        for (BossDef b : ContentRegistry.bosses()) {
            drops(out, b.drops);
            if (b.pet != null) {
                out.add(b.pet);
            }
        }
        for (Recipe r : Recipes.all()) {
            out.addAll(r.outputs().keySet());
        }
        for (QuestInfo q : ContentRegistry.quests()) {
            for (String reward : q.itemRewards()) {
                out.add(reward.split(":")[0]);
            }
        }
        for (Tree t : Tree.values()) {
            out.add(t.logItem());
        }
        for (Rock r : Rock.values()) {
            out.add(r.oreItem());
        }
        for (Fish f : Fish.values()) {
            out.add(f.rawItem());
        }
        for (Herb h : Herb.values()) {
            out.add(h.grimyItem());
        }
        for (Gem g : Gem.values()) {
            out.add(g.uncutItem());
        }
        for (HunterCreature h : HunterCreature.values()) {
            out.addAll(h.loot());
        }
        for (com.runecraft.osrs.content.systems.ClueScrolls.Tier t : com.runecraft.osrs.content.systems.ClueScrolls.Tier.values()) {
            out.addAll(com.runecraft.osrs.content.systems.ClueRewards.uniques(t));
            out.addAll(com.runecraft.osrs.content.systems.ClueRewards.megaRares(t));
        }
        for (com.runecraft.osrs.content.extras.Pets.PetDef p : com.runecraft.osrs.content.extras.Pets.all()) {
            if (!p.sources().isEmpty() || p.skill() != null) { // boss/minigame sources, or a skilling roll (PetService)
                out.add(p.id());
            }
        }
        for (Object[] a : com.runecraft.osrs.content.minigames.MinigameItems.ARTEFACTS) {
            out.add((String) a[0]); // Pyramid Plunder urns/chests
        }
        for (String tier : com.runecraft.osrs.content.minigames.MinigameItems.DEFENDER_TIERS) {
            out.add(tier + "_defender"); // Warriors' Guild cyclopes (MinigameRules.WarriorsGuild picks the next tier)
        }
        scanStaticTables(out);
        return out;
    }

    private static void drops(Set<String> out, Collection<Drop> drops) {
        for (Drop d : drops) {
            out.add(d.item());
        }
    }

    /** Reflectively collects items from every static table of loot-like records in com.runecraft.osrs. */
    private static void scanStaticTables(Set<String> out) {
        Path root;
        try {
            root = Path.of(Recipe.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        Map<Object, Boolean> seen = new IdentityHashMap<>();
        try (Stream<Path> files = Files.walk(root.resolve("com/runecraft/osrs"))) {
            for (Path f : (Iterable<Path>) files::iterator) {
                String name = root.relativize(f).toString();
                if (!name.endsWith(".class")) {
                    continue;
                }
                Class<?> c;
                try {
                    c = Class.forName(name.substring(0, name.length() - 6).replace('/', '.').replace('\\', '.'), true,
                            CollectionLogSources.class.getClassLoader());
                } catch (Throwable t) {
                    continue;
                }
                for (Field field : c.getDeclaredFields()) {
                    if (!Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) {
                        continue;
                    }
                    try {
                        field.setAccessible(true);
                        collect(out, field.get(null), seen, 0);
                    } catch (Throwable ignored) {
                        // inaccessible field
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void collect(Set<String> out, Object o, Map<Object, Boolean> seen, int depth) {
        if (o == null || depth > 4 || o instanceof String || o instanceof Number || o instanceof Enum<?> || seen.put(o, true) != null) {
            return;
        }
        if (o instanceof Map.Entry<?, ?> entry && entry.getKey() instanceof String key && entry.getValue() instanceof Integer) {
            out.add(key); // weighted table entry (item -> weight), e.g. the unsired table
        } else if (o instanceof Collection<?> col) {
            col.forEach(e -> collect(out, e, seen, depth + 1));
        } else if (o instanceof Map<?, ?> map) {
            map.values().forEach(e -> collect(out, e, seen, depth + 1));
        } else if (o.getClass().isArray() && !o.getClass().getComponentType().isPrimitive()) {
            for (int i = 0; i < Array.getLength(o); i++) {
                collect(out, Array.get(o, i), seen, depth + 1);
            }
        } else if (o.getClass().isRecord()) {
            for (RecordComponent rc : o.getClass().getRecordComponents()) {
                try {
                    Method m = rc.getAccessor();
                    m.setAccessible(true);
                    Object v = m.invoke(o);
                    if ((rc.getName().equals("item") || rc.getName().equals("pet")) && v instanceof String s) {
                        out.add(s);
                    } else if (!(v instanceof String)) {
                        collect(out, v, seen, depth + 1);
                    }
                } catch (Throwable ignored) {
                    // skip
                }
            }
        } else if (o.getClass().getName().startsWith("com.runecraft.osrs")) {
            // Plain data holders (e.g. reward shops): look at their instance fields.
            for (Field f : o.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive()) {
                    continue;
                }
                try {
                    f.setAccessible(true);
                    collect(out, f.get(o), seen, depth + 1);
                } catch (Throwable ignored) {
                    // skip
                }
            }
        }
    }
}
