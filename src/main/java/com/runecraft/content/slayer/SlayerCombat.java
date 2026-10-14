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
import com.runecraft.osrs.content.slayer.SlayerGear;
import com.runecraft.player.Players;
import com.runecraft.registry.ModItems;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Slayer combat mechanics: black mask / slayer helmet bonuses, leaf-bladed weapons, finishing
 * items (rock hammer, bag of salt, ice cooler, fungicide) and protective equipment.
 */
final class SlayerCombat {
    /** Next game tick each creature may land its "unprotected" special effect. */
    private static final Map<LivingEntity, Long> NEXT_EFFECT = new WeakHashMap<>();

    private SlayerCombat() {
    }

    static void register() {
        CombatHooks.addModifier(new CombatHooks.PlayerAttackModifier() {
            // The black mask / slayer helmet bonus (melee 7/6, imbued ranged and magic 23/20 on task, never with a
            // salve amulet) is part of com.runecraft.osrs.PlayerHitCalc, applied through combat.HitCalcBridge.

            @Override
            public int afterRoll(ServerPlayerEntity player, LivingEntity target, AttackType type, int damage) {
                return SlayerCombat.afterRoll(player, target, type, damage);
            }
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient() || hand != Hand.MAIN_HAND || !(player instanceof ServerPlayerEntity sp)
                    || !(entity instanceof LivingEntity living)) {
                return ActionResult.PASS;
            }
            return useFinisher(sp, living) ? ActionResult.SUCCESS : ActionResult.PASS;
        });
        ServerTickEvents.END_SERVER_TICK.register(SlayerCombat::tick);
    }

    private static String category(LivingEntity target) {
        if (!(target instanceof OsrsMonsterEntity)) {
            return null;
        }
        return SlayerService.categoryOf(MonsterStats.of(target));
    }

    // ------------------------------------------------------------------ special weapons & finishers

    static int afterRoll(ServerPlayerEntity player, LivingEntity target, AttackType type, int damage) {
        String category = category(target);
        if (category == null) {
            return damage;
        }
        if (SlayerGear.LEAF_BLADED_CATEGORIES.contains(category) && !canHurtLeafBladed(player, type)) {
            Msg.action(player, "You need a leaf-bladed weapon, broad ammunition or Magic Dart to harm this creature.");
            return 0;
        }
        String light = SlayerGear.LIGHT_SOURCE.get(category);
        if (light != null && !Inv.has(player, light, 1)) {
            Msg.action(player, "The swarm is too fast to hit; you need a lit bug lantern to lure it.");
            return 0;
        }
        SlayerGear.Finisher f = SlayerGear.finisher(category);
        if (f != null && damage > 0) {
            int health = (int) Math.ceil(target.getHealth());
            if (damage >= health) {
                SlayerState.Profile p = SlayerService.profile(player);
                if (p.unlocks.contains(f.unlock()) && Inv.has(player, f.item(), 1)) {
                    if (f.consumed()) {
                        Inv.remove(player, f.item(), 1);
                    }
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

    private static boolean canHurtLeafBladed(ServerPlayerEntity player, AttackType type) {
        String held = Inv.idOf(player.getMainHandStack());
        return switch (type) {
            case MAGIC -> SlayerGear.MAGIC_DART_STAVES.contains(held);
            case RANGED -> SlayerGear.BROAD_AMMO.contains(firstAmmo(player));
            default -> SlayerGear.LEAF_BLADED_WEAPONS.contains(held);
        };
    }

    /** The arrow stack a bow would fire (off hand first, then inventory order), like vanilla. */
    private static String firstAmmo(ServerPlayerEntity player) {
        if (isArrow(player.getOffHandStack())) {
            return Inv.idOf(player.getOffHandStack());
        }
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (isArrow(s)) {
                return Inv.idOf(s);
            }
        }
        return null;
    }

    private static boolean isArrow(ItemStack stack) {
        ItemDef def = ModItems.def(stack);
        return def != null && def.kind() == ItemDef.Kind.ARROW;
    }

    /** Using a rock hammer / bag of salt / ice cooler / fungicide spray on a weakened creature finishes it. */
    private static boolean useFinisher(ServerPlayerEntity player, LivingEntity target) {
        String category = category(target);
        SlayerGear.Finisher f = category == null ? null : SlayerGear.finisher(category);
        if (f == null || !f.item().equals(Inv.idOf(player.getMainHandStack()))) {
            return false;
        }
        int health = (int) Math.ceil(target.getHealth());
        if (health > f.threshold()) {
            Msg.game(player, "The creature isn't weak enough yet.");
            return true;
        }
        if (f.consumed()) {
            player.getMainHandStack().decrement(1);
        }
        Msg.game(player, f.verb());
        CombatHandler.applyDamage(player, target, health);
        return true;
    }

    // ------------------------------------------------------------------ protective equipment

    private static void tick(MinecraftServer server) {
        if (server.getTicks() % 200 == 0) {
            // Keeps the HUD task counter fresh (also covers players who just joined).
            server.getPlayerManager().getPlayerList().forEach(SlayerNet::sync);
        }
        if (server.getTicks() % ActionManager.MC_TICKS_PER_GAME_TICK != 0) {
            return;
        }
        long now = ActionManager.gameTick();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
                continue;
            }
            List<OsrsMonsterEntity> near = player.getWorld().getEntitiesByClass(OsrsMonsterEntity.class,
                    player.getBoundingBox().expand(6), m -> m.isAlive() && m.getTarget() == player);
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

    static boolean isProtected(ServerPlayerEntity player, SlayerGear.Protection prot) {
        return switch (prot.where()) {
            case HEAD -> {
                String head = Inv.idOf(player.getEquippedStack(EquipmentSlot.HEAD));
                yield prot.items().contains(head) || SlayerGear.SLAYER_HELMETS.contains(head);
            }
            case SHIELD -> prot.items().contains(Inv.idOf(player.getOffHandStack()));
            case FEET -> prot.items().contains(Inv.idOf(player.getEquippedStack(EquipmentSlot.FEET)));
            case INVENTORY -> prot.items().stream().anyMatch(i -> Inv.has(player, i, 1));
        };
    }

    private static void unprotectedHit(ServerPlayerEntity player, OsrsMonsterEntity mob, SlayerGear.Protection prot) {
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
