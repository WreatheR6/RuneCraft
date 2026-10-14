package com.runecraft.osrs.content.raids;

import com.runecraft.osrs.content.raids.RoomDef.Kind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Every raid room, and the room sequences each raid is built from. */
public final class RaidLayouts {
    private static final Map<String, RoomDef> ROOMS = new LinkedHashMap<>();

    /**
     * The two OSRS combat-room rotations of the Chambers of Xeric. A raid takes a consecutive
     * window of four rooms from one rotation (either direction, wrapping around), exactly how
     * scouted raids in OSRS always follow these cycles.
     */
    public static final List<List<String>> COX_ROTATIONS = List.of(
            List.of("tekton", "vasa", "guardians", "mystics", "shamans", "muttadiles", "vanguards", "vespula"),
            List.of("tekton", "muttadiles", "guardians", "vespula", "shamans", "vasa", "vanguards", "mystics"));
    /** CoX puzzle rooms. Their points are awarded per action (see the room logic), not on completion. */
    public static final List<String> COX_PUZZLES = List.of("ice_demon", "thieving", "tightrope", "crystal_crabs");
    /** ToA paths: puzzle room followed by the path's boss. */
    public static final List<List<String>> TOA_PATHS = List.of(
            List.of("scabaras_puzzle", "kephri"),
            List.of("het_puzzle", "akkha"),
            List.of("apmeken_puzzle", "baba"),
            List.of("crondis_puzzle", "zebak"));

