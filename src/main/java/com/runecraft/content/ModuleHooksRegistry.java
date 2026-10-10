package com.runecraft.content;

import com.runecraft.content.skilling.SkillingHooks;
import com.runecraft.content.slayer.SlayerHooks;
import com.runecraft.content.godwars.GodWarsWildernessHooks;
import com.runecraft.content.bosses.BossesHooks;
import com.runecraft.content.raids.RaidsHooks;
import com.runecraft.content.quests.QuestsHooks;
import com.runecraft.content.systems.SystemsHooks;
import com.runecraft.content.minigames.MinigamesHooks;
import com.runecraft.content.npcs.NpcsHooks;
import com.runecraft.content.zeah.ZeahHooks;
import com.runecraft.content.dungeons.DungeonsHooks;
import com.runecraft.content.extras.ExtrasHooks;
import com.runecraft.content.islands.IslandsHooks;

import java.util.List;

/** Minecraft-side hooks for every content module. */
public final class ModuleHooksRegistry {
    public static final List<ModuleHooks> ALL = List.of(
            new SkillingHooks(),
            new SlayerHooks(),
            new GodWarsWildernessHooks(),
            new BossesHooks(),
            new RaidsHooks(),
            new QuestsHooks(),
            new SystemsHooks(),
            new MinigamesHooks(),
            new NpcsHooks(),
            new ZeahHooks(),
            new DungeonsHooks(),
            new ExtrasHooks(),
            new IslandsHooks());

    private ModuleHooksRegistry() {
    }
}
