package com.runecraft.boss;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * What a ground hazard or boss floor mark is (round 42, // playability): the colour of its ring and the temporary floor block
 * that marks its tiles ({@link FloorMarkers}), so a real-time player sees where not to stand (or, for a glyph, where to stand)
 * at a glance instead of reading a few particles. OSRS shows these as a texture on the tiles. Append new kinds last.
 */
public enum HazardKind {
    /** Acid (Vorkath, Araxxor): lime. */
    ACID(0x9CFF2E, Blocks.LIME_CONCRETE, ParticleTypes.SNEEZE),
    /** Fire and lava: orange. */
    FIRE(0xFF8A1F, Blocks.ORANGE_CONCRETE, ParticleTypes.FLAME),
    /** Poison and venom pools (Hydra): green. */
    POISON(0x2E9E3A, Blocks.GREEN_CONCRETE, ParticleTypes.SNEEZE),
    /** Gas clouds (Duke Sucellus): grey. */
    GAS(0xB8B8C8, Blocks.LIGHT_GRAY_CONCRETE, ParticleTypes.CLOUD),
    /** A spot to stand on (Eyatlalli's glyph): yellow. */
    GLYPH(0xFFD24A, Blocks.YELLOW_CONCRETE, ParticleTypes.END_ROD),
    /** A spike about to erupt (the Phantom Muspah): light blue. */
    ICE(0x9AD8FF, Blocks.LIGHT_BLUE_CONCRETE, ParticleTypes.SNOWFLAKE),
    /** A standing spike (the Phantom Muspah): white. */
    SPIKE(0xF0F4FF, Blocks.WHITE_CONCRETE, ParticleTypes.SNOWFLAKE),
    /** Something about to land here (round 43: a boss's strike, falling rocks, a javelin, a shockwave): red. */
    STRIKE(0xE03020, Blocks.RED_CONCRETE, ParticleTypes.CRIT),
    /** A safe spot among danger (round 43: Sol Heredit's safe tiles, the glyph's shadow, the earthen shield): cyan. */
    SAFE(0x30D0D0, Blocks.CYAN_CONCRETE, ParticleTypes.END_ROD),
    /** Shadow and darkness about to strike (round 43: the Whisperer's tentacles, the Nightmare's claws and surge): purple. */
    SHADOW(0x8A3FC0, Blocks.PURPLE_CONCRETE, ParticleTypes.SQUID_INK),
    /** Blood pools (round 43: the Blood Moon, the corrupted Hunllef's crimson tiles): dark red. */
    BLOOD(0x8A1020, Blocks.NETHER_WART_BLOCK, ParticleTypes.DAMAGE_INDICATOR);

    /** The ring's colour. */
    public final int rgb;
    /** The block that temporarily replaces the floor under it. */
    public final Block block;
    /** The sparse fill drawn inside its ring. */
    public final ParticleOptions fill;

    HazardKind(int rgb, Block block, ParticleOptions fill) {
        this.rgb = rgb;
        this.block = block;
        this.fill = fill;
    }

    /**
     * The kind a strike drawn with {@code particle} marks its tiles with (round 43): fire and lava orange, snow light blue, acid
     * lime, ink purple; anything else is a plain strike (red).
     */
    public static HazardKind forParticle(ParticleOptions particle) {
        if (particle == null) {
            return STRIKE;
        }
        net.minecraft.core.particles.ParticleType<?> t = particle.getType();
        if (t == ParticleTypes.FLAME || t == ParticleTypes.SMALL_FLAME || t == ParticleTypes.LAVA || t == ParticleTypes.SOUL_FIRE_FLAME
                || t == ParticleTypes.FALLING_LAVA || t == ParticleTypes.DRIPPING_LAVA) {
            return FIRE;
        }
        if (t == ParticleTypes.SNOWFLAKE || t == ParticleTypes.ITEM_SNOWBALL) {
            return ICE;
        }
        if (t == ParticleTypes.SNEEZE) {
            return ACID;
        }
        if (t == ParticleTypes.ITEM_SLIME) {
            return POISON;
        }
        if (t == ParticleTypes.SQUID_INK || t == ParticleTypes.SCULK_SOUL || t == ParticleTypes.PORTAL) {
            return SHADOW;
        }
        return STRIKE;
    }

    /** {@link #forParticle(ParticleOptions)} for a particle id ("minecraft:flame"), as content data names it. */
    public static HazardKind forParticleId(String id) {
        if (id == null) {
            return null;
        }
        var type = net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.get(net.minecraft.resources.ResourceLocation.tryParse(id));
        return type instanceof net.minecraft.core.particles.SimpleParticleType simple ? forParticle(simple) : null;
    }
}
