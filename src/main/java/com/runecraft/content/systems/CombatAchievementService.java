package com.runecraft.content.systems;

import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.MonsterStats;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.content.raids.RaidInstance;
import com.runecraft.content.raids.RaidManager;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.raids.Invocation;
import com.runecraft.osrs.content.raids.RaidScaling;
import com.runecraft.osrs.content.systems.CombatAchievementRules;
import com.runecraft.osrs.content.systems.CombatAchievementRules.Hit;
import com.runecraft.osrs.content.systems.CombatAchievementRules.Kill;
import com.runecraft.osrs.content.systems.CombatAchievementRules.Metrics;
import com.runecraft.osrs.content.systems.CombatAchievements;
import com.runecraft.osrs.content.systems.CombatAchievements.Task;
import com.runecraft.osrs.content.systems.CombatAchievements.Tier;
import com.runecraft.osrs.content.systems.CombatAchievements.Track;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Tracks Combat Achievement tasks: kill counts (bosses and minigame completions) are polled, regular monster kills
 * come from the kill event, and fight tasks (speed, perfection, restrictions) are measured over a fight with one
 * boss or monster that starts with the first hit either way and ends with its kill. The same measurements are kept
 * over whole raids and minigame runs (Fight Caves, Inferno, Tempoross...), and kill streaks, kills within a time and
 * average kill times are kept per task.
 */
public final class CombatAchievementService {
    /** A fight, raid or minigame run in progress and what has been measured so far. */
    private static final class Tracker {
        final String key;
        final long start;
        long last;
        int kcAtStart;
        final Metrics m = new Metrics();
        float health;
        int prayer;
        /** Damage named by the damage hook since the last game tick (anything beyond it was scripted). */
        int attributed;
        Map<String, Integer> food;
        int doses = -1;
        String equipment;
        RaidInstance raid;
        int raidDeathsAtStart;
        int myRaidDeathsAtStart;
        boolean credited;
        /** The minigame session of a run (it reports the challenge played and conditions it judged itself). */
        MinigameSession session;
        /** Delve runs: the fight key of the boss being delved (its hits count as the target's). */
        String target;
        /** Where the player stood last tick (tiles walked). */
        Vec3 pos;
        /** Fights: the creature first engaged and where it stood then ("without the boss ever moving"). */
        LivingEntity targetEntity;
        Vec3 targetStart;
        /** Fights: the game tick of the player's latest attack still to be compared with the target's attack tick, and on whom. */
        long pendingAttackTick = -1;
        int pendingAttackMob;

        Tracker(String key, long now) {
            this.key = key;
            this.start = now;
            this.last = now;
        }

        boolean fight() {
            return !key.startsWith("mg:") && !key.equals("raid") && !key.startsWith("delve:") && !key.startsWith("run:");
        }
    }

    /** A kill streak of one task: kills so far and where the last one happened. */
    private static final class Streak {
        int count;
        Vec3 anchor;
        String world;
    }

    /** "Without leaving": a streak breaks when the player moves this far from where the last kill happened. */
    private static final double STREAK_RANGE = 64; // approx: OSRS checks the instance or room
    /** Players this close to a fight count towards its group size. */
    private static final double GROUP_RANGE = 24;

    private static final Map<UUID, Tracker> FIGHTS = new HashMap<>();
    private static final Map<UUID, Tracker> RUNS = new HashMap<>();
    /** Doom of Mokhaiotl delve runs: from the delve 1 fight on, judged at every level completed. */
    private static final Map<UUID, Tracker> DELVES = new HashMap<>();
    /** Barrows runs: from the first brother fought to the chest being looted (in memory: a relog starts a new one). */
    private static final Map<UUID, Tracker> BARROWS = new HashMap<>();
    /** A Barrows run with no brother fought for this long (30 minutes, approx) is dropped. */
    private static final long BARROWS_RUN_TIMEOUT_TICKS = 3000;
    /** Game tick of each monster's latest attack (keyed by entity id), for "attack on the same tick" conditions. */
    private static final Map<Integer, Long> LAST_ATTACK_TICK = new HashMap<>();
    /** Attack key of the latest damaging hit each player took (what a Vengeance recoil reflects). */
    private static final Map<UUID, String> LAST_HIT = new HashMap<>();
    /** Recent crossbow/ballista hits on tormented demons per player: {entity id, game tick}. */
    private static final Map<UUID, List<long[]>> TD_HITS = new HashMap<>();
    /** "Rapid Reload": three demons within 3 seconds (5 game ticks). */
    private static final int RAPID_RELOAD_TICKS = 5;
    private static final Map<UUID, Map<String, Streak>> STREAKS = new HashMap<>();
    private static final Map<UUID, Map<String, List<Kill>>> WINDOWS = new HashMap<>();
    private static final Map<UUID, Map<String, List<Long>>> DURATIONS = new HashMap<>();
    /** Recent kills (fight key, game tick): kills just before a boss fight count towards it (Kraken's tentacles). */
    private static final Map<UUID, List<Kill>> RECENT = new HashMap<>();

    private CombatAchievementService() {
    }

    public static void register() {
        CombatHooks.addModifier(new CombatHooks.PlayerAttackModifier() {
            @Override
            public int afterRoll(ServerPlayer player, LivingEntity target, AttackType type, int damage) {
                onAttack(player, target, type, damage);
                return damage;
            }
        });
        CombatHooks.addDamageListener(CombatAchievementService::onDamaged);
        com.runecraft.boss.Mechanics.attackListener = CombatAchievementService::bossAttack;
        com.runecraft.content.GameEvents.onKill(CombatAchievementService::onKill);
        SysTicks.everyPlayer(CombatAchievementService::tick);
    }

    /**
     * A boss script reports something the fight's tasks check with {@code flag=}/{@code noflag=} (a grub absorbed, a
     * kill during an exposed phase...): it goes on the player's current fight and, on a delve run, on the run.
     */
    public static void flag(ServerPlayer p, String flag) {
        for (Tracker t : trackers(p)) {
            t.m.flags.add(flag);
        }
    }

