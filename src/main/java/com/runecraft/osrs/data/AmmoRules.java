package com.runecraft.osrs.data;

import java.util.Map;

/**
 * Which ammunition a bow or crossbow can fire, as in OSRS: each bow fires arrows up to a metal tier
 * (normal bows iron, oak steel, willow mithril, maple adamant, yew rune, magic amethyst, dark/twisted bow dragon)
 * and each crossbow bolts up to its own tier (gem-tipped bolts count as the metal bolt they are made from).
 * Weapons not listed here (crystal bows, blowpipe, Karil's crossbow, salamanders...) keep firing anything.
 */
public final class AmmoRules {
    private AmmoRules() {
    }

    private enum Kind { ARROW, BOLT }

    private record Ammo(Kind kind, int rank) {
    }

    private record Launcher(Kind kind, int maxRank) {
    }

    private static final int BRONZE = 1, IRON = 2, STEEL = 3, MITHRIL = 4, ADAMANT = 5, RUNE = 6, AMETHYST = 7, DRAGON = 8;
    private static final int B_BRONZE = 1, B_BLURITE = 2, B_IRON = 3, B_STEEL = 4, B_MITHRIL = 5, B_ADAMANT = 6, B_RUNE = 7, B_DRAGON = 8;

    private static final Map<String, Ammo> AMMO = Map.ofEntries(
            Map.entry("bronze_arrow", new Ammo(Kind.ARROW, BRONZE)),
            Map.entry("iron_arrow", new Ammo(Kind.ARROW, IRON)),
            Map.entry("steel_arrow", new Ammo(Kind.ARROW, STEEL)),
            Map.entry("mithril_arrow", new Ammo(Kind.ARROW, MITHRIL)),
            Map.entry("adamant_arrow", new Ammo(Kind.ARROW, ADAMANT)),
            Map.entry("rune_arrow", new Ammo(Kind.ARROW, RUNE)),
            Map.entry("broad_arrows", new Ammo(Kind.ARROW, BRONZE)), // any bow (Slayer reward)
            Map.entry("amethyst_arrow", new Ammo(Kind.ARROW, AMETHYST)),
            Map.entry("dragon_arrow", new Ammo(Kind.ARROW, DRAGON)),
            Map.entry("bronze_bolts", new Ammo(Kind.BOLT, B_BRONZE)),
            Map.entry("opal_bolts", new Ammo(Kind.BOLT, B_BRONZE)),
            Map.entry("silver_bolts", new Ammo(Kind.BOLT, B_BRONZE)), // approx: usable from any crossbow
            Map.entry("broad_bolts", new Ammo(Kind.BOLT, B_BRONZE)), // approx: usable from any crossbow
            Map.entry("amethyst_broad_bolts", new Ammo(Kind.BOLT, B_BRONZE)), // approx: like broad bolts
            Map.entry("kebbit_bolts", new Ammo(Kind.BOLT, B_BRONZE)),
            Map.entry("long_kebbit_bolts", new Ammo(Kind.BOLT, B_BRONZE)),
            Map.entry("blurite_bolts", new Ammo(Kind.BOLT, B_BLURITE)),
            Map.entry("jade_bolts", new Ammo(Kind.BOLT, B_BLURITE)),
            Map.entry("iron_bolts", new Ammo(Kind.BOLT, B_IRON)),
            Map.entry("pearl_bolts", new Ammo(Kind.BOLT, B_IRON)),
            Map.entry("steel_bolts", new Ammo(Kind.BOLT, B_STEEL)),
            Map.entry("topaz_bolts", new Ammo(Kind.BOLT, B_STEEL)),
            Map.entry("mithril_bolts", new Ammo(Kind.BOLT, B_MITHRIL)),
            Map.entry("sapphire_bolts", new Ammo(Kind.BOLT, B_MITHRIL)),
            Map.entry("emerald_bolts", new Ammo(Kind.BOLT, B_MITHRIL)),
            Map.entry("adamant_bolts", new Ammo(Kind.BOLT, B_ADAMANT)),
            Map.entry("ruby_bolts", new Ammo(Kind.BOLT, B_ADAMANT)),
            Map.entry("diamond_bolts", new Ammo(Kind.BOLT, B_ADAMANT)),
            Map.entry("rune_bolts", new Ammo(Kind.BOLT, B_RUNE)),
            Map.entry("dragonstone_bolts", new Ammo(Kind.BOLT, B_RUNE)),
            Map.entry("onyx_bolts", new Ammo(Kind.BOLT, B_RUNE)),
            Map.entry("dragon_bolts", new Ammo(Kind.BOLT, B_DRAGON)));

