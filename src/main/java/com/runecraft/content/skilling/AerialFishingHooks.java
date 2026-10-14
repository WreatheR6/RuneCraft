package com.runecraft.content.skilling;

import com.runecraft.content.BlockHooks;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.skilling.AerialFishing;
import com.runecraft.osrs.content.skilling.GatheringData;
import com.runecraft.quest.QuestScripts;
import com.runecraft.skill.Stun;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/**
 * Aerial fishing at Lake Molch ({@link AerialFishing}) and Kylie Minnow's minnow-for-shark trade at the Fishing Guild.
 * The cormorant's glove is carried rather than worn (there is no hands slot) and the weapon hand must be empty; a
 * player with the levels and no glove is lent one at the first cast (OSRS: Alry the Angler hands it out). // approx
 */
final class AerialFishingHooks {
    /** Game ticks between cormorant catches (OSRS: every tick from an adjacent spot, slower from further away). // approx */
    private static final int INTERVAL = 3;

    private AerialFishingHooks() {
    }

    static void init() {
        BlockHooks.register(AerialFishing.SPOT, (player, pos, state) -> fish(player, pos));
        QuestScripts.onTalk(GatheringData.KYLIE_MINNOW, AerialFishingHooks::kylie);
    }

    private static String bait(ServerPlayerEntity player) {
        if (Inv.has(player, AerialFishing.KING_WORM, 1)) {
            return AerialFishing.KING_WORM;
        }
        return Inv.has(player, AerialFishing.OFFCUTS, 1) ? AerialFishing.OFFCUTS : null;
    }

    private static boolean ready(ServerPlayerEntity player) {
        if (!player.getMainHandStack().isEmpty()) {
            Msg.game(player, "You need both hands free to fish with a cormorant.");
            return false;
        }
        if (bait(player) == null) {
            Msg.game(player, "You need some king worms or fish offcuts to reward your cormorant.");
            return false;
        }
        return true;
    }

    private static void fish(ServerPlayerEntity player, BlockPos pos) {
        if (!Stun.check(player) || !SkillingUtil.need(player, Skill.FISHING, AerialFishing.FISHING_LEVEL, "fish here")
                || !SkillingUtil.need(player, Skill.HUNTER, AerialFishing.HUNTER_LEVEL, "fish here")) {
            return;
        }
        if (!Inv.has(player, AerialFishing.GLOVE, 1)) {
            SkillingUtil.give(player, AerialFishing.GLOVE, 1);
            Msg.game(player, "Alry the Angler lends you a cormorant's glove, and a cormorant settles on it.");
        }
        if (!ready(player)) {
            return;
        }
        Msg.action(player, "You send your cormorant to try to catch a fish from out of the water.");
        SkillingUtil.start(player, new SkillingUtil.BlockAction(pos, AerialFishing.SPOT, INTERVAL) {
            @Override
            boolean roll(ServerPlayerEntity p, Random random) {
                if (!Inv.has(p, AerialFishing.GLOVE, 1) || !ready(p)) {
                    return false;
                }
                int fishing = SkillingUtil.level(p, Skill.FISHING);
                int hunter = SkillingUtil.level(p, Skill.HUNTER);
                String bait = bait(p);
                if (bait.equals(AerialFishing.KING_WORM) || random.nextDouble() < AerialFishing.OFFCUT_USE_CHANCE) {
                    Inv.remove(p, bait, 1);
                }
                p.getWorld().playSound(null, pos, SoundEvents.ENTITY_FISHING_BOBBER_SPLASH, SoundCategory.PLAYERS, 0.4f, 1.2f);
                if (random.nextInt(AerialFishing.GOLDEN_TENCH_CHANCE) == 0) {
                    SkillingUtil.give(p, AerialFishing.GOLDEN_TENCH, 1);
                    Msg.red(p, "Your cormorant returns with a golden tench!");
                    return true;
                }
                int x = Math.max(1, AerialFishing.x(fishing, hunter));
                AerialFishing.Fish f = AerialFishing.catchFor(fishing, hunter, random.nextInt(x));
                SkillingUtil.give(p, f.item(), 1);
                XpService.add(p, Skill.FISHING, f.fishingXp());
                XpService.add(p, Skill.HUNTER, f.hunterXp());
                Msg.action(p, "Your cormorant returns with its catch: " + Msg.article(SkillingUtil.lower(f.name())) + ".");
                if (random.nextDouble() < AerialFishing.pearlChance(fishing, hunter)) {
                    SkillingUtil.give(p, AerialFishing.PEARL, 1);
                    Msg.red(p, "You find a molch pearl in the catch!");
                }
                return true;
            }
        });
    }

    /** Kylie Minnow: one raw shark for every 40 minnows handed over. */
    private static void kylie(ServerPlayerEntity player, QuestNpcEntity npc) {
        int minnows = Inv.count(player, GatheringData.MINNOW);
        int sharks = minnows / GatheringData.MINNOWS_PER_SHARK;
        if (sharks == 0) {
            Msg.game(player, "Kylie Minnow: Bring me " + GatheringData.MINNOWS_PER_SHARK
                    + " minnows from my platform and I'll swap them for a raw shark.");
            return;
        }
        Inv.remove(player, GatheringData.MINNOW, sharks * GatheringData.MINNOWS_PER_SHARK);
        SkillingUtil.give(player, "raw_shark", sharks);
        Msg.game(player, "Kylie Minnow: " + sharks * GatheringData.MINNOWS_PER_SHARK + " minnows? Here's " + sharks + " raw shark"
                + (sharks > 1 ? "s" : "") + " for them.");
    }
}
