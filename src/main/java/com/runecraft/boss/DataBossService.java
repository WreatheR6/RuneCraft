package com.runecraft.boss;

import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.bosses.BossLair;
import com.runecraft.osrs.content.bosses.BossVariant;
import com.runecraft.registry.ModEntities;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Spawning data-driven bosses and building their arenas. */
public final class DataBossService {
    /** Extra conditions modules can add before a boss may be summoned (e.g. quest requirements). */
    private static final java.util.Map<String, BiPredicate<ServerPlayer, BossDef>> REQUIREMENTS = new java.util.HashMap<>();

    /** Picks the variant a summon starts (awakened boss, delve level); returning null starts the ordinary fight. */
    private static final java.util.Map<String, BiFunction<ServerPlayer, BossDef, BossVariant>> VARIANTS = new java.util.HashMap<>();
    /** Called after a boss's kill has been credited (drops, pet and kill count done). */
    private static final java.util.Map<String, java.util.List<BiConsumer<ServerPlayer, DataBossEntity>>> KILLED =
            new java.util.HashMap<>();

    /**
     * Rolls a boss's kill loot instead of the plain drop table (the Desert Treasure II bosses' roll-until-success order and
     * perfect kills): {@code (killer, boss, table) -> items}.
     */
    public interface LootRoller {
        java.util.List<com.runecraft.skill.Drops.Rolled> roll(ServerPlayer killer, DataBossEntity boss,
                java.util.List<com.runecraft.osrs.data.Drop> table);
    }

    private static final java.util.Map<String, LootRoller> LOOT = new java.util.HashMap<>();

    /** Bosses fought beside a main boss whose kill they are part of (see {@link #companion}). */
    private static final java.util.Set<String> COMPANIONS = new java.util.HashSet<>();

    /**
     * Marks a boss fought beside a main boss (Eldric beside Branda, Dawn beside Dusk, Nex's generals): its death is part of
     * the main boss's kill, so it counts no kill count and rolls no pet of its own (its drop table, if any, still drops).
     */
    public static void companion(String bossId) {
        COMPANIONS.add(bossId);
    }

    public static boolean isCompanion(String bossId) {
        return COMPANIONS.contains(bossId);
    }

    private DataBossService() {
    }

    public static void loot(String bossId, LootRoller roller) {
        LOOT.put(bossId, roller);
    }

    /**
     * The loot of a kill of {@code bossId} ({@code boss} is the entity that died: a later form credits the main boss): the
     * boss's roller when it has one, else {@code table} rolled as OSRS rolls a boss's drop table
     * ({@link com.runecraft.osrs.content.bosses.BossLoot}: its 100% lines, tertiaries on their own and its main-table rolls
     * (one, or two for Zulrah, Vorkath...), never nothing from a full table) rather than every line on its own like an ordinary
     * monster's.
     */
    public static java.util.List<com.runecraft.skill.Drops.Rolled> rollLoot(ServerPlayer killer, DataBossEntity boss, String bossId,
            java.util.List<com.runecraft.osrs.data.Drop> table) {
        LootRoller r = LOOT.get(bossId);
        if (r != null) {
            java.util.List<com.runecraft.skill.Drops.Rolled> out = r.roll(killer, boss, table);
            lastRoll = new LootRoll(bossId, out.size(), out.size(), new java.util.ArrayList<>());
            return out;
        }
        net.minecraft.util.RandomSource random = boss.getRandom();
        java.util.List<com.runecraft.osrs.data.Drop> lines = com.runecraft.osrs.content.bosses.BossLoot.pick(bossId, table,
                random::nextInt);
        java.util.List<com.runecraft.skill.Drops.Rolled> out = com.runecraft.skill.Drops.resolve(lines, random);
        lastRoll = new LootRoll(bossId, lines.size(), out.size(), new java.util.ArrayList<>());
        return out;
    }

