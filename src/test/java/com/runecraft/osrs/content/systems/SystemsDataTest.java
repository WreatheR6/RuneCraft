package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Skill;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure-data tests for the systems module. */
class SystemsDataTest {
    // ---------------- Special attacks ----------------

    @Test
    void specCostsMatchOsrs() {
        assertEquals(25, SpecialAttacks.of("dragon_dagger").cost());
        assertEquals(25, SpecialAttacks.of("dragon_dagger_pp").cost());
        assertEquals(55, SpecialAttacks.of("dragon_scimitar").cost());
        assertEquals(25, SpecialAttacks.of("dragon_longsword").cost());
        assertEquals(100, SpecialAttacks.of("dragon_battleaxe").cost());
        assertEquals(50, SpecialAttacks.of("dragon_warhammer").cost());
        assertEquals(30, SpecialAttacks.of("dragon_warhammer").param());
        assertEquals(50, SpecialAttacks.of("dragon_claws").cost());
        assertEquals(60, SpecialAttacks.of("granite_maul").cost());
        assertEquals(50, SpecialAttacks.of("abyssal_whip").cost());
        assertEquals(25, SpecialAttacks.of("abyssal_dagger").cost());
        assertEquals(50, SpecialAttacks.of("abyssal_bludgeon").cost());
        assertEquals(55, SpecialAttacks.of("dark_bow").cost());
        assertEquals(55, SpecialAttacks.of("magic_shortbow").cost());
        assertEquals(50, SpecialAttacks.of("toxic_blowpipe").cost());
        assertEquals(50, SpecialAttacks.of("voidwaker").cost());
        assertEquals(30, SpecialAttacks.of("crystal_halberd").cost());
        assertEquals(35, SpecialAttacks.of("elder_maul").param());
        assertNull(SpecialAttacks.of("bronze_dagger"));
        assertNull(SpecialAttacks.of("soulreaper_axe"));
    }

    @Test
    void godswordsAreFaithful() {
        SpecialAttacks.Spec ags = SpecialAttacks.of("armadyl_godsword");
        assertEquals(2.0, ags.accuracy(), 1e-9);
        assertEquals(1.375, ags.damage(), 1e-9);
        assertEquals(1.21, SpecialAttacks.of("bandos_godsword").damage(), 1e-9);
        assertEquals(SpecialAttacks.Effect.DRAIN_BY_DAMAGE, SpecialAttacks.of("bandos_godsword").effect());
        assertEquals(SpecialAttacks.Effect.HEAL_AND_PRAY, SpecialAttacks.of("saradomin_godsword").effect());
        assertEquals(SpecialAttacks.Effect.FREEZE, SpecialAttacks.of("zamorak_godsword").effect());
        assertEquals(1.15, SpecialAttacks.of("dragon_dagger").accuracy(), 1e-9);
        assertEquals(1.15, SpecialAttacks.of("dragon_dagger").damage(), 1e-9);
    }

    @Test
    void sgsAndRampageAndPenance() {
        assertEquals(10, SpecialAttacks.sgsRestore(6)[0]);
        assertEquals(5, SpecialAttacks.sgsRestore(6)[1]);
        assertEquals(30, SpecialAttacks.sgsRestore(60)[0]);
        assertEquals(15, SpecialAttacks.sgsRestore(60)[1]);
        assertEquals(0, SpecialAttacks.sgsRestore(0)[0]);
        // 99 in all four: drains 9 each = 36, boost 10 + 9 = 19.
        assertEquals(19, SpecialAttacks.rampageBoost(99, 99, 99, 99));
        assertEquals(1.0, SpecialAttacks.penance(99, 99), 1e-9);
        assertEquals(1.495, SpecialAttacks.penance(99, 0), 1e-9);
    }

