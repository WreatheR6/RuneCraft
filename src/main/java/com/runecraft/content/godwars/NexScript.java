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
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

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
    private ServerPlayerEntity sacrifice;
    private long sacrificeAt;
    private ServerPlayerEntity prisoner;
    private Vec3d prisonCentre;
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
        ServerWorld world = (ServerWorld) boss.getWorld();
        int[][] corners = {{-9, -9}, {9, -9}, {9, 9}, {-9, 9}};
        for (int i = 0; i < 4; i++) {
            BossDef def = ContentRegistry.boss(GwdBosses.NEX_MINIONS[i]);
            if (def == null) {
                continue;
            }
            DataBossEntity minion = DataBossService.spawn(world, boss.getBlockPos().add(corners[i][0], 0, corners[i][1]), def, null);
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
    protected AttackDef pick(DataBossEntity boss, ServerPlayerEntity target, double distance) {
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
    protected int landed(DataBossEntity boss, ServerPlayerEntity player, int damage, AttackDef attack) {
        if (attack == GwdBosses.NEX_SHADOW) {
            double dist = Math.sqrt(player.squaredDistanceTo(boss));
            double factor = Math.max(0.2, Math.min(1.0, 1.0 - (dist - 2) / 14.0));
            return (int) Math.round(damage * factor);
        }
        if (attack == GwdBosses.NEX_BLOOD || attack == GwdBosses.NEX_ZAROS) {
            healReal(boss, damage * 0.25);
        }
        return damage;
    }

    @Override
    public int modifyIncoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
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
        ServerWorld world = (ServerWorld) boss.getWorld();
        // A general has fallen: move on to the next phase.
        if (shieldIndex >= 0 && (minions[shieldIndex] == null || !minions[shieldIndex].isAlive())) {
            stage = Stage.values()[shieldIndex + 1];
            shieldIndex = -1;
            shout(boss, STAGE_LINES[stage.ordinal()]);
            world.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE,
                    1.0f, 0.7f);
            if (stage == Stage.ZAROS) {
                healReal(boss, 500);
                world.spawnParticles(ParticleTypes.DRAGON_BREATH, boss.getX(), boss.getY() + 2, boss.getZ(), 60, 1, 1, 1, 0.05);
            }
            nextSpecial = gameTick + 8;
        }
        if (siphonUntil > gameTick) {
            world.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, boss.getX(), boss.getY() + 1.5, boss.getZ(), 6, 0.8, 0.8, 0.8, 0.1);
        }
        resolveSacrifice(boss, world, gameTick);
        resolvePrison(boss, world, gameTick);
        tickCough(boss, world, gameTick);
        List<ServerPlayerEntity> nearby = players(boss, 24);
        if (nearby.isEmpty() || shieldIndex >= 0 || gameTick < nextSpecial) {
            return;
        }
        nextSpecial = gameTick + 15 + boss.getRandom().nextInt(10);
        alternate = !alternate;
        ServerPlayerEntity victim = nearby.get(boss.getRandom().nextInt(nearby.size()));
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
                    for (ServerPlayerEntity p : nearby) {
                        strike(boss, p.getPos(), 3, 50, 1.3, ParticleTypes.SQUID_INK, ParticleTypes.LARGE_SMOKE, (hit, dmg) -> {
                            drainPrayer(hit, 10);
                            if (dmg > 0) {
                                CombatAchievementService.flagNear(boss, CombatAchievements.FLAG_NEX_SHADOW_SMASH);
                            }
                        });
                    }
                } else {
                    shout(boss, "Embrace darkness!");
                    for (ServerPlayerEntity p : nearby) {
                        p.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 100, 0, false, false));
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
                    strike(boss, boss.getPos(), 2, 60, 3.5, ParticleTypes.SNOWFLAKE, ParticleTypes.ITEM_SNOWBALL, (hit, dmg) -> {
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
                for (ServerPlayerEntity p : nearby) {
                    drainPrayer(p, 8);
                }
            }
        }
    }

    /** "There is... NO ESCAPE!": dash in a line through the target, hitting everyone in the way. */
    private void smokeDash(DataBossEntity boss, ServerWorld world, ServerPlayerEntity victim) {
        shout(boss, "There is... NO ESCAPE!");
        Vec3d from = boss.getPos();
        Vec3d dir = victim.getPos().subtract(from).multiply(1, 0, 1);
        if (dir.lengthSquared() < 0.01) {
            return;
        }
        Vec3d to = victim.getPos().add(dir.normalize().multiply(3));
        Vec3d home = new Vec3d(homeX, homeY, homeZ);
        if (to.subtract(home).horizontalLength() > 14) {
            to = home.add(to.subtract(home).multiply(1, 0, 1).normalize().multiply(14));
        }
        Vec3d end = new Vec3d(to.x, boss.getY(), to.z);
        for (int i = 0; i <= 16; i++) {
            Vec3d p = from.add(end.subtract(from).multiply(i / 16.0));
            world.spawnParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y + 1, p.z, 4, 0.3, 0.5, 0.3, 0.01);
        }
        for (ServerPlayerEntity p : players(boss, 24)) {
            if (segmentDistance(p.getPos(), from, end) < 1.6) {
                CombatHooks.nameAttack("smoke_dash", null);
                if (CombatHandler.applyDamage(boss, p, boss.getRandom().nextInt(51)) > 0) {
                    CombatAchievementService.flagNear(boss, CombatAchievements.FLAG_NEX_SMOKE_DASH);
                }
            }
        }
        boss.requestTeleport(end.x, end.y, end.z);
    }

    /** Players coughing from the Choke virus, with the game tick it wears off. */
    private final java.util.Map<ServerPlayerEntity, Long> coughing = new java.util.HashMap<>();
    /** How long the cough lasts (approx), and how much shorter with a gas mask, facemask or slayer helmet. */
    private static final int COUGH_TICKS = 30;
    private static final int COUGH_TICKS_MASKED = 20;

    private static boolean masked(ServerPlayerEntity p) {
        String head = com.runecraft.util.Inv.idOf(p.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD));
        return head.equals("gas_mask") || head.equals("facemask") || head.startsWith("slayer_helmet");
    }

    private void infect(ServerPlayerEntity p, long gameTick) {
        if (!coughing.containsKey(p)) {
            Msg.red(p, "You've been infected by Nex's virus!");
        }
        coughing.put(p, gameTick + (masked(p) ? COUGH_TICKS_MASKED : COUGH_TICKS));
    }

    /**
     * "Let the virus flow through you!" (OSRS Wiki, Choke): the victim is infected and coughs, losing 2 prayer points
     * every 2 ticks; the virus spreads to adjacent players, infecting them or resetting their cough.
     */
    private void virus(DataBossEntity boss, ServerPlayerEntity victim, List<ServerPlayerEntity> nearby, long gameTick) {
        shout(boss, "Let the virus flow through you!");
        PoisonService.poison(victim, 6, gameTick);
        infect(victim, gameTick);
        CombatAchievementService.flagNear(boss, CombatAchievements.FLAG_NEX_COUGHING);
    }

    private void tickCough(DataBossEntity boss, ServerWorld world, long gameTick) {
        if (coughing.isEmpty()) {
            return;
        }
        coughing.entrySet().removeIf(e -> !e.getKey().isAlive() || gameTick >= e.getValue());
        for (ServerPlayerEntity p : new ArrayList<>(coughing.keySet())) {
            if (gameTick % 2 == 0) {
                drainPrayer(p, 2);
                world.spawnParticles(ParticleTypes.SNEEZE, p.getX(), p.getY() + 1.7, p.getZ(), 4, 0.2, 0.1, 0.2, 0.01);
                if (gameTick % 6 == 0) {
                    Msg.action(p, "*Cough*");
                }
            }
            for (ServerPlayerEntity o : players(boss, 24)) {
                if (o != p && o.squaredDistanceTo(p) < 2.5 * 2.5) {
                    infect(o, gameTick);
                }
            }
        }
        if (coughing.isEmpty()) {
            CombatAchievementService.unflagNear(boss, CombatAchievements.FLAG_NEX_COUGHING);
        }
    }

    private void resolveSacrifice(DataBossEntity boss, ServerWorld world, long gameTick) {
        if (sacrifice == null) {
            return;
        }
        if (!sacrifice.isAlive()) {
            sacrifice = null;
            return;
        }
        if (gameTick < sacrificeAt) {
            world.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, sacrifice.getX(), sacrifice.getY() + 2.3, sacrifice.getZ(), 3, 0.2, 0.1,
                    0.2, 0);
            return;
        }
        PlayerData data = Players.data(sacrifice);
        drainPrayer(sacrifice, data.prayerPoints() / 3);
        if (sacrifice.squaredDistanceTo(boss) < 7 * 7) {
            int dealt = CombatHandler.applyDamage(boss, sacrifice, boss.getRandom().nextInt(51));
            healReal(boss, dealt);
            Msg.red(sacrifice, "Nex drains your blood!");
        }
        sacrifice = null;
    }

    private void iceprison(DataBossEntity boss, ServerWorld world, ServerPlayerEntity victim, long gameTick) {
        shout(boss, "Die now, in a prison of ice!");
        clearPrison(world);
        prisoner = victim;
        prisonCentre = victim.getPos();
        prisonAt = gameTick + 5;
        victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 10, false, true));
        BlockPos c = victim.getBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                for (int dy = 0; dy <= 1; dy++) {
                    BlockPos p = c.add(dx, dy, dz);
                    if (world.getBlockState(p).isAir()) {
                        world.setBlockState(p, Blocks.ICE.getDefaultState());
                        prisonBlocks.add(p);
                    }
                }
            }
        }
    }

    private void resolvePrison(DataBossEntity boss, ServerWorld world, long gameTick) {
        if (prisoner == null || gameTick < prisonAt) {
            return;
        }
        if (prisoner.isAlive() && prisoner.getPos().squaredDistanceTo(prisonCentre) < 1.5 * 1.5) {
            if (CombatHandler.applyDamage(boss, prisoner, boss.getRandom().nextInt(61)) > 0) {
                CombatAchievementService.flagNear(boss, CombatAchievements.FLAG_NEX_ICE);
            }
            Msg.red(prisoner, "The ice prison shatters around you!");
        }
        world.spawnParticles(ParticleTypes.ITEM_SNOWBALL, prisonCentre.x, prisonCentre.y + 1, prisonCentre.z, 30, 1, 1, 1, 0.1);
        clearPrison(world);
        prisoner = null;
    }

    private void clearPrison(ServerWorld world) {
        for (BlockPos p : prisonBlocks) {
            if (world.getBlockState(p).isOf(Blocks.ICE)) {
                world.setBlockState(p, Blocks.AIR.getDefaultState());
            }
        }
        prisonBlocks.clear();
    }

    private static void drainPrayer(ServerPlayerEntity player, int amount) {
        if (amount <= 0) {
            return;
        }
        PlayerData data = Players.data(player);
        data.setPrayerPoints(Math.max(0, data.prayerPoints() - amount));
        ServerNetworking.sync(player);
    }

    static double segmentDistance(Vec3d p, Vec3d a, Vec3d b) {
        Vec3d ab = b.subtract(a);
        double len = ab.lengthSquared();
        double t = len < 1e-6 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dotProduct(ab) / len));
        return a.add(ab.multiply(t)).distanceTo(p);
    }

    @Override
    public void onDeath(DataBossEntity boss, ServerPlayerEntity killer) {
        shout(boss, "Taste my wrath!");
        ServerWorld world = (ServerWorld) boss.getWorld();
        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, boss.getX(), boss.getY() + 1, boss.getZ(), 2, 1, 1, 1, 0);
        for (ServerPlayerEntity p : players(boss, 4)) {
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
