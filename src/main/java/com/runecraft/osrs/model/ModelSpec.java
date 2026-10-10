package com.runecraft.osrs.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A custom entity model described as data: a tree of boxes ("parts"), each with a role that drives
 * its animation (legs walk, wings flap, tails sway, heads look at you, jaws chomp when attacking).
 *
 * <p>UV coordinates are packed automatically by {@link #layout()} so the Java renderer and the
 * Python texture generator ({@code tools/gen_assets.py}) agree on where every face lives. Units
 * are Minecraft model pixels (16 per block); the root pivot sits at the entity's feet with Y=24
 * meaning "on the ground" exactly like vanilla models (vanilla flips Y: smaller Y is higher).
 *
 * <p>Build with the fluent {@link Builder}:
 * <pre>
 * ModelSpec.builder("giant_mole")
 *     .part("body", null, Role.BODY, 0x5A3A2A).pivot(0, 18, 0).box(-8, -6, -12, 16, 12, 24)
 *     .part("head", "body", Role.HEAD, 0x6A4A3A).pivot(0, -1, -12).box(-5, -4, -8, 10, 8, 8)
 *     .build();
 * </pre>
 */
public final class ModelSpec {
    /** Animation role of a part. */
    public enum Role {
        BODY, HEAD, NECK, JAW,
        LEG_FRONT_LEFT, LEG_FRONT_RIGHT, LEG_BACK_LEFT, LEG_BACK_RIGHT,
        /** Extra legs (spiders, scorpions): alternate swing by index parity. */
        LEG_EXTRA,
        ARM_LEFT, ARM_RIGHT, WING_LEFT, WING_RIGHT, TAIL, TENTACLE, FIN,
        /** Spins slowly around Y (orbs, crystals, rings). */
        SPIN,
        /** Bobs up and down (floating heads, eyes). */
        FLOAT,
        STATIC
    }

    /** One box of a part, in model pixels relative to the part pivot. */
    public record Box(float x, float y, float z, int w, int h, int d, float dilation) {
    }

    /** A model part. {@code u}/{@code v} are filled in by layout(). */
    public static final class Part {
        public final String name;
        public final String parent;
        public final Role role;
        public final int color;
        public final int accent;
        public float px;
        public float py;
        public float pz;
        public float pitch;
        public float yaw;
        public float roll;
        public final List<Box> boxes = new ArrayList<>();
        public final List<int[]> uvs = new ArrayList<>();
        /** Face detail painted by the texture generator: "eyes", "scales", "stripes", "spots", "plates", "glow", "none". */
        public String pattern = "none";
        /** For LEG_EXTRA: index used to alternate the gait. */
        public int index;
        /**
         * Emissive detail rendered full-bright on top of the model (an extra "_glow" texture layer):
         * "none", "full" (the whole part), "eyes" (eyes on the front face), "cracks" (lava veins),
         * "maw" (mouth/inner faces), "runes" (glowing glyphs/trim).
         */
        public String glow = "none";
        /**
         * Surface material for the texture painter (null = inferred from the pattern and names): e.g. "skin",
         * "rock", "scales", "fur", "feathers", "cloth", "leather", "metal", "chain", "bone", "chitin", "membrane",
         * "slime", "hair", "wood", "ghost", "moss", "ice", "flesh", "face", "lava".
         */
        public String material;

        Part(String name, String parent, Role role, int color, int accent) {
            this.name = name;
            this.parent = parent;
            this.role = role;
            this.color = color;
            this.accent = accent;
        }
    }

    private final String id;
    private final List<Part> parts;
    private final float scale;
    private final float shadow;
    private int texWidth = 64;
    private int texHeight = 64;

    private ModelSpec(String id, List<Part> parts, float scale, float shadow) {
        this.id = id;
        this.parts = Collections.unmodifiableList(parts);
        this.scale = scale;
        this.shadow = shadow;
        layout();
    }

    public String id() {
        return id;
    }

    public List<Part> parts() {
        return parts;
    }

    /** Render scale applied on top of the model (lets small models represent huge bosses). */
    public float scale() {
        return scale;
    }

    public float shadowRadius() {
        return shadow;
    }

    public int texWidth() {
        return texWidth;
    }

    public int texHeight() {
        return texHeight;
    }

    /** True if some part has emissive detail, i.e. the model has a full-bright "_glow" texture layer. */
    public boolean hasGlow() {
        for (Part p : parts) {
            if (!"none".equals(p.glow) || "glow".equals(p.pattern)) {
                return true;
            }
        }
        return false;
    }

    /** Shelf-packs every box's UV region (2*(d+w) wide, d+h tall) into the smallest power-of-two texture. */
    private void layout() {
        for (int size : new int[]{64, 128, 256, 512, 1024}) {
            if (tryLayout(size)) {
                texWidth = size;
                return;
            }
        }
        throw new IllegalStateException("Model " + id + " is too large to texture");
    }

    private boolean tryLayout(int width) {
        int x = 0;
        int y = 0;
        int rowHeight = 0;
        for (Part p : parts) {
            p.uvs.clear();
            for (Box b : p.boxes) {
                int bw = 2 * (b.d() + b.w());
                int bh = b.d() + b.h();
                if (bw > width) {
                    return false;
                }
                if (x + bw > width) {
                    x = 0;
                    y += rowHeight;
                    rowHeight = 0;
                }
                p.uvs.add(new int[]{x, y});
                x += bw;
                rowHeight = Math.max(rowHeight, bh);
            }
        }
        int h = y + rowHeight;
        int th = 64;
        while (th < h) {
            th *= 2;
        }
        if (th > width * 2) {
            return false;
        }
        texHeight = th;
        return true;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private final List<Part> parts = new ArrayList<>();
        private Part current;
        private float scale = 1.0f;
        private float shadow = 0.5f;
        private int extraLegs;

        private Builder(String id) {
            this.id = id;
        }

        public Builder scale(float scale) {
            this.scale = scale;
            return this;
        }

        public Builder shadow(float shadow) {
            this.shadow = shadow;
            return this;
        }

        /** Starts a new part. {@code parent} is null for root-level parts. */
        public Builder part(String name, String parent, Role role, int color) {
            return part(name, parent, role, color, darker(color));
        }

        public Builder part(String name, String parent, Role role, int color, int accent) {
            current = new Part(name, parent, role, color, accent);
            if (role == Role.LEG_EXTRA) {
                current.index = extraLegs++;
            }
            parts.add(current);
            return this;
        }

        public Builder pivot(float x, float y, float z) {
            current.px = x;
            current.py = y;
            current.pz = z;
            return this;
        }

        /** Initial rotation in degrees. */
        public Builder rotate(float pitch, float yaw, float roll) {
            current.pitch = (float) Math.toRadians(pitch);
            current.yaw = (float) Math.toRadians(yaw);
            current.roll = (float) Math.toRadians(roll);
            return this;
        }

        public Builder box(float x, float y, float z, int w, int h, int d) {
            current.boxes.add(new Box(x, y, z, w, h, d, 0f));
            return this;
        }

        public Builder box(float x, float y, float z, int w, int h, int d, float dilation) {
            current.boxes.add(new Box(x, y, z, w, h, d, dilation));
            return this;
        }

        public Builder pattern(String pattern) {
            current.pattern = pattern;
            return this;
        }

        /** Emissive detail for the current part (see {@link Part#glow}). */
        public Builder glow(String glow) {
            current.glow = glow == null ? "none" : glow;
            return this;
        }

        /** Surface material for the current part (see {@link Part#material}). */
        public Builder material(String material) {
            current.material = material;
            return this;
        }

        /** Name of the part most recently started (handy for kits that attach children). */
        public String currentName() {
            return current == null ? null : current.name;
        }

        public ModelSpec build() {
            for (Part p : parts) {
                if (p.boxes.isEmpty()) {
                    throw new IllegalStateException(id + ": part " + p.name + " has no boxes");
                }
                if (p.parent != null && parts.stream().noneMatch(o -> o.name.equals(p.parent))) {
                    throw new IllegalStateException(id + ": part " + p.name + " has unknown parent " + p.parent);
                }
            }
            return new ModelSpec(id, new ArrayList<>(parts), scale, shadow);
        }

        private static int darker(int c) {
            int r = (c >> 16 & 0xFF) * 3 / 4;
            int g = (c >> 8 & 0xFF) * 3 / 4;
            int b = (c & 0xFF) * 3 / 4;
            return r << 16 | g << 8 | b;
        }
    }
}
