package com.runecraft.selftest;

import com.runecraft.combat.CombatHandler;
import com.runecraft.content.slayer.SlayerCombat;
import com.runecraft.content.slayer.SlayerService;
import com.runecraft.content.slayer.SlayerState;
import com.runecraft.content.systems.SysState;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.dungeons.Dungeons;
import com.runecraft.osrs.content.slayer.SlayerBossTasks;
import com.runecraft.osrs.content.slayer.SlayerGear;
import com.runecraft.osrs.content.slayer.SlayerMasters;
import com.runecraft.osrs.content.slayer.SlayerTasks;
import com.runecraft.osrs.content.slayer.Superiors;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModEntities;
import com.runecraft.util.Inv;
import com.runecraft.world.StructurePlacer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;

/**
 * Slayer walked as a player would (round 35): a task from a master, its kills (Slayer experience, the count), completing
 * it on a streak milestone with the right points, buying an unlock and an item with points, a boss task with its chosen
 * size, credited through the boss API and finished with the 5,000 experience bonus; then the rules that bite: a task-only
 * dungeon's monsters can't be hurt off task, protective gear (worn in the right slot) stops a creature's special, the
 * leaf-bladed and finishing-item rules, and the Karuulm Slayer Dungeon's floor burns without boots of stone. Works with the
 * fake player and with a real one ({@code /rc selftest real slayer:}).
 */
final class SlayerChecks {
    private final TestContext c;

    private SlayerChecks(TestContext c) {
        this.c = c;
    }

    static List<SelfTest.Check> all(TestContext context) {
        SlayerChecks g = new SlayerChecks(context);
        List<SelfTest.Check> checks = new ArrayList<>();
        checks.add(new SelfTest.Check("slayer:walk:task_points_unlock_boss", g::walk));
        checks.add(new SelfTest.Check("slayer:task_only_dungeon", g::taskOnly));
        checks.add(new SelfTest.Check("slayer:protective_gear", g::protectiveGear));
        checks.add(new SelfTest.Check("slayer:leaf_bladed_and_finishers", g::weaponsAndFinishers));
        checks.add(new SelfTest.Check("slayer:karuulm_hot_floor", g::hotFloor));
        checks.add(new SelfTest.Check("slayer:nightmare_zone_credit", g::nmzCredit));
        // Round 44: every task-only area (a task from a master, refused without it, one kill there) and the rewards.
        for (TaskOnlySpot spot : taskOnlySpots()) {
            checks.add(new SelfTest.Check("slayer:task_only:" + spot.dungeon().replace("dng_", "") + ":" + spot.task(),
                    () -> g.taskOnlyArea(spot)));
        }
        checks.add(new SelfTest.Check("slayer:konar_location", g::konarLocation));
        checks.add(new SelfTest.Check("slayer:mortimer", g::mortimer));
        checks.add(new SelfTest.Check("slayer:task_storage", g::taskStorage));
        checks.add(new SelfTest.Check("slayer:duly_noted_and_rune_pouch", g::dulyNotedAndPouch));
        return checks;
    }

    private static AssertionError failure(String message) {
        return new AssertionError(message);
    }

    private ServerPlayer p() {
        return c.player;
    }

    private PlayerData data() {
        return Players.data(c.player);
    }

    private SlayerState.Profile profile() {
        return SlayerService.profile(p());
    }

    /** Runs a {@code /slayer} subcommand as the player (with operator rights, so no master needs to stand nearby). */
    private void slayerCommand(String args) {
        c.server.getCommands().performPrefixedCommand(p().createCommandSourceStack().withPermission(4), "slayer " + args);
    }

