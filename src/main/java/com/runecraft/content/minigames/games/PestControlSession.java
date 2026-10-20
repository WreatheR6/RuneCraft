package com.runecraft.content.minigames.games;

import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.MonsterStats;
import com.runecraft.combat.PoisonService;
import com.runecraft.content.minigames.MinigameCombat;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.network.Net;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.CombatFormulas;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameCurrencies;
import com.runecraft.osrs.content.minigames.MinigameRules.PestControl;
import com.runecraft.osrs.content.minigames.PestControlRules;
import com.runecraft.osrs.content.minigames.PestControlRules.Role;
import com.runecraft.osrs.content.minigames.PestControlRules.Variant;
import com.runecraft.platform.UseBlockCallback;
import com.runecraft.platform.UseItemCallback;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.util.StackData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.Vec3;

/**
 * Pest Control (OSRS Wiki, {@link PestControlRules}): defend the Void Knight in his fort while destroying the four portals.
 * The shields drop in one of six rotations, the first 15 seconds in and then every 30 seconds; each portal (200 hitpoints on
 * the novice lander, 250 on the others; weak to one style) spawns pests of its lander's levels at the wiki's spawn chances.
 * Defilers, shifters and torchers attack the knight (defilers and torchers over the walls, shifters by teleporting beside
 * him), brawlers fight players, ravagers tear down the gates and barricades, splatters detonate on them (and when they die)
 * for 5-24 typeless damage around them, and spinners heal their portal and explode in poison when it falls. Players repair
 * gates and barricades with a hammer and a log. Every destroyed portal heals the knight by 50; the game is won when all four
 * fall or the knight survives 20 minutes, lost when he dies. Winners whose activity bar isn't empty get 3/4/5 commendation
 * points (plus the Combat Achievement bonus) and ten coins per combat level.
 */
public class PestControlSession extends MinigameSession {
    /** Extra commendation points per won game (the systems module adds the Combat Achievement tier bonus). */
    public static java.util.function.ToIntFunction<ServerPlayer> extraPoints = p -> 0;
    /** Tag carrying a pest's level variant (its stats and max hit), e.g. {@code rcpc_lvl:97}. */
    public static final String LEVEL_TAG = "rcpc_lvl:";
    /** Attack speed that keeps spinners from fighting back (they never do in OSRS). */
    private static final int NO_AUTO_ATTACK = 1_000_000;

    /** Portal spots (arena x, z; +z is south): purple west, blue east, yellow south-east, red south-west. */
    private static final int[][] PORTAL_POS = {{-24, 0}, {24, 0}, {12, 22}, {-12, 22}};
    private static final String[] PORTAL_DIR = {"western", "eastern", "south-eastern", "south-western"};
    /** The landing jetty north of the fort, where the lander drops the players (and a dead player respawns). */
    private static final int DOCK_Z = -18;

    final int tier;
    private final Mob[] portals = new Mob[4];
    private final boolean[] shielded = {true, true, true, true};
    private final boolean[] destroyed = new boolean[4];
    private final int[] rotation;
    private int shieldsDropped;
    private Mob knight;
    private final Map<UUID, Integer> activity = new HashMap<>();
    final List<Pest> pests = new ArrayList<>();
    final List<Barrier> barriers = new ArrayList<>();
    private boolean won;
    private boolean timedOut;

    /** One spawned pest: its creature, level variant and the portal it came from. */
    static final class Pest {
        final Mob mob;
        final String id;
        final Variant variant;
        final int portal;
        long nextAttack;
        long nextBlink;
        Vec3 lastPos;
        boolean exploded;

        Pest(Mob mob, String id, Variant variant, int portal) {
            this.mob = mob;
            this.id = id;
            this.variant = variant;
            this.portal = portal;
            this.lastPos = mob.position();
        }

        Role role() {
            return PestControlRules.role(id);
        }
    }

    /** A gate or barricade section standing in the arena, with its hitpoints. */
    final class Barrier {
        final PestControlRules.Structure def;
        final List<BlockPos> blocks = new ArrayList<>();
        final int max;
        int hp;
        int stage;

        Barrier(PestControlRules.Structure def) {
            this.def = def;
            this.max = def.gate() ? PestControlRules.GATE_HP : PestControlRules.BARRICADE_HP;
            this.hp = max;
            for (int[] t : def.tiles()) {
                blocks.add(rel(t[0], 1, -t[1]));
            }
        }

        boolean standing() {
            return hp > 0;
        }

