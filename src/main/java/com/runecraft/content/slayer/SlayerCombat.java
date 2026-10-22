package com.runecraft.content.slayer;

import com.runecraft.action.ActionManager;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.MonsterStats;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.content.systems.SysState;
import com.runecraft.osrs.content.slayer.SlayerGear;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.osrs.data.AmmoRules;
import com.runecraft.player.Players;
import com.runecraft.registry.ModItems;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.platform.ServerTickEvents;
import com.runecraft.platform.UseEntityCallback;
import com.runecraft.platform.UseItemCallback;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Slayer combat mechanics: black mask / slayer helmet bonuses, leaf-bladed weapons, finishing
 * items (rock hammer, bag of salt, ice cooler, fungicide) and protective equipment. Public for the self-test
 * ({@code selftest.SlayerChecks}).
 */
public final class SlayerCombat {
    /** Next game tick each creature may land its "unprotected" special effect. */
    private static final Map<LivingEntity, Long> NEXT_EFFECT = new WeakHashMap<>();

    private SlayerCombat() {
    }

    static void register() {
        CombatHooks.addModifier(new CombatHooks.PlayerAttackModifier() {
            // The black mask / slayer helmet bonus (melee 7/6, imbued ranged and magic 23/20 on task, never with a
            // salve amulet) is part of com.runecraft.osrs.PlayerHitCalc, applied through combat.HitCalcBridge.

            @Override
            public int afterRoll(ServerPlayer player, LivingEntity target, AttackType type, int damage) {
                return SlayerCombat.afterRoll(player, target, type, damage);
            }
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClientSide() || hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer sp)
                    || !(entity instanceof LivingEntity living)) {
                return InteractionResult.PASS;
            }
            return useFinisher(sp, living) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        });
        ServerTickEvents.END_SERVER_TICK.register(SlayerCombat::tick);
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (world.isClientSide() || !(player instanceof ServerPlayer sp) || !SlayerGear.SLAYER_BELL.equals(Inv.idOf(stack))) {
                return InteractionResultHolder.pass(stack);
            }
            ringBell(sp);
            return InteractionResultHolder.sidedSuccess(stack, false);
        });
    }

    /** Molanisks lured off their wall by a slayer bell (they stay down until they die). */
    private static final Map<LivingEntity, Long> LURED = new WeakHashMap<>();

    /**
     * Rings a slayer bell: molanisks within 3 tiles drop off the walls for good and attack the player who rang it (OSRS
     * Wiki). Returns how many.
     */
    public static int ringBell(ServerPlayer player) {
        if (Players.data(player).skills().level(Skill.SLAYER) < SlayerGear.BELL_SLAYER_LEVEL) {
            Msg.game(player, "You need a Slayer level of " + SlayerGear.BELL_SLAYER_LEVEL + " to use the slayer bell.");
            return 0;
        }
        List<OsrsMonsterEntity> near = player.level().getEntitiesOfClass(OsrsMonsterEntity.class,
                player.getBoundingBox().inflate(SlayerGear.BELL_RADIUS),
                m -> m.isAlive() && SlayerGear.BELL_CATEGORY.equals(category(m)) && !lured(m));
        for (OsrsMonsterEntity m : near) {
            LURED.put(m, Long.MAX_VALUE);
            m.setTarget(player);
        }
        Msg.game(player, near.isEmpty() ? "You ring the bell. Nothing happens." : "You ring the bell. A molanisk drops off the wall!");
        return near.size();
    }

    private static boolean lured(LivingEntity target) {
        Long until = LURED.get(target);
        return until != null && ActionManager.gameTick() <= until;
    }

    private static String category(LivingEntity target) {
        if (!(target instanceof OsrsMonsterEntity)) {
            return null;
        }
        return SlayerService.categoryOf(MonsterStats.of(target));
    }

    private static String creatureId(LivingEntity target) {
        MonsterStats ms = target instanceof OsrsMonsterEntity ? MonsterStats.of(target) : null;
        return ms != null && ms.profile() != null ? ms.profile().id : null;
    }

    // ------------------------------------------------------------------ special weapons & finishers

    /** The damage a player's rolled hit really does: 0 without a leaf-bladed weapon or light, a finisher's rule at low HP. */
    public static int afterRoll(ServerPlayer player, LivingEntity target, AttackType type, int damage) {
        String category = category(target);
        if (category == null) {
            return damage;
        }
        if (SlayerGear.LEAF_BLADED_CATEGORIES.contains(category) && !canHurtLeafBladed(player, type)) {
            Msg.action(player, "You need a leaf-bladed weapon, broad ammunition or Magic Dart to harm this creature.");
            return 0;
        }
        if (SlayerGear.BELL_CATEGORY.equals(category) && !lured(target)) {
            Msg.action(player, "The molanisk clings to the wall out of reach; ring a slayer bell to lure it down.");
            return 0;
        }
        String light = SlayerGear.LIGHT_SOURCE.get(category);
        if (light != null && !Inv.has(player, light, 1)) {
            Msg.action(player, "The swarm is too fast to hit; you need a lit bug lantern to lure it.");
            return 0;
        }
        SlayerGear.Finisher f = SlayerGear.finisher(creatureId(target), category);
        if (f != null && damage > 0) {
            int health = (int) Math.ceil(target.getHealth());
            if (damage >= health) {
                SlayerState.Profile p = SlayerService.profile(player);
                String carried = carriedFinisher(player, f);
                if (p.unlocks.contains(f.unlock()) && carried != null) {
                    spendFinisher(player, f, carried, null);
                    Msg.game(player, f.verb());
                    return damage;
                }
                Msg.game(player, "The " + target.getName().getString().toLowerCase(java.util.Locale.ROOT)
                        + " is weak enough to finish off: use " + Msg.article(ItemName.of(f.item())) + " on it.");
                return Math.max(0, health - 1);
            }
        }
        return damage;
    }

    private static boolean canHurtLeafBladed(ServerPlayer player, AttackType type) {
        String held = Inv.idOf(player.getMainHandItem());
        return switch (type) {
            case MAGIC -> SlayerGear.MAGIC_DART_STAVES.contains(held);
            case RANGED -> SlayerGear.BROAD_AMMO.contains(firstAmmo(player, held));
            default -> SlayerGear.LEAF_BLADED_WEAPONS.contains(held);
        };
    }

    /**
     * The ammunition stack the held bow or crossbow would fire (off hand first, then inventory order, like vanilla),
     * skipping ammunition it can't fire (broad bolts in a crossbow, broad arrows in a bow).
     */
    private static String firstAmmo(ServerPlayer player, String weapon) {
        if (isArrow(player.getOffhandItem()) && AmmoRules.canFire(weapon, Inv.idOf(player.getOffhandItem()))) {
            return Inv.idOf(player.getOffhandItem());
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (isArrow(s) && AmmoRules.canFire(weapon, Inv.idOf(s))) {
                return Inv.idOf(s);
            }
        }
        return null;
    }

    private static boolean isArrow(ItemStack stack) {
        ItemDef def = ModItems.def(stack);
        return def != null && def.kind() == ItemDef.Kind.ARROW;
    }

    /**
     * The finishing item the player carries for {@code f}: its own item (a fungicide spray only with a charge left) or its
     * alternate (a rock thrownhammer for the rock hammer), or null.
     */
    private static String carriedFinisher(ServerPlayer player, SlayerGear.Finisher f) {
        if ("fungicide_spray".equals(f.item())) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack s = player.getInventory().getItem(i);
                if ("fungicide_spray".equals(Inv.idOf(s)) && SlayerItemService.sprayCharges(s) > 0) {
                    return f.item();
                }
            }
            return null;
        }
        if (Inv.has(player, f.item(), 1)) {
            return f.item();
        }
        String alt = SlayerGear.FINISHER_ALTERNATES.get(f.item());
        return alt != null && Inv.has(player, alt, 1) ? alt : null;
    }

    /** Uses up what a finish costs: a consumed item (from {@code held} when given), a spray charge. */
    private static void spendFinisher(ServerPlayer player, SlayerGear.Finisher f, String item, ItemStack held) {
        if ("fungicide_spray".equals(item)) {
            SlayerItemService.useSprayCharge(player);
        } else if (f.consumed() || SlayerGear.CONSUMED_ALTERNATES.contains(item)) {
            if (held != null) {
                held.shrink(1);
            } else {
                Inv.remove(player, item, 1);
            }
        }
    }

    /** Using a rock hammer / thrownhammer / bag of salt / ice cooler / fungicide spray on a weakened creature finishes it. */
    private static boolean useFinisher(ServerPlayer player, LivingEntity target) {
        String category = category(target);
        SlayerGear.Finisher f = category == null ? null : SlayerGear.finisher(creatureId(target), category);
        String held = Inv.idOf(player.getMainHandItem());
        if (f == null || !(f.item().equals(held) || held.equals(SlayerGear.FINISHER_ALTERNATES.get(f.item())))) {
            return false;
        }
        int health = (int) Math.ceil(target.getHealth());
        if (health > f.threshold()) {
            Msg.game(player, "The creature isn't weak enough yet.");
            return true;
        }
        if ("fungicide_spray".equals(held) && SlayerItemService.sprayCharges(player.getMainHandItem()) <= 0) {
            Msg.game(player, "Your fungicide spray is empty. Refill it with fungicide.");
            return true;
        }
        if ("fungicide_spray".equals(held)) {
            SlayerItemService.spendSprayCharge(player.getMainHandItem());
        } else {
            spendFinisher(player, f, held, player.getMainHandItem());
        }
        Msg.game(player, f.verb());
        CombatHandler.applyDamage(player, target, health);
        return true;
    }

    // ------------------------------------------------------------------ protective equipment

    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % 200 == 0) {
            // Keeps the HUD task counter fresh (also covers players who just joined).
            server.getPlayerList().getPlayers().forEach(SlayerNet::sync);
        }
        if (server.getTickCount() % ActionManager.MC_TICKS_PER_GAME_TICK != 0) {
            return;
        }
        long now = ActionManager.gameTick();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
                continue;
            }
            hotFloor(player, now);
            List<OsrsMonsterEntity> near = player.level().getEntitiesOfClass(OsrsMonsterEntity.class,
                    player.getBoundingBox().inflate(6), m -> m.isAlive() && m.getTarget() == player);
            for (OsrsMonsterEntity mob : near) {
                MonsterStats ms = mob.stats();
                SlayerGear.Protection prot = SlayerGear.protection(
                        ms != null && ms.profile() != null ? ms.profile().id : null, category(mob));
                if (prot == null || isProtected(player, prot)) {
                    continue;
                }
                Long next = NEXT_EFFECT.get(mob);
                if (next != null && now < next) {
                    continue;
                }
                MonsterStats stats = mob.stats();
                NEXT_EFFECT.put(mob, now + Math.max(2, stats == null ? 4 : stats.attackSpeed()));
                unprotectedHit(player, mob, prot);
            }
        }
    }

    /** Whether the player wears (or carries) the protective item the creature's special attack needs. */
    public static boolean isProtected(ServerPlayer player, SlayerGear.Protection prot) {
        return switch (prot.where()) {
            case HEAD -> {
                String head = Inv.idOf(player.getItemBySlot(EquipmentSlot.HEAD));
                yield prot.items().contains(head) || SlayerGear.SLAYER_HELMETS.contains(head);
            }
            case SHIELD -> prot.items().contains(Inv.idOf(player.getOffhandItem()));
            case FEET -> prot.items().contains(Inv.idOf(player.getItemBySlot(EquipmentSlot.FEET)));
            case INVENTORY -> prot.items().stream().anyMatch(i -> Inv.has(player, i, 1));
            // The witchwood icon and slayer gloves are worn in the systems module's neck and hands slots.
            case NECK -> prot.items().contains(SysState.of(player).wornId(Jewellery.Slot.NECK));
            case HANDS -> prot.items().contains(SysState.of(player).wornId(Jewellery.Slot.HANDS));
        };
    }

    /** Game tick the player was last warned about the Karuulm floor. */
    private static final Map<ServerPlayer, Long> FLOOR_WARNED = new WeakHashMap<>();

    /** The Karuulm Slayer Dungeon's burning floor: 4 damage every tick without boots of stone or the elite diary. */
    private static void hotFloor(ServerPlayer player, long now) {
        int damage = SlayerService.hotFloorDamage(player);
        if (damage <= 0) {
            return;
        }
        Long warned = FLOOR_WARNED.get(player);
        if (warned == null || now - warned >= 10) {
            FLOOR_WARNED.put(player, now);
            Msg.red(player, SlayerGear.HOT_FLOOR_MESSAGE + " (Wear boots of stone, boots of brimstone or granite boots.)");
        }
        CombatHandler.applyDamage(null, player, damage);
    }

    /** The creature's special attack on an unprotected player: extra damage and drained levels. */
    public static void unprotectedHit(ServerPlayer player, OsrsMonsterEntity mob, SlayerGear.Protection prot) {
        Msg.red(player, prot.message());
        int damage = 1 + player.getRandom().nextInt(Math.max(1, prot.damage()));
        CombatHandler.applyDamage(mob, player, damage);
        if (prot.drain() > 0 && !prot.drains().isEmpty()) {
            SkillSet skills = Players.data(player).skills();
            for (Skill s : prot.drains()) {
                if (s == Skill.PRAYER) {
                    var data = Players.data(player);
                    data.setPrayerPoints(Math.max(0, data.prayerPoints() - prot.drain()));
                    continue;
                }
                int floor = 1 - skills.level(s);
                skills.setBoost(s, Math.max(floor, skills.boost(s) - prot.drain()));
            }
            ServerNetworking.sync(player);
        }
    }

    /** Display names for finishing items without depending on the item registry. */
    private static final class ItemName {
        private ItemName() {
        }

        static String of(String id) {
            com.runecraft.osrs.ItemDef def = com.runecraft.osrs.ItemCatalog.get(id);
            return def == null ? id.replace('_', ' ') : def.name().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
