package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.Skill;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A minigame reward shop. Each entry costs one or more currencies; a currency is either an item
 * ({@code item:tokkul}) or a points balance kept by the minigames module ({@code pts:pest_control}).
 * Entries give items, skill experience (Pest Control style), unlock a flag (Bones to Peaches) or
 * upgrade an item the player carries (imbues, wand upgrades: {@code requiresItem} is taken and
 * {@code item} given).
 */
public final class RewardShop {
    public enum Reward { ITEM, XP, UNLOCK, UPGRADE }

    public record Cost(String currency, int amount) {
        public boolean isItem() {
            return currency.startsWith("item:");
        }

        /** Item id or points key without the prefix. */
        public String key() {
            return currency.substring(currency.indexOf(':') + 1);
        }
    }

    /**
     * One thing for sale. For XP entries {@code item} is the skill name and {@code count} is the
     * multiplier N in Pest Control's formula floor(level^2 / 600) * N per point.
     */
    public record Entry(Reward reward, String item, int count, List<Cost> costs, String label, Skill requiredSkill,
                        int requiredLevel, String requiresItem) {
        public boolean affordable(Map<String, Integer> balances, int times) {
            for (Cost c : costs) {
                if (balances.getOrDefault(c.currency(), 0) < (long) c.amount() * times) {
                    return false;
                }
            }
            return true;
        }

        public String costText() {
            StringBuilder sb = new StringBuilder();
            for (Cost c : costs) {
                if (sb.length() > 0) {
                    sb.append(" + ");
                }
                sb.append(String.format("%,d", c.amount())).append(' ').append(MinigameCurrencies.name(c.currency()));
            }
            return sb.toString();
        }
    }

    private final String id;
    private final String title;
    private final List<Entry> entries = new ArrayList<>();

    public RewardShop(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public List<Entry> entries() {
        return entries;
    }

    /** Every currency used by this shop, in first-use order. */
    public List<String> currencies() {
        List<String> list = new ArrayList<>();
        for (Entry e : entries) {
            for (Cost c : e.costs()) {
                if (!list.contains(c.currency())) {
                    list.add(c.currency());
                }
            }
        }
        return list;
    }

    // --- builder helpers ---
    RewardShop item(String item, int count, String currency, int amount) {
        entries.add(new Entry(Reward.ITEM, item, count, List.of(new Cost(currency, amount)), null, null, 0, null));
        return this;
    }

    RewardShop item(String item, String currency, int amount) {
        return item(item, 1, currency, amount);
    }

    RewardShop itemReq(String item, String currency, int amount, String requiresItem) {
        entries.add(new Entry(Reward.ITEM, item, 1, List.of(new Cost(currency, amount)), null, null, 0, requiresItem));
        return this;
    }

    /** An item that needs a skill level to buy (Pest Control's resource packs). */
    RewardShop itemSkill(String item, int count, String currency, int amount, Skill skill, int level) {
        entries.add(new Entry(Reward.ITEM, item, count, List.of(new Cost(currency, amount)), null, skill, level, null));
        return this;
    }

    /** Turns a carried {@code from} into {@code to} (imbues, wand upgrades). */
    RewardShop upgrade(String from, String to, List<Cost> costs) {
        entries.add(new Entry(Reward.UPGRADE, to, 1, List.copyOf(costs), null, null, 0, from));
        return this;
    }

    RewardShop upgrade(String from, String to, String currency, int amount) {
        return upgrade(from, to, List.of(new Cost(currency, amount)));
    }

    RewardShop itemMulti(String item, List<Cost> costs) {
        entries.add(new Entry(Reward.ITEM, item, 1, List.copyOf(costs), null, null, 0, null));
        return this;
    }

    /** A rolled reward ({@code #...} item handled by the shop service) with its own label. */
    RewardShop labelled(String item, String label, List<Cost> costs) {
        entries.add(new Entry(Reward.ITEM, item, 1, List.copyOf(costs), label, null, 0, null));
        return this;
    }

    RewardShop xp(Skill skill, int multiplier, String currency, int amount, int minLevel) {
        entries.add(new Entry(Reward.XP, skill.name(), multiplier, List.of(new Cost(currency, amount)),
                skill.displayName() + " XP (" + amount + " pts)", skill, minLevel, null));
        return this;
    }

    /** Flat XP: {@code xpEach} experience per currency unit (stored as count = -xpEach * 10). */
    RewardShop xpFlat(Skill skill, double xpEach, String currency, int amount) {
        entries.add(new Entry(Reward.XP, skill.name(), -(int) Math.round(xpEach * 10), List.of(new Cost(currency, amount)),
                skill.displayName() + " XP (" + amount + (amount == 1 ? " ticket)" : " tickets)"), null, 0, null));
        return this;
    }

    /** XP an XP entry gives a player of {@code level}: Pest Control's formula, or flat XP per unit for {@link #xpFlat}. */
    public static double xpFor(Entry e, int level) {
        int amount = e.costs().get(0).amount();
        if (e.count() < 0) {
            return -e.count() / 10.0 * amount;
        }
        return MinigameRules.PestControl.xpFor(level, e.count(), amount);
    }

    RewardShop unlock(String flag, String label, List<Cost> costs) {
        entries.add(new Entry(Reward.UNLOCK, flag, 1, List.copyOf(costs), label, null, 0, null));
        return this;
    }

    /** Pest Control's XP per point: floor(level^2 / 600) * N. */
    public static int pestControlXpPerPoint(int level, int multiplier) {
        return (level * level / 600) * multiplier;
    }

    /**
     * Experience bought with points: floor(level^2 / 600) * N per point. Pest Control (and the
     * shops modelled on it) add 1% for 10 points and 10% for 100 points at once; Soul Wars' zeal
     * gets no bulk bonus (OSRS Wiki).
     */
    public static int xpReward(Entry e, int level) {
        Cost c = e.costs().get(0);
        int per = pestControlXpPerPoint(level, e.count());
        if (c.currency().equals(MinigameCurrencies.ZEAL)) {
            return per * c.amount();
        }
        double bonus = c.amount() >= 100 ? 1.10 : c.amount() >= 10 ? 1.01 : 1.0;
        return (int) Math.floor(per * c.amount() * bonus);
    }
}
