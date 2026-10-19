package com.runecraft.content.systems;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.runecraft.osrs.Experience;
import com.runecraft.osrs.QuestInfo;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.systems.Capes;
import com.runecraft.osrs.content.systems.ClueScrolls;
import com.runecraft.osrs.content.systems.Diaries;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Skillcapes, max/quest/diary capes, the hiscores and the module's chat commands. */
public final class CapeService {
    private CapeService() {
    }

    static int count99(PlayerData d) {
        int n = 0;
        for (Skill s : Skill.values()) {
            if (d.skills().level(s) >= 99) {
                n++;
            }
        }
        return n;
    }

    static boolean allQuests(PlayerData d) {
        List<QuestInfo> quests = ContentRegistry.quests();
        for (QuestInfo q : quests) {
            if (!d.questComplete(q)) {
                return false;
            }
        }
        return !quests.isEmpty();
    }

    public static void open(ServerPlayer p) {
        PlayerData d = Players.data(p);
        SysState.SysPlayer sp = SysState.of(p);
        int n99 = count99(d);
        SysNet.Screen s = new SysNet.Screen("capes", "Capes of accomplishment");
        s.line("Skillcapes cost " + String.format(Locale.ROOT, "%,d", Capes.SKILLCAPE_PRICE) + " coins. With more than one 99 you receive the trimmed version.",
                0xC0C0C0);
        s.line("You have " + n99 + " skill" + (n99 == 1 ? "" : "s") + " at level 99.");
        for (Skill sk : Skill.values()) {
            if (d.skills().level(sk) >= 99) {
                s.button("skill:" + sk.name(), "Buy " + sk.displayName() + " cape", "", true);
            }
        }
        boolean max = n99 == Skill.count();
        long maxPrice = (long) Capes.MAX_CAPE_PRICE_PER_SKILL * (Skill.count() - 1);
        s.button("max", "Max cape (" + String.format(Locale.ROOT, "%,d", maxPrice) + ")", max ? "" : "Requires 99 in every skill.", max);
        boolean qp = allQuests(d);
        s.button("qp", "Quest point cape (99,000)", qp ? "" : "Requires every quest to be completed.", qp);
        boolean diary = Diaries.allElite(sp.diaryDone);
        s.button("diary", "Achievement diary cape (99,000)", diary ? "" : "Requires every achievement diary to be completed.", diary);
        s.send(p);
    }

    static void action(ServerPlayer p, String action, Map<String, String> fields) {
        PlayerData d = Players.data(p);
        String item;
        long price = Capes.SKILLCAPE_PRICE;
        if (action.startsWith("skill:")) {
            Skill sk;
            try {
                sk = Skill.valueOf(action.substring(6));
            } catch (IllegalArgumentException e) {
                return;
            }
            if (d.skills().level(sk) < 99) {
                return;
            }
            item = Capes.capeId(sk, count99(d) > 1);
        } else if (action.equals("max")) {
            if (count99(d) < Skill.count()) {
                return;
            }
            item = "max_cape";
            price = (long) Capes.MAX_CAPE_PRICE_PER_SKILL * (Skill.count() - 1);
        } else if (action.equals("qp")) {
            if (!allQuests(d)) {
                return;
            }
            item = "quest_point_cape";
        } else if (action.equals("diary")) {
            if (!Diaries.allElite(SysState.of(p).diaryDone)) {
                return;
            }
            item = "achievement_diary_cape";
        } else {
            return;
        }
        if (price > Integer.MAX_VALUE || !Inv.has(p, "coins", (int) price)) {
            Msg.game(p, "You need " + String.format(Locale.ROOT, "%,d", price) + " coins.");
            return;
        }
        Inv.remove(p, "coins", (int) price);
        Inv.give(p, item, 1);
        com.runecraft.network.ServerNetworking.sync(p);
        Msg.action(p, "You purchase the " + com.runecraft.osrs.ItemCatalog.get(item).name() + ". Wear it with pride!");
    }

    // ---------------- Hiscores ----------------

    public static void snapshot(ServerPlayer p) {
        SysState state = SysState.get(p.getServer());
        int[] xp = new int[Skill.count()];
        PlayerData d = Players.data(p);
        for (Skill s : Skill.values()) {
            xp[s.ordinal()] = d.skills().xpTenths(s);
        }
        state.hiscores().put(p.getUUID(), new SysState.HiscoreEntry(p.getName().getString(), xp));
    }

