package com.runecraft.item;

import com.runecraft.combat.AttackTimer;
import com.runecraft.osrs.AttackClock;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.data.AmmoRules;
import com.runecraft.osrs.data.ArrowTier;
import com.runecraft.skill.ItemUses;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

/** Vanilla-backed equipment classes that also carry their OSRS {@link ItemDef}. */
public final class OsrsEquipmentItems {
    /** Command tag prefix carrying an arrow's ranged strength to the combat handler. */
    public static final String RANGED_STRENGTH_TAG = "runecraft_rstr_";
    /** Vanilla's AxeItem / PickaxeItem attack speeds, kept for tools with no OSRS attack speed. */
    private static final float VANILLA_AXE_SPEED = -3.0f;
    private static final float VANILLA_PICKAXE_SPEED = -2.8f;

    private OsrsEquipmentItems() {
    }

    /**
     * {@code // playability}: the vanilla ATTACK_SPEED modifier of an item with an OSRS attack speed, so vanilla's attack
     * indicator (and the hotbar one) fills in exactly the OSRS period; {@code fallback} for items without one.
     */
    static float attackSpeed(ItemDef def, float fallback) {
        return def != null && def.equipment() != null && def.equipment().attackSpeed() > 0
                ? AttackClock.vanillaAttackSpeedModifier(def.equipment().attackSpeed()) : fallback;
    }

    public static class Axe extends AxeItem implements OsrsItemLike {
        private final ItemDef def;

        public Axe(ItemDef def, Tier material, Properties settings) {
            // Replaces (doesn't stack with) AxeItem's own -3.0 attack speed.
            super(material, settings.attributes(AxeItem.createAttributes(material, 5.0f, attackSpeed(def, VANILLA_AXE_SPEED))));
            this.def = def;
        }

        @Override
        public ItemDef def() {
            return def;
        }
    }

    public static class Pickaxe extends PickaxeItem implements OsrsItemLike {
        private final ItemDef def;

        public Pickaxe(ItemDef def, Tier material, Properties settings) {
            super(material, settings.attributes(PickaxeItem.createAttributes(material, 1, attackSpeed(def, VANILLA_PICKAXE_SPEED))));
            this.def = def;
        }

        @Override
        public ItemDef def() {
            return def;
        }
    }

    public static class Weapon extends SwordItem implements OsrsItemLike {
        /** {@code // playability}: a halberd reaches 2 OSRS tiles, one block further than other melee weapons. */
        public static final double HALBERD_EXTRA_REACH = 1.0;
        private final ItemDef def;

        public Weapon(ItemDef def, Tier material, int attackDamage, float attackSpeed, Properties settings) {
            super(material, settings.attributes(attributes(def, material, attackDamage, attackSpeed)));
            this.def = def;
        }

        private static ItemAttributeModifiers attributes(ItemDef def, Tier material, int attackDamage, float attackSpeed) {
            ItemAttributeModifiers mods = SwordItem.createAttributes(material, attackDamage, attackSpeed);
            if (def != null && def.id().contains("halberd")) {
                mods = mods.withModifierAdded(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(
                        com.runecraft.RuneCraft.id("halberd_reach"), HALBERD_EXTRA_REACH, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND);
            }
            return mods;
        }

        @Override
        public ItemDef def() {
            return def;
        }
    }

    public static class Armour extends ArmorItem implements OsrsItemLike {
        private final ItemDef def;

        public Armour(ItemDef def, Holder<ArmorMaterial> material, ArmorItem.Type type, Properties settings) {
            super(material, type, settings);
            this.def = def;
        }

        @Override
        public ItemDef def() {
            return def;
        }
    }

