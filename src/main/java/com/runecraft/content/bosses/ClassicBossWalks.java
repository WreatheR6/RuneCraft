package com.runecraft.content.bosses;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.Mechanics;
import com.runecraft.combat.CombatHandler;
import com.runecraft.content.systems.SysCombat;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.bosses.ClassicBosses;
import com.runecraft.osrs.content.bosses.ZulrahVorkath;
import com.runecraft.osrs.content.bosses.ZulrahVorkath.Step;
import com.runecraft.player.Players;
import com.runecraft.registry.ModEntities;
import com.runecraft.util.Inv;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Self-test walks of the classic bosses' mechanics ({@code boss:zulrah_rotation}, {@code boss:vorkath_specials},
 * {@code boss:kalphite_queen_forms}, {@code boss:corp_mechanics}, {@code boss:sarachnis_web}, {@code boss:scurrius_food}): each
 * drives a freshly spawned boss's script game tick by game tick, as a fight would, and checks the mechanic a player has to
 * answer actually happens (and what the player can do about it works). Area damage (strikes, stomps, splinters) only reaches a
 * player in the world's player list, so those parts are only checked as the real player ({@code /rc selftest real boss:}).
 * Each returns a summary or throws {@link AssertionError}.
 */
final class ClassicBossWalks {
    private static final net.minecraft.resources.ResourceLocation HP_BUFFER = com.runecraft.RuneCraft.id("selftest_classic_hp");

    private ClassicBossWalks() {
    }

    private static void expect(boolean ok, String message) {
        if (!ok) {
            throw new AssertionError(message);
        }
    }

    /** Whether the player is in the world's player list (area attacks reach them): the real player, not the fake one. */
    private static boolean listed(ServerPlayer p) {
        return p.serverLevel().players().contains(p);
    }

