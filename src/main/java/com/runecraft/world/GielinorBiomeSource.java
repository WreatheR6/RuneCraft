package com.runecraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Region;
import com.runecraft.osrs.world.Elevation;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/**
 * Places biomes by real Old School RuneScape geography (see {@link GielinorMap}) instead of
 * climate noise - Lumbridge at the origin, the Wilderness to the north, the desert to the east.
 * The data pack maps each region name to a biome. Two optional terrain biomes follow the elevation
 * model instead: {@code sea} for the open sea and {@code mountain_peaks} for snow-capped summits.
 */
public class GielinorBiomeSource extends BiomeSource {
    public static final MapCodec<GielinorBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Biome.CODEC).fieldOf("biomes").forGetter(s -> s.byName)
    ).apply(instance, instance.stable(GielinorBiomeSource::new)));

    private final Map<String, Holder<Biome>> byName;
    private final Map<Region, Holder<Biome>> byRegion = new EnumMap<>(Region.class);
    private final Holder<Biome> fallback;
    private final Holder<Biome> sea;
    private final Holder<Biome> peaks;

    public GielinorBiomeSource(Map<String, Holder<Biome>> byName) {
        this.byName = byName;
        Holder<Biome> first = null;
        for (Region r : Region.values()) {
            Holder<Biome> b = byName.get(r.id());
            if (b != null) {
                byRegion.put(r, b);
                if (first == null) {
                    first = b;
                }
            }
        }
        if (first == null) {
            first = byName.values().iterator().next();
        }
        this.fallback = byName.getOrDefault(Region.UNCHARTED_ISLES.id(), first);
        this.sea = byName.get("sea");
        this.peaks = byName.get("mountain_peaks");
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return byName.values().stream().distinct();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler noise) {
        int bx = QuartPos.toBlock(x);
        int bz = QuartPos.toBlock(z);
        if (sea != null && Elevation.isOcean(bx, bz)) {
            return sea;
        }
        if (peaks != null && Elevation.isSnowyPeak(bx, bz)) {
            return peaks;
        }
        Region region = GielinorMap.regionAtBlock(bx, bz);
        return byRegion.getOrDefault(region, fallback);
    }
}
