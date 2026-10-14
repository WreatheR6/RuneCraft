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

    /** Waterbirth sublevel 1's circular doors (two players, or one with a pet rock on the pressure pad). */
    public static final String CIRCULAR_DOOR = "dng_circular_door";
    /** Waterbirth sublevel 1's door-supports around the central room (broken by a ranged or magic attack). */
    public static final String DOOR_SUPPORT = "dng_door_support";
    /** The iron ladder from Waterbirth sublevel 2's rock lobster room up to the Lighthouse. */
    public static final String LIGHTHOUSE_LADDER = "dng_lighthouse_ladder";
    /** A pile of rocks by the Waterbirth arrival ladder: a pet rock for the circular doors. */
    public static final String ROCK_PILE = "dng_rock_pile";

    /**
     * Where the Kings' ladder at block (x, y, z) takes a player: from the great hall down into the Kings' lair, from
     * the lair back up to the great hall. Returns the block {x, y, z} to stand on.
     */
    public static int[] kingsLadderTarget(int x, int y, int z) {
        return WaterbirthDungeon.inLair(x, y, z) ? WaterbirthDungeon.hallArrival() : WaterbirthDungeon.lairArrival();
    }

    /** True if a player with feet at (x, y, z) stands on a floor trap of the Temple of Marimbo Dungeon's tunnel. */
    public static boolean apeAtollTrap(int x, int y, int z) {
        return ApeAtollDungeons.trapAt(x, y, z);
    }

    /** True if rocks can fall on a player with feet at (x, y, z) in the Ape Atoll Dungeon (away from the rock ledges). */
    public static boolean apeAtollFallingRocks(int x, int y, int z) {
        return ApeAtollDungeons.fallingRocks(x, y, z);
    }

    /** True if the block is in the Dagannoth Kings' lair (Waterbirth sublevel 6). */
    public static boolean inKingsLair(int x, int y, int z) {
        return WaterbirthDungeon.inLair(x, y, z);
    }

    /**
     * True if the held weapon can knock down a door-support from a distance: a ranged weapon (bows, crossbows,
     * thrown weapons, blowpipes, chinchompas) or a magic one (staves, wands, sceptres, tridents).
     */
    public static boolean breaksDoorSupport(String heldId) {
        if (heldId == null || heldId.isEmpty()) {
            return false;
        }
        for (String kind : new String[]{"bow", "thrownaxe", "dart", "knife", "javelin", "chinchompa", "blowpipe", "ballista",
                "staff", "wand", "sceptre", "trident", "crozier"}) {
            if (heldId.contains(kind)) {
                return true;
            }
        }
        return false;
    }

    /** Block {x, z} of the Lighthouse, where its iron ladder from the Waterbirth Island Dungeon comes up. */
    public static int[] lighthouseExit() {
        return WaterbirthDungeon.lighthouse();
    }

    /** Block {x, y, z} beside the Waterbirth sublevel's iron ladder, where climbing down from the Lighthouse arrives. */
    public static int[] lighthouseLadderBelow() {
        return WaterbirthDungeon.lighthouseLadderArrival();
    }

    /** Block {x, z} of the centre of the Lighthouse tower's ground floor (KandarinNorth's tower at tile 2503, 3633). */
    public static int[] lighthouseTowerCentre() {
        return new int[]{com.runecraft.osrs.world.StructureDef.bx(2503) + 1, com.runecraft.osrs.world.StructureDef.bz(3633) + 1};
    }

    /** The Dagannoth Kings, always present in their lair (OSRS), and their stations there: {x, y, z} feet blocks. */
    public static final List<String> KINGS = List.of("dagannoth_rex", "dagannoth_prime", "dagannoth_supreme");

    public static int[] kingSpot(int king) {
        return WaterbirthDungeon.kingSpot(king);
    }

    /** Ticks (game) before a slain King returns: 90 seconds (OSRS). */
    public static final int KING_RESPAWN_TICKS = 150;

    /** True if a player at block x is on the entrance hall's side of the circular doors (west of them). */
    public static boolean outsideCircularDoor(double playerX) {
        return playerX < WaterbirthDungeon.doorX() + 0.5;
    }

    /** True if a player at (px, pz) is inside the central room, i.e. nearer its middle than the door-support is. */
    public static boolean insideCentralRoom(double px, double pz, int supportX, int supportZ) {
        int[] c = WaterbirthDungeon.centralRoom();
        double dp = Math.hypot(px - c[0] - 0.5, pz - c[1] - 0.5);
        double ds = Math.hypot(supportX - c[0], supportZ - c[1]);
        return dp < ds;
    }

    /** A Mort'ton catacomb door that opens for any shade key of its metal ({@link ShadeCatacombs}). */
    public static String shadeDoor(String metal) {
        return "dng_shade_door_" + metal;
    }

    /** Which reward chest each Stronghold of Security floor ends at, and the floor's door. */
    public static final List<String> SOS_DOORS = List.of(GATE_OF_WAR, RICKETY_DOOR, OOZING_BARRIER, PORTAL_OF_DEATH);
    public static final List<String> SOS_CHESTS = List.of(GIFT_OF_PEACE, GRAIN_OF_PLENTY, BOX_OF_HEALTH, CRADLE_OF_LIFE);
    /** Coins from each chest (the Cradle of Life gives boots instead). */
    public static final List<Integer> SOS_COINS = List.of(2000, 3000, 5000, 0);

    /** A portal out of the Rune essence mine: returns the player to the NPC who teleported them ({@link EssenceMine}). */
    public static final String ESSENCE_PORTAL = "dng_essence_mine_portal";

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
                BlockDef.scripted(KINGS_LADDER, "Ladder (Dagannoth Kings)", 0x5A4A3A, "pit"),
                BlockDef.scripted(CIRCULAR_DOOR, "Door", 0x4A5A5A, "portal_door"),
                BlockDef.scripted(DOOR_SUPPORT, "Door-support", 0x6A5A3A, "statue"),
                BlockDef.scripted(LIGHTHOUSE_LADDER, "Iron ladder (Lighthouse)", 0x5A5A5A, "pit"),
                BlockDef.scripted(ROCK_PILE, "Pile of rocks", 0x7A7A72, "boulder"),
                BlockDef.scripted(ESSENCE_PORTAL, "Portal", 0xC8D8F0, "portal_glow")));
        for (String metal : com.runecraft.osrs.content.CommonItems.SHADE_KEY_METALS) {
            list.add(BlockDef.scripted(shadeDoor(metal), "Wooden doors (" + metal + " lock)", 0x6A4A2A, "portal_door"));
        }
        return list;
    }
}
