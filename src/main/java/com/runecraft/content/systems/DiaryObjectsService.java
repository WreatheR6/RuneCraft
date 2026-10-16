package com.runecraft.content.systems;

import com.runecraft.RuneCraft;
import com.runecraft.action.Action;
import com.runecraft.action.ActionManager;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.GameEvents;
import com.runecraft.osrs.QuestInfo;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.systems.DiaryExtras;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.skill.Tools;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import com.runecraft.world.LandmarkBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Diary objects added in round 23: the Hosidius saltpetre deposits (dig with a spade), the Legends' Guild totem pole
 * (recharges skills necklaces and combat bracelets to 4 after Legends' Quest), the Varrock sawmill operator (converts every
 * log of one kind in one go) and the trunk a killed Ent leaves for 101 ticks (chopped by its killer for logs). Also puts the
 * muddy and crystal chests on their tiles (their places build nothing by themselves).
 */
final class DiaryObjectsService {
    private DiaryObjectsService() {
    }

    /** A fallen Ent trunk: who may chop it and the game tick it disappears. */
    private record Trunk(UUID owner, long expires, boolean wilderness) {
    }

    private static final Map<BlockPos, Trunk> TRUNKS = new HashMap<>();

    static void register() {
        BlockHooks.register(DiaryExtras.SALTPETRE_DEPOSIT, (p, pos, state) -> dig(p, pos));
        BlockHooks.register(DiaryExtras.LEGENDS_TOTEM, (p, pos, state) -> totem(p));
        BlockHooks.register(DiaryExtras.ENT_TRUNK, (p, pos, state) -> chop(p, pos));
        for (int i = 0; i < DiaryExtras.SALTPETRE_TILES.length; i++) {
            LandmarkBuilder.registerCustom(DiaryExtras.SALTPETRE_DEPOSIT + "_" + (i + 1),
                    (world, centre) -> place(world, centre, DiaryExtras.SALTPETRE_DEPOSIT, false));
        }
        LandmarkBuilder.registerCustom(DiaryExtras.LEGENDS_TOTEM, (world, centre) -> place(world, centre, DiaryExtras.LEGENDS_TOTEM, true));
        LandmarkBuilder.registerCustom(DiaryExtras.MUDDY_CHEST, (world, centre) -> place(world, centre, DiaryExtras.MUDDY_CHEST, false));
        LandmarkBuilder.registerCustom(DiaryExtras.CRYSTAL_CHEST, (world, centre) -> place(world, centre, DiaryExtras.CRYSTAL_CHEST, true));
        GameEvents.onActivity((p, key) -> {
            if (key.equals("talk:" + DiaryExtras.SAWMILL_OPERATOR)) {
                sawmill(p);
            }
        });
        GameEvents.onKill((p, entity, stats) -> {
            if (stats != null && stats.profile() != null && "ent".equals(stats.profile().id)) {
                fallTrunk(p, (ServerWorld) entity.getWorld(), entity.getBlockPos());
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 20 != 0 || TRUNKS.isEmpty()) {
                return;
            }
            long now = ActionManager.gameTick();
            ServerWorld world = Gielinor.world(server);
            Iterator<Map.Entry<BlockPos, Trunk>> it = TRUNKS.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<BlockPos, Trunk> e = it.next();
                if (e.getValue().expires() <= now) {
                    it.remove();
                    if (world != null && isBlock(world.getBlockState(e.getKey()), DiaryExtras.ENT_TRUNK)) {
                        world.setBlockState(e.getKey(), Blocks.AIR.getDefaultState());
                    }
                }
            }
        });
    }

    private static Block block(String id) {
        return Registries.BLOCK.get(RuneCraft.id(id));
    }

    static boolean isBlock(BlockState state, String id) {
        Block b = block(id);
        return b != Blocks.AIR && state.isOf(b);
    }

    /** Puts a block on the ground at a place; {@code indoors} uses the first floor above sea level (a hall, not the roof). */
    static void place(ServerWorld world, BlockPos centre, String id, boolean indoors) {
        Block b = block(id);
        if (b == Blocks.AIR) {
            return;
        }
        int x = centre.getX();
        int z = centre.getZ();
        com.runecraft.world.StructurePlacer.applyIfLoaded(world, x >> 4, z >> 4);
        int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        int y = top;
        if (indoors) {
            for (int yy = world.getSeaLevel(); yy < top; yy++) {
                BlockPos at = new BlockPos(x, yy, z);
                if (world.getBlockState(at).isAir() && world.getBlockState(at.down()).isSolidBlock(world, at.down())) {
                    y = yy;
                    break;
                }
            }
        }
        BlockPos at = new BlockPos(x, y, z);
        if (!world.getBlockState(at).isAir() || !world.getBlockState(at.down()).isSolidBlock(world, at.down())) {
            RuneCraft.LOGGER.info("No room for {} at {}", id, at);
            return;
        }
        world.setBlockState(at, b.getDefaultState(), Block.NOTIFY_LISTENERS);
    }

    private static boolean questDone(ServerPlayerEntity p, String questId) {
        QuestInfo q = ContentRegistry.quest(questId);
        return q == null || Players.data(p).questComplete(q);
    }

    // ---------------- Saltpetre ----------------

    private static void dig(ServerPlayerEntity p, BlockPos pos) {
        if (!Inv.has(p, "spade", 1)) {
            Msg.game(p, "You need a spade to dig up the saltpetre.");
            return;
        }
        Msg.game(p, "You dig into the saltpetre deposit.");
        ActionManager.start(p, new Action() {
            int counter;

            @Override
            public boolean tick(ServerPlayerEntity player, long gameTick) {
                if (!isBlock(player.getWorld().getBlockState(pos), DiaryExtras.SALTPETRE_DEPOSIT) || !Inv.has(player, "spade", 1)) {
                    return false;
                }
                if (++counter < 3) { // approx: a dig every 3 ticks
                    return true;
                }
                counter = 0;
                if (!Inv.hasRoomFor(player, "saltpetre")) {
                    Msg.game(player, "Your inventory is too full to hold any more saltpetre.");
                    return false;
                }
                int n = DiaryExtras.saltpetreDug(Players.data(player).skills().currentLevel(Skill.MINING), player.getRandom().nextDouble());
                Inv.give(player, "saltpetre", n);
                GameEvents.fireObtain(player, "saltpetre", n);
                GameEvents.fireActivity(player, "dig:saltpetre");
                Msg.game(player, n == 2 ? "You dig up two lumps of saltpetre." : "You dig up some saltpetre.");
                return true;
            }
        });
    }

    // ---------------- Totem pole ----------------

    private static void totem(ServerPlayerEntity p) {
        if (!questDone(p, "legends_quest")) {
            Msg.game(p, "A sculpted trunk of wood. Nothing happens.");
            return;
        }
        int charged = 0;
        for (int i = 0; i < p.getInventory().size(); i++) {
            ItemStack s = p.getInventory().getStack(i);
            Jewellery.Worn w = s.isEmpty() ? null : Jewellery.worn(Inv.idOf(s));
            if (w == null || w.chargeBase() == null || !DiaryExtras.TOTEM_RECHARGES.contains(w.chargeBase())) {
                continue;
            }
            String to = w.chargeBase() + "_" + DiaryExtras.TOTEM_CHARGES;
            Jewellery.Worn target = Jewellery.worn(to);
            if (target == null || w.charges() >= target.charges()) {
                continue;
            }
            p.getInventory().setStack(i, Inv.stack(to, s.getCount()));
            charged += s.getCount();
        }
        if (charged == 0) {
            Msg.game(p, "The totem pole can recharge skills necklaces and combat bracelets.");
            return;
        }
        Msg.action(p, "You touch the jewellery against the totem pole. It recharges your jewellery.");
        GameEvents.fireActivity(p, "totem_recharge");
    }

    // ---------------- Sawmill operator ----------------

    private static void sawmill(ServerPlayerEntity p) {
        for (DiaryExtras.Sawmill s : DiaryExtras.SAWMILL) {
            int logs = Inv.count(p, s.logs());
            if (logs <= 0) {
                continue;
            }
            int n = DiaryExtras.sawmillBatch(logs, Inv.count(p, "coins"), s.fee());
            if (n <= 0) {
                QuestApi.say(p, "Sawmill operator", "That'll be " + s.fee() + " coins a plank, and you can't afford even one.");
                return;
            }
            Inv.remove(p, s.logs(), n);
            Inv.remove(p, "coins", n * s.fee());
            Inv.give(p, s.plank(), n);
            GameEvents.fireObtain(p, s.plank(), n);
            QuestApi.say(p, "Sawmill operator", "There you go: " + n + " plank" + (n == 1 ? "" : "s") + " for " + n * s.fee() + " coins.");
            return;
        }
        QuestApi.say(p, "Sawmill operator", "Bring me logs and I'll turn them into planks: 100 coins for normal, 250 for oak, "
                + "500 for teak and 1,500 for mahogany.");
    }

    // ---------------- Ent trunks ----------------

    private static void fallTrunk(ServerPlayerEntity p, ServerWorld world, BlockPos pos) {
        Block b = block(DiaryExtras.ENT_TRUNK);
        if (b == Blocks.AIR || !world.getBlockState(pos).isAir() || !world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) {
            return;
        }
        world.setBlockState(pos, b.getDefaultState(), Block.NOTIFY_LISTENERS);
        TRUNKS.put(pos.toImmutable(), new Trunk(p.getUuid(), ActionManager.gameTick() + DiaryExtras.ENT_TRUNK_TICKS,
                Gielinor.wildernessLevel(p) > 0));
        Msg.game(p, "The Ent's trunk falls to the ground.");
    }

    private static void chop(ServerPlayerEntity p, BlockPos pos) {
        Trunk trunk = TRUNKS.get(pos);
        if (trunk == null || !trunk.owner().equals(p.getUuid())) {
            Msg.game(p, "This is not your Ent to chop.");
            return;
        }
        if (Tools.bestAxeTool(p, Players.data(p).skills()) == null) {
            Msg.game(p, "You need an axe to chop this trunk.");
            return;
        }
        Msg.game(p, "You swing your axe at the trunk.");
        ActionManager.start(p, new Action() {
            int counter;

            @Override
            public boolean tick(ServerPlayerEntity player, long gameTick) {
                if (!TRUNKS.containsKey(pos) || !isBlock(player.getWorld().getBlockState(pos), DiaryExtras.ENT_TRUNK)
                        || player.getBlockPos().getSquaredDistance(pos) > 16) {
                    return false;
                }
                if (++counter < DiaryExtras.ENT_CHOP_TICKS) {
                    return true;
                }
                counter = 0;
                int current = Players.data(player).skills().currentLevel(Skill.WOODCUTTING);
                if (player.getRandom().nextDouble() >= DiaryExtras.entChopChance(current, false)) {
                    return true;
                }
                String log = DiaryExtras.entLog(Players.data(player).skills().level(Skill.WOODCUTTING), player.getRandom().nextDouble());
                int n = DiaryExtras.entLogsPerChop(trunk.wilderness());
                ItemStack noted = com.runecraft.bank.Notes.note(log, null, n);
                if (noted.isEmpty()) {
                    Inv.give(player, log, n);
                } else {
                    Inv.give(player, noted);
                }
                XpService.add(player, Skill.WOODCUTTING, DiaryExtras.ENT_XP);
                GameEvents.fireObtain(player, log, n);
                GameEvents.fireActivity(player, "ent_chop:" + log);
                return true;
            }
        });
    }
}
