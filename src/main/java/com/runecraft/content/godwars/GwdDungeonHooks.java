package com.runecraft.content.godwars;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.content.BlockHooks;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.godwars.GodItems;
import com.runecraft.osrs.content.godwars.GodWarsFactions;
import com.runecraft.osrs.content.godwars.GodWarsFactions.God;
import com.runecraft.osrs.content.godwars.GwdDungeon;
import com.runecraft.osrs.content.godwars.GwdDungeon.Altar;
import com.runecraft.osrs.content.godwars.GwdDungeon.Area;
import com.runecraft.osrs.content.godwars.GwdDungeon.Passage;
import com.runecraft.osrs.content.godwars.GwdDungeon.Spot;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.platform.PlayerBlockBreakEvents;
import com.runecraft.platform.ServerTickEvents;
import com.runecraft.platform.UseBlockCallback;
import com.runecraft.quest.QuestApi;
import com.runecraft.registry.ModBlocks;
import com.runecraft.registry.ModEntities;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import com.runecraft.world.StructurePlacer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The God Wars Dungeon's ways in and between its areas ({@link GwdDungeon}): the hole north of Trollheim (Troll
 * Stronghold's Dad beaten, 60 Strength for the boulder or 60 Agility for the little crack, and a rope the first time),
 * the rope back up, every obstacle and door with its OSRS requirement, the altars by the generals (Prayer above the
 * maximum per god item worn, every 10 minutes and out of combat; sneak-use, even in combat or holding something, to
 * teleport out of the chamber), the ancient barrier before Nex
 * (she appears when someone passes it while nobody is fighting her). Also: God Wars followers leave alone players who
 * wear an item of their god, and leaving the dungeon clears the essence.
 */
final class GwdDungeonHooks {
    private static final String[] STRANGE_VOICE = {"Give me the power of shadow...", "You cannot imprison me forever...",
            "Face me and know despair...", "Mortals, only death awaits you here...", "My revenge will be great...",
            "All those who oppose us will perish..."};
    /** Game tick each player last prayed at a God Wars altar. */
    private static final Map<UUID, Long> ALTAR_USED = new HashMap<>();
    /** Game tick each player in the dungeon was last attacked (hit or missed) by a creature. */
    private static final Map<UUID, Long> ATTACKED = new HashMap<>();
    private static long ticks;

    private GwdDungeonHooks() {
    }

