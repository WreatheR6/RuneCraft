package com.runecraft.content.systems;

import com.runecraft.combat.MonsterStats;
import com.runecraft.osrs.CombatLevel;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.QuestInfo;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.systems.ClueScrolls;
import com.runecraft.osrs.content.systems.Diaries;
import com.runecraft.osrs.content.systems.Diaries.Area;
import com.runecraft.osrs.content.systems.Diaries.Task;
import com.runecraft.osrs.content.systems.Diaries.Tier;
import com.runecraft.osrs.content.systems.Diaries.Type;
import com.runecraft.osrs.content.systems.DiaryRewards;
import com.runecraft.osrs.content.systems.Teleports;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Locale;
import java.util.Map;

/** Tracks achievement diary tasks from game events and periodic checks, and hands out rewards. */
public final class DiaryService {
    private DiaryService() {
    }

    // ---------------- Daily limits ----------------

    static long today() {
        return Math.floorDiv(System.currentTimeMillis(), 86_400_000L);
    }

    private static void rollDay(SysState.SysPlayer sp) {
        long d = today();
        if (sp.day != d) {
            sp.day = d;
            sp.dailyUses.clear();
        }
    }

    public static int dailyLeft(ServerPlayerEntity p, String key, int max) {
        SysState.SysPlayer sp = SysState.of(p);
        rollDay(sp);
        return Math.max(0, max - sp.dailyUses.getOrDefault(key, 0));
    }

    public static boolean useDaily(ServerPlayerEntity p, String key, int max) {
        SysState.SysPlayer sp = SysState.of(p);
        rollDay(sp);
        int used = sp.dailyUses.getOrDefault(key, 0);
        if (used >= max) {
            return false;
        }
        sp.dailyUses.put(key, used + 1);
        return true;
    }

    // ---------------- Areas ----------------

    /** Whether the player is inside a task restriction (null = anywhere, an area id or an "x,y,r" circle). */
    static boolean inArea(ServerPlayerEntity p, String restrict) {
        if (restrict == null) {
            return true;
        }
        if (!Gielinor.is(p)) {
            return false;
        }
        int[] at = SysTeleport.osrs(p);
        return Diaries.restrictContains(restrict, Gielinor.region(p).name(), at[0], at[1]);
    }

    /** The task's skill requirements (boosts count, as in OSRS). */
    static boolean meets(ServerPlayerEntity p, Task task) {
        PlayerData d = Players.data(p);
        for (Map.Entry<Skill, Integer> e : task.reqs().entrySet()) {
            int lvl = Math.max(d.skills().level(e.getKey()), d.skills().currentLevel(e.getKey()));
            if (lvl < e.getValue()) {
                return false;
            }
        }
        return true;
    }

    // ---------------- Completion ----------------

    static void complete(ServerPlayerEntity p, Task task) {
        SysState.SysPlayer sp = SysState.of(p);
        if (sp.diaryDone.contains(task.id()) || !meets(p, task)) {
            return;
        }
        sp.diaryDone.add(task.id());
        Area a = Diaries.area(task.area());
        p.sendMessage(Text.literal("Well done! You have completed " + (task.tier() == Tier.EASY || task.tier() == Tier.ELITE ? "an " : "a ")
                + task.tier().id() + " task in the " + a.name() + " area. Your Achievement Diary has been updated.")
                .formatted(Formatting.DARK_GREEN), false);
        if (Diaries.tierComplete(sp.diaryDone, task.area(), task.tier())) {
            p.sendMessage(Text.literal("Congratulations! You have completed all of the " + task.tier().id() + " tasks in the " + a.name()
                    + " area. Claim your reward from the Achievement Diary menu (J) or the diary master.").formatted(Formatting.GOLD), false);
        }
    }

    private static void count(ServerPlayerEntity p, Task task, int amount) {
        SysState.SysPlayer sp = SysState.of(p);
        if (sp.diaryDone.contains(task.id()) || !meets(p, task)) {
            return;
        }
        int n = sp.diaryCounters.merge(task.id(), amount, Integer::sum);
        if (n >= task.count()) {
            sp.diaryCounters.remove(task.id());
            complete(p, task);
        }
    }

    // ---------------- Events ----------------

    public static void onXp(ServerPlayerEntity p, Skill skill, int tenths) {
        SysState.SysPlayer sp = SysState.of(p);
        PlayerData d = Players.data(p);
        for (Task t : Diaries.tasks()) {
            if (t.type() == Type.XP_IN && t.target().equals(skill.name()) && !sp.diaryDone.contains(t.id())
                    && d.skills().level(skill) >= t.level() && inArea(p, t.restrict())) {
                complete(p, t);
            }
        }
        DiaryPerks.onXp(p, skill);
    }

    public static void onKill(ServerPlayerEntity p, LivingEntity e, MonsterStats stats) {
        String name = (stats == null ? e.getName().getString() : stats.name()).toLowerCase(Locale.ROOT);
        for (Task t : Diaries.tasks()) {
            if (t.type() == Type.KILL && Diaries.killMatches(t.target(), name) && inArea(p, t.restrict())) {
                count(p, t, 1);
            }
        }
    }

