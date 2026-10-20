package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import net.minecraft.server.level.ServerPlayer;

/**
 * Entry points of the in-game self-test ({@code boss:} checks in {@code selftest/GameplayChecks}) into the package-private
 * boss scripts: each drives one scripted mechanic on a freshly spawned boss and returns a summary, or throws.
 */
public final class BossSelfTests {
    private BossSelfTests() {
    }

    /**
     * The Nightmare or Phosani's Nightmare walked through her phases ({@code boss:walk:<id>}): asleep, then each phase's
     * shield, specials, totems, blast and sleepwalkers, Phosani's desperation phase, the kill and its loot.
     */
    public static String nightmareWalk(DataBossEntity boss, ServerPlayer p) {
        return NightmareMuspahWalks.nightmare(boss, p);
    }

    /**
     * The Phantom Muspah walked through its fight ({@code boss:walk:phantom_muspah}): form swaps and their spikes, both
     * specials, the shockwave behind a spike, the prayer shield and its drains, the spikes after it, the kill.
     */
    public static String muspahWalk(DataBossEntity boss, ServerPlayer p) {
        return NightmareMuspahWalks.muspah(boss, p);
    }

    /** Araxxor's nine eggs: hatching order and destroyed eggs. */
    public static String araxxorEggs(DataBossEntity boss, ServerPlayer p) {
        return SlayerScripts.selfTestAraxxor(boss, p);
    }

    /** The Abyssal Sire's spawns mature into scions after 15 seconds and die with it. */
    public static String sireSpawns(DataBossEntity boss, ServerPlayer p) {
        return SlayerScripts.selfTestSire(boss, p);
    }

    /** The Kraken's whirlpools: disturbed in order, then it rises; ranged hits deal a seventh. */
    public static String krakenWhirlpools(DataBossEntity boss, ServerPlayer p) {
        return SlayerScripts.selfTestKraken(boss, p);
    }

    /** The Eclipse Moon's clones: a faced clone is parried, one behind the player hits. */
    public static String eclipseClones(DataBossEntity boss, ServerPlayer p) {
        return MoonScripts.selfTestEclipse(boss, p);
    }

    /** A dossier (Yama) opens into a contract or chasm teleport scrolls. */
    public static String yamaDossier(DataBossEntity boss, ServerPlayer p) {
        com.runecraft.util.Inv.give(p, "dossier", 1);
        String got = BossesHooks.openDossier(p);
        if (got == null || !com.runecraft.util.Inv.has(p, got, 1) || com.runecraft.util.Inv.has(p, "dossier", 1)) {
            throw new IllegalStateException("the dossier didn't open (" + got + ")");
        }
        com.runecraft.util.Inv.remove(p, got, com.runecraft.util.Inv.count(p, got));
        return "dossier gave " + got;
    }

    /** Zalcano's armour only takes imbued tephra; a golem killed with tephra leaves 16-24 imbued tephra. */
    public static String zalcanoTephra(DataBossEntity boss, ServerPlayer p) {
        return ClassicScripts.selfTestZalcano(boss, p);
    }

    /** Zulrah through a whole rotation: phases, forms, spots, clouds and snakelings, then the next rotation's start. */
    public static String zulrahRotation(DataBossEntity boss, ServerPlayer p) {
        return ClassicBossWalks.zulrahRotation(boss, p);
    }

    /** Vorkath's acid phase, zombified spawn (killed and left to explode) and dragonfire bomb. */
    public static String vorkathSpecials(DataBossEntity boss, ServerPlayer p) {
        return ClassicBossWalks.vorkathSpecials(boss, p);
    }

    /** The Kalphite Queen's crawling form shedding its shell and the airborne form's kill. */
    public static String kalphiteQueenForms(DataBossEntity boss, ServerPlayer p) {
        return ClassicBossWalks.kalphiteQueenForms(boss, p);
    }

    /** The Corporeal Beast's damage rule, splash orb, stomp and dark energy core. */
    public static String corpMechanics(DataBossEntity boss, ServerPlayer p) {
        return ClassicBossWalks.corpMechanics(boss, p);
    }

    /** Sarachnis's healing strikes, web and spawns. */
    public static String sarachnisWeb(DataBossEntity boss, ServerPlayer p) {
        return ClassicBossWalks.sarachnisWeb(boss, p);
    }

    /** Scurrius's food piles, centre phase, bricks and rat waves. */
    public static String scurriusFood(DataBossEntity boss, ServerPlayer p) {
        return ClassicBossWalks.scurriusFood(boss, p);
    }

