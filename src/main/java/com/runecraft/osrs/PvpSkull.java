package com.runecraft.osrs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The PvP skull (OSRS Wiki, "Skull (status)"). A player who attacks another player in the Wilderness without having
 * been attacked by them first is skulled for 30 minutes (since the 9 December 2021 update; every further unprovoked
 * attack restarts the timer). Players who were attacked may retaliate against their attacker without a skull. The
 * Emblem Trader skulls on request. A skulled player keeps no items on death (one with Protect Item), and dying
 * removes the skull. Times are in game ticks (0.6 s).
 */
public final class PvpSkull {
    /** Scoreboard team holding skulled players: its prefix draws the skull above their heads, the HUD reads it. */
    public static final String TEAM = "rc_skulled";
    /** 30 minutes after attacking a player. */
    public static final int ATTACK_TICKS = 3000;
    /** Skull from the Emblem Trader's "Skull" option. // approx: the wiki gives no duration; 20 minutes assumed */
    public static final int EMBLEM_TRADER_TICKS = 2000;
    /** How long a player may retaliate against an attacker without being skulled. // approx: as long as a skull lasts */
    public static final int RETALIATION_TICKS = ATTACK_TICKS;

    /** skulled player -> game tick the skull ends. */
    private final Map<UUID, Long> skullUntil = new HashMap<>();
    /** victim -> (attacker -> last game tick the attacker attacked the victim). */
    private final Map<UUID, Map<UUID, Long>> attackedBy = new HashMap<>();

    /**
     * Records {@code attacker} attacking {@code victim} at game tick {@code now}. Returns true when this attack skulls
     * (or re-skulls) the attacker: it happened in the Wilderness and the victim had not attacked the attacker within
     * {@link #RETALIATION_TICKS}.
     */
    public boolean onAttack(UUID attacker, UUID victim, long now, boolean wilderness) {
        if (attacker.equals(victim) || !wilderness) {
            return false;
        }
        attackedBy.computeIfAbsent(victim, k -> new HashMap<>()).put(attacker, now);
        Long provoked = attackedBy.getOrDefault(attacker, Map.of()).get(victim);
        if (provoked != null && now - provoked <= RETALIATION_TICKS) {
            return false;
        }
        skull(attacker, now, ATTACK_TICKS);
        return true;
    }

    /** Skulls a player for {@code ticks} from now (an existing longer skull is kept). */
    public void skull(UUID player, long now, int ticks) {
        skullUntil.merge(player, now + ticks, Math::max);
    }

    public boolean isSkulled(UUID player, long now) {
        Long until = skullUntil.get(player);
        return until != null && until > now;
    }

    /** Game ticks left on the skull, 0 when not skulled. */
    public long ticksLeft(UUID player, long now) {
        Long until = skullUntil.get(player);
        return until == null ? 0 : Math.max(0, until - now);
    }

    /** Death (or leaving the game for good) removes the skull and the player's retaliation records. */
    public void clear(UUID player) {
        skullUntil.remove(player);
        attackedBy.remove(player);
    }

    /** Removes and returns the players whose skull has run out; also forgets retaliation records that expired. */
    public List<UUID> expire(long now) {
        List<UUID> out = new ArrayList<>();
        skullUntil.entrySet().removeIf(e -> {
            if (e.getValue() <= now) {
                out.add(e.getKey());
                return true;
            }
            return false;
        });
        attackedBy.values().forEach(m -> m.values().removeIf(t -> now - t > RETALIATION_TICKS));
        attackedBy.values().removeIf(Map::isEmpty);
        return out;
    }

    /** Items kept on death: none while skulled (one with Protect Item), otherwise 3 (4). */
    public int itemsKept(UUID player, long now, boolean protectItem) {
        return PrayerEffects.itemsKept(isSkulled(player, now), protectItem);
    }

    /** Whole minutes left, rounded up, for messages. */
    public static int minutes(long ticks) {
        return (int) ((ticks + 99) / 100);
    }
}
