package com.runecraft.osrs.content;

/**
 * A non-combat character (quest giver, shopkeeper, master...). Rendered as a biped with a skin
 * generated from {@link #colors} (skin, top, bottom, hat/hair). Spawned at {@link #place} (a
 * Landmark enum name or a PlaceDef id) with the given block offset.
 */
public record NpcDef(String id, String name, String place, int dx, int dz, int[] colors) {
    public static NpcDef of(String id, String name, String place, int dx, int dz, int skin, int top, int bottom, int hat) {
        return new NpcDef(id, name, place, dx, dz, new int[]{skin, top, bottom, hat});
    }
}
