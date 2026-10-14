package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.data.EquipmentBonuses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Enchanted jewellery and other items worn in the extra equipment slots this module adds (neck, ring,
 * cape, hands). Minecraft has no such slots, so worn items live in the systems module's state and their
 * bonuses are applied through combat hooks. Stats are the real OSRS bonuses.
 */
public final class Jewellery {
    public enum Slot { NECK, RING, CAPE, HANDS, AMMO }

    /** Special behaviour of a worn item. */
    public enum Perk {
        NONE, RECOIL, SUFFERING, LIFE, PHOENIX, WEALTH, BERSERKER, REGEN, FORGING, DODGY, CLAY, INOCULATION, BINDING,
        LIGHTBEARER, SALVE, TELEPORT_ONLY
    }

    /**
     * A wearable item. {@code chargeBase} groups charged variants (e.g. amulet_of_glory_1..6); {@code charges}
     * is the number of teleport charges this exact item holds (-1 for unlimited/none).
     */
    public record Worn(String id, String name, Slot slot, EquipmentStats stats, Perk perk, String chargeBase, int charges,
                       List<Teleports.Destination> teleports, boolean crumbles) {
    }

    private static final Map<String, Worn> WORN = new LinkedHashMap<>();
    private static final List<Item> ITEMS = new ArrayList<>();

    private record Item(String id, String name, String shape, int color, int value, String examine) {
    }

    public static EquipmentStats s(int stab, int slash, int crush, int mag, int rng, int dStab, int dSlash, int dCrush,
                                   int dMag, int dRng, int str, int rStr, int mDmg, int pray) {
        return new EquipmentStats(stab, slash, crush, mag, rng, dStab, dSlash, dCrush, dMag, dRng, str, rStr, mDmg, pray);
    }

    private static final EquipmentStats NONE = EquipmentStats.NONE;
    /** Slayer ring teleport charges (OSRS: 8). */
    public static final int SLAYER_RING_CHARGES = 8;
    /** Ring of returning teleport charges (OSRS: 5). */
    public static final int RING_OF_RETURNING_CHARGES = 5;
    /**
     * Teleport items held in the hand rather than worn in an extra slot (the pharaoh's sceptre is a weapon): their
     * teleport menu works like jewellery's, but they can't go in a jewellery slot.
     */
    private static final Map<String, Worn> HELD = new LinkedHashMap<>();

