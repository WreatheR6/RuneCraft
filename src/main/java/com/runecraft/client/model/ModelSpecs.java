package com.runecraft.client.model;

import com.runecraft.RuneCraft;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.model.ModelSpec;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;

import java.util.HashMap;
import java.util.Map;

/** Turns {@link ModelSpec}s into registered Minecraft model layers. */
public final class ModelSpecs {
    private static final Map<String, EntityModelLayer> LAYERS = new HashMap<>();

    private ModelSpecs() {
    }

    public static void registerAll() {
        for (ModelSpec spec : ContentRegistry.models().values()) {
            EntityModelLayer layer = new EntityModelLayer(RuneCraft.id(spec.id()), "main");
            LAYERS.put(spec.id(), layer);
            EntityModelLayerRegistry.registerModelLayer(layer, () -> build(spec));
        }
    }

    public static EntityModelLayer layer(String specId) {
        return LAYERS.get(specId);
    }

    public static TexturedModelData build(ModelSpec spec) {
        ModelData data = new ModelData();
        Map<String, ModelPartData> built = new HashMap<>();
        boolean progress = true;
        while (built.size() < spec.parts().size() && progress) {
            progress = false;
            for (ModelSpec.Part p : spec.parts()) {
                if (built.containsKey(p.name)) {
                    continue;
                }
                ModelPartData parent = p.parent == null ? data.getRoot() : built.get(p.parent);
                if (parent == null) {
                    continue;
                }
                ModelPartBuilder builder = ModelPartBuilder.create();
                for (int i = 0; i < p.boxes.size(); i++) {
                    ModelSpec.Box b = p.boxes.get(i);
                    int[] uv = p.uvs.get(i);
                    builder.uv(uv[0], uv[1]).cuboid(b.x(), b.y(), b.z(), b.w(), b.h(), b.d(), new Dilation(b.dilation()));
                }
                built.put(p.name, parent.addChild(p.name, builder, ModelTransform.of(p.px, p.py, p.pz, p.pitch, p.yaw, p.roll)));
                progress = true;
            }
        }
        return TexturedModelData.of(data, spec.texWidth(), spec.texHeight());
    }
}
