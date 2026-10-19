package com.runecraft.content.systems;

import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.systems.ClueScrolls;
import com.runecraft.osrs.content.systems.GrandExchange;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.osrs.content.systems.Spellbooks;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** World-save storage for the systems module: per-player data, the Grand Exchange and the hiscores. */
public final class SysState extends SavedData {
    public static final String KEY = "runecraft_systems";

    /** Everything the systems module remembers about a player. */
    public static final class SysPlayer {
        public int specEnergy = 100;
        public boolean specArmed;
        /** Run energy in hundredths of a percent (0-10000). */
        public int runEnergy = 10000;
        public final Map<Jewellery.Slot, ItemStack> worn = new EnumMap<>(Jewellery.Slot.class);
        public int recoilCharges = Jewellery.RECOIL_CHARGES;
        /** Per-player charge pools of the semi-precious jewellery (item id -> charges left; see Jewellery.chargePool). */
        public final Map<String, Integer> jewelleryCharges = new HashMap<>();
        /** Game ticks left on the PvP skull (SkullService), kept so a skull survives logging out and restarts. */
        public int skullTicks;
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
        /** Soulreaper axe soul stacks (0-5), consumed by its special attack. */
        public int soulStacks;
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
        /** Permanent item unlocks (e.g. Xeric's Honour after using an ancient tablet on Xeric's talisman). */
        public final Set<String> unlocks = new LinkedHashSet<>();
        /** Charges that belong to the player rather than an item (Charges.perPlayer: ring of the elements, camulet). */
        public final Map<String, Integer> playerCharges = new HashMap<>();
        /**
         * Dwarf multicannon (CannonService): set while the player's cannon is out in the world; if it is gone (decayed,
         * logged out, server restart) Nulodion gives it back with the cannonballs it held.
         */
        public boolean cannonOut;
        public int cannonSteel;
        public int cannonGranite;
        /** Looting bag contents (LootingBagService) and whether the bag is open (auto-collects Wilderness pickups). */
        public final com.runecraft.osrs.content.systems.LootingBag.Contents lootingBag = new com.runecraft.osrs.content.systems.LootingBag.Contents();
        public boolean lootingBagOpen;

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

        CompoundTag toNbt(HolderLookup.Provider registries) {
            CompoundTag n = new CompoundTag();
            n.putInt("Spec", specEnergy);
            n.putInt("Run", runEnergy);
            CompoundTag worn = new CompoundTag();
            this.worn.forEach((slot, stack) -> {
                if (!stack.isEmpty()) {
                    worn.put(slot.name(), stack.saveOptional(registries));
                }
            });
            n.put("Worn", worn);
            n.putInt("Recoil", recoilCharges);
            n.putInt("SkullTicks", skullTicks);
            CompoundTag jc = new CompoundTag();
            jewelleryCharges.forEach(jc::putInt);
            n.put("JewelleryCharges", jc);
            n.putString("Book", book.name());
            n.putString("Autocast", autocast);
            CompoundTag clues = new CompoundTag();
            this.clues.forEach((t, v) -> clues.putLongArray(t.name(), v));
            n.put("Clues", clues);
            CompoundTag done = new CompoundTag();
            cluesCompleted.forEach((t, v) -> done.putInt(t.name(), v));
            n.put("CluesDone", done);
            n.put("Diary", strings(diaryDone));
            CompoundTag counters = new CompoundTag();
            diaryCounters.forEach(counters::putInt);
            n.put("DiaryCounters", counters);
            n.put("DiaryClaimed", strings(diaryClaimed));
            n.put("CaDone", strings(caDone));
            CompoundTag caCount = new CompoundTag();
            caCounters.forEach(caCount::putInt);
            n.put("CaCounters", caCount);
            n.put("CaClaimed", strings(caClaimed));
            CompoundTag daily = new CompoundTag();
            dailyUses.forEach(daily::putInt);
            n.put("Daily", daily);
            CompoundTag owned = new CompoundTag();
            playerCharges.forEach(owned::putInt);
            n.put("PlayerCharges", owned);
            n.putLong("Day", day);
            n.putInt("GeTrades", geTrades);
            n.put("Spells", strings(spellsCast));
            n.put("PrayerScrolls", strings(prayerScrolls));
            n.put("Unlocks", strings(unlocks));
            n.putString("CluePuzzle", cluePuzzle);
            if (cannonOut) {
                CompoundTag cannon = new CompoundTag();
                cannon.putInt("Steel", cannonSteel);
                cannon.putInt("Granite", cannonGranite);
                n.put("Cannon", cannon);
            }
            ListTag bag = new ListTag();
            for (com.runecraft.osrs.content.systems.LootingBag.Stack st : lootingBag.stacks()) {
                CompoundTag c = new CompoundTag();
                c.putString("Item", st.item);
                c.putString("Tag", st.tag);
                c.putBoolean("Noted", st.noted);
                c.putBoolean("Stackable", st.stackable);
                c.putLong("Count", st.count());
                bag.add(c);
            }
            n.put("LootingBag", bag);
            n.putBoolean("LootingBagOpen", lootingBagOpen);
            return n;
        }

