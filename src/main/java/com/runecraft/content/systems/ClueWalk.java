package com.runecraft.content.systems;

import com.runecraft.combat.CombatHandler;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.QuestNpc;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.systems.ClueChallenges;
import com.runecraft.osrs.content.systems.ClueNpcs;
import com.runecraft.osrs.content.systems.ClueReqs;
import com.runecraft.osrs.content.systems.ClueRewards;
import com.runecraft.osrs.content.systems.CluePuzzles;
import com.runecraft.osrs.content.systems.ClueScrolls;
import com.runecraft.osrs.content.systems.ClueScrolls.Action;
import com.runecraft.osrs.content.systems.ClueScrolls.Clue;
import com.runecraft.osrs.content.systems.ClueScrolls.Step;
import com.runecraft.osrs.content.systems.ClueScrolls.StepType;
import com.runecraft.osrs.content.systems.ClueScrolls.Tier;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.osrs.content.systems.MimicRewards;
import com.runecraft.osrs.content.systems.Teleports;
import com.runecraft.player.Players;
import com.runecraft.registry.ModEntities;
import com.runecraft.util.Inv;
import com.runecraft.world.Gielinor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Self-test driver ({@code minigame:clue_<tier>}): a real player follows a treasure trail of each tier made of real OSRS
 * steps, chosen so every step type is solved at least once, through the same entry points the game uses (the clue
 * scroll's buttons, right-clicking NPCs, the strange device, the STASH unit, the puzzle and light boxes, challenge
 * scroll answers, killing guardians and key/kill monsters as the player), then opens the reward casket and checks the
 * reward. The master walk also fights the Mimic and trades four clue scrolls in to Watson. Real player only (guardians
 * target players in the player list). The harness restores the player's whole save afterwards.
 */
public final class ClueWalk {
    private static final net.minecraft.resources.ResourceLocation HP_BUFFER = com.runecraft.RuneCraft.id("selftest_clue_hp");

    private final ServerPlayer player;
    private final ServerLevel world;
    private final Tier tier;
    private final List<ClueScrolls.Npc> npcs = ClueService.npcs();
    private final List<Entity> spawned = new ArrayList<>();
    private final StringBuilder log = new StringBuilder();
    private final Random random = new Random(0xC10EL);

    private ClueWalk(ServerPlayer player, ServerLevel world, Tier tier) {
        this.player = player;
        this.world = world;
        this.tier = tier;
    }

