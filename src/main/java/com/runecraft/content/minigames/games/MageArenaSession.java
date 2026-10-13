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
 * solve a maze: 2 points, +8 points and 10 law runes every fifth maze. Magic 33.</li>
 * <li>Alchemists' Playground: find the item in the cupboards and alch it (Low Level Alchemy from
 * 21, High from 55) into the coin collector. Items are worth 1, 5, 8, 15 or 30 coins, rotating
 * every minute; 1 point and 10 coins per 100 coins deposited, plus 2 Magic XP per coin.</li>
 * <li>Enchanting Chamber: take shapes from the piles and enchant them with your best enchantment
 * spell (1 cosmic each): points equal to the spell's level, doubled every tenth shape, +2 for the
 * indicated shape. Magic 7.</li>
 * <li>Creature Graveyard: gather bones while dodging falling bones (2 damage) and cast Bones to
 * Bananas (or Bones to Peaches once unlocked) into the food chute: 1 point per 16 bananas or 8
 * peaches, at most 3 per deposit. Magic 15.</li>
 * </ul>
 */
public class MageArenaSession extends MinigameSession {
    private final int room;
    private int statueHits;
    private int mazes;
    private int cupboard;
    private int[] statue = {0, 0};
    private int enchanted;
    /** Alchemy value index (into MageArena.ALCHEMY_VALUES) of the cupboard item right now. */
    private int alchValue;

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

    private static final int[] MAGIC = MageArena.ROOM_MAGIC;

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
        int have = MinigameData.of(p).points(p.getUuid(), MinigameCurrencies.key(currency));
        amount = Math.max(0, Math.min(amount, MageArena.MAX_POINTS[room] - have));
        if (amount == 0) {
            Msg.game(p, "You can't hold any more " + MinigameCurrencies.name(currency) + ".");
            return;
        }
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
                    boolean bonus = mazes % MageArena.TELEKINETIC_BONUS_EVERY == 0;
                    int pts = MageArena.TELEKINETIC_MAZE + (bonus ? MageArena.TELEKINETIC_BONUS : 0);
                    points(player, MinigameCurrencies.MTA_TELEKINETIC, pts);
                    if (bonus) {
                        reward(player, "law_rune", MageArena.TELEKINETIC_BONUS_LAWS);
                    }
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
                boolean high = level(player, Skill.MAGIC) >= 55;
                if (!runes(player, "nature_rune", items, "fire_rune", (high ? 5 : 3) * items)) {
                    return true;
                }
                Inv.remove(player, "alchemy_item", items);
                int coins = items * MageArena.ALCHEMY_VALUES[alchValue];
                xp(player, Skill.MAGIC, (high ? 65 : 31) * items + MageArena.ALCHEMY_XP_PER_COIN * coins);
                int pts = MageArena.alchemistPoints(coins, random.nextDouble());
                points(player, MinigameCurrencies.MTA_ALCHEMIST, pts);
                reward(player, "coins", pts * MageArena.ALCHEMY_COINS_REWARD);
                Msg.game(player, "You alch " + items + " item" + (items > 1 ? "s" : "") + " for " + coins + " coins and deposit them.");
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
                int spell = Math.max(1, MageArena.enchantLevel(level(player, Skill.MAGIC)));
                xp(player, Skill.MAGIC, MageArena.ENCHANT_XP[spell - 1] * cosmic);
                int pts = 0;
                for (int i = 0; i < cosmic; i++) {
                    pts += MageArena.enchantPoints(spell, ++enchanted, random.nextInt(4) == 0, false);
                }
                points(player, MinigameCurrencies.MTA_ENCHANTMENT, pts);
                return true;
            }
            case "mta_bone_pile" -> {
                Inv.give(player, "graveyard_bones", 1 + random.nextInt(4));
                return true;
            }
            case "mta_food_chute" -> {
                int bones = Inv.count(player, "graveyard_bones");
                if (bones == 0) {
                    Msg.game(player, "Gather bones from the piles first.");
                    return true;
                }
                boolean peaches = MinigameData.of(player).flag(player.getUuid(), "bones_to_peaches") && level(player, Skill.MAGIC) >= 60;
                if (peaches ? !runes(player, "nature_rune", 2, "water_rune", 4, "earth_rune", 4)
                        : !runes(player, "nature_rune", 1, "water_rune", 2, "earth_rune", 2)) {
                    return true;
                }
                Inv.remove(player, "graveyard_bones", bones);
                int fruit = 0;
                for (int i = 0; i < bones; i++) {
                    fruit += 1 + random.nextInt(4); // each bone type turns into 1-4 fruit
                }
                xp(player, Skill.MAGIC, peaches ? 35.5 : 25);
                int pts = MageArena.graveyardPoints(fruit, peaches, random.nextDouble());
                points(player, MinigameCurrencies.MTA_GRAVEYARD, pts);
                Msg.game(player, "You deposit " + fruit + (peaches ? " peaches." : " bananas."));
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    protected void gameTick() {
        if (room == 1 && ticks % MageArena.ALCHEMY_ROTATE_TICKS == 0) {
            alchValue = random.nextInt(MageArena.ALCHEMY_VALUES.length);
            cupboard = random.nextInt(8);
            broadcast("The Alchemy Guardian rearranges the cupboards: items are now worth "
                    + MageArena.ALCHEMY_VALUES[alchValue] + " coins.");
        }
        if (room == 3 && ticks % 8 == 0) {
            for (ServerPlayerEntity p : online()) {
                if (random.nextInt(3) == 0) {
                    world.spawnParticles(ParticleTypes.WHITE_ASH, p.getX(), p.getY() + 3, p.getZ(), 15, 0.5, 0.5, 0.5, 0);
                    hurt(p, MageArena.GRAVEYARD_BONE_DAMAGE);
                    Msg.action(p, "You're hit by falling bones!");
                }
            }
        }
    }

    @Override
    protected void onDeath(ServerPlayerEntity player) {
        if (room == 3) {
            int have = MinigameData.of(player).points(player.getUuid(), "mta_graveyard");
            addPoints(player, MinigameCurrencies.MTA_GRAVEYARD, -Math.min(have, MageArena.GRAVEYARD_DEATH_PENALTY));
        }
        super.onDeath(player);
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
