package com.runecraft.content.systems;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatZones;
import com.runecraft.combat.Equipment;
import com.runecraft.combat.HitCalcBridge;
import com.runecraft.combat.MonsterStats;
import com.runecraft.content.BlockHooks;
import com.runecraft.entity.BossEntity;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Chance;
import com.runecraft.osrs.CombatFormulas;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.MultiCombat;
import com.runecraft.osrs.PlayerHitCalc;
import com.runecraft.osrs.QuestInfo;
import com.runecraft.osrs.SingleCombat;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.dungeons.Dungeons;
import com.runecraft.osrs.content.systems.Multicannon;
import com.runecraft.osrs.world.ArenaLayout;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.quest.QuestScripts;
import com.runecraft.registry.ModBlocks;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The dwarf multicannon ({@link Multicannon}): set up by using the cannon base (with all four parts and Dwarf Cannon done),
 * loaded and repaired by clicking it, picked up by clicking it while sneaking. Every game tick the barrel turns to its next
 * direction and fires one cannonball at the nearest monster in that direction's target area (line of sight, single-way
 * rules: under attack in single combat it only shoots that attacker). Accuracy is the owner's attack roll for the wielded
 * weapon (ranged, or the melee style; powered staves the better of both) without target-specific boosts, against the
 * target's heavy ranged defence. Granite dust used with Smithing 50 coats steel cannonballs. Nulodion sells the set and
 * returns lost cannons.
 */
public final class CannonService {
    /** A set-up (or setting-up) cannon. */
    public static final class Cannon {
        public final UUID owner;
        public final ServerLevel world;
        public final BlockPos pos;
        /** Assembly steps done (1 = base ... 4 = complete). */
        public int stage;
        public long nextStep;
        public long placedAt;
        public int facing;
        public int steel;
        public int granite;
        public boolean broken;
        public int fired;

        Cannon(UUID owner, ServerLevel world, BlockPos pos) {
            this.owner = owner;
            this.world = world;
            this.pos = pos;
        }

        public boolean complete() {
            return stage >= Multicannon.PARTS.size();
        }

        public int balls() {
            return steel + granite;
        }
    }

    private static final Map<UUID, Cannon> CANNONS = new HashMap<>();
    /** Blocks the owner may be from a working cannon before it stops firing. // approx */
    private static final double OWNER_RANGE = 32.0;
    /** A cannon can't be set up this close (blocks) to a boss: most bosses destroy cannons. */
    private static final double BOSS_RADIUS = 24.0;

    private CannonService() {
    }

