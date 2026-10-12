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
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Slayer tasks, points, streaks, keys and superiors (called through {@link com.runecraft.combat.Slayer}). */
public final class SlayerService {
    private SlayerService() {
    }

    public static SlayerState.Profile profile(ServerPlayerEntity player) {
        SlayerState.Profile p = SlayerState.of(player.getServer()).get(player.getUuid());
        if (!p.migrated) {
            migrate(player, p);
        }
        return p;
    }

    /** Moves a task stored by the old core system (PlayerData) into the slayer module's state. */
    private static void migrate(ServerPlayerEntity player, SlayerState.Profile p) {
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
    public static void talkToStation(ServerPlayerEntity player) {
        PlayerData data = Players.data(player);
        SlayerMasters.Master m = SlayerMasters.bestFor(data.skills().combatLevel(), data.skills().level(Skill.SLAYER),
                x -> x.quest() == null || ContentRegistry.quest(x.quest()) == null || QuestApi.isComplete(player, x.quest()));
        talk(player, m);
    }

    public static void talk(ServerPlayerEntity player, SlayerMasters.Master master) {
        PlayerData data = Players.data(player);
        SlayerState.Profile p = profile(player);
        int combat = data.skills().combatLevel();
        int slayer = data.skills().level(Skill.SLAYER);
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
            say(player, master, "You're still hunting " + describe(p) + "; you have " + p.remaining + " to go.");
            menu(player, p);
            return;
        }
        assign(player, master, p, combat, slayer);
    }

