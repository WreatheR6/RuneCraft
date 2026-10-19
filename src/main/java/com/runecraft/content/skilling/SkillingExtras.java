package com.runecraft.content.skilling;

import com.runecraft.action.ActionManager;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.systems.SysState;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.Chance;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.skilling.RunecraftData;
import com.runecraft.osrs.content.skilling.ShootingStars;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestScripts;
import com.runecraft.skill.Gathering;
import com.runecraft.skill.Tools;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.platform.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;

/**
 * Round 28 skilling extras: the celestial ring (invisible +4 Mining, a stardust charge per ore for a 1/10 extra ore) and the
 * daeyalt essence mine (the active essence rock, shards traded with Noranna Tytanin for daeyalt essence).
 */
public final class SkillingExtras {
    /** NBT key holding a celestial ring's stardust charges. */
    public static final String CELESTIAL_NBT = "CelestialCharges";

    private SkillingExtras() {
    }

    static void init() {
        Gathering.miningBoost = p -> celestialWorn(p) ? ShootingStars.CELESTIAL_BOOST : 0;
        Gathering.extraOre = SkillingExtras::celestialExtraOre;
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (hand != InteractionHand.MAIN_HAND || stack.isEmpty() || !Inv.idOf(stack).equals(ShootingStars.STARDUST)) {
                return InteractionResultHolder.pass(stack);
            }
            if (world.isClientSide() || !(player instanceof ServerPlayer sp)) {
                return InteractionResultHolder.pass(stack);
            }
            return chargeCelestial(sp) ? InteractionResultHolder.success(stack) : InteractionResultHolder.pass(stack);
        });
        BlockHooks.register(RunecraftData.DAEYALT_ROCK, (player, pos, state) -> mineDaeyalt(player, pos));
        QuestScripts.onTalk(RunecraftData.NORANNA, SkillingExtras::noranna);
    }

    // ------------------------------------------------------------------ celestial ring

    /** The worn celestial ring (charged or not), or an empty stack. */
    private static ItemStack celestialRing(ServerPlayer p) {
        ItemStack ring = SysState.of(p).worn(Jewellery.Slot.RING);
        String id = Inv.idOf(ring);
        return id.equals(ShootingStars.CELESTIAL_RING) || id.equals(ShootingStars.CELESTIAL_RING_UNCHARGED) ? ring : ItemStack.EMPTY;
    }

    public static boolean celestialWorn(ServerPlayer p) {
        return !celestialRing(p).isEmpty();
    }

    public static int celestialCharges(ItemStack ring) {
        CompoundTag nbt = ring.getTag();
        return nbt != null && nbt.contains(CELESTIAL_NBT) ? nbt.getInt(CELESTIAL_NBT) : 0;
    }

    /** Called for every ore mined: a charged ring uses a charge on each ore from a suitable rock, extra ore or not (OSRS). */
    private static boolean celestialExtraOre(ServerPlayer p, String ore) {
        ItemStack ring = celestialRing(p);
        int charges = celestialCharges(ring);
        if (ring.isEmpty() || !ShootingStars.celestialUsesCharge(ore, charges)) {
            return false;
        }
        boolean extra = ShootingStars.celestialExtraOre(ore, charges, p.getRandom().nextDouble());
        int left = charges - 1;
        if (left <= 0) {
            SysState.of(p).worn.put(Jewellery.Slot.RING, Inv.stack(ShootingStars.CELESTIAL_RING_UNCHARGED, 1));
            Msg.red(p, "Your celestial ring has run out of stardust.");
        } else {
            ring.getOrCreateTag().putInt(CELESTIAL_NBT, left);
        }
        return extra;
    }

    /**
     * Using stardust charges the celestial ring worn, or else the first one in the inventory: one charge per stardust, up to
     * 10,000. An uncharged ring becomes a charged one.
     */
    public static boolean chargeCelestial(ServerPlayer p) {
        ItemStack ring = celestialRing(p);
        boolean worn = !ring.isEmpty();
        int slot = -1;
        if (!worn) {
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                String id = Inv.idOf(p.getInventory().getItem(i));
                if (id.equals(ShootingStars.CELESTIAL_RING) || id.equals(ShootingStars.CELESTIAL_RING_UNCHARGED)) {
                    slot = i;
                    ring = p.getInventory().getItem(i);
                    break;
                }
            }
        }
        if (ring.isEmpty()) {
            return false;
        }
        int charges = celestialCharges(ring);
        int add = ShootingStars.celestialCharge(charges, Inv.count(p, ShootingStars.STARDUST));
        if (add <= 0) {
            Msg.game(p, "Your celestial ring can't hold any more stardust.");
            return true;
        }
        Inv.remove(p, ShootingStars.STARDUST, add);
        ItemStack charged = Inv.idOf(ring).equals(ShootingStars.CELESTIAL_RING) ? ring : Inv.stack(ShootingStars.CELESTIAL_RING, 1);
        charged.getOrCreateTag().putInt(CELESTIAL_NBT, charges + add);
        if (charged != ring) {
            if (worn) {
                SysState.of(p).worn.put(Jewellery.Slot.RING, charged);
            } else {
                p.getInventory().setItem(slot, charged);
            }
        }
        Msg.game(p, "You add " + add + " stardust to your celestial ring. It now has " + (charges + add) + " charges.");
        return true;
    }

    // ------------------------------------------------------------------ daeyalt essence mine

    private static int phase(BlockPos pos) {
        return RunecraftData.daeyaltPhase(pos.getX(), pos.getZ());
    }

    private static void mineDaeyalt(ServerPlayer player, BlockPos pos) {
        if (!SkillingUtil.need(player, Skill.MINING, RunecraftData.DAEYALT_MINING_LEVEL, "mine daeyalt essence")) {
            return;
        }
        if (Tools.bestPickaxe(player, Players.data(player).skills()) == null) {
            Msg.game(player, "You need a pickaxe to mine this rock.");
            return;
        }
        if (!RunecraftData.daeyaltActive(ActionManager.gameTick(), phase(pos))) {
            Msg.game(player, "This rock isn't active at the moment. Look for the one with sparkles spiralling around it.");
            return;
        }
        String blockId = SkillingUtil.blockId(player.level().getBlockState(pos));
        Msg.action(player, "You swing your pickaxe at the rock.");
        SkillingUtil.start(player, new SkillingUtil.BlockAction(pos, blockId, 4) {
            @Override
            boolean roll(ServerPlayer p, RandomSource random) {
                if (!RunecraftData.daeyaltActive(ActionManager.gameTick(), phase(pos))) {
                    Msg.game(p, "The rock's sparkle fades: another daeyalt rock is active now.");
                    return false;
                }
                p.level().playSound(null, pos, SoundEvents.STONE_HIT, SoundSource.PLAYERS, 1f, 1f);
                // Success curve approx (OSRS: about 4,000 shards an hour with a dragon pickaxe).
                int level = SkillingUtil.level(p, Skill.MINING) + (celestialWorn(p) ? ShootingStars.CELESTIAL_BOOST : 0);
                if (random.nextDouble() < Chance.lerp(level, 96, 160)) {
                    int shards = RunecraftData.DAEYALT_SHARDS_MIN
                            + random.nextInt(RunecraftData.DAEYALT_SHARDS_MAX - RunecraftData.DAEYALT_SHARDS_MIN + 1);
                    SkillingUtil.give(p, "daeyalt_shard", shards);
                    XpService.add(p, Skill.MINING, RunecraftData.DAEYALT_MINING_XP);
                    Msg.action(p, "You mine " + shards + " daeyalt shards.");
                }
                return true;
            }
        });
    }

    /** Noranna Tytanin trades every daeyalt shard for a daeyalt essence (OSRS sends them to the bank; here, the inventory). */
    private static void noranna(ServerPlayer player, QuestNpcEntity npc) {
        int shards = Inv.count(player, "daeyalt_shard");
        if (shards <= 0) {
            Msg.game(player, "Noranna Tytanin: Bring me daeyalt shards from the essence rocks and I'll give you daeyalt essence"
                    + " for them, one for one.");
            return;
        }
        Inv.remove(player, "daeyalt_shard", shards);
        SkillingUtil.give(player, "daeyalt_essence", shards);
        Msg.game(player, "Noranna Tytanin exchanges your " + shards + " daeyalt shards for daeyalt essence.");
    }
}
