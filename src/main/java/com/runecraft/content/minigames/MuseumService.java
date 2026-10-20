package com.runecraft.content.minigames;

import com.runecraft.content.BlockHooks;
import com.runecraft.content.GameEvents;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.minigames.VarrockMuseum;
import com.runecraft.osrs.content.minigames.VarrockMuseum.CrateLoot;
import com.runecraft.osrs.content.minigames.VarrockMuseum.KudosReward;
import com.runecraft.osrs.content.minigames.VarrockMuseum.Question;
import com.runecraft.osrs.content.minigames.VarrockMuseum.QuestKudos;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.quest.QuestScripts;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.LandmarkBuilder;
import com.runecraft.platform.ServerLifecycleEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.Block;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Varrock Museum kudos (see {@link VarrockMuseum}): Orlando Smith's natural history quiz (the plaque
 * asks the next question of your first unfinished display, answered with chat links that run
 * {@code /minigame museum <n>}), cleaning finds, the display cases and storage crate, Historian
 * Minas' quest kudos, the information clerk's XP rewards and the Haig Halen / Orlando diary checks.
 * Kudos and progress are kept per player in {@link MinigameData}.
 */
final class MuseumService {
    private static final String KUDOS = "museum_kudos";
    private static final String QUIZ_STARTED = "museum_quiz_started";
    private static boolean wrapped;

    private MuseumService() {
    }

