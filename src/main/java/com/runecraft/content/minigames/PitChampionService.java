package com.runecraft.content.minigames;

import com.runecraft.action.ActionManager;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.SkullService;
import com.runecraft.content.minigames.games.FightPitsSession;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.content.minigames.FightPits;
import com.runecraft.osrs.content.minigames.FightPits.SkullLoss;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * The TzHaar Fight Pit champion's red skull (OSRS Wiki, "TzHaar Fight Pit"): the last player standing wears it for an hour,
 * shown to everyone through {@link SkullService#setChampionSkull} (not a PvP skull: items kept on death don't change). It is
 * lost on being defeated in the pit, leaving a game early, teleporting, changing armour after leaving the pit, turning on an
 * overhead prayer, taking monster damage or logging out ({@link FightPits#skullLoss}).
 */
public final class PitChampionService {
    private static final class Champion {
        final long until;
        boolean inPit = true;
        Vec3d pos;
        RegistryKey<World> world;
        String gear;

        Champion(long until) {
            this.until = until;
        }
    }

    private static final Map<UUID, Champion> CHAMPIONS = new HashMap<>();
    private static final EquipmentSlot[] GEAR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.OFFHAND};

    private PitChampionService() {
    }

    static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 20 != 0 || CHAMPIONS.isEmpty()) {
                return;
            }
            long now = ActionManager.gameTick();
            Iterator<Map.Entry<UUID, Champion>> it = CHAMPIONS.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Champion> e = it.next();
                ServerPlayerEntity p = server.getPlayerManager().getPlayer(e.getKey());
                if (p == null) {
                    it.remove();
                    continue;
                }
                Champion c = e.getValue();
                boolean inPit = MinigameManager.session(p) instanceof FightPitsSession;
                if (inPit || c.inPit || c.pos == null) {
                    // In the pit (or just out of it, after the game's teleport back): only take a fresh snapshot.
                    c.inPit = inPit;
                    snapshot(c, p);
                    if (now >= c.until) {
                        it.remove();
                        lose(p, SkullLoss.EXPIRED);
                    }
                    continue;
                }
                boolean overhead = Players.data(p).activePrayers().stream().anyMatch(Prayer::isOverhead);
                SkullLoss loss = FightPits.skullLoss(now, c.until, p.getPos().distanceTo(c.pos), p.getWorld().getRegistryKey() == c.world,
                        !gear(p).equals(c.gear), overhead);
                if (loss != null) {
                    it.remove();
                    lose(p, loss);
                } else {
                    c.pos = p.getPos();
                }
            }
        });
        CombatHooks.addHurtListener((player, attacker, damage) -> {
            if (damage > 0 && attacker != null && !(attacker instanceof PlayerEntity) && CHAMPIONS.containsKey(player.getUuid())) {
                clear(player, SkullLoss.MONSTER_DAMAGE);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (CHAMPIONS.remove(handler.getPlayer().getUuid()) != null) {
                SkullService.setChampionSkull(handler.getPlayer(), false);
            }
        });
    }

    private static void snapshot(Champion c, ServerPlayerEntity p) {
        c.pos = p.getPos();
        c.world = p.getWorld().getRegistryKey();
        c.gear = gear(p);
    }

    /** Armour and shield ids. // approx: the weapon isn't watched, since the main hand follows the hotbar here */
    private static String gear(ServerPlayerEntity p) {
        StringBuilder sb = new StringBuilder();
        for (EquipmentSlot slot : GEAR) {
            sb.append(Inv.idOf(p.getEquippedStack(slot))).append('|');
        }
        return sb.toString();
    }

    /** The last player standing becomes champion: the red skull for an hour. */
    public static void crown(ServerPlayerEntity player) {
        CHAMPIONS.put(player.getUuid(), new Champion(ActionManager.gameTick() + FightPits.SKULL_TICKS));
        SkullService.setChampionSkull(player, true);
    }

    public static boolean isChampion(ServerPlayerEntity player) {
        return CHAMPIONS.containsKey(player.getUuid());
    }

    /** Removes the champion's skull (defeated in the pit, left a game early...). */
    public static void clear(ServerPlayerEntity player, SkullLoss reason) {
        if (CHAMPIONS.remove(player.getUuid()) != null) {
            lose(player, reason);
        }
    }

    private static void lose(ServerPlayerEntity player, SkullLoss reason) {
        SkullService.setChampionSkull(player, false);
        Msg.game(player, reason.message);
    }
}
