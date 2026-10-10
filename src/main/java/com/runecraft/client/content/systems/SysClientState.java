package com.runecraft.client.content.systems;

/** Client mirror of the systems module's HUD state. */
public final class SysClientState {
    public static boolean received;
    public static int spec = 100;
    public static boolean specArmed;
    /** Special attack cost of the wielded weapon, or -1. */
    public static int specCost = -1;
    /** Run energy in hundredths of a percent. */
    public static int run = 10000;
    public static boolean stamina;
    public static String book = "Standard";
    public static String autocast = "";
    public static boolean vengeance;
    public static int thrall;

    private SysClientState() {
    }
}