    /** Takes a flag back (a condition that only holds for a while, e.g. the Giant Mole having just resurfaced). */
    public static void unflag(ServerPlayer p, String flag) {
        for (Tracker t : trackers(p)) {
            t.m.flags.remove(flag);
        }
    }

    /** Counts something a script observed for the player's fight and run (burrows, souls negated...). */
    public static void count(ServerPlayer p, String key, int n) {
        for (Tracker t : trackers(p)) {
            t.m.count(key, n);
        }
    }

    /** Range within which players count as part of a boss's fight for team-wide flags. */
    private static final double SCRIPT_RANGE = 32;

    private static List<ServerPlayer> near(LivingEntity source) {
        if (!(source.level() instanceof net.minecraft.server.level.ServerLevel world)) {
            return List.of();
        }
        return world.getPlayers(o -> o.isAlive() && !o.isSpectator() && o.distanceToSqr(source) < SCRIPT_RANGE * SCRIPT_RANGE);
    }

    /** {@link #flag} for every player fighting near a boss ("without anyone..." conditions). */
    public static void flagNear(LivingEntity source, String flag) {
        for (ServerPlayer p : near(source)) {
            flag(p, flag);
        }
    }

    public static void unflagNear(LivingEntity source, String flag) {
        for (ServerPlayer p : near(source)) {
            unflag(p, flag);
        }
    }

    public static void countNear(LivingEntity source, String key) {
        for (ServerPlayer p : near(source)) {
            count(p, key, 1);
        }
    }

    /**
     * Whether the player's fight against {@code target} (still running: the kill's loot is rolled before the fight is
     * judged) meets a rule-language condition, e.g. a perfect kill's {@code nodmg=...}. False when no such fight is tracked.
     */
    public static boolean fightMeets(ServerPlayer p, LivingEntity target, String rule) {
        Tracker f = FIGHTS.get(p.getUUID());
        String key = fightKey(target);
        return f != null && key != null && key.equals(f.key) && CombatAchievementRules.meets(rule, f.m);
    }

    /** Attack key of the latest damaging hit the player took ("mechanic" when it had no name). */
    public static String lastHitAttack(ServerPlayer p) {
        return LAST_HIT.getOrDefault(p.getUUID(), "mechanic");
    }

    /** The last attack of each monster, for "the same attack twice in a row" conditions (keyed by entity id). */
    private static final Map<Integer, String> LAST_ATTACK = new HashMap<>();

    /**
     * A monster or boss used an attack (hit or not): counted as "used:" + attack for everyone fighting it, and flagged
     * "repeat:" + attack when it used the same attack twice in a row.
     */
    private static void bossAttack(OsrsMonsterEntity mob, ServerPlayer target, com.runecraft.osrs.content.AttackDef attack) {
        if (attack == null || attack.name() == null) {
            return;
        }
        String key = CombatAchievementRules.attackKey(attack.name());
        String prev = LAST_ATTACK.put(mob.getId(), key);
        if (LAST_ATTACK.size() > 512) {
            LAST_ATTACK.clear();
        }
        LAST_ATTACK_TICK.put(mob.getId(), SysTicks.now());
        if (LAST_ATTACK_TICK.size() > 512) {
            LAST_ATTACK_TICK.clear();
        }
        String fight = fightKey(mob);
        if (target != null) {
            engage(target, mob);
        }
        for (ServerPlayer p : near(mob)) {
            Tracker f = FIGHTS.get(p.getUUID());
            if (f == null || fight == null || !f.key.equals(fight)) {
                continue;
            }
            f.m.count("used:" + key, 1);
            if (key.equals(prev)) {
                f.m.flags.add("repeat:" + key);
            }
        }
    }

    // ------------------------------------------------------------------ fights

    /** Boss id of an entity (secondary forms included), or null for a regular monster. */
    static String bossId(LivingEntity e) {
        if (e instanceof com.runecraft.boss.DataBossEntity b && b.def() != null) {
            return b.def().id;
        }
        if (e instanceof com.runecraft.entity.BossEntity b && b.boss() != null) {
            return b.boss().entityId();
        }
        return null;
    }

    private static String fightKey(LivingEntity e) {
        if (e == null || e instanceof ServerPlayer) {
            return null;
        }
        String boss = bossId(e);
        if (boss != null) {
            return CombatAchievements.fightKey(boss, null);
        }
        MonsterStats stats = MonsterStats.of(e);
        if (stats.profile() instanceof BossDef bd) {
            return CombatAchievements.fightKey(bd.id, null);
        }
        if (stats.boss() != null) {
            return CombatAchievements.fightKey(stats.boss().entityId(), null);
        }
        return CombatAchievements.fightKey(null, stats.name());
    }

    private static String nameOf(LivingEntity e) {
        if (e == null) {
            return "";
        }
        if (e instanceof ServerPlayer pl) {
            return pl.getName().getString().toLowerCase(Locale.ROOT);
        }
        return MonsterStats.of(e).name().toLowerCase(Locale.ROOT);
    }

    /** Starts or continues the fight with an entity; a different opponent starts a new fight. */
    private static Tracker engage(ServerPlayer p, LivingEntity other) {
        String key = fightKey(other);
        if (key == null) {
            return null;
        }
        long now = SysTicks.now();
        if (key.equals("barrows_brother")) {
            Tracker run = BARROWS.get(p.getUUID());
            if (run == null) {
                run = new Tracker(CombatAchievements.BARROWS_RUN, now);
                run.target = key;
                run.kcAtStart = Players.data(p).killCount("barrows_chests");
                start(p, run);
                BARROWS.put(p.getUUID(), run);
            }
            run.last = now;
        }
        Tracker f = FIGHTS.get(p.getUUID());
        if (f != null && !f.key.equals(key) && !f.key.startsWith("kill:")) {
            // A boss fight carries on while its minions, bodyguards or spawns join in.
            f.last = now;
            f.m.struck.add(key);
            return null;
        }
        if (f == null || !f.key.equals(key)) {
            f = new Tracker(key, now);
            f.kcAtStart = bossKc(p, key);
            f.targetEntity = other;
            f.targetStart = other.position();
            start(p, f);
            for (Kill k : RECENT.getOrDefault(p.getUUID(), List.of())) {
                if (now - k.end() <= CombatAchievements.FIGHT_TIMEOUT_TICKS) {
                    f.m.killed(k.key());
                }
            }
            f.m.startPlayers = playersNear(p);
            f.m.maxPlayers = f.m.startPlayers;
            if (other instanceof com.runecraft.boss.DataBossEntity boss) {
                f.m.awakened = boss.awakened();
                f.m.delve = boss.delve();
                if (boss.delve() == 1) {
                    Tracker run = new Tracker("delve:" + key, now);
                    run.target = key;
                    start(p, run);
                    DELVES.put(p.getUUID(), run);
                }
            }
            RaidInstance raid = RaidManager.of(p);
            if (raid != null) {
                f.raid = raid;
                f.raidDeathsAtStart = raid.totalDeaths();
                f.myRaidDeathsAtStart = raid.deaths(p.getUUID());
            }
            FIGHTS.put(p.getUUID(), f);
        }
        f.last = now;
        return f;
    }

