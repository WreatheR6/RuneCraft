package com.runecraft.content.godwars;

import com.runecraft.action.ActionManager;
import com.runecraft.combat.Aggression;
import com.runecraft.combat.CombatHandler;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.systems.SysState;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.item.ItemCharges;
import com.runecraft.osrs.Experience;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.godwars.FeroxEnclave;
import com.runecraft.osrs.content.godwars.RevenantCaves;
import com.runecraft.osrs.content.godwars.RevenantRewards;
import com.runecraft.osrs.content.npcs.WildernessTransport;
import com.runecraft.osrs.content.skilling.AgilityRewards;
import com.runecraft.osrs.content.systems.Charges;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.osrs.data.AgilityCourse;
import com.runecraft.platform.UseItemCallback;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.world.Gielinor;
import com.runecraft.world.StructurePlacer;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Self-test drivers for the Wilderness, walked as the real player through the real blocks, items and hit paths:
 * <ul>
 *   <li>{@code boss:revenant_caves}: the level 17 cavern (the 100,000 coin fee), the easy Agility pillars, a revenant met
 *       with and without a charged bracelet of ethereum, Viggora's chainmace refused until activated with 1,000 revenant
 *       ether, then charged and swung until the revenant dies (ether drop, respawn timer), and the stairs out;</li>
 *   <li>{@code minigame:wilderness_agility}: the dispenser's fee, a lap of the course, the ticket and the lap loot;</li>
 *   <li>{@code minigame:wilderness_obelisk_ferox}: the level 13 obelisk's countdown and teleport, the Ferox Enclave's safe
 *       zone (no looting bag), its barrier against a tele-blocked player and its pool of refreshment.</li>
 * </ul>
 * Every broken step throws an {@link AssertionError} naming it; the player's state is put back after.
 */
public final class WildernessWalk {
    private final ServerPlayer player;
    private final ServerLevel world;
    private final StringBuilder log = new StringBuilder();

    private static final net.minecraft.resources.ResourceLocation HP_BUFFER = com.runecraft.RuneCraft.id("selftest_wild_hp");
    /** Items the walks hand out (and take back after). */
    private static final String[] GEAR = {Inv.COINS, "viggoras_chainmace", "revenant_ether", "bracelet_of_ethereum", "looting_bag",
            AgilityRewards.WILDERNESS_TICKET, "lobster"};
    private static final Skill[] LEVELS = {Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.HITPOINTS, Skill.AGILITY, Skill.PRAYER,
            Skill.MAGIC};

    private WildernessWalk(ServerPlayer player) {
        this.player = player;
        this.world = Gielinor.world(player.getServer());
    }

    public static String revenants(ServerPlayer player, BlockPos back) {
        return run(player, back, WildernessWalk::revenantCaves);
    }

    public static String agility(ServerPlayer player, BlockPos back) {
        return run(player, back, WildernessWalk::agilityLap);
    }

    public static String obeliskFerox(ServerPlayer player, BlockPos back) {
        return run(player, back, WildernessWalk::obeliskAndFerox);
    }

    private interface Steps {
        void walk(WildernessWalk w);
    }