    static void register() {
        BlockHooks.register(Multicannon.BLOCK, CannonService::click);
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            String id = stack.isEmpty() ? "" : Inv.idOf(stack);
            boolean ours = id.equals(Multicannon.BASE) || id.equals(Multicannon.GRANITE_DUST) && Inv.count(player, Multicannon.STEEL_BALL) > 0;
            if (!ours) {
                return InteractionResultHolder.pass(stack);
            }
            if (world.isClientSide() || !(player instanceof ServerPlayer p)) {
                return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
            }
            if (id.equals(Multicannon.BASE)) {
                setUp(p);
            } else {
                coatGranite(p);
            }
            return InteractionResultHolder.sidedSuccess(p.getItemInHand(hand), false);
        });
        QuestScripts.onTalk(Multicannon.NULODION, (p, npc) -> nulodion(p));
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> dispatcher.register(
                net.minecraft.commands.Commands.literal("cannon").then(net.minecraft.commands.Commands.literal("buy")
                        .then(net.minecraft.commands.Commands.argument("item",
                                com.mojang.brigadier.arguments.StringArgumentType.word()).executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayer();
                            if (p != null) {
                                buyPart(p, com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "item"));
                            }
                            return 1;
                        })))));
        SysTicks.everyTick((server, tick) -> tick(server, tick));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            Cannon c = CANNONS.remove(handler.player.getUUID());
            if (c != null) {
                // The cannon is left behind; Nulodion keeps it (and its cannonballs) for the owner.
                remove(c);
                stash(SysState.of(handler.player), c);
            }
        });
    }

    /** The player's cannon, or null. */
    public static Cannon of(ServerPlayer p) {
        return CANNONS.get(p.getUUID());
    }

    // ------------------------------------------------------------------ setting up

    /** Why a cannon can't be set up around {@code centre}, or null if it can. */
    public static String prohibited(ServerPlayer p, BlockPos centre) {
        if (!Gielinor.is(p) || RaidManagerBridge.inRaid(p)
                || com.runecraft.content.minigames.MinigameManager.session(p) != null) {
            return Multicannon.DEFAULT_MESSAGE;
        }
        int ox = GielinorMap.toOsrsX(centre.getX());
        int oy = GielinorMap.toOsrsY(centre.getZ());
        Multicannon.Zone zone = Multicannon.zone(ox, oy);
        if (zone != null) {
            return zone.message();
        }
        for (String[] a : Multicannon.AROUND) {
            int[] c = MultiCombat.placeCentre(a[0]);
            int r = Integer.parseInt(a[1]);
            if (c != null && Math.abs(ox - c[0]) <= r && Math.abs(oy - c[1]) <= r) {
                return a[2];
            }
        }
        for (Dungeons.Camp camp : Dungeons.camps()) {
            String message = Multicannon.DUNGEONS.get(camp.dungeon());
            if (message != null && centre.getX() >= Math.min(camp.x1(), camp.x2()) - 8 && centre.getX() <= Math.max(camp.x1(), camp.x2()) + 8
                    && centre.getZ() >= Math.min(camp.z1(), camp.z2()) - 8 && centre.getZ() <= Math.max(camp.z1(), camp.z2()) + 8
                    && centre.getY() >= camp.feetY() - 6 && centre.getY() <= camp.feetY() + 16) {
                return message;
            }
        }
        for (Map.Entry<String, String> e : Multicannon.BOSS_LAIRS.entrySet()) {
            ArenaLayout.Arena arena = ArenaLayout.of(e.getKey());
            if (arena != null && Math.abs(centre.getX() - arena.x()) <= arena.radius() + 4
                    && Math.abs(centre.getZ() - arena.z()) <= arena.radius() + 4) {
                return e.getValue();
            }
        }
        AABB near = block(centre).inflate(BOSS_RADIUS);
        if (!p.level().getEntitiesOfClass(LivingEntity.class, near, e -> e.isAlive() && destroysCannons(e)).isEmpty()) {
            return "It doesn't seem safe to set up a cannon here.";
        }
        return null;
    }

    /** Whether the 3x3 around {@code centre} is free floor. */
    static boolean clear(ServerLevel world, BlockPos centre) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos at = centre.offset(dx, 0, dz);
                if (!world.getBlockState(at).isAir() || !world.getBlockState(at.above()).isAir() || world.getBlockState(at.below()).isAir()) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Starts setting up a cannon on the tile in front of the player; true if it began. */
    public static boolean setUp(ServerPlayer p) {
        Direction d = p.getDirection();
        return setUp(p, p.blockPosition().relative(d, 2), false);
    }

    /** Sets up a cannon centred on {@code centre}; {@code instant} skips the assembly ticks (self-test). */
    public static boolean setUp(ServerPlayer p, BlockPos centre, boolean instant) {
        QuestInfo quest = ContentRegistry.quest(Multicannon.QUEST);
        if (quest != null && !Players.data(p).questComplete(quest)) {
            Msg.red(p, "You need to complete Dwarf Cannon to set up a dwarf multicannon.");
            return false;
        }
        SysState.SysPlayer sp = SysState.of(p);
        if (CANNONS.containsKey(p.getUUID())) {
            Msg.game(p, "You already have a cannon set up.");
            return false;
        }
        if (sp.cannonOut) {
            Msg.game(p, "Nulodion still has your last cannon. Speak to him to get it back.");
            return false;
        }
        for (String part : Multicannon.PARTS) {
            if (!Inv.has(p, part, 1)) {
                Msg.game(p, "You need the cannon base, stand, barrels and furnace to set up a cannon.");
                return false;
            }
        }
        String why = prohibited(p, centre);
        if (why != null) {
            Msg.red(p, why);
            return false;
        }
        ServerLevel world = (ServerLevel) p.level();
        if (!clear(world, centre)) {
            Msg.game(p, "There isn't enough space to set up a cannon here.");
            return false;
        }
        Block block = ModBlocks.MODULE_BLOCKS.get(Multicannon.BLOCK);
        if (block == null) {
            return false;
        }
        Cannon c = new Cannon(p.getUUID(), world, centre.immutable());
        CANNONS.put(p.getUUID(), c);
        sp.cannonOut = true;
        sp.cannonSteel = 0;
        sp.cannonGranite = 0;
        Inv.remove(p, Multicannon.BASE, 1);
        world.setBlock(c.pos, block.defaultBlockState(), Block.UPDATE_ALL);
        world.playSound(null, c.pos, SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.5f, 1.2f);
        c.stage = 1;
        Msg.game(p, "You place the cannon base on the ground.");
        c.nextStep = SysTicks.now() + Multicannon.SETUP_TICKS_PER_STEP;
        if (instant) {
            while (!c.complete()) {
                assemble(p, c);
            }
        }
        return true;
    }

    private static void assemble(ServerPlayer p, Cannon c) {
        String part = Multicannon.PARTS.get(c.stage);
        if (!Inv.has(p, part, 1)) {
            Msg.red(p, "You need the cannon " + part.substring("cannon_".length()) + " to finish setting up your cannon.");
            c.nextStep = SysTicks.now() + 5;
            return;
        }
        Inv.remove(p, part, 1);
        c.stage++;
        c.nextStep = SysTicks.now() + Multicannon.SETUP_TICKS_PER_STEP;
        switch (part) {
            case Multicannon.STAND -> Msg.game(p, "You add the stand.");
            case Multicannon.BARRELS -> Msg.game(p, "You add the barrels.");
            default -> Msg.game(p, "You add the furnace.");
        }
        if (c.complete()) {
            c.placedAt = SysTicks.now();
        }
    }

    // ------------------------------------------------------------------ clicking it

    private static void click(ServerPlayer p, BlockPos pos, BlockState state) {
        Cannon c = null;
        for (Cannon k : CANNONS.values()) {
            if (k.pos.equals(pos) && k.world == p.level()) {
                c = k;
            }
        }
        if (c == null) {
            // Left over from before a restart: the owner gets it back from Nulodion.
            p.level().removeBlock(pos, false);
            Msg.game(p, "The abandoned cannon falls apart. Its owner can reclaim it from Nulodion.");
            return;
        }
        if (!c.owner.equals(p.getUUID())) {
            Msg.game(p, "This isn't your cannon!");
            return;
        }
        if (p.isShiftKeyDown()) {
            pickUp(p);
        } else if (!c.complete()) {
            Msg.game(p, "You're still setting up your cannon.");
        } else if (c.broken) {
            c.broken = false;
            c.placedAt = SysTicks.now();
            Msg.game(p, "You repair your cannon, restoring it to working order.");
        } else {
            load(p);
        }
    }

    /** Loads cannonballs (granite first; a cannon holding granite takes no steel until it is empty); returns how many. */
    public static int load(ServerPlayer p) {
        Cannon c = of(p);
        if (c == null || !c.complete()) {
            return 0;
        }
        int capacity = Multicannon.capacity(CombatAchievementService.tier(p));
        int room = capacity - c.balls();
        if (room <= 0) {
            Msg.game(p, "Your cannon is already full.");
            return 0;
        }
        int added = 0;
        if (c.steel == 0) {
            added = Inv.remove(p, Multicannon.GRANITE_BALL, room);
            c.granite += added;
        }
        if (added == 0 && c.granite == 0) {
            added = Inv.remove(p, Multicannon.STEEL_BALL, room);
            c.steel += added;
        }
        if (added == 0) {
            Msg.game(p, c.granite > 0 ? "Your cannon is loaded with granite cannonballs; use them up first."
                    : "You don't have any cannonballs.");
            return 0;
        }
        Msg.game(p, "You load the cannon with " + added + " cannonball" + (added == 1 ? "" : "s") + ".");
        return added;
    }

    /** Picks the cannon up: the four parts and its cannonballs go back to the inventory. */
    public static boolean pickUp(ServerPlayer p) {
        Cannon c = CANNONS.remove(p.getUUID());
        if (c == null) {
            return false;
        }
        remove(c);
        for (int i = 0; i < c.stage; i++) {
            Inv.give(p, Multicannon.PARTS.get(i), 1);
        }
        giveBalls(p, c.steel, c.granite);
        SysState.SysPlayer sp = SysState.of(p);
        sp.cannonOut = false;
        sp.cannonSteel = 0;
        sp.cannonGranite = 0;
        Msg.game(p, "You pick up the cannon. It's really heavy.");
        return true;
    }

    private static void giveBalls(ServerPlayer p, int steel, int granite) {
        if (steel > 0) {
            Inv.give(p, Multicannon.STEEL_BALL, steel);
        }
        if (granite > 0) {
            Inv.give(p, Multicannon.GRANITE_BALL, granite);
        }
    }

    private static void remove(Cannon c) {
        if (c.world.getBlockState(c.pos).getBlock() == ModBlocks.MODULE_BLOCKS.get(Multicannon.BLOCK)) {
            c.world.removeBlock(c.pos, false);
        }
    }

    private static void stash(SysState.SysPlayer sp, Cannon c) {
        sp.cannonOut = true;
        sp.cannonSteel = c.steel;
        sp.cannonGranite = c.granite;
    }

    // ------------------------------------------------------------------ firing

    private static void tick(MinecraftServer server, long now) {
        if (CANNONS.isEmpty()) {
            return;
        }
        for (Cannon c : new ArrayList<>(CANNONS.values())) {
            ServerPlayer owner = server.getPlayerList().getPlayer(c.owner);
            if (owner == null) {
                continue;
            }
            try {
                tick(owner, c, now);
            } catch (RuntimeException e) {
                com.runecraft.RuneCraft.LOGGER.error("Dwarf multicannon tick failed", e);
            }
        }
    }

    /** One game tick of a cannon: assembly, wear and one turn of the barrel (firing if loaded). */
    public static void tick(ServerPlayer owner, Cannon c, long now) {
        if (!c.complete()) {
            if (now >= c.nextStep) {
                assemble(owner, c);
            }
            return;
        }
        switch (Multicannon.life(now - c.placedAt)) {
            case DECAYS -> {
                CANNONS.remove(c.owner);
                remove(c);
                stash(SysState.of(owner), c);
                Msg.red(owner, "Your cannon has decayed. Speak to Nulodion to obtain a new one.");
                return;
            }
            case BREAKS -> {
                if (!c.broken) {
                    c.broken = true;
                    Msg.red(owner, "Your cannon has broken!");
                }
                return;
            }
            default -> {
            }
        }
        if (c.broken || c.balls() <= 0 || owner.level() != c.world
                || owner.distanceToSqr(Vec3.atCenterOf(c.pos)) > OWNER_RANGE * OWNER_RANGE) {
            return;
        }
        c.facing = Multicannon.next(c.facing);
        int[] step = Multicannon.step(c.facing);
        c.world.sendParticles(ParticleTypes.SMOKE, c.pos.getX() + 0.5 + step[0] * 0.6, c.pos.getY() + 1.1,
                c.pos.getZ() + 0.5 - step[1] * 0.6, 1, 0.02, 0.02, 0.02, 0.0);
        LivingEntity target = target(owner, c);
        if (target != null) {
            fire(owner, c, target);
        }
    }

    /** The monster the cannon shoots this tick (nearest in the facing direction's area), or null. */
    static LivingEntity target(ServerPlayer owner, Cannon c) {
        boolean single = !CombatZones.isMulti(owner);
        LivingEntity attacker = owner.getLastHurtByMob();
        boolean underAttack = single && attacker != null && attacker.isAlive() && validTarget(attacker);
        double reach = Multicannon.reachBlocks();
        AABB box = block(c.pos).inflate(reach, 4, reach);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : c.world.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && validTarget(e))) {
            if (underAttack && e != attacker) {
                continue;
            }
            // Target areas are in OSRS tiles (2 blocks each), measured from the tile under the cannon's centre block.
            int[] d = Multicannon.tileDelta(c.pos.getX(), c.pos.getZ(), e.getBlockX(), e.getBlockZ());
            if (!Multicannon.inArea(c.facing, d[0], d[1])) {
                continue;
            }
            if (CombatZones.check(owner, e) != SingleCombat.Verdict.OK) {
                continue;
            }
            double dist = e.distanceToSqr(Vec3.atCenterOf(c.pos));
            if (dist < bestDist && lineOfSight(owner, c, e)) {
                best = e;
                bestDist = dist;
            }
        }
        return best;
    }

    static boolean validTarget(LivingEntity e) {
        if (e instanceof ServerPlayer || e instanceof QuestNpcEntity || e instanceof BossEntity || e.isInvulnerable()
                || Multicannon.immune(creatureId(e))) {
            return false;
        }
        return e instanceof OsrsMonsterEntity || e instanceof Monster
                || e instanceof Mob && com.runecraft.osrs.data.Monster.byEntity(
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString()) != null;
    }

    private static AABB block(BlockPos pos) {
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }

    static boolean isBoss(LivingEntity e) {
        return e instanceof BossEntity || e instanceof DataBossEntity;
    }

    /** The RuneCraft creature id of an entity (a data boss's id, or the entity type's path), or null. */
    public static String creatureId(LivingEntity e) {
        if (e instanceof DataBossEntity b && b.def() != null) {
            return b.def().id;
        }
        net.minecraft.resources.ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return id == null ? null : id.getPath();
    }

    /** A boss that destroys cannons set up near it (all but {@link Multicannon#CANNON_BOSSES}). */
    static boolean destroysCannons(LivingEntity e) {
        return isBoss(e) && !Multicannon.CANNON_BOSSES.contains(creatureId(e));
    }

    private static boolean lineOfSight(ServerPlayer owner, Cannon c, LivingEntity e) {
        Vec3 from = new Vec3(c.pos.getX() + 0.5, c.pos.getY() + 1.2, c.pos.getZ() + 0.5);
        Vec3 to = new Vec3(e.getX(), e.getY(0.5), e.getZ());
        return c.world.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                owner)).getType() == HitResult.Type.MISS;
    }

    /** Fires one cannonball at {@code target}; returns the damage rolled. */
    public static int fire(ServerPlayer owner, Cannon c, LivingEntity target) {
        boolean granite = c.granite > 0;
        if (granite) {
            c.granite--;
        } else {
            c.steel--;
        }
        c.fired++;
        c.world.playSound(null, c.pos, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.35f, 1.6f);
        c.world.sendParticles(ParticleTypes.LARGE_SMOKE, target.getX(), target.getY(0.6), target.getZ(), 3, 0.2, 0.2, 0.2, 0.01);
        int damage = 0;
        if (owner.getRandom().nextDouble() < accuracy(owner, target)) {
            damage = owner.getRandom().nextInt(Multicannon.maxHit(granite) + 1);
        }
        int dealt = CombatHandler.applyDamage(owner, target, damage);
        if (dealt > 0) {
            XpService.addTenths(owner, Skill.RANGED, dealt * Multicannon.XP_TENTHS_PER_DAMAGE);
        }
        if (c.balls() == 0) {
            Msg.game(owner, "Your cannon is out of ammo!");
        }
        return damage;
    }

    /**
     * Chance to hit: the owner's attack roll for their weapon (ranged bonus with a ranged weapon, the selected melee
     * style's bonus otherwise, the better of the two with a powered staff), without target-specific boosts (slayer helm,
     * salve amulet...), against the target's heavy ranged defence.
     */
    public static double accuracy(ServerPlayer owner, LivingEntity target) {
        EquipmentDef weapon = Equipment.weapon(owner, Players.data(owner).skills());
        EquipmentDef.WeaponClass cls = weapon == null ? EquipmentDef.WeaponClass.NONE : weapon.weaponClass();
        String weaponId = Inv.idOf(owner.getMainHandItem());
        boolean powered = com.runecraft.osrs.CombatBonuses.poweredStaffMaxHit(weaponId, 99) >= 0;
        long roll;
        if (powered) {
            roll = Math.max(attackRoll(owner, target, AttackType.RANGED),
                    attackRoll(owner, target, Equipment.meleeType(owner, Players.data(owner).skills())));
        } else if (cls == EquipmentDef.WeaponClass.BOW) {
            roll = attackRoll(owner, target, AttackType.RANGED);
        } else {
            roll = attackRoll(owner, target, Equipment.meleeType(owner, Players.data(owner).skills()));
        }
        MonsterStats stats = MonsterStats.of(target);
        int[] ranged = stats.rangedDefences();
        int heavy = ranged != null ? ranged[2] : stats.defenceBonus(AttackType.RANGED);
        return Chance.hitChance(roll, CombatFormulas.npcRoll(stats.defence(), heavy));
    }

    private static long attackRoll(ServerPlayer owner, LivingEntity target, AttackType type) {
        PlayerHitCalc.Input in = HitCalcBridge.input(owner, target, type, null, 0);
        in.onTask = false;
        in.attributes.clear();
        in.revenant = false;
        return PlayerHitCalc.attackRoll(in);
    }

    // ------------------------------------------------------------------ granite cannonballs, Nulodion

    /** Coats steel cannonballs with granite dust (50 Smithing, one dust per ball, no XP); returns how many. */
    public static int coatGranite(ServerPlayer p) {
        if (Players.data(p).skills().currentLevel(Skill.SMITHING) < Multicannon.GRANITE_LEVEL) {
            Msg.game(p, "You need a Smithing level of " + Multicannon.GRANITE_LEVEL + " to make granite cannonballs.");
            return 0;
        }
        int n = Math.min(Inv.count(p, Multicannon.STEEL_BALL), Inv.count(p, Multicannon.GRANITE_DUST));
        if (n <= 0) {
            return 0;
        }
        Inv.remove(p, Multicannon.STEEL_BALL, n);
        Inv.remove(p, Multicannon.GRANITE_DUST, n);
        Inv.give(p, Multicannon.GRANITE_BALL, n);
        Msg.game(p, "You apply a thick coating of granite dust to your cannonballs.");
        return n;
    }

    /** Nulodion: swaps the old quest mould, returns a lost cannon, and sells the set (sneak while talking to buy). */
    public static void nulodion(ServerPlayer p) {
        String name = "Nulodion";
        if (Inv.has(p, Multicannon.OLD_QUEST_MOULD, 1)) {
            int n = Inv.remove(p, Multicannon.OLD_QUEST_MOULD, 64);
            Inv.give(p, Multicannon.AMMO_MOULD, n);
            QuestApi.say(p, name, "That old mould's no good. Here, take a proper one.");
        }
        QuestInfo quest = ContentRegistry.quest(Multicannon.QUEST);
        if (quest != null && !Players.data(p).questComplete(quest)) {
            QuestApi.say(p, name, "Hello traveller. I'm the Black Guard's weapons engineer. Captain Lawgof might need your help.");
            return;
        }
        SysState.SysPlayer sp = SysState.of(p);
        if (sp.cannonOut && !CANNONS.containsKey(p.getUUID())) {
            for (String part : Multicannon.PARTS) {
                Inv.give(p, part, 1);
            }
            giveBalls(p, sp.cannonSteel, sp.cannonGranite);
            sp.cannonOut = false;
            sp.cannonSteel = 0;
            sp.cannonGranite = 0;
            QuestApi.say(p, name, "I found your cannon lying around. Here you go, cannonballs and all. Try not to lose it again!");
            return;
        }
        if (!p.isShiftKeyDown()) {
            QuestApi.say(p, name, "Hello again. I can sell you a dwarf multicannon: all four parts and an ammo mould for "
                    + String.format("%,d", Multicannon.SET_PRICE) + " coins (talk to me while sneaking), or the parts one at a time.");
            net.minecraft.network.chat.MutableComponent line = net.minecraft.network.chat.Component.literal("Multicannon parts for sale: ")
                    .withStyle(net.minecraft.ChatFormatting.GRAY);
            for (String part : Multicannon.PARTS) {
                line.append(buyLink(Inv.stack(part, 1).getHoverName().getString(), part, Multicannon.PART_PRICE)).append(" ");
            }
            line.append(buyLink("Ammo mould", Multicannon.AMMO_MOULD, Multicannon.MOULD_PRICE));
            p.displayClientMessage(line, false);
            return;
        }
        if (!Inv.has(p, Inv.COINS, Multicannon.SET_PRICE)) {
            QuestApi.say(p, name, "You don't have enough coins. The set costs " + String.format("%,d", Multicannon.SET_PRICE) + ".");
            return;
        }
        Inv.remove(p, Inv.COINS, Multicannon.SET_PRICE);
        for (String part : Multicannon.PARTS) {
            Inv.give(p, part, 1);
        }
        Inv.give(p, Multicannon.AMMO_MOULD, 1);
        QuestApi.say(p, name, "There you go, a fine dwarf multicannon. Use the base to set it up somewhere roomy.");
    }

    private static net.minecraft.network.chat.MutableComponent buyLink(String label, String item, int price) {
        return net.minecraft.network.chat.Component.literal("[" + label + " " + String.format("%,d", price) + "]").withStyle(st -> st
                .withColor(net.minecraft.ChatFormatting.GOLD)
                .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/cannon buy " + item))
                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                        net.minecraft.network.chat.Component.literal("Buy one " + label))));
    }

    /**
     * Nulodion's shop ("Multicannon parts for sale", OSRS Wiki: 107% of value, so 200,625 coins a part and 5 for the ammo
     * mould): sells one part (or mould) after Dwarf Cannon. Shop stock limits (5 of each) are not modelled. Returns success.
     */
    public static boolean buyPart(ServerPlayer p, String item) {
        int price = item.equals(Multicannon.AMMO_MOULD) ? Multicannon.MOULD_PRICE
                : Multicannon.PARTS.contains(item) ? Multicannon.PART_PRICE : -1;
        if (price < 0) {
            return false;
        }
        QuestInfo quest = ContentRegistry.quest(Multicannon.QUEST);
        if (quest != null && !Players.data(p).questComplete(quest)) {
            Msg.game(p, "Nulodion only sells cannon parts to those who have completed Dwarf Cannon.");
            return false;
        }
        double nx = GielinorMap.toBlockX(Multicannon.NULODION_X);
        double nz = GielinorMap.toBlockZ(Multicannon.NULODION_Y);
        if (p.distanceToSqr(nx, p.getY(), nz) > 24 * 24) {
            Msg.game(p, "You need to be at Nulodion's workshop to buy cannon parts.");
            return false;
        }
        if (!Inv.has(p, Inv.COINS, price)) {
            Msg.game(p, "You don't have enough coins.");
            return false;
        }
        Inv.remove(p, Inv.COINS, price);
        Inv.give(p, item, 1);
        Msg.game(p, "You buy " + Inv.stack(item, 1).getHoverName().getString() + " for " + String.format("%,d", price) + " coins.");
        return true;
    }

    /** Raids are checked through the raids module's manager. */
    private static final class RaidManagerBridge {
        static boolean inRaid(ServerPlayer p) {
            return com.runecraft.content.raids.RaidManager.at(p) != null;
        }
    }
}
