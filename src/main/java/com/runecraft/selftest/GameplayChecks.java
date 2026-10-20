package com.runecraft.selftest;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.MonsterStats;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.content.quests.QuestInterpreter;
import com.runecraft.content.quests.QuestsState;
import com.runecraft.content.raids.RaidInstance;
import com.runecraft.content.raids.RaidManager;
import com.runecraft.entity.BossEntity;
import com.runecraft.entity.BossService;
import com.runecraft.menu.MenuService;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Quest;
import com.runecraft.osrs.QuestInfo;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Recipes;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.Spell;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.godwars.GwdBosses;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.quests.QuestBook;
import com.runecraft.osrs.content.quests.QuestSpec;
import com.runecraft.osrs.content.quests.QuestSpec.Trigger;
import com.runecraft.osrs.content.quests.QuestWalk;
import com.runecraft.osrs.content.raids.RaidType;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.osrs.content.systems.Teleports;
import com.runecraft.osrs.data.Boss;
import com.runecraft.player.PlayerData;
import com.runecraft.player.PlayerDataState;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.quest.QuestManager;
import com.runecraft.registry.ModEntities;
import com.runecraft.util.Inv;
import com.runecraft.world.Gielinor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** The self-test's gameplay checks. Each check starts from {@link TestContext#reset()}. */
final class GameplayChecks {
    /** Game ticks each minigame session runs for. */
    private static final int MINIGAME_TICKS = 300;
    /** Game ticks each raid room runs for. */
    private static final int RAID_ROOM_TICKS = 40;
    /** Minecraft ticks each boss lives before it is killed. */
    private static final int BOSS_TICKS = 200;
    /** Recipes tried per menu before the "make" check gives up. */
    private static final int RECIPE_CANDIDATES = 8;

    private final TestContext c;

    private GameplayChecks(TestContext c) {
        this.c = c;
    }

