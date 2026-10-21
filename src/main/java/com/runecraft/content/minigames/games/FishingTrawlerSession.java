package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameRules.Trawler;
import com.runecraft.osrs.content.minigames.SkillingOutfits;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.Blocks;

/**
 * Fishing Trawler (OSRS Wiki; numbers in {@link Trawler}): keep Murphy's trawler afloat for five minutes. Leaks spring in
 * the hull (patch them with swamp paste from the Khazard General Store) and every open leak lets water in (bail it out at
 * the bucket); the net tears now and then (mend it with a rope, more often successfully with Crafting) and catches nothing
 * while torn; the kraken's enormous tentacle comes up every 45 seconds, slowing the boat until it is chopped off (an axe)
 * and slapping rails loose (fix them with a hammer; the deck box hands out hammers and bronze axes). Each job earns
 * contribution points; 50 earn a share of the catch (bigger with a fuller meter), rolled by Fishing level, with junk, a 1/12
 * chance of a missing angler piece (two sets in all, the second after the first) and the heron. The deck box's hammer and
 * bronze axe are ordinary items the player keeps (as from the Wintertodt's crates). If the water fills the hold the trawler
 * sinks and nobody gets anything.
 */
public class FishingTrawlerSession extends MinigameSession {
    private static final int[] LEAK_Z = {-12, -9, -6, -3, 0, 3, 6, 9};
    /** MinigameData stat prefix: how many of each angler piece the net has given the player (0-2). */
    private static final String ANGLER_KEY = "trawler_angler:";
    private static final int[] RAIL_Z = {-6, -2, 2, 6};
    private static final String[] AXES = {"dragon_axe", "rune_axe", "adamant_axe", "mithril_axe", "black_axe", "steel_axe", "iron_axe",
            "bronze_axe", "crystal_axe", "infernal_axe", "3rd_age_axe"};
    private final List<BlockPos> leaks = new ArrayList<>();
    private final Map<UUID, Integer> contribution = new HashMap<>();
    private final Set<BlockPos> brokenRails = new HashSet<>();
    private int water;
    private int startPlayers = 1;
    private boolean netTorn;
    private long netIntactTicks;
    private long tentacleTicks;
    /** The tentacle: side (-1 west, +1 east, 0 none), chops left and when it came up. */
    private int tentacleSide;
    private int tentacleHp;
    private long tentacleAt;
    private int nextSide = 1;

    public FishingTrawlerSession(int id, int slot, ServerLevel world, String variant) {
        super(Minigame.FISHING_TRAWLER, id, slot, world, variant);
    }

    @Override
    protected int halfX() {
        return 7;
    }

    @Override
    protected int halfZ() {
        return 16;
    }

    @Override
    protected int height() {
        return 7;
    }

    @Override
    protected void build() {
        room(Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.STRIPPED_SPRUCE_LOG);
        for (int side : new int[]{-7, 7}) {
            for (int z : RAIL_Z) {
                set(side, 1, z, Blocks.OAK_FENCE.defaultBlockState());
            }
        }
        place(0, -14, "trawler_bucket");
        place(3, -14, "trawler_box");
        place(0, 13, "trawler_net");
        place(0, 15, "minigame_exit");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
        return rel(index % 3 - 1, 1, 10);
    }

    @Override
    protected void start() {
        startPlayers = Math.max(1, online().size());
        broadcast("Murphy: All aboard! Patch the leaks with swamp paste, bail the water, mend the net and keep that kraken off!");
    }

    private int add(ServerPlayer p, int points) {
        return contribution.merge(p.getUUID(), points, (a, b) -> Math.min(Trawler.MAX_CONTRIBUTION, a + b));
    }

    // ------------------------------------------------------------------ events

    private BlockPos leakSpot(int side, int index) {
        return rel(side * 8, 2, LEAK_Z[index]);
    }