    @Test
    void energyRegeneratesTenPercentEveryThirtySeconds() {
        assertEquals(50, SpecialAttacks.regen(50, false, 49));
        assertEquals(60, SpecialAttacks.regen(50, false, 50));
        assertEquals(100, SpecialAttacks.regen(95, false, 100));
        assertEquals(60, SpecialAttacks.regen(50, true, 25));
    }

    @Test
    void clawCascade() {
        Random r = new Random(1);
        for (int i = 0; i < 200; i++) {
            int[] h = SpecialAttacks.clawHits(r, new boolean[]{true, false, false, false}, 40);
            assertTrue(h[0] >= 20 && h[0] <= 39, "first hit " + h[0]);
            assertEquals(h[0] / 2, h[1]);
            assertEquals(h[1] / 2, h[2]);
            assertEquals(h[2] + 1, h[3]);
            int[] s = SpecialAttacks.clawHits(r, new boolean[]{false, true, false, false}, 40);
            assertEquals(0, s[0]);
            assertTrue(s[1] >= 15 && s[1] <= 35);
            assertEquals(s[1], s[2] + s[3]);
            int[] f = SpecialAttacks.clawHits(r, new boolean[]{false, false, false, true}, 40);
            assertTrue(f[3] >= 10 && f[3] <= 50);
            int[] m = SpecialAttacks.clawHits(r, new boolean[4], 40);
            assertTrue(m[2] <= 1 && m[3] <= 1 && m[0] == 0 && m[1] == 0);
        }
    }

    // ---------------- Spellbooks ----------------

    @Test
    void ancientMagicksTable() {
        List<Spellbooks.Spell> ancient = Spellbooks.book(Spellbooks.Book.ANCIENT);
        assertEquals(16, ancient.stream().filter(Spellbooks.Spell::isCombat).count());
        assertEquals(9, ancient.stream().filter(s -> s.kind() == Spellbooks.Kind.TELEPORT).count());
        Spellbooks.Spell barrage = Spellbooks.get("ice_barrage");
        assertEquals(94, barrage.level());
        assertEquals(30, barrage.maxHit());
        assertEquals(52.0, barrage.xp(), 1e-9);
        assertTrue(barrage.area());
        assertEquals(32, Spellbooks.freezeTicks(barrage));
        assertEquals(6, barrage.runes().get("water_rune"));
        assertEquals(4, barrage.runes().get("death_rune"));
        assertEquals(2, barrage.runes().get("blood_rune"));
        assertEquals(Spellbooks.Effect.HEAL, Spellbooks.get("blood_blitz").effect());
        assertEquals(25, Spellbooks.get("blood_blitz").param());
        assertEquals(Spellbooks.Effect.POISON, Spellbooks.get("smoke_rush").effect());
        assertEquals("desert_treasure_i", Spellbooks.Book.ANCIENT.quest);
        assertEquals("lunar_diplomacy", Spellbooks.Book.LUNAR.quest);
    }

    @Test
    void lunarAndArceuus() {
        Spellbooks.Spell veng = Spellbooks.get("vengeance");
        assertEquals(94, veng.level());
        assertEquals(4, veng.runes().get("astral_rune"));
        assertEquals(86, Spellbooks.get("plank_make").level());
        assertEquals(96, Spellbooks.get("spellbook_swap").level());
        assertEquals(80, Spellbooks.get("death_charge").level());
        assertEquals(3, Spellbooks.thrallTier("resurrect_greater_ghost"));
        assertEquals(2, Spellbooks.thrallTier("resurrect_superior_zombie"));
        assertEquals(1, Spellbooks.thrallTier("resurrect_lesser_skeleton"));
        assertEquals(19, Spellbooks.magicDartMax(99));
        assertEquals("hellhound", Spellbooks.headFor("Hellhound").monsterKeyword());
        assertEquals("ensouled_dragon_head", Spellbooks.headFor("Green dragon").id());
        assertNull(Spellbooks.headFor("Cow"));
    }

