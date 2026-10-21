package com.runecraft.network;

import com.runecraft.RuneCraft;
import net.minecraft.resources.ResourceLocation;

/** Packet channel ids. */
public final class Net {
    // Server -> client
    public static final ResourceLocation SYNC = RuneCraft.id("sync");
    public static final ResourceLocation XP_DROP = RuneCraft.id("xp_drop");
    public static final ResourceLocation HITSPLAT = RuneCraft.id("hitsplat");
    public static final ResourceLocation OPEN_MENU = RuneCraft.id("open_menu");
    /** Combat Options state: auto-retaliate on (boolean), standing in a multicombat area (boolean). */
    public static final ResourceLocation COMBAT_STATE = RuneCraft.id("combat_state");
    // Client -> server
    public static final ResourceLocation MENU_CHOICE = RuneCraft.id("menu_choice");
    public static final ResourceLocation SET_STYLE = RuneCraft.id("set_style");
    public static final ResourceLocation TOGGLE_PRAYER = RuneCraft.id("toggle_prayer");
    public static final ResourceLocation CAST_SPELL = RuneCraft.id("cast_spell");
    public static final ResourceLocation SET_AUTOCAST = RuneCraft.id("set_autocast");
    /** Toggles auto-retaliate (no payload). */
    public static final ResourceLocation TOGGLE_RETALIATE = RuneCraft.id("toggle_retaliate");
    /** Sets a prayer on or off (varint ordinal, boolean on): idempotent, sent by the prayer hotkeys and the side panel. */
    public static final ResourceLocation PRAYER_SET = RuneCraft.id("prayer/set");
    /** Switches the quick-prayers (no payload), OSRS's quick-prayer orb. */
    public static final ResourceLocation PRAYER_QUICK = RuneCraft.id("prayer/quick");
    /** Sets the quick-prayer selection (varlong mask of prayer ordinals). */
    public static final ResourceLocation PRAYER_QUICK_SET = RuneCraft.id("prayer/quick_set");

    public static final byte SPLAT_MISS = 0;
    public static final byte SPLAT_DAMAGE = 1;
    public static final byte SPLAT_HEAL = 2;
    public static final byte SPLAT_POISON = 3;
    public static final byte SPLAT_VENOM = 4;
    /** A monster's hit stopped by the matching protection prayer: a blue 0 with the overhead's tag and a shield sound. */
    public static final byte SPLAT_BLOCKED = 5;

    private Net() {
    }
}
