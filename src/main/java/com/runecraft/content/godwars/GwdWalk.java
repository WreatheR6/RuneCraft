package com.runecraft.content.godwars;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.Mechanics;
import com.runecraft.combat.CombatHandler;
import com.runecraft.content.BlockHooks;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.QuestInfo;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.godwars.GodWarsFactions;
import com.runecraft.osrs.content.godwars.GodWarsFactions.God;
import com.runecraft.osrs.content.godwars.GwdBosses;
import com.runecraft.osrs.content.godwars.GwdDungeon;
import com.runecraft.osrs.content.godwars.GwdDungeon.Area;
import com.runecraft.osrs.content.godwars.GwdDungeon.Passage;
import com.runecraft.osrs.content.godwars.GwdDungeon.Spot;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.util.Inv;
import com.runecraft.world.Gielinor;
import com.runecraft.world.StructurePlacer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Self-test driver ({@code boss:gwd_walk}): walks the God Wars Dungeon the way a player does, through the real blocks and
 * hit paths: the hole north of Trollheim (rope tied), Bandos' big door, kill count from real follower kills, the
 * altar, each general's door and chamber (its general killed with its bodyguards around, kill count, loot, essence, the
 * respawn timer), Saradomin's two rocks, the icy river, the grapple to the Eyrie, the frozen door, the Ancient Prison's
 * doors and essence, the ancient barrier and Nex through her five phases, and the rope back up (essence cleared). Every
 * step that doesn't work throws an {@link AssertionError} saying which. The player's God Wars state is put back after.
 */
public final class GwdWalk {
    private final ServerPlayer player;
    private final ServerLevel world;
    private final StringBuilder log = new StringBuilder();

    private GwdWalk(ServerPlayer player) {
        this.player = player;
        this.world = Gielinor.world(player.getServer());
    }

    private static final net.minecraft.resources.ResourceLocation HP_BUFFER = com.runecraft.RuneCraft.id("selftest_gwd_hp");
    /** Items the walk hands out (and takes back after). */
    private static final String[] GEAR = {"rope", "hammer", "rune_crossbow", "mith_grapple"};
    /** Quests the walk sets (and puts back after). */
    private static final String[] QUESTS = {"troll_stronghold", "the_frozen_door"};
    /** The obstacles' levels: 70 Strength, Ranged, Agility and Hitpoints; Prayer for the altars. */
    private static final Skill[] LEVELS = {Skill.STRENGTH, Skill.RANGED, Skill.AGILITY, Skill.HITPOINTS, Skill.PRAYER};

