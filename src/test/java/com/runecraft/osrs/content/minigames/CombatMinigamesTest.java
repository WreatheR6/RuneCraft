package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.minigames.MinigameRules.BarbarianAssault;
import com.runecraft.osrs.content.minigames.MinigameRules.CastleWars;
import com.runecraft.osrs.content.minigames.MinigameRules.GnomeRestaurant;
import com.runecraft.osrs.content.minigames.MinigameRules.MageArena;
import com.runecraft.osrs.content.minigames.MinigameRules.NightmareZone;
import com.runecraft.osrs.content.minigames.MinigameRules.PestControl;
import com.runecraft.osrs.content.minigames.MinigameRules.RoguesDen;
import com.runecraft.osrs.content.minigames.MinigameRules.SoulWars;
import com.runecraft.osrs.content.minigames.MinigameRules.TearsOfGuthix;
import com.runecraft.osrs.content.minigames.MinigameRules.TroubleBrewing;
import com.runecraft.osrs.content.systems.Jewellery;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Combat and other non-skilling minigames checked against the OSRS Wiki. */
class CombatMinigamesTest {
    private static RewardShop.Entry entry(String shop, String item) {
        return RewardShops.get(shop).entries().stream().filter(e -> e.item().equals(item)).findFirst().orElseThrow(() ->
                new AssertionError(shop + " lacks " + item));
    }

    private static int cost(String shop, String item, String currency) {
        return entry(shop, item).costs().stream().filter(c -> c.currency().equals(currency)).mapToInt(RewardShop.Cost::amount)
                .findFirst().orElse(0);
    }

    @Test
    void pestControl() {
        assertEquals(List.of(3, 4, 5), List.of(PestControl.POINTS[0], PestControl.POINTS[1], PestControl.POINTS[2]));
        assertEquals(List.of(40, 70, 100), List.of(PestControl.COMBAT[0], PestControl.COMBAT[1], PestControl.COMBAT[2]));
        assertEquals(200, PestControl.PORTAL_HP[0]);
        assertEquals(250, PestControl.PORTAL_HP[2]);
        // The 4,000 point cap.
        assertEquals(5, PestControl.pointsAwarded(2, 0));
        assertEquals(2, PestControl.pointsAwarded(2, 3998));
        assertEquals(0, PestControl.pointsAwarded(0, 4000));
        // Void Knights' Reward Options (OSRS Wiki).
        assertEquals(250, cost("pest_control", "void_knight_mace", MinigameCurrencies.PEST_CONTROL));
        assertEquals(150, cost("pest_control", "void_knight_gloves", MinigameCurrencies.PEST_CONTROL));
        assertEquals(200, cost("pest_control", "void_ranger_helm", MinigameCurrencies.PEST_CONTROL));
        assertEquals(10, cost("pest_control", "void_seal", MinigameCurrencies.PEST_CONTROL));
        RewardShop.Entry herbs = entry("pest_control", "#herbs");
        assertEquals(30, herbs.costs().get(0).amount());
        assertEquals(Skill.HERBLORE, herbs.requiredSkill());
        assertEquals(25, herbs.requiredLevel());
        assertEquals(Skill.MINING, entry("pest_control", "#ores").requiredSkill());
        // Void equipment needs 42 in every combat skill and 22 Prayer.
        assertEquals(22, ItemCatalog.get("void_knight_robe").equipment().requirements().get(Skill.PRAYER));
        // XP per point: floor(level^2/600) * N; 25 Attack = 35, 99 Prayer = 288.
        RewardShop.Entry att10 = RewardShops.get("pest_control").entries().stream()
                .filter(e -> e.reward() == RewardShop.Reward.XP && e.item().equals("ATTACK") && e.costs().get(0).amount() == 10)
                .findFirst().orElseThrow();
        assertEquals(353, RewardShop.xpReward(att10, 25)); // 350 + 1%
        assertEquals(288, RewardShop.pestControlXpPerPoint(99, 18));
        // Void set effects.
        assertEquals(1.10, PestControl.voidAccuracy(0));
        assertEquals(1.10, PestControl.voidDamage(0, false));
        assertEquals(1.10, PestControl.voidDamage(1, false));
        assertEquals(1.0, PestControl.voidDamage(2, false));
        assertEquals(1.05, PestControl.voidDamage(2, true));
    }

