package com.runecraft.osrs;

import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.systems.Spellbooks;
import com.runecraft.osrs.data.Boss;
import com.runecraft.osrs.data.Potion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Single/multi-way combat, poison and venom, Saradomin brews, curse and binding spells, Tele Block, NPC attack bonuses. */
class CombatRulesTest {
    // ---------------------------------------------------------------- multi-way areas

    @Test
    void multiAreasResolve() {
        for (MultiCombat.Around a : MultiCombat.AROUND) {
            assertNotNull(MultiCombat.placeCentre(a.place()), "unknown place for multi area " + a.name() + ": " + a.place());
        }
        for (String boss : MultiCombat.MULTI_BOSS_ARENAS) {
            com.runecraft.osrs.world.ArenaLayout.Arena arena = com.runecraft.osrs.world.ArenaLayout.of(boss);
            assertNotNull(arena, "no arena for multi boss " + boss);
            assertTrue(MultiCombat.isMultiAtBlock(arena.x(), arena.z()), boss + " arena centre");
            assertTrue(MultiCombat.isMultiAtBlock(arena.x() + arena.radius(), arena.z() - arena.radius()), boss + " arena edge");
        }
        assertEquals(MultiCombat.BOXES.size() + MultiCombat.AROUND.size() + MultiCombat.MULTI_BOSS_ARENAS.size(),
                MultiCombat.areas().size());
    }

    @Test
    void singleWayByDefault() {
        assertFalse(MultiCombat.isMulti(3222, 3218), "Lumbridge");
        assertFalse(MultiCombat.isMulti(3213, 3428), "Varrock");
        assertFalse(MultiCombat.isMulti(3093, 3493), "Edgeville");
        assertFalse(MultiCombat.isMulti(2895, 3445), "Taverley");
        assertFalse(MultiCombat.isMulti(2890, 3676), "Trollheim");
        assertFalse(MultiCombat.isMulti(3100, 3560), "low Wilderness west of Edgeville");
        assertFalse(MultiCombat.isMultiAtBlock(0, 0), "Lumbridge at block (0, 0)");
    }

    @Test
    void multiAreas() {
        assertTrue(MultiCombat.isMulti(2882, 5311), "God Wars Dungeon"); // at its real tiles (GwdDungeon)
        assertTrue(MultiCombat.isMulti(2832, 5302), "Kree'arra's chamber");
        assertTrue(MultiCombat.isMulti(2925, 5203), "Nex's chamber");
        assertTrue(MultiCombat.isMulti(3127, 3833), "Revenant Caves");
        assertTrue(MultiCombat.isMulti(1636, 3673), "Catacombs of Kourend");
        assertTrue(MultiCombat.isMulti(3292, 3848), "Callisto's den");
        assertTrue(MultiCombat.isMulti(3266, 3912), "Rogues' Castle (Chaos Elemental)");
        assertTrue(MultiCombat.isMulti(3226, 3108), "Kalphite Lair");
        assertTrue(MultiCombat.isMulti(3020, 3620), "Dark Warriors' Fortress");
        com.runecraft.osrs.content.godwars.GwdDungeon.Spot gwd = com.runecraft.osrs.content.godwars.GwdDungeon.LANDING;
        assertTrue(MultiCombat.isMultiAtBlock(GielinorMap.toBlockX(gwd.x()), GielinorMap.toBlockZ(gwd.y())));
    }

    // ---------------------------------------------------------------- single-way rules

    @Test
    void npcWontJoinASingleWayFight() {
        SingleCombat sc = new SingleCombat();
        int player = 1;
        int goblin = 2;
        int guard = 3;
        sc.onHit(goblin, player, 100);
        assertEquals(SingleCombat.Verdict.TARGET_BUSY, sc.check(guard, player, 103, false, false, id -> true));
        assertEquals(SingleCombat.Verdict.OK, sc.check(guard, player, 103, true, false, id -> true), "multi");
        assertEquals(SingleCombat.Verdict.OK, sc.check(goblin, player, 103, false, false, id -> true), "same attacker");
        // 8 ticks after the goblin's last hit another monster may attack.
        assertEquals(SingleCombat.Verdict.TARGET_BUSY, sc.check(guard, player, 107, false, false, id -> true));
        assertEquals(SingleCombat.Verdict.OK, sc.check(guard, player, 108, false, false, id -> true));
        // A dead/despawned goblin no longer blocks anyone.
        assertEquals(SingleCombat.Verdict.OK, sc.check(guard, player, 101, false, false, id -> id != goblin));
    }

