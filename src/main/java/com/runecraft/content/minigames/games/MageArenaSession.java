package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameCurrencies;
import com.runecraft.osrs.content.minigames.MinigameRules.MageArena;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * The Mage Training Arena's four rooms (entered from separate entrances):
 * <ul>
 * <li>Telekinetic Theatre: Telekinetic Grab (1 law, 1 air) the guardian statue three times to
 * solve a maze: 2 points, +8 every fifth maze. Magic 33.</li>
 * <li>Alchemists' Playground: find the item in the cupboards and High Alchemy it (1 nature, 5
 * fire) into the coin collector: 1 point per 100 coins. Magic 55.</li>
 * <li>Enchanting Chamber: take shapes from the piles and enchant them into the hole (1 cosmic
 * each): 2 points per shape. Magic 7.</li>
 * <li>Creature Graveyard: gather bones from the piles while dodging falling bones and cast Bones
 * to Bananas (1 nature, 2 water, 2 earth) into the food chute: 1 point per 4 bones. Magic 15.</li>
 * </ul>
 */
public class MageArenaSession extends MinigameSession {
    private final int room;
    private int statueHits;
    private int mazes;
    private int cupboard;
    private int[] statue = {0, 0};

    public MageArenaSession(int id, int slot, ServerWorld world, String variant) {
        super(Minigame.MAGE_TRAINING_ARENA, id, slot, world, variant);
        int r = 0;
        for (int i = 0; i < MageArena.ROOMS.length; i++) {
            if (variant.contains(MageArena.ROOMS[i])) {
                r = i;
            }
        }
        this.room = r;
    }

    @Override
    protected int halfX() {
        return 14;
    }

    @Override
    protected int halfZ() {
        return 14;
    }

    private static final int[] MAGIC = {33, 55, 7, 15};

    @Override
    protected void build() {
        room(Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_TILES, Blocks.AMETHYST_BLOCK);
        switch (room) {
            case 0 -> {
                for (int i = 0; i < 18; i++) {
                    int x = random.nextInt(24) - 12;
                    int z = random.nextInt(20) - 12;
                    if (Math.abs(x) + Math.abs(z) > 3) {
                        set(x, 1, z, Blocks.DEEPSLATE_BRICK_WALL.getDefaultState());
                    }
                }
                moveStatue();
            }
            case 1 -> {
                for (int i = 0; i < 8; i++) {
                    place(-12 + i * 3, -12, "mta_cupboard");
                }
                place(0, 0, "mta_coin_collector");
                cupboard = random.nextInt(8);
            }
            case 2 -> {
                place(-10, -10, "mta_shape_pile");
                place(10, -10, "mta_shape_pile");
                place(-10, 6, "mta_shape_pile");
                place(10, 6, "mta_shape_pile");
                place(0, -2, "mta_enchant_hole");
            }
            default -> {
                place(-10, -10, "mta_bone_pile");
                place(10, -10, "mta_bone_pile");
                place(-10, 4, "mta_bone_pile");
                place(10, 4, "mta_bone_pile");
                place(0, -12, "mta_food_chute");
            }
        }
        place(0, 13, "minigame_exit");
    }

    private void moveStatue() {
        set(statue[0], 1, statue[1], Blocks.AIR.getDefaultState());
        statue = new int[]{random.nextInt(22) - 11, random.nextInt(18) - 12};
        place(statue[0], statue[1], "mta_statue");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayerEntity player, int index) {
        return rel(index % 3 - 1, 1, 11);
    }

    @Override
    protected void start() {
        for (ServerPlayerEntity p : online()) {
            if (level(p, Skill.MAGIC) < MAGIC[room]) {
                Msg.game(p, "You need a Magic level of " + MAGIC[room] + " to train in this room.");
                removePlayer(p, true);
            }
        }
    }

    private boolean runes(ServerPlayerEntity p, Object... pairs) {
        for (int i = 0; i < pairs.length; i += 2) {
            if (!Inv.has(p, (String) pairs[i], (Integer) pairs[i + 1])) {
                Msg.game(p, "You don't have enough runes to cast this spell.");
                return false;
            }
        }
        for (int i = 0; i < pairs.length; i += 2) {
            Inv.remove(p, (String) pairs[i], (Integer) pairs[i + 1]);
        }
        return true;
    }

    private void points(ServerPlayerEntity p, String currency, int amount) {
        addPoints(p, currency, amount);
        Msg.action(p, "+" + amount + " " + MinigameCurrencies.name(currency));
    }

