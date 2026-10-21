package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.content.minigames.PlankSack;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.MahoganyHomesLayouts;
import com.runecraft.osrs.content.minigames.MahoganyHomesLayouts.Hotspot;
import com.runecraft.osrs.content.minigames.MahoganyHomesLayouts.Layout;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameCurrencies;
import com.runecraft.osrs.content.minigames.MinigameRules.MahoganyHomes;
import com.runecraft.osrs.content.minigames.SkillingOutfits;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Mahogany Homes (OSRS Wiki; layouts in {@link MahoganyHomesLayouts}). The contractor's desk hands out a contract of the
 * best tier the player's (boostable) Construction level allows, or a lower one when sneak-clicked: Beginner (1, planks),
 * Novice (20, oak), Adept (50, teak) or Expert (70, mahogany). The contract is a random homeowner of the four cities and
 * one of their fixed layouts for that tier; the house is built with its hotspots. Old furniture is removed and rebuilt with
 * planks, broken furniture repaired with planks or a steel bar (a hammer and, where the game has one, a saw or Amy's saw);
 * planks come from the inventory, then the plank sack. Talking to the homeowner when everything is done pays the
 * completion XP and the tier's carpenter points; the carpenter's outfit adds up to 2.5% to the contract's XP.
 */
public class MahoganyHomesSession extends MinigameSession {
    private static final int[][] SLOTS = {{-8, -8}, {-3, -8}, {3, -8}, {8, -8}, {-8, -2}, {8, -2}, {-8, 4}, {8, 4}};
    private static final int[] HOMEOWNER = {0, -4};
    private static final int[] CONTRACTOR = {-5, 9};
    /** Per slot: 0 done (or no hotspot), 1 needs a repair, 2 needs building, 3 old furniture to remove. */
    private final int[] state = new int[SLOTS.length];
    private Layout layout;
    private int chosenTier = -1;
    private int contracts;

    public MahoganyHomesSession(int id, int slot, ServerLevel world, String variant) {
        super(Minigame.MAHOGANY_HOMES, id, slot, world, variant);
    }

    @Override
    protected int halfX() {
        return 12;
    }

    @Override
    protected int halfZ() {
        return 12;
    }

    private static final Block[] FLOORS = {Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.BIRCH_PLANKS, Blocks.DARK_OAK_PLANKS};

    @Override
    protected void build() {
        int city = layout == null ? 0 : List.of("Ardougne", "Falador", "Hosidius", "Varrock").indexOf(layout.region());
        room(FLOORS[Math.max(0, city)], Blocks.BRICKS, Blocks.WHITE_WOOL);
        for (int k = 0; k < SLOTS.length; k++) {
            Hotspot h = hotspot(k);
            if (h == null) {
                set(SLOTS[k][0], 1, SLOTS[k][1], Blocks.BOOKSHELF.defaultBlockState()); // furniture that needs nothing
                set(SLOTS[k][0], 2, SLOTS[k][1], Blocks.AIR.defaultBlockState());
            } else {
                place(SLOTS[k][0], SLOTS[k][1], "mh_hotspot");
                show(k);
            }
        }
        place(HOMEOWNER[0], HOMEOWNER[1], "mh_homeowner");
        place(CONTRACTOR[0], CONTRACTOR[1], "mh_contractor");
        place(0, 11, "minigame_exit");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
        return rel(index % 3 - 1, 1, 9);
    }

    @Override
    protected void start() {
        broadcast("Amy: Welcome to Mahogany Homes! Take a contract at the desk (sneak-click for an easier tier).");
    }

    private Hotspot hotspot(int slot) {
        if (layout == null) {
            return null;
        }
        for (Hotspot h : layout.hotspots()) {
            if (h.slot() == slot) {
                return h;
            }
        }
        return null;
    }

    /** What stands on a hotspot: the old furniture, nothing (ready to build), something broken, or the finished piece. */
    private void show(int k) {
        Block b = switch (state[k]) {
            case 3 -> Blocks.COMPOSTER;
            case 1 -> Blocks.CRACKED_STONE_BRICKS;
            case 2 -> Blocks.AIR;
            default -> layout != null && layout.tier() >= 2 ? Blocks.BOOKSHELF : Blocks.CRAFTING_TABLE;
        };
        set(SLOTS[k][0], 2, SLOTS[k][1], b.defaultBlockState());
    }

    private int bestTier(ServerPlayer p) {
        return MahoganyHomes.tierFor(Players.data(p).skills().currentLevel(Skill.CONSTRUCTION));
    }

    private void newContract(ServerPlayer p) {
        // A click repeats the last tier taken ("Last-tier contract"; the best available at first); a sneak-click steps one
        // tier down, from Beginner back to the best.
        int best = bestTier(p);
        int last = chosenTier < 0 ? best : Math.min(chosenTier, best);
        int tier = p.isShiftKeyDown() ? (last == 0 ? best : last - 1) : last;
        chosenTier = tier;
        List<String> owners = MahoganyHomesLayouts.owners();
        String owner = owners.get(random.nextInt(owners.size()));
        List<Layout> options = MahoganyHomesLayouts.layouts(tier, owner);
        layout = options.get(random.nextInt(options.size()));
        for (int k = 0; k < SLOTS.length; k++) {
            Hotspot h = hotspot(k);
            state[k] = h == null ? 0 : h.repair() ? 1 : 3;
        }
        build();
        Msg.game(p, "Amy: " + owner + " in " + layout.region() + " needs some work done: a " + MahoganyHomes.TIERS[tier]
                + " contract. You'll need about " + layout.planks() + " " + plankName() + "s"
                + (layout.bars() > 0 ? " and " + layout.bars() + " steel bar" : "") + ".");
    }

    private String plankName() {
        return ItemCatalog.get(MahoganyHomes.PLANKS[layout.tier()]).name().toLowerCase(java.util.Locale.ROOT);
    }

    private boolean done() {
        for (int s : state) {
            if (s != 0) {
                return false;
            }
        }
        return true;
    }

    private static List<String> worn(ServerPlayer p) {
        List<String> ids = new ArrayList<>();
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ids.add(Inv.idOf(p.getItemBySlot(slot)));
        }
        return ids;
    }

    private static boolean hasTools(ServerPlayer p) {
        boolean saw = ItemCatalog.get("saw") == null || Inv.has(p, "saw", 1) || Inv.has(p, "amys_saw", 1);
        return saw && Inv.has(p, "hammer", 1);
    }

    /** Planks of the contract's type carried in the inventory and the plank sack. */
    private int planks(ServerPlayer p) {
        String plank = MahoganyHomes.PLANKS[layout.tier()];
        return Inv.count(p, plank) + PlankSack.count(p, plank);
    }

    private void usePlanks(ServerPlayer p, int n) {
        String plank = MahoganyHomes.PLANKS[layout.tier()];
        int fromInv = Inv.remove(p, plank, Math.min(n, Inv.count(p, plank)));
        PlankSack.take(p, plank, n - fromInv);
    }

    private void work(ServerPlayer player, int k) {
        Hotspot h = hotspot(k);
        if (h == null || state[k] == 0) {
            Msg.game(player, "This furniture is in perfect condition.");
            return;
        }
        if (state[k] == 3) {
            state[k] = 2;
            show(k);
            Msg.action(player, "You remove the old " + h.name().toLowerCase(java.util.Locale.ROOT) + ".");
            return;
        }
        if (!hasTools(player)) {
            Msg.game(player, "You need a hammer" + (ItemCatalog.get("saw") == null ? "" : " and a saw") + " to do that.");
            return;
        }
        if (h.steelBar()) {
            if (!Inv.has(player, "steel_bar", h.count())) {
                Msg.game(player, "You need a steel bar to repair the " + h.name().toLowerCase(java.util.Locale.ROOT) + ".");
                return;
            }
            Inv.remove(player, "steel_bar", h.count());
        } else {
            if (planks(player) < h.count()) {
                Msg.game(player, "You need " + h.count() + " " + plankName() + (h.count() == 1 ? "" : "s") + " to "
                        + (h.repair() ? "repair" : "build") + " the " + h.name().toLowerCase(java.util.Locale.ROOT) + ".");
                return;
            }
            usePlanks(player, h.count());
        }
        double xp = h.count() * MahoganyHomes.hotspotXp(layout.tier(), h.steelBar(), h.repair());
        outfitXp(player, Skill.CONSTRUCTION, SkillingOutfits.boosted(xp, SkillingOutfits.multiplier(SkillingOutfits.CARPENTER, worn(player))));
        state[k] = 0;
        show(k);
        Msg.action(player, "You " + (h.repair() ? "repair" : "build") + " the " + h.name().toLowerCase(java.util.Locale.ROOT) + ".");
        if (done()) {
            Msg.game(player, "That's everything. Speak to " + layout.owner() + " to finish the contract.");
        }
    }

    private void homeowner(ServerPlayer player) {
        if (layout == null) {
            Msg.game(player, "Thanks again! Take a new contract at the desk.");
            return;
        }
        if (!done()) {
            List<String> left = new ArrayList<>();
            for (int k = 0; k < SLOTS.length; k++) {
                if (state[k] != 0) {
                    left.add(hotspot(k).name());
                }
            }
            Msg.game(player, layout.owner() + ": Still to do: " + String.join(", ", left) + ".");
            return;
        }
        int tier = layout.tier();
        double xp = SkillingOutfits.boosted(MahoganyHomes.COMPLETION_XP[tier],
                SkillingOutfits.multiplier(SkillingOutfits.CARPENTER, worn(player)));
        outfitXp(player, Skill.CONSTRUCTION, xp);
        addPoints(player, MinigameCurrencies.CARPENTER, MahoganyHomes.POINTS[tier]);
        contracts++;
        int total = MinigameData.of(player).incrementStat(player.getUUID(), "kc:" + game.id());
        Msg.game(player, layout.owner() + ": Lovely, thank you! Have a cup of tea. (" + MahoganyHomes.POINTS[tier]
                + " carpenter points; contracts completed: " + total + ")");
        layout = null;
    }

    @Override
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
        switch (blockId) {
            case "mh_hotspot" -> {
                BlockPos r = pos.subtract(origin);
                for (int k = 0; k < SLOTS.length; k++) {
                    if (SLOTS[k][0] == r.getX() && SLOTS[k][1] == r.getZ()) {
                        work(player, k);
                        return true;
                    }
                }
                Msg.game(player, "This furniture is in perfect condition.");
            }
            case "mh_homeowner" -> homeowner(player);
            case "mh_contractor" -> {
                if (layout != null) {
                    Msg.game(player, "Amy: You still have a contract with " + layout.owner() + " in " + layout.region() + ".");
                } else {
                    newContract(player);
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void gameTick() {
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        List<String> lines = new ArrayList<>();
        lines.add("Mahogany Homes - " + MinigameData.of(player).points(player.getUUID(), "mahogany_homes") + " carpenter points");
        if (layout == null) {
            lines.add("Take a contract at the desk");
            return lines;
        }
        lines.add(MahoganyHomes.TIERS[layout.tier()] + ": " + layout.owner() + ", " + layout.region());
        int left = 0;
        for (int s : state) {
            left += s != 0 ? 1 : 0;
        }
        lines.add(left == 0 ? "Done: speak to " + layout.owner() : left + " furniture to fix (" + planks(player) + " " + plankName()
                + "s carried)");
        lines.add("Contracts this visit: " + contracts);
        return lines;
    }

    // ------------------------------------------------------------------ self-test hooks

    public Layout selfTestLayout() {
        return layout;
    }

    public int selfTestState(int slot) {
        return state[slot];
    }

    public BlockPos selfTestSlot(int slot) {
        return rel(SLOTS[slot][0], 1, SLOTS[slot][1]);
    }

    public BlockPos selfTestHomeowner() {
        return rel(HOMEOWNER[0], 1, HOMEOWNER[1]);
    }

    public BlockPos selfTestContractor() {
        return rel(CONTRACTOR[0], 1, CONTRACTOR[1]);
    }
}
