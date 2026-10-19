package com.runecraft.content.zeah;

import com.runecraft.action.Action;
import com.runecraft.action.ActionManager;
import com.runecraft.collection.CollectionLog;
import com.runecraft.content.GameEvents;
import com.runecraft.osrs.Chance;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.zeah.ZeahHunter;
import com.runecraft.osrs.data.ToolTier;
import com.runecraft.player.Players;
import com.runecraft.skill.Tools;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

/** Lovakite mining, the Blast Mine and the Varlamore Hunter spots. */
final class ZeahSkilling {
    /** Lovakite: Mining 65, 10 XP per ore; respawns after ~35 game ticks (approx.). */
    static final int LOVAKITE_LEVEL = 65;
    static final double LOVAKITE_XP = 10.0;
    /** Blast Mine: Mining 43. XP for excavating the rock and for collecting the blasted ore (approx.). */
    static final int BLAST_LEVEL = 43;
    static final double EXCAVATE_XP = 10.0;
    static final double BLAST_XP = 40.0;

    private static final Map<String, Long> DEPLETED = new HashMap<>();
    /** Hard rocks that have been excavated and packed with dynamite. */
    private static final Set<String> CHARGED = new HashSet<>();
    private static final Map<String, LaidTrap> TRAPS = new HashMap<>();

    private record LaidTrap(UUID owner, long laid) {
    }

    private ZeahSkilling() {
    }

    private static String key(ServerLevel world, BlockPos pos) {
        return world.dimension().location() + "|" + pos.asLong();
    }

    private static boolean depleted(String key) {
        Long until = DEPLETED.get(key);
        return until != null && ActionManager.gameTick() < until;
    }

    private static int level(ServerPlayer player, Skill skill) {
        return Players.data(player).skills().currentLevel(skill);
    }

    private static boolean need(ServerPlayer player, Skill skill, int lvl, String what) {
        if (level(player, skill) < lvl) {
            Msg.game(player, "You need " + Msg.article(skill.displayName()) + " level of " + lvl + " to " + what + ".");
            return false;
        }
        return true;
    }

    private static void give(ServerPlayer player, String item, int count) {
        Inv.give(player, item, count);
        CollectionLog.record(player, item);
        GameEvents.fireObtain(player, item, count);
    }

    private static String name(String item) {
        return item.replace('_', ' ');
    }

    // ------------------------------------------------------------------ Lovakite

    static void mineLovakite(ServerPlayer player, BlockPos pos) {
        if (!need(player, Skill.MINING, LOVAKITE_LEVEL, "mine lovakite")) {
            return;
        }
        ServerLevel world = (ServerLevel) player.level();
        String key = key(world, pos);
        if (depleted(key)) {
            Msg.game(player, "There is currently no ore available in this rock.");
            return;
        }
        ToolTier pick = Tools.bestPickaxe(player, Players.data(player).skills());
        if (pick == null) {
            Msg.game(player, "You need a pickaxe to mine this rock. You do not have a pickaxe which you have the Mining level to use.");
            return;
        }
        Msg.action(player, "You swing your pickaxe at the rock.");
        int interval = pick.miningTicks();
        ActionManager.start(player, new Action() {
            private int counter;

            @Override
            public boolean tick(ServerPlayer p, long gameTick) {
                if (p.distanceToSqr(Vec3.atCenterOf(pos)) > 36.0 || depleted(key)) {
                    return false;
                }
                if (++counter < interval) {
                    return true;
                }
                counter = 0;
                p.swing(InteractionHand.MAIN_HAND, true);
                world.playSound(null, pos, SoundEvents.STONE_HIT, SoundSource.PLAYERS, 1.0f, 0.9f);
                if (p.getRandom().nextDouble() >= Chance.lerp(level(p, Skill.MINING), 20, 90)) {
                    return true;
                }
                give(p, "lovakite_ore", 1);
                XpService.add(p, Skill.MINING, LOVAKITE_XP);
                Msg.action(p, "You manage to mine some lovakite.");
                DEPLETED.put(key, ActionManager.gameTick() + 35);
                return false;
            }
        });
    }

    // ------------------------------------------------------------------ Blast Mine

    /** Blasted ore yields: item, Mining level, weight (approx. of the Blast Mine's ore table). */
    private static final Object[][] BLASTED = {{"coal", 30, 50}, {"gold_ore", 40, 20}, {"mithril_ore", 55, 16},
            {"adamantite_ore", 70, 10}, {"runite_ore", 85, 4}};

