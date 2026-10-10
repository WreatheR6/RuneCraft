package com.runecraft.osrs.content.npcs;

import java.util.List;

/**
 * An ordinary character standing at its OSRS tile ({@code x}, {@code y}). {@code role} decides what
 * right-clicking does: open {@code shop}, open the bank, offer the {@code travel} destinations, open
 * a production menu, or just say one of {@code lines}.
 */
public record TownNpc(String id, String name, String area, int x, int y, Role role, String shop, String travel,
                      List<String> lines, int[] colors) {

    public enum Role { SHOP, BANKER, AMBIENT, TRAVEL, TANNER }
}
