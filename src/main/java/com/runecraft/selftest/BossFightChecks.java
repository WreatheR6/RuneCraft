package com.runecraft.selftest;

import com.runecraft.RuneCraft;
import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.boss.Mechanics;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.MagicService;
import com.runecraft.entity.BossEntity;
import com.runecraft.entity.BossService;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.item.OsrsEquipmentItems;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Spell;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.godwars.GwdBosses;
import com.runecraft.osrs.data.Boss;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModEntities;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Real fights against the main bosses ({@code boss:fight:<id>}): unlike {@code boss:<id>}, which ticks a boss and kills it
 * outright, these let time pass ({@link ActionManager#advanceForSelfTest}) and check what a player would see:
 * <ol>
 *   <li>the boss attacks the player (any named attack, special or auto);</li>
 *   <li>the player's own melee, magic and ranged attacks (through the real combat paths, with a whip, Fire Surge and a magic
 *   shortbow, at 99s and with an accuracy boost so a few attacks are enough) take hitpoints off it;</li>
 *   <li>heavy hits through the boss's own damage rules kill it within {@value #KILL_TICKS} game ticks, its adds (minions,
 *   spawns, bodyguards, Nex's generals) being cleared and its scripted mechanic done for it when it can't be hurt
 *   ({@code BossSelfTests.assist}): its script says so, or the player's attacks roll damage that its rules take to nothing
 *   (Bryophyta behind her growthlings, the Abyssal Sire until its respiratory systems are gone); the player's own attack
 *   also goes in right after the first heavy hit that gets through, so a boss hurtable only late isn't finished first;</li>
 *   <li>the kill adds a kill count and leaves loot (on the ground or in the inventory).</li>
 * </ol>
 * The player keeps a big health buffer and is healed every tick, so it never dies. A boss that swaps itself for another
 * entity (Zulrah's and the Hydra's forms) is followed. Nothing else in the world ticks during the fight (minions only
 * act through the boss), and the fake player is not in the world's player list, so area attacks only reach a real player.
 */
final class BossFightChecks {
    /** Game ticks to see the boss attack and the player's attacks land before the kill starts anyway. */
    static final int ENGAGE_TICKS = 60;
    /** Game ticks to finish the boss with heavy hits. */
    static final int KILL_TICKS = 200;
    /** Game ticks between the player's attacks (a whip's speed). */
    private static final int PLAYER_SPEED = 4;
    private static final int MC = ActionManager.MC_TICKS_PER_GAME_TICK;
    /** Player attack-roll multiplier during the fight: a few attacks must be enough to see damage go through. */
    private static final double ACCURACY_BOOST = 4.0;
    private static final ResourceLocation HP_BUFFER = RuneCraft.id("selftest_fight_hp");

    /** Data bosses fought here: every non-raid boss of these categories, plus the Dagannoth Kings fought in their lair. */
    private static final Set<String> CATEGORIES = Set.of("Bosses", "Wilderness", "God Wars Dungeon");
    private static final Set<String> EXTRA = Set.of("dagannoth_rex", "dagannoth_prime", "dagannoth_supreme",
            "kreearra", "commander_zilyana", "nex"); // the God Wars generals stand in their dungeon chambers

    private static Recording recording;
    private static boolean hooked;

    private final TestContext c;

    private BossFightChecks(TestContext c) {
        this.c = c;
    }

    /** Which data bosses get a fight check (main bosses; not quest, raid, minigame or secondary forms). */
    static boolean fought(BossDef def) {
        if (def == null || java.util.Arrays.asList(GwdBosses.NEX_MINIONS).contains(def.id)) {
            return false;
        }
        return EXTRA.contains(def.id) || (!def.raidOnly && def.place != null && CATEGORIES.contains(def.category));
    }

    static List<SelfTest.Check> all(TestContext context) {
        BossFightChecks f = new BossFightChecks(context);
        List<SelfTest.Check> checks = new ArrayList<>();
        for (Boss b : ModEntities.BOSS_TYPES.keySet()) {
            if (b != Boss.COUNT_DRAYNOR && b != Boss.DELRITH) {
                checks.add(new SelfTest.Check("boss:fight:" + b.entityId(), () -> f.legacy(b)));
            }
        }
        for (String id : ModEntities.DATA_BOSS_TYPES.keySet()) {
            if (fought(ContentRegistry.boss(id))) {
                checks.add(new SelfTest.Check("boss:fight:" + id, () -> f.data(id)));
            }
        }
        // Phase walks: the fight played through every phase and mechanic, each step checked (BossSelfTests).
        checks.add(new SelfTest.Check("boss:walk:the_nightmare",
                () -> f.walk("the_nightmare", com.runecraft.content.bosses.BossSelfTests::nightmareWalk)));
        checks.add(new SelfTest.Check("boss:walk:phosanis_nightmare",
                () -> f.walk("phosanis_nightmare", com.runecraft.content.bosses.BossSelfTests::nightmareWalk)));
        checks.add(new SelfTest.Check("boss:walk:phantom_muspah",
                () -> f.walk("phantom_muspah", com.runecraft.content.bosses.BossSelfTests::muspahWalk)));
        return checks;
    }

    /**
     * A phase walk: the boss spawned next to the player (who keeps a big health buffer), then {@code run} plays its fight
     * through, phase by phase, and throws on the first broken step.
     */
    private String walk(String id, java.util.function.BiFunction<DataBossEntity, ServerPlayer, String> run) {
        BossDef def = ContentRegistry.boss(id);
        if (def == null) {
            throw new AssertionError("no BossDef " + id);
        }
        DataBossEntity boss = DataBossService.spawn(c.world, c.near(3, 0), def, c.player);
        if (boss == null) {
            throw new AssertionError("DataBossService.spawn returned null");
        }
        AttributeInstance health = c.player.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.removeModifier(HP_BUFFER);
            health.addTransientModifier(new AttributeModifier(HP_BUFFER, 2000, AttributeModifier.Operation.ADD_VALUE));
        }
        try {
            boss.setTarget(c.player);
            return run.apply(boss, c.player);
        } catch (IllegalStateException e) {
            throw new AssertionError(e.getMessage(), e);
        } finally {
            if (boss.isAlive()) {
                boss.discard();
            }
            if (health != null) {
                health.removeModifier(HP_BUFFER);
            }
            XpService.updateMaxHealth(c.player);
            c.player.setHealth(c.player.getMaxHealth());
        }
    }

    // ------------------------------------------------------------------ what the fight saw

    /** Damage rolls against the test player and attacks the bosses started, while a fight runs. */
    private static final class Recording {
        final ServerPlayer player;
        final Set<UUID> bosses = new HashSet<>();
        final Map<String, Integer> attacks = new LinkedHashMap<>();
        int otherRolls;
        int firstAttackTick = -1;
        int tick;
        /** Rolled damage of the player's last attack (before the boss's own rules), -1 when none was seen. */
        int lastRoll = -1;

        Recording(ServerPlayer player) {
            this.player = player;
        }

        void attack(Entity from, String name) {
            if (from != null && bosses.contains(from.getUUID())) {
                attacks.merge(name == null ? "?" : name, 1, Integer::sum);
                if (firstAttackTick < 0) {
                    firstAttackTick = tick;
                }
            }
        }
    }

    private static void hook() {
        if (hooked) {
            return;
        }
        hooked = true;
        CombatHooks.addDamageListener((player, attacker, attack, style, damage) -> {
            Recording r = recording;
            if (r == null || player != r.player) {
                return;
            }
            if (attacker != null && r.bosses.contains(attacker.getUUID())) {
                r.attack(attacker, attack != null ? attack : style != null ? style.name().toLowerCase(Locale.ROOT) : "typeless");
            } else {
                r.otherRolls++;
            }
        });
        CombatHooks.addModifier(new CombatHooks.PlayerAttackModifier() {
            @Override
            public double accuracy(ServerPlayer player, LivingEntity target, AttackType type) {
                Recording r = recording;
                return r != null && player == r.player ? ACCURACY_BOOST : 1.0;
            }

            @Override
            public int afterRoll(ServerPlayer player, LivingEntity target, AttackType type, int damage) {
                Recording r = recording;
                if (r != null && player == r.player) {
                    r.lastRoll = damage;
                }
                return damage;
            }
        });
    }

    // ------------------------------------------------------------------ entry points

    private String legacy(Boss boss) {
        if (boss.key() != null) {
            Inv.give(c.player, boss.key(), 1);
        }
        // A Barrows brother is fought as one summoned from his sarcophagus: in a run of the test's own (the player's run is
        // put back afterwards), so the kill must reach that run and its chest (fight()).
        int brother = com.runecraft.osrs.content.dungeons.Barrows.indexOf(boss);
        com.runecraft.osrs.content.dungeons.BarrowsRun saved = brother >= 0
                ? com.runecraft.content.dungeons.BarrowsHooks.startSelfTestRun(c.player, brother) : null;
        try {
            BlockPos at = c.near(0, 0);
            BossService.summon(c.player, c.world, at, boss);
            List<BossEntity> found = c.world.getEntities(ModEntities.BOSS_TYPES.get(boss), new AABB(at).inflate(48), e -> e.isAlive());
            if (found.isEmpty()) {
                throw new AssertionError("BossService.summon spawned nothing");
            }
            return fight(found.get(0), boss.entityId(), false, true);
        } finally {
            if (brother >= 0) {
                com.runecraft.content.dungeons.BarrowsHooks.endSelfTestRun(c.player, saved);
            }
        }
    }

    private String data(String id) {
        BossDef def = ContentRegistry.boss(id);
        if (def == null) {
            throw new AssertionError("no BossDef for a registered data boss type");
        }
        if (def.key != null) {
            Inv.give(c.player, def.key, 1);
        }
        // A Slayer boss: refused without its task, then fought on it (the task and its Slayer XP checked after the kill).
        SlayerBossGate gate = SlayerBossGate.open(c, def);
        try {
            BlockPos at = c.near(0, 0);
            EntityType<DataBossEntity> type = ModEntities.DATA_BOSS_TYPES.get(id);
            DataBossService.summon(c.player, c.world, at, def);
            List<DataBossEntity> found = c.world.getEntities(type, new AABB(at).inflate(48), e -> e.isAlive());
            String how = "";
            if (found.isEmpty()) {
                if (gate != null) {
                    throw new AssertionError(gate.refusals() + ", but not summoned on a " + com.runecraft.osrs.content.bosses
                            .SlayerBossRules.of(id).tasks().get(0) + " task either");
                }
                DataBossEntity spawned = DataBossService.spawn(c.world, c.near(3, 0), def, c.player);
                if (spawned == null) {
                    throw new AssertionError("DataBossService.spawn returned null");
                }
                found = List.of(spawned);
                how = "spawned directly, ";
            } else if (gate != null) {
                how = gate.refusals() + gate.afterSummon() + "; on task: ";
            }
            boolean passive = true;
            for (AttackDef a : def.attacks) {
                passive &= a.maxHit() <= 0;
            }
            String fought = fight(found.get(0), id, passive, com.runecraft.content.bosses.BossSelfTests.realAttacksHurt(id));
            return how + fought + (gate == null ? "" : gate.credit());
        } finally {
            if (gate != null) {
                gate.close();
            }
        }
    }

    // ------------------------------------------------------------------ the fight

    /**
     * @param passive     the boss never attacks (no attack is expected)
     * @param realAttacks whether ordinary weapon attacks can hurt it at all (else only its mechanic does: Zalcano)
     */
    private String fight(LivingEntity first, String id, boolean passive, boolean realAttacks) {
        hook();
        ServerPlayer p = c.player;
        // the weapon goes in the main hand first, so the runes don't take its slot
        p.setItemInHand(InteractionHand.MAIN_HAND, Inv.stack("abyssal_whip", 1));
        PlayerData data = Players.data(p);
        Recording rec = new Recording(p);
        Set<UUID> seen = new HashSet<>();
        for (LivingEntity e : bossesAround()) {
            seen.add(e.getUUID());
        }
        AttributeInstance health = p.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.removeModifier(HP_BUFFER);
            health.addTransientModifier(new AttributeModifier(HP_BUFFER, 2000, AttributeModifier.Operation.ADD_VALUE));
        }
        Mechanics.AttackListener before = Mechanics.attackListener;
        Mechanics.attackListener = (mob, target, attack) -> {
            before.onAttack(mob, target, attack);
            Recording r = recording;
            if (r != null && target == r.player) {
                r.attack(mob, attack.name());
            }
        };
        recording = rec;
        LivingEntity boss = first;
        List<String> forms = new ArrayList<>();
        Map<AttackType, int[]> landed = new LinkedHashMap<>(); // style -> {attacks, hits (hp taken off), rolled > 0 but no hp lost}
        int kcBefore = totalKills(data);
        int heldBefore = com.runecraft.content.bosses.BossSelfTests.heldLoot(id, p);
        DataBossService.LootRoll rollBefore = DataBossService.lastRoll();
        int logBefore = data.collectionLog().size();
        Set<UUID> items = new HashSet<>();
        int invBefore = 0;
        int tick = 0;
        int firstHitTick = realAttacks ? -1 : 0;
        int killStart = -1;
        int killedAt = -1;
        int invulnerableTicks = 0;
        int cleared = 0;
        Set<String> assists = new LinkedHashSet<>();
        Vec3 deathAt = boss.position();
        AttackType[] styles = {AttackType.SLASH, AttackType.MAGIC, AttackType.RANGED};
        int playerAttacks = 0;
        // A player attack rolled damage that the boss's own rules took to nothing (Bryophyta behind her growthlings, the
        // Abyssal Sire's first phase): it can't be hurt yet even when its script calls it vulnerable.
        boolean blocked = false;
        // The last heavy hit took hitpoints off it: the player's own attack goes in before the next one can finish it.
        boolean heavyLanded = false;
        try {
            rec.bosses.add(boss.getUUID());
            for (; tick < ENGAGE_TICKS + KILL_TICKS; tick++) {
                rec.tick = tick;
                if (boss.isAlive()) {
                    deathAt = boss.position();
                }
                // one game tick of the world as the boss sees it
                for (int m = 0; m < MC; m++) {
                    ActionManager.advanceForSelfTest();
                    if (boss.isAlive()) {
                        if (boss instanceof Mob mob && mob.getTarget() != p) {
                            mob.setTarget(p);
                        }
                        // As ServerLevel.tickNonPassenger does: an entity never counts its own ticks, and the bosses run
                        // their game tick (scripts, leash) every MC_TICKS_PER_GAME_TICK of them. Without this a boss
                        // summoned here kept tickCount 0 and ran a game tick on every Minecraft tick: twelve a game tick
                        // (Zulrah swapped forms every one to three game ticks and ran out of forms before it could die).
                        boss.tickCount++;
                        boss.tick();
                    }
                    Mechanics.tickForSelfTest();
                    p.setHealth(p.getMaxHealth());
                }
                if (!boss.isAlive()) {
                    if (totalKills(data) > kcBefore) {
                        killedAt = tick;
                        break;
                    }
                    LivingEntity next = nextForm(seen);
                    if (next == null || forms.size() >= 12) {
                        String how = boss.isRemoved() && boss.getHealth() > 0 ? "vanished" : "died";
                        throw new AssertionError(how + " after " + tick + " game ticks without a kill count"
                                + (forms.isEmpty() ? "" : " (forms " + String.join(" -> ", forms) + ")"));
                    }
                    forms.add(name(boss) + "->" + name(next) + "@" + tick);
                    boss = next;
                    seen.add(boss.getUUID());
                    rec.bosses.add(boss.getUUID());
                    continue;
                }
                seen.add(boss.getUUID());
                boolean engaged = (passive || rec.firstAttackTick >= 0) && firstHitTick >= 0;
                if (killStart < 0 && ((engaged && tick >= 8) || tick >= ENGAGE_TICKS)) {
                    // on to the kill: what lands on the ground or in the pack from here on is loot
                    killStart = tick;
                    for (ItemEntity e : c.world.getEntitiesOfClass(ItemEntity.class, around(), e -> true)) {
                        items.add(e.getUUID());
                    }
                    invBefore = inventoryCount(p);
                }
                boolean hurtable = !(boss instanceof DataBossEntity d) || d.script().isVulnerable(d);
                // the player's own attacks, cycling melee, magic and ranged, until one has taken hitpoints off it
                if (firstHitTick < 0 && (tick % PLAYER_SPEED == 0 || heavyLanded)) {
                    AttackType style = styles[playerAttacks++ % styles.length];
                    float hp = boss.getHealth();
                    rec.lastRoll = -1;
                    int carried = inventoryCount(p);
                    playerAttack(p, boss, style);
                    if (killStart >= 0) {
                        invBefore += inventoryCount(p) - carried; // runes spent on a cast are not loot taken
                    }
                    int[] s = landed.computeIfAbsent(style, k -> new int[3]);
                    s[0]++;
                    if (boss.getHealth() < hp || !boss.isAlive()) {
                        s[1]++;
                        firstHitTick = tick;
                    } else if (rec.lastRoll > 0) {
                        s[2]++;
                        blocked = true;
                    }
                }
                if (killStart < 0) {
                    if (!hurtable || blocked && firstHitTick < 0) {
                        assist(boss, p, assists);
                        if (++invulnerableTicks % 10 == 0) {
                            cleared += clearAdds(p, rec.bosses, seen); // what shields it (totems, minions, spawns)
                        }
                    }
                    continue;
                }
                if (tick - killStart >= KILL_TICKS) {
                    break;
                }
                // the kill: a heavy hit through the boss's own damage rules every game tick
                float hp = boss.getHealth();
                heavyLanded = false;
                if (!heavyHit(p, boss)) {
                    invulnerableTicks++;
                    if (invulnerableTicks % 5 == 0) {
                        cleared += clearAdds(p, rec.bosses, seen);
                    }
                    assist(boss, p, assists);
                } else if (boss.isAlive() && boss.getHealth() >= hp) {
                    invulnerableTicks++;
                } else {
                    heavyLanded = true;
                }
            }
            if (rec.firstAttackTick < 0 && !passive && c.realPlayer()) {
                throw new AssertionError("never attacked the player in " + tick + " game ticks"
                        + (rec.otherRolls > 0 ? " (" + rec.otherRolls + " hits from other sources)" : ""));
            }
            if (firstHitTick < 0) {
                throw new AssertionError("the player's attacks never took hitpoints off it: " + describe(landed)
                        + (killedAt >= 0 ? " (killed by the heavy hits @" + killedAt + ")" : ""));
            }
            if (killedAt < 0) {
                String why = boss.isAlive() ? String.format(Locale.ROOT, "%.0f%% health left", boss.getHealth() * 100 / boss.getMaxHealth())
                        : "gone without a kill count";
                throw new AssertionError("not killed in " + KILL_TICKS + " game ticks of heavy hits: " + why + ", "
                        + invulnerableTicks + " ticks it couldn't be hurt" + (assists.isEmpty() ? "" : ", assists " + assists)
                        + (cleared > 0 ? ", " + cleared + " adds cleared" : "")
                        + (forms.isEmpty() ? "" : ", forms " + String.join(" ", forms)));
            }
        } finally {
            recording = null;
            Mechanics.attackListener = before;
            if (health != null) {
                health.removeModifier(HP_BUFFER);
            }
            XpService.updateMaxHealth(p);
            p.setHealth(p.getMaxHealth());
        }
        int kc = totalKills(data) - kcBefore;
        int ground = 0;
        for (ItemEntity e : c.world.getEntitiesOfClass(ItemEntity.class, new AABB(BlockPos.containing(deathAt)).inflate(12), e -> true)) {
            if (!items.contains(e.getUUID())) {
                ground++;
            }
        }
        int pocketed = Math.max(0, inventoryCount(p) - invBefore);
        // loot kept for the player elsewhere (the Doom's delve loot waits in the burrow hole until claimed)
        pocketed += Math.max(0, com.runecraft.content.bosses.BossSelfTests.heldLoot(id, p) - heldBefore);
        String lootNote = "";
        int brother = boss instanceof BossEntity legacyBoss ? com.runecraft.osrs.content.dungeons.Barrows.indexOf(legacyBoss.boss()) : -1;
        if (brother >= 0) {
            // OSRS: the Barrows brothers drop nothing themselves; the Barrows chest rolls the run's loot (one roll per brother
            // killed, plus one). The kill must count in the player's run (legacy() summoned him in one, as his sarcophagus
            // does), and the chest of that run must give loot.
            com.runecraft.osrs.content.dungeons.BarrowsRun run = com.runecraft.content.dungeons.BarrowsHooks.selfTestRun(p);
            if (!run.killed(brother)) {
                throw new AssertionError("killed (kc +" + kc + "), but the kill didn't count in the player's Barrows run (summoned "
                        + Integer.toBinaryString(run.summonedMask()) + ", killed " + Integer.toBinaryString(run.killedMask())
                        + "), so the chest would not roll for him");
            }
            List<com.runecraft.osrs.content.dungeons.BarrowsRun.Reward> chest = List.of();
            for (int roll = 0; roll < 20 && chest.isEmpty(); roll++) {
                chest = run.rollRewards(new java.util.Random(roll));
            }
            if (chest.isEmpty()) {
                throw new AssertionError("killed (kc +" + kc + "), but the Barrows chest of the run he died in gave nothing in 20 opens");
            }
            lootNote = " (a brother drops nothing himself: the kill counted in the run, whose chest gives " + chest.get(0).count()
                    + " " + chest.get(0).item() + (chest.size() > 1 ? " and " + (chest.size() - 1) + " more" : "") + ")";
        } else if (ground + pocketed == 0) {
            // OSRS can leave a kill empty only through a shared table's "nothing" slot (the gem drop table's 63/128): the
            // kill's roll landed on drop lines that gave no item. Any other empty kill is a bug (an empty roll of the boss's
            // own table, or loot that landed out of the player's sight).
            DataBossService.LootRoll roll = DataBossService.lastRoll();
            boolean rolled = roll != null && roll != rollBefore && id.equals(roll.bossId());
            if (!rolled || roll.lines() == 0 || roll.items() > 0) {
                throw new AssertionError("killed (kc +" + kc + ") but no loot: nothing on the ground or in the inventory"
                        + (rolled ? " (its roll: " + roll.lines() + " drop lines, " + roll.items() + " items)" : ""));
            }
            lootNote = " (its roll landed on a shared table's nothing slot)";
        }
        String attacked = rec.firstAttackTick >= 0 ? "attacked @" + rec.firstAttackTick + " " + rec.attacks
                : passive ? "passive" : "no attack reached the fake player (area attacks only reach listed players)";
        return attacked + ", hit it @" + firstHitTick + " " + (realAttacks ? describe(landed) : "(only its mechanic hurts it)")
                + ", killed @" + killedAt + (forms.isEmpty() ? "" : " forms " + String.join(" ", forms))
                + (assists.isEmpty() ? "" : " assists " + assists) + (cleared > 0 ? " adds " + cleared : "")
                + ", kc +" + kc + ", loot " + ground + (pocketed > 0 ? "+" + pocketed + " pocketed" : "") + lootNote
                + ", log +" + (data.collectionLog().size() - logBefore);
    }

    /** Does the boss's scripted mechanic for the player while it can't be hurt (BossSelfTests.assist). */
    private static void assist(LivingEntity boss, ServerPlayer p, Set<String> assists) {
        if (boss instanceof DataBossEntity d) {
            String a = com.runecraft.content.bosses.BossSelfTests.assist(d, p);
            if (a != null) {
                assists.add(a);
            }
        }
    }

    private static String describe(Map<AttackType, int[]> landed) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<AttackType, int[]> e : landed.entrySet()) {
            int[] s = e.getValue();
            sb.append(sb.length() == 0 ? "" : " ").append(e.getKey().name().toLowerCase(Locale.ROOT)).append(' ').append(s[1]).append('/')
                    .append(s[0]);
            if (s[2] > 0) {
                sb.append(" (").append(s[2]).append(" blocked)");
            }
        }
        return sb.toString();
    }

    /** One player attack through the real combat path of a style (the weapon for it is put in the main hand). */
    private void playerAttack(ServerPlayer p, LivingEntity boss, AttackType style) {
        PlayerData data = Players.data(p);
        data.lastAttackTick = ActionManager.mcTick() - 10_000;
        switch (style) {
            case MAGIC -> {
                // The runes of this one cast, handed over right before it: however many casts the fight takes (one attack in
                // three, up to one attack a game tick once heavy hits land), the pack never runs dry nor fills up with runes
                // (stacks of 64: carrying 200 casts' worth, 58 slots, dropped Zalcano's imbued tephra and the loot on the floor).
                for (Map.Entry<com.runecraft.osrs.Rune, Integer> e : Spell.FIRE_SURGE.runes().entrySet()) {
                    int lack = e.getValue() - Inv.count(p, e.getKey().itemId());
                    if (lack > 0) {
                        Inv.give(p, e.getKey().itemId(), lack);
                    }
                }
                p.setItemInHand(InteractionHand.MAIN_HAND, Inv.stack("staff_of_fire", 1));
                MagicService.setAutocast(p, Spell.FIRE_SURGE);
                MagicService.autocastAttack(p, boss);
            }
            case RANGED -> {
                p.setItemInHand(InteractionHand.MAIN_HAND, Inv.stack("magic_shortbow", 1));
                if (Inv.item("rune_arrow") instanceof OsrsEquipmentItems.Arrow arrowItem) {
                    ItemStack ammo = Inv.stack("rune_arrow", 1);
                    AbstractArrow arrow = arrowItem.createArrow(c.world, ammo, p, null);
                    Holder<DamageType> hit = c.world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                            .getHolderOrThrow(CombatHandler.OSRS_HIT);
                    boss.hurt(new DamageSource(hit, arrow, p), 1f);
                }
            }
            default -> {
                p.setItemInHand(InteractionHand.MAIN_HAND, Inv.stack("abyssal_whip", 1));
                boss.hurt(CombatHandler.source(p), 1f);
            }
        }
    }

    /**
     * A heavy hit (a sixth of the boss's hitpoints, at least 50) through the boss's own rules (immunities, damage caps,
     * shields, forms), trying melee, magic and ranged until one is taken. False when no style could hurt it now.
     */
    static boolean heavyHit(ServerPlayer p, LivingEntity boss) {
        if (!(boss instanceof OsrsMonsterEntity monster)) {
            return CombatHandler.applyDamage(p, boss, 50) > 0;
        }
        int hp = boss instanceof DataBossEntity d ? d.trueHitpoints() : Math.round(boss.getMaxHealth());
        int big = Math.max(50, hp / 6);
        for (AttackType type : new AttackType[]{AttackType.SLASH, AttackType.MAGIC, AttackType.RANGED}) {
            if (boss instanceof DataBossEntity d && !d.script().isVulnerable(d)) {
                return false;
            }
            int dmg = monster.modifyIncoming(p, big, type);
            if (dmg > 0) {
                CombatHandler.applyDamage(p, boss, dmg);
                return true;
            }
        }
        return false;
    }

    /** Kills the living non-player creatures around the boss that aren't one of its forms (minions, spawns, bodyguards). */
    private int clearAdds(ServerPlayer p, Set<UUID> forms, Set<UUID> seen) {
        int n = 0;
        for (LivingEntity e : c.world.getEntitiesOfClass(LivingEntity.class, around(),
                e -> e.isAlive() && !(e instanceof Player) && !forms.contains(e.getUUID()))) {
            if (e instanceof DataBossEntity d) {
                seen.add(d.getUUID()); // a companion boss (Nex's generals, Dawn, Eldric): never taken for a next form
            }
            for (int i = 0; i < 4 && e.isAlive(); i++) {
                CombatHandler.applyDamage(p, e, Math.max(1, (int) Math.ceil(e.getHealth())));
            }
            if (e.isAlive()) {
                e.kill();
            }
            n++;
        }
        return n;
    }

    private AABB around() {
        return new AABB(TestContext.PLATFORM).inflate(48);
    }

    /** A boss entity around the platform that hasn't been seen yet (the next form), or null. */
    private LivingEntity nextForm(Set<UUID> seen) {
        for (LivingEntity e : bossesAround()) {
            if (!seen.contains(e.getUUID())) {
                return e;
            }
        }
        return null;
    }

    private List<LivingEntity> bossesAround() {
        return c.world.getEntitiesOfClass(LivingEntity.class, around(),
                e -> e.isAlive() && (e instanceof DataBossEntity || e instanceof BossEntity));
    }

    private static String name(LivingEntity e) {
        if (e instanceof DataBossEntity d && d.def() != null) {
            return d.def().id;
        }
        return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
    }

    private static int totalKills(PlayerData data) {
        int total = 0;
        for (int kc : data.killCounts().values()) {
            total += kc;
        }
        return total;
    }

    private static int inventoryCount(ServerPlayer p) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            n += p.getInventory().getItem(i).getCount();
        }
        return n;
    }
}
