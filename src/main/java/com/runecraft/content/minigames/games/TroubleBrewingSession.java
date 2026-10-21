package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameRules.TroubleBrewing;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

/**
 * Trouble Brewing on Mos Le'Harmless (OSRS Wiki; numbers in {@link TroubleBrewing}). Players come in with an empty inventory
 * and no hat ({@link #requirement}) and leave everything the game gave them behind. The workbench hands out the tools
 * (buckets, bowls, raw meat, a knife, a tinderbox and an axe); buckets are filled at the water pump; the team's flowers grow
 * by the far end and go into the kettle, which fills bowls with coloured water; a bowl of it dyes a monkey that fetches a
 * bitternut from the bitternut tree; raw meat on a sweetgrub mound draws out sweetgrubs (or sweetflies, or collapses it);
 * scrapey tree logs are chopped and their bark scraped off with a knife. Everything goes into the hopper. With an
 * ingredient set in the hopper and the three boilers lit with three logs each, a bottle of rum brews in 38 seconds and must
 * be packed in the crate by someone whose ingredients went into it before it breaks. After 20 minutes each player gets 10
 * pieces of eight per team bottle plus one per resource deposited (at most 100), and the rum reward XP for every
 * ingredient of theirs that was brewed. The team colour (red, from Fancy Dan's room) is cosmetic: there is no second team
 * to sabotage the brewery.
 */
public class TroubleBrewingSession extends MinigameSession {
    private static final String WATER = "minecraft:water_bucket";
    private static final String BUCKET = "minecraft:bucket";
    private static final String BOWL = "minecraft:bowl";
    private static final String MEAT = "minecraft:beef";
    private static final String COLOURED = "bowl_of_red_water";
    private static final String FLOWERS = "tb_red_flowers";
    private static final String[] SOLIDS = {"bitternut", "sweetgrubs", "scrapey_tree_bark"};
    private static final int[][] BOILERS = {{-4, 4}, {0, 4}, {4, 4}};
    private static final String[] TOOLS = {"knife", "tinderbox", "bronze_axe"};
    /** Game supplies, all taken back when a player leaves (they came in with an empty inventory). */
    private static final String[] SUPPLIES = {"bitternut", "sweetgrubs", "scrapey_tree_bark", "scrapey_tree_logs", COLOURED, FLOWERS, WATER,
            BUCKET, BOWL, MEAT, "logs"};
    /** Tools each player took from the workbench (a wielded axe of their own stays theirs). */
    private final Map<UUID, Map<String, Integer>> handedOut = new HashMap<>();
    /** Hopper contents: who deposited each unit, oldest first (bitternut, sweetgrubs, bark, water, coloured water). */
    private final Map<String, Deque<UUID>> hopper = new HashMap<>();
    private final Map<UUID, Integer> deposited = new HashMap<>();
    /** Bottles a player may still pack (their ingredients' share) and their rum reward credits, in thirds. */
    private final Map<UUID, Integer> collect = new HashMap<>();
    private final Map<UUID, Integer> rumThirds = new HashMap<>();
    private final int[] boilerLogs = new int[BOILERS.length];
    private final boolean[] lit = new boolean[BOILERS.length];
    private int logStore = TroubleBrewing.LOG_STORE;
    private int kettle;
    private long brewingUntil = -1;
    /** A finished bottle on the conveyor (since when), and who may pack it. */
    private long bottleAt = -1;
    private final java.util.Set<UUID> bottleOwners = new java.util.HashSet<>();
    private long cooldownUntil = -1;
    private long grubsBackAt = -1;
    private int teamBottles;

    public TroubleBrewingSession(int id, int slot, ServerLevel world, String variant) {
        super(Minigame.TROUBLE_BREWING, id, slot, world, variant);
        for (String k : new String[]{"bitternut", "sweetgrubs", "scrapey_tree_bark", WATER, COLOURED}) {
            hopper.put(k, new ArrayDeque<>());
        }
    }

