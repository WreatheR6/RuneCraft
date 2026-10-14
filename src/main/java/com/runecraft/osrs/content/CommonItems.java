package com.runecraft.osrs.content;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.Skill;

import java.util.Map;

/**
 * Items referenced by more than one content module (defined once here so modules can reference
 * them freely in drop tables and recipes).
 */
public final class CommonItems {
    private CommonItems() {
    }

    /** Shades of Mort'ton keys, worst to best (minigames: pyre rewards and chests; dungeons: the catacomb doors). */
    public static final String[] SHADE_KEY_METALS = {"bronze", "steel", "black", "silver", "gold"};
    public static final String[] SHADE_KEY_COLOURS = {"red", "brown", "crimson", "black", "purple"};
    private static final int[] SHADE_KEY_RGB = {0x8A5A2A, 0x9A9AA0, 0x2A2A2A, 0xD0D0E0, 0xE0B030};

    /** A shade key id: the red key keeps the old id ({@code bronze_shade_key}), the others add their colour. */
    public static String shadeKey(String metal, String colour) {
        return metal + "_shade_key" + (colour.equals("red") ? "" : "_" + colour);
    }

    public static void add(ItemSink items) {
        // Machete (npcs: Karamja shops; minigames: Tai Bwo Wannai Cleanup; skilling: hunter loot).
        items.add("machete", "Machete", Kind.TOOL, "scimitar", 0x9A9AA0).value(40).stack(1).examine("An effective tool for chopping tough jungle plants.");
        items.add("green_dragonhide", "Green dragonhide", Kind.SIMPLE, "hide", 0x3A8A3A).value(1800);
        items.add("blue_dragonhide", "Blue dragonhide", Kind.SIMPLE, "hide", 0x3A5AB0).value(2000);
        items.add("red_dragonhide", "Red dragonhide", Kind.SIMPLE, "hide", 0xB03A3A).value(2400);
        items.add("black_dragonhide", "Black dragonhide", Kind.SIMPLE, "hide", 0x2A2A2A).value(3200);
        items.add("anti_dragon_shield", "Anti-dragon shield", Kind.SHIELD, "kiteshield", 0x6A8AA0).value(20).stack(1).tier("steel")
                .equipment(new EquipmentDef(EquipmentDef.Slot.SHIELD, new EquipmentStats(0, 0, 0, -10, -5, 20, 24, 22, 10, 22, 0, 0, 0, 0),
                        Map.of(), 0, AttackType.CRUSH, EquipmentDef.WeaponClass.NONE))
                .examine("Protects you from dragonfire.");
        items.add("antifire_potion", "Antifire potion", Kind.POTION, "potion", 0x9A3A9A).value(180).stack(1);
        items.add("extended_antifire", "Extended antifire", Kind.POTION, "potion", 0xB04AB0).value(400).stack(1);
        items.add("abyssal_whip", "Abyssal whip", Kind.MELEE_WEAPON, "whip", 0x5A2A2A).value(120001).stack(1).tier("dragon")
                .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON, new EquipmentStats(0, 82, 0, 0, 0, 0, 0, 0, 0, 0, 82, 0, 0, 0),
                        Map.of(Skill.ATTACK, 70), 4, AttackType.SLASH, EquipmentDef.WeaponClass.MELEE));
        for (String tier : new String[]{"beginner", "easy", "medium", "hard", "elite", "master"}) {
            String name = Character.toUpperCase(tier.charAt(0)) + tier.substring(1);
            items.add("clue_scroll_" + tier, "Clue scroll (" + tier + ")", Kind.SIMPLE, "note", 0xE8D8A0).value(1).stack(1)
                    .examine("A " + name.toLowerCase() + " clue scroll.");
        }
        // Boss/slayer materials upgraded by other modules: Doom of Mokhaiotl and Maggot King drops (bosses) become
        // confliction gauntlets and the necklace of rupture (systems); the tormented synapse (slayer, Tormented Demons)
        // becomes the purging staff (skilling) and the scorching bow (slayer).
        items.add("mokhaiotl_cloth", "Mokhaiotl cloth", Kind.SIMPLE, "cloth", 0x6A3A8A).value(30_000_000).stack(1)
                .examine("Woven from the Doom's flesh. Upgrades a tormented bracelet into confliction gauntlets.");
        items.add("demon_tear", "Demon tear", Kind.SIMPLE, "gem", 0xC03050).value(1).stack(100000)
                .examine("Charges the eye of ayak.");
        items.add("elder_venator_fang", "Elder venator fang", Kind.SIMPLE, "fang", 0xE0D0B0).value(40_000_000).stack(1)
                .examine("Combine with a necklace of anguish to make a necklace of rupture (84 Crafting).");
        items.add("tormented_synapse", "Tormented synapse", Kind.SIMPLE, "orb", 0xE05020).value(800_000).stack(1)
                .examine("A twitching synapse from a tormented demon. Its fiery energy could empower a demonbane weapon.");
        // Caught from maniacal monkeys (skilling, Hunter) and dropped by demonic gorillas (slayer); strings a ballista.
        items.add("monkey_tail", "Monkey tail", Kind.SIMPLE, "tail", 0x8A6A3A).value(1).examine("A whole maniacal monkey tail. Ballista string!");
        // Demonic gorilla uniques (slayer module drops them; ballistae are fletched from them, uncut zenyte crafted).
        items.add("zenyte_shard", "Zenyte shard", Kind.SIMPLE, "shard", 0xF0A020).value(75_000)
                .examine("A shard of unstable zenyte.");
        items.add("ballista_limbs", "Ballista limbs", Kind.SIMPLE, "limbs", 0x8A6A4A).value(50_000).examine("Sturdy struts.");
        items.add("ballista_spring", "Ballista spring", Kind.SIMPLE, "clockwork", 0x9A9AA0).value(50_000)
                .examine("A spring-loaded mechanism.");
        items.add("light_frame", "Light frame", Kind.SIMPLE, "stock", 0xA0784A).value(50_000).examine("A light wooden frame.");
        items.add("heavy_frame", "Heavy frame", Kind.SIMPLE, "stock", 0x6A4A2A).value(50_000).examine("A heavy wooden frame.");
        // Construction material also used to repair the Mort'ton temple (minigames); sacred oil, made at the temple's
        // fire altar and turned into pyre logs (minigames), is also a Shades of Mort'ton quest item (quests).
        items.add("limestone_brick", "Limestone brick", Kind.SIMPLE, "brick", 0xD8D0B0).value(21);
        items.add("sacred_oil", "Sacred oil", Kind.POTION, "potion", 0xE0E0A0).value(100).stack(1).notDrinkable() // OSRS: Empty/Drop only
                .examine("4 doses of sacred oil.");
        for (int m = 0; m < SHADE_KEY_METALS.length; m++) {
            String metal = SHADE_KEY_METALS[m];
            String cap = Character.toUpperCase(metal.charAt(0)) + metal.substring(1);
            for (String c : SHADE_KEY_COLOURS) {
                items.add(shadeKey(metal, c), cap + " key " + c, Kind.SIMPLE, "key", SHADE_KEY_RGB[m]).value(81).stack(1)
                        .examine("A " + metal + " key with a " + c + " painted eyelet (opens chests found in the Mort'ton catacombs).");
            }
        }
        // Herblore secondaries from other modules (skilling grinds and brews them): the nihil shard (godwars, Nex)
        // becomes nihil dust for ancient brews; the star flower and gorak claws (quests, Fairytale II) make magic essence.
        items.add("nihil_shard", "Nihil shard", Kind.SIMPLE, "shard", 0x7A3A9A).value(400).stack(64)
                .examine("A shard of a nihil.");
        items.add("star_flower", "Star flower", Kind.SIMPLE, "herb", 0xE0E080).value(0)
                .examine("A rare flower, ingredient of the Fairy Queen's cure.");
        items.add("gorak_claws", "Gorak claws", Kind.SIMPLE, "bones", 0x6A3A6A).value(0).examine("Claws from a gorak.");
        // Monster drops whose lines the wiki drop sweep used to skip (MonsterDropsTable): long and curved bones (1/400
        // and 1/5,012 from most big-bone monsters; Construction bones sold to Barlak - approx: not buryable here),
        // oyster pearls (Waterbirth and lighthouse dagannoth) and unicorn horns (implings, Varlamore; ground to dust).
        items.add("long_bone", "Long bone", Kind.SIMPLE, "bones", 0xE0D8C0).value(5).examine("A Construction bone.");
        items.add("curved_bone", "Curved bone", Kind.SIMPLE, "bones", 0xD8D0B0).value(7).examine("A curved Construction bone.");
        items.add("oyster_pearls", "Oyster pearls", Kind.SIMPLE, "bead", 0xF0F0E8).value(1400)
                .examine("I could work wonders with a chisel on these pearls.");
        items.add("unicorn_horn", "Unicorn horn", Kind.SIMPLE, "tail", 0xF0F0F0).value(20).examine("This horn has restorative properties.");
        // Mort Myre snail drops (npcs module): shells chiselled into snelms and snail corpses cooked (skilling).
        String[][] shells = {{"myre", "7A7A3A"}, {"ochre", "C0902A"}, {"red", "8A1A1A"}, {"blue", "3A4A8A"}};
        for (String[] sh : shells) {
            for (String form : new String[]{"round", "pointed"}) {
                items.add("blamish_" + sh[0] + "_shell_" + form, "Blamish " + sh[0] + " shell (" + form + ")", Kind.SIMPLE, "shell",
                        Integer.parseInt(sh[1], 16)).value(150).examine("A " + form + " snail shell. I could make a snelm from it.");
            }
        }
        items.add("blamish_bark_shell", "Blamish bark shell", Kind.SIMPLE, "shell", 0x5A4A3A).value(150)
                .examine("A snail shell. I could make a snelm from it.");
        // Karamja cooking ingredients: dropped by the slayer module's jungle spiders and oomlie birds, cooked by the skilling module.
        items.add("spider_carcass", "Spider carcass", Kind.SIMPLE, "meat_raw", 0x5A4A3A).value(15).weight(0.8)
                .examine("Its creeping days are over!");
        items.add("raw_oomlie", "Raw oomlie", Kind.SIMPLE, "meat_raw", 0xC07A6A).value(10).weight(0.225)
                .examine("Raw meat from the oomlie bird.");
        items.add("thin_snail", "Thin snail", Kind.SIMPLE, "meat_raw", 0x8A7A5A).value(5)
                .examine("The thin, slimy corpse of a deceased giant snail.");
        items.add("lean_snail", "Lean snail", Kind.SIMPLE, "meat_raw", 0x8A7A5A).value(8)
                .examine("The lean, slimy corpse of a deceased giant snail.");
        items.add("fat_snail", "Fat snail", Kind.SIMPLE, "meat_raw", 0x8A7A5A).value(10)
                .examine("The fat, slimy corpse of a deceased giant snail.");
        diaryItems(items);
        CommonEquipment.add(items);
    }

    /** Team capes 1-50 (pink 1-10, yellow 11-20, blue 21-30, green 31-40, red 41-50), sold by the Wilderness cape merchants. */
    public static final int[] TEAM_CAPE_RGB = {0xE070B0, 0xE0D040, 0x3A5AC0, 0x3A9A3A, 0xC03030};

    public static String teamCape(int n) {
        return "team_" + n + "_cape";
    }

    /**
     * Items behind diary tasks that several modules hand out (OSRS Wiki values): the rusty sword (H.A.M. members, Tindel
     * Marchant), the muddy key (chaos dwarves; the Lava Maze muddy chest), the crystal key (joined from the slayer module's
     * key halves; the Taverley crystal chest), the Stronghold of Security sceptre pieces, the white 2h sword (Sir Vyvin) and
     * the team capes (cape merchants).
     */
    private static void diaryItems(ItemSink items) {
        items.add("rusty_sword", "Rusty sword", Kind.SIMPLE, "longsword", 0x8A5A3A).value(1).stack(1).weight(2.267)
                .examine("A decent-enough weapon gone rusty.");
        items.add("muddy_key", "Muddy key", Kind.SIMPLE, "key", 0x7A5A3A).value(80).stack(1)
                .examine("It looks like the key to a chest (Used to open the muddy chest in the lava maze).");
        items.add("crystal_key", "Crystal key", Kind.SIMPLE, "key", 0xB0E0E8).value(150).stack(1)
                .examine("A mysterious key for a mysterious chest.");
        items.add("left_skull_half", "Left skull half", Kind.SIMPLE, "skull", 0xE0D8C0).value(1).stack(1)
                .examine("The left half of a strange skull.");
        items.add("right_skull_half", "Right skull half", Kind.SIMPLE, "skull", 0xE0D8C0).value(1).stack(1)
                .examine("The right half of a strange skull.");
        items.add("strange_skull", "Strange skull", Kind.SIMPLE, "skull", 0xE0D8C0).value(1).stack(1)
                .examine("A weird looking skull, it looks like there are holes for a staff.");
        items.add("top_of_sceptre", "Top of sceptre", Kind.SIMPLE, "staff", 0xA08A50).value(1).stack(1)
                .examine("The top half of a sceptre.");
        items.add("bottom_of_sceptre", "Bottom of sceptre", Kind.SIMPLE, "staff", 0xA08A50).value(1).stack(1)
                .examine("The bottom half of a sceptre.");
        items.add("runed_sceptre", "Runed sceptre", Kind.SIMPLE, "staff", 0xA08A50).value(1).stack(1)
                .examine("A sceptre covered in runes, it looks like a skull should fit on top.");
        items.add("skull_sceptre", "Skull sceptre", Kind.STAFF, "staff_orb", 0xE0D8C0).value(1).stack(1).weight(1.814)
                .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON, new EquipmentStats(0, -1, 7, 10, 0, 2, 3, 1, 4, 0, 3, 0, 0, 0),
                        Map.of(), 5, AttackType.CRUSH, EquipmentDef.WeaponClass.STAFF))
                .examine("A fragile magical Sceptre.");
        items.add("white_2h_sword", "White 2h sword", Kind.MELEE_WEAPON, "2h_sword", 0xE8E8F0).value(3840).stack(1).tier("black")
                .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON, new EquipmentStats(-4, 27, 21, -4, 0, 0, 0, 0, 0, -1, 26, 0, 0, 1),
                        Map.of(Skill.ATTACK, 10), 7, AttackType.SLASH, EquipmentDef.WeaponClass.MELEE))
                .examine("A two handed sword, favoured by the White Knights.");
        // Gold helmet (Between a Rock...; 3 gold bars at an anvil, 50 Smithing) and dwarven helmet (Grim Tales reward).
        items.add("gold_helmet", "Gold helmet", Kind.ARMOUR, "full_helm", 0xE0C040).value(1).stack(1).weight(31.751)
                .equipment(EquipmentDef.armour(EquipmentDef.Slot.HEAD, new EquipmentStats(0, 0, 0, 0, 0, 0, 10, 20, 0, 10, 0, 0, 0, 0), 30))
                .examine("A gold helmet. Needed to mine the Arzinian mine's gold.");
        items.add("dwarven_helmet", "Dwarven helmet", Kind.ARMOUR, "full_helm", 0x8A6A3A).value(5000).stack(1)
                .equipment(EquipmentDef.armour(EquipmentDef.Slot.HEAD, new EquipmentStats(0, 0, 6, -2, -2, 27, 28, 31, 5, 24, 0, 0, 0, 0), 50))
                .examine("A sturdy dwarven helmet.");
        for (int n = 1; n <= 50; n++) {
            items.add(teamCape(n), "Team-" + n + " cape", Kind.SIMPLE, "cape", TEAM_CAPE_RGB[(n - 1) / 10]).value(50).stack(1)
                    .equipment(new EquipmentDef(EquipmentDef.Slot.HEAD, new EquipmentStats(0, 0, 0, 0, 0, 0, 1, 1, 0, 2, 0, 0, 0, 0),
                            Map.of(), 0, AttackType.CRUSH, EquipmentDef.WeaponClass.NONE))
                    .examine("Ooohhh look at the pretty colours...");
        }
    }
}
