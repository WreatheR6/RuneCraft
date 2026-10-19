package com.runecraft.osrs.content.systems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Degrading and charged equipment, kept as a per-stack charge count (NBT on the Minecraft side, see
 * {@code ChargeService}). One table, pure logic:
 *
 * <ul>
 *   <li><b>Barrows</b>: 15 hours of combat per piece (90,000 game ticks, one charge per tick spent in
 *   combat, even without being hit), shown as 100/75/50/25 and broken at 0 (no bonuses). Repaired by Bob
 *   in Lumbridge at a cost proportional to the wear (full repair: helm 60k, body 90k, legs 80k, weapon
 *   100k) or at a POH armour stand for 1% less per 2 Smithing levels.</li>
 *   <li><b>Crystal</b> (helm, body, legs, bow of faerdhinen, blade of saeldor, crystal bow): up to
 *   20,000 charges, one per attack (weapons) or per hit taken (armour); each crystal shard adds 100,
 *   recharged in Lletya or Prifddinas. Empty items are inactive (no bonuses, weapons won't attack).</li>
 *   <li><b>Rune/scale charged</b>: tridents (2,500 casts; seas: 1 death, 1 chaos, 5 fire, 10 coins per
 *   charge; swamp: a Zulrah's scale instead of the coins), sanguinesti staff (20,000; 3 blood runes),
 *   serpentine helms (11,000 scales; 10 scales on entering combat and 10 more every 90 ticks in
 *   combat), toxic blowpipe (16,383 scales, a scale on two shots out of three; it still fires the
 *   ammunition in the inventory like the other bows here, so darts are not stored).</li>
 * </ul>
 */
public final class Charges {
    private Charges() {
    }

    public enum Kind { BARROWS, CRYSTAL, CHARGED, SERPENTINE, BLOWPIPE }

    /** When charges drain. */
    public enum Drain { COMBAT_TICK, ATTACK, HIT_TAKEN, SERPENTINE, TELEPORT }

    /**
     * A charged item. {@code materials} is what one charge costs ({@code item -> amount}); for crystal it
     * is one shard per {@code chargesPerUnit} (100) charges.
     */
    public record Spec(String id, String name, Kind kind, int max, int initial, Drain drain, Map<String, Integer> materials,
                       int chargesPerUnit, int repairCost) {
        public boolean weapon() {
            return drain == Drain.ATTACK;
        }
    }

    public static final int BARROWS_MAX = 90_000;
    public static final int BARROWS_STAGE = BARROWS_MAX / 4;
    public static final int CRYSTAL_MAX = 20_000;
    public static final int CRYSTAL_PER_SHARD = 100;
    public static final int TRIDENT_MAX = 2_500;
    public static final int SANGUINESTI_MAX = 20_000;
    public static final int SERPENTINE_MAX = 11_000;
    public static final int BLOWPIPE_MAX = 16_383;
    public static final String ARCLIGHT = "arclight";
    public static final int ARCLIGHT_MAX = 10_000;
    public static final int ARCLIGHT_START = 1_000;
    public static final int ARCLIGHT_PER_3_SHARDS = 1_000;
    public static final String XERICS_TALISMAN = "xerics_talisman";
    /** Xeric's talisman (wiki): one charge per lizardman fang, at most 1,000; one charge per teleport. */
    public static final int XERICS_TALISMAN_MAX = 1_000;
    /** Charges of a talisman made from the inert one with 100 fangs (the zeah module's recipe). approx */
    public static final int XERICS_TALISMAN_START = 100;
    /** Game ticks without attacking or being attacked after which a player counts as out of combat (approx). */
    public static final String CAMULET = "camulet";
    public static final int CAMULET_MAX = 4;
    /** One bucket of ugthanki dung refills the camulet completely (wiki). */
    public static final String UGTHANKI_DUNG = "ugthanki_dung";
    public static final String SKULL_SCEPTRE = "skull_sceptre";
    /** Skull sceptre (wiki): 10 charges, 14/18/22/26 with each Varrock Diary tier; made with the cap's charges. */
    public static final int SKULL_SCEPTRE_MAX = 26;
    /** Teleport crystal (wiki): made with 4 charges, (5) the most; Eluned recharges it in Lletya or Prifddinas. */
    public static final String TELEPORT_CRYSTAL = "teleport_crystal";
    public static final int TELEPORT_CRYSTAL_MAX = 5;
    /** Coins per charge Eluned takes (OSRS: 150-750 coins for 3 charges, 5 with the hard Western Provinces Diary). approx */
    public static final int TELEPORT_CRYSTAL_COINS = 50;
    /** Chronicle (wiki): one teleport card per charge, at most 1,000. */
    public static final String CHRONICLE = "chronicle";
    public static final int CHRONICLE_MAX = 1_000;
    public static final int COMBAT_TIMEOUT_TICKS = 8;
    /** Ring of the elements (wiki): one air, water, earth, fire and law rune per charge, at most 10,000. */
    public static final String RING_OF_THE_ELEMENTS = "ring_of_the_elements";
    public static final int RING_OF_THE_ELEMENTS_MAX = 10_000;
    /** Giantsoul amulet (wiki): one big bones and one law rune per charge, at most 16,000. */
    public static final String GIANTSOUL_AMULET = "giantsoul_amulet";
    public static final int GIANTSOUL_AMULET_MAX = 16_000;
    /** Pendant of ates (wiki): one frozen tear per charge, at most 1,000; it drops inert (uncharged). */
    public static final String PENDANT_OF_ATES = "pendant_of_ates";
    public static final int PENDANT_OF_ATES_MAX = 1_000;
    /**
     * Enchanted lyre (wiki): charged only when empty, by offering the Fossegrimen a raw shark (2 charges), manta ray (3) or
     * sea turtle (4) - or a raw bass (2) while wearing a ring of charos (a) - plus one with the easy Fremennik Diary.
     */
    public static final String ENCHANTED_LYRE = "enchanted_lyre";
    public static final int ENCHANTED_LYRE_MAX = 5;
    /**
     * Kharedst's memoirs (wiki): one law, body, mind and soul rune per charge (10 Magic XP each in OSRS), 20 per page up to
     * 100; the book of the dead holds 250. OSRS recharges them at the Old Memorial north of Land's End; here as any charged
     * teleport item (sneak-use with the runes). approx
     */
    public static final String KHAREDSTS_MEMOIRS = "kharedsts_memoirs";
    public static final int KHAREDSTS_MEMOIRS_MAX = 100;
    public static final String BOOK_OF_THE_DEAD = "book_of_the_dead";
    public static final int BOOK_OF_THE_DEAD_MAX = 250;
    /**
     * Sailors' amulet (wiki): ten charges per law rune and ten water runes, at most 10,000; one per teleport. One item id
     * (the skilling module's salvage drop, "inert") holds the charges. approx: OSRS swaps it for the charged amulet
     */
    public static final String SAILORS_AMULET = "sailors_amulet_inert";
    public static final int SAILORS_AMULET_MAX = 10_000;
    public static final int SAILORS_AMULET_PER_UNIT = 10;

    /** Charges the Fossegrimen gives an empty enchanted lyre for an offering of this raw fish (0 if she won't take it). */
    public static int lyreCharges(String fish, boolean ringOfCharosA, boolean easyFremennikDiary) {
        int base = switch (fish == null ? "" : fish) {
            case "raw_shark" -> 2;
            case "raw_manta_ray" -> 3;
            case "raw_sea_turtle" -> 4;
            case "raw_bass" -> ringOfCharosA ? 2 : 0; // "explain that it is really a small shark"
            default -> 0;
        };
        return base == 0 ? 0 : base + (easyFremennikDiary ? 1 : 0);
    }

    /** How an empty teleport item with no charge materials is recharged (shown in its menu), or null. */
    public static String rechargeHint(String itemId) {
        return switch (itemId == null ? "" : itemId) {
            case ENCHANTED_LYRE -> "Offer the Fossegrimen, on the lake south-west of Rellekka, a raw shark, manta ray or sea turtle to charge it.";
            case PHARAOHS_SCEPTRE -> "The guardian mummy in Jalsavrah recharges it.";
            default -> null;
        };
    }
    /**
     * Pharaoh's sceptre (wiki): holds 3 charges, 10/25/50/100 with the easy/medium/hard/elite Desert Diary; recharged
     * by the guardian mummy in Jalsavrah for 6 gold, 12 stone or 24 pottery/ivory artefacts.
     */
    /** Revenant ether a revenant weapon holds (wiki). */
    public static final int REVENANT_ETHER_MAX = 16_000;
    public static final String PHARAOHS_SCEPTRE = "pharaohs_sceptre";
    /** Rod of Ivandis (wiki): 10 charges, one per Retainer special attack; it crumbles to dust when the last is used. */
    public static final String ROD_OF_IVANDIS = "rod_of_ivandis";
    public static final int ROD_OF_IVANDIS_MAX = 10;
    public static final int PHARAOHS_SCEPTRE_MAX = 100;
    private static final int[] PHARAOHS_SCEPTRE_CAPS = {3, 10, 25, 50, 100};

    /** The skull sceptre's charges for the number of Varrock Diary tiers completed (0-4): 10, 14, 18, 22, 26. */
    public static int skullSceptreCap(int varrockTiersDone) {
        return 10 + 4 * Math.max(0, Math.min(4, varrockTiersDone));
    }

    /**
     * Teleport items whose charges belong to the player rather than to one item (OSRS): the ring of the elements and the
     * camulet (using a charge on one camulet uses it on all of them).
     */
    public static boolean perPlayer(String itemId) {
        return RING_OF_THE_ELEMENTS.equals(itemId) || CAMULET.equals(itemId);
    }

    /**
     * A {@link #perPlayer} item's charges: the player's own count ({@code player}, null before they first had one) merged
     * with {@code legacyStack}, the charges a stack held from before they moved to the player (null when the stack has
     * none, or only mirrors the player's count). The larger of the two is kept; with neither, the item's starting charges.
     */
    public static int perPlayerCharges(Spec s, Integer player, Integer legacyStack) {
        int own = player != null ? player : legacyStack != null ? legacyStack : s.initial();
        return clamp(s, legacyStack != null ? Math.max(own, legacyStack) : own);
    }

    /** The sceptre's charge cap for the number of Desert Diary tiers completed (0-4). */
    public static int pharaohsSceptreCap(int desertTiersDone) {
        return PHARAOHS_SCEPTRE_CAPS[Math.max(0, Math.min(4, desertTiersDone))];
    }

    private static final Map<String, Spec> SPECS = new LinkedHashMap<>();

    private static void put(Spec s) {
        if (SPECS.put(s.id(), s) != null) {
            throw new IllegalStateException("duplicate charge spec " + s.id());
        }
    }

    private static void barrows(String id, String name, int repair) {
        put(new Spec(id, name, Kind.BARROWS, BARROWS_MAX, BARROWS_MAX, Drain.COMBAT_TICK, Map.of(), 1, repair));
    }

    private static void crystal(String id, String name, Drain drain) {
        // approx: items sung from seeds start fully charged here
        put(new Spec(id, name, Kind.CRYSTAL, CRYSTAL_MAX, CRYSTAL_MAX, drain, Map.of("crystal_shard", 1), CRYSTAL_PER_SHARD, 0));
    }

    static {
        String[][] brothers = {
                {"ahrims", "Ahrim's", "ahrims_hood", "ahrims_robetop", "ahrims_robeskirt", "ahrims_staff"},
                {"dharoks", "Dharok's", "dharoks_helm", "dharoks_platebody", "dharoks_platelegs", "dharoks_greataxe"},
                {"guthans", "Guthan's", "guthans_helm", "guthans_platebody", "guthans_chainskirt", "guthans_warspear"},
                {"karils", "Karil's", "karils_coif", "karils_leathertop", "karils_leatherskirt", "karils_crossbow"},
                {"torags", "Torag's", "torags_helm", "torags_platebody", "torags_platelegs", "torags_hammers"},
                {"veracs", "Verac's", "veracs_helm", "veracs_brassard", "veracs_plateskirt", "veracs_flail"}};
        int[] repair = {60_000, 90_000, 80_000, 100_000};
        for (String[] b : brothers) {
            for (int i = 0; i < 4; i++) {
                barrows(b[2 + i], b[1] + " " + b[2 + i].substring(b[0].length() + 1).replace('_', ' '), repair[i]);
            }
        }
        crystal("crystal_helm", "Crystal helm", Drain.HIT_TAKEN);
        crystal("crystal_body", "Crystal body", Drain.HIT_TAKEN);
        crystal("crystal_legs", "Crystal legs", Drain.HIT_TAKEN);
        crystal("bow_of_faerdhinen", "Bow of faerdhinen", Drain.ATTACK);
        crystal("blade_of_saeldor", "Blade of saeldor", Drain.ATTACK);
        crystal("crystal_bow_quest", "Crystal bow", Drain.ATTACK);

        // Kraken drops the trident fully charged ("(full)"); the swamp trident is made uncharged.
        put(new Spec("trident_of_the_seas", "Trident of the seas", Kind.CHARGED, TRIDENT_MAX, TRIDENT_MAX, Drain.ATTACK,
                ordered("death_rune", 1, "chaos_rune", 1, "fire_rune", 5, "coins", 10), 1, 0));
        put(new Spec("trident_of_the_swamp", "Trident of the swamp", Kind.CHARGED, TRIDENT_MAX, 0, Drain.ATTACK,
                ordered("death_rune", 1, "chaos_rune", 1, "fire_rune", 5, "zulrahs_scales", 1), 1, 0));
        put(new Spec("sanguinesti_staff", "Sanguinesti staff", Kind.CHARGED, SANGUINESTI_MAX, 0, Drain.ATTACK,
                ordered("blood_rune", 3), 1, 0));
        for (String[] h : new String[][]{{"serpentine_helm", "Serpentine helm"}, {"tanzanite_helm", "Tanzanite helm"},
                {"magma_helm", "Magma helm"}}) {
            put(new Spec(h[0], h[1], Kind.SERPENTINE, SERPENTINE_MAX, 0, Drain.SERPENTINE, ordered("zulrahs_scales", 1), 1, 0));
        }
        put(new Spec("toxic_blowpipe", "Toxic blowpipe", Kind.BLOWPIPE, BLOWPIPE_MAX, 0, Drain.ATTACK,
                ordered("zulrahs_scales", 1), 1, 0));
        // Arclight (wiki): made with 1,000 charges, one used per hit, 1,000 more per 3 ancient shards up to 10,000.
        // OSRS also takes single shards (333 charges); here shards go in threes. Empty, it can't attack (OSRS: it
        // becomes inactive, with Darklight's stats) - approx.
        put(new Spec(ARCLIGHT, "Arclight", Kind.CHARGED, ARCLIGHT_MAX, ARCLIGHT_START, Drain.ATTACK,
                ordered("ancient_shard", 3), ARCLIGHT_PER_3_SHARDS, 0));
        put(new Spec(XERICS_TALISMAN, "Xeric's talisman", Kind.CHARGED, XERICS_TALISMAN_MAX, XERICS_TALISMAN_START, Drain.TELEPORT,
                ordered("lizardman_fang", 1), 1, 0));
        // Camulet: 4 teleport charges per player, one bucket of ugthanki dung refills all of them (ChargeService). Skull
        // sceptre: made with 10-26 charges by Varrock Diary tier (skullSceptreCap), crumbles when empty.
        put(new Spec(CAMULET, "Camulet", Kind.CHARGED, CAMULET_MAX, CAMULET_MAX, Drain.TELEPORT, ordered(UGTHANKI_DUNG, 1), 1, 0));
        put(new Spec(SKULL_SCEPTRE, "Skull sceptre", Kind.CHARGED, SKULL_SCEPTRE_MAX, 10, Drain.TELEPORT, ordered(), 1, 0));
        // Drained by its special attack only (SpecialAttackService), so it uses the manual TELEPORT drain.
        put(new Spec(ROD_OF_IVANDIS, "Rod of Ivandis", Kind.CHARGED, ROD_OF_IVANDIS_MAX, ROD_OF_IVANDIS_MAX, Drain.TELEPORT, ordered(),
                1, 0));
        put(new Spec(TELEPORT_CRYSTAL, "Teleport crystal", Kind.CHARGED, TELEPORT_CRYSTAL_MAX, 4, Drain.TELEPORT,
                ordered("coins", TELEPORT_CRYSTAL_COINS), 1, 0));
        put(new Spec(CHRONICLE, "Chronicle", Kind.CHARGED, CHRONICLE_MAX, 0, Drain.TELEPORT, ordered("teleport_card", 1), 1, 0));
        // Round 17 teleport items. The ring of the elements' charges belong to the player (perPlayer).
        put(new Spec(RING_OF_THE_ELEMENTS, "Ring of the elements", Kind.CHARGED, RING_OF_THE_ELEMENTS_MAX, 0, Drain.TELEPORT,
                ordered("air_rune", 1, "water_rune", 1, "earth_rune", 1, "fire_rune", 1, "law_rune", 1), 1, 0));
        put(new Spec(GIANTSOUL_AMULET, "Giantsoul amulet", Kind.CHARGED, GIANTSOUL_AMULET_MAX, 0, Drain.TELEPORT,
                ordered("big_bones", 1, "law_rune", 1), 1, 0));
        put(new Spec(PENDANT_OF_ATES, "Pendant of ates", Kind.CHARGED, PENDANT_OF_ATES_MAX, 0, Drain.TELEPORT,
                ordered("frozen_tear", 1), 1, 0));
        // Round 32 transport: the enchanted lyre (charged by the Fossegrimen only), the sailors' amulet and the Kourend memoir
        // books (a book given with its five pages holds 20 per page: 100).
        put(new Spec(ENCHANTED_LYRE, "Enchanted lyre", Kind.CHARGED, ENCHANTED_LYRE_MAX, 0, Drain.TELEPORT, ordered(), 1, 0));
        put(new Spec(SAILORS_AMULET, "Sailors' amulet", Kind.CHARGED, SAILORS_AMULET_MAX, 0, Drain.TELEPORT,
                ordered("law_rune", 1, "water_rune", 10), SAILORS_AMULET_PER_UNIT, 0));
        put(new Spec(KHAREDSTS_MEMOIRS, "Kharedst's memoirs", Kind.CHARGED, KHAREDSTS_MEMOIRS_MAX, KHAREDSTS_MEMOIRS_MAX, Drain.TELEPORT,
                ordered("law_rune", 1, "body_rune", 1, "mind_rune", 1, "soul_rune", 1), 1, 0));
        put(new Spec(BOOK_OF_THE_DEAD, "Book of the dead", Kind.CHARGED, BOOK_OF_THE_DEAD_MAX, KHAREDSTS_MEMOIRS_MAX, Drain.TELEPORT,
                ordered("law_rune", 1, "body_rune", 1, "mind_rune", 1, "soul_rune", 1), 1, 0));
        // Revenant weapons and their wilderness-boss upgrades (wiki): up to 16,000 revenant ether, one per attack; they drop
        // uncharged ("(u)") and can't attack empty, which also ends their +50% Wilderness bonus (PlayerHitCalc.revWeapon).
        for (String[] w : new String[][]{{"craws_bow", "Craw's bow"}, {"viggoras_chainmace", "Viggora's chainmace"},
                {"thammarons_sceptre", "Thammaron's sceptre"}, {"webweaver_bow", "Webweaver bow"},
                {"ursine_chainmace", "Ursine chainmace"}, {"accursed_sceptre", "Accursed sceptre"}}) {
            put(new Spec(w[0], w[1], Kind.CHARGED, REVENANT_ETHER_MAX, 0, Drain.ATTACK, ordered("revenant_ether", 1), 1, 0));
        }
        // Bracelet of ethereum (wiki): up to 16,000 revenant ether; one per revenant attack while worn (JewelleryService).
        put(new Spec("bracelet_of_ethereum", "Bracelet of ethereum", Kind.CHARGED, REVENANT_ETHER_MAX, 0, Drain.HIT_TAKEN,
                ordered("revenant_ether", 1), 1, 0));
        // A sceptre found in the pyramid is uncharged (OSRS since 2022): the guardian mummy charges it.
        put(new Spec(PHARAOHS_SCEPTRE, "Pharaoh's sceptre", Kind.CHARGED, PHARAOHS_SCEPTRE_MAX, 0, Drain.TELEPORT,
                ordered(), 1, 0));
    }

    private static Map<String, Integer> ordered(Object... kv) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], (Integer) kv[i + 1]);
        }
        return Collections.unmodifiableMap(m);
    }

    public static Spec of(String itemId) {
        return itemId == null ? null : SPECS.get(itemId);
    }

    public static List<Spec> all() {
        return List.copyOf(SPECS.values());
    }

    // ------------------------------------------------------------------ state

    /** Clamp to [0, max]. */
    public static int clamp(Spec s, int charges) {
        return Math.max(0, Math.min(s.max(), charges));
    }

    /** Charges after draining {@code amount}. */
    public static int drain(Spec s, int charges, int amount) {
        return clamp(s, charges - Math.max(0, amount));
    }

    /** No bonuses / can't attack: broken Barrows, inactive crystal, uncharged weapons and helms. */
    public static boolean depleted(Spec s, int charges) {
        // Teleport items keep their bonuses when empty (an uncharged pharaoh's sceptre still fights).
        return s.drain() != Drain.TELEPORT && charges <= 0;
    }

    /** Barrows degradation stage shown in the name: 100, 75, 50, 25 or 0 (broken). */
    public static int barrowsStage(int charges) {
        if (charges <= 0) {
            return 0;
        }
        return (int) Math.ceil(charges / (double) BARROWS_STAGE) * 25;
    }

    /** The tooltip line for an item with this many charges. */
    public static String describe(Spec s, int charges) {
        return switch (s.kind()) {
            case BARROWS -> charges >= s.max() ? "Undamaged" : charges <= 0 ? "Broken - needs repair"
                    : "Degraded: " + barrowsStage(charges) + " (" + percent(charges, s.max()) + "% left)";
            case CRYSTAL -> charges <= 0 ? "Inactive - recharge with crystal shards" : "Charges: " + charges + "/" + s.max();
            case CHARGED -> charges <= 0 ? "Uncharged" : "Charges: " + charges + "/" + s.max();
            case SERPENTINE, BLOWPIPE -> charges <= 0 ? "Scales: 0 (uncharged)" : "Scales: " + charges + "/" + s.max();
        };
    }

    private static int percent(int charges, int max) {
        return (int) Math.ceil(charges * 100.0 / max);
    }

    // ------------------------------------------------------------------ combat drain

    /** Serpentine helm scales used on this game tick, given the ticks spent in the current fight (0 = just entered combat). */
    public static int serpentineDrain(long ticksInCombat) {
        if (ticksInCombat < 0) {
            return 0;
        }
        return ticksInCombat % 90 == 0 ? 10 : 0;
    }

    /** Blowpipe scale use: one scale on two shots out of three ({@code shot} counts from 0). */
    public static int blowpipeScales(long shot) {
        return shot % 3 == 2 ? 0 : 1;
    }

    // ------------------------------------------------------------------ recharging

    /** Camulet: one bucket of ugthanki dung refills it to full; a full camulet takes none (no dung wasted). */
    public static int camuletRefill(int charges, int dung) {
        return dung <= 0 || charges >= CAMULET_MAX ? 0 : CAMULET_MAX - Math.max(0, charges);
    }

    /** How many charges can be added from the materials available (capped by the free capacity). */
    public static int rechargeable(Spec s, int charges, Map<String, Integer> available) {
        int room = s.max() - clamp(s, charges);
        if (room <= 0 || s.materials().isEmpty()) {
            return 0;
        }
        long units = Long.MAX_VALUE;
        for (Map.Entry<String, Integer> e : s.materials().entrySet()) {
            units = Math.min(units, available.getOrDefault(e.getKey(), 0) / e.getValue());
        }
        long add = units * s.chargesPerUnit();
        if (s.chargesPerUnit() > 1) {
            // crystal: whole shards only, and never more shards than the free room needs (rounded down)
            add = Math.min(add, (long) (room / s.chargesPerUnit()) * s.chargesPerUnit());
        }
        return (int) Math.min(add, room);
    }

    /** Materials used to add {@code charges} charges. */
    public static Map<String, Integer> cost(Spec s, int charges) {
        Map<String, Integer> out = new LinkedHashMap<>();
        int units = s.chargesPerUnit() > 1 ? charges / s.chargesPerUnit() : charges;
        for (Map.Entry<String, Integer> e : s.materials().entrySet()) {
            out.put(e.getKey(), e.getValue() * units);
        }
        return out;
    }

    // ------------------------------------------------------------------ Barrows repairs

    /** Coins Bob (or another repair NPC) charges for this piece at this many charges. */
    public static int repairCost(Spec s, int charges) {
        if (s.kind() != Kind.BARROWS) {
            return 0;
        }
        int lost = s.max() - clamp(s, charges);
        return (int) Math.round((double) s.repairCost() * lost / s.max());
    }

    /** POH armour stand cost: 1% cheaper per 2 Smithing levels (99 Smithing: 49.5% off). */
    public static int armourStandCost(Spec s, int charges, int smithingLevel) {
        return (int) Math.round(repairCost(s, charges) * (1 - Math.max(0, Math.min(99, smithingLevel)) / 200.0));
    }

    /** All charge specs of one kind (for tests and tooltips). */
    public static List<Spec> ofKind(Kind kind) {
        List<Spec> out = new ArrayList<>();
        for (Spec s : SPECS.values()) {
            if (s.kind() == kind) {
                out.add(s);
            }
        }
        return out;
    }
}