    static void register() {
        BlockHooks.register(VarrockMuseum.PLAQUE, (player, pos, state) -> plaque(player));
        BlockHooks.register(VarrockMuseum.SPECIMEN_ROCKS, (player, pos, state) -> rocks(player));
        BlockHooks.register(VarrockMuseum.SPECIMEN_TABLE, (player, pos, state) -> clean(player));
        BlockHooks.register(VarrockMuseum.TOOL_RACK, (player, pos, state) -> tools(player));
        BlockHooks.register(VarrockMuseum.STORAGE_CRATE, (player, pos, state) -> crate(player));
        BlockHooks.register(VarrockMuseum.DISPLAY_CASE, (player, pos, state) -> display(player));
        QuestScripts.onTalk(VarrockMuseum.ORLANDO, (player, npc) -> orlando(player));
        QuestScripts.onTalk(VarrockMuseum.CLERK, (player, npc) -> clerk(player));
        QuestScripts.onTalk(VarrockMuseum.ARCHAEOLOGIST, (player, npc) -> archaeologist(player));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> wrap());
        LandmarkBuilder.registerCustom(VarrockMuseum.PLACE, MuseumService::build);
    }

    private static void wrap() {
        if (wrapped) {
            return;
        }
        wrapped = true;
        BiConsumer<ServerPlayer, QuestNpcEntity> minas = QuestScripts.handler("historian_minas");
        QuestScripts.onTalk("historian_minas", (player, npc) -> {
            if (!minas(player) && minas != null) {
                minas.accept(player, npc);
            }
        });
        BiConsumer<ServerPlayer, QuestNpcEntity> haig = QuestScripts.handler("curator_haig_halen");
        QuestScripts.onTalk("curator_haig_halen", (player, npc) -> {
            if (haig != null) {
                haig.accept(player, npc);
            }
            int k = kudos(player);
            QuestApi.say(player, "Curator Haig Halen", "Thank you for your help with the museum. You have " + k + " kudos.");
            if (k >= VarrockMuseum.HAIG_HALEN_KUDOS) {
                GameEvents.fireActivity(player, "kudos:haig_halen_50");
            }
        });
    }

    private static MinigameData data(ServerPlayer p) {
        return MinigameData.of(p);
    }

    static int kudos(ServerPlayer p) {
        return data(p).stat(p.getUUID(), KUDOS);
    }

    private static void addKudos(ServerPlayer p, int n) {
        int k = kudos(p) + n;
        data(p).setStat(p.getUUID(), KUDOS, k);
        Msg.game(p, "You gain " + n + " kudos. (Total: " + k + ".)");
    }

    // ------------------------------------------------------------------ natural history quiz

    private static int correct(ServerPlayer p, int d) {
        return data(p).stat(p.getUUID(), "museum_quiz_" + d);
    }

    private static int firstUnfinished(ServerPlayer p) {
        for (int d = 0; d < VarrockMuseum.DISPLAYS.size(); d++) {
            if (correct(p, d) < VarrockMuseum.QUIZ_RIGHT_ANSWERS) {
                return d;
            }
        }
        return -1;
    }

    static void plaque(ServerPlayer p) {
        if (!data(p).flag(p.getUUID(), QUIZ_STARTED)) {
            Msg.game(p, "This looks like some sort of quiz. I wonder if I can take part...");
            return;
        }
        int d = firstUnfinished(p);
        if (d < 0) {
            Msg.game(p, "You have completed every display's quiz. Speak to Orlando Smith.");
            return;
        }
        List<Question> qs = VarrockMuseum.questions(d);
        int qi = data(p).stat(p.getUUID(), "museum_quiz_next_" + d) % qs.size();
        data(p).setStat(p.getUUID(), "museum_quiz_display", d);
        data(p).setStat(p.getUUID(), "museum_quiz_q", qi + 1);
        Question q = qs.get(qi);
        p.displayClientMessage(Component.literal(VarrockMuseum.DISPLAYS.get(d) + ": " + q.question()).withStyle(ChatFormatting.GOLD), false);
        MutableComponent line = Component.literal("");
        for (int i = 0; i < q.answers().size(); i++) {
            String cmd = "/minigame museum " + i;
            line.append(Component.literal("[" + q.answers().get(i) + "] ").withStyle(st -> st.withColor(ChatFormatting.GREEN)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd))));
        }
        p.displayClientMessage(line, false);
    }

    static void answer(ServerPlayer p, int answer) {
        MinigameData data = data(p);
        UUID u = p.getUUID();
        int asked = data.stat(u, "museum_quiz_q");
        if (asked <= 0) {
            Msg.game(p, "Read one of the natural history plaques first.");
            return;
        }
        int d = data.stat(u, "museum_quiz_display");
        int qi = asked - 1;
        data.setStat(u, "museum_quiz_q", 0);
        List<Question> qs = VarrockMuseum.questions(d);
        data.setStat(u, "museum_quiz_next_" + d, (qi + 1) % qs.size());
        if (answer != qs.get(qi).correct()) {
            QuestApi.say(p, "Orlando Smith", "Hmm, I don't think that's right, mate. Try again.");
            return;
        }
        int right = correct(p, d) + 1;
        data.setStat(u, "museum_quiz_" + d, right);
        QuestApi.say(p, "Orlando Smith", "Nice job, mate. That looks about right.");
        if (right == VarrockMuseum.QUIZ_RIGHT_ANSWERS) {
            Msg.game(p, "You have completed the " + VarrockMuseum.DISPLAYS.get(d) + " display's quiz.");
            addKudos(p, VarrockMuseum.KUDOS_PER_DISPLAY);
            if (firstUnfinished(p) < 0) {
                QuestApi.say(p, "Orlando Smith", "Bonza, mate! I think that's all of them.");
            }
        }
    }

    static void orlando(ServerPlayer p) {
        MinigameData data = data(p);
        UUID u = p.getUUID();
        if (!data.flag(u, QUIZ_STARTED)) {
            data.setFlag(u, QUIZ_STARTED);
            QuestApi.say(p, "Orlando Smith", "G'day, mate! Fancy a quiz on the creatures in here? Read the plaques by the displays and "
                    + "answer three questions right on each.");
            return;
        }
        if (firstUnfinished(p) < 0 && !data.flag(u, "museum_quiz_reward")) {
            data.setFlag(u, "museum_quiz_reward");
            XpService.add(p, Skill.HUNTER, VarrockMuseum.QUIZ_XP);
            XpService.add(p, Skill.SLAYER, VarrockMuseum.QUIZ_XP);
            QuestApi.say(p, "Orlando Smith", "You've finished the whole quiz! Here's a little something for your trouble.");
        } else {
            int done = 0;
            for (int d = 0; d < VarrockMuseum.DISPLAYS.size(); d++) {
                done += correct(p, d) >= VarrockMuseum.QUIZ_RIGHT_ANSWERS ? 1 : 0;
            }
            QuestApi.say(p, "Orlando Smith", "You've done " + done + " of " + VarrockMuseum.DISPLAYS.size()
                    + " displays, mate. You have " + kudos(p) + " kudos.");
        }
        if (kudos(p) >= VarrockMuseum.ORLANDO_KUDOS) {
            GameEvents.fireActivity(p, "kudos:orlando_153");
        }
    }

    // ------------------------------------------------------------------ Historian Minas, the clerk

    /** Hands out kudos for every completed timeline quest; false if there was nothing new. */
    static boolean minas(ServerPlayer p) {
        boolean any = false;
        for (QuestKudos q : VarrockMuseum.QUEST_KUDOS) {
            String flag = "museum_quest_" + q.quest();
            if (ContentRegistry.quest(q.quest()) == null || data(p).flag(p.getUUID(), flag) || !QuestApi.isComplete(p, q.quest())) {
                continue;
            }
            data(p).setFlag(p.getUUID(), flag);
            QuestApi.say(p, "Historian Minas", "Tell me about " + ContentRegistry.quest(q.quest()).displayName()
                    + "... Fascinating! That belongs on our timeline.");
            addKudos(p, q.kudos());
            if (q.lampXp() > 0) {
                lamp(p, q.lampXp(), q.lampLevel());
            }
            any = true;
        }
        return any;
    }

    /** An antique lamp: XP in the player's lowest skill at or above {@code minLevel}. // approx: no lamp item */
    private static void lamp(ServerPlayer p, int xp, int minLevel) {
        Skill best = null;
        int bestLevel = Integer.MAX_VALUE;
        for (Skill s : Skill.values()) {
            int l = Players.data(p).skills().currentLevel(s);
            if (l >= minLevel && l < bestLevel) {
                best = s;
                bestLevel = l;
            }
        }
        if (best != null) {
            XpService.add(p, best, xp);
            Msg.game(p, "You rub the antique lamp and gain " + xp + " " + best.displayName() + " XP.");
        }
    }

    static void clerk(ServerPlayer p) {
        int k = kudos(p);
        boolean any = false;
        for (KudosReward r : VarrockMuseum.KUDOS_REWARDS) {
            String flag = "museum_reward_" + r.kudos();
            if (k >= r.kudos() && !data(p).flag(p.getUUID(), flag)) {
                data(p).setFlag(p.getUUID(), flag);
                for (Map.Entry<Skill, Integer> e : r.xp().entrySet()) {
                    XpService.add(p, e.getKey(), e.getValue());
                }
                QuestApi.say(p, "Information clerk", "With " + r.kudos() + " kudos you've earned a reward from the museum!");
                any = true;
            }
        }
        if (!any) {
            QuestApi.say(p, "Information clerk", "You have " + k + " kudos. Rewards await at 51, 101 and 151 kudos.");
        }
    }

    // ------------------------------------------------------------------ cleaning finds

    static void tools(ServerPlayer p) {
        List<String> given = new ArrayList<>();
        for (String t : List.of("trowel", "rock_pick", "specimen_brush", "leather_gloves", "leather_boots")) {
            if (!Inv.has(p, t, 1) && !t.equals(Inv.idOf(p.getItemBySlot(EquipmentSlot.FEET)))
                    && !com.runecraft.content.systems.SysState.of(p).wearing(t)) {
                Inv.give(p, t, 1);
                given.add(t);
            }
        }
        Msg.game(p, given.isEmpty() ? "You already have the tools you need." : "You take some tools from the rack.");
    }

    static void rocks(ServerPlayer p) {
        if (!QuestApi.isComplete(p, VarrockMuseum.DIG_SITE)) {
            Msg.game(p, "You need to complete The Dig Site before you can help clean the finds.");
            return;
        }
        Inv.give(p, "uncleaned_find", 1);
        Msg.game(p, "You take an uncleaned find from the pile.");
    }

    static void clean(ServerPlayer p) {
        if (!Inv.has(p, "uncleaned_find", 1)) {
            Msg.game(p, "You need an uncleaned find from the specimen rocks.");
            return;
        }
        // OSRS: leather boots and leather gloves equipped (the gloves go in the systems module's hands slot).
        if (!"leather_boots".equals(Inv.idOf(p.getItemBySlot(EquipmentSlot.FEET)))
                || !com.runecraft.content.systems.SysState.of(p).wearing("leather_gloves")) {
            Msg.game(p, "You need to wear leather boots and leather gloves to protect yourself while cleaning.");
            return;
        }
        for (String t : VarrockMuseum.TOOLS) {
            if (!Inv.has(p, t, 1)) {
                Msg.game(p, "You need a trowel, a rock pick and a specimen brush. There are some on the tool rack.");
                return;
            }
        }
        Inv.remove(p, "uncleaned_find", 1);
        if (p.getRandom().nextDouble() < VarrockMuseum.CRUMBLE_CHANCE) {
            Msg.game(p, "The find crumbles to dust in your hands - oops! Better not let the professionals see that one!");
            return;
        }
        List<String> owned = new ArrayList<>();
        for (String a : List.of("old_symbol", "ancient_symbol", "old_coin", "ancient_coin", "clean_necklace")) {
            if (Inv.has(p, a, 1) || data(p).flag(p.getUUID(), "museum_display_" + a)) {
                owned.add(a);
            }
        }
        String item = VarrockMuseum.cleanedFind(p.getRandom().nextDouble(), p.getRandom().nextDouble(), owned);
        Inv.give(p, item, item.equals("coins") ? 5 : 1);
        Msg.game(p, "You clean the find and uncover " + itemName(item) + ".");
    }

    private static String itemName(String id) {
        var def = com.runecraft.osrs.ItemCatalog.get(id);
        return def == null ? id : def.name().toLowerCase(java.util.Locale.ROOT);
    }

    static void display(ServerPlayer p) {
        boolean any = false;
        for (String a : VarrockMuseum.ARTEFACTS) {
            String flag = "museum_display_" + a;
            if (Inv.has(p, a, 1) && !data(p).flag(p.getUUID(), flag)) {
                Inv.remove(p, a, 1);
                data(p).setFlag(p.getUUID(), flag);
                Msg.game(p, "You place the " + itemName(a) + " in its display case.");
                addKudos(p, VarrockMuseum.ARTEFACT_KUDOS);
                any = true;
            }
        }
        if (!any) {
            Msg.game(p, "You have nothing to put on display. Old and ancient symbols and coins, and a piece of pottery, belong here.");
        }
    }

    static void crate(ServerPlayer p) {
        int n = 0;
        for (String f : VarrockMuseum.CRATE_FINDS) {
            while (Inv.has(p, f, 1)) {
                Inv.remove(p, f, 1);
                CrateLoot l = VarrockMuseum.crateRoll(p.getRandom().nextDouble());
                if (l == null) {
                    lamp(p, VarrockMuseum.CRATE_LAMP_XP, 10);
                } else {
                    Inv.give(p, l.item(), l.min() + p.getRandom().nextInt(l.max() - l.min() + 1));
                }
                n++;
            }
        }
        Msg.game(p, n == 0 ? "The crate takes pottery, jewellery, old chipped vases and arrowheads." : "You place " + n + " find"
                + (n == 1 ? "" : "s") + " in the crate and take a reward for each.");
    }

    static void archaeologist(ServerPlayer p) {
        int jewellery = Inv.count(p, "museum_jewellery");
        if (jewellery > 0) {
            Inv.remove(p, "museum_jewellery", jewellery);
            Inv.give(p, Inv.COINS, 5 * jewellery);
            QuestApi.say(p, "Archaeologist", "Old jewellery! Not much use to us, but here's 5 coins apiece.");
            return;
        }
        if (Inv.has(p, "clean_necklace", 1)) {
            Inv.remove(p, "clean_necklace", 1);
            data(p).setFlag(p.getUUID(), "museum_display_clean_necklace");
            QuestApi.say(p, "Archaeologist", "This necklace is from the Digsite! A Lvl-3 Enchant on a ruby necklace would make a pendant "
                    + "that takes you there.");
            return;
        }
        QuestApi.say(p, "Archaeologist", "Clean the finds at the specimen tables. Artefacts go in the display cases, the rest in the "
                + "storage crate.");
    }

    // ------------------------------------------------------------------ placement

    /** Places the cleaning area's blocks on the museum's ground floor (south end, OSRS tile row 3441). */
    private static void build(ServerLevel world, BlockPos centre) {
        String[] ids = {VarrockMuseum.SPECIMEN_ROCKS, VarrockMuseum.SPECIMEN_TABLE, VarrockMuseum.SPECIMEN_TABLE, VarrockMuseum.TOOL_RACK,
                VarrockMuseum.STORAGE_CRATE, VarrockMuseum.DISPLAY_CASE, VarrockMuseum.PLAQUE};
        int z = GielinorMap.toBlockZ(3440) - 1;
        for (int i = 0; i < ids.length; i++) {
            Block b = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("runecraft", ids[i]));
            if (b == net.minecraft.world.level.block.Blocks.AIR) {
                continue;
            }
            int x = GielinorMap.toBlockX(3253) + 3 * i;
            Integer y = floor(world, x, z);
            if (y != null) {
                world.setBlockAndUpdate(new BlockPos(x, y, z), b.defaultBlockState());
            }
        }
    }

    /** The first air block above a solid floor, searching up from below the ground. */
    private static Integer floor(ServerLevel world, int x, int z) {
        for (int y = 56; y < 80; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (world.getBlockState(pos).isAir() && !world.getBlockState(pos.below()).isAir()
                    && world.getBlockState(pos.above()).isAir()) {
                return y;
            }
        }
        return null;
    }
}