    @Override
    protected boolean onBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
        switch (blockId) {
            case "mta_statue" -> {
                if (!runes(player, "law_rune", 1, "air_rune", 1)) {
                    return true;
                }
                xp(player, Skill.MAGIC, 43);
                world.spawnParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 20, 0.4, 0.6, 0.4, 0.3);
                if (++statueHits >= 3) {
                    statueHits = 0;
                    mazes++;
                    int pts = MageArena.TELEKINETIC_MAZE + (mazes % MageArena.TELEKINETIC_BONUS_EVERY == 0 ? MageArena.TELEKINETIC_BONUS : 0);
                    points(player, MinigameCurrencies.MTA_TELEKINETIC, pts);
                    Msg.game(player, "Congratulations, you have solved the maze!");
                    xp(player, Skill.MAGIC, 8 * level(player, Skill.MAGIC) / 10.0);
                }
                moveStatue();
                return true;
            }
            case "mta_cupboard" -> {
                int index = (pos.getX() - origin.getX() + 12) / 3;
                if (index == cupboard) {
                    Inv.give(player, "alchemy_item", 1);
                    Msg.game(player, "You find an item worth alching.");
                    cupboard = random.nextInt(8);
                } else {
                    Msg.game(player, "The cupboard is empty.");
                }
                return true;
            }
            case "mta_coin_collector" -> {
                int items = Inv.count(player, "alchemy_item");
                if (items == 0) {
                    Msg.game(player, "Search the cupboards for items to alchemise.");
                    return true;
                }
                if (!runes(player, "nature_rune", items, "fire_rune", 5 * items)) {
                    return true;
                }
                Inv.remove(player, "alchemy_item", items);
                int coins = 0;
                for (int i = 0; i < items; i++) {
                    coins += 300 + random.nextInt(900);
                }
                xp(player, Skill.MAGIC, 65 * items);
                points(player, MinigameCurrencies.MTA_ALCHEMIST, Math.max(1, coins / MageArena.ALCHEMY_COINS_PER_POINT));
                reward(player, "coins", coins / 10);
                return true;
            }
            case "mta_shape_pile" -> {
                Inv.give(player, "enchantment_shape", 1 + random.nextInt(4));
                return true;
            }
            case "mta_enchant_hole" -> {
                int shapes = Inv.count(player, "enchantment_shape");
                if (shapes == 0) {
                    Msg.game(player, "Gather shapes from the piles first.");
                    return true;
                }
                int cosmic = Math.min(shapes, Inv.count(player, "cosmic_rune"));
                if (cosmic == 0) {
                    Msg.game(player, "You need cosmic runes to enchant the shapes.");
                    return true;
                }
                Inv.remove(player, "cosmic_rune", cosmic);
                Inv.remove(player, "enchantment_shape", cosmic);
                xp(player, Skill.MAGIC, 17.5 * cosmic);
                points(player, MinigameCurrencies.MTA_ENCHANTMENT, 2 * cosmic / MageArena.ENCHANT_SHAPES_PER_POINT);
                return true;
            }
            case "mta_bone_pile" -> {
                Inv.give(player, "graveyard_bones", 1 + random.nextInt(4));
                return true;
            }
            case "mta_food_chute" -> {
                int bones = Inv.count(player, "graveyard_bones");
                if (bones < MageArena.GRAVEYARD_BONES_PER_POINT) {
                    Msg.game(player, "You need at least " + MageArena.GRAVEYARD_BONES_PER_POINT + " bones.");
                    return true;
                }
                if (!runes(player, "nature_rune", 1, "water_rune", 2, "earth_rune", 2)) {
                    return true;
                }
                Inv.remove(player, "graveyard_bones", bones);
                xp(player, Skill.MAGIC, 25);
                points(player, MinigameCurrencies.MTA_GRAVEYARD, bones / MageArena.GRAVEYARD_BONES_PER_POINT);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    protected void gameTick() {
        if (room == 3 && ticks % 8 == 0) {
            for (ServerPlayerEntity p : online()) {
                if (random.nextInt(3) == 0) {
                    world.spawnParticles(ParticleTypes.WHITE_ASH, p.getX(), p.getY() + 3, p.getZ(), 15, 0.5, 0.5, 0.5, 0);
                    hurt(p, 1 + random.nextInt(2));
                    Msg.action(p, "You're hit by falling bones!");
                }
            }
        }
    }

    @Override
    protected void onLeave(ServerPlayerEntity player) {
        Inv.remove(player, "alchemy_item", 1000);
        Inv.remove(player, "enchantment_shape", 1000);
        Inv.remove(player, "graveyard_bones", 1000);
    }

    @Override
    protected List<String> hud(ServerPlayerEntity player) {
        MinigameData d = MinigameData.of(player);
        String[] names = {"Telekinetic Theatre", "Alchemists' Playground", "Enchanting Chamber", "Creature Graveyard"};
        return List.of("Mage Training Arena - " + names[room],
                "Telekinetic: " + d.points(player.getUuid(), "mta_telekinetic") + "  Alchemist: " + d.points(player.getUuid(), "mta_alchemist"),
                "Enchantment: " + d.points(player.getUuid(), "mta_enchantment") + "  Graveyard: " + d.points(player.getUuid(), "mta_graveyard"),
                room == 0 ? "Mazes solved: " + mazes + " (statue grabs " + statueHits + "/3)" : "");
    }
}
