package com.runecraft.content.bosses;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.PrayerService;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.content.bosses.BossMechanics;
import com.runecraft.osrs.content.bosses.MuspahRules;
import com.runecraft.osrs.content.bosses.NightmareRules;
import com.runecraft.osrs.content.bosses.NightmareRules.Special;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * The phase walks of the in-game self-test ({@code boss:walk:the_nightmare}, {@code boss:walk:phosanis_nightmare},
 * {@code boss:walk:phantom_muspah}): a player fights the boss through every phase as the OSRS fight goes, the script's own
 * code doing each mechanic, and every step is checked. Any broken step throws with what went wrong.
 */
final class NightmareMuspahWalks {
    /** A reflected hit (a ring of recoil's or Vengeance's) in the walks. */
    private static final int REFLECTED = 12;
    /** The player's health buffer while {@link #playToKill} plays a fight on. */
    private static final net.minecraft.resources.ResourceLocation KILL_HP_BUFFER = com.runecraft.RuneCraft.id("selftest_kill_hp");
    private static long tick;

    private NightmareMuspahWalks() {
    }

    private static void check(boolean ok, String what) {
        if (!ok) {
            throw new IllegalStateException(what);
        }
    }

    /** Game ticks of the boss's script, the player kept alive and targeted. */
    private static void step(DataBossEntity boss, ServerPlayer p, int ticks) {
        for (int i = 0; i < ticks && boss.isAlive(); i++) {
            boss.setTarget(p);
            boss.script().gameTick(boss, ++tick);
            p.setHealth(p.getMaxHealth());
        }
    }

    /** One of the player's hits through the boss's own damage rules; returns what it took. */
    private static int hurt(DataBossEntity boss, ServerPlayer p, int damage, AttackType type) {
        int d = boss.modifyIncoming(p, damage, type);
        if (d > 0) {
            CombatHandler.applyDamage(p, boss, d);
        }
        p.setHealth(p.getMaxHealth());
        return d;
    }

    private static void kill(ServerPlayer p, Mob m) {
        for (int i = 0; i < 4 && m.isAlive(); i++) {
            CombatHandler.applyDamage(p, m, (int) Math.ceil(m.getHealth()) + 1);
        }
        if (m.isAlive()) {
            m.kill();
        }
    }

    // ------------------------------------------------------------------ the boss:<id> kill

