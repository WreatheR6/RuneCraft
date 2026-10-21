package com.runecraft.content.slayer;

import com.runecraft.collection.CollectionLog;
import com.runecraft.combat.MonsterStats;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.Region;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.CombatProfile;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.slayer.SlayerBossTasks;
import com.runecraft.osrs.content.slayer.SlayerCategories;
import com.runecraft.osrs.content.slayer.SlayerChests;
import com.runecraft.osrs.content.slayer.SlayerGear;
import com.runecraft.osrs.content.slayer.SlayerMasters;
import com.runecraft.osrs.content.slayer.SlayerRewards;
import com.runecraft.osrs.content.slayer.SlayerTasks;
import com.runecraft.osrs.content.slayer.Superiors;
import com.runecraft.osrs.world.TaskRegions;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.registry.ModEntities;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Slayer tasks, points, streaks, keys and superiors (called through {@link com.runecraft.combat.Slayer}). */
public final class SlayerService {
    private SlayerService() {
    }

    public static SlayerState.Profile profile(ServerPlayer player) {
        SlayerState.Profile p = SlayerState.of(player.getServer()).get(player.getUUID());
        if (!p.migrated) {
            migrate(player, p);
        }
        return p;
    }

    /** Moves a task stored by the old core system (PlayerData) into the slayer module's state. */
    private static void migrate(ServerPlayer player, SlayerState.Profile p) {
        p.migrated = true;
        PlayerData data = Players.data(player);
        p.streak = Math.max(p.streak, data.slayerStreak());
        if (data.slayerTask() != null && data.slayerRemaining() > 0) {
            SlayerCategories.Category c = SlayerCategories.ofBuiltIn(data.slayerTask().name());
            if (c != null && !p.hasTask()) {
                p.task = c.id();
                p.remaining = data.slayerRemaining();
                p.amount = p.remaining;
                p.master = "turael";
            }
        }
        data.setSlayerTask(null, 0);
    }

    // ------------------------------------------------------------------ masters

    /** The generic slayer master station: the best master for the player's levels. */
    public static void talkToStation(ServerPlayer player) {
        PlayerData data = Players.data(player);
        SlayerMasters.Master m = SlayerMasters.bestFor(data.skills().combatLevel(), data.skills().level(Skill.SLAYER),
                x -> x.quest() == null || ContentRegistry.quest(x.quest()) == null || QuestApi.isComplete(player, x.quest()));
        talk(player, m);
    }

    public static void talk(ServerPlayer player, SlayerMasters.Master master) {
        PlayerData data = Players.data(player);
        SlayerState.Profile p = profile(player);
        int combat = data.skills().combatLevel();
        int slayer = data.skills().level(Skill.SLAYER);
        // OSRS: showing a master the Slayer cape (99 Slayer) waives their combat level requirement.
        String cape = com.runecraft.content.systems.SysState.of(player).wornId(com.runecraft.osrs.content.systems.Jewellery.Slot.CAPE);
        boolean capeShown = slayer >= 99 && (cape.startsWith("slayer_cape") || Inv.has(player, "slayer_cape", 1)
                || Inv.has(player, "slayer_cape_t", 1));
        if (capeShown) {
            combat = Math.max(combat, master.combatLevel());
        }
        if (combat < master.combatLevel() || slayer < master.slayerLevel()) {
            say(player, master, "You're too weak to be one of my students. Come back with a combat level of "
                    + master.combatLevel() + (master.slayerLevel() > 1 ? " and a Slayer level of " + master.slayerLevel() : "") + ".");
            return;
        }
        if (master.quest() != null && ContentRegistry.quest(master.quest()) != null
                && !QuestApi.isComplete(player, master.quest())) {
            say(player, master, "I don't take students who haven't completed "
                    + ContentRegistry.quest(master.quest()).displayName() + ".");
            return;
        }
        if (p.hasTask()) {
            // Turael (not Spria) swaps another master's task for one of his own, resetting the streak; he
            // refuses when he could have assigned that task himself (OSRS).
            SlayerMasters.Master current = p.master == null ? null : SlayerMasters.get(p.master);
            boolean turaelSkip = master.changesTasks() && current != null && !current.changesTasks()
                    && !current.id().equals("spria") && master.assignment(p.task) == null;
            if (turaelSkip) {
                say(player, master, "That's a bit much for you, isn't it? I'll give you something easier, but your task streak is reset.");
                if (current.wilderness()) {
                    p.wildyStreak = 0;
                } else {
                    p.streak = 0;
                }
                p.task = null;
                p.remaining = 0;
                assign(player, master, p, combat, slayer);
                return;
            }
            if (p.chooseAmount) {
                // A boss task whose amount hasn't been chosen yet: the master asks again (OSRS).
                askBossAmount(player, master, p);
                menu(player, p);
                return;
            }
            say(player, master, "You're still hunting " + describe(p) + "; you have " + p.remaining + " to go.");
            menu(player, p);
            return;
        }
        assign(player, master, p, combat, slayer);
    }

