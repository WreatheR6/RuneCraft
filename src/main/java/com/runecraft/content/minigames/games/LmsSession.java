package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.Arena;
import com.runecraft.content.minigames.LmsStash;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.content.systems.RunEnergyService;
import com.runecraft.content.systems.SysState;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.LastManStanding;
import com.runecraft.osrs.content.minigames.LastManStanding.Build;
import com.runecraft.osrs.content.minigames.LastManStanding.Landmark;
import com.runecraft.osrs.content.minigames.LastManStanding.Mode;
import com.runecraft.osrs.content.minigames.LastManStanding.Stack;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.player.Players;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Last Man Standing on the Deserted Island (see {@link LastManStanding}). Every player's own things are put aside
 * ({@link LmsStash}) and they play the build chosen for the game with its loadout. Kills restore the killer, give a bloody
 * (or bloodier) key for the island's chests and 20 seconds of immunity; the victim's loot (not their supplies) drops for the
 * killer. The fog closes in on one landmark; loot crates appear and are announced. Deaths are safe: leaving the island, for
 * any reason, gives back everything the player brought. Competitive games award points and change the player's rank.
 */
public class LmsSession extends MinigameSession {
    private static final int HALF = 36;

    private final Mode mode;
    private Build build = Build.MAX_MED;
    private boolean started;
    private int starters;
    private Landmark safeZone;
    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, Long> immuneUntil = new HashMap<>();
    private final Map<UUID, Integer> fogTicks = new HashMap<>();
    private final Map<UUID, UUID> lastHitBy = new HashMap<>();
    private final List<BlockPos> crates = new ArrayList<>();

    public LmsSession(int id, int slot, ServerWorld world, String variant) {
        super(Minigame.LAST_MAN_STANDING, id, slot, world, variant);
        this.mode = Mode.of(variant);
    }

    /**
     * Competitive and High Stakes games need total level 750 and 30 quest points, or 1,500 total; High Stakes also needs the
     * 500,000-coin fee (or a free entry), taken when the game starts.
     */
    public static String requirement(ServerPlayerEntity player, String variant) {
        Mode m = Mode.of(variant);
        if (!m.ranked()) {
            return null;
        }
        var data = Players.data(player);
        if (!LastManStanding.competitiveAllowed(data.skills().totalLevel(), data.questPoints())) {
            return "Lisa: " + m.displayName() + " games need a total level of 750 and 30 quest points, or a total level of 1,500.";
        }
        if (m == Mode.HIGH_STAKES && MinigameData.of(player).stat(player.getUuid(), LastManStanding.HIGH_STAKES_FREE) <= 0
                && !Inv.has(player, Inv.COINS, LastManStanding.HIGH_STAKES_FEE)) {
            return "Lisa: High Stakes games cost " + String.format("%,d", LastManStanding.HIGH_STAKES_FEE) + " coins to enter.";
        }
        return null;
    }

    /**
     * Takes every High Stakes player's fee (a free entry first) before the game starts; players who can't pay are sent back.
     * Returns the players who paid coins, so a cancelled game can refund them.
     */
    private List<ServerPlayerEntity> takeFees() {
        List<ServerPlayerEntity> paid = new ArrayList<>();
        for (ServerPlayerEntity p : online()) {
            MinigameData data = MinigameData.of(p);
            int free = data.stat(p.getUuid(), LastManStanding.HIGH_STAKES_FREE);
            if (free > 0) {
                data.setStat(p.getUuid(), LastManStanding.HIGH_STAKES_FREE, free - 1);
                Msg.game(p, "Lisa: This High Stakes game is on the house.");
            } else if (Inv.has(p, Inv.COINS, LastManStanding.HIGH_STAKES_FEE)) {
                Inv.remove(p, Inv.COINS, LastManStanding.HIGH_STAKES_FEE);
                paid.add(p);
            } else {
                Msg.game(p, "Lisa: You can't pay the High Stakes entry fee.");
                removePlayer(p, true);
            }
        }
        return paid;
    }

