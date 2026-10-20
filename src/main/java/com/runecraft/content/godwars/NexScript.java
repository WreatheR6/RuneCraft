package com.runecraft.content.godwars;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.content.systems.CombatAchievementService;
import com.runecraft.combat.PoisonService;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.godwars.GwdBosses;
import com.runecraft.osrs.content.godwars.GwdDungeon;
import com.runecraft.osrs.content.systems.CombatAchievements;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Nex, the Zarosian general, fought in her chamber of the Ancient Prison. Five phases:
 * <ol>
 *   <li><b>Smoke</b> (100-80%): opens with <i>"Let the virus flow through you!"</i> (choke, spreads to
 *       neighbours), then smoke bullets that poison, <i>"There is... NO ESCAPE!"</i> dashes and drags.</li>
 *   <li><b>Shadow</b> (80-60%, after Fumus): shadow shots (max 60) hit harder the closer you stand;
 *       <i>"Fear the shadow!"</i> shadow traps under every player and <i>"Embrace darkness!"</i>
 *       (darkness, and damage while standing next to her).</li>
 *   <li><b>Blood</b> (60-40%, after Umbra): blood barrage heals her and splashes 3x3; <i>"A siphon will solve
 *       this!"</i> summons blood reavers (one per player, up to eight) that heal her when it ends;
 *       <i>"I demand a blood sacrifice!"</i> marks a player who must get seven tiles away (up to 50, and
 *       nearby players take damage and lose a third of their prayer).</li>
 *   <li><b>Ice</b> (40-20%, after Cruor): ice barrage freezes for 15 ticks; <i>"Die now, in a prison of ice!"</i>
 *       (up to 75 unless an ice block is broken) and <i>"Contain this!"</i> (ice spikes around her, up to 60;
 *       both switch protection prayers off).</li>
 *   <li><b>Zaros</b> (20-0%, after Glacies): <i>"NOW, THE POWER OF ZAROS!"</i> she heals 500 and, every four
 *       attacks, switches between Soul Split (heals a quarter of her hits), Deflect Melee (melee hits nothing,
 *       a tenth reflects) and no curse.</li>
 * </ol>
 * At each threshold Nex becomes immune and the matching general (waiting invulnerable in a corner)
 * must be killed first. On death: <i>"Taste my wrath!"</i>, up to 50 to everyone close by after
 * five ticks. Nex has 3400 hitpoints; Minecraft caps max health at 1024, so incoming damage and healing are
 * scaled to keep the fight's real length.
 */
final class NexScript extends GwdScript {
    enum Stage { SMOKE, SHADOW, BLOOD, ICE, ZAROS }

    private static final String[] STAGE_LINES = {"Fill my soul with smoke!", "Darken my shadow!", "Flood my lungs with blood!",
            "Infuse me with the power of ice!", "NOW, THE POWER OF ZAROS!"};

    /** Blood Sacrifice: the marked player must be this far (blocks: seven tiles) from her. */
    private static final double SACRIFICE_BLOCKS = 14;
    /** Ice Prison's most damage (OSRS Wiki infobox: 75). */
    private static final int PRISON_MAX = 75;
    /** Wrath: most damage, delay after her death (3 seconds) and half-size of the 5x5 around her, in blocks. */
    private static final int WRATH_MAX = 50;
    private static final int WRATH_DELAY_TICKS = 5;
    private static final double WRATH_REACH = 5.0;

    Stage stage = Stage.SMOKE;
    private final DataBossEntity[] minions = new DataBossEntity[4];
    private int shieldIndex = -1;
    private double damageCarry;
    private long siphonUntil = -1;
    private long nextSpecial;
    private boolean alternate;
    private ServerPlayer sacrifice;
    private long sacrificeAt;
    private ServerPlayer prisoner;
    private Vec3 prisonCentre;
    private long prisonAt;
    private final List<BlockPos> prisonBlocks = new ArrayList<>();
    private double homeX;
    private double homeY;
    private double homeZ;

