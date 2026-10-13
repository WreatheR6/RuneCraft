package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.Skill;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A minigame reward shop. Each entry costs one or more currencies; a currency is either an item
 * ({@code item:tokkul}) or a points balance kept by the minigames module ({@code pts:pest_control}).
 * Entries give items, skill experience (Pest Control style) or unlock a flag (Bones to Peaches).
 */
public final class RewardShop {
    public enum Reward { ITEM, XP, UNLOCK }

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

    RewardShop itemMulti(String item, List<Cost> costs) {
        entries.add(new Entry(Reward.ITEM, item, 1, List.copyOf(costs), null, null, 0, null));
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
}
