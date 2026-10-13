package com.runecraft.content.systems;

import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.MonsterStats;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.systems.CombatAchievements;
import com.runecraft.osrs.content.systems.CombatAchievements.Task;
import com.runecraft.osrs.content.systems.CombatAchievements.Tier;
import com.runecraft.osrs.content.systems.CombatAchievements.Track;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks Combat Achievement tasks: kill counts (bosses and minigame completions) are polled, regular monster kills
 * come from the kill event, and fight tasks (speed, perfection, restrictions) are measured over a fight with one
 * boss or monster that starts with the first hit either way and ends with its kill.
 */
public final class CombatAchievementService {
    /** One fight in progress. */
    private static final class Fight {
        final String key;
        final long start;
        long last;
        int kcAtStart;
        boolean damaged;
        boolean unprotected;
        boolean prayerLost;
        int prayer;
        float health;
        final Set<AttackType> styles = EnumSet.noneOf(AttackType.class);
        final Set<String> weapons = new LinkedHashSet<>();
        String lastWeapon;

        Fight(String key, long now) {
            this.key = key;
            this.start = now;
            this.last = now;
        }
    }

    private static final Map<UUID, Fight> FIGHTS = new HashMap<>();

    private CombatAchievementService() {
    }

    public static void register() {
        CombatHooks.addModifier(new CombatHooks.PlayerAttackModifier() {
            @Override
            public int afterRoll(ServerPlayerEntity player, LivingEntity target, AttackType type, int damage) {
                Fight f = engage(player, target);
                if (f != null) {
                    f.styles.add(type);
                    String weapon = player.getMainHandStack().isEmpty() ? "" : Inv.idOf(player.getMainHandStack());
                    f.weapons.add(weapon);
                    f.lastWeapon = weapon;
                }
                return damage;
            }
        });
        CombatHooks.addHurtListener((player, attacker, damage) -> {
            if (attacker != null) {
                engage(player, attacker);
            }
            Fight f = FIGHTS.get(player.getUuid());
            if (f != null && damage > 0) {
                f.damaged = true;
                if (!protectedFrom(player)) {
                    f.unprotected = true;
                }
            }
        });
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
        if (e instanceof ServerPlayerEntity) {
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

    /** Starts or continues the fight with an entity; a different opponent starts a new fight. */
    private static Fight engage(ServerPlayerEntity p, LivingEntity other) {
        String key = fightKey(other);
        if (key == null) {
            return null;
        }
        long now = SysTicks.now();
        Fight f = FIGHTS.get(p.getUuid());
        if (f != null && !f.key.equals(key) && !f.key.startsWith("kill:")) {
            // A boss fight carries on while its minions, bodyguards or spawns join in.
            f.last = now;
            return null;
        }
        if (f == null || !f.key.equals(key)) {
            f = new Fight(key, now);
            f.kcAtStart = bossKc(p, key);
            f.prayer = Players.data(p).prayerPoints();
            f.health = p.getHealth();
            FIGHTS.put(p.getUuid(), f);
        }
        f.last = now;
        return f;
    }

    private static boolean protectedFrom(ServerPlayerEntity p) {
        Set<Prayer> active = Players.data(p).activePrayers();
        return active.contains(Prayer.PROTECT_FROM_MELEE) || active.contains(Prayer.PROTECT_FROM_MAGIC)
                || active.contains(Prayer.PROTECT_FROM_MISSILES);
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

    private static void tick(ServerPlayerEntity p, long tick) {
        Fight f = FIGHTS.get(p.getUuid());
        if (f != null) {
            if (tick - f.last > CombatAchievements.FIGHT_TIMEOUT_TICKS || !p.isAlive()) {
                FIGHTS.remove(p.getUuid());
            } else {
                // Damage from anything (hazards, specials, minions) counts, not just hits reported to the hurt listener.
                if (p.getHealth() < f.health - 0.01f) {
                    f.damaged = true;
                    if (!protectedFrom(p)) {
                        f.unprotected = true;
                    }
                }
                f.health = p.getHealth();
                int prayer = Players.data(p).prayerPoints();
                if (prayer < f.prayer) {
                    f.prayerLost = true;
                }
                f.prayer = prayer;
                if (!f.key.startsWith("kill:") && bossKc(p, f.key) > f.kcAtStart) {
                    finish(p, f);
                }
            }
        }
        if ((tick + p.getId()) % 20 == 0) {
            checkKillCounts(p);
        }
    }

    private static void finish(ServerPlayerEntity p, Fight f) {
        FIGHTS.remove(p.getUuid());
        CombatAchievements.FightResult r = new CombatAchievements.FightResult(SysTicks.now() - f.start, f.damaged, f.unprotected,
                f.prayerLost, f.styles, f.weapons, f.lastWeapon);
        SysState.SysPlayer sp = SysState.of(p);
        for (Task t : CombatAchievements.tasks()) {
            if (t.track().fight() && f.key.equals(t.target()) && !sp.caDone.contains(t.id()) && CombatAchievements.fightMeets(t, r)) {
                complete(p, t);
            }
        }
    }

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
        // Bosses finish their fight when their kill count goes up; regular monsters finish here.
        Fight f = FIGHTS.get(p.getUuid());
        if (f != null && f.key.startsWith("kill:") && bossId(e) == null && f.key.equals("kill:" + name)) {
            finish(p, f);
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
}
