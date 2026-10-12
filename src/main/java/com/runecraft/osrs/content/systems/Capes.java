package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef.Slot;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ItemSink;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.runecraft.osrs.content.systems.Defs.req;
import static com.runecraft.osrs.content.systems.Defs.st;

/**
 * Skillcapes (+ trimmed), the max cape, quest point cape, achievement diary cape, diary rewards and other
 * module items that don't belong to a bigger table (lamps, ensouled heads, staves, teleport tablets).
 */
public final class Capes {
    /** Untrimmed skillcape (and quest point cape) bonuses: +9 to every defence, no prayer. */
    public static final EquipmentStats SKILLCAPE = st(0, 0, 0, 0, 0, 9, 9, 9, 9, 9, 0, 0, 0, 0);
    /** Trimmed skillcapes, the max cape and the achievement diary cape add +4 prayer. */
    public static final EquipmentStats SKILLCAPE_T = st(0, 0, 0, 0, 0, 9, 9, 9, 9, 9, 0, 0, 0, 4);
    public static final int SKILLCAPE_PRICE = 99_000;
    /** The max cape costs 99,000 coins per skill (2,277,000 for 23 skills in OSRS). */
    public static final int MAX_CAPE_PRICE_PER_SKILL = 99_000;

    private Capes() {
    }

    public static String capeId(Skill skill, boolean trimmed) {
        return skill.id() + "_cape" + (trimmed ? "_t" : "");
    }

    /** Teleports granted by a skillcape (OSRS perks where they are teleports). */
    public static List<Teleports.Destination> capeTeleports(Skill skill) {
        return switch (skill) {
            case CRAFTING -> List.of(Teleports.at("Crafting Guild", 2933, 3290));
            case FISHING -> List.of(Teleports.at("Fishing Guild", 2611, 3393), Teleports.at("Otto's Grotto", 2500, 3488));
            case FARMING -> List.of(Teleports.at("Farming Guild", 1249, 3718));
            case STRENGTH, ATTACK -> List.of(Teleports.at("Warriors' Guild", 2882, 3548));
            case HUNTER -> List.of(Teleports.at("Hunter Guild", 1559, 3049));
            case CONSTRUCTION -> List.of(Teleports.at("Rimmington", 2954, 3224), Teleports.at("Taverley", 2894, 3465),
                    Teleports.at("Pollnivneach", 3340, 3003), Teleports.at("Rellekka", 2670, 3631), Teleports.at("Brimhaven", 2758, 3178),
                    Teleports.at("Yanille", 2544, 3095), Teleports.at("Hosidius", 1744, 3517));
            case MAGIC -> List.of();
            case SLAYER -> List.of(Teleports.at("Slayer Tower", 3422, 3537), Teleports.at("Stronghold Slayer Cave", 2433, 3424));
            case RUNECRAFT -> List.of(Teleports.at("Ourania Altar", 2468, 3246));
            case COOKING -> List.of(Teleports.at("Cooks' Guild", 3143, 3442));
            case MINING -> List.of(Teleports.at("Mining Guild", 3016, 3339));
            case WOODCUTTING -> List.of(Teleports.at("Woodcutting Guild", 1662, 3505));
            case AGILITY -> List.of(Teleports.at("Agility Pyramid", 3354, 2830));
            case SAILING -> List.of(Teleports.at("Port Sarim", 3023, 3208));
            default -> List.of();
        };
    }

    private static int capeColour(Skill s) {
        return s.color();
    }

