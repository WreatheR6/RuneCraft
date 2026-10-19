package com.runecraft.player;

import com.runecraft.osrs.CombatStyle;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.osrs.Spell;
import com.runecraft.osrs.data.AgilityCourse;
import com.runecraft.osrs.data.Monster;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/** Everything RuneCraft remembers about a player. Persisted in {@link PlayerDataState}. */
public final class PlayerData {
    /** Pre-2.0 banks were a 54-slot chest; old saves are migrated into {@link #bank()}. */
    public static final int BANK_SIZE = 54;

    private final SkillSet skills = new SkillSet();
    private int prayerPoints = 1;
    private final Set<Prayer> activePrayers = EnumSet.noneOf(Prayer.class);
    private int prayerDrainCounter;
    private CombatStyle style = CombatStyle.ACCURATE;
    private Spell autocast;
    private boolean autoRetaliate = true;
    private long coins;
    private Monster slayerTask;
    private int slayerRemaining;
    private int slayerStreak;
    private final int[] agilityProgress = new int[AgilityCourse.values().length];
    private final int[] agilityLaps = new int[AgilityCourse.values().length];
    private final com.runecraft.osrs.bank.BankModel bank = new com.runecraft.osrs.bank.BankModel();
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

    /** OSRS Auto Retaliate (Combat Options), on by default. */
    public boolean autoRetaliate() {
        return autoRetaliate;
    }

    public void setAutoRetaliate(boolean autoRetaliate) {
        this.autoRetaliate = autoRetaliate;
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

    /** Laps completed of a course (OSRS lap count). */
    public int agilityLaps(AgilityCourse course) {
        return agilityLaps[course.ordinal()];
    }

    public int addAgilityLap(AgilityCourse course) {
        return ++agilityLaps[course.ordinal()];
    }

    public com.runecraft.osrs.bank.BankModel bank() {
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

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
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
        nbt.putBoolean("NoAutoRetaliate", !autoRetaliate);
        nbt.putLong("Coins", coins);
        nbt.putString("SlayerTask", slayerTask == null ? "" : slayerTask.name());
        nbt.putInt("SlayerRemaining", slayerRemaining);
        nbt.putInt("SlayerStreak", slayerStreak);
        nbt.putIntArray("Agility", agilityProgress);
        nbt.putIntArray("AgilityLaps", agilityLaps);
        nbt.put("Bank2", com.runecraft.bank.BankService.write(bank));
        nbt.putBoolean("Visited", visitedGielinor);
        if (returnPos != null) {
            nbt.putLong("ReturnPos", returnPos.asLong());
        }
        nbt.putInt("QuestPoints", questPoints);
        CompoundTag kc = new CompoundTag();
        killCounts.forEach(kc::putInt);
        nbt.put("KillCounts", kc);
        CompoundTag quests = new CompoundTag();
        questStages.forEach(quests::putInt);
        nbt.put("Quests", quests);
        ListTag log = new ListTag();
        for (String item : collectionLog) {
            log.add(net.minecraft.nbt.StringTag.valueOf(item));
        }
        nbt.put("CollectionLog", log);
        return nbt;
    }

    public void fromNbt(CompoundTag nbt, HolderLookup.Provider registries) {
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
        autoRetaliate = !nbt.getBoolean("NoAutoRetaliate");
        coins = nbt.getLong("Coins");
        slayerTask = parse(Monster.class, nbt.getString("SlayerTask"), null);
        slayerRemaining = nbt.getInt("SlayerRemaining");
        slayerStreak = nbt.getInt("SlayerStreak");
        int[] agility = nbt.getIntArray("Agility");
        System.arraycopy(agility, 0, agilityProgress, 0, Math.min(agility.length, agilityProgress.length));
        int[] laps = nbt.getIntArray("AgilityLaps");
        System.arraycopy(laps, 0, agilityLaps, 0, Math.min(laps.length, agilityLaps.length));
        bank.clear();
        if (nbt.contains("Bank2")) {
            com.runecraft.bank.BankService.read(bank, nbt.getCompound("Bank2"), registries);
        } else {
            // Old 54-slot chest bank: move every stack into the new bank.
            ListTag bankList = nbt.getList("Bank", Tag.TAG_COMPOUND);
            for (int i = 0; i < bankList.size(); i++) {
                ItemStack stack = com.runecraft.util.LegacyStacks.parse(registries, bankList.getCompound(i));
                if (!stack.isEmpty()) {
                    com.runecraft.bank.BankService.depositStack(registries, bank, stack, stack.getCount(), 0);
                }
            }
        }
        visitedGielinor = nbt.getBoolean("Visited");
        returnPos = nbt.contains("ReturnPos") ? BlockPos.of(nbt.getLong("ReturnPos")) : null;
        questPoints = nbt.getInt("QuestPoints");
        killCounts.clear();
        CompoundTag kc = nbt.getCompound("KillCounts");
        for (String k : kc.getAllKeys()) {
            killCounts.put(k, kc.getInt(k));
        }
        questStages.clear();
        CompoundTag quests = nbt.getCompound("Quests");
        for (String k : quests.getAllKeys()) {
            questStages.put(k, quests.getInt(k));
        }
        collectionLog.clear();
        ListTag log = nbt.getList("CollectionLog", Tag.TAG_STRING);
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
