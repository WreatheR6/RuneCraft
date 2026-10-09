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

    private static final List<XpListener> XP = new ArrayList<>();
    private static final List<KillListener> KILLS = new ArrayList<>();
    private static final List<MakeListener> MAKES = new ArrayList<>();
    private static final List<ObtainListener> OBTAINS = new ArrayList<>();

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
