package com.runecraft.skill;

import com.runecraft.action.Action;
import com.runecraft.action.ActionManager;
import com.runecraft.block.FishingSpotBlock;
import com.runecraft.block.ResourceBlock;
import com.runecraft.block.RockBlock;
import com.runecraft.block.TreeLogBlock;
import com.runecraft.collection.CollectionLog;
import com.runecraft.osrs.Chance;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.osrs.TreeTimers;
import com.runecraft.osrs.data.Fish;
import com.runecraft.osrs.data.FishingMethod;
import com.runecraft.osrs.data.FishingSpot;
import com.runecraft.osrs.data.Gem;
import com.runecraft.osrs.data.Rock;
import com.runecraft.osrs.data.SpecialTool;
import com.runecraft.osrs.data.ToolTier;
import com.runecraft.osrs.data.Tree;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import java.util.List;
import java.util.Locale;

/** Woodcutting, Mining and Fishing. */
public final class Gathering {
    private static final double REACH_SQ = 5.0 * 5.0;
    /** Forestry tree timers, shared by every tree in the world (see {@link TreeTimers}). */
    public static final TreeTimers TREE_TIMERS = new TreeTimers();

    private Gathering() {
    }

    // ---------------- Woodcutting ----------------

    public static void woodcut(ServerPlayerEntity player, BlockPos pos, Tree tree) {
        if (!Stun.check(player)) {
            return;
        }
        SkillSet skills = Players.data(player).skills();
        if (skills.currentLevel(Skill.WOODCUTTING) < tree.level()) {
            Msg.game(player, "You need a Woodcutting level of " + tree.level() + " to chop down this tree.");
            return;
        }
        ToolTier axe = Tools.bestAxe(player, skills);
        if (axe == null) {
            Msg.game(player, Tools.anyOwned(player, ItemDef.Kind.AXE) != null
                    ? "You do not have an axe which you have the Woodcutting level to use."
                    : "You do not have an axe which you have the woodcutting level to use.");
            return;
        }
        if (!Inv.hasRoomFor(player, tree.logItem())) {
            Msg.game(player, "Your inventory is too full to hold any more " + tree.logName().toLowerCase(Locale.ROOT) + ".");
            return;
        }
        Msg.action(player, "You swing your axe at the tree.");
        ActionManager.start(player, new GatherAction(pos, 4) {
            @Override
            boolean valid(ServerPlayerEntity p, BlockState state) {
                return state.getBlock() instanceof TreeLogBlock b && b.tree() == tree && !state.get(ResourceBlock.DEPLETED);
            }

            @Override
            boolean roll(ServerPlayerEntity p, ServerWorld world, BlockState state, Random random) {
                SkillSet s = Players.data(p).skills();
                Tools.Tool best = Tools.bestAxeTool(p, s);
                if (best == null) {
                    return false;
                }
                world.playSound(null, pos, SoundEvents.BLOCK_WOOD_HIT, SoundCategory.PLAYERS, 1.0f, 0.8f + random.nextFloat() * 0.4f);
                double chance = Chance.lerp(s.currentLevel(Skill.WOODCUTTING), tree.low() * best.woodcuttingMultiplier(),
                        tree.high() * best.woodcuttingMultiplier());
                String timerKey = treeKey(world, pos);
                boolean expired = TREE_TIMERS.chop(timerKey, tree.despawnTicks(), ActionManager.gameTick());
                if (random.nextDouble() < chance) {
                    if (!Inv.hasRoomFor(p, tree.logItem())) {
                        Msg.game(p, "Your inventory is too full to hold any more logs.");
                        return false;
                    }
                    if (best.itemId().equals("infernal_axe") && random.nextDouble() < SpecialTool.INFERNAL_CHANCE) {
                        // The infernal axe burns a third of the logs it cuts, for half the Firemaking XP.
                        Msg.action(p, "The infernal axe incinerates some " + tree.logName().toLowerCase(Locale.ROOT) + ".");
                        XpService.add(p, Skill.FIREMAKING, SpecialTool.infernalFiremakingXp(tree));
                        world.spawnParticles(net.minecraft.particle.ParticleTypes.FLAME, p.getX(), p.getY() + 1, p.getZ(), 6,
                                0.2, 0.3, 0.2, 0.01);
                    } else {
                        Inv.give(p, tree.logItem(), 1);
                        CollectionLog.obtain(p, tree.logItem());
                        Msg.action(p, "You get some " + tree.logName().toLowerCase(Locale.ROOT) + ".");
                    }
                    XpService.add(p, Skill.WOODCUTTING, tree.xp());
                    birdNest(p, random);
                    if (expired) {
                        TREE_TIMERS.reset(timerKey);
                        ((ResourceBlock) state.getBlock()).deplete(world, pos, state, tree.respawnTicks());
                        world.playSound(null, pos, SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.BLOCKS, 1.0f, 0.8f);
                        return false;
                    }
                }
                return true;
            }
        });
    }