    @Override
    protected void build() {
        room(Blocks.SAND, Blocks.SPRUCE_LOG, Blocks.SANDSTONE);
        place(0, 0, "tb_brewing_vat");
        for (int[] b : BOILERS) {
            place(b[0], b[1], "tb_boiler");
        }
        place(-8, 4, "tb_log_store");
        place(8, 4, "tb_workbench");
        place(-6, 0, "tb_water_pump");
        place(6, 0, "tb_kettle");
        place(0, -4, "tb_crate");
        place(-16, -12, "tb_bitternut_tree");
        place(16, -12, "tb_scrapey_tree");
        place(0, -18, "tb_sweetgrub_mound");
        place(0, 18, "tb_flowers");
        place(0, 22, "minigame_exit");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
        return rel(index % 5 - 2, 1, 12);
    }

    @Override
    protected void start() {
        broadcast("Fancy Dan: Welcome to the red team! Fill the hopper, keep the boilers lit and pack the rum!");
    }

    private void addContribution(ServerPlayer p, int n) {
        deposited.merge(p.getUUID(), n, Integer::sum);
    }

    private boolean boilersReady() {
        for (int i = 0; i < BOILERS.length; i++) {
            if (!lit[i] || boilerLogs[i] < TroubleBrewing.BOILER_MIN_LOGS) {
                return false;
            }
        }
        return true;
    }

    private boolean ingredientsReady() {
        for (String s : SOLIDS) {
            if (hopper.get(s).isEmpty()) {
                return false;
            }
        }
        return hopper.get(WATER).size() >= TroubleBrewing.WATER_PER_RUM && hopper.get(COLOURED).size() >= TroubleBrewing.COLOURED_PER_RUM;
    }

    // ------------------------------------------------------------------ gathering

    private void workbench(ServerPlayer p) {
        for (String tool : TOOLS) {
            if (!Inv.has(p, tool, 1)) {
                Inv.give(p, tool, 1);
                handedOut.computeIfAbsent(p.getUUID(), k -> new HashMap<>()).merge(tool, 1, Integer::sum);
            }
        }
        topUp(p, BUCKET, 5, WATER);
        topUp(p, BOWL, 3, COLOURED);
        topUp(p, MEAT, 3, null);
        Msg.game(p, "You take some tools and supplies from the workbench.");
    }

    private static void topUp(ServerPlayer p, String item, int to, String filled) {
        int have = Inv.count(p, item) + (filled == null ? 0 : Inv.count(p, filled));
        if (have < to) {
            Inv.give(p, item, to - have);
        }
    }

    private void bitternut(ServerPlayer p) {
        if (Inv.count(p, "bitternut") >= TroubleBrewing.MAX_NUTS) {
            Msg.game(p, "You can't carry any more bitternuts.");
            return;
        }
        if (Inv.remove(p, COLOURED, 1) == 0) {
            Msg.game(p, "You need a bowl of coloured water to dye a monkey for your team.");
            return;
        }
        Inv.give(p, BOWL, 1);
        xp(p, Skill.AGILITY, TroubleBrewing.MONKEY_XP);
        xp(p, Skill.HUNTER, TroubleBrewing.MONKEY_XP);
        Msg.action(p, "You catch a monkey, dye it and send it up the tree...");
        int[] t = {0};
        repeat(p, pl -> {
            if (isEnded()) {
                return false;
            }
            if (++t[0] < 5) { // approx: the monkey's climb
                return true;
            }
            Inv.give(pl, "bitternut", 1);
            xp(pl, Skill.HUNTER, TroubleBrewing.BITTERNUT_HUNTER_XP);
            Msg.action(pl, "The monkey throws you a bitternut.");
            return false;
        });
    }

    private void sweetgrubs(ServerPlayer p) {
        if (grubsBackAt > ticks) {
            Msg.game(p, "The mound has collapsed. It will take a while to fill again.");
            return;
        }
        if (Inv.remove(p, MEAT, 1) == 0) {
            Msg.game(p, "You need some raw meat to lure the sweetgrubs out.");
            return;
        }
        if (random.nextInt(TroubleBrewing.GRUB_ROLL) == 0) {
            grubsBackAt = ticks + 50; // approx
            Msg.game(p, "The mound collapses as the sweetgrubs flee.");
        } else if (random.nextInt(TroubleBrewing.GRUB_ROLL) == 0) {
            hurt(p, 1 + random.nextInt(2));
            Msg.red(p, "A swarm of sweetflies bursts out of the mound!");
        } else {
            Inv.give(p, "sweetgrubs", 1);
            xp(p, Skill.HUNTER, TroubleBrewing.SWEETGRUB_HUNTER_XP);
            Msg.action(p, "You lure out some sweetgrubs.");
        }
    }