    @Test
    void spellsAreWellFormed() {
        Set<String> ids = new HashSet<>();
        for (Spellbooks.Spell s : Spellbooks.all()) {
            assertTrue(ids.add(s.id()), s.id());
            assertTrue(s.level() >= 1 && s.level() <= 99, s.id());
            for (String rune : s.runes().keySet()) {
                assertNotNull(ItemCatalog.get(rune), s.id() + " rune " + rune);
            }
            if (s.kind() == Spellbooks.Kind.TELEPORT) {
                assertNotNull(s.destination(), s.id());
            }
        }
        for (Spellbooks.Head h : Spellbooks.HEADS) {
            assertNotNull(ItemCatalog.get(h.id()), h.id());
            assertNotNull(Spellbooks.get(h.spell()), h.spell());
        }
        for (Spellbooks.Tablet t : Spellbooks.TABLETS) {
            assertNotNull(ItemCatalog.get(t.id()), t.id());
        }
    }

    // ---------------- Jewellery ----------------

    @Test
    void jewelleryCharges() {
        Jewellery.Worn glory4 = Jewellery.worn("amulet_of_glory_4");
        assertNotNull(glory4);
        assertEquals("amulet_of_glory_3", Jewellery.afterCharge(glory4));
        assertEquals("amulet_of_glory", Jewellery.afterCharge(Jewellery.worn("amulet_of_glory_1")));
        assertNull(Jewellery.afterCharge(Jewellery.worn("games_necklace_1")));
        assertEquals("ring_of_dueling_7", Jewellery.afterCharge(Jewellery.worn("ring_of_dueling_8")));
        assertEquals("amulet_of_glory_4", Jewellery.recharged("amulet_of_glory"));
        assertEquals(4, glory4.teleports().size());
        assertEquals("Edgeville", glory4.teleports().get(0).name());
        assertEquals(10, glory4.stats().stabAttack());
        assertEquals(6, glory4.stats().meleeStrength());
        assertEquals(3, glory4.stats().prayer());
        assertEquals(8, Jewellery.worn("amulet_of_fury").stats().meleeStrength());
        assertEquals(10, Jewellery.worn("amulet_of_torture").stats().meleeStrength());
        assertEquals(5, Jewellery.worn("necklace_of_anguish").stats().rangedStrength());
        assertEquals(5, Jewellery.worn("tormented_bracelet").stats().magicDamagePercent());
    }

    @Test
    void recoilAndEnchanting() {
        assertEquals(1, Jewellery.recoil(1));
        assertEquals(2, Jewellery.recoil(10));
        assertEquals(5, Jewellery.recoil(45));
        assertEquals(0, Jewellery.recoil(0));
        assertEquals(40, Jewellery.RECOIL_CHARGES);
        assertEquals("ring_of_recoil", Jewellery.ENCHANT.get("sapphire_ring"));
        assertEquals(7, Jewellery.ENCHANT_LEVEL.get("zenyte_amulet"));
        for (String to : Jewellery.ENCHANT.values()) {
            assertNotNull(ItemCatalog.get(to), "enchanted item " + to);
        }
    }

    // ---------------- Clue scrolls ----------------

    @Test
    void coordinateClues() {
        assertEquals("00 degrees 00 minutes north, 00 degrees 00 minutes east", ClueScrolls.coordinateText(2440, 3161));
        // 120 tiles north = 64 minutes = 1 degree 4 minutes.
        assertEquals("01 degrees 04 minutes north, 00 degrees 00 minutes east", ClueScrolls.coordinateText(2440, 3281));
        assertTrue(ClueScrolls.coordinateText(2400, 3100).endsWith("west"));
        assertTrue(ClueScrolls.coordinateText(2400, 3100).contains("south"));
        assertEquals(120, ClueScrolls.tilesFrom(1, 4));
    }

