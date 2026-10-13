package com.runecraft.boss;

import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.bosses.BossVariant;
import com.runecraft.registry.ModEntities;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/** Spawning data-driven bosses and building their arenas. */
public final class DataBossService {
    /** Extra conditions modules can add before a boss may be summoned (e.g. quest requirements). */
    private static final java.util.Map<String, BiPredicate<ServerPlayerEntity, BossDef>> REQUIREMENTS = new java.util.HashMap<>();

    /** Picks the variant a summon starts (awakened boss, delve level); returning null starts the ordinary fight. */
    private static final java.util.Map<String, BiFunction<ServerPlayerEntity, BossDef, BossVariant>> VARIANTS = new java.util.HashMap<>();
    /** Called after a boss's kill has been credited (drops, pet and kill count done). */
    private static final java.util.Map<String, java.util.List<BiConsumer<ServerPlayerEntity, DataBossEntity>>> KILLED =
            new java.util.HashMap<>();

    private DataBossService() {
    }

    public static void requirement(String bossId, BiPredicate<ServerPlayerEntity, BossDef> check) {
        REQUIREMENTS.put(bossId, check);
    }

    public static void variant(String bossId, BiFunction<ServerPlayerEntity, BossDef, BossVariant> chooser) {
        VARIANTS.put(bossId, chooser);
    }

    public static void onKilled(String bossId, BiConsumer<ServerPlayerEntity, DataBossEntity> listener) {
        KILLED.computeIfAbsent(bossId, k -> new java.util.ArrayList<>()).add(listener);
    }

    static void killed(ServerPlayerEntity player, DataBossEntity boss) {
        BossDef def = boss.def();
        if (def != null) {
            for (BiConsumer<ServerPlayerEntity, DataBossEntity> l : KILLED.getOrDefault(def.id, java.util.List.of())) {
                l.accept(player, boss);
            }
        }
    }

    public static void summon(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BossDef boss) {
        EntityType<DataBossEntity> type = ModEntities.DATA_BOSS_TYPES.get(boss.id);
        if (type == null) {
            return;
        }
        if (!world.getEntitiesByType(type, new Box(pos).expand(64), e -> e.isAlive()).isEmpty()) {
            Msg.game(player, boss.name + " is already here!");
            return;
        }
        BiPredicate<ServerPlayerEntity, BossDef> req = REQUIREMENTS.get(boss.id);
        if (req != null && !req.test(player, boss)) {
            return;
        }
        if (boss.slayerLevel > 1 && com.runecraft.player.Players.data(player).skills()
                .level(com.runecraft.osrs.Skill.SLAYER) < boss.slayerLevel) {
            Msg.game(player, "You need a Slayer level of " + boss.slayerLevel + " to fight " + boss.name + ".");
            return;
        }
        if (boss.key != null) {
            if (!Inv.has(player, boss.key, 1)) {
                Msg.game(player, "You need " + Msg.article(boss.key.replace('_', ' ')) + " to enter.");
                return;
            }
            Inv.remove(player, boss.key, 1);
        }
        BiFunction<ServerPlayerEntity, BossDef, BossVariant> chooser = VARIANTS.get(boss.id);
        BossVariant variant = chooser == null ? null : chooser.apply(player, boss);
        DataBossEntity entity = spawn(world, pos.up().offset(player.getHorizontalFacing(), 3), boss, player);
        if (entity != null && variant != null) {
            entity.applyVariant(variant);
        }
        world.playSound(null, pos, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 0.6f, 1.0f);
        Msg.red(player, (variant != null ? variant.name() : boss.name) + " (level-" + (variant != null ? variant.combatLevel()
                : boss.combatLevel) + ") awakens!");
    }

