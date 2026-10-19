package com.runecraft.content.minigames.games;

import com.runecraft.combat.CombatHandler;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.TzHaarWaves;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;

/**
 * The Inferno: 69 waves (see {@link TzHaarWaves}). Nibblers chew on the three obsidian pillars,
 * which collapse and hurt anyone beside them; Jal-Ak split into three Jal-AkRek; Jal-ImKot dig
 * towards distant players; Jal-Zek resurrect fallen monsters; JalTok-Jads get healers; TzKal-Zuk
 * (see {@link ZukScript}) hides behind the Ancestral Glyph, summons a JalTok-Jad at 480 hitpoints,
 * Jal-MejJak healers at 240 and Jal-Xil + Jal-Zek reinforcements every 210 seconds (paused below
 * 600 hitpoints until the Jad dies). Entry costs a fire cape
 * (sacrificed once); victory pays the infernal cape and 16,440 tokkul.
 */
public class InfernoSession extends TzHaarSession {
    private static final int[][] PILLARS = {{-9, -4}, {9, -4}, {0, 8}};
    private final int[] pillarHp = {255, 255, 255};
    private final List<String> fallen = new ArrayList<>();
    private int imkotFar;
    private boolean zukJad;
    private boolean zukHealers;
    private long zukSpawnedAt = -1;
    private long setTimer;
    private boolean zukDead;
    /** "Playing with Jads": when the first of wave 68's JalTok-Jads died, and whether the wave has been judged. */
    private long firstJadDeath = -1;
    private boolean jadsJudged;

    public InfernoSession(int id, int slot, ServerLevel world, String variant) {
        super(Minigame.INFERNO, id, slot, world, variant);
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
        return new BlockPos[]{rel(-17, 1, -17), rel(17, 1, -17), rel(0, 1, -18), rel(-18, 1, -4), rel(18, 1, -4), rel(-12, 1, -10),
                rel(12, 1, -10)};
    }

    @Override
    protected void build() {
        room(net.minecraft.world.level.block.Blocks.BLACKSTONE, net.minecraft.world.level.block.Blocks.POLISHED_BLACKSTONE_BRICKS, net.minecraft.world.level.block.Blocks.MAGMA_BLOCK);
        for (int i = 0; i < 3; i++) {
            buildPillar(i, true);
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
    }

    @Override
    protected Mob spawnWaveCreature(String id, BlockPos at) {
        if (id.equals(TzHaarWaves.ZUK)) {
            broadcastRed("TzKal-Zuk: Ah, the JalYt finally arrives. Let us begin.");
            Mob glyph = spawn("ancestral_glyph", rel(0, 1, -10));
            if (glyph != null) {
                glyph.addTag(com.runecraft.content.minigames.MinigameCombat.INVULNERABLE);
            }
            zukSpawnedAt = ticks;
            setTimer = 0;
            return spawn(TzHaarWaves.ZUK, rel(0, 1, -18));
        }
        if (id.equals(TzHaarWaves.JAL_NIB)) {
            int pillar = standingPillar();
            if (pillar >= 0) {
                return spawn(id, rel(PILLARS[pillar][0] + random.nextInt(5) - 2, 1, PILLARS[pillar][1] + 2));
            }
        }
        return super.spawnWaveCreature(id, at);
    }

    private int standingPillar() {
        List<Integer> up = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            if (pillarHp[i] > 0) {
                up.add(i);
            }
        }
        return up.isEmpty() ? -1 : up.get(random.nextInt(up.size()));
    }

    @Override
    protected void gameTick() {
        if (wave == TzHaarWaves.INFERNO_WAVES && zukSpawnedAt >= 0) {
            zukPhase();
            healers();
            return;
        }
        if (wave == 68 && firstJadDeath >= 0 && !jadsJudged && mobsAlive() == 0) {
            jadsJudged = true;
            if (ticks - firstJadDeath <= 50) { // 30 seconds
                caRunFlags.add(com.runecraft.osrs.content.systems.CombatAchievements.FLAG_JADS_QUICK);
            }
        }
        super.gameTick();
        nibblers();
        imkotDig();
        zekResurrect();
        if (wave >= 67) {
            checkJadHealers(TzHaarWaves.JALTOK_JAD, TzHaarWaves.infernoJadHealers(wave));
        }
    }

