package com.runecraft.player;

import com.runecraft.osrs.CombatStyle;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.osrs.Spell;
import com.runecraft.osrs.data.AgilityCourse;
import com.runecraft.osrs.data.Monster;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;
import java.util.Set;

/** Everything RuneCraft remembers about a player. Persisted in {@link PlayerDataState}. */
public final class PlayerData {
    public static final int BANK_SIZE = 54;

    private final SkillSet skills = new SkillSet();
    private int prayerPoints = 1;
    private final Set<Prayer> activePrayers = EnumSet.noneOf(Prayer.class);
    private int prayerDrainCounter;
    private CombatStyle style = CombatStyle.ACCURATE;
    private Spell autocast;
    private long coins;
    private Monster slayerTask;
    private int slayerRemaining;
    private int slayerStreak;
    private final int[] agilityProgress = new int[AgilityCourse.values().length];
    private final SimpleInventory bank = new SimpleInventory(BANK_SIZE);
    private boolean visitedGielinor;
    private BlockPos returnPos;
    private int questPoints;
    private final java.util.Map<String, Integer> killCounts = new java.util.HashMap<>();
    private final java.util.Map<String, Integer> questStages = new java.util.HashMap<>();
    private final java.util.Set<String> collectionLog = new java.util.LinkedHashSet<>();

    // Transient (not saved)
    public long lastAttackTick = -1000;
    public long stunnedUntil;
    public MenuType openMenu;
    public BlockPos menuStation;
    public boolean menuRange;
    public String lastRegion = "";
    public double sailingDistance;
    public long lastAgilityWarning;

    public SkillSet skills() {
        return skills;
    }

    public int prayerPoints() {
        return prayerPoints;
    }

    public void setPrayerPoints(int points) {
        this.prayerPoints = Math.max(0, Math.min(points, skills.currentLevel(Skill.PRAYER)));
    }

    public Set<Prayer> activePrayers() {
        return activePrayers;
    }

    public boolean isActive(Prayer prayer) {
        return activePrayers.contains(prayer);
    }

    public int prayerDrainCounter() {
        return prayerDrainCounter;
    }

    public void setPrayerDrainCounter(int value) {
        this.prayerDrainCounter = value;
    }

    public CombatStyle style() {
        return style;
    }

    public void setStyle(CombatStyle style) {
        this.style = style;
    }

    public Spell autocast() {
        return autocast;
    }

    public void setAutocast(Spell autocast) {
        this.autocast = autocast;
    }

    public long coins() {
        return coins;
    }

    public void setCoins(long coins) {
        this.coins = Math.max(0, Math.min(coins, Integer.MAX_VALUE));
    }

    public Monster slayerTask() {
        return slayerTask;
    }

    public int slayerRemaining() {
        return slayerRemaining;
    }

    public void setSlayerTask(Monster task, int remaining) {
        this.slayerTask = task;
        this.slayerRemaining = remaining;
    }

    public int slayerStreak() {
        return slayerStreak;
    }

    public void setSlayerStreak(int streak) {
        this.slayerStreak = streak;
    }

    public int agilityProgress(AgilityCourse course) {
        return agilityProgress[course.ordinal()];
    }

    public void setAgilityProgress(AgilityCourse course, int step) {
        agilityProgress[course.ordinal()] = step;
    }

    public SimpleInventory bank() {
        return bank;
    }

    public boolean visitedGielinor() {
        return visitedGielinor;
    }

    public void setVisitedGielinor(boolean visited) {
        this.visitedGielinor = visited;
    }

    public BlockPos returnPos() {
        return returnPos;
    }

    public void setReturnPos(BlockPos pos) {
        this.returnPos = pos;
    }

    public int questPoints() {
        return questPoints;
    }

    public void setQuestPoints(int questPoints) {
        this.questPoints = questPoints;
    }

    public int killCount(String boss) {
        return killCounts.getOrDefault(boss, 0);
    }

    public int incrementKillCount(String boss) {
        return killCounts.merge(boss, 1, Integer::sum);
    }

    public java.util.Map<String, Integer> killCounts() {
        return killCounts;
    }

    public int questStage(com.runecraft.osrs.QuestInfo quest) {
        return questStages.getOrDefault(quest.questId(), 0);
    }

    public void setQuestStage(com.runecraft.osrs.QuestInfo quest, int stage) {
        questStages.put(quest.questId(), stage);
    }

