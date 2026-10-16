package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.Spell;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.minigames.EmirsArena.Build;
import com.runecraft.osrs.content.minigames.EmirsArena.Options;
import com.runecraft.osrs.content.minigames.EmirsArena.Rule;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.osrs.content.systems.Spellbooks;
import com.runecraft.osrs.data.BlightedSacks;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Round 31: blighted spell sacks, Emir's Arena ranked builds and fun weapons, the soul cape, Yt-HurKot heals, Sol Heredit. */
class MinigamesRound31Test {
    // ------------------------------------------------------------------ blighted spell sacks

    @Test
    void blightedSacksCoverTheirWikiSpells() {
        for (Spell s : Spell.values()) {
            String n = s.name();
            boolean waveOrSurge = n.endsWith("_WAVE") || n.endsWith("_SURGE");
            assertEquals(waveOrSurge ? BlightedSacks.SURGE : null, BlightedSacks.sackFor(n), n);
        }
        Map<String, String> book = Map.ofEntries(Map.entry("bind", BlightedSacks.ENTANGLE), Map.entry("snare", BlightedSacks.ENTANGLE),
                Map.entry("entangle", BlightedSacks.ENTANGLE), Map.entry("ice_rush", BlightedSacks.ANCIENT_ICE),
                Map.entry("ice_burst", BlightedSacks.ANCIENT_ICE), Map.entry("ice_blitz", BlightedSacks.ANCIENT_ICE),
                Map.entry("ice_barrage", BlightedSacks.ANCIENT_ICE), Map.entry("tele_block", BlightedSacks.TELEPORT),
                Map.entry("vengeance", BlightedSacks.VENGEANCE), Map.entry("vengeance_other", BlightedSacks.VENGEANCE));
        book.forEach((spell, sack) -> {
            assertNotNull(Spellbooks.get(spell), spell + " is a spell");
            assertEquals(sack, BlightedSacks.sackFor(spell), spell);
        });
        for (String other : new String[]{"smoke_barrage", "blood_barrage", "fire_blast", "teleblock", "crumble_undead", "heal_other"}) {
            assertNull(BlightedSacks.sackFor(other), other);
        }
        assertNull(BlightedSacks.sackFor(null));
        for (String sack : BlightedSacks.ALL) {
            assertNotNull(ItemCatalog.get(sack), sack + " is an item");
        }
    }

    @Test
    void blightedSacksWorkInTheWildernessCastleWarsAndSoulWars() {
        for (String sack : BlightedSacks.ALL) {
            assertTrue(BlightedSacks.usable(sack, 1, null), sack);
            assertTrue(BlightedSacks.usable(sack, 56, Minigame.CASTLE_WARS.id()), sack);
            assertFalse(BlightedSacks.usable(sack, 0, null), sack);
            assertFalse(BlightedSacks.usable(sack, 0, Minigame.LAST_MAN_STANDING.id()), sack);
            assertFalse(BlightedSacks.usable(sack, 0, Minigame.EMIRS_ARENA.id()), sack);
            boolean teleport = sack.equals(BlightedSacks.TELEPORT);
            assertEquals(!teleport, BlightedSacks.usable(sack, 0, Minigame.CASTLE_WARS.id()), sack);
            assertEquals(!teleport, BlightedSacks.usable(sack, 0, Minigame.SOUL_WARS.id()), sack);
        }
        assertFalse(BlightedSacks.usable(null, 30, null));
    }

    // ------------------------------------------------------------------ Emir's Arena

    @Test
    void rankedBuildsMatchTheWikiAfterTheMarch2026Update() {
        int[][] expected = {
                // attack, strength, defence, hitpoints, ranged, magic, prayer
                {99, 99, 99, 99, 99, 99, 99}, // Max/Med
                {75, 99, 50, 99, 99, 99, 63}, // Zerker
                {60, 99, 1, 99, 99, 99, 52},  // 1 Def Pure
        };
        Skill[] order = {Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.HITPOINTS, Skill.RANGED, Skill.MAGIC, Skill.PRAYER};
        for (Build b : Build.values()) {
            for (int i = 0; i < order.length; i++) {
                assertEquals(expected[b.ordinal()][i], b.level(order[i]), b + " " + order[i]);
            }
            assertEquals(order.length, LastManStanding.BUILD_SKILLS.size());
            assertTrue(LastManStanding.BUILD_SKILLS.containsAll(Set.of(order)));
            assertEquals(b.name(), b.kit.name(), "kit of the same build");
            LastManStanding.Loadout kit = EmirsArena.loadout(b);
            for (String id : kit.worn()) {
                assertNotNull(ItemCatalog.get(id), id);
            }
            kit.carried().forEach(st -> assertNotNull(ItemCatalog.get(st.item()), st.item()));
        }
        assertEquals("Max/Med", Build.MAX_MED.displayName);
        assertEquals("1 Def Pure", Build.PURE.displayName);
    }

