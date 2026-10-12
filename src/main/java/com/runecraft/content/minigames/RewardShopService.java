package com.runecraft.content.minigames;

import com.runecraft.RuneCraft;
import com.runecraft.collection.CollectionLog;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.MinigameCurrencies;
import com.runecraft.osrs.content.minigames.MinigameRules;
import com.runecraft.osrs.content.minigames.RewardShop;
import com.runecraft.osrs.content.minigames.RewardShops;
import com.runecraft.player.Players;
import com.runecraft.skill.Drops;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** Opening reward shops on the client and handling purchases. */
public final class RewardShopService {
    private static final Random RANDOM = new Random();

    /** Adjusts a cost for a player, e.g. diary discounts: (player, shop id, amount) -> amount. */
    public interface CostAdjuster {
        int adjust(ServerPlayerEntity player, String shopId, int amount);
    }

    /** Set by other modules (the systems module's Karamja gloves TzHaar discount). */
    public static CostAdjuster costAdjuster = (p, shop, amount) -> amount;
    /** Extra reason an item can't be bought (diary requirements), or null: (player, item id) -> reason. */
    public static java.util.function.BiFunction<ServerPlayerEntity, String, String> extraLock = (p, item) -> null;

    private RewardShopService() {
    }

    /** The entry with its costs adjusted for this player. */
    private static RewardShop.Entry priced(ServerPlayerEntity player, String shopId, RewardShop.Entry e) {
        java.util.List<RewardShop.Cost> costs = new java.util.ArrayList<>();
        boolean changed = false;
        for (RewardShop.Cost c : e.costs()) {
            int amount = Math.max(0, costAdjuster.adjust(player, shopId, c.amount()));
            changed |= amount != c.amount();
            costs.add(new RewardShop.Cost(c.currency(), amount));
        }
        return changed ? new RewardShop.Entry(e.reward(), e.item(), e.count(), costs, e.label(), e.requiredSkill(),
                e.requiredLevel(), e.requiresItem()) : e;
    }

    public static int balance(ServerPlayerEntity player, String currency) {
        if (currency.startsWith("item:")) {
            return Inv.count(player, MinigameCurrencies.key(currency));
        }
        return MinigameData.of(player).points(player.getUuid(), MinigameCurrencies.key(currency));
    }

    private static Map<String, Integer> balances(ServerPlayerEntity player, RewardShop shop) {
        Map<String, Integer> map = new HashMap<>();
        for (String c : shop.currencies()) {
            map.put(c, balance(player, c));
        }
        return map;
    }

    private static String label(RewardShop.Entry e) {
        if (e.label() != null) {
            return e.label();
        }
        return switch (e.item()) {
            case "#herbs" -> "Herb pack";
            case "#seeds" -> "Seed pack";
            case "#ores" -> "Mineral pack";
            default -> {
                ItemDef def = ItemCatalog.get(e.item());
                String name = def == null ? e.item() : def.name();
                yield e.count() > 1 ? name + " x" + e.count() : name;
            }
        };
    }

    private static String icon(RewardShop.Entry e) {
        return switch (e.reward()) {
            case XP -> "minecraft:experience_bottle";
            case UNLOCK -> RuneCraft.resolve("bones_to_peaches_tablet").toString();
            case ITEM -> switch (e.item()) {
                case "#herbs" -> RuneCraft.resolve("grimy_ranarr_weed").toString();
                case "#seeds" -> RuneCraft.resolve("ranarr_seed").toString();
                case "#ores" -> RuneCraft.resolve("coal").toString();
                default -> RuneCraft.resolve(e.item()).toString();
            };
        };
    }

    /** Problem that prevents buying (level, prerequisite item, already unlocked) or null. */
    private static String locked(ServerPlayerEntity player, RewardShop.Entry e) {
        if (e.requiredSkill() != null && Players.data(player).skills().level(e.requiredSkill()) < e.requiredLevel()) {
            return "Requires level " + e.requiredLevel() + " " + e.requiredSkill().displayName();
        }
        if (e.requiresItem() != null && !Inv.has(player, e.requiresItem(), 1)) {
            ItemDef def = ItemCatalog.get(e.requiresItem());
            return "Requires " + (def == null ? e.requiresItem() : def.name());
        }
        if (e.reward() == RewardShop.Reward.UNLOCK && MinigameData.of(player).flag(player.getUuid(), e.item())) {
            return "Already unlocked";
        }
        return extraLock.apply(player, e.item());
    }

