package com.runecraft.content.systems;

import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.systems.ClueScrolls;
import com.runecraft.osrs.content.systems.GrandExchange;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.osrs.content.systems.Spellbooks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentState;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** World-save storage for the systems module: per-player data, the Grand Exchange and the hiscores. */
public final class SysState extends PersistentState {
    public static final String KEY = "runecraft_systems";

    /** Everything the systems module remembers about a player. */
    public static final class SysPlayer {
        public int specEnergy = 100;
        public boolean specArmed;
        /** Run energy in hundredths of a percent (0-10000). */
        public int runEnergy = 10000;
        public final Map<Jewellery.Slot, ItemStack> worn = new EnumMap<>(Jewellery.Slot.class);
        public int recoilCharges = Jewellery.RECOIL_CHARGES;
        public Spellbooks.Book book = Spellbooks.Book.STANDARD;
        public String autocast = "";
        public final Map<ClueScrolls.Tier, long[]> clues = new EnumMap<>(ClueScrolls.Tier.class);
        public final Map<ClueScrolls.Tier, Integer> cluesCompleted = new EnumMap<>(ClueScrolls.Tier.class);
        public final Set<String> diaryDone = new LinkedHashSet<>();
        public final Map<String, Integer> diaryCounters = new HashMap<>();
        public final Set<String> diaryClaimed = new LinkedHashSet<>();
        /** Combat Achievements: completed task ids, progress of multi-kill tasks and claimed tiers (enum names). */
        public final Set<String> caDone = new LinkedHashSet<>();
        public final Map<String, Integer> caCounters = new HashMap<>();
        public final Set<String> caClaimed = new LinkedHashSet<>();
        public final Map<String, Integer> dailyUses = new HashMap<>();
        public long day = -1;
        public int geTrades;
        public final Set<String> spellsCast = new LinkedHashSet<>();
        public boolean warnedBook;
        /** The clue puzzle box / light box in progress: "TIER|S|board" or "TIER|L|box" ({@code CluePuzzles} encoding), or "". */
        public String cluePuzzle = "";

        // Transient combat state (game ticks)
        public boolean vengeance;
        public long vengeanceReadyAt;
        public long staminaUntil;
        public long powerOfDeathUntil;
        public long markOfDarknessUntil;
        public long chargeUntil;
        public long thrallUntil;
        public int thrallTier;
        public String thrallType = "";
        public long nextThrallAttack;
        public boolean deathCharge;
        public Spellbooks.Book swapBook;
        public long lastCombatTick;
        public int lastTargetId = -1;
        public long magicImbueUntil;
        public long shadowVeilUntil;
        public long shadowVeilReadyAt;
        public long wardUntil;
        public long vileVigourReadyAt;
        public long thrallReadyAt;
        /** Corruption spell cast and waiting for the next hit on a player: 0 none, 1 lesser, 2 greater. */
        public int corruptionPending;
        public long corruptionPendingUntil;
        /** Corruption on this player: pulses left (0 = not corrupted), strength and next pulse tick. */
        public int corruptionPulses;
        public boolean corruptionGreater;
        public boolean corruptionFast;
        public long nextCorruptionPulse;
        /** Prayer scrolls read (Rigour, Augury, Preserve, Deadeye, Mystic Vigour), by prayer name. */
        public final Set<String> prayerScrolls = new LinkedHashSet<>();

        public ItemStack worn(Jewellery.Slot slot) {
            return worn.getOrDefault(slot, ItemStack.EMPTY);
        }

        /** Item id worn in a slot, or "". */
        public String wornId(Jewellery.Slot slot) {
            ItemStack s = worn(slot);
            return s.isEmpty() ? "" : com.runecraft.util.Inv.idOf(s);
        }

        public boolean wearing(String id) {
            for (ItemStack s : worn.values()) {
                if (!s.isEmpty() && com.runecraft.util.Inv.idOf(s).equals(id)) {
                    return true;
                }
            }
            return false;
        }

