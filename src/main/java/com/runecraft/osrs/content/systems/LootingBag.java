package com.runecraft.osrs.content.systems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The looting bag's Wilderness drop (OSRS Wiki, "Looting bag"): any monster in the Wilderness may drop it, except
 * aviansies, abyssal demons and black chinchompas, and only while the player owns none. Level 10 or lower: 1/15; level
 * 50 or higher: 1/3; in between 1/(150/level).
 *
 * <p>Storage (OSRS Wiki, "Looting bag"): the bag holds 28 stacks. Tradeable items can only be put in while in the
 * Wilderness (use them on the bag, or its Deposit option); stackable and noted items stack as in the inventory. While the
 * bag is open, tradeable items picked up in the Wilderness go straight into it. At a bank (or deposit box) everything in
 * it is banked; unbankable items go to the inventory. Contents belong to the player (one bag per player) and are all
 * dropped when the player dies in the Wilderness.
 */
public final class LootingBag {
    public static final String ID = "looting_bag";
    /** Stacks the bag holds. */
    public static final int CAPACITY = 28;

    private LootingBag() {
    }

    /** Chance (0..1) that a Wilderness monster of this combat level drops the bag. */
    public static double chance(int combatLevel) {
        if (combatLevel <= 10) {
            return 1.0 / 15;
        }
        if (combatLevel >= 50) {
            return 1.0 / 3;
        }
        return combatLevel / 150.0;
    }

    /** Whether an item may be put in the bag here: only tradeable items, only in the Wilderness. */
    public static boolean canStore(boolean tradeable, int wildernessLevel) {
        return tradeable && wildernessLevel > 0;
    }

    /** One stack in the bag: item id, NBT tag (as the bank keys it), noted flag and count. */
    public static final class Stack {
        public final String item;
        public final String tag;
        public final boolean noted;
        public final boolean stackable;
        private long count;

        public Stack(String item, String tag, boolean noted, boolean stackable, long count) {
            this.item = item;
            this.tag = tag == null ? "" : tag;
            this.noted = noted;
            this.stackable = stackable || noted;
            this.count = Math.max(0, count);
        }

        public long count() {
            return count;
        }

        boolean same(String item, String tag, boolean noted) {
            return this.item.equals(item) && this.tag.equals(tag == null ? "" : tag) && this.noted == noted;
        }
    }

    /** What the bag holds, in the order it was put in. */
    public static final class Contents {
        private final List<Stack> stacks = new ArrayList<>();

        public List<Stack> stacks() {
            return Collections.unmodifiableList(stacks);
        }

        public boolean isEmpty() {
            return stacks.isEmpty();
        }

        /** Slots in use: one per stack (an unstackable item is one stack of 1). */
        public int slotsUsed() {
            return stacks.size();
        }

        public int freeSlots() {
            return CAPACITY - stacks.size();
        }

        /** Total of an item in the bag (noted and unnoted). */
        public long count(String item) {
            long n = 0;
            for (Stack s : stacks) {
                if (s.item.equals(item)) {
                    n += s.count;
                }
            }
            return n;
        }

        /**
         * Puts up to {@code n} items in: a stackable (or noted) item joins its stack or takes one free slot; unstackable items
         * take one slot each. Returns how many went in.
         */
        public int store(String item, String tag, boolean noted, boolean stackable, int n) {
            if (n <= 0) {
                return 0;
            }
            if (stackable || noted) {
                for (Stack s : stacks) {
                    if (s.same(item, tag, noted)) {
                        long room = Integer.MAX_VALUE - s.count;
                        int put = (int) Math.min(n, room);
                        s.count += put;
                        return put;
                    }
                }
                if (stacks.size() >= CAPACITY) {
                    return 0;
                }
                stacks.add(new Stack(item, tag, noted, true, n));
                return n;
            }
            int put = Math.min(n, CAPACITY - stacks.size());
            for (int i = 0; i < put; i++) {
                stacks.add(new Stack(item, tag, false, false, 1));
            }
            return Math.max(0, put);
        }

        /** Restores a saved stack as it was (no merging). */
        public void restore(Stack s) {
            if (s.count > 0 && stacks.size() < CAPACITY) {
                stacks.add(s);
            }
        }

        /** Takes every stack out (to bank or drop), leaving the bag empty. */
        public List<Stack> takeAll() {
            List<Stack> all = new ArrayList<>(stacks);
            stacks.clear();
            return all;
        }
    }

    /** Monsters that never drop it. */
    public static boolean excluded(String creatureId) {
        return creatureId.contains("aviansie") || creatureId.contains("abyssal_demon") || creatureId.contains("black_chinchompa");
    }
}
