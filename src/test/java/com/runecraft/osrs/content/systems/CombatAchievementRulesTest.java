package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.content.raids.Invocation;
import com.runecraft.osrs.content.systems.CombatAchievementRules.Hit;
import com.runecraft.osrs.content.systems.CombatAchievementRules.Kill;
import com.runecraft.osrs.content.systems.CombatAchievementRules.Metrics;
import com.runecraft.osrs.content.systems.CombatAchievementRules.Raid;
import com.runecraft.osrs.content.systems.CombatAchievements.Task;
import com.runecraft.osrs.content.systems.CombatAchievements.Tier;
import com.runecraft.osrs.content.systems.CombatAchievements.Track;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatAchievementRulesTest {
    private static Task task(int wikiId) {
        return CombatAchievements.task("ca_" + wikiId);
    }

    @Test
    void everyRuleIsAppliedToAnUntrackedTask() {
        for (Map.Entry<Integer, CombatAchievementRules.Spec> e : CombatAchievementRules.specs().entrySet()) {
            Task t = task(e.getKey());
            assertTrue(t != null, "no task " + e.getKey());
            // A rule only fills in a task the description parser leaves untracked.
            assertEquals(e.getValue().track(), t.track(), t.name() + " (" + t.description() + ")");
            assertEquals(e.getValue().target(), t.target(), t.name());
        }
    }

    /** Every condition parses, and every item, prayer, invocation and spellbook a rule names exists. */
    @Test
    void rulesNameThingsThatExist() {
        List<String> problems = new ArrayList<>();
        for (Task t : CombatAchievements.tasks()) {
            if (t.rule() == null) {
                continue;
            }
            for (String alt : t.rule().split("\\|\\|")) {
                for (String token : alt.trim().split("\\s+")) {
                    if (token.isEmpty()) {
                        continue;
                    }
                    if (!CombatAchievementRules.knownToken(token)) {
                        problems.add(t.name() + ": bad condition " + token);
                        continue;
                    }
                    int eq = token.indexOf('=');
                    String key = eq < 0 ? token : token.substring(0, eq);
                    String value = eq < 0 ? "" : token.substring(eq + 1);
                    switch (key) {
                        case "weapon", "noweapon", "finish", "wear", "foodonly" -> {
                            for (String spec : value.split("[,/]")) {
                                boolean ok = spec.startsWith("*") ? ItemCatalog.all().stream().anyMatch(d -> d.id().contains(spec.substring(1)))
                                        : ItemCatalog.get(spec) != null;
                                if (!ok) {
                                    problems.add(t.name() + ": no item " + spec);
                                }
                            }
                        }
                        case "prayer", "noprayer" -> {
                            try {
                                Prayer.valueOf(value);
                            } catch (IllegalArgumentException e) {
                                problems.add(t.name() + ": no prayer " + value);
                            }
                        }
                        case "inv" -> {
                            for (String inv : value.split(",")) {
                                try {
                                    Invocation.valueOf(inv);
                                } catch (IllegalArgumentException e) {
                                    problems.add(t.name() + ": no invocation " + inv);
                                }
                            }
                        }
                        case "book" -> Spellbooks.Book.valueOf(value);
                        case "mode" -> assertTrue(Set.of("normal", "challenge", "entry", "hard").contains(value), t.name());
                        default -> {
                        }
                    }
                }
            }
            if (t.track() == Track.WINDOW || t.track() == Track.AVERAGE) {
                assertTrue(CombatAchievementRules.timeLimit(t.rule()) > 0, t.name());
                assertTrue(t.count() >= 2, t.name());
            }
            if (t.track() == Track.STREAK) {
                assertTrue(t.count() >= 2, t.name());
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    /** "Without taking damage from X" names attacks some boss or monster really has. */
    @Test
    void namedAttacksExist() {
        Set<String> attacks = new java.util.HashSet<>(Set.of("boss", "other", "melee", "bossmelee", "offprayer", "mechanic", "hazard",
                "poison"));
        attacks.addAll(CombatAchievementRules.SCRIPTED_ATTACKS);
        attacks.addAll(com.runecraft.osrs.content.bosses.BossMechanics.SCRIPTED_ATTACKS);
        for (AttackType t : AttackType.values()) {
            attacks.add(t.name().toLowerCase(java.util.Locale.ROOT));
        }
        List<com.runecraft.osrs.content.CombatProfile<?>> profiles = new ArrayList<>(com.runecraft.osrs.content.ContentRegistry.bosses());
        profiles.addAll(com.runecraft.osrs.content.ContentRegistry.monsters());
        for (com.runecraft.osrs.content.CombatProfile<?> p : profiles) {
            p.attacks.forEach(a -> attacks.add(CombatAchievementRules.attackKey(a.name())));
            if (p instanceof com.runecraft.osrs.content.BossDef b) {
                b.phases.forEach(ph -> ph.attacks().forEach(a -> attacks.add(CombatAchievementRules.attackKey(a.name()))));
            }
        }
        Set<String> names = new java.util.HashSet<>();
        profiles.forEach(p -> names.add(p.name.toLowerCase(java.util.Locale.ROOT)));
        List<String> problems = new ArrayList<>();
        for (Task t : CombatAchievements.tasks()) {
            if (t.rule() == null) {
                continue;
            }
            for (String token : t.rule().split("\\s+")) {
                if (token.startsWith("nodmg=")) {
                    for (String a : token.substring(6).split(",")) {
                        if (!attacks.contains(a)) {
                            problems.add(t.name() + ": no attack " + a);
                        }
                    }
                }
                if (token.startsWith("onlydmg=")) {
                    for (String a : token.substring(8).split(",")) {
                        if (!attacks.contains(a) && !names.contains(a.replace('+', ' '))) {
                            problems.add(t.name() + ": no attack or attacker " + a);
                        }
                    }
                }
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void countsMovementGearAndOtherTargets() {
        Metrics m = new Metrics();
        assertTrue(CombatAchievementRules.meets("count<=2:mole_burrow", m));
        m.count("mole_burrow", 3);
        assertFalse(CombatAchievementRules.meets("count<=2:mole_burrow", m));
        m.count("used:frozen_weapons", 1);
        assertTrue(CombatAchievementRules.meets("count<=1:used:frozen_weapons/used:winter_storm", m));
        m.count("used:winter_storm", 1);
        assertFalse(CombatAchievementRules.meets("count<=1:used:frozen_weapons/used:winter_storm", m));
        assertTrue(CombatAchievementRules.meets("count>=6:soul_negated", new Metrics()) == false);

        m.moved = 40.5;
        assertTrue(CombatAchievementRules.meets("moved<=40", m));
        m.moved = 41;
        assertFalse(CombatAchievementRules.meets("moved<=40", m));

        Metrics g = new Metrics();
        g.gear(List.of("rune_platebody", "rune_platelegs", ""));
        assertEquals(40, g.maxGearTier);
        assertTrue(g.maxWeight > 15 && g.maxWeight < 25, "weight " + g.maxWeight); // 9.979 + 9.071 kg
        assertTrue(g.maxGearValue > 50_000, "value " + g.maxGearValue);
        assertTrue(CombatAchievementRules.meets("weight<40 geartier<75 gearvalue<=2000000", g));
        g.gear(List.of("twisted_bow"));
        assertFalse(CombatAchievementRules.meets("geartier<75", g));
        assertFalse(CombatAchievementRules.meets("gearvalue<=2000000", g));
        // Equipment ItemValues doesn't weigh gets its weight from the osrsreboxed snapshot.
        assertTrue(com.runecraft.osrs.data.EquipmentWeights.size() > 300);
        assertTrue(CombatAchievementRules.gearWeight(List.of("adamant_boots")) > 1);

        Metrics o = new Metrics();
        assertTrue(CombatAchievementRules.meets("noattack=kill:ice+elemental/kill:fire+elemental", o));
        o.struck.add("kill:fire elemental");
        assertFalse(CombatAchievementRules.meets("noattack=kill:ice+elemental/kill:fire+elemental", o));
    }

    @Test
    void scriptFlagsInRulesAreReported() {
        // Every flag and counter a rule checks is one the game reports (a typo would make a task impossible or free).
        Set<String> reported = new java.util.HashSet<>(Set.of("grub_absorbed", "burrowed", "exposed", "mopping_up", "noshield", "noinv",
                "fluke", "multistyle"));
        List<java.lang.reflect.Field> fields = new ArrayList<>(List.of(CombatAchievements.class.getFields()));
        fields.addAll(List.of(com.runecraft.osrs.content.minigames.FortisColosseum.class.getFields()));
        fields.addAll(List.of(com.runecraft.osrs.content.bosses.BossMechanics.class.getFields()));
        fields.addAll(List.of(com.runecraft.osrs.content.raids.ToaLoot.class.getFields()));
        for (java.lang.reflect.Field f : fields) {
            if ((f.getName().startsWith("FLAG_") || f.getName().startsWith("COUNT_")) && f.getType() == String.class) {
                try {
                    reported.add((String) f.get(null));
                } catch (IllegalAccessException e) {
                    throw new AssertionError(e);
                }
            }
        }
        List<String> problems = new ArrayList<>();
        for (Task t : CombatAchievements.tasks()) {
            if (t.rule() == null) {
                continue;
            }
            for (String token : t.rule().split("\\s+")) {
                String name = token.startsWith("flag=") ? token.substring(5) : token.startsWith("noflag=") ? token.substring(7)
                        : token.startsWith("count") ? token.substring(token.indexOf(':') + 1) : null;
                if (name == null) {
                    continue;
                }
                for (String n : name.split("/")) {
                    if (!n.startsWith("used:") && !n.startsWith("repeat:") && !n.startsWith(CombatAchievements.FLAG_VENGEANCE_ON) && !reported.contains(n)) {
                        problems.add(t.name() + ": " + n);
                    }
                }
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void mostTasksAreTracked() {
        long tracked = CombatAchievements.tasks().stream().filter(t -> t.track() != Track.UNTRACKED).count();
        assertTrue(tracked >= 430, "tracked " + tracked);
        // Enough points to reach the elite tier through tracked tasks.
        int points = CombatAchievements.tasks().stream().filter(t -> t.track() != Track.UNTRACKED).mapToInt(Task::points).sum();
        assertTrue(points >= CombatAchievements.threshold(Tier.ELITE), "points " + points);
    }

    @Test
    void damageConditions() {
        Metrics m = new Metrics();
        m.took(new Hit(true, "cerberus", "bite", AttackType.CRUSH, 0, false));
        assertTrue(CombatAchievementRules.meets("nodmg", m));
        m.took(new Hit(true, "cerberus", "lava pool", AttackType.MAGIC, 12, false));
        assertFalse(CombatAchievementRules.meets("nodmg", m));
        assertFalse(CombatAchievementRules.meets("nodmg=lava_pool,hazard", m));
        assertTrue(CombatAchievementRules.meets("nodmg=melee", m));
        assertFalse(CombatAchievementRules.meets("nodmg=offprayer", m));
        assertTrue(CombatAchievementRules.meets("nodmg=other", m));
        m.took(new Hit(false, "spiritual mage", null, null, 5, true));
        assertFalse(CombatAchievementRules.meets("nodmg=mechanic", m));
        assertFalse(CombatAchievementRules.meets("nodmg=other", m));
        assertTrue(CombatAchievementRules.meets("nodmg=bossmelee", m));
        assertTrue(CombatAchievementRules.meets("nohitby=tztok-jad", m));
        assertTrue(CombatAchievementRules.meets("nodmg=boss || nodmg=melee", m));
        // onlydmg: every damaging hit is one of the listed attacks or attackers.
        assertFalse(CombatAchievementRules.meets("onlydmg=lava_pool", m));
        assertTrue(CombatAchievementRules.meets("onlydmg=lava_pool,mechanic", m));
        assertTrue(CombatAchievementRules.meets("onlydmg=lava_pool,spiritual+mage", m));
        assertTrue(CombatAchievementRules.meets("onlydmg=boss,other", m));
        assertFalse(CombatAchievementRules.meets("offprayerhitby=spiritual+mage", m));
        assertTrue(CombatAchievementRules.meets("offprayerhitby=cerberus", m));
    }

    @Test
    void fightConditions() {
        Metrics m = new Metrics();
        m.ticks = 833; // 8:20
        m.styles.add(AttackType.MAGIC);
        m.spells.add("earth_wave");
        m.weapons.add("dragon_hunter_wand");
        m.targetAttacks = 4;
        m.specAttacks = 4;
        m.targetHits = 3;
        m.maxPlayers = 1;
        m.maxHp = 10;
        m.minHp = 4;
        m.prayersAtEnd.add("PROTECT_FROM_MAGIC");
        m.prayersEver.add("PROTECT_FROM_MAGIC");
        m.wornAtEnd.add("dragonfire_shield");
        m.killed("kill:enormous tentacle");
        assertTrue(CombatAchievementRules.meets("t<8:21", m));
        assertFalse(CombatAchievementRules.meets("t<8:20", m));
        assertTrue(CombatAchievementRules.meets("t<=8:20", m));
        assertTrue(CombatAchievementRules.meets("style=magic spell=*earth", m));
        assertFalse(CombatAchievementRules.meets("style=melee,ranged", m));
        assertTrue(CombatAchievementRules.meets("weapon=*dragon_hunter spec hits<=4 solo maxhp<=10 minhp>=4", m));
        assertFalse(CombatAchievementRules.meets("hits<=2", m));
        assertTrue(CombatAchievementRules.meets("prayer=PROTECT_FROM_MAGIC noprayer=PROTECT_FROM_MISSILES", m));
        assertFalse(CombatAchievementRules.meets("antifire wear=anti_dragon_shield/dragonfire_shield", m));
        m.antifire = true;
        assertTrue(CombatAchievementRules.meets("antifire wear=anti_dragon_shield/dragonfire_shield", m));
        assertFalse(CombatAchievementRules.meets("killed>=4:kill:enormous+tentacle", m));
        for (int i = 0; i < 3; i++) {
            m.killed("kill:enormous tentacle");
        }
        assertTrue(CombatAchievementRules.meets("killed>=4:kill:enormous+tentacle", m));
        assertFalse(CombatAchievementRules.meets("nokill=kill:enormous+tentacle", m));
        m.eaten.add("plain_pizza");
        assertTrue(CombatAchievementRules.meets("nopotion foodonly=plain_pizza/half_plain_pizza", m));
        assertFalse(CombatAchievementRules.meets("nosupplies", m));
        // Raid conditions need a raid.
        assertFalse(CombatAchievementRules.meets("mode=challenge", m));
        m.raid = new Raid("COX", "CHALLENGE", 1, 0, Set.of(), 0, 0, 0, true, 45_000, false);
        assertTrue(CombatAchievementRules.meets("mode=challenge size=1 deathless nodeath target points>40000", m));
        m.raid = new Raid("TOA", "NORMAL", 3, 350, Set.of("INSANITY", "ON_A_DIET"), 4, 1, 0, false, 0, true);
        assertTrue(CombatAchievementRules.meets("level>=300 size>=2 inv=INSANITY path>=4 nodeath", m));
        assertFalse(CombatAchievementRules.meets("deathless", m));
        assertFalse(CombatAchievementRules.meets("nospirit", m));
        assertFalse(CombatAchievementRules.meets("inv=ON_A_DIET,DEHYDRATION", m));
    }

    @Test
    void timeLimitsAndFilters() {
        assertEquals(1000, CombatAchievementRules.parseTime("10:00"));
        assertEquals(6500, CombatAchievementRules.parseTime("65:00"));
        assertEquals(1, CombatAchievementRules.parseTime("1t"));
        assertEquals(1000, CombatAchievementRules.timeLimit("t<=10:00 fromstart"));
        assertEquals(799, CombatAchievementRules.timeLimit("t<8:00 distinct"));
        assertEquals("weapon=dragon_scimitar", CombatAchievementRules.killFilter("t<=8:00 fromstart distinct weapon=dragon_scimitar"));
        assertTrue(CombatAchievementRules.hasFlag("t<=0:10 distinct", "distinct"));
        assertEquals(1000, task(558).ticks());
    }

    @Test
    void windowsAndAverages() {
        List<Kill> kills = new ArrayList<>();
        kills.add(new Kill("araxxor", 0, 200));
        kills.add(new Kill("araxxor", 230, 450));
        kills.add(new Kill("araxxor", 480, 700));
        assertFalse(CombatAchievementRules.windowMet(kills, 4, 1000, false, true));
        kills.add(new Kill("araxxor", 720, 990));
        assertTrue(CombatAchievementRules.windowMet(kills, 4, 1000, false, true));
        assertFalse(CombatAchievementRules.windowMet(kills, 4, 980, false, true));
        // Distinct targets: two Rex kills count once.
        List<Kill> dks = List.of(new Kill("dagannoth_rex", 0, 100), new Kill("dagannoth_rex", 0, 101), new Kill("dagannoth_prime", 0, 104),
                new Kill("dagannoth_supreme", 0, 110));
        assertTrue(CombatAchievementRules.windowMet(dks, 3, 15, true, false));
        assertFalse(CombatAchievementRules.windowMet(dks, 3, 5, true, false));
        assertTrue(CombatAchievementRules.averageMet(List.of(400L, 300L, 300L, 300L, 300L), 4, 300));
        assertFalse(CombatAchievementRules.averageMet(List.of(300L, 300L, 300L, 400L), 4, 300));
        assertFalse(CombatAchievementRules.averageMet(List.of(100L), 4, 300));
    }

    @Test
    void taskShapes() {
        assertEquals(Track.RAID, task(288).track());
        assertEquals("COX", task(288).target());
        assertEquals(10, task(288).count());
        assertEquals(Track.STREAK, task(172).track());
        assertEquals(100, task(172).count());
        assertTrue(task(537).targets("eclipse_moon"));
        assertFalse(task(537).targets("eclipse"));
        assertEquals(Track.RUN, task(155).track());
        assertEquals(Track.KILLS, task(341).track());
        assertTrue(task(448).rule().contains("INSANITY") && task(448).rule().contains("BOULDERDASH"));
        assertTrue(task(252).rule().contains("veracs_flail"));
        assertTrue(task(49).rule().contains("arclight"));
    }

    @Test
    void bossMechanicTasks() {
        // Grotesque Guardians: the perfect kill needs no blinding/prison/rocks/lightning damage, no Dawn heal and one prison.
        Metrics m = new Metrics();
        m.count(CombatAchievements.COUNT_DUSK_PRISON, 1);
        assertTrue(CombatAchievementRules.meets(task(90).rule(), m));
        assertTrue(CombatAchievementRules.meets(task(89).rule(), m));
        m.count(CombatAchievements.COUNT_DUSK_PRISON, 1);
        assertFalse(CombatAchievementRules.meets(task(89).rule(), m));
        Metrics blind = new Metrics();
        blind.took(new CombatAchievementRules.Hit(true, "dusk", "blinding", null, 25, false));
        assertFalse(CombatAchievementRules.meets(task(84).rule(), blind));
        assertTrue(CombatAchievementRules.meets(task(85).rule(), blind));
        assertEquals(Track.STREAK, task(91).track());
        assertEquals(5, task(91).count());
        // Vorkath: the spawn must die (not by Crumble Undead); The Walk is judged by the script.
        Metrics v = new Metrics();
        assertFalse(CombatAchievementRules.meets(task(271).rule(), v));
        v.killed("kill:zombified spawn");
        assertTrue(CombatAchievementRules.meets(task(271).rule(), v));
        v.flags.add(CombatAchievements.FLAG_VORKATH_CRUMBLE);
        assertFalse(CombatAchievementRules.meets(task(271).rule(), v));
        // Vengeance finishers, K'ril's prayer smash, Leviathan stuns, the dark energy core, Nex's cough.
        Metrics z = new Metrics();
        assertFalse(CombatAchievementRules.meets(task(227).rule(), z));
        z.flags.add(CombatAchievements.FLAG_VENGEANCE_LAST);
        assertTrue(CombatAchievementRules.meets(task(227).rule(), z));
        assertTrue(CombatAchievementRules.meets(task(160).rule(), z));
        Metrics k = new Metrics();
        assertTrue(CombatAchievementRules.meets(task(334).rule(), k));
        k.count(CombatAchievements.COUNT_KRIL_YARR, 1);
        assertFalse(CombatAchievementRules.meets(task(334).rule(), k));
        Metrics l = new Metrics();
        l.count(CombatAchievements.COUNT_LEVIATHAN_STUN, 1);
        assertTrue(CombatAchievementRules.meets(task(510).rule(), l));
        l.count(CombatAchievements.COUNT_LEVIATHAN_STUN, 1);
        assertFalse(CombatAchievementRules.meets(task(510).rule(), l));
        Metrics c = new Metrics();
        assertTrue(CombatAchievementRules.meets(task(67).rule(), c));
        c.killed("kill:dark energy core");
        assertFalse(CombatAchievementRules.meets(task(67).rule(), c));
        Metrics n = new Metrics();
        assertFalse(CombatAchievementRules.meets(task(418).rule(), n));
        n.flags.add(CombatAchievements.FLAG_NEX_COUGHING);
        assertTrue(CombatAchievementRules.meets(task(418).rule(), n));
    }

    @Test
    void rewardHelpers() {
        assertEquals(35, CombatAchievements.bossTaskMax(35, null));
        assertEquals(40, CombatAchievements.bossTaskMax(35, Tier.EASY));
        assertEquals(65, CombatAchievements.bossTaskMax(35, Tier.GRANDMASTER));
        assertEquals(200, CombatAchievements.superiorChance(200, Tier.HARD));
        assertEquals(150, CombatAchievements.superiorChance(200, Tier.ELITE));
        assertEquals(1000, CombatAchievements.imbueCost(1000, Tier.MEDIUM));
        assertEquals(500, CombatAchievements.imbueCost(1000, Tier.HARD));
        assertFalse(CombatAchievements.barrowsDrainNullified(Tier.EASY, "ghommals_hilt_1"));
        assertTrue(CombatAchievements.barrowsDrainNullified(Tier.MEDIUM, "ghommals_hilt_1"));
        assertTrue(CombatAchievements.barrowsDrainNullified(Tier.MASTER, "ghommals_avernic_defender_5"));
        assertFalse(CombatAchievements.barrowsDrainNullified(Tier.MASTER, "avernic_defender"));
        assertFalse(CombatAchievements.barrowsDrainNullified(Tier.MASTER, ""));
    }
}
