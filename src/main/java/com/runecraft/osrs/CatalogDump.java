package com.runecraft.osrs;

import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.CombatProfile;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.data.FishingSpot;
import com.runecraft.osrs.data.Herb;
import com.runecraft.osrs.data.Rock;
import com.runecraft.osrs.data.RuneAltar;
import com.runecraft.osrs.data.Thieving;
import com.runecraft.osrs.data.Tree;
import com.runecraft.osrs.model.ModelSpec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Writes the item/block/entity/model catalog as JSON for tools/gen_assets.py and tools/gen_worldgen.py. */
public final class CatalogDump {
    private CatalogDump() {
    }

    // ---- tiny JSON writer ----
    private static String q(String s) {
        return s == null ? "null" : "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String obj(Object... kv) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < kv.length; i += 2) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(q((String) kv[i])).append(": ").append(val(kv[i + 1]));
        }
        return sb.append('}').toString();
    }

    private static String val(Object v) {
        if (v == null) {
            return "null";
        }
        if (v instanceof String s) {
            return s.startsWith("{") || s.startsWith("[") ? s : q(s);
        }
        if (v instanceof Float f) {
            return String.format(Locale.ROOT, "%s", f);
        }
        if (v instanceof int[] arr) {
            List<String> parts = new ArrayList<>();
            for (int a : arr) {
                parts.add(String.valueOf(a));
            }
            return "[" + String.join(", ", parts) + "]";
        }
        if (v instanceof List<?> list) {
            List<String> parts = new ArrayList<>();
            for (Object o : list) {
                parts.add(val(o));
            }
            return "[" + String.join(", ", parts) + "]";
        }
        return String.valueOf(v);
    }

    private static String arr(List<String> items) {
        return "[\n    " + String.join(",\n    ", items) + "\n  ]";
    }

    public static void main(String[] args) throws IOException {
        List<String> items = new ArrayList<>();
        for (ItemDef d : ItemCatalog.all()) {
            items.add(obj("id", d.id(), "name", d.name(), "kind", d.kind().name(), "shape", d.shape(), "color", d.color(),
                    "tier", d.tier(), "slot", d.equipment() == null ? null : d.equipment().slot().name()));
        }
        List<String> trees = new ArrayList<>();
        for (Tree t : Tree.values()) {
            trees.add(obj("id", t.id(), "name", t.displayName(), "color", t.color()));
        }
        List<String> rocks = new ArrayList<>();
        for (Rock r : Rock.values()) {
            rocks.add(obj("id", r.id(), "name", r.displayName(), "color", r.color()));
        }
        List<String> spots = new ArrayList<>();
        for (FishingSpot s : FishingSpot.values()) {
            spots.add(obj("id", s.blockId(), "name", s.displayName()));
        }
        List<String> altars = new ArrayList<>();
        for (RuneAltar a : RuneAltar.values()) {
            altars.add(obj("id", a.blockId(), "name", a.rune().displayName().replace(" rune", "") + " Altar"));
        }
        List<String> stalls = new ArrayList<>();
        for (Thieving.Stall s : Thieving.Stall.values()) {
            stalls.add(obj("id", s.blockId(), "name", s.displayName()));
        }
        List<String> monsters = new ArrayList<>();
        for (MonsterDef m : ContentRegistry.monsters()) {
            List<String> spawns = new ArrayList<>();
            for (MonsterDef.Spawn s : m.spawns) {
                spawns.add(obj("region", s.region().id(), "weight", s.weight(), "min", s.min(), "max", s.max()));
            }
            monsters.add(profile(m, "spawns", "[" + String.join(", ", spawns) + "]", "underground", m.underground));
        }
        List<String> bosses = new ArrayList<>();
        for (BossDef b : ContentRegistry.bosses()) {
            bosses.add(profile(b, "place", b.place, "arena_style", b.arenaStyle, "raid_only", b.raidOnly,
                    "summon_block", b.summonBlockId()));
        }
        List<String> npcs = new ArrayList<>();
        for (NpcDef n : ContentRegistry.npcs()) {
            npcs.add(obj("id", n.id(), "name", n.name(), "colors", n.colors(),
                    "race", NpcRace.of(n.id(), n.name(), n.place()).name().toLowerCase(Locale.ROOT)));
        }
        List<String> blocks = new ArrayList<>();
        for (BlockDef b : ContentRegistry.blocks()) {
            blocks.add(obj("id", b.id(), "name", b.name(), "kind", b.kind().name(), "color", b.color(), "look", b.look(),
                    "breakable", b.breakable()));
        }
        List<String> models = new ArrayList<>();
        for (Map.Entry<String, ModelSpec> e : ContentRegistry.models().entrySet()) {
            ModelSpec m = e.getValue();
            List<String> parts = new ArrayList<>();
            for (ModelSpec.Part p : m.parts()) {
                List<String> boxes = new ArrayList<>();
                for (int i = 0; i < p.boxes.size(); i++) {
                    ModelSpec.Box b = p.boxes.get(i);
                    int[] uv = p.uvs.get(i);
                    boxes.add(obj("w", b.w(), "h", b.h(), "d", b.d(), "u", uv[0], "v", uv[1],
                            "x", b.x(), "y", b.y(), "z", b.z(), "dil", b.dilation()));
                }
                parts.add(obj("name", p.name, "role", p.role.name(), "color", p.color, "accent", p.accent,
                        "pattern", p.pattern, "glow", p.glow, "material", p.material, "parent", p.parent, "px", p.px, "py", p.py, "pz", p.pz,
                        "pitch", p.pitch, "yaw", p.yaw, "roll", p.roll, "boxes", "[" + String.join(", ", boxes) + "]"));
            }
            models.add(obj("id", m.id(), "tex_w", m.texWidth(), "tex_h", m.texHeight(), "scale", m.scale(), "has_glow", m.hasGlow(),
                    "parts", "[" + String.join(", ", parts) + "]"));
        }
        List<String> questNpcs = new ArrayList<>();
        for (QuestNpc q : QuestNpc.values()) {
            questNpcs.add(obj("id", q.name().toLowerCase(Locale.ROOT), "name", q.displayName(),
                    "look", q.look().name().toLowerCase(Locale.ROOT),
                    "race", NpcRace.of(q.name(), q.displayName(), q.landmark().name()).name().toLowerCase(Locale.ROOT)));
        }
        List<String> quests = new ArrayList<>();
        for (QuestInfo qi : ContentRegistry.quests()) {
            quests.add(obj("id", qi.questId(), "name", qi.displayName(), "points", qi.questPoints()));
        }
        String json = "{\n  \"items\": " + arr(items)
                + ",\n  \"trees\": " + arr(trees)
                + ",\n  \"rocks\": " + arr(rocks)
                + ",\n  \"fishing_spots\": " + arr(spots)
                + ",\n  \"altars\": " + arr(altars)
                + ",\n  \"stalls\": " + arr(stalls)
                + ",\n  \"monsters\": " + (monsters.isEmpty() ? "[]" : arr(monsters))
                + ",\n  \"bosses\": " + (bosses.isEmpty() ? "[]" : arr(bosses))
                + ",\n  \"npcs\": " + (npcs.isEmpty() ? "[]" : arr(npcs))
                + ",\n  \"blocks\": " + (blocks.isEmpty() ? "[]" : arr(blocks))
                + ",\n  \"models\": " + (models.isEmpty() ? "[]" : arr(models))
                + ",\n  \"quest_npcs\": " + arr(questNpcs)
                + ",\n  \"quests\": " + arr(quests)
                + ",\n  \"herbs\": " + Herb.values().length + "\n}\n";
        Path out = Path.of(args.length > 0 ? args[0] : "build/catalog.json");
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        Files.writeString(out, json);
        System.out.println("Wrote " + ItemCatalog.all().size() + " items, " + monsters.size() + " monsters, "
                + bosses.size() + " bosses, " + models.size() + " models to " + out);
    }

    private static String profile(CombatProfile<?> p, Object... extra) {
        Object[] base = {"id", p.id, "name", p.name, "level", p.combatLevel, "model", p.model, "skin", p.skin,
                "scale", p.scale};
        Object[] all = new Object[base.length + extra.length];
        System.arraycopy(base, 0, all, 0, base.length);
        System.arraycopy(extra, 0, all, base.length, extra.length);
        return obj(all);
    }
}