    /** Key identifying a tree block for {@link #TREE_TIMERS}. */
    public static String treeKey(ServerWorld world, BlockPos pos) {
        return world.getRegistryKey().getValue() + "@" + pos.asLong();
    }

    private static void birdNest(ServerPlayerEntity player, Random random) {
        if (random.nextInt(256) == 0) {
            Msg.red(player, "A bird's nest falls out of the tree.");
            String seed = Drops.rollSeed(random);
            Inv.give(player, seed, 1);
        }
    }

    // ---------------- Mining ----------------

    public static void mine(ServerPlayerEntity player, BlockPos pos, Rock rock) {
        if (!Stun.check(player)) {
            return;
        }
        SkillSet skills = Players.data(player).skills();
        if (skills.currentLevel(Skill.MINING) < rock.level()) {
            Msg.game(player, "You need a Mining level of " + rock.level() + " to mine this rock.");
            return;
        }
        ToolTier pick = Tools.bestPickaxe(player, skills);
        if (pick == null) {
            Msg.game(player, "You need a pickaxe to mine this rock. You do not have a pickaxe which you have the Mining level to use.");
            return;
        }
        String item = rock.oreItem() == null ? "uncut_sapphire" : rock.oreItem();
        if (!Inv.hasRoomFor(player, item)) {
            Msg.game(player, "Your inventory is too full to hold any more " + rock.oreName().toLowerCase(Locale.ROOT) + ".");
            return;
        }
        Msg.action(player, "You swing your pickaxe at the rock.");
        ActionManager.start(player, new GatherAction(pos, pick.miningTicks()) {
            @Override
            boolean valid(ServerPlayerEntity p, BlockState state) {
                return state.getBlock() instanceof RockBlock b && b.rock() == rock && !state.get(ResourceBlock.DEPLETED);
            }

            @Override
            int interval(ServerPlayerEntity p) {
                // Dragon/infernal pickaxes have a 1/6 chance of a 2-tick roll, the crystal pickaxe 1/4.
                Tools.Tool best = Tools.bestPickaxeTool(p, Players.data(p).skills());
                return best == null ? 8 : best.miningTicks(p.getRandom().nextDouble());
            }

            @Override
            boolean roll(ServerPlayerEntity p, ServerWorld world, BlockState state, Random random) {
                SkillSet s = Players.data(p).skills();
                if (Tools.bestPickaxe(p, s) == null) {
                    return false;
                }
                world.playSound(null, pos, SoundEvents.BLOCK_STONE_HIT, SoundCategory.PLAYERS, 1.0f, 0.8f + random.nextFloat() * 0.4f);
                double chance = Chance.lerp(s.currentLevel(Skill.MINING), rock.low(), rock.high());
                if (random.nextDouble() >= chance) {
                    return true;
                }
                String mined = rock.oreItem() != null ? rock.oreItem() : rollGemRock(random);
                if (!Inv.hasRoomFor(p, mined)) {
                    Msg.game(p, "Your inventory is too full to hold any more ore.");
                    return false;
                }
                Tools.Tool pickaxe = Tools.bestPickaxeTool(p, s);
                com.runecraft.osrs.data.Bar smelted = SpecialTool.infernalBar(mined);
                if (pickaxe != null && pickaxe.itemId().equals("infernal_pickaxe") && smelted != null
                        && random.nextDouble() < SpecialTool.INFERNAL_CHANCE) {
                    // The infernal pickaxe smelts a third of the ore it mines, for half the bar's Smithing XP.
                    Inv.give(p, smelted.itemId(), 1);
                    CollectionLog.obtain(p, smelted.itemId());
                    XpService.add(p, Skill.SMITHING, SpecialTool.infernalSmithingXp(mined));
                    Msg.action(p, "Your infernal pickaxe smelts the ore into " + Msg.article(smelted.displayName().toLowerCase(Locale.ROOT)) + ".");
                } else {
                    Inv.give(p, mined, 1);
                    CollectionLog.obtain(p, mined);
                    ItemDef def = ItemCatalog.get(mined);
                    Msg.action(p, rock == Rock.RUNE_ESSENCE || rock == Rock.PURE_ESSENCE ? "You mine an unbound rune stone."
                            : "You manage to mine some " + (def == null ? "ore" : def.name().toLowerCase(Locale.ROOT)
                            .replace(" ore", "")) + ".");
                }
                XpService.add(p, Skill.MINING, rock.xp());
                // Random gems while mining ore (1/256, not from essence or gem rocks): sapphire 1/2,
                // emerald 1/4, ruby 1/8, diamond 1/8.
                if (rock.oreItem() != null && rock != Rock.RUNE_ESSENCE && rock != Rock.PURE_ESSENCE && random.nextInt(256) == 0) {
                    String gem = rollMiningGem(random);
                    Inv.give(p, gem, 1);
                    Msg.red(p, "You just found " + com.runecraft.util.Msg.article(ItemCatalog.get(gem).name()) + "!");
                }
                if (rock.depletes()) {
                    ((ResourceBlock) state.getBlock()).deplete(world, pos, state, rock.respawnTicks());
                    return false;
                }
                return true;
            }
        });
    }

