package com.runecraft.combat;

import com.runecraft.action.ActionManager;
import com.runecraft.osrs.CombatEffects;
import com.runecraft.osrs.AttackType;
import com.runecraft.player.Players;
import com.runecraft.util.Msg;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/** The Tele Block spell: stops a player teleporting for 5 minutes (2.5 with Protect from Magic). */
public final class Teleblock {
    private static final Map<UUID, CombatEffects.Teleblock> STATES = new ConcurrentHashMap<>();

    private Teleblock() {
    }

    /** Tele blocks a player; returns false (with a message to the caster) if it can't be applied now. */
    public static boolean apply(ServerPlayer caster, ServerPlayer victim) {
        // Protect from Magic halves it, read through the protection chokepoint (its switch grace)
        boolean prayer = CombatHandler.isProtected(Players.data(victim), AttackType.MAGIC);
        CombatEffects.Teleblock tb = STATES.computeIfAbsent(victim.getUUID(), u -> new CombatEffects.Teleblock());
        if (!tb.apply(ActionManager.gameTick(), prayer)) {
            Msg.game(caster, "That player is already affected by this spell.");
            return false;
        }
        Msg.red(victim, "A teleport block has been cast on you. It should wear off in "
                + CombatEffects.minutes(CombatEffects.teleblockTicks(prayer)) + ".");
        return true;
    }

    /** True if the player is tele blocked (and tells them so). */
    public static boolean blocked(ServerPlayer player) {
        CombatEffects.Teleblock tb = STATES.get(player.getUUID());
        long now = ActionManager.gameTick();
        if (tb == null || !tb.blocked(now)) {
            return false;
        }
        Msg.game(player, "A magical force stops you from teleporting. (" + CombatEffects.minutes(tb.remaining(now)) + " left)");
        return true;
    }

    /** True if the player is tele blocked, without telling them (the Ferox Enclave's barriers check it every tick). */
    public static boolean active(ServerPlayer player) {
        CombatEffects.Teleblock tb = STATES.get(player.getUUID());
        return tb != null && tb.blocked(ActionManager.gameTick());
    }

    /** Removes the tele block (death, logout). */
    public static void forget(UUID player) {
        STATES.remove(player);
    }
}
