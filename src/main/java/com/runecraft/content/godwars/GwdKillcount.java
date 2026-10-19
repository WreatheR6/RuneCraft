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
import com.runecraft.platform.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/**
 * God Wars kill count: killing a god's followers raises that god's count; entering a general's
 * chamber (summoning the general) costs {@value GodWarsFactions#REQUIRED_KILLCOUNT}. An ecumenical key
 * can be used instead. Also rolls the built-in General Graardor's and K'ril Tsutsaroth's hilts,
 * godsword shards and pets.
 */
public final class GwdKillcount {
    /** Kill count needed to open a general's chamber (the systems module lowers it with Combat Achievement tiers). */
    public static java.util.function.ToIntFunction<ServerPlayer> required = p -> GodWarsFactions.REQUIRED_KILLCOUNT;
    /**
     * Ecumenical key chance (1 in n) for a Wilderness God Wars Dungeon kill: 1/60 (OSRS Wiki); the systems module lowers it
     * to 1/55..1/40 with the hard..grandmaster Combat Achievement tiers.
     */
    public static java.util.function.ToIntFunction<ServerPlayer> ecumenicalChance = p -> 60;
    /** Most ecumenical keys a player may hold (3; 4 and 5 with the medium and hard Wilderness diaries in OSRS). // approx */
    public static final int ECUMENICAL_KEY_CAP = 3;

    private GwdKillcount() {
    }

    static int get(ServerPlayer player, God god) {
        return GwdKillcountState.get(player.getServer()).get(player.getUUID(), god);
    }

    /** Checks and consumes the kill count (or an ecumenical key) needed to fight a general. */
    static boolean enter(ServerPlayer player, God god, String general) {
        GwdKillcountState state = GwdKillcountState.get(player.getServer());
        int kc = state.get(player.getUUID(), god);
        int need = required.applyAsInt(player);
        int after = kc >= need ? kc - need : -1;
        if (after >= 0) {
            state.set(player.getUUID(), god, after);
            Msg.game(player, "The door to " + general + "'s chamber opens. (" + need + " "
                    + god.displayName() + " kill count used.)");
            return true;
        }
        if (Inv.has(player, "ecumenical_key", 1)) {
            Inv.remove(player, "ecumenical_key", 1);
            Msg.game(player, "You use your ecumenical key to open the door to " + general + "'s chamber.");
            return true;
        }
        Msg.red(player, "You need " + need + " " + god.displayName() + " kill count to enter "
                + general + "'s chamber. You have " + kc + ".");
        return false;
    }

    /** Kill listener: follower kill count and the built-in generals' extra loot. */
    static void onKill(ServerPlayer player, LivingEntity entity, MonsterStats stats) {
        if (stats != null && stats.profile() != null) {
            God god = GodWarsFactions.followerGod(stats.profile().id);
            if (god != null) {
                GwdKillcountState state = GwdKillcountState.get(player.getServer());
                int kc = state.get(player.getUUID(), god) + 1;
                state.set(player.getUUID(), god, kc);
                int need = required.applyAsInt(player);
                Msg.action(player, god.displayName() + " kill count: " + kc + (kc < need ? " / " + need : ""));
                if (com.runecraft.world.Gielinor.wildernessLevel(entity) > 0
                        && entity.getRandom().nextInt(Math.max(1, ecumenicalChance.applyAsInt(player))) == 0) {
                    if (Inv.count(player, "ecumenical_key") >= ECUMENICAL_KEY_CAP) {
                        Msg.game(player, "You already have enough of these.");
                    } else {
                        entity.spawnAtLocation(Inv.stack("ecumenical_key", 1));
                    }
                }
            }
        }
        if (entity instanceof BossEntity be && be.boss() != null) {
            String id = be.boss().entityId();
            for (Drops.Rolled r : Drops.roll(GwdBosses.builtinExtraDrops(id), entity.getRandom(), false)) {
                entity.spawnAtLocation(Inv.stack(r.item(), r.count()));
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
            if (world.isClientSide() || hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer sp)) {
                return InteractionResult.PASS;
            }
            BlockPos pos = hit.getBlockPos();
            if (!(world.getBlockState(pos).getBlock() instanceof BossSummonBlock stone)) {
                return InteractionResult.PASS;
            }
            Boss boss = stone.boss();
            God god = GodWarsFactions.generalGod(boss.entityId());
            if (god == null) {
                return InteractionResult.PASS;
            }
            EntityType<BossEntity> type = ModEntities.BOSS_TYPES.get(boss);
            if (type != null && !((ServerLevel) world).getEntities(type, new AABB(pos).inflate(64), e -> e.isAlive()).isEmpty()) {
                return InteractionResult.PASS; // already fighting: let the stone say so
            }
            return enter(sp, god, boss.displayName()) ? InteractionResult.PASS : InteractionResult.FAIL;
        });
    }
}
