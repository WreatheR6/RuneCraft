package com.runecraft.quest;

import com.runecraft.combat.MonsterStats;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.NpcDef;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Dialogue and kill hooks for module NPCs and quests. Modules register handlers from their
 * {@code ModuleHooks.init()}.
 */
public final class QuestScripts {
    @FunctionalInterface
    public interface KillHook {
        void onKill(ServerPlayerEntity player, LivingEntity entity, MonsterStats stats);
    }

    private static final Map<String, BiConsumer<ServerPlayerEntity, QuestNpcEntity>> TALK = new HashMap<>();
    private static final List<KillHook> KILLS = new ArrayList<>();

    private QuestScripts() {
    }

    /** Registers what happens when a player talks to a module NPC (NpcDef id). */
    public static void onTalk(String npcId, BiConsumer<ServerPlayerEntity, QuestNpcEntity> handler) {
        TALK.put(npcId, handler);
    }

    /** The talk handler currently registered for an NPC, or null (lets a module wrap another's dialogue). */
    public static BiConsumer<ServerPlayerEntity, QuestNpcEntity> handler(String npcId) {
        return TALK.get(npcId);
    }

    public static void onKill(KillHook hook) {
        KILLS.add(hook);
    }

    static void talk(ServerPlayerEntity player, QuestNpcEntity entity) {
        BiConsumer<ServerPlayerEntity, QuestNpcEntity> handler = TALK.get(entity.npcId());
        if (handler != null) {
            handler.accept(player, entity);
            return;
        }
        NpcDef def = ContentRegistry.npc(entity.npcId());
        QuestApi.say(player, def == null ? "Stranger" : def.name(), "Hello there, adventurer.");
    }

    static void onKill(ServerPlayerEntity player, LivingEntity entity, MonsterStats stats) {
        for (KillHook hook : KILLS) {
            hook.onKill(player, entity, stats);
        }
    }
}
