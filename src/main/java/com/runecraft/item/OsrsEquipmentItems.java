package com.runecraft.item;

import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.data.AmmoRules;
import com.runecraft.osrs.data.ArrowTier;
import com.runecraft.skill.ItemUses;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
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
import net.minecraft.world.level.Level;

/** Vanilla-backed equipment classes that also carry their OSRS {@link ItemDef}. */
public final class OsrsEquipmentItems {
    /** Command tag prefix carrying an arrow's ranged strength to the combat handler. */
    public static final String RANGED_STRENGTH_TAG = "runecraft_rstr_";

    private OsrsEquipmentItems() {
    }

    public static class Axe extends AxeItem implements OsrsItemLike {
        private final ItemDef def;

        public Axe(ItemDef def, Tier material, Properties settings) {
            super(material, 5.0f, -3.0f, settings);
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
            super(material, 1, -2.8f, settings);
            this.def = def;
        }

        @Override
        public ItemDef def() {
            return def;
        }
    }

    public static class Weapon extends SwordItem implements OsrsItemLike {
        private final ItemDef def;

        public Weapon(ItemDef def, Tier material, int attackDamage, float attackSpeed, Properties settings) {
            super(material, attackDamage, attackSpeed, settings);
            this.def = def;
        }

        @Override
        public ItemDef def() {
            return def;
        }
    }

    public static class Armour extends ArmorItem implements OsrsItemLike {
        private final ItemDef def;

        public Armour(ItemDef def, ArmorMaterial material, ArmorItem.Type type, Properties settings) {
            super(material, type, settings);
            this.def = def;
        }

        @Override
        public ItemDef def() {
            return def;
        }
    }

    public static class Bow extends BowItem implements OsrsItemLike {
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
        public AbstractArrow createArrow(Level world, ItemStack stack, LivingEntity shooter) {
            net.minecraft.world.entity.projectile.Arrow arrow = new net.minecraft.world.entity.projectile.Arrow(world, shooter);
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
            super(settings);
            this.def = def;
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
