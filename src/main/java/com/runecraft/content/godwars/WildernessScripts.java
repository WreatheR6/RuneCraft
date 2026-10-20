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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Scripts for the Wilderness bosses that need more than their attack data. */
final class WildernessScripts {
    /** The Chaos Fanatic's green explosions and the crazy archaeologist's books (OSRS Wiki special max hits). */
    static final int FANATIC_EXPLOSION_MAX = 31;
    static final int ARCHAEOLOGIST_BOOK_MAX = 24;

    private WildernessScripts() {
    }

    /** Players fighting a boss: those in the world near it plus its target (which may be a test player). */
    static List<ServerPlayer> fighters(DataBossEntity boss, double radius) {
        List<ServerPlayer> list = new ArrayList<>(players(boss, radius));
        if (boss.getTarget() instanceof ServerPlayer t && t.isAlive() && !list.contains(t)) {
            list.add(t);
        }
        return list;
    }

    private static List<ServerPlayer> players(DataBossEntity boss, double radius) {
        return ((ServerLevel) boss.level()).getPlayers(p -> p.isAlive() && !p.isSpectator() && !p.isCreative()
                && p.distanceToSqr(boss) <= radius * radius);
    }

    private static boolean prays(ServerPlayer p, com.runecraft.osrs.Prayer prayer) {
        return Players.data(p).isActive(prayer);
    }

