package com.runecraft.osrs.bank;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.data.ItemValues;
import com.runecraft.osrs.data.Untradeables;

/**
 * OSRS bank notes. A note is a stackable paper form of a tradeable, non-stackable item: it can be withdrawn
 * from the bank with "Withdraw as Note", sold to general stores and the Grand Exchange, and exchanged back at a
 * bank booth or banker, but not eaten, worn or used in skilling. In RuneCraft every note is the one
 * {@link #NOTE_ITEM} item whose data names the item it stands for.
 */
public final class BankNotes {
    /** The single note item; its stack data names the noted item. */
    public static final String NOTE_ITEM = "bank_note";
    /** OSRS examine text of every bank note. */
    public static final String EXAMINE = "Swap this note at any bank for the equivalent item.";

    private BankNotes() {
    }

    /** Whether OSRS lets this item be traded (Grand Exchange / players). Unknown RuneCraft items count as tradeable. */
    public static boolean tradeable(String id) {
        if (id == null || Untradeables.untradeable(id)) {
            return false;
        }
        ItemValues.Ref ref = ItemValues.get(id);
        return ref == null || ref.tradeable();
    }

    /** Whether OSRS stacks this item in the inventory (stackable items have no note form). */
    public static boolean osrsStackable(String id, ItemDef def) {
        ItemValues.Ref ref = ItemValues.get(id);
        if (ref != null) {
            return ref.stackable();
        }
        if (def == null) {
            return false;
        }
        return switch (def.kind()) {
            case RUNE, COINS, ARROW -> true;
            default -> false;
        };
    }

    /** The rule itself: only existing, tradeable, non-stackable items that aren't notes have a note form. */
    public static boolean rule(boolean exists, boolean isNote, boolean tradeable, boolean stackable) {
        return exists && !isNote && tradeable && !stackable;
    }

    /** Whether {@code id} can be withdrawn as a note. */
    public static boolean canNote(String id) {
        if (id == null) {
            return false;
        }
        ItemDef def = ItemCatalog.get(id);
        return rule(def != null, NOTE_ITEM.equals(id) || "coins".equals(id), tradeable(id), osrsStackable(id, def));
    }
}
