package com.runecraft.content.skilling;

import com.runecraft.action.Action;
import com.runecraft.action.ActionManager;
import com.runecraft.combat.PoisonService;
import com.runecraft.combat.PrayerService;
import com.runecraft.content.systems.RunEnergyService;
import com.runecraft.content.systems.SpecialAttackService;
import com.runecraft.content.BlockHooks;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.osrs.content.skilling.ConstructionData;
import com.runecraft.osrs.content.skilling.ConstructionData.Furniture;
import com.runecraft.osrs.content.systems.SpecialAttacks;
import com.runecraft.osrs.data.Bone;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import com.runecraft.platform.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
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
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            ItemStack held = player.getMainHandItem();
            if (held.isEmpty()) {
                return InteractionResult.PASS;
            }
            Furniture f = ConstructionData.byFlatpack(Inv.idOf(held));
            if (f == null) {
                return InteractionResult.PASS;
            }
            if (world.isClientSide() || !(player instanceof ServerPlayer sp)) {
                return InteractionResult.SUCCESS;
            }
            BlockPos target = hit.getBlockPos().relative(hit.getDirection());
            BlockState block = SkillingUtil.moduleBlock(f.block());
            if (block == null || !world.getBlockState(target).canBeReplaced() || !world.mayInteract(sp, target)) {
                Msg.game(sp, "You can't set up the " + SkillingUtil.lower(f.name()) + " there.");
                return InteractionResult.SUCCESS;
            }
            world.setBlockAndUpdate(target, block);
            if (!sp.getAbilities().instabuild) {
                held.shrink(1);
            }
            world.playSound(null, target, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1f, 1f);
            Msg.action(sp, "You set up the " + SkillingUtil.lower(f.name()) + ".");
            return InteractionResult.SUCCESS;
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

    private static int litBurners(ServerLevel world, BlockPos altar) {
        long now = ActionManager.gameTick();
        String worldId = world.dimension().location().toString();
        BURNERS.values().removeIf(l -> l.until() < now);
        int n = 0;
        for (Lit l : BURNERS.values()) {
            if (l.world().equals(worldId) && l.pos().distSqr(altar) <= 64) {
                n++;
            }
        }
        return Math.min(2, n);
    }

    private static void burner(ServerPlayer player, BlockPos pos) {
        ServerLevel world = (ServerLevel) player.level();
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
        BURNERS.put(key, new Lit(world.dimension().location().toString(), pos, ActionManager.gameTick() + 200));
        world.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 10, 0.2, 0.1, 0.2, 0.01);
        world.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1f, 1f);
        Msg.action(player, "You light the incense burner.");
    }

    private static void altar(ServerPlayer player, BlockPos pos, Furniture f) {
        Bone first = Bone.byItem(SkillingUtil.heldId(player));
        if (first == null) {
            int burners = litBurners((ServerLevel) player.level(), pos);
            PrayerService.useAltar(player); // praying at the altar recharges Prayer points
            Msg.game(player, "Use bones on the altar to offer them to the gods (" + (f.power() + burners * 50) + "% Prayer XP).");
            return;
        }
        ActionManager.start(player, new Action() {
            int counter = 2;

            @Override
            public boolean tick(ServerPlayer p, long gameTick) {
                if (++counter < 3) {
                    return true;
                }
                counter = 0;
                ServerLevel world = (ServerLevel) p.level();
                if (p.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) > 25) {
                    return false;
                }
                // The shared Prayer bone offering, with this altar's multiplier (gilded + 2 lit burners = 350%).
                if (!PrayerService.offerHeldBone(p, (f.power() + litBurners(world, pos) * 50) / 100.0)) {
                    return false;
                }
                world.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 6, 0.2, 0.3, 0.2, 0.02);
                return true;
            }
        });
    }

    private static void larder(ServerPlayer player, Furniture f) {
        long now = ActionManager.gameTick();
        Long next = LARDER_COOLDOWN.get(player.getUUID());
        if (next != null && now < next) {
            Msg.game(player, "You've just taken something from the larder.");
            return;
        }
        LARDER_COOLDOWN.put(player.getUUID(), now + 25);
        List<String> items = ConstructionData.larderItems(f.power());
        String item = items.get(player.getRandom().nextInt(items.size()));
        Inv.give(player, item, 1);
        Msg.action(player, "You take " + Msg.article(SkillingUtil.lower(SkillingUtil.name(item))) + " from the larder.");
    }

    private static void bookcase(ServerPlayer player) {
        PlayerData data = Players.data(player);
        Msg.game(player, "You browse your bookcase. You have " + data.questPoints() + " quest points and a total level of "
                + data.skills().totalLevel() + ".");
    }

    private static void pool(ServerPlayer player, BlockPos pos, Furniture f) {
        PlayerData data = Players.data(player);
        SkillSet skills = data.skills();
        int tier = f.power();
        SpecialAttackService.restoreEnergy(player, SpecialAttacks.MAX_ENERGY);
        if (tier >= 2) {
            RunEnergyService.restore(player, 10000);
        }
        if (tier >= 3) {
            data.setPrayerPoints(skills.level(Skill.PRAYER));
        }
        if (tier >= 4) {
            for (Skill s : Skill.values()) {
                if (skills.boost(s) < 0 && s != Skill.HITPOINTS && s != Skill.PRAYER) {
                    skills.setBoost(s, 0);
                }
            }
        }
        if (tier >= 5) {
            player.setHealth(player.getMaxHealth());
            PoisonService.cure(player, true, 0, ActionManager.gameTick());
        }
        ServerLevel world = (ServerLevel) player.level();
        world.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 30, 0.4, 0.2, 0.4, 0.1);
        world.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1f, 1f);
        Msg.action(player, "You drink from the " + SkillingUtil.lower(f.name()) + " and feel refreshed.");
        ServerNetworking.sync(player);
    }

    private static void portal(ServerPlayer player, Furniture f) {
        Landmark l = Landmark.byName(f.destination());
        if (l == null) {
            return;
        }
        Msg.action(player, "You step through the portal.");
        Gielinor.teleport(player, l);
    }
}
