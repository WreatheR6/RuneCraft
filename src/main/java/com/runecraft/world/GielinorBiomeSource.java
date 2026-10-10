package com.runecraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Region;
import com.runecraft.osrs.world.Elevation;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;

import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Places biomes by real Old School RuneScape geography (see {@link GielinorMap}) instead of
 * climate noise - Lumbridge at the origin, the Wilderness to the north, the desert to the east.
 * The data pack maps each region name to a biome. Two optional terrain biomes follow the elevation
 * model instead: {@code sea} for the open sea and {@code mountain_peaks} for snow-capped summits.
 */
public class GielinorBiomeSource extends BiomeSource {
    public static final Codec<GielinorBiomeSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Biome.REGISTRY_CODEC).fieldOf("biomes").forGetter(s -> s.byName)
    ).apply(instance, instance.stable(GielinorBiomeSource::new)));

    private final Map<String, RegistryEntry<Biome>> byName;
    private final Map<Region, RegistryEntry<Biome>> byRegion = new EnumMap<>(Region.class);
    private final RegistryEntry<Biome> fallback;
    private final RegistryEntry<Biome> sea;
    private final RegistryEntry<Biome> peaks;

    public GielinorBiomeSource(Map<String, RegistryEntry<Biome>> byName) {
        this.byName = byName;
        RegistryEntry<Biome> first = null;
        for (Region r : Region.values()) {
            RegistryEntry<Biome> b = byName.get(r.id());
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
    protected Codec<? extends BiomeSource> getCodec() {
        return CODEC;
    }

    @Override
    protected Stream<RegistryEntry<Biome>> biomeStream() {
        return byName.values().stream().distinct();
    }

    @Override
    public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseUtil.MultiNoiseSampler noise) {
        int bx = BiomeCoords.toBlock(x);
        int bz = BiomeCoords.toBlock(z);
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