    public static void items(ItemSink items) {
        for (Skill s : Skill.values()) {
            String name = s.displayName() + " cape";
            Defs.worn(items, capeId(s, false), name, "cape", capeColour(s), SKILLCAPE_PRICE, Jewellery.Slot.CAPE, SKILLCAPE,
                    req(s, 99), Jewellery.Perk.NONE, capeTeleports(s)).examine("The cape worn by masters of " + s.displayName() + ".");
            Defs.worn(items, capeId(s, true), name + "(t)", "cape", capeColour(s), SKILLCAPE_PRICE, Jewellery.Slot.CAPE, SKILLCAPE_T,
                    req(s, 99), Jewellery.Perk.NONE, capeTeleports(s)).examine("The trimmed cape worn by masters of " + s.displayName() + ".");
        }
        List<Teleports.Destination> maxTeles = new ArrayList<>();
        for (Skill s : Skill.values()) {
            for (Teleports.Destination d : capeTeleports(s)) {
                if (maxTeles.stream().noneMatch(x -> x.name().equals(d.name()))) {
                    maxTeles.add(d);
                }
            }
        }
        Defs.worn(items, "max_cape", "Max cape", "cape", 0xC02020, 2_277_000, Jewellery.Slot.CAPE,
                SKILLCAPE_T, Map.of(), Jewellery.Perk.NONE, maxTeles)
                .examine("A cape worn by those who've achieved 99 in all skills.");
        Defs.worn(items, "quest_point_cape", "Quest point cape", "cape", 0x3A60C0, 99_000, Jewellery.Slot.CAPE, SKILLCAPE,
                Map.of(), Jewellery.Perk.NONE, List.of()).examine("The cape worn by those who've completed every quest.");
        List<Teleports.Destination> diaryTeles = new ArrayList<>();
        for (Diaries.Area a : Diaries.AREAS) {
            diaryTeles.add(Teleports.at(a.name(), a.x(), a.y()));
        }
        Defs.worn(items, "achievement_diary_cape", "Achievement diary cape", "cape", 0x8A6A3A, 99_000, Jewellery.Slot.CAPE, SKILLCAPE_T,
                Map.of(), Jewellery.Perk.NONE, diaryTeles).examine("A cape worn by those who've completed every achievement diary.");

        // ---------------- Achievement diary rewards ----------------
        for (Diaries.Area a : Diaries.AREAS) {
            for (Diaries.Tier t : Diaries.Tier.values()) {
                int n = t.ordinal() + 1;
                String id = a.reward(t);
                String name = a.rewardName() + " " + n;
                diaryReward(items, a.id(), id, name, n);
            }
        }
        for (Diaries.Tier t : Diaries.Tier.values()) {
            Defs.simple(items, "antique_lamp_" + t.id(), "Antique lamp (" + t.id() + " diary)", "lamp", 0xE8C030, 1).stack(1)
                    .examine("Rub it to gain " + String.format(Locale.ROOT, "%,d", t.lampXp) + " XP in a skill of level "
                            + t.lampMinLevel + " or higher.");
        }

        // ---------------- Arceuus: ensouled heads ----------------
        for (Spellbooks.Head h : Spellbooks.HEADS) {
            Defs.simple(items, h.id(), h.name(), "head", 0x7A6A5A, 300).stack(1)
                    .examine("Reanimate it with " + Spellbooks.get(h.spell()).name() + " for " + (int) h.xp() + " Prayer XP.");
        }
        // ---------------- Teleport tablets and scrolls ----------------
        for (Spellbooks.Tablet tab : Spellbooks.TABLETS) {
            boolean scroll = tab.value() >= 2500;
            Defs.simple(items, tab.id(), tab.name() + (scroll ? " scroll" : ""), scroll ? "note" : "tablet",
                    scroll ? 0xE8D8A0 : 0x9A8A6A, tab.value()).examine("Break it to teleport to " + tab.destination().name() + ".");
        }
        // ---------------- Staves for god spells / ancients ----------------
        Defs.staff(items, "saradomin_staff", "Saradomin staff", "staff_orb", 0xE0E0F0, 80000,
                st(2, -1, 9, 10, 0, 2, 3, 1, 10, 0, 6, 0, 0, 0), req(Skill.MAGIC, 60)).examine("A staff of Saradomin; casts Saradomin Strike.");
        Defs.staff(items, "guthix_staff", "Guthix staff", "staff_orb", 0x30A030, 80000,
                st(2, -1, 9, 10, 0, 2, 3, 1, 10, 0, 6, 0, 0, 0), req(Skill.MAGIC, 60)).examine("A staff of Guthix; casts Claws of Guthix.");
        Defs.staff(items, "zamorak_staff", "Zamorak staff", "staff_orb", 0xA02020, 80000,
                st(2, -1, 9, 10, 0, 2, 3, 1, 10, 0, 6, 0, 0, 0), req(Skill.MAGIC, 60)).examine("A staff of Zamorak; casts Flames of Zamorak.");
        // master_wand is defined by the minigames module (Mage Training Arena).
        Defs.staff(items, "ancient_sceptre", "Ancient sceptre", "staff_orb", 0x6A3A8A, 3_000_000,
                st(0, 0, 0, 20, 0, 0, 0, 0, 20, 0, 0, 0, 5, 0), req(Skill.MAGIC, 70)).examine("Boosts the power of Ancient Magicks.");
        for (String god : new String[]{"saradomin", "guthix", "zamorak"}) {
            int c = god.equals("saradomin") ? 0x3050C0 : god.equals("guthix") ? 0x30A030 : 0xA02020;
            Defs.worn(items, god + "_cape", Defs.cap(god) + " cape", "cape", c, 1000, Jewellery.Slot.CAPE,
                    st(0, 0, 0, 10, 0, 0, 0, 0, 10, 0, 0, 0, 0, 0), Map.of(), Jewellery.Perk.NONE, List.of())
                    .examine("A cape from the Mage Arena; empowers the god spells under Charge.");
        }
    }

