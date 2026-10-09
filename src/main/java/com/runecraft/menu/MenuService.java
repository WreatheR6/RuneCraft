package com.runecraft.menu;

import com.runecraft.action.Action;
import com.runecraft.action.ActionManager;
import com.runecraft.collection.CollectionLog;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Recipes;
import com.runecraft.osrs.SkillSet;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.skill.Stun;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** OSRS "Make-X" interfaces for production skills and shops. */
public final class MenuService {
    private static final double STATION_REACH_SQ = 6.0 * 6.0;

    private MenuService() {
    }

    /** Opens a menu. {@code station} may be null for tool-based menus (knife, chisel...). */
    public static void open(ServerPlayerEntity player, MenuType type, BlockPos station, boolean range) {
        if (!Stun.check(player)) {
            return;
        }
        ActionManager.cancel(player);
        PlayerData data = Players.data(player);
        List<ServerNetworking.MenuOption> options = new ArrayList<>();
        for (Recipe r : Recipes.menu(type)) {
            boolean relevant = type.isShop() || type == MenuType.CONSTRUCTION
                    || r.inputs().keySet().stream().filter(k -> !k.equals(Inv.COINS)).allMatch(k -> Inv.has(player, k, 1));
            if (relevant) {
                options.add(new ServerNetworking.MenuOption(r, canMake(player, data.skills(), r, 1) == null));
            }
        }
        if (options.isEmpty()) {
            Msg.game(player, emptyMessage(type));
            return;
        }
        data.openMenu = type;
        data.menuStation = station;
        data.menuRange = range;
        ServerNetworking.openMenu(player, type, options);
    }

    private static String emptyMessage(MenuType type) {
        return switch (type) {
            case COOKING -> "You have nothing to cook.";
            case SMELTING -> "You don't have any ores to smelt.";
            case SMITHING -> "You don't have any bars to smith.";
            case FLETCHING -> "You have nothing to fletch.";
            case GEM_CUTTING -> "You have no uncut gems to cut.";
            case LEATHERWORKING -> "You need leather and thread (string) to craft with.";
            case SPINNING -> "You have nothing to spin.";
            case TANNING -> "You have no hides to tan.";
            case HERBLORE -> "You have nothing to mix.";
            default -> "Nothing interesting happens.";
        };
    }

    /** Returns why the recipe can't be made (shown to the player), or null if it can. */
    private static String canMake(ServerPlayerEntity player, SkillSet skills, Recipe r, int times) {
        if (r.skill() != null && skills.currentLevel(r.skill()) < r.level()) {
            return "You need " + Msg.article(r.skill().displayName()) + " level of " + r.level()
                    + " to make that.";
        }
        if (r.requiredTool() != null && !Inv.has(player, r.requiredTool(), 1)) {
            ItemDef tool = ItemCatalog.get(r.requiredTool());
            return "You need " + Msg.article(tool == null ? r.requiredTool() : tool.name().toLowerCase(Locale.ROOT)) + " to do that.";
        }
        for (Map.Entry<String, Integer> e : r.inputs().entrySet()) {
            if (!Inv.has(player, e.getKey(), e.getValue() * times)) {
                if (e.getKey().equals(Inv.COINS)) {
                    return "You don't have enough coins.";
                }
                return "You don't have the materials to make that.";
            }
        }
        return null;
    }

