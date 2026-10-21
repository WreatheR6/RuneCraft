package com.runecraft.quest;

import com.runecraft.combat.MonsterStats;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.FreeQuestObjectives;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.ImpCatcherBeads;
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
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/**
 * Scripts the quests: NPC dialogue, stage changes, kill objectives and rewards. Dialogue is
 * delivered as chat lines prefixed with the speaker, like OSRS chatbox dialogue.
 */
public final class QuestManager {
    private QuestManager() {
    }

    // ---------------- Helpers ----------------

    private static void say(ServerPlayer player, QuestNpc npc, String line) {
        QuestApi.say(player, npc.displayName(), line);
    }

    private static void you(ServerPlayer player, String line) {
        QuestApi.you(player, line);
    }

    private static int stage(ServerPlayer p, com.runecraft.osrs.QuestInfo q) {
        return Players.data(p).questStage(q);
    }

    static void set(ServerPlayer p, com.runecraft.osrs.QuestInfo q, int stage) {
        PlayerData data = Players.data(p);
        if (data.questStage(q) == 0 && stage > 0) {
            p.displayClientMessage(Component.literal("You have started a new quest: " + q.displayName()).withStyle(ChatFormatting.DARK_PURPLE), false);
        }
        data.setQuestStage(q, stage);
        ServerNetworking.sync(p);
    }

    static boolean takeAll(ServerPlayer p, Map<String, Integer> items) {
        for (Map.Entry<String, Integer> e : items.entrySet()) {
            if (!Inv.has(p, e.getKey(), e.getValue())) {
                return false;
            }
        }
        items.forEach((k, v) -> Inv.remove(p, k, v));
        return true;
    }

    static boolean meetsRequirements(ServerPlayer p, com.runecraft.osrs.QuestInfo q) {
        PlayerData data = Players.data(p);
        if (q instanceof com.runecraft.osrs.content.QuestDef def) {
            for (String req : def.questRequirements()) {
                com.runecraft.osrs.QuestInfo other = com.runecraft.osrs.content.ContentRegistry.quest(req);
                if (other != null && !data.questComplete(other)) {
                    Msg.game(p, "You need to complete " + other.displayName() + " first.");
                    return false;
                }
            }
        }
        for (Map.Entry<Skill, Integer> e : q.requirements().entrySet()) {
            if (data.skills().level(e.getKey()) < e.getValue()) {
                Msg.game(p, "You need " + Msg.article(e.getKey().displayName()) + " level of " + e.getValue() + " to start this quest.");
                return false;
            }
        }
        return true;
    }

