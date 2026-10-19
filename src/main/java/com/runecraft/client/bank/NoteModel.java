package com.runecraft.client.bank;

import com.runecraft.RuneCraft;
import com.runecraft.bank.Notes;
import com.runecraft.osrs.bank.BankNotes;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws a bank note as in OSRS: the note paper with a smaller copy of the noted item's icon on top. The
 * {@code runecraft:bank_note} item model is wrapped after baking; its override list picks, per stack, a model made
 * of the paper's quads plus the noted item's quads shrunk and moved in front of the paper.
 */
public final class NoteModel {
    /** Size of the item drawn on the note, and how far in front of the paper it sits (block units). */
    private static final float SCALE = 0.62f;
    private static final float DX = 0.03f;
    private static final float DY = 0.05f;
    private static final float DZ = 0.045f;

    private NoteModel() {
    }

    static void register() {
        ModelLoadingPlugin.register(ctx -> ctx.modifyModelAfterBake().register((model, context) -> {
            ResourceLocation id = context.id();
            if (model != null && id instanceof ModelResourceLocation mid && mid.getNamespace().equals(RuneCraft.MOD_ID)
                    && mid.getPath().equals(BankNotes.NOTE_ITEM) && mid.getVariant().equals("inventory")) {
                return new Wrapped(model);
            }
            return model;
        }));
    }

    /** Delegates to a baked model. */
    private static class Delegate implements BakedModel {
        protected final BakedModel base;

        Delegate(BakedModel base) {
            this.base = base;
        }

        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction face, RandomSource random) {
            return base.getQuads(state, face, random);
        }

        @Override
        public boolean useAmbientOcclusion() {
            return base.useAmbientOcclusion();
        }

        @Override
        public boolean isGui3d() {
            return base.isGui3d();
        }

        @Override
        public boolean usesBlockLight() {
            return base.usesBlockLight();
        }

        @Override
        public boolean isCustomRenderer() {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return base.getParticleIcon();
        }

        @Override
        public ItemTransforms getTransforms() {
            return base.getTransforms();
        }

        @Override
        public ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }
    }

    /** The bank note's own model: plain paper, with per-stack composites through its overrides. */
    private static final class Wrapped extends Delegate {
        private final Overrides overrides = new Overrides(this);
        final Map<String, BakedModel> composites = new HashMap<>();

        Wrapped(BakedModel base) {
            super(base);
        }

        @Override
        public ItemOverrides getOverrides() {
            return overrides;
        }
    }

    private static final class Overrides extends ItemOverrides {
        private final Wrapped owner;

        Overrides(Wrapped owner) {
            super(null, null, List.of());
            this.owner = owner;
        }

        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, ClientLevel world, LivingEntity entity, int seed) {
            if (!Notes.isNote(stack)) {
                return owner;
            }
            String key = Notes.notedId(stack) + "#" + Notes.notedTag(stack);
            BakedModel cached = owner.composites.get(key);
            if (cached != null) {
                return cached;
            }
            BakedModel built = owner;
            try {
                ItemStack inner = Notes.unnoted(stack, 1);
                if (!inner.isEmpty() && !Notes.isNote(inner)) {
                    BakedModel innerModel = Minecraft.getInstance().getItemRenderer().getItemModelShaper().getItemModel(inner);
                    if (innerModel != null) {
                        BakedModel resolved = innerModel.getOverrides().resolve(innerModel, inner, world, entity, seed);
                        if (resolved != null && !resolved.isCustomRenderer()) {
                            built = new Composite(owner.base, resolved);
                        }
                    }
                }
            } catch (RuntimeException e) {
                RuneCraft.LOGGER.warn("Couldn't build the bank note model for {}", key, e);
            }
            if (owner.composites.size() < 4096) {
                owner.composites.put(key, built);
            }
            return built;
        }
    }

    /** Paper quads plus the noted item's quads, shrunk and in front. */
    private static final class Composite extends Delegate {
        private final BakedModel inner;
        private final Map<Direction, List<BakedQuad>> cache = new HashMap<>();
        private List<BakedQuad> unculled;

        Composite(BakedModel paper, BakedModel inner) {
            super(paper);
            this.inner = inner;
        }

        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction face, RandomSource random) {
            if (face == null) {
                if (unculled == null) {
                    unculled = merge(base.getQuads(state, null, random), inner.getQuads(state, null, random));
                }
                return unculled;
            }
            return cache.computeIfAbsent(face, f -> merge(base.getQuads(state, f, random), inner.getQuads(state, f, random)));
        }

        private static List<BakedQuad> merge(List<BakedQuad> paper, List<BakedQuad> item) {
            List<BakedQuad> out = new ArrayList<>(paper.size() + item.size());
            out.addAll(paper);
            for (BakedQuad q : item) {
                out.add(shrink(q));
            }
            return out;
        }

        private static BakedQuad shrink(BakedQuad q) {
            int[] data = q.getVertices().clone();
            int stride = data.length / 4;
            for (int v = 0; v < 4; v++) {
                int o = v * stride;
                float x = Float.intBitsToFloat(data[o]);
                float y = Float.intBitsToFloat(data[o + 1]);
                float z = Float.intBitsToFloat(data[o + 2]);
                data[o] = Float.floatToRawIntBits(0.5f + (x - 0.5f) * SCALE + DX);
                data[o + 1] = Float.floatToRawIntBits(0.5f + (y - 0.5f) * SCALE + DY);
                data[o + 2] = Float.floatToRawIntBits(0.5f + (z - 0.5f) * SCALE + DZ);
            }
            return new BakedQuad(data, q.getTintIndex(), q.getDirection(), q.getSprite(), q.isShade());
        }
    }
}
