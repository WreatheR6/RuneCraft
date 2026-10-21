package com.runecraft.boss;

import com.runecraft.osrs.ArenaTiming;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * How a pending ground strike looks and sounds, the same in every arena (round 41 playability plan, {@link ArenaTiming}):
 * scripted bosses, raids and the God Wars/Wilderness bosses' own strike lists all draw it. A high-contrast dust outline at the
 * strike's true radius (the area the horizontal landing test hits), typeless/AoE orange, redder and larger in its last
 * {@link ArenaTiming#FINAL_CUE_MC} Minecraft ticks, with the content's particle as a sparse fill; a bell rising in pitch as
 * the nearest one lands. // playability: OSRS marks the tiles once and the hit is tile-exact
 */
public final class StrikeMarkers {
    /** Typeless/AoE orange 0xFFA020 (the round 41 style colours). */
    public static final DustParticleOptions OUTLINE = new DustParticleOptions(new Vector3f(1.0f, 0.63f, 0.13f), 1.0f);
    /** The brighter red-orange of a strike about to land. */
    public static final DustParticleOptions OUTLINE_LANDING = new DustParticleOptions(new Vector3f(1.0f, 0.22f, 0.1f), 1.6f);

    private StrikeMarkers() {
    }

    /** Draws a strike's outline with {@code points} points ({@link ArenaTiming#ringPoints}) and its fill (may be null). */
    public static void ring(ServerLevel world, Vec3 centre, double radius, ParticleOptions fill, boolean landing, int points) {
        DustParticleOptions dust = landing ? OUTLINE_LANDING : OUTLINE;
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points;
            world.sendParticles(dust, centre.x + Math.cos(a) * radius, centre.y + 0.1, centre.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
        if (fill != null) {
            int n = (int) Math.max(2, Math.min(8, radius * 3));
            world.sendParticles(fill, centre.x, centre.y + 0.15, centre.z, n, radius * 0.45, 0.02, radius * 0.45, 0.0);
        }
    }

    /** The final-cue bell of the strike landing soonest, {@code leftMc} Minecraft ticks before it lands (higher as it nears). */
    public static void bell(ServerLevel world, Vec3 pos, long leftMc) {
        long left = Math.max(0, Math.min(ArenaTiming.FINAL_CUE_MC, leftMc));
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.HOSTILE, 0.5f,
                1.4f + 0.06f * (ArenaTiming.FINAL_CUE_MC - left));
    }
}
