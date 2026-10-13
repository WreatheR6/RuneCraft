package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameItemUse;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.GuardiansRewards;
import com.runecraft.osrs.content.minigames.GuardiansRift;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameRules.Guardians;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Guardians of the Rift: mine guardian fragments from the huge remains, shape them into guardian
 * essence at the workbench, then enter the active elemental or catalytic guardian to craft runes
 * (the raiments of the eye add up to 60% more). Each essence imbued gives a guardian stone; each
 * stone placed in the Great Guardian gives 2 energy of its type and powers him (250 stones per player
 * to reach 100%). With 300+ energy, every 100 energy of a type is a reward point, and each Rewards
 * Guardian search costs one elemental and one catalytic point.
 *
 * <p>Defence ({@link GuardiansRift}): uncharged cells from the cell table are charged at the active altar;
 * a charged cell on a cell tile raises, strengthens or recharges a barrier, or with a chisel at an essence
 * pile becomes a rift guardian. After two minutes the rift's creatures wear the barriers down and then the
 * Great Guardian; the game is lost if his health reaches 0.
 */
public class GuardiansSession extends MinigameSession {
    private static final int GAME_TICKS = 1000;
    private static final int[] TILE_X = {-6, -2, 2, 6};
    private final Map<UUID, int[]> energy = new HashMap<>();
    /** Recharging done per player this game: Runecraft XP, energy (both capped). */
    private final Map<UUID, double[]> recharged = new HashMap<>();
    private final GuardiansRift rift = new GuardiansRift();
    private double power;
    private int elemental;
    private int catalytic;

    public GuardiansSession(int id, int slot, ServerWorld world, String variant) {
        super(Minigame.GUARDIANS_OF_THE_RIFT, id, slot, world, variant);
    }

    @Override
    protected int height() {
        return 14;
    }

    @Override
    protected void build() {
        room(Blocks.PURPUR_BLOCK, Blocks.OBSIDIAN, Blocks.AMETHYST_BLOCK);
        place(-18, -6, "guardian_remains");
        place(-18, 0, "guardian_remains");
        place(-18, 6, "guardian_remains");
        place(-8, 10, "gotr_workbench");
        place(12, -12, "elemental_rift");
        place(-12, -12, "catalytic_rift");
        place(0, -6, "gotr_great_guardian");
        for (int x : TILE_X) {
            place(x, -2, "gotr_cell_tile");
        }
        place(8, 10, "gotr_cell_table");
        place(14, 6, "gotr_elemental_essence");
        place(-12, 6, "gotr_catalytic_essence");
        place(0, 20, "minigame_exit");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayerEntity player, int index) {
        return rel(index % 5 - 2, 1, 16);
    }

    @Override
    protected void start() {
        MobEntity g = spawn("great_guardian", rel(0, 1, -10));
        if (g != null) {
            g.setAiDisabled(true);
        }
        rotateAltars();
        broadcastRed("The rift becomes active! Power the Great Guardian to seal it. Creatures come through in two minutes:"
                + " raise barriers with charged cells.");
    }

    private void rotateAltars() {
        List<Integer> el = new ArrayList<>();
        List<Integer> cat = new ArrayList<>();
        int minRc = 99;
        for (ServerPlayerEntity p : online()) {
            minRc = Math.min(minRc, level(p, Skill.RUNECRAFT));
        }
        for (int i = 0; i < Guardians.ALTARS.length; i++) {
            if ((Integer) Guardians.ALTARS[i][2] <= Math.max(1, minRc)) {
                ((Boolean) Guardians.ALTARS[i][4] ? cat : el).add(i);
            }
        }
        elemental = el.isEmpty() ? 0 : el.get(random.nextInt(el.size()));
        catalytic = cat.isEmpty() ? 4 : cat.get(random.nextInt(cat.size()));
        broadcast("The active guardians are now " + name(elemental) + " and " + name(catalytic) + ".");
    }

    private static String name(int altar) {
        String id = (String) Guardians.ALTARS[altar][0];
        return Character.toUpperCase(id.charAt(0)) + id.substring(1);
    }

