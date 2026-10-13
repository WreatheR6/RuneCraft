package com.runecraft.content.skilling;

import com.runecraft.content.BlockHooks;
import com.runecraft.content.GameEvents;
import com.runecraft.menu.MenuService;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.content.skilling.BrewingData;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

/**
 * Brewing's fermenting vats: each player has their own vat at each brewery (kept in the skilling world
 * save, section "vats"). Brewing at an empty vat puts the brew in it (the BREWING menu takes the
 * ingredients and gives the Cooking XP); it ferments for three 640-minute growth cycles of world time and
 * is then drained into eight beer glasses, matured, regular or bad (see {@link BrewingData#outcome}).
 */
final class BrewingHooks {
    private static final String VAT = "fermenting_vat";
    private static final long TICKS_PER_MINUTE = 1200;

    private BrewingHooks() {
    }

    static void init() {
        BlockHooks.register(VAT, (player, pos, state) -> vat(player, pos));
        MenuService.REQUIREMENTS.add((player, recipe) -> {
            if (BrewingData.brewByRecipe(recipe.id()) == null) {
                return null;
            }
            BlockPos station = Players.data(player).menuStation;
            return station != null && vats(player).contains(key(player, station)) ? "The vat is already full." : null;
        });
        GameEvents.onMake(BrewingHooks::started);
    }

    private static NbtCompound vats(ServerPlayerEntity player) {
        return SkillingState.get(player.getServer()).section("vats");
    }

    private static String key(ServerPlayerEntity player, BlockPos pos) {
        return player.getUuidAsString() + "|" + SkillingUtil.key(player.getWorld(), pos);
    }

    private static long cycleTicks() {
        return BrewingData.CYCLE_MINUTES * TICKS_PER_MINUTE;
    }

    /** The yeast went in: the brew starts fermenting in this player's vat. */
    private static void started(ServerPlayerEntity player, Recipe recipe) {
        BrewingData.Brew brew = BrewingData.brewByRecipe(recipe.id());
        BlockPos station = Players.data(player).menuStation;
        if (brew == null || station == null) {
            return;
        }
        // "The stuff" (Trouble Brewing) goes in with the malt; it can't be added to cider.
        boolean stuff = brew.malt() && Inv.has(player, BrewingData.THE_STUFF, 1);
        if (stuff) {
            Inv.remove(player, BrewingData.THE_STUFF, 1);
        }
        NbtCompound v = new NbtCompound();
        v.putString("brew", brew.id());
        v.putLong("start", player.getWorld().getTime());
        v.putInt("outcome", BrewingData.outcome(stuff, player.getRandom().nextDouble()).ordinal());
        vats(player).put(key(player, station), v);
        Msg.game(player, "You add the yeast" + (stuff ? " and some of \"the stuff\"" : "") + ". The " + SkillingUtil.lower(brew.name())
                + " will take about a day to ferment.");
    }

    private static void vat(ServerPlayerEntity player, BlockPos pos) {
        NbtCompound all = vats(player);
        String key = key(player, pos);
        if (!all.contains(key)) {
            MenuService.open(player, MenuType.BREWING, pos, false);
            return;
        }
        NbtCompound v = all.getCompound(key);
        BrewingData.Brew brew = null;
        for (BrewingData.Brew b : BrewingData.BREWS) {
            if (b.id().equals(v.getString("brew"))) {
                brew = b;
            }
        }
        if (brew == null) {
            all.remove(key);
            return;
        }
        long left = BrewingData.readyAt(v.getLong("start"), cycleTicks()) - player.getWorld().getTime();
        if (left > 0) {
            long minutes = (left + TICKS_PER_MINUTE - 1) / TICKS_PER_MINUTE;
            Msg.game(player, "The " + SkillingUtil.lower(brew.name()) + " is still fermenting. It should be ready in about "
                    + minutes / 60 + "h " + minutes % 60 + "m.");
            return;
        }
        int o = v.getInt("outcome");
        if (o == BrewingData.Outcome.BAD.ordinal()) {
            all.remove(key);
            Msg.game(player, "The " + SkillingUtil.lower(brew.name()) + " has gone bad. You turn the valve and drain the vat.");
            return;
        }
        if (!Inv.has(player, "beer_glass", BrewingData.PINTS)) {
            Msg.game(player, "The vat is full of " + (o == BrewingData.Outcome.MATURE.ordinal() ? SkillingUtil.lower(brew.matureName())
                    : SkillingUtil.lower(brew.name())) + ". You need " + BrewingData.PINTS + " beer glasses to collect it.");
            return;
        }
        Inv.remove(player, "beer_glass", BrewingData.PINTS);
        boolean mature = o == BrewingData.Outcome.MATURE.ordinal();
        SkillingUtil.give(player, mature ? brew.mature() : brew.id(), BrewingData.PINTS);
        all.remove(key);
        Msg.game(player, mature ? "The brew has matured nicely! You fill your glasses with " + brew.matureName() + "."
                : "You turn the valve and fill your glasses with " + brew.name() + ".");
    }
}
