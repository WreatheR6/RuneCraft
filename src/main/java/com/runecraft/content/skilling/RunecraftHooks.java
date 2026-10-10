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
import net.minecraft.block.BlockState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

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
            if (hand != Hand.MAIN_HAND || world.isClient() || !(player instanceof ServerPlayerEntity sp)) {
                return ActionResult.PASS;
            }
            BlockState state = world.getBlockState(hit.getBlockPos());
            if (!(state.getBlock() instanceof RuneAltarBlock altarBlock)) {
                return ActionResult.PASS;
            }
            ItemHooks.emptyPouches(sp);
            return altar(sp, altarBlock.altar(), hit.getBlockPos()) ? ActionResult.SUCCESS : ActionResult.PASS;
        });
        BlockHooks.register("ourania_altar", (player, pos, state) -> ourania(player, pos));
        for (CraftingData.Orb orb : CraftingData.ORBS) {
            BlockHooks.register(orb.obelisk(), (player, pos, state) -> chargeOrbs(player, pos, orb));
        }
    }

    /** Tiara binding and combination runes; returns true if handled (otherwise normal crafting runs). */
    private static boolean altar(ServerPlayerEntity player, RuneAltar altar, BlockPos pos) {
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
        for (RunecraftData.Combination c : RunecraftData.COMBINATIONS) {
            if (c.altar() == altar && c.talisman().equals(held)) {
                combination(player, c, pos);
                return true;
            }
        }
        return false;
    }

    private static void combination(ServerPlayerEntity player, RunecraftData.Combination c, BlockPos pos) {
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
        Inv.remove(player, c.talisman(), 1);
        // A binding necklace (if another module adds one) guarantees success; otherwise 50% per rune.
        boolean binding = Inv.has(player, "binding_necklace", 1);
        Random random = player.getRandom();
        int made = 0;
        for (int i = 0; i < n; i++) {
            if (binding || random.nextBoolean()) {
                made++;
            }
        }
        SkillingUtil.give(player, c.id(), made);
        XpService.add(player, Skill.RUNECRAFT, c.xp() * n);
        Msg.game(player, "You bind the temple's power into " + made + " " + SkillingUtil.lower(c.name()) + "s.");
        effects(player, pos);
    }

    private static void ourania(ServerPlayerEntity player, BlockPos pos) {
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
        Random random = player.getRandom();
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

    private static void effects(ServerPlayerEntity player, BlockPos pos) {
        ServerWorld world = (ServerWorld) player.getWorld();
        world.spawnParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.1);
        world.playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    private static boolean canCharge(ServerPlayerEntity player, CraftingData.Orb orb) {
        return Inv.has(player, "unpowered_orb", 1) && Inv.has(player, "cosmic_rune", 3) && Inv.has(player, orb.rune(), 30);
    }

    private static void chargeOrbs(ServerPlayerEntity player, BlockPos pos, CraftingData.Orb orb) {
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
            public boolean tick(ServerPlayerEntity p, long gameTick) {
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
                p.swingHand(Hand.MAIN_HAND, true);
                Msg.action(p, "You charge the orb with the power of " + orb.element() + ".");
                ((ServerWorld) p.getWorld()).spawnParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.5,
                        pos.getZ() + 0.5, 20, 0.3, 0.5, 0.3, 0.2);
                return true;
            }
        });
    }

    // ------------------------------------------------------------------ wine

    /** Unfermented wine turns into wine after ~12 seconds (OSRS: 200 Cooking XP each, bad wine below 68). */
    static void fermentWine(ServerPlayerEntity player) {
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