    public static void open(ServerPlayerEntity player, String shopId) {
        RewardShop shop = RewardShops.get(shopId);
        if (shop == null) {
            return;
        }
        Map<String, Integer> bal = balances(player, shop);
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(shop.id());
        buf.writeString(shop.title());
        buf.writeVarInt(shop.currencies().size());
        for (String c : shop.currencies()) {
            buf.writeString(MinigameCurrencies.name(c));
            buf.writeVarInt(bal.get(c));
        }
        buf.writeVarInt(shop.entries().size());
        for (RewardShop.Entry raw : shop.entries()) {
            RewardShop.Entry e = priced(player, shop.id(), raw);
            String lock = locked(player, e);
            buf.writeString(label(e));
            buf.writeString(icon(e));
            buf.writeString(e.costText());
            buf.writeBoolean(lock == null && e.affordable(bal, 1));
            buf.writeString(lock == null ? "" : lock);
        }
        ServerPlayNetworking.send(player, MinigameNet.SHOP, buf);
    }

    public static void buy(ServerPlayerEntity player, String shopId, int index, int quantity) {
        RewardShop shop = RewardShops.get(shopId);
        if (shop == null || index < 0 || index >= shop.entries().size()) {
            return;
        }
        RewardShop.Entry e = priced(player, shop.id(), shop.entries().get(index));
        int times = e.reward() == RewardShop.Reward.ITEM ? Math.max(1, Math.min(quantity, 100)) : 1;
        String lock = locked(player, e);
        if (lock != null) {
            Msg.game(player, lock + ".");
            return;
        }
        Map<String, Integer> bal = balances(player, shop);
        if (!e.affordable(bal, times)) {
            // Buy as many as affordable.
            while (times > 1 && !e.affordable(bal, times)) {
                times--;
            }
            if (!e.affordable(bal, times)) {
                Msg.game(player, "You can't afford that. It costs " + e.costText() + ".");
                return;
            }
        }
        for (RewardShop.Cost c : e.costs()) {
            int amount = c.amount() * times;
            if (c.isItem()) {
                Inv.remove(player, c.key(), amount);
            } else {
                MinigameData.of(player).addPoints(player.getUuid(), c.key(), -amount);
            }
        }
        switch (e.reward()) {
            case ITEM -> {
                for (int i = 0; i < times; i++) {
                    giveItem(player, e);
                }
                Msg.game(player, "You buy " + label(e) + (times > 1 ? " x" + times : "") + ".");
            }
            case XP -> {
                Skill skill = Skill.valueOf(e.item());
                int level = Players.data(player).skills().level(skill);
                int amount = e.costs().get(0).amount();
                int xp = MinigameRules.PestControl.xpFor(level, e.count(), amount);
                XpService.add(player, skill, xp);
                Msg.game(player, "You gain " + String.format("%,d", xp) + " " + skill.displayName() + " experience.");
            }
            case UNLOCK -> {
                MinigameData.of(player).setFlag(player.getUuid(), e.item());
                if (e.item().equals("bones_to_peaches")) {
                    Inv.give(player, "bones_to_peaches_tablet", 1);
                    CollectionLog.obtain(player, "bones_to_peaches_tablet");
                }
                Msg.game(player, "You unlock " + label(e) + "!");
            }
        }
        open(player, shopId);
    }

    private static void giveItem(ServerPlayerEntity player, RewardShop.Entry e) {
        switch (e.item()) {
            case "#herbs" -> {
                for (int i = 0; i < 3 + RANDOM.nextInt(4); i++) {
                    give(player, Drops.rollHerb(player.getRandom()), 1);
                }
            }
            case "#seeds" -> {
                for (int i = 0; i < 3 + RANDOM.nextInt(3); i++) {
                    give(player, Drops.rollSeed(player.getRandom()), 1);
                }
            }
            case "#ores" -> {
                String[] ores = {"coal", "iron_ore", "mithril_ore", "adamantite_ore"};
                give(player, ores[RANDOM.nextInt(ores.length)], 10 + RANDOM.nextInt(15));
            }
            default -> give(player, e.item(), e.count());
        }
    }

    private static void give(ServerPlayerEntity player, String item, int count) {
        if (item == null) {
            return;
        }
        Inv.give(player, item, count);
        CollectionLog.obtain(player, item);
    }
}
