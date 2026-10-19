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
 * Nex, the Zarosian general. Five phases:
 * <ol>
 *   <li><b>Smoke</b> (100-80%): smoke bullets poison everyone; <i>"There is... NO ESCAPE!"</i> dashes
 *       through players in a line; <i>"Let the virus flow through you!"</i> infects a player and
 *       everyone standing next to them.</li>
 *   <li><b>Shadow</b> (80-60%, after Fumus): shadow shots hit harder the closer you stand;
 *       <i>"Fear the shadow!"</i> shadow traps under every player (move!) and <i>"Embrace darkness!"</i>.</li>
 *   <li><b>Blood</b> (60-40%, after Umbra): blood barrage heals her; <i>"A siphon will solve this!"</i>
 *       turns damage into healing and summons blood reavers; <i>"I demand a blood sacrifice!"</i> marks a
 *       player who must get away from her.</li>
 *   <li><b>Ice</b> (40-20%, after Cruor): ice barrage freezes; <i>"Die now, in a prison of ice!"</i>
 *       and <i>"Contain this!"</i> (ice spikes around her: keep your distance).</li>
 *   <li><b>Zaros</b> (20-0%, after Glacies): <i>"NOW, THE POWER OF ZAROS!"</i> she heals 500 and her
 *       attacks drain prayer and soul-split.</li>
 * </ol>
 * At each threshold Nex becomes immune and the matching general (waiting invulnerable in a corner)
 * must be killed first. On death: <i>"Taste my wrath!"</i>. Nex has 3400 hitpoints; Minecraft caps max
 * health at 1024, so incoming damage and healing are scaled to keep the fight's real length.
 */
final class NexScript extends GwdScript {
    enum Stage { SMOKE, SHADOW, BLOOD, ICE, ZAROS }

    private static final String[] STAGE_LINES = {"Fill my soul with smoke!", "Darken my shadow!", "Flood my lungs with blood!",
            "Infuse me with the power of ice!", "NOW, THE POWER OF ZAROS!"};

