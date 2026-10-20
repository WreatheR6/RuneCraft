package com.runecraft.content.minigames.games;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.content.minigames.MinigameCombat;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.TzHaarWaves;
import com.runecraft.osrs.content.systems.CombatAchievements;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The Inferno: 69 waves (see {@link TzHaarWaves}), each monster at a random one of nine spawn points.
 * <ul>
 *   <li>Jal-Nib spawn between the three rocky supports (255 hitpoints each), each picks a random standing pillar and hits it for
 *   2-4 every 4 ticks; a falling pillar takes half the current hitpoints of anyone within a tile. With every pillar down they
 *   go for the player. Clearing wave 66 brings the rest down.</li>
 *   <li>Jal-Ak read the player's protection prayer 3 ticks before each attack and use the style it doesn't block, then split
 *   into the three Jal-AkRek. Jal-MejRah drain run energy. Jal-ImKot dig to the player 50 ticks into the wave and every 40-60
 *   after (not within 15 ticks of attacking), striking 6 ticks after surfacing.</li>
 *   <li>Jal-Zek revive monsters slain this wave (1 in 10 of their attacks, each monster once, at half hitpoints, near the
 *   centre-east; no nibblers or Jal-AkRek) and attack again 8 ticks later.</li>
 *   <li>Wave 67's JalTok-Jad summons five Yt-HurKot at half health; wave 68's three attack every 9 ticks, 3 ticks apart, with
 *   three healers each.</li>
 *   <li>TzKal-Zuk ({@link ZukScript}) hides behind the Ancestral Glyph, which slides east-west in front of him and holds 600
 *   damage from the monsters that attack it. A Jal-Xil + Jal-Zek set attacks the glyph after its first full sweep and every
 *   3:30 after (the timer pauses at 600 hitpoints, gaining 1:45, until the JalTok-Jad he calls at 480); at 240 he enrages and
 *   four Jal-MejJak heal him 15-24 every 3 ticks until attacked, then lob lava.</li>
 * </ul>
 * Entry costs a fire cape (sacrificed once); victory pays the infernal cape and 16,440 tokkul (doubled by the elite Karamja
 * Diary); a death pays tokkul for the waves completed.
 */
public class InfernoSession extends TzHaarSession {
    /** The rocky supports: west, north, south (arena offsets; north is -z). // approx */
    private static final int[][] PILLARS = {{-10, -2}, {3, -9}, {5, 6}};
    /** The nine spawn points monsters are dealt to at random each wave. // approx: positions after the arena map */
    private static final int[][] SPAWNS = {{-16, -17}, {-5, -17}, {8, -17}, {17, -15}, {-17, -8}, {-4, -12}, {12, -7}, {-15, 5},
            {14, 3}};
    /** Where Jal-Zek bring monsters back (the centre-east of the arena). */
    private static final int[] REVIVE_AT = {7, -1};
    /** Zuk's platform at the north wall, and the glyph's lane in front of him. */
    private static final int ZUK_Z = -19;
    private static final int GLYPH_Z = -14;
    /** The glyph slides this far either side of the centre, this many blocks a tick. // approx */
    private static final double GLYPH_RANGE = 9;
    private static final double GLYPH_STEP = 0.5;
    /** Monsters attacking the glyph until a player hits them. */
    private static final String SHIELD = "rcmg_zuk_shield";
    /** A monster a Jal-Zek brought back (it can't be revived again). */
    private static final String REVIVED = "rcmg_revived";

    private final int[] pillarHp = {TzHaarWaves.PILLAR_HP, TzHaarWaves.PILLAR_HP, TzHaarWaves.PILLAR_HP};
    /** Monsters slain this wave that a Jal-Zek may revive (creature ids). */
    private final List<String> fallen = new ArrayList<>();
    private final Map<Integer, Integer> nibblerPillar = new HashMap<>();
    private final Map<Integer, Long> imkotDig = new HashMap<>();
    private final Map<Integer, Long> blobAttackAt = new HashMap<>();
    private final Map<Integer, String> blobStyle = new HashMap<>();
    private long waveStartedAt;
    /** Jal-MejJak lava balls in flight: landing tick and spot. */
    private final List<Object[]> blasts = new ArrayList<>();

    // TzKal-Zuk
    private boolean zukSpawned;
    private boolean zukDead;
    private int glyphId = -1;
    private double glyphX;
    private double glyphDir = -1;
    private int glyphTurns;
    private int glyphHp = TzHaarWaves.GLYPH_HP;
    private boolean firstSet;
    private int untilSet = TzHaarWaves.ZUK_SET_TICKS;
    private boolean setDelayAdded;
    private int sets;
    private boolean zukJad;
    private int zukJadId = -1;
    private boolean zukHealers;

    /** "Playing with Jads": when the first of wave 68's JalTok-Jads died, and whether the wave has been judged. */
    private long firstJadDeath = -1;
    private boolean jadsJudged;

    public InfernoSession(int id, int slot, ServerLevel world, String variant) {
        super(Minigame.INFERNO, id, slot, world, variant);
    }

