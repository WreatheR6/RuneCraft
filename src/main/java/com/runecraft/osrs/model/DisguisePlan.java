package com.runecraft.osrs.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * How a model looks while its creature is disguised (OSRS rock crabs lie as plain "Rocks" until a
 * player comes close): only the subtree of one part (the shell, e.g. {@code "boulder"}) is drawn; its
 * ancestors keep their transforms but not their own boxes, everything else is hidden, and the whole
 * model is lowered by {@link #drop()} pixels so the shell rests on the ground instead of on legs.
 */
public final class DisguisePlan {
    /** Name of the part whose subtree is the disguise, for the creatures that have one. */
    public static final String SHELL = "boulder";
    /** Model-space y of the ground (Minecraft entity models stand on y = 24). */
    private static final float GROUND = 24f;

    private final Set<String> shown;
    private final Set<String> ancestors;
    private final float drop;

    private DisguisePlan(Set<String> shown, Set<String> ancestors, float drop) {
        this.shown = Collections.unmodifiableSet(shown);
        this.ancestors = Collections.unmodifiableSet(ancestors);
        this.drop = drop;
    }

    /** The disguise of a model whose part {@code shell} is the disguise, or null if it has no such part. */
    public static DisguisePlan of(ModelSpec spec, String shell) {
        Map<String, ModelSpec.Part> byName = new HashMap<>();
        for (ModelSpec.Part p : spec.parts()) {
            byName.put(p.name, p);
        }
        if (!byName.containsKey(shell)) {
            return null;
        }
        Set<String> shown = new HashSet<>();
        for (ModelSpec.Part p : spec.parts()) {
            for (ModelSpec.Part q = p; q != null; q = q.parent == null ? null : byName.get(q.parent)) {
                if (q.name.equals(shell)) {
                    shown.add(p.name);
                    break;
                }
            }
        }
        Set<String> ancestors = new HashSet<>();
        for (ModelSpec.Part q = byName.get(byName.get(shell).parent); q != null; q = q.parent == null ? null : byName.get(q.parent)) {
            ancestors.add(q.name);
        }
        // lowest point of the shell (pivots summed down the chain; part rotations are small and ignored)
        float bottom = Float.NEGATIVE_INFINITY;
        for (String name : shown) {
            ModelSpec.Part p = byName.get(name);
            float y = 0;
            for (ModelSpec.Part q = p; q != null; q = q.parent == null ? null : byName.get(q.parent)) {
                y += q.py;
            }
            for (ModelSpec.Box b : p.boxes) {
                bottom = Math.max(bottom, y + b.y() + b.h());
            }
        }
        float drop = bottom == Float.NEGATIVE_INFINITY ? 0 : Math.max(0, GROUND - bottom);
        return new DisguisePlan(shown, ancestors, drop);
    }

    /** Parts drawn while disguised (the shell and everything on it). */
    public Set<String> shown() {
        return shown;
    }

    /** Parts above the shell: transforms kept, own boxes not drawn. */
    public Set<String> ancestors() {
        return ancestors;
    }

    /** True if a part is drawn at all while disguised (shown, or an ancestor passing through to the shell). */
    public boolean visible(String part) {
        return shown.contains(part) || ancestors.contains(part);
    }

    /** Pixels the model is lowered by while disguised. */
    public float drop() {
        return drop;
    }
}