    /** Walks one tier's trail and puts the player back at {@code back}; returns a summary or throws {@link AssertionError}. */
    public static String run(ServerPlayer player, BlockPos back, Tier tier) {
        ServerLevel world = Gielinor.world(player.getServer());
        expect(world != null, "no Gielinor");
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.removeModifier(HP_BUFFER);
            health.addTransientModifier(new AttributeModifier(HP_BUFFER, 2000, AttributeModifier.Operation.ADD_VALUE));
            player.setHealth(player.getMaxHealth());
        }
        ClueWalk w = new ClueWalk(player, world, tier);
        try {
            return w.walk();
        } catch (AssertionError e) {
            throw new AssertionError(e.getMessage() + " (done: " + w.log + ")");
        } finally {
            for (Entity e : w.spawned) {
                if (e.isAlive()) {
                    e.discard();
                }
            }
            // Every guardian of this walk, also one a failed step left behind (it must not answer the next tier's dig).
            ClueService.clearGuardians(player);
            ClueService.unforce(player);
            if (health != null) {
                health.removeModifier(HP_BUFFER);
                player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
            }
            player.teleportTo(world, back.getX() + 0.5, back.getY(), back.getZ() + 0.5, player.getYRot(), player.getXRot());
        }
    }

    // ------------------------------------------------------------------ the trails

    private String walk() {
        List<Step> steps = new ArrayList<>();
        switch (tier) {
            case BEGINNER -> {
                steps.add(pick(c -> c.type() == StepType.ANAGRAM));
                steps.add(pick(c -> c.type() == StepType.EMOTE && !c.stash().isEmpty() && !c.req().isEmpty()));
                steps.add(pick(c -> c.type() == StepType.HOT_COLD));
                steps.add(pick(c -> c.type() == StepType.CHARLIE));
                steps.add(pick(c -> c.type() == StepType.MAP));
            }
            case EASY -> {
                steps.add(pick(c -> c.type() == StepType.CRYPTIC && c.action() == Action.SEARCH));
                steps.add(pick(c -> c.type() == StepType.MUSIC));
                steps.add(pick(c -> c.type() == StepType.EMOTE && c.emotes().size() == 1 && !c.req().isEmpty()));
                steps.add(pick(c -> c.type() == StepType.CRYPTIC && c.action() == Action.TALK));
            }
            case MEDIUM -> {
                steps.add(pick(c -> c.type() == StepType.ANAGRAM && !c.answer().isEmpty()));
                steps.add(pick(c -> c.type() == StepType.CIPHER && !c.answer().isEmpty()));
                steps.add(pick(c -> c.type() == StepType.COORDINATE));
                steps.add(pick(c -> c.type() == StepType.CRYPTIC && c.task().equals("key") && killable(c.npc())));
                steps.add(pick(c -> c.type() == StepType.EMOTE && c.emotes().size() == 2));
            }
            case HARD -> {
                steps.add(pick(c -> c.type() == StepType.COORDINATE && c.guardian() == ClueScrolls.Guardian.SARADOMIN_WIZARD));
                steps.add(pick(c -> c.type() == StepType.EMOTE && c.guardian() != null));
                steps.add(pick(c -> c.type() == StepType.ANAGRAM && c.answer().isEmpty()));
                steps.add(pick(c -> c.type() == StepType.FAIRY_RING));
            }
            case ELITE -> {
                steps.add(pick(c -> c.type() == StepType.SHERLOCK && c.text().equals("Mine a mithril ore.")));
                steps.add(pick(c -> c.type() == StepType.CRYPTIC && c.action() == Action.KILL && killable(c.npc())));
                steps.add(pick(c -> c.type() == StepType.COORDINATE && c.guardian() != null));
            }
            case MASTER -> {
                steps.add(pick(c -> c.type() == StepType.HOT_COLD));
                steps.add(pick(c -> c.type() == StepType.FALO));
                Step light = pick(c -> c.type() == StepType.ANAGRAM && c.answer().isEmpty());
                steps.add(new Step(light.clue(), light.npcId(), light.npcName(), CluePuzzles.LIGHT_BOX, null));
                steps.add(pick(c -> c.type() == StepType.EMOTE && c.guardian() != null));
            }
        }
        clearClues();
        ClueService.force(player, tier, steps);
        for (int i = 0; i < steps.size(); i++) {
            Step s = steps.get(i);
            expect(ClueService.stepIndex(player, tier) == i, "trail at step " + ClueService.stepIndex(player, tier) + ", wanted " + i);
            ClueService.read(player, tier);
            solve(s);
            expect(ClueService.stepIndex(player, tier) != i, s.type() + " step not completed: " + s.text());
            log.append(s.type().name().toLowerCase(Locale.ROOT)).append(' ');
        }
        expect(!Inv.has(player, tier.scrollItem(), 1), "the clue scroll is still held after the last step");
        expect(Inv.has(player, tier.casketItem(), 1), "no reward casket");
        String reward = openCasket();
        if (tier == Tier.MASTER) {
            reward += "; " + mimic() + "; " + lostClue() + "; " + watson();
        }
        return tier.id() + " trail: " + log.toString().trim() + "; " + reward;
    }

    /** A usable clue of this tier matching the test, as a trail step. */
    private Step pick(Predicate<Clue> test) {
        for (Clue c : ClueScrolls.usableClues(tier, npcs)) {
            if (test.test(c)) {
                return ClueScrolls.step(c, random, npcs);
            }
        }
        throw new AssertionError("no usable " + tier + " clue for this step");
    }

    private static boolean killable(String monster) {
        return monsterId(monster) != null;
    }

    // ------------------------------------------------------------------ solving one step

    private void solve(Step s) {
        switch (s.action()) {
            case DIG -> dig(s);
            case SEARCH -> search(s);
            case EMOTE -> emote(s);
            case KILL -> {
                go(player.getBlockX(), player.getBlockZ(), false);
                kill(spawnMonster(monsterId(s.clue().npc())));
            }
            case TALK -> talk(s);
        }
    }

    private void dig(Step s) {
        giveIfMissing("spade");
        if (s.type() == StepType.HOT_COLD) {
            // The strange device: Reldo or Jorral hands it over, then it gets hotter as the player closes in.
            talkTo(tier == Tier.MASTER ? "jorral" : "reldo");
            expect(Inv.has(player, ClueScrolls.strangeDevice(tier), 1), "no strange device from " + (tier == Tier.MASTER ? "Jorral" : "Reldo"));
            goTile(s.x() + 40, s.y());
            ClueService.feel(player, tier);
            goTile(s.x(), s.y());
            ClueService.feel(player, tier);
            heal();
        } else if (s.type() == StepType.COORDINATE) {
            coordinateTools();
            goTile(s.x(), s.y());
            ClueService.sextant(player);
        } else {
            goTile(s.x(), s.y());
        }
        if (s.guardian() != null) {
            settle(); // the guardian appears in the chunks around the player, loaded as for a player who walked here
        }
        expect(ClueService.dig(player, tier, true), "the dig at " + s.x() + "," + s.y() + " wasn't taken (player at "
                + java.util.Arrays.toString(SysTeleport.osrs(player)) + ")");
        if (s.guardian() != null) {
            List<LivingEntity> guards = ClueService.guardians(player, tier);
            expect(!guards.isEmpty(), s.guardian() + " didn't appear");
            for (LivingEntity g : guards) {
                expect(ClueService.dropsNoClues(g), g.getName().getString() + " rolls clue scroll drops");
                kill(g);
            }
            expect(ClueService.flag(player, tier, ClueService.GUARDIAN_KILLED), "killing the guardian didn't count");
            expect(ClueService.dig(player, tier, true), "digging again after the guardian failed");
            log.append('[').append(s.guardian().display()).append("] ");
        }
    }

    /**
     * The sextant, watch and chart from their NPCs (OSRS order: the Observatory professor's chart, Murphy's sextant, then
     * Brother Kojo's watch; Kojo turns a hunter away before Murphy has).
     */
    private void coordinateTools() {
        SysState.SysPlayer sp = SysState.of(player);
        for (String tool : new String[]{"sextant", "watch", "chart"}) {
            while (Inv.has(player, tool, 1)) {
                Inv.remove(player, tool, 1);
            }
        }
        sp.unlocks.remove(ClueService.CHART_FROM_PROFESSOR);
        sp.unlocks.remove(ClueService.SEXTANT_FROM_MURPHY);
        talkTo("brother_kojo");
        expect(!Inv.has(player, "watch", 1), "Brother Kojo gave a watch before Murphy's sextant");
        talkTo("observatory_professor");
        expect(Inv.has(player, "chart", 1), "the Observatory professor gave no chart");
        talkTo(firstNpc("mg_murphy", "rfd_murphy"));
        expect(Inv.has(player, "sextant", 1), "Murphy gave no sextant");
        talkTo("brother_kojo");
        expect(Inv.has(player, "watch", 1), "Brother Kojo gave no watch");
        log.append("[sextant, watch, chart] ");
    }

    /** The first of these NPC ids this world places. */
    private String firstNpc(String... ids) {
        for (String id : ids) {
            for (ClueScrolls.Npc n : npcs) {
                if (n.id().equals(id)) {
                    return id;
                }
            }
        }
        throw new AssertionError("none of " + String.join(", ", ids) + " is placed");
    }

    private void search(Step s) {
        goTile(s.x(), s.y());
        if (s.keyMonster() != null) {
            settle(); // the key drops where the monster dies, in the chunks around the player
            expect(ClueService.search(player, tier, true), "search wasn't taken");
            expect(ClueService.stepIndex(player, tier) >= 0 && ClueService.current(player, tier) == s, "a locked search advanced");
            kill(spawnMonster(monsterId(s.keyMonster())));
            collect("key_medium", "key_elite");
            expect(Inv.has(player, "key_medium", 1) || Inv.has(player, "key_elite", 1), "the " + s.keyMonster() + " dropped no key");
        }
        expect(ClueService.search(player, tier, true), "search at " + s.x() + "," + s.y() + " wasn't taken");
    }

    private void emote(Step s) {
        // STASH unit: build it, fill it with the clue's items and take them out again.
        ClueScrolls.Stash stash = ClueScrolls.stashUnits().get(s.clue().stash());
        for (ClueReqs.Token t : ClueReqs.firstChoice(s.req())) {
            Inv.give(player, t.item(), t.qty());
        }
        goTile(s.x(), s.y());
        if (stash != null && Teleports.within(stash.x(), stash.y(), s.x(), s.y(), ClueScrolls.EMOTE_RADIUS)) {
            Players.data(player).skills().setXpTenths(Skill.CONSTRUCTION, 130_344_310);
            Inv.give(player, stash.tier().stashPlank(), 2);
            giveIfMissing("hammer");
            giveIfMissing("saw");
            if (stash.tier() == Tier.MASTER) {
                Inv.give(player, "gold_leaf", 1);
            }
            for (String metal : new String[]{"bronze", "iron", "steel", "black", "mithril", "adamant", "rune"}) {
                while (Inv.has(player, metal + "_nails", 1)) {
                    Inv.remove(player, metal + "_nails", 1);
                }
            }
            if (ItemCatalog.get("dragon_nails") != null) {
                Inv.give(player, "dragon_nails", 10);
                ClueService.stash(player, tier);
                expect(!SysState.of(player).stash.containsKey(stash.name()), "a STASH unit was built with dragon nails");
            }
            Inv.give(player, "bronze_nails", 10);
            ClueService.stash(player, tier);
            expect(SysState.of(player).stash.containsKey(stash.name()), "STASH unit not built");
            ClueService.stash(player, tier);
            expect(!SysState.of(player).stash.get(stash.name()).isEmpty(), "STASH unit not filled");
            ClueService.stash(player, tier);
            expect(SysState.of(player).stash.get(stash.name()).isEmpty(), "items not taken out of the STASH unit");
            log.append("[STASH] ");
        }
        if (s.guardian() != null) {
            settle(); // the double agent appears in the chunks around the player
        }
        // Uri is not pleased without the items: emote first with nothing worn.
        if (!s.req().isEmpty()) {
            performEmotes(s);
            expect(ClueService.current(player, tier) == s, "the emote step advanced without its items");
        }
        for (ClueReqs.Token t : ClueReqs.firstChoice(s.req())) {
            equip(t.item());
        }
        performEmotes(s);
        if (s.guardian() != null) {
            List<LivingEntity> agents = ClueService.guardians(player, tier);
            expect(!agents.isEmpty(), "no double agent appeared");
            for (LivingEntity a : agents) {
                kill(a);
            }
            performEmotes(s);
            log.append("[double agent] ");
        }
    }

    /** Performs the step's emotes in order from the clue scroll's buttons ("emote:TIER:i"), as the player does. */
    private void performEmotes(Step s) {
        for (int i = 0; i < s.emotes().size(); i++) {
            ClueService.action(player, "emote:" + tier.name() + ":" + i, java.util.Map.of());
        }
    }

    private void talk(Step s) {
        expect(s.npcId() != null, "no NPC for " + s.clue().npc());
        switch (s.type()) {
            case CHARLIE -> {
                talkStep(s);
                ClueReqs.Token item = ClueReqs.firstChoice(s.req()).get(0);
                Inv.give(player, item.item(), 1);
                talkStep(s);
            }
            case FALO -> {
                talkStep(s);
                expect(ClueService.current(player, tier) == s, "Falo accepted nothing");
                for (ClueReqs.Token t : ClueReqs.firstChoice(s.req())) {
                    Inv.give(player, t.item(), t.qty());
                }
                talkStep(s);
            }
            case SHERLOCK -> {
                talkStep(s);
                ClueChallenges.Challenge c = ClueChallenges.of(s.clue().text());
                expect(c != null && c.kind() == ClueChallenges.Kind.OBTAIN, "challenge " + s.clue().text());
                com.runecraft.content.GameEvents.fireObtain(player, c.targets().get(0), c.count());
                talkStep(s);
            }
            case MUSIC -> {
                talkStep(s);
                goTile(s.x(), s.y());
                ClueService.playSong(player, tier);
            }
            default -> {
                if (s.hasChallenge()) {
                    ClueService.answer(player, tier, s.clue().answer());
                    expect(ClueService.current(player, tier) == s, "a challenge answer was accepted before the NPC set it");
                }
                talkStep(s);
                if (s.box() != null) {
                    expect(Inv.has(player, s.box(), 1), "no " + s.box() + " handed over");
                    expect(ClueService.solvePuzzleForTest(player), "couldn't solve the " + s.box());
                    talkStep(s);
                    log.append('[').append(s.box()).append("] ");
                } else if (s.hasChallenge()) {
                    // From afar the answer is refused; at the NPC a wrong one is, then the right one advances.
                    int[] at = ClueService.target(player, s);
                    goTile(at[0] + 60, at[1]);
                    ClueService.answer(player, tier, s.clue().answer());
                    expect(ClueService.current(player, tier) == s, "a challenge answer was accepted away from the NPC");
                    npc(s.npcId());
                    ClueService.answer(player, tier, "-12345");
                    expect(ClueService.current(player, tier) == s, "a wrong challenge answer was accepted");
                    ClueService.answer(player, tier, s.clue().answer());
                    log.append("[challenge] ");
                }
            }
        }
    }

    /** Right-clicks the step's NPC (found where it stands, or put there for the test). */
    private void talkStep(Step s) {
        ClueService.talk(player, npc(s.npcId()));
    }

    private void talkTo(String npcId) {
        ClueService.talk(player, npc(npcId));
    }

    private QuestNpcEntity npc(String npcId) {
        ClueScrolls.Npc where = null;
        for (ClueScrolls.Npc n : npcs) {
            if (n.id().equals(npcId)) {
                where = n;
            }
        }
        expect(where != null, "unknown NPC " + npcId);
        int y = where.y() >= 6400 ? where.y() - 6400 : where.y();
        goTile(where.x(), y);
        AABB box = player.getBoundingBox().inflate(48, 64, 48);
        for (QuestNpcEntity e : world.getEntitiesOfClass(QuestNpcEntity.class, box, e -> e.isAlive() && npcId.equals(e.npcId()))) {
            return e;
        }
        QuestNpcEntity e = ModEntities.QUEST_NPC.create(world);
        expect(e != null, "no NPC entity type");
        NpcDef def = ContentRegistry.npc(npcId);
        if (def != null) {
            e.setNpc(def);
        } else {
            e.setNpc(QuestNpc.valueOf(npcId));
        }
        e.moveTo(player.getX() + 1, player.getY(), player.getZ(), 0, 0);
        world.addFreshEntity(e);
        spawned.add(e);
        return e;
    }

    // ------------------------------------------------------------------ the casket, the Mimic and Watson

    private String openCasket() {
        int before = Players.data(player).collectionLog().size();
        int done = SysState.of(player).cluesCompleted.getOrDefault(tier, 0);
        ItemStack casket = find(tier.casketItem());
        expect(casket != null, "casket not in the inventory");
        SysState.of(player).mimicEnabled = false;
        // The casket itself is used up, so a single reward item can take its slot: count what arrived besides it.
        int stacks = countItems() - 1;
        long amount = countAmount() - 1;
        int coins = Inv.count(player, Inv.COINS);
        ClueService.openCasket(player, casket, tier);
        expect(SysState.of(player).cluesCompleted.getOrDefault(tier, 0) == done + 1, "completed count not raised");
        expect(!Inv.has(player, tier.casketItem(), 1), "casket not used up");
        expect(countAmount() > amount || Inv.count(player, Inv.COINS) > coins, "the casket gave nothing");
        return "casket gave " + (countItems() - stacks) + " stacks" + (Inv.count(player, Inv.COINS) > coins ? " and "
                + (Inv.count(player, Inv.COINS) - coins) + " coins" : "") + ", log +" + (Players.data(player).collectionLog().size() - before);
    }

    /** Items in the inventory, counting every item of a stack. */
    private long countAmount() {
        long n = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            n += player.getInventory().getItem(i).getCount();
        }
        return n;
    }

    /**
     * The Mimic: with Mimic encounters on, the 10th master casket is the guaranteed one. Its mimic casket is presented to the
     * strange casket at Watson's house (the boss's summon); a failed attempt keeps the casket with one item roll fewer, the
     * kill opens it and gives the Mimic scroll case, and a casket whose attempts are all spent crumbles.
     */
    private String mimic() {
        SysState.SysPlayer sp = SysState.of(player);
        while (Inv.has(player, ClueRewards.MIMIC_ITEM, 1)) {
            Inv.remove(player, ClueRewards.MIMIC_ITEM, 1);
        }
        sp.mimicEnabled = true;
        sp.mimicSeen = false;
        sp.mimicCase = false;
        sp.mimicFight = "";
        sp.cluesCompleted.put(Tier.MASTER, MimicRewards.GUARANTEED_MASTER - 1);
        Inv.give(player, Tier.MASTER.casketItem(), 1);
        ClueService.openCasket(player, find(Tier.MASTER.casketItem()), Tier.MASTER);
        sp.mimicEnabled = false;
        ItemStack mimic = ClueService.mimicStack(player, null);
        expect(mimic != null, "the 10th master casket wasn't the guaranteed Mimic");
        MimicRewards.Casket casket = ClueService.casketOf(mimic);
        expect(casket.tier() == Tier.MASTER && casket.attempt() == 0 && casket.rolls() >= Tier.MASTER.minRolls(), "mimic casket " + casket);
        com.runecraft.osrs.content.BossDef def = ContentRegistry.boss(MimicRewards.MIMIC);
        expect(def != null, "no Mimic boss");
        expect(def.key == null, "the Mimic's summon uses up a key");
        goTile(com.runecraft.osrs.content.systems.SystemsContent.WATSON_X, com.runecraft.osrs.content.systems.SystemsContent.WATSON_Y);
        settle(); // the Mimic appears by the strange casket, in the chunks around the player
        int items = countItems();
        // First attempt: the player leaves and the Mimic resets; the casket stays with one try used.
        com.runecraft.boss.DataBossEntity boss = summonMimic(def);
        expect(Inv.has(player, ClueRewards.MIMIC_ITEM, 1), "presenting the mimic casket used it up");
        boss.discard();
        MimicRewards.Casket after = ClueService.casketOf(ClueService.mimicStack(player, null));
        expect(after.attempt() == 1, "the first attempt wasn't counted: " + after);
        // Second attempt: killed, with one casket roll fewer.
        boss = summonMimic(def);
        MimicRewards.Casket fought = ClueService.casketOf(ClueService.mimicStack(player, null));
        expect(fought.attempt() == 2 && fought.rollsNow() == casket.rolls() - 1, "second attempt " + fought);
        kill(boss);
        expect(!Inv.has(player, ClueRewards.MIMIC_ITEM, 1), "killing the Mimic didn't open its casket");
        expect(sp.mimicFight.isEmpty(), "the Mimic fight wasn't closed");
        expect(sp.mimicCase, "no Mimic scroll case");
        int limit = ClueRewards.stackLimit(Tier.EASY, sp.cluesCompleted.getOrDefault(Tier.EASY, 0));
        expect(ClueService.stackLimit(player, Tier.EASY) == limit + 1, "the Mimic scroll case didn't raise the stack limit");
        int gained = countItems() - items;
        // A casket whose five elite tries are all spent crumbles at the strange casket.
        ClueService.giveMimic(player, new MimicRewards.Casket(Tier.ELITE, 4, MimicRewards.attempts(Tier.ELITE)));
        com.runecraft.boss.DataBossService.summon(player, world, player.blockPosition(), def);
        expect(mimicsAround().isEmpty(), "a spent mimic casket summoned the Mimic");
        expect(!Inv.has(player, ClueRewards.MIMIC_ITEM, 1), "a spent mimic casket didn't crumble");
        return "Mimic guaranteed on the 10th master casket, killed on attempt 2 (+" + gained + " stacks), scroll case";
    }

    private com.runecraft.boss.DataBossEntity summonMimic(com.runecraft.osrs.content.BossDef def) {
        com.runecraft.boss.DataBossService.summon(player, world, player.blockPosition(), def);
        List<com.runecraft.boss.DataBossEntity> found = mimicsAround();
        expect(!found.isEmpty(), "the strange casket didn't summon the Mimic");
        spawned.add(found.get(0));
        player.teleportTo(world, found.get(0).getX() + 2, found.get(0).getY(), found.get(0).getZ(), player.getYRot(), player.getXRot());
        return found.get(0);
    }

    private List<com.runecraft.boss.DataBossEntity> mimicsAround() {
        EntityType<com.runecraft.boss.DataBossEntity> type = ModEntities.DATA_BOSS_TYPES.get(MimicRewards.MIMIC);
        return type == null ? List.of() : world.getEntities(type, player.getBoundingBox().inflate(64), e -> e.isAlive());
    }

    /**
     * A clue scroll read and then lost (dropped, died with it) doesn't block its tier: a scroll box opens to a fresh scroll
     * whose trail starts over, and with that scroll held the next box refuses.
     */
    private String lostClue() {
        Tier t = Tier.EASY;
        String box = ClueRewards.scrollBox(t);
        if (Players.data(player).bank().count(t.scrollItem(), "") > 0) {
            return "lost clue: skipped (an easy clue is banked)";
        }
        clearClues();
        SysState.SysPlayer sp = SysState.of(player);
        sp.clues.put(t, new long[]{7, 2, 0}); // read, two steps done, then the scroll was lost
        Inv.give(player, box, 1);
        ClueService.openScrollBox(player, find(box), t);
        expect(Inv.has(player, t.scrollItem(), 1), "a lost easy clue kept the scroll box shut");
        expect(!sp.clues.containsKey(t), "the lost clue's progress carried over to the new scroll");
        Inv.give(player, box, 1);
        ClueService.openScrollBox(player, find(box), t);
        expect(Inv.count(player, box) == 1, "a scroll box opened with a clue already held");
        while (Inv.has(player, box, 1)) {
            Inv.remove(player, box, 1);
        }
        return "lost clue: scroll box opened afresh";
    }

    /** Watson: an easy, medium, hard and elite clue scroll for a master clue. */
    private String watson() {
        clearClues();
        for (Tier t : new Tier[]{Tier.EASY, Tier.MEDIUM, Tier.HARD, Tier.ELITE}) {
            Inv.give(player, t.scrollItem(), 1);
        }
        SysState.of(player).watsonHanded = 0;
        // Right-clicking Watson: no clue step uses him, so his own dialogue runs (QuestScripts handler).
        npc(com.runecraft.osrs.content.systems.SystemsContent.WATSON);
        ClueService.watson(player);
        expect(Inv.has(player, Tier.MASTER.scrollItem(), 1), "Watson gave no master clue");
        expect(!Inv.has(player, Tier.ELITE.scrollItem(), 1), "Watson didn't take the clues");
        return "Watson: master clue";
    }

    // ------------------------------------------------------------------ helpers

    private void clearClues() {
        for (Tier t : Tier.values()) {
            while (Inv.has(player, t.scrollItem(), 1)) {
                Inv.remove(player, t.scrollItem(), 1);
            }
            SysState.of(player).clues.remove(t);
        }
    }

    private void goTile(int x, int y) {
        go(GielinorMap.toBlockX(x), GielinorMap.toBlockZ(y), true);
    }

    private void go(int bx, int bz, boolean exact) {
        world.getChunk(bx >> 4, bz >> 4);
        BlockPos pos = Gielinor.safeSpot(world, bx, bz);
        if (exact && (Math.abs(pos.getX() - bx) > 2 || Math.abs(pos.getZ() - bz) > 2)) {
            pos = new BlockPos(bx, world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz), bz);
        }
        player.teleportTo(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot(), player.getXRot());
    }

    /** Holds the chunks around a spot the walk settles at; the ticket lapses by itself a few seconds after the walk. */
    private static final net.minecraft.server.level.TicketType<net.minecraft.world.level.ChunkPos> SETTLE =
            net.minecraft.server.level.TicketType.create("runecraft_clue_walk",
                    java.util.Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong), 100);
    /** How long {@link #settle} waits for the chunks at most. */
    private static final long SETTLE_NANOS = 5_000_000_000L;

    /**
     * Gives the server the moment a player walking in real time always gives it on arriving at a spot (as CoxTobWalk does
     * for its rooms). The walk plays a whole trail inside one server tick and teleports from step to step; a chunk it loads
     * on arrival only becomes visible to entity searches once the server confirms it on its main thread, normally between
     * ticks. A guardian or double agent summoned there, a key dropped there or the Mimic is then left out of every search
     * (ClueService.guardians, the item pick-up), and a guardian the walk can't see is never killed or cleared. This holds the
     * chunks around the player (as the player's own view does) and runs the server's queued chunk work until they are
     * entity-ticking, at most {@link #SETTLE_NANOS}.
     */
    private void settle() {
        net.minecraft.server.level.ServerChunkCache chunks = world.getChunkSource();
        net.minecraft.world.level.ChunkPos at = player.chunkPosition();
        chunks.addRegionTicket(SETTLE, at, 3, at); // ticket level 30: this chunk and the ring around it entity-tick
        long deadline = System.nanoTime() + SETTLE_NANOS;
        while (!entityTicking(player.blockPosition(), 8)) {
            if (System.nanoTime() > deadline) {
                log.append("(chunks at ").append(at).append(" still loading after ").append(SETTLE_NANOS / 1_000_000_000L).append(" s) ");
                return;
            }
            if (!chunks.pollTask()) {
                java.util.concurrent.locks.LockSupport.parkNanos(100_000L); // wait for the chunk workers
            }
        }
    }

    /** Whether every chunk within {@code r} blocks of {@code c} is loaded, visible and ticking for entities. */
    private boolean entityTicking(BlockPos c, int r) {
        for (int x = (c.getX() - r) >> 4; x <= (c.getX() + r) >> 4; x++) {
            for (int z = (c.getZ() - r) >> 4; z <= (c.getZ() + r) >> 4; z++) {
                if (!world.isPositionEntityTicking(new BlockPos(x << 4, c.getY(), z << 4))) {
                    return false;
                }
            }
        }
        return true;
    }

    private void heal() {
        player.setHealth(player.getMaxHealth());
    }

    private void giveIfMissing(String id) {
        if (ItemCatalog.get(id) != null && !Inv.has(player, id, 1)) {
            Inv.give(player, id, 1);
        }
    }

    /**
     * Wears an item from the pack the way a player does: jewellery, capes and gloves through the item's own "Wear"
     * ({@link JewelleryService#wear}, from the off hand), armour and shields into their slot, and a weapon wielded into the
     * selected hotbar slot, whatever was held there going back into the pack. (Putting it straight into the main hand used
     * to overwrite the held stack, which was the clue scroll: the trail then ignored the player.) An item the game can't
     * wear at all fails the walk.
     */
    private void equip(String id) {
        ItemStack stack = takeOne(id);
        expect(!stack.isEmpty(), "no item " + id);
        Jewellery.Worn worn = Jewellery.worn(id);
        if (worn != null) {
            ItemStack off = player.getOffhandItem();
            player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, stack);
            JewelleryService.wear(player, net.minecraft.world.InteractionHand.OFF_HAND);
            ItemStack replaced = player.getOffhandItem(); // what the slot held before, or nothing
            player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, off);
            if (!replaced.isEmpty()) {
                Inv.give(player, replaced);
            }
            expect(Inv.idOf(SysState.of(player).worn(worn.slot())).equals(id), "couldn't wear the " + id);
            return;
        }
        ItemDef def = ItemCatalog.get(id);
        expect(def != null && def.equipment() != null, "the game can't wear or wield " + id);
        if (def.equipment().slot() == com.runecraft.osrs.EquipmentDef.Slot.WEAPON) {
            int selected = player.getInventory().selected;
            ItemStack held = player.getInventory().getItem(selected);
            player.getInventory().setItem(selected, stack);
            if (!held.isEmpty()) {
                Inv.give(player, held);
            }
            return;
        }
        EquipmentSlot slot = switch (def.equipment().slot()) {
            case HEAD -> EquipmentSlot.HEAD;
            case CHEST -> EquipmentSlot.CHEST;
            case LEGS -> EquipmentSlot.LEGS;
            case FEET -> EquipmentSlot.FEET;
            case SHIELD -> EquipmentSlot.OFFHAND;
            case WEAPON -> EquipmentSlot.MAINHAND; // handled above
        };
        ItemStack old = player.getItemBySlot(slot);
        player.setItemSlot(slot, stack);
        if (!old.isEmpty()) {
            Inv.give(player, old);
        }
    }

    /** One of an item taken out of the pack (the 36 inventory slots), or a new one when the pack has none. */
    private ItemStack takeOne(String id) {
        for (ItemStack s : player.getInventory().items) {
            if (!s.isEmpty() && Inv.idOf(s).equals(id)) {
                return s.split(1);
            }
        }
        return Inv.stack(id, 1);
    }

    private ItemStack find(String id) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.isEmpty() && Inv.idOf(s).equals(id)) {
                return s;
            }
        }
        return null;
    }

    private int countItems() {
        int n = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (!player.getInventory().getItem(i).isEmpty()) {
                n++;
            }
        }
        return n;
    }

    /** Picks up dropped items of the given ids lying near the player. */
    private void collect(String... ids) {
        for (ItemEntity e : world.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(16), e -> e.isAlive())) {
            String id = Inv.idOf(e.getItem());
            for (String want : ids) {
                if (want.equals(id)) {
                    Inv.give(player, e.getItem().copy());
                    e.discard();
                }
            }
        }
    }

    /** The monster id of a clue's monster name ("|" alternatives): the first that this world has. */
    static String monsterId(String names) {
        for (String name : names.split("\\|")) {
            for (com.runecraft.osrs.data.Monster m : com.runecraft.osrs.data.Monster.values()) {
                if (m.displayName().equalsIgnoreCase(name) && ModEntities.typeById(m.entity()) != null) {
                    return m.entity();
                }
            }
            for (MonsterDef m : ContentRegistry.monsters()) {
                if (m.name.equalsIgnoreCase(name) && ModEntities.typeById(m.id) != null) {
                    return m.id;
                }
            }
        }
        return null;
    }

    private LivingEntity spawnMonster(String id) {
        expect(id != null, "no monster to spawn");
        EntityType<?> type = ModEntities.typeById(id);
        expect(type != null, "no entity type " + id);
        Entity e = type.create(world);
        expect(e instanceof Mob, id + " is not a mob");
        Mob mob = (Mob) e;
        mob.moveTo(player.getX() + 2, player.getY(), player.getZ(), 0, 0);
        mob.finalizeSpawn(world, world.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        world.addFreshEntity(mob);
        spawned.add(mob);
        return mob;
    }

    /** Kills a creature as the player through the real damage path. */
    private void kill(LivingEntity e) {
        for (int i = 0; i < 200 && e.isAlive(); i++) {
            int d = e instanceof com.runecraft.entity.OsrsMonsterEntity m
                    ? m.modifyIncoming(player, Math.max(20, Math.round(e.getMaxHealth())), com.runecraft.osrs.AttackType.MAGIC)
                    : Math.max(20, Math.round(e.getMaxHealth()));
            CombatHandler.applyDamage(player, e, Math.max(1, d));
            heal();
        }
        expect(!e.isAlive(), e.getName().getString() + " couldn't be killed");
    }

    private static void expect(boolean ok, String message) {
        if (!ok) {
            throw new AssertionError(message);
        }
    }
}
