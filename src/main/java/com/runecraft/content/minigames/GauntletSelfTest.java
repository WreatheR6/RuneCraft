package com.runecraft.content.minigames;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.Mechanics;
import com.runecraft.combat.CombatHandler;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.bosses.BossSelfTests;
import com.runecraft.content.minigames.games.GauntletSession;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Gauntlet;
import com.runecraft.osrs.content.minigames.Gauntlet.Foe;
import com.runecraft.osrs.content.minigames.Gauntlet.Mode;
import com.runecraft.osrs.content.minigames.Gauntlet.Node;
import com.runecraft.osrs.content.minigames.Gauntlet.Piece;
import com.runecraft.osrs.content.minigames.Gauntlet.Tier;
import com.runecraft.osrs.content.minigames.GauntletLobby;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.util.Inv;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Self-test driver for the Gauntlet ({@code minigame:gauntlet_walk} and {@code minigame:gauntlet_failures}, real-player
 * mode): walks a run the way a player does, through the real blocks, session, hit path and Hunllef script, letting game
 * ticks pass by hand. Every step that doesn't work throws an {@link AssertionError} saying which.
 */
public final class GauntletSelfTest {
    private static final int MC = ActionManager.MC_TICKS_PER_GAME_TICK;
    private final ServerPlayer p;
    private final ServerLevel world;
    private final StringBuilder log = new StringBuilder();
    private GauntletSession g;

    private GauntletSelfTest(ServerPlayer p) {
        this.p = p;
        this.world = com.runecraft.world.Gielinor.world(p.getServer());
    }

    static AssertionError fail(String message) {
        return new AssertionError(message);
    }

    static void expect(boolean ok, String message) {
        if (!ok) {
            throw fail(message);
        }
    }

    private void note(String s) {
        log.append(log.length() == 0 ? "" : "; ").append(s);
    }

    private String blockAt(BlockPos pos) {
        return BuiltInRegistries.BLOCK.getKey(world.getBlockState(pos).getBlock()).getPath();
    }

    /** Right-clicks a block as the player (the module's block hooks, then the session). */
    private void use(BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        BlockHooks.use(p, id, pos, state);
    }

    private static BlockPos lobbyTile(int[] tile) {
        return new BlockPos(GauntletLobby.blockX(tile[0]), GauntletLobby.GROUND, GauntletLobby.blockZ(tile[1]));
    }

    private void moveTo(BlockPos stand) {
        p.teleportTo(world, stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, p.getYRot(), 0);
    }

    private void heal() {
        if (p.getHealth() < p.getMaxHealth()) {
            p.setHealth(p.getMaxHealth());
        }
        PlayerData d = Players.data(p);
        if (d.prayerPoints() < 50) {
            d.setPrayerPoints(99);
        }
    }

    /** One game tick of the run: the session, and the Hunllef as the world would tick it. */
    private void tick() {
        Mob boss = g.hunllef();
        for (int m = 0; m < MC; m++) {
            ActionManager.advanceForSelfTest();
            if (boss != null && boss.isAlive()) {
                if (boss.getTarget() != p) {
                    boss.setTarget(p);
                }
                boss.tick();
            }
            Mechanics.tickForSelfTest();
            heal();
        }
        if (!g.isEnded()) {
            MinigameManager.selfTestTick(g);
        }
    }

    /** A player hit through the real path (the monster's own damage rules), without the accuracy roll. */
    private int hit(LivingEntity target, int damage, AttackType type) {
        int d = damage;
        if (target instanceof OsrsMonsterEntity m) {
            d = m.modifyIncoming(p, d, type);
        }
        int dealt = CombatHandler.applyDamage(p, target, d);
        CombatHandler.hookAfterHit(p, target, type, dealt);
        return dealt;
    }

    private void kill(LivingEntity mob) {
        for (int i = 0; i < 60 && mob.isAlive(); i++) {
            hit(mob, 40, AttackType.SLASH);
        }
        expect(!mob.isAlive(), MinigameManager.creatureId(mob) + " did not die");
    }

    private int count(String item) {
        return Inv.count(p, item);
    }

    private static void songOfTheElves(ServerPlayer p) {
        var q = QuestApi.quest("song_of_the_elves");
        expect(q != null, "no Song of the Elves quest");
        Players.data(p).setQuestStage(q, q.finalStage());
    }