    /** Nibblers chew on the pillar they spawned at; collapsed pillars hurt anyone beside them. */
    private void nibblers() {
        for (LivingEntity nib : living(TzHaarWaves.JAL_NIB)) {
            int best = -1;
            double bestD = Double.MAX_VALUE;
            for (int i = 0; i < 3; i++) {
                if (pillarHp[i] <= 0) {
                    continue;
                }
                double d = nib.blockPosition().distSqr(rel(PILLARS[i][0], 1, PILLARS[i][1]));
                if (d < bestD) {
                    bestD = d;
                    best = i;
                }
            }
            if (!(nib instanceof Mob mob)) {
                continue;
            }
            if (best < 0) {
                if (mob.getTarget() == null) {
                    aggro(mob);
                }
                continue;
            }
            if (bestD > 9) {
                BlockPos p = rel(PILLARS[best][0], 1, PILLARS[best][1]);
                mob.getNavigation().moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 1.0);
            } else {
                pillarHp[best] -= random.nextInt(5);
                if (pillarHp[best] <= 0) {
                    collapse(best);
                }
            }
        }
    }

    private void collapse(int i) {
        buildPillar(i, false);
        BlockPos c = rel(PILLARS[i][0], 1, PILLARS[i][1]);
        world.sendParticles(ParticleTypes.EXPLOSION, c.getX() + 0.5, c.getY() + 1, c.getZ() + 0.5, 6, 1.5, 1, 1.5, 0);
        broadcastRed("A pillar collapses!");
        if (wave < 67) {
            caRunFlags.add(com.runecraft.osrs.content.systems.CombatAchievements.FLAG_PILLAR_EARLY);
        }
        for (ServerPlayer p : online()) {
            if (p.blockPosition().distSqr(c) < 16) {
                CombatHandler.applyDamage(null, p, 10 + random.nextInt(40));
            }
        }
    }

    private void imkotDig() {
        for (LivingEntity e : living(TzHaarWaves.JAL_IMKOT)) {
            if (e instanceof Mob mob && mob.getTarget() != null && mob.distanceToSqr(mob.getTarget()) > 100) {
                if (++imkotFar >= 10) {
                    imkotFar = 0;
                    LivingEntity t = mob.getTarget();
                    mob.teleportTo(t.getX() + random.nextInt(3) - 1, t.getY(), t.getZ() + random.nextInt(3) - 1);
                    caRunFlags.add(com.runecraft.osrs.content.systems.CombatAchievements.FLAG_IMKOT_DUG);
                    world.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, mob.getX(), mob.getY(), mob.getZ(), 10, 0.5, 0.2, 0.5, 0.01);
                    if (t instanceof ServerPlayer p) {
                        Msg.action(p, "Jal-ImKot burrows underground and resurfaces beside you!");
                    }
                }
            }
        }
    }

    private void zekResurrect() {
        if (ticks % 20 != 0 || fallen.isEmpty()) {
            return;
        }
        List<LivingEntity> zeks = living(TzHaarWaves.JAL_ZEK);
        if (zeks.isEmpty() || random.nextInt(3) != 0) {
            return;
        }
        String id = fallen.remove(random.nextInt(fallen.size()));
        LivingEntity zek = zeks.get(0);
        Mob back = spawn(id, zek.blockPosition().offset(2, 0, 0));
        if (back != null) {
            back.setHealth(back.getMaxHealth() / 2);
            aggro(back);
            broadcastRed("Jal-Zek resurrects a fallen " + back.getName().getString() + "!");
        }
    }

    private void zukPhase() {
        List<LivingEntity> zuks = living(TzHaarWaves.ZUK);
        if (zuks.isEmpty()) {
            return;
        }
        LivingEntity zuk = zuks.get(0);
        float hp = zuk.getHealth();
        boolean paused = hp <= TzHaarWaves.ZUK_SET_PAUSE_HP && (!zukJad || !living(TzHaarWaves.JALTOK_JAD).isEmpty());
        if (!paused && ++setTimer >= TzHaarWaves.ZUK_SET_TICKS) {
            setTimer = 0;
            for (String id : new String[]{TzHaarWaves.JAL_XIL, TzHaarWaves.JAL_ZEK}) {
                Mob m = spawn(id, rel(random.nextBoolean() ? -16 : 16, 1, -14));
                if (m != null) {
                    aggro(m);
                }
            }
            broadcastRed("TzKal-Zuk summons reinforcements!");
        }
        if (!zukJad && hp <= TzHaarWaves.ZUK_JAD_HP) {
            zukJad = true;
            Mob jad = spawn(TzHaarWaves.JALTOK_JAD, rel(0, 1, -2));
            if (jad != null) {
                aggro(jad);
            }
            broadcastRed("TzKal-Zuk calls forth a JalTok-Jad!");
        }
        checkJadHealers(TzHaarWaves.JALTOK_JAD, 3);
        if (!zukHealers && hp <= TzHaarWaves.ZUK_HEALER_HP) {
            zukHealers = true;
            for (int i = 0; i < 4; i++) {
                Mob h = spawn("jal_mejjak", rel(-6 + i * 4, 1, -15));
                if (h != null) {
                    aggro(h);
                }
            }
            broadcastRed("Jal-MejJak arrive to heal TzKal-Zuk!");
        }
        if (ticks % 4 == 0) {
            for (LivingEntity h : living("jal_mejjak")) {
                if (!h.getTags().contains(DISTRACTED) && zuk.getHealth() < zuk.getMaxHealth()) {
                    zuk.heal(3 + random.nextInt(6));
                }
            }
        }
    }

    @Override
    protected void onPlayerHit(ServerPlayer player, LivingEntity target, int dealt) {
        super.onPlayerHit(player, target, dealt);
        if (MinigameManager.creatureId(target).equals("jal_mejjak")) {
            target.addTag(DISTRACTED);
        }
    }

    @Override
    protected void onKill(ServerPlayer player, LivingEntity entity, String creatureId) {
        switch (creatureId) {
            case TzHaarWaves.JAL_AK -> {
                for (String small : new String[]{"jal_akrek_ket", "jal_akrek_xil", "jal_akrek_mej"}) {
                    Mob m = spawn(small, entity.blockPosition().offset(random.nextInt(3) - 1, 0, random.nextInt(3) - 1));
                    if (m != null) {
                        m.setTarget(player);
                    }
                }
                fallen.add(creatureId);
            }
            case TzHaarWaves.ZUK -> {
                zukDead = true;
                for (LivingEntity e : living(null)) {
                    e.discard();
                }
                end(true);
            }
            case TzHaarWaves.JAL_MEJRAH, TzHaarWaves.JAL_IMKOT, TzHaarWaves.JAL_XIL -> fallen.add(creatureId);
            case TzHaarWaves.JALTOK_JAD -> {
                if (wave == 68 && firstJadDeath < 0) {
                    firstJadDeath = ticks;
                }
            }
            default -> {
            }
        }
    }

    @Override
    protected void onEnd(boolean success) {
        if (!success || !zukDead) {
            return;
        }
        for (ServerPlayer p : online()) {
            reward(p, "infernal_cape", 1);
            reward(p, "tokkul", TzHaarWaves.INFERNO_TOKKUL);
            int kc = MinigameData.of(p).incrementStat(p.getUUID(), "kc:" + game.id());
            Msg.game(p, "You have defeated TzKal-Zuk! Your TzKal-Zuk kill count is: " + kc + ".");
            if (random.nextInt(TzHaarWaves.ZUK_PET_CHANCE) == 0) {
                reward(p, "jal_nib_rek", 1);
                p.getServer().getPlayerList().broadcastSystemMessage(Component.literal(p.getName().getString()
                        + " has a funny feeling like they're being followed.").withStyle(ChatFormatting.GOLD), false);
            }
            p.getServer().getPlayerList().broadcastSystemMessage(Component.literal(p.getName().getString()
                    + " has defeated TzKal-Zuk and received an infernal cape!").withStyle(ChatFormatting.GOLD), false);
        }
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        List<String> lines = new ArrayList<>(super.hud(player));
        lines.add("Pillars: " + (pillarHp[0] > 0 ? pillarHp[0] : "X") + " / " + (pillarHp[1] > 0 ? pillarHp[1] : "X") + " / "
                + (pillarHp[2] > 0 ? pillarHp[2] : "X"));
        return lines;
    }
}
