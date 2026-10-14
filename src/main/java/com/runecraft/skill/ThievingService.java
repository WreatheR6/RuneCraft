package com.runecraft.skill;

import com.runecraft.block.StallBlock;
import com.runecraft.collection.CollectionLog;
import com.runecraft.content.GameEvents;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.data.Thieving;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pickpocketing and stealing from stalls. Every pickpocket target in the game (built-in creatures,
 * the npcs module's townsfolk and the skilling module's targets) goes through {@link #attempt}, which
 * applies the shared OSRS rules: coin pouches (stacking to 28, more with the Ardougne diary), the
 * rogue equipment's double loot, the dodgy necklace, the Thieving cape bonus and the stun on failure.
 */
public final class ThievingService {
    private ThievingService() {
    }

    public static void stealFromStall(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state, StallBlock block) {
        if (!Stun.check(player)) {
            return;
        }
        Thieving.Stall stall = block.stall();
        if (state.get(StallBlock.EMPTY)) {
            Msg.game(player, "The stall is empty.");
            return;
        }
        if (Players.data(player).skills().currentLevel(Skill.THIEVING) < stall.level()) {
            Msg.game(player, "You need a Thieving level of " + stall.level() + " to steal from the " + stall.displayName() + ".");
            return;
        }
        Thieving.Loot l = stall.pick(world.getRandom().nextInt(stall.totalWeight()));
        player.swingHand(Hand.MAIN_HAND, true);
        Inv.give(player, l.item(), l.min());
        CollectionLog.obtain(player, l.item());
        GameEvents.fireActivity(player, "thieve:" + stall.blockId()); // Rocky's rate is per stall
        XpService.add(player, Skill.THIEVING, stall.xp());
        Msg.action(player, "You steal from the " + stall.displayName() + ".");
        block.empty(world, pos, state);
    }

    /** Pickpocket success-chance multiplier (achievement diary perks register theirs; 1.0 = none). */
    public static java.util.function.ToDoubleFunction<ServerPlayerEntity> pickpocketBonus = p -> 1.0;

    /** Whether a failed pickpocket goes unnoticed (Shadow Veil registers its 15% chance; default never). */
    public static java.util.function.Predicate<ServerPlayerEntity> failUnnoticed = p -> false;

    /** Whether the player wears an item in one of the systems module's extra slots (cape, neck, hands...). */
    public static java.util.function.BiPredicate<ServerPlayerEntity, String> wearingExtra = (p, id) -> false;

    /**
     * A worn dodgy necklace saving the player from a stun: the systems module rolls its 25% chance and
     * uses up a charge (default: no necklace).
     */
    public static java.util.function.Predicate<ServerPlayerEntity> dodgyNecklace = p -> false;

    /** Coin pouches the player may carry (the systems module raises it with the Ardougne diary). */
    public static java.util.function.ToIntFunction<ServerPlayerEntity> coinPouchLimit = p -> Thieving.COIN_POUCH_LIMIT;

    /**
     * A pickpocket success chance after bonuses: gloves of silence and the Ardougne diary ({@link #pickpocketBonus}),
     * then the Thieving cape, each scaling the target's low/high values ({@link Thieving#pickpocketChance}).
     */
    public static double pickpocketChance(ServerPlayerEntity player, int level, int low, int high) {
        double bonus = pickpocketBonus.applyAsDouble(player);
        double cape = wearingExtra.test(player, "thieving_cape") || wearingExtra.test(player, "thieving_cape_t")
                || wearingExtra.test(player, "max_cape") ? Thieving.THIEVING_CAPE_BONUS : 1.0;
        return Thieving.pickpocketChance(level, low, high, bonus, cape);
    }

    /** Rogue equipment pieces worn (mask, top, trousers, boots in the armour slots; gloves in the hands slot). */
    public static int roguePieces(ServerPlayerEntity player) {
        int n = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (Thieving.ROGUE_OUTFIT.contains(Inv.idOf(player.getEquippedStack(slot)))) {
                n++;
            }
        }
        if (wearingExtra.test(player, "rogue_gloves")) {
            n++;
        }
        return n;
    }

    /** Coin pouches of every kind the player carries. */
    public static int coinPouches(ServerPlayerEntity player) {
        int n = 0;
        for (Thieving.CoinPouch p : Thieving.COIN_POUCHES) {
            n += Inv.count(player, p.itemId());
        }
        return n;
    }

    /**
     * Opens every coin pouch of one kind in the inventory and gives the coins. Returns false if the
     * item is not a coin pouch.
     */
    public static boolean openPouches(ServerPlayerEntity player, String itemId) {
        Thieving.CoinPouch pouch = Thieving.pouchByItem(itemId);
        if (pouch == null) {
            return false;
        }
        int n = Inv.count(player, itemId);
        if (n <= 0) {
            return true;
        }
        Inv.remove(player, itemId, n);
        long coins = 0;
        for (int i = 0; i < n; i++) {
            coins += pouch.min() + (pouch.max() > pouch.min() ? player.getRandom().nextInt(pouch.max() - pouch.min() + 1) : 0);
        }
        Inv.give(player, Inv.COINS, (int) Math.min(Integer.MAX_VALUE, coins));
        Msg.action(player, "You open " + (n == 1 ? "the coin pouch" : "all of your coin pouches") + " and find " + coins + " coins.");
        com.runecraft.network.ServerNetworking.sync(player);
        return true;
    }

    /** One pickpocket target as seen by the shared rules. {@code pouch} is a coin pouch key or null. */
    public record Target(String name, int level, double xp, int low, int high, int stunDamage, String pouch) {
    }

    /** Rolls a successful pickpocket's loot: (item, amount) pairs; "coins" lines become a coin pouch. */
    @FunctionalInterface
    public interface LootRoll {
        void roll(java.util.function.BiConsumer<String, Integer> out);
    }

    /** Pickpocketing one of the built-in targets (men, Al-Kharid warriors, guards...). */
    public static void pickpocket(ServerPlayerEntity player, LivingEntity target, Thieving.Pickpocket kind) {
        attempt(player, target, new Target(kind.displayName(), kind.level(), kind.xp(), kind.low(), kind.high(), kind.stunDamage(),
                kind.pouch()), out -> {
            for (Thieving.Loot l : kind.loot()) {
                if (l.weight() <= 1 || player.getRandom().nextInt(l.weight()) == 0) {
                    out.accept(l.item(), l.min() + (l.max() > l.min() ? player.getRandom().nextInt(l.max() - l.min() + 1) : 0));
                }
            }
        });
    }

    /**
     * The shared pickpocket attempt: level and pouch checks, the success roll, loot (coins as one
     * coin pouch, doubled by the rogue equipment), and on failure the stun and the NPC's hit (which a
     * dodgy necklace may prevent).
     */
    public static void attempt(ServerPlayerEntity player, LivingEntity npc, Target t, LootRoll loot) {
        attempt(player, npc, t, loot, false);
    }

    /** As {@link #attempt(ServerPlayerEntity, LivingEntity, Target, LootRoll)}; {@code sure} = the target is knocked out. */
    public static void attempt(ServerPlayerEntity player, LivingEntity npc, Target t, LootRoll loot, boolean sure) {
        if (!Stun.check(player)) {
            return;
        }
        String who = t.name().startsWith("TzHaar") ? t.name() : t.name().toLowerCase(Locale.ROOT);
        int level = Players.data(player).skills().currentLevel(Skill.THIEVING);
        if (level < t.level()) {
            Msg.game(player, "You need a Thieving level of " + t.level() + " to pickpocket the " + who + ".");
            return;
        }
        if (t.pouch() != null && coinPouches(player) >= coinPouchLimit.applyAsInt(player)) {
            Msg.game(player, "You need to empty your coin pouches before you can continue pickpocketing.");
            return;
        }
        Msg.action(player, "You attempt to pick the " + who + "'s pocket.");
        player.swingHand(Hand.MAIN_HAND, true);
        if (sure || player.getRandom().nextDouble() < pickpocketChance(player, level, t.low(), t.high())) {
            List<String> items = new ArrayList<>();
            List<Integer> counts = new ArrayList<>();
            boolean[] pouched = {false};
            loot.roll((item, n) -> {
                if (item.equals(Inv.COINS) && t.pouch() != null) {
                    // One pouch per pickpocket; the 1-coin "nothing else" filler lines give nothing.
                    if (n > 1 && !pouched[0]) {
                        pouched[0] = true;
                        items.add(Thieving.pouchId(t.pouch()));
                        counts.add(1);
                    }
                } else if (n > 0) {
                    items.add(item);
                    counts.add(n);
                }
            });
            boolean twice = player.getRandom().nextDouble() < Thieving.rogueDoubleChance(roguePieces(player));
            for (int i = 0; i < items.size(); i++) {
                int n = counts.get(i) * (twice ? 2 : 1);
                String item = items.get(i);
                Inv.give(player, item, n);
                if (!item.equals(Inv.COINS)) {
                    CollectionLog.record(player, item);
                }
                GameEvents.fireObtain(player, item, n);
            }
            GameEvents.fireActivity(player, "thieve:" + t.name().toLowerCase(Locale.ROOT)); // Rocky's rate is per target
            XpService.add(player, Skill.THIEVING, t.xp());
            Msg.action(player, "You pick the " + who + "'s pocket.");
            if (twice && !items.isEmpty()) {
                Msg.game(player, "Your rogue clothing allows you to steal twice as much loot!");
            }
            com.runecraft.network.ServerNetworking.sync(player);
        } else {
            Msg.game(player, "You fail to pick the " + who + "'s pocket.");
            if (failUnnoticed.test(player)) {
                Msg.game(player, "Your attempt goes unnoticed.");
                return;
            }
            if (dodgyNecklace.test(player)) {
                Msg.game(player, "Your dodgy necklace protects you.");
                return;
            }
            Msg.red(player, "You have been stunned!");
            npc.swingHand(Hand.MAIN_HAND);
            // OSRS: a failed pickpocket always hits for the NPC's listed stun damage.
            com.runecraft.combat.CombatHandler.applyDamage(npc, player, Math.max(1, t.stunDamage()));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 4, false, false));
            Stun.apply(player, Thieving.STUN_TICKS);
        }
    }
}
