package com.runecraft.content.godwars;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.combat.PoisonService;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.godwars.WildernessBosses;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Msg;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.List;

/** Scripts for the Wilderness bosses that need more than their attack data. */
final class WildernessScripts {
    private WildernessScripts() {
    }

    /** Bosses whose attack data covers everything; the script only tops up the pet chance. */
    static final class PetOnly extends GwdScript {
    }

    /**
     * Moves an equipped item into the inventory (Chaos Elemental's madness, Chaos Fanatic's disarm).
     * Returns false if nothing could be unequipped.
     */
    static boolean unequip(ServerPlayerEntity player, boolean weaponOnly) {
        List<EquipmentSlot> slots = new ArrayList<>();
        if (!player.getMainHandStack().isEmpty()) {
            slots.add(EquipmentSlot.MAINHAND);
        }
        if (!weaponOnly) {
            for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
                    EquipmentSlot.OFFHAND}) {
                if (!player.getEquippedStack(s).isEmpty()) {
                    slots.add(s);
                }
            }
        }
        if (slots.isEmpty()) {
            return false;
        }
        EquipmentSlot slot = slots.get(player.getRandom().nextInt(slots.size()));
        var inv = player.getInventory();
        if (slot == EquipmentSlot.MAINHAND) {
            ItemStack held = player.getMainHandStack().copy();
            for (int i = 0; i < inv.main.size(); i++) {
                if (i != inv.selectedSlot && inv.main.get(i).isEmpty()) {
                    inv.main.set(i, held);
                    player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                    return true;
                }
            }
            return false;
        }
        ItemStack worn = player.getEquippedStack(slot).copy();
        player.equipStack(slot, ItemStack.EMPTY);
        if (!inv.insertStack(worn)) {
            player.dropItem(worn, false);
        }
        return true;
    }

    /**
     * Chaos Elemental: its "confusion" attack comes in all three styles; "discord" teleports the
     * target a few tiles away and "madness" unequips one of their items.
     */
    static final class ChaosElemental extends GwdScript {
        @Override
        protected AttackDef pick(DataBossEntity boss, ServerPlayerEntity target, double distance) {
            int r = boss.getRandom().nextInt(20);
            if (r < 2) {
                return WildernessBosses.CHAOS_DISCORD;
            }
            if (r < 4) {
                return WildernessBosses.CHAOS_MADNESS;
            }
            if (r >= 15 && distance <= meleeReach(boss)) {
                return WildernessBosses.CHAOS_PRIMARY_MELEE;
            }
            return r < 10 ? WildernessBosses.CHAOS_PRIMARY : WildernessBosses.CHAOS_PRIMARY_RANGED;
        }

        @Override
        protected int landed(DataBossEntity boss, ServerPlayerEntity player, int damage, AttackDef attack) {
            if (attack == WildernessBosses.CHAOS_DISCORD) {
                ServerWorld world = (ServerWorld) boss.getWorld();
                double angle = boss.getRandom().nextDouble() * Math.PI * 2;
                double x = player.getX() + Math.cos(angle) * 6;
                double z = player.getZ() + Math.sin(angle) * 6;
                int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
                world.spawnParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1, player.getZ(), 30, 0.4, 0.8, 0.4, 0.3);
                player.requestTeleport(x, y, z);
            } else if (attack == WildernessBosses.CHAOS_MADNESS) {
                if (unequip(player, false)) {
                    com.runecraft.content.systems.CombatAchievementService.flag(player,
                            com.runecraft.osrs.content.systems.CombatAchievements.FLAG_CHAOS_UNEQUIPPED);
                    Msg.red(player, "The Chaos Elemental's madness makes you remove an item!");
                }
            }
            return damage;
        }
    }

    /** Chaos Fanatic: shouts, throws three green explosions around his target and disarms players. */
    static final class ChaosFanatic extends GwdScript {
        private static final String[] SHOUTS = {"Burn!", "WEUGH!", "Develish Oxen Roll!", "All your wilderness are belong to them!",
                "AhehHeheuhHhahueHuUEehEahAH", "I shall call him squidgy and he shall be my squidgy!", "Kill! Kill!"};

        @Override
        public void gameTick(DataBossEntity boss, long gameTick) {
            super.gameTick(boss, gameTick);
            if (!(boss.getTarget() instanceof ServerPlayerEntity target) || target.squaredDistanceTo(boss) > 20 * 20) {
                return;
            }
            if (gameTick % 8 == 0 && boss.getRandom().nextInt(2) == 0) {
                shout(boss, SHOUTS[boss.getRandom().nextInt(SHOUTS.length)]);
            }
            if (gameTick % 14 == 0) {
                for (int i = 0; i < 3; i++) {
                    Vec3d pos = target.getPos().add(boss.getRandom().nextInt(5) - 2, 0, boss.getRandom().nextInt(5) - 2);
                    strike(boss, pos, 3, 15, 1.3, ParticleTypes.HAPPY_VILLAGER, ParticleTypes.EXPLOSION, null);
                }
            }
            if (gameTick % 25 == 0 && boss.getRandom().nextInt(3) == 0 && unequip(target, true)) {
                Msg.red(target, "The Chaos Fanatic has disarmed you!");
            }
        }
    }

    /** Crazy archaeologist: rants, and rains three explosive books around his target. */
    static final class CrazyArchaeologist extends GwdScript {
        private static final String[] SHOUTS = {"I'm Bellock - respect me!", "Get off my site!", "No-one messes with Bellock's dig!",
                "These ruins are mine!", "Taste my knowledge!", "You belong in a museum!"};

        @Override
        public void gameTick(DataBossEntity boss, long gameTick) {
            super.gameTick(boss, gameTick);
            if (!(boss.getTarget() instanceof ServerPlayerEntity target) || target.squaredDistanceTo(boss) > 20 * 20) {
                return;
            }
            if (gameTick % 9 == 0 && boss.getRandom().nextInt(2) == 0) {
                shout(boss, SHOUTS[boss.getRandom().nextInt(SHOUTS.length)]);
            }
            if (gameTick % 16 == 0) {
                shout(boss, "Rain of knowledge!");
                for (int i = 0; i < 3; i++) {
                    Vec3d pos = target.getPos().add(boss.getRandom().nextInt(5) - 2, 0, boss.getRandom().nextInt(5) - 2);
                    strike(boss, pos, 4, 23, 1.3, ParticleTypes.ENCHANT, ParticleTypes.EXPLOSION, null);
                }
            }
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayerEntity killer) {
            shout(boss, "Ow!");
            super.onDeath(boss, killer);
        }
    }

    /**
     * Scorpia: her offspring scuttle around her; below half health two guardians appear and heal
     * her while they live.
     */
    static final class Scorpia extends GwdScript {
        private final List<MobEntity> guardians = new ArrayList<>();
        private boolean guardiansSummoned;

        @Override
        public void onSpawn(DataBossEntity boss) {
            for (int i = 0; i < 3; i++) {
                spawnMinion(boss, "scorpias_offspring", boss.getRandom().nextInt(7) - 3, boss.getRandom().nextInt(7) - 3);
            }
        }

        @Override
        public void gameTick(DataBossEntity boss, long gameTick) {
            super.gameTick(boss, gameTick);
            if (!guardiansSummoned && boss.getHealth() < boss.getMaxHealth() / 2) {
                guardiansSummoned = true;
                guardians.add(spawnMinion(boss, "scorpias_guardian", 2, 2));
                guardians.add(spawnMinion(boss, "scorpias_guardian", -2, -2));
                for (ServerPlayerEntity p : players(boss, 24)) {
                    Msg.red(p, "Scorpia's guardians arrive to heal her!");
                }
            }
            if (gameTick % 2 == 0) {
                ServerWorld world = (ServerWorld) boss.getWorld();
                for (MobEntity g : guardians) {
                    if (g != null && g.isAlive() && g.squaredDistanceTo(boss) < 10 * 10 && boss.getHealth() < boss.getMaxHealth()) {
                        boss.heal(1);
                        world.spawnParticles(ParticleTypes.HEART, g.getX(), g.getY() + 0.8, g.getZ(), 1, 0.2, 0.2, 0.2, 0);
                    }
                }
            }
        }
    }

    /**
     * King Black Dragon: three heads, four breaths. Regular dragonfire; toxic breath that poisons;
     * icy breath that freezes; shocking breath that lowers combat stats. An anti-dragon shield and
     * antifire protect as usual (handled by the DRAGONFIRE special).
     */
    static final class KingBlackDragon extends GwdScript {
        @Override
        protected AttackDef pick(DataBossEntity boss, ServerPlayerEntity target, double distance) {
            if (distance <= meleeReach(boss) && boss.getRandom().nextInt(2) == 0) {
                return WildernessBosses.KBD_MELEE;
            }
            return switch (boss.getRandom().nextInt(6)) {
                case 0 -> WildernessBosses.KBD_POISON;
                case 1 -> WildernessBosses.KBD_ICE;
                case 2 -> WildernessBosses.KBD_SHOCK;
                default -> WildernessBosses.KBD_FIRE;
            };
        }

        @Override
        protected int landed(DataBossEntity boss, ServerPlayerEntity player, int damage, AttackDef attack) {
            long tick = com.runecraft.action.ActionManager.gameTick();
            if (attack == WildernessBosses.KBD_POISON) {
                PoisonService.poison(player, 8, tick);
                Msg.action(player, "You are poisoned by the toxic breath!");
            } else if (attack == WildernessBosses.KBD_ICE) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 5 * 12, 10, false, true));
                Msg.action(player, "The icy breath freezes you in place!");
            } else if (attack == WildernessBosses.KBD_SHOCK) {
                PlayerData data = Players.data(player);
                for (Skill s : WildernessBosses.SHOCK_DRAINS) {
                    data.skills().setBoost(s, data.skills().boost(s) - 2);
                }
                ServerNetworking.sync(player);
                Msg.action(player, "You're shocked and weakened!");
            }
            return damage;
        }
    }
}
