package com.runecraft.content.minigames;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.runecraft.content.minigames.games.EmirsArenaSession;
import com.runecraft.content.systems.SysState;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.content.minigames.EmirsArena;
import com.runecraft.osrs.content.minigames.EmirsArena.DuelSlot;
import com.runecraft.osrs.content.minigames.EmirsArena.Options;
import com.runecraft.osrs.content.minigames.EmirsArena.Rule;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameCurrencies;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.quest.QuestApi;
import com.runecraft.quest.QuestScripts;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import com.runecraft.platform.CommandRegistrationCallback;
import com.runecraft.platform.UseEntityCallback;
import com.runecraft.platform.UseItemCallback;
import com.runecraft.platform.ServerPlayConnectionEvents;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Emir's Arena outside the fights (see {@link EmirsArena}): challenging another player inside the arena grounds (right-click
 * them, or /duel challenge), accepting (right-click the challenger back, or /duel accept), the challenger's duel options
 * (/duel rule, /duel slot, /duel preset), Mubariz's PvP Arena Rewards, Arena Guard Yon, the No Food / No Drinks options and
 * the scroll of imbuing.
 */
public final class EmirsArenaService {
    /** A challenge lasts 30 seconds (600 server ticks). // approx */
    private static final int CHALLENGE_TICKS = 600;

    private record Challenge(UUID target, int at, Options options) {
    }

    private static final Map<UUID, Options> OPTIONS = new HashMap<>();
    private static final Map<UUID, Challenge> CHALLENGES = new HashMap<>();

    private EmirsArenaService() {
    }