    @Test
    void anagramsArePermutations() {
        Random r = new Random(5);
        String a = ClueScrolls.anagram("Father Aereck", r);
        char[] got = a.replace(" ", "").toCharArray();
        char[] want = "FATHERAERECK".toCharArray();
        java.util.Arrays.sort(got);
        java.util.Arrays.sort(want);
        assertEquals(new String(want), new String(got));
        assertNotEquals("FATHERAERECK", a.replace(" ", ""));
    }

    @Test
    void trailsAreDeterministicAndSized() {
        List<ClueScrolls.Npc> npcs = ClueScrolls.npcs();
        assertFalse(npcs.isEmpty());
        for (ClueScrolls.Tier t : ClueScrolls.Tier.values()) {
            for (long seed = 0; seed < 30; seed++) {
                List<ClueScrolls.Step> a = ClueScrolls.generate(t, seed, npcs);
                List<ClueScrolls.Step> b = ClueScrolls.generate(t, seed, npcs);
                assertEquals(a, b);
                assertTrue(a.size() >= t.minSteps() && a.size() <= t.maxSteps(), t + " " + a.size());
                for (ClueScrolls.Step s : a) {
                    assertFalse(s.text().isEmpty());
                    if (s.item() != null) {
                        assertNotNull(ItemCatalog.get(s.item()), "fetch item " + s.item());
                    }
                    if (!s.isDig()) {
                        assertNotNull(s.npcId());
                    }
                }
            }
            assertNotNull(ItemCatalog.get(t.scrollItem()));
            assertNotNull(ItemCatalog.get(t.casketItem()));
            assertEquals(t, ClueScrolls.Tier.byScroll(t.scrollItem()));
            assertEquals(t, ClueScrolls.Tier.byCasket(t.casketItem()));
        }
    }

    @Test
    void casketRewardsExist() {
        Random r = new Random(42);
        for (ClueScrolls.Tier t : ClueScrolls.Tier.values()) {
            assertFalse(ClueRewards.uniques(t).isEmpty(), t + " uniques");
            for (String u : ClueRewards.logItems(t)) {
                assertNotNull(ItemCatalog.get(u), t + " reward " + u);
            }
            for (ClueRewards.Loot l : ClueRewards.standard(t)) {
                assertNotNull(ItemCatalog.get(l.item()), t + " loot " + l.item());
            }
            for (int i = 0; i < 300; i++) {
                List<ClueRewards.Reward> rewards = ClueRewards.roll(t, r);
                assertFalse(rewards.isEmpty());
                for (ClueRewards.Reward rw : rewards) {
                    assertNotNull(ItemCatalog.get(rw.item()), rw.item());
                    assertTrue(rw.count() > 0);
                }
            }
        }
        assertTrue(ClueRewards.megaRares(ClueScrolls.Tier.MEDIUM).contains("ranger_boots"));
        assertTrue(ClueRewards.megaRares(ClueScrolls.Tier.HARD).contains("3rd_age_platebody"));
        assertTrue(ClueRewards.megaRares(ClueScrolls.Tier.MASTER).contains("bloodhound"));
        assertTrue(ClueRewards.uniques(ClueScrolls.Tier.HARD).contains("robin_hood_hat"));
        assertTrue(ClueRewards.uniques(ClueScrolls.Tier.HARD).contains("saradomin_page_1"));
        assertTrue(ClueRewards.uniques(ClueScrolls.Tier.MEDIUM).contains("holy_sandals"));
        assertEquals(0, ClueRewards.masterClueOneIn(ClueScrolls.Tier.BEGINNER));
        assertEquals(5, ClueRewards.masterClueOneIn(ClueScrolls.Tier.ELITE));
    }

    // ---------------- Grand Exchange ----------------

