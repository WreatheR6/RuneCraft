package com.runecraft.content.bosses;

import com.runecraft.action.ActionManager;
import com.runecraft.osrs.content.bosses.ClassicBosses;
import com.runecraft.util.Msg;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Scurrius's food piles as scenery (OSRS Wiki "Food Pile"): three piles in his lair, where he eats between phases
 * ({@code ClassicScripts.Scurrius}) and where players can eat once every ten minutes to heal to full, but not for a
 * couple of seconds after being attacked, so his giant rats (which outlive him) keep players from them.
 */
final class ScurriusPiles {
    /** Game tick of each player's last meal at a pile, and of the last attack on them (a miss counts). */
    private static final Map<UUID, Long> LAST_MEAL = new HashMap<>();
    private static final Map<UUID, Long> LAST_ATTACKED = new HashMap<>();

    private ScurriusPiles() {
    }

    static void register() {
        com.runecraft.content.BlockHooks.register(ClassicBosses.SCURRIUS_FOOD_PILE, (p, pos, state) -> eat(p));
        com.runecraft.combat.CombatHooks.addDamageListener((player, attacker, attack, style, damage) -> {
            if (attacker != null && attacker != player) {
                LAST_ATTACKED.put(player.getUUID(), ActionManager.gameTick());
            }
        });
        // The piles stand in his arena (worlds whose Varrock Sewers place is built from round 44 on).
        com.runecraft.world.LandmarkBuilder.registerCustom("varrock_sewers", ScurriusPiles::build);
    }

    /** Lays the three piles on the arena floor at their offsets from its centre. */
    private static void build(ServerLevel world, BlockPos placeCentre) {
        com.runecraft.osrs.world.ArenaLayout.Arena arena = com.runecraft.osrs.world.ArenaLayout.of("scurrius");
        Block pile = com.runecraft.registry.ModBlocks.MODULE_BLOCKS.get(ClassicBosses.SCURRIUS_FOOD_PILE);
        if (arena == null || pile == null) {
            return;
        }
        for (int[] o : ClassicBosses.SCURRIUS_PILES) {
            int x = arena.x() + o[0];
            int z = arena.z() + o[1];
            int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            world.setBlock(new BlockPos(x, y, z), pile.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Eats from a food pile: full hitpoints, once every ten minutes, never straight after being attacked. Returns whether it ate. */
    static boolean eat(ServerPlayer p) {
        long now = ActionManager.gameTick();
        long meal = LAST_MEAL.getOrDefault(p.getUUID(), -1L);
        long attacked = LAST_ATTACKED.getOrDefault(p.getUUID(), -1L);
        if (!ClassicBosses.canEatPile(now, meal, attacked)) {
            if (meal >= 0 && now - meal < ClassicBosses.FOOD_PILE_COOLDOWN_TICKS) {
                long minutes = (ClassicBosses.FOOD_PILE_COOLDOWN_TICKS - (now - meal) + 99) / 100;
                Msg.game(p, "You're not hungry enough to eat that again yet. (" + minutes + " minute" + (minutes == 1 ? "" : "s") + ")");
            } else {
                Msg.game(p, "You can't eat from the food pile while you're under attack.");
            }
            return false;
        }
        LAST_MEAL.put(p.getUUID(), now);
        p.setHealth(p.getMaxHealth());
        com.runecraft.network.ServerNetworking.sync(p);
        Msg.game(p, "You eat from the food pile. It's not as bad as it looks.");
        return true;
    }

    /** Self-test: forgets a player's meal and attack times. */
    static void reset(ServerPlayer p) {
        LAST_MEAL.remove(p.getUUID());
        LAST_ATTACKED.remove(p.getUUID());
    }

    /** Self-test: marks the player as just attacked (a giant rat's miss). */
    static void attackedNow(ServerPlayer p) {
        LAST_ATTACKED.put(p.getUUID(), ActionManager.gameTick());
    }
}