    public static void onMake(ServerPlayerEntity p, Recipe r) {
        for (Task t : Diaries.tasks()) {
            if (t.type() != Type.MAKE) {
                continue;
            }
            for (Map.Entry<String, Integer> out : r.outputs().entrySet()) {
                if (Diaries.itemMatches(t.target(), out.getKey()) && inArea(p, t.restrict())) {
                    count(p, t, out.getValue());
                }
            }
        }
        DiaryPerks.onMake(p, r);
    }

    /** An item gathered, stolen, dropped or rewarded. */
    public static void onObtain(ServerPlayerEntity p, String item, int amount) {
        for (Task t : Diaries.tasks()) {
            if (t.type() == Type.OBTAIN && Diaries.itemMatches(t.target(), item) && inArea(p, t.restrict())) {
                count(p, t, Math.max(1, amount));
            }
        }
        DiaryPerks.onObtain(p, item);
    }

    public static void onCast(ServerPlayerEntity p, String spell) {
        SysState.of(p).spellsCast.add(spell);
        for (Task t : Diaries.tasks()) {
            if (t.type() == Type.CAST && Diaries.spellMatches(t.target(), spell)) {
                complete(p, t);
            }
        }
    }

    public static void onClue(ServerPlayerEntity p, ClueScrolls.Tier tier) {
        for (Task t : Diaries.tasks()) {
            if (t.type() == Type.CLUE && t.target().equals(tier.id())) {
                complete(p, t);
            }
        }
    }

    public static void onGeTrade(ServerPlayerEntity p) {
        check(p, Type.GE);
    }

    private static final Type[] PERIODIC = {Type.LEVEL, Type.HAVE, Type.QUEST, Type.VISIT, Type.KC, Type.GE, Type.CLUE, Type.CAST,
            Type.TOTAL_LEVEL, Type.COMBAT_LEVEL, Type.QUEST_POINTS, Type.SKILLCAPE};

    /** Periodic checks for state-based tasks. */
    public static void gameTick(ServerPlayerEntity p, long tick) {
        if ((tick + p.getId()) % 10 != 0) {
            return;
        }
        for (Type type : PERIODIC) {
            check(p, type);
        }
    }

    private static boolean at(ServerPlayerEntity p, String target) {
        if (!Gielinor.is(p)) {
            return false;
        }
        int[] v = Diaries.visitTarget(target);
        int[] pos = SysTeleport.osrs(p);
        return Teleports.within(pos[0], pos[1], v[0], v[1], v[2]);
    }

    static void check(ServerPlayerEntity p, Type type) {
        SysState.SysPlayer sp = SysState.of(p);
        PlayerData d = Players.data(p);
        for (Task t : Diaries.tasks()) {
            if (t.type() != type || sp.diaryDone.contains(t.id())) {
                continue;
            }
            boolean done = switch (type) {
                case LEVEL -> d.skills().level(Skill.valueOf(t.target())) >= t.level();
                case TOTAL_LEVEL -> d.skills().totalLevel() >= t.level();
                case COMBAT_LEVEL -> CombatLevel.of(d.skills()) >= t.level();
                case HAVE -> inArea(p, t.restrict()) && haveItem(p, t.target(), t.count());
                case QUEST -> {
                    QuestInfo q = ContentRegistry.quest(t.target());
                    // A quest that doesn't exist in this world can't block the diary.
                    yield q == null || d.questComplete(q);
                }
                case QUEST_POINTS -> {
                    if (!t.target().equals("all")) {
                        yield d.questPoints() >= t.level();
                    }
                    boolean all = true;
                    for (QuestInfo q : ContentRegistry.quests()) {
                        all &= d.questComplete(q);
                    }
                    yield all;
                }
                case VISIT -> at(p, t.target());
                case SKILLCAPE -> {
                    boolean cape = false;
                    for (Skill s : Skill.values()) {
                        cape |= d.skills().level(s) >= 99;
                    }
                    yield cape && at(p, t.target());
                }
                case KC -> Diaries.kcMet(t.target(), t.count(), d.killCounts());
                case GE -> sp.geTrades >= t.count();
                case CLUE -> {
                    try {
                        yield sp.cluesCompleted.getOrDefault(ClueScrolls.Tier.valueOf(t.target().toUpperCase(Locale.ROOT)), 0) > 0;
                    } catch (IllegalArgumentException e) {
                        yield false;
                    }
                }
                case CAST -> sp.spellsCast.stream().anyMatch(s -> Diaries.spellMatches(t.target(), s));
                default -> false;
            };
            if (done) {
                complete(p, t);
            }
        }
    }