    private static String rollMiningGem(Random random) {
        int r = random.nextInt(8);
        Gem gem = r < 4 ? Gem.SAPPHIRE : r < 6 ? Gem.EMERALD : r < 7 ? Gem.RUBY : Gem.DIAMOND;
        return gem.uncutItem();
    }

    private static String rollGemRock(Random random) {
        int r = random.nextInt(128);
        Gem gem;
        if (r < 60) {
            gem = Gem.OPAL;
        } else if (r < 90) {
            gem = Gem.JADE;
        } else if (r < 105) {
            gem = Gem.RED_TOPAZ;
        } else if (r < 114) {
            gem = Gem.SAPPHIRE;
        } else if (r < 119) {
            gem = Gem.EMERALD;
        } else if (r < 124) {
            gem = Gem.RUBY;
        } else {
            gem = Gem.DIAMOND;
        }
        return gem.uncutItem();
    }

    // ---------------- Fishing ----------------

    public static void fish(ServerPlayerEntity player, BlockPos pos, FishingSpot spot) {
        if (!Stun.check(player)) {
            return;
        }
        FishingMethod method = chooseMethod(player, spot);
        if (method == null) {
            FishingMethod any = spot.catches().keySet().iterator().next();
            Msg.game(player, "You need " + Msg.article(ItemCatalog.get(any.tool()).name().toLowerCase(Locale.ROOT))
                    + " to fish here.");
            return;
        }
        SkillSet skills = Players.data(player).skills();
        List<Fish> fish = spot.catches().get(method);
        int lowest = fish.stream().mapToInt(Fish::level).min().orElse(1);
        if (skills.currentLevel(Skill.FISHING) < lowest) {
            Msg.game(player, "You need a Fishing level of at least " + lowest + " to fish here.");
            return;
        }
        if (method.bait() != null && !Inv.has(player, method.bait(), 1)) {
            Msg.game(player, "You don't have any " + baitName(method) + " left.");
            return;
        }
        Msg.action(player, switch (method) {
            case NET -> "You cast out your net...";
            case BAIT, DARK_BAIT -> "You cast out your line...";
            case LURE -> "You cast out your line...";
            case CAGE -> "You attempt to catch a lobster.";
            case HARPOON -> "You start harpooning fish.";
        });
        ActionManager.start(player, new GatherAction(pos, 5) {
            @Override
            boolean valid(ServerPlayerEntity p, BlockState state) {
                return state.getBlock() instanceof FishingSpotBlock b && b.spot() == spot;
            }

            @Override
            boolean roll(ServerPlayerEntity p, ServerWorld world, BlockState state, Random random) {
                if (!hasTool(p, method)) {
                    return false;
                }
                if (FishingSpots.tick(world, pos, state) != null) {
                    Msg.game(p, "The fishing spot has moved.");
                    return false;
                }
                if (method.bait() != null && !Inv.has(p, method.bait(), 1)) {
                    Msg.game(p, "You don't have any " + baitName(method) + " left.");
                    return false;
                }
                int level = Players.data(p).skills().currentLevel(Skill.FISHING);
                world.playSound(null, pos, SoundEvents.ENTITY_FISHING_BOBBER_SPLASH, SoundCategory.PLAYERS, 0.4f, 1.0f);
                // Dragon/infernal harpoons catch 20% faster, the crystal harpoon 35%.
                String harpoon = method == FishingMethod.HARPOON ? Tools.bestHarpoon(p, Players.data(p).skills()) : null;
                double boost = harpoon == null ? 1.0 : SpecialTool.harpoonMultiplier(harpoon);
                for (Fish f : fish) {
                    if (level >= f.level() && random.nextDouble() < Chance.lerp(level, f.low(), f.high()) * boost) {
                        if (!Inv.hasRoomFor(p, f.rawItem())) {
                            Msg.game(p, "You can't carry any more fish.");
                            return false;
                        }
                        if (method.bait() != null) {
                            Inv.remove(p, method.bait(), 1);
                        }
                        CollectionLog.obtain(p, f.rawItem());
                        if ("infernal_harpoon".equals(harpoon) && random.nextDouble() < SpecialTool.INFERNAL_CHANCE) {
                            // The infernal harpoon cooks a third of its catches, for half the Cooking XP.
                            Inv.give(p, f.cookedItem(), 1);
                            XpService.add(p, Skill.COOKING, SpecialTool.infernalCookingXp(f));
                            Msg.action(p, "Your infernal harpoon cooks the " + f.cookedName().toLowerCase(Locale.ROOT) + ".");
                        } else {
                            Inv.give(p, f.rawItem(), 1);
                            Msg.action(p, "You catch " + (f == Fish.SHRIMPS || f == Fish.ANCHOVIES ? "some " : "a ")
                                    + f.cookedName().toLowerCase(Locale.ROOT) + ".");
                        }
                        XpService.add(p, Skill.FISHING, f.xp());
                        if (random.nextInt(1000) == 0) {
                            Msg.red(p, "You feel something weird sneaking into your backpack.");
                            Inv.give(p, "coins", 1000);
                        }
                        return true;
                    }
                }
                return true;
            }
        });
    }

