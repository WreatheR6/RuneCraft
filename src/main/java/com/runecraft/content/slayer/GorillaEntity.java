package com.runecraft.content.slayer;

import com.runecraft.action.ActionManager;
import com.runecraft.combat.CombatHandler;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.slayer.DemonicGorillas;
import com.runecraft.osrs.content.slayer.DemonicGorillas.Style;
import com.runecraft.util.Msg;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Demonic and tortured gorillas (see {@link DemonicGorillas}): they attack with one style at a time and switch to another
 * after three (tortured: four) missed attacks; demonic gorillas pray against one style and switch to the style that last
 * damaged them after four hits totalling 70 damage; on Ranged or Magic they sometimes drop a boulder on the player's
 * tile for a third of their current hitpoints.
 */
public class GorillaEntity extends OsrsMonsterEntity {
    private DemonicGorillas.Tracker tracker;
    private Random rng;
    /** True while one of its own standard attacks is in flight (its outcome counts towards the style switch). */
    private boolean countingAttack;
    private ServerPlayer boulderTarget;
    private Vec3 boulderAt;
    private long boulderDue = -1;
    private long lastCombatTick;
    /** The protection prayer shown over a demonic gorilla's head (its name tag), or null before the first update. */
    private Style shownPrayer;

    public GorillaEntity(EntityType<? extends Monster> type, Level world) {
        super(type, world);
    }

    private boolean demonic() {
        return stats() != null && stats().profile() != null && DemonicGorillas.DEMONIC.equals(stats().profile().id);
    }

    private DemonicGorillas.Tracker tracker() {
        if (tracker == null) {
            rng = new Random(getRandom().nextLong());
            tracker = new DemonicGorillas.Tracker(demonic(), rng);
        }
        return tracker;
    }

    private String label() {
        return demonic() ? "demonic gorilla" : "tortured gorilla";
    }

    private AttackDef attackFor(Style style) {
        List<AttackDef> attacks = attackList();
        if (attacks == null) {
            return null;
        }
        for (AttackDef a : attacks) {
            if (Style.of(a.style()) == style) {
                return a;
            }
        }
        return attacks.get(0);
    }

    /** Overhead prayer indicator: the active protection prayer in the gorilla's name tag, coloured by style. */
    private void showPrayer() {
        if (!demonic()) {
            return;
        }
        Style prayer = tracker().prayer();
        if (prayer == null || prayer == shownPrayer) {
            return;
        }
        shownPrayer = prayer;
        ChatFormatting colour = switch (prayer) {
            case MELEE -> ChatFormatting.RED;
            case RANGED -> ChatFormatting.GREEN;
            case MAGIC -> ChatFormatting.AQUA;
        };
        setCustomName(Component.literal("Demonic gorilla ").append(Component.literal("[" + prayer.prayerName() + "]").withStyle(colour)));
        setCustomNameVisible(true);
    }

    @Override
    public AttackType chooseStyle(ServerPlayer target, double distanceSq) {
        AttackDef a = attackFor(tracker().attackStyle());
        if (a == null) {
            return super.chooseStyle(target, distanceSq);
        }
        nextAttack = a;
        return a.style();
    }

    @Override
    public void performAttack(ServerPlayer target, AttackType type) {
        DemonicGorillas.Tracker t = tracker();
        lastCombatTick = ActionManager.gameTick();
        if (t.attackStyle() != Style.MELEE && boulderDue < 0 && rng.nextInt(DemonicGorillas.BOULDER_CHANCE) == 0) {
            // The boulder toss: beats its chest and a rock falls on the player's tile a moment later.
            boulderTarget = target;
            boulderAt = target.position();
            boulderDue = ActionManager.gameTick() + DemonicGorillas.BOULDER_DELAY_TICKS;
            swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            ServerLevel world = (ServerLevel) level();
            world.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, boulderAt.x, boulderAt.y + 3,
                    boulderAt.z, 20, 0.5, 0.5, 0.5, 0.01);
            world.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 0.6f, 1.4f);
            Msg.action(target, "The " + label() + " beats its chest - a boulder falls towards you!");
            return;
        }
        countingAttack = true;
        try {
            super.performAttack(target, type);
        } finally {
            countingAttack = false;
        }
    }

    @Override
    public int onHitPlayer(ServerPlayer player, int damage, AttackType type) {
        int out = super.onHitPlayer(player, damage, type);
        if (countingAttack && tracker().attacked(out <= 0, rng)) {
            Style now = tracker().attackStyle();
            ((ServerLevel) level()).sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getY() + getBbHeight() + 0.3, getZ(), 4, 0.4, 0.2,
                    0.4, 0.0);
            level().playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 0.8f, 0.8f);
            Msg.action(player, "The " + label() + " screams \"Rhaaaaaaa!\" and switches to " + now.name().toLowerCase(Locale.ROOT) + ".");
        }
        return out;
    }

    @Override
    public int modifyIncoming(ServerPlayer attacker, int damage, AttackType type) {
        int d = super.modifyIncoming(attacker, damage, type);
        DemonicGorillas.Tracker t = tracker();
        lastCombatTick = ActionManager.gameTick();
        Style style = Style.of(type);
        if (t.blocks(style) && !com.runecraft.combat.SetEffectHooks.takeVeracHit(attacker)) {
            Msg.action(attacker, "The demonic gorilla's " + style.prayerName() + " blocks your attack.");
            return 0;
        }
        if (t.damaged(style, d)) {
            ((ServerLevel) level()).sendParticles(ParticleTypes.ENCHANT, getX(), getY() + getBbHeight(), getZ(), 20, 0.5, 0.4, 0.5, 0.2);
            Msg.action(attacker, "The demonic gorilla pounds its chest and switches to " + style.prayerName() + "!");
        }
        showPrayer();
        return d;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % 20 == 0 && demonic()) {
            showPrayer();
        }
        if (tickCount % ActionManager.MC_TICKS_PER_GAME_TICK != 0 || tracker == null) {
            return;
        }
        long now = ActionManager.gameTick();
        if (boulderDue >= 0 && now < boulderDue) {
            // The boulder's shadow on the marked tile.
            ((ServerLevel) level()).sendParticles(ParticleTypes.LARGE_SMOKE, boulderAt.x, boulderAt.y + 0.1, boulderAt.z, 6, 0.4, 0.02,
                    0.4, 0.0);
        }
        if (boulderDue >= 0 && now >= boulderDue) {
            boulderDue = -1;
            ServerLevel world = (ServerLevel) level();
            world.sendParticles(ParticleTypes.EXPLOSION, boulderAt.x, boulderAt.y + 0.5, boulderAt.z, 2, 0.3, 0.2, 0.3, 0);
            world.playSound(null, boulderAt.x, boulderAt.y, boulderAt.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.5f, 1.2f);
            // One tile (two blocks) around the marked spot: step off it to dodge.
            if (boulderTarget != null && boulderTarget.isAlive() && boulderTarget.position().distanceToSqr(boulderAt) < 1.0) {
                int hp = (int) Math.ceil(boulderTarget.getHealth());
                CombatHandler.applyDamage(this, boulderTarget, DemonicGorillas.boulderDamage(hp));
            }
            boulderTarget = null;
        }
        if (getTarget() == null && now - lastCombatTick > DemonicGorillas.STYLE_RESET_TICKS && tracker.misses() > 0) {
            tracker.resetStyle(rng);
            lastCombatTick = now;
        }
    }
}
