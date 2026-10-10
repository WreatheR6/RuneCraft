package com.runecraft.osrs.content.islands;

import com.runecraft.osrs.content.BlockDef;

import java.util.List;

/**
 * Scripted blocks: fairy rings (the network through Zanaris), Fossil Island's Magic Mushtrees (the
 * Mycelium transportation system), the Miscellania kingdom's royal ledger and workforce board, and
 * the obstacles of the Ape Atoll Agility Course.
 */
public final class IslandsBlocks {
    private IslandsBlocks() {
    }

    public static final String FAIRY_RING = "isl_fairy_ring";
    public static final String MUSHTREE = "isl_magic_mushtree";
    public static final String KINGDOM_LEDGER = "isl_kingdom_ledger";
    public static final String KINGDOM_WORKFORCE = "isl_kingdom_workforce";
    public static final String APE_OBSTACLE = "isl_ape_atoll_obstacle";

    public static List<BlockDef> all() {
        return List.of(
                BlockDef.scripted(FAIRY_RING, "Fairy ring", 0x9AD08A, "portal_glow"),
                BlockDef.scripted(MUSHTREE, "Magic Mushtree", 0x8A5AB0, "mushroom"),
                BlockDef.scripted(KINGDOM_LEDGER, "Royal ledger", 0xC8A050, "book"),
                BlockDef.scripted(KINGDOM_WORKFORCE, "Kingdom workforce board", 0x8A6A3A, "notice_board"),
                BlockDef.scripted(APE_OBSTACLE, "Ape Atoll agility obstacle", 0x7A5A2A, "rope"));
    }
}
