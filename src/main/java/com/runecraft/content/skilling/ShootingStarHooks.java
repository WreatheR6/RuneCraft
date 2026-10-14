package com.runecraft.content.skilling;

import com.runecraft.action.ActionManager;
import com.runecraft.content.BlockHooks;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.Chance;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.skilling.ShootingStars;
import com.runecraft.osrs.data.ToolTier;
import com.runecraft.osrs.world.StructureDef;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestScripts;
import com.runecraft.skill.Stun;
import com.runecraft.skill.Tools;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import com.runecraft.world.StructurePlacer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Shooting Stars ({@link ShootingStars}): a star crashes at a landing site every 75-105 minutes and is announced to
 * every player; mining it gives stardust and Mining XP, each 7 minutes of mining wears a layer away, and Dusuri
 * trades stardust for rewards (talk to buy the selected reward, sneak and talk to see the next). The star, the
 * crash timer and discovery bonuses are kept in memory (a restart clears them, like an OSRS system update). // approx
 */
final class ShootingStarHooks {
    private static final class Star {
        final BlockPos pos;
        final ShootingStars.Site site;
        int size;
        int worn;
        long lastMined = -100;
        UUID discoverer;

        Star(BlockPos pos, ShootingStars.Site site, int size) {
            this.pos = pos;
            this.site = site;
            this.size = size;
        }
    }

    private static Star star;
    private static ShootingStars.Site lastSite;
    private static long nextCrash = -1;
    private static long lastTick = -1;
    /** Discovery bonus stardust left per player. */
    private static final Map<UUID, Integer> BONUS = new HashMap<>();
    /** Reward each player has selected at Dusuri's shop. */
    private static final Map<UUID, Integer> SELECTED = new HashMap<>();

    private ShootingStarHooks() {
    }

    static void init() {
        BlockHooks.register(ShootingStars.STAR, (player, pos, state) -> mine(player, pos));
        QuestScripts.onTalk(ShootingStars.DUSURI, ShootingStarHooks::dusuri);
        ServerTickEvents.END_SERVER_TICK.register(ShootingStarHooks::tick);
    }