    /** Jal-MejRah drain 3 run energy per hit unless the player prays Protect from Missiles (OSRS Wiki "Jal-MejRah"). */
    public static void register() {
        CombatHooks.addDamageListener((player, attacker, attack, style, damage) -> {
            if (attacker == null || !MinigameManager.creatureId(attacker).equals(TzHaarWaves.JAL_MEJRAH)
                    || !(MinigameManager.owner(attacker) instanceof InfernoSession)
                    || Players.data(player).isActive(Prayer.PROTECT_FROM_MISSILES)) {
                return;
            }
            int energy = com.runecraft.content.systems.SysState.of(player).runEnergy;
            com.runecraft.content.systems.RunEnergyService.restore(player, -Math.min(energy, TzHaarWaves.BAT_RUN_DRAIN));
        });
    }

    @Override
    protected int totalWaves() {
        return TzHaarWaves.INFERNO_WAVES;
    }

    @Override
    protected List<String> waveMonsters(int wave) {
        return TzHaarWaves.inferno(wave);
    }

    @Override
    protected int halfX() {
        return 22;
    }

    @Override
    protected int halfZ() {
        return 22;
    }

    @Override
    protected BlockPos[] spawnPoints() {
        BlockPos[] out = new BlockPos[SPAWNS.length];
        for (int i = 0; i < SPAWNS.length; i++) {
            out[i] = rel(SPAWNS[i][0], 1, SPAWNS[i][1]);
        }
        return out;
    }

    @Override
    protected void build() {
        room(Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.MAGMA_BLOCK);
        for (int i = 0; i < PILLARS.length; i++) {
            buildPillar(i, true);
        }
        // The lava lane Zuk floats in, the glyph's lane and his platform at the north wall (harmless blocks: magma would burn).
        for (int x = -12; x <= 12; x++) {
            for (int z = ZUK_Z - 2; z <= GLYPH_Z; z++) {
                set(x, 0, z, (z == GLYPH_Z ? Blocks.POLISHED_BLACKSTONE : Blocks.NETHER_WART_BLOCK).defaultBlockState());
            }
        }
        place(0, 19, "minigame_exit");
    }

