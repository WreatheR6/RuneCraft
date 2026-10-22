package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.Mechanics;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.PoisonService;
import com.runecraft.content.systems.CombatAchievementService;
import com.runecraft.content.systems.SpecialAttackService;
import com.runecraft.content.systems.SysCombat;
import com.runecraft.content.systems.SysState;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.bosses.BossMechanics;
import com.runecraft.osrs.content.bosses.BossMechanics.YamaContract;
import com.runecraft.osrs.content.bosses.ModernBosses;
import com.runecraft.osrs.content.bosses.VarlamoreBosses;
import com.runecraft.osrs.content.bosses.YamaLoot;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Scripts for Yama (with his contracts), Amoxliatl and the Maggot King. Numbers: {@link BossMechanics}. */
final class ModernScripts {
    private ModernScripts() {
    }

    /** One OSRS tile is two blocks. */
    private static final double TILE = 2.0;

    private static int tiles(double blocks) {
        return (int) Math.floor(blocks / TILE + 0.25);
    }

    private static double flat(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static void setSpeed(Mob mob, double speed) {
        var attr = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null && attr.getBaseValue() != speed) {
            attr.setBaseValue(speed);
        }
    }

    private static void hurt(DataBossEntity boss, ServerPlayer p, String attack, int damage) {
        CombatHooks.nameAttack(attack, null);
        CombatHandler.applyDamage(boss, p, Math.max(0, damage));
    }

    /**
     * A line hazard sweeping across the arena: Yama's shadow waves (jump over them; touching one on the ground deals up to
     * 20 and snuffs out protection prayers) and fire streaks (keep moving; standing still in one burns and binds you).
     */
    private static final class Sweep {
        final Vec3 start;
        final Vec3 dir;
        final double length;
        final double halfWidth;
        final double speed;
        final boolean shadow;
        final Set<UUID> passed = new HashSet<>();
        double travelled;

        Sweep(Vec3 start, Vec3 dir, double length, double halfWidth, double speed, boolean shadow) {
            this.start = start;
            this.dir = dir;
            this.length = length;
            this.halfWidth = halfWidth;
            this.speed = speed;
            this.shadow = shadow;
        }
    }

    // =====================================================================================================
    // Yama
    // =====================================================================================================

    /**
     * Yama. Phases 1 and 2: autos alternate between fire (magic) and shadow (ranged) projectiles in blocks of three between
     * specials (melee swipe through prayer if you stand next to him); each special is preceded by shadow waves or fire
     * streaks and must be negated by standing on the matching glyph (phase 2: Meteor Strike and Shadow Stomp); every second
     * shadow special summons void flares that explode and heal him unless killed. At 66% and 33% he exiles you to fight the
     * Judge of Yama, who prays against ranged and your last style. Phase 3: he stands in the centre attacking every 7 ticks,
     * Shadow Crash out of melee range, a void flare with every melee swipe. Holding a sigil contract when challenging him
     * raises the stakes (and returns the contract on a kill).
     */
    static final class Yama extends ScriptedBoss {
        private static final Map<UUID, YamaContract> PENDING = new HashMap<>();

        /**
         * Called by the summon requirement: Yama is fought after A Kingdom Divided; a contract held in the main hand is shown to
         * him and consumed. The sigil, oathplate and harmony contracts are for one challenger alone (OSRS Wiki "Yama#Contracts":
         * only the catalyst, worm and shard acquisition contracts allow a partner), so they are refused while another player
         * stands by.
         */
        static boolean offerContract(ServerPlayer player) {
            var quest = com.runecraft.osrs.content.ContentRegistry.quest(YAMA_QUEST);
            if (quest != null && !com.runecraft.player.Players.data(player).questComplete(quest)) {
                Msg.game(player, "The Voice of Yama: The Master of Pacts has no business with you. (Complete A Kingdom Divided.)");
                return false;
            }
            YamaContract c = YamaContract.byItem(BossFx.heldId(player));
            if (c != null && Inv.has(player, c.item, 1)) {
                if (!YamaLoot.duoAllowed(c) && othersNear(player)) {
                    Msg.game(player, "Yama: The terms of the " + c.displayName() + " are for you alone. Come back without company.");
                    return false;
                }
                Inv.remove(player, c.item, 1);
                PENDING.put(player.getUUID(), c);
                Msg.red(player, "Yama: " + c.displayName() + "? Very well. Let us see if you can honour these terms.");
            } else {
                PENDING.remove(player.getUUID());
            }
            return true;
        }

        private static final String YAMA_QUEST = "a_kingdom_divided";
        /** Autos before Yama may turn on the other player of a duo. */
        private static final int DUO_SWITCH_AUTOS = 6; // approx (wiki: "periodically")

        /** Another player within the lair's presence range of this one. */
        private static boolean othersNear(ServerPlayer player) {
            double range = com.runecraft.osrs.content.bosses.BossLair.PRESENCE_RANGE;
            for (ServerPlayer other : ((ServerLevel) player.level()).players()) {
                if (other != player && other.isAlive() && !other.isSpectator() && other.distanceToSqr(player) < range * range) {
                    return true;
                }
            }
            return false;
        }

        /** Each fighter's damage to Yama (their contribution, YamaLoot) and the players themselves. */
        private final Map<UUID, Integer> damageBy = new java.util.LinkedHashMap<>();
        private final Map<UUID, ServerPlayer> fighters = new java.util.LinkedHashMap<>();
        /** The pet rolls this script made on the kill, per player (1/n each; the boss engine's own roll isn't here). */
        private final Map<UUID, List<Integer>> petRolls = new HashMap<>();
        /** Who showed Yama the contract (only they receive its reward) and their name. */
        private UUID contractHost;
        private String contractHostName = "my challenger";
        /** Exile thresholds already passed (two: 66.6%/33.3%, or 75%/50% under a contract). */
        private int thresholdsPassed;
        /** Autos at the current target (in a duo he turns on the other player now and then). */
        private int autosAtTarget;

        /** Live Yama fights (for the familiar contract's rules on the fighters' food, potions and specials). */
        private static final Set<Yama> ACTIVE = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
        /** The boss this script runs. */
        private DataBossEntity self;

        /** The live Yama fight under this contract that a player is part of (in the arena, not an intruder), or null. */
        static Yama fightOf(ServerPlayer p, java.util.function.Predicate<YamaContract> contract) {
            synchronized (ACTIVE) {
                for (Yama y : ACTIVE) {
                    DataBossEntity b = y.self;
                    if (b != null && b.isAlive() && !b.isRemoved() && y.contract != null && contract.test(y.contract)
                            && p.level() == b.level() && p.distanceToSqr(b) < 32 * 32 && !y.intruder(p)) {
                        return y;
                    }
                }
            }
            return null;
        }

        /** The rewards of a kill: every fighter's own roll on their contribution; the killer's come back to drop. */
        static void register() {
            com.runecraft.boss.DataBossService.loot("yama", Yama::rollLoot);
            // The contract of familiar acquisition (OSRS Wiki): no healing or prayer restoration from consumables, doubled
            // natural special attack regeneration and special attacks that always hit.
            com.runecraft.skill.ItemUses.addConsumeGuard((player, def) -> fightOf(player, YamaContract::noConsumables) != null
                    && BossMechanics.familiarBlocks(def.id(), def.kind() == com.runecraft.osrs.ItemDef.Kind.FOOD)
                    ? "The terms of your contract forbid that." : null);
            com.runecraft.content.systems.SpecialAttackService.doubleRegen = com.runecraft.content.systems.SpecialAttackService.doubleRegen
                    .or(p -> fightOf(p, YamaContract::noConsumables) != null);
            CombatHooks.addModifier(new CombatHooks.PlayerAttackModifier() {
                @Override
                public double accuracy(ServerPlayer player, net.minecraft.world.entity.LivingEntity target, AttackType type) {
                    return SpecialAttackService.specialActive(player) && fightOf(player, YamaContract::noConsumables) != null
                            ? MinionRules.ALWAYS_HITS : 1.0;
                }
            });
            // The contract of forfeit breath (OSRS Wiki): "Yama drains run energy percentage equal to damage dealt" - any
            // damage of his, his flares' or his Judge's (the losing blow itself excepted).
            CombatHooks.addDamageListener((player, attacker, attack, style, damage) -> {
                if (damage <= 0 || attacker == null || "forfeit".equals(attack)) {
                    return;
                }
                Yama y = attacker instanceof DataBossEntity b && b.script() instanceof Yama s ? s : OWNED.get(attacker);
                if (y != null && y.contract == YamaContract.FORFEIT_BREATH && !y.intruder(player)) {
                    SysState.SysPlayer sp = SysState.of(player);
                    sp.runEnergy = BossMechanics.forfeitAfterHit(sp.runEnergy, damage);
                }
            });
            // Under a solo contract only its challenger may hurt Yama, his flares or his Judge, whatever the damage path
            // (the hit hook below refuses the usual paths before any contribution is credited).
            com.runecraft.combat.CombatHooks.addMonsterDamageGate((player, target) -> {
                Yama y = target instanceof DataBossEntity b && b.script() instanceof Yama s ? s : OWNED.get(target);
                return y != null && y.intruder(player) ? y.intruderMessage() : null;
            });
        }

