package com.runecraft.content.minigames.games;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.ThreatCues;
import com.runecraft.entity.BossEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.ThreatTiming;
import com.runecraft.osrs.content.minigames.TzHaarWaves;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;

/**
 * TzTok-Jad (the built-in boss of the Fight Cave): his bite is a stab, used only next to his target, and his magic fireball
 * hits up to 95 where his melee and ranged attacks reach 97 (OSRS Wiki "TzTok-Jad"). The telegraphs (rearing up for magic,
 * stomping for ranged, the hit landing 3 ticks later) are {@code BossEntity}'s Jad mechanic.
 *
 * <p>Real time (round 42, // playability): for those 3 ticks (36 Minecraft ticks) Jad glows in the style coming, blue for magic
 * and green for ranged, redrawn every few Minecraft ticks, with a rising note in the last half second, on top of the rear-up or
 * stomp, its sound and the coloured warning in the middle of the screen. OSRS shows the animation only, read on the tick it
 * starts. The 3-tick lead, his attack speed and his max hits are unchanged.
 */
public final class TzTokJadScript implements BossEntity.Script {
    /** A telegraph being shown: Jad, the style, and the Minecraft ticks it started and lands on. */
    record Cue(BossEntity jad, AttackType style, long startMc, long dueMc) {
    }

    private static final List<Cue> CUES = new ArrayList<>();
    /** Self-test hook: the cues drawn (Jad's style glow) as {@code (style, startMc, dueMc, drawnMc)}. */
    public interface CueListener {
        void drawn(BossEntity jad, AttackType style, long startMc, long dueMc, long drawnMc);
    }

    public static CueListener cueListener = (jad, style, startMc, dueMc, drawnMc) -> {
    };

    @Override
    public AttackType chooseStyle(BossEntity boss, ServerPlayer target, double distanceSq, AttackType chosen) {
        return chosen.isMelee() ? AttackType.STAB : chosen;
    }

    @Override
    public int maxHit(BossEntity boss, ServerPlayer player, AttackType type, int max) {
        return type == AttackType.MAGIC ? TzHaarWaves.JAD_MAGIC_MAX : max;
    }

    /** After a magic or ranged attack started (its hit lands 3 ticks on): the style glow begins. */
    @Override
    public void afterAttack(BossEntity boss, ServerPlayer target, AttackType type) {
        if (type == null || type.isMelee()) {
            return;
        }
        long now = ActionManager.mcTick();
        Cue cue = new Cue(boss, type, now, now + (long) TzHaarWaves.JAD_TELEGRAPH_TICKS * TzHaarWaves.MC_PER_TICK);
        CUES.removeIf(c -> c.jad() == boss);
        CUES.add(cue);
        draw(cue, now);
    }

    /** Every Minecraft tick (from the Inferno's tick hook, which ticks every TzHaar cue): redraws and expires the glows. */
    static void tickCues() {
        if (CUES.isEmpty()) {
            return;
        }
        long now = ActionManager.mcTick();
        for (Cue c : CUES.toArray(new Cue[0])) {
            if (!c.jad().isAlive() || c.jad().isRemoved() || now >= c.dueMc()) {
                CUES.remove(c);
                continue;
            }
            if (ThreatTiming.redraw(c.startMc(), now)) {
                draw(c, now);
            }
            ThreatCues.finalNote(c.jad(), ThreatTiming.finalNotePitch(now, c.dueMc()));
        }
    }

    private static void draw(Cue c, long now) {
        boolean bright = c.dueMc() - now <= ThreatTiming.FINAL_NOTE_MC;
        ThreatCues.halo(c.jad(), ThreatCues.rgb(c.style()), c.style() == AttackType.MAGIC ? ParticleTypes.FLAME : ParticleTypes.LAVA,
                bright);
        cueListener.drawn(c.jad(), c.style(), c.startMc(), c.dueMc(), now);
    }

    /** Self-test: whether Jad is glowing for an attack on its way, and its style (null for none). */
    public static AttackType cueStyle(BossEntity jad) {
        for (Cue c : CUES) {
            if (c.jad() == jad) {
                return c.style();
            }
        }
        return null;
    }
}
