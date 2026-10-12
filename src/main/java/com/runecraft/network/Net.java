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
    /** Combat Options state: auto-retaliate on (boolean), standing in a multicombat area (boolean). */
    public static final Identifier COMBAT_STATE = RuneCraft.id("combat_state");
    // Client -> server
    public static final Identifier MENU_CHOICE = RuneCraft.id("menu_choice");
    public static final Identifier SET_STYLE = RuneCraft.id("set_style");
    public static final Identifier TOGGLE_PRAYER = RuneCraft.id("toggle_prayer");
    public static final Identifier CAST_SPELL = RuneCraft.id("cast_spell");
    public static final Identifier SET_AUTOCAST = RuneCraft.id("set_autocast");
    /** Toggles auto-retaliate (no payload). */
    public static final Identifier TOGGLE_RETALIATE = RuneCraft.id("toggle_retaliate");

    public static final byte SPLAT_MISS = 0;
    public static final byte SPLAT_DAMAGE = 1;
    public static final byte SPLAT_HEAL = 2;
    public static final byte SPLAT_POISON = 3;
    public static final byte SPLAT_VENOM = 4;

    private Net() {
    }
}