    @Test
    void playerCantStealOrSwitchTargets() {
        SingleCombat sc = new SingleCombat();
        int alice = 1;
        int bob = 2;
        int cow = 3;
        int rat = 4;
        sc.onHit(alice, cow, 50);
        assertEquals(SingleCombat.Verdict.TARGET_BUSY, sc.check(bob, cow, 52, false, false, null));
        assertEquals("Someone else is fighting that.", SingleCombat.Verdict.TARGET_BUSY.message);
        assertEquals(SingleCombat.Verdict.OK, sc.check(alice, cow, 52, false, false, null));
        // A monster attacking Bob means Bob can't start on a different monster.
        sc.onHit(rat, bob, 60);
        assertEquals(SingleCombat.Verdict.ATTACKER_BUSY, sc.check(bob, 99, 61, false, false, null));
        assertEquals(SingleCombat.Verdict.OK, sc.check(bob, rat, 61, false, false, null), "fighting back is fine");
        // A monster that is attacking Bob can't be attacked by Alice either.
        assertEquals(SingleCombat.Verdict.TARGET_BUSY, sc.check(alice, rat, 61, false, false, null));
        // PvP combat lasts 10 ticks.
        sc.onHit(alice, bob, 200);
        assertEquals(SingleCombat.Verdict.TARGET_BUSY, sc.check(99, bob, 209, false, true, null));
        assertEquals(SingleCombat.Verdict.OK, sc.check(99, bob, 210, false, true, null));
        sc.prune(1000);
        assertNull(sc.state(bob));
    }

    // ---------------------------------------------------------------- poison / venom

    @Test
    void poisonDecays() {
        Toxins.State s = new Toxins.State();
        assertTrue(Toxins.poison(s, 6, 0));
        assertFalse(Toxins.poison(s, 4, 1), "weaker poison doesn't replace stronger");
        int[] expected = {6, 6, 6, 6, 6, 5, 5, 5, 5, 5, 4};
        long tick = 0;
        for (int e : expected) {
            tick += Toxins.HIT_INTERVAL;
            assertEquals(e, Toxins.tick(s, tick));
        }
        assertEquals(0, Toxins.tick(s, tick + 1), "hits only every 30 ticks");
    }