    private static final Map<String, Launcher> LAUNCHERS = Map.ofEntries(
            Map.entry("shortbow", new Launcher(Kind.ARROW, IRON)),
            Map.entry("longbow", new Launcher(Kind.ARROW, IRON)),
            Map.entry("oak_shortbow", new Launcher(Kind.ARROW, STEEL)),
            Map.entry("oak_longbow", new Launcher(Kind.ARROW, STEEL)),
            Map.entry("willow_shortbow", new Launcher(Kind.ARROW, MITHRIL)),
            Map.entry("willow_longbow", new Launcher(Kind.ARROW, MITHRIL)),
            Map.entry("willow_comp_bow", new Launcher(Kind.ARROW, MITHRIL)),
            Map.entry("maple_shortbow", new Launcher(Kind.ARROW, ADAMANT)),
            Map.entry("maple_longbow", new Launcher(Kind.ARROW, ADAMANT)),
            Map.entry("yew_shortbow", new Launcher(Kind.ARROW, RUNE)),
            Map.entry("yew_longbow", new Launcher(Kind.ARROW, RUNE)),
            Map.entry("yew_comp_bow", new Launcher(Kind.ARROW, RUNE)),
            Map.entry("magic_shortbow", new Launcher(Kind.ARROW, AMETHYST)),
            Map.entry("magic_longbow", new Launcher(Kind.ARROW, AMETHYST)),
            Map.entry("magic_comp_bow", new Launcher(Kind.ARROW, AMETHYST)),
            Map.entry("seercull", new Launcher(Kind.ARROW, AMETHYST)),
            Map.entry("3rd_age_bow", new Launcher(Kind.ARROW, AMETHYST)), // approx
            Map.entry("dark_bow", new Launcher(Kind.ARROW, DRAGON)),
            Map.entry("twisted_bow", new Launcher(Kind.ARROW, DRAGON)),
            Map.entry("venator_bow", new Launcher(Kind.ARROW, DRAGON)),
            Map.entry("bronze_crossbow", new Launcher(Kind.BOLT, B_BRONZE)),
            Map.entry("blurite_crossbow", new Launcher(Kind.BOLT, B_BLURITE)),
            Map.entry("iron_crossbow", new Launcher(Kind.BOLT, B_IRON)),
            Map.entry("steel_crossbow", new Launcher(Kind.BOLT, B_STEEL)),
            Map.entry("mithril_crossbow", new Launcher(Kind.BOLT, B_MITHRIL)),
            Map.entry("adamant_crossbow", new Launcher(Kind.BOLT, B_ADAMANT)),
            Map.entry("rune_crossbow", new Launcher(Kind.BOLT, B_RUNE)),
            Map.entry("dragon_crossbow", new Launcher(Kind.BOLT, B_DRAGON)),
            Map.entry("dragon_hunter_crossbow", new Launcher(Kind.BOLT, B_DRAGON)),
            Map.entry("armadyl_crossbow", new Launcher(Kind.BOLT, B_DRAGON)),
            Map.entry("zaryte_crossbow", new Launcher(Kind.BOLT, B_DRAGON)));

    /** The light and heavy ballista fire javelins of any metal, and nothing else. */
    private static final java.util.Set<String> BALLISTAE = java.util.Set.of("light_ballista", "heavy_ballista");

    /** Antler bolts are only fired by the Hunters' sunlight crossbow, which fires nothing else. */
    private static boolean antler(String id) {
        return id != null && id.endsWith("_antler_bolts");
    }

    /** Enchanted bolts fire like their plain bolts; every gem-tipped dragon bolt needs a dragon-tier crossbow. */
    private static Ammo ammo(String id) {
        if (id.endsWith("_e")) {
            id = id.substring(0, id.length() - 2);
        }
        Ammo a = AMMO.get(id);
        if (a == null && id.endsWith("_dragon_bolts")) {
            a = new Ammo(Kind.BOLT, B_DRAGON);
        }
        return a;
    }

    /** True when {@code weaponId} has an ammunition restriction. */
    public static boolean restricted(String weaponId) {
        return LAUNCHERS.containsKey(weaponId) || "hunters_sunlight_crossbow".equals(weaponId) || BALLISTAE.contains(weaponId);
    }

    /**
     * Whether {@code weaponId} can fire {@code ammoId} (a RuneCraft item id, or null for non-RuneCraft ammo such as
     * vanilla arrows).
     */
    public static boolean canFire(String weaponId, String ammoId) {
        if ("hunters_sunlight_crossbow".equals(weaponId)) {
            return antler(ammoId);
        }
        if (BALLISTAE.contains(weaponId)) {
            return ammoId != null && ammoId.endsWith("_javelin");
        }
        Launcher l = LAUNCHERS.get(weaponId);
        if (l == null) {
            return true;
        }
        Ammo a = ammoId == null ? null : ammo(ammoId);
        return a != null && a.kind() == l.kind() && a.rank() <= l.maxRank();
    }
}
