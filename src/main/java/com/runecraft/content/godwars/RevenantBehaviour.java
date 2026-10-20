package com.runecraft.content.godwars;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.Mechanics;
import com.runecraft.combat.Aggression;
import com.runecraft.combat.HitCalcBridge;
import com.runecraft.combat.SkullService;
import com.runecraft.content.systems.SysState;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.item.ItemCharges;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.PvpSkull;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.godwars.RevenantRewards;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.platform.ServerEntityEvents;
import com.runecraft.platform.ServerTickEvents;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * How revenants fight (Revenants/Strategies): they attack with the style the player is weakest against and never the one
 * prayed against, and heal 20 hitpoints instead of attacking while below half health (15-tick cooldown, 8-25 heals each).
 * A charged bracelet of ethereum keeps them from attacking on sight. The amulet of avarice keeps its wearer skulled, and
 * the revenant maledictus's top damager wearing one gets the Forinthry Surge for 30 minutes (lost when the amulet comes off).
 */
final class RevenantBehaviour {
    private RevenantBehaviour() {
    }

    /** A revenant's heals: how many it has left and when it last healed. */
    private static final class HealState {
        int left = -1;
        long last = -1;
    }

    private static final Map<LivingEntity, HealState> HEALS = new WeakHashMap<>();
    /** Forinthry Surge: game tick it ends, per player. */
    private static final Map<UUID, Long> SURGE = new HashMap<>();
    private static long ticks;

    static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof OsrsMonsterEntity mob && RevenantRewards.isRevenant(RevenantLoot.creatureId(mob))) {
                attach(mob);
            }
        });
        Aggression.addPeaceful((mob, player) -> RevenantRewards.isRevenant(RevenantLoot.creatureId(mob)) && chargedBracelet(player));
        HitCalcBridge.forinthrySurge = RevenantBehaviour::surge;
        com.runecraft.combat.PrayerEffectsService.beforeKeep = RevenantBehaviour::wildernessDeath;
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++ticks % 20 != 0) {
                return;
            }
            long now = ActionManager.gameTick();
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (RevenantLoot.wearingAvarice(p)) {
                    // "The skull cannot be removed until the amulet is unequipped, after which it behaves like a normal PK skull."
                    SkullService.skull(p, PvpSkull.EMBLEM_TRADER_TICKS);
                } else if (SURGE.remove(p.getUUID()) != null) {
                    Msg.game(p, "Without your amulet of avarice, the Forinthry Surge fades.");
                }
                Long until = SURGE.get(p.getUUID());
                if (until != null && now >= until) {
                    SURGE.remove(p.getUUID());
                    Msg.game(p, "Your Forinthry Surge has worn off.");
                }
            }
        });
    }

    /**
     * A death in the Wilderness empties every Wilderness weapon the player carries (wiki, Craw's bow: the stored ether is
     * always dropped and the 1,000 activation ether always disappears, even on a protected weapon).
     */
    static void wildernessDeath(ServerPlayer player) {
        if (com.runecraft.world.Gielinor.wildernessLevel(player) <= 0) {
            return;
        }
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            var stack = inv.getItem(i);
            if (stack.isEmpty() || !com.runecraft.osrs.content.systems.Charges.needsActivation(Inv.idOf(stack))) {
                continue;
            }
            int ether = ItemCharges.get(stack);
            ItemCharges.set(stack, 0);
            ItemCharges.setActivated(stack, false);
            for (int left = ether; left > 0; ) {
                int n = Math.min(64, left);
                player.drop(Inv.stack("revenant_ether", n), true, false);
                left -= n;
            }
        }
    }

    /** Gives a revenant its fighting behaviour (once). */
    static void attach(OsrsMonsterEntity mob) {
        if (mob.attackInterceptor == null) {
            mob.attackInterceptor = RevenantBehaviour::attack;
        }
    }

    /** Whether the player wears a bracelet of ethereum holding revenant ether. */
    static boolean chargedBracelet(ServerPlayer player) {
        var hands = SysState.of(player).worn(Jewellery.Slot.HANDS);
        return !hands.isEmpty() && "bracelet_of_ethereum".equals(Inv.idOf(hands)) && ItemCharges.get(hands) > 0;
    }

    /** Whether the player has the Forinthry Surge (and still wears the amulet of avarice). */
    static boolean surge(ServerPlayer player) {
        Long until = SURGE.get(player.getUUID());
        return until != null && ActionManager.gameTick() < until && RevenantLoot.wearingAvarice(player);
    }

    /** Grants the Forinthry Surge (the maledictus's top damager wearing an amulet of avarice). */
    static void grantSurge(ServerPlayer player) {
        SURGE.put(player.getUUID(), ActionManager.gameTick() + RevenantRewards.FORINTHRY_SURGE_TICKS);
        Msg.game(player, "You feel the Forinthry Surge empower you against the revenants.");
    }

    /** Heals left for a revenant (the self-test). */
    static int healsLeft(LivingEntity mob) {
        HealState s = HEALS.get(mob);
        return s == null ? -1 : s.left;
    }

    private static boolean attack(OsrsMonsterEntity mob, ServerPlayer target, AttackDef planned) {
        HealState h = HEALS.computeIfAbsent(mob, m -> new HealState());
        if (h.left < 0) {
            h.left = RevenantRewards.heals(mob.getRandom()::nextInt);
        }
        long now = ActionManager.gameTick();
        if (RevenantRewards.shouldHeal(mob.getHealth(), mob.getMaxHealth(), now, h.last, h.left)) {
            h.left--;
            h.last = now;
            mob.setHealth(Math.min(mob.getMaxHealth(), mob.getHealth() + RevenantRewards.HEAL_AMOUNT));
            if (mob.level() instanceof ServerLevel world) {
                world.sendParticles(ParticleTypes.ELECTRIC_SPARK, mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(), 30, 0.3,
                        mob.getBbHeight() / 2, 0.3, 0.2);
                world.playSound(null, mob.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 0.6f, 1.4f);
            }
            return true;
        }
        List<AttackDef> attacks = mob.stats() == null || mob.stats().profile() == null ? List.of() : mob.stats().profile().attacks;
        AttackDef melee = null;
        for (AttackDef a : attacks) {
            if (a.style().isMelee()) {
                melee = a;
            }
        }
        boolean reach = melee != null && mob.distanceTo(target) - mob.getBbWidth() / 2 <= melee.range();
        PlayerData data = Players.data(target);
        AttackType protect = data.isActive(Prayer.PROTECT_FROM_MAGIC) ? AttackType.MAGIC
                : data.isActive(Prayer.PROTECT_FROM_MISSILES) ? AttackType.RANGED
                : data.isActive(Prayer.PROTECT_FROM_MELEE) ? AttackType.SLASH : null;
        EquipmentStats gear = HitCalcBridge.gearStats(target);
        AttackType meleeType = melee == null ? AttackType.SLASH : melee.style();
        AttackType style = RevenantRewards.chooseStyle(meleeType, reach, gear.defence(meleeType), gear.defence(AttackType.RANGED),
                gear.defence(AttackType.MAGIC), protect);
        for (AttackDef a : attacks) {
            if (a.style() == style) {
                Mechanics.perform(mob, target, a, 1.0);
                return true;
            }
        }
        return false;
    }
}