    /** Springs a leak in a random sound spot of the hull; false when every spot already leaks. */
    private boolean springLeak() {
        List<BlockPos> free = new ArrayList<>();
        for (int side : new int[]{-1, 1}) {
            for (int i = 0; i < LEAK_Z.length; i++) {
                BlockPos pos = leakSpot(side, i);
                if (!leaks.contains(pos)) {
                    free.add(pos);
                }
            }
        }
        if (free.isEmpty()) {
            return false;
        }
        BlockPos pos = free.get(random.nextInt(free.size()));
        world.setBlockAndUpdate(pos, com.runecraft.content.minigames.Arena.module("trawler_leak"));
        leaks.add(pos);
        broadcast("A leak springs in the hull!");
        return true;
    }

    private void tearNet() {
        if (!netTorn) {
            netTorn = true;
            broadcastRed("The net has torn! It won't catch anything until it's mended.");
        }
    }

    private void tentacle() {
        if (tentacleSide != 0) {
            return;
        }
        tentacleSide = nextSide;
        nextSide = -nextSide;
        tentacleHp = Trawler.TENTACLE_HP;
        tentacleAt = ticks;
        set(tentacleSide * 6, 1, 0, com.runecraft.content.minigames.Arena.module("trawler_tentacle"));
        // It slaps the rail once or twice as it comes up.
        int slaps = 1 + random.nextInt(2);
        for (int i = 0; i < slaps; i++) {
            BlockPos rail = rel(tentacleSide * 7, 1, RAIL_Z[random.nextInt(RAIL_Z.length)]);
            brokenRails.add(rail);
            world.setBlockAndUpdate(rail, com.runecraft.content.minigames.Arena.module("trawler_rail"));
        }
        broadcastRed("An enormous tentacle rises from the sea and slaps the " + (tentacleSide < 0 ? "west" : "east") + " rail!");
    }

    private void tentacleGone() {
        if (tentacleSide != 0) {
            set(tentacleSide * 6, 1, 0, Blocks.AIR.defaultBlockState());
            tentacleSide = 0;
        }
    }

    // ------------------------------------------------------------------ jobs

    private static double chance(int level, int low, int high) {
        int l = Math.max(1, Math.min(99, level));
        return (1 + Math.floor(low * (99 - l) / 98.0 + high * (l - 1) / 98.0 + 0.5)) / 256.0;
    }

