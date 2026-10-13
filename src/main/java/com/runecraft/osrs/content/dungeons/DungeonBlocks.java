package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.content.BlockDef;

import java.util.ArrayList;
import java.util.List;

/**
 * Scripted dungeon blocks: the Stronghold of Security's doors and reward chests, Brimhaven's
 * vines, the Mining Guild door, the Abyss rifts and the Barrows' sarcophagi, crypt stairs, tunnel
 * doors and chest (see {@link Barrows}).
 */
public final class DungeonBlocks {
    private DungeonBlocks() {
    }

    // Stronghold of Security doors (one per floor) and the four reward chests.
    public static final String GATE_OF_WAR = "dng_gate_of_war";
    public static final String RICKETY_DOOR = "dng_rickety_door";
    public static final String OOZING_BARRIER = "dng_oozing_barrier";
    public static final String PORTAL_OF_DEATH = "dng_portal_of_death";
    public static final String GIFT_OF_PEACE = "dng_gift_of_peace";
    public static final String GRAIN_OF_PLENTY = "dng_grain_of_plenty";
    public static final String BOX_OF_HEALTH = "dng_box_of_health";
    public static final String CRADLE_OF_LIFE = "dng_cradle_of_life";
    /** Brimhaven Dungeon vines (need an axe to hack through). */
    public static final String VINES = "dng_jungle_vines";
    /** The Mining Guild door in the Dwarven Mine (60 Mining). */
    public static final String MINING_GUILD_DOOR = "dng_mining_guild_door";
    /** An Abyss rift: teleports to the rune altar it leads to. */
    public static final String RIFT = "dng_abyssal_rift";
    /** The passage blockers of the Abyss's inner ring (passable through the gaps only). */
    public static final String ABYSS_PASSAGE = "dng_abyss_passage";

    /** The Waterbirth Island Dungeon's ladder down to the Dagannoth Kings (to the bosses module's arena). */
    public static final String KINGS_LADDER = "dng_kings_ladder";

    /** A Mort'ton catacomb door that opens for any shade key of its metal ({@link ShadeCatacombs}). */
    public static String shadeDoor(String metal) {
        return "dng_shade_door_" + metal;
    }

    /** Which reward chest each Stronghold of Security floor ends at, and the floor's door. */
    public static final List<String> SOS_DOORS = List.of(GATE_OF_WAR, RICKETY_DOOR, OOZING_BARRIER, PORTAL_OF_DEATH);
    public static final List<String> SOS_CHESTS = List.of(GIFT_OF_PEACE, GRAIN_OF_PLENTY, BOX_OF_HEALTH, CRADLE_OF_LIFE);
    /** Coins from each chest (the Cradle of Life gives boots instead). */
    public static final List<Integer> SOS_COINS = List.of(2000, 3000, 5000, 0);

    /** Blocks that are passed through (the player is moved to the far side) when used. */
    public static final List<String> PASSABLE = List.of(GATE_OF_WAR, RICKETY_DOOR, OOZING_BARRIER, PORTAL_OF_DEATH, VINES,
            MINING_GUILD_DOOR, ABYSS_PASSAGE);

    public static List<BlockDef> all() {
        List<BlockDef> list = new ArrayList<>(List.of(
                BlockDef.scripted(GATE_OF_WAR, "Gate of War", 0x6A6A6A, "door"),
                BlockDef.scripted(RICKETY_DOOR, "Rickety door", 0x7A5A3A, "door"),
                BlockDef.scripted(OOZING_BARRIER, "Oozing barrier", 0x6A8A3A, "door"),
                BlockDef.scripted(PORTAL_OF_DEATH, "Portal of Death", 0x4A2A5A, "portal_glow"),
                BlockDef.scripted(GIFT_OF_PEACE, "Gift of Peace", 0xC0A050, "chest"),
                BlockDef.scripted(GRAIN_OF_PLENTY, "Grain of Plenty", 0xD0B060, "crate"),
                BlockDef.scripted(BOX_OF_HEALTH, "Box of Health", 0xC04040, "chest"),
                BlockDef.scripted(CRADLE_OF_LIFE, "Cradle of Life", 0xE0D0B0, "statue"),
                BlockDef.scripted(VINES, "Vines", 0x3A7A2A, "young_tree"),
                BlockDef.scripted(MINING_GUILD_DOOR, "Mining Guild door", 0x5A4A3A, "door"),
                BlockDef.scripted(RIFT, "Abyssal rift", 0x8A2AA0, "portal_glow"),
                BlockDef.scripted(ABYSS_PASSAGE, "Passage", 0x3A1A3A, "boulder"),
                BlockDef.scripted(Barrows.SARCOPHAGUS, "Sarcophagus", 0x5A5856, "coffin"),
                BlockDef.scripted(Barrows.CRYPT_STAIRS, "Crypt stairs", 0x3A3A3A, "statue"),
                BlockDef.scripted(Barrows.TUNNEL_DOOR, "Crypt door", 0x4A3A2A, "door"),
                BlockDef.scripted(Barrows.CHEST, "Barrows chest", 0x6A5A3A, "chest"),
                BlockDef.scripted(KINGS_LADDER, "Ladder (Dagannoth Kings)", 0x5A4A3A, "pit")));
        for (String metal : com.runecraft.osrs.content.CommonItems.SHADE_KEY_METALS) {
            list.add(BlockDef.scripted(shadeDoor(metal), "Wooden doors (" + metal + " lock)", 0x6A4A2A, "portal_door"));
        }
        return list;
    }
}
