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
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Right-click behaviour for catalog items. */
public final class ItemUses {
    private ItemUses() {
    }

    /** A rule that may forbid eating or drinking (e.g. ToA invocations); returns the message to show, or null. */
    public interface ConsumeGuard {
        String blocked(ServerPlayer player, ItemDef def);
    }

    private static final java.util.List<ConsumeGuard> CONSUME_GUARDS = new java.util.ArrayList<>();

    public static void addConsumeGuard(ConsumeGuard guard) {
        CONSUME_GUARDS.add(guard);
    }

    public static boolean hasUse(ItemDef def) {
        return switch (def.kind()) {
            case FOOD, BONES, POTION, GRIMY_HERB, SLAYER_GEM, VIAL -> true;
            case TOOL -> def.id().equals("knife") || def.id().equals("chisel") || def.id().equals("needle")
                    || def.id().equals("tinderbox");
            default -> def.id().equals("vial_of_water") || com.runecraft.quest.QuestItems.hasUse(def.id());
        };
    }

    public static void use(ServerPlayer player, ItemStack stack, InteractionHand hand, ItemDef def) {
        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return;
        }
        // One eat delay for all food and one potion delay for all potions (OSRS), not one per item.
        if (ConsumeCooldowns.blocked(player, ConsumeCooldowns.groupOf(def.id(), def))) {
            return;
        }
        if (com.runecraft.quest.QuestItems.hasUse(def.id())) {
            com.runecraft.quest.QuestItems.use(player, stack, def.id());
            return;
        }
        if (def.kind() == ItemDef.Kind.FOOD || def.kind() == ItemDef.Kind.POTION) {
            for (ConsumeGuard guard : CONSUME_GUARDS) {
                String why = guard.blocked(player, def);
                if (why != null) {
                    Msg.game(player, why);
                    return;
                }
            }
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

    private static void eat(ServerPlayer player, ItemStack stack, ItemDef def) {
        stack.shrink(1);
        player.heal(def.heal());
        player.getFoodData().eat(Math.max(1, def.heal() / 2), 0.3f);
        ConsumeCooldowns.consume(player, def.id());
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5f, 1.0f);
        Msg.action(player, "You eat the " + def.name().toLowerCase(Locale.ROOT) + ". It heals some health.");
        ActionManager.cancel(player);
        com.runecraft.content.GameEvents.fireActivity(player, "eat:" + def.id());
    }

    private static void bury(ServerPlayer player, ItemStack stack, ItemDef def) {
        Bone bone = Bone.byItem(def.id());
        if (bone == null) {
            return;
        }
        player.getCooldowns().addCooldown(stack.getItem(), 2 * ActionManager.MC_TICKS_PER_GAME_TICK);
        stack.shrink(1);
        player.swing(InteractionHand.MAIN_HAND, true);
        XpService.add(player, Skill.PRAYER, bone.xp());
        Msg.action(player, "You dig a hole in the ground... You bury the bones.");
    }

