package com.runecraft.content.bosses;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.network.HudNet;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.RealReaction;
import com.runecraft.osrs.content.bosses.NightmareRules;
import com.runecraft.osrs.content.bosses.WhispererRules;
import com.runecraft.selftest.RealPlayer;
import com.runecraft.util.Inv;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The no-heal pass's boss plans (round 44 playability, {@code boss:noheal:<id>} in the self-test): what a player answers in each
 * fight beyond what the shared real player ({@link RealPlayer}: protection prayers on the cues, stepping out of ground strikes,
 * hazards and danger marks, eating and drinking from a fixed budget) does by itself. Each plan sets out the supplies the fight
 * is played with (an anti-dragon shield and antifire for Vorkath, an antivenom for the snakes) and, every game tick, the extra
 * answers: crumbling Vorkath's spawn, outrunning the Hydra's currents and the Hunllef's tornadoes, the Nightmare's totems,
 * adds and flowers, the Whisperer's specials through the Shadow Realm, Sol Heredit's grapples. The player is mortal: a hit
 * that would kill it fails the check (a correctly reacting player must survive on the cues alone).
 */
public final class NoHealWalks {
    /** The bosses of the no-heal pass. */
    public static final List<String> BOSSES = List.of("vorkath", "zulrah", "alchemical_hydra", "cerberus", "the_nightmare",
            "vardorvis", "the_whisperer", "sol_heredit", "crystalline_hunllef");

    private NoHealWalks() {
    }

    /** The fight the plan plays in: the walk driver (the world's ticks, walking) plus the player's attacks. */
    public interface Fight extends BossWalks.Driver {
        RealPlayer real();

        /** Game ticks fought so far. */
        int tick();

        /** Whether the player's next attack is ready (one every few game ticks, its weapon's speed). */
        boolean attackReady();

        /**
         * One of the player's attacks at {@code target} when it is ready: a hit of {@code damage} through the target's own rules
         * (immunities, shields). False when the attack isn't ready yet.
         */
        boolean attack(LivingEntity target, int damage, AttackType type);

        /** An attack whose damage the caller worked out through the target's own rule (the Nightmare's totems). */
        boolean attackRaw(LivingEntity target, int damage);
    }

    /** What a fight adds to the shared real player. */
    public interface Plan {
        /** Supplies beyond the food budget, worn or applied before the fight. */
        default void gear(ServerPlayer p) {
        }

        /** Blocks south of the boss the player starts at. */
        default int startDistance() {
            return 6;
        }

        /** Game ticks the fight lasts at most (the player's attacks are paced to kill the boss in about this time). */
        default int fightTicks() {
            return 200;
        }

        default void start(DataBossEntity boss, Fight f) {
        }

        /** Every game tick, after the world's: the fight's own answers (adds, chasers, specials). */
        default void gameTick(DataBossEntity boss, Fight f) {
        }

        /** Whether the player attacks the boss itself this game tick (false while it must not, or the plan's adds come first). */
        default boolean drive(DataBossEntity boss, Fight f) {
            return true;
        }

        /** The attack styles the player uses on the boss, the first it takes hitpoints from. */
        default AttackType[] styles(DataBossEntity boss) {
            return new AttackType[]{AttackType.MAGIC, AttackType.RANGED, AttackType.SLASH};
        }

        default void close(ServerPlayer p) {
        }

        /** What the plan did (for the check's line). */
        default String note() {
            return "";
        }
    }

    /** The plan of a boss (the shared player alone for a boss with none of its own). */
    public static Plan plan(String id) {
        return switch (id) {
            case "vorkath" -> new Vorkath();
            case "zulrah" -> new Plan() {
                @Override
                public void gear(ServerPlayer p) {
                    antivenom(p);
                }
            };
            case "alchemical_hydra" -> new Hydra();
            case "the_nightmare" -> new Nightmare();
            case "the_whisperer" -> new Whisperer();
            case "sol_heredit" -> new Sol();
            case "crystalline_hunllef" -> new Hunllef();
            default -> new Plan() {
            };
        };
    }

