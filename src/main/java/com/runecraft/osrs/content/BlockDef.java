package com.runecraft.osrs.content;

import com.runecraft.osrs.MenuType;

/**
 * A simple module block. {@link Kind#MENU} blocks open a Make-X menu when clicked; {@link Kind#SCRIPTED}
 * blocks call the handler a module registers with {@code BlockHooks.register(id, handler)};
 * {@link Kind#DECOR} blocks do nothing. Textures are generated from {@link #color} and {@link #look}.
 */
public record BlockDef(String id, String name, Kind kind, MenuType menu, int color, String look, boolean breakable) {
    public enum Kind { MENU, SCRIPTED, DECOR }

    public static BlockDef menu(String id, String name, MenuType menu, int color, String look) {
        return new BlockDef(id, name, Kind.MENU, menu, color, look, true);
    }

    public static BlockDef scripted(String id, String name, int color, String look) {
        return new BlockDef(id, name, Kind.SCRIPTED, null, color, look, false);
    }

    public static BlockDef decor(String id, String name, int color, String look) {
        return new BlockDef(id, name, Kind.DECOR, null, color, look, true);
    }
}