    @Test
    void voidGlovesAndImbuesAreWorn() {
        assertEquals(Jewellery.Slot.HANDS, Jewellery.worn("void_knight_gloves").slot());
        assertEquals(Jewellery.Slot.HANDS, Jewellery.worn("penance_gloves").slot());
        assertEquals(Jewellery.Slot.HANDS, Jewellery.worn("infinity_gloves").slot());
        assertEquals(Jewellery.Slot.CAPE, Jewellery.worn("soul_cape").slot());
        assertEquals(8, Jewellery.worn("soul_cape").stats().prayer());
        assertEquals(8, Jewellery.worn("berserker_ring_i").stats().meleeStrength());
        assertEquals(20, Jewellery.worn("ring_of_suffering_i").stats().stabDefence());
        assertEquals(Jewellery.Slot.NECK, Jewellery.worn("salve_amulet_ei").slot());
        for (Object[] imbue : MinigameRules.Imbues.ITEMS) {
            assertTrue(ItemCatalog.exists((String) imbue[0]), (String) imbue[0]);
            assertTrue(ItemCatalog.exists((String) imbue[1]), (String) imbue[1]);
            // Nightmare Zone points are 2,500 per zeal except for the ring of suffering (725,000 vs 300).
            if (!imbue[0].equals("ring_of_suffering")) {
                assertEquals((Integer) imbue[3] * 2500, (int) (Integer) imbue[2], (String) imbue[0]);
            }
        }
    }

    @Test
    void barbarianAssault() {
        // Penance hitpoints (OSRS Wiki).
        assertEquals(28, BarbarianAssault.hitpoints(0, 1));
        assertEquals(56, BarbarianAssault.hitpoints(0, 9));
        assertEquals(58, BarbarianAssault.hitpoints(1, 9));
        assertEquals(5, BarbarianAssault.hitpoints(2, 6));
        assertEquals(76, BarbarianAssault.hitpoints(3, 9));
        assertEquals(60, BarbarianAssault.hitpoints(3, 10));
        // Eight omega eggs kill the 250 hitpoint queen.
        assertTrue(BarbarianAssault.omegaEggDamage() * 8 >= BarbarianAssault.QUEEN_HP);
        assertTrue(BarbarianAssault.omegaEggDamage() * 7 < BarbarianAssault.QUEEN_HP);
        // Full-round base points per role, wave 10 = 80 in role.
        assertEquals(370, BarbarianAssault.fullGameHonour(0));
        assertEquals(357, BarbarianAssault.fullGameHonour(1));
        assertEquals(278, BarbarianAssault.fullGameHonour(2));
        assertEquals(305, BarbarianAssault.fullGameHonour(3));
        assertEquals(80, BarbarianAssault.honour(2, 10));
        assertTrue(BarbarianAssault.honour(0, 9) > BarbarianAssault.honour(0, 1));
        // Runner penalties: 3 each (max 10), double for defenders (max 20), none on wave 10.
        assertEquals(6, BarbarianAssault.runnerPenalty(0, 2, 3));
        assertEquals(10, BarbarianAssault.runnerPenalty(0, 5, 3));
        assertEquals(20, BarbarianAssault.runnerPenalty(1, 5, 3));
        assertEquals(0, BarbarianAssault.runnerPenalty(1, 5, 10));
        // Team points never go below 0, but the attacker keeps its own kills; the defender loses its own points too.
        assertEquals(6, BarbarianAssault.waveHonour(0, 1, 9));
        assertEquals(0, BarbarianAssault.waveHonour(1, 1, 9));
        assertEquals(10, BarbarianAssault.addHonour(4990, 50));
        // Commander Connad: torso 375 each role + a queen kill; gloves no queen kill; granite body only coins.
        assertEquals(375, cost("barbarian_assault", "fighter_torso", MinigameCurrencies.BA_HEALER));
        assertEquals(1, cost("barbarian_assault", "fighter_torso", MinigameCurrencies.BA_QUEEN));
        assertEquals(0, cost("barbarian_assault", "penance_gloves", MinigameCurrencies.BA_QUEEN));
        assertEquals(0, cost("barbarian_assault", "runner_boots", MinigameCurrencies.BA_QUEEN));
        assertEquals(95_000, cost("barbarian_assault", "granite_body", MinigameCurrencies.COINS));
        assertEquals(0, cost("barbarian_assault", "granite_body", MinigameCurrencies.BA_ATTACKER));
        // The armour set (hat, gloves, boots, torso, skirt) totals 1,275 in each role.
        int set = 0;
        for (String item : List.of("fighter_hat", "penance_gloves", "runner_boots", "fighter_torso", "penance_skirt")) {
            set += cost("barbarian_assault", item, MinigameCurrencies.BA_COLLECTOR);
        }
        assertEquals(1275, set);
        assertEquals(13, ContentRegistry.boss("penance_queen").maxHit());
    }

