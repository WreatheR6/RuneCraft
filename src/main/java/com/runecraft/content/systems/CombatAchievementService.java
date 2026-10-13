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
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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

        Tracker(String key, long now) {
            this.key = key;
            this.start = now;
            this.last = now;
        }

        boolean fight() {
            return !key.startsWith("mg:") && !key.equals("raid");
        }
    }

    /** A kill streak of one task: kills so far and where the last one happened. */
    private static final class Streak {
        int count;
        Vec3d anchor;
        String world;
    }

    /** "Without leaving": a streak breaks when the player moves this far from where the last kill happened. */
    private static final double STREAK_RANGE = 64; // approx: OSRS checks the instance or room
    /** Players this close to a fight count towards its group size. */
    private static final double GROUP_RANGE = 24;

    private static final Map<UUID, Tracker> FIGHTS = new HashMap<>();
    private static final Map<UUID, Tracker> RUNS = new HashMap<>();
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
            public int afterRoll(ServerPlayerEntity player, LivingEntity target, AttackType type, int damage) {
                onAttack(player, target, type, damage);
                return damage;
            }
        });
        CombatHooks.addDamageListener(CombatAchievementService::onDamaged);
        com.runecraft.content.GameEvents.onKill(CombatAchievementService::onKill);
        SysTicks.everyPlayer(CombatAchievementService::tick);
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
        if (e == null || e instanceof ServerPlayerEntity) {
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
        if (e instanceof ServerPlayerEntity pl) {
            return pl.getName().getString().toLowerCase(Locale.ROOT);
        }
        return MonsterStats.of(e).name().toLowerCase(Locale.ROOT);
    }

    /** Starts or continues the fight with an entity; a different opponent starts a new fight. */
    private static Tracker engage(ServerPlayerEntity p, LivingEntity other) {
        String key = fightKey(other);
        if (key == null) {
            return null;
        }
        long now = SysTicks.now();
        Tracker f = FIGHTS.get(p.getUuid());
        if (f != null && !f.key.equals(key) && !f.key.startsWith("kill:")) {
            // A boss fight carries on while its minions, bodyguards or spawns join in.
            f.last = now;
            return null;
        }
        if (f == null || !f.key.equals(key)) {
            f = new Tracker(key, now);
            f.kcAtStart = bossKc(p, key);
            start(p, f);
            for (Kill k : RECENT.getOrDefault(p.getUuid(), List.of())) {
                if (now - k.end() <= CombatAchievements.FIGHT_TIMEOUT_TICKS) {
                    f.m.killed(k.key());
                }
            }
            f.m.startPlayers = playersNear(p);
            f.m.maxPlayers = f.m.startPlayers;
            RaidInstance raid = RaidManager.of(p);
            if (raid != null) {
                f.raid = raid;
                f.raidDeathsAtStart = raid.totalDeaths();
                f.myRaidDeathsAtStart = raid.deaths(p.getUuid());
            }
            FIGHTS.put(p.getUuid(), f);
        }
        f.last = now;
        return f;
    }

    /** The starting state of a fight or run. */
    private static void start(ServerPlayerEntity p, Tracker t) {
        PlayerData d = Players.data(p);
        t.prayer = d.prayerPoints();
        t.health = p.getHealth();
        t.m.maxHp = (int) Math.ceil(p.getHealth());
        t.m.minHp = t.m.maxHp;
        t.m.book = SysState.of(p).book.name();
        for (Prayer pr : d.activePrayers()) {
            t.m.prayersEver.add(pr.name());
        }
    }

    private static int playersNear(ServerPlayerEntity p) {
        return p.getServerWorld().getPlayers(o -> o.isAlive() && !o.isSpectator() && o.squaredDistanceTo(p) < GROUP_RANGE * GROUP_RANGE).size();
    }

    private static boolean protectedFrom(ServerPlayerEntity p, AttackType style) {
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
    private static int bossKc(ServerPlayerEntity p, String key) {
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
    private static String spellOf(ServerPlayerEntity p) {
        String auto = SysState.of(p).autocast;
        if (auto != null && !auto.isEmpty()) {
            return auto;
        }
        com.runecraft.osrs.Spell s = Players.data(p).autocast();
        return s == null ? null : s.name().toLowerCase(Locale.ROOT);
    }

    /** One attack by the player (hit or miss). */
    private static void onAttack(ServerPlayerEntity p, LivingEntity target, AttackType type, int damage) {
        String weapon = p.getMainHandStack().isEmpty() ? "" : Inv.idOf(p.getMainHandStack());
        boolean spec = SpecialAttackService.active(p) != null;
        String spell = type == AttackType.MAGIC ? spellOf(p) : null;
        Tracker f = engage(p, target);
        if (f != null) {
            record(f.m, type, weapon, spell, spec);
            f.m.targetAttacks++;
            if (spec) {
                f.m.specAttacks++;
            }
            if (damage > 0) {
                f.m.targetHits++;
            }
        }
        Tracker run = RUNS.get(p.getUuid());
        if (run != null) {
            record(run.m, type, weapon, spell, spec);
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
    private static void onDamaged(ServerPlayerEntity p, LivingEntity attacker, String attack, AttackType style, int damage) {
        if (attacker != null && !(attacker instanceof ServerPlayerEntity)) {
            engage(p, attacker);
        }
        String attackerKey = fightKey(attacker);
        String attackerName = nameOf(attacker);
        boolean prayed = protectedFrom(p, style);
        for (Tracker t : trackers(p)) {
            boolean fromTarget = t.fight() && t.key.equals(attackerKey);
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

    private static List<Tracker> trackers(ServerPlayerEntity p) {
        List<Tracker> list = new ArrayList<>(2);
        Tracker f = FIGHTS.get(p.getUuid());
        if (f != null) {
            list.add(f);
        }
        Tracker r = RUNS.get(p.getUuid());
        if (r != null) {
            list.add(r);
        }
        return list;
    }

    // ------------------------------------------------------------------ per-tick measurements

    private static void tick(ServerPlayerEntity p, long tick) {
        if (!p.isAlive()) {
            FIGHTS.remove(p.getUuid());
            STREAKS.remove(p.getUuid());
            return;
        }
        Tracker f = FIGHTS.get(p.getUuid());
        if (f != null && tick - f.last > CombatAchievements.FIGHT_TIMEOUT_TICKS) {
            FIGHTS.remove(p.getUuid());
            f = null;
        }
        tickRun(p);
        List<Tracker> active = trackers(p);
        if (!active.isEmpty()) {
            Snapshot s = snapshot(p);
            int near = f != null ? playersNear(p) : 1;
            for (Tracker t : active) {
                measure(p, t, s);
                if (t == f) {
                    t.m.maxPlayers = Math.max(t.m.maxPlayers, near);
                }
            }
        }
        f = FIGHTS.get(p.getUuid());
        if (f != null && f.fight() && !f.key.startsWith("kill:") && bossKc(p, f.key) > f.kcAtStart) {
            finish(p, f, null);
        }
        checkStreakRange(p);
        if ((tick + p.getId()) % 20 == 0) {
            checkKillCounts(p);
        }
    }

    /** What the player carries and wears this tick. */
    private record Snapshot(Map<String, Integer> food, int doses, String equipment, Set<String> prayers, String book, boolean running,
                            float health, int prayer) {
    }

    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

    private static Snapshot snapshot(ServerPlayerEntity p) {
        Map<String, Integer> food = new HashMap<>();
        int doses = 0;
        for (int i = 0; i < p.getInventory().size(); i++) {
            ItemStack st = p.getInventory().getStack(i);
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
                doses += st.getCount() * Math.max(1, 4 - st.getDamage());
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
    private static List<String> worn(ServerPlayerEntity p) {
        List<String> ids = new ArrayList<>();
        for (EquipmentSlot slot : SLOTS) {
            ItemStack st = p.getEquippedStack(slot);
            ids.add(st.isEmpty() ? "" : Inv.idOf(st));
        }
        SysState.SysPlayer sp = SysState.of(p);
        for (Jewellery.Slot slot : Jewellery.Slot.values()) {
            ids.add(sp.wornId(slot));
        }
        return ids;
    }

    private static void measure(ServerPlayerEntity p, Tracker t, Snapshot s) {
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
        }
        t.prayer = s.prayer();
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
        t.food = s.food();
        t.doses = s.doses();
        t.equipment = s.equipment();
        m.prayersEver.addAll(s.prayers());
        if (s.running()) {
            m.ran = true;
        }
        if (m.book != null && !m.book.equals(s.book())) {
            m.bookChanged = true;
        }
    }

    // ------------------------------------------------------------------ kills

    private static void onKill(ServerPlayerEntity p, LivingEntity e, MonsterStats stats) {
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
        List<Kill> recent = RECENT.computeIfAbsent(p.getUuid(), u -> new ArrayList<>());
        recent.add(new Kill(key, now, now));
        if (recent.size() > 30) {
            recent.remove(0);
        }
        Tracker f = FIGHTS.get(p.getUuid());
        boolean mine = f != null && f.key.equals(key);
        windows(p, key, mine ? f.start : now, now, mine ? f.m : null);
        if (mine) {
            Metrics m = f.m;
            StatusEffectInstance slow = e.getStatusEffect(StatusEffects.SLOWNESS);
            m.frozen = slow != null && slow.getAmplifier() >= 6;
            m.othersAlive = !e.getWorld().getEntitiesByClass(OsrsMonsterEntity.class, e.getBoundingBox().expand(16),
                    o -> o != e && o.isAlive()).isEmpty();
            String raw = bossId(e);
            m.killedId = raw != null ? raw : name;
            // Bosses finish their fight when their kill count goes up; that has happened by now.
            if (key.startsWith("kill:") || bossKc(p, key) > f.kcAtStart) {
                finish(p, f, e);
            }
        }
    }

    private static void finish(ServerPlayerEntity p, Tracker f, LivingEntity killed) {
        FIGHTS.remove(p.getUuid());
        long now = SysTicks.now();
        Metrics m = f.m;
        m.ticks = now - f.start;
        m.prayersAtEnd.clear();
        for (Prayer pr : Players.data(p).activePrayers()) {
            m.prayersAtEnd.add(pr.name());
        }
        m.wornAtEnd.clear();
        m.wornAtEnd.addAll(worn(p));
        m.antifire = p.hasStatusEffect(StatusEffects.FIRE_RESISTANCE);
        if (f.raid != null) {
            m.raid = raidInfo(f.raid, p, f.raidDeathsAtStart, f.myRaidDeathsAtStart);
        }
        CombatAchievements.FightResult r = m.result();
        SysState.SysPlayer sp = SysState.of(p);
        List<Long> durations = DURATIONS.computeIfAbsent(p.getUuid(), u -> new HashMap<>()).computeIfAbsent(f.key, k -> new ArrayList<>());
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
                    Streak s = STREAKS.computeIfAbsent(p.getUuid(), u -> new HashMap<>()).computeIfAbsent(t.id(), k -> new Streak());
                    if (!CombatAchievementRules.meets(CombatAchievementRules.killFilter(t.rule()), m)) {
                        s.count = 0;
                    } else {
                        s.count++;
                    }
                    s.anchor = p.getPos();
                    s.world = p.getWorld().getRegistryKey().getValue().toString();
                    if (s.count >= t.count()) {
                        STREAKS.get(p.getUuid()).remove(t.id());
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
    private static void windows(ServerPlayerEntity p, String key, long start, long end, Metrics fight) {
        SysState.SysPlayer sp = SysState.of(p);
        for (Task t : CombatAchievements.tasks()) {
            if (t.track() != Track.WINDOW || sp.caDone.contains(t.id()) || !t.targets(key)) {
                continue;
            }
            String filter = CombatAchievementRules.killFilter(t.rule());
            if (!filter.isEmpty() && (fight == null || !CombatAchievementRules.meets(filter, fight))) {
                continue;
            }
            List<Kill> kills = WINDOWS.computeIfAbsent(p.getUuid(), u -> new HashMap<>()).computeIfAbsent(t.id(), k -> new ArrayList<>());
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
    private static void checkStreakRange(ServerPlayerEntity p) {
        Map<String, Streak> streaks = STREAKS.get(p.getUuid());
        if (streaks == null || streaks.isEmpty()) {
            return;
        }
        String world = p.getWorld().getRegistryKey().getValue().toString();
        streaks.values().removeIf(s -> s.anchor == null || !world.equals(s.world)
                || s.anchor.squaredDistanceTo(p.getPos()) > STREAK_RANGE * STREAK_RANGE);
    }

    /** Progress of a counted task for the task list: kills, completions or the current streak. */
    static int progress(ServerPlayerEntity p, Task t) {
        if (t.track() == Track.STREAK) {
            Map<String, Streak> streaks = STREAKS.get(p.getUuid());
            Streak s = streaks == null ? null : streaks.get(t.id());
            return s == null ? 0 : s.count;
        }
        return SysState.of(p).caCounters.getOrDefault(t.id(), 0);
    }

    // ------------------------------------------------------------------ raids and minigame runs

    private static CombatAchievementRules.Raid raidInfo(RaidInstance raid, ServerPlayerEntity p, int deathsAtStart, int myDeathsAtStart) {
        Set<String> invocations = new HashSet<>();
        for (Invocation i : raid.invocations()) {
            invocations.add(i.name());
        }
        boolean withinTarget = raid.elapsedTicks() <= RaidScaling.targetTicks(raid.type, raid.mode(), raid.partySize());
        return new CombatAchievementRules.Raid(raid.type.name(), raid.mode().name(), raid.partySize(), raid.raidLevel(), invocations,
                raid.pathLevel(), raid.totalDeaths() - deathsAtStart, raid.deaths(p.getUuid()) - myDeathsAtStart, withinTarget,
                raid.points(p.getUuid()), raid.suppliesTaken());
    }

    /** Starts, ends and completes raid and minigame runs. */
    private static void tickRun(ServerPlayerEntity p) {
        UUID u = p.getUuid();
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

    private static void completeRaid(ServerPlayerEntity p, Tracker run) {
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

    private static void completeRun(ServerPlayerEntity p, Tracker run) {
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

    /** Completes a task, or counts towards it when it needs several qualifying completions. */
    private static void counted(ServerPlayerEntity p, SysState.SysPlayer sp, Task t) {
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
    static int kc(ServerPlayerEntity p, String key) {
        if (key.startsWith("mg:")) {
            return com.runecraft.content.minigames.MinigameData.of(p).stat(p.getUuid(), "kc:" + key.substring(3));
        }
        return Players.data(p).killCount(key);
    }

    static void checkKillCounts(ServerPlayerEntity p) {
        SysState.SysPlayer sp = SysState.of(p);
        for (Task t : CombatAchievements.tasks()) {
            if (t.track() == Track.KC && !sp.caDone.contains(t.id()) && CombatAchievements.kcValue(t.target(), k -> kc(p, k)) >= t.count()) {
                complete(p, t);
            }
        }
    }

    // ------------------------------------------------------------------ completion and rewards

    public static int points(ServerPlayerEntity p) {
        return CombatAchievements.points(SysState.of(p).caDone);
    }

    /** The highest tier the player's points unlock, or null. */
    public static Tier tier(ServerPlayerEntity p) {
        return CombatAchievements.unlocked(points(p));
    }

    static void complete(ServerPlayerEntity p, Task t) {
        SysState.SysPlayer sp = SysState.of(p);
        Tier before = CombatAchievements.unlocked(CombatAchievements.points(sp.caDone));
        if (!sp.caDone.add(t.id())) {
            return;
        }
        int points = CombatAchievements.points(sp.caDone);
        p.sendMessage(Text.literal("Congratulations, you've completed " + (t.tier() == Tier.EASY || t.tier() == Tier.ELITE ? "an " : "a ")
                + t.tier().id() + " combat task: " + t.name() + " (" + t.points() + " point" + (t.points() == 1 ? "" : "s")
                + "). Total points: " + points + ".").formatted(Formatting.DARK_GREEN), false);
        Tier after = CombatAchievements.unlocked(points);
        if (after != null && after != before) {
            p.sendMessage(Text.literal("You have unlocked the " + after.id() + " tier of Combat Achievement rewards! Claim them from Ghommal "
                    + "at the Warriors' Guild or the Combat Achievements menu.").formatted(Formatting.GOLD), false);
        }
    }

    public static boolean claimable(SysState.SysPlayer sp, Tier t) {
        return CombatAchievements.unlocked(CombatAchievements.points(sp.caDone), t) && !sp.caClaimed.contains(t.name());
    }

    /** Hands out a tier's rewards (or a replacement hilt). */
    public static void claim(ServerPlayerEntity p, Tier t) {
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

    public static void rubLamp(ServerPlayerEntity p, Tier tier, Skill skill) {
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
    static int effectiveHilt(ServerPlayerEntity p, String hiltId) {
        Tier unlocked = tier(p);
        return unlocked == null ? 0 : Math.min(CombatAchievements.hiltTier(hiltId), unlocked.ordinal() + 1);
    }

    static void hiltMenu(ServerPlayerEntity p, String hiltId) {
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
        s.send(p);
    }

    static void hiltAction(ServerPlayerEntity p, String action, Map<String, String> fields) {
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
    public static boolean pennySavesCharge(ServerPlayerEntity p) {
        SysState.SysPlayer sp = SysState.of(p);
        if (!sp.wornId(com.runecraft.osrs.content.systems.Jewellery.Slot.AMMO).equals(CombatAchievements.LUCKY_PENNY)) {
            return false;
        }
        Tier t = tier(p);
        return t != null && t.ordinal() >= Tier.MASTER.ordinal() && p.getRandom().nextDouble() < CombatAchievements.LUCKY_PENNY_CHANCE;
    }

    /** Medium tier: no Barrows Prayer drain while a Ghommal's hilt is held in the off-hand. */
    public static boolean barrowsDrainNullified(ServerPlayerEntity p) {
        ItemStack off = p.getOffHandStack();
        return !off.isEmpty() && CombatAchievements.barrowsDrainNullified(tier(p), Inv.idOf(off));
    }

    /** "Like a boss": the most kills a boss slayer task can have (35, +5 per unlocked tier). */
    public static int bossTaskMax(ServerPlayerEntity p, int base) {
        return CombatAchievements.bossTaskMax(base, tier(p));
    }

    /** Elite tier: superior slayer monsters appear 1/150 instead of 1/200. */
    public static int superiorChance(ServerPlayerEntity p, int base) {
        return CombatAchievements.superiorChance(base, tier(p));
    }
}