    @Test
    void venomRampsTo20() {
        Toxins.State s = new Toxins.State();
        Toxins.poison(s, 4, 0);
        assertTrue(Toxins.venom(s, 0), "venom overrides poison");
        assertFalse(Toxins.poison(s, 10, 1), "poison doesn't override venom");
        int[] expected = {6, 8, 10, 12, 14, 16, 18, 20, 20};
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], Toxins.tick(s, (i + 1L) * Toxins.HIT_INTERVAL));
        }
    }

    @Test
    void antipoisonTurnsVenomIntoPoison() {
        Toxins.State s = new Toxins.State();
        Toxins.venom(s, 0);
        Toxins.tick(s, 30);
        Toxins.tick(s, 60);
        assertEquals(Toxins.Cure.VENOM_TO_POISON, Toxins.cure(s, false, Toxins.ANTIPOISON[0], 0, 61));
        assertTrue(s.poisoned());
        assertFalse(s.envenomed());
        assertEquals(Toxins.VENOM_TO_POISON, s.damage());
        assertFalse(s.poisonImmune(61), "no immunity from the conversion");
        // A second dose cures the poison and gives 90 s of poison immunity.
        assertEquals(Toxins.Cure.CURED, Toxins.cure(s, false, Toxins.ANTIPOISON[0], 0, 62));
        assertTrue(s.poisonImmune(62 + 149));
        assertFalse(s.poisonImmune(62 + 150));
        assertFalse(s.venomImmune(62), "antipoisons never protect from venom");
        assertTrue(Toxins.venom(s, 70));
    }

    @Test
    void antiVenomHasSeparateTimers() {
        Toxins.State s = new Toxins.State();
        Toxins.venom(s, 0);
        assertEquals(Toxins.Cure.CURED, Toxins.cure(s, true, Toxins.ANTI_VENOM_PLUS[0], Toxins.ANTI_VENOM_PLUS[1], 10));
        assertFalse(s.active());
        assertFalse(Toxins.venom(s, 10 + 359), "venom immunity ~3.6 minutes");
        assertTrue(Toxins.venom(s, 10 + 360));
        Toxins.cure(s, true, 0, 0, 400);
        assertFalse(Toxins.poison(s, 6, 10 + 1499), "poison immunity 15 minutes");
        assertTrue(Toxins.poison(s, 6, 10 + 1500));
        assertTrue(Toxins.ANTI_VENOM[0] > Toxins.ANTI_VENOM[1], "anti-venom: long poison, short venom immunity");
    }

    // ---------------------------------------------------------------- Saradomin brew

    @Test
    void saradominBrewOverheals() {
        assertEquals(16, CombatEffects.brewHeal(99));
        assertEquals(115, CombatEffects.brewCap(99));
        assertEquals(115, CombatEffects.afterBrew(99, 99), "can boost above 99");
        assertEquals(115, CombatEffects.afterBrew(110, 99), "capped at level + 16");
        assertEquals(66, CombatEffects.afterBrew(50, 99));
        assertEquals(120, CombatEffects.afterBrew(120, 99), "never lowers a higher boost");
        SkillSet skills = new SkillSet();
        for (Skill s : new Skill[]{Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC, Skill.HITPOINTS}) {
            skills.setXpTenths(s, Experience.xpForLevel(99) * 10);
        }
        assertTrue(Potion.SARADOMIN_BREW.overheals());
        assertEquals(16, Potion.SARADOMIN_BREW.heal(skills));
        Potion.SARADOMIN_BREW.apply(skills);
        assertEquals(99 + 21, skills.currentLevel(Skill.DEFENCE), "defence +20% +2");
        assertEquals(99 - 11, skills.currentLevel(Skill.ATTACK), "attack -10% -2");
        assertEquals(99 - 11, skills.currentLevel(Skill.STRENGTH));
        assertEquals(99 - 11, skills.currentLevel(Skill.RANGED));
        assertEquals(99 - 11, skills.currentLevel(Skill.MAGIC));
        Potion.SARADOMIN_BREW.apply(skills);
        assertEquals(88 - 10, skills.currentLevel(Skill.ATTACK), "second dose drains 10% of the drained level + 2");
        assertEquals(120, skills.currentLevel(Skill.DEFENCE), "defence boost doesn't stack");
    }

    // ---------------------------------------------------------------- curses, binds, tele block

    @Test
    void curseSpellsDrainTheRightStat() {
        assertEquals(Skill.ATTACK, CombatEffects.curse("confuse").skill());
        assertEquals(Skill.STRENGTH, CombatEffects.curse("weaken").skill());
        assertEquals(Skill.DEFENCE, CombatEffects.curse("curse").skill());
        assertEquals(Skill.DEFENCE, CombatEffects.curse("vulnerability").skill());
        assertEquals(10, CombatEffects.curse("vulnerability").percent());
        assertEquals(Skill.STRENGTH, CombatEffects.curse("enfeeble").skill());
        assertEquals(Skill.ATTACK, CombatEffects.curse("stun").skill());
        assertEquals(10, CombatEffects.curse("stun").percent());
        assertNull(CombatEffects.curse("wind_strike"));
        assertEquals(Spellbooks.Effect.DRAIN_STRENGTH, Spellbooks.get("weaken").effect());
        assertEquals(Spellbooks.Effect.DRAIN_STRENGTH, Spellbooks.get("enfeeble").effect());
        assertEquals(Spellbooks.Effect.DRAIN_ATTACK, Spellbooks.get("stun").effect());
        assertEquals(Spellbooks.Effect.DRAIN_DEFENCE, Spellbooks.get("vulnerability").effect());
    }

    @Test
    void cursesDontStack() {
        assertEquals(15, CombatEffects.curseDrain(150, 150, 10), "Vulnerability on 150 Defence");
        assertEquals(0, CombatEffects.curseDrain(150, 135, 10), "already lowered by 10%");
        assertEquals(0, CombatEffects.curseDrain(150, 100, 10), "already lowered more");
        assertEquals(5, CombatEffects.curseDrain(150, 140, 10), "tops up to a 10% drain");
        assertEquals(1, CombatEffects.curseDrain(10, 10, 5), "at least one level");
        assertEquals(7, CombatEffects.curseDrain(150, 150, 5), "Curse 5%");
    }

    @Test
    void bindingSpells() {
        assertEquals(0, Spellbooks.get("bind").maxHit());
        assertEquals(3, Spellbooks.get("snare").maxHit());
        assertEquals(5, Spellbooks.get("entangle").maxHit());
        for (String id : new String[]{"bind", "snare", "entangle"}) {
            int[] b = CombatEffects.binding(id);
            assertEquals(b[0], Spellbooks.get(id).maxHit(), id);
            assertEquals(b[1], Spellbooks.get(id).param(), id);
        }
        assertEquals(8, CombatEffects.bindTicks(16, true), "halved by Protect from Magic");
    }

    @Test
    void teleBlock() {
        Spellbooks.Spell tb = Spellbooks.get("tele_block");
        assertNotNull(tb);
        assertEquals(85, tb.level());
        assertEquals(Spellbooks.Effect.TELEBLOCK, tb.effect());
        assertEquals(Integer.valueOf(1), tb.runes().get("law_rune"));
        assertEquals(Integer.valueOf(1), tb.runes().get("chaos_rune"));
        assertEquals(Integer.valueOf(1), tb.runes().get("death_rune"));
        assertEquals(500, CombatEffects.teleblockTicks(false), "5 minutes");
        assertEquals(250, CombatEffects.teleblockTicks(true), "2.5 minutes with Protect from Magic");
        CombatEffects.Teleblock state = new CombatEffects.Teleblock();
        assertFalse(state.blocked(0));
        assertTrue(state.apply(0, true));
        assertTrue(state.blocked(249));
        assertFalse(state.blocked(250));
        assertFalse(state.apply(100, false), "already blocked");
        assertFalse(state.apply(300, false), "immune for a minute after it ends");
        assertTrue(state.apply(350, false));
        assertEquals("5:00", CombatEffects.minutes(state.remaining(350)));
    }

    // ---------------------------------------------------------------- NPC attack bonuses

    @Test
    void npcAttackBonuses() {
        assertEquals(120, Boss.GENERAL_GRAARDOR.attackBonus(AttackType.CRUSH));
        assertEquals(100, Boss.GENERAL_GRAARDOR.attackBonus(AttackType.RANGED));
        assertEquals(160, Boss.KRIL_TSUTSAROTH.attackBonus(AttackType.SLASH));
        assertEquals(60, Boss.TZTOK_JAD.attackBonus(AttackType.MAGIC));
        assertEquals(0, Boss.OBOR.attackBonus(AttackType.CRUSH), "defaults to 0");
        BossDef zily = ContentRegistry.boss("commander_zilyana");
        assertEquals(195, zily.attackBonus(AttackType.SLASH));
        assertEquals(200, zily.attackBonus(AttackType.MAGIC));
        BossDef kree = ContentRegistry.boss("kreearra");
        assertEquals(120, kree.attackBonus(AttackType.RANGED));
        BossDef nex = ContentRegistry.boss("nex");
        assertEquals(200, nex.attackBonus(AttackType.STAB));
        assertEquals(100, nex.attackBonus(AttackType.MAGIC));
        assertEquals(150, nex.attackBonus(AttackType.RANGED));
        assertEquals(50, ContentRegistry.boss("cerberus").attackBonus(AttackType.MAGIC));
        assertEquals(50, ContentRegistry.boss("zulrah").attackBonus(AttackType.RANGED));
        // The bonus feeds the NPC attack roll: (level + 9) * (bonus + 64).
        assertEquals((255L + 9) * (120 + 64), CombatFormulas.npcRoll(255, Boss.GENERAL_GRAARDOR.attackBonus(AttackType.CRUSH)));
    }
}