    /** "How many would you like to slay?": clickable boss-task amounts from 3 to the player's maximum. */
    private static void askBossAmount(ServerPlayer player, SlayerMasters.Master master, SlayerState.Profile p) {
        SlayerBossTasks.BossTask boss = SlayerBossTasks.get(p.task);
        if (boss == null) {
            p.chooseAmount = false;
            return;
        }
        int max = bossMax(player, boss);
        say(player, master, "How many would you like to slay? (" + boss.min() + "-" + max + "; until you choose, it's " + p.amount + ".)");
        MutableComponent line = Component.literal("Amount: ").withStyle(ChatFormatting.GRAY);
        java.util.Set<Integer> choices = new java.util.TreeSet<>(List.of(boss.min(), 10, 15, 20, 25, 30, max));
        for (int n : choices) {
            if (n <= max) {
                line.append(button("[" + n + "]", "/slayer amount " + n, "Slay " + n + " " + boss.name()));
                line.append(Component.literal(" "));
            }
        }
        player.displayClientMessage(line, false);
    }

    /** The most kills the player may choose for a boss task: 35 (Barrows 36) plus 5 (6) per Combat Achievement tier. */
    static int bossMax(ServerPlayer player, SlayerBossTasks.BossTask boss) {
        com.runecraft.osrs.content.systems.CombatAchievements.Tier tier =
                com.runecraft.content.systems.CombatAchievementService.tier(player);
        return boss.maxKills(tier == null ? 0 : tier.ordinal() + 1);
    }

    /**
     * Sets the size of a just-assigned boss task ({@code /slayer amount}): allowed until the first kill counts, clamped to
     * 3 and the player's maximum. Returns false (and says why) when there is nothing to choose.
     */
    public static boolean chooseBossAmount(ServerPlayer player, int wanted) {
        SlayerState.Profile p = profile(player);
        SlayerBossTasks.BossTask boss = SlayerBossTasks.get(p.task);
        if (!p.hasTask() || boss == null || !p.chooseAmount) {
            Msg.game(player, "You don't have a boss task to choose the size of.");
            return false;
        }
        int n = Math.max(boss.min(), Math.min(bossMax(player, boss), wanted));
        p.amount = n;
        p.remaining = n;
        p.chooseAmount = false;
        Msg.game(player, "Your task is to kill " + n + " " + boss.name() + ".");
        SlayerNet.sync(player);
        return true;
    }

