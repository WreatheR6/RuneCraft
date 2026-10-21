package com.runecraft.content.minigames.games;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.BossScript;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.Mechanics;
import com.runecraft.boss.ThreatCues;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.ThreatTiming;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.minigames.TzHaarWaves;
import com.runecraft.util.Msg;
import java.util.List;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * TzKal-Zuk: his typeless fire blast (max 148) is blocked only when the player stands behind the Ancestral Glyph, which the
 * Inferno session slides back and forth in front of him; nothing else stops it. He never moves, attacks every 10 ticks and
 * every 7 once enraged at 240 hitpoints. His 1,200 hitpoints are above Minecraft's 1,024 health cap, so incoming damage is
 * scaled to his health ({@link TzHaarWaves#zukHitpoints} reads them back).
 *
 * <p>Real time (round 42, // playability): each blast charges for {@link TzHaarWaves#ZUK_CHARGE_MC} Minecraft ticks (an orange
 * glow, the typeless colour, a rising note and a warning in the middle of the screen) and lands as the charge ends, the glyph
 * checked then; the glyph's shadow (the exact area {@link TzHaarWaves#glyphShields} covers) is drawn on the floor in green
 * every few Minecraft ticks, brighter while a blast charges, and a player outside it during a charge stands in an orange ring.
 * OSRS shows his attack animation and the projectile, and the glyph's model lags its true position. The cadence (stamped
 * when the charge starts) and the blast's roll are unchanged.
 */
public class ZukScript implements BossScript, DataBossEntity.HpScaled {
    /** The glyph's shadow on the floor: the safe green of Sol Heredit's safe tiles. */
    private static final int SHADOW_RGB = 0x70FF70;
    /** How far behind the glyph its shadow is drawn (blocks); the cover itself reaches the arena's far wall. */
    private static final double SHADOW_LENGTH = 9.0;

    /** Self-test hook: sees every blast charge as it starts and as it lands. */
    public interface ChargeListener {
        default void charged(DataBossEntity zuk, ServerPlayer target, long startMc, long dueMc) {
        }

        default void landed(DataBossEntity zuk, ServerPlayer target, long mc, boolean shielded) {
        }
    }

    public static ChargeListener chargeListener = new ChargeListener() {
    };

    private boolean enraged;
    /** The blast being charged: when it lands (Minecraft tick, -1 for none), at whom, and which attack. */
    private long chargeDueMc = -1;
    private long chargeStartMc;
    private ServerPlayer chargeTarget;
    private AttackDef chargeAttack;
    private long lastShadowMc = -1_000_000L;
    private LivingEntity glyphCache;
    /** The glyph's shadow as cyan floor blocks (round 43): the columns {@code glyphShields} covers all over, near the glyph. */
    private com.runecraft.boss.FloorMarkers.Layer shadowFloor;
    private long glyphLookedMc = -1_000_000L;

    static LivingEntity glyph(DataBossEntity boss) {
        List<LivingEntity> list = ((ServerLevel) boss.level()).getEntitiesOfClass(LivingEntity.class,
                boss.getBoundingBox().inflate(30), e -> e.isAlive() && MinigameManager.creatureId(e).equals("ancestral_glyph"));
        return list.isEmpty() ? null : list.get(0);
    }

    /** Whether the glyph stands between Zuk and the player (lined up east-west, and the player on its far side). */
    static boolean shielded(DataBossEntity boss, LivingEntity glyph, LivingEntity player) {
        if (glyph == null) {
            return false;
        }
        return TzHaarWaves.glyphShields(player.getX(), player.getZ(), glyph.getX(), glyph.getZ(), glyph.getBbWidth(), boss.getZ());
    }

    @Override
    public void gameTick(DataBossEntity boss, long gameTick) {
        if (!enraged && TzHaarWaves.zukHitpoints(boss.getHealth(), boss.getMaxHealth()) <= TzHaarWaves.ZUK_HEALER_HP) {
            enraged = true;
            boss.overrideStats(s -> TzHaarSession.withSpeed(s, TzHaarWaves.ZUK_ENRAGED_SPEED));
        }
    }

    /** Whether Zuk attacks at his enraged speed. */
    public boolean enraged() {
        return enraged;
    }

    @Override
    public int modifyIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
        if (type != null && type.isMelee()) {
            // He floats in the lava beyond the platform: only ranged and magic reach him (OSRS Wiki "TzKal-Zuk").
            Msg.action(attacker, "TzKal-Zuk is out of reach of melee attacks.");
            return 0;
        }
        if (damage <= 0 || boss.getMaxHealth() >= TzHaarWaves.ZUK_HP) {
            return damage;
        }
        double scaled = damage * (double) boss.getMaxHealth() / TzHaarWaves.ZUK_HP;
        int whole = (int) scaled;
        return whole + (boss.getRandom().nextDouble() < scaled - whole ? 1 : 0);
    }

    @Override
    public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
        if (shielded(boss, glyph(boss), player)) {
            Msg.action(player, "The Ancestral Glyph absorbs TzKal-Zuk's blast.");
            return 0;
        }
        return damage;
    }

    @Override
    public void onSpawn(DataBossEntity boss) {
        boss.setNoAi(false);
        boss.attackInterceptor = this::charge;
    }

    /**
     * His attack goal's turn (the cadence is stamped now): the blast starts charging and lands {@link TzHaarWaves#ZUK_CHARGE_MC}
     * Minecraft ticks later ({@link #tick}).
     */
    private boolean charge(com.runecraft.entity.OsrsMonsterEntity mob, ServerPlayer target, AttackDef planned) {
        if (planned == null || !(mob instanceof DataBossEntity boss)) {
            return false;
        }
        if (chargeDueMc >= 0) {
            return true; // still charging the last one (never at his cadence; a charge is shorter than his 7 ticks)
        }
        startCharge(boss, target, planned);
        return true;
    }

    private void startCharge(DataBossEntity boss, ServerPlayer target, AttackDef attack) {
        long now = ActionManager.mcTick();
        chargeStartMc = now;
        chargeDueMc = now + TzHaarWaves.ZUK_CHARGE_MC;
        chargeTarget = target;
        chargeAttack = attack;
        // it lands at once when performed at the charge's end: the charge was its flight
        Mechanics.selfDelivered(attack);
        ThreatCues.launchSound(boss, null, AttackDef.Special.NONE);
        ThreatCues.halo(boss, ThreatCues.AREA_RGB, ParticleTypes.FLAME, false);
        boolean covered = shielded(boss, glyph(boss), target);
        Msg.telegraph(target, Component.literal(covered ? "TzKal-Zuk charges a blast!" : "TzKal-Zuk charges a blast! Get behind the glyph!")
                .withStyle(s -> s.withColor(Msg.TYPELESS_RGB)));
        drawShadow(boss, true);
        chargeListener.charged(boss, target, chargeStartMc, chargeDueMc);
    }

    @Override
    public void tick(DataBossEntity boss) {
        if (!boss.isAlive()) {
            chargeDueMc = -1;
            return;
        }
        if (boss.attackInterceptor == null) {
            boss.attackInterceptor = this::charge; // a Zuk loaded from disk got a fresh script
        }
        long now = ActionManager.mcTick();
        if (chargeDueMc >= 0) {
            if (now >= chargeDueMc) {
                land(boss, now);
            } else {
                if (ThreatTiming.redraw(chargeStartMc, now)) {
                    ThreatCues.halo(boss, ThreatCues.AREA_RGB, ParticleTypes.FLAME, chargeDueMc - now <= ThreatTiming.FINAL_NOTE_MC);
                }
                ThreatCues.finalNote(boss, ThreatTiming.finalNotePitch(now, chargeDueMc));
            }
        }
        if (now - lastShadowMc >= ThreatTiming.REDRAW_MC) {
            drawShadow(boss, chargeDueMc >= 0);
        }
    }

    /** The charge ends: the blast lands on its target through the attack path (roll, glyph, damage), if still there. */
    private void land(DataBossEntity boss, long now) {
        ServerPlayer t = chargeTarget;
        AttackDef a = chargeAttack;
        chargeDueMc = -1;
        chargeTarget = null;
        chargeAttack = null;
        if (t == null || a == null || !t.isAlive() || t.isRemoved() || t.level() != boss.level()) {
            return;
        }
        boolean covered = shielded(boss, glyph(boss), t);
        Mechanics.perform(boss, t, a, 1.0);
        chargeListener.landed(boss, t, now, covered);
    }

    /**
     * The glyph's shadow on the floor: two green lines along the edges of the area its cover counts in, running away from Zuk,
     * and a sparse fill; brighter while a blast charges. A charging blast's target outside it stands in an orange ring.
     */
    private void drawShadow(DataBossEntity boss, boolean charging) {
        long now = ActionManager.mcTick();
        lastShadowMc = now;
        if (glyphCache == null || !glyphCache.isAlive() || now - glyphLookedMc >= 40) {
            glyphCache = glyph(boss);
            glyphLookedMc = now;
        }
        LivingEntity glyph = glyphCache;
        if (shadowFloor == null) {
            shadowFloor = new com.runecraft.boss.FloorMarkers.Layer(boss);
        }
        if (glyph == null || !glyph.isAlive()) {
            shadowFloor.clear();
            return;
        }
        ServerLevel world = (ServerLevel) boss.level();
        double half = TzHaarWaves.glyphShadowHalfWidth(glyph.getBbWidth());
        double dir = Math.signum(glyph.getZ() - boss.getZ());
        if (dir == 0) {
            dir = 1;
        }
        showShadow(world, boss, glyph);
        double y = glyph.getY() + 0.12;
        boolean bright = charging && chargeDueMc - now <= ThreatTiming.FINAL_NOTE_MC;
        DustParticleOptions edge = ThreatCues.dust(SHADOW_RGB, charging ? (bright ? 1.8f : 1.4f) : 1.0f);
        for (double d = 0.6; d <= SHADOW_LENGTH; d += 0.75) {
            double z = glyph.getZ() + dir * d;
            world.sendParticles(edge, glyph.getX() - half, y, z, 1, 0, 0, 0, 0);
            world.sendParticles(edge, glyph.getX() + half, y, z, 1, 0, 0, 0, 0);
        }
        if (charging) {
            world.sendParticles(ThreatCues.dust(SHADOW_RGB, 0.9f), glyph.getX(), y, glyph.getZ() + dir * SHADOW_LENGTH / 2, 6,
                    half * 0.5, 0.0, SHADOW_LENGTH * 0.3, 0);
            ServerPlayer t = chargeTarget;
            if (t != null && t.isAlive() && !shielded(boss, glyph, t)) {
                ThreatCues.ring(world, t.position(), 0.8, ThreatCues.AREA_RGB, null, bright);
            }
        }
    }

    /**
     * The glyph's shadow as cyan floor blocks (round 43), following it as it slides: every block column the cover counts all
     * over ({@link TzHaarWaves#glyphShadowColumns}: {@link TzHaarWaves#glyphShields} at all four corners, lined up within the
     * half width and further from Zuk than the glyph), out to the drawn length, so a player anywhere on a cyan block is
     * covered. Put back as it moves on, when the glyph shatters, when Zuk dies or the run ends, and when the server stops.
     * // playability: OSRS players learn where its model lags its true position
     */
    private void showShadow(ServerLevel world, DataBossEntity boss, LivingEntity glyph) {
        java.util.List<int[]> cols = TzHaarWaves.glyphShadowColumns(glyph.getX(), glyph.getZ(), glyph.getBbWidth(), boss.getZ(),
                SHADOW_LENGTH);
        shadowFloor.show(new com.runecraft.boss.FloorMarkers.Shape(world).columns(cols, glyph.getY(), com.runecraft.boss.HazardKind.SAFE));
    }

    /** Self-test: floor blocks the glyph's shadow marks now. */
    public int shadowMarked() {
        return shadowFloor == null ? 0 : shadowFloor.size();
    }

    /** Self-test: whether a blast is charging, and when it lands (Minecraft tick, -1 for none). */
    public long chargeDueMc() {
        return chargeDueMc;
    }
}
