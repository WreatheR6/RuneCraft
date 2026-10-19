package com.runecraft.content.systems;

import com.runecraft.combat.PrayerService;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Skill;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;

/**
 * Prayer scrolls: reading a dexterous / arcane / torn / Deadeye / Mystic Vigour prayer scroll consumes it and teaches
 * Rigour, Augury, Preserve, Deadeye or Mystic Vigour for good (OSRS). The prayers can't be used before.
 */
final class PrayerScrolls {
    private PrayerScrolls() {
    }

    static void register() {
        PrayerService.scrollRead = (p, prayer) -> SysState.of(p).prayerScrolls.contains(prayer.name());
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            Prayer prayer = stack.isEmpty() ? null : Prayer.taughtBy(Inv.idOf(stack));
            if (prayer == null) {
                return InteractionResultHolder.pass(stack);
            }
            if (!world.isClientSide() && player instanceof ServerPlayer p) {
                read(p, stack, prayer);
            }
            return InteractionResultHolder.success(stack);
        });
    }

    private static void read(ServerPlayer p, ItemStack stack, Prayer prayer) {
        SysState.SysPlayer sp = SysState.of(p);
        if (sp.prayerScrolls.contains(prayer.name())) {
            Msg.game(p, "You have already learnt " + prayer.displayName() + ".");
            return;
        }
        stack.shrink(1);
        sp.prayerScrolls.add(prayer.name());
        p.level().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1f);
        Msg.action(p, "You study the scroll and learn a new prayer: " + prayer.displayName() + "!");
        if (Players.data(p).skills().level(Skill.PRAYER) < prayer.level()) {
            Msg.game(p, "You will be able to use it at Prayer level " + prayer.level() + ".");
        }
    }
}