    private static void assign(ServerPlayer player, SlayerMasters.Master master, SlayerState.Profile p, int combat,
                               int slayer) {
        Random random = new Random(player.getRandom().nextLong());
        String previous = p.task;
        // Every master keeps their own block list (Turael and Spria share one).
        List<String> blocked = SlayerRewards.blockedFor(p.blocked, master.id());
        java.util.function.Predicate<String> quests = q -> questMet(player, q);
        SlayerMasters.Assignment a = master.pick(random,
                x -> SlayerTasks.eligible(x, slayer, combat, p.unlocks, blocked, previous, quests), p.unlocks);
        if (a == null) {
            a = master.pick(random, x -> SlayerTasks.eligible(x, slayer, combat, p.unlocks, blocked, null, quests), p.unlocks);
        }
        if (a == null) {
            say(player, master, "I have nothing for you right now.");
            return;
        }
        SlayerRewards.Unlock ext = SlayerRewards.extensionFor(a.category());
        boolean extended = ext != null && p.unlocks.contains(ext.id());
        SlayerBossTasks.BossTask boss = null;
        if (a.category().equals(SlayerBossTasks.CATEGORY)) {
            // "Like a Boss": one boss the player can reach, with equal chance (Krystilia: Wilderness bosses only; the
            // Alchemical Hydra: Konar only).
            boss = SlayerBossTasks.pick(random, slayer, master.wilderness(), blocked, previous, master.id(),
                    t -> bossReachable(player, t));
            if (boss == null) {
                // No boss the player can reach (quests, skills): one of the master's other tasks instead.
                a = master.pick(random, x -> !x.category().equals(SlayerBossTasks.CATEGORY)
                        && SlayerTasks.eligible(x, slayer, combat, p.unlocks, blocked, previous, quests), p.unlocks);
                if (a == null) {
                    say(player, master, "I have nothing for you right now.");
                    return;
                }
                ext = SlayerRewards.extensionFor(a.category());
                extended = ext != null && p.unlocks.contains(ext.id());
            }
        }
        p.task = boss != null ? boss.id() : a.category();
        // A boss task's size is the player's choice (3-35, +5 per Combat Achievement tier; Barrows 3-36, +6): it starts
        // at the maximum until they pick one.
        p.amount = boss != null ? bossMax(player, boss) : SlayerTasks.amount(a, extended, random);
        p.chooseAmount = boss != null;
        p.remaining = p.amount;
        p.master = master.id();
        p.region = null;
        if (master.locationRestricted() && boss == null) {
            List<Region> regions = SlayerTasks.regions(a.category());
            if (!regions.isEmpty()) {
                p.region = regions.get(random.nextInt(regions.size())).name();
            }
        }
        if (boss != null) {
            say(player, master, "Excellent. You're now assigned to kill " + boss.name() + "."
                    + (master.wilderness() ? " They only count in the Wilderness." : ""));
            askBossAmount(player, master, p);
            SlayerNet.sync(player);
            return;
        }
        say(player, master, "Your new task is to kill " + p.amount + " " + describe(p) + "."
                + (master.wilderness() ? " They only count in the Wilderness." : ""));
        // OSRS: a TzHaar assignment comes with the option to kill TzTok-Jad (or TzKal-Zuk) instead.
        com.runecraft.content.minigames.games.TzHaarSlayer.offer(player);
        SlayerNet.sync(player);
    }

    /** A boss's own requirements for a boss task: its quests and other skill (God Wars generals: 70). */
    private static boolean bossReachable(ServerPlayer player, SlayerBossTasks.BossTask t) {
        for (String q : t.quests()) {
            if (!questMet(player, q)) {
                return false;
            }
        }
        return t.skill() == null || Players.data(player).skills().level(t.skill()) >= t.skillLevel();
    }

    /**
     * A task's quest requirement: {@code id} must be complete, {@code ~id} only started. Quests RuneCraft
     * doesn't have never block a task.
     */
    private static boolean questMet(ServerPlayer player, String requirement) {
        boolean started = requirement.startsWith("~");
        String id = started ? requirement.substring(1) : requirement;
        if (ContentRegistry.quest(id) == null) {
            return true;
        }
        return started ? QuestApi.stage(player, id) > 0 || QuestApi.isComplete(player, id) : QuestApi.isComplete(player, id);
    }

    /** Whether the elite tier of the master's points diary (Konar: Kourend &amp; Kebos; Nieve/Steve: Western) is done. */
    private static boolean eliteDiary(ServerPlayer player, SlayerMasters.Master master) {
        return master.diary() != null && com.runecraft.osrs.content.systems.Diaries.tierComplete(
                com.runecraft.content.systems.SysState.of(player).diaryDone, master.diary(),
                com.runecraft.osrs.content.systems.Diaries.Tier.ELITE);
    }

    /** Kingdom a kill counts in for Konar: a carved dungeon counts as the kingdom it belongs to. */
    private static Region taskRegion(Entity entity) {
        return TaskRegions.at(entity.blockPosition().getX(), entity.blockPosition().getY(), entity.blockPosition().getZ());
    }

