package com.runecraft.osrs.content;

import com.runecraft.osrs.content.bosses.BossesContent;
import com.runecraft.osrs.content.godwars.GodWarsWildernessContent;
import com.runecraft.osrs.content.minigames.MinigamesContent;
import com.runecraft.osrs.content.npcs.NpcsContent;
import com.runecraft.osrs.content.quests.QuestsContent;
import com.runecraft.osrs.content.raids.RaidsContent;
import com.runecraft.osrs.content.skilling.SkillingContent;
import com.runecraft.osrs.content.slayer.SlayerContent;
import com.runecraft.osrs.content.systems.SystemsContent;
import com.runecraft.osrs.content.world.WorldContent;
import com.runecraft.osrs.content.zeah.ZeahContent;

import java.util.List;

/** Every content module, in load order. */
public final class Modules {
    public static final List<ContentModule> ALL = List.of(
            new SkillingContent(),
            new SlayerContent(),
            new GodWarsWildernessContent(),
            new BossesContent(),
            new RaidsContent(),
            new QuestsContent(),
            new SystemsContent(),
            new MinigamesContent(),
            new WorldContent(),
            new NpcsContent(),
            new ZeahContent());

    private Modules() {
    }
}
