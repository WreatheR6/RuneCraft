package com.runecraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.runecraft.RuneCraft;
import com.runecraft.osrs.world.Elevation;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Density function {@code runecraft:gielinor_terrain}: solid below the OSRS-faithful terrain height
 * from {@link Elevation} and air above, i.e. {@code (height(x, z) - y) * scale}, clamped to
 * +-{@link #LIMIT}. Used as the Gielinor noise router's final density (wrapped in
 * {@code minecraft:interpolated}, which is exact vertically because the density is linear in y).
 */
public record GielinorTerrainDensity(double scale) implements DensityFunction {
    public static final double LIMIT = 64.0;
    public static final MapCodec<GielinorTerrainDensity> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("scale", 0.125).forGetter(GielinorTerrainDensity::scale)
    ).apply(instance, GielinorTerrainDensity::new));
    public static final KeyDispatchDataCodec<GielinorTerrainDensity> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    /** Registers the density function type (call from mod init, before worlds load). */
    public static void register() {
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, RuneCraft.id("gielinor_terrain"), CODEC.codec());
    }

    @Override
    public double compute(DensityFunction.FunctionContext pos) {
        double d = (Elevation.height(pos.blockX(), pos.blockZ()) - pos.blockY()) * scale;
        return Math.max(-LIMIT, Math.min(LIMIT, d));
    }

    @Override
    public void fillArray(double[] densities, DensityFunction.ContextProvider applier) {
        applier.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapAll(DensityFunction.Visitor visitor) {
        return visitor.apply(this);
    }

    @Override
    public double minValue() {
        return -LIMIT;
    }

    @Override
    public double maxValue() {
        return LIMIT;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