    /** Spawns a boss at a position, targeting {@code target} (nullable). */
    public static DataBossEntity spawn(ServerWorld world, BlockPos at, BossDef boss, ServerPlayerEntity target) {
        EntityType<DataBossEntity> type = ModEntities.DATA_BOSS_TYPES.get(boss.id);
        if (type == null) {
            return null;
        }
        DataBossEntity entity = type.create(world);
        if (entity == null) {
            return null;
        }
        entity.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, world.getRandom().nextFloat() * 360f, 0);
        entity.initialize(world, world.getLocalDifficulty(at), SpawnReason.EVENT, null, null);
        if (target != null) {
            entity.setTarget(target);
        }
        world.spawnEntity(entity);
        entity.script().onSpawn(entity);
        return entity;
    }

    /** Builds a circular arena of the boss's style centred on {@code center} (floor at center.y). */
    public static void buildArena(ServerWorld world, BlockPos center, int radius, String style) {
        BlockState floor;
        BlockState wall;
        BlockState accent;
        switch (style == null ? "stone" : style) {
            case "lava" -> {
                floor = Blocks.BLACKSTONE.getDefaultState();
                wall = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
                accent = Blocks.MAGMA_BLOCK.getDefaultState();
            }
            case "ice" -> {
                floor = Blocks.PACKED_ICE.getDefaultState();
                wall = Blocks.BLUE_ICE.getDefaultState();
                accent = Blocks.SNOW_BLOCK.getDefaultState();
            }
            case "crypt" -> {
                floor = Blocks.DEEPSLATE_TILES.getDefaultState();
                wall = Blocks.CHISELED_DEEPSLATE.getDefaultState();
                accent = Blocks.CRACKED_DEEPSLATE_TILES.getDefaultState();
            }
            case "jungle" -> {
                floor = Blocks.MOSSY_COBBLESTONE.getDefaultState();
                wall = Blocks.MOSSY_STONE_BRICKS.getDefaultState();
                accent = Blocks.MOSS_BLOCK.getDefaultState();
            }
            case "void" -> {
                floor = Blocks.OBSIDIAN.getDefaultState();
                wall = Blocks.CRYING_OBSIDIAN.getDefaultState();
                accent = Blocks.PURPUR_BLOCK.getDefaultState();
            }
            case "water" -> {
                floor = Blocks.PRISMARINE_BRICKS.getDefaultState();
                wall = Blocks.DARK_PRISMARINE.getDefaultState();
                accent = Blocks.SEA_LANTERN.getDefaultState();
            }
            case "sand" -> {
                floor = Blocks.SMOOTH_SANDSTONE.getDefaultState();
                wall = Blocks.CUT_SANDSTONE.getDefaultState();
                accent = Blocks.CHISELED_SANDSTONE.getDefaultState();
            }
            default -> {
                floor = Blocks.STONE_BRICKS.getDefaultState();
                wall = Blocks.CRACKED_STONE_BRICKS.getDefaultState();
                accent = Blocks.CHISELED_STONE_BRICKS.getDefaultState();
            }
        }
        for (int dx = -radius - 1; dx <= radius + 1; dx++) {
            for (int dz = -radius - 1; dz <= radius + 1; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > radius + 1.5) {
                    continue;
                }
                BlockPos p = center.add(dx, 0, dz);
                for (int fill = 1; fill <= 3; fill++) {
                    BlockState below = world.getBlockState(p.down(fill));
                    if (below.isAir() || !below.getFluidState().isEmpty()) {
                        world.setBlockState(p.down(fill), wall, Block.NOTIFY_LISTENERS);
                    }
                }
                boolean ring = d > radius - 0.5;
                world.setBlockState(p, ring ? wall : ((dx + dz) % 7 == 0 ? accent : floor), Block.NOTIFY_LISTENERS);
                for (int up = 1; up <= 8; up++) {
                    BlockState s = ring && up <= 2 && (Math.abs(dx) > 1 && Math.abs(dz) > 1) ? wall : Blocks.AIR.getDefaultState();
                    world.setBlockState(p.up(up), s, Block.NOTIFY_LISTENERS);
                }
            }
        }
    }
}