    /**
     * Plays a fight on to where its death counts, as a player would, for the {@code boss:<id>} check (which otherwise kills
     * a boss outright, a death these scripts refuse until then). The Nightmare only dies to the third blast of her totems and
     * Phosani at the end of her desperation phase: she is woken, her shields broken, her totems charged and her sleepwalkers
     * killed, and the blast or Phosani's last hit ends her. The Phantom Muspah only dies in its post-shield form: it is hurt
     * down to its shockwave and its prayer shield drained. Returns what was played, or null for other bosses.
     */
    static String playToKill(DataBossEntity boss, ServerPlayer p) {
        if (!(boss.script() instanceof NightmareScript) && !(boss.script() instanceof MuspahScript)) {
            return null;
        }
        // The check ticked the boss on a clock that stood still: the attacks it wound up meanwhile (standard attacks, claws,
        // orbs) all land on the first game ticks played here. The player keeps a big health buffer, as in a fight check.
        AttributeInstance health = p.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.removeModifier(KILL_HP_BUFFER);
            health.addTransientModifier(new AttributeModifier(KILL_HP_BUFFER, 2000, AttributeModifier.Operation.ADD_VALUE));
        }
        p.setHealth(p.getMaxHealth());
        try {
            return boss.script() instanceof NightmareScript n ? nightmareToKill(boss, p, n)
                    : muspahToKill(boss, p, (MuspahScript) boss.script());
        } finally {
            if (health != null) {
                health.removeModifier(KILL_HP_BUFFER);
            }
            com.runecraft.skill.XpService.updateMaxHealth(p);
            p.setHealth(p.getMaxHealth());
        }
    }

    private static String nightmareToKill(DataBossEntity boss, ServerPlayer p, NightmareScript n) {
        tick = Math.max(ActionManager.gameTick(), n.now);
        boolean quiet = n.selfTestQuiet;
        n.selfTestQuiet = true; // her standard attacks only from here: her specials don't change what ends the fight
        int hits = 0;
        int totems = 0;
        int walkers = 0;
        int ticks = 0;
        boolean woke = n.stage == NightmareScript.Stage.ASLEEP;
        try {
            while (boss.isAlive() && ticks < 2000) {
                switch (n.stage) {
                    case ASLEEP -> n.wake(boss); // disturbed: no need to wait out her sleep
                    case SHIELD, DESPERATION -> {
                        if (n.flowersBloomed()) {
                            step(boss, p, 1); // an attack from a berry quadrant would heal her: the flowers wither first
                            ticks++;
                        } else {
                            hurt(boss, p, 97, AttackType.CRUSH);
                            hits++;
                        }
                    }
                    case TOTEMS -> {
                        for (Mob t : new ArrayList<>(n.totems)) {
                            for (int guard = 0; t.isAlive() && guard < 200; guard++) {
                                CombatHandler.applyDamage(p, t, n.totemDamage(p, t, AttackType.MAGIC, 40));
                            }
                            totems++;
                        }
                        NightmareScript.CHARGING.remove(p.getUUID());
                        step(boss, p, 1); // charged: they blast her
                        ticks++;
                    }
                    case WALKERS -> {
                        for (Mob w : new ArrayList<>(n.walkers)) {
                            kill(p, w);
                            walkers++;
                        }
                        step(boss, p, 1);
                        ticks++;
                    }
                    default -> { // DEAD: the killing blow lands on her next game tick
                        step(boss, p, 1);
                        ticks++;
                    }
                }
                if (hits > 1000) {
                    break;
                }
            }
        } finally {
            n.selfTestQuiet = quiet;
            NightmareScript.CHARGING.remove(p.getUUID());
        }
        return (woke ? "woke her, " : "") + hits + " hits on her shields" + (n.phosani() ? " and desperation" : "") + ", " + totems
                + " totems charged, " + walkers + " sleepwalkers killed" + (boss.isAlive() ? ", still " + n.stage : ", killed by the "
                + (n.phosani() ? "last hit" : "totems' blast"));
    }

    private static String muspahToKill(DataBossEntity boss, ServerPlayer p, MuspahScript m) {
        tick = Math.max(ActionManager.gameTick(), m.now);
        int hits = 0;
        int drains = 0;
        int ticks = 0;
        while (boss.isAlive() && m.stage != MuspahScript.Stage.POST && ticks < 600 && drains < 200) {
            switch (m.stage) {
                case FORMS, CLOUDS -> {
                    hurt(boss, p, 60, AttackType.RANGED);
                    hits++;
                }
                case SHIELDED -> {
                    boss.modifyIncoming(p, 10, AttackType.RANGED); // each attack takes 2 off its prayer shield
                    drains++;
                    continue;
                }
                default -> {
                }
            }
            step(boss, p, 1); // SHOCKWAVE: it sends out its shockwave, then raises its shield (or skips it)
            ticks++;
        }
        return hits + " hits to its shockwave, " + drains + " attacks on its prayer shield"
                + (m.stage == MuspahScript.Stage.POST ? ", post-shield form" : ", still " + m.stage);
    }

    // ------------------------------------------------------------------ The Nightmare / Phosani's Nightmare

    static String nightmare(DataBossEntity boss, ServerPlayer p) {
        if (!(boss.script() instanceof NightmareScript n)) {
            throw new IllegalStateException("no Nightmare script");
        }
        boolean phosani = n.phosani();
        String id = boss.def().id;
        PlayerData data = Players.data(p);
        int kcBefore = data.killCount(id);
        tick = ActionManager.gameTick();
        n.selfTestQuiet = true;
        List<String> log = new ArrayList<>();
        try {
            check(n.stage == NightmareScript.Stage.ASLEEP && !n.isVulnerable(boss), "not asleep when disturbed (" + n.stage + ")");
            check(boss.modifyIncoming(p, 50, AttackType.SLASH) == 0, "hurt while asleep");
            int slept = 0;
            while (n.stage == NightmareScript.Stage.ASLEEP && slept < 80) {
                step(boss, p, 1);
                slept++;
            }
            int wake = phosani ? NightmareRules.PHOSANI_WAKE_TICKS : NightmareRules.NIGHTMARE_WAKE_TICKS;
            check(n.stage == NightmareScript.Stage.SHIELD && slept == wake, "woke after " + slept + " ticks (expected " + wake + "), " + n.stage);
            check(n.shieldMax == NightmareRules.shield(phosani, n.players), "shield " + n.shieldMax);
            log.add("woke @" + slept + " (" + n.players + "p, shield " + n.shieldMax + ", totems " + n.totemCharge + ")");
            if (phosani) {
                for (int i = 1; i < n.sets.size(); i++) {
                    check(n.sets.get(i).a() != n.sets.get(i - 1).a(), "special sets don't alternate: " + n.sets);
                }
                log.add("sets " + n.sets);
            }
            for (int phase = 0; phase < NightmareRules.SHIELD_PHASES; phase++) {
                check(n.phase == phase && n.stage == NightmareScript.Stage.SHIELD, "phase " + n.phase + " " + n.stage + " (expected " + phase + ")");
                List<Special> specials = phosani ? n.sets.get(phase).specials() : NightmareRules.nightmareSpecials(phase);
                List<String> did = new ArrayList<>();
                for (Special s : specials) {
                    did.add(special(boss, p, n, s));
                }
                // A ring of recoil or Vengeance off her hits counts toward her shield like any hit (OSRS), though not toward
                // the player's share of the loot.
                step(boss, p, 1);
                // So it does right after a hit of theirs too small to move her bar: her rules count a 1, but scaled to her
                // health cap it comes to 0 more often than not and never lands (a 1 that comes to 1 here isn't applied).
                int small;
                int tries = 0;
                do {
                    small = boss.modifyIncoming(p, 1, AttackType.CRUSH);
                } while (small > 0 && ++tries < 30);
                int shieldBefore = n.shieldLeft;
                int[] share = n.contribution(p).clone();
                CombatHandler.applyDamage(p, boss, REFLECTED);
                check(n.stage == NightmareScript.Stage.SHIELD && n.shieldLeft == Math.max(0, shieldBefore - REFLECTED),
                        "a reflected hit didn't count toward her shield (" + shieldBefore + " -> " + n.shieldLeft + ", " + n.stage + ")");
                check(java.util.Arrays.equals(share, n.contribution(p)), "a reflected hit counted toward the player's share of the loot");
                // The shield, broken by the player's hits; her bar falls with it.
                float before = boss.getHealth();
                int hits = 0;
                while (n.stage == NightmareScript.Stage.SHIELD && hits < 300) {
                    hurt(boss, p, 97, AttackType.CRUSH);
                    hits++;
                }
                check(n.stage == NightmareScript.Stage.TOTEMS, "shield not broken after " + hits + " hits (" + n.shieldLeft + " left)");
                check(boss.getHealth() < before, "her health didn't fall with her shield (" + before + " -> " + boss.getHealth() + ")");
                check(!n.isVulnerable(boss) && boss.modifyIncoming(p, 50, AttackType.SLASH) == 0, "hurt while the totems are up");
                check(n.totems.size() == NightmareRules.TOTEMS, n.totems.size() + " totems");
                // A ring of recoil or Vengeance off her hits while she is immune does nothing, let alone end her: only the
                // totems' blast does. Her health is what is left to deal to her (1 once the last shield is down) from her
                // next game tick on; the shield hits took it down on their own scale until then.
                step(boss, p, 1);
                float immune = boss.getHealth();
                CombatHandler.applyDamage(p, boss, (int) Math.ceil(immune) + 5);
                check(boss.isAlive() && n.stage == NightmareScript.Stage.TOTEMS, "a reflected hit killed her while the totems were up ("
                        + n.stage + ")");
                check(Math.abs(boss.getHealth() - immune) <= 1f, "a reflected hit hurt her while the totems were up (" + immune + " -> "
                        + boss.getHealth() + ")");
                step(boss, p, 1);
                check(Math.abs(boss.getHealth() - immune) <= 1f, "her health moved after a reflected hit while the totems were up ("
                        + immune + " -> " + boss.getHealth() + ")");
                Mob first = n.totems.get(0);
                boolean xpBefore = com.runecraft.skill.XpService.allowed(p, com.runecraft.osrs.Skill.MAGIC);
                int magic = n.totemDamage(p, first, AttackType.MAGIC, 10);
                // Charging a totem gives no real combat experience (OSRS: fake XP drops).
                check(!com.runecraft.skill.XpService.allowed(p, com.runecraft.osrs.Skill.MAGIC)
                        && !com.runecraft.skill.XpService.allowed(p, com.runecraft.osrs.Skill.HITPOINTS)
                        && com.runecraft.skill.XpService.allowed(p, com.runecraft.osrs.Skill.PRAYER) == xpBefore,
                        "charging a totem gives combat experience");
                int melee = n.totemDamage(p, first, AttackType.CRUSH, 10);
                NightmareScript.CHARGING.remove(p.getUUID());
                check(com.runecraft.skill.XpService.allowed(p, com.runecraft.osrs.Skill.MAGIC) == xpBefore,
                        "combat experience still withheld after the totem hit");
                check(magic == 2 * melee && melee > 0, "magic doesn't charge twice as fast (" + magic + " vs " + melee + ")");
                for (Mob t : new ArrayList<>(n.totems)) {
                    int guard = 0;
                    while (t.isAlive() && guard++ < 200) {
                        CombatHandler.applyDamage(p, t, n.totemDamage(p, t, AttackType.MAGIC, 40));
                    }
                }
                NightmareScript.CHARGING.remove(p.getUUID());
                step(boss, p, 1);
                if (!phosani && phase == NightmareRules.SHIELD_PHASES - 1) {
                    check(!boss.isAlive(), "the third blast didn't kill her (" + n.stage + ")");
                    log.add("p" + (phase + 1) + " " + did + ", shield in " + hits + " hits, killed by the totems");
                    break;
                }
                check(n.phase == phase + 1, "the totems' blast didn't end the phase (" + n.phase + ", " + n.stage + ")");
                String walkers = "";
                if (NightmareRules.sleepwalkersAfter(phosani, phase + 1)) {
                    int expected = NightmareRules.sleepwalkers(phosani, phase + 1, n.players);
                    check(n.stage == NightmareScript.Stage.WALKERS && n.walkers.size() == expected,
                            n.walkers.size() + " sleepwalkers, expected " + expected + " (" + n.stage + ")");
                    for (Mob w : new ArrayList<>(n.walkers)) {
                        kill(p, w);
                    }
                    step(boss, p, 1);
                    walkers = ", " + expected + " sleepwalkers killed";
                }
                log.add("p" + (phase + 1) + " " + did + ", shield in " + hits + " hits, totems" + walkers);
            }
            if (phosani) {
                check(n.stage == NightmareScript.Stage.DESPERATION && n.isVulnerable(boss)
                        && n.desperationLeft == NightmareRules.PHOSANI_DESPERATION_HEALTH, "no desperation phase (" + n.stage + ")");
                step(boss, p, 4);
                check(!n.walkers.isEmpty(), "no sleepwalker in her desperation phase");
                for (Mob w : new ArrayList<>(n.walkers)) {
                    kill(p, w);
                }
                // Vengeance or a ring of recoil counts toward her desperation health too.
                int left = n.desperationLeft;
                CombatHandler.applyDamage(p, boss, REFLECTED);
                check(boss.isAlive() && n.desperationLeft == left - REFLECTED, "a reflected hit didn't count toward her desperation health ("
                        + left + " -> " + n.desperationLeft + ")");
                int hits = 0;
                while (boss.isAlive() && hits < 20) {
                    hurt(boss, p, 60, AttackType.CRUSH);
                    hits++;
                }
                check(!boss.isAlive(), "desperation phase not ended in 20 hits (" + n.desperationLeft + " left)");
                log.add("desperation in " + hits + " hits");
            }
            int kc = data.killCount(id) - kcBefore;
            check(kc == 1, "kill count +" + kc);
            return String.join("; ", log) + ", kc +1";
        } finally {
            n.selfTestQuiet = false;
            NightmareScript.CURSED.remove(p.getUUID());
            NightmareScript.CHARGING.remove(p.getUUID());
            NightmareScript.CURED.remove(p.getUUID());
            NightmareScript.SLOWED.remove(p.getUUID());
            BossFx.unstun(p);
        }
    }

    /** Does one of her specials and checks what it must do; returns a short note. */
    private static String special(DataBossEntity boss, ServerPlayer p, NightmareScript n, Special s) {
        PlayerData data = Players.data(p);
        switch (s) {
            case GRASPING_CLAWS -> {
                n.graspingClaws(boss, p);
                step(boss, p, NightmareRules.CLAWS_DELAY);
                return "claws";
            }
            case HUSKS -> {
                n.husks(boss, p);
                check(n.husks.size() == NightmareRules.HUSKS && n.husks.stream().allMatch(Mob::isAlive), n.husks.size() + " husks");
                step(boss, p, 3); // they bind the player and attack
                check(p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the husks didn't bind the player");
                for (Mob h : new ArrayList<>(n.husks)) {
                    kill(p, h);
                }
                step(boss, p, 1);
                check(n.husks.isEmpty() && !p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "still bound after the husks died");
                return "husks";
            }
            case CORPSE_FLOWERS -> {
                hurt(boss, p, 150, AttackType.CRUSH); // a dent in the shield the flowers can heal
                n.corpseFlowers(boss);
                step(boss, p, NightmareRules.FLOWER_BLOOM_TICKS + 1);
                check(n.flowersBloomed(), "the corpse flowers didn't bloom");
                // The player's quadrant made the one opposite the safe one: their attacks heal her instead.
                Vec3 c = boss.position();
                int q = NightmareRules.quadrant(p.getX() - c.x, p.getZ() - c.z);
                n.flowerSafe = (q + 2) % 4;
                int shield = n.shieldLeft;
                int took = boss.modifyIncoming(p, 30, AttackType.CRUSH);
                check(took == 0 && n.shieldLeft >= shield, "an attack from a berry quadrant hurt her (" + shield + " -> " + n.shieldLeft + ")");
                n.flowerTicks = 0;
                n.clawChain = 0;
                step(boss, p, 1);
                return "flowers";
            }
            case CURSE -> {
                data.setPrayerPoints(Math.max(data.prayerPoints(), 50));
                data.activePrayers().clear();
                n.curse(boss, p);
                check(NightmareScript.CURSED.containsKey(p.getUUID()), "not cursed");
                PrayerService.toggle(p, Prayer.PROTECT_FROM_MELEE);
                boolean shuffled = data.isActive(Prayer.PROTECT_FROM_MAGIC) && !data.isActive(Prayer.PROTECT_FROM_MELEE);
                PrayerService.toggle(p, Prayer.PROTECT_FROM_MELEE); // off again
                check(shuffled, "the curse didn't turn Protect from Melee into Protect from Magic (" + data.activePrayers() + ")");
                // Five of her attacks lift it.
                var ranged = n.phosani() ? com.runecraft.osrs.content.bosses.MinigameBosses.PHOSANI_RANGED
                        : com.runecraft.osrs.content.bosses.MinigameBosses.NIGHTMARE_RANGED;
                for (int i = 0; i < NightmareRules.CURSE_ATTACKS; i++) {
                    check(NightmareScript.CURSED.containsKey(p.getUUID()), "the curse wore off after " + i + " attacks");
                    n.perform(boss, p, ranged);
                }
                check(!NightmareScript.CURSED.containsKey(p.getUUID()), "the curse outlasted five attacks");
                // Out of her dream (walked or teleported off, dead, logged out) the curse lets go, and so does her loot.
                n.curse(boss, p);
                Vec3 at = p.position();
                p.setPos(at.x + NightmareScript.FIGHT_RADIUS + 16, at.y, at.z);
                boolean away = !NightmareScript.inFight(boss, p);
                PrayerService.toggle(p, Prayer.PROTECT_FROM_MELEE);
                boolean freed = data.isActive(Prayer.PROTECT_FROM_MELEE) && !NightmareScript.CURSED.containsKey(p.getUUID());
                String prayers = data.activePrayers().toString();
                p.setPos(at.x, at.y, at.z);
                data.activePrayers().clear();
                check(away, "a player " + (int) (NightmareScript.FIGHT_RADIUS + 16) + " blocks away still counts as in her dream");
                check(freed, "the curse still shuffled the prayers of a player who left her dream (" + prayers + ")");
                // Nor does it outlast its time limit.
                n.curse(boss, p);
                NightmareScript.CURSED.computeIfPresent(p.getUUID(), (k, c) -> new NightmareScript.Curse(c.boss(), c.attacksLeft(),
                        ActionManager.gameTick() - 1));
                PrayerService.toggle(p, Prayer.PROTECT_FROM_MELEE);
                boolean expired = data.isActive(Prayer.PROTECT_FROM_MELEE) && !NightmareScript.CURSED.containsKey(p.getUUID());
                data.activePrayers().clear();
                check(expired, "the curse outlasted its time limit");
                step(boss, p, NightmareRules.RANGED_WIND_UP);
                return "curse";
            }
            case PARASITES -> {
                hurt(boss, p, 200, AttackType.CRUSH);
                n.parasites(boss, p);
                check(n.impregnated.containsKey(p.getUUID()), "no parasite on the player");
                com.runecraft.content.skilling.ItemHooks.DISEASE_CURE.forEach(l -> l.accept(p)); // a sanfew serum dose
                step(boss, p, BossMechanics.PARASITE_BURST_TICKS);
                check(n.parasites.size() == 1 && n.parasites.get(0).weakened(), n.parasites.size() + " parasites after the burst");
                Mob parasite = n.parasites.get(0).mob();
                int hp = Math.round(parasite.getMaxHealth());
                if (!n.phosani()) {
                    check(hp == NightmareRules.parasiteHitpoints(false, true, 0), "weakened parasite with " + hp + " hp");
                } else {
                    check(hp == NightmareRules.parasiteHitpoints(true, true, 0), "Phosani's weakened parasite with " + hp + " hp");
                }
                int shield = n.shieldLeft;
                step(boss, p, BossMechanics.PARASITE_HEAL_INTERVAL);
                check(n.shieldLeft > shield, "the parasite didn't heal her shield (" + shield + " -> " + n.shieldLeft + ")");
                kill(p, parasite);
                return "parasite (" + hp + " hp)";
            }
            case SURGE -> {
                n.surge(boss);
                Vec3 from = boss.position();
                step(boss, p, NightmareRules.SURGE_WIND_UP);
                check(boss.position().distanceToSqr(from) > 12 * 12, "she didn't surge across the arena");
                return "surge";
            }
            case SPORES -> {
                n.placeSpores(boss);
                check(n.spores.size() == BossMechanics.SPORES, n.spores.size() + " spores");
                n.burstSpore(boss, p, p.position());
                check(n.weary(p), "a burst spore didn't make the player weary");
                n.spores.clear();
                n.clawChain = 0;
                return "spores";
            }
            default -> {
                return s.name();
            }
        }
    }

    // ------------------------------------------------------------------ The Phantom Muspah

    static String muspah(DataBossEntity boss, ServerPlayer p) {
        if (!(boss.script() instanceof MuspahScript m)) {
            throw new IllegalStateException("no Muspah script");
        }
        PlayerData data = Players.data(p);
        var sp = com.runecraft.content.systems.SysState.of(p);
        int kcBefore = data.killCount("phantom_muspah");
        boolean hadSmite = data.activePrayers().contains(Prayer.SMITE);
        tick = ActionManager.gameTick();
        List<String> log = new ArrayList<>();
        try {
            step(boss, p, 1);
            check(m.stage == MuspahScript.Stage.FORMS && m.form == m.spawnForm && m.spawnForm != null, "spawned in " + m.form);
            MuspahRules.Form first = m.form;
            // Its first swap: the form's damage (100 ranged, 80 melee).
            int need = first == MuspahRules.Form.RANGED ? MuspahRules.RANGED_FORM_DAMAGE : MuspahRules.MELEE_FORM_DAMAGE;
            for (int i = 0; i < 4 && m.form == first; i++) {
                hurt(boss, p, need / 4 + 1, AttackType.RANGED);
            }
            check(m.form != first && m.swapped, "no swap after " + need + " damage in its " + first + " form");
            long under = BossMechanics.tile(p.getBlockX(), p.getBlockZ());
            check(m.erupting.containsKey(under), "no spike marked under the player on the swap");
            step(boss, p, MuspahRules.SPIKE_ERUPT_TICKS);
            check(m.spikes.contains(under), "the marked spike didn't erupt");
            m.spikes.remove(under); // the player stands there; the walk goes on without the spike
            // Its second swap needs four separate hits as well.
            // (105 damage in four hits keeps it above 637, where its first special would come first)
            MuspahRules.Form second = m.form;
            hurt(boss, p, 60, AttackType.MAGIC);
            check(m.form == second, "swapped again after a single hit");
            for (int i = 0; i < 3; i++) {
                hurt(boss, p, 15, AttackType.MAGIC);
            }
            check(m.form != second, "no swap after four hits and the form's damage");
            log.add("spawned " + first + ", swapped twice, spike under the player");
            // Both specials.
            for (int s = 0; s < 2; s++) {
                int threshold = s == 0 ? MuspahRules.FIRST_SPECIAL_HP : MuspahRules.SECOND_SPECIAL_HP;
                int guard = 0;
                while (m.specialsDone == s && guard++ < 60) {
                    if (m.stage == MuspahScript.Stage.FORMS) {
                        hurt(boss, p, Math.max(1, Math.min(60, (int) boss.getHealth() - threshold)), AttackType.RANGED);
                    }
                    step(boss, p, 1);
                }
                MuspahRules.Special special = MuspahRules.special(s, m.spawnForm);
                check(m.specialsDone == s + 1, "special " + (s + 1) + " not used at " + threshold + " (" + boss.getHealth() + " health)");
                if (special == MuspahRules.Special.LIGHTNING_CLOUDS) {
                    check(m.stage == MuspahScript.Stage.CLOUDS, "no lightning clouds (" + m.stage + ")");
                    step(boss, p, 2);
                    check(p.hasEffect(MobEffects.DARKNESS), "the arena didn't darken");
                    int t = 0;
                    while (m.stage == MuspahScript.Stage.CLOUDS && t++ < 40) {
                        step(boss, p, 1);
                    }
                    check(m.stage == MuspahScript.Stage.FORMS, "the clouds never ended");
                    log.add("lightning clouds @" + threshold);
                } else {
                    check(m.homing.size() >= MuspahRules.HOMING_MIN && m.homing.size() <= MuspahRules.HOMING_MAX,
                            m.homing.size() + " homing spikes");
                    int[] h = m.homing.get(0);
                    double d0 = Math.hypot(h[0] - p.getBlockX(), h[1] - p.getBlockZ());
                    step(boss, p, 2 * MuspahRules.HOMING_STEP_TICKS);
                    if (!m.homing.isEmpty()) {
                        int[] h2 = m.homing.get(0);
                        check(Math.hypot(h2[0] - p.getBlockX(), h2[1] - p.getBlockZ()) < d0 || h2 != h, "the homing spikes didn't move");
                    }
                    step(boss, p, MuspahRules.HOMING_TICKS);
                    check(m.homing.isEmpty(), "the homing spikes didn't harden");
                    log.add("homing spikes @" + threshold);
                }
            }
            // Below 127: the shockwave, which a spike between it and the player stops.
            int guard = 0;
            while (m.stage != MuspahScript.Stage.SHOCKWAVE && guard++ < 60) {
                if (m.stage == MuspahScript.Stage.FORMS) {
                    hurt(boss, p, Math.max(1, Math.min(60, (int) boss.getHealth() - MuspahRules.SHIELD_BELOW + 1)), AttackType.RANGED);
                }
                step(boss, p, 1);
            }
            check(m.stage == MuspahScript.Stage.SHOCKWAVE, "no shockwave below " + MuspahRules.SHIELD_BELOW + " (" + m.stage + ")");
            int cx = boss.getBlockX();
            int cz = boss.getBlockZ();
            int half = MuspahScript.footprintHalf(boss);
            // The player hides behind a spike: a spike on the line to it, past its body. Right beside its body there is no
            // room for one, so a player standing there steps back first (as in OSRS).
            Vec3 stood = p.position();
            boolean stepped = Math.max(Math.abs(p.getBlockX() - cx), Math.abs(p.getBlockZ() - cz)) < half + 2;
            if (stepped) {
                p.setPos(cx + half + 2 + 0.5, stood.y, cz + 0.5);
            }
            try {
                int px = p.getBlockX();
                int pz = p.getBlockZ();
                m.spikes.removeIf(t -> t == BossMechanics.tile(px, pz));
                long shelter = 0;
                boolean placed = false;
                for (long t : MuspahRules.line(cx, cz, px, pz)) {
                    int x = (int) (t >> 32);
                    int z = (int) t;
                    if (Math.max(Math.abs(x - cx), Math.abs(z - cz)) > half && (x != px || z != pz)) {
                        m.spikes.add(t);
                        shelter = t;
                        placed = true;
                        break;
                    }
                }
                check(placed, "no tile between the Muspah (footprint half-size " + half + " at " + cx + "," + cz + ") and the player at "
                        + px + "," + pz + " for a spike");
                check(MuspahRules.sheltered(m.spikes, cx, cz, px, pz, half), "the spike doesn't shelter the player");
                step(boss, p, MuspahRules.SHOCKWAVE_WIND_UP);
                check(m.stage == MuspahScript.Stage.SHIELDED && m.shield() == BossMechanics.MUSPAH_SHIELD, "no prayer shield after the shockwave ("
                        + m.stage + ")");
                log.add("shockwave behind a spike" + (stepped ? " (stepped back to " + px + "," + pz + ")" : "") + ", spike at "
                        + (int) (shelter >> 32) + "," + (int) shelter);
            } finally {
                if (stepped) {
                    p.setPos(stood.x, stood.y, stood.z);
                }
            }
            // Immune while shielded: Vengeance or a ring of recoil off its hits can't hurt it, let alone kill it.
            float shielded = boss.getHealth();
            CombatHandler.applyDamage(p, boss, (int) Math.ceil(shielded) + 10);
            check(boss.isAlive() && m.stage == MuspahScript.Stage.SHIELDED, "a reflected hit killed it through its shield (" + m.stage + ")");
            step(boss, p, 1);
            check(boss.getHealth() >= shielded - 0.5f, "a reflected hit hurt it through its shield (" + shielded + " -> " + boss.getHealth() + ")");
            // The shield: Smite and a corruption spell drain it beyond its 2 an attack.
            data.activePrayers().add(Prayer.SMITE);
            int absorbed = boss.modifyIncoming(p, 20, AttackType.RANGED);
            int afterSmite = m.shield();
            int expected = BossMechanics.MUSPAH_SHIELD - BossMechanics.MUSPAH_SHIELD_PER_ATTACK - BossMechanics.muspahSmiteDrain(20);
            check(absorbed == 0 && afterSmite <= expected, "Smite hit: took " + absorbed + ", shield " + afterSmite + " (expected <= " + expected + ")");
            data.activePrayers().remove(Prayer.SMITE);
            sp.corruptionPending = 2;
            sp.corruptionPendingUntil = com.runecraft.content.systems.SysTicks.now() + 100;
            boss.modifyIncoming(p, 0, AttackType.MAGIC);
            int afterCorruption = m.shield();
            check(afterSmite - afterCorruption == BossMechanics.MUSPAH_SHIELD_PER_ATTACK + BossMechanics.muspahCorruptionDrain(true),
                    "Greater Corruption drained " + (afterSmite - afterCorruption) + " of the shield");
            check(m.drains.containsAll(java.util.Set.of(BossMechanics.DRAIN_SMITE, BossMechanics.DRAIN_CORRUPTION)), "drain sources " + m.drains);
            int hits = 0;
            while (m.stage == MuspahScript.Stage.SHIELDED && hits++ < 60) {
                boss.modifyIncoming(p, 10, AttackType.RANGED);
            }
            check(m.stage == MuspahScript.Stage.POST, "the shield never broke (" + m.shield() + " left)");
            check(!m.erupting.isEmpty() || !m.spikes.isEmpty(), "no spikes when the shield shattered");
            log.add("shield 75 -> " + afterSmite + " (Smite) -> " + afterCorruption + " (Greater Corruption) -> broken");
            hits = 0;
            while (boss.isAlive() && hits++ < 40) {
                hurt(boss, p, 60, AttackType.RANGED);
            }
            check(!boss.isAlive(), "not killed after its shield (" + boss.getHealth() + " health)");
            int kc = data.killCount("phantom_muspah") - kcBefore;
            check(kc == 1, "kill count +" + kc);
            // A frozen cache from its unique table opens into one roll of its own table.
            int caches = com.runecraft.util.Inv.count(p, MuspahRules.FROZEN_CACHE);
            com.runecraft.util.Inv.give(p, MuspahRules.FROZEN_CACHE, 1);
            String got = MuspahScript.openCache(p);
            check(got != null && (got.equals(MuspahRules.FROZEN_CACHE) || com.runecraft.util.Inv.count(p, MuspahRules.FROZEN_CACHE) == caches),
                    "the frozen cache didn't open (" + got + ")");
            log.add("frozen cache gave " + got);
            return String.join("; ", log) + ", kc +1";
        } finally {
            if (!hadSmite) {
                data.activePrayers().remove(Prayer.SMITE);
            }
            sp.corruptionPending = 0;
            p.removeEffect(MobEffects.DARKNESS);
        }
    }
}
