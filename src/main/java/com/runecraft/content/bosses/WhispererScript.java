package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.content.systems.CombatAchievementService;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.bosses.BossMechanics;
import com.runecraft.osrs.content.bosses.Dt2Bosses;
import com.runecraft.osrs.content.bosses.WhispererRules;
import com.runecraft.osrs.content.systems.CombatAchievements;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The Whisperer (OSRS Wiki "The Whisperer/Strategies"; the Desert Treasure II quest version hits for less).
 * <ul>
 *   <li>Volleys of three projectiles a tick apart (five awakened), soul bolts (magic, 36) and tentacle lashes (ranged, 40),
 *   all one style before her first special, then two and one, after the third alternating; protection prayers block them.
 *   After each volley her tentacles converge on the player's tile: move off it or take 20 (awakened 30) and lose as much
 *   sanity.</li>
 *   <li>Next to her she whacks with her tentacles instead: two hits, the second the same as the first, through prayer (which
 *   only trims it) - stay away from her.</li>
 *   <li>At 80, 55 and 30% (awakened about every tenth of her 2,700 hitpoints) a special, in the order Corrupted Seeds,
 *   Screech, Soul Siphon (or starting at the Screech), taking 70% less damage meanwhile (60% from inside the Shadow Realm).
 *   The blackstone fragment glows: activating it steps into the Shadow Realm (sanity and prayer drain there), activating it
 *   again steps out, and the special's end brings everyone back.
 *   <b>Screech</b>: from the south end of the arena, three blasts (five awakened) of 45 (70) at anyone she can see - hide
 *   behind the floating columns, which crumble as she screeches and break when someone hides behind them (their health,
 *   20/40/60, only shows in the realm);
 *   <b>Soul Siphon</b>: lost souls in four sets (Vita 2: heal 20% hitpoints, Oratio 3: 20% prayer, Sanitas 3: 15% sanity,
 *   Mors 4: 50 to her, 75 if every soul dies), only hurt from the realm; kill a full set within 20 ticks or take 50 while she
 *   heals 100 (awakened: one more soul per set, alone at a cardinal point; every soul within 35 ticks, or she heals 100 for
 *   each set left); <b>Corrupted Seeds</b>: one of two symmetric layouts; step on every light
 *   seed (told apart only in the realm) before time runs out (or take 75 and lose 75% sanity), never on a dark one (15-25,
 *   awakened 60-75); clean, it restores 15% sanity. After each special her binding orb holds the player for ~8 seconds
 *   while she comes for them.</li>
 *   <li>At 0 hitpoints she heals 140 (awakened 250), pulls everyone into the Shadow Realm and attacks every 2 ticks, one
 *   projectile at a time (ranged first, switching every two; awakened magic first, switching every attack), a tentacle
 *   with each.</li>
 *   <li>Sanity ({@link WhispererRules}): shown on the action bar; at 0% the darkness deals 5, 10, 20... every 2 ticks.
 *   Dark Memories counts each pre-enrage tick a player spends in the realm.</li>
 * </ul>
 */
final class WhispererScript extends ScriptedBoss {
    private enum Special { SEEDS, SCREECH, SIPHON }

    private record Seed(Vec3 pos, boolean light) {
    }

    private static final class Column {
        final Vec3 pos;
        final List<BlockPos> blocks = new ArrayList<>();
        int health;

        Column(Vec3 pos, int health) {
            this.pos = pos;
            this.health = health;
        }
    }

    private static final String NBT_COLUMNS = "WhispererColumns";
    /** Blocks per seed-layout tile offset (the arena is 13 blocks across its radius). approx */
    private static final double SEED_SPACING = 1.5;
    private static final DustParticleOptions LIGHT_SEED = new DustParticleOptions(new Vector3f(0.55f, 1.0f, 0.4f), 1.4f);
    private static final DustParticleOptions DARK_SEED = new DustParticleOptions(new Vector3f(0.1f, 0.35f, 0.1f), 1.4f);
    private static final DustParticleOptions HIDDEN_SEED = new DustParticleOptions(new Vector3f(0.45f, 0.45f, 0.45f), 1.2f);

    /** Max hit scale: 1 for the boss, below 1 for the weaker Desert Treasure II quest version. */
    private final double scale;
    /** 0: seeds first, 1: screech first (-1 until her first game tick). */
    private int rotation = -1;
    private int specialsDone;
    private Special special;
    private int specialTick;
    private boolean nextMagic;
    private final List<AttackDef> volley = new ArrayList<>();
    private boolean enraged;
    private boolean enragePending;
    private int enrageAttacks;
    /** Game ticks she walks after the player once her binding orb has landed. */
    private int chase;
    /** The second hit of her tentacle whack (the first's damage), landing next tick, or -1. */
    private int echo = -1;
    private ServerPlayer echoTarget;
    private final List<Column> columns = new ArrayList<>();
    private int screechesLeft;
    private int nextScreech;
    private final List<Mob> souls = new ArrayList<>();
    private final Map<Mob, Integer> soulSet = new HashMap<>();
    private final Set<Integer> setsDone = new LinkedHashSet<>();
    private final List<Seed> seeds = new ArrayList<>();
    /** Players who stepped on a dark seed during the current Corrupted Seeds (no sanity back for them). */
    private final Set<UUID> steppedDark = new HashSet<>();
    /** Column blocks saved with her when she was unloaded mid-Screech: cleared on her first game tick back. */
    private long[] savedColumns;
    /** Each fighting player's sanity, and who stands in the Shadow Realm now. */
    private final Map<UUID, WhispererRules.Sanity> sanity = new HashMap<>();
    private final Set<UUID> realm = new HashSet<>();
    /** Specials that ran out on the players (seeds erupting, a completed chant): for the self-test walk. */
    private int failures;

