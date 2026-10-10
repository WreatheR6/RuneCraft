package com.runecraft.osrs.content;

import com.runecraft.osrs.Region;

import java.util.ArrayList;
import java.util.List;

/**
 * A regular monster added by a content module. Gielinor has no natural (biome) spawning: a monster
 * only appears at its OSRS spawn locations, from a module camp or a point in
 * {@link com.runecraft.osrs.world.SpawnTable}. {@link #spawns} only records the kingdoms it lives in.
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
