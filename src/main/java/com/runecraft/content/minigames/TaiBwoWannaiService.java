package com.runecraft.content.minigames;

import com.runecraft.content.BlockHooks;
import com.runecraft.content.GameEvents;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.TaiBwoWannai;
import com.runecraft.osrs.content.minigames.TaiBwoWannai.Event;
import com.runecraft.osrs.content.minigames.TaiBwoWannai.Jungle;
import com.runecraft.osrs.content.minigames.TaiBwoWannai.Spar;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.quest.QuestScripts;
import com.runecraft.registry.ModEntities;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.LandmarkBuilder;
import com.runecraft.platform.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Tai Bwo Wannai Cleanup (see {@link TaiBwoWannai}): hacking jungle, repairing the village fence,
 * events while hacking, favour from killing jungle spiders and event monsters, trading favour for
 * sticks with the villagers (Murcaily first) and Gabooty's Cooperative. Favour, the fence state and
 * the villagers' cooldowns are kept per player in {@link MinigameData}.
 */
final class TaiBwoWannaiService {
    private static final String FAVOUR = "tbw_favour";
    private static final String FENCE_LEVEL = "tbw_fence_level";
    private static final String FENCE_AT = "tbw_fence_minute";
    private static final String STARTED = "tbw_started";
    private static final String REPAIRED = "tbw_fence_repaired";
    private static final String FIRST_STICKS = "tbw_first_sticks";
    private static final String EVENT_TAG = "rc_tbw_event";
    private static boolean wrapped;

    private TaiBwoWannaiService() {
    }

    static void register() {
        for (Jungle j : Jungle.values()) {
            BlockHooks.register(j.block, (player, pos, state) -> hack(player, j));
        }
        BlockHooks.register(TaiBwoWannai.FENCE, (player, pos, state) -> repair(player));
        for (String npc : TaiBwoWannai.SUPPLIERS) {
            if (!npc.equals("murcaily")) {
                QuestScripts.onTalk(npc, (player, entity) -> villager(player, npc, entity));
            }
        }
        ServerLifecycleEvents.SERVER_STARTED.register(server -> wrapMurcaily());
        GameEvents.onKill((player, entity, stats) -> onKill(player, entity));
        LandmarkBuilder.registerCustom(TaiBwoWannai.PLACE, TaiBwoWannaiService::build);
    }

    private static void wrapMurcaily() {
        if (wrapped) {
            return;
        }
        wrapped = true;
        BiConsumer<ServerPlayer, QuestNpcEntity> quest = QuestScripts.handler("murcaily");
        QuestScripts.onTalk("murcaily", (player, npc) -> {
            int stage = QuestApi.stage(player, "my_arms_big_adventure");
            boolean maba = stage > 0 && !QuestApi.isComplete(player, "my_arms_big_adventure");
            if (maba && quest != null) {
                quest.accept(player, npc);
                return;
            }
            murcaily(player);
        });
    }

    private static MinigameData data(ServerPlayer p) {
        return MinigameData.of(p);
    }

    static int favour(ServerPlayer p) {
        return data(p).stat(p.getUUID(), FAVOUR);
    }

    private static String percent(int tenths) {
        return tenths / 10 + "." + tenths % 10 + "%";
    }

    private static boolean unlocked(ServerPlayer p) {
        if (!QuestApi.isComplete(p, TaiBwoWannai.QUEST)) {
            Msg.game(p, "You need to complete Jungle Potion first.");
            return false;
        }
        if (!data(p).flag(p.getUUID(), STARTED)) {
            Msg.game(p, "You should speak to Murcaily by the Hardwood Grove first.");
            return false;
        }
        return true;
    }

    static void addFavour(ServerPlayer p, int tenths) {
        int before = favour(p);
        int after = TaiBwoWannai.addFavour(before, tenths);
        if (after == before) {
            return;
        }
        data(p).setStat(p.getUUID(), FAVOUR, after);
        Msg.game(p, "Tai Bwo Wannai favour: " + percent(after) + ".");
        if (after >= TaiBwoWannai.MAX_FAVOUR) {
            GameEvents.fireActivity(p, "tbw_favour:100");
        }
    }