    static {
        // --- Chambers of Xeric ---
        room("cox_lobby", "Chambers of Xeric", RaidType.COX, Kind.LOBBY, "crypt", List.of(), 0,
                "Take supplies from the storage chest, then pass the barrier to descend into the Chambers.");
        room("tekton", "Tekton", RaidType.COX, Kind.COMBAT, "lava", List.of("tekton"), 0,
                "Tekton returns to his anvil to sharpen his hammer. Dodge the sparks and hit him with crush.");
        room("vasa", "Vasa Nistirio", RaidType.COX, Kind.COMBAT, "void", List.of("vasa_nistirio"), 0,
                "Destroy the glowing crystal Vasa draws power from, and run when he teleports you to him.");
        room("guardians", "Guardians", RaidType.COX, Kind.COMBAT, "stone", List.of("cox_guardian", "cox_guardian"), 0,
                "The Guardians are made of stone. Only a pickaxe will chip them apart; Mining level and a better pickaxe hit harder.");
        room("mystics", "Skeletal Mystics", RaidType.COX, Kind.COMBAT, "crypt",
                List.of("skeletal_mystic", "skeletal_mystic", "skeletal_mystic"), 0,
                "Skeletal Mystics guard this cavern; more of them rise for larger parties.");
        room("shamans", "Lizardman Shamans", RaidType.COX, Kind.COMBAT, "jungle",
                List.of("cox_lizardman_shaman", "cox_lizardman_shaman", "cox_lizardman_shaman"), 0,
                "Move when a shaman leaps, and run from the purple spawn before they explode.");
        room("muttadiles", "Muttadiles", RaidType.COX, Kind.COMBAT, "water", List.of("small_muttadile"), 0,
                "Chop down the meat tree so the Muttadiles cannot feed. Its mother waits beneath the water.");
        room("vanguards", "Vanguards", RaidType.COX, Kind.COMBAT, "stone",
                List.of("vanguard_melee", "vanguard_ranged", "vanguard_magic"), 0,
                "Keep the three Vanguards' health even: if one falls too far behind, they all reset.");
        room("vespula", "Vespula", RaidType.COX, Kind.COMBAT, "jungle", List.of("vespula", "vespula_portal"), 0,
                "Bring Vespula down to 20% so she lands, then destroy the abyssal portal. Kill any soldiers her lux grubs hatch into.");
        room("ice_demon", "Ice demon", RaidType.COX, Kind.PUZZLE, "ice", List.of(), 0,
                "Chop the saplings for kindling and light the braziers to thaw the Ice demon, then slay it.");
        room("thieving", "Thieving", RaidType.COX, Kind.PUZZLE, "crypt", List.of(), 0,
                "Search the grubby chests for cavern grubs and feed them to the scavenger's trough.");
        room("tightrope", "Tightrope", RaidType.COX, Kind.PUZZLE, "void", List.of(), 0,
                "Kill the deathly mages and rangers, then cross the tightrope.");
        room("crystal_crabs", "Crystal crabs", RaidType.COX, Kind.PUZZLE, "ice", List.of(), 0,
                "Strike the crystal crabs to make them shimmer, and lead a shimmering crab to each dark crystal to light it.");
        room("olm", "The Great Olm", RaidType.COX, Kind.FINAL, "lava", List.of("great_olm"), 0,
                "Kill both claws to expose the head. Watch for falling crystals, acid, lightning and portals.");
        room("cox_reward", "Ancient chest", RaidType.COX, Kind.REWARD, "crypt", List.of(), 0,
                "Open the ancient chest to claim your reward.");

        // --- Theatre of Blood ---
        room("tob_lobby", "Theatre of Blood", RaidType.TOB, Kind.LOBBY, "crypt", List.of(), 0,
                "Steel yourself. If your whole party dies, the Theatre is lost.");
        room("maiden", "The Maiden of Sugadinti", RaidType.TOB, Kind.COMBAT, "crypt", List.of("the_maiden_of_sugadinti"), 0,
                "Kill the Nylocas Matomenos before they reach the Maiden, and avoid her blood.");
        room("bloat", "The Pestilent Bloat", RaidType.TOB, Kind.COMBAT, "crypt", List.of("pestilent_bloat"), 0,
                "The Bloat can only be harmed while it rests. Get away when it wakes and stomps.");
        room("nylocas", "The Nylocas", RaidType.TOB, Kind.COMBAT, "crypt", List.of(), 0,
                "Grey Nylocas die to melee, green to ranged and blue to magic. Then face their queen.");
        room("sotetseg", "Sotetseg", RaidType.TOB, Kind.COMBAT, "lava", List.of("sotetseg"), 0,
                "Share the death ball with your team, and find the hidden path through the maze.");
        room("xarpus", "Xarpus", RaidType.TOB, Kind.COMBAT, "water", List.of("xarpus"), 0,
                "Absorb the exhumed acid, dodge the spit and never attack Xarpus while he faces you.");
        room("verzik", "Verzik Vitur", RaidType.TOB, Kind.FINAL, "crypt", List.of("verzik_vitur"), 0,
                "Hide behind the pillars from her electric blasts. She has three forms.");
        room("tob_reward", "Treasure room", RaidType.TOB, Kind.REWARD, "crypt", List.of(), 0,
                "Claim the Monumental chest's reward.");

        // --- Tombs of Amascut ---
        room("toa_lobby", "Tombs of Amascut", RaidType.TOA, Kind.LOBBY, "sand", List.of(), 0,
                "Choose your invocations at the obelisk to set the raid level, then begin.");
        room("scabaras_puzzle", "Path of Scabaras", RaidType.TOA, Kind.PUZZLE, "sand", List.of(), RaidScaling.TOA_ROOM_POINTS,
                "Watch the order the tiles light up, then step on them in the same order.");
        room("kephri", "Kephri", RaidType.TOA, Kind.COMBAT, "sand", List.of("kephri"), RaidScaling.TOA_ROOM_POINTS,
                "Kephri's shield regenerates; kill her scarabs before they reach her.");
        room("het_puzzle", "Path of Het", RaidType.TOA, Kind.PUZZLE, "sand", List.of(), RaidScaling.TOA_ROOM_POINTS,
                "Mine through Het's seal while the light of the sun burns the room.");
        room("akkha", "Akkha", RaidType.TOA, Kind.COMBAT, "sand", List.of("akkha"), RaidScaling.TOA_ROOM_POINTS,
                "Akkha switches styles and splits off shadows. Kill his shadow to make him vulnerable.");
        room("apmeken_puzzle", "Path of Apmeken", RaidType.TOA, Kind.PUZZLE, "jungle", List.of(), RaidScaling.TOA_ROOM_POINTS,
                "Listen to Apmeken's sight: repair the pillars, vents and roof supports as they fail.");
        room("baba", "Ba-Ba", RaidType.TOA, Kind.COMBAT, "jungle", List.of("baba"), RaidScaling.TOA_ROOM_POINTS,
                "Destroy the rolling boulders and stay out of the falling rocks.");
        room("crondis_puzzle", "Path of Crondis", RaidType.TOA, Kind.PUZZLE, "water", List.of(), RaidScaling.TOA_ROOM_POINTS,
                "Carry water from the container to the Palm of Resourcefulness.");
        room("zebak", "Zebak", RaidType.TOA, Kind.COMBAT, "water", List.of("zebak"), RaidScaling.TOA_ROOM_POINTS,
                "Hide behind the jugs when Zebak sends a wave.");
        room("wardens", "The Wardens", RaidType.TOA, Kind.FINAL, "sand", List.of("wardens_obelisk"), RaidScaling.TOA_ROOM_POINTS,
                "Destroy the obelisk, then defeat the Wardens.");
        room("toa_reward", "Chest room", RaidType.TOA, Kind.REWARD, "sand", List.of(), 0,
                "Open the sarcophagus chest for your reward.");
    }