    static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (hand != InteractionHand.MAIN_HAND || world.isClientSide() || !(player instanceof ServerPlayer sp)
                    || !(entity instanceof ServerPlayer target) || !inArena(sp)) {
                return InteractionResult.PASS;
            }
            challenge(sp, target);
            return InteractionResult.SUCCESS;
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (world.isClientSide() || !(player instanceof ServerPlayer sp) || stack.isEmpty()
                    || !(MinigameManager.session(sp) instanceof EmirsArenaSession duel)) {
                return InteractionResultHolder.pass(stack);
            }
            ItemDef def = ItemCatalog.get(Inv.idOf(stack));
            if (def != null && duel.blocksConsumable(def)) {
                Msg.game(sp, def.kind() == ItemDef.Kind.POTION ? "Drinks have been disabled for this duel."
                        : "Food has been disabled for this duel.");
                return InteractionResultHolder.fail(stack);
            }
            return InteractionResultHolder.pass(stack);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID u = handler.getPlayer().getUUID();
            CHALLENGES.remove(u);
            OPTIONS.remove(u);
        });
        QuestScripts.onTalk(EmirsArena.MUBARIZ, (player, npc) -> {
            int pts = MinigameData.of(player).points(player.getUUID(), MinigameCurrencies.key(EmirsArena.POINTS));
            QuestApi.say(player, "Mubariz", "Welcome to the Emir's Arena! Spend your reward points here. You have " + pts
                    + " (at most " + EmirsArena.MAX_POINTS + ").");
            RewardShopService.open(player, EmirsArena.SHOP);
        });
        QuestScripts.onTalk(EmirsArena.GUARD, (player, npc) -> guard(player));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                Commands.literal("duel")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayer();
                            if (p != null) {
                                showOptions(p);
                            }
                            return 1;
                        })
                        .then(Commands.literal("rule").then(Commands.argument("rule", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p != null) {
                                        toggleRule(p, StringArgumentType.getString(ctx, "rule"));
                                    }
                                    return 1;
                                })))
                        .then(Commands.literal("slot").then(Commands.argument("slot", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p != null) {
                                        toggleSlot(p, StringArgumentType.getString(ctx, "slot"));
                                    }
                                    return 1;
                                })))
                        .then(Commands.literal("preset").then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p != null) {
                                        preset(p, StringArgumentType.getString(ctx, "name"));
                                    }
                                    return 1;
                                })))
                        .then(Commands.literal("challenge").then(Commands.argument("player", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p != null) {
                                        byName(p, StringArgumentType.getString(ctx, "player"));
                                    }
                                    return 1;
                                })))
                        .then(Commands.literal("accept").then(Commands.argument("player", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p != null) {
                                        byName(p, StringArgumentType.getString(ctx, "player"));
                                    }
                                    return 1;
                                })))));
    }

    static boolean inArena(ServerPlayer p) {
        return Gielinor.is(p) && EmirsArena.inArena(GielinorMap.toOsrsX(p.getBlockX()), GielinorMap.toOsrsY(p.getBlockZ()));
    }

    static Options options(ServerPlayer p) {
        return OPTIONS.getOrDefault(p.getUUID(), Options.none());
    }

    private static void guard(ServerPlayer p) {
        MinigameData data = MinigameData.of(p);
        UUID u = p.getUUID();
        int stored = data.stat(u, EmirsArena.RANK);
        int rank = stored == 0 ? EmirsArena.START_RANK : stored;
        QuestApi.say(p, "Arena Guard Yon", "Welcome to the Emir's Arena. Challenge another fighter anywhere in the arena grounds and "
                + "the duel takes place in one of our combat areas. Staking is not allowed any more: the Emir won't have gambling here. "
                + "For ranked fights, sign up at the recruitment board by the gate.");
        Msg.game(p, "Rank " + rank + " (" + EmirsArena.rankTitle(rank) + "), ranked wins " + data.stat(u, EmirsArena.WINS)
                + ", losses " + data.stat(u, EmirsArena.LOSSES) + "; legacy duels won " + data.stat(u, EmirsArena.LEGACY_WINS)
                + ", lost " + data.stat(u, EmirsArena.LEGACY_LOSSES) + ".");
        showOptions(p);
    }

    private static void showOptions(ServerPlayer p) {
        Msg.game(p, "Your duel options: " + options(p).describe() + ".");
        StringBuilder rules = new StringBuilder();
        for (Rule r : Rule.values()) {
            rules.append(rules.length() == 0 ? "" : ", ").append(r.key());
        }
        Msg.game(p, "/duel rule <" + rules + ">, /duel slot <head|cape|neck|ammo|weapon|body|shield|legs|hands|feet|ring>, "
                + "/duel preset <whip|boxing|clear>, /duel challenge <player>.");
    }

    private static void toggleRule(ServerPlayer p, String key) {
        Rule r = Rule.byKey(key);
        if (r == null) {
            Msg.game(p, "There is no duel option called " + key + ".");
            return;
        }
        Options o = options(p).toggle(r);
        OPTIONS.put(p.getUUID(), o);
        Msg.game(p, r.displayName + (o.has(r) ? " is on." : " is off."));
    }

    private static void toggleSlot(ServerPlayer p, String key) {
        DuelSlot s = DuelSlot.byKey(key);
        if (s == null) {
            Msg.game(p, "There is no equipment slot called " + key + ".");
            return;
        }
        Options o = options(p).toggle(s);
        OPTIONS.put(p.getUUID(), o);
        Msg.game(p, "The " + s.key() + " slot is " + (o.disabled(s) ? "disabled." : "enabled."));
    }

    private static void preset(ServerPlayer p, String name) {
        Options o = name.equalsIgnoreCase("clear") ? Options.none() : EmirsArena.preset(name);
        if (o == null) {
            Msg.game(p, "The presets are whip, boxing and clear.");
            return;
        }
        OPTIONS.put(p.getUUID(), o);
        Msg.game(p, "Duel options: " + o.describe() + ".");
    }

    private static void byName(ServerPlayer p, String name) {
        ServerPlayer target = p.getServer().getPlayerList().getPlayerByName(name);
        if (target == null) {
            Msg.game(p, "There is no player called " + name + " online.");
            return;
        }
        if (!inArena(p)) {
            Msg.game(p, "You can only challenge players inside the Emir's Arena.");
            return;
        }
        challenge(p, target);
    }

    /** Challenges {@code target}, or accepts the challenge {@code target} sent first. */
    static void challenge(ServerPlayer p, ServerPlayer target) {
        if (target == p || !inArena(target)) {
            Msg.game(p, "You can only challenge players inside the Emir's Arena.");
            return;
        }
        if (MinigameManager.session(p) != null || MinigameManager.session(target) != null) {
            Msg.game(p, "That player is busy at the moment.");
            return;
        }
        int now = p.getServer().getTickCount();
        Challenge theirs = CHALLENGES.get(target.getUUID());
        if (theirs != null && theirs.target().equals(p.getUUID()) && now - theirs.at() <= CHALLENGE_TICKS) {
            CHALLENGES.remove(target.getUUID());
            CHALLENGES.remove(p.getUUID());
            String problem = EmirsArena.problem(theirs.options());
            if (problem != null) {
                Msg.game(p, problem);
                Msg.game(target, problem);
                return;
            }
            Msg.game(p, "You accept " + target.getName().getString() + "'s challenge. Rules: " + theirs.options().describe() + ".");
            Msg.game(target, p.getName().getString() + " accepts your challenge.");
            MinigameManager.start(Minigame.EMIRS_ARENA, EmirsArena.DUEL_PREFIX + theirs.options().encode(), List.of(target, p));
            return;
        }
        Challenge mine = CHALLENGES.get(p.getUUID());
        if (mine != null && mine.target().equals(target.getUUID()) && now - mine.at() < 10) {
            return; // the same click arrives twice (interact and interact-at)
        }
        Options o = options(p);
        String problem = EmirsArena.problem(o);
        if (problem != null) {
            Msg.game(p, problem);
            return;
        }
        CHALLENGES.put(p.getUUID(), new Challenge(target.getUUID(), now, o));
        Msg.game(p, "Sending duel request to " + target.getName().getString() + "...");
        Msg.game(p, "As this is an unofficial duel, you will not receive rank or reward points for participating.");
        Msg.game(target, p.getName().getString() + " wishes to duel with you (" + o.describe() + "). Right-click them or type "
                + "/duel accept " + p.getName().getString() + " to accept.");
    }

    /**
     * Uses a scroll of imbuing on the first item the player wears or carries that can be imbued (the Nightmare Zone / Soul
     * Wars list). Returns true if the scroll was used up.
     */
    /** What the scroll turns {@code item} into, or null (also when a needed tool, the holy wrench, isn't carried). */
    private static String imbuable(ServerPlayer p, String item) {
        String to = EmirsArena.imbued(item);
        String tool = to == null ? null : com.runecraft.osrs.content.minigames.MinigameRules.Imbues.tool(item);
        if (tool != null && !Inv.has(p, tool, 1)) {
            Msg.game(p, "You need a holy wrench in your inventory to imbue the ring of the gods.");
            return null;
        }
        return to;
    }

    static boolean useScrollOfImbuing(ServerPlayer p) {
        SysState.SysPlayer sp = SysState.of(p);
        for (Jewellery.Slot slot : new Jewellery.Slot[]{Jewellery.Slot.RING, Jewellery.Slot.NECK}) {
            String to = imbuable(p, sp.wornId(slot));
            if (to != null) {
                sp.worn.put(slot, Inv.stack(to, 1));
                imbuedMessage(p, to);
                return true;
            }
        }
        String head = imbuable(p, Inv.idOf(p.getItemBySlot(EquipmentSlot.HEAD)));
        if (head != null) {
            p.setItemSlot(EquipmentSlot.HEAD, Inv.stack(head, 1));
            imbuedMessage(p, head);
            return true;
        }
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            String to = s.isEmpty() ? null : imbuable(p, Inv.idOf(s));
            if (to != null) {
                s.shrink(1);
                if (s.isEmpty()) {
                    inv.setItem(i, Inv.stack(to, 1));
                } else {
                    Inv.give(p, to, 1);
                }
                imbuedMessage(p, to);
                return true;
            }
        }
        Msg.game(p, "You have nothing the scroll can imbue (slayer helmets, black masks, salve amulets and the imbuable rings).");
        return false;
    }

    private static void imbuedMessage(ServerPlayer p, String to) {
        ItemDef def = ItemCatalog.get(to);
        Msg.game(p, "The scroll imbues your item: you now have " + (def == null ? to : def.name()) + ".");
    }
}