    private static void diaryReward(ItemSink items, String area, String id, String name, int n) {
        List<Teleports.Destination> teles = DiaryRewards.destinations(area, n);
        List<String> perks = DiaryRewards.perks(area, n);
        String ex = "A reward for completing the " + Diaries.area(area).name() + " achievement diary."
                + (perks.isEmpty() ? "" : " " + String.join("; ", perks) + ".");
        switch (area) {
            case "ardougne" -> Defs.worn(items, id, name, "cape", 0x4A8A4A, 1000 * n, Jewellery.Slot.CAPE,
                    st(0, 0, 0, 0, 0, n, n, n, n, n, 0, 0, 0, n), Map.of(), Jewellery.Perk.NONE, teles).examine(ex);
            case "desert" -> Defs.worn(items, id, name, "amulet", 0xD8B060, 1000 * n, Jewellery.Slot.NECK, EquipmentStats.NONE,
                    Map.of(), Jewellery.Perk.NONE, teles).examine(ex);
            case "falador" -> Defs.armour(items, id, name, "kiteshield", 0xE0E0F0, "steel", 1000 * n, Slot.SHIELD,
                    st(0, 0, 0, 0, 0, 3 * n, 3 * n, 3 * n, 2 * n, 3 * n, 0, 0, 0, n), Map.of()).examine(ex + " Right-click to recharge prayer.");
            case "fremennik" -> Defs.armour(items, id, name, "boots", 0x6A4A2A, "leather", 1000 * n, Slot.FEET,
                    st(0, 0, 0, 0, 0, n, n, n, n, n, 0, 0, 0, 0), Map.of()).examine(ex + (teles.isEmpty() ? "" : " Sneak + right-click to teleport."));
            case "kandarin" -> Defs.armour(items, id, name, "full_helm", 0x3A5A8A, "steel", 1000 * n, Slot.HEAD,
                    st(0, 0, 0, 0, 0, 2 * n, 2 * n, 2 * n, n, 2 * n, 0, 0, 0, 0), Map.of()).examine(ex + (teles.isEmpty() ? "" : " Sneak + right-click to teleport."));
            case "karamja" -> Defs.worn(items, id, name, "gloves", 0x3A8A3A, 1000 * n, Jewellery.Slot.HANDS,
                    st(n, n, n, 0, n, n, n, n, 0, n, n, 0, 0, 0), Map.of(), Jewellery.Perk.NONE, teles).examine(ex);
            case "kourend" -> Defs.worn(items, id, name, "bead", 0xA0C0E0, 1000 * n, Jewellery.Slot.AMMO,
                    st(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, n == 4 ? 2 : 1), Map.of(), Jewellery.Perk.NONE, teles).examine(ex);
            case "lumbridge" -> Defs.worn(items, id, name, "ring", 0x8A6A3A, 1000 * n, Jewellery.Slot.RING, EquipmentStats.NONE,
                    Map.of(), Jewellery.Perk.NONE, teles).examine(ex + " Rub it to restore run energy or alchemise items.");
            case "morytania" -> Defs.armour(items, id, name, "platelegs", 0x5A3A6A, "steel", 1000 * n, Slot.LEGS,
                    st(0, 0, 0, 0, 0, 2 * n, 2 * n, 2 * n, n, 2 * n, 0, 0, 0, 0), Map.of()).examine(ex + (teles.isEmpty() ? "" : " Sneak + right-click to teleport."));
            case "varrock" -> Defs.armour(items, id, name, "platebody", 0x8A8A9A, "steel", 1000 * n, Slot.CHEST,
                    st(0, 0, 0, -10, -5, 10 * n, 10 * n, 9 * n, -2, 10 * n, 0, 0, 0, 0), Map.of()).examine(ex);
            case "western" -> Defs.worn(items, id, name, "cape", 0x6A8A3A, 1000 * n, Jewellery.Slot.CAPE,
                    st(0, 0, 0, 0, 0, n, n, n, n, n, 0, 0, 0, 0), Map.of(), Jewellery.Perk.NONE, teles).examine(ex);
            default -> Defs.melee(items, id, name, "longsword", 0x8A2020, "steel", 1000 * n,
                    st(4 * n, 8 * n, -2, 0, 0, 0, 2, 1, 0, 0, 7 * n, 0, 0, 0), Map.of(), 4, AttackType.SLASH)
                    .examine(ex + (teles.isEmpty() ? "" : " Sneak + right-click to teleport."));
        }
    }
}
