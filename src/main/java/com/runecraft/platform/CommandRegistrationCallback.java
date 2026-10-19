package com.runecraft.platform;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/** Fabric's command registration callback, fired from NeoForge's {@code RegisterCommandsEvent}. */
@FunctionalInterface
public interface CommandRegistrationCallback {
    void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment);

    Event<CommandRegistrationCallback> EVENT = new Event<>(listeners -> (dispatcher, access, env) -> {
        for (CommandRegistrationCallback l : listeners) {
            l.register(dispatcher, access, env);
        }
    });
}
