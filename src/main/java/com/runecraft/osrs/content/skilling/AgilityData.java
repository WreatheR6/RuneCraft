package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.EquipmentDef.Slot;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.NpcDef;

import java.util.List;

/**
 * Agility rewards: the Graceful outfit, sold by Grace in the Rogues' Den for Marks of grace (OSRS
 * prices, 260 marks for the set). Wearing all six pieces restores run energy 30% faster (an Agility
 * cape counts as the Graceful cape).
 */
public final class AgilityData extends SkillingPart {
    public static final String GRACE = "rogues_den_grace";

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

    /** True when all six Graceful pieces are worn (an Agility or max cape may replace the Graceful cape). */
    public static boolean fullGraceful(String head, String body, String legs, String feet, String cape, String hands) {
        boolean capeOk = cape.equals("graceful_cape") || cape.startsWith("agility_cape") || cape.equals("max_cape");
        return capeOk && head.equals("graceful_hood") && body.equals("graceful_top") && legs.equals("graceful_legs")
                && feet.equals("graceful_boots") && hands.equals("graceful_gloves");
    }

    @Override
    public void items(ItemSink s) {
        for (Piece p : GRACEFUL) {
            Slot slot = switch (p.id()) {
                case "graceful_hood" -> Slot.HEAD;
                case "graceful_top" -> Slot.CHEST;
                case "graceful_legs" -> Slot.LEGS;
                case "graceful_boots" -> Slot.FEET;
                default -> null;
            };
            if (slot == null) {
                // Cape and gloves are worn in the systems module's cape/hands slots.
                simple(s, p.id(), p.name(), p.shape(), COLOR, 0).stack(1)
                        .examine("Part of the Graceful outfit: light as a feather.");
            } else {
                s.add(p.id(), p.name(), Kind.ARMOUR, p.shape(), COLOR).value(0).stack(1).equipment(wear(slot, EquipmentStats.NONE))
                        .examine("Part of the Graceful outfit: light as a feather.");
            }
        }
    }

    @Override
    public List<BlockDef> blocks() {
        // One block for both ends of every surface shortcut (see AgilityShortcuts).
        return List.of(BlockDef.scripted(AgilityShortcuts.BLOCK, "Agility shortcut", 0x8A7A5A, "rope"));
    }

    @Override
    public List<NpcDef> npcs() {
        return List.of(NpcDef.of(GRACE, "Grace", "mg_rogues_den", -2, 2, 0xE8C0A0, COLOR, 0x2A5A4A, 0x3A2A1A));
    }
}