    /** Marks a quest complete and hands out rewards (once). */
    public static void complete(ServerPlayer p, com.runecraft.osrs.QuestInfo q) {
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
        p.displayClientMessage(Component.literal("Congratulations, you've completed a quest: ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(q.displayName()).withStyle(ChatFormatting.YELLOW)), false);
        p.displayClientMessage(Component.literal("Quest points: " + data.questPoints() + " / " + Quest.totalQuestPoints()), false);
        p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1f, 1f);
        ServerNetworking.sync(p);
    }

    // ---------------- Dialogue ----------------

    public static void talk(ServerPlayer p, QuestNpcEntity entity) {
        QuestNpc npc = entity.npc();
        com.runecraft.content.GameEvents.fireActivity(p, "talk:" + entity.npcId().toLowerCase(java.util.Locale.ROOT));
        if (npc == null) {
            QuestScripts.talk(p, entity);
            return;
        }
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

    private static void cook(ServerPlayer p, QuestNpc npc) {
        Quest q = Quest.COOKS_ASSISTANT;
        switch (stage(p, q)) {
            case 0 -> {
                say(p, npc, "What am I to do? It's the Duke's birthday and I can't make his cake!");
                say(p, npc, "I need a bucket of milk, an egg and a pot of flour. Could you fetch them for me?");
                you(p, "Yes, I'll help you.");
                set(p, q, 1);
            }
            case 1 -> {
                if (takeAll(p, FreeQuestObjectives.COOKS_ASSISTANT)) {
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

    private static void fred(ServerPlayer p, QuestNpc npc) {
        Quest q = Quest.SHEEP_SHEARER;
        switch (stage(p, q)) {
            case 0 -> {
                say(p, npc, "I'm looking for someone to shear my sheep. Bring me 20 balls of wool and I'll pay you.");
                say(p, npc, "You can spin wool on the spinning wheel in Lumbridge castle.");
                set(p, q, 1);
            }
            case 1 -> {
                if (takeAll(p, FreeQuestObjectives.SHEEP_SHEARER)) {
                    say(p, npc, "That's the last of them. Thank you! Here's your payment.");
                    complete(p, q);
                } else {
                    say(p, npc, "I need 20 balls of wool. You have " + Inv.count(p, "ball_of_wool") + ".");
                }
            }
            default -> say(p, npc, "Those sheep won't shear themselves! Oh wait, you already did.");
        }
    }

    private static void doric(ServerPlayer p, QuestNpc npc) {
        Quest q = Quest.DORICS_QUEST;
        switch (stage(p, q)) {
            case 0 -> {
                say(p, npc, "Hello traveller. I need materials for an order: 6 clay, 4 copper ore and 2 iron ore.");
                say(p, npc, "Fetch them and you may use my anvils.");
                set(p, q, 1);
            }
            case 1 -> {
                if (takeAll(p, FreeQuestObjectives.DORICS_QUEST)) {
                    say(p, npc, "Well done! Here's some coins for your trouble.");
                    complete(p, q);
                } else {
                    say(p, npc, "I still need 6 clay, 4 copper ore and 2 iron ore.");
                }
            }
            default -> say(p, npc, "Feel free to use my anvils any time.");
        }
    }

    private static void runeMysteries(ServerPlayer p, QuestNpc npc) {
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
                // OSRS: Sedridor keeps the Duke's talisman to study it; the quest reward is an air talisman.
                Inv.remove(p, "air_talisman", 1);
                say(p, npc, "This talisman... it is the key to the lost art of Runecraft! I'll keep it to study. Take this package to Aubury in Varrock.");
                Inv.give(p, "research_package", 1);
                set(p, q, 2);
            } else if (s == 3 && Inv.has(p, "research_notes", 1)) {
                Inv.remove(p, "research_notes", 1);
                say(p, npc, "Aubury's notes confirm it! Take this air talisman. Seek out the altars of Gielinor.");
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

    private static void restlessGhost(ServerPlayer p, QuestNpc npc) {
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
                    say(p, npc, "You can understand me! A warlock stole my skull. I sense it lies on the altar in the Wizards' Tower basement...");
                    set(p, q, 3);
                } else if (s >= 3 && Inv.has(p, "ghosts_skull", 1)) {
                    Inv.remove(p, "ghosts_skull", 1);
                    you(p, "I'll put your skull back in your coffin.");
                    say(p, npc, "My skull! Thank you! Now I can rest in peace...");
                    complete(p, q);
                } else if (s == 3) {
                    say(p, npc, "Please find my skull. It's on the altar beneath the Wizards' Tower, south of Draynor.");
                } else {
                    say(p, npc, "...");
                }
            }
        }
    }

    private static void impCatcher(ServerPlayer p, QuestNpc npc) {
        Quest q = Quest.IMP_CATCHER;
        switch (stage(p, q)) {
            case 0 -> {
                say(p, npc, "My beads! Imps stole my black, red, yellow and white beads. Kill the imps around Misthalin and Asgarnia and bring them back.");
                set(p, q, 1);
            }
            case 1 -> {
                if (takeAll(p, Map.of("black_bead", 1, "red_bead", 1, "yellow_bead", 1, "white_bead", 1))) {
                    say(p, npc, "All four beads! Here, take this amulet of accuracy.");
                    complete(p, q);
                } else {
                    List<String> missing = ImpCatcherBeads.missing(item -> Inv.has(p, item, 1));
                    say(p, npc, "I still need " + missing.size() + " of my beads. The imps have them!");
                }
            }
            default -> say(p, npc, "Thanks for finding my beads.");
        }
    }

    private static void witchsPotion(ServerPlayer p, QuestNpc npc) {
        Quest q = Quest.WITCHS_POTION;
        switch (stage(p, q)) {
            case 0 -> {
                say(p, npc, "Want to learn magic? Bring me a rat's tail, an eye of newt, burnt meat and an onion.");
                set(p, q, 1);
            }
            case 1 -> {
                if (takeAll(p, FreeQuestObjectives.WITCHS_POTION)) {
                    say(p, npc, "Excellent. *She drops the ingredients into her cauldron.* Now drink from my cauldron...");
                    set(p, q, 2);
                } else {
                    say(p, npc, "I need a rat's tail, an eye of newt, some burnt meat and an onion. The rats in Brian's shop next door have tails.");
                }
            }
            case 2 -> say(p, npc, "Well, go on then. Drink from the cauldron!");
            default -> say(p, npc, "Mwahaha.");
        }
    }

    private static void romeoAndJuliet(ServerPlayer p, QuestNpc npc) {
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
                    say(p, npc, "I need cadava berries to make that. Cadava bushes grow south-east of Varrock, by the mine.");
                } else {
                    say(p, npc, "Potions for sale!");
                }
            }
        }
    }

    private static void druidicRitual(ServerPlayer p, QuestNpc npc) {
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
                say(p, npc, "We need raw beef, raw rat meat, raw bear meat and raw chicken, dipped in the Cauldron of Thunder.");
                say(p, npc, "The cauldron is in Taverley Dungeon, just south of the village.");
                set(p, q, 2);
            } else if (s == 2) {
                if (takeAll(p, FreeQuestObjectives.DRUIDIC_RITUAL)) {
                    say(p, npc, "Perfect. The ritual is done. Tell Kaqemeex.");
                    set(p, q, 3);
                } else {
                    say(p, npc, "I still need enchanted beef, rat, bear and chicken. Dip the raw meats in the Cauldron of Thunder.");
                }
            } else {
                say(p, npc, "Guthix be with you.");
            }
        }
    }

    private static void vampyreSlayer(ServerPlayer p, QuestNpc npc) {
        Quest q = Quest.VAMPYRE_SLAYER;
        int s = stage(p, q);
        if (npc == QuestNpc.MORGAN) {
            if (s == 0) {
                say(p, npc, "A vampyre terrorises our village from Draynor Manor! My friend Dr Harlow in Varrock knows how to kill it.");
                set(p, q, 1);
            } else {
                say(p, npc, s == 3 ? "Slay Count Draynor! His coffin is in the manor's basement. Bring garlic, a stake and a hammer." : "Please help us!");
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

    private static void demonSlayer(ServerPlayer p, QuestNpc npc) {
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

    // ---------------- Place objectives ----------------

    /** Checks the hand-scripted quests' place objectives ({@link FreeQuestObjectives#SPOTS}); called once a second. */
    public static void spotTick(ServerPlayer p) {
        PlayerData data = Players.data(p);
        int tx = GielinorMap.toOsrsX(p.getBlockX());
        int ty = GielinorMap.toOsrsY(p.getBlockZ());
        // under the surface (a basement or dungeon): dungeon spots (OSRS y 6400+) are drawn under their surface tile
        boolean below = p.getBlockY() < com.runecraft.osrs.world.Elevation.height(p.getBlockX(), p.getBlockZ()) - 4;
        for (FreeQuestObjectives.Spot spot : FreeQuestObjectives.SPOTS) {
            if (!spot.active(data.questStage(spot.quest())) || !spot.contains(tx, ty, below) || !spot.applies(item -> Inv.has(p, item, 1))) {
                continue;
            }
            p.displayClientMessage(Component.literal(spot.message()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
            switch (spot.action()) {
                case GIVE -> Inv.give(p, spot.item(), 1);
                case ENCHANT_MEATS -> FreeQuestObjectives.DRUIDIC_MEATS.forEach((raw, enchanted) -> {
                    if (Inv.has(p, raw, 1) && !Inv.has(p, enchanted, 1)) {
                        Inv.remove(p, raw, 1);
                        Inv.give(p, enchanted, 1);
                    }
                });
                case COMPLETE -> complete(p, spot.quest());
            }
            if (spot.spawn() != null) {
                EntityType.byString(spot.spawn()).ifPresent(type -> {
                    Entity e = type.create(p.level());
                    if (e != null) {
                        e.moveTo(p.getX() + 2, p.getY(), p.getZ() + 2, 0f, 0f);
                        p.level().addFreshEntity(e);
                    }
                });
            }
            ServerNetworking.sync(p);
        }
    }

    // ---------------- Kill objectives & quest drops ----------------

    public static void onKill(ServerPlayer p, LivingEntity entity, MonsterStats stats) {
        QuestScripts.onKill(p, entity, stats);
        PlayerData data = Players.data(p);
        String name = stats.name();
        for (FreeQuestObjectives.QuestDrop drop : FreeQuestObjectives.DROPS) {
            if (drop.matches(data.questStage(drop.quest()), name) && !Inv.has(p, drop.item(), 1)
                    && p.getRandom().nextInt(Math.max(1, drop.oneIn())) == 0) {
                com.runecraft.combat.LootDrops.spawn(entity, Inv.stack(drop.item(), 1), p);
            }
        }
        // OSRS: imps carry Mizgog's beads (also on their normal drop table); while the quest needs them,
        // an imp also often drops a bead the player is still missing.
        if (data.questStage(Quest.IMP_CATCHER) == 1
                && ImpCatcherBeads.carriesBeads(stats.profile() != null ? stats.profile().id : null, name)) {
            String bead = ImpCatcherBeads.questDrop(item -> Inv.has(p, item, 1),
                    p.getRandom().nextInt(ImpCatcherBeads.QUEST_DROP_ONE_IN), p.getRandom().nextInt(4));
            if (bead != null) {
                com.runecraft.combat.LootDrops.spawn(entity, Inv.stack(bead, 1), p);
            }
        }
        Boss boss = ModEntities.boss(entity.getType());
        if (boss == Boss.COUNT_DRAYNOR && data.questStage(Quest.VAMPYRE_SLAYER) == 3) {
            // OSRS: without a stake and a hammer the Count can't be finished off.
            for (String item : FreeQuestObjectives.VAMPYRE_SLAYER_KILL) {
                if (!Inv.has(p, item, 1)) {
                    Msg.game(p, "Count Draynor retreats to his coffin. You need a stake and a hammer to finish him off.");
                    return;
                }
            }
            Inv.remove(p, "stake", 1);
            Msg.game(p, "You hammer the stake into the vampyre's chest!");
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
