package com.runecraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.runecraft.entity.Disguisable;
import com.runecraft.osrs.model.DisguisePlan;
import com.runecraft.osrs.model.ModelSpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/** An entity model built from a {@link ModelSpec}, animated procedurally by part role. */
public class GenericModel<T extends Entity> extends HierarchicalModel<T> {
    private record Animated(ModelPart part, ModelSpec.Part spec) {
    }

    private final ModelPart root;
    private final List<Animated> animated = new ArrayList<>();
    /** Every part of the tree, collected once (setAngles used to stream root.traverse() every frame). */
    private final ModelPart[] allParts;
    /** Glow pass: parts whose own boxes have no emissive texels (children still drawn), or null = draw everything. */
    private ModelPart[] glowHidden;
    /** Glow pass: parts with nothing emissive in their whole subtree (not drawn at all). */
    private ModelPart[] glowPruned;
    /** Scratch for restoring the flags after the glow pass (render thread only; no per-frame allocation). */
    private boolean[] wasHidden;
    private boolean[] wasVisible;
    /** How the model looks while its creature is disguised (rock crabs as plain rocks), or null. */
    private final DisguisePlan disguise;
    /** Per animated part, while disguised: drawn at all / own boxes skipped (ancestor of the shell). */
    private boolean[] disguiseVisible;
    private boolean[] disguiseAncestor;

    public GenericModel(ModelPart root, ModelSpec spec) {
        this.root = root;
        for (ModelSpec.Part p : spec.parts()) {
            ModelPart part = find(root, spec, p);
            if (part != null) {
                animated.add(new Animated(part, p));
            }
        }
        this.allParts = root.getAllParts().toArray(ModelPart[]::new);
        planGlow(spec);
        this.disguise = DisguisePlan.of(spec, DisguisePlan.SHELL);
        if (disguise != null) {
            disguiseVisible = new boolean[animated.size()];
            disguiseAncestor = new boolean[animated.size()];
            for (int i = 0; i < animated.size(); i++) {
                String name = animated.get(i).spec().name;
                disguiseVisible[i] = disguise.visible(name);
                disguiseAncestor[i] = disguise.ancestors().contains(name);
            }
        }
    }

    /**
     * Shows only the shell while the creature is disguised (rock crabs lying as "Rocks"), the whole
     * model otherwise. Returns true if disguised (the caller then skips the animations).
     */
    private boolean applyDisguise(T entity) {
        if (disguise == null) {
            return false;
        }
        boolean disguised = entity instanceof Disguisable d && d.isDisguised();
        for (int i = 0; i < animated.size(); i++) {
            Animated a = animated.get(i);
            a.part().visible = !disguised || disguiseVisible[i];
            a.part().skipDraw = disguised && disguiseAncestor[i];
            if (disguised && a.spec().parent == null) {
                a.part().y += disguise.drop();
            }
        }
        return disguised;
    }

    /** Works out which parts the glow pass can skip, from the lit-part list made with the glow texture. */
    private void planGlow(ModelSpec spec) {
        Set<String> lit = GlowParts.lit(spec.id());
        if (lit == null || animated.size() != spec.parts().size()) {
            return;
        }
        Map<String, ModelSpec.Part> byName = new HashMap<>();
        for (ModelSpec.Part p : spec.parts()) {
            byName.put(p.name, p);
        }
        if (!byName.keySet().containsAll(lit)) {
            return; // stale list (parts renamed since the textures were made): draw everything
        }
        Set<String> subtreeLit = new HashSet<>();
        for (String name : lit) {
            for (ModelSpec.Part p = byName.get(name); p != null && subtreeLit.add(p.name); p = p.parent == null ? null : byName.get(p.parent)) {
                // marks the lit part and every ancestor
            }
        }
        List<ModelPart> hidden = new ArrayList<>();
        List<ModelPart> pruned = new ArrayList<>();
        for (Animated a : animated) {
            if (!subtreeLit.contains(a.spec().name)) {
                pruned.add(a.part());
            } else if (!lit.contains(a.spec().name)) {
                hidden.add(a.part());
            }
        }
        glowHidden = hidden.toArray(new ModelPart[0]);
        glowPruned = pruned.toArray(new ModelPart[0]);
        wasHidden = new boolean[glowHidden.length];
        wasVisible = new boolean[glowPruned.length];
    }

