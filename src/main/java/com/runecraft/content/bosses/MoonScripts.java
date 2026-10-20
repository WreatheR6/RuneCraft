package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.Equipment;
import com.runecraft.combat.HitCalcBridge;
import com.runecraft.combat.MonsterStats;
import com.runecraft.content.systems.CombatAchievementService;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Chance;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.bosses.BossMechanics;
import com.runecraft.osrs.content.bosses.MoonsRules;
import com.runecraft.osrs.content.bosses.MoonsRules.Special;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The Moons of Peril (zeah module bosses; OSRS Wiki "Moons of Peril/Strategies", {@link MoonsRules}). Every Moon:
 * <ul>
 *   <li>stands in the middle of her chamber and attacks every 6 ticks with a typeless three-hit stack (4, 8 and 20; a miss
 *   ends it), each landed hit adding 1, 2 or 5 stacks of her Curse of the Moon (decaying 2 every 5 ticks); she wears herself
 *   down a little with every attack;</li>
 *   <li>Eyatlalli's glyph glows on one of eight sigils round her: off it during her standard attacks the player takes 1, 2,
 *   4, 8, then 16 a tick and a curse stack; it moves on clockwise after every two attacks, and the full and new moon start
 *   her specials (one every six standard attacks, alternating);</li>
 *   <li>has flat armour (Blue -5, Eclipse 6 or 4 during her Clones, Blood -2) and her own curse: Blue locks the player's
 *   joints at 18 stacks (the next attack is cancelled), Eclipse makes attacks glance (max hit 12 lower, 8 during the Clones,
 *   at a chance of stacks %), Blood heals off her hits (1x, 2x, 3x within 30-50) and her mechanics (2x, jaguars 3x).</li>
 * </ul>
 * Specials: <b>Blue</b> - Weapon Freeze (the weapon is frozen into one of three blocks of ice; punch the glowing one free
 * with bare hands before it shatters for up to 28, dodging ice shards) and Frost Storm (two braziers go out: relight them while
 * dodging the tornadoes, or she heals 10 per unlit brazier five times; both lit lift some of the curse). <b>Eclipse</b> -
 * Searing Rays (keep behind her moon shield through its full turn, each ray that finds the player a little damage and a big
 * dose of curse) and Clones (bound, fifteen clones in five cycles: face each to parry it for an always-hitting melee hit;
 * only melee hurts her meanwhile). <b>Blood</b> - Blood Rain (she vanishes and blood pools rain down: stay off them) and
 * Blood Jaguars (eight jaguars cleave the tiles in front of them six times while pools form under the player; stand on the
 * glyph and hit its jaguar to win back up to 25 hitpoints).
 * approx: the special timings, the Searing Rays' and tornadoes' damage, how many punches free the weapon, and her wear.
 */
final class MoonScripts {
    private MoonScripts() {
    }

    /** The creature standing for the Blue Moon's blocks of frozen weapons. */
    static final String FROZEN_WEAPONS = "moon_frozen_weapons";
    private static final String JAGUAR = "blood_jaguar";
    /** Blocks between the Moon and her glyph ring. approx */
    private static final double GLYPH_RADIUS = 4.0;
    private static final double ON_GLYPH = 1.3;
    private static final DustParticleOptions GLYPH_LIT = new DustParticleOptions(new Vector3f(1.0f, 0.85f, 0.3f), 1.6f);
    private static final DustParticleOptions GLYPH_DIM = new DustParticleOptions(new Vector3f(0.5f, 0.5f, 0.6f), 0.8f);

    private static AttackDef attack(DataBossEntity boss, String name) {
        BossDef def = boss.def();
        if (def == null) {
            return null;
        }
        for (AttackDef a : def.attacks) {
            if (a.name().equals(name)) {
                return a;
            }
        }
        return null;
    }

    /**
     * A hit after a Moon's flat armour (OSRS Wiki "Flat armour"): only successful melee and ranged hits change; player magic
     * (powered staves too) and thralls ignore it.
     */
    static int flatArmour(int damage, int armour, AttackType type) {
        if (type == AttackType.MAGIC || com.runecraft.content.systems.SpellService.thrallHitting()) {
            return damage;
        }
        return BossMechanics.flatArmour(damage, armour);
    }

    private static boolean isWeapon(ItemStack s) {
        if (s.isEmpty()) {
            return false;
        }
        com.runecraft.osrs.ItemDef def = com.runecraft.registry.ModItems.def(s);
        return def != null && def.equipment() != null && def.equipment().slot() == com.runecraft.osrs.EquipmentDef.Slot.WEAPON
                || s.getItem() instanceof net.minecraft.world.item.SwordItem || s.getItem() instanceof net.minecraft.world.item.BowItem;
    }

    /** Moves the weapon in a player's hand into a free pack slot (it never leaves the player); false when there is no room. */
    static boolean stowWeapon(ServerPlayer p) {
        ItemStack held = p.getMainHandItem();
        if (!isWeapon(held)) {
            return false;
        }
        var inv = p.getInventory();
        for (int i = 0; i < inv.items.size(); i++) {
            if (i != inv.selected && inv.items.get(i).isEmpty()) {
                inv.items.set(i, held.copy());
                p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                return true;
            }
        }
        return false;
    }

