package com.runecraft.content;

/**
 * Server/common-side initialisation for a content module (register boss scripts, quest scripts,
 * block hooks, event listeners, extra blocks/items...). Listed in {@link ModuleHooksRegistry}.
 */
public interface ModuleHooks {
    /** Called at the end of {@code RuneCraft.onInitialize()}, after all registries are filled. */
    default void init() {
    }
}