    private static void assign(ServerPlayerEntity player, SlayerMasters.Master master, SlayerState.Profile p, int combat,
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
            // "Like a Boss": one boss with equal chance (Krystilia: Wilderness bosses only).
            boss = SlayerBossTasks.pick(random, slayer, master.wilderness(), blocked, previous);
            if (boss == null) {
                say(player, master, "I have nothing for you right now.");
                return;
            }
        }
        p.task = boss != null ? boss.id() : a.category();
        p.amount = boss != null ? boss.min() + random.nextInt(boss.max() - boss.min() + 1) : SlayerTasks.amount(a, extended, random);
        p.remaining = p.amount;
        p.master = master.id();
        p.region = null;
        if (master.locationRestricted() && boss == null) {
            List<Region> regions = SlayerTasks.regions(a.category());
            if (!regions.isEmpty()) {
                p.region = regions.get(random.nextInt(regions.size())).name();
            }
        }
        say(player, master, "Your new task is to kill " + p.amount + " " + describe(p) + "."
                + (master.wilderness() ? " They only count in the Wilderness." : ""));
        SlayerNet.sync(player);
    }

    /**
     * A task's quest requirement: {@code id} must be complete, {@code ~id} only started. Quests RuneCraft
     * doesn't have never block a task.
     */
    private static boolean questMet(ServerPlayerEntity player, String requirement) {
        boolean started = requirement.startsWith("~");
        String id = started ? requirement.substring(1) : requirement;
        if (ContentRegistry.quest(id) == null) {
            return true;
        }
        return started ? QuestApi.stage(player, id) > 0 || QuestApi.isComplete(player, id) : QuestApi.isComplete(player, id);
    }

    /** Whether the elite tier of the master's points diary (Konar: Kourend &amp; Kebos; Nieve/Steve: Western) is done. */
    private static boolean eliteDiary(ServerPlayerEntity player, SlayerMasters.Master master) {
        return master.diary() != null && com.runecraft.osrs.content.systems.Diaries.tierComplete(
                com.runecraft.content.systems.SysState.of(player).diaryDone, master.diary(),
                com.runecraft.osrs.content.systems.Diaries.Tier.ELITE);
    }

    /** Kingdom a kill counts in for Konar: a carved dungeon counts as the kingdom it belongs to. */
    private static Region taskRegion(Entity entity) {
        return TaskRegions.at(entity.getBlockPos().getX(), entity.getBlockPos().getY(), entity.getBlockPos().getZ());
    }

    private static String describe(SlayerState.Profile p) {
        String name = SlayerTasks.displayName(p.task);
        if (p.region != null) {
            name += " in " + Region.valueOf(p.region).displayName();
        }
        return name;
    }

    private static void say(ServerPlayerEntity player, SlayerMasters.Master master, String line) {
        player.sendMessage(Text.literal(master.name() + ": ").formatted(Formatting.YELLOW)
                .append(Text.literal(line).formatted(Formatting.WHITE)), false);
    }

    /** Clickable task-management options shown by masters. */
    static void menu(ServerPlayerEntity player, SlayerState.Profile p) {
        MutableText line = Text.literal("Options: ").formatted(Formatting.GRAY);
        line.append(button("[Skip " + SlayerRewards.SKIP_COST + "pts]", "/slayer skip", "Cancel this task for 30 points"));
        line.append(Text.literal(" "));
        line.append(button("[Block " + SlayerRewards.blockCost(p.master) + "pts]", "/slayer block", "Never be assigned this task again"));
        line.append(Text.literal(" "));
        line.append(button("[Rewards]", "/slayer rewards", "Spend your " + p.points + " slayer points"));
        line.append(Text.literal(" "));
        line.append(button("[Shop]", "/slayer shop", "Buy slayer equipment"));
        line.append(Text.literal(" "));
        line.append(button("[Craft]", "/slayer craft", "Assemble slayer helmets, rings and more"));
        player.sendMessage(line, false);
    }

    static MutableText button(String label, String command, String hover) {
        return Text.literal(label).styled(s -> s.withColor(Formatting.GOLD)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hover))));
    }

    public static void checkTask(ServerPlayerEntity player) {
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
        if (task == null || stats == null) {
            return false;
        }
        if (task.equals(categoryOf(stats))) {
            return true;
        }
        String id = stats.profile() != null ? stats.profile().id : stats.boss() != null ? stats.boss().entityId() : null;
        return SlayerBossTasks.counts(task, id);
    }

    /** True if the entity counts towards the player's current task right now (location rules included). */
    public static boolean isOnTask(ServerPlayerEntity player, LivingEntity entity) {
        SlayerState.Profile p = profile(player);
        if (!p.hasTask()) {
            return false;
        }
        if (!counts(p.task, MonsterStats.of(entity))) {
            return false;
        }
        SlayerMasters.Master m = p.master == null ? null : SlayerMasters.get(p.master);
        if (m != null && m.wilderness() && Gielinor.wildernessLevel(entity) <= 0) {
            return false;
        }
        return p.region == null || taskRegion(entity).name().equals(p.region);
    }

    public static void onKill(ServerPlayerEntity player, LivingEntity entity, MonsterStats stats) {
        SlayerState.Profile p = profile(player);
        if (!p.hasTask() || !counts(p.task, stats)) {
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
        CombatProfile<?> prof = stats.profile();
        Superiors.Superior superior = prof == null ? null : Superiors.byId(prof.id);
        XpService.add(player, Skill.SLAYER, superior != null ? superior.xp() : stats.hitpoints());
        // "Double Trouble": each Grotesque Guardians kill counts twice.
        p.remaining = Math.max(0, p.remaining - SlayerTasks.killCredit(p.task, p.unlocks));
        Random random = new Random(player.getRandom().nextLong());
        if (master != null && master.locationRestricted() && random.nextInt(SlayerChests.brimstoneKeyChance(stats.combatLevel())) == 0) {
            reward(player, entity, "brimstone_key", "A brimstone key falls to the floor.");
        }
        if (master != null && master.wilderness() && random.nextInt(SlayerChests.larranKeyChance(stats.combatLevel())) == 0) {
            reward(player, entity, "larran_key", "A Larran's key falls to the floor.");
        }
        // Superiors may also appear on the task's last kill (OSRS).
        if (superior == null && prof != null && p.unlocks.contains("bigger_and_badder")
                && random.nextInt(SlayerRewards.SUPERIOR_CHANCE) == 0) {
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

    private static void complete(ServerPlayerEntity player, SlayerState.Profile p, SlayerMasters.Master master,
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
        p.region = null;
        ServerNetworking.sync(player);
    }

    private static void reward(ServerPlayerEntity player, LivingEntity at, String item, String message) {
        at.dropStack(Inv.stack(item, 1));
        CollectionLog.obtain(player, item);
        Msg.red(player, message);
    }

    private static void spawnSuperior(ServerPlayerEntity player, LivingEntity base, Superiors.Superior s) {
        EntityType<?> type = ModEntities.typeById(s.id());
        if (type == null || !(base.getWorld() instanceof ServerWorld world)) {
            return;
        }
        Entity e = type.create(world);
        if (e == null) {
            return;
        }
        e.refreshPositionAndAngles(base.getX(), base.getY(), base.getZ(), base.getYaw(), 0);
        world.spawnEntity(e);
        if (e instanceof MobEntity mob) {
            mob.setTarget(player);
        }
        Msg.red(player, "A superior foe has appeared...");
    }

    // ------------------------------------------------------------------ combat bonuses

    /** Black mask / slayer helmet: +16.67% melee max hit against the current task. */
    public static int boostMaxHit(ServerPlayerEntity player, LivingEntity target, int maxHit) {
        if (target != null && wearingAny(player, SlayerGear.MELEE_BOOST_HEADGEAR) && isOnTask(player, target)
                && !com.runecraft.combat.GearEffects.salveActive(player, target, com.runecraft.osrs.AttackType.CRUSH)) {
            return maxHit * 7 / 6;
        }
        return maxHit;
    }

    static boolean wearingAny(ServerPlayerEntity player, List<String> items) {
        String head = Inv.idOf(player.getEquippedStack(EquipmentSlot.HEAD));
        return items.contains(head);
    }

    /** Points, streak and unlock bookkeeping for commands. */
    static String summary(SlayerState.Profile p) {
        return String.format(Locale.ROOT, "Points: %d  Streak: %d  Wilderness streak: %d  Tasks completed: %d", p.points,
                p.streak, p.wildyStreak, p.completed);
    }
}
