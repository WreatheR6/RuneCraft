package com.runecraft.osrs.bank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/**
 * A player's bank, as in OSRS: up to {@link #CAPACITY} item slots, each holding any amount up to
 * 2,147,483,647, sorted into the main tab (0) and up to {@link #MAX_TABS} numbered tabs. Emptied slots can stay
 * as placeholders (count 0). Entries are kept in display order: tabs 1..9 first, then the main tab, which is how
 * the "View all" tab shows them. An entry is identified by its item id plus an opaque {@code tag} (the stack's
 * extra data, e.g. a potion's doses or an item's charges), so differently-charged items don't merge.
 */
public final class BankModel {
    /** OSRS member bank size. */
    public static final int CAPACITY = 800;
    public static final int MAX_TABS = 9;

    /** One bank slot. {@code count == 0} is a placeholder. */
    public static final class Entry {
        public final String item;
        public final String tag;
        public long count;
        public int tab;

        public Entry(String item, String tag, long count, int tab) {
            this.item = item;
            this.tag = tag == null ? "" : tag;
            this.count = count;
            this.tab = tab;
        }

        public boolean placeholder() {
            return count <= 0;
        }

        public String key() {
            return tag.isEmpty() ? item : item + "#" + tag;
        }

        @Override
        public String toString() {
            return key() + " x" + count + " (tab " + tab + ")";
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    /** "Always set placeholders" setting. */
    public boolean placeholders;
    /** "Withdraw as: Note" setting. */
    public boolean noted;
    /** Selected default quantity and the remembered X amount. */
    public BankQuantity.Mode quantity = BankQuantity.Mode.ONE;
    public long x = 1;

    public List<Entry> entries() {
        return Collections.unmodifiableList(entries);
    }

    public int slotsUsed() {
        return entries.size();
    }

    public Entry find(String item, String tag) {
        String t = tag == null ? "" : tag;
        for (Entry e : entries) {
            if (e.item.equals(item) && e.tag.equals(t)) {
                return e;
            }
        }
        return null;
    }

    public Entry byKey(String key) {
        for (Entry e : entries) {
            if (e.key().equals(key)) {
                return e;
            }
        }
        return null;
    }

    public long count(String item, String tag) {
        Entry e = find(item, tag);
        return e == null ? 0 : e.count;
    }

    /** Highest tab in use (0 when only the main tab has items). */
    public int tabCount() {
        int max = 0;
        for (Entry e : entries) {
            max = Math.max(max, e.tab);
        }
        return max;
    }

    private static int order(int tab) {
        return tab == 0 ? MAX_TABS + 1 : tab;
    }

    /** Index after the last entry that sorts at or before {@code tab}. */
    private int insertIndex(int tab) {
        int idx = 0;
        for (int i = 0; i < entries.size(); i++) {
            if (order(entries.get(i).tab) <= order(tab)) {
                idx = i + 1;
            }
        }
        return idx;
    }

    /** A valid destination tab: 0..9, and at most one past the last tab in use (making a new tab). */
    private int clampTab(int tab) {
        if (tab <= 0) {
            return 0;
        }
        return Math.min(Math.min(tab, tabCount() + 1), MAX_TABS);
    }

    /**
     * Deposits items. A matching slot (or placeholder) keeps its place; a new item goes at the end of {@code tab}.
     *
     * @return how many were deposited (less than {@code n} if the bank is full or the stack would pass the cap)
     */
    public long deposit(String item, String tag, long n, int tab) {
        if (n <= 0 || item == null || item.isEmpty()) {
            return 0;
        }
        Entry e = find(item, tag);
        if (e != null) {
            long add = Math.min(n, BankQuantity.MAX - e.count);
            e.count += add;
            return add;
        }
        if (entries.size() >= CAPACITY) {
            return 0;
        }
        int t = clampTab(tab);
        long add = Math.min(n, BankQuantity.MAX);
        entries.add(insertIndex(t), new Entry(item, tag, add, t));
        return add;
    }

    /** Whether {@code item} could be deposited (an existing slot with room, or a free slot). */
    public boolean hasRoomFor(String item, String tag) {
        Entry e = find(item, tag);
        return e != null ? e.count < BankQuantity.MAX : entries.size() < CAPACITY;
    }

    /**
     * Withdraws up to {@code n}. An emptied slot becomes a placeholder when placeholders are on, otherwise it
     * is removed (and an emptied tab closes).
     *
     * @return how many were taken
     */
    public long withdraw(String item, String tag, long n) {
        Entry e = find(item, tag);
        if (e == null || n <= 0 || e.count <= 0) {
            return 0;
        }
        long take = Math.min(n, e.count);
        e.count -= take;
        if (e.count == 0 && !placeholders) {
            entries.remove(e);
            normalizeTabs();
        }
        return take;
    }

    /** Moves an item to the end of another tab (dragging it onto the tab button); tab {@code tabCount()+1} opens a new tab. */
    public boolean moveToTab(String key, int tab) {
        Entry e = byKey(key);
        if (e == null) {
            return false;
        }
        int t = clampTab(tab);
        if (t == e.tab) {
            return false;
        }
        entries.remove(e);
        // A brand-new tab is numbered after the remaining tabs once the old tab may have emptied.
        e.tab = t;
        entries.add(insertIndex(t), e);
        normalizeTabs();
        return true;
    }

    /** OSRS "Swap" rearrange mode: two slots trade places (and tabs). */
    public boolean swap(String keyA, String keyB) {
        Entry a = byKey(keyA);
        Entry b = byKey(keyB);
        if (a == null || b == null || a == b) {
            return false;
        }
        int ia = entries.indexOf(a);
        int ib = entries.indexOf(b);
        int ta = a.tab;
        a.tab = b.tab;
        b.tab = ta;
        entries.set(ia, b);
        entries.set(ib, a);
        return true;
    }

    /** OSRS "Insert" rearrange mode: {@code keyA} moves in front of {@code keyB}, joining its tab. */
    public boolean insert(String keyA, String keyB) {
        Entry a = byKey(keyA);
        Entry b = byKey(keyB);
        if (a == null || b == null || a == b) {
            return false;
        }
        entries.remove(a);
        a.tab = b.tab;
        entries.add(entries.indexOf(b), a);
        normalizeTabs();
        return true;
    }

    /** Closes gaps in tab numbers (an emptied tab disappears and the later tabs shift down, as in OSRS). */
    public void normalizeTabs() {
        TreeSet<Integer> used = new TreeSet<>();
        for (Entry e : entries) {
            if (e.tab > 0) {
                used.add(e.tab);
            }
        }
        int[] remap = new int[MAX_TABS + 2];
        int next = 1;
        for (int t : used) {
            remap[Math.min(t, MAX_TABS + 1)] = next++;
        }
        for (Entry e : entries) {
            if (e.tab > 0) {
                e.tab = Math.min(MAX_TABS, remap[Math.min(e.tab, MAX_TABS + 1)]);
            }
        }
    }

    /** Removes every placeholder ("Release all placeholders"); returns how many were removed. */
    public int releasePlaceholders() {
        int before = entries.size();
        entries.removeIf(Entry::placeholder);
        normalizeTabs();
        return before - entries.size();
    }

    /**
     * The slots shown for a tab: tab 0 ("View all") shows everything in order, tab n only that tab. A non-empty
     * search shows every matching slot in the bank regardless of tab.
     */
    public List<Entry> view(int tab, String search, Function<String, String> nameOf) {
        List<Entry> out = new ArrayList<>();
        String q = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        for (Entry e : entries) {
            if (!q.isEmpty()) {
                String name = nameOf == null ? e.item : nameOf.apply(e.item);
                if (name != null && name.toLowerCase(Locale.ROOT).contains(q)) {
                    out.add(e);
                }
            } else if (tab <= 0 || e.tab == tab) {
                out.add(e);
            }
        }
        return out;
    }

    /** Total value (count x price each, saturating), e.g. at Grand Exchange guide prices. */
    public long value(ToLongFunction<String> priceEach) {
        return value(entries, priceEach);
    }

    public static long value(List<Entry> entries, ToLongFunction<String> priceEach) {
        long total = 0;
        for (Entry e : entries) {
            if (e.count <= 0) {
                continue;
            }
            long each = Math.max(0, priceEach.applyAsLong(e.item));
            long v = each != 0 && e.count > Long.MAX_VALUE / each ? Long.MAX_VALUE : e.count * each;
            total = Long.MAX_VALUE - total < v ? Long.MAX_VALUE : total + v;
        }
        return total;
    }

    /** Loads a saved slot as is (no merging), keeping the saved order. */
    public void load(String item, String tag, long count, int tab) {
        if (entries.size() < CAPACITY && find(item, tag) == null) {
            entries.add(new Entry(item, tag, Math.max(0, Math.min(count, BankQuantity.MAX)), Math.max(0, Math.min(tab, MAX_TABS))));
        }
    }

    /** Re-sorts loaded entries into display order and closes tab gaps. */
    public void finishLoad() {
        List<Entry> copy = new ArrayList<>(entries);
        entries.clear();
        copy.sort((a, b) -> Integer.compare(order(a.tab), order(b.tab)));
        entries.addAll(copy);
        normalizeTabs();
    }

    public void clear() {
        entries.clear();
    }
}
