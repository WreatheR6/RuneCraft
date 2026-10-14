package com.runecraft.content.quests;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.combat.MonsterStats;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.QuestInfo;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.quests.QuestBook;
import com.runecraft.osrs.content.quests.QuestSpec;
import com.runecraft.osrs.content.quests.QuestSpec.Trigger;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.quest.QuestScripts;
import com.runecraft.registry.ModEntities;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import com.runecraft.world.LandmarkBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Runs the declarative {@link QuestSpec} state machines: NPC dialogue, kill objectives, quest-item
 * drops, "reach this place" objectives, quest-boss gating, shops and post-quest chatter.
 */
public final class QuestInterpreter {
    private static QuestBook book;
    /** Last time (server ticks) a player was told why a "reach" objective didn't trigger. */
    private static final Map<java.util.UUID, Integer> REACH_NOTICE = new java.util.HashMap<>();

    private QuestInterpreter() {
    }

    static void init() {
        book = QuestBook.get();
        for (NpcDef npc : book.npcs) {
            String id = npc.id();
            QuestScripts.onTalk(id, (player, entity) -> talk(player, id));
        }
        QuestScripts.onKill(QuestInterpreter::onKill);
        for (BossDef boss : book.bosses) {
            DataBossService.requirement(boss.id, (player, def) -> canFight(player, def.id));
        }
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 20 == 0) {
                reachTick(server);
            }
        });
    }

    /** Self-test hook ({@code com.runecraft.selftest}): talks to a quest NPC through the real dialogue dispatcher. */
    public static void selfTestTalk(ServerPlayerEntity p, String npcId) {
        talk(p, npcId);
    }

    /** Self-test hook: executes a trigger as if its objective (reach a place, a drop) had just happened. */
    public static void selfTestFire(ServerPlayerEntity p, QuestSpec q, Trigger t) {
        fire(p, q, t, npcName(t.target));
    }

    // ---------------------------------------------------------------- State helpers

    static int stage(ServerPlayerEntity p, QuestSpec q) {
        return QuestApi.stage(p, q.id());
    }

    static boolean active(ServerPlayerEntity p, QuestSpec q) {
        int s = stage(p, q);
        return s > 0 && s < q.stages.size();
    }

    static boolean complete(ServerPlayerEntity p, String questId) {
        QuestInfo q = ContentRegistry.quest(questId);
        return q != null && Players.data(p).questComplete(q);
    }

    static boolean flag(ServerPlayerEntity p, String flag) {
        return QuestsState.get(p).hasFlag(p.getUuid(), flag);
    }

    /** True when the trigger should be ignored entirely (already done / not applicable). */
    static boolean skipped(ServerPlayerEntity p, Trigger t) {
        for (String item : t.unlessItems) {
            if (Inv.has(p, item, 1)) {
                return true;
            }
        }
        for (String f : t.unlessFlags) {
            if (flag(p, f)) {
                return true;
            }
        }
        if (t.kind == Trigger.Kind.DROP) {
            for (String item : t.give.keySet()) {
                if (Inv.has(p, item, 1)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** First unmet condition as a short reason, or null when every condition is met. */
    static String unmet(ServerPlayerEntity p, Trigger t) {
        PlayerData data = Players.data(p);
        if (t.minQuestPoints > 0 && data.questPoints() < t.minQuestPoints) {
            return "You need " + t.minQuestPoints + " quest points.";
        }
        if (t.minCombat > 0 && data.skills().combatLevel() < t.minCombat) {
            return "You need a combat level of " + t.minCombat + ".";
        }
        for (Map.Entry<Skill, Integer> e : t.skills.entrySet()) {
            if (data.skills().level(e.getKey()) < e.getValue()) {
                return "You need " + Msg.article(e.getKey().displayName()) + " level of " + e.getValue() + ".";
            }
        }
        for (String q : t.quests) {
            if (!complete(p, q)) {
                QuestInfo info = ContentRegistry.quest(q);
                return "You need to complete " + (info == null ? q : info.displayName()) + ".";
            }
        }
        for (String f : t.needFlags) {
            if (!flag(p, f)) {
                return "";
            }
        }
        for (Map.Entry<String, Integer> e : t.need.entrySet()) {
            if (!Inv.has(p, e.getKey(), e.getValue())) {
                return "You need " + describe(e.getKey(), e.getValue()) + ".";
            }
        }
        for (Map.Entry<String, Integer> e : t.take.entrySet()) {
            if (!Inv.has(p, e.getKey(), e.getValue())) {
                return "You need " + describe(e.getKey(), e.getValue()) + ".";
            }
        }
        for (String tool : t.tools) {
            if (!hasTool(p, tool)) {
                return "You need " + QuestSpec.toolName(tool) + ".";
            }
        }
        return null;
    }

    /** Whether the player carries (or wields) any tool meeting a requirement such as "pickaxe" or "axe:mithril". */
    static boolean hasTool(ServerPlayerEntity p, String tool) {
        for (int i = 0; i < p.getInventory().size(); i++) {
            if (QuestSpec.toolSatisfies(tool, com.runecraft.registry.ModItems.def(p.getInventory().getStack(i)))) {
                return true;
            }
        }
        return false;
    }

    static String describe(String item, int count) {
        String name = item;
        com.runecraft.osrs.ItemDef def = com.runecraft.osrs.ItemCatalog.get(item);
        if (def != null) {
            name = def.name();
        } else {
            name = item.substring(item.indexOf(':') + 1).replace('_', ' ');
        }
        return count > 1 ? count + " x " + name : Msg.article(name.toLowerCase(Locale.ROOT));
    }

    static String npcName(String npcId) {
        NpcDef def = ContentRegistry.npc(npcId);
        return def == null ? "Stranger" : def.name();
    }

    // ---------------------------------------------------------------- Dialogue

    static void speak(ServerPlayerEntity p, String speaker, List<String> lines) {
        for (String line : lines) {
            if (line.startsWith("> ")) {
                QuestApi.you(p, line.substring(2));
            } else if (line.startsWith("* ") || speaker.isEmpty()) {
                String text = line.startsWith("* ") ? line.substring(2) : line;
                p.sendMessage(Text.literal(text).formatted(Formatting.GRAY, Formatting.ITALIC), false);
            } else if (line.startsWith("@") && line.contains(": ")) {
                int colon = line.indexOf(": ");
                QuestApi.say(p, line.substring(1, colon), line.substring(colon + 2));
            } else {
                QuestApi.say(p, speaker, line);
            }
        }
    }

    static void talk(ServerPlayerEntity p, String npcId) {
        String name = npcName(npcId);
        // 1. Objectives of quests in progress (the first satisfiable one across all active quests).
        List<QuestSpec> owners = new ArrayList<>();
        List<Trigger> here = new ArrayList<>();
        for (QuestSpec q : book.quests) {
            if (!active(p, q)) {
                continue;
            }
            for (Trigger t : q.stages.get(stage(p, q)).triggers) {
                if (t.kind == Trigger.Kind.TALK && t.target.equals(npcId) && !skipped(p, t)) {
                    owners.add(q);
                    here.add(t);
                }
            }
        }
        if (!here.isEmpty()) {
            for (int i = 0; i < here.size(); i++) {
                if (unmet(p, here.get(i)) == null) {
                    fire(p, owners.get(i), here.get(i), name);
                    return;
                }
            }
            Trigger main = here.stream().filter(t -> t.next != t.stage).findFirst().orElse(here.get(0));
            showMissing(p, main, name);
            return;
        }
        // 2. Quests this NPC starts.
        for (QuestSpec q : book.quests) {
            if (stage(p, q) != 0) {
                continue;
            }
            for (Trigger t : q.stages.get(0).triggers) {
                if (t.kind != Trigger.Kind.TALK || !t.target.equals(npcId) || skipped(p, t)) {
                    continue;
                }
                if (q.minQuestPoints > 0 && Players.data(p).questPoints() < q.minQuestPoints) {
                    QuestApi.say(p, name, "Come back when you're more experienced, adventurer.");
                    Msg.game(p, "You need " + q.minQuestPoints + " quest points to start " + q.def.displayName() + ".");
                    return;
                }
                String reason = unmet(p, t);
                if (reason != null) {
                    showMissing(p, t, name);
                    return;
                }
                if (!QuestApi.start(p, q.id())) {
                    QuestApi.say(p, name, "You don't look ready for what I have in mind. Come back later.");
                    return;
                }
                fire(p, q, t, name);
                return;
            }
        }
        // 3. Hints for the current stage of an active quest.
        for (QuestSpec q : book.quests) {
            if (active(p, q)) {
                List<String> hint = q.stages.get(stage(p, q)).hints.get(npcId);
                if (hint != null) {
                    speak(p, name, hint);
                    return;
                }
            }
        }
        // 4. Shops.
        if (QuestShops.offer(p, npcId, name)) {
            return;
        }
        // 5. Post-quest lines.
        for (QuestSpec q : book.quests) {
            List<String> done = q.doneLines.get(npcId);
            if (done != null && complete(p, q.id())) {
                speak(p, name, done);
                return;
            }
        }
        QuestApi.say(p, name, "Hello there, adventurer.");
    }

    static void showMissing(ServerPlayerEntity p, Trigger t, String name) {
        if (!t.missing.isEmpty()) {
            speak(p, name, t.missing);
        } else {
            QuestApi.say(p, name, "Come back when you're ready.");
        }
        String reason = unmet(p, t);
        if (reason != null && !reason.isEmpty()) {
            Msg.game(p, reason);
        }
    }

    /** Executes a trigger whose conditions are met. */
    static void fire(ServerPlayerEntity p, QuestSpec q, Trigger t, String speaker) {
        if (!t.take.isEmpty() && !QuestApi.takeAll(p, t.take)) {
            return;
        }
        speak(p, speaker, t.lines);
        t.give.forEach((item, n) -> Inv.give(p, item, n));
        t.xp.forEach((skill, xp) -> XpService.add(p, skill, xp));
        QuestsState state = QuestsState.get(p);
        t.setFlags.forEach(f -> state.setFlag(p.getUuid(), f));
        if (t.spawn != null) {
            spawnNear(p, t.spawn);
        }
        if (t.travel != null) {
            travel(p, t.travel);
        }
        if (t.completes()) {
            QuestApi.complete(p, q.id());
            for (String unlock : q.unlocks) {
                p.sendMessage(Text.literal("Unlocked: " + unlock).formatted(Formatting.GOLD), false);
            }
        } else if (t.next != t.stage) {
            QuestApi.setStage(p, q.id(), t.next);
            p.sendMessage(Text.literal("[" + q.def.displayName() + "] " + q.stages.get(t.next).journal).formatted(Formatting.DARK_AQUA), false);
        }
        ServerNetworking.sync(p);
    }

    // ---------------------------------------------------------------- Kills, drops, places, bosses

    /** Identifiers a killed creature can be matched by: module id, entity id and snake_case name. */
    static List<String> ids(LivingEntity entity, MonsterStats stats) {
        List<String> ids = new ArrayList<>();
        if (stats.profile() != null) {
            ids.add(stats.profile().id);
        }
        if (stats.monster() != null) {
            ids.add(stats.monster().entity());
        }
        ids.add(Registries.ENTITY_TYPE.getId(entity.getType()).toString());
        ids.add(stats.name().toLowerCase(Locale.ROOT).replace(' ', '_').replace("'", ""));
        return ids;
    }

    static void onKill(ServerPlayerEntity p, LivingEntity entity, MonsterStats stats) {
        List<String> ids = ids(entity, stats);
        for (QuestSpec q : book.quests) {
            if (!active(p, q)) {
                continue;
            }
            QuestSpec.Stage stage = q.stages.get(stage(p, q));
            for (Trigger t : stage.triggers) {
                if (!ids.contains(t.target) || skipped(p, t)) {
                    continue;
                }
                if (t.kind == Trigger.Kind.DROP) {
                    if (unmet(p, t) == null && p.getRandom().nextInt(Math.max(1, t.chance)) == 0) {
                        t.give.forEach((item, n) -> entity.dropStack(Inv.stack(item, n)));
                    }
                } else if (t.kind == Trigger.Kind.KILL && unmet(p, t) == null) {
                    fire(p, q, t, stats.name());
                    return;
                }
            }
        }
    }

    /** Whether a summoning stone may raise this quest boss for the player. */
    static boolean canFight(ServerPlayerEntity p, String bossId) {
        for (QuestSpec q : book.quests) {
            if (!active(p, q)) {
                continue;
            }
            for (Trigger t : q.stages.get(stage(p, q)).triggers) {
                if (t.kind == Trigger.Kind.KILL && t.target.equals(bossId) && !skipped(p, t)) {
                    String reason = unmet(p, t);
                    if (reason == null) {
                        return true;
                    }
                    if (!reason.isEmpty()) {
                        Msg.game(p, reason);
                    } else {
                        Msg.game(p, "You're not ready to face this foe yet.");
                    }
                    return false;
                }
            }
        }
        BossDef def = ContentRegistry.boss(bossId);
        Msg.game(p, "You have no reason to disturb " + (def == null ? "this creature" : def.name) + " right now.");
        return false;
    }

    static void spawnNear(ServerPlayerEntity p, String bossId) {
        BossDef def = ContentRegistry.boss(bossId);
        EntityType<DataBossEntity> type = ModEntities.DATA_BOSS_TYPES.get(bossId);
        if (def == null || type == null || !(p.getWorld() instanceof ServerWorld world)) {
            return;
        }
        if (!world.getEntitiesByType(type, new Box(p.getBlockPos()).expand(32), e -> e.isAlive()).isEmpty()) {
            Msg.game(p, def.name + " is already nearby!");
            return;
        }
        BlockPos at = p.getBlockPos().offset(p.getHorizontalFacing(), 4);
        DataBossService.spawn(world, at, def, p);
        Msg.red(p, def.name + " (level-" + def.combatLevel + ") appears!");
    }

    static int[] placeCentre(String place) {
        Landmark l = Landmark.byName(place);
        if (l != null) {
            return new int[]{l.blockX(), l.blockZ()};
        }
        PlaceDef d = ContentRegistry.place(place);
        return d == null ? null : new int[]{d.blockX(), d.blockZ()};
    }

    static void travel(ServerPlayerEntity p, String place) {
        Landmark l = Landmark.byName(place);
        if (l != null) {
            Gielinor.teleport(p, l);
            return;
        }
        PlaceDef d = ContentRegistry.place(place);
        ServerWorld world = Gielinor.world(p.getServer());
        if (d == null || world == null) {
            return;
        }
        LandmarkBuilder.buildNow(world, d);
        BlockPos pos = Gielinor.safeSpot(world, d.blockX() + 2, d.blockZ() + 2);
        p.teleport(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, p.getYaw(), p.getPitch());
        Msg.game(p, "You arrive at " + d.name() + ".");
    }

    static void reachTick(MinecraftServer server) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (!Gielinor.is(p) || p.isSpectator()) {
                continue;
            }
            // place objectives of the hand-scripted quests (Restless Ghost altar, cadava bushes...)
            com.runecraft.quest.QuestManager.spotTick(p);
            for (QuestSpec q : book.quests) {
                if (!active(p, q)) {
                    continue;
                }
                for (Trigger t : q.stages.get(stage(p, q)).triggers) {
                    if (t.kind != Trigger.Kind.REACH || skipped(p, t)) {
                        continue;
                    }
                    int[] c = placeCentre(t.target);
                    if (c == null) {
                        continue;
                    }
                    double dx = p.getX() - c[0];
                    double dz = p.getZ() - c[1];
                    if (dx * dx + dz * dz > t.radius * t.radius) {
                        continue;
                    }
                    String reason = unmet(p, t);
                    if (reason == null) {
                        fire(p, q, t, "");
                        break;
                    }
                    int last = REACH_NOTICE.getOrDefault(p.getUuid(), -10000);
                    if (server.getTicks() - last > 600) {
                        REACH_NOTICE.put(p.getUuid(), server.getTicks());
                        if (!t.missing.isEmpty()) {
                            speak(p, "", t.missing);
                        }
                        if (!reason.isEmpty()) {
                            Msg.game(p, "[" + q.def.displayName() + "] " + reason);
                        }
                    }
                }
            }
        }
    }
}