    /**
     * Draws the model for the full-bright glow layer: only the parts with emissive texels (all other
     * texels of the glow texture are transparent black, which the additive blend ignores, so the result
     * is the same as drawing the whole model with a fraction of the vertices).
     */
    public void renderGlow(PoseStack matrices, VertexConsumer vertices, int light, int overlay) {
        if (glowHidden == null) {
            root.render(matrices, vertices, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
            return;
        }
        for (int i = 0; i < glowHidden.length; i++) {
            wasHidden[i] = glowHidden[i].skipDraw;
            glowHidden[i].skipDraw = true;
        }
        for (int i = 0; i < glowPruned.length; i++) {
            wasVisible[i] = glowPruned[i].visible;
            glowPruned[i].visible = false;
        }
        try {
            root.render(matrices, vertices, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
        } finally {
            for (int i = 0; i < glowHidden.length; i++) {
                glowHidden[i].skipDraw = wasHidden[i];
            }
            for (int i = 0; i < glowPruned.length; i++) {
                glowPruned[i].visible = wasVisible[i];
            }
        }
    }

    private static ModelPart find(ModelPart root, ModelSpec spec, ModelSpec.Part target) {
        List<String> path = new ArrayList<>();
        ModelSpec.Part current = target;
        while (current != null) {
            path.add(0, current.name);
            String parent = current.parent;
            current = parent == null ? null : spec.parts().stream().filter(x -> x.name.equals(parent)).findFirst().orElse(null);
        }
        ModelPart part = root;
        for (String name : path) {
            if (!part.hasChild(name)) {
                return null;
            }
            part = part.getChild(name);
        }
        return part;
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        for (ModelPart part : allParts) {
            part.resetPose();
        }
        if (applyDisguise(entity)) {
            return;
        }
        float walk = Mth.cos(limbAngle * 0.6662f) * 1.2f * limbDistance;
        float swing = Mth.sin(Mth.sqrt(attackTime) * (float) Math.PI);
        for (Animated a : animated) {
            ModelPart p = a.part();
            switch (a.spec().role) {
                case HEAD -> {
                    p.yRot += headYaw * ((float) Math.PI / 180f);
                    p.xRot += headPitch * ((float) Math.PI / 180f);
                }
                case NECK -> {
                    p.yRot += headYaw * ((float) Math.PI / 180f) * 0.3f;
                    p.xRot += Mth.sin(animationProgress * 0.05f) * 0.05f;
                }
                case JAW -> p.xRot += swing * 0.7f + Math.max(0, Mth.sin(animationProgress * 0.1f)) * 0.08f;
                case LEG_FRONT_LEFT, LEG_BACK_RIGHT -> p.xRot += walk;
                case LEG_FRONT_RIGHT, LEG_BACK_LEFT -> p.xRot -= walk;
                case LEG_EXTRA -> {
                    float phase = a.spec().index % 2 == 0 ? walk : -walk;
                    p.yRot += phase * 0.5f;
                    p.zRot += Math.abs(phase) * 0.3f * (a.spec().index % 4 < 2 ? 1 : -1);
                }
                case ARM_LEFT -> {
                    p.xRot -= walk * 0.8f;
                    p.xRot -= swing * 0.6f;
                }
                case ARM_RIGHT -> {
                    p.xRot += walk * 0.8f;
                    p.xRot -= swing * 1.4f;
                }
                case WING_LEFT -> p.zRot -= 0.3f + Mth.sin(animationProgress * 0.25f) * 0.6f;
                case WING_RIGHT -> p.zRot += 0.3f + Mth.sin(animationProgress * 0.25f) * 0.6f;
                case TAIL -> {
                    p.yRot += Mth.sin(animationProgress * 0.12f) * 0.35f;
                    p.xRot += walk * 0.15f;
                }
                case TENTACLE -> {
                    p.xRot += Mth.sin(animationProgress * 0.15f + a.spec().index) * 0.3f;
                    p.zRot += Mth.cos(animationProgress * 0.13f + a.spec().index) * 0.2f;
                }
                case FIN -> p.yRot += Mth.sin(animationProgress * 0.3f) * 0.3f;
                case SPIN -> p.yRot += animationProgress * 0.05f;
                case FLOAT -> p.y += Mth.sin(animationProgress * 0.1f) * 1.5f;
                case BODY -> p.y += Mth.sin(animationProgress * 0.08f) * 0.3f;
                default -> {
                }
            }
        }
    }
}
