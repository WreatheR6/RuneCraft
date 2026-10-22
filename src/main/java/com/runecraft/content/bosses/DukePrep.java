package com.runecraft.content.bosses;

import com.runecraft.action.Action;
import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.bosses.DukeRules;
import com.runecraft.player.Players;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
        com.runecraft.content.BlockHooks.register(DukeRules.SALT_PILE, (p, pos, state) -> mineSalt(p, pos));
        com.runecraft.content.BlockHooks.register(DukeRules.VAT, (p, pos, state) -> useVat(p, vatKey(p, pos)));
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

    /** Players mining a salt deposit, with their running action (weak: a player who logged out is forgotten). */
    private static final Map<ServerPlayer, SaltMining> MINING = new WeakHashMap<>();

    /**
     * Starts mining salax salt from a deposit (a pickaxe needed): one harvest each time the pickaxe swings, for five minutes
     * ("Salt Deposit"). Clicking the deposit again while mining only restarts the five minutes, so holding the button down
     * neither speeds it up nor restarts the swing.
     */
    static void mineSalt(ServerPlayer p, BlockPos pos) {
        if (com.runecraft.skill.Tools.bestPickaxe(p, Players.data(p).skills()) == null) {
            Msg.game(p, "You need a pickaxe to mine the salt.");
            return;
        }
        SaltMining running = MINING.get(p);
        if (running != null && ActionManager.isBusy(p)) {
            running.left = DukeRules.SALT_MINING_TICKS;
            return;
        }
        Msg.action(p, "You swing your pickaxe at the salt deposit.");
        SaltMining mining = new SaltMining(pos);
        ActionManager.start(p, mining); // (stops whatever ran before, which forgets an older salt action first)
        MINING.put(p, mining);
    }

    /** One harvest of the deposit: 3-6 salt by Mining level and 5 Mining XP; returns the salt mined. */
    static int harvestSalt(ServerPlayer p) {
        int n = DukeRules.saltPerHarvest(Players.data(p).skills().level(Skill.MINING));
        Inv.give(p, DukeRules.SALAX_SALT, n);
        XpService.add(p, Skill.MINING, DukeRules.SALT_XP);
        Msg.action(p, "You mine " + n + " salax salt.");
        return n;
    }

    /** Mining a salt deposit: a harvest every pickaxe swing (3 ticks with a rune pickaxe) until five minutes pass. */
    private static final class SaltMining implements Action {
        private final BlockPos pos;
        private int left = DukeRules.SALT_MINING_TICKS;
        private int wait = -1;

        SaltMining(BlockPos pos) {
            this.pos = pos;
        }

        @Override
        public boolean tick(ServerPlayer p, long gameTick) {
            if (--left < 0) {
                Msg.game(p, "You stop mining the salt deposit.");
                return false;
            }
            com.runecraft.skill.Tools.Tool pick = com.runecraft.skill.Tools.bestPickaxeTool(p, Players.data(p).skills());
            if (pick == null) {
                return false;
            }
            if (wait < 0) {
                wait = pick.miningTicks(p.getRandom().nextDouble());
            }
            if (--wait > 0) {
                return true;
            }
            wait = -1;
            p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            p.level().playSound(null, pos, SoundEvents.STONE_HIT, SoundSource.PLAYERS, 1f, 1f);
            harvestSalt(p);
            return true;
        }

        @Override
        public void stop(ServerPlayer p) {
            MINING.remove(p, this);
        }
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

    /**
     * The fermentation vats by dimension and block (a vat is a block of the asylum). Each brews one poison at a time
     * ({@link DukeRules.Vat}); vats are scenery, so a server restart empties them (approx: OSRS vats reset with the instance).
     */
    private static final Map<String, DukeRules.Vat> VATS = new java.util.HashMap<>();

    /** A vat's key: its dimension and block position. */
    static String vatKey(ServerPlayer p, BlockPos pos) {
        return p.level().dimension().location() + "@" + pos.asLong();
    }

    /**
     * Using a fermentation vat (OSRS Wiki "Arder-musca poison", "Duke Sucellus/Strategies"): an empty vat takes six salax
     * salt, six arder powder and six musca powder for one poison and ferments them for {@link DukeRules#FERMENT_TICKS}; a
     * finished batch is collected (10 Herblore XP) by using the vat again. Returns 1 when a poison was collected, else 0.
     */
    static int useVat(ServerPlayer p, String key) {
        DukeRules.Vat vat = VATS.computeIfAbsent(key, k -> new DukeRules.Vat());
        long now = ActionManager.gameTick();
        if (vat.collect(now)) {
            Inv.give(p, DukeRules.POISON, 1);
            XpService.add(p, Skill.HERBLORE, DukeRules.POISON_XP);
            Msg.game(p, "You collect an arder-musca poison from the vat.");
            return 1;
        }
        if (!vat.empty()) {
            Msg.game(p, "The mixture is still fermenting.");
            return 0;
        }
        if (DukeRules.poisons(Inv.count(p, DukeRules.SALAX_SALT), Inv.count(p, DukeRules.ARDER_POWDER),
                Inv.count(p, DukeRules.MUSCA_POWDER)) <= 0) {
            Msg.game(p, "The vat needs " + DukeRules.PER_POISON + " salax salt, arder powder and musca powder for a poison.");
            return 0;
        }
        Inv.remove(p, DukeRules.SALAX_SALT, DukeRules.PER_POISON);
        Inv.remove(p, DukeRules.ARDER_POWDER, DukeRules.PER_POISON);
        Inv.remove(p, DukeRules.MUSCA_POWDER, DukeRules.PER_POISON);
        vat.fill(now);
        Msg.game(p, "You add the salt and powders to the vat. The mixture begins to ferment.");
        return 0;
    }

    /** Self-test: game ticks until a vat's batch is ready (0 when ready or empty). */
    static long vatTicksLeft(String key) {
        DukeRules.Vat vat = VATS.get(key);
        return vat == null ? 0 : vat.ticksLeft(ActionManager.gameTick());
    }

    /** Self-test: empties a vat. */
    static void clearVat(String key) {
        VATS.remove(key);
    }
}