    public static void choose(ServerPlayerEntity player, String recipeId, int quantity) {
        PlayerData data = Players.data(player);
        Recipe recipe = Recipes.get(recipeId);
        if (recipe == null || recipe.menu() != data.openMenu) {
            return;
        }
        if (data.menuStation != null && player.squaredDistanceTo(data.menuStation.toCenterPos()) > STATION_REACH_SQ) {
            Msg.game(player, "You're too far away.");
            return;
        }
        String why = canMake(player, data.skills(), recipe, 1);
        if (why != null) {
            Msg.game(player, why);
            return;
        }
        int qty = Math.max(1, Math.min(quantity, 10_000));
        if (recipe.ticks() <= 0) {
            // Shops, tanners and sawmills are instant.
            int made = 0;
            while (made < qty && canMake(player, data.skills(), recipe, 1) == null) {
                makeOnce(player, recipe, data.menuRange);
                made++;
            }
            if (made < qty && made > 0) {
                Msg.game(player, recipe.menu().isShop() ? "You don't have enough coins." : "You have run out of materials.");
            }
            return;
        }
        boolean range = data.menuRange;
        ActionManager.start(player, new Action() {
            int remaining = qty;
            int counter = recipe.ticks() - 1;

            @Override
            public boolean tick(ServerPlayerEntity p, long gameTick) {
                if (++counter < recipe.ticks()) {
                    return true;
                }
                counter = 0;
                String reason = canMake(p, Players.data(p).skills(), recipe, 1);
                if (reason != null) {
                    if (remaining != qty) {
                        Msg.game(p, "You have run out of materials.");
                    } else {
                        Msg.game(p, reason);
                    }
                    return false;
                }
                p.swingHand(Hand.MAIN_HAND, true);
                makeOnce(p, recipe, range);
                return --remaining > 0;
            }
        });
    }

    private static void makeOnce(ServerPlayerEntity player, Recipe r, boolean range) {
        SkillSet skills = Players.data(player).skills();
        r.inputs().forEach((item, count) -> Inv.remove(player, item, count));
        double chance = r.successChance();
        if (r.stopBurn() > 0) {
            chance = r.cookingSuccess(skills.currentLevel(r.skill()), range);
        }
        boolean success = player.getRandom().nextDouble() < chance;
        if (success) {
            r.outputs().forEach((item, count) -> {
                Inv.give(player, item, count);
                CollectionLog.obtain(player, item);
            });
            if (r.skill() != null) {
                XpService.addTenths(player, r.skill(), r.xpTenths());
            }
            Msg.action(player, successMessage(r));
            sound(player, r);
        } else {
            if (r.failItem() != null) {
                Inv.give(player, r.failItem(), 1);
                Msg.action(player, "You accidentally burn the " + r.name().toLowerCase(Locale.ROOT) + ".");
            } else {
                Msg.action(player, "The ore is too impure and you fail to refine it.");
            }
        }
        if (r.menu().isShop()) {
            ServerNetworking.sync(player);
        }
    }

    private static String successMessage(Recipe r) {
        String n = r.name().toLowerCase(Locale.ROOT);
        return switch (r.menu()) {
            case COOKING -> "You successfully cook " + (n.endsWith("s") ? "some " : "a ") + n + ".";
            case SMELTING -> "You retrieve " + Msg.article(n) + ".";
            case SMITHING -> "You hammer the metal and make " + (Character.isDigit(n.charAt(0)) ? n : Msg.article(n)) + ".";
            case FLETCHING -> "You carefully make " + (Character.isDigit(n.charAt(0)) ? n : Msg.article(n)) + ".";
            case GEM_CUTTING -> "You cut the " + n + ".";
            case HERBLORE -> "You mix the ingredients to make " + Msg.article(n) + ".";
            case CONSTRUCTION -> r.skill() == null ? "The sawmill operator converts your logs." : "You build " + Msg.article(n) + ".";
            default -> r.menu().isShop() ? "You buy " + n.replaceAll(" - .*", "") + "." : "You make " + Msg.article(n) + ".";
        };
    }

    private static void sound(ServerPlayerEntity player, Recipe r) {
        SoundEvent sound = switch (r.menu()) {
            case SMITHING -> SoundEvents.BLOCK_ANVIL_USE;
            case SMELTING -> SoundEvents.BLOCK_FURNACE_FIRE_CRACKLE;
            case COOKING -> SoundEvents.BLOCK_FIRE_EXTINGUISH;
            case HERBLORE -> SoundEvents.ITEM_BOTTLE_FILL;
            case FLETCHING, GEM_CUTTING, CONSTRUCTION -> SoundEvents.UI_STONECUTTER_TAKE_RESULT;
            default -> SoundEvents.ENTITY_ITEM_PICKUP;
        };
        player.getWorld().playSound(null, player.getBlockPos(), sound, SoundCategory.PLAYERS, 0.4f, 1.0f);
    }
}