    @Test
    void castleWars() {
        assertEquals(3, CastleWars.tickets(1, 0));
        assertEquals(2, CastleWars.tickets(2, 1));
        assertEquals(1, CastleWars.tickets(0, 0));
        assertEquals(2, CastleWars.tickets(1, 1));
        assertEquals(1, CastleWars.tickets(0, 3));
        // "_helm" is the (med) decorative helm, 4 / 40 / 400; the full helm is 5 / 40 / 500 (white cut from 50 in April 2025).
        assertEquals(4, cost("castle_wars", "red_decorative_helm", MinigameCurrencies.CW_TICKETS));
        assertEquals(5, cost("castle_wars", "red_decorative_full_helm", MinigameCurrencies.CW_TICKETS));
        assertEquals(8, cost("castle_wars", "red_decorative_body", MinigameCurrencies.CW_TICKETS));
        assertEquals(80, cost("castle_wars", "white_decorative_body", MinigameCurrencies.CW_TICKETS));
        assertEquals(40, cost("castle_wars", "white_decorative_helm", MinigameCurrencies.CW_TICKETS));
        assertEquals(40, cost("castle_wars", "white_decorative_full_helm", MinigameCurrencies.CW_TICKETS));
        assertEquals(500, cost("castle_wars", "gold_decorative_full_helm", MinigameCurrencies.CW_TICKETS));
        assertEquals(800, cost("castle_wars", "gold_decorative_body", MinigameCurrencies.CW_TICKETS));
        assertEquals(500, cost("castle_wars", "gold_decorative_sword", MinigameCurrencies.CW_TICKETS));
        assertEquals(75, cost("castle_wars", "guthix_halo", MinigameCurrencies.CW_TICKETS));
        assertEquals(10, cost("castle_wars", "zamorak_hood", MinigameCurrencies.CW_TICKETS));
        assertEquals(3, ItemCatalog.get("saradomin_halo").equipment().stats().prayer());
        // Round 40: Lanthus' whole stock (OSRS Wiki "Castle Wars Ticket Exchange").
        assertEquals(400, cost("castle_wars", "gold_decorative_helm", MinigameCurrencies.CW_TICKETS));
        assertEquals(60, cost("castle_wars", "white_decorative_skirt", MinigameCurrencies.CW_TICKETS));
        assertEquals(400, cost("castle_wars", "gold_decorative_boots", MinigameCurrencies.CW_TICKETS));
        assertEquals(10, cost("castle_wars", "saradomin_castlewars_cloak", MinigameCurrencies.CW_TICKETS));
        assertEquals(100, cost("castle_wars", "zamorak_banner", MinigameCurrencies.CW_TICKETS));
        assertEquals(20, cost("castle_wars", "decorative_magic_hat", MinigameCurrencies.CW_TICKETS));
        assertEquals(30, cost("castle_wars", "decorative_ranged_legs", MinigameCurrencies.CW_TICKETS));
        assertEquals(40, cost("castle_wars", "decorative_quiver", MinigameCurrencies.CW_TICKETS));
        assertEquals(39, RewardShops.get("castle_wars").entries().size(), "24 decorative pieces, 4 hoods and cloaks, 2 banners, "
                + "6 magic and ranged pieces and 3 halos");
        // Red, white and gold have steel, mithril and adamant stats and need 5, 20 and 30 Defence (Attack for the sword).
        com.runecraft.osrs.EquipmentDef redHelm = ItemCatalog.get("red_decorative_full_helm").equipment();
        assertEquals(9, redHelm.stats().stabDefence());
        assertEquals(5, redHelm.requirements().get(Skill.DEFENCE));
        assertEquals(14, ItemCatalog.get("gold_decorative_helm").equipment().stats().stabDefence());
        com.runecraft.osrs.EquipmentDef goldBody = ItemCatalog.get("gold_decorative_body").equipment();
        assertEquals(65, goldBody.stats().stabDefence());
        assertEquals(30, goldBody.requirements().get(Skill.DEFENCE));
        com.runecraft.osrs.EquipmentDef whiteSword = ItemCatalog.get("white_decorative_sword").equipment();
        assertEquals(22, whiteSword.stats().meleeStrength());
        assertEquals(20, whiteSword.requirements().get(Skill.ATTACK));
        assertEquals(1, ItemCatalog.get("gold_decorative_boots").equipment().stats().meleeStrength());
        assertEquals(20, ItemCatalog.get("decorative_magic_top").equipment().stats().magicDefence());
        assertEquals(20, ItemCatalog.get("decorative_magic_top").equipment().requirements().get(Skill.MAGIC));
        assertEquals(2, ItemCatalog.get("zamorak_hood").equipment().stats().crushDefence());
        Jewellery.Worn cloak = Jewellery.worn("saradomin_castlewars_cloak");
        assertNotNull(cloak);
        assertEquals(Jewellery.Slot.CAPE, cloak.slot());
        assertEquals(2, cloak.stats().rangedDefence());
        assertEquals(30, Jewellery.wearLevels("decorative_quiver").get(Skill.RANGED));
        // The team cloaks handed out at the portals are cape-slot items of their own (not the reward hoods).
        assertEquals(Jewellery.Slot.CAPE, Jewellery.worn("zamorak_team_cloak").slot());
        assertTrue(RewardShops.get("castle_wars").entries().stream().noneMatch(e -> e.item().endsWith("_team_cloak")));
        List<String> log = castleWarsLog();
        assertEquals(39, log.size());
        for (String id : log) {
            assertTrue(ItemCatalog.exists(id), id);
        }
    }

