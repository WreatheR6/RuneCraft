package com.runecraft.network;

import com.runecraft.RuneCraft;
import net.minecraft.util.Identifier;

/** Packet channel ids. */
public final class Net {
    // Server -> client
    public static final Identifier SYNC = RuneCraft.id("sync");
    public static final Identifier XP_DROP = RuneCraft.id("xp_drop");
    public static final Identifier HITSPLAT = RuneCraft.id("hitsplat");
    public static final Identifier OPEN_MENU = RuneCraft.id("open_menu");
    // Client -> server
    public static final Identifier MENU_CHOICE = RuneCraft.id("menu_choice");
    public static final Identifier SET_STYLE = RuneCraft.id("set_style");
    public static final Identifier TOGGLE_PRAYER = RuneCraft.id("toggle_prayer");
    public static final Identifier CAST_SPELL = RuneCraft.id("cast_spell");
    public static final Identifier SET_AUTOCAST = RuneCraft.id("set_autocast");

    public static final byte SPLAT_MISS = 0;
    public static final byte SPLAT_DAMAGE = 1;
    public static final byte SPLAT_HEAL = 2;

    private Net() {
    }
}
