package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.collection.CollectionLog;
import com.runecraft.osrs.content.bosses.RoyalTitansLoot;
import com.runecraft.osrs.content.bosses.RoyalTitansLoot.Choice;
import com.runecraft.platform.CommandRegistrationCallback;
import com.runecraft.skill.Drops;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * The fallen Royal Titans (OSRS Wiki "Royal Titans" Rewards): every player who damaged them chooses, through chat links
 * ({@code /titans loot|pages branda|eldric}, {@code /titans sacrifice}), to loot one Titan, sacrifice the loot for a better
 * pet chance, or take desiccated pages. Each player's rewards are rolled on their own contribution
 * ({@link RoyalTitansLoot}); the kill drops nothing itself. A choice left open for five minutes loots Branda.
 */
public final class TitansLoot {
    private static final long TIMEOUT_MC_TICKS = 5 * 60 * 20;

    /** A player's open choice: their damage to the Titans and the fallen Branda (for the drop transformers). */
    private record Pending(int damage, DataBossEntity branda, long since, boolean killer) {
    }

    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    /** The last choice made (self-test: what each player received). */
    private static final Map<UUID, List<RoyalTitansLoot.Loot>> LAST = new HashMap<>();

    private TitansLoot() {
    }

    static void register() {
        DataBossService.loot(RoyalTitansLoot.BRANDA, (killer, boss, table) -> {
            open(killer, boss);
            return List.of(); // each fighter chooses (and rolls) their own loot
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> dispatcher.register(Commands.literal("titans")
                .then(Commands.literal("loot").then(Commands.argument("titan", StringArgumentType.word()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayer();
                    if (p != null) {
                        choose(p, Choice.LOOT, StringArgumentType.getString(ctx, "titan"));
                    }
                    return 1;
                })))
                .then(Commands.literal("pages").then(Commands.argument("titan", StringArgumentType.word()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayer();
                    if (p != null) {
                        choose(p, Choice.PAGES, StringArgumentType.getString(ctx, "titan"));
                    }
                    return 1;
                })))
                .then(Commands.literal("sacrifice").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayer();
                    if (p != null) {
                        choose(p, Choice.SACRIFICE, RoyalTitansLoot.BRANDA);
                    }
                    return 1;
                }))));
        com.runecraft.platform.ServerTickEvents.END_SERVER_TICK.register(TitansLoot::timeouts);
    }

    /** The Titans fell: every fighter still in the lair (and the killer) gets their choice. */
    static void open(ServerPlayer killer, DataBossEntity branda) {
        EncounterScripts.RoyalTitans titans = branda.script() instanceof EncounterScripts.RoyalTitans t ? t : null;
        long now = killer.getServer().getTickCount();
        boolean killerFought = false;
        if (titans != null) {
            for (ServerPlayer p : titans.fighters()) {
                boolean inLair = p.isAlive() && p.level() == branda.level() && p.distanceToSqr(branda) < 48 * 48;
                if (p == killer || inLair) {
                    killerFought |= p == killer;
                    offer(p, new Pending(titans.damageBy(p), branda, now, p == killer));
                }
            }
        }
        if (!killerFought) {
            // a kill by a player the script didn't see hit (a damage path without the hit hook): full credit for the killer
            offer(killer, new Pending(RoyalTitansLoot.TOTAL_HITPOINTS, branda, now, true));
        }
    }

    private static void offer(ServerPlayer p, Pending pending) {
        Pending before = PENDING.put(p.getUUID(), pending);
        if (before != null) {
            resolve(p, before, Choice.LOOT, RoyalTitansLoot.BRANDA); // a choice left from an earlier kill loots Branda
        }
        prompt(p, pending);
    }

    private static MutableComponent button(String label, String command, String hover, ChatFormatting colour) {
        return Component.literal(label).withStyle(s -> s.withColor(colour)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hover))));
    }

    private static void prompt(ServerPlayer p, Pending pending) {
        p.sendSystemMessage(Component.literal("The Royal Titans lie defeated (your contribution: "
                        + RoyalTitansLoot.percent(pending.damage()) + "%). ").withStyle(ChatFormatting.RED)
                .append(button("[Loot Branda]", "/titans loot branda", "Two rolls on Branda's table (or a unique).", ChatFormatting.GOLD))
                .append(Component.literal(" "))
                .append(button("[Loot Eldric]", "/titans loot eldric", "Two rolls on Eldric's table (or a unique).", ChatFormatting.AQUA))
                .append(Component.literal(" "))
                .append(button("[Sacrifice]", "/titans sacrifice", "Forfeit the loot to double your chance at the pet.",
                        ChatFormatting.DARK_RED))
                .append(Component.literal(" "))
                .append(button("[Take pages]", "/titans pages branda", "10-19 desiccated pages.", ChatFormatting.GRAY)));
    }

    /** Makes a player's choice; false when they have none open. Public for the self-test. */
    public static boolean choose(ServerPlayer p, Choice choice, String titanName) {
        String titan = RoyalTitansLoot.titan(titanName);
        if (titan == null) {
            Msg.game(p, "Loot Branda or Eldric.");
            return false;
        }
        Pending pending = PENDING.remove(p.getUUID());
        if (pending == null) {
            Msg.game(p, "There are no fallen Titans of yours to loot.");
            return false;
        }
        resolve(p, pending, choice, titan);
        return true;
    }

    private static void resolve(ServerPlayer p, Pending pending, Choice choice, String titan) {
        List<RoyalTitansLoot.Loot> loot = RoyalTitansLoot.roll(titan, choice, pending.damage(),
                new java.util.Random(p.getRandom().nextLong()));
        LAST.put(p.getUUID(), loot);
        for (RoyalTitansLoot.Loot l : loot) {
            int doses = com.runecraft.osrs.data.Drop.dosesOf(l.item());
            String item = com.runecraft.osrs.data.Drop.baseOf(l.item());
            Inv.give(p, Drops.withDoses(com.runecraft.combat.CombatHooks.dropStack(p, pending.branda(), item, l.count()), doses));
            CollectionLog.obtain(p, item);
        }
        // The killer's kill count and first pet roll came with the kill; the others' come here. Sacrificing adds a pet roll
        // (1/3,000 twice: about the wiki's 1/1,500).
        int petRolls = (pending.killer() ? 0 : 1) + (choice == Choice.SACRIFICE ? 1 : 0);
        for (int i = 0; i < petRolls; i++) {
            if (p.getRandom().nextInt(RoyalTitansLoot.PET) == 0) {
                Inv.give(p, "pet_bran", 1);
                CollectionLog.obtain(p, "pet_bran");
                p.getServer().getPlayerList().broadcastSystemMessage(Component.literal(p.getName().getString()
                        + " has a funny feeling like they're being followed.").withStyle(ChatFormatting.GOLD), false);
            } else if (choice == Choice.SACRIFICE && i == petRolls - 1) {
                Msg.game(p, "You don't feel lucky during your sacrifice.");
            }
        }
        if (!pending.killer()) {
            int kc = com.runecraft.player.Players.data(p).incrementKillCount(RoyalTitansLoot.BRANDA);
            Msg.game(p, "Your Royal Titans kill count is: " + kc + ".");
            if (pending.branda().def() != null) {
                com.runecraft.content.GameEvents.fireKill(p, pending.branda(), com.runecraft.combat.MonsterStats.from(pending.branda().def()));
            }
        }
        Msg.game(p, switch (choice) {
            case LOOT -> "You loot " + (RoyalTitansLoot.ELDRIC.equals(titan) ? "Eldric the Ice King." : "Branda the Fire Queen.");
            case SACRIFICE -> "You sacrifice the Titans' loot.";
            case PAGES -> "You take the Titans' pages.";
        });
    }

    private static void timeouts(MinecraftServer server) {
        if (PENDING.isEmpty() || server.getTickCount() % 100 != 0) {
            return;
        }
        for (Iterator<Map.Entry<UUID, Pending>> it = PENDING.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Pending> e = it.next();
            if (server.getTickCount() - e.getValue().since() < TIMEOUT_MC_TICKS) {
                continue;
            }
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p != null) {
                it.remove();
                resolve(p, e.getValue(), Choice.LOOT, RoyalTitansLoot.BRANDA);
            }
        }
    }

    /** Self-test: the damage behind this player's open choice, or -1 when none is open. */
    public static int pendingDamage(ServerPlayer p) {
        Pending pending = PENDING.get(p.getUUID());
        return pending == null ? -1 : pending.damage();
    }

    /** Self-test: what this player's last choice gave. */
    public static List<RoyalTitansLoot.Loot> lastLoot(ServerPlayer p) {
        return LAST.getOrDefault(p.getUUID(), List.of());
    }

    /** Self-test: open choices held for the player (the fight check counts them as the kill's loot). */
    public static int held(ServerPlayer p) {
        return PENDING.containsKey(p.getUUID()) ? 1 : 0;
    }
}
