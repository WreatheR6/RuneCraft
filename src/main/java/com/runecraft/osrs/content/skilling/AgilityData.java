package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.EquipmentDef.Slot;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.data.AgilityCourse;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Agility rewards: the Graceful outfit, sold by Grace in the Rogues' Den for Marks of grace (OSRS
 * prices, 260 marks for the set). Wearing all six pieces restores run energy 30% faster (an Agility
 * cape counts as the Graceful cape). A full set can be recoloured: the Varlamore set by Worm Tongue
 * at the Colossal Wyrm course (650 termites) and the Agility Arena set by Pirate Jackie the Fruit
 * (250 Brimhaven vouchers); recoloured pieces work exactly like the plain ones.
 *
 * <p>Also the course reward items ({@link AgilityRewards}) and the mith grapple for grapple
 * shortcuts: a mith grapple tip (59 Smithing, one mithril bar, 50 XP) fletched onto a mithril bolt
 * (59 Fletching, 11 XP) and tied to a rope.
 */
public final class AgilityData extends SkillingPart {
    public static final String GRACE = "rogues_den_grace";
    /** Suffixes of the Graceful recolours (item id = plain id + suffix). */
    public static final String VARLAMORE = "_varlamore";
    public static final String AGILITY_ARENA = "_agility_arena";
    public static final List<String> RECOLOURS = List.of(VARLAMORE, AGILITY_ARENA);
    /** Place by the Colossal Wyrm course where Worm Tongue keeps his stall. */
    public static final String WYRM_PLACE = "colossal_wyrm_remains";

    /** A Graceful piece and its price in Marks of grace. */
    public record Piece(String id, String name, String shape, int marks) {
    }

    public static final List<Piece> GRACEFUL = List.of(
            new Piece("graceful_hood", "Graceful hood", "cowl", 35),
            new Piece("graceful_cape", "Graceful cape", "cape", 40),
            new Piece("graceful_top", "Graceful top", "leather_body", 55),
            new Piece("graceful_legs", "Graceful legs", "chaps", 60),
            new Piece("graceful_gloves", "Graceful gloves", "gloves", 30),
            new Piece("graceful_boots", "Graceful boots", "boots", 40));

    private static final int COLOR = 0x2F7F5F;

    public static Piece piece(String id) {
        for (Piece p : GRACEFUL) {
            if (p.id().equals(id)) {
                return p;
            }
        }
        return null;
    }

    /** The plain Graceful id of a (possibly recoloured) piece; other ids are returned unchanged. */
    public static String plain(String id) {
        if (id != null && id.startsWith("graceful_")) {
            for (String suffix : RECOLOURS) {
                if (id.endsWith(suffix)) {
                    return id.substring(0, id.length() - suffix.length());
                }
            }
        }
        return id;
    }

    /** True when all six Graceful pieces (any colour) are worn (an Agility or max cape may replace the Graceful cape). */
    public static boolean fullGraceful(String head, String body, String legs, String feet, String cape, String hands) {
        String c = plain(cape);
        boolean capeOk = c.equals("graceful_cape") || c.startsWith("agility_cape") || c.equals("max_cape");
        return capeOk && plain(head).equals("graceful_hood") && plain(body).equals("graceful_top") && plain(legs).equals("graceful_legs")
                && plain(feet).equals("graceful_boots") && plain(hands).equals("graceful_gloves");
    }