    private static List<String> castleWarsLog() {
        return com.runecraft.osrs.CollectionLogData.pages().stream().filter(p -> p.name().equals("Castle Wars")).findFirst()
                .orElseThrow().items();
    }

    @Test
    void mageTrainingArena() {
        assertEquals(21, MageArena.ROOM_MAGIC[1]);
        assertEquals(6, MageArena.enchantLevel(87));
        assertEquals(1, MageArena.enchantLevel(7));
        assertEquals(0, MageArena.enchantLevel(6));
        // Lvl-4 enchant: 4 points, 8 on the tenth shape, +2 for the bonus shape after doubling.
        assertEquals(4, MageArena.enchantPoints(4, 3, false, false));
        assertEquals(8, MageArena.enchantPoints(4, 10, false, false));
        assertEquals(10, MageArena.enchantPoints(4, 10, true, false));
        assertEquals(8, MageArena.enchantPoints(4, 1, false, true));
        // 250 coins: 2 points plus a 50% chance of a third.
        assertEquals(2, MageArena.alchemistPoints(250, 0.6));
        assertEquals(3, MageArena.alchemistPoints(250, 0.4));
        // 20 bananas = 1.25 points; 27 peaches capped at 3.
        assertEquals(1, MageArena.graveyardPoints(20, false, 0.9));
        assertEquals(2, MageArena.graveyardPoints(20, false, 0.1));
        assertEquals(3, MageArena.graveyardPoints(27, true, 0.0));
        // Rewards Guardian: infinity totals and wand upgrades hand in the previous wand.
        int enchant = 0;
        for (String piece : List.of("infinity_hat", "infinity_top", "infinity_bottoms", "infinity_boots", "infinity_gloves")) {
            enchant += cost("mage_training_arena", piece, MinigameCurrencies.MTA_ENCHANTMENT);
        }
        assertEquals(14_700, enchant);
        RewardShop.Entry master = entry("mage_training_arena", "master_wand");
        assertEquals(RewardShop.Reward.UPGRADE, master.reward());
        assertEquals("teacher_wand", master.requiresItem());
        assertEquals(15, cost("mage_training_arena", "lava_rune", MinigameCurrencies.MTA_ENCHANTMENT));
        assertEquals(0, cost("mage_training_arena", "law_rune", MinigameCurrencies.MTA_ALCHEMIST));
        assertEquals(2, cost("mage_training_arena", "law_rune", MinigameCurrencies.MTA_TELEKINETIC));
    }

    @Test
    void troubleBrewing() {
        assertEquals(0, TroubleBrewing.piecesOfEight(0, 0));
        assertEquals(150 + 100, TroubleBrewing.piecesOfEight(15, 340));
        assertEquals(10 + 37, TroubleBrewing.piecesOfEight(1, 37));
        assertEquals(1000, cost("trouble_brewing", "grey_naval_shirt", MinigameCurrencies.EIGHT));
        assertEquals(500, cost("trouble_brewing", "black_tricorn_hat", MinigameCurrencies.EIGHT));
        assertEquals(1000, cost("trouble_brewing", "brown_navy_slacks", MinigameCurrencies.EIGHT));
        assertEquals(20, cost("trouble_brewing", "bottle_of_rum", MinigameCurrencies.EIGHT));
        assertEquals(7, MinigameItems.NAVAL_COLOURS.size());
    }

