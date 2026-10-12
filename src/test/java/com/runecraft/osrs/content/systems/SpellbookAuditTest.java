package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.Rune;
import com.runecraft.osrs.Spell;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Completeness audit of all four OSRS spellbooks against the OSRS wiki spell tables. Every OSRS spell must exist
 * (core {@link Spell} enum or {@link Spellbooks}) with the wiki's level, base XP and runes, or be listed in
 * {@link Spellbooks#UNIMPLEMENTED} with a reason.
 */
class SpellbookAuditTest {
    /** book | name | level | xp | runes ("air 1, mind 1"); "-" for no runes. */
    private static final String[] WIKI = {
            // ---------------- Standard ----------------
            "STANDARD|Lumbridge Home Teleport|1|0|-",
            "STANDARD|Wind Strike|1|5.5|air 1, mind 1",
            "STANDARD|Confuse|3|13|water 3, earth 2, body 1",
            "STANDARD|Enchant Crossbow Bolt|4|9|cosmic 1, air 2",
            "STANDARD|Water Strike|5|7.5|water 1, air 1, mind 1",
            "STANDARD|Lvl-1 Enchant|7|17.5|cosmic 1, water 1",
            "STANDARD|Earth Strike|9|9.5|earth 2, air 1, mind 1",
            "STANDARD|Weaken|11|21|water 3, earth 2, body 1",
            "STANDARD|Fire Strike|13|11.5|fire 3, air 2, mind 1",
            "STANDARD|Bones to Bananas|15|25|earth 2, water 2, nature 1",
            "STANDARD|Wind Bolt|17|13.5|air 2, chaos 1",
            "STANDARD|Curse|19|29|water 2, earth 3, body 1",
            "STANDARD|Bind|20|30|earth 3, water 3, nature 2",
            "STANDARD|Low Level Alchemy|21|31|fire 3, nature 1",
            "STANDARD|Water Bolt|23|16.5|water 2, air 2, chaos 1",
            "STANDARD|Varrock Teleport|25|35|fire 1, air 3, law 1",
            "STANDARD|Lvl-2 Enchant|27|37|cosmic 1, air 3",
            "STANDARD|Earth Bolt|29|19.5|earth 3, air 2, chaos 1",
            "STANDARD|Lumbridge Teleport|31|41|earth 1, air 3, law 1",
            "STANDARD|Telekinetic Grab|33|43|air 1, law 1",
            "STANDARD|Fire Bolt|35|22.5|fire 4, air 3, chaos 1",
            "STANDARD|Falador Teleport|37|48|water 1, air 3, law 1",
            "STANDARD|Crumble Undead|39|24.5|earth 2, air 2, chaos 1",
            "STANDARD|Teleport to House|40|30|law 1, air 1, earth 1",
            "STANDARD|Wind Blast|41|25.5|air 3, death 1",
            "STANDARD|Superheat Item|43|53|fire 4, nature 1",
            "STANDARD|Camelot Teleport|45|55.5|air 5, law 1",
            "STANDARD|Water Blast|47|28.5|water 3, air 3, death 1",
            "STANDARD|Lvl-3 Enchant|49|59|cosmic 1, fire 5",
            "STANDARD|Iban Blast|50|30|death 1, fire 5",
            "STANDARD|Snare|50|60|nature 3, earth 4, water 4",
            "STANDARD|Magic Dart|50|30|death 1, mind 4",
            "STANDARD|Ardougne Teleport|51|61|water 2, law 2",
            "STANDARD|Earth Blast|53|31.5|earth 4, air 3, death 1",
            "STANDARD|Civitas illa Fortis Teleport|54|64|law 2, earth 1, fire 1",
            "STANDARD|High Level Alchemy|55|65|fire 5, nature 1",
            "STANDARD|Charge Water Orb|56|66|cosmic 3, water 30",
            "STANDARD|Lvl-4 Enchant|57|67|cosmic 1, earth 10",
            "STANDARD|Watchtower Teleport|58|68|earth 2, law 2",
            "STANDARD|Fire Blast|59|34.5|fire 5, air 4, death 1",
            "STANDARD|Charge Earth Orb|60|70|cosmic 3, earth 30",
            "STANDARD|Bones to Peaches|60|35.5|nature 2, water 4, earth 4",
            "STANDARD|Saradomin Strike|60|35|blood 2, fire 2, air 4",
            "STANDARD|Claws of Guthix|60|35|blood 2, fire 1, air 4",
            "STANDARD|Flames of Zamorak|60|35|blood 2, fire 4, air 1",
            "STANDARD|Trollheim Teleport|61|68|fire 2, law 2",
            "STANDARD|Wind Wave|62|36|air 5, blood 1",
            "STANDARD|Charge Fire Orb|63|73|cosmic 3, fire 30",
            "STANDARD|Ape Atoll Teleport|64|74|law 2, fire 2, water 2",
            "STANDARD|Water Wave|65|37.5|water 7, air 5, blood 1",
            "STANDARD|Charge Air Orb|66|76|cosmic 3, air 30",
            "STANDARD|Vulnerability|66|76|soul 1, earth 5, water 5",
            "STANDARD|Lvl-5 Enchant|68|78|cosmic 1, earth 15, water 15",
            "STANDARD|Kourend Castle Teleport|69|82|soul 2, law 2, water 4, fire 5",
            "STANDARD|Earth Wave|70|40|earth 7, air 5, blood 1",
            "STANDARD|Enfeeble|73|83|soul 1, earth 8, water 8",
            "STANDARD|Fire Wave|75|42.5|fire 7, air 5, blood 1",
            "STANDARD|Entangle|79|89|nature 4, earth 5, water 5",
            "STANDARD|Stun|80|90|soul 1, earth 12, water 12",
            "STANDARD|Charge|80|180|blood 3, fire 3, air 3",
            "STANDARD|Wind Surge|81|44.5|air 7, wrath 1",
            "STANDARD|Tele Block|85|80|law 1, chaos 1, death 1",
            "STANDARD|Teleport to Target|85|45|law 1, chaos 1, death 1",
            "STANDARD|Water Surge|85|46.5|water 10, air 7, wrath 1",
            "STANDARD|Lvl-6 Enchant|87|97|cosmic 1, earth 20, fire 20",
            "STANDARD|Earth Surge|90|48.5|earth 10, air 7, wrath 1",
            "STANDARD|Lvl-7 Enchant|93|110|cosmic 1, blood 20, soul 20",
            "STANDARD|Fire Surge|95|50.5|fire 10, air 7, wrath 1",
            // ---------------- Ancient Magicks ----------------
            "ANCIENT|Edgeville Home Teleport|1|0|-",
            "ANCIENT|Smoke Rush|50|30|chaos 2, death 2, fire 1, air 1",
            "ANCIENT|Shadow Rush|52|31|chaos 2, death 2, air 1, soul 1",
            "ANCIENT|Paddewwa Teleport|54|64|law 2, air 1, fire 1",
            "ANCIENT|Blood Rush|56|33|blood 1, death 2, chaos 2",
            "ANCIENT|Ice Rush|58|34|water 2, death 2, chaos 2",
            "ANCIENT|Senntisten Teleport|60|70|law 2, soul 1",
            "ANCIENT|Smoke Burst|62|36|chaos 4, death 2, fire 2, air 2",
            "ANCIENT|Shadow Burst|64|37|chaos 4, death 2, air 1, soul 2",
            "ANCIENT|Kharyrll Teleport|66|76|law 2, blood 1",
            "ANCIENT|Blood Burst|68|39|chaos 4, death 2, blood 2",
            "ANCIENT|Ice Burst|70|40|water 4, chaos 4, death 2",
            "ANCIENT|Lassar Teleport|72|82|law 2, water 4",
            "ANCIENT|Smoke Blitz|74|42|blood 2, death 2, fire 2, air 2",
            "ANCIENT|Shadow Blitz|76|43|blood 2, death 2, air 2, soul 2",
            "ANCIENT|Dareeyak Teleport|78|88|law 2, air 2, fire 3",
            "ANCIENT|Blood Blitz|80|45|blood 4, death 2",
            "ANCIENT|Ice Blitz|82|46|blood 2, death 2, water 3",
            "ANCIENT|Carrallanger Teleport|84|94|law 2, soul 2",
            "ANCIENT|Smoke Barrage|86|48|blood 2, death 4, fire 4, air 4",
            "ANCIENT|Shadow Barrage|88|48|blood 2, death 4, air 4, soul 3",
            "ANCIENT|Annakarl Teleport|90|100|law 2, blood 2",
            "ANCIENT|Blood Barrage|92|51|blood 4, death 4, soul 1",
            "ANCIENT|Ice Barrage|94|52|blood 2, death 4, water 6",
            "ANCIENT|Ghorrock Teleport|96|106|law 2, water 8",
            // ---------------- Lunar ----------------
            "LUNAR|Lunar Home Teleport|1|0|-",
            "LUNAR|Bake Pie|65|60|astral 1, fire 5, water 4",
            "LUNAR|Geomancy|65|60|astral 2, earth 8, nature 3",
            "LUNAR|Cure Plant|66|60|astral 1, earth 8",
            "LUNAR|Monster Examine|66|61|astral 1, cosmic 1, mind 1",
            "LUNAR|NPC Contact|67|63|astral 1, cosmic 1, air 2",
            "LUNAR|Cure Other|68|65|astral 1, law 1, earth 10",
            "LUNAR|Humidify|68|65|astral 1, water 3, fire 1",
            "LUNAR|Moonclan Teleport|69|66|astral 2, law 1, earth 2",
            "LUNAR|Tele Group Moonclan|70|67|astral 2, law 1, earth 4",
            "LUNAR|Cure Me|71|69|astral 2, cosmic 2, law 1",
            "LUNAR|Ourania Teleport|71|69|astral 2, law 1, earth 6",
            "LUNAR|Hunter Kit|71|70|astral 2, earth 2",
            "LUNAR|Waterbirth Teleport|72|71|astral 2, law 1, water 1",
            "LUNAR|Tele Group Waterbirth|73|72|astral 2, law 1, water 5",
            "LUNAR|Cure Group|74|74|astral 2, cosmic 2, law 2",
            "LUNAR|Stat Spy|75|76|astral 2, cosmic 2, body 5",
            "LUNAR|Barbarian Teleport|75|76|astral 2, law 2, fire 3",
            "LUNAR|Tele Group Barbarian|76|77|astral 2, law 2, fire 6",
            "LUNAR|Superglass Make|77|78|astral 2, fire 6, air 10",
            "LUNAR|Tan Leather|78|81|astral 2, fire 5, nature 1",
            "LUNAR|Khazard Teleport|78|80|astral 2, law 2, water 4",
            "LUNAR|Tele Group Khazard|79|81|astral 2, law 2, water 8",
            "LUNAR|Dream|79|82|astral 2, cosmic 1, body 5",
            "LUNAR|String Jewellery|80|83|astral 2, earth 10, water 5",
            "LUNAR|Stat Restore Pot Share|81|84|astral 2, water 10, earth 10",
            "LUNAR|Magic Imbue|82|86|astral 2, fire 7, water 7",
            "LUNAR|Fertile Soil|83|87|astral 3, nature 2, earth 15",
            "LUNAR|Boost Potion Share|84|88|astral 3, water 10, earth 12",
            "LUNAR|Fishing Guild Teleport|85|89|astral 3, law 3, water 10",
            "LUNAR|Tele Group Fishing Guild|86|90|astral 3, law 3, water 14",
            "LUNAR|Plank Make|86|90|astral 2, nature 1, earth 15",
            "LUNAR|Catherby Teleport|87|92|astral 3, law 3, water 10",
            "LUNAR|Tele Group Catherby|88|93|astral 3, law 3, water 15",
            "LUNAR|Recharge Dragonstone|89|97.5|astral 1, water 4, soul 1",
            "LUNAR|Ice Plateau Teleport|89|96|astral 3, law 3, water 8",
            "LUNAR|Tele Group Ice Plateau|90|99|astral 3, law 3, water 16",
            "LUNAR|Energy Transfer|91|100|astral 3, law 2, nature 1",
            "LUNAR|Heal Other|92|101|astral 3, law 3, blood 1",
            "LUNAR|Vengeance Other|93|108|astral 3, death 2, earth 10",
            "LUNAR|Vengeance|94|112|astral 4, death 2, earth 10",
            "LUNAR|Heal Group|95|124|astral 4, law 6, blood 3",
            "LUNAR|Spellbook Swap|96|130|astral 3, cosmic 2, law 1",
            // ---------------- Arceuus ----------------
            "ARCEUUS|Arceuus Home Teleport|1|0|-",
            "ARCEUUS|Arceuus Library Teleport|6|10|law 1, earth 2",
            "ARCEUUS|Basic Reanimation|16|32|body 4, nature 2",
            "ARCEUUS|Draynor Manor Teleport|17|16|law 1, earth 1, water 1",
            "ARCEUUS|Battlefront Teleport|23|19|law 1, earth 1, fire 1",
            "ARCEUUS|Mind Altar Teleport|28|22|law 1, mind 1",
            "ARCEUUS|Respawn Teleport|34|27|law 1, soul 1",
            "ARCEUUS|Ghostly Grasp|35|22.5|chaos 1, air 4",
            "ARCEUUS|Resurrect Lesser Skeleton|38|55|air 10, cosmic 1, mind 5",
            "ARCEUUS|Resurrect Lesser Zombie|38|55|air 10, cosmic 1, mind 5",
            "ARCEUUS|Resurrect Lesser Ghost|38|55|air 10, cosmic 1, mind 5",
            "ARCEUUS|Salve Graveyard Teleport|40|30|law 1, soul 2",
            "ARCEUUS|Adept Reanimation|41|80|body 4, nature 3, soul 1",
            "ARCEUUS|Inferior Demonbane|44|27|fire 4, chaos 1",
            "ARCEUUS|Shadow Veil|47|58|earth 5, fire 5, cosmic 5",
            "ARCEUUS|Fenkenstrain's Castle Teleport|48|50|law 1, earth 1, soul 1",
            "ARCEUUS|Dark Lure|50|60|death 1, nature 1",
            "ARCEUUS|Skeletal Grasp|56|33|death 1, earth 8",
            "ARCEUUS|Resurrect Superior Skeleton|57|70|earth 10, cosmic 1, death 5",
            "ARCEUUS|Resurrect Superior Zombie|57|70|earth 10, cosmic 1, death 5",
            "ARCEUUS|Resurrect Superior Ghost|57|70|earth 10, cosmic 1, death 5",
            "ARCEUUS|Mark of Darkness|59|70|cosmic 1, soul 1",
            "ARCEUUS|West Ardougne Teleport|61|68|law 2, soul 2",
            "ARCEUUS|Superior Demonbane|62|36|fire 5, soul 1",
            "ARCEUUS|Lesser Corruption|64|75|death 1, soul 2",
            "ARCEUUS|Harmony Island Teleport|65|74|law 1, nature 1, soul 1",
            "ARCEUUS|Vile Vigour|66|76|air 3, soul 1",
            "ARCEUUS|Degrime|70|83|earth 4, nature 2",
            "ARCEUUS|Cemetery Teleport|71|82|blood 1, law 1, soul 1",
            "ARCEUUS|Expert Reanimation|72|138|blood 1, nature 3, soul 2",
            "ARCEUUS|Ward of Arceuus|73|83|cosmic 1, nature 2, soul 4",
            "ARCEUUS|Resurrect Greater Skeleton|76|88|fire 10, cosmic 1, blood 5",
            "ARCEUUS|Resurrect Greater Zombie|76|88|fire 10, cosmic 1, blood 5",
            "ARCEUUS|Resurrect Greater Ghost|76|88|fire 10, cosmic 1, blood 5",
            "ARCEUUS|Resurrect Crops|78|90|earth 25, blood 8, nature 12, soul 8",
            "ARCEUUS|Undead Grasp|79|46.5|blood 1, fire 4",
            "ARCEUUS|Death Charge|80|90|blood 1, death 1, soul 1",
            "ARCEUUS|Dark Demonbane|82|43.5|fire 6, soul 2",
            "ARCEUUS|Barrows Teleport|83|90|blood 1, law 2, soul 2",
            "ARCEUUS|Demonic Offering|84|175|soul 1, wrath 1",
            "ARCEUUS|Greater Corruption|85|95|blood 1, soul 3",
            "ARCEUUS|Master Reanimation|90|170|blood 2, nature 4, soul 4",
            "ARCEUUS|Ape Atoll Teleport|90|100|blood 2, law 2, soul 2",
            "ARCEUUS|Sinister Offering|92|180|blood 1, wrath 1",
    };

    /** name -> max hit for every damaging OSRS combat spell (wiki). */
    private static final Map<String, Integer> MAX_HITS = new LinkedHashMap<>();

    static {
        String[] hits = {"Wind Strike 2", "Water Strike 4", "Earth Strike 6", "Fire Strike 8", "Wind Bolt 9", "Water Bolt 10",
                "Earth Bolt 11", "Fire Bolt 12", "Wind Blast 13", "Water Blast 14", "Earth Blast 15", "Fire Blast 16",
                "Wind Wave 17", "Water Wave 18", "Earth Wave 19", "Fire Wave 20", "Wind Surge 21", "Water Surge 22",
                "Earth Surge 23", "Fire Surge 24", "Crumble Undead 15", "Iban Blast 25", "Snare 3", "Entangle 5",
                "Saradomin Strike 20", "Claws of Guthix 20", "Flames of Zamorak 20",
                "Smoke Rush 13", "Shadow Rush 14", "Blood Rush 15", "Ice Rush 16", "Smoke Burst 17", "Shadow Burst 18",
                "Blood Burst 21", "Ice Burst 22", "Smoke Blitz 23", "Shadow Blitz 24", "Blood Blitz 25", "Ice Blitz 26",
                "Smoke Barrage 27", "Shadow Barrage 28", "Blood Barrage 29", "Ice Barrage 30",
                "Ghostly Grasp 12", "Skeletal Grasp 17", "Undead Grasp 24", "Inferior Demonbane 16", "Superior Demonbane 23",
                "Dark Demonbane 30"};
        for (String h : hits) {
            int sp = h.lastIndexOf(' ');
            MAX_HITS.put(h.substring(0, sp), Integer.parseInt(h.substring(sp + 1)));
        }
    }

    private record Found(String name, int level, double xp, Map<String, Integer> runes, int maxHit) {
    }

    private static Found find(String book, String name) {
        if (book.equals("STANDARD")) {
            for (Spell s : Spell.values()) {
                if (s.displayName().equals(name)) {
                    Map<String, Integer> runes = new LinkedHashMap<>();
                    for (Map.Entry<Rune, Integer> e : s.runes().entrySet()) {
                        runes.put(e.getKey().itemId(), e.getValue());
                    }
                    return new Found(name, s.level(), s.xpTenths() / 10.0, runes, s.maxHit());
                }
            }
        }
        for (Spellbooks.Spell s : Spellbooks.all()) {
            if (s.book().name().equals(book) && s.name().equals(name)) {
                return new Found(name, s.level(), s.xp(), s.runes(), s.maxHit());
            }
        }
        return null;
    }

    private static Map<String, Integer> runes(String spec) {
        Map<String, Integer> m = new LinkedHashMap<>();
        if (spec.equals("-")) {
            return m;
        }
        for (String part : spec.split(", ")) {
            String[] kv = part.split(" ");
            m.put(kv[0] + "_rune", Integer.parseInt(kv[1]));
        }
        return m;
    }

    @Test
    void everyOsrsSpellExistsWithWikiLevelXpAndRunes() {
        List<String> problems = new ArrayList<>();
        int implemented = 0;
        for (String row : WIKI) {
            String[] f = row.split("\\|");
            String book = f[0];
            String name = f[1];
            Found s = find(book, name);
            if (s == null) {
                if (!Spellbooks.UNIMPLEMENTED.containsKey(name)) {
                    problems.add(book + " " + name + ": missing and not listed in Spellbooks.UNIMPLEMENTED");
                }
                continue;
            }
            implemented++;
            if (Spellbooks.UNIMPLEMENTED.containsKey(name)) {
                problems.add(name + ": implemented but still listed as unimplemented");
            }
            if (s.level() != Integer.parseInt(f[2])) {
                problems.add(name + ": level " + s.level() + " != " + f[2]);
            }
            if (Math.abs(s.xp() - Double.parseDouble(f[3])) > 1e-6) {
                problems.add(name + ": xp " + s.xp() + " != " + f[3]);
            }
            if (!s.runes().equals(runes(f[4]))) {
                problems.add(name + ": runes " + s.runes() + " != " + runes(f[4]));
            }
            Integer max = MAX_HITS.get(name);
            if (max != null && s.maxHit() != max) {
                problems.add(name + ": max hit " + s.maxHit() + " != " + max);
            }
        }
        if (!problems.isEmpty()) {
            fail(String.join("\n", problems));
        }
        assertEquals(WIKI.length - Spellbooks.UNIMPLEMENTED.size(), implemented);
    }

    @Test
    void unimplementedSpellsAreRealOsrsSpellsWithReasons() {
        for (Map.Entry<String, String> e : Spellbooks.UNIMPLEMENTED.entrySet()) {
            boolean listed = false;
            for (String row : WIKI) {
                listed |= row.split("\\|")[1].equals(e.getKey());
            }
            assertTrue(listed, e.getKey());
            assertTrue(e.getValue().length() > 10, e.getKey());
        }
    }

    @Test
    void noSpellOutsideTheWikiTables() {
        for (Spellbooks.Spell s : Spellbooks.all()) {
            boolean listed = false;
            for (String row : WIKI) {
                String[] f = row.split("\\|");
                listed |= f[0].equals(s.book().name()) && f[1].equals(s.name());
            }
            assertTrue(listed, s.book() + " " + s.name() + " is not an OSRS spell (or the audit table is out of date)");
        }
    }

    /** OSRS arrival tiles (wiki) for every teleport spell; the mod's destination must be within 8 tiles. */
    @Test
    void teleportsLandAtTheirOsrsDestination() {
        Object[][] tiles = {
                {"paddewwa_teleport", 3098, 3480}, {"senntisten_teleport", 3322, 3336}, {"kharyrll_teleport", 3492, 3471},
                {"lassar_teleport", 3006, 3471}, {"dareeyak_teleport", 2966, 3696}, {"carrallanger_teleport", 3157, 3666},
                {"annakarl_teleport", 3288, 3886}, {"ghorrock_teleport", 2977, 3873},
                {"moonclan_teleport", 2113, 3917}, {"ourania_teleport", 2468, 3246}, {"waterbirth_teleport", 2546, 3757},
                {"barbarian_teleport", 2543, 3568}, {"khazard_teleport", 2636, 3167}, {"fishing_guild_teleport", 2611, 3393},
                {"catherby_teleport", 2804, 3433}, {"ice_plateau_teleport", 2973, 3939},
                {"arceuus_library_teleport", 1632, 3838}, {"draynor_manor_teleport", 3108, 3352},
                {"battlefront_teleport", 1349, 3739}, {"mind_altar_teleport", 2979, 3509},
                {"salve_graveyard_teleport", 3432, 3460}, {"fenkenstrains_castle_teleport", 3548, 3528},
                {"west_ardougne_teleport", 2500, 3290}, {"harmony_island_teleport", 3797, 2866},
                {"cemetery_teleport", 2978, 3763}, {"barrows_teleport", 3565, 3315},
                {"kourend_castle_teleport", 1643, 3673}, {"civitas_illa_fortis_teleport", 1681, 3133}};
        for (Object[] t : tiles) {
            Spellbooks.Spell s = Spellbooks.get((String) t[0]);
            assertNotNull(s, (String) t[0]);
            assertTrue(Math.abs(s.destination().x() - (int) t[1]) <= 8 && Math.abs(s.destination().y() - (int) t[2]) <= 8,
                    t[0] + " lands at " + s.destination());
        }
        // Every Tele Group spell goes where its single-target version does.
        for (Spellbooks.Spell s : Spellbooks.book(Spellbooks.Book.LUNAR)) {
            if (Spellbooks.isGroupTeleport(s)) {
                String single = s.id().replace("tele_group_", "") + "_teleport";
                assertEquals(Spellbooks.get(single).destination(), s.destination(), s.id());
            }
        }
        // Core standard teleports (wiki arrival tiles; Watchtower lands on the tower, not in Yanille).
        assertEquals(2549, Spell.WATCHTOWER_TELEPORT.tile()[0]);
        assertEquals(3112, Spell.WATCHTOWER_TELEPORT.tile()[1]);
        assertEquals(com.runecraft.osrs.Landmark.VARROCK, Spell.VARROCK_TELEPORT.destination());
        assertEquals(com.runecraft.osrs.Landmark.CAMELOT, Spell.CAMELOT_TELEPORT.destination());
        assertEquals(com.runecraft.osrs.Landmark.TROLLHEIM, Spell.TROLLHEIM_TELEPORT.destination());
    }

    @Test
    void freezeAndBindDurations() {
        // Ice: rush 5 s, burst 10 s, blitz 15 s, barrage 20 s. Bind 5 s, Snare 10 s, Entangle 15 s.
        assertEquals(8, Spellbooks.freezeTicks(Spellbooks.get("ice_rush")));
        assertEquals(16, Spellbooks.freezeTicks(Spellbooks.get("ice_burst")));
        assertEquals(24, Spellbooks.freezeTicks(Spellbooks.get("ice_blitz")));
        assertEquals(32, Spellbooks.freezeTicks(Spellbooks.get("ice_barrage")));
        assertEquals(8, Spellbooks.freezeTicks(Spellbooks.get("bind")));
        assertEquals(16, Spellbooks.freezeTicks(Spellbooks.get("snare")));
        assertEquals(24, Spellbooks.freezeTicks(Spellbooks.get("entangle")));
        assertTrue(Spellbooks.get("ice_burst").area() && Spellbooks.get("ice_barrage").area());
        assertTrue(!Spellbooks.get("ice_rush").area() && !Spellbooks.get("ice_blitz").area());
    }

    @Test
    void ancientSecondaryEffects() {
        // Blood spells heal 25% of the damage dealt; smoke poisons (2 rush/burst, 4 blitz/barrage);
        // shadow lowers Attack (10% rush/burst, 15% blitz/barrage).
        for (String id : new String[]{"blood_rush", "blood_burst", "blood_blitz", "blood_barrage"}) {
            assertEquals(Spellbooks.Effect.HEAL, Spellbooks.get(id).effect(), id);
            assertEquals(25, Spellbooks.get(id).param(), id);
        }
        assertEquals(2, Spellbooks.get("smoke_rush").param());
        assertEquals(2, Spellbooks.get("smoke_burst").param());
        assertEquals(4, Spellbooks.get("smoke_blitz").param());
        assertEquals(4, Spellbooks.get("smoke_barrage").param());
        assertEquals(10, Spellbooks.get("shadow_rush").param());
        assertEquals(10, Spellbooks.get("shadow_burst").param());
        assertEquals(15, Spellbooks.get("shadow_blitz").param());
        assertEquals(15, Spellbooks.get("shadow_barrage").param());
        for (String id : new String[]{"shadow_rush", "shadow_burst", "shadow_blitz", "shadow_barrage"}) {
            assertEquals(Spellbooks.Effect.DRAIN_ATTACK, Spellbooks.get(id).effect(), id);
        }
    }

    @Test
    void lunarUtilityRules() {
        assertTrue(Spellbooks.canShare("stat_restore_pot_share", "super_restore"));
        assertTrue(Spellbooks.canShare("stat_restore_pot_share", "prayer_potion"));
        assertTrue(!Spellbooks.canShare("stat_restore_pot_share", "super_strength"));
        assertTrue(Spellbooks.canShare("boost_potion_share", "ranging_potion"));
        assertTrue(!Spellbooks.canShare("boost_potion_share", "saradomin_brew"));
        for (String id : Spellbooks.RESTORE_SHARE) {
            assertNotNull(com.runecraft.osrs.ItemCatalog.get(id), id);
        }
        for (String id : Spellbooks.BOOST_SHARE) {
            assertNotNull(com.runecraft.osrs.ItemCatalog.get(id), id);
        }
        assertEquals(7, Spellbooks.book(Spellbooks.Book.LUNAR).stream().filter(Spellbooks::isGroupTeleport).count());
        assertTrue(!Spellbooks.isGroupTeleport(Spellbooks.get("moonclan_teleport")));
        // Utility spells whose effect lives in Spellbooks / the spell service all have a description.
        for (Spellbooks.Spell s : Spellbooks.all()) {
            assertTrue(s.description() != null && !s.description().isBlank(), s.id());
        }
    }

    @Test
    void arceuusMechanics() {
        assertEquals(1, Spellbooks.thrallMaxHit(1));
        assertEquals(2, Spellbooks.thrallMaxHit(2));
        assertEquals(3, Spellbooks.thrallMaxHit(Spellbooks.thrallTier("resurrect_greater_zombie")));
        assertEquals(0.50, Spellbooks.resurrectCropsChance(78), 1e-9);
        assertEquals(0.75, Spellbooks.resurrectCropsChance(99), 1e-9);
        assertEquals(0.75, Spellbooks.resurrectCropsChance(110), 1e-9);
        assertEquals(0.50, Spellbooks.resurrectCropsChance(60), 1e-9);
        // Lesser corruption drains 1/2/3 (6 total), greater 2/4/6 (12) every 10 ticks (5 with Mark of Darkness).
        assertEquals(6, Spellbooks.corruptionDrain(false, 0) + Spellbooks.corruptionDrain(false, 1) + Spellbooks.corruptionDrain(false, 2));
        assertEquals(12, Spellbooks.corruptionDrain(true, 0) + Spellbooks.corruptionDrain(true, 1) + Spellbooks.corruptionDrain(true, 2));
        assertEquals(10, Spellbooks.corruptionInterval(false));
        assertEquals(5, Spellbooks.corruptionInterval(true));
        // Vile Vigour converts prayer 1:1 into missing run energy.
        assertEquals(40, Spellbooks.vileVigourCost(60, 99));
        assertEquals(12, Spellbooks.vileVigourCost(10, 12));
        assertEquals(0, Spellbooks.vileVigourCost(100, 50));
        assertEquals(0.15, Spellbooks.SHADOW_VEIL_CHANCE, 1e-9);
        assertEquals(1, Spellbooks.WARDED_BIND_TICKS);
        // Demonbane spells only hit demons; Crumble Undead only undead.
        assertEquals(Spellbooks.Effect.DEMON, Spellbooks.get("dark_demonbane").effect());
        assertEquals(Spellbooks.Effect.UNDEAD, Spellbooks.get("crumble_undead").effect());
        // Every reanimation spell has heads.
        for (String spell : new String[]{"basic_reanimation", "adept_reanimation", "expert_reanimation", "master_reanimation"}) {
            assertTrue(Spellbooks.HEADS.stream().anyMatch(h -> h.spell().equals(spell)), spell);
        }
    }
}
