package com.runecraft.combat;

import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.PrayerEffects;
import com.runecraft.osrs.Skill;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModItems;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import com.runecraft.platform.ServerLivingEntityEvents;
import com.runecraft.platform.ServerPlayerEvents;
import com.runecraft.platform.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The prayers whose effect isn't a stat bonus: Smite, Redemption, Retribution and Protect Item (items kept on death).
 * Formulas live in {@link PrayerEffects}.
 */
public final class PrayerEffectsService {
    /** Items kept on death, waiting for the player to respawn. */
    private static final Map<UUID, Stash> KEPT = new HashMap<>();

    private record Stash(ServerPlayer body, List<ItemStack> items) {
    }

    private PrayerEffectsService() {
    }

    static void register() {
        CombatHooks.addModifier(new CombatHooks.PlayerAttackModifier() {
            @Override
            public void afterHit(ServerPlayer player, LivingEntity target, AttackType type, int dealt) {
                smite(player, target, dealt);
            }
        });
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer player && Gielinor.is(player)) {
                keepItems(player);
            }
            return true;
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player && Gielinor.is(player)) {
                retribution(player);
            }
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            Stash stash = KEPT.remove(newPlayer.getUUID());
            if (stash != null) {
                stash.items().forEach(s -> com.runecraft.util.Inv.give(newPlayer, s));
                Msg.game(newPlayer, "You keep your " + stash.items().size() + " most valuable item stack"
                        + (stash.items().size() == 1 ? "." : "s."));
            }
        });
        // Another mod or module may cancel a death after the items were set aside: hand them straight back.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (KEPT.isEmpty()) {
                return;
            }
            for (Iterator<Map.Entry<UUID, Stash>> it = KEPT.entrySet().iterator(); it.hasNext(); ) {
                Stash s = it.next().getValue();
                if (s.body().isAlive() && !s.body().isRemoved()) {
                    s.items().forEach(i -> com.runecraft.util.Inv.give(s.body(), i));
                    it.remove();
                }
            }
        });
    }

    /** Smite: a quarter of the damage dealt to a player is drained from their Prayer points. */
    private static void smite(ServerPlayer player, LivingEntity target, int dealt) {
        if (dealt <= 0 || !(target instanceof ServerPlayer victim) || !Players.data(player).isActive(Prayer.SMITE)) {
            return;
        }
        int drain = PrayerEffects.smiteDrain(dealt);
        if (drain <= 0) {
            return;
        }
        PlayerData vd = Players.data(victim);
        vd.setPrayerPoints(Math.max(0, vd.prayerPoints() - drain));
        ServerNetworking.sync(victim);
    }

    /** Redemption, checked every game tick: below 10% HP heals 25% of Prayer and uses up all Prayer points. */
    static void redemption(ServerPlayer player, PlayerData data) {
        if (!data.isActive(Prayer.REDEMPTION) || !player.isAlive()) {
            return;
        }
        int hp = (int) Math.ceil(player.getHealth());
        if (!PrayerEffects.redemptionTriggers(hp, (int) player.getMaxHealth())) {
            return;
        }
        player.heal(PrayerEffects.redemptionHeal(data.skills().level(Skill.PRAYER)));
        data.setPrayerPoints(0);
        data.activePrayers().clear();
        if (player.level() instanceof ServerLevel world) {
            world.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.5, player.getZ(), 8, 0.4, 0.4, 0.4, 0.1);
        }
        Msg.game(player, "You have been healed by the gods, but your Prayer has been drained.");
        ServerNetworking.sync(player);
    }

    /** Retribution: on death, hits adjacent enemies (just the attacker in single-way combat) for up to 25% of Prayer. */
    private static void retribution(ServerPlayer player) {
        PlayerData data = Players.data(player);
        if (!data.isActive(Prayer.RETRIBUTION) || !(player.level() instanceof ServerLevel world)) {
            return;
        }
        int max = PrayerEffects.retributionMaxHit(data.skills().level(Skill.PRAYER));
        List<LivingEntity> targets = new ArrayList<>();
        if (CombatZones.isMulti(player)) {
            // Adjacent squares: one OSRS tile is two blocks.
            targets.addAll(world.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(3.0),
                    e -> e != player && e.isAlive() && !(e instanceof com.runecraft.entity.QuestNpcEntity)));
        } else if (player.getLastHurtByMob() != null && player.getLastHurtByMob().isAlive() && player.getLastHurtByMob().distanceToSqr(player) <= 16) {
            targets.add(player.getLastHurtByMob());
        }
        world.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 1, player.getZ(), 6, 1.0, 0.5, 1.0, 0.0);
        world.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.6f, 1.3f);
        for (LivingEntity t : targets) {
            if (t instanceof ServerPlayer other && !CombatHandler.canAttackPlayer(player, other)) {
                continue;
            }
            CombatHandler.applyDamage(player, t, player.getRandom().nextInt(max + 1));
        }
    }

    /** Protect Item / items kept on death: the 3 (4 with Protect Item) most valuable items stay with the player. */
    private static void keepItems(ServerPlayer player) {
        if (KEPT.containsKey(player.getUUID())) {
            return;
        }
        boolean protect = Players.data(player).isActive(Prayer.PROTECT_ITEM);
        int keep = SkullService.itemsKept(player, protect); // 0 (+1) while skulled
        Inventory inv = player.getInventory();
        List<PrayerEffects.Carried> carried = new ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            ItemDef def = ModItems.def(s);
            carried.add(new PrayerEffects.Carried(i, s.getCount(), def == null ? 0 : def.value()));
        }
        List<ItemStack> kept = new ArrayList<>();
        for (PrayerEffects.Kept k : PrayerEffects.keptOnDeath(carried, keep)) {
            kept.add(inv.getItem(k.slot()).split(k.count()));
        }
        if (!kept.isEmpty()) {
            KEPT.put(player.getUUID(), new Stash(player, kept));
        }
    }
}
