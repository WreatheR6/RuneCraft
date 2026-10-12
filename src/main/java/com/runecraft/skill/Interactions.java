package com.runecraft.skill;

import com.runecraft.block.StationBlock;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.menu.MenuService;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipes;
import com.runecraft.osrs.Station;
import com.runecraft.osrs.data.Monster;
import com.runecraft.osrs.data.Thieving;
import com.runecraft.registry.ModItems;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

/** Hooks into vanilla interactions: vanilla stations in Gielinor, selling to shops, and pickpocketing. */
public final class Interactions {
    private Interactions() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (hand != Hand.MAIN_HAND) {
                return ActionResult.PASS;
            }
            BlockState state = world.getBlockState(hit.getBlockPos());
            Block block = state.getBlock();
            // Sneak + right-click a shop counter while holding an item: sell it.
            if (player.isSneaking() && block instanceof StationBlock station && isShop(station.station())) {
                if (!world.isClient() && player instanceof ServerPlayerEntity sp) {
                    sell(sp);
                }
                return ActionResult.SUCCESS;
            }
            if (!Gielinor.is(world)) {
                return ActionResult.PASS;
            }
            MenuType menu = null;
            boolean range = false;
            ItemStack held = player.getMainHandStack();
            if (block == Blocks.CAMPFIRE || block == Blocks.SOUL_CAMPFIRE) {
                menu = MenuType.COOKING;
            } else if ((block == Blocks.SMOKER || block == Blocks.FURNACE) && isRaw(held)) {
                menu = MenuType.COOKING;
                range = true;
            } else if (block == Blocks.BLAST_FURNACE) {
                menu = MenuType.SMELTING;
            } else if ((block == Blocks.ANVIL || block == Blocks.CHIPPED_ANVIL || block == Blocks.DAMAGED_ANVIL) && isBar(held)) {
                menu = MenuType.SMITHING;
            } else if (block == Blocks.LOOM && !held.isEmpty()) {
                menu = MenuType.SPINNING;
            }
            if (menu == null) {
                return ActionResult.PASS;
            }
            if (!world.isClient() && player instanceof ServerPlayerEntity sp) {
                MenuService.open(sp, menu, hit.getBlockPos(), range);
            }
            return ActionResult.SUCCESS;
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (hand != Hand.MAIN_HAND || !(entity instanceof LivingEntity living)) {
                return ActionResult.PASS;
            }
            Thieving.Pickpocket kind = pickpocketTarget(living);
            if (kind == null || (entity instanceof VillagerEntity && !player.isSneaking())) {
                return ActionResult.PASS;
            }
            if (!world.isClient() && player instanceof ServerPlayerEntity sp) {
                ThievingService.pickpocket(sp, living, kind);
            }
            return ActionResult.SUCCESS;
        });
    }

    private static boolean isShop(Station s) {
        return s == Station.GENERAL_STORE || s == Station.RUNE_SHOP || s == Station.WEAPON_SHOP || s == Station.TOOL_SHOP
                || s == Station.ARCHERY_SHOP;
    }

    private static boolean isRaw(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        String id = Inv.idOf(stack);
        return Recipes.menu(MenuType.COOKING).stream().anyMatch(r -> r.inputs().containsKey(id));
    }

    private static boolean isBar(ItemStack stack) {
        return !stack.isEmpty() && Inv.idOf(stack).endsWith("_bar");
    }

    private static Thieving.Pickpocket pickpocketTarget(LivingEntity entity) {
        if (entity instanceof VillagerEntity) {
            return Thieving.Pickpocket.MAN;
        }
        if (entity instanceof OsrsMonsterEntity monster && monster.stats() != null && monster.stats().monster() != null) {
            Monster m = monster.stats().monster();
            return switch (m) {
                case MAN -> Thieving.Pickpocket.MAN;
                case AL_KHARID_WARRIOR -> Thieving.Pickpocket.AL_KHARID_WARRIOR;
                case GUARD -> Thieving.Pickpocket.GUARD;
                default -> null;
            };
        }
        return null;
    }

    /** General stores buy items at 40% of their value. */
    private static void sell(ServerPlayerEntity player) {
        ItemStack held = player.getMainHandStack();
        // Bank notes sell as the item they stand for.
        ItemDef def = com.runecraft.bank.Notes.isNote(held)
                ? com.runecraft.osrs.ItemCatalog.get(com.runecraft.bank.Notes.notedId(held)) : ModItems.def(held);
        if (def == null || def.id().equals("coins")) {
            Msg.game(player, "Hold the item you want to sell and sneak + right-click the shop.");
            return;
        }
        int each = (int) Math.floor(def.value() * 0.4);
        int count = held.getCount();
        held.setCount(0);
        Inv.give(player, "coins", each * count);
        Msg.game(player, "You sell " + count + " x " + def.name() + " for " + (each * count) + " coins.");
        ServerNetworking.sync(player);
    }

    static String id(Block block) {
        return Registries.BLOCK.getId(block).toString();
    }
}