    private static void drink(ServerPlayer player, ItemStack stack, InteractionHand hand, ItemDef def) {
        if (!def.drinkable()) {
            Msg.game(player, "You can't drink that.");
            return;
        }
        Potion potion = null;
        for (Potion p : Potion.values()) {
            if (p.itemId().equals(def.id())) {
                potion = p;
            }
        }
        if (potion == null) {
            if (def.id().equals("antifire_potion") || def.id().equals("extended_antifire")) {
                int minutes = def.id().equals("antifire_potion") ? 6 : 12;
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, minutes * 60 * 20, 0));
                ConsumeCooldowns.consume(player, def.id());
                int used = stack.getDamageValue() + 1;
                Msg.action(player, "You drink some of your antifire potion.");
                if (used >= 4) {
                    player.setItemInHand(hand, Inv.stack("vial", 1));
                } else {
                    stack.setDamageValue(used);
                }
            }
            return;
        }
        PlayerData data = Players.data(player);
        int prayer = potion.apply(data.skills());
        prayer += prayer > 0 ? wrenchBonus(player, 25) : 0;
        if (prayer > 0) {
            // Prayer restores never exceed the (possibly boosted) Prayer level.
            int cap = Math.max(data.skills().level(Skill.PRAYER), data.skills().currentLevel(Skill.PRAYER));
            data.setPrayerPoints(Math.max(data.prayerPoints(), Math.min(cap, data.prayerPoints() + prayer)));
        }
        int heal = potion.heal(data.skills());
        if (heal > 0 && potion.overheals() && com.runecraft.world.Gielinor.is(player)) {
            com.runecraft.combat.Overheal.brew(player, data.skills().level(Skill.HITPOINTS));
        } else if (heal > 0) {
            player.heal(heal);
        }
        if (potion == Potion.ANTIPOISON) {
            player.removeEffect(MobEffects.POISON);
            com.runecraft.combat.PoisonService.cure(player, false, com.runecraft.osrs.Toxins.ANTIPOISON[0], ActionManager.gameTick());
        } else if (potion == Potion.ENERGY) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 60, 0));
        }
        int dosesUsed = stack.getDamageValue() + 1;
        int left = 4 - dosesUsed;
        player.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.5f, 1.0f);
        ConsumeCooldowns.consume(player, def.id());
        Msg.action(player, "You drink some of your " + def.name().toLowerCase(Locale.ROOT) + " potion. "
                + (left > 0 ? "You have " + left + " dose" + (left > 1 ? "s" : "") + " of potion left." : "You have finished your potion."));
        if (left <= 0) {
            player.setItemInHand(hand, Inv.stack("vial", 1));
        } else {
            stack.setDamageValue(dosesUsed);
        }
        ServerNetworking.sync(player);
    }

    private static void clean(ServerPlayer player, ItemStack stack, ItemDef def) {
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
                player.getCooldowns().addCooldown(stack.getItem(), ActionManager.MC_TICKS_PER_GAME_TICK);
                stack.shrink(1);
                Inv.give(player, h.cleanItem(), 1);
                XpService.add(player, Skill.HERBLORE, h.cleanXp());
                Msg.action(player, "You clean the dirt from the " + h.displayName().toLowerCase(Locale.ROOT) + ".");
                return;
            }
        }
    }

    private static void fillVial(ServerPlayer player, ItemStack stack) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(5.0));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.SOURCE_ONLY, player));
        if (hit.getType() == HitResult.Type.BLOCK) {
            FluidState fluid = player.level().getFluidState(hit.getBlockPos());
            if (fluid.is(FluidTags.WATER)) {
                stack.shrink(1);
                Inv.give(player, "vial_of_water", 1);
                player.level().playSound(null, player.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1f, 1f);
                Msg.action(player, "You fill the vial from the water.");
                return;
            }
        }
        Msg.game(player, "Use the vial on some water to fill it.");
    }

    // ---------------- Firemaking ----------------

    private static void lightFire(ServerPlayer player) {
        Tree logs = null;
        int level = Players.data(player).skills().currentLevel(Skill.FIREMAKING);
        ItemStack off = player.getOffhandItem();
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
        BlockPos pos = player.blockPosition();
        if (!player.level().getBlockState(pos).isAir() || !ModBlocks.FIRE.defaultBlockState().canSurvive(player.level(), pos)) {
            Msg.game(player, "You can't light a fire here.");
            return;
        }
        Tree burning = logs;
        Msg.action(player, "You attempt to light the logs.");
        ActionManager.start(player, new Action() {
            @Override
            public boolean tick(ServerPlayer p, long gameTick) {
                if (!Inv.has(p, burning.logItem(), 1)) {
                    return false;
                }
                p.swing(InteractionHand.MAIN_HAND, true);
                int lvl = Players.data(p).skills().currentLevel(Skill.FIREMAKING);
                if (p.getRandom().nextDouble() >= Chance.lerp(lvl, 64, 512)) {
                    return true;
                }
                ServerLevel world = (ServerLevel) p.level();
                BlockPos at = p.blockPosition();
                if (!world.getBlockState(at).isAir()) {
                    return false;
                }
                Inv.remove(p, burning.logItem(), 1);
                world.setBlockAndUpdate(at, ModBlocks.FIRE.defaultBlockState());
                XpService.add(p, Skill.FIREMAKING, burning.firemakingXp());
                Msg.action(p, "The fire catches and the logs begin to burn.");
                // Step west like the game does.
                BlockPos west = at.relative(Direction.WEST);
                BlockState w = world.getBlockState(west);
                if (w.getCollisionShape(world, west).isEmpty() && world.getBlockState(west.above()).getCollisionShape(world, west.above()).isEmpty()) {
                    p.teleportTo(west.getX() + 0.5, p.getY(), west.getZ() + 0.5);
                }
                return false;
            }
        }, true);
    }

    // ---------------- Hunter traps ----------------

    public static void layTrap(ServerPlayer player, ItemStack stack, ItemDef def, BlockPos clicked, Direction side) {
        HunterCreature.Trap trap = def.id().equals("bird_snare") ? HunterCreature.Trap.BIRD_SNARE : HunterCreature.Trap.BOX_TRAP;
        int required = HunterService.levelFor(trap);
        if (Players.data(player).skills().currentLevel(Skill.HUNTER) < required) {
            Msg.game(player, "You need a Hunter level of " + required + " to set up this trap.");
            return;
        }
        BlockPos pos = clicked.relative(side);
        if (side != Direction.UP || !player.level().getBlockState(pos).isAir()) {
            Msg.game(player, "You can't lay a trap here.");
            return;
        }
        if (HunterService.atTrapLimit(player)) {
            return;
        }
        stack.shrink(1);
        player.level().setBlockAndUpdate(pos, (trap == HunterCreature.Trap.BIRD_SNARE ? ModBlocks.BIRD_SNARE : ModBlocks.BOX_TRAP)
                .defaultBlockState());
        HunterService.claim(player, (net.minecraft.server.level.ServerLevel) player.level(), pos, true);
        Msg.action(player, "You set up the " + def.name().toLowerCase(Locale.ROOT) + ".");
    }

    public static void staff(ServerPlayer player) {
        MagicService.autocastAttack(player);
    }


    /**
     * Holy wrench (carried), prayer cape or ring of the gods (i) (worn): the extra prayer points for one dose of a
     * prayer-restoring potion whose restore is {@code percent}% of the Prayer level ({@link com.runecraft.osrs.data.Potion#wrenchBonus}).
     */
    public static int wrenchBonus(ServerPlayer player, int percent) {
        boolean wrench = com.runecraft.util.Inv.has(player, com.runecraft.osrs.data.Potion.HOLY_WRENCH, 1);
        if (!wrench) {
            var sp = com.runecraft.content.systems.SysState.of(player);
            for (String id : com.runecraft.osrs.data.Potion.WRENCH_WORN) {
                wrench |= sp.wearing(id);
            }
            wrench |= com.runecraft.osrs.data.Potion.WRENCH_WORN.contains(com.runecraft.util.Inv.idOf(
                    player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST)));
        }
        return wrench ? com.runecraft.osrs.data.Potion.wrenchBonus(Players.data(player).skills().level(com.runecraft.osrs.Skill.PRAYER), percent) : 0;
    }
}
