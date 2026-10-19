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
import com.runecraft.platform.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Shooting Stars ({@link ShootingStars}): a star crashes at a landing site every 75-105 minutes and is announced to
 * every player; mining it gives stardust and Mining XP, each 7 minutes of mining wears a layer away, and Dusuri
 * trades stardust for rewards (talk to buy the selected reward, sneak and talk to see the next). The star (site,
 * size, wear, discoverer), the time to the next crash and the discovery bonuses are saved in {@link SkillingState}
 * (section {@value #SECTION}) and survive a restart.
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
    /** SkillingState section holding the star and the crash timer, and the one holding discovery bonuses. */
    static final String SECTION = "shooting_star";
    static final String BONUS_SECTION = "shooting_star_bonus";
    private static boolean loaded;

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
        if (!loaded) {
            loaded = true;
            try {
                load(server, now);
            } catch (RuntimeException e) {
                com.runecraft.RuneCraft.LOGGER.error("Shooting star state failed to load", e);
            }
        }
        if (now % 20 == 0) {
            save(server, now);
        }
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
            save(server, now);
            return;
        }
        // A layer wears away only while someone is mining the star.
        if (star != null && now - star.lastMined <= 8 && ++star.worn >= ShootingStars.LAYER_TICKS) {
            star.worn = 0;
            star.size--;
            ServerLevel world = Gielinor.world(server);
            if (star.size <= 0) {
                if (world != null && isStar(world.getBlockState(star.pos))) {
                    world.setBlock(star.pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
                star = null;
            } else if (world != null) {
                world.playSound(null, star.pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0f, 0.7f);
            }
            save(server, now);
        }
    }

    /** Restores the star, the crash timer and the discovery bonuses saved before a restart. */
    private static void load(MinecraftServer server, long now) {
        SkillingState state = SkillingState.get(server);
        CompoundTag tag = state.section(SECTION);
        if (tag.contains("nextIn")) {
            nextCrash = now + Math.max(1, tag.getLong("nextIn"));
        }
        int last = tag.contains("lastSite") ? tag.getInt("lastSite") : -1;
        lastSite = last >= 0 && last < ShootingStars.SITES.size() ? ShootingStars.SITES.get(last) : null;
        int site = tag.contains("site") ? tag.getInt("site") : -1;
        if (site >= 0 && site < ShootingStars.SITES.size() && tag.getInt("size") > 0) {
            Star s = new Star(new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z")), ShootingStars.SITES.get(site),
                    tag.getInt("size"));
            s.worn = tag.getInt("worn");
            if (tag.hasUUID("discoverer")) {
                s.discoverer = tag.getUUID("discoverer");
            }
            star = s;
        }
        CompoundTag bonus = state.section(BONUS_SECTION);
        for (String key : bonus.getAllKeys()) {
            try {
                BONUS.put(UUID.fromString(key), bonus.getInt(key));
            } catch (IllegalArgumentException ignored) {
                // not a player id
            }
        }
    }

    /** Writes the star, the crash timer and the bonuses into the world save. */
    private static void save(MinecraftServer server, long now) {
        SkillingState state = SkillingState.get(server);
        CompoundTag tag = new CompoundTag();
        if (nextCrash >= 0) {
            tag.putLong("nextIn", Math.max(1, nextCrash - now));
        }
        tag.putInt("lastSite", lastSite == null ? -1 : ShootingStars.SITES.indexOf(lastSite));
        Star s = star;
        if (s != null) {
            tag.putInt("site", ShootingStars.SITES.indexOf(s.site));
            tag.putInt("size", s.size);
            tag.putInt("worn", s.worn);
            tag.putInt("x", s.pos.getX());
            tag.putInt("y", s.pos.getY());
            tag.putInt("z", s.pos.getZ());
            if (s.discoverer != null) {
                tag.putUUID("discoverer", s.discoverer);
            }
        }
        CompoundTag bonus = new CompoundTag();
        BONUS.forEach((id, n) -> {
            if (n > 0) {
                bonus.putInt(id.toString(), n);
            }
        });
        replace(state.section(SECTION), tag);
        replace(state.section(BONUS_SECTION), bonus);
    }

    private static void replace(CompoundTag target, CompoundTag with) {
        for (String key : new java.util.ArrayList<>(target.getAllKeys())) {
            target.remove(key);
        }
        target.merge(with);
    }

    private static boolean isStar(BlockState state) {
        return SkillingUtil.blockId(state).equals(ShootingStars.STAR);
    }

    private static void crash(MinecraftServer server, ThreadLocalRandom random) {
        ServerLevel world = Gielinor.world(server);
        BlockState block = SkillingUtil.moduleBlock(ShootingStars.STAR);
        if (world == null || block == null) {
            return;
        }
        if (star != null && isStar(world.getBlockState(star.pos))) {
            world.setBlock(star.pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        ShootingStars.Site site = ShootingStars.nextSite(lastSite, random.nextDouble());
        lastSite = site;
        int x = StructureDef.bx(site.x());
        int z = StructureDef.bz(site.y());
        world.getChunk(x >> 4, z >> 4);
        StructurePlacer.applyIfLoaded(world, x >> 4, z >> 4);
        BlockPos pos = Gielinor.safeSpot(world, x, z);
        world.setBlock(pos, block, Block.UPDATE_ALL);
        star = new Star(pos, site, ShootingStars.landingSize(random.nextDouble()));
        server.getPlayerList().broadcastSystemMessage(Component.literal("A shooting star has crashed in " + site.region() + ": "
                + site.name() + ".").withStyle(ChatFormatting.GOLD), false);
    }

    private static void mine(ServerPlayer player, BlockPos pos) {
        ServerLevel world = (ServerLevel) player.level();
        if (star == null || !star.pos.equals(pos)) {
            // A star left over from before a restart has burnt out.
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            Msg.game(player, "The star crumbles away.");
            return;
        }
        if (player.isShiftKeyDown()) {
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
            boolean roll(ServerPlayer p, RandomSource random) {
                Star s = star;
                if (s == null || !s.pos.equals(pos) || Tools.bestPickaxe(p, Players.data(p).skills()) == null
                        || SkillingUtil.level(p, Skill.MINING) < ShootingStars.level(s.size)) {
                    return false;
                }
                s.lastMined = ActionManager.gameTick();
                p.level().playSound(null, pos, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 1.0f, 0.9f);
                if (random.nextDouble() >= Chance.lerp(SkillingUtil.level(p, Skill.MINING), ShootingStars.LOW, ShootingStars.HIGH)) {
                    return true;
                }
                if (s.discoverer == null) {
                    s.discoverer = p.getUUID();
                    BONUS.put(p.getUUID(), ShootingStars.DISCOVERY_BONUS);
                    Msg.red(p, "Congratulations! You were the first person to find this star!");
                }
                int dust = 1;
                int bonus = BONUS.getOrDefault(p.getUUID(), 0);
                if (bonus > 0) {
                    dust++;
                    BONUS.put(p.getUUID(), bonus - 1);
                }
                SkillingUtil.give(p, ShootingStars.STARDUST, dust);
                XpService.add(p, Skill.MINING, ShootingStars.XP);
                return true;
            }
        });
    }

    private static void dusuri(ServerPlayer player, QuestNpcEntity npc) {
        int n = ShootingStars.SHOP.size();
        int index = SELECTED.getOrDefault(player.getUUID(), 0) % n;
        if (player.isShiftKeyDown() || !SELECTED.containsKey(player.getUUID())) {
            if (SELECTED.containsKey(player.getUUID())) {
                index = (index + 1) % n;
            }
            SELECTED.put(player.getUUID(), index);
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
            RandomSource random = player.getRandom();
            for (int i = 0; i < r.count(); i++) {
                SkillingUtil.roll(player, ShootingStars.BAG_OF_GEMS, random);
            }
        } else {
            SkillingUtil.give(player, r.give(), r.count());
        }
        Msg.game(player, "Dusuri: A fine choice. May the stars guide you.");
    }
}
