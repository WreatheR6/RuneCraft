package com.runecraft.quest;

import com.runecraft.combat.MonsterStats;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.Quest;
import com.runecraft.osrs.QuestNpc;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.data.Boss;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModEntities;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Map;

/**
 * Scripts the quests: NPC dialogue, stage changes, kill objectives and rewards. Dialogue is
 * delivered as chat lines prefixed with the speaker, like OSRS chatbox dialogue.
 */
public final class QuestManager {
    private QuestManager() {
    }

    // ---------------- Helpers ----------------

    private static void say(ServerPlayerEntity player, QuestNpc npc, String line) {
        player.sendMessage(Text.literal(npc.displayName() + ": ").formatted(Formatting.DARK_BLUE)
                .append(Text.literal(line)), false);
    }

    private static void you(ServerPlayerEntity player, String line) {
        player.sendMessage(Text.literal("You: ").formatted(Formatting.DARK_GREEN).append(Text.literal(line)), false);
    }

    private static int stage(ServerPlayerEntity p, Quest q) {
        return Players.data(p).questStage(q);
    }

    private static void set(ServerPlayerEntity p, Quest q, int stage) {
        PlayerData data = Players.data(p);
        if (data.questStage(q) == 0 && stage > 0) {
            p.sendMessage(Text.literal("You have started a new quest: " + q.displayName()).formatted(Formatting.DARK_PURPLE), false);
        }
        data.setQuestStage(q, stage);
        ServerNetworking.sync(p);
    }

    private static boolean takeAll(ServerPlayerEntity p, Map<String, Integer> items) {
        for (Map.Entry<String, Integer> e : items.entrySet()) {
            if (!Inv.has(p, e.getKey(), e.getValue())) {
                return false;
            }
        }
        items.forEach((k, v) -> Inv.remove(p, k, v));
        return true;
    }

    private static boolean meetsRequirements(ServerPlayerEntity p, Quest q) {
        PlayerData data = Players.data(p);
        for (Map.Entry<Skill, Integer> e : q.requirements().entrySet()) {
            if (data.skills().level(e.getKey()) < e.getValue()) {
                Msg.game(p, "You need " + Msg.article(e.getKey().displayName()) + " level of " + e.getValue() + " to start this quest.");
                return false;
            }
        }
        return true;
    }