    private RaidLayouts() {
    }

    private static void room(String id, String name, RaidType raid, Kind kind, String style, List<String> bosses, int points,
                             String hint) {
        ROOMS.put(id, new RoomDef(id, name, raid, kind, style, bosses, points, hint));
    }

    public static RoomDef room(String id) {
        return ROOMS.get(id);
    }

    public static Map<String, RoomDef> all() {
        return Collections.unmodifiableMap(ROOMS);
    }

    /** A Chambers of Xeric raid: lobby, two floors of combat + puzzle rooms, the Great Olm, reward. */
    public static List<RoomDef> cox(Random random) {
        List<String> rotation = COX_ROTATIONS.get(random.nextInt(COX_ROTATIONS.size()));
        int start = random.nextInt(rotation.size());
        int dir = random.nextBoolean() ? 1 : -1;
        List<String> combats = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            combats.add(rotation.get(Math.floorMod(start + dir * i, rotation.size())));
        }
        List<String> puzzles = new ArrayList<>(COX_PUZZLES);
        Collections.shuffle(puzzles, random);
        List<String> ids = List.of("cox_lobby", combats.get(0), puzzles.get(0), combats.get(1),
                combats.get(2), puzzles.get(1), combats.get(3), "olm", "cox_reward");
        return resolve(ids);
    }

    /** The Theatre of Blood always runs in the same order. */
    public static List<RoomDef> tob() {
        return resolve(List.of("tob_lobby", "maiden", "bloat", "nylocas", "sotetseg", "xarpus", "verzik", "tob_reward"));
    }

    /** Tombs of Amascut: the four paths in a random order (chosen by the party in OSRS), then the Wardens. */
    public static List<RoomDef> toa(Random random) {
        List<List<String>> paths = new ArrayList<>(TOA_PATHS);
        Collections.shuffle(paths, random);
        List<String> ids = new ArrayList<>();
        ids.add("toa_lobby");
        for (List<String> p : paths) {
            ids.addAll(p);
        }
        ids.add("wardens");
        ids.add("toa_reward");
        return resolve(ids);
    }

    public static List<RoomDef> layout(RaidType type, Random random) {
        return switch (type) {
            case COX -> cox(random);
            case TOB -> tob();
            case TOA -> toa(random);
        };
    }

    private static List<RoomDef> resolve(List<String> ids) {
        List<RoomDef> list = new ArrayList<>();
        for (String id : ids) {
            RoomDef r = ROOMS.get(id);
            if (r == null) {
                throw new IllegalStateException("Unknown raid room " + id);
            }
            list.add(r);
        }
        return Collections.unmodifiableList(list);
    }
}