    @Override
    protected int halfX() {
        return HALF;
    }

    @Override
    protected int halfZ() {
        return HALF;
    }

    @Override
    protected int height() {
        return 16;
    }

    // ------------------------------------------------------------------ the island

    @Override
    protected void build() {
        room(Blocks.SAND, Blocks.STONE_BRICKS, Blocks.SAND);
        // Sea around the island, grass and sand inland.
        for (int dx = -HALF; dx <= HALF; dx++) {
            for (int dz = -HALF; dz <= HALF; dz++) {
                double r = Math.sqrt(dx * dx + dz * dz);
                if (r > HALF - 2) {
                    set(dx, 0, dz, Blocks.WATER.getDefaultState());
                } else if (r < HALF - 5) {
                    set(dx, 0, dz, ((dx * 31 + dz * 17) & 7) == 0 ? Blocks.COARSE_DIRT.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState());
                }
            }
        }
        for (Landmark l : LastManStanding.LANDMARKS) {
            int[] o = LastManStanding.offset(l.x(), l.y());
            switch (l.name()) {
                case "Moser settlement" -> {
                    hut(o[0] - 3, o[1], Blocks.COBBLESTONE.getDefaultState(), Blocks.OAK_PLANKS.getDefaultState());
                    hut(o[0] + 3, o[1] + 1, Blocks.COBBLESTONE.getDefaultState(), Blocks.OAK_PLANKS.getDefaultState());
                }
                case "Trinity outpost" -> tower(o[0], o[1]);
                case "the mountain" -> mountain(o[0], o[1]);
                case "the swamp" -> swamp(o[0], o[1]);
                case "rocky outcrop" -> rocks(o[0], o[1]);
                case "Debtor hideout" -> hut(o[0], o[1], Blocks.DARK_OAK_PLANKS.getDefaultState(), Blocks.DARK_OAK_PLANKS.getDefaultState());
                default -> ruins(o[0], o[1]);
            }
        }
        java.util.Set<Long> placed = new java.util.HashSet<>();
        for (int[] c : LastManStanding.CHESTS) {
            int[] o = LastManStanding.offset(c[0], c[1]);
            if (placed.add(BlockPos.asLong(o[0], 0, o[1]))) {
                set(o[0], surface(o[0], o[1]), o[1], Arena.module(LastManStanding.CHEST));
            }
        }
        // Rowboats on the east and west shores: leave (forfeit) the game.
        set(HALF - 4, 1, 0, Arena.module(LastManStanding.ROWBOAT));
        set(-HALF + 4, 1, 0, Arena.module(LastManStanding.ROWBOAT));
    }

    /** First free height above the island floor at an offset (on top of hills and inside huts). */
    private int surface(int dx, int dz) {
        for (int y = 1; y < 10; y++) {
            if (world.getBlockState(rel(dx, y, dz)).isAir()) {
                return y;
            }
        }
        return 10;
    }