    WhispererScript() {
        this(1.0);
    }

    WhispererScript(double scale) {
        this.scale = scale;
    }

    @Override
    protected double multiplier(DataBossEntity boss) {
        return super.multiplier(boss) * scale;
    }

    @Override
    protected boolean driven() {
        return true;
    }

    @Override
    protected boolean wantsMelee(DataBossEntity boss, ServerPlayer target) {
        return chase > 0;
    }

    @Override
    protected boolean canAttack(DataBossEntity boss) {
        return special == null && !enragePending;
    }

    @Override
    protected int attackDelay(DataBossEntity boss, AttackDef attack) {
        if (enraged) {
            return 2;
        }
        return volley.isEmpty() ? super.attackDelay(boss, attack) : 1;
    }

    private int scaled(DataBossEntity boss, AttackDef a) {
        return (int) Math.round(maxHit(boss, a) * scale);
    }

    private int scaled(DataBossEntity boss, int normal, int awakened) {
        return (int) Math.round((boss.awakened() ? awakened : normal) * scale);
    }

    /** Players in the fight: those around her plus her target (which may be a test player). */
    private static List<ServerPlayer> fighters(DataBossEntity boss, ServerPlayer target) {
        List<ServerPlayer> list = new ArrayList<>(BossFx.players(boss, 24));
        if (target != null && target.isAlive() && !list.contains(target)) {
            list.add(target);
        }
        return list;
    }

    /** Her hitpoints in her own (variant's) terms. */
    private static double trueHp(DataBossEntity boss) {
        return percent(boss) / 100.0 * boss.trueHitpoints();
    }

    // ---------------- the Shadow Realm and sanity ----------------

    private WhispererRules.Sanity sanityOf(ServerPlayer p) {
        return sanity.computeIfAbsent(p.getUUID(), k -> new WhispererRules.Sanity());
    }

    boolean inRealm(ServerPlayer p) {
        return p != null && realm.contains(p.getUUID());
    }

    int sanity(ServerPlayer p) {
        return sanityOf(p).value();
    }

    /** Pre-enrage realm ticks counted for a player this fight (Dark Memories). */
    int realmTicks(ServerPlayer p) {
        return sanityOf(p).realmTicks();
    }

    private void drainSanity(ServerPlayer p, int amount) {
        if (p != null && amount > 0) {
            sanityOf(p).drain((int) Math.round(amount * scale));
        }
    }

    private void restoreSanity(ServerPlayer p, int amount) {
        if (p != null) {
            sanityOf(p).restore(amount);
        }
    }

    /** The blackstone fragment activated near her: into the Shadow Realm while it glows, or back out. */
    boolean toggleRealm(DataBossEntity boss, ServerPlayer p) {
        if (enraged || enragePending) {
            Msg.game(p, "The Whisperer holds you in the Shadow Realm.");
            return false;
        }
        if (realm.contains(p.getUUID())) {
            leaveRealm(p);
            Msg.game(p, "You step out of the Shadow Realm.");
            return true;
        }
        if (!WhispererRules.fragmentGlows(special != null, enraged)) {
            Msg.game(p, "Your blackstone fragment isn't glowing.");
            return false;
        }
        enterRealm(boss, p);
        Msg.action(p, "You step into the Shadow Realm...");
        return true;
    }

