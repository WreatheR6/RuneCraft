package com.runecraft.osrs.content;

import com.runecraft.osrs.ItemDef;

/** Lets content modules add items to the {@link com.runecraft.osrs.ItemCatalog}. */
@FunctionalInterface
public interface ItemSink {
    /**
     * Adds an item; configure it with the returned ItemDef's fluent setters (value, stack, heal,
     * equipment, tier, rangedStrength, examine). {@code shape} picks the texture template.
     */
    ItemDef add(String id, String name, ItemDef.Kind kind, String shape, int color);
}
