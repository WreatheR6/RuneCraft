package com.runecraft.selftest;

import com.runecraft.content.quests.QuestInterpreter;
import com.runecraft.content.quests.QuestLampService;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.quests.QuestLamps;
import com.runecraft.osrs.content.quests.QuestLamps.Grant;
import com.runecraft.osrs.content.quests.QuestLamps.Kind;
import com.runecraft.osrs.content.quests.QuestLamps.Lamp;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

/**
 * Quest lamps, tomes and skill choices as a player meets them ({@code quest:lamps:<quest>}): the quest's grant hands them out,
 * each lamp is rubbed with the held item's real use (the use-item callback opens the skill choice) and the choice is made
 * through the screen's button action, which must put exactly the lamp's XP into that skill and use the lamp up. The walks
 * ({@code quest:walk:}, {@code quest:steps:}) claim their quests' lamps the same way after the real completion.
 */
final class QuestLampChecks {
    private QuestLampChecks() {
    }

    static List<SelfTest.Check> all(TestContext c) {
        List<SelfTest.Check> checks = new ArrayList<>();
        for (String quest : QuestLamps.quests()) {
            checks.add(new SelfTest.Check("quest:lamps:" + quest, () -> {
                QuestLampService.selfTestReset(c.player, quest);
                QuestLampService.grant(c.player, quest);
                for (Grant g : QuestLamps.grants(quest)) {
                    Lamp l = QuestLamps.get(g.lamp());
                    // Duke trains only players Daero trained: refused before (claim then has Daero train the player first)
                    if (g.needsChoice() != null && QuestLampService.choose(c.player, l.id() + ":" + firstSkill(c.player, l).name())) {
                        throw new AssertionError(l.name() + " was given before " + QuestLamps.get(g.needsChoice()).name());
                    }
                }
                return claim(c, quest);
            }));
        }
        checks.add(new SelfTest.Check("quest:lamps:reclaim", () -> reclaim(c)));
        checks.add(new SelfTest.Check("quest:lamps:kingdom_divided", () -> kingdomDivided(c)));
        checks.add(new SelfTest.Check("quest:lamps:museum", () -> museum(c)));
        return checks;
    }

    /**
     * The Varrock Museum's antique lamps (Historian Minas, the specimen crate) are rubbed like any quest lamp: each one in hand
     * opens the skill choice and gives exactly its XP, and a skill below its level is refused.
     */
    private static String museum(TestContext c) {
        ServerPlayer p = c.player;
        PlayerData d = Players.data(p);
        List<String> done = new ArrayList<>();
        for (int xp : new int[]{500, 1000, 5000, 10000}) {
            Lamp l = QuestLamps.get(com.runecraft.osrs.content.minigames.VarrockMuseum.lampItem(xp));
            Inv.remove(p, l.id(), Inv.count(p, l.id()));
            Inv.give(p, l.id(), 1);
            if (!rub(c, l.id()).consumesAction()) {
                throw new AssertionError(l.id() + ": rubbing it did nothing");
            }
            Skill s = firstSkill(p, l);
            int before = d.skills().xpTenths(s);
            if (!QuestLampService.choose(p, l.id() + ":" + s.name())) {
                throw new AssertionError(l.id() + ": the choice was refused");
            }
            if (d.skills().xpTenths(s) - before != xp * 10 || Inv.count(p, l.id()) != 0) {
                throw new AssertionError(l.id() + ": " + (d.skills().xpTenths(s) - before) / 10.0 + " XP, " + Inv.count(p, l.id()) + " left");
            }
            done.add(xp + " XP");
        }
        return "museum lamps rubbed: " + String.join(", ", done);
    }

    /** The first skill a player may put a lamp's XP into (every skill is 99 on the test player). */
    private static Skill firstSkill(ServerPlayer p, Lamp l) {
        for (Skill s : Skill.values()) {
            if (l.allows(s, Players.data(p).skills().level(s))) {
                return s;
            }
        }
        throw new AssertionError(l.name() + ": no skill the player may choose");
    }