    private int[] energy(ServerPlayerEntity p) {
        return energy.computeIfAbsent(p.getUuid(), k -> new int[2]);
    }

    /** Adds energy within the per-game caps, telling the player once they are fully attuned. */
    private void gain(ServerPlayerEntity p, int elemental, int catalytic) {
        if (!GuardiansRewards.addEnergy(energy(p), elemental, catalytic)) {
            Msg.game(p, "You are fully attuned with runic energy and cannot earn anymore until the rift has been closed.");
        }
    }

    private int tileIndex(BlockPos pos) {
        int dx = pos.getX() - origin.getX();
        int best = 0;
        for (int i = 1; i < TILE_X.length; i++) {
            if (Math.abs(TILE_X[i] - dx) < Math.abs(TILE_X[best] - dx)) {
                best = i;
            }
        }
        return best;
    }

    /** The charged cell the player carries, if any. */
    private static GuardiansRift.Cell chargedCell(ServerPlayerEntity p) {
        for (GuardiansRift.Cell c : GuardiansRift.Cell.values()) {
            if (Inv.has(p, c.item, 1)) {
                return c;
            }
        }
        return null;
    }

    private static int raiments(ServerPlayerEntity p) {
        int n = 0;
        String[] pieces = {"hat_of_the_eye", "robe_top_of_the_eye", "robe_bottoms_of_the_eye", "boots_of_the_eye"};
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < 4; i++) {
            if (Inv.idOf(p.getEquippedStack(slots[i])).equals(pieces[i])) {
                n++;
            }
        }
        return n;
    }

    @Override
    protected boolean onBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
        switch (blockId) {
            case "guardian_remains" -> {
                if (!hasPickaxe(player)) {
                    Msg.game(player, "You need a pickaxe to mine the remains.");
                    return true;
                }
                repeat(player, p -> {
                    if (!Inv.hasRoomFor(p, "guardian_fragments")) {
                        return false;
                    }
                    int mining = level(p, Skill.MINING);
                    if (random.nextInt(100) < 40 + mining / 2) {
                        Inv.give(p, "guardian_fragments", 1 + (mining >= 70 && random.nextBoolean() ? 1 : 0));
                        xp(p, Skill.MINING, mining * 0.15);
                    }
                    return true;
                });
                return true;
            }
            case "gotr_workbench" -> {
                repeat(player, p -> {
                    int n = Math.min(3, Inv.count(p, "guardian_fragments"));
                    if (n == 0) {
                        return false;
                    }
                    Inv.remove(p, "guardian_fragments", n);
                    Inv.give(p, "guardian_essence", n);
                    xp(p, Skill.CRAFTING, 0.5 * n);
                    return true;
                });
                return true;
            }
            case "gotr_cell_table" -> {
                int have = Inv.count(player, GuardiansRift.UNCHARGED_CELL);
                int take = GuardiansRift.MAX_UNCHARGED_CELLS - have;
                if (take <= 0) {
                    Msg.game(player, "You can't carry any more uncharged cells.");
                } else {
                    Inv.give(player, GuardiansRift.UNCHARGED_CELL, take);
                    Msg.game(player, "You take " + take + " uncharged cell" + (take == 1 ? "." : "s."));
                }
                return true;
            }
            case "gotr_cell_tile" -> {
                int tile = tileIndex(pos);
                GuardiansRift.Cell cell = chargedCell(player);
                GuardiansRift.Use use;
                if (rift.tier(tile) == -2) {
                    if (Inv.count(player, "guardian_fragments") < GuardiansRift.REPAIR_FRAGMENTS) {
                        Msg.game(player, "You need " + GuardiansRift.REPAIR_FRAGMENTS + " guardian fragments to repair the cell tile.");
                        return true;
                    }
                    use = rift.repair(tile);
                    Inv.remove(player, "guardian_fragments", GuardiansRift.REPAIR_FRAGMENTS);
                } else if (cell == null) {
                    Msg.game(player, "Barrier: " + rift.describe(tile) + ". Use a charged cell to raise, strengthen or recharge it.");
                    return true;
                } else {
                    double[] r = recharged.computeIfAbsent(player.getUuid(), k -> new double[2]);
                    boolean recharge = rift.tier(tile) >= cell.ordinal();
                    use = rift.useCell(tile, cell, r[0], (int) r[1]);
                    if (!use.ok()) {
                        Msg.game(player, use.message());
                        return true;
                    }
                    Inv.remove(player, cell.item, 1);
                    if (recharge) {
                        r[0] += use.xp();
                        r[1] += use.elemental();
                    }
                }
                gain(player, use.elemental(), use.catalytic());
                if (use.xp() > 0) {
                    xp(player, Skill.RUNECRAFT, use.xp());
                }
                Msg.game(player, use.message());
                return true;
            }
            case "gotr_elemental_essence", "gotr_catalytic_essence" -> {
                GuardiansRift.Cell cell = chargedCell(player);
                if (cell == null || !Inv.has(player, "chisel", 1)) {
                    Msg.game(player, "You need a chisel and a charged cell to assemble a rift guardian.");
                    return true;
                }
                GuardiansRift.Use use = rift.assemble(cell, blockId.startsWith("gotr_catalytic"));
                if (use.ok()) {
                    Inv.remove(player, cell.item, 1);
                    gain(player, use.elemental(), use.catalytic());
                    xp(player, Skill.RUNECRAFT, use.xp());
                    xp(player, Skill.CRAFTING, GuardiansRift.GUARDIAN_CRAFTING_XP);
                }
                Msg.game(player, use.message());
                return true;
            }
            case "elemental_rift", "catalytic_rift" -> {
                craft(player, blockId.startsWith("elemental") ? elemental : catalytic);
                return true;
            }
            case "gotr_great_guardian" -> {
                int el = Inv.remove(player, "elemental_guardian_stone", 1000);
                int cat = Inv.remove(player, "catalytic_guardian_stone", 1000);
                if (el + cat == 0) {
                    Msg.game(player, "The Great Guardian needs guardian stones. Craft runes to earn them.");
                    return true;
                }
                // Each stone gives 2 energy of its type and 5 Runecraft XP (OSRS Wiki); 250 stones per player fill him.
                gain(player, GuardiansRewards.stoneEnergy(el, 0), GuardiansRewards.stoneEnergy(cat, 0));
                power = Math.min(100, power + (el + cat) * GuardiansRewards.powerPerStone(Math.max(1, playerCount())));
                xp(player, Skill.RUNECRAFT, (el + cat) * GuardiansRewards.XP_PER_STONE);
                Msg.game(player, "You power the Great Guardian with " + (el + cat) + " stone" + (el + cat > 1 ? "s" : "") + ".");
                if (power >= 100) {
                    broadcastRed("The Great Guardian successfully closed the rift!");
                    end(true);
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private static boolean hasPickaxe(ServerPlayerEntity p) {
        for (String tier : new String[]{"bronze", "iron", "steel", "black", "mithril", "adamant", "rune", "dragon"}) {
            if (Inv.has(p, tier + "_pickaxe", 1)) {
                return true;
            }
        }
        return false;
    }

    private void craft(ServerPlayerEntity p, int altar) {
        Object[] a = Guardians.ALTARS[altar];
        int need = (Integer) a[2];
        if (level(p, Skill.RUNECRAFT) < need) {
            Msg.game(p, "You need a Runecraft level of " + need + " to use this guardian.");
            return;
        }
        // An uncharged cell is charged at the altar (only one charged cell can be carried).
        if (chargedCell(p) == null && Inv.has(p, GuardiansRift.UNCHARGED_CELL, 1)) {
            GuardiansRift.Cell cell = GuardiansRift.cellFor((String) a[0]);
            Inv.remove(p, GuardiansRift.UNCHARGED_CELL, 1);
            Inv.give(p, cell.item, 1);
            Msg.game(p, "You charge a cell: " + cell.label.toLowerCase(java.util.Locale.ROOT) + " cell.");
        }
        int ess = Inv.remove(p, "guardian_essence", 1000);
        if (ess == 0) {
            Msg.game(p, "You need guardian essence to craft runes here.");
            return;
        }
        double bonus = Guardians.raimentBonus(raiments(p));
        int runes = (int) Math.floor(ess * (1 + bonus) + random.nextDouble());
        reward(p, (String) a[1], runes);
        xp(p, Skill.RUNECRAFT, ess * (Double) a[3]);
        boolean cat = (Boolean) a[4];
        // One guardian stone per essence imbued; the stones carry the energy to the Great Guardian.
        Inv.give(p, cat ? "catalytic_guardian_stone" : "elemental_guardian_stone", ess * GuardiansRewards.STONES_PER_ESSENCE);
        Msg.game(p, "You bind the temple's power into " + runes + " " + name(altar).toLowerCase() + " runes.");
    }

    @Override
    protected void gameTick() {
        if (ticks % Guardians.ALTAR_SWITCH_TICKS == 0) {
            rotateAltars();
        }
        String msg = rift.tick(ticks, playerCount(), power, random::nextDouble);
        if (ticks == GuardiansRift.PREP_TICKS) {
            broadcastRed("Abyssal creatures pour through the rift!");
        }
        if (msg != null) {
            broadcastRed(msg);
        }
        if (rift.guardianHealth() <= 0 || ticks >= GAME_TICKS) {
            broadcastRed("The Great Guardian was defeated!");
            end(false);
        }
    }

    @Override
    protected void onEnd(boolean success) {
        for (ServerPlayerEntity p : online()) {
            Inv.remove(p, "guardian_fragments", 1000);
            Inv.remove(p, "guardian_essence", 1000);
            Inv.remove(p, "elemental_guardian_stone", 1000);
            Inv.remove(p, "catalytic_guardian_stone", 1000);
            Inv.remove(p, GuardiansRift.UNCHARGED_CELL, 1000);
            for (GuardiansRift.Cell c : GuardiansRift.Cell.values()) {
                Inv.remove(p, c.item, 1000);
            }
            if (!success) {
                continue;
            }
            int[] e = energy(p);
            if (!GuardiansRewards.qualifies(e[0], e[1])) {
                Msg.game(p, "You didn't contribute enough energy (" + (e[0] + e[1]) + "/" + GuardiansRewards.MIN_ENERGY
                        + ") for a reward.");
                continue;
            }
            MinigameData data = MinigameData.of(p);
            int elPts = data.stat(p.getUuid(), "gotr_elemental_points") + GuardiansRewards.points(e[0], random.nextDouble());
            int catPts = data.stat(p.getUuid(), "gotr_catalytic_points") + GuardiansRewards.points(e[1], random.nextDouble());
            int searches = Guardians.searches(elPts, catPts);
            data.setStat(p.getUuid(), "gotr_elemental_points", elPts - searches);
            data.setStat(p.getUuid(), "gotr_catalytic_points", catPts - searches);
            for (int i = 0; i < searches; i++) {
                MinigameItemUse.riftSearch(p);
            }
            xp(p, Skill.RUNECRAFT, GuardiansRewards.completionXp(level(p, Skill.RUNECRAFT)));
            int kc = data.incrementStat(p.getUuid(), "kc:" + game.id());
            Msg.game(p, "Elemental energy: " + e[0] + ". Catalytic energy: " + e[1] + ". You search the Rewards Guardian "
                    + searches + " time" + (searches == 1 ? "" : "s") + " (points left: " + (elPts - searches) + " elemental, "
                    + (catPts - searches) + " catalytic). Rifts closed: " + kc + ".");
        }
    }

    @Override
    protected List<String> hud(ServerPlayerEntity player) {
        int[] e = energy(player);
        StringBuilder barriers = new StringBuilder("Barriers:");
        for (int i = 0; i < GuardiansRift.CELL_TILES; i++) {
            barriers.append(' ').append(rift.describe(i)).append(i + 1 < GuardiansRift.CELL_TILES ? "," : "");
        }
        return List.of("Guardians of the Rift", "Guardian power: " + (int) power + "%  Health: " + rift.guardianHealth() + "%",
                "Elemental: " + name(elemental) + "  Catalytic: " + name(catalytic),
                "Energy: " + e[0] + " elemental / " + e[1] + " catalytic", barriers.toString(),
                "Rift guardians: " + rift.activeRiftGuardians(), "Time left: " + time(GAME_TICKS - ticks));
    }
}
