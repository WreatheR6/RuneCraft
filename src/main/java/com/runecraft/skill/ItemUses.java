package com.runecraft.skill;

import com.runecraft.action.Action;
import com.runecraft.action.ActionManager;
import com.runecraft.combat.MagicService;
import com.runecraft.combat.Slayer;
import com.runecraft.menu.MenuService;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.Chance;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.data.Bone;
import com.runecraft.osrs.data.Herb;
import com.runecraft.osrs.data.HunterCreature;
import com.runecraft.osrs.data.Potion;
import com.runecraft.osrs.data.Tree;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModBlocks;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.BlockState;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Locale;

/** Right-click behaviour for catalog items. */
public final class ItemUses {
    /** OSRS eating delay: 3 game ticks. */
    private static final int EAT_DELAY = 3 * ActionManager.MC_TICKS_PER_GAME_TICK;

    private ItemUses() {
    }

    public static boolean hasUse(ItemDef def) {
        return switch (def.kind()) {
            case FOOD, BONES, POTION, GRIMY_HERB, SLAYER_GEM, VIAL -> true;
            case TOOL -> def.id().equals("knife") || def.id().equals("chisel") || def.id().equals("needle")
                    || def.id().equals("tinderbox");
            default -> def.id().equals("vial_of_water") || com.runecraft.quest.QuestItems.hasUse(def.id());
        };
    }

    public static void use(ServerPlayerEntity player, ItemStack stack, Hand hand, ItemDef def) {
        if (player.getItemCooldownManager().isCoolingDown(stack.getItem())) {
            return;
        }
        if (com.runecraft.quest.QuestItems.hasUse(def.id())) {
            com.runecraft.quest.QuestItems.use(player, stack, def.id());
            return;
        }
        switch (def.kind()) {
            case FOOD -> eat(player, stack, def);
            case BONES -> bury(player, stack, def);
            case POTION -> drink(player, stack, hand, def);
            case GRIMY_HERB -> clean(player, stack, def);
            case SLAYER_GEM -> Slayer.checkTask(player);
            case VIAL -> fillVial(player, stack);
            default -> {
                switch (def.id()) {
                    case "knife" -> MenuService.open(player, MenuType.FLETCHING, null, false);
                    case "chisel" -> MenuService.open(player, MenuType.GEM_CUTTING, null, false);
                    case "needle" -> MenuService.open(player, MenuType.LEATHERWORKING, null, false);
                    case "vial_of_water" -> MenuService.open(player, MenuType.HERBLORE, null, false);
                    case "tinderbox" -> lightFire(player);
                    default -> {
                    }
                }
            }
        }
    }