    @Test
    void roguesDen() {
        assertEquals(RoguesDen.OBSTACLES.length, RoguesDen.DRAIN.length);
        assertEquals(RoguesDen.OBSTACLES.length, RoguesDen.AGILITY.length);
        assertEquals(5, RoguesDen.DRAIN[0]); // contortion bars
        assertEquals(240.0, RoguesDen.xp(20, 8, true));
        assertTrue(RoguesDen.xp(20, 4, false) < 240.0);
        assertEquals(50, Minigame.ROGUES_DEN.level());
    }

    @Test
    void gnomeRestaurant() {
        assertEquals(0, GnomeRestaurant.tokens(0, 11));
        assertEquals(1, GnomeRestaurant.tokens(11, 12));
        assertEquals(1, GnomeRestaurant.tokens(10, 13));
        for (MinigameRules.Loot l : GnomeRestaurant.TIPS) {
            assertTrue(ItemCatalog.exists(l.item()), l.item());
        }
        for (RewardShop.Entry e : RewardShops.get("gnome_restaurant").entries()) {
            assertEquals(MinigameCurrencies.GNOME_TOKENS, e.costs().get(0).currency());
        }
    }

    @Test
    void nightmareZone() {
        assertNotNull(Minigame.byId("nightmare_zone"));
        for (String q : NightmareZone.QUESTS) {
            assertNotNull(ContentRegistry.quest(q), q);
        }
        assertEquals(NightmareZone.QUESTS.size(), Set.copyOf(NightmareZone.QUESTS).size());
        assertTrue(NightmareZone.canEnter(q -> q.equals("VAMPYRE_SLAYER") || q.equals("holy_grail") || q.equals("lost_city")
                || q.equals("fight_arena") || q.equals("dragon_slayer_i")));
        assertFalse(NightmareZone.canEnter(q -> q.equals("VAMPYRE_SLAYER")));
        for (Object[] b : NightmareZone.BOSSES) {
            assertNotNull(ContentRegistry.monster((String) b[0]), (String) b[0]);
        }
        assertEquals(224, NightmareZone.points("nmz_count_draynor", false));
        assertEquals(3872, NightmareZone.points("nmz_glod", false));
        assertEquals(0, NightmareZone.points("chicken", false));
        assertEquals(170, ContentRegistry.monster("nmz_khazard_warlord").hitpoints);
        // Dom Onion's Reward Shop: resources and imbues.
        assertEquals(75, cost("nightmare_zone", "flax", MinigameCurrencies.NIGHTMARE_ZONE));
        assertEquals(145, cost("nightmare_zone", "vial_of_water", MinigameCurrencies.NIGHTMARE_ZONE));
        assertEquals(650_000, cost("nightmare_zone", "berserker_ring_i", MinigameCurrencies.NIGHTMARE_ZONE));
        assertEquals(1_250_000, cost("nightmare_zone", "black_mask_i", MinigameCurrencies.NIGHTMARE_ZONE));
        assertEquals(725_000, cost("nightmare_zone", "ring_of_suffering_i", MinigameCurrencies.NIGHTMARE_ZONE));
        assertEquals("berserker_ring", entry("nightmare_zone", "berserker_ring_i").requiresItem());
        assertEquals(RewardShop.Reward.UPGRADE, entry("nightmare_zone", "salve_amulet_i").reward());
    }

