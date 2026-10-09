package com.runecraft.boss;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Registry of boss scripts by BossDef id. */
public final class BossScripts {
    private static final Map<String, Supplier<BossScript>> SCRIPTS = new HashMap<>();
    private static final BossScript NONE = new BossScript() {
    };

    private BossScripts() {
    }

    public static void register(String bossId, Supplier<BossScript> factory) {
        SCRIPTS.put(bossId, factory);
    }

    public static BossScript create(String bossId) {
        Supplier<BossScript> f = SCRIPTS.get(bossId);
        return f == null ? NONE : f.get();
    }
}
