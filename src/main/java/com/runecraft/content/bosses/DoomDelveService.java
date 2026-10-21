package com.runecraft.content.bosses;

import com.runecraft.boss.BossScripts;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.collection.CollectionLog;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.Equipment;
import com.runecraft.combat.MonsterStats;
import com.runecraft.combat.PoisonService;
import com.runecraft.content.systems.CombatAchievementService;
import com.runecraft.content.systems.SpecialAttackService;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.bosses.BossVariant;
import com.runecraft.osrs.content.bosses.DoomDelves;
import com.runecraft.osrs.content.bosses.ModernBosses;
import com.runecraft.osrs.content.slayer.TormentedDemons;
import com.runecraft.osrs.content.systems.Teleports;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModItems;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.platform.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Doom of Mokhaiotl's delves. Each defeat sends the Doom one level deeper and holds that level's loot for the
 * player: using the summoning stone again follows it down (the next level is harder), sneaking and using the stone
 * claims the loot and ends the run. Dying on a delve run forfeits the run's loot; leaving the ruins (teleporting or walking
 * off) ends the run too, but its loot waits in the lobby chest (claimed the same way). Runs, demonic charge stacks, held
 * loot and the chest are kept in the world save ({@link DoomDelveState}). The deepest level reached is kept as a personal
 * best ({@link DoomDelves#DEEPEST_KEY} in the kill counts), as is the number of deep delves (9+) completed. The Doom is
 * fought after The Final Dawn; Mokhaiotl waystones teleport to its ruins.
 */
final class DoomDelveService {
    private DoomDelveService() {
    }

    /** Acid blood left on each player's last level (it carries over to the next level, except delve 6). */
    private static final Map<UUID, List<Vec3>> ACID = new HashMap<>();
    /** A run whose player is this far (blocks) from where its last level was fought, or in another dimension, has left. */
    static final double LEAVE_BLOCKS = 48;
    /** Where a waystone lands: just north of the lair (the Ruins of Mokhaiotl, OSRS 1311, 9497 underground). // approx */
    private static final Teleports.Destination RUINS = Teleports.at("Ruins of Mokhaiotl", 1282, 3140, "the_final_dawn");

    static void init() {
        BossScripts.register(DoomDelves.BOSS, DoomScript::new);
        DataBossService.requirement(DoomDelves.BOSS, DoomDelveService::beforeSummon);
        DataBossService.variant(DoomDelves.BOSS, DoomDelveService::nextLevel);
        DataBossService.onKilled(DoomDelves.BOSS, DoomDelveService::completed);
        // The kill drops nothing: the level's loot is rolled into the burrow hole by completed().
        DataBossService.loot(DoomDelves.BOSS, (killer, boss, table) -> List.of());
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer p) {
                died(p);
            }
        });
        // Demonic larvae: non-demonbane hits deal at most 1, a demonbane hit kills a small larva outright.
        CombatHooks.addModifier(new CombatHooks.PlayerAttackModifier() {
            @Override
            public int afterRoll(ServerPlayer player, LivingEntity target, AttackType type, int damage) {
                if (!isLarva(target)) {
                    return damage;
                }
                return DoomDelves.larvaHit(damage, demonbane(player));
            }
        });
        com.runecraft.platform.ServerTickEvents.END_SERVER_TICK.register(DoomDelveService::checkLeavers);
        com.runecraft.platform.UseItemCallback.EVENT.register((player, world, hand) -> {
            net.minecraft.world.item.ItemStack stack = player.getItemInHand(hand);
            if (world.isClientSide() || stack.isEmpty() || !(player instanceof ServerPlayer p)) {
                return net.minecraft.world.InteractionResultHolder.pass(stack);
            }
            String id = Inv.idOf(stack);
            if (DoomDelves.WAYSTONE.equals(id)) {
                useWaystone(p);
            } else if (DoomDelves.SUN_KISSED_BONES.equals(id)) {
                breakDownBones(p);
            } else {
                return net.minecraft.world.InteractionResultHolder.pass(stack);
            }
            return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, false);
        });
    }

    private static boolean isLarva(LivingEntity e) {
        MonsterStats stats = MonsterStats.of(e);
        if (stats.profile() == null) {
            return false;
        }
        String id = stats.profile().id;
        return ModernBosses.DOOM_LARVA.equals(id) || ModernBosses.DOOM_GIANT_LARVA.equals(id);
    }

    static boolean demonbane(ServerPlayer p) {
        return TormentedDemons.pierces(BossFx.heldId(p)); // approx: demonbane spells and the eye of ayak are not counted
    }

    private static DoomDelveState state(ServerPlayer p) {
        return DoomDelveState.get(p.getServer());
    }

    private static boolean questDone(ServerPlayer player) {
        var quest = com.runecraft.osrs.content.ContentRegistry.quest(DoomDelves.QUEST);
        return quest == null || Players.data(player).questComplete(quest);
    }

    /** The Final Dawn first; sneak-using the stone claims the loot held from the run and the chest instead of descending. */
    private static boolean beforeSummon(ServerPlayer player, BossDef boss) {
        DoomDelveState.Run run = state(player).get(player.getUUID());
        if (player.isShiftKeyDown() && run != null) {
            claim(player, run);
            return false;
        }
        if (!questDone(player)) {
            Msg.game(player, "You need to complete The Final Dawn to challenge the Doom of Mokhaiotl.");
            return false;
        }
        return true;
    }

    private static BossVariant nextLevel(ServerPlayer player, BossDef boss) {
        DoomDelveState.Run run = state(player).getOrCreate(player.getUUID());
        int level = run.completed + 1;
        if (level > 1) {
            Msg.game(player, "You follow the Doom of Mokhaiotl down the burrow hole to delve level " + level + ".");
        } else {
            ACID.remove(player.getUUID());
            run.stacks = 0; // a fresh run: no demonic charge
        }
        return DoomDelves.variant(boss, level);
    }

    private static void completed(ServerPlayer killer, DataBossEntity boss) {
        int level = Math.max(1, boss.delve());
        ServerPlayer player = delver(boss, killer, level);
        if (player == null) {
            // not silent: the kill counted, but this level was no step of the killer's run (another's summon, or a run ended)
            if (killer != null) {
                Msg.game(killer, "This delve level was not the next one of your run: its loot is not held for you.");
            }
            return;
        }
        DoomDelveState st = state(player);
        DoomDelveState.Run run = st.getOrCreate(player.getUUID());
        run.completed = level;
        run.at = boss.blockPosition().asLong();
        run.dimension = boss.level().dimension().location().toString();
        List<DoomDelves.Loot> loot = DoomDelves.roll(level, new java.util.Random(player.getRandom().nextLong()));
        run.pending.addAll(loot);
        st.setDirty();
        PlayerData data = Players.data(player);
        int best = data.killCount(DoomDelves.DEEPEST_KEY);
        if (level > best) {
            data.killCounts().put(DoomDelves.DEEPEST_KEY, level);
            Msg.game(player, "New personal best: delve level " + level + ".");
        }
        if (level >= DoomDelves.DEEPEST) {
            data.incrementKillCount(DoomDelves.DEEP_DELVES_KEY);
        }
        boolean unique = loot.stream().anyMatch(l -> DoomDelves.UNIQUES.contains(l.item()));
        Msg.game(player, "Delve level " + level + " complete. Loot held: " + summary(run.pending) + ".");
        if (unique) {
            Msg.red(player, "A light shines from the burrow hole... delving deeper would risk your unique reward!");
        }
        Msg.game(player, "Use the summoning stone to delve deeper (level " + (level + 1) + "), or sneak and use it to claim your loot.");
    }

    /**
     * The player whose run a killed delve level belongs to: the one who summoned it at the stone, whoever dealt the damage or
     * the killing blow (the OSRS fight is an instance: nobody else can be in it, and the level is the next one of the run that
     * delved into it). Only while that run is still the one the level came from (it has completed the level before): a run
     * ended by a death or by leaving gets nothing, and a level never goes to a player who did not delve to it (a helper with no
     * run, or a shallower one, would otherwise skip levels and take deep loot). A boss with no summoner recorded (spawned
     * without the stone: an admin's or the self-test's, or one saved before round 43) goes to its top damager or killer whose
     * run it continues. No run at all is a run at level 0 ({@link DoomDelveState.Run#completed}, as the stone's first summon
     * creates it): delve 1 starts a fresh run, and only delve 1. Null: nobody's run.
     */
    static ServerPlayer delver(DataBossEntity boss, ServerPlayer killer, int level) {
        ServerPlayer top = com.runecraft.combat.KillCredit.topDamager(boss, killer);
        UUID summoner = boss.summoner();
        List<ServerPlayer> candidates = new ArrayList<>();
        if (summoner != null) {
            MinecraftServer server = boss.getServer();
            for (ServerPlayer p : new ServerPlayer[]{top, killer, server == null ? null : server.getPlayerList().getPlayer(summoner)}) {
                if (p != null && summoner.equals(p.getUUID())) {
                    candidates.add(p);
                }
            }
        } else {
            candidates.add(top);
            candidates.add(killer);
        }
        for (ServerPlayer p : candidates) {
            if (p == null) {
                continue;
            }
            DoomDelveState.Run run = state(p).get(p.getUUID());
            if ((run == null ? 0 : run.completed) == level - 1) {
                return p;
            }
        }
        return null;
    }

    /** A death on a delve run forfeits the run's loot (OSRS: "they will forfeit all accumulated drops"); the chest is kept. */
    static void died(ServerPlayer p) {
        ACID.remove(p.getUUID());
        DoomDelveState st = state(p);
        DoomDelveState.Run run = st.get(p.getUUID());
        if (run == null || !run.active()) {
            return;
        }
        if (!run.pending.isEmpty()) {
            Msg.red(p, "Your delve run is over; the Doom's loot is lost.");
        }
        run.pending.clear();
        run.completed = 0;
        run.stacks = 0;
        if (run.empty()) {
            st.remove(p.getUUID());
        }
        st.setDirty();
    }

    /** Ends a run whose player left the ruins: back to delve 1, its loot moved to the lobby chest. */
    static void left(ServerPlayer p, DoomDelveState st, DoomDelveState.Run run) {
        ACID.remove(p.getUUID());
        run.chest.addAll(run.pending);
        boolean any = !run.pending.isEmpty();
        run.pending.clear();
        run.completed = 0;
        run.stacks = 0;
        st.setDirty();
        Msg.game(p, any ? "You leave the delve. Your unclaimed loot waits in the chest by the scoreboard (sneak and use the summoning stone to claim it)."
                : "You leave the delve; the Doom of Mokhaiotl returns to the surface.");
    }

    private static void checkLeavers(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) {
            return;
        }
        DoomDelveState st = DoomDelveState.get(server);
        for (Map.Entry<UUID, DoomDelveState.Run> e : new ArrayList<>(st.all().entrySet())) {
            DoomDelveState.Run run = e.getValue();
            if (!run.active() || run.dimension == null) {
                continue;
            }
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null || !p.isAlive()) {
                continue;
            }
            if (hasLeft(p, run)) {
                left(p, st, run);
            }
        }
    }

    static boolean hasLeft(ServerPlayer p, DoomDelveState.Run run) {
        if (!p.level().dimension().location().toString().equals(run.dimension)) {
            return true;
        }
        BlockPos at = BlockPos.of(run.at);
        double dx = p.getX() - (at.getX() + 0.5);
        double dz = p.getZ() - (at.getZ() + 0.5);
        return dx * dx + dz * dz > LEAVE_BLOCKS * LEAVE_BLOCKS;
    }

    /** Self-test: stacks of loot this player's run holds in the burrow hole and the chest (claimed at the stone), 0 without. */
    static int heldLoot(ServerPlayer player) {
        DoomDelveState.Run run = state(player).get(player.getUUID());
        return run == null ? 0 : run.pending.size() + run.chest.size();
    }

    /** Self-test: the last level this player's run completed (0: no run going). */
    static int runLevel(ServerPlayer player) {
        DoomDelveState.Run run = state(player).get(player.getUUID());
        return run == null ? 0 : run.completed;
    }

    /** Self-test: stacks of demonic charge on this player's run. */
    static int runStacks(ServerPlayer player) {
        DoomDelveState.Run run = state(player).get(player.getUUID());
        return run == null ? 0 : run.stacks;
    }

    /** Self-test: takes this player's run out of the save (restored by {@link #restoreRun}); null when they had none. */
    static DoomDelveState.Run takeRun(ServerPlayer player) {
        ACID.remove(player.getUUID());
        return state(player).remove(player.getUUID());
    }

    static void restoreRun(ServerPlayer player, DoomDelveState.Run run) {
        DoomDelveState st = state(player);
        st.remove(player.getUUID());
        if (run != null) {
            st.all().put(player.getUUID(), run);
            st.setDirty();
        }
    }

    /** Self-test: the server tick's leave check for one player; true when it ended their run. */
    static boolean checkLeft(ServerPlayer player) {
        DoomDelveState st = state(player);
        DoomDelveState.Run run = st.get(player.getUUID());
        if (run == null || !run.active() || !hasLeft(player, run)) {
            return false;
        }
        left(player, st, run);
        return true;
    }

    /** Self-test: forgets this player's run, chest and acid (a clean start). */
    static void resetRun(ServerPlayer player) {
        state(player).remove(player.getUUID());
        ACID.remove(player.getUUID());
    }

    /** Self-test: the stone's claim (sneak-use): every held stack goes to the player; false when nothing was held. */
    static boolean claimForSelfTest(ServerPlayer player) {
        DoomDelveState.Run run = state(player).get(player.getUUID());
        if (run == null) {
            return false;
        }
        claim(player, run);
        return true;
    }

    /** Self-test: the next level of this player's run, spawned at {@code at} as the stone would (variant included). */
    static DataBossEntity descend(ServerPlayer player, ServerLevel world, BlockPos at) {
        BossDef def = com.runecraft.osrs.content.ContentRegistry.boss(DoomDelves.BOSS);
        BossVariant v = nextLevel(player, def);
        DataBossEntity boss = DataBossService.spawn(world, at, def, player);
        if (boss != null) {
            boss.summoner(player.getUUID());
            boss.applyVariant(v);
        }
        return boss;
    }

    private static void claim(ServerPlayer player, DoomDelveState.Run run) {
        state(player).remove(player.getUUID());
        ACID.remove(player.getUUID());
        List<DoomDelves.Loot> all = new ArrayList<>(run.chest);
        all.addAll(run.pending);
        if (all.isEmpty()) {
            Msg.game(player, "You have no loot waiting from the Doom.");
            return;
        }
        for (DoomDelves.Loot l : all) {
            Inv.give(player, l.item(), l.count());
            CollectionLog.obtain(player, l.item());
        }
        if (run.completed > 0) {
            Msg.game(player, "You claim your rewards from " + run.completed + " delve level" + (run.completed == 1 ? "" : "s") + ".");
        } else {
            Msg.game(player, "You collect your unclaimed rewards from the chest.");
        }
    }

    /** Stacks of demonic charge on this player's run (they carry over between levels); returns the count before. */
    static int addStacks(ServerPlayer player, int n) {
        DoomDelveState st = state(player);
        DoomDelveState.Run run = st.getOrCreate(player.getUUID());
        int before = run.stacks;
        run.stacks += n;
        st.setDirty();
        return before;
    }

    /** A Mokhaiotl waystone: one use, to the Ruins of Mokhaiotl after The Final Dawn. Public for the self-test. */
    static boolean useWaystone(ServerPlayer p) {
        if (!Inv.has(p, DoomDelves.WAYSTONE, 1)) {
            return false;
        }
        if (!com.runecraft.content.systems.SysTeleport.teleport(p, RUINS, com.runecraft.content.systems.SysTeleport.SPELL_WILDERNESS)) {
            return false;
        }
        Inv.remove(p, DoomDelves.WAYSTONE, 1);
        return true;
    }

    /** Breaks down every sun-kissed bones carried with a chisel: 45 blessed bone shards and 5 Crafting XP each. */
    static int breakDownBones(ServerPlayer p) {
        if (!Inv.has(p, "chisel", 1)) {
            Msg.game(p, "You need a chisel to break down the bones.");
            return 0;
        }
        int n = Inv.count(p, DoomDelves.SUN_KISSED_BONES);
        if (n <= 0) {
            return 0;
        }
        Inv.remove(p, DoomDelves.SUN_KISSED_BONES, n);
        Inv.give(p, "blessed_bone_shards", n * DoomDelves.SHARDS_PER_SUN_KISSED_BONES);
        com.runecraft.skill.XpService.add(p, com.runecraft.osrs.Skill.CRAFTING, DoomDelves.SUN_KISSED_CRAFTING_XP * n);
        Msg.game(p, "You break down " + n + " sun-kissed bones into " + n * DoomDelves.SHARDS_PER_SUN_KISSED_BONES + " blessed bone shards.");
        return n;
    }

    private static String summary(List<DoomDelves.Loot> loot) {
        Map<String, Integer> merged = new java.util.LinkedHashMap<>();
        for (DoomDelves.Loot l : loot) {
            merged.merge(l.item(), l.count(), Integer::sum);
        }
        List<String> parts = new ArrayList<>();
        merged.forEach((item, n) -> parts.add(n + " x " + item.replace('_', ' ')));
        return String.join(", ", parts);
    }

    private static boolean wearsShield(ServerPlayer p) {
        if (Equipment.twoHanded(p)) {
            return false;
        }
        ItemDef def = ModItems.def(p.getOffhandItem());
        return def != null && def.equipment() != null && def.equipment().slot() == EquipmentDef.Slot.SHIELD;
    }

    /**
     * The fight (OSRS Wiki "Doom of Mokhaiotl" and its strategy page; timings the wiki doesn't give are approx):
     * <ul>
     *   <li>Standard attacks: magic (blue) and ranged (green) orbs, melee (red) orbs from delve 2, all blocked by the matching
     *       prayer; a tongue lash (up to 40, halved by Protect from Melee) when you stand next to it.</li>
     *   <li>Rock Throw: a rock explodes 7 ticks after it is thrown into debris on looming shadows around you (15-20), switches
     *       off your protection prayers and launches 1-3 projectiles (two sets from delve 8) that always hit unless you pray
     *       again; the rock on your tile stays (shelter from car slams).</li>
     *   <li>Melee charge: with a rock throw the Doom charges its Special Beam Cannon (60, 80 at delves 4-5, 99 from 6); only
     *       a melee hit interrupts it (the melee punish: a fifth of your melee strength bonus as extra damage, its next
     *       attack delayed 8/7/6 ticks). Attacks while it charges always hit; a kill by a melee punish scatters holy water.</li>
     *   <li>Demonic larvae (grubs) crawl to the Doom; one that reaches it heals the Doom and hurts you, more for every stack
     *       of demonic charge (stacks carry over between levels). Killed larvae explode: 5-10 to the Doom (100 to its
     *       shield) by it, else up to 21 to players in a 3x3 area (giant larvae 4x4).</li>
     *   <li>Shield phase (delve 3+): at 75% hitpoints it raises a 500-point shield only demonbane breaks while larvae drop
     *       constantly; each demonbane hit resets its charge bar, a full bar fires the beam and ends the phase. From delve 5 it
     *       loops: shield, burrow, volatile earth and shockwave, then the shield again after two attacks.</li>
     *   <li>Burrow ("car") phase (delve 5+): rocks fall, it charges constantly (any hit resets the bar, every hit lands) and
     *       zooms twice (delve 5) or three times at a yellow eye four tiles past you, trampling (10/20/30/40) anyone in its way;
     *       from delve 6 each zoom ends in a car slam that only a rock between you and it blocks.</li>
     *   <li>Shockwave: about a minute in (from delve 5 right after the burrow), volatile earth rises; 20 ticks later 1-5
     *       shockwaves (30-41 each) land. Destroy two to raise an earthen shield that walks from the second towards the
     *       first (half speed at delve 1), clearing acid blood, and stand in it.</li>
     *   <li>Acid blood (delve 3+): sprayed when it is damaged outside its shield and burrow; up to 7 a tick with venom, and it
     *       stays for the next level (delve 6 wipes it once).</li>
     * </ul>
     */
    static final class DoomScript extends ScriptedBoss {
        private record Larva(Mob mob, boolean giant) {
        }

        /** A thrown rock: it explodes at {@code due}, then its debris and projectiles land. */
        private static final class RockThrow {
            final long due;
            final boolean magicFirst;
            long landsAt = -1;
            final List<Vec3> shadows = new ArrayList<>();
            int projectilesLeft;
            long nextProjectile;
            boolean magicNext;
            Vec3 stoodAt;

            RockThrow(long due, boolean magicFirst) {
                this.due = due;
                this.magicFirst = magicFirst;
            }
        }

        private enum Zoom { WINDUP, MOVING, SLAM }

        private int level;
        private boolean started;
        private long lastSplat;
        private final List<Vec3> acid = new ArrayList<>();
        /** The acid blood, the shockwave's ring and the earthen shield as floor blocks (round 43). */
        private com.runecraft.boss.FloorMarkers.Layer doomFloor;
        private final List<Larva> larvae = new ArrayList<>();
        private long nextLarva;
        // Beam / melee punish / rock throws
        private int attacksSinceBeam;
        private int attacksSinceRock;
        private int beamTicks;
        private long exposedUntil;
        private boolean holyWater;
        private final List<RockThrow> throwsInFlight = new ArrayList<>();
        /** Rocks standing in the arena (rock throws and the burrow's falling rocks): shelter from car slams. */
        private final List<Vec3> rocks = new ArrayList<>();
        // Shield phase
        private boolean shield;
        private int shieldPoints;
        private int shieldCharge;
        private int shieldsDone;
        private int attacksSinceShield;
        /** From delve 5 the shield comes back two attacks after each shockwave. */
        private boolean loopShield;
        // Burrow phase
        private boolean burrowed;
        private int zoomsLeft;
        private Zoom zoom;
        private int zoomTicks;
        private Vec3 zoomFrom;
        private Vec3 eye;
        private int burrowCharge;
        private final Set<UUID> trampled = new HashSet<>();
        // Shockwave
        private long nextShockwave;
        private long shockwaveDue = -1;
        private int shockwavesLeft;
        private final List<Mob> earth = new ArrayList<>();
        private final List<Vec3> earthBroken = new ArrayList<>();
        private Vec3 earthenShield;
        /** Where the earthen shield stood before its last step, and the Minecraft tick it stepped on (drawn sliding between). */
        private Vec3 earthenPrev;
        private long earthenMovedMc;
        private Vec3 earthenTarget;
        private int cleared;

        @Override
        protected boolean driven() {
            return true;
        }

        @Override
        protected int attackDelay(DataBossEntity boss, AttackDef attack) {
            return DoomDelves.attackSpeed(Math.max(1, boss.delve()));
        }

        @Override
        protected boolean canAttack(DataBossEntity boss) {
            return beamTicks <= 0 && !shield && !burrowed && !shockwaveLanding();
        }

        /** The shockwaves themselves are landing (the Doom keeps attacking through the volatile earth's wind-up). */
        private boolean shockwaveLanding() {
            return shockwaveDue >= 0 && now >= shockwaveDue;
        }

        @Override
        public boolean isVulnerable(DataBossEntity boss) {
            return true; // the burrowed Doom takes hits too (every one lands); the shield is handled in incoming()
        }

        /** Attacks made while it charges (melee charge, shield, burrow) always land: no defence roll. */
        @Override
        public void adjustDefence(DataBossEntity boss, ServerPlayer attacker, AttackType type, com.runecraft.osrs.PlayerHitCalc.Input in) {
            if (beamTicks > 0 || burrowed || shield) {
                in.targetDefenceLevel = 0;
                in.targetDefenceBonus = -64;
                if (in.targetRangedDefence != null) {
                    java.util.Arrays.fill(in.targetRangedDefence, -64);
                }
            }
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            if (shieldDue(boss)) {
                startShield(boss);
                return null;
            }
            attacksSinceShield++;
            if (attacksSinceBeam >= DoomDelves.ATTACKS_PER_BEAM && shockwaveDue < 0) {
                startBeam(boss, target);
                return null;
            }
            attacksSinceBeam++;
            if (++attacksSinceRock >= DoomDelves.ATTACKS_PER_ROCK_THROW) {
                throwRock(boss, target);
                cooldown = attackDelay(boss, null) - 1;
                return null;
            }
            if (distance <= 2.5) {
                tongueLash(boss, target);
                return null;
            }
            int roll = boss.getRandom().nextInt(level >= 2 ? 3 : 2);
            return roll == 0 ? ModernBosses.DOOM_MAGIC : roll == 1 ? ModernBosses.DOOM_RANGED : ModernBosses.DOOM_MELEE_ORB;
        }

        /** Whether the shield phase starts now: at 75% after two attacks (once at delves 3-4), and two attacks after each shockwave from 5. */
        private boolean shieldDue(DataBossEntity boss) {
            if (level < DoomDelves.SHIELD_FROM || attacksSinceShield < DoomDelves.SHIELD_MIN_ATTACKS) {
                return false;
            }
            if (shieldsDone == 0) {
                return boss.getHealth() <= boss.getMaxHealth() * DoomDelves.SHIELD_BELOW;
            }
            return level >= DoomDelves.BURROW_FROM && loopShield;
        }

        private void tongueLash(DataBossEntity boss, ServerPlayer target) {
            cooldown = attackDelay(boss, null) - 1;
            attackCount++;
            boss.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            int dmg = boss.getRandom().nextInt(DoomDelves.MELEE_MAX + 1);
            if (BossFx.protects(target, Prayer.PROTECT_FROM_MELEE)) {
                dmg /= 2;
            }
            CombatHooks.nameAttack(ModernBosses.DOOM_MELEE.name(), AttackType.CRUSH);
            CombatHandler.applyDamage(boss, target, dmg);
        }

        /**
         * The players its mechanics reach: those around it plus its target. The Doom is fought alone in an instance (OSRS Wiki
         * "Doom of Mokhaiotl"), so every mechanic aimed at "the player" (the Rock Throw exploding over them and switching
         * their protection prayers off, at every delve level; the debris, larvae, shockwaves, the burrow) lands on the one
         * it fights, even when that player isn't in the level's player list (the self-test's fake player); others standing
         * by are hit too, as the lair is shared.
         */
        private static List<ServerPlayer> fighters(DataBossEntity boss, ServerPlayer target) {
            List<ServerPlayer> list = new ArrayList<>(BossFx.players(boss, 24));
            if (target != null && target.isAlive() && target.level() == boss.level() && target.distanceToSqr(boss) <= 24 * 24
                    && !list.contains(target)) {
                list.add(target);
            }
            return list;
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
            if (!boss.isAlive()) {
                return;
            }
            level = Math.max(1, boss.delve());
            if (!started && target != null) {
                started = true;
                List<Vec3> carried = ACID.remove(target.getUUID());
                if (carried != null && level != DoomDelves.ACID_WIPE_LEVEL) {
                    acid.addAll(carried);
                } else if (carried != null && !carried.isEmpty()) {
                    Msg.game(target, "The acid blood is washed away as you reach delve level " + level + ".");
                }
                nextShockwave = now + DoomDelves.SHOCKWAVE_INTERVAL;
                nextLarva = now + DoomDelves.larvaInterval(level);
            }
            if (!started) {
                return;
            }
            List<ServerPlayer> near = fighters(boss, target);
            for (ServerPlayer p : near) {
                if (!wearsShield(p)) {
                    CombatAchievementService.flag(p, DoomDelves.FLAG_NO_SHIELD);
                }
            }
            ServerLevel world = BossFx.world(boss);
            for (Vec3 r : rocks) {
                world.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, r.x, r.y + 0.3, r.z, 1, 0.3, 0.3, 0.3, 0);
            }
            tickAcid(boss, near);
            tickLarvae(boss, target, near);
            tickRockThrows(boss, target, near);
            if (beamTicks > 0) {
                beamTicks--;
                BossFx.burst(boss, ParticleTypes.ELECTRIC_SPARK, 8, 1.0);
                if (beamTicks == 0) {
                    fireBeam(boss, target);
                }
            }
            if (shield) {
                tickShield(boss, target);
            }
            if (burrowed) {
                tickBurrow(boss, target, near);
            }
            tickShockwave(boss, near);
            showFloor(boss);
            if (!shield && !burrowed && now >= nextLarva && target != null) {
                nextLarva = now + DoomDelves.larvaInterval(level);
                boolean giant = DoomDelves.giantLarvae(level);
                for (int i = 0; i < DoomDelves.larvaeOutsideShield(level); i++) {
                    spawnLarva(boss, target, giant);
                }
            }
        }

        // ---------------- Special Beam Cannon, the melee punish and Rock Throw ----------------

        private void startBeam(DataBossEntity boss, ServerPlayer target) {
            attacksSinceBeam = 0;
            beamTicks = DoomDelves.BEAM_CHARGE_TICKS;
            BossFx.say(boss, "The Doom of Mokhaiotl prays against magic and missiles and begins charging a beam... punish it with melee!");
            BossFx.sound(boss, SoundEvents.BEACON_POWER_SELECT, 0.6f);
            if (target != null) {
                throwRock(boss, target); // melee charges always come with a rock throw
            }
        }

        private void fireBeam(DataBossEntity boss, ServerPlayer target) {
            if (target == null || !target.isAlive()) {
                return;
            }
            BossFx.say(boss, "The Doom of Mokhaiotl fires its beam!");
            BossFx.world(boss).sendParticles(ParticleTypes.SONIC_BOOM, target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
            CombatHooks.nameAttack("charge", AttackType.CRUSH);
            CombatHandler.applyDamage(boss, target, boss.getRandom().nextInt(DoomDelves.chargeMaxHit(level) + 1));
        }

        private void throwRock(DataBossEntity boss, ServerPlayer target) {
            attacksSinceRock = 0;
            RockThrow t = new RockThrow(now + DoomDelves.ROCK_THROW_TICKS, boss.getRandom().nextInt(3) == 0);
            throwsInFlight.add(t);
            BossFx.warn(boss, t.magicFirst ? "The Doom hurls a glowing blue rock into the air!" : "The Doom hurls a rock into the air!");
        }

        private void tickRockThrows(DataBossEntity boss, ServerPlayer target, List<ServerPlayer> near) {
            ServerLevel world = BossFx.world(boss);
            Iterator<RockThrow> it = throwsInFlight.iterator();
            while (it.hasNext()) {
                RockThrow t = it.next();
                if (t.landsAt < 0) {
                    if (now < t.due || target == null) {
                        continue;
                    }
                    // the rock explodes over the player: shadows where the debris will fall, one on their tile
                    t.stoodAt = target.position();
                    t.shadows.add(t.stoodAt);
                    int extra = level >= 8 ? 8 : 4;
                    for (int i = 0; i < extra; i++) {
                        t.shadows.add(around(boss, t.stoodAt, 8));
                    }
                    t.landsAt = now + 2;
                    t.projectilesLeft = DoomDelves.rockThrowProjectiles(level);
                    t.magicNext = t.magicFirst;
                    t.nextProjectile = now + 2 + (level >= 7 ? 2 : 0);
                    for (ServerPlayer p : near) {
                        BossFx.disablePrayers(p, "The exploding rock knocks your protection prayers away!");
                    }
                    continue;
                }
                if (now < t.landsAt) {
                    for (Vec3 s : t.shadows) {
                        world.sendParticles(ParticleTypes.SMOKE, s.x, s.y + 0.1, s.z, 3, 0.4, 0.02, 0.4, 0);
                    }
                } else if (now == t.landsAt) {
                    for (ServerPlayer p : near) {
                        for (Vec3 s : t.shadows) {
                            if (flat(p.position(), s) < 1.2) {
                                CombatHooks.nameAttack("debris", null);
                                CombatHandler.applyDamage(boss, p, DoomDelves.DEBRIS_MIN
                                        + boss.getRandom().nextInt(DoomDelves.DEBRIS_MAX - DoomDelves.DEBRIS_MIN + 1));
                                break;
                            }
                        }
                        if (flat(p.position(), t.stoodAt) < 1.0) {
                            // the rock that stays on the tile shoves a player who didn't move
                            p.teleportTo(p.getX() + (boss.getRandom().nextBoolean() ? 2 : -2), p.getY(), p.getZ());
                        }
                    }
                    if (rocks.size() < 12) {
                        rocks.add(t.stoodAt);
                    }
                }
                if (t.projectilesLeft > 0 && now >= t.nextProjectile && target != null) {
                    t.projectilesLeft--;
                    t.nextProjectile = now + 1;
                    boolean magic = t.magicNext;
                    t.magicNext = !t.magicNext;
                    int dmg = boss.getRandom().nextInt(DoomDelves.standardMaxHit(level) + 1); // 100% accurate
                    if (BossFx.protects(target, magic ? Prayer.PROTECT_FROM_MAGIC : Prayer.PROTECT_FROM_MISSILES)) {
                        dmg = 0;
                    }
                    CombatHooks.nameAttack(magic ? ModernBosses.DOOM_MAGIC.name() : ModernBosses.DOOM_RANGED.name(),
                            magic ? AttackType.MAGIC : AttackType.RANGED);
                    CombatHandler.applyDamage(boss, target, dmg);
                }
                if (now > t.landsAt && t.projectilesLeft <= 0) {
                    it.remove();
                }
            }
        }

        // ---------------- Shield phase ----------------

        private void startShield(DataBossEntity boss) {
            shield = true;
            loopShield = false;
            shieldPoints = DoomDelves.SHIELD_POINTS;
            shieldCharge = 0;
            beamTicks = 0;
            BossFx.say(boss, "The Doom of Mokhaiotl raises a demonic shield and begins to charge! Only demonbane can break it.");
            BossFx.burst(boss, ParticleTypes.ENCHANT, 30, 1.5);
        }

        private void tickShield(DataBossEntity boss, ServerPlayer target) {
            shieldCharge++;
            BossFx.ring(BossFx.world(boss), boss.position(), boss.getBbWidth() / 2 + 0.5, ParticleTypes.SOUL_FIRE_FLAME);
            if (target != null && shieldCharge % DoomDelves.shieldLarvaInterval(level) == 0) {
                spawnLarva(boss, target, false);
            }
            if (shieldCharge >= DoomDelves.shieldChargeTicks(level)) {
                fireBeam(boss, target);
                endShield(boss);
            }
        }

        private void damageShield(DataBossEntity boss, int amount) {
            shieldPoints -= amount;
            if (shieldPoints <= 0) {
                BossFx.say(boss, "The Doom's shield shatters!");
                endShield(boss);
            } else {
                BossFx.warn(boss, "The Doom's shield: " + shieldPoints + "/" + DoomDelves.SHIELD_POINTS);
            }
        }

        private void endShield(DataBossEntity boss) {
            shield = false;
            shieldsDone++;
            attacksSinceShield = 0;
            cooldown = Math.max(cooldown, 3);
            if (level >= DoomDelves.BURROW_FROM) {
                startBurrow(boss);
            }
        }

        // ---------------- Burrow ("car") phase ----------------

        private void startBurrow(DataBossEntity boss) {
            burrowed = true;
            zoomsLeft = DoomDelves.zooms(level);
            zoom = null;
            burrowCharge = 0;
            for (int i = 0; i < DoomDelves.burrowRocks(level) && rocks.size() < 12; i++) {
                rocks.add(around(boss, homeVec(), 10));
            }
            BossFx.say(boss, "The Doom of Mokhaiotl burrows underground and rocks fall from the ceiling!");
            BossFx.burst(boss, ParticleTypes.CAMPFIRE_COSY_SMOKE, 20, 1.5);
        }

        private void tickBurrow(DataBossEntity boss, ServerPlayer target, List<ServerPlayer> near) {
            ServerLevel world = BossFx.world(boss);
            if (++burrowCharge >= DoomDelves.BURROW_CHARGE_TICKS) {
                burrowCharge = 0;
                fireBeam(boss, target);
            }
            if (zoom == null) {
                if (zoomsLeft <= 0 || target == null) {
                    endBurrow(boss, near);
                    return;
                }
                zoomsLeft--;
                zoom = Zoom.WINDUP;
                zoomTicks = DoomDelves.ZOOM_WINDUP;
                zoomFrom = boss.position();
                eye = eyeBeyond(boss, target);
                trampled.clear();
                BossFx.warn(boss, "A yellow eye marks the Doom's path - get out of its way!");
                return;
            }
            switch (zoom) {
                case WINDUP -> {
                    world.sendParticles(ParticleTypes.GLOW, eye.x, eye.y + 0.3, eye.z, 4, 0.3, 0.1, 0.3, 0);
                    if (--zoomTicks <= 0) {
                        zoom = Zoom.MOVING;
                        zoomTicks = DoomDelves.zoomTicks(level);
                        zoomFrom = boss.position();
                    }
                }
                case MOVING -> {
                    int total = DoomDelves.zoomTicks(level);
                    int step = total - zoomTicks + 1;
                    Vec3 at = zoomFrom.add(eye.subtract(zoomFrom).scale(Math.min(1.0, step / (double) total)));
                    boss.getNavigation().stop();
                    boss.teleportTo(at.x, boss.getY(), at.z);
                    BossFx.burst(boss, ParticleTypes.EXPLOSION, 1, 0.8);
                    rocks.removeIf(r -> flat(r, boss.position()) < 2.5); // it smashes rocks in its way
                    for (ServerPlayer p : near) {
                        if (!trampled.contains(p.getUUID()) && flat(p.position(), boss.position()) <= boss.getBbWidth() / 2 + 1.0) {
                            trampled.add(p.getUUID());
                            CombatHooks.nameAttack("trample", AttackType.CRUSH);
                            CombatHandler.applyDamage(boss, p, DoomDelves.trampleDamage(level));
                        }
                    }
                    if (--zoomTicks <= 0) {
                        if (DoomDelves.carSlams(level)) {
                            zoom = Zoom.SLAM;
                            zoomTicks = 3;
                            BossFx.warn(boss, "The Doom of Mokhaiotl rises for a car slam - get behind a rock!");
                        } else {
                            zoom = null;
                        }
                    }
                }
                case SLAM -> {
                    if (--zoomTicks <= 0) {
                        carSlam(boss, near);
                        zoom = null;
                    }
                }
            }
        }

        /** The eye: four tiles past the player in the compass direction they stand in from the Doom, kept inside the arena. */
        private Vec3 eyeBeyond(DataBossEntity boss, ServerPlayer target) {
            Vec3 d = target.position().subtract(boss.position());
            double angle = Math.atan2(d.z, d.x);
            double snapped = Math.round(angle / (Math.PI / 4)) * (Math.PI / 4);
            Vec3 at = target.position().add(Math.cos(snapped) * 8, 0, Math.sin(snapped) * 8);
            Vec3 c = homeVec();
            double max = Math.max(4, (boss.def() == null ? 14 : boss.def().arenaRadius) - 4);
            Vec3 rel = at.subtract(c);
            double len = Math.sqrt(rel.x * rel.x + rel.z * rel.z);
            if (len > max) {
                at = c.add(rel.x * max / len, 0, rel.z * max / len);
            }
            return BossFx.ground(BossFx.world(boss), at.x, at.z, boss.getY());
        }

        private void carSlam(DataBossEntity boss, List<ServerPlayer> near) {
            BossFx.burst(boss, ParticleTypes.EXPLOSION_EMITTER, 1, 0.5);
            BossFx.sound(boss, SoundEvents.GENERIC_EXPLODE.value(), 0.7f);
            for (ServerPlayer p : near) {
                if (!sheltered(boss, p.position())) {
                    CombatHooks.nameAttack("slam", AttackType.CRUSH);
                    CombatHandler.applyDamage(boss, p, boss.getRandom().nextInt(DoomDelves.slamMaxHit(level) + 1));
                }
            }
            // OSRS: the slam smashes every rock within 15 tiles except those another rock protects. This arena is far smaller
            // than the OSRS cavern, so only rocks near the Doom go (the falling rocks must last three slams). // approx
            List<Vec3> kept = new ArrayList<>();
            for (Vec3 r : rocks) {
                if (flat(r, boss.position()) > DoomDelves.SLAM_SMASH_BLOCKS || sheltered(boss, r, rocks)) {
                    kept.add(r);
                }
            }
            rocks.clear();
            rocks.addAll(kept);
            cooldown = Math.max(cooldown, 2);
        }

        /** Whether a rock stands between the Doom and this spot (within two blocks of the spot, on the Doom's side). */
        private boolean sheltered(DataBossEntity boss, Vec3 spot) {
            return sheltered(boss, spot, rocks);
        }

        private static boolean sheltered(DataBossEntity boss, Vec3 spot, List<Vec3> rocks) {
            Vec3 toBoss = boss.position().subtract(spot);
            for (Vec3 r : rocks) {
                if (r == spot) {
                    continue;
                }
                Vec3 toRock = r.subtract(spot);
                double dist = Math.sqrt(toRock.x * toRock.x + toRock.z * toRock.z);
                if (dist <= 2.0 && dist > 0.01 && toRock.x * toBoss.x + toRock.z * toBoss.z > 0) {
                    return true;
                }
            }
            return false;
        }

        private void endBurrow(DataBossEntity boss, List<ServerPlayer> near) {
            burrowed = false;
            zoom = null;
            for (ServerPlayer p : near) {
                CombatAchievementService.flag(p, DoomDelves.FLAG_BURROWED);
            }
            cooldown = Math.max(cooldown, 2);
            startShockwave(boss); // straight after the burrow phase
        }

        // ---------------- Demonic larvae ----------------

        private void spawnLarva(DataBossEntity boss, ServerPlayer target, boolean giant) {
            Vec3 d = target.position().subtract(boss.position());
            double len = Math.max(0.1, Math.sqrt(d.x * d.x + d.z * d.z));
            double side = (boss.getRandom().nextDouble() - 0.5) * 6;
            double reach = Math.max(4, Math.min(len, 9));
            double x = boss.getX() + d.x / len * reach - d.z / len * side;
            double z = boss.getZ() + d.z / len * reach + d.x / len * side;
            Vec3 at = BossFx.ground(BossFx.world(boss), x, z, boss.getY());
            Mob mob = BossFx.spawnMinion(boss, giant ? ModernBosses.DOOM_GIANT_LARVA : ModernBosses.DOOM_LARVA, at, null);
            if (mob != null) {
                larvae.add(new Larva(mob, giant));
            }
        }

        private void tickLarvae(DataBossEntity boss, ServerPlayer target, List<ServerPlayer> near) {
            Iterator<Larva> it = larvae.iterator();
            while (it.hasNext()) {
                Larva l = it.next();
                Mob mob = l.mob();
                if (!mob.isAlive()) {
                    if (mob.isDeadOrDying()) {
                        explode(boss, l, near);
                    }
                    it.remove();
                    continue;
                }
                Vec3 d = boss.position().subtract(mob.position());
                double len = Math.sqrt(d.x * d.x + d.z * d.z);
                if (len <= boss.getBbWidth() / 2 + 0.6) {
                    absorb(boss, l, target, near);
                    mob.discard();
                    it.remove();
                    continue;
                }
                double step = Math.min(len, 0.6); // approx: about a tile a tick
                mob.teleportTo(mob.getX() + d.x / len * step, mob.getY(), mob.getZ() + d.z / len * step);
            }
        }

        private void absorb(DataBossEntity boss, Larva l, ServerPlayer target, List<ServerPlayer> near) {
            ServerPlayer victim = target != null ? target : near.isEmpty() ? null : near.get(0);
            int stacks = victim != null ? addStacks(victim, DoomDelves.larvaStacks(l.giant())) : 0;
            boss.heal(DoomDelves.larvaHeal(stacks, l.giant()));
            BossFx.burst(boss, ParticleTypes.HEART, 4, 0.8);
            if (victim != null) {
                CombatHooks.nameAttack("larva", null);
                CombatHandler.applyDamage(boss, victim, DoomDelves.larvaDamage(stacks, l.giant(), level));
                Msg.red(victim, "A larva reaches the Doom, which grows stronger (demonic charge " + (stacks + DoomDelves.larvaStacks(l.giant())) + ").");
            }
            for (ServerPlayer p : near) {
                CombatAchievementService.flag(p, DoomDelves.FLAG_GRUB_ABSORBED);
            }
        }

        private void explode(DataBossEntity boss, Larva l, List<ServerPlayer> near) {
            Mob mob = l.mob();
            ServerLevel world = BossFx.world(boss);
            world.sendParticles(ParticleTypes.EXPLOSION, mob.getX(), mob.getY() + 0.3, mob.getZ(), 1, 0, 0, 0, 0);
            double tiles = DoomDelves.larvaExplosionTiles(l.giant());
            double reach = boss.getBbWidth() / 2 + tiles * 2 + 0.5;
            if (flat(mob.position(), boss.position()) <= reach) {
                // by the Doom: it takes the blast (or its shield does) and no player is hurt
                if (shield) {
                    damageShield(boss, DoomDelves.LARVA_SHIELD_DAMAGE);
                } else {
                    int dmg = DoomDelves.LARVA_DOOM_MIN + boss.getRandom().nextInt(DoomDelves.LARVA_DOOM_MAX - DoomDelves.LARVA_DOOM_MIN + 1);
                    boss.setHealth(Math.max(1, boss.getHealth() - dmg));
                }
                return;
            }
            for (ServerPlayer p : near) {
                if (Math.abs(p.getX() - mob.getX()) <= tiles * 2 + 0.5 && Math.abs(p.getZ() - mob.getZ()) <= tiles * 2 + 0.5) {
                    CombatHooks.nameAttack("larva_explosion", null);
                    CombatHandler.applyDamage(boss, p, boss.getRandom().nextInt(DoomDelves.LARVA_EXPLOSION_MAX + 1));
                }
            }
        }

        // ---------------- Shockwave, volatile earth and the earthen shield ----------------

        private void startShockwave(DataBossEntity boss) {
            if (shockwaveDue >= 0) {
                return;
            }
            shockwaveDue = now + DoomDelves.SHOCKWAVE_WINDUP;
            shockwavesLeft = DoomDelves.shockwaves(level);
            earthBroken.clear();
            earthenShield = null;
            cleared = 0;
            // spread around the arena, far enough apart for a shield that lasts through every shockwave
            double radius = Math.max(6, (boss.def() == null ? 14 : boss.def().arenaRadius) - 3);
            for (int i = 0; i < DoomDelves.VOLATILE_EARTH; i++) {
                double a = (i + boss.getRandom().nextDouble() * 0.6) * 2 * Math.PI / DoomDelves.VOLATILE_EARTH;
                double r = radius * (0.7 + 0.3 * boss.getRandom().nextDouble());
                Vec3 c = homeVec();
                Vec3 at = BossFx.ground(BossFx.world(boss), c.x + Math.cos(a) * r, c.z + Math.sin(a) * r, c.y);
                Mob e = BossFx.spawnMinion(boss, ModernBosses.DOOM_VOLATILE_EARTH, at, null);
                if (e != null) {
                    earth.add(e);
                    // "a player will deal their maximum hit to a volatile earth (despite having 1 Hitpoint)"
                    MinionRules.set(e, new MinionRules.Rule() {
                        @Override
                        public double accuracy(ServerPlayer p, LivingEntity minion, AttackType type) {
                            return MinionRules.ALWAYS_HITS;
                        }

                        @Override
                        public boolean alwaysMaxHit(ServerPlayer p, LivingEntity minion, AttackType type) {
                            return true;
                        }

                        @Override
                        public int damage(ServerPlayer p, LivingEntity minion, AttackType type, int d) {
                            return Math.max(1, d);
                        }
                    });
                }
            }
            BossFx.say(boss, "Volatile earth erupts! Destroy two to raise an earthen shield before the shockwave"
                    + (shockwavesLeft > 1 ? "s (" + shockwavesLeft + ")" : "") + " lands.");
        }

        private void tickShockwave(DataBossEntity boss, List<ServerPlayer> near) {
            if (shockwaveDue < 0) {
                if (now >= nextShockwave && !shield && !burrowed && beamTicks <= 0 && !near.isEmpty()) {
                    startShockwave(boss);
                }
                return;
            }
            Iterator<Mob> it = earth.iterator();
            while (it.hasNext()) {
                Mob e = it.next();
                if (!e.isAlive()) {
                    if (e.isDeadOrDying() && earthBroken.size() < 2) {
                        earthBroken.add(e.position());
                    }
                    it.remove();
                }
            }
            if (earthenShield == null && earthBroken.size() >= 2) {
                earthenShield = earthBroken.get(1);
                earthenPrev = earthenShield;
                earthenMovedMc = com.runecraft.action.ActionManager.mcTick();
                earthenTarget = earthBroken.get(0);
                for (Mob e : earth) {
                    e.discard();
                }
                earth.clear();
                BossFx.warn(boss, "An earthen shield rises from the rubble!");
            }
            if (earthenShield != null) {
                Vec3 d = earthenTarget.subtract(earthenShield);
                double len = Math.sqrt(d.x * d.x + d.z * d.z);
                double step = DoomDelves.earthenShieldSpeed(level) * 2.0; // tiles a tick, two blocks a tile
                if (len <= 0.05) {
                    earthenShield = null; // it dissipates on reaching its destination
                } else {
                    earthenPrev = earthenShield;
                    earthenMovedMc = com.runecraft.action.ActionManager.mcTick();
                    earthenShield = earthenShield.add(d.x / len * Math.min(step, len), 0, d.z / len * Math.min(step, len));
                    Iterator<Vec3> a = acid.iterator();
                    while (a.hasNext()) {
                        Vec3 splat = a.next();
                        if (flat(splat, earthenShield) <= 1.0) {
                            a.remove();
                            cleared++;
                        }
                    }
                    BossFx.ring(BossFx.world(boss), earthenShield, DoomDelves.EARTHEN_SHIELD_BLOCKS, ParticleTypes.WAX_ON);
                }
            }
            if (shockwaveDue - now == com.runecraft.osrs.content.bosses.BossRealtime.DOOM_SHOCKWAVE_CUE_TICKS) {
                // the countdown: 36 MC before the first shockwave (its ring closing on the floor from now, tick())
                BossFx.warn(boss, "The shockwave is coming! Stand in the earthen shield"
                        + (shockwavesLeft > 1 ? " (" + shockwavesLeft + " shockwaves)" : "") + "!");
            }
            if (now >= shockwaveDue && (now - shockwaveDue) % DoomDelves.SHOCKWAVE_GAP == 0 && shockwavesLeft > 0) {
                shockwavesLeft--;
                BossFx.sound(boss, SoundEvents.GENERIC_EXPLODE.value(), 0.5f);
                BossFx.ring(BossFx.world(boss), boss.position(), 10, ParticleTypes.EXPLOSION);
                for (ServerPlayer p : near) {
                    if (!inEarthenShield(p.position())) {
                        CombatHooks.nameAttack("shockwave", AttackType.MAGIC);
                        CombatHandler.applyDamage(boss, p, DoomDelves.SHOCKWAVE_MIN
                                + boss.getRandom().nextInt(DoomDelves.SHOCKWAVE_MAX - DoomDelves.SHOCKWAVE_MIN + 1));
                    }
                }
                if (shockwavesLeft <= 0) {
                    for (ServerPlayer p : near) {
                        if (cleared >= DoomDelves.MOPPING_UP_SPLATS) {
                            CombatAchievementService.flag(p, DoomDelves.FLAG_MOPPING_UP);
                        }
                    }
                    for (Mob e : earth) {
                        e.discard();
                    }
                    earth.clear();
                    earthenShield = null;
                    shockwaveDue = -1;
                    nextShockwave = now + DoomDelves.SHOCKWAVE_INTERVAL;
                    if (level >= DoomDelves.BURROW_FROM && shieldsDone > 0) {
                        loopShield = true;
                        attacksSinceShield = 0;
                    }
                }
            }
        }

        /**
         * Whether a player stands in the earthen shield: within its reach of where it stands now or anywhere along the step it
         * just took. // playability: OSRS moves it tile by tile with the player on the same tick; here it jumps once a game tick
         * while the player walks smoothly beside its drawn (sliding) ring
         */
        private boolean inEarthenShield(Vec3 pos) {
            if (earthenShield == null) {
                return false;
            }
            Vec3 a = earthenPrev == null ? earthenShield : earthenPrev;
            double abx = earthenShield.x - a.x;
            double abz = earthenShield.z - a.z;
            double len = abx * abx + abz * abz;
            double t = len < 1e-9 ? 0 : Math.max(0, Math.min(1, ((pos.x - a.x) * abx + (pos.z - a.z) * abz) / len));
            return Math.hypot(pos.x - (a.x + abx * t), pos.z - (a.z + abz * t)) <= DoomDelves.EARTHEN_SHIELD_BLOCKS;
        }

        /**
         * Every Minecraft tick: from the countdown to the last shockwave, a ring closing on the floor around the Doom (orange, bright
         * as a wave lands) and the earthen shield's safe circle (green) sliding between its steps, redrawn every 4 MC.
         * // playability: OSRS shows the volatile earth and the shield only
         */
        @Override
        public void tick(DataBossEntity boss) {
            super.tick(boss);
            long mc = com.runecraft.action.ActionManager.mcTick();
            if (mc % com.runecraft.osrs.ThreatTiming.REDRAW_MC != 0 || shockwaveDue < 0) {
                return;
            }
            ServerLevel world = BossFx.world(boss);
            boolean countdown = com.runecraft.osrs.content.bosses.BossRealtime.shockwaveCountdown(now, shockwaveDue);
            if (countdown || (now >= shockwaveDue && shockwavesLeft > 0)) {
                double radius = countdown ? 4 + 2.0 * Math.max(0, shockwaveDue - now) : 10;
                com.runecraft.boss.ThreatCues.ring(world, boss.position(), Math.min(10, radius), com.runecraft.boss.ThreatCues.AREA_RGB,
                        null, !countdown || shockwaveDue - now <= 1);
            }
            if (earthenShield != null) {
                Vec3 from = earthenPrev == null ? earthenShield : earthenPrev;
                double t = Math.max(0, Math.min(1, (mc - earthenMovedMc) / (double) com.runecraft.action.ActionManager.MC_TICKS_PER_GAME_TICK));
                Vec3 at = from.add(earthenShield.subtract(from).scale(t));
                com.runecraft.boss.ThreatCues.ring(world, at, DoomDelves.EARTHEN_SHIELD_BLOCKS, 0x40FF60, null, true);
            }
        }

        /**
         * Round 43 floor markers, every game tick: each acid blood splat lime (the 0.8-block test in {@link #tickAcid}); from the
         * countdown to the last shockwave, a red ring closing on the Doom like the dust one; the blocks wholly inside the
         * earthen shield's circle cyan where it stands (added last, so it shows through the rest). Put back as they go, when the Doom dies or leaves, and when
         * the server stops. // playability: OSRS shows the splats, the volatile earth and the shield only
         */
        private void showFloor(DataBossEntity boss) {
            if (doomFloor == null) {
                doomFloor = new com.runecraft.boss.FloorMarkers.Layer(boss);
            }
            com.runecraft.boss.FloorMarkers.Shape shape = new com.runecraft.boss.FloorMarkers.Shape(BossFx.world(boss));
            for (Vec3 a : acid) {
                shape.disc(a, 0.8, com.runecraft.boss.HazardKind.ACID);
            }
            if (shockwaveDue >= 0) {
                boolean countdown = com.runecraft.osrs.content.bosses.BossRealtime.shockwaveCountdown(now, shockwaveDue);
                if (countdown || (now >= shockwaveDue && shockwavesLeft > 0)) {
                    double r = Math.min(10, countdown ? 4 + 2.0 * Math.max(0, shockwaveDue - now) : 10);
                    shape.ring(boss.position(), r - 1, r, com.runecraft.boss.HazardKind.STRIKE);
                }
                if (earthenShield != null) {
                    shape.discInside(earthenShield, DoomDelves.EARTHEN_SHIELD_BLOCKS, com.runecraft.boss.HazardKind.SAFE);
                }
            }
            doomFloor.show(shape);
        }

        /** Self-test: the floor blocks the Doom's acid, shockwave ring and earthen shield mark now. */
        int walkFloorMarked() {
            return doomFloor == null ? 0 : doomFloor.size();
        }

        // ---------------- Acid blood ----------------

        private void tickAcid(DataBossEntity boss, List<ServerPlayer> near) {
            if (acid.isEmpty()) {
                return;
            }
            ServerLevel world = BossFx.world(boss);
            for (Vec3 a : acid) {
                world.sendParticles(ParticleTypes.SNEEZE, a.x, a.y + 0.1, a.z, 2, 0.4, 0.02, 0.4, 0.0);
            }
            for (ServerPlayer p : near) {
                for (Vec3 a : acid) {
                    if (p.position().distanceToSqr(a) < 0.8 * 0.8) {
                        CombatHooks.nameAttack("hazard", null);
                        CombatHandler.applyDamage(boss, p, 1 + boss.getRandom().nextInt(7));
                        PoisonService.venom(p, now);
                        break;
                    }
                }
            }
        }

        private void sprayAcid(DataBossEntity boss) {
            lastSplat = now;
            int splats = level >= 8 ? 4 : 2 + boss.getRandom().nextInt(2);
            for (int i = 0; i < splats && acid.size() < 80; i++) {
                double angle = boss.getRandom().nextDouble() * Math.PI * 2;
                double r = 2 + boss.getRandom().nextDouble() * 3;
                acid.add(BossFx.ground(BossFx.world(boss), boss.getX() + Math.cos(angle) * r, boss.getZ() + Math.sin(angle) * r, boss.getY()));
            }
            BossFx.burst(boss, ParticleTypes.SNEEZE, 12, 1.5);
        }

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (shield) {
                if (demonbane(attacker) && damage > 0) {
                    shieldCharge = 0; // demonbane resets the charge bar
                    damageShield(boss, damage * DoomDelves.SHIELD_DEMONBANE_FACTOR);
                } else {
                    Msg.action(attacker, "The demonic shield resists your attack!");
                }
                return 0;
            }
            if (burrowed) {
                burrowCharge = 0; // any hit resets the burrowed charge
                return damage;
            }
            int d = damage;
            boolean punished = false;
            if (beamTicks > 0 && type != null && type.isMelee()) {
                int str = Equipment.total(attacker, Players.data(attacker).skills()).meleeStrength();
                d += DoomDelves.punishBonus(str);
                beamTicks = 0;
                cooldown = Math.max(cooldown, DoomDelves.punishDelay(level));
                exposedUntil = now + 2;
                punished = true;
                Msg.game(attacker, "You punish the Doom of Mokhaiotl, interrupting its beam!");
                BossFx.burst(boss, ParticleTypes.CRIT, 15, 1.0);
            }
            boolean exposed = beamTicks > 0 || now <= exposedUntil;
            if (exposed && d >= boss.getHealth()) {
                CombatAchievementService.flag(attacker, DoomDelves.FLAG_EXPOSED);
                if (punished && level <= DoomDelves.HOLY_WATER_UNTIL) {
                    holyWater = true;
                }
            }
            if (level >= DoomDelves.ACID_FROM && d > 0 && now - lastSplat >= 3) { // approx: at most one spray every 3 ticks
                sprayAcid(boss);
            }
            return d;
        }

        /** Holy water from a kill by a melee punish: the killer is restored and acid blood around the Doom is washed away. */
        private void scatterHolyWater(DataBossEntity boss, ServerPlayer killer) {
            killer.heal(DoomDelves.HOLY_WATER_HP);
            PlayerData data = Players.data(killer);
            data.setPrayerPoints(data.prayerPoints() + DoomDelves.HOLY_WATER_PRAYER);
            com.runecraft.network.ServerNetworking.sync(killer);
            SpecialAttackService.restoreEnergy(killer, DoomDelves.HOLY_WATER_SPEC);
            for (int i = 0; i < 6; i++) {
                Vec3 at = around(boss, boss.position(), 8);
                BossFx.world(boss).sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.2, at.z, 12, 1.0, 0.1, 1.0, 0);
                acid.removeIf(a -> Math.abs(a.x - at.x) <= 3 && Math.abs(a.z - at.z) <= 3);
            }
            Msg.game(killer, "Holy water scatters from the Doom, restoring you.");
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayer killer) {
            if (doomFloor != null) {
                doomFloor.clear();
            }
            for (Larva l : larvae) {
                l.mob().discard();
            }
            larvae.clear();
            for (Mob e : earth) {
                e.discard();
            }
            earth.clear();
            if (killer != null && holyWater) {
                scatterHolyWater(boss, killer);
            }
            if (killer != null && !acid.isEmpty()) {
                ACID.put(killer.getUUID(), new ArrayList<>(acid));
            }
        }

        // ---------------- self-test access (DoomWalk) ----------------

        int walkLevel() {
            return level;
        }

        boolean walkCharging() {
            return beamTicks > 0;
        }

        void walkStartBeam(DataBossEntity boss, ServerPlayer target) {
            startBeam(boss, target);
        }

        boolean walkShielded() {
            return shield;
        }

        int walkShieldPoints() {
            return shieldPoints;
        }

        int walkShieldCharge() {
            return shieldCharge;
        }

        void walkStartShield(DataBossEntity boss) {
            startShield(boss);
        }

        boolean walkBurrowed() {
            return burrowed;
        }

        Vec3 walkEye() {
            return zoom == Zoom.WINDUP ? eye : null;
        }

        boolean walkSlamming() {
            return zoom == Zoom.SLAM;
        }

        List<Vec3> walkRocks() {
            return rocks;
        }

        boolean walkShockwavePending() {
            return shockwaveDue >= 0;
        }

        void walkStartShockwave(DataBossEntity boss) {
            startShockwave(boss);
        }

        List<Mob> walkEarth() {
            return earth;
        }

        Vec3 walkEarthenShield() {
            return earthenShield;
        }

        List<Mob> walkLarvae() {
            List<Mob> out = new ArrayList<>();
            for (Larva l : larvae) {
                out.add(l.mob());
            }
            return out;
        }

        void walkSpawnLarva(DataBossEntity boss, ServerPlayer target, boolean giant) {
            spawnLarva(boss, target, giant);
        }

        int walkAcid() {
            return acid.size();
        }

        void walkSprayAcid(DataBossEntity boss) {
            sprayAcid(boss);
        }

        boolean walkRockThrowInFlight() {
            return !throwsInFlight.isEmpty();
        }

        void walkThrowRock(DataBossEntity boss, ServerPlayer target) {
            throwRock(boss, target);
        }

        List<Vec3> walkShadows() {
            List<Vec3> out = new ArrayList<>();
            for (RockThrow t : throwsInFlight) {
                if (t.landsAt >= 0 && now < t.landsAt) {
                    out.addAll(t.shadows);
                }
            }
            return out;
        }

        boolean walkLoopShield() {
            return loopShield;
        }

        /** Game ticks until the first shockwave lands (-1: no volatile earth up). */
        long walkShockwaveTicksLeft() {
            return shockwaveDue < 0 ? -1 : shockwaveDue - now;
        }

        int walkShockwavesLeft() {
            return shockwaveDue < 0 ? 0 : shockwavesLeft;
        }
    }

    private static double flat(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