    private void scrapeyTree(ServerPlayer p) {
        if (!Inv.has(p, "bronze_axe", 1) && !Inv.has(p, "rune_axe", 1) && !Inv.has(p, "dragon_axe", 1)) {
            Msg.game(p, "You need an axe to chop the scrapey tree.");
            return;
        }
        repeat(p, pl -> {
            if (isEnded() || !Inv.hasRoomFor(pl, "scrapey_tree_logs")) {
                return false;
            }
            if (random.nextInt(3) == 0) { // approx chop rate
                Inv.give(pl, "scrapey_tree_logs", 1);
            }
            return true;
        });
    }

    /** Scrapes the bark off every scrapey tree log carried (a knife on the logs). */
    public static boolean useKnife(ServerPlayer p, String item) {
        if (!(MinigameManager.session(p) instanceof TroubleBrewingSession tb) || !item.equals("knife")
                || !Inv.has(p, "scrapey_tree_logs", 1)) {
            return false;
        }
        tb.scrape(p);
        return true;
    }

    private void scrape(ServerPlayer p) {
        int n = Inv.remove(p, "scrapey_tree_logs", Inv.count(p, "scrapey_tree_logs"));
        for (int i = 0; i < n; i++) {
            Inv.give(p, "scrapey_tree_bark", 1);
            xp(p, Skill.FLETCHING, TroubleBrewing.BARK_FLETCHING_XP);
            if (random.nextInt(TroubleBrewing.DISEASE_ROLL) == 0) {
                hurt(p, TroubleBrewing.DISEASE_DAMAGE);
                Msg.red(p, "Something in the bark makes you feel ill.");
            }
        }
        Msg.action(p, "You scrape the bark off " + n + " log" + (n == 1 ? "" : "s") + ".");
    }

    // ------------------------------------------------------------------ the brewery

    private void deposit(ServerPlayer p) {
        int total = 0;
        boolean any = false;
        for (String item : new String[]{"bitternut", "sweetgrubs", "scrapey_tree_bark", COLOURED, WATER}) {
            int n = Inv.remove(p, item, Inv.count(p, item));
            if (n <= 0) {
                continue;
            }
            any = true;
            for (int i = 0; i < n; i++) {
                hopper.get(item).add(p.getUUID());
            }
            total += n;
            double xp = item.equals("bitternut") ? TroubleBrewing.BITTERNUT_XP : item.equals(WATER) ? 0 : TroubleBrewing.DEPOSIT_XP;
            xp(p, Skill.COOKING, n * xp);
            if (item.equals(WATER)) {
                Inv.give(p, BUCKET, n);
            } else if (item.equals(COLOURED)) {
                Inv.give(p, BOWL, n);
            }
        }
        if (!any) {
            Msg.game(p, "Bring bitternuts, sweetgrubs, scrapey bark, buckets of water or coloured water for the hopper.");
            return;
        }
        addContribution(p, total);
        cooldownUntil = -1; // a deposit starts the next bottle at once
        Msg.game(p, "You put " + total + " ingredient" + (total == 1 ? "" : "s") + " in the hopper.");
    }

    private void boiler(ServerPlayer p, int i) {
        int logs = Inv.count(p, "logs");
        if (logs > 0 && boilerLogs[i] < TroubleBrewing.BOILER_MAX_LOGS) {
            int n = Math.min(logs, TroubleBrewing.BOILER_MAX_LOGS - boilerLogs[i]);
            Inv.remove(p, "logs", n);
            boilerLogs[i] += n;
            cooldownUntil = -1;
            Msg.action(p, "You put " + n + " log" + (n == 1 ? "" : "s") + " in boiler " + (i + 1) + " (" + boilerLogs[i] + ").");
        }
        if (!lit[i] && boilerLogs[i] > 0) {
            if (!Inv.has(p, "tinderbox", 1)) {
                Msg.game(p, "You need a tinderbox to light the boiler.");
                return;
            }
            lit[i] = true;
            xp(p, Skill.FIREMAKING, TroubleBrewing.LIGHT_FIREMAKING_XP);
            addContribution(p, 1);
            set(BOILERS[i][0], 2, BOILERS[i][1], Blocks.CAMPFIRE.defaultBlockState());
            Msg.action(p, "You light boiler " + (i + 1) + ".");
        } else if (boilerLogs[i] == 0) {
            Msg.game(p, "The boiler needs logs from the log store.");
        }
    }

