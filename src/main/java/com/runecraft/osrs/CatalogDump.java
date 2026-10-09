package com.runecraft.osrs;

import com.runecraft.osrs.data.FishingSpot;
import com.runecraft.osrs.data.Herb;
import com.runecraft.osrs.data.Rock;
import com.runecraft.osrs.data.RuneAltar;
import com.runecraft.osrs.data.Thieving;
import com.runecraft.osrs.data.Tree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes the item/block catalog as JSON for tools/gen_assets.py. */
public final class CatalogDump {
    private CatalogDump() {
    }

    public static void main(String[] args) throws IOException {
        StringBuilder sb = new StringBuilder("{\n  \"items\": [\n");
        boolean first = true;
        for (ItemDef d : ItemCatalog.all()) {
            if (!first) {
                sb.append(",\n");
            }
            first = false;
            sb.append(String.format(java.util.Locale.ROOT,
                    "    {\"id\": \"%s\", \"name\": \"%s\", \"kind\": \"%s\", \"shape\": \"%s\", \"color\": %d, \"tier\": %s}",
                    d.id(), d.name().replace("\"", "\\\""), d.kind(), d.shape(), d.color(),
                    d.tier() == null ? "null" : "\"" + d.tier() + "\""));
        }
        sb.append("\n  ],\n  \"trees\": [");
        first = true;
        for (Tree t : Tree.values()) {
            sb.append(first ? "" : ", ").append(String.format(java.util.Locale.ROOT,
                    "{\"id\": \"%s\", \"name\": \"%s\", \"color\": %d}", t.id(), t.displayName(), t.color()));
            first = false;
        }
        sb.append("],\n  \"rocks\": [");
        first = true;
        for (Rock r : Rock.values()) {
            sb.append(first ? "" : ", ").append(String.format(java.util.Locale.ROOT,
                    "{\"id\": \"%s\", \"name\": \"%s\", \"color\": %d}", r.id(), r.displayName(), r.color()));
            first = false;
        }
        sb.append("],\n  \"fishing_spots\": [");
        first = true;
        for (FishingSpot s : FishingSpot.values()) {
            sb.append(first ? "" : ", ").append(String.format(java.util.Locale.ROOT,
                    "{\"id\": \"%s\", \"name\": \"%s\"}", s.blockId(), s.displayName()));
            first = false;
        }
        sb.append("],\n  \"altars\": [");
        first = true;
        for (RuneAltar a : RuneAltar.values()) {
            sb.append(first ? "" : ", ").append(String.format(java.util.Locale.ROOT,
                    "{\"id\": \"%s\", \"name\": \"%s\"}", a.blockId(), a.rune().displayName().replace(" rune", "") + " Altar"));
            first = false;
        }
        sb.append("],\n  \"stalls\": [");
        first = true;
        for (Thieving.Stall s : Thieving.Stall.values()) {
            sb.append(first ? "" : ", ").append(String.format(java.util.Locale.ROOT,
                    "{\"id\": \"%s\", \"name\": \"%s\"}", s.blockId(), s.displayName()));
            first = false;
        }
        sb.append("],\n  \"herbs\": ").append(Herb.values().length).append("\n}\n");
        Path out = Path.of(args.length > 0 ? args[0] : "build/catalog.json");
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        Files.writeString(out, sb.toString());
        System.out.println("Wrote " + ItemCatalog.all().size() + " items to " + out);
    }
}
