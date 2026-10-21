package com.runecraft.boss;

import com.runecraft.action.ActionManager;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.ThreatTiming;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.util.Msg;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * What a player sees and hears of a monster attack before it lands (round 41, // playability): style colours, per-style launch
 * sounds, dust halos and true-radius ground rings, and a warning line shown to the attack's target as a subtitle in the middle of
 * the screen (which "You eat the shark." and other action-bar lines can't overwrite).
 *
 * <p>Style colours are fixed everywhere: melee red {@value #MELEE_RGB}, ranged green {@value #RANGED_RGB}, magic blue
 * {@value #MAGIC_RGB}, typeless and area attacks orange {@value #AREA_RGB}.
 */
public final class ThreatCues {
    public static final int MELEE_RGB = 0xE03030;
    public static final int RANGED_RGB = 0x40C040;
    public static final int MAGIC_RGB = 0x4080FF;
    public static final int AREA_RGB = 0xFFA020;
    public static final int HAZARD_RGB = 0x9AFF30;
    /** Subtitle lines to one player within this many MC are merged into one (a volley's warnings don't flash past). */
    public static final int SUBTITLE_MERGE_MC = 10;

    /** Self-test hook: sees every warning line sent to a player ({@code subtitle} true when it went to the subtitle channel). */
    public interface WarningListener {
        void onWarning(ServerPlayer player, String text, boolean subtitle);
    }

    public static WarningListener warningListener = (player, text, subtitle) -> {
    };

    private record Line(long mc, String text) {
    }

    private static final Map<UUID, Line> LAST_LINE = new HashMap<>();

    private ThreatCues() {
    }

    /** The colour of an attack style (null: typeless). */
    public static int rgb(AttackType style) {
        if (style == null) {
            return AREA_RGB;
        }
        return switch (style) {
            case MAGIC -> MAGIC_RGB;
            case RANGED -> RANGED_RGB;
            default -> MELEE_RGB;
        };
    }

    public static DustParticleOptions dust(int rgb, float scale) {
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f), scale);
    }

    /**
     * An attack's warning: to its target as a coloured subtitle (merged with another line sent within
     * {@value #SUBTITLE_MERGE_MC} MC), to other players within 40 blocks on the action bar as before.
     */
    public static void announce(LivingEntity source, ServerPlayer target, String text, AttackType style) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (target != null) {
            subtitle(target, text, rgb(style));
        }
        if (source.level() instanceof ServerLevel world) {
            for (ServerPlayer p : world.getPlayers(pl -> pl != target && pl.distanceToSqr(source) < 40 * 40)) {
                Msg.action(p, text);
            }
        }
    }

    /** One warning line in the middle of a player's screen (the vanilla subtitle: no client code needed). */
    public static void subtitle(ServerPlayer player, String text, int rgb) {
        long now = ActionManager.mcTick();
        if (LAST_LINE.size() > 256) {
            LAST_LINE.values().removeIf(l -> now - l.mc() > SUBTITLE_MERGE_MC);
        }
        Line last = LAST_LINE.get(player.getUUID());
        String shown = text;
        if (last != null && now - last.mc() < SUBTITLE_MERGE_MC && !last.text().equals(text) && !last.text().endsWith(" · " + text)) {
            shown = last.text() + " · " + text;
        }
        LAST_LINE.put(player.getUUID(), new Line(now, shown));
        if (player instanceof net.neoforged.neoforge.common.util.FakePlayer || player.connection == null) {
            Msg.action(player, shown);
            warningListener.onWarning(player, text, false);
            return;
        }
        player.connection.send(new ClientboundSetTitlesAnimationPacket(2, 30, 6));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.literal(shown).withStyle(s -> s.withColor(TextColor.fromRgb(rgb)))));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.empty()));
        warningListener.onWarning(player, text, true);
    }

    /** Forgets a player's merge state (logout). */
    public static void forget(UUID player) {
        LAST_LINE.remove(player);
    }

    /** The launch sound of an attack style: a swing, a bow or crossbow, a spell being prepared, an area charge. */
    public static void launchSound(LivingEntity from, AttackType style, AttackDef.Special special) {
        ServerLevel world = (ServerLevel) from.level();
        double x = from.getX();
        double y = from.getY() + from.getBbHeight() * 0.5;
        double z = from.getZ();
        if (special == AttackDef.Special.AOE || special == AttackDef.Special.SKY_STRIKE || style == null) {
            world.playSound(null, x, y, z, SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 0.7f, 1.4f);
            return;
        }
        switch (style) {
            case MAGIC -> world.playSound(null, x, y, z, SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.HOSTILE, 0.8f, 1.2f);
            case RANGED -> {
                world.playSound(null, x, y, z, SoundEvents.SKELETON_SHOOT, SoundSource.HOSTILE, 0.8f, 0.9f);
                world.playSound(null, x, y, z, SoundEvents.CROSSBOW_LOADING_END, SoundSource.HOSTILE, 0.5f, 1.0f);
            }
            default -> world.playSound(null, x, y, z, SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 0.8f, 0.7f);
        }
    }

    /** The rising warning note of a telegraph's last moments ({@link ThreatTiming#finalNotePitch}). */
    public static void finalNote(LivingEntity from, float pitch) {
        if (pitch > 0 && from.level() instanceof ServerLevel world) {
            world.playSound(null, from.getX(), from.getY() + from.getBbHeight() * 0.5, from.getZ(), SoundEvents.NOTE_BLOCK_BELL.value(),
                    SoundSource.HOSTILE, 1.0f, pitch);
        }
    }

    /**
     * A telegraph's halo: a ring of style-coloured dust around the monster at mid height, with a few of the attack's own particles
     * (its content look) among them.
     */
    public static void halo(LivingEntity mob, int rgb, ParticleOptions extra, boolean bright) {
        ServerLevel world = (ServerLevel) mob.level();
        double r = mob.getBbWidth() / 2 + 0.4;
        double y = mob.getY() + mob.getBbHeight() * 0.55;
        DustParticleOptions dust = dust(rgb, bright ? 2.0f : 1.4f);
        int points = 14;
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points;
            world.sendParticles(dust, mob.getX() + Math.cos(a) * r, y, mob.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
        world.sendParticles(dust, mob.getX(), mob.getY() + mob.getBbHeight() + 0.3, mob.getZ(), 3, 0.2, 0.1, 0.2, 0);
        if (extra != null) {
            world.sendParticles(extra, mob.getX(), y, mob.getZ(), 3, r * 0.6, mob.getBbHeight() * 0.3, r * 0.6, 0.01);
        }
    }

    /**
     * A ground ring at its true radius: a dust point every ~0.4 blocks of its edge (12 to 48) just above the floor, plus a sparse
     * fill of the content's own particle.
     */
    public static void ring(ServerLevel world, Vec3 centre, double radius, int rgb, ParticleOptions fill, boolean bright) {
        ring(world, centre, radius, rgb, fill, bright, (int) Math.max(12, Math.min(48, Math.round(Math.PI * 2 * radius * 8 / 3))));
    }

    /**
     * A ground hazard's ring (radius 1): 8 lime points and a sparse fill, light enough for arenas that keep dozens of hazards for
     * minutes (Sol Heredit's molten sand).
     */
    public static void hazardRing(ServerLevel world, Vec3 centre, int rgb, ParticleOptions fill, boolean bright) {
        ring(world, centre, ThreatTiming.HAZARD_RADIUS, rgb, fill, bright, 8);
    }

    private static void ring(ServerLevel world, Vec3 centre, double radius, int rgb, ParticleOptions fill, boolean bright, int points) {
        DustParticleOptions dust = dust(rgb, bright ? 1.8f : 1.3f);
        double y = centre.y + 0.15;
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points;
            world.sendParticles(dust, centre.x + Math.cos(a) * radius, y, centre.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
        if (fill != null) {
            world.sendParticles(fill, centre.x, y, centre.z, (int) Math.max(2, Math.min(10, radius * 2)), radius * 0.4, 0.02,
                    radius * 0.4, 0);
        }
    }

    /** A projectile's head: two or three style-coloured dust motes at {@code at}, and the attack's own particle when it has one. */
    public static void head(ServerLevel world, Vec3 at, int rgb, ParticleOptions extra) {
        world.sendParticles(dust(rgb, 1.8f), at.x, at.y, at.z, 3, 0.06, 0.06, 0.06, 0);
        if (extra != null) {
            world.sendParticles(extra, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
    }

    /** A projectile reaching its target: a burst and a style sound. */
    public static void impact(ServerLevel world, Vec3 at, AttackType style, ParticleOptions extra) {
        world.sendParticles(dust(rgb(style), 1.6f), at.x, at.y, at.z, 10, 0.25, 0.25, 0.25, 0);
        if (extra != null) {
            world.sendParticles(extra, at.x, at.y, at.z, 4, 0.2, 0.2, 0.2, 0.02);
        }
        if (style == AttackType.MAGIC) {
            world.playSound(null, at.x, at.y, at.z, SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 0.6f, 1.4f);
        } else {
            world.playSound(null, at.x, at.y, at.z, SoundEvents.ARROW_HIT, SoundSource.HOSTILE, 0.7f, 1.0f);
        }
    }

    /** A projectile that hit a wall or lost its target: a puff and a hiss where it stopped. */
    public static void fizzle(ServerLevel world, Vec3 at) {
        world.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, at.x, at.y, at.z, 6, 0.15, 0.15, 0.15, 0.01);
        world.playSound(null, BlockPos.containing(at), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.3f, 1.6f);
    }

    /** A melee swing that found nobody in reach: the vanilla whoosh. */
    public static void whoosh(LivingEntity from) {
        from.level().playSound(null, from.getX(), from.getY() + 1, from.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.HOSTILE,
                0.8f, 0.8f);
    }
}