    // ------------------------------------------------------------------ the lobby and entering

    /** Through the Gauntlet Portal into the lobby; checks its objects; back through the platform and in again. */
    private void lobby() {
        expect(GauntletService.enterLobby(p), "the Gauntlet Portal refused the player");
        expect(GauntletService.inLobby(p.blockPosition()), "the portal did not put the player in the lobby (at " + p.blockPosition() + ")");
        Map<String, int[]> objects = Map.of(Gauntlet.REWARD_CHEST, GauntletLobby.REWARD_CHEST, Minigame.GAUNTLET.entranceBlock(),
                GauntletLobby.ENTRANCE, Gauntlet.CORRUPTED_ENTRANCE, GauntletLobby.CORRUPTED_ENTRANCE, Gauntlet.TELEPORT_PLATFORM,
                GauntletLobby.PLATFORM, Gauntlet.SCOREBOARD, GauntletLobby.SCOREBOARD);
        for (Map.Entry<String, int[]> e : objects.entrySet()) {
            BlockPos at = lobbyTile(e.getValue());
            expect(blockAt(at).equals(e.getKey()), "the lobby has " + blockAt(at) + " where " + e.getKey() + " should stand ("
                    + e.getValue()[0] + "," + e.getValue()[1] + ")");
        }
        expect(world.getBlockState(lobbyTile(GauntletLobby.PLATFORM).above(2)).isAir(), "the lobby's ceiling is too low");
        use(lobbyTile(GauntletLobby.PLATFORM));
        int px = GauntletLobby.blockX(GauntletLobby.PORTAL_X);
        int pz = GauntletLobby.blockZ(GauntletLobby.PORTAL_Y);
        expect(Math.abs(p.getBlockX() - px) <= 12 && Math.abs(p.getBlockZ() - pz) <= 12 && !GauntletService.inLobby(p.blockPosition()),
                "the lobby's teleport platform did not lead back to the Gauntlet Portal");
        expect(GauntletService.enterLobby(p) && GauntletService.inLobby(p.blockPosition()), "could not get back into the lobby");
        note("lobby objects in place");
    }

    /** Enters through the lobby's entrance block; checks the stash and the starting items. */
    private void enter(Mode mode) {
        Inv.give(p, "bronze_dagger", 1);
        int[] tile = mode == Mode.NORMAL ? GauntletLobby.ENTRANCE : GauntletLobby.CORRUPTED_ENTRANCE;
        BlockPos entrance = lobbyTile(tile);
        moveTo(entrance.south());
        use(entrance);
        expect(MinigameManager.session(p) instanceof GauntletSession, "the lobby's " + mode + " entrance did not start a run ("
                + GauntletSession.requirement(p, mode == Mode.NORMAL ? Minigame.GAUNTLET.entranceBlock() : Gauntlet.CORRUPTED_ENTRANCE) + ")");
        g = (GauntletSession) MinigameManager.session(p);
        expect(g.mode() == mode, "entered the " + g.mode() + " Gauntlet, not the " + mode + " one");
        expect(g.online().contains(p), "the player is not in the run's online list (fake player?)");
        expect(!Inv.has(p, "bronze_dagger", 1), "the player's items were not stored on entry");
        for (String item : Gauntlet.startingItems(mode)) {
            expect(Inv.has(p, item, 1), "entering didn't hand out " + item);
        }
    }

    /** Leaves no run behind (a failed check must not strand the player's items). */
    private void close() {
        if (g != null && !g.isEnded()) {
            g.end(false);
        }
        if (GauntletStash.has(p)) {
            GauntletStash.restore(p);
        }
        GauntletStash.claimChest(p);
    }

    // ------------------------------------------------------------------ the maze

    private void enterRoom(int cx, int cy) {
        moveTo(g.roomCentre(cx, cy));
        tick();
        expect(g.lit(cx, cy), "walking into room " + cx + "," + cy + " did not light it");
    }

    private void clearCreatures() {
        for (Mob m : g.creatures()) {
            kill(m);
        }
    }