    static void blastMine(ServerPlayer player, BlockPos pos) {
        if (!need(player, Skill.MINING, BLAST_LEVEL, "work in the Blast Mine")) {
            return;
        }
        ServerLevel world = (ServerLevel) player.level();
        String key = key(world, pos);
        if (depleted(key)) {
            Msg.game(player, "The rubble still needs to settle.");
            return;
        }
        if (!CHARGED.contains(key)) {
            if (!Inv.has(player, "chisel", 1)) {
                Msg.game(player, "You need a chisel to excavate the hard rock.");
                return;
            }
            if (!Inv.has(player, "dynamite", 1)) {
                Msg.game(player, "You need some dynamite to blast this rock. Thirus sells it.");
                return;
            }
            Inv.remove(player, "dynamite", 1);
            player.swing(InteractionHand.MAIN_HAND, true);
            XpService.add(player, Skill.MINING, EXCAVATE_XP);
            CHARGED.add(key);
            Msg.action(player, "You excavate a hole in the rock and pack it with dynamite. Light it with a tinderbox.");
            return;
        }
        if (!Inv.has(player, "tinderbox", 1)) {
            Msg.game(player, "You need a tinderbox to light the dynamite.");
            return;
        }
        CHARGED.remove(key);
        world.playSound(null, pos, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0f, 1.0f);
        world.sendParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 3, 0.4, 0.4, 0.4, 0.0);
        int lvl = level(player, Skill.MINING);
        int total = 0;
        for (Object[] o : BLASTED) {
            if (lvl >= (Integer) o[1]) {
                total += (Integer) o[2];
            }
        }
        int r = player.getRandom().nextInt(Math.max(1, total));
        String ore = "coal";
        for (Object[] o : BLASTED) {
            if (lvl < (Integer) o[1]) {
                continue;
            }
            if (r < (Integer) o[2]) {
                ore = (String) o[0];
                break;
            }
            r -= (Integer) o[2];
        }
        int amount = 1 + player.getRandom().nextInt(2);
        give(player, ore, amount);
        XpService.add(player, Skill.MINING, BLAST_XP);
        Msg.action(player, "The dynamite explodes! You collect " + amount + " " + name(ore) + ".");
        DEPLETED.put(key, ActionManager.gameTick() + 20);
    }

    // ------------------------------------------------------------------ Hunter

    static void hunt(ServerPlayer player, BlockPos pos, ZeahHunter.Spot spot) {
        ServerLevel world = (ServerLevel) player.level();
        String key = key(world, pos);
        long now = ActionManager.gameTick();
        String creature = spot.creature().toLowerCase();
        if (!spot.laid()) {
            if (!need(player, Skill.HUNTER, spot.level(), "catch " + creature + "s")) {
                return;
            }
            String net = Inv.idOf(player.getMainHandItem());
            if (!net.equals("butterfly_net") && !net.equals("magic_butterfly_net")) {
                Msg.game(player, "You need to wield a butterfly net to catch moths.");
                return;
            }
            if (spot.consumed() != null && !Inv.has(player, spot.consumed(), 1)) {
                Msg.game(player, "You need an empty " + name(spot.consumed()) + " to keep the moth in.");
                return;
            }
            player.swing(InteractionHand.MAIN_HAND, true);
            if (player.getRandom().nextDouble() >= ZeahHunter.catchChance(level(player, Skill.HUNTER), spot)) {
                Msg.game(player, "The " + creature + " flutters out of reach.");
                return;
            }
            if (spot.consumed() != null) {
                Inv.remove(player, spot.consumed(), 1);
            }
            for (String item : spot.loot()) {
                give(player, item, 1);
            }
            XpService.add(player, Skill.HUNTER, spot.xp());
            Msg.action(player, "You catch " + Msg.article(creature) + ".");
            return;
        }
        LaidTrap laid = TRAPS.get(key);
        if (laid != null) {
            if (!laid.owner().equals(player.getUUID())) {
                Msg.game(player, "This isn't your trap.");
                return;
            }
            if (now - laid.laid() < spot.catchTicks()) {
                Msg.game(player, "Nothing has been caught yet. Wait a little longer.");
                return;
            }
            TRAPS.remove(key);
            com.runecraft.skill.HunterService.release(world, pos);
            if (player.getRandom().nextDouble() < ZeahHunter.catchChance(level(player, Skill.HUNTER), spot)) {
                for (String item : spot.loot()) {
                    give(player, item, 1);
                }
                XpService.add(player, Skill.HUNTER, spot.xp());
                Msg.action(player, "You've caught " + Msg.article(creature) + "!");
                world.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1f, 1f);
            } else {
                Msg.game(player, "The " + creature + " escaped from your trap.");
            }
            return;
        }
        if (!need(player, Skill.HUNTER, spot.level(), "hunt " + creature + "s")) {
            return;
        }
        for (String tool : spot.tools()) {
            if (!Inv.has(player, tool, 1)) {
                Msg.game(player, "You need " + Msg.article(name(tool)) + " to set this trap.");
                return;
            }
        }
        // Varlamore traps share the Hunter trap limit with every other laid trap (OSRS).
        if (com.runecraft.skill.HunterService.atTrapLimit(player)) {
            return;
        }
        if (spot.consumed() != null) {
            if (!Inv.has(player, spot.consumed(), 1)) {
                Msg.game(player, "You need some " + name(spot.consumed()) + " to set this trap.");
                return;
            }
            Inv.remove(player, spot.consumed(), 1);
        }
        player.swing(InteractionHand.MAIN_HAND, true);
        TRAPS.put(key, new LaidTrap(player.getUUID(), now));
        com.runecraft.skill.HunterService.claim(player, world, pos, false);
        Msg.action(player, spot.method() == ZeahHunter.Method.PITFALL ? "You dig a pit and cover it with logs."
                : "You set up the box trap by the burrow.");
    }
}
