package com.runecraft.content.skilling;

import com.runecraft.action.Action;
import com.runecraft.action.ActionManager;
import com.runecraft.block.RuneAltarBlock;
import com.runecraft.content.BlockHooks;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.skilling.CraftingData;
import com.runecraft.osrs.content.skilling.RunecraftData;
import com.runecraft.osrs.data.RuneAltar;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Runecraft extras at the built-in altars (tiaras, combination runes, emptying essence pouches),
 * the Ourania altar, elemental orb charging at obelisks, and wine fermentation.
 */
final class RunecraftHooks {
    private RunecraftHooks() {
    }

    static void init() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (hand != InteractionHand.MAIN_HAND || world.isClientSide() || !(player instanceof ServerPlayer sp)) {
                return InteractionResult.PASS;
            }
            BlockState state = world.getBlockState(hit.getBlockPos());
            if (!(state.getBlock() instanceof RuneAltarBlock altarBlock)) {
                return InteractionResult.PASS;
            }
            ItemHooks.emptyPouches(sp);
            return altar(sp, altarBlock.altar(), hit.getBlockPos()) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        });
        BlockHooks.register("ourania_altar", (player, pos, state) -> ourania(player, pos));
        for (CraftingData.Orb orb : CraftingData.ORBS) {
            BlockHooks.register(orb.obelisk(), (player, pos, state) -> chargeOrbs(player, pos, orb));
        }
    }

    /** Tiara binding and combination runes; returns true if handled (otherwise normal crafting runs). */
    private static boolean altar(ServerPlayer player, RuneAltar altar, BlockPos pos) {
        String held = SkillingUtil.heldId(player);
        if (held.equals("tiara")) {
            String talisman = RunecraftData.talisman(altar);
            if (!SkillingUtil.needItem(player, talisman, "You need " + Msg.article(SkillingUtil.lower(SkillingUtil.name(talisman)))
                    + " to bind the tiara.")) {
                return true;
            }
            if (!SkillingUtil.need(player, Skill.RUNECRAFT, altar.level(), "bind this tiara")) {
                return true;
            }
            Inv.remove(player, "tiara", 1);
            Inv.remove(player, talisman, 1);
            SkillingUtil.give(player, RunecraftData.tiara(altar), 1);
            XpService.add(player, Skill.RUNECRAFT, RunecraftData.tiaraXp(altar));
            Msg.action(player, "You bind the power of the talisman into your tiara.");
            effects(player, pos);
            return true;
        }
        // Aether runes: soul runes held at the Cosmic Altar with Magic Imbue (the soul talisman isn't obtainable).
        if (altar == RuneAltar.COSMIC && held.equals("soul_rune") && com.runecraft.skill.Runecrafting.imbued.test(player)) {
            aether(player, pos);
            return true;
        }
        for (RunecraftData.Combination c : RunecraftData.COMBINATIONS) {
            if (c.altar() == altar && c.talisman().equals(held)) {
                combination(player, c, pos);
                return true;
            }
            // Magic Imbue: hold the other element's rune instead of its talisman.
            if (c.altar() == altar && c.rune().equals(held) && com.runecraft.skill.Runecrafting.imbued.test(player)) {
                combination(player, c, pos);
                return true;
            }
        }
        return false;
    }

    private static void combination(ServerPlayer player, RunecraftData.Combination c, BlockPos pos) {
        if (!SkillingUtil.need(player, Skill.RUNECRAFT, c.level(), "craft " + SkillingUtil.lower(c.name()) + "s")) {
            return;
        }
        int essence = Inv.count(player, "pure_essence");
        int runes = Inv.count(player, c.rune());
        int n = Math.min(essence, runes);
        if (n <= 0) {
            Msg.game(player, "You need pure essence and " + SkillingUtil.lower(SkillingUtil.name(c.rune())) + "s to make "
                    + SkillingUtil.lower(c.name()) + "s.");
            return;
        }
        Inv.remove(player, "pure_essence", n);
        Inv.remove(player, c.rune(), n);
        if (!com.runecraft.skill.Runecrafting.imbued.test(player)) {
            Inv.remove(player, c.talisman(), 1);
        }
        // A worn binding necklace guarantees success (one of its 16 charges per trip); otherwise 50% per rune.
        boolean binding = useBindingCharge(player);
        RandomSource random = player.getRandom();
        int made = 0;
        for (int i = 0; i < n; i++) {
            if (binding || random.nextBoolean()) {
                made++;
            }
        }
        SkillingUtil.give(player, c.id(), made);
        XpService.add(player, Skill.RUNECRAFT, c.xp() * made);   // no XP for the essence lost without a binding necklace
        Msg.game(player, "You bind the temple's power into " + made + " " + SkillingUtil.lower(c.name()) + "s.");
        effects(player, pos);
    }

    /** Uses a charge of a worn binding necklace (16 per player, OSRS Wiki); returns false if none is worn. */
    private static boolean useBindingCharge(ServerPlayer player) {
        return com.runecraft.content.systems.JewelleryService.bindingCharge(player);
    }

    /**
     * Aether runes (OSRS Wiki): 90 Runecraft; one essence, soul rune and aether catalyst per rune, 50% each without a binding
     * necklace. A failed rune loses its essence and soul rune but keeps its catalyst. Daeyalt essence pays 30 XP a rune.
     */
    private static void aether(ServerPlayer player, BlockPos pos) {
        if (!SkillingUtil.need(player, Skill.RUNECRAFT, RunecraftData.AETHER_LEVEL, "craft aether runes")) {
            return;
        }
        boolean daeyalt = Inv.count(player, "daeyalt_essence") > 0;
        String essenceId = daeyalt ? "daeyalt_essence" : "pure_essence";
        int n = Math.min(Inv.count(player, essenceId), Math.min(Inv.count(player, "soul_rune"),
                Inv.count(player, RunecraftData.AETHER_CATALYST)));
        if (n <= 0) {
            Msg.game(player, "You need pure essence, soul runes and aether catalysts to make aether runes.");
            return;
        }
        Inv.remove(player, essenceId, n);
        Inv.remove(player, "soul_rune", n);
        boolean binding = useBindingCharge(player);
        RandomSource random = player.getRandom();
        int made = 0;
        for (int i = 0; i < n; i++) {
            if (binding || random.nextBoolean()) {
                made++;
            }
        }
        Inv.remove(player, RunecraftData.AETHER_CATALYST, made);
        SkillingUtil.give(player, RunecraftData.AETHER_RUNE, made);
        XpService.add(player, Skill.RUNECRAFT, RunecraftData.essenceXp(RunecraftData.AETHER_XP, made, daeyalt));
        Msg.game(player, "You bind the temple's power into " + made + " aether rune" + (made == 1 ? "." : "s."));
        effects(player, pos);
    }

    private static void ourania(ServerPlayer player, BlockPos pos) {
        int essence = Inv.count(player, "pure_essence");
        ItemHooks.emptyPouches(player);
        essence = Inv.count(player, "pure_essence");
        if (essence == 0) {
            Msg.game(player, "You need pure essence to craft runes at the Ourania altar.");
            return;
        }
        int level = SkillingUtil.level(player, Skill.RUNECRAFT);
        List<RuneAltar> unlocked = new ArrayList<>();
        for (RuneAltar a : RuneAltar.values()) {
            if (a.level() <= level && a != RuneAltar.WRATH) {
                unlocked.add(a);
            }
        }
        Inv.remove(player, "pure_essence", essence);
        RandomSource random = player.getRandom();
        double xp = 0;
        for (int i = 0; i < essence; i++) {
            // Favour higher runes as the player levels: pick the better of two rolls half the time.
            RuneAltar a = unlocked.get(random.nextInt(unlocked.size()));
            if (random.nextBoolean()) {
                RuneAltar b = unlocked.get(random.nextInt(unlocked.size()));
                if (b.level() > a.level()) {
                    a = b;
                }
            }
            SkillingUtil.give(player, a.rune().itemId(), 1);
            xp += a.xp() * RunecraftData.OURANIA_MULTIPLIER;
        }
        XpService.add(player, Skill.RUNECRAFT, xp);
        Msg.game(player, "You bind the essence into a random assortment of runes.");
        effects(player, pos);
    }

    private static void effects(ServerPlayer player, BlockPos pos) {
        ServerLevel world = (ServerLevel) player.level();
        world.sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.1);
        world.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private static boolean canCharge(ServerPlayer player, CraftingData.Orb orb) {
        return Inv.has(player, "unpowered_orb", 1) && Inv.has(player, "cosmic_rune", 3) && Inv.has(player, orb.rune(), 30);
    }

    private static void chargeOrbs(ServerPlayer player, BlockPos pos, CraftingData.Orb orb) {
        if (!SkillingUtil.need(player, Skill.MAGIC, orb.magicLevel(), "charge " + orb.element() + " orbs")) {
            return;
        }
        if (!canCharge(player, orb)) {
            Msg.game(player, "You need an unpowered orb, 3 cosmic runes and 30 " + orb.element() + " runes to charge an orb.");
            return;
        }
        ActionManager.start(player, new Action() {
            int counter = 4;

            @Override
            public boolean tick(ServerPlayer p, long gameTick) {
                if (++counter < 5) {
                    return true;
                }
                counter = 0;
                if (!canCharge(p, orb)) {
                    return false;
                }
                Inv.remove(p, "unpowered_orb", 1);
                Inv.remove(p, "cosmic_rune", 3);
                Inv.remove(p, orb.rune(), 30);
                SkillingUtil.give(p, orb.orb(), 1);
                XpService.add(p, Skill.MAGIC, orb.magicXp());
                p.swing(InteractionHand.MAIN_HAND, true);
                Msg.action(p, "You charge the orb with the power of " + orb.element() + ".");
                ((ServerLevel) p.level()).sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.5,
                        pos.getZ() + 0.5, 20, 0.3, 0.5, 0.3, 0.2);
                return true;
            }
        });
    }

    // ------------------------------------------------------------------ wine

    /** Unfermented wine turns into wine after ~12 seconds (OSRS: 200 Cooking XP each, bad wine below 68). */
    static void fermentWine(ServerPlayer player) {
        int jugs = Inv.count(player, "unfermented_wine");
        if (jugs <= 0) {
            return;
        }
        Inv.remove(player, "unfermented_wine", jugs);
        int level = SkillingUtil.level(player, Skill.COOKING);
        int good = 0;
        for (int i = 0; i < jugs; i++) {
            double chance = level >= 68 ? 1.0 : 0.45 + 0.55 * Math.max(0, level - 35) / 33.0;
            if (player.getRandom().nextDouble() < chance) {
                good++;
            }
        }
        if (good > 0) {
            SkillingUtil.give(player, "jug_of_wine", good);
            XpService.add(player, Skill.COOKING, 200.0 * good);
        }
        if (jugs - good > 0) {
            Inv.give(player, "jug_of_bad_wine", jugs - good);
        }
        Msg.game(player, "The wine has fermented" + (jugs - good > 0 ? ", but " + (jugs - good) + " jug" + (jugs - good > 1 ? "s" : "")
                + " went bad." : ".").toLowerCase(Locale.ROOT));
    }
}
