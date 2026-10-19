package com.runecraft.content.skilling;

import com.runecraft.RuneCraft;
import net.minecraft.resources.ResourceLocation;

/** Networking ids of the Sailing boats (shared by the server and the client). */
public final class SeaNet {
    private SeaNet() {
    }

    /** Server -> client: the per-tick velocity multiplier of the boat you ride (hull speed); 1 for none. */
    public static final ResourceLocation BOAT_SPEED = RuneCraft.id("skilling/boat_speed");
}