    /**
     * A kill of {@code bossId} ({@code died} is the entity that died: a later form credits the main boss): rolls its loot
     * ({@link #rollLoot}) and puts it down where the kill leaves it ({@link #lootSpot}), each item through the drop
     * transformers (ring of wealth coins, noted drops from diaries...) and into the killer's collection log. The item entities
     * are kept with the roll ({@link LootRoll#dropped}).
     */
    public static void dropKillLoot(ServerPlayer killer, DataBossEntity died, String bossId,
            java.util.List<com.runecraft.osrs.data.Drop> table) {
        net.minecraft.world.entity.Entity at = lootSpot(bossId, died, killer);
        java.util.List<com.runecraft.skill.Drops.Rolled> loot = rollLoot(killer, died, bossId, table);
        LootRoll roll = lastRoll;
        for (com.runecraft.skill.Drops.Rolled r : loot) {
            // laid still on its tile (a big boss's south-westernmost one), the killer's for 60 s (round 43, as every monster drop)
            com.runecraft.combat.LootDrops.spawn(at, com.runecraft.skill.Drops.withDoses(com.runecraft.combat.CombatHooks.dropStack(
                    killer, died, r.item(), r.count()), r.doses()), killer, roll.dropped()::add);
            com.runecraft.collection.CollectionLog.obtain(killer, r.item());
        }
    }

    /**
     * What the last boss kill's loot roll landed on: drop lines and the items they gave, and the item entities that loot became
     * on the ground ({@link #dropKillLoot}; empty for loot handed over otherwise). Fewer items than lines means a shared table's
     * "nothing" slot (OSRS: the gem drop table gives nothing 63/128 of the time). Read by the fight self-test to tell an empty
     * roll from loot that didn't land where the player can see it, and to say where each stack went.
     */
    public record LootRoll(String bossId, int lines, int items, java.util.List<net.minecraft.world.entity.item.ItemEntity> dropped) {
    }

    private static LootRoll lastRoll;

    public static LootRoll lastRoll() {
        return lastRoll;
    }

    /** Bosses whose kill loot is received under the killer instead of where the boss died ({@link #lootUnderKiller}). */
    private static final java.util.Set<String> LOOT_UNDER_KILLER = new java.util.HashSet<>();

    /**
     * Marks a boss whose kill loot appears under the player who killed it, not where it died (OSRS Wiki, Zulrah: "the drops
     * will be received under the player, much like cave krakens"; Zulrah dies out in its swamp). Every form crediting the
     * boss's kill drops there too.
     */
    public static void lootUnderKiller(String bossId) {
        LOOT_UNDER_KILLER.add(bossId);
    }

    public static boolean lootsUnderKiller(String bossId) {
        return LOOT_UNDER_KILLER.contains(bossId);
    }

    /**
     * Where a kill of {@code bossId} leaves its loot: under the killer for a {@link #lootUnderKiller} boss while they are in
     * its fight (within its lair's reach and {@link BossLair#PRESENCE_RANGE} blocks of it, the range in which a player keeps a
     * boss's fight going), else where the entity that died ({@code died}) stands. OSRS hands Zulrah's drops out under a player
     * standing in its shrine; a kill credited once the killer has left (a later form's pending death, a delayed hit) drops
     * where the boss died, like any other boss's.
     */
    public static net.minecraft.world.entity.Entity lootSpot(String bossId, DataBossEntity died, ServerPlayer killer) {
        double range = BossLair.PRESENCE_RANGE;
        boolean inFight = killer != null && killer.level() == died.level() && died.inLairReach(killer)
                && killer.distanceToSqr(died) <= range * range;
        return inFight && LOOT_UNDER_KILLER.contains(bossId) ? killer : died;
    }

    public static void requirement(String bossId, BiPredicate<ServerPlayer, BossDef> check) {
        REQUIREMENTS.put(bossId, check);
    }

    public static void variant(String bossId, BiFunction<ServerPlayer, BossDef, BossVariant> chooser) {
        VARIANTS.put(bossId, chooser);
    }

    public static void onKilled(String bossId, BiConsumer<ServerPlayer, DataBossEntity> listener) {
        KILLED.computeIfAbsent(bossId, k -> new java.util.ArrayList<>()).add(listener);
    }

