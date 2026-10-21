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
import com.runecraft.platform.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
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
            } else if (key.equals("talk:mg_pirate_jackie")) {
                // Pirate Jackie the Fruit runs the Agility Arena's shop and is also the Karamja Diary's taskmaster.
                p.displayClientMessage(net.minecraft.network.chat.Component.literal("Pirate Jackie the Fruit: Arr, and how be yer Karamja diary? ")
                        .append(net.minecraft.network.chat.Component.literal("[Karamja Diary]").withStyle(s -> s
                                .withColor(net.minecraft.ChatFormatting.GOLD)
                                .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                                        net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/rc diary karamja")))), false);
            }
        });
        // Round 40: the diary taskmasters, the Falador water pump and the Rellekka well.
        for (DiaryExtras.Taskmaster t : DiaryExtras.TASKMASTERS) {
            if (!t.npcId().startsWith("mg_")) {
                com.runecraft.quest.QuestScripts.onTalk(t.npcId(), (p, npc) -> taskmaster(p, t));
            }
        }
        BlockHooks.register(DiaryExtras.WATER_PUMP, (p, pos, state) -> fillWater(p, "pump"));
        BlockHooks.register(DiaryExtras.WELL, (p, pos, state) -> fillWater(p, "well"));
        LandmarkBuilder.registerCustom(DiaryExtras.WATER_PUMP, (world, centre) -> place(world, centre, DiaryExtras.WATER_PUMP, false));
        LandmarkBuilder.registerCustom(DiaryExtras.WELL, (world, centre) -> place(world, centre, DiaryExtras.WELL, false));
        GameEvents.onKill((p, entity, stats) -> {
            if (stats != null && stats.profile() != null && "ent".equals(stats.profile().id)) {
                fallTrunk(p, (ServerLevel) entity.level(), entity.blockPosition());
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0 || TRUNKS.isEmpty()) {
                return;
            }
            long now = ActionManager.gameTick();
            ServerLevel world = Gielinor.world(server);
            Iterator<Map.Entry<BlockPos, Trunk>> it = TRUNKS.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<BlockPos, Trunk> e = it.next();
                if (e.getValue().expires() <= now) {
                    it.remove();
                    if (world != null && isBlock(world.getBlockState(e.getKey()), DiaryExtras.ENT_TRUNK)) {
                        world.setBlockAndUpdate(e.getKey(), Blocks.AIR.defaultBlockState());
                    }
                }
            }
        });
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.get(RuneCraft.id(id));
    }

    static boolean isBlock(BlockState state, String id) {
        Block b = block(id);
        return b != Blocks.AIR && state.is(b);
    }

    /** Puts a block on the ground at a place; {@code indoors} uses the first floor above sea level (a hall, not the roof). */
    static void place(ServerLevel world, BlockPos centre, String id, boolean indoors) {
        Block b = block(id);
        if (b == Blocks.AIR) {
            return;
        }
        int x = centre.getX();
        int z = centre.getZ();
        com.runecraft.world.StructurePlacer.applyIfLoaded(world, x >> 4, z >> 4);
        int top = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int y = top;
        if (indoors) {
            for (int yy = world.getSeaLevel(); yy < top; yy++) {
                BlockPos at = new BlockPos(x, yy, z);
                if (world.getBlockState(at).isAir() && world.getBlockState(at.below()).isRedstoneConductor(world, at.below())) {
                    y = yy;
                    break;
                }
            }
        }
        BlockPos at = new BlockPos(x, y, z);
        if (!world.getBlockState(at).isAir() || !world.getBlockState(at.below()).isRedstoneConductor(world, at.below())) {
            RuneCraft.LOGGER.info("No room for {} at {}", id, at);
            return;
        }
        world.setBlock(at, b.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    private static boolean questDone(ServerPlayer p, String questId) {
        QuestInfo q = ContentRegistry.quest(questId);
        return q == null || Players.data(p).questComplete(q);
    }

    // ---------------- Taskmasters and water sources (round 40) ----------------

    /** A diary taskmaster: shows the area's diary at its first unclaimed tier (rewards are claimed there). */
    static void taskmaster(ServerPlayer p, DiaryExtras.Taskmaster t) {
        com.runecraft.osrs.content.systems.Diaries.Area a = com.runecraft.osrs.content.systems.Diaries.area(t.area());
        SysState.SysPlayer sp = SysState.of(p);
        com.runecraft.osrs.content.systems.Diaries.Tier show = com.runecraft.osrs.content.systems.Diaries.Tier.ELITE;
        for (com.runecraft.osrs.content.systems.Diaries.Tier tier : com.runecraft.osrs.content.systems.Diaries.Tier.values()) {
            if (!sp.diaryClaimed.contains(a.id() + ":" + tier.name())) {
                show = tier;
                break;
            }
        }
        QuestApi.say(p, t.name(), DiaryService.claimable(sp, a, show)
                ? "Well done! You've finished the " + show.id() + " " + a.name() + " tasks. Here, take your reward."
                : "How's your " + a.name() + " Achievement Diary coming along?");
        SysMenus.diary(p, a.id(), show);
    }

    /** A pump or well: fills the held bucket, jug, bowl or vial with water. */
    static void fillWater(ServerPlayer p, String what) {
        ItemStack held = p.getMainHandItem();
        String full = held.isEmpty() ? null : DiaryExtras.waterFill(Inv.idOf(held));
        if (full == null) {
            Msg.game(p, "Use a bucket, jug, bowl or vial on the " + what + " to fill it with water.");
            return;
        }
        held.shrink(1);
        Inv.give(p, full, 1);
        p.level().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.BUCKET_FILL,
                net.minecraft.sounds.SoundSource.PLAYERS, 1f, 1f);
        Msg.action(p, "You fill it with water from the " + what + ".");
        GameEvents.fireActivity(p, "fill:" + (full.startsWith("minecraft:") ? full.substring("minecraft:".length()) : full));
    }

    // ---------------- Saltpetre ----------------

    private static void dig(ServerPlayer p, BlockPos pos) {
        if (!Inv.has(p, "spade", 1)) {
            Msg.game(p, "You need a spade to dig up the saltpetre.");
            return;
        }
        Msg.game(p, "You dig into the saltpetre deposit.");
        ActionManager.start(p, new Action() {
            int counter;

            @Override
            public boolean tick(ServerPlayer player, long gameTick) {
                if (!isBlock(player.level().getBlockState(pos), DiaryExtras.SALTPETRE_DEPOSIT) || !Inv.has(player, "spade", 1)) {
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

    private static void totem(ServerPlayer p) {
        if (!questDone(p, "legends_quest")) {
            Msg.game(p, "A sculpted trunk of wood. Nothing happens.");
            return;
        }
        int charged = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            Jewellery.Worn w = s.isEmpty() ? null : Jewellery.worn(Inv.idOf(s));
            if (w == null || w.chargeBase() == null || !DiaryExtras.TOTEM_RECHARGES.contains(w.chargeBase())) {
                continue;
            }
            String to = w.chargeBase() + "_" + DiaryExtras.TOTEM_CHARGES;
            Jewellery.Worn target = Jewellery.worn(to);
            if (target == null || w.charges() >= target.charges()) {
                continue;
            }
            p.getInventory().setItem(i, Inv.stack(to, s.getCount()));
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

    private static void sawmill(ServerPlayer p) {
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

    private static void fallTrunk(ServerPlayer p, ServerLevel world, BlockPos pos) {
        Block b = block(DiaryExtras.ENT_TRUNK);
        if (b == Blocks.AIR || !world.getBlockState(pos).isAir() || !world.getBlockState(pos.below()).isRedstoneConductor(world, pos.below())) {
            return;
        }
        world.setBlock(pos, b.defaultBlockState(), Block.UPDATE_CLIENTS);
        TRUNKS.put(pos.immutable(), new Trunk(p.getUUID(), ActionManager.gameTick() + DiaryExtras.ENT_TRUNK_TICKS,
                Gielinor.wildernessLevel(p) > 0));
        Msg.game(p, "The Ent's trunk falls to the ground.");
    }

    private static void chop(ServerPlayer p, BlockPos pos) {
        Trunk trunk = TRUNKS.get(pos);
        if (trunk == null || !trunk.owner().equals(p.getUUID())) {
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
            public boolean tick(ServerPlayer player, long gameTick) {
                if (!TRUNKS.containsKey(pos) || !isBlock(player.level().getBlockState(pos), DiaryExtras.ENT_TRUNK)
                        || player.blockPosition().distSqr(pos) > 16) {
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