        double distanceSq(Vec3 at) {
            double best = Double.MAX_VALUE;
            for (BlockPos b : blocks) {
                best = Math.min(best, at.distanceToSqr(b.getX() + 0.5, b.getY(), b.getZ() + 0.5));
            }
            return best;
        }

        void damage(int amount) {
            if (amount <= 0 || hp <= 0) {
                return;
            }
            hp = Math.max(0, hp - amount);
            redraw();
            if (hp == 0) {
                broadcastRed("The " + def.name() + " has been destroyed!");
            }
        }

        void repair() {
            hp = max;
            redraw();
        }

        void redraw() {
            int now = PestControlRules.damageStage(hp, max);
            if (now == stage && hp > 0) {
                return;
            }
            stage = now;
            boolean alongX = blocks.size() > 1 && blocks.get(0).getZ() == blocks.get(1).getZ();
            for (BlockPos b : blocks) {
                put(b, structureBlock(def.gate(), stage, alongX, world.getBlockState(b)));
            }
        }
    }

    public PestControlSession(int id, int slot, ServerLevel world, String variant) {
        super(Minigame.PEST_CONTROL, id, slot, world, variant);
        this.tier = variant.contains("veteran") ? 2 : variant.contains("intermediate") ? 1 : 0;
        this.rotation = PestControlRules.SHIELD_ROTATIONS[random.nextInt(PestControlRules.SHIELD_ROTATIONS.length)];
    }

    @Override
    protected int halfX() {
        return 30;
    }

    @Override
    protected int halfZ() {
        return 30;
    }

    private void put(BlockPos pos, BlockState state) {
        com.runecraft.content.minigames.Arena.set(world, pos, state);
    }

    /** A stone wall segment running along x or z (sides set: the arena is built without neighbour updates). */
    private static BlockState wall(boolean alongX) {
        BlockState s = Blocks.STONE_BRICK_WALL.defaultBlockState().setValue(WallBlock.UP, true);
        return alongX ? s.setValue(WallBlock.EAST_WALL, WallSide.LOW).setValue(WallBlock.WEST_WALL, WallSide.LOW)
                : s.setValue(WallBlock.NORTH_WALL, WallSide.LOW).setValue(WallBlock.SOUTH_WALL, WallSide.LOW);
    }