    static void register() {
        for (String block : new String[]{GwdDungeon.BLOCK_DOOR, GwdDungeon.BLOCK_PILLAR, GwdDungeon.BLOCK_ROCK, GwdDungeon.BLOCK_ICE_BRIDGE,
                GwdDungeon.BLOCK_FROZEN_DOOR, GwdDungeon.BLOCK_BARRIER}) {
            BlockHooks.register(block, (player, pos, state) -> usePassage(player, pos));
        }
        for (Altar a : GwdDungeon.altars()) {
            BlockHooks.register(a.block(), (player, pos, state) -> useAltar(player, a));
        }
        BlockHooks.register(GwdDungeon.BLOCK_HOLE, (player, pos, state) -> climbDown(player));
        BlockHooks.register(GwdDungeon.BLOCK_ROPE, (player, pos, state) -> climbUp(player));
        com.runecraft.combat.Aggression.addPeaceful(GwdDungeonHooks::calmed);
        com.runecraft.combat.CombatHooks.addDamageListener((player, attacker, attack, style, damage) -> {
            if (attacker != null && attacker != player && GwdDungeon.contains(player.getBlockX(), player.getBlockY(), player.getBlockZ())) {
                ATTACKED.put(player.getUUID(), ActionManager.gameTick());
            }
        });
        // the dungeon cannot be dug through or built in outside creative mode, so its doors and obstacles hold;
        // only the ice of Nex's prison breaks
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> player.isCreative() || !Gielinor.is(world)
                || !GwdDungeon.contains(pos.getX(), pos.getY(), pos.getZ()) || state.is(Blocks.ICE));
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            BlockPos at = hit.getBlockPos();
            // The altars are handled here, ahead of vanilla: a sneaking player holding anything never reaches the block's
            // own use, and the sneak-use is how a player teleports out of a chamber.
            Altar altar = world.getBlockState(at).getBlock() instanceof com.runecraft.block.ModuleBlock mb ? GwdDungeon.altar(mb.def().id())
                    : null;
            if (altar != null && !(player.isCreative() && player.getItemInHand(hand).getItem() instanceof BlockItem)) {
                if (hand == net.minecraft.world.InteractionHand.MAIN_HAND && player instanceof ServerPlayer sp) {
                    useAltar(sp, altar);
                }
                return InteractionResult.sidedSuccess(world.isClientSide());
            }
            if (!world.isClientSide() && !player.isCreative() && player.getItemInHand(hand).getItem() instanceof BlockItem
                    && GwdDungeon.contains(at.getX(), at.getY(), at.getZ()) && Gielinor.is(world)) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++ticks % 20 != 0) {
                return;
            }
            ServerLevel world = Gielinor.world(server);
            if (world == null) {
                return;
            }
            try {
                tick(world);
            } catch (RuntimeException e) {
                com.runecraft.RuneCraft.LOGGER.error("God Wars Dungeon hooks failed", e);
            }
        });
    }

    // ------------------------------------------------------------------ worn items and aggression

    /** Item ids of everything the player wears: armour, hands, the extra slots and quest gear. */
    static List<String> wornIds(ServerPlayer p) {
        List<String> ids = new ArrayList<>();
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
                EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
            ItemStack st = p.getItemBySlot(slot);
            if (!st.isEmpty()) {
                ids.add(Inv.idOf(st));
            }
        }
        for (ItemStack st : com.runecraft.content.systems.SysState.of(p).worn.values()) {
            if (st != null && !st.isEmpty()) {
                ids.add(Inv.idOf(st));
            }
        }
        ids.addAll(com.runecraft.combat.HitCalcBridge.extraWornIds.apply(p));
        return ids;
    }

    /** OSRS: a God Wars follower leaves alone a player wearing at least one item devoted to its god. */
    static boolean calmed(Mob mob, ServerPlayer player) {
        if (!(mob instanceof com.runecraft.entity.OsrsMonsterEntity m) || m.stats() == null || m.stats().profile() == null) {
            return false;
        }
        String god = GodWarsFactions.allegiance(m.stats().profile().id);
        return god != null && GodItems.wears(god, wornIds(player));
    }

    // ------------------------------------------------------------------ the hole and the rope

    /** Teleports a player onto a tile of the dungeon, building the chunks around it first. */
    static void send(ServerPlayer player, Spot s) {
        ServerLevel world = Gielinor.world(player.getServer());
        if (world == null) {
            return;
        }
        StructurePlacer.applyAround(world, s.blockX() + 1, s.blockZ() + 1, 10);
        player.teleportTo(world, s.blockX() + 1.0, s.feetY(), s.blockZ() + 1.0, player.getYRot(), player.getXRot());
    }

    static void climbDown(ServerPlayer player) {
        PlayerData data = Players.data(player);
        if (QuestApi.stage(player, "troll_stronghold") < 2 && !QuestApi.isComplete(player, "troll_stronghold")) {
            Msg.game(player, "The trolls' champion Dad bars the way north of Trollheim. (Troll Stronghold: defeat Dad.)");
            return;
        }
        SkillSet s = data.skills();
        if (s.currentLevel(Skill.STRENGTH) < 60 && s.currentLevel(Skill.AGILITY) < 60) {
            Msg.game(player, "You need 60 Strength to move the boulder or 60 Agility to squeeze through the little crack.");
            return;
        }
        GwdKillcountState state = GwdKillcountState.get(player.getServer());
        if (!state.flag(player.getUUID(), GwdKillcountState.ROPE_HOLE)) {
            if (!Inv.has(player, "rope", 1)) {
                Msg.game(player, "The hole is too deep to climb down without a rope.");
                return;
            }
            Inv.remove(player, "rope", 1);
            state.setFlag(player.getUUID(), GwdKillcountState.ROPE_HOLE);
            Msg.game(player, "You tie the rope to the rocks above the hole.");
        }
        Msg.game(player, "You climb down the rope into the God Wars Dungeon.");
        send(player, GwdDungeon.LANDING);
    }

    static void climbUp(ServerPlayer player) {
        ServerLevel world = Gielinor.world(player.getServer());
        if (world == null) {
            return;
        }
        GwdKillcount.clear(player);
        BlockPos to = Gielinor.safeSpot(world, com.runecraft.osrs.world.StructureDef.bx(GwdDungeon.SURFACE_EXIT[0]),
                com.runecraft.osrs.world.StructureDef.bz(GwdDungeon.SURFACE_EXIT[1]));
        Msg.game(player, "You climb up the rope.");
        player.teleportTo(world, to.getX() + 0.5, to.getY(), to.getZ() + 0.5, player.getYRot(), player.getXRot());
    }

    /** Puts the hole into the God Wars Dungeon landmark's plaza (once, in old worlds too) and the rope at the landing. */
    private static void placeHole(ServerLevel world) {
        int x = com.runecraft.osrs.world.StructureDef.bx(GwdDungeon.HOLE_TILE[0]);
        int z = com.runecraft.osrs.world.StructureDef.bz(GwdDungeon.HOLE_TILE[1]);
        if (!world.hasChunk(x >> 4, z >> 4)) {
            return;
        }
        Block hole = ModBlocks.MODULE_BLOCKS.get(GwdDungeon.BLOCK_HOLE);
        if (hole == null) {
            return;
        }
        // checked every second while someone is near: the landmark's plaza, built on the same ground, may pave it over
        int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        for (int dy = 0; dy >= -3; dy--) {
            if (world.getBlockState(new BlockPos(x, y + dy, z)).is(hole)) {
                return;
            }
        }
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                world.setBlock(new BlockPos(x + dx, y, z + dz), hole.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** True if the block is the surface hole (the self-test). */
    static BlockPos holePos(ServerLevel world) {
        int x = com.runecraft.osrs.world.StructureDef.bx(GwdDungeon.HOLE_TILE[0]);
        int z = com.runecraft.osrs.world.StructureDef.bz(GwdDungeon.HOLE_TILE[1]);
        world.getChunk(x >> 4, z >> 4);
        placeHole(world);
        Block hole = ModBlocks.MODULE_BLOCKS.get(GwdDungeon.BLOCK_HOLE);
        int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        for (int dy = 0; dy >= -4; dy--) {
            BlockPos p = new BlockPos(x, y + dy, z);
            if (hole != null && world.getBlockState(p).is(hole)) {
                return p;
            }
        }
        return null;
    }

    private static void tick(ServerLevel world) {
        long now = ActionManager.gameTick();
        ATTACKED.values().removeIf(at -> now - at > 100);
        Landmark gwd = Landmark.GOD_WARS_DUNGEON;
        for (ServerPlayer p : world.players()) {
            if (Math.abs(p.getX() - gwd.blockX()) < 48 && Math.abs(p.getZ() - gwd.blockZ()) < 48) {
                placeHole(world);
            }
            // OSRS: leaving the dungeon (not logging out inside it) removes all the essence gathered.
            if (!GwdDungeon.contains(p.getBlockX(), p.getBlockY(), p.getBlockZ()) && GwdKillcount.anyEssence(p)) {
                GwdKillcount.clear(p);
            }
        }
    }

    // ------------------------------------------------------------------ passages

    /** Which side of a passage the player stands on: true for the inner side ({@link Passage#b()}). */
    static boolean innerSide(ServerPlayer player, Passage p) {
        int plane = GwdDungeon.planeAt(player.getBlockY());
        Area here = plane < 0 ? null : GwdDungeon.areaAt(player.getBlockX(), player.getBlockY(), player.getBlockZ());
        Area outer = GwdDungeon.areaOf(p.a().plane(), p.a().x(), p.a().y());
        Area inner = GwdDungeon.areaOf(p.b().plane(), p.b().x(), p.b().y());
        if (outer != inner && here != null) {
            if (here == inner) {
                return true;
            }
            if (here == outer) {
                return false;
            }
        }
        if (p.a().plane() != p.b().plane() && plane >= 0) {
            return plane == p.b().plane();
        }
        return dist2(player, p.b()) < dist2(player, p.a());
    }

    private static double dist2(ServerPlayer player, Spot s) {
        double dx = player.getX() - (s.blockX() + 1);
        double dz = player.getZ() - (s.blockZ() + 1);
        double dy = player.getY() - s.feetY();
        return dx * dx + dz * dz + dy * dy * 4;
    }

    static void usePassage(ServerPlayer player, BlockPos pos) {
        Passage p = GwdDungeon.passageAt(pos.getX(), pos.getY(), pos.getZ());
        if (p == null) {
            Msg.game(player, "Nothing interesting happens.");
            return;
        }
        boolean inner = innerSide(player, p);
        if (cross(player, p, !inner)) {
            send(player, inner ? p.a() : p.b());
        }
    }

    /**
     * Checks a passage's requirement for going in ({@code in}: from the outer side to the inner one) or out, consuming
     * what it costs; true if the player may cross. The messages say why not.
     */
    static boolean cross(ServerPlayer player, Passage p, boolean in) {
        SkillSet s = Players.data(player).skills();
        switch (p.kind()) {
            case BIG_DOOR -> {
                if (!in) {
                    Msg.game(player, "You push the big door open.");
                    return true;
                }
                if (s.level(Skill.STRENGTH) < 70) {
                    Msg.game(player, "You need a Strength level of 70 to bang the big door's gong hard enough.");
                    return false;
                }
                if (!hasHammer(player)) {
                    Msg.game(player, "You need a hammer to bang on the big door's gong.");
                    return false;
                }
            }
            case GRAPPLE -> {
                if (s.level(Skill.RANGED) < 70) {
                    Msg.game(player, "You need a Ranged level of 70 to grapple across the chasm.");
                    return false;
                }
                if (in && (!hasCrossbow(player) || !hasGrapple(player))) {
                    Msg.game(player, "You need a crossbow and a mith grapple to swing across to the pillar.");
                    return false;
                }
            }
            case ROCK -> {
                if (s.level(Skill.AGILITY) < 70) {
                    Msg.game(player, "You need an Agility level of 70 to climb this rock.");
                    return false;
                }
                int flag = p.id().equals("saradomin_upper_rock") ? GwdKillcountState.ROPE_UPPER_ROCK : GwdKillcountState.ROPE_LOWER_ROCK;
                GwdKillcountState state = GwdKillcountState.get(player.getServer());
                if (!state.flag(player.getUUID(), flag)) {
                    if (!Inv.has(player, "rope", 1)) {
                        Msg.game(player, "You need a rope to tie to the rock before you can climb down.");
                        return false;
                    }
                    Inv.remove(player, "rope", 1);
                    state.setFlag(player.getUUID(), flag);
                    Msg.game(player, "You tie a rope to the rock.");
                }
                Msg.game(player, in ? "You climb down the rope." : "You climb up the rope.");
                return true;
            }
            case ICE_BRIDGE -> {
                if (s.currentLevel(Skill.HITPOINTS) < 70) {
                    Msg.game(player, "You need a Hitpoints level of 70 to brave the icy water.");
                    return false;
                }
                if (in) {
                    PlayerData data = Players.data(player);
                    data.setPrayerPoints(0);
                    ServerNetworking.sync(player);
                    Msg.red(player, "The icy water drains your Prayer points.");
                }
            }
            case CHAMBER_DOOR -> {
                if (in) {
                    return GwdKillcount.enter(player, p.god(), generalName(p.god()));
                }
                Msg.game(player, "You leave the chamber.");
                return true;
            }
            case FROZEN_DOOR -> {
                if (in && !QuestApi.isComplete(player, "the_frozen_door") && !Inv.has(player, "frozen_key", 1)) {
                    Msg.game(player, "You try to open the door, but it doesn't seem to budge.");
                    Msg.red(player, "Strange Voice: " + STRANGE_VOICE[player.getRandom().nextInt(STRANGE_VOICE.length)]);
                    return false;
                }
                Msg.game(player, in ? p.message() : "You climb back up to the God Wars Dungeon.");
                return true;
            }
            case PRISON_DOOR -> {
                Msg.game(player, p.message());
                return true;
            }
            case ESSENCE_DOOR -> {
                if (in) {
                    return GwdKillcount.enterRefuge(player);
                }
                Msg.game(player, "You leave the refuge.");
                return true;
            }
            case BARRIER -> {
                if (!in) {
                    Msg.game(player, "The barrier won't let you leave. Use the altar to teleport out of the chamber.");
                    return false;
                }
                ServerLevel world = (ServerLevel) player.level();
                if (nexBusy(world)) {
                    Msg.red(player, "The barrier glows red: Nex is being fought. Wait until the fight is over.");
                    return false;
                }
                Msg.game(player, p.message());
                startNex(world, player);
                return true;
            }
            default -> {
            }
        }
        if (p.message() != null) {
            Msg.game(player, p.message());
        }
        return true;
    }

    static String generalName(God god) {
        return switch (god) {
            case ARMADYL -> "Kree'arra";
            case BANDOS -> "General Graardor";
            case SARADOMIN -> "Commander Zilyana";
            case ZAMORAK -> "K'ril Tsutsaroth";
        };
    }

    private static boolean carries(ServerPlayer player, java.util.function.Predicate<String> id) {
        if (id.test(Inv.idOf(player.getMainHandItem())) || id.test(Inv.idOf(player.getOffhandItem()))) {
            return true;
        }
        for (ItemStack st : player.getInventory().items) {
            if (!st.isEmpty() && id.test(Inv.idOf(st))) {
                return true;
            }
        }
        for (ItemStack st : com.runecraft.content.systems.SysState.of(player).worn.values()) {
            if (st != null && !st.isEmpty() && id.test(Inv.idOf(st))) {
                return true;
            }
        }
        return false;
    }

    /** A hammer, any warhammer, the elder maul or the Imcando hammer (OSRS Wiki, Big door). */
    static boolean hasHammer(ServerPlayer player) {
        return carries(player, id -> id.equals("hammer") || id.endsWith("warhammer") || id.equals("elder_maul")
                || id.equals("imcando_hammer"));
    }

    static boolean hasCrossbow(ServerPlayer player) {
        return carries(player, id -> id.endsWith("crossbow") && !id.endsWith("_u"));
    }

    /** The skilling module's mith grapple (carried or worn as ammunition). */
    static boolean hasGrapple(ServerPlayer player) {
        return carries(player, id -> id.equals("mith_grapple"));
    }

    // ------------------------------------------------------------------ altars

    static void useAltar(ServerPlayer player, Altar a) {
        if (player.isShiftKeyDown()) {
            Msg.game(player, "You teleport out of the chamber.");
            send(player, a.outside());
            return;
        }
        if (inCombat(player)) {
            // OSRS Wiki (God Wars Dungeon): the altar can only be used while the player isn't in combat; its teleport can.
            Msg.game(player, "You can't use the altar while you are in combat. (Sneak and use it to teleport out of the chamber.)");
            return;
        }
        long now = ActionManager.gameTick();
        Long last = ALTAR_USED.get(player.getUUID());
        if (last != null && now - last < GwdDungeon.ALTAR_COOLDOWN_TICKS) {
            long minutes = (GwdDungeon.ALTAR_COOLDOWN_TICKS - (now - last)) * 6 / 600 + 1;
            Msg.game(player, "The gods won't answer your prayers again so soon. (About " + minutes + " minute" + (minutes == 1 ? "" : "s")
                    + "; sneak and use the altar to teleport out of the chamber.)");
            return;
        }
        String god = a.god() == null ? "zaros" : a.god().id();
        int items = GodItems.count(god, wornIds(player));
        PlayerData data = Players.data(player);
        SkillSet s = data.skills();
        if (items > 0) {
            s.setBoost(Skill.PRAYER, Math.max(s.boost(Skill.PRAYER), items));
        }
        data.setPrayerPoints(s.level(Skill.PRAYER) + items);
        if (a.god() == null && items > 0) {
            // The Zarosian altar also restores hitpoints, special attack and run energy to a player wearing a Zarosian item.
            player.setHealth(player.getMaxHealth());
            com.runecraft.content.systems.SysState.SysPlayer sp = com.runecraft.content.systems.SysState.of(player);
            sp.specEnergy = 100;
            sp.runEnergy = 10000;
            com.runecraft.content.systems.SysNet.sendState(player);
        }
        ALTAR_USED.put(player.getUUID(), now);
        ServerNetworking.sync(player);
        Msg.game(player, "You recharge your Prayer points at the " + a.name() + (items > 0 ? " (+" + items + " for your devotion)." : "."));
    }

    /** Forgets a player's altar cooldown (the self-test). */
    static void resetAltar(ServerPlayer player) {
        ALTAR_USED.remove(player.getUUID());
    }

    /**
     * OSRS's combat timer: a player who attacked something or was attacked in the last
     * {@link com.runecraft.osrs.SingleCombat#COMBAT_TICKS} game ticks is in combat.
     */
    static boolean inCombat(ServerPlayer player) {
        long now = ActionManager.gameTick();
        long attacking = com.runecraft.content.systems.SysState.of(player).lastCombatTick;
        Long attacked = ATTACKED.get(player.getUUID());
        int window = com.runecraft.osrs.SingleCombat.COMBAT_TICKS;
        return (attacking > 0 && now - attacking < window) || (attacked != null && now - attacked < window);
    }

    // ------------------------------------------------------------------ Nex

    /** Nex, alive in her chamber, or null. */
    static DataBossEntity nex(ServerLevel world) {
        EntityType<DataBossEntity> type = ModEntities.DATA_BOSS_TYPES.get("nex");
        if (type == null) {
            return null;
        }
        Spot c = GwdDungeon.NEX_SPAWN;
        List<DataBossEntity> found = world.getEntities(type, new net.minecraft.world.phys.AABB(c.blockX() - 40, c.feetY() - 6,
                c.blockZ() - 40, c.blockX() + 40, c.feetY() + 16, c.blockZ() + 40), e -> e.isAlive());
        return found.isEmpty() ? null : found.get(0);
    }

    /** True while Nex is alive and somebody is in her chamber fighting her (the barrier glows red). */
    static boolean nexBusy(ServerLevel world) {
        if (nex(world) == null) {
            return false;
        }
        for (ServerPlayer p : world.players()) {
            if (p.isAlive() && GwdDungeon.areaAt(p.getBlockX(), p.getBlockY(), p.getBlockZ()) == Area.NEX_CHAMBER) {
                return true;
            }
        }
        return false;
    }

    /** Nex appears in the middle of her chamber when someone passes the barrier and she isn't there yet. */
    static DataBossEntity startNex(ServerLevel world, ServerPlayer player) {
        DataBossEntity existing = nex(world);
        if (existing != null) {
            return existing;
        }
        BossDef def = ContentRegistry.boss("nex");
        if (def == null) {
            return null;
        }
        Spot c = GwdDungeon.NEX_SPAWN;
        StructurePlacer.applyAround(world, c.blockX(), c.blockZ(), 40);
        BlockPos at = new BlockPos(c.blockX(), c.feetY(), c.blockZ());
        DataBossEntity nex = DataBossService.spawn(world, at, def, null);
        if (nex != null) {
            nex.markSummoned(at, 34);
            if (player != null) {
                nex.setTarget(player);
            }
        }
        return nex;
    }
}