    /** The best machete carried or wielded (0 = plain machete), or -1 for none. */
    private static int macheteTier(ServerPlayer p) {
        int best = -1;
        String wielded = Inv.idOf(p.getMainHandItem());
        for (int i = 0; i < TaiBwoWannai.MACHETES.size(); i++) {
            String m = TaiBwoWannai.MACHETES.get(i);
            if (Inv.has(p, m, 1) || m.equals(wielded)) {
                best = i;
            }
        }
        return best;
    }

    private static int level(ServerPlayer p, Skill s) {
        return Players.data(p).skills().currentLevel(s);
    }

    private static final java.util.Map<UUID, Long> LAST_HACK = new java.util.HashMap<>();

    static void hack(ServerPlayer p, Jungle j) {
        if (!unlocked(p)) {
            return;
        }
        int tier = macheteTier(p);
        if (tier < 0) {
            Msg.game(p, "You need a machete to hack through the jungle.");
            return;
        }
        int wc = level(p, Skill.WOODCUTTING);
        if (wc < j.level) {
            Msg.game(p, "You need a Woodcutting level of " + j.level + " to hack " + j.name.toLowerCase(java.util.Locale.ROOT) + ".");
            return;
        }
        long now = p.getServer().getTickCount();
        Long last = LAST_HACK.get(p.getUUID());
        if (last != null && now - last < 12) { // one swing per 0.6 s tick
            return;
        }
        LAST_HACK.put(p.getUUID(), now);
        if (p.getRandom().nextDouble() >= TaiBwoWannai.hackChance(j, wc, tier)) {
            XpService.add(p, Skill.WOODCUTTING, 1); // a failed hack still gives 1 XP
            Msg.game(p, "You swing your machete at the jungle.");
            return;
        }
        XpService.add(p, Skill.WOODCUTTING, j.xp);
        Inv.give(p, j.spar, 1);
        Msg.game(p, "You hack away some of the jungle and find a thatching spar.");
        addFavour(p, j.favour);
        if (p.getRandom().nextDouble() < TaiBwoWannai.EVENT_CHANCE) {
            event(p, wc);
        }
    }

    private static void event(ServerPlayer p, int wc) {
        if (p.getRandom().nextDouble() < TaiBwoWannai.goutTuberChance(wc)) {
            Inv.give(p, "gout_tuber", 1);
            Msg.game(p, "You uncover a gout tuber and dig it up.");
            return;
        }
        Event e = Event.roll(p.getRandom().nextDouble());
        if (e == Event.GEM_ROCK) {
            if (level(p, Skill.MINING) >= 40 && hasPickaxe(p)) {
                String[] gems = {"uncut_opal", "uncut_jade", "uncut_red_topaz", "uncut_sapphire", "uncut_emerald", "uncut_ruby",
                        "uncut_diamond"};
                for (int i = 0; i < 3; i++) {
                    Inv.give(p, gems[p.getRandom().nextInt(gems.length)], 1); // approx: equal gem chances
                }
                Msg.game(p, "You uncover a gem rock and mine three gems from it.");
            } else {
                Msg.game(p, "You uncover a gem rock. You need a pickaxe and 40 Mining to mine it.");
            }
            return;
        }
        if (e.creature != null && spawn(p, e.creature)) {
            Msg.game(p, "A jungle spider leaps out of the undergrowth!");
            return;
        }
        // Creatures this world doesn't have: you fight them off at once and earn their favour. // approx
        String name = e.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ').replace(" 20", "");
        Msg.game(p, "You fight off a " + name + " that leaps out of the jungle.");
        addFavour(p, e.favour);
    }

    private static boolean hasPickaxe(ServerPlayer p) {
        String held = Inv.idOf(p.getMainHandItem());
        if (held.endsWith("pickaxe")) {
            return true;
        }
        for (String t : List.of("bronze", "iron", "steel", "black", "mithril", "adamant", "rune", "dragon", "crystal")) {
            if (Inv.has(p, t + "_pickaxe", 1)) {
                return true;
            }
        }
        return false;
    }

