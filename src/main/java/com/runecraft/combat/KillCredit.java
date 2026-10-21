package com.runecraft.combat;

import com.runecraft.osrs.MonsterAi;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Who killed a monster (round 42): the damage every player dealt to it ({@link CombatHandler#applyDamage}, a thrall's damage
 * counting for its owner, poison and venom for whoever poisoned it), and the top damager when it dies. OSRS gives the drop, the
 * kill (Slayer, kill counts, the collection log, quest and diary kills) to whoever dealt the most damage, not to the last hit.
 * Bosses keep their own loot rules ({@code DataBossEntity} contributions).
 */
public final class KillCredit {
    private static final Map<LivingEntity, MonsterAi.DamageLedger<UUID>> LEDGERS = new WeakHashMap<>();
    /** The top damager of each monster whose death was handed out ({@link #winner}), for kill hooks that run after it (round 43). */
    private static final Map<LivingEntity, UUID> TOP_AT_DEATH = new WeakHashMap<>();
    /** Damagers by UUID, for players not in the player list (the self-test's fake players). */
    private static final Map<UUID, WeakReference<ServerPlayer>> DAMAGERS = new HashMap<>();

    private KillCredit() {
    }

    /** Records {@code dealt} damage from {@code attacker} (a player, or a summon standing in for one) to a monster. */
    public static synchronized void record(LivingEntity attacker, LivingEntity target, int dealt) {
        if (dealt <= 0 || target == null || target instanceof Player) {
            return;
        }
        ServerPlayer who = attacker instanceof ServerPlayer p ? p : owner(attacker);
        if (who == null) {
            return;
        }
        LEDGERS.computeIfAbsent(target, t -> new MonsterAi.DamageLedger<>()).add(who.getUUID(), dealt);
        DAMAGERS.put(who.getUUID(), new WeakReference<>(who));
    }

    /**
     * The player credited with a monster's death: its top damager while online (or a fake player still in the world), else the
     * player who landed the killing blow ({@code killer}, may be null). Clears the monster's ledger.
     */
    public static synchronized ServerPlayer winner(LivingEntity dead, ServerPlayer killer) {
        MonsterAi.DamageLedger<UUID> ledger = LEDGERS.remove(dead);
        UUID top = ledger == null ? null : ledger.top();
        if (top == null) {
            return killer;
        }
        TOP_AT_DEATH.put(dead, top);
        if (killer != null && killer.getUUID().equals(top)) {
            return killer;
        }
        ServerPlayer p = player(dead.getServer(), top);
        return p != null && p.isAlive() ? p : killer;
    }

    /**
     * The player who dealt the most damage to a monster: from its ledger while it lives, or as recorded when its death was handed
     * out ({@link #winner}); {@code fallback} when nobody is recorded or that player is gone. For kill hooks that credit something
     * other than the drop (round 43: a Doom of Mokhaiotl delve level goes to the top damager's run, not the killing blow's).
     */
    public static synchronized ServerPlayer topDamager(LivingEntity e, ServerPlayer fallback) {
        MonsterAi.DamageLedger<UUID> ledger = LEDGERS.get(e);
        UUID top = ledger != null ? ledger.top() : TOP_AT_DEATH.get(e);
        if (top == null || fallback != null && fallback.getUUID().equals(top)) {
            return fallback;
        }
        ServerPlayer p = player(e.getServer(), top);
        return p != null && p.isAlive() ? p : fallback;
    }

    /** The damage each player has dealt to a living monster so far (self-tests). */
    public static synchronized Map<UUID, Long> damage(LivingEntity e) {
        MonsterAi.DamageLedger<UUID> ledger = LEDGERS.get(e);
        return ledger == null ? Map.of() : Map.copyOf(ledger.entries());
    }

    /** Forgets a monster's damage (it reset: went home after a chase). */
    public static synchronized void forget(LivingEntity e) {
        LEDGERS.remove(e);
    }

    /** The player who owns a summon ({@code runecraft_owner:<uuid>} tag: thralls, the cannon's pets), or null. */
    public static ServerPlayer owner(Entity e) {
        if (e == null || e instanceof Player) {
            return null;
        }
        for (String tag : e.getTags()) {
            if (tag.startsWith("runecraft_owner:")) {
                try {
                    return player(e.getServer(), UUID.fromString(tag.substring("runecraft_owner:".length())));
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private static synchronized ServerPlayer player(MinecraftServer server, UUID id) {
        ServerPlayer p = server == null ? null : server.getPlayerList().getPlayer(id);
        if (p == null) {
            WeakReference<ServerPlayer> ref = DAMAGERS.get(id);
            p = ref == null ? null : ref.get();
            if (p != null && p.isRemoved()) {
                p = null;
            }
        }
        return p;
    }
}
