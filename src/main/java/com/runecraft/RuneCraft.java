package com.runecraft;

import com.runecraft.action.ActionManager;
import com.runecraft.combat.CombatHandler;
import com.runecraft.command.RuneCraftCommands;
import com.runecraft.network.ServerNetworking;
import com.runecraft.player.PlayerEvents;
import com.runecraft.registry.ModBlocks;
import com.runecraft.registry.ModEntities;
import com.runecraft.registry.ModItemGroups;
import com.runecraft.registry.ModItems;
import com.runecraft.skill.Interactions;
import com.runecraft.world.GielinorBiomeSource;
import com.runecraft.world.GielinorTerrainDensity;
import com.runecraft.world.LandmarkBuilder;
import com.runecraft.world.LocationSpawner;
import com.runecraft.world.StructurePlacer;
import com.runecraft.world.TerrainFeatures;
import net.fabricmc.api.ModInitializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RuneCraft implements ModInitializer {
    public static final String MOD_ID = "runecraft";
    public static final Logger LOGGER = LoggerFactory.getLogger("RuneCraft");

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    /** Resolves a catalog item id: bare ids are RuneCraft items, namespaced ids are used as-is. */
    public static Identifier resolve(String itemId) {
        return itemId.contains(":") ? new Identifier(itemId) : id(itemId);
    }

    @Override
    public void onInitialize() {
        ModBlocks.register();
        ModItems.register();
        ModEntities.register();
        ModItemGroups.register();
        Registry.register(Registries.BIOME_SOURCE, id("gielinor"), GielinorBiomeSource.CODEC);
        GielinorTerrainDensity.register();
        TerrainFeatures.register();

        ServerNetworking.register();
        com.runecraft.bank.BankService.register();
        PlayerEvents.register();
        ActionManager.register();
        CombatHandler.register();
        Interactions.register();
        StructurePlacer.register();
        LandmarkBuilder.register();
        LocationSpawner.register();
        RuneCraftCommands.register();
        com.runecraft.boss.Mechanics.register();
        for (com.runecraft.content.ModuleHooks hooks : com.runecraft.content.ModuleHooksRegistry.ALL) {
            hooks.init();
        }
        LOGGER.info("Welcome to Gielinor. {} items, {} blocks registered.", ModItems.count(), ModBlocks.count());
    }
}
