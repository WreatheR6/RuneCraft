package com.runecraft.content.raids;

import com.runecraft.boss.BossScripts;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.ModuleHooks;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.raids.Invocation;
import com.runecraft.osrs.content.raids.RaidScaling;
import com.runecraft.osrs.content.raids.RaidType;
import com.runecraft.osrs.content.raids.RaidsContent;
import com.runecraft.world.LandmarkBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Minecraft-side behaviour for the raids content module: the raid framework (entrances, instances,
 * rooms, deaths, points, rewards), boss scripts for every raid boss, the entrance structures and
 * the {@code /raids} command.
 */
public final class RaidsHooks implements ModuleHooks {
    @Override
    public void init() {
        RaidsContent content = new RaidsContent();
        for (BossDef boss : content.bosses()) {
            BossScripts.register(boss.id, RaidScript::new);
        }
        for (RaidType type : RaidType.values()) {
            BlockHooks.register(type.entranceBlock(), (player, pos, state) -> RaidManager.enter(player, type, pos));
        }
        for (BlockDef block : content.blocks()) {
            if (RaidType.byId(block.id().replace("_entrance", "")) == null) {
                BlockHooks.register(block.id(), (player, pos, state) -> RaidManager.useBlock(player, block.id(), pos));
            }
        }
        RaidManager.register();
        RaidCommands.register();
        registerToaScaling();
        ToaSupplyService.init();
        ToaPuzzleRooms.init();
        CoxSupplyService.init();
        registerNylocasStyles();
        LandmarkBuilder.registerCustom(RaidType.COX.placeId(), RaidsHooks::buildMountQuidamortem);
        LandmarkBuilder.registerCustom(RaidType.TOB.placeId(), RaidsHooks::buildVerSinhaza);
        // The Tombs of Amascut entrance is the Jaltevas Pyramid structure in the Necropolis (EasternDesert.tombsOfAmascut);
        // the old block pyramid built here stood half over it.
    }

    /**
     * ToA raid level (OSRS): +2% creature accuracy and defence per 5 raid levels (attack and defence rolls x (250 +
     * level) / 250), and Overly Draining making every special attack cost 100% energy (halved by liquid adrenaline).
     */
    private static void registerToaScaling() {
        com.runecraft.combat.CombatHooks.addModifier(new com.runecraft.combat.CombatHooks.PlayerAttackModifier() {
            @Override
            public double accuracy(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.entity.LivingEntity target,
                                   com.runecraft.osrs.AttackType type) {
                RaidInstance raid = target == null ? null : RaidManager.at(target);
                if (raid == null || raid.type != RaidType.TOA) {
                    return 1.0;
                }
                // The creature's defence roll grows with the raid level; dividing the attack roll is equivalent.
                return 1.0 / RaidScaling.toaDefence(RaidInstance.creatureId(target), raid.raidLevel());
            }
        });
        com.runecraft.combat.CombatHooks.addNpcAccuracyModifier((mob, type) -> {
            RaidInstance raid = RaidManager.at(mob);
            return raid == null || raid.type != RaidType.TOA || RaidScaling.toaUnscaledDamage(RaidInstance.creatureId(mob)) ? 1.0
                    : RaidScaling.toaAccuracy(raid.raidLevel());
        });
        com.runecraft.content.systems.SpecialAttackService.addCostRule((player, spec) -> {
            RaidInstance raid = RaidManager.of(player);
            if (raid == null || raid.type != RaidType.TOA || !raid.contains(player)) {
                return -1;
            }
            return com.runecraft.osrs.content.raids.ToaSupplies.specialCost(spec.cost(),
                    Invocation.specialCost(raid.invocations()) >= 0, ToaSupplyService.adrenaline(player));
        });
    }

    /** ToB Nylocas waves: note attacks of a style the nylocas is immune to ("Perfect Nylocas"). */
    private static void registerNylocasStyles() {
        com.runecraft.combat.CombatHooks.addModifier(new com.runecraft.combat.CombatHooks.PlayerAttackModifier() {
            @Override
            public int afterRoll(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.entity.LivingEntity target,
                                 com.runecraft.osrs.AttackType type, int damage) {
                TobRooms.Nylocas room = target == null ? null : TobRooms.Nylocas.of(player);
                if (room != null) {
                    room.attacked(target, type);
                }
                return damage;
            }
        });
    }

    /** A rocky mountainside with a glowing cave mouth, north of the plaza. */
    private static void buildMountQuidamortem(ServerLevel world, BlockPos c) {
        BlockPos base = c.offset(0, 0, -16);
        for (int layer = 0; layer < 9; layer++) {
            int r = 10 - layer;
            RaidBuilder.fill(world, base.getX() - r, base.getY() + layer, base.getZ() - r, base.getX() + r, base.getY() + layer,
                    base.getZ() + r, layer % 3 == 2 ? Blocks.COBBLED_DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState());
        }
        RaidBuilder.fill(world, base.getX() - 1, base.getY() + 1, base.getZ() + 4, base.getX() + 1, base.getY() + 3, base.getZ() + 10,
                Blocks.AIR.defaultBlockState());
        RaidBuilder.set(world, base.offset(0, 4, 10), Blocks.SHROOMLIGHT.defaultBlockState());
        for (int i = -1; i <= 1; i += 2) {
            RaidBuilder.set(world, base.offset(i * 3, 1, 10), Blocks.AMETHYST_CLUSTER.defaultBlockState());
        }
    }

    /** Ver Sinhaza: dark stone walls and towers around the Theatre's doors. */
    private static void buildVerSinhaza(ServerLevel world, BlockPos c) {
        BlockPos base = c.offset(0, 0, -14);
        BlockState wall = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        RaidBuilder.fill(world, base.getX() - 9, base.getY() + 1, base.getZ() - 1, base.getX() + 9, base.getY() + 6, base.getZ(), wall);
        RaidBuilder.fill(world, base.getX() - 2, base.getY() + 1, base.getZ() - 1, base.getX() + 2, base.getY() + 4, base.getZ(),
                Blocks.RED_STAINED_GLASS.defaultBlockState());
        for (int i = -1; i <= 1; i += 2) {
            RaidBuilder.fill(world, base.getX() + i * 9 - 1, base.getY() + 1, base.getZ() - 2, base.getX() + i * 9 + 1, base.getY() + 11,
                    base.getZ() + 1, wall);
            RaidBuilder.set(world, base.offset(i * 9, 12, 0), Blocks.REDSTONE_LAMP.defaultBlockState());
            RaidBuilder.set(world, base.offset(i * 4, 7, 0), Blocks.SOUL_LANTERN.defaultBlockState());
        }
    }

}
