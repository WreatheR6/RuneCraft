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
    public enum Drain { COMBAT_TICK, ATTACK, HIT_TAKEN, SERPENTINE }

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
    /** Game ticks without attacking or being attacked after which a player counts as out of combat (approx). */
    public static final int COMBAT_TIMEOUT_TICKS = 8;

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
        return charges <= 0;
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