    /** The Hueycoatl's body parts block the head until they are down. */
    public static String hueycoatlPhases(DataBossEntity boss, ServerPlayer p) {
        return EncounterScripts.selfTestHueycoatl(boss, p);
    }

    /**
     * The fight self-test ({@code boss:fight:<id>}) asks this while a boss can't be hurt: does the part of its mechanic a
     * player would do that isn't killing its adds (the test clears those itself), and returns what it did, or null.
     */
    public static String assist(DataBossEntity boss, ServerPlayer p) {
        if (boss.script() instanceof SlayerScripts.Kraken k) {
            return k.selfTestDisturb(p) ? "disturbed the whirlpools" : null;
        }
        if (boss.script() instanceof ClassicScripts.Zalcano) {
            return ClassicScripts.selfTestAssistZalcano(boss, p);
        }
        if (boss.script() instanceof NightmareScript n && n.stage == NightmareScript.Stage.ASLEEP) {
            n.wake(boss); // disturbed: no need to wait out her sleep
            return "woke her";
        }
        if (boss.script() instanceof EncounterScripts.Duke duke && duke.asleep()) {
            for (int i = 0; i < com.runecraft.osrs.content.bosses.DukeRules.poisonsToWake(boss.awakened()); i++) {
                duke.feed(boss, p, com.runecraft.osrs.content.bosses.DukeRules.POISON_VALUE);
            }
            return "woke the Duke with arder-musca poison";
        }
        return null;
    }

    /**
     * Loot a kill leaves somewhere other than the ground or the inventory: the Doom of Mokhaiotl's delve loot waits in the
     * burrow hole until claimed at the stone; Araxxor's waits on its corpse until harvested. Returns the number of stacks held
     * for the player.
     */
    public static int heldLoot(String bossId, ServerPlayer p) {
        if (AraxxorCorpse.BOSS.equals(bossId)) {
            return AraxxorCorpse.held(p);
        }
        if (com.runecraft.osrs.content.bosses.MoonsRules.MOONS.contains(bossId)) {
            return LunarChest.pending(p); // a Moon's loot waits in the Lunar Chest
        }
        return com.runecraft.osrs.content.bosses.DoomDelves.BOSS.equals(bossId) ? DoomDelveService.heldLoot(p) : 0;
    }

    /** The Lunar Chest's record of a player's pacified Moons, saved and restored around the self-test (null: none). */
    public static Object lunarChestSave(net.minecraft.server.MinecraftServer server, java.util.UUID player) {
        return LunarChest.selfTestSave(server, player);
    }

    public static void lunarChestRestore(net.minecraft.server.MinecraftServer server, java.util.UUID player, Object saved) {
        LunarChest.selfTestRestore(server, player, saved);
    }

    /**
     * What a Hunllef's script is doing (the Gauntlet walk in {@code content/minigames/GauntletSelfTest}): whether it is in its
     * magic cycle, the style it protects from, attacks into the current cycle, off-prayer hits taken since its last prayer
     * switch, tornadoes out, floor tiles out (and whether they are orange) and attacks made.
     */
    public record HunllefView(boolean magic, com.runecraft.osrs.AttackType prays, int styleAttacks, int offPrayerHits,
                              int tornadoes, int floorTiles, boolean floorOrange, int attacks) {
    }

    /** The Hunllef script's view, or null when the boss is not a Hunllef. */
    public static HunllefView hunllef(DataBossEntity boss) {
        return boss.script() instanceof EncounterScripts.Hunllef h ? h.view() : null;
    }

    /**
     * Has a Hunllef's prayer-disabling orb hit the player: through the real attack path (accuracy roll and all) when
     * {@code rolled} is negative, else only its on-hit rule for that roll. Returns the damage let through after the
     * protection prayer (-1 when the orb never landed or the boss is not a Hunllef).
     */
    public static int hunllefPrayerDisable(DataBossEntity boss, ServerPlayer target, int rolled) {
        return boss.script() instanceof EncounterScripts.Hunllef h ? h.selfTestPrayerDisable(boss, target, rolled) : -1;
    }

    /**
     * Whether a player's ordinary weapon attacks can take hitpoints off this boss at all. False for Zalcano, who is only hurt
     * by imbued tephra (her armour) and pickaxes (once she falls).
     */
    public static boolean realAttacksHurt(String bossId) {
        return !"zalcano".equals(bossId);
    }
}