        /** The void flares and Judge of each fight (for the solo-contract damage gate). */
        private static final Map<net.minecraft.world.entity.Entity, Yama> OWNED = new java.util.WeakHashMap<>();

        /**
         * A player who isn't part of this fight's terms: a solo contract (OSRS Wiki "Yama#Contracts": the sigil, oathplate
         * and harmony contracts, "Challenger must be alone") is between Yama and the player who showed it to him; anyone
         * joining after the summon can't hurt him, isn't targeted or exiled, and rolls no loot.
         */
        boolean intruder(ServerPlayer p) {
            return p != null && contract != null && !YamaLoot.duoAllowed(contract) && contractHost != null
                    && !p.getUUID().equals(contractHost);
        }

        private String intruderMessage() {
            return "Yama: These terms are between me and " + contractHostName + ".";
        }

        static List<com.runecraft.skill.Drops.Rolled> rollLoot(ServerPlayer killer, DataBossEntity boss,
                                                               List<com.runecraft.osrs.data.Drop> table) {
            Yama y = boss.script() instanceof Yama s ? s : null;
            YamaContract contract = y == null ? null : y.contract;
            java.util.Random r = new java.util.Random(boss.getRandom().nextLong());
            List<com.runecraft.skill.Drops.Rolled> mine = rolled(YamaLoot.roll(y == null ? 1.0 : y.contribution(killer), contract, r));
            if (y == null) {
                return mine;
            }
            for (ServerPlayer p : y.fighters.values()) {
                if (p == killer || y.intruder(p) || !p.isAlive() || p.level() != boss.level() || p.distanceToSqr(boss) > 48 * 48) {
                    continue;
                }
                double share = y.contribution(p);
                for (com.runecraft.skill.Drops.Rolled d : rolled(YamaLoot.roll(share, contract, r))) {
                    Inv.give(p, com.runecraft.skill.Drops.withDoses(CombatHooks.dropStack(p, boss, d.item(), d.count()), d.doses()));
                    com.runecraft.collection.CollectionLog.obtain(p, d.item());
                }
                if (!YamaLoot.junk(share)) {
                    y.rollPet(p, YamaLoot.petRate(contract), r::nextInt);
                }
                int kc = com.runecraft.player.Players.data(p).incrementKillCount("yama");
                Msg.game(p, "Your Yama success count is: " + kc + ".");
                if (boss.def() != null) {
                    com.runecraft.content.GameEvents.fireKill(p, boss, com.runecraft.combat.MonsterStats.from(boss.def()));
                }
            }
            return mine;
        }

        /** Rolls a 1/{@code rate} chance of Yami for a player (and notes the roll for the walk). */
        private void rollPet(ServerPlayer p, int rate, java.util.function.IntUnaryOperator nextInt) {
            petRolls.computeIfAbsent(p.getUUID(), k -> new ArrayList<>()).add(rate);
            if (nextInt.applyAsInt(rate) == 0) {
                givePet(p);
            }
        }

        private static void givePet(ServerPlayer p) {
            Inv.give(p, "pet_yami", 1);
            com.runecraft.collection.CollectionLog.obtain(p, "pet_yami");
            p.getServer().getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.literal(
                    p.getName().getString() + " has a funny feeling like they're being followed.")
                    .withStyle(net.minecraft.ChatFormatting.GOLD), false);
        }

        private static List<com.runecraft.skill.Drops.Rolled> rolled(List<YamaLoot.Loot> loot) {
            List<com.runecraft.skill.Drops.Rolled> out = new ArrayList<>();
            for (YamaLoot.Loot l : loot) {
                out.add(new com.runecraft.skill.Drops.Rolled(com.runecraft.osrs.data.Drop.baseOf(l.item()), l.count(),
                        com.runecraft.osrs.data.Drop.dosesOf(l.item())));
            }
            return out;
        }

        /** A fighter's share of Yama's hitpoints dealt (0-1); a killer the script never saw hit him gets full credit. */
        double contribution(ServerPlayer p) {
            Integer d = damageBy.get(p.getUUID());
            return d == null ? (damageBy.isEmpty() ? 1.0 : 0.0) : YamaLoot.contribution(d, BossMechanics.YAMA_HITPOINTS);
        }

        // ---------------- self-test access (YamaWalk) ----------------

        /** Shows Yama a contract for the player's next challenge (as holding it at the summon does). */
        static void walkOffer(ServerPlayer p, YamaContract c) {
            PENDING.put(p.getUUID(), c);
        }

        /** Credits damage to a fighter as if they had dealt it (the duo partner). */
        void walkCredit(ServerPlayer p, int damage) {
            damageBy.merge(p.getUUID(), damage, Integer::sum);
            fighters.put(p.getUUID(), p);
        }

        YamaContract walkContract() {
            return contract;
        }

        /** The script's pet rolls on the kill for a player (1/n each), on top of the boss engine's 1/2,500 for the slayer. */
        List<Integer> walkPetRolls(ServerPlayer p) {
            return petRolls.getOrDefault(p.getUUID(), List.of());
        }

        int walkStage() {
            return stage;
        }

        boolean walkExiled() {
            return exiled;
        }

        Mob walkJudge() {
            return judge;
        }

        boolean walkExilePending() {
            return exileDelay >= 0;
        }

        String walkPendingName() {
            return pending == null ? null : pending.name();
        }

        List<Mob> walkFlareMobs() {
            flares.removeIf(f -> !f.mob().isAlive());
            List<Mob> out = new ArrayList<>();
            for (Flare f : flares) {
                out.add(f.mob());
            }
            return out;
        }

        /** The lit glyphs of an element (where a player must stand to block the matching special). */
        List<Vec3> walkGlyphs(boolean fire) {
            List<Vec3> out = new ArrayList<>();
            for (Glyph g : glyphs) {
                if (g.fire == fire && glyphReady(g)) {
                    out.add(g.pos);
                }
            }
            return out;
        }

        /** Starts his next special now (the walk doesn't wait out three autos). */
        void walkStartSpecial(DataBossEntity boss, ServerPlayer target) {
            startSpecial(boss, target);
        }

        boolean walkPendingFire() {
            return pending == Special.GLYPH_FIRE || pending == Special.METEOR;
        }

        Vec3 walkPendingAt() {
            return pendingAt;
        }

        int walkThresholdsPassed() {
            return thresholdsPassed;
        }

        void walkSpawnFlares(DataBossEntity boss, int n) {
            spawnFlares(boss, n);
        }

        private static final class Glyph {
            final Vec3 pos;
            final boolean fire;
            long readyAt;
            final Set<UUID> on = new HashSet<>();

            Glyph(Vec3 pos, boolean fire) {
                this.pos = pos;
                this.fire = fire;
            }
        }

        /** A void flare and the health it spawned with (its explosion scales with the share of that left). */
        private record Flare(Mob mob, long due, float full) {
        }

        private enum Special { GLYPH_FIRE, GLYPH_SHADOW, METEOR, STOMP }

        private YamaContract contract;
        private int stage = 1;
        /** His primary target last attacked him with magic: magic defence +60 instead of -30 (phases 1 and 2). */
        private boolean targetMaging;
        private boolean fireStyle;
        private int autosToSpecial = 1;
        private int shadowSpecials;
        private boolean meleeStance = true;
        private boolean hazardWaves;
        private final List<Glyph> glyphs = new ArrayList<>();
        private final List<Flare> flares = new ArrayList<>();
        private final List<Sweep> sweeps = new ArrayList<>();
        private final Map<UUID, Vec3> lastPos = new HashMap<>();
        private Special pending;
        private long pendingDue;
        private Vec3 pendingAt;
        // exile
        private int exileDelay = -1;
        private int exilesQueued;
        private boolean exiled;
        private long exileStart;
        private Mob judge;
        private int judgeAttacks;
        private final Map<UUID, Integer> exileHits = new HashMap<>();
        private final List<ServerPlayer> exilees = new ArrayList<>();
        // phase 3
        private int crashTimer;
        private int crashAxis;
        private long nextFlare;

        @Override
        public void onSpawn(DataBossEntity boss) {
            // The glyphs, the exile spot and the Judge are laid out around the arena centre, and he plants himself there.
            home = arenaCentre(boss);
            ServerPlayer challenger = boss.getTarget() instanceof ServerPlayer p ? p : null;
            if (challenger != null) {
                contract = PENDING.remove(challenger.getUUID());
                contractHost = contract == null ? null : challenger.getUUID();
                contractHostName = challenger.getName().getString();
            }
            self = boss;
            synchronized (ACTIVE) {
                ACTIVE.add(this);
            }
            autosToSpecial = Math.min(autosToSpecial, BossMechanics.yamaAutosBeforeSpecial(contract));
            fireStyle = boss.getRandom().nextBoolean();
            BossFx.say(boss, contract == null ? "Yama: Let us begin. Mind the glyphs, mortal."
                    : "Yama: The terms of the " + contract.displayName() + " are binding. Begin!");
        }

        @Override
        protected boolean driven() {
            return true;
        }

        @Override
        protected boolean wantsMelee(DataBossEntity boss, ServerPlayer target) {
            return meleeStance && stage < 3 && !exiled;
        }