    @Test
    void soulWars() {
        assertEquals(60, SoulWars.zeal(1, 30, 6, 260));
        assertEquals(50, SoulWars.zeal(0, 99, 99, 9999));
        assertEquals(40, SoulWars.zeal(-1, 30, 6, 260));
        assertEquals(0, SoulWars.zeal(1, 0, 0, 0));
        assertEquals(20, SoulWars.zeal(1, 30, 0, 0));
        assertEquals(0, SoulWars.reducedDamage(40, 100));
        assertEquals(30, SoulWars.reducedDamage(40, 25));
        assertEquals(1500, ContentRegistry.monster("avatar_of_destruction").hitpoints);
        assertNotNull(ContentRegistry.monster("avatar_of_creation"));
        assertNotNull(ContentRegistry.monster("forgotten_soul"));
        // Zeal XP: floor(level^2/600) * 30/27/14 per token, no bulk bonus, level 30 needed.
        RewardShop.Entry str100 = RewardShops.get("soul_wars").entries().stream()
                .filter(e -> e.reward() == RewardShop.Reward.XP && e.item().equals("STRENGTH") && e.costs().get(0).amount() == 100)
                .findFirst().orElseThrow();
        assertEquals(30, str100.requiredLevel());
        assertEquals(16 * 30 * 100, RewardShop.xpReward(str100, 99));
        assertEquals(2500, cost("soul_wars", "soul_cape", MinigameCurrencies.ZEAL));
        assertEquals(260, cost("soul_wars", "seers_ring_i", MinigameCurrencies.ZEAL));
        assertEquals(200, cost("soul_wars", "granite_ring_i", MinigameCurrencies.ZEAL));
        // Round 40: fragments and bones are worth 1% with control, a third without (kept in thirds so none is lost).
        int r = SoulWars.FULL_REDUCTION_THIRDS;
        assertEquals(100, SoulWars.percent(r));
        r = SoulWars.offer(r, 10, true);
        assertEquals(90, SoulWars.percent(r));
        r = SoulWars.offer(r, 1, false);
        assertEquals(90, SoulWars.percent(r), "a third of a percent off still shows 90%");
        r = SoulWars.offer(r, 2, false);
        assertEquals(89, SoulWars.percent(r));
        assertEquals(0, SoulWars.offer(r, 1000, true));
        assertEquals(SoulWars.FULL_REDUCTION_THIRDS, SoulWars.bury(r, 1000, false), "bones can't push past 100%");
        assertEquals(r + 3, SoulWars.bury(r, 1, true));
        assertEquals(0, SoulWars.avatarDamage(40, SoulWars.FULL_REDUCTION_THIRDS));
        assertEquals(30, SoulWars.avatarDamage(40, 25 * 3));
        assertEquals(0, SoulWars.control(-1, 2, 1));
        assertEquals(1, SoulWars.control(0, 1, 3));
        assertEquals(0, SoulWars.control(0, 2, 2), "a tie keeps the current owner");
        assertEquals(-1, SoulWars.control(-1, 0, 0));
        // Forgotten Souls (level 46 and 76) drop 2 fragments and aren't aggressive.
        assertEquals(50, ContentRegistry.monster("forgotten_soul").hitpoints);
        assertEquals(80, ContentRegistry.monster("forgotten_soul_76").hitpoints);
        assertEquals(2, ContentRegistry.monster("forgotten_soul").drops.get(0).min());
    }

    @Test
    void tearsOfGuthix() {
        assertEquals(10.0, TearsOfGuthix.xpPerTear(0));
        assertEquals(60.0, TearsOfGuthix.xpPerTear(13_500));
        assertEquals(15.0, TearsOfGuthix.xpPerTear(1350));
        assertEquals(60.0 * 200, TearsOfGuthix.xp(200, 1_000_000));
        assertEquals(250, TearsOfGuthix.ticks(250));
        assertEquals(Skill.values().length, Set.copyOf(TearsOfGuthix.ORDER).size());
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill s : Skill.values()) {
            xp.put(s, 5000);
        }
        // Ties go to the earlier skill in the wiki's list: Attack first.
        assertEquals(Skill.ATTACK, TearsOfGuthix.skill(xp::get, s -> true));
        xp.put(Skill.HERBLORE, 10);
        assertEquals(Skill.HERBLORE, TearsOfGuthix.skill(xp::get, s -> true));
        assertEquals(Skill.ATTACK, TearsOfGuthix.skill(xp::get, s -> s != Skill.HERBLORE));
        assertNull(TearsOfGuthix.skill(xp::get, s -> false));
    }

    @Test
    void newMinigamesAreWired() {
        for (String id : List.of("nightmare_zone", "soul_wars", "tears_of_guthix")) {
            Minigame m = Minigame.byId(id);
            assertNotNull(m, id);
            assertTrue(MinigameWorld.placeBlocks().get(m.place()).contains(m.entranceBlock()), id);
        }
        assertTrue(MinigameWorld.NPC_SHOPS.containsValue("nightmare_zone"));
        assertTrue(MinigameWorld.NPC_SHOPS.containsValue("soul_wars"));
        assertEquals(MinigameItems.NAVAL_COLOURS.size() * 3 + 1, MinigameWorld.collectionLog().stream()
                .filter(p -> p.name().equals("Trouble Brewing")).findFirst().orElseThrow().items().size());
    }
}
