package com.runecraft.content;

import com.runecraft.combat.MonsterStats;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Skill;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

/** Game events modules can listen to (achievement diaries, clue steps, minigame points...). */
public final class GameEvents {
    public interface XpListener {
        void onXp(ServerPlayerEntity player, Skill skill, int tenths);
    }

    public interface KillListener {
        void onKill(ServerPlayerEntity player, LivingEntity entity, MonsterStats stats);
    }

    public interface MakeListener {
        void onMake(ServerPlayerEntity player, Recipe recipe);
    }

    public interface ObtainListener {
        void onObtain(ServerPlayerEntity player, String item, int count);
    }

    public interface CastListener {
        void onCast(ServerPlayerEntity player, String spellId);
    }

    /** A named activity such as "talk:" + npc id, "minigame_start:" + game id or "eat:" + item id (see Diaries.ACTIVITIES). */
    public interface ActivityListener {
        void onActivity(ServerPlayerEntity player, String key);
    }

    private static final List<ActivityListener> ACTIVITIES = new ArrayList<>();
    private static final List<XpListener> XP = new ArrayList<>();
    private static final List<KillListener> KILLS = new ArrayList<>();
    private static final List<MakeListener> MAKES = new ArrayList<>();
    private static final List<ObtainListener> OBTAINS = new ArrayList<>();
    private static final List<CastListener> CASTS = new ArrayList<>();

    private GameEvents() {
    }

    public static void onXp(XpListener l) {
        XP.add(l);
    }

    public static void onKill(KillListener l) {
        KILLS.add(l);
    }

    public static void onMake(MakeListener l) {
        MAKES.add(l);
    }

    /** Fired when a skilling action, drop or reward gives a player an item. */
    public static void onObtain(ObtainListener l) {
        OBTAINS.add(l);
    }

    /** Fired when a standard-spellbook spell (teleport, alchemy, superheat) is cast; ids are lower-case enum names. */
    public static void onCast(CastListener l) {
        CASTS.add(l);
    }

    /** Fired for named activities other modules can't observe otherwise (NPC talk, minigame starts/wins, food eaten...). */
    public static void onActivity(ActivityListener l) {
        ACTIVITIES.add(l);
    }

    public static void fireActivity(ServerPlayerEntity p, String key) {
        ACTIVITIES.forEach(l -> l.onActivity(p, key));
    }

    public static void fireCast(ServerPlayerEntity p, String spellId) {
        CASTS.forEach(l -> l.onCast(p, spellId));
    }

    public static void fireXp(ServerPlayerEntity p, Skill skill, int tenths) {
        XP.forEach(l -> l.onXp(p, skill, tenths));
    }

    public static void fireKill(ServerPlayerEntity p, LivingEntity e, MonsterStats stats) {
        KILLS.forEach(l -> l.onKill(p, e, stats));
    }

    public static void fireMake(ServerPlayerEntity p, Recipe r) {
        MAKES.forEach(l -> l.onMake(p, r));
    }

    public static void fireObtain(ServerPlayerEntity p, String item, int count) {
        OBTAINS.forEach(l -> l.onObtain(p, item, count));
    }
}