    /** The block a gate or barricade shows at a damage stage: oak healthy, spruce damaged, dark oak broken, gone when destroyed. */
    private static BlockState structureBlock(boolean gate, int stage, boolean alongX, BlockState current) {
        if (stage >= 3) {
            return Blocks.AIR.defaultBlockState();
        }
        if (gate) {
            Block b = stage == 0 ? Blocks.OAK_FENCE_GATE : stage == 1 ? Blocks.SPRUCE_FENCE_GATE : Blocks.DARK_OAK_FENCE_GATE;
            boolean open = current.getBlock() instanceof FenceGateBlock && current.getValue(FenceGateBlock.OPEN);
            return b.defaultBlockState().setValue(FenceGateBlock.FACING, alongX ? Direction.NORTH : Direction.EAST)
                    .setValue(FenceGateBlock.OPEN, open).setValue(FenceGateBlock.IN_WALL, true);
        }
        Block b = stage == 0 ? Blocks.OAK_FENCE : stage == 1 ? Blocks.SPRUCE_FENCE : Blocks.DARK_OAK_FENCE;
        BlockState s = b.defaultBlockState();
        return alongX ? s.setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.WEST, true)
                : s.setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.SOUTH, true);
    }

    @Override
    protected void build() {
        room(Blocks.GRASS_BLOCK, Blocks.MOSSY_STONE_BRICKS, Blocks.COARSE_DIRT);
        // The fort: west and east walls with a gate each, the south wall with the third gate; open to the north (the jetty).
        for (int dy = PestControlRules.FORT_SOUTH; dy <= PestControlRules.FORT_NORTH; dy++) {
            set(PestControlRules.FORT_WEST, 1, -dy, wall(false));
            set(PestControlRules.FORT_EAST, 1, -dy, wall(false));
        }
        for (int dx = PestControlRules.FORT_WEST; dx <= PestControlRules.FORT_EAST; dx++) {
            set(dx, 1, -PestControlRules.FORT_SOUTH, wall(true));
        }
        // The knight's platform.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                set(dx, 0, dz, Blocks.POLISHED_ANDESITE.defaultBlockState());
            }
        }
        for (PestControlRules.Structure s : PestControlRules.STRUCTURES) {
            Barrier b = new Barrier(s);
            b.stage = -1;
            b.redraw();
            barriers.add(b);
        }
        // Portal pads in their colours.
        BlockState[] pads = {Blocks.PURPLE_CONCRETE.defaultBlockState(), Blocks.BLUE_CONCRETE.defaultBlockState(),
                Blocks.YELLOW_CONCRETE.defaultBlockState(), Blocks.RED_CONCRETE.defaultBlockState()};
        for (int i = 0; i < 4; i++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    set(PORTAL_POS[i][0] + dx, 0, PORTAL_POS[i][1] + dz, pads[i]);
                }
            }
        }
        // The landing jetty north of the fort, with the squire's way back to the outpost.
        for (int x = -3; x <= 3; x++) {
            for (int z = DOCK_Z - 3; z <= DOCK_Z + 1; z++) {
                set(x, 0, z, Blocks.SPRUCE_PLANKS.defaultBlockState());
            }
        }
        place(0, DOCK_Z - 3, "minigame_exit");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
        return rel(index % 5 - 2, 1, DOCK_Z);
    }

    @Override
    protected void start() {
        knight = spawn("void_knight", rel(0, 1, 0));
        if (knight != null) {
            setHitpoints(knight, PestControl.VOID_KNIGHT_HP);
        }
        for (int i = 0; i < 4; i++) {
            portals[i] = spawn(PestControl.PORTALS[i], rel(PORTAL_POS[i][0], 1, PORTAL_POS[i][1]));
            if (portals[i] != null) {
                setHitpoints(portals[i], PestControl.PORTAL_HP[tier]);
                portals[i].addTag(MinigameCombat.INVULNERABLE);
            } else {
                destroyed[i] = true;
            }
        }
        for (ServerPlayer p : online()) {
            activity.put(p.getUUID(), 0);
        }
        broadcast("You must defend the Void Knight while the portals are unsummoned. The ritual takes twenty minutes, but if you "
                + "can destroy the portals early, the Void Knight will be able to escape.");
    }

    // ------------------------------------------------------------------ the game

    @Override
    protected void gameTick() {
        if (knight == null || !knight.isAlive()) {
            broadcastRed("The Void Knight was killed, another of our Order has fallen and that Island is lost.");
            end(false);
            return;
        }
        knight.setTarget(null);
        if (ticks >= PestControl.GAME_TICKS) {
            won = true;
            timedOut = true;
            end(true);
            return;
        }
        while (shieldsDropped < 4 && ticks >= PestControlRules.shieldDropTick(shieldsDropped)) {
            dropShield(rotation[shieldsDropped++]);
        }
        for (int i = 0; i < 4; i++) {
            if (!destroyed[i] && (portals[i] == null || !portals[i].isAlive())) {
                portalDestroyed(i);
            }
        }
        if (allPortalsDestroyed()) {
            won = true;
            end(true);
            return;
        }
        for (UUID u : players) {
            activity.computeIfPresent(u, (k, v) -> PestControlRules.activityDecay(v));
        }
        tickPests();
        if (ticks % PestControlRules.SPAWN_TICKS == 0 && pests.size() < PestControlRules.pestCap(playerCount())) {
            for (int i = 0; i < 4; i++) {
                if (!destroyed[i]) {
                    spawnPest(i, PestControlRules.rollPest(random.nextDouble()), null);
                }
            }
        }
    }

    private boolean allPortalsDestroyed() {
        for (boolean d : destroyed) {
            if (!d) {
                return false;
            }
        }
        return true;
    }

    private void dropShield(int i) {
        if (destroyed[i] || !shielded[i]) {
            return;
        }
        shielded[i] = false;
        if (portals[i] != null) {
            portals[i].getTags().remove(MinigameCombat.INVULNERABLE);
        }
        broadcastRed("The " + colour(i) + ", " + PORTAL_DIR[i] + " portal shield has dropped!");
    }

    private void portalDestroyed(int i) {
        destroyed[i] = true;
        shielded[i] = false;
        broadcastRed("The " + colour(i) + ", " + PORTAL_DIR[i] + " portal has been destroyed!");
        if (knight != null && knight.isAlive()) {
            float before = knight.getHealth();
            knight.setHealth(Math.min(knight.getMaxHealth(), before + PestControlRules.KNIGHT_HEAL_PER_PORTAL));
            ServerNetworking.hitsplat(knight, Math.round(knight.getHealth() - before), Net.SPLAT_HEAL);
        }
        // Its spinners spin around and explode, poisoning the players around them.
        for (Pest p : new ArrayList<>(pests)) {
            if (p.portal == i && p.id.equals("spinner") && p.mob.isAlive()) {
                spinnerExplosion(p);
            }
        }
    }

    /** A pest of {@code id} from portal {@code i}: a level of this lander (or {@code forced}) with its stats and max hit. */
    Pest spawnPest(int i, String id, Variant forced) {
        Mob portal = portals[i];
        BlockPos base = portal != null ? portal.blockPosition() : rel(PORTAL_POS[i][0], 1, PORTAL_POS[i][1]);
        Mob mob = spawn(id, base.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2));
        if (mob == null) {
            return null;
        }
        Variant v = forced != null ? forced : PestControlRules.rollVariant(id, tier, random.nextDouble());
        applyVariant(mob, v, id.equals("spinner"));
        Pest p = new Pest(mob, id, v, i);
        p.nextAttack = ticks + PestControlRules.PEST_ATTACK_TICKS;
        p.nextBlink = ticks + PestControlRules.SHIFTER_BLINK_TICKS;
        pests.add(p);
        if (p.role() == Role.PLAYERS) {
            aggro(mob);
        }
        return p;
    }

    /** Gives a pest the stats, hitpoints and max hit of its level (osrs-dps-calc). */
    static void applyVariant(Mob mob, Variant v, boolean neverAttacks) {
        if (mob instanceof OsrsMonsterEntity o) {
            o.overrideStats(s -> new MonsterStats(s.name(), v.level(), v.hitpoints(), Math.max(1, v.attack()), Math.max(1, v.strength()),
                    Math.max(1, v.defence()), Math.max(1, v.magic()), Math.max(1, v.ranged()), s.defenceBonus(),
                    neverAttacks ? NO_AUTO_ATTACK : s.attackSpeed(), s.style(), v.maxHit(), s.monster(), s.profile(), s.boss()));
        }
        setHitpoints(mob, v.hitpoints());
        mob.getTags().removeIf(t -> t.startsWith(LEVEL_TAG));
        mob.addTag(LEVEL_TAG + v.level());
    }

    /** The level variant a pest was spawned as, or null for anything else. */
    static Variant variantOf(LivingEntity mob) {
        for (String t : mob.getTags()) {
            if (t.startsWith(LEVEL_TAG)) {
                try {
                    return PestControlRules.variant(MinigameManager.creatureId(mob), Integer.parseInt(t.substring(LEVEL_TAG.length())));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    private void tickPests() {
        for (Pest p : new ArrayList<>(pests)) {
            Mob m = p.mob;
            if (!m.isAlive() || m.isRemoved()) {
                pests.remove(p);
                if (p.id.equals("splatter") && !p.exploded) {
                    splatterExplosion(p, p.lastPos);
                }
                continue;
            }
            p.lastPos = m.position();
            switch (p.role()) {
                case HEALER -> spinner(p);
                case BOMBER -> splatter(p);
                case STRUCTURES -> ravager(p);
                case KNIGHT -> knightAttacker(p);
                default -> {
                    if (!(m.getTarget() instanceof ServerPlayer)) {
                        aggro(m);
                    }
                }
            }
        }
    }

    /** True while a player fights this pest close by: it fights back instead of going about its business. */
    private static boolean engaged(Mob m) {
        return m.getTarget() instanceof ServerPlayer t && t.isAlive() && t.distanceToSqr(m) < 64;
    }

    private void spinner(Pest p) {
        p.mob.setTarget(null);
        Mob portal = portals[p.portal];
        if (destroyed[p.portal] || portal == null || !portal.isAlive()) {
            return;
        }
        if (p.mob.distanceToSqr(portal) > 9) {
            p.mob.getNavigation().moveTo(portal.getX(), portal.getY(), portal.getZ(), 1.0);
        } else if (ticks % PestControlRules.SPINNER_HEAL_TICKS == 0 && portal.getHealth() < portal.getMaxHealth()) {
            int heal = PestControlRules.SPINNER_HEAL_MIN + random.nextInt(PestControlRules.SPINNER_HEAL_MAX - PestControlRules.SPINNER_HEAL_MIN + 1);
            float before = portal.getHealth();
            portal.setHealth(Math.min(portal.getMaxHealth(), before + heal));
            ServerNetworking.hitsplat(portal, Math.round(portal.getHealth() - before), Net.SPLAT_HEAL);
            world.sendParticles(ParticleTypes.HAPPY_VILLAGER, portal.getX(), portal.getY() + 2, portal.getZ(), 3, 0.5, 0.5, 0.5, 0);
        }
    }

    private void splatter(Pest p) {
        if (engaged(p.mob)) {
            return;
        }
        Barrier b = nearestStanding(p.mob.position());
        if (b == null) {
            if (!(p.mob.getTarget() instanceof ServerPlayer)) {
                aggro(p.mob);
            }
            return;
        }
        if (b.distanceSq(p.mob.position()) <= 2.25) {
            splatterExplosion(p, p.mob.position());
            p.mob.discard();
            pests.remove(p);
        } else {
            BlockPos at = b.blocks.get(0);
            p.mob.getNavigation().moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 1.0);
        }
    }

    private void ravager(Pest p) {
        if (engaged(p.mob)) {
            return;
        }
        p.mob.setTarget(null);
        Barrier b = nearestStanding(p.mob.position());
        if (b == null) {
            return;
        }
        if (b.distanceSq(p.mob.position()) <= 4.0) {
            p.mob.getNavigation().stop();
            if (ticks >= p.nextAttack) {
                p.nextAttack = ticks + PestControlRules.PEST_ATTACK_TICKS;
                p.mob.swing(InteractionHand.MAIN_HAND);
                b.damage(random.nextInt(p.variant.maxHit() + 1));
            }
        } else {
            BlockPos at = b.blocks.get(0);
            p.mob.getNavigation().moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 1.0);
        }
    }

    private void knightAttacker(Pest p) {
        if (engaged(p.mob) || knight == null) {
            return;
        }
        Mob m = p.mob;
        m.setTarget(null); // no player close by: the Void Knight comes first
        double reach = PestControlRules.knightReach(p.id);
        double d = m.distanceToSqr(knight);
        if (d > reach * reach) {
            if (p.id.equals("shifter") && ticks >= p.nextBlink) {
                p.nextBlink = ticks + PestControlRules.SHIFTER_BLINK_TICKS;
                blinkTowardsKnight(m);
            } else {
                m.getNavigation().moveTo(knight.getX(), knight.getY(), knight.getZ(), 1.0);
            }
            return;
        }
        m.getNavigation().stop();
        m.getLookControl().setLookAt(knight);
        if (ticks >= p.nextAttack) {
            p.nextAttack = ticks + PestControlRules.PEST_ATTACK_TICKS;
            strikeKnight(p);
        }
    }

    /**
     * A shifter teleports beside the Void Knight, or about six blocks towards him from further away, over walls and gates
     * (the first free spot of 6, 7, 5, 8 and 4 blocks; approx: the wiki says "short distances").
     */
    private void blinkTowardsKnight(Mob m) {
        List<Vec3> spots = new ArrayList<>();
        if (m.distanceToSqr(knight) < 144) {
            double a = random.nextDouble() * Math.PI * 2;
            for (int i = 0; i < 4; i++) {
                spots.add(knight.position().add(Math.cos(a + i * Math.PI / 2) * 1.5, 0, Math.sin(a + i * Math.PI / 2) * 1.5));
            }
        } else {
            Vec3 dir = knight.position().subtract(m.position()).normalize();
            for (int d : new int[]{6, 7, 5, 8, 4}) {
                spots.add(m.position().add(dir.scale(d)));
            }
        }
        for (Vec3 to : spots) {
            BlockPos at = BlockPos.containing(to.x, origin.getY() + 1, to.z);
            if (world.getBlockState(at).isAir() && world.getBlockState(at.above()).isAir()) {
                world.sendParticles(ParticleTypes.PORTAL, m.getX(), m.getY() + 0.5, m.getZ(), 20, 0.3, 0.5, 0.3, 0.2);
                m.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
                return;
            }
        }
    }

    /** One accuracy-rolled pest attack on the Void Knight (torchers roll magic accuracy against ranged defence). */
    private void strikeKnight(Pest p) {
        MonsterStats s = MonsterStats.of(p.mob);
        AttackType style = p.id.equals("defiler") ? AttackType.RANGED : p.id.equals("torcher") ? AttackType.MAGIC : AttackType.STAB;
        int level = style == AttackType.RANGED ? s.ranged() : style == AttackType.MAGIC ? s.magic() : s.attack();
        long attackRoll = CombatHandler.npcAttackRoll(p.mob, s, style, level);
        long defenceRoll = CombatHandler.defenceRoll(knight, style == AttackType.MAGIC ? AttackType.RANGED : style);
        int damage = CombatFormulas.roll(new java.util.Random(random.nextLong()), attackRoll, defenceRoll, p.variant.maxHit());
        p.mob.swing(InteractionHand.MAIN_HAND);
        if (style != AttackType.STAB) {
            Vec3 from = p.mob.getEyePosition();
            Vec3 to = knight.position().add(0, 1, 0);
            for (int i = 0; i <= 6; i++) {
                Vec3 at = from.add(to.subtract(from).scale(i / 6.0));
                world.sendParticles(style == AttackType.MAGIC ? ParticleTypes.FLAME : ParticleTypes.CRIT, at.x, at.y, at.z, 1, 0, 0, 0, 0);
            }
        }
        CombatHandler.applyDamage(p.mob, knight, damage);
        knight.setTarget(null);
    }

    /** A splatter detonates: 5-24 typeless damage to every player and monster in the 3x3 around it, and the structures it touches. */
    private void splatterExplosion(Pest p, Vec3 at) {
        p.exploded = true;
        world.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, 3, 0.5, 0.3, 0.5, 0);
        for (ServerPlayer pl : online()) {
            if (pl.position().distanceToSqr(at) <= 2.25) {
                hurt(pl, PestControlRules.SPLATTER_MIN + random.nextInt(PestControlRules.SPLATTER_MAX - PestControlRules.SPLATTER_MIN + 1));
            }
        }
        for (LivingEntity e : living(null)) {
            if (e == p.mob || e == knight || e.position().distanceToSqr(at) > 2.25 || e.getTags().contains(MinigameCombat.INVULNERABLE)) {
                continue;
            }
            int dmg = PestControlRules.SPLATTER_MIN + random.nextInt(PestControlRules.SPLATTER_MAX - PestControlRules.SPLATTER_MIN + 1);
            CombatHandler.applyDamage(null, e, dmg);
            ServerNetworking.hitsplat(e, dmg, Net.SPLAT_DAMAGE);
        }
        for (Barrier b : barriers) {
            if (b.standing() && b.distanceSq(at) <= 4.0) {
                b.damage(PestControlRules.SPLATTER_MIN + random.nextInt(PestControlRules.SPLATTER_MAX - PestControlRules.SPLATTER_MIN + 1));
            }
        }
    }

    /** A spinner whose portal fell explodes: 5 damage and poison to the players around it. */
    private void spinnerExplosion(Pest p) {
        Vec3 at = p.mob.position();
        world.sendParticles(ParticleTypes.ITEM_SLIME, at.x, at.y + 0.5, at.z, 20, 1.0, 0.5, 1.0, 0.1);
        for (ServerPlayer pl : online()) {
            if (pl.position().distanceToSqr(at) <= 9) {
                hurt(pl, PestControlRules.SPINNER_EXPLOSION);
                PoisonService.poison(pl, PestControlRules.SPINNER_POISON, com.runecraft.action.ActionManager.gameTick());
            }
        }
        p.exploded = true;
        p.mob.discard();
        pests.remove(p);
    }

    private Barrier nearestStanding(Vec3 from) {
        Barrier best = null;
        double bd = Double.MAX_VALUE;
        for (Barrier b : barriers) {
            if (!b.standing()) {
                continue;
            }
            double d = b.distanceSq(from);
            if (d < bd) {
                bd = d;
                best = b;
            }
        }
        return best;
    }

    private static String colour(int i) {
        return PestControl.PORTALS[i].replace("_portal", "");
    }

    // ------------------------------------------------------------------ players

    @Override
    protected void onPlayerHit(ServerPlayer player, LivingEntity target, int dealt) {
        String cid = MinigameManager.creatureId(target);
        boolean bonus = cid.endsWith("_portal") || cid.equals("spinner");
        activity.merge(player.getUUID(), PestControlRules.activityAfterHit(0, dealt, bonus),
                (old, gain) -> Math.min(PestControlRules.ACTIVITY_MAX, old + gain));
    }

    @Override
    protected void onDeath(ServerPlayer player) {
        Msg.red(player, "Oh dear, you are dead! You return to the lander.");
        BlockPos at = spawnPoint(player, 0);
        player.teleportTo(world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, player.getYRot(), player.getXRot());
    }

    /** Repairs the nearest damaged gate or barricade within reach with a hammer and a log (5 Crafting XP). */
    boolean repair(ServerPlayer player) {
        Barrier best = null;
        double bd = 9.0;
        for (Barrier b : barriers) {
            double d = b.distanceSq(player.position());
            if (b.hp < b.max && d <= bd) {
                bd = d;
                best = b;
            }
        }
        if (best == null) {
            return false;
        }
        if (!Inv.has(player, "hammer", 1)) {
            Msg.game(player, "You need a hammer to repair the " + best.def.name() + ".");
            return true;
        }
        String log = logIn(player);
        if (log == null) {
            Msg.game(player, "You need some logs to repair the " + best.def.name() + ".");
            return true;
        }
        Inv.remove(player, log, 1);
        best.repair();
        player.swing(InteractionHand.MAIN_HAND);
        xp(player, Skill.CRAFTING, PestControlRules.REPAIR_XP);
        activity.merge(player.getUUID(), PestControlRules.ACTIVITY_PER_REPAIR,
                (old, gain) -> Math.min(PestControlRules.ACTIVITY_MAX, old + gain));
        Msg.game(player, "You repair the " + best.def.name() + ".");
        return true;
    }

    private static String logIn(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            String id = Inv.idOf(player.getInventory().getItem(i));
            if (id.equals("logs") || id.endsWith("_logs") && !id.contains("pyre") && !id.contains("burnt")) {
                return id;
            }
        }
        return null;
    }

    /** Rubs a Void seal: 8-12 damage to every pest within three tiles; eight charges. */
    boolean rubSeal(ServerPlayer player, ItemStack seal) {
        int charges = StackData.get(seal).contains("SealCharges") ? StackData.get(seal).getInt("SealCharges") : PestControlRules.SEAL_CHARGES;
        world.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1, player.getZ(), 30, 1.5, 0.5, 1.5, 0.1);
        int hit = 0;
        for (Pest p : new ArrayList<>(pests)) {
            if (p.mob.isAlive() && p.mob.distanceToSqr(player) <= PestControlRules.SEAL_RADIUS * PestControlRules.SEAL_RADIUS) {
                int dmg = PestControlRules.SEAL_MIN + random.nextInt(PestControlRules.SEAL_MAX - PestControlRules.SEAL_MIN + 1);
                CombatHandler.applyDamage(player, p.mob, dmg);
                hit++;
            }
        }
        charges--;
        if (charges <= 0) {
            seal.shrink(1);
            Msg.game(player, "Your Void seal dissolves as its last charge is used.");
        } else {
            int left = charges;
            StackData.update(seal, t -> t.putInt("SealCharges", left));
            Msg.game(player, "You rub the seal" + (hit == 0 ? "." : " and the pests around you are struck.") + " It has " + left
                    + " charge" + (left == 1 ? "" : "s") + " left.");
        }
        return true;
    }

    @Override
    protected void onEnd(boolean success) {
        for (ServerPlayer p : online()) {
            if (success && won) {
                // The Western Provinces diary's "Complete a novice / intermediate / veteran game of Pest Control".
                com.runecraft.content.GameEvents.fireActivity(p, "minigame_win:pest_control_"
                        + PestControl.TIERS[tier].toLowerCase(java.util.Locale.ROOT));
            }
            int act = activity.getOrDefault(p.getUUID(), 0);
            if (success && won && act > 0) {
                int balance = MinigameData.of(p).points(p.getUUID(), "pest_control");
                int pts = Math.max(0, Math.min(PestControl.pointsAwarded(tier, balance) + extraPoints.applyAsInt(p),
                        PestControl.MAX_POINTS - balance));
                addPoints(p, MinigameCurrencies.PEST_CONTROL, pts);
                reward(p, "coins", Players.data(p).skills().combatLevel() * PestControl.COINS_PER_COMBAT_LEVEL);
                int total = MinigameData.of(p).points(p.getUUID(), "pest_control");
                MinigameData.of(p).incrementStat(p.getUUID(), "kc:" + game.id());
                Msg.game(p, (timedOut ? "Congratulations! The Void Knight survived the attack! " : "Congratulations! You managed to "
                        + "destroy all the portals! ") + "We've awarded you " + pts + " Void Knight Commendation point"
                        + (pts == 1 ? "" : "s") + ". Please also accept these coins as a reward. You now have " + total + " points.");
            } else if (success && won) {
                Msg.game(p, "The Void Knights notice your lack of zeal. You receive no commendation points this time.");
            } else {
                Msg.game(p, "The Void Knight was killed, another of our Order has fallen and that Island is lost.");
            }
            // Hitpoints, prayer and special attack energy are restored after every game (wiki).
            p.setHealth(p.getMaxHealth());
            Players.data(p).setPrayerPoints(Players.data(p).skills().level(Skill.PRAYER));
            com.runecraft.content.systems.SpecialAttackService.restoreEnergy(p, 100);
            ServerNetworking.sync(p);
        }
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        List<String> lines = new ArrayList<>();
        lines.add("Pest Control (" + PestControl.TIERS[tier] + ")");
        lines.add("Time left: " + time(PestControl.GAME_TICKS - ticks));
        lines.add("Void Knight: " + (knight == null ? 0 : (int) Math.ceil(knight.getHealth())) + " / " + PestControl.VOID_KNIGHT_HP);
        String[] letters = {"P", "B", "Y", "R"};
        StringBuilder sb = new StringBuilder("Portals:");
        for (int i = 0; i < 4; i++) {
            Mob p = portals[i];
            sb.append(' ').append(letters[i]).append(' ')
                    .append(destroyed[i] || p == null ? "X" : shielded[i] ? "shield" : String.valueOf((int) Math.ceil(p.getHealth())));
        }
        lines.add(sb.toString());
        lines.add("Activity: " + activity.getOrDefault(player.getUUID(), 0) * 100 / PestControlRules.ACTIVITY_MAX + "%");
        return lines;
    }

    // ------------------------------------------------------------------ hooks

    /** Pests hit as hard as their level (max hit hook), hammers repair, Void seals are rubbed. Called once by the module. */
    public static void register() {
        CombatHooks.addNpcVersusPlayer(new CombatHooks.NpcVersusPlayer() {
            @Override
            public int maxHitBonus(LivingEntity mob, ServerPlayer target, AttackType type) {
                Variant v = variantOf(mob);
                if (v == null) {
                    return 0;
                }
                MonsterStats s = MonsterStats.of(mob);
                if (s == null || s.profile() == null || s.profile().attacks.isEmpty()) {
                    return 0;
                }
                int base = s.profile().attacks.get(0).maxHit();
                for (com.runecraft.osrs.content.AttackDef a : s.profile().attacks) {
                    if (a.style() == type) {
                        base = a.maxHit();
                        break;
                    }
                }
                return v.maxHit() - base;
            }
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (world.isClientSide() || !(player instanceof ServerPlayer sp) || stack.isEmpty()) {
                return InteractionResultHolder.pass(stack);
            }
            String id = Inv.idOf(stack);
            if (!id.equals("void_seal") && !id.equals("hammer")) {
                return InteractionResultHolder.pass(stack);
            }
            PestControlSession s = of(sp);
            if (id.equals("void_seal")) {
                if (s == null) {
                    Msg.game(sp, "The seal can only be used in a game of Pest Control.");
                    return InteractionResultHolder.success(stack);
                }
                s.rubSeal(sp, stack);
                return InteractionResultHolder.success(stack);
            }
            return s != null && s.repair(sp) ? InteractionResultHolder.success(stack) : InteractionResultHolder.pass(stack);
        });
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer sp) || hand != InteractionHand.MAIN_HAND
                    || !Inv.idOf(sp.getMainHandItem()).equals("hammer")) {
                return InteractionResult.PASS;
            }
            PestControlSession s = of(sp);
            return s != null && s.repair(sp) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        });
    }

    /** The Pest Control game a player is in, or null. */
    static PestControlSession of(Player player) {
        return player instanceof ServerPlayer sp && MinigameManager.session(sp) instanceof PestControlSession pc && !pc.isEnded() ? pc : null;
    }

    // ------------------------------------------------------------------ self-test access

    Mob knight() {
        return knight;
    }

    Mob portal(int i) {
        return portals[i];
    }

    boolean shielded(int i) {
        return shielded[i];
    }

    boolean destroyed(int i) {
        return destroyed[i];
    }

    int[] rotation() {
        return rotation;
    }

    int activity(ServerPlayer p) {
        return activity.getOrDefault(p.getUUID(), 0);
    }

    void setActivity(ServerPlayer p, int value) {
        activity.put(p.getUUID(), value);
    }

    long tick() {
        return ticks;
    }

    void selfTestTick() {
        MinigameManager.selfTestTick(this);
    }

    void selfTestTimeOut() {
        ticks = PestControl.GAME_TICKS;
    }
}