    /** The starting state of a fight or run. */
    private static void start(ServerPlayer p, Tracker t) {
        PlayerData d = Players.data(p);
        t.prayer = d.prayerPoints();
        if (t.prayer > 0) {
            t.m.flags.add(CombatAchievements.FLAG_HAD_PRAYER);
        }
        t.health = p.getHealth();
        t.m.maxHp = (int) Math.ceil(p.getHealth());
        t.m.minHp = t.m.maxHp;
        t.m.book = SysState.of(p).book.name();
        t.pos = p.position();
        t.m.gear(worn(p));
        for (Prayer pr : d.activePrayers()) {
            t.m.prayersEver.add(pr.name());
        }
    }

    private static int playersNear(ServerPlayer p) {
        return p.serverLevel().getPlayers(o -> o.isAlive() && !o.isSpectator() && o.distanceToSqr(p) < GROUP_RANGE * GROUP_RANGE).size();
    }

    private static boolean protectedFrom(ServerPlayer p, AttackType style) {
        PlayerData d = Players.data(p);
        if (style == null) {
            Set<Prayer> active = d.activePrayers();
            return active.contains(Prayer.PROTECT_FROM_MELEE) || active.contains(Prayer.PROTECT_FROM_MAGIC)
                    || active.contains(Prayer.PROTECT_FROM_MISSILES);
        }
        return switch (style) {
            case MAGIC -> d.isActive(Prayer.PROTECT_FROM_MAGIC);
            case RANGED -> d.isActive(Prayer.PROTECT_FROM_MISSILES);
            default -> d.isActive(Prayer.PROTECT_FROM_MELEE);
        };
    }

    /** Kill count of everything that counts as a fight key's boss (main id plus forms/partners). */
    private static int bossKc(ServerPlayer p, String key) {
        if (key.startsWith("kill:")) {
            return 0;
        }
        int kc = 0;
        for (Map.Entry<String, Integer> e : Players.data(p).killCounts().entrySet()) {
            if (CombatAchievements.canonical(e.getKey()).equals(key)) {
                kc += e.getValue();
            }
        }
        return kc;
    }

    /** The spell behind a magic attack: this module's autocast, else the standard-book autocast. */
    private static String spellOf(ServerPlayer p) {
        String auto = SysState.of(p).autocast;
        if (auto != null && !auto.isEmpty()) {
            return auto;
        }
        com.runecraft.osrs.Spell s = Players.data(p).autocast();
        return s == null ? null : s.name().toLowerCase(Locale.ROOT);
    }

    /** One attack by the player (hit or miss). */
    private static void onAttack(ServerPlayer p, LivingEntity target, AttackType type, int damage) {
        String weapon = p.getMainHandItem().isEmpty() ? "" : Inv.idOf(p.getMainHandItem());
        boolean spec = SpecialAttackService.active(p) != null;
        String spell = type == AttackType.MAGIC ? spellOf(p) : null;
        Tracker f = engage(p, target);
        unflag(p, CombatAchievements.FLAG_VENGEANCE_LAST);
        if (f != null) {
            record(f.m, type, weapon, spell, spec);
            f.m.targetAttacks++;
            if (spec) {
                f.m.specAttacks++;
            }
            if (damage > 0) {
                f.m.targetHits++;
            }
            f.pendingAttackTick = SysTicks.now();
            f.pendingAttackMob = target.getId();
            if (damage > 0 && f.key.equals("kill:tormented demon") && (weapon.contains("crossbow") || weapon.contains("ballista"))) {
                rapidReload(p, f, target);
            }
        }
        Tracker run = RUNS.get(p.getUUID());
        if (run != null) {
            record(run.m, type, weapon, spell, spec);
        }
        Tracker delve = DELVES.get(p.getUUID());
        if (delve != null && delve.target.equals(fightKey(target))) {
            record(delve.m, type, weapon, spell, spec);
        }
    }

    /** "Rapid Reload": a damaging crossbow/ballista hit on a tormented demon; three different demons within 3 seconds flag the fight. */
    private static void rapidReload(ServerPlayer p, Tracker f, LivingEntity target) {
        long now = SysTicks.now();
        List<long[]> hits = TD_HITS.computeIfAbsent(p.getUUID(), u -> new ArrayList<>());
        hits.removeIf(h -> now - h[1] >= RAPID_RELOAD_TICKS);
        hits.add(new long[]{target.getId(), now});
        Set<Long> demons = new HashSet<>();
        for (long[] h : hits) {
            demons.add(h[0]);
        }
        if (demons.size() >= 3) {
            f.m.flags.add(CombatAchievements.FLAG_RAPID_RELOAD);
        }
    }

    private static void record(Metrics m, AttackType type, String weapon, String spell, boolean spec) {
        m.styles.add(type);
        m.weapons.add(weapon);
        m.lastWeapon = weapon;
        m.lastSpec = spec;
        m.lastSpell = spell;
        if (spell != null) {
            m.spells.add(spell);
        }
    }