    /** Gathers every node of a type in the current room through the real block, tool check and gathering step. */
    private int gatherAll(Node node) {
        int got = 0;
        for (Map.Entry<BlockPos, Node> e : g.nodesLeft().entrySet()) {
            if (e.getValue() != node) {
                continue;
            }
            BlockPos pos = e.getKey();
            moveTo(pos.north());
            use(pos);
            ActionManager.cancel(p);
            for (int i = 0; i < 10 && g.gatherOnce(p, node, pos); i++) {
                got++;
            }
            got++;
            expect(world.getBlockState(pos).isAir(), "the " + node.displayName + " did not deplete");
        }
        return got;
    }

    /** Lights rooms until one of each armour node has been gathered (the inner rooms always hold them in the normal mode). */
    private void gather(Mode mode) {
        Map<Node, Integer> gathered = new java.util.EnumMap<>(Node.class);
        long xpBefore = Players.data(p).skills().xpTenths(Skill.MINING);
        List<int[]> order = new ArrayList<>();
        for (int x = Gauntlet.BOSS - 1; x <= Gauntlet.BOSS + 1; x++) {
            for (int y = Gauntlet.BOSS - 1; y <= Gauntlet.BOSS + 1; y++) {
                order.add(new int[]{x, y});
            }
        }
        for (int x = 0; x < Gauntlet.GRID; x++) {
            for (int y = 0; y < Gauntlet.GRID; y++) {
                if (Math.abs(x - Gauntlet.BOSS) > 1 || Math.abs(y - Gauntlet.BOSS) > 1) {
                    order.add(new int[]{x, y});
                }
            }
        }
        int rooms = 0;
        for (int[] c : order) {
            Gauntlet.Room room = g.room(c[0], c[1]);
            if (room.kind() != Gauntlet.RoomKind.RESOURCE || room.creatures().stream().anyMatch(k -> Foe.of(k) == Foe.DEMI)) {
                continue;
            }
            boolean wanted = false;
            for (Node n : new Node[]{Node.ORE, Node.BARK, Node.LINUM}) {
                wanted |= room.nodes().contains(n) && !gathered.containsKey(n);
            }
            if (!wanted) {
                continue;
            }
            enterRoom(c[0], c[1]);
            rooms++;
            clearCreatures();
            for (Node n : new Node[]{Node.ORE, Node.BARK, Node.LINUM}) {
                if (!gathered.containsKey(n) && room.nodes().contains(n)) {
                    int before = count(n.item(mode));
                    int got = gatherAll(n);
                    expect(count(n.item(mode)) - before == got, n + ": gathered " + got + " but holds " + (count(n.item(mode)) - before) + " more");
                    expect(got >= n.yield, n + " gave " + got + " (a node gives " + n.yield + ")");
                    gathered.put(n, got);
                }
            }
            if (gathered.size() == 3) {
                break;
            }
        }
        expect(gathered.size() == 3, "found only " + gathered.keySet() + " in the maze");
        expect(Players.data(p).skills().xpTenths(Skill.MINING) > xpBefore, "mining a crystal deposit gave no Mining XP");
        note("gathered " + gathered + " in " + rooms + " rooms");
    }

    /** Kills one demi-boss (the N/E/S/W-most rooms always hold one): frame, its component and 50+ shards. */
    private Piece demiBoss(Mode mode) {
        for (int[] c : new int[][]{{Gauntlet.BOSS, 0}, {Gauntlet.GRID - 1, Gauntlet.BOSS}, {Gauntlet.BOSS, Gauntlet.GRID - 1}, {0, Gauntlet.BOSS}}) {
            Gauntlet.Room room = g.room(c[0], c[1]);
            String kind = room.creatures().isEmpty() ? null : room.creatures().get(0);
            if (kind == null || Foe.of(kind) != Foe.DEMI) {
                throw fail("room " + c[0] + "," + c[1] + " has no demi-boss: " + room.creatures());
            }
            int shards = count(Gauntlet.shards(mode));
            int frames = count(Gauntlet.frame(mode));
            enterRoom(c[0], c[1]);
            List<Mob> mobs = g.creatures();
            Mob demi = mobs.stream().filter(m -> MinigameManager.creatureId(m).equals(Gauntlet.creature(mode, kind))).findFirst().orElse(null);
            expect(demi != null, "lighting the room did not wake its " + kind);
            kill(demi);
            Piece piece = Gauntlet.componentOf(kind);
            expect(count(Gauntlet.component(mode, piece)) == 1, "the first " + kind + " did not drop its " + Gauntlet.component(mode, piece));
            expect(count(Gauntlet.frame(mode)) == frames + 1, "the " + kind + " did not drop a weapon frame");
            expect(count(Gauntlet.shards(mode)) - shards >= 50, "the " + kind + " dropped only " + (count(Gauntlet.shards(mode)) - shards) + " shards");
            note("killed the " + kind);
            return piece;
        }
        throw fail("no demi-boss");
    }

