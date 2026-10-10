package com.runecraft.content.islands;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.islands.IslandsAgility;
import com.runecraft.player.Players;
import com.runecraft.skill.XpService;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** The Ape Atoll Agility Course: use the obstacles in order for their experience and the lap. */
final class IslandsAgilityService {
    /** The obstacle each player must use next. */
    private static final Map<UUID, Integer> NEXT = new HashMap<>();

    private IslandsAgilityService() {
    }

    static void use(ServerPlayerEntity player, BlockPos pos) {
        int index = IslandsAgility.indexAt(GielinorMap.toOsrsX(pos.getX()), GielinorMap.toOsrsY(pos.getZ()));
        if (index < 0) {
            Msg.game(player, "Nothing interesting happens.");
            return;
        }
        if (Players.data(player).skills().level(Skill.AGILITY) < IslandsAgility.APE_ATOLL_LEVEL) {
            Msg.game(player, "You need an Agility level of " + IslandsAgility.APE_ATOLL_LEVEL + " to attempt this course.");
            return;
        }
        int expected = NEXT.getOrDefault(player.getUuid(), 0);
        if (index != expected && index != 0) {
            Msg.game(player, "You must tackle the course's obstacles in order. Start at the stepping stone.");
            return;
        }
        IslandsAgility.Obstacle o = IslandsAgility.APE_ATOLL.get(index);
        ServerWorld world = Gielinor.world(player.getServer());
        if (world != null) {
            BlockPos land = Gielinor.safeSpot(world, GielinorMap.toBlockX(o.landX()) + 1, GielinorMap.toBlockZ(o.landY()) + 1);
            player.teleport(world, land.getX() + 0.5, land.getY(), land.getZ() + 0.5, player.getYaw(), player.getPitch());
        }
        XpService.add(player, Skill.AGILITY, o.xp());
        boolean last = index == IslandsAgility.APE_ATOLL.size() - 1;
        NEXT.put(player.getUuid(), last ? 0 : index + 1);
        Msg.game(player, last ? "You complete a lap of the Ape Atoll Agility Course." : "You cross the " + o.name().toLowerCase() + ".");
    }
}
