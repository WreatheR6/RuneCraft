package com.runecraft.client.content;

import com.runecraft.client.content.skilling.SkillingClientHooks;
import com.runecraft.client.content.slayer.SlayerClientHooks;
import com.runecraft.client.content.godwars.GodWarsWildernessClientHooks;
import com.runecraft.client.content.bosses.BossesClientHooks;
import com.runecraft.client.content.raids.RaidsClientHooks;
import com.runecraft.client.content.quests.QuestsClientHooks;
import com.runecraft.client.content.systems.SystemsClientHooks;
import com.runecraft.client.content.minigames.MinigamesClientHooks;
import com.runecraft.client.content.npcs.NpcsClientHooks;
import com.runecraft.client.content.zeah.ZeahClientHooks;
import com.runecraft.client.content.extras.ExtrasClientHooks;

import java.util.List;

/** Client hooks for every content module. */
public final class ClientModuleHooksRegistry {
    public static final List<ClientModuleHooks> ALL = List.of(
            new SkillingClientHooks(),
            new SlayerClientHooks(),
            new GodWarsWildernessClientHooks(),
            new BossesClientHooks(),
            new RaidsClientHooks(),
            new QuestsClientHooks(),
            new SystemsClientHooks(),
            new MinigamesClientHooks(),
            new NpcsClientHooks(),
            new ZeahClientHooks(),
            new ExtrasClientHooks());

    private ClientModuleHooksRegistry() {
    }
}
