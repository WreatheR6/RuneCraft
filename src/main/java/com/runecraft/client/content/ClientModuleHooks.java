package com.runecraft.client.content;

/** Client initialisation for a content module (entity renderers, screens, HUD elements). */
public interface ClientModuleHooks {
    default void initClient() {
    }
}
