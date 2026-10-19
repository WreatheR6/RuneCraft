package com.runecraft.content.minigames;

import com.runecraft.RuneCraft;
import com.runecraft.collection.CollectionLog;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.MinigameCurrencies;
import com.runecraft.osrs.content.minigames.RewardShop;
import com.runecraft.osrs.content.minigames.RewardShops;
import com.runecraft.player.Players;
import com.runecraft.skill.Drops;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.platform.PacketByteBufs;
import com.runecraft.platform.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** Opening reward shops on the client and handling purchases. */
public final class RewardShopService {
    private static final Random RANDOM = new Random();

    /** Adjusts a cost for a player, e.g. diary discounts: (player, shop id, amount) -> amount. */
    public interface CostAdjuster {
        int adjust(ServerPlayer player, String shopId, int amount);
    }

    /** Set by other modules (the systems module's Karamja gloves TzHaar discount). */
    public static CostAdjuster costAdjuster = (p, shop, amount) -> amount;
    /** Extra reason an item can't be bought (diary requirements), or null: (player, item id) -> reason. */
    public static java.util.function.BiFunction<ServerPlayer, String, String> extraLock = (p, item) -> null;
    /** Adjusts Nightmare Zone / Soul Wars imbue costs (Combat Achievements hard tier halves them): (player, cost) -> cost. */
    public static java.util.function.BiFunction<ServerPlayer, Integer, Integer> imbueCost = (p, amount) -> amount;

    private RewardShopService() {
    }

    /** The entry with its costs adjusted for this player. */
    private static RewardShop.Entry priced(ServerPlayer player, String shopId, RewardShop.Entry e) {
        java.util.List<RewardShop.Cost> costs = new java.util.ArrayList<>();
        boolean changed = false;
        for (RewardShop.Cost c : e.costs()) {
            int amount = Math.max(0, costAdjuster.adjust(player, shopId, c.amount()));
            if (e.reward() == RewardShop.Reward.UPGRADE && (shopId.equals("nightmare_zone") || shopId.equals("soul_wars"))) {
                amount = Math.max(0, imbueCost.apply(player, amount));
            }
            changed |= amount != c.amount();
            costs.add(new RewardShop.Cost(c.currency(), amount));
        }
        return changed ? new RewardShop.Entry(e.reward(), e.item(), e.count(), costs, e.label(), e.requiredSkill(),
                e.requiredLevel(), e.requiresItem()) : e;
    }

    public static int balance(ServerPlayer player, String currency) {
        if (currency.startsWith("item:")) {
            return Inv.count(player, MinigameCurrencies.key(currency));
        }
        return MinigameData.of(player).points(player.getUUID(), MinigameCurrencies.key(currency));
    }

    private static Map<String, Integer> balances(ServerPlayer player, RewardShop shop) {
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
        if (e.item().startsWith(com.runecraft.osrs.content.minigames.RewardShops.GRACEFUL_RECOLOUR)) {
            ItemDef hood = ItemCatalog.get("graceful_hood" + e.item().substring(
                    com.runecraft.osrs.content.minigames.RewardShops.GRACEFUL_RECOLOUR.length()));
            return "Graceful recolour" + (hood == null ? "" : hood.name().substring("Graceful hood".length()));
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
            case UPGRADE -> RuneCraft.resolve(e.item()).toString();
            case ITEM -> switch (e.item()) {
                case "#herbs" -> RuneCraft.resolve("grimy_ranarr_weed").toString();
                case "#seeds" -> RuneCraft.resolve("ranarr_seed").toString();
                case "#ores" -> RuneCraft.resolve("coal").toString();
                case "#ba_gamble_low" -> RuneCraft.resolve("rune_axe").toString();
                case "#ba_gamble_medium" -> RuneCraft.resolve("teak_plank").toString();
                case "#ba_gamble_high" -> RuneCraft.resolve("pet_penance_queen").toString();
                default -> e.item().startsWith(com.runecraft.osrs.content.minigames.RewardShops.GRACEFUL_RECOLOUR)
                        ? RuneCraft.resolve("graceful_hood" + e.item().substring(
                                com.runecraft.osrs.content.minigames.RewardShops.GRACEFUL_RECOLOUR.length())).toString()
                        : RuneCraft.resolve(e.item()).toString();
            };
        };
    }

