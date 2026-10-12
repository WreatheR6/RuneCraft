package com.runecraft.content.skilling;

import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.skilling.AgilityData;
import com.runecraft.osrs.content.skilling.AgilityShortcuts;
import com.runecraft.quest.QuestScripts;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Grace's Graceful clothing shop in the Rogues' Den: talk to buy the selected piece for Marks of
 * grace; talk while sneaking to choose another piece.
 */
final class AgilityHooks {
    /** Piece each player has selected at Grace's shop (not persisted). */
    private static final Map<UUID, Integer> SELECTED = new HashMap<>();

    private AgilityHooks() {
    }

    static void init() {
        QuestScripts.onTalk(AgilityData.GRACE, AgilityHooks::grace);
        com.runecraft.content.BlockHooks.register(AgilityShortcuts.BLOCK, (player, pos, state) -> shortcut(player, pos));
        AgilityPlacer.init();
    }

    /** Uses the agility shortcut whose end is at this block: level checks, then off to the other end. */
    private static void shortcut(ServerPlayerEntity player, net.minecraft.util.math.BlockPos pos) {
        int x = com.runecraft.osrs.GielinorMap.toOsrsX(pos.getX());
        int y = com.runecraft.osrs.GielinorMap.toOsrsY(pos.getZ());
        AgilityShortcuts.Shortcut s = AgilityShortcuts.at(x, y);
        if (s == null) {
            Msg.game(player, "Nothing interesting happens.");
            return;
        }
        if (s.level() > 0 && !SkillingUtil.need(player, Skill.AGILITY, s.level(), s.verb())) {
            return;
        }
        for (Map.Entry<Skill, Integer> req : s.other().entrySet()) {
            if (!SkillingUtil.need(player, req.getKey(), req.getValue(), s.verb())) {
                return;
            }
        }
        boolean fromA = s.endAt(x, y) == 0;
        int tx = fromA ? s.bx() : s.ax();
        int ty = fromA ? s.by() : s.ay();
        net.minecraft.server.world.ServerWorld world = (net.minecraft.server.world.ServerWorld) player.getWorld();
        net.minecraft.util.math.BlockPos land = com.runecraft.world.Gielinor.safeSpot(world,
                com.runecraft.osrs.GielinorMap.toBlockX(tx) + 1, com.runecraft.osrs.GielinorMap.toBlockZ(ty) + 1);
        player.teleport(world, land.getX() + 0.5, land.getY(), land.getZ() + 0.5, player.getYaw(), player.getPitch());
        if (s.xp() > 0) {
            com.runecraft.skill.XpService.add(player, Skill.AGILITY, s.xp());
        }
        Msg.action(player, "You " + s.verb() + ".");
    }

    private static void grace(ServerPlayerEntity player, QuestNpcEntity npc) {
        int index = SELECTED.getOrDefault(player.getUuid(), 0) % AgilityData.GRACEFUL.size();
        if (player.isSneaking() || !SELECTED.containsKey(player.getUuid())) {
            if (SELECTED.containsKey(player.getUuid())) {
                index = (index + 1) % AgilityData.GRACEFUL.size();
            }
            SELECTED.put(player.getUuid(), index);
            AgilityData.Piece p = AgilityData.GRACEFUL.get(index);
            Msg.game(player, "Grace: The " + SkillingUtil.lower(p.name()) + " costs " + p.marks()
                    + " Marks of grace. Talk to me again to buy it, or sneak and talk to see something else.");
            return;
        }
        AgilityData.Piece p = AgilityData.GRACEFUL.get(index);
        if (!Inv.has(player, "mark_of_grace", p.marks())) {
            Msg.game(player, "Grace: You'll need " + p.marks() + " Marks of grace for the " + SkillingUtil.lower(p.name())
                    + ". You have " + Inv.count(player, "mark_of_grace") + ".");
            return;
        }
        Inv.remove(player, "mark_of_grace", p.marks());
        SkillingUtil.give(player, p.id(), 1);
        Msg.game(player, "Grace: Here you go. Light as a feather, isn't it?");
    }
}
