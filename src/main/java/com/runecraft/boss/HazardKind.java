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
    SPIKE(0xF0F4FF, Blocks.WHITE_CONCRETE, ParticleTypes.SNOWFLAKE);

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
}