    static void killed(ServerPlayer player, DataBossEntity boss) {
        BossDef def = boss.def();
        if (def != null) {
            for (BiConsumer<ServerPlayer, DataBossEntity> l : KILLED.getOrDefault(def.id, java.util.List.of())) {
                l.accept(player, boss);
            }
        }
    }

    public static void summon(ServerPlayer player, ServerLevel world, BlockPos pos, BossDef boss) {
        EntityType<DataBossEntity> type = ModEntities.DATA_BOSS_TYPES.get(boss.id);
        if (type == null) {
            return;
        }
        if (!world.getEntities(type, new AABB(pos).inflate(64), e -> e.isAlive()).isEmpty()) {
            Msg.game(player, boss.name + " is already here!");
            return;
        }
        BiPredicate<ServerPlayer, BossDef> req = REQUIREMENTS.get(boss.id);
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
        BiFunction<ServerPlayer, BossDef, BossVariant> chooser = VARIANTS.get(boss.id);
        BossVariant variant = chooser == null ? null : chooser.apply(player, boss);
        BlockPos at = spawnPoint(world, pos, boss, player);
        com.runecraft.osrs.world.ArenaLayout.Arena arena = com.runecraft.osrs.world.ArenaLayout.of(boss.id);
        // a lair boss closes in on a target terrain keeps out of its reach (BossLair), and so do the companions its script
        // raises in onSpawn (Eldric beside Branda): set before that, as they copy its lair rules when they rise
        DataBossEntity entity = spawn(world, at, boss, player,
                arena != null && at.getX() == arena.x() && at.getZ() == arena.z() ? arena.radius() + 8 : 24, true);
        if (entity != null && variant != null) {
            entity.applyVariant(variant);
        }
        world.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.6f, 1.0f);
        Msg.red(player, (variant != null ? variant.name() : boss.name) + " (level-" + (variant != null ? variant.combatLevel()
                : boss.combatLevel) + ") awakens!");
    }

    /**
     * Where a summoned boss appears: the centre of its arena when the stone is its arena's (the stone stands by the north
     * entrance, so a spawn next to it could land in the entrance or outside the ring, and the scripts lay their vents,
     * totems, pools and spots out around where the boss first stands), else three blocks from the stone the way the player
     * faces (stones in hand-built lairs: the Dagannoth Kings', the King Black Dragon's).
     */
    static BlockPos spawnPoint(ServerLevel world, BlockPos stone, BossDef boss, ServerPlayer player) {
        com.runecraft.osrs.world.ArenaLayout.Arena a = com.runecraft.osrs.world.ArenaLayout.of(boss.id);
        if (a != null && Math.hypot(stone.getX() - a.x(), stone.getZ() - a.z()) <= a.radius() + 3) {
            world.getChunk(a.x() >> 4, a.z() >> 4);
            int top = world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, a.x(), a.z());
            // the floor is level with the stone's base; a big gap means something stands over the centre (keep the stone's level)
            return new BlockPos(a.x(), Math.abs(top - stone.getY()) <= 3 ? top : stone.getY(), a.z());
        }
        return stone.above().relative(player.getDirection(), 3);
    }

    /** Spawns a boss at a position, targeting {@code target} (nullable). */
    public static DataBossEntity spawn(ServerLevel world, BlockPos at, BossDef boss, ServerPlayer target) {
        return spawn(world, at, boss, target, 0);
    }

    /**
     * Spawns a boss; with {@code leash > 0} it is kept in a lair around {@code at} ({@link DataBossEntity#markSummoned}: a
     * summoning stone's boss, or a quest boss spawned beside its player with {@link BossLair#QUEST_LEASH}), set before its
     * script's onSpawn so the guards it places belong to the lair. Quest bosses keep their terrain safespots.
     */
    public static DataBossEntity spawn(ServerLevel world, BlockPos at, BossDef boss, ServerPlayer target, int leash) {
        return spawn(world, at, boss, target, leash, false);
    }

    /**
     * As {@link #spawn(ServerLevel, BlockPos, BossDef, ServerPlayer, int)}; {@code breakSafespots} (a summoning stone's boss,
     * {@link DataBossEntity#breakSafespots}) is set with the lair, before the script's onSpawn: a companion raised there
     * ({@code BossFx.spawnBoss}: Eldric beside Branda, Dawn beside Dusk, Rex's brothers) copies the lair rules as they stand
     * then, and one raised before the rule was set kept every terrain safespot in a lair whose boss broke them.
     */
    public static DataBossEntity spawn(ServerLevel world, BlockPos at, BossDef boss, ServerPlayer target, int leash,
            boolean breakSafespots) {
        EntityType<DataBossEntity> type = ModEntities.DATA_BOSS_TYPES.get(boss.id);
        if (type == null) {
            return null;
        }
        DataBossEntity entity = type.create(world);
        if (entity == null) {
            return null;
        }
        entity.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, world.getRandom().nextFloat() * 360f, 0);
        entity.finalizeSpawn(world, world.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
        if (target != null) {
            entity.setTarget(target);
        }
        if (leash > 0) {
            entity.markSummoned(at, leash);
            entity.breakSafespots(breakSafespots);
        }
        world.addFreshEntity(entity);
        entity.script().onSpawn(entity);
        return entity;
    }

    /** Builds a circular arena of the boss's style centred on {@code center} (floor at center.y). */
    public static void buildArena(ServerLevel world, BlockPos center, int radius, String style) {
        BlockState floor;
        BlockState wall;
        BlockState accent;
        switch (style == null ? "stone" : style) {
            case "lava" -> {
                floor = Blocks.BLACKSTONE.defaultBlockState();
                wall = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
                accent = Blocks.MAGMA_BLOCK.defaultBlockState();
            }
            case "ice" -> {
                floor = Blocks.PACKED_ICE.defaultBlockState();
                wall = Blocks.BLUE_ICE.defaultBlockState();
                accent = Blocks.SNOW_BLOCK.defaultBlockState();
            }
            case "crypt" -> {
                floor = Blocks.DEEPSLATE_TILES.defaultBlockState();
                wall = Blocks.CHISELED_DEEPSLATE.defaultBlockState();
                accent = Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState();
            }
            case "jungle" -> {
                floor = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
                wall = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
                accent = Blocks.MOSS_BLOCK.defaultBlockState();
            }
            case "void" -> {
                floor = Blocks.OBSIDIAN.defaultBlockState();
                wall = Blocks.CRYING_OBSIDIAN.defaultBlockState();
                accent = Blocks.PURPUR_BLOCK.defaultBlockState();
            }
            case "water" -> {
                floor = Blocks.PRISMARINE_BRICKS.defaultBlockState();
                wall = Blocks.DARK_PRISMARINE.defaultBlockState();
                accent = Blocks.SEA_LANTERN.defaultBlockState();
            }
            case "sand" -> {
                floor = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
                wall = Blocks.CUT_SANDSTONE.defaultBlockState();
                accent = Blocks.CHISELED_SANDSTONE.defaultBlockState();
            }
            default -> {
                floor = Blocks.STONE_BRICKS.defaultBlockState();
                wall = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
                accent = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
            }
        }
        for (int dx = -radius - 1; dx <= radius + 1; dx++) {
            for (int dz = -radius - 1; dz <= radius + 1; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > radius + 1.5) {
                    continue;
                }
                BlockPos p = center.offset(dx, 0, dz);
                for (int fill = 1; fill <= 3; fill++) {
                    BlockState below = world.getBlockState(p.below(fill));
                    if (below.isAir() || !below.getFluidState().isEmpty()) {
                        world.setBlock(p.below(fill), wall, Block.UPDATE_CLIENTS);
                    }
                }
                boolean ring = d > radius - 0.5;
                world.setBlock(p, ring ? wall : ((dx + dz) % 7 == 0 ? accent : floor), Block.UPDATE_CLIENTS);
                for (int up = 1; up <= 8; up++) {
                    BlockState s = ring && up <= 2 && (Math.abs(dx) > 1 && Math.abs(dz) > 1) ? wall : Blocks.AIR.defaultBlockState();
                    world.setBlock(p.above(up), s, Block.UPDATE_CLIENTS);
                }
            }
        }
    }
}