    /** Marks a quest complete and hands out rewards (once). */
    public static void complete(ServerPlayerEntity p, Quest q) {
        PlayerData data = Players.data(p);
        if (data.questComplete(q)) {
            return;
        }
        data.setQuestStage(q, q.finalStage());
        data.setQuestPoints(data.questPoints() + q.questPoints());
        q.xpRewards().forEach((skill, xp) -> XpService.add(p, skill, xp));
        for (String reward : q.itemRewards()) {
            String[] parts = reward.split(":");
            int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
            Inv.give(p, parts[0], count);
            com.runecraft.collection.CollectionLog.obtain(p, parts[0]);
        }
        p.sendMessage(Text.literal("Congratulations, you've completed a quest: ").formatted(Formatting.GOLD)
                .append(Text.literal(q.displayName()).formatted(Formatting.YELLOW)), false);
        p.sendMessage(Text.literal("Quest points: " + data.questPoints() + " / " + Quest.totalQuestPoints()), false);
        p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1f, 1f);
        ServerNetworking.sync(p);
    }

    // ---------------- Dialogue ----------------

    public static void talk(ServerPlayerEntity p, QuestNpcEntity entity) {
        QuestNpc npc = entity.npc();
        switch (npc) {
            case COOK -> cook(p, npc);
            case FRED_THE_FARMER -> fred(p, npc);
            case DORIC -> doric(p, npc);
            case DUKE_HORACIO, SEDRIDOR, AUBURY -> runeMysteries(p, npc);
            case FATHER_AERECK, FATHER_URHNEY, RESTLESS_GHOST -> restlessGhost(p, npc);
            case WIZARD_MIZGOG -> impCatcher(p, npc);
            case HETTY -> witchsPotion(p, npc);
            case ROMEO, JULIET, FATHER_LAWRENCE, APOTHECARY -> romeoAndJuliet(p, npc);
            case KAQEMEEX, SANFEW -> druidicRitual(p, npc);
            case MORGAN, DR_HARLOW -> vampyreSlayer(p, npc);
            case GYPSY_ARIS, SIR_PRYSIN, WIZARD_TRAIBORN -> demonSlayer(p, npc);
        }
    }

    private static void cook(ServerPlayerEntity p, QuestNpc npc) {
        Quest q = Quest.COOKS_ASSISTANT;
        switch (stage(p, q)) {
            case 0 -> {
                say(p, npc, "What am I to do? It's the Duke's birthday and I can't make his cake!");
                say(p, npc, "I need a bucket of milk, an egg and a pot of flour. Could you fetch them for me?");
                you(p, "Yes, I'll help you.");
                set(p, q, 1);
            }
            case 1 -> {
                if (takeAll(p, Map.of("minecraft:milk_bucket", 1, "minecraft:egg", 1, "pot_of_flour", 1))) {
                    say(p, npc, "You've brought me everything I need! I am saved! Thank you!");
                    Inv.give(p, "minecraft:bucket", 1);
                    complete(p, q);
                } else {
                    say(p, npc, "I still need a bucket of milk, an egg and a pot of flour.");
                }
            }
            default -> say(p, npc, "Thanks again for your help. The Duke loved his cake!");
        }
    }

    private static void fred(ServerPlayerEntity p, QuestNpc npc) {
        Quest q = Quest.SHEEP_SHEARER;
        switch (stage(p, q)) {
            case 0 -> {
                say(p, npc, "I'm looking for someone to shear my sheep. Bring me 20 balls of wool and I'll pay you.");
                say(p, npc, "You can spin wool on the spinning wheel in Lumbridge castle.");
                set(p, q, 1);
            }
            case 1 -> {
                if (takeAll(p, Map.of("ball_of_wool", 20))) {
                    say(p, npc, "That's the last of them. Thank you! Here's your payment.");
                    complete(p, q);
                } else {
                    say(p, npc, "I need 20 balls of wool. You have " + Inv.count(p, "ball_of_wool") + ".");
                }
            }
            default -> say(p, npc, "Those sheep won't shear themselves! Oh wait, you already did.");
        }
    }

    private static void doric(ServerPlayerEntity p, QuestNpc npc) {
        Quest q = Quest.DORICS_QUEST;
        switch (stage(p, q)) {
            case 0 -> {
                say(p, npc, "Hello traveller. I need materials for an order: 6 clay, 4 copper ore and 2 iron ore.");
                say(p, npc, "Fetch them and you may use my anvils.");
                set(p, q, 1);
            }
            case 1 -> {
                if (takeAll(p, Map.of("clay", 6, "copper_ore", 4, "iron_ore", 2))) {
                    say(p, npc, "Well done! Here's some coins for your trouble.");
                    complete(p, q);
                } else {
                    say(p, npc, "I still need 6 clay, 4 copper ore and 2 iron ore.");
                }
            }
            default -> say(p, npc, "Feel free to use my anvils any time.");
        }
    }

    private static void runeMysteries(ServerPlayerEntity p, QuestNpc npc) {
        Quest q = Quest.RUNE_MYSTERIES;
        int s = stage(p, q);
        if (npc == QuestNpc.DUKE_HORACIO) {
            if (s == 0) {
                say(p, npc, "Greetings. I found this strange talisman. Would you take it to the head wizard, Sedridor, at the Wizards' Tower?");
                Inv.give(p, "air_talisman", 1);
                set(p, q, 1);
            } else {
                say(p, npc, "Welcome to my castle, adventurer.");
            }
        } else if (npc == QuestNpc.SEDRIDOR) {
            if (s == 1 && Inv.has(p, "air_talisman", 1)) {
                say(p, npc, "This talisman... it is the key to the lost art of Runecraft! Take this package to Aubury in Varrock.");
                Inv.give(p, "research_package", 1);
                set(p, q, 2);
            } else if (s == 3 && Inv.has(p, "research_notes", 1)) {
                Inv.remove(p, "research_notes", 1);
                say(p, npc, "Aubury's notes confirm it! You may keep the talisman. Seek out the altars of Gielinor.");
                complete(p, q);
            } else {
                say(p, npc, "The Wizards' Tower has stood for centuries.");
            }
        } else {
            if (s == 2 && Inv.remove(p, "research_package", 1) == 1) {
                say(p, npc, "Sedridor sent this? Fascinating... Take these notes back to him.");
                Inv.give(p, "research_notes", 1);
                set(p, q, 3);
            } else {
                say(p, npc, "Do you want to buy some runes?");
            }
        }
    }

    private static void restlessGhost(ServerPlayerEntity p, QuestNpc npc) {
        Quest q = Quest.RESTLESS_GHOST;
        int s = stage(p, q);
        switch (npc) {
            case FATHER_AERECK -> {
                if (s == 0) {
                    say(p, npc, "A ghost is haunting my graveyard! Please, speak to Father Urhney in the swamp, he knows about ghosts.");
                    set(p, q, 1);
                } else {
                    say(p, npc, "Bless you, child.");
                }
            }
            case FATHER_URHNEY -> {
                if (s == 1) {
                    say(p, npc, "A ghost, you say? Bah. Take this amulet of ghostspeak and leave me in peace.");
                    Inv.give(p, "ghostspeak_amulet", 1);
                    set(p, q, 2);
                } else if (s >= 2 && s < q.finalStage() && !Inv.has(p, "ghostspeak_amulet", 1)) {
                    say(p, npc, "Lost it already? Here, take another.");
                    Inv.give(p, "ghostspeak_amulet", 1);
                } else {
                    say(p, npc, "Go away!");
                }
            }
            default -> {
                if (!Inv.has(p, "ghostspeak_amulet", 1)) {
                    say(p, npc, "Wooo wooo woooo!");
                    return;
                }
                if (s == 2) {
                    say(p, npc, "You can understand me! Someone stole my skull. I think the skeletons of Gielinor took it...");
                    set(p, q, 3);
                } else if (s >= 3 && Inv.has(p, "ghosts_skull", 1)) {
                    Inv.remove(p, "ghosts_skull", 1);
                    say(p, npc, "My skull! Thank you! Now I can rest in peace...");
                    complete(p, q);
                } else if (s == 3) {
                    say(p, npc, "Please find my skull. Skeletons may have it.");
                } else {
                    say(p, npc, "...");
                }
            }
        }
    }

    private static void impCatcher(ServerPlayerEntity p, QuestNpc npc) {
        Quest q = Quest.IMP_CATCHER;
        switch (stage(p, q)) {
            case 0 -> {
                say(p, npc, "My beads! Imps stole my black, red, yellow and white beads. Dark wizards and goblins may have them now.");
                set(p, q, 1);
            }
            case 1 -> {
                if (takeAll(p, Map.of("black_bead", 1, "red_bead", 1, "yellow_bead", 1, "white_bead", 1))) {
                    say(p, npc, "All four beads! Here, take this amulet of accuracy.");
                    complete(p, q);
                } else {
                    say(p, npc, "I still need a black, red, yellow and white bead.");
                }
            }
            default -> say(p, npc, "Thanks for finding my beads.");
        }
    }

    private static void witchsPotion(ServerPlayerEntity p, QuestNpc npc) {
        Quest q = Quest.WITCHS_POTION;
        switch (stage(p, q)) {
            case 0 -> {
                say(p, npc, "Want to learn magic? Bring me a rat's tail, an eye of newt, burnt meat and an onion.");
                set(p, q, 1);
            }
            case 1 -> {
                if (takeAll(p, Map.of("rats_tail", 1, "eye_of_newt", 1, "burnt_meat", 1, "onion", 1))) {
                    say(p, npc, "Excellent. Now drink from my cauldron...");
                    you(p, "*gulp*");
                    complete(p, q);
                } else {
                    say(p, npc, "I need a rat's tail, an eye of newt, burnt meat and an onion. Giant rats might have a tail.");
                }
            }
            default -> say(p, npc, "Mwahaha.");
        }
    }

    private static void romeoAndJuliet(ServerPlayerEntity p, QuestNpc npc) {
        Quest q = Quest.ROMEO_AND_JULIET;
        int s = stage(p, q);
        switch (npc) {
            case ROMEO -> {
                if (s == 0) {
                    say(p, npc, "Have you seen Juliet? Could you find her for me?");
                    set(p, q, 1);
                } else if (s == 2 && Inv.remove(p, "message_from_juliet", 1) == 1) {
                    say(p, npc, "A message from Juliet! She has a plan... I should ask Father Lawrence.");
                    set(p, q, 3);
                } else if (s == 6) {
                    say(p, npc, "She'll pretend to be dead so we can escape together? Wonderful!");
                    complete(p, q);
                } else {
                    say(p, npc, "Juliet, oh Juliet...");
                }
            }
            case JULIET -> {
                if (s == 1) {
                    say(p, npc, "Romeo sent you? Please give him this message.");
                    Inv.give(p, "message_from_juliet", 1);
                    set(p, q, 2);
                } else if (s == 5 && Inv.remove(p, "cadava_potion", 1) == 1) {
                    say(p, npc, "The potion! Tell Romeo the plan.");
                    set(p, q, 6);
                } else {
                    say(p, npc, "Wherefore art thou, Romeo?");
                }
            }
            case FATHER_LAWRENCE -> {
                if (s == 3) {
                    say(p, npc, "Juliet can drink a potion to fake her death. Ask the Apothecary to make a Cadava potion.");
                    set(p, q, 4);
                } else {
                    say(p, npc, "Bless you.");
                }
            }
            default -> {
                if (s == 4 && Inv.remove(p, "cadava_berries", 1) == 1) {
                    say(p, npc, "Cadava berries? Here's your potion. Don't drink it yourself!");
                    Inv.give(p, "cadava_potion", 1);
                    set(p, q, 5);
                } else if (s == 4) {
                    say(p, npc, "I need cadava berries to make that. Herb patches and goblins sometimes have them.");
                } else {
                    say(p, npc, "Potions for sale!");
                }
            }
        }
    }

    private static void druidicRitual(ServerPlayerEntity p, QuestNpc npc) {
        Quest q = Quest.DRUIDIC_RITUAL;
        int s = stage(p, q);
        if (npc == QuestNpc.KAQEMEEX) {
            if (s == 0) {
                say(p, npc, "Our stone circle has been defiled. Speak to Sanfew to help with the ritual, and I'll teach you Herblore.");
                set(p, q, 1);
            } else if (s == 3) {
                say(p, npc, "The ritual is complete! Let me teach you the ancient art of Herblore.");
                complete(p, q);
            } else {
                say(p, npc, "Hello again.");
            }
        } else {
            if (s == 1) {
                say(p, npc, "We need raw beef, raw chicken, raw porkchop and raw mutton for the ritual.");
                set(p, q, 2);
            } else if (s == 2) {
                if (takeAll(p, Map.of("minecraft:beef", 1, "minecraft:chicken", 1, "minecraft:porkchop", 1, "minecraft:mutton", 1))) {
                    say(p, npc, "Perfect. The ritual is done. Tell Kaqemeex.");
                    set(p, q, 3);
                } else {
                    say(p, npc, "I still need raw beef, raw chicken, raw porkchop and raw mutton.");
                }
            } else {
                say(p, npc, "Guthix be with you.");
            }
        }
    }

    private static void vampyreSlayer(ServerPlayerEntity p, QuestNpc npc) {
        Quest q = Quest.VAMPYRE_SLAYER;
        int s = stage(p, q);
        if (npc == QuestNpc.MORGAN) {
            if (s == 0) {
                say(p, npc, "A vampyre terrorises our village from Draynor Manor! My friend Dr Harlow in Varrock knows how to kill it.");
                set(p, q, 1);
            } else {
                say(p, npc, s == 3 ? "Slay Count Draynor! His coffin is in the manor." : "Please help us!");
            }
        } else {
            if (s == 1) {
                say(p, npc, "A vampyre? *hic* Buy me a beer and I'll tell you.");
                set(p, q, 2);
            } else if (s == 2 && Inv.remove(p, "beer", 1) == 1) {
                say(p, npc, "Ahh. Take this stake. Bring a hammer, and drive it through his heart once he's weakened.");
                Inv.give(p, "stake", 1);
                set(p, q, 3);
            } else if (s == 3 && !Inv.has(p, "stake", 1)) {
                say(p, npc, "Lost your stake? Here's another.");
                Inv.give(p, "stake", 1);
            } else {
                say(p, npc, "*hic*");
            }
        }
    }

    private static void demonSlayer(ServerPlayerEntity p, QuestNpc npc) {
        Quest q = Quest.DEMON_SLAYER;
        int s = stage(p, q);
        switch (npc) {
            case GYPSY_ARIS -> {
                if (s == 0) {
                    say(p, npc, "I see... the demon Delrith will return to destroy Varrock! Only you can stop him, with Silverlight.");
                    say(p, npc, "Sir Prysin keeps the sword.");
                    set(p, q, 1);
                } else {
                    say(p, npc, "Cross my palm with silver...");
                }
            }
            case SIR_PRYSIN -> {
                if (s == 1) {
                    say(p, npc, "Silverlight is locked away and I lost the keys. Wizard Traiborn has one... he's a bit odd.");
                    set(p, q, 2);
                } else if (s == 3 && Inv.remove(p, "silverlights_key", 1) == 1) {
                    say(p, npc, "The key! Here is Silverlight. Delrith is summoned at the stone circle south of Varrock.");
                    Inv.give(p, "silverlight", 1);
                    set(p, q, 4);
                } else if (s == 4 && !Inv.has(p, "silverlight", 1)) {
                    say(p, npc, "You lost Silverlight?! Fine, take it back.");
                    Inv.give(p, "silverlight", 1);
                } else {
                    say(p, npc, "Hello.");
                }
            }
            default -> {
                if (s == 2) {
                    if (takeAll(p, Map.of("bones", 25))) {
                        say(p, npc, "25 bones! Wonderful. Here is the key... I think.");
                        Inv.give(p, "silverlights_key", 1);
                        set(p, q, 3);
                    } else {
                        say(p, npc, "A key? I'll need 25 bones for the spell. You have " + Inv.count(p, "bones") + ".");
                    }
                } else {
                    say(p, npc, "Ello young thingummywut.");
                }
            }
        }
    }

    // ---------------- Kill objectives & quest drops ----------------

    public static void onKill(ServerPlayerEntity p, LivingEntity entity, MonsterStats stats) {
        PlayerData data = Players.data(p);
        String name = stats.name();
        if (data.questStage(Quest.RESTLESS_GHOST) == 3 && name.equals("Skeleton") && !Inv.has(p, "ghosts_skull", 1)
                && p.getRandom().nextInt(4) == 0) {
            entity.dropStack(Inv.stack("ghosts_skull", 1));
        }
        if (data.questStage(Quest.IMP_CATCHER) == 1 && (name.equals("Dark wizard") || name.equals("Goblin"))
                && p.getRandom().nextInt(3) == 0) {
            String[] beads = {"black_bead", "red_bead", "yellow_bead", "white_bead"};
            entity.dropStack(Inv.stack(beads[p.getRandom().nextInt(beads.length)], 1));
        }
        if (data.questStage(Quest.WITCHS_POTION) == 1 && name.equals("Giant rat") && !Inv.has(p, "rats_tail", 1)) {
            entity.dropStack(Inv.stack("rats_tail", 1));
        }
        if (data.questStage(Quest.ROMEO_AND_JULIET) == 4 && name.equals("Goblin") && p.getRandom().nextInt(3) == 0) {
            entity.dropStack(Inv.stack("cadava_berries", 1));
        }
        Boss boss = ModEntities.boss(entity.getType());
        if (boss == Boss.COUNT_DRAYNOR && data.questStage(Quest.VAMPYRE_SLAYER) == 3) {
            complete(p, Quest.VAMPYRE_SLAYER);
        } else if (boss == Boss.DELRITH && data.questStage(Quest.DEMON_SLAYER) == 4) {
            Msg.game(p, "Delrith is banished back to the void!");
            complete(p, Quest.DEMON_SLAYER);
        } else if (boss == Boss.TZTOK_JAD) {
            complete(p, Quest.FIGHT_CAVES);
        } else if (boss != null && boss.lair() == com.runecraft.osrs.Landmark.BARROWS) {
            int done = 0;
            for (Boss b : new Boss[]{Boss.AHRIM, Boss.DHAROK, Boss.GUTHAN, Boss.KARIL, Boss.TORAG, Boss.VERAC}) {
                if (data.killCount(b.entityId()) > 0) {
                    done++;
                }
            }
            if (done == 6) {
                complete(p, Quest.BARROWS_INTRO);
            } else if (data.questStage(Quest.BARROWS_INTRO) == 0) {
                set(p, Quest.BARROWS_INTRO, 1);
            }
        }
    }
}
