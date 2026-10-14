package com.runecraft.osrs;

import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Boss;
import com.runecraft.osrs.data.Drop;
import com.runecraft.osrs.data.DropTables;
import com.runecraft.osrs.data.Monster;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every item id that a drop or loot table can hand out must resolve to a real item: module monster and
 * boss drops, the built-in monster/boss enums, the shared gem/rare/mega tables, and every static
 * table of loot records in the pure-Java core (any record with a String {@code item} component, found by
 * reflection), so a typo or a missing item (e.g. rune_spear on the rare drop table) can't silently roll
 * nothing.
 */
class DropReferencesTest {
    @Test
    void everyDroppedItemExists() throws Exception {
        Set<String> missing = new TreeSet<>();
        for (MonsterDef m : ContentRegistry.monsters()) {
            m.drops.forEach(d -> check(d.item(), "monster " + m.id, missing));
        }
        for (BossDef b : ContentRegistry.bosses()) {
            b.drops.forEach(d -> check(d.item(), "boss " + b.id, missing));
        }
        for (Monster m : Monster.values()) {
            m.drops().forEach(d -> check(d.item(), "Monster." + m, missing));
        }
        for (Boss b : Boss.values()) {
            b.drops().forEach(d -> check(d.item(), "Boss." + b, missing));
        }
        for (String t : List.of("#gems", "#rare", "#mega")) {
            DropTables.table(t).forEach(e -> check(e.item(), "table " + t, missing));
        }
        for (Class<?> c : coreClasses()) {
            scanStatics(c, missing);
        }
        assertTrue(missing.isEmpty(), "Unknown dropped items:\n  " + String.join("\n  ", missing));
    }

    private static void check(String item, String where, Set<String> missing) {
        if (item == null || item.isEmpty() || item.startsWith("#") || item.startsWith("minecraft:")) {
            return;
        }
        String base = Drop.baseOf(item);
        if (!ItemCatalog.exists(base)) {
            missing.add(item + " (" + where + ")");
        } else if (Drop.dosesOf(item) > 0 && ItemCatalog.get(base).kind() != ItemDef.Kind.POTION) {
            missing.add(item + " (" + where + ": dose suffix on a non-potion)");
        }
    }

    private static List<Class<?>> coreClasses() throws IOException {
        Path root = Path.of("src/main/java");
        List<Class<?>> out = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root.resolve("com/runecraft/osrs"))) {
            for (Path p : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String name = root.relativize(p).toString().replace('/', '.').replace('\\', '.');
                name = name.substring(0, name.length() - ".java".length());
                try {
                    out.add(Class.forName(name));
                } catch (Throwable ignored) {
                    // classes that can't load standalone are covered by their own tests
                }
            }
        }
        return out;
    }

    private static void scanStatics(Class<?> c, Set<String> missing) {
        Field[] fields;
        try {
            fields = c.getDeclaredFields();
        } catch (Throwable t) {
            return;
        }
        for (Field f : fields) {
            if (!Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive()) {
                continue;
            }
            try {
                f.setAccessible(true);
                walk(f.get(null), c.getSimpleName() + "." + f.getName(), missing, new IdentityHashMap<>(), 0);
            } catch (Throwable ignored) {
                // inaccessible or failing initialiser: nothing to check
            }
        }
    }

    private static void walk(Object o, String where, Set<String> missing, Map<Object, Boolean> seen, int depth) {
        if (o == null || depth > 6 || o instanceof String || o instanceof Number || o instanceof Class<?>
                || seen.put(o, Boolean.TRUE) != null) {
            return;
        }
        if (o instanceof Drop d) {
            check(d.item(), where, missing);
        } else if (o instanceof Collection<?> col) {
            for (Object x : col) {
                walk(x, where, missing, seen, depth + 1);
            }
        } else if (o instanceof Map<?, ?> map) {
            for (Object x : map.values()) {
                walk(x, where, missing, seen, depth + 1);
            }
        } else if (o instanceof Object[] arr) {
            for (Object x : arr) {
                walk(x, where, missing, seen, depth + 1);
            }
        } else if (o instanceof Enum<?> e) {
            for (Field f : e.getDeclaringClass().getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive()) {
                    continue;
                }
                try {
                    f.setAccessible(true);
                    walk(f.get(o), where + "." + e.name(), missing, seen, depth + 1);
                } catch (Throwable ignored) {
                    // not reachable
                }
            }
        } else if (o.getClass().isRecord()) {
            for (RecordComponent rc : o.getClass().getRecordComponents()) {
                try {
                    rc.getAccessor().setAccessible(true);
                    Object v = rc.getAccessor().invoke(o);
                    if (rc.getName().equals("item") && v instanceof String s) {
                        check(s, where, missing);
                    } else if (v instanceof Collection<?> || v instanceof Map<?, ?> || v instanceof Object[]) {
                        walk(v, where, missing, seen, depth + 1);
                    }
                } catch (Throwable ignored) {
                    // accessor not reachable
                }
            }
        }
    }
}
