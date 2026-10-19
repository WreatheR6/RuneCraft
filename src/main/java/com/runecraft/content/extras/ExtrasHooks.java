package com.runecraft.content.extras;

import com.runecraft.content.ModuleHooks;
import com.runecraft.osrs.content.extras.Poh;
import com.runecraft.util.Inv;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.TypedActionResult;

/**
 * Minecraft side of the extras module: pets (followers, skilling pet rolls, pet house), the
 * player-owned house (portals, rooms, hotspots, furniture effects) and playable Sailing (boats,
 * charter docks, voyages, shipwrights).
 */
public final class ExtrasHooks implements ModuleHooks {
    @Override
    public void init() {
        ExtrasEntities.register();
        PetService.init();
        HouseService.init();
        HouseFurniture.init();
        HouseServants.init();
        SailingService.init();
        ExtrasCommands.register();
        com.runecraft.content.systems.SysTeleport.houseTeleport = HouseService::teleportHome;
        com.runecraft.content.islands.IslandsTravelService.houseFairyRing =
                p -> HouseService.layout(p.getServer(), p.getUuid()).count(Poh.Effect.FAIRY_RING) > 0;
        com.runecraft.content.islands.IslandsTravelService.houseRingTeleport = HouseService::teleportToFairyRing;
        // Teleport to house tablet.
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (stack.isEmpty() || !Poh.HOUSE_TABLET.equals(Inv.idOf(stack))) {
                return TypedActionResult.pass(stack);
            }
            if (!world.isClient() && player instanceof ServerPlayerEntity sp) {
                if (com.runecraft.world.Gielinor.wildernessLevel(sp) > 20) {
                    com.runecraft.util.Msg.game(sp, "A mysterious force prevents you from teleporting.");
                } else {
                    stack.decrement(1);
                    HouseService.teleportHome(sp);
                }
            }
            return TypedActionResult.success(stack);
        });
    }
}