    /** Any damage roll against the player, with the attack's name when the game knows it. */
    private static void onDamaged(ServerPlayer p, LivingEntity attacker, String attack, AttackType style, int damage) {
        if (attacker != null && !(attacker instanceof ServerPlayer)) {
            engage(p, attacker);
        }
        String attackerKey = fightKey(attacker);
        String attackerName = nameOf(attacker);
        boolean prayed = protectedFrom(p, style);
        if (damage > 0) {
            LAST_HIT.put(p.getUUID(), CombatAchievementRules.attackKey(attack));
        }
        for (Tracker t : trackers(p)) {
            boolean fromTarget = t.fight() ? t.key.equals(attackerKey) : t.target != null && t.target.equals(attackerKey);
            if (attacker != null) {
                t.m.attackers.add(attackerName);
                if (fromTarget) {
                    t.m.targetAttacked = true;
                }
            }
            if (damage > 0) {
                t.attributed += damage;
                t.m.took(new Hit(fromTarget, attackerName, attack, style, damage, prayed));
            }
        }
    }

    private static List<Tracker> trackers(ServerPlayer p) {
        List<Tracker> list = new ArrayList<>(2);
        Tracker f = FIGHTS.get(p.getUUID());
        if (f != null) {
            list.add(f);
        }
        Tracker r = RUNS.get(p.getUUID());
        if (r != null) {
            list.add(r);
        }
        Tracker d = DELVES.get(p.getUUID());
        if (d != null) {
            list.add(d);
        }
        Tracker b = BARROWS.get(p.getUUID());
        if (b != null) {
            list.add(b);
        }
        return list;
    }

    // ------------------------------------------------------------------ per-tick measurements

    private static void tick(ServerPlayer p, long tick) {
        if (!p.isAlive()) {
            FIGHTS.remove(p.getUUID());
            STREAKS.remove(p.getUUID());
            DELVES.remove(p.getUUID());
            BARROWS.remove(p.getUUID());
            return;
        }
        Tracker f = FIGHTS.get(p.getUUID());
        if (f != null && tick - f.last > CombatAchievements.FIGHT_TIMEOUT_TICKS) {
            FIGHTS.remove(p.getUUID());
            f = null;
        }
        if (f != null) {
            tickFight(f, tick);
        }
        tickRun(p);
        Tracker barrows = BARROWS.get(p.getUUID());
        if (barrows != null && Players.data(p).killCount("barrows_chests") > barrows.kcAtStart) {
            BARROWS.remove(p.getUUID());
            completeRun(p, barrows);
        } else if (barrows != null && tick - barrows.last > BARROWS_RUN_TIMEOUT_TICKS) {
            BARROWS.remove(p.getUUID()); // an abandoned run: the next brother fought starts a new one
        }
        if (com.runecraft.combat.PoisonService.isVenomImmune(p, tick) || com.runecraft.combat.PoisonService.isPoisonImmune(p, tick)) {
            for (Tracker t : trackers(p)) {
                t.m.flags.add(CombatAchievements.FLAG_TOXIN_IMMUNE);
            }
        }
        List<Tracker> active = trackers(p);
        if (!active.isEmpty()) {
            Snapshot s = snapshot(p);
            int near = playersNear(p);
            for (Tracker t : active) {
                measure(p, t, s);
                if (t == f || t.raid == null) {
                    t.m.maxPlayers = Math.max(t.m.maxPlayers, near);
                }
            }
        }
        f = FIGHTS.get(p.getUUID());
        if (f != null && f.fight() && !f.key.startsWith("kill:") && bossKc(p, f.key) > f.kcAtStart) {
            finish(p, f, null);
        }
        checkStreakRange(p);
        if ((tick + p.getId()) % 20 == 0) {
            checkKillCounts(p);
        }
    }

    /**
     * Per-tick fight checks: an attack from the previous tick that the target didn't answer on the same tick ("Mirror Image"),
     * and the target having moved more than a tile (two blocks) from where the fight started.
     */
    private static void tickFight(Tracker f, long tick) {
        if (f.pendingAttackTick >= 0 && tick > f.pendingAttackTick) {
            Long at = LAST_ATTACK_TICK.get(f.pendingAttackMob);
            if (at == null || at != f.pendingAttackTick) {
                f.m.flags.add(CombatAchievements.FLAG_OFF_BEAT);
            }
            f.pendingAttackTick = -1;
        }
        LivingEntity target = f.targetEntity;
        if (target != null && target.isAlive() && f.targetStart != null && target.position().distanceToSqr(f.targetStart) > 4.0) {
            f.m.flags.add(CombatAchievements.FLAG_TARGET_MOVED);
        }
    }

    /** What the player carries and wears this tick. */
    private record Snapshot(Map<String, Integer> food, int doses, String equipment, Set<String> prayers, String book, boolean running,
                            float health, int prayer) {
    }

    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