    /** A health buffer so the boss's hits between two heals can't kill the test player; returns a restorer. */
    private static Runnable buffer(ServerPlayer p) {
        AttributeInstance health = p.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.removeModifier(HP_BUFFER);
            health.addTransientModifier(new AttributeModifier(HP_BUFFER, 2000, AttributeModifier.Operation.ADD_VALUE));
            p.setHealth(p.getMaxHealth());
        }
        return () -> {
            if (health != null) {
                health.removeModifier(HP_BUFFER);
            }
            p.setHealth(Math.min(p.getHealth(), p.getMaxHealth()));
            p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            p.removeEffect(MobEffects.BLINDNESS);
            com.runecraft.combat.PoisonService.cure(p, true, 0, ActionManager.gameTick());
        };
    }

    /** One game tick of the boss's script and the world's boss mechanics (telegraphed attacks, hazards). */
    private static long tick(DataBossEntity boss, ServerPlayer p, long now) {
        ActionManager.advanceForSelfTest();
        if (boss.isAlive() && !boss.isRemoved()) {
            boss.setTarget(p);
            boss.script().gameTick(boss, now + 1);
            boss.script().tick(boss);
        }
        Mechanics.tickForSelfTest();
        return now + 1;
    }

    private static List<DataBossEntity> around(ServerPlayer p, String bossId) {
        EntityType<DataBossEntity> type = ModEntities.DATA_BOSS_TYPES.get(bossId);
        return type == null ? List.of() : p.serverLevel().getEntities(type, new AABB(p.blockPosition()).inflate(64),
                e -> e.isAlive() && !e.isRemoved());
    }

    // ------------------------------------------------------------------ Zulrah

    /**
     * Zulrah through a whole rotation: every phase surfaces in its form and spot (each form a separate entity, followed across
     * the swaps), can't be hit while it surfaces, performs its attacks, venom cloud barrages and snakeling orbs, and after the
     * rotation's last phase the next rotation begins with the green ranged phase in the middle (OSRS Wiki rotations).
     */
    static String zulrahRotation(DataBossEntity first, ServerPlayer p) {
        expect(first.script() instanceof ZulrahScript, "Zulrah has no Zulrah script");
        List<String> events = new ArrayList<>();
        ZulrahScript.trace = events::add;
        Runnable restore = buffer(p);
        Set<UUID> seen = new HashSet<>();
        DataBossEntity boss = first;
        long now = ActionManager.gameTick();
        int rotation = -1;
        List<Step> rot = null;
        int surfaced = 0;
        int forms = 0;
        try {
            for (int t = 0; t < 3000; t++) {
                if (!boss.isAlive() || boss.isRemoved()) {
                    DataBossEntity next = null;
                    for (String id : new String[]{"zulrah", "zulrah_magma", "zulrah_tanzanite"}) {
                        for (DataBossEntity e : around(p, id)) {
                            if (!seen.contains(e.getUUID()) && e != boss) {
                                next = e;
                            }
                        }
                    }
                    expect(next != null, "Zulrah dived and no next form surfaced (events " + events + ")");
                    boss = next;
                    forms++;
                }
                seen.add(boss.getUUID());
                int before = events.size();
                now = tick(boss, p, now);
                p.setHealth(p.getMaxHealth());
                for (int i = before; i < events.size(); i++) {
                    String e = events.get(i);
                    if (!e.startsWith("surface ")) {
                        continue;
                    }
                    String[] parts = e.split(" ");
                    String[] pos = parts[3].split("/");
                    int r = Integer.parseInt(pos[0]);
                    int s = Integer.parseInt(pos[1]);
                    expect(!boss.script().isVulnerable(boss), "Zulrah could be hit while surfacing (" + e + ")");
                    if (rot == null) {
                        rotation = r;
                        rot = ZulrahVorkath.ROTATIONS.get(r);
                        expect(s == 0, "the fight began at phase " + s + ", not the opening");
                    } else if (surfaced >= rot.size()) {
                        // the next rotation's first phase: green, in the middle (the wiki's last phase of every rotation)
                        expect(s == 0 && parts[1].equals("SERPENTINE") && parts[2].equals("CENTRE"),
                                "after the rotation came " + e + " instead of the green phase in the middle");
                        surfaced++;
                        break;
                    }
                    Step want = rot.get(surfaced);
                    expect(r == rotation && s == surfaced, "phase " + surfaced + " of rotation " + (rotation + 1) + " surfaced as " + e);
                    expect(parts[1].equals(want.form().name()) && parts[2].equals(want.spot().name()),
                            "phase " + (surfaced + 1) + " of rotation " + (rotation + 1) + " should be " + want.form() + " at "
                                    + want.spot() + ", was " + e);
                    expect(boss.def() != null && boss.def().id.equals(want.form().bossId),
                            "phase " + (surfaced + 1) + " is " + want.form() + " but the entity is " + (boss.def() == null ? "?" : boss.def().id));
                    surfaced++;
                }
                if (rot != null && surfaced > rot.size()) {
                    break;
                }
            }
            expect(rot != null, "Zulrah never surfaced");
            expect(surfaced > rot.size(), "only " + surfaced + " of the rotation's " + rot.size() + " phases surfaced in 3000 ticks");
            // Every orb and barrage of the rotation was thrown: the trace has one event per action.
            int clouds = 0;
            int orbs = 0;
            for (Step s : rot) {
                clouds += s.cloudBarrages();
                orbs += s.snakelingOrbs();
            }
            int seenClouds = 0;
            int seenOrbs = 0;
            int phase = 0;
            for (String e : events) {
                if (e.startsWith("surface ")) {
                    if (++phase > rot.size()) {
                        break;
                    }
                } else if (e.equals("cloud")) {
                    seenClouds++;
                } else if (e.equals("snakeling")) {
                    seenOrbs++;
                }
            }
            expect(seenClouds == clouds, "rotation " + (rotation + 1) + " threw " + seenClouds + " venom cloud barrages, expected " + clouds);
            expect(seenOrbs == orbs, "rotation " + (rotation + 1) + " threw " + seenOrbs + " snakeling orbs, expected " + orbs);
            return "rotation " + (rotation + 1) + ": " + rot.size() + " phases in order, " + forms + " form swaps, " + clouds
                    + " cloud barrages, " + orbs + " snakeling orbs, then the green phase in the middle";
        } finally {
            ZulrahScript.trace = null;
            restore.run();
            for (String id : new String[]{"zulrah", "zulrah_magma", "zulrah_tanzanite"}) {
                around(p, id).forEach(DataBossEntity::discard);
            }
            EntityType<?> snake = ModEntities.typeById("snakeling");
            if (snake != null) {
                p.serverLevel().getEntities(snake, new AABB(p.blockPosition()).inflate(64), e -> true).forEach(net.minecraft.world.entity.Entity::discard);
            }
        }
    }

    // ------------------------------------------------------------------ Vorkath

    /**
     * Vorkath's specials: the acid phase (half damage taken, the acid pool under the player hurts, it ends after the rapid
     * fire), the frozen phase (the player is frozen, Vorkath can't be hurt while the zombified spawn lives, killing it frees
     * both), a spawn left alone explodes on the player, and the dragonfire bomb lands on the player's tile.
     */
    static String vorkathSpecials(DataBossEntity boss, ServerPlayer p) {
        expect(boss.script() instanceof VorkathScript, "Vorkath has no Vorkath script");
        VorkathScript v = (VorkathScript) boss.script();
        Runnable restore = buffer(p);
        long now = ActionManager.gameTick();
        StringBuilder out = new StringBuilder();
        try {
            // Acid phase.
            v.startAcid(boss, p);
            expect(v.acidPhase(), "the acid phase didn't start");
            int half = boss.modifyIncoming(p, 40, AttackType.RANGED);
            expect(half == 20, "Vorkath took " + half + " of a 40 during the acid phase (expected half)");
            float hp = p.getHealth();
            for (int i = 0; i < ZulrahVorkath.RAPID_FIRE_SHOTS + 2 && v.acidPhase(); i++) {
                now = tick(boss, p, now);
            }
            expect(!v.acidPhase(), "the acid phase never ended");
            expect(p.getHealth() < hp, "standing still in the acid phase hurt nothing (the pool under the player)");
            out.append("acid: half damage, ").append(Math.round(hp - p.getHealth())).append(" taken standing still");
            p.setHealth(p.getMaxHealth());
            int full = boss.modifyIncoming(p, 40, AttackType.RANGED);
            expect(full == 40, "Vorkath still took " + full + " of a 40 after the acid phase");
            // Frozen phase: the spawn killed.
            v.startSpawn(boss, p);
            Mob spawn = v.spawn();
            expect(spawn != null && spawn.isAlive(), "no zombified spawn");
            expect(!boss.script().isVulnerable(boss), "Vorkath could be hurt while its spawn lived");
            expect(p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the icy breath didn't freeze the player");
            double away = Math.sqrt(spawn.distanceToSqr(p));
            CombatHandler.applyDamage(p, spawn, Math.round(spawn.getMaxHealth()) + 10);
            now = tick(boss, p, now);
            expect(v.spawn() == null, "the killed spawn is still tracked");
            expect(boss.script().isVulnerable(boss), "Vorkath stayed immune after its spawn died");
            expect(!p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the player stayed frozen after the spawn died");
            out.append("; spawn landed ").append(String.format(Locale.ROOT, "%.0f", away)).append(" blocks away, killed: freed");
            // Frozen phase: the spawn reaches the player and explodes.
            v.startSpawn(boss, p);
            spawn = v.spawn();
            expect(spawn != null, "no second zombified spawn");
            spawn.teleportTo(p.getX() + 0.5, p.getY(), p.getZ());
            hp = p.getHealth();
            v.cooldown = 10; // no attack of Vorkath's own in the same tick: only the explosion is measured
            now = tick(boss, p, now);
            expect(!spawn.isAlive() || spawn.isRemoved(), "the spawn next to the player didn't explode");
            float exploded = hp - p.getHealth();
            expect(exploded > 0 && exploded <= ZulrahVorkath.SPAWN_EXPLOSION_MAX, "the spawn's explosion hit " + exploded);
            out.append("; a spawn left alone exploded for ").append(Math.round(exploded));
            p.setHealth(p.getMaxHealth());
            now = tick(boss, p, now);
            // The dragonfire bomb, on the player's tile.
            v.bomb(boss, p);
            expect(v.strikesPending(), "the dragonfire bomb wasn't launched");
            v.cooldown = 20; // only the bomb lands while it is measured
            hp = p.getHealth();
            for (int i = 0; i <= ZulrahVorkath.VORKATH_BOMB.telegraphTicks(); i++) {
                now = tick(boss, p, now);
            }
            if (listed(p)) {
                float bomb = hp - p.getHealth();
                expect(bomb <= ZulrahVorkath.VORKATH_BOMB.maxHit(), "the bomb hit " + bomb);
                out.append("; bomb on the player's tile hit ").append(Math.round(bomb));
            } else {
                out.append("; bomb launched (its strike only reaches listed players)");
            }
            return out.toString();
        } finally {
            restore.run();
        }
    }

    // ------------------------------------------------------------------ Kalphite Queen

    /**
     * The Kalphite Queen's two forms: a lethal hit on the crawling form leaves it to shed its shell (immune, 20 ticks), the
     * airborne form emerges with full hitpoints and the Defence drained on the first form, and its death counts one Kalphite
     * Queen kill.
     */
    static String kalphiteQueenForms(DataBossEntity boss, ServerPlayer p) {
        expect(boss.script() instanceof ClassicScripts.KalphiteQueen, "the Kalphite Queen has no crawling-form script");
        ClassicScripts.KalphiteQueen kq = (ClassicScripts.KalphiteQueen) boss.script();
        Runnable restore = buffer(p);
        long now = ActionManager.gameTick();
        DataBossEntity airborne = null;
        try {
            SysCombat.drainDefence(boss, 30); // e.g. a Bandos godsword special
            int drainedDefence = SysCombat.defenceLevel(boss);
            int lethal = boss.modifyIncoming(p, 1000, AttackType.MAGIC);
            CombatHandler.applyDamage(p, boss, lethal);
            expect(boss.isAlive(), "the crawling form died instead of shedding its shell");
            expect(kq.transforming(), "a lethal hit didn't start her transformation");
            expect(!boss.script().isVulnerable(boss) && boss.modifyIncoming(p, 50, AttackType.MAGIC) == 0,
                    "she could be hurt while shedding her shell");
            int ticks = 0;
            while (airborne == null && ticks < ClassicBosses.KQ_TRANSFORM_TICKS + 5) {
                now = tick(boss, p, now);
                ticks++;
                List<DataBossEntity> found = around(p, "kalphite_queen_airborne");
                airborne = found.isEmpty() ? null : found.get(0);
            }
            expect(airborne != null, "the airborne form never emerged");
            expect(ticks >= ClassicBosses.KQ_TRANSFORM_TICKS, "the airborne form emerged after " + ticks + " ticks (wiki: 20)");
            expect(!boss.isAlive() || boss.isRemoved(), "the crawling form is still there beside the airborne one");
            expect(airborne.getHealth() >= airborne.getMaxHealth() - 0.01f, "the airborne form didn't emerge at full health");
            expect(SysCombat.defenceLevel(airborne) == drainedDefence, "her drained Defence (" + drainedDefence
                    + ") didn't carry over to the airborne form (" + SysCombat.defenceLevel(airborne) + ")");
            int kc = Players.data(p).killCount("kalphite_queen");
            int finish = airborne.modifyIncoming(p, 1000, AttackType.STAB);
            CombatHandler.applyDamage(p, airborne, finish);
            for (int i = 0; i < 3 && Players.data(p).killCount("kalphite_queen") == kc; i++) {
                now = tick(airborne, p, now);
                airborne.script().tick(airborne);
            }
            expect(Players.data(p).killCount("kalphite_queen") == kc + 1, "killing the airborne form didn't count a Kalphite Queen kill");
            return "shed her shell for " + ticks + " ticks (immune), emerged airborne at full health with Defence " + drainedDefence
                    + " carried over, its kill counted";
        } finally {
            restore.run();
            around(p, "kalphite_queen_airborne").forEach(DataBossEntity::discard);
        }
    }

    // ------------------------------------------------------------------ Corporeal Beast

    /**
     * The Corporeal Beast: full damage only from magic and Corpbane stabs (else half), the splash orb and its six splinters, the
     * stomp under it and the dark energy core after big hits.
     */
    static String corpMechanics(DataBossEntity boss, ServerPlayer p) {
        expect(boss.script() instanceof ClassicScripts.Corp, "the Corporeal Beast has no Corp script");
        ClassicScripts.Corp corp = (ClassicScripts.Corp) boss.script();
        Runnable restore = buffer(p);
        ItemStack held = p.getMainHandItem().copy();
        long now = ActionManager.gameTick();
        StringBuilder out = new StringBuilder();
        try {
            // Damage rules (its 2,000 hitpoints are scaled into Minecraft's health cap: compare averages).
            double whip = average(boss, p, "abyssal_whip", AttackType.SLASH);
            double spear = average(boss, p, "zamorakian_spear", AttackType.STAB);
            double hasta = average(boss, p, "rune_hasta", AttackType.STAB);
            double magic = average(boss, p, "kodai_wand", AttackType.MAGIC);
            expect(spear > whip * 1.6 && magic > whip * 1.6, String.format(Locale.ROOT,
                    "full damage should be about twice the halved: whip %.1f, spear stab %.1f, magic %.1f", whip, spear, magic));
            expect(hasta < spear * 0.7, String.format(Locale.ROOT, "a hasta isn't Corpbane but took %.1f (spear %.1f)", hasta, spear));
            out.append(String.format(Locale.ROOT, "whip %.1f / spear %.1f / magic %.1f / hasta %.1f per 100", whip, spear, magic, hasta));
            p.setItemInHand(InteractionHand.MAIN_HAND, held);
            // The splash orb: seven strikes (the orb and six splinters).
            int before = corp.splashes;
            corp.splash(boss, p);
            expect(corp.splashes == before + 1 && corp.strikesPending(), "the splash orb wasn't thrown");
            float hp = p.getHealth();
            for (int i = 0; i < 6; i++) {
                now = tick(boss, p, now);
            }
            expect(!corp.strikesPending(), "the splash orb's strikes never landed");
            if (listed(p)) {
                float took = hp - p.getHealth();
                expect(took <= ClassicBosses.CORP_SPLASH_DIRECT + 2 * ClassicBosses.CORP_SPLINTER_DIRECT,
                        "the splash orb hit for " + took);
                out.append("; splash orb landed (").append(Math.round(took)).append(" taken)");
                // The stomp: standing under it.
                p.setHealth(p.getMaxHealth());
                p.teleportTo(boss.getX(), boss.getY(), boss.getZ());
                hp = p.getHealth();
                int stomped = corp.stomp(boss);
                expect(stomped >= 1, "standing under the Corporeal Beast wasn't stomped");
                float stomp = hp - p.getHealth();
                expect(stomp >= ClassicBosses.CORP_STOMP_MIN && stomp <= ClassicBosses.CORP_STOMP.maxHit(), "the stomp hit " + stomp);
                out.append("; stomp ").append(Math.round(stomp));
                p.teleportTo(boss.getX() + 6, boss.getY(), boss.getZ()); // out from under it again
            } else {
                out.append("; splash orb landed (strikes and stomps only reach listed players)");
            }
            // The dark energy core: a 1/8 chance after a hit of 32 or more.
            Mob core = null;
            for (int i = 0; i < 120 && core == null; i++) {
                boss.setHealth(boss.getMaxHealth());
                p.setHealth(p.getMaxHealth());
                boss.modifyIncoming(p, 64, AttackType.MAGIC);
                now = tick(boss, p, now);
                core = corp.core();
            }
            expect(core != null, "no dark energy core after 120 big hits (1/8 each)");
            out.append("; dark energy core released");
            core.discard();
            return out.toString();
        } finally {
            p.setItemInHand(InteractionHand.MAIN_HAND, held);
            restore.run();
            Mob c = corp.core();
            if (c != null) {
                c.discard();
            }
        }
    }

    /** The damage per 100 the Corporeal Beast's rules let through, averaged over many hits with a weapon in hand. */
    private static double average(DataBossEntity boss, ServerPlayer p, String weapon, AttackType type) {
        p.setItemInHand(InteractionHand.MAIN_HAND, Inv.stack(weapon, 1));
        int sum = 0;
        for (int i = 0; i < 200; i++) {
            sum += boss.modifyIncoming(p, 100, type);
        }
        return sum / 200.0;
    }

    // ------------------------------------------------------------------ Sarachnis

    /**
     * Sarachnis: her healing strikes (5 melee, 10 ranged), the sticky web every fourth standard attack (the player is bound),
     * and her spawns at 66%: a melee and a magic spawn per player, which die with her.
     */
    static String sarachnisWeb(DataBossEntity boss, ServerPlayer p) {
        expect(boss.script() instanceof ClassicScripts.Sarachnis, "Sarachnis has no Sarachnis script");
        ClassicScripts.Sarachnis s = (ClassicScripts.Sarachnis) boss.script();
        Runnable restore = buffer(p);
        long now = ActionManager.gameTick();
        StringBuilder out = new StringBuilder();
        try {
            boss.setHealth(boss.getMaxHealth() - 40);
            float h = boss.getHealth();
            s.onHitPlayer(boss, p, 7, AttackType.RANGED);
            expect(Math.abs(boss.getHealth() - h - ClassicBosses.SARACHNIS_RANGED_HEAL) < 0.01, "a ranged hit healed her "
                    + (boss.getHealth() - h));
            h = boss.getHealth();
            s.onHitPlayer(boss, p, 7, AttackType.STAB);
            expect(Math.abs(boss.getHealth() - h - ClassicBosses.SARACHNIS_MELEE_HEAL) < 0.01, "a melee hit healed her "
                    + (boss.getHealth() - h));
            out.append("heals 10/5 on ranged/melee hits");
            // Her standard attacks: the fifth attack is the web.
            p.teleportTo(boss.getX() + 1.5, boss.getY(), boss.getZ());
            for (int i = 0; i < 40 && s.webs == 0; i++) {
                now = tick(boss, p, now);
                p.setHealth(p.getMaxHealth());
            }
            expect(s.webs >= 1, "no sticky web in 40 ticks of fighting");
            expect(p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the web didn't bind the player");
            p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            out.append("; web after four attacks bound the player");
            if (listed(p)) {
                int n = s.summonSpawns(boss);
                expect(n == 2, n + " spawns for one player (expected a melee and a magic one)");
                s.onRemoved(boss);
                out.append("; a melee and a magic spawn, gone with her");
            } else {
                out.append("; spawns are summoned per listed player");
            }
            return out.toString();
        } finally {
            restore.run();
            s.onRemoved(boss);
        }
    }

    // ------------------------------------------------------------------ Scurrius

    /** Scurrius's phases: at 80% he eats five bites at a food pile, at 30% he holds the centre; his bricks and rat waves. */
    static String scurriusFood(DataBossEntity boss, ServerPlayer p) {
        expect(boss.script() instanceof ClassicScripts.Scurrius, "Scurrius has no Scurrius script");
        ClassicScripts.Scurrius s = (ClassicScripts.Scurrius) boss.script();
        Runnable restore = buffer(p);
        long now = ActionManager.gameTick();
        StringBuilder out = new StringBuilder();
        List<Mob> rats = new ArrayList<>();
        try {
            now = tick(boss, p, now);
            boss.setHealth(boss.getMaxHealth() * 0.79f);
            float h = boss.getHealth();
            now = tick(boss, p, now);
            expect(s.phase == 2 && s.eating, "below 80% he didn't go to a food pile (phase " + s.phase + ")");
            for (int i = 0; i < 40 && s.eating; i++) {
                now = tick(boss, p, now);
                p.setHealth(p.getMaxHealth());
            }
            expect(!s.eating, "he never finished eating");
            expect(boss.getHealth() >= h + ClassicBosses.SCURRIUS_BITES * 5 - 0.01, "five bites healed him only "
                    + (boss.getHealth() - h));
            out.append("ate five bites (+").append(Math.round(boss.getHealth() - h)).append(")");
            boss.setHealth(boss.getMaxHealth() * 0.29f);
            now = tick(boss, p, now);
            expect(s.phase == 3, "below 30% he didn't move to the centre (phase " + s.phase + ")");
            out.append("; centre at 30%");
            s.bricks(boss, p);
            expect(s.strikesPending(), "the falling bricks weren't thrown");
            int waves = s.waves;
            s.summonRats(boss, p);
            EntityType<?> rat = ModEntities.typeById("scurrius_giant_rat");
            if (rat != null) {
                for (net.minecraft.world.entity.Entity e : p.serverLevel().getEntities(rat, new AABB(boss.blockPosition()).inflate(32),
                        e -> e.isAlive())) {
                    if (e instanceof Mob m) {
                        rats.add(m);
                    }
                }
            }
            expect(s.waves == waves + 1 && rats.size() >= ClassicBosses.SCURRIUS_RATS, "a rat wave brought " + rats.size() + " rats");
            out.append("; bricks fell, a wave of ").append(rats.size()).append(" giant rats");
            return out.toString();
        } finally {
            restore.run();
            rats.forEach(Mob::discard);
        }
    }

    /** Kills a living creature as the player through its own damage rules (heavy hits). */
    static boolean kill(ServerPlayer p, LivingEntity e) {
        for (int i = 0; i < 200 && e.isAlive(); i++) {
            CombatHandler.applyDamage(p, e, Math.max(20, Math.round(e.getMaxHealth())));
        }
        return !e.isAlive();
    }
}