    @Override
    public void items(ItemSink s) {
        addSet(s, "", "", COLOR, "Part of the Graceful outfit: light as a feather.");
        addSet(s, VARLAMORE, " (Varlamore)", 0x3A7AB0, "Part of the Graceful outfit, in the colours of Varlamore.");
        addSet(s, AGILITY_ARENA, " (Agility Arena)", 0xC8A030, "Part of the Graceful outfit, in Brimhaven's colours.");
        simple(s, AgilityRewards.TERMITES, "Termites", "creature", 0x8A6A3A, 0).stack(Integer.MAX_VALUE)
                .examine("Insects in a jar. Worm Tongue trades for them.");
        simple(s, AgilityRewards.WILDERNESS_TICKET, "Wilderness agility ticket", "note", 0xB03A2A, 0).stack(Integer.MAX_VALUE)
                .examine("Exchange it at the Agility dispenser for Agility experience.");
        simple(s, AgilityCourse.WEREWOLF_STICK, "Stick", "shaft", 0x8A6A3A, 0).stack(1)
                .examine("The Agility Trainer wants it back.");
        simple(s, "mith_grapple_tip", "Mith grapple tip", "arrowtips", 0x6A6AA0, 63).examine("A grapple tip for a mithril bolt.");
        simple(s, "mith_grapple_unf", "Mith grapple (unf)", "arrow", 0x6A6AA0, 72).stack(1).examine("It needs a rope.");
        tool(s, AgilityShortcuts.MITH_GRAPPLE, "Mith grapple", "arrow", 0x6A6AA0, 72)
                .examine("A mithril grapple on a rope; fire it from a crossbow at a grapple shortcut.");
    }

    private static void addSet(ItemSink s, String suffix, String nameSuffix, int color, String examine) {
        for (Piece p : GRACEFUL) {
            String id = p.id() + suffix;
            Slot slot = switch (p.id()) {
                case "graceful_hood" -> Slot.HEAD;
                case "graceful_top" -> Slot.CHEST;
                case "graceful_legs" -> Slot.LEGS;
                case "graceful_boots" -> Slot.FEET;
                default -> null;
            };
            if (slot == null) {
                // Cape and gloves are worn in the systems module's cape/hands slots.
                simple(s, id, p.name() + nameSuffix, p.shape(), color, 0).stack(1).examine(examine);
            } else {
                s.add(id, p.name() + nameSuffix, Kind.ARMOUR, p.shape(), color).value(0).stack(1)
                        .equipment(wear(slot, EquipmentStats.NONE)).examine(examine);
            }
        }
    }

    @Override
    public void recipes(Consumer<Recipe> out) {
        out.accept(r("smith_mith_grapple_tip", MenuType.SMITHING, "Mith grapple tip", Skill.SMITHING, 59, 50.0)
                .in("mithril_bar", 1).out("mith_grapple_tip", 1).tool("hammer").ticks(4).build());
        out.accept(r("fletch_mith_grapple_unf", MenuType.FLETCHING, "Mith grapple (unf)", Skill.FLETCHING, 59, 11.0)
                .in("mith_grapple_tip", 1).in("mithril_bolts", 1).out("mith_grapple_unf", 1).ticks(2).build());
        out.accept(plain("fletch_mith_grapple", MenuType.FLETCHING, "Mith grapple")
                .in("mith_grapple_unf", 1).in("rope", 1).out(AgilityShortcuts.MITH_GRAPPLE, 1).ticks(2).build());
    }

    @Override
    public List<BlockDef> blocks() {
        // One block for both ends of every shortcut (see AgilityShortcuts).
        return List.of(BlockDef.scripted(AgilityShortcuts.BLOCK, "Agility shortcut", 0x8A7A5A, "rope"));
    }

    @Override
    public List<PlaceDef> places() {
        return List.of(PlaceDef.of(WYRM_PLACE, "Colossal Wyrm Remains", 1655, 2941, PlaceDef.Style.NONE, List.of()));
    }

    @Override
    public List<NpcDef> npcs() {
        List<NpcDef> list = new ArrayList<>();
        list.add(NpcDef.of(GRACE, "Grace", "mg_rogues_den", -2, 2, 0xE8C0A0, COLOR, 0x2A5A4A, 0x3A2A1A));
        list.add(NpcDef.of(AgilityRewards.WORM_TONGUE, "Worm Tongue", WYRM_PLACE, 0, 0, 0xA07850, 0x6A5A3A, 0x4A3A2A, 0x8A6A3A));
        return list;
    }
}