    private static String baitName(FishingMethod method) {
        ItemDef def = ItemCatalog.get(method.bait());
        return def != null ? def.name().toLowerCase(Locale.ROOT) : "feathers";
    }

    /** Uses the method matching the tool in hand, otherwise the first method whose tool you carry. */
    private static FishingMethod chooseMethod(ServerPlayerEntity player, FishingSpot spot) {
        ItemStack held = player.getMainHandStack();
        String heldId = held.isEmpty() ? "" : Inv.idOf(held);
        for (FishingMethod m : spot.catches().keySet()) {
            if (m.tool().equals(heldId) || (m == FishingMethod.HARPOON && SpecialTool.isHarpoon(heldId) && hasTool(player, m))) {
                return m;
            }
        }
        for (FishingMethod m : FishingMethod.values()) {
            if (spot.catches().containsKey(m) && hasTool(player, m)) {
                return m;
            }
        }
        return null;
    }

    /** Whether the player carries a tool for a method (any usable harpoon works for harpooning). */
    private static boolean hasTool(ServerPlayerEntity player, FishingMethod method) {
        if (method == FishingMethod.HARPOON) {
            return Tools.bestHarpoon(player, Players.data(player).skills()) != null;
        }
        return Inv.has(player, method.tool(), 1);
    }

    /** Shared logic for a gathering action at a block: validity, reach and a roll every N game ticks. */
    abstract static class GatherAction implements Action {
        final BlockPos pos;
        private final int baseInterval;
        private int counter;
        private int target;

        GatherAction(BlockPos pos, int interval) {
            this.pos = pos;
            this.baseInterval = interval;
        }

        abstract boolean valid(ServerPlayerEntity player, BlockState state);

        abstract boolean roll(ServerPlayerEntity player, ServerWorld world, BlockState state, Random random);

        int interval(ServerPlayerEntity player) {
            return baseInterval;
        }

        @Override
        public boolean tick(ServerPlayerEntity player, long gameTick) {
            ServerWorld world = (ServerWorld) player.getWorld();
            BlockState state = world.getBlockState(pos);
            if (!valid(player, state) || player.squaredDistanceTo(net.minecraft.util.math.Vec3d.ofCenter(pos)) > REACH_SQ) {
                return false;
            }
            player.swingHand(Hand.MAIN_HAND, true);
            if (target <= 0) {
                target = interval(player);
            }
            counter++;
            if (counter < target) {
                return true;
            }
            counter = 0;
            target = 0;
            return roll(player, world, state, world.getRandom());
        }
    }

}