    /** Puts a weapon (by id) from the pack back in an empty hand. */
    static void rewield(ServerPlayer p, String id) {
        if (id == null || !p.getMainHandItem().isEmpty()) {
            return;
        }
        var inv = p.getInventory();
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack s = inv.items.get(i);
            if (i != inv.selected && !s.isEmpty() && id.equals(Inv.idOf(s))) {
                p.setItemInHand(InteractionHand.MAIN_HAND, s.copy());
                inv.items.set(i, ItemStack.EMPTY);
                return;
            }
        }
    }

    // =====================================================================================================
    // The shared fight
    // =====================================================================================================

    abstract static class Moon extends ScriptedBoss {
        /** A landed hit of her standard attack, due on a later tick (the three come a tick apart). */
        private record Pending(ServerPlayer player, int index, int damage, long at) {
        }

        final String id;
        int glyph = MoonsRules.FIRST_GLYPH;
        private int sinceGlyph;
        private boolean specialDue;
        Special special;
        int specialTick;
        final Map<UUID, MoonsRules.Curse> curses = new HashMap<>();
        private final Map<UUID, Integer> offGlyph = new HashMap<>();
        private final List<Pending> pending = new ArrayList<>();
        /** Specials that ran out on the player (a failed freeze, unlit braziers...): for the self-test walk. */
        int failures;
        int specialsDone;

        Moon(String id) {
            this.id = id;
        }

        @Override
        protected boolean driven() {
            return true;
        }

        @Override
        protected boolean canAttack(DataBossEntity boss) {
            return special == null;
        }

        @Override
        protected int attackDelay(DataBossEntity boss, AttackDef attack) {
            return MoonsRules.ATTACK_SPEED;
        }

        /** Her standard attack's definition (its name is what her hits report). */
        abstract AttackDef standard(DataBossEntity boss);

        /** The Moon's flat armour now. */
        int armour() {
            return MoonsRules.flatArmour(id, false);
        }

        MoonsRules.Curse curse(ServerPlayer p) {
            return curses.computeIfAbsent(p.getUUID(), k -> new MoonsRules.Curse());
        }

        void addCurse(ServerPlayer p, int stacks) {
            int warned = curse(p).add(stacks);
            if (warned > 0 && MoonsRules.BLUE.equals(id)) {
                Msg.game(p, warned < MoonsRules.BLUE_WARNINGS[1] ? "The Blue Moon pierces your defence. The cold is starting to get in..."
                        : "The Blue Moon pierces your defence. Your joints are starting to lock up...");
            }
        }

        /** The players in her chamber (and her target, which may be a test player). */
        List<ServerPlayer> fighters(DataBossEntity boss, ServerPlayer target) {
            double r = boss.def() == null ? 12 : boss.def().arenaRadius + 1;
            List<ServerPlayer> list = new ArrayList<>(BossFx.players(boss, r));
            if (target != null && target.isAlive() && !list.contains(target)) {
                list.add(target);
            }
            return list;
        }

        /** Where a glyph (0 = the full moon, north; clockwise) stands round her. */
        Vec3 glyphPos(DataBossEntity boss, int g) {
            double a = g * Math.PI * 2 / MoonsRules.GLYPHS;
            Vec3 c = homeVec();
            return BossFx.ground(BossFx.world(boss), c.x + Math.sin(a) * GLYPH_RADIUS, c.z - Math.cos(a) * GLYPH_RADIUS, c.y);
        }

        Vec3 glyphPos(DataBossEntity boss) {
            return glyphPos(boss, glyph);
        }

        /** Whether the glyph must be stood on now (between specials, and through the Blood Moon's jaguars). */
        boolean glyphActive() {
            return special == null || special == Special.BLOOD_JAGUARS;
        }

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            return flatArmour(damage, armour(), type);
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            if (specialDue) {
                specialDue = false;
                special = MoonsRules.special(id, glyph == MoonsRules.FULL_MOON);
                specialTick = 0;
                specialsDone++;
                CombatAchievementService.countNear(boss, "used:" + special.key);
                startSpecial(boss, target, special);
                return null;
            }
            standardAttack(boss, target);
            cooldown = MoonsRules.ATTACK_SPEED - 1;
            return null;
        }

        /** Three hits rolled against the player's melee defence (slash), a miss ending the stack; they land a tick apart. */
        void standardAttack(DataBossEntity boss, ServerPlayer target) {
            AttackDef def = standard(boss);
            last = def;
            attackCount++;
            com.runecraft.boss.Mechanics.attackListener.onAttack(boss, target, def);
            MonsterStats stats = MonsterStats.of(boss);
            long attackRoll = CombatHandler.npcAttackRoll(boss, stats, AttackType.SLASH, stats.attack());
            long defenceRoll = CombatHandler.defenceRoll(target, AttackType.SLASH);
            double chance = Chance.hitChance(attackRoll, defenceRoll);
            int[] hits = MoonsRules.standardHits(chance, boss.getRandom()::nextDouble, boss.getRandom()::nextInt);
            for (int i = 0; i < hits.length; i++) {
                if (hits[i] >= 0) {
                    pending.add(new Pending(target, i, hits[i], now + 1 + i));
                }
            }
            if (hits[0] < 0) {
                com.runecraft.network.ServerNetworking.hitsplat(target, 0, com.runecraft.network.Net.SPLAT_MISS);
            }
            BossFx.burst(boss, ParticleTypes.SWEEP_ATTACK, 2, 0.8);
            int wear = MoonsRules.selfDamage(percent(boss) / 100.0 * boss.trueHitpoints());
            if (wear > 0) {
                boss.setHealth(Math.max(1f, boss.getHealth() - mcHp(boss, wear)));
            }
            if (++sinceGlyph >= MoonsRules.STANDARD_PER_GLYPH) {
                sinceGlyph = 0;
                glyph = MoonsRules.nextGlyph(glyph);
                specialDue = MoonsRules.specialGlyph(glyph);
            }
        }

        /** A landed standard hit reached the player: the curse stacks (and the Blood Moon heals). */
        void standardLanded(DataBossEntity boss, ServerPlayer p, int index, int dealt) {
            addCurse(p, MoonsRules.CURSE_PER_HIT[index]);
        }

        abstract void startSpecial(DataBossEntity boss, ServerPlayer target, Special s);

        /** One tick of the running special; true when it is over. */
        abstract boolean tickSpecial(DataBossEntity boss, ServerPlayer target);

        /** Clears what a special left (minions, binds, weapons): it ended, she died or she was removed. */
        void cleanUp(DataBossEntity boss) {
        }

        void endSpecial(DataBossEntity boss) {
            cleanUp(boss);
            special = null;
            glyph = MoonsRules.nextGlyph(glyph); // the glyph moves on from the full or new moon
            sinceGlyph = 0;
            cooldown = 2;
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
            ServerLevel w = BossFx.world(boss);
            // she keeps to the middle of her chamber
            Vec3 c = homeVec();
            boss.getNavigation().stop();
            if (boss.position().distanceToSqr(c) > 1.5 * 1.5) {
                boss.teleportTo(c.x, c.y, c.z);
            }
            List<ServerPlayer> here = fighters(boss, target);
            for (ServerPlayer p : here) {
                curse(p).tick();
            }
            // her standard hits, a tick apart; a player who left her reach (or stepped under her) dodges the rest
            pending.removeIf(h -> {
                if (now < h.at()) {
                    return false;
                }
                ServerPlayer p = h.player();
                double d = Math.sqrt(p.distanceToSqr(boss.getX(), p.getY(), boss.getZ()));
                if (p.isAlive() && d <= 10 && d >= 1.0) {
                    CombatHooks.nameAttack(standard(boss).name(), null);
                    int dealt = CombatHandler.applyDamage(boss, p, h.damage());
                    standardLanded(boss, p, h.index(), dealt);
                }
                return true;
            });
            // Eyatlalli's glyph
            for (int g = 0; g < MoonsRules.GLYPHS; g++) {
                Vec3 at = glyphPos(boss, g);
                boolean lit = g == glyph && glyphActive();
                w.sendParticles(lit ? GLYPH_LIT : GLYPH_DIM, at.x, at.y + 0.1, at.z, lit ? 6 : 1, lit ? 0.4 : 0.1, 0.02, lit ? 0.4 : 0.1, 0);
            }
            if (glyphActive() && attackCount > 0) {
                Vec3 at = glyphPos(boss);
                for (ServerPlayer p : here) {
                    if (p.position().distanceToSqr(at.x, p.getY(), at.z) <= ON_GLYPH * ON_GLYPH) {
                        offGlyph.remove(p.getUUID());
                        continue;
                    }
                    int n = offGlyph.merge(p.getUUID(), 1, Integer::sum);
                    CombatHandler.applyDamage(boss, p, MoonsRules.offGlyphDamage(n));
                    addCurse(p, 1);
                    if (n == 1) {
                        Msg.red(p, "Stand on Eyatlalli's glowing glyph!");
                    }
                }
            }
            if (special != null) {
                specialTick++;
                if (tickSpecial(boss, target)) {
                    endSpecial(boss);
                }
            }
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayer killer) {
            cleanUp(boss);
            special = null;
            pending.clear();
        }

        @Override
        public void onRemoved(DataBossEntity boss) {
            cleanUp(boss);
            special = null;
            pending.clear();
        }

        // self-test hooks
        /** Ticks in a row a player has stood off the glyph (0 on it). */
        int offGlyphTicks(ServerPlayer p) {
            return offGlyph.getOrDefault(p.getUUID(), 0);
        }

        void selfTestSpecialNow() {
            specialDue = true;
            glyph = MoonsRules.FULL_MOON;
        }

        void selfTestNewMoonNow() {
            specialDue = true;
            glyph = MoonsRules.NEW_MOON;
        }
    }

    // =====================================================================================================
    // The Blue Moon
    // =====================================================================================================

    /** Blue Moon: -5 flat armour, the joint-locking curse, Weapon Freeze and Frost Storm. */
    static final class BlueMoon extends Moon {
        // Weapon Freeze
        private final List<Mob> blocks = new ArrayList<>();
        private Mob realBlock;
        private int punches;
        private ServerPlayer frozenOwner;
        private String frozenWeapon;
        // Frost Storm
        private final boolean[] lit = new boolean[MoonsRules.BRAZIERS];
        private final List<Vec3> storms = new ArrayList<>();
        private int heals;

        BlueMoon() {
            super(MoonsRules.BLUE);
        }

        @Override
        AttackDef standard(DataBossEntity boss) {
            AttackDef a = attack(boss, "spear thrust");
            return a != null ? a : boss.def().attacks.get(0);
        }

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (attacker != null && curse(attacker).blueLocks()) {
                Msg.game(attacker, "Your joints lock up as you go to attack. The cold is too much to bear!");
                return 0;
            }
            return super.incoming(boss, attacker, damage, type);
        }

        Vec3 brazier(int i) {
            Vec3 c = homeVec();
            return c.add(i == 0 ? -8 : 8, 0, 0);
        }

        @Override
        void startSpecial(DataBossEntity boss, ServerPlayer target, Special s) {
            if (s == Special.WEAPON_FREEZE) {
                frozenOwner = target;
                frozenWeapon = isWeapon(target.getMainHandItem()) ? Inv.idOf(target.getMainHandItem()) : null;
                if (frozenWeapon != null && stowWeapon(target)) {
                    Msg.red(target, "The Blue Moon freezes your weapon out of your hands!");
                } else {
                    frozenWeapon = null;
                    CombatHandler.applyDamage(boss, target, MoonsRules.FREEZE_NO_WEAPON[0]
                            + boss.getRandom().nextInt(MoonsRules.FREEZE_NO_WEAPON[1] - MoonsRules.FREEZE_NO_WEAPON[0] + 1));
                    Msg.red(target, "With no weapon in hand, your hands begin to freeze over!");
                }
                punches = 0;
                int real = boss.getRandom().nextInt(MoonsRules.FREEZE_BLOCKS);
                Vec3 c = homeVec();
                for (int i = 0; i < MoonsRules.FREEZE_BLOCKS; i++) {
                    double a = Math.PI * 2 * i / MoonsRules.FREEZE_BLOCKS + Math.PI / 2;
                    Mob m = BossFx.spawnMinion(boss, FROZEN_WEAPONS,
                            BossFx.ground(BossFx.world(boss), c.x + Math.cos(a) * 6, c.z + Math.sin(a) * 6, c.y), null);
                    if (m == null) {
                        continue;
                    }
                    blocks.add(m);
                    boolean isReal = i == real;
                    if (isReal) {
                        realBlock = m;
                        m.setGlowingTag(true);
                    }
                    MinionRules.set(m, new MinionRules.Rule() {
                        @Override
                        public double accuracy(ServerPlayer p, LivingEntity mob, AttackType type) {
                            return MinionRules.ALWAYS_HITS;
                        }

                        @Override
                        public int damage(ServerPlayer p, LivingEntity mob, AttackType type, int d) {
                            if (mob != realBlock) {
                                Msg.action(p, "Your weapon isn't in there!");
                                return 0;
                            }
                            if (!p.getMainHandItem().isEmpty() || !type.isMelee()) {
                                Msg.action(p, "Only your bare hands can break the ice.");
                                return 0;
                            }
                            return ++punches >= MoonsRules.FREEZE_PUNCHES ? MinionRules.lethal(mob) : 1;
                        }
                    });
                }
                BossFx.warn(boss, "Punch the glowing block of ice to free your weapon!");
            } else {
                lit[0] = false;
                lit[1] = false;
                heals = 0;
                storms.clear();
                Vec3 c = homeVec();
                storms.add(c.add(0, 0, -6));
                storms.add(c.add(0, 0, 6));
                BossFx.say(boss, "The Blue Moon conjures a frost storm and the braziers go out! Relight them!");
            }
        }

        @Override
        boolean tickSpecial(DataBossEntity boss, ServerPlayer target) {
            ServerLevel w = BossFx.world(boss);
            if (special == Special.WEAPON_FREEZE) {
                if (frozenOwner != null && isWeapon(frozenOwner.getMainHandItem()) && stowWeapon(frozenOwner)) {
                    Msg.red(frozenOwner, "Your hands are too cold to hold a weapon until the ice is broken!");
                }
                if (realBlock != null) {
                    w.sendParticles(ParticleTypes.SNOWFLAKE, realBlock.getX(), realBlock.getY() + 1.2, realBlock.getZ(), 3, 0.3, 0.3, 0.3, 0);
                }
                if (specialTick % 3 == 0 && frozenOwner != null) {
                    strike(boss, around(boss, frozenOwner.position(), 2), 2, 4, 1.0, ParticleTypes.SNOWFLAKE); // approx: ice shards
                }
                if (realBlock == null || !realBlock.isAlive()) {
                    Msg.game(frozenOwner, "You free your weapon from the ice.");
                    return true;
                }
                if (specialTick >= MoonsRules.FREEZE_TICKS) {
                    failures++;
                    if (frozenOwner != null) {
                        CombatHooks.nameAttack(Special.WEAPON_FREEZE.key, null);
                        CombatHandler.applyDamage(boss, frozenOwner, boss.getRandom().nextInt(MoonsRules.FREEZE_FAIL_MAX + 1));
                        Msg.red(frozenOwner, "The ice shatters around your weapon!");
                    }
                    return true;
                }
                return false;
            }
            // Frost Storm
            for (int i = 0; i < MoonsRules.BRAZIERS; i++) {
                Vec3 b = brazier(i);
                w.sendParticles(lit[i] ? ParticleTypes.FLAME : ParticleTypes.SMOKE, b.x, b.y + 0.8, b.z, 4, 0.2, 0.3, 0.2, 0.01);
                for (ServerPlayer p : fighters(boss, target)) {
                    if (!lit[i] && p.position().distanceToSqr(b.x, p.getY(), b.z) <= 1.6 * 1.6) {
                        lit[i] = true;
                        Msg.game(p, "You relight the brazier.");
                    }
                }
            }
            for (int s = 0; s < storms.size(); s++) {
                double a = (specialTick * 0.35) + s * Math.PI;
                Vec3 c = homeVec();
                Vec3 at = BossFx.ground(w, c.x + Math.cos(a) * 6, c.z + Math.sin(a) * 6, c.y);
                storms.set(s, at);
                w.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y + 1, at.z, 10, 0.4, 0.9, 0.4, 0.05);
                for (ServerPlayer p : fighters(boss, target)) {
                    if (p.position().distanceToSqr(at.x, p.getY(), at.z) <= 1.2 * 1.2) {
                        CombatHandler.applyDamage(boss, p, 1 + boss.getRandom().nextInt(4)); // approx
                        BossFx.stun(p, 2); // forced to walk for two ticks
                    }
                }
            }
            if (specialTick % MoonsRules.STORM_HEAL_EVERY == 0) {
                heals++;
                int unlit = (lit[0] ? 0 : 1) + (lit[1] ? 0 : 1);
                int heal = MoonsRules.stormHeal(unlit);
                if (heal > 0) {
                    boss.heal(mcHp(boss, heal));
                    BossFx.burst(boss, ParticleTypes.SNOWFLAKE, 10, 1.0);
                }
                if (heals >= MoonsRules.STORM_HEALS) {
                    if (unlit == 0) {
                        for (ServerPlayer p : fighters(boss, target)) {
                            curse(p).remove(curse(p).stacks() / 2); // approx: "a portion" of the curse
                        }
                    } else {
                        failures++;
                    }
                    return true;
                }
            }
            return false;
        }

        @Override
        void cleanUp(DataBossEntity boss) {
            blocks.forEach(Mob::discard);
            blocks.clear();
            realBlock = null;
            if (frozenOwner != null) {
                rewield(frozenOwner, frozenWeapon);
            }
            frozenOwner = null;
            frozenWeapon = null;
            storms.clear();
        }

        Mob selfTestRealBlock() {
            return realBlock;
        }

        Vec3 selfTestBrazier(int i) {
            return brazier(i);
        }
    }

    // =====================================================================================================
    // The Eclipse Moon
    // =====================================================================================================

    /**
     * Eclipse Moon: 6 flat armour (4 during the Clones), glancing attacks, Searing Rays and Clones. Clones: the player is
     * bound and fifteen clones (five cycles of three) appear around them one at a time; facing a clone as it attacks parries
     * it, which sends a melee hit back at the Moon that always lands; a clone that isn't faced hits the player. approx: the
     * player is bound where they stand rather than shoved to the centre of the arena; clone timing is not documented.
     */
    static final class EclipseMoon extends Moon {
        private int cloneStep = -1;
        private long cloneNext;
        private Vec3 clone;
        private ServerPlayer bound;
        private boolean parrying;
        /** Searing Rays: the moon shield's angle round her (radians) and the player it shelters. */
        private double shield;
        private ServerPlayer sheltered;

        EclipseMoon() {
            super(MoonsRules.ECLIPSE);
        }

        @Override
        AttackDef standard(DataBossEntity boss) {
            AttackDef a = attack(boss, "shield bash");
            return a != null ? a : boss.def().attacks.get(0);
        }

        @Override
        int armour() {
            return MoonsRules.flatArmour(id, cloneStep >= 0);
        }

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (attacker != null && !parrying) {
                CombatAchievementService.flag(attacker, BossMechanics.FLAG_ECLIPSE_DIRECT_HIT);
                if (cloneStep >= 0 && !type.isMelee()) {
                    return 0; // only melee hurts her during the Clones
                }
            }
            if (attacker != null && damage > 0 && boss.getRandom().nextDouble() < MoonsRules.glanceChance(curse(attacker).stacks())) {
                Msg.action(attacker, "Blinded by the Eclipse Moon's light, your attack glances off its shield!");
                if (type.isMelee()) {
                    int max = Math.max(1, HitCalcBridge.maxHit(attacker, boss, HitCalcBridge.input(attacker, boss, type, null, 0)));
                    damage = MoonsRules.glance(damage, max, cloneStep >= 0);
                } else {
                    damage = Math.max(0, damage - (cloneStep >= 0 ? MoonsRules.ECLIPSE_GLANCE_CLONES : MoonsRules.ECLIPSE_GLANCE));
                }
            }
            return super.incoming(boss, attacker, damage, type);
        }

        private Vec3 ring(DataBossEntity boss, double angle, double r) {
            Vec3 c = homeVec();
            return BossFx.ground(BossFx.world(boss), c.x + Math.cos(angle) * r, c.z + Math.sin(angle) * r, c.y);
        }

        /** Where a player is safe from the Searing Rays now: just behind the moon shield. */
        Vec3 shelter(DataBossEntity boss) {
            return ring(boss, shield, GLYPH_RADIUS + 1.5);
        }

        @Override
        void startSpecial(DataBossEntity boss, ServerPlayer target, Special s) {
            if (s == Special.CLONES) {
                startClones(boss, target);
                return;
            }
            sheltered = target;
            Vec3 c = homeVec();
            shield = Math.atan2(target.getZ() - c.z, target.getX() - c.x);
            Vec3 at = shelter(boss);
            target.teleportTo(at.x, at.y, at.z); // shoved behind the shield
            BossFx.say(boss, "A moon shield appears and the Eclipse Moon shoves you behind it! Keep behind it!");
        }

        @Override
        boolean tickSpecial(DataBossEntity boss, ServerPlayer target) {
            if (special == Special.CLONES) {
                return tickClones(boss);
            }
            ServerLevel w = BossFx.world(boss);
            if (specialTick % 2 == 0) {
                // the rays fire past the shield, then it moves on (a full turn in eight steps) for the player to follow
                for (ServerPlayer p : fighters(boss, target)) {
                    Vec3 safe = shelter(boss);
                    if (p.position().distanceToSqr(safe.x, p.getY(), safe.z) > 1.6 * 1.6) {
                        CombatHandler.applyDamage(boss, p, boss.getRandom().nextInt(MoonsRules.RAY_MAX + 1));
                        addCurse(p, MoonsRules.RAY_CURSE);
                        Msg.red(p, "A searing ray blinds you!");
                    }
                }
                shield += Math.PI / 4;
            }
            Vec3 sh = ring(boss, shield, GLYPH_RADIUS);
            w.sendParticles(ParticleTypes.END_ROD, sh.x, sh.y + 1, sh.z, 6, 0.5, 0.6, 0.5, 0);
            Vec3 safe = shelter(boss);
            w.sendParticles(GLYPH_LIT, safe.x, safe.y + 0.1, safe.z, 3, 0.3, 0.02, 0.3, 0);
            return specialTick >= 16;
        }

        void startClones(DataBossEntity boss, ServerPlayer target) {
            bound = target;
            cloneStep = 0;
            clone = null;
            cloneNext = now + 2;
            int ticks = BossMechanics.ECLIPSE_CLONE_CYCLES * BossMechanics.ECLIPSE_CLONES_PER_CYCLE * (BossMechanics.ECLIPSE_CLONE_INTERVAL + 1)
                    + 2 * BossMechanics.ECLIPSE_CLONE_CYCLES + 4;
            BossFx.stun(target, ticks);
            BossFx.warn(boss, "The Eclipse Moon binds you in place as her clones surround you... face each one to parry it!");
        }

        private void endClones() {
            if (bound != null) {
                BossFx.unstun(bound);
            }
            bound = null;
            clone = null;
            cloneStep = -1;
        }

        private boolean tickClones(DataBossEntity boss) {
            if (cloneStep < 0) {
                return true;
            }
            if (bound == null || !bound.isAlive() || !boss.isAlive() || bound.distanceToSqr(boss) > 32 * 32) {
                endClones();
                return true;
            }
            if (clone != null) {
                BossFx.world(boss).sendParticles(ParticleTypes.LARGE_SMOKE, clone.x, clone.y + 1.0, clone.z, 4, 0.2, 0.6, 0.2, 0);
                BossFx.world(boss).sendParticles(ParticleTypes.END_ROD, clone.x, clone.y + 1.8, clone.z, 1, 0.1, 0.1, 0.1, 0);
            }
            if (now < cloneNext) {
                return false;
            }
            if (clone == null) {
                double a = boss.getRandom().nextInt(8) * Math.PI / 4;
                clone = BossFx.ground(BossFx.world(boss), bound.getX() + Math.cos(a) * 2.5, bound.getZ() + Math.sin(a) * 2.5, bound.getY());
                cloneNext = now + BossMechanics.ECLIPSE_CLONE_INTERVAL;
                return false;
            }
            resolveClone(boss, bound, clone);
            clone = null;
            cloneStep++;
            if (cloneStep >= BossMechanics.ECLIPSE_CLONE_CYCLES * BossMechanics.ECLIPSE_CLONES_PER_CYCLE) {
                BossFx.say(boss, "The Eclipse Moon's clones fade away.");
                endClones();
                return true;
            }
            cloneNext = now + (cloneStep % BossMechanics.ECLIPSE_CLONES_PER_CYCLE == 0 ? 3 : 1);
            return false;
        }

        /** A clone attacks: parried (the player faces it) for a free, always-hitting melee hit on the Moon, else it hits. */
        boolean resolveClone(DataBossEntity boss, ServerPlayer p, Vec3 at) {
            if (BossMechanics.faces(p.getYRot(), at.x - p.getX(), at.z - p.getZ())) {
                AttackType type = Equipment.meleeType(p, Players.data(p).skills());
                com.runecraft.osrs.PlayerHitCalc.Input in = HitCalcBridge.input(p, boss, type, null, 0);
                int max = Math.max(0, HitCalcBridge.maxHit(p, boss, in));
                int damage = p.getRandom().nextInt(max + 1);
                parrying = true;
                try {
                    damage = boss.modifyIncoming(p, damage, type);
                } finally {
                    parrying = false;
                }
                int dealt = CombatHandler.applyDamage(p, boss, damage);
                CombatHandler.awardXp(p, Players.data(p).style().forMelee(), dealt, false);
                BossFx.world(boss).sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y + 1.0, at.z, 1, 0, 0, 0, 0);
                Msg.action(p, "You parry the clone and strike back at the Eclipse Moon!");
                return true;
            }
            AttackDef c = attack(boss, "moonlight clones");
            int max = c == null ? 28 : maxHit(boss, c);
            CombatHooks.nameAttack(Special.CLONES.key, null);
            CombatHandler.applyDamage(boss, p, p.getRandom().nextInt(max + 1));
            return false;
        }

        @Override
        void cleanUp(DataBossEntity boss) {
            endClones();
            sheltered = null;
        }

        int selfTestCloneStep() {
            return cloneStep;
        }

        Vec3 selfTestClone() {
            return clone;
        }
    }

    // =====================================================================================================
    // The Blood Moon
    // =====================================================================================================

    /** Blood Moon: -2 flat armour, healing off every hit, Blood Rain and Blood Jaguars. */
    static final class BloodMoon extends Moon {
        private record Pool(Vec3 pos, long armedAt, long goneAt, int max) {
        }

        private final List<Pool> pools = new ArrayList<>();
        private final List<Mob> jaguars = new ArrayList<>();
        private int restored;
        private boolean vanished;

        BloodMoon() {
            super(MoonsRules.BLOOD);
        }

        @Override
        AttackDef standard(DataBossEntity boss) {
            AttackDef a = attack(boss, "macuahuitl flurry");
            return a != null ? a : boss.def().attacks.get(0);
        }

        @Override
        public boolean isVulnerable(DataBossEntity boss) {
            return !vanished;
        }

        @Override
        void standardLanded(DataBossEntity boss, ServerPlayer p, int index, int dealt) {
            super.standardLanded(boss, p, index, dealt);
            int heal = MoonsRules.bloodHeal(index, dealt);
            if (heal > 0) {
                boss.heal(mcHp(boss, heal));
                BossFx.world(boss).sendParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY() + 1, p.getZ(), 4, 0.2, 0.3, 0.2, 0.05);
            }
        }

        /** Damage from one of her mechanics, healing her {@code mult} times what it dealt. */
        private void bleed(DataBossEntity boss, ServerPlayer p, int max, int mult, String name) {
            CombatHooks.nameAttack(name, null);
            int dealt = CombatHandler.applyDamage(boss, p, boss.getRandom().nextInt(max + 1));
            if (dealt > 0) {
                boss.heal(mcHp(boss, dealt * mult));
            }
        }

        @Override
        void startSpecial(DataBossEntity boss, ServerPlayer target, Special s) {
            pools.clear();
            if (s == Special.BLOOD_RAIN) {
                vanished = true;
                boss.setInvisible(true);
                BossFx.say(boss, "The Blood Moon vanishes and blood rains from above!");
                return;
            }
            restored = 0;
            bleed(boss, target, MoonsRules.JAGUAR_SUMMON_MAX, 1, Special.BLOOD_JAGUARS.key);
            Vec3 c = homeVec();
            for (int i = 0; i < MoonsRules.JAGUARS; i++) {
                // between two sigils, a little further out than the glyph ring
                double a = (i + 0.5) * Math.PI * 2 / MoonsRules.JAGUARS;
                Vec3 at = BossFx.ground(BossFx.world(boss), c.x + Math.sin(a) * (GLYPH_RADIUS + 2.5), c.z - Math.cos(a) * (GLYPH_RADIUS + 2.5), c.y);
                Mob j = BossFx.spawnMinion(boss, JAGUAR, at, null);
                if (j == null) {
                    continue;
                }
                j.setNoAi(true);
                j.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, c);
                jaguars.add(j);
                MinionRules.set(j, new MinionRules.Rule() {
                    @Override
                    public int damage(ServerPlayer p, LivingEntity mob, AttackType type, int d) {
                        int back = Math.min(d, MoonsRules.JAGUAR_RESTORE_MAX - restored);
                        if (back > 0) {
                            restored += back;
                            p.heal(back);
                        }
                        return d;
                    }
                });
            }
            BossFx.say(boss, "The Blood Moon draws your blood to summon her jaguars! Stand on the glyph and strike its jaguar!");
        }

        @Override
        boolean tickSpecial(DataBossEntity boss, ServerPlayer target) {
            ServerLevel w = BossFx.world(boss);
            List<ServerPlayer> here = fighters(boss, target);
            if (special == Special.BLOOD_RAIN) {
                if (specialTick % 2 == 1) {
                    Vec3 c = homeVec();
                    for (int i = 0; i < 4; i++) {
                        pools.add(new Pool(around(boss, c, 9), now + 2, now + 7, MoonsRules.RAIN_POOL_MAX));
                    }
                    for (ServerPlayer p : here) {
                        pools.add(new Pool(p.position(), now + 2, now + 7, MoonsRules.RAIN_POOL_MAX));
                    }
                }
                boolean any = tickPools(boss, here, Special.BLOOD_RAIN.key, 2);
                if (!any && specialTick % 2 == 0) {
                    boss.setHealth(Math.max(1f, boss.getHealth() - mcHp(boss, 1))); // approx: chip damage while dodged
                }
                return specialTick >= MoonsRules.RAIN_TICKS;
            }
            // Blood Jaguars: pools under the player every 3 ticks (seven sets), the jaguars cleave every 4 (six times)
            if (specialTick % 3 == 1 && specialTick / 3 < MoonsRules.JAGUAR_POOL_SETS) {
                for (ServerPlayer p : here) {
                    pools.add(new Pool(p.position(), now + 2, now + 3, MoonsRules.JAGUAR_POOL_MAX));
                }
            }
            tickPools(boss, here, "blood_pool", 2);
            if (specialTick % 4 == 0) {
                Vec3 c = homeVec();
                for (Mob j : jaguars) {
                    if (!j.isAlive()) {
                        continue;
                    }
                    Vec3 front = j.position().add(c.subtract(j.position()).multiply(1, 0, 1).normalize().scale(1.5));
                    w.sendParticles(ParticleTypes.SWEEP_ATTACK, front.x, front.y + 0.6, front.z, 2, 0.6, 0.2, 0.6, 0);
                    for (ServerPlayer p : here) {
                        if (p.position().distanceToSqr(front.x, p.getY(), front.z) <= 1.3 * 1.3) {
                            bleed(boss, p, MoonsRules.JAGUAR_CLEAVE_MAX, 3, "jaguar_cleave");
                        }
                    }
                }
            }
            return specialTick >= 4 * MoonsRules.JAGUAR_ATTACKS;
        }

        /** Blood pools: armed after their warning, they hit (and heal her {@code mult}x) whoever stands in them. */
        private boolean tickPools(DataBossEntity boss, List<ServerPlayer> here, String name, int mult) {
            ServerLevel w = BossFx.world(boss);
            pools.removeIf(pool -> now >= pool.goneAt());
            boolean anyHit = false;
            for (Pool pool : pools) {
                boolean armed = now >= pool.armedAt();
                w.sendParticles(armed ? ParticleTypes.DAMAGE_INDICATOR : ParticleTypes.FALLING_LAVA, pool.pos().x, pool.pos().y + 0.1,
                        pool.pos().z, armed ? 2 : 1, 0.3, 0.02, 0.3, 0);
                if (!armed) {
                    continue;
                }
                for (ServerPlayer p : here) {
                    if (p.position().distanceToSqr(pool.pos().x, p.getY(), pool.pos().z) <= 0.9 * 0.9) {
                        bleed(boss, p, pool.max(), mult, name);
                        anyHit = true;
                    }
                }
            }
            return anyHit;
        }

        @Override
        void cleanUp(DataBossEntity boss) {
            jaguars.forEach(Mob::discard);
            jaguars.clear();
            pools.clear();
            if (vanished) {
                vanished = false;
                boss.setInvisible(false);
            }
        }

        List<Mob> selfTestJaguars() {
            return jaguars;
        }

        /** Hitpoints the player has won back off her jaguars this special. */
        int selfTestRestored() {
            return restored;
        }

        /** Armed pools and pools still warning, by position (self-test: a safe spot away from them). */
        List<Vec3> selfTestPools() {
            List<Vec3> out = new ArrayList<>();
            for (Pool p : pools) {
                out.add(p.pos());
            }
            return out;
        }
    }

    // =====================================================================================================
    // Self-tests
    // =====================================================================================================

    /**
     * Self-test (boss:eclipse_clones): a clone the player faces is parried and damages the Moon; one behind them hits the
     * player.
     */
    static String selfTestEclipse(DataBossEntity boss, ServerPlayer p) {
        if (!(boss.script() instanceof EclipseMoon e)) {
            throw new IllegalStateException("the Eclipse Moon has no EclipseMoon script");
        }
        e.gameTick(boss, com.runecraft.action.ActionManager.gameTick());
        e.startClones(boss, p);
        try {
            p.setYRot(0f); // facing +z
            Vec3 ahead = p.position().add(0, 0, 2.5);
            Vec3 behind = p.position().add(0, 0, -2.5);
            if (!e.resolveClone(boss, p, ahead)) {
                throw new IllegalStateException("a clone in front of the player wasn't parried");
            }
            if (e.resolveClone(boss, p, behind)) {
                throw new IllegalStateException("a clone behind the player was parried");
            }
            return "clone ahead parried, clone behind hit";
        } finally {
            e.endClones();
        }
    }
}
