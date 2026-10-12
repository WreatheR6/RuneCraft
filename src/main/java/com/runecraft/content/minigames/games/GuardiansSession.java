package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameItemUse;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.GuardiansRewards;
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
 * to reach 100%). With 150+ energy, every 100 energy of a type is a reward point, and each Rewards
 * Guardian search costs one elemental and one catalytic point.
 */
public class GuardiansSession extends MinigameSession {
    private static final int GAME_TICKS = 1000;
    private final Map<UUID, int[]> energy = new HashMap<>();
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
        broadcastRed("The rift becomes active! Power the Great Guardian to seal it.");
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
                int[] e = energy(player);
                e[0] = Math.min(GuardiansRewards.ENERGY_CAP, e[0] + GuardiansRewards.stoneEnergy(el, 0));
                e[1] = Math.min(GuardiansRewards.ENERGY_CAP, e[1] + GuardiansRewards.stoneEnergy(cat, 0));
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
        if (ticks >= GAME_TICKS) {
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
        return List.of("Guardians of the Rift", "Guardian power: " + (int) power + "%", "Elemental: " + name(elemental) + "  Catalytic: "
                + name(catalytic), "Energy: " + e[0] + " elemental / " + e[1] + " catalytic", "Time left: " + time(GAME_TICKS - ticks));
    }
}
