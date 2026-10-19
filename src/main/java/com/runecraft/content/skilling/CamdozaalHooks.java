package com.runecraft.content.skilling;

import com.runecraft.content.BlockHooks;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.skilling.Camdozaal;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * The Ruins of Camdozaal ({@link Camdozaal}): the preparation tables (prepare every raw fish carried, 4 ticks each, with
 * a knife), the altar (offer every prepared fish, 3 ticks each) and the Sacred Forge (forges the barronite mace from its
 * head, guard and handle and 1,500 shards, Ramarno's job in OSRS). The fishing spot is a {@code GatheringData} spot.
 */
final class CamdozaalHooks {
    private CamdozaalHooks() {
    }

    static void init() {
        BlockHooks.register(Camdozaal.PREPARATION_TABLE, (player, pos, state) -> prepare(player, pos));
        BlockHooks.register(Camdozaal.ALTAR, (player, pos, state) -> offer(player, pos));
        BlockHooks.register(Camdozaal.SACRED_FORGE, (player, pos, state) -> forge(player));
    }

    /** The first raw Camdozaal fish carried that the player can prepare, or null. */
    private static Camdozaal.Fish nextRaw(ServerPlayer player) {
        int cooking = SkillingUtil.level(player, Skill.COOKING);
        for (Camdozaal.Fish f : Camdozaal.FISH) {
            if (cooking >= f.level() && Inv.has(player, f.raw(), 1)) {
                return f;
            }
        }
        return null;
    }

    private static Camdozaal.Fish nextPrepared(ServerPlayer player) {
        for (Camdozaal.Fish f : Camdozaal.FISH) {
            if (Inv.has(player, f.prepared(), 1)) {
                return f;
            }
        }
        return null;
    }

    private static void prepare(ServerPlayer player, BlockPos pos) {
        if (!SkillingUtil.needItem(player, "knife", "You need a knife to prepare the fish.")) {
            return;
        }
        if (nextRaw(player) == null) {
            boolean any = false;
            for (Camdozaal.Fish f : Camdozaal.FISH) {
                any |= Inv.has(player, f.raw(), 1);
            }
            Msg.game(player, any ? "You need a higher Cooking level to prepare that fish." : "You have no fish to prepare.");
            return;
        }
        Msg.action(player, "You begin preparing the fish.");
        SkillingUtil.start(player, new SkillingUtil.BlockAction(pos, Camdozaal.PREPARATION_TABLE, Camdozaal.PREPARE_TICKS) {
            @Override
            boolean roll(ServerPlayer p, RandomSource random) {
                Camdozaal.Fish f = nextRaw(p);
                if (f == null || !Inv.has(p, "knife", 1)) {
                    return false;
                }
                Inv.remove(p, f.raw(), 1);
                if (random.nextDouble() < f.prepareChance(SkillingUtil.level(p, Skill.COOKING))) {
                    SkillingUtil.give(p, f.prepared(), 1);
                    XpService.add(p, Skill.COOKING, f.cookingXp());
                    Msg.action(p, "You prepare the " + SkillingUtil.lower(f.name()) + ".");
                } else {
                    SkillingUtil.give(p, f.ruined(), 1);
                    Msg.action(p, "You accidentally ruin the " + SkillingUtil.lower(f.name()) + ".");
                }
                return nextRaw(p) != null;
            }
        });
    }

    private static void offer(ServerPlayer player, BlockPos pos) {
        if (nextPrepared(player) == null) {
            Msg.game(player, "You have no prepared fish to offer. Prepare them at the table first.");
            return;
        }
        Msg.action(player, "You begin offering the fish.");
        SkillingUtil.start(player, new SkillingUtil.BlockAction(pos, Camdozaal.ALTAR, Camdozaal.OFFER_TICKS) {
            @Override
            boolean roll(ServerPlayer p, RandomSource random) {
                Camdozaal.Fish f = nextPrepared(p);
                if (f == null) {
                    return false;
                }
                Inv.remove(p, f.prepared(), 1);
                XpService.add(p, Skill.PRAYER, f.prayerXp());
                p.level().playSound(null, pos, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.6f, 1.0f);
                return nextPrepared(p) != null;
            }
        });
    }

    private static void forge(ServerPlayer player) {
        boolean parts = Inv.has(player, Camdozaal.HEAD, 1) && Inv.has(player, Camdozaal.GUARD, 1) && Inv.has(player, Camdozaal.HANDLE, 1);
        if (!parts) {
            Msg.game(player, "The Sacred Forge burns hot. With a barronite head, guard and handle and " + Camdozaal.MACE_SHARDS
                    + " barronite shards, a barronite mace could be forged here.");
            return;
        }
        if (!Inv.has(player, Camdozaal.SHARDS, Camdozaal.MACE_SHARDS)) {
            Msg.game(player, "You need " + Camdozaal.MACE_SHARDS + " barronite shards to forge the mace.");
            return;
        }
        Inv.remove(player, Camdozaal.HEAD, 1);
        Inv.remove(player, Camdozaal.GUARD, 1);
        Inv.remove(player, Camdozaal.HANDLE, 1);
        Inv.remove(player, Camdozaal.SHARDS, Camdozaal.MACE_SHARDS);
        SkillingUtil.give(player, Camdozaal.MACE, 1);
        Msg.game(player, "The pieces fuse together in the Sacred Forge. You now have a barronite mace.");
    }
}