    private void enterRealm(DataBossEntity boss, ServerPlayer p) {
        realm.add(p.getUUID());
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20 * 60 * 3, 0, false, false));
        BossFx.world(boss).sendParticles(ParticleTypes.SCULK_SOUL, p.getX(), p.getY() + 1, p.getZ(), 12, 0.4, 0.6, 0.4, 0.02);
        BossFx.sound(p, net.minecraft.sounds.SoundEvents.SCULK_SHRIEKER_SHRIEK, 1.4f);
    }

    private void leaveRealm(ServerPlayer p) {
        if (realm.remove(p.getUUID())) {
            p.removeEffect(MobEffects.DARKNESS);
        }
    }

    private void leaveRealmAll(DataBossEntity boss) {
        for (UUID id : new ArrayList<>(realm)) {
            if (BossFx.world(boss).getPlayerByUUID(id) instanceof ServerPlayer p) {
                leaveRealm(p);
                Msg.game(p, "The Shadow Realm fades around you.");
            }
        }
        realm.clear();
    }

    /** One game tick of everyone's sanity: realm drain and Dark Memories, restoration outside, the darkness at 0%. */
    private void tickSanity(DataBossEntity boss, ServerPlayer target) {
        List<ServerPlayer> here = fighters(boss, target);
        Set<UUID> ids = new HashSet<>();
        for (ServerPlayer p : here) {
            ids.add(p.getUUID());
        }
        // gone from the fight (teleported away, died): sanity is fully restored for their next visit
        sanity.keySet().removeIf(id -> !ids.contains(id));
        for (UUID id : new ArrayList<>(realm)) {
            if (!ids.contains(id)) {
                realm.remove(id);
                if (BossFx.world(boss).getPlayerByUUID(id) instanceof ServerPlayer gone) {
                    gone.removeEffect(MobEffects.DARKNESS);
                }
            }
        }
        for (ServerPlayer p : here) {
            WhispererRules.Sanity s = sanityOf(p);
            boolean in = enraged || realm.contains(p.getUUID());
            boolean wasInsane = s.insane();
            int dmg = s.tick(in, enraged, boss.awakened());
            if (in && !enraged) {
                BossFx.drainPrayer(p, boss.awakened() ? WhispererRules.REALM_DRAIN_AWAKENED : WhispererRules.REALM_DRAIN);
                CombatAchievementService.count(p, CombatAchievements.COUNT_SHADOW_REALM_TICKS, 1);
            }
            if (s.insane() && !wasInsane) {
                Msg.red(p, "The darkness of the Shadow Realm begins to consume your mind!");
            }
            if (dmg > 0) {
                CombatHandler.applyDamage(boss, p, dmg);
            }
            if (now % 2 == 0 || s.insane()) {
                ChatFormatting colour = s.value() > 50 ? ChatFormatting.AQUA : s.value() > 20 ? ChatFormatting.YELLOW : ChatFormatting.RED;
                p.displayClientMessage(Component.literal("Sanity: " + s.value() + "%" + (in ? "  (Shadow Realm)" : "")
                        + (WhispererRules.fragmentGlows(special != null, enraged) && !in ? "  - your fragment glows" : ""))
                        .withStyle(colour), true);
            }
        }
    }

    // ---------------- combat ----------------

    @Override
    protected int incoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
        if (enragePending) {
            return 0; // between her "death" and the enrage
        }
        double d = special != null ? damage * WhispererRules.specialDamage(inRealm(attacker)) : damage;
        int out = (int) d + (boss.getRandom().nextDouble() < d - (int) d ? 1 : 0);
        if (!enraged && !enragePending && out > 0) {
            int hp = boss.trueHitpoints();
            double mc = hp > MAX_MC_HEALTH ? out * (double) MAX_MC_HEALTH / hp : out;
            if (mc >= boss.getHealth()) {
                // 0 hitpoints the first time: she heals and enrages next tick instead of dying
                enragePending = true;
                boss.setHealth(1f);
                return 0;
            }
        }
        return out;
    }

    @Override
    protected AttackDef nextAttack(DataBossEntity boss, ServerPlayer target, double distance) {
        if (distance < 1.6 && echo < 0) {
            volley.clear(); // she cancels a volley for an immediate melee attack
            return Dt2Bosses.WHISPER_MELEE;
        }
        if (enraged) {
            enrageAttacks++;
            boolean magic = boss.awakened() ? enrageAttacks % 2 == 1 : (enrageAttacks - 1) / 2 % 2 == 1;
            tentacles(boss, target);
            return magic ? Dt2Bosses.WHISPER_MAGIC : Dt2Bosses.WHISPER_RANGED;
        }
        if (volley.isEmpty()) {
            for (boolean magic : BossMechanics.whispererVolley(specialsDone, boss.awakened(), nextMagic)) {
                volley.add(magic ? Dt2Bosses.WHISPER_MAGIC : Dt2Bosses.WHISPER_RANGED);
            }
            nextMagic = boss.getRandom().nextBoolean(); // approx: which style leads each volley
        }
        AttackDef a = volley.remove(0);
        if (volley.isEmpty()) {
            tentacles(boss, target);
        }
        return a;
    }

    /** Her tentacles surface around the player and converge on the tile they stood on: damage and as much sanity, once. */
    private void tentacles(DataBossEntity boss, ServerPlayer target) {
        int dmg = scaled(boss, BossMechanics.WHISPERER_TENTACLE, BossMechanics.WHISPERER_TENTACLE_AWAKENED);
        strike(boss, target.position(), 3, 0, 0.9, ParticleTypes.SQUID_INK, (hit, pos) -> {
            for (ServerPlayer h : hit) {
                CombatHandler.applyDamage(boss, h, dmg);
                sanityOf(h).drain(dmg);
            }
        });
    }

    @Override
    public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
        if (last != Dt2Bosses.WHISPER_MELEE) {
            return damage;
        }
        if (damage <= 0) {
            damage = boss.getRandom().nextInt(scaled(boss, Dt2Bosses.WHISPER_MELEE) + 1); // it always lands
        }
        if (BossFx.protects(player, Prayer.PROTECT_FROM_MELEE)) {
            damage = (int) Math.round(damage * BossMechanics.WHISPERER_MELEE_THROUGH_PRAYER);
        }
        echo = damage;
        echoTarget = player;
        return damage;
    }

    @Override
    protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
        ServerLevel w = BossFx.world(boss);
        if (savedColumns != null) {
            for (long pos : savedColumns) {
                clearColumnBlock(w, BlockPos.of(pos));
            }
            savedColumns = null;
        }
        if (rotation < 0) {
            rotation = boss.getRandom().nextInt(2);
            nextMagic = boss.getRandom().nextBoolean();
        }
        tickSanity(boss, target);
        if (echo >= 0) {
            if (echoTarget != null && echoTarget.isAlive() && echoTarget.distanceToSqr(boss) < 6 * 6) {
                CombatHooks.nameAttack(Dt2Bosses.WHISPER_MELEE.name(), AttackType.CRUSH);
                CombatHandler.applyDamage(boss, echoTarget, echo);
            }
            echo = -1;
            echoTarget = null;
        }
        if (enragePending) {
            enrage(boss, target);
            return;
        }
        if (chase > 0) {
            chase--;
        }
        if (special != null) {
            specialTick++;
            switch (special) {
                case SCREECH -> tickScreech(boss, target);
                case SIPHON -> tickSiphon(boss, target);
                case SEEDS -> tickSeeds(boss, target);
            }
            return;
        }
        if (!enraged && target != null
                && BossMechanics.whispererSpecialDue(specialsDone, trueHp(boss), boss.trueHitpoints(), boss.awakened())) {
            startSpecial(boss, target);
        }
    }

    private void enrage(DataBossEntity boss, ServerPlayer target) {
        enragePending = false;
        endSpecial(boss, null, false);
        enraged = true;
        volley.clear();
        boss.setHealth(Math.max(1f, mcHp(boss, boss.awakened() ? BossMechanics.WHISPERER_ENRAGE_HEAL_AWAKENED
                : BossMechanics.WHISPERER_ENRAGE_HEAL)));
        BossFx.say(boss, "The Whisperer drags you into the Shadow Realm and attacks in a frenzy!");
        for (ServerPlayer p : fighters(boss, target)) {
            enterRealm(boss, p);
        }
        cooldown = 2;
    }

    // ---------------- specials ----------------

    private void startSpecial(DataBossEntity boss, ServerPlayer target) {
        String which = BossMechanics.whispererSpecial(rotation, specialsDone);
        specialsDone++;
        specialTick = 0;
        volley.clear();
        Vec3 c = homeVec();
        ServerLevel w = BossFx.world(boss);
        switch (which) {
            case "screech" -> {
                special = Special.SCREECH;
                Vec3 south = BossFx.ground(w, c.x, c.z + 9, c.y);
                boss.getNavigation().stop();
                boss.teleportTo(south.x, south.y, south.z);
                raiseColumns(boss);
                screechesLeft = boss.awakened() ? BossMechanics.WHISPERER_SCREECHES_AWAKENED : BossMechanics.WHISPERER_SCREECHES;
                nextScreech = 5;
                BossFx.say(boss, "The Whisperer gathers energy for a screech! Hide behind the floating columns!");
            }
            case "siphon" -> {
                special = Special.SIPHON;
                boss.getNavigation().stop();
                boss.teleportTo(c.x, c.y, c.z);
                summonSouls(boss, target);
                BossFx.say(boss, "The Whisperer siphons the lost souls! Kill a whole set before her chant completes!");
            }
            default -> {
                special = Special.SEEDS;
                boss.getNavigation().stop();
                boss.teleportTo(c.x, c.y, c.z);
                scatterSeeds(boss);
                BossFx.say(boss, "The Whisperer scatters corrupted seeds! Step on every light seed - avoid the dark ones!");
            }
        }
        for (ServerPlayer p : fighters(boss, target)) {
            if (Inv.has(p, WhispererRules.FRAGMENT, 1)) {
                Msg.action(p, "Your blackstone fragment begins to glow.");
            }
        }
    }

    /** Ends the special: clears what it left, and (unless enraging) her binding orb holds the target while she comes. */
    private void endSpecial(DataBossEntity boss, ServerPlayer target, boolean bind) {
        if (special == null) {
            return;
        }
        ServerLevel w = BossFx.world(boss);
        for (Column col : columns) {
            col.blocks.forEach(p -> clearColumnBlock(w, p));
        }
        columns.clear();
        souls.forEach(Mob::discard);
        souls.clear();
        soulSet.clear();
        setsDone.clear();
        seeds.clear();
        steppedDark.clear();
        if (special == Special.SCREECH) {
            Vec3 c = homeVec();
            boss.teleportTo(c.x, c.y, c.z);
        }
        special = null;
        if (!enraged) {
            leaveRealmAll(boss); // the fragment stops glowing
        }
        if (bind && target != null) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BossMechanics.WHISPERER_BIND_TICKS * 12, 10, false, true));
            Msg.red(target, "The Whisperer's orb binds you in place!");
            chase = 6;
        }
        cooldown = 2;
    }

    /** Sends a particle only to the players in (or out of) the Shadow Realm. */
    private void realmParticle(DataBossEntity boss, ServerPlayer target, boolean inside, ParticleOptions particle, Vec3 at,
                               int count, double spread) {
        ServerLevel w = BossFx.world(boss);
        for (ServerPlayer p : fighters(boss, target)) {
            if ((enraged || realm.contains(p.getUUID())) == inside) {
                w.sendParticles(p, particle, false, at.x, at.y, at.z, count, spread, spread * 0.2, spread, 0);
            }
        }
    }

    // Screech

    private void raiseColumns(DataBossEntity boss) {
        ServerLevel w = BossFx.world(boss);
        Vec3 c = homeVec();
        // between her (at the south end) and the rest of the arena; 20/40/60 health as in OSRS (awakened up to 100, approx)
        double[][] spots = {{-6, -1}, {-3, -4}, {3, -4}, {6, -1}, {0, -2}, {0, -7}};
        int[] health = boss.awakened() ? new int[]{20, 40, 60, 80, 100, 100} : new int[]{20, 20, 40, 40, 60, 60};
        for (int i = 0; i < spots.length; i++) {
            Vec3 at = BossFx.ground(w, c.x + spots[i][0], c.z + spots[i][1], c.y);
            Column col = new Column(at, health[i]);
            BlockPos base = BlockPos.containing(at);
            for (int y = 0; y < 4; y++) {
                BlockPos p = base.above(y);
                if (w.getBlockState(p).isAir()) {
                    w.setBlock(p, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), Block.UPDATE_ALL);
                    col.blocks.add(p.immutable());
                }
            }
            columns.add(col);
        }
    }

    private static void clearColumnBlock(ServerLevel w, BlockPos p) {
        if (w.getBlockState(p).is(Blocks.POLISHED_BLACKSTONE_BRICKS)) {
            w.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    /** A column's health as seen from the Shadow Realm: one mark per 20 above it, red to green. */
    private void showColumns(DataBossEntity boss, ServerPlayer target) {
        for (Column col : columns) {
            float g = Math.min(1f, col.health / 60f);
            DustParticleOptions dust = new DustParticleOptions(new Vector3f(1f - g, g, 0.1f), 1.3f);
            for (int i = 0; i < Math.max(1, col.health / 20); i++) {
                realmParticle(boss, target, true, dust, col.pos.add(0, 4.4 + i * 0.4, 0), 1, 0.05);
            }
        }
    }

    private void tickScreech(DataBossEntity boss, ServerPlayer target) {
        showColumns(boss, target);
        if (--nextScreech > 0) {
            BossFx.burst(boss, ParticleTypes.SONIC_BOOM, 1, 0.5);
            return;
        }
        nextScreech = 4;
        screechesLeft--;
        ServerLevel w = BossFx.world(boss);
        BossFx.sound(boss, net.minecraft.sounds.SoundEvents.WARDEN_SONIC_BOOM, 1.0f);
        int dmg = scaled(boss, Dt2Bosses.WHISPER_SCREECH);
        Set<Column> hidingBehind = new HashSet<>();
        for (ServerPlayer p : fighters(boss, target)) {
            if (boss.hasLineOfSight(p)) {
                CombatHooks.nameAttack(Dt2Bosses.WHISPER_SCREECH.name(), null);
                CombatHandler.applyDamage(boss, p, dmg);
                sanityOf(p).drain(dmg);
                Msg.red(p, "The screech tears through you!");
            } else {
                Column nearest = null;
                for (Column col : columns) {
                    if (nearest == null || col.pos.distanceToSqr(p.position()) < nearest.pos.distanceToSqr(p.position())) {
                        nearest = col;
                    }
                }
                if (nearest != null) {
                    hidingBehind.add(nearest);
                }
            }
        }
        // every column takes 20 from each screech; the one someone hid behind breaks
        columns.removeIf(col -> {
            col.health -= 20;
            if (col.health > 0 && !hidingBehind.contains(col)) {
                return false;
            }
            col.blocks.forEach(p -> clearColumnBlock(w, p));
            w.sendParticles(ParticleTypes.LARGE_SMOKE, col.pos.x, col.pos.y + 1.5, col.pos.z, 15, 0.3, 1.0, 0.3, 0.02);
            return true;
        });
        if (screechesLeft <= 0) {
            endSpecial(boss, target, true);
        }
    }

    // Soul Siphon

    private void summonSouls(DataBossEntity boss, ServerPlayer target) {
        Vec3 c = homeVec();
        // Awakened: one more soul in each set (16 in all), the four standing alone at the cardinal points while the sets' rows
        // keep the diagonals (wiki "The Whisperer/Strategies", "Lost Soul"). approx: which set's lone soul stands at which point
        int extra = boss.awakened() ? 1 : 0;
        for (int set = 0; set < BossMechanics.WHISPERER_SOUL_SETS.length; set++) {
            double a = Math.PI / 4 + set * Math.PI / 2;
            int row = BossMechanics.WHISPERER_SOUL_SET_SIZES[set];
            for (int i = 0; i < row + extra; i++) {
                double x;
                double z;
                if (i < row) {
                    double along = (i - (row - 1) / 2.0) * 1.4;
                    x = c.x + Math.cos(a) * 6 - Math.sin(a) * along;
                    z = c.z + Math.sin(a) * 6 + Math.cos(a) * along;
                } else {
                    double cardinal = set * Math.PI / 2;
                    x = c.x + Math.cos(cardinal) * 6;
                    z = c.z + Math.sin(cardinal) * 6;
                }
                Mob soul = BossFx.spawnMinion(boss, BossMechanics.WHISPERER_LOST_SOUL, BossFx.ground(BossFx.world(boss), x, z, c.y), null);
                if (soul == null) {
                    continue;
                }
                soul.setCustomName(Component.literal("Lost Soul ("
                        + Character.toUpperCase(BossMechanics.WHISPERER_SOUL_SETS[set].charAt(0))
                        + BossMechanics.WHISPERER_SOUL_SETS[set].substring(1) + "!)"));
                MinionRules.set(soul, new MinionRules.Rule() {
                    @Override
                    public double accuracy(ServerPlayer p, net.minecraft.world.entity.LivingEntity mob, AttackType type) {
                        return MinionRules.ALWAYS_HITS;
                    }

                    @Override
                    public int damage(ServerPlayer p, net.minecraft.world.entity.LivingEntity mob, AttackType type, int d) {
                        // (the Desert Treasure II quest version lets a player who has no fragment yet hit them anyway)
                        boolean questWithout = scale < 1 && !Inv.has(p, WhispererRules.FRAGMENT, 1);
                        if (!WhispererRules.canHitSoul(inRealm(p)) && !questWithout) {
                            Msg.action(p, "Your attack passes through the lost soul: it is in the Shadow Realm.");
                            return 0;
                        }
                        return MinionRules.lethal(mob);
                    }
                });
                souls.add(soul);
                soulSet.put(soul, set);
            }
        }
    }

    private void tickSiphon(DataBossEntity boss, ServerPlayer target) {
        for (Mob soul : souls) {
            if (soul.isAlive()) {
                realmParticle(boss, target, true, ParticleTypes.SOUL, soul.position().add(0, 1, 0), 2, 0.3);
            }
        }
        boolean allDead = true;
        for (int set = 0; set < BossMechanics.WHISPERER_SOUL_SETS.length; set++) {
            boolean dead = true;
            for (Map.Entry<Mob, Integer> e : soulSet.entrySet()) {
                if (e.getValue() == set && e.getKey().isAlive()) {
                    dead = false;
                    allDead = false;
                }
            }
            if (dead && setsDone.add(set)) {
                soulEffect(boss, target, set);
            }
        }
        int due = boss.awakened() ? BossMechanics.WHISPERER_SIPHON_TICKS_AWAKENED : BossMechanics.WHISPERER_SIPHON_TICKS;
        if (!allDead && specialTick < due) {
            return;
        }
        if (allDead && setsDone.contains(3)) {
            // every soul gone: Mors hits harder
            int bonus = BossMechanics.WHISPERER_MORS_DAMAGE_ALL - BossMechanics.WHISPERER_MORS_DAMAGE;
            boss.setHealth(Math.max(1f, boss.getHealth() - mcHp(boss, bonus)));
        }
        boolean failed = boss.awakened() ? !allDead : setsDone.isEmpty();
        if (failed && target != null) {
            failures++;
            int left = BossMechanics.WHISPERER_SOUL_SETS.length - setsDone.size();
            int dmg = scaled(boss, Dt2Bosses.WHISPER_SIPHON);
            CombatHooks.nameAttack(Dt2Bosses.WHISPER_SIPHON.name(), null);
            CombatHandler.applyDamage(boss, target, dmg);
            sanityOf(target).drain(dmg);
            boss.heal(mcHp(boss, BossMechanics.WHISPERER_SIPHON_HEAL * Math.max(1, boss.awakened() ? left : 1)));
            Msg.red(target, "The Whisperer's chant completes, siphoning your soul!");
        }
        endSpecial(boss, target, true);
    }

    /** A full set of lost souls killed: its chant turns on the Whisperer. */
    private void soulEffect(DataBossEntity boss, ServerPlayer target, int set) {
        if (target == null) {
            return;
        }
        PlayerData data = Players.data(target);
        switch (set) {
            case 0 -> { // Vita!
                target.heal(target.getMaxHealth() * 0.2f);
                Msg.game(target, "Vita! The souls restore your health.");
            }
            case 1 -> { // Oratio!
                int level = data.skills().level(Skill.PRAYER);
                data.setPrayerPoints(Math.min(level, data.prayerPoints() + (int) Math.round(level * 0.2)));
                com.runecraft.network.ServerNetworking.sync(target);
                Msg.game(target, "Oratio! The souls restore your prayer.");
            }
            case 2 -> { // Sanitas!
                restoreSanity(target, WhispererRules.SANITAS_RESTORE);
                Msg.game(target, "Sanitas! The souls steady your mind.");
            }
            default -> { // Mors!
                boss.setHealth(Math.max(1f, boss.getHealth() - mcHp(boss, BossMechanics.WHISPERER_MORS_DAMAGE)));
                BossFx.burst(boss, ParticleTypes.SCULK_SOUL, 20, 1.0);
                Msg.game(target, "Mors! The souls turn on the Whisperer.");
            }
        }
    }

    // Corrupted Seeds

    private void scatterSeeds(DataBossEntity boss) {
        seeds.clear();
        steppedDark.clear();
        Vec3 c = homeVec();
        ServerLevel w = BossFx.world(boss);
        int layout = boss.getRandom().nextInt(WhispererRules.seedLayouts());
        for (WhispererRules.Seed s : WhispererRules.seeds(layout, boss.getRandom().nextInt(2))) {
            seeds.add(new Seed(BossFx.ground(w, c.x + s.dx() * SEED_SPACING, c.z + s.dz() * SEED_SPACING, c.y), s.light()));
        }
    }

    private void tickSeeds(DataBossEntity boss, ServerPlayer target) {
        ServerLevel w = BossFx.world(boss);
        for (Seed s : seeds) {
            Vec3 at = s.pos().add(0, 0.15, 0);
            // light and dark seeds look alike from outside the realm
            realmParticle(boss, target, true, s.light() ? LIGHT_SEED : DARK_SEED, at, 2, 0.2);
            realmParticle(boss, target, false, HIDDEN_SEED, at, 2, 0.2);
        }
        int[] dark = boss.awakened() ? BossMechanics.WHISPERER_DARK_SEED_AWAKENED : BossMechanics.WHISPERER_DARK_SEED;
        for (ServerPlayer p : fighters(boss, target)) {
            seeds.removeIf(s -> {
                if (p.position().distanceToSqr(s.pos()) > 0.8 * 0.8) {
                    return false;
                }
                w.sendParticles(ParticleTypes.POOF, s.pos().x, s.pos().y + 0.3, s.pos().z, 6, 0.2, 0.2, 0.2, 0.02);
                if (!s.light()) {
                    steppedDark.add(p.getUUID());
                    CombatHandler.applyDamage(boss, p, (int) Math.round((dark[0] + boss.getRandom().nextInt(dark[1] - dark[0] + 1)) * scale));
                    Msg.red(p, "A corrupted seed bursts under you!");
                }
                return true;
            });
        }
        boolean lightLeft = seeds.stream().anyMatch(Seed::light);
        int due = boss.awakened() ? BossMechanics.WHISPERER_SEED_TICKS_AWAKENED : BossMechanics.WHISPERER_SEED_TICKS;
        if (!lightLeft) {
            BossFx.say(boss, "The corrupted seeds wither.");
            for (ServerPlayer p : fighters(boss, target)) {
                if (!steppedDark.contains(p.getUUID())) {
                    restoreSanity(p, WhispererRules.SEEDS_CLEAN_RESTORE);
                }
            }
            endSpecial(boss, target, true);
        } else if (specialTick >= due) {
            failures++;
            if (target != null) {
                int dmg = (int) Math.round(BossMechanics.WHISPERER_SEED_FAIL * scale);
                CombatHandler.applyDamage(boss, target, dmg);
                sanityOf(target).drain(dmg);
                Msg.red(target, "The corrupted seeds erupt around you!");
            }
            endSpecial(boss, target, true);
        }
    }

    // ---------------- clean-up ----------------

    @Override
    public void writeNbt(DataBossEntity boss, CompoundTag nbt) {
        List<Long> all = new ArrayList<>();
        for (Column col : columns) {
            col.blocks.forEach(p -> all.add(p.asLong()));
        }
        if (savedColumns != null) {
            for (long p : savedColumns) {
                all.add(p);
            }
        }
        if (!all.isEmpty()) {
            nbt.putLongArray(NBT_COLUMNS, all.stream().mapToLong(Long::longValue).toArray());
        }
    }

    @Override
    public void readNbt(DataBossEntity boss, CompoundTag nbt) {
        if (nbt.contains(NBT_COLUMNS)) {
            savedColumns = nbt.getLongArray(NBT_COLUMNS);
        }
    }

    @Override
    public void onDeath(DataBossEntity boss, ServerPlayer killer) {
        onRemoved(boss);
        if (killer != null) {
            killer.removeEffect(MobEffects.DARKNESS);
        }
    }

    @Override
    public void onRemoved(DataBossEntity boss) {
        endSpecial(boss, null, false);
        realm.clear();
        sanity.clear(); // her death restores everyone's sanity
        for (ServerPlayer p : BossFx.players(boss, 32)) {
            p.removeEffect(MobEffects.DARKNESS);
        }
    }

    // ---------------- the blackstone fragment, Ketla's workbench, the entry requirement ----------------

    /** The Whisperer (normal or quest version) a player near her is fighting, or null. */
    static DataBossEntity near(ServerPlayer p) {
        DataBossEntity best = null;
        for (DataBossEntity d : p.serverLevel().getEntitiesOfClass(DataBossEntity.class, new AABB(p.blockPosition()).inflate(32),
                e -> e.isAlive() && e.script() instanceof WhispererScript)) {
            if (best == null || d.distanceToSqr(p) < best.distanceToSqr(p)) {
                best = d;
            }
        }
        return best;
    }

    /** Activates a blackstone fragment (right-click): the Shadow Realm of the Whisperer the player fights. */
    static boolean activateFragment(ServerPlayer p) {
        DataBossEntity boss = near(p);
        if (boss == null) {
            Msg.game(p, "The fragment is dormant: there is no Shadow Realm to step into here.");
            return false;
        }
        return ((WhispererScript) boss.script()).toggleRealm(boss, p);
    }

    static void register() {
        com.runecraft.platform.UseItemCallback.EVENT.register((player, world, hand) -> {
            net.minecraft.world.item.ItemStack stack = player.getItemInHand(hand);
            if (world.isClientSide() || stack.isEmpty() || !(player instanceof ServerPlayer p)
                    || !WhispererRules.FRAGMENT.equals(Inv.idOf(stack))) {
                return net.minecraft.world.InteractionResultHolder.pass(stack);
            }
            activateFragment(p);
            return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, false);
        });
        com.runecraft.content.BlockHooks.register(WhispererRules.WORKBENCH, (player, pos, state) -> {
            if (!dt2Done(player)) {
                Msg.game(player, "Ketla's tools lie on the workbench. (Complete Desert Treasure II.)");
            } else if (Inv.has(player, WhispererRules.FRAGMENT, 1)) {
                Msg.game(player, "You already have a blackstone fragment.");
            } else {
                Inv.give(player, WhispererRules.FRAGMENT, 1);
                Msg.game(player, "You take a blackstone fragment from Ketla's workbench.");
            }
        });
        // OSRS: the Whisperer is fought after Desert Treasure II, and the player refuses to without a blackstone fragment.
        com.runecraft.boss.DataBossService.requirement("the_whisperer", (player, def) -> {
            if (!dt2Done(player)) {
                Msg.game(player, "You need to complete Desert Treasure II to fight the Whisperer.");
                return false;
            }
            if (!Inv.has(player, WhispererRules.FRAGMENT, 1)) {
                Msg.game(player, "You'll need your blackstone fragment to face the Whisperer. (Ketla's workbench in the Lassar "
                        + "Undercity has another.)");
                return false;
            }
            return true;
        });
    }

    static boolean dt2Done(ServerPlayer player) {
        var quest = com.runecraft.osrs.content.ContentRegistry.quest("desert_treasure_ii");
        return quest == null || Players.data(player).questComplete(quest);
    }

    // ---------------- self-test ----------------

    /** Starts her next special now (self-test walk). */
    void selfTestSpecial(DataBossEntity boss, ServerPlayer target) {
        if (rotation < 0) {
            rotation = 0;
        }
        startSpecial(boss, target);
    }

    String selfTestSpecialName() {
        return special == null ? null : special.name().toLowerCase(java.util.Locale.ROOT);
    }

    int selfTestFailures() {
        return failures;
    }

    boolean selfTestEnraged() {
        return enraged;
    }

    List<Mob> selfTestSouls() {
        return souls;
    }

    /** The light seeds still to step on. */
    List<Vec3> selfTestLightSeeds() {
        List<Vec3> out = new ArrayList<>();
        for (Seed s : seeds) {
            if (s.light()) {
                out.add(s.pos());
            }
        }
        return out;
    }

    /**
     * Where to stand behind the weakest column still standing (the side away from her), or null. Each screech breaks the
     * column hidden behind and takes 20 from the rest, so the strong ones are saved for the last screeches (wiki: "you must
     * save the 60-health pillar for the final screech"); the awakened five need the 20, 40, 60, 80 and 100 in that order.
     */
    Vec3 selfTestHidingSpot(DataBossEntity boss) {
        Column best = null;
        for (Column col : columns) {
            if (best == null || col.health < best.health) {
                best = col;
            }
        }
        if (best == null) {
            return null;
        }
        Vec3 away = best.pos.subtract(boss.position()).multiply(1, 0, 1).normalize();
        return best.pos.add(away.scale(1.2));
    }
}
