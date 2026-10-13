package com.runecraft.content.slayer;

import com.runecraft.action.ActionManager;
import com.runecraft.combat.CombatHandler;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.slayer.DemonicGorillas;
import com.runecraft.osrs.content.slayer.DemonicGorillas.Style;
import com.runecraft.util.Msg;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.Locale;
import java.util.Random;

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
    private ServerPlayerEntity boulderTarget;
    private Vec3d boulderAt;
    private long boulderDue = -1;
    private long lastCombatTick;

    public GorillaEntity(EntityType<? extends HostileEntity> type, World world) {
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

    @Override
    public AttackType chooseStyle(ServerPlayerEntity target, double distanceSq) {
        AttackDef a = attackFor(tracker().attackStyle());
        if (a == null) {
            return super.chooseStyle(target, distanceSq);
        }
        nextAttack = a;
        return a.style();
    }

    @Override
    public void performAttack(ServerPlayerEntity target, AttackType type) {
        DemonicGorillas.Tracker t = tracker();
        lastCombatTick = ActionManager.gameTick();
        if (t.attackStyle() != Style.MELEE && boulderDue < 0 && rng.nextInt(DemonicGorillas.BOULDER_CHANCE) == 0) {
            // The boulder toss: beats its chest and a rock falls on the player's tile a moment later.
            boulderTarget = target;
            boulderAt = target.getPos();
            boulderDue = ActionManager.gameTick() + DemonicGorillas.BOULDER_DELAY_TICKS;
            swingHand(net.minecraft.util.Hand.MAIN_HAND);
            ServerWorld world = (ServerWorld) getWorld();
            world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, boulderAt.x, boulderAt.y + 3,
                    boulderAt.z, 20, 0.5, 0.5, 0.5, 0.01);
            world.playSound(null, getBlockPos(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 0.6f, 1.4f);
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
    public int onHitPlayer(ServerPlayerEntity player, int damage, AttackType type) {
        int out = super.onHitPlayer(player, damage, type);
        if (countingAttack && tracker().attacked(out <= 0, rng)) {
            Style now = tracker().attackStyle();
            ((ServerWorld) getWorld()).spawnParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getY() + getHeight() + 0.3, getZ(), 4, 0.4, 0.2,
                    0.4, 0.0);
            getWorld().playSound(null, getBlockPos(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 0.8f, 0.8f);
            Msg.action(player, "The " + label() + " screams \"Rhaaaaaaa!\" and switches to " + now.name().toLowerCase(Locale.ROOT) + ".");
        }
        return out;
    }

    @Override
    public int modifyIncoming(ServerPlayerEntity attacker, int damage, AttackType type) {
        int d = super.modifyIncoming(attacker, damage, type);
        DemonicGorillas.Tracker t = tracker();
        lastCombatTick = ActionManager.gameTick();
        Style style = Style.of(type);
        if (t.blocks(style)) {
            Msg.action(attacker, "The demonic gorilla's " + style.prayerName() + " blocks your attack.");
            return 0;
        }
        if (t.damaged(style, d)) {
            ((ServerWorld) getWorld()).spawnParticles(ParticleTypes.ENCHANT, getX(), getY() + getHeight(), getZ(), 20, 0.5, 0.4, 0.5, 0.2);
            Msg.action(attacker, "The demonic gorilla pounds its chest and switches to " + style.prayerName() + "!");
        }
        return d;
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (age % ActionManager.MC_TICKS_PER_GAME_TICK != 0 || tracker == null) {
            return;
        }
        long now = ActionManager.gameTick();
        if (boulderDue >= 0 && now < boulderDue) {
            // The boulder's shadow on the marked tile.
            ((ServerWorld) getWorld()).spawnParticles(ParticleTypes.LARGE_SMOKE, boulderAt.x, boulderAt.y + 0.1, boulderAt.z, 6, 0.4, 0.02,
                    0.4, 0.0);
        }
        if (boulderDue >= 0 && now >= boulderDue) {
            boulderDue = -1;
            ServerWorld world = (ServerWorld) getWorld();
            world.spawnParticles(ParticleTypes.EXPLOSION, boulderAt.x, boulderAt.y + 0.5, boulderAt.z, 2, 0.3, 0.2, 0.3, 0);
            world.playSound(null, boulderAt.x, boulderAt.y, boulderAt.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 0.5f, 1.2f);
            // One tile (two blocks) around the marked spot: step off it to dodge.
            if (boulderTarget != null && boulderTarget.isAlive() && boulderTarget.getPos().squaredDistanceTo(boulderAt) < 1.0) {
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