        public boolean wearingPrefix(String prefix) {
            for (ItemStack s : worn.values()) {
                if (!s.isEmpty() && com.runecraft.util.Inv.idOf(s).startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }

        NbtCompound toNbt() {
            NbtCompound n = new NbtCompound();
            n.putInt("Spec", specEnergy);
            n.putInt("Run", runEnergy);
            NbtCompound worn = new NbtCompound();
            this.worn.forEach((slot, stack) -> {
                if (!stack.isEmpty()) {
                    worn.put(slot.name(), stack.writeNbt(new NbtCompound()));
                }
            });
            n.put("Worn", worn);
            n.putInt("Recoil", recoilCharges);
            n.putString("Book", book.name());
            n.putString("Autocast", autocast);
            NbtCompound clues = new NbtCompound();
            this.clues.forEach((t, v) -> clues.putLongArray(t.name(), v));
            n.put("Clues", clues);
            NbtCompound done = new NbtCompound();
            cluesCompleted.forEach((t, v) -> done.putInt(t.name(), v));
            n.put("CluesDone", done);
            n.put("Diary", strings(diaryDone));
            NbtCompound counters = new NbtCompound();
            diaryCounters.forEach(counters::putInt);
            n.put("DiaryCounters", counters);
            n.put("DiaryClaimed", strings(diaryClaimed));
            n.put("CaDone", strings(caDone));
            NbtCompound caCount = new NbtCompound();
            caCounters.forEach(caCount::putInt);
            n.put("CaCounters", caCount);
            n.put("CaClaimed", strings(caClaimed));
            NbtCompound daily = new NbtCompound();
            dailyUses.forEach(daily::putInt);
            n.put("Daily", daily);
            n.putLong("Day", day);
            n.putInt("GeTrades", geTrades);
            n.put("Spells", strings(spellsCast));
            n.put("PrayerScrolls", strings(prayerScrolls));
            n.putString("CluePuzzle", cluePuzzle);
            return n;
        }

        void fromNbt(NbtCompound n) {
            specEnergy = n.contains("Spec") ? n.getInt("Spec") : 100;
            runEnergy = n.contains("Run") ? n.getInt("Run") : 10000;
            worn.clear();
            NbtCompound w = n.getCompound("Worn");
            for (Jewellery.Slot slot : Jewellery.Slot.values()) {
                if (w.contains(slot.name())) {
                    ItemStack s = ItemStack.fromNbt(w.getCompound(slot.name()));
                    if (!s.isEmpty()) {
                        worn.put(slot, s);
                    }
                }
            }
            recoilCharges = n.contains("Recoil") ? n.getInt("Recoil") : Jewellery.RECOIL_CHARGES;
            try {
                book = Spellbooks.Book.valueOf(n.getString("Book"));
            } catch (IllegalArgumentException e) {
                book = Spellbooks.Book.STANDARD;
            }
            autocast = n.getString("Autocast");
            clues.clear();
            NbtCompound c = n.getCompound("Clues");
            for (ClueScrolls.Tier t : ClueScrolls.Tier.values()) {
                if (c.contains(t.name())) {
                    clues.put(t, c.getLongArray(t.name()));
                }
            }
            cluesCompleted.clear();
            NbtCompound cd = n.getCompound("CluesDone");
            for (ClueScrolls.Tier t : ClueScrolls.Tier.values()) {
                if (cd.contains(t.name())) {
                    cluesCompleted.put(t, cd.getInt(t.name()));
                }
            }
            readStrings(n.getList("Diary", NbtElement.STRING_TYPE), diaryDone);
            diaryCounters.clear();
            NbtCompound dc = n.getCompound("DiaryCounters");
            for (String k : dc.getKeys()) {
                diaryCounters.put(k, dc.getInt(k));
            }
            readStrings(n.getList("DiaryClaimed", NbtElement.STRING_TYPE), diaryClaimed);
            readStrings(n.getList("CaDone", NbtElement.STRING_TYPE), caDone);
            caCounters.clear();
            NbtCompound cc = n.getCompound("CaCounters");
            for (String k : cc.getKeys()) {
                caCounters.put(k, cc.getInt(k));
            }
            readStrings(n.getList("CaClaimed", NbtElement.STRING_TYPE), caClaimed);
            dailyUses.clear();
            NbtCompound d = n.getCompound("Daily");
            for (String k : d.getKeys()) {
                dailyUses.put(k, d.getInt(k));
            }
            day = n.contains("Day") ? n.getLong("Day") : -1;
            geTrades = n.getInt("GeTrades");
            readStrings(n.getList("Spells", NbtElement.STRING_TYPE), spellsCast);
            readStrings(n.getList("PrayerScrolls", NbtElement.STRING_TYPE), prayerScrolls);
            cluePuzzle = n.getString("CluePuzzle");
        }
    }

    /** Snapshot of a player's stats for the hiscores. */
    public record HiscoreEntry(String name, int[] xpTenths) {
        public long totalXp() {
            long t = 0;
            for (int x : xpTenths) {
                t += x / 10;
            }
            return t;
        }

        public int totalLevel() {
            int t = 0;
            for (int x : xpTenths) {
                t += com.runecraft.osrs.Experience.levelForTenths(x);
            }
            return t;
        }
    }

    private final Map<UUID, SysPlayer> players = new HashMap<>();
    private final GrandExchange ge = new GrandExchange();
    private final Map<UUID, HiscoreEntry> hiscores = new HashMap<>();
    /** Last traded price per item on the Grand Exchange. */
    private final Map<String, Integer> lastPrices = new HashMap<>();

    public static SysState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(SysState::fromNbt, SysState::new, KEY);
    }

    public static SysPlayer of(ServerPlayerEntity player) {
        return get(player.getServer()).player(player.getUuid());
    }

    public SysPlayer player(UUID uuid) {
        return players.computeIfAbsent(uuid, u -> new SysPlayer());
    }

    public GrandExchange ge() {
        return ge;
    }

    public Map<UUID, HiscoreEntry> hiscores() {
        return hiscores;
    }

