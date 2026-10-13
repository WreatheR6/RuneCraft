package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.raids.RaidType;
import com.runecraft.osrs.content.systems.CombatAchievements.FightResult;
import com.runecraft.osrs.content.systems.CombatAchievements.Task;
import com.runecraft.osrs.content.systems.CombatAchievements.Tier;
import com.runecraft.osrs.content.systems.CombatAchievements.Track;
import com.runecraft.osrs.data.Boss;
import com.runecraft.osrs.data.Monster;
import com.runecraft.osrs.data.Untradeables;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatAchievementsTest {
    private static Task byName(String name) {
        return CombatAchievements.tasks().stream().filter(t -> t.name().equals(name)).findFirst().orElseThrow(() -> new AssertionError(name));
    }

    private static Set<String> bossIds() {
        Set<String> ids = new HashSet<>();
        for (BossDef b : ContentRegistry.bosses()) {
            ids.add(b.id);
        }
        for (Boss b : Boss.values()) {
            ids.add(b.entityId());
        }
        ids.add("barrows_chests");
        ids.add("barrows_brother");
        return ids;
    }

    private static Set<String> monsterNames() {
        Set<String> names = new HashSet<>();
        for (MonsterDef m : ContentRegistry.monsters()) {
            names.add(m.name.toLowerCase(Locale.ROOT));
        }
        for (Monster m : Monster.values()) {
            names.add(m.displayName().toLowerCase(Locale.ROOT));
        }
        return names;
    }

    @Test
    void tierCountsPointsAndThresholdsFollowTheWiki() {
        int[] counts = {41, 60, 86, 164, 174, 121};
        int[] thresholds = {41, 161, 419, 1075, 1945, 2671};
        for (Tier t : Tier.values()) {
            assertEquals(counts[t.ordinal()], CombatAchievements.tasks(t).size(), t.name());
            assertEquals(t.ordinal() + 1, t.points, t.name());
            assertEquals(thresholds[t.ordinal()], CombatAchievements.threshold(t), t.name());
        }
        assertEquals(646, CombatAchievements.tasks().size());
        assertEquals(2671, CombatAchievements.totalPoints());
        assertEquals(5000, Tier.EASY.lampXp);
        assertEquals(50000, Tier.GRANDMASTER.lampXp);
        assertEquals(70, Tier.GRANDMASTER.lampMinLevel);
    }

    @Test
    void awakenedAndDelveTasksAreTracked() {
        assertEquals(468, CombatAchievements.tasks().stream().filter(t -> t.track() != Track.UNTRACKED).count());
        for (int id : new int[]{490, 499, 508, 517, 610, 611, 612, 616, 617, 618, 619, 621, 622, 623}) {
            assertTrue(CombatAchievements.task("ca_" + id).track() != Track.UNTRACKED, "ca_" + id);
        }
        assertEquals("delve:doom_of_mokhaiotl", CombatAchievements.task("ca_622").target());
        assertEquals(999, CombatAchievements.task("ca_622").ticks()); // strictly under 10:00

        CombatAchievementRules.Metrics m = new CombatAchievementRules.Metrics();
        assertFalse(CombatAchievementRules.meets("awakened", m));
        m.awakened = true;
        assertTrue(CombatAchievementRules.meets("awakened", m));
        m.delve = 8;
        assertTrue(CombatAchievementRules.meets("delve>=8", m));
        assertFalse(CombatAchievementRules.meets("delve>=16", m));
        assertTrue(CombatAchievementRules.meets("delve=8 noprayrestore", m));
        m.prayerRestored = true;
        assertFalse(CombatAchievementRules.meets("noprayrestore", m));
        m.weapons.add("abyssal_whip");
        assertTrue(CombatAchievementRules.meets("onehand", m));
        m.weapons.add("scythe_of_vitur");
        assertFalse(CombatAchievementRules.meets("onehand", m));
    }

    @Test
    void idsAreUniqueAndStable() {
        Set<String> ids = new HashSet<>();
        Set<Integer> wiki = new HashSet<>();
        for (Task t : CombatAchievements.tasks()) {
            assertTrue(ids.add(t.id()), t.id());
            assertTrue(wiki.add(t.wikiId()), t.name());
            assertEquals("ca_" + t.wikiId(), t.id());
            assertEquals(t, CombatAchievements.task(t.id()));
            assertFalse(t.name().isBlank());
            assertFalse(t.description().isBlank());
        }
        assertEquals("ca_0", byName("Noxious Foe").id());
    }

    @Test
    void trackedTasksReferToThingsThatExist() {
        Set<String> bosses = bossIds();
        Set<String> monsters = monsterNames();
        Set<String> minigames = new HashSet<>();
        for (Minigame m : Minigame.values()) {
            minigames.add(m.id());
        }
        List<String> problems = new ArrayList<>();
        for (Task t : CombatAchievements.tasks()) {
            if (t.track() == Track.UNTRACKED) {
                assertNull(t.target(), t.name());
                continue;
            }
            assertNotNull(t.target(), t.name());
            switch (t.track()) {
                case KC -> {
                    for (String k : t.target().split("[|&]")) {
                        boolean ok = k.startsWith("mg:") ? minigames.contains(k.substring(3)) : bosses.contains(k);
                        if (!ok) {
                            problems.add(t.name() + ": no kill count " + k);
                        }
                    }
                    assertTrue(t.count() >= 1, t.name());
                }
                case KILLS -> {
                    for (String n : t.target().split("\\|")) {
                        if (!monsters.contains(n)) {
                            problems.add(t.name() + ": no monster " + n);
                        }
                    }
                }
                case RAID -> {
                    try {
                        RaidType.valueOf(t.target());
                    } catch (IllegalArgumentException e) {
                        problems.add(t.name() + ": no raid " + t.target());
                    }
                }
                case RUN -> {
                    boolean delve = t.target().startsWith("delve:") && bosses.contains(t.target().substring(6));
                    if (!delve && (!t.target().startsWith("mg:") || !minigames.contains(t.target().substring(3)))) {
                        problems.add(t.name() + ": no minigame " + t.target());
                    }
                }
                default -> {
                    for (String key : t.target().split("[|]")) {
                        boolean ok = key.startsWith("kill:") ? monsters.contains(key.substring(5)) : bosses.contains(key);
                        if (!ok) {
                            problems.add(t.name() + ": no fight target " + key);
                        }
                    }
                    if (t.track() == Track.SPEED) {
                        assertTrue(t.ticks() > 0, t.name());
                    }
                    if (t.track() == Track.WEAPON || t.track() == Track.FINISH) {
                        for (String w : t.style().split("\\|")) {
                            boolean exists = w.equals("fists") || (w.startsWith("*")
                                    ? ItemCatalog.all().stream().anyMatch(d -> d.id().contains(w.substring(1))) : ItemCatalog.get(w) != null);
                            if (!exists) {
                                problems.add(t.name() + ": no weapon " + w);
                            }
                        }
                    }
                }
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
        for (String id : List.of("zulrah_magma", "alchemical_hydra_enraged", "kalphite_queen_airborne", "dawn", "eldric_the_ice_king",
                "verzik_vitur", "olm_left_claw", "elidinis_warden")) {
            assertTrue(bosses.contains(id), id);
            assertTrue(bosses.contains(CombatAchievements.canonical(id)), id);
        }
    }

    @Test
    void enoughTasksAreTrackedToReachTheHardTier() {
        int tracked = 0;
        Set<Track> used = EnumSet.noneOf(Track.class);
        for (Task t : CombatAchievements.tasks()) {
            used.add(t.track());
            if (t.track() != Track.UNTRACKED) {
                tracked += t.points();
            }
        }
        assertTrue(tracked >= CombatAchievements.threshold(Tier.HARD), "tracked points " + tracked);
        assertEquals(EnumSet.allOf(Track.class), used);
        // Every kill-count and speed task of a boss that exists here is tracked.
        for (Task t : CombatAchievements.tasks(Tier.EASY)) {
            // (no Deranged archaeologist or Shayzien Giant Cave in this world yet)
            if (t.type() == CombatAchievements.Type.KILL_COUNT && !t.description().contains("Giant Cave") && !t.monster().startsWith("Deranged")) {
                assertTrue(t.track() == Track.KC || t.track() == Track.KILLS, t.name());
            }
        }
    }

    @Test
    void descriptionsParse() {
        assertEquals(90, CombatAchievements.parseTicks("Kill Zulrah in less than 54 seconds, without a slayer task."));
        assertEquals(150, CombatAchievements.parseTicks("Kill the Royal Titans in less than 1:30 minutes."));
        assertEquals(150, CombatAchievements.parseTicks("Kill the Phantom Muspah in less than 1 minute and 30 seconds."));
        assertEquals(133, CombatAchievements.parseTicks("Kill Zulrah in less than 1 minute 20 seconds, without a slayer task."));
        assertEquals(250, CombatAchievements.parseTicks("Kill Glough in less than 2 minutes and 30 seconds."));
        assertEquals(300, CombatAchievements.parseTicks("Kill Galvek in less than 3 minutes."));
        assertEquals(25, CombatAchievements.parseCount("Kill Zulrah 25 times."));
        assertEquals(1, CombatAchievements.parseCount("Kill Nex once."));
        assertEquals(1, CombatAchievements.parseCount("Complete the Theatre of Blood: Entry Mode 1 time."));

        Task noxious = byName("Noxious Foe");
        assertEquals(Track.KILLS, noxious.track());
        assertEquals("aberrant spectre", noxious.target());
        Task barrows = byName("Barrows Novice");
        assertEquals(Track.KC, barrows.track());
        assertEquals("barrows_chests", barrows.target());
        assertEquals(10, barrows.count());
        assertEquals(Track.KC, byName("Fight Caves Veteran").track());
        assertEquals("mg:fight_caves", byName("Fight Caves Veteran").target());
        Task fists = CombatAchievements.tasks().stream().filter(t -> t.description().equals("Kill Vorkath with only your fists."))
                .findFirst().orElseThrow();
        assertEquals(Track.WEAPON, fists.track());
        assertEquals("fists", fists.style());
        Task stab = CombatAchievements.tasks().stream().filter(t -> t.description().equals("Kill the King Black Dragon with a stab weapon."))
                .findFirst().orElseThrow();
        assertEquals(Track.STYLE, stab.track());
        assertEquals("king_black_dragon", stab.target());
        // Awakened bosses and delve levels are tracked through the awakened/delve conditions; so are raid runs and group sizes.
        for (Task t : CombatAchievements.tasks()) {
            String d = t.description();
            if (d.contains("Awakened")) {
                assertEquals(Track.RULE, t.track(), t.description());
                assertEquals("awakened", t.rule(), t.description());
            }
            if (d.contains("at delve level")) {
                assertEquals(Track.RULE, t.track(), t.description());
                assertTrue(t.rule().startsWith("delve>="), t.description());
            }
            if (d.startsWith("Complete a Chambers of Xeric") && d.contains("(Trio)")) {
                assertEquals(Track.RAID, t.track(), t.description());
            }
        }
    }

    @Test
    void pointsUnlockTiers() {
        assertNull(CombatAchievements.unlocked(40));
        assertEquals(Tier.EASY, CombatAchievements.unlocked(41));
        assertEquals(Tier.MEDIUM, CombatAchievements.unlocked(161));
        assertEquals(Tier.HARD, CombatAchievements.unlocked(1074));
        assertEquals(Tier.GRANDMASTER, CombatAchievements.unlocked(2671));
        List<String> done = new ArrayList<>();
        for (Task t : CombatAchievements.tasks(Tier.MASTER)) {
            done.add(t.id());
        }
        // Points from any tier count: all master tasks (870 points) unlock the hard tier.
        assertEquals(870, CombatAchievements.points(done));
        assertEquals(Tier.HARD, CombatAchievements.unlocked(CombatAchievements.points(done)));
    }

    @Test
    void fightConditions() {
        Task speed = CombatAchievements.tasks().stream().filter(t -> t.description().equals("Kill Vorkath in less than 54 seconds."))
                .findFirst().orElseThrow();
        FightResult quick = new FightResult(80, true, true, true, EnumSet.of(AttackType.RANGED), Set.of("dragon_hunter_crossbow"),
                "dragon_hunter_crossbow");
        FightResult slow = new FightResult(95, false, false, false, EnumSet.of(AttackType.STAB), Set.of(""), "");
        assertTrue(CombatAchievements.fightMeets(speed, quick));
        assertFalse(CombatAchievements.fightMeets(speed, slow));
        Task noDamage = byName("Alcleanical Hydra");
        assertEquals(Track.NO_DAMAGE, noDamage.track());
        assertFalse(CombatAchievements.fightMeets(noDamage, quick));
        assertTrue(CombatAchievements.fightMeets(noDamage, slow));
        Task fists = CombatAchievements.tasks().stream().filter(t -> t.description().equals("Kill Vorkath with only your fists."))
                .findFirst().orElseThrow();
        assertTrue(CombatAchievements.fightMeets(fists, slow));
        assertFalse(CombatAchievements.fightMeets(fists, quick));
        assertTrue(CombatAchievements.weaponMatches(CombatAchievements.DEMONBANE, "arclight"));
        assertTrue(CombatAchievements.weaponMatches("*spear|*hasta|*halberd", "dragon_halberd"));
        assertFalse(CombatAchievements.weaponMatches("*spear|*hasta|*halberd", "abyssal_whip"));
        assertTrue(CombatAchievements.styleAllowed("melee", AttackType.CRUSH));
        assertFalse(CombatAchievements.styleAllowed("melee", AttackType.MAGIC));
        assertTrue(CombatAchievements.styleAllowed("stab", AttackType.STAB));
        assertFalse(CombatAchievements.styleAllowed("crush", AttackType.SLASH));
        assertEquals(3, CombatAchievements.kcValue("blue_moon&eclipse_moon&blood_moon",
                k -> Map.of("blue_moon", 5, "eclipse_moon", 3, "blood_moon", 4).getOrDefault(k, 0)));
        assertEquals(7, CombatAchievements.kcValue("callisto|artio", k -> Map.of("callisto", 4, "artio", 3).getOrDefault(k, 0)));
        assertEquals("zulrah", CombatAchievements.fightKey("zulrah_magma", null));
        assertEquals("kill:greater demon", CombatAchievements.fightKey(null, "Greater demon"));
        assertTrue(CombatAchievements.killMatches("bloodveld|mutated bloodveld", "Mutated Bloodveld"));
        assertFalse(CombatAchievements.killMatches("black dragon", "Brutal black dragon"));
    }

    @Test
    void rewards() {
        // Hilts: GWD 3/5/unlimited a day, Mor Ul Rek 3/5/unlimited from the elite hilt; higher hilts keep lower teleports.
        assertEquals(1, CombatAchievements.hiltTeleports(1).size());
        assertEquals(3, CombatAchievements.hiltTeleports(1).get(0).perDay(1));
        assertEquals(5, CombatAchievements.hiltTeleports(2).get(0).perDay(2));
        assertEquals(0, CombatAchievements.hiltTeleports(3).get(0).perDay(3));
        assertEquals(1, CombatAchievements.hiltTeleports(3).size());
        assertEquals(2, CombatAchievements.hiltTeleports(4).size());
        assertEquals("Mor Ul Rek", CombatAchievements.hiltTeleports(4).get(1).dest().name());
        assertEquals(3, CombatAchievements.hiltTeleports(4).get(1).perDay(4));
        assertEquals(5, CombatAchievements.hiltTeleports(5).get(1).perDay(5));
        assertEquals(0, CombatAchievements.hiltTeleports(6).get(1).perDay(6));
        assertEquals(4, CombatAchievements.hiltTier("ghommals_hilt_4"));
        assertEquals(0, CombatAchievements.hiltTier("ghommals_hilt_9"));
        assertEquals(0, CombatAchievements.hiltTier("abyssal_whip"));
        assertEquals(40, CombatAchievements.gwdKillcount(Tier.MEDIUM));
        assertEquals(35, CombatAchievements.gwdKillcount(Tier.HARD));
        assertEquals(30, CombatAchievements.gwdKillcount(Tier.ELITE));
        assertEquals(25, CombatAchievements.gwdKillcount(Tier.MASTER));
        assertEquals(15, CombatAchievements.gwdKillcount(Tier.GRANDMASTER));
        assertEquals(0, CombatAchievements.pestControlBonus(null));
        assertEquals(1, CombatAchievements.pestControlBonus(Tier.EASY));
        assertEquals(3, CombatAchievements.pestControlBonus(Tier.MASTER));
        assertEquals(2, CombatAchievements.warriorTokenMultiplier(Tier.EASY));
        assertEquals(3, CombatAchievements.warriorTokenMultiplier(Tier.ELITE));
        assertEquals(2.0, CombatAchievements.thrallDurationMultiplier(Tier.MASTER));
        assertEquals(1.0, CombatAchievements.thrallDurationMultiplier(Tier.ELITE));
        assertEquals(1.05, CombatAchievements.clueChanceMultiplier(Tier.HARD, ClueScrolls.Tier.MEDIUM));
        assertEquals(1.0, CombatAchievements.clueChanceMultiplier(Tier.HARD, ClueScrolls.Tier.ELITE));
        assertEquals(1.0, CombatAchievements.clueChanceMultiplier(Tier.GRANDMASTER, ClueScrolls.Tier.MASTER));
        assertEquals(30, CombatAchievements.likeABossExtra(Tier.GRANDMASTER));
        assertEquals(3, CombatAchievements.tzhaarTaskKills(Tier.GRANDMASTER));
        for (Tier t : Tier.values()) {
            assertFalse(CombatAchievements.rewards(t).isEmpty());
            for (String id : List.of(t.hilt(), t.lamp())) {
                ItemDef d = ItemCatalog.get(id);
                assertNotNull(d, id);
                assertTrue(Untradeables.untradeable(id), id);
            }
            assertEquals(ItemDef.Kind.SHIELD, ItemCatalog.get(t.hilt()).kind());
        }
        for (String id : List.of(CombatAchievements.LUCKY_PENNY, "ghommals_avernic_defender_5", "ghommals_avernic_defender_6")) {
            assertNotNull(ItemCatalog.get(id), id);
            assertTrue(Untradeables.untradeable(id), id);
        }
        assertEquals(Jewellery.Slot.AMMO, Jewellery.worn(CombatAchievements.LUCKY_PENNY).slot());
        assertEquals(8, ItemCatalog.get("ghommals_avernic_defender_6").equipment().stats().meleeStrength());
    }

    @Test
    void ghommalStandsOutsideTheWarriorsGuild() {
        NpcDef npc = ContentRegistry.npc(SystemsContent.GHOMMAL);
        assertNotNull(npc);
        PlaceDef place = ContentRegistry.place(npc.place());
        assertNotNull(place);
        assertEquals(2879, place.osrsX());
        assertEquals(3547, place.osrsY());
        assertEquals(1, ContentRegistry.npcs().stream().filter(n -> n.name().equals("Ghommal")).count());
    }
}