    @Test
    void geMatchesAtTheOlderOffersPrice() {
        GrandExchange ge = new GrandExchange();
        GrandExchange.Offer sell = ge.create("alice", "rune_platebody", false, 10, 38000, 1);
        assertTrue(ge.place(sell).isEmpty());
        GrandExchange.Offer buy = ge.create("bob", "rune_platebody", true, 4, 40000, 2);
        List<GrandExchange.Trade> trades = ge.place(buy);
        assertEquals(1, trades.size());
        assertEquals(38000, trades.get(0).price());
        assertTrue(buy.complete());
        assertEquals(4, buy.itemsToCollect);
        // Buyer is refunded the difference.
        assertEquals(4L * 2000, buy.coinsToCollect);
        // Seller pays 2% tax.
        assertEquals(4L * (38000 - 760), sell.coinsToCollect);
        assertEquals(6, sell.remaining());
        ge.abort(sell);
        assertEquals(6, sell.itemsToCollect);
        assertTrue(sell.complete());
    }

    @Test
    void geDoesNotMatchOwnOrWorseOffers() {
        GrandExchange ge = new GrandExchange();
        ge.place(ge.create("alice", "shark", false, 100, 900, 1));
        assertTrue(ge.place(ge.create("alice", "shark", true, 10, 1000, 2)).isEmpty());
        assertTrue(ge.place(ge.create("bob", "shark", true, 10, 800, 3)).isEmpty());
        GrandExchange.Offer cheap = ge.create("carol", "shark", false, 5, 700, 4);
        List<GrandExchange.Trade> t = ge.place(cheap);
        // Carol's sell matches the best buy (Alice's own buy at 1000 is a different owner from Carol).
        assertEquals(1000, t.get(0).price());
    }

    @Test
    void geTaxAndNpcMarket() {
        assertEquals(0, GrandExchange.taxPerItem(49));
        assertEquals(1, GrandExchange.taxPerItem(50));
        assertEquals(20, GrandExchange.taxPerItem(1000));
        assertEquals(5_000_000, GrandExchange.taxPerItem(Integer.MAX_VALUE));
        assertEquals(105, GrandExchange.npcBuyPrice(100));
        assertEquals(95, GrandExchange.npcSellPrice(100));
        GrandExchange ge = new GrandExchange();
        GrandExchange.Offer buy = ge.create("solo", "lobster", true, 50, 300, 1);
        GrandExchange.Offer low = ge.create("solo", "shark", true, 5, 10, 1);
        ge.place(buy);
        ge.place(low);
        int touched = ge.npcFill(item -> item.equals("lobster") ? 268 : 1000);
        assertEquals(1, touched);
        assertTrue(buy.complete());
        assertEquals(50, buy.itemsToCollect);
        assertEquals(50L * (300 - GrandExchange.npcBuyPrice(268)), buy.coinsToCollect);
        assertFalse(low.complete());
    }

    // ---------------- Diaries ----------------

    @Test
    void diariesCoverEveryAreaAndTier() {
        assertEquals(12, Diaries.AREAS.size());
        Set<String> ids = new HashSet<>();
        for (Diaries.Task t : Diaries.tasks()) {
            assertTrue(ids.add(t.id()), t.id());
            assertNotNull(Diaries.area(t.area()), t.id());
            switch (t.type()) {
                case LEVEL, XP_IN -> {
                    Skill s = Skill.valueOf(t.target());
                    assertNotNull(s);
                    assertTrue(t.level() >= 0 && t.level() <= 99, t.id());
                }
                case VISIT -> assertEquals(3, Diaries.visitTarget(t.target()).length);
                case HAVE, MAKE -> {
                    if (!t.target().startsWith("*") && !t.target().contains("|")) {
                        assertNotNull(ItemCatalog.get(t.target()), t.id() + " item " + t.target());
                    }
                }
                default -> {
                }
            }
        }
        for (Diaries.Area a : Diaries.AREAS) {
            for (Diaries.Tier tier : Diaries.Tier.values()) {
                assertTrue(Diaries.tasks(a.id(), tier).size() >= 5, a.id() + " " + tier);
                ItemDef reward = ItemCatalog.get(a.reward(tier));
                assertNotNull(reward, a.reward(tier));
            }
        }
        for (Diaries.Tier t : Diaries.Tier.values()) {
            assertNotNull(ItemCatalog.get("antique_lamp_" + t.id()));
        }
        assertEquals(2500, Diaries.Tier.EASY.lampXp);
        assertEquals(50000, Diaries.Tier.ELITE.lampXp);
    }