    public Map<String, Integer> lastPrices() {
        return lastPrices;
    }

    @Override
    public boolean isDirty() {
        return true;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound ps = new NbtCompound();
        players.forEach((u, p) -> ps.put(u.toString(), p.toNbt()));
        nbt.put("Players", ps);
        NbtList offers = new NbtList();
        for (GrandExchange.Offer o : ge.offers()) {
            NbtCompound c = new NbtCompound();
            c.putLong("Id", o.id);
            c.putString("Owner", o.owner);
            c.putString("Item", o.item);
            c.putBoolean("Buy", o.buy);
            c.putInt("Qty", o.quantity);
            c.putInt("Price", o.price);
            c.putLong("Created", o.created);
            c.putInt("Filled", o.filled);
            c.putLong("Coins", o.coinsToCollect);
            c.putInt("Items", o.itemsToCollect);
            c.putBoolean("Aborted", o.aborted);
            offers.add(c);
        }
        nbt.put("GeOffers", offers);
        nbt.putLong("GeNextId", ge.nextId());
        // 4-hour buy limits: elapsed game ticks of each running window, plus the wall clock to count downtime.
        NbtList limits = new NbtList();
        for (GrandExchange.LimitWindow w : ge.saveBuyLimits(SysTicks.now())) {
            NbtCompound c = new NbtCompound();
            c.putString("Owner", w.owner());
            c.putString("Item", w.item());
            c.putLong("Elapsed", w.elapsedTicks());
            c.putLong("Bought", w.bought());
            limits.add(c);
        }
        nbt.put("GeBuyLimits", limits);
        nbt.putLong("GeSavedAt", System.currentTimeMillis());
        NbtCompound hs = new NbtCompound();
        hiscores.forEach((u, e) -> {
            NbtCompound c = new NbtCompound();
            c.putString("Name", e.name());
            c.putIntArray("Xp", e.xpTenths());
            hs.put(u.toString(), c);
        });
        nbt.put("Hiscores", hs);
        NbtCompound prices = new NbtCompound();
        lastPrices.forEach(prices::putInt);
        nbt.put("Prices", prices);
        return nbt;
    }

    public static SysState fromNbt(NbtCompound nbt) {
        SysState s = new SysState();
        NbtCompound ps = nbt.getCompound("Players");
        for (String k : ps.getKeys()) {
            try {
                SysPlayer p = new SysPlayer();
                p.fromNbt(ps.getCompound(k));
                s.players.put(UUID.fromString(k), p);
            } catch (IllegalArgumentException ignored) {
                // skip malformed entries
            }
        }
        NbtList offers = nbt.getList("GeOffers", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < offers.size(); i++) {
            NbtCompound c = offers.getCompound(i);
            GrandExchange.Offer o = new GrandExchange.Offer(c.getLong("Id"), c.getString("Owner"), c.getString("Item"),
                    c.getBoolean("Buy"), c.getInt("Qty"), c.getInt("Price"), c.getLong("Created"));
            o.filled = c.getInt("Filled");
            o.coinsToCollect = c.getLong("Coins");
            o.itemsToCollect = c.getInt("Items");
            o.aborted = c.getBoolean("Aborted");
            s.ge.offers().add(o);
        }
        s.ge.setNextId(Math.max(1, nbt.getLong("GeNextId")));
        java.util.List<GrandExchange.LimitWindow> windows = new java.util.ArrayList<>();
        NbtList limits = nbt.getList("GeBuyLimits", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < limits.size(); i++) {
            NbtCompound c = limits.getCompound(i);
            windows.add(new GrandExchange.LimitWindow(c.getString("Owner"), c.getString("Item"), c.getLong("Elapsed"), c.getLong("Bought")));
        }
        long savedAt = nbt.getLong("GeSavedAt");
        long offline = savedAt <= 0 ? 0 : Math.max(0, System.currentTimeMillis() - savedAt) / 600;
        s.ge.loadBuyLimits(windows, SysTicks.now(), offline);
        NbtCompound hs = nbt.getCompound("Hiscores");
        for (String k : hs.getKeys()) {
            try {
                NbtCompound c = hs.getCompound(k);
                int[] xp = c.getIntArray("Xp");
                if (xp.length == Skill.count()) {
                    s.hiscores.put(UUID.fromString(k), new HiscoreEntry(c.getString("Name"), xp));
                }
            } catch (IllegalArgumentException ignored) {
                // skip
            }
        }
        NbtCompound prices = nbt.getCompound("Prices");
        for (String k : prices.getKeys()) {
            s.lastPrices.put(k, prices.getInt(k));
        }
        return s;
    }

    private static NbtList strings(Set<String> set) {
        NbtList l = new NbtList();
        for (String s : set) {
            l.add(NbtString.of(s));
        }
        return l;
    }

    private static void readStrings(NbtList list, Set<String> into) {
        into.clear();
        for (int i = 0; i < list.size(); i++) {
            into.add(list.getString(i));
        }
    }
}