    /** Problem that prevents buying (level, prerequisite item, already unlocked) or null. */
    private static String locked(ServerPlayer player, RewardShop.Entry e) {
        if (e.requiredSkill() != null && Players.data(player).skills().level(e.requiredSkill()) < e.requiredLevel()) {
            return "Requires level " + e.requiredLevel() + " " + e.requiredSkill().displayName();
        }
        if (e.requiresItem() != null && !Inv.has(player, e.requiresItem(), 1)) {
            ItemDef def = ItemCatalog.get(e.requiresItem());
            return "Requires " + (def == null ? e.requiresItem() : def.name());
        }
        String tool = e.reward() == RewardShop.Reward.UPGRADE ? com.runecraft.osrs.content.minigames.MinigameRules.Imbues.tool(e.requiresItem()) : null;
        if (tool != null && !Inv.has(player, tool, 1)) {
            ItemDef def = ItemCatalog.get(tool);
            return "Requires " + (def == null ? tool : def.name().toLowerCase(java.util.Locale.ROOT)) + " in your inventory";
        }
        if (e.item().startsWith(com.runecraft.osrs.content.minigames.RewardShops.GRACEFUL_RECOLOUR)) {
            for (com.runecraft.osrs.content.skilling.AgilityData.Piece p : com.runecraft.osrs.content.skilling.AgilityData.GRACEFUL) {
                if (!Inv.has(player, p.id(), 1)) {
                    return "Requires a full Graceful set in your inventory";
                }
            }
        }
        if (e.reward() == RewardShop.Reward.UNLOCK && MinigameData.of(player).flag(player.getUUID(), e.item())) {
            return "Already unlocked";
        }
        // Barbarian Assault gambles: low and medium need wave 3 / 6 of the current round.
        com.runecraft.osrs.content.minigames.BaGambles.Tier gamble = com.runecraft.osrs.content.minigames.BaGambles.Tier.of(e.item());
        if (gamble != null) {
            String waveLock = com.runecraft.osrs.content.minigames.BaGambles.lock(gamble,
                    com.runecraft.content.minigames.games.BarbarianAssaultSession.waveReached(player));
            if (waveLock != null) {
                return waveLock;
            }
        }
        // Last Man Standing: tradeable rewards need a won game (OSRS Wiki, May 2026).
        int wins = com.runecraft.osrs.content.minigames.LastManStanding.winsNeeded(e.item());
        if (wins > 0 && e.costs().stream().anyMatch(c -> c.currency().equals(com.runecraft.osrs.content.minigames.LastManStanding.POINTS))
                && MinigameData.of(player).stat(player.getUUID(), com.runecraft.osrs.content.minigames.LastManStanding.WINS) < wins) {
            return "Requires " + wins + " competitive Last Man Standing win" + (wins == 1 ? "" : "s");
        }
        return extraLock.apply(player, e.item());
    }

    public static void open(ServerPlayer player, String shopId) {
        RewardShop shop = RewardShops.get(shopId);
        if (shop == null) {
            return;
        }
        Map<String, Integer> bal = balances(player, shop);
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(shop.id());
        buf.writeUtf(shop.title());
        buf.writeVarInt(shop.currencies().size());
        for (String c : shop.currencies()) {
            buf.writeUtf(MinigameCurrencies.name(c));
            buf.writeVarInt(bal.get(c));
        }
        buf.writeVarInt(shop.entries().size());
        for (RewardShop.Entry raw : shop.entries()) {
            RewardShop.Entry e = priced(player, shop.id(), raw);
            String lock = locked(player, e);
            buf.writeUtf(label(e));
            buf.writeUtf(icon(e));
            buf.writeUtf(e.costText());
            buf.writeBoolean(lock == null && e.affordable(bal, 1));
            buf.writeUtf(lock == null ? "" : lock);
        }
        ServerPlayNetworking.send(player, MinigameNet.SHOP, buf);
    }