        void fromNbt(CompoundTag n, HolderLookup.Provider registries) {
            specEnergy = n.contains("Spec") ? n.getInt("Spec") : 100;
            runEnergy = n.contains("Run") ? n.getInt("Run") : 10000;
            worn.clear();
            CompoundTag w = n.getCompound("Worn");
            for (Jewellery.Slot slot : Jewellery.Slot.values()) {
                if (w.contains(slot.name())) {
                    ItemStack s = ItemStack.parseOptional(registries, w.getCompound(slot.name()));
                    if (!s.isEmpty()) {
                        worn.put(slot, s);
                    }
                }
            }
            recoilCharges = n.contains("Recoil") ? n.getInt("Recoil") : Jewellery.RECOIL_CHARGES;
            skullTicks = n.getInt("SkullTicks");
            jewelleryCharges.clear();
            CompoundTag jc = n.getCompound("JewelleryCharges");
            for (String k : jc.getAllKeys()) {
                jewelleryCharges.put(k, jc.getInt(k));
            }
            try {
                book = Spellbooks.Book.valueOf(n.getString("Book"));
            } catch (IllegalArgumentException e) {
                book = Spellbooks.Book.STANDARD;
            }
            autocast = n.getString("Autocast");
            clues.clear();
            CompoundTag c = n.getCompound("Clues");
            for (ClueScrolls.Tier t : ClueScrolls.Tier.values()) {
                if (c.contains(t.name())) {
                    clues.put(t, c.getLongArray(t.name()));
                }
            }
            cluesCompleted.clear();
            CompoundTag cd = n.getCompound("CluesDone");
            for (ClueScrolls.Tier t : ClueScrolls.Tier.values()) {
                if (cd.contains(t.name())) {
                    cluesCompleted.put(t, cd.getInt(t.name()));
                }
            }
            readStrings(n.getList("Diary", Tag.TAG_STRING), diaryDone);
            diaryCounters.clear();
            CompoundTag dc = n.getCompound("DiaryCounters");
            for (String k : dc.getAllKeys()) {
                diaryCounters.put(k, dc.getInt(k));
            }
            readStrings(n.getList("DiaryClaimed", Tag.TAG_STRING), diaryClaimed);
            readStrings(n.getList("CaDone", Tag.TAG_STRING), caDone);
            caCounters.clear();
            CompoundTag cc = n.getCompound("CaCounters");
            for (String k : cc.getAllKeys()) {
                caCounters.put(k, cc.getInt(k));
            }
            readStrings(n.getList("CaClaimed", Tag.TAG_STRING), caClaimed);
            dailyUses.clear();
            CompoundTag d = n.getCompound("Daily");
            for (String k : d.getAllKeys()) {
                dailyUses.put(k, d.getInt(k));
            }
            playerCharges.clear();
            CompoundTag pc = n.getCompound("PlayerCharges");
            for (String k : pc.getAllKeys()) {
                playerCharges.put(k, pc.getInt(k));
            }
            day = n.contains("Day") ? n.getLong("Day") : -1;
            geTrades = n.getInt("GeTrades");
            readStrings(n.getList("Spells", Tag.TAG_STRING), spellsCast);
            readStrings(n.getList("PrayerScrolls", Tag.TAG_STRING), prayerScrolls);
            readStrings(n.getList("Unlocks", Tag.TAG_STRING), unlocks);
            cluePuzzle = n.getString("CluePuzzle");
            cannonOut = n.contains("Cannon");
            cannonSteel = n.getCompound("Cannon").getInt("Steel");
            cannonGranite = n.getCompound("Cannon").getInt("Granite");
            lootingBag.takeAll();
            ListTag bag = n.getList("LootingBag", Tag.TAG_COMPOUND);
            for (int i = 0; i < bag.size(); i++) {
                CompoundTag entry = bag.getCompound(i);
                lootingBag.restore(new com.runecraft.osrs.content.systems.LootingBag.Stack(entry.getString("Item"), entry.getString("Tag"),
                        entry.getBoolean("Noted"), entry.getBoolean("Stackable"), entry.getLong("Count")));
            }
            lootingBagOpen = n.getBoolean("LootingBagOpen");
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
    private HolderLookup.Provider registries;

    public static SysState get(MinecraftServer server) {
        SysState s = server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(SysState::new, SysState::fromNbt, null), KEY);
        s.registries = server.registryAccess();
        return s;
    }