    private void crate(ServerPlayer p) {
        if (bottleAt < 0) {
            Msg.game(p, "There's no bottle of rum waiting to be packed.");
            return;
        }
        int left = collect.getOrDefault(p.getUUID(), 0);
        if (!bottleOwners.contains(p.getUUID()) || left <= 0) {
            Msg.game(p, "You didn't put anything into that bottle, so you can't pack it.");
            return;
        }
        collect.put(p.getUUID(), left - 1);
        bottleAt = -1;
        bottleOwners.clear();
        teamBottles++;
        broadcast(p.getName().getString() + " packs a bottle of rum. Team bottles: " + teamBottles + ".");
    }

    @Override
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
        switch (blockId) {
            case "tb_workbench" -> workbench(player);
            case "tb_water_pump" -> {
                int n = Inv.remove(player, BUCKET, Inv.count(player, BUCKET));
                if (n == 0) {
                    Msg.game(player, "You need an empty bucket.");
                } else {
                    Inv.give(player, WATER, n);
                    Msg.action(player, "You fill " + n + " bucket" + (n == 1 ? "" : "s") + " at the pump.");
                }
            }
            case "tb_flowers" -> {
                int n = Math.min(TroubleBrewing.MAX_FLOWERS - Inv.count(player, FLOWERS), TroubleBrewing.MAX_FLOWERS);
                if (n <= 0) {
                    Msg.game(player, "You can't carry any more flowers.");
                } else {
                    Inv.give(player, FLOWERS, n);
                    Msg.action(player, "You pick some red flowers.");
                }
            }
            case "tb_kettle" -> {
                int put = Math.min(Inv.count(player, FLOWERS), TroubleBrewing.KETTLE_MAX - kettle);
                if (put > 0) {
                    Inv.remove(player, FLOWERS, put);
                    kettle += put;
                }
                int bowls = Math.min(kettle, Inv.count(player, BOWL));
                if (bowls > 0) {
                    Inv.remove(player, BOWL, bowls);
                    Inv.give(player, COLOURED, bowls);
                    kettle -= bowls;
                }
                Msg.action(player, put + bowls == 0 ? "The kettle needs flowers, and you need empty bowls."
                        : "The kettle holds " + kettle + " flowers; you fill " + bowls + " bowl" + (bowls == 1 ? "" : "s") + ".");
            }
            case "tb_bitternut_tree" -> bitternut(player);
            case "tb_sweetgrub_mound" -> sweetgrubs(player);
            case "tb_scrapey_tree" -> {
                if (Inv.has(player, "scrapey_tree_logs", 1) && Inv.has(player, "knife", 1) && player.isShiftKeyDown()) {
                    scrape(player);
                } else {
                    scrapeyTree(player);
                }
            }
            case "tb_log_store" -> {
                int n = Math.min(logStore, 10);
                if (n <= 0 || !Inv.hasRoomFor(player, "logs")) {
                    Msg.game(player, n <= 0 ? "The log store is empty." : "You have no room for logs.");
                } else {
                    logStore -= n;
                    Inv.give(player, "logs", n);
                    Msg.action(player, "You take " + n + " logs (" + logStore + " left).");
                }
            }
            case "tb_boiler" -> {
                BlockPos r = pos.subtract(origin);
                for (int i = 0; i < BOILERS.length; i++) {
                    if (BOILERS[i][0] == r.getX() && BOILERS[i][1] == r.getZ()) {
                        boiler(player, i);
                    }
                }
            }
            case "tb_brewing_vat" -> deposit(player);
            case "tb_crate" -> crate(player);
            default -> {
                return false;
            }
        }
        return true;
    }

    private UUID take(String item) {
        return hopper.get(item).poll();
    }

    @Override
    protected void gameTick() {
        for (int i = 0; i < BOILERS.length; i++) {
            if (lit[i] && ticks % TroubleBrewing.LOG_BURN_TICKS == 0 && --boilerLogs[i] <= 0) {
                boilerLogs[i] = 0;
                lit[i] = false;
                set(BOILERS[i][0], 2, BOILERS[i][1], Blocks.AIR.defaultBlockState());
                broadcastRed("Boiler " + (i + 1) + " has gone out!");
            }
        }
        if (bottleAt >= 0 && ticks - bottleAt >= TroubleBrewing.BOTTLE_BREAK_TICKS) {
            bottleAt = -1;
            bottleOwners.clear();
            broadcastRed("Nobody packed the bottle of rum and it falls off the conveyor and breaks!");
        }
        if (brewingUntil < 0 && bottleAt < 0 && ticks >= cooldownUntil && boilersReady() && ingredientsReady()) {
            java.util.Set<UUID> owners = new java.util.HashSet<>();
            for (String s : SOLIDS) {
                UUID u = take(s);
                owners.add(u);
                rumThirds.merge(u, TroubleBrewing.rumThirds(s), Integer::sum);
                collect.merge(u, 1, Integer::sum);
            }
            for (int i = 0; i < TroubleBrewing.COLOURED_PER_RUM; i++) {
                UUID u = take(COLOURED);
                owners.add(u);
                rumThirds.merge(u, TroubleBrewing.rumThirds(COLOURED), Integer::sum);
            }
            for (int i = 0; i < TroubleBrewing.WATER_PER_RUM; i++) {
                owners.add(take(WATER));
            }
            owners.remove(null);
            bottleOwners.clear();
            bottleOwners.addAll(owners);
            for (UUID u : owners) {
                collect.putIfAbsent(u, 0);
                if (collect.get(u) == 0) {
                    collect.put(u, 1); // water and coloured water also earn the right to pack this bottle
                }
            }
            brewingUntil = ticks + TroubleBrewing.BREW_TICKS;
        }
        if (brewingUntil >= 0 && ticks >= brewingUntil) {
            brewingUntil = -1;
            bottleAt = ticks;
            cooldownUntil = ticks + TroubleBrewing.BOTTLE_BREAK_TICKS;
            broadcast("A bottle of rum rolls off the line! Pack it in the crate before it breaks.");
        }
        if (ticks >= TroubleBrewing.GAME_TICKS) {
            end(true);
        }
    }

    /**
     * Entry check (OSRS Wiki "Trouble Brewing", Joining a game: "you cannot have any followers, items in your inventory, or
     * anything equipped in the head slot"), so that everything taken back on leaving was the game's: a wielded weapon (an axe
     * for the scrapey trees) may come along.
     */
    public static String requirement(ServerPlayer p) {
        net.minecraft.world.entity.player.Inventory inv = p.getInventory();
        for (int i = 0; i < inv.items.size(); i++) {
            net.minecraft.world.item.ItemStack st = inv.items.get(i);
            if (!st.isEmpty() && !(i == inv.selected && wielded(st))) {
                return "Honest Jimmy: Ye can't take anything into the brewery, matey. Bank yer inventory first.";
            }
        }
        if (!p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty()) {
            return "Honest Jimmy: Take off yer hat, matey. Brewers wear their team's bandana.";
        }
        return null;
    }

    private static boolean wielded(net.minecraft.world.item.ItemStack st) {
        com.runecraft.osrs.ItemDef def = com.runecraft.registry.ModItems.def(st);
        return def != null && def.equipment() != null && def.equipment().slot() == com.runecraft.osrs.EquipmentDef.Slot.WEAPON
                || st.getItem() instanceof net.minecraft.world.item.TieredItem;
    }

    /** Workbench items can't leave the minigame (OSRS Wiki "Workbench (Trouble Brewing, tools)"). */
    @Override
    protected void onLeave(ServerPlayer player) {
        for (String item : SUPPLIES) {
            Inv.remove(player, item, Inv.count(player, item));
        }
        Map<String, Integer> tools = handedOut.remove(player.getUUID());
        if (tools != null) {
            tools.forEach((tool, n) -> Inv.remove(player, tool, Math.min(n, Inv.count(player, tool))));
        }
    }

    @Override
    protected void onEnd(boolean success) {
        for (ServerPlayer p : online()) {
            int resources = deposited.getOrDefault(p.getUUID(), 0);
            if (resources == 0) {
                Msg.game(p, "You didn't contribute anything this game.");
                continue;
            }
            int eight = TroubleBrewing.piecesOfEight(teamBottles, resources);
            reward(p, "pieces_of_eight", eight);
            int thirds = rumThirds.getOrDefault(p.getUUID(), 0);
            if (thirds > 0) {
                xp(p, Skill.CRAFTING, thirds * TroubleBrewing.RUM_CRAFTING_XP / 3);
                xp(p, Skill.COOKING, thirds * TroubleBrewing.RUM_COOKING_XP / 3);
                xp(p, Skill.FIREMAKING, thirds * TroubleBrewing.RUM_FIREMAKING_XP / 3);
            }
            MinigameData.of(p).incrementStat(p.getUUID(), "kc:" + game.id());
            Msg.game(p, "The game is over! Your team packed " + teamBottles + " bottle" + (teamBottles == 1 ? "" : "s")
                    + " of rum and you earn " + eight + " pieces of eight.");
        }
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        StringBuilder boilers = new StringBuilder();
        for (int i = 0; i < BOILERS.length; i++) {
            boilers.append(i == 0 ? "" : " ").append(lit[i] ? boilerLogs[i] : "x");
        }
        return List.of("Trouble Brewing (red team)", "Team bottles: " + teamBottles + (brewingUntil >= 0 ? " (brewing)" : "")
                        + (bottleAt >= 0 ? " - PACK THE RUM!" : ""),
                "Hopper: " + hopper.get("bitternut").size() + " nuts, " + hopper.get("sweetgrubs").size() + " grubs, "
                        + hopper.get("scrapey_tree_bark").size() + " bark, " + hopper.get(WATER).size() + " water, "
                        + hopper.get(COLOURED).size() + " red",
                "Boilers: " + boilers + "  Logs in store: " + logStore,
                "Your contribution: " + Math.min(TroubleBrewing.MAX_CONTRIBUTION, deposited.getOrDefault(player.getUUID(), 0)),
                "Time left: " + time(TroubleBrewing.GAME_TICKS - ticks));
    }

    // ------------------------------------------------------------------ self-test hooks

    public record View(int teamBottles, boolean brewing, boolean bottleWaiting, int contribution, int rumThirds, int litBoilers,
                       int hopperNuts, int hopperWater, int hopperColoured, int kettle) {
    }

    public View selfTestView(ServerPlayer p) {
        int litCount = 0;
        for (int i = 0; i < BOILERS.length; i++) {
            litCount += lit[i] && boilerLogs[i] >= TroubleBrewing.BOILER_MIN_LOGS ? 1 : 0;
        }
        return new View(teamBottles, brewingUntil >= 0, bottleAt >= 0, deposited.getOrDefault(p.getUUID(), 0),
                rumThirds.getOrDefault(p.getUUID(), 0), litCount, hopper.get("bitternut").size(), hopper.get(WATER).size(),
                hopper.get(COLOURED).size(), kettle);
    }

    public BlockPos selfTestBlock(String id) {
        return switch (id) {
            case "tb_brewing_vat" -> rel(0, 1, 0);
            case "tb_log_store" -> rel(-8, 1, 4);
            case "tb_workbench" -> rel(8, 1, 4);
            case "tb_water_pump" -> rel(-6, 1, 0);
            case "tb_kettle" -> rel(6, 1, 0);
            case "tb_crate" -> rel(0, 1, -4);
            case "tb_bitternut_tree" -> rel(-16, 1, -12);
            case "tb_scrapey_tree" -> rel(16, 1, -12);
            case "tb_sweetgrub_mound" -> rel(0, 1, -18);
            case "tb_flowers" -> rel(0, 1, 18);
            default -> rel(0, 1, 22);
        };
    }

    public BlockPos selfTestBoiler(int i) {
        return rel(BOILERS[i][0], 1, BOILERS[i][1]);
    }

    /** Self-test: runs the clock to the last ticks of the game. */
    public void selfTestNearEnd() {
        ticks = Math.max(ticks, TroubleBrewing.GAME_TICKS - 5);
    }
}