    /** MinigameData stat key for a daily-limited reward: today's UTC epoch day x 1000 + the number bought today. */
    private static String dailyKey(String shopId, String item) {
        return "daily:" + shopId + ":" + item;
    }

    private static long today() {
        return java.time.LocalDate.now(java.time.ZoneOffset.UTC).toEpochDay();
    }

    /** How many of a daily-limited reward the player has bought today (OSRS days reset at 00:00 UTC). */
    public static int boughtToday(ServerPlayer player, String shopId, String item) {
        int v = MinigameData.of(player).stat(player.getUUID(), dailyKey(shopId, item));
        return v / 1000 == (int) (today() % 2_000_000) ? v % 1000 : 0;
    }

    private static void recordToday(ServerPlayer player, String shopId, String item, int count) {
        MinigameData.of(player).setStat(player.getUUID(), dailyKey(shopId, item), (int) (today() % 2_000_000) * 1000 + count);
    }

    /** Self-test: forgets today's purchases of a daily-limited reward. */
    public static void resetDaily(ServerPlayer player, String shopId, String item) {
        MinigameData.of(player).setStat(player.getUUID(), dailyKey(shopId, item), 0);
    }

    public static void buy(ServerPlayer player, String shopId, int index, int quantity) {
        RewardShop shop = RewardShops.get(shopId);
        if (shop == null || index < 0 || index >= shop.entries().size()) {
            return;
        }
        RewardShop.Entry e = priced(player, shop.id(), shop.entries().get(index));
        int times = e.reward() == RewardShop.Reward.ITEM
                && !e.item().startsWith(com.runecraft.osrs.content.minigames.RewardShops.GRACEFUL_RECOLOUR)
                ? Math.max(1, Math.min(quantity, 100)) : 1;
        String lock = locked(player, e);
        if (lock != null) {
            Msg.game(player, lock + ".");
            return;
        }
        int daily = com.runecraft.osrs.content.minigames.RewardShops.dailyLimit(shop.id(), e.item());
        if (daily > 0) {
            int left = daily - boughtToday(player, shop.id(), e.item());
            if (left <= 0) {
                Msg.game(player, "You can only buy " + daily + " of those a day.");
                return;
            }
            times = Math.min(times, left);
        }
        // Nightmare Zone potions are bought by the dose into the lobby's potion barrels (255 doses each).
        boolean barrel = shop.id().equals("nightmare_zone")
                && com.runecraft.osrs.content.minigames.MinigameRules.NightmareZone.POTIONS.contains(e.item());
        if (barrel) {
            times = com.runecraft.content.minigames.games.NightmareZoneSession.barrelRoom(player, e.item(), times);
            if (times <= 0) {
                Msg.game(player, "That potion barrel is full.");
                return;
            }
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
        if (daily > 0) {
            recordToday(player, shop.id(), e.item(), boughtToday(player, shop.id(), e.item()) + times);
        }
        for (RewardShop.Cost c : e.costs()) {
            int amount = c.amount() * times;
            if (c.isItem()) {
                Inv.remove(player, c.key(), amount);
            } else {
                MinigameData.of(player).addPoints(player.getUUID(), c.key(), -amount);
            }
        }
        switch (e.reward()) {
            case ITEM -> {
                if (barrel) {
                    com.runecraft.content.minigames.games.NightmareZoneSession.storeDoses(player, e.item(), times);
                    Msg.game(player, "You buy " + times + " dose" + (times == 1 ? "" : "s") + " of " + label(e) + ". The barrel now holds "
                            + com.runecraft.content.minigames.games.NightmareZoneSession.barrel(player, e.item()) + ".");
                    open(player, shopId);
                    return;
                }
                if (RewardShops.noted(shop.id(), e.item())
                        && com.runecraft.osrs.bank.BankNotes.canNote(e.item())) {
                    // Handed out as bank notes; whatever doesn't fit drops at the player's feet.
                    int n = e.count() * times;
                    net.minecraft.world.item.ItemStack note = com.runecraft.bank.Notes.note(e.item(), "", 1);
                    int in = note.isEmpty() ? 0 : com.runecraft.bank.Notes.insert(player, note, n);
                    if (note.isEmpty()) {
                        give(player, e.item(), n);
                    } else {
                        if (in < n) {
                            player.drop(note.copyWithCount(n - in), false);
                        }
                        CollectionLog.obtain(player, e.item());
                    }
                } else {
                    for (int i = 0; i < times; i++) {
                        giveItem(player, e);
                    }
                }
                Msg.game(player, "You buy " + label(e) + (times > 1 ? " x" + times : "") + ".");
            }
            case XP -> {
                Skill skill = Skill.valueOf(e.item());
                int level = Players.data(player).skills().level(skill);
                // Flat-rate entries (negative count, e.g. Agility tickets) use xpFor; points shops use xpReward.
                int xp = e.count() < 0 ? (int) Math.floor(RewardShop.xpFor(e, level)) : RewardShop.xpReward(e, level);
                XpService.add(player, skill, xp);
                Msg.game(player, "You gain " + String.format("%,d", xp) + " " + skill.displayName() + " experience.");
            }
            case UNLOCK -> {
                MinigameData.of(player).setFlag(player.getUUID(), e.item());
                if (e.item().equals("bones_to_peaches")) {
                    Inv.give(player, "bones_to_peaches_tablet", 1);
                    CollectionLog.obtain(player, "bones_to_peaches_tablet");
                }
                Msg.game(player, "You unlock " + label(e) + "!");
            }
            case UPGRADE -> {
                // The requirement check above guarantees the item is carried.
                Inv.remove(player, e.requiresItem(), 1);
                give(player, e.item(), 1);
                ItemDef from = ItemCatalog.get(e.requiresItem());
                Msg.game(player, "Your " + (from == null ? e.requiresItem() : from.name()) + " becomes " + label(e) + ".");
            }
        }
        open(player, shopId);
    }

    private static void giveItem(ServerPlayer player, RewardShop.Entry e) {
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
            case "#ba_gamble_low", "#ba_gamble_medium", "#ba_gamble_high" -> com.runecraft.content.minigames.games.BarbarianAssaultSession
                    .gamble(player, com.runecraft.osrs.content.minigames.BaGambles.Tier.of(e.item()));
            case "#ores" -> {
                // Mineral pack (OSRS Wiki): 25 coal and 18 iron ore.
                give(player, "coal", 25);
                give(player, "iron_ore", 18);
            }
            default -> {
                if (e.item().startsWith(com.runecraft.osrs.content.minigames.RewardShops.GRACEFUL_RECOLOUR)) {
                    // Recolour the carried plain Graceful set (checked in locked()).
                    String suffix = e.item().substring(com.runecraft.osrs.content.minigames.RewardShops.GRACEFUL_RECOLOUR.length());
                    for (com.runecraft.osrs.content.skilling.AgilityData.Piece p : com.runecraft.osrs.content.skilling.AgilityData.GRACEFUL) {
                        Inv.remove(player, p.id(), 1);
                        give(player, p.id() + suffix, 1);
                    }
                } else {
                    give(player, e.item(), e.count());
                }
            }
        }
    }

    private static void give(ServerPlayer player, String item, int count) {
        if (item == null) {
            return;
        }
        Inv.give(player, item, count);
        CollectionLog.obtain(player, item);
    }
}
