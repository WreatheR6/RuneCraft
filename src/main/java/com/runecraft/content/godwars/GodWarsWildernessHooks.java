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
        BossScripts.register("callisto", () -> new WildernessScripts.Bear(false));
        BossScripts.register("artio", () -> new WildernessScripts.Bear(true));
        BossScripts.register("venenatis", () -> new WildernessScripts.Spider(false));
        BossScripts.register("spindel", () -> new WildernessScripts.Spider(true));
        BossScripts.register("vetion", () -> new VetionScript(false));
        BossScripts.register("calvarion", () -> new VetionScript(true));
        BossScripts.register("scorpia", WildernessScripts.Scorpia::new);
        BossScripts.register("chaos_elemental", WildernessScripts.ChaosElemental::new);
        BossScripts.register("chaos_fanatic", WildernessScripts.ChaosFanatic::new);
        BossScripts.register("crazy_archaeologist", WildernessScripts.CrazyArchaeologist::new);
        BossScripts.register("king_black_dragon", WildernessScripts.KingBlackDragon::new);

        // The God Wars Dungeon at its real tiles: obstacles, chambers, altars, spawns, essence. The generals stand in their
        // chambers, so summoning stones left by older worlds only point the way.
        for (String general : new String[]{"kreearra", "commander_zilyana", "nex"}) {
            DataBossService.requirement(general, (player, def) -> {
                GwdKillcount.redirect(player, def.name);
                return false;
            });
        }
        GwdKillcount.registerStoneRedirect();
        GameEvents.onKill(GwdKillcount::onKill);
        GameEvents.onKill((player, entity, stats) -> GwdSpawner.onKill(entity));
        GwdDungeonHooks.register();
        GwdSpawner.register();
        GameEvents.onKill(RevenantLoot::onKill);
        GameEvents.onKill(MaledictusService::onKill);
        BossScripts.register(com.runecraft.osrs.content.godwars.RevenantRewards.MALEDICTUS, MaledictusService.Script::new);
        GwdCamps.register();
    }
}
