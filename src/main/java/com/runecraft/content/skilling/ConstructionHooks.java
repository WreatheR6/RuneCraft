package com.runecraft.content.skilling;

import com.runecraft.action.Action;
import com.runecraft.action.ActionManager;
import com.runecraft.combat.PoisonService;
import com.runecraft.content.BlockHooks;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.osrs.content.skilling.ConstructionData;
import com.runecraft.osrs.content.skilling.ConstructionData.Furniture;
import com.runecraft.osrs.data.Bone;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Construction: placing flatpacks, and furniture behaviour (bone altars with incense burners,
 * larders, bookcases, restoration pools, portals).
 */
final class ConstructionHooks {
    /** Lit incense burners: position key -> (world id, pos, game tick they go out). */
    private static final Map<String, Lit> BURNERS = new HashMap<>();
    private static final Map<UUID, Long> LARDER_COOLDOWN = new HashMap<>();

    private record Lit(String world, BlockPos pos, long until) {
    }

    private ConstructionHooks() {
    }

    static void init() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (hand != Hand.MAIN_HAND) {
                return ActionResult.PASS;
            }
            ItemStack held = player.getMainHandStack();
            if (held.isEmpty()) {
                return ActionResult.PASS;
            }
            Furniture f = ConstructionData.byFlatpack(Inv.idOf(held));
            if (f == null) {
                return ActionResult.PASS;
            }
            if (world.isClient() || !(player instanceof ServerPlayerEntity sp)) {
                return ActionResult.SUCCESS;
            }
            BlockPos target = hit.getBlockPos().offset(hit.getSide());
            BlockState block = SkillingUtil.moduleBlock(f.block());
            if (block == null || !world.getBlockState(target).isReplaceable() || !world.canPlayerModifyAt(sp, target)) {
                Msg.game(sp, "You can't set up the " + SkillingUtil.lower(f.name()) + " there.");
                return ActionResult.SUCCESS;
            }
            world.setBlockState(target, block);
            if (!sp.getAbilities().creativeMode) {
                held.decrement(1);
            }
            world.playSound(null, target, SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 1f, 1f);
            Msg.action(sp, "You set up the " + SkillingUtil.lower(f.name()) + ".");
            return ActionResult.SUCCESS;
        });
        for (Furniture f : ConstructionData.FURNITURE) {
            switch (f.use()) {
                case ALTAR -> BlockHooks.register(f.block(), (player, pos, state) -> altar(player, pos, f));
                case BURNER -> BlockHooks.register(f.block(), (player, pos, state) -> burner(player, pos));
                case LARDER -> BlockHooks.register(f.block(), (player, pos, state) -> larder(player, f));
                case BOOKCASE -> BlockHooks.register(f.block(), (player, pos, state) -> bookcase(player));
                case POOL -> BlockHooks.register(f.block(), (player, pos, state) -> pool(player, pos, f));
                case PORTAL -> BlockHooks.register(f.block(), (player, pos, state) -> portal(player, f));
                default -> {
                }
            }
        }
    }

    private static int litBurners(ServerWorld world, BlockPos altar) {
        long now = ActionManager.gameTick();
        String worldId = world.getRegistryKey().getValue().toString();
        BURNERS.values().removeIf(l -> l.until() < now);
        int n = 0;
        for (Lit l : BURNERS.values()) {
            if (l.world().equals(worldId) && l.pos().getSquaredDistance(altar) <= 64) {
                n++;
            }
        }
        return Math.min(2, n);
    }

    private static void burner(ServerPlayerEntity player, BlockPos pos) {
        ServerWorld world = (ServerWorld) player.getWorld();
        String key = SkillingUtil.key(world, pos);
        Lit lit = BURNERS.get(key);
        if (lit != null && lit.until() > ActionManager.gameTick()) {
            Msg.game(player, "The incense burner is already lit.");
            return;
        }
        if (!SkillingUtil.need(player, Skill.FIREMAKING, 30, "light the incense burner")
                || !SkillingUtil.needItem(player, "tinderbox", "You need a tinderbox to light the burner.")
                || !SkillingUtil.needItem(player, "marrentill", "You need a clean marrentill to light the incense burner.")) {
            return;
        }
        Inv.remove(player, "marrentill", 1);
        BURNERS.put(key, new Lit(world.getRegistryKey().getValue().toString(), pos, ActionManager.gameTick() + 200));
        world.spawnParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 10, 0.2, 0.1, 0.2, 0.01);
        world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1f, 1f);
        Msg.action(player, "You light the incense burner.");
    }

    private static void altar(ServerPlayerEntity player, BlockPos pos, Furniture f) {
        Bone first = Bone.byItem(SkillingUtil.heldId(player));
        if (first == null) {
            int burners = litBurners((ServerWorld) player.getWorld(), pos);
            Msg.game(player, "Use bones on the altar to offer them to the gods (" + (f.power() + burners * 50) + "% Prayer XP).");
            return;
        }
        ActionManager.start(player, new Action() {
            int counter = 2;

            @Override
            public boolean tick(ServerPlayerEntity p, long gameTick) {
                if (++counter < 3) {
                    return true;
                }
                counter = 0;
                String id = SkillingUtil.heldId(p);
                Bone bone = Bone.byItem(id);
                if (bone == null) {
                    return false;
                }
                ServerWorld world = (ServerWorld) p.getWorld();
                double mult = (f.power() + litBurners(world, pos) * 50) / 100.0;
                p.getMainHandStack().decrement(1);
                p.swingHand(Hand.MAIN_HAND, true);
                XpService.add(p, Skill.PRAYER, bone.xp() * mult);
                world.spawnParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 6, 0.2, 0.3, 0.2, 0.02);
                Msg.action(p, "The gods are pleased with your offering.");
                return true;
            }
        });
    }

    private static void larder(ServerPlayerEntity player, Furniture f) {
        long now = ActionManager.gameTick();
        Long next = LARDER_COOLDOWN.get(player.getUuid());
        if (next != null && now < next) {
            Msg.game(player, "You've just taken something from the larder.");
            return;
        }
        LARDER_COOLDOWN.put(player.getUuid(), now + 25);
        List<String> items = ConstructionData.larderItems(f.power());
        String item = items.get(player.getRandom().nextInt(items.size()));
        Inv.give(player, item, 1);
        Msg.action(player, "You take " + Msg.article(SkillingUtil.lower(SkillingUtil.name(item))) + " from the larder.");
    }

    private static void bookcase(ServerPlayerEntity player) {
        PlayerData data = Players.data(player);
        Msg.game(player, "You browse your bookcase. You have " + data.questPoints() + " quest points and a total level of "
                + data.skills().totalLevel() + ".");
    }

    private static void pool(ServerPlayerEntity player, BlockPos pos, Furniture f) {
        PlayerData data = Players.data(player);
        SkillSet skills = data.skills();
        int tier = f.power();
        player.getHungerManager().add(20, 1.0f);
        if (tier >= 2) {
            for (Skill s : Skill.values()) {
                if (skills.boost(s) < 0 && s != Skill.HITPOINTS && s != Skill.PRAYER) {
                    skills.setBoost(s, 0);
                }
            }
        }
        if (tier >= 3) {
            data.setPrayerPoints(skills.level(Skill.PRAYER));
        }
        if (tier >= 4) {
            PoisonService.cure(player, true, 0, ActionManager.gameTick());
        }
        if (tier >= 5) {
            player.setHealth(player.getMaxHealth());
        }
        ServerWorld world = (ServerWorld) player.getWorld();
        world.spawnParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 30, 0.4, 0.2, 0.4, 0.1);
        world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.BLOCKS, 1f, 1f);
        Msg.action(player, "You drink from the " + SkillingUtil.lower(f.name()) + " and feel refreshed.");
        ServerNetworking.sync(player);
    }

    private static void portal(ServerPlayerEntity player, Furniture f) {
        Landmark l = Landmark.byName(f.destination());
        if (l == null) {
            return;
        }
        Msg.action(player, "You step through the portal.");
        Gielinor.teleport(player, l);
    }
}