    static {
        // ---------- Sapphire (Lvl-1 Enchant) ----------
        worn("ring_of_recoil", "Ring of recoil", Slot.RING, "ring", 0x3050C0, 900, NONE, Perk.RECOIL,
                "An enchanted ring that damages opponents who hit you.");
        charged("games_necklace", "Games necklace", Slot.NECK, "necklace", 0x3050C0, 1050, NONE, 8, Teleports.GAMES, true);
        worn("amulet_of_magic", "Amulet of magic", Slot.NECK, "amulet", 0x3050C0, 900, s(0, 0, 0, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                Perk.NONE, "A magical amulet.");
        worn("bracelet_of_clay", "Bracelet of clay", Slot.HANDS, "bracelet", 0x3050C0, 900, NONE, Perk.CLAY,
                "Softens clay as you mine it.");
        // ---------- Emerald (Lvl-2) ----------
        charged("ring_of_dueling", "Ring of dueling", Slot.RING, "ring", 0x30A050, 975, NONE, 8, Teleports.DUELING, true);
        worn("binding_necklace", "Binding necklace", Slot.NECK, "necklace", 0x30A050, 1275, NONE, Perk.BINDING,
                "Guarantees combination rune crafting.");
        worn("amulet_of_defence", "Amulet of defence", Slot.NECK, "amulet", 0x30A050, 1275, s(0, 0, 0, 0, 0, 7, 7, 7, 7, 7, 0, 0, 0, 0),
                Perk.NONE, "A defensive amulet.");
        worn("amulet_of_defence_t", "Amulet of defence (t)", Slot.NECK, "amulet", 0x50C070, 1275, s(0, 0, 0, 0, 0, 7, 7, 7, 7, 7, 0, 0, 0, 0),
                Perk.NONE, "A trimmed amulet of defence.");
        worn("castle_wars_bracelet", "Castle wars bracelet", Slot.HANDS, "bracelet", 0x30A050, 1275, NONE, Perk.NONE,
                "Increases tickets earned at Castle Wars.");
        // ---------- Ruby (Lvl-3) ----------
        worn("ring_of_forging", "Ring of forging", Slot.RING, "ring", 0xC03030, 2025, NONE, Perk.FORGING,
                "Guarantees iron smelting success.");
        worn("dodgy_necklace", "Dodgy necklace", Slot.NECK, "necklace", 0xC03030, 2025, NONE, Perk.DODGY,
                "Protects you from being caught while pickpocketing.");
        worn("amulet_of_strength", "Amulet of strength", Slot.NECK, "amulet", 0xC03030, 2025, s(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10, 0, 0, 0),
                Perk.NONE, "An enchanted ruby amulet.");
        worn("amulet_of_strength_t", "Strength amulet (t)", Slot.NECK, "amulet", 0xE05050, 2025, s(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10, 0, 0, 0),
                Perk.NONE, "A trimmed strength amulet.");
        worn("inoculation_bracelet", "Inoculation bracelet", Slot.HANDS, "bracelet", 0xC03030, 2025, NONE, Perk.INOCULATION,
                "Protects you from disease.");
        // ---------- Diamond (Lvl-4) ----------
        worn("ring_of_life", "Ring of life", Slot.RING, "ring", 0xE0E0F0, 3525, NONE, Perk.LIFE,
                "Teleports you to safety when your life is in danger.");
        worn("phoenix_necklace", "Phoenix necklace", Slot.NECK, "necklace", 0xE0E0F0, 3525, NONE, Perk.PHOENIX,
                "Heals you when you are about to die.");
        worn("amulet_of_power", "Amulet of power", Slot.NECK, "amulet", 0xE0E0F0, 3525, s(6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 0, 0, 1),
                Perk.NONE, "A powerful amulet.");
        charged("abyssal_bracelet", "Abyssal bracelet", Slot.HANDS, "bracelet", 0xE0E0F0, 4200, NONE, 5, List.of(), true);
        // ---------- Dragonstone (Lvl-5) ----------
        charged("ring_of_wealth", "Ring of wealth", Slot.RING, "ring", 0xA040C0, 17625, NONE, 5, Teleports.WEALTH, false);
        charged("skills_necklace", "Skills necklace", Slot.NECK, "necklace", 0xA040C0, 17625, NONE, 6, Teleports.SKILLS, false);
        charged("amulet_of_glory", "Amulet of glory", Slot.NECK, "amulet", 0xA040C0, 17625, s(10, 10, 10, 10, 10, 3, 3, 3, 3, 3, 6, 0, 0, 3),
                6, Teleports.GLORY, false);
        charged("amulet_of_glory_t", "Amulet of glory (t)", Slot.NECK, "amulet", 0xC060E0, 17625, s(10, 10, 10, 10, 10, 3, 3, 3, 3, 3, 6, 0, 0, 3),
                6, Teleports.GLORY, false);
        charged("combat_bracelet", "Combat bracelet", Slot.HANDS, "bracelet", 0xA040C0, 17625, s(7, 7, 7, 3, 7, 5, 5, 5, 5, 5, 6, 0, 0, 3),
                6, Teleports.COMBAT, false);
        worn("amulet_of_eternal_glory", "Amulet of eternal glory", Slot.NECK, "amulet", 0xE0A0FF, 17625,
                s(10, 10, 10, 10, 10, 3, 3, 3, 3, 3, 6, 0, 0, 3), Perk.TELEPORT_ONLY, "An amulet with unlimited charges.");
        teleportsFor("amulet_of_eternal_glory", Teleports.GLORY);
        // ---------- Onyx (Lvl-6) ----------
        worn("ring_of_stone", "Ring of stone", Slot.RING, "ring", 0x303030, 202500, NONE, Perk.NONE, "Turns you into a rock.");
        worn("berserker_necklace", "Berserker necklace", Slot.NECK, "necklace", 0x303030, 202500, s(-10, -10, -10, 0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0),
                Perk.BERSERKER, "Increases the power of obsidian weapons by 20%.");
        worn("amulet_of_fury", "Amulet of fury", Slot.NECK, "amulet", 0x303030, 202500, s(10, 10, 10, 10, 10, 15, 15, 15, 15, 15, 8, 0, 0, 5),
                Perk.NONE, "A very powerful onyx amulet.");
        worn("regen_bracelet", "Regen bracelet", Slot.HANDS, "bracelet", 0x303030, 202500, s(8, 8, 8, 0, 8, 8, 8, 8, 8, 8, 7, 0, 0, 3),
                Perk.REGEN, "Doubles your hitpoint regeneration.");
        // ---------- Zenyte (Lvl-7) ----------
        worn("ring_of_suffering", "Ring of suffering", Slot.RING, "ring", 0xF0A020, 3000000, s(0, 0, 0, 0, 0, 20, 20, 20, 20, 20, 0, 0, 0, 4),
                Perk.SUFFERING, "A ring that recoils damage when charged with rings of recoil.");
        worn("necklace_of_anguish", "Necklace of anguish", Slot.NECK, "necklace", 0xF0A020, 3000000, s(0, 0, 0, 0, 15, 0, 0, 0, 0, 0, 0, 5, 0, 2),
                Perk.NONE, "A ranger's necklace.");
        worn("amulet_of_torture", "Amulet of torture", Slot.NECK, "amulet", 0xF0A020, 3000000, s(15, 15, 15, 0, 0, 0, 0, 0, 0, 0, 10, 0, 0, 2),
                Perk.NONE, "A melee fighter's amulet.");
        worn("tormented_bracelet", "Tormented bracelet", Slot.HANDS, "bracelet", 0xF0A020, 3000000, s(0, 0, 0, 10, 0, 0, 0, 0, 0, 0, 0, 0, 5, 0),
                Perk.NONE, "A mage's bracelet.");
        // Upgrades (wiki / DPS calculator bonuses; both need 90 Hitpoints in OSRS).
        worn("necklace_of_rupture", "Necklace of rupture", Slot.NECK, "necklace", 0xC02040, 4_000_000,
                s(0, 0, 0, 0, 20, 0, 0, 0, 0, 0, 0, 8, 0, 3), Perk.NONE, "A necklace of anguish set with an elder venator fang.");
        worn("confliction_gauntlets", "Confliction gauntlets", Slot.HANDS, "bracelet", 0x8A3A9A, 4_000_000,
                s(0, 0, 0, 20, -4, 15, 18, 7, 5, 5, 0, 0, 7, 2), Perk.NONE,
                "A tormented bracelet woven with Mokhaiotl cloth. A missed spell makes the next one roll accuracy twice.");
        // ---------- Other worn items ----------
        charged("digsite_pendant", "Digsite pendant", Slot.NECK, "necklace", 0xB08040, 1200, NONE, 5, Teleports.DIGSITE, true);
        charged("necklace_of_passage", "Necklace of passage", Slot.NECK, "necklace", 0x70A0D0, 2000, NONE, 5, Teleports.PASSAGE, true);
        charged("burning_amulet", "Burning amulet", Slot.NECK, "amulet", 0xE06020, 2000, NONE, 5, Teleports.BURNING, true);
        // Xeric's talisman: one charge per lizardman fang, up to 1,000 (per-stack charges: Charges.XERICS_TALISMAN).
        worn("xerics_talisman", "Xeric's talisman", Slot.NECK, "amulet", 0x60B0A0, 3000, NONE, Perk.TELEPORT_ONLY,
                "An enervated relic of the ancient times of Kourend.");
        teleportsFor("xerics_talisman", Teleports.XERIC);
        // Slayer ring (items defined by the slayer module): 8 teleports, then it crumbles; the eternal ring never runs out.
        chargedExternal("slayer_ring", "Slayer ring", Slot.RING, SLAYER_RING_CHARGES, Teleports.SLAYER);
        WORN.put("slayer_ring_eternal", new Worn("slayer_ring_eternal", "Slayer ring (eternal)", Slot.RING, NONE, Perk.TELEPORT_ONLY,
                null, -1, Teleports.SLAYER, false));
        worn("drakans_medallion", "Drakan's medallion", Slot.NECK, "amulet", 0x902020, 3000, NONE, Perk.TELEPORT_ONLY, "A medallion of the Drakan family.");
        teleportsFor("drakans_medallion", Teleports.DRAKAN);
        // Pharaoh's sceptre (minigames module weapon): wielded, not worn here; per-stack charges (Charges.PHARAOHS_SCEPTRE).
        HELD.put("pharaohs_sceptre", new Worn("pharaohs_sceptre", "Pharaoh's sceptre", Slot.RING, NONE, Perk.TELEPORT_ONLY, null, -1,
                Teleports.PHARAOH, false));
        worn("holy_symbol", "Holy symbol", Slot.NECK, "amulet", 0xE0E0E0, 300, s(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8),
                Perk.NONE, "A blessed holy symbol of Saradomin.");
        worn("unholy_symbol", "Unholy symbol", Slot.NECK, "amulet", 0x404040, 300, s(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8),
                Perk.NONE, "An unholy symbol of Zamorak.");
        worn("lightbearer", "Lightbearer", Slot.RING, "ring", 0xF0E080, 3000000, NONE, Perk.LIGHTBEARER,
                "Doubles the rate at which special attack energy regenerates.");
        wornExternal("treasonous_ring", Slot.RING, s(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)); // item defined by the godwars module
        wornExternal("tyrannical_ring", Slot.RING, s(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)); // item defined by the godwars module
        wornExternal("ring_of_the_gods", Slot.RING, s(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4)); // item defined by the godwars module
        // Boss accessories (bosses module BossItems.ACCESSORIES) and the Zaryte vambraces (godwars module): stats from the
        // DPS calculator's equipment.json, so they count in the gear totals PlayerHitCalc sees (round 15; were unwearable).
        wornExternal("ultor_ring", Slot.RING, s(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 12, 0, 0, 0));
        wornExternal("bellator_ring", Slot.RING, s(0, 20, 0, 0, 0, 0, 0, 0, 0, 0, 6, 0, 0, 0));
        wornExternal("magus_ring", Slot.RING, s(0, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0));
        wornExternal("venator_ring", Slot.RING, s(0, 0, 0, 0, 10, 0, 0, 0, 0, 0, 0, 2, 0, 0));
        wornExternal("zaryte_vambraces", Slot.HANDS, s(-8, -8, -8, 0, 18, 8, 8, 8, 5, 8, 0, 2, 0, 1));
        wornExternal("granite_gloves", Slot.HANDS, s(5, 5, 9, -3, -1, 8, 8, 8, -3, 5, 7, 0, 0, 0));
        wornExternal("dragonbone_necklace", Slot.NECK, s(10, 10, 10, 10, 10, 2, 2, 2, 2, 2, 2, 0, 0, 12));
        wornExternal("dizanas_quiver", Slot.CAPE, s(0, 0, 0, 0, 18, 0, 0, 0, 0, 0, 0, 3, 0, 0));
        // Bosses-module drops with per-stack teleport charges (Charges): frozen tears / big bones and law runes.
        externalTeleporter("pendant_of_ates", "Pendant of ates", Slot.NECK, s(0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 2),
                Teleports.ATES); // wiki bonuses (EquipmentWikiSyncTest)
        externalTeleporter("giantsoul_amulet", "Giantsoul amulet", Slot.NECK, NONE, Teleports.GIANTSOUL);
        // Guardians of the Rift reward (minigames module): elemental altar teleports, one charge of each elemental rune and
        // a law rune per teleport.
        externalTeleporter("ring_of_the_elements", "Ring of the elements", Slot.RING, NONE, Teleports.ELEMENTS);
        // Ring of returning: a jade ring enchanted with Lvl-2 Enchant; five teleports to the respawn point, then it crumbles.
        charged("ring_of_returning", "Ring of returning", Slot.RING, "ring", 0x50C060, 1140, NONE, RING_OF_RETURNING_CHARGES,
                Teleports.RETURNING, true); // value approx
        worn("ring_of_nature", "Ring of nature", Slot.RING, "ring", 0x30A030, 10000, NONE, Perk.NONE, "A ring of nature.");
        worn("ring_of_coins", "Ring of coins", Slot.RING, "ring", 0xE8C030, 10000, NONE, Perk.NONE, "Turns you into a pile of coins.");
        worn("amulet_of_magic_t", "Amulet of magic (t)", Slot.NECK, "amulet", 0x5070E0, 900, s(0, 0, 0, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                Perk.NONE, "A trimmed amulet of magic.");
        worn("amulet_of_power_t", "Amulet of power (t)", Slot.NECK, "amulet", 0xF0F0FF, 3525, s(6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 0, 0, 1),
                Perk.NONE, "A trimmed amulet of power.");
        // Worn core/common items (stats only).
        wornExternal("amulet_of_accuracy", Slot.NECK, s(4, 4, 4, 4, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        wornExternal("amulet_of_the_damned", Slot.NECK, s(10, 10, 10, 10, 10, 3, 3, 3, 3, 3, 6, 0, 0, 3)); // minigames module
        wornExternal("ghostspeak_amulet", Slot.NECK, s(0, 0, 0, 0, 0, 3, 3, 3, 3, 3, 0, 0, 0, 0));
        wornExternal("fire_cape", Slot.CAPE, s(1, 1, 1, 1, 1, 11, 11, 11, 11, 11, 4, 0, 0, 2));
        wornExternal("infernal_cape", Slot.CAPE, s(4, 4, 4, 1, 1, 12, 12, 12, 12, 12, 8, 0, 0, 2));
        wornExternal("champions_cape", Slot.CAPE, s(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        wornExternal("avas_accumulator", Slot.CAPE, s(0, 0, 0, 0, 4, 0, 1, 0, 4, 0, 0, 0, 0, 0));
        wornExternal("barrows_gloves", Slot.HANDS, s(12, 12, 12, 6, 12, 12, 12, 12, 6, 12, 12, 0, 0, 0));
        wornExternal("ferocious_gloves", Slot.HANDS, s(16, 16, 16, -16, -16, 0, 0, 0, 0, 0, 14, 0, 0, 0));
        wornExternal("occult_necklace", Slot.NECK, s(0, 0, 0, 12, 0, 0, 0, 0, 0, 0, 0, 0, 10, 0));
        // Items defined in osrs/content/CommonEquipment (the salve amulets below are too).
        wornExternal("mystic_gloves_light", Slot.HANDS, s(0, 0, 0, 3, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0));
        wornExternal("mystic_gloves_dark", Slot.HANDS, s(0, 0, 0, 3, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0));
        wornExternal("mystic_gloves", Slot.HANDS, s(0, 0, 0, 3, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0));
        wornExternal("mystic_gloves_dusk", Slot.HANDS, s(0, 0, 0, 3, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0));
        wornExternal("black_cape", Slot.CAPE, s(0, 0, 0, 0, 0, 0, 1, 1, 0, 2, 0, 0, 0, 0));
        // Team capes 1-50 (CommonItems): the same bonuses as the plain coloured capes.
        for (int n = 1; n <= 50; n++) {
            wornExternal(com.runecraft.osrs.content.CommonItems.teamCape(n), Slot.CAPE, s(0, 0, 0, 0, 0, 0, 1, 1, 0, 2, 0, 0, 0, 0));
        }
        // Camulet (quests module, Enakhra's Lament): an amulet that teleports to Enakhra's Temple (per-stack charges:
        // Charges "camulet").
        WORN.put("camulet", new Worn("camulet", "Camulet", Slot.NECK, ref("camulet", NONE), Perk.TELEPORT_ONLY, null, -1,
                Teleports.CAMULET, false));
        wornExternal("pirates_hook", Slot.HANDS, s(0, 0, 0, 0, 0, 1, 5, 3, 0, 0, 0, 0, 0, 0)); // item defined by the minigames module
        wornExternal("salve_amulet", Slot.NECK, s(0, 0, 0, 0, 0, 3, 3, 3, 0, 0, 0, 0, 0, 3));
        wornExternal("salve_amulet_e", Slot.NECK, s(0, 0, 0, 0, 0, 3, 3, 3, 0, 0, 0, 0, 0, 3));
        wornExternal("berserker_ring", Slot.RING, s(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 0, 0));
        wornExternal("archers_ring", Slot.RING, s(0, 0, 0, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        wornExternal("seers_ring", Slot.RING, s(0, 0, 0, 4, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0));
        wornExternal("warrior_ring", Slot.RING, s(0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        wornExternal("steel_ring", Slot.RING, s(0, 0, 0, 0, 0, 24, 24, 24, 0, 0, 0, 0, 0, 0)); // item defined by the bosses module
        // Imbued items (minigames module: Nightmare Zone / Soul Wars imbues; stats per the wiki DPS calculator).
        wornExternal("granite_ring", Slot.RING, s(0, 0, 0, 0, 0, 2, 2, 2, -2, 12, 0, 0, 0, 0)); // item defined by the bosses module
        wornExternal("berserker_ring_i", Slot.RING, s(0, 0, 0, 0, 0, 0, 0, 8, 0, 0, 8, 0, 0, 0));
        wornExternal("archers_ring_i", Slot.RING, s(0, 0, 0, 0, 8, 0, 0, 0, 0, 8, 0, 0, 0, 0));
        wornExternal("seers_ring_i", Slot.RING, s(0, 0, 0, 12, 0, 0, 0, 0, 12, 0, 0, 0, 0, 0));
        wornExternal("warrior_ring_i", Slot.RING, s(0, 8, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0));
        wornExternal("tyrannical_ring_i", Slot.RING, s(0, 0, 8, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0));
        wornExternal("treasonous_ring_i", Slot.RING, s(8, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 0));
        wornExternal("ring_of_the_gods_i", Slot.RING, s(0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 8));
        wornExternal("granite_ring_i", Slot.RING, s(0, 0, 0, 0, 0, 4, 4, 4, -4, 24, 0, 0, 0, 0));
        wornExternal("ring_of_suffering_i", Slot.RING, s(0, 0, 0, 0, 0, 20, 20, 20, 20, 20, 0, 0, 0, 4));
        wornExternal("salve_amulet_i", Slot.NECK, s(0, 0, 0, 0, 0, 3, 3, 3, 0, 0, 0, 0, 0, 3));
        wornExternal("salve_amulet_ei", Slot.NECK, s(0, 0, 0, 0, 0, 3, 3, 3, 0, 0, 0, 0, 0, 3));
        // Minigame gloves and capes (minigames module).
        wornExternal("void_knight_gloves", Slot.HANDS, s(0, 0, 0, 0, 0, 6, 6, 6, 4, 6, 0, 0, 0, 0));
        wornExternal("penance_gloves", Slot.HANDS, s(0, 0, 0, 0, 0, 0, 3, 4, 0, 2, 0, 0, 0, 0));
        wornExternal("infinity_gloves", Slot.HANDS, s(0, 0, 0, 5, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0));
        wornExternal("soul_cape", Slot.CAPE, s(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8));
        wornExternal("brimstone_ring", Slot.RING, s(4, 4, 4, 6, 4, 0, 0, 0, 0, 0, 4, 0, 0, 0));
        wornExternal("obsidian_cape", Slot.CAPE, s(0, 0, 0, 0, 0, 9, 9, 9, 9, 9, 0, 0, 0, 0));
        wornExternal("graceful_cape", Slot.CAPE, NONE);   // item defined by the skilling module
        wornExternal("graceful_gloves", Slot.HANDS, NONE); // item defined by the skilling module
        for (String recolour : new String[]{"_varlamore", "_agility_arena"}) { // the skilling module's recoloured sets
            wornExternal("graceful_cape" + recolour, Slot.CAPE, NONE);
            wornExternal("graceful_gloves" + recolour, Slot.HANDS, NONE);
        }
        wornExternal("gloves_of_silence", Slot.HANDS, s(0, 0, 0, 0, 0, 0, 1, 2, 0, 0, 0, 0, 0, 0)); // skilling module (Hunter)
        wornExternal("rogue_gloves", Slot.HANDS, s(0, 0, 0, 0, 0, 2, 2, 2, 2, 2, 0, 0, 0, 0)); // minigames module (Rogues' Den); counts for rogue double loot
        wornExternal("spotted_cape", Slot.CAPE, s(0, 0, 0, 0, 0, 0, 1, 1, 0, 2, 0, 0, 0, 0));   // skilling module (Hunter)
        wornExternal("spottier_cape", Slot.CAPE, s(0, 0, 0, 0, 0, 0, 1, 1, 0, 2, 0, 0, 0, 0)); // skilling module (Hunter)
        wornExternal("ectophial", Slot.NECK, NONE);
        // Monster-drop gloves and cloak (CommonEquipment: Waterbirth dagannoth, rock crabs, wallasalki, crawling hands,
        // Ancient Prison).
        for (String gloves : new String[]{"spined_gloves", "rock_shell_gloves", "skeletal_gloves", "fremennik_gloves",
                "purple_gloves", "red_gloves", "teal_gloves", "yellow_gloves"}) {
            wornExternal(gloves, Slot.HANDS, s(0, 0, 0, 0, 0, 0, 1, 2, 0, 0, 0, 0, 0, 0));
        }
        wornExternal("ancient_ceremonial_gloves", Slot.HANDS, s(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1));
        wornExternal("fremennik_brown_cloak", Slot.CAPE, s(0, 0, 0, 0, 0, 0, 1, 1, 0, 2, 0, 0, 0, 0));
    }

    private Jewellery() {
    }

    private static void worn(String id, String name, Slot slot, String shape, int color, int value, EquipmentStats stats, Perk perk,
                             String examine) {
        WORN.put(id, new Worn(id, name, slot, ref(id, stats), perk, null, -1, List.of(), false));
        ITEMS.add(new Item(id, name, shape, color, value, examine));
    }

    private static void teleportsFor(String id, List<Teleports.Destination> dests) {
        Worn w = WORN.get(id);
        WORN.put(id, new Worn(w.id(), w.name(), w.slot(), w.stats(), w.perk(), null, -1, dests, false));
    }

    /** A worn item another module defines that teleports (charges kept per stack, see {@link Charges}). */
    private static void externalTeleporter(String id, String name, Slot slot, EquipmentStats stats, List<Teleports.Destination> dests) {
        WORN.put(id, new Worn(id, name, slot, ref(id, stats), Perk.TELEPORT_ONLY, null, -1, dests, false));
    }

    private static void wornExternal(String id, Slot slot, EquipmentStats stats) {
        WORN.putIfAbsent(id, new Worn(id, id, slot, ref(id, stats), Perk.NONE, null, -1, List.of(), false));
    }

    /** Worn charge states {@code base_1 .. base_max} ("Name (n)") of items another module defines; they crumble when empty. */
    private static void chargedExternal(String base, String name, Slot slot, int max, List<Teleports.Destination> dests) {
        for (int n = max; n >= 1; n--) {
            String id = base + "_" + n;
            WORN.put(id, new Worn(id, name + " (" + n + ")", slot, ref(id, NONE), Perk.TELEPORT_ONLY, base, n, dests, true));
        }
    }

    /**
     * Defines {@code base_1 .. base_max} (named "Name(n)") plus, unless the item crumbles to dust, the
     * uncharged {@code base}.
     */
    private static void charged(String base, String name, Slot slot, String shape, int color, int value, EquipmentStats stats, int max,
                                List<Teleports.Destination> dests, boolean crumbles) {
        for (int n = max; n >= 1; n--) {
            String id = base + "_" + n;
            Perk perk = base.startsWith("ring_of_wealth") ? Perk.WEALTH : Perk.NONE;
            WORN.put(id, new Worn(id, name + "(" + n + ")", slot, ref(id, stats), perk, base, n, dests, crumbles));
            ITEMS.add(new Item(id, name + "(" + n + ")", shape, color, value, "A " + name.toLowerCase() + " with " + n
                    + " charge" + (n == 1 ? "" : "s") + " left."));
        }
        if (!crumbles) {
            Perk perk = base.startsWith("ring_of_wealth") ? Perk.WEALTH : Perk.NONE;
            WORN.put(base, new Worn(base, name, slot, ref(base, stats), perk, base, 0, List.of(), false));
            ITEMS.add(new Item(base, name, shape, color, value / 2, "It has no charges left; recharge it at the Fountain of Heroes."));
        }
    }

    public static void items(ItemSink items) {
        for (Item i : ITEMS) {
            items.add(i.id(), i.name(), Kind.SIMPLE, i.shape(), i.color()).value(i.value()).stack(1).examine(i.examine());
        }
    }

    /** Worn items defined by other tables register while the item catalog is built. */
    private static void ensureLoaded() {
        com.runecraft.osrs.ItemCatalog.exists("coins");
    }

    /** Lets other data tables (capes, diary rewards) register worn items. */
    public static void register(Worn w) {
        EquipmentStats stats = ref(w.id(), w.stats());
        WORN.put(w.id(), stats.equals(w.stats()) ? w
                : new Worn(w.id(), w.name(), w.slot(), stats, w.perk(), w.chargeBase(), w.charges(), w.teleports(), w.crumbles()));
    }

    /** The OSRS reference bonuses for a worn item (see {@link EquipmentBonuses}), else the given stats. */
    private static EquipmentStats ref(String id, EquipmentStats stats) {
        return EquipmentBonuses.statsOr(id, stats);
    }

    public static Worn worn(String id) {
        ensureLoaded();
        return WORN.get(id);
    }

    /** A hand-held teleport item (pharaoh's sceptre), or null. */
    public static Worn held(String id) {
        return HELD.get(id);
    }

    /** Every hand-held teleport item. */
    public static List<Worn> allHeld() {
        return List.copyOf(HELD.values());
    }

    public static List<Worn> all() {
        ensureLoaded();
        return Collections.unmodifiableList(new ArrayList<>(WORN.values()));
    }

    /** Item id after using one charge: {@code base_(n-1)}, the uncharged base, or null if it crumbles. */
    public static String afterCharge(Worn w) {
        if (w.chargeBase() == null || w.charges() <= 0) {
            return w.id();
        }
        if (w.charges() == 1) {
            return w.crumbles() ? null : w.chargeBase();
        }
        return w.chargeBase() + "_" + (w.charges() - 1);
    }

    /** The fully charged id for a charge group (glory and skills necklaces recharge to 4 at the fountain, like OSRS). */
    public static String recharged(String base) {
        return switch (base) {
            case "amulet_of_glory", "amulet_of_glory_t", "skills_necklace", "combat_bracelet" -> base + "_4";
            case "ring_of_wealth" -> base + "_5";
            default -> null;
        };
    }

    /** Ring of recoil: reflects 10% of damage taken (+1), 40 damage per ring. */
    public static final int RECOIL_CHARGES = 40;

    public static int recoil(int damage) {
        return damage <= 0 ? 0 : damage / 10 + 1;
    }

    /** Enchanting: unenchanted jewellery id -> enchanted id, keyed by enchant level 1-7. */
    public static final Map<String, String> ENCHANT = new LinkedHashMap<>();
    public static final Map<String, Integer> ENCHANT_LEVEL = new LinkedHashMap<>();

    static {
        enchant(1, "sapphire_ring", "ring_of_recoil");
        enchant(1, "sapphire_necklace", "games_necklace_8");
        enchant(1, "sapphire_amulet", "amulet_of_magic");
        enchant(1, "sapphire_bracelet", "bracelet_of_clay");
        enchant(2, "emerald_ring", "ring_of_dueling_8");
        enchant(2, "emerald_necklace", "binding_necklace");
        enchant(2, "emerald_amulet", "amulet_of_defence");
        enchant(2, "emerald_bracelet", "castle_wars_bracelet");
        enchant(2, "jade_ring", "ring_of_returning_" + RING_OF_RETURNING_CHARGES);
        enchant(3, "ruby_ring", "ring_of_forging");
        enchant(3, "ruby_necklace", "dodgy_necklace");
        enchant(3, "ruby_amulet", "amulet_of_strength");
        enchant(3, "ruby_bracelet", "inoculation_bracelet");
        enchant(4, "diamond_ring", "ring_of_life");
        enchant(4, "diamond_necklace", "phoenix_necklace");
        enchant(4, "diamond_amulet", "amulet_of_power");
        enchant(4, "diamond_bracelet", "abyssal_bracelet_5");
        enchant(5, "dragonstone_ring", "ring_of_wealth");
        enchant(5, "dragon_necklace", "skills_necklace");
        enchant(5, "dragonstone_necklace", "skills_necklace");
        enchant(5, "dragonstone_amulet", "amulet_of_glory");
        enchant(5, "dragonstone_bracelet", "combat_bracelet");
        enchant(6, "onyx_ring", "ring_of_stone");
        enchant(6, "onyx_necklace", "berserker_necklace");
        enchant(6, "onyx_amulet", "amulet_of_fury");
        enchant(6, "onyx_bracelet", "regen_bracelet");
        enchant(7, "zenyte_ring", "ring_of_suffering");
        enchant(7, "zenyte_necklace", "necklace_of_anguish");
        enchant(7, "zenyte_amulet", "amulet_of_torture");
        enchant(7, "zenyte_bracelet", "tormented_bracelet");
    }

    private static void enchant(int level, String from, String to) {
        ENCHANT.put(from, to);
        ENCHANT_LEVEL.put(from, level);
    }
}
