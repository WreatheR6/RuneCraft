package com.runecraft.osrs.content.zeah;

import com.runecraft.osrs.content.BlockDef;

import java.util.List;

/**
 * Scripted blocks: the Lovakengj minecart network, lovakite rocks, the Blast Mine's hard rock and
 * the Varlamore Hunter spots (antelope pitfalls, moth bushes and embertailed jerboa burrows).
 */
public final class ZeahBlocks {
    private ZeahBlocks() {
    }

    public static final String MINECART = "zeah_minecart";
    public static final String LOVAKITE_ROCK = "zeah_lovakite_rock";
    public static final String HARD_ROCK = "zeah_hard_rock";
    public static final String SUNLIGHT_ANTELOPE_PIT = "zeah_sunlight_antelope_pit";
    public static final String MOONLIGHT_ANTELOPE_PIT = "zeah_moonlight_antelope_pit";
    public static final String SUNLIGHT_MOTH_BUSH = "zeah_sunlight_moth_bush";
    public static final String MOONLIGHT_MOTH_BUSH = "zeah_moonlight_moth_bush";
    public static final String JERBOA_BURROW = "zeah_jerboa_burrow";
    /** The altar at the centre of the Catacombs of Kourend: Darklight + 3 ancient shards make Arclight. */
    public static final String CATACOMBS_ALTAR = "zeah_catacombs_altar";
    /** Ancient shards needed to forge Arclight (wiki). */
    public static final int ARCLIGHT_SHARDS = 3;

    public static List<BlockDef> all() {
        return List.of(
                BlockDef.scripted(MINECART, "Lovakengj minecart", 0x6A5A4A, "machine"),
                BlockDef.scripted(LOVAKITE_ROCK, "Lovakite rocks", 0x4A6A7A, "rock"),
                BlockDef.scripted(HARD_ROCK, "Hard rock", 0x5A4A3A, "rock"),
                BlockDef.scripted(SUNLIGHT_ANTELOPE_PIT, "Sunlight antelope pit", 0xC0A050, "pit"),
                BlockDef.scripted(MOONLIGHT_ANTELOPE_PIT, "Moonlight antelope pit", 0x5A6AA0, "pit"),
                BlockDef.scripted(SUNLIGHT_MOTH_BUSH, "Sunlight moth bush", 0xE0C040, "young_tree"),
                BlockDef.scripted(MOONLIGHT_MOTH_BUSH, "Moonlight moth bush", 0x8090D0, "young_tree"),
                BlockDef.scripted(JERBOA_BURROW, "Embertailed jerboa burrow", 0xC06A3A, "boulder"),
                BlockDef.scripted(CATACOMBS_ALTAR, "Altar", 0x5A5A6A, "altar"));
    }
}
