package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.content.ContentModule;

/** Extra skilling content: new resources, recipes, minigame-free skilling methods, Construction, Sailing. */
public final class SkillingContent implements ContentModule {
    @Override
    public String id() {
        return "skilling";
    }
}
