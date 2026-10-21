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
import com.runecraft.platform.ServerTickEvents;
import com.runecraft.platform.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
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
            if (server.getTickCount() % 20 == 0) {
                reachTick(server);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> QuestPuzzles.forget(handler.player.getUUID()));
    }

    /** Self-test hook ({@code com.runecraft.selftest}): talks to a quest NPC through the real dialogue dispatcher. */
    public static void selfTestTalk(ServerPlayer p, String npcId) {
        talk(p, npcId);
    }

    /** Self-test hook: executes a trigger as if its objective (reach a place, a drop) had just happened. */
    public static void selfTestFire(ServerPlayer p, QuestSpec q, Trigger t) {
        fire(p, q, t, npcName(t.target));
    }

    // ---------------------------------------------------------------- State helpers

    static int stage(ServerPlayer p, QuestSpec q) {
        return QuestApi.stage(p, q.id());
    }

    static boolean active(ServerPlayer p, QuestSpec q) {
        int s = stage(p, q);
        return s > 0 && s < q.stages.size();
    }

    static boolean complete(ServerPlayer p, String questId) {
        QuestInfo q = ContentRegistry.quest(questId);
        return q != null && Players.data(p).questComplete(q);
    }

    static boolean flag(ServerPlayer p, String flag) {
        return QuestsState.get(p).hasFlag(p.getUUID(), flag);
    }

    /** True when the trigger should be ignored entirely (already done / not applicable). */
    static boolean skipped(ServerPlayer p, Trigger t) {
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
                // a counted drop (5 mudskipper hides) keeps rolling until the player carries enough
                if (Inv.has(p, item, Math.max(1, t.count))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** First unmet condition as a short reason, or null when every condition is met. */
    static String unmet(ServerPlayer p, Trigger t) {
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
            if (!Inv.has(p, e.getKey(), e.getValue()) && !(e.getValue() == 1 && wears(p, e.getKey()))) {
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
        if (t.emptyHanded && !emptyHanded(p)) {
            return "You need an empty inventory and nothing worn.";
        }
        if (t.nearPlace != null && !near(p, t.nearPlace, t.nearRadius)) {
            return "";
        }
        return null;
    }

    /** Below this block y a player counts as underground (carved dungeons lie below y 56). */
    static final int UNDERGROUND_Y = 56;

    /** Whether the player is within {@code tiles} OSRS tiles (horizontally) of a place's centre. */
    static boolean near(ServerPlayer p, String place, int tiles) {
        int[] c = placeCentre(place);
        if (c == null) {
            return false;
        }
        double dx = p.getX() - c[0];
        double dz = p.getZ() - c[1];
        double r = tiles * (double) com.runecraft.osrs.GielinorMap.SCALE;
        return dx * dx + dz * dz <= r * r;
    }

    /**
     * Whether the player wears {@code id} in a slot the inventory doesn't hold: the systems module's ring, neck, hands and cape
     * slots or a quest gear slot. A needed item counts worn too (a ghostspeak amulet, the leather gloves for the Witch's House
     * gate, which OSRS wants equipped).
     */
    static boolean wears(ServerPlayer p, String id) {
        return com.runecraft.content.systems.SysState.of(p).wearing(id) || QuestsState.get(p).wornAll(p.getUUID()).containsValue(id);
    }

    /** Nothing carried and nothing worn: inventory, armour and off-hand, and the virtual ring, neck, hands and cape slots. */
    static boolean emptyHanded(ServerPlayer p) {
        if (!p.getInventory().isEmpty()) {
            return false;
        }
        for (net.minecraft.world.item.ItemStack worn : com.runecraft.content.systems.SysState.of(p).worn.values()) {
            if (worn != null && !worn.isEmpty()) {
                return false;
            }
        }
        return QuestsState.get(p).wornAll(p.getUUID()).isEmpty();
    }

    /** Sends everything the player carries and wears, quest gear slots included, to their bank (a quest deposit service). */
    static void bankEverything(ServerPlayer p) {
        com.runecraft.bank.BankService.depositAll(p);
        QuestsState state = QuestsState.get(p);
        com.runecraft.osrs.bank.BankModel bank = Players.data(p).bank();
        for (Map.Entry<String, String> e : new ArrayList<>(state.wornAll(p.getUUID()).entrySet())) {
            if (e.getValue() != null && bank.deposit(e.getValue(), "", 1, 0) > 0) {
                state.wear(p.getUUID(), e.getKey(), null);
            }
        }
        ServerNetworking.sync(p);
    }

    /** Self-test hook: empties the player's hands for an {@code emptyHanded} trigger (inventory and every worn slot). */
    public static void selfTestEmptyHands(ServerPlayer p) {
        p.getInventory().clearContent();
        com.runecraft.content.systems.SysState.of(p).worn.clear();
        QuestsState state = QuestsState.get(p);
        for (String slot : new ArrayList<>(state.wornAll(p.getUUID()).keySet())) {
            state.wear(p.getUUID(), slot, null);
        }
    }

    /** Whether the player carries (or wields) any tool meeting a requirement such as "pickaxe" or "axe:mithril". */
    static boolean hasTool(ServerPlayer p, String tool) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (QuestSpec.toolSatisfies(tool, com.runecraft.registry.ModItems.def(p.getInventory().getItem(i)))) {
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

    static void speak(ServerPlayer p, String speaker, List<String> lines) {
        for (String line : lines) {
            // "?flag|text" / "?!flag|text" (conditions can be chained): spoken only when the player has (hasn't) the flags
            boolean spoken = true;
            while (line.startsWith("?") && line.indexOf('|') > 1) {
                int bar = line.indexOf('|');
                boolean negate = line.charAt(1) == '!';
                spoken &= flag(p, line.substring(negate ? 2 : 1, bar)) != negate;
                line = line.substring(bar + 1);
            }
            if (!spoken) {
                continue;
            }
            if (line.startsWith("> ")) {
                QuestApi.you(p, line.substring(2));
            } else if (line.startsWith("* ") || speaker.isEmpty()) {
                String text = line.startsWith("* ") ? line.substring(2) : line;
                p.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
            } else if (line.startsWith("@") && line.contains(": ")) {
                int colon = line.indexOf(": ");
                QuestApi.say(p, line.substring(1, colon), line.substring(colon + 2));
            } else {
                QuestApi.say(p, speaker, line);
            }
        }
    }

    static void talk(ServerPlayer p, String npcId) {
        String name = npcName(npcId);
        // 0. Puzzles this NPC presents (Recruitment Drive's examiners, Murder Mystery's accusation).
        if (QuestPuzzles.talk(p, npcId, book.quests)) {
            return;
        }
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
        // 1b. A quest reward still to claim here: an unmade skill choice, or a lost lamp to give back.
        if (QuestLampService.talk(p, npcId)) {
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

    static void showMissing(ServerPlayer p, Trigger t, String name) {
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
    static void fire(ServerPlayer p, QuestSpec q, Trigger t, String speaker) {
        // a repeatable "raise the boss here" objective (nothing else to do) waits while its boss is still around, instead of
        // repeating its lines every second
        if (t.kind == Trigger.Kind.REACH && t.next == t.stage && t.spawn != null && t.setFlags.isEmpty() && t.flagOneOf.isEmpty()
                && t.give.isEmpty() && t.take.isEmpty() && bossNearby(p, t.spawn)) {
            return;
        }
        if (!t.take.isEmpty() && !QuestApi.takeAll(p, t.take)) {
            return;
        }
        // flags first, so the trigger's own lines can depend on them ("?flag|text")
        QuestsState state = QuestsState.get(p);
        t.setFlags.forEach(f -> state.setFlag(p.getUUID(), f));
        if (!t.flagOneOf.isEmpty() && t.flagOneOf.stream().noneMatch(f -> state.hasFlag(p.getUUID(), f))) {
            state.setFlag(p.getUUID(), QuestSpec.pickFlag(p.getStringUUID(), t.flagOneOf));
        }
        speak(p, speaker, t.lines);
        t.give.forEach((item, n) -> Inv.give(p, item, n));
        t.xp.forEach((skill, xp) -> XpService.add(p, skill, xp));
        if (t.spawn != null) {
            spawnNear(p, t.spawn);
        }
        if (t.travel != null) {
            travel(p, t.travel);
        }
        if (t.completes()) {
            QuestApi.complete(p, q.id());
            QuestLampService.grant(p, q.id()); // its lamps, tomes and skill choices (once per player)
            for (String unlock : q.unlocks) {
                p.displayClientMessage(Component.literal("Unlocked: " + unlock).withStyle(ChatFormatting.GOLD), false);
            }
        } else if (t.next != t.stage) {
            QuestApi.setStage(p, q.id(), t.next);
            p.displayClientMessage(Component.literal("[" + q.def.displayName() + "] " + q.stages.get(t.next).journal).withStyle(ChatFormatting.DARK_AQUA), false);
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
        ids.add(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
        ids.add(stats.name().toLowerCase(Locale.ROOT).replace(' ', '_').replace("'", ""));
        return ids;
    }

    static void onKill(ServerPlayer p, LivingEntity entity, MonsterStats stats) {
        if (!MageArenaService.killCounts(p, entity)) {
            return; // a Kolodion form another player raised
        }
        List<String> ids = ids(entity, stats);
        if (QuestPuzzles.onKill(p, ids)) {
            return;
        }
        for (QuestSpec q : book.quests) {
            if (!active(p, q)) {
                continue;
            }
            QuestSpec.Stage stage = q.stages.get(stage(p, q));
            for (int i = 0; i < stage.triggers.size(); i++) {
                Trigger t = stage.triggers.get(i);
                if (!ids.contains(t.target) || skipped(p, t)) {
                    continue;
                }
                if (t.kind == Trigger.Kind.DROP) {
                    if (unmet(p, t) == null && p.getRandom().nextInt(Math.max(1, t.chance)) == 0) {
                        t.give.forEach((item, n) -> com.runecraft.combat.LootDrops.spawn(entity, Inv.stack(item, n), p));
                    }
                } else if (t.kind == Trigger.Kind.KILL && unmet(p, t) == null) {
                    if (t.count > 1) {
                        // Multi-kill objective: count the kill (saved with the world) and fire on the last one.
                        QuestsState state = QuestsState.get(p);
                        String key = t.counterKey(q.id(), i);
                        int done = state.addKill(p.getUUID(), key);
                        if (done < t.count) {
                            p.displayClientMessage(Component.literal("[" + q.def.displayName() + "] " + stats.name() + ": " + done + "/" + t.count + " killed.")
                                    .withStyle(ChatFormatting.DARK_AQUA), false);
                            return;
                        }
                        state.clearKills(p.getUUID(), key);
                    }
                    fire(p, q, t, stats.name());
                    return;
                }
            }
        }
    }

    /** Whether a summoning stone may raise this quest boss for the player. */
    static boolean canFight(ServerPlayer p, String bossId) {
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

    /** Whether a living quest boss of this id is within 32 blocks of the player. */
    static boolean bossNearby(ServerPlayer p, String bossId) {
        EntityType<DataBossEntity> type = ModEntities.DATA_BOSS_TYPES.get(bossId);
        return type != null && p.level() instanceof ServerLevel world
                && !world.getEntities(type, new AABB(p.blockPosition()).inflate(32), e -> e.isAlive()).isEmpty();
    }

    static void spawnNear(ServerPlayer p, String bossId) {
        BossDef def = ContentRegistry.boss(bossId);
        EntityType<DataBossEntity> type = ModEntities.DATA_BOSS_TYPES.get(bossId);
        if (def == null || type == null || !(p.level() instanceof ServerLevel world)) {
            return;
        }
        if (bossNearby(p, bossId)) {
            Msg.game(p, def.name + " is already nearby!");
            return;
        }
        BlockPos at = p.blockPosition().relative(p.getDirection(), 4);
        // leashed where it appears; it vanishes a minute after its fight is left (BossLair)
        DataBossService.spawn(world, at, def, p, com.runecraft.osrs.content.bosses.BossLair.QUEST_LEASH);
        Msg.red(p, def.name + " (level-" + def.combatLevel + ") appears!");
    }

    /**
     * Self-test hook (the step walk, which fights its foes on the flat test platform): moves a quest boss a step called up
     * beside the player ({@link #spawnNear}) to {@code at}, and its lair with it. spawnNear leashes the boss where it appeared
     * ({@link com.runecraft.osrs.content.bosses.BossLair#QUEST_LEASH}: an OSRS quest boss stays where it appears and is gone
     * once the player walks off). Moved alone, it would be put straight back there, out of the player's reach (no hit lands
     * from outside its lair), and vanish a minute later with nobody near (the lair's reset) instead of dying, so its kill
     * would never reach the quest.
     */
    public static void selfTestMoveQuestBoss(DataBossEntity boss, BlockPos at) {
        boss.getNavigation().stop();
        boss.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        boss.markSummoned(at, com.runecraft.osrs.content.bosses.BossLair.QUEST_LEASH);
    }

    static int[] placeCentre(String place) {
        Landmark l = Landmark.byName(place);
        if (l != null) {
            return new int[]{l.blockX(), l.blockZ()};
        }
        PlaceDef d = ContentRegistry.place(place);
        return d == null ? null : new int[]{d.blockX(), d.blockZ()};
    }

    static void travel(ServerPlayer p, String place) {
        Landmark l = Landmark.byName(place);
        if (l != null) {
            Gielinor.teleport(p, l);
            return;
        }
        PlaceDef d = ContentRegistry.place(place);
        ServerLevel world = Gielinor.world(p.getServer());
        if (d == null || world == null) {
            return;
        }
        LandmarkBuilder.buildNow(world, d);
        BlockPos pos = Gielinor.safeSpot(world, d.blockX() + 2, d.blockZ() + 2);
        p.teleportTo(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, p.getYRot(), p.getXRot());
        Msg.game(p, "You arrive at " + d.name() + ".");
    }

    static void reachTick(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!Gielinor.is(p) || p.isSpectator()) {
                continue;
            }
            // place objectives of the hand-scripted quests (Restless Ghost altar, cadava bushes...)
            com.runecraft.quest.QuestManager.spotTick(p);
            reach(p, server);
        }
    }

    /** Self-test hook: runs the place objectives for one player where they stand now, as the 20-tick check does. */
    public static void selfTestReach(ServerPlayer p) {
        REACH_NOTICE.remove(p.getUUID());
        reach(p, p.getServer());
    }

    /** The place objectives (and place-presented puzzles) of a player's active quests, where the player stands now. */
    static void reach(ServerPlayer p, MinecraftServer server) {
        for (QuestSpec q : book.quests) {
            if (!active(p, q)) {
                continue;
            }
            for (Trigger t : q.stages.get(stage(p, q)).triggers) {
                boolean depthOk = !t.underground || p.getY() < UNDERGROUND_Y;
                if (t.kind == Trigger.Kind.PUZZLE && !skipped(p, t)) {
                    int[] at = placeCentre(t.target);
                    if (at != null) {
                        double px = p.getX() - at[0];
                        double pz = p.getZ() - at[1];
                        QuestPuzzles.reach(p, q, t, depthOk && px * px + pz * pz <= t.radius * t.radius);
                    }
                    continue;
                }
                if (t.kind != Trigger.Kind.REACH || skipped(p, t) || !depthOk) {
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
                int last = REACH_NOTICE.getOrDefault(p.getUUID(), -10000);
                if (server.getTickCount() - last > 600) {
                    REACH_NOTICE.put(p.getUUID(), server.getTickCount());
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

    /** How long {@link #selfTestLoad} waits for the chunks it loads, in seconds. */
    public static final int SELF_TEST_LOAD_SECONDS = 5;

    /**
     * Self-test hook (the step walk): puts the player at a quest place's centre (underground: down in its dungeon) and runs
     * the place objectives there once, as the reach tick does every second. Returns 0 when it did; -1 if the place is unknown;
     * otherwise the number of chunks around the place still loading after {@link #SELF_TEST_LOAD_SECONDS} s, and the player
     * stays put (a foe raised there would be invisible, so the caller reports the load rather than a missing foe).
     */
    public static int selfTestArrive(ServerPlayer p, String place, boolean underground) {
        int[] c = placeCentre(place);
        if (c == null || !(p.level() instanceof ServerLevel world)) {
            return -1;
        }
        // A player who walks somewhere has the chunks around them loaded. The walk teleports there inside one server tick, so
        // a foe the objective calls up beside the player would land in a chunk the server hasn't confirmed yet, hidden from
        // every entity search (the walk couldn't find it, fight it or bring it home): load them first.
        int loading = selfTestLoad(world, c[0], c[1], 8);
        if (loading > 0) {
            return loading;
        }
        double y = underground ? UNDERGROUND_Y - 16 : p.getY();
        p.teleportTo(world, c[0] + 0.5, y, c[1] + 0.5, p.getYRot(), p.getXRot());
        reach(p, p.getServer());
        return 0;
    }

    /**
     * Loads the chunks within {@code r} blocks of block ({@code x}, {@code z}) and runs the server's queued chunk work until
     * each is a confirmed full chunk, whose entities area searches see (at most {@link #SELF_TEST_LOAD_SECONDS} s), as
     * TzHaarSelfTest settles an arena. Returns how many are still not full chunks when that time runs out (0: all are).
     */
    static int selfTestLoad(ServerLevel world, int x, int z, int r) {
        net.minecraft.server.level.ServerChunkCache chunks = world.getChunkSource();
        List<net.minecraft.server.level.ChunkHolder> holders = new ArrayList<>();
        for (int cx = (x - r) >> 4; cx <= (x + r) >> 4; cx++) {
            for (int cz = (z - r) >> 4; cz <= (z + r) >> 4; cz++) {
                world.getChunk(cx, cz);
                net.minecraft.server.level.ChunkHolder holder = chunks.chunkMap.getVisibleChunkIfPresent(net.minecraft.world.level.ChunkPos.asLong(cx, cz));
                if (holder != null) {
                    holders.add(holder);
                }
            }
        }
        long deadline = System.nanoTime() + SELF_TEST_LOAD_SECONDS * 1_000_000_000L;
        while (holders.stream().anyMatch(h -> !h.getFullChunkFuture().isDone()) && System.nanoTime() < deadline) {
            if (!chunks.pollTask()) {
                java.util.concurrent.locks.LockSupport.parkNanos(100_000L); // the rest arrives from the chunk worker threads
            }
        }
        for (int i = 0; i < 100_000 && chunks.pollTask(); i++) {
            // the promotions the completed chunks queued (each call runs one task)
        }
        return (int) holders.stream().filter(h -> !h.getFullChunkFuture().isDone()).count();
    }

    /** Self-test hook: why a trigger would not fire for the player now (switched off, or its first unmet condition), or null. */
    public static String selfTestBlocked(ServerPlayer p, Trigger t) {
        if (skipped(p, t)) {
            return "switched off (an unless flag or item)";
        }
        String reason = unmet(p, t);
        return reason == null ? null : reason.isEmpty() ? "a flag or place condition is unmet" : reason;
    }
}
