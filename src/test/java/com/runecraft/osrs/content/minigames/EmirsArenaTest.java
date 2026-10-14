package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.content.minigames.EmirsArena.DuelSlot;
import com.runecraft.osrs.content.minigames.EmirsArena.Options;
import com.runecraft.osrs.content.minigames.EmirsArena.Rule;
import com.runecraft.osrs.content.minigames.LastManStanding.Build;
import com.runecraft.osrs.content.systems.SpecialAttacks;
import com.runecraft.osrs.data.AmmoRules;
import com.runecraft.osrs.data.EquipmentBonuses;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class EmirsArenaTest {
    @Test
    void minigameIsAppendedLastAndEnteredInAlKharid() {
        Minigame[] all = Minigame.values();
        assertEquals(Minigame.EMIRS_ARENA, all[all.length - 2]); // the Fortis Colosseum came after it
        assertEquals(2, Minigame.EMIRS_ARENA.maxPlayers());
        assertTrue(EmirsArena.inArena(Minigame.EMIRS_ARENA.entranceX(), Minigame.EMIRS_ARENA.entranceY()));
        // The ring of dueling's Emir's Arena teleport lands inside the arena grounds.
        assertTrue(EmirsArena.inArena(3315, 3235));
        assertFalse(EmirsArena.inArena(3293, 3174)); // Al Kharid bank
    }

    @Test
    void twelveDuelOptionsAndElevenSlots() {
        assertEquals(12, Rule.values().length);
        assertEquals(11, DuelSlot.values().length);
        assertEquals(Rule.NO_SPECIAL_ATTACKS, Rule.byKey("no_special_attacks"));
        assertEquals(Rule.FUN_WEAPONS, Rule.byKey("Fun Weapons"));
    }

    @Test
    void optionsRoundTrip() {
        Options o = Options.none().toggle(Rule.NO_PRAYER).toggle(Rule.NO_FOOD).toggle(DuelSlot.SHIELD).toggle(DuelSlot.RING);
        Options back = Options.decode(o.encode());
        assertEquals(o.rules(), back.rules());
        assertEquals(o.slots(), back.slots());
        assertFalse(o.toggle(Rule.NO_PRAYER).has(Rule.NO_PRAYER));
        assertEquals("no restrictions", Options.none().describe());
    }

    @Test
    void impossibleDuelsAreRefused() {
        Options none = new Options(EnumSet.of(Rule.NO_RANGED, Rule.NO_MELEE, Rule.NO_MAGIC), EnumSet.noneOf(DuelSlot.class));
        assertNotNull(EmirsArena.problem(none));
        assertNull(EmirsArena.problem(Options.none()));
        assertNull(EmirsArena.problem(EmirsArena.preset("whip")));
        assertNull(EmirsArena.problem(EmirsArena.preset("boxing")));
    }

    @Test
    void presetsAndStyles() {
        Options whip = EmirsArena.preset("whip");
        assertFalse(whip.disabled(DuelSlot.WEAPON));
        assertTrue(whip.disabled(DuelSlot.SHIELD) && whip.disabled(DuelSlot.BODY));
        assertTrue(EmirsArena.weaponAllowed(whip, "abyssal_whip"));
        Options boxing = EmirsArena.preset("boxing");
        assertFalse(EmirsArena.weaponAllowed(boxing, "abyssal_whip"));
        assertTrue(EmirsArena.weaponAllowed(boxing, ""));
        assertTrue(EmirsArena.styleAllowed(boxing, AttackType.CRUSH));
        assertFalse(EmirsArena.styleAllowed(boxing, AttackType.MAGIC));
        Options fun = Options.none().toggle(Rule.FUN_WEAPONS);
        assertFalse(EmirsArena.weaponAllowed(fun, ""));
        assertTrue(EmirsArena.weaponAllowed(fun, "rubber_chicken"));
    }

    @Test
    void foodAndDrinks() {
        Options noFood = Options.none().toggle(Rule.NO_FOOD);
        assertTrue(EmirsArena.blockedConsumable(noFood, Kind.FOOD, "shark"));
        assertTrue(EmirsArena.blockedConsumable(noFood, Kind.POTION, "saradomin_brew"));
        assertFalse(EmirsArena.blockedConsumable(noFood, Kind.POTION, "super_restore"));
        Options noDrinks = Options.none().toggle(Rule.NO_DRINKS);
        assertTrue(EmirsArena.blockedConsumable(noDrinks, Kind.POTION, "super_restore"));
        assertFalse(EmirsArena.blockedConsumable(noDrinks, Kind.FOOD, "shark"));
    }

    @Test
    void rankedRewardsFollowTheWiki() {
        assertEquals(16, EmirsArena.rewardPoints(true, 0));
        assertEquals(20, EmirsArena.rewardPoints(true, 4));
        assertEquals(26, EmirsArena.rewardPoints(true, 50));
        assertEquals(12, EmirsArena.rewardPoints(false, 9));
        assertEquals(5, EmirsArena.cappedGain(14_995, 16));
        assertEquals(0, EmirsArena.cappedGain(15_000, 16));
        assertTrue(EmirsArena.rankChange(2500, 2500, true) > 0);
        assertTrue(EmirsArena.rankChange(2500, 2500, false) < 0);
        assertEquals("Bronze I", EmirsArena.rankTitle(0));
        assertEquals("Steel III", EmirsArena.rankTitle(EmirsArena.START_RANK));
        assertEquals("Rune IV", EmirsArena.rankTitle(5999));
        assertEquals("Dragon Grandmaster", EmirsArena.rankTitle(9000));
    }

    @Test
    void rewardShopSellsTheScrollOfImbuing() {
        RewardShop shop = RewardShops.get(EmirsArena.SHOP);
        assertNotNull(shop);
        RewardShop.Entry scroll = shop.entries().stream().filter(e -> e.item().equals(EmirsArena.SCROLL_OF_IMBUING)).findFirst()
                .orElseThrow();
        assertEquals(200, scroll.costs().get(0).amount());
        RewardShop.Entry sack = shop.entries().stream().filter(e -> e.item().equals(EmirsArena.BLIGHTED_SURGE_SACK)).findFirst()
                .orElseThrow();
        assertEquals(50, sack.count());
        assertEquals(10, sack.costs().get(0).amount());
    }

    @Test
    void scrollImbuesTheNightmareZoneItems() {
        assertEquals("berserker_ring_i", EmirsArena.imbued("berserker_ring"));
        assertEquals("salve_amulet_ei", EmirsArena.imbued("salve_amulet_e"));
        assertNull(EmirsArena.imbued("ring_of_wealth"));
        // Every imbue is offered by both Nightmare Zone and Soul Wars, which the scroll mirrors.
        for (Object[] row : MinigameRules.Imbues.ITEMS) {
            for (String shop : new String[]{"nightmare_zone", "soul_wars"}) {
                assertTrue(RewardShops.get(shop).entries().stream().anyMatch(e -> e.item().equals(row[1])
                        && row[0].equals(e.requiresItem())), shop + " " + row[1]);
            }
        }
    }

    @Test
    void lmsBuildPrayerRules() {
        for (Build b : Build.values()) {
            assertFalse(b.allows(Prayer.PROTECT_ITEM));
            assertFalse(b.allows(Prayer.SMITE));
            assertFalse(b.allows(Prayer.REDEMPTION));
            assertTrue(b.allows(Prayer.PROTECT_FROM_MELEE));
            assertTrue(b.allows(Prayer.EAGLE_EYE));
        }
        assertTrue(Build.MAX_MED.allows(Prayer.PIETY));
        assertTrue(Build.MAX_MED.allows(Prayer.RIGOUR));
        assertTrue(Build.MAX_MED.allows(Prayer.AUGURY));
        for (Build b : new Build[]{Build.ZERKER, Build.PURE}) {
            for (Prayer p : new Prayer[]{Prayer.PIETY, Prayer.RIGOUR, Prayer.AUGURY, Prayer.CHIVALRY}) {
                assertFalse(b.allows(p), b + " " + p);
                assertNotNull(LastManStanding.prayerBlocked(b, p));
            }
        }
        assertNull(LastManStanding.prayerBlocked(Build.MAX_MED, Prayer.PIETY));
    }

    @Test
    void lmsCopiesUseTheBaseItemsRules() {
        assertEquals("abyssal_whip", EquipmentBonuses.reference("lms_abyssal_whip"));
        assertEquals("abyssal_whip", EquipmentBonuses.reference("abyssal_whip"));
        assertNotNull(SpecialAttacks.of("lms_armadyl_godsword"));
        assertEquals(SpecialAttacks.of("armadyl_godsword").name(), SpecialAttacks.of("lms_armadyl_godsword").name());
        assertTrue(EquipmentBonuses.twoHanded("lms_armadyl_godsword"));
        assertTrue(AmmoRules.restricted("lms_armadyl_crossbow"));
        assertTrue(AmmoRules.canFire("lms_armadyl_crossbow", "lms_opal_dragon_bolts_e"));
        assertFalse(AmmoRules.canFire("lms_armadyl_crossbow", "rune_arrow"));
    }
}
