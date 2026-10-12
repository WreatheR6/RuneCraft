package com.runecraft.osrs.bank;

import java.util.List;

/**
 * Bank deposit boxes: deposit-only banks (deposit inventory, worn items, or single items; never withdraw) on the
 * docks and in the fishing/skilling spots of OSRS. Each spot gets one box on the ground when its chunk is first
 * built (systems module places, style NONE).
 */
public final class DepositBoxes {
    /** The deposit box block id. */
    public static final String BLOCK = "bank_deposit_box";

    /** A deposit box at an OSRS tile. */
    public record Spot(String id, String name, int osrsX, int osrsY) {
    }

    /** Real deposit box spots (tiles approximate to within a few squares). */
    public static final List<Spot> SPOTS = List.of(
            new Spot("deposit_box_port_sarim", "Port Sarim deposit box", 3045, 3235), // approx: on the docks
            new Spot("deposit_box_catherby", "Catherby deposit box", 2837, 3436), // approx: beach by the fishing spots
            new Spot("deposit_box_port_khazard", "Port Khazard deposit box", 2663, 3160), // approx: docks
            new Spot("deposit_box_musa_point", "Musa Point deposit box", 2954, 3150)); // approx: Karamja docks

    private DepositBoxes() {
    }
}