    @Test
    void rankedBuildRotatesEveryWednesday() {
        long start = LocalDate.of(2022, 7, 27).toEpochDay();
        assertEquals(start, EmirsArena.ROTATION_START_EPOCH_DAY);
        assertEquals(Build.MAX_MED, EmirsArena.weeklyBuild(start));
        assertEquals(Build.MAX_MED, EmirsArena.weeklyBuild(start + 6));
        assertEquals(Build.ZERKER, EmirsArena.weeklyBuild(start + 7));
        assertEquals(Build.PURE, EmirsArena.weeklyBuild(start + 14));
        assertEquals(Build.MAX_MED, EmirsArena.weeklyBuild(start + 21));
        assertEquals(Build.PURE, EmirsArena.weeklyBuild(start - 1), "the week before the first is the third build");
        // Every week change falls on a Wednesday, and each build lasts exactly seven days.
        for (long d = start; d < start + 400; d++) {
            boolean change = EmirsArena.weeklyBuild(d) != EmirsArena.weeklyBuild(d - 1);
            assertEquals(LocalDate.ofEpochDay(d).getDayOfWeek() == java.time.DayOfWeek.WEDNESDAY, change, LocalDate.ofEpochDay(d).toString());
        }
    }

    @Test
    void funWeaponsDuelsCanBeFoughtWithTheCrierBell() {
        Options fun = new Options(EnumSet.of(Rule.FUN_WEAPONS), EnumSet.noneOf(EmirsArena.DuelSlot.class));
        assertTrue(EmirsArena.weaponAllowed(fun, "crier_bell"));
        assertFalse(EmirsArena.weaponAllowed(fun, "abyssal_whip"));
        assertFalse(EmirsArena.weaponAllowed(fun, ""), "fists are not a fun weapon");
        ItemDef bell = ItemCatalog.get("crier_bell");
        assertNotNull(bell);
        assertNotNull(bell.equipment());
        assertEquals(EquipmentDef.Slot.WEAPON, bell.equipment().slot(), "the crier bell is wielded");
        assertTrue(bell.equipment().stats().crushAttack() < 0, "fun weapons have negative bonuses");
        // The wiki's exclusions.
        for (String excluded : new String[]{"cursed_goblin_hammer", "hunting_knife", "nutcracker_staff", "skis"}) {
            assertFalse(EmirsArena.FUN_WEAPONS.contains(excluded), excluded);
        }
        assertEquals(EmirsArena.FUN_WEAPONS.size(), new HashSet<>(EmirsArena.FUN_WEAPONS).size(), "no duplicates");
    }

    // ------------------------------------------------------------------ Soul Wars, TzHaar, Colosseum

    @Test
    void soulCapeNeedsSeventyPrayerToBuyAndWear() {
        RewardShop.Entry cape = RewardShops.get("soul_wars").entries().stream().filter(e -> e.item().equals("soul_cape"))
                .findFirst().orElseThrow();
        assertEquals(Skill.PRAYER, cape.requiredSkill());
        assertEquals(70, cape.requiredLevel());
        assertEquals(2500, cape.costs().get(0).amount());
        assertEquals(Map.of(Skill.PRAYER, 70), Jewellery.wearLevels("soul_cape"));
        assertEquals(Map.of(), Jewellery.wearLevels("amulet_of_glory"));
    }

    @Test
    void ytHurKotHealAsMuchAsModAshSaid() {
        Random r = new Random(31);
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < 2000; i++) {
            assertEquals(5, TzHaarWaves.hurKotHeal(TzHaarWaves.JAD, r));
            int h = TzHaarWaves.hurKotHeal(TzHaarWaves.JALTOK_JAD, r);
            assertTrue(h >= 15 && h <= 24, "JalTok-Jad heal " + h);
            seen.add(h);
        }
        assertEquals(10, seen.size(), "every heal from 15 to 24 happens");
        assertEquals(4, TzHaarWaves.HURKOT_HEAL_TICKS);
    }

    @Test
    void shadeCatacombsBelowMorttonHaveKeyChests() {
        List<String> chests = com.runecraft.osrs.content.dungeons.ShadeCatacombs.CHESTS;
        assertEquals(ShadesOfMortton.METALS.length, chests.size());
        Set<String> blocks = new HashSet<>();
        ContentRegistry.blocks().forEach(b -> blocks.add(b.id()));
        Set<String> placed = new HashSet<>();
        for (com.runecraft.osrs.world.StructureDef s : ContentRegistry.structures()) {
            s.ops().forEach(op -> placed.add(op.block()));
        }
        for (String chest : chests) {
            assertTrue(blocks.contains(chest), chest + " is a registered block");
            assertTrue(placed.contains("runecraft:" + chest), chest + " stands in the catacombs");
        }
    }

    @Test
    void solHereditHasNoSummoningArena() {
        BossDef sol = ContentRegistry.boss(FortisColosseum.SOL_HEREDIT);
        assertNotNull(sol);
        assertTrue(sol.raidOnly, "Sol Heredit is only fought as wave 12, so no summoning stone or surface arena");
    }
}
