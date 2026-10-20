package com.runecraft.combat;

import com.runecraft.network.Net;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.Toxins;
import com.runecraft.util.Msg;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * OSRS poison and venom (rules in {@link Toxins}). Poison hits every 30 game ticks (18s), starting at its
 * strength and dropping by 1 every 5 hits. Venom starts at 6 and rises by 2 per hit up to 20. Poison and
 * venom immunity run on separate timers.
 */
public final class PoisonService {
    private static final Map<UUID, Toxins.State> STATES = new ConcurrentHashMap<>();
    /**
     * Poisoned or envenomed monster -> the player whose hit last landed on it: a poison or venom tick that kills it gives
     * that player the kill (loot, kill count, Slayer, quests), as in OSRS.
     */
    private static final Map<UUID, ServerPlayer> CREDIT = new ConcurrentHashMap<>();
    /** Told when poison or venom hurts a monster (Combat Achievements: "A Slow Death"). */
    public static java.util.function.Consumer<LivingEntity> monsterToxinListener = e -> {
    };

    private PoisonService() {
    }

    private static Toxins.State state(LivingEntity target) {
        return STATES.computeIfAbsent(target.getUUID(), u -> new Toxins.State());
    }

    public static void poison(LivingEntity target, int damage, long gameTick) {
        if (!(target instanceof ServerPlayer) && com.runecraft.osrs.data.MonsterTraits.poisonImmune(MonsterStats.of(target).traitsKey())) {
            return; // OSRS: poison-immune monsters (most bosses, undead, demons...) can't be poisoned
        }
        if (Toxins.poison(state(target), damage, gameTick) && target instanceof ServerPlayer p) {
            p.displayClientMessage(Component.literal("You have been poisoned!").withStyle(ChatFormatting.DARK_GREEN), false);
        }
        noteCredit(target);
    }

    /** Remembers the player whose hit last landed on a poisoned monster (weapons poison right after their hit lands). */
    private static void noteCredit(LivingEntity target) {
        if (!(target instanceof ServerPlayer) && target.getLastHurtByMob() instanceof ServerPlayer hitter
                && STATES.containsKey(target.getUUID())) {
            CREDIT.put(target.getUUID(), hitter);
        }
    }

    /** The player a toxin kill of this monster is credited to (after a death or relog, the same player's new entity). */
    private static ServerPlayer credit(LivingEntity target) {
        noteCredit(target);
        ServerPlayer hitter = CREDIT.get(target.getUUID());
        if (hitter != null && (hitter.isRemoved() || hitter.hasDisconnected())) {
            hitter = target.getServer() == null ? null : target.getServer().getPlayerList().getPlayer(hitter.getUUID());
        }
        return hitter;
    }

    private static void forget(LivingEntity entity) {
        STATES.remove(entity.getUUID());
        CREDIT.remove(entity.getUUID());
    }

    public static void venom(LivingEntity target, long gameTick) {
        if (!(target instanceof ServerPlayer)) {
            String key = MonsterStats.of(target).traitsKey();
            if (com.runecraft.osrs.data.MonsterTraits.venomImmune(key)) {
                // OSRS: a venom-immune monster that isn't poison-immune is poisoned (6) instead
                if (!com.runecraft.osrs.data.MonsterTraits.poisonImmune(key)) {
                    poison(target, 6, gameTick);
                }
                return;
            }
        }
        if (Toxins.venom(state(target), gameTick) && target instanceof ServerPlayer p) {
            p.displayClientMessage(Component.literal("You have been envenomed!").withStyle(ChatFormatting.DARK_GREEN), false);
        }
        noteCredit(target);
    }

    /**
     * Cures poison and grants poison immunity for the given game ticks. With {@code venomToo} venom is cured as
     * well (restoration pools, deaths...); without it an antipoison drunk while envenomed only turns the venom
     * into ordinary poison.
     */
    public static void cure(LivingEntity target, boolean venomToo, int immunityTicks, long gameTick) {
        Toxins.Cure result = Toxins.cure(state(target), venomToo, immunityTicks, 0, gameTick);
        if (result == Toxins.Cure.VENOM_TO_POISON && target instanceof ServerPlayer p) {
            Msg.game(p, "The venom in your blood turns into a weaker poison.");
        }
    }

    /** Anti-venom: cures venom and poison, with separate venom and poison immunity timers (game ticks). */
    public static void antiVenom(LivingEntity target, int poisonImmunityTicks, int venomImmunityTicks, long gameTick) {
        Toxins.cure(state(target), true, poisonImmunityTicks, venomImmunityTicks, gameTick);
    }

    /**
     * A copy of the entity's poison/venom state and immunity timers, or null when it has none. Self-tests save it with
     * this, start from {@link #clear} and put it back with {@link #restore}: the state lives outside the player data,
     * and the fake self-test player is never ticked (it isn't in the player list), so venom from an earlier check
     * would otherwise stay on it for good.
     */
    public static Toxins.State snapshot(LivingEntity target) {
        Toxins.State s = STATES.get(target.getUUID());
        return s == null ? null : s.copy();
    }

    /** Puts back a state saved with {@link #snapshot} (null: no poison, venom or immunity). */
    public static void restore(LivingEntity target, Toxins.State saved) {
        if (saved == null) {
            STATES.remove(target.getUUID());
        } else {
            STATES.put(target.getUUID(), saved.copy());
        }
    }

    /** Forgets the entity's poison, venom and both immunity timers (self-test set-up). */
    public static void clear(LivingEntity target) {
        STATES.remove(target.getUUID());
    }

    public static boolean isPoisoned(LivingEntity target) {
        Toxins.State s = STATES.get(target.getUUID());
        return s != null && s.active();
    }

    public static boolean isEnvenomed(LivingEntity target) {
        Toxins.State s = STATES.get(target.getUUID());
        return s != null && s.envenomed();
    }

    public static boolean isVenomImmune(LivingEntity target, long gameTick) {
        Toxins.State s = STATES.get(target.getUUID());
        return s != null && s.venomImmune(gameTick);
    }

    public static boolean isPoisonImmune(LivingEntity target, long gameTick) {
        Toxins.State s = STATES.get(target.getUUID());
        return s != null && s.poisonImmune(gameTick);
    }

    /** Called once per game tick for each player (and for module mobs from their tick). */
    public static void tick(LivingEntity entity, long gameTick) {
        Toxins.State s = STATES.get(entity.getUUID());
        if (s == null) {
            return;
        }
        if (!entity.isAlive()) {
            forget(entity);
            return;
        }
        boolean venom = s.envenomed();
        int damage = Toxins.tick(s, gameTick);
        if (damage > 0) {
            ServerPlayer credit = null;
            if (!(entity instanceof ServerPlayer)) {
                // Told first: a tick that kills is the fight's last damage when the kill is judged.
                monsterToxinListener.accept(entity);
                credit = credit(entity);
            }
            CombatHooks.nameAttack("poison", null);
            CombatHandler.typelessKillCredit = credit;
            try {
                CombatHandler.applyDamage(null, entity, damage);
            } finally {
                CombatHandler.typelessKillCredit = null;
            }
            ServerNetworking.hitsplat(entity, damage, venom ? Net.SPLAT_VENOM : Net.SPLAT_POISON);
            if (!s.active() && entity instanceof ServerPlayer p) {
                Msg.game(p, "The poison has worn off.");
            }
        }
        if (s.idle(gameTick)) {
            forget(entity);
        }
    }
}