    /** Ranked lines for overall (skill null) or one skill. */
    public static List<String> ranking(MinecraftServer server, Skill skill, int limit) {
        List<Map.Entry<UUID, SysState.HiscoreEntry>> list = new ArrayList<>(SysState.get(server).hiscores().entrySet());
        if (skill == null) {
            list.sort((a, b) -> {
                int c = Integer.compare(b.getValue().totalLevel(), a.getValue().totalLevel());
                return c != 0 ? c : Long.compare(b.getValue().totalXp(), a.getValue().totalXp());
            });
        } else {
            list.sort((a, b) -> Integer.compare(b.getValue().xpTenths()[skill.ordinal()], a.getValue().xpTenths()[skill.ordinal()]));
        }
        List<String> out = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<UUID, SysState.HiscoreEntry> e : list) {
            if (rank > limit) {
                break;
            }
            SysState.HiscoreEntry h = e.getValue();
            if (skill == null) {
                out.add(rank + ". " + h.name() + " - total " + h.totalLevel() + " (" + String.format(Locale.ROOT, "%,d", h.totalXp()) + " xp)");
            } else {
                int tenths = h.xpTenths()[skill.ordinal()];
                out.add(rank + ". " + h.name() + " - level " + Experience.levelForTenths(tenths) + " ("
                        + String.format(Locale.ROOT, "%,d", tenths / 10) + " xp)");
            }
            rank++;
        }
        return out;
    }

    // ---------------- Commands ----------------

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            register(dispatcher, "rc");
            register(dispatcher, "runecraft");
        });
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher, String root) {
        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal(root)
                .then(Commands.literal("hiscores")
                        .executes(ctx -> hiscores(ctx.getSource(), null))
                        .then(Commands.argument("skill", StringArgumentType.word())
                                .executes(ctx -> hiscores(ctx.getSource(), StringArgumentType.getString(ctx, "skill")))))
                .then(Commands.literal("diary").executes(ctx -> {
                    SysMenus.diaries(ctx.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("ge").executes(ctx -> {
                    GeService.open(ctx.getSource().getPlayerOrException(), null, null);
                    return 1;
                }))
                .then(Commands.literal("spellbook").executes(ctx -> {
                    SysMenus.spellbook(ctx.getSource().getPlayerOrException(), 0);
                    return 1;
                }))
                .then(Commands.literal("worn").executes(ctx -> {
                    SysMenus.worn(ctx.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("spec").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Msg.game(p, "Special attack energy: " + SysState.of(p).specEnergy + "%");
                    return 1;
                }))
                .then(Commands.literal("clue").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("tier", StringArgumentType.word()).executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            ClueScrolls.Tier t = ClueScrolls.Tier.byScroll("clue_scroll_" + StringArgumentType.getString(ctx, "tier").toLowerCase(Locale.ROOT));
                            if (t == null) {
                                ctx.getSource().sendFailure(Component.literal("Unknown tier"));
                                return 0;
                            }
                            Inv.give(p, t.scrollItem(), 1);
                            return 1;
                        })))
                .then(Commands.literal("specenergy").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    SpecialAttackService.restoreEnergy(p, 100);
                    return 1;
                }));
        dispatcher.register(node);
    }

    private static int hiscores(CommandSourceStack src, String skillName) {
        Skill skill = null;
        if (skillName != null && !skillName.equalsIgnoreCase("overall")) {
            skill = Skill.byId(skillName.toLowerCase(Locale.ROOT));
            if (skill == null) {
                src.sendFailure(Component.literal("Unknown skill: " + skillName));
                return 0;
            }
        }
        if (src.getPlayer() != null) {
            snapshot(src.getPlayer());
        }
        List<String> lines = ranking(src.getServer(), skill, 10);
        String title = "Hiscores - " + (skill == null ? "Overall" : skill.displayName());
        src.sendSuccess(() -> Component.literal(title).withStyle(ChatFormatting.GOLD), false);
        if (lines.isEmpty()) {
            src.sendSuccess(() -> Component.literal("No players ranked yet."), false);
        }
        for (String l : lines) {
            src.sendSuccess(() -> Component.literal(l), false);
        }
        return 1;
    }
}