    /**
     * OSRS bows and crossbows. Outside Gielinor they are vanilla bows. Inside Gielinor ({@code // playability}, round 41)
     * they shoot on the player's attack clock instead of on vanilla's draw power: OSRS fires one arrow every weapon-speed
     * ticks (Rapid one tick faster) from a click, whatever the draw, so
     * <ul>
     *   <li>holding the use key fires a full-velocity arrow each time the clock is ready, once drawn
     *       {@value AttackClock#HOLD_DRAW_MC} MC ticks (hold to fire);</li>
     *   <li>a release after a draw of {@value AttackClock#TAP_DRAW_MC}+ MC ticks that fired nothing fires now when ready,
     *       or is buffered and fires on the ready tick while the bow is still held;</li>
     *   <li>a left click on a monster shoots at it (aimed, through {@link AttackTimer});</li>
     *   <li>the bow never wears out (OSRS bows don't; crystal bows keep their charges in {@link ItemCharges}).</li>
     * </ul>
     * Every arrow is shot at velocity 3, so aim doesn't depend on draw time; its damage is still rolled when it lands
     * (CombatHandler.playerRanged).
     */
    public static class Bow extends BowItem implements OsrsItemLike {
        /** Vanilla's full-draw arrow velocity. */
        public static final float FULL_VELOCITY = 3.0f;
        private final ItemDef def;

        public Bow(ItemDef def, Properties settings) {
            super(settings);
            this.def = def;
        }

        @Override
        public ItemDef def() {
            return def;
        }

        /** OSRS ammo restrictions: e.g. a willow bow can't fire adamant arrows and a crossbow fires only bolts. */
        @Override
        public Predicate<ItemStack> getAllSupportedProjectiles() {
            return stack -> {
                if (!ARROW_ONLY.test(stack)) {
                    return false;
                }
                ItemDef ammo = stack.getItem() instanceof OsrsItemLike like ? like.def() : null;
                return AmmoRules.canFire(def.id(), ammo == null ? null : ammo.id());
            };
        }

        private static boolean osrs(LivingEntity user) {
            return user instanceof Player && Gielinor.is(user);
        }

        /** Hold to fire: every MC tick of the draw. */
        @Override
        public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseDuration) {
            if (!osrs(user)) {
                super.onUseTick(level, user, stack, remainingUseDuration);
                return;
            }
            if (level instanceof ServerLevel && user instanceof ServerPlayer player) {
                int drawn = getUseDuration(stack, user) - remainingUseDuration;
                if (drawn >= AttackClock.HOLD_DRAW_MC && AttackTimer.ready(player)) {
                    if (fire(player, handOf(player, stack), stack, null)) {
                        AttackTimer.markDrawShot(player, drawn);
                    } else {
                        // Out of ammo mid-draw: the draw ends, so the message shows once (BowItem.use won't start a
                        // new draw without ammo). stopUsingItem, not releaseUsingItem, which would try a release shot.
                        player.stopUsingItem();
                    }
                }
            }
        }

        @Override
        public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
            if (!osrs(user)) {
                super.releaseUsing(stack, level, user, timeLeft);
                return;
            }
            if (!(level instanceof ServerLevel) || !(user instanceof ServerPlayer player)) {
                return; // the server shoots
            }
            int drawn = getUseDuration(stack, user) - timeLeft;
            if (drawn < AttackClock.TAP_DRAW_MC || AttackTimer.firedThisDraw(player, drawn)) {
                return;
            }
            if (AttackTimer.gate(player, null, AttackClock.Kind.RANGED, AttackTimer.resolver(AttackClock.Kind.RANGED))
                    && fire(player, handOf(player, stack), stack, null)) {
                AttackTimer.markDrawShot(player, drawn);
            }
        }

        private static InteractionHand handOf(Player player, ItemStack stack) {
            return player.getOffhandItem() == stack ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        }