    private static boolean spawn(ServerPlayer p, String creature) {
        EntityType<?> type = ModEntities.typeById(creature);
        if (type == null) {
            return false;
        }
        ServerLevel world = p.serverLevel();
        Entity entity = type.create(world);
        if (!(entity instanceof Mob mob)) {
            return false;
        }
        mob.moveTo(p.getX() + 1.5, p.getY(), p.getZ() + 1.5, p.getYRot(), 0);
        mob.finalizeSpawn(world, world.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        mob.addTag(EVENT_TAG);
        mob.setTarget(p);
        world.addFreshEntity(mob);
        return true;
    }

    private static void onKill(ServerPlayer p, net.minecraft.world.entity.LivingEntity entity) {
        if (!QuestApi.isComplete(p, TaiBwoWannai.QUEST) || !data(p).flag(p.getUUID(), STARTED)) {
            return;
        }
        boolean event = entity.getTags().contains(EVENT_TAG);
        String id = MinigameManager.creatureId(entity);
        if (!id.equals("jungle_spider")) {
            return;
        }
        int x = GielinorMap.toOsrsX(entity.getBlockX());
        int y = GielinorMap.toOsrsY(entity.getBlockZ());
        boolean nearVillage = x >= 2740 && x <= 2900 && y >= 3030 && y <= 3130;
        // Wild jungle spiders count only after your first trading sticks (wiki).
        if (event || nearVillage && data(p).flag(p.getUUID(), FIRST_STICKS)) {
            addFavour(p, 10 + p.getRandom().nextInt(11)); // 1-2%
        }
    }

    private static int minuteNow() {
        return (int) (System.currentTimeMillis() / 60_000L);
    }

    /** The fence's level after decay (one level per {@link TaiBwoWannai#FENCE_DECAY_TICKS} ticks). */
    private static int fenceLevel(ServerPlayer p) {
        MinigameData d = data(p);
        int level = d.stat(p.getUUID(), FENCE_LEVEL);
        int at = d.stat(p.getUUID(), FENCE_AT);
        int decayMinutes = TaiBwoWannai.FENCE_DECAY_TICKS * 6 / 600;
        if (level > 0 && decayMinutes > 0) {
            int lost = (minuteNow() - at) / decayMinutes;
            if (lost > 0) {
                level = Math.max(0, level - lost);
                d.setStat(p.getUUID(), FENCE_LEVEL, level);
                d.setStat(p.getUUID(), FENCE_AT, minuteNow());
            }
        }
        return level;
    }

    static void repair(ServerPlayer p) {
        if (!unlocked(p)) {
            return;
        }
        if (macheteTier(p) < 0) {
            Msg.game(p, "You need a machete to work the thatching spars into the fence.");
            return;
        }
        int level = fenceLevel(p);
        if (level >= TaiBwoWannai.FENCE_MAX) {
            Msg.game(p, "This section of fence is already reinforced.");
            return;
        }
        Spar spar = null;
        for (int i = Spar.values().length - 1; i >= 0; i--) {
            if (Inv.has(p, Spar.values()[i].item, 1)) {
                spar = Spar.values()[i];
                break;
            }
        }
        if (spar == null) {
            Msg.game(p, "You need a thatching spar to repair the fence. Hack some jungle for one.");
            return;
        }
        Inv.remove(p, spar.item, 1);
        XpService.add(p, Skill.WOODCUTTING, spar.xp);
        data(p).setStat(p.getUUID(), FENCE_LEVEL, level + 1);
        data(p).setStat(p.getUUID(), FENCE_AT, minuteNow());
        data(p).setFlag(p.getUUID(), REPAIRED);
        Msg.game(p, level == 0 ? "You repair the rotten fence." : "You reinforce the fence.");
        addFavour(p, spar.favour);
    }

    static void murcaily(ServerPlayer p) {
        if (!QuestApi.isComplete(p, TaiBwoWannai.QUEST)) {
            QuestApi.say(p, "Murcaily", "This jungle grove is no place for outsiders, bwana.");
            return;
        }
        MinigameData d = data(p);
        if (!d.flag(p.getUUID(), STARTED)) {
            d.setFlag(p.getUUID(), STARTED);
            QuestApi.say(p, "Murcaily", "The villagers are coming home, but the jungle has overrun the village. Hack it back with a "
                    + "machete, use the thatching spars to repair the fence, and we will reward your help with trading sticks.");
            return;
        }
        if (!d.flag(p.getUUID(), REPAIRED)) {
            QuestApi.say(p, "Murcaily", "Repair a part of the village fence first, then come back to me for your trading sticks.");
            return;
        }
        if (giveSticks(p, "murcaily", "Murcaily")) {
            d.setFlag(p.getUUID(), FIRST_STICKS);
        }
    }

    static void villager(ServerPlayer p, String npc, QuestNpcEntity entity) {
        String name = entity.getName().getString();
        if (!QuestApi.isComplete(p, TaiBwoWannai.QUEST)) {
            QuestApi.say(p, name, "Welcome to Tai Bwo Wannai, bwana.");
            return;
        }
        MinigameData d = data(p);
        boolean ready = d.flag(p.getUUID(), FIRST_STICKS);
        if (ready && favour(p) > 0 && cooldownLeft(p, npc) <= 0) {
            giveSticks(p, npc, name);
            return;
        }
        if (npc.equals("mg_gabooty")) {
            RewardShopService.open(p, TaiBwoWannai.SHOP);
            return;
        }
        QuestApi.say(p, name, !ready ? "Help Murcaily clean up the village first, bwana."
                : favour(p) <= 0 ? "Do some more work around the village, bwana." : "I have no more sticks for you yet, bwana.");
    }

    private static int cooldownLeft(ServerPlayer p, String npc) {
        int at = data(p).stat(p.getUUID(), "tbw_sticks_" + npc);
        int minutes = TaiBwoWannai.STICK_COOLDOWN_TICKS * 6 / 600;
        return at == 0 ? 0 : at + minutes - minuteNow();
    }

    private static int clothingWorn(ServerPlayer p) {
        int n = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            String id = Inv.idOf(p.getItemBySlot(slot));
            if (id.startsWith("villager_") || id.startsWith("tribal_top_")) {
                n++;
            }
        }
        for (String c : TaiBwoWannai.COLOURS) {
            if (Inv.has(p, "villager_armband_" + c, 1)) {
                n++;
                break;
            }
        }
        return n;
    }