    private Stage stage = Stage.SMOKE;
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
        int[][] corners = {{-9, -9}, {9, -9}, {9, 9}, {-9, 9}};
        for (int i = 0; i < 4; i++) {
            BossDef def = ContentRegistry.boss(GwdBosses.NEX_MINIONS[i]);
            if (def == null) {
                continue;
            }
            DataBossEntity minion = DataBossService.spawn(world, boss.blockPosition().offset(corners[i][0], 0, corners[i][1]), def, null);
            if (minion != null && minion.script() instanceof NexMinionScript s) {
                s.nex = boss;
                s.active = false;
            }
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

    @Override
    protected AttackDef pick(DataBossEntity boss, ServerPlayer target, double distance) {
        if (distance <= meleeReach(boss) && boss.getRandom().nextInt(3) == 0) {
            return GwdBosses.NEX_MELEE;
        }
        return switch (stage) {
            case SMOKE -> GwdBosses.NEX_SMOKE;
            case SHADOW -> GwdBosses.NEX_SHADOW;
            case BLOOD -> GwdBosses.NEX_BLOOD;
            case ICE -> GwdBosses.NEX_ICE;
            case ZAROS -> GwdBosses.NEX_ZAROS;
        };
    }

    @Override
    protected boolean isMultiTarget(AttackDef attack) {
        return attack != GwdBosses.NEX_MELEE;
    }

    @Override
    protected int landed(DataBossEntity boss, ServerPlayer player, int damage, AttackDef attack) {
        if (attack == GwdBosses.NEX_SHADOW) {
            double dist = Math.sqrt(player.distanceToSqr(boss));
            double factor = Math.max(0.2, Math.min(1.0, 1.0 - (dist - 2) / 14.0));
            return (int) Math.round(damage * factor);
        }
        if (attack == GwdBosses.NEX_BLOOD || attack == GwdBosses.NEX_ZAROS) {
            healReal(boss, damage * 0.25);
        }
        return damage;
    }

    @Override
    public int modifyIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
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
        List<ServerPlayer> nearby = players(boss, 24);
        if (nearby.isEmpty() || shieldIndex >= 0 || gameTick < nextSpecial) {
            return;
        }
        nextSpecial = gameTick + 15 + boss.getRandom().nextInt(10);
        alternate = !alternate;
        ServerPlayer victim = nearby.get(boss.getRandom().nextInt(nearby.size()));
        switch (stage) {
            case SMOKE -> {
                if (alternate) {
                    smokeDash(boss, world, victim);
                } else {
                    virus(boss, victim, nearby, gameTick);
                }
            }
            case SHADOW -> {
                if (alternate) {
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
                    for (ServerPlayer p : nearby) {
                        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, false, false));
                    }
                }
            }
            case BLOOD -> {
                if (alternate) {
                    shout(boss, "A siphon will solve this!");
                    siphonUntil = gameTick + 8;
                    boss.summonMinions(2);
                } else {
                    shout(boss, "I demand a blood sacrifice!");
                    sacrifice = victim;
                    sacrificeAt = gameTick + 7;
                    Msg.red(victim, "Nex has marked you for a blood sacrifice! Get away from her!");
                }
            }
            case ICE -> {
                if (alternate) {
                    iceprison(boss, world, victim, gameTick);
                } else {
                    shout(boss, "Contain this!");
                    strike(boss, boss.position(), 2, 60, 3.5, ParticleTypes.SNOWFLAKE, ParticleTypes.ITEM_SNOWBALL, (hit, dmg) -> {
                        drainPrayer(hit, Players.data(hit).prayerPoints() / 4);
                        if (dmg > 0) {
                            CombatAchievementService.flagNear(boss, CombatAchievements.FLAG_NEX_ICE);
                        }
                    });
                }
            }
            case ZAROS -> {
                // Nex's Zarosian power: a burst of soul-draining energy around her.
                shout(boss, "I fear no one!");
                for (ServerPlayer p : nearby) {
                    drainPrayer(p, 8);
                }
            }
        }
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
        for (ServerPlayer p : players(boss, 24)) {
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
        return head.equals("gas_mask") || head.equals("facemask") || head.startsWith("slayer_helmet");
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
            for (ServerPlayer o : players(boss, 24)) {
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
        PlayerData data = Players.data(sacrifice);
        drainPrayer(sacrifice, data.prayerPoints() / 3);
        if (sacrifice.distanceToSqr(boss) < 7 * 7) {
            int dealt = CombatHandler.applyDamage(boss, sacrifice, boss.getRandom().nextInt(51));
            healReal(boss, dealt);
            Msg.red(sacrifice, "Nex drains your blood!");
        }
        sacrifice = null;
    }

    private void iceprison(DataBossEntity boss, ServerLevel world, ServerPlayer victim, long gameTick) {
        shout(boss, "Die now, in a prison of ice!");
        clearPrison(world);
        prisoner = victim;
        prisonCentre = victim.position();
        prisonAt = gameTick + 5;
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 10, false, true));
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
        if (prisoner.isAlive() && prisoner.position().distanceToSqr(prisonCentre) < 1.5 * 1.5) {
            if (CombatHandler.applyDamage(boss, prisoner, boss.getRandom().nextInt(61)) > 0) {
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
        world.sendParticles(ParticleTypes.EXPLOSION_EMITTER, boss.getX(), boss.getY() + 1, boss.getZ(), 2, 1, 1, 1, 0);
        for (ServerPlayer p : players(boss, 4)) {
            CombatHandler.applyDamage(null, p, boss.getRandom().nextInt(41));
        }
        for (DataBossEntity minion : minions) {
            if (minion != null && minion.isAlive()) {
                minion.discard();
            }
        }
        clearPrison(world);
        super.onDeath(boss, killer);
    }
}