    private void buildPillar(int i, boolean standing) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int y = 1; y <= 5; y++) {
                    set(PILLARS[i][0] + dx, y, PILLARS[i][1] + dz,
                            standing ? (y == 5 ? Blocks.CRYING_OBSIDIAN : Blocks.OBSIDIAN).defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
        return rel(index % 3 - 1, 1, 16);
    }

    @Override
    protected void start() {
        for (ServerPlayer p : online()) {
            MinigameData data = MinigameData.of(p);
            if (!data.flag(p.getUUID(), "inferno_cape_sacrificed") && Inv.remove(p, "fire_cape", 1) > 0) {
                data.setFlag(p.getUUID(), "inferno_cape_sacrificed");
                Msg.game(p, "You sacrifice your fire cape to the Inferno. You may now enter freely.");
            }
        }
        resumePaused();
        for (ServerPlayer p : online()) {
            TzHaarSlayer.enter(p, Minigame.INFERNO, wave < 1);
        }
    }

    // ------------------------------------------------------------------ spawning

    @Override
    protected void spawnWave(int wave, List<String> monsters) {
        fallen.clear();
        waveStartedAt = ticks;
        List<Integer> free = new ArrayList<>();
        for (int i = 0; i < SPAWNS.length; i++) {
            free.add(i);
        }
        Collections.shuffle(free, random);
        int next = 0;
        int jads = 0;
        for (String id : monsters) {
            BlockPos at;
            if (id.equals(TzHaarWaves.JAL_NIB)) {
                // Nibblers appear together between the three pillars.
                at = rel(-1 + random.nextInt(3) - 1, 1, -2 + random.nextInt(3) - 1);
            } else if (id.equals(TzHaarWaves.JALTOK_JAD) && wave == 68) {
                // Wave 68's Jads stand in a triangle around the centre.
                int[][] triangle = {{-7, -8}, {7, -8}, {0, 3}};
                at = rel(triangle[jads][0], 1, triangle[jads][1]);
            } else if (id.equals(TzHaarWaves.ZUK) || id.equals(TzHaarWaves.JALTOK_JAD)) {
                at = rel(0, 1, -6);
            } else {
                int[] s = SPAWNS[free.get(next++ % free.size())];
                at = rel(s[0], 1, s[1]);
            }
            Mob mob = spawnWaveCreature(id, at);
            if (mob == null) {
                continue;
            }
            if (id.equals(TzHaarWaves.JALTOK_JAD) && wave == 68) {
                // Every 9 ticks, each Jad 3 ticks after the previous one (OSRS Wiki "Inferno/Strategies").
                drive(mob, TzHaarWaves.TRIPLE_JAD_SPEED, 2 + jads * TzHaarWaves.TRIPLE_JAD_STAGGER);
                jads++;
            }
            if (!id.equals(TzHaarWaves.ZUK) && mob.getTarget() == null && !id.equals(TzHaarWaves.JAL_NIB)) {
                aggro(mob);
            }
        }
        if (monsters.contains(TzHaarWaves.ZUK)) {
            for (ServerPlayer p : online()) { // wave 69 starting counts as engaging Zuk (Facing Jad Head-on II)
                com.runecraft.content.systems.CombatAchievementService.engagedNow(p, TzHaarWaves.ZUK);
            }
        }
    }

    @Override
    protected Mob spawnWaveCreature(String id, BlockPos at) {
        if (id.equals(TzHaarWaves.ZUK)) {
            return spawnZuk();
        }
        Mob mob = spawn(id, at);
        if (mob == null) {
            return null;
        }
        prepare(mob, id);
        return mob;
    }

    /** Sets up the session's control of a freshly spawned (or revived) creature. */
    private void prepare(Mob mob, String id) {
        switch (id) {
            case TzHaarWaves.JAL_NIB -> drive(mob, TzHaarWaves.NIBBLER_ATTACK_TICKS, 1);
            case TzHaarWaves.JAL_AK -> drive(mob, baseSpeed(mob), 1);
            case TzHaarWaves.JAL_ZEK -> drive(mob, baseSpeed(mob), 2);
            case TzHaarWaves.JAL_IMKOT -> {
                drive(mob, baseSpeed(mob), 1);
                imkotDig.put(mob.getId(), waveStartedAt + TzHaarWaves.IMKOT_FIRST_DIG);
            }
            case TzHaarWaves.JALTOK_JAD -> drive(mob, baseSpeed(mob), 2);
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ ticking

    @Override
    protected int waveAlive() {
        int n = 0;
        for (LivingEntity e : living(null)) {
            if (e.getId() != glyphId) {
                n++;
            }
        }
        return n;
    }

    @Override
    protected void gameTick() {
        if (wave == 68 && firstJadDeath >= 0 && !jadsJudged && waveAlive() == 0) {
            jadsJudged = true;
            if (ticks - firstJadDeath <= 50) { // 30 seconds
                caRunFlags.add(CombatAchievements.FLAG_JADS_QUICK);
                // OSRS awards "Playing with Jads" as wave 68 is cleared, not when the run ends.
                for (ServerPlayer p : online()) {
                    com.runecraft.content.systems.CombatAchievementService.flag(p, CombatAchievements.FLAG_JADS_QUICK);
                }
            }
        }
        super.gameTick();
        nibblers();
        imkotDig();
        landBlasts();
        if (wave == 67 || wave == 68) {
            checkJadHealers(TzHaarWaves.JALTOK_JAD, TzHaarWaves.infernoJadHealers(wave));
        }
        if (wave == TzHaarWaves.INFERNO_WAVES && zukSpawned && !zukDead) {
            zukPhase();
        }
    }

    @Override
    protected boolean drivenAttack(OsrsMonsterEntity mob) {
        String id = MinigameManager.creatureId(mob);
        if (mob.getTags().contains(SHIELD)) {
            shieldHit(mob);
            return true;
        }
        return switch (id) {
            case TzHaarWaves.JAL_NIB -> true; // they chew pillars (nibblers()); with none left they are released
            case TzHaarWaves.JAL_AK -> blobTurn(mob);
            case TzHaarWaves.JAL_ZEK -> zekTurn(mob);
            case TzHaarWaves.JAL_IMKOT -> attack(mob, attackOf(mob, "melee"));
            case "jal_mejjak" -> mejJakTurn(mob);
            default -> attack(mob, null);
        };
    }

    // ------------------------------------------------------------------ nibblers and pillars

    private boolean pillarStanding(int i) {
        return pillarHp[i] > 0;
    }

    private int randomStandingPillar() {
        List<Integer> up = new ArrayList<>();
        for (int i = 0; i < PILLARS.length; i++) {
            if (pillarStanding(i)) {
                up.add(i);
            }
        }
        return up.isEmpty() ? -1 : up.get(random.nextInt(up.size()));
    }

    /** Each nibbler goes for a random standing pillar and hits it for 2-4 every 4 ticks; with none left it hunts the player. */
    private void nibblers() {
        for (LivingEntity e : living(TzHaarWaves.JAL_NIB)) {
            if (!(e instanceof Mob nib)) {
                continue;
            }
            Integer target = nibblerPillar.get(nib.getId());
            if (target == null || !pillarStanding(target)) {
                target = randomStandingPillar();
                if (target < 0) {
                    nibblerPillar.remove(nib.getId());
                    if (driven(nib)) {
                        release(nib);
                    }
                    if (nib.getTarget() == null) {
                        aggro(nib);
                    }
                    continue;
                }
                nibblerPillar.put(nib.getId(), target);
            }
            nib.setTarget(null);
            BlockPos p = rel(PILLARS[target][0], 1, PILLARS[target][1]);
            double d = Math.sqrt(nib.distanceToSqr(p.getX() + 0.5, nib.getY(), p.getZ() + 0.5));
            if (d > 2.7) {
                nib.getNavigation().moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 1.0);
            } else if (ticks % TzHaarWaves.NIBBLER_ATTACK_TICKS == nib.getId() % TzHaarWaves.NIBBLER_ATTACK_TICKS) {
                nib.getNavigation().stop();
                nib.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                damagePillar(target, TzHaarWaves.NIBBLER_PILLAR_MIN
                        + random.nextInt(TzHaarWaves.NIBBLER_PILLAR_MAX - TzHaarWaves.NIBBLER_PILLAR_MIN + 1));
            }
        }
    }

    private void damagePillar(int i, int damage) {
        if (!pillarStanding(i)) {
            return;
        }
        pillarHp[i] = Math.max(0, pillarHp[i] - damage);
        BlockPos c = rel(PILLARS[i][0], 2, PILLARS[i][1]);
        world.sendParticles(ParticleTypes.SMOKE, c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 3, 1.2, 0.5, 1.2, 0.01);
        if (pillarHp[i] == 0) {
            collapse(i);
        }
    }

    /** A pillar falls: half the current hitpoints of every player and monster within a tile of it. */
    private void collapse(int i) {
        pillarHp[i] = 0;
        buildPillar(i, false);
        BlockPos c = rel(PILLARS[i][0], 1, PILLARS[i][1]);
        world.sendParticles(ParticleTypes.EXPLOSION, c.getX() + 0.5, c.getY() + 1, c.getZ() + 0.5, 6, 1.5, 1, 1.5, 0);
        broadcastRed("A pillar collapses!");
        if (wave < 67) {
            caRunFlags.add(CombatAchievements.FLAG_PILLAR_EARLY);
        }
        double reach = 3.2;
        for (ServerPlayer p : online()) {
            if (horizontal(p, c) <= reach) {
                hurt(p, TzHaarWaves.pillarCollapseDamage((int) Math.ceil(p.getHealth())));
            }
        }
        for (LivingEntity e : living(null)) {
            if (horizontal(e, c) <= reach + e.getBbWidth() / 2) {
                CombatHandler.applyDamage(null, e, TzHaarWaves.pillarCollapseDamage((int) Math.ceil(e.getHealth())));
            }
        }
    }

    private static double horizontal(LivingEntity e, BlockPos c) {
        double dx = e.getX() - (c.getX() + 0.5);
        double dz = e.getZ() - (c.getZ() + 0.5);
        return Math.sqrt(dx * dx + dz * dz);
    }

    @Override
    protected void onWaveCleared(int cleared) {
        if (cleared == TzHaarWaves.PILLARS_COLLAPSE_AFTER) {
            for (int i = 0; i < PILLARS.length; i++) {
                if (pillarStanding(i)) {
                    collapse(i);
                }
            }
        }
    }

    // ------------------------------------------------------------------ blobs, magers, meleers

    /** Jal-Ak: reads the protection prayer as soon as its target is in range (or 3 ticks after attacking), attacks 3 later. */
    private boolean blobTurn(OsrsMonsterEntity blob) {
        ServerPlayer t = targetOf(blob);
        if (t == null) {
            return false;
        }
        int id = blob.getId();
        Long at = blobAttackAt.get(id);
        if (at == null) {
            if (reach(blob, t) > 9.5 || !blob.getSensing().hasLineOfSight(t)) {
                return false;
            }
            PlayerData data = Players.data(t);
            blobStyle.put(id, TzHaarWaves.blobStyle(data.isActive(Prayer.PROTECT_FROM_MAGIC), data.isActive(Prayer.PROTECT_FROM_MISSILES),
                    data.isActive(Prayer.PROTECT_FROM_MELEE), reach(blob, t) <= 2.5, random.nextDouble()));
            blobAttackAt.put(id, ticks + TzHaarWaves.BLOB_READ_TICKS);
            nextAttack.put(id, ticks + TzHaarWaves.BLOB_READ_TICKS);
            return false;
        }
        blobAttackAt.remove(id);
        String style = blobStyle.remove(id);
        AttackDef a = attackOf(blob, style == null ? "magic" : style);
        boolean hit = a != null && attack(blob, a.melee() && reach(blob, t) > 2.5 ? attackOf(blob, "ranged") : a);
        if (hit) {
            lastAttack.put(id, ticks);
        }
        nextAttack.put(id, ticks + TzHaarWaves.BLOB_READ_TICKS);
        return false;
    }

    /** Jal-Zek: 1 in 10 of its attacks revives a monster slain this wave instead, then it waits 8 ticks. */
    private boolean zekTurn(OsrsMonsterEntity zek) {
        ServerPlayer t = targetOf(zek);
        if (t == null || reach(zek, t) > 12.5 || !zek.getSensing().hasLineOfSight(t)) {
            return false;
        }
        if (!fallen.isEmpty() && random.nextInt(TzHaarWaves.ZEK_REVIVE_CHANCE) == 0) {
            revive(zek);
            nextAttack.put(zek.getId(), ticks + TzHaarWaves.ZEK_REVIVE_PAUSE);
            return false;
        }
        return attack(zek, attackOf(zek, reach(zek, t) <= 2.5 ? "melee" : "magic"));
    }

    /** Brings back one monster slain this wave at half hitpoints near the centre-east. */
    public final Mob revive(LivingEntity zek) {
        if (fallen.isEmpty()) {
            return null;
        }
        String id = fallen.remove(0);
        Mob back = spawn(id, rel(REVIVE_AT[0] + random.nextInt(3) - 1, 1, REVIVE_AT[1] + random.nextInt(3) - 1));
        if (back == null) {
            return null;
        }
        back.addTag(REVIVED);
        back.setHealth(back.getMaxHealth() / 2);
        prepare(back, id);
        delayAttack(back, 3);
        aggro(back);
        world.sendParticles(ParticleTypes.FLAME, back.getX(), back.getY() + 1, back.getZ(), 20, 0.5, 1, 0.5, 0.02);
        broadcastRed("Jal-Zek resurrects a fallen " + back.getName().getString() + "!");
        return back;
    }

    /** Jal-ImKot that couldn't land a hit for a while dig up beside the player. */
    private void imkotDig() {
        for (LivingEntity e : living(TzHaarWaves.JAL_IMKOT)) {
            if (!(e instanceof Mob mob)) {
                continue;
            }
            long due = imkotDig.computeIfAbsent(mob.getId(), k -> waveStartedAt + TzHaarWaves.IMKOT_FIRST_DIG);
            if (ticks < due) {
                continue;
            }
            imkotDig.put(mob.getId(), ticks + TzHaarWaves.IMKOT_DIG_MIN
                    + random.nextInt(TzHaarWaves.IMKOT_DIG_MAX - TzHaarWaves.IMKOT_DIG_MIN + 1));
            ServerPlayer t = targetOf(mob);
            Long last = lastAttack.get(mob.getId());
            if (t == null || (last != null && ticks - last < TzHaarWaves.IMKOT_ATTACK_LOCK) || reach(mob, t) <= 2.5) {
                continue;
            }
            dig(mob, t);
        }
    }

    /** Jal-ImKot burrows and resurfaces next to the player, striking 6 ticks later. */
    public final void dig(Mob imkot, ServerPlayer t) {
        world.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, imkot.getX(), imkot.getY(), imkot.getZ(), 10, 0.5, 0.2, 0.5, 0.01);
        double a = random.nextDouble() * Math.PI * 2;
        imkot.teleportTo(t.getX() + Math.cos(a) * 1.6, t.getY(), t.getZ() + Math.sin(a) * 1.6);
        delayAttack(imkot, TzHaarWaves.IMKOT_SURFACE_DELAY);
        nextAttack.put(imkot.getId(), ticks + TzHaarWaves.IMKOT_SURFACE_DELAY);
        caRunFlags.add(CombatAchievements.FLAG_IMKOT_DUG);
        world.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, imkot.getX(), imkot.getY(), imkot.getZ(), 10, 0.5, 0.2, 0.5, 0.01);
        Msg.action(t, "Jal-ImKot burrows underground and resurfaces beside you!");
    }

    // ------------------------------------------------------------------ TzKal-Zuk

    private Mob spawnZuk() {
        broadcastRed("TzKal-Zuk: Ah, the JalYt finally arrives. Let us begin.");
        Mob glyph = spawn("ancestral_glyph", rel(0, 1, GLYPH_Z));
        if (glyph != null) {
            glyph.addTag(MinigameCombat.INVULNERABLE);
            glyph.setNoAi(true);
            glyphId = glyph.getId();
        }
        glyphX = 0;
        glyphDir = -1;
        glyphTurns = 0;
        glyphHp = TzHaarWaves.GLYPH_HP;
        zukSpawned = true;
        Mob zuk = spawn(TzHaarWaves.ZUK, rel(0, 1, ZUK_Z));
        if (zuk != null) {
            aggro(zuk);
        }
        return zuk;
    }

    private LivingEntity zuk() {
        List<LivingEntity> z = living(TzHaarWaves.ZUK);
        return z.isEmpty() ? null : z.get(0);
    }

    /** TzKal-Zuk's real hitpoints (his Minecraft health is capped at 1,024). */
    public final int zukHitpoints() {
        LivingEntity z = zuk();
        return z == null ? 0 : TzHaarWaves.zukHitpoints(z.getHealth(), z.getMaxHealth());
    }

    private LivingEntity glyph() {
        net.minecraft.world.entity.Entity e = glyphId < 0 ? null : world.getEntity(glyphId);
        return e instanceof LivingEntity l && l.isAlive() ? l : null;
    }

    private void zukPhase() {
        LivingEntity zuk = zuk();
        if (zuk == null) {
            return;
        }
        moveGlyph();
        int hp = zukHitpoints();
        if (firstSet) {
            if (!setDelayAdded && hp <= TzHaarWaves.ZUK_SET_PAUSE_HP) {
                setDelayAdded = true;
                untilSet += TzHaarWaves.ZUK_SET_DELAY;
            }
            untilSet = TzHaarWaves.zukSetTick(untilSet, hp, zukJad);
            if (untilSet <= 0) {
                spawnSet();
            }
        }
        if (!zukJad && hp <= TzHaarWaves.ZUK_JAD_HP) {
            zukJad = true;
            Mob jad = spawn(TzHaarWaves.JALTOK_JAD, rel(0, 1, 12));
            if (jad != null) {
                zukJadId = jad.getId();
                shieldAttacker(jad);
            }
            broadcastRed("TzKal-Zuk calls forth a JalTok-Jad!");
        }
        checkJadHealers(TzHaarWaves.JALTOK_JAD, TzHaarWaves.infernoJadHealers(TzHaarWaves.INFERNO_WAVES));
        if (!zukHealers && hp <= TzHaarWaves.ZUK_HEALER_HP) {
            zukHealers = true;
            for (int i = 0; i < 4; i++) {
                Mob h = spawn("jal_mejjak", rel(-6 + i * 4, 1, GLYPH_Z - 2));
                if (h != null) {
                    h.setNoAi(true);
                    drive(h, baseSpeed(h), 1);
                }
            }
            broadcastRed("TzKal-Zuk is enraged! Jal-MejJak arrive to heal him!");
        }
        if (ticks % TzHaarWaves.MEJJAK_HEAL_TICKS == 0) {
            for (LivingEntity h : living("jal_mejjak")) {
                if (!h.getTags().contains(DISTRACTED) && zuk.getHealth() < zuk.getMaxHealth()) {
                    zuk.heal(TzHaarWaves.mejJakHeal(random) * zuk.getMaxHealth() / TzHaarWaves.ZUK_HP);
                    world.sendParticles(ParticleTypes.HEART, zuk.getX(), zuk.getY() + zuk.getBbHeight(), zuk.getZ(), 1, 0.6, 0.3, 0.6, 0);
                }
            }
        }
    }

    /** The glyph slides east-west in front of Zuk (always starting west); its first full sweep brings the first set. */
    private void moveGlyph() {
        LivingEntity glyph = glyph();
        if (glyph == null) {
            return;
        }
        glyphX += glyphDir * GLYPH_STEP;
        if (Math.abs(glyphX) >= GLYPH_RANGE) {
            glyphX = Math.signum(glyphX) * GLYPH_RANGE;
            glyphDir = -glyphDir;
            glyphTurns++;
            // West end, east end, back to the west end: one full rotation.
            if (glyphTurns == 3 && !firstSet) {
                firstSet = true;
                spawnSet();
            }
        }
        glyph.teleportTo(origin.getX() + 0.5 + glyphX, glyph.getY(), origin.getZ() + 0.5 + GLYPH_Z);
        world.sendParticles(ParticleTypes.FLAME, glyph.getX(), glyph.getY() + 2, glyph.getZ(), 2, 0.4, 0.6, 0.1, 0);
    }

    /** A Jal-Xil and a Jal-Zek appear behind the player and attack the glyph. // approx: spawn tiles */
    private void spawnSet() {
        untilSet = TzHaarWaves.ZUK_SET_TICKS;
        sets++;
        String[] ids = {TzHaarWaves.JAL_XIL, TzHaarWaves.JAL_ZEK};
        for (int i = 0; i < ids.length; i++) {
            Mob m = spawn(ids[i], rel(i == 0 ? -8 : 8, 1, 14));
            if (m != null) {
                shieldAttacker(m);
            }
        }
        broadcastRed("A Jal-Xil and a Jal-Zek emerge to attack the glyph!");
    }

    /** A monster that stands still attacking the glyph until a player hits it. */
    private void shieldAttacker(Mob m) {
        m.addTag(SHIELD);
        m.setTarget(null);
        m.setNoAi(true);
        drive(m, baseSpeed(m), 2);
    }

    /** One attack of a shield attacker against the glyph (up to its ranged or magic max hit). */
    private void shieldHit(OsrsMonsterEntity m) {
        LivingEntity glyph = glyph();
        if (glyph == null) {
            return;
        }
        AttackDef a = attackOf(m, "magic");
        if (a == null) {
            a = attackOf(m, "ranged");
        }
        int max = a == null ? 10 : a.maxHit();
        glyphHp -= random.nextInt(max + 1);
        Vec3 from = m.position().add(0, m.getBbHeight() * 0.7, 0);
        Vec3 to = glyph.position().add(0, 1.5, 0);
        for (int i = 0; i <= 8; i++) {
            Vec3 p = from.add(to.subtract(from).scale(i / 8.0));
            world.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        if (glyphHp <= 0) {
            glyphHp = 0;
            glyph.discard();
            glyphId = -1;
            broadcastRed("The Ancestral Glyph shatters! Nothing stands between you and TzKal-Zuk now.");
        }
    }

    /** Jal-MejJak: nothing until attacked (they heal Zuk), then a lava ball at the player's spot every 3 ticks. */
    private boolean mejJakTurn(OsrsMonsterEntity h) {
        if (!h.getTags().contains(DISTRACTED)) {
            return true;
        }
        List<ServerPlayer> online = online();
        if (online.isEmpty()) {
            return true;
        }
        ServerPlayer t = online.get(random.nextInt(online.size()));
        Vec3 spot = t.position();
        blasts.add(new Object[]{ticks + 2, spot});
        world.sendParticles(ParticleTypes.LAVA, spot.x, spot.y + 0.1, spot.z, 12, 1.0, 0.05, 1.0, 0);
        return true;
    }

    private void landBlasts() {
        for (java.util.Iterator<Object[]> it = blasts.iterator(); it.hasNext(); ) {
            Object[] b = it.next();
            if (ticks < (long) b[0]) {
                continue;
            }
            it.remove();
            Vec3 spot = (Vec3) b[1];
            world.sendParticles(ParticleTypes.EXPLOSION, spot.x, spot.y + 0.5, spot.z, 2, 0.8, 0.2, 0.8, 0);
            for (ServerPlayer p : online()) {
                if (Math.abs(p.getX() - spot.x) <= 1.5 && Math.abs(p.getZ() - spot.z) <= 1.5) {
                    hurt(p, TzHaarWaves.mejJakBlast(random));
                }
            }
        }
    }

    /** Zuk's JalTok-Jad brings its three healers north of it (OSRS Wiki "JalTok-Jad"). */
    @Override
    protected List<net.minecraft.core.BlockPos> healerSpawns(LivingEntity jad, int healers) {
        if (jad.getId() != zukJadId) {
            return super.healerSpawns(jad, healers);
        }
        List<BlockPos> out = new ArrayList<>();
        for (int i = 0; i < healers; i++) {
            out.add(BlockPos.containing(jad.getX() - 2 + i * 2, jad.getY(), jad.getZ() - jad.getBbWidth() / 2 - 1.5));
        }
        return out;
    }

    // ------------------------------------------------------------------ hits, kills and the end

    @Override
    protected void onPlayerHit(ServerPlayer player, LivingEntity target, AttackType type, int dealt) {
        super.onPlayerHit(player, target, type, dealt);
        if (target.getTags().contains(SHIELD) && target instanceof Mob m) {
            // A shield attacker hit by a player turns on them.
            m.removeTag(SHIELD);
            m.setNoAi(false);
            m.setTarget(player);
            if (!MinigameManager.creatureId(m).equals(TzHaarWaves.JAL_ZEK) && !MinigameManager.creatureId(m).equals(TzHaarWaves.JALTOK_JAD)) {
                release(m);
            }
        }
        if (MinigameManager.creatureId(target).equals("jal_mejjak") && !target.getTags().contains(DISTRACTED)) {
            target.addTag(DISTRACTED);
            Msg.action(player, "The Jal-MejJak stops healing TzKal-Zuk and turns on you.");
        }
    }

    @Override
    protected void onKill(ServerPlayer player, LivingEntity entity, String creatureId) {
        TzHaarSlayer.kill(player, Minigame.INFERNO, creatureId, entity);
        if (TzHaarWaves.revivable(creatureId) && !entity.getTags().contains(REVIVED) && wave < TzHaarWaves.INFERNO_WAVES) {
            fallen.add(creatureId);
        }
        switch (creatureId) {
            case TzHaarWaves.JAL_AK -> {
                for (String small : new String[]{"jal_akrek_ket", "jal_akrek_xil", "jal_akrek_mej"}) {
                    Mob m = spawn(small, entity.blockPosition().offset(random.nextInt(3) - 1, 0, random.nextInt(3) - 1));
                    if (m != null) {
                        m.setTarget(player);
                    }
                }
            }
            case TzHaarWaves.ZUK -> {
                zukDead = true;
                for (LivingEntity e : living(null)) {
                    e.discard();
                }
                end(true);
            }
            case TzHaarWaves.JALTOK_JAD -> {
                if (wave == 68 && firstJadDeath < 0) {
                    firstJadDeath = ticks;
                }
            }
            default -> {
            }
        }
    }

    /** Tokkul for the waves completed when a run ends early, doubled by the elite Karamja Diary. // approx amounts */
    private void payForfeit(ServerPlayer player) {
        int tokkul = TzHaarWaves.diaryTokkul(TzHaarWaves.infernoTokkul(completedWaves), eliteKaramja(player));
        if (tokkul > 0) {
            reward(player, "tokkul", tokkul);
            Msg.game(player, "TzHaar-Ket-Keh: You did well, JalYt. Take this TokKul. (" + tokkul + ")");
        }
    }

    @Override
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
        if (blockId.equals("minigame_exit")) {
            payForfeit(player);
            removePlayer(player, true);
            return true;
        }
        return false;
    }

    @Override
    protected void onDeath(ServerPlayer player) {
        Msg.red(player, "You have been defeated! The Inferno spits you out.");
        payForfeit(player);
        removePlayer(player, true);
    }

    @Override
    protected void onLeave(ServerPlayer player) {
        if (!zukDead && !pausing) {
            TzHaarSlayer.leave(player, Minigame.INFERNO);
        }
    }

    @Override
    protected boolean pauses() {
        return true;
    }

    /** A paused run keeps its pillars' hitpoints. */
    @Override
    protected void savePause(MinigameData data, java.util.UUID player) {
        for (int i = 0; i < PILLARS.length; i++) {
            data.setStat(player, pauseKey("pillar" + i, game), pillarHp[i] + 1);
        }
    }

    @Override
    protected void loadPause(MinigameData data, java.util.UUID player) {
        for (int i = 0; i < PILLARS.length; i++) {
            int saved = data.stat(player, pauseKey("pillar" + i, game));
            if (saved > 0) {
                pillarHp[i] = Math.min(TzHaarWaves.PILLAR_HP, saved - 1);
                if (pillarHp[i] == 0) {
                    buildPillar(i, false);
                }
            }
        }
    }

    @Override
    protected void onEnd(boolean success) {
        if (!success || !zukDead) {
            return;
        }
        for (ServerPlayer p : online()) {
            boolean task = TzHaarSlayer.onTask(p, Minigame.INFERNO);
            reward(p, "infernal_cape", 1);
            reward(p, "tokkul", TzHaarWaves.diaryTokkul(TzHaarWaves.INFERNO_TOKKUL, eliteKaramja(p)));
            MinigameData.of(p).incrementStat(p.getUUID(), "kc:" + game.id());
            if (random.nextInt(task ? TzHaarWaves.ZUK_PET_CHANCE_TASK : TzHaarWaves.ZUK_PET_CHANCE) == 0) {
                TzHaarSlayer.pet(p, "jal_nib_rek");
            }
            TzHaarSlayer.complete(p, Minigame.INFERNO);
            p.getServer().getPlayerList().broadcastSystemMessage(Component.literal(p.getName().getString()
                    + " has defeated TzKal-Zuk and received an infernal cape!").withStyle(ChatFormatting.GOLD), false);
        }
    }

    // ------------------------------------------------------------------ self-test hooks and HUD

    /** Hitpoints of the three pillars (0 = fallen). */
    public final int[] pillars() {
        return pillarHp.clone();
    }

    public final int glyphHp() {
        return glyphHp;
    }

    public final boolean glyphStanding() {
        return glyph() != null;
    }

    public final int sets() {
        return sets;
    }

    public final boolean zukJadSpawned() {
        return zukJad;
    }

    public final boolean zukHealersSpawned() {
        return zukHealers;
    }

    public final int fallenThisWave() {
        return fallen.size();
    }

    /** Self-test hook: sets TzKal-Zuk's real hitpoints. */
    public final void selfTestZukHitpoints(int hp) {
        LivingEntity z = zuk();
        if (z != null) {
            z.setHealth(Math.max(1f, hp * z.getMaxHealth() / TzHaarWaves.ZUK_HP));
        }
    }

    /** Self-test hook: damages a pillar through the nibblers' damage path. */
    public final void selfTestDamagePillar(int i, int damage) {
        damagePillar(i, damage);
    }

    /** Self-test hook: whether a pillar's blocks still stand. */
    public final boolean selfTestPillarStanding(int i) {
        return world.getBlockState(rel(PILLARS[i][0], 2, PILLARS[i][1])).is(Blocks.OBSIDIAN);
    }

    /** Self-test hook: the centre of the pillar a nibbler is going for, or null. */
    public final BlockPos selfTestNibblerPillar(LivingEntity nib) {
        Integer i = nibblerPillar.get(nib.getId());
        return i == null ? null : rel(PILLARS[i][0], 1, PILLARS[i][1]);
    }

    /** Self-test hook: the style a Jal-Ak read and is about to attack with ("melee", "ranged", "magic"), or null. */
    public final String selfTestBlobRead(LivingEntity blob) {
        return blobStyle.get(blob.getId());
    }

    /** Self-test hook: the Ancestral Glyph, or null. */
    public final LivingEntity selfTestGlyph() {
        return glyph();
    }

    /** Self-test hook: whether a monster is still attacking the glyph. */
    public final boolean selfTestShieldAttacker(LivingEntity e) {
        return e.getTags().contains(SHIELD);
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        List<String> lines = new ArrayList<>(super.hud(player));
        if (wave < TzHaarWaves.INFERNO_WAVES) {
            lines.add("Pillars: " + (pillarHp[0] > 0 ? pillarHp[0] : "X") + " / " + (pillarHp[1] > 0 ? pillarHp[1] : "X") + " / "
                    + (pillarHp[2] > 0 ? pillarHp[2] : "X"));
        } else if (zukSpawned) {
            lines.add("TzKal-Zuk: " + zukHitpoints() + " / " + TzHaarWaves.ZUK_HP);
            lines.add("Glyph: " + (glyph() == null ? "destroyed" : glyphHp + " / " + TzHaarWaves.GLYPH_HP));
        }
        if (TzHaarSlayer.onTask(player, Minigame.INFERNO)) {
            lines.add("Slayer task: TzKal-Zuk");
        }
        return lines;
    }
}
