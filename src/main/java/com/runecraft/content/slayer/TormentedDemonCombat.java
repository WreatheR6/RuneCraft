package com.runecraft.content.slayer;

import com.runecraft.action.ActionManager;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.MonsterStats;
import com.runecraft.content.systems.SysCombat;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.slayer.TormentedDemons;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Tormented Demon mechanics (see {@link TormentedDemons}): it prays against one combat style and swaps to the style that
 * last damaged it every time it loses 25% of its hitpoints; its fire shield weakens non-demonbane hits except right after
 * the first hit and after each fire bomb. Also the demonbane passives of Silverlight, Darklight, Arclight, Emberlight, the
 * scorching bow and burning claws against every demon, and forging Emberlight (a tormented synapse used on an anvil).
 */
public final class TormentedDemonCombat implements CombatHooks.PlayerAttackModifier {
    private enum Style { MELEE, RANGED, MAGIC }

    private static final class State {
        Style prayer;
        Style lastStyle;
        int lostSinceSwap;
        long shieldDownUntil;
        long nextBomb;
    }

    private static final Map<LivingEntity, State> STATES = new WeakHashMap<>();
    /** Game ticks between fire bombs: about {@link TormentedDemons#ATTACKS_PER_BOMB} attacks at speed 6. */
    private static final int BOMB_INTERVAL = TormentedDemons.ATTACKS_PER_BOMB * 6;

    private TormentedDemonCombat() {
    }

    static void register() {
        CombatHooks.addModifier(new TormentedDemonCombat());
        // Emberlight: use a tormented synapse on an anvil with a fully charged Arclight and a hammer.
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (hand != net.minecraft.util.Hand.MAIN_HAND || player.getMainHandStack().isEmpty()
                    || !"tormented_synapse".equals(Inv.idOf(player.getMainHandStack()))) {
                return net.minecraft.util.ActionResult.PASS;
            }
            net.minecraft.block.Block block = world.getBlockState(hit.getBlockPos()).getBlock();
            boolean anvil = block instanceof com.runecraft.block.StationBlock s && s.station() == com.runecraft.osrs.Station.ANVIL
                    || block == net.minecraft.block.Blocks.ANVIL || block == net.minecraft.block.Blocks.CHIPPED_ANVIL
                    || block == net.minecraft.block.Blocks.DAMAGED_ANVIL;
            if (!anvil) {
                return net.minecraft.util.ActionResult.PASS;
            }
            if (!world.isClient() && player instanceof ServerPlayerEntity sp) {
                forgeEmberlight(sp);
            }
            return net.minecraft.util.ActionResult.SUCCESS;
        });
    }

    private static void forgeEmberlight(ServerPlayerEntity p) {
        net.minecraft.item.ItemStack arclight = net.minecraft.item.ItemStack.EMPTY;
        for (net.minecraft.item.ItemStack s : p.getInventory().main) {
            if (!s.isEmpty() && com.runecraft.osrs.content.systems.Charges.ARCLIGHT.equals(Inv.idOf(s))
                    && (arclight.isEmpty() || com.runecraft.item.ItemCharges.get(s) > com.runecraft.item.ItemCharges.get(arclight))) {
                arclight = s;
            }
        }
        if (arclight.isEmpty()) {
            Msg.game(p, "You need Arclight to reforge with the synapse.");
            return;
        }
        int smithing = com.runecraft.player.Players.data(p).skills().currentLevel(com.runecraft.osrs.Skill.SMITHING);
        String problem = TormentedDemons.emberlightProblem(smithing, com.runecraft.item.ItemCharges.get(arclight),
                com.runecraft.osrs.content.systems.Charges.ARCLIGHT_MAX, Inv.has(p, "hammer", 1));
        if (problem != null) {
            Msg.game(p, problem);
            return;
        }
        arclight.decrement(1);
        Inv.remove(p, "tormented_synapse", 1);
        Inv.give(p, "emberlight", 1);
        com.runecraft.collection.CollectionLog.obtain(p, "emberlight");
        com.runecraft.skill.XpService.add(p, com.runecraft.osrs.Skill.SMITHING, TormentedDemons.EMBERLIGHT_XP);
        p.swingHand(net.minecraft.util.Hand.MAIN_HAND, true);
        Msg.action(p, "You hammer the tormented synapse into Arclight. It bursts into flame: Emberlight!");
    }

    private static boolean isDemon(LivingEntity target) {
        MonsterStats stats = MonsterStats.of(target);
        return stats.profile() != null && TormentedDemons.ID.equals(stats.profile().id);
    }

    private static Style style(AttackType type) {
        return type == AttackType.MAGIC ? Style.MAGIC : type == AttackType.RANGED ? Style.RANGED : Style.MELEE;
    }

    private static String weapon(ServerPlayerEntity p) {
        return p.getMainHandStack().isEmpty() ? "" : Inv.idOf(p.getMainHandStack());
    }

    private static double demonbane(ServerPlayerEntity p, LivingEntity target) {
        Double bonus = TormentedDemons.DEMONBANE_BONUS.get(weapon(p));
        return bonus != null && SysCombat.isDemon(target) ? bonus : 1.0;
    }

    @Override
    public double accuracy(ServerPlayerEntity player, LivingEntity target, AttackType type) {
        return demonbane(player, target);
    }

    @Override
    public double maxHit(ServerPlayerEntity player, LivingEntity target, AttackType type) {
        return demonbane(player, target);
    }

    private static State state(LivingEntity demon, long now) {
        return STATES.computeIfAbsent(demon, d -> {
            State s = new State();
            s.prayer = Style.values()[d.getRandom().nextInt(3)];
            s.shieldDownUntil = now + TormentedDemons.SHIELD_DOWN_TICKS; // the first hit knocks the shield down
            s.nextBomb = now + BOMB_INTERVAL;
            return s;
        });
    }

    @Override
    public int afterRoll(ServerPlayerEntity player, LivingEntity target, AttackType type, int damage) {
        if (!isDemon(target)) {
            return damage;
        }
        long now = ActionManager.gameTick();
        State s = state(target, now);
        if (now >= s.nextBomb) {
            // Approximation: the shield drops on the fire bomb cadence rather than on the bomb itself.
            s.shieldDownUntil = now + TormentedDemons.SHIELD_DOWN_TICKS;
            s.nextBomb = now + BOMB_INTERVAL;
            ((ServerWorld) target.getWorld()).spawnParticles(ParticleTypes.LARGE_SMOKE, target.getX(), target.getBodyY(0.8),
                    target.getZ(), 20, 0.5, 0.6, 0.5, 0.02);
        }
        if (s.prayer == style(type)) {
            Msg.action(player, "The tormented demon's prayer protects it from your attack.");
            return 0;
        }
        return TormentedDemons.throughShield(damage, now >= s.shieldDownUntil, weapon(player));
    }

    @Override
    public void afterHit(ServerPlayerEntity player, LivingEntity target, AttackType type, int dealt) {
        if (dealt <= 0 || !isDemon(target)) {
            return;
        }
        State s = state(target, ActionManager.gameTick());
        s.lastStyle = style(type);
        s.lostSinceSwap += dealt;
        if (TormentedDemons.swapsPrayer(s.lostSinceSwap, (int) Math.ceil(target.getMaxHealth()))) {
            s.lostSinceSwap = 0;
            if (s.prayer != s.lastStyle) {
                s.prayer = s.lastStyle;
                Msg.action(player, "The tormented demon now protects itself from " + s.prayer.name().toLowerCase(java.util.Locale.ROOT)
                        + ".");
            }
        }
    }
}
