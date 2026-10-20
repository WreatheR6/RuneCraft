package com.runecraft.content.godwars;

import com.runecraft.boss.BossScripts;
import com.runecraft.boss.DataBossService;
import com.runecraft.content.GameEvents;
import com.runecraft.content.ModuleHooks;
import com.runecraft.osrs.content.godwars.GodWarsFactions.God;
import com.runecraft.osrs.content.godwars.GwdBosses;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;

/**
 * Minecraft-side behaviour for the godwars module: boss scripts (Kree'arra, Zilyana, Nex and her
 * generals, the Wilderness bosses), God Wars kill count (follower kills, chamber entry for all four
 * generals, ecumenical keys), the Ancient Prison's frozen door, follower camps and the built-in
 * generals' hilts, shards and pets.
 */
public final class GodWarsWildernessHooks implements ModuleHooks {
    @Override
    public void init() {
        // God Wars Dungeon.
        BossScripts.register("kreearra", KreeArraScript::new);
        BossScripts.register("commander_zilyana", ZilyanaScript::new);
        BossScripts.register("nex", NexScript::new);
        for (String minion : GwdBosses.NEX_MINIONS) {
            BossScripts.register(minion, NexMinionScript::new);
            DataBossService.companion(minion); // part of Nex's kill: no kill count of their own
        }
        // Wilderness.
        for (String id : new String[]{"callisto", "artio", "venenatis", "spindel"}) {
            BossScripts.register(id, WildernessScripts.PetOnly::new);
        }
        BossScripts.register("vetion", () -> new VetionScript(false));
        BossScripts.register("calvarion", () -> new VetionScript(true));
        BossScripts.register("scorpia", WildernessScripts.Scorpia::new);
        BossScripts.register("chaos_elemental", WildernessScripts.ChaosElemental::new);
        BossScripts.register("chaos_fanatic", WildernessScripts.ChaosFanatic::new);
        BossScripts.register("crazy_archaeologist", WildernessScripts.CrazyArchaeologist::new);
        BossScripts.register("king_black_dragon", WildernessScripts.KingBlackDragon::new);

        // Kill count gates.
        DataBossService.requirement("kreearra", (player, def) -> GwdKillcount.enter(player, God.ARMADYL, def.name));
        DataBossService.requirement("commander_zilyana", (player, def) -> GwdKillcount.enter(player, God.SARADOMIN, def.name));
        DataBossService.requirement("nex", (player, def) -> {
            if (Inv.has(player, "frozen_key", 1)) {
                Msg.game(player, "You unlock the frozen door to the Ancient Prison.");
                return true;
            }
            Msg.red(player, "The frozen door is sealed. You need a frozen key, made from the four frozen key pieces "
                    + "carried by the generals' bodyguards.");
            return false;
        });
        GwdKillcount.registerBuiltinGate();
        GameEvents.onKill(GwdKillcount::onKill);
        GameEvents.onKill(RevenantLoot::onKill);
        GameEvents.onKill(MaledictusService::onKill);
        BossScripts.register(com.runecraft.osrs.content.godwars.RevenantRewards.MALEDICTUS, MaledictusService.Script::new);
        GwdCamps.register();
    }
}