    @Test
    void diaryAreasAndCompletion() {
        Diaries.Area varrock = Diaries.area("varrock");
        assertTrue(varrock.contains("MISTHALIN", 3213, 3428));
        assertFalse(varrock.contains("MISTHALIN", 3222, 3100));
        assertTrue(Diaries.area("wilderness").contains("WILDERNESS", 0, 0));
        assertTrue(Diaries.area("kourend").contains(null, 1643, 3673));
        Set<String> done = new HashSet<>();
        assertFalse(Diaries.tierComplete(done, "varrock", Diaries.Tier.EASY));
        for (Diaries.Task t : Diaries.tasks("varrock", Diaries.Tier.EASY)) {
            done.add(t.id());
        }
        assertTrue(Diaries.tierComplete(done, "varrock", Diaries.Tier.EASY));
        assertFalse(Diaries.allElite(done));
        assertTrue(Diaries.itemMatches("*potion", "super_attack_potion"));
        assertTrue(Diaries.itemMatches("*", "anything"));
        assertFalse(Diaries.itemMatches("rune_platebody", "rune_platelegs"));
        assertEquals("varrock_armour_4", varrock.reward(Diaries.Tier.ELITE));
    }

    // ---------------- Capes, run energy ----------------

    @Test
    void capesExist() {
        for (Skill s : Skill.values()) {
            ItemDef cape = ItemCatalog.get(Capes.capeId(s, false));
            ItemDef trimmed = ItemCatalog.get(Capes.capeId(s, true));
            assertNotNull(cape, s.name());
            assertNotNull(trimmed, s.name());
            assertEquals(99, cape.equipment().requirements().get(s));
            assertEquals(Jewellery.Slot.CAPE, Jewellery.worn(cape.id()).slot());
            assertEquals(9, Jewellery.worn(cape.id()).stats().stabDefence());
            assertEquals(0, Jewellery.worn(cape.id()).stats().prayer()); // only trimmed capes give +4 prayer
            assertEquals(4, Jewellery.worn(trimmed.id()).stats().prayer());
        }
        assertNotNull(ItemCatalog.get("max_cape"));
        assertNotNull(ItemCatalog.get("quest_point_cape"));
        assertNotNull(ItemCatalog.get("achievement_diary_cape"));
        assertEquals(2_277_000, Capes.MAX_CAPE_PRICE_PER_SKILL * 23);
        assertFalse(Capes.capeTeleports(Skill.CRAFTING).isEmpty());
    }

    @Test
    void runEnergy() {
        assertEquals(150, RunEnergy.ticksToEmpty(0, false));
        assertTrue(RunEnergy.ticksToEmpty(0, true) > 400);
        assertTrue(RunEnergy.ticksToEmpty(64, false) < 80);
        assertTrue(RunEnergy.regen(99, false) > RunEnergy.regen(1, false));
        assertTrue(RunEnergy.regen(99, true) > RunEnergy.regen(99, false));
    }

    @Test
    void moduleItemsHaveUniqueIdsAndBlocksPlaces() {
        SystemsContent content = new SystemsContent();
        List<String> blockIds = new ArrayList<>();
        content.blocks().forEach(b -> blockIds.add(b.id()));
        content.places().forEach(p -> p.blocks().forEach(b -> assertTrue(blockIds.contains(b), b)));
        assertEquals("systems", content.id());
        assertEquals(6, content.collectionLog().size());
    }
}
