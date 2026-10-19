package com.runecraft.platform.client;

import com.runecraft.platform.Event;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;

import java.util.function.Consumer;

/**
 * The part of Fabric's model loading API the mod uses: wrapping models after baking. Listeners run from NeoForge's
 * {@code ModelEvent.ModifyBakingResult} once per baked model, keyed by its {@link ModelResourceLocation}.
 */
public final class ModelLoadingPlugin {
    private ModelLoadingPlugin() {
    }

    @FunctionalInterface
    public interface AfterBake {
        BakedModel modifyModelAfterBake(BakedModel model, AfterBakeContext context);
    }

    public record AfterBakeContext(ModelResourceLocation id) {
    }

    public static final class Context {
        private Context() {
        }

        public Event<AfterBake> modifyModelAfterBake() {
            return AFTER_BAKE;
        }
    }

    static final Event<AfterBake> AFTER_BAKE = new Event<>(listeners -> (model, context) -> {
        BakedModel current = model;
        for (AfterBake l : listeners) {
            current = l.modifyModelAfterBake(current, context);
        }
        return current;
    });

    private static final Context CONTEXT = new Context();

    public static void register(Consumer<Context> plugin) {
        plugin.accept(CONTEXT);
    }
}