    private static String describe(SlayerState.Profile p) {
        String name = SlayerTasks.displayName(p.task);
        if (p.region != null) {
            name += " in " + Region.valueOf(p.region).displayName();
        }
        return name;
    }

    private static void say(ServerPlayer player, SlayerMasters.Master master, String line) {
        player.displayClientMessage(Component.literal(master.name() + ": ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(line).withStyle(ChatFormatting.WHITE)), false);
    }

    /** Clickable task-management options shown by masters. */
    static void menu(ServerPlayer player, SlayerState.Profile p) {
        MutableComponent line = Component.literal("Options: ").withStyle(ChatFormatting.GRAY);
        line.append(button("[Skip " + SlayerRewards.SKIP_COST + "pts]", "/slayer skip", "Cancel this task for 30 points"));
        line.append(Component.literal(" "));
        line.append(button("[Block " + SlayerRewards.blockCost(p.master) + "pts]", "/slayer block", "Never be assigned this task again"));
        line.append(Component.literal(" "));
        line.append(button("[Rewards]", "/slayer rewards", "Spend your " + p.points + " slayer points"));
        line.append(Component.literal(" "));
        line.append(button("[Shop]", "/slayer shop", "Buy slayer equipment"));
        line.append(Component.literal(" "));
        line.append(button("[Craft]", "/slayer craft", "Assemble slayer helmets, rings and more"));
        if (p.unlocks.contains(SlayerRewards.TASK_STORAGE)) {
            line.append(Component.literal(" "));
            if (p.hasStoredTask()) {
                line.append(button("[" + (p.hasTask() ? "Swap" : "Unstore") + " task]", "/slayer unstore",
                        "Stored: " + p.storedRemaining + " " + SlayerTasks.displayName(p.storedTask)));
            } else {
                line.append(button("[Store task]", "/slayer store", "Put this task aside for later (free)"));
            }
        }
        player.displayClientMessage(line, false);
    }

    // ------------------------------------------------------------------ task storage

    /**
     * "Task Storage" (OSRS Wiki "Slayer Rewards"): stores the current task for free, for later; refused without the unlock,
     * without a task, or when a task is already stored. Wilderness tasks may be stored too.
     */
    public static boolean storeTask(ServerPlayer player) {
        SlayerState.Profile p = profile(player);
        if (!p.unlocks.contains(SlayerRewards.TASK_STORAGE)) {
            Msg.game(player, "You need to unlock Task Storage (500 points) first.");
            return false;
        }
        if (!p.hasTask()) {
            Msg.game(player, "You don't have a task to store.");
            return false;
        }
        if (p.hasStoredTask()) {
            Msg.game(player, "You already have a task stored: " + p.storedRemaining + " " + SlayerTasks.displayName(p.storedTask) + ".");
            return false;
        }
        if (com.runecraft.content.minigames.games.TzHaarSlayer.bossTaskTaken(player)) {
            Msg.game(player, "You can't store a task you have taken on as TzTok-Jad or TzKal-Zuk.");
            return false;
        }
        p.swapStored();
        Msg.game(player, "You store your task: " + p.storedRemaining + " " + SlayerTasks.displayName(p.storedTask)
                + ". You can take it up again from any Slayer master.");
        SlayerNet.sync(player);
        return true;
    }

    /**
     * Takes the stored task back up: unstores it when there is no current task, else swaps the two (free, OSRS). False
     * without a stored task.
     */
    public static boolean unstoreTask(ServerPlayer player) {
        SlayerState.Profile p = profile(player);
        if (!p.hasStoredTask()) {
            Msg.game(player, "You don't have a stored task.");
            return false;
        }
        boolean swap = p.hasTask();
        p.swapStored();
        Msg.game(player, (swap ? "You swap tasks: you're" : "You take up your stored task: you're") + " now hunting "
                + describe(p) + " (" + p.remaining + " to go)" + (swap ? "; your previous task is stored." : "."));
        SlayerNet.sync(player);
        return true;
    }

    // ------------------------------------------------------------------ Duly Noted

    /** "Duly Noted": a mithril dragon killed on task drops its mithril bars as banknotes (OSRS); null leaves a drop alone. */
    static net.minecraft.world.item.ItemStack dulyNoted(ServerPlayer killer, LivingEntity mob, String item, int count) {
        if (killer == null || mob == null) {
            return null;
        }
        String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getPath();
        if (!SlayerRewards.dulyNoted(id, item) || !profile(killer).unlocks.contains(SlayerRewards.DULY_NOTED)
                || !isOnTask(killer, mob)) {
            return null;
        }
        return com.runecraft.bank.Notes.note(item, "", count);
    }

    static MutableComponent button(String label, String command, String hover) {
        return Component.literal(label).withStyle(s -> s.withColor(ChatFormatting.GOLD)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hover))));
    }

    public static void checkTask(ServerPlayer player) {
        SlayerState.Profile p = profile(player);
        if (!p.hasTask()) {
            Msg.game(player, "You need something new to hunt. Visit a Slayer master. (Streak: " + p.streak + ", points: "
                    + p.points + ")");
        } else {
            Msg.game(player, "You're assigned to kill " + describe(p) + "; only " + p.remaining + " more to go. (Streak: "
                    + p.streak + ", points: " + p.points + ")");
        }
        SlayerNet.sync(player);
    }

    // ------------------------------------------------------------------ kills

    /** The task category a kill counts towards, or null. */
    public static String categoryOf(MonsterStats stats) {
        if (stats == null) {
            return null;
        }
        if (stats.profile() != null) {
            return SlayerCategories.categoryOf(stats.profile().id, stats.profile().slayerCategory);
        }
        if (stats.monster() != null) {
            SlayerCategories.Category c = SlayerCategories.ofBuiltIn(stats.monster().name());
            return c == null ? null : c.id();
        }
        return null;
    }

    /** True if a kill of this creature counts towards {@code task}: its category, or a boss task for that boss. */
    static boolean counts(String task, MonsterStats stats) {
        return counts(task, stats, null);
    }

    /** As {@link #counts(String, MonsterStats)} for a task set by {@code masterId} (Konar's boss tasks are stricter). */
    static boolean counts(String task, MonsterStats stats, String masterId) {
        if (task == null || stats == null) {
            return false;
        }
        if (task.equals(categoryOf(stats))) {
            return true;
        }
        if (stats.profile() != null
                && SlayerCategories.counts(task, stats.profile().id, stats.profile().slayerCategory)) {
            return true;
        }
        String id = stats.profile() != null ? stats.profile().id : stats.boss() != null ? stats.boss().entityId() : null;
        return SlayerBossTasks.counts(task, id, masterId);
    }

    /**
     * Public API for other modules (slayer bosses, task-only areas): whether the player's current task is
     * {@code categoryOrCreature} (a task category id such as "hellhounds" or "boss_cerberus") or counts kills of that
     * creature or boss id ("cerberus": a hellhound task or a Cerberus boss task). Location rules (Konar's region,
     * Krystilia's Wilderness) are not applied: see {@link #isOnTask} for those.
     */
    public static boolean hasTaskFor(ServerPlayer player, String categoryOrCreature) {
        SlayerState.Profile p = profile(player);
        if (!p.hasTask() || categoryOrCreature == null) {
            return false;
        }
        if (p.task.equals(categoryOrCreature)) {
            return true;
        }
        String declared = declaredCategory(categoryOrCreature);
        return SlayerCategories.counts(p.task, categoryOrCreature, declared)
                || SlayerBossTasks.counts(p.task, categoryOrCreature, p.master);
    }

    /** As {@link #hasTaskFor(ServerPlayer, String)} for a creature in the world (whatever counts towards the task). */
    public static boolean taskCountsFor(ServerPlayer player, LivingEntity entity) {
        SlayerState.Profile p = profile(player);
        return p.hasTask() && counts(p.task, MonsterStats.of(entity), p.master);
    }

    /**
     * Public API for the TzHaar minigames: how many TzTok-Jad / TzKal-Zuk kills a TzHaar task taken as a boss task may ask
     * for (OSRS Combat Achievement rewards: 1, 2 from the master tier, 3 from the grandmaster tier). The player is offered 1
     * up to this many ({@code content.minigames.games.TzHaarSlayer}).
     */
    public static int tzhaarBossTaskKills(ServerPlayer player) {
        return com.runecraft.osrs.content.systems.CombatAchievements.tzhaarTaskKills(
                com.runecraft.content.systems.CombatAchievementService.tier(player));
    }

    /** The slayer category a module monster or boss declares, or null. */
    private static String declaredCategory(String creatureId) {
        com.runecraft.osrs.content.MonsterDef m = ContentRegistry.monster(creatureId);
        if (m != null) {
            return m.slayerCategory;
        }
        com.runecraft.osrs.content.BossDef b = ContentRegistry.boss(creatureId);
        return b == null ? null : b.slayerCategory;
    }

    /** True if the entity counts towards the player's current task right now (location rules included). */
    public static boolean isOnTask(ServerPlayer player, LivingEntity entity) {
        SlayerState.Profile p = profile(player);
        if (!p.hasTask()) {
            return false;
        }
        if (!counts(p.task, MonsterStats.of(entity), p.master)) {
            return false;
        }
        SlayerMasters.Master m = p.master == null ? null : SlayerMasters.get(p.master);
        if (m != null && m.wilderness() && Gielinor.wildernessLevel(entity) <= 0) {
            return false;
        }
        return p.region == null || taskRegion(entity).name().equals(p.region);
    }

    /**
     * Kills that never count for Slayer: those that give no experience (Nightmare Zone practice dreams). Other dreams do
     * count: the dream bosses listed on the wiki's Slayer training table (Elvarg for green dragons, Dad for trolls...) give task
     * credit and their own Slayer XP (OSRS Wiki "Nightmare Zone"; the others have no Slayer category).
     */
    private static boolean noSlayerCredit(ServerPlayer player) {
        return !XpService.allowed(player, Skill.SLAYER);
    }

    /** The Slayer XP of a Nightmare Zone boss killed on task (the wiki's normal / hard values), or -1 for other kills. */
    private static double dreamSlayerXp(LivingEntity entity, String id) {
        return com.runecraft.osrs.content.minigames.MinigameRules.NightmareZone.slayerXp(id,
                entity.getTags().contains(com.runecraft.content.minigames.games.NightmareZoneSession.HARD_TAG));
    }

    /**
     * Public API for other modules: counts a boss kill towards the player's task when the boss died without the usual
     * death path ({@link #onKill}; e.g. a boss form or instance the boss script removes itself). Gives the boss's Slayer
     * experience and one kill (two for the Grotesque Guardians with "Double Trouble"); does nothing off task. The location
     * rules are judged where the player stands: see {@link #creditBossKill(ServerPlayer, String, Entity)}.
     */
    public static void creditBossKill(ServerPlayer player, String bossId) {
        creditBossKill(player, bossId, null);
    }

    /**
     * As {@link #creditBossKill(ServerPlayer, String)}, with the location rules (Krystilia's Wilderness, the kingdom Konar
     * named: Cerberus counts for her hellhounds only when she sent the player to Asgarnia) judged where {@code boss} died,
     * or where the player stands when it is null.
     */
    public static void creditBossKill(ServerPlayer player, String bossId, Entity boss) {
        SlayerState.Profile p = profile(player);
        if (!hasTaskFor(player, bossId) || noSlayerCredit(player)) {
            return;
        }
        Entity at = boss != null ? boss : player;
        SlayerMasters.Master master = p.master == null ? null : SlayerMasters.get(p.master);
        if (master != null && master.wilderness() && Gielinor.wildernessLevel(at) <= 0) {
            Msg.game(player, "Krystilia's tasks only count in the Wilderness.");
            return;
        }
        if (p.region != null && !taskRegion(at).name().equals(p.region)) {
            Msg.game(player, "Konar wants you to kill these in " + Region.valueOf(p.region).displayName() + ".");
            return;
        }
        com.runecraft.osrs.content.BossDef def = ContentRegistry.boss(bossId);
        com.runecraft.osrs.data.Boss legacy = com.runecraft.osrs.data.Boss.byEntity(bossId);
        int hp = def != null ? def.hitpoints : legacy != null ? legacy.hitpoints() : 0;
        XpService.add(player, Skill.SLAYER, com.runecraft.osrs.data.MonsterTraits.slayerXp(bossId, hp));
        p.chooseAmount = false;
        p.remaining = Math.max(0, p.remaining - SlayerTasks.killCredit(p.task, p.unlocks));
        if (p.remaining <= 0) {
            complete(player, p, master, master != null && eliteDiary(player, master));
        }
        SlayerNet.sync(player);
    }

    public static void onKill(ServerPlayer player, LivingEntity entity, MonsterStats stats) {
        SlayerState.Profile p = profile(player);
        if (!p.hasTask() || !counts(p.task, stats, p.master)) {
            return;
        }
        if (com.runecraft.content.minigames.games.TzHaarSlayer.bossTaskTaken(player)) {
            // A TzHaar task taken as TzTok-Jad / TzKal-Zuk kills counts only those bosses, one per run (TzHaarSlayer): the
            // TzHaar of Mor Ul Rek are no longer the task.
            return;
        }
        if (noSlayerCredit(player)) {
            // Kills that give no experience (Nightmare Zone practice dreams) give no task credit either.
            return;
        }
        SlayerMasters.Master master = p.master == null ? null : SlayerMasters.get(p.master);
        if (master != null && master.wilderness() && Gielinor.wildernessLevel(entity) <= 0) {
            Msg.game(player, "Krystilia's tasks only count in the Wilderness.");
            return;
        }
        if (p.region != null && !taskRegion(entity).name().equals(p.region)) {
            Msg.game(player, "Konar wants you to kill these in " + Region.valueOf(p.region).displayName() + ".");
            return;
        }
        // The first kill fixes a boss task's size.
        p.chooseAmount = false;
        CombatProfile<?> prof = stats.profile();
        Superiors.Superior superior = prof == null ? null : Superiors.byId(prof.id);
        // OSRS: a kill gives its hitpoints in Slayer XP unless the wiki lists another value (bosses, dragons, dream bosses...).
        double dream = prof == null ? -1 : dreamSlayerXp(entity, prof.id);
        XpService.add(player, Skill.SLAYER, superior != null ? superior.xp() : dream >= 0 ? dream
                : com.runecraft.osrs.data.MonsterTraits.slayerXp(stats.traitsKey(), stats.hitpoints()));
        // "Double Trouble": each Grotesque Guardians kill counts twice.
        // Expeditious bracelet / bracelet of slaughter (systems module): 25% chance to double or void the credit.
        String killed = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
        int credit = com.runecraft.content.systems.JewelleryService.slayerCredit(player, SlayerTasks.killCredit(p.task, p.unlocks),
                killed.equals("tztok_jad") || killed.equals("tzkal_zuk"));
        p.remaining = Math.max(0, p.remaining - credit);
        Random random = new Random(player.getRandom().nextLong());
        if (master != null && master.locationRestricted() && random.nextInt(SlayerChests.brimstoneKeyChance(stats.combatLevel())) == 0) {
            reward(player, entity, "brimstone_key", "A brimstone key falls to the floor.");
        }
        if (master != null && master.wilderness() && random.nextInt(SlayerChests.larranKeyChance(stats.combatLevel())) == 0) {
            reward(player, entity, "larran_key", "A Larran's key falls to the floor.");
        }
        // Superiors may also appear on the task's last kill (OSRS).
        if (superior == null && prof != null && p.unlocks.contains("bigger_and_badder")
                && random.nextInt(com.runecraft.content.systems.CombatAchievementService.superiorChance(player, SlayerRewards.SUPERIOR_CHANCE)) == 0) {
            Superiors.Superior s = Superiors.forBase(prof.id);
            if (s != null) {
                spawnSuperior(player, entity, s);
            }
        }
        if (p.remaining <= 0) {
            complete(player, p, master, master != null && eliteDiary(player, master));
        }
        SlayerNet.sync(player);
    }

    /**
     * Completes the current task now, with its streak and points: the last TzTok-Jad / TzKal-Zuk kill ends a TzHaar task
     * taken as a boss task ({@code content.minigames.games.TzHaarSlayer}).
     */
    public static void completeTask(ServerPlayer player) {
        SlayerState.Profile p = profile(player);
        if (p.task == null) {
            return;
        }
        SlayerMasters.Master master = p.master == null ? null : SlayerMasters.get(p.master);
        complete(player, p, master, master != null && eliteDiary(player, master));
        SlayerNet.sync(player);
    }

    private static void complete(ServerPlayer player, SlayerState.Profile p, SlayerMasters.Master master,
                                 boolean eliteDiary) {
        p.remaining = 0;
        p.completed++;
        // Krystilia's Wilderness tasks keep their own streak (OSRS).
        int streak;
        if (master != null && master.wilderness()) {
            streak = ++p.wildyStreak;
        } else {
            streak = ++p.streak;
        }
        int points = master == null ? 0 : master.pointsFor(streak, eliteDiary);
        p.points += points;
        Msg.red(player, "You have completed your task! You've completed " + streak + " tasks in a row"
                + (points > 0 ? " and received " + points + " points, giving you a total of " + p.points : "")
                + ". Return to a Slayer master.");
        if (SlayerBossTasks.get(p.task) != null) {
            // Every boss task ends with 5,000 bonus Slayer experience, whatever its length (OSRS).
            XpService.add(player, Skill.SLAYER, SlayerBossTasks.COMPLETION_XP);
        }
        p.chooseAmount = false;
        p.region = null;
        ServerNetworking.sync(player);
    }

    private static void reward(ServerPlayer player, LivingEntity at, String item, String message) {
        com.runecraft.combat.LootDrops.spawn(at, Inv.stack(item, 1), player);
        CollectionLog.obtain(player, item);
        Msg.red(player, message);
    }

    private static void spawnSuperior(ServerPlayer player, LivingEntity base, Superiors.Superior s) {
        EntityType<?> type = ModEntities.typeById(s.id());
        if (type == null || !(base.level() instanceof ServerLevel world)) {
            return;
        }
        Entity e = type.create(world);
        if (e == null) {
            return;
        }
        e.moveTo(base.getX(), base.getY(), base.getZ(), base.getYRot(), 0);
        world.addFreshEntity(e);
        if (e instanceof Mob mob) {
            mob.setTarget(player);
        }
        Msg.red(player, "A superior foe has appeared...");
    }

    // ------------------------------------------------------------------ combat bonuses

    /** Black mask / slayer helmet: +16.67% melee max hit against the current task. */
    public static int boostMaxHit(ServerPlayer player, LivingEntity target, int maxHit) {
        if (target != null && wearingAny(player, SlayerGear.MELEE_BOOST_HEADGEAR) && isOnTask(player, target)
                && !com.runecraft.combat.GearEffects.salveActive(player, target, com.runecraft.osrs.AttackType.CRUSH)) {
            return maxHit * 7 / 6;
        }
        return maxHit;
    }

    /**
     * Burn damage the Karuulm Slayer Dungeon's volcanic floor deals the player this tick: 4 beyond the entrance chamber, 0
     * elsewhere, with boots of stone, boots of brimstone or granite boots, or with the elite Kourend &amp; Kebos Diary.
     */
    public static int hotFloorDamage(ServerPlayer player) {
        // The dungeon is carved in Gielinor only: the same block coordinates elsewhere (Overworld, Nether) are plain ground.
        if (!com.runecraft.world.Gielinor.is(player)) {
            return 0;
        }
        net.minecraft.core.BlockPos at = player.blockPosition();
        if (!SlayerGear.hotFloorAt(at.getX(), at.getY(), at.getZ())) {
            return 0;
        }
        boolean elite = com.runecraft.osrs.content.systems.Diaries.tierComplete(
                com.runecraft.content.systems.SysState.of(player).diaryDone, "kourend",
                com.runecraft.osrs.content.systems.Diaries.Tier.ELITE);
        return SlayerGear.hotFloorDamage(Inv.idOf(player.getItemBySlot(EquipmentSlot.FEET)), elite);
    }

    static boolean wearingAny(ServerPlayer player, List<String> items) {
        String head = Inv.idOf(player.getItemBySlot(EquipmentSlot.HEAD));
        return items.contains(head);
    }

    /** Points, streak and unlock bookkeeping for commands. */
    static String summary(SlayerState.Profile p) {
        return String.format(Locale.ROOT, "Points: %d  Streak: %d  Wilderness streak: %d  Tasks completed: %d", p.points,
                p.streak, p.wildyStreak, p.completed);
    }
}
