package com.runecraft.item;

import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.data.ArrowTier;
import com.runecraft.skill.ItemUses;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** Vanilla-backed equipment classes that also carry their OSRS {@link ItemDef}. */
public final class OsrsEquipmentItems {
    /** Command tag prefix carrying an arrow's ranged strength to the combat handler. */
    public static final String RANGED_STRENGTH_TAG = "runecraft_rstr_";

    private OsrsEquipmentItems() {
    }

    public static class Axe extends AxeItem implements OsrsItemLike {
        private final ItemDef def;

        public Axe(ItemDef def, ToolMaterial material, Settings settings) {
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

        public Pickaxe(ItemDef def, ToolMaterial material, Settings settings) {
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

        public Weapon(ItemDef def, ToolMaterial material, int attackDamage, float attackSpeed, Settings settings) {
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

        public Armour(ItemDef def, ArmorMaterial material, ArmorItem.Type type, Settings settings) {
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

        public Bow(ItemDef def, Settings settings) {
            super(settings);
            this.def = def;
        }

        @Override
        public ItemDef def() {
            return def;
        }
    }

    public static class Arrow extends ArrowItem implements OsrsItemLike {
        private final ItemDef def;

        public Arrow(ItemDef def, Settings settings) {
            super(settings);
            this.def = def;
        }

        @Override
        public ItemDef def() {
            return def;
        }

        @Override
        public PersistentProjectileEntity createArrow(World world, ItemStack stack, LivingEntity shooter) {
            ArrowEntity arrow = new ArrowEntity(world, shooter);
            arrow.addCommandTag(RANGED_STRENGTH_TAG + def.rangedStrength());
            arrow.addCommandTag("runecraft_arrow_" + def.id());
            // OSRS arrows break or drop on hit (handled by the combat handler); never pick up vanilla arrows.
            arrow.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
            return arrow;
        }
    }

    /** Staffs cast the autocast spell on right-click. */
    public static class Staff extends Item implements OsrsItemLike {
        private final ItemDef def;

        public Staff(ItemDef def, Settings settings) {
            super(settings);
            this.def = def;
        }

        @Override
        public ItemDef def() {
            return def;
        }

        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            if (!world.isClient() && user instanceof ServerPlayerEntity player) {
                ItemUses.staff(player);
            }
            return TypedActionResult.success(user.getStackInHand(hand), world.isClient());
        }
    }

    public static int rangedStrengthOf(ArrowTier tier) {
        return tier.rangedStrength();
    }
}
