package com.runecraft.osrs;

import java.util.Set;

/**
 * Where fired ranged ammunition goes (OSRS Wiki, Ava's device, "Effectiveness"; Ava's accumulator, ranging cape, max cape):
 * 20% breaks on impact; without an Ava's device the other 80% drops to the floor under the target; a worn device returns part
 * of the fired ammo to the quiver and drops the rest of the 80% (attractor 60% back / 20% dropped, accumulator 72% / 8%,
 * assembler 80% / 0%). The ranging cape and the max cape work as an accumulator (a Vorkath's head upgrade to assembler rates
 * isn't modelled). A metal torso stops every device ("metallic interference"; table snapshot in the tests).
 *
 * <p>Arrows, bolts, darts, javelins, throwing knives, thrownaxes and toktz-xil-ul can be recovered; ammunition that is always
 * consumed (chinchompas, holy water) never drops.
 */
public final class AmmoRecovery {
    /** Percent of fired ammo that breaks on impact, with or without a device. */
    public static final int BREAK_PERCENT = 20;

    /** The worn ammo-saving device. */
    public enum Device {
        NONE(0), ATTRACTOR(60), ACCUMULATOR(72), ASSEMBLER(80);

        /** Percent of fired ammo returned to the quiver. */
        public final int recoverPercent;

        Device(int recoverPercent) {
            this.recoverPercent = recoverPercent;
        }

        /** Percent of fired ammo dropped under the target: what neither breaks nor comes back. */
        public int dropPercent() {
            return 100 - BREAK_PERCENT - recoverPercent;
        }
    }

    /** What happened to one fired piece of ammo. */
    public enum Outcome { BROKE, DROPPED, RECOVERED }

    private static final Set<String> ATTRACTORS = Set.of("avas_attractor");
    private static final Set<String> ACCUMULATORS = Set.of("avas_accumulator", "accumulator_max_cape", "ranged_cape",
            "ranged_cape_t", "max_cape");
    private static final Set<String> ASSEMBLERS = Set.of("avas_assembler", "assembler_max_cape", "masori_assembler",
            "masori_assembler_max_cape");

    /**
     * Torso armour whose metal stops Ava's devices, the ranging cape and the max cape (OSRS Wiki, Ava's device, "Metallic
     * interference"; bronze, heraldic (h1-h5), most trimmed and god-trimmed bodies, Bandos, Torva and Armadyl are compatible).
     */
    public static final Set<String> INTERFERING = Set.of(
            "iron_chainbody", "iron_platebody", "steel_platebody", "black_chainbody", "black_platebody", "black_platebody_g",
            "black_platebody_t", "mithril_chainbody", "mithril_platebody", "adamant_chainbody", "adamant_platebody",
            "adamant_platebody_g", "adamant_platebody_t", "rune_chainbody", "rune_platebody", "rune_platebody_g",
            "rune_platebody_t", "dragon_chainbody", "dragon_platebody", "elite_black_platebody", "gilded_platebody",
            "saradomin_platebody", "zamorak_platebody", "guthix_platebody", "gold_decorative_body", "red_decorative_body",
            "white_decorative_body", "ahrims_robetop", "dharoks_platebody", "guthans_platebody", "torags_platebody",
            "veracs_brassard", "justiciar_chestguard", "statiuss_platebody_bh",
            // Last Man Standing copies of the Barrows bodies
            "lms_ahrims_robetop", "lms_dharoks_platebody");

    private AmmoRecovery() {
    }

    /** The device a worn cape gives, with the worn torso (a metal one stops it); either id may be null. */
    public static Device device(String capeId, String bodyId) {
        if (capeId == null || (bodyId != null && INTERFERING.contains(bodyId))) {
            return Device.NONE;
        }
        if (ASSEMBLERS.contains(capeId)) {
            return Device.ASSEMBLER;
        }
        if (ACCUMULATORS.contains(capeId)) {
            return Device.ACCUMULATOR;
        }
        return ATTRACTORS.contains(capeId) ? Device.ATTRACTOR : Device.NONE;
    }

    /** Whether fired ammo of this item can drop or come back (always-consumed ammunition can't). */
    public static boolean recoverable(String ammoId) {
        return ammoId != null && !ammoId.isEmpty() && !ammoId.contains("chinchompa") && !ammoId.contains("holy_water");
    }

    /** One fired piece of ammo, from a uniform roll in [0, 100). */
    public static Outcome outcome(Device device, int roll) {
        if (roll < device.recoverPercent) {
            return Outcome.RECOVERED;
        }
        if (roll < device.recoverPercent + device.dropPercent()) {
            return Outcome.DROPPED;
        }
        return Outcome.BROKE;
    }
}