    /** Sets the player up, runs the steps and puts everything back. */
    private static String run(ServerPlayer player, BlockPos back, Steps steps) {
        WildernessState state = WildernessState.get(player.getServer());
        Map<String, Integer> saved = state.snapshot(player.getUUID());
        PlayerData data = Players.data(player);
        int[] xp = data.skills().rawXp();
        int[] boosts = data.skills().rawBoosts();
        int[] gear = new int[GEAR.length];
        for (int i = 0; i < GEAR.length; i++) {
            gear[i] = Inv.count(player, GEAR[i]);
        }
        SysState.SysPlayer sp = SysState.of(player);
        ItemStack hands = sp.worn(Jewellery.Slot.HANDS).copy();
        ItemStack mainHand = player.getMainHandItem().copy();
        int spec = sp.specEnergy;
        try {
            for (Skill s : LEVELS) {
                data.skills().setXpTenths(s, Math.max(data.skills().xpTenths(s), Experience.xpForLevel(90) * 10));
            }
            AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
            if (health != null) {
                health.removeModifier(HP_BUFFER);
                health.addTransientModifier(new AttributeModifier(HP_BUFFER, 2000, AttributeModifier.Operation.ADD_VALUE));
            }
            player.setHealth(player.getMaxHealth());
            WildernessWalk w = new WildernessWalk(player);
            try {
                steps.walk(w);
                return w.log.toString();
            } catch (AssertionError e) {
                throw new AssertionError(e.getMessage() + " (done: " + w.log + ")");
            }
        } finally {
            AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
            if (health != null) {
                health.removeModifier(HP_BUFFER);
                player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
            }
            state.restore(player.getUUID(), saved);
            com.runecraft.combat.Teleblock.forget(player.getUUID());
            data.skills().load(xp, boosts);
            sp.worn.put(Jewellery.Slot.HANDS, hands);
            sp.specEnergy = spec;
            player.setItemInHand(InteractionHand.MAIN_HAND, mainHand);
            for (int i = 0; i < GEAR.length; i++) {
                int extra = Inv.count(player, GEAR[i]) - gear[i];
                if (extra > 0) {
                    Inv.remove(player, GEAR[i], extra);
                } else if (extra < 0) {
                    Inv.give(player, GEAR[i], -extra);
                }
            }
            com.runecraft.network.ServerNetworking.sync(player);
            if (back != null && player.isAlive()) {
                player.teleportTo(back.getX() + 0.5, back.getY(), back.getZ() + 0.5);
            }
        }
    }

    private static void expect(boolean ok, String message) {
        if (!ok) {
            throw new AssertionError(message);
        }
    }

    private void note(String s) {
        log.append(log.length() == 0 ? "" : "; ").append(s);
    }

    private void heal() {
        player.setHealth(player.getMaxHealth());
    }

    private static String idOf(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
    }