    private static void antivenom(ServerPlayer p) {
        com.runecraft.combat.PoisonService.antiVenom(p, 6000, 6000, ActionManager.gameTick());
    }

    /** Game ticks between noticing something and acting on it: the reaction time, rounded up to the next game tick. */
    private static final int REACT_TICKS = (RealReaction.REACTION_MC + ActionManager.MC_TICKS_PER_GAME_TICK - 1)
            / ActionManager.MC_TICKS_PER_GAME_TICK;

    // ------------------------------------------------------------------ Vorkath

    /**
     * Vorkath: an anti-dragon shield and antifire (his dragonfire does nothing through both), an antivenom against the venom
     * fire; the zombified spawn is crumbled (one cast) a reaction time after it appears, while the ice holds the player.
     */
    private static final class Vorkath implements Plan {
        private long spawnSeen = -1;
        private int spawns;

        @Override
        public void gear(ServerPlayer p) {
            p.setItemSlot(EquipmentSlot.OFFHAND, Inv.stack("anti_dragon_shield", 1));
            p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 60 * 10, 0, false, false));
            antivenom(p);
        }

        @Override
        public void gameTick(DataBossEntity boss, Fight f) {
            if (!(boss.script() instanceof VorkathScript v)) {
                return;
            }
            Mob spawn = v.spawn();
            if (spawn == null || !spawn.isAlive()) {
                spawnSeen = -1;
                return;
            }
            if (spawnSeen < 0) {
                spawnSeen = f.tick();
            } else if (f.tick() - spawnSeen >= REACT_TICKS
                    && f.attack(spawn, (int) Math.ceil(spawn.getHealth()) + 1, AttackType.MAGIC)) { // crumble undead
                spawns++;
            }
        }

        @Override
        public boolean drive(DataBossEntity boss, Fight f) {
            return !(boss.script() instanceof VorkathScript v) || v.spawn() == null || !v.spawn().isAlive();
        }

        @Override
        public void close(ServerPlayer p) {
            p.removeEffect(MobEffects.FIRE_RESISTANCE);
            p.setItemSlot(EquipmentSlot.OFFHAND, net.minecraft.world.item.ItemStack.EMPTY);
        }

        @Override
        public String note() {
            return spawns + " zombified spawn(s) crumbled";
        }
    }

    // ------------------------------------------------------------------ the Alchemical Hydra

    /** The Hydra: its lightning currents and tracking flame are outrun (a clear spot away from them once one comes near). */
    private static final class Hydra implements Plan {
        private int flees;

        @Override
        public void gameTick(DataBossEntity boss, Fight f) {
            if (!(boss.script() instanceof SlayerScripts.Hydra h)) {
                return;
            }
            fleeFrom(f, h.selfTestChasers(), 4.0, 6.0);
        }

        private void fleeFrom(Fight f, List<Vec3> chasers, double near, double clear) {
            ServerPlayer p = f.player();
            for (Vec3 c : chasers) {
                if (Math.hypot(c.x - p.getX(), c.z - p.getZ()) < near) {
                    Vec3 spot = f.real().clearSpot(chasers, clear, 12);
                    if (spot != null) {
                        f.real().goTo(spot);
                        flees++;
                    }
                    return;
                }
            }
        }

        @Override
        public String note() {
            return flees + " run(s) from its currents";
        }
    }

    // ------------------------------------------------------------------ the Nightmare

    /**
     * The Nightmare: woken, her shields hit, her totems charged in her totem phases, her husks, parasites and sleepwalkers
     * killed as they come (a reaction time after they appear, one attack each), and the safe quadrant walked to while her
     * flowers bloom (an attack from a berry quadrant heals her, standing in one hurts).
     */
    private static final class Nightmare implements Plan {
        private final Map<LivingEntity, Integer> seen = new LinkedHashMap<>();
        private int adds;
        private int totemHits;
        private int flowerWalks;

        @Override
        public int startDistance() {
            return 7;
        }

        @Override
        public int fightTicks() {
            return 300;
        }

        @Override
        public void gameTick(DataBossEntity boss, Fight f) {
            if (!(boss.script() instanceof NightmareScript n)) {
                return;
            }
            if (n.stage == NightmareScript.Stage.ASLEEP) {
                n.wake(boss); // the first attack disturbs her
                return;
            }
            ServerPlayer p = f.player();
            if (n.flowersBloomed()) {
                Vec3 c = boss.position();
                if (NightmareRules.quadrant(p.getX() - c.x, p.getZ() - c.z) != n.flowerSafe && !f.real().moving()) {
                    double sx = n.flowerSafe == 0 || n.flowerSafe == 3 ? 5 : -5;
                    double sz = n.flowerSafe == 0 || n.flowerSafe == 1 ? -5 : 5;
                    f.real().goTo(new Vec3(c.x + sx, p.getY(), c.z + sz));
                    flowerWalks++;
                }
            }
            List<Mob> targets = new ArrayList<>(n.husks);
            for (NightmareScript.Parasite par : n.parasites) {
                targets.add(par.mob());
            }
            targets.addAll(n.walkers);
            for (Mob m : targets) {
                if (m == null || !m.isAlive()) {
                    continue;
                }
                int first = seen.computeIfAbsent(m, k -> f.tick());
                if (f.tick() - first >= REACT_TICKS) {
                    if (f.attack(m, (int) Math.ceil(m.getHealth()) + 1, AttackType.MAGIC)) {
                        adds++;
                    }
                    return;
                }
            }
            if (n.stage == NightmareScript.Stage.TOTEMS) {
                for (Mob t : n.totems) {
                    if (t.isAlive() && f.attackRaw(t, n.totemDamage(p, t, AttackType.MAGIC, 40))) {
                        totemHits++;
                        return;
                    }
                }
            }
        }

        @Override
        public boolean drive(DataBossEntity boss, Fight f) {
            if (!(boss.script() instanceof NightmareScript n)) {
                return true;
            }
            if (n.stage != NightmareScript.Stage.SHIELD && n.stage != NightmareScript.Stage.DESPERATION) {
                return false;
            }
            if (n.flowersBloomed()) {
                Vec3 c = boss.position();
                ServerPlayer p = f.player();
                return NightmareRules.quadrant(p.getX() - c.x, p.getZ() - c.z) == n.flowerSafe;
            }
            return true;
        }

        @Override
        public AttackType[] styles(DataBossEntity boss) {
            return new AttackType[]{AttackType.CRUSH, AttackType.MAGIC, AttackType.RANGED};
        }

        @Override
        public void close(ServerPlayer p) {
            NightmareScript.CURSED.remove(p.getUUID());
            NightmareScript.CHARGING.remove(p.getUUID());
            NightmareScript.SLOWED.remove(p.getUUID());
        }

        @Override
        public String note() {
            return adds + " adds killed, " + totemHits + " totem hits, " + flowerWalks + " walk(s) to the safe quadrant";
        }
    }

    // ------------------------------------------------------------------ the Whisperer

    /**
     * The Whisperer: her specials as her phase walk answers them (BossWalks: the seeds stepped on from memory, the souls killed
     * from the Shadow Realm, the screech hidden from behind the weakest column), then back to the fight on foot.
     */
    private static final class Whisperer implements Plan {
        private final List<String> log = new ArrayList<>();

        @Override
        public void gear(ServerPlayer p) {
            Inv.give(p, WhispererRules.FRAGMENT, 1);
        }

        @Override
        public int startDistance() {
            return 8;
        }

        @Override
        public void gameTick(DataBossEntity boss, Fight f) {
            if (!(boss.script() instanceof WhispererScript w) || log.size() > 8) {
                return;
            }
            String special = w.selfTestSpecialName();
            if (special == null) {
                return;
            }
            log.add(switch (special) {
                case "seeds" -> BossWalks.walkSeeds(boss, w, f);
                case "siphon" -> BossWalks.walkSiphon(boss, w, f);
                default -> BossWalks.walkScreech(boss, w, f);
            });
            if (w.inRealm(f.player())) {
                WhispererScript.activateFragment(f.player()); // out of the realm again
            }
            f.walkTo(w.homeVec().add(0, 0, -8), boss);
        }

        @Override
        public void close(ServerPlayer p) {
            Inv.remove(p, WhispererRules.FRAGMENT, Inv.count(p, WhispererRules.FRAGMENT));
        }

        @Override
        public String note() {
            return String.join("; ", log);
        }
    }

    // ------------------------------------------------------------------ Sol Heredit

    /**
     * Sol Heredit: his spear and shield are dodged by the shared player (the safe tiles they show); a grapple is parried on the
     * called slot's key a reaction time after its prompt.
     */
    private static final class Sol implements Plan {
        private Consumer<HudNet.SentGrapple> before;
        private long answerAt = -1;
        private int slot = -1;
        private int parried;

        @Override
        public int startDistance() {
            return 2;
        }

        @Override
        public void start(DataBossEntity boss, Fight f) {
            ServerPlayer p = f.player();
            before = HudNet.grappleListener;
            Consumer<HudNet.SentGrapple> was = before;
            HudNet.grappleListener = g -> {
                was.accept(g);
                if (g.player() == p && g.called() >= 0) {
                    slot = g.called();
                    answerAt = ActionManager.mcTick() + RealReaction.REACTION_MC;
                }
            };
        }

        @Override
        public void gameTick(DataBossEntity boss, Fight f) {
            if (answerAt >= 0 && ActionManager.mcTick() >= answerAt) {
                answerAt = -1;
                if (HudNet.answerGrapple(f.player(), slot)) {
                    parried++;
                }
            }
        }

        @Override
        public void close(ServerPlayer p) {
            if (before != null) {
                HudNet.grappleListener = before;
            }
        }

        @Override
        public String note() {
            return parried + " grapple(s) parried";
        }
    }

    // ------------------------------------------------------------------ the Crystalline Hunllef

    /** The Hunllef: its tornadoes are outrun; the player attacks with a style its prayer doesn't protect. */
    private static final class Hunllef implements Plan {
        private int flees;

        @Override
        public int fightTicks() {
            return 150;
        }

        @Override
        public void gameTick(DataBossEntity boss, Fight f) {
            EntityType<?> type = com.runecraft.registry.ModEntities.typeById("hunllef_tornado");
            if (type == null) {
                return;
            }
            List<Vec3> at = new ArrayList<>();
            for (net.minecraft.world.entity.Entity e : boss.level().getEntities(type, new AABB(boss.blockPosition()).inflate(32), e -> e.isAlive())) {
                at.add(e.position());
            }
            ServerPlayer p = f.player();
            for (Vec3 t : at) {
                if (Math.hypot(t.x - p.getX(), t.z - p.getZ()) < 4.0) {
                    Vec3 spot = f.real().clearSpot(at, 6.0, 12);
                    if (spot != null) {
                        f.real().goTo(spot);
                        flees++;
                    }
                    return;
                }
            }
        }

        @Override
        public AttackType[] styles(DataBossEntity boss) {
            BossSelfTests.HunllefView v = BossSelfTests.hunllef(boss);
            AttackType prays = v == null ? null : v.prays();
            List<AttackType> out = new ArrayList<>();
            for (AttackType t : new AttackType[]{AttackType.MAGIC, AttackType.RANGED, AttackType.SLASH}) {
                boolean same = prays != null && (t == prays || (t.isMelee() && prays.isMelee()));
                if (!same) {
                    out.add(t);
                }
            }
            return out.toArray(new AttackType[0]);
        }

        @Override
        public String note() {
            return flees + " run(s) from its tornadoes";
        }
    }
}
