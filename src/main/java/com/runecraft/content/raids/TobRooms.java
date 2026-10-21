package com.runecraft.content.raids;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.combat.CombatHooks;
import com.runecraft.content.systems.CombatAchievementService;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.raids.RaidMode;
import com.runecraft.osrs.content.raids.RaidScaling;
import com.runecraft.osrs.content.raids.RaidType;
import com.runecraft.osrs.content.raids.RaidsContent;
import com.runecraft.osrs.content.raids.RoomDef;
import com.runecraft.osrs.content.raids.TobBosses;
import com.runecraft.osrs.content.raids.TobNylocas;
import com.runecraft.osrs.content.raids.TobRules;
import com.runecraft.osrs.content.systems.CombatAchievements;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Theatre of Blood rooms (round 33 rebuild after the OSRS wiki's "Theatre of Blood/Strategies" and the bosses' pages;
 * numbers in {@link TobRules}). Each room keeps its own tick counter, so its timers follow the raid clock whether the
 * server or the self-test ({@link CoxTobWalk}) drives it. When a boss dies the whole team is restored (hitpoints, prayer,
 * special attack); the supply chests after the Pestilent Bloat and Sotetseg sell supplies for points.
 */
final class TobRooms {
    private TobRooms() {
    }

    /** Registers the hooks the ToB rooms need (called once from {@link CoxSupplyService#init}). */
    static void init() {
        CombatHooks.addModifier(new CombatHooks.PlayerAttackModifier() {
            @Override
            public double accuracy(ServerPlayer player, LivingEntity target, AttackType type) {
                // OSRS wiki / DPS calculator: the Dawnbringer ignores accuracy against Verzik's phase 1 (always a successful hit).
                return target != null && "verzik_vitur".equals(RaidInstance.creatureId(target))
                        && TobBosses.DAWNBRINGER.equals(Inv.idOf(player.getMainHandItem())) ? 1e6 : 1.0;
            }

            @Override
            public int afterRoll(ServerPlayer player, LivingEntity target, AttackType type, int damage) {
                if (target == null) {
                    return damage;
                }
                String id = RaidInstance.creatureId(target);
                RaidInstance raid = RaidManager.at(target);
                if (raid == null || raid.type != RaidType.TOB) {
                    return damage;
                }
                if ("blood_spawn".equals(id) && raid.mode() == RaidMode.HARD) {
                    // OSRS wiki: Hard mode blood spawns can be attacked but take no damage.
                    if (damage > 0) {
                        Msg.action(player, "The blood spawn is unharmed.");
                    }
                    return 0;
                }
                // OSRS: only poison and venom truly harm the Nylocas Athanatos (Verzik phase 2).
                Verzik verzik = "nylocas_athanatos".equals(id) ? Verzik.of(raid, target.position()) : null;
                return verzik != null ? verzik.athanatosHit(player, target, type, damage) : damage;
            }
        });
        // The Dawnbringer reaches Verzik from at most 7 tiles away (OSRS wiki: its attack range), special attack included.
        CombatHooks.addMonsterDamageGate((player, target) -> "verzik_vitur".equals(RaidInstance.creatureId(target))
                && TobBosses.DAWNBRINGER.equals(Inv.idOf(player.getMainHandItem())) && !Verzik.dawnbringerReaches(player, target)
                ? "You are too far away for the Dawnbringer to reach Verzik." : null);
        // Entry mode bandages (OSRS "Bandages (Theatre of Blood)"): food that also restores prayer and run energy, cures
        // poison, boosts combat stats and works as a stamina potion.
        com.runecraft.platform.UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.isEmpty() || !TobRules.BANDAGES.equals(Inv.idOf(stack))) {
                return net.minecraft.world.InteractionResultHolder.pass(stack);
            }
            if (!(player instanceof ServerPlayer sp)) {
                return net.minecraft.world.InteractionResultHolder.success(stack); // the server applies it
            }
            if (sp.getCooldowns().isOnCooldown(stack.getItem())) {
                return net.minecraft.world.InteractionResultHolder.success(stack);
            }
            useBandage(sp);
            stack.shrink(1);
            com.runecraft.skill.ConsumeCooldowns.consume(sp, TobRules.BANDAGES); // food: the shared eat delay and attack delay
            return net.minecraft.world.InteractionResultHolder.success(stack);
        });
    }

    /** A bandage's effects (OSRS wiki). */
    static void useBandage(ServerPlayer p) {
        var data = Players.data(p);
        var skills = data.skills();
        p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + TobRules.BANDAGE_HEAL));
        data.setPrayerPoints(Math.min(skills.level(Skill.PRAYER), data.prayerPoints() + TobRules.BANDAGE_PRAYER));
        com.runecraft.combat.PoisonService.cure(p, false, 0, ActionManager.gameTick());
        for (Skill s : new Skill[]{Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE}) {
            skills.setBoost(s, Math.max(skills.boost(s), TobRules.bandageMeleeBoost(skills.level(s))));
        }
        skills.setBoost(Skill.RANGED, Math.max(skills.boost(Skill.RANGED), TobRules.bandageRangedBoost(skills.level(Skill.RANGED))));
        skills.setBoost(Skill.MAGIC, Math.max(skills.boost(Skill.MAGIC), TobRules.BANDAGE_MAGIC_BOOST));
        // A stamina potion's effect (two minutes, as a dose) and 20% run energy.
        com.runecraft.content.systems.SysState.of(p).staminaUntil = ActionManager.gameTick() + 200;
        com.runecraft.content.systems.RunEnergyService.restore(p, TobRules.BANDAGE_RUN_PERCENT * 100);
        com.runecraft.network.ServerNetworking.sync(p);
        p.level().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_EAT, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.0f);
        Msg.action(p, "You use the bandages. You feel restored.");
    }

    /**
     * A player holding the Dawnbringer dies or leaves (OSRS wiki "Dawnbringer"): it drops to the floor where they stood so
     * a teammate can pick it up.
     */
    static void dropDawnbringer(RaidInstance raid, ServerPlayer p) {
        int n = Inv.count(p, TobBosses.DAWNBRINGER);
        if (n <= 0 || raid.roomAt(p.position()) < 0) {
            return;
        }
        Inv.remove(p, TobBosses.DAWNBRINGER, n);
        ItemEntity drop = new ItemEntity(raid.world, p.getX(), p.getY() + 0.2, p.getZ(), Inv.stack(TobBosses.DAWNBRINGER, 1));
        drop.setDefaultPickUpDelay();
        drop.setUnlimitedLifetime();
        Verzik room = Verzik.of(raid, p.position());
        if (raid.world.addFreshEntity(drop) && room != null) {
            room.dropped.add(drop);
        }
        raid.say("The Dawnbringer falls to the floor.");
    }

    /** The completion count key of a ToB mode ("tob_entry", "tob_normal", "tob_hard"). */
    static String modeKey(RaidMode mode) {
        return "tob_" + mode.name().toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * Why the party can't play Hard mode, or null (OSRS: every participant needs a Normal mode completion). Completions from
     * before the modes were counted apart count when they outnumber the Entry mode ones.
     */
    static String hardModeBlocker(RaidInstance raid) {
        RaidsState state = RaidsState.get(raid.world.getServer());
        for (UUID u : raid.party()) {
            int normal = state.completions(u, modeKey(RaidMode.NORMAL)) + state.completions(u, modeKey(RaidMode.HARD));
            int legacy = state.completions(u, RaidType.TOB.id()) - state.completions(u, modeKey(RaidMode.ENTRY))
                    - state.completions(u, modeKey(RaidMode.NORMAL)) - state.completions(u, modeKey(RaidMode.HARD));
            if (normal <= 0 && legacy <= 0) {
                return raid.name(u) + " needs to complete the Theatre of Blood in Normal mode before attempting Hard mode.";
            }
        }
        return null;
    }

    /** A party member died in the Theatre (called before they are moved out of the fight). */
    static void playerDied(RaidInstance raid, ServerPlayer p) {
        dropDawnbringer(raid, p);
    }

    /**
     * An Entry mode wipe (OSRS, March 2023): unlimited attempts, so the party is put back outside the boss room and
     * handed ten more bandages each, on top of those they carry (OSRS wiki "Theatre of Blood/Entry Mode": "you may choose
     * to die repeatedly on purpose to stack additional bandages").
     */
    static void entryWipe(RaidInstance raid) {
        for (ServerPlayer p : raid.players()) {
            Inv.give(p, TobRules.BANDAGES, TobRules.ENTRY_BANDAGES);
            Msg.game(p, "The vampyres send you ten bandages for your next attempt.");
        }
    }

    /** Moves passive crabs/spawn toward a point. */
    static void walk(LivingEntity e, Vec3 to) {
        if (e instanceof Mob mob) {
            mob.setTarget(null);
            if (mob.tickCount % 10 == 0 || mob.getNavigation().isDone()) {
                mob.getNavigation().moveTo(to.x, to.y, to.z, 1.0);
            }
        }
    }

    // ================================================================== the whole Theatre

    /** Supply chest state per raid: points (they carry over), what was bought per chest, deaths and MVPs since a chest. */
    private static final class Supplies {
        int unlocked;
        final Map<UUID, Integer> points = new HashMap<>();
        final Map<UUID, Integer> awardedFor = new HashMap<>();
        final Map<UUID, Integer> staminas = new HashMap<>();
        final Map<UUID, Integer> deathsAtChest = new HashMap<>();
        final Map<UUID, Integer> roomDeaths = new HashMap<>();
        final Set<UUID> mvp = new HashSet<>();
        final List<Map<UUID, Integer>> lastFights = new ArrayList<>();
        /** Where the opened supply chests stand: supplies are only bought standing at one. */
        final List<BlockPos> chests = new ArrayList<>();
    }

    /** How close (blocks) a player must stand to a supply chest to buy from it. */
    private static final double CHEST_REACH = 4.5;

    private static final Map<RaidInstance, Supplies> SUPPLIES = new WeakHashMap<>();

    private static Supplies supplies(RaidInstance raid) {
        return SUPPLIES.computeIfAbsent(raid, r -> new Supplies());
    }

    /**
     * A boss died (OSRS): the team's hitpoints, prayer and special attack energy are restored (drained stats are not),
     * and the room's deaths and MVP count towards the next supply chest.
     */
    static void bossCleared(RoomLogic room, Map<UUID, Integer> damage) {
        RaidInstance raid = room.raid;
        for (ServerPlayer p : raid.players()) {
            if (raid.isDowned(p)) {
                continue;
            }
            p.setHealth(p.getMaxHealth());
            var data = Players.data(p);
            data.setPrayerPoints(data.skills().level(Skill.PRAYER));
            com.runecraft.content.systems.SpecialAttackService.restoreEnergy(p, com.runecraft.osrs.content.systems.SpecialAttacks.MAX_ENERGY);
            com.runecraft.network.ServerNetworking.sync(p);
        }
        room.say("Your hitpoints, prayer and special attack energy are restored.");
        Supplies s = supplies(raid);
        UUID mvp = null;
        for (Map.Entry<UUID, Integer> e : damage.entrySet()) {
            if (mvp == null || e.getValue() > damage.get(mvp)) {
                mvp = e.getKey();
            }
        }
        if (mvp != null) {
            s.mvp.add(mvp);
        }
        Map<UUID, Integer> fight = new HashMap<>();
        for (UUID u : raid.party()) {
            int total = raid.deaths(u);
            fight.put(u, total - s.roomDeaths.getOrDefault(u, 0));
            s.roomDeaths.put(u, total);
        }
        s.lastFights.add(fight);
        while (s.lastFights.size() > 2) {
            s.lastFights.remove(0);
        }
    }

    /** Opens the next supply chest in a cleared room (after the Bloat and Sotetseg). */
    static void placeChest(RoomLogic room) {
        Supplies s = supplies(room.raid);
        s.unlocked++;
        BlockPos at = room.at(RaidBuilder.HALF - 3, 4);
        RaidBuilder.set(room.world(), at, RaidBuilder.module(RaidsContent.SUPPLIES));
        s.chests.add(at);
        room.say("A chest of supplies from the entertained vampyres appears. Spend your points on it before you go on.");
    }

    /** The supply chest (OSRS): points for the fights since the last chest, then the stock to buy from. */
    static void supplyChest(RaidInstance raid, ServerPlayer p) {
        Supplies s = supplies(raid);
        if (raid.roomAt(p.position()) == 0) {
            Msg.game(p, "The storage unit is for your own items. The vampyres leave supply chests after the Pestilent Bloat and Sotetseg.");
            return;
        }
        if (s.unlocked == 0) {
            Msg.game(p, "The chest is empty. The vampyres leave supplies after the Pestilent Bloat and Sotetseg.");
            return;
        }
        UUID u = p.getUUID();
        if (s.awardedFor.getOrDefault(u, 0) < s.unlocked) {
            s.awardedFor.put(u, s.unlocked);
            s.staminas.remove(u);
            int deathsInLastTwo = 0;
            for (Map<UUID, Integer> f : s.lastFights) {
                deathsInLastTwo += f.getOrDefault(u, 0) > 0 ? 1 : 0;
            }
            int deaths = raid.deaths(u) - s.deathsAtChest.getOrDefault(u, 0);
            s.deathsAtChest.put(u, raid.deaths(u));
            boolean mvp = s.mvp.remove(u);
            if (raid.mode() == RaidMode.ENTRY) {
                // OSRS Entry mode: ten bandages instead of points; the chest holds no more than ten.
                int have = Inv.count(p, TobRules.BANDAGES);
                Inv.give(p, TobRules.BANDAGES, TobRules.ENTRY_BANDAGES);
                Msg.game(p, "You take ten bandages from the chest." + (have > 0 ? "" : " Use them to heal and boost yourself."));
                return;
            }
            if (TobRules.onionOnly(deathsInLastTwo)) {
                Inv.give(p, "onion", 1);
                Msg.red(p, "You died in both of the last two fights: the vampyres leave you only an onion.");
                return;
            }
            int got = TobRules.chestPoints(TobRules.performance(deaths, mvp), raid.mode(), new java.util.Random(p.getRandom().nextLong()));
            s.points.merge(u, got, Integer::sum);
            Msg.game(p, "The vampyres were entertained: you earn " + got + " supply points.");
        }
        showStock(p, s.points.getOrDefault(u, 0));
    }

    private static void showStock(ServerPlayer p, int points) {
        p.displayClientMessage(Component.literal("Supply chest - " + points + " point" + (points == 1 ? "" : "s") + " to spend:")
                .withStyle(ChatFormatting.GOLD), false);
        MutableComponent line = Component.literal("");
        for (TobRules.Supply s : TobRules.SUPPLIES) {
            line.append(Component.literal("[" + s.name() + " " + s.cost() + "] ").withStyle(st -> st.withColor(ChatFormatting.GREEN)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/raids buy " + s.item()))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            Component.literal(s.cost() + " point" + (s.cost() == 1 ? "" : "s") + (s.perChest() > 0 ? ", one per chest" : ""))))));
        }
        p.displayClientMessage(line, false);
    }

    /** Buys one item from an open supply chest ({@code /raids buy <item>}). */
    static int buy(ServerPlayer p, String item) {
        RaidInstance raid = RaidManager.of(p);
        if (raid == null || raid.type != RaidType.TOB) {
            Msg.game(p, "There is no supply chest here.");
            return 0;
        }
        Supplies s = supplies(raid);
        TobRules.Supply supply = TobRules.supply(item);
        if (raid.isDowned(p)) {
            Msg.game(p, "You can't reach the supply chest while you're dead.");
            return 0;
        }
        // OSRS: supplies come only from the chests themselves, after the Bloat and Sotetseg.
        boolean atChest = false;
        for (BlockPos c : s.chests) {
            atChest |= raid.roomAt(p.position()) == raid.roomAt(Vec3.atCenterOf(c))
                    && p.position().distanceToSqr(Vec3.atBottomCenterOf(c)) <= CHEST_REACH * CHEST_REACH;
        }
        if (!atChest) {
            Msg.game(p, "You need to be at a supply chest to take supplies.");
            return 0;
        }
        if (s.unlocked == 0 || s.awardedFor.getOrDefault(p.getUUID(), 0) < s.unlocked) {
            Msg.game(p, "Open the supply chest first.");
            return 0;
        }
        if (supply == null) {
            Msg.game(p, "The chest has no such supply.");
            return 0;
        }
        int points = s.points.getOrDefault(p.getUUID(), 0);
        if (points < supply.cost()) {
            Msg.game(p, "You need " + supply.cost() + " points for that; you have " + points + ".");
            return 0;
        }
        if (supply.perChest() > 0 && s.staminas.getOrDefault(p.getUUID(), 0) >= supply.perChest()) {
            Msg.game(p, "You can only take one " + supply.name() + " from each chest.");
            return 0;
        }
        s.points.put(p.getUUID(), points - supply.cost());
        if (supply.perChest() > 0) {
            s.staminas.merge(p.getUUID(), 1, Integer::sum);
        }
        raid.markSuppliesTaken(p.getUUID()); // "Pack Like a Yak": nothing bought from a supply chest
        Inv.give(p, supply.item(), 1);
        Msg.game(p, "You take a " + supply.name() + ". (" + (points - supply.cost()) + " points left)");
        return 1;
    }

    /** Supply points a player has left (self-test). */
    static int points(RaidInstance raid, ServerPlayer p) {
        return supplies(raid).points.getOrDefault(p.getUUID(), 0);
    }

    /** A ToB room that tracks each player's damage (the room's MVP feeds the supply points). */
    abstract static class TobRoom extends RoomLogic implements CoxTobWalk.Walkable {
        final Map<UUID, Integer> damage = new HashMap<>();
        int t;
        /** The room's "Perfect" combat achievement flag (set once the condition fails), or null. */
        private final String perfectFlag;
        private boolean imperfect;

        TobRoom(RaidInstance raid, RoomDef def, int index, String perfectFlag) {
            super(raid, def, index);
            this.perfectFlag = perfectFlag;
        }

        /** The room's "Perfect" task can no longer be done this fight. */
        void fail() {
            imperfect = true;
        }

        /** A hit from one of the boss's attacks: any damage fails the room's "Perfect" task. */
        int hurt(LivingEntity source, ServerPlayer p, int dmg, AttackType style, boolean piercing) {
            int dealt = RaidEffects.hit(source, p, dmg, style, piercing);
            if (dealt > 0) {
                fail();
            }
            return dealt;
        }

        /** {@link #hurt} for damage already set for the mode (no Entry or Hard mode multiplier on top). */
        int hurtRaw(LivingEntity source, ServerPlayer p, int dmg) {
            int dealt = RaidEffects.hitRaw(source, p, dmg);
            if (dealt > 0) {
                fail();
            }
            return dealt;
        }

        /**
         * Keeps the failed "Perfect" flag on everyone fighting the boss: flags live on the current fight, which restarts when
         * the player's target changes (a Matomenos, the next phase), so it is set again every tick.
         */
        void reflag(LivingEntity boss) {
            if (imperfect && perfectFlag != null && alive(boss)) {
                CombatAchievementService.flagNear(boss, perfectFlag);
            }
        }

        @Override
        public void onCreatureHit(ServerPlayer player, LivingEntity target, int dealt) {
            damage.merge(player.getUUID(), dealt, Integer::sum);
        }

        @Override
        protected void onComplete() {
            bossCleared(this, damage);
            cleanup();
        }
    }

    // ================================================================== The Maiden of Sugadinti

    /**
     * The Maiden of Sugadinti (OSRS wiki). Every 10 ticks she attacks: Blackstorm at the closest player (always lands, up to
     * 36.5 + 3.5 per Matomenos that reached her, halved by Protect from Magic, half the time draining the target's combat
     * stats), or blood splats (a spot under every player and two more around one of them: 10 + 2 per Matomenos, healing
     * her by the same; a spot becomes a blood spawn 10% of the time, 20% if stepped on), after which she throws Blackstorm
     * twice. At 70%, 50% and 30% she calls two Nylocas Matomenos per player (ten in Hard mode); one that reaches her heals
     * her by twice its hitpoints. Blood spawns leave trails that hurt and heal her, drying in 20-30 seconds.
     */
    static final class Maiden extends TobRoom {
        private static final net.minecraft.core.particles.DustParticleOptions TRAIL =
                new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.55f, 0.0f, 0.0f), 1.5f);
        private DataBossEntity maiden;
        private final Set<Integer> thresholds = new HashSet<>();
        private int fed;
        private int splatCooldown;
        private final Map<Vec3, Integer> trail = new HashMap<>();
        private final Set<BlockPos> trailTiles = new HashSet<>();
        private int matomenosCalled;
        /** Damage blood trails dealt, spots in the last throw and blood spawns made (self-test). */
        private int trailDamage;
        private int lastSplats;
        private int bloodSpawns;

        Maiden(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index, CombatAchievements.FLAG_MAIDEN_IMPERFECT);
        }

        @Override
        public void decorate() {
            for (int i = 0; i < 18; i++) {
                RaidBuilder.set(world(), center().offset(rnd().nextInt(36) - 18, 0, rnd().nextInt(36) - 18),
                        Blocks.RED_CONCRETE.defaultBlockState());
            }
            for (int z = -16; z <= 16; z += 8) {
                RaidBuilder.fill(world(), center().getX() + 20, center().getY() + 6, center().getZ() + z,
                        center().getX() + 20, center().getY() + 9, center().getZ() + z, Blocks.CHAIN.defaultBlockState());
            }
        }

        @Override
        public void start() {
            thresholds.clear();
            fed = 0;
            splatCooldown = 0;
            matomenosCalled = 0;
            bloodSpawns = 0;
            trail.clear();
            trailTiles.clear();
            trailMarked.clear();
            maiden = spawnBoss("the_maiden_of_sugadinti", at(15, 0));
            if (maiden != null) {
                maiden.setNoAi(true);
                anchor(maiden);
            }
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            t++;
            if (!alive(maiden)) {
                return;
            }
            // Hard mode (OSRS): each of the first five Matomenos to reach her speeds up her attacks. // approx: a tick each
            int speed = TobRules.MAIDEN_ATTACK_SPEED - (hard() ? Math.min(5, fed) : 0);
            if (t % speed == 0) {
                attack();
            }
            double hp = raid.healthFraction(maiden);
            for (int th : new int[]{70, 50, 30}) {
                if (hp * 100 <= th && thresholds.add(th)) {
                    int n = RaidScaling.tobMaidenNylocas(raid.partySize(), hard());
                    for (int i = 0; i < n; i++) {
                        int z = (i % 2 == 0 ? -1 : 1) * 18;
                        spawnMob("nylocas_matomenos", at(4 + (i / 2) * 3, z), false);
                    }
                    matomenosCalled += n;
                    warn("The Maiden calls upon the Nylocas! (" + th + "%)");
                }
            }
            int reached = healers("nylocas_matomenos", maiden, 2.0, "A Nylocas Matomenos reaches the Maiden and heals her!");
            if (reached > 0) {
                fed += reached;
                fail();
            }
            reflag(maiden);
            List<LivingEntity> spawns = living("blood_spawn");
            for (LivingEntity sp : spawns) {
                if (t % 2 == 0) {
                    trail.put(sp.position(), hard() ? Integer.MAX_VALUE : t + TobRules.MAIDEN_TRAIL_TICKS);
                    if (trailTiles.add(sp.blockPosition())) {
                        CombatAchievementService.countNear(maiden, CombatAchievements.COUNT_MAIDEN_TRAIL);
                    }
                    markTrail(sp.position());
                }
                if (sp.tickCount > 200) {
                    CombatAchievementService.flagNear(maiden, CombatAchievements.FLAG_BLOODSPAWN_OLD);
                }
                if (sp.tickCount % 60 == 0) {
                    TobRooms.walk(sp, vec(rnd().nextInt(30) - 15, rnd().nextInt(30) - 15));
                }
            }
            trail.entrySet().removeIf(e -> e.getValue() < t);
            // Every blood trail tile shows (dark red), not only one stood in. // playability: OSRS draws the trail on the floor
            for (Vec3 tr : trail.keySet()) {
                world().sendParticles(TRAIL, tr.x, tr.y + 0.1, tr.z, 1, 0.25, 0, 0.25, 0);
            }
            for (ServerPlayer p : players()) {
                for (Vec3 tr : trail.keySet()) {
                    if (RaidEffects.horizontalDistSq(p.position(), tr) < 0.8 * 0.8) {
                        if (t % 2 == 0) {
                            world().sendParticles(ParticleTypes.DAMAGE_INDICATOR, tr.x, tr.y + 0.3, tr.z, 2, 0.2, 0.1, 0.2, 0);
                        }
                        // OSRS wiki "Blood spawn": a blood trail hits for 5-13 (2-5 in Entry mode) and heals the Maiden by it.
                        int min = TobRules.bloodTrailMin(raid.mode());
                        int dealt = hurtRaw(maiden, p, min + rnd().nextInt(TobRules.bloodTrailMax(raid.mode()) - min + 1));
                        raid.heal(maiden, dealt);
                        trailDamage += dealt;
                        break;
                    }
                }
            }
        }

        /** The room tick each blood-trail floor block stays marked until (Hard mode: for good). */
        private final Map<BlockPos, Integer> trailMarked = new HashMap<>();

        /**
         * A blood trail's tile marked on the floor for as long as the trail lies there (round 43; Hard mode: the pools never
         * despawn, OSRS wiki). // playability: OSRS paints the blood on the tiles
         */
        private void markTrail(Vec3 at) {
            BlockPos b = BlockPos.containing(at);
            int until = hard() ? Integer.MAX_VALUE : t + TobRules.MAIDEN_TRAIL_TICKS;
            Integer marked = trailMarked.get(b);
            if (marked != null && marked >= until - 4) {
                return;
            }
            trailMarked.put(b, until);
            RaidFloor.disc(world(), at, 0.1, RaidFloor.BLOOD, hard() ? 0 : TobRules.MAIDEN_TRAIL_TICKS, maiden);
        }

        private ServerPlayer closest() {
            ServerPlayer best = null;
            for (ServerPlayer p : players()) {
                if (best == null || p.distanceToSqr(maiden) < best.distanceToSqr(maiden)) {
                    best = p;
                }
            }
            return best;
        }

        /** The player standing furthest from the Maiden (OSRS: the extra blood spots go to them). */
        private ServerPlayer furthest() {
            ServerPlayer best = null;
            for (ServerPlayer p : players()) {
                if (best == null || p.distanceToSqr(maiden) > best.distanceToSqr(maiden)) {
                    best = p;
                }
            }
            return best;
        }

        private void attack() {
            ServerPlayer target = closest();
            if (target == null) {
                return;
            }
            if (splatCooldown <= 0 && rnd().nextBoolean()) {
                splatCooldown = TobRules.MAIDEN_SPLAT_COOLDOWN;
                bloodSplats();
                return;
            }
            splatCooldown--;
            int max = TobRules.maidenBlackstormMax(fed);
            // Blackstorm flies as a magic projectile, drawn all the way; the prayer is read as it lands. // playability: it was
            // a line of particles drawn once
            DataBossEntity source = maiden;
            java.util.function.BooleanSupplier current = current();
            RaidEffects.projectile(world(), maiden.getEyePosition(), target, AttackType.MAGIC, 2,
                    () -> alive(source) && current.getAsBoolean() && inside(target.position()), () -> {
                int dmg = 1 + rnd().nextInt(max);
                boolean prayed = RaidEffects.protectedFrom(target, AttackType.MAGIC);
                if (prayed) {
                    dmg /= 2;
                }
                int dealt = RaidEffects.hit(maiden, target, dmg, null, true);
                if (dealt > 0 && !prayed) {
                    fail(); // "Perfect Maiden": no damage off prayer
                }
                if (rnd().nextBoolean()) {
                    drain(target, TobRules.maidenDrain(dealt));
                }
            });
        }

        /** Blackstorm's drain hits the stats of the target's best attack style. // approx: chosen from the weapon */
        private void drain(ServerPlayer p, int amount) {
            if (amount <= 0) {
                return;
            }
            String weapon = Inv.idOf(p.getMainHandItem());
            Skill[] skills = weapon.contains("bow") || weapon.contains("dart") || weapon.contains("blowpipe") || weapon.contains("chinchompa")
                    ? new Skill[]{Skill.RANGED} : weapon.contains("staff") || weapon.contains("trident") || weapon.contains("wand")
                    || weapon.contains("sceptre") ? new Skill[]{Skill.MAGIC} : new Skill[]{Skill.ATTACK, Skill.STRENGTH};
            var data = Players.data(p).skills();
            for (Skill s : skills) {
                data.setBoost(s, data.boost(s) - amount);
            }
            com.runecraft.network.ServerNetworking.sync(p);
            Msg.red(p, "The Maiden's Blackstorm saps your strength!");
        }

        /**
         * Blood Splat (OSRS): a spot under every player and two more in a 5x5 area around the player standing furthest
         * away. Each spot becomes a blood spawn 10% of the time (20% if stood on); a throw everyone dodged makes at most one
         * (August 2021), and no more than eight blood spawns are about at once.
         */
        private void bloodSplats() {
            List<ServerPlayer> players = players();
            if (players.isEmpty()) {
                return;
            }
            List<Vec3> spots = new ArrayList<>();
            for (ServerPlayer p : players) {
                spots.add(p.position());
            }
            ServerPlayer far = furthest();
            for (int i = 0; i < TobRules.MAIDEN_EXTRA_SPLATS; i++) {
                spots.add(far.position().add(rnd().nextInt(5) - 2, 0, rnd().nextInt(5) - 2));
            }
            lastSplats = spots.size();
            // Each spot is a raid strike (round 42): outlined from the throw at its true size, redrawn until it lands, recorded.
            // The last one to land decides the blood spawns. // playability: OSRS marks the tiles once, a tick or two ahead
            boolean[] stepped = new boolean[spots.size()];
            DataBossEntity source = maiden;
            java.util.function.BooleanSupplier current = current();
            for (int i = 0; i < spots.size(); i++) {
                int k = i;
                Vec3 at = spots.get(i);
                RaidEffects.Marker m = RaidEffects.mark(world(), at, 0.9).delay(3).source(source).warn(ParticleTypes.DAMAGE_INDICATOR)
                        .floor(RaidFloor.BLOOD).live(() -> alive(source) && current.getAsBoolean())
                        .onHit(p -> {
                            stepped[k] = true;
                            int dealt = hurt(source, p, TobRules.maidenSplat(fed), null, true);
                            raid.heal(source, dealt);
                            var data = Players.data(p);
                            data.setPrayerPoints(Math.max(0, data.prayerPoints() - 5)); // approx: "prayer points are also drained"
                        });
                if (i == spots.size() - 1) {
                    m.onLand(() -> splatSpawns(spots, stepped));
                }
                m.start();
            }
        }

        /** After a blood throw landed: each spot becomes a blood spawn by chance (more likely where stepped on), capped. */
        private void splatSpawns(List<Vec3> spots, boolean[] stepped) {
            boolean anyStepped = false;
            List<Vec3> spawnAt = new ArrayList<>();
            for (int i = 0; i < spots.size(); i++) {
                anyStepped |= stepped[i];
                if (rnd().nextDouble() < TobRules.bloodSpawnChance(stepped[i])) {
                    spawnAt.add(spots.get(i));
                }
            }
            int allowed = TobRules.bloodSpawnsFromThrow(anyStepped, spawnAt.size());
            // At most eight blood spawns appear in the whole fight (OSRS wiki "Blood spawn"), killed ones included.
            for (int i = 0; i < allowed && bloodSpawns < TobRules.MAIDEN_BLOOD_SPAWN_CAP; i++) {
                spawnMob("blood_spawn", BlockPos.containing(spawnAt.get(i)), false);
                bloodSpawns++;
            }
        }

        @Override
        public void cleanup() {
            super.cleanup();
            living("nylocas_matomenos").forEach(LivingEntity::discard);
            living("blood_spawn").forEach(LivingEntity::discard);
        }

        @Override
        public String walk(CoxTobWalk w) {
            w.expect(alive(maiden), "the Maiden did not spawn");
            int guard = 0;
            boolean sawNylocas = false;
            boolean fedOne = false;
            boolean checkedSpawn = false;
            while (alive(maiden) && guard++ < 800) {
                List<LivingEntity> nylos = living("nylocas_matomenos");
                sawNylocas |= !nylos.isEmpty();
                if (!fedOne && !nylos.isEmpty()) {
                    // Let one Matomenos reach her (OSRS: it heals her by twice its hitpoints and strengthens her attacks).
                    fedOne = true;
                    LivingEntity n = nylos.get(0);
                    int before = raid.hitpoints(maiden);
                    int nyloHp = raid.hitpoints(n);
                    n.teleportTo(maiden.getX() - 1.5, maiden.getY(), maiden.getZ());
                    w.until(() -> !alive(n), 5, "a Nylocas Matomenos next to the Maiden did not reach her");
                    w.expect(fed == 1 && raid.hitpoints(maiden) - before >= Math.min(2 * nyloHp, raid.maxHitpoints(maiden) - before),
                            "a Matomenos that reached her healed " + (raid.hitpoints(maiden) - before) + " (expected 2 x " + nyloHp
                                    + "), fed " + fed);
                    w.expect(TobRules.maidenBlackstormMax(fed) == 40, "one Matomenos fed did not raise Blackstorm to 40");
                    nylos = living("nylocas_matomenos");
                }
                for (LivingEntity n : nylos) {
                    w.kill(n, AttackType.MAGIC, "a Nylocas Matomenos");
                }
                if (hard() && !checkedSpawn) {
                    // Hard mode: blood spawns can be attacked but take no damage (OSRS wiki).
                    checkedSpawn = true;
                    LivingEntity spawn = living("blood_spawn").isEmpty() ? spawnMob("blood_spawn", at(0, 6), false) : living("blood_spawn").get(0);
                    w.expect(w.hit(spawn, 20, AttackType.RANGED) == 0, "a Hard mode blood spawn took damage");
                }
                w.expect(bloodSpawns <= TobRules.MAIDEN_BLOOD_SPAWN_CAP, "the Maiden made " + bloodSpawns + " blood spawns (at most eight a fight)");
                w.hit(maiden, Math.max(10, raid.maxHitpoints(maiden) / 12), AttackType.SLASH);
                w.tick(1);
            }
            w.expect(!alive(maiden), "the Maiden did not die");
            w.expect(fedOne, "no Matomenos was let through to the Maiden");
            w.expect(sawNylocas && matomenosCalled >= 3 * RaidScaling.tobMaidenNylocas(raid.partySize(), hard()),
                    "the Maiden called " + matomenosCalled + " Matomenos (expected three groups)");
            w.expect(lastSplats == 0 || lastSplats == raid.partySize() + TobRules.MAIDEN_EXTRA_SPLATS,
                    "a blood splat throw had " + lastSplats + " spots (one per player and two more at the furthest)");
            return matomenosCalled + " Matomenos called, " + bloodSpawns + " blood spawn(s), trails dealt " + trailDamage;
        }
    }

    // ================================================================== The Pestilent Bloat

    /**
     * The Pestilent Bloat (OSRS wiki): it walks its path round the central tank, its flies hitting everyone in sight (and
     * anyone in sight of them) for up to 20 a tick (25% less with Protect from Missiles) and halving damage taken; below
     * 90% health hands fall from the ceiling (30-50 and a stun). After 34-42 ticks (38-46 the first time; +4 if nobody hit
     * it during its last shutdown) it shuts down for 32 ticks and stomps on the 30th (40-80 to anyone close). It may turn
     * around every 32 ticks (16 in Hard mode) and can't go down for 5 ticks after a turn. It runs between 40% and 60%
     * health.
     */
    static final class Bloat extends TobRoom {
        /** The stomp's reach is outlined this many game ticks before it lands. */
        private static final int STOMP_WARNING_TICKS = com.runecraft.osrs.ArenaTiming.STRIKE_FLOOR_TICKS;
        private DataBossEntity bloat;
        private boolean walking = true;
        private int stateTicks;
        private int stateLength = RaidScaling.TOB_BLOAT_FIRST_WALK;
        private int waypoint;
        private int direction = 1;
        private int turnTicks;
        private boolean hitWhileDown;
        private int hitsBelow40;
        private int shutdowns;
        private int stompRestores;
        private boolean stacked;

        Bloat(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index, CombatAchievements.FLAG_BLOAT_IMPERFECT);
        }

        private Vec3 waypoint(int i) {
            int[][] corners = {{-11, -11}, {11, -11}, {11, 11}, {-11, 11}};
            int[] c = corners[Math.floorMod(i, 4)];
            return vec(c[0], c[1]);
        }

        @Override
        public void decorate() {
            RaidBuilder.pillar(world(), center(), 4, RaidBuilder.HEIGHT, Blocks.DEEPSLATE_BRICKS.defaultBlockState());
            for (int i = 0; i < 6; i++) {
                RaidBuilder.set(world(), center().offset(rnd().nextInt(36) - 18, RaidBuilder.HEIGHT, rnd().nextInt(36) - 18),
                        Blocks.CHAIN.defaultBlockState());
            }
        }

        @Override
        public void start() {
            walking = true;
            stateTicks = 0;
            waypoint = 0;
            direction = rnd().nextBoolean() ? 1 : -1;
            turnTicks = 0;
            hitsBelow40 = 0;
            shutdowns = 0;
            stacked = false;
            stateLength = RaidScaling.TOB_BLOAT_FIRST_WALK + rnd().nextInt(9);
            bloat = spawnBoss("pestilent_bloat", BlockPos.containing(waypoint(0)));
        }

        @Override
        public void bossTick(DataBossEntity boss) {
            boss.setTarget(null);
            if (!walking) {
                boss.getNavigation().stop();
                return;
            }
            Vec3 wp = waypoint(waypoint);
            if (boss.position().distanceToSqr(wp) < 2.0) {
                waypoint += direction;
                wp = waypoint(waypoint);
            }
            if (boss.tickCount % 10 == 0 || boss.getNavigation().isDone()) {
                boolean runs = TobRules.bloatRuns(raid.healthFraction(boss), hitsBelow40);
                boss.getNavigation().moveTo(wp.x, wp.y, wp.z, runs ? 2.0 : 1.3);
            }
        }

        private void maybeTurn() {
            int every = hard() ? RaidScaling.TOB_BLOAT_TURN_HARD : RaidScaling.TOB_BLOAT_TURN;
            if (++turnTicks < every) {
                return;
            }
            turnTicks = 0;
            if (rnd().nextInt(hard() ? 2 : 4) != 0) { // approx: the turn chance isn't published
                return;
            }
            direction = -direction;
            waypoint += direction;
            if (stateLength - stateTicks < 5) {
                stateLength = stateTicks + 5;
            }
            warn("The Bloat turns around!");
        }

        @Override
        public int bossIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (!walking) {
                hitWhileDown = true;
            }
            if (raid.healthFraction(boss) < 0.4) {
                hitsBelow40++;
            }
            // OSRS: the Bloat takes 50% less damage (rounded down) while it moves.
            return walking ? damage / 2 : damage;
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            t++;
            if (!alive(bloat)) {
                return;
            }
            reflag(bloat);
            stateTicks++;
            List<ServerPlayer> players = players();
            if (hard()) {
                // "Personal Space" (Hard mode CA): nobody may stand on top of anyone else during the fight.
                Set<BlockPos> tiles = new HashSet<>();
                for (ServerPlayer p : players) {
                    stacked |= !tiles.add(p.blockPosition());
                }
                if (stacked) {
                    CombatAchievementService.flagNear(bloat, CombatAchievements.FLAG_BLOAT_STACKED);
                }
            }
            if (walking) {
                for (ServerPlayer p : fliesTargets(players)) {
                    int dmg = rnd().nextInt(TobRules.BLOAT_FLIES_MAX + 1);
                    if (RaidEffects.protectedFrom(p, AttackType.RANGED)) {
                        dmg = dmg * 3 / 4;
                    }
                    hurt(bloat, p, dmg, null, true);
                }
                if ((hard() || raid.healthFraction(bloat) < TobRules.BLOAT_HANDS_BELOW) && t % 3 == 0 && !players.isEmpty()) {
                    hands(players);
                }
                maybeTurn();
                if (stateTicks >= stateLength) {
                    walking = false;
                    stateTicks = 0;
                    stateLength = RaidScaling.TOB_BLOAT_DOWN;
                    hitWhileDown = false;
                    shutdowns++;
                    say("The Bloat shuts down. Attack it now!");
                    CombatAchievementService.countNear(bloat, CombatAchievements.COUNT_BLOAT_SHUTDOWN);
                }
            } else {
                world().sendParticles(ParticleTypes.SMOKE, bloat.getX(), bloat.getY() + 2, bloat.getZ(), 4, 0.8, 0.4, 0.8, 0.01);
                if (hard() && t % 3 == 0 && !players.isEmpty()) {
                    hands(players);
                }
                if (stateTicks == TobRules.BLOAT_STOMP_TICK - STOMP_WARNING_TICKS) {
                    // The stomp on its 30th shutdown tick, as a raid strike outlined around the Bloat for its last moments.
                    // // playability: OSRS shows the Bloat rearing up; here its reach is drawn from 3 ticks out
                    DataBossEntity source = bloat;
                    Vec3 at = bloat.position();
                    RaidEffects.telegraph(players, "The Bloat is about to stomp - get away from it!");
                    RaidEffects.mark(world(), at, 4.5).delay(STOMP_WARNING_TICKS).source(source).warn(ParticleTypes.SMOKE)
                            .floor(RaidFloor.DANGER).live(() -> alive(source) && !walking)
                            .onHit(p -> {
                                hurt(source, p, TobRules.BLOAT_STOMP_MIN + rnd().nextInt(TobRules.BLOAT_STOMP_MAX - TobRules.BLOAT_STOMP_MIN + 1),
                                        null, true);
                                Vec3 push = p.position().subtract(source.position()).multiply(1, 0, 1);
                                push = push.lengthSqr() < 1e-4 ? new Vec3(1.2, 0, 0) : push.normalize().scale(1.2);
                                p.push(push.x, 0.5, push.z);
                                p.hurtMarked = true;
                                Msg.red(p, "The Bloat stomps on you!");
                            })
                            .onLand(() -> {
                                world().sendParticles(ParticleTypes.EXPLOSION_EMITTER, source.getX(), source.getY(), source.getZ(), 1, 0, 0, 0, 0);
                                // OSRS: the stomp restores any Defence drained from it.
                                if (com.runecraft.content.systems.SysCombat.drainedStatLevel(source, Skill.DEFENCE)
                                        < com.runecraft.content.systems.SysCombat.baseStatLevel(source, Skill.DEFENCE)) {
                                    com.runecraft.content.systems.SysCombat.drain(source).defence = 0;
                                    stompRestores++;
                                }
                            }).start();
                }
                if (stateTicks >= stateLength) {
                    walking = true;
                    stateTicks = 0;
                    stateLength = RaidScaling.TOB_BLOAT_WALK + rnd().nextInt(9) + (hitWhileDown ? 0 : TobRules.BLOAT_UNATTACKED_BONUS);
                }
            }
        }

        /**
         * The flies hit everyone in the Bloat's sight and, in turn, anyone in sight of a player they hit (OSRS), so hiding
         * behind the tank only works when nobody can see a team-mate in the open.
         */
        List<ServerPlayer> fliesTargets(List<ServerPlayer> players) {
            List<ServerPlayer> struck = new ArrayList<>();
            for (ServerPlayer p : players) {
                if (canSee(bloat, p)) {
                    struck.add(p);
                }
            }
            for (int i = 0; i < struck.size(); i++) {
                ServerPlayer seen = struck.get(i);
                for (ServerPlayer q : players) {
                    if (!struck.contains(q) && canSee(seen, q)) {
                        struck.add(q);
                    }
                }
            }
            return struck;
        }

        /**
         * Hands fall from the ceiling: 30-50 and a short stun. Each lands as a raid strike, its spot outlined from when it
         * starts to fall. // playability: OSRS shows the limb's shadow; here the spot's true size, redrawn until it lands
         */
        private void hands(List<ServerPlayer> players) {
            ServerPlayer p = players.get(rnd().nextInt(players.size()));
            Vec3 at = p.position().add(rnd().nextInt(5) - 2, 0, rnd().nextInt(5) - 2);
            world().sendParticles(ParticleTypes.FALLING_LAVA, at.x, at.y + 3, at.z, 10, 0.4, 0.2, 0.4, 0);
            DataBossEntity source = bloat;
            // Round 43: its shadow marked on the floor, and the limb seen falling from the ceiling onto it. // playability: OSRS
            // shows the falling limb's shadow
            long due = RaidEffects.now() + 3;
            RaidEffects.cue(3, center().getCenter(), () -> {
                if (!alive(source)) {
                    return false;
                }
                double left = (due * com.runecraft.osrs.ArenaTiming.MC_PER_GAME_TICK - RaidEffects.clockMc())
                        / (3.0 * com.runecraft.osrs.ArenaTiming.MC_PER_GAME_TICK);
                double h = 0.5 + Math.max(0, left) * (RaidBuilder.HEIGHT - 2);
                world().sendParticles(LIMB, at.x, at.y + h, at.z, 6, 0.25, 0.25, 0.25, 0);
                return true;
            });
            RaidEffects.mark(world(), at, 1.0).delay(3).source(source).warn(ParticleTypes.FALLING_LAVA).floor(RaidFloor.DANGER).live(current()).onHit(q -> {
                handsLanded++;
                hurt(source, q, TobRules.BLOAT_HANDS_MIN + rnd().nextInt(TobRules.BLOAT_HANDS_MAX - TobRules.BLOAT_HANDS_MIN + 1), null, true);
                com.runecraft.content.systems.SysCombat.freeze(q, 3);
            }).start();
        }

        /** Falling hands that landed on a player (self-test). */
        int handsLanded;
        private static final net.minecraft.core.particles.DustParticleOptions LIMB =
                new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.45f, 0.55f, 0.25f), 1.8f);

        @Override
        protected void onComplete() {
            super.onComplete();
            placeChest(this);
        }

        @Override
        public String walk(CoxTobWalk w) {
            w.expect(alive(bloat) && walking, "the Bloat did not start walking");
            int halved = w.hit(bloat, 20, AttackType.SLASH);
            w.expect(halved <= 10, "the walking Bloat took " + halved + " from a 20 hit (expected half)");
            w.until(() -> !walking, 60, "the Bloat never shut down");
            // OSRS: the stomp at the end of a shutdown restores any Defence drained from it.
            int baseDef = com.runecraft.content.systems.SysCombat.baseStatLevel(bloat, Skill.DEFENCE);
            com.runecraft.content.systems.SysCombat.drainDefence(bloat, 50);
            w.expect(com.runecraft.content.systems.SysCombat.drainedStatLevel(bloat, Skill.DEFENCE) < baseDef, "could not drain the Bloat's Defence");
            w.until(() -> walking, RaidScaling.TOB_BLOAT_DOWN + 2, "the Bloat never got up after its shutdown");
            w.expect(com.runecraft.content.systems.SysCombat.drainedStatLevel(bloat, Skill.DEFENCE) == baseDef && stompRestores > 0,
                    "the Bloat's stomp did not restore its drained Defence");
            w.until(() -> !walking, 80, "the Bloat never shut down a second time");
            int guard = 0;
            while (alive(bloat) && guard++ < 1000) {
                w.hit(bloat, Math.max(10, raid.maxHitpoints(bloat) / 10), AttackType.SLASH);
                w.tick(1);
            }
            w.expect(!alive(bloat), "the Bloat did not die");
            // The supply chest appears; spend the points.
            BlockPos chest = w.block(this, RaidsContent.SUPPLIES);
            w.moveTo(chest.offset(-1, 0, 0));
            w.use(chest);
            int points = TobRooms.points(raid, w.player());
            int less = hard() ? TobRules.HARD_CHEST_POINTS_LESS : 0;
            w.expect(points >= 6 - less && points <= 13 - less, "the first supply chest gave " + points + " points (expected "
                    + (6 - less) + "-" + (13 - less) + ")");
            w.expect(buy(w.player(), "stamina_potion") == 1 && buy(w.player(), "stamina_potion") == 0, "the chest sold more than one stamina potion");
            w.expect(buy(w.player(), "shark") == 1 && w.count("shark") > 0, "buying a shark from the chest failed");
            // Supplies come only from the chest itself.
            w.moveTo(center().offset(-RaidBuilder.HALF + 3, 1, 0));
            w.expect(buy(w.player(), "shark") == 0, "supplies could be bought away from the chest");
            w.moveTo(chest.offset(-1, 0, 0));
            return shutdowns + " shutdown(s); chest gave " + points + " points";
        }
    }

    // ================================================================== The Nylocas

    /**
     * The Nylocas (OSRS wiki): 31 fixed waves of nylocas walk in from the east, south and west lanes; most attack the
     * four supports, the aggressive ones the team. Each breed only takes damage from its own style; big nylocas split
     * into two smalls; every nylocas self-destructs 30 seconds after spawning. A collapsing support hits the whole team
     * for up to 50 and losing all four kills everyone. Waves stall while 12 (24 from wave 20) nylocas are alive. In Hard
     * mode a Nylocas Prinkipas drops in with waves 10, 20 and 30. Then the Nylocas Vasilias: it changes form every 10
     * ticks, and a hit of the wrong style is reflected onto the attacker and heals it.
     */
    static final class Nylocas extends TobRoom {
        private static final String[] FORMS = {"grey (melee)", "green (ranged)", "blue (magic)"};
        private static final int[][] PILLARS = {{-5, -5}, {-5, 5}, {5, -5}, {5, 5}};
        private static final int[][] LANES = {{19, 0}, {0, 19}, {-19, 8}};

        private static final class Nylo {
            final Mob mob;
            final TobNylocas.Breed breed;
            final boolean big;
            final boolean aggressive;
            int age;
            /** Players who attacked it with the wrong style: they can no longer damage it (OSRS). */
            final Set<UUID> spurned = new HashSet<>();

            Nylo(Mob mob, TobNylocas.Breed breed, boolean big, boolean aggressive) {
                this.mob = mob;
                this.breed = breed;
                this.big = big;
                this.aggressive = aggressive;
            }
        }

        private final List<Nylo> nylos = new ArrayList<>();
        private final int[] pillarHp = new int[TobNylocas.PILLARS];
        private int wave;
        private int waveTimer;
        private DataBossEntity vasilias;
        private int form;
        private int formTimer;
        private DataBossEntity prinkipas;
        private int prinkipasForm;
        private int prinkipasTimer;
        private boolean wrongStyle;
        private boolean pillarLost;
        private boolean prinkipasChanged;
        private int killed;
        /** Someone took a Vasilias or Prinkipas hit without the right protection prayer ("Perfect Nylocas"). */
        private boolean offPrayer;
        /** Damage reflected onto players by wrong-style hits (Hard mode) and splashed by the Hard mode Vasilias (self-test). */
        private int reflected;
        private int splashed;
        private int prinkipasSeen;

        Nylocas(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index, null);
        }

        private static AttackDef formAttack(int form, int maxHit) {
            return switch (form) {
                case 0 -> AttackDef.of("bite", AttackType.STAB, maxHit, 3, 10);
                case 1 -> AttackDef.of("spit", AttackType.RANGED, maxHit, 12, 10).particle("minecraft:crit");
                default -> AttackDef.of("blast", AttackType.MAGIC, maxHit, 12, 10).particle("minecraft:witch");
            };
        }

        private BlockPos pillarBase(int i) {
            return center().offset(PILLARS[i][0], 0, PILLARS[i][1]);
        }

        @Override
        public void decorate() {
            for (int i = 0; i < PILLARS.length; i++) {
                RaidBuilder.pillar(world(), pillarBase(i), 1, 4, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
                RaidBuilder.set(world(), pillarBase(i).above(5), Blocks.SOUL_LANTERN.defaultBlockState());
            }
        }

        @Override
        public void start() {
            wave = 0;
            waveTimer = 2;
            vasilias = null;
            prinkipas = null;
            nylos.clear();
            wrongStyle = false;
            pillarLost = false;
            prinkipasChanged = false;
            offPrayer = false;
            java.util.Arrays.fill(pillarHp, TobNylocas.PILLAR_HP);
            decorate(); // the supports stand again on a retry (Entry mode)
            say("Protect the supports! " + TobNylocas.WAVES + " waves of Nylocas approach.");
            // Round 43: each support's health bar over it, and all four on the raid HUD. // playability: OSRS shows a health
            // bar over each support
            java.util.function.BooleanSupplier attempt = current();
            RaidEffects.cue(Integer.MAX_VALUE / 4, center().getCenter(), () -> {
                if (!attempt.getAsBoolean()) {
                    return false;
                }
                for (int i = 0; i < PILLARS.length; i++) {
                    drawSupportBar(i);
                }
                return true;
            });
        }

        private static final net.minecraft.core.particles.DustParticleOptions BAR_GOOD =
                new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.2f, 0.9f, 0.2f), 1.0f);
        private static final net.minecraft.core.particles.DustParticleOptions BAR_HALF =
                new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1.0f, 0.85f, 0.1f), 1.0f);
        private static final net.minecraft.core.particles.DustParticleOptions BAR_LOW =
                new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.95f, 0.15f, 0.1f), 1.0f);
        private static final net.minecraft.core.particles.DustParticleOptions BAR_LOST =
                new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.25f, 0.05f, 0.05f), 0.7f);

        /** A support's health bar: a line of dust over it, its standing share coloured green, yellow or red. */
        private void drawSupportBar(int i) {
            if (pillarHp[i] <= 0) {
                return;
            }
            BlockPos b = pillarBase(i);
            double share = pillarHp[i] / (double) TobNylocas.PILLAR_HP;
            var colour = share > 0.5 ? BAR_GOOD : share > 0.25 ? BAR_HALF : BAR_LOW;
            double y = b.getY() + 6.4;
            for (int k = 0; k <= 10; k++) {
                double f = k / 10.0;
                world().sendParticles(f <= share ? colour : BAR_LOST, b.getX() - 0.5 + f * 2, y, b.getZ() + 0.5, 1, 0, 0, 0, 0);
            }
        }

        /** The supports' health for the raid HUD, by compass ("Supports NW 100 NE 62 ..."). */
        @Override
        String hudNote() {
            StringBuilder s = new StringBuilder("Supports");
            for (int i = 0; i < PILLARS.length; i++) {
                String c = (PILLARS[i][1] < 0 ? "N" : "S") + (PILLARS[i][0] < 0 ? "W" : "E");
                s.append(' ').append(c).append(' ').append(Math.max(0, pillarHp[i]));
            }
            return s.toString();
        }

        /** The Nylocas room a player is fighting in, if any. */
        static Nylocas of(ServerPlayer p) {
            RaidInstance raid = RaidManager.of(p);
            return raid != null && raid.type == RaidType.TOB && raid.logic() instanceof Nylocas n ? n : null;
        }

        private int standing() {
            int n = 0;
            for (int hp : pillarHp) {
                if (hp > 0) {
                    n++;
                }
            }
            return n;
        }

        private void spawnNylo(TobNylocas.Breed breed, boolean big, boolean aggressive, BlockPos pos) {
            Mob m = spawnMob(breed.creatureId, pos, false);
            if (m == null) {
                return;
            }
            if (big) {
                raid.setHitpoints(m, RaidScaling.tobScaledHp(breed.creatureId, TobNylocas.BIG_HP, raid.partySize(), raid.mode()));
            }
            if (!aggressive) {
                m.setTarget(null);
            }
            nylos.add(new Nylo(m, breed, big, aggressive));
        }

        private void spawnWave(int number) {
            TobNylocas.Wave w = TobNylocas.wave(number);
            for (TobNylocas.Lane lane : TobNylocas.Lane.values()) {
                int[] g = LANES[lane.ordinal()];
                boolean south = lane == TobNylocas.Lane.SOUTH;
                int k = 0;
                for (TobNylocas.Nylo n : w.lane(lane)) {
                    int off = k * 2 - 1;
                    spawnNylo(n.breed(), n.big(), n.aggressive(), at(g[0] + (south ? off : 0), g[1] + (south ? 0 : off)));
                    k++;
                }
            }
            say("Wave " + number + "/" + TobNylocas.WAVES + " of Nylocas approaches!");
            if (hard() && TobNylocas.prinkipasWave(number) && !alive(prinkipas)) {
                prinkipas = spawnBoss("nylocas_prinkipas", at(0, 0), false);
                prinkipasForm = 0;
                prinkipasTimer = TobNylocas.FORM_TICKS;
                if (prinkipas != null) {
                    anchor(prinkipas);
                    warn("A Nylocas Prinkipas drops down in its " + FORMS[0] + " form!");
                }
            }
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            t++;
            tickNylos();
            if (standing() == 0) {
                return;
            }
            if (alive(prinkipas) && --prinkipasTimer <= 0) {
                prinkipasTimer = TobNylocas.FORM_TICKS;
                prinkipasForm = TobNylocas.nextForm(prinkipasForm, rnd());
                prinkipasChanged = true;
                warn("The Nylocas Prinkipas changes to its " + FORMS[prinkipasForm] + " form!");
            }
            if (vasilias == null) {
                if (wave < TobNylocas.WAVES) {
                    if (--waveTimer <= 0 && nylos.size() < TobNylocas.cap(wave + 1)) {
                        wave++;
                        waveTimer = TobNylocas.WAVE_INTERVAL_TICKS;
                        spawnWave(wave);
                    }
                } else if (nylos.isEmpty() && !alive(prinkipas)) {
                    vasilias = spawnBoss("nylocas_vasilias", at(0, 0));
                    form = 0;
                    formTimer = formTicks();
                    warn("The Nylocas Vasilias emerges in its " + FORMS[form] + " form!");
                }
                return;
            }
            if (!alive(vasilias)) {
                return;
            }
            if (wrongStyle) {
                CombatAchievementService.flagNear(vasilias, CombatAchievements.FLAG_NYLO_WRONG_STYLE);
            }
            if (pillarLost) {
                CombatAchievementService.flagNear(vasilias, CombatAchievements.FLAG_NYLO_PILLAR_LOST);
            }
            if (prinkipasChanged) {
                CombatAchievementService.flagNear(vasilias, CombatAchievements.FLAG_PRINKIPAS_CHANGED);
            }
            if (offPrayer) {
                CombatAchievementService.flagNear(vasilias, CombatAchievements.FLAG_NYLO_OFF_PRAYER);
            }
            if (--formTimer <= 0) {
                formTimer = formTicks();
                form = TobNylocas.nextForm(form, rnd());
                warn("The Nylocas Vasilias changes to its " + FORMS[form] + " form!");
            }
            ParticleOptions colour = switch (form) {
                case 0 -> ParticleTypes.CRIT;
                case 1 -> ParticleTypes.HAPPY_VILLAGER;
                default -> ParticleTypes.SOUL_FIRE_FLAME;
            };
            world().sendParticles(colour, vasilias.getX(), vasilias.getY() + 1, vasilias.getZ(), 6, 1, 0.5, 1, 0.01);
        }

        /** Entry mode Vasilias changes form more slowly. // approx: 14 ticks */
        private int formTicks() {
            return raid.mode() == RaidMode.ENTRY ? 14 : TobNylocas.FORM_TICKS;
        }

        private void tickNylos() {
            List<Nylo> split = new ArrayList<>();
            java.util.Iterator<Nylo> it = nylos.iterator();
            while (it.hasNext()) {
                Nylo n = it.next();
                if (!alive(n.mob)) {
                    it.remove();
                    if (n.mob.isDeadOrDying()) {
                        killed++;
                        if (n.big) {
                            split.add(n);
                        }
                    }
                    continue;
                }
                if (++n.age >= TobNylocas.LIFETIME_TICKS) {
                    explode(n);
                    it.remove();
                    continue;
                }
                if (n.age == TobNylocas.LIFETIME_TICKS - NYLO_WARNING_TICKS) {
                    // An old nylocas about to explode shows its blast ring around it. // playability: OSRS shows it swelling
                    RaidEffects.fuse(n.mob, n.big ? 2.5 : 1.8, NYLO_WARNING_TICKS);
                }
                if (n.aggressive) {
                    if (!(n.mob.getTarget() instanceof ServerPlayer current) || !current.isAlive()) {
                        ServerPlayer target = randomPlayer();
                        if (target != null) {
                            n.mob.setTarget(target);
                        }
                    }
                    continue;
                }
                n.mob.setTarget(null);
                int p = nearestPillar(n.mob.position());
                if (p < 0) {
                    continue;
                }
                Vec3 pc = Vec3.atBottomCenterOf(pillarBase(p).above());
                double d2 = RaidEffects.horizontalDistSq(n.mob.position(), pc);
                if (d2 > 2.6 * 2.6) {
                    if (n.age % 8 == 1 || n.mob.getNavigation().isDone()) {
                        n.mob.getNavigation().moveTo(pc.x, pc.y, pc.z, 1.0);
                    }
                } else if (n.age % 3 == 0) {
                    int before = pillarHp[p];
                    pillarHp[p] -= TobNylocas.pillarDamage(n.big, rnd());
                    world().sendParticles(ParticleTypes.CRIT, pc.x, pc.y + 1, pc.z, 2, 0.6, 0.6, 0.6, 0.01);
                    if (pillarHp[p] <= 0) {
                        collapse(p);
                    } else {
                        pillarWarning(p, before);
                    }
                }
            }
            for (Nylo n : split) {
                // OSRS: a slain big nylocas splits into two small nylocas of any breed.
                BlockPos at = n.mob.blockPosition();
                TobNylocas.Breed[] breeds = TobNylocas.Breed.values();
                for (int i = 0; i < 2; i++) {
                    spawnNylo(breeds[rnd().nextInt(breeds.length)], false, n.aggressive, at.offset(i, 0, 0));
                }
            }
        }

        private int nearestPillar(Vec3 pos) {
            int best = -1;
            double bestD = Double.MAX_VALUE;
            for (int i = 0; i < PILLARS.length; i++) {
                if (pillarHp[i] <= 0) {
                    continue;
                }
                double d = RaidEffects.horizontalDistSq(pos, Vec3.atBottomCenterOf(pillarBase(i)));
                if (d < bestD) {
                    bestD = d;
                    best = i;
                }
            }
            return best;
        }

        /** An old nylocas's blast ring shows for its last this-many game ticks. */
        private static final int NYLO_WARNING_TICKS = com.runecraft.osrs.ArenaTiming.STRIKE_FLOOR_TICKS;

        /** Supports' health warnings given (self-test). */
        int pillarWarnings;

        /**
         * A support crossing half or a quarter of its health is called out by compass, so the party knows where to clear the
         * nylocas before it falls. // playability: OSRS shows each support's health bar over it
         */
        private void pillarWarning(int p, int before) {
            int now = pillarHp[p];
            for (int threshold : new int[]{TobNylocas.PILLAR_HP / 2, TobNylocas.PILLAR_HP / 4}) {
                if (before > threshold && now <= threshold) {
                    pillarWarnings++;
                    BlockPos b = pillarBase(p);
                    String where = com.runecraft.osrs.content.raids.RaidRealtime.compass(b.getX() + 0.5 - (center().getX() + 0.5),
                            b.getZ() + 0.5 - (center().getZ() + 0.5));
                    warn("The " + where + " support is cracking! (" + Math.max(0, now) + "/" + TobNylocas.PILLAR_HP + ")");
                    world().sendParticles(ParticleTypes.LARGE_SMOKE, b.getX() + 0.5, b.getY() + 3, b.getZ() + 0.5, 12, 0.6, 1.2, 0.6, 0.02);
                }
            }
        }

        private void explode(Nylo n) {
            Vec3 pos = n.mob.position();
            world().sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 0.5, pos.z, 1, 0, 0, 0, 0);
            int max = n.big ? TobNylocas.BIG_EXPLOSION : TobNylocas.SMALL_EXPLOSION;
            double r = n.big ? 2.5 : 1.8;
            for (ServerPlayer p : players()) {
                if (p.position().distanceToSqr(pos) <= r * r) {
                    RaidEffects.hit(n.mob, p, rnd().nextInt(max + 1), null, true);
                }
            }
            n.mob.discard();
        }

        private void collapse(int i) {
            pillarHp[i] = 0;
            pillarLost = true;
            BlockPos b = pillarBase(i);
            RaidBuilder.pillar(world(), b, 1, 5, Blocks.AIR.defaultBlockState());
            RaidBuilder.pillar(world(), b, 1, 1, Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState());
            world().sendParticles(ParticleTypes.EXPLOSION, b.getX() + 0.5, b.getY() + 2, b.getZ() + 0.5, 4, 1, 1, 1, 0);
            if (standing() == 0) {
                warn("The last support collapses and the roof caves in!");
                for (ServerPlayer p : players()) {
                    RaidEffects.hit(null, p, Math.round(p.getMaxHealth()) + 100, null, true);
                }
                return;
            }
            warn("A support collapses! Rocks fall from the roof! (" + standing() + " left)");
            for (ServerPlayer p : players()) {
                RaidEffects.hit(null, p, rnd().nextInt(TobNylocas.PILLAR_COLLAPSE_MAX_HIT + 1), null, true);
            }
        }

        private Nylo nyloOf(LivingEntity target) {
            for (Nylo n : nylos) {
                if (n.mob == target) {
                    return n;
                }
            }
            return null;
        }

        /**
         * A player attacked a wave nylocas (OSRS wiki): only its own style harms it, and a player who used the wrong style
         * can no longer damage it at all; in Hard mode the hit they would have dealt is also reflected onto them (capped at
         * the nylocas's hitpoints). Any wrong-style attack fails "Perfect Nylocas". Returns the damage that may land.
         */
        int attacked(ServerPlayer player, LivingEntity target, AttackType type, int damage) {
            TobNylocas.Breed b = TobNylocas.Breed.byCreature(RaidInstance.creatureId(target));
            if (b == null || type == null) {
                return damage;
            }
            Nylo n = nyloOf(target);
            if (n != null && n.spurned.contains(player.getUUID())) {
                Msg.action(player, "You can no longer damage this nylocas.");
                return 0;
            }
            if (b.weakTo(type)) {
                return damage;
            }
            wrongStyle = true;
            if (n != null) {
                n.spurned.add(player.getUUID());
            }
            int reflect = TobNylocas.wrongStyleReflect(damage, raid.hitpoints(target), hard());
            if (reflect > 0) {
                reflected += RaidEffects.hitRaw(target, player, reflect);
                Msg.red(player, "The nylocas reflects your attack back at you!");
            }
            return 0;
        }

        @Override
        public AttackDef bossAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            if (boss == prinkipas) {
                return formAttack(prinkipasForm, TobNylocas.PRINKIPAS_MAX_HIT);
            }
            return formAttack(form, TobNylocas.VASILIAS_MAX_HIT);
        }

        /**
         * The Vasilias's (and Prinkipas's) hits (OSRS wiki): the right prayer blocks melee but lets up to 17 of a ranged or
         * magic hit through (// approx: a quarter of the Prinkipas's 38). In Hard mode the Vasilias's ranged attack also
         * hits everyone within 2 tiles of its target, and its magic attack bounces to every other player, ignoring prayer,
         * for half within 2 tiles of the target and more beyond, unless everyone stands on the one tile.
         */
        @Override
        public int bossOutgoing(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
            if (boss != vasilias && boss != prinkipas) {
                return damage;
            }
            int f = boss == prinkipas ? prinkipasForm : form;
            boolean prayed = type != null && RaidEffects.protectedFrom(player, type);
            if (prayed && f != 0 && damage == 0) {
                int through = boss == prinkipas ? TobNylocas.PRINKIPAS_MAX_HIT / 4 : TobNylocas.VASILIAS_PRAYED_MAX;
                damage = rnd().nextInt(through + 1);
            }
            if (!prayed && damage > 0) {
                offPrayer = true;
            }
            if (hard() && boss == vasilias && f != 0) {
                List<ServerPlayer> others = new ArrayList<>(players());
                others.remove(player);
                double near = TobNylocas.VASILIAS_HARD_SPLASH_TILES * 2 + 0.5;
                boolean stacked = true;
                for (ServerPlayer o : others) {
                    stacked &= o.blockPosition().equals(player.blockPosition());
                }
                for (ServerPlayer o : others) {
                    boolean within = RaidEffects.horizontalDistSq(o.position(), player.position()) <= near * near;
                    if (f == 1 && within) {
                        splashed += RaidEffects.hit(boss, o, rnd().nextInt(TobNylocas.VASILIAS_MAX_HIT + 1), AttackType.RANGED, false);
                    } else if (f == 2 && !stacked && damage > 0) {
                        splashed += RaidEffects.hit(boss, o, TobNylocas.vasiliasHardBounce(damage, within), null, true);
                    }
                }
            }
            return damage;
        }

        @Override
        public int bossIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            int f = boss == prinkipas ? prinkipasForm : form;
            if (TobNylocas.formWeakTo(f, type)) {
                return damage;
            }
            // OSRS: the wrong style is reflected onto the attacker and heals the boss by the same amount.
            wrongStyle = true;
            if (damage > 0 && attacker != null) {
                Msg.action(attacker, "It is in its " + FORMS[f] + " form - your attack is reflected!");
                RaidEffects.hit(boss, attacker, damage, null, true);
                raid.heal(boss, damage);
            }
            return 0;
        }

        @Override
        public void cleanup() {
            super.cleanup();
            for (Nylo n : nylos) {
                if (n.mob.isAlive()) {
                    n.mob.discard();
                }
            }
            nylos.clear();
            if (prinkipas != null && prinkipas.isAlive()) {
                prinkipas.discard();
            }
            for (TobNylocas.Breed b : TobNylocas.Breed.values()) {
                living(b.creatureId).forEach(LivingEntity::discard);
            }
        }

        private static AttackType styleFor(TobNylocas.Breed breed) {
            for (AttackType t : new AttackType[]{AttackType.SLASH, AttackType.RANGED, AttackType.MAGIC}) {
                if (breed.weakTo(t)) {
                    return t;
                }
            }
            return AttackType.SLASH;
        }

        private static AttackType formStyle(int form) {
            for (AttackType t : new AttackType[]{AttackType.SLASH, AttackType.RANGED, AttackType.MAGIC}) {
                if (TobNylocas.formWeakTo(form, t)) {
                    return t;
                }
            }
            return AttackType.SLASH;
        }

        @Override
        public String walk(CoxTobWalk w) {
            int guard = 0;
            boolean triedWrong = false;
            UUID me = w.player().getUUID();
            while (vasilias == null && guard++ < 1500) {
                for (Nylo n : new ArrayList<>(nylos)) {
                    if (!alive(n.mob) || n.spurned.contains(me)) {
                        continue; // a nylocas this player can no longer damage is left to self-destruct
                    }
                    if (!triedWrong) {
                        // OSRS: the wrong style can't harm it, and the attacker can't damage it afterwards even with the
                        // right one; Hard mode also reflects the hit (capped at the nylocas's hitpoints).
                        triedWrong = true;
                        AttackType wrong = n.breed.weakTo(AttackType.MAGIC) ? AttackType.RANGED : AttackType.MAGIC;
                        int before = reflected;
                        w.expect(w.hit(n.mob, 10, wrong) == 0, "a " + n.breed + " nylocas took damage from the wrong style");
                        w.expect(w.hit(n.mob, 10, styleFor(n.breed)) == 0, "after a wrong-style attack the player could still damage the "
                                + n.breed + " nylocas");
                        w.expect(hard() ? reflected > before : reflected == before, hard() ? "a Hard mode nylocas did not reflect a "
                                + "wrong-style hit" : "a Normal mode nylocas reflected a hit");
                        continue;
                    }
                    w.kill(n.mob, styleFor(n.breed), "a " + n.breed + " nylocas");
                }
                if (alive(prinkipas)) {
                    // Hard mode: the Prinkipas changes form like the Vasilias; only its current style harms it.
                    prinkipasSeen++;
                    w.hit(prinkipas, Math.max(10, raid.maxHitpoints(prinkipas) / 6), formStyle(prinkipasForm));
                }
                w.tick(1);
            }
            w.expect(!hard() || prinkipasSeen > 0, "no Nylocas Prinkipas dropped in during the Hard mode waves");
            w.expect(vasilias != null, "the Nylocas Vasilias never emerged (wave " + wave + "/" + TobNylocas.WAVES + ", " + nylos.size() + " alive)");
            w.expect(wave == TobNylocas.WAVES && standing() == TobNylocas.PILLARS, "waves " + wave + ", supports standing " + standing());
            guard = 0;
            while (alive(vasilias) && guard++ < 800) {
                w.hit(vasilias, Math.max(10, raid.maxHitpoints(vasilias) / 12), formStyle(form));
                w.tick(1);
            }
            w.expect(!alive(vasilias), "the Nylocas Vasilias did not die");
            w.expect(!wrongStyle || triedWrong, "a wrong-style hit was recorded");
            return TobNylocas.WAVES + " waves, " + killed + " nylocas killed, Vasilias slain";
        }
    }

    // ================================================================== Sotetseg

    /**
     * Sotetseg (OSRS wiki): every 5 ticks it lunges (up to 45, halved by Protect from Melee) at someone in reach or fires a
     * red orb (up to 50, fully blocked by Protect from Magic) that splits into up to two more red or black (ranged) orbs at
     * other players; a hit knocks off protection prayers. After 10 magic attacks it launches the ball of death, shared by
     * everyone within 3x3 of the target. At 66% and 33% it drags one player (Hard mode: all but one) into the shadow realm,
     * whose maze they must walk while the others follow the same path on the arena floor (a wrong tile: 6.67% of current
     * hitpoints + 15 a tick); the runner takes 1-3 every 7 ticks.
     */
    static final class Sotetseg extends TobRoom {
        private static final int COLS = 7;
        private static final int ROWS = 14;
        /** A runner still in the maze after this many ticks is thrown out. // approx: a safety net, OSRS has no limit */
        private static final int MAZE_SAFETY_TICKS = 200;
        private DataBossEntity sotetseg;
        private final Set<Long> path = new HashSet<>();
        /** The maze's tiles in walking order (the tornado follows them). */
        private final List<Long> pathOrder = new ArrayList<>();
        private final Set<UUID> runners = new HashSet<>();
        private int mazeTicks;
        private boolean maze66;
        private boolean maze33;
        private int magicAttacks;
        private int mazes;
        private int balls;
        /** The maze tornado's place in {@link #pathOrder} (-1: none), its step timer, and how often it struck (self-test). */
        private int tornado = -1;
        private int tornadoTimer;
        private int tornadoes;
        private int tornadoHits;
        private int wrongTiles;
        /** Hard and Entry mode task conditions: a ball was shared / split by the whole team, a non-ball hit landed, a death. */
        private boolean ballShared;
        private boolean teamSplit;
        private boolean nonBallHit;
        private int deathsAtStart;

        Sotetseg(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index, CombatAchievements.FLAG_SOTETSEG_IMPERFECT);
        }

        private BlockPos mazeOrigin() {
            return center().offset(-COLS, 0, RaidBuilder.MAZE_OFFSET_Z - ROWS);
        }

        private BlockPos arenaOrigin() {
            return center().offset(-COLS, 0, -ROWS);
        }

        @Override
        public void decorate() {
            BlockPos o = mazeOrigin();
            int y = o.getY();
            RaidBuilder.loadChunks(world(), o.getX() - 4, o.getZ() - 6, o.getX() + COLS * 2 + 4, o.getZ() + ROWS * 2 + 6);
            RaidBuilder.fill(world(), o.getX() - 2, y - 1, o.getZ() - 5, o.getX() + COLS * 2 + 1, y - 1, o.getZ() + ROWS * 2 + 4,
                    Blocks.OBSIDIAN.defaultBlockState());
            RaidBuilder.fill(world(), o.getX() - 2, y, o.getZ() - 5, o.getX() + COLS * 2 + 1, y + 3, o.getZ() + ROWS * 2 + 4,
                    Blocks.RED_STAINED_GLASS.defaultBlockState());
            RaidBuilder.fill(world(), o.getX() - 1, y + 1, o.getZ() - 4, o.getX() + COLS * 2, y + 3, o.getZ() + ROWS * 2 + 3,
                    Blocks.AIR.defaultBlockState());
            RaidBuilder.fill(world(), o.getX() - 1, y, o.getZ() - 4, o.getX() + COLS * 2, y, o.getZ() - 1,
                    Blocks.CRIMSON_NYLIUM.defaultBlockState());
            RaidBuilder.fill(world(), o.getX() - 1, y, o.getZ() + ROWS * 2, o.getX() + COLS * 2, y, o.getZ() + ROWS * 2 + 3,
                    Blocks.RED_NETHER_BRICKS.defaultBlockState());
            RaidBuilder.fill(world(), o.getX(), y, o.getZ(), o.getX() + COLS * 2 - 1, y, o.getZ() + ROWS * 2 - 1,
                    Blocks.POLISHED_BLACKSTONE.defaultBlockState());
            BlockPos a = arenaOrigin();
            RaidBuilder.fill(world(), a.getX(), a.getY(), a.getZ(), a.getX() + COLS * 2 - 1, a.getY(), a.getZ() + ROWS * 2 - 1,
                    Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            RaidBuilder.pillar(world(), center().offset(-12, 0, -8), 1, 2, Blocks.CRIMSON_HYPHAE.defaultBlockState());
            RaidBuilder.pillar(world(), center().offset(-12, 0, 8), 1, 2, Blocks.CRIMSON_HYPHAE.defaultBlockState());
        }

        @Override
        public void start() {
            maze66 = false;
            maze33 = false;
            magicAttacks = 0;
            tornado = -1;
            ballShared = false;
            teamSplit = false;
            nonBallHit = false;
            deathsAtStart = raid.totalDeaths();
            runners.clear();
            sotetseg = spawnBoss("sotetseg", at(13, 0));
            if (sotetseg != null) {
                sotetseg.setNoAi(true);
                anchor(sotetseg);
            }
        }

        private static long key(int col, int row) {
            return ((long) col << 32) | (row & 0xffffffffL);
        }

        private ServerPlayer player(UUID id) {
            return world().getServer().getPlayerList().getPlayer(id);
        }

        private void startMaze() {
            List<ServerPlayer> party = players();
            if (party.isEmpty()) {
                return;
            }
            mazes++;
            path.clear();
            pathOrder.clear();
            walkedTiles.clear();
            tornado = -1;
            int col = rnd().nextInt(COLS);
            for (int r = 0; r < ROWS; r++) {
                path.add(key(col, r));
                int next = Mth.clamp(col + rnd().nextInt(5) - 2, 0, COLS - 1);
                for (int c = Math.min(col, next); c <= Math.max(col, next); c++) {
                    path.add(key(c, r));
                }
                // The order a runner walks it: across this row from the column they came in on, then up to the next row.
                int step = next >= col ? 1 : -1;
                for (int c = col; c != next + step; c += step) {
                    pathOrder.add(key(c, r));
                }
                col = next;
            }
            java.util.Collections.shuffle(party, new java.util.Random(rnd().nextLong()));
            int taken = hard() && party.size() > 1 ? party.size() - 1 : 1;
            runners.clear();
            segments.clear();
            trails.clear();
            releaseMazeMarks();
            mazeTicks = 0;
            BlockPos o = mazeOrigin();
            for (int i = 0; i < taken; i++) {
                ServerPlayer p = party.get(i);
                runners.add(p.getUUID());
                // Hard mode (OSRS wiki): the runners divide the maze among themselves, in orb order (round 43; here in the
                // order they were picked): each walks its own rows and leaves its own trail.
                int from = i * ROWS / taken;
                int to = (i + 1) * ROWS / taken;
                segments.put(p.getUUID(), new int[]{from, to});
                if (i == 0) {
                    raid.teleport(p, new BlockPos(o.getX() + COLS - taken / 2 + i, o.getY() + 1, o.getZ() - 2));
                } else {
                    int startCol = firstPathColumn(from);
                    raid.teleport(p, new BlockPos(o.getX() + startCol * 2, o.getY() + 1, o.getZ() + from * 2));
                }
                warn("Sotetseg chooses " + p.getName().getString() + "... They are dragged into the shadow realm!");
                Msg.game(p, taken > 1 ? "Follow the souls through rows " + (from + 1) + "-" + to + " of the maze. Stepping off the path hurts."
                        : "Follow the souls through the maze. Stepping off the path hurts.");
            }
            // Round 43: the path is marked on the shadow realm's floor for the runners. // playability: OSRS shows them the red path
            List<int[]> cols = new ArrayList<>();
            for (long k : path) {
                addTile(cols, o, (int) (k >> 32), (int) k);
            }
            mazePathMarks = RaidFloor.columns(world(), cols, o.getY() + 1, RaidFloor.SAFE, 0, sotetseg, Vec3.atBottomCenterOf(o));
            for (int i = taken; i < party.size(); i++) {
                // OSRS: the others are thrown to the far end of the arena.
                raid.teleport(party.get(i), at(-16, i % 2 == 0 ? -3 : 3));
                Msg.game(party.get(i), "Follow the red glow across the tiled floor: only the shadow realm runner can see the path.");
            }
        }

        /** Hard mode: the maze rows each runner walks ({from, to}, to exclusive); one runner walks them all. */
        private final Map<UUID, int[]> segments = new HashMap<>();
        /** The path tiles each runner has walked, in order: their own trail on the arena floor (round 43). */
        private final Map<UUID, List<Long>> trails = new java.util.LinkedHashMap<>();
        /** The path marked on the shadow realm's floor, and the trail tiles marked on the arena floor. */
        private RaidFloor.Group mazePathMarks;
        private final Map<Long, RaidFloor.Group> trailMarks = new HashMap<>();

        /** The column of the first path tile in a row (as the runner arrives on it). */
        private int firstPathColumn(int row) {
            for (long k : pathOrder) {
                if ((int) k == row) {
                    return (int) (k >> 32);
                }
            }
            return COLS / 2;
        }

        /** Adds the four floor columns of maze tile (c, r) on a grid starting at {@code o}. */
        private static void addTile(List<int[]> cols, BlockPos o, int c, int r) {
            for (int dx = 0; dx < 2; dx++) {
                for (int dz = 0; dz < 2; dz++) {
                    cols.add(new int[]{o.getX() + c * 2 + dx, o.getZ() + r * 2 + dz});
                }
            }
        }

        /** Puts back the maze's floor marks (the maze is walked, or the room is left). */
        private void releaseMazeMarks() {
            if (mazePathMarks != null) {
                mazePathMarks.release();
                mazePathMarks = null;
            }
            trailMarks.values().forEach(RaidFloor.Group::release);
            trailMarks.clear();
        }

        @Override
        public boolean bossVulnerable(DataBossEntity boss) {
            return runners.isEmpty();
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            t++;
            if (!alive(sotetseg)) {
                return;
            }
            reflag(sotetseg);
            flagTasks();
            // OSRS wiki: its Defence can't be drained below 100.
            int def = com.runecraft.content.systems.SysCombat.baseStatLevel(sotetseg, Skill.DEFENCE);
            var drain = com.runecraft.content.systems.SysCombat.drain(sotetseg);
            drain.defence = Math.min(drain.defence, Math.max(0, def - TobRules.SOTETSEG_DEFENCE_FLOOR));
            if (!runners.isEmpty()) {
                mazeTick();
                if (runners.isEmpty()) {
                    // The maze is walked: Sotetseg's Defence is restored (OSRS wiki).
                    com.runecraft.content.systems.SysCombat.drain(sotetseg).defence = 0;
                    tornado = -1;
                    releaseMazeMarks();
                }
                return;
            }
            double hp = raid.healthFraction(sotetseg);
            if ((hp <= 0.66 && !maze66) || (hp <= 0.33 && !maze33)) {
                if (hp <= 0.33) {
                    maze33 = true;
                }
                maze66 = true;
                startMaze();
                return;
            }
            if (quietTicks > 0) {
                quietTicks--;
            } else if (t % TobRules.SOTETSEG_ATTACK_SPEED == 0) {
                attack();
            }
        }

        private void attack() {
            List<ServerPlayer> party = players();
            if (party.isEmpty()) {
                return;
            }
            ServerPlayer target = party.get(rnd().nextInt(party.size()));
            if (target.distanceToSqr(sotetseg) < 3.5 * 3.5 && rnd().nextBoolean()) {
                int roll = rnd().nextInt(TobRules.SOTETSEG_MELEE_MAX + 1);
                // OSRS: the melee hitsplat lands a tick after the lunge; here after the lunge's wind-up, on a player still in
                // reach, the prayer read as it lands. // playability
                sotetseg.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                java.util.function.BooleanSupplier current = current();
                com.runecraft.boss.Mechanics.deliverMelee(sotetseg, target, com.runecraft.osrs.ThreatTiming.MELEE_REACH, () -> {
                    if (alive(sotetseg) && current.getAsBoolean() && inside(target.position())) {
                        boolean prayed = RaidEffects.protectedFrom(target, AttackType.CRUSH);
                        int dealt = RaidEffects.hit(sotetseg, target, prayed ? roll / 2 : roll, null, true);
                        nonBallHit |= dealt > 0;
                        if (dealt > 0 && !prayed) {
                            fail(); // "Perfect Sotetseg": no damage off prayer
                        }
                    }
                });
                return;
            }
            if (++magicAttacks % TobRules.SOTETSEG_ATTACKS_PER_BALL == 0) {
                int count = hard() && party.size() > 1 ? 2 : 1;
                java.util.Collections.shuffle(party, new java.util.Random(rnd().nextLong()));
                for (int i = 0; i < Math.min(count, party.size()); i++) {
                    deathBall(party.get(i));
                }
                return;
            }
            orb(target, AttackType.MAGIC, true);
        }

        /** Game ticks an orb flies (OSRS: it hits three ticks after it leaves). */
        private static final int ORB_TICKS = 3;

        /**
         * A red (magic) or black (ranged) orb; the first one splits into up to two more at other players. Each flies as a
         * projectile in its style's colour (magic blue, ranged green), its colour and prayer named to its player as it leaves;
         * two orbs of different styles at one player land at least two game ticks apart, in order. // playability: OSRS lands
         * a split orb of the other style a tick after the next, and the colour is the only cue
         */
        private void orb(ServerPlayer target, AttackType style, boolean split) {
            boolean magic = style == AttackType.MAGIC;
            Msg.telegraph(target, magic ? "Red orb (magic) - pray Magic!" : "Black orb (ranged) - pray Missiles!", null);
            orbs++;
            java.util.function.BooleanSupplier current = current();
            RaidEffects.projectile(world(), sotetseg.getEyePosition(), target, style, ORB_TICKS,
                    () -> alive(sotetseg) && current.getAsBoolean() && inside(target.position()), () -> {
                        if (!RaidEffects.protectedFrom(target, style)) {
                            orbHits++;
                            nonBallHit |= hurt(sotetseg, target, 1 + rnd().nextInt(TobRules.SOTETSEG_ORB_MAX), null, true) > 0;
                            dropOverheads(target, "Sotetseg's orb knocks out your protection prayers!");
                        } else {
                            orbBlocks++;
                        }
                        if (split) {
                            List<ServerPlayer> others = new ArrayList<>(players());
                            others.remove(target);
                            java.util.Collections.shuffle(others, new java.util.Random(rnd().nextLong()));
                            for (int i = 0; i < Math.min(2, others.size()); i++) {
                                orb(others.get(i), rnd().nextBoolean() ? AttackType.MAGIC : AttackType.RANGED, false);
                            }
                        }
                    });
        }

        /** Orbs fired, and those that landed unblocked or blocked (self-test). */
        int orbs;
        int orbHits;
        int orbBlocks;
        /** Self-test: game ticks Sotetseg holds his attacks (a check isolating one mechanic). */
        int quietTicks;

        private void deathBall(ServerPlayer target) {
            balls++;
            warn(target.getName().getString() + " has discovered a large ball of energy coming their way... Stand together to share it!");
            // Who shares it: everyone inside the green ring around its target when it lands. // playability: OSRS shares it
            // with the 3x3 around the target, read off the tiles
            RaidEffects.cue(4, () -> {
                if (!target.isAlive() || !alive(sotetseg)) {
                    return false;
                }
                com.runecraft.boss.ThreatCues.ring(world(), target.position(), 3.5, 0x73FF73, null, false);
                return true;
            });
            later(4, () -> {
                if (!target.isAlive() || !alive(sotetseg) || !runners.isEmpty()) {
                    return;
                }
                List<ServerPlayer> share = new ArrayList<>();
                for (ServerPlayer p : players()) {
                    if (p.distanceToSqr(target) <= 3.5 * 3.5) {
                        share.add(p);
                    }
                }
                if (!share.contains(target)) {
                    share.add(target);
                }
                ballShared |= share.size() > 1;
                teamSplit |= share.size() > 1 && share.size() >= players().size();
                world().sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 1, target.getZ(), 3, 1, 0.5, 1, 0);
                // OSRS: up to 188 split between everyone within 3x3 of the target. // approx: scaled like the HP by party
                int each = (int) (RaidScaling.TOB_SOTETSEG_BALL * RaidScaling.tobHp(raid.partySize())) / share.size();
                for (ServerPlayer p : share) {
                    RaidEffects.hit(sotetseg, p, each, null, true);
                }
                if (share.size() == 1 && target.isAlive()) {
                    CombatAchievementService.countNear(sotetseg, CombatAchievements.COUNT_SOTETSEG_BALL_ALONE);
                }
            });
        }

        /** Hard and Entry mode task conditions on everyone in the fight. */
        private void flagTasks() {
            if (ballShared) {
                CombatAchievementService.flagNear(sotetseg, CombatAchievements.FLAG_SOTETSEG_BALL_SHARED);
            }
            if (teamSplit) {
                CombatAchievementService.flagNear(sotetseg, CombatAchievements.FLAG_SOTETSEG_TEAM_SPLIT);
            }
            if (nonBallHit || raid.totalDeaths() > deathsAtStart) {
                CombatAchievementService.flagNear(sotetseg, CombatAchievements.FLAG_SOTETSEG_HIT);
            }
        }

        private static final net.minecraft.core.particles.DustParticleOptions MAZE_PATH =
                new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.55f, 0.75f, 1.0f), 1.1f);
        private static final net.minecraft.core.particles.DustParticleOptions MAZE_NEXT =
                new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1.0f, 1.0f, 1.0f), 1.4f);
        private static final net.minecraft.core.particles.DustParticleOptions MAZE_WALKED =
                new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.95f, 0.15f, 0.15f), 1.2f);

        /** The path tiles the maze runner has walked, in order (lit on the arena floor for the others). */
        private final List<Long> walkedTiles = new ArrayList<>();

        /** A maze tile's 2x2 outline on a grid starting at {@code o}. */
        private void drawTile(BlockPos o, int c, int r, net.minecraft.core.particles.DustParticleOptions dust) {
            double x0 = o.getX() + c * 2 + 0.15;
            double z0 = o.getZ() + r * 2 + 0.15;
            double y = o.getY() + 1.1;
            for (int i = 0; i <= 3; i++) {
                double f = i * 1.7 / 3;
                world().sendParticles(dust, x0 + f, y, z0, 1, 0, 0, 0, 0);
                world().sendParticles(dust, x0 + f, y, z0 + 1.7, 1, 0, 0, 0, 0);
                world().sendParticles(dust, x0, y, z0 + f, 1, 0, 0, 0, 0);
                world().sendParticles(dust, x0 + 1.7, y, z0 + f, 1, 0, 0, 0, 0);
            }
        }

        /**
         * Whether a position on a maze grid starting at {@code o} is off the path, with the edge leniency of
         * {@link com.runecraft.osrs.content.raids.RaidRealtime#offPath}. // playability: OSRS judges the tile stood on
         */
        private boolean offPath(BlockPos o, Vec3 pos) {
            return com.runecraft.osrs.content.raids.RaidRealtime.offPath(pos.x - o.getX(), pos.z - o.getZ(), 2, COLS, ROWS,
                    (c, r) -> path.contains(key(c, r)));
        }

        /** The tile (column, row) a position stands on in a maze grid starting at {@code o}, or null off the grid. */
        private int[] tileOf(BlockPos o, Vec3 pos) {
            int c = (int) Math.floor((pos.x - o.getX()) / 2);
            int r = (int) Math.floor((pos.z - o.getZ()) / 2);
            return c >= 0 && c < COLS && r >= 0 && r < ROWS ? new int[]{c, r} : null;
        }

        private void mazeTick() {
            BlockPos o = mazeOrigin();
            mazeTicks++;
            List<ServerPlayer> inMaze = new ArrayList<>();
            // each runner's row and the first row of their part: the tornado reads the progress of the runner lowest in orb
            // order through their own part (a hard mode runner who starts mid-maze has walked none of it)
            List<int[]> progress = new ArrayList<>();
            for (UUID id : new ArrayList<>(runners)) {
                ServerPlayer p = player(id);
                if (p == null || !p.isAlive() || raid.isDowned(p)) {
                    runners.remove(id);
                    continue;
                }
                int col = (int) Math.floor((p.getX() - o.getX()) / 2);
                int row = (int) Math.floor((p.getZ() - o.getZ()) / 2);
                int[] seg = segments.getOrDefault(id, new int[]{0, ROWS});
                if (p.getZ() >= o.getZ() + seg[1] * 2) {
                    raid.teleport(p, at(-14, 0));
                    say(p.getName().getString() + (seg[1] >= ROWS ? " escapes the shadow realm!" : " has walked their part of the maze!"));
                    runners.remove(id);
                    continue;
                }
                if (col >= 0 && col < COLS && row >= 0 && row < ROWS && path.contains(key(col, row))) {
                    List<Long> trail = trails.computeIfAbsent(id, k -> new ArrayList<>());
                    long k = key(col, row);
                    if (!trail.contains(k)) {
                        trail.add(k);
                    }
                }
                if (mazeTicks > MAZE_SAFETY_TICKS) {
                    // A safety net for a runner who never moves (OSRS has no time limit; the tornado punishes dawdling).
                    raid.teleport(p, at(-14, 0));
                    RaidEffects.hit(sotetseg, p, 30, null, true);
                    Msg.red(p, "The shadow realm spits you out.");
                    runners.remove(id);
                    continue;
                }
                if (mazeTicks % 7 == 0) {
                    RaidEffects.hit(sotetseg, p, 1 + rnd().nextInt(3), null, true);
                }
                if (offPath(o, p.position())) {
                    hurtRaw(sotetseg, p, TobRules.mazeWrongTile(Math.round(p.getHealth()), raid.mode()));
                    wrongTiles++;
                    nonBallHit = true;
                    fail();
                    Msg.red(p, "The shadows lash at you!");
                }
                // The next three rows of the path, each tile outlined in pale blue, redrawn until the next game tick.
                // // playability: OSRS lights the path's tiles for the runner; here their whole 2x2 outline, in walking order
                int from = Math.max(0, row);
                RaidEffects.cue(1, () -> {
                    for (int r = from; r < Math.min(ROWS, from + 3); r++) {
                        for (int c = 0; c < COLS; c++) {
                            if (path.contains(key(c, r))) {
                                drawTile(o, c, r, r == from ? MAZE_NEXT : MAZE_PATH);
                            }
                        }
                    }
                    return true;
                });
                inMaze.add(p);
                progress.add(new int[]{row, seg[0]});
            }
            if (!inMaze.isEmpty()) {
                int[] rows = new int[progress.size()];
                int[] starts = new int[progress.size()];
                for (int i = 0; i < rows.length; i++) {
                    rows[i] = progress.get(i)[0];
                    starts[i] = progress.get(i)[1];
                }
                arenaTick(o, inMaze, TobRules.mazeTornadoProgress(rows, starts));
            }
        }

        /** {@code leadRow}: the maze runners' progress the tornado reads ({@link TobRules#mazeTornadoProgress}). */
        private void arenaTick(BlockPos o, List<ServerPlayer> inMaze, int leadRow) {
            BlockPos a = arenaOrigin();
            // Each runner's red glow where they stand (OSRS), and (round 43) each runner's own trail: every path tile they have
            // walked stays lit and marked on the arena floor, their newest the brightest, so the others can follow each part of
            // the path at their own pace. // playability: OSRS lights the tile each runner stands on, read tick by tick
            for (ServerPlayer runner : inMaze) {
                int col = (int) Math.floor((runner.getX() - o.getX()) / 2);
                int row = (int) Math.floor((runner.getZ() - o.getZ()) / 2);
                if (col >= 0 && col < COLS && row >= 0 && row < ROWS) {
                    world().sendParticles(ParticleTypes.CRIMSON_SPORE, a.getX() + col * 2 + 1, a.getY() + 1.1, a.getZ() + row * 2 + 1, 6, 0.5,
                            0.1, 0.5, 0);
                }
            }
            walkedTiles.clear();
            List<Long> newest = new ArrayList<>();
            for (List<Long> trail : trails.values()) {
                for (long k : trail) {
                    if (!walkedTiles.contains(k)) {
                        walkedTiles.add(k);
                    }
                    if (!trailMarks.containsKey(k)) {
                        List<int[]> cols = new ArrayList<>();
                        addTile(cols, a, (int) (k >> 32), (int) k);
                        trailMarks.put(k, RaidFloor.columns(world(), cols, a.getY() + 1, RaidFloor.SAFE, 0, sotetseg,
                                new Vec3(a.getX() + (int) (k >> 32) * 2 + 1, a.getY() + 1, a.getZ() + (int) k * 2 + 1)));
                    }
                }
                if (!trail.isEmpty()) {
                    newest.add(trail.get(trail.size() - 1));
                }
            }
            List<Long> lit = new ArrayList<>(walkedTiles);
            RaidEffects.cue(1, () -> {
                for (long t2 : lit) {
                    drawTile(a, (int) (t2 >> 32), (int) t2, newest.contains(t2) ? MAZE_NEXT : MAZE_WALKED);
                }
                return true;
            });
            List<ServerPlayer> followers = new ArrayList<>();
            int furthest = leadRow;
            for (ServerPlayer p : players()) {
                if (runners.contains(p.getUUID())) {
                    continue;
                }
                followers.add(p);
                int[] tile = tileOf(a, p.position());
                if (tile == null) {
                    continue;
                }
                furthest = Math.max(furthest, tile[1]);
                if (offPath(a, p.position())) {
                    hurtRaw(sotetseg, p, TobRules.mazeWrongTile(Math.round(p.getHealth()), raid.mode()));
                    wrongTiles++;
                    nonBallHit = true;
                    fail();
                    if (!cooldown(p, 4)) {
                        Msg.red(p, "That tile isn't on the path!");
                    }
                }
            }
            tornadoTick(furthest, followers);
        }

        /**
         * The maze tornado (OSRS wiki): once anyone on the arena floor, or the runner lowest in orb order (through their own
         * part of the maze in hard mode), is past the third row it follows the path from the start; anyone it
         * catches brings its damage down on the whole party. It chases the players on the arena floor, or the maze runner
         * when they are alone in the encounter; stepping back to the third row makes it vanish.
         */
        private void tornadoTick(int furthestRow, List<ServerPlayer> followers) {
            if (pathOrder.isEmpty() || !TobRules.mazeTornado(furthestRow)) {
                tornado = -1;
                return;
            }
            boolean solo = followers.isEmpty();
            List<ServerPlayer> chased = new ArrayList<>(followers);
            if (solo) {
                for (UUID id : runners) {
                    ServerPlayer p = player(id);
                    if (p != null && p.isAlive()) {
                        chased.add(p);
                    }
                }
            }
            if (tornado < 0) {
                tornado = 0;
                tornadoTimer = 0;
                tornadoes++;
                warn("A tornado forms at the start of the maze and follows the path!");
            } else if (++tornadoTimer >= TobRules.SOTETSEG_TORNADO_TICKS_PER_TILE) {
                tornadoTimer = 0;
                tornado = Math.min(tornado + 1, pathOrder.size() - 1);
            }
            long k = pathOrder.get(tornado);
            BlockPos origin = solo ? mazeOrigin() : arenaOrigin();
            Vec3 at = new Vec3(origin.getX() + (int) (k >> 32) * 2 + 1, origin.getY() + 1, origin.getZ() + (int) k * 2 + 1);
            world().sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, at.x, at.y + 0.5, at.z, 3, 0.3, 0.6, 0.3, 0.01);
            boolean caught = false;
            for (ServerPlayer p : chased) {
                caught |= RaidEffects.horizontalDistSq(p.position(), at) < 1.2 * 1.2;
            }
            if (!caught) {
                return;
            }
            tornadoHits++;
            nonBallHit = true;
            fail(); // "Perfect Sotetseg": nobody may be hit by the tornado
            warn("The tornado catches up and tears through the party!");
            for (ServerPlayer p : raid.activePlayers()) {
                if (inside(p.position()) || runners.contains(p.getUUID())) {
                    hurtRaw(sotetseg, p, TobRules.SOTETSEG_TORNADO_MIN
                            + rnd().nextInt(TobRules.SOTETSEG_TORNADO_MAX - TobRules.SOTETSEG_TORNADO_MIN + 1));
                }
            }
            tornado = -1; // it dissipates; a new one forms at the start while anyone is still past the third row
        }

        @Override
        protected void onComplete() {
            super.onComplete();
            placeChest(this);
        }

        @Override
        public void cleanup() {
            super.cleanup();
            for (UUID id : runners) {
                ServerPlayer p = player(id);
                if (p != null && p.isAlive()) {
                    raid.teleport(p, at(-14, 0));
                }
            }
            runners.clear();
            segments.clear();
            trails.clear();
            releaseMazeMarks();
        }

        /**
         * Walks the shadow realm maze along the path, one row at a time, out at the far end. On the first maze it steps on a
         * wrong tile once (the shadows must lash out) and checks the tornado forms once the runner, alone in the encounter,
         * passes the third row.
         */
        private void runMaze(CoxTobWalk w) {
            BlockPos o = mazeOrigin();
            w.expect(runners.contains(w.player().getUUID()), "Sotetseg did not take the player into the shadow realm");
            if (mazes == 1) {
                int wrongCol = -1;
                for (int c = 0; c < COLS && wrongCol < 0; c++) {
                    if (!path.contains(key(c, 0))) {
                        wrongCol = c;
                    }
                }
                if (wrongCol >= 0) {
                    int before = wrongTiles;
                    w.moveTo(new Vec3(o.getX() + wrongCol * 2 + 1, o.getY() + 1, o.getZ() + 1));
                    w.tick(1);
                    w.expect(wrongTiles > before, "standing on a wrong maze tile did nothing");
                }
                // A foot over the edge of a path tile is forgiven; the middle of the wrong tile next to it is not (round 42).
                for (int c = 0; c < COLS; c++) {
                    if (path.contains(key(c, 0)) && c + 1 < COLS && !path.contains(key(c + 1, 0))) {
                        double edge = o.getX() + (c + 1) * 2;
                        w.expect(!offPath(o, new Vec3(edge + 0.25, o.getY() + 1, o.getZ() + 1)),
                                "a foot over the edge of a maze path tile counted as a wrong tile");
                        w.expect(offPath(o, new Vec3(edge + 1, o.getY() + 1, o.getZ() + 1)), "the middle of a wrong maze tile was forgiven");
                        break;
                    }
                }
            }
            for (int r = 0; r < ROWS; r++) {
                int col = -1;
                for (int c = 0; c < COLS && col < 0; c++) {
                    if (path.contains(key(c, r))) {
                        col = c;
                    }
                }
                w.expect(col >= 0, "the maze has no path tile in row " + r);
                w.moveTo(new Vec3(o.getX() + col * 2 + 1, o.getY() + 1, o.getZ() + r * 2 + 1));
                w.tick(1);
                if (r < TobRules.SOTETSEG_TORNADO_ROW && raid.players().size() == 1) {
                    w.expect(tornado < 0, "the maze tornado formed with the lone runner on row " + (r + 1));
                }
                if (r == TobRules.SOTETSEG_TORNADO_ROW + 1 && raid.players().size() == 1) {
                    w.expect(tornado >= 0, "no tornado followed the lone runner past the third row");
                }
            }
            w.moveTo(new Vec3(o.getX() + COLS, o.getY() + 1, o.getZ() + ROWS * 2 + 1.5));
            w.tick(1);
            w.expect(runners.isEmpty() && inside(w.player().position()), "walking out of the maze did not return the player to the arena");
            w.expect(com.runecraft.content.systems.SysCombat.drainedStatLevel(sotetseg, Skill.DEFENCE)
                    == com.runecraft.content.systems.SysCombat.baseStatLevel(sotetseg, Skill.DEFENCE), "the maze did not restore Sotetseg's Defence");
        }

        @Override
        public String walk(CoxTobWalk w) {
            // Round 43: an orb named mid-screen ("Red orb (magic) - pray Magic!") is answered through the prayer keys' path a
            // game tick later and blocked (his other attacks hold meanwhile, so none knocks the prayer off first).
            quietTicks = 12;
            int blocked = orbBlocks;
            int unblocked = orbHits;
            orb(w.player(), AttackType.MAGIC, false);
            w.pray(com.runecraft.osrs.Prayer.PROTECT_FROM_MAGIC, true);
            w.until(() -> orbBlocks > blocked || orbHits > unblocked, ORB_TICKS + 6, "Sotetseg's orb never landed");
            w.expect(orbBlocks == blocked + 1 && orbHits == unblocked, "a red orb answered with Protect from Magic a tick after its cue "
                    + "was not blocked");
            w.pray(com.runecraft.osrs.Prayer.PROTECT_FROM_MAGIC, false);
            quietTicks = 0;
            // OSRS wiki: its Defence can't be drained below 100.
            com.runecraft.content.systems.SysCombat.drainDefence(sotetseg, 500);
            w.tick(1);
            w.expect(com.runecraft.content.systems.SysCombat.drainedStatLevel(sotetseg, Skill.DEFENCE) == TobRules.SOTETSEG_DEFENCE_FLOOR,
                    "Sotetseg's Defence was drained to " + com.runecraft.content.systems.SysCombat.drainedStatLevel(sotetseg, Skill.DEFENCE)
                            + " (floor 100)");
            int guard = 0;
            while (alive(sotetseg) && guard++ < 1200) {
                if (!runners.isEmpty()) {
                    runMaze(w);
                    continue;
                }
                w.hit(sotetseg, Math.max(10, raid.maxHitpoints(sotetseg) / 15), AttackType.SLASH);
                w.tick(1);
            }
            w.expect(!alive(sotetseg), "Sotetseg did not die");
            w.expect(mazes == 2, "Sotetseg ran " + mazes + " mazes (expected two, at 66% and 33%)");
            w.expect(tornadoes > 0, "the maze tornado never formed");
            BlockPos chest = w.block(this, RaidsContent.SUPPLIES);
            w.moveTo(chest.offset(-1, 0, 0));
            w.use(chest);
            int points = TobRooms.points(raid, w.player());
            w.expect(points >= 6 - (hard() ? TobRules.HARD_CHEST_POINTS_LESS : 0), "the second supply chest gave no points (" + points + " left)");
            return mazes + " mazes, " + balls + " death ball(s); " + points + " supply points";
        }
    }

    // ================================================================== Xarpus

    /**
     * Xarpus (OSRS wiki). Phase 1: weakened to 75% health, he doesn't attack but unearths exhumeds; one nobody stands on
     * heals him. After 12 exhumeds (15 for four or five players) he wakes. Phase 2: he spits poison at players; the
     * splatter stays on its tile for the rest of the fight (4-8 a tick, more for every exhumed absorbed); anyone under him
     * is pelted for up to 9 a tick. Phase 3 at 25%: he stops spitting and stares at one quadrant at a time, turning every 8
     * ticks (never back to the same one); attacking from the quadrant he stares at draws a 50-75 poison retaliation. When
     * he dies the Dawnbringer is left for Verzik.
     */
    static final class Xarpus extends TobRoom {
        private record Exhumed(Vec3 pos, int expires) {
        }

        private DataBossEntity xarpus;
        private int phase;
        private int quadrant = -1;
        private int absorbed;
        private int resolved;
        private int intercepted;
        private int turnTimer;
        private final List<Exhumed> exhumeds = new ArrayList<>();
        /** Poisoned tiles: they stay for the rest of the fight. */
        private final List<Vec3> pools = new ArrayList<>();
        /** Hard mode: times he turned to face a successful attacker (self-test). */
        private int forcedTurns;
        /** Tiles each side of Xarpus's arena centre (OSRS: a 16 x 16 tile arena; the Exhumed map). */
        private static final int ARENA_HALF = 8;

        Xarpus(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index, CombatAchievements.FLAG_XARPUS_IMPERFECT);
        }

        @Override
        public void decorate() {
            BlockPos c = center();
            RaidBuilder.fill(world(), c.getX() - 2, c.getY(), c.getZ() - 2, c.getX() + 2, c.getY(), c.getZ() + 2,
                    Blocks.DARK_PRISMARINE.defaultBlockState());
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    RaidBuilder.set(world(), c.offset(sx * 3, 1, sz * 3), Blocks.SEA_PICKLE.defaultBlockState());
                }
            }
        }

        @Override
        public void start() {
            phase = 1;
            absorbed = 0;
            resolved = 0;
            quadrant = -1;
            exhumeds.clear();
            pools.clear();
            xarpus = spawnBoss("xarpus", at(0, 0));
            if (xarpus != null) {
                anchor(xarpus);
                xarpus.setNoAi(true);
                raid.setCurrentHitpoints(xarpus, raid.maxHitpoints(xarpus) * 3 / 4);
            }
            if (hard()) {
                // OSRS wiki: the Hard mode fight opens with poison over the two outer rings of his arena, except the six
                // tiles at the entrance.
                for (int tx = -ARENA_HALF; tx <= ARENA_HALF; tx++) {
                    for (int tz = -ARENA_HALF; tz <= ARENA_HALF; tz++) {
                        if (TobRules.xarpusHardEdge(tx, tz, ARENA_HALF)) {
                            pools.add(vec(tx * 2, tz * 2));
                            markPool(vec(tx * 2, tz * 2));
                        }
                    }
                }
                warn("Xarpus spits poison across the edges of his arena!");
            }
        }

        @Override
        public boolean bossVulnerable(DataBossEntity boss) {
            return phase > 1;
        }

        /** Yaw for a quadrant (0-3: south-east, south-west, north-west, north-east as he faces them). */
        private static float yaw(int q) {
            return new float[]{-45, 45, 135, -135}[Math.floorMod(q, 4)];
        }

        /** How close (squared, horizontally) a player must stand to an exhumed to stop it. */
        private static final double EXHUMED_REACH_SQ = 1.2;

        /** The corner Xarpus stares at, by compass ("north-east"...). */
        String stareCompass() {
            double a = Math.toRadians(yaw(quadrant));
            return com.runecraft.osrs.content.raids.RaidRealtime.compass(-Math.sin(a), Math.cos(a));
        }

        @Override
        public void bossTick(DataBossEntity boss) {
            if (phase == 3) {
                boss.setYRot(yaw(quadrant));
                boss.setYHeadRot(yaw(quadrant));
                boss.setYBodyRot(yaw(quadrant));
            }
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            t++;
            if (!alive(xarpus)) {
                return;
            }
            reflag(xarpus);
            List<ServerPlayer> players = players();
            tickPools(players);
            if (phase == 1) {
                int total = TobRules.xarpusExhumeds(raid.partySize());
                if (t % TobRules.XARPUS_EXHUMED_INTERVAL == 0 && resolved + exhumeds.size() < total && !players.isEmpty()) {
                    ServerPlayer p = players.get(rnd().nextInt(players.size()));
                    Exhumed e = new Exhumed(exhumedTile(p), t + TobRules.XARPUS_EXHUMED_TICKS);
                    exhumeds.add(e);
                    // Round 43: its tile marked on the floor (where to stand) until it resolves. // playability: OSRS shows the
                    // acid bubbling on its tile
                    RaidFloor.disc(world(), e.pos(), 0.1, RaidFloor.SAFE, TobRules.XARPUS_EXHUMED_TICKS, xarpus);
                    exhumedMarks++;
                    // Where to stand to stop it: its true reach in green until it is resolved. // playability: OSRS shows the
                    // acid bubbling up on one tile
                    RaidEffects.cue(TobRules.XARPUS_EXHUMED_TICKS, () -> {
                        if (!exhumeds.contains(e)) {
                            return false;
                        }
                        com.runecraft.boss.ThreatCues.ring(world(), e.pos(), Math.sqrt(EXHUMED_REACH_SQ), 0x73FF73, null, false);
                        return true;
                    });
                }
                for (int i = exhumeds.size() - 1; i >= 0; i--) {
                    Exhumed e = exhumeds.get(i);
                    world().sendParticles(ParticleTypes.SNEEZE, e.pos().x, e.pos().y + 0.1, e.pos().z, 5, 0.3, 0.02, 0.3, 0);
                    if (t < e.expires()) {
                        continue;
                    }
                    exhumeds.remove(i);
                    resolved++;
                    ServerPlayer absorber = null;
                    for (ServerPlayer p : players) {
                        if (RaidEffects.horizontalDistSq(p.position(), e.pos()) < EXHUMED_REACH_SQ) {
                            absorber = p;
                            break;
                        }
                    }
                    if (absorber != null) {
                        intercepted++;
                        RaidEffects.hit(null, absorber, 3, null, true);
                        absorber.heal(3);
                    } else {
                        raid.heal(xarpus, Math.max(1, raid.maxHitpoints(xarpus) / 25)); // approx
                        absorbed++;
                        if (absorbed > 2) {
                            fail(); // "Perfect Xarpus": an exhumed may heal him at most twice
                        }
                        say("Xarpus absorbs the exhumed acid!");
                        if (raid.healthFraction(xarpus) >= 0.999) {
                            CombatAchievementService.flagNear(xarpus, CombatAchievements.FLAG_XARPUS_FULL_HEAL);
                        }
                    }
                }
                if (resolved >= total) {
                    phase = 2;
                    exhumeds.clear();
                    warn("Xarpus awakens!");
                }
                return;
            }
            // Pebbles for anyone standing under him.
            for (ServerPlayer p : players) {
                if (RaidEffects.horizontalDistSq(p.position(), xarpus.position()) < 1.5 * 1.5) {
                    hurt(xarpus, p, rnd().nextInt(TobRules.XARPUS_STOMP_MAX + 1), null, true);
                }
            }
            if (phase == 2 && t % 4 == 0 && !players.isEmpty()) {
                ServerPlayer p = players.get(rnd().nextInt(players.size()));
                Vec3 at = p.position();
                world().sendParticles(ParticleTypes.ITEM_SLIME, at.x, at.y + 0.2, at.z, 10, 0.8, 0.05, 0.8, 0);
                // The spit lands as a raid strike, its splash outlined from the spit. // playability: OSRS shows the projectile
                DataBossEntity source = xarpus;
                RaidEffects.mark(world(), at, 1.5).delay(3).source(source).warn(ParticleTypes.ITEM_SLIME).floor(RaidFloor.POISON).live(current())
                        .onHit(q -> hurt(source, q, 1 + rnd().nextInt(4), null, true)) // the splash "lightly damages"
                        .onLand(() -> {
                            pools.add(at);
                            markPool(at);
                        }).start();
            }
            if (phase == 2 && raid.healthFraction(xarpus) <= TobRules.XARPUS_SCREECH_AT) {
                phase = 3;
                quadrant = rnd().nextInt(4);
                turnTimer = TobRules.XARPUS_TURN_TICKS;
                warn("Xarpus shrieks and stares intently at one corner! Do not attack him from where he looks!");
                RaidEffects.telegraph(players, "Xarpus stares " + stareCompass() + " - don't attack him from there!");
            }
            if (phase == 3 && --turnTimer <= 0) {
                turnTimer = TobRules.XARPUS_TURN_TICKS;
                quadrant = TobRules.xarpusNextQuadrant(quadrant, rnd());
                say("Xarpus turns to stare at another corner.");
                // the corner named by compass. // playability: OSRS shows only his head turning
                RaidEffects.telegraph(players, "Xarpus stares " + stareCompass() + " - don't attack him from there!");
            }
        }

        /**
         * The tile a new exhumed rises from. OSRS: exhumeds come up out of the floor of Xarpus's arena (the wiki's Exhumed
         * map covers the arena's tiles), and a player stops one by standing on its tile. Here they rise within 4 tiles of a
         * player (// approx: the room is wider than his 16x16 arena, so one anywhere would be out of reach), always on a
         * free floor tile of the room: never in its walls, corner columns or the doorway, where nobody standing on it is in
         * the arena.
         */
        private Vec3 exhumedTile(ServerPlayer p) {
            for (int tries = 0; tries < 16; tries++) {
                Vec3 at = tile(p.getBlockX() + rnd().nextInt(9) - 4, p.getBlockZ() + rnd().nextInt(9) - 4);
                if (onArenaFloor(at)) {
                    return at;
                }
            }
            return tile(p.getBlockX(), p.getBlockZ()); // the player's own tile (players() only holds players in the arena)
        }

        /** Exhumed tiles marked on the floor (self-test). */
        int exhumedMarks;

        /**
         * A poison pool's 3x3 marked on the floor for the rest of the fight (round 43): what hurts is what is marked (a pool
         * hurts within 1.5 blocks each way). // playability: OSRS paints the poison on the tiles
         */
        private void markPool(Vec3 at) {
            BlockPos b = BlockPos.containing(at);
            RaidFloor.rect(world(), b.getX() - 1, b.getZ() - 1, b.getX() + 1, b.getZ() + 1, at.y, RaidFloor.POISON, 0, xarpus);
        }

        /** The standing position at the centre of the block column (x, z). */
        private Vec3 tile(int x, int z) {
            return new Vec3(x + 0.5, center().getY() + 1, z + 0.5);
        }

        /** Whether a standing position is on a free floor tile of the arena (inside the room, not in a wall or column). */
        private boolean onArenaFloor(Vec3 at) {
            BlockPos b = BlockPos.containing(at);
            return inside(at) && b.getY() == center().getY() + 1 && !world().getBlockState(b).isCollisionShapeFullBlock(world(), b)
                    && !world().getBlockState(b.above()).isCollisionShapeFullBlock(world(), b.above())
                    && world().getBlockState(b.below()).isCollisionShapeFullBlock(world(), b.below());
        }

        /** A position relative to the room centre, in blocks (x east, z south). */
        private String where(Vec3 pos) {
            return Math.round(pos.x - (center().getX() + 0.5)) + ", " + Math.round(pos.z - (center().getZ() + 0.5));
        }

        private void tickPools(List<ServerPlayer> players) {
            if (t % 3 == 0) {
                for (Vec3 pool : pools) {
                    world().sendParticles(ParticleTypes.ITEM_SLIME, pool.x, pool.y + 0.1, pool.z, 2, 0.4, 0.02, 0.4, 0);
                }
            }
            for (ServerPlayer p : players) {
                for (Vec3 pool : pools) {
                    if (Math.abs(p.getX() - pool.x) < 1.5 && Math.abs(p.getZ() - pool.z) < 1.5) {
                        hurt(xarpus, p, TobRules.xarpusPoison(4 + rnd().nextInt(5), absorbed), null, true);
                        break;
                    }
                }
            }
        }

        /** The quadrant (0-3) a position lies in, seen from Xarpus. */
        private int quadrantOf(Vec3 pos) {
            double dx = pos.x - xarpus.getX();
            double dz = pos.z - xarpus.getZ();
            float angle = (float) Math.toDegrees(Math.atan2(-dx, dz));
            int best = 0;
            for (int q = 1; q < 4; q++) {
                if (Math.abs(Mth.wrapDegrees(angle - yaw(q))) < Math.abs(Mth.wrapDegrees(angle - yaw(best)))) {
                    best = q;
                }
            }
            return best;
        }

        @Override
        public int bossIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (phase != 3) {
                return damage;
            }
            int from = quadrantOf(attacker.position());
            if (from == quadrant) {
                Msg.red(attacker, "Xarpus is staring in your direction and retaliates with a powerful attack!");
                CombatAchievementService.flagNear(boss, CombatAchievements.FLAG_XARPUS_REFLECT);
                hurt(boss, attacker, TobRules.xarpusRetaliation(50 + rnd().nextInt(26), absorbed), null, true);
            } else if (hard() && damage > 0) {
                // Hard mode (OSRS wiki): a successful hit turns him to stare at the attacker's quadrant at once.
                quadrant = TobRules.xarpusHardStare(quadrant, from, true);
                turnTimer = TobRules.XARPUS_TURN_TICKS;
                forcedTurns++;
            }
            return damage;
        }

        @Override
        protected void onComplete() {
            super.onComplete();
            pools.clear();
            // OSRS: the Dawnbringer waits with a skeleton by the entrance to Verzik's throne room.
            say("Xarpus falls. A skeleton by the entrance to Verzik's throne room still clutches the Dawnbringer.");
        }

        @Override
        public String walk(CoxTobWalk w) {
            w.expect(alive(xarpus) && !bossVulnerable(xarpus), "Xarpus did not start weakened and unattackable");
            w.expect(raid.healthFraction(xarpus) < 0.76, "Xarpus did not start at 75% health");
            w.expect(!hard() || pools.size() > 4 * ARENA_HALF * 2, "the Hard mode fight did not open with poison on the arena's edges ("
                    + pools.size() + " tiles)");
            if (hard()) {
                Vec3 entrance = vec(-ARENA_HALF * 2, 0);
                for (Vec3 pool : pools) {
                    w.expect(RaidEffects.horizontalDistSq(pool, entrance) > 1, "the Hard mode opening poison covers the entrance");
                }
            }
            // Stand on the first exhumed so it can't heal him. OSRS: exhumeds rise from the arena floor, so every one must be
            // on a free floor tile of the room (one in the wall or the doorway can't be stood on from inside the arena).
            w.until(() -> !exhumeds.isEmpty(), 10, "no exhumed appeared");
            Exhumed first = exhumeds.get(0);
            w.expect(onArenaFloor(first.pos()), "the first exhumed rose off the arena floor (at " + where(first.pos()) + ")");
            // round 43: its tile is marked on the floor while it bubbles
            w.expect(exhumedMarks > 0 && com.runecraft.boss.FloorMarkers.marked(world(), BlockPos.containing(first.pos()).below()),
                    "the first exhumed's tile was not marked on the floor (at " + where(first.pos()) + ")");
            w.moveTo(first.pos());
            w.until(() -> intercepted > 0 || phase > 1, TobRules.XARPUS_EXHUMED_TICKS + 2, "standing on an exhumed (at " + where(first.pos())
                    + ") did not stop it");
            for (int i = 0; i < 120 && phase == 1; i++) {
                for (Exhumed e : exhumeds) {
                    w.expect(onArenaFloor(e.pos()), "an exhumed rose off the arena floor (at " + where(e.pos()) + ")");
                }
                w.tick(1);
            }
            w.expect(phase > 1, "Xarpus never woke (" + resolved + "/" + TobRules.xarpusExhumeds(raid.partySize()) + " exhumeds; waited 120 ticks)");
            int guard = 0;
            while (alive(xarpus) && guard++ < 800) {
                if (phase == 3) {
                    // Attack from the corner opposite the one he stares at.
                    float yaw = yaw(quadrant + 2);
                    double rad = Math.toRadians(yaw);
                    w.moveTo(new Vec3(xarpus.getX() - Math.sin(rad) * 4, xarpus.getY(), xarpus.getZ() + Math.cos(rad) * 4));
                    w.expect(quadrantOf(w.player().position()) != quadrant, "could not find a corner Xarpus isn't staring at");
                }
                boolean p3 = phase == 3;
                int dealt = w.hit(xarpus, Math.max(10, raid.maxHitpoints(xarpus) / 15), AttackType.RANGED);
                if (p3 && hard() && dealt > 0 && alive(xarpus)) {
                    w.expect(quadrant == quadrantOf(w.player().position()), "a successful Hard mode hit did not turn Xarpus to the attacker");
                }
                w.tick(1);
            }
            w.expect(!alive(xarpus), "Xarpus did not die (phase " + phase + ")");
            w.expect(!hard() || forcedTurns > 0, "Hard mode Xarpus never turned to face an attacker");
            return intercepted + " exhumed(s) stopped, " + absorbed + " absorbed" + (hard() ? ", " + forcedTurns + " forced turn(s)" : "");
        }
    }

    // ================================================================== Verzik Vitur

    /**
     * Verzik Vitur (OSRS wiki "Verzik Vitur" with its infobox, which settles the numbers the Strategies page disputes).
     * Phase 1 on her throne: her shield caps hits at 10 (melee) and 3 (ranged, magic) except the Dawnbringer's (range 7
     * tiles; it drops to the floor when its holder dies or leaves); every 14 ticks a blast of up to 137 (halved by Protect
     * from Magic) hits everyone she can see, a pillar absorbing it for anyone behind it (Hard mode: debris falls around a
     * struck pillar). Phase 2: urnbombs at every player (up to 44, halved by Protect from Missiles; Hard mode ~20% more and a
     * poison pool), a body slam ("bounce", up to 50, knocked back and stunned) on anyone in reach, a stomp (82) under her,
     * and every fifth attack a bouncing lightning ball (48, 25 in insulated boots); combat nylocas home in on a player and
     * explode for up to 63; a Nylocas Athanatos thrown at a player (78 where it lands) heals her until a poison hit or a
     * Smoke spell's poison bursts it for up to 70 on her. Below 35% she periodically summons Nylocas Matomenos (200
     * hitpoints) and absorbs what is left of them, and mostly casts a healing spell (up to 45, halved by prayer, healing
     * half). Phase 3: every 7 ticks projectiles at everyone (up to 33, halved by the right prayer) or a melee hit (63) on
     * anyone next to her; after every four attacks a special in turn: nylocas, webs, yellows (stand alone on a pool or take
     * up to 80; three strikes in Hard mode), the green ball (75% of the Hitpoints level unless bounced; lethal unbounced in
     * Hard mode with company). At 20% she enrages: attacks every 5 ticks, projectiles up to 34, and a purple tornado chases
     * every player (half their hitpoints, at least 5, healing her three times that). Hard mode: at 5% she heals 30% once.
     * Her Defence can't be lowered.
     */
    static final class Verzik extends TobRoom {
        private static final int[][] PILLARS = {{0, -7}, {0, 7}, {-8, -7}, {-8, 7}};
        private static final TobNylocas.Breed[] BREEDS = TobNylocas.Breed.values();

        /** A combat nylocas of phases 2 and 3: the player it follows, its age and (Hard mode) its place in the style cycle. */
        private static final class Crab {
            Mob mob;
            ServerPlayer target;
            int age;
            int breed;

            Crab(Mob mob, ServerPlayer target, int breed) {
                this.mob = mob;
                this.target = target;
                this.breed = breed;
            }
        }

        private DataBossEntity verzik;
        private int form;
        private final int[] pillarBlasts = new int[PILLARS.length];
        private boolean pillarLost;
        private int attacks;
        private int lightnings;
        private boolean p25;
        private int summoning;
        private int matomenosTimer;
        private final List<Mob> matomenos = new ArrayList<>();
        private final List<Crab> crabs = new ArrayList<>();
        private LivingEntity athanatos;
        private int athanatosRespawn = -1;
        private boolean enraged;
        private boolean lastStand;
        private int invulnerable;
        private final Map<UUID, Vec3> tornadoes = new HashMap<>();
        private final Map<UUID, Integer> tornadoRespawn = new HashMap<>();
        private final Map<BlockPos, Integer> webs = new HashMap<>();
        private final Map<UUID, Integer> araneaReady = new HashMap<>();
        private final List<Vec3> yellows = new ArrayList<>();
        /** Hard mode urnbomb poison pools and the tick each dries up. */
        private final Map<Vec3, Integer> urnPools = new HashMap<>();
        private int specials;
        /** Task conditions (combat achievements) and self-test counters. */
        private boolean nyloDamage;
        private boolean nyloPopped;
        private boolean p3Melee;
        private boolean bounced;
        private boolean greenPassed;
        private boolean teamwork;
        private boolean yellowHit;
        private int crabsPopped;
        private int crabsCycled;
        private int athanatosLandings;
        private int athanatosBursts;
        private int athanatosRespawns;
        private int matomenosAbsorbed;
        private int matomenosHealed;
        private int debris;
        private int slams;
        private int urnsLanded;
        private int urnPoolsMade;
        private int lastLightningMax;
        private int absorbedDrains;

        Verzik(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index, CombatAchievements.FLAG_VERZIK_IMPERFECT);
        }

        @Override
        public void decorate() {
            BlockPos c = center();
            RaidBuilder.fill(world(), c.getX() + 12, c.getY(), c.getZ() - 4, c.getX() + 18, c.getY() + 1, c.getZ() + 4,
                    Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            RaidBuilder.fill(world(), c.getX() + 12, c.getY() + 1, c.getZ() - 1, c.getX() + 13, c.getY() + 1, c.getZ() + 1,
                    Blocks.RED_CARPET.defaultBlockState());
            pillars(Blocks.STONE_BRICKS.defaultBlockState());
            RaidBuilder.set(world(), skeleton(), RaidBuilder.module(RaidsContent.TOB_DAWNBRINGER_SKELETON));
        }

        /** The skeleton holding the Dawnbringer, next to the entrance of the throne room (OSRS wiki). */
        private BlockPos skeleton() {
            return at(-RaidBuilder.HALF + 2, 3);
        }

        /** The Verzik room a player is in, if any. */
        static Verzik of(RaidInstance raid, Vec3 pos) {
            int room = raid == null ? -1 : raid.roomAt(pos);
            return room >= 0 && raid.logic(room) instanceof Verzik v ? v : null;
        }

        /** Dawnbringers still in play: carried by a living member of the party or lying in this room. */
        private int dawnbringers() {
            int n = 0;
            for (ServerPlayer p : raid.players()) {
                if (!raid.isDowned(p)) {
                    n += Inv.count(p, TobBosses.DAWNBRINGER);
                }
            }
            return n + floorDawnbringers();
        }

        /**
         * Dawnbringers dropped in this room by a holder who died or left, until someone picks them up. Kept by reference as
         * well as searched for: an item entity in a chunk the server has not yet confirmed as loaded (a room built moments
         * before) is left out of area searches, and must still count as lying on the floor.
         */
        private final List<ItemEntity> dropped = new ArrayList<>();

        /** The Dawnbringer item entities lying in this room. */
        List<ItemEntity> floorItems() {
            dropped.removeIf(e -> e.isRemoved() || !TobBosses.DAWNBRINGER.equals(Inv.idOf(e.getItem())));
            List<ItemEntity> found = new ArrayList<>(dropped);
            for (ItemEntity e : world().getEntitiesOfClass(ItemEntity.class, RaidBuilder.roomBox(raid.slot, index),
                    e -> TobBosses.DAWNBRINGER.equals(Inv.idOf(e.getItem())))) {
                if (!found.contains(e)) {
                    found.add(e);
                }
            }
            return found;
        }

        /** Dawnbringers lying on the floor of this room. */
        int floorDawnbringers() {
            int n = 0;
            for (ItemEntity e : floorItems()) {
                n += e.getItem().getCount();
            }
            return n;
        }

        @Override
        public boolean useBlock(ServerPlayer player, String blockId, BlockPos pos) {
            if (!blockId.equals(RaidsContent.TOB_DAWNBRINGER_SKELETON)) {
                return false;
            }
            if (form != 1) {
                Msg.game(player, "They battled through challenge after challenge, but didn't quite make it...");
                return true;
            }
            // OSRS: searchable once per raid; one lost to a leaver can be found again, so the shield can always be broken.
            if (dawnbringers() > 0) {
                Msg.game(player, Inv.has(player, TobBosses.DAWNBRINGER, 1) ? "You already have the Dawnbringer."
                        : "The skeleton's hands are empty: someone has already taken the Dawnbringer.");
                return true;
            }
            Inv.give(player, TobBosses.DAWNBRINGER, 1);
            Msg.game(player, "You find the Dawnbringer; you feel a pulse of energy burst through it.");
            return true;
        }

        /** OSRS: once Verzik's shield is destroyed the Dawnbringer crumbles to dust. */
        private void crumbleDawnbringers() {
            for (ServerPlayer p : raid.players()) {
                if (Inv.remove(p, TobBosses.DAWNBRINGER, Inv.count(p, TobBosses.DAWNBRINGER)) > 0) {
                    Msg.game(p, "The weapon falls apart in your hand as Verzik's shield is destroyed.");
                }
            }
            floorItems().forEach(ItemEntity::discard);
            dropped.clear();
        }

        /**
         * Whether the Dawnbringer reaches Verzik from where the player stands: its attack range is 7 tiles (14 blocks) from
         * the edge of her throne-sized body (OSRS wiki "Dawnbringer").
         */
        static boolean dawnbringerReaches(ServerPlayer p, LivingEntity verzik) {
            double reach = TobRules.DAWNBRINGER_RANGE_TILES * 2 + verzik.getBbWidth() / 2 + 0.5;
            return RaidEffects.horizontalDistSq(p.position(), verzik.position()) <= reach * reach;
        }

        private void pillars(BlockState state) {
            for (int[] p : PILLARS) {
                RaidBuilder.pillar(world(), center().offset(p[0], 0, p[1]), 1, 5, state);
            }
        }

        private Vec3 pillarCentre(int i) {
            return Vec3.atBottomCenterOf(center().offset(PILLARS[i][0], 1, PILLARS[i][1]));
        }

        private int shelter(ServerPlayer p) {
            int best = -1;
            double bestD = 3.5 * 3.5;
            for (int i = 0; i < PILLARS.length; i++) {
                if (pillarBlasts[i] < 0) {
                    continue;
                }
                double d = RaidEffects.horizontalDistSq(p.position(), pillarCentre(i));
                if (d < bestD) {
                    bestD = d;
                    best = i;
                }
            }
            return best;
        }

        private void collapse(int i) {
            pillarBlasts[i] = -1;
            Vec3 c = pillarCentre(i);
            RaidBuilder.pillar(world(), center().offset(PILLARS[i][0], 0, PILLARS[i][1]), 1, 5, Blocks.AIR.defaultBlockState());
            world().sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 1, c.z, 4, 1, 1, 1, 0);
            int max = RaidScaling.verzikPillarCollapse(raid.mode());
            for (ServerPlayer p : players()) {
                if (RaidEffects.horizontalDistSq(p.position(), c) <= 2.6 * 2.6) {
                    hurt(verzik, p, rnd().nextInt(max + 1), null, true);
                    com.runecraft.content.systems.SysCombat.freeze(p, 3);
                    Vec3 push = p.position().subtract(c).multiply(1, 0, 1).normalize().scale(0.8);
                    p.push(push.x, 0.3, push.z);
                    p.hurtMarked = true;
                    Msg.red(p, "The pillar collapses on top of you!");
                }
            }
        }

        /** Hard mode: a blast striking a pillar brings down debris within 3 tiles of it (up to 35 and a stun each). */
        private void fallingDebris(int i) {
            Vec3 c = pillarCentre(i);
            int reach = TobRules.VERZIK_HARD_DEBRIS_TILES * 2 + 1;
            for (int k = 0; k < TobRules.VERZIK_HARD_DEBRIS; k++) {
                Vec3 at = c.add(rnd().nextInt(reach * 2 + 1) - reach, 0, rnd().nextInt(reach * 2 + 1) - reach);
                world().sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 3, at.z, 12, 0.4, 0.3, 0.4, 0);
                later(2, () -> {
                    for (ServerPlayer p : players()) {
                        if (RaidEffects.horizontalDistSq(p.position(), at) < 1.0) {
                            debris++;
                            hurt(verzik, p, rnd().nextInt(TobRules.VERZIK_HARD_DEBRIS_MAX + 1), null, true);
                            com.runecraft.content.systems.SysCombat.freeze(p, 3);
                            Msg.red(p, "Debris from the pillar falls on you!");
                        }
                    }
                });
            }
        }

        @Override
        public void start() {
            form = 1;
            java.util.Arrays.fill(pillarBlasts, 0);
            pillarLost = false;
            attacks = 0;
            lightnings = 0;
            p25 = false;
            summoning = 0;
            enraged = false;
            lastStand = false;
            invulnerable = 0;
            specials = 0;
            athanatos = null;
            athanatosRespawn = -1;
            crabs.clear();
            matomenos.clear();
            tornadoes.clear();
            tornadoRespawn.clear();
            yellows.clear();
            urnPools.clear();
            // A retry (Entry mode) is a new fight for the combat achievements too.
            nyloDamage = nyloPopped = p3Melee = bounced = greenPassed = teamwork = yellowHit = false;
            // A retry (Entry mode) finds the throne room as it was: the pillars and the skeleton stand again.
            pillars(Blocks.STONE_BRICKS.defaultBlockState());
            RaidBuilder.set(world(), skeleton(), RaidBuilder.module(RaidsContent.TOB_DAWNBRINGER_SKELETON));
            verzik = spawnBoss("verzik_vitur", at(15, 0).above());
            if (verzik != null) {
                verzik.setNoAi(true);
            }
            anchor(verzik);
        }

        @Override
        public boolean bossVulnerable(DataBossEntity boss) {
            return boss != verzik || invulnerable <= 0;
        }

        /**
         * Verzik's boss bar (round 43, {@code // playability}): her phase of three and what comes next, counted from the room's
         * own counters: phase 1 the seconds to her next charged blast (hide behind a pillar), phase 2 the attacks to her lightning
         * (every fifth), phase 3 the attacks to her next special and which one (crabs, webs, yellows, green ball, in order). OSRS
         * players count these themselves.
         */
        @Override
        public com.runecraft.osrs.BossBarStatus.Status bossBarStatus(DataBossEntity boss) {
            com.runecraft.osrs.BossBarStatus.Status s = super.bossBarStatus(boss);
            if (boss != verzik) {
                return s;
            }
            s = s.with(com.runecraft.osrs.BossBarStatus.phaseText(form - 2, 2));
            return switch (form) {
                case 1 -> s.with(com.runecraft.osrs.BossSwitches.inSeconds("blast", com.runecraft.osrs.BossSwitches.verzikBlastIn(t)));
                case 2 -> s.with(com.runecraft.osrs.BossSwitches.nextText("lightning", com.runecraft.osrs.BossSwitches.verzikLightningIn(attacks)));
                default -> s.with(com.runecraft.osrs.BossSwitches.nextText(com.runecraft.osrs.BossSwitches.verzikSpecialName(
                        com.runecraft.osrs.BossSwitches.verzikNextSpecial(specials)), com.runecraft.osrs.BossSwitches.verzikSpecialIn(attacks)))
                        .with(enraged ? "enraged" : "");
            };
        }

        @Override
        public int bossIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (boss != verzik) {
                return damage;
            }
            if (form == 1) {
                boolean dawnbringer = TobBosses.DAWNBRINGER.equals(Inv.idOf(attacker.getMainHandItem()));
                return TobRules.verzikP1Cap(damage, type != null && type.isMelee(), dawnbringer);
            }
            if (form == 3 && type != null && type.isMelee()) {
                p3Melee = true; // "Harder Mode III": nobody may hit her with melee in phase 3
            }
            if (form == 2 && summoning > 0 && damage > 0) {
                // OSRS: during the first moments of a phase 2.5 summon, damage heals her.
                raid.heal(verzik, damage);
                Msg.action(attacker, "Verzik absorbs the blow and heals!");
                return 0;
            }
            return damage;
        }

        @Override
        public void gameTick(long tick) {
            t++;
            if (verzik == null) {
                return;
            }
            Vec3 pos = verzik.position();
            if (!alive(verzik)) {
                if (form == 1) {
                    form = 2;
                    crumbleDawnbringers();
                    for (int i = 0; i < PILLARS.length; i++) {
                        if (pillarBlasts[i] >= 0) {
                            collapse(i);
                        }
                    }
                    pillars(Blocks.AIR.defaultBlockState());
                    verzik = spawnBoss("verzik_vitur_p2", BlockPos.containing(vec(0, 0)));
                    if (verzik != null) {
                        verzik.setNoAi(true);
                    }
                    attacks = 0;
                    warn("Verzik Vitur: Let's see how you handle this! The pillars crumble!");
                    summonNylocas(true);
                } else if (form == 2) {
                    form = 3;
                    for (Mob m : matomenos) {
                        if (m.isAlive()) {
                            m.discard();
                        }
                    }
                    matomenos.clear();
                    verzik = spawnBoss("verzik_vitur_p3", BlockPos.containing(pos));
                    if (verzik != null) {
                        verzik.setNoAi(true);
                    }
                    attacks = 0;
                    warn("Verzik Vitur: Behold my true nature!");
                } else {
                    complete();
                }
                return;
            }
            reflag(verzik);
            flagTasks();
            absorbDrains();
            if (invulnerable > 0) {
                invulnerable--;
            }
            List<ServerPlayer> players = players();
            tickCrabs(players);
            switch (form) {
                case 1 -> phaseOne();
                case 2 -> phaseTwo(players);
                default -> phaseThree(players);
            }
        }

        /** Combat achievement conditions on everyone fighting her. */
        private void flagTasks() {
            if (nyloDamage) {
                CombatAchievementService.flagNear(verzik, CombatAchievements.FLAG_VERZIK_NYLO_DAMAGE);
            }
            if (nyloPopped) {
                CombatAchievementService.flagNear(verzik, CombatAchievements.FLAG_VERZIK_NYLO_POPPED);
            }
            if (p3Melee) {
                CombatAchievementService.flagNear(verzik, CombatAchievements.FLAG_VERZIK_P3_MELEE);
            }
            if (bounced) {
                CombatAchievementService.flagNear(verzik, CombatAchievements.FLAG_VERZIK_BOUNCED);
            }
            if (greenPassed) {
                CombatAchievementService.flagNear(verzik, CombatAchievements.FLAG_VERZIK_GREEN_PASSED);
            }
            if (teamwork) {
                CombatAchievementService.flagNear(verzik, CombatAchievements.FLAG_VERZIK_TEAMWORK);
            }
        }

        /** OSRS: her Defence can't be lowered ("Verzik Vitur absorbs the defence reduction!"). */
        private void absorbDrains() {
            if (com.runecraft.content.systems.SysCombat.drainedStatLevel(verzik, Skill.DEFENCE)
                    < com.runecraft.content.systems.SysCombat.baseStatLevel(verzik, Skill.DEFENCE)) {
                com.runecraft.content.systems.SysCombat.forget(verzik);
                absorbedDrains++;
                say("Verzik Vitur absorbs the defence reduction!");
            }
        }

        // ---------------- phase 1

        private void phaseOne() {
            if (pillarLost) {
                CombatAchievementService.flagNear(verzik, CombatAchievements.FLAG_VERZIK_PILLAR_LOST);
            }
            if (t % TobRules.VERZIK_P1_BLAST_TICKS != 6) {
                return;
            }
            warn("Verzik charges a blast of energy! Hide behind a pillar!");
            later(2, () -> {
                if (!alive(verzik) || form != 1) {
                    return;
                }
                Set<Integer> struck = new HashSet<>();
                for (ServerPlayer p : players()) {
                    if (canSee(verzik, p)) {
                        Vec3 a = verzik.getEyePosition();
                        Vec3 b = p.getEyePosition();
                        for (int i = 0; i <= 10; i++) {
                            Vec3 v = a.lerp(b, i / 10.0);
                            world().sendParticles(ParticleTypes.ELECTRIC_SPARK, v.x, v.y, v.z, 2, 0.1, 0.1, 0.1, 0);
                        }
                        int dmg = rnd().nextInt(TobRules.VERZIK_P1_BLAST_MAX + 1);
                        if (RaidEffects.protectedFrom(p, AttackType.MAGIC)) {
                            dmg /= 2;
                        }
                        hurt(verzik, p, dmg, null, true);
                    } else {
                        Msg.action(p, "The pillar shields you from the blast.");
                        int pillar = shelter(p);
                        if (pillar >= 0) {
                            struck.add(pillar);
                        }
                    }
                }
                for (int i : struck) {
                    if (pillarBlasts[i] < 0) {
                        continue;
                    }
                    if (hard()) {
                        fallingDebris(i);
                    }
                    if (++pillarBlasts[i] >= RaidScaling.VERZIK_PILLAR_BLASTS) {
                        pillarLost = true;
                        warn("A supporting pillar crumbles under Verzik's blasts!");
                        collapse(i);
                    }
                }
            });
        }

        // ---------------- phase 2

        private void phaseTwo(List<ServerPlayer> players) {
            tickUrnPools(players);
            tickAthanatos();
            if (summoning > 0) {
                summoning--;
                return;
            }
            if (!p25 && raid.healthFraction(verzik) < TobRules.VERZIK_P25_AT) {
                p25 = true;
                warn("Verzik summons Nylocas Matomenos - attacking her now only heals her!");
                summonMatomenos();
                return;
            }
            if (p25 && --matomenosTimer <= 0) {
                absorbMatomenos();
                summonMatomenos();
                return;
            }
            if (players.isEmpty() || t % TobRules.VERZIK_P2_SPEED != 0) {
                return;
            }
            attacks++;
            if (attacks % TobRules.VERZIK_LIGHTNING_EVERY == 0) {
                lightning(players);
                return;
            }
            if (p25 && rnd().nextDouble() < TobRules.VERZIK_HEAL_SPELL_CHANCE) {
                healingSpell(players.get(rnd().nextInt(players.size())));
                return;
            }
            java.util.function.BooleanSupplier current = current();
            DataBossEntity source = verzik;
            for (ServerPlayer p : players) {
                double d = p.distanceToSqr(verzik);
                if (d < 1.2 * 1.2) {
                    // Stomp: anyone underneath, always lands; Protect from Melee doesn't help. // playability: OSRS lands it on
                    // the attack tick; here after her wind-up, on a player still under her
                    verzik.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    com.runecraft.boss.Mechanics.deliverMelee(source, p, 0.0, () -> {
                        if (alive(source) && current.getAsBoolean()) {
                            hurt(source, p, rnd().nextInt(TobRules.VERZIK_STOMP_MAX + 1), null, true);
                            knockBack(p, 3);
                            bounced = true;
                            Msg.red(p, "Verzik Vitur: There's nothing for you there!");
                        }
                    });
                    return;
                }
            }
            for (ServerPlayer p : players) {
                if (p.distanceToSqr(verzik) < 2.8 * 2.8 && rnd().nextInt(4) != 0) {
                    // The body slam ("bounce"): up to 50, rolled against crush; knocked back and stunned (wiki infobox).
                    // // playability: OSRS lands it on the attack tick; here after her wind-up, on a player still in reach
                    verzik.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    com.runecraft.boss.Mechanics.deliverMelee(source, p, com.runecraft.osrs.ThreatTiming.MELEE_REACH, () -> {
                        if (alive(source) && current.getAsBoolean()) {
                            hurt(source, p, rnd().nextInt(TobRules.VERZIK_SLAM_MAX + 1), null, true);
                            knockBack(p, 3);
                            com.runecraft.content.systems.SysCombat.freeze(p, 5);
                            bounced = true;
                            slams++;
                            Msg.red(p, "Verzik body slams you!");
                        }
                    });
                    return;
                }
            }
            // Urnbombs at every player's spot, landing a few ticks later (Hard mode: harder, and a poison pool stays). Each is a
            // raid strike outlined from the throw. // playability: it was a flame burst on the throw only
            int urnMax = TobRules.verzikUrnMax(raid.mode());
            for (ServerPlayer p : players) {
                Vec3 at = p.position();
                RaidEffects.mark(world(), at, 0.8).delay(3).source(source).warn(ParticleTypes.FLAME).live(current)
                        .onHit(q -> {
                            int dmg = 1 + rnd().nextInt(urnMax);
                            if (RaidEffects.protectedFrom(q, AttackType.RANGED)) {
                                dmg /= 2;
                            }
                            hurt(source, q, dmg, null, true);
                        })
                        .onLand(() -> {
                            urnsLanded++;
                            if (hard()) {
                                urnPools.put(at, t + TobRules.VERZIK_HARD_POOL_TICKS);
                                urnPoolsMade++;
                            }
                        }).start();
            }
        }

        /** Hard mode urnbomb pools: 9-10 a tick and poison for anyone standing in one, for 16 ticks. */
        private void tickUrnPools(List<ServerPlayer> players) {
            urnPools.entrySet().removeIf(e -> e.getValue() < t);
            for (Vec3 pool : urnPools.keySet()) {
                if (t % 2 == 0) {
                    world().sendParticles(ParticleTypes.ITEM_SLIME, pool.x, pool.y + 0.1, pool.z, 3, 0.4, 0.02, 0.4, 0);
                }
            }
            for (ServerPlayer p : players) {
                for (Vec3 pool : urnPools.keySet()) {
                    if (RaidEffects.horizontalDistSq(p.position(), pool) < 1.0) {
                        hurtRaw(verzik, p, TobRules.VERZIK_HARD_POOL_MIN
                                + rnd().nextInt(TobRules.VERZIK_HARD_POOL_MAX - TobRules.VERZIK_HARD_POOL_MIN + 1));
                        com.runecraft.combat.PoisonService.poison(p, 6, ActionManager.gameTick());
                        break;
                    }
                }
            }
        }

        private void knockBack(ServerPlayer p, double strength) {
            Vec3 push = p.position().subtract(verzik.position()).multiply(1, 0, 1);
            if (push.lengthSqr() < 1e-4) {
                push = new Vec3(1, 0, 0);
            }
            push = push.normalize().scale(strength * 0.4);
            p.push(push.x, 0.4, push.z);
            p.hurtMarked = true;
        }

        /**
         * The lightning ball (OSRS wiki, Verzik Vitur and the ToB strategies): it flies at a random player and chains between
         * players in a random sequence. On each player it reaches it jumps on to a random teammate in range; if Verzik stands in
         * that path it arcs through her (15-20 to her, no accuracy roll) and is spent; the fourth player it reaches, or one with
         * no teammate near, is struck for up to 48 (25 in insulated boots); each player it chains through takes a small hit
         * ({@link TobRules#VERZIK_LIGHTNING_CHAIN_MAX}). Round 43: a real ball passed between the players,
         * each jump decided from where everyone stands as it arrives (it was a coin flip at launch). // playability: OSRS
         * resolves the chain as it fires, flying about a tick between players; here three game ticks to the first player and two
         * per jump, drawn all the way, each player it jumps to warned mid-screen
         */
        private void lightning(List<ServerPlayer> players) {
            lightnings++;
            ServerPlayer at = players.get(rnd().nextInt(players.size()));
            say("Verzik fires a ball of lightning!");
            Msg.telegraph(at, "Verzik's lightning ball is coming at you - keep her between you and a teammate!", null);
            lightningHop(at, verzik.getEyePosition(), 1, current(), RaidEffects.now());
            // OSRS: after every third lightning ball, a 25% chance of the nylocas being summoned again.
            if (lightnings % 3 == 0 && !alive(athanatos) && !p25 && rnd().nextDouble() < TobRules.VERZIK_RESUMMON_CHANCE) {
                summonNylocas(false);
            }
        }

        /** The ball flying from {@code from} to {@code at}, the {@code reached}th player it reaches; what it does there. */
        private void lightningHop(ServerPlayer at, Vec3 from, int reached, java.util.function.BooleanSupplier current, long launched) {
            DataBossEntity source = verzik;
            RaidEffects.projectile(world(), from, at, null, reached == 1 ? LIGHTNING_TICKS : LIGHTNING_HOP_TICKS,
                    () -> alive(source) && current.getAsBoolean(), () -> {
                        if (reached == 1) {
                            lastLightningFlight = RaidEffects.now() - launched;
                        }
                        List<ServerPlayer> here = players();
                        int holder = here.indexOf(at);
                        if (holder < 0) {
                            return; // its player left the room: it fizzles
                        }
                        double[][] xz = new double[here.size()][];
                        for (int i = 0; i < here.size(); i++) {
                            xz[i] = new double[]{here.get(i).getX(), here.get(i).getZ()};
                        }
                        com.runecraft.osrs.content.raids.RaidRealtime.Bounce b = com.runecraft.osrs.content.raids.RaidRealtime.lightningBounce(
                                reached, holder, xz, source.getX(), source.getZ(), rnd().nextInt(1 << 16));
                        lightningReached = Math.max(lightningReached, reached);
                        if (b.chain() != com.runecraft.osrs.content.raids.RaidRealtime.Chain.STRIKE) {
                            // OSRS wiki: it chains between players "hitting them for a small amount of damage"; a ball passed
                            // on correctly doesn't fail "Perfect Verzik" (only the big zap does), so no hurt() here
                            lightningChainHits++;
                            RaidEffects.hit(source, at, TobRules.VERZIK_LIGHTNING_CHAIN_MIN + rnd().nextInt(
                                    TobRules.VERZIK_LIGHTNING_CHAIN_MAX - TobRules.VERZIK_LIGHTNING_CHAIN_MIN + 1), null, true);
                        }
                        switch (b.chain()) {
                            case THROUGH_VERZIK -> {
                                // the arc from its player through her, then spent
                                RaidEffects.drawLane(world(), at.position().add(0, 1, 0), source.position().add(0, 1.5, 0), 0.3,
                                        ParticleTypes.ELECTRIC_SPARK, ParticleTypes.ELECTRIC_SPARK, 0);
                                world().sendParticles(ParticleTypes.ELECTRIC_SPARK, source.getX(), source.getY() + 2, source.getZ(), 30, 0.6, 1, 0.6, 0.1);
                                com.runecraft.combat.CombatHandler.applyDamage(null, source, TobRules.VERZIK_LIGHTNING_SELF_MIN
                                        + rnd().nextInt(TobRules.VERZIK_LIGHTNING_SELF_MAX - TobRules.VERZIK_LIGHTNING_SELF_MIN + 1));
                                lightningThrough++;
                                say("The lightning arcs through Verzik!");
                            }
                            case NEXT -> {
                                ServerPlayer next = here.get(b.next());
                                Msg.telegraph(next, "The lightning ball jumps to you (" + (reached + 1) + "/"
                                        + com.runecraft.osrs.content.raids.RaidRealtime.VERZIK_LIGHTNING_PLAYERS
                                        + ") - keep Verzik between you and a teammate!", null);
                                lightningHop(next, at.position().add(0, 1, 0), reached + 1, current, launched);
                            }
                            default -> {
                                boolean insulated = "insulated_boots".equals(Inv.idOf(at.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET)));
                                lastLightningMax = insulated ? TobRules.VERZIK_LIGHTNING_INSULATED_MAX : TobRules.VERZIK_LIGHTNING_MAX;
                                lightningStrikes++;
                                hurt(source, at, rnd().nextInt(lastLightningMax + 1), null, true);
                            }
                        }
                    });
        }

        /** Game ticks the lightning ball flies to its first player (and its last such flight, self-test). */
        static final int LIGHTNING_TICKS = com.runecraft.osrs.ArenaTiming.STRIKE_FLOOR_TICKS;
        /** Game ticks the lightning ball flies on each jump between players (the shortest raid projectile flight). */
        static final int LIGHTNING_HOP_TICKS = com.runecraft.osrs.content.raids.RaidRealtime.PROJECTILE_MIN_TICKS;
        long lastLightningFlight = -1;
        /** Self-test: the most players one ball reached, balls that arced through Verzik, and players struck. */
        int lightningReached;
        int lightningThrough;
        int lightningStrikes;
        /** Self-test: the small hits on the players the ball chained through. */
        int lightningChainHits;

        private void healingSpell(ServerPlayer target) {
            int total = 0;
            for (ServerPlayer p : players()) {
                if (p.distanceToSqr(target) <= 1.5 * 1.5) {
                    int dmg = rnd().nextInt(TobRules.VERZIK_HEAL_SPELL_MAX + 1);
                    if (RaidEffects.protectedFrom(p, AttackType.MAGIC)) {
                        dmg /= 2;
                    }
                    total += dmg;
                    hurt(verzik, p, dmg, null, true);
                }
            }
            raid.heal(verzik, total / 2);
            world().sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + 1, target.getZ(), 15, 0.8, 0.5, 0.8, 0);
        }

        /** Phase 2.5: two Nylocas Matomenos at her sides (one when only one player is left alive); damage heals her meanwhile. */
        private void summonMatomenos() {
            summoning = TobRules.VERZIK_SUMMON_TICKS;
            matomenosTimer = TobRules.VERZIK_MATOMENOS_INTERVAL;
            int n = TobRules.verzikMatomenos(raid.activePlayers().size());
            for (int i = 0; i < n; i++) {
                int side = i == 0 ? -1 : 1;
                Mob m = spawnMob("nylocas_matomenos", BlockPos.containing(verzik.position()).offset(0, 0, side * 3), false);
                if (m != null) {
                    raid.setHitpoints(m, RaidScaling.tobScaledHp("nylocas_matomenos", TobRules.VERZIK_MATOMENOS_HP, raid.partySize(),
                            raid.mode()));
                    m.setNoAi(true);
                    matomenos.add(m);
                }
            }
        }

        /**
         * The Matomenos still alive when she summons the next pair: she kills them off and heals by their remaining
         * hitpoints; in Hard mode they explode on players next to them.
         */
        private void absorbMatomenos() {
            for (Mob m : matomenos) {
                if (!alive(m)) {
                    continue;
                }
                int hp = raid.hitpoints(m);
                raid.heal(verzik, hp);
                matomenosAbsorbed++;
                matomenosHealed += hp;
                world().sendParticles(ParticleTypes.HEART, verzik.getX(), verzik.getY() + 2, verzik.getZ(), 4, 0.5, 0.3, 0.5, 0);
                if (hard()) {
                    world().sendParticles(ParticleTypes.EXPLOSION, m.getX(), m.getY() + 0.5, m.getZ(), 2, 0.4, 0.4, 0.4, 0);
                    for (ServerPlayer p : players()) {
                        if (p.distanceToSqr(m) < 3.5 * 3.5) {
                            hurt(verzik, p, TobRules.verzikMatomenosBlast(hp), null, true);
                        }
                    }
                }
                m.discard();
            }
            matomenos.clear();
            say("Verzik absorbs her Nylocas Matomenos!");
        }

        /**
         * Combat nylocas that follow a player and explode on reaching them (up to 63, up to 8 within three tiles); phase 2's
         * first group comes with a Nylocas Athanatos thrown at a random player.
         */
        private void summonNylocas(boolean athanatosToo) {
            List<ServerPlayer> players = players();
            for (int i = 0; i < Math.max(2, players.size() * 2); i++) {
                // Hard mode (OSRS wiki): they come green (Toxobolos) and turn blue, then grey.
                int breed = hard() ? TobNylocas.Breed.TOXOBOLOS.ordinal() : rnd().nextInt(BREEDS.length);
                Mob m = spawnMob(BREEDS[breed].creatureId, BlockPos.containing(verzik.position()).offset(rnd().nextInt(5) - 2, 0,
                        rnd().nextInt(5) - 2), false);
                if (m != null) {
                    m.setTarget(null);
                    crabs.add(new Crab(m, players.isEmpty() ? null : players.get(rnd().nextInt(players.size())), breed));
                }
            }
            say("Verzik summons explosive Nylocas!");
            if (athanatosToo && !alive(athanatos)) {
                throwAthanatos();
            }
        }

        /** The Athanatos is thrown from Verzik at a random player: up to 78 on the tile it lands on, half next to it. */
        private void throwAthanatos() {
            ServerPlayer target = randomPlayer();
            Vec3 at = target == null ? vec(-10, rnd().nextBoolean() ? -10 : 10) : target.position();
            world().sendParticles(ParticleTypes.WITCH, at.x, at.y + 0.2, at.z, 20, 0.6, 0.05, 0.6, 0);
            warn("Verzik hurls a purple projectile! A Nylocas Athanatos will land there - poison it!");
            later(TobRules.ATHANATOS_LANDING_DELAY, () -> {
                if (!alive(verzik) || form != 2) {
                    return;
                }
                athanatosLandings++;
                world().sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, 2, 0.3, 0.3, 0.3, 0);
                for (ServerPlayer p : players()) {
                    double d2 = RaidEffects.horizontalDistSq(p.position(), at);
                    if (d2 < 1.0) {
                        hurt(verzik, p, rnd().nextInt(TobRules.ATHANATOS_LANDING_MAX + 1), null, true);
                    } else if (d2 < 2.6 * 2.6) {
                        hurt(verzik, p, rnd().nextInt(TobRules.ATHANATOS_LANDING_MAX / 2 + 1), null, true);
                    }
                }
                athanatos = spawnMob("nylocas_athanatos", BlockPos.containing(at), false);
                athanatosRespawn = -1;
            });
        }

        /**
         * The Athanatos heals her every few ticks and regenerates; a poison hit (or poison from a Smoke spell) bursts it on
         * her for up to 70. Killed by raw damage it comes back.
         */
        private void tickAthanatos() {
            if (athanatos != null && !alive(athanatos)) {
                athanatos = null;
                athanatosRespawn = t + TobRules.ATHANATOS_RESPAWN_TICKS;
                say("The Nylocas Athanatos dies without its poison... and Verzik prepares another.");
            }
            if (athanatos == null && athanatosRespawn >= 0 && t >= athanatosRespawn && !p25) {
                athanatosRespawn = -1;
                athanatosRespawns++;
                throwAthanatos();
            }
            if (!alive(athanatos)) {
                return;
            }
            if (com.runecraft.combat.PoisonService.isPoisoned(athanatos) || com.runecraft.combat.PoisonService.isEnvenomed(athanatos)) {
                burstAthanatos(null, true);
                return;
            }
            if (t % 4 == 0) {
                raid.heal(verzik, TobRules.ATHANATOS_HEAL_MIN + rnd().nextInt(TobRules.ATHANATOS_HEAL_MAX - TobRules.ATHANATOS_HEAL_MIN + 1));
                world().sendParticles(ParticleTypes.HEART, verzik.getX(), verzik.getY() + 2, verzik.getZ(), 2, 0.4, 0.3, 0.4, 0);
            }
            raid.heal(athanatos, TobRules.ATHANATOS_REGEN);
            TobRooms.walk(athanatos, verzik.position());
        }

        /**
         * The Athanatos absorbs poison and bursts against Verzik for up to 70. {@code discard}: remove it now (otherwise the
         * hit that burst it kills it); either way it doesn't come back.
         */
        private void burstAthanatos(ServerPlayer attacker, boolean discard) {
            athanatosBursts++;
            if (alive(verzik)) {
                com.runecraft.combat.CombatHandler.applyDamage(attacker, verzik, rnd().nextInt(TobRules.ATHANATOS_POISON_MAX + 1));
            }
            say("The Nylocas Athanatos absorbs the poison and bursts against Verzik!");
            if (athanatos != null) {
                world().sendParticles(ParticleTypes.ITEM_SLIME, athanatos.getX(), athanatos.getY() + 0.5, athanatos.getZ(), 20, 0.5, 0.5, 0.5, 0);
                if (discard) {
                    athanatos.discard();
                }
            }
            athanatos = null;
            athanatosRespawn = -1;
        }

        /** Moves the combat nylocas after their players, cycles their style (Hard mode) and pops them. */
        private void tickCrabs(List<ServerPlayer> players) {
            for (Crab c : new ArrayList<>(crabs)) {
                if (!alive(c.mob)) {
                    crabs.remove(c);
                    continue;
                }
                c.age++;
                if (c.target == null || !c.target.isAlive() || raid.isDowned(c.target) || !inside(c.target.position())) {
                    c.target = players.isEmpty() ? null : players.get(rnd().nextInt(players.size()));
                }
                if (c.target != null) {
                    TobRooms.walk(c.mob, c.target.position());
                }
                if (hard()) {
                    // Hard mode (OSRS wiki): they never time out and change colour green, blue, grey (ranged, magic, melee).
                    if (c.age % TobRules.VERZIK_HARD_NYLO_CYCLE_TICKS == 0) {
                        cycle(c);
                    }
                } else if (c.age > TobRules.VERZIK_NYLO_TICKS) {
                    // "If they have not exploded after ~15 seconds of movement, they will die."
                    world().sendParticles(ParticleTypes.POOF, c.mob.getX(), c.mob.getY() + 0.5, c.mob.getZ(), 6, 0.3, 0.3, 0.3, 0);
                    c.mob.discard();
                    crabs.remove(c);
                    continue;
                }
                if (c.target != null && RaidEffects.horizontalDistSq(c.mob.position(), c.target.position()) < 1.6 * 1.6) {
                    pop(c);
                }
            }
        }

        /** A Hard mode nylocas changes to the next style in the cycle, keeping its health and target. */
        private void cycle(Crab c) {
            int hp = raid.hitpoints(c.mob);
            int next = (c.breed + 1) % BREEDS.length;
            Mob m = spawnMob(BREEDS[next].creatureId, c.mob.blockPosition(), false);
            if (m == null) {
                return;
            }
            raid.setCurrentHitpoints(m, hp);
            m.setTarget(null);
            c.mob.discard();
            c.mob = m;
            c.breed = next;
            crabsCycled++;
        }

        /** A nylocas reached its player: up to 63 next to it, up to 8 within three tiles. */
        private void pop(Crab c) {
            crabsPopped++;
            nyloPopped = true; // "Nylo Sniper": nobody may let one reach them
            Vec3 at = c.mob.position();
            world().sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, 2, 0.4, 0.4, 0.4, 0);
            for (ServerPlayer p : players()) {
                double d2 = RaidEffects.horizontalDistSq(p.position(), at);
                int dealt = 0;
                if (d2 < 1.6 * 1.6) {
                    dealt = hurt(verzik, p, rnd().nextInt(TobRules.VERZIK_NYLO_MAX + 1), null, true);
                } else if (d2 < 6.5 * 6.5) {
                    dealt = hurt(verzik, p, rnd().nextInt(TobRules.VERZIK_NYLO_FAR + 1), null, true);
                }
                nyloDamage |= dealt > 0;
            }
            c.mob.discard();
            crabs.remove(c);
        }

        /**
         * Whether this attack can poison or venom (OSRS wiki "Nylocas Athanatos"): a poisoned or venomous weapon (blowpipe,
         * toxic staff, trident of the swamp, weapons ending (p) / (p+) / (p++) or coated with weapon poison), poisoned arrows,
         * bolts or ammunition for a ranged attack, or any attack while wearing a serpentine helm. A Smoke spell bursts it
         * only through the poison it inflicts (1 in 8 hits, {@link #athanatosHit}), whether cast by hand or autocast.
         */
        static boolean poisonous(ServerPlayer p, AttackType type) {
            String head = Inv.idOf(p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD));
            if (head.endsWith("serpentine_helm") || head.endsWith("tanzanite_helm") || head.endsWith("magma_helm")) {
                return true;
            }
            if (type == AttackType.MAGIC) {
                String w = Inv.idOf(p.getMainHandItem());
                return w.startsWith("trident_of_the_swamp") || w.contains("toxic");
            }
            ItemStack weapon = p.getMainHandItem();
            if (poisonedItem(weapon)) {
                return true;
            }
            if (type == AttackType.RANGED) {
                // The ammunition the shot uses: the worn ammo slot, or the first arrow / bolt a bow would fire.
                if (poisonedItem(com.runecraft.content.systems.SysState.of(p).worn(com.runecraft.osrs.content.systems.Jewellery.Slot.AMMO))) {
                    return true;
                }
                for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                    ItemStack st = p.getInventory().getItem(i);
                    com.runecraft.osrs.ItemDef def = com.runecraft.registry.ModItems.def(st);
                    if (def != null && def.kind() == com.runecraft.osrs.ItemDef.Kind.ARROW) {
                        return poisonedItem(st);
                    }
                }
            }
            return false;
        }

        private static boolean poisonedItem(ItemStack stack) {
            return poisonedId(Inv.idOf(stack)) || com.runecraft.util.StackData.contains(stack, com.runecraft.content.skilling.ItemHooks.POISON_NBT);
        }

        private static boolean poisonedId(String w) {
            return w != null && (w.contains("blowpipe") || w.contains("toxic") || w.contains("serpentine") || w.contains("venom")
                    || w.endsWith("_p") || w.endsWith("_pp") || w.endsWith("_kp") || w.contains("_p_") || w.contains("(p"));
        }

        /**
         * The Athanatos rule, called from the hit filter registered in {@link TobRooms#init}: a poison or venom hit bursts
         * it; anything else barely hurts it (a quarter of the damage).
         */
        int athanatosHit(ServerPlayer attacker, LivingEntity target, AttackType type, int damage) {
            boolean poison = poisonous(attacker, type);
            if (!poison && type == AttackType.MAGIC && damage > 0
                    && TobRules.smokeSpell(com.runecraft.content.systems.SpellService.castingSpellId())) {
                // A Smoke spell (cast by hand or autocast) kills it through the poison it inflicts: 1 in 8 successful hits.
                poison = rnd().nextInt(TobRules.SMOKE_POISON_CHANCE) == 0;
            }
            if (!poison) {
                int raw = TobRules.athanatosRawDamage(damage);
                if (damage > 0) {
                    Msg.action(attacker, "The Nylocas Athanatos barely notices. Only poison will truly harm it.");
                }
                return raw;
            }
            if (damage <= 0) {
                return 0; // a miss can't poison it
            }
            int hp = raid.hitpoints(target);
            if (target == athanatos) {
                burstAthanatos(attacker, false);
            }
            return Math.max(damage, hp);
        }

        // ---------------- phase 3

        private void phaseThree(List<ServerPlayer> players) {
            if (!enraged && raid.healthFraction(verzik) < TobRules.VERZIK_ENRAGE_AT) {
                enraged = true;
                warn("Verzik Vitur: I'm not finished with you just yet!");
                for (ServerPlayer p : players) {
                    tornadoes.put(p.getUUID(), verzik.position());
                }
            }
            if (hard() && !lastStand && raid.healthFraction(verzik) <= TobRules.VERZIK_HARD_LAST_STAND_AT) {
                // Hard mode (OSRS wiki): a one-time recovery of 30% of her hitpoints.
                lastStand = true;
                raid.heal(verzik, (int) Math.round(raid.maxHitpoints(verzik) * TobRules.VERZIK_HARD_LAST_STAND_HEAL));
                warn("Verzik Vitur: You think this is over?");
            }
            tickWebs(players);
            tickTornadoes(players);
            if (players.isEmpty() || invulnerable > 0) {
                return;
            }
            int speed = enraged ? TobRules.VERZIK_P3_ENRAGED_SPEED : TobRules.VERZIK_P3_SPEED;
            if (t % speed != 0) {
                return;
            }
            attacks++;
            if (attacks % (TobRules.VERZIK_P3_ATTACKS_PER_SPECIAL + 1) == 0) {
                TobRules.P3Special s = TobRules.P3_ROTATION.get(specials++ % TobRules.P3_ROTATION.size());
                switch (s) {
                    case NYLOCAS -> summonNylocas(false);
                    case WEBS -> webs(players);
                    case YELLOWS -> yellows();
                    default -> greenBall(players.get(rnd().nextInt(players.size())));
                }
                return;
            }
            boolean melee = false;
            for (ServerPlayer p : players) {
                if (p.distanceToSqr(verzik) < 2.5 * 2.5 && rnd().nextBoolean()) {
                    melee = true;
                }
            }
            if (melee) {
                for (ServerPlayer p : players) {
                    if (p.distanceToSqr(verzik) < 2.5 * 2.5) {
                        hurt(verzik, p, rnd().nextInt(TobRules.VERZIK_P3_MELEE_MAX + 1), null, true);
                    }
                }
                return;
            }
            AttackType style = rnd().nextBoolean() ? AttackType.RANGED : AttackType.MAGIC;
            int max = enraged ? TobRules.VERZIK_ENRAGED_PROJECTILE_MAX : TobRules.VERZIK_P3_PROJECTILE_MAX;
            for (ServerPlayer p : players) {
                projectile(p, style, max);
            }
        }

        /** Game ticks her phase 3 projectiles fly. // playability: OSRS rolls them as she attacks */
        static final int P3_PROJECTILE_TICKS = com.runecraft.osrs.ArenaTiming.STRIKE_FLOOR_TICKS;

        /**
         * One phase 3 ranged or magic auto: its style named as it leaves her and drawn as a style-coloured trail on its way,
         * landing {@link #P3_PROJECTILE_TICKS} ticks later, the protection prayer read as it lands (up to 33, 34 enraged;
         * halved by the right prayer). // playability: in OSRS the style is only seen on the projectile and the prayer must
         * be right as she attacks, a switch a tick on a 7-tick (enraged 5) cycle
         */
        private void projectile(ServerPlayer p, AttackType style, int max) {
            boolean magic = style == AttackType.MAGIC;
            Msg.telegraph(p, magic ? "Verzik fires a magic orb!" : "Verzik fires a ranged bolt!", null);
            int rgb = magic ? Msg.MAGIC_RGB : Msg.RANGED_RGB;
            net.minecraft.core.particles.DustParticleOptions dust = new net.minecraft.core.particles.DustParticleOptions(
                    new org.joml.Vector3f(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f), 1.6f);
            Vec3 from = verzik.position().add(0, verzik.getBbHeight() * 0.7, 0);
            long launchMc = RaidEffects.clockMc();
            double flightMc = P3_PROJECTILE_TICKS * (double) ActionManager.MC_TICKS_PER_GAME_TICK;
            RaidEffects.cue(P3_PROJECTILE_TICKS, () -> {
                if (!alive(verzik) || !p.isAlive()) {
                    return false;
                }
                double f = Math.min(1.0, (RaidEffects.clockMc() - launchMc) / flightMc);
                Vec3 to = p.position().add(0, 1.0, 0);
                for (int k = 0; k < 3; k++) {
                    double t = Math.max(0, f - k * 0.08);
                    Vec3 at = from.lerp(to, t);
                    world().sendParticles(dust, at.x, at.y, at.z, 2, 0.05, 0.05, 0.05, 0.0);
                }
                return true;
            });
            later(P3_PROJECTILE_TICKS, () -> {
                if (!alive(verzik) || !p.isAlive() || !players().contains(p)) {
                    return;
                }
                int dmg = rnd().nextInt(max + 1);
                boolean prayed = RaidEffects.protectedFrom(p, style);
                if (prayed) {
                    dmg /= 2;
                }
                // "Perfect Verzik": only her spider form's correctly prayed projectiles may land.
                if (RaidEffects.hit(verzik, p, dmg, null, true) > 0 && !prayed) {
                    fail();
                }
            });
        }

        /** Sticky webs: she moves to the middle (invulnerable) and throws webs at the players; a web left too long snaps. */
        private void webs(List<ServerPlayer> players) {
            say("Verzik spins sticky webs!");
            invulnerable = 3;
            move(verzik, vec(0, 0));
            for (ServerPlayer p : players) {
                for (int i = 0; i < 3; i++) {
                    BlockPos w = p.blockPosition().offset(rnd().nextInt(5) - 2, 0, rnd().nextInt(5) - 2);
                    if (world().getBlockState(w).isAir() && inside(Vec3.atBottomCenterOf(w))) {
                        RaidBuilder.set(world(), w, Blocks.COBWEB.defaultBlockState());
                        webs.put(w, t + TobRules.WEB_SNAP_TICKS);
                    }
                }
            }
        }

        private void tickWebs(List<ServerPlayer> players) {
            for (java.util.Iterator<Map.Entry<BlockPos, Integer>> it = webs.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<BlockPos, Integer> e = it.next();
                if (!world().getBlockState(e.getKey()).is(Blocks.COBWEB)) {
                    it.remove();
                    continue;
                }
                if (t >= e.getValue()) {
                    for (ServerPlayer p : players) {
                        if (p.blockPosition().equals(e.getKey())) {
                            // Aranea boots negate a snapping web once every 8 ticks (OSRS wiki).
                            boolean aranea = "aranea_boots".equals(Inv.idOf(p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET)));
                            if (aranea && araneaReady.getOrDefault(p.getUUID(), 0) <= t) {
                                araneaReady.put(p.getUUID(), t + TobRules.WEB_ARANEA_COOLDOWN);
                                Msg.game(p, "Your aranea boots let you slip out of the web unharmed.");
                                continue;
                            }
                            hurt(verzik, p, TobRules.WEB_SNAP_MIN + rnd().nextInt(TobRules.WEB_SNAP_MAX - TobRules.WEB_SNAP_MIN + 1), null, true);
                            Msg.red(p, "The web snaps around you!");
                        }
                    }
                    RaidBuilder.set(world(), e.getKey(), Blocks.AIR.defaultBlockState());
                    it.remove();
                }
            }
        }

        /**
         * Power blast: one pool per player; anyone not alone on a pool takes up to 80. Verzik can't be hurt meanwhile. In
         * Hard mode she strikes three times, with fresh pools each time.
         */
        private void yellows() {
            int volleys = TobRules.yellowVolleys(raid.mode());
            warn("Verzik charges a powerful blast! Stand alone on a yellow pool!");
            invulnerable = 7 + (volleys - 1) * TobRules.YELLOW_VOLLEY_GAP;
            yellowVolley(volleys, 0);
        }

        private void yellowVolley(int volleys, int k) {
            yellows.clear();
            for (ServerPlayer p : players()) {
                yellows.add(p.position().add(rnd().nextInt(7) - 3, 0, rnd().nextInt(7) - 3));
            }
            List<Vec3> pools = new ArrayList<>(yellows);
            int delay = k == 0 ? 6 : TobRules.YELLOW_VOLLEY_GAP;
            for (int i = 0; i < delay; i++) {
                later(i + 1, () -> {
                    for (Vec3 v : pools) {
                        world().sendParticles(ParticleTypes.WAX_ON, v.x, v.y + 0.1, v.z, 8, 0.4, 0.05, 0.4, 0);
                    }
                });
            }
            later(delay, () -> {
                if (!alive(verzik)) {
                    return;
                }
                Map<Vec3, Integer> occupancy = new HashMap<>();
                Map<ServerPlayer, Vec3> on = new HashMap<>();
                List<ServerPlayer> here = players();
                for (ServerPlayer p : here) {
                    for (Vec3 v : pools) {
                        if (RaidEffects.horizontalDistSq(p.position(), v) < 1.0) {
                            occupancy.merge(v, 1, Integer::sum);
                            on.put(p, v);
                            break;
                        }
                    }
                }
                if (k == 0) {
                    yellowHit = false;
                }
                for (ServerPlayer p : here) {
                    Vec3 v = on.get(p);
                    if (v == null || occupancy.get(v) > 1) {
                        yellowHit |= hurt(verzik, p, rnd().nextInt(TobRules.VERZIK_YELLOW_MAX + 1), null, true) > 0;
                    }
                }
                yellows.clear();
                if (k + 1 < volleys) {
                    yellowVolley(volleys, k + 1);
                } else if (enraged && hard() && here.size() >= 2 && !yellowHit) {
                    teamwork = true; // "Team Work Makes the Dream Work": a whole power blast without damage during the tornadoes
                }
            });
        }

        /**
         * The green ball: 75% of the Hitpoints level, or bounced on to a neighbour. Hard mode: unbounced it kills while two
         * or more players are in the arena; bounced it deals 89 to the next player. // approx: one bounce ends it
         */
        private void greenBall(ServerPlayer target) {
            Msg.red(target, "Verzik Vitur fires a powerful projectile in your direction...");
            later(4, () -> {
                if (!target.isAlive() || !alive(verzik)) {
                    return;
                }
                ServerPlayer buddy = null;
                for (ServerPlayer p : players()) {
                    if (p != target && p.distanceToSqr(target) < 1.5 * 1.5) {
                        buddy = p;
                        break;
                    }
                }
                world().sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + 1, target.getZ(), 20, 0.5, 0.5, 0.5, 0);
                int level = Players.data(target).skills().level(Skill.HITPOINTS);
                if (buddy != null) {
                    greenPassed = true;
                    say("The green ball bounces to " + buddy.getName().getString() + (hard() ? "!" : " and fizzles out."));
                    if (hard()) {
                        hurtRaw(verzik, buddy, TobRules.greenBallHard(level, 1, players().size(), Math.round(buddy.getHealth())));
                    }
                } else if (hard()) {
                    hurtRaw(verzik, target, TobRules.greenBallHard(level, 0, players().size(), Math.round(target.getHealth())));
                } else {
                    hurt(verzik, target, TobRules.greenBall(level), null, true);
                }
            });
        }

        /** Enrage: a purple tornado per player creeps after them; contact takes half their hitpoints (at least 5) and heals her 3x. */
        private void tickTornadoes(List<ServerPlayer> players) {
            if (!enraged) {
                return;
            }
            for (ServerPlayer p : players) {
                UUID u = p.getUUID();
                Integer respawn = tornadoRespawn.get(u);
                if (respawn != null) {
                    if (t >= respawn) {
                        tornadoRespawn.remove(u);
                        tornadoes.put(u, verzik.position());
                    }
                    continue;
                }
                Vec3 at = tornadoes.get(u);
                if (at == null) {
                    tornadoes.put(u, verzik.position());
                    continue;
                }
                Vec3 to = p.position().subtract(at);
                Vec3 step = to.lengthSqr() > 0.36 ? to.normalize().scale(0.6) : to;
                at = at.add(step.x, 0, step.z);
                tornadoes.put(u, at);
                world().sendParticles(ParticleTypes.WITCH, at.x, at.y + 0.5, at.z, 6, 0.3, 0.6, 0.3, 0);
                if (RaidEffects.horizontalDistSq(at, p.position()) < 0.7 * 0.7) {
                    int dealt = hurtRaw(verzik, p, TobRules.tornadoHit(Math.round(p.getHealth())));
                    raid.heal(verzik, dealt * 3);
                    tornadoes.remove(u);
                    tornadoRespawn.put(u, t + TobRules.TORNADO_RESPAWN_TICKS);
                    Msg.red(p, "The purple tornado tears into you!");
                }
            }
        }

        @Override
        public void cleanup() {
            super.cleanup();
            if (alive(verzik)) {
                verzik.discard();
            }
            if (alive(athanatos)) {
                athanatos.discard();
            }
            // A Dawnbringer left on the floor goes with the room (the shutdown sweep only finds entities it can see).
            dropped.forEach(ItemEntity::discard);
            dropped.clear();
            for (Crab c : crabs) {
                if (c.mob.isAlive()) {
                    c.mob.discard();
                }
            }
            crabs.clear();
            living("nylocas_matomenos").forEach(LivingEntity::discard);
            living("nylocas_athanatos").forEach(LivingEntity::discard);
            for (TobNylocas.Breed b : BREEDS) {
                living(b.creatureId).forEach(LivingEntity::discard);
            }
            for (BlockPos w : webs.keySet()) {
                if (world().getBlockState(w).is(Blocks.COBWEB)) {
                    RaidBuilder.set(world(), w, Blocks.AIR.defaultBlockState());
                }
            }
            webs.clear();
            tornadoes.clear();
            urnPools.clear();
        }

        private static AttackType styleFor(TobNylocas.Breed breed) {
            for (AttackType t : new AttackType[]{AttackType.SLASH, AttackType.RANGED, AttackType.MAGIC}) {
                if (breed.weakTo(t)) {
                    return t;
                }
            }
            return AttackType.SLASH;
        }

        /** Kills the combat nylocas around (each with its own style), letting the first of the fight reach the player. */
        private void dealWithCrabs(CoxTobWalk w) {
            for (Crab c : new ArrayList<>(crabs)) {
                if (!alive(c.mob)) {
                    continue;
                }
                if (crabsPopped == 0 && c.target == w.player()) {
                    // Let it reach the player: it must explode.
                    c.mob.teleportTo(w.player().getX() + 0.5, w.player().getY(), w.player().getZ());
                    w.until(() -> crabsPopped > 0, 3, "a nylocas that reached its player did not explode");
                    continue;
                }
                TobNylocas.Breed b = TobNylocas.Breed.byCreature(RaidInstance.creatureId(c.mob));
                w.kill(c.mob, styleFor(b == null ? TobNylocas.Breed.ISCHYROS : b), "a " + b + " nylocas");
            }
        }

        @Override
        public String walk(CoxTobWalk w) {
            w.expect(form == 1 && alive(verzik), "Verzik did not sit on her throne");
            w.hold(null);
            int capped = w.hit(verzik, 50, AttackType.SLASH);
            w.expect(capped <= 10, "Verzik's phase 1 shield let a melee hit of " + capped + " through (cap 10)");
            int rangedCapped = w.hit(verzik, 50, AttackType.RANGED);
            w.expect(rangedCapped <= 3, "Verzik's phase 1 shield let a ranged hit of " + rangedCapped + " through (cap 3)");
            // Her Defence can't be lowered.
            com.runecraft.content.systems.SysCombat.drainDefence(verzik, 10);
            w.tick(1);
            w.expect(absorbedDrains > 0 && com.runecraft.content.systems.SysCombat.drainedStatLevel(verzik, Skill.DEFENCE)
                    == com.runecraft.content.systems.SysCombat.baseStatLevel(verzik, Skill.DEFENCE), "Verzik's Defence could be drained");
            // The skeleton at the entrance holds the Dawnbringer: one at a time.
            BlockPos bones = w.block(this, RaidsContent.TOB_DAWNBRINGER_SKELETON);
            w.moveTo(bones.offset(1, 0, 0));
            w.use(bones);
            w.use(bones);
            w.expect(w.count(TobBosses.DAWNBRINGER) == 1, "the skeleton gave " + w.count(TobBosses.DAWNBRINGER) + " Dawnbringers (expected one)");
            w.take(TobBosses.DAWNBRINGER);
            w.use(bones);
            w.expect(w.count(TobBosses.DAWNBRINGER) == 1, "a lost Dawnbringer could not be found again");
            // OSRS: the Dawnbringer drops to the floor when its holder dies or leaves, and the skeleton stays empty meanwhile.
            TobRooms.dropDawnbringer(raid, w.player());
            w.expect(w.count(TobBosses.DAWNBRINGER) == 0 && floorDawnbringers() == 1, "the Dawnbringer did not drop to the floor (carried "
                    + w.count(TobBosses.DAWNBRINGER) + ", on the floor " + floorDawnbringers() + ")");
            w.use(bones);
            w.expect(w.count(TobBosses.DAWNBRINGER) == 0, "the skeleton gave a second Dawnbringer while one lay on the floor");
            floorItems().forEach(ItemEntity::discard); // picked up again
            w.hold(TobBosses.DAWNBRINGER);
            // Its range is 7 tiles: from the far end of the hall it can't reach her.
            w.moveTo(at(-RaidBuilder.HALF + 2, 0));
            w.expect(!dawnbringerReaches(w.player(), verzik) && w.hit(verzik, 100, AttackType.MAGIC) == 0,
                    "the Dawnbringer hit Verzik from across the hall (range 7 tiles)");
            w.moveTo(at(4, 0));
            w.expect(dawnbringerReaches(w.player(), verzik), "the Dawnbringer could not reach Verzik from 7 tiles");
            int guard = 0;
            while (form == 1 && guard++ < 300) {
                w.hit(verzik, 100, AttackType.MAGIC);
                w.tick(1);
            }
            w.expect(form == 2, "the Dawnbringer did not break Verzik's phase 1 shield");
            w.expect(w.count(TobBosses.DAWNBRINGER) == 0 && floorDawnbringers() == 0, "the Dawnbringer did not crumble when the shield broke");
            w.hold(null);
            w.moveTo(at(-12, 0));
            // The first group of combat nylocas: one reaching its player explodes; the rest are killed with their style.
            w.expect(!crabs.isEmpty(), "phase 2 began without its nylocas");
            w.expect(!hard() || crabs.stream().allMatch(c -> c.age >= TobRules.VERZIK_HARD_NYLO_CYCLE_TICKS
                    || c.breed == TobNylocas.Breed.TOXOBOLOS.ordinal()), "Hard mode nylocas did not come green (Toxobolos)");
            dealWithCrabs(w);
            // The Athanatos is thrown at a player and heals her; only poison truly hurts it, and one killed without poison
            // comes back.
            w.until(() -> alive(athanatos), TobRules.ATHANATOS_LANDING_DELAY + 3, "no Nylocas Athanatos came in phase 2");
            w.expect(athanatosLandings == 1, "the Athanatos did not land with its projectile");
            int ath = raid.hitpoints(athanatos);
            int raw = w.hit(athanatos, 40, AttackType.SLASH);
            w.expect(raw > 0 && raw <= 10, "a 40 non-poison hit on the Athanatos dealt " + raw + " (expected a quarter)");
            w.expect(alive(athanatos) && raid.hitpoints(athanatos) < ath, "raw damage did nothing to the Athanatos");
            for (int i = 0; i < 20 && alive(athanatos); i++) {
                w.hit(athanatos, 2000, AttackType.SLASH);
                w.tick(1);
            }
            w.expect(!alive(athanatos), "the Athanatos could not be killed with raw damage at all");
            w.until(() -> alive(athanatos), TobRules.ATHANATOS_RESPAWN_TICKS + TobRules.ATHANATOS_LANDING_DELAY + 4,
                    "the Athanatos killed without poison did not come back");
            w.expect(athanatosRespawns == 1 && athanatosBursts == 0, "the raw kill counted as a burst");
            // A poisoned weapon bursts it at once, hurting Verzik.
            int before = raid.hitpoints(verzik);
            w.hold("toxic_blowpipe");
            w.expect(w.hit(athanatos, 0, AttackType.RANGED) == 0 && alive(athanatos), "a missed poisoned attack burst the Athanatos");
            w.hit(athanatos, 5, AttackType.RANGED);
            w.expect(athanatosBursts == 1 && !alive(athanatos), "a poisoned weapon hit did not burst the Athanatos");
            w.expect(raid.hitpoints(verzik) <= before + TobRules.ATHANATOS_HEAL_MAX, "the bursting Athanatos did not hurt Verzik");
            w.tick(TobRules.ATHANATOS_RESPAWN_TICKS + TobRules.ATHANATOS_LANDING_DELAY + 2);
            w.expect(!alive(athanatos), "a burst Athanatos came back");
            w.hold(null);
            // The body slam: stand right next to her.
            Vec3 beside = verzik.position().add(2, 0, 0);
            for (int i = 0; i < 40 && slams == 0; i++) {
                w.moveTo(beside);
                w.tick(1);
            }
            w.expect(slams > 0 && bounced, "standing next to Verzik in phase 2 never drew her body slam");
            w.moveTo(at(-12, 0));
            // Insulated boots soften the lightning (25 instead of 48).
            w.player().setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, Inv.stack("insulated_boots", 1));
            int balls = lightnings;
            lastLightningFlight = -1;
            w.until(() -> lightnings > balls, 30, "no lightning ball came in phase 2");
            // since round 42 the ball flies three game ticks, named to its target as it leaves
            w.until(() -> lastLightningFlight >= 0, LIGHTNING_TICKS + 3, "the lightning ball never landed");
            w.expect(lastLightningFlight >= LIGHTNING_TICKS, "the lightning ball landed " + lastLightningFlight + " game ticks after it left");
            w.expect(lastLightningMax == TobRules.VERZIK_LIGHTNING_INSULATED_MAX, "insulated boots did not soften the lightning ball ("
                    + lastLightningMax + ")");
            // round 43: the ball is passed between players; alone, with no teammate to pass it to, it strikes its first player
            w.expect(lightningStrikes > 0 && lightningReached == 1 && lightningThrough == 0 && lightningChainHits == 0,
                    "a solo player's lightning ball reached " + lightningReached + " player(s), struck " + lightningStrikes
                            + ", arced through Verzik " + lightningThrough + ", chained through " + lightningChainHits);
            w.player().setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, ItemStack.EMPTY);
            guard = 0;
            int healedBefore = 0;
            while (form == 2 && guard++ < 900) {
                dealWithCrabs(w);
                if (p25 && summoning > 0 && healedBefore == 0) {
                    // Damage during a summon heals her.
                    int hp = raid.hitpoints(verzik);
                    w.hit(verzik, 20, AttackType.SLASH);
                    w.expect(raid.hitpoints(verzik) > hp, "a hit during her Matomenos summon did not heal Verzik");
                    healedBefore = 1;
                    w.tick(summoning + 1);
                    // Leave the first pair alive: she must absorb them when the next pair comes.
                    int absorbed = matomenosAbsorbed;
                    w.until(() -> matomenosAbsorbed > absorbed, TobRules.VERZIK_MATOMENOS_INTERVAL + 2,
                            "Verzik did not absorb the Matomenos left alive");
                    w.expect(matomenosHealed > 0, "absorbing the Matomenos did not heal her");
                    continue;
                }
                for (Mob m : new ArrayList<>(matomenos)) {
                    if (alive(m) && summoning <= 0) {
                        w.expect(raid.maxHitpoints(m) == RaidScaling.tobScaledHp("nylocas_matomenos", TobRules.VERZIK_MATOMENOS_HP,
                                raid.partySize(), raid.mode()), "her Matomenos don't have 200 (scaled) hitpoints");
                        w.kill(m, AttackType.MAGIC, "a Nylocas Matomenos");
                    }
                }
                if (summoning <= 0) {
                    w.hit(verzik, Math.max(10, raid.maxHitpoints(verzik) / 15), AttackType.SLASH);
                }
                w.tick(1);
            }
            w.expect(form == 3, "Verzik did not reach phase 3 (phase 2.5 " + p25 + ")");
            w.expect(p25, "Verzik skipped phase 2.5");
            w.expect(crabsPopped > 0, "no nylocas exploded on reaching its player");
            w.expect(urnsLanded > 0, "no urnbomb landed in phase 2");
            w.expect(!hard() || urnPoolsMade > 0, "Hard mode urnbombs left no poison pools");
            guard = 0;
            while (alive(verzik) && form == 3 && guard++ < 2000) {
                dealWithCrabs(w);
                // Slowly enough for her special rotation to come round (a special every fifth attack, 7 ticks apart).
                w.hit(verzik, Math.max(10, raid.maxHitpoints(verzik) / 80), AttackType.RANGED);
                w.tick(1);
            }
            w.until(() -> done, 3, "Verzik's death did not end the fight");
            w.expect(enraged, "Verzik never enraged at 20%");
            w.expect(specials >= 2, "Verzik used " + specials + " phase 3 special(s)");
            w.expect(!hard() || lastStand, "Hard mode Verzik never healed at 5%");
            w.expect(!p3Melee, "a ranged-only phase 3 recorded a melee hit (Harder Mode III)");
            return "three phases, " + specials + " phase 3 special(s), " + lightnings + " lightning ball(s), " + crabsPopped + " nylocas popped, "
                    + athanatosBursts + " Athanatos burst" + (hard() ? ", " + debris + " debris hit(s), " + crabsCycled + " style change(s)" : "");
        }
    }
}