    static List<SelfTest.Check> all(TestContext context) {
        GameplayChecks g = new GameplayChecks(context);
        List<SelfTest.Check> checks = new ArrayList<>();
        checks.add(new SelfTest.Check("saveload:playerdata", g::saveLoad));
        for (MenuType type : MenuType.values()) {
            checks.add(new SelfTest.Check("menu:" + type.name().toLowerCase(Locale.ROOT), () -> g.openMenu(type)));
        }
        for (MenuType type : MenuType.values()) {
            checks.add(new SelfTest.Check("make:" + type.name().toLowerCase(Locale.ROOT), () -> g.make(type)));
        }
        Map<String, QuestSpec> specs = new HashMap<>();
        for (QuestSpec q : QuestBook.get().quests) {
            specs.put(q.id(), q);
        }
        for (QuestInfo q : ContentRegistry.quests()) {
            checks.add(new SelfTest.Check("quest:" + q.questId(), () -> g.quest(q, specs.get(q.questId()))));
        }
        checks.add(new SelfTest.Check("quest:puzzle_retry", g::puzzleRetry));
        for (Minigame m : Minigame.values()) {
            checks.add(new SelfTest.Check("minigame:" + m.id(), () -> g.minigame(m)));
        }
        checks.addAll(MinigameRewardChecks.all(context));
        checks.add(new SelfTest.Check("minigame:fight_caves_run", () -> g.tzhaarRun(
                com.runecraft.content.minigames.games.TzHaarSelfTest::fightCaveRun)));
        checks.add(new SelfTest.Check("minigame:inferno_run", () -> g.tzhaarRun(
                com.runecraft.content.minigames.games.TzHaarSelfTest::infernoRun)));
        for (RaidType r : RaidType.values()) {
            checks.add(new SelfTest.Check("raid:" + r.id(), () -> g.raid(r)));
        }
        checks.add(new SelfTest.Check("raid:cox_walk", () -> g.walk(com.runecraft.content.raids.CoxTobWalk::coxRun)));
        checks.add(new SelfTest.Check("raid:tob_walk", () -> g.walk(com.runecraft.content.raids.CoxTobWalk::tobRun)));
        checks.add(new SelfTest.Check("raid:tob_wipe", () -> g.walk(com.runecraft.content.raids.CoxTobWalk::tobWipe)));
        checks.add(new SelfTest.Check("raid:cox_supplies", g::coxSupplies));
        checks.add(new SelfTest.Check("raid:toa_full_run", g::toaFullRun));
        checks.add(new SelfTest.Check("raid:toa_deaths", g::toaDeaths));
        for (Boss b : ModEntities.BOSS_TYPES.keySet()) {
            checks.add(new SelfTest.Check("boss:" + b.entityId(), () -> g.legacyBoss(b)));
        }
        for (String id : ModEntities.DATA_BOSS_TYPES.keySet()) {
            checks.add(new SelfTest.Check("boss:" + id, () -> g.dataBoss(id)));
        }
        checks.add(new SelfTest.Check("boss:bryophyta_growthlings", g::bryophytaGrowthlings));
        checks.add(new SelfTest.Check("boss:obor_knockback", g::oborKnockback));
        checks.add(new SelfTest.Check("boss:araxxor_eggs",
                () -> g.scripted("araxxor", com.runecraft.content.bosses.BossSelfTests::araxxorEggs)));
        checks.add(new SelfTest.Check("boss:sire_spawns",
                () -> g.scripted("abyssal_sire", com.runecraft.content.bosses.BossSelfTests::sireSpawns)));
        checks.add(new SelfTest.Check("boss:kraken_whirlpools",
                () -> g.scripted("kraken", com.runecraft.content.bosses.BossSelfTests::krakenWhirlpools)));
        checks.add(new SelfTest.Check("boss:eclipse_clones",
                () -> g.scripted("eclipse_moon", com.runecraft.content.bosses.BossSelfTests::eclipseClones)));
        checks.add(new SelfTest.Check("boss:zalcano_tephra",
                () -> g.scripted("zalcano", com.runecraft.content.bosses.BossSelfTests::zalcanoTephra)));
        checks.add(new SelfTest.Check("boss:yama_dossier",
                () -> g.scripted("yama", com.runecraft.content.bosses.BossSelfTests::yamaDossier)));
        checks.add(new SelfTest.Check("boss:hueycoatl_phases",
                () -> g.scripted("the_hueycoatl", com.runecraft.content.bosses.BossSelfTests::hueycoatlPhases)));
        checks.addAll(BossFightChecks.all(context));
        // The God Wars Dungeon walked end to end: the hole, kill count, each general's chamber, the prison and Nex.
        checks.add(new SelfTest.Check("boss:gwd_walk", () -> g.walk(com.runecraft.content.godwars.GwdWalk::run)));
        for (Map.Entry<String, int[]> e : destinations().entrySet()) {
            checks.add(new SelfTest.Check("teleport:" + e.getKey(), () -> g.teleport(e.getValue()[0], e.getValue()[1])));
        }
        for (String npcId : com.runecraft.osrs.content.dungeons.EssenceMine.TELEPORTERS.keySet()) {
            checks.add(new SelfTest.Check("essencemine:" + npcId.toLowerCase(Locale.ROOT), () -> g.essenceMine(npcId)));
        }
        checks.add(new SelfTest.Check("transport:kbd_levers", g::kbdLevers));
        for (String code : new String[]{"BKQ", "DIP", "DLS", "BLQ"}) {
            checks.add(new SelfTest.Check("transport:fairy_ring_" + code.toLowerCase(Locale.ROOT), () -> g.fairyRingRealm(code)));
        }
        // Items, combat and interactions (after the teleports, which generate the spell destinations' chunks).
        checks.addAll(ItemCombatChecks.all(context));
        checks.addAll(CannonChecks.all(context));
        checks.addAll(DungeonWorldChecks.all(context));
        checks.addAll(SlayerChecks.all(context));
        checks.addAll(SeaChecks.all(context));
        checks.addAll(SkillChecks.all(context));
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

    // ------------------------------------------------------------------ save / load

    private String saveLoad() {
        PlayerData data = data();
        data.setCoins(1_234_567);
        data.setQuestPoints(42);
        data.incrementKillCount("selftest_boss");
        data.collectionLog().add("abyssal_whip");
        data.setSlayerStreak(7);
        data.skills().setXpTenths(Skill.SLAYER, 1_000_000);
        net.minecraft.world.item.ItemStack bars = Inv.stack("bronze_bar", 10);
        if (!bars.isEmpty()) {
            com.runecraft.bank.BankService.depositStack(data.bank(), bars, 10, 0);
        }
        CompoundTag saved = data.toNbt();
        PlayerData loaded = new PlayerData();
        loaded.fromNbt(saved.copy(), c.server.registryAccess());
        CompoundTag again = loaded.toNbt();
        if (!saved.equals(again)) {
            throw failure("PlayerData changed after a save/load round trip:\n  saved  " + saved + "\n  loaded " + again);
        }
        // The whole world-save state, as written to data/runecraft.dat.
        CompoundTag state = Players.state(c.server).save(new CompoundTag(), c.server.registryAccess());
        PlayerDataState reread = PlayerDataState.fromNbt(state, c.server.registryAccess());
        CompoundTag mine = reread.get(p().getUUID()).toNbt();
        if (!saved.equals(mine)) {
            throw failure("PlayerDataState lost this player's data on a round trip:\n  saved  " + saved + "\n  loaded " + mine);
        }
        return saved.getAllKeys().size() + " keys";
    }

    // ------------------------------------------------------------------ menus and recipes

    private List<Recipe> candidates(MenuType type) {
        List<Recipe> list = new ArrayList<>(Recipes.menu(type));
        list.sort(Comparator.comparingInt(Recipe::level));
        return list;
    }

    private void giveFor(Recipe r, int times) {
        if (r.requiredTool() != null) {
            Inv.give(p(), r.requiredTool(), 1);
        }
        r.inputs().forEach((item, n) -> {
            if (!item.equals(Inv.COINS)) {
                Inv.give(p(), item, n * times);
            }
        });
        Inv.give(p(), Inv.COINS, 10_000_000);
    }

    private String openMenu(MenuType type) {
        List<Recipe> recipes = candidates(type);
        if (recipes.isEmpty()) {
            throw failure("no recipes use this menu");
        }
        c.completeQuests(null);
        giveFor(recipes.get(0), 1);
        MenuService.open(p(), type, c.near(1, 0), true);
        if (data().openMenu != type) {
            throw failure("MenuService.open offered nothing although the inputs of " + recipes.get(0).id() + " were in the inventory");
        }
        return recipes.size() + " recipes";
    }

    private String make(MenuType type) {
        List<Recipe> recipes = candidates(type);
        if (recipes.isEmpty()) {
            throw failure("no recipes use this menu");
        }
        c.completeQuests(null);
        List<String> tried = new ArrayList<>();
        for (Recipe r : recipes.subList(0, Math.min(RECIPE_CANDIDATES, recipes.size()))) {
            p().getInventory().clearContent();
            giveFor(r, 3);
            Map<String, Integer> before = new LinkedHashMap<>();
            r.outputs().keySet().forEach(item -> before.put(item, Inv.count(p(), item)));
            int xpBefore = r.skill() == null ? 0 : data().skills().xpTenths(r.skill());
            data().openMenu = null;
            MenuService.open(p(), type, c.near(1, 0), true);
            if (data().openMenu != type) {
                tried.add(r.id() + " (menu did not open)");
                continue;
            }
            MenuService.choose(p(), r.id(), 3);
            for (int i = 0; i < r.ticks() * 3 + 6 && ActionManager.tickNow(p()); i++) {
                // advance the Make-X action one game tick at a time
            }
            ActionManager.cancel(p());
            boolean made = false;
            for (Map.Entry<String, Integer> e : before.entrySet()) {
                if (Inv.count(p(), e.getKey()) > e.getValue()) {
                    made = true;
                }
            }
            int xpGained = r.skill() == null ? 0 : data().skills().xpTenths(r.skill()) - xpBefore;
            if (made) {
                if (r.skill() != null && r.xpTenths() > 0 && xpGained <= 0) {
                    throw failure(r.id() + " was made but gave no " + r.skill().displayName() + " XP");
                }
                return "made " + r.id() + (xpGained > 0 ? ", +" + xpGained / 10.0 + " xp" : "");
            }
            tried.add(r.id() + (xpGained > 0 ? " (xp but no output)" : ""));
        }
        throw failure("no recipe could be made; tried " + String.join(", ", tried));
    }

    // ------------------------------------------------------------------ quests

    /** Hands the player whatever a trigger needs and removes what would make it skip. */
    private void prepare(Trigger t) {
        if (t.emptyHanded) {
            QuestInterpreter.selfTestEmptyHands(p());
        }
        t.need.forEach((item, n) -> Inv.give(p(), item, n));
        t.take.forEach((item, n) -> Inv.give(p(), item, n));
        t.tools.forEach(tool -> Inv.give(p(), QuestSpec.toolItem(tool), 1));
        QuestsState state = QuestsState.get(p());
        t.needFlags.forEach(f -> state.setFlag(p().getUUID(), f));
        // a flag that would skip the advancing trigger (another per-player route, Curse of the Empty Lord) is cleared
        t.unlessFlags.forEach(f -> state.clearFlag(p().getUUID(), f));
        for (String item : t.unlessItems) {
            Inv.remove(p(), item, 10_000);
        }
        if (t.kind == Trigger.Kind.DROP) {
            t.give.keySet().forEach(item -> Inv.remove(p(), item, 10_000));
        }
    }

    /** Why a directly fired trigger did nothing: the items it takes that the player still lacks. */
    private String whyNot(Trigger t) {
        List<String> missing = new ArrayList<>();
        t.take.forEach((item, n) -> {
            if (!Inv.has(p(), item, n)) {
                missing.add(n + " x " + item + (!item.equals(Inv.COINS) && Inv.stack(item, 1).isEmpty() ? " (not a registered item)" : ""));
            }
        });
        return missing.isEmpty() ? "" : ": its take failed, missing " + String.join(", ", missing);
    }

    /**
     * Reports {@code count} kills of {@code creature} through the real kill hooks (multi-kill objectives count each
     * one); false if it isn't a known entity.
     */
    private boolean kill(String creature, int count) {
        EntityType<?> type = ModEntities.typeById(creature);
        if (type == null) {
            return false;
        }
        for (int i = 0; i < Math.max(1, count); i++) {
            Entity e = type.create(c.world);
            if (!(e instanceof LivingEntity victim)) {
                return false;
            }
            victim.moveTo(p().getX() + 2, p().getY(), p().getZ(), 0f, 0f);
            QuestManager.onKill(p(), victim, MonsterStats.of(victim));
            victim.discard();
        }
        return true;
    }

    private String quest(QuestInfo q, QuestSpec spec) {
        String id = q.questId();
        c.completeQuests(q);
        PlayerData data = data();
        data.setQuestStage(q, 0);
        data.setQuestPoints(1000);
        int qpBefore = data.questPoints();
        Map<Skill, Integer> xpBefore = new HashMap<>();
        q.xpRewards().keySet().forEach(s -> xpBefore.put(s, data.skills().xpTenths(s)));
        String walk;
        if (spec == null) {
            // Hand-written quest (QuestManager dialogue on QuestNpcEntity): check the stage API only.
            QuestApi.setStage(p(), id, 1);
            if (QuestApi.stage(p(), id) != 1) {
                throw failure("setStage(1) did not stick");
            }
            walk = "scripted quest: stage API only";
        } else {
            int steps = 0;
            int forced = 0;
            String stuck = null;
            for (int guard = 0; guard < QuestWalk.maxSteps(spec) && !data.questComplete(q); guard++) {
                int s = data.questStage(q);
                if (s < 0 || s >= spec.stages.size()) {
                    stuck = "stage " + s + " is outside the " + spec.stages.size() + " stages";
                    break;
                }
                Trigger t = QuestWalk.advancing(spec.stages.get(s));
                if (t == null) {
                    stuck = "stage " + s + " has no trigger that moves it on";
                    break;
                }
                prepare(t);
                boolean handled = true;
                switch (t.kind) {
                    case TALK -> {
                        // A real player talks again after a once-only side dialogue (stay + flag) answered first.
                        for (int talk = 0; talk < 3 && data.questStage(q) == s && !data.questComplete(q); talk++) {
                            if (talk > 0) {
                                prepare(t);
                            }
                            QuestInterpreter.selfTestTalk(p(), t.target);
                        }
                    }
                    case KILL -> handled = kill(t.target, t.count);
                    case PUZZLE -> {
                        // Click the player's own room's solution through the real chat-link handler; a fight room
                        // (Recruitment Drive's Sir Leye) then needs its foe killed.
                        String foe = com.runecraft.content.quests.QuestPuzzles.selfTestSolve(p(), spec, t);
                        if (foe != null) {
                            handled = kill(foe, 1);
                        }
                    }
                    default -> QuestInterpreter.selfTestFire(p(), spec, t);
                }
                if (!handled || (data.questStage(q) == s && !data.questComplete(q))) {
                    // The dialogue picked another branch or a condition wasn't met: fire the objective directly.
                    prepare(t);
                    QuestInterpreter.selfTestFire(p(), spec, t);
                    if (data.questStage(q) == s && !data.questComplete(q)) {
                        stuck = "stage " + s + "/" + spec.stages.size() + " (" + QuestWalk.describe(t) + ") did not advance" + whyNot(t);
                        break;
                    }
                    forced++;
                }
                steps++;
            }
            if (stuck == null && !data.questComplete(q)) {
                stuck = "no completion after " + steps + " triggers (now at stage " + data.questStage(q) + ")";
            }
            if (stuck != null) {
                String allowed = QuestWalk.UNWALKABLE.get(id);
                if (allowed == null) {
                    throw failure("quest walk stuck after " + steps + " steps: " + stuck
                            + " (fix the quest, or list it in QuestWalk.UNWALKABLE with the reason)");
                }
                walk = "walked " + steps + "/" + spec.stages.size() + " then forced complete (unwalkable: " + allowed + "; " + stuck + ")";
            } else {
                walk = "walked " + steps + " steps" + (forced > 0 ? " (" + forced + " fired directly)" : "");
            }
        }
        boolean natural = data.questComplete(q);
        if (!natural) {
            p().getInventory().clearContent();
            QuestApi.complete(p(), id);
        }
        if (!data.questComplete(q)) {
            throw failure("complete() did not mark the quest complete");
        }
        int qp = data.questPoints() - qpBefore;
        if (qp != q.questPoints()) {
            throw failure("completing gave " + qp + " quest points, expected " + q.questPoints());
        }
        for (Map.Entry<Skill, Double> e : q.xpRewards().entrySet()) {
            Skill s = e.getKey();
            if (e.getValue() > 0 && data.skills().xpTenths(s) <= xpBefore.get(s)) {
                throw failure("no " + s.displayName() + " XP reward");
            }
        }
        if (!natural) {
            for (String reward : q.itemRewards()) {
                String item = reward.split(":")[0];
                if (!Inv.has(p(), item, 1) && Inv.stack(item, 1).isEmpty()) {
                    throw failure("reward item " + item + " does not exist");
                }
            }
        }
        // A completed quest must survive a save/load.
        PlayerData loaded = new PlayerData();
        loaded.fromNbt(data.toNbt(), c.server.registryAccess());
        if (!loaded.questComplete(q)) {
            throw failure("completion lost on save/load");
        }
        return walk;
    }

    /**
     * Recruitment Drive's test rooms through the puzzle chat-link handler: a wrong answer sends the player back and
     * re-draws the rooms (attempt counter), the right answer passes the room.
     */
    private String puzzleRetry() {
        QuestSpec rd = QuestBook.get().spec("recruitment_drive");
        QuestInfo q = ContentRegistry.quest("recruitment_drive");
        if (rd == null || q == null) {
            throw failure("recruitment_drive is missing");
        }
        c.completeQuests(q);
        QuestInterpreter.selfTestEmptyHands(p()); // the testing grounds take nothing in
        QuestsState state = QuestsState.get(p());
        Trigger first = rd.stages.get(1).triggers.stream().filter(t -> t.kind == Trigger.Kind.PUZZLE && t.slot == 0).findFirst()
                .orElseThrow(() -> failure("no first test room"));
        first.puzzle.failFlags.forEach(f -> state.clearFlag(p().getUUID(), f));
        String attempts = "puzzle:" + first.puzzle.key;
        try {
            data().setQuestStage(q, 1);
            int before = state.kills(p().getUUID(), attempts);
            if (!com.runecraft.content.quests.QuestPuzzles.selfTestFail(p(), rd, first)) {
                throw failure("a wrong answer did not reset the room");
            }
            if (state.kills(p().getUUID(), attempts) != before + 1 || data().questStage(q) != first.puzzle.failStage) {
                throw failure("a wrong answer did not re-draw and send the player back (stage " + data().questStage(q) + ")");
            }
            String foe = com.runecraft.content.quests.QuestPuzzles.selfTestSolve(p(), rd, first);
            if (foe != null) {
                kill(foe, 1);
            }
            if (!state.hasFlag(p().getUUID(), "rd_test0")) {
                throw failure("solving the first room did not pass it" + (foe != null ? " (fight room: " + foe + ")" : ""));
            }
            return "wrong answer re-drew (attempt " + (before + 1) + "), first room passed" + (foe != null ? " by killing " + foe : "");
        } finally {
            first.puzzle.failFlags.forEach(f -> state.clearFlag(p().getUUID(), f));
            data().setQuestStage(q, 0);
        }
    }

    // ------------------------------------------------------------------ minigames and raids

    private String minigame(Minigame m) {
        // Entry fees and tickets the real minigames ask for.
        if (m == Minigame.VOLCANIC_MINE) {
            Inv.give(p(), "numulite", com.runecraft.osrs.content.minigames.VolcanicMine.ENTRY_NUMULITE);
        } else if (m == Minigame.CHAMPIONS_CHALLENGE) {
            data().setQuestPoints(Math.max(data().questPoints(), com.runecraft.osrs.content.minigames.ChampionsChallenge.QUEST_POINTS));
            Inv.give(p(), "imp_champion_scroll", 1);
        }
        MinigameSession s = MinigameManager.selfTestStart(m, m.entranceBlock(), p());
        int ticks = 0;
        int online = s.online().size();
        while (ticks < MINIGAME_TICKS && !s.isEnded()) {
            MinigameManager.selfTestTick(s);
            c.heal();
            ticks++;
        }
        boolean endedItself = s.isEnded();
        s.end(true);
        if (MinigameManager.session(p()) != null) {
            throw failure("the player is still bound to a session after it ended");
        }
        return ticks + " game ticks, " + online + " online" + (endedItself ? ", ended by itself" : "");
    }

    private String raid(RaidType type) {
        RaidManager.enter(p(), type, c.near(0, 2));
        RaidInstance raid = RaidManager.of(p());
        if (raid == null) {
            throw failure("RaidManager.enter did not start a raid (see the log for \"Failed to start a raid\")");
        }
        int party = raid.players().size();
        int active = raid.activePlayers().size();
        int rooms = RaidManager.selfTestRun(raid, RAID_ROOM_TICKS);
        RaidManager.selfTestClose(raid);
        if (RaidManager.of(p()) != null) {
            throw failure("the player is still bound to the raid after it closed");
        }
        return raid.rooms.size() + " rooms, " + rooms + " started, party " + party + " online / " + active + " active";
    }

    /**
     * A whole Tombs of Amascut run as the real player (round 33): invocations, the barrier, the Nexus, every path's puzzle
     * solved and boss killed in a random order, the Helpful Spirit, the Wardens' three phases and the chest. Raid rooms
     * only see players in the server's player list, so the fake player can't do it.
     */
    private String toaFullRun() {
        if (!c.realPlayer()) {
            throw new SelfTest.Skip("raid rooms only see players in the player list; runs in '/rc selftest real raid:'");
        }
        try {
            return com.runecraft.content.raids.RaidSelfTest.toaRun(p(), c.near(0, 2));
        } catch (AssertionError e) {
            throw failure(e.getMessage());
        }
    }

    /**
     * Tombs of Amascut deaths: the death penalty, a wipe resetting the room with honey locusts, a Hardcore Run wipe; then
     * logging out mid-fight and back in (a death, rejoining downed, the solo wipe) and leaving through the lobby exit.
     */
    private String toaDeaths() {
        if (!c.realPlayer()) {
            throw new SelfTest.Skip("raid rooms only see players in the player list; runs in '/rc selftest real raid:'");
        }
        try {
            return com.runecraft.content.raids.RaidSelfTest.toaDeaths(p(), c.near(0, 2));
        } catch (AssertionError e) {
            throw failure(e.getMessage());
        }
    }

    /**
     * A whole Fight Cave or Inferno run as the real player (round 34, {@code TzHaarSelfTest}): minigame sessions only see
     * players in the server's player list, so the fake player can't do it.
     */
    private String tzhaarRun(java.util.function.Function<ServerPlayer, String> run) {
        if (!c.realPlayer()) {
            throw new SelfTest.Skip("minigame sessions only see players in the player list; runs in '/rc selftest real minigame:'");
        }
        try {
            return run.apply(p());
        } catch (AssertionError e) {
            throw failure(e.getMessage());
        }
    }

    /** CoX raid herblore through the real service: clean, brew, mix an overload and drink (round 29). */
    private String coxSupplies() {
        RaidManager.enter(p(), RaidType.COX, c.near(0, 2));
        RaidInstance raid = RaidManager.of(p());
        if (raid == null) {
            throw failure("RaidManager.enter did not start a raid");
        }
        try {
            return com.runecraft.content.raids.CoxSupplyService.selfTest(p());
        } catch (AssertionError e) {
            throw failure(e.getMessage());
        } finally {
            RaidManager.selfTestClose(raid);
        }
    }

    /**
     * A whole CoX or ToB run walked room by room as the real player (round 33, {@code CoxTobWalk}). Raid rooms only see
     * players in the server's player list, so the fake player can't do it.
     */
    private String walk(java.util.function.BiFunction<ServerPlayer, BlockPos, String> run) {
        if (!c.realPlayer()) {
            throw new SelfTest.Skip("raid rooms and the God Wars Dungeon only see players in the player list; runs in '/rc selftest real'");
        }
        try {
            return run.apply(p(), c.near(0, 2));
        } catch (AssertionError e) {
            throw failure(e.getMessage());
        }
    }

    // ------------------------------------------------------------------ bosses

    private String legacyBoss(Boss boss) {
        if (boss == Boss.COUNT_DRAYNOR) {
            data().setQuestStage(Quest.VAMPYRE_SLAYER, 3);
        } else if (boss == Boss.DELRITH) {
            data().setQuestStage(Quest.DEMON_SLAYER, 4);
        }
        if (boss.key() != null) {
            Inv.give(p(), boss.key(), 1);
        }
        BlockPos at = c.near(0, 0);
        BossService.summon(p(), c.world, at, boss);
        EntityType<BossEntity> type = ModEntities.BOSS_TYPES.get(boss);
        List<BossEntity> found = c.world.getEntities(type, new AABB(at).inflate(48), e -> e.isAlive());
        if (found.isEmpty()) {
            throw failure("BossService.summon spawned nothing");
        }
        return fight(found.get(0), boss.entityId());
    }

    /** Summons a legacy boss next to the platform (as {@link #legacyBoss}) and returns it. */
    private BossEntity summonLegacy(Boss boss) {
        if (boss.key() != null) {
            Inv.give(p(), boss.key(), 1);
        }
        BlockPos at = c.near(0, 0);
        BossService.summon(p(), c.world, at, boss);
        List<BossEntity> found = c.world.getEntities(ModEntities.BOSS_TYPES.get(boss), new AABB(at).inflate(48), e -> e.isAlive());
        if (found.isEmpty()) {
            throw failure("BossService.summon spawned nothing");
        }
        return found.get(0);
    }

    /** Bryophyta's growthlings: summoned three at a time, she is immune while they live (OldBossScripts). */
    private String bryophytaGrowthlings() {
        BossEntity boss = summonLegacy(Boss.BRYOPHYTA);
        try {
            return com.runecraft.content.bosses.OldBossScripts.selfTestGrowthlings(boss, p());
        } finally {
            boss.discard();
        }
    }

    /** Obor's knockback slam pushes the player away from him; his ranged attack has its own max hit. */
    private String oborKnockback() {
        BossEntity boss = summonLegacy(Boss.OBOR);
        try {
            p().teleportTo(boss.getX() + 2.5, boss.getY(), boss.getZ());
            return com.runecraft.content.bosses.OldBossScripts.selfTestKnockback(boss, p());
        } finally {
            boss.discard();
        }
    }

    /** Spawns a data boss next to the player, targeting them, runs one scripted-mechanic check on it and removes it. */
    private String scripted(String id, java.util.function.BiFunction<DataBossEntity, ServerPlayer, String> test) {
        BossDef def = ContentRegistry.boss(id);
        if (def == null) {
            throw failure("no BossDef " + id);
        }
        DataBossEntity boss = DataBossService.spawn(c.world, c.near(3, 0), def, p());
        if (boss == null) {
            throw failure("DataBossService.spawn returned null for " + id);
        }
        try {
            boss.setTarget(p());
            return test.apply(boss, p());
        } finally {
            boss.discard();
            c.heal();
        }
    }

    private String dataBoss(String id) {
        BossDef def = ContentRegistry.boss(id);
        if (def == null) {
            throw failure("no BossDef for a registered data boss type");
        }
        if (java.util.Arrays.asList(GwdBosses.NEX_MINIONS).contains(id)) {
            // NexMinionScript discards a general that has no Nex: they only exist in Nex's corners (see boss:nex).
            throw new SelfTest.Skip("no target: Nex's generals are spawned by Nex and vanish without her (covered by boss:nex)");
        }
        if (def.key != null) {
            Inv.give(p(), def.key, 1);
        }
        BlockPos at = c.near(0, 0);
        EntityType<DataBossEntity> type = ModEntities.DATA_BOSS_TYPES.get(id);
        DataBossService.summon(p(), c.world, at, def);
        List<DataBossEntity> found = c.world.getEntities(type, new AABB(at).inflate(48), e -> e.isAlive());
        String how = "summoned";
        if (found.isEmpty()) {
            // A requirement (quest stage, Slayer task...) refused the summon: spawn it directly.
            DataBossEntity spawned = DataBossService.spawn(c.world, c.near(3, 0), def, p());
            if (spawned == null) {
                throw failure("DataBossService.spawn returned null");
            }
            found = List.of(spawned);
            how = "spawned directly (summon refused)";
        }
        return how + ", " + fight(found.get(0), id);
    }

    /** Boss entities (data-driven or legacy) alive around the test platform. */
    private List<LivingEntity> bossesAround() {
        AABB box = new AABB(TestContext.PLATFORM).inflate(48);
        return c.world.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && (e instanceof DataBossEntity || e instanceof BossEntity));
    }

