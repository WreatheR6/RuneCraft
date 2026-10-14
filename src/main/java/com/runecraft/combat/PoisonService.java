package com.runecraft.combat;

import com.runecraft.network.Net;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.Toxins;
import com.runecraft.util.Msg;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OSRS poison and venom (rules in {@link Toxins}). Poison hits every 30 game ticks (18s), starting at its
 * strength and dropping by 1 every 5 hits. Venom starts at 6 and rises by 2 per hit up to 20. Poison and
 * venom immunity run on separate timers.
 */
public final class PoisonService {
    private static final Map<UUID, Toxins.State> STATES = new ConcurrentHashMap<>();

    private PoisonService() {
    }

    private static Toxins.State state(LivingEntity target) {
        return STATES.computeIfAbsent(target.getUuid(), u -> new Toxins.State());
    }

    public static void poison(LivingEntity target, int damage, long gameTick) {
        if (!(target instanceof ServerPlayerEntity) && com.runecraft.osrs.data.MonsterTraits.poisonImmune(MonsterStats.of(target).traitsKey())) {
            return; // OSRS: poison-immune monsters (most bosses, undead, demons...) can't be poisoned
        }
        if (Toxins.poison(state(target), damage, gameTick) && target instanceof ServerPlayerEntity p) {
            p.sendMessage(Text.literal("You have been poisoned!").formatted(Formatting.DARK_GREEN), false);
        }
    }

    public static void venom(LivingEntity target, long gameTick) {
        if (!(target instanceof ServerPlayerEntity)) {
            String key = MonsterStats.of(target).traitsKey();
            if (com.runecraft.osrs.data.MonsterTraits.venomImmune(key)) {
                // OSRS: a venom-immune monster that isn't poison-immune is poisoned (6) instead
                if (!com.runecraft.osrs.data.MonsterTraits.poisonImmune(key)) {
                    poison(target, 6, gameTick);
                }
                return;
            }
        }
        if (Toxins.venom(state(target), gameTick) && target instanceof ServerPlayerEntity p) {
            p.sendMessage(Text.literal("You have been envenomed!").formatted(Formatting.DARK_GREEN), false);
        }
    }

    /**
     * Cures poison and grants poison immunity for the given game ticks. With {@code venomToo} venom is cured as
     * well (restoration pools, deaths...); without it an antipoison drunk while envenomed only turns the venom
     * into ordinary poison.
     */
    public static void cure(LivingEntity target, boolean venomToo, int immunityTicks, long gameTick) {
        Toxins.Cure result = Toxins.cure(state(target), venomToo, immunityTicks, 0, gameTick);
        if (result == Toxins.Cure.VENOM_TO_POISON && target instanceof ServerPlayerEntity p) {
            Msg.game(p, "The venom in your blood turns into a weaker poison.");
        }
    }

    /** Anti-venom: cures venom and poison, with separate venom and poison immunity timers (game ticks). */
    public static void antiVenom(LivingEntity target, int poisonImmunityTicks, int venomImmunityTicks, long gameTick) {
        Toxins.cure(state(target), true, poisonImmunityTicks, venomImmunityTicks, gameTick);
    }

    public static boolean isPoisoned(LivingEntity target) {
        Toxins.State s = STATES.get(target.getUuid());
        return s != null && s.active();
    }

    public static boolean isEnvenomed(LivingEntity target) {
        Toxins.State s = STATES.get(target.getUuid());
        return s != null && s.envenomed();
    }

    public static boolean isVenomImmune(LivingEntity target, long gameTick) {
        Toxins.State s = STATES.get(target.getUuid());
        return s != null && s.venomImmune(gameTick);
    }

    /** Called once per game tick for each player (and for module mobs from their tick). */
    public static void tick(LivingEntity entity, long gameTick) {
        Toxins.State s = STATES.get(entity.getUuid());
        if (s == null) {
            return;
        }
        if (!entity.isAlive()) {
            STATES.remove(entity.getUuid());
            return;
        }
        boolean venom = s.envenomed();
        int damage = Toxins.tick(s, gameTick);
        if (damage > 0) {
            CombatHooks.nameAttack("poison", null);
            CombatHandler.applyDamage(null, entity, damage);
            ServerNetworking.hitsplat(entity, damage, venom ? Net.SPLAT_VENOM : Net.SPLAT_POISON);
            if (!s.active() && entity instanceof ServerPlayerEntity p) {
                Msg.game(p, "The poison has worn off.");
            }
        }
        if (s.idle(gameTick)) {
            STATES.remove(entity.getUuid());
        }
    }
}