    /** Right-clicks with the lamp in hand as a player does: the use-item callbacks, then the item's own use. */
    private static InteractionResult rub(TestContext c, String id) {
        ServerPlayer p = c.player;
        int slot = -1;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (id.equals(Inv.idOf(p.getInventory().getItem(i)))) {
                slot = i;
                break;
            }
        }
        if (slot < 0) {
            throw new AssertionError("no " + id + " to rub");
        }
        ItemStack lamp = p.getInventory().getItem(slot);
        ItemStack hand = p.getMainHandItem();
        p.getInventory().setItem(slot, hand);
        p.setItemInHand(InteractionHand.MAIN_HAND, lamp);
        p.setShiftKeyDown(false);
        return p.gameMode.useItem(p, c.world, p.getMainHandItem(), InteractionHand.MAIN_HAND);
    }

    /**
     * Claims everything a completed quest handed out, as the player: every lamp and tome rubbed until it is gone, every choice
     * made, each use adding exactly its XP. Returns what was claimed.
     */
    static String claim(TestContext c, String quest) {
        ServerPlayer p = c.player;
        PlayerData d = Players.data(p);
        List<String> done = new ArrayList<>();
        for (Grant g : QuestLamps.grants(quest)) {
            Lamp l = QuestLamps.get(g.lamp());
            int n = QuestLampService.granted(p, g);
            if (n == 0) {
                if (g.fewerIfGranted() == null) {
                    throw new AssertionError(quest + " handed out no " + l.name());
                }
                continue;
            }
            if (n != g.count() && g.fewerIfGranted() == null) {
                throw new AssertionError(quest + " handed out " + n + " " + l.name() + ", not " + g.count());
            }
            int uses = n * l.uses();
            if (g.needsChoice() != null && QuestLampService.used(p, g.needsChoice()) == 0) {
                // Duke trains only players Daero trained: a walk's earlier quests were completed without their rewards
                for (Grant need : QuestLamps.grants()) {
                    if (need.lamp().equals(g.needsChoice())) {
                        QuestLampService.grant(p, need.quest());
                        done.add(claim(c, need.quest()));
                    }
                }
            }
            if (l.kind() != Kind.CHOICE && Inv.count(p, l.id()) < n) {
                throw new AssertionError(quest + ": " + Inv.count(p, l.id()) + " " + l.id() + " carried, " + n + " handed out");
            }
            for (int i = 0; i < uses; i++) {
                if (l.kind() != Kind.CHOICE) {
                    InteractionResult r = rub(c, l.id());
                    if (!r.consumesAction()) {
                        throw new AssertionError(l.id() + ": rubbing it did nothing (" + r + ")");
                    }
                }
                Skill skill = l.packages().isEmpty() ? firstSkill(p, l) : null;
                int pick = skill == null ? i % l.packages().size() : -1;
                String action = l.id() + ":" + (skill != null ? skill.name() : "pkg" + pick);
                Map<Skill, Integer> expect = QuestLamps.xpOf(l, skill, pick);
                Map<Skill, Integer> before = new java.util.EnumMap<>(Skill.class);
                expect.keySet().forEach(s -> before.put(s, d.skills().xpTenths(s)));
                if (!QuestLampService.choose(p, action)) {
                    throw new AssertionError(l.id() + ": use " + (i + 1) + "/" + uses + " (" + action + ") was refused");
                }
                for (Map.Entry<Skill, Integer> e : expect.entrySet()) {
                    int got = d.skills().xpTenths(e.getKey()) - before.get(e.getKey());
                    if (got != e.getValue() * 10) {
                        throw new AssertionError(l.id() + ": " + e.getKey().displayName() + " gained " + got / 10.0 + " XP, not "
                                + e.getValue());
                    }
                }
            }
            Skill any = l.packages().isEmpty() ? firstSkill(p, l) : null;
            if (QuestLampService.choose(p, l.id() + ":" + (any != null ? any.name() : "pkg0"))) {
                throw new AssertionError(l.id() + ": a use beyond the " + uses + " handed out was granted");
            }
            if (l.kind() != Kind.CHOICE && Inv.count(p, l.id()) != 0) {
                throw new AssertionError(l.id() + ": " + Inv.count(p, l.id()) + " left after every use");
            }
            done.add(n + " x " + l.name() + (l.uses() > 1 ? " (" + l.uses() + " uses)" : ""));
        }
        return "claimed " + String.join(", ", done);
    }

    /** A lost lamp comes back from its NPC; a lamp rubbed or still carried doesn't. */
    private static String reclaim(TestContext c) {
        ServerPlayer p = c.player;
        String quest = "one_small_favour";
        Lamp l = QuestLamps.get("quest_lamp_one_small_favour");
        QuestLampService.selfTestReset(p, quest);
        QuestLampService.grant(p, quest);
        if (Inv.count(p, l.id()) != 2) {
            throw new AssertionError("One Small Favour handed out " + Inv.count(p, l.id()) + " lamps, not 2");
        }
        rub(c, l.id());
        if (!QuestLampService.choose(p, l.id() + ":" + firstSkill(p, l).name())) {
            throw new AssertionError("the first lamp was refused");
        }
        QuestInterpreter.selfTestTalk(p, l.npc());
        if (Inv.count(p, l.id()) != 1) {
            throw new AssertionError("talking to Yanni with one lamp carried left " + Inv.count(p, l.id()) + " lamps");
        }
        Inv.remove(p, l.id(), 1); // lost
        QuestInterpreter.selfTestTalk(p, l.npc());
        if (Inv.count(p, l.id()) != 1) {
            throw new AssertionError("Yanni gave back " + Inv.count(p, l.id()) + " lamps for the lost one, not 1");
        }
        rub(c, l.id());
        QuestLampService.choose(p, l.id() + ":" + firstSkill(p, l).name());
        QuestInterpreter.selfTestTalk(p, l.npc());
        if (Inv.count(p, l.id()) != 0) {
            throw new AssertionError("Yanni gave a third lamp after both were rubbed");
        }
        // an unmade choice: Irena offers it again
        QuestLampService.selfTestReset(p, "the_tourist_trap");
        QuestLampService.grant(p, "the_tourist_trap");
        Lamp irena = QuestLamps.get("tourist_trap_reward");
        if (QuestLampService.owed(p, irena) != 2) {
            throw new AssertionError("Irena's reward owes " + QuestLampService.owed(p, irena) + " choices, not 2");
        }
        if (!QuestLampService.talk(p, irena.npc())) {
            throw new AssertionError("Irena didn't offer the unmade choice again");
        }
        return "lost lamp given back once, none after both were rubbed; Irena re-offers her unmade choice";
    }

    /** A Kingdom Divided and Architectural Alliance hand out two lamps between them, in either order. */
    private static String kingdomDivided(TestContext c) {
        ServerPlayer p = c.player;
        String lamp = "quest_lamp_a_kingdom_divided";
        QuestLampService.selfTestReset(p, "architectural_alliance");
        QuestLampService.selfTestReset(p, "a_kingdom_divided");
        Inv.remove(p, lamp, Inv.count(p, lamp));
        QuestLampService.grant(p, "architectural_alliance");
        QuestLampService.grant(p, "a_kingdom_divided");
        int hosaFirst = Inv.count(p, lamp);
        Inv.remove(p, lamp, hosaFirst);
        QuestLampService.selfTestReset(p, "architectural_alliance");
        QuestLampService.selfTestReset(p, "a_kingdom_divided");
        QuestLampService.grant(p, "a_kingdom_divided");
        QuestLampService.grant(p, "architectural_alliance");
        int fulloreFirst = Inv.count(p, lamp);
        Inv.remove(p, lamp, fulloreFirst);
        if (hosaFirst != 2 || fulloreFirst != 2) {
            throw new AssertionError("lamps between the two quests: " + hosaFirst + " (Hosa first), " + fulloreFirst
                    + " (Commander Fullore first), not 2");
        }
        return "two lamps between them in either order";
    }
}