    private static boolean haveItem(ServerPlayerEntity p, String target, int count) {
        if (!target.startsWith("*") && !target.contains("|")) {
            return Inv.count(p, target) >= count || SysState.of(p).wearing(target);
        }
        int total = 0;
        for (int i = 0; i < p.getInventory().size(); i++) {
            var s = p.getInventory().getStack(i);
            if (!s.isEmpty() && Diaries.itemMatches(target, Inv.idOf(s))) {
                total += s.getCount();
            }
        }
        return total >= count;
    }

    // ---------------- Rewards ----------------

    /** The reward teleport at a menu index of a diary reward item, or null. */
    static DiaryRewards.Tele rewardTele(String itemId, int index) {
        Area a = DiaryRewards.areaOf(itemId);
        if (a == null) {
            return null;
        }
        java.util.List<DiaryRewards.Tele> teles = DiaryRewards.teleports(a.id(), DiaryRewards.tierOf(itemId));
        return index >= 0 && index < teles.size() ? teles.get(index) : null;
    }

    /** Button label of a reward teleport, with the uses left today for limited ones. */
    static String rewardTeleLabel(ServerPlayerEntity p, String itemId, int index) {
        DiaryRewards.Tele t = rewardTele(itemId, index);
        if (t == null) {
            return "?";
        }
        int perDay = t.perDay(DiaryRewards.tierOf(itemId));
        return t.dest().name() + (perDay == 0 ? "" : " (" + dailyLeft(p, t.key(), perDay) + "/" + perDay + " today)");
    }

    /** Uses a diary reward item's teleport, honouring its daily charges. Returns true if the player teleported. */
    static boolean rewardTeleport(ServerPlayerEntity p, String itemId, int index) {
        DiaryRewards.Tele t = rewardTele(itemId, index);
        if (t == null) {
            return false;
        }
        int perDay = t.perDay(DiaryRewards.tierOf(itemId));
        if (perDay > 0 && dailyLeft(p, t.key(), perDay) <= 0) {
            Msg.game(p, "You have used all of today's teleports to " + t.dest().name() + ".");
            return false;
        }
        if (!SysTeleport.teleport(p, t.dest(), SysTeleport.JEWELLERY_WILDERNESS)) {
            return false;
        }
        if (perDay > 0) {
            useDaily(p, t.key(), perDay);
        }
        return true;
    }

    public static boolean claimable(SysState.SysPlayer sp, Area a, Tier t) {
        return Diaries.tierComplete(sp.diaryDone, a.id(), t) && !sp.diaryClaimed.contains(a.id() + ":" + t.name());
    }

    public static void claim(ServerPlayerEntity p, Area a, Tier t) {
        SysState.SysPlayer sp = SysState.of(p);
        if (!Diaries.tierComplete(sp.diaryDone, a.id(), t)) {
            Msg.game(p, "You haven't completed all of the " + t.id() + " tasks in the " + a.name() + " area yet.");
            return;
        }
        String key = a.id() + ":" + t.name();
        if (t.ordinal() > 0 && !sp.diaryClaimed.contains(a.id() + ":" + Tier.values()[t.ordinal() - 1].name())) {
            Msg.game(p, "You need to claim your " + Tier.values()[t.ordinal() - 1].id() + " " + a.name() + " reward first.");
            return;
        }
        String reward = a.reward(t);
        if (sp.diaryClaimed.contains(key)) {
            // Lost rewards can be reclaimed (if not carried or worn).
            if (Inv.has(p, reward, 1) || sp.wearing(reward)) {
                Msg.game(p, "You already have your " + ItemCatalog.get(reward).name() + ".");
                return;
            }
            Inv.give(p, reward, 1);
            Msg.action(p, "The diary master hands you a replacement " + ItemCatalog.get(reward).name() + ".");
            return;
        }
        sp.diaryClaimed.add(key);
        Inv.give(p, reward, 1);
        Inv.give(p, "antique_lamp_" + t.id(), 1);
        Msg.action(p, "You receive the " + ItemCatalog.get(reward).name() + " and an antique lamp.");
    }

    /** Antique lamp: XP in a chosen skill. */
    public static void rubLamp(ServerPlayerEntity p, Tier tier, Skill skill) {
        if (!Inv.has(p, "antique_lamp_" + tier.id(), 1)) {
            return;
        }
        if (Players.data(p).skills().level(skill) < tier.lampMinLevel) {
            Msg.game(p, "You need at least level " + tier.lampMinLevel + " " + skill.displayName() + " to use this lamp on it.");
            return;
        }
        Inv.remove(p, "antique_lamp_" + tier.id(), 1);
        com.runecraft.skill.XpService.add(p, skill, tier.lampXp);
        Msg.action(p, "The lamp grants you " + String.format(Locale.ROOT, "%,d", tier.lampXp) + " " + skill.displayName() + " experience.");
    }

    public static int done(SysState.SysPlayer sp, Area a, Tier t) {
        int n = 0;
        for (Task task : Diaries.tasks(a.id(), t)) {
            if (sp.diaryDone.contains(task.id())) {
                n++;
            }
        }
        return n;
    }
}
