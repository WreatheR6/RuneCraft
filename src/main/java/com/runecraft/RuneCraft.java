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
import com.runecraft.platform.PlatformEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(RuneCraft.MOD_ID)
public class RuneCraft {
    public static final String MOD_ID = "runecraft";
    public static final Logger LOGGER = LoggerFactory.getLogger("RuneCraft");

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Resolves a catalog item id: bare ids are RuneCraft items, namespaced ids are used as-is. */
    public static ResourceLocation resolve(String itemId) {
        return itemId.contains(":") ? ResourceLocation.parse(itemId) : id(itemId);
    }

    private static boolean initialized;

    public RuneCraft(IEventBus modBus) {
        PlatformEvents.init(modBus);
        modBus.addListener(RegisterEvent.class, RuneCraft::register);
        modBus.addListener(EntityAttributeCreationEvent.class, ModEntities::registerAttributes);
        modBus.addListener(RegisterSpawnPlacementsEvent.class, ModEntities::registerSpawnPlacements);
        modBus.addListener(FMLCommonSetupEvent.class, e -> e.enqueueWork(RuneCraft::initialize));
    }

    /**
     * Fills the game registries, each inside NeoForge's registration event for that registry (they are frozen at
     * any other time). Blocks also register their block items: every registry is open while the events run.
     */
    private static void register(RegisterEvent event) {
        ResourceKey<? extends Registry<?>> key = event.getRegistryKey();
        if (key.equals(Registries.BLOCK)) {
            ModBlocks.register();
        } else if (key.equals(Registries.ENTITY_TYPE)) {
            ModEntities.register();
            com.runecraft.content.extras.ExtrasEntities.register();
        } else if (key.equals(Registries.ITEM)) {
            ModItems.register();
        } else if (key.equals(Registries.CREATIVE_MODE_TAB)) {
            ModItemGroups.register();
        } else if (key.equals(Registries.BIOME_SOURCE)) {
            Registry.register(BuiltInRegistries.BIOME_SOURCE, id("gielinor"), GielinorBiomeSource.CODEC);
        } else if (key.equals(Registries.DENSITY_FUNCTION_TYPE)) {
            GielinorTerrainDensity.register();
        }
    }

    /**
     * The rest of the old Fabric {@code onInitialize}: hooks, networking, commands and module init. Runs once after
     * registration: on the client from the first client registration event (before the client init), on a dedicated
     * server from common setup.
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
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
        com.runecraft.selftest.SelfTest.register();
        com.runecraft.boss.Mechanics.register();
        for (com.runecraft.content.ModuleHooks hooks : com.runecraft.content.ModuleHooksRegistry.ALL) {
            hooks.init();
        }
        LOGGER.info("Welcome to Gielinor. {} items, {} blocks registered.", ModItems.count(), ModBlocks.count());
    }
}
