package com.runecraft.osrs.content;

import com.runecraft.osrs.Region;

import java.util.ArrayList;
import java.util.List;

/**
 * A regular monster added by a content module. Spawns naturally in the listed kingdoms
 * (tools/gen_worldgen.py reads this from build/catalog.json).
 */
public final class MonsterDef extends CombatProfile<MonsterDef> {
    public record Spawn(Region region, int weight, int min, int max) {
    }

    public final List<Spawn> spawns = new ArrayList<>();
    /** Spawns only underground (below y=40) / in dungeons rather than the surface. */
    public boolean underground;

    public MonsterDef(String id, String name) {
        super(id, name);
    }

    public static MonsterDef of(String id, String name) {
        return new MonsterDef(id, name);
    }

    public MonsterDef spawn(Region region, int weight, int min, int max) {
        spawns.add(new Spawn(region, weight, min, max));
        return this;
    }

    public MonsterDef underground() {
        this.underground = true;
        return this;
    }
}