    public boolean questComplete(com.runecraft.osrs.QuestInfo quest) {
        return questStage(quest) >= quest.finalStage();
    }

    public java.util.Set<String> collectionLog() {
        return collectionLog;
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putIntArray("Xp", skills.rawXp());
        nbt.putIntArray("Boosts", skills.rawBoosts());
        nbt.putInt("Prayer", prayerPoints);
        int mask = 0;
        for (Prayer p : activePrayers) {
            mask |= 1 << p.ordinal();
        }
        nbt.putInt("ActivePrayers", mask);
        nbt.putString("Style", style.name());
        nbt.putString("Autocast", autocast == null ? "" : autocast.name());
        nbt.putLong("Coins", coins);
        nbt.putString("SlayerTask", slayerTask == null ? "" : slayerTask.name());
        nbt.putInt("SlayerRemaining", slayerRemaining);
        nbt.putInt("SlayerStreak", slayerStreak);
        nbt.putIntArray("Agility", agilityProgress);
        NbtList bankList = new NbtList();
        for (int i = 0; i < bank.size(); i++) {
            ItemStack stack = bank.getStack(i);
            if (!stack.isEmpty()) {
                NbtCompound c = new NbtCompound();
                c.putByte("Slot", (byte) i);
                stack.writeNbt(c);
                bankList.add(c);
            }
        }
        nbt.put("Bank", bankList);
        nbt.putBoolean("Visited", visitedGielinor);
        if (returnPos != null) {
            nbt.putLong("ReturnPos", returnPos.asLong());
        }
        nbt.putInt("QuestPoints", questPoints);
        NbtCompound kc = new NbtCompound();
        killCounts.forEach(kc::putInt);
        nbt.put("KillCounts", kc);
        NbtCompound quests = new NbtCompound();
        questStages.forEach(quests::putInt);
        nbt.put("Quests", quests);
        NbtList log = new NbtList();
        for (String item : collectionLog) {
            log.add(net.minecraft.nbt.NbtString.of(item));
        }
        nbt.put("CollectionLog", log);
        return nbt;
    }

    public void fromNbt(NbtCompound nbt) {
        skills.load(nbt.getIntArray("Xp"), nbt.getIntArray("Boosts"));
        prayerPoints = nbt.getInt("Prayer");
        activePrayers.clear();
        int mask = nbt.getInt("ActivePrayers");
        for (Prayer p : Prayer.values()) {
            if ((mask & (1 << p.ordinal())) != 0) {
                activePrayers.add(p);
            }
        }
        style = parse(CombatStyle.class, nbt.getString("Style"), CombatStyle.ACCURATE);
        autocast = parse(Spell.class, nbt.getString("Autocast"), null);
        coins = nbt.getLong("Coins");
        slayerTask = parse(Monster.class, nbt.getString("SlayerTask"), null);
        slayerRemaining = nbt.getInt("SlayerRemaining");
        slayerStreak = nbt.getInt("SlayerStreak");
        int[] agility = nbt.getIntArray("Agility");
        System.arraycopy(agility, 0, agilityProgress, 0, Math.min(agility.length, agilityProgress.length));
        bank.clear();
        NbtList bankList = nbt.getList("Bank", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < bankList.size(); i++) {
            NbtCompound c = bankList.getCompound(i);
            int slot = c.getByte("Slot") & 0xFF;
            if (slot < bank.size()) {
                bank.setStack(slot, ItemStack.fromNbt(c));
            }
        }
        visitedGielinor = nbt.getBoolean("Visited");
        returnPos = nbt.contains("ReturnPos") ? BlockPos.fromLong(nbt.getLong("ReturnPos")) : null;
        questPoints = nbt.getInt("QuestPoints");
        killCounts.clear();
        NbtCompound kc = nbt.getCompound("KillCounts");
        for (String k : kc.getKeys()) {
            killCounts.put(k, kc.getInt(k));
        }
        questStages.clear();
        NbtCompound quests = nbt.getCompound("Quests");
        for (String k : quests.getKeys()) {
            questStages.put(k, quests.getInt(k));
        }
        collectionLog.clear();
        NbtList log = nbt.getList("CollectionLog", NbtElement.STRING_TYPE);
        for (int i = 0; i < log.size(); i++) {
            collectionLog.add(log.getString(i));
        }
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String name, E fallback) {
        if (name == null || name.isEmpty()) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