    /** The nearest block with this id around a column (searching down from {@code yTop}), or null. */
    private BlockPos find(int bx, int bz, int radius, int yBottom, int yTop, String id) {
        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    for (int y = yTop; y >= yBottom; y--) {
                        BlockPos pos = new BlockPos(bx + dx, y, bz + dz);
                        if (idOf(world.getBlockState(pos)).equals(id)) {
                            return pos;
                        }
                    }
                }
            }
        }
        return null;
    }

    /** Right-clicks a scripted block through BlockHooks. */
    private void use(BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        BlockHooks.use(player, idOf(state), pos, state);
    }

    /** Sneak-uses the held item through the real right-click event (recharging a weapon). */
    private void sneakUseHeld() {
        player.setShiftKeyDown(true);
        try {
            UseItemCallback.EVENT.invoker().interact(player, world, InteractionHand.MAIN_HAND);
        } finally {
            player.setShiftKeyDown(false);
        }
    }

    /** One real melee swing at a creature (the attack timer is skipped). */
    private void swing(Mob target) {
        Players.data(player).lastAttackTick = -1_000_000;
        target.invulnerableTime = 0;
        target.hurt(player.damageSources().playerAttack(player), 1f);
    }

    // ------------------------------------------------------------------ the Revenant Caves

    private void revenantCaves() {
        expect(world != null, "no Gielinor");
        RevenantCaves.Entrance entrance = RevenantCaves.ENTRANCES.get(0);
        int ex = GielinorMap.toBlockX(entrance.x1());
        int ez = GielinorMap.toBlockZ(entrance.y1());
        StructurePlacer.applyAround(world, ex, ez, 16);
        BlockPos top = Gielinor.safeSpot(world, ex, ez + 6);
        BlockPos mouth = find(ex, ez, 6, Math.max(world.getMinBuildHeight(), top.getY() - 20), top.getY() + 10, RevenantCaves.BLOCK_CAVERN);
        expect(mouth != null, "no cavern block at the level " + entrance.level() + " entrance (" + entrance.x1() + "," + entrance.y1() + ")");
        player.teleportTo(world, top.getX() + 0.5, top.getY(), top.getZ() + 0.5, 0, 0);
        // Without the fee (none carried or banked) the caves stay shut.
        int coins = Inv.count(player, Inv.COINS);
        Inv.remove(player, Inv.COINS, coins);
        long banked = Players.data(player).bank().count(Inv.COINS, "");
        if (banked >= RevenantCaves.FEE) {
            Players.data(player).bank().withdraw(Inv.COINS, "", banked);
        }
        use(mouth);
        expect(!RevenantCavesService.inCaves(player), "entered the caves without paying the fee");
        Inv.give(player, Inv.COINS, RevenantCaves.FEE + 5);
        use(mouth);
        expect(RevenantCavesService.inCaves(player), "the cavern didn't lead into the caves (" + player.blockPosition().toShortString() + ")");
        expect(Inv.count(player, Inv.COINS) == 5, "the 100,000 coin fee wasn't taken");
        expect(Gielinor.wildernessLevel(player) >= 17, "the caves under level 17 are level " + Gielinor.wildernessLevel(player));
        if (banked >= RevenantCaves.FEE) {
            Players.data(player).bank().deposit(Inv.COINS, "", banked, 0);
        }
        note("cavern (fee paid, level " + Gielinor.wildernessLevel(player) + ")");

        // The easy Agility pillars (65): its pillar block jumps the player to the far end.
        RevenantCaves.Pillar pillar = RevenantCaves.PILLARS.get(0);
        RevenantCavesService.land(player, pillar.ax(), pillar.ay());
        BlockPos pillarBlock = find(RevenantCaves.blockX(pillar.px()), RevenantCaves.blockZ(pillar.py()), 2, RevenantCaves.FLOOR - 2,
                RevenantCaves.FLOOR + RevenantCaves.HEIGHT, RevenantCaves.BLOCK_PILLAR);
        expect(pillarBlock != null, "no pillar block at " + pillar.px() + "," + pillar.py());
        int agilityXp = Players.data(player).skills().xpTenths(Skill.AGILITY);
        use(pillarBlock);
        int[] at = RevenantCaves.tileAt(player.getBlockX(), player.getBlockZ());
        expect(Math.abs(at[0] - pillar.bx()) <= 1 && Math.abs(at[1] - pillar.by()) <= 1, "the pillars didn't take the player across (at "
                + at[0] + "," + at[1] + ", want " + pillar.bx() + "," + pillar.by() + ")");
        expect(Players.data(player).skills().xpTenths(Skill.AGILITY) > agilityXp, "no Agility XP for the pillars");
        heal();
        note("pillars " + pillar.id());

        // A revenant imp: attacks on sight without a charged bracelet, not with one.
        int index = -1;
        for (int i = 0; i < RevenantCaves.SPAWNS.size(); i++) {
            if (RevenantCaves.SPAWNS.get(i).creature().equals("revenant_imp")) {
                index = i;
                break;
            }
        }
        expect(index >= 0, "no revenant imp spawn point");
        RevenantCaves.Spawn spawn = RevenantCaves.SPAWNS.get(index);
        RevenantCavesService.land(player, spawn.x() + 1, spawn.y());
        RevenantCavesService.resetTimer(index);
        Mob old = RevenantCavesService.find(world, index);
        if (old != null) {
            old.discard();
        }
        RevenantCavesService.stock(world, List.of(player));
        Mob rev = RevenantCavesService.find(world, index);
        expect(rev != null, "the revenant imp didn't spawn at " + spawn.x() + "," + spawn.y());
        expect(rev instanceof OsrsMonsterEntity o && o.attackInterceptor != null, "the revenant has no revenant behaviour");
        SysState.SysPlayer sp = SysState.of(player);
        sp.worn.put(Jewellery.Slot.HANDS, ItemStack.EMPTY);
        expect(!Aggression.peaceful(rev, player), "a revenant left alone a player without a bracelet of ethereum");
        ItemStack bracelet = Inv.stack("bracelet_of_ethereum", 1);
        sp.worn.put(Jewellery.Slot.HANDS, bracelet);
        ItemCharges.set(bracelet, 0);
        expect(!Aggression.peaceful(rev, player), "an empty bracelet of ethereum kept a revenant away");
        ItemCharges.set(bracelet, 50);
        expect(Aggression.peaceful(rev, player), "a charged bracelet of ethereum didn't keep the revenant from attacking");
        note("bracelet (charged: peaceful)");

        // Viggora's chainmace: refused until activated with 1,000 ether, then charged with the rest and swung.
        ItemStack mace = Inv.stack("viggoras_chainmace", 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, mace);
        float hp = rev.getHealth();
        swing(rev);
        expect(rev.getHealth() == hp, "an unactivated Viggora's chainmace hit the revenant");
        Inv.give(player, "revenant_ether", Charges.WILDERNESS_WEAPON_ACTIVATION + 200);
        sneakUseHeld();
        ItemStack held = player.getMainHandItem();
        expect(ItemCharges.activated(held), "1,000 revenant ether didn't activate the chainmace");
        expect(ItemCharges.get(held) == 200, "the chainmace holds " + ItemCharges.get(held) + " ether after activation, expected 200");
        expect(Inv.count(player, "revenant_ether") == 0, "the ether wasn't used up");
        int swings = 0;
        int etherNear = etherAround(rev);
        while (rev.isAlive() && swings < 80) {
            swing(rev);
            heal();
            swings++;
        }
        expect(!rev.isAlive(), "the revenant imp survived " + swings + " chainmace swings");
        expect(ItemCharges.get(player.getMainHandItem()) < 200, "swinging the chainmace used no ether");
        expect(etherAround(rev) > etherNear || Inv.count(player, "revenant_ether") > 0, "the revenant dropped no revenant ether");
        expect(RevenantCavesService.respawnLeft(index) > 0, "the revenant has no respawn timer");
        note("chainmace activated, revenant imp killed in " + swings + " swings (" + ItemCharges.get(player.getMainHandItem()) + " ether left)");

        // The stairs out: one way to the trapdoor in level 17.
        RevenantCaves.Exit exit = RevenantCaves.EXITS.get(0);
        RevenantCavesService.land(player, exit.x() + 1, exit.y());
        BlockPos stairs = find(RevenantCaves.blockX(exit.x()), RevenantCaves.blockZ(exit.y()), 2, RevenantCaves.FLOOR - 2,
                RevenantCaves.FLOOR + RevenantCaves.HEIGHT, RevenantCaves.BLOCK_STAIRS);
        expect(stairs != null, "no stairs block at " + exit.x() + "," + exit.y());
        use(stairs);
        expect(!RevenantCavesService.inCaves(player), "the stairs didn't lead out");
        int[] out = {GielinorMap.toOsrsX(player.getBlockX()), GielinorMap.toOsrsY(player.getBlockZ())};
        expect(Math.abs(out[0] - exit.toX()) <= 3 && Math.abs(out[1] - exit.toY()) <= 3, "the stairs came out at " + out[0] + "," + out[1]
                + ", not by the trapdoor at " + exit.toX() + "," + exit.toY());
        note("stairs out by the trapdoor");
    }

    private int etherAround(Mob near) {
        int n = 0;
        for (ItemEntity i : world.getEntitiesOfClass(ItemEntity.class, new AABB(near.blockPosition()).inflate(8), e -> true)) {
            if ("revenant_ether".equals(Inv.idOf(i.getItem()))) {
                n += i.getItem().getCount();
            }
        }
        return n;
    }

    // ------------------------------------------------------------------ the Wilderness Agility Course

    private void agilityLap() {
        expect(world != null, "no Gielinor");
        AgilityCourse course = AgilityCourse.WILDERNESS;
        int cx = (AgilityRewards.WILDERNESS_COURSE[0] + AgilityRewards.WILDERNESS_COURSE[2]) / 2;
        int cy = (AgilityRewards.WILDERNESS_COURSE[1] + AgilityRewards.WILDERNESS_COURSE[3]) / 2;
        BlockPos in = Gielinor.safeSpot(world, GielinorMap.toBlockX(cx), GielinorMap.toBlockZ(cy));
        player.teleportTo(world, in.getX() + 0.5, in.getY(), in.getZ() + 0.5, 0, 0);
        expect(AgilityRewards.inWildernessCourse(GielinorMap.toOsrsX(player.getBlockX()), GielinorMap.toOsrsY(player.getBlockZ())),
                "not inside the course after teleporting to its middle");
        // Sneak + use the dispenser: the 150,000 coin fee for lap loot.
        Inv.give(player, Inv.COINS, AgilityRewards.WILDERNESS_FEE);
        int coins = Inv.count(player, Inv.COINS);
        BlockState dispenser = BuiltInRegistries.BLOCK.get(com.runecraft.RuneCraft.id("agility_ticket_dispenser")).defaultBlockState();
        player.setShiftKeyDown(true);
        try {
            BlockHooks.use(player, "agility_ticket_dispenser", player.blockPosition(), dispenser);
        } finally {
            player.setShiftKeyDown(false);
        }
        expect(Inv.count(player, Inv.COINS) == coins - AgilityRewards.WILDERNESS_FEE, "the dispenser didn't take the 150,000 coin fee");
        // One lap: every obstacle in order through the real step path.
        PlayerData data = Players.data(player);
        int laps = data.agilityLaps(course);
        int xp = data.skills().xpTenths(Skill.AGILITY);
        for (int step = 0; step < course.obstacles(); step++) {
            com.runecraft.skill.AgilityService.step(player, course, step);
        }
        expect(data.agilityLaps(course) == laps + 1, "the " + course.obstacles() + " obstacles in order didn't complete a lap");
        expect(data.skills().xpTenths(Skill.AGILITY) > xp, "no Agility XP for the lap");
        // Tag the dispenser: a ticket for the lap and its loot (the fee was paid).
        int tickets = Inv.count(player, AgilityRewards.WILDERNESS_TICKET);
        int loot = lootCarried();
        BlockHooks.use(player, "agility_ticket_dispenser", player.blockPosition(), dispenser);
        expect(Inv.count(player, AgilityRewards.WILDERNESS_TICKET) >= tickets + 1, "tagging the dispenser gave no ticket");
        expect(lootCarried() > loot, "the paid lap gave no loot");
        note("fee paid, lap " + (laps + 1) + " (" + course.obstacles() + " obstacles), ticket and loot");
    }

    /** How many of the course's loot items the player carries. */
    private int lootCarried() {
        int n = 0;
        for (com.runecraft.osrs.content.skilling.Loot l : AgilityRewards.WILDERNESS_LOOT) {
            n += Inv.count(player, l.item());
        }
        return n;
    }

    // ------------------------------------------------------------------ obelisk and Ferox

    private void obeliskAndFerox() {
        expect(world != null, "no Gielinor");
        WildernessTransport.Obelisk o = WildernessTransport.obelisk(13);
        expect(o != null, "no level 13 obelisk");
        BlockPos centre = com.runecraft.content.npcs.WildernessTransportService.selfTestPrepare(world, o);
        player.teleportTo(world, centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5, 0, 0);
        BlockPos pillar = com.runecraft.content.npcs.WildernessTransportService.selfTestPillar(world, o);
        expect(pillar != null, "no obelisk pillar at the level 13 obelisk");
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pillar),
                net.minecraft.core.Direction.UP, pillar, false);
        var result = com.runecraft.platform.UseBlockCallback.EVENT.invoker().interact(player, world, InteractionHand.MAIN_HAND, hit);
        expect(result.consumesAction(), "activating the obelisk pillar wasn't handled");
        long armed = com.runecraft.content.npcs.WildernessTransportService.selfTestArmedIn(o, player.getServer());
        expect(armed == (long) WildernessTransport.OBELISK_DELAY_TICKS * ActionManager.MC_TICKS_PER_GAME_TICK,
                "the obelisk isn't counting down (" + armed + " ticks)");
        BlockPos before = player.blockPosition();
        int fired = com.runecraft.content.npcs.WildernessTransportService.selfTestFireArmed(player.getServer());
        expect(fired == 1, "the armed obelisk didn't fire");
        WildernessTransport.Obelisk landed = WildernessTransport.platformAt(GielinorMap.toOsrsX(player.getBlockX()),
                GielinorMap.toOsrsY(player.getBlockZ()));
        expect(landed != null, "the obelisk didn't put the player on an obelisk's platform (" + player.blockPosition().toShortString()
                + ", from " + before.toShortString() + ")");
        note("obelisk 13 -> " + landed.level());

        // The Ferox Enclave: a safe zone (no player fights, no looting bag), a barrier against tele-blocked players and the pool.
        int fx = GielinorMap.toBlockX(3139);
        int fz = GielinorMap.toBlockZ(3632);
        StructurePlacer.applyAround(world, fx, fz, 40);
        BlockPos outside = Gielinor.safeSpot(world, GielinorMap.toBlockX(3139), GielinorMap.toBlockZ(3612));
        player.teleportTo(world, outside.getX() + 0.5, outside.getY(), outside.getZ() + 0.5, 0, 0);
        expect(!Gielinor.inSafeZone(player) && Gielinor.wildernessLevel(player) > 0, "south of the enclave isn't Wilderness");
        FeroxService.barrier(player);
        com.runecraft.combat.Teleblock.apply(player, player);
        // the paved path inside the south gate (the garden's fountain stands in the middle)
        BlockPos inside = Gielinor.safeSpot(world, GielinorMap.toBlockX(3139), GielinorMap.toBlockZ(3621));
        player.teleportTo(world, inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5, 0, 0);
        FeroxService.barrier(player);
        expect(!Gielinor.inSafeZone(player), "a tele-blocked player got through the enclave's barrier");
        com.runecraft.combat.Teleblock.forget(player.getUUID());
        player.teleportTo(world, inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5, 0, 0);
        FeroxService.barrier(player);
        expect(Gielinor.inSafeZone(player), "the enclave isn't a safe zone");
        expect(!CombatHandler.canAttackPlayer(player, player), "player fights are allowed in the enclave");
        Inv.give(player, "looting_bag", 1);
        Inv.give(player, "lobster", 1);
        int slot = -1;
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            if ("lobster".equals(Inv.idOf(player.getInventory().items.get(i)))) {
                slot = i;
            }
        }
        expect(slot >= 0, "the lobster isn't in the inventory");
        expect(com.runecraft.content.systems.LootingBagService.store(player, slot, false) == 0, "the looting bag took items in the enclave");
        note("Ferox: safe zone, barrier held a tele-blocked player, no looting bag");

        // The pool: hitpoints, prayer, run energy, stats and poison restored, prayers off, special attack left alone.
        BlockPos pool = find(fx, fz, 40, inside.getY() - 6, inside.getY() + 6, FeroxEnclave.BLOCK_POOL);
        expect(pool != null, "no pool of refreshment in the enclave");
        PlayerData data = Players.data(player);
        SysState.SysPlayer sp = SysState.of(player);
        player.setHealth(player.getMaxHealth() / 2);
        data.setPrayerPoints(1);
        data.skills().setBoost(Skill.ATTACK, -5);
        data.skills().setBoost(Skill.STRENGTH, 6);
        data.activePrayers().add(com.runecraft.osrs.Prayer.PROTECT_ITEM);
        sp.runEnergy = 100;
        sp.specEnergy = 40;
        com.runecraft.combat.PoisonService.poison(player, 6, ActionManager.gameTick());
        use(pool);
        expect(player.getHealth() >= player.getMaxHealth() - 0.01f, "the pool didn't restore hitpoints");
        expect(data.prayerPoints() == data.skills().level(Skill.PRAYER), "the pool didn't restore prayer points");
        expect(data.skills().boost(Skill.ATTACK) == 0 && data.skills().boost(Skill.STRENGTH) == 0, "the pool didn't reset boosted and lowered stats");
        expect(sp.runEnergy >= 10000, "the pool didn't restore run energy");
        expect(data.activePrayers().isEmpty(), "the pool didn't turn prayers off");
        expect(sp.specEnergy == 40, "the enclave's pool restored special attack energy (" + sp.specEnergy + ")");
        expect(!com.runecraft.combat.PoisonService.isPoisoned(player), "the pool didn't cure poison");
        note("pool of refreshment (special attack untouched)");
    }
}