    /** Binds a player in place for a few game ticks (bear traps, the knockback orb's stun). */
    static void bind(ServerPlayer p, int ticks) {
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks * 12, 10, false, true));
    }

    /**
     * Throws a player up to {@code tiles} tiles straight away from {@code from}, stopping at a wall (or anything solid);
     * returns the whole tiles moved.
     */
    static int knockBack(DataBossEntity from, ServerPlayer target, int tiles) {
        Vec3 dir = new Vec3(target.getX() - from.getX(), 0, target.getZ() - from.getZ());
        dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : dir.normalize();
        ServerLevel w = (ServerLevel) from.level();
        Vec3 start = target.position();
        Vec3 at = start;
        int moved = 0;
        for (int step = 1; step <= tiles * 2; step++) { // one OSRS tile is two blocks
            Vec3 next = start.add(dir.scale(step));
            net.minecraft.core.BlockPos feet = net.minecraft.core.BlockPos.containing(next);
            if (!w.getBlockState(feet).getCollisionShape(w, feet).isEmpty()
                    || !w.getBlockState(feet.above()).getCollisionShape(w, feet.above()).isEmpty()) {
                break;
            }
            at = next;
            moved = step;
        }
        if (moved > 0) {
            target.teleportTo(at.x, at.y, at.z);
        }
        return moved / 2;
    }

    /**
     * Callisto and Artio (OSRS Wiki, post-2023 rework): a claw swipe in reach (Protect from Melee only halves it; it never
     * hits below 35 / 15 when it lands), crescents at everyone in the den when he can't reach his target, and now and then a
     * slow white orb (blocked by Protect from Magic; off prayer it throws the target 3 tiles back and hits 5, +15 per tile a
     * wall cuts short, then briefly stuns). At 66% and 33% he roars (breaking any freeze) and scatters bear traps (up to 15
     * and a bind when stepped on); from 33% a fresh set comes with every attack.
     */
    static final class Bear extends GwdScript {
        private record Trap(Vec3 pos, long until) {
        }

        private final AttackDef swipe;
        private final AttackDef crescent;
        private final AttackDef orb;
        private final int swipeMin;
        private final List<Trap> traps = new ArrayList<>();
        private int sinceOrb;
        private boolean roared66;
        private boolean roared33;

        Bear(boolean artio) {
            this.swipe = artio ? WildernessBosses.ARTIO_SWIPE : WildernessBosses.CALLISTO_SWIPE;
            this.crescent = artio ? WildernessBosses.ARTIO_CRESCENT : WildernessBosses.CALLISTO_CRESCENT;
            this.orb = artio ? WildernessBosses.ARTIO_ORB : WildernessBosses.CALLISTO_ORB;
            this.swipeMin = artio ? WildernessBosses.ARTIO_SWIPE_MIN : WildernessBosses.CALLISTO_SWIPE_MIN;
        }

        @Override
        protected AttackDef pick(DataBossEntity boss, ServerPlayer target, double distance) {
            if (roared33) {
                layTraps(boss, target);
            }
            if (++sinceOrb >= 5 && boss.getRandom().nextInt(3) == 0) { // approx: "occasionally"
                sinceOrb = 0;
                return orb;
            }
            return distance <= meleeReach(boss) ? swipe : crescent;
        }

        @Override
        protected boolean isMultiTarget(AttackDef attack) {
            return attack == crescent;
        }

        @Override
        protected int landed(DataBossEntity boss, ServerPlayer player, int damage, AttackDef attack) {
            if (attack == swipe) {
                int d = damage > 0 ? Math.max(damage, swipeMin) : 0;
                return prays(player, com.runecraft.osrs.Prayer.PROTECT_FROM_MELEE) ? d / 2 : d;
            }
            if (attack == orb) {
                if (prays(player, com.runecraft.osrs.Prayer.PROTECT_FROM_MAGIC)) {
                    return 0;
                }
                int moved = knockBack(boss, player, WildernessBosses.ORB_KNOCKBACK_TILES);
                bind(player, 2);
                Msg.red(player, "The orb throws you backwards!");
                return WildernessBosses.orbDamage(moved);
            }
            return damage;
        }

        private void layTraps(DataBossEntity boss, ServerPlayer target) {
            traps.clear();
            long until = com.runecraft.action.ActionManager.gameTick() + 12; // approx: "linger for a short duration"
            Vec3 c = boss.position();
            for (int i = 0; i < 10; i++) {
                double a = boss.getRandom().nextDouble() * Math.PI * 2;
                double r = 2 + boss.getRandom().nextDouble() * 9;
                double x = c.x + Math.cos(a) * r;
                double z = c.z + Math.sin(a) * r;
                int y = ((ServerLevel) boss.level()).getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
                traps.add(new Trap(new Vec3(x, Math.abs(y - c.y) > 4 ? c.y : y, z), until));
            }
        }

        private void roar(DataBossEntity boss) {
            boss.removeEffect(MobEffects.MOVEMENT_SLOWDOWN); // he glows red and breaks free of binds
            shout(boss, "Raaarrrgh!");
            layTraps(boss, boss.getTarget() instanceof ServerPlayer t ? t : null);
            for (ServerPlayer p : fighters(boss, 24)) {
                Msg.red(p, boss.getName().getString() + " roars and scatters bear traps across the den!");
            }
        }

        @Override
        public void gameTick(DataBossEntity boss, long gameTick) {
            super.gameTick(boss, gameTick);
            float pct = boss.getHealth() / boss.getMaxHealth();
            if (!roared66 && pct <= 0.66f) {
                roared66 = true;
                roar(boss);
            }
            if (!roared33 && pct <= 0.33f) {
                roared33 = true;
                roar(boss);
            }
            if (traps.isEmpty()) {
                return;
            }
            ServerLevel w = (ServerLevel) boss.level();
            List<ServerPlayer> near = fighters(boss, 32);
            traps.removeIf(t -> {
                if (gameTick > t.until()) {
                    return true;
                }
                w.sendParticles(ParticleTypes.CRIMSON_SPORE, t.pos().x, t.pos().y + 0.1, t.pos().z, 3, 0.25, 0.02, 0.25, 0);
                for (ServerPlayer p : near) {
                    if (p.position().distanceToSqr(t.pos()) < 0.8 * 0.8) {
                        com.runecraft.combat.CombatHooks.nameAttack("bear_trap", null);
                        com.runecraft.combat.CombatHandler.applyDamage(boss, p, boss.getRandom().nextInt(WildernessBosses.BEAR_TRAP_MAX + 1));
                        bind(p, 3);
                        Msg.red(p, "You step on a bear trap!");
                        return true;
                    }
                }
                return false;
            });
        }
    }

    /**
     * Venenatis and Spindel (OSRS Wiki, post-2023 rework): a lunge in reach, otherwise 8 ranged then 8 magic attacks,
     * shuffling to another spot every 4. Spiderlings (5 hitpoints, each hit drains a Prayer point) come at the start of each
     * ranged run, and the sticky web (7x5 tiles: 3 damage, 3 Prayer and 10% run energy every other tick) on the third magic
     * attack. Every living spiderling makes her hits stronger and lets part of a protected hit through.
     */
    static final class Spider extends GwdScript {
        private final AttackDef bite;
        private final AttackDef spit;
        private final AttackDef bolt;
        private final List<Mob> spiderlings = new ArrayList<>();
        private int cycle;
        private Vec3 web;
        private long webUntil = -1;

        Spider(boolean spindel) {
            this.bite = spindel ? WildernessBosses.SPINDEL_BITE : WildernessBosses.VENENATIS_BITE;
            this.spit = spindel ? WildernessBosses.SPINDEL_SPIT : WildernessBosses.VENENATIS_SPIT;
            this.bolt = spindel ? WildernessBosses.SPINDEL_WEB_BOLT : WildernessBosses.VENENATIS_WEB_BOLT;
        }

        private int alive() {
            spiderlings.removeIf(m -> m == null || !m.isAlive());
            return spiderlings.size();
        }

        @Override
        protected AttackDef pick(DataBossEntity boss, ServerPlayer target, double distance) {
            int i = cycle++ % 16;
            if (i == 0) {
                for (int k = 0; k < 2; k++) {
                    spiderlings.add(spawnMinion(boss, "venenatis_spiderling", boss.getRandom().nextInt(7) - 3, boss.getRandom().nextInt(7) - 3));
                }
                shout(boss, "*hiss*");
            } else if (i == 10) {
                web = target.position();
                webUntil = com.runecraft.action.ActionManager.gameTick() + 16; // approx: how long the web stays
                for (ServerPlayer p : fighters(boss, 24)) {
                    Msg.red(p, boss.getName().getString() + " launches a sticky web!");
                }
            }
            if (i % 4 == 3) {
                shuffle(boss);
            }
            if (distance <= meleeReach(boss)) {
                return bite;
            }
            return i < 8 ? spit : bolt;
        }

        /** Moves to another spot of her lair (every 4 attacks). */
        private void shuffle(DataBossEntity boss) {
            double a = boss.getRandom().nextDouble() * Math.PI * 2;
            double r = 3 + boss.getRandom().nextDouble() * 4;
            boss.getNavigation().moveTo(boss.getX() + Math.cos(a) * r, boss.getY(), boss.getZ() + Math.sin(a) * r, 1.2);
        }

        @Override
        protected int landed(DataBossEntity boss, ServerPlayer player, int damage, AttackDef attack) {
            int n = alive();
            double d = damage * (1 + WildernessBosses.SPIDERLING_DAMAGE_BONUS * n);
            com.runecraft.osrs.Prayer prayer = attack == bite ? com.runecraft.osrs.Prayer.PROTECT_FROM_MELEE
                    : attack == spit ? com.runecraft.osrs.Prayer.PROTECT_FROM_MISSILES : com.runecraft.osrs.Prayer.PROTECT_FROM_MAGIC;
            if (prays(player, prayer)) {
                d *= Math.min(1.0, WildernessBosses.SPIDERLING_PRAYER_LEAK * n);
            }
            return (int) Math.round(d);
        }

        @Override
        public void gameTick(DataBossEntity boss, long gameTick) {
            super.gameTick(boss, gameTick);
            if (web == null) {
                return;
            }
            if (gameTick > webUntil) {
                web = null;
                return;
            }
            ServerLevel w = (ServerLevel) boss.level();
            w.sendParticles(ParticleTypes.WHITE_ASH, web.x, web.y + 0.1, web.z, 20, 3.5, 0.05, 2.5, 0);
            if (gameTick % 2 != 0) {
                return;
            }
            for (ServerPlayer p : fighters(boss, 32)) {
                if (Math.abs(p.getX() - web.x) <= 7 && Math.abs(p.getZ() - web.z) <= 5 && Math.abs(p.getY() - web.y) < 2) {
                    com.runecraft.combat.CombatHooks.nameAttack("sticky_web", null);
                    com.runecraft.combat.CombatHandler.applyDamage(boss, p, WildernessBosses.WEB_DAMAGE);
                    PlayerData data = Players.data(p);
                    data.setPrayerPoints(Math.max(0, data.prayerPoints() - WildernessBosses.WEB_PRAYER));
                    com.runecraft.content.systems.SysState.SysPlayer sp = com.runecraft.content.systems.SysState.of(p);
                    sp.runEnergy = Math.max(0, sp.runEnergy - 1000);
                    ServerNetworking.sync(p);
                }
            }
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayer killer) {
            spiderlings.forEach(m -> {
                if (m != null) {
                    m.discard();
                }
            });
            super.onDeath(boss, killer);
        }
    }

    /**
     * Moves an equipped item into the inventory (Chaos Elemental's madness, Chaos Fanatic's disarm).
     * Returns false if nothing could be unequipped.
     */
    static boolean unequip(ServerPlayer player, boolean weaponOnly) {
        List<EquipmentSlot> slots = new ArrayList<>();
        if (!player.getMainHandItem().isEmpty()) {
            slots.add(EquipmentSlot.MAINHAND);
        }
        if (!weaponOnly) {
            for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
                    EquipmentSlot.OFFHAND}) {
                if (!player.getItemBySlot(s).isEmpty()) {
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
            ItemStack held = player.getMainHandItem().copy();
            for (int i = 0; i < inv.items.size(); i++) {
                if (i != inv.selected && inv.items.get(i).isEmpty()) {
                    inv.items.set(i, held);
                    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    return true;
                }
            }
            return false;
        }
        ItemStack worn = player.getItemBySlot(slot).copy();
        player.setItemSlot(slot, ItemStack.EMPTY);
        if (!inv.add(worn)) {
            player.drop(worn, false);
        }
        return true;
    }

    /**
     * Chaos Elemental: its "confusion" attack comes in all three styles; "discord" teleports the
     * target a few tiles away and "madness" unequips one of their items.
     */
    static final class ChaosElemental extends GwdScript {
        @Override
        protected AttackDef pick(DataBossEntity boss, ServerPlayer target, double distance) {
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
        protected int landed(DataBossEntity boss, ServerPlayer player, int damage, AttackDef attack) {
            if (attack == WildernessBosses.CHAOS_DISCORD) {
                ServerLevel world = (ServerLevel) boss.level();
                double angle = boss.getRandom().nextDouble() * Math.PI * 2;
                double x = player.getX() + Math.cos(angle) * 6;
                double z = player.getZ() + Math.sin(angle) * 6;
                int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
                world.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1, player.getZ(), 30, 0.4, 0.8, 0.4, 0.3);
                player.teleportTo(x, y, z);
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
            if (!(boss.getTarget() instanceof ServerPlayer target) || target.distanceToSqr(boss) > 20 * 20) {
                return;
            }
            if (gameTick % 8 == 0 && boss.getRandom().nextInt(2) == 0) {
                shout(boss, SHOUTS[boss.getRandom().nextInt(SHOUTS.length)]);
            }
            if (gameTick % 14 == 0) {
                // OSRS Wiki: six slow green projectiles in an arc towards the player, up to 31 each where they land (step away).
                for (int i = 0; i < 6; i++) {
                    Vec3 pos = target.position().add(boss.getRandom().nextInt(5) - 2, 0, boss.getRandom().nextInt(5) - 2);
                    strike("green explosion", boss, pos, 4, FANATIC_EXPLOSION_MAX, 1.3, ParticleTypes.HAPPY_VILLAGER, ParticleTypes.EXPLOSION, null);
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
            if (!(boss.getTarget() instanceof ServerPlayer target) || target.distanceToSqr(boss) > 20 * 20) {
                return;
            }
            if (gameTick % 9 == 0 && boss.getRandom().nextInt(2) == 0) {
                shout(boss, SHOUTS[boss.getRandom().nextInt(SHOUTS.length)]);
            }
            if (gameTick % 16 == 0) {
                // OSRS Wiki: three books in an arc, exploding in a 3x3 for up to 24; one splits into two more that land later.
                shout(boss, "Rain of knowledge!");
                Vec3 split = null;
                for (int i = 0; i < 3; i++) {
                    Vec3 pos = target.position().add(boss.getRandom().nextInt(5) - 2, 0, boss.getRandom().nextInt(5) - 2);
                    strike("explosive book", boss, pos, 4, ARCHAEOLOGIST_BOOK_MAX, 1.5, ParticleTypes.ENCHANT, ParticleTypes.EXPLOSION, null);
                    if (i == 0) {
                        split = pos;
                    }
                }
                for (int i = 0; i < 2; i++) {
                    Vec3 pos = split.add(boss.getRandom().nextInt(5) - 2, 0, boss.getRandom().nextInt(5) - 2);
                    strike("explosive book", boss, pos, 7, ARCHAEOLOGIST_BOOK_MAX, 1.5, ParticleTypes.ENCHANT, ParticleTypes.EXPLOSION, null);
                }
            }
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayer killer) {
            shout(boss, "Ow!");
            super.onDeath(boss, killer);
        }
    }

    /**
     * Scorpia: her offspring scuttle around her; below half health two guardians appear and heal
     * her while they live.
     */
    static final class Scorpia extends GwdScript {
        private final List<Mob> guardians = new ArrayList<>();
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
                for (ServerPlayer p : players(boss, 24)) {
                    Msg.red(p, "Scorpia's guardians arrive to heal her!");
                }
            }
            if (gameTick % 2 == 0) {
                ServerLevel world = (ServerLevel) boss.level();
                for (Mob g : guardians) {
                    if (g != null && g.isAlive() && g.distanceToSqr(boss) < 10 * 10 && boss.getHealth() < boss.getMaxHealth()) {
                        boss.heal(1);
                        world.sendParticles(ParticleTypes.HEART, g.getX(), g.getY() + 0.8, g.getZ(), 1, 0.2, 0.2, 0.2, 0);
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
        protected AttackDef pick(DataBossEntity boss, ServerPlayer target, double distance) {
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
        protected int landed(DataBossEntity boss, ServerPlayer player, int damage, AttackDef attack) {
            long tick = com.runecraft.action.ActionManager.gameTick();
            if (attack == WildernessBosses.KBD_POISON) {
                PoisonService.poison(player, 8, tick);
                Msg.action(player, "You are poisoned by the toxic breath!");
            } else if (attack == WildernessBosses.KBD_ICE) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5 * 12, 10, false, true));
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
