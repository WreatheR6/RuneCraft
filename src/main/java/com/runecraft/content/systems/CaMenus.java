package com.runecraft.content.systems;

import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.systems.CombatAchievements;
import com.runecraft.osrs.content.systems.CombatAchievements.Task;
import com.runecraft.osrs.content.systems.CombatAchievements.Tier;
import com.runecraft.osrs.content.systems.CombatAchievements.Track;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The Combat Achievements screens: overview with rewards, task lists per tier and per boss, and the antique lamps. */
public final class CaMenus {
    private static final int TASKS_PER_PAGE = 12;
    private static final int BOSSES_PER_PAGE = 24;

    private CaMenus() {
    }

    public static void registerHandlers() {
        SysNet.onAction("ca", (p, a, f) -> {
            String[] parts = a.split(":");
            try {
                switch (parts[0]) {
                    case "tier" -> tier(p, Tier.valueOf(parts[1]), 0);
                    case "claim" -> {
                        CombatAchievementService.claim(p, Tier.valueOf(parts[1]));
                        overview(p);
                    }
                    case "bosses" -> bosses(p, 0);
                    case "back" -> SysMenus.hub(p);
                    default -> {
                    }
                }
            } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException ignored) {
                // bad action
            }
        });
        SysNet.onAction("catier", (p, a, f) -> {
            String[] parts = a.split(":");
            try {
                if (parts[0].equals("page")) {
                    tier(p, Tier.valueOf(parts[1]), Integer.parseInt(parts[2]));
                } else {
                    overview(p);
                }
            } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException ignored) {
                overview(p);
            }
        });
        SysNet.onAction("caboss", (p, a, f) -> {
            try {
                if (a.startsWith("page:")) {
                    bosses(p, Integer.parseInt(a.substring(5)));
                } else if (a.startsWith("boss:")) {
                    boss(p, a.substring(5));
                } else {
                    overview(p);
                }
            } catch (NumberFormatException ignored) {
                overview(p);
            }
        });
        SysNet.onAction("cabosstasks", (p, a, f) -> bosses(p, 0));
        SysNet.onAction("calamp", (p, a, f) -> {
            String[] parts = a.split(":");
            if (parts.length == 2) {
                try {
                    CombatAchievementService.rubLamp(p, Tier.valueOf(parts[0]), Skill.valueOf(parts[1]));
                } catch (IllegalArgumentException ignored) {
                    // bad value
                }
            }
        });
        SysNet.onAction("cahilt", CombatAchievementService::hiltAction);
    }

    public static void overview(ServerPlayerEntity p) {
        SysState.SysPlayer sp = SysState.of(p);
        int points = CombatAchievements.points(sp.caDone);
        Tier unlocked = CombatAchievements.unlocked(points);
        SysNet.Screen s = new SysNet.Screen("ca", "Combat Achievements");
        s.line("Points: " + points + " / " + CombatAchievements.totalPoints() + "   Tasks: " + sp.caDone.size() + " / "
                + CombatAchievements.tasks().size() + "   Rewards unlocked: " + (unlocked == null ? "none" : unlocked.displayName()), 0xFFD700);
        Tier next = unlocked == null ? Tier.EASY : unlocked == Tier.GRANDMASTER ? null : Tier.values()[unlocked.ordinal() + 1];
        if (next != null) {
            s.line("Next tier: " + next.displayName() + " at " + CombatAchievements.threshold(next) + " points ("
                    + (CombatAchievements.threshold(next) - points) + " to go). Points from any tier count.", 0xC0C0C0);
        }
        s.line("Claim rewards from Ghommal outside the Warriors' Guild, or here.", 0xA0A0A0);
        for (Tier t : Tier.values()) {
            int done = 0;
            int total = 0;
            for (Task task : CombatAchievements.tasks(t)) {
                total++;
                if (sp.caDone.contains(task.id())) {
                    done++;
                }
            }
            s.button("tier:" + t.name(), t.displayName() + " " + done + "/" + total + " (" + t.points + " pt" + (t.points == 1 ? "" : "s")
                    + " each)", "Rewards at " + CombatAchievements.threshold(t) + " points: " + String.join("; ", CombatAchievements.rewards(t)), true);
        }
        for (Tier t : Tier.values()) {
            boolean ok = CombatAchievements.unlocked(points, t);
            if (ok) {
                s.button("claim:" + t.name(), (sp.caClaimed.contains(t.name()) ? "Reclaim " : "Claim ") + t.displayName() + " rewards",
                        String.join("; ", CombatAchievements.rewards(t)), true);
            }
        }
        s.button("bosses", "Tasks by boss", "", true);
        s.button("back", "Back", "", true);
        s.send(p);
    }

    private static String line(ServerPlayerEntity p, SysState.SysPlayer sp, Task t) {
        boolean done = sp.caDone.contains(t.id());
        boolean counted = t.track() == Track.KILLS || t.track() == Track.RAID || t.track() == Track.RUN || t.track() == Track.STREAK;
        String progress = counted && t.count() > 1 && !done ? " (" + CombatAchievementService.progress(p, t) + "/" + t.count() + ")" : "";
        return (done ? "[x] " : t.track() == Track.UNTRACKED ? "[-] " : "[ ] ") + t.name() + " - " + t.description() + progress;
    }

    private static int color(SysState.SysPlayer sp, Task t) {
        return sp.caDone.contains(t.id()) ? 0x60E060 : t.track() == Track.UNTRACKED ? 0x808080 : 0xE0E0E0;
    }

    public static void tier(ServerPlayerEntity p, Tier tier, int page) {
        SysState.SysPlayer sp = SysState.of(p);
        List<Task> tasks = CombatAchievements.tasks(tier);
        int pages = Math.max(1, (tasks.size() + TASKS_PER_PAGE - 1) / TASKS_PER_PAGE);
        page = Math.max(0, Math.min(page, pages - 1));
        SysNet.Screen s = new SysNet.Screen("catier", tier.displayName() + " Combat Achievements - page " + (page + 1) + "/" + pages);
        s.line("[x] done   [ ] tracked   [-] not tracked in this world yet", 0xA0A0A0);
        for (int i = page * TASKS_PER_PAGE; i < Math.min(tasks.size(), (page + 1) * TASKS_PER_PAGE); i++) {
            Task t = tasks.get(i);
            s.line(line(p, sp, t) + " [" + t.monster() + ", " + t.type().label + "]", color(sp, t));
        }
        if (page > 0) {
            s.button("page:" + tier.name() + ":" + (page - 1), "< Previous page", "", true);
        }
        if (page < pages - 1) {
            s.button("page:" + tier.name() + ":" + (page + 1), "Next page >", "", true);
        }
        s.button("back", "Back", "", true);
        s.send(p);
    }

    public static void bosses(ServerPlayerEntity p, int page) {
        SysState.SysPlayer sp = SysState.of(p);
        List<Map.Entry<String, List<Task>>> all = new ArrayList<>(CombatAchievements.byMonster().entrySet());
        int pages = Math.max(1, (all.size() + BOSSES_PER_PAGE - 1) / BOSSES_PER_PAGE);
        page = Math.max(0, Math.min(page, pages - 1));
        SysNet.Screen s = new SysNet.Screen("caboss", "Combat Achievements by boss - page " + (page + 1) + "/" + pages);
        for (int i = page * BOSSES_PER_PAGE; i < Math.min(all.size(), (page + 1) * BOSSES_PER_PAGE); i++) {
            Map.Entry<String, List<Task>> e = all.get(i);
            long done = e.getValue().stream().filter(t -> sp.caDone.contains(t.id())).count();
            s.button("boss:" + e.getKey(), e.getKey() + " " + done + "/" + e.getValue().size(), "", true);
        }
        if (page > 0) {
            s.button("page:" + (page - 1), "< Previous page", "", true);
        }
        if (page < pages - 1) {
            s.button("page:" + (page + 1), "Next page >", "", true);
        }
        s.button("back", "Back", "", true);
        s.send(p);
    }

    public static void boss(ServerPlayerEntity p, String monster) {
        SysState.SysPlayer sp = SysState.of(p);
        List<Task> tasks = CombatAchievements.byMonster().get(monster);
        if (tasks == null) {
            return;
        }
        SysNet.Screen s = new SysNet.Screen("cabosstasks", monster + " - Combat Achievements");
        for (Task t : tasks) {
            s.line(line(p, sp, t) + " [" + t.tier().displayName() + "]", color(sp, t));
            if (t.track() != Track.UNTRACKED && !sp.caDone.contains(t.id())) {
                s.line("    " + t.track().hint, 0x909090);
            }
        }
        s.button("back", "Back", "", true);
        s.send(p);
    }

    public static void lamp(ServerPlayerEntity p, Tier tier) {
        SysNet.Screen s = new SysNet.Screen("calamp", "Antique lamp (" + tier.id() + " combat achievements)");
        s.line("Choose a skill to gain " + String.format(Locale.ROOT, "%,d", tier.lampXp) + " XP (level " + tier.lampMinLevel + "+).");
        PlayerData d = Players.data(p);
        for (Skill sk : Skill.values()) {
            s.button(tier.name() + ":" + sk.name(), sk.displayName(), "", d.skills().level(sk) >= tier.lampMinLevel);
        }
        s.send(p);
    }
}