    private static String bossName(LivingEntity e) {
        if (e instanceof DataBossEntity d && d.def() != null) {
            return d.def().id;
        }
        return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
    }

    /**
     * Ticks a boss next to the player, kills it as the player and checks the kill was counted. A boss that swaps
     * itself for another entity (Zulrah's forms, Hydra's phases) is followed to the new form.
     */
    private String fight(LivingEntity first, String id) {
        java.util.Set<java.util.UUID> seen = new java.util.HashSet<>();
        for (LivingEntity e : bossesAround()) {
            seen.add(e.getUUID());
        }
        LivingEntity boss = first;
        List<String> forms = new ArrayList<>();
        int ticks = 0;
        while (true) {
            seen.add(boss.getUUID());
            if (boss instanceof net.minecraft.world.entity.Mob mob) {
                mob.setTarget(p());
            }
            while (ticks < BOSS_TICKS && boss.isAlive()) {
                boss.tick();
                c.heal();
                ticks++;
            }
            if (boss.isAlive()) {
                break;
            }
            LivingEntity next = null;
            for (LivingEntity e : bossesAround()) {
                if (!seen.contains(e.getUUID())) {
                    next = e;
                    break;
                }
            }
            if (next == null || forms.size() >= 12) {
                String how = boss.isRemoved() && boss.getHealth() > 0 ? "despawned" : "died";
                throw failure(how + " by itself after " + ticks + " ticks without being attacked"
                        + (forms.isEmpty() ? "" : " (forms " + String.join(" -> ", forms) + ")")
                        + (c.realPlayer() ? "" : "; the fake player is not in the world's player list"));
            }
            forms.add(bossName(boss) + "->" + bossName(next) + "@" + ticks);
            boss = next;
        }
        int kcBefore = totalKills();
        int logBefore = data().collectionLog().size();
        boss.setHealth(0f);
        boss.die(CombatHandler.source(p()));
        int kc = totalKills() - kcBefore;
        // A companion (Dawn beside Dusk, Eldric beside Branda) is part of its main boss's kill: OSRS counts one "Grotesque
        // Guardians" / "Royal Titans" kill when the main boss dies (boss:fight:dusk and boss:fight:branda_the_fire_queen
        // check that one), so its own death must add none.
        boolean companion = DataBossService.isCompanion(id);
        if (companion && kc != 0) {
            throw failure("a companion boss added a kill count of its own (+" + kc + "): its death is part of the main boss's kill");
        }
        if (!companion && kc <= 0) {
            throw failure("killing it did not add a kill count (" + ticks + " ticks alive)");
        }
        int drops = c.world.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(boss.blockPosition()).inflate(6),
                e -> true).size();
        return ticks + " ticks" + (forms.isEmpty() ? "" : ", forms " + String.join(" ", forms))
                + (companion ? ", companion: no kill count of its own (shared with its main boss)" : ", kc +" + kc) + ", " + drops
                + " drops, log +" + (data().collectionLog().size() - logBefore);
    }

    private int totalKills() {
        int total = 0;
        for (int kc : data().killCounts().values()) {
            total += kc;
        }
        return total;
    }

    // ------------------------------------------------------------------ teleports

    /** Every teleport destination (jewellery, other teleport items, spells, landmarks) as block x/z. */
    private static Map<String, int[]> destinations() {
        Map<String, int[]> all = new LinkedHashMap<>();
        List<Teleports.Destination> dests = new ArrayList<>();
        for (Jewellery.Worn w : Jewellery.all()) {
            dests.addAll(w.teleports());
        }
        for (Jewellery.Worn w : Jewellery.allHeld()) {
            dests.addAll(w.teleports());
        }
        dests.addAll(Teleports.XERIC);
        dests.addAll(Teleports.SLAYER);
        dests.addAll(Teleports.DRAKAN);
        dests.addAll(Teleports.ECTOPHIAL);
        dests.addAll(Teleports.CRAFTING_CAPE);
        dests.add(Teleports.XERICS_HONOUR);
        dests.add(Teleports.EDGEVILLE_HOME);
        dests.add(Teleports.LUNAR_HOME);
        dests.add(Teleports.ARCEUUS_HOME);
        dests.addAll(Teleports.CAMULET);
        for (Teleports.Destination d : dests) {
            // an OSRS dungeon tile falls back to the surface above it when no dungeon is carved there
            all.putIfAbsent(key(d.name()) + "@" + d.x() + "," + d.y(),
                    new int[] {GielinorMap.toBlockX(d.x()), GielinorMap.toBlockZ(Teleports.surfaceY(d))});
        }
        // Wilderness obelisks (riders land on the destination's platform) and the surface lever destinations.
        for (com.runecraft.osrs.content.npcs.WildernessTransport.Obelisk o : com.runecraft.osrs.content.npcs.WildernessTransport.OBELISKS) {
            all.putIfAbsent("obelisk_" + o.level(), new int[] {GielinorMap.toBlockX(o.x()), GielinorMap.toBlockZ(o.y())});
        }
        for (com.runecraft.osrs.content.npcs.WildernessTransport.Lever l : com.runecraft.osrs.content.npcs.WildernessTransport.LEVERS) {
            if (com.runecraft.osrs.content.npcs.WildernessTransport.cavernArrival(l) == null) { // caverns: transport:kbd_levers
                all.putIfAbsent("lever_" + l.id(), new int[] {GielinorMap.toBlockX(l.toX()) + 1, GielinorMap.toBlockZ(l.toY()) + 1});
            }
        }
        for (Spell s : Spell.values()) {
            int[] tile = s.tile();
            if (tile != null) {
                all.putIfAbsent("spell_" + key(s.name()) + "@" + tile[0] + "," + tile[1],
                        new int[] {GielinorMap.toBlockX(tile[0]), GielinorMap.toBlockZ(tile[1])});
            }
        }
        for (Landmark l : Landmark.values()) {
            all.putIfAbsent("landmark_" + key(l.name()),
                    new int[] {l.blockX() + (l.isAltar() ? 3 : 0), l.blockZ() + 3});
        }
        return all;
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
    }

    /** Throws unless the player stands within one block of {@code a} (x, feet y, z). */
    private void near(int[] a, String what) {
        BlockPos at = p().blockPosition();
        if (Math.abs(at.getX() - a[0]) > 1 || Math.abs(at.getZ() - a[2]) > 1 || Math.abs(at.getY() - a[1]) > 1) {
            throw failure(what + " at " + at.toShortString() + ", expected " + a[0] + " " + a[1] + " " + a[2]);
        }
    }

    /**
     * The King Black Dragon's levers: the lever in the Lava Maze's entrance chamber lands beside the lever in the lair (with
     * the dragon's summoning stone), and the lair's lever brings the player back into the chamber.
     */
    private String kbdLevers() {
        c.home();
        com.runecraft.osrs.content.npcs.WildernessTransport.Lever in = com.runecraft.osrs.content.npcs.WildernessTransport.lever("kbd_lair_entrance");
        com.runecraft.osrs.content.npcs.WildernessTransport.Lever out = com.runecraft.osrs.content.npcs.WildernessTransport.lever("kbd_lair");
        com.runecraft.content.npcs.WildernessTransportService.pull(p(), in);
        int[] a = com.runecraft.osrs.content.npcs.WildernessTransport.kbdLairArrival();
        near(a, "lair arrival");
        int[] st = com.runecraft.osrs.content.npcs.WildernessTransport.KBD_STONE_TILE;
        // The stone lies outside the chunks the arrival builds (a real player's view distance queues them): build its chunk.
        com.runecraft.world.StructurePlacer.applyAround(c.world, GielinorMap.toBlockX(st[0]), GielinorMap.toBlockZ(st[1]), 0);
        BlockState stone = c.world.getBlockState(new BlockPos(GielinorMap.toBlockX(st[0]), a[1], GielinorMap.toBlockZ(st[1])));
        if (!(stone.getBlock() instanceof com.runecraft.block.DataBossSummonBlock)) {
            throw failure("no King Black Dragon summoning stone in the lair, found " + stone);
        }
        BlockState lever = c.world.getBlockState(new BlockPos(GielinorMap.toBlockX(out.x()), a[1], GielinorMap.toBlockZ(out.y())));
        if (!lever.is(net.minecraft.world.level.block.Blocks.LEVER)) {
            throw failure("no lever in the lair, found " + lever);
        }
        com.runecraft.content.npcs.WildernessTransportService.pull(p(), out);
        near(com.runecraft.osrs.content.npcs.WildernessTransport.kbdRoomArrival(), "entrance chamber arrival");
        int[] lb = com.runecraft.osrs.content.npcs.WildernessTransport.kbdLeverBlock();
        BlockState back = c.world.getBlockState(new BlockPos(lb[0], com.runecraft.osrs.content.npcs.WildernessTransport.KBD_ROOM_FLOOR + 1, lb[1]));
        if (!back.is(net.minecraft.world.level.block.Blocks.LEVER)) {
            throw failure("no lever in the entrance chamber, found " + back);
        }
        c.home();
        return "lair and back";
    }

    /**
     * A realm fairy ring (BKQ's Enchanted Valley, and the rings beside the Abyssal Nexus, the Myreque hideout and Yu'biusk):
     * travelling there lands next to a fairy ring the player can leave from (laid on arrival in worlds built before it).
     */
    private String fairyRingRealm(String code) {
        c.home();
        com.runecraft.content.islands.IslandsTravelService.travelTo(p(), code);
        net.minecraft.world.level.block.Block ring = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                com.runecraft.RuneCraft.resolve(com.runecraft.osrs.content.islands.IslandsBlocks.FAIRY_RING));
        BlockPos at = p().blockPosition();
        for (BlockPos q : BlockPos.betweenClosed(at.offset(-4, -3, -4), at.offset(4, 3, 4))) {
            if (c.world.getBlockState(q).is(ring)) {
                String where = GielinorMap.toOsrsX(at.getX()) + "," + GielinorMap.toOsrsY(at.getZ()) + " y " + at.getY();
                c.home();
                return "ring beside " + where;
            }
        }
        String where = GielinorMap.toOsrsX(at.getX()) + "," + GielinorMap.toOsrsY(at.getZ()) + " y " + at.getY();
        c.home();
        throw failure("no fairy ring near the arrival at " + where);
    }

    /**
     * A Rune essence mine teleporter: without Rune Mysteries the teleport is refused; with it the player lands on the mine's
     * floor beside an outcrop of rune essence rock and a portal, and the portal returns them to where they stood.
     */
    private String essenceMine(String npcId) {
        boolean builtIn = false;
        try {
            com.runecraft.osrs.QuestNpc.valueOf(npcId);
            builtIn = true;
        } catch (IllegalArgumentException ignored) {
            // A module NPC.
        }
        if (!builtIn && ContentRegistry.npc(npcId) == null) {
            throw failure("no NPC " + npcId);
        }
        c.home();
        if (ContentRegistry.quest(com.runecraft.osrs.content.dungeons.EssenceMine.QUEST) != null
                && com.runecraft.content.dungeons.EssenceMineService.teleport(p(), npcId)) {
            throw failure("teleported before Rune Mysteries");
        }
        c.completeQuests(null);
        c.home();
        BlockPos start = p().blockPosition();
        if (!com.runecraft.content.dungeons.EssenceMineService.teleport(p(), npcId)) {
            throw failure("teleport refused after Rune Mysteries");
        }
        BlockPos at = p().blockPosition();
        int ox = GielinorMap.toOsrsX(at.getX());
        int oy = GielinorMap.toOsrsY(at.getZ());
        String where = ox + "," + oy + " y " + at.getY();
        if (!com.runecraft.osrs.content.dungeons.EssenceMine.contains(ox, oy, at.getY())
                || !com.runecraft.osrs.content.dungeons.EssenceMine.inRoom(ox, oy)) {
            throw failure("landed outside the mine at " + where);
        }
        BlockState feet = c.world.getBlockState(at);
        BlockState below = c.world.getBlockState(at.below());
        if (!feet.getCollisionShape(c.world, at).isEmpty() || below.getCollisionShape(c.world, at.below()).isEmpty()) {
            throw failure("no clear footing at " + where + " (feet " + feet + ", below " + below + ")");
        }
        int[] o = com.runecraft.osrs.content.dungeons.EssenceMine.OUTCROPS.get(0);
        BlockState rock = c.world.getBlockState(new BlockPos(GielinorMap.toBlockX(o[0]),
                com.runecraft.osrs.content.dungeons.EssenceMine.FLOOR + 1, GielinorMap.toBlockZ(o[1])));
        if (!(rock.getBlock() instanceof com.runecraft.block.RockBlock rb) || rb.rock() != com.runecraft.osrs.data.Rock.RUNE_ESSENCE) {
            throw failure("no rune essence outcrop, found " + rock);
        }
        int[] portal = com.runecraft.osrs.content.dungeons.EssenceMine.PORTALS.get(0);
        BlockState portalState = c.world.getBlockState(new BlockPos(GielinorMap.toBlockX(portal[0]),
                com.runecraft.osrs.content.dungeons.EssenceMine.FLOOR + 1, GielinorMap.toBlockZ(portal[1])));
        String portalId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(portalState.getBlock()).getPath();
        if (!portalId.equals(com.runecraft.osrs.content.dungeons.DungeonBlocks.ESSENCE_PORTAL)) {
            throw failure("no portal, found " + portalId);
        }
        if (!npcId.equals(com.runecraft.content.dungeons.EssenceMineService.lastTeleporter(p()))) {
            throw failure("teleporter not remembered: " + com.runecraft.content.dungeons.EssenceMineService.lastTeleporter(p()));
        }
        com.runecraft.content.dungeons.EssenceMineService.leave(p());
        BlockPos back = p().blockPosition();
        if (back.distSqr(start) > 4) {
            throw failure("portal returned to " + back + " instead of " + start);
        }
        return "landed at " + where;
    }

    private String teleport(int x, int z) {
        ServerLevel world = c.world;
        BlockPos pos = Gielinor.safeSpot(world, x, z);
        BlockState feet = world.getBlockState(pos);
        BlockState head = world.getBlockState(pos.above());
        BlockState below = world.getBlockState(pos.below());
        String where = pos.getX() + " " + pos.getY() + " " + pos.getZ();
        if (!feet.getCollisionShape(world, pos).isEmpty() || !head.getCollisionShape(world, pos.above()).isEmpty()) {
            throw failure("lands inside a block at " + where + " (" + feet + " / " + head + ")");
        }
        if (!feet.getFluidState().isEmpty() || !below.getFluidState().isEmpty()) {
            throw failure("lands in liquid at " + where + " (feet " + feet + ", below " + below + ")");
        }
        if (below.getCollisionShape(world, pos.below()).isEmpty()) {
            throw failure("nothing to stand on at " + where + " (below " + below + ")");
        }
        // The player stays on the test platform: moving it would load chunks around every destination.
        return where + " on " + net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(below.getBlock());
    }
}
