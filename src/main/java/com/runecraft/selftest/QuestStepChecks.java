package com.runecraft.selftest;

import com.runecraft.RuneCraft;
import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.boss.Mechanics;
import com.runecraft.combat.CombatHandler;
import com.runecraft.content.quests.QuestInterpreter;
import com.runecraft.content.quests.QuestPuzzles;
import com.runecraft.content.quests.QuestsState;
import com.runecraft.osrs.QuestInfo;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.CombatProfile;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.quests.QuestBook;
import com.runecraft.osrs.content.quests.QuestSpec;
import com.runecraft.osrs.content.quests.QuestSpec.Trigger;
import com.runecraft.osrs.content.quests.QuestStepWalk;
import com.runecraft.osrs.content.quests.QuestWalk;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModEntities;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Quests walked step by step ({@code quest:steps:<id>}, {@link QuestStepWalk#STEP_WALKED}): unlike {@code quest:<id>}, which only
 * fires each stage's advancing trigger and hands over the flags it needs, this walk takes every objective in order the way
 * {@link QuestStepWalk#nextStep} plans it (the same rule {@code QuestStepWalkTest} checks statically), earning each flag and
 * quest item:
 * <ul>
 *   <li>talks go through the real dialogue dispatcher, which must fire the step the walk means;</li>
 *   <li>places are reached by standing at them while the real place check runs;</li>
 *   <li>foes are raised the way a player meets them (a step spawned them, or the summoning stone at their arena, which asks the
 *   quest whether the player may fight them now) and fought to death on the test platform (one called up at a far place is
 *   brought there with the lair it is leashed to): they attack, the player's hits
 *   go through their own damage rules (a foe that needs a special weapon is checked to shrug off ordinary ones), and the
 *   kill reaches the quest through the real death path;</li>
 *   <li>puzzles are shown where they are presented and answered through the chat-link handler;</li>
 *   <li>the only things handed over are what a player brings from outside the quest (tools, runes, coins, ingredients) and
 *   flags earned in other quests;</li>
 *   <li>a step may cost only the quest items it takes (a fight's weapon swap puts what was in hand back in the pack).</li>
 * </ul>
 * Any step that doesn't take effect fails the check with the stage, step and reason; so does a quest that doesn't end with
 * its quest points, XP and item rewards.
 */
final class QuestStepChecks {
    private static final int MC = ActionManager.MC_TICKS_PER_GAME_TICK;
    /** Game ticks a foe gets to attack before the kill starts. */
    private static final int ENGAGE_TICKS = 12;
    /** Game ticks of heavy hits to kill a foe. */
    private static final int KILL_TICKS = 300;
    private static final ResourceLocation HP_BUFFER = RuneCraft.id("selftest_quest_hp");

    private final TestContext c;
    /** The stage the walk is at (for raising the next of several foes). */
    private QuestSpec.Stage stage;
    /** This step's foe that left the fight without dying, as a note for the step's failure, or null. */
    private String vanished;

    private QuestStepChecks(TestContext c) {
        this.c = c;
    }

    static List<SelfTest.Check> all(TestContext context) {
        QuestStepChecks w = new QuestStepChecks(context);
        List<SelfTest.Check> checks = new ArrayList<>();
        for (String id : QuestStepWalk.STEP_WALKED) {
            checks.add(new SelfTest.Check("quest:steps:" + id, () -> w.walk(id)));
        }
        return checks;
    }

    private ServerPlayer p() {
        return c.player;
    }

    private AssertionError failure(String message) {
        return new AssertionError(message);
    }

    /**
     * Puts the player at a quest place and runs its objectives ({@link QuestInterpreter#selfTestArrive}). False if the place
     * is unknown. Chunks there that never finished loading fail the step as such: a foe raised in one would be invisible, and
     * the step would otherwise fail later as a foe that never came, which points at the quest spec instead.
     */
    private boolean arrive(String where, String place, boolean underground) {
        int loading = QuestInterpreter.selfTestArrive(p(), place, underground);
        if (loading > 0) {
            throw failure(where + ": " + loading + " chunk(s) at " + place + " were still loading after "
                    + QuestInterpreter.SELF_TEST_LOAD_SECONDS + " s");
        }
        return loading == 0;
    }

    private String walk(String id) {
        QuestSpec q = QuestBook.get().spec(id);
        QuestInfo info = ContentRegistry.quest(id);
        if (q == null || info == null) {
            throw failure("no quest " + id);
        }
        c.completeQuests(info);
        PlayerData data = Players.data(p());
        data.setQuestStage(info, 0);
        data.setQuestPoints(Math.max(q.minQuestPoints, 1000));
        int qpBefore = data.questPoints();
        Map<Skill, Integer> xpBefore = new HashMap<>();
        info.xpRewards().keySet().forEach(s -> xpBefore.put(s, data.skills().xpTenths(s)));
        QuestsState state = QuestsState.get(p());
        Set<String> questItems = QuestStepWalk.questItems(q);
        // flags earned in other quests (their prerequisites are done)
        QuestStepWalk.outsideFlags(q).forEach(f -> state.setFlag(p().getUUID(), f));
        QuestStepWalk.Player view = new QuestStepWalk.Player() {
            @Override
            public boolean flag(String flag) {
                return state.hasFlag(p().getUUID(), flag);
            }

            @Override
            public int carried(String item) {
                return Inv.count(p(), item);
            }
        };
        AttributeInstance health = p().getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.removeModifier(HP_BUFFER);
            health.addTransientModifier(new AttributeModifier(HP_BUFFER, 2000, AttributeModifier.Operation.ADD_VALUE));
        }
        Mechanics.AttackListener before = Mechanics.attackListener;
        int[] attacksOnPlayer = new int[1];
        Mechanics.attackListener = (mob, target, attack) -> {
            before.onAttack(mob, target, attack);
            if (target == p()) {
                attacksOnPlayer[0]++;
            }
        };
        int steps = 0;
        int fights = 0;
        int puzzles = 0;
        int places = 0;
        List<String> notes = new ArrayList<>();
        try {
            Set<Trigger> firedHere = new LinkedHashSet<>();
            int guard = q.triggers().size() * 3 + q.stages.size() + 5;
            while (!data.questComplete(info)) {
                if (guard-- <= 0) {
                    throw failure("no completion after " + steps + " steps (now at stage " + data.questStage(info) + ")");
                }
                int s = data.questStage(info);
                if (s < 0 || s >= q.stages.size()) {
                    throw failure("stage " + s + " is outside the " + q.stages.size() + " stages");
                }
                QuestSpec.Stage stage = q.stages.get(s);
                this.stage = stage;
                Trigger t = QuestStepWalk.nextStep(stage, view, questItems, firedHere);
                if (t == null) {
                    throw failure("stage " + s + " after " + steps + " steps: no step can fire ("
                            + QuestStepWalk.whyStuck(stage, view, questItems) + ")");
                }
                String where = "stage " + s + " step " + (steps + 1) + " (" + QuestWalk.describe(t) + ", trigger "
                        + stage.triggers.indexOf(t) + ")";
                List<ItemStack> banked = prepare(t, questItems);
                Snapshot snap = new Snapshot(t, s, info, questItems);
                vanished = null;
                switch (t.kind) {
                    case TALK -> QuestInterpreter.selfTestTalk(p(), t.target);
                    case REACH -> {
                        if (!arrive(where, t.target, t.underground)) {
                            throw failure(where + ": no place " + t.target);
                        }
                        places++;
                    }
                    case PUZZLE -> {
                        if (ContentRegistry.npc(t.target) == null && !arrive(where, t.target, t.underground)) {
                            throw failure(where + ": no place or NPC " + t.target);
                        }
                        String foe = QuestPuzzles.selfTestSolve(p(), q, t);
                        bringFoesHome();
                        c.home();
                        if (foe != null) {
                            notes.add(fight(where, foe));
                            fights++;
                        }
                        puzzles++;
                    }
                    case KILL -> {
                        if (t.nearPlace != null) {
                            arrive(where, t.nearPlace, false);
                        }
                        for (int i = 0; i < Math.max(1, t.count) && !snap.fired(); i++) {
                            notes.add(fight(where, t.target));
                            fights++;
                        }
                    }
                    case DROP -> {
                        for (int i = 0; i < Math.max(60, t.chance * 8) && !snap.fired(); i++) {
                            fight(where, t.target);
                            pickUpDrops(t);
                        }
                    }
                }
                String missing = snap.missing();
                if (missing != null) {
                    // what didn't happen, then the step's state now: still ready (the talk or place fired another step), or
                    // switched off (it fired, e.g. its flag is set, but part of it - its foe - didn't come through)
                    String why = QuestInterpreter.selfTestBlocked(p(), t);
                    throw failure(where + " did not take effect (" + missing + (vanished != null ? "; " + vanished : "") + "): "
                            + (why != null ? "the step is now " + why
                            : "the step is still ready, the talk or place fired another step"));
                }
                banked.forEach(stack -> Inv.give(p(), stack)); // back from the bank
                String lost = snap.lostQuestItem();
                if (lost != null) {
                    throw failure(where + " lost the quest item " + lost + ", which it doesn't use up");
                }
                bringFoesHome();
                c.home();
                c.heal();
                steps++;
                if (data.questComplete(info) || data.questStage(info) != s) {
                    firedHere.clear();
                } else {
                    firedHere.add(t);
                }
            }
            if (attacksOnPlayer[0] == 0 && fights > 0 && c.realPlayer()) {
                throw failure("walked " + steps + " steps and " + fights + " fights, but no foe ever attacked the player");
            }
        } finally {
            Mechanics.attackListener = before;
            if (health != null) {
                health.removeModifier(HP_BUFFER);
            }
            XpService.updateMaxHealth(p());
            p().setHealth(p().getMaxHealth());
        }
        int qp = data.questPoints() - qpBefore;
        if (qp != info.questPoints()) {
            throw failure("completing gave " + qp + " quest points, expected " + info.questPoints());
        }
        for (Map.Entry<Skill, Double> e : info.xpRewards().entrySet()) {
            if (e.getValue() > 0 && data.skills().xpTenths(e.getKey()) <= xpBefore.get(e.getKey())) {
                throw failure("no " + e.getKey().displayName() + " XP reward");
            }
        }
        for (String reward : info.itemRewards()) {
            String item = reward.split(":")[0];
            if (!Inv.has(p(), item, 1)) {
                throw failure("item reward " + item + " not in the pack after completing");
            }
        }
        return "walked " + steps + " steps: " + fights + " fights, " + puzzles + " puzzles, " + places + " places, "
                + attacksOnPlayer[0] + " attacks taken" + (notes.isEmpty() ? "" : "; " + String.join(", ", new LinkedHashSet<>(notes)));
    }

    /** What a step should change, to tell afterwards whether it took effect. */
    private final class Snapshot {
        final Trigger t;
        final int stage;
        final QuestInfo info;
        final Map<String, Integer> carried = new HashMap<>();
        final boolean flagOneOfSet;
        final boolean foeUp;
        /** The quest items carried before the step: only what it takes may go (a fight's weapon swap must not cost one). */
        final Map<String, Integer> questCarried = new HashMap<>();

        Snapshot(Trigger t, int stage, QuestInfo info, Set<String> questItems) {
            this.t = t;
            this.stage = stage;
            this.info = info;
            t.give.keySet().forEach(item -> carried.put(item, Inv.count(p(), item)));
            for (String item : questItems) {
                int n = Inv.count(p(), item);
                if (n > 0) {
                    questCarried.put(item, n);
                }
            }
            QuestsState state = QuestsState.get(p());
            flagOneOfSet = t.flagOneOf.stream().anyMatch(f -> state.hasFlag(p().getUUID(), f));
            foeUp = t.spawn != null && !foes(t.spawn, 64).isEmpty();
        }

        boolean fired() {
            return missing() == null;
        }

        /** What the step should have done but didn't (its flag, its item, its foe), or null when it took effect. */
        String missing() {
            PlayerData data = Players.data(p());
            if (data.questComplete(info) || data.questStage(info) != stage) {
                return null;
            }
            QuestsState state = QuestsState.get(p());
            for (String f : t.setFlags) {
                if (!state.hasFlag(p().getUUID(), f)) {
                    return "flag " + f + " not set";
                }
            }
            if (!t.flagOneOf.isEmpty() && !flagOneOfSet && t.flagOneOf.stream().noneMatch(f -> state.hasFlag(p().getUUID(), f))) {
                return "none of " + t.flagOneOf + " set";
            }
            for (Map.Entry<String, Integer> e : carried.entrySet()) {
                int now = Inv.count(p(), e.getKey());
                if (t.kind == Trigger.Kind.DROP ? now < Math.max(1, t.count) : now <= e.getValue() - t.take.getOrDefault(e.getKey(), 0)) {
                    return "no " + e.getKey() + " received";
                }
            }
            if (t.spawn != null && !foeUp && foes(t.spawn, 64).isEmpty()) {
                return "no " + t.spawn + " raised near the player";
            }
            // a side step that sets nothing and gives nothing (a respawn, a travel) took effect when it ran
            return !t.setFlags.isEmpty() || !t.flagOneOf.isEmpty() || !t.give.isEmpty() || t.spawn != null || t.travel != null
                    ? null : "the stage did not move on";
        }

        /** A quest item the step lost although it doesn't take it (or takes fewer), or null. */
        String lostQuestItem() {
            for (Map.Entry<String, Integer> e : questCarried.entrySet()) {
                int now = Inv.count(p(), e.getKey());
                int left = e.getValue() - t.take.getOrDefault(e.getKey(), 0);
                if (now < left) {
                    return e.getKey() + " (" + e.getValue() + " before, " + now + " after)";
                }
            }
            return null;
        }
    }

    /**
     * Before a step: the pack holds only the quest items earned so far plus what this step asks the player to bring from
     * outside the quest (tools, ingredients, runes, coins). A step done empty-handed (Lady Meilyr's spin) has the player bank
     * everything first: the quest items banked are returned, to be taken back after the step.
     */
    private List<ItemStack> prepare(Trigger t, Set<String> questItems) {
        ServerPlayer p = p();
        List<ItemStack> banked = new ArrayList<>();
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack stack = p.getInventory().getItem(i);
            if (!stack.isEmpty() && !questItems.contains(Inv.idOf(stack))) {
                p.getInventory().setItem(i, ItemStack.EMPTY);
            } else if (!stack.isEmpty() && t.emptyHanded) {
                banked.add(stack.copy());
            }
        }
        if (t.emptyHanded) {
            QuestInterpreter.selfTestEmptyHands(p);
        }
        for (Map<String, Integer> items : List.of(t.need, t.take)) {
            items.forEach((item, n) -> {
                if (!questItems.contains(item)) {
                    int lack = n - Inv.count(p, item);
                    if (lack > 0) {
                        Inv.give(p, item, lack);
                    }
                }
            });
        }
        t.tools.forEach(tool -> Inv.give(p, QuestSpec.toolItem(tool), 1));
        return banked;
    }

    // ------------------------------------------------------------------ foes

    /** Living creatures of an id within {@code range} blocks of the player. */
    private List<LivingEntity> foes(String id, int range) {
        EntityType<?> type = ModEntities.typeById(id);
        List<LivingEntity> found = new ArrayList<>();
        if (type == null) {
            return found;
        }
        for (Entity e : c.world.getEntities((Entity) null, new AABB(p().blockPosition()).inflate(range),
                e -> e.getType() == type && e.isAlive() && e instanceof LivingEntity)) {
            found.add((LivingEntity) e);
        }
        return found;
    }

    /**
     * Quest foes a step raised where the player stood (a place far from the platform) are brought to the platform, each with
     * the lair it is leashed to ({@link QuestInterpreter#selfTestMoveQuestBoss}): a quest boss stays where it appeared, so
     * one moved alone is put back there, beyond the player's reach, and resets a minute later without dying.
     */
    private void bringFoesHome() {
        if (p().distanceToSqr(TestContext.PLATFORM.getX() + 0.5, TestContext.PLATFORM.getY(), TestContext.PLATFORM.getZ() + 0.5) < 32 * 32) {
            return;
        }
        int n = 0;
        for (Entity e : c.world.getEntities((Entity) null, new AABB(p().blockPosition()).inflate(48),
                e -> e instanceof DataBossEntity d && d.isAlive() && d.def() != null && d.def().questBoss())) {
            QuestInterpreter.selfTestMoveQuestBoss((DataBossEntity) e, TestContext.PLATFORM.offset(3 + n, 0, 0));
            n++;
        }
    }

    /**
     * Raises a foe the way a player meets it (already raised by a step, the summoning stone at its arena, or a creature of
     * the world) and fights it to death on the platform. Returns a short note.
     */
    private String fight(String where, String id) {
        c.home();
        LivingEntity foe = raise(where, id);
        ServerPlayer p = p();
        CombatProfile<?> profile = profile(id);
        String weapon = profile != null && profile.requiredWeapon != null ? profile.requiredWeapon : "abyssal_whip";
        String note = "";
        if (profile != null && profile.requiredWeapon != null) {
            // its rule: ordinary weapons don't hurt it
            wield(p, "abyssal_whip");
            float hp = foe.getHealth();
            BossFightChecks.heavyHit(p, foe);
            if (foe.getHealth() < hp) {
                throw failure(where + ": " + id + " took damage from an abyssal whip, but needs " + profile.requiredWeapon);
            }
            note = id + " only hurt by " + profile.requiredWeapon;
        }
        wield(p, weapon);
        int tick = 0;
        for (; tick < ENGAGE_TICKS + KILL_TICKS && foe.isAlive(); tick++) {
            for (int m = 0; m < MC; m++) {
                ActionManager.advanceForSelfTest();
                if (foe.isAlive()) {
                    if (foe instanceof Mob mob && mob.getTarget() != p) {
                        mob.setTarget(p);
                    }
                    foe.tickCount++;
                    foe.tick();
                }
                Mechanics.tickForSelfTest();
                p.setHealth(p.getMaxHealth());
            }
            if (!foe.isAlive() || tick < ENGAGE_TICKS) {
                continue;
            }
            if (!BossFightChecks.heavyHit(p, foe) && foe instanceof DataBossEntity d) {
                com.runecraft.content.bosses.BossSelfTests.assist(d, p);
            }
            if (tick % 10 == 0) {
                clearAdds(foe);
            }
        }
        if (foe.isAlive()) {
            throw failure(where + ": " + id + " still alive after " + tick + " game ticks ("
                    + Math.round(foe.getHealth() * 100 / foe.getMaxHealth()) + "% health)");
        }
        if (foe.isRemoved() && foe.getHealth() > 0) {
            // gone without dying (a lair reset, a despawn): no kill reached the quest, which the step's check then reports
            vanished = id + " vanished at " + Math.round(foe.getHealth() * 100 / foe.getMaxHealth()) + "% health without dying ("
                    + foe.getRemovalReason() + ")";
        }
        return note;
    }

    /**
     * Wields a weapon as a player does: what was in hand goes back into the pack. A quest item held there (Kruk's greegree,
     * which a player takes off to fight Kob and Keef) must not be lost to the swap.
     */
    private static void wield(ServerPlayer p, String weapon) {
        ItemStack held = p.getMainHandItem();
        p.setItemInHand(InteractionHand.MAIN_HAND, Inv.stack(weapon, 1));
        if (!held.isEmpty() && !weapon.equals(Inv.idOf(held))) {
            Inv.give(p, held);
        }
    }

    private static CombatProfile<?> profile(String id) {
        BossDef boss = ContentRegistry.boss(id);
        if (boss != null) {
            return boss;
        }
        return ContentRegistry.monster(id);
    }

    private LivingEntity raise(String where, String id) {
        List<LivingEntity> up = foes(id, 48);
        if (!up.isEmpty()) {
            return up.get(0);
        }
        EntityType<?> type = ModEntities.typeById(id);
        if (type == null) {
            throw failure(where + ": no creature with id " + id + " to fight");
        }
        BossDef boss = ContentRegistry.boss(id);
        boolean questBoss = boss != null && QuestBook.get().bosses.stream().anyMatch(b -> b.id.equals(id));
        if (questBoss) {
            if (boss.place == null) {
                // the next of several (ten mourners, two axemen): a player stands at the place or talks again, and the step that
                // raised the first raises the next
                if (stage != null) {
                    for (Trigger t : stage.triggers) {
                        if (id.equals(t.spawn) && t.kind != Trigger.Kind.KILL && QuestInterpreter.selfTestBlocked(p(), t) == null) {
                            if (t.kind == Trigger.Kind.REACH) {
                                arrive(where, t.target, t.underground);
                            } else if (t.kind == Trigger.Kind.TALK) {
                                QuestInterpreter.selfTestTalk(p(), t.target);
                            }
                            bringFoesHome();
                            c.home();
                            up = foes(id, 48);
                            if (!up.isEmpty()) {
                                return up.get(0);
                            }
                        }
                    }
                }
                throw failure(where + ": nothing raised " + id + " (no earlier step spawned it and it has no arena)");
            }
            // the summoning stone at its arena: it asks the quest whether the player may fight it now
            DataBossService.summon(p(), c.world, c.near(0, -4), boss);
            up = foes(id, 48);
            if (up.isEmpty()) {
                throw failure(where + ": the summoning stone would not raise " + id);
            }
            return up.get(0);
        }
        Entity e = type.create(c.world);
        if (!(e instanceof LivingEntity living)) {
            throw failure(where + ": " + id + " is not a creature");
        }
        e.moveTo(TestContext.PLATFORM.getX() + 3.5, TestContext.PLATFORM.getY(), TestContext.PLATFORM.getZ() + 0.5, 0f, 0f);
        if (e instanceof Mob mob) {
            mob.finalizeSpawn(c.world, c.world.getCurrentDifficultyAt(e.blockPosition()), MobSpawnType.EVENT, null);
        }
        c.world.addFreshEntity(e);
        return living;
    }

    /** Kills what a foe has raised around it (minions, spawns) so its kill isn't held up. */
    private void clearAdds(LivingEntity foe) {
        for (LivingEntity e : c.world.getEntitiesOfClass(LivingEntity.class, new AABB(TestContext.PLATFORM).inflate(40),
                e -> e.isAlive() && e != foe && !(e instanceof Player) && !(e instanceof DataBossEntity))) {
            CombatHandler.applyDamage(p(), e, Math.max(1, (int) Math.ceil(e.getHealth())));
            if (e.isAlive()) {
                e.discard();
            }
        }
    }

    /** A drop objective's item lying on the ground is picked up. */
    private void pickUpDrops(Trigger t) {
        for (ItemEntity e : c.world.getEntitiesOfClass(ItemEntity.class, new AABB(TestContext.PLATFORM).inflate(40), e -> true)) {
            String id = Inv.idOf(e.getItem());
            if (t.give.containsKey(id)) {
                Inv.give(p(), e.getItem().copy());
                e.discard();
            }
        }
    }
}
