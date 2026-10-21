package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossService;
import com.runecraft.collection.CollectionLog;
import com.runecraft.osrs.content.bosses.BossLoot;
import com.runecraft.osrs.content.bosses.SlayerBosses;
import com.runecraft.platform.CommandRegistrationCallback;
import com.runecraft.skill.Drops;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Araxxor's corpse (OSRS Wiki "Araxxor/Strategies"): after the kill the player chooses to harvest it, for the kill's loot,
 * or destroy it, forfeiting the loot (elite clue scrolls excepted) for a second roll at the pet. The loot is rolled at the
 * kill and held here until the choice (chat links run {@code /araxxor harvest|destroy}); an undecided corpse is harvested
 * when the player kills Araxxor again or after five minutes, and handed to a player still online when the server stops. A kill
 * under 1:15 adds coagulated venom to the corpse's loot.
 */
public final class AraxxorCorpse {
    static final String BOSS = "araxxor";
    private static final long TIMEOUT_MC_TICKS = 5 * 60 * 20;
    /** The kill-under-1:15 reward ({@link SlayerBosses#coagulatedVenom}). */
    static final String COAGULATED_VENOM = "coagulated_venom";

    /** One rolled drop and the stack it drops as (made at the kill, through the drop transformers, like any kill's loot). */
    private record Loot(Drops.Rolled rolled, ItemStack stack) {
    }

    private record Pending(List<Loot> loot, ServerLevel world, Vec3 at, long since) {
    }

    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private AraxxorCorpse() {
    }

    static void register() {
        DataBossService.loot(BOSS, (killer, boss, table) -> {
            net.minecraft.util.RandomSource random = boss.getRandom();
            List<Loot> loot = new ArrayList<>();
            List<Drops.Rolled> rolled = new ArrayList<>(Drops.resolve(BossLoot.pick(BOSS, table, random::nextInt), random));
            Pending before = PENDING.remove(killer.getUUID());
            if (before != null) {
                harvest(killer, before); // a corpse left undecided is harvested
            }
            // A kill under 1:15 by a player without one guarantees coagulated venom (a tertiary: lost with the corpse).
            if (boss.script() instanceof SlayerScripts.Araxxor a
                    && SlayerBosses.coagulatedVenom(a.fightTicks(), owns(killer, COAGULATED_VENOM))) {
                rolled.add(new Drops.Rolled(COAGULATED_VENOM, 1));
            }
            for (Drops.Rolled r : rolled) {
                loot.add(new Loot(r, Drops.withDoses(com.runecraft.combat.CombatHooks.dropStack(killer, boss, r.item(), r.count()),
                        r.doses())));
            }
            PENDING.put(killer.getUUID(), new Pending(loot, (ServerLevel) boss.level(), boss.position(),
                    killer.getServer().getTickCount()));
            prompt(killer);
            return List.of(); // nothing drops until the corpse is harvested
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> dispatcher.register(Commands.literal("araxxor")
                .then(Commands.literal("harvest").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayer();
                    if (p != null) {
                        decide(p, true);
                    }
                    return 1;
                }))
                .then(Commands.literal("destroy").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayer();
                    if (p != null) {
                        decide(p, false);
                    }
                    return 1;
                }))));
        com.runecraft.platform.ServerTickEvents.END_SERVER_TICK.register(AraxxorCorpse::timeouts);
        // No corpse outlives its server (it holds that server's world and tick count): a player still online gets the loot
        // in their pack (overflow at their feet), as if harvested; the rest is gone, as an instance's loot is on logout.
        com.runecraft.platform.ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (Map.Entry<UUID, Pending> e : PENDING.entrySet()) {
                ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
                if (p != null) {
                    for (Loot l : e.getValue().loot()) {
                        if (!l.stack().isEmpty()) {
                            Inv.give(p, l.stack().copy());
                        }
                        CollectionLog.obtain(p, l.rolled().item());
                    }
                }
            }
            PENDING.clear();
        });
    }

    /** Whether the player carries or banks the item. */
    private static boolean owns(ServerPlayer p, String item) {
        if (Inv.has(p, item, 1)) {
            return true;
        }
        Pending held = PENDING.get(p.getUUID());
        if (held != null && held.loot().stream().anyMatch(l -> l.rolled().item().equals(item))) {
            return true;
        }
        var bank = com.runecraft.player.Players.data(p).bank();
        return bank != null && bank.count(item, null) > 0;
    }

    private static MutableComponent button(String label, String command, String hover, ChatFormatting colour) {
        return Component.literal(label).withStyle(s -> s.withColor(colour)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hover))));
    }

    private static void prompt(ServerPlayer p) {
        p.sendSystemMessage(Component.literal("Araxxor's corpse lies before you. ").withStyle(ChatFormatting.RED)
                .append(button("[Harvest]", "/araxxor harvest", "Take the loot.", ChatFormatting.GOLD))
                .append(Component.literal(" "))
                .append(button("[Destroy]", "/araxxor destroy",
                        "Forfeit the loot (elite clue scrolls excepted) for a second chance at the pet.", ChatFormatting.DARK_RED)));
    }

    /** Harvests (true) or destroys the player's Araxxor corpse; false when there is none. Public for the self-test. */
    public static boolean decide(ServerPlayer p, boolean harvest) {
        Pending c = PENDING.remove(p.getUUID());
        if (c == null) {
            Msg.game(p, "There's no Araxxor corpse of yours to " + (harvest ? "harvest" : "destroy") + ".");
            return false;
        }
        if (harvest) {
            harvest(p, c);
            return true;
        }
        // Destroyed: only elite clue scrolls survive, and the pet gets another roll.
        List<Loot> kept = new ArrayList<>();
        for (Loot l : c.loot()) {
            if (l.rolled().item().startsWith("clue_scroll_elite")) {
                kept.add(l);
            }
        }
        drop(p, new Pending(kept, c.world(), c.at(), c.since()));
        int petRate = com.runecraft.osrs.content.extras.Pets.engineRate(BOSS);
        if (petRate > 0 && p.getRandom().nextInt(petRate) == 0) {
            Inv.give(p, "pet_nid", 1);
            CollectionLog.obtain(p, "pet_nid");
            p.getServer().getPlayerList().broadcastSystemMessage(Component.literal(p.getName().getString()
                    + " has a funny feeling like they're being followed.").withStyle(ChatFormatting.GOLD), false);
        }
        Msg.game(p, "You destroy Araxxor's corpse.");
        return true;
    }

    private static void harvest(ServerPlayer p, Pending c) {
        drop(p, c);
        Msg.game(p, "You harvest Araxxor's corpse.");
    }

    /** Drops a corpse's loot where Araxxor died and logs it. */
    private static void drop(ServerPlayer p, Pending c) {
        for (Loot l : c.loot()) {
            if (!l.stack().isEmpty()) {
                // one entity per full stack: one holding more than 99 (Araxxor's 1,200 pure essence) can't be saved
                // laid still where Araxxor died, the harvester's for 60 s (round 43, as every monster drop)
                com.runecraft.combat.LootDrops.spawnAt(c.world(), c.at(), l.stack().copy(), p, null);
            }
            CollectionLog.obtain(p, l.rolled().item());
        }
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
                harvest(p, e.getValue());
            }
        }
    }

    /** Stacks held on the player's undecided corpse (the fight self-test counts them as the kill's loot). */
    public static int held(ServerPlayer p) {
        Pending c = PENDING.get(p.getUUID());
        return c == null ? 0 : c.loot().size();
    }
}
