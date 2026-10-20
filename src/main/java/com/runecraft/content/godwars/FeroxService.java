package com.runecraft.content.godwars;

import com.runecraft.action.ActionManager;
import com.runecraft.combat.PoisonService;
import com.runecraft.combat.Teleblock;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.systems.RunEnergyService;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.osrs.content.godwars.FeroxEnclave;
import com.runecraft.platform.ServerTickEvents;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * The Ferox Enclave in the world ({@link FeroxEnclave}): the chapel's pools of refreshment and the barriers that keep
 * tele-blocked players out. (Its safe zone for player fights and the looting bag is {@link Gielinor#inSafeZone}.)
 */
public final class FeroxService {
    private FeroxService() {
    }

    /** Where each player last stood outside the enclave, to put a tele-blocked one back through the barrier. */
    private static final Map<UUID, Vec3> OUTSIDE = new HashMap<>();
    private static long ticks;

    static void register() {
        BlockHooks.register(FeroxEnclave.BLOCK_POOL, (player, pos, state) -> drink(player, pos));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++ticks % 2 != 0) {
                return;
            }
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                barrier(p);
            }
        });
    }

    /** A tele-blocked player stepping into the enclave is put back outside the barrier. */
    static void barrier(ServerPlayer p) {
        if (!Gielinor.inSafeZone(p)) {
            OUTSIDE.put(p.getUUID(), p.position());
            return;
        }
        Vec3 back = OUTSIDE.get(p.getUUID());
        if (back != null && !FeroxEnclave.barrierLetsIn(Teleblock.active(p))) {
            p.teleportTo(back.x, back.y, back.z);
            Msg.game(p, FeroxEnclave.BARRIER_MESSAGE);
            return;
        }
        OUTSIDE.remove(p.getUUID());
    }

    /** Drinks from a pool of refreshment (public for the self-test). */
    public static void drink(ServerPlayer player, BlockPos pos) {
        PlayerData data = Players.data(player);
        SkillSet skills = data.skills();
        FeroxEnclave.PoolEffect fx = FeroxEnclave.POOL;
        if (fx.hitpoints()) {
            player.setHealth(player.getMaxHealth());
        }
        if (fx.prayer()) {
            data.setPrayerPoints(skills.level(Skill.PRAYER));
        }
        if (fx.runEnergy()) {
            RunEnergyService.restore(player, 10000);
        }
        if (fx.resetStats()) {
            // "reset all boosted or lowered skills"
            for (Skill s : Skill.values()) {
                if (s != Skill.HITPOINTS && s != Skill.PRAYER && skills.boost(s) != 0) {
                    skills.setBoost(s, 0);
                }
            }
        }
        if (fx.curesPoison()) {
            PoisonService.cure(player, fx.curesVenom(), 0, ActionManager.gameTick());
        }
        if (fx.disablesPrayers()) {
            data.activePrayers().clear();
        }
        // fx.specialAttack() is false: the enclave's pools leave special attack energy alone
        if (player.level() instanceof ServerLevel world && pos != null) {
            world.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 30, 0.4, 0.2, 0.4, 0.1);
            world.playSound(null, pos, SoundEvents.GENERIC_DRINK, SoundSource.BLOCKS, 1f, 1f);
        }
        Msg.game(player, FeroxEnclave.POOL_MESSAGE);
        ServerNetworking.sync(player);
    }
}