    private static void eat(ServerPlayerEntity player, ItemStack stack, ItemDef def) {
        stack.decrement(1);
        player.heal(def.heal());
        player.getHungerManager().add(Math.max(1, def.heal() / 2), 0.3f);
        player.getItemCooldownManager().set(stack.getItem(), EAT_DELAY);
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 0.5f, 1.0f);
        Msg.action(player, "You eat the " + def.name().toLowerCase(Locale.ROOT) + ". It heals some health.");
        ActionManager.cancel(player);
    }

    private static void bury(ServerPlayerEntity player, ItemStack stack, ItemDef def) {
        Bone bone = Bone.byItem(def.id());
        if (bone == null) {
            return;
        }
        stack.decrement(1);
        player.getItemCooldownManager().set(stack.getItem(), 2 * ActionManager.MC_TICKS_PER_GAME_TICK);
        player.swingHand(Hand.MAIN_HAND, true);
        XpService.add(player, Skill.PRAYER, bone.xp());
        Msg.action(player, "You dig a hole in the ground... You bury the bones.");
    }

    private static void drink(ServerPlayerEntity player, ItemStack stack, Hand hand, ItemDef def) {
        Potion potion = null;
        for (Potion p : Potion.values()) {
            if (p.itemId().equals(def.id())) {
                potion = p;
            }
        }
        if (potion == null) {
            return;
        }
        PlayerData data = Players.data(player);
        int prayer = potion.apply(data.skills());
        if (prayer > 0) {
            data.setPrayerPoints(data.prayerPoints() + prayer);
        }
        int heal = potion.heal(data.skills());
        if (heal > 0) {
            player.heal(heal);
        }
        if (potion == Potion.ANTIPOISON) {
            player.removeStatusEffect(StatusEffects.POISON);
        } else if (potion == Potion.ENERGY) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 60, 0));
        }
        int dosesUsed = stack.getDamage() + 1;
        int left = 4 - dosesUsed;
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 0.5f, 1.0f);
        player.getItemCooldownManager().set(stack.getItem(), EAT_DELAY);
        Msg.action(player, "You drink some of your " + def.name().toLowerCase(Locale.ROOT) + " potion. "
                + (left > 0 ? "You have " + left + " dose" + (left > 1 ? "s" : "") + " of potion left." : "You have finished your potion."));
        if (left <= 0) {
            player.setStackInHand(hand, Inv.stack("vial", 1));
        } else {
            stack.setDamage(dosesUsed);
        }
        ServerNetworking.sync(player);
    }

    private static void clean(ServerPlayerEntity player, ItemStack stack, ItemDef def) {
        if (!Players.data(player).questComplete(com.runecraft.osrs.Quest.DRUIDIC_RITUAL)) {
            Msg.game(player, "You need to complete the Druidic Ritual quest to use the Herblore skill.");
            return;
        }
        for (Herb h : Herb.values()) {
            if (h.grimyItem().equals(def.id())) {
                if (Players.data(player).skills().currentLevel(Skill.HERBLORE) < h.cleanLevel()) {
                    Msg.game(player, "You need level " + h.cleanLevel() + " Herblore to clean the " + h.displayName() + ".");
                    return;
                }
                stack.decrement(1);
                Inv.give(player, h.cleanItem(), 1);
                XpService.add(player, Skill.HERBLORE, h.cleanXp());
                player.getItemCooldownManager().set(stack.getItem(), ActionManager.MC_TICKS_PER_GAME_TICK);
                Msg.action(player, "You clean the dirt from the " + h.displayName().toLowerCase(Locale.ROOT) + ".");
                return;
            }
        }
    }

    private static void fillVial(ServerPlayerEntity player, ItemStack stack) {
        Vec3d eye = player.getEyePos();
        Vec3d end = eye.add(player.getRotationVec(1.0f).multiply(5.0));
        BlockHitResult hit = player.getWorld().raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.SOURCE_ONLY, player));
        if (hit.getType() == HitResult.Type.BLOCK) {
            FluidState fluid = player.getWorld().getFluidState(hit.getBlockPos());
            if (fluid.isIn(FluidTags.WATER)) {
                stack.decrement(1);
                Inv.give(player, "vial_of_water", 1);
                player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.PLAYERS, 1f, 1f);
                Msg.action(player, "You fill the vial from the water.");
                return;
            }
        }
        Msg.game(player, "Use the vial on some water to fill it.");
    }

    // ---------------- Firemaking ----------------

    private static void lightFire(ServerPlayerEntity player) {
        Tree logs = null;
        int level = Players.data(player).skills().currentLevel(Skill.FIREMAKING);
        ItemStack off = player.getOffHandStack();
        for (Tree t : Tree.values()) {
            if (!off.isEmpty() && Inv.idOf(off).equals(t.logItem())) {
                logs = t;
            }
        }
        if (logs == null) {
            for (Tree t : Tree.values()) {
                if (Inv.has(player, t.logItem(), 1) && level >= t.firemakingLevel()) {
                    logs = t;
                }
            }
        }
        if (logs == null) {
            Msg.game(player, "You don't have any logs that you can light.");
            return;
        }
        if (level < logs.firemakingLevel()) {
            Msg.game(player, "You need a Firemaking level of " + logs.firemakingLevel() + " to light these logs.");
            return;
        }
        BlockPos pos = player.getBlockPos();
        if (!player.getWorld().getBlockState(pos).isAir() || !ModBlocks.FIRE.getDefaultState().canPlaceAt(player.getWorld(), pos)) {
            Msg.game(player, "You can't light a fire here.");
            return;
        }
        Tree burning = logs;
        Msg.action(player, "You attempt to light the logs.");
        ActionManager.start(player, new Action() {
            @Override
            public boolean tick(ServerPlayerEntity p, long gameTick) {
                if (!Inv.has(p, burning.logItem(), 1)) {
                    return false;
                }
                p.swingHand(Hand.MAIN_HAND, true);
                int lvl = Players.data(p).skills().currentLevel(Skill.FIREMAKING);
                if (p.getRandom().nextDouble() >= Chance.lerp(lvl, 65, 513)) {
                    return true;
                }
                ServerWorld world = (ServerWorld) p.getWorld();
                BlockPos at = p.getBlockPos();
                if (!world.getBlockState(at).isAir()) {
                    return false;
                }
                Inv.remove(p, burning.logItem(), 1);
                world.setBlockState(at, ModBlocks.FIRE.getDefaultState());
                XpService.add(p, Skill.FIREMAKING, burning.firemakingXp());
                Msg.action(p, "The fire catches and the logs begin to burn.");
                // Step west like the game does.
                BlockPos west = at.offset(Direction.WEST);
                BlockState w = world.getBlockState(west);
                if (w.getCollisionShape(world, west).isEmpty() && world.getBlockState(west.up()).getCollisionShape(world, west.up()).isEmpty()) {
                    p.requestTeleport(west.getX() + 0.5, p.getY(), west.getZ() + 0.5);
                }
                return false;
            }
        }, true);
    }

    // ---------------- Hunter traps ----------------

    public static void layTrap(ServerPlayerEntity player, ItemStack stack, ItemDef def, BlockPos clicked, Direction side) {
        HunterCreature.Trap trap = def.id().equals("bird_snare") ? HunterCreature.Trap.BIRD_SNARE : HunterCreature.Trap.BOX_TRAP;
        int required = HunterService.levelFor(trap);
        if (Players.data(player).skills().currentLevel(Skill.HUNTER) < required) {
            Msg.game(player, "You need a Hunter level of " + required + " to set up this trap.");
            return;
        }
        BlockPos pos = clicked.offset(side);
        if (side != Direction.UP || !player.getWorld().getBlockState(pos).isAir()) {
            Msg.game(player, "You can't lay a trap here.");
            return;
        }
        stack.decrement(1);
        player.getWorld().setBlockState(pos, (trap == HunterCreature.Trap.BIRD_SNARE ? ModBlocks.BIRD_SNARE : ModBlocks.BOX_TRAP)
                .getDefaultState());
        Msg.action(player, "You set up the " + def.name().toLowerCase(Locale.ROOT) + ".");
    }

    public static void staff(ServerPlayerEntity player) {
        MagicService.autocastAttack(player);
    }

}
