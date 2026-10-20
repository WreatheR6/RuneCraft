package com.runecraft.selftest;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.combat.CombatHandler;
import com.runecraft.content.quests.QuestInterpreter;
import com.runecraft.content.quests.QuestPuzzles;
import com.runecraft.content.quests.QuestsState;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.QuestInfo;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.quests.QuestBook;
import com.runecraft.osrs.content.quests.QuestSpec;
import com.runecraft.osrs.content.quests.QuestSpec.Trigger;
import com.runecraft.osrs.content.quests.QuestStrictWalk;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModEntities;
import com.runecraft.util.Inv;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/**
 * Master and late experienced quests walked step by step by the player ({@code quest:walk:<id>}): the plan of
 * {@link QuestStrictWalk} carried out with real actions. The player talks to each NPC through the real dialogue
 * dispatcher, goes to every place (the reach objectives run where the player stands), raises each quest boss the way the
 * game does (its summoning stone's requirement, or the step that calls it up) and kills it through its own damage rules,
 * kills creatures until their quest drop falls and picks it up, and answers each puzzle through its chat links. Only
 * outside supplies (logs, bars, runes, a cake) are handed over; quest items and flags come from the quest itself. A step
 * whose effect doesn't happen (stage, flag, item) fails the check with what the game said instead. Runs with the fake
 * player on the server and with the real player in the client walkthrough.
 */
final class QuestWalkChecks {
    /** Most heavy hits one kill may take. */
    private static final int MAX_HITS = 80;

    private final TestContext c;

    private QuestWalkChecks(TestContext c) {
        this.c = c;
    }

    static List<SelfTest.Check> all(TestContext context) {
        QuestWalkChecks w = new QuestWalkChecks(context);
        List<SelfTest.Check> checks = new ArrayList<>();
        for (String id : QuestStrictWalk.WALKED) {
            checks.add(new SelfTest.Check("quest:walk:" + id, () -> w.walk(id)));
        }
        return checks;
    }

    private ServerPlayer p() {
        return c.player;
    }

    private String walk(String id) {
        QuestSpec q = QuestBook.get().spec(id);
        QuestInfo info = ContentRegistry.quest(id);
        if (q == null || info == null) {
            throw new AssertionError("no quest " + id);
        }
        ServerPlayer p = p();
        PlayerData data = Players.data(p);
        c.completeQuests(info);
        data.setQuestStage(info, 0);
        data.setQuestPoints(1000);
        int qpBefore = data.questPoints();
        QuestStrictWalk.Plan plan = QuestStrictWalk.plan(q, p.getStringUUID());
        if (!plan.completes()) {
            throw new AssertionError("no walk for this player: " + plan.stuck());
        }
        Set<String> questItems = QuestStrictWalk.questItems(q);
        Set<String> kept = QuestStrictWalk.givenItems(q);
        int places = 0;
        int kills = 0;
        int puzzles = 0;
        try {
            for (int i = 0; i < plan.steps().size(); i++) {
                QuestStrictWalk.Step step = plan.steps().get(i);
                Trigger t = step.fired();
                String where = "step " + (i + 1) + "/" + plan.steps().size() + " (" + step + ")";
                int stageBefore = data.questStage(info);
                supply(t, questItems, kept);
                QuestsState state = QuestsState.get(p);
                step.outsideFlags().forEach(f -> state.setFlag(p.getUUID(), f));
                switch (step.kind()) {
                    case TALK -> QuestInterpreter.selfTestTalk(p, step.target());
                    case REACH -> {
                        goTo(t.target, t.underground);
                        places++;
                        QuestInterpreter.selfTestReach(p);
                    }
                    case KILL -> {
                        for (int k = 0; k < Math.max(1, step.count()); k++) {
                            slay(t.target, t, where, k > 0);
                            kills++;
                        }
                    }
                    case DROP -> kills += farm(t, where);
                    case PUZZLE -> {
                        if (ContentRegistry.npc(t.target) == null) {
                            goTo(t.target, t.underground);
                            places++;
                            QuestInterpreter.selfTestReach(p);
                        }
                        String foe = QuestPuzzles.selfTestSolve(p, q, t);
                        if (foe != null) {
                            slay(foe, null, where, false);
                            kills++;
                        }
                        puzzles++;
                    }
                }
                check(info, q, t, step, stageBefore, where);
                c.heal();
            }
        } finally {
            leave();
            c.home();
        }
        if (!data.questComplete(info)) {
            throw new AssertionError("walked every step but the quest isn't complete (stage " + data.questStage(info) + ")");
        }
        int qp = data.questPoints() - qpBefore;
        if (qp != info.questPoints()) {
            throw new AssertionError("completing gave " + qp + " quest points, expected " + info.questPoints());
        }
        return plan.steps().size() + " steps: " + kills + " kills, " + places + " places, " + puzzles + " puzzles";
    }

