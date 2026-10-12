package com.runecraft.osrs.content.minigames;

import java.util.LinkedHashMap;
import java.util.Map;

/** Points balances kept by the minigames module (item currencies are just items). */
public final class MinigameCurrencies {
    public static final String PEST_CONTROL = "pts:pest_control";
    public static final String BA_ATTACKER = "pts:ba_attacker";
    public static final String BA_DEFENDER = "pts:ba_defender";
    public static final String BA_COLLECTOR = "pts:ba_collector";
    public static final String BA_HEALER = "pts:ba_healer";
    public static final String MTA_TELEKINETIC = "pts:mta_telekinetic";
    public static final String MTA_ALCHEMIST = "pts:mta_alchemist";
    public static final String MTA_ENCHANTMENT = "pts:mta_enchantment";
    public static final String MTA_GRAVEYARD = "pts:mta_graveyard";
    public static final String TITHE = "pts:tithe_farm";
    public static final String FOUNDRY = "pts:giants_foundry";
    public static final String CARPENTER = "pts:mahogany_homes";
    public static final String VOLCANIC_MINE = "pts:volcanic_mine";
    public static final String TOKKUL = "item:tokkul";
    public static final String PEARLS = "item:abyssal_pearls";
    public static final String CW_TICKETS = "item:castle_wars_ticket";
    public static final String WG_TOKENS = "item:warrior_guild_token";
    public static final String MARKS = "item:hallowed_mark";
    public static final String ARENA_TICKETS = "item:agility_arena_ticket";
    public static final String EIGHT = "item:pieces_of_eight";
    public static final String PERMITS = "item:reward_permit";
    public static final String GNOME_TOKENS = "item:gnome_restaurant_token";
    public static final String COINS = "item:coins";

    private static final Map<String, String> NAMES = new LinkedHashMap<>();

    static {
        NAMES.put(PEST_CONTROL, "Commendation points");
        NAMES.put(BA_ATTACKER, "Attacker honour points");
        NAMES.put(BA_DEFENDER, "Defender honour points");
        NAMES.put(BA_COLLECTOR, "Collector honour points");
        NAMES.put(BA_HEALER, "Healer honour points");
        NAMES.put(MTA_TELEKINETIC, "Telekinetic pizazz points");
        NAMES.put(MTA_ALCHEMIST, "Alchemist pizazz points");
        NAMES.put(MTA_ENCHANTMENT, "Enchantment pizazz points");
        NAMES.put(MTA_GRAVEYARD, "Graveyard pizazz points");
        NAMES.put(TITHE, "Tithe Farm points");
        NAMES.put(FOUNDRY, "Foundry reputation");
        NAMES.put(CARPENTER, "Carpenter points");
        NAMES.put(VOLCANIC_MINE, "Volcanic Mine points");
        NAMES.put(TOKKUL, "Tokkul");
        NAMES.put(PEARLS, "Abyssal pearls");
        NAMES.put(CW_TICKETS, "Castle wars tickets");
        NAMES.put(WG_TOKENS, "Warrior guild tokens");
        NAMES.put(MARKS, "Hallowed marks");
        NAMES.put(ARENA_TICKETS, "Agility arena tickets");
        NAMES.put(EIGHT, "Pieces of eight");
        NAMES.put(PERMITS, "Reward permits");
        NAMES.put(GNOME_TOKENS, "Reward tokens");
        NAMES.put(COINS, "Coins");
    }

    private MinigameCurrencies() {
    }

    public static String name(String currency) {
        return NAMES.getOrDefault(currency, currency);
    }

    public static Map<String, String> all() {
        return NAMES;
    }

    /** Points key (without prefix) for a "pts:" currency. */
    public static String key(String currency) {
        return currency.substring(currency.indexOf(':') + 1);
    }
}