    private LivingEntity spawnAt(EntityType<?> type, BlockPos pos, boolean frozen) {
        Entity e = type.create(c.world);
        if (!(e instanceof LivingEntity living)) {
            throw failure(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type) + " is not a living entity");
        }
        living.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
        if (living instanceof Mob mob) {
            mob.finalizeSpawn(c.world, c.world.getCurrentDifficultyAt(pos), MobSpawnType.SPAWNER, null);
            mob.setPersistenceRequired();
            mob.setNoAi(frozen);
        }
        c.world.addFreshEntity(living);
        return living;
    }

    private LivingEntity spawn(String id) {
        EntityType<?> type = ModEntities.typeById(id);
        if (type == null) {
            throw new SelfTest.Skip("no " + id + " entity");
        }
        return spawnAt(type, c.near(3, 0), true);
    }

    private void killAsPlayer(LivingEntity victim) {
        victim.setHealth(0f);
        victim.die(CombatHandler.source(p()));
        victim.discard();
    }

    /** An entity type whose kill counts towards {@code task} (not a superior), or null. */
    private static EntityType<?> target(String task) {
        for (MonsterDef m : SlayerTasks.creatures(task)) {
            if (Superiors.byId(m.id) == null && ModEntities.typeById(m.id) != null) {
                return ModEntities.typeById(m.id);
            }
        }
        return null;
    }

    private double slayerXp() {
        return data().skills().xpTenths(Skill.SLAYER) / 10.0;
    }

    // ------------------------------------------------------------------ the walk

    private String walk() {
        c.completeQuests(null);
        SlayerState.Profile prof = profile();
        StringBuilder note = new StringBuilder();
        // 1. Vannaka (combat 40, no quest) gives a task the player can find; nine tasks are already done.
        SlayerMasters.Master vannaka = SlayerMasters.get("vannaka");
        EntityType<?> type = null;
        for (int tries = 0; tries < 40 && type == null; tries++) {
            prof.task = null;
            prof.remaining = 0;
            SlayerService.talk(p(), vannaka);
            if (!prof.hasTask()) {
                throw failure("Vannaka gave a maxed player no task");
            }
            type = target(prof.task);
        }
        if (type == null) {
            throw new SelfTest.Skip("none of Vannaka's tasks had a spawnable creature");
        }
        SlayerMasters.Assignment line = vannaka.assignment(prof.task);
        if (line == null || prof.amount < line.min() || prof.amount > Math.max(line.max(), line.extMax())
                || !"vannaka".equals(prof.master)) {
            throw failure("Vannaka's task " + prof.amount + " x " + prof.task + " is outside his table " + line);
        }
        note.append("Vannaka: ").append(prof.amount).append(" x ").append(prof.task);
        // 2. Kill three: each gives Slayer experience and lowers the count by one.
        prof.streak = 9;
        prof.points = 0;
        int kills = Math.min(3, prof.amount - 1);
        for (int i = 0; i < kills; i++) {
            int before = prof.remaining;
            double xp = slayerXp();
            killAsPlayer(spawnAt(type, c.near(3, 0), true));
            if (prof.remaining != before - 1) {
                throw failure("a " + prof.task + " kill took the count from " + before + " to " + prof.remaining);
            }
            if (slayerXp() <= xp) {
                throw failure("a " + prof.task + " kill gave no Slayer experience");
            }
        }
        // 3. The last kill completes the 10th task in a row: Vannaka's 8 points x5.
        prof.remaining = 1;
        killAsPlayer(spawnAt(type, c.near(3, 0), true));
        if (prof.hasTask() || prof.streak != 10 || prof.points != 40) {
            throw failure("completing the 10th Vannaka task left " + prof.remaining + " to go, streak " + prof.streak + ", "
                    + prof.points + " points (want 0, 10, 40)");
        }
        note.append(", completed: streak 10, 40 pts");
        // 4. Spend points: "Like a Boss" (200) and a looting bag (10).
        prof.points = 250;
        slayerCommand("unlock " + SlayerBossTasks.UNLOCK);
        slayerCommand("buy looting_bag");
        if (!prof.unlocks.contains(SlayerBossTasks.UNLOCK) || prof.points != 40 || !Inv.has(p(), "looting_bag", 1)) {
            throw failure("buying Like a Boss and a looting bag left " + prof.points + " points, unlocks " + prof.unlocks
                    + ", looting bag " + Inv.has(p(), "looting_bag", 1));
        }
        note.append(", bought Like a Boss + looting bag");
        // 5. Duradel (Shilo Village done) sooner or later assigns a boss; its size is the player's choice.
        SlayerMasters.Master duradel = SlayerMasters.get("duradel");
        SlayerBossTasks.BossTask boss = null;
        for (int tries = 0; tries < 600 && boss == null; tries++) {
            prof.task = null;
            prof.remaining = 0;
            SlayerService.talk(p(), duradel);
            boss = SlayerBossTasks.get(prof.task);
        }
        if (boss == null) {
            throw failure("Duradel never assigned a boss task in 600 tries with Like a Boss");
        }
        if (!prof.chooseAmount || prof.amount != boss.maxKills(0)) {
            throw failure("a new " + boss.id() + " task should wait for its size at " + boss.maxKills(0) + ", got " + prof.amount
                    + " (choosing " + prof.chooseAmount + ")");
        }
        slayerCommand("amount 3");
        if (prof.amount != 3 || prof.remaining != 3 || prof.chooseAmount) {
            throw failure("choosing 3 kills made the boss task " + prof.remaining + "/" + prof.amount);
        }
        note.append(", boss task ").append(boss.id());
        // 6. The bosses module's API: the task covers the boss; three credited kills finish it with 5,000 bonus XP.
        String bossId = boss.creatures().get(0);
        if (!SlayerService.hasTaskFor(p(), bossId) || !SlayerService.hasTaskFor(p(), boss.id())
                || SlayerService.hasTaskFor(p(), "zulrah".equals(bossId) ? "vorkath" : "zulrah")) {
            throw failure("hasTaskFor is wrong for the " + boss.id() + " task");
        }
        double xpBefore = slayerXp();
        for (int i = 0; i < 3; i++) {
            SlayerService.creditBossKill(p(), bossId);
        }
        double gained = slayerXp() - xpBefore;
        if (prof.hasTask() || prof.streak != 11 || prof.points != 40 + 15 || gained < SlayerBossTasks.COMPLETION_XP) {
            throw failure("three " + bossId + " kills left " + prof.remaining + " to go, streak " + prof.streak + ", "
                    + prof.points + " points, +" + gained + " XP (want 0, 11, 55, >= 5,000)");
        }
        note.append(": done, +").append(Math.round(gained)).append(" XP");
        // 7. A hellhound task covers Cerberus (the slayer bosses' access rule) but not the Kraken.
        prof.task = "hellhounds";
        prof.remaining = 10;
        if (!SlayerService.hasTaskFor(p(), "cerberus") || SlayerService.hasTaskFor(p(), "kraken")) {
            throw failure("a hellhound task should cover Cerberus and not the Kraken");
        }
        // 8. Konar's hellhounds: a boss kill credited through the API counts only in the kingdom she named.
        BlockPos at = p().blockPosition();
        com.runecraft.osrs.Region here = com.runecraft.osrs.world.TaskRegions.at(at.getX(), at.getY(), at.getZ());
        prof.master = "konar";
        prof.region = (here == com.runecraft.osrs.Region.ASGARNIA ? com.runecraft.osrs.Region.WILDERNESS
                : com.runecraft.osrs.Region.ASGARNIA).name();
        try {
            SlayerService.creditBossKill(p(), "cerberus");
            if (prof.remaining != 10) {
                throw failure("a Cerberus kill counted for Konar's hellhounds outside " + prof.region);
            }
            prof.region = here.name();
            SlayerService.creditBossKill(p(), "cerberus");
            if (prof.remaining != 9) {
                throw failure("a Cerberus kill in " + here + " didn't count for Konar's hellhounds there (" + prof.remaining
                        + " left, want 9)");
            }
        } finally {
            prof.task = null;
            prof.remaining = 0;
            prof.region = null;
        }
        note.append(", Konar's kingdom rule holds for boss credit");
        // 9. Her named location: Cerberus counts for hellhounds she wants killed in Taverley Dungeon (wherever the lair
        // is), not for those she wants killed in the Karuulm Slayer Dungeon.
        prof.task = "hellhounds";
        prof.remaining = 10;
        prof.master = "konar";
        try {
            prof.region = SlayerService.LOCATION + "karuulm_slayer_dungeon";
            SlayerService.creditBossKill(p(), "cerberus");
            if (prof.remaining != 10) {
                throw failure("a Cerberus kill counted for Konar's hellhounds in the Karuulm Slayer Dungeon");
            }
            prof.region = SlayerService.LOCATION + "taverley_dungeon";
            SlayerService.creditBossKill(p(), "cerberus");
            if (prof.remaining != 9) {
                throw failure("a Cerberus kill didn't count for Konar's hellhounds in Taverley Dungeon (" + prof.remaining
                        + " left, want 9)");
            }
        } finally {
            prof.task = null;
            prof.remaining = 0;
            prof.region = null;
        }
        note.append(", and her Taverley Dungeon location takes Cerberus");
        return note.toString();
    }

    /** Konar's place for a task at a spot: her location holding it, else the kingdom (a task with no built location). */
    private static String konarPlaceAt(String task, BlockPos at) {
        for (com.runecraft.osrs.content.slayer.KonarLocations.Location l
                : com.runecraft.osrs.content.slayer.KonarLocations.candidates(task)) {
            if (l.contains(at.getX(), at.getY(), at.getZ())) {
                return SlayerService.LOCATION + l.id();
            }
        }
        return com.runecraft.osrs.world.TaskRegions.at(at.getX(), at.getY(), at.getZ()).name();
    }

    // ------------------------------------------------------------------ Mortimer (round 44)

    /**
     * Mortimer offers two tasks with modifiers (three after 100 of his tasks); the player picks one, and completing it pays
     * only the points modifier's points.
     */
    private String mortimer() {
        c.completeQuests(null);
        SlayerState.Profile prof = profile();
        SlayerMasters.Master mortimer = SlayerMasters.get(com.runecraft.osrs.content.slayer.Mortimer.ID);
        int doneBefore = prof.mortimerTasks;
        int pointsBefore = prof.points;
        try {
            prof.task = null;
            prof.remaining = 0;
            prof.mortimerTasks = 0;
            SlayerService.talk(p(), mortimer);
            List<com.runecraft.osrs.content.slayer.Mortimer.Offer> offers = SlayerService.mortimerOffers(p());
            if (prof.hasTask() || offers.size() != 2 || offers.get(0).category().equals(offers.get(1).category())) {
                throw failure("Mortimer should offer two different tasks, offered " + offers + (prof.hasTask() ? " and assigned "
                        + prof.task : ""));
            }
            com.runecraft.osrs.content.slayer.Mortimer.Offer pick = offers.get(1);
            if (!SlayerService.chooseMortimer(p(), 2) || !pick.category().equals(prof.task) || prof.amount != pick.amount()
                    || !pick.modifier().name().equals(prof.modifier) || !com.runecraft.osrs.content.slayer.Mortimer.ID.equals(prof.master)) {
                throw failure("picking Mortimer's second offer " + pick + " gave " + prof.task + " x" + prof.amount + " ("
                        + prof.modifier + ")");
            }
            // Finish it with a points modifier: only that pays.
            prof.modifier = com.runecraft.osrs.content.slayer.Mortimer.Modifier.POINTS.name();
            prof.modifierValue = 20;
            prof.points = 0;
            SlayerService.completeTask(p());
            if (prof.points != 20 || prof.mortimerTasks != 1 || prof.modifier != null) {
                throw failure("a Mortimer task with +20 points paid " + prof.points + " (tasks " + prof.mortimerTasks
                        + ", modifier " + prof.modifier + ")");
            }
            prof.mortimerTasks = 100;
            prof.task = null;
            prof.remaining = 0;
            SlayerService.talk(p(), mortimer);
            int third = SlayerService.mortimerOffers(p()).size();
            if (third != 3) {
                throw failure("after 100 of his tasks Mortimer offered " + third + " tasks, not 3");
            }
            return "offered " + offers.get(0).category() + " / " + pick.category() + ", took " + pick.category() + " ("
                    + pick.modifier().describe(pick.value()) + "); +20 points modifier paid 20; 3 offers at 100 tasks";
        } finally {
            prof.task = null;
            prof.remaining = 0;
            prof.modifier = null;
            prof.modifierValue = 0;
            prof.mortimerTasks = doneBefore;
            prof.points = pointsBefore;
        }
    }

    // ------------------------------------------------------------------ Konar's locations (round 44)

    /**
     * Konar names a location, not a kingdom: her task comes with one of its built locations; a hellhound killed in Taverley
     * Dungeon counts when she named Taverley Dungeon and not when she named the Stronghold Slayer Cave, also in the west.
     */
    private String konarLocation() {
        c.completeQuests(null);
        SlayerState.Profile prof = profile();
        SlayerMasters.Master konar = SlayerMasters.get("konar");
        StringBuilder note = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            prof.task = null;
            prof.remaining = 0;
            prof.region = null;
            SlayerService.talk(p(), konar);
            if (!prof.hasTask()) {
                throw failure("Konar gave a maxed player no task");
            }
            if (prof.task.startsWith("boss_") || com.runecraft.osrs.content.slayer.KonarLocations.candidates(prof.task).isEmpty()) {
                continue;
            }
            String id = prof.region == null || !prof.region.startsWith(SlayerService.LOCATION) ? null
                    : prof.region.substring(SlayerService.LOCATION.length());
            com.runecraft.osrs.content.slayer.KonarLocations.Location l =
                    id == null ? null : com.runecraft.osrs.content.slayer.KonarLocations.get(id);
            if (l == null || !com.runecraft.osrs.content.slayer.KonarLocations.candidates(prof.task).contains(l)) {
                throw failure("Konar's " + prof.task + " task came with " + prof.region + ", not one of its locations");
            }
            note.append(prof.task).append(" in ").append(l.name()).append("; ");
            break;
        }
        Dungeons.Camp cp = camp("dng_taverley_dungeon", "hellhound");
        BlockPos at = new BlockPos(cp.centreX(), cp.feetY(), cp.centreZ());
        EntityType<?> type = ModEntities.typeById("hellhound");
        if (type == null) {
            throw new SelfTest.Skip("no hellhound entity");
        }
        StructurePlacer.applyAround(c.world, at.getX(), at.getZ(), 6);
        p().teleportTo(c.world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p().getYRot(), p().getXRot());
        ChunkSettle.holdAndSettle(c.world, at, 16);
        LivingEntity first = null;
        LivingEntity second = null;
        try {
            prof.task = "hellhounds";
            prof.amount = 40;
            prof.remaining = 40;
            prof.master = "konar";
            prof.region = SlayerService.LOCATION + "stronghold_slayer_cave";
            first = spawnAt(type, at.offset(1, 0, 0), true);
            killAsPlayer(first);
            if (prof.remaining != 40) {
                throw failure("a hellhound in Taverley Dungeon counted for Konar's Stronghold Slayer Cave task");
            }
            prof.region = SlayerService.LOCATION + "taverley_dungeon";
            second = spawnAt(type, at.offset(1, 0, 0), true);
            killAsPlayer(second);
            if (prof.remaining != 39) {
                throw failure("a hellhound in Taverley Dungeon didn't count for Konar's Taverley Dungeon task ("
                        + prof.remaining + " left, want 39)");
            }
            return note + "Taverley hellhound: Stronghold task 40 -> 40, Taverley task 40 -> 39";
        } finally {
            for (LivingEntity e : new LivingEntity[]{first, second}) {
                if (e != null && e.isAlive()) {
                    e.discard();
                }
            }
            prof.task = null;
            prof.remaining = 0;
            prof.region = null;
            c.home();
        }
    }

    // ------------------------------------------------------------------ task-only dungeons

    private static Dungeons.Camp camp(String dungeon, String creature) {
        for (Dungeons.Camp cp : Dungeons.camps()) {
            if (cp.dungeon().equals(dungeon) && (creature == null || cp.creatures().contains(creature))) {
                return cp;
            }
        }
        throw failure("no " + creature + " camp in " + dungeon);
    }

    /** A smoke devil in the Smoke Devil Dungeon takes no damage off task (or on a Thermonuclear smoke devil task). */
    private String taskOnly() {
        Dungeons.Camp cp = camp("dng_smoke_devil_dungeon", "smoke_devil");
        BlockPos at = new BlockPos(cp.centreX(), cp.feetY(), cp.centreZ());
        if (Dungeons.taskOnly(at.getX(), at.getY(), at.getZ()) == null) {
            throw failure("the Smoke Devil Dungeon camp at " + at.toShortString() + " is not task-only");
        }
        StructurePlacer.applyAround(c.world, at.getX(), at.getZ(), 4);
        EntityType<?> type = ModEntities.typeById("smoke_devil");
        if (type == null) {
            throw new SelfTest.Skip("no smoke_devil entity");
        }
        SlayerState.Profile prof = profile();
        LivingEntity devil = spawnAt(type, at, true);
        try {
            prof.task = null;
            prof.remaining = 0;
            float hp = devil.getHealth();
            if (CombatHandler.applyDamage(p(), devil, 3) != 0 || devil.getHealth() < hp) {
                throw failure("a player without a task hurt a smoke devil in the Smoke Devil Dungeon");
            }
            prof.task = "boss_thermonuclear_smoke_devil";
            prof.remaining = 5;
            if (CombatHandler.applyDamage(p(), devil, 3) != 0) {
                throw failure("a Thermonuclear smoke devil boss task let the player hurt an ordinary smoke devil");
            }
            prof.task = "smoke_devils";
            prof.remaining = 50;
            int dealt = CombatHandler.applyDamage(p(), devil, 3);
            if (dealt <= 0) {
                throw failure("a smoke devil task still couldn't hurt a smoke devil in the dungeon");
            }
            return "off task 0, on task " + dealt;
        } finally {
            devil.discard();
        }
    }

    // ------------------------------------------------------------------ task-only areas (round 44)

    /** One task-only area to walk: a camp inside it and the task its guardian asks for. */
    record TaskOnlySpot(String dungeon, String task, Dungeons.Camp camp) {
    }

    /** One camp per task-only area kind (dungeon and task), the Stronghold Slayer Cave's hellhounds for the whole cave. */
    static List<TaskOnlySpot> taskOnlySpots() {
        List<TaskOnlySpot> out = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Dungeons.Camp cp : Dungeons.camps()) {
            Dungeons.TaskOnlyArea a = Dungeons.taskOnlyArea(cp.centreX(), cp.feetY() + 1, cp.centreZ());
            if (a == null || cp.creatures().isEmpty() || cp.creatures().get(0).startsWith("minecraft:")) {
                continue;
            }
            boolean whole = Dungeons.TASK_ONLY.containsKey(a.dungeon());
            if (whole && !a.dungeon().equals("dng_stronghold_slayer_cave")) {
                continue; // the older task-only dungeons have their own checks
            }
            String task = Dungeons.ANY_TASK.equals(a.task()) ? categoryOf(cp.creatures().get(0)) : a.task();
            if (task != null && seen.add(a.dungeon() + ":" + (whole ? "" : task))) {
                out.add(new TaskOnlySpot(a.dungeon(), task, cp));
            }
        }
        return out;
    }

    private static String categoryOf(String creature) {
        MonsterDef m = com.runecraft.osrs.content.ContentRegistry.monster(creature);
        return m == null ? null : SlayerTasks.categoryOf(m);
    }

    /**
     * A master who assigns {@code task}: Krystilia for a Wilderness area, else any other, Konar (sending the player to this
     * very place) only when no one else does (hydras).
     */
    private static SlayerMasters.Master masterFor(String task, boolean wilderness) {
        SlayerMasters.Master krystilia = null;
        SlayerMasters.Master konar = null;
        for (SlayerMasters.Master m : SlayerMasters.all()) {
            if (m.assignment(task) == null) {
                continue;
            }
            if (m.locationRestricted()) {
                konar = m;
            } else if (m.wilderness()) {
                krystilia = m;
            } else if (!wilderness) {
                return m;
            }
        }
        return wilderness ? krystilia : konar;
    }

    /**
     * In a task-only area, as a player would be: off task the guardian stops every hit (no damage on any path); a task
     * from a master who assigns it lets the player hurt the creature, and a kill there counts.
     */
    private String taskOnlyArea(TaskOnlySpot spot) {
        Dungeons.Camp cp = spot.camp();
        String creature = cp.creatures().get(0);
        EntityType<?> type = ModEntities.typeById(creature);
        if (type == null) {
            throw new SelfTest.Skip("no " + creature + " entity");
        }
        BlockPos at = new BlockPos(cp.centreX(), cp.feetY(), cp.centreZ());
        Dungeons.TaskOnlyArea area = Dungeons.taskOnlyArea(at.getX(), at.getY() + 1, at.getZ());
        if (area == null) {
            throw failure(creature + " camp at " + at.toShortString() + " is not task-only");
        }
        StructurePlacer.applyAround(c.world, at.getX(), at.getZ(), 6);
        p().teleportTo(c.world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p().getYRot(), p().getXRot());
        ChunkSettle.holdAndSettle(c.world, at, 16);
        SlayerState.Profile prof = profile();
        LivingEntity mob = spawnAt(type, at.offset(1, 0, 0), true);
        try {
            prof.task = null;
            prof.remaining = 0;
            float hp = mob.getHealth();
            if (CombatHandler.applyDamage(p(), mob, 3) != 0 || mob.getHealth() < hp) {
                throw failure("a player without a task hurt a " + creature + " in " + area.dungeon() + " (" + area.guardian() + ")");
            }
            String said = com.runecraft.content.dungeons.DungeonsHooks.taskOnlyMessage(area);
            if (area.guardian() == null || !said.startsWith(area.guardian())) {
                throw failure("no Slayer guardian speaks for " + area.dungeon() + ": " + said);
            }
            boolean wild = com.runecraft.world.Gielinor.wildernessLevel(mob) > 0;
            SlayerMasters.Master master = masterFor(spot.task(), wild);
            if (master == null) {
                throw failure("no Slayer master assigns " + spot.task());
            }
            SlayerMasters.Assignment line = master.assignment(spot.task());
            prof.task = spot.task();
            prof.amount = line.min();
            prof.remaining = line.min();
            prof.master = master.id();
            prof.region = master.locationRestricted() ? konarPlaceAt(spot.task(), at) : null;
            int dealt = CombatHandler.applyDamage(p(), mob, 3);
            if (dealt <= 0) {
                throw failure("a " + spot.task() + " task from " + master.name() + " still couldn't hurt a " + creature);
            }
            double xp = slayerXp();
            int before = prof.remaining;
            killAsPlayer(mob);
            if (prof.remaining != before - 1 || slayerXp() <= xp) {
                throw failure("a " + creature + " kill in the task-only area took the task from " + before + " to " + prof.remaining
                        + " (+" + (slayerXp() - xp) + " XP)");
            }
            return area.guardian() + " refused the off-task hit; " + master.name() + "'s " + spot.task() + " task: hit " + dealt
                    + ", kill " + before + " -> " + prof.remaining;
        } finally {
            if (mob.isAlive()) {
                mob.discard();
            }
            prof.task = null;
            prof.remaining = 0;
            c.home();
        }
    }

    // ------------------------------------------------------------------ rewards (round 44)

    /** Task Storage bought with points: store a task, take another, swap them back, unstore once nothing is current. */
    private String taskStorage() {
        c.completeQuests(null);
        SlayerState.Profile prof = profile();
        prof.unlocks.remove(com.runecraft.osrs.content.slayer.SlayerRewards.TASK_STORAGE);
        prof.storedTask = null;
        prof.storedRemaining = 0;
        prof.task = "hellhounds";
        prof.amount = 40;
        prof.remaining = 33;
        prof.master = "vannaka";
        if (SlayerService.storeTask(p()) || prof.hasStoredTask()) {
            throw failure("a task was stored without the Task Storage unlock");
        }
        prof.points = 520;
        slayerCommand("unlock " + com.runecraft.osrs.content.slayer.SlayerRewards.TASK_STORAGE);
        if (!prof.unlocks.contains(com.runecraft.osrs.content.slayer.SlayerRewards.TASK_STORAGE) || prof.points != 20) {
            throw failure("unlocking Task Storage left " + prof.points + " points (want 20)");
        }
        slayerCommand("store");
        if (prof.hasTask() || !"hellhounds".equals(prof.storedTask) || prof.storedRemaining != 33) {
            throw failure("storing the task left task " + prof.task + "/" + prof.remaining + ", stored " + prof.storedTask + "/"
                    + prof.storedRemaining);
        }
        // A new task from a master while one is stored; storing again is refused (one stored task at a time).
        SlayerService.talk(p(), SlayerMasters.get("vannaka"));
        if (!prof.hasTask()) {
            throw failure("Vannaka gave no task while one was stored");
        }
        String second = prof.task;
        int secondLeft = prof.remaining;
        if (SlayerService.storeTask(p())) {
            throw failure("a second task was stored while one already was");
        }
        slayerCommand("unstore");
        if (!"hellhounds".equals(prof.task) || prof.remaining != 33 || !second.equals(prof.storedTask)
                || prof.storedRemaining != secondLeft) {
            throw failure("swapping left task " + prof.task + "/" + prof.remaining + ", stored " + prof.storedTask);
        }
        // Finish the current one, then take the stored one up again.
        prof.task = null;
        prof.remaining = 0;
        slayerCommand("unstore");
        if (!second.equals(prof.task) || prof.remaining != secondLeft || prof.hasStoredTask()) {
            throw failure("unstoring left task " + prof.task + "/" + prof.remaining + ", stored " + prof.storedTask);
        }
        prof.task = null;
        prof.remaining = 0;
        return "stored 33 hellhounds, took " + secondLeft + " " + second + ", swapped and unstored";
    }

    /** Duly Noted notes a mithril dragon's bars only on task; the rune pouch holds runes that spells then use. */
    private String dulyNotedAndPouch() {
        SlayerState.Profile prof = profile();
        StringBuilder note = new StringBuilder();
        EntityType<?> dragonType = ModEntities.typeById("mithril_dragon");
        if (dragonType != null) {
            LivingEntity dragon = spawnAt(dragonType, c.near(4, 0), true);
            try {
                prof.unlocks.add(com.runecraft.osrs.content.slayer.SlayerRewards.DULY_NOTED);
                prof.task = "metal_dragons";
                prof.remaining = 30;
                prof.master = "nieve";
                prof.region = null;
                ItemStack onTask = com.runecraft.combat.CombatHooks.dropStack(p(), dragon, "mithril_bar", 3);
                ItemStack other = com.runecraft.combat.CombatHooks.dropStack(p(), dragon, "rune_battleaxe", 1);
                prof.task = "black_demons";
                ItemStack offTask = com.runecraft.combat.CombatHooks.dropStack(p(), dragon, "mithril_bar", 3);
                if (!com.runecraft.bank.Notes.isNote(onTask) || onTask.getCount() != 3) {
                    throw failure("Duly Noted didn't note a mithril dragon's 3 bars on task: " + onTask);
                }
                if (com.runecraft.bank.Notes.isNote(other) || com.runecraft.bank.Notes.isNote(offTask)) {
                    throw failure("Duly Noted noted another drop or an off-task kill's bars");
                }
                note.append("mithril bars noted on task only");
            } finally {
                dragon.discard();
                prof.task = null;
                prof.remaining = 0;
            }
        }
        // The rune pouch: 750 points, one only; runes stored in it pay for a spell.
        prof.points = 1600;
        slayerCommand("buy rune_pouch");
        slayerCommand("buy rune_pouch");
        if (!Inv.has(p(), "rune_pouch", 1) || Inv.count(p(), "rune_pouch") != 1 || prof.points != 850) {
            throw failure("buying the rune pouch twice left " + Inv.count(p(), "rune_pouch") + " pouches and " + prof.points
                    + " points (want 1 and 850)");
        }
        Inv.remove(p(), "air_rune", Inv.count(p(), "air_rune"));
        Inv.remove(p(), "mind_rune", Inv.count(p(), "mind_rune"));
        Inv.give(p(), "air_rune", 50);
        Inv.give(p(), "mind_rune", 3);
        slayerCommand("pouch deposit air_rune");
        if (Inv.count(p(), "air_rune") != 0 || com.runecraft.content.slayer.SlayerItemService.pouchCount(p(), "air_rune") != 50) {
            throw failure("depositing 50 air runes left " + Inv.count(p(), "air_rune") + " carried and "
                    + com.runecraft.content.slayer.SlayerItemService.pouchCount(p(), "air_rune") + " in the pouch");
        }
        java.util.Map<String, Integer> strike = java.util.Map.of("air_rune", 1, "mind_rune", 1);
        if (!com.runecraft.content.systems.SpellService.payRunes(p(), strike)) {
            throw failure("Wind Strike's runes weren't paid from the rune pouch");
        }
        if (com.runecraft.content.slayer.SlayerItemService.pouchCount(p(), "air_rune") != 49 || Inv.count(p(), "mind_rune") != 2) {
            throw failure("a cast took the wrong runes: pouch air " + com.runecraft.content.slayer.SlayerItemService.pouchCount(p(), "air_rune")
                    + ", mind " + Inv.count(p(), "mind_rune"));
        }
        slayerCommand("pouch withdraw air_rune 49");
        Inv.remove(p(), "air_rune", 49);
        Inv.remove(p(), "mind_rune", 2);
        Inv.remove(p(), "rune_pouch", 1);
        note.append(note.length() > 0 ? "; " : "").append("rune pouch bought once (850 points left), Wind Strike paid from it");
        // A slayer helmet comes apart into its six components.
        Inv.give(p(), "slayer_helmet", 1);
        slayerCommand("disassemble slayer_helmet");
        boolean ok = !Inv.has(p(), "slayer_helmet", 1) && Inv.has(p(), "black_mask", 1) && Inv.has(p(), "enchanted_gem", 1);
        for (String part : com.runecraft.osrs.content.slayer.SlayerRewards.HELMET_PARTS) {
            Inv.remove(p(), part, 1);
        }
        if (!ok) {
            throw failure("disassembling a slayer helmet didn't give back its components");
        }
        return note.append("; slayer helmet disassembled").toString();
    }

    // ------------------------------------------------------------------ protective gear

    private String protectiveGear() {
        StringBuilder note = new StringBuilder();
        SlayerGear.Protection banshee = SlayerGear.protection("banshees");
        if (SlayerCombat.isProtected(p(), banshee)) {
            throw failure("a bare head counts as earmuffs");
        }
        // The banshee's scream drains the combat stats of an unprotected player.
        LivingEntity mob = spawn("banshee");
        try {
            if (!(mob instanceof OsrsMonsterEntity monster)) {
                throw failure("the banshee is not an OSRS monster");
            }
            int before = data().skills().boost(Skill.ATTACK);
            SlayerCombat.unprotectedHit(p(), monster, banshee);
            if (data().skills().boost(Skill.ATTACK) >= before) {
                throw failure("an unprotected banshee scream didn't drain Attack");
            }
            note.append("scream drained Attack ").append(data().skills().boost(Skill.ATTACK) - before);
        } finally {
            mob.discard();
        }
        for (String head : List.of("earmuffs", "slayer_helmet", "black_slayer_helmet_i")) {
            p().setItemSlot(EquipmentSlot.HEAD, Inv.stack(head, 1));
            if (!SlayerCombat.isProtected(p(), banshee)) {
                throw failure(head + " doesn't protect from banshees");
            }
        }
        p().setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        // The witchwood icon must be worn (neck slot), not carried; slayer gloves likewise in the hands slot.
        SlayerGear.Protection horror = SlayerGear.protection("cave_horrors");
        SysState.SysPlayer sp = SysState.of(p());
        try {
            Inv.give(p(), "witchwood_icon", 1);
            if (SlayerCombat.isProtected(p(), horror)) {
                throw failure("a witchwood icon in the pack protected from cave horrors");
            }
            sp.worn.put(Jewellery.Slot.NECK, Inv.stack("witchwood_icon", 1));
            if (!SlayerCombat.isProtected(p(), horror)) {
                throw failure("a worn witchwood icon didn't protect from cave horrors");
            }
            sp.worn.put(Jewellery.Slot.HANDS, Inv.stack("slayer_gloves", 1));
            if (!SlayerCombat.isProtected(p(), SlayerGear.protection("fever_spiders"))) {
                throw failure("worn slayer gloves didn't protect from fever spiders");
            }
        } finally {
            sp.worn.remove(Jewellery.Slot.NECK);
            sp.worn.remove(Jewellery.Slot.HANDS);
        }
        // Mirror shield for basilisks.
        SlayerGear.Protection basilisk = SlayerGear.protection("basilisks");
        if (SlayerCombat.isProtected(p(), basilisk)) {
            throw failure("no shield protected from a basilisk's gaze");
        }
        p().setItemSlot(EquipmentSlot.OFFHAND, Inv.stack("mirror_shield", 1));
        boolean shielded = SlayerCombat.isProtected(p(), basilisk);
        p().setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        if (!shielded) {
            throw failure("a mirror shield didn't protect from a basilisk's gaze");
        }
        return note.append("; earmuffs, helmets, worn icon/gloves, mirror shield protect").toString();
    }

    // ------------------------------------------------------------------ leaf-bladed weapons and finishers

    private String weaponsAndFinishers() {
        LivingEntity kurask = spawn("kurask");
        LivingEntity gargoyle = spawn("gargoyle");
        try {
            p().setItemSlot(EquipmentSlot.MAINHAND, Inv.stack("abyssal_whip", 1));
            if (SlayerCombat.afterRoll(p(), kurask, AttackType.SLASH, 10) != 0) {
                throw failure("an abyssal whip hurt a kurask");
            }
            p().setItemSlot(EquipmentSlot.MAINHAND, Inv.stack("leaf_bladed_sword", 1));
            if (SlayerCombat.afterRoll(p(), kurask, AttackType.SLASH, 10) != 10) {
                throw failure("a leaf-bladed sword didn't hurt a kurask");
            }
            p().setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            // A gargoyle can't be killed by the hit itself: it needs the rock hammer (or Gargoyle smasher with a hammer).
            gargoyle.setHealth(5f);
            int left = SlayerCombat.afterRoll(p(), gargoyle, AttackType.CRUSH, 20);
            if (left >= 5) {
                throw failure("a lethal hit on a gargoyle without a rock hammer did " + left + " of its 5 hitpoints");
            }
            profile().unlocks.add("gargoyle_smasher");
            Inv.give(p(), "rock_hammer", 1);
            if (SlayerCombat.afterRoll(p(), gargoyle, AttackType.CRUSH, 20) != 20) {
                throw failure("Gargoyle smasher with a rock hammer didn't finish the gargoyle");
            }
            // A molanisk clings to the wall until a slayer bell is rung.
            LivingEntity molanisk = spawn("molanisk");
            try {
                if (SlayerCombat.afterRoll(p(), molanisk, AttackType.CRUSH, 6) != 0) {
                    throw failure("a molanisk was hurt before a slayer bell lured it down");
                }
                if (SlayerCombat.ringBell(p()) < 1 || SlayerCombat.afterRoll(p(), molanisk, AttackType.CRUSH, 6) != 6) {
                    throw failure("ringing a slayer bell didn't lure the molanisk down");
                }
            } finally {
                molanisk.discard();
            }
            return "whip 0 / leaf-bladed 10 on a kurask; gargoyle left at " + (5 - left) + " hp until smashed; molanisk belled";
        } finally {
            kurask.discard();
            gargoyle.discard();
        }
    }

    // ------------------------------------------------------------------ Nightmare Zone

    /**
     * Nightmare Zone kills and Slayer (OSRS Wiki "Nightmare Zone", Slayer training): a dream boss on the wiki's table counts
     * for its task with the table's Slayer XP (Dad: trolls, 126), a boss that isn't on it doesn't (the Bouncer is no hellhound),
     * and a practice dream gives no credit at all.
     */
    private String nmzCredit() {
        c.completeQuests(null);
        EntityType<?> dad = ModEntities.typeById("nmz_dad");
        EntityType<?> bouncer = ModEntities.typeById("nmz_bouncer");
        if (dad == null || bouncer == null) {
            throw failure("no Nightmare Zone boss entities");
        }
        SlayerState.Profile prof = profile();
        prof.task = "trolls";
        prof.amount = 10;
        prof.remaining = 10;
        double xp = slayerXp();
        com.runecraft.content.minigames.MinigameSession s = com.runecraft.content.minigames.MinigameManager.selfTestStart(
                com.runecraft.osrs.content.minigames.Minigame.NIGHTMARE_ZONE,
                com.runecraft.osrs.content.minigames.Minigame.NIGHTMARE_ZONE.entranceBlock(), p());
        try {
            killAsPlayer(spawnAt(dad, p().blockPosition().offset(2, 0, 0), true));
            if (prof.remaining != 9) {
                throw failure("Dad killed in a rumble didn't count for the troll task (" + prof.remaining + " left)");
            }
            if (Math.abs(slayerXp() - xp - 126) > 0.05) {
                throw failure("Dad gave " + (slayerXp() - xp) + " Slayer XP in a normal dream, not the wiki's 126");
            }
            prof.task = "hellhounds";
            prof.remaining = 10;
            killAsPlayer(spawnAt(bouncer, p().blockPosition().offset(2, 0, 0), true));
            if (prof.remaining != 10) {
                throw failure("the Bouncer counted for a hellhound task");
            }
        } finally {
            s.end(false);
        }
        prof.task = "trolls";
        prof.remaining = 10;
        com.runecraft.content.minigames.MinigameSession practice = com.runecraft.content.minigames.MinigameManager.selfTestStart(
                com.runecraft.osrs.content.minigames.Minigame.NIGHTMARE_ZONE,
                new com.runecraft.osrs.content.minigames.MinigameRules.NightmareZone.Dream(
                        com.runecraft.osrs.content.minigames.MinigameRules.NightmareZone.Mode.PRACTICE, false, "nmz_dad").variant(), p());
        try {
            killAsPlayer(spawnAt(dad, p().blockPosition().offset(2, 0, 0), true));
            if (prof.remaining != 10) {
                throw failure("a practice dream kill counted for the task");
            }
        } finally {
            practice.end(false);
        }
        c.home();
        return "rumble Dad 10 -> 9 (+126 Slayer XP), Bouncer no hellhound credit, practice no credit";
    }

    // ------------------------------------------------------------------ Karuulm hot floor

    private String hotFloor() {
        Dungeons.Camp cp = camp(SlayerGear.HOT_FLOOR_DUNGEON, "wyrm");
        BlockPos at = new BlockPos(cp.centreX(), cp.feetY(), cp.centreZ());
        StructurePlacer.applyAround(c.world, at.getX(), at.getZ(), 4);
        try {
            p().teleportTo(c.world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p().getYRot(), p().getXRot());
            int bare = SlayerService.hotFloorDamage(p());
            p().setItemSlot(EquipmentSlot.FEET, Inv.stack("boots_of_stone", 1));
            int stone = SlayerService.hotFloorDamage(p());
            p().setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
            if (bare != SlayerGear.HOT_FLOOR_DAMAGE || stone != 0) {
                throw failure("the Karuulm floor burned " + bare + " barefoot and " + stone + " in boots of stone (want 4 and 0)");
            }
            c.home();
            if (SlayerService.hotFloorDamage(p()) != 0) {
                throw failure("the floor burns outside the Karuulm Slayer Dungeon");
            }
            return "barefoot 4 per tick, boots of stone 0, outside 0";
        } finally {
            c.home();
        }
    }
}