    /** Runs the whole walk and puts the player back at {@code back}; returns a short summary or throws {@link AssertionError}. */
    public static String run(ServerPlayer player, BlockPos back) {
        GwdKillcountState state = GwdKillcountState.get(player.getServer());
        int[] saved = state.snapshot(player.getUUID());
        PlayerData data = Players.data(player);
        int[] xp = data.skills().rawXp();
        int[] boosts = data.skills().rawBoosts();
        int[] gear = new int[GEAR.length];
        for (int i = 0; i < GEAR.length; i++) {
            gear[i] = Inv.count(player, GEAR[i]);
        }
        int[] stages = new int[QUESTS.length];
        for (int i = 0; i < QUESTS.length; i++) {
            QuestInfo q = QuestApi.quest(QUESTS[i]);
            stages[i] = q == null ? 0 : data.questStage(q);
        }
        state.restore(player.getUUID(), null);
        GwdDungeonHooks.resetAltar(player);
        try {
            for (Skill s : LEVELS) {
                data.skills().setXpTenths(s, Math.max(data.skills().xpTenths(s), com.runecraft.osrs.Experience.xpForLevel(80) * 10));
            }
            // a buffer of health so the generals' hits between two heals can't kill the test player
            AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
            if (health != null) {
                health.removeModifier(HP_BUFFER);
                health.addTransientModifier(new AttributeModifier(HP_BUFFER, 2000, AttributeModifier.Operation.ADD_VALUE));
            }
            GwdWalk w = new GwdWalk(player);
            try {
                return w.walk();
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
            GwdDungeonHooks.resetAltar(player);
            data.skills().load(xp, boosts);
            for (int i = 0; i < QUESTS.length; i++) {
                QuestInfo q = QuestApi.quest(QUESTS[i]);
                if (q != null) {
                    data.setQuestStage(q, stages[i]);
                }
            }
            for (int i = 0; i < GEAR.length; i++) {
                int extra = Inv.count(player, GEAR[i]) - gear[i];
                if (extra > 0) {
                    Inv.remove(player, GEAR[i], extra);
                }
            }
            com.runecraft.network.ServerNetworking.sync(player);
            if (back != null && player.isAlive()) {
                player.teleportTo(back.getX() + 0.5, back.getY(), back.getZ() + 0.5);
            }
        }
    }

    private static AssertionError fail(String message) {
        return new AssertionError(message);
    }

    private static void expect(boolean ok, String message) {
        if (!ok) {
            throw fail(message);
        }
    }

    private void note(String s) {
        log.append(log.length() == 0 ? "" : "; ").append(s);
    }

    private Area here() {
        return GwdDungeon.areaAt(player.getBlockX(), player.getBlockY(), player.getBlockZ());
    }

    private void heal() {
        player.setHealth(player.getMaxHealth());
        PlayerData data = Players.data(player);
        if (data.prayerPoints() < 30) {
            data.setPrayerPoints(99);
        }
    }

    private void questStage(String quest, boolean complete, int stage) {
        QuestInfo q = QuestApi.quest(quest);
        expect(q != null, "no quest " + quest);
        Players.data(player).setQuestStage(q, complete ? q.finalStage() : stage);
    }

    // ------------------------------------------------------------------ blocks

    /** Right-clicks the scripted block at {@code pos} through BlockHooks, checking it is the expected block. */
    private void use(BlockPos pos, String expectedBlock) {
        BlockState state = world.getBlockState(pos);
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        expect(id.equals(expectedBlock), "expected " + expectedBlock + " at " + pos.toShortString() + " but found " + id);
        BlockHooks.use(player, id, pos, state);
    }

    /**
     * Sneak-uses an altar with a weapon in hand through the real right-click event, as a player teleports out of a chamber
     * (vanilla never reaches the block's own use for a sneaking player holding something).
     */
    private void sneakUseAltar(GwdDungeon.Altar altar) {
        Spot s = altar.spot();
        StructurePlacer.applyAround(world, s.blockX(), s.blockZ(), 12);
        BlockPos pos = new BlockPos(s.blockX(), s.feetY(), s.blockZ());
        String id = BuiltInRegistries.BLOCK.getKey(world.getBlockState(pos).getBlock()).getPath();
        expect(id.equals(altar.block()), "expected " + altar.block() + " at " + pos.toShortString() + " but found " + id);
        net.minecraft.world.item.ItemStack held = player.getMainHandItem();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, Inv.stack("rune_crossbow", 1));
        player.setShiftKeyDown(true);
        try {
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),
                    net.minecraft.core.Direction.UP, pos, false);
            var result = com.runecraft.platform.UseBlockCallback.EVENT.invoker().interact(player, world,
                    net.minecraft.world.InteractionHand.MAIN_HAND, hit);
            expect(result.consumesAction(), altar.name() + ": sneak-use with a weapon in hand wasn't handled (" + result + ")");
        } finally {
            player.setShiftKeyDown(false);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, held);
        }
    }

    /** The first block of a passage's block on a tile (carved first). */
    private BlockPos blockOf(Passage p, Spot s) {
        StructurePlacer.applyAround(world, s.blockX(), s.blockZ(), 12);
        return new BlockPos(s.blockX(), GwdDungeon.floorY(s.plane()) + 1, s.blockZ());
    }

    /** Uses a passage from the side the player stands on and checks the player ends up in {@code expected}. */
    private void through(String id, Area expected) {
        Passage p = GwdDungeon.passage(id);
        expect(p != null, "no passage " + id);
        boolean inner = GwdDungeonHooks.innerSide(player, p);
        Spot from = inner ? p.b() : p.a();
        if (player.distanceToSqr(from.blockX() + 1, from.feetY(), from.blockZ() + 1) > 8 * 8) {
            stand(from); // walk up to the obstacle first
        }
        Spot block = nearest(p, from);
        use(blockOf(p, block), p.block());
        Area now = here();
        expect(now == expected, id + ": expected to be in " + expected + " but stand in " + now + " at " + player.blockPosition().toShortString());
    }

    private static Spot nearest(Passage p, Spot from) {
        Spot best = p.blocks().get(0);
        double bd = Double.MAX_VALUE;
        for (Spot s : p.blocks()) {
            if (s.plane() != from.plane()) {
                continue;
            }
            double d = Math.hypot(s.x() - from.x(), s.y() - from.y());
            if (d < bd) {
                bd = d;
                best = s;
            }
        }
        return best;
    }

    /** Uses a passage expecting a refusal (the player stays where they are). */
    private void refused(String id, String why) {
        Passage p = GwdDungeon.passage(id);
        Area before = here();
        boolean inner = GwdDungeonHooks.innerSide(player, p);
        use(blockOf(p, nearest(p, inner ? p.b() : p.a())), p.block());
        expect(here() == before, id + " let the player through " + why);
    }

    /** Stands the player on a tile of the dungeon (carving around it). */
    private void stand(Spot s) {
        GwdDungeonHooks.send(player, s);
    }

    // ------------------------------------------------------------------ fighting

    /** A player hit through the creature's own damage rules (immunities, invulnerable phases), without the accuracy roll. */
    private int hit(LivingEntity e, int damage) {
        for (AttackType type : new AttackType[]{AttackType.RANGED, AttackType.MAGIC, AttackType.SLASH}) {
            int d = e instanceof OsrsMonsterEntity m ? m.modifyIncoming(player, damage, type) : damage;
            if (d > 0) {
                return CombatHandler.applyDamage(player, e, d);
            }
        }
        return 0;
    }

    /** Kills a creature with heavy hits; false if it couldn't be hurt. */
    private boolean kill(LivingEntity e) {
        for (int i = 0; i < 400 && e.isAlive(); i++) {
            int big = Math.max(50, Math.round(e.getMaxHealth()) / 4);
            if (e instanceof DataBossEntity d) {
                big = Math.max(50, d.trueHitpoints() / 6);
            }
            if (hit(e, big) <= 0 && e.isAlive()) {
                return false;
            }
        }
        return !e.isAlive();
    }

    /** One game tick of a boss fight: the boss (and the world's boss mechanics) tick, the player is healed. */
    private void tick(LivingEntity boss) {
        for (int m = 0; m < ActionManager.MC_TICKS_PER_GAME_TICK; m++) {
            ActionManager.advanceForSelfTest();
            if (boss.isAlive()) {
                if (boss instanceof Mob mob && mob.getTarget() != player) {
                    mob.setTarget(player);
                }
                boss.tick();
            }
            Mechanics.tickForSelfTest();
            heal();
        }
    }

    private int groundItemsNear(LivingEntity e) {
        return world.getEntitiesOfClass(ItemEntity.class, new AABB(e.blockPosition()).inflate(10), i -> true).size();
    }

    // ------------------------------------------------------------------ the walk

    private String walk() {
        expect(world != null, "no Gielinor");
        Inv.give(player, "rope", 3);
        Inv.give(player, "hammer", 1);
        Inv.give(player, "rune_crossbow", 1);
        Inv.give(player, "mith_grapple", 1);
        enter();
        bandos();
        saradomin();
        zamorak();
        armadyl();
        nex();
        leave();
        return log.toString();
    }

    /** Trollheim: the hole needs Dad beaten, 60 Strength or Agility and a rope the first time. */
    private void enter() {
        BlockPos hole = GwdDungeonHooks.holePos(world);
        expect(hole != null, "no hole at the God Wars Dungeon landmark");
        questStage("troll_stronghold", false, 1);
        player.teleportTo(world, hole.getX() + 0.5, hole.getY() + 1, hole.getZ() + 0.5, 0, 0);
        use(hole, GwdDungeon.BLOCK_HOLE);
        expect(here() == null, "climbed down without having beaten Dad");
        questStage("troll_stronghold", false, 2);
        int ropes = Inv.count(player, "rope");
        use(hole, GwdDungeon.BLOCK_HOLE);
        expect(here() == Area.MAIN, "the hole didn't lead into the dungeon (" + player.blockPosition().toShortString() + ")");
        expect(Inv.count(player, "rope") == ropes - 1, "the rope wasn't tied to the hole");
        note("hole -> " + GwdDungeon.LANDING.x() + "," + GwdDungeon.LANDING.y());
    }

    /** Kills followers of a god near the player through the real kill path; returns the essence gained. */
    private int killFollowers(God god, int max, String calmItem) {
        StructurePlacer.applyAround(world, player.getBlockX(), player.getBlockZ(), 40);
        GwdSpawner.stock(world, List.of(player));
        expect(checkCalm(god, calmItem), "no follower near the player to check god items against");
        int before = GwdKillcount.get(player, god);
        int killed = 0;
        for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(48, 12, 48),
                e -> e.isAlive() && e instanceof OsrsMonsterEntity m && m.stats() != null && m.stats().profile() != null
                        && GodWarsFactions.followerGod(m.stats().profile().id) == god)) {
            if (killed >= max) {
                break;
            }
            expect(kill(e), "couldn't kill the follower " + e.getName().getString());
            killed++;
        }
        int gained = GwdKillcount.get(player, god) - before;
        expect(killed > 0, "no " + god.displayName() + " followers spawned near " + here());
        expect(gained == killed, killed + " " + god.displayName() + " followers killed but the kill count rose by " + gained);
        return gained;
    }

    /** A god item calms that god's followers but not the others; false when no follower stands near. */
    private boolean checkCalm(God god, String item) {
        for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(48, 12, 48),
                e -> e.isAlive() && e instanceof OsrsMonsterEntity)) {
            OsrsMonsterEntity m = (OsrsMonsterEntity) e;
            if (m.stats() == null || m.stats().profile() == null || !(e instanceof Mob mob)) {
                continue;
            }
            God g = GodWarsFactions.followerGod(m.stats().profile().id);
            if (g == null) {
                continue;
            }
            boolean before = GwdDungeonHooks.calmed(mob, player);
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, Inv.stack(item, 1));
            boolean after = GwdDungeonHooks.calmed(mob, player);
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, net.minecraft.world.item.ItemStack.EMPTY);
            expect(!before, "a follower was calm without a god item");
            expect(after == (g == god), m.stats().profile().id + " calm=" + after + " with " + item);
            return true;
        }
        return false;
    }

    /** Enters a general's chamber (kill count spent), kills the general with its bodyguards around, checks the rest. */
    private void general(God god, String door, Area chamber) {
        refusedWithoutKc(god, door);
        GwdKillcount.set(player, god, GwdKillcount.required.applyAsInt(player));
        through(door, chamber);
        expect(GwdKillcount.get(player, god) == 0, "the door didn't take the kill count");
        GwdDungeon.Lair general = null;
        List<LivingEntity> guards = new ArrayList<>();
        for (GwdDungeon.Lair l : GwdDungeon.lairs()) {
            if (l.area() != chamber) {
                continue;
            }
            StructurePlacer.applyAround(world, l.spot().blockX(), l.spot().blockZ(), 24);
        }
        GwdSpawner.stock(world, List.of(player));
        LivingEntity boss = null;
        for (GwdDungeon.Lair l : GwdDungeon.lairs()) {
            if (l.area() != chamber) {
                continue;
            }
            LivingEntity e = GwdSpawner.find(world, l.creature());
            expect(e != null, l.creature() + " is not in " + chamber);
            expect(GwdDungeon.areaAt(e.getBlockX(), e.getBlockY(), e.getBlockZ()) == chamber, l.creature() + " stands outside its chamber");
            if (l.general()) {
                general = l;
                boss = e;
            } else {
                guards.add(e);
            }
        }
        expect(boss != null && guards.size() == 3, chamber + ": general " + boss + ", " + guards.size() + " bodyguards");
        // The bodyguards give kill count; one of them is killed and must wait for its respawn timer.
        LivingEntity guard = guards.get(0);
        String guardId = GwdKillcount.creatureId(guard, ((OsrsMonsterEntity) guard).stats());
        expect(kill(guard), "couldn't kill " + guardId);
        expect(GwdKillcount.get(player, god) == 1, "the bodyguard gave no kill count");
        expect(GwdSpawner.respawnLeft(guardId) > 0, guardId + " has no respawn timer");
        GwdSpawner.stock(world, List.of(player));
        expect(GwdSpawner.find(world, guardId) == null, guardId + " came back at once");
        GwdDungeon.Altar altar = null;
        for (GwdDungeon.Altar a : GwdDungeon.altars()) {
            if (a.god() == god) {
                altar = a;
            }
        }
        expect(altar != null, "no altar for " + god);
        // Its room-wide attacks reach the player in the chamber but not by its door, outside the walls.
        if (boss instanceof DataBossEntity d && d.script() instanceof GwdScript script) {
            expect(script.roomPlayers(d, 64).contains(player), "the player in " + chamber + " isn't reached by its room-wide attacks");
            net.minecraft.world.phys.Vec3 inside = player.position();
            stand(altar.outside());
            expect(here() != chamber && player.distanceToSqr(d) < 64 * 64, "not outside " + chamber + "'s door");
            expect(!script.roomPlayers(d, 64).contains(player), general.creature() + "'s room-wide attacks reach past its chamber's walls");
            player.teleportTo(inside.x, inside.y, inside.z);
            expect(here() == chamber, "couldn't step back into " + chamber);
        }
        // The general fights: let it attack a few ticks, then kill it.
        PlayerData data = Players.data(player);
        int kcBefore = data.killCount(general.creature());
        for (int t = 0; t < 12; t++) {
            tick(boss);
        }
        expect(kill(boss), "couldn't kill " + general.creature());
        expect(data.killCount(general.creature()) == kcBefore + 1, general.creature() + " kill count didn't rise");
        expect(GwdKillcount.get(player, god) == 2, "the general gave no kill count");
        expect(groundItemsNear(boss) > 0 || Inv.count(player, "bones") + Inv.count(player, "big_bones") > 0, general.creature() + " dropped nothing");
        expect(GwdSpawner.respawnLeft(general.creature()) > GwdDungeon.GENERAL_RESPAWN_TICKS - 20, "no respawn timer for " + general.creature());
        GwdSpawner.stock(world, List.of(player));
        expect(GwdSpawner.find(world, general.creature()) == null, general.creature() + " respawned at once");
        GwdSpawner.resetTimer(general.creature());
        GwdSpawner.resetTimer(guardId);
        // The altar: refused in combat (a bodyguard swings at the player), Prayer restored once the combat timer has run out
        // (above the maximum with the god's items), then its teleport out of the chamber, which works in combat.
        LivingEntity swinger = null;
        for (LivingEntity e : guards) {
            if (e.isAlive()) {
                swinger = e;
            }
        }
        expect(swinger != null, "no bodyguard left alive in " + chamber);
        data.setPrayerPoints(0);
        GwdDungeonHooks.resetAltar(player);
        CombatHandler.applyDamage(swinger, player, 0);
        expect(GwdDungeonHooks.inCombat(player), "a bodyguard's attack didn't put the player in combat");
        GwdDungeonHooks.useAltar(player, altar);
        expect(data.prayerPoints() == 0, "the altar restored Prayer in combat");
        for (int t = 0; t < com.runecraft.osrs.SingleCombat.COMBAT_TICKS * ActionManager.MC_TICKS_PER_GAME_TICK; t++) {
            ActionManager.advanceForSelfTest();
        }
        expect(!GwdDungeonHooks.inCombat(player), "still in combat after the combat timer");
        GwdDungeonHooks.useAltar(player, altar);
        expect(data.prayerPoints() >= data.skills().level(Skill.PRAYER), "the altar didn't restore Prayer");
        CombatHandler.applyDamage(swinger, player, 0);
        sneakUseAltar(altar);
        expect(here() != chamber, "the altar's teleport didn't leave " + chamber);
        // Clear the chamber for the next visitor.
        for (LivingEntity e : guards) {
            if (e.isAlive()) {
                e.discard();
            }
        }
        note(general.creature() + " killed");
    }

    private void refusedWithoutKc(God god, String door) {
        Passage p = GwdDungeon.passage(door);
        stand(p.a());
        GwdKillcount.set(player, god, 0);
        int keys = Inv.count(player, "ecumenical_key");
        if (keys == 0) {
            refused(door, "without kill count");
        }
    }

    private void bandos() {
        stand(GwdDungeon.passage("bandos_big_door").a());
        int hammers = Inv.count(player, "hammer");
        Inv.remove(player, "hammer", hammers);
        if (!GwdDungeonHooks.hasHammer(player)) { // a warhammer of the player's own would bang the gong too
            refused("bandos_big_door", "without a hammer");
        }
        Inv.give(player, "hammer", hammers);
        through("bandos_big_door", Area.BANDOS_STRONGHOLD);
        int gained = killFollowers(God.BANDOS, 6, "bandos_boots");
        note("bandos kc +" + gained);
        general(God.BANDOS, "graardor_door", Area.GRAARDOR_CHAMBER);
        stand(GwdDungeon.passage("bandos_big_door").b());
        through("bandos_big_door", Area.MAIN);
    }

    private void saradomin() {
        stand(GwdDungeon.passage("saradomin_upper_rock").a());
        int ropesBefore = Inv.count(player, "rope");
        through("saradomin_upper_rock", Area.SARADOMIN_ENCAMPMENT);
        through("saradomin_lower_rock", Area.SARADOMIN_ENCAMPMENT);
        expect(GwdDungeon.planeAt(player.getBlockY()) == 0, "the lower rock didn't lead down to the Encampment's floor");
        int ropes = Inv.count(player, "rope");
        expect(ropes == ropesBefore - 2, "the two rocks didn't take a rope each (" + ropes + " of " + ropesBefore + " left)");
        general(God.SARADOMIN, "zilyana_door", Area.ZILYANA_CHAMBER);
        // back up both ropes (tied for good now)
        stand(GwdDungeon.passage("saradomin_lower_rock").b());
        through("saradomin_lower_rock", Area.SARADOMIN_ENCAMPMENT);
        through("saradomin_upper_rock", Area.MAIN);
    }

    private void zamorak() {
        stand(GwdDungeon.passage("zamorak_ice_bridge").a());
        Players.data(player).setPrayerPoints(50);
        through("zamorak_ice_bridge", Area.ZAMORAK_FORTRESS);
        expect(Players.data(player).prayerPoints() == 0, "the icy river didn't drain Prayer");
        general(God.ZAMORAK, "kril_door", Area.KRIL_CHAMBER);
        stand(GwdDungeon.passage("zamorak_ice_bridge").b());
        through("zamorak_ice_bridge", Area.MAIN);
    }

    private void armadyl() {
        stand(GwdDungeon.passage("armadyl_pillar").a());
        int grapples = Inv.count(player, "mith_grapple");
        Inv.remove(player, "mith_grapple", grapples);
        if (!GwdDungeonHooks.hasGrapple(player)) { // one worn as ammunition still counts
            refused("armadyl_pillar", "without a mith grapple");
        }
        Inv.give(player, "mith_grapple", grapples);
        through("armadyl_pillar", Area.ARMADYL_EYRIE);
        int gained = killFollowers(God.ARMADYL, 4, "armadyl_dhide_boots");
        note("armadyl kc +" + gained);
        general(God.ARMADYL, "kreearra_door", Area.KREEARRA_CHAMBER);
        stand(GwdDungeon.passage("armadyl_pillar").b());
        through("armadyl_pillar", Area.MAIN);
    }

    private void nex() {
        stand(GwdDungeon.passage("frozen_door").a());
        questStage("the_frozen_door", false, 0);
        Inv.remove(player, "frozen_key", Inv.count(player, "frozen_key"));
        refused("frozen_door", "before The Frozen Door");
        questStage("the_frozen_door", true, 0);
        through("frozen_door", Area.PRISON_ENTRANCE);
        through("prison_door", Area.ANCIENT_PRISON);
        // Zarosian essence from a real kill, then enough to pass the refuge door.
        StructurePlacer.applyAround(world, player.getBlockX(), player.getBlockZ(), 40);
        GwdSpawner.stock(world, List.of(player));
        int before = GwdKillcount.zaros(player);
        LivingEntity zarosian = null;
        for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(48, 12, 48),
                e -> e.isAlive() && e instanceof OsrsMonsterEntity m && m.stats() != null && m.stats().profile() != null
                        && GodWarsFactions.zarosEssence(m.stats().profile().id) > 0)) {
            zarosian = e;
            break;
        }
        expect(zarosian != null, "no Zarosian follower in the Ancient Prison");
        String zid = ((OsrsMonsterEntity) zarosian).stats().profile().id;
        expect(kill(zarosian), "couldn't kill " + zid);
        expect(GwdKillcount.zaros(player) - before >= GodWarsFactions.zarosEssence(zid), "the Zarosian kill gave no essence");
        stand(GwdDungeon.passage("refuge_door").a());
        GwdKillcount.setZaros(player, 0);
        refused("refuge_door", "without Zarosian essence");
        GwdKillcount.setZaros(player, GodWarsFactions.REQUIRED_ZAROS_ESSENCE);
        through("refuge_door", Area.PRISON_REFUGE);
        through("ancient_barrier", Area.NEX_CHAMBER);
        DataBossEntity nex = GwdDungeonHooks.nex(world);
        expect(nex != null, "Nex didn't appear when the barrier was passed");
        refused("ancient_barrier", "out of Nex's chamber (the altar is the way out)");
        // Her specials and spells pick their victims in her chamber, never in the refuge by the bank next door.
        GwdScript nexScript = (GwdScript) nex.script();
        expect(nexScript.roomPlayers(nex, 64).contains(player), "the player in Nex's chamber isn't among her targets");
        net.minecraft.world.phys.Vec3 inside = player.position();
        stand(Area.PRISON_REFUGE.seeds().get(0));
        expect(here() == Area.PRISON_REFUGE && player.distanceToSqr(nex) < 64 * 64, "not in the refuge by Nex's chamber");
        expect(!nexScript.roomPlayers(nex, 64).contains(player), "Nex's attacks reach into the refuge");
        player.teleportTo(inside.x, inside.y, inside.z);
        expect(here() == Area.NEX_CHAMBER, "couldn't step back into Nex's chamber");
        // The fight: Nex shields herself behind each mage in turn; the mage is killed, then she goes on.
        PlayerData data = Players.data(player);
        int kcBefore = data.killCount("nex");
        int phases = 0;
        for (int round = 0; round < 400 && nex.isAlive(); round++) {
            tick(nex);
            if (!nex.isAlive()) {
                break;
            }
            if (nex.script().isVulnerable(nex)) {
                hit(nex, Math.max(50, nex.trueHitpoints() / 12));
                continue;
            }
            for (DataBossEntity mage : world.getEntitiesOfClass(DataBossEntity.class, nex.getBoundingBox().inflate(48, 12, 48),
                    e -> e.isAlive() && e.def() != null && java.util.Arrays.asList(GwdBosses.NEX_MINIONS).contains(e.def().id))) {
                if (mage.script().isVulnerable(mage)) {
                    expect(kill(mage), "couldn't kill " + mage.def().id);
                    phases++;
                }
            }
            for (LivingEntity r : world.getEntitiesOfClass(LivingEntity.class, nex.getBoundingBox().inflate(48, 12, 48),
                    e -> e.isAlive() && e instanceof OsrsMonsterEntity m && m.stats() != null && m.stats().profile() != null
                            && "blood_reaver".equals(m.stats().profile().id))) {
                kill(r);
            }
        }
        expect(!nex.isAlive(), "Nex not killed (" + Math.round(nex.getHealth() * 100 / nex.getMaxHealth()) + "% left, " + phases + " mages)");
        expect(phases == 4, "Nex died after " + phases + " of her four mages");
        expect(data.killCount("nex") == kcBefore + 1, "Nex's kill count didn't rise");
        expect(GwdKillcount.zaros(player) >= 5, "Nex gave no Zarosian essence");
        note("nex killed through " + phases + " mages");
        // Her altar teleports out to the refuge's door.
        sneakUseAltar(GwdDungeon.altar("gwd_ancient_altar"));
        expect(here() == Area.ANCIENT_PRISON, "Nex's altar didn't teleport out of the chamber (" + here() + ")");
        through("prison_door", Area.PRISON_ENTRANCE);
        through("frozen_door", Area.MAIN);
    }

    /** The rope back up clears the essence. */
    private void leave() {
        GwdKillcount.set(player, God.BANDOS, 5);
        stand(GwdDungeon.LANDING);
        Spot rope = GwdDungeon.ROPE;
        use(new BlockPos(rope.blockX(), rope.feetY() + 1, rope.blockZ()), GwdDungeon.BLOCK_ROPE);
        expect(here() == null && !GwdDungeon.contains(player.getBlockX(), player.getBlockY(), player.getBlockZ()), "the rope didn't lead out");
        expect(GwdKillcount.get(player, God.BANDOS) == 0, "leaving the dungeon didn't clear the essence");
        note("rope up");
    }
}