        /**
         * One shot that the attack clock allows now: the next ammo at full velocity (at {@code target} when given, else
         * where the player looks), then the clock and the item cooldown (the hotbar sweep) take the weapon's ranged period.
         * Returns false (with the OSRS message) when there is no ammo.
         */
        public boolean fire(ServerPlayer player, InteractionHand hand, ItemStack bow, LivingEntity target) {
            ItemStack ammo = player.getProjectile(bow);
            if (ammo.isEmpty()) {
                Msg.game(player, "There is no ammo left in your quiver.");
                return false;
            }
            List<ItemStack> drawn = draw(bow, ammo, player);
            if (drawn.isEmpty()) {
                return false;
            }
            ServerLevel level = (ServerLevel) player.level();
            int period = AttackTimer.rangedPeriod(player);
            AttackTimer.accept(player, AttackClock.Kind.RANGED, period);
            shoot(level, player, hand, bow, drawn, FULL_VELOCITY, 1.0f, false, target);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0f,
                    1.0f / (level.getRandom().nextFloat() * 0.4f + 1.2f) + 0.5f);
            player.awardStat(Stats.ITEM_USED.get(this));
            player.getCooldowns().addCooldown(this, period);
            return true;
        }

        /** A shot at a target (left click, auto-retaliate) flies straight at it, lifted for the arrow's drop; else vanilla. */
        @Override
        protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index, float velocity, float inaccuracy,
                float angle, LivingEntity target) {
            if (target == null || !osrs(shooter)) {
                super.shootProjectile(shooter, projectile, index, velocity, inaccuracy, angle, target);
                return;
            }
            double dx = target.getX() - projectile.getX();
            double dz = target.getZ() - projectile.getZ();
            double dy = target.getY(0.5) - projectile.getY();
            double flat = Math.sqrt(dx * dx + dz * dz);
            double flightTicks = Math.ceil(flat / Math.max(0.1, velocity));
            double drop = 0.05 * flightTicks * (flightTicks + 1) / 2; // arrow gravity 0.05 per tick // approx: ignores drag
            // The click already chose the target: no extra spread (OSRS accuracy is the only miss roll).
            projectile.shoot(dx, dy + drop, dz, velocity, 0.0f);
        }
    }

    public static class Arrow extends ArrowItem implements OsrsItemLike {
        private final ItemDef def;

        public Arrow(ItemDef def, Properties settings) {
            super(settings);
            this.def = def;
        }

        @Override
        public ItemDef def() {
            return def;
        }

        @Override
        public AbstractArrow createArrow(Level world, ItemStack stack, LivingEntity shooter, ItemStack weapon) {
            net.minecraft.world.entity.projectile.Arrow arrow =
                    new net.minecraft.world.entity.projectile.Arrow(world, shooter, stack.copyWithCount(1), weapon);
            arrow.addTag(RANGED_STRENGTH_TAG + def.rangedStrength());
            arrow.addTag("runecraft_arrow_" + def.id());
            // OSRS arrows break or drop on hit (handled by the combat handler); never pick up vanilla arrows.
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            return arrow;
        }
    }

    /** Staffs cast the autocast spell on right-click. */
    public static class Staff extends Item implements OsrsItemLike {
        private final ItemDef def;

        public Staff(ItemDef def, Properties settings) {
            super(settings.attributes(staffAttributes(def)));
            this.def = def;
        }

        /** {@code // playability}: the staff's OSRS attack speed as its vanilla ATTACK_SPEED (a truthful indicator). */
        private static ItemAttributeModifiers staffAttributes(ItemDef def) {
            if (def == null || def.equipment() == null || def.equipment().attackSpeed() <= 0) {
                return ItemAttributeModifiers.EMPTY;
            }
            return ItemAttributeModifiers.builder().add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID,
                    attackSpeed(def, 0f), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND).build();
        }

        @Override
        public ItemDef def() {
            return def;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
            if (!world.isClientSide() && user instanceof ServerPlayer player) {
                ItemUses.staff(player);
            }
            return InteractionResultHolder.sidedSuccess(user.getItemInHand(hand), world.isClientSide());
        }
    }

    public static int rangedStrengthOf(ArrowTier tier) {
        return tier.rangedStrength();
    }
}