    private boolean hasAxe(ServerPlayer p) {
        for (String axe : AXES) {
            if (Inv.has(p, axe, 1)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
        switch (blockId) {
            case "trawler_leak" -> {
                if (!leaks.contains(pos)) {
                    return true;
                }
                if (Inv.remove(player, "swamp_paste", 1) == 0) {
                    Msg.game(player, "You need swamp paste to patch the leak.");
                    return true;
                }
                leaks.remove(pos);
                world.setBlockAndUpdate(pos, Blocks.DARK_OAK_PLANKS.defaultBlockState());
                add(player, Trawler.LEAK_POINTS);
                xp(player, Skill.CONSTRUCTION, Trawler.ACTION_XP);
                Msg.action(player, "You patch the leak.");
            }
            case "trawler_bucket" -> {
                if (water <= 0) {
                    Msg.game(player, "There's no water to bail.");
                    return true;
                }
                int[] t = {0};
                repeat(player, p -> {
                    if (water <= 0 || isEnded()) {
                        return false;
                    }
                    if (++t[0] % 3 == 0) { // scoop and empty over the side (approx)
                        water = Math.max(0, water - 4);
                        add(p, Trawler.BAIL_POINTS);
                    }
                    return true;
                });
            }
            case "trawler_net" -> {
                if (!netTorn) {
                    Msg.game(player, "The net is in good condition.");
                    return true;
                }
                if (!Inv.has(player, "rope", 1)) {
                    Msg.game(player, "You need a rope to mend the net.");
                    return true;
                }
                if (random.nextDouble() >= chance(Players.data(player).skills().currentLevel(Skill.CRAFTING), 100, 240)) { // approx
                    Msg.game(player, "You fail to repair the net in the harsh conditions.");
                    return true;
                }
                Inv.remove(player, "rope", 1);
                netTorn = false;
                add(player, Trawler.NET_POINTS);
                xp(player, Skill.CRAFTING, Trawler.ACTION_XP);
                Msg.action(player, "You mend the net.");
            }
            case "trawler_tentacle" -> {
                if (tentacleSide == 0) {
                    return true;
                }
                if (!hasAxe(player)) {
                    Msg.game(player, "You need an axe to chop at the tentacle.");
                    return true;
                }
                if (random.nextDouble() >= chance(Players.data(player).skills().currentLevel(Skill.WOODCUTTING), 64, 200)) { // approx
                    Msg.action(player, "You swing at the tentacle and miss.");
                    return true;
                }
                add(player, Trawler.CHOP_POINTS);
                xp(player, Skill.WOODCUTTING, Trawler.ACTION_XP);
                if (--tentacleHp <= 0) {
                    tentacleGone();
                    Msg.game(player, "You chop at the tentacle and it slips back into the sea.");
                } else {
                    Msg.action(player, "You chop at the tentacle.");
                }
            }
            case "trawler_rail" -> {
                if (!brokenRails.contains(pos)) {
                    return true;
                }
                if (!Inv.has(player, "hammer", 1)) {
                    Msg.game(player, "You need a hammer to fix the rail.");
                    return true;
                }
                brokenRails.remove(pos);
                world.setBlockAndUpdate(pos, Blocks.OAK_FENCE.defaultBlockState());
                add(player, Trawler.RAIL_POINTS);
                xp(player, Skill.CONSTRUCTION, Trawler.ACTION_XP);
                Msg.action(player, "You fix the rail.");
            }
            case "trawler_box" -> {
                boolean gave = false;
                for (String tool : new String[]{"hammer", "bronze_axe"}) {
                    if (!Inv.has(player, tool, 1)) {
                        Inv.give(player, tool, 1);
                        gave = true;
                    }
                }
                Msg.game(player, gave ? "You take some tools from the box." : "You already have a hammer and an axe.");
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void gameTick() {
        if (ticks % Trawler.leakInterval(startPlayers) == 0) {
            springLeak();
        }
        if (ticks % Trawler.NET_TEAR_INTERVAL == Trawler.NET_TEAR_INTERVAL / 2) {
            tearNet();
        }
        if (ticks % Trawler.TENTACLE_INTERVAL == 0) {
            tentacle();
        } else if (tentacleSide != 0 && ticks - tentacleAt >= 40) { // approx: it sinks back by itself
            tentacleGone();
        }
        if (!netTorn) {
            netIntactTicks++;
        }
        if (tentacleSide != 0) {
            tentacleTicks++;
        }
        if (ticks % 3 == 0) {
            water = Math.min(Trawler.MAX_WATER, water + leaks.size()); // approx rate
        }
        if (water >= Trawler.MAX_WATER) {
            broadcastRed("The trawler sinks! You cling to a barrel and swim back to shore with nothing.");
            for (ServerPlayer p : online()) {
                hurt(p, 1 + random.nextInt(2));
            }
            end(false);
            return;
        }
        if (ticks >= Trawler.GAME_TICKS) {
            end(true);
        }
    }

    private static List<String> worn(ServerPlayer p) {
        List<String> ids = new ArrayList<>();
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ids.add(Inv.idOf(p.getItemBySlot(slot)));
        }
        return ids;
    }

    @Override
    protected void onEnd(boolean success) {
        double intact = ticks <= 0 ? 1 : netIntactTicks / (double) ticks;
        double slowed = ticks <= 0 ? 0 : tentacleTicks / (double) ticks;
        for (ServerPlayer p : online()) {
            if (!success) {
                continue;
            }
            int c = contribution.getOrDefault(p.getUUID(), 0);
            int fishing = level(p, Skill.FISHING);
            if (c < Trawler.MIN_CONTRIBUTION || fishing < Trawler.REWARD_LEVEL) {
                Msg.game(p, c < Trawler.MIN_CONTRIBUTION ? "Murphy: You barely lifted a finger! No share of the catch for you."
                        : "You need a Fishing level of " + Trawler.REWARD_LEVEL + " to take anything from the net.");
                continue;
            }
            int count = Trawler.fishCount(startPlayers, c, intact, slowed);
            Map<String, Integer> fish = new LinkedHashMap<>();
            double mult = SkillingOutfits.multiplier(SkillingOutfits.ANGLER, worn(p));
            double xp = 0;
            for (int i = 0; i < count; i++) {
                Trawler.Catch f = Trawler.fish(fishing, random::nextDouble);
                fish.merge(f.item(), 1, Integer::sum);
                xp += SkillingOutfits.boosted(f.xp(), mult);
            }
            int junk = Trawler.junkCount(random.nextDouble());
            for (int i = 0; i < junk; i++) {
                reward(p, Trawler.JUNK.get(random.nextInt(Trawler.JUNK.size())), 1);
                xp += Trawler.JUNK_XP;
            }
            fish.forEach((item, n) -> reward(p, item, n));
            xp(p, Skill.FISHING, xp);
            if (random.nextInt(Trawler.ANGLER_ROLL) == 0) {
                anglerPiece(p);
            }
            if (random.nextDouble() < Trawler.heronChance(c)) {
                reward(p, "pet_heron", 1);
                p.getServer().getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.literal(p.getName().getString()
                        + " has a funny feeling like they're being followed.").withStyle(net.minecraft.ChatFormatting.GOLD), false);
            }
            MinigameData.of(p).incrementStat(p.getUUID(), "kc:" + game.id());
            Msg.game(p, "The trawler returns to port. You search the net: " + count + " fish" + (junk > 0 ? " and some junk" : "") + ".");
        }
    }

    /**
     * A successful angler roll: a piece of the set being collected (two sets in all, the second once the first is complete;
     * OSRS Wiki "Fishing Trawler"), counted per piece in MinigameData; nothing once both sets are complete. Pieces received
     * before the count was kept are read from the collection log.
     */
    private String anglerPiece(ServerPlayer p) {
        MinigameData data = MinigameData.of(p);
        int[] received = new int[Trawler.OUTFIT.length];
        for (int i = 0; i < received.length; i++) {
            received[i] = Math.max(data.stat(p.getUUID(), ANGLER_KEY + Trawler.OUTFIT[i]),
                    Players.data(p).collectionLog().contains(Trawler.OUTFIT[i]) ? 1 : 0);
        }
        int i = Trawler.anglerPiece(received, random::nextInt);
        if (i < 0) {
            return null;
        }
        data.setStat(p.getUUID(), ANGLER_KEY + Trawler.OUTFIT[i], received[i] + 1);
        reward(p, Trawler.OUTFIT[i], 1);
        Msg.game(p, "The luck of the sea is in today. You spot a piece of fishing gear caught up in the netting!");
        return Trawler.OUTFIT[i];
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        int c = contribution.getOrDefault(player.getUUID(), 0);
        return List.of("Fishing Trawler", "Water: " + water + "%  Leaks: " + leaks.size(),
                "Net: " + (netTorn ? "TORN" : "fine") + (tentacleSide != 0 ? "  Tentacle " + (tentacleSide < 0 ? "west" : "east") : ""),
                "Contribution: " + c + " / " + Trawler.MAX_CONTRIBUTION + (c >= Trawler.MIN_CONTRIBUTION ? " (enough)" : ""),
                "Time left: " + time(Trawler.GAME_TICKS - ticks));
    }

    // ------------------------------------------------------------------ self-test hooks

    public record View(int water, List<BlockPos> leaks, boolean netTorn, int tentacleSide, int tentacleHp, List<BlockPos> brokenRails,
                       int contribution) {
    }

    public View selfTestView(ServerPlayer p) {
        return new View(water, List.copyOf(leaks), netTorn, tentacleSide, tentacleHp, List.copyOf(brokenRails),
                contribution.getOrDefault(p.getUUID(), 0));
    }

    /** Self-test: a successful angler roll for the player; the piece given, or null once both sets are complete. */
    public String selfTestAnglerRoll(ServerPlayer p) {
        return anglerPiece(p);
    }

    public boolean selfTestLeak() {
        return springLeak();
    }

    public void selfTestTearNet() {
        tearNet();
    }

    public void selfTestTentacle() {
        tentacle();
    }

    public BlockPos selfTestTentaclePos() {
        return rel(tentacleSide * 6, 1, 0);
    }

    public BlockPos selfTestBlock(String id) {
        return switch (id) {
            case "trawler_bucket" -> rel(0, 1, -14);
            case "trawler_box" -> rel(3, 1, -14);
            case "trawler_net" -> rel(0, 1, 13);
            default -> rel(0, 1, 15);
        };
    }
}