    private void hut(int cx, int cz, BlockState wall, BlockState roof) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                boolean edge = Math.abs(x) == 2 || Math.abs(z) == 2;
                for (int y = 1; y <= 3; y++) {
                    boolean door = z == 2 && x == 0 && y <= 2;
                    set(cx + x, y, cz + z, edge && !door ? wall : Blocks.AIR.getDefaultState());
                }
                set(cx + x, 4, cz + z, roof);
            }
        }
    }

    private void tower(int cx, int cz) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                boolean edge = Math.abs(x) == 2 || Math.abs(z) == 2;
                for (int y = 1; y <= 6; y++) {
                    boolean door = z == 2 && x == 0 && y <= 2;
                    boolean window = edge && y == 4 && (x == 0 || z == 0);
                    set(cx + x, y, cz + z, edge && !door && !window ? Blocks.STONE_BRICKS.getDefaultState() : Blocks.AIR.getDefaultState());
                }
                set(cx + x, 7, cz + z, (Math.abs(x) == 2 || Math.abs(z) == 2) && ((x + z) & 1) == 0
                        ? Blocks.STONE_BRICK_WALL.getDefaultState() : Blocks.AIR.getDefaultState());
            }
        }
    }

    private void mountain(int cx, int cz) {
        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                int h = 5 - (int) Math.round(Math.sqrt(x * x + z * z) * 0.75);
                for (int y = 1; y <= h; y++) {
                    set(cx + x, y, cz + z, y == h ? Blocks.GRASS_BLOCK.getDefaultState() : Blocks.STONE.getDefaultState());
                }
            }
        }
    }

    private void swamp(int cx, int cz) {
        for (int x = -6; x <= 6; x++) {
            for (int z = -5; z <= 5; z++) {
                int n = (x * 13 + z * 7 + x * z) & 3;
                set(cx + x, 0, cz + z, n == 0 ? Blocks.WATER.getDefaultState() : Blocks.MUD.getDefaultState());
            }
        }
    }

    private void rocks(int cx, int cz) {
        int[][] boulders = {{0, 0, 3}, {-3, 2, 2}, {3, -2, 2}, {2, 3, 1}, {-2, -3, 1}};
        for (int[] b : boulders) {
            for (int y = 1; y <= b[2]; y++) {
                set(cx + b[0], y, cz + b[1], (y & 1) == 0 ? Blocks.ANDESITE.getDefaultState() : Blocks.COBBLESTONE.getDefaultState());
                set(cx + b[0] + 1, y, cz + b[1], Blocks.STONE.getDefaultState());
            }
        }
    }

    private void ruins(int cx, int cz) {
        for (int i = -3; i <= 3; i++) {
            int h = 1 + Math.floorMod(i * 7, 3);
            for (int y = 1; y <= h; y++) {
                set(cx + i, y, cz - 3, Blocks.MOSSY_COBBLESTONE.getDefaultState());
                if (i != 0) {
                    set(cx - 3, y, cz + i, Blocks.MOSSY_COBBLESTONE.getDefaultState());
                }
            }
        }
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayerEntity player, int index) {
        // Pairs of spawn points around the shore, like the Deserted Island.
        double a = (index / 2) * 2.399963 + (index % 2) * 0.12;
        int x = (int) Math.round(Math.cos(a) * (HALF - 9));
        int z = (int) Math.round(Math.sin(a) * (HALF - 9));
        return rel(x, surface(x, z), z);
    }

    // ------------------------------------------------------------------ game

    @Override
    protected void start() {
        List<ServerPlayerEntity> paid = mode == Mode.HIGH_STAKES && playerCount() >= mode.minPlayers ? takeFees() : List.of();
        starters = playerCount();
        if (starters < mode.minPlayers) {
            for (ServerPlayerEntity p : paid) {
                Inv.give(p, Inv.COINS, LastManStanding.HIGH_STAKES_FEE);
            }
            broadcast("Lisa: There aren't enough players for a game. " + mode.minPlayers + " are needed.");
            end(false);
            return;
        }
        Map<Build, Integer> votes = new EnumMap<>(Build.class);
        for (ServerPlayerEntity p : online()) {
            Build pref = Build.of(MinigameData.of(p).stat(p.getUuid(), LastManStanding.BUILD_PREFERENCE));
            votes.merge(pref, 1, Integer::sum);
        }
        build = LastManStanding.chooseBuild(votes, random);
        safeZone = LastManStanding.LANDMARKS.get(random.nextInt(LastManStanding.LANDMARKS.size()));
        for (ServerPlayerEntity p : online()) {
            LmsStash.stash(p);
            for (Skill s : LastManStanding.BUILD_SKILLS) {
                XpService.setLevel(p, s, build.level(s));
            }
            for (Skill s : Skill.values()) {
                Players.data(p).skills().setBoost(s, 0);
            }
            Players.data(p).activePrayers().clear();
            equip(p);
            refill(p);
            restoreStats(p);
            Msg.game(p, mode.displayName() + " game: everyone plays the " + build.displayName
                    + " build. Your own items are safe with Lisa until you leave.");
        }
        started = true;
        broadcastRed("The game begins in 12 seconds. Be the last one standing!");
    }

    private void equip(ServerPlayerEntity p) {
        for (String id : LastManStanding.loadout(build).worn()) {
            wear(p, Inv.stack(id, 1));
        }
        for (Stack s : LastManStanding.loadout(build).carried()) {
            Inv.give(p, s.item(), s.count());
        }
    }

    /** Puts an item straight into its equipment slot (armour, weapon, shield or one of the extra worn slots). */
    static void wear(ServerPlayerEntity p, ItemStack stack) {
        String id = Inv.idOf(stack);
        Jewellery.Worn worn = Jewellery.worn(id);
        if (worn != null) {
            SysState.of(p).worn.put(worn.slot(), stack);
            return;
        }
        ItemDef def = ItemCatalog.get(id);
        if (def == null || def.equipment() == null) {
            Inv.give(p, stack);
            return;
        }
        EquipmentDef.Slot slot = def.equipment().slot();
        switch (slot) {
            case HEAD -> p.equipStack(EquipmentSlot.HEAD, stack);
            case CHEST -> p.equipStack(EquipmentSlot.CHEST, stack);
            case LEGS -> p.equipStack(EquipmentSlot.LEGS, stack);
            case FEET -> p.equipStack(EquipmentSlot.FEET, stack);
            case SHIELD -> p.equipStack(EquipmentSlot.OFFHAND, stack);
            default -> p.setStackInHand(Hand.MAIN_HAND, stack);
        }
    }

    /** Tops the player's supplies back up to the loadout's amounts (10 sharks, 3 brews...). */
    private static void refill(ServerPlayerEntity p) {
        for (Stack s : LastManStanding.SUPPLIES) {
            int missing = s.count() - Inv.count(p, s.item());
            if (missing > 0) {
                Inv.give(p, s.item(), missing);
            }
        }
    }

    /** Full health, prayer, special attack and run energy. */
    private static void restoreStats(ServerPlayerEntity p) {
        p.setHealth(p.getMaxHealth());
        Players.data(p).setPrayerPoints(99);
        SysState.of(p).specEnergy = 100;
        RunEnergyService.restore(p, 10_000);
    }

    /**
     * The build's prayer rules (see {@link Build#allows}): a message for a prayer the build can't use, "" for one it can (no
     * quest or scroll unlocks needed on the island), null before the game starts.
     */
    public String prayerRule(com.runecraft.osrs.Prayer prayer) {
        if (!started) {
            return null;
        }
        String blocked = LastManStanding.prayerBlocked(build, prayer);
        return blocked == null ? "" : blocked;
    }

    @Override
    protected boolean canAttackPlayer(ServerPlayerEntity attacker, ServerPlayerEntity target) {
        if (!started || ticks < LastManStanding.START_TICKS) {
            return false;
        }
        if (ticks < immuneUntil.getOrDefault(target.getUuid(), 0L)) {
            return false;
        }
        immuneUntil.remove(attacker.getUuid()); // attacking ends your own immunity // approx
        lastHitBy.put(target.getUuid(), attacker.getUuid());
        return true;
    }

    @Override
    protected void gameTick() {
        if (!started) {
            return;
        }
        if (playerCount() <= 1) {
            end(true);
            return;
        }
        if (ticks >= LastManStanding.GAME_LIMIT_TICKS) {
            broadcastRed("The game has run out of time. There is no winner.");
            end(false);
            return;
        }
        if (ticks == LastManStanding.START_TICKS) {
            broadcastRed("FIGHT!");
        }
        for (ServerPlayerEntity p : online()) {
            // Infinite prayer and run energy.
            Players.data(p).setPrayerPoints(99);
            RunEnergyService.restore(p, 10_000);
        }
        if (ticks == LastManStanding.FOG_WARNING_TICK) {
            int[] o = LastManStanding.offset(safeZone.x(), safeZone.y());
            set(o[0], surface(o[0], o[1]), o[1], Arena.module(LastManStanding.FOG_MARKER));
            broadcastRed("Lethal fog will soon fill the island! The safe zone is " + safeZone.name() + ".");
        }
        if (ticks == LastManStanding.FOG_TICK) {
            broadcastRed("The fog begins to close in on " + safeZone.name() + ".");
        }
        if (ticks == LastManStanding.FINAL_TICK) {
            broadcastRed("The fog is at its thickest: get into the safe zone at " + safeZone.name() + "!");
        }
        fog();
        if (ticks > LastManStanding.START_TICKS && ticks % LastManStanding.CRATE_INTERVAL == 0 && crates.size() < 2) {
            spawnCrate();
        }
    }

    private void fog() {
        int[] o = LastManStanding.offset(safeZone.x(), safeZone.y());
        int radius = LastManStanding.safeRadius(ticks);
        for (ServerPlayerEntity p : online()) {
            BlockPos at = relPos(p);
            double dx = at.getX() - o[0];
            double dz = at.getZ() - o[1];
            int tiles = (int) Math.round(Math.sqrt(dx * dx + dz * dz) * 2);
            int depth = tiles - radius;
            int inFog = depth > 0 && ticks >= LastManStanding.FOG_TICK ? fogTicks.merge(p.getUuid(), 1, Integer::sum) : 0;
            if (inFog == 0) {
                fogTicks.remove(p.getUuid());
            }
            int dmg = LastManStanding.fogDamage(ticks, depth, inFog, random);
            if (dmg > 0) {
                hurt(p, dmg);
            }
        }
    }

    private void spawnCrate() {
        BlockPos at = randomFloor(0, 0, HALF - 8);
        BlockPos r = at.subtract(origin);
        BlockPos top = rel(r.getX(), surface(r.getX(), r.getZ()), r.getZ());
        Arena.set(world, top, Arena.module(LastManStanding.CRATE));
        crates.add(top);
        Landmark near = LastManStanding.nearest(LastManStanding.ISLAND_X + r.getX() * 2, LastManStanding.ISLAND_Y - r.getZ() * 2);
        broadcast("A loot crate has spawned near " + near.name() + ".");
    }

    @Override
    protected boolean onBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
        if (blockId.equals(LastManStanding.CHEST)) {
            boolean bloodier = Inv.has(player, LastManStanding.BLOODIER_KEY, 1);
            if (!bloodier && !Inv.has(player, LastManStanding.BLOODY_KEY, 1)) {
                Msg.game(player, "You need a bloody key to open this chest. Kill another player to get one.");
                return true;
            }
            Inv.remove(player, bloodier ? LastManStanding.BLOODIER_KEY : LastManStanding.BLOODY_KEY, 1);
            List<Stack> loot = LastManStanding.openChest(build, bloodier, random);
            give(player, loot);
            Msg.game(player, "You open the chest and find " + describe(loot) + ".");
            return true;
        }
        if (blockId.equals(LastManStanding.CRATE)) {
            List<Stack> loot = LastManStanding.openCrate(build, random);
            give(player, loot);
            crates.remove(pos);
            Arena.set(world, pos, Blocks.AIR.getDefaultState());
            Msg.game(player, "You search the crate and find " + describe(loot) + ".");
            return true;
        }
        if (blockId.equals(LastManStanding.ROWBOAT)) {
            Msg.game(player, "You row away from the island, forfeiting the game.");
            removePlayer(player, true);
            return true;
        }
        if (blockId.equals(LastManStanding.FOG_MARKER)) {
            Msg.game(player, "A fog marker: the safe zone closes in on " + safeZone.name() + ".");
            return true;
        }
        return false;
    }

    private static void give(ServerPlayerEntity p, List<Stack> loot) {
        for (Stack s : loot) {
            Inv.give(p, s.item(), s.count());
        }
    }

    private static String describe(List<Stack> loot) {
        List<String> names = new ArrayList<>();
        for (Stack s : loot) {
            ItemDef def = ItemCatalog.get(s.item());
            String name = def == null ? s.item() : def.name().replace(" (Last Man Standing)", "");
            names.add(s.count() > 1 ? s.count() + " x " + name : name);
        }
        return String.join(", ", names);
    }

    @Override
    protected void onDeath(ServerPlayerEntity player) {
        int place = playerCount();
        ServerPlayerEntity killer = killerOf(player);
        // The victim's loot (not their supplies) drops for the killer; their own items are safe.
        dropLoot(player);
        if (mode.ranked()) {
            award(player, place);
        }
        if (mode == Mode.HIGH_STAKES) {
            highStakes(player, place);
        }
        Msg.red(player, "You have been defeated! You finish in place " + place + ". Your items are returned to you.");
        removePlayer(player, true);
        if (killer != null && has(killer)) {
            kills.merge(killer.getUuid(), 1, Integer::sum);
            MinigameData.of(killer).incrementStat(killer.getUuid(), LastManStanding.KILLS);
            String key = LastManStanding.keyFor(playerCount());
            Inv.give(killer, key, 1);
            refill(killer);
            restoreStats(killer);
            immuneUntil.put(killer.getUuid(), ticks + LastManStanding.IMMUNITY_TICKS);
            Msg.game(killer, "You have defeated " + player.getName().getString() + "! You receive a "
                    + (key.equals(LastManStanding.BLOODIER_KEY) ? "bloodier" : "bloody") + " key and are safe from attack for 20 seconds.");
        }
        if (started && playerCount() > 1) {
            broadcast(player.getName().getString() + " has been defeated. " + playerCount() + " players remain.");
        }
    }

    private ServerPlayerEntity killerOf(ServerPlayerEntity victim) {
        LivingEntity attacker = victim.getAttacker();
        if (attacker instanceof ServerPlayerEntity p && has(p)) {
            return p;
        }
        UUID u = lastHitBy.get(victim.getUuid());
        return u == null ? null : world.getServer().getPlayerManager().getPlayer(u);
    }

    private void dropLoot(ServerPlayerEntity victim) {
        List<ItemStack> drops = new ArrayList<>();
        var inv = victim.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (!s.isEmpty() && !LastManStanding.isSupply(Inv.idOf(s))) {
                drops.add(s.copy());
            }
        }
        for (ItemStack s : SysState.of(victim).worn.values()) {
            if (!s.isEmpty()) {
                drops.add(s.copy());
            }
        }
        // Starting gear isn't worth dropping: only what was looted on the island.
        java.util.Set<String> starter = new java.util.HashSet<>(LastManStanding.loadout(build).worn());
        LastManStanding.loadout(build).carried().forEach(st -> starter.add(st.item()));
        for (ItemStack s : drops) {
            if (!starter.contains(Inv.idOf(s))) {
                world.spawnEntity(new ItemEntity(world, victim.getX(), victim.getY() + 0.5, victim.getZ(), s));
            }
        }
    }

    /** Competitive rewards for a placement: points (with at least one kill), rank and the win count. */
    private void award(ServerPlayerEntity p, int place) {
        MinigameData data = MinigameData.of(p);
        int k = kills.getOrDefault(p.getUuid(), 0);
        int pts = LastManStanding.points(place, k);
        if (pts > 0) {
            addPoints(p, LastManStanding.POINTS, pts);
        }
        int rank = data.stat(p.getUuid(), LastManStanding.RANK);
        if (rank == 0) {
            rank = LastManStanding.START_RANK;
        }
        rank = Math.max(1, rank + LastManStanding.rankChange(place));
        data.setStat(p.getUuid(), LastManStanding.RANK, rank);
        Msg.game(p, "You placed " + place + " with " + k + " kill" + (k == 1 ? "" : "s") + ": " + pts + " point"
                + (pts == 1 ? "" : "s") + ". Your rank is now " + rank + " (" + LastManStanding.rankTitle(rank) + ").");
    }

    /** High Stakes prizes go to the reward chest by Justine (7.5M first, 2.5M second); third to fifth play next time free. */
    private static void highStakes(ServerPlayerEntity p, int place) {
        MinigameData data = MinigameData.of(p);
        int prize = LastManStanding.highStakesPrize(place);
        if (prize > 0) {
            int held = data.stat(p.getUuid(), LastManStanding.HIGH_STAKES_CHEST);
            data.setStat(p.getUuid(), LastManStanding.HIGH_STAKES_CHEST, (int) Math.min(Integer.MAX_VALUE, (long) held + prize));
            Msg.game(p, String.format("%,d", prize) + " coins wait for you in the High Stakes reward chest; ask Justine for them.");
        } else if (LastManStanding.highStakesFreeEntry(place)) {
            data.incrementStat(p.getUuid(), LastManStanding.HIGH_STAKES_FREE);
            Msg.game(p, "Your next High Stakes game is free of charge.");
        }
    }

    @Override
    protected void onLeave(ServerPlayerEntity player) {
        // Death, forfeit (rowboat, logout, /minigame leave) or the end of the game: everything brought in comes back.
        immuneUntil.remove(player.getUuid());
        fogTicks.remove(player.getUuid());
        LmsStash.restore(player);
    }

    @Override
    protected void onEnd(boolean success) {
        if (!success || !started) {
            return;
        }
        List<ServerPlayerEntity> left = online();
        if (left.size() != 1) {
            return;
        }
        ServerPlayerEntity winner = left.get(0);
        MinigameData data = MinigameData.of(winner);
        data.incrementStat(winner.getUuid(), "kc:" + game.id());
        if (mode == Mode.HIGH_STAKES) {
            highStakes(winner, 1);
        }
        if (mode.ranked()) {
            award(winner, 1);
            int wins = data.incrementStat(winner.getUuid(), LastManStanding.WINS);
            Msg.game(winner, "You have won " + wins + " competitive game" + (wins == 1 ? "" : "s") + " of Last Man Standing.");
        } else {
            data.incrementStat(winner.getUuid(), LastManStanding.CASUAL_WINS);
        }
        winner.getServer().getPlayerManager().broadcast(net.minecraft.text.Text.literal(winner.getName().getString()
                + " is the last man standing!").formatted(net.minecraft.util.Formatting.GOLD), false);
    }

    @Override
    protected List<String> hud(ServerPlayerEntity player) {
        List<String> lines = new ArrayList<>();
        lines.add("Last Man Standing (" + mode.displayName().toLowerCase(java.util.Locale.ROOT) + ", " + build.displayName + ")");
        lines.add("Players left: " + playerCount() + "  Kills: " + kills.getOrDefault(player.getUuid(), 0));
        if (ticks < LastManStanding.START_TICKS) {
            lines.add("Starting in " + time(LastManStanding.START_TICKS - ticks));
        } else if (ticks < LastManStanding.FOG_WARNING_TICK) {
            lines.add("Fog in " + time(LastManStanding.FOG_TICK - ticks));
        } else {
            lines.add("Safe zone: " + safeZone.name() + " (" + LastManStanding.safeRadius(ticks) + " tiles)");
        }
        long immune = immuneUntil.getOrDefault(player.getUuid(), 0L) - ticks;
        if (immune > 0) {
            lines.add("Unattackable: " + time(immune));
        }
        lines.add("Time: " + time(ticks));
        return lines;
    }
}
