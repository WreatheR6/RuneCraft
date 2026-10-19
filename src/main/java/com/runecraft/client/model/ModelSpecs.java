package com.runecraft.client.model;

import com.runecraft.RuneCraft;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.platform.client.EntityModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import java.util.HashMap;
import java.util.Map;

/** Turns {@link ModelSpec}s into registered Minecraft model layers. */
public final class ModelSpecs {
    private static final Map<String, ModelLayerLocation> LAYERS = new HashMap<>();

    private ModelSpecs() {
    }

    public static void registerAll() {
        for (ModelSpec spec : ContentRegistry.models().values()) {
            ModelLayerLocation layer = new ModelLayerLocation(RuneCraft.id(spec.id()), "main");
            LAYERS.put(spec.id(), layer);
            EntityModelLayerRegistry.registerModelLayer(layer, () -> build(spec));
        }
    }

    public static ModelLayerLocation layer(String specId) {
        return LAYERS.get(specId);
    }

    public static LayerDefinition build(ModelSpec spec) {
        MeshDefinition data = new MeshDefinition();
        Map<String, PartDefinition> built = new HashMap<>();
        boolean progress = true;
        while (built.size() < spec.parts().size() && progress) {
            progress = false;
            for (ModelSpec.Part p : spec.parts()) {
                if (built.containsKey(p.name)) {
                    continue;
                }
                PartDefinition parent = p.parent == null ? data.getRoot() : built.get(p.parent);
                if (parent == null) {
                    continue;
                }
                CubeListBuilder builder = CubeListBuilder.create();
                for (int i = 0; i < p.boxes.size(); i++) {
                    ModelSpec.Box b = p.boxes.get(i);
                    int[] uv = p.uvs.get(i);
                    builder.texOffs(uv[0], uv[1]).addBox(b.x(), b.y(), b.z(), b.w(), b.h(), b.d(), new CubeDeformation(b.dilation()));
                }
                built.put(p.name, parent.addOrReplaceChild(p.name, builder, PartPose.offsetAndRotation(p.px, p.py, p.pz, p.pitch, p.yaw, p.roll)));
                progress = true;
            }
        }
        return LayerDefinition.create(data, spec.texWidth(), spec.texHeight());
    }
}