    public static SysPlayer of(ServerPlayer player) {
        return get(player.getServer()).player(player.getUUID());
    }

    public SysPlayer player(UUID uuid) {
        return players.computeIfAbsent(uuid, u -> new SysPlayer());
    }

    /** Self-test hook ({@code com.runecraft.selftest}): an opaque copy of one player's state, or null if there is none. */
    public Object selfTestSave(UUID uuid) {
        SysPlayer p = players.get(uuid);
        return p == null ? null : p.toNbt(registries);
    }

    /** Self-test hook: puts back a {@link #selfTestSave} copy; null drops the player's state (a fresh one is made on use). */
    public void selfTestRestore(UUID uuid, Object saved) {
        players.remove(uuid);
        if (saved instanceof CompoundTag n) {
            SysPlayer p = new SysPlayer();
            p.fromNbt(n.copy(), registries);
            players.put(uuid, p);
        }
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
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        CompoundTag ps = new CompoundTag();
        players.forEach((u, p) -> ps.put(u.toString(), p.toNbt(registries)));
        nbt.put("Players", ps);
        ListTag offers = new ListTag();
        for (GrandExchange.Offer o : ge.offers()) {
            CompoundTag c = new CompoundTag();
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
        ListTag limits = new ListTag();
        for (GrandExchange.LimitWindow w : ge.saveBuyLimits(SysTicks.now())) {
            CompoundTag c = new CompoundTag();
            c.putString("Owner", w.owner());
            c.putString("Item", w.item());
            c.putLong("Elapsed", w.elapsedTicks());
            c.putLong("Bought", w.bought());
            limits.add(c);
        }
        nbt.put("GeBuyLimits", limits);
        nbt.putLong("GeSavedAt", System.currentTimeMillis());
        CompoundTag hs = new CompoundTag();
        hiscores.forEach((u, e) -> {
            CompoundTag c = new CompoundTag();
            c.putString("Name", e.name());
            c.putIntArray("Xp", e.xpTenths());
            hs.put(u.toString(), c);
        });
        nbt.put("Hiscores", hs);
        CompoundTag prices = new CompoundTag();
        lastPrices.forEach(prices::putInt);
        nbt.put("Prices", prices);
        return nbt;
    }

    public static SysState fromNbt(CompoundTag nbt, HolderLookup.Provider registries) {
        SysState s = new SysState();
        CompoundTag ps = nbt.getCompound("Players");
        for (String k : ps.getAllKeys()) {
            try {
                SysPlayer p = new SysPlayer();
                p.fromNbt(ps.getCompound(k), registries);
                s.players.put(UUID.fromString(k), p);
            } catch (IllegalArgumentException ignored) {
                // skip malformed entries
            }
        }
        ListTag offers = nbt.getList("GeOffers", Tag.TAG_COMPOUND);
        for (int i = 0; i < offers.size(); i++) {
            CompoundTag c = offers.getCompound(i);
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
        ListTag limits = nbt.getList("GeBuyLimits", Tag.TAG_COMPOUND);
        for (int i = 0; i < limits.size(); i++) {
            CompoundTag c = limits.getCompound(i);
            windows.add(new GrandExchange.LimitWindow(c.getString("Owner"), c.getString("Item"), c.getLong("Elapsed"), c.getLong("Bought")));
        }
        long savedAt = nbt.getLong("GeSavedAt");
        long offline = savedAt <= 0 ? 0 : Math.max(0, System.currentTimeMillis() - savedAt) / 600;
        s.ge.loadBuyLimits(windows, SysTicks.now(), offline);
        CompoundTag hs = nbt.getCompound("Hiscores");
        for (String k : hs.getAllKeys()) {
            try {
                CompoundTag c = hs.getCompound(k);
                int[] xp = c.getIntArray("Xp");
                if (xp.length == Skill.count()) {
                    s.hiscores.put(UUID.fromString(k), new HiscoreEntry(c.getString("Name"), xp));
                }
            } catch (IllegalArgumentException ignored) {
                // skip
            }
        }
        CompoundTag prices = nbt.getCompound("Prices");
        for (String k : prices.getAllKeys()) {
            s.lastPrices.put(k, prices.getInt(k));
        }
        return s;
    }

    private static ListTag strings(Set<String> set) {
        ListTag l = new ListTag();
        for (String s : set) {
            l.add(StringTag.valueOf(s));
        }
        return l;
    }

    private static void readStrings(ListTag list, Set<String> into) {
        into.clear();
        for (int i = 0; i < list.size(); i++) {
            into.add(list.getString(i));
        }
    }
}