    /** Converts real (3400 hp) amounts into this entity's hitpoints. */
    private static double scale(DataBossEntity boss) {
        BossDef def = boss.def();
        if (def == null || def.hitpoints <= boss.getMaxHealth()) {
            return 1.0;
        }
        return boss.getMaxHealth() / def.hitpoints;
    }

    private static void healReal(DataBossEntity boss, double amount) {
        boss.heal((float) (amount * scale(boss)));
    }

    @Override
    public void onSpawn(DataBossEntity boss) {
        homeX = boss.getX();
        homeY = boss.getY();
        homeZ = boss.getZ();
        ServerLevel world = (ServerLevel) boss.level();
        int[][] corners = {{-9, -9}, {9, -9}, {-9, 9}, {9, 9}};
        boolean chamber = GwdDungeon.areaAt(boss.getBlockX(), boss.getBlockY(), boss.getBlockZ()) == GwdDungeon.Area.NEX_CHAMBER;
        for (int i = 0; i < 4; i++) {
            BossDef def = ContentRegistry.boss(GwdBosses.NEX_MINIONS[i]);
            if (def == null) {
                continue;
            }
            // In her chamber the mages stand in the four corners of the cross (Fumus north-west ... Glacies south-east).
            GwdDungeon.Spot corner = GwdDungeon.NEX_MAGES.get(i);
            BlockPos at = chamber ? new BlockPos(corner.blockX(), corner.feetY(), corner.blockZ())
                    : boss.blockPosition().offset(corners[i][0], 0, corners[i][1]);
            DataBossEntity minion = DataBossService.spawn(world, at, def, null);
            if (minion != null && minion.script() instanceof NexMinionScript s) {
                s.nex = boss;
                s.active = false;
            }
            boss.claimMinion(minion); // her reset clears them
            minions[i] = minion;
        }
        shout(boss, "AT LAST!");
        shout(boss, STAGE_LINES[0]);
        nextSpecial = ActionManager.gameTick() + 15;
    }

    @Override
    public boolean isVulnerable(DataBossEntity boss) {
        return shieldIndex < 0 || minions[shieldIndex] == null || !minions[shieldIndex].isAlive();
    }

    @Override
    public void onPhase(DataBossEntity boss, int phase) {
        if (phase < 0 || phase > 3) {
            return;
        }
        shieldIndex = phase;
        DataBossEntity minion = minions[phase];
        if (minion != null && minion.isAlive() && minion.script() instanceof NexMinionScript s) {
            s.active = true;
            if (boss.getTarget() != null) {
                minion.setTarget(boss.getTarget());
            }
        }
        siphonUntil = -1;
    }

    /** Melee attacks left in the current string (OSRS, June 2025: she always swings three times in a row). */
    private int meleeStreak;
    /** Auto-attacks made in the Zaros phase: her curse changes every four (Soul Split, Deflect Melee, none). */
    private int zarosAttacks;

    /** Nex's curse in the Zaros phase: 0 Soul Split, 1 Deflect Melee, 2 none. */
    int zarosCurse() {
        return (zarosAttacks / 4) % 3;
    }

    @Override
    protected AttackDef pick(DataBossEntity boss, ServerPlayer target, double distance) {
        if (stage == Stage.ZAROS) {
            zarosAttacks++;
        }
        boolean adjacent = distance <= meleeReach(boss);
        if (adjacent && (meleeStreak > 0 || boss.getRandom().nextInt(3) == 0)) {
            meleeStreak = meleeStreak > 0 ? meleeStreak - 1 : 2;
            return GwdBosses.NEX_MELEE;
        }
        meleeStreak = 0;
        return switch (stage) {
            case SMOKE -> GwdBosses.NEX_SMOKE;
            case SHADOW -> GwdBosses.NEX_SHADOW;
            case BLOOD -> GwdBosses.NEX_BLOOD;
            case ICE -> GwdBosses.NEX_ICE;
            case ZAROS -> GwdBosses.NEX_ZAROS;
        };
    }

    /** Her blood barrage is cast at one player and splashes the 3x3 around them; her other spells hit everyone. */
    @Override
    protected boolean isMultiTarget(AttackDef attack) {
        return attack != GwdBosses.NEX_MELEE && attack != GwdBosses.NEX_BLOOD;
    }