    /**
     * Hands over the outside supplies a step needs, after putting away the last step's (as the plan does); quest items
     * stay and must already be carried.
     */
    private void supply(Trigger t, Set<String> questItems, Set<String> kept) {
        for (int slot = 0; slot < p().getInventory().getContainerSize(); slot++) {
            String id = Inv.idOf(p().getInventory().getItem(slot));
            if (id != null && !kept.contains(id)) {
                p().getInventory().setItem(slot, net.minecraft.world.item.ItemStack.EMPTY);
            }
        }
        for (Map<String, Integer> m : List.of(t.need, t.take)) {
            m.forEach((item, n) -> {
                if (!questItems.contains(item)) {
                    int have = Inv.count(p(), item);
                    if (have < n) {
                        Inv.give(p(), item, n - have);
                    }
                }
            });
        }
        for (String tool : t.tools) {
            String item = QuestSpec.toolItem(tool);
            if (Inv.count(p(), item) == 0) {
                Inv.give(p(), item, 1);
            }
        }
    }

    /** What firing the step's trigger must have done: the stage moved, its flags set, its items handed over. */
    private void check(QuestInfo info, QuestSpec q, Trigger t, QuestStrictWalk.Step step, int stageBefore, String where) {
        PlayerData data = Players.data(p());
        List<String> wrong = new ArrayList<>();
        if (t.completes()) {
            if (!data.questComplete(info)) {
                wrong.add("the quest did not complete");
            }
        } else {
            int expected = t.next != t.stage ? t.next : Math.max(1, stageBefore);
            if (data.questStage(info) != expected) {
                wrong.add("stage is " + data.questStage(info) + ", expected " + expected);
            }
        }
        QuestsState state = QuestsState.get(p());
        for (String f : t.setFlags) {
            if (!state.hasFlag(p().getUUID(), f)) {
                wrong.add("flag " + f + " not set");
            }
        }
        for (Map.Entry<String, Integer> e : t.give.entrySet()) {
            int n = t.kind == Trigger.Kind.DROP ? Math.max(1, t.count) : 1;
            if (!Inv.has(p(), e.getKey(), n)) {
                wrong.add("no " + e.getKey() + (n > 1 ? " x" + n : "") + " carried");
            }
        }
        if (!wrong.isEmpty()) {
            throw new AssertionError(where + " in stage " + stageBefore + " (\"" + q.stages.get(Math.max(0, Math.min(stageBefore,
                    q.stages.size() - 1))).journal + "\"): " + String.join(", ", wrong));
        }
    }

    // ------------------------------------------------------------------ places

    /** Block centre of a place (a Landmark name or a PlaceDef id), or null. */
    private static int[] centre(String place) {
        Landmark l = Landmark.byName(place);
        if (l != null) {
            return new int[]{l.blockX(), l.blockZ()};
        }
        PlaceDef d = ContentRegistry.place(place);
        return d == null ? null : new int[]{d.blockX(), d.blockZ()};
    }