    private static Snapshot snapshot(ServerPlayer p) {
        Map<String, Integer> food = new HashMap<>();
        int doses = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (st.isEmpty()) {
                continue;
            }
            String id = Inv.idOf(st);
            ItemDef def = ItemCatalog.get(id);
            if (def == null) {
                continue;
            }
            if (def.kind() == ItemDef.Kind.FOOD) {
                food.merge(id, st.getCount(), Integer::sum);
            } else if (def.kind() == ItemDef.Kind.POTION) {
                doses += st.getCount() * Math.max(1, 4 - st.getDamageValue());
            }
        }
        Set<String> prayers = new HashSet<>();
        for (Prayer pr : Players.data(p).activePrayers()) {
            prayers.add(pr.name());
        }
        return new Snapshot(food, doses, String.join(",", worn(p)), prayers, SysState.of(p).book.name(), p.isSprinting(), p.getHealth(),
                Players.data(p).prayerPoints());
    }

    /** Item ids of everything worn: armour and hands, plus the jewellery slots. */
    private static List<String> worn(ServerPlayer p) {
        List<String> ids = new ArrayList<>();
        for (EquipmentSlot slot : SLOTS) {
            ItemStack st = p.getItemBySlot(slot);
            ids.add(st.isEmpty() ? "" : Inv.idOf(st));
        }
        SysState.SysPlayer sp = SysState.of(p);
        for (Jewellery.Slot slot : Jewellery.Slot.values()) {
            ids.add(sp.wornId(slot));
        }
        return ids;
    }

    private static void measure(ServerPlayer p, Tracker t, Snapshot s) {
        Metrics m = t.m;
        // Damage from anything (scripted specials, hazards nobody named) counts, not just hits the damage hook saw.
        float drop = t.health - s.health() - t.attributed;
        if (drop > 0.5f) {
            m.took(new Hit(false, "", null, null, Math.round(drop), protectedFrom(p, null)));
        }
        t.attributed = 0;
        t.health = s.health();
        int hp = (int) Math.ceil(s.health());
        m.maxHp = Math.max(m.maxHp, hp);
        m.minHp = Math.min(m.minHp, hp);
        if (s.prayer() < t.prayer) {
            m.prayerLost = true;
        } else if (s.prayer() > t.prayer) {
            m.prayerRestored = true;
        }
        t.prayer = s.prayer();
        if (s.prayer() > 0) {
            m.flags.add(CombatAchievements.FLAG_HAD_PRAYER);
        }
        if (t.food != null) {
            for (Map.Entry<String, Integer> e : t.food.entrySet()) {
                if (s.food().getOrDefault(e.getKey(), 0) < e.getValue()) {
                    m.eaten.add(e.getKey()); // approx: dropping food counts as eating it
                }
            }
            if (s.doses() < t.doses) {
                m.drank = true;
            }
            if (!s.equipment().equals(t.equipment)) {
                m.equipChanged = true;
            }
        }
        if (!s.equipment().equals(t.equipment)) {
            m.gear(List.of(s.equipment().split(",", -1)));
        }
        t.food = s.food();
        t.doses = s.doses();
        t.equipment = s.equipment();
        Vec3 at = p.position();
        if (t.pos != null) {
            double step = Math.sqrt(Math.pow(at.x - t.pos.x, 2) + Math.pow(at.z - t.pos.z, 2));
            if (step < 8) { // teleports and knockbacks across the arena aren't walking
                m.moved += step / 2;
            }
        }
        t.pos = at;
        m.prayersEver.addAll(s.prayers());
        if (s.running()) {
            m.ran = true;
        }
        if (m.book != null && !m.book.equals(s.book())) {
            m.bookChanged = true;
        }
    }

    // ------------------------------------------------------------------ kills

    private static void onKill(ServerPlayer p, LivingEntity e, MonsterStats stats) {
        String name = (stats == null ? e.getName().getString() : stats.name()).toLowerCase(Locale.ROOT);
        SysState.SysPlayer sp = SysState.of(p);
        for (Task t : CombatAchievements.tasks()) {
            if (t.track() == Track.KILLS && !sp.caDone.contains(t.id()) && CombatAchievements.killMatches(t.target(), name)) {
                int n = sp.caCounters.merge(t.id(), 1, Integer::sum);
                if (n >= t.count()) {
                    sp.caCounters.remove(t.id());
                    complete(p, t);
                }
            }
        }
        String key = fightKey(e);
        if (key == null) {
            return;
        }
        long now = SysTicks.now();
        for (Tracker t : trackers(p)) {
            t.m.killed(key);
        }
        List<Kill> recent = RECENT.computeIfAbsent(p.getUUID(), u -> new ArrayList<>());
        recent.add(new Kill(key, now, now));
        if (recent.size() > 30) {
            recent.remove(0);
        }
        Tracker f = FIGHTS.get(p.getUUID());
        boolean mine = f != null && f.key.equals(key);
        windows(p, key, mine ? f.start : now, now, mine ? f.m : null);
        if (mine) {
            Metrics m = f.m;
            MobEffectInstance slow = e.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            m.frozen = slow != null && slow.getAmplifier() >= 6;
            m.othersAlive = !e.level().getEntitiesOfClass(OsrsMonsterEntity.class, e.getBoundingBox().inflate(16),
                    o -> o != e && o.isAlive()).isEmpty();
            String raw = bossId(e);
            m.killedId = raw != null ? raw : name;
            // Bosses finish their fight when their kill count goes up; that has happened by now.
            if (key.startsWith("kill:") || bossKc(p, key) > f.kcAtStart) {
                finish(p, f, e);
            }
        }
    }

    private static void finish(ServerPlayer p, Tracker f, LivingEntity killed) {
        FIGHTS.remove(p.getUUID());
        long now = SysTicks.now();
        Metrics m = f.m;
        m.ticks = now - f.start;
        m.prayersAtEnd.clear();
        for (Prayer pr : Players.data(p).activePrayers()) {
            m.prayersAtEnd.add(pr.name());
        }
        m.wornAtEnd.clear();
        m.wornAtEnd.addAll(worn(p));
        m.antifire = p.hasEffect(MobEffects.FIRE_RESISTANCE);
        if (f.raid != null) {
            m.raid = raidInfo(f.raid, p, f.raidDeathsAtStart, f.myRaidDeathsAtStart);
        }
        CombatAchievements.FightResult r = m.result();
        SysState.SysPlayer sp = SysState.of(p);
        if (m.delve > 0) {
            completeDelve(p, sp, m.delve, now);
        }
        List<Long> durations = DURATIONS.computeIfAbsent(p.getUUID(), u -> new HashMap<>()).computeIfAbsent(f.key, k -> new ArrayList<>());
        durations.add(m.ticks);
        if (durations.size() > 10) {
            durations.remove(0);
        }
        for (Task t : CombatAchievements.tasks()) {
            if (sp.caDone.contains(t.id())) {
                continue;
            }
            if (t.track().fight()) {
                if (f.key.equals(t.target()) && CombatAchievements.fightMeets(t, r)) {
                    complete(p, t);
                }
                continue;
            }
            if (!t.targets(f.key)) {
                continue;
            }
            switch (t.track()) {
                case RULE -> {
                    if (CombatAchievementRules.meets(t.rule(), m)) {
                        complete(p, t);
                    }
                }
                case STREAK -> {
                    Streak s = STREAKS.computeIfAbsent(p.getUUID(), u -> new HashMap<>()).computeIfAbsent(t.id(), k -> new Streak());
                    if (!CombatAchievementRules.meets(CombatAchievementRules.killFilter(t.rule()), m)) {
                        s.count = 0;
                    } else {
                        s.count++;
                    }
                    s.anchor = p.position();
                    s.world = p.level().dimension().location().toString();
                    if (s.count >= t.count()) {
                        STREAKS.get(p.getUUID()).remove(t.id());
                        complete(p, t);
                    }
                }
                case AVERAGE -> {
                    if (CombatAchievementRules.averageMet(durations, t.count(), CombatAchievementRules.timeLimit(t.rule()))) {
                        complete(p, t);
                    }
                }
                default -> {
                }
            }
        }
    }

    /** Kills for time-window tasks: each task keeps its own recent kills that met its per-kill conditions. */
    private static void windows(ServerPlayer p, String key, long start, long end, Metrics fight) {
        SysState.SysPlayer sp = SysState.of(p);
        for (Task t : CombatAchievements.tasks()) {
            if (t.track() != Track.WINDOW || sp.caDone.contains(t.id()) || !t.targets(key)) {
                continue;
            }
            String filter = CombatAchievementRules.killFilter(t.rule());
            if (!filter.isEmpty() && (fight == null || !CombatAchievementRules.meets(filter, fight))) {
                continue;
            }
            List<Kill> kills = WINDOWS.computeIfAbsent(p.getUUID(), u -> new HashMap<>()).computeIfAbsent(t.id(), k -> new ArrayList<>());
            kills.add(new Kill(key, start, end));
            if (kills.size() > 20) {
                kills.remove(0);
            }
            if (CombatAchievementRules.windowMet(kills, t.count(), CombatAchievementRules.timeLimit(t.rule()),
                    CombatAchievementRules.hasFlag(t.rule(), "distinct"), CombatAchievementRules.hasFlag(t.rule(), "fromstart"))) {
                complete(p, t);
            }
        }
    }

    /** "Without leaving": streaks end when the player goes elsewhere. */
    private static void checkStreakRange(ServerPlayer p) {
        Map<String, Streak> streaks = STREAKS.get(p.getUUID());
        if (streaks == null || streaks.isEmpty()) {
            return;
        }
        String world = p.level().dimension().location().toString();
        streaks.values().removeIf(s -> s.anchor == null || !world.equals(s.world)
                || s.anchor.distanceToSqr(p.position()) > STREAK_RANGE * STREAK_RANGE);
    }

    /** Progress of a counted task for the task list: kills, completions or the current streak. */
    static int progress(ServerPlayer p, Task t) {
        if (t.track() == Track.STREAK) {
            Map<String, Streak> streaks = STREAKS.get(p.getUUID());
            Streak s = streaks == null ? null : streaks.get(t.id());
            return s == null ? 0 : s.count;
        }
        return SysState.of(p).caCounters.getOrDefault(t.id(), 0);
    }

    // ------------------------------------------------------------------ raids and minigame runs

    private static CombatAchievementRules.Raid raidInfo(RaidInstance raid, ServerPlayer p, int deathsAtStart, int myDeathsAtStart) {
        Set<String> invocations = new HashSet<>();
        for (Invocation i : raid.invocations()) {
            invocations.add(i.name());
        }
        boolean withinTarget = raid.elapsedTicks() <= RaidScaling.targetTicks(raid.type, raid.mode(), raid.partySize());
        return new CombatAchievementRules.Raid(raid.type.name(), raid.mode().name(), raid.partySize(), raid.raidLevel(), invocations,
                raid.pathLevel(), raid.totalDeaths() - deathsAtStart, raid.deaths(p.getUUID()) - myDeathsAtStart, withinTarget,
                raid.points(p.getUUID()), raid.suppliesTaken());
    }

    /** Starts, ends and completes raid and minigame runs. */
    private static void tickRun(ServerPlayer p) {
        UUID u = p.getUUID();
        Tracker run = RUNS.get(u);
        RaidInstance raid = RaidManager.of(p);
        if (raid != null && raid.begun()) {
            if (run == null || run.raid != raid) {
                run = new Tracker("raid", SysTicks.now());
                run.raid = raid;
                start(p, run);
                RUNS.put(u, run);
            }
            if (raid.finished() && !run.credited) {
                run.credited = true;
                completeRaid(p, run);
            }
            return;
        }
        if (run != null && run.raid != null) {
            RUNS.remove(u);
            run = null;
        }
        MinigameSession session = MinigameManager.session(p);
        String key = session == null ? null : "mg:" + session.game.id();
        if (run != null) {
            if (kc(p, run.key) > run.kcAtStart) {
                RUNS.remove(u);
                completeRun(p, run);
                run = null;
            } else if (!run.key.equals(key)) {
                RUNS.remove(u);
                run = null;
            }
        }
        if (run == null && key != null) {
            run = new Tracker(key, SysTicks.now());
            run.session = session;
            run.kcAtStart = kc(p, key);
            start(p, run);
            RUNS.put(u, run);
        }
    }

    private static void completeRaid(ServerPlayer p, Tracker run) {
        RaidInstance raid = run.raid;
        Metrics m = run.m;
        m.ticks = raid.elapsedTicks();
        m.raid = raidInfo(raid, p, 0, 0);
        m.wornAtEnd.clear();
        m.wornAtEnd.addAll(worn(p));
        SysState.SysPlayer sp = SysState.of(p);
        for (Task t : CombatAchievements.tasks()) {
            if (t.track() == Track.RAID && !sp.caDone.contains(t.id()) && t.target().equals(raid.type.name())
                    && CombatAchievementRules.meets(t.rule(), m)) {
                counted(p, sp, t);
            }
        }
    }

    private static void completeRun(ServerPlayer p, Tracker run) {
        Metrics m = run.m;
        m.ticks = SysTicks.now() - run.start;
        if (run.session != null) {
            m.stage = run.session.caStage();
            m.flags.addAll(run.session.caFlags(p));
        }
        m.wornAtEnd.clear();
        m.wornAtEnd.addAll(worn(p));
        SysState.SysPlayer sp = SysState.of(p);
        for (Task t : CombatAchievements.tasks()) {
            if (t.track() == Track.RUN && !sp.caDone.contains(t.id()) && t.target().equals(run.key)
                    && CombatAchievementRules.meets(t.rule(), m)) {
                counted(p, sp, t);
            }
        }
    }

    /** A delve level was completed: judges the delve run so far (time from the delve 1 fight, conditions over every level). */
    private static void completeDelve(ServerPlayer p, SysState.SysPlayer sp, int level, long now) {
        Tracker run = DELVES.get(p.getUUID());
        if (run == null) {
            return;
        }
        Metrics m = run.m;
        m.delve = level;
        m.ticks = now - run.start;
        m.wornAtEnd.clear();
        m.wornAtEnd.addAll(worn(p));
        for (Task t : CombatAchievements.tasks()) {
            if (t.track() == Track.RUN && !sp.caDone.contains(t.id()) && t.target().equals(run.key)
                    && CombatAchievementRules.meets(t.rule(), m)) {
                counted(p, sp, t);
            }
        }
    }

    /** Completes a task, or counts towards it when it needs several qualifying completions. */
    private static void counted(ServerPlayer p, SysState.SysPlayer sp, Task t) {
        if (t.count() <= 1) {
            complete(p, t);
            return;
        }
        int n = sp.caCounters.merge(t.id(), 1, Integer::sum);
        if (n >= t.count()) {
            sp.caCounters.remove(t.id());
            complete(p, t);
        } else {
            Msg.game(p, "Combat task progress: " + t.name() + " (" + n + "/" + t.count() + ").");
        }
    }

    /** Kill count of one key: a boss id or "mg:" + minigame id (completions). */
    static int kc(ServerPlayer p, String key) {
        if (key.startsWith("mg:")) {
            return com.runecraft.content.minigames.MinigameData.of(p).stat(p.getUUID(), "kc:" + key.substring(3));
        }
        return Players.data(p).killCount(key);
    }

    static void checkKillCounts(ServerPlayer p) {
        SysState.SysPlayer sp = SysState.of(p);
        for (Task t : CombatAchievements.tasks()) {
            if (t.track() == Track.KC && !sp.caDone.contains(t.id()) && CombatAchievements.kcValue(t.target(), k -> kc(p, k)) >= t.count()) {
                complete(p, t);
            }
        }
    }

    // ------------------------------------------------------------------ completion and rewards

    public static int points(ServerPlayer p) {
        return CombatAchievements.points(SysState.of(p).caDone);
    }

    /** The highest tier the player's points unlock, or null. */
    public static Tier tier(ServerPlayer p) {
        return CombatAchievements.unlocked(points(p));
    }

    static void complete(ServerPlayer p, Task t) {
        SysState.SysPlayer sp = SysState.of(p);
        Tier before = CombatAchievements.unlocked(CombatAchievements.points(sp.caDone));
        if (!sp.caDone.add(t.id())) {
            return;
        }
        int points = CombatAchievements.points(sp.caDone);
        p.displayClientMessage(Component.literal("Congratulations, you've completed " + (t.tier() == Tier.EASY || t.tier() == Tier.ELITE ? "an " : "a ")
                + t.tier().id() + " combat task: " + t.name() + " (" + t.points() + " point" + (t.points() == 1 ? "" : "s")
                + "). Total points: " + points + ".").withStyle(ChatFormatting.DARK_GREEN), false);
        Tier after = CombatAchievements.unlocked(points);
        if (after != null && after != before) {
            p.displayClientMessage(Component.literal("You have unlocked the " + after.id() + " tier of Combat Achievement rewards! Claim them from Ghommal "
                    + "at the Warriors' Guild or the Combat Achievements menu.").withStyle(ChatFormatting.GOLD), false);
        }
    }

    public static boolean claimable(SysState.SysPlayer sp, Tier t) {
        return CombatAchievements.unlocked(CombatAchievements.points(sp.caDone), t) && !sp.caClaimed.contains(t.name());
    }

    /** Hands out a tier's rewards (or a replacement hilt). */
    public static void claim(ServerPlayer p, Tier t) {
        SysState.SysPlayer sp = SysState.of(p);
        int points = CombatAchievements.points(sp.caDone);
        if (!CombatAchievements.unlocked(points, t)) {
            Msg.game(p, "You need " + CombatAchievements.threshold(t) + " Combat Achievement points to claim the " + t.id()
                    + " rewards. You have " + points + ".");
            return;
        }
        if (sp.caClaimed.contains(t.name())) {
            if (Inv.has(p, t.hilt(), 1)) {
                Msg.game(p, "You already have your " + ItemCatalog.get(t.hilt()).name() + ".");
                return;
            }
            Inv.give(p, t.hilt(), 1);
            Msg.action(p, "Ghommal hands you a replacement " + ItemCatalog.get(t.hilt()).name() + ".");
            return;
        }
        sp.caClaimed.add(t.name());
        Inv.give(p, t.hilt(), 1);
        Inv.give(p, t.lamp(), 1);
        if (t == Tier.MASTER) {
            Inv.give(p, CombatAchievements.LUCKY_PENNY, 1);
        }
        Msg.action(p, "You receive " + ItemCatalog.get(t.hilt()).name() + (t == Tier.MASTER ? ", Ghommal's lucky penny" : "")
                + " and an antique lamp.");
    }

    public static void rubLamp(ServerPlayer p, Tier tier, Skill skill) {
        if (!Inv.has(p, tier.lamp(), 1)) {
            return;
        }
        if (Players.data(p).skills().level(skill) < tier.lampMinLevel) {
            Msg.game(p, "You need at least level " + tier.lampMinLevel + " " + skill.displayName() + " to use this lamp on it.");
            return;
        }
        Inv.remove(p, tier.lamp(), 1);
        com.runecraft.skill.XpService.add(p, skill, tier.lampXp);
        Msg.action(p, "The lamp grants you " + String.format(Locale.ROOT, "%,d", tier.lampXp) + " " + skill.displayName() + " experience.");
    }

    /** A hilt works up to the tier the player's points currently unlock (as in OSRS). */
    static int effectiveHilt(ServerPlayer p, String hiltId) {
        Tier unlocked = tier(p);
        return unlocked == null ? 0 : Math.min(CombatAchievements.hiltTier(hiltId), unlocked.ordinal() + 1);
    }

    static void hiltMenu(ServerPlayer p, String hiltId) {
        int hilt = effectiveHilt(p, hiltId);
        SysNet.Screen s = new SysNet.Screen("cahilt", ItemCatalog.get(hiltId) == null ? hiltId : ItemCatalog.get(hiltId).name());
        if (hilt == 0) {
            s.line("You need to have unlocked a tier of Combat Achievement rewards to use this item.", 0xFF8080);
        } else {
            List<CombatAchievements.HiltTele> teles = CombatAchievements.hiltTeleports(hilt);
            for (int i = 0; i < teles.size(); i++) {
                CombatAchievements.HiltTele t = teles.get(i);
                int perDay = t.perDay(hilt);
                s.button(hiltId + "|" + i, t.dest().name() + (perDay == 0 ? "" : " (" + DiaryService.dailyLeft(p, t.key(), perDay) + "/"
                        + perDay + " today)"), "", true);
            }
        }
        int tier = CombatAchievements.hiltTier(hiltId);
        if (tier >= 5) {
            boolean has = Inv.has(p, "avernic_defender", 1);
            s.button(hiltId + "|defender", "Attach to an avernic defender", has ? "" : "You need an avernic defender.", has && hilt >= tier);
        }
        for (String orn : new String[]{CombatAchievements.DHCB_KBD, CombatAchievements.DHCB_VORKATH}) {
            Tier need = CombatAchievements.dhcbOrnamentTier(orn);
            String head = CombatAchievements.dhcbHead(orn);
            if (hilt >= need.ordinal() + 1 && Inv.has(p, "dragon_hunter_crossbow", 1)) {
                boolean hasHead = Inv.has(p, head, 1);
                s.button(hiltId + "|" + orn, "Make a " + ItemCatalog.get(orn).name(), hasHead ? "" : "You need " + ItemCatalog.get(head).name() + ".",
                        hasHead);
            }
            if (Inv.has(p, orn, 1)) {
                s.button(hiltId + "|revert:" + orn, "Take the head off your " + ItemCatalog.get(orn).name(), "", true);
            }
        }
        s.send(p);
    }

    static void hiltAction(ServerPlayer p, String action, Map<String, String> fields) {
        int bar = action.lastIndexOf('|');
        if (bar < 0) {
            return;
        }
        String hiltId = action.substring(0, bar);
        if (!Inv.has(p, hiltId, 1)) {
            Msg.game(p, "You no longer have that hilt.");
            return;
        }
        int hilt = effectiveHilt(p, hiltId);
        String rest = action.substring(bar + 1);
        if (rest.startsWith("revert:")) {
            String orn = rest.substring(7);
            String head = CombatAchievements.dhcbHead(orn);
            if (head != null && Inv.has(p, orn, 1)) {
                Inv.remove(p, orn, 1);
                Inv.give(p, "dragon_hunter_crossbow", 1);
                Inv.give(p, head, 1);
                Msg.action(p, "You take the head off the crossbow.");
            }
            return;
        }
        Tier ornTier = CombatAchievements.dhcbOrnamentTier(rest);
        if (ornTier != null) {
            String head = CombatAchievements.dhcbHead(rest);
            if (hilt >= ornTier.ordinal() + 1 && Inv.has(p, "dragon_hunter_crossbow", 1) && Inv.has(p, head, 1)) {
                Inv.remove(p, "dragon_hunter_crossbow", 1);
                Inv.remove(p, head, 1);
                Inv.give(p, rest, 1);
                Msg.action(p, "Ghommal's hilt lets you fix the head to your dragon hunter crossbow.");
            }
            return;
        }
        if (rest.equals("defender")) {
            int tier = CombatAchievements.hiltTier(hiltId);
            if (tier >= 5 && hilt >= tier && Inv.has(p, "avernic_defender", 1)) {
                Inv.remove(p, hiltId, 1);
                Inv.remove(p, "avernic_defender", 1);
                Inv.give(p, "ghommals_avernic_defender_" + tier, 1);
                Msg.action(p, "You attach the hilt to your avernic defender.");
            }
            return;
        }
        try {
            List<CombatAchievements.HiltTele> teles = CombatAchievements.hiltTeleports(hilt);
            int i = Integer.parseInt(rest);
            if (i < 0 || i >= teles.size()) {
                return;
            }
            CombatAchievements.HiltTele t = teles.get(i);
            int perDay = t.perDay(hilt);
            if (perDay > 0 && DiaryService.dailyLeft(p, t.key(), perDay) <= 0) {
                Msg.game(p, "You have used all of today's teleports to " + t.dest().name() + ".");
                return;
            }
            if (SysTeleport.teleport(p, t.dest(), SysTeleport.JEWELLERY_WILDERNESS) && perDay > 0) {
                DiaryService.useDaily(p, t.key(), perDay);
            }
        } catch (NumberFormatException ignored) {
            // bad index
        }
    }

    // ------------------------------------------------------------------ reward effects used by other systems

    /** Ghommal's lucky penny worn in the ammo slot saves a charge 5% of the time (master tier needed). */
    public static boolean pennySavesCharge(ServerPlayer p) {
        SysState.SysPlayer sp = SysState.of(p);
        if (!sp.wornId(com.runecraft.osrs.content.systems.Jewellery.Slot.AMMO).equals(CombatAchievements.LUCKY_PENNY)) {
            return false;
        }
        Tier t = tier(p);
        return t != null && t.ordinal() >= Tier.MASTER.ordinal() && p.getRandom().nextDouble() < CombatAchievements.LUCKY_PENNY_CHANCE;
    }

    /** Medium tier: no Barrows Prayer drain while a Ghommal's hilt is held in the off-hand. */
    public static boolean barrowsDrainNullified(ServerPlayer p) {
        ItemStack off = p.getOffhandItem();
        return !off.isEmpty() && CombatAchievements.barrowsDrainNullified(tier(p), Inv.idOf(off));
    }

    /** "Like a boss": the most kills a boss slayer task can have (35, +5 per unlocked tier). */
    public static int bossTaskMax(ServerPlayer p, int base) {
        return CombatAchievements.bossTaskMax(base, tier(p));
    }

    /** Elite tier: superior slayer monsters appear 1/150 instead of 1/200. */
    public static int superiorChance(ServerPlayer p, int base) {
        return CombatAchievements.superiorChance(base, tier(p));
    }
}