    @Override
    protected int landed(DataBossEntity boss, ServerPlayer player, int damage, AttackDef attack) {
        int dealt = damage;
        if (attack == GwdBosses.NEX_SHADOW) {
            // Shadow shots hit harder the closer the player stands (OSRS Wiki: up to 60, 30 through Protect from Missiles).
            double dist = Math.sqrt(player.distanceToSqr(boss));
            double factor = Math.max(0.2, Math.min(1.0, 1.0 - (dist - 2) / 14.0));
            dealt = (int) Math.round(damage * factor);
        }
        if (attack == GwdBosses.NEX_BLOOD) {
            healReal(boss, damage * 0.25);
            splashAround(boss, attack, player, 3.0);
        }
        // Zaros phase: under Soul Split every hit heals her (approx: a quarter of the damage, like the players' Soul Split).
        if (stage == Stage.ZAROS && zarosCurse() == 0) {
            healReal(boss, dealt * 0.25);
        }
        return dealt;
    }

    @Override
    public int modifyIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
        if (stage == Stage.ZAROS && zarosCurse() == 1 && type != null && type.isMelee() && damage > 0) {
            // Deflect Melee: melee hits do nothing and a tenth comes back at the attacker (approx).
            CombatHandler.applyDamage(boss, attacker, Math.max(1, damage / 10));
            Msg.action(attacker, "Nex deflects your melee attack!");
            return 0;
        }
        if (siphonUntil > ActionManager.gameTick()) {
            healReal(boss, damage);
            if (damage > 0) {
                CombatAchievementService.flagNear(boss, CombatAchievements.FLAG_NEX_SIPHON_HEAL);
            }
            Msg.action(attacker, "Nex siphons your attack to heal herself!");
            return 0;
        }
        damageCarry += damage * scale(boss);
        int out = (int) damageCarry;
        damageCarry -= out;
        return out;
    }

    @Override
    public void gameTick(DataBossEntity boss, long gameTick) {
        super.gameTick(boss, gameTick);
        ServerLevel world = (ServerLevel) boss.level();
        // A general has fallen: move on to the next phase.
        if (shieldIndex >= 0 && (minions[shieldIndex] == null || !minions[shieldIndex].isAlive())) {
            stage = Stage.values()[shieldIndex + 1];
            shieldIndex = -1;
            shout(boss, STAGE_LINES[stage.ordinal()]);
            world.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE,
                    1.0f, 0.7f);
            if (stage == Stage.ICE) {
                // the blood phase's reavers die when the ice phase begins
                for (net.minecraft.world.entity.LivingEntity r : reavers) {
                    r.discard();
                }
                reavers.clear();
            }
            if (stage == Stage.ZAROS) {
                healReal(boss, 500);
                world.sendParticles(ParticleTypes.DRAGON_BREATH, boss.getX(), boss.getY() + 2, boss.getZ(), 60, 1, 1, 1, 0.05);
            }
            nextSpecial = gameTick + 8;
        }
        if (siphonUntil > gameTick) {
            world.sendParticles(ParticleTypes.DAMAGE_INDICATOR, boss.getX(), boss.getY() + 1.5, boss.getZ(), 6, 0.8, 0.8, 0.8, 0.1);
        }
        resolveSacrifice(boss, world, gameTick);
        resolvePrison(boss, world, gameTick);
        tickCough(boss, world, gameTick);
        tickDarkness(boss, gameTick);
        List<ServerPlayer> nearby = roomPlayers(boss, 24);
        if (nearby.isEmpty() || shieldIndex >= 0 || gameTick < nextSpecial) {
            return;
        }
        nextSpecial = gameTick + 15 + boss.getRandom().nextInt(10);
        ServerPlayer victim = nearby.get(boss.getRandom().nextInt(nearby.size()));
        // OSRS: each phase's specials come in a random order; the smoke phase always opens with Choke.
        int roll = boss.getRandom().nextInt(stage == Stage.SMOKE ? 3 : 2);
        switch (stage) {
            case SMOKE -> {
                if (!choked || roll == 0) {
                    choked = true;
                    virus(boss, victim, nearby, gameTick);
                } else if (roll == 1) {
                    smokeDash(boss, world, victim);
                } else {
                    drag(boss, world, nearby);
                }
            }
            case SHADOW -> {
                if (roll == 0) {
                    shout(boss, "Fear the shadow!");
                    for (ServerPlayer p : nearby) {
                        strike(boss, p.position(), 3, 50, 1.3, ParticleTypes.SQUID_INK, ParticleTypes.LARGE_SMOKE, (hit, dmg) -> {
                            drainPrayer(hit, 10);
                            if (dmg > 0) {
                                CombatAchievementService.flagNear(boss, CombatAchievements.FLAG_NEX_SHADOW_SMASH);
                            }
                        });
                    }
                } else {
                    shout(boss, "Embrace darkness!");
                    darknessUntil = gameTick + DARKNESS_TICKS;
                    for (ServerPlayer p : nearby) {
                        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DARKNESS_TICKS * 12, 0, false, false));
                    }
                }
            }
            case BLOOD -> {
                if (roll == 0) {
                    siphon(boss, world, nearby, gameTick);
                } else {
                    shout(boss, "I demand a blood sacrifice!");
                    sacrifice = victim;
                    sacrificeAt = gameTick + 7;
                    Msg.red(victim, "Nex has marked you for a blood sacrifice! Get at least seven tiles away from her!");
                }
            }
            case ICE -> {
                if (roll == 0) {
                    iceprison(boss, world, victim, gameTick);
                } else {
                    shout(boss, "Contain this!");
                    // icicles in the 5x5 around her (she is 3x3): up to 60, and they switch off protection prayers
                    strike(boss, boss.position(), 2, 60, 5.0, ParticleTypes.SNOWFLAKE, ParticleTypes.ITEM_SNOWBALL, (hit, dmg) -> {
                        dropProtection(hit);
                        if (dmg > 0) {
                            CombatAchievementService.flagNear(boss, CombatAchievements.FLAG_NEX_ICE);
                        }
                    });
                }
            }
            case ZAROS -> {
                // No specials: the Ruinous Powers (Soul Split, Deflect Melee, then none, four attacks each) do the work.
            }
        }
    }

    /** Smoke phase opened with Choke. */
    private boolean choked;
    /** Embrace Darkness: until this tick, standing next to her hurts. */
    private long darknessUntil = -1;
    /** Game ticks each player has stood next to her in the darkness. */
    private final java.util.Map<ServerPlayer, Integer> inDarkness = new java.util.HashMap<>();
    private static final int DARKNESS_TICKS = 25; // approx (the wiki gives no length)
    /** Reavers of the current blood siphon. */
    private final List<net.minecraft.world.entity.LivingEntity> reavers = new ArrayList<>();

    /** Embrace Darkness: a player next to her for three ticks or more takes shadow damage every other tick (approx 5-10). */
    private void tickDarkness(DataBossEntity boss, long gameTick) {
        if (gameTick >= darknessUntil) {
            inDarkness.clear();
            return;
        }
        for (ServerPlayer p : roomPlayers(boss, 24)) {
            boolean close = p.distanceToSqr(boss) <= (meleeReach(boss) + 0.5) * (meleeReach(boss) + 0.5);
            int t = close ? inDarkness.merge(p, 1, Integer::sum) : 0;
            if (!close) {
                inDarkness.remove(p);
            }
            if (t >= 3 && gameTick % 2 == 0) {
                CombatHandler.applyDamage(boss, p, 5 + boss.getRandom().nextInt(6));
                Msg.red(p, "The pure shadow around Nex burns you!");
            }
        }
    }

    /**
     * "A siphon will solve this!": she kneels for 8 ticks, all damage healing her, and summons blood reavers (one per player
     * here, up to eight); the reavers of the last siphon die and heal her for their remaining hitpoints.
     */
    private void siphon(DataBossEntity boss, ServerLevel world, List<ServerPlayer> nearby, long gameTick) {
        shout(boss, "A siphon will solve this!");
        for (net.minecraft.world.entity.LivingEntity r : reavers) {
            if (r.isAlive()) {
                healReal(boss, r.getHealth());
                r.discard();
            }
        }
        reavers.clear();
        siphonUntil = gameTick + 8;
        int n = Math.max(1, Math.min(8, nearby.size()));
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            net.minecraft.world.entity.Mob m = spawnMinion(boss, "blood_reaver", Math.cos(a) * 4, Math.sin(a) * 4);
            if (m != null) {
                reavers.add(m);
            }
        }
    }

    /** Drag (smoke phase): pulls a player within 6 tiles next to her, stuns them and drops their protection prayers. */
    private void drag(DataBossEntity boss, ServerLevel world, List<ServerPlayer> nearby) {
        for (ServerPlayer p : nearby) {
            if (p.distanceToSqr(boss) > 12 * 12) {
                continue;
            }
            boolean praying = Players.data(p).isActive(com.runecraft.osrs.Prayer.PROTECT_FROM_MAGIC);
            if (boss.getRandom().nextInt(praying ? 8 : 4) != 0) {
                continue;
            }
            Vec3 dir = boss.position().subtract(p.position()).multiply(1, 0, 1);
            Vec3 to = dir.lengthSqr() < 4 ? p.position() : p.position().add(dir.normalize().scale(dir.length() - 2));
            p.teleportTo(to.x, p.getY(), to.z);
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 10, false, true));
            dropProtection(p);
            Msg.red(p, "Nex drags you towards her!");
            return;
        }
    }

    /** Switches off a player's protection prayers (Containment, Ice Prison, Drag). */
    private static void dropProtection(ServerPlayer p) {
        PlayerData data = Players.data(p);
        data.activePrayers().removeIf(pr -> pr == com.runecraft.osrs.Prayer.PROTECT_FROM_MELEE
                || pr == com.runecraft.osrs.Prayer.PROTECT_FROM_MISSILES || pr == com.runecraft.osrs.Prayer.PROTECT_FROM_MAGIC);
        ServerNetworking.sync(p);
    }

    /** "There is... NO ESCAPE!": dash in a line through the target, hitting everyone in the way. */
    private void smokeDash(DataBossEntity boss, ServerLevel world, ServerPlayer victim) {
        shout(boss, "There is... NO ESCAPE!");
        Vec3 from = boss.position();
        Vec3 dir = victim.position().subtract(from).multiply(1, 0, 1);
        if (dir.lengthSqr() < 0.01) {
            return;
        }
        Vec3 to = victim.position().add(dir.normalize().scale(3));
        Vec3 home = new Vec3(homeX, homeY, homeZ);
        if (to.subtract(home).horizontalDistance() > 14) {
            to = home.add(to.subtract(home).multiply(1, 0, 1).normalize().scale(14));
        }
        Vec3 end = new Vec3(to.x, boss.getY(), to.z);
        for (int i = 0; i <= 16; i++) {
            Vec3 p = from.add(end.subtract(from).scale(i / 16.0));
            world.sendParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y + 1, p.z, 4, 0.3, 0.5, 0.3, 0.01);
        }
        for (ServerPlayer p : roomPlayers(boss, 24)) {
            if (segmentDistance(p.position(), from, end) < 1.6) {
                CombatHooks.nameAttack("smoke_dash", null);
                if (CombatHandler.applyDamage(boss, p, boss.getRandom().nextInt(51)) > 0) {
                    CombatAchievementService.flagNear(boss, CombatAchievements.FLAG_NEX_SMOKE_DASH);
                }
            }
        }
        boss.teleportTo(end.x, end.y, end.z);
    }

    /** Players coughing from the Choke virus, with the game tick it wears off. */
    private final java.util.Map<ServerPlayer, Long> coughing = new java.util.HashMap<>();
    /** How long the cough lasts (approx), and how much shorter with a gas mask, facemask or slayer helmet. */
    private static final int COUGH_TICKS = 30;
    private static final int COUGH_TICKS_MASKED = 20;

    private static boolean masked(ServerPlayer p) {
        String head = com.runecraft.util.Inv.idOf(p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD));
        // Any slayer helmet, recoloured ones included ("black_slayer_helmet", "tzkal_slayer_helmet_i").
        return head.equals("gas_mask") || head.equals("facemask") || head.endsWith("slayer_helmet") || head.endsWith("slayer_helmet_i");
    }

    private void infect(ServerPlayer p, long gameTick) {
        if (!coughing.containsKey(p)) {
            Msg.red(p, "You've been infected by Nex's virus!");
        }
        coughing.put(p, gameTick + (masked(p) ? COUGH_TICKS_MASKED : COUGH_TICKS));
    }

    /**
     * "Let the virus flow through you!" (OSRS Wiki, Choke): the victim is infected and coughs, losing 2 prayer points
     * every 2 ticks; the virus spreads to adjacent players, infecting them or resetting their cough.
     */
    private void virus(DataBossEntity boss, ServerPlayer victim, List<ServerPlayer> nearby, long gameTick) {
        shout(boss, "Let the virus flow through you!");
        PoisonService.poison(victim, 6, gameTick);
        infect(victim, gameTick);
        CombatAchievementService.flagNear(boss, CombatAchievements.FLAG_NEX_COUGHING);
    }

    private void tickCough(DataBossEntity boss, ServerLevel world, long gameTick) {
        if (coughing.isEmpty()) {
            return;
        }
        coughing.entrySet().removeIf(e -> !e.getKey().isAlive() || gameTick >= e.getValue());
        for (ServerPlayer p : new ArrayList<>(coughing.keySet())) {
            if (gameTick % 2 == 0) {
                drainPrayer(p, 2);
                world.sendParticles(ParticleTypes.SNEEZE, p.getX(), p.getY() + 1.7, p.getZ(), 4, 0.2, 0.1, 0.2, 0.01);
                if (gameTick % 6 == 0) {
                    Msg.action(p, "*Cough*");
                }
            }
            for (ServerPlayer o : roomPlayers(boss, 24)) {
                if (o != p && o.distanceToSqr(p) < 2.5 * 2.5) {
                    infect(o, gameTick);
                }
            }
        }
        if (coughing.isEmpty()) {
            CombatAchievementService.unflagNear(boss, CombatAchievements.FLAG_NEX_COUGHING);
        }
    }

    private void resolveSacrifice(DataBossEntity boss, ServerLevel world, long gameTick) {
        if (sacrifice == null) {
            return;
        }
        if (!sacrifice.isAlive()) {
            sacrifice = null;
            return;
        }
        if (gameTick < sacrificeAt) {
            world.sendParticles(ParticleTypes.DAMAGE_INDICATOR, sacrifice.getX(), sacrifice.getY() + 2.3, sacrifice.getZ(), 3, 0.2, 0.1,
                    0.2, 0);
            return;
        }
        // OSRS Wiki: the marked player must be at least seven tiles away, else up to 50 through Protect from Magic (it heals
        // her) and a third of their Prayer; the players closest to Nex take up to 12 and lose a third of their Prayer too.
        if (sacrifice.distanceToSqr(boss) < SACRIFICE_BLOCKS * SACRIFICE_BLOCKS) {
            PlayerData data = Players.data(sacrifice);
            drainPrayer(sacrifice, data.prayerPoints() / 3);
            int dealt = CombatHandler.applyDamage(boss, sacrifice, boss.getRandom().nextInt(51));
            healReal(boss, dealt);
            Msg.red(sacrifice, "Nex drains your blood!");
            for (ServerPlayer p : roomPlayers(boss, meleeReach(boss) + 1)) {
                if (p != sacrifice) {
                    drainPrayer(p, Players.data(p).prayerPoints() / 3);
                    CombatHandler.applyDamage(boss, p, boss.getRandom().nextInt(13));
                }
            }
        }
        sacrifice = null;
    }

    private void iceprison(DataBossEntity boss, ServerLevel world, ServerPlayer victim, long gameTick) {
        shout(boss, "Die now, in a prison of ice!");
        clearPrison(world);
        prisoner = victim;
        prisonCentre = victim.position();
        prisonAt = gameTick + 7; // shatters four seconds after it forms
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 84, 10, false, true));
        dropProtection(victim);
        BlockPos c = victim.blockPosition();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                for (int dy = 0; dy <= 1; dy++) {
                    BlockPos p = c.offset(dx, dy, dz);
                    if (world.getBlockState(p).isAir()) {
                        world.setBlockAndUpdate(p, Blocks.ICE.defaultBlockState());
                        prisonBlocks.add(p);
                    }
                }
            }
        }
    }

    private void resolvePrison(DataBossEntity boss, ServerLevel world, long gameTick) {
        if (prisoner == null || gameTick < prisonAt) {
            return;
        }
        // OSRS Wiki: up to 75 to everyone still inside, unless one of the stalagmites was broken (here: an ice block mined).
        boolean broken = false;
        for (BlockPos p : prisonBlocks) {
            broken |= !world.getBlockState(p).is(Blocks.ICE);
        }
        if (broken) {
            Msg.game(prisoner, "You broke out of the ice prison!");
        } else if (prisoner.isAlive() && prisoner.position().distanceToSqr(prisonCentre) < 1.5 * 1.5) {
            if (CombatHandler.applyDamage(boss, prisoner, boss.getRandom().nextInt(PRISON_MAX + 1)) > 0) {
                CombatAchievementService.flagNear(boss, CombatAchievements.FLAG_NEX_ICE);
            }
            Msg.red(prisoner, "The ice prison shatters around you!");
        }
        world.sendParticles(ParticleTypes.ITEM_SNOWBALL, prisonCentre.x, prisonCentre.y + 1, prisonCentre.z, 30, 1, 1, 1, 0.1);
        clearPrison(world);
        prisoner = null;
    }

    private void clearPrison(ServerLevel world) {
        for (BlockPos p : prisonBlocks) {
            if (world.getBlockState(p).is(Blocks.ICE)) {
                world.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
            }
        }
        prisonBlocks.clear();
    }

    private static void drainPrayer(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return;
        }
        PlayerData data = Players.data(player);
        data.setPrayerPoints(Math.max(0, data.prayerPoints() - amount));
        ServerNetworking.sync(player);
    }

    static double segmentDistance(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double len = ab.lengthSqr();
        double t = len < 1e-6 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / len));
        return a.add(ab.scale(t)).distanceTo(p);
    }

    @Override
    public void onDeath(DataBossEntity boss, ServerPlayer killer) {
        shout(boss, "Taste my wrath!");
        ServerLevel world = (ServerLevel) boss.level();
        // Wrath (OSRS Wiki): up to 50 to everyone in the 5x5 around her, three seconds after her death (time to run away).
        Vec3 at = boss.position();
        GwdDungeon.Area in = room(boss);
        long seed = boss.getRandom().nextLong();
        com.runecraft.content.systems.SysTicks.later(WRATH_DELAY_TICKS, () -> {
            world.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + 1, at.z, 2, 1, 1, 1, 0);
            java.util.Random r = new java.util.Random(seed);
            for (ServerPlayer p : world.getPlayers(pl -> pl.isAlive() && !pl.isSpectator() && Math.abs(pl.getX() - at.x) <= WRATH_REACH
                    && Math.abs(pl.getZ() - at.z) <= WRATH_REACH && Math.abs(pl.getY() - at.y) < 4
                    && GwdDungeon.inRoom(in, pl.getBlockX(), pl.getBlockY(), pl.getBlockZ()))) {
                CombatHooks.nameAttack("wrath", null);
                CombatHandler.applyDamage(null, p, r.nextInt(WRATH_MAX + 1));
            }
        });
        for (net.minecraft.world.entity.LivingEntity r : reavers) {
            r.discard();
        }
        reavers.clear();
        for (DataBossEntity minion : minions) {
            if (minion != null && minion.isAlive()) {
                minion.discard();
            }
        }
        clearPrison(world);
        super.onDeath(boss, killer);
    }

    /** Gone without a kill (her lair reset): her generals and the ice prison go too. */
    @Override
    public void onRemoved(DataBossEntity boss) {
        for (DataBossEntity minion : minions) {
            if (minion != null && minion.isAlive()) {
                minion.discard();
            }
        }
        clearPrison((ServerLevel) boss.level());
    }
}
