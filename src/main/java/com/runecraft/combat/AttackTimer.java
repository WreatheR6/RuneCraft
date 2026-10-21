package com.runecraft.combat;

import com.runecraft.RuneCraft;
import com.runecraft.action.ActionManager;
import com.runecraft.item.OsrsEquipmentItems;
import com.runecraft.osrs.AttackClock;
import com.runecraft.osrs.CombatBonuses;
import com.runecraft.osrs.CombatStyle;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.ItemDef;
import com.runecraft.platform.PacketByteBufs;
import com.runecraft.platform.ServerPlayNetworking;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModItems;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The server's one attack clock for every player attack (round 41, {@link AttackClock}): melee, autocast and powered-staff
 * spells, the other spellbooks' autocasts, bows, thrown weapons, PvP-minigame melee and auto-retaliate all ask it whether
 * they may resolve, and stamp it when they do.
 * <ul>
 *   <li>Readiness is {@code PlayerData.lastAttackTick + period}, the period being that of the attack that stamped it
 *       (stored here per player). With none recorded it is the wielded weapon's speed x 12, today's rule. Code that moves
 *       lastAttackTick itself keeps its meaning: self-tests set it far back to make a player ready, the Nightmare's spores
 *       push it forward on the attack tick ({@code lastAttackTick == now}), Amoxliatl's ice sets it a tick from ready.
 *       {@link #accept} therefore always stamps the resolve tick itself, before the hit is rolled.</li>
 *   <li>{@code // playability}: a click before the ready tick is buffered in one slot (OSRS queues the next action too)
 *       and resolves on the exact ready tick from {@link #tick}, which {@code PlayerEvents.everyMcTick} calls every MC tick.
 *       Before, an early click was dropped silently while the client's vanilla indicator restarted on every click, so
 *       the real interval drifted towards twice the OSRS period.</li>
 *   <li>Every accepted or buffered click sends the client {@link #CLOCK} for the timer under the crosshair.</li>
 * </ul>
 */
public final class AttackTimer {
    /** S2C: varint remaining MC, varint period MC, varint {@link AttackClock.Kind} ordinal, boolean queued. */
    public static final ResourceLocation CLOCK = RuneCraft.id("combat/attack_clock");
    /** Spell range in blocks (MagicService and SpellService cast up to 20). */
    public static final double SPELL_RANGE = 20.0;
    /** The transient ATTACK_SPEED modifier that makes vanilla's indicator take the OSRS period with no weapon modifier. */
    public static final ResourceLocation UNARMED_SPEED = RuneCraft.id("unarmed_speed");
    private static final int NO_TARGET_MESSAGE_MC = 20;

    /** Resolves one attack of a kind on a target (null for a bow shot in the look direction). */
    @FunctionalInterface
    public interface Resolver {
        void resolve(ServerPlayer player, LivingEntity target);
    }

    private static final class State {
        final AttackClock.Buffer buffer = new AttackClock.Buffer();
        Resolver resolver;
        int slot = -1;
        Item item;
        ResourceKey<Level> level;
        long noTargetAt = Long.MIN_VALUE / 4;
        /** MC tick the bow draw that last fired started at (one release shot per draw). */
        long shotDrawStart = Long.MIN_VALUE / 4;
    }

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    private AttackTimer() {
    }

    private static State state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), u -> new State());
    }

    /** The wielded weapon's period: the fallback when no attack has been recorded yet. */
    private static int fallbackPeriod(ServerPlayer player) {
        return AttackClock.periodMc(Equipment.weaponSpeed(player, Players.data(player).skills()));
    }

    /** First MC tick the player's next attack may resolve. */
    public static long readyAt(ServerPlayer player) {
        State s = STATES.get(player.getUUID());
        long last = Players.data(player).lastAttackTick;
        return s == null ? AttackClock.readyAt(last, fallbackPeriod(player)) : s.buffer.readyAt(last, fallbackPeriod(player));
    }

    public static boolean ready(ServerPlayer player) {
        return AttackClock.ready(ActionManager.mcTick(), readyAt(player));
    }

    /** MC ticks until the next attack may resolve (0 when ready). */
    public static long remaining(ServerPlayer player) {
        return Math.max(0, readyAt(player) - ActionManager.mcTick());
    }

    /** True while the player attacked within the last attack period plus {@code extraMc} (auto-retaliate leaves them be). */
    public static boolean recentlyAttacked(ServerPlayer player, int extraMc) {
        return ActionManager.mcTick() < readyAt(player) + extraMc;
    }

    /** True while a click is buffered. */
    public static boolean hasPending(ServerPlayer player) {
        State s = STATES.get(player.getUUID());
        return s != null && s.buffer.hasPending();
    }

    /**
     * Stamps an attack that resolves now: lastAttackTick = now and its period, then tells the client. Called by every
     * resolver once the attack is certain (after its own refusals), before the hit is rolled.
     */
    public static void accept(ServerPlayer player, AttackClock.Kind kind, int periodMc) {
        Players.data(player).lastAttackTick = ActionManager.mcTick();
        State s = state(player);
        s.buffer.accept(periodMc);
        s.resolver = null;
        send(player, periodMc, periodMc, kind, false);
    }

    /**
     * Adds {@code mc} to a weapon delay that is still running (OSRS food, wiki Food/Fast foods: "Foods only add to an existing
     * attack delay between weapon attacks"); nothing when the weapon is ready. The client's timer bar is told the new wait.
     * Returns the delay added.
     */
    public static int addDelay(ServerPlayer player, int mc) {
        if (mc <= 0 || ready(player)) {
            return 0;
        }
        Players.data(player).lastAttackTick += mc;
        State s = STATES.get(player.getUUID());
        int period = s == null ? fallbackPeriod(player) : s.buffer.periodMc(fallbackPeriod(player));
        long left = remaining(player);
        boolean queued = s != null && s.buffer.hasPending();
        send(player, left, (int) Math.max(period, left), queued ? s.buffer.pendingKind() : kindFor(player), queued);
        return mc;
    }

    /** A click on {@code target} resolved with the kind's own resolver. Returns true when it resolved now. */
    public static boolean click(ServerPlayer player, LivingEntity target, AttackClock.Kind kind) {
        return click(player, target, kind, resolver(kind));
    }

    /** A click with its own resolver (PvP-minigame melee). Returns true when it resolved now, false when it was buffered. */
    public static boolean click(ServerPlayer player, LivingEntity target, AttackClock.Kind kind, Resolver resolver) {
        if (gate(player, target, kind, resolver)) {
            resolver.resolve(player, target);
            return true;
        }
        return false;
    }

    /**
     * The one attack gate: true when an attack may resolve right now (the caller resolves it and calls {@link #accept});
     * otherwise the click is buffered with {@code resolver} (a newer click replaces an older one), the client is told
     * (a queued marker) and the player hears a quiet click, and false is returned.
     */
    public static boolean gate(ServerPlayer player, LivingEntity target, AttackClock.Kind kind, Resolver resolver) {
        State s = state(player);
        long now = ActionManager.mcTick();
        long last = Players.data(player).lastAttackTick;
        boolean had = s.buffer.hasPending();
        if (s.buffer.click(now, last, fallbackPeriod(player), kind, target == null ? -1 : target.getId())) {
            s.resolver = null;
            return true;
        }
        s.resolver = resolver;
        s.slot = player.getInventory().selected;
        s.item = player.getMainHandItem().getItem();
        s.level = player.level().dimension();
        long readyAt = s.buffer.readyAt(last, fallbackPeriod(player));
        send(player, Math.max(0, readyAt - now), s.buffer.periodMc(fallbackPeriod(player)), kind, true);
        if (!had) {
            // playability: the click was heard (OSRS shows the queued action only through the next swing)
            player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.12f, 1.9f);
        }
        return false;
    }

    /** Every MC tick for each listed player ({@code PlayerEvents.everyMcTick}): resolves or drops the buffered click. */
    public static void tick(ServerPlayer player) {
        updateIndicatorSpeed(player);
        State s = STATES.get(player.getUUID());
        if (s == null || !s.buffer.hasPending()) {
            return;
        }
        if (player.isDeadOrDying() || player.isRemoved() || player.getInventory().selected != s.slot
                || player.getMainHandItem().getItem() != s.item || !player.level().dimension().equals(s.level)) {
            drop(player, s); // switched slot or weapon, died, changed level
            return;
        }
        LivingEntity target = null;
        if (s.buffer.pendingTarget() >= 0) {
            Entity e = player.level().getEntity(s.buffer.pendingTarget());
            if (!(e instanceof LivingEntity l) || !l.isAlive() || l.isRemoved()) {
                drop(player, s); // the target died
                return;
            }
            target = l;
        }
        long now = ActionManager.mcTick();
        long last = Players.data(player).lastAttackTick;
        if (!AttackClock.ready(now, s.buffer.readyAt(last, fallbackPeriod(player)))) {
            return;
        }
        AttackClock.Kind kind = s.buffer.pendingKind();
        boolean valid = target == null || canHit(player, target, kind);
        AttackClock.Step step = s.buffer.tick(now, last, fallbackPeriod(player), valid);
        if (step == AttackClock.Step.FIRE) {
            Resolver r = s.resolver != null ? s.resolver : resolver(kind);
            s.resolver = null;
            player.swing(InteractionHand.MAIN_HAND, true);
            resolve(player, target, kind, r);
        } else if (step == AttackClock.Step.DROP) {
            drop(player, s);
        }
    }

    /**
     * Runs a resolver from the server tick (a buffered click, auto-retaliate). A click resolves inside packet handling,
     * where an exception only costs that packet; here it is logged instead of stopping the server tick.
     */
    static void resolve(ServerPlayer player, LivingEntity target, AttackClock.Kind kind, Resolver resolver) {
        try {
            resolver.resolve(player, target);
        } catch (RuntimeException e) {
            RuneCraft.LOGGER.error("RuneCraft: {} attack by {} failed", kind, player.getName().getString(), e);
        }
    }

    /** {@link #tick} plus auto-retaliate for a player that is not in the server's player list (the self-test's fake player). */
    public static void tickForSelfTest(ServerPlayer player) {
        tick(player);
        AutoRetaliate.mcTick(player);
    }

    public static void forget(UUID player) {
        STATES.remove(player);
    }

    private static void drop(ServerPlayer player, State s) {
        AttackClock.Kind kind = s.buffer.pendingKind();
        s.buffer.clearPending();
        s.resolver = null;
        send(player, remaining(player), s.buffer.periodMc(fallbackPeriod(player)), kind == null ? AttackClock.Kind.MELEE : kind, false);
    }

    /** Whether a buffered attack of this kind can land on {@code target} from where the player stands now. */
    static boolean canHit(ServerPlayer player, LivingEntity target, AttackClock.Kind kind) {
        if (target.level() != player.level()) {
            return false;
        }
        if (!(target instanceof ServerPlayer) && CombatZones.check(player, target) != com.runecraft.osrs.SingleCombat.Verdict.OK) {
            return false;
        }
        return switch (kind) {
            case MELEE -> player.canInteractWithEntity(target, 0.5) && player.hasLineOfSight(target);
            case SPELL -> player.distanceTo(target) <= SPELL_RANGE && clearShot(player, target);
            default -> clearShot(player, target);
        };
    }

    /** No block between the player's eyes and the middle of the target. */
    public static boolean clearShot(ServerPlayer player, LivingEntity target) {
        Vec3 eye = player.getEyePosition();
        Vec3 centre = target.getBoundingBox().getCenter();
        return player.level().clip(new ClipContext(eye, centre, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
                .getType() == HitResult.Type.MISS;
    }

    /** "No target." at most once a second, so a held staff or a spam of clicks at nothing doesn't flood the action bar. */
    public static void noTarget(ServerPlayer player) {
        State s = state(player);
        long now = ActionManager.mcTick();
        if (now - s.noTargetAt >= NO_TARGET_MESSAGE_MC) {
            s.noTargetAt = now;
            Msg.action(player, "No target.");
        }
    }

    // ---------------- Kinds and resolvers ----------------

    /** True when the wielded weapon casts: a powered staff, or a staff with an autocast spell set (either spellbook). */
    public static boolean casts(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        String id = Inv.idOf(held);
        if (CombatBonuses.poweredStaffMaxHit(id, 1) >= 0) {
            return true;
        }
        ItemDef def = ModItems.def(held);
        EquipmentDef weapon = def == null ? null : def.equipment();
        boolean staff = def != null && (def.kind() == ItemDef.Kind.STAFF
                || weapon != null && weapon.weaponClass() == EquipmentDef.WeaponClass.STAFF);
        return staff && (Players.data(player).autocast() != null
                || !com.runecraft.content.systems.SysState.of(player).autocast.isEmpty());
    }

    /** What a click on a monster does with what is wielded: a bow shoots, a casting staff casts, anything else is melee. */
    public static AttackClock.Kind kindFor(ServerPlayer player) {
        if (player.getMainHandItem().getItem() instanceof OsrsEquipmentItems.Bow) {
            return AttackClock.Kind.RANGED;
        }
        return casts(player) ? AttackClock.Kind.SPELL : AttackClock.Kind.MELEE;
    }

    /** The resolver of a kind. */
    public static Resolver resolver(AttackClock.Kind kind) {
        return switch (kind) {
            case MELEE -> CombatHandler::resolveMelee;
            case SPELL -> AttackTimer::castSpell;
            case RANGED -> AttackTimer::fireHeldBow;
            case THROWN -> (p, t) -> {
            };
        };
    }

    /** The autocast spell or powered staff at {@code target}; the same dispatch as auto-retaliate. */
    static void castSpell(ServerPlayer player, LivingEntity target) {
        if (target == null) {
            return;
        }
        boolean powered = CombatBonuses.poweredStaffMaxHit(Inv.idOf(player.getMainHandItem()), 1) >= 0;
        if (powered || Players.data(player).autocast() != null) {
            MagicService.autocastAttack(player, target);
        } else {
            com.runecraft.content.systems.SpellService.retaliate(player, target);
        }
    }

    /** One shot from the bow in the main hand, at {@code target} when given (left-click, auto-retaliate). */
    static void fireHeldBow(ServerPlayer player, LivingEntity target) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            if (held.getItem() instanceof OsrsEquipmentItems.Bow bow) {
                bow.fire(player, hand, held, target);
                return;
            }
        }
    }

    /** MC ticks between shots with the wielded ranged weapon on the player's style (Rapid is a tick faster). */
    public static int rangedPeriod(ServerPlayer player) {
        PlayerData data = Players.data(player);
        int speed = Equipment.weaponSpeed(player, data.skills());
        return AttackClock.periodMc(AttackClock.rangedSpeed(speed, data.style().forRanged() == CombatStyle.RAPID));
    }

    /** Whether this bow draw (started {@code drawnMc} ago) already fired a shot; release only fires a draw that hasn't. */
    public static boolean firedThisDraw(ServerPlayer player, int drawnMc) {
        return state(player).shotDrawStart == ActionManager.mcTick() - drawnMc;
    }

    public static void markDrawShot(ServerPlayer player, int drawnMc) {
        state(player).shotDrawStart = ActionManager.mcTick() - drawnMc;
    }

    // ---------------- Vanilla indicator ----------------

    /**
     * {@code // playability}: vanilla's attack indicator fills at the ATTACK_SPEED attribute. Weapons carry the OSRS speed
     * as their modifier (ModItems); with no such modifier (empty hand, bow, thrown weapon, any other item) a transient
     * {@link #UNARMED_SPEED} gives the OSRS period of what is held (unarmed 4 ticks = 48 MC) inside Gielinor.
     */
    static void updateIndicatorSpeed(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (speed == null) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        boolean want = Gielinor.is(player) && !ownAttackSpeed(held);
        AttributeModifier current = speed.getModifier(UNARMED_SPEED);
        if (!want) {
            if (current != null) {
                speed.removeModifier(UNARMED_SPEED);
            }
            return;
        }
        int ticks = held.getItem() instanceof OsrsEquipmentItems.Bow ? rangedPeriod(player) / AttackClock.MC_PER_GAME_TICK
                : Equipment.weaponSpeed(player, Players.data(player).skills());
        double amount = AttackClock.vanillaAttackSpeedModifier(ticks);
        if (current == null || Math.abs(current.amount() - amount) > 1e-6) {
            speed.removeModifier(UNARMED_SPEED);
            speed.addTransientModifier(new AttributeModifier(UNARMED_SPEED, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    /** True when the stack's own main-hand modifiers set the attack speed (OSRS weapons, axes, pickaxes, staffs). */
    public static boolean ownAttackSpeed(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        boolean[] found = {false};
        stack.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.value() == Attributes.ATTACK_SPEED.value()) {
                found[0] = true;
            }
        });
        return found[0];
    }

    /** The vanilla indicator's period (MC) for a stack in the main hand: base 4 plus the stack's own ATTACK_SPEED modifiers. */
    public static double vanillaPeriodMc(ItemStack stack) {
        double[] speed = {AttackClock.VANILLA_BASE_ATTACK_SPEED};
        stack.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.value() == Attributes.ATTACK_SPEED.value() && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                speed[0] += modifier.amount();
            }
        });
        return AttackClock.vanillaPeriodMc(speed[0]);
    }

    // ---------------- Client packet ----------------

    private static void send(ServerPlayer player, long remainingMc, int periodMc, AttackClock.Kind kind, boolean queued) {
        if (!ServerPlayNetworking.canSend(player, CLOCK)) {
            return;
        }
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt((int) Math.min(Integer.MAX_VALUE, Math.max(0, remainingMc)));
        buf.writeVarInt(Math.max(0, periodMc));
        buf.writeVarInt(kind.ordinal());
        buf.writeBoolean(queued);
        ServerPlayNetworking.send(player, CLOCK, buf);
    }
}