    private static boolean giveSticks(ServerPlayer p, String npc, String name) {
        int left = cooldownLeft(p, npc);
        if (left > 0) {
            QuestApi.say(p, name, "I have no more sticks for you yet. Come back in " + left + " minute" + (left == 1 ? "" : "s") + ".");
            return false;
        }
        int favour = favour(p);
        if (favour <= 0) {
            QuestApi.say(p, name, "Do some more work around the village first, bwana.");
            return false;
        }
        int[] trade = TaiBwoWannai.trade(favour, clothingWorn(p));
        data(p).setStat(p.getUUID(), FAVOUR, favour - trade[1]);
        data(p).setStat(p.getUUID(), "tbw_sticks_" + npc, minuteNow());
        Inv.give(p, TaiBwoWannai.STICKS, trade[0]);
        QuestApi.say(p, name, "Thank you for your work, bwana. Take these " + trade[0] + " trading sticks.");
        Msg.game(p, "Tai Bwo Wannai favour: " + percent(favour - trade[1]) + ".");
        return true;
    }

    /** Places the jungle patches and fence sections east of the village, near Murcaily. */
    private static void build(ServerLevel world, BlockPos centre) {
        Block light = block(Jungle.LIGHT.block);
        Block medium = block(Jungle.MEDIUM.block);
        Block dense = block(Jungle.DENSE.block);
        Block fence = block(TaiBwoWannai.FENCE);
        // Fence sections along the village's east side (approx tiles 2803, 3066-3082).
        if (fence != null) {
            for (int ty = 3066; ty <= 3082; ty += 4) {
                put(world, GielinorMap.toBlockX(2803), GielinorMap.toBlockZ(ty), fence);
            }
        }
        // Jungle: 60% light, 30% medium, 10% dense, scattered between the village and the grove. // approx
        java.util.Random r = new java.util.Random(3084L);
        for (int i = 0; i < 40; i++) {
            int tx = 2805 + r.nextInt(22);
            int ty = 3060 + r.nextInt(40);
            double roll = r.nextDouble();
            Block b = roll < 0.6 ? light : roll < 0.9 ? medium : dense;
            if (b != null) {
                put(world, GielinorMap.toBlockX(tx), GielinorMap.toBlockZ(ty), b);
            }
        }
    }

    private static Block block(String id) {
        Block b = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("runecraft", id));
        return b == net.minecraft.world.level.block.Blocks.AIR ? null : b;
    }

    /** Puts a block on natural ground (grass, dirt or sand) only, never on a building. */
    private static void put(ServerLevel world, int x, int z, Block block) {
        int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos ground = new BlockPos(x, y - 1, z);
        var state = world.getBlockState(ground);
        if (state.is(BlockTags.DIRT) || state.is(BlockTags.SAND)) {
            world.setBlockAndUpdate(ground.above(), block.defaultBlockState());
        }
    }
}