    /** Lights more rooms and kills their creatures until the player holds enough shards. */
    private void shardsUpTo(Mode mode, int want) {
        for (int x = 0; x < Gauntlet.GRID && count(Gauntlet.shards(mode)) < want; x++) {
            for (int y = 0; y < Gauntlet.GRID && count(Gauntlet.shards(mode)) < want; y++) {
                Gauntlet.Room room = g.room(x, y);
                if (g.lit(x, y) || room.creatures().isEmpty()) {
                    continue;
                }
                enterRoom(x, y);
                clearCreatures();
            }
        }
        expect(count(Gauntlet.shards(mode)) >= want, "the whole maze gave only " + count(Gauntlet.shards(mode)) + " shards");
    }

    /** Sings armour and a weapon up to perfected at the singing bowl. */
    private void sing(Mode mode, Piece weapon) {
        moveTo(g.roomCentre(Gauntlet.START_X, Gauntlet.START_Y));
        tick();
        shardsUpTo(mode, 100);
        moveTo(g.roomCentre(Gauntlet.START_X, Gauntlet.START_Y));
        long smithing = Players.data(p).skills().xpTenths(Skill.SMITHING);
        int points = g.points();
        use(g.startObject(Gauntlet.SINGING_BOWL));
        expect(g.singRecipe(p, "helm_basic"), "singing a basic helm failed");
        expect(Inv.has(p, Gauntlet.equipment(mode, Piece.HELM, Tier.BASIC), 1), "no basic helm after singing it");
        String key = weapon.key();
        expect(g.singRecipe(p, key + "_basic"), "singing a basic " + key + " from the frame failed");
        expect(g.singRecipe(p, key + "_attuned"), "attuning the " + key + " failed");
        expect(g.singRecipe(p, key + "_perfected"), "perfecting the " + key + " with its component failed");
        String perfected = Gauntlet.equipment(mode, weapon, Tier.PERFECTED);
        expect(Inv.has(p, perfected, 1), "no perfected " + key);
        expect(Players.data(p).skills().xpTenths(Skill.SMITHING) > smithing, "singing gave no Smithing XP");
        expect(g.points() - points == 2 + 2 + 5 + 10, "singing gave " + (g.points() - points) + " points (want 19)");
        // wear them: the weapon in hand, the helm on the head
        net.minecraft.world.item.ItemStack held = p.getMainHandItem().copy();
        Inv.remove(p, perfected, 1);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, Inv.stack(perfected, 1));
        if (!held.isEmpty() && !perfected.equals(Inv.idOf(held))) {
            Inv.give(p, held);
        }
        String helm = Gauntlet.equipment(mode, Piece.HELM, Tier.BASIC);
        Inv.remove(p, helm, 1);
        p.setItemSlot(EquipmentSlot.HEAD, Inv.stack(helm, 1));
        note("sang a helm and a perfected " + key);
    }

    // ------------------------------------------------------------------ the Hunllef

    private String hunllef(Mode mode) {
        BlockPos barrier = g.startObject(Gauntlet.BARRIER);
        expect(blockAt(barrier).equals(Gauntlet.BARRIER), "no barrier at the boss room");
        moveTo(barrier.south());
        use(barrier);
        expect(g.inBossFight() && g.selfTestHunllefAlive(), "the barrier did not start the Hunllef fight");
        DataBossEntity boss = (DataBossEntity) g.hunllef();
        BossSelfTests.HunllefView v = BossSelfTests.hunllef(boss);
        expect(v != null, "the Hunllef has no Hunllef script");
        List<String> attacks = new ArrayList<>();
        Mechanics.AttackListener before = Mechanics.attackListener;
        Mechanics.attackListener = (mob, target, attack) -> {
            before.onAttack(mob, target, attack);
            if (mob == boss) {
                attacks.add(attack.name());
            }
        };
        boolean sawMagic = false;
        boolean backToRanged = false;
        int maxTornadoes = 0;
        boolean floor = false;
        boolean orange = false;
        try {
            for (int t = 0; t < 120; t++) {
                tick();
                v = BossSelfTests.hunllef(boss);
                sawMagic |= v.magic();
                backToRanged |= sawMagic && !v.magic();
                maxTornadoes = Math.max(maxTornadoes, v.tornadoes());
                floor |= v.floorTiles() > 0;
                orange |= v.floorOrange();
                if (backToRanged && maxTornadoes > 0 && orange && t > 40) {
                    break;
                }
            }
        } finally {
            Mechanics.attackListener = before;
        }
        expect(!attacks.isEmpty(), "the Hunllef never attacked");
        expect(attacks.get(0).equals(mode == Mode.NORMAL ? "crystal_spikes" : "corrupted_spikes"),
                "the Hunllef's first attack was " + attacks.get(0) + ", not ranged");
        expect(sawMagic && backToRanged, "the Hunllef did not switch styles every four attacks (" + attacks + ")");
        expect(maxTornadoes == Gauntlet.tornadoes(mode, 100), "the Hunllef summoned " + maxTornadoes + " tornadoes at full health");
        expect(floor && orange, "the floor never lit up (tiles " + floor + ", orange " + orange + ")");
        prayerDisable(mode, boss);
        // Its prayer: six off-prayer hits of one style make it protect from that style, which then does nothing.
        AttackType prays = BossSelfTests.hunllef(boss).prays();
        AttackType off = prays == AttackType.RANGED ? AttackType.MAGIC : AttackType.RANGED;
        expect(hit(boss, 1, prays) == 0, "a hit of the style it protects from (" + prays + ") did damage");
        float hp = boss.getHealth();
        for (int i = 0; i < 6; i++) {
            hit(boss, 1, off);
        }
        expect(boss.getHealth() < hp, "off-prayer hits did no damage");
        expect(BossSelfTests.hunllef(boss).prays() == off, "six " + off + " hits did not make it pray against " + off);
        expect(hit(boss, 5, off) == 0, "it still took " + off + " damage after praying against it");
        // The kill, with heavy hits of styles it doesn't protect from.
        int kc = Players.data(p).killCount(mode.boss);
        AttackType[] styles = {AttackType.SLASH, AttackType.RANGED, AttackType.MAGIC};
        for (int i = 0; i < 200 && boss.isAlive(); i++) {
            AttackType now = BossSelfTests.hunllef(boss).prays();
            AttackType use = styles[i % 3];
            if (sameStyle(use, now)) {
                use = styles[(i + 1) % 3];
            }
            hit(boss, 40, use);
        }
        expect(!boss.isAlive(), "the Hunllef did not die to 200 heavy hits");
        expect(Players.data(p).killCount(mode.boss) == kc + 1, "the Hunllef's kill count did not go up");
        expect(g.bossDead(), "the run did not see the Hunllef's death");
        return "attacks " + attacks.subList(0, Math.min(10, attacks.size())) + ", tornadoes " + maxTornadoes;
    }

    /**
     * The prayer-disabling orb is one of its magic attacks: Protect from Magic still lowers it to the prayed max (12 / 16,
     * less with armour), and then every prayer goes off. Unprayed it lands in full.
     */
    private void prayerDisable(Mode mode, DataBossEntity boss) {
        PlayerData d = Players.data(p);
        int prayedMax = Gauntlet.hunllefMaxHit(mode, 0);
        int top = Gauntlet.hunllefUnprayedMax(mode);
        d.activePrayers().add(com.runecraft.osrs.Prayer.PROTECT_FROM_MAGIC);
        int through = BossSelfTests.hunllefPrayerDisable(boss, p, top);
        expect(through >= 0 && through <= prayedMax,
                "a top prayer-disable roll through Protect from Magic let " + through + " through (prayed max " + prayedMax + ")");
        expect(d.activePrayers().isEmpty(), "the prayer-disabling orb left prayers on");
        expect(BossSelfTests.hunllefPrayerDisable(boss, p, top) == top, "an unprayed prayer-disable roll was lowered");
        // The real attack, accuracy roll and all, ten times through the right prayer: never above the prayed max.
        int worst = 0;
        for (int i = 0; i < 10; i++) {
            heal();
            d.activePrayers().add(com.runecraft.osrs.Prayer.PROTECT_FROM_MAGIC);
            int landed = prayerOrb(boss);
            expect(landed >= 0, "the Hunllef's prayer-disabling orb never landed");
            expect(landed <= prayedMax, "a prayer-disabling orb through Protect from Magic hit " + landed + " (prayed max " + prayedMax + ")");
            expect(!d.isActive(com.runecraft.osrs.Prayer.PROTECT_FROM_MAGIC), "the prayer-disabling orb left Protect from Magic on");
            worst = Math.max(worst, landed);
        }
        heal();
        note("prayer-disabling orb through the right prayer: at most " + worst);
    }

    /** The player the prayer-disabling orb is watched landing on, and the damage it landed for (-1 while in flight). */
    private static ServerPlayer orbWatched;
    private static int orbDamage = -1;
    private static boolean orbHooked;

    /**
     * The Hunllef's prayer-disabling orb through the real attack path (accuracy roll and all): launched, then flown to the player
     * (it lands after its flight since round 41: the clock and the pending hits are advanced to its landing), and the damage it
     * came through for read off the damage hook. -1 when it never landed.
     */
    private int prayerOrb(DataBossEntity boss) {
        if (!orbHooked) {
            orbHooked = true;
            com.runecraft.combat.CombatHooks.addDamageListener((player, attacker, attack, style, damage) -> {
                if (player == orbWatched && attacker == g.hunllef() && "prayer_disable".equals(attack)) {
                    orbDamage = damage;
                }
            });
        }
        orbWatched = p;
        orbDamage = -1;
        try {
            int now = BossSelfTests.hunllefPrayerDisable(boss, p, -1);
            for (int i = 0; i < 60 && orbDamage < 0 && now < 0; i++) {
                ActionManager.advanceForSelfTest();
                Mechanics.tickForSelfTest();
            }
            return now >= 0 ? now : orbDamage;
        } finally {
            orbWatched = null;
        }
    }

    private static boolean sameStyle(AttackType a, AttackType b) {
        return a == b || (a.isMelee() && b.isMelee());
    }

    // ------------------------------------------------------------------ the whole run

    /**
     * Walks a whole normal Gauntlet run as the real player: the portal and lobby, the entrance, rooms lit, nodes gathered,
     * creatures and a demi-boss killed, armour and a perfected weapon sung, the Hunllef's cycles, tornadoes, floor and
     * prayer, its kill, the way back to the lobby and the reward chest. Returns a summary; throws on the first broken step.
     */
    public static String walk(ServerPlayer p) {
        GauntletSelfTest t = new GauntletSelfTest(p);
        songOfTheElves(p);
        GauntletStash.restore(p);
        GauntletStash.claimChest(p);
        p.getInventory().clearContent();
        try {
            t.lobby();
            t.enter(Mode.NORMAL);
            t.gather(Mode.NORMAL);
            Piece weapon = t.demiBoss(Mode.NORMAL);
            t.sing(Mode.NORMAL, weapon);
            t.note(t.hunllef(Mode.NORMAL));
            int shards = t.count("crystal_shard");
            for (int i = 0; i < 10 && !t.g.isEnded(); i++) {
                t.tick();
            }
            expect(t.g.isEnded(), "the run did not end after the Hunllef died");
            expect(MinigameManager.session(p) == null, "the player is still bound to the run");
            expect(GauntletService.inLobby(p.blockPosition()), "the player did not come back to the lobby (at " + p.blockPosition() + ")");
            expect(Inv.has(p, "bronze_dagger", 1), "the player's belongings were not given back");
            for (String id : Gauntlet.runItems()) {
                expect(!Inv.has(p, id, 1), id + " left the Gauntlet");
            }
            int stacks = GauntletStash.chestSize(p);
            expect(stacks >= 2, "the reward chest holds " + stacks + " stacks (want the shards and the main-table rolls)");
            BlockPos chest = lobbyTile(GauntletLobby.REWARD_CHEST);
            t.moveTo(chest.south());
            t.use(chest);
            expect(GauntletStash.chestSize(p) == 0, "opening the reward chest did not empty it");
            int got = t.count("crystal_shard") - shards;
            expect(got >= 5 && got <= 9, "the chest gave " + got + " crystal shards (wiki: 5-9)");
            t.note("chest: " + stacks + " stacks, " + got + " crystal shards");
            return t.log.toString();
        } finally {
            t.close();
        }
    }

    /**
     * The ways a run ends without the Hunllef: the starting room's teleport platform (no loot), a death with points (the junk
     * or incomplete table, a death counted), and the corrupted entrance's requirement.
     */
    public static String failures(ServerPlayer p) {
        GauntletSelfTest t = new GauntletSelfTest(p);
        songOfTheElves(p);
        GauntletStash.restore(p);
        GauntletStash.claimChest(p);
        p.getInventory().clearContent();
        try {
            expect(GauntletService.enterLobby(p), "the Gauntlet Portal refused the player");
            if (Players.data(p).killCount(Mode.NORMAL.boss) == 0) {
                expect(GauntletSession.requirement(p, Gauntlet.CORRUPTED_ENTRANCE) != null, "the Corrupted Gauntlet let in a player with no completion");
            }
            // The teleport platform: out, with nothing in the chest.
            t.enter(Mode.NORMAL);
            BlockPos platform = t.g.startObject(Gauntlet.TELEPORT_PLATFORM);
            expect(t.blockAt(platform).equals(Gauntlet.TELEPORT_PLATFORM), "no teleport platform in the starting room");
            // the tool storage gives back a lost tool
            String pick = Node.ORE.tool(Mode.NORMAL);
            Inv.remove(p, pick, 1);
            t.use(t.g.startObject(Gauntlet.TOOL_STORAGE));
            expect(Inv.has(p, pick, 1), "the tool storage did not give back the pickaxe");
            t.use(platform);
            expect(t.g.isEnded() && MinigameManager.session(p) == null, "the teleport platform did not end the run");
            expect(GauntletStash.chestSize(p) == 0, "leaving by the teleport platform left loot in the chest");
            expect(GauntletService.inLobby(p.blockPosition()), "the teleport platform did not send the player back to the lobby");
            // A death after a kill: the chest rolls the junk table (1-49 points).
            t.enter(Mode.NORMAL);
            int deaths = MinigameData.of(p).stat(p.getUUID(), "deaths:" + Mode.NORMAL.boss);
            Mob weak = null;
            for (int x = 0; x < Gauntlet.GRID && weak == null; x++) {
                for (int y = 0; y < Gauntlet.GRID && weak == null; y++) {
                    if (t.g.room(x, y).creatures().stream().anyMatch(k -> Foe.of(k) == Foe.WEAK) && !t.g.lit(x, y)) {
                        t.enterRoom(x, y);
                        weak = t.g.creatures().stream().filter(m -> {
                            String k = Gauntlet.kindOf(MinigameManager.creatureId(m));
                            return k != null && Foe.of(k) == Foe.WEAK;
                        }).findFirst().orElse(null);
                    }
                }
            }
            expect(weak != null, "no weak creature in the maze");
            t.kill(weak);
            expect(t.g.points() == Gauntlet.killPoints(Foe.WEAK), "a weak kill gave " + t.g.points() + " points");
            expect(Inv.has(p, Gauntlet.frame(Mode.NORMAL), 1), "the first weak creature did not drop a weapon frame");
            expect(!MinigameManager.allowDeath(p), "a death in the Gauntlet was not handled by the run");
            expect(MinigameManager.session(p) == null, "the player is still in the run after dying");
            expect(MinigameData.of(p).stat(p.getUUID(), "deaths:" + Mode.NORMAL.boss) == deaths + 1, "the death was not counted");
            expect(Inv.has(p, "bronze_dagger", 1), "the player's belongings were not given back after the death");
            expect(GauntletStash.chestSize(p) == 1, "a 2-point run left " + GauntletStash.chestSize(p) + " stacks in the chest (want one junk item)");
            int junk = 0;
            for (String j : List.of(Gauntlet.IWANS_FLYER, Gauntlet.APOTHECARY_POTION, Gauntlet.ROTTEN_TOMATO)) {
                junk -= t.count(j);
            }
            GauntletStash.claimChest(p);
            for (String j : List.of(Gauntlet.IWANS_FLYER, Gauntlet.APOTHECARY_POTION, Gauntlet.ROTTEN_TOMATO)) {
                junk += t.count(j);
            }
            expect(junk == 1, "the chest of a 2-point run did not hold a junk item");
            return "teleport platform: empty chest; death at 2 points: one junk item, deaths +1; tool storage";
        } finally {
            t.close();
        }
    }
}
