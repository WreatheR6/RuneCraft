package com.runecraft.skill;

import com.runecraft.combat.PrayerService;
import com.runecraft.combat.Slayer;
import com.runecraft.menu.MenuService;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Station;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.world.Gielinor;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/** What happens when a station block is clicked. */
public final class Stations {
    private Stations() {
    }

    public static void use(ServerPlayerEntity player, Station station, BlockPos pos) {
        switch (station) {
            case BANK_BOOTH -> openBank(player);
            case FURNACE -> MenuService.open(player, MenuType.SMELTING, pos, false);
            case ANVIL -> MenuService.open(player, MenuType.SMITHING, pos, false);
            case RANGE -> MenuService.open(player, MenuType.COOKING, pos, true);
            case PRAYER_ALTAR -> PrayerService.useAltar(player);
            case GENERAL_STORE -> MenuService.open(player, MenuType.SHOP_GENERAL, pos, false);
            case RUNE_SHOP -> MenuService.open(player, MenuType.SHOP_RUNES, pos, false);
            case WEAPON_SHOP -> MenuService.open(player, MenuType.SHOP_WEAPONS, pos, false);
            case TOOL_SHOP -> MenuService.open(player, MenuType.SHOP_TOOLS, pos, false);
            case ARCHERY_SHOP -> MenuService.open(player, MenuType.SHOP_ARCHERY, pos, false);
            case SPINNING_WHEEL -> MenuService.open(player, MenuType.SPINNING, pos, false);
            case TANNER -> MenuService.open(player, MenuType.TANNING, pos, false);
            case WORKBENCH -> MenuService.open(player, MenuType.CONSTRUCTION, pos, false);
            case HERBLORE_TABLE -> MenuService.open(player, MenuType.HERBLORE, pos, false);
            case SLAYER_MASTER -> Slayer.talkToMaster(player);
            case PORTAL -> {
                if (Gielinor.is(player)) {
                    Gielinor.leave(player);
                } else {
                    Gielinor.teleport(player, Landmark.LUMBRIDGE);
                }
            }
            default -> {
            }
        }
    }

    public static void openBank(ServerPlayerEntity player) {
        PlayerData data = Players.data(player);
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> GenericContainerScreenHandler.createGeneric9x6(syncId, inv, data.bank()),
                Text.literal("The Bank of Gielinor")));
    }
}
