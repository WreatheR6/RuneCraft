package com.runecraft.content.minigames;

import com.runecraft.action.ActionManager;
import com.runecraft.collection.CollectionLog;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.GameEvents;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.minigames.ShadesOfMortton;
import com.runecraft.osrs.content.minigames.ShadesOfMortton.PyreLog;
import com.runecraft.osrs.content.minigames.ShadesOfMortton.Shade;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shades of Mort'ton in town: sanctity (earned by repairing the Flamtaer temple and killing Loar shades,
 * -1% a minute, lost on logout), the sacred fire (olive oil to sacred oil, 0.9% sanctity a dose, at
 * least 10%), pyre logs (logs + sacred oil, 5 Firemaking XP a dose), the funeral pyre (cremating shade
 * remains for Firemaking and Prayer XP and a key or coins) and Razmire's two shops.
 */
public final class ShadesService {
    private static final Map<UUID, Double> SANCTITY = new HashMap<>();
    private static final Map<UUID, Double> RESOURCES = new HashMap<>();

    private ShadesService() {
    }

    static void register() {
        BlockHooks.register("funeral_pyre", (player, pos, state) -> pyre(player, pos));
        BlockHooks.register("flamtaer_altar", (player, pos, state) -> sanctify(player));
        BlockHooks.register("flamtaer_temple", (player, pos, state) -> repair(player));
        BlockHooks.register("razmire_general_store", (player, pos, state) -> buy(player, ShadesOfMortton.OLIVE_OIL_PRICE,
                "olive oil(4)", new String[]{"olive_oil"}, new int[]{1}));
        BlockHooks.register("razmire_builders_merchant", (player, pos, state) -> buy(player, ShadesOfMortton.REPAIR_SET_PRICE,
                "set of temple repair supplies", new String[]{"timber_beam", "limestone_brick", "swamp_paste"},
                new int[]{1, 1, ShadesOfMortton.PASTE_PER_SET}));
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (world.isClient() || !(player instanceof ServerPlayerEntity sp) || stack.isEmpty()
                    || !"sacred_oil".equals(Inv.idOf(stack))) {
                return TypedActionResult.pass(stack);
            }
            startPyreLogs(sp);
            return TypedActionResult.success(stack);
        });
        GameEvents.onKill((player, entity, stats) -> {
            if (Shade.LOAR.creature().equals(MinigameManager.creatureId(entity))) {
                addSanctity(player, ShadesOfMortton.SANCTITY_PER_KILL);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 1200 == 0) {
                SANCTITY.replaceAll((u, v) -> Math.max(0, v - ShadesOfMortton.SANCTITY_DRAIN_PER_MINUTE));
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            SANCTITY.remove(handler.getPlayer().getUuid());
            RESOURCES.remove(handler.getPlayer().getUuid());
        });
    }

    static boolean questDone(ServerPlayerEntity player) {
        return ContentRegistry.quest(ShadesOfMortton.QUEST) == null || QuestApi.isComplete(player, ShadesOfMortton.QUEST);
    }

    private static boolean gate(ServerPlayerEntity player) {
        if (!questDone(player)) {
            Msg.game(player, "You need to complete the Shades of Mort'ton quest first.");
            return false;
        }
        return true;
    }

    private static double sanctity(ServerPlayerEntity p) {
        return SANCTITY.getOrDefault(p.getUuid(), 0.0);
    }

    private static void addSanctity(ServerPlayerEntity p, double amount) {
        SANCTITY.put(p.getUuid(), Math.max(0, Math.min(100, sanctity(p) + amount)));
    }

    private static String pct(double v) {
        return String.format("%.1f%%", v);
    }

    // ------------------------------------------------------------------ doses (potion-style: stack damage = doses used)

    private static int doses(ServerPlayerEntity p, String id) {
        int total = 0;
        for (ItemStack s : p.getInventory().main) {
            if (!s.isEmpty() && id.equals(Inv.idOf(s))) {
                total += Math.max(0, 4 - s.getDamage());
            }
        }
        return total;
    }

    private static void takeDoses(ServerPlayerEntity p, String id, int n) {
        var main = p.getInventory().main;
        for (int i = 0; i < main.size() && n > 0; i++) {
            ItemStack s = main.get(i);
            if (s.isEmpty() || !id.equals(Inv.idOf(s))) {
                continue;
            }
            int left = Math.max(0, 4 - s.getDamage());
            int use = Math.min(left, n);
            n -= use;
            if (use == left) {
                main.set(i, Inv.stack("vial", 1));
            } else {
                s.setDamage(s.getDamage() + use);
            }
        }
    }

    // ------------------------------------------------------------------ sacred fire

    private static void sanctify(ServerPlayerEntity player) {
        if (!gate(player)) {
            return;
        }
        if (sanctity(player) < ShadesOfMortton.ALTAR_MIN_SANCTITY) {
            Msg.game(player, "You need at least " + pct(ShadesOfMortton.ALTAR_MIN_SANCTITY) + " sanctity to use the sacred fire. "
                    + "Repair the temple or slay the shades around it. (Sanctity: " + pct(sanctity(player)) + ")");
            return;
        }
        var main = player.getInventory().main;
        int done = 0;
        for (int i = 0; i < main.size(); i++) {
            ItemStack s = main.get(i);
            if (s.isEmpty() || !"olive_oil".equals(Inv.idOf(s))) {
                continue;
            }
            int d = Math.max(0, 4 - s.getDamage());
            double cost = d * ShadesOfMortton.SANCTITY_PER_DOSE;
            if (sanctity(player) < cost) {
                break;
            }
            addSanctity(player, -cost);
            ItemStack oil = Inv.stack("sacred_oil", 1);
            oil.setDamage(s.getDamage());
            main.set(i, oil);
            done++;
        }
        if (done == 0) {
            Msg.game(player, "You have no olive oil to sanctify.");
            return;
        }
        CollectionLog.obtain(player, "sacred_oil");
        Msg.game(player, "You sanctify " + done + " vial" + (done == 1 ? "" : "s") + " of olive oil in the sacred fire. (Sanctity: "
                + pct(sanctity(player)) + ")");
    }

    // ------------------------------------------------------------------ temple repairs

    private static void repair(ServerPlayerEntity player) {
        if (!gate(player)) {
            return;
        }
        boolean flamtaer = Inv.has(player, "flamtaer_hammer", 1);
        if (!flamtaer && !Inv.has(player, "hammer", 1)) {
            Msg.game(player, "You need a hammer to repair the temple.");
            return;
        }
        if (!stock(player)) {
            Msg.game(player, "You need a timber beam (or plank), a limestone brick and " + ShadesOfMortton.PASTE_PER_SET
                    + " swamp paste to repair the temple. Razmire sells them.");
            return;
        }
        int every = flamtaer ? ShadesOfMortton.REPAIR_TICKS_FLAMTAER : ShadesOfMortton.REPAIR_TICKS;
        Msg.action(player, "You start repairing the temple walls.");
        ActionManager.start(player, (p, tick) -> {
            if (tick % every != 0) {
                return true;
            }
            if (!stock(p)) {
                Msg.game(p, "You have run out of building materials.");
                return false;
            }
            RESOURCES.put(p.getUuid(), RESOURCES.getOrDefault(p.getUuid(), 0.0) - ShadesOfMortton.RESOURCES_PER_REPAIR);
            addSanctity(p, ShadesOfMortton.SANCTITY_PER_REPAIR);
            XpService.add(p, Skill.CRAFTING, ShadesOfMortton.REPAIR_CRAFTING_XP);
            Msg.action(p, "Resources: " + pct(RESOURCES.getOrDefault(p.getUuid(), 0.0)) + "  Sanctity: " + pct(sanctity(p)));
            return true;
        });
    }

    /** Makes sure at least 1% resources are on hand, using a set of materials if needed. */
    private static boolean stock(ServerPlayerEntity p) {
        if (RESOURCES.getOrDefault(p.getUuid(), 0.0) >= ShadesOfMortton.RESOURCES_PER_REPAIR) {
            return true;
        }
        String wood = Inv.has(p, "timber_beam", 1) ? "timber_beam" : Inv.has(p, "plank", 1) ? "plank" : null;
        if (wood == null || !Inv.has(p, "limestone_brick", 1) || !Inv.has(p, "swamp_paste", ShadesOfMortton.PASTE_PER_SET)) {
            return false;
        }
        Inv.remove(p, wood, 1);
        Inv.remove(p, "limestone_brick", 1);
        Inv.remove(p, "swamp_paste", ShadesOfMortton.PASTE_PER_SET);
        RESOURCES.put(p.getUuid(), RESOURCES.getOrDefault(p.getUuid(), 0.0) + ShadesOfMortton.RESOURCES_PER_SET);
        return true;
    }

    // ------------------------------------------------------------------ pyre logs

    /** The best pyre log the player has the logs and the sacred oil for. */
    private static PyreLog makeable(ServerPlayerEntity p) {
        int oil = doses(p, "sacred_oil");
        PyreLog best = null;
        for (PyreLog log : PyreLog.values()) {
            if (oil >= log.doses() && Inv.has(p, log.log(), 1)) {
                best = log;
            }
        }
        return best;
    }

    private static void startPyreLogs(ServerPlayerEntity player) {
        if (!questDone(player)) {
            Msg.game(player, "This sacred oil doesn't seem fresh. You should make your own.");
            return;
        }
        if (makeable(player) == null) {
            Msg.game(player, "You need logs and enough sacred oil (2 doses for normal and oak logs, 3 for willow to mahogany, "
                    + "4 for yew and better) to make pyre logs.");
            return;
        }
        ActionManager.start(player, (p, tick) -> {
            PyreLog log = makeable(p);
            if (log == null) {
                return false;
            }
            takeDoses(p, "sacred_oil", log.doses());
            Inv.remove(p, log.log(), 1);
            Inv.give(p, log.id(), 1);
            XpService.add(p, Skill.FIREMAKING, log.makeXp());
            Msg.action(p, "You coat the logs with sacred oil.");
            return true;
        });
    }

    // ------------------------------------------------------------------ funeral pyre

    private static void pyre(ServerPlayerEntity player, BlockPos pos) {
        if (!gate(player)) {
            return;
        }
        if (!Inv.has(player, "tinderbox", 1)) {
            Msg.game(player, "You need a tinderbox to light the funeral pyre.");
            return;
        }
        int fm = Players.data(player).skills().currentLevel(Skill.FIREMAKING);
        PyreLog[] logs = PyreLog.values();
        Shade[] shades = Shade.values();
        boolean anyRemains = false;
        boolean anyLogs = false;
        for (int l = logs.length - 1; l >= 0; l--) {
            PyreLog log = logs[l];
            if (!Inv.has(player, log.id(), 1)) {
                continue;
            }
            anyLogs = true;
            if (fm < log.level()) {
                continue;
            }
            for (int s = shades.length - 1; s >= 0; s--) {
                Shade shade = shades[s];
                if (!Inv.has(player, shade.remains(), 1)) {
                    continue;
                }
                anyRemains = true;
                if (log.burns(shade)) {
                    cremate(player, pos, log, shade);
                    return;
                }
            }
        }
        for (Shade shade : shades) {
            anyRemains |= Inv.has(player, shade.remains(), 1);
        }
        if (!anyLogs) {
            Msg.game(player, "You need pyre logs (logs coated with sacred oil) to build a funeral pyre.");
        } else if (!anyRemains) {
            Msg.game(player, "You have no shade remains to cremate.");
        } else {
            Msg.game(player, "Your pyre logs can't burn those remains, or you need a higher Firemaking level to light them.");
        }
    }

    private static void cremate(ServerPlayerEntity player, BlockPos pos, PyreLog log, Shade shade) {
        Inv.remove(player, log.id(), 1);
        Inv.remove(player, shade.remains(), 1);
        XpService.add(player, Skill.FIREMAKING, log.firemakingXp());
        XpService.add(player, Skill.PRAYER, log.prayerXp(shade));
        ServerWorld world = (ServerWorld) player.getWorld();
        world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 30, 0.3, 0.8, 0.3, 0.03);
        world.spawnParticles(ParticleTypes.SOUL, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 6, 0.2, 0.6, 0.2, 0.05);
        ShadesOfMortton.Cremation c = ShadesOfMortton.cremate(shade, player.getRandom()::nextDouble);
        if (c.key() != null) {
            Inv.give(player, c.key(), 1);
            CollectionLog.obtain(player, c.key());
            Msg.game(player, "The " + shade.displayName() + " shade's spirit rises from the pyre and leaves a key behind.");
        } else {
            Inv.give(player, Inv.COINS, c.coins());
            Msg.game(player, "The " + shade.displayName() + " shade's spirit rises from the pyre and leaves " + c.coins() + " coins.");
        }
    }

    // ------------------------------------------------------------------ Razmire

    private static void buy(ServerPlayerEntity player, int price, String what, String[] items, int[] counts) {
        if (!Inv.has(player, Inv.COINS, price)) {
            Msg.game(player, "Razmire: That'll be " + price + " coins for a " + what + ".");
            return;
        }
        Inv.remove(player, Inv.COINS, price);
        for (int i = 0; i < items.length; i++) {
            Inv.give(player, items[i], counts[i]);
        }
        Msg.game(player, "You buy a " + what + " for " + price + " coins.");
    }
}