    private static void tick(MinecraftServer server) {
        long now = ActionManager.gameTick();
        if (now == lastTick) {
            return;
        }
        lastTick = now;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (nextCrash < 0) {
            nextCrash = now + random.nextInt(ShootingStars.FIRST_CRASH_MIN, ShootingStars.FIRST_CRASH_MAX + 1);
        }
        if (now >= nextCrash) {
            nextCrash = now + ShootingStars.nextCrashTicks(random.nextDouble());
            try {
                crash(server, random);
            } catch (RuntimeException e) {
                com.runecraft.RuneCraft.LOGGER.error("Shooting star failed to land", e);
            }
            return;
        }
        // A layer wears away only while someone is mining the star.
        if (star != null && now - star.lastMined <= 8 && ++star.worn >= ShootingStars.LAYER_TICKS) {
            star.worn = 0;
            star.size--;
            ServerWorld world = Gielinor.world(server);
            if (star.size <= 0) {
                if (world != null && isStar(world.getBlockState(star.pos))) {
                    world.setBlockState(star.pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                }
                star = null;
            } else if (world != null) {
                world.playSound(null, star.pos, SoundEvents.BLOCK_STONE_BREAK, SoundCategory.BLOCKS, 1.0f, 0.7f);
            }
        }
    }

    private static boolean isStar(BlockState state) {
        return SkillingUtil.blockId(state).equals(ShootingStars.STAR);
    }

    private static void crash(MinecraftServer server, ThreadLocalRandom random) {
        ServerWorld world = Gielinor.world(server);
        BlockState block = SkillingUtil.moduleBlock(ShootingStars.STAR);
        if (world == null || block == null) {
            return;
        }
        if (star != null && isStar(world.getBlockState(star.pos))) {
            world.setBlockState(star.pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        }
        ShootingStars.Site site = ShootingStars.nextSite(lastSite, random.nextDouble());
        lastSite = site;
        int x = StructureDef.bx(site.x());
        int z = StructureDef.bz(site.y());
        world.getChunk(x >> 4, z >> 4);
        StructurePlacer.applyIfLoaded(world, x >> 4, z >> 4);
        BlockPos pos = Gielinor.safeSpot(world, x, z);
        world.setBlockState(pos, block, Block.NOTIFY_ALL);
        star = new Star(pos, site, ShootingStars.landingSize(random.nextDouble()));
        server.getPlayerManager().broadcast(Text.literal("A shooting star has crashed in " + site.region() + ": "
                + site.name() + ".").formatted(Formatting.GOLD), false);
    }

    private static void mine(ServerPlayerEntity player, BlockPos pos) {
        ServerWorld world = (ServerWorld) player.getWorld();
        if (star == null || !star.pos.equals(pos)) {
            // A star left over from before a restart has burnt out.
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            Msg.game(player, "The star crumbles away.");
            return;
        }
        if (player.isSneaking()) {
            Msg.game(player, "This is a size-" + star.size + " star. A Mining level of at least " + ShootingStars.level(star.size)
                    + " is required to mine this layer. It has been mined about " + (star.worn * 100 / ShootingStars.LAYER_TICKS)
                    + "% of the way to the next layer.");
            return;
        }
        if (!Stun.check(player) || !SkillingUtil.need(player, Skill.MINING, ShootingStars.level(star.size), "mine this layer of the star")) {
            return;
        }
        ToolTier pick = Tools.bestPickaxe(player, Players.data(player).skills());
        if (pick == null) {
            Msg.game(player, "You need a pickaxe to mine this star.");
            return;
        }
        Msg.action(player, "You swing your pickaxe at the star.");
        SkillingUtil.start(player, new SkillingUtil.BlockAction(pos, ShootingStars.STAR, pick.miningTicks()) {
            @Override
            boolean roll(ServerPlayerEntity p, Random random) {
                Star s = star;
                if (s == null || !s.pos.equals(pos) || Tools.bestPickaxe(p, Players.data(p).skills()) == null
                        || SkillingUtil.level(p, Skill.MINING) < ShootingStars.level(s.size)) {
                    return false;
                }
                s.lastMined = ActionManager.gameTick();
                p.getWorld().playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.PLAYERS, 1.0f, 0.9f);
                if (random.nextDouble() >= Chance.lerp(SkillingUtil.level(p, Skill.MINING), ShootingStars.LOW, ShootingStars.HIGH)) {
                    return true;
                }
                if (s.discoverer == null) {
                    s.discoverer = p.getUuid();
                    BONUS.put(p.getUuid(), ShootingStars.DISCOVERY_BONUS);
                    Msg.red(p, "Congratulations! You were the first person to find this star!");
                }
                int dust = 1;
                int bonus = BONUS.getOrDefault(p.getUuid(), 0);
                if (bonus > 0) {
                    dust++;
                    BONUS.put(p.getUuid(), bonus - 1);
                }
                SkillingUtil.give(p, ShootingStars.STARDUST, dust);
                XpService.add(p, Skill.MINING, ShootingStars.XP);
                return true;
            }
        });
    }

    private static void dusuri(ServerPlayerEntity player, QuestNpcEntity npc) {
        int n = ShootingStars.SHOP.size();
        int index = SELECTED.getOrDefault(player.getUuid(), 0) % n;
        if (player.isSneaking() || !SELECTED.containsKey(player.getUuid())) {
            if (SELECTED.containsKey(player.getUuid())) {
                index = (index + 1) % n;
            }
            SELECTED.put(player.getUuid(), index);
            ShootingStars.Reward r = ShootingStars.SHOP.get(index);
            Msg.game(player, "Dusuri: The " + r.label() + " costs " + r.stardust()
                    + " stardust. Talk to me again to buy it, or sneak and talk to see something else.");
            return;
        }
        ShootingStars.Reward r = ShootingStars.SHOP.get(index);
        if (!Inv.has(player, ShootingStars.STARDUST, r.stardust())) {
            Msg.game(player, "Dusuri: You'll need " + r.stardust() + " stardust for that. You have "
                    + Inv.count(player, ShootingStars.STARDUST) + ".");
            return;
        }
        Inv.remove(player, ShootingStars.STARDUST, r.stardust());
        if (r.give().equals("bag_full_of_gems")) {
            Random random = player.getRandom();
            for (int i = 0; i < r.count(); i++) {
                SkillingUtil.roll(player, ShootingStars.BAG_OF_GEMS, random);
            }
        } else {
            SkillingUtil.give(player, r.give(), r.count());
        }
        Msg.game(player, "Dusuri: A fine choice. May the stars guide you.");
    }
}
