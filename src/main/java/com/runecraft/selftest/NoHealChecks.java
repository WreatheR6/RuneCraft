package com.runecraft.selftest;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.boss.Mechanics;
import com.runecraft.combat.CombatHandler;
import com.runecraft.content.bosses.NoHealWalks;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.skill.XpService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The no-heal pass (round 44 playability, {@code boss:noheal:<id>}): a representative set of bosses ({@link NoHealWalks#BOSSES})
 * fought by a player who is NOT invulnerable and is never topped up, with a fixed budget of {@value #FOOD} sharks and
 * {@value #PRAYER_POTIONS} prayer potions eaten and drunk through the quick-eat and quick-drink keys. It reacts like a real
 * player ({@link RealPlayer}: protection prayers a reaction time after each cue, ground strikes, hazards and danger marks walked
 * out of at a run) and does what each fight asks beyond that ({@link NoHealWalks.Plan}); its attacks take the boss down at a
 * player's pace, through the boss's own damage rules. A hit that would have killed it fails the check: the cues alone must
 * keep a correctly reacting player alive. Only the real player can take damage, so the checks skip on the fake one.
 */
final class NoHealChecks {
    static final int FOOD = 10;
    static final int PRAYER_POTIONS = 4;
    private static final int MC = ActionManager.MC_TICKS_PER_GAME_TICK;
    /** The player's attack speed (game ticks), a 2.4 s weapon. */
    private static final int PLAYER_SPEED = 4;
    /** Game ticks to wait for a boss's next form after one went (a dive, a phase swap). */
    private static final int FORM_WAIT = 20;
    /** The longest walk one step of a plan may take (MC). */
    private static final int WALK_MC = 200;

    private final TestContext c;

    private NoHealChecks(TestContext c) {
        this.c = c;
    }

    static List<SelfTest.Check> all(TestContext context) {
        NoHealChecks n = new NoHealChecks(context);
        List<SelfTest.Check> checks = new ArrayList<>();
        for (String id : NoHealWalks.BOSSES) {
            checks.add(new SelfTest.Check("boss:noheal:" + id, () -> n.fight(id)));
        }
        return checks;
    }

    private AABB arena() {
        return new AABB(TestContext.PLATFORM).inflate(40);
    }

    private String fight(String id) {
        if (!c.realPlayer()) {
            throw new SelfTest.Skip("the fake player takes no damage; the no-heal pass runs in '/rc selftest real boss:noheal'");
        }
        BossDef def = ContentRegistry.boss(id);
        if (def == null) {
            throw new AssertionError("no BossDef " + id);
        }
        ServerPlayer p = c.player;
        PlayerData data = Players.data(p);
        NoHealWalks.Plan plan = NoHealWalks.plan(id);
        XpService.updateMaxHealth(p);
        p.setHealth(p.getMaxHealth());
        data.setPrayerPoints(99);
        plan.gear(p);
        DataBossEntity first = DataBossService.spawn(c.world, c.near(0, 0), def, p);
        if (first == null) {
            throw new AssertionError("DataBossService.spawn returned null for " + id);
        }
        Vec3 start = Vec3.atBottomCenterOf(c.near(0, plan.startDistance()));
        float yaw = (float) (Math.toDegrees(Math.atan2(first.getZ() - start.z, first.getX() - start.x)) - 90.0);
        p.teleportTo(c.world, start.x, start.y, start.z, yaw, 0);
        RealPlayer real = RealPlayer.start(p).autoPray(true).autoDodge(true).noHeal(FOOD, PRAYER_POTIONS);
        Fight f = new Fight(p, real, first);
        int kcBefore = data.killCount(id);
        String outcome;
        try {
            plan.start(first, f);
            outcome = f.run(plan, id, kcBefore);
            real.assertFair("boss:noheal:" + id);
        } catch (IllegalStateException e) {
            throw new AssertionError(e.getMessage() + " (" + real.summary() + ")", e);
        } finally {
            real.close();
            plan.close(p);
            for (LivingEntity e : c.world.getEntitiesOfClass(LivingEntity.class, arena(), e -> !(e instanceof ServerPlayer))) {
                e.discard();
            }
            c.world.getEntitiesOfClass(ItemEntity.class, arena(), e -> true).forEach(ItemEntity::discard);
            for (Prayer pr : Prayer.values()) {
                if (data.isActive(pr)) {
                    com.runecraft.combat.PrayerService.set(p, pr, false);
                }
            }
            com.runecraft.combat.PoisonService.clear(p);
            p.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN); // a freeze or bind left on
            XpService.updateMaxHealth(p);
            p.setHealth(p.getMaxHealth());
            c.home();
        }
        String note = plan.note();
        return outcome + (note.isEmpty() ? "" : "; " + note) + "; " + real.summary();
    }

    /** The fight: the world's Minecraft ticks with the real player in them, the plan's answers and the player's attacks. */
    private final class Fight implements NoHealWalks.Fight {
        private final ServerPlayer p;
        private final RealPlayer real;
        private DataBossEntity boss;
        private final Set<UUID> forms = new HashSet<>();
        private final List<String> formLog = new ArrayList<>();
        private int tick;
        private int nextAttack;
        private final Map<String, Integer> damageMark = new HashMap<>();
        private final Map<String, Integer> biggest = new HashMap<>();
        private int attacks;

        Fight(ServerPlayer p, RealPlayer real, DataBossEntity boss) {
            this.p = p;
            this.real = real;
            this.boss = boss;
            forms.add(boss.getUUID());
        }

        /** One Minecraft tick of the world: the clock, the boss (its form now), the pending hits and hazards, the player. */
        private void mc() {
            ActionManager.advanceForSelfTest();
            DataBossEntity b = boss;
            if (b != null && b.isAlive()) {
                if (b.getTarget() != p) {
                    b.setTarget(p);
                }
                b.tickCount++; // as ServerLevel.tickNonPassenger does (see BossFightChecks)
                b.tick();
            }
            Mechanics.tickForSelfTest();
            real.tick();
        }

        String run(NoHealWalks.Plan plan, String id, int kcBefore) {
            int fightTicks = plan.fightTicks();
            int planned = Math.max(4, fightTicks / PLAYER_SPEED - 6);
            double boost = 1.0;
            int waiting = 0;
            boolean killed = false;
            for (; tick < fightTicks + 40; tick++) {
                for (int m = 0; m < MC; m++) {
                    mc();
                }
                if (real.death() != null) {
                    break;
                }
                if (boss == null || !boss.isAlive() || boss.isRemoved()) {
                    if (Players.data(p).killCount(id) > kcBefore) {
                        killed = true;
                        break;
                    }
                    DataBossEntity next = nextForm();
                    if (next == null) {
                        if (++waiting > FORM_WAIT) {
                            // gone with no form after it: the fight is over (its kill count may go under another name)
                            killed = true;
                            break;
                        }
                        continue;
                    }
                    waiting = 0;
                    formLog.add(name(boss) + "->" + name(next) + "@" + tick);
                    boss = next;
                    forms.add(next.getUUID());
                }
                plan.gameTick(boss, this);
                if (real.death() != null) {
                    break;
                }
                if (!attackReady() || !plan.drive(boss, this) || !boss.script().isVulnerable(boss)) {
                    continue;
                }
                // the player's attack on the boss: a share of its hitpoints that ends it in about the fight's time
                double share = 1.15 / planned;
                float before = boss.getHealth();
                int want = Math.max(1, (int) Math.ceil(boss.trueHitpoints() * share * boost));
                boolean hit = false;
                for (AttackType type : plan.styles(boss)) {
                    int dmg = boss.modifyIncoming(p, want, type);
                    if (dmg > 0) {
                        CombatHandler.applyDamage(p, boss, dmg);
                        nextAttack = tick + PLAYER_SPEED;
                        attacks++;
                        hit = true;
                        break;
                    }
                }
                if (hit) {
                    double took = (before - Math.max(0, boss.getHealth())) / Math.max(1f, boss.getMaxHealth());
                    // a shield or a damage cap took most of it: harder next time; well over: back down
                    boost = took < share / 3 ? Math.min(8, boost * 2) : took > share * 1.5 ? Math.max(1, boost / 2) : boost;
                }
            }
            if (real.death() != null) {
                return "died"; // assertFair reports it
            }
            String hp = boss != null && boss.isAlive() ? String.format(Locale.ROOT, ", %s at %.0f%%", name(boss),
                    boss.getHealth() * 100 / Math.max(1f, boss.getMaxHealth())) : "";
            return (killed ? "killed" : "survived") + " in " + tick + " game ticks (" + attacks + " attacks on it" + hp + ")"
                    + (formLog.isEmpty() ? "" : ", forms " + String.join(" ", formLog));
        }

        private DataBossEntity nextForm() {
            for (DataBossEntity e : c.world.getEntitiesOfClass(DataBossEntity.class, arena(), e -> e.isAlive() && !e.isRemoved())) {
                if (!forms.contains(e.getUUID())) {
                    return e;
                }
            }
            return null;
        }

        private String name(LivingEntity e) {
            return e instanceof DataBossEntity d && d.def() != null ? d.def().id : e == null ? "?" : e.getName().getString();
        }

        // ------------------------------------------------------------------ NoHealWalks.Fight

        @Override
        public RealPlayer real() {
            return real;
        }

        @Override
        public int tick() {
            return tick;
        }

        @Override
        public boolean attackReady() {
            return tick >= nextAttack;
        }

        @Override
        public boolean attack(LivingEntity target, int damage, AttackType type) {
            if (!attackReady() || target == null || !target.isAlive()) {
                return false;
            }
            int d = target instanceof OsrsMonsterEntity m ? m.modifyIncoming(p, damage, type) : damage;
            CombatHandler.applyDamage(p, target, d);
            nextAttack = tick + PLAYER_SPEED;
            attacks++;
            return true;
        }

        @Override
        public boolean attackRaw(LivingEntity target, int damage) {
            if (!attackReady() || target == null || !target.isAlive()) {
                return false;
            }
            CombatHandler.applyDamage(p, target, damage);
            nextAttack = tick + PLAYER_SPEED;
            attacks++;
            return true;
        }

        // ------------------------------------------------------------------ BossWalks.Driver (for the plans' special walks)

        @Override
        public void ticks(DataBossEntity b, int n) {
            for (int i = 0; i < n * MC; i++) {
                mc();
            }
        }

        @Override
        public void mcTicks(DataBossEntity b, int n) {
            for (int i = 0; i < n; i++) {
                mc();
            }
        }

        @Override
        public ServerPlayer player() {
            return p;
        }

        @Override
        public int damage(String attack) {
            return real.taken().getOrDefault(attack, 0) - damageMark.getOrDefault(attack, 0);
        }

        @Override
        public int biggest(String attack) {
            return biggest.getOrDefault(attack, damage(attack));
        }

        @Override
        public void clearDamage() {
            damageMark.clear();
            damageMark.putAll(real.taken());
            biggest.clear();
        }

        @Override
        public void moveTo(Vec3 at, DataBossEntity b) {
            walkTo(at, b); // a mortal player is never put anywhere: it walks
        }

        @Override
        public void walkTo(Vec3 at, DataBossEntity b) {
            real.walkTo(at, WALK_MC, this::mc);
            if (b != null) {
                real.faceTowards(b.position());
            }
        }

        @Override
        public void react(Prayer prayer) {
            real.react(prayer);
        }

        @Override
        public void avoid(List<Vec3> spots, double radius) {
            real.avoid(spots, radius);
        }

        @Override
        public void clearAvoid() {
            real.clearAvoid();
        }

        @Override
        public void keep(Prayer prayer) {
            real.keep(prayer);
        }

        @Override
        public int prayerRestores() {
            return real.restores();
        }

        @Override
        public void walkOut(String name, Vec3 safe, int sinceCueMc, int landsInMc, DataBossEntity b) {
            int since = Math.max(0, sinceCueMc);
            real.dodgeBudget(name, Math.hypot(safe.x - p.getX(), safe.z - p.getZ()), since + Math.max(0, landsInMc));
            mcTicks(b, Math.max(0, com.runecraft.osrs.RealReaction.REACTION_MC - since));
            walkTo(safe, b);
        }
    }
}
