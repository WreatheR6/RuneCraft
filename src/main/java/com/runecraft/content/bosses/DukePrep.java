package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.bosses.DukeRules;
import com.runecraft.player.Players;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;

/**
 * Waking Duke Sucellus ({@link DukeRules}): the asylum's mushrooms, salt piles and fermentation vats, grinding mushrooms with
 * a pestle and mortar, and feeding him poison or powder (using it on him) until his tolerance is filled.
 */
final class DukePrep {
    private DukePrep() {
    }

    static void register() {
        com.runecraft.content.BlockHooks.register(DukeRules.ARDER_MUSHROOMS, (p, pos, state) -> pick(p, DukeRules.ARDER_MUSHROOM));
        com.runecraft.content.BlockHooks.register(DukeRules.MUSCA_MUSHROOMS, (p, pos, state) -> pick(p, DukeRules.MUSCA_MUSHROOM));
        com.runecraft.content.BlockHooks.register(DukeRules.SALT_PILE, (p, pos, state) -> mineSalt(p));
        com.runecraft.content.BlockHooks.register(DukeRules.VAT, (p, pos, state) -> ferment(p));
        com.runecraft.platform.UseItemCallback.EVENT.register((player, world, hand) -> {
            net.minecraft.world.item.ItemStack stack = player.getItemInHand(hand);
            String id = Inv.idOf(stack);
            if (world.isClientSide() || !(player instanceof ServerPlayer p)
                    || !(DukeRules.ARDER_MUSHROOM.equals(id) || DukeRules.MUSCA_MUSHROOM.equals(id))) {
                return InteractionResultHolder.pass(stack);
            }
            grind(p, id);
            return InteractionResultHolder.sidedSuccess(stack, false);
        });
        com.runecraft.platform.UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            // (both interaction packets fire this: only the plain one, without a hit result, feeds him)
            if (world.isClientSide() || hit != null || hand != net.minecraft.world.InteractionHand.MAIN_HAND
                    || !(player instanceof ServerPlayer p) || !(entity instanceof DataBossEntity boss)
                    || !(boss.script() instanceof EncounterScripts.Duke duke) || !duke.asleep()) {
                return InteractionResult.PASS;
            }
            String held = Inv.idOf(p.getItemInHand(hand));
            int value = DukeRules.POISON.equals(held) ? DukeRules.POISON_VALUE
                    : DukeRules.ARDER_POWDER.equals(held) || DukeRules.MUSCA_POWDER.equals(held) ? DukeRules.POWDER_VALUE : 0;
            if (value == 0) {
                Msg.game(p, "Duke Sucellus is fast asleep. Arder-musca poison might weaken him enough to wake him.");
                return InteractionResult.SUCCESS;
            }
            Inv.remove(p, held, 1);
            duke.feed(boss, p, value);
            return InteractionResult.SUCCESS;
        });
    }

    /** Picks a mushroom (they grow back). */
    static void pick(ServerPlayer p, String mushroom) {
        Inv.give(p, mushroom, 1);
        Msg.game(p, "You pick " + (mushroom.startsWith("arder") ? "an arder" : "a musca") + " mushroom.");
    }

    /** Mines salax salt from a pile (a pickaxe needed). */
    static void mineSalt(ServerPlayer p) {
        var skills = Players.data(p).skills();
        if (com.runecraft.skill.Tools.bestPickaxe(p, skills) == null) {
            Msg.game(p, "You need a pickaxe to mine the salt.");
            return;
        }
        int n = DukeRules.saltPerHarvest(skills.level(Skill.MINING));
        Inv.give(p, DukeRules.SALAX_SALT, n);
        XpService.add(p, Skill.MINING, 2.0 * n); // approx
        Msg.game(p, "You mine " + n + " salax salt.");
    }

    /** Grinds every mushroom of one kind into powder (a pestle and mortar needed); returns the powder made. */
    static int grind(ServerPlayer p, String mushroom) {
        if (!Inv.has(p, "pestle_and_mortar", 1)) {
            Msg.game(p, "You need a pestle and mortar to grind the mushroom.");
            return 0;
        }
        int n = Inv.count(p, mushroom);
        if (n <= 0) {
            return 0;
        }
        String powder = DukeRules.ARDER_MUSHROOM.equals(mushroom) ? DukeRules.ARDER_POWDER : DukeRules.MUSCA_POWDER;
        int made = n * DukeRules.powderPerMushroom(Players.data(p).skills().level(Skill.HERBLORE));
        Inv.remove(p, mushroom, n);
        Inv.give(p, powder, made);
        Msg.game(p, "You grind the mushrooms into " + made + " powder.");
        return made;
    }

    /** A fermentation vat: six salt and six of each powder make an arder-musca poison; returns how many were made. */
    static int ferment(ServerPlayer p) {
        int n = DukeRules.poisons(Inv.count(p, DukeRules.SALAX_SALT), Inv.count(p, DukeRules.ARDER_POWDER),
                Inv.count(p, DukeRules.MUSCA_POWDER));
        if (n <= 0) {
            Msg.game(p, "The vat needs " + DukeRules.PER_POISON + " salax salt, arder powder and musca powder for a poison.");
            return 0;
        }
        int take = n * DukeRules.PER_POISON;
        Inv.remove(p, DukeRules.SALAX_SALT, take);
        Inv.remove(p, DukeRules.ARDER_POWDER, take);
        Inv.remove(p, DukeRules.MUSCA_POWDER, take);
        Inv.give(p, DukeRules.POISON, n);
        Msg.game(p, "The mixture ferments into " + n + " arder-musca poison" + (n > 1 ? "s." : "."));
        return n;
    }
}
