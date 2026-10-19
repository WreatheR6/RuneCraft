package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.Mechanics;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.content.minigames.SolHeredit;
import com.runecraft.osrs.content.minigames.SolHeredit.Attack;
import com.runecraft.osrs.content.minigames.SolHeredit.Grapple;
import com.runecraft.player.Players;
import com.runecraft.util.Msg;
import com.runecraft.platform.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Sol Heredit ({@link SolHeredit}): spear and shield AoEs dodged by position (the dust covers the pattern's tiles, prayer
 * doesn't help), the triple parry (Protect from Melee switched on just before each hit), the grapple (a gear slot to defend
 * within 4 ticks: chat links run {@code /solheredit parry <slot>}; the last tick is a perfect parry and the next attack within
 * 5 ticks is a max hit), phase transitions with molten sand and light-beam crystals, and the enrage at 10%.
 */
public final class SolHereditScript extends ScriptedBoss {
    /** Players with a grapple to defend, and the script that called it. */
    private static final Map<UUID, SolHereditScript> GRAPPLED = new HashMap<>();
    /** Perfect parries: player to the last game tick their next attack is a max hit. */
    private static final Map<UUID, Long> PERFECT = new HashMap<>();

    private final Random random = new Random();
    private int phase = 1;
    private Attack previous;
    private boolean afterPhase;
    private int wait = 3;
    private int rooted;
    // Pending regular attack.
    private Attack pending;
    private long pendingLands;
    private BlockPos pendingCentre;
    private int[] pendingFacing;
    // Triple parry: the tick of the start, hits landed so far.
    private long tripleStart = -1;
    private long tripleNext;
    private int tripleHit;
    // Grapple.
    private Grapple grapple;
    private long grappleStart;
    private UUID grappled;
    private boolean defended;
    /** The grapple was answered (right or wrong slot): later answers are refused, so a wrong slot can't be taken back. */
    private boolean answered;
    // Protect from Melee tracking (per target).
    private long protectSince = -1;
    // Light beams.
    private final List<long[]> lasers = new ArrayList<>();
    private long nextLaser;
    private int beamIndex;
    private long nextSand;
    private boolean opened;

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> dispatcher.register(
                Commands.literal("solheredit").then(Commands.literal("parry")
                        .then(Commands.argument("slot", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayer();
                            if (p != null) {
                                parry(p, com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "slot"));
                            }
                            return 1;
                        })))));
        CombatHooks.addModifier(new CombatHooks.PlayerAttackModifier() {
            @Override
            public boolean alwaysMaxHit(ServerPlayer player, LivingEntity target, AttackType type) {
                Long until = PERFECT.get(player.getUUID());
                return until != null && com.runecraft.action.ActionManager.gameTick() <= until && isSol(target);
            }

            @Override
            public void afterHit(ServerPlayer player, LivingEntity target, AttackType type, int dealt) {
                if (isSol(target)) {
                    PERFECT.remove(player.getUUID());
                }
            }
        });
    }

    private static boolean isSol(LivingEntity e) {
        return e instanceof DataBossEntity b && b.def() != null && b.def().id.equals("sol_heredit");
    }

    /** {@code /solheredit parry <slot>}: defends the called gear slot; returns whether it blocked the grapple. */
    public static boolean parry(ServerPlayer p, String slot) {
        SolHereditScript s = GRAPPLED.get(p.getUUID());
        if (s == null || s.grapple == null) {
            Msg.game(p, "There is nothing to defend against.");
            return false;
        }
        if (s.answered) {
            Msg.game(p, "You have already answered Sol Heredit's grapple.");
            return s.defended;
        }
        s.answered = true;
        Grapple g = Grapple.bySlot(slot);
        if (g != s.grapple) {
            Msg.red(p, "You defend the wrong slot!");
            s.grappleTicksLeftWrong();
            return false;
        }
        int tick = (int) (s.now - s.grappleStart);
        s.defended = true;
        if (SolHeredit.perfectParry(tick)) {
            PERFECT.put(p.getUUID(), s.now + SolHeredit.PERFECT_PARRY_TICKS);
            Msg.game(p, "You perfectly parry Sol Heredit's grapple!");
        } else {
            Msg.game(p, "You successfully defend from Sol Heredit's grapple!");
        }
        return true;
    }

    /** A wrong slot leaves the grapple undefended: it lands on the next game tick (and {@link #answered} refuses a retry). */
    private void grappleTicksLeftWrong() {
        defended = false;
        grappleStart = now - SolHeredit.GRAPPLE_WINDOW_TICKS;
    }

    /** The current phase (1-6), the last attack and whether a grapple is waiting (self-test). */
    public int phase() {
        return phase;
    }

    public Attack previous() {
        return previous;
    }

    @Override
    protected boolean driven() {
        return true;
    }

    @Override
    protected boolean wantsMelee(DataBossEntity boss, ServerPlayer target) {
        return true;
    }

    @Override
    protected boolean canAttack(DataBossEntity boss) {
        return false; // every attack is scripted below
    }

    @Override
    public void onDeath(DataBossEntity boss, ServerPlayer killer) {
        cleanUp(boss);
    }

    private void cleanUp(DataBossEntity boss) {
        GRAPPLED.values().removeIf(s -> s == this);
        Mechanics.clearHazards(BossFx.world(boss), boss.getBoundingBox().inflate(24, 6, 24));
    }

    @Override
    protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
        if (!boss.isAlive()) {
            cleanUp(boss);
            return;
        }
        if (!opened && target != null) {
            opened = true;
            BossFx.say(boss, "Sol Heredit: " + SolHeredit.OPENING_LINE);
        }
        if (rooted > 0) {
            rooted--;
            boss.getNavigation().stop();
        }
        if (target != null) {
            boolean on = Players.data(target).isActive(Prayer.PROTECT_FROM_MELEE);
            protectSince = on ? (protectSince < 0 ? now : protectSince) : -1;
        }
        int nowPhase = SolHeredit.phase(percent(boss));
        if (nowPhase > phase) {
            for (int p = phase + 1; p <= nowPhase; p++) {
                enterPhase(boss, target, p);
            }
            phase = nowPhase;
        }
        tickPending(boss);
        tickTriple(boss, target);
        tickGrapple(boss);
        tickLasers(boss, target);
        if (phase >= 6 && target != null && now >= nextSand) {
            nextSand = now + SolHeredit.ENRAGE_SAND_TICKS;
            sand(boss, target, 1, false);
        }
        if (wait > 0) {
            wait--;
            return;
        }
        if (target == null || pending != null || tripleStart >= 0 || grapple != null) {
            return;
        }
        Attack a = SolHeredit.next(previous, phase, afterPhase, random);
        BlockPos c = boss.blockPosition();
        BlockPos t = target.blockPosition();
        if (!a.special() && Math.max(Math.abs(t.getX() - c.getX()), Math.abs(t.getZ() - c.getZ())) > SolHeredit.MELEE_REACH) {
            return; // he must be next to the player to start a regular attack
        }
        afterPhase = false;
        previous = a;
        // wait counts down on the ticks in between (the ScriptedBoss convention), so the next attack starts attackDelay ticks on.
        wait = Math.max(1, SolHeredit.attackDelay(a, phase)) - 1;
        switch (a) {
            case TRIPLE -> {
                tripleStart = now;
                tripleHit = 0;
                tripleNext = now + SolHeredit.tripleGap(0, phase);
                BossFx.warn(boss, "Sol Heredit grips his spear tightly...");
            }
            case GRAPPLE -> startGrapple(boss, target);
            default -> {
                pending = a;
                pendingLands = now + SolHeredit.AOE_LAND_TICKS;
                pendingCentre = c;
                pendingFacing = SolHeredit.facing(t.getX() - c.getX(), t.getZ() - c.getZ());
                rooted = SolHeredit.AOE_ROOT_TICKS - 1; // this tick plus the next AOE_ROOT_TICKS - 1
                boss.getNavigation().stop();
                BossFx.warn(boss, a.spear() ? "Sol Heredit draws back his spear!" : "Sol Heredit raises his shield!");
            }
        }
    }

    private void enterPhase(DataBossEntity boss, ServerPlayer target, int p) {
        // The BossDef's phases (same thresholds) announce SolHeredit.PHASE_LINES.
        afterPhase = true;
        if (target != null) {
            sand(boss, target, p >= 6 ? SolHeredit.ENRAGE_INITIAL_SAND : SolHeredit.SAND_PER_PHASE, true);
        }
        nextLaser = Math.min(nextLaser == 0 ? Long.MAX_VALUE : nextLaser, now + 6);
    }

    /** Beams of light around the player (one on the player's tile when {@code onPlayer}) leaving molten sand 2 ticks later. */
    private void sand(DataBossEntity boss, ServerPlayer target, int count, boolean onPlayer) {
        ServerLevel world = BossFx.world(boss);
        BlockPos t = target.blockPosition();
        for (int i = 0; i < count; i++) {
            int dx = i == 0 && onPlayer ? 0 : random.nextInt(2 * SolHeredit.SAND_RADIUS + 1) - SolHeredit.SAND_RADIUS;
            int dz = i == 0 && onPlayer ? 0 : random.nextInt(2 * SolHeredit.SAND_RADIUS + 1) - SolHeredit.SAND_RADIUS;
            Vec3 at = BossFx.ground(world, t.getX() + dx + 0.5, t.getZ() + dz + 0.5, target.getY());
            BossFx.column(world, at, ParticleTypes.END_ROD, 4);
            strike(boss, at, SolHeredit.SAND_DELAY_TICKS, 0, 0.5, ParticleTypes.END_ROD, (hit, pos) ->
                    Mechanics.addHazard(world, pos, SolHeredit.SAND_MAX_DAMAGE, 6000, boss));
        }
    }

    private void tickPending(DataBossEntity boss) {
        if (pending == null || now < pendingLands) {
            return;
        }
        Attack a = pending;
        pending = null;
        ServerLevel world = BossFx.world(boss);
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                if (SolHeredit.hits(a, dx, dz, pendingFacing[0], pendingFacing[1]) && (dx + dz) % 2 == 0) {
                    world.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pendingCentre.getX() + dx + 0.5, pendingCentre.getY() + 0.2,
                            pendingCentre.getZ() + dz + 0.5, 1, 0.3, 0.05, 0.3, 0.0);
                }
            }
        }
        for (ServerPlayer p : BossFx.players(boss, 16)) {
            BlockPos b = p.blockPosition();
            if (SolHeredit.hits(a, b.getX() - pendingCentre.getX(), b.getZ() - pendingCentre.getZ(), pendingFacing[0], pendingFacing[1])) {
                CombatHooks.nameAttack(a.spear() ? "spear" : "shield_bash", null);
                CombatHandler.applyDamage(boss, p, random.nextInt(SolHeredit.AOE_MAX_HIT + 1));
            }
        }
    }

    private void tickTriple(DataBossEntity boss, ServerPlayer target) {
        if (tripleStart < 0 || now < tripleNext) {
            return;
        }
        if (target != null) {
            boolean on = Players.data(target).isActive(Prayer.PROTECT_FROM_MELEE);
            if (SolHeredit.parried(on, protectSince, now)) {
                CombatHooks.nameAttack("triple_attack", null);
                CombatHandler.applyDamage(boss, target, 0);
            } else {
                if (on) {
                    // A prayer switched on too early is switched off and the hit lands.
                    BossFx.disablePrayers(target, "Sol Heredit's spear knocks your prayer away!");
                    protectSince = -1;
                }
                CombatHooks.nameAttack("triple_attack", null);
                CombatHandler.applyDamage(boss, target, SolHeredit.tripleDamage(tripleHit, phase));
            }
        }
        tripleHit++;
        if (tripleHit >= 3) {
            tripleStart = -1;
        } else {
            tripleNext = now + SolHeredit.tripleGap(tripleHit, phase);
        }
    }

    private void startGrapple(DataBossEntity boss, ServerPlayer target) {
        grapple = Grapple.values()[random.nextInt(Grapple.values().length)];
        grappleStart = now;
        grappled = target.getUUID();
        defended = false;
        answered = false;
        GRAPPLED.put(target.getUUID(), this);
        BossFx.say(boss, "Sol Heredit: " + grapple.shout);
        MutableComponent line = Component.literal("Defend: ").withStyle(ChatFormatting.GRAY);
        for (Grapple g : Grapple.values()) {
            String label = g.slot.substring(0, 1).toUpperCase() + g.slot.substring(1);
            line.append(Component.literal("[" + label + "]").withStyle(st -> st.withColor(ChatFormatting.GOLD)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/solheredit parry " + g.slot))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Defend your " + g.slot)))))
                    .append(Component.literal(" "));
        }
        target.displayClientMessage(line, false);
    }

    private void tickGrapple(DataBossEntity boss) {
        if (grapple == null || now - grappleStart < SolHeredit.GRAPPLE_WINDOW_TICKS) {
            return;
        }
        ServerPlayer p = BossFx.world(boss).getServer().getPlayerList().getPlayer(grappled);
        if (p == null) {
            for (ServerPlayer near : BossFx.players(boss, 24)) {
                if (near.getUUID().equals(grappled)) {
                    p = near;
                }
            }
        }
        if (p != null && !defended && boss.isAlive()) {
            CombatHooks.nameAttack("grapple", null);
            CombatHandler.applyDamage(boss, p, random.nextInt(SolHeredit.GRAPPLE_MAX_HIT + 1));
        }
        GRAPPLED.remove(grappled);
        grapple = null;
    }

    /** Light-beam crystals: each shot targets the player's tile and lands a few ticks later. */
    private void tickLasers(DataBossEntity boss, ServerPlayer target) {
        ServerLevel world = BossFx.world(boss);
        for (int i = lasers.size() - 1; i >= 0; i--) {
            long[] l = lasers.get(i);
            Vec3 at = new Vec3(l[1] + 0.5, l[2], l[3] + 0.5);
            if (now < l[0]) {
                BossFx.ring(world, at, 0.6, ParticleTypes.END_ROD);
                continue;
            }
            lasers.remove(i);
            world.sendParticles(ParticleTypes.FLASH, at.x, at.y + 0.5, at.z, 1, 0, 0, 0, 0);
            for (ServerPlayer p : BossFx.players(boss, 24)) {
                if (p.getBlockX() == l[1] && p.getBlockZ() == l[3]) {
                    CombatHooks.nameAttack("laser", null);
                    CombatHandler.applyDamage(boss, p, random.nextInt(SolHeredit.LASER_MAX_HIT + 1));
                }
            }
        }
        int beams = SolHeredit.beams(phase);
        if (target == null || beams == 0 || now < nextLaser) {
            return;
        }
        boolean enraged = phase >= 6;
        nextLaser = now + Math.max(4, (enraged ? SolHeredit.ENRAGE_LASER_INTERVAL_TICKS : SolHeredit.LASER_INTERVAL_TICKS) / beams);
        BlockPos t = target.blockPosition();
        // The crystal circles the arena's edge; it shines a harmless beam at the player's tile, then fires.
        double angle = (beamIndex++ % 8) * Math.PI / 4;
        Vec3 crystal = new Vec3(t.getX() + 0.5 + Math.cos(angle) * 8, target.getY() + 1, t.getZ() + 0.5 + Math.sin(angle) * 8);
        world.sendParticles(ParticleTypes.END_ROD, crystal.x, crystal.y, crystal.z, 6, 0.2, 0.2, 0.2, 0.0);
        lasers.add(new long[]{now + (enraged ? SolHeredit.ENRAGE_LASER_WARNING_TICKS : SolHeredit.LASER_WARNING_TICKS),
                t.getX(), (long) Math.floor(target.getY()), t.getZ()});
        Msg.action(target, "A beam of light locks onto you!");
    }
}