    /** Goes to a place: onto the ground there, or into the dungeon below it for an underground objective. */
    private void goTo(String place, boolean underground) {
        int[] at = centre(place);
        if (at == null) {
            throw new AssertionError("no place " + place + " to go to");
        }
        leave();
        c.world.getChunk(at[0] >> 4, at[1] >> 4);
        int y = underground ? 40 : c.world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at[0], at[1]) + 1;
        p().teleportTo(c.world, at[0] + 0.5, y, at[1] + 0.5, p().getYRot(), p().getXRot());
    }

    /** Clears what the walk leaves where the player stands: quest bosses a step called up, and ground loot. */
    private void leave() {
        AABB box = p().getBoundingBox().inflate(40);
        for (DataBossEntity boss : c.world.getEntitiesOfClass(DataBossEntity.class, box,
                e -> e.isAlive() && e.def() != null && "Quest bosses".equals(e.def().category))) {
            boss.discard();
        }
        sweep(Set.of());
    }

    // ------------------------------------------------------------------ fights

    /** The creature ids a living entity answers to for a quest kill (as QuestInterpreter matches them). */
    private static Set<String> ids(LivingEntity e) {
        Set<String> ids = new HashSet<>();
        if (e instanceof DataBossEntity d && d.def() != null) {
            ids.add(d.def().id);
        }
        BossDef boss = ModEntities.dataBoss(e.getType());
        if (boss != null) {
            ids.add(boss.id);
        }
        String key = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();
        ids.add(key);
        if (key.startsWith("runecraft:")) {
            ids.add(key.substring("runecraft:".length()));
        }
        ids.add(e.getName().getString().toLowerCase(Locale.ROOT).replace(' ', '_').replace("'", ""));
        return ids;
    }

    private LivingEntity alive(String creature) {
        AABB box = p().getBoundingBox().inflate(40);
        for (LivingEntity e : c.world.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !(e instanceof Player))) {
            if (ids(e).contains(creature)) {
                return e;
            }
        }
        return null;
    }

    /**
     * Raises and kills one {@code creature}: one already up near the player (a step called it up), else a quest boss from
     * its summoning stone (which asks the quest whether the player may fight it), else the creature itself.
     */
    private void slay(String creature, Trigger t, String where, boolean again) {
        if (t != null && t.nearPlace != null) {
            goTo(t.nearPlace, t.underground);
        }
        LivingEntity foe = again ? null : alive(creature);
        if (foe == null) {
            if (t == null || t.nearPlace == null) {
                c.home();
            }
            foe = raise(creature, where);
        }
        kill(foe, where);
    }

    private LivingEntity raise(String creature, String where) {
        BlockPos at = p().blockPosition().relative(p().getDirection(), 3);
        BossDef boss = ContentRegistry.boss(creature);
        if (boss != null && ModEntities.DATA_BOSS_TYPES.containsKey(creature)) {
            DataBossService.summon(p(), c.world, at, boss);
            LivingEntity up = alive(creature);
            if (up == null) {
                throw new AssertionError(where + ": the summoning stone would not raise " + boss.name + " (see the game messages)");
            }
            return up;
        }
        EntityType<?> type = ModEntities.typeById(creature);
        if (type == null || (creature.contains(":") && BuiltInRegistries.ENTITY_TYPE.getKey(type).toString().equals("minecraft:pig")
                && !creature.equals("minecraft:pig"))) {
            throw new AssertionError(where + ": no creature " + creature + " can be spawned");
        }
        Entity e = type.create(c.world);
        if (!(e instanceof LivingEntity living)) {
            throw new AssertionError(where + ": " + creature + " is not a creature");
        }
        living.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
        if (living instanceof Mob mob) {
            mob.finalizeSpawn(c.world, c.world.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
        }
        c.world.addFreshEntity(living);
        return living;
    }

    /** Heavy hits through the creature's own damage rules (melee, then magic, then ranged) until it dies. */
    private void kill(LivingEntity foe, String where) {
        String name = foe.getName().getString();
        for (int hit = 0; hit < MAX_HITS && foe.isAlive(); hit++) {
            if (!heavyHit(foe)) {
                if (foe instanceof DataBossEntity d && !d.script().isVulnerable(d)) {
                    clearShield(d);
                    continue;
                }
                throw new AssertionError(where + ": nothing the player does hurts " + name);
            }
        }
        if (foe.isAlive()) {
            throw new AssertionError(where + ": " + name + " still alive after " + MAX_HITS + " heavy hits");
        }
        // the creature's own loot is left behind; quest drops are picked up by farm()
        sweep(Set.of());
    }

    /** Cuts down whatever shields an invulnerable boss (Hespori's flowers): the other creatures close to it. */
    private void clearShield(DataBossEntity boss) {
        for (LivingEntity m : c.world.getEntitiesOfClass(LivingEntity.class, boss.getBoundingBox().inflate(16),
                e -> e != boss && e.isAlive() && e instanceof OsrsMonsterEntity)) {
            for (int hit = 0; hit < 20 && m.isAlive(); hit++) {
                if (!heavyHit(m)) {
                    break;
                }
            }
        }
    }

    private boolean heavyHit(LivingEntity foe) {
        if (!(foe instanceof OsrsMonsterEntity monster)) {
            return CombatHandler.applyDamage(p(), foe, Math.max(20, (int) Math.ceil(foe.getMaxHealth() / 4))) > 0;
        }
        int hp = foe instanceof DataBossEntity d ? d.trueHitpoints() : Math.round(foe.getMaxHealth());
        int big = Math.max(30, hp / 5);
        for (AttackType type : new AttackType[]{AttackType.SLASH, AttackType.MAGIC, AttackType.RANGED, AttackType.CRUSH, AttackType.STAB}) {
            int dmg = monster.modifyIncoming(p(), big, type);
            if (dmg > 0) {
                CombatHandler.applyDamage(p(), foe, dmg);
                return true;
            }
        }
        return false;
    }

    /** Kills the drop step's creature until its quest item has fallen (as often as needed) and picks it up. */
    private int farm(Trigger t, String where) {
        String item = t.give.keySet().iterator().next();
        int need = Math.max(1, t.count);
        int tries = Math.min(600, Math.max(20, t.chance * need * 15));
        int n = 0;
        while (Inv.count(p(), item) < need && n < tries) {
            if (t.nearPlace != null) {
                goTo(t.nearPlace, t.underground);
            } else {
                c.home();
            }
            LivingEntity foe = raise(t.target, where);
            String name = foe.getName().getString();
            for (int hit = 0; hit < MAX_HITS && foe.isAlive(); hit++) {
                if (!heavyHit(foe)) {
                    throw new AssertionError(where + ": nothing the player does hurts " + name);
                }
            }
            n++;
            sweep(Set.of(item));
        }
        if (Inv.count(p(), item) < need) {
            throw new AssertionError(where + ": " + n + " kills of " + t.target + " dropped " + Inv.count(p(), item) + "/" + need + " " + item);
        }
        return n;
    }

    /** Picks up the dropped items of these ids around the player and clears the rest of the ground loot. */
    private void sweep(Set<String> pickUp) {
        AABB box = p().getBoundingBox().inflate(16);
        for (ItemEntity drop : c.world.getEntitiesOfClass(ItemEntity.class, box, e -> e.isAlive())) {
            String id = Inv.idOf(drop.getItem());
            if (pickUp.contains(id)) {
                Inv.give(p(), drop.getItem().copy());
            }
            drop.discard();
        }
    }
}
