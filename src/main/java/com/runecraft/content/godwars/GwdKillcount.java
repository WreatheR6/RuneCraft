package com.runecraft.content.godwars;

import com.runecraft.block.BossSummonBlock;
import com.runecraft.collection.CollectionLog;
import com.runecraft.combat.MonsterStats;
import com.runecraft.entity.BossEntity;
import com.runecraft.osrs.content.godwars.GodWarsFactions;
import com.runecraft.osrs.content.godwars.GodWarsFactions.God;
import com.runecraft.osrs.content.godwars.GwdBosses;
import com.runecraft.osrs.data.Boss;
import com.runecraft.registry.ModEntities;
import com.runecraft.skill.Drops;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/**
 * God Wars kill count: killing a god's followers raises that god's count; entering a general's
 * chamber (summoning the general) costs {@value GodWarsFactions#REQUIRED_KILLCOUNT}. An ecumenical key
 * can be used instead. Also rolls the built-in General Graardor's and K'ril Tsutsaroth's hilts,
 * godsword shards and pets.
 */
final class GwdKillcount {
    private GwdKillcount() {
    }

    static int get(ServerPlayerEntity player, God god) {
        return GwdKillcountState.get(player.getServer()).get(player.getUuid(), god);
    }

    /** Checks and consumes the kill count (or an ecumenical key) needed to fight a general. */
    static boolean enter(ServerPlayerEntity player, God god, String general) {
        GwdKillcountState state = GwdKillcountState.get(player.getServer());
        int kc = state.get(player.getUuid(), god);
        int after = GodWarsFactions.afterEntry(kc);
        if (after >= 0) {
            state.set(player.getUuid(), god, after);
            Msg.game(player, "The door to " + general + "'s chamber opens. (" + GodWarsFactions.REQUIRED_KILLCOUNT + " "
                    + god.displayName() + " kill count used.)");
            return true;
        }
        if (Inv.has(player, "ecumenical_key", 1)) {
            Inv.remove(player, "ecumenical_key", 1);
            Msg.game(player, "You use your ecumenical key to open the door to " + general + "'s chamber.");
            return true;
        }
        Msg.red(player, "You need " + GodWarsFactions.REQUIRED_KILLCOUNT + " " + god.displayName() + " kill count to enter "
                + general + "'s chamber. You have " + kc + ".");
        return false;
    }

    /** Kill listener: follower kill count and the built-in generals' extra loot. */
    static void onKill(ServerPlayerEntity player, LivingEntity entity, MonsterStats stats) {
        if (stats != null && stats.profile() != null) {
            God god = GodWarsFactions.followerGod(stats.profile().id);
            if (god != null) {
                GwdKillcountState state = GwdKillcountState.get(player.getServer());
                int kc = state.get(player.getUuid(), god) + 1;
                state.set(player.getUuid(), god, kc);
                Msg.action(player, god.displayName() + " kill count: " + kc + (kc < GodWarsFactions.REQUIRED_KILLCOUNT
                        ? " / " + GodWarsFactions.REQUIRED_KILLCOUNT : ""));
            }
        }
        if (entity instanceof BossEntity be && be.boss() != null) {
            String id = be.boss().entityId();
            for (Drops.Rolled r : Drops.roll(GwdBosses.builtinExtraDrops(id), entity.getRandom(), false)) {
                entity.dropStack(Inv.stack(r.item(), r.count()));
                CollectionLog.obtain(player, r.item());
            }
            String pet = GwdBosses.builtinPet(id);
            if (pet != null && entity.getRandom().nextInt(GwdBosses.BUILTIN_PET_RATE) == 0) {
                GwdScript.givePet(player, pet);
            }
        }
    }

    /** Blocks the built-in Graardor / K'ril summoning stones until the kill count is met. */
    static void registerBuiltinGate() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient() || hand != Hand.MAIN_HAND || !(player instanceof ServerPlayerEntity sp)) {
                return ActionResult.PASS;
            }
            BlockPos pos = hit.getBlockPos();
            if (!(world.getBlockState(pos).getBlock() instanceof BossSummonBlock stone)) {
                return ActionResult.PASS;
            }
            Boss boss = stone.boss();
            God god = GodWarsFactions.generalGod(boss.entityId());
            if (god == null) {
                return ActionResult.PASS;
            }
            EntityType<BossEntity> type = ModEntities.BOSS_TYPES.get(boss);
            if (type != null && !((ServerWorld) world).getEntitiesByType(type, new Box(pos).expand(64), e -> e.isAlive()).isEmpty()) {
                return ActionResult.PASS; // already fighting: let the stone say so
            }
            return enter(sp, god, boss.displayName()) ? ActionResult.PASS : ActionResult.FAIL;
        });
    }
}
