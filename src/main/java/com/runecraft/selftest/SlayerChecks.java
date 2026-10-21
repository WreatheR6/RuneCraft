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
        return note.toString();
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