        @Override
        public boolean isVulnerable(DataBossEntity boss) {
            return !exiled;
        }

        @Override
        protected boolean canAttack(DataBossEntity boss) {
            return !exiled && exileDelay < 0 && pending == null;
        }

        @Override
        protected double multiplier(DataBossEntity boss) {
            double m = BossMechanics.yamaHitScale(contract);
            return contract == YamaContract.BLOODIED_BLOWS ? m * BossMechanics.WRETCHED_STRENGTH : m;
        }

        @Override
        protected int attackDelay(DataBossEntity boss, AttackDef attack) {
            return contract != null || stage >= 3 ? BossMechanics.YAMA_SPEED_FAST : BossMechanics.YAMA_SPEED;
        }

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (intruder(attacker)) {
                Msg.game(attacker, intruderMessage()); // nothing lands and nothing counts toward a share of the loot
                return 0;
            }
            if (attacker != null && attacker == target(boss)) {
                meleeStance = type.isMelee();
                targetMaging = type == AttackType.MAGIC;
            }
            double d = damage;
            if (exileDelay >= 0) {
                d *= 0.5; // 50% damage resistance between reaching a threshold and the exile
            }
            if (contract == YamaContract.BLOODIED_BLOWS) {
                d *= BossMechanics.WRETCHED_STRENGTH;
            }
            if (contract != null && contract.hitpoints > BossMechanics.YAMA_HITPOINTS) {
                d *= BossMechanics.YAMA_HITPOINTS / (double) contract.hitpoints;
            }
            int whole = (int) d;
            int dealt = whole + (boss.getRandom().nextDouble() < d - whole ? 1 : 0);
            if (attacker != null && dealt > 0) {
                // contribution: the damage in Yama's own 2,500 hitpoints (a contract's extra hitpoints are scaled out above)
                damageBy.merge(attacker.getUUID(), dealt, Integer::sum);
                fighters.put(attacker.getUUID(), attacker);
            }
            return dealt;
        }

        /** Defence bonuses by phase, contract and his target's style (OSRS Wiki infobox; glyphic attenuation 666 but crush). */
        @Override
        public void adjustDefence(DataBossEntity boss, ServerPlayer attacker, AttackType type,
                                  com.runecraft.osrs.PlayerHitCalc.Input in) {
            boolean p3 = stage >= 3;
            in.targetDefenceBonus = BossMechanics.yamaDefenceBonus(type, targetMaging, p3, contract);
            if (type == AttackType.RANGED) {
                in.targetRangedDefence = BossMechanics.yamaRangedDefences(p3, contract);
            }
        }

        /**
         * His fire (magic) and shadow (ranged) autos pierce the matching protection prayer: up to 3 in phase 3, more in every
         * phase under the contracts of sensory clouding and familiar acquisition, never under divine severance
         * ({@link BossMechanics#yamaAutoPierce}).
         */
        @Override
        protected void afterAttack(DataBossEntity boss, ServerPlayer target, AttackDef attack) {
            int pierce = BossMechanics.yamaAutoPierce(stage, contract);
            if (pierce <= 0 || target == null || (attack != ModernBosses.YAMA_FIRE && attack != ModernBosses.YAMA_SHADOW)) {
                return;
            }
            Prayer prayer = attack == ModernBosses.YAMA_FIRE ? Prayer.PROTECT_FROM_MAGIC : Prayer.PROTECT_FROM_MISSILES;
            if (BossFx.protects(target, prayer)) {
                hurt(boss, target, attack.name(), boss.getRandom().nextInt(pierce + 1));
            }
        }

        @Override
        public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
            return damage; // forfeit breath's drain on damage: the damage listener in register(), for every kind of hit
        }

        /** A floor spot at an offset from the arena centre, pulled in to stay at least two blocks inside the ring wall. */
        private Vec3 floorSpot(DataBossEntity boss, double dx, double dz) {
            double max = Math.max(2.0, (boss.def() == null ? 15 : boss.def().arenaRadius) - 2.0);
            double len = Math.hypot(dx, dz);
            if (len > max) {
                dx *= max / len;
                dz *= max / len;
            }
            Vec3 c = homeVec();
            return BossFx.ground(BossFx.world(boss), c.x + dx, c.z + dz, c.y);
        }

        /** The spot when a player fits there (solid floor, feet and head clear), otherwise the nearest safe column. */
        private static Vec3 standable(ServerLevel w, Vec3 spot) {
            BlockPos feet = BlockPos.containing(spot);
            if (w.getBlockState(feet).getCollisionShape(w, feet).isEmpty()
                    && w.getBlockState(feet.above()).getCollisionShape(w, feet.above()).isEmpty()
                    && !w.getBlockState(feet.below()).getCollisionShape(w, feet.below()).isEmpty()) {
                return spot;
            }
            return Vec3.atBottomCenterOf(Gielinor.safeSpot(w, feet.getX(), feet.getZ()));
        }

        private void placeGlyphs(DataBossEntity boss) {
            int[][] at = {{-6, -5}, {6, 5}, {6, -5}, {-6, 5}};
            for (int i = 0; i < at.length; i++) {
                glyphs.add(new Glyph(floorSpot(boss, at[i][0], at[i][1]), i < 2));
            }
        }

        private boolean glyphReady(Glyph g) {
            return stage < 3 && now >= g.readyAt;
        }

        /** Whether anyone stands on a lit glyph of this element (one player protects the whole team). */
        private boolean glyphProtects(DataBossEntity boss, boolean fire) {
            for (Glyph g : glyphs) {
                if (g.fire == fire && glyphReady(g) && !g.on.isEmpty()) {
                    g.readyAt = now + (contract == YamaContract.GLYPHIC_ATTENUATION ? 30 : 20);
                    return true;
                }
            }
            return false;
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            if (stage >= 3) {
                if (distance < 2.6) {
                    melee(boss, target, BossMechanics.yamaMeleeThroughPrayer(stage, contract));
                    spawnFlares(boss, 1);
                    return null;
                }
                if (++crashTimer >= 3) {
                    crashTimer = 0;
                    BossFx.warn(boss, "Yama hurls fireballs that burst into shadow!");
                    for (int i = 0; i < 3; i++) {
                        shadowCrash(boss, target, i);
                    }
                    hazard(boss, true, 2);
                }
                fireStyle = !fireStyle;
                return fireStyle ? ModernBosses.YAMA_FIRE : ModernBosses.YAMA_SHADOW;
            }
            if (++autosAtTarget >= DUO_SWITCH_AUTOS) {
                autosAtTarget = 0;
                List<ServerPlayer> others = new ArrayList<>(BossFx.players(boss, 24));
                others.remove(target);
                others.removeIf(this::intruder);
                if (!others.isEmpty()) {
                    ServerPlayer next = others.get(boss.getRandom().nextInt(others.size()));
                    boss.setTarget(next);
                    BossFx.say(boss, "Yama: Your turn, " + next.getName().getString() + ".");
                }
            }
            if (autosToSpecial <= 0) {
                startSpecial(boss, target);
                return null;
            }
            autosToSpecial--;
            if (distance < 2.6) {
                melee(boss, target, BossMechanics.yamaMeleeThroughPrayer(stage, contract));
                return null;
            }
            return fireStyle ? ModernBosses.YAMA_FIRE : ModernBosses.YAMA_SHADOW;
        }

        /** The axe swing: hits everyone within three tiles of the target; Protect from Melee caps it. */
        private void melee(DataBossEntity boss, ServerPlayer target, int throughPrayer) {
            int max = (int) Math.round(BossMechanics.YAMA_AUTO_MAX * multiplier(boss));
            cooldown = attackDelay(boss, null) - 1;
            attackCount++;
            for (ServerPlayer p : BossFx.players(boss, 24)) {
                if (flat(p.position(), target.position()) > 3 * TILE) {
                    continue;
                }
                int roll = boss.getRandom().nextInt(max + 1);
                if (BossFx.protects(p, Prayer.PROTECT_FROM_MELEE)) {
                    roll = Math.min(roll, (int) Math.round(throughPrayer * multiplier(boss)));
                }
                CombatHooks.nameAttack(ModernBosses.YAMA_SLASH.name(), AttackType.SLASH);
                int dealt = CombatHandler.applyDamage(boss, p, roll);
                if (dealt > 0) {
                    PoisonService.poison(p, 20, now);
                }
            }
            BossFx.burst(boss, ParticleTypes.SWEEP_ATTACK, 6, 1.5);
        }

        private void startSpecial(DataBossEntity boss, ServerPlayer target) {
            autosToSpecial = BossMechanics.yamaAutosBeforeSpecial(contract);
            cooldown = attackDelay(boss, null) - 1;
            hazard(boss, hazardWaves, BossMechanics.yamaHazardSets(stage, contract));
            hazardWaves = !hazardWaves;
            if (!fireStyle && ++shadowSpecials % 2 == 0) {
                BossFx.warn(boss, "Yama summons void flares! Destroy them before they explode.");
                spawnFlares(boss, contract != null ? contract.flares : 2);
                return; // the style stays the same after void flares
            }
            if (fireStyle) {
                pending = stage >= 2 ? Special.METEOR : Special.GLYPH_FIRE;
            } else {
                pending = stage >= 2 ? Special.STOMP : Special.GLYPH_SHADOW;
            }
            pendingAt = target.position();
            // phase 1's glyph specials land 6 ticks on (5 under a contract: wiki); phase 2's Meteor Strike 5, Shadow Stomp 4
            pendingDue = now + (pending == Special.METEOR ? 5 : pending == Special.STOMP ? 4 : BossMechanics.yamaGlyphSpecialTicks(contract));
            BossFx.cast(boss, pending == Special.METEOR ? "Meteor" : pending == Special.STOMP ? "Stomp" : "Glyph", null, // round 43
                    (int) (pendingDue - now) * com.runecraft.osrs.ArenaTiming.MC_PER_GAME_TICK, 24);
            BossFx.warn(boss, switch (pending) {
                case GLYPH_FIRE -> "Flames begin to envelop you... find a Glyph of Fire!";
                case GLYPH_SHADOW -> "Shadows swirl around you... find a Glyph of Shadow!";
                case METEOR -> "Yama calls down a meteor! Get away from it or onto a Glyph of Fire!";
                case STOMP -> "Yama raises his foot... stand on a Glyph of Shadow!";
            });
            fireStyle = !fireStyle;
        }

        private void landSpecial(DataBossEntity boss) {
            Special s = pending;
            pending = null;
            boolean fire = s == Special.GLYPH_FIRE || s == Special.METEOR;
            if (glyphProtects(boss, fire)) {
                BossFx.say(boss, "The glyph absorbs Yama's " + (fire ? "fire" : "shadow") + "!");
                return;
            }
            ServerLevel w = BossFx.world(boss);
            for (ServerPlayer p : BossFx.players(boss, 24)) {
                switch (s) {
                    case GLYPH_FIRE -> {
                        hurt(boss, p, "glyph_flames", boss.getRandom().nextInt(BossMechanics.YAMA_GLYPH_SPECIAL_MAX + 1));
                        BossDots.burn(boss, p, BossMechanics.YAMA_GLYPH_BURN); // unprotected fire specials burn
                    }
                    case GLYPH_SHADOW -> hurt(boss, p, "shadow_engulf", boss.getRandom().nextInt(BossMechanics.YAMA_GLYPH_SPECIAL_MAX + 1));
                    case METEOR -> {
                        int dmg = BossMechanics.meteorDamage(tiles(flat(p.position(), pendingAt)));
                        if (dmg > 0) {
                            hurt(boss, p, "meteor_strike", dmg >= 121 ? dmg : dmg - boss.getRandom().nextInt(6));
                            BossDots.burn(boss, p, BossMechanics.YAMA_METEOR_BURN);
                        }
                    }
                    case STOMP -> {
                        hurt(boss, p, "shadow_stomp", boss.getRandom().nextInt(BossMechanics.YAMA_SHADOW_STOMP_MAX + 1));
                        BossFx.stun(p, 2);
                        BossFx.drainPrayer(p, 10); // approx
                    }
                }
            }
            if (s == Special.METEOR) {
                w.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pendingAt.x, pendingAt.y + 0.5, pendingAt.z, 1, 0, 0, 0, 0);
            }
        }

        private void spawnFlares(DataBossEntity boss, int count) {
            spawnFlares(boss, count, false);
        }

        /**
         * Void flares around the arena, at most four at a time; {@code reduced}: the 81-hitpoint flares the contracts of divine
         * severance and familiar acquisition spawn all fight (OSRS Wiki, 4 June 2025: "Void flares that spawn with reduced
         * health have had their health decreased from 83 to 81").
         */
        private void spawnFlares(DataBossEntity boss, int count, boolean reduced) {
            flares.removeIf(f -> !f.mob().isAlive());
            int room = 4 - flares.size();
            for (int i = 0; i < Math.min(count, room); i++) {
                Mob m = BossFx.spawnMinion(boss, BossMechanics.YAMA_VOID_FLARE, around(boss, homeVec(), 9), null);
                if (m == null) {
                    continue;
                }
                OWNED.put(m, this);
                if (reduced) {
                    m.setHealth(m.getMaxHealth() * BossMechanics.CONTINUOUS_FLARE_HP / BossMechanics.VOID_FLARE_HP);
                } else if (stage >= 3) {
                    m.setHealth(m.getMaxHealth() / 2); // 71 of 140 in phase 3
                }
                flares.add(new Flare(m, now + BossMechanics.VOID_FLARE_TICKS, m.getHealth()));
                boolean resistant = contract == YamaContract.BLOODIED_BLOWS;
                // Magic and ranged attacks always land their max hit on a flare, except under the Contract of bloodied blows.
                MinionRules.set(m, new MinionRules.Rule() {
                    @Override
                    public double accuracy(ServerPlayer p, net.minecraft.world.entity.LivingEntity minion, AttackType type) {
                        return guaranteed(type) ? MinionRules.ALWAYS_HITS : 1.0;
                    }

                    @Override
                    public boolean alwaysMaxHit(ServerPlayer p, net.minecraft.world.entity.LivingEntity minion, AttackType type) {
                        return guaranteed(type);
                    }

                    private boolean guaranteed(AttackType type) {
                        return !resistant && (type == AttackType.MAGIC || type == AttackType.RANGED);
                    }

                    @Override
                    public int damage(ServerPlayer p, net.minecraft.world.entity.LivingEntity minion, AttackType type, int d) {
                        int dealt = resistant ? d / 2 : d;
                        if (dealt >= minion.getHealth() && !SpecialAttackService.specialActive(p)) {
                            CombatAchievementService.flag(p, BossMechanics.FLAG_YAMA_FLARE_NONSPEC);
                        }
                        return dealt;
                    }
                });
            }
        }

        /** Shadow waves from the south-east/west or fire streaks from the east/west edges. */
        private void hazard(DataBossEntity boss, boolean waves, int count) {
            BossDefRadius r = new BossDefRadius(boss);
            for (int i = 0; i < count; i++) {
                Vec3 dir;
                if (waves) {
                    double sx = boss.getRandom().nextBoolean() ? 1 : -1;
                    dir = new Vec3(-sx, 0, -1).normalize(); // from the south-east or south-west, heading north
                } else {
                    dir = new Vec3(boss.getRandom().nextBoolean() ? 1 : -1, 0, 0);
                }
                Vec3 start = homeVec().subtract(dir.scale(r.radius + i * 4.0));
                sweeps.add(new Sweep(start, dir, 2 * r.radius + i * 4.0, r.radius, waves ? 1.5 : 2.0, waves));
            }
            BossFx.warn(boss, waves ? "Waves of shadow surge across the arena - skip over them!" : "Fire streaks across the arena - keep moving!");
        }

        /** Phase 3: three fireballs in a line on the target; shadows converge on the centre one (a bow tie). */
        private void shadowCrash(DataBossEntity boss, ServerPlayer target, int index) {
            int axis = (crashAxis + index) % 2; // cardinal, diagonal, cardinal
            Vec3 along;
            if (axis == 0) {
                along = boss.getRandom().nextBoolean() ? new Vec3(1, 0, 0) : new Vec3(0, 0, 1);
            } else {
                along = new Vec3(boss.getRandom().nextBoolean() ? 1 : -1, 0, 1).normalize();
            }
            crashAxis++;
            Vec3 centre = target.position();
            Vec3 dirAlong = along;
            int delay = 3 + index * 2;
            java.util.function.BiConsumer<List<ServerPlayer>, Vec3> land = (hit, pos) -> crashLands(boss, target, pos, dirAlong);
            for (int k = -1; k <= 1; k++) {
                Vec3 at = centre.add(dirAlong.scale(k * 2 * TILE));
                if (k == 0) {
                    strike(boss, at, delay, 0, 0.9, ParticleTypes.SOUL_FIRE_FLAME, land);
                } else {
                    strike(boss, at, delay, 0, 0.9, ParticleTypes.SOUL_FIRE_FLAME);
                }
            }
        }

        private void crashLands(DataBossEntity boss, ServerPlayer owner, Vec3 centre, Vec3 along) {
            Vec3 perp = new Vec3(-along.z, 0, along.x);
            for (ServerPlayer p : BossFx.players(boss, 24)) {
                Vec3 rel = new Vec3(p.getX() - centre.x, 0, p.getZ() - centre.z);
                double a = Math.abs(rel.dot(along));
                double side = Math.abs(rel.dot(perp));
                if (a <= 2 * TILE + 0.5 && side < 1.0 + 0.25 * a) {
                    hurt(boss, p, "shadow_crash", boss.getRandom().nextInt(BossMechanics.YAMA_SHADOW_CRASH_MAX + 1));
                }
                if (p == owner && flat(p.position(), centre) > 1.5 * TILE) {
                    CombatAchievementService.flag(p, BossMechanics.FLAG_YAMA_CRASH_FAR);
                }
            }
        }

        /**
         * The BossDef's 66%/33% phases only mark the base thresholds; the script keeps its own (66.6% and 33.3%, or 75% and
         * 50% under a contract: OSRS Wiki, 29 May 2025) in {@link #checkThresholds}.
         */
        @Override
        public void onPhase(DataBossEntity boss, int phase) {
        }

        private void checkThresholds(DataBossEntity boss) {
            double[] at = YamaLoot.thresholds(contract);
            while (thresholdsPassed < at.length && percent(boss) <= at[thresholdsPassed]) {
                thresholdsPassed++;
                exilesQueued++; // both thresholds can pass with one hit: the second exile follows the first
                if (!exiled && exileDelay < 0) {
                    exileDelay = 4;
                }
                pending = null;
                BossFx.say(boss, "Yama grows bored and raises his axe's pommel...");
            }
        }

        private void beginExile(DataBossEntity boss) {
            exileDelay = -1;
            exiled = true;
            exileStart = now;
            judgeAttacks = 0;
            exileHits.clear();
            exilees.clear();
            flares.forEach(f -> f.mob().discard());
            flares.clear();
            sweeps.clear();
            setSpeed(boss, 0.0);
            boss.getNavigation().stop();
            Vec3 c = homeVec();
            boss.teleportTo(c.x, c.y, c.z);
            ServerLevel w = BossFx.world(boss);
            // The exile spot and the Judge sit in the arena's west side, inside the ring wall.
            Vec3 spot = standable(w, floorSpot(boss, -12, 4));
            for (ServerPlayer p : BossFx.players(boss, 24)) {
                if (intruder(p)) {
                    continue;
                }
                p.teleportTo(spot.x, spot.y, spot.z);
                exilees.add(p);
                Msg.red(p, "Yama slams his axe down and exiles you from the arena! Defeat the Judge of Yama.");
            }
            ServerPlayer target = exilees.isEmpty() ? null : exilees.get(0);
            judge = BossFx.spawnMinion(boss, BossMechanics.YAMA_JUDGE, floorSpot(boss, -12, -3), target);
            if (judge != null) {
                OWNED.put(judge, this);
                // The Judge prays against ranged and the last style it was hit with (melee at the start); other attacks
                // always land, and always for their max hit.
                AttackType[] last = {AttackType.CRUSH};
                MinionRules.set(judge, new MinionRules.Rule() {
                    @Override
                    public double accuracy(ServerPlayer p, net.minecraft.world.entity.LivingEntity m, AttackType type) {
                        return MinionRules.ALWAYS_HITS;
                    }

                    @Override
                    public boolean alwaysMaxHit(ServerPlayer p, net.minecraft.world.entity.LivingEntity m, AttackType type) {
                        return true;
                    }

                    @Override
                    public int damage(ServerPlayer p, net.minecraft.world.entity.LivingEntity m, AttackType type, int d) {
                        boolean blocked = type == AttackType.RANGED || type.isMelee() == last[0].isMelee()
                                && (type == AttackType.MAGIC) == (last[0] == AttackType.MAGIC);
                        last[0] = type;
                        if (blocked) {
                            CombatAchievementService.flag(p, BossMechanics.FLAG_YAMA_JUDGE_WRONG);
                            Msg.action(p, "The Judge of Yama's prayers block your attack!");
                            return 0;
                        }
                        return Math.max(d, 1);
                    }
                });
            }
        }

        private void exileTick(DataBossEntity boss) {
            boolean judgeAlive = judge != null && judge.isAlive();
            if (judgeAlive && now - exileStart < 300) {
                // every 5 ticks, one bomb; under a contract every 3 ticks, two bombs (OSRS Wiki "Yama/Strategies")
                if (now > exileStart && (now - exileStart) % BossMechanics.yamaJudgeTicks(contract) == 0) {
                    judgeAttacks++;
                    for (ServerPlayer p : exilees) {
                        if (!p.isAlive()) {
                            continue;
                        }
                        UUID id = p.getUUID();
                        for (int bomb = 0; bomb < BossMechanics.yamaJudgeBombs(contract); bomb++) {
                            // the second bomb lands a couple of blocks off the player (approx: where, the wiki doesn't say)
                            Vec3 at = bomb == 0 ? p.position() : around(boss, p.position(), 2.5);
                            namedStrike("fire_wave", boss, at, 2, BossMechanics.JUDGE_FIRE_MAX, 1.3, ParticleTypes.FLAME, (hit, pos) -> {
                                if (hit.contains(p)) {
                                    exileHits.merge(id, 1, Integer::sum);
                                }
                            });
                        }
                        if (judgeAttacks == 5 || judgeAttacks == 13) {
                            hurt(boss, p, "cavern_shake", 5 + boss.getRandom().nextInt(3));
                            exileHits.merge(id, 1, Integer::sum);
                            Msg.red(p, "The cavern shakes!");
                        }
                    }
                }
                return;
            }
            // The Judge is slain (or the exile ran out): back to Yama for the next phase.
            if (judge != null && judge.isAlive()) {
                judge.discard();
            }
            judge = null;
            exiled = false;
            stage++;
            Vec3 front = floorSpot(boss, 0, 7);
            for (ServerPlayer p : exilees) {
                if (exileHits.getOrDefault(p.getUUID(), 0) > 1) {
                    CombatAchievementService.flag(p, BossMechanics.FLAG_YAMA_EXILE_HITS);
                }
                if (p.isAlive()) {
                    p.teleportTo(front.x, front.y, front.z);
                }
            }
            exilees.clear();
            meleeStance = true;
            autosToSpecial = Math.min(1, BossMechanics.yamaAutosBeforeSpecial(contract));
            cooldown = 3;
            if (--exilesQueued > 0) {
                exileDelay = 4;
            }
            if (stage >= 3) {
                setSpeed(boss, 0.0);
                BossFx.say(boss, "Yama: Enough! You will burn! (He plants himself in the centre; the glyphs fade.)");
                nextFlare = now + 20;
            } else {
                BossFx.say(boss, "Yama: Your judgement is not yet over.");
            }
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
            if (glyphs.isEmpty()) {
                placeGlyphs(boss);
            }
            if (boss.isAlive()) {
                checkThresholds(boss);
            }
            ServerLevel w = BossFx.world(boss);
            List<ServerPlayer> players = BossFx.players(boss, 24);
            if (contract != null && target != null) {
                if (contract == YamaContract.FORFEIT_BREATH) {
                    // OSRS Wiki: 1% every 4 ticks, besides 1% a point of damage he deals; the player dies at 0%
                    for (ServerPlayer p : players) {
                        if (intruder(p)) {
                            continue;
                        }
                        SysState.SysPlayer sp = SysState.of(p);
                        if (now % BossMechanics.FORFEIT_DRAIN_TICKS == 0) {
                            sp.runEnergy = Math.max(0, sp.runEnergy - BossMechanics.FORFEIT_DRAIN);
                        }
                        if (sp.runEnergy <= 0) {
                            sp.runEnergy = BossMechanics.RUN_ENERGY_FULL;
                            Msg.red(p, "You run out of breath and forfeit the contract!");
                            hurt(boss, p, "forfeit", 999);
                        }
                    }
                }
                if (contract.seversPrayers && now % 10 == 0) {
                    for (ServerPlayer p : players) {
                        if (BossFx.protects(p, Prayer.PROTECT_FROM_MAGIC) || BossFx.protects(p, Prayer.PROTECT_FROM_MISSILES)
                                || BossFx.protects(p, Prayer.PROTECT_FROM_MELEE)) {
                            BossFx.disablePrayers(p, "Yama severs your connection to the gods!");
                        }
                    }
                }
                if (contract.continuousFlares && !exiled && now % 20 == 0) { // approx: one every 20 ticks
                    spawnFlares(boss, 1, true);
                }
                CombatAchievementService.flag(target, BossMechanics.FLAG_YAMA_CONTRACT);
            }
            if (exileDelay > 0 && --exileDelay == 0) {
                beginExile(boss);
            }
            if (exiled) {
                exileTick(boss);
                return;
            }
            setSpeed(boss, stage < 3 && meleeStance ? 0.22 : 0.0);
            // Glyphs: lit glyphs glow; stepping on one costs health (fire) or prayer (shadow) and uses it up unless a
            // matching special is on its way.
            for (Glyph g : glyphs) {
                boolean ready = glyphReady(g);
                w.sendParticles(ready ? (g.fire ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME) : ParticleTypes.SMOKE,
                        g.pos.x, g.pos.y + 0.1, g.pos.z, 3, 0.4, 0.05, 0.4, 0);
                Set<UUID> now0 = new HashSet<>();
                for (ServerPlayer p : players) {
                    if (!ready || flat(p.position(), g.pos) > 1.3 || Math.abs(p.getY() - g.pos.y) > 2) {
                        continue;
                    }
                    now0.add(p.getUUID());
                    if (g.on.contains(p.getUUID())) {
                        continue;
                    }
                    int cost = BossMechanics.glyphCost(tiles(flat(boss.position(), g.pos))) * BossMechanics.yamaGlyphCostScale(contract);
                    if (g.fire) {
                        hurt(boss, p, "glyph_of_fire", cost);
                    } else {
                        BossFx.drainPrayer(p, cost);
                    }
                    boolean matching = pending != null && (g.fire == (pending == Special.GLYPH_FIRE || pending == Special.METEOR));
                    if (!matching) {
                        g.readyAt = now + 20; // used up
                    }
                }
                g.on.clear();
                g.on.addAll(now0);
            }
            if (pending != null) {
                if (pending == Special.METEOR) {
                    BossFx.ring(w, pendingAt, 2 * TILE, ParticleTypes.LAVA);
                } else if (target != null) {
                    w.sendParticles(pending == Special.GLYPH_FIRE ? ParticleTypes.FLAME : ParticleTypes.SQUID_INK,
                            target.getX(), target.getY() + 1, target.getZ(), 6, 0.4, 0.6, 0.4, 0.01);
                }
                if (now >= pendingDue) {
                    landSpecial(boss);
                }
            }
            tickSweeps(boss, w, players);
            tickFlares(boss, players);
            if (stage >= 3 && now >= nextFlare && target != null) {
                nextFlare = now + 30;
                spawnFlares(boss, 2);
            }
            for (ServerPlayer p : players) {
                lastPos.put(p.getUUID(), p.position());
            }
        }

        private void tickSweeps(DataBossEntity boss, ServerLevel w, List<ServerPlayer> players) {
            Iterator<Sweep> it = sweeps.iterator();
            while (it.hasNext()) {
                Sweep s = it.next();
                s.travelled += s.speed;
                if (s.travelled > s.length) {
                    it.remove();
                    continue;
                }
                Vec3 centre = s.start.add(s.dir.scale(s.travelled));
                Vec3 perp = new Vec3(-s.dir.z, 0, s.dir.x);
                ParticleOptions fx = s.shadow ? ParticleTypes.SQUID_INK : ParticleTypes.FLAME;
                for (double k = -s.halfWidth; k <= s.halfWidth; k += 1.5) {
                    Vec3 at = centre.add(perp.scale(k));
                    w.sendParticles(fx, at.x, centre.y + 0.2, at.z, 1, 0.1, 0.1, 0.1, 0);
                }
                for (ServerPlayer p : players) {
                    if (s.passed.contains(p.getUUID())) {
                        continue;
                    }
                    Vec3 rel = new Vec3(p.getX() - centre.x, 0, p.getZ() - centre.z);
                    if (Math.abs(rel.dot(s.dir)) > 1.0 || Math.abs(rel.dot(perp)) > s.halfWidth) {
                        continue;
                    }
                    s.passed.add(p.getUUID());
                    if (s.shadow) {
                        if (p.onGround()) {
                            hurt(boss, p, "shadow_wave", boss.getRandom().nextInt(BossMechanics.YAMA_SHADOW_WAVE_MAX + 1));
                            BossFx.disablePrayers(p, "The shadows snuff out your prayers!");
                        } else {
                            Msg.action(p, "You skip over the wave of shadow.");
                        }
                    } else {
                        Vec3 before = lastPos.get(p.getUUID());
                        boolean moving = before != null && flat(before, p.position()) > 0.15;
                        if (!moving) {
                            hurt(boss, p, "fire_streak", boss.getRandom().nextInt(BossMechanics.YAMA_FIRE_STREAK_MAX + 1));
                            BossFx.stun(p, 3);
                            Msg.red(p, "The flames sear you to the ground!");
                        }
                    }
                }
            }
        }

        private void tickFlares(DataBossEntity boss, List<ServerPlayer> players) {
            Iterator<Flare> it = flares.iterator();
            while (it.hasNext()) {
                Flare f = it.next();
                Mob m = f.mob();
                if (!m.isAlive()) {
                    it.remove();
                    continue;
                }
                BossFx.ring(BossFx.world(boss), m.position(), 0.8, now + 6 >= f.due() ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.WITCH);
                if (now < f.due()) {
                    continue;
                }
                it.remove();
                double frac = f.full() <= 0 ? 0 : Math.min(1.0, m.getHealth() / f.full()); // phase 3 flares spawn at half health
                m.discard();
                if (glyphProtects(boss, true) || glyphProtects(boss, false)) {
                    BossFx.say(boss, "The glyph absorbs the void flare's explosion.");
                    continue;
                }
                int dmg = contract != null && contract.lethalFlares ? 999 : BossMechanics.flareExplosion(frac, stage >= 3);
                for (ServerPlayer p : players) {
                    hurt(boss, p, "void_flare", contract != null && contract.lethalFlares ? dmg : Math.max(0, dmg - boss.getRandom().nextInt(3)));
                }
                boss.heal(mcHp(boss, Math.min(dmg, 44)));
                BossFx.sound(boss, SoundEvents.GENERIC_EXPLODE.value(), 0.9f);
            }
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayer killer) {
            synchronized (ACTIVE) {
                ACTIVE.remove(this);
            }
            flares.forEach(f -> f.mob().discard());
            if (judge != null) {
                judge.discard();
            }
            if (contract != null && killer != null) {
                // OSRS Wiki "Yama#Contracts": sigil contracts come back with their purifying sigil piece; acquisition
                // contracts are used up and guarantee their reward (oathplate: the first piece the player isn't carrying).
                // Only the player who showed Yama the contract receives it (the duo partner rolls the unique table only).
                ServerPlayer slayer = killer; // who landed the kill: the boss engine rolled their pet, and rollLoot skipped them
                ServerPlayer host = contractHost == null ? null : fighters.get(contractHost);
                if (host != null && host.isAlive()) {
                    killer = host;
                }
                if (contract.returnedOnKill()) {
                    Inv.give(killer, contract.item, 1);
                    Msg.game(killer, "Yama returns your " + contract.displayName().toLowerCase(java.util.Locale.ROOT) + ".");
                }
                // The slayer's pet (the contract holder or their partner): the boss engine rolled the usual 1/2,500 with the
                // kill; the familiar contract tops it up to 1/100, as rollLoot rolls 1/100 for every other fighter.
                int extra = YamaLoot.killerExtraPetRate(contract);
                if (extra > 0) {
                    rollPet(slayer, extra, boss.getRandom()::nextInt);
                }
                String reward = contract.reward();
                if (reward == null) {
                    return; // the familiar contract's reward is its pet chance (and the standard table, rolled as usual)
                }
                if (contract == YamaContract.OATHPLATE_ACQUISITION) {
                    for (String piece : new String[]{"oathplate_legs", "oathplate_chest", "oathplate_helm"}) {
                        if (!Inv.has(killer, piece, 1)) {
                            reward = piece;
                        }
                    }
                }
                Inv.give(killer, reward, contract.rewardQuantity());
                com.runecraft.collection.CollectionLog.obtain(killer, reward);
                Msg.game(killer, "Yama honours the terms of your contract: " + contract.rewardQuantity() + " x "
                        + com.runecraft.osrs.ItemCatalog.get(reward).name() + ".");
            }
        }

        /** The arena radius of the boss (its BossDef's), for hazards that sweep the whole arena. */
        private record BossDefRadius(double radius) {
            BossDefRadius(DataBossEntity boss) {
                this(boss.def() == null ? 14 : Math.max(8, boss.def().arenaRadius - 1));
            }
        }
    }

    // =====================================================================================================
    // Amoxliatl
    // =====================================================================================================

    /**
     * Amoxliatl: a standard magic attack that also drops ice spikes and leaves an icy pool on your tile; after every 2-4
     * standard attacks she alternates between Unstable Ice (1-4 blocks to break with melee - one hit each, at attack speed
     * 1 - within 15 ticks, or they shatter
     * into icy pools and heal her) and Icicle Crash (switches off your prayers, then a 34 you can block by praying again).
     */
    static final class Amoxliatl extends ScriptedBoss {
        private int untilSpecial = 2;
        private boolean iceNext;
        private final List<Mob> ice = new ArrayList<>();
        private long shatterAt = -1;

        @Override
        public void onSpawn(DataBossEntity boss) {
            iceNext = boss.getRandom().nextBoolean();
            untilSpecial = 2 + boss.getRandom().nextInt(3);
        }

        @Override
        protected boolean driven() {
            return true;
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            if (untilSpecial <= 0) {
                untilSpecial = 2 + boss.getRandom().nextInt(3);
                boolean doIce = iceNext;
                iceNext = !iceNext;
                if (doIce) {
                    unstableIce(boss, target);
                    cooldown = attackDelay(boss, null) - 1;
                    return null;
                }
                BossFx.disablePrayers(target, "Amoxliatl disables your prayers and launches an icicle attack!");
                return VarlamoreBosses.AMOX_SPIKES;
            }
            untilSpecial--;
            // The standard attack: ice spikes on your tile and around the arena (typeless) and an icy pool where you stood.
            Vec3 at = target.position();
            ServerLevel w = BossFx.world(boss);
            namedStrike("ice_spikes", boss, at, 2, 8, 0.9, ParticleTypes.SNOWFLAKE, (hit, pos) -> icyPool(w, boss, pos, 0.9)); // approx damage
            for (int i = 0; i < 2; i++) {
                namedStrike("ice_spikes", boss, around(boss, homeVec(), 8), 2, 8, 0.9, ParticleTypes.SNOWFLAKE);
            }
            return VarlamoreBosses.AMOX_ICE;
        }

        private void icyPool(ServerLevel w, DataBossEntity boss, Vec3 pos, double radius) {
            int dmg = BossMechanics.ICY_POOL_MIN + boss.getRandom().nextInt(BossMechanics.ICY_POOL_MAX - BossMechanics.ICY_POOL_MIN + 1);
            Mechanics.addHazard(w, pos, dmg, 12, boss); // approx duration
        }

        private void unstableIce(DataBossEntity boss, ServerPlayer target) {
            int n = 1 + boss.getRandom().nextInt(4);
            for (int i = 0; i < n; i++) {
                Mob m = BossFx.spawnMinion(boss, BossMechanics.UNSTABLE_ICE, around(boss, target.position(), 4), null);
                if (m == null) {
                    continue;
                }
                ice.add(m);
                // Only melee breaks it, always in one hit.
                MinionRules.set(m, new MinionRules.Rule() {
                    @Override
                    public double accuracy(ServerPlayer p, net.minecraft.world.entity.LivingEntity mob, AttackType type) {
                        return type.isMelee() ? MinionRules.ALWAYS_HITS : 1.0;
                    }

                    @Override
                    public int damage(ServerPlayer p, net.minecraft.world.entity.LivingEntity mob, AttackType type, int d) {
                        if (!type.isMelee()) {
                            Msg.action(p, "Only a melee attack can break the unstable ice.");
                            return 0;
                        }
                        // OSRS Wiki: melee on unstable ice works at attack speed 1, so the next swing is ready a tick later.
                        com.runecraft.player.PlayerData data = com.runecraft.player.Players.data(p);
                        int speed = com.runecraft.combat.Equipment.weaponSpeed(p, data.skills());
                        data.lastAttackTick = com.runecraft.action.ActionManager.mcTick()
                                - (long) (speed - 1) * com.runecraft.action.ActionManager.MC_TICKS_PER_GAME_TICK;
                        return MinionRules.lethal(mob);
                    }
                });
            }
            shatterAt = now + BossMechanics.UNSTABLE_ICE_TICKS;
            BossFx.warn(boss, "Amoxliatl forms some unstable ice blocks around you.");
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
            ice.removeIf(m -> !m.isAlive());
            if (shatterAt < 0) {
                return;
            }
            if (ice.isEmpty()) {
                shatterAt = -1;
                return;
            }
            for (Mob m : ice) {
                BossFx.ring(BossFx.world(boss), m.position(), 0.7, ParticleTypes.SNOWFLAKE);
            }
            if (now < shatterAt) {
                return;
            }
            ServerLevel w = BossFx.world(boss);
            for (Mob m : ice) {
                icyPool(w, boss, m.position(), 1.5 * TILE);
                for (int[] o : new int[][]{{2, 0}, {-2, 0}, {0, 2}, {0, -2}}) {
                    icyPool(w, boss, m.position().add(o[0], 0, o[1]), 1.0);
                }
                boss.heal(mcHp(boss, BossMechanics.UNSTABLE_ICE_HEAL_MIN
                        + boss.getRandom().nextInt(BossMechanics.UNSTABLE_ICE_HEAL_MAX - BossMechanics.UNSTABLE_ICE_HEAL_MIN + 1)));
                w.sendParticles(ParticleTypes.ITEM_SNOWBALL, m.getX(), m.getY() + 0.5, m.getZ(), 20, 0.6, 0.4, 0.6, 0.1);
                m.discard();
            }
            ice.clear();
            shatterAt = -1;
            shattered++;
            CombatAchievementService.flagNear(boss, BossMechanics.FLAG_AMOX_ICE_SHATTERED);
            BossFx.say(boss, "The unstable ice shatters, and Amoxliatl draws strength from it!");
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayer killer) {
            ice.forEach(Mob::discard);
        }

        /** Times her unstable ice shattered (self-test walk). */
        private int shattered;

        int walkShattered() {
            return shattered;
        }

        List<Mob> walkIce() {
            return ice;
        }

        /** Makes her next attack a special: Unstable Ice ({@code ice}) or Icicle Crash. */
        void walkSpecialNext(boolean ice) {
            untilSpecial = 0;
            iceNext = ice;
        }
    }

    // =====================================================================================================
    // The Maggot King
    // =====================================================================================================

    /**
     * The Maggot King: ranged spit (poisonous bile) or magic spit (sticky bile) 4-8 times, then a screech (turn your
     * protection prayer off) with carrion raining down, its defence halved meanwhile, and a style switch. From ~1,250
     * hitpoints each screech launches ur-maggot larvae (shoot them with a bow before they land); from ~980 it slams the
     * ground in front of it after screeching (55, 35 through prayer) - hit it as the slam lands for a 50% stronger melee
     * punish; below ~299 it only slams and launches up to four larvae.
     */
    static final class MaggotKing extends ScriptedBoss {
        private boolean magic;
        private int untilScreech = 4;
        private int screechTicks = -1;
        private boolean screeched;
        private boolean meleeMode;
        private long slamDue = -1;
        private Vec3 slamDir;
        private long punishUntil = -1;
        private final Set<UUID> punished = new HashSet<>();
        private boolean westNext = true;
        private final List<Larva> larvae = new ArrayList<>();
        /** Carrion lying on the floor (where it landed, and when it rots away): landed larvae eat it to heal the King. */
        private final List<Carrion> carrion = new ArrayList<>();
        private MinionRules.AttackWatcher watcher;

        private record Carrion(Vec3 pos, long until) {
        }

        private static final class Larva {
            final Mob mob;
            final long landsAt;
            double dirX;
            int chip = 3;

            Larva(Mob mob, long landsAt, double dirX) {
                this.mob = mob;
                this.landsAt = landsAt;
                this.dirX = dirX;
            }
        }

        private int trueHp(DataBossEntity boss) {
            return (int) Math.round(boss.getHealth() / boss.getMaxHealth() * BossMechanics.MAGGOT_HITPOINTS);
        }

        @Override
        public void onSpawn(DataBossEntity boss) {
            untilScreech = BossMechanics.maggotAttacksBeforeScreech(boss.getRandom().nextInt(5));
            // Melee punish: an attack landed while the slam comes down deals 50% more.
            watcher = (p, target, type, d) -> {
                if (!boss.isRemoved() && target == boss && punishUntil >= now && type.isMelee()) {
                    punished.add(p.getUUID());
                }
            };
            MinionRules.watch(watcher);
        }

        @Override
        protected boolean driven() {
            return true;
        }

        @Override
        protected boolean canAttack(DataBossEntity boss) {
            return screechTicks < 0 && slamDue < 0;
        }

        @Override
        protected int attackDelay(DataBossEntity boss, AttackDef attack) {
            BossDefSpeed s = new BossDefSpeed(boss);
            return screeched ? Math.max(2, s.speed - 1) : s.speed; // one tick faster after the first screech
        }

        private record BossDefSpeed(int speed) {
            BossDefSpeed(DataBossEntity boss) {
                this(boss.def() == null ? 5 : boss.def().attackSpeed);
            }
        }

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (attacker != null && punishUntil >= now && type.isMelee()) {
                Msg.action(attacker, "You punish the Maggot King's slam!");
                return (int) Math.round(damage * BossMechanics.MAGGOT_PUNISH_DAMAGE);
            }
            return damage;
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            int hp = trueHp(boss);
            if (untilScreech <= 0) {
                startScreech(boss);
                return null;
            }
            untilScreech--;
            if (meleeMode || hp <= BossMechanics.MAGGOT_FINAL_HP) {
                slamDir = new Vec3(target.getX() - boss.getX(), 0, target.getZ() - boss.getZ()).normalize();
                slamDue = now + 3;
                BossFx.warn(boss, "The Maggot King rears its head to slam!");
                BossFx.cast(boss, "Slam", null, 3 * com.runecraft.osrs.ArenaTiming.MC_PER_GAME_TICK, 16); // round 43
                return null;
            }
            return magic ? ModernBosses.MAGGOT_MAGIC : ModernBosses.MAGGOT_RANGED;
        }

        @Override
        protected void afterAttack(DataBossEntity boss, ServerPlayer target, AttackDef attack) {
            // Bile on the target's tile and two adjacent ones; ranged spit also poisons.
            ServerLevel w = BossFx.world(boss);
            Vec3 at = target.position();
            Vec3 side = boss.getRandom().nextBoolean() ? new Vec3(TILE, 0, 0) : new Vec3(0, 0, TILE);
            for (int k = -1; k <= 1; k++) {
                // bile: a green floor block while it lasts (approx: until stepped on or covered)
                Mechanics.addHazard(w, at.add(side.scale(k)), 3, 40, boss, com.runecraft.boss.HazardKind.POISON);
            }
            if (attack == ModernBosses.MAGGOT_RANGED) {
                PoisonService.poison(target, 8, now);
            }
        }

        private void startScreech(DataBossEntity boss) {
            screechTicks = 5; // 2-tick wind-up, then up to three damaging ticks
            BossFx.warn(boss, "A white aura surrounds the Maggot King - turn off your protection prayer!");
            BossFx.burst(boss, ParticleTypes.WHITE_ASH, 30, 2.0);
            SysCombat.drain(boss).defence = Math.max(SysCombat.drain(boss).defence, 100); // defence halved to 100 (of 200)
        }

        private void endScreech(DataBossEntity boss) {
            screechTicks = -1;
            screeched = true;
            SysCombat.drain(boss).defence = 0; // drained defence is restored after a screech
            untilScreech = BossMechanics.maggotAttacksBeforeScreech(boss.getRandom().nextInt(5));
            int hp = trueHp(boss);
            // Carrion: big piles (25) on random tiles, then smaller bits (10) next to them; it wipes away bile.
            ServerLevel w = BossFx.world(boss);
            for (int i = 0; i < 5; i++) {
                Vec3 at = around(boss, homeVec(), 12);
                namedStrike("carrion", boss, at, 3, BossMechanics.MAGGOT_CARRION_MAX, 1.0, ParticleTypes.MYCELIUM, (hit, pos) -> {
                    Mechanics.clearHazards(w, new AABB(pos, pos).inflate(1.5, 2, 1.5));
                    carrion.add(new Carrion(pos, now + BossMechanics.MAGGOT_CARRION_TICKS));
                    for (int[] o : new int[][]{{2, 0}, {-2, 0}, {0, 2}, {0, -2}}) {
                        namedStrike("carrion", boss, pos.add(o[0], 0, o[1]), 2, BossMechanics.MAGGOT_CARRION_BIT_MAX, 0.9,
                                ParticleTypes.MYCELIUM, (h2, bit) -> carrion.add(new Carrion(bit, now + BossMechanics.MAGGOT_CARRION_TICKS)));
                    }
                });
            }
            if (hp <= BossMechanics.MAGGOT_MELEE_HP && hp > BossMechanics.MAGGOT_FINAL_HP) {
                meleeMode = !meleeMode;
                if (!meleeMode) {
                    magic = !magic; // back to the style used before melee... then switches as usual
                }
            } else if (hp > BossMechanics.MAGGOT_MELEE_HP) {
                magic = !magic;
            }
            launchLarvae(boss, hp);
        }

        private void launchLarvae(DataBossEntity boss, int hp) {
            larvae.removeIf(l -> !l.mob.isAlive());
            int n = BossMechanics.maggotLarvae(hp, larvae.size());
            for (int i = 0; i < n; i++) {
                double side = westNext ? -1 : 1;
                westNext = !westNext;
                Vec3 c = homeVec();
                Vec3 at = BossFx.ground(BossFx.world(boss), c.x + side * 8, c.z + 4 + i * TILE, c.y);
                Mob m = BossFx.spawnMinion(boss, BossMechanics.UR_MAGGOT_LARVA, at, null);
                if (m == null) {
                    continue;
                }
                Larva l = new Larva(m, now + 4, -side);
                larvae.add(l);
                // In the air it can only be shot down with a bow (and then dies at once).
                MinionRules.set(m, (p, mob, type, d) -> {
                    if (now < l.landsAt) {
                        return type == AttackType.RANGED ? MinionRules.lethal(mob) : 0;
                    }
                    return d;
                });
            }
            if (n > 0) {
                BossFx.warn(boss, "The Maggot King launches ur-maggot larvae - shoot them down before they land!");
            }
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
            ServerLevel w = BossFx.world(boss);
            List<ServerPlayer> players = BossFx.players(boss, 24);
            if (screechTicks >= 0) {
                BossFx.burst(boss, ParticleTypes.WHITE_ASH, 8, 2.0);
                if (screechTicks <= 3 && screechTicks > 0) {
                    for (ServerPlayer p : players) {
                        if (BossFx.protects(p, Prayer.PROTECT_FROM_MAGIC) || BossFx.protects(p, Prayer.PROTECT_FROM_MISSILES)
                                || BossFx.protects(p, Prayer.PROTECT_FROM_MELEE)) {
                            hurt(boss, p, "screech", BossMechanics.MAGGOT_SCREECH_MIN
                                    + boss.getRandom().nextInt(BossMechanics.MAGGOT_SCREECH_MAX - BossMechanics.MAGGOT_SCREECH_MIN + 1));
                        }
                    }
                }
                if (screechTicks-- == 0) {
                    endScreech(boss);
                }
            }
            if (slamDue >= 0) {
                if (now < slamDue) {
                    Vec3 front = boss.position().add(slamDir.scale(4));
                    BossFx.ring(w, front, 3.0, ParticleTypes.MYCELIUM);
                } else {
                    slam(boss, players);
                    slamDue = -1;
                    cooldown = attackDelay(boss, null) - 1;
                }
            }
            if (punishUntil >= 0 && now > punishUntil) {
                for (ServerPlayer p : players) {
                    if (!punished.contains(p.getUUID())) {
                        CombatAchievementService.flag(p, BossMechanics.FLAG_MAGGOT_PUNISH_MISSED);
                    }
                }
                punished.clear();
                punishUntil = -1;
            }
            tickLarvae(boss, players);
        }

        /** The 3x4-tile slam in front of it (towards where the target stood), with biles launched ahead. */
        private void slam(DataBossEntity boss, List<ServerPlayer> players) {
            Vec3 perp = new Vec3(-slamDir.z, 0, slamDir.x);
            for (ServerPlayer p : players) {
                Vec3 rel = new Vec3(p.getX() - boss.getX(), 0, p.getZ() - boss.getZ());
                double a = rel.dot(slamDir);
                double side = Math.abs(rel.dot(perp));
                if (a > 0 && a <= boss.getBbWidth() / 2 + 4 * TILE && side <= 1.5 * TILE) {
                    int roll = boss.getRandom().nextInt(BossMechanics.MAGGOT_SLAM_MAX + 1);
                    if (BossFx.protects(p, Prayer.PROTECT_FROM_MELEE)) {
                        roll = Math.min(roll, BossMechanics.MAGGOT_SLAM_PRAYED);
                    }
                    hurt(boss, p, "slam", roll);
                }
            }
            BossFx.world(boss).sendParticles(ParticleTypes.EXPLOSION, boss.getX() + slamDir.x * 4, boss.getY() + 0.3,
                    boss.getZ() + slamDir.z * 4, 3, 1.5, 0.2, 1.5, 0);
            for (int i = 0; i < 3; i++) {
                Mechanics.addHazard(BossFx.world(boss), boss.position().add(slamDir.scale(3 + 2 * i)).add(perp.scale((i - 1) * TILE)),
                        3, 40, boss);
            }
            punishUntil = now + 2; // the punish window: hit it while its head is down
            punished.clear();
        }

        private void tickLarvae(DataBossEntity boss, List<ServerPlayer> players) {
            carrion.removeIf(c -> now >= c.until());
            if (now % 4 == 0) {
                for (Carrion c : carrion) {
                    BossFx.world(boss).sendParticles(ParticleTypes.MYCELIUM, c.pos().x, c.pos().y + 0.2, c.pos().z, 3, 0.4, 0.05, 0.4, 0);
                }
            }
            Iterator<Larva> it = larvae.iterator();
            while (it.hasNext()) {
                Larva l = it.next();
                if (!l.mob.isAlive()) {
                    it.remove();
                    continue;
                }
                if (now < l.landsAt) {
                    BossFx.world(boss).sendParticles(ParticleTypes.CLOUD, l.mob.getX(), l.mob.getY() + 1.5, l.mob.getZ(), 2, 0.2, 0.2, 0.2, 0);
                    continue;
                }
                ServerPlayer touching = null;
                for (ServerPlayer p : players) {
                    if (flat(p.position(), l.mob.position()) < 1.4) {
                        touching = p;
                    }
                }
                if (touching != null) {
                    // It latches on: 3 damage, one more every tick, healing the King.
                    hurt(boss, touching, "larva", l.chip);
                    boss.heal(mcHp(boss, l.chip));
                    CombatAchievementService.flagNear(boss, BossMechanics.FLAG_MAGGOT_HEALED);
                    l.chip++;
                    continue;
                }
                // Crawls back and forth along its row.
                Vec3 c = homeVec();
                double nx = l.mob.getX() + l.dirX * 0.5;
                if (Math.abs(nx - c.x) > 10) {
                    l.dirX = -l.dirX;
                    nx = l.mob.getX() + l.dirX * 0.5;
                }
                l.mob.teleportTo(nx, l.mob.getY(), l.mob.getZ());
                eatCarrion(boss, l);
            }
        }

        /** A landed larva eats the carrion it crawls over: 1 hitpoint back to the King per pile (OSRS Wiki). */
        private void eatCarrion(DataBossEntity boss, Larva l) {
            Iterator<Carrion> it = carrion.iterator();
            while (it.hasNext()) {
                Carrion c = it.next();
                if (flat(l.mob.position(), c.pos()) < 1.2) {
                    it.remove();
                    boss.heal(mcHp(boss, BossMechanics.MAGGOT_CARRION_HEAL));
                    CombatAchievementService.flagNear(boss, BossMechanics.FLAG_MAGGOT_HEALED);
                    BossFx.world(boss).sendParticles(ParticleTypes.HEART, l.mob.getX(), l.mob.getY() + 0.6, l.mob.getZ(), 1, 0, 0, 0, 0);
                }
            }
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayer killer) {
            larvae.forEach(l -> l.mob.discard());
            if (watcher != null) {
                MinionRules.unwatch(watcher);
            }
        }

        @Override
        public void onRemoved(DataBossEntity boss) {
            onDeath(boss, null);
        }
    }
}
