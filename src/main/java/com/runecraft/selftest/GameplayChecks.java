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
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
        for (Minigame m : Minigame.values()) {
            checks.add(new SelfTest.Check("minigame:" + m.id(), () -> g.minigame(m)));
        }
        for (RaidType r : RaidType.values()) {
            checks.add(new SelfTest.Check("raid:" + r.id(), () -> g.raid(r)));
        }
        for (Boss b : ModEntities.BOSS_TYPES.keySet()) {
            checks.add(new SelfTest.Check("boss:" + b.entityId(), () -> g.legacyBoss(b)));
        }
        for (String id : ModEntities.DATA_BOSS_TYPES.keySet()) {
            checks.add(new SelfTest.Check("boss:" + id, () -> g.dataBoss(id)));
        }
        for (Map.Entry<String, int[]> e : destinations().entrySet()) {
            checks.add(new SelfTest.Check("teleport:" + e.getKey(), () -> g.teleport(e.getValue()[0], e.getValue()[1])));
        }
        for (String npcId : com.runecraft.osrs.content.dungeons.EssenceMine.TELEPORTERS.keySet()) {
            checks.add(new SelfTest.Check("essencemine:" + npcId.toLowerCase(Locale.ROOT), () -> g.essenceMine(npcId)));
        }
        // Items, combat and interactions (after the teleports, which generate the spell destinations' chunks).
        checks.addAll(ItemCombatChecks.all(context));
        return checks;
    }

    private static AssertionError failure(String message) {
        return new AssertionError(message);
    }

    private ServerPlayerEntity p() {
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
        net.minecraft.item.ItemStack bars = Inv.stack("bronze_bar", 10);
        if (!bars.isEmpty()) {
            com.runecraft.bank.BankService.depositStack(data.bank(), bars, 10, 0);
        }
        NbtCompound saved = data.toNbt();
        PlayerData loaded = new PlayerData();
        loaded.fromNbt(saved.copy());
        NbtCompound again = loaded.toNbt();
        if (!saved.equals(again)) {
            throw failure("PlayerData changed after a save/load round trip:\n  saved  " + saved + "\n  loaded " + again);
        }
        // The whole world-save state, as written to data/runecraft.dat.
        NbtCompound state = Players.state(c.server).writeNbt(new NbtCompound());
        PlayerDataState reread = PlayerDataState.fromNbt(state);
        NbtCompound mine = reread.get(p().getUuid()).toNbt();
        if (!saved.equals(mine)) {
            throw failure("PlayerDataState lost this player's data on a round trip:\n  saved  " + saved + "\n  loaded " + mine);
        }
        return saved.getKeys().size() + " keys";
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
            p().getInventory().clear();
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

    /** The trigger that moves a stage on: the first non-drop one, else a drop that advances. */
    private static Trigger advancing(QuestSpec.Stage stage) {
        Trigger drop = null;
        for (Trigger t : stage.triggers) {
            if (t.next == t.stage) {
                continue;
            }
            if (t.kind != Trigger.Kind.DROP) {
                return t;
            }
            if (drop == null) {
                drop = t;
            }
        }
        return drop;
    }

    /** Hands the player whatever a trigger needs and removes what would make it skip. */
    private void prepare(Trigger t) {
        t.need.forEach((item, n) -> Inv.give(p(), item, n));
        t.take.forEach((item, n) -> Inv.give(p(), item, n));
        QuestsState state = QuestsState.get(p());
        t.needFlags.forEach(f -> state.setFlag(p().getUuid(), f));
        for (String item : t.unlessItems) {
            Inv.remove(p(), item, 10_000);
        }
        if (t.kind == Trigger.Kind.DROP) {
            t.give.keySet().forEach(item -> Inv.remove(p(), item, 10_000));
        }
    }

    /** Reports a kill of {@code creature} through the real kill hooks; false if it isn't a known entity. */
    private boolean kill(String creature) {
        EntityType<?> type = ModEntities.typeById(creature);
        if (type == null) {
            return false;
        }
        Entity e = type.create(c.world);
        if (!(e instanceof LivingEntity victim)) {
            return false;
        }
        victim.refreshPositionAndAngles(p().getX() + 2, p().getY(), p().getZ(), 0f, 0f);
        QuestManager.onKill(p(), victim, MonsterStats.of(victim));
        victim.discard();
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
            for (int guard = 0; guard < spec.stages.size() * 3 + 3 && !data.questComplete(q); guard++) {
                int s = data.questStage(q);
                if (s < 0 || s >= spec.stages.size()) {
                    stuck = "stage " + s + " is outside the " + spec.stages.size() + " stages";
                    break;
                }
                Trigger t = advancing(spec.stages.get(s));
                if (t == null) {
                    stuck = "stage " + s + " has no trigger that moves it on";
                    break;
                }
                prepare(t);
                boolean handled = true;
                switch (t.kind) {
                    case TALK -> QuestInterpreter.selfTestTalk(p(), t.target);
                    case KILL -> handled = kill(t.target);
                    default -> QuestInterpreter.selfTestFire(p(), spec, t);
                }
                if (!handled || (data.questStage(q) == s && !data.questComplete(q))) {
                    // The dialogue picked another branch or a condition wasn't met: fire the objective directly.
                    prepare(t);
                    QuestInterpreter.selfTestFire(p(), spec, t);
                    if (data.questStage(q) == s && !data.questComplete(q)) {
                        stuck = "stuck at stage " + s + " (" + t.kind.name().toLowerCase(Locale.ROOT) + " " + t.target + ")";
                        break;
                    }
                    forced++;
                }
                steps++;
            }
            walk = data.questComplete(q) ? "walked " + steps + " steps" + (forced > 0 ? " (" + forced + " fired directly)" : "")
                    : "walked " + steps + "/" + spec.stages.size() + " then forced complete" + (stuck == null ? "" : ": " + stuck);
        }
        boolean natural = data.questComplete(q);
        if (!natural) {
            p().getInventory().clear();
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
        loaded.fromNbt(data.toNbt());
        if (!loaded.questComplete(q)) {
            throw failure("completion lost on save/load");
        }
        return walk;
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
        List<BossEntity> found = c.world.getEntitiesByType(type, new Box(at).expand(48), e -> e.isAlive());
        if (found.isEmpty()) {
            throw failure("BossService.summon spawned nothing");
        }
        return fight(found.get(0), boss.entityId());
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
        List<DataBossEntity> found = c.world.getEntitiesByType(type, new Box(at).expand(48), e -> e.isAlive());
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
        Box box = new Box(TestContext.PLATFORM).expand(48);
        return c.world.getEntitiesByClass(LivingEntity.class, box,
                e -> e.isAlive() && (e instanceof DataBossEntity || e instanceof BossEntity));
    }

    private static String bossName(LivingEntity e) {
        if (e instanceof DataBossEntity d && d.def() != null) {
            return d.def().id;
        }
        return net.minecraft.registry.Registries.ENTITY_TYPE.getId(e.getType()).getPath();
    }

    /**
     * Ticks a boss next to the player, kills it as the player and checks the kill was counted. A boss that swaps
     * itself for another entity (Zulrah's forms, Hydra's phases) is followed to the new form.
     */
    private String fight(LivingEntity first, String id) {
        java.util.Set<java.util.UUID> seen = new java.util.HashSet<>();
        for (LivingEntity e : bossesAround()) {
            seen.add(e.getUuid());
        }
        LivingEntity boss = first;
        List<String> forms = new ArrayList<>();
        int ticks = 0;
        while (true) {
            seen.add(boss.getUuid());
            if (boss instanceof net.minecraft.entity.mob.MobEntity mob) {
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
                if (!seen.contains(e.getUuid())) {
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
        boss.onDeath(CombatHandler.source(p()));
        int kc = totalKills() - kcBefore;
        if (kc <= 0) {
            throw failure("killing it did not add a kill count (" + ticks + " ticks alive)");
        }
        int drops = c.world.getEntitiesByClass(net.minecraft.entity.ItemEntity.class, new Box(boss.getBlockPos()).expand(6),
                e -> true).size();
        return ticks + " ticks" + (forms.isEmpty() ? "" : ", forms " + String.join(" ", forms)) + ", kc +" + kc + ", " + drops + " drops, log +" + (data().collectionLog().size() - logBefore);
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
        for (Teleports.Destination d : dests) {
            all.putIfAbsent(key(d.name()) + "@" + d.x() + "," + d.y(),
                    new int[] {GielinorMap.toBlockX(d.x()), GielinorMap.toBlockZ(d.y())});
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
        BlockPos start = p().getBlockPos();
        if (!com.runecraft.content.dungeons.EssenceMineService.teleport(p(), npcId)) {
            throw failure("teleport refused after Rune Mysteries");
        }
        BlockPos at = p().getBlockPos();
        int ox = GielinorMap.toOsrsX(at.getX());
        int oy = GielinorMap.toOsrsY(at.getZ());
        String where = ox + "," + oy + " y " + at.getY();
        if (!com.runecraft.osrs.content.dungeons.EssenceMine.contains(ox, oy, at.getY())
                || !com.runecraft.osrs.content.dungeons.EssenceMine.inRoom(ox, oy)) {
            throw failure("landed outside the mine at " + where);
        }
        BlockState feet = c.world.getBlockState(at);
        BlockState below = c.world.getBlockState(at.down());
        if (!feet.getCollisionShape(c.world, at).isEmpty() || below.getCollisionShape(c.world, at.down()).isEmpty()) {
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
        String portalId = net.minecraft.registry.Registries.BLOCK.getId(portalState.getBlock()).getPath();
        if (!portalId.equals(com.runecraft.osrs.content.dungeons.DungeonBlocks.ESSENCE_PORTAL)) {
            throw failure("no portal, found " + portalId);
        }
        if (!npcId.equals(com.runecraft.content.dungeons.EssenceMineService.lastTeleporter(p()))) {
            throw failure("teleporter not remembered: " + com.runecraft.content.dungeons.EssenceMineService.lastTeleporter(p()));
        }
        com.runecraft.content.dungeons.EssenceMineService.leave(p());
        BlockPos back = p().getBlockPos();
        if (back.getSquaredDistance(start) > 4) {
            throw failure("portal returned to " + back + " instead of " + start);
        }
        return "landed at " + where;
    }

    private String teleport(int x, int z) {
        ServerWorld world = c.world;
        BlockPos pos = Gielinor.safeSpot(world, x, z);
        BlockState feet = world.getBlockState(pos);
        BlockState head = world.getBlockState(pos.up());
        BlockState below = world.getBlockState(pos.down());
        String where = pos.getX() + " " + pos.getY() + " " + pos.getZ();
        if (!feet.getCollisionShape(world, pos).isEmpty() || !head.getCollisionShape(world, pos.up()).isEmpty()) {
            throw failure("lands inside a block at " + where + " (" + feet + " / " + head + ")");
        }
        if (!feet.getFluidState().isEmpty() || !below.getFluidState().isEmpty()) {
            throw failure("lands in liquid at " + where + " (feet " + feet + ", below " + below + ")");
        }
        if (below.getCollisionShape(world, pos.down()).isEmpty()) {
            throw failure("nothing to stand on at " + where + " (below " + below + ")");
        }
        // The player stays on the test platform: moving it would load chunks around every destination.
        return where + " on " + net.minecraft.registry.Registries.BLOCK.getId(below.getBlock());
    }
}
