package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

/**
 * Sculpting helpers on top of {@link ModelSpec.Builder}: mirrored left/right part pairs and single parts
 * with material/glow in one fluent call. Coordinates follow ModelSpec (model pixels, Y=24 is the ground,
 * smaller Y is higher, -Z is the front). The "left" part sits at +X.
 */
public final class Kit {
    private Kit() {
    }

    /** A single part, built fluently and emitted with {@link P#add()}. */
    public static P part(ModelSpec.Builder b, String name, String parent, Role role, int color) {
        return new P(b, name, parent, role, color);
    }

    /** A mirrored pair: "name_l" at +X and "name_r" at -X; a sided parent "x" means "x_l"/"x_r". */
    public static Sym sym(ModelSpec.Builder b, String name, String parent, boolean sidedParent, int color) {
        return new Sym(b, name, parent, sidedParent, color);
    }

    public static class P {
        final ModelSpec.Builder b;
        final String name;
        final String parent;
        Role role;
        final int color;
        Integer accent;
        float px;
        float py;
        float pz;
        float pitch;
        float yaw;
        float roll;
        String material;
        String glow;
        String pattern;
        final List<float[]> boxes = new ArrayList<>();

        P(ModelSpec.Builder b, String name, String parent, Role role, int color) {
            this.b = b;
            this.name = name;
            this.parent = parent;
            this.role = role;
            this.color = color;
        }

        public P accent(int accent) {
            this.accent = accent;
            return this;
        }

        public P at(float x, float y, float z) {
            px = x;
            py = y;
            pz = z;
            return this;
        }

        public P rot(float pitch, float yaw, float roll) {
            this.pitch = pitch;
            this.yaw = yaw;
            this.roll = roll;
            return this;
        }

        public P mat(String material) {
            this.material = material;
            return this;
        }

        public P glow(String glow) {
            this.glow = glow;
            return this;
        }

        public P pattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        public P box(float x, float y, float z, int w, int h, int d) {
            boxes.add(new float[]{x, y, z, w, h, d, 0});
            return this;
        }

        public P box(float x, float y, float z, int w, int h, int d, float dilation) {
            boxes.add(new float[]{x, y, z, w, h, d, dilation});
            return this;
        }

        /** A box centred on the pivot in X and Z, hanging down from it by {@code y0}. */
        public P cbox(float y0, int w, int h, int d) {
            return box(-w / 2f, y0, -d / 2f, w, h, d);
        }

        public ModelSpec.Builder add() {
            emit(name, parent, role, px, py, pz, pitch, yaw, roll, false);
            return b;
        }

        void emit(String n, String par, Role r, float x, float y, float z, float pi, float ya, float ro, boolean mirror) {
            if (accent != null) {
                b.part(n, par, r, color, accent);
            } else {
                b.part(n, par, r, color);
            }
            b.pivot(x, y, z).rotate(pi, ya, ro);
            for (float[] bx : boxes) {
                float bxX = mirror ? -(bx[0] + bx[3]) : bx[0];
                b.box(bxX, bx[1], bx[2], (int) bx[3], (int) bx[4], (int) bx[5], bx[6]);
            }
            if (material != null) {
                b.material(material);
            }
            if (glow != null) {
                b.glow(glow);
            }
            if (pattern != null) {
                b.pattern(pattern);
            }
        }
    }

    public static final class Sym extends P {
        final boolean sidedParent;
        Role roleR;

        Sym(ModelSpec.Builder b, String name, String parent, boolean sidedParent, int color) {
            super(b, name, parent, Role.STATIC, color);
            this.sidedParent = sidedParent;
            this.roleR = Role.STATIC;
        }

        public Sym roles(Role left, Role right) {
            this.role = left;
            this.roleR = right;
            return this;
        }

        @Override
        public Sym accent(int accent) {
            super.accent(accent);
            return this;
        }

        @Override
        public Sym at(float x, float y, float z) {
            super.at(x, y, z);
            return this;
        }

        @Override
        public Sym rot(float pitch, float yaw, float roll) {
            super.rot(pitch, yaw, roll);
            return this;
        }

        @Override
        public Sym mat(String material) {
            super.mat(material);
            return this;
        }

        @Override
        public Sym glow(String glow) {
            super.glow(glow);
            return this;
        }

        @Override
        public Sym pattern(String pattern) {
            super.pattern(pattern);
            return this;
        }

        @Override
        public Sym box(float x, float y, float z, int w, int h, int d) {
            super.box(x, y, z, w, h, d);
            return this;
        }

        @Override
        public Sym box(float x, float y, float z, int w, int h, int d, float dilation) {
            super.box(x, y, z, w, h, d, dilation);
            return this;
        }

        @Override
        public Sym cbox(float y0, int w, int h, int d) {
            super.cbox(y0, w, h, d);
            return this;
        }

        @Override
        public ModelSpec.Builder add() {
            String pl = parent == null ? null : sidedParent ? parent + "_l" : parent;
            String pr = parent == null ? null : sidedParent ? parent + "_r" : parent;
            emit(name + "_l", pl, role, px, py, pz, pitch, yaw, roll, false);
            emit(name + "_r", pr, roleR, -px, py, pz, pitch, -yaw, -roll, true);
            return b;
        }
    }
}
